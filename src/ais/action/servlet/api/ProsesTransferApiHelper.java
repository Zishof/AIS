package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.akunting.PostingProsesTransferAction;
import ais.common.Common;
import ais.common.EbisnisMenuKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.CaraPembayaranTransfer;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.akunting.ProsesTransfer;
import ais.database.model.akunting.Transitori;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.WaktuUtil;

/**
 * <h3>API JSON "Proses Transfer" (pencairan DPC) untuk POS Desktop/Android.</h3>
 *
 * <p>Memindahkan {@code ais.action.master.akunting.ProsesTransferAction} — mata rantai
 * TERAKHIR alur Keuangan. Delapan modul yang sudah lebih dulu dipindahkan semuanya
 * bermuara di {@link DaftarPengajuanTransfer} (DPC) lalu berhenti di sana; pencairannya
 * hanya ada di layar ZK. Padahal mesin posting menuntut
 * {@code daftarPengajuanTransfer.prosesTransfer} tidak null — sehingga dokumen yang lahir
 * di POS tidak akan pernah dapat dijurnal tanpa seseorang membuka ZK.</p>
 *
 * <p><b>Empat langkah, sama persis dengan layar ZK:</b></p>
 * <ol>
 * <li>Staf keuangan membuat satu Proses Transfer dan mencentang baris DPC yang akan
 *     dibayarkan; nilainya dijumlahkan otomatis dan kodenya dibuat dari
 *     {@code NomorSuratAlurKeuangan.DPC}.</li>
 * <li>Penyetuju menyetujuinya ({@code disetujuiOleh}, {@code tanggalPersetujuan}).</li>
 * <li>Setelah disetujui, tiap baris ditandai <b>Transfer</b> atau <b>Transitori</b>.</li>
 * <li>Petugas bank menandainya cair ({@code realisasikanOleh},
 *     {@code tanggalRealisasikan}).</li>
 * </ol>
 *
 * <p><b>Kenapa langkah ketiga penting.</b> Tanda Transfer/Transitori pada tiap baris
 * itulah yang menentukan <b>akun kredit jurnalnya</b>: {@code transitori=true} mengkredit
 * {@code caraPembayaranTransfer.akunTransitori}, selain itu mengkredit
 * {@code caraPembayaranTransfer.akun}. Keduanya saling meniadakan — satu baris tidak
 * mungkin keduanya sekaligus.</p>
 *
 * <p><b>Jebakan pada penandanya.</b> {@code DaftarPengajuanTransfer.getTransfer()} dan
 * {@code getTransitori()} adalah getter TERHITUNG yang tidak pernah mengembalikan null
 * (null dipetakan ke {@code false}), dan {@code getTransfer()} bahkan menyetel dirinya
 * sendiri menjadi true bila proses transfernya sudah direalisasi. Karena Hibernate
 * menyimpan nilai GETTER-nya, {@code setTransfer(null)} tidak pernah menghasilkan NULL
 * di basis data — hasilnya {@code false}. Layar ZK menulis null di jalur
 * pembatalannya; di sini ditulis {@code Boolean.FALSE} terang-terangan supaya kodenya
 * menyatakan apa yang sungguh tersimpan. Hasil akhirnya identik.</p>
 *
 * <p><b>Satu penyimpangan sadar dari layar ZK.</b> Penyaring kategori di ZK berpola
 * <i>daftar putih per kolom sumber</i>: kolom yang belum punya cabangnya membuat barisnya
 * TIDAK PERNAH tampil di penyaring mana pun. Cacat itu sudah pernah terjadi pada
 * Reimbursement Pegawai (dan ditambal di sana), dan masih berlaku untuk
 * {@code dana_talangan} serta {@code pertangungjawaban_kas_besar} — dua muara yang dibuat
 * modul-modul terbaru. Di sini kategorinya <b>dihitung dari barisnya</b>, dan baris yang
 * tidak cocok dengan kategori mana pun jatuh ke kategori <b>Lainnya</b>. Dengan begitu
 * layar ini secara struktural tidak mungkin menyembunyikan satu baris pun.</p>
 */
public final class ProsesTransferApiHelper {

	private static final String KUNCI = "proses_transfer";

	private ProsesTransferApiHelper() {
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	/**
	 * <p><b>Catatan keamanan (diverifikasi 2026-09-03, TIDAK diubah sesuai keputusan
	 * eksplisit -- lihat di bawah).</b> {@code role == null} di sini FAIL-OPEN (mengizinkan
	 * penuh), bukan menolak. Ini BUKAN cacat unik file ini: pola identik disalin ke 20+
	 * {@code *ApiHelper.bolehAksi()} lain (mis. {@code MasterKeuanganApiHelper},
	 * {@code PertangungjawabanKasBesarApiHelper}, {@code ClosingApiHelper},
	 * {@code ReimbursementApiHelper}) dan ke gerbang utama
	 * {@code PosApi.bolehAksesActionKantin} sendiri -- satu-satunya pengecualian fail-closed
	 * di seluruh {@code ais.action.servlet.api} adalah {@code HotelApiHelper}. Dilacak sebagai
	 * {@code task_66986071} (kini di 7 helper). Gerbang di sini istimewa dibanding sebagian
	 * saudaranya: menjaga bukan cuma create/update/delete master data, tapi juga
	 * <b>approve/reject/realisasi</b> -- realisasi memicu posting jurnal otomatis
	 * ({@link ais.action.master.akunting.PostingProsesTransferAction#postingSatu}), jadi
	 * fail-open di sini berarti pencairan dana, bukan sekadar perubahan data master.</p>
	 *
	 * <p>{@code role} (hasil {@link Tbmuser#hakAkses()}) null BUKAN berarti "user baru belum
	 * diberi role" (kolom FK-nya {@code nullable=false}), melainkan entitas {@code Tbmrole}
	 * yang dirujuk sudah ter-detach/hilang dari session (cache stale) -- kondisi anomali
	 * cache, bukan alur normal yang bisa dipicu sembarang pengguna terautentikasi kapan pun.</p>
	 *
	 * <p>Karena konvensi ini konsisten &amp; disengaja di seluruh lapisan API Keuangan,
	 * mengubahnya jadi fail-closed HANYA di sini akan (a) menciptakan inkonsistensi perilaku
	 * antar modul yang identik strukturnya, dan (b) berisiko mengunci akun sah yang cache
	 * role-nya sedang stale, tanpa menutup celah yang sama di 6+ file lain maupun di gerbang
	 * {@code PosApi.bolehAksesActionKantin} yang berjalan LEBIH DULU. Perbaikan yang aman
	 * ada di akar penyebab ({@code hakAkses()}/cache tidak boleh diam-diam mengembalikan null
	 * untuk user sah) atau sebagai keputusan produk terpisah yang diterapkan konsisten ke
	 * SELURUH keluarga {@code *ApiHelper} akuntansi/keuangan sekaligus ({@code task_66986071})
	 * -- bukan tambalan sepihak di satu file. Dikonfirmasi ulang &amp; dibiarkan sesuai
	 * keputusan eksplisit pengguna, sesi audit 2026-09-03 (rantai realisasi transfer).</p>
	 */
	private static boolean bolehAksi(Tbmuser tbmuser, String aksi) {
		if (Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		if (role == null) {
			return true;
		}
		return EbisnisMenuKatalog.bolehAksi(EbisnisMenuKatalog.urai(role.getEbisnisMenu()), KUNCI, aksi);
	}

	private static JSONObject hakAksesJson(Tbmuser tbmuser) throws Exception {
		JSONObject j = new JSONObject();
		j.put("create", bolehAksi(tbmuser, "create"));
		j.put("update", bolehAksi(tbmuser, "update"));
		j.put("delete", bolehAksi(tbmuser, "delete"));
		j.put("approve", bolehAksi(tbmuser, "approve"));
		j.put("reject", bolehAksi(tbmuser, "reject"));
		return j;
	}

	private static void batalkanDiam(Session session) {
		try {
			if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception e) {
			// rollback gagal: kegagalan aslinya yang dilaporkan ke pemanggil
		}
	}

	private static Date tanggal(JSONObject request, String kunci) {
		String s = request == null ? "" : request.optString(kunci, "").trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(s.substring(0, 10));
		} catch (Exception e) {
			return null;
		}
	}

	private static String teksTanggal(Date t) {
		return t == null ? "" : new java.text.SimpleDateFormat("yyyy-MM-dd").format(t);
	}

	private static String nama(Tbmuser u) {
		return u == null ? "" : (u.getUserNama() == null ? "" : u.getUserNama());
	}

	// ============================================================ kategori sumber

	/**
	 * Kolom sumber DPC yang dikenal, berurutan. Urutannya menentukan kategori mana yang
	 * menang bila satu baris (secara tidak wajar) mengisi lebih dari satu kolom.
	 *
	 * <p>Nama kolom di sini TIDAK PERNAH datang dari luar — permintaan hanya menyebut
	 * kunci kategori, dan hanya kunci pada daftar ini yang diterima.</p>
	 */
	private static final String[][] KATEGORI = {
			{ "uang_muka", "uang_muka", "Uang Muka" },
			{ "pj_uang_muka", "pertangungjawaban", "Pertanggungjawaban Uang Muka" },
			{ "kas_besar", "kas_besar", "Kas Besar" },
			{ "pj_kas_besar", "pertangungjawaban_kas_besar", "Pertanggungjawaban Kas Besar" },
			{ "kas_kecil", "jenis_kas_kecil", "Kas Kecil" },
			{ "penggantian_kas_kecil", "penggantian_kas_kecil", "Penggantian Kas Kecil" },
			{ "dana_talangan", "dana_talangan", "Dana Talangan" },
			{ "reimbursement", "reimbursement_pegawai", "Reimbursement Pegawai" },
			{ "pajak", "pajak", "Pajak" },
			{ "diskon", "diskon_tagihan", "Diskon Tagihan" },
			{ "termin", "pembayaran_termin_master_asset_detail", "Pengadaan (Termin)" },
			{ "dp", "pembayaran_dp_master_asset_detail", "Pengadaan (DP)" },
			{ "saldo_awal_asset", "saldo_awal_master_asset", "Pengadaan (Saldo Awal)" },
			{ "pengadaan", "pembayaran_pengadaan_master_asset_detail", "Pengadaan" },
			{ "pegawai", "pengajuan_transaksi_pegawai", "Pengajuan Transaksi Pegawai" },
			{ "koperasi", "transaksi_koperasi", "Transaksi Koperasi" },
	};

	/** Kunci semu untuk baris yang tidak mengisi satu pun kolom sumber di atas. */
	private static final String LAINNYA = "lainnya";

	/**
	 * Ekspresi SQL yang memetakan tiap baris ke KUNCI kategorinya. Dibangun dari
	 * {@link #KATEGORI} supaya menambah sumber baru cukup menambah satu baris di sana —
	 * dan baris yang belum dikenal jatuh ke {@code lainnya}, bukan menghilang.
	 */
	private static String ekspresiKategori(String alias) {
		StringBuilder b = new StringBuilder("CASE");
		for (int i = 0; i < KATEGORI.length; i++) {
			b.append(" WHEN ").append(alias).append('.').append(KATEGORI[i][1])
					.append(" IS NOT NULL THEN '").append(KATEGORI[i][0]).append('\'');
		}
		b.append(" ELSE '").append(LAINNYA).append("' END");
		return b.toString();
	}

	private static String labelKategori(String kunci) {
		for (int i = 0; i < KATEGORI.length; i++) {
			if (KATEGORI[i][0].equals(kunci)) {
				return KATEGORI[i][2];
			}
		}
		return "Lainnya";
	}

	private static boolean kategoriSah(String kunci) {
		if (LAINNYA.equals(kunci)) {
			return true;
		}
		for (int i = 0; i < KATEGORI.length; i++) {
			if (KATEGORI[i][0].equals(kunci)) {
				return true;
			}
		}
		return false;
	}

	/** Daftar kunci kategori dari permintaan, sudah disaring daftar putih. */
	private static java.util.List<String> kategoriDiminta(JSONObject request) {
		java.util.List<String> pilih = new java.util.ArrayList<String>();
		JSONArray a = request == null ? null : request.optJSONArray("kategori");
		for (int i = 0; a != null && i < a.length(); i++) {
			String k = a.optString(i, "").trim();
			if (kategoriSah(k) && !pilih.contains(k)) {
				pilih.add(k);
			}
		}
		return pilih;
	}

	// ============================================================ status dokumen

	/**
	 * Tiga status yang benar-benar berbeda perlakuannya, bukan sekadar label:
	 * <b>Draft</b> masih boleh disunting dan barisnya boleh dilepas; <b>Disetujui</b>
	 * mengunci isinya tetapi membuka penandaan Transfer/Transitori; <b>Terealisasi</b>
	 * mengunci semuanya karena dananya sudah cair.
	 */
	private static String statusDokumen(ProsesTransfer pt) {
		if (pt == null) {
			return "";
		}
		if (pt.getRealisasikanOleh() != null) {
			return "Terealisasi";
		}
		if (pt.getDisetujuiOleh() != null) {
			return "Disetujui";
		}
		return "Draft";
	}

	// ============================================================ opsi

	public static void opsi(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			JSONArray cara = new JSONArray();
			PreparedStatement ps = conn.prepareStatement(
					"SELECT c.id, COALESCE(c.nama,''), COALESCE(c.kode,''),"
							+ " ka.kode, ka.nama, kt.kode, kt.nama, COALESCE(c.defaultpembayaran,false)"
							+ " FROM akunting.cara_pembayaran_transfer c"
							+ " LEFT JOIN akunting.akun ka ON ka.id = c.akun"
							+ " LEFT JOIN akunting.akun kt ON kt.id = c.akun_transitori"
							+ " WHERE COALESCE(c.aktif,true) ORDER BY c.nama LIMIT 500");
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("nama", rs.getString(2));
				j.put("kode", rs.getString(3));
				j.put("akun", gabung(rs.getString(4), rs.getString(5)));
				j.put("akunTransitori", gabung(rs.getString(6), rs.getString(7)));
				j.put("bawaan", rs.getBoolean(8));
				// Cara bayar tanpa akun tidak akan pernah menghasilkan jurnal; ditandai di
				// sini supaya ketahuan sebelum dipakai, bukan saat posting diam-diam gagal.
				j.put("akunLengkap", rs.getString(4) != null);
				cara.put(j);
			}
			rs.close();
			ps.close();

			JSONArray satker = new JSONArray();
			ps = conn.prepareStatement("SELECT id, COALESCE(nama,'') FROM rab.satuan_kerja ORDER BY nama LIMIT 500");
			rs = ps.executeQuery();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("nama", rs.getString(2));
				satker.put(j);
			}
			rs.close();
			ps.close();

			JSONArray kategori = new JSONArray();
			for (int i = 0; i < KATEGORI.length; i++) {
				JSONObject j = new JSONObject();
				j.put("kunci", KATEGORI[i][0]);
				j.put("label", KATEGORI[i][2]);
				kategori.put(j);
			}
			JSONObject lain = new JSONObject();
			lain.put("kunci", LAINNYA);
			lain.put("label", "Lainnya");
			// Bukan pelengkap kosmetik: inilah yang membuat baris bersumber baru tetap
			// terlihat alih-alih tersaring diam-diam seperti di layar ZK.
			lain.put("catatan", "Baris DPC yang sumbernya belum dikenali penyaring.");
			kategori.put(lain);

			JSONArray status = new JSONArray();
			status.put("Draft");
			status.put("Disetujui");
			status.put("Terealisasi");

			hasil.put("status", "00");
			hasil.put("caraPembayaran", cara);
			hasil.put("satuanKerja", satker);
			hasil.put("kategori", kategori);
			hasil.put("daftarStatus", status);
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static String gabung(String kode, String nama) {
		String a = kode == null ? "" : kode.trim();
		String b = nama == null ? "" : nama.trim();
		return (a + " " + b).trim();
	}

	// ============================================================ daftar

	/**
	 * <p><b>Catatan keamanan (diverifikasi 2026-09-03, TIDAK diubah).</b> Method ini (dan
	 * {@code detail}/{@code kandidat}/{@code opsi}/{@code dasbor}) TIDAK memanggil
	 * {@link #bolehAksi} -- siapa pun dengan {@code tbmuser} terautentikasi (token API apa
	 * saja) dapat membaca seluruh daftar batch pencairan dana, terlepas dari menu/role-nya.
	 * INI BUKAN cacat unik file ini: {@code daftar()} di SELURUH helper Keuangan lain
	 * ({@code MasterKeuanganApiHelper}, {@code KasKecilApiHelper}, {@code KasBesarApiHelper},
	 * {@code ReimbursementApiHelper}, {@code UangMukaApiHelper}, dst.) punya pola identik:
	 * baca hanya digerbangi autentikasi, bukan hak menu.</p>
	 *
	 * <p><b>Ditambah: nol filter tenant.</b> {@link ais.database.model.akunting.ProsesTransfer}
	 * dan {@link DaftarPengajuanTransfer} TIDAK PUNYA kolom tenant sama sekali (bukan sekadar
	 * predikat yang lupa ditambahkan ke query -- kolomnya memang tidak ada di skema; lihat
	 * Javadoc kelas {@code ProsesTransfer} bagian "Cakupan tenant" untuk detail lengkap).
	 * Query di bawah ({@code daftar}/{@code kandidat}/{@code itemTerpasang}) karena itu
	 * mengembalikan baris SELURUH tenant tanpa kemungkinan menyaringnya di level kode ini.
	 * Menutup gap ini butuh migrasi skema (menambah kolom tenant) di seluruh keluarga
	 * Keuangan sekaligus -- keputusan produk terpisah, bukan tambalan query satu file.
	 * Dikonfirmasi &amp; dibiarkan sesuai keputusan eksplisit pengguna, sesi audit 2026-09-03.</p>
	 */
	public static void daftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String cari = request == null ? "" : request.optString("cari", "").trim();
		String statusFilter = request == null ? "" : request.optString("statusFilter", "").trim();
		long caraId = request == null ? 0 : request.optLong("caraPembayaranId", 0);
		Date dari = tanggal(request, "dari");
		Date sampai = tanggal(request, "sampai");

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			StringBuilder sql = new StringBuilder(
					"SELECT p.id, COALESCE(p.kode,''), COALESCE(p.nama,''), COALESCE(p.keterangan,''),"
							+ " p.tanggal_pembuatan, COALESCE(p.nilai,0), c.nama,"
							+ " du.usernama, p.tanggal_persetujuan, ru.usernama, p.tanggal_realisasikan,"
							+ " (SELECT count(*) FROM akunting.daftar_pengajuan_transfer d"
							+ "  WHERE d.proses_transfer = p.id),"
							+ " COALESCE(p.catatan_persetujuan,''), COALESCE(p.catatan_realisasi,'')"
							+ " FROM akunting.proses_transfer p"
							+ " LEFT JOIN akunting.cara_pembayaran_transfer c ON c.id = p.cara_pembayaran_transfer"
							+ " LEFT JOIN tbmuser du ON du.userid = p.disetujui_oleh"
							+ " LEFT JOIN tbmuser ru ON ru.userid = p.realisasikan_oleh"
							+ " WHERE COALESCE(p.aktif,true)");
			if (!cari.isEmpty()) {
				sql.append(" AND (COALESCE(p.kode,'') ILIKE ? OR COALESCE(p.nama,'') ILIKE ?)");
			}
			if (caraId != 0) {
				sql.append(" AND p.cara_pembayaran_transfer = ?");
			}
			if (dari != null) {
				sql.append(" AND date(p.tanggal_pembuatan) >= ?");
			}
			if (sampai != null) {
				sql.append(" AND date(p.tanggal_pembuatan) <= ?");
			}
			if ("Draft".equals(statusFilter)) {
				sql.append(" AND p.disetujui_oleh IS NULL");
			} else if ("Disetujui".equals(statusFilter)) {
				sql.append(" AND p.disetujui_oleh IS NOT NULL AND p.realisasikan_oleh IS NULL");
			} else if ("Terealisasi".equals(statusFilter)) {
				sql.append(" AND p.realisasikan_oleh IS NOT NULL");
			}
			sql.append(" ORDER BY p.id DESC LIMIT 300");

			PreparedStatement ps = conn.prepareStatement(sql.toString());
			int k = 1;
			if (!cari.isEmpty()) {
				String kw = "%" + cari + "%";
				ps.setString(k++, kw);
				ps.setString(k++, kw);
			}
			if (caraId != 0) {
				ps.setLong(k++, caraId);
			}
			if (dari != null) {
				ps.setDate(k++, new java.sql.Date(dari.getTime()));
			}
			if (sampai != null) {
				ps.setDate(k++, new java.sql.Date(sampai.getTime()));
			}
			ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			double total = 0;
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kode", rs.getString(2));
				j.put("nama", rs.getString(3));
				j.put("keterangan", rs.getString(4));
				j.put("tanggalPembuatan", teksTanggal(rs.getTimestamp(5)));
				double nilai = rs.getDouble(6);
				j.put("nilai", nilai);
				total += nilai;
				j.put("caraPembayaran", rs.getString(7) == null ? "" : rs.getString(7));
				String setuju = rs.getString(8);
				j.put("disetujuiOleh", setuju == null ? "" : setuju);
				j.put("tanggalPersetujuan", teksTanggal(rs.getTimestamp(9)));
				String realisasi = rs.getString(10);
				j.put("realisasikanOleh", realisasi == null ? "" : realisasi);
				j.put("tanggalRealisasikan", teksTanggal(rs.getTimestamp(11)));
				j.put("jumlahItem", rs.getLong(12));
				j.put("catatanPersetujuan", rs.getString(13));
				j.put("catatanRealisasi", rs.getString(14));
				j.put("statusDokumen", realisasi != null ? "Terealisasi" : (setuju != null ? "Disetujui" : "Draft"));
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("totalNilai", total);
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ detail

	public static void detail(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		if (id == 0) {
			tolak(hasil, "Proses transfer belum dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ProsesTransfer pt = (ProsesTransfer) session.get(ProsesTransfer.class, Long.valueOf(id));
			if (pt == null) {
				tolak(hasil, "Proses transfer tidak ditemukan.");
				return;
			}
			JSONObject h = new JSONObject();
			h.put("id", pt.getId());
			h.put("kode", pt.getKode() == null ? "" : pt.getKode());
			h.put("nama", pt.getNama() == null ? "" : pt.getNama());
			h.put("keterangan", pt.getKeterangan() == null ? "" : pt.getKeterangan());
			h.put("nilai", pt.getNilai() == null ? 0 : pt.getNilai().doubleValue());
			h.put("tanggalPembuatan", teksTanggal(pt.getTanggalPembuatan()));
			h.put("caraPembayaranId", pt.getCaraPembayaranTransfer() == null ? JSONObject.NULL
					: pt.getCaraPembayaranTransfer().getId());
			h.put("caraPembayaran", pt.getCaraPembayaranTransfer() == null ? ""
					: (pt.getCaraPembayaranTransfer().getNama() == null ? ""
							: pt.getCaraPembayaranTransfer().getNama()));
			h.put("disetujuiOleh", nama(pt.getDisetujuiOleh()));
			h.put("tanggalPersetujuan", teksTanggal(pt.getTanggalPersetujuan()));
			h.put("catatanPersetujuan", pt.getCatatanPersetujuan() == null ? "" : pt.getCatatanPersetujuan());
			h.put("realisasikanOleh", nama(pt.getRealisasikanOleh()));
			h.put("tanggalRealisasikan", teksTanggal(pt.getTanggalRealisasikan()));
			h.put("catatanRealisasi", pt.getCatatanRealisasi() == null ? "" : pt.getCatatanRealisasi());
			h.put("statusDokumen", statusDokumen(pt));

			hasil.put("status", "00");
			hasil.put("header", h);
			hasil.put("item", itemTerpasang(session, id));
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Baris DPC yang sudah menempel pada satu proses transfer. */
	private static JSONArray itemTerpasang(Session session, long prosesTransferId) throws Exception {
		PreparedStatement ps = session.connection().prepareStatement(
				"SELECT d.id, COALESCE(d.kode,''), COALESCE(d.nama,''), COALESCE(d.nominal,0),"
						+ " COALESCE(d.transfer,false), COALESCE(d.transitori,false), s.nama, "
						+ ekspresiKategori("d")
						+ " FROM akunting.daftar_pengajuan_transfer d"
						+ " LEFT JOIN rab.satuan_kerja s ON s.id = d.satuan_kerja"
						+ " WHERE d.proses_transfer = ? ORDER BY d.id");
		ps.setLong(1, prosesTransferId);
		ResultSet rs = ps.executeQuery();
		JSONArray arr = new JSONArray();
		while (rs.next()) {
			JSONObject j = new JSONObject();
			j.put("id", rs.getLong(1));
			j.put("kode", rs.getString(2));
			j.put("nama", rs.getString(3));
			j.put("nominal", rs.getDouble(4));
			j.put("transfer", rs.getBoolean(5));
			j.put("transitori", rs.getBoolean(6));
			j.put("satuanKerja", rs.getString(7) == null ? "" : rs.getString(7));
			String kat = rs.getString(8);
			j.put("kategori", kat);
			j.put("sumber", labelKategori(kat));
			arr.put(j);
		}
		rs.close();
		ps.close();
		return arr;
	}

	// ============================================================ kandidat

	/**
	 * Baris DPC yang belum ditarik ke proses transfer mana pun. Inilah isi panel pilihan
	 * saat membuat atau menyunting satu proses transfer.
	 */
	public static void kandidat(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String cari = request == null ? "" : request.optString("cari", "").trim();
		long satkerId = request == null ? 0 : request.optLong("satuanKerjaId", 0);
		java.util.List<String> kategori = kategoriDiminta(request);

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder sql = new StringBuilder(
					"SELECT d.id, COALESCE(d.kode,''), COALESCE(d.nama,''), COALESCE(d.nominal,0), s.nama, "
							+ ekspresiKategori("d")
							+ " FROM akunting.daftar_pengajuan_transfer d"
							+ " LEFT JOIN rab.satuan_kerja s ON s.id = d.satuan_kerja"
							+ " WHERE d.proses_transfer IS NULL AND COALESCE(d.aktif,true)");
			if (!cari.isEmpty()) {
				sql.append(" AND (COALESCE(d.kode,'') ILIKE ? OR COALESCE(d.nama,'') ILIKE ?)");
			}
			if (satkerId != 0) {
				sql.append(" AND d.satuan_kerja = ?");
			}
			if (!kategori.isEmpty()) {
				sql.append(" AND ").append(ekspresiKategori("d")).append(" IN (");
				for (int i = 0; i < kategori.size(); i++) {
					sql.append(i == 0 ? "?" : ",?");
				}
				sql.append(')');
			}
			sql.append(" ORDER BY d.id DESC LIMIT 500");

			PreparedStatement ps = session.connection().prepareStatement(sql.toString());
			int k = 1;
			if (!cari.isEmpty()) {
				String kw = "%" + cari + "%";
				ps.setString(k++, kw);
				ps.setString(k++, kw);
			}
			if (satkerId != 0) {
				ps.setLong(k++, satkerId);
			}
			for (int i = 0; i < kategori.size(); i++) {
				ps.setString(k++, kategori.get(i));
			}
			ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			double total = 0;
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kode", rs.getString(2));
				j.put("nama", rs.getString(3));
				double n = rs.getDouble(4);
				j.put("nominal", n);
				total += n;
				j.put("satuanKerja", rs.getString(5) == null ? "" : rs.getString(5));
				String kat = rs.getString(6);
				j.put("kategori", kat);
				j.put("sumber", labelKategori(kat));
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("totalNilai", total);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ simpan

	public static void simpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		boolean baru = id == 0;
		if (!bolehAksi(tbmuser, baru ? "create" : "update")) {
			tolak(hasil, baru ? "Anda tidak memiliki hak membuat proses transfer."
					: "Anda tidak memiliki hak mengubah proses transfer.");
			return;
		}
		// Urutan validasinya disamakan dengan ProsesTransferAction.onSave.
		String judul = request.optString("nama", "").trim();
		if (judul.isEmpty()) {
			tolak(hasil, "Judul Proses Transfer wajib diisi.");
			return;
		}
		JSONArray dptIds = request.optJSONArray("dptIds");
		if (dptIds == null || dptIds.length() == 0) {
			tolak(hasil, "Pilih minimal satu baris DPC yang akan ditransfer.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ProsesTransfer pt = baru ? new ProsesTransfer()
					: (ProsesTransfer) session.get(ProsesTransfer.class, Long.valueOf(id));
			if (pt == null) {
				tolak(hasil, "Proses transfer tidak ditemukan.");
				return;
			}
			if (!baru && pt.getRealisasikanOleh() != null) {
				tolak(hasil, "Proses transfer " + pt.getKode()
						+ " sudah direalisasikan sehingga isinya tidak boleh diubah.");
				return;
			}
			if (!baru && pt.getDisetujuiOleh() != null) {
				tolak(hasil, "Proses transfer " + pt.getKode()
						+ " sudah disetujui. Batalkan persetujuannya lebih dulu bila isinya perlu diubah.");
				return;
			}

			CaraPembayaranTransfer cara = null;
			long caraId = request.optLong("caraPembayaranId", 0);
			if (caraId != 0) {
				cara = (CaraPembayaranTransfer) session.get(CaraPembayaranTransfer.class, Long.valueOf(caraId));
			}
			Date tglBuat = tanggal(request, "tanggalPembuatan");
			if (tglBuat == null) {
				tglBuat = pt.getTanggalPembuatan() == null ? WaktuUtil.getDate() : pt.getTanggalPembuatan();
			}

			session.beginTransaction();
			pt.setNama(judul);
			pt.setKeterangan(request.optString("keterangan", "").trim());
			pt.setCaraPembayaranTransfer(cara);
			pt.setTanggalPembuatan(tglBuat);
			pt.setAktif(Boolean.TRUE);
			Calendar cal = WaktuUtil.getCalendar();
			cal.setTime(tglBuat);
			pt.setTahun(Integer.valueOf(cal.get(Calendar.YEAR)));
			pt.setBulan(Integer.valueOf(cal.get(Calendar.MONTH) + 1));
			if (baru) {
				pt.setNomorSuratAlurKeuangan(NomorSuratAlurKeuangan.DPC);
				pt.setKode(buatKode(session));
			}
			session.saveOrUpdate(pt);
			session.flush();

			// Baris yang DIPILIH ditautkan; yang tadinya menempel tetapi kini tidak lagi
			// dipilih DILEPASKAN -- kalau tidak, baris itu "nyangkut" selamanya di status
			// sudah diajukan dan tidak dapat diproses transfer lain.
			java.util.Set<Long> pilih = new java.util.LinkedHashSet<Long>();
			for (int i = 0; i < dptIds.length(); i++) {
				long v = dptIds.optLong(i, 0);
				if (v != 0) {
					pilih.add(Long.valueOf(v));
				}
			}
			double totalNilai = 0;
			@SuppressWarnings("unchecked")
			java.util.List<DaftarPengajuanTransfer> terpasang = session
					.createCriteria(DaftarPengajuanTransfer.class)
					.add(Restrictions.eq("prosesTransfer", pt)).list();
			for (int i = 0; i < terpasang.size(); i++) {
				DaftarPengajuanTransfer d = terpasang.get(i);
				if (!pilih.contains(d.getId())) {
					d.setProsesTransfer(null);
					d.setTransfer(Boolean.FALSE);
					d.setTransitori(Boolean.FALSE);
					session.update(d);
					selaraskanCatatanTransitori(session, d, false);
				}
			}
			for (java.util.Iterator<Long> it = pilih.iterator(); it.hasNext();) {
				Long v = it.next();
				DaftarPengajuanTransfer d = (DaftarPengajuanTransfer) session
						.get(DaftarPengajuanTransfer.class, v);
				if (d == null) {
					continue;
				}
				if (d.getProsesTransfer() != null && !d.getProsesTransfer().getId().equals(pt.getId())) {
					throw new IllegalStateException("Baris " + d.getKode()
							+ " sudah ditarik ke proses transfer " + d.getProsesTransfer().getKode() + ".");
				}
				d.setProsesTransfer(pt);
				session.update(d);
				totalNilai += d.getNominal() == null ? 0 : d.getNominal().doubleValue();
			}
			pt.setNilai(Double.valueOf(totalNilai));
			session.update(pt);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", pt.getId());
			hasil.put("kode", pt.getKode());
			hasil.put("nilai", totalNilai);
			hasil.put("message", baru ? "Proses transfer " + pt.getKode() + " dibuat."
					: "Proses transfer " + pt.getKode() + " diperbarui.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Proses transfer belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Penomoran memakai alur yang sama dengan layar ZK
	 * ({@code NomorSuratAlurKeuangan.DPC}), termasuk aturan resetnya.
	 */
	private static String buatKode(Session session) {
		try {
			if (NomorSuratAlurKeuangan.DPC == null || NomorSuratAlurKeuangan.DPC.getNomorSurat() == null) {
				return Common.getGeneratedBarCode();
			}
			NomorSurat ns = NomorSuratAlurKeuangan.DPC.getNomorSurat();
			Long index = Boolean.TRUE.equals(ns.getGunakanIndexUrut()) ? ns.getNomorIndex()
					: indexBerikutnya(session, ns);
			NomorSurat.tambahIndexNomorSurat(ns);
			String noAgenda = ns.format(index, WaktuUtil.getDate());
			return ais.action.master.KodeUnikUtil.pastikanUnik(ProsesTransfer.class, noAgenda);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit ProsesTransferApiHelper.buatKode");
			return Common.getGeneratedBarCode();
		}
	}

	/** Nomor urut berikutnya, mengikuti lingkup reset yang dipilih pada nomor suratnya. */
	private static Long indexBerikutnya(Session session, NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return Long.valueOf(0);
		}
		int tahun = WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Criteria c = session.createCriteria(ProsesTransfer.class)
				.createAlias("nomorSuratAlurKeuangan", "nomorSuratAlurKeuangan", Criteria.LEFT_JOIN)
				.createAlias("nomorSuratAlurKeuangan.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN);
		c.add(Boolean.TRUE.equals(nomorSurat.getUrutBerdasarkanNomor())
				? Restrictions.eq("nomorSuratAlurKeuangan.nomorSurat", nomorSurat)
				: (Boolean.TRUE.equals(nomorSurat.getUrutBerdasarkanKelompok())
						&& nomorSurat.getKelompokNomorSurat() != null
								? Restrictions.eq("nomorSurat.kelompokNomorSurat",
										nomorSurat.getKelompokNomorSurat())
								: Restrictions.sqlRestriction("true")));
		c.add(Boolean.TRUE.equals(nomorSurat.getResetUrutanTiapTahun())
				? Restrictions.eq("tahun", Integer.valueOf(tahun))
				: Restrictions.sqlRestriction("true"));
		c.add(Boolean.TRUE.equals(nomorSurat.getResetUrutanTiapBulan())
				? Restrictions.and(Restrictions.eq("tahun", Integer.valueOf(tahun)),
						Restrictions.eq("bulan", Integer.valueOf(bulan)))
				: Restrictions.sqlRestriction("true"));
		c.add(nomorSurat.getResetTiap() != null
				? Restrictions.ge("tanggalPembuatan", nomorSurat.getResetTiap())
				: Restrictions.sqlRestriction("true"));
		Number n = (Number) c.setProjection(Projections.rowCount()).uniqueResult();
		return Long.valueOf((n == null ? 0L : n.longValue()) + 1L);
	}

	// ============================================================ hapus

	public static void hapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "delete")) {
			tolak(hasil, "Anda tidak memiliki hak menghapus proses transfer.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ProsesTransfer pt = id == 0 ? null : (ProsesTransfer) session.get(ProsesTransfer.class, Long.valueOf(id));
			if (pt == null) {
				tolak(hasil, "Proses transfer tidak ditemukan.");
				return;
			}
			if (pt.getRealisasikanOleh() != null) {
				tolak(hasil, "Proses transfer " + pt.getKode()
						+ " sudah direalisasikan sehingga tidak boleh dihapus.");
				return;
			}
			if (pt.getDisetujuiOleh() != null) {
				tolak(hasil, "Proses transfer " + pt.getKode()
						+ " sudah disetujui. Batalkan persetujuannya lebih dulu.");
				return;
			}
			session.beginTransaction();
			// Baris DPC-nya DILEPASKAN lebih dulu, bukan ikut terhapus: dokumen sumbernya
			// masih ada dan berhak diproses transfer lain.
			@SuppressWarnings("unchecked")
			java.util.List<DaftarPengajuanTransfer> nempel = session
					.createCriteria(DaftarPengajuanTransfer.class)
					.add(Restrictions.eq("prosesTransfer", pt)).list();
			for (int i = 0; i < nempel.size(); i++) {
				DaftarPengajuanTransfer d = nempel.get(i);
				d.setProsesTransfer(null);
				d.setTransfer(Boolean.FALSE);
				d.setTransitori(Boolean.FALSE);
				session.update(d);
				selaraskanCatatanTransitori(session, d, false);
			}
			session.flush();
			String kode = pt.getKode();
			session.delete(pt);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("message", "Proses transfer " + kode + " dihapus; "
					+ nempel.size() + " baris DPC dikembalikan ke daftar belum diproses.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Proses transfer belum dapat dihapus: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ persetujuan

	public static void setujui(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "approve")) {
			tolak(hasil, "Anda tidak memiliki hak menyetujui proses transfer.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Date tanggalPersetujuan = tanggal(request, "tanggalPersetujuan");
		String catatanPersetujuan = request == null ? ""
				: request.optString("catatanPersetujuan", "").trim();
		if (tanggalPersetujuan == null) {
			tolak(hasil, "Tanggal disetujui wajib diisi.");
			return;
		}
		if (catatanPersetujuan.length() > 2000) {
			tolak(hasil, "Catatan persetujuan maksimal 2000 karakter.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ProsesTransfer pt = id == 0 ? null : (ProsesTransfer) session.get(ProsesTransfer.class, Long.valueOf(id));
			if (pt == null) {
				tolak(hasil, "Proses transfer tidak ditemukan.");
				return;
			}
			if (pt.getDisetujuiOleh() != null) {
				tolak(hasil, "Proses transfer " + pt.getKode() + " sudah disetujui.");
				return;
			}
			if (pt.getCaraPembayaranTransfer() == null) {
				// Tanpa cara pembayaran, akun kredit jurnalnya tidak dapat ditentukan sama
				// sekali -- lebih baik ditolak di sini daripada gagal diam-diam saat posting.
				tolak(hasil, "Cara Pembayaran Transfer belum dipilih; tanpa itu jurnalnya "
						+ "tidak dapat menentukan akun kredit.");
				return;
			}
			session.beginTransaction();
			pt.setDisetujuiOleh(tbmuser);
			pt.setTanggalPersetujuan(tanggalPersetujuan);
			pt.setCatatanPersetujuan(catatanPersetujuan.length() == 0 ? null : catatanPersetujuan);
			session.update(pt);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("message", "Proses transfer " + pt.getKode() + " disetujui. "
					+ "Tandai tiap baris sebagai Transfer atau Transitori sebelum direalisasikan.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Persetujuan belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void batalSetuju(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "reject")) {
			tolak(hasil, "Anda tidak memiliki hak membatalkan persetujuan proses transfer.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ProsesTransfer pt = id == 0 ? null : (ProsesTransfer) session.get(ProsesTransfer.class, Long.valueOf(id));
			if (pt == null) {
				tolak(hasil, "Proses transfer tidak ditemukan.");
				return;
			}
			if (pt.getDisetujuiOleh() == null) {
				tolak(hasil, "Proses transfer " + pt.getKode() + " belum disetujui.");
				return;
			}
			if (pt.getRealisasikanOleh() != null) {
				tolak(hasil, "Proses transfer " + pt.getKode() + " sudah direalisasikan; "
						+ "batalkan realisasinya lebih dulu.");
				return;
			}
			session.beginTransaction();
			pt.setDisetujuiOleh(null);
			pt.setTanggalPersetujuan(null);
			pt.setCatatanPersetujuan(null);
			session.update(pt);
			// Sama dengan layar ZK: baris yang menempel DIBEBASKAN supaya tidak nyangkut
			// selamanya di status "sudah diajukan". Hanya berlaku selama dananya belum cair.
			@SuppressWarnings("unchecked")
			java.util.List<DaftarPengajuanTransfer> nempel = session
					.createCriteria(DaftarPengajuanTransfer.class)
					.add(Restrictions.eq("prosesTransfer", pt)).list();
			for (int i = 0; i < nempel.size(); i++) {
				DaftarPengajuanTransfer d = nempel.get(i);
				d.setProsesTransfer(null);
				d.setTransfer(Boolean.FALSE);
				d.setTransitori(Boolean.FALSE);
				session.update(d);
				selaraskanCatatanTransitori(session, d, false);
			}
			pt.setNilai(Double.valueOf(0));
			session.update(pt);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("message", "Persetujuan dibatalkan; " + nempel.size()
					+ " baris DPC dikembalikan ke daftar belum diproses.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Pembatalan persetujuan belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ realisasi

	public static void realisasikan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "approve")) {
			tolak(hasil, "Anda tidak memiliki hak merealisasikan proses transfer.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Date tanggalInput = tanggal(request, "tanggalRealisasikan");
		String catatanRealisasi = request == null ? ""
				: request.optString("catatanRealisasi", "").trim();
		if (catatanRealisasi.length() > 2000) {
			tolak(hasil, "Catatan realisasi maksimal 2000 karakter.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		String kode = "";
		boolean sudahDirealisasikan = false;
		boolean lanjutPosting = false;
		Date tanggalUntukPosting = null;
		try {
			ProsesTransfer pt = id == 0 ? null : (ProsesTransfer) session.get(ProsesTransfer.class, Long.valueOf(id));
			if (pt == null) {
				tolak(hasil, "Proses transfer tidak ditemukan.");
				return;
			}
			if (pt.getDisetujuiOleh() == null) {
				tolak(hasil, "Proses transfer " + pt.getKode()
						+ " belum disetujui sehingga belum boleh direalisasikan.");
				return;
			}
			kode = pt.getKode();
			if (pt.getRealisasikanOleh() != null) {
				// Pemanggilan ulang dipakai sebagai mekanisme pemulihan bila realisasi sudah
				// tersimpan tetapi proses posting otomatis sebelumnya sempat terputus.
				sudahDirealisasikan = true;
				lanjutPosting = true;
				tanggalUntukPosting = pt.getTanggalRealisasikan();
			} else {
				if (tanggalInput == null) {
					tolak(hasil, "Tanggal realisasi wajib diisi.");
					return;
				}
				// Baris yang belum ditandai Transfer maupun Transitori tidak menentukan akun
				// kredit apa pun; dokumen sumbernya akan DILEWATI mesin posting tanpa pesan
				// galat. Karena itu realisasi ditahan sampai semuanya bertanda.
				PreparedStatement ps = session.connection().prepareStatement(
					"SELECT count(*) FROM akunting.daftar_pengajuan_transfer"
							+ " WHERE proses_transfer = ? AND COALESCE(transfer,false) = false"
							+ " AND COALESCE(transitori,false) = false");
				ps.setLong(1, id);
				ResultSet rs = ps.executeQuery();
				rs.next();
				long belumBertanda = rs.getLong(1);
				rs.close();
				ps.close();
				if (belumBertanda > 0) {
					tolak(hasil, belumBertanda + " baris belum ditandai Transfer atau Transitori. "
						+ "Tanda itu yang menentukan akun kredit jurnalnya, jadi tanpa tanda "
						+ "dokumen sumbernya tidak akan terjurnal.");
					return;
				}

				session.beginTransaction();
				pt.setRealisasikanOleh(tbmuser);
				pt.setTanggalRealisasikan(tanggalInput);
				pt.setCatatanRealisasi(catatanRealisasi.length() == 0 ? null : catatanRealisasi);
				session.update(pt);
				session.getTransaction().commit();
				lanjutPosting = true;
				tanggalUntukPosting = tanggalInput;
			}
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Realisasi belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		if (!lanjutPosting) {
			return;
		}

		// Session eksplisit di atas sudah ditutup sebelum mesin posting membuka
		// currentNativeSession(). Ini mencegah transaksi menggantung dan memastikan
		// jurnal umum langsung terbentuk pada aksi realisasi yang sama.
		int jumlahJurnal = PostingProsesTransferAction.postingSatu(id, tbmuser,
				tanggalUntukPosting == null ? WaktuUtil.getDate() : tanggalUntukPosting);
		hasil.put("status", "00");
		hasil.put("jumlahJurnal", jumlahJurnal);
		hasil.put("jurnalOtomatis", Boolean.TRUE);
		if (jumlahJurnal > 0) {
			hasil.put("message", "Proses transfer " + kode
					+ " direalisasikan dan " + jumlahJurnal + " jurnal umum dibuat otomatis.");
		} else if (sudahDirealisasikan) {
			hasil.put("message", "Proses transfer " + kode
					+ " sudah direalisasikan dan jurnalnya sudah tercatat.");
		} else {
			hasil.put("message", "Proses transfer " + kode
					+ " direalisasikan. Tidak ada baris baru yang perlu dijurnal.");
		}
	}

	public static void batalRealisasi(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "reject")) {
			tolak(hasil, "Anda tidak memiliki hak membatalkan realisasi proses transfer.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ProsesTransfer pt = id == 0 ? null : (ProsesTransfer) session.get(ProsesTransfer.class, Long.valueOf(id));
			if (pt == null) {
				tolak(hasil, "Proses transfer tidak ditemukan.");
				return;
			}
			if (pt.getRealisasikanOleh() == null) {
				tolak(hasil, "Proses transfer " + pt.getKode() + " belum direalisasikan.");
				return;
			}
			// Aturan layar ZK dipertahankan: hanya pelaksana realisasinya sendiri yang boleh
			// membatalkannya -- pencatat pencairan dana yang tahu apakah dananya benar
			// batal cair. Admin tetap dapat menembusnya bila diperlukan.
			boolean pelaksana = tbmuser != null && tbmuser.getUserId() != null
					&& pt.getRealisasikanOleh().getUserId() != null
					&& tbmuser.getUserId().equals(pt.getRealisasikanOleh().getUserId());
			if (!pelaksana && !Common.getApakahAdminLain(tbmuser)) {
				tolak(hasil, "Realisasi hanya dapat dibatalkan oleh "
						+ nama(pt.getRealisasikanOleh()) + " yang mencatatnya.");
				return;
			}
			Number jumlahSudahPosting = (Number) session.createCriteria(DaftarPengajuanTransfer.class)
					.createAlias("prosesTransfer", "prosesTransfer")
					.add(Restrictions.eq("prosesTransfer.id", Long.valueOf(id)))
					.add(Restrictions.isNotNull("postingHistory"))
					.setProjection(Projections.rowCount()).uniqueResult();
			if (jumlahSudahPosting != null && jumlahSudahPosting.longValue() > 0) {
				tolak(hasil, "Realisasi proses transfer " + pt.getKode()
						+ " sudah membentuk jurnal umum. Batalkan posting jurnalnya terlebih dahulu "
						+ "agar pencairan dan buku besar tetap konsisten.");
				return;
			}
			session.beginTransaction();
			pt.setRealisasikanOleh(null);
			pt.setTanggalRealisasikan(null);
			pt.setCatatanRealisasi(null);
			session.update(pt);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("message", "Realisasi proses transfer " + pt.getKode() + " dibatalkan.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Pembatalan realisasi belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ tanda per baris

	/**
	 * Menandai satu baris DPC sebagai <b>Transfer</b> atau <b>Transitori</b>. Keduanya
	 * saling meniadakan, persis seperti kedua kotak centang di layar ZK yang saling
	 * menyembunyikan.
	 */
	public static void tandaiItem(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "update")) {
			tolak(hasil, "Anda tidak memiliki hak mengubah tanda transfer.");
			return;
		}
		long dptId = request == null ? 0 : request.optLong("dptId", 0);
		String mode = request == null ? "" : request.optString("mode", "").trim();
		if (!"transfer".equals(mode) && !"transitori".equals(mode) && !"kosong".equals(mode)) {
			tolak(hasil, "Tanda tidak dikenali. Pilih transfer, transitori, atau kosong.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			DaftarPengajuanTransfer d = dptId == 0 ? null
					: (DaftarPengajuanTransfer) session.get(DaftarPengajuanTransfer.class, Long.valueOf(dptId));
			if (d == null || d.getProsesTransfer() == null) {
				tolak(hasil, "Baris DPC tidak ditemukan pada proses transfer mana pun.");
				return;
			}
			ProsesTransfer pt = d.getProsesTransfer();
			if (pt.getDisetujuiOleh() == null) {
				tolak(hasil, "Tanda Transfer/Transitori baru dapat diisi setelah proses transfer disetujui.");
				return;
			}
			if (pt.getRealisasikanOleh() != null) {
				tolak(hasil, "Proses transfer " + pt.getKode()
						+ " sudah direalisasikan sehingga tandanya terkunci.");
				return;
			}
			session.beginTransaction();
			d.setTransfer(Boolean.valueOf("transfer".equals(mode)));
			d.setTransitori(Boolean.valueOf("transitori".equals(mode)));
			session.update(d);
			selaraskanCatatanTransitori(session, d, "transitori".equals(mode));
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("message", "kosong".equals(mode) ? "Tanda baris dikosongkan."
					: ("transitori".equals(mode)
							? "Baris ditandai Transitori; jurnalnya mengkredit akun transitori."
							: "Baris ditandai Transfer; jurnalnya mengkredit akun kas/bank."));
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Tanda belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Menyelaraskan CATATAN {@link Transitori} dengan bendera transitori pada barisnya.
	 *
	 * <p>Ini bukan pelengkap: baris {@code akunting.transitori} itulah yang menjadi
	 * kandidat modul Proses Transitori. Tanpa catatan itu, dana yang sudah masuk
	 * rekening transitori tidak punya jalan keluar sama sekali. Layar ZK membuatnya
	 * pada listener kotak centang "Transitori" dan menghapusnya saat centangnya
	 * dilepas; perilaku itu ditiru persis di sini.</p>
	 *
	 * <p>Catatan yang SUDAH masuk satu Proses Transitori tidak dihapus — dananya sudah
	 * diproses keluar, dan menghapusnya akan memutus riwayat batch itu. Dalam keadaan
	 * itu bendera barisnya pun memang tidak seharusnya diubah lagi.</p>
	 */
	private static void selaraskanCatatanTransitori(Session session,
			DaftarPengajuanTransfer d, boolean transitori) {
		Transitori tr = (Transitori) session.createCriteria(Transitori.class)
			.add(Restrictions.eq("daftarPengajuanTransfer", d)).setMaxResults(1).uniqueResult();
		if (transitori) {
			if (tr == null) {
				tr = new Transitori();
				tr.setDaftarPengajuanTransfer(d);
				tr.setNama(d.getNama());
				tr.setKode(d.getKode());
				tr.setAktif(Boolean.TRUE);
				session.save(tr);
				session.flush();
			}
			if (d.getTransitoriData() == null) {
				d.setTransitoriData(tr);
				session.update(d);
			}
		} else if (tr != null) {
			if (tr.getProsesTransitori() != null) {
				return;
			}
			if (d.getTransitoriData() != null) {
				d.setTransitoriData(null);
				session.update(d);
				session.flush();
			}
			session.delete(tr);
			session.flush();
		}
	}

	/** Melepaskan satu baris DPC dari proses transfer; hanya selama belum disetujui. */
	public static void lepasItem(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "update")) {
			tolak(hasil, "Anda tidak memiliki hak mengubah isi proses transfer.");
			return;
		}
		long dptId = request == null ? 0 : request.optLong("dptId", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			DaftarPengajuanTransfer d = dptId == 0 ? null
					: (DaftarPengajuanTransfer) session.get(DaftarPengajuanTransfer.class, Long.valueOf(dptId));
			if (d == null || d.getProsesTransfer() == null) {
				tolak(hasil, "Baris DPC tidak ditemukan pada proses transfer mana pun.");
				return;
			}
			ProsesTransfer pt = d.getProsesTransfer();
			if (pt.getDisetujuiOleh() != null) {
				tolak(hasil, "Proses transfer " + pt.getKode()
						+ " sudah disetujui; barisnya tidak dapat dilepas satu per satu.");
				return;
			}
			session.beginTransaction();
			d.setProsesTransfer(null);
			d.setTransfer(Boolean.FALSE);
			d.setTransitori(Boolean.FALSE);
			session.update(d);
			selaraskanCatatanTransitori(session, d, false);
			session.flush();
			double sisa = 0;
			@SuppressWarnings("unchecked")
			java.util.List<DaftarPengajuanTransfer> nempel = session
					.createCriteria(DaftarPengajuanTransfer.class)
					.add(Restrictions.eq("prosesTransfer", pt)).list();
			for (int i = 0; i < nempel.size(); i++) {
				sisa += nempel.get(i).getNominal() == null ? 0 : nempel.get(i).getNominal().doubleValue();
			}
			pt.setNilai(Double.valueOf(sisa));
			session.update(pt);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("nilai", sisa);
			hasil.put("message", "Baris " + d.getKode() + " dikembalikan ke daftar belum diproses.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Baris belum dapat dilepas: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ dasbor

	public static void dasbor(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			JSONArray kpi = new JSONArray();
			kpi.put(angka(conn, "Draft",
					"SELECT count(*) FROM akunting.proses_transfer WHERE COALESCE(aktif,true) AND disetujui_oleh IS NULL"));
			kpi.put(angka(conn, "Menunggu Realisasi",
					"SELECT count(*) FROM akunting.proses_transfer WHERE COALESCE(aktif,true)"
							+ " AND disetujui_oleh IS NOT NULL AND realisasikan_oleh IS NULL"));
			kpi.put(angka(conn, "Terealisasi",
					"SELECT count(*) FROM akunting.proses_transfer WHERE COALESCE(aktif,true)"
							+ " AND realisasikan_oleh IS NOT NULL"));
			kpi.put(angka(conn, "DPC Belum Diproses",
					"SELECT count(*) FROM akunting.daftar_pengajuan_transfer"
							+ " WHERE proses_transfer IS NULL AND COALESCE(aktif,true)"));

			// Komposisi DPC yang MENGANTRE, per kategori sumber -- inilah yang menunjukkan
			// modul mana yang dananya belum cair.
			JSONArray komposisi = new JSONArray();
			PreparedStatement ps = conn.prepareStatement(
					"SELECT " + ekspresiKategori("d") + " AS kat, count(*), COALESCE(sum(d.nominal),0)"
							+ " FROM akunting.daftar_pengajuan_transfer d"
							+ " WHERE d.proses_transfer IS NULL AND COALESCE(d.aktif,true)"
							+ " GROUP BY 1 ORDER BY 2 DESC");
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("label", labelKategori(rs.getString(1)));
				j.put("nilai", rs.getLong(2));
				j.put("nominal", rs.getDouble(3));
				komposisi.put(j);
			}
			rs.close();
			ps.close();

			// Yang sudah disetujui tetapi belum cair, diurut umur -- antrean inilah yang
			// menahan dokumen sumbernya tidak bisa dijurnal.
			JSONArray daftar = new JSONArray();
			ps = conn.prepareStatement(
					"SELECT COALESCE(p.kode,''), COALESCE(p.nama,''),"
							+ " COALESCE(date_part('day', now() - p.tanggal_persetujuan),0)"
							+ " FROM akunting.proses_transfer p"
							+ " WHERE COALESCE(p.aktif,true) AND p.disetujui_oleh IS NOT NULL"
							+ " AND p.realisasikan_oleh IS NULL"
							+ " ORDER BY p.tanggal_persetujuan LIMIT 20");
			rs = ps.executeQuery();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("kode", rs.getString(1));
				j.put("keterangan", rs.getString(2));
				j.put("umurHari", rs.getLong(3));
				daftar.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("kpi", kpi);
			hasil.put("komposisi", komposisi);
			hasil.put("komposisiJudul", "DPC Mengantre per Sumber");
			hasil.put("daftar", daftar);
			hasil.put("daftarJudul", "Disetujui, Menunggu Pencairan");
			hasil.put("catatanKosong", "Belum ada proses transfer yang menunggu pencairan.");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static JSONObject angka(Connection conn, String label, String sql) throws Exception {
		PreparedStatement ps = conn.prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		rs.next();
		JSONObject j = new JSONObject();
		j.put("label", label);
		j.put("nilai", rs.getLong(1));
		rs.close();
		ps.close();
		return j;
	}

	// ============================================================ dispatcher

	/** Dipakai dispatcher: seluruh aksi berawalan {@code proses_transfer_}. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if ("proses_transfer_opsi".equals(action)) {
			opsi(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transfer_daftar".equals(action)) {
			daftar(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transfer_detail".equals(action)) {
			detail(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transfer_kandidat".equals(action)) {
			kandidat(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transfer_simpan".equals(action)) {
			simpan(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transfer_hapus".equals(action)) {
			hapus(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transfer_setujui".equals(action)) {
			setujui(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transfer_batal_setuju".equals(action)) {
			batalSetuju(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transfer_realisasikan".equals(action)) {
			realisasikan(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transfer_batal_realisasi".equals(action)) {
			batalRealisasi(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transfer_tandai".equals(action)) {
			tandaiItem(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transfer_lepas".equals(action)) {
			lepasItem(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transfer_dasbor".equals(action)) {
			dasbor(tbmuser, request, hasil);
			return true;
		}
		return false;
	}
}
