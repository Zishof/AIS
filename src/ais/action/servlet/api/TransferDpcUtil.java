package ais.action.servlet.api;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.KasBesar;
import ais.database.model.akunting.PenggantianKasKecil;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.akunting.PertangungjawabanKasBesar;
import ais.database.model.akunting.UangMuka;

/**
 * <h3>Muara seluruh dokumen Keuangan: DPC (Daftar Pengajuan Transfer).</h3>
 *
 * <p>Di ZK, dokumen keuangan yang sudah disetujui tidak langsung dibayar. Ia masuk
 * lebih dulu ke <b>Daftar Pengajuan Transfer</b> -- kolam pengajuan yang dipegang
 * bagian keuangan pusat -- lalu dari sana diproses menjadi Proses Transfer. Penautan
 * itu dikerjakan oleh {@code DaftarPengajuanTransfer.simpanXxx(...)}, yang bersifat
 * <b>idempoten</b>: bila dokumen sudah punya baris DPC, ia tidak membuat yang kedua.</p>
 *
 * <p>Kelas ini memindahkan tombol <i>Singkronkan</i> milik layar ZK
 * ({@code SinkronDaftarPengajuanTransferHelper}) menjadi aksi per-dokumen untuk
 * Desktop/Android, dengan <b>gerbang persetujuan yang sama persis</b>:</p>
 * <table border="1">
 * <tr><th>Dokumen</th><th>Syarat</th></tr>
 * <tr><td>Uang Muka</td><td>status Disetujui</td></tr>
 * <tr><td>Kas Besar</td><td>status Disetujui</td></tr>
 * <tr><td>Penggantian Kas Kecil</td><td>status Disetujui <b>dan</b> penyetuju terisi</td></tr>
 * <tr><td>Pertanggungjawaban (LPJ Uang Muka)</td><td>disaring di dalam {@code simpanPertangungjawaban}</td></tr>
 * <tr><td>Pertanggungjawaban Kas Besar</td><td>disaring di dalam {@code simpanPertangungjawabanKasBesar}</td></tr>
 * </table>
 *
 * <p>Kas Kecil sengaja TIDAK punya aksi ini: pengeluaran kas kecil tidak ditransfer
 * satu per satu, uangnya kembali lewat dokumen Penggantian Kas Kecil.</p>
 */
public final class TransferDpcUtil {

	private TransferDpcUtil() {
	}

	/** Nama kolom relasi pada {@code akunting.daftar_pengajuan_transfer} per modul. */
	private static String kolom(String modul) {
		if ("uang_muka".equals(modul)) {
			return "uang_muka";
		}
		if ("pj_uang_muka".equals(modul)) {
			return "pertangungjawaban";
		}
		if ("kas_besar".equals(modul)) {
			return "kas_besar";
		}
		if ("pj_kas_besar".equals(modul)) {
			return "pertangungjawaban_kas_besar";
		}
		if ("penggantian_kas_kecil".equals(modul)) {
			return "penggantian_kas_kecil";
		}
		if ("dana_talangan".equals(modul)) {
			return "dana_talangan";
		}
		return null;
	}

	private static boolean bolehAjukan(Tbmuser tbmuser, String modul) {
		if (ais.common.Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		ais.database.model.Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		if (role == null) {
			return true;
		}
		return ais.common.EbisnisMenuKatalog.bolehAksi(
				ais.common.EbisnisMenuKatalog.urai(role.getEbisnisMenu()), modul, "approve");
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	// ============================================================ ajukan ke DPC

	/**
	 * Masukkan satu dokumen yang sudah disetujui ke daftar pengajuan transfer.
	 *
	 * <p>Aman dipanggil berulang: bila dokumennya sudah tertaut, jawabannya berisi
	 * baris DPC yang sudah ada, bukan baris baru.</p>
	 */
	public static void ajukan(String modul, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		if (id == 0) {
			tolak(hasil, "Dokumen yang akan diajukan belum dipilih.");
			return;
		}
		// Mengajukan ke DPC memindahkan dokumen ke tangan bagian keuangan; haknya
		// disamakan dengan hak MENYETUJUI dokumen itu, bukan sekadar hak melihat.
		if (!bolehAjukan(tbmuser, modul)) {
			tolak(hasil, "Anda tidak memiliki hak mengajukan dokumen ini ke proses transfer.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String masalah = null;
			DaftarPengajuanTransfer dpc = null;

			if ("uang_muka".equals(modul)) {
				UangMuka e = (UangMuka) session.get(UangMuka.class, Long.valueOf(id));
				if (e == null) {
					masalah = "Uang muka tidak ditemukan.";
				} else if (e.getDaftarPengajuanTransfer() != null) {
					dpc = e.getDaftarPengajuanTransfer();
				} else if (!UangMuka.DISETUJU.equals(e.getStatus())) {
					masalah = "Uang muka baru bisa diajukan ke proses transfer setelah disetujui.";
				} else {
					DaftarPengajuanTransfer.simpanUangMuka(e);
					dpc = e.getDaftarPengajuanTransfer();
				}
			} else if ("kas_besar".equals(modul)) {
				KasBesar e = (KasBesar) session.get(KasBesar.class, Long.valueOf(id));
				if (e == null) {
					masalah = "Kas besar tidak ditemukan.";
				} else if (e.getDaftarPengajuanTransfer() != null) {
					dpc = e.getDaftarPengajuanTransfer();
				} else if (!KasBesar.DISETUJU.equals(e.getStatus())) {
					masalah = "Kas besar baru bisa diajukan ke proses transfer setelah disetujui.";
				} else {
					DaftarPengajuanTransfer.simpanKasBesar(e);
					dpc = e.getDaftarPengajuanTransfer();
				}
			} else if ("penggantian_kas_kecil".equals(modul)) {
				PenggantianKasKecil e = (PenggantianKasKecil) session.get(PenggantianKasKecil.class,
						Long.valueOf(id));
				if (e == null) {
					masalah = "Penggantian kas kecil tidak ditemukan.";
				} else if (e.getDaftarPengajuanTransfer() != null) {
					dpc = e.getDaftarPengajuanTransfer();
				} else if (e.getDisetujuiOleh() == null || !PenggantianKasKecil.DISETUJU.equals(e.getStatus())) {
					masalah = "Penggantian kas kecil baru bisa diajukan setelah disetujui.";
				} else {
					DaftarPengajuanTransfer.simpanPenggantianKasKecil(e);
					dpc = e.getDaftarPengajuanTransfer();
				}
			} else if ("dana_talangan".equals(modul)) {
				ais.database.model.akunting.DanaTalangan e = (ais.database.model.akunting.DanaTalangan) session
						.get(ais.database.model.akunting.DanaTalangan.class, Long.valueOf(id));
				if (e == null) {
					masalah = "Dana talangan tidak ditemukan.";
				} else if (e.getDaftarPengajuanTransfer() != null) {
					dpc = e.getDaftarPengajuanTransfer();
				} else if (e.getDisetujuiOleh() == null
						|| !ais.database.model.akunting.DanaTalangan.DISETUJU.equals(e.getStatus())) {
					masalah = "Dana talangan baru bisa diajukan ke proses transfer setelah disetujui.";
				} else {
					DaftarPengajuanTransfer.simpanDanaTalangan(e);
					dpc = e.getDaftarPengajuanTransfer();
				}
			} else if ("pj_uang_muka".equals(modul)) {
				Pertangungjawaban e = (Pertangungjawaban) session.get(Pertangungjawaban.class, Long.valueOf(id));
				if (e == null) {
					masalah = "Pertanggungjawaban tidak ditemukan.";
				} else if (e.getDaftarPengajuanTransfer() != null) {
					dpc = e.getDaftarPengajuanTransfer();
				} else if (e.getDisetujuiOleh() == null) {
					masalah = "Pertanggungjawaban baru bisa diajukan setelah disetujui.";
				} else if (e.getDikembalikan() == null || e.getDikembalikan().doubleValue() <= 0.1) {
					// Yang ditransfer pada LPJ adalah SISA yang harus dikembalikan; kalau
					// realisasinya menghabiskan uang muka, tidak ada apa pun untuk ditransfer.
					masalah = "Tidak ada dana yang harus dikembalikan, jadi tidak perlu ditransfer.";
				} else {
					DaftarPengajuanTransfer.simpanPertangungjawaban(e);
					dpc = e.getDaftarPengajuanTransfer();
					if (dpc == null) {
						masalah = "Pertanggungjawaban ini belum memenuhi syarat untuk ditransfer.";
					}
				}
			} else if ("pj_kas_besar".equals(modul)) {
				PertangungjawabanKasBesar e = (PertangungjawabanKasBesar) session
						.get(PertangungjawabanKasBesar.class, Long.valueOf(id));
				if (e == null) {
					masalah = "Pertanggungjawaban kas besar tidak ditemukan.";
				} else if (e.getDaftarPengajuanTransfer() != null) {
					dpc = e.getDaftarPengajuanTransfer();
				} else if (e.getDisetujuiOleh() == null) {
					masalah = "Pertanggungjawaban kas besar baru bisa diajukan setelah disetujui.";
				} else if (e.getDikembalikan() == null || e.getDikembalikan().doubleValue() <= 0.1) {
					masalah = "Tidak ada dana yang harus dikembalikan, jadi tidak perlu ditransfer.";
				} else {
					DaftarPengajuanTransfer.simpanPertangungjawabanKasBesar(e);
					dpc = e.getDaftarPengajuanTransfer();
					if (dpc == null) {
						masalah = "Pertanggungjawaban kas besar ini belum memenuhi syarat untuk ditransfer.";
					}
				}
			} else {
				masalah = "Modul ini tidak mengenal pengajuan proses transfer.";
			}

			if (masalah != null) {
				tolak(hasil, masalah);
				return;
			}
			hasil.put("status", "00");
			hasil.put("dpcId", dpc == null || dpc.getId() == null ? JSONObject.NULL : dpc.getId());
			hasil.put("dpcKode", dpc == null || dpc.getKode() == null ? "" : dpc.getKode());
			hasil.put("dpcNama", dpc == null || dpc.getNama() == null ? "" : dpc.getNama());
			hasil.put("message", "Dokumen masuk daftar pengajuan transfer"
					+ (dpc == null || dpc.getKode() == null || dpc.getKode().isEmpty() ? "."
							: " " + dpc.getKode() + "."));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ status pada daftar

	/**
	 * Menempelkan status DPC pada tiap baris daftar dokumen, sehingga layar dapat
	 * membedakan "belum diajukan", "menunggu diproses", dan "sudah ditransfer" tanpa
	 * perlu membuka menu Proses Transfer.
	 *
	 * <p>Satu query untuk seluruh halaman, bukan per baris -- daftar bisa berisi
	 * ratusan dokumen.</p>
	 */
	public static void lampirkanStatus(Session session, String modul, JSONArray data) throws Exception {
		String kolom = kolom(modul);
		if (session == null || kolom == null || data == null || data.length() == 0) {
			return;
		}
		StringBuilder ids = new StringBuilder();
		for (int i = 0; i < data.length(); i++) {
			JSONObject b = data.optJSONObject(i);
			if (b == null || b.optLong("id", 0) == 0) {
				continue;
			}
			if (ids.length() > 0) {
				ids.append(',');
			}
			ids.append(b.optLong("id", 0));
		}
		if (ids.length() == 0) {
			return;
		}

		java.util.HashMap petaKode = new java.util.HashMap();
		java.util.HashMap petaProses = new java.util.HashMap();
		PreparedStatement ps = session.connection().prepareStatement(
				"SELECT d." + kolom + ", COALESCE(d.kode,''), d.proses_transfer"
						+ " FROM akunting.daftar_pengajuan_transfer d"
						+ " WHERE d." + kolom + " IN (" + ids + ")");
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			Long kunci = Long.valueOf(rs.getLong(1));
			petaKode.put(kunci, rs.getString(2));
			petaProses.put(kunci, rs.getObject(3) == null ? Boolean.FALSE : Boolean.TRUE);
		}
		rs.close();
		ps.close();

		for (int i = 0; i < data.length(); i++) {
			JSONObject b = data.optJSONObject(i);
			if (b == null) {
				continue;
			}
			Long kunci = Long.valueOf(b.optLong("id", 0));
			boolean adaDpc = petaKode.containsKey(kunci);
			boolean diproses = adaDpc && Boolean.TRUE.equals(petaProses.get(kunci));
			b.put("dpcAda", adaDpc);
			b.put("dpcKode", adaDpc ? String.valueOf(petaKode.get(kunci)) : "");
			b.put("dpcStatus", !adaDpc ? "Belum diajukan" : diproses ? "Sudah ditransfer" : "Menunggu transfer");
		}
	}
}
