package ais.delivery.email.sender;

/*
 * MAILSENDER_IGNORE_NOTIF_CONFIG_FOR_EMAIL_2026_05_30
 * Enhancement:
 * - Pengiriman email tetap berjalan dan tetap membuat record Notifikasi walaupun
 *   konfigurasi "aktfikan_pengiriman_notif" = TIDAK AKTIF.
 * - Pengaturan tersebut tetap dihormati untuk simpanNotif biasa di luar jalur email.
 */


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
//import javax.activation.DataHandler;
//import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.pdfbox.util.PDFMergerUtility;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Messagebox;

import ais.action.servlet.Wa;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Notifikasi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyMessageboxConfig;

public class MailSender {

	// Pembatas konkurensi kirim email/notifikasi. DULU tiap kirim = `new Thread(...).start()` (tak
	// terbatas) → di beban tinggi ribuan raw-thread lahir (snapshot: "total dimulai" belasan ribu,
	// puluhan Thread-#### di MailSender$4.run), masing-masing buka Session+transaksi+koneksi c3p0+audit
	// Envers → pool DB & statement-cache HABIS (BasicResourcePool/GooGooStatementCache antre) + heap
	// tertekan. Sekarang semua dispatch lewat pool TETAP kecil + antrean; saat antrean penuh tugas
	// dijalankan di thread pemanggil (CallerRunsPolicy = backpressure) alih-alih melahirkan thread
	// tanpa batas. Ukuran pool via -Dmail.pool.size (default 4).
	private static final int MAIL_POOL_SIZE;
	static {
		int n = 4;
		try {
			n = Integer.parseInt(System.getProperty("mail.pool.size", "4").trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/delivery/email/sender/MailSender.java:74");
		}
		MAIL_POOL_SIZE = n < 1 ? 1 : n;
	}

	private static final java.util.concurrent.ExecutorService MAIL_POOL = new java.util.concurrent.ThreadPoolExecutor(
			MAIL_POOL_SIZE, MAIL_POOL_SIZE, 60L, java.util.concurrent.TimeUnit.SECONDS,
			new java.util.concurrent.ArrayBlockingQueue<Runnable>(1000), new java.util.concurrent.ThreadFactory() {
				private final java.util.concurrent.atomic.AtomicInteger seq = new java.util.concurrent.atomic.AtomicInteger();

				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "MailSender-worker-" + seq.incrementAndGet());
					t.setDaemon(true);
					return t;
				}
			}, new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

	/**
	 * Menjalankan tugas kirim email/notifikasi pada pool TETAP (pengganti {@code new Thread(...).start()}).
	 * Membungkus tugas agar Session Hibernate thread-local SELALU ditutup setelah selesai — penting karena
	 * thread pool dipakai-ulang (raw-thread lama mati sendiri; thread pool tidak). Bila antrean penuh,
	 * CallerRunsPolicy menjalankan di thread pemanggil (backpressure), bukan melahirkan thread baru.
	 */
	private static void submitEmail(final Runnable r) {
		Runnable wrapped = new Runnable() {
			@Override
			public void run() {
				try {
					r.run();
				} finally {
					try {
						HibernateUtil.closeSession();
					} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/delivery/email/sender/MailSender.java:107");
					}
				}
			}
		};
		try {
			MAIL_POOL.execute(wrapped);
		} catch (Throwable t) {
			// Fail-safe: jangan sampai gagal-dispatch menelan email — jalankan langsung.
			wrapped.run();
		}
	}

	public static void sendMail(JSONArray userIds, String subject, String body, String sender, String recipients,
			GeneralValueObject dataObject, boolean kirimkankeWa) throws Exception {
		JSONArray attachmentsData = null;
		sendMail(userIds, subject, body, sender, recipients, dataObject, attachmentsData, kirimkankeWa, null);
	}

	public static void sendMail(JSONArray userIds, String subject, String body, String sender, String recipients,
			GeneralValueObject dataObject, PrintStream out, boolean kirimkankeWa) throws Exception {
		File file = null;
		JSONArray attachmentsData = null;
		sendMailLampiran(userIds, subject, body, sender, recipients, out, dataObject, attachmentsData, kirimkankeWa,
				file);
	}

	public static void sendMailLampiran(JSONArray userIds, String subject, String body, String sender,
			String recipientsTemp, GeneralValueObject dataObject, boolean kirimkankeWa, File... file) throws Exception {
		JSONArray attachmentsData = null;
		sendMailLampiran(userIds, subject, body, sender, recipientsTemp, null, dataObject, attachmentsData,
				kirimkankeWa, file);
	}

	public static void sendMail(JSONArray userIds, String subject, String body, String sender, String recipients,
			GeneralValueObject dataObject, JSONArray attachmentsData, boolean kirimkankeWa) throws Exception {
		sendMail(userIds, subject, body, sender, recipients, dataObject, attachmentsData, kirimkankeWa, null);
	}

	public static void sendMail(JSONArray userIds, String subject, String body, String sender, String recipients,
			GeneralValueObject dataObject, JSONArray attachmentsData, boolean kirimkankeWa, PrintStream out)
			throws Exception {
		File file = null;
		// recipients = recipients.replaceAll("@.", "@");
		sendMailLampiran(userIds, subject, body, sender, recipients, out, dataObject, attachmentsData, kirimkankeWa,
				file);
	}

	public static void sendMailLampiran(JSONArray userIds, String subject, String body, String sender,
			String recipientsTemp, GeneralValueObject dataObject, JSONArray attachmentsData, boolean kirimkankeWa,
			File... file) throws Exception {
		sendMailLampiran(userIds, subject, body, sender, recipientsTemp, null, dataObject, attachmentsData,
				kirimkankeWa, file);
	}

	public static void sendMail(JSONArray userIds, String subject, String body, String sender, String recipients,
			GeneralValueObject dataObject) throws Exception {
		JSONArray attachmentsData = null;
		sendMail(userIds, subject, body, sender, recipients, dataObject, attachmentsData, null);
	}

	public static void sendMail(JSONArray userIds, String subject, String body, String sender, String recipients,
			GeneralValueObject dataObject, PrintStream out) throws Exception {
		File file = null;
		JSONArray attachmentsData = null;
		sendMailLampiran(userIds, subject, body, sender, recipients, out, dataObject, attachmentsData, false, file);
	}

	public static void sendMailLampiran(JSONArray userIds, String subject, String body, String sender,
			String recipientsTemp, GeneralValueObject dataObject, File... file) throws Exception {
		JSONArray attachmentsData = null;
		sendMailLampiran(userIds, subject, body, sender, recipientsTemp, null, dataObject, attachmentsData, false,
				file);
	}

	public static void sendMail(JSONArray userIds, String subject, String body, String sender, String recipients,
			GeneralValueObject dataObject, JSONArray attachmentsData) throws Exception {
		sendMail(userIds, subject, body, sender, recipients, dataObject, attachmentsData, null);
	}

	public static void sendMail(JSONArray userIds, String subject, String body, String sender, String recipients,
			GeneralValueObject dataObject, JSONArray attachmentsData, PrintStream out) throws Exception {
		File file = null;
		// recipients = recipients.replaceAll("@.", "@");
		sendMailLampiran(userIds, subject, body, sender, recipients, out, dataObject, attachmentsData, false, file);
	}

	public static void sendMailLampiran(JSONArray userIds, String subject, String body, String sender,
			String recipientsTemp, GeneralValueObject dataObject, JSONArray attachmentsData, File... file)
			throws Exception {
		sendMailLampiran(userIds, subject, body, sender, recipientsTemp, null, dataObject, attachmentsData, false,
				file);
	}

	public static Set<String> notifSudah = new HashSet<String>();

	public static Notifikasi simpanNotif(JSONArray userIdsTemp, String recipientsTemp, String subject, String body,
			GeneralValueObject dataObject, File... temp) {
		JSONArray attachmentsData = null;
		return simpanNotif(userIdsTemp, recipientsTemp, subject, body, dataObject, attachmentsData, false, temp);
	}

	public static Notifikasi simpanNotif(JSONArray userIdsTemp, String recipientsTemp, String subject, String body,
			GeneralValueObject dataObject, boolean kirimkankeWa, File... temp) {
		JSONArray attachmentsData = null;
		return simpanNotif(userIdsTemp, recipientsTemp, subject, body, dataObject, attachmentsData, kirimkankeWa, temp);
	}

	public static Notifikasi simpanNotif(JSONArray userIdsTemp, String recipientsTemp, String subject, String body,
			GeneralValueObject dataObject, JSONArray attachmentsData, boolean kirimkankeWa, File... temp) {
		return simpanNotifInternal(userIdsTemp, recipientsTemp, subject, body, dataObject, attachmentsData, kirimkankeWa,
				false, temp);
	}

	/**
	 * Menyimpan notifikasi terstruktur yang dapat <b>diklik untuk membuka halaman
	 * terkait</b>, sekaligus (opsional) mengirim email bila konfigurasi email aktif.
	 *
	 * <p>
	 * Berbeda dari {@link #simpanNotif}, method ini:
	 * </p>
	 * <ul>
	 * <li><b>Selalu</b> membuat record {@link Notifikasi} terlebih dahulu — tidak
	 * tergantung pada konfigurasi pengaktifan email. Jadi walaupun email dimatikan,
	 * pemberitahuan tetap tersimpan dan muncul di lonceng/pusat notifikasi.</li>
	 * <li>Menyisipkan {@code classData} ke dalam {@code keterangan} (agar notifikasi
	 * tampil pada lonceng yang menyaring {@code keterangan ilike '%classData%'}).</li>
	 * <li>Menyimpan URL tujuan klik ke kolom
	 * {@link Notifikasi#getJikaDiKlikBukaHalamanZKoss()} /
	 * {@link Notifikasi#getJikaDiKlikBukaHalamanJSp()} <i>dan</i> menyalinnya ke
	 * {@code keterangan} ({@code bukaZk}/{@code bukaJsp}) supaya konsumen yang hanya
	 * membaca proyeksi {@code keterangan} (lonceng ZK) tetap bisa membuka halaman.</li>
	 * <li>Mengirim push (GCP) / WhatsApp lewat {@link #kirimNotif} tanpa menimpa
	 * {@code keterangan} yang sudah diperkaya.</li>
	 * <li>Bila ada penerima email, mengirim email — gating tetap dihormati di
	 * {@link #sendMailProcess} (mis. {@code aktfikan_pengiriman_email}).</li>
	 * </ul>
	 *
	 * @param userIdsTemp   daftar user id penerima (NIM / NIS / userId dosen-guru)
	 * @param recipientsTemp daftar email penerima (dipisah koma), boleh kosong/null
	 * @param subject       judul pemberitahuan
	 * @param body          isi pemberitahuan (HTML)
	 * @param sender        alamat pengirim email
	 * @param dataObject    objek terkait (untuk classData), boleh null
	 * @param bukaZk        URL/halaman ZKoss yang dibuka saat diklik, boleh null
	 * @param bukaJsp       URL/halaman JSP yang dibuka saat diklik, boleh null
	 * @param statusNotif   tipe notifikasi (mis. INFO/WARNING/DANGER), boleh null
	 * @param akademik      tandai email akademik (gate aktfikan_pengiriman_email_akademik)
	 * @param tagihan       tandai email tagihan (gate aktfikan_pengiriman_email_tagihan)
	 * @param kirimkankeWa  kirim juga ke WhatsApp bila true
	 * @param temp          lampiran opsional
	 * @return record {@link Notifikasi} yang tersimpan, atau null bila tidak ada penerima
	 */
	public static Notifikasi simpanNotifikasiHalaman(JSONArray userIdsTemp, String recipientsTemp, String subject,
			String body, String sender, GeneralValueObject dataObject, String bukaZk, String bukaJsp,
			String statusNotif, boolean akademik, boolean tagihan, boolean kirimkankeWa, File... temp) {

		// Formalkan isi pesan (bahasa sangat resmi + panjang minimal) sebelum disimpan/
		// dikirim. Idempoten: bila body sudah diformalkan dari hulu, dikembalikan apa adanya.
		// Notifikasi adalah sumber yang juga dipakai ulang untuk email & WhatsApp.
		if (ais.common.FormalisasiPesanUtil.terapkanNotifikasi()) {
			body = ais.common.FormalisasiPesanUtil.bungkusFormalHtml(subject, body);
		}

		final File[] file = jadikanSatuFilePdf(temp);

		// Dedup penerima berbasis (objek + subject + user) agar tidak dobel kirim.
		JSONArray userIds = null;
		if (userIdsTemp != null) {
			if (dataObject != null && dataObject.getId() != null) {
				userIds = new JSONArray();
				for (int i = 0; i < userIdsTemp.length(); i++) {
					try {
						String d = userIdsTemp.getString(i);
						if (!d.trim().isEmpty()) {
							String key = "HAL_" + dataObject.getClass().getSimpleName() + "_" + dataObject.getId() + "_"
									+ (subject == null ? "" : subject.hashCode()) + "_" + d;
							if (!notifSudah.contains(key)) {
								userIds.put(d);
								notifSudah.add(key);
							}
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/delivery/email/sender/MailSender.java:290");
					}
				}
			} else {
				userIds = userIdsTemp;
			}
		}

		if (recipientsTemp == null && (userIds == null || userIds.length() == 0)) {
			return null;
		}

		// classData selalu dibuat agar notifikasi lolos filter lonceng (ilike classData).
		JSONObject classData = new JSONObject();
		try {
			if (dataObject != null) {
				classData.put("name", dataObject.getClass().getName().split("_")[0]);
				if (dataObject instanceof Tbmuser) {
					classData.put("id", ((Tbmuser) dataObject).getUserId());
				} else if (dataObject instanceof Tbmrole) {
					classData.put("id", ((Tbmrole) dataObject).getRoleId());
				} else {
					classData.put("id", dataObject.getId());
				}
				classData.put("object_kode", dataObject.getKode() == null ? "" : dataObject.getKode());
				classData.put("object_name", dataObject.getNama() == null ? "" : dataObject.getNama());
				classData.put("object_keterangan", dataObject.getKeterangan() == null ? "" : dataObject.getKeterangan());
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/delivery/email/sender/MailSender.java:319");
		}

		// Bila tujuan klik versi JSP tidak diberikan TAPI tujuan ZKoss berupa halaman .zul,
		// turunkan otomatis rute JSP dari path .zul tersebut (konvensi modul JSP:
		// "/a/b/c.zul" -> "/baru?p=abczul&s=index"). Dengan begitu notifikasi yang sama
		// dapat langsung dibuka di antarmuka JSP maupun ZKoss tanpa konfigurasi ganda.
		String bukaJspFinal = bukaJsp;
		if ((bukaJspFinal == null || bukaJspFinal.trim().isEmpty()) && bukaZk != null
				&& bukaZk.trim().toLowerCase().endsWith(".zul")) {
			bukaJspFinal = "/baru?p=" + bukaZk.trim().replaceAll("[^A-Za-z0-9]", "") + "&s=index";
		}

		Notifikasi notifikasi = new Notifikasi();
		try {
			notifikasi.setNama(userIds == null ? null : userIds.toString());

			if (recipientsTemp != null) {
				JSONArray jsonArray = new JSONArray();
				for (String s : recipientsTemp.split(",")) {
					jsonArray.put(s);
				}
				notifikasi.setEmails(jsonArray.toString());
			}

			JSONObject jsonObject = new JSONObject();
			jsonObject.put("subject", subject);
			jsonObject.put("body", body);
			jsonObject.put("classData", classData);
			if (bukaZk != null && !bukaZk.trim().isEmpty()) {
				jsonObject.put("bukaZk", bukaZk.trim());
			}
			if (bukaJspFinal != null && !bukaJspFinal.trim().isEmpty()) {
				jsonObject.put("bukaJsp", bukaJspFinal.trim());
			}
			notifikasi.setKeterangan(jsonObject.toString());

			if (bukaZk != null && !bukaZk.trim().isEmpty()) {
				notifikasi.setJikaDiKlikBukaHalamanZKoss(bukaZk.trim());
			}
			if (bukaJspFinal != null && !bukaJspFinal.trim().isEmpty()) {
				notifikasi.setJikaDiKlikBukaHalamanJSp(bukaJspFinal.trim());
			}
			if (statusNotif != null && !statusNotif.trim().isEmpty()) {
				notifikasi.setStatusNotif(statusNotif.trim());
			}

			org.hibernate.Session session2 = HibernateUtil.currentNativeSession();
			session2.getTransaction().begin();
			session2.save(notifikasi);
			session2.getTransaction().commit();
			session2.disconnect();
			session2.close();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/delivery/email/sender/MailSender.java:373");
		}
		HibernateUtil.closeSession();

		if (notifikasi.getId() == null) {
			return null;
		}

		// Notifikasi baru tersimpan -> tandai cache lonceng/pusat notifikasi agar segar.
		try {
			ais.common.NotifikasiCache.tandaiKotor();
		} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/delivery/email/sender/MailSender.java:384");
		}

		// Push (GCP) + WhatsApp. ubahKeterangan=false agar keterangan terperkaya tetap.
		try {
			kirimNotif(notifikasi, false, classData, null, kirimkankeWa, file);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/delivery/email/sender/MailSender.java:391");
		}

		// Email — hanya benar-benar terkirim bila konfigurasi email aktif (dicek di
		// sendMailProcess). Tetap memakai record notifikasi yang sama (tidak dobel).
		try {
			if (recipientsTemp != null && !recipientsTemp.trim().isEmpty()) {
				boolean sendinblue = Common.bolehKonfigurasi("aktfikan_pengiriman_email_menggunakan_sendinblue.com", Konfigurasi.TIDAK_AKTIF);
				if (sendinblue) {
					sendinblue(userIds, subject, body, sender, recipientsTemp, null, notifikasi, file);
				} else {
					String emailMonitoring = Common.getKonfigurasi("alamat_email_monitoring", "").getNilai();
					String recipients = emailMonitoring.trim().isEmpty() ? recipientsTemp
							: recipientsTemp + "," + emailMonitoring;
					for (String r : recipients.split(",")) {
						String m = r.trim();
						if (Common.isValidEmailAddress(m)) {
							sendMailProcess(subject, body, sender, m, null, akademik, tagihan, notifikasi, file);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/delivery/email/sender/MailSender.java:414");
		}

		return notifikasi;
	}

	/**
	 * Khusus proses pengiriman email: tetap membuat record Notifikasi walaupun
	 * konfigurasi "aktfikan_pengiriman_notif" bernilai tidak aktif.
	 *
	 * Catatan:
	 * - Pengaturan tersebut tetap dihormati untuk pemanggilan simpanNotif biasa.
	 * - Pengiriman email tidak boleh ikut berhenti hanya karena notifikasi aplikasi
	 *   dimatikan.
	 */
	private static Notifikasi simpanNotifUntukEmail(JSONArray userIdsTemp, String recipientsTemp, String subject,
			String body, GeneralValueObject dataObject, JSONArray attachmentsData, boolean kirimkankeWa, File... temp) {
		return simpanNotifInternal(userIdsTemp, recipientsTemp, subject, body, dataObject, attachmentsData, kirimkankeWa,
				true, temp);
	}

	private static Notifikasi simpanNotifInternal(JSONArray userIdsTemp, String recipientsTemp, String subject,
			String body, GeneralValueObject dataObject, JSONArray attachmentsData, boolean kirimkankeWa,
			boolean abaikanPengaturanNotif, File... temp) {

		boolean notifAktif = abaikanPengaturanNotif || Common.bolehKonfigurasi("aktfikan_pengiriman_notif");
		if (!notifAktif) {
			return null;
		}

		// Formalkan isi pesan (sangat resmi + panjang minimal) sebelum disimpan sebagai
		// record Notifikasi. Idempoten terhadap body yang sudah diformalkan dari hulu.
		if (ais.common.FormalisasiPesanUtil.terapkanNotifikasi()) {
			body = ais.common.FormalisasiPesanUtil.bungkusFormalHtml(subject, body);
		}

			final File[] file = jadikanSatuFilePdf(temp);
			JSONArray userIds = null;
			if (userIdsTemp != null) {
				if (dataObject != null && dataObject.getId() != null) {
					userIds = new JSONArray();
					for (int i = 0; i < userIdsTemp.length(); i++) {
						try {
							String d = userIdsTemp.getString(i);
							if (!d.trim().isEmpty()) {

								String key = dataObject.getClass().getSimpleName() + "_" + dataObject.getId() + "_" + d;
								if (!notifSudah.contains(key)) {
									userIds.put(d);
									notifSudah.add(key);
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/delivery/email/sender/MailSender.java:467");
						}
					}
				} else {
					userIds = userIdsTemp;
				}
			}

			if (recipientsTemp != null || (userIds != null && userIds.length() > 0)) {
				Notifikasi notifikasi = new Notifikasi();
				try {

					notifikasi.setNama(userIds == null ? null : userIds.toString());

					if (recipientsTemp != null) {
						JSONArray jsonArray = new JSONArray();
						for (String s : recipientsTemp.split(",")) {
							jsonArray.put(s);
						}
						notifikasi.setEmails(jsonArray.toString());
					}

					JSONObject jsonObject = new JSONObject();
					jsonObject.put("subject", subject);
					jsonObject.put("body", body);

					notifikasi.setKeterangan(jsonObject.toString());

					org.hibernate.Session session2 = HibernateUtil.currentNativeSession();
					session2.getTransaction().begin();
					session2.save(notifikasi);
					session2.getTransaction().commit();
					session2.disconnect();
					session2.close();

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/delivery/email/sender/MailSender.java:503");
				}
				HibernateUtil.closeSession();

				// Notifikasi baru tersimpan -> segarkan cache lonceng/pusat notifikasi.
				try {
					ais.common.NotifikasiCache.tandaiKotor();
				} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/delivery/email/sender/MailSender.java:510");
				}

				try {

					JSONObject classData = new JSONObject();
					if (dataObject != null) {

						classData.put("name", dataObject.getClass().getName().split("_")[0]);

						if (dataObject instanceof Tbmuser) {
							classData.put("id", ((Tbmuser) dataObject).getUserId());
						} else if (dataObject instanceof Tbmrole) {
							classData.put("id", ((Tbmrole) dataObject).getRoleId());
						} else {
							classData.put("id", dataObject.getId());
						}

						classData.put("object_kode", dataObject.getKode() == null ? "" : dataObject.getKode());
						classData.put("object_name", dataObject.getNama() == null ? "" : dataObject.getNama());
						classData.put("object_keterangan",
								dataObject.getKeterangan() == null ? "" : dataObject.getKeterangan());
					}
					if (kirimkankeWa) {
						kirimNotif(notifikasi, classData, attachmentsData, kirimkankeWa, file);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/delivery/email/sender/MailSender.java:537");
				}

				return notifikasi;
			} else {
				return null;
			}
		
	}


	public static List<String> kirimNotif(Notifikasi notifikasi, JSONObject classData, JSONArray attachmentsData,
			File... file) throws Exception {
		return kirimNotif(notifikasi, true, classData, attachmentsData, false, file);
	}

	public static List<String> kirimNotif(Notifikasi notifikasi, JSONObject classData, JSONArray attachmentsData,
			boolean kirimkankeWa, File... file) throws Exception {
		return kirimNotif(notifikasi, true, classData, attachmentsData, kirimkankeWa, file);
	}

	@SuppressWarnings("unchecked")
	public static List<String> kirimNotif(Notifikasi notifikasi, boolean ubahKeterangan, JSONObject classData,
			final JSONArray attachmentsDataFinal, boolean kirimkankeWa, final File... file) throws Exception {
		List<String> hasils = new ArrayList<String>();
		JSONArray userIds = new JSONArray(notifikasi.getNama());
		final Set<String> users = new HashSet<String>();
		List<Long> ids = new ArrayList<Long>();
		for (int i = 0; i < userIds.length(); i++) {
			String d = userIds.getString(i);
			if (!d.trim().isEmpty()) {
				users.add(d);

				if (Common.isNumber(d.trim())) {
					ids.add(Long.parseLong(d.trim()));
				}
			}
		}

		if (!users.isEmpty()) {

			JSONObject jsonObject = new JSONObject(notifikasi.getKeterangan());
			String titleTemp = jsonObject.isNull("subject") ? "{NO TITLE}" : jsonObject.getString("subject");
			String bodyTemp = jsonObject.isNull("body") ? "{NO BODY}" : jsonObject.getString("body");
			final String bodyAsli = jsonObject.isNull("body") ? "{NO BODY}" : jsonObject.getString("body");
			try {
				titleTemp = Jsoup.parse(titleTemp).text();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/delivery/email/sender/MailSender.java:585");
			}

			try {
				bodyTemp = Jsoup.parse(bodyTemp.replaceAll("<br>", "\n")).text();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/delivery/email/sender/MailSender.java:591");
			}

			final String body = bodyTemp;
			final String title = titleTemp;

			if (kirimkankeWa && Common.bolehKonfigurasi("aktifkan_reply_chatbot")) {
				submitEmail(new Runnable() {

					@Override
					public void run() {
						try {

						try {

							String url = null;
							String name = null;
							try {
								if (file != null && file.length > 0) {
									name = file[0].getName();
									url = Common.getRequestHostWithProtocolSimple()
											+ file[0].getAbsolutePath().split("webapps")[1];
								} else if (attachmentsDataFinal != null && attachmentsDataFinal.length() > 0) {
									name = attachmentsDataFinal.getJSONObject(0).getString("name");
									url = attachmentsDataFinal.getJSONObject(0).getString("url");
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/delivery/email/sender/MailSender.java:618");
							}

							Set<String> tos = new HashSet<String>();

							org.hibernate.Session session2 = HibernateUtil.currentNativeSession();
							tos.addAll(session2.createCriteria(Tbmuser.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.in("userId", users)).setProjection(Projections.property("hp"))
									.add(Restrictions.isNotNull("hp")).add(Restrictions.ne("hp", "")).list());

							tos.addAll(session2.createCriteria(Mahasiswa.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.in("nim", users)).setProjection(Projections.property("telp"))
									.add(Restrictions.isNotNull("telp")).add(Restrictions.ne("telp", "")).list());

							tos.addAll(session2.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
									.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
									.add(Restrictions.or(Restrictions.in("nomorIndukNasional", users),
											Restrictions.in("nomorInduk", users))

									).setProjection(Projections.property("teleponSiswa"))
									.add(Restrictions.isNotNull("teleponSiswa"))
									.add(Restrictions.ne("teleponSiswa", "")).list());

							tos.addAll(session2.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
									.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
									.add(Restrictions.or(Restrictions.in("nomorIndukNasional", users),
											Restrictions.in("nomorInduk", users))

									).setProjection(Projections.property("hp1ayah"))
									.add(Restrictions.isNotNull("hp1ayah")).add(Restrictions.ne("hp1ayah", "")).list());

							tos.addAll(session2.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
									.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
									.add(Restrictions.or(Restrictions.in("nomorIndukNasional", users),
											Restrictions.in("nomorInduk", users))

									).setProjection(Projections.property("hp1ibu"))
									.add(Restrictions.isNotNull("hp1ibu")).add(Restrictions.ne("hp1ibu", "")).list());
							session2.disconnect();
							session2.close();
							HibernateUtil.closeSession();

							String dawal = Common.getKonfigurasi("pesan_tambahan_notif_awal",
									"*Pesan ini dibuat secara otomatis oleh sistem sebagai notifikasi/pemberitahuan kepada Anda*\n\n")
									.getNilai();

							// Susun teks WhatsApp resmi (tag HTML dibersihkan + panjang minimal terpenuhi).
							String d = ais.common.FormalisasiPesanUtil.teksWa(title, bodyAsli, dawal);
							System.out.println("tos -> " + tos);
							System.out.println("d -> " + d);

							System.out.println("name -> " + name);
							System.out.println("url -> " + url);

							for (String to : tos) {
								Wa.kirimWaViaUltramsg(to, d, name, url);
							}

						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/delivery/email/sender/MailSender.java:679");
						}

											} finally {
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				});
			}

			org.hibernate.Session session2 = HibernateUtil.currentNativeSession();
			List<String> tos = session2.createCriteria(Tbmuser.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.in("userId", users)).setProjection(Projections.property("gcpToken"))
					.add(Restrictions.isNotNull("gcpToken")).add(Restrictions.ne("gcpToken", "")).list();

			tos.addAll(session2.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.in("nim", users)).setProjection(Projections.property("gcpToken"))
					.add(Restrictions.isNotNull("gcpToken")).add(Restrictions.ne("gcpToken", "")).list());

			tos.addAll(session2.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
					.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah")).add(Restrictions
							.or(Restrictions.in("nomorIndukNasional", users), Restrictions.in("nomorInduk", users))

					).setProjection(Projections.property("gcpToken")).add(Restrictions.isNotNull("gcpToken"))
					.add(Restrictions.ne("gcpToken", "")).list());

			session2.disconnect();
			session2.close();
			HibernateUtil.closeSession();
			String hasilSemua = "";

			JSONArray attachmentsData = attachmentsDataFinal;

			JSONArray tokens = new JSONArray();

			for (String to : tos) {
				if (to != null && !to.trim().isEmpty()) {
					for (String t : to.split(";")) {
						tokens.put(t);
					}
				}
			}

			if (tokens.length() > 0) {

				String linkPost = Common.getKonfigurasi("link_push_multiple_devices_notif",
						"http://dev.ecampus.id:3000/push_multiple_devices").getNilai().trim();

				// Push ke server mobile (dev.ecampus.id:3000/push_multiple_devices) DINONAKTIFKAN
				// secara default. Aktifkan kembali dengan konfigurasi
				// "aktifkan_push_multiple_devices_notif" = AKTIF bila server push sudah siap.
				boolean pushAktif = Common.bolehKonfigurasi("aktifkan_push_multiple_devices_notif", Konfigurasi.TIDAK_AKTIF);

				JSONObject data = new JSONObject();
				data.put("id", notifikasi.getId() + "");
				data.put("time", Common.dateFormat3.get().format(notifikasi.getWaktu()));
				data.put("title", title);
				data.put("body", body);

				if (classData != null) {
					data.put("classData", classData);
					jsonObject.put("classData", classData);
				}

				if (attachmentsData != null) {
					data.put("attachment", attachmentsData);
				} else if (file != null && (file.length > 0 && file[0] != null)) {
					JSONArray attachments = new JSONArray();
					for (File f : file) {

						System.out.println("Kirimkna file " + f.getAbsolutePath());

						try {
							String fileAtt = Common.getRequestHostWithProtocolSimple()
									+ f.getAbsolutePath().split("webapps")[1];

							System.out.println("Kirimkna link file " + fileAtt);

							JSONObject attachment = new JSONObject();
							attachment.put("url", fileAtt);
							attachment.put("name", f.getName());

							attachments.put(attachment);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/delivery/email/sender/MailSender.java:764");
							// TODO: handle exception
						}
					}

					data.put("attachment", attachments);
				}

				JSONObject notification = new JSONObject();
				notification.put("title", title);
				notification.put("body", body);

				JSONObject postData = new JSONObject();

				postData.put("data", data);
				postData.put("notification", notification);
				postData.put("tokens", tokens);

				if (!pushAktif) {
					// Dinonaktifkan: lewati pengiriman push ke server mobile.
					System.out.println("Push ke mobile NONAKTIF (aktifkan_push_multiple_devices_notif != AKTIF). "
							+ "Lewati " + linkPost);
				} else {
					System.out.println("linkPost -> " + linkPost);
					System.out.println("kirim -> " + postData);

					// Payload dikirim via STDIN ("--data-binary @-"), BUKAN sebagai argumen baris perintah.
					// Body notifikasi yang panjang membuat argumen melebihi batas ARG_MAX OS sehingga
					// ProcessBuilder.start() gagal "Cannot run program curl: error=7, Argument list too long".
					String[] command = { "curl", "-k", "-H", "Content-Type: application/json", "-X", "POST", linkPost,
							"--data-binary", "@-" };

					ProcessBuilder process = new ProcessBuilder(command);
					Process p;
					p = process.start();
					try {
						java.io.OutputStream osCurl = p.getOutputStream();
						osCurl.write(postData.toString().getBytes("UTF-8"));
						osCurl.flush();
						osCurl.close();
					} catch (Exception exStdin) {
						exStdin.printStackTrace(); ais.common.ErrorAuditUtil.record(exStdin, "auto-audit src/ais/delivery/email/sender/MailSender.java:805");
					}
					BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
					StringBuilder builder = new StringBuilder();
					String line = null;
					while ((line = reader.readLine()) != null) {
						builder.append(line);
						builder.append(System.getProperty("line.separator"));
					}
					String hasil = builder.toString();

					hasilSemua += hasilSemua.isEmpty() ? hasil : ";" + hasil;

					System.out.println("hasil -> " + hasil);
					hasils.add(hasil);
				}
			}

			if (attachmentsData == null) {
				attachmentsData = new JSONArray();
				if (file != null && (file.length > 0 && file[0] != null)) {

					for (File f : file) {
						try {
							String fileAtt = Common.getRequestHostWithProtocolSimple()
									+ f.getAbsolutePath().split("webapps")[1];

							System.out.println("Kirimkna link file " + fileAtt);

							JSONObject attachment = new JSONObject();
							attachment.put("url", fileAtt);
							attachment.put("name", f.getName());

							attachmentsData.put(attachment);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/delivery/email/sender/MailSender.java:839");
							// TODO: handle exception
						}
					}
				}
			}

			if (notifikasi != null) {
				try {

					if (ubahKeterangan) {
						if (attachmentsData != null) {
							jsonObject.put("attachments", attachmentsData);
						}
						notifikasi.setKeterangan(jsonObject.toString());
					}
					notifikasi.setHasil(hasilSemua);
					session2 = HibernateUtil.currentNativeSession();
					session2.getTransaction().begin();
					session2.update(notifikasi);
					session2.getTransaction().commit();
					session2.disconnect();
					session2.close();
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/delivery/email/sender/MailSender.java:863");
				}
				HibernateUtil.closeSession();
			}
		}
		return hasils;

	}

	public static File[] jadikanSatuFilePdf(File... file) {

		if (file == null || file.length == 0) {
			return new File[] {};
		}

		List<File> filesbaru = new ArrayList<File>();
		List<File> filespdf = new ArrayList<File>();
		String path = "";
		for (File f : file) {
			if (f != null) {
				if (f.getName().toLowerCase().endsWith(".pdf")) {
					filespdf.add(f);
					path = f.getParentFile().getAbsolutePath();
				} else {
					filesbaru.add(f);
				}
			}
		}

		if (filespdf.size() == 1) {
			filesbaru.add(filespdf.get(0));
		} else if (!filespdf.isEmpty()) {
			File filePdfBaru = new File(path + "/" + Common.getGeneratedBarCode() + ".pdf");

			PDFMergerUtility ut = new PDFMergerUtility();

			for (File f : filespdf) {
				ut.addSource(f);
			}

			try {
				ut.setDestinationStream(new FileOutputStream(filePdfBaru));
				ut.mergeDocuments();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/delivery/email/sender/MailSender.java:907");
			}

			filesbaru.add(filePdfBaru);
		}

		return filesbaru.toArray(new File[] {});
	}

	private static void sendMailLampiran(JSONArray userIds, String subject, String body, String sender,
			String recipientsTemp, PrintStream out, GeneralValueObject dataObject, JSONArray attachmentsData,
			boolean kirimkankeWa, File... file) throws Exception {

		boolean tidakBoleh = Common.getKonfigurasi("email_tidak_boleh_kirim_dari", "notify@tarunabakti.or.id")
				.getNilai().trim().equalsIgnoreCase(sender);
		if (tidakBoleh) {
			System.out.println("Tidak boleh kirimkna email ke " + sender);
			return;
		}

		Notifikasi notifikasi = MailSender.simpanNotifUntukEmail(userIds, recipientsTemp, subject, body, dataObject,
				attachmentsData, kirimkankeWa, file);

		boolean sendinblue = Common.bolehKonfigurasi("aktfikan_pengiriman_email_menggunakan_sendinblue.com", Konfigurasi.TIDAK_AKTIF);

		if (sendinblue) {
			sendinblue(userIds, subject, body, sender, recipientsTemp, out, notifikasi, file);
		} else {

			String emailMonitoring = Common.getKonfigurasi("alamat_email_monitoring", "").getNilai();
			if (!emailMonitoring.trim().isEmpty()) {
				recipientsTemp = recipientsTemp.trim().isEmpty() ? emailMonitoring
						: recipientsTemp + "," + emailMonitoring;
			}

			if (recipientsTemp.indexOf(',') > 0) {
				String[] r = recipientsTemp.split(",");
				for (int i = 0; i < r.length; i++) {
					String m = r[i].trim();
					if (Common.isValidEmailAddress(m)) {
						sendMailProcess(subject, body, sender, m, out, true, false, notifikasi, file);
					}
				}

			} else {
				sendMailProcess(subject, body, sender, recipientsTemp, out, true, false, notifikasi, file);
			}
		}
	}

	public static void sendMailLampiranTagihan(JSONArray userIds, String subject, String body, String sender,
			String recipientsTemp, PrintStream out, boolean tampilHasil, GeneralValueObject dataObject, File... file)
			throws Exception {
		boolean tidakBoleh = Common.getKonfigurasi("email_tidak_boleh_kirim_dari", "notify@tarunabakti.or.id")
				.getNilai().trim().equalsIgnoreCase(sender);
		if (tidakBoleh) {
			System.out.println("Tidak boleh kirimkna email ke " + sender);
			return;
		}
		JSONArray attachmentsData = null;
		sendMailLampiranTagihan(userIds, subject, body, sender, recipientsTemp, out, tampilHasil, dataObject,
				attachmentsData, false, file);
	}

	public static void sendMailLampiranTagihan(JSONArray userIds, String subject, String body, String sender,
			String recipientsTemp, PrintStream out, boolean tampilHasil, GeneralValueObject dataObject,
			boolean kirimkankeWa, File... file) throws Exception {
		boolean tidakBoleh = Common.getKonfigurasi("email_tidak_boleh_kirim_dari", "notify@tarunabakti.or.id")
				.getNilai().trim().equalsIgnoreCase(sender);
		if (tidakBoleh) {
			System.out.println("Tidak boleh kirimkna email ke " + sender);
			return;
		}
		JSONArray attachmentsData = null;
		sendMailLampiranTagihan(userIds, subject, body, sender, recipientsTemp, out, tampilHasil, dataObject,
				attachmentsData, kirimkankeWa, file);
	}

	private static void sendMailLampiranTagihan(JSONArray userIds, String subject, String body, String sender,
			String recipientsTemp, PrintStream out, boolean tampilHasil, GeneralValueObject dataObject,
			JSONArray attachmentsData, boolean kirimkankeWa, File... file) throws Exception {

		final Notifikasi notif = simpanNotifUntukEmail(userIds, recipientsTemp, subject, body, dataObject, attachmentsData,
				kirimkankeWa, file);

		boolean sendinblue = Common.bolehKonfigurasi("aktfikan_pengiriman_email_menggunakan_sendinblue.com", Konfigurasi.TIDAK_AKTIF);

		if (sendinblue) {
			sendinblue(userIds, subject, body, sender, recipientsTemp, out, notif, file);
		} else {

			String emailMonitoring = Common.getKonfigurasi("alamat_email_monitoring", "").getNilai();
			if (!emailMonitoring.trim().isEmpty()) {
				recipientsTemp = recipientsTemp.trim().isEmpty() ? emailMonitoring
						: recipientsTemp + "," + emailMonitoring;
			}

			if (recipientsTemp.indexOf(',') > 0) {
				String[] r = recipientsTemp.split(",");
				for (int i = 0; i < r.length; i++) {
					String m = r[i].trim();
					if (Common.isValidEmailAddress(m)) {
						sendMailProcess(subject, body, sender, m, out, false, true, notif, file);
					}
				}

			} else {
				sendMailProcess(subject, body, sender, recipientsTemp, out, false, true, notif, file);
			}
		}

		if (tampilHasil) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (notif != null) {
						Messagebox.show(notif.getHasil() + "\n\n" + notif.getHasilEmail(), "Info Pengiriman Data",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					} else {
						Messagebox.show("Pengiriman email sudah diproses. Data notifikasi tidak dibuat karena penerima kosong.",
								"Info Pengiriman Data", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					}
				}
			}, "Proses pengiriman data", false, 4000);
		}
	}

	private static void sendinblue(final JSONArray userIds, final String subject, final String body,
			final String sender, final String recipientsTemp, final PrintStream out, final Notifikasi notifikasi,
			File... temp) {
		final File[] file = jadikanSatuFilePdf(temp);
		submitEmail(new Runnable() {

			@Override
			public void run() {
				try {

				try {

					JSONObject postData = new JSONObject();

					// 1. Set Sender (Global Parameter)
					JSONObject senderD = new JSONObject();
					senderD.put("email", sender);
					senderD.put("name", "Email Informasi");
					postData.put("sender", senderD);

					// 2. Set Subject & HTML Content (Global Parameter)
					postData.put("subject", subject);
					postData.put("htmlContent", body);

					// 3. Set Message Versions (Batch Sending Format)
					JSONArray messageVersions = new JSONArray();
					for (String s : recipientsTemp.split(",")) {
						String emailStr = s.trim(); // Menghindari error akibat spasi
						if (!emailStr.isEmpty()) {
							JSONObject version = new JSONObject();

							JSONArray tos = new JSONArray();
							JSONObject to = new JSONObject();
							to.put("email", emailStr);
							to.put("name", "Penerima Informasi");
							tos.put(to);

							version.put("to", tos); // 'to' berada di dalam 'messageVersions'
							messageVersions.put(version);
						}
					}
					// Masukkan messageVersions ke payload
					postData.put("messageVersions", messageVersions);

					// 4. Set Attachments (Global Parameter)
					if (file != null && (file.length > 0 && file[0] != null)) {
						JSONArray attachments = new JSONArray();
						for (File f : file) {
							System.out.println("Kirimkan file " + f.getAbsolutePath());

							try {
								String fileAtt = Common.getRequestHostWithProtocolSimple()
										+ f.getAbsolutePath().split("webapps")[1];

								System.out.println("Kirimkan link file " + fileAtt);

								JSONObject attachment = new JSONObject();
								attachment.put("url", fileAtt);
								attachment.put("name", f.getName());

								attachments.put(attachment);

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/delivery/email/sender/MailSender.java:1098"); // Minimal log jika terjadi error
							}
						}

						// Pastikan array attachments tidak kosong sebelum di-put
						if (attachments.length() > 0) {
							postData.put("attachment", attachments);
						}
					}

					// Update endpoint domain ke brevo.com (sendinblue.com tetap berfungsi namun
					// deprecated)
					String strURL = "https://api.brevo.com/v3/smtp/email";

					String api_key = Common.getKonfigurasi("key_sendinblue.com", "").getNilai();

					// Payload via STDIN ("--data-binary @-") agar tidak melebihi batas ARG_MAX OS
					// (body email formal yang panjang sebelumnya memicu "Argument list too long").
					String[] command = { "curl", "-H", "accept: application/json", "-H", "api-key: " + api_key, "-H",
							"Content-Type: application/json", "--location", strURL, "--data-binary", "@-" };

					ProcessBuilder process = new ProcessBuilder(command);
					Process p;
					p = process.start();
					try {
						java.io.OutputStream osCurl = p.getOutputStream();
						osCurl.write(postData.toString().getBytes("UTF-8"));
						osCurl.flush();
						osCurl.close();
					} catch (Exception exStdin) {
						exStdin.printStackTrace(); ais.common.ErrorAuditUtil.record(exStdin, "auto-audit src/ais/delivery/email/sender/MailSender.java:1128");
					}
					BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
					StringBuilder builder = new StringBuilder();
					String line = null;
					while ((line = reader.readLine()) != null) {
						builder.append(line);
						builder.append(System.getProperty("line.separator"));
					}
					String hasil = builder.toString();

					System.out.println(postData);
					System.out.println(hasil);

					if (out != null) {
						out.println(postData + "\n\n\n" + hasil);
					}

					if (notifikasi != null) {
						notifikasi.setHasilEmail(hasil);
						org.hibernate.Session session2 = HibernateUtil.currentNativeSession();
						session2.getTransaction().begin();
						session2.update(notifikasi);
						session2.getTransaction().commit();

						session2.disconnect();
						session2.close();
						HibernateUtil.closeSession();
					}
				} catch (Exception e) {
					if (out != null) {
						e.printStackTrace(out);
					} else {
						Common.tampilErrorJikaAdmin(e);
					}
				}

							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		});
	}

	public static void sendMailLampiranAll(JSONArray userIds, String subject, String body, String sender,
			String recipientsTemp, PrintStream out, GeneralValueObject dataObject, File... file) throws Exception {
		boolean tidakBoleh = Common.getKonfigurasi("email_tidak_boleh_kirim_dari", "notify@tarunabakti.or.id")
				.getNilai().trim().equalsIgnoreCase(sender);
		if (tidakBoleh) {
			System.out.println("Tidak boleh kirimkna email ke " + sender);
			return;
		}
		JSONArray attachmentsData = null;
		sendMailLampiranAll(userIds, subject, body, sender, recipientsTemp, out, dataObject, attachmentsData, false,
				file);
	}

	public static void sendMailLampiranAll(JSONArray userIds, String subject, String body, String sender,
			String recipientsTemp, PrintStream out, GeneralValueObject dataObject, boolean kirimkankeWa, File... file)
			throws Exception {
		boolean tidakBoleh = Common.getKonfigurasi("email_tidak_boleh_kirim_dari", "notify@tarunabakti.or.id")
				.getNilai().trim().equalsIgnoreCase(sender);
		if (tidakBoleh) {
			System.out.println("Tidak boleh kirimkna email ke " + sender);
			return;
		}
		JSONArray attachmentsData = null;
		sendMailLampiranAll(userIds, subject, body, sender, recipientsTemp, out, dataObject, attachmentsData,
				kirimkankeWa, file);
	}

	private static void sendMailLampiranAll(JSONArray userIds, String subject, String body, String sender,
			String recipientsTemp, PrintStream out, GeneralValueObject dataObject, JSONArray attachmentsData,
			boolean kirimkankeWa, File... file) throws Exception {

		Notifikasi notifikasi = simpanNotifUntukEmail(userIds, recipientsTemp, subject, body, dataObject, attachmentsData,
				kirimkankeWa, file);

		boolean sendinblue = Common.bolehKonfigurasi("aktfikan_pengiriman_email_menggunakan_sendinblue.com", Konfigurasi.TIDAK_AKTIF);

		if (sendinblue) {
			sendinblue(userIds, subject, body, sender, recipientsTemp, out, notifikasi, file);
		} else {

			if (recipientsTemp.indexOf(',') > 0) {
				String[] r = recipientsTemp.split(",");
				for (int i = 0; i < r.length; i++) {
					String m = r[i].trim();
					if (Common.isValidEmailAddress(m)) {
						sendMailProcess(subject, body, sender, m, out, true, true, notifikasi, file);
					}
				}

			} else {
				sendMailProcess(subject, body, sender, recipientsTemp, out, true, true, notifikasi, file);
			}
		}
	}

	private static void sendMailProcess(final String subject, final String body, final String sender,
			String recipientsTemp, final PrintStream out, boolean akademik, boolean tagihan,
			final Notifikasi notifikasi, File... temp) {
		final File[] file = jadikanSatuFilePdf(temp);
		// Formalkan isi email (bahasa sangat resmi + panjang minimal) bila diaktifkan.
		// Idempoten: bila body sudah diformalkan (mis. dari simpanNotifikasiHalaman),
		// fungsi mengembalikan body apa adanya sehingga tidak terjadi pemformatan ganda.
		final String bodyFinal = ais.common.FormalisasiPesanUtil.terapkanEmail()
				? ais.common.FormalisasiPesanUtil.bungkusFormalHtml(subject, body)
				: body;
		if ((akademik && recipientsTemp != null && !recipientsTemp.trim().isEmpty()
				&& Common.bolehKonfigurasi("aktfikan_pengiriman_email_akademik"))
				|| (recipientsTemp != null && !recipientsTemp.trim().isEmpty() && tagihan
						&& Common.bolehKonfigurasi("aktfikan_pengiriman_email_tagihan"))) {

			boolean kirimEmail = Common.bolehKonfigurasi("aktfikan_pengiriman_email", Konfigurasi.TIDAK_AKTIF);
			if (recipientsTemp.endsWith("@ahoo.co")) {
				recipientsTemp = recipientsTemp.replaceAll("@ahoo.co", "@yahoo.co");
			}
			if (recipientsTemp.endsWith("@mail.com")) {
				recipientsTemp = recipientsTemp.replaceAll("@mail.com", "@gmail.com");
			}
			final String recipients = recipientsTemp;
			if (kirimEmail) {

				submitEmail(new Runnable() {

					@Override
					public void run() {
						String hasil = "";
						try {

							Properties props = new Properties();

							String mailhost = Common.getKonfigurasi("default_mailhost", "smtp.gmail.com").getNilai();
							String protocol = Common.getKonfigurasi("default_mail_protocol", "smtp").getNilai();
							String auth = Common.getKonfigurasi("default_mail_auth", "true").getNilai();
							String port = Common.getKonfigurasi("default_mail_port", "465").getNilai();
							String soketPort = Common.getKonfigurasi("default_mail_soket_port", "465").getNilai();
							String soketClass = Common
									.getKonfigurasi("default_mail_soket_class", "javax.net.ssl.SSLSocketFactory")
									.getNilai();
							String soketFallback = Common.getKonfigurasi("default_mail_soket_fallback", "false")
									.getNilai();
							String soketQuitwaitback = Common.getKonfigurasi("default_mail_soket_quitwait", "false")
									.getNilai();

							String starttls = Common.getKonfigurasi("mail.smtp.starttls.enable", "").getNilai();
							String protocols = Common.getKonfigurasi("mail.smtp.ssl.protocols", "TLSv1.2").getNilai();

							String ssl = Common.getKonfigurasi("mail.smtp.ssl.enable", "").getNilai();

							boolean debug = Common.bolehKonfigurasi("mail_debug", Konfigurasi.TIDAK_AKTIF) || out != null;

							props.setProperty("mail.transport.protocol", protocol);
							props.setProperty("mail.host", mailhost);
							props.setProperty("mail.smtp.host", mailhost);
							props.put("mail.smtp.auth", auth);
							if (!ssl.trim().isEmpty()) {
								props.put("mail.smtp.ssl.enable", ssl);
							}
							props.put("mail.smtp.port", port);
							props.put("mail.smtp.socketFactory.port", soketPort);
							if (!soketClass.trim().isEmpty()) {
								props.put("mail.smtp.socketFactory.class", soketClass);
							}
							if (!soketFallback.trim().isEmpty()) {
								props.put("mail.smtp.socketFactory.fallback", soketFallback);
							}
							if (!soketQuitwaitback.trim().isEmpty()) {
								props.setProperty("mail.smtp.quitwait", soketQuitwaitback);
							}

							if (!starttls.trim().isEmpty()) {
								props.put("mail.smtp.starttls.enable", starttls); // enable STARTTLS
							}
							if (!protocols.trim().isEmpty()) {
								props.put("mail.smtp.ssl.protocols", protocols); // enable STARTTLS
							}

							System.out.println("props -> " + props);

							Session session = Session.getInstance(props, new javax.mail.Authenticator() {
								protected PasswordAuthentication getPasswordAuthentication() {
									String email_username = Common.getKonfigurasi("default_email_username", "zishof")
											.getNilai();
									String email_password = Common.getKonfigurasi("default_email_password", "zishof")
											.getNilai();
									return new PasswordAuthentication(email_username, email_password);
								}
							});
							session.setDebug(debug);
							MimeMessage message = new MimeMessage(session);
							message.setSender(new InternetAddress(sender));
							message.setSubject(subject);

							if (file != null && (file.length > 0 && file[0] != null)) {

								Multipart multipart = new MimeMultipart();

								// Create a multipar message
								// Create the message part
								BodyPart messageBodyPart = new MimeBodyPart();
								messageBodyPart.setContent(bodyFinal, "text/html");

								// Set text message part
								multipart.addBodyPart(messageBodyPart);

								for (File f : file) {
									if (f != null) {
										// Part two is attachment
										messageBodyPart = new MimeBodyPart();
										FileDataSource source = new FileDataSource(f);
										messageBodyPart.setDataHandler(new DataHandler(source));
//										messageBodyPart.set
//										messageBodyPart.setDataHandler(null)
										messageBodyPart.setFileName(f.getName());
										multipart.addBodyPart(messageBodyPart);
									}
								}

								// Send the complete message parts
								message.setContent(multipart);

							} else {
								message.setContent(bodyFinal, "text/html");
							}
							if (recipients.indexOf(',') > 0)
								message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
							else
								message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipients));
							if (out != null) {
								session.setDebugOut(out);
							}

							Transport.send(message);

							hasil = "Kirim ke " + recipients + " sukses";

						} catch (Exception e) {

							hasil = "Kirim ke " + recipients + " gagal " + e.getMessage();

							if (out != null) {
								e.printStackTrace(out);
							} else {
								boolean isTransient = (e instanceof javax.mail.MessagingException
										&& (e.getMessage() != null && (e.getMessage().contains("connect") || e.getMessage().contains("UnknownHost"))))
										|| (e instanceof com.sun.mail.smtp.SMTPSendFailedException);
								if (!isTransient) {
									Common.tampilErrorJikaAdmin(e);
								} else {
									System.err.println("[MailSender] SMTP gagal ke " + recipients + ": " + e.getMessage());
								}
							}
						}

						if (notifikasi != null) {
							org.hibernate.Session session2 = null;
							try {
								String hasilTerpotong = hasil == null ? null
										: (hasil.length() > 2000 ? hasil.substring(0, 2000) : hasil);
								notifikasi.setHasilEmail(hasilTerpotong);
								session2 = HibernateUtil.currentNativeSession();
								session2.getTransaction().begin();
								session2.update(notifikasi);
								session2.getTransaction().commit();
							} catch (Throwable t) {
								try {
									if (session2 != null && session2.getTransaction() != null
											&& session2.getTransaction().isActive())
										session2.getTransaction().rollback();
								} catch (Exception exRb) { ais.common.ErrorAuditUtil.record(exRb, "auto-audit(empty-catch) src/ais/delivery/email/sender/MailSender.java:1399"); /* ignore */ }
								System.err.println("[MailSender] Gagal simpan status notifikasi: " + t);
							} finally {
								try {
									if (session2 != null && session2.isOpen()) {
										session2.disconnect();
										session2.close();
									}
								} catch (Exception exCl) { ais.common.ErrorAuditUtil.record(exCl, "auto-audit(empty-catch) src/ais/delivery/email/sender/MailSender.java:1407"); /* ignore */ }
								HibernateUtil.closeSession();
							}
						}
					}
				});
			}

		}

	}

	public static void main(String[] args) {
		// Recipient's email ID needs to be mentioned.
		String to = "fauzioke2003@gmail.com";

		// Sender's email ID needs to be mentioned
		final String from = "postmaster@unsika.ac.id";

		// Assuming you are sending email from localhost
		String host = "smtp.mailgun.org";

		// Get system properties
		Properties properties = new Properties();
		properties.setProperty("mail.transport.protocol", "smtp");
		properties.setProperty("mail.host", host);
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.port", "587");
		properties.put("mail.smtp.socketFactory.port", "587");

		properties.setProperty("mail.smtp.host", host);

		System.out.println("properties -> " + properties);

		Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(from, "f5dfcfb0d9fa70e2593fe51e1edb9793-e2e3d8ec-dafe870f");

			}
		});
		session.setDebug(false);
		try {
			// Create a default MimeMessage object.
			MimeMessage message = new MimeMessage(session);

			// Set From: header field of the header.
			message.setFrom(new InternetAddress("no-reply@unsika.ac.id"));

			// Set To: header field of the header.
			// message.setRecipients(Message.RecipientType.BCC,
			// InternetAddress.parse(to));
			message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));

			// Set Subject: header field
			message.setSubject("This is the Subject Line!");

			// Now set the actual message
			message.setContent(
					"Anda mendapatkan informasi dari catatan KRS.<br><img src='http://ecampus.pelitabangsa.ac.id/pb/AmbilMedia?id=admin&name=nama&foto=foto&clazz=ais.database.model.file.FotoAdmin&property=tbmuser&height=75&width=65&var=7'/><br>admin<br>Isi catatan-nya adalah : ok, sip<br>Untuk informasi lebih lanjut bisa dilihat di http://ecampus.pelitabangsa.ac.id/pb, kemudian click menu KRS, cari catatan sbb : Mahasiswa: 1234567891 MAHASISWA TEST H2H, semester: 1, Tahun Akademik: 2014/2015<br><br>Terima Kasih",
					"text/html");

			// Send message
			Transport.send(message);
			System.out.println("Sent message successfully....");
		} catch (Exception mex) {
			mex.printStackTrace(); ais.common.ErrorAuditUtil.record(mex, "auto-audit src/ais/delivery/email/sender/MailSender.java:1472");
		}
	}

}
