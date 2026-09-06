package ais.action.master.koperasi.helper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Rincian TRANSAKSI penyusun satu angka laporan -- sumber tunggal untuk popup
 * "Asal Angka" di SELURUH kanal: POS Desktop/Android (aksi
 * {@code laporan_rincian_transaksi} di PosApi), JSP
 * ({@code laporan_laporan_service.jsp}), dan ZKoss ({@code LaporanKantinZkPanel}).
 *
 * <p><b>Kenapa satu tempat.</b> Sebelumnya popup asal-angka hanya mengulang isi
 * baris ringkasan sehingga tidak menjawab "angka ini berasal dari nota mana
 * saja". Ketika drill-down sungguhan ditambahkan, query-nya HARUS satu supaya
 * ketiga kanal tidak pernah menampilkan angka yang berbeda untuk pertanyaan yang
 * sama -- itu jenis perbedaan yang paling sulit dilacak saat rekonsiliasi.</p>
 *
 * <p><b>Kenapa generik.</b> Penyaringnya adalah dimensi APA PUN yang dikirim
 * pemanggil (kode/nama produk, kasir, metode bayar, pelanggan); dimensi yang
 * tidak diisi diabaikan. Dengan begitu satu method melayani seluruh keluarga
 * laporan berbasis transaksi tanpa cabang per laporan, dan laporan baru ikut
 * mendapat drill-down tanpa pekerjaan tambahan.</p>
 */
public final class LaporanRincianTransaksiUtil {

	private LaporanRincianTransaksiUtil() {
	}

	/** Batas bawaan baris yang dikembalikan supaya popup tetap ringan. */
	public static final int BATAS_BAWAAN = 200;
	/** Batas tertinggi yang boleh diminta pemanggil. */
	public static final int BATAS_MAKS = 500;

	/** Dimensi penyaring. Field yang dibiarkan kosong TIDAK ikut menyaring. */
	public static final class Dimensi {
		/** ID header transaksi persis; dipakai laporan omzet detail agar satu baris membuka satu nota. */
		public String idTransaksi;
		public String kodeProduk;
		public String namaProduk;
		public String kasir;
		public String metode;
		/** SALDO atau NON_SALDO; klasifikasinya mengikuti konfigurasi master cara bayar. */
		public String kelompokPembayaran;
		/** Nama toko pada baris rekapan ketika laporan dijalankan untuk Semua Toko. */
		public String toko;
		/** Lingkup tenant server-side; tidak pernah dipercaya dari payload klien. */
		public Long pendaftarId;
		public String pelanggan;
		/** Kode anggota persis; dipakai laporan saldo agar kolom Kode tidak keliru dianggap kode produk. */
		public String kodePelanggan;

		/**
		 * Baris laporan yang pelanggannya berupa label sintetis "Umum / Non-Anggota"
		 * -- bukan nama seseorang, melainkan penanda bahwa transaksinya TIDAK punya
		 * anggota sama sekali. Menyaringnya sebagai nama (ILIKE) tidak akan pernah
		 * cocok, sehingga drill-down baris itu selalu kosong dan pengguna menyimpulkan
		 * datanya tidak ada -- padahal justru baris itu yang paling perlu ditelusuri.
		 */
		public boolean pelangganKosong;

		/** Filter legacy untuk laporan pembayaran sebagian; laporan piutang memakai {@link #hanyaPiutang}. */
		public boolean hanyaBelumLunas;

		/** Hanya transaksi dengan slot pembayaran Kasbon yang efektif sebagai piutang. */
		public boolean hanyaPiutang;

		private static String bersih(String v) {
			return v == null ? "" : v.trim();
		}

		public boolean kosong() {
			return bersih(idTransaksi).length() == 0
					&& bersih(kodeProduk).length() == 0 && bersih(namaProduk).length() == 0
					&& bersih(kasir).length() == 0 && bersih(metode).length() == 0
					&& bersih(kelompokPembayaran).length() == 0 && bersih(toko).length() == 0
					&& bersih(pelanggan).length() == 0 && bersih(kodePelanggan).length() == 0
					&& !pelangganKosong && !hanyaBelumLunas && !hanyaPiutang;
		}
	}

	/** Nominal slot pembayaran pertama; slot pertama tidak mempunyai kolom nominal tersendiri. */
	static String nominalSlotSatu(String headerAlias) {
		return "GREATEST(0, COALESCE(" + headerAlias + ".total_biaya,0)"
				+ " - COALESCE(" + headerAlias + ".nominal_bayar_2,0)"
				+ " - COALESCE(" + headerAlias + ".nominal_bayar_3,0)"
				+ " - COALESCE(" + headerAlias + ".nominal_bayar_4,0)"
				+ " - COALESCE(" + headerAlias + ".nominal_bayar_5,0))";
	}

	/** Cara bayar dianggap Saldo bila otomatis memotong deposit atau ditandai eksplisit demikian. */
	static String syaratSaldo(String caraAlias) {
		return "(COALESCE(" + caraAlias + ".manual,true)=false OR COALESCE("
				+ caraAlias + ".memotong_deposit,false)=true)";
	}

	/** Total porsi Saldo satu nota, termasuk seluruh lima slot split-payment. */
	static String nilaiSaldoNota(String headerAlias, String[] caraAliases) {
		String[] nominal = { nominalSlotSatu(headerAlias),
				"COALESCE(" + headerAlias + ".nominal_bayar_2,0)",
				"COALESCE(" + headerAlias + ".nominal_bayar_3,0)",
				"COALESCE(" + headerAlias + ".nominal_bayar_4,0)",
				"COALESCE(" + headerAlias + ".nominal_bayar_5,0)" };
		StringBuilder out = new StringBuilder("(");
		for (int i = 0; i < caraAliases.length && i < nominal.length; i++) {
			if (i > 0) out.append(" + ");
			out.append("CASE WHEN ").append(syaratSaldo(caraAliases[i])).append(" THEN ")
					.append(nominal[i]).append(" ELSE 0 END");
		}
		return out.append(")").toString();
	}

	/**
	 * Gerbang piutang yang fail-closed: flag master harus aktif DAN kode/nama harus benar-benar
	 * Kasbon. Dengan demikian salah flag pada Voucher/QRIS tidak boleh mencemari piutang.
	 */
	static String syaratKasbon(String caraAlias) {
		return "(COALESCE(" + caraAlias + ".masuk_sebagai_hutang,false)=true"
				+ " AND LOWER(COALESCE(" + caraAlias + ".kode,'') || ' ' || COALESCE("
				+ caraAlias + ".nama,'')) LIKE '%kasbon%')";
	}

	/** Label bisnis yang stabil walau nama master memakai spasi/underscore/ejaan lama. */
	static String labelJenisKasbon(String caraAlias) {
		String sumber = "LOWER(COALESCE(" + caraAlias + ".kode,'') || ' ' || COALESCE("
				+ caraAlias + ".nama,''))";
		return "(CASE WHEN " + sumber + " LIKE '%divisi%' THEN 'Kasbon Divisi'"
				+ " WHEN " + sumber + " LIKE '%pejuang%' THEN 'Kasbon Pejuang'"
				+ " WHEN " + sumber + " LIKE '%operasional%' THEN 'Kasbon Operasional'"
				+ " ELSE COALESCE(NULLIF(TRIM(" + caraAlias + ".nama),''),'Kasbon') END)";
	}

	/** Nilai Kasbon satu nota, termasuk kombinasi sampai lima slot pembayaran. */
	static String nilaiPiutangNota(String headerAlias, String[] caraAliases) {
		String[] nominal = { nominalSlotSatu(headerAlias),
				"COALESCE(" + headerAlias + ".nominal_bayar_2,0)",
				"COALESCE(" + headerAlias + ".nominal_bayar_3,0)",
				"COALESCE(" + headerAlias + ".nominal_bayar_4,0)",
				"COALESCE(" + headerAlias + ".nominal_bayar_5,0)" };
		StringBuilder out = new StringBuilder("(");
		for (int i = 0; i < caraAliases.length && i < nominal.length; i++) {
			if (i > 0) out.append(" + ");
			out.append("CASE WHEN ").append(syaratKasbon(caraAliases[i])).append(" THEN ")
					.append(nominal[i]).append(" ELSE 0 END");
		}
		return out.append(")").toString();
	}

	/** Daftar jenis Kasbon pada satu nota split-payment. */
	static String jenisPiutangNota(String[] caraAliases) {
		StringBuilder out = new StringBuilder("CONCAT_WS(', '");
		for (int i = 0; i < caraAliases.length; i++) {
			out.append(", CASE WHEN ").append(syaratKasbon(caraAliases[i])).append(" THEN ")
					.append(labelJenisKasbon(caraAliases[i])).append(" ELSE NULL END");
		}
		return out.append(")").toString();
	}

	/** Daftar metode pembayaran faktur, termasuk seluruh slot split-payment. */
	static String daftarMetodePembayaranNota(String[] caraAliases) {
		StringBuilder out = new StringBuilder("CONCAT_WS(', '");
		for (int i = 0; i < caraAliases.length; i++) {
			String cara = caraAliases[i];
			out.append(", CASE WHEN ").append(cara).append(".id IS NOT NULL THEN ")
					.append("COALESCE(NULLIF(TRIM(").append(cara).append(".nama),''),")
					.append("NULLIF(TRIM(").append(cara).append(".kode),''),'Metode Pembayaran')")
					.append(" ELSE NULL END");
		}
		return out.append(")").toString();
	}

	static String joinCaraPembayaranNota(String headerAlias, String[] caraAliases) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < caraAliases.length; i++) {
			String sufiks = i == 0 ? "" : "_" + (i + 1);
			out.append(" LEFT JOIN koperasi.cara_pembayaran_koperasi ").append(caraAliases[i])
					.append(" ON ").append(caraAliases[i]).append(".id=").append(headerAlias)
					.append(".cara_pembayaran_koperasi").append(sufiks);
		}
		return out.toString();
	}

	/**
	 * @param session sesi Hibernate aktif (TIDAK ditutup di sini -- pemanggil yang
	 *                membukanya bertanggung jawab menutupnya)
	 * @param tokoId  wajib; membatasi hasil pada satu toko
	 * @return amplop {@code {status, data[], jumlahBaris, totalQty, totalNilai, dibatasi}}
	 */
	public static JSONObject ambil(Session session, Long tokoId, String tglMulai, String tglSampai,
			Dimensi dimensi, int batasDiminta) throws Exception {
		JSONObject hasil = new JSONObject();
		if (tglMulai == null || tglMulai.trim().length() == 0
				|| tglSampai == null || tglSampai.trim().length() == 0) {
			hasil.put("status", "error");
			hasil.put("message", "Rentang tanggal wajib diisi.");
			return hasil;
		}
		int batas = Math.min(BATAS_MAKS, Math.max(20, batasDiminta <= 0 ? BATAS_BAWAAN : batasDiminta));
		Dimensi d = dimensi == null ? new Dimensi() : dimensi;

		StringBuilder w = new StringBuilder(" WHERE 1=1 ");
		List<Object> prm = new ArrayList<Object>();
		if (tokoId != null) {
			w.append(" AND a.toko=? ");
			prm.add(tokoId);
		} else if (d.pendaftarId != null) {
			w.append(" AND EXISTS (SELECT 1 FROM koperasi.toko lingkup_toko"
					+ " WHERE lingkup_toko.id=a.toko AND lingkup_toko.pendaftar=?) ");
			prm.add(d.pendaftarId);
		}
		w.append(" AND DATE(a.waktu)>=CAST(? AS date) AND DATE(a.waktu)<=CAST(? AS date) ");
		prm.add(tglMulai.trim());
		prm.add(tglSampai.trim());
		if (isi(d.idTransaksi)) {
			w.append(" AND CAST(pak.id AS text)=? ");
			prm.add(d.idTransaksi.trim());
		}
		// Kode produk lebih tepat daripada nama, jadi bila keduanya ada kode yang menang.
		if (isi(d.kodeProduk)) {
			w.append(" AND COALESCE(pr.kode,'')=? ");
			prm.add(d.kodeProduk.trim());
		} else if (isi(d.namaProduk)) {
			w.append(" AND COALESCE(NULLIF(TRIM(a.nama),''),COALESCE(pr.nama,'')) ILIKE ? ");
			prm.add(d.namaProduk.trim());
		}
		if (isi(d.kasir)) {
			w.append(" AND COALESCE(pak.kasir_login_nama,'') ILIKE ? ");
			prm.add(d.kasir.trim());
		}
		String[] caraAliases = { "c1", "c2", "c3", "c4", "c5" };
		String daftarMetode = daftarMetodePembayaranNota(caraAliases);
		String nilaiSaldo = nilaiSaldoNota("pak", caraAliases);
		String rasioSaldo = "(CASE WHEN COALESCE(pak.total_biaya,0)>0 THEN"
				+ " LEAST(1.0,GREATEST(0.0,(" + nilaiSaldo
				+ ")/NULLIF(COALESCE(pak.total_biaya,0),0))) ELSE 0.0 END)";
		String faktorPembayaran = "1.0";
		if (isi(d.metode)) {
			w.append(" AND COALESCE(NULLIF(").append(daftarMetode)
					.append(",''),COALESCE(a.carabayar,'')) ILIKE ? ");
			prm.add("%" + d.metode.trim() + "%");
		}
		if ("SALDO".equalsIgnoreCase(d.kelompokPembayaran)) {
			w.append(" AND ").append(nilaiSaldo).append(" > 0 ");
			faktorPembayaran = rasioSaldo;
		} else if ("NON_SALDO".equalsIgnoreCase(d.kelompokPembayaran)) {
			w.append(" AND GREATEST(COALESCE(pak.total_biaya,0)-LEAST(COALESCE(pak.total_biaya,0),")
					.append(nilaiSaldo).append("),0) > 0 ");
			faktorPembayaran = "(1.0-" + rasioSaldo + ")";
		}
		if (isi(d.toko)) {
			w.append(" AND EXISTS (SELECT 1 FROM koperasi.toko tk WHERE tk.id=pak.toko AND COALESCE(tk.nama,'') ILIKE ?) ");
			prm.add(d.toko.trim());
		}
		if (d.pelangganKosong) {
			// Sengaja diperiksa dari FK-nya, bukan dari nama yang kosong: nama bisa
			// saja terisi teks bebas di kolom member sementara anggotanya tetap null,
			// dan yang menentukan baris masuk kelompok "Umum / Non-Anggota" di laporan
			// adalah FK itu.
			w.append(" AND pak.anggota_koperasi IS NULL ");
		} else if (isi(d.kodePelanggan)) {
			w.append(" AND COALESCE(ak.kode,ak.kode_identitas,'')=? ");
			prm.add(d.kodePelanggan.trim());
		} else if (isi(d.pelanggan)) {
			w.append(" AND COALESCE(ak.nama,a.member,'') ILIKE ? ");
			prm.add("%" + d.pelanggan.trim() + "%");
		}
		String nilaiPiutang = nilaiPiutangNota("pak", caraAliases);
		if (d.hanyaPiutang) {
			w.append(" AND ").append(nilaiPiutang).append(" > 0 ");
		} else if (d.hanyaBelumLunas) {
			w.append(" AND (COALESCE(pak.bayar_tunai,0)+COALESCE(pak.bayar_non_tunai,0))"
					+ " < COALESCE(pak.total_biaya,0) ");
		}
		w.append(" AND COALESCE(a.aktif,true)=true ");

		String sql = "SELECT a.waktu, COALESCE(pak.kode,'') nota,"
				+ " COALESCE(NULLIF(TRIM(pak.kasir_login_nama),''),'-') kasir,"
				+ " COALESCE(NULLIF(TRIM(ak.nama),''),NULLIF(TRIM(a.member),''),'Umum') pelanggan,"
				+ " COALESCE(NULLIF(TRIM(a.nama),''),COALESCE(pr.nama,'-')) produk,"
				+ " COALESCE(pr.kode,'') kode_produk,"
				+ " (COALESCE(a.qty,0)*" + faktorPembayaran + ") qty, COALESCE(a.hargasatuan,0) harga,"
				+ " (COALESCE(a.diskon,0)*" + faktorPembayaran + ") diskon,"
				+ " (COALESCE(a.total,0)*" + faktorPembayaran + ") total,"
				+ " COALESCE(NULLIF(" + daftarMetode
				+ ",''),NULLIF(TRIM(a.carabayar),''),'-') metode,"
				+ " " + jenisPiutangNota(caraAliases) + " jenis_piutang,"
				+ " " + nilaiPiutang + " nilai_piutang, CAST(pak.id AS text) id_transaksi"
				+ " FROM koperasi.pembelian a"
				+ " LEFT JOIN koperasi.pembelian_anggota_koperasi pak ON pak.id=a.pembelian_anggota_koperasi"
				+ " LEFT JOIN koperasi.produk pr ON pr.id=a.produk"
				+ " LEFT JOIN koperasi.anggota_koperasi ak ON ak.id=pak.anggota_koperasi"
				+ joinCaraPembayaranNota("pak", caraAliases)
				+ w + " ORDER BY a.waktu DESC LIMIT " + batas;

		Connection conn = session.connection();
		PreparedStatement ps = conn.prepareStatement(sql);
		try {
			for (int i = 0; i < prm.size(); i++) {
				Object v = prm.get(i);
				if (v instanceof Long) {
					ps.setLong(i + 1, ((Long) v).longValue());
				} else {
					ps.setString(i + 1, String.valueOf(v));
				}
			}
			ResultSet rs = ps.executeQuery();
			JSONArray data = new JSONArray();
			double totalQty = 0, totalNilai = 0, totalPiutang = 0;
			Set<String> fakturPiutangTerhitung = new HashSet<String>();
			while (rs.next()) {
				JSONObject o = new JSONObject();
				Timestamp t = rs.getTimestamp(1);
				o.put("waktu", t == null ? "" : t.toString());
				o.put("nota", teks(rs.getString(2)));
				o.put("kasir", teks(rs.getString(3)));
				o.put("pelanggan", teks(rs.getString(4)));
				o.put("produk", teks(rs.getString(5)));
				o.put("kodeProduk", teks(rs.getString(6)));
				o.put("qty", rs.getDouble(7));
				o.put("harga", rs.getDouble(8));
				o.put("diskon", rs.getDouble(9));
				o.put("total", rs.getDouble(10));
				o.put("metode", teks(rs.getString(11)));
				o.put("jenisPiutang", teks(rs.getString(12)));
				double nilaiPiutangFaktur = rs.getDouble(13);
				o.put("nilaiPiutang", nilaiPiutangFaktur);
				String idTransaksi = teks(rs.getString(14));
				o.put("idTransaksi", idTransaksi);
				if (nilaiPiutangFaktur > 0 && fakturPiutangTerhitung.add(idTransaksi)) {
					totalPiutang += nilaiPiutangFaktur;
				}
				totalQty += rs.getDouble(7);
				totalNilai += rs.getDouble(10);
				data.put(o);
			}
			rs.close();
			hasil.put("status", "success");
			hasil.put("data", data);
			hasil.put("jumlahBaris", data.length());
			hasil.put("totalQty", totalQty);
			hasil.put("totalNilai", totalNilai);
			hasil.put("totalPiutang", totalPiutang);
			// Ditandai supaya jumlah baris yang tampil tidak disalahartikan sbg
			// jumlah sebenarnya ketika hasilnya terpotong batas.
			hasil.put("dibatasi", data.length() >= batas);
			return hasil;
		} finally {
			try {
				ps.close();
			} catch (Exception abaikan) {
				ais.common.ErrorAuditUtil.record(abaikan,
						"LaporanRincianTransaksiUtil.ambil: gagal menutup statement");
			}
		}
	}

	private static boolean isi(String v) {
		return v != null && v.trim().length() > 0;
	}

	private static String teks(String v) {
		return v == null ? "" : v;
	}
}
