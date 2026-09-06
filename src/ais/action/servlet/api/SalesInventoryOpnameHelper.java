package ais.action.servlet.api;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;

/**
 * <h3>Laporan Opname — layar legacy 09, dan cetaknya layar 10.</h3>
 *
 * <p>Dua aksi: {@code si_stock_count_list} dan {@code si_stock_count_detail}.</p>
 *
 * <h4>KOREKSI: apa sebenarnya layar 09 dan 10 itu</h4>
 * <p>Versi pertama kelas ini menduga 09 = daftar sesi dan 10 = rinciannya. Dugaan itu
 * <b>keliru</b>, dan baru terlihat setelah tangkapan layar aplikasi lamanya benar-benar
 * dibuka:</p>
 * <ul>
 * <li><b>Layar 09 "LAPORAN STOK OPNAME"</b> adalah daftar <b>datar</b> — satu baris per
 * produk per tanggal, berkolom TGL.OPNAME, #KODE, NAMA BARANG, SAT., STOK KOMP., STOK FISIK,
 * SELISIH, HRG.POKOK, TOTAL HARGA, dengan <b>satu angka total</b> di kanan bawah dan tombol
 * CETAK. Bukan daftar sesi.</li>
 * <li><b>Layar 10 "Mencetak Laporan Opname"</b> adalah <b>hasil cetak</b> layar 09 (pratinjau
 * Report Designer), bukan rincian per dokumen.</li>
 * </ul>
 * <p>Karena itu {@code si_stock_count_list} bawaannya {@code mode=produk} — bentuk datar yang
 * sepadan dengan layar 09. Mode {@code sesi} tetap ada karena berguna (ia menjawab "opname apa
 * saja yang pernah dilakukan"), tetapi ia menjawab pertanyaan yang <b>tidak</b> ditanyakan layar
 * lamanya, jadi ia bukan bawaan. {@code si_stock_count_detail} melayani penelusuran satu sesi —
 * tambahan di atas paritas, bukan padanan layar 10.</p>
 *
 * <p>Pelajarannya: paritas fungsional tidak dapat disimpulkan dari NAMA layar lama. "Laporan
 * Opname" dan "Mencetak Laporan Opname" terdengar seperti daftar dan rinciannya; keduanya
 * ternyata satu laporan dan cetaknya.</p>
 *
 * <h4>Mengapa aksi ini ada, padahal layar Stok Opname sudah ada</h4>
 * <p>Gerbang izin di {@code PosApi} sudah menyediakan cabang {@code si_stock_count_*} dengan
 * catatan "layar 9-10 reuse Stok Opname existing", tetapi <b>tidak ada handler-nya</b> — hanya
 * gerbangnya. Akibatnya layar 09-10 jatuh ke aksi {@code so_*} milik layar Stok Opname POS, dan
 * aksi-aksi itu membaca lewat entitas Hibernate yang schema-nya dipatok {@code @Table(schema=...)}.
 * Entitas tidak dapat melihat schema tenant, sehingga 461 dokumen opname tenant terbaca sebagai
 * NOL — bukan karena datanya tidak ada, melainkan karena dibaca lewat jalur yang salah.</p>
 *
 * <p>Perbedaan itu tidak terlihat dari isi tabel; hanya terlihat dengan menjalankan aksi yang
 * benar-benar dipakai layarnya. Pelajaran yang sama sudah dicatat pada {@code si_receivable_list}
 * (doc 120 &sect;3).</p>
 *
 * <h4>Satu perbedaan bentuk antara kedua jalur</h4>
 * <p>{@code koperasi.stok_opname} datar (satu baris = satu produk, tanpa kepala dokumen);
 * model tenant berkepala + berrinci. Jalur legacy karena itu <b>mengelompokkan</b> baris datarnya
 * per tanggal &times; toko menjadi satu "sesi", supaya kolom keluaran kedua jalur sama dan
 * berurutan sama. Konsekuensinya {@code id} sesi legacy adalah {@code MIN(id)} barisnya —
 * penunjuk, bukan kunci dokumen — dan {@code si_stock_count_detail} memakainya untuk menemukan
 * kembali tanggal &times; toko sesi itu. Ini dinyatakan di sini supaya tidak ada yang mengira
 * {@code id} legacy menunjuk satu dokumen yang berdiri sendiri.</p>
 *
 * @see SalesInventoryOpnameTenant
 */
public final class SalesInventoryOpnameHelper {

	private SalesInventoryOpnameHelper() {
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static String hariIni() {
		return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
	}

	private static String mundur(int hari) {
		return new java.text.SimpleDateFormat("yyyy-MM-dd")
				.format(new java.util.Date(System.currentTimeMillis() - hari * 24L * 3600 * 1000));
	}

	/**
	 * {@code si_stock_count_list} — satu baris per sesi/dokumen opname dalam rentang tanggal.
	 *
	 * <p>Param: {@code dari}/{@code sampai} (default 365 hari terakhir — opname jarang harian,
	 * jadi jendela 30 hari seperti layar persediaan akan tampak kosong pada sebagian besar data
	 * sungguhan), {@code keyword} (nomor dokumen/keterangan), {@code toko_id} (admin),
	 * {@code hanya_berselisih} untuk menyaring sesi yang benar-benar menemukan selisih.</p>
	 */
	public static void stockCountList(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		// Bawaannya PER PRODUK, karena itulah bentuk layar legacy 09: daftar datar satu baris
		// per produk per tanggal dengan satu total di bawah. Mode "sesi" menjawab pertanyaan
		// lain ("opname apa saja yang pernah dilakukan") dan tetap tersedia, tetapi
		// menjadikannya bawaan berarti layar ini menjawab pertanyaan yang tidak ditanyakan
		// layar lamanya.
		String mode = request == null ? "" : request.optString("mode", "").trim().toLowerCase();
		if (!"sesi".equals(mode)) {
			stockCountRows(ctx, request, hasil);
			return;
		}
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int size = request == null ? 20 : request.optInt("page_size", 20);
		if (size < 1) size = 20;
		if (size > 100) size = 100;
		String keyword = request == null || request.isNull("keyword") ? null : request.optString("keyword", "").trim();
		String dari = request == null ? "" : request.optString("dari", "").trim();
		String sampai = request == null ? "" : request.optString("sampai", "").trim();
		if (dari.isEmpty()) dari = mundur(365);
		if (sampai.isEmpty()) sampai = hariIni();
		if (!dari.matches("\\d{4}-\\d{2}-\\d{2}") || !sampai.matches("\\d{4}-\\d{2}-\\d{2}")) {
			hasil.put("status", "91");
			hasil.put("description", "Format tanggal harus yyyy-MM-dd.");
			return;
		}
		Long tokoId = ctx.admin
				? ais.common.Common.angkaAtauNull(request, "toko_id")
				: ctx.tokoId;
		boolean hanyaBerselisih = request != null && request.optBoolean("hanya_berselisih", false);

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			boolean jalurTenant = SalesInventoryOpnameTenant.aktif(ctx);
			java.util.List<Object> params = new java.util.ArrayList<Object>();
			String select;
			if (jalurTenant) {
				String skema = SalesInventoryOpnameTenant.skema(ctx.tenant);
				StringBuilder where = new StringBuilder(
						SalesInventoryOpnameTenant.whereDasar(dari, sampai));
				where.append(SalesInventoryOpnameTenant.syaratTokoOpname(skema, tokoId));
				if (keyword != null && !keyword.isEmpty()) {
					where.append(" AND (COALESCE(o.nomor_dokumen,'') ILIKE ?"
							+ " OR COALESCE(o.keterangan,'') ILIKE ?) ");
					String k = "%" + keyword + "%";
					params.add(k); params.add(k);
				}
				select = SalesInventoryOpnameTenant.selectDaftar(skema, where.toString());
			} else {
				select = selectDaftarLegacy(dari, sampai, tokoId, keyword, params);
			}

			// Saringan "hanya berselisih" ditempatkan DI LUAR agregatnya, bukan di dalam: di
			// jalur legacy ia agregat GROUP BY, dan menyaring agregat di WHERE adalah galat SQL.
			String filterLuar = hanyaBerselisih
					? " WHERE (t.total_lebih <> 0 OR t.total_kurang <> 0) " : "";
			String bungkus = "SELECT * FROM (" + select + ") t" + filterLuar
					+ " ORDER BY t.tanggal DESC, t.nomor DESC";

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
			double sumLebih = 0, sumKurang = 0, sumNilai = 0;
			long sumProduk = 0;
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("nomor", str(rs.getString(2)));
				java.sql.Date tgl = rs.getDate(3);
				j.put("tanggal", tgl == null ? "" : tgl.toString());
				j.put("gudang", str(rs.getString(4)));
				j.put("status", str(rs.getString(5)));
				j.put("keterangan", str(rs.getString(6)));
				j.put("oleh", str(rs.getString(7)));
				long jml = rs.getLong(8);
				double lebih = rs.getDouble(9), kurang = rs.getDouble(10);
				j.put("jumlahProduk", jml);
				j.put("totalLebih", lebih);
				j.put("totalKurang", kurang);
				j.put("selisihBersih", rs.getDouble(11));
				double nilai = rs.getDouble(12);
				j.put("nilaiSelisih", nilai);
				sumProduk += jml; sumLebih += lebih; sumKurang += kurang; sumNilai += nilai;
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
			// Total halaman, BUKAN total seluruh rentang -- dinamai begitu supaya tidak ada layar
			// yang menampilkannya sebagai ringkasan keseluruhan.
			hasil.put("jumlahProdukHalaman", sumProduk);
			hasil.put("totalLebihHalaman", sumLebih);
			hasil.put("totalKurangHalaman", sumKurang);
			hasil.put("nilaiSelisihHalaman", sumNilai);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Daftar sesi jalur legacy: baris datar {@code koperasi.stok_opname} dikelompokkan per
	 * tanggal &times; toko.
	 *
	 * <p>Kolomnya sama dan berurutan sama dengan {@link SalesInventoryOpnameTenant#selectDaftar};
	 * {@code nomor} dibentuk dari tanggalnya karena legacy tidak punya nomor dokumen, dan
	 * {@code gudang} diisi nama tokonya karena di sana yang menampung opname adalah toko.</p>
	 */
	private static String selectDaftarLegacy(String dari, String sampai, Long tokoId, String keyword,
			java.util.List<Object> params) {
		StringBuilder where = new StringBuilder(
				" WHERE o.waktuopname >= DATE '" + dari + "'"
				+ " AND o.waktuopname < (DATE '" + sampai + "' + INTERVAL '1 day') ");
		if (tokoId != null) {
			where.append(" AND o.toko = ? ");
			params.add(tokoId);
		}
		if (keyword != null && !keyword.isEmpty()) {
			where.append(" AND COALESCE(o.keterangan,'') ILIKE ? ");
			params.add("%" + keyword + "%");
		}
		return "SELECT MIN(o.id) AS id,"
				+ " TO_CHAR(DATE(o.waktuopname),'\"SO-\"YYYYMMDD') AS nomor,"
				+ " DATE(o.waktuopname) AS tanggal,"
				+ " COALESCE(MAX(t.nama),'(Tanpa toko)') AS gudang,"
				+ " 'SELESAI' AS status,"
				+ " COALESCE(MAX(o.keterangan),'') AS keterangan,"
				+ " COALESCE(MAX(o.oleh),'') AS oleh,"
				+ " COUNT(*) AS jumlah_produk,"
				+ " COALESCE(SUM(CASE WHEN o.selisih > 0 THEN o.selisih ELSE 0 END),0) AS total_lebih,"
				+ " COALESCE(SUM(CASE WHEN o.selisih < 0 THEN -o.selisih ELSE 0 END),0) AS total_kurang,"
				+ " COALESCE(SUM(o.selisih),0) AS selisih_bersih,"
				+ " COALESCE(SUM(o.selisih * COALESCE(p.hargabeli,0)),0) AS nilai_selisih"
				+ " FROM koperasi.stok_opname o"
				+ " LEFT JOIN koperasi.produk p ON p.id = o.produk"
				+ " LEFT JOIN koperasi.toko t ON t.id = o.toko"
				+ where
				+ " GROUP BY DATE(o.waktuopname), o.toko";
	}

	/**
	 * Mode <b>per produk</b> — padanan langsung layar legacy 09 ("LAPORAN STOK OPNAME").
	 *
	 * <p>Satu baris per produk per tanggal, berkolom sama dengan legacy: TGL.OPNAME, #KODE,
	 * NAMA BARANG, SAT., STOK KOMP., STOK FISIK, SELISIH, HRG.POKOK, TOTAL HARGA.</p>
	 *
	 * <h4>Totalnya dihitung atas SELURUH rentang, bukan halaman aktif</h4>
	 * <p>Legacy menampilkan satu angka di kanan bawah yang menjumlahkan seluruh baris pada
	 * periodenya. Menjumlahkan halaman aktif saja menghasilkan angka yang berubah setiap kali
	 * halaman digeser — terlihat seperti total, berperilaku seperti bukan. Karena itu
	 * totalnya dikueri terpisah dari paginasinya.</p>
	 */
	private static void stockCountRows(EbisnisActorContextResolver.ActorContext ctx,
			JSONObject request, JSONObject hasil) throws Exception {
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int size = request == null ? 20 : request.optInt("page_size", 20);
		if (size < 1) size = 20;
		if (size > 100) size = 100;
		String keyword = request == null || request.isNull("keyword") ? null
				: request.optString("keyword", "").trim();
		String dari = request == null ? "" : request.optString("dari", "").trim();
		String sampai = request == null ? "" : request.optString("sampai", "").trim();
		if (dari.isEmpty()) dari = mundur(365);
		if (sampai.isEmpty()) sampai = hariIni();
		if (!dari.matches("\\d{4}-\\d{2}-\\d{2}") || !sampai.matches("\\d{4}-\\d{2}-\\d{2}")) {
			hasil.put("status", "91");
			hasil.put("description", "Format tanggal harus yyyy-MM-dd.");
			return;
		}
		Long tokoId = ctx.admin
				? ais.common.Common.angkaAtauNull(request, "toko_id")
				: ctx.tokoId;
		boolean hanyaBerselisih = request != null && request.optBoolean("hanya_berselisih", false);

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			boolean jalurTenant = SalesInventoryOpnameTenant.aktif(ctx);
			java.util.List<Object> params = new java.util.ArrayList<Object>();
			String select;
			if (jalurTenant) {
				String skema = SalesInventoryOpnameTenant.skema(ctx.tenant);
				StringBuilder where = new StringBuilder(
						SalesInventoryOpnameTenant.whereDasar(dari, sampai));
				where.append(SalesInventoryOpnameTenant.syaratTokoOpname(skema, tokoId));
				if (keyword != null && !keyword.isEmpty()) {
					where.append(" AND (COALESCE(p.kode,'') ILIKE ?"
							+ " OR COALESCE(p.nama,'') ILIKE ?) ");
					String k = "%" + keyword + "%";
					params.add(k); params.add(k);
				}
				select = SalesInventoryOpnameTenant.selectBarisProduk(skema, where.toString());
			} else {
				select = selectBarisProdukLegacy(dari, sampai, tokoId, keyword, params);
			}

			String filterLuar = hanyaBerselisih ? " WHERE t.selisih <> 0 " : "";
			String bungkus = "SELECT * FROM (" + select + ") t" + filterLuar
					+ " ORDER BY t.tanggal DESC, t.kode ASC";

			// Total SELURUH rentang -- bukan halaman aktif. Lihat javadoc.
			java.sql.PreparedStatement psT = session.connection().prepareStatement(
					"SELECT COUNT(*), COALESCE(SUM(c.selisih),0), COALESCE(SUM(c.nilai),0),"
					+ " COALESCE(SUM(CASE WHEN c.selisih > 0 THEN c.selisih ELSE 0 END),0),"
					+ " COALESCE(SUM(CASE WHEN c.selisih < 0 THEN -c.selisih ELSE 0 END),0)"
					+ " FROM (" + bungkus + ") c");
			for (int i = 0; i < params.size(); i++) psT.setObject(i + 1, params.get(i));
			java.sql.ResultSet rsT = psT.executeQuery();
			long total = 0;
			double selisihTotal = 0, nilaiTotal = 0, lebihTotal = 0, kurangTotal = 0;
			if (rsT.next()) {
				total = rsT.getLong(1);
				selisihTotal = rsT.getDouble(2);
				nilaiTotal = rsT.getDouble(3);
				lebihTotal = rsT.getDouble(4);
				kurangTotal = rsT.getDouble(5);
			}
			rsT.close(); psT.close();

			java.sql.PreparedStatement ps = session.connection()
					.prepareStatement(bungkus + " LIMIT ? OFFSET ?");
			int idx = 1;
			for (int i = 0; i < params.size(); i++) ps.setObject(idx++, params.get(i));
			ps.setInt(idx++, size);
			ps.setInt(idx++, (page - 1) * size);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("idSesi", rs.getLong(1));
				java.sql.Date tgl = rs.getDate(2);
				j.put("tanggal", tgl == null ? "" : tgl.toString());
				j.put("kode", str(rs.getString(3)));
				j.put("nama", str(rs.getString(4)));
				j.put("satuan", str(rs.getString(5)));
				j.put("stokSistem", rs.getDouble(6));
				j.put("stokFisik", rs.getDouble(7));
				j.put("selisih", rs.getDouble(8));
				j.put("harga", rs.getDouble(9));
				j.put("nilai", rs.getDouble(10));
				arr.put(j);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("mode", "produk");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", page);
			hasil.put("pageSize", size);
			hasil.put("dari", dari);
			hasil.put("sampai", sampai);
			// Tanpa akhiran "Halaman": ini memang total seluruh rentang, padanan langsung
			// angka di kanan bawah layar legacy.
			hasil.put("selisihBersih", selisihTotal);
			hasil.put("totalLebih", lebihTotal);
			hasil.put("totalKurang", kurangTotal);
			hasil.put("nilaiSelisih", nilaiTotal);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Baris datar jalur legacy; kolom sama dan berurutan sama dengan jalur tenant. */
	private static String selectBarisProdukLegacy(String dari, String sampai, Long tokoId,
			String keyword, java.util.List<Object> params) {
		StringBuilder where = new StringBuilder(
				" WHERE o.waktuopname >= DATE '" + dari + "'"
				+ " AND o.waktuopname < (DATE '" + sampai + "' + INTERVAL '1 day') ");
		if (tokoId != null) {
			where.append(" AND o.toko = ? ");
			params.add(tokoId);
		}
		if (keyword != null && !keyword.isEmpty()) {
			where.append(" AND (COALESCE(p.kode,'') ILIKE ? OR COALESCE(p.nama,'') ILIKE ?) ");
			String k = "%" + keyword + "%";
			params.add(k); params.add(k);
		}
		return "SELECT o.id AS id_sesi, DATE(o.waktuopname) AS tanggal,"
				+ " COALESCE(p.kode,'') AS kode, COALESCE(p.nama,'') AS nama,"
				+ " COALESCE(NULLIF(TRIM(sp.nama),''),'(Belum diatur)') AS satuan,"
				+ " COALESCE(o.stoksistem,0) AS sistem, COALESCE(o.stokfisik,0) AS fisik,"
				+ " COALESCE(o.selisih,0) AS selisih, COALESCE(p.hargabeli,0) AS harga,"
				+ " COALESCE(o.selisih,0) * COALESCE(p.hargabeli,0) AS nilai"
				+ " FROM koperasi.stok_opname o"
				+ " LEFT JOIN koperasi.produk p ON p.id = o.produk"
				+ " LEFT JOIN koperasi.satuan_produk sp ON sp.id = p.satuan"
				+ where;
	}

	/**
	 * {@code si_stock_count_detail} — rincian satu sesi opname: produk demi produk, stok sistem vs
	 * fisik, selisih, dan nilainya.
	 *
	 * <p>Param: {@code id} wajib (id dokumen tenant, atau id penunjuk sesi pada jalur legacy).</p>
	 */
	public static void stockCountDetail(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		Long id = ais.common.Common.angkaAtauNull(request, "id");
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			boolean jalurTenant = SalesInventoryOpnameTenant.aktif(ctx);
			JSONObject kepala = new JSONObject();
			String sqlRinci;
			java.util.List<Object> params = new java.util.ArrayList<Object>();

			if (jalurTenant) {
				String skema = SalesInventoryOpnameTenant.skema(ctx.tenant);
				java.sql.PreparedStatement psk = session.connection()
						.prepareStatement(SalesInventoryOpnameTenant.selectKepala(skema));
				psk.setLong(1, id.longValue());
				java.sql.ResultSet rsk = psk.executeQuery();
				boolean ada = rsk.next();
				if (ada) {
					kepala.put("id", rsk.getLong(1));
					kepala.put("nomor", str(rsk.getString(2)));
					java.sql.Date t = rsk.getDate(3);
					kepala.put("tanggal", t == null ? "" : t.toString());
					kepala.put("gudang", str(rsk.getString(4)));
					kepala.put("status", str(rsk.getString(5)));
					kepala.put("keterangan", str(rsk.getString(6)));
					kepala.put("oleh", str(rsk.getString(7)));
				}
				rsk.close(); psk.close();
				if (!ada) {
					hasil.put("status", "91");
					hasil.put("description", "Dokumen opname tidak ditemukan.");
					return;
				}
				sqlRinci = SalesInventoryOpnameTenant.selectRinci(skema);
				params.add(Long.valueOf(id.longValue()));
			} else {
				// Jalur legacy: `id` menunjuk SATU BARIS, dan sesinya adalah seluruh baris pada
				// tanggal x toko yang sama. Tanggal dan tokonya karena itu dibaca dulu dari baris
				// penunjuk itu, bukan diterima dari klien -- klien tidak boleh menentukan lingkup
				// baca sendiri.
				java.sql.PreparedStatement psk = session.connection().prepareStatement(
						"SELECT DATE(o.waktuopname), o.toko, COALESCE(MAX(t.nama),'(Tanpa toko)'),"
						+ " COALESCE(MAX(o.keterangan),''), COALESCE(MAX(o.oleh),'')"
						+ " FROM koperasi.stok_opname o LEFT JOIN koperasi.toko t ON t.id = o.toko"
						+ " WHERE o.id = ? GROUP BY DATE(o.waktuopname), o.toko");
				psk.setLong(1, id.longValue());
				java.sql.ResultSet rsk = psk.executeQuery();
				java.sql.Date tgl = null;
				Long toko = null;
				boolean ada = rsk.next();
				if (ada) {
					tgl = rsk.getDate(1);
					long tk = rsk.getLong(2);
					toko = rsk.wasNull() ? null : Long.valueOf(tk);
					kepala.put("id", id.longValue());
					kepala.put("nomor", tgl == null ? "" : "SO-" + tgl.toString().replace("-", ""));
					kepala.put("tanggal", tgl == null ? "" : tgl.toString());
					kepala.put("gudang", str(rsk.getString(3)));
					kepala.put("status", "SELESAI");
					kepala.put("keterangan", str(rsk.getString(4)));
					kepala.put("oleh", str(rsk.getString(5)));
				}
				rsk.close(); psk.close();
				if (!ada) {
					hasil.put("status", "91");
					hasil.put("description", "Sesi opname tidak ditemukan.");
					return;
				}
				sqlRinci = "SELECT o.produk AS produk_id, COALESCE(p.kode,'') AS kode,"
						+ " COALESCE(p.nama,'') AS nama,"
						+ " COALESCE(NULLIF(TRIM(sp.nama),''),'(Belum diatur)') AS satuan,"
						+ " COALESCE(o.stoksistem,0) AS sistem, COALESCE(o.stokfisik,0) AS fisik,"
						+ " COALESCE(o.selisih,0) AS selisih, COALESCE(p.hargabeli,0) AS harga,"
						+ " COALESCE(o.selisih,0) * COALESCE(p.hargabeli,0) AS nilai,"
						+ " COALESCE(o.keterangan,'') AS keterangan"
						+ " FROM koperasi.stok_opname o"
						+ " LEFT JOIN koperasi.produk p ON p.id = o.produk"
						+ " LEFT JOIN koperasi.satuan_produk sp ON sp.id = p.satuan"
						+ " WHERE DATE(o.waktuopname) = ?"
						+ (toko == null ? " AND o.toko IS NULL" : " AND o.toko = ?")
						+ " ORDER BY COALESCE(p.kode,'')";
				params.add(tgl);
				if (toko != null) params.add(toko);
			}

			java.sql.PreparedStatement ps = session.connection().prepareStatement(sqlRinci);
			for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			double sumLebih = 0, sumKurang = 0, sumNilai = 0;
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("produkId", rs.getLong(1));
				j.put("kode", str(rs.getString(2)));
				j.put("nama", str(rs.getString(3)));
				j.put("satuan", str(rs.getString(4)));
				j.put("stokSistem", rs.getDouble(5));
				j.put("stokFisik", rs.getDouble(6));
				double selisih = rs.getDouble(7);
				j.put("selisih", selisih);
				j.put("harga", rs.getDouble(8));
				double nilai = rs.getDouble(9);
				j.put("nilai", nilai);
				j.put("keterangan", str(rs.getString(10)));
				if (selisih > 0) sumLebih += selisih; else sumKurang += -selisih;
				sumNilai += nilai;
				arr.put(j);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("kepala", kepala);
			hasil.put("data", arr);
			hasil.put("jumlahProduk", arr.length());
			hasil.put("totalLebih", sumLebih);
			hasil.put("totalKurang", sumKurang);
			hasil.put("selisihBersih", sumLebih - sumKurang);
			hasil.put("nilaiSelisih", sumNilai);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
}
