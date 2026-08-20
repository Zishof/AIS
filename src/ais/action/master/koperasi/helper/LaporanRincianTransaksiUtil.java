package ais.action.master.koperasi.helper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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
		public String kodeProduk;
		public String namaProduk;
		public String kasir;
		public String metode;
		public String pelanggan;

		private static String bersih(String v) {
			return v == null ? "" : v.trim();
		}

		public boolean kosong() {
			return bersih(kodeProduk).length() == 0 && bersih(namaProduk).length() == 0
					&& bersih(kasir).length() == 0 && bersih(metode).length() == 0
					&& bersih(pelanggan).length() == 0;
		}
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
		if (tokoId == null || tglMulai == null || tglMulai.trim().length() == 0
				|| tglSampai == null || tglSampai.trim().length() == 0) {
			hasil.put("status", "error");
			hasil.put("message", "Toko dan rentang tanggal wajib diisi.");
			return hasil;
		}
		int batas = Math.min(BATAS_MAKS, Math.max(20, batasDiminta <= 0 ? BATAS_BAWAAN : batasDiminta));
		Dimensi d = dimensi == null ? new Dimensi() : dimensi;

		StringBuilder w = new StringBuilder(
				" WHERE a.toko=? AND DATE(a.waktu)>=?::date AND DATE(a.waktu)<=?::date ");
		List<Object> prm = new ArrayList<Object>();
		prm.add(tokoId);
		prm.add(tglMulai.trim());
		prm.add(tglSampai.trim());
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
		if (isi(d.metode)) {
			w.append(" AND COALESCE(a.carabayar,'') ILIKE ? ");
			prm.add("%" + d.metode.trim() + "%");
		}
		if (isi(d.pelanggan)) {
			w.append(" AND COALESCE(ak.nama,a.member,'') ILIKE ? ");
			prm.add("%" + d.pelanggan.trim() + "%");
		}

		String sql = "SELECT a.waktu, COALESCE(pak.kode,'') nota,"
				+ " COALESCE(NULLIF(TRIM(pak.kasir_login_nama),''),'-') kasir,"
				+ " COALESCE(NULLIF(TRIM(ak.nama),''),NULLIF(TRIM(a.member),''),'Umum') pelanggan,"
				+ " COALESCE(NULLIF(TRIM(a.nama),''),COALESCE(pr.nama,'-')) produk,"
				+ " COALESCE(pr.kode,'') kode_produk,"
				+ " COALESCE(a.qty,0) qty, COALESCE(a.hargasatuan,0) harga,"
				+ " COALESCE(a.diskon,0) diskon, COALESCE(a.total,0) total,"
				+ " COALESCE(NULLIF(TRIM(a.carabayar),''),'-') metode"
				+ " FROM koperasi.pembelian a"
				+ " LEFT JOIN koperasi.pembelian_anggota_koperasi pak ON pak.id=a.pembelian_anggota_koperasi"
				+ " LEFT JOIN koperasi.produk pr ON pr.id=a.produk"
				+ " LEFT JOIN koperasi.anggota_koperasi ak ON ak.id=pak.anggota_koperasi"
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
			double totalQty = 0, totalNilai = 0;
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
