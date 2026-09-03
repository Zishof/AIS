package ais.action.servlet.api;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;

/**
 * <h3>Persediaan &amp; Kartu Stok -- layar legacy 08 (Data Stok Barang), varian Inventory &amp; Sales.</h3>
 *
 * <p>Saldo stok = HASIL LEDGER, bukan angka bebas (Matriks layar 08) -- suku ledger PERSIS sama
 * dgn {@code StokKantinUtil.formulaStokSql} (8 suku): pengadaan(+), opname selisih(&plusmn;),
 * penjualan(-), pemakaian bahan baku(-), retur penjualan kembali-ke-stok(+), mutasi antar toko
 * (&plusmn;), retur pembelian(-). Kesetaraan legacy: AWAL + MASUK - KELUAR = AKHIR per rentang
 * tanggal acuan.</p>
 *
 * <p>PERINGATAN nama kolom (landmine implicit-naming deployment ini -- camelCase TIDAK diberi
 * underscore): {@code pengadaan_produk.waktupengadaan}, {@code stok_opname.waktuopname}
 * (dikonfirmasi dari SQL produksi KantinHelper:7736/8146). Tabel lain memakai kolom {@code waktu}
 * satu kata.</p>
 */
public final class SalesInventoryStokHelper {

	private SalesInventoryStokHelper() {
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	/** Ekspresi SUM satu suku ledger utk produk p.id, dgn kondisi tanggal tambahan {@code kondisi}. */
	private static String suku(String tabel, String kolomQty, String kolomProduk, String kolomWaktu,
			String kondisiEkstra, String kondisiWaktu) {
		return "COALESCE((SELECT SUM(" + kolomQty + ") FROM koperasi." + tabel + " x WHERE x." + kolomProduk
				+ " = p.id" + kondisiEkstra + (kondisiWaktu == null ? "" : " AND x." + kolomWaktu + " " + kondisiWaktu)
				+ "),0)";
	}

	/** MASUK - KELUAR ternet dalam satu ekspresi utk rentang kondisi waktu tertentu (null = seluruh histori). */
	private static String ekspresiMasuk(String kondisiWaktu) {
		return suku("pengadaan_produk", "qty", "produk", "waktupengadaan", "", kondisiWaktu)
				+ " + " + suku("retur_penjualan", "qty", "produk", "waktu", " AND x.kembalikan_ke_stok = true", kondisiWaktu)
				+ " + " + suku("mutasi_stok_toko", "qty", "produk_tujuan", "waktu", "", kondisiWaktu);
	}

	private static String ekspresiKeluar(String kondisiWaktu) {
		return suku("pembelian", "qty", "produk", "waktu", "", kondisiWaktu)
				+ " + " + suku("pemakaian_bahan_baku", "qty", "produk", "waktu", "", kondisiWaktu)
				+ " + " + suku("mutasi_stok_toko", "qty", "produk_asal", "waktu", "", kondisiWaktu)
				+ " + " + suku("retur_pembelian", "qty", "produk", "waktu", "", kondisiWaktu);
	}

	private static String ekspresiOpname(String kondisiWaktu) {
		return suku("stok_opname", "selisih", "produk", "waktuopname", "", kondisiWaktu);
	}

	/**
	 * {@code si_inventory_balance} -- daftar persediaan per produk dgn kolom legacy layar 08:
	 * Awal / Masuk / Keluar / Penyesuaian opname / Akhir / nilai (akhir &times; harga beli) / stok
	 * minimum, dalam rentang {@code dari}..{@code sampai} (default: 30 hari terakhir s.d. hari ini).
	 * Filter: keyword (kode/nama/barcode), {@code toko_id} (admin; non-admin dipaksa scope toko
	 * aktor bila ada), flag {@code hanya_minimum} / {@code hanya_negatif} / {@code hanya_tersedia}.
	 */
	public static void inventoryBalance(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int size = request == null ? 20 : request.optInt("page_size", 20);
		if (size < 1) size = 20;
		if (size > 100) size = 100;
		String keyword = request == null || request.isNull("keyword") ? null : request.optString("keyword", "").trim();
		String dari = request == null ? "" : request.optString("dari", "").trim();
		String sampai = request == null ? "" : request.optString("sampai", "").trim();
		if (dari.isEmpty()) dari = new java.text.SimpleDateFormat("yyyy-MM-dd")
				.format(new java.util.Date(System.currentTimeMillis() - 30L * 24 * 3600 * 1000));
		if (sampai.isEmpty()) sampai = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
		Long tokoId = ctx.admin
				? ais.common.Common.angkaAtauNull(request, "toko_id")
				: ctx.tokoId;
		boolean hanyaMinimum = request != null && request.optBoolean("hanya_minimum", false);
		boolean hanyaNegatif = request != null && request.optBoolean("hanya_negatif", false);
		boolean hanyaTersedia = request != null && request.optBoolean("hanya_tersedia", false);

		// Kondisi waktu memakai literal tervalidasi (yyyy-MM-dd) -- BUKAN konkatenasi input mentah.
		if (!dari.matches("\\d{4}-\\d{2}-\\d{2}") || !sampai.matches("\\d{4}-\\d{2}-\\d{2}")) {
			hasil.put("status", "91");
			hasil.put("description", "Format tanggal harus yyyy-MM-dd.");
			return;
		}
		String kondisiAwal = "< DATE '" + dari + "'";
		String kondisiPeriode = "BETWEEN DATE '" + dari + "' AND (DATE '" + sampai + "' + INTERVAL '1 day')";

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE COALESCE(p.aktif, true) = true ");
			java.util.List<Object> params = new java.util.ArrayList<Object>();
			if (keyword != null && !keyword.isEmpty()) {
				where.append(" AND (p.kode ILIKE ? OR p.nama ILIKE ? OR COALESCE(p.barcode,'') ILIKE ?) ");
				String k = "%" + keyword + "%";
				params.add(k); params.add(k); params.add(k);
			}
			boolean jalurTenant = SalesInventoryStokTenant.aktif(ctx);
			if (tokoId != null) {
				if (jalurTenant) {
					// Lingkup toko pada model tenant = lingkup GUDANG. Daftar barisnya dibatasi
					// produk yang berstok di gudang toko itu; angkanya dibatasi terpisah, di
					// dalam selectSaldo. Tanpa keduanya, produk toko ini akan tampil dengan
					// stok se-tenant.
					where.append(SalesInventoryStokTenant.syaratTokoProduk(
							SalesInventoryStokTenant.skema(ctx.tenant), tokoId));
				} else {
					where.append(" AND p.toko = ? ");
					params.add(tokoId);
				}
			}

			String select;
			if (jalurTenant) {
				// Buku besar tunggal mutasi_stok. Kolom keluarannya sama dan berurutan sama
				// dengan jalur legacy, sehingga seluruh pembungkus, paginasi, dan perakitan
				// JSON di bawah dipakai bersama tanpa digandakan.
				select = SalesInventoryStokTenant.selectSaldo(
						SalesInventoryStokTenant.skema(ctx.tenant), dari, sampai, where.toString(),
						tokoId);
			} else {
				String awal = "(" + ekspresiMasuk(kondisiAwal) + " + " + ekspresiOpname(kondisiAwal) + " - ("
						+ ekspresiKeluar(kondisiAwal) + "))";
				String masuk = "(" + ekspresiMasuk(kondisiPeriode) + ")";
				String keluar = "(" + ekspresiKeluar(kondisiPeriode) + ")";
				String opname = "(" + ekspresiOpname(kondisiPeriode) + ")";

				// PERHATIAN kolom: hargajual/hargabeli TANPA underscore (Produk.getHargaBeli/getHargaJual
				// tak ber-@Column, implicit-naming deployment ini menggabung camelCase -- terbukti dari
				// SQL produksi KantinHelper:3494); satuan = FK ke koperasi.satuan_produk.
				select = "SELECT p.id AS id, p.kode AS kode, COALESCE(p.barcode,'') AS barcode, "
						+ "p.nama AS nama, COALESCE(NULLIF(TRIM(sp.nama),''),'(Belum diatur)') AS satuan, "
						+ "COALESCE(p.hargabeli,0) AS harga_beli, COALESCE(p.hargajual,0) AS harga_jual, "
						+ "COALESCE(p.stok_minimum,0) AS stok_minimum, "
						+ awal + " AS awal, " + masuk + " AS masuk, " + keluar + " AS keluar, " + opname + " AS opname "
						+ "FROM koperasi.produk p LEFT JOIN koperasi.satuan_produk sp ON p.satuan = sp.id" + where;
			}
			String filterLuar = "";
			if (hanyaMinimum) filterLuar = " WHERE (t.awal + t.masuk + t.opname - t.keluar) <= t.stok_minimum ";
			else if (hanyaNegatif) filterLuar = " WHERE (t.awal + t.masuk + t.opname - t.keluar) < 0 ";
			else if (hanyaTersedia) filterLuar = " WHERE (t.awal + t.masuk + t.opname - t.keluar) > 0 ";

			String bungkus = "SELECT * FROM (SELECT t.id, t.kode, t.barcode, t.nama, t.satuan, t.harga_beli, "
					+ "t.harga_jual, t.stok_minimum, t.awal, t.masuk, t.keluar, t.opname, "
					+ "(t.awal + t.masuk + t.opname - t.keluar) AS akhir FROM (" + select + ") t"
					+ filterLuar + ") z ORDER BY z.kode ASC";

			java.sql.PreparedStatement psTotal = session.connection()
					.prepareStatement("SELECT COUNT(*) FROM (" + bungkus + ") c");
			for (int i = 0; i < params.size(); i++) psTotal.setObject(i + 1, params.get(i));
			java.sql.ResultSet rsTotal = psTotal.executeQuery();
			long total = rsTotal.next() ? rsTotal.getLong(1) : 0;
			rsTotal.close(); psTotal.close();

			java.sql.PreparedStatement ps = session.connection()
					.prepareStatement(bungkus + " LIMIT ? OFFSET ?");
			int idx = 1;
			for (int i = 0; i < params.size(); i++) ps.setObject(idx++, params.get(i));
			ps.setInt(idx++, size);
			ps.setInt(idx++, (page - 1) * size);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			double totalNilai = 0;
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("produkId", rs.getLong(1));
				j.put("kode", str(rs.getString(2)));
				j.put("barcode", str(rs.getString(3)));
				j.put("nama", str(rs.getString(4)));
				j.put("satuan", str(rs.getString(5)));
				double hargaBeli = rs.getDouble(6);
				j.put("hargaBeli", hargaBeli);
				j.put("hargaJual", rs.getDouble(7));
				j.put("stokMinimum", rs.getDouble(8));
				j.put("awal", rs.getDouble(9));
				j.put("masuk", rs.getDouble(10));
				j.put("keluar", rs.getDouble(11));
				j.put("opname", rs.getDouble(12));
				double akhir = rs.getDouble(13);
				j.put("akhir", akhir);
				double nilai = akhir * hargaBeli;
				j.put("totalHarga", nilai);
				totalNilai += nilai;
				arr.put(j);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", page);
			hasil.put("pageSize", size);
			hasil.put("dari", dari);
			hasil.put("sampai", sampai);
			hasil.put("totalNilaiHalaman", totalNilai);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * {@code si_inventory_ledger} -- KARTU STOK satu produk: seluruh mutasi ber-tanggal (8 suku
	 * ledger) + saldo berjalan (window SUM) urut waktu. Param: {@code produk_id} wajib,
	 * {@code dari}/{@code sampai} opsional (default 90 hari). Baris juga membawa jenis mutasi +
	 * referensi (nomor faktur kulakan, kode transaksi, dsb. bila ada).
	 */
	public static void inventoryLedger(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		Long produkId = ais.common.Common.angkaAtauNull(request, "produk_id");
		if (produkId == null) {
			hasil.put("status", "91");
			hasil.put("description", "produk_id wajib diisi.");
			return;
		}
		String dari = request.optString("dari", "").trim();
		String sampai = request.optString("sampai", "").trim();
		if (dari.isEmpty()) dari = new java.text.SimpleDateFormat("yyyy-MM-dd")
				.format(new java.util.Date(System.currentTimeMillis() - 90L * 24 * 3600 * 1000));
		if (sampai.isEmpty()) sampai = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
		if (!dari.matches("\\d{4}-\\d{2}-\\d{2}") || !sampai.matches("\\d{4}-\\d{2}-\\d{2}")) {
			hasil.put("status", "91");
			hasil.put("description", "Format tanggal harus yyyy-MM-dd.");
			return;
		}
		long pid = produkId.longValue();

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String rentang = "BETWEEN DATE '" + dari + "' AND (DATE '" + sampai + "' + INTERVAL '1 day')";
			StringBuilder sql = new StringBuilder();
			if (SalesInventoryStokTenant.aktif(ctx)) {
				// Sembilan UNION legacy runtuh menjadi satu pemindaian: di sisi tenant seluruh
				// pergerakan sudah berada pada satu buku besar. Kolom keluarannya tetap
				// waktu/jenis/referensi/masuk/keluar + saldo berjalan, sehingga perakitan JSON
				// di bawah tidak berubah.
				String sk = SalesInventoryStokTenant.skema(ctx.tenant);
				sql.append("WITH kartu AS ( ")
						.append(SalesInventoryStokTenant.sqlKartu(sk, pid, dari, sampai))
						.append(") SELECT waktu, jenis, referensi, masuk, keluar, "
								+ "SUM(masuk - keluar) OVER (ORDER BY waktu ASC, jenis ASC, referensi ASC "
								+ "ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS saldo_periode "
								+ "FROM kartu ORDER BY waktu ASC, jenis ASC, referensi ASC LIMIT 2000");
			} else {
			sql.append("WITH kartu AS ( ");
			sql.append("SELECT x.waktupengadaan AS waktu, 'Kulakan/Pengadaan' AS jenis, COALESCE(x.nomorfaktur,'') AS referensi, x.qty AS masuk, 0 AS keluar FROM koperasi.pengadaan_produk x WHERE x.produk = ").append(pid)
					.append(" AND x.waktupengadaan ").append(rentang);
			sql.append(" UNION ALL SELECT x.waktu, 'Penjualan', COALESCE(('#' || x.id), ''), 0, x.qty FROM koperasi.pembelian x WHERE x.produk = ").append(pid)
					.append(" AND x.waktu ").append(rentang);
			sql.append(" UNION ALL SELECT x.waktu, 'Pemakaian Bahan Baku', COALESCE(('#' || x.id), ''), 0, x.qty FROM koperasi.pemakaian_bahan_baku x WHERE x.produk = ").append(pid)
					.append(" AND x.waktu ").append(rentang);
			sql.append(" UNION ALL SELECT x.waktu, 'Retur Penjualan (kembali ke stok)', COALESCE(('#' || x.id), ''), x.qty, 0 FROM koperasi.retur_penjualan x WHERE x.produk = ").append(pid)
					.append(" AND x.kembalikan_ke_stok = true AND x.waktu ").append(rentang);
			sql.append(" UNION ALL SELECT x.waktu, 'Retur Pembelian', COALESCE(('#' || x.id), ''), 0, x.qty FROM koperasi.retur_pembelian x WHERE x.produk = ").append(pid)
					.append(" AND x.waktu ").append(rentang);
			sql.append(" UNION ALL SELECT x.waktu, ('Produksi (' || x.jenis || CASE WHEN x.arah = 'REVERSE' THEN ', dibalik' ELSE '' END || ')'), "
					+ "COALESCE(('#' || x.dokumen_id), ''), COALESCE(x.qty_masuk,0), COALESCE(x.qty_keluar,0) FROM koperasi.mutasi_stok_produksi x WHERE x.produk = ").append(pid)
					.append(" AND x.waktu ").append(rentang);
			sql.append(" UNION ALL SELECT x.waktu, 'Mutasi Masuk (antar toko)', COALESCE(('#' || x.id), ''), x.qty, 0 FROM koperasi.mutasi_stok_toko x WHERE x.produk_tujuan = ").append(pid)
					.append(" AND x.waktu ").append(rentang);
			sql.append(" UNION ALL SELECT x.waktu, 'Mutasi Keluar (antar toko)', COALESCE(('#' || x.id), ''), 0, x.qty FROM koperasi.mutasi_stok_toko x WHERE x.produk_asal = ").append(pid)
					.append(" AND x.waktu ").append(rentang);
			sql.append(" UNION ALL SELECT x.waktuopname, 'Penyesuaian Opname', COALESCE(('#' || x.id), ''), "
					+ "CASE WHEN x.selisih >= 0 THEN x.selisih ELSE 0 END, CASE WHEN x.selisih < 0 THEN -x.selisih ELSE 0 END "
					+ "FROM koperasi.stok_opname x WHERE x.produk = ").append(pid)
					.append(" AND x.waktuopname ").append(rentang);
			sql.append(") SELECT waktu, jenis, referensi, masuk, keluar, "
					+ "SUM(masuk - keluar) OVER (ORDER BY waktu ASC, jenis ASC, referensi ASC ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS saldo_periode "
					+ "FROM kartu ORDER BY waktu ASC, jenis ASC, referensi ASC LIMIT 2000");
			}

			// Saldo AWAL sebelum rentang (semua suku < dari) supaya kartu bisa menampilkan saldo absolut.
			//
			// PENTING: ini berada di LUAR cabang tenant/legacy di atas, sehingga kedua jalur
			// melewatinya. Sebelum perbaikan ini, jalur tenant membangun kartunya dari
			// mutasi_stok tenant tetapi saldo awalnya tetap dari koperasi.* -- yakni tabel
			// instalasi bersama, disaring dengan id produk MILIK TENANT. Id itu bertabrakan
			// antar-schema, jadi hasilnya bukan galat melainkan angka milik data lain (atau nol),
			// dan karena tiap baris kartu ditampilkan sebagai saldoAwal + saldo berjalan,
			// SELURUH kolom saldo pada kartu itu ikut salah tanpa satu pun tanda.
			String kondisiAwal = "< DATE '" + dari + "'";
			String sqlAwal = SalesInventoryStokTenant.aktif(ctx)
					? SalesInventoryStokTenant.sqlSaldoAwal(
							SalesInventoryStokTenant.skema(ctx.tenant), pid, dari)
					: "SELECT (" + ekspresiMasukPid(pid, kondisiAwal) + " + "
							+ ekspresiOpnamePid(pid, kondisiAwal)
							+ " - (" + ekspresiKeluarPid(pid, kondisiAwal) + "))";
			java.sql.PreparedStatement psAwal = session.connection().prepareStatement(sqlAwal);
			java.sql.ResultSet rsAwal = psAwal.executeQuery();
			double saldoAwal = rsAwal.next() ? rsAwal.getDouble(1) : 0;
			rsAwal.close(); psAwal.close();

			java.sql.PreparedStatement ps = session.connection().prepareStatement(sql.toString());
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("waktu", str(rs.getTimestamp(1)));
				j.put("jenis", str(rs.getString(2)));
				j.put("referensi", str(rs.getString(3)));
				j.put("masuk", rs.getDouble(4));
				j.put("keluar", rs.getDouble(5));
				j.put("saldo", saldoAwal + rs.getDouble(6));
				arr.put(j);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("saldoAwal", saldoAwal);
			hasil.put("dari", dari);
			hasil.put("sampai", sampai);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// Varian ekspresi per-pid (tanpa alias p) utk query saldo awal kartu stok.
	private static String sukuPid(String tabel, String kolomQty, String kolomProduk, String kolomWaktu,
			long pid, String kondisiEkstra, String kondisiWaktu) {
		return "COALESCE((SELECT SUM(" + kolomQty + ") FROM koperasi." + tabel + " x WHERE x." + kolomProduk
				+ " = " + pid + kondisiEkstra + " AND x." + kolomWaktu + " " + kondisiWaktu + "),0)";
	}

	private static String ekspresiMasukPid(long pid, String kondisiWaktu) {
		return sukuPid("pengadaan_produk", "qty", "produk", "waktupengadaan", pid, "", kondisiWaktu)
				+ " + " + sukuPid("retur_penjualan", "qty", "produk", "waktu", pid, " AND x.kembalikan_ke_stok = true", kondisiWaktu)
				+ " + " + sukuPid("mutasi_stok_toko", "qty", "produk_tujuan", "waktu", pid, "", kondisiWaktu);
	}

	private static String ekspresiKeluarPid(long pid, String kondisiWaktu) {
		return sukuPid("pembelian", "qty", "produk", "waktu", pid, "", kondisiWaktu)
				+ " + " + sukuPid("pemakaian_bahan_baku", "qty", "produk", "waktu", pid, "", kondisiWaktu)
				+ " + " + sukuPid("mutasi_stok_toko", "qty", "produk_asal", "waktu", pid, "", kondisiWaktu)
				+ " + " + sukuPid("retur_pembelian", "qty", "produk", "waktu", pid, "", kondisiWaktu);
	}

	private static String ekspresiOpnamePid(long pid, String kondisiWaktu) {
		return sukuPid("stok_opname", "selisih", "produk", "waktuopname", pid, "", kondisiWaktu);
	}
}
