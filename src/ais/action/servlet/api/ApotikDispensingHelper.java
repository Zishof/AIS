package ais.action.servlet.api;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.EbisnisMenuKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.sirs.ApotikDispensingLog;
import ais.database.model.sirs.Resep;

/**
 * <h3>Dispensing resep: pemeriksaan kedua &amp; konseling (IR-05).</h3>
 *
 * <p>Menutup gap yang dicatat pada {@code docs/apotik-uiux/02-api-action-map.md}:
 * sebelumnya klien tidak boleh menampilkan tombol double-check/konseling karena
 * tidak ada endpoint yang benar-benar menulis apa pun.</p>
 *
 * <p><b>Aturan keselamatan yang DITEGAKKAN SERVER</b> (bukan sekadar dianjurkan
 * UI):</p>
 * <ul>
 *   <li>pemeriksa kedua WAJIB akun berbeda dari penyiap — inilah inti gunanya
 *       pemeriksaan kedua; klien tidak dapat melewatinya. Lihat
 *       {@link ApotikDispensingLog#getPenyiapUserId()} untuk batas kekuatan
 *       aturan ini: {@code penyiap_user_id} adalah PERNYATAAN pengirim, dan
 *       yang divalidasi server hanyalah bahwa ia merujuk akun {@code Tbmuser}
 *       yang ADA dan AKTIF — bukan bukti siapa yang benar-benar menyiapkan;</li>
 *   <li>satu resep hanya boleh punya SATU catatan aktif per jenis (idempoten:
 *       kiriman ulang mengembalikan catatan yang sudah ada, bukan duplikat);</li>
 *   <li>catatan bersifat append-only: pembatalan ({@code apotik_dispensing_batalkan})
 *       menonaktifkan baris, tidak pernah menghapusnya, sehingga jejak audit
 *       tetap utuh, dan menuntut alasan tertulis;</li>
 *   <li>ketiga aksi ({@code status}/{@code catat}/{@code batalkan}) menjaga
 *       hak akses menu {@code apotik_resep} — baca lewat {@link #bolehLihatMenu}
 *       (disalin dari {@code ApotikApiHelper}, lihat javadocnya), tulis/batal
 *       lewat {@link ApotikApiHelper#bolehAksiMenu} — bukan lagi hanya gerbang
 *       menu kasar {@code PosApi.bolehAksesActionKantin} di dispatcher.</li>
 * </ul>
 *
 * <p><b>Yang SENGAJA tidak dikerjakan di sini:</b> peringatan klinis (alergi,
 * interaksi obat, duplikasi terapi, pemeriksaan dosis) — IR-03. Itu menuntut
 * basis pengetahuan obat yang belum dimiliki sistem; membuat panel "aman"
 * tanpa data sungguhan justru berbahaya karena memberi rasa aman palsu.</p>
 */
public final class ApotikDispensingHelper {

	private ApotikDispensingHelper() {
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	private static String str(String v) {
		return v == null ? "" : v;
	}

	private static Long optLong(JSONObject request, String field) {
		if (request == null || request.isNull(field)) return null;
		try {
			return Long.valueOf((request.get(field) + "").trim());
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean jenisSah(String jenis) {
		return ApotikDispensingLog.JENIS_DOUBLE_CHECK.equals(jenis)
				|| ApotikDispensingLog.JENIS_KONSELING.equals(jenis);
	}

	/**
	 * Gerbang BACA berbasis visibilitas menu -- BUKAN {@code ApotikApiHelper.bolehAksiMenu}.
	 * Kunci {@code apotik_resep} memang ada di {@code EbisnisMenuKatalog.KUNCI_CRUD}, tapi grid
	 * CRUD-nya hanya pernah memuat lima aksi lama ({@code create/update/delete/approve/reject});
	 * "view" bukan salah satunya, jadi memanggil {@code bolehAksiMenu(user, kunci, "view")} akan
	 * SELALU ditolak (fail-closed permanen) utk siapa pun selain admin/role kosong. Yang benar-
	 * benar menandakan pengguna "boleh melihat apotek ini" adalah kunci {@code menu.<kunci>},
	 * default TERTUTUP utk {@code apotik_resep} ({@code EbisnisMenuKatalog.KUNCI_DEFAULT_NONAKTIF})
	 * sampai admin menyalakannya. Pola identik {@code ApotikApiHelper.bolehLihatMenu}; disalin ke
	 * sini (bukan dipanggil dari sana) karena method itu {@code private} di kelasnya.
	 */
	private static boolean bolehLihatMenu(Tbmuser tbmuser, String kunciMenu) {
		if (Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		if (role == null) {
			return true;
		}
		JSONObject menu = EbisnisMenuKatalog.urai(role.getEbisnisMenu()).optJSONObject("menu");
		return menu != null && menu.optBoolean(kunciMenu, false);
	}

	@SuppressWarnings("unchecked")
	private static ApotikDispensingLog cariAktif(Session session, Resep resep, String jenis) {
		List<ApotikDispensingLog> ada = session.createCriteria(ApotikDispensingLog.class)
				.add(Restrictions.eq("resep", resep))
				.add(Restrictions.eq("jenis", jenis))
				.add(Restrictions.eq("aktif", Boolean.TRUE))
				.addOrder(Order.desc("id")).setMaxResults(1).list();
		return ada.isEmpty() ? null : ada.get(0);
	}

	private static JSONObject baris(ApotikDispensingLog log) throws Exception {
		java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		JSONObject j = new JSONObject();
		j.put("id", log.getId());
		j.put("jenis", str(log.getJenis()));
		j.put("pelakuUserId", str(log.getPelakuUserId()));
		j.put("pelakuNama", str(log.getPelakuNama()));
		j.put("penyiapUserId", str(log.getPenyiapUserId()));
		if (!str(log.getPenyiapUserId()).isEmpty()) {
			// Lihat ApotikDispensingLog.getPenyiapUserId(): akunnya kini divalidasi ADA & AKTIF
			// sebelum disimpan, tetapi itu tidak membuktikan akun tsb yang BENAR-BENAR menyiapkan
			// obat ini -- nilainya tetap pernyataan pengirim. UI/laporan jangan menyajikannya
			// sebagai fakta terverifikasi.
			j.put("penyiapKeterangan", "Akun ada & aktif; bukan bukti terverifikasi bahwa akun ini "
					+ "yang benar-benar menyiapkan obat.");
		}
		j.put("catatan", str(log.getCatatan()));
		j.put("aktif", log.getAktif());
		j.put("alasanBatal", str(log.getAlasanBatal()));
		j.put("waktu", log.getWaktu() == null ? "" : fmt.format(log.getWaktu()));
		return j;
	}

	/**
	 * {@code apotik_dispensing_status {resep_id*}} — catatan aktif per resep.
	 * Dipakai UI untuk menentukan langkah berikutnya, bukan untuk menyimpulkan
	 * sendiri apakah pemeriksaan sudah dilakukan.
	 */
	public static void status(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		// Gerbang baca -- sebelumnya endpoint ini sama sekali tidak memeriksa hak apa pun,
		// satu-satunya penyaring adalah gerbang menu kasar PosApi.bolehAksesActionKantin di
		// dispatcher. Pola identik ApotikApiHelper.antreanFarmasiList: bolehAksi(..., "view")
		// akan SELALU ditolak krn "view" tak pernah tersimpan di grid CRUD -- yang benar dipakai
		// utk gerbang baca adalah visibilitas menu.
		if (!bolehLihatMenu(tbmuser, "apotik_resep")) {
			tolak(hasil, "Akun tidak berhak melihat catatan dispensing.");
			return;
		}
		Long resepId = optLong(request, "resep_id");
		if (resepId == null) {
			tolak(hasil, "resep_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Resep resep = (Resep) session.get(Resep.class, resepId);
			if (resep == null) {
				tolak(hasil, "Resep tidak ditemukan.");
				return;
			}
			ApotikDispensingLog cek = cariAktif(session, resep,
					ApotikDispensingLog.JENIS_DOUBLE_CHECK);
			ApotikDispensingLog konseling = cariAktif(session, resep,
					ApotikDispensingLog.JENIS_KONSELING);
			JSONArray arr = new JSONArray();
			if (cek != null) arr.put(baris(cek));
			if (konseling != null) arr.put(baris(konseling));
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("sudahDoubleCheck", cek != null);
			hasil.put("sudahKonseling", konseling != null);
			// Identitas akun berjalan dikirim supaya UI dapat menonaktifkan
			// tombol pemeriksa kedua bila penyiapnya adalah dirinya sendiri --
			// server tetap menolak, ini hanya agar tombolnya tidak menjebak.
			hasil.put("userIdAnda", tbmuser == null ? "" : str(tbmuser.getUserId()));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * {@code apotik_dispensing_catat {resep_id*, jenis* (DOUBLE_CHECK|KONSELING),
	 * penyiap_user_id?, catatan?}}.
	 */
	public static void catat(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (tbmuser == null || tbmuser.getUserId() == null) {
			tolak(hasil, "Sesi tidak dikenali.");
			return;
		}
		// Gerbang tulis -- sebelumnya endpoint ini hanya memeriksa autentikasi (tbmuser != null),
		// bukan otorisasi menu, berbeda dari jalur tetangga (antreanFarmasiSimpan/Status/Hapus)
		// yang konsisten menjaga lewat ApotikApiHelper.bolehAksiMenu.
		if (!ApotikApiHelper.bolehAksiMenu(tbmuser, "apotik_resep", "create")) {
			tolak(hasil, "Akun tidak berhak mencatat pemeriksaan kedua/konseling resep.");
			return;
		}
		Long resepId = optLong(request, "resep_id");
		String jenis = request == null ? "" : request.optString("jenis", "").trim().toUpperCase();
		if (resepId == null || !jenisSah(jenis)) {
			tolak(hasil, "resep_id dan jenis (DOUBLE_CHECK|KONSELING) wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Resep resep = (Resep) session.get(Resep.class, resepId);
			if (resep == null) {
				tolak(hasil, "Resep tidak ditemukan.");
				return;
			}
			// Idempoten: kiriman ulang tidak membuat catatan kedua.
			ApotikDispensingLog ada = cariAktif(session, resep, jenis);
			if (ada != null) {
				hasil.put("status", "00");
				hasil.put("idempotent", true);
				hasil.put("id", ada.getId());
				hasil.put("description", "Catatan " + jenis + " sudah ada untuk resep ini.");
				return;
			}
			String penyiap = request.optString("penyiap_user_id", "").trim();
			if (!penyiap.isEmpty()) {
				// Pagar termurah untuk gap didokumentasikan di ApotikDispensingLog.getPenyiapUserId():
				// penyiap_user_id datang dari PAYLOAD, tidak ada catatan peladen berwenang tentang
				// siapa yang benar-benar menyiapkan obat untuk dibandingkan. Ini TIDAK membuktikan
				// identitas penyiap sungguhan -- hanya menutup kasus paling lugu (nama karangan/typo)
				// dengan memastikan nilainya merujuk akun Tbmuser yang benar-benar ada dan aktif.
				Tbmuser penyiapAkun = (Tbmuser) session.createCriteria(Tbmuser.class)
						.add(Restrictions.ilike("userId", penyiap, MatchMode.EXACT))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
						.setMaxResults(1).uniqueResult();
				if (penyiapAkun == null) {
					tolak(hasil, "penyiap_user_id (" + penyiap + ") bukan akun yang ada dan aktif.");
					return;
				}
			}
			if (ApotikDispensingLog.JENIS_DOUBLE_CHECK.equals(jenis)) {
				if (penyiap.isEmpty()) {
					tolak(hasil, "penyiap_user_id wajib diisi untuk pemeriksaan kedua.");
					return;
				}
				// ATURAN KERAS: pemeriksa kedua harus orang lain. Inilah satu-
				// satunya alasan pemeriksaan kedua ada; UI tidak boleh melewatinya.
				if (penyiap.equalsIgnoreCase(tbmuser.getUserId())) {
					tolak(hasil, "DITOLAK: pemeriksa kedua harus akun yang BERBEDA "
							+ "dari penyiap obat (" + penyiap + ").");
					return;
				}
			}
			ApotikDispensingLog log = new ApotikDispensingLog();
			log.setResep(resep);
			log.setJenis(jenis);
			log.setPenyiapUserId(penyiap.isEmpty() ? null : penyiap);
			log.setPelakuUserId(tbmuser.getUserId());
			log.setPelakuNama(str(tbmuser.getUserNama()));
			log.setCatatan(request.optString("catatan", "").trim());
			log.setAktif(Boolean.TRUE);
			log.setWaktu(new Date());
			log.setOleh(tbmuser.getUserId());
			log.setOlehId(tbmuser.getUserId());
			session.beginTransaction();
			session.save(log);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", log.getId());
			hasil.put("description", ApotikDispensingLog.JENIS_DOUBLE_CHECK.equals(jenis)
					? "Pemeriksaan kedua tercatat atas nama " + tbmuser.getUserId() + "."
					: "Konseling tercatat atas nama " + tbmuser.getUserId() + ".");
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "ApotikDispensingHelper.catat rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * {@code apotik_dispensing_batalkan {id*, alasan*}} -- menonaktifkan catatan (append-only:
	 * baris TIDAK PERNAH dihapus, lihat {@link ApotikDispensingLog#setAktif(Boolean)}).
	 *
	 * <p>Gerbang hak sengaja TERPISAH dari {@link #catat} ("delete", bukan "create") --
	 * mencatat pemeriksaan dan membatalkan catatan pemeriksaan adalah dua wewenang berbeda.
	 * Alasan wajib diisi dan disimpan; siapa yang membatalkan serta kapan direkam otomatis
	 * oleh {@code AuditTimestampInterceptor} lewat kolom audit {@code oleh}/{@code olehId}/
	 * {@code tanggal_dirubah} pada baris yang di-{@code update} -- tidak perlu kolom baru
	 * untuk itu.</p>
	 */
	public static void batalkan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (tbmuser == null || tbmuser.getUserId() == null) {
			tolak(hasil, "Sesi tidak dikenali.");
			return;
		}
		if (!ApotikApiHelper.bolehAksiMenu(tbmuser, "apotik_resep", "delete")) {
			tolak(hasil, "Akun tidak berhak membatalkan catatan dispensing.");
			return;
		}
		Long id = optLong(request, "id");
		String alasan = request == null ? "" : request.optString("alasan", "").trim();
		if (id == null) {
			tolak(hasil, "id wajib diisi.");
			return;
		}
		if (alasan.isEmpty()) {
			tolak(hasil, "alasan pembatalan wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ApotikDispensingLog log = (ApotikDispensingLog) session.get(ApotikDispensingLog.class, id);
			if (log == null) {
				tolak(hasil, "Catatan tidak ditemukan.");
				return;
			}
			if (!Boolean.TRUE.equals(log.getAktif())) {
				hasil.put("status", "00");
				hasil.put("idempotent", true);
				hasil.put("description", "Catatan ini sudah tidak aktif.");
				return;
			}
			log.setAktif(Boolean.FALSE);
			log.setAlasanBatal(alasan);
			log.setOleh(tbmuser.getUserId());
			log.setOlehId(tbmuser.getUserId());
			session.beginTransaction();
			session.update(log);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", log.getId());
			hasil.put("description", "Catatan " + str(log.getJenis()) + " dibatalkan.");
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "ApotikDispensingHelper.batalkan rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Dispatcher aksi {@code apotik_dispensing_*}. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request,
			JSONObject hasil) throws Exception {
		if ("apotik_dispensing_status".equals(action)) { status(tbmuser, request, hasil); return true; }
		if ("apotik_dispensing_catat".equals(action)) { catat(tbmuser, request, hasil); return true; }
		if ("apotik_dispensing_batalkan".equals(action)) { batalkan(tbmuser, request, hasil); return true; }
		return false;
	}
}
