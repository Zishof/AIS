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

/**
 * Mesin produksi tunggal untuk seluruh pengiriman email, notifikasi in-app, WhatsApp, dan push
 * mobile di AIS. Lihat dokumentasi paket {@link ais.delivery.email.sender package-info} untuk
 * peta lengkap kelas, pola overload berlapis, dan peringatan keamanan terkait kredensial tertanam
 * di {@link #main(String[])} milik kelas ini.
 *
 * <h2>Alur baku (berlaku di hampir semua metode publik kelas ini)</h2>
 * <ol>
 * <li>Buat/ambil record {@link ais.database.model.Notifikasi} lebih dulu lewat salah satu varian
 * {@code simpanNotif*} — ini SELALU terjadi lebih dulu, apa pun kanal pengirimannya, sehingga
 * riwayat "apa yang dikirim ke siapa" tetap lengkap walau kanal aktual (email/WA/push) gagal atau
 * dimatikan lewat konfigurasi.</li>
 * <li>Deduplikasi penerima memakai {@link #notifSudah}, sehingga permintaan kirim berulang dengan
 * kombinasi (objek data + subjek + user) yang sama tidak membuat notifikasi dobel.</li>
 * <li>Kanal aktual dijalankan asinkron lewat {@link #submitEmail(Runnable)} di atas
 * {@link #MAIL_POOL}, dengan setiap saklar kanal (email langsung vs Brevo, WA, push) dicek
 * terhadap konfigurasi via {@code ais.common.Common#bolehKonfigurasi}.</li>
 * <li>Hasil pengiriman (sukses/gagal/pesan error, per kanal) ditulis balik ke kolom
 * {@code hasil}/{@code hasilEmail} pada baris {@link ais.database.model.Notifikasi} yang sama,
 * sehingga riwayat notifikasi juga berfungsi sebagai log pengiriman.</li>
 * </ol>
 *
 * <p>
 * Kelas ini murni statis (tidak ada instance state) kecuali dua bidang paket ini sendiri:
 * {@link #MAIL_POOL} (pool thread bersama) dan {@link #notifSudah} (cache dedup proses-hidup,
 * tidak persisten lintas restart JVM).
 * </p>
 */
public class MailSender {

	/** Selang minimum antar-laporan kegagalan autentikasi SMTP ke audit. */
	private static final long JEDA_LAPOR_AUTH_MS = 15L * 60L * 1000L;

	/** Waktu terakhir kegagalan autentikasi SMTP dilaporkan penuh ke audit. */
	private static final java.util.concurrent.atomic.AtomicLong AUTH_GAGAL_TERAKHIR =
		new java.util.concurrent.atomic.AtomicLong(0L);

	/**
	 * Ukuran tetap {@link #MAIL_POOL} — jumlah thread worker yang boleh mengirim email/notifikasi
	 * secara bersamaan. Dibaca sekali saat pemuatan kelas dari properti sistem
	 * {@code -Dmail.pool.size} (default {@code "1"}, dipaksa minimal 1 bila nilai yang diberikan
	 * tidak valid atau kurang dari 1).
	 *
	 * <p>
	 * <b>Sejarah desain</b> — pembatas konkurensi kirim email/notifikasi. DULU tiap kirim =
	 * {@code new Thread(...).start()} (tak terbatas) → di beban tinggi ribuan raw-thread lahir
	 * (snapshot: "total dimulai" belasan ribu, puluhan {@code Thread-####} di
	 * {@code MailSender$4.run}), masing-masing buka Session+transaksi+koneksi c3p0+audit Envers →
	 * pool DB & statement-cache HABIS ({@code BasicResourcePool}/{@code GooGooStatementCache}
	 * antre) + heap tertekan. Sekarang semua dispatch lewat pool TETAP kecil + antrean; saat
	 * antrean penuh tugas dijalankan di thread pemanggil ({@link
	 * java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy} = backpressure) alih-alih
	 * melahirkan thread tanpa batas. SMTP diurutkan secara default (ukuran 1) untuk mencegah
	 * penyedia memblokir banyak login bersamaan; masih dapat diubah secara eksplisit via
	 * {@code -Dmail.pool.size} bila penyedia mengizinkan koneksi paralel lebih banyak.
	 * </p>
	 */
	private static final int MAIL_POOL_SIZE;
	static {
		int n = 1;
		try {
			n = Integer.parseInt(System.getProperty("mail.pool.size", "1").trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/delivery/email/sender/MailSender.java:74");
		}
		MAIL_POOL_SIZE = n < 1 ? 1 : n;
	}

	/**
	 * Pool thread tetap ({@link #MAIL_POOL_SIZE} worker daemon, antrean {@link
	 * java.util.concurrent.ArrayBlockingQueue} berkapasitas 1000) tempat seluruh pekerjaan kirim
	 * email/WA/push dijalankan. Diakses lewat {@link #submitEmail(Runnable)}, tidak pernah
	 * langsung dari luar kelas ini. Worker diberi nama {@code MailSender-worker-N} untuk
	 * memudahkan identifikasi di thread dump saat diagnosis.
	 */
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
	 *
	 * @param r tugas kirim (biasanya menutup satu koneksi SMTP/panggilan HTTP) yang akan dibungkus
	 *          dan diserahkan ke {@link #MAIL_POOL}; bila {@code execute} melempar (mis. pool sudah
	 *          shutdown), tugas dijalankan langsung di thread pemanggil sebagai fail-safe agar
	 *          email tidak hilang begitu saja.
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

	/**
	 * Titik masuk publik utama untuk mengirim satu email/notifikasi. Ini adalah anggota PERTAMA
	 * dari keluarga besar overload {@code sendMail}/{@code sendMailLampiran} (14 varian publik di
	 * kelas ini) yang seluruhnya bermuara pada satu implementasi privat kanonik:
	 * {@link #sendMailLampiran(JSONArray, String, String, String, String, PrintStream,
	 * GeneralValueObject, JSONArray, boolean, File...)}. Dokumentasi mendalam tentang ALUR
	 * pengiriman (kapan {@link ais.database.model.Notifikasi} dibuat, kapan email benar-benar
	 * terkirim, urutan Brevo-vs-SMTP-langsung, penanganan galat) ada pada implementasi kanonik
	 * tersebut serta pada {@link #sendMailProcess}; javadoc pada tiap overload publik di bawah ini
	 * sengaja diringkas karena hanya berbeda pada nilai default yang disisipkan untuk parameter
	 * opsional (lampiran {@code File}, {@code attachmentsData} JSON, {@code kirimkankeWa},
	 * {@code PrintStream out} untuk mengalirkan log debug SMTP ke pemanggil) — lihat juga
	 * penjelasan pola overload berlapis di {@link ais.delivery.email.sender package-info}.
	 *
	 * <p>
	 * Varian ini memanggil overload lain dengan {@code attachmentsData=null} (tanpa lampiran JSON,
	 * hanya lampiran {@code File} bila dipakai lewat varian {@code sendMailLampiran}) dan
	 * {@code out=null} (tidak ada aliran log debug ke pemanggil).
	 * </p>
	 *
	 * @param userIds       daftar user id penerima (dipakai untuk resolusi push token/nomor WA,
	 *                      bukan alamat email — lihat {@code recipients} untuk itu)
	 * @param subject       judul email/notifikasi
	 * @param body          isi pesan (HTML)
	 * @param sender        alamat pengirim; dicek terhadap konfigurasi
	 *                      {@code email_tidak_boleh_kirim_dari} di implementasi kanonik
	 * @param recipients    alamat email penerima, dipisah koma
	 * @param dataObject    entitas terkait (dipakai untuk metadata {@code classData} pada
	 *                      notifikasi), boleh {@code null}
	 * @param kirimkankeWa  kirim juga salinan pesan ke WhatsApp penerima (lewat Ultramsg) bila
	 *                      {@code true}
	 * @throws Exception diteruskan apa adanya dari implementasi kanonik (mis. kegagalan parsing
	 *                    alamat email, kegagalan Hibernate saat menyimpan notifikasi)
	 */
	public static void sendMail(JSONArray userIds, String subject, String body, String sender, String recipients,
			GeneralValueObject dataObject, boolean kirimkankeWa) throws Exception {
		JSONArray attachmentsData = null;
		sendMail(userIds, subject, body, sender, recipients, dataObject, attachmentsData, kirimkankeWa, null);
	}

	/** Seperti {@link #sendMail(JSONArray, String, String, String, String, boolean)}, tanpa lampiran, dengan tambahan {@code out} untuk mengalirkan log debug SMTP ke pemanggil (mis. tombol "test kirim" di layar admin). */
	public static void sendMail(JSONArray userIds, String subject, String body, String sender, String recipients,
			GeneralValueObject dataObject, PrintStream out, boolean kirimkankeWa) throws Exception {
		File file = null;
		JSONArray attachmentsData = null;
		sendMailLampiran(userIds, subject, body, sender, recipients, out, dataObject, attachmentsData, kirimkankeWa,
				file);
	}

	/** Seperti {@link #sendMail(JSONArray, String, String, String, String, boolean)}, dengan lampiran berupa {@link File} (bukan {@code attachmentsData} JSON) dan tanpa {@code PrintStream}. */
	public static void sendMailLampiran(JSONArray userIds, String subject, String body, String sender,
			String recipientsTemp, GeneralValueObject dataObject, boolean kirimkankeWa, File... file) throws Exception {
		JSONArray attachmentsData = null;
		sendMailLampiran(userIds, subject, body, sender, recipientsTemp, null, dataObject, attachmentsData,
				kirimkankeWa, file);
	}

	/** Seperti {@link #sendMail(JSONArray, String, String, String, String, boolean)}, dengan {@code attachmentsData} berupa lampiran JSON (mis. daftar {url,name} yang sudah diunggah sebelumnya) alih-alih {@link File} mentah. */
	public static void sendMail(JSONArray userIds, String subject, String body, String sender, String recipients,
			GeneralValueObject dataObject, JSONArray attachmentsData, boolean kirimkankeWa) throws Exception {
		sendMail(userIds, subject, body, sender, recipients, dataObject, attachmentsData, kirimkankeWa, null);
	}

	/** Kombinasi {@code attachmentsData} JSON + {@code kirimkankeWa} + {@code PrintStream out} untuk log debug — parameter paling lengkap di antara varian {@code sendMail} publik (tetap bukan kanonik; delegasi akhir tetap ke {@link #sendMailLampiran(JSONArray, String, String, String, String, PrintStream, GeneralValueObject, JSONArray, boolean, File...) varian privat}). */
	public static void sendMail(JSONArray userIds, String subject, String body, String sender, String recipients,
			GeneralValueObject dataObject, JSONArray attachmentsData, boolean kirimkankeWa, PrintStream out)
			throws Exception {
		File file = null;
		// recipients = recipients.replaceAll("@.", "@");
		sendMailLampiran(userIds, subject, body, sender, recipients, out, dataObject, attachmentsData, kirimkankeWa,
				file);
	}

	/** Seperti {@link #sendMailLampiran(JSONArray, String, String, String, String, GeneralValueObject, boolean, File...)}, dengan {@code attachmentsData} JSON tambahan di samping lampiran {@link File}. */
	public static void sendMailLampiran(JSONArray userIds, String subject, String body, String sender,
			String recipientsTemp, GeneralValueObject dataObject, JSONArray attachmentsData, boolean kirimkankeWa,
			File... file) throws Exception {
		sendMailLampiran(userIds, subject, body, sender, recipientsTemp, null, dataObject, attachmentsData,
				kirimkankeWa, file);
	}

	/** Varian paling ringkas: tanpa lampiran, tanpa {@code attachmentsData}, {@code kirimkankeWa} default {@code false}. Cocok untuk notifikasi email sederhana tanpa fan-out WhatsApp. */
	public static void sendMail(JSONArray userIds, String subject, String body, String sender, String recipients,
			GeneralValueObject dataObject) throws Exception {
		JSONArray attachmentsData = null;
		sendMail(userIds, subject, body, sender, recipients, dataObject, attachmentsData, null);
	}

	/** Seperti varian ringkas {@link #sendMail(JSONArray, String, String, String, String, GeneralValueObject)}, dengan tambahan {@code out} untuk log debug SMTP. */
	public static void sendMail(JSONArray userIds, String subject, String body, String sender, String recipients,
			GeneralValueObject dataObject, PrintStream out) throws Exception {
		File file = null;
		JSONArray attachmentsData = null;
		sendMailLampiran(userIds, subject, body, sender, recipients, out, dataObject, attachmentsData, false, file);
	}

	/** Varian ringkas dengan lampiran {@link File}, tanpa {@code attachmentsData} JSON/{@code kirimkankeWa}/{@code out} (semua default). */
	public static void sendMailLampiran(JSONArray userIds, String subject, String body, String sender,
			String recipientsTemp, GeneralValueObject dataObject, File... file) throws Exception {
		JSONArray attachmentsData = null;
		sendMailLampiran(userIds, subject, body, sender, recipientsTemp, null, dataObject, attachmentsData, false,
				file);
	}

	/** Varian ringkas dengan {@code attachmentsData} JSON, tanpa lampiran {@link File}/{@code kirimkankeWa}/{@code out}. */
	public static void sendMail(JSONArray userIds, String subject, String body, String sender, String recipients,
			GeneralValueObject dataObject, JSONArray attachmentsData) throws Exception {
		sendMail(userIds, subject, body, sender, recipients, dataObject, attachmentsData, null);
	}

	/** Seperti {@link #sendMail(JSONArray, String, String, String, String, GeneralValueObject, JSONArray)}, dengan tambahan {@code out} untuk log debug SMTP. */
	public static void sendMail(JSONArray userIds, String subject, String body, String sender, String recipients,
			GeneralValueObject dataObject, JSONArray attachmentsData, PrintStream out) throws Exception {
		File file = null;
		// recipients = recipients.replaceAll("@.", "@");
		sendMailLampiran(userIds, subject, body, sender, recipients, out, dataObject, attachmentsData, false, file);
	}

	/** Varian dengan {@code attachmentsData} JSON DAN lampiran {@link File} sekaligus, tanpa {@code kirimkankeWa}/{@code out}. Anggota terakhir keluarga overload {@code sendMail}/{@code sendMailLampiran} publik; delegasi berikutnya menuju implementasi kanonik privat. */
	public static void sendMailLampiran(JSONArray userIds, String subject, String body, String sender,
			String recipientsTemp, GeneralValueObject dataObject, JSONArray attachmentsData, File... file)
			throws Exception {
		sendMailLampiran(userIds, subject, body, sender, recipientsTemp, null, dataObject, attachmentsData, false,
				file);
	}

	/**
	 * Cache dedup proses-hidup untuk {@link #simpanNotifikasiHalaman}: kunci
	 * {@code "HAL_"+kelas+"_"+id+"_"+hashSubjek+"_"+userId} dicatat di sini begitu satu penerima
	 * pernah diproses untuk kombinasi (objek data + subjek) tertentu, sehingga panggilan berikutnya
	 * dengan kombinasi identik tidak membuat notifikasi/entry email dobel. TIDAK dibersihkan
	 * otomatis dan TIDAK persisten — tumbuh selama JVM hidup dan kembali kosong setelah restart
	 * aplikasi (dedup lintas restart bergantung sepenuhnya pada data {@link
	 * ais.database.model.Notifikasi} di database, bukan set ini).
	 */
	public static Set<String> notifSudah = new HashSet<String>();

	/**
	 * Menyimpan record {@link Notifikasi} "polos" (tanpa target klik/halaman, berbeda dari
	 * {@link #simpanNotifikasiHalaman} yang menambahkan {@code bukaZk}/{@code bukaJsp}) dan
	 * mengirim email/WA bila konfigurasi {@code aktfikan_pengiriman_notif} aktif. Ini anggota
	 * pertama keluarga overload {@code simpanNotif}; implementasi sesungguhnya ada di
	 * {@link #simpanNotifInternal} (dipanggil dengan {@code abaikanPengaturanNotif=false}, artinya
	 * saklar {@code aktfikan_pengiriman_notif} DIHORMATI — kebalikan dari
	 * {@link #simpanNotifUntukEmail} yang mengabaikan saklar itu demi jalur email).
	 *
	 * @param userIdsTemp    daftar user id penerima (untuk push/WA), boleh {@code null}
	 * @param recipientsTemp alamat email penerima dipisah koma, boleh {@code null}
	 * @param subject        judul notifikasi
	 * @param body           isi notifikasi (HTML)
	 * @param dataObject     entitas terkait untuk metadata {@code classData}, boleh {@code null}
	 * @param temp           lampiran opsional (digabung jadi satu PDF lewat
	 *                       {@link #jadikanSatuFilePdf} sebelum disimpan)
	 * @return record {@link Notifikasi} yang tersimpan, atau {@code null} bila
	 *         {@code aktfikan_pengiriman_notif} tidak aktif atau tidak ada penerima
	 */
	public static Notifikasi simpanNotif(JSONArray userIdsTemp, String recipientsTemp, String subject, String body,
			GeneralValueObject dataObject, File... temp) {
		JSONArray attachmentsData = null;
		return simpanNotif(userIdsTemp, recipientsTemp, subject, body, dataObject, attachmentsData, false, temp);
	}

	/** Seperti {@link #simpanNotif(JSONArray, String, String, String, GeneralValueObject, File...)}, dengan flag {@code kirimkankeWa} eksplisit untuk fan-out WhatsApp. */
	public static Notifikasi simpanNotif(JSONArray userIdsTemp, String recipientsTemp, String subject, String body,
			GeneralValueObject dataObject, boolean kirimkankeWa, File... temp) {
		JSONArray attachmentsData = null;
		return simpanNotif(userIdsTemp, recipientsTemp, subject, body, dataObject, attachmentsData, kirimkankeWa, temp);
	}

	/** Varian paling lengkap {@code simpanNotif}, dengan {@code attachmentsData} JSON tambahan di samping lampiran {@link File}. Meneruskan langsung ke {@link #simpanNotifInternal} dengan {@code abaikanPengaturanNotif=false}. */
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
	 *
	 * <p>
	 * Dipanggil secara internal oleh keluarga {@code sendMail*}/{@code sendMailLampiran*} sebelum
	 * kanal email sesungguhnya dijalankan — lihat {@link #sendMailLampiran(JSONArray, String,
	 * String, String, String, PrintStream, GeneralValueObject, JSONArray, boolean, File...)}.
	 * Delegasi ke {@link #simpanNotifInternal} dengan {@code abaikanPengaturanNotif=true}.
	 * </p>
	 *
	 * @param userIdsTemp     daftar user id penerima (untuk push/WA), boleh {@code null}
	 * @param recipientsTemp  alamat email penerima dipisah koma, boleh {@code null}
	 * @param subject         judul email
	 * @param body            isi email (HTML)
	 * @param dataObject      entitas terkait untuk metadata {@code classData}, boleh {@code null}
	 * @param attachmentsData lampiran berbentuk JSON (url+nama), boleh {@code null}
	 * @param kirimkankeWa    kirim juga push (GCP) + WhatsApp lewat {@link #kirimNotif} bila
	 *                        {@code true}
	 * @param temp            lampiran {@link File} opsional, digabung jadi satu PDF lewat
	 *                        {@link #jadikanSatuFilePdf}
	 * @return record {@link Notifikasi} yang tersimpan, atau {@code null} bila tidak ada penerima
	 *         email maupun user id
	 */
	private static Notifikasi simpanNotifUntukEmail(JSONArray userIdsTemp, String recipientsTemp, String subject,
			String body, GeneralValueObject dataObject, JSONArray attachmentsData, boolean kirimkankeWa, File... temp) {
		return simpanNotifInternal(userIdsTemp, recipientsTemp, subject, body, dataObject, attachmentsData, kirimkankeWa,
				true, temp);
	}

	/**
	 * Implementasi kanonik seluruh keluarga {@code simpanNotif}/{@code simpanNotifUntukEmail}:
	 * satu-satunya tempat yang benar-benar membangun dan menyimpan baris
	 * {@link ais.database.model.Notifikasi} untuk jalur "notifikasi polos" (tanpa target klik
	 * halaman — bandingkan dengan {@link #simpanNotifikasiHalaman} yang punya implementasi
	 * terpisah karena field tambahannya berbeda).
	 *
	 * <p>
	 * Urutan kerja: (1) bila {@code notifAktif} bernilai {@code false} — yaitu
	 * {@code abaikanPengaturanNotif=false} DAN konfigurasi {@code aktfikan_pengiriman_notif} tidak
	 * aktif — method berhenti lebih awal dan mengembalikan {@code null} tanpa menyentuh database
	 * sama sekali; (2) bila {@link ais.common.FormalisasiPesanUtil#terapkanNotifikasi()} aktif,
	 * {@code body} dibungkus jadi teks resmi lewat {@link
	 * ais.common.FormalisasiPesanUtil#bungkusFormalHtml}; (3) lampiran {@code temp} digabung jadi
	 * satu PDF; (4) userIds dideduplikasi terhadap {@link #notifSudah} berbasis kunci
	 * {@code kelas+"_"+id+"_"+userId} (CATATAN: kunci di sini TIDAK menyertakan hash subjek,
	 * berbeda dari kunci dedup di {@link #simpanNotifikasiHalaman} yang menyertakan
	 * {@code hashSubjek} — dua mekanisme dedup ini independen walaupun berbagi set
	 * {@link #notifSudah} yang sama); (5) baris {@link Notifikasi} disimpan dalam transaksi
	 * Hibernate sendiri (rollback eksplisit bila gagal, sesi selalu ditutup di {@code finally});
	 * (6) cache lonceng notifikasi ditandai kotor lewat
	 * {@link ais.common.NotifikasiCache#tandaiKotor()}; (7) bila {@code kirimkankeWa} aktif, push +
	 * WhatsApp dikirim lewat {@link #kirimNotif}. Kanal EMAIL sendiri TIDAK dikirim dari method
	 * ini — pemanggil (keluarga {@code sendMail*}) yang bertanggung jawab memanggil
	 * {@link #sendMailProcess} atau {@link #sendinblue} setelah record ini tersimpan.
	 * </p>
	 *
	 * @param userIdsTemp             daftar user id penerima, boleh {@code null}
	 * @param recipientsTemp          alamat email penerima dipisah koma, boleh {@code null}
	 * @param subject                 judul pesan
	 * @param body                    isi pesan (HTML), diformalkan bila fitur formalisasi aktif
	 * @param dataObject              entitas terkait untuk metadata {@code classData}
	 * @param attachmentsData         lampiran JSON, boleh {@code null}
	 * @param kirimkankeWa            kirim juga push/WhatsApp via {@link #kirimNotif}
	 * @param abaikanPengaturanNotif  bila {@code true}, saklar {@code aktfikan_pengiriman_notif}
	 *                                DILEWATI (dipakai jalur email lewat
	 *                                {@link #simpanNotifUntukEmail}); bila {@code false}, saklar
	 *                                itu DIHORMATI (jalur {@link #simpanNotif} biasa)
	 * @param temp                    lampiran {@link File} opsional
	 * @return record {@link Notifikasi} tersimpan, atau {@code null} bila notifikasi nonaktif atau
	 *         tidak ada penerima
	 */
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

					org.hibernate.Session session2 = null;
					org.hibernate.Transaction transaction2 = null;
					try {
						session2 = HibernateUtil.currentNativeSession();
						transaction2 = session2.beginTransaction();
						session2.save(notifikasi);
						transaction2.commit();
						transaction2 = null;
					} finally {
						if (transaction2 != null) {
							try {
								transaction2.rollback();
							} catch (Exception rollbackException) {
								ais.common.ErrorAuditUtil.record(rollbackException,
										"auto-audit(empty-catch) src/ais/delivery/email/sender/MailSender.java:simpanNotif-rollback");
							}
						}
						Common.closeNativeSessionQuietly(session2);
					}

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/delivery/email/sender/MailSender.java:503");
				}
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


	/** Seperti {@link #kirimNotif(Notifikasi, boolean, JSONObject, JSONArray, boolean, File...)} dengan {@code ubahKeterangan=true} dan {@code kirimkankeWa=false} (push saja, tanpa WhatsApp). */
	public static List<String> kirimNotif(Notifikasi notifikasi, JSONObject classData, JSONArray attachmentsData,
			File... file) throws Exception {
		return kirimNotif(notifikasi, true, classData, attachmentsData, false, file);
	}

	/** Seperti {@link #kirimNotif(Notifikasi, JSONObject, JSONArray, File...)} dengan flag {@code kirimkankeWa} eksplisit. */
	public static List<String> kirimNotif(Notifikasi notifikasi, JSONObject classData, JSONArray attachmentsData,
			boolean kirimkankeWa, File... file) throws Exception {
		return kirimNotif(notifikasi, true, classData, attachmentsData, kirimkankeWa, file);
	}

	/**
	 * Implementasi kanonik pengiriman <b>push notification</b> (lewat server relay
	 * {@code link_push_multiple_devices_notif}, default {@code dev.ecampus.id:3000}) dan,
	 * opsional, <b>WhatsApp</b> (lewat {@code Wa#kirimWaViaUltramsg}, hanya bila konfigurasi
	 * {@code aktifkan_reply_chatbot} aktif) untuk satu record {@link Notifikasi} yang SUDAH
	 * tersimpan. Dipanggil dari {@link #simpanNotifikasiHalaman} dan {@link #simpanNotifInternal}
	 * setelah baris notifikasi dibuat — tidak pernah dipanggil sebelum record tersimpan karena
	 * method ini membaca {@code notifikasi.getNama()} (daftar user id JSON) dan
	 * {@code notifikasi.getKeterangan()} (subjek+body JSON) dari objek yang diberikan.
	 *
	 * <p>
	 * Resolusi tujuan push memakai kolom {@code gcpToken} pada tiga entitas ({@link
	 * ais.database.model.Tbmuser}, {@link ais.database.model.Mahasiswa},
	 * {@link ais.database.model.sekolah.Siswa}) yang cocok dengan userId/NIM/NISN di
	 * {@code notifikasi.getNama()}; resolusi tujuan WhatsApp memakai kolom nomor HP/telepon pada
	 * entitas yang sama (termasuk HP orang tua siswa: {@code hp1ayah}/{@code hp1ibu}). Payload push
	 * dikirim ke {@code linkPost} lewat {@code curl} eksternal dengan body JSON dialirkan via
	 * STDIN ({@code --data-binary @-}), BUKAN sebagai argumen baris perintah — ini sengaja
	 * dirancang begitu karena body notifikasi panjang pernah melebihi batas {@code ARG_MAX} OS dan
	 * membuat {@link ProcessBuilder#start()} gagal dengan "Argument list too long". Pengiriman
	 * push ke server mobile dapat dimatikan lewat konfigurasi
	 * {@code aktifkan_push_multiple_devices_notif} (default TIDAK AKTIF).
	 * </p>
	 *
	 * <p>
	 * Bila {@code ubahKeterangan=true}, kolom {@code keterangan} pada {@code notifikasi} ditulis
	 * ulang untuk menyisipkan {@code attachments}; bila {@code false} (dipakai
	 * {@link #simpanNotifikasiHalaman}), {@code keterangan} yang sudah diperkaya dengan
	 * {@code classData}/{@code bukaZk}/{@code bukaJsp} TIDAK ditimpa. Kolom {@code hasil} pada
	 * {@code notifikasi} selalu ditulis dengan gabungan respons {@code curl} dari setiap batch
	 * token push.
	 * </p>
	 *
	 * @param notifikasi          record yang sudah tersimpan, dibaca+diperbarui di tempat
	 * @param ubahKeterangan      timpa kolom {@code keterangan} dengan lampiran bila {@code true}
	 * @param classData           metadata objek terkait, disisipkan ke payload push
	 * @param attachmentsDataFinal lampiran JSON siap pakai; bila {@code null}, dibangun dari
	 *                             {@code file} yang diberikan
	 * @param kirimkankeWa         kirim juga ke WhatsApp lewat Ultramsg (bila
	 *                             {@code aktifkan_reply_chatbot} aktif)
	 * @param file                 lampiran {@link File} opsional
	 * @return daftar respons mentah dari setiap panggilan {@code curl} ke server push
	 * @throws Exception diteruskan dari kegagalan parsing JSON keterangan/nama notifikasi
	 */
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

	/**
	 * Menggabungkan seluruh lampiran ber-ekstensi {@code .pdf} di antara {@code file} menjadi
	 * SATU berkas PDF (lewat {@link org.apache.pdfbox.util.PDFMergerUtility}), sementara lampiran
	 * non-PDF diteruskan apa adanya. Dipanggil di awal hampir setiap alur kirim di kelas ini
	 * ({@link #simpanNotifikasiHalaman}, {@link #simpanNotifInternal}, {@link #sendinblue}, dst.)
	 * sehingga penerima yang mendapat beberapa dokumen PDF sekaligus (mis. beberapa halaman
	 * tagihan) menerimanya sebagai satu lampiran gabungan, bukan berkas terpisah-pisah.
	 *
	 * <p>
	 * Berkas PDF gabungan baru diberi nama acak lewat {@link Common#getGeneratedBarCode()} dan
	 * ditulis ke direktori yang sama dengan PDF sumber terakhir yang diproses. Bila proses
	 * penggabungan gagal (mis. salah satu PDF sumber korup) atau berkas hasil kosong/tidak
	 * terbentuk, method jatuh kembali ({@code fallback}) mengirim seluruh PDF asli TANPA
	 * digabung, alih-alih gagal total — kegagalan digabung tidak boleh membuat lampiran hilang.
	 * </p>
	 *
	 * @param file lampiran campuran PDF dan non-PDF, boleh berisi {@code null} (diabaikan) atau
	 *             kosong
	 * @return array baru: lampiran non-PDF apa adanya + (bila ada PDF) satu PDF gabungan, atau
	 *         seluruh PDF asli bila penggabungan gagal; array kosong bila {@code file} kosong/null
	 */
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

			FileOutputStream out = null;
			try {
				out = new FileOutputStream(filePdfBaru);
				ut.setDestinationStream(out);
				ut.mergeDocuments();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/delivery/email/sender/MailSender.java:907");
			} finally {
				try {
					if (out != null) {
						out.close();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "MailSender.jadikanSatuFilePdf.close");
				}
			}

			if (filePdfBaru.exists() && filePdfBaru.length() > 0) {
				filesbaru.add(filePdfBaru);
			} else {
				filesbaru.addAll(filespdf);
			}
		}

		return filesbaru.toArray(new File[] {});
	}

	/**
	 * Implementasi kanonik privat dari seluruh keluarga {@code sendMail}/{@code sendMailLampiran}
	 * publik (14 overload, lihat javadoc di overload publik pertama untuk peta lengkapnya). Semua
	 * parameter opsional keluarga itu bermuara di sini dengan nilai final-nya masing-masing.
	 *
	 * <p>
	 * Urutan kerja: (1) tolak pengiriman lebih dulu bila {@code sender} sama dengan
	 * {@code email_tidak_boleh_kirim_dari} (daftar penolakan sederhana berbasis satu alamat, bukan
	 * daftar/pola — pengecekan hanya persis-sama, tidak case-sensitive); (2) simpan record
	 * {@link Notifikasi} lewat {@link #simpanNotifUntukEmail} (saklar
	 * {@code aktfikan_pengiriman_notif} DIABAIKAN di jalur ini — email tidak boleh berhenti hanya
	 * karena notifikasi aplikasi dimatikan); (3) pilih penyedia: Brevo/Sendinblue (
	 * {@link #sendinblue}) bila konfigurasi {@code aktfikan_pengiriman_email_menggunakan_sendinblue.com}
	 * aktif, selain itu SMTP langsung lewat {@link #sendMailProcess}, dipanggil SEKALI PER ALAMAT
	 * bila {@code recipientsTemp} berisi beberapa alamat dipisah koma (bukan satu pesan dengan
	 * banyak penerima To); (4) alamat email monitoring ({@code alamat_email_monitoring}, bila
	 * diisi) ditambahkan sebagai penerima tambahan pada jalur SMTP langsung — TIDAK pada jalur
	 * Brevo. Flag {@code akademik}/{@code tagihan} pada {@link #sendMailProcess} selalu
	 * {@code true, false} di jalur ini (bukan email tagihan) — bandingkan dengan
	 * {@link #sendMailLampiranTagihan(JSONArray, String, String, String, String, PrintStream,
	 * boolean, GeneralValueObject, JSONArray, boolean, File...) varian tagihan}.
	 * </p>
	 *
	 * @param userIds         daftar user id penerima (untuk metadata notifikasi/push)
	 * @param subject         judul email
	 * @param body            isi email (HTML)
	 * @param sender          alamat pengirim; ditolak bila cocok {@code email_tidak_boleh_kirim_dari}
	 * @param recipientsTemp  alamat email penerima dipisah koma
	 * @param out             aliran opsional untuk log debug SMTP; {@code null} berarti tidak ada
	 * @param dataObject      entitas terkait untuk metadata notifikasi
	 * @param attachmentsData lampiran JSON, boleh {@code null}
	 * @param kirimkankeWa    kirim juga push/WhatsApp lewat jalur {@link #simpanNotifUntukEmail}
	 * @param file            lampiran {@link File}, digabung jadi satu PDF sebelum dikirim
	 * @throws Exception diteruskan dari kegagalan penyimpanan notifikasi atau parsing alamat email
	 */
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

	/**
	 * Varian {@code sendMail} khusus EMAIL TAGIHAN (mis. tagihan SPP/kuliah/koperasi): berbeda
	 * dari keluarga {@code sendMailLampiran} biasa karena meneruskan {@code tagihan=true} ke
	 * {@link #sendMailProcess}, yang menggerbangi pengiriman lewat konfigurasi
	 * {@code aktfikan_pengiriman_email_tagihan} (terpisah dari gerbang
	 * {@code aktfikan_pengiriman_email_akademik} milik email non-tagihan) dan bisa opsional
	 * menampilkan messagebox hasil pengiriman ke pengguna ({@code tampilHasil}). Anggota pertama
	 * dari tiga overload; delegasi akhir ke {@link
	 * #sendMailLampiranTagihan(JSONArray, String, String, String, String, PrintStream, boolean,
	 * GeneralValueObject, JSONArray, boolean, File...) varian kanonik privat} di bawah.
	 *
	 * @param userIds        daftar user id penerima
	 * @param subject        judul email tagihan
	 * @param body           isi email tagihan (HTML)
	 * @param sender         alamat pengirim; ditolak bila cocok {@code email_tidak_boleh_kirim_dari}
	 * @param recipientsTemp alamat email penerima dipisah koma
	 * @param out            aliran opsional untuk log debug SMTP
	 * @param tampilHasil    tampilkan {@code Messagebox} hasil pengiriman ke pengguna setelah
	 *                       ~4 detik (dipakai saat dipanggil langsung dari tombol UI, bukan job
	 *                       batch)
	 * @param dataObject     entitas tagihan terkait, untuk metadata notifikasi
	 * @param file           lampiran {@link File} (mis. PDF tagihan), digabung jadi satu PDF
	 * @throws Exception diteruskan dari kegagalan penyimpanan notifikasi/pengiriman
	 */
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

	/** Seperti {@link #sendMailLampiranTagihan(JSONArray, String, String, String, String, PrintStream, boolean, GeneralValueObject, File...)}, dengan flag {@code kirimkankeWa} eksplisit untuk fan-out WhatsApp. */
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

	/**
	 * Implementasi kanonik privat {@link #sendMailLampiranTagihan(JSONArray, String, String,
	 * String, String, PrintStream, boolean, GeneralValueObject, File...)}. Sama strukturnya dengan
	 * {@link #sendMailLampiran(JSONArray, String, String, String, String, PrintStream,
	 * GeneralValueObject, JSONArray, boolean, File...)} (simpan notifikasi → pilih Brevo vs SMTP
	 * langsung → kirim per-alamat), TIGA perbedaan: (1) selalu meneruskan {@code akademik=false,
	 * tagihan=true} ke {@link #sendMailProcess} sehingga gerbang konfigurasinya
	 * {@code aktfikan_pengiriman_email_tagihan}, BUKAN {@code aktfikan_pengiriman_email_akademik};
	 * (2) TIDAK menambahkan {@code alamat_email_monitoring} ke daftar penerima (berbeda dari
	 * varian non-tagihan); (3) bila {@code tampilHasil=true}, menjadwalkan
	 * {@link Common#createDefaultTimer} yang menampilkan {@code Messagebox} berisi
	 * {@code notif.getHasil()}+{@code notif.getHasilEmail()} kira-kira 4 detik kemudian (memberi
	 * waktu tugas asinkron di {@link #MAIL_POOL} untuk menuliskan hasilnya lebih dulu — BUKAN
	 * jaminan sinkron, hanya perkiraan waktu tunggu).
	 *
	 * @param userIds         daftar user id penerima
	 * @param subject         judul email
	 * @param body            isi email (HTML)
	 * @param sender          alamat pengirim
	 * @param recipientsTemp  alamat email penerima dipisah koma
	 * @param out             aliran opsional untuk log debug SMTP
	 * @param tampilHasil     tampilkan messagebox hasil pengiriman setelah ~4 detik
	 * @param dataObject      entitas tagihan terkait
	 * @param attachmentsData lampiran JSON, boleh {@code null}
	 * @param kirimkankeWa    kirim juga push/WhatsApp
	 * @param file            lampiran {@link File}
	 * @throws Exception diteruskan dari kegagalan penyimpanan notifikasi/pengiriman
	 */
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
						ais.ui.util.MyMessageboxConfig.show(notif.getHasil() + "\n\n" + notif.getHasilEmail(), "Info Pengiriman Data",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					} else {
						ais.ui.util.MyMessageboxConfig.show("Pengiriman email sudah diproses. Data notifikasi tidak dibuat karena penerima kosong.",
								"Info Pengiriman Data", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					}
				}
			}, "Proses pengiriman data", false, 4000);
		}
	}

	/**
	 * Jalur pengiriman email alternatif lewat API transaksional Brevo (domain lama
	 * {@code sendinblue.com}, endpoint kini {@code https://api.brevo.com/v3/smtp/email}),
	 * dipakai sebagai pengganti SMTP langsung ({@link #sendMailProcess}) ketika konfigurasi
	 * {@code aktfikan_pengiriman_email_menggunakan_sendinblue.com} aktif. Dipanggil dari SEMUA
	 * keluarga {@code sendMail*}/{@code sendMailLampiran*} yang memilih Brevo, sehingga method ini
	 * harus menangani baik email biasa maupun tagihan secara identik (tidak ada pembeda
	 * akademik/tagihan di jalur Brevo, berbeda dari jalur SMTP langsung yang menggerbangi
	 * keduanya secara terpisah di {@link #sendMailProcess}).
	 *
	 * <p>
	 * Setiap alamat pada {@code recipientsTemp} (dipisah koma) dibungkus sebagai satu entri
	 * {@code messageVersions[].to[]} pada payload Brevo — sehingga secara teknis satu panggilan
	 * HTTP dapat mengirim ke banyak penerima sekaligus dengan konten identik (BUKAN
	 * personalisasi per penerima). Kunci API dibaca dari konfigurasi {@code key_sendinblue.com}.
	 * Payload dikirim lewat proses {@code curl} eksternal dengan body JSON dialirkan via STDIN
	 * ({@code --data-binary @-}), pola yang sama dan untuk alasan yang sama dengan
	 * {@link #kirimNotif}: menghindari batas {@code ARG_MAX} OS pada body email formal yang
	 * panjang. Dijalankan asinkron lewat {@link #submitEmail(Runnable)}; hasil mentah respons
	 * {@code curl} dituliskan ke {@code notifikasi.setHasilEmail(hasil)} bila {@code notifikasi}
	 * tidak {@code null}.
	 * </p>
	 *
	 * @param userIds        tidak dipakai langsung di badan method (diteruskan untuk konsistensi
	 *                       tanda tangan dengan pemanggil, dibaca lewat closure)
	 * @param subject        judul email
	 * @param body           isi email (HTML), dikirim sebagai {@code htmlContent}
	 * @param sender         alamat pengirim, dikirim sebagai {@code sender.email} dengan nama
	 *                       tampilan tetap {@code "Email Informasi"}
	 * @param recipientsTemp alamat email penerima dipisah koma; bila kosong/{@code null}, method
	 *                       berhenti lebih awal dan menulis pesan "penerima kosong" ke
	 *                       {@code notifikasi.setHasilEmail}
	 * @param out            aliran opsional; bila tidak {@code null}, payload+hasil dicetak juga
	 *                       ke sana selain ke {@code System.out}
	 * @param notifikasi     record notifikasi yang diperbarui dengan hasil pengiriman, boleh
	 *                       {@code null}
	 * @param temp           lampiran {@link File}, digabung jadi satu PDF sebelum dikirim
	 */
	private static void sendinblue(final JSONArray userIds, final String subject, final String body,
			final String sender, final String recipientsTemp, final PrintStream out, final Notifikasi notifikasi,
			File... temp) {
		if (recipientsTemp == null || recipientsTemp.trim().isEmpty()) {
			if (notifikasi != null) {
				notifikasi.setHasilEmail("Email tidak dikirim karena alamat penerima kosong.");
			}
			return;
		}
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

	/**
	 * Varian {@code sendMail} untuk pengiriman "massal ke banyak alamat sekaligus dari satu
	 * panggilan" — secara struktur SAMA dengan {@link #sendMailLampiran(JSONArray, String,
	 * String, String, String, PrintStream, GeneralValueObject, JSONArray, boolean, File...)}
	 * (simpan notifikasi → pilih Brevo vs SMTP → kirim per-alamat via {@link #sendMailProcess}),
	 * dengan SATU perbedaan penting: selalu meneruskan {@code akademik=true, tagihan=true} ke
	 * {@link #sendMailProcess}, sehingga pesan lolos gerbang konfigurasi bila SALAH SATU dari
	 * {@code aktfikan_pengiriman_email_akademik} ATAU {@code aktfikan_pengiriman_email_tagihan}
	 * aktif (bukan memerlukan keduanya) — cocok untuk pengumuman lintas kategori yang tidak murni
	 * akademik maupun murni tagihan. Anggota pertama dari tiga overload; delegasi akhir ke
	 * {@link #sendMailLampiranAll(JSONArray, String, String, String, String, PrintStream,
	 * GeneralValueObject, JSONArray, boolean, File...) varian kanonik privat} di bawah.
	 *
	 * @param userIds        daftar user id penerima
	 * @param subject        judul email
	 * @param body           isi email (HTML)
	 * @param sender         alamat pengirim; ditolak bila cocok {@code email_tidak_boleh_kirim_dari}
	 * @param recipientsTemp alamat email penerima dipisah koma
	 * @param out            aliran opsional untuk log debug SMTP
	 * @param dataObject     entitas terkait untuk metadata notifikasi
	 * @param file           lampiran {@link File}, digabung jadi satu PDF
	 * @throws Exception diteruskan dari kegagalan penyimpanan notifikasi/pengiriman
	 */
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

	/** Seperti {@link #sendMailLampiranAll(JSONArray, String, String, String, String, PrintStream, GeneralValueObject, File...)}, dengan flag {@code kirimkankeWa} eksplisit untuk fan-out WhatsApp. */
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

	/**
	 * Implementasi kanonik privat {@link #sendMailLampiranAll(JSONArray, String, String, String,
	 * String, PrintStream, GeneralValueObject, File...)}: identik strukturnya dengan
	 * {@link #sendMailLampiran(JSONArray, String, String, String, String, PrintStream,
	 * GeneralValueObject, JSONArray, boolean, File...)}, KECUALI dua hal — (1) selalu meneruskan
	 * {@code akademik=true, tagihan=true} ke {@link #sendMailProcess} (gerbang OR, lihat javadoc
	 * overload publik pertama), dan (2) TIDAK menambahkan {@code alamat_email_monitoring} ke
	 * daftar penerima pada jalur SMTP langsung (berbeda dari
	 * {@link #sendMailLampiran(JSONArray, String, String, String, String, PrintStream,
	 * GeneralValueObject, JSONArray, boolean, File...)} yang menambahkannya).
	 *
	 * @param userIds         daftar user id penerima
	 * @param subject         judul email
	 * @param body            isi email (HTML)
	 * @param sender          alamat pengirim
	 * @param recipientsTemp  alamat email penerima dipisah koma
	 * @param out             aliran opsional untuk log debug SMTP
	 * @param dataObject      entitas terkait
	 * @param attachmentsData lampiran JSON, boleh {@code null}
	 * @param kirimkankeWa    kirim juga push/WhatsApp
	 * @param file            lampiran {@link File}
	 * @throws Exception diteruskan dari kegagalan penyimpanan notifikasi/pengiriman
	 */
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

	/**
	 * Satu-satunya tempat di kelas ini yang benar-benar membuka koneksi SMTP dan mengirim email
	 * lewat JavaMail API ({@link Transport#send(Message)}) — semua kanal "SMTP langsung" (bukan
	 * Brevo) dari seluruh keluarga {@code sendMail*} bermuara ke sini, SATU PANGGILAN PER ALAMAT
	 * penerima (pemanggil yang memecah {@code recipientsTemp} yang berisi banyak alamat).
	 *
	 * <h3>Gerbang pengiriman</h3>
	 * <p>
	 * Email hanya benar-benar dikirim bila kombinasi {@code akademik}/{@code tagihan} dan
	 * konfigurasi terkait terpenuhi: {@code (akademik && aktfikan_pengiriman_email_akademik) ||
	 * (tagihan && aktfikan_pengiriman_email_tagihan)}, DAN konfigurasi umum
	 * {@code aktfikan_pengiriman_email} juga aktif (default TIDAK AKTIF). Bila salah satu gerbang
	 * tidak terpenuhi, method berhenti tanpa membuka koneksi SMTP maupun menyentuh
	 * {@code notifikasi} sama sekali — TIDAK ada percobaan kirim dan TIDAK ada pencatatan
	 * kegagalan untuk kasus ini (berbeda dari kegagalan SMTP sungguhan, yang tetap dicatat ke
	 * {@code notifikasi.setHasilEmail}).
	 * </p>
	 *
	 * <h3>Normalisasi domain</h3>
	 * <p>
	 * Dua salah ketik domain umum diperbaiki otomatis sebelum dikirim:
	 * {@code @ahoo.co}→{@code @yahoo.co} dan {@code @mail.com}→{@code @gmail.com}. Untuk host SMTP
	 * Microsoft resmi ({@code office365.com}/{@code outlook.com}), port dan properti STARTTLS
	 * dipaksa ke kombinasi yang bekerja (587 + STARTTLS + {@code auth.mechanisms=LOGIN}, SSL
	 * socket dimatikan) — kombinasi lama berbawaan SSL socket port 465 dapat mencapai server lewat
	 * relay tertentu tetapi autentikasinya ditolak 535; normalisasi ini HANYA berlaku untuk host
	 * Microsoft, penyedia lain tetap memakai konfigurasi {@code default_mail_*} apa adanya.
	 * </p>
	 *
	 * <h3>Penanganan galat autentikasi</h3>
	 * <p>
	 * Kegagalan autentikasi ({@link javax.mail.AuthenticationFailedException}, kode SMTP 535
	 * 5.7.3) diperlakukan sebagai masalah KONFIGURASI, bukan masalah pesan spesifik — ia akan
	 * gagal identik untuk SETIAP pesan berikutnya sampai kredensial/metode autentikasi dibetulkan
	 * di Pengaturan Email. Karena itu waktu kegagalan dicatat ke {@link #AUTH_GAGAL_TERAKHIR} dan
	 * pelaporan penuh ke audit diredam menjadi maksimal sekali per {@link #JEDA_LAPOR_AUTH_MS}
	 * (15 menit) — bila belum lewat jeda, pengiriman berikutnya bahkan tidak dicoba sama sekali
	 * ({@code Transport.send} dilewati) dan hanya dicatat sebagai "ditunda sementara". Ini
	 * mencegah satu kredensial SMTP yang rusak membanjiri audit dengan ribuan stack trace identik
	 * dan menenggelamkan galat lain yang lebih actionable. Kegagalan transient (koneksi/DNS,
	 * {@link com.sun.mail.smtp.SMTPSendFailedException}) hanya dicatat ke {@code System.err},
	 * TIDAK ke audit admin.
	 * </p>
	 *
	 * <p>
	 * Dijalankan asinkron lewat {@link #submitEmail(Runnable)}; hasil (sukses/gagal/pesan galat,
	 * dipotong maksimal 2000 karakter) ditulis ke {@code notifikasi.setHasilEmail} dalam transaksi
	 * Hibernate tersendiri.
	 * </p>
	 *
	 * @param subject       judul email
	 * @param body          isi email; diformalkan lewat {@link
	 *                      ais.common.FormalisasiPesanUtil#bungkusFormalHtml} bila fitur
	 *                      formalisasi email aktif
	 * @param sender        alamat pengirim (header {@code Sender}, bukan {@code From})
	 * @param recipientsTemp satu atau beberapa alamat penerima dipisah koma
	 * @param out           aliran opsional untuk mengaktifkan {@code session.setDebug(true)} dan
	 *                      mengalirkan log JavaMail ke sana
	 * @param akademik      gerbang lewat {@code aktfikan_pengiriman_email_akademik} bila
	 *                      {@code true}
	 * @param tagihan       gerbang lewat {@code aktfikan_pengiriman_email_tagihan} bila
	 *                      {@code true}
	 * @param notifikasi    record yang diperbarui dengan hasil pengiriman, boleh {@code null}
	 * @param temp          lampiran {@link File}, digabung jadi satu PDF lewat
	 *                      {@link #jadikanSatuFilePdf}
	 */
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

							// Office 365/Outlook SMTP memakai STARTTLS pada port 587. Konfigurasi lama AIS
							// berbawaan SSL socket port 465; kombinasi itu dapat mencapai server melalui
							// relay tertentu tetapi autentikasinya ditolak 535. Normalisasi hanya untuk host
							// Microsoft resmi; penyedia lain tetap mengikuti konfigurasi lama apa adanya.
							String hostKecil = mailhost == null ? "" : mailhost.trim().toLowerCase();
							boolean smtpMicrosoft = hostKecil.indexOf("office365.com") >= 0
									|| hostKecil.indexOf("outlook.com") >= 0;
							if (smtpMicrosoft) {
								port = "587";
								soketPort = "587";
								soketClass = "";
								starttls = "true";
								ssl = "false";
							}

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
							if (smtpMicrosoft) {
								props.put("mail.smtp.starttls.required", "true");
								props.put("mail.smtp.auth.mechanisms", "LOGIN");
							}
							if (!protocols.trim().isEmpty()) {
								props.put("mail.smtp.ssl.protocols", protocols); // enable STARTTLS
							}

							System.out.println("props -> " + props);

							final String emailUsername = Common
									.getKonfigurasi("default_email_username", "").getNilai();
							final String emailPassword = Common
									.getKonfigurasi("default_email_password", "").getNilai();
							if (emailUsername == null || emailUsername.trim().isEmpty()
									|| emailPassword == null || emailPassword.length() == 0) {
								throw new IllegalStateException(
										"Username/password SMTP belum dikonfigurasi pada Pengaturan Email.");
							}
							Session session = Session.getInstance(props, new javax.mail.Authenticator() {
								protected PasswordAuthentication getPasswordAuthentication() {
									return new PasswordAuthentication(emailUsername.trim(), emailPassword);
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

							long authGagal = AUTH_GAGAL_TERAKHIR.get();
							if (authGagal > 0L && System.currentTimeMillis() - authGagal < JEDA_LAPOR_AUTH_MS) {
								hasil = "Kirim ke " + recipients
										+ " ditunda sementara setelah autentikasi SMTP ditolak";
							} else {
								Transport.send(message);
								hasil = "Kirim ke " + recipients + " sukses";
							}

						} catch (Exception e) {

							hasil = "Kirim ke " + recipients + " gagal " + e.getMessage();

							if (out != null) {
								e.printStackTrace(out);
							} else {
								// Kegagalan AUTENTIKASI (535 5.7.3) adalah masalah KONFIGURASI, bukan
								// masalah pesan ini: ia gagal identik untuk setiap pesan sampai kredensial
								// atau metode autentikasinya dibetulkan. Mencatat stack penuh per pesan
								// membanjiri audit dan menenggelamkan galat lain. Tetap DILAPORKAN, hanya
								// diredam jadi sekali per selang waktu; pengiriman tetap dihitung gagal.
								if (e instanceof javax.mail.AuthenticationFailedException) {
									long sekarang = System.currentTimeMillis();
									AUTH_GAGAL_TERAKHIR.set(sekarang);
									hasil = "Kirim ke " + recipients + " gagal: autentikasi SMTP ditolak. "
											+ "Periksa username/password dan pastikan SMTP AUTH diaktifkan pada penyedia email.";
									System.err.println("[MailSender] " + hasil + " Server: " + e.getMessage());
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
										session2.clear();
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

	/**
	 * <b>Kode uji coba manual peninggalan — bukan bagian dari alur aplikasi.</b> Tidak dipanggil
	 * dari kode aplikasi mana pun (dikonfirmasi via pencarian referensi di seluruh {@code src/});
	 * satu-satunya cara method ini berjalan adalah dieksekusi langsung sebagai entry point Java
	 * ({@code java ais.delivery.email.sender.MailSender}) dari command line, yang dalam praktiknya
	 * tidak pernah terjadi di lingkungan produksi/deployment web AIS.
	 *
	 * <p>
	 * <b>PERINGATAN KEAMANAN:</b> method ini menanam kunci API Mailgun secara langsung di kode
	 * sumber (bukan dibaca dari konfigurasi runtime seperti jalur produksi
	 * {@link #sendMailProcess}), untuk akun {@code postmaster@unsika.ac.id}. Dokumentasi ini
	 * sengaja TIDAK menghapus kredensial tersebut — keputusan menghapus/merotasi kredensial di
	 * luar cakupan pekerjaan dokumentasi murni dan sebaiknya ditinjau terlebih dahulu apakah kunci
	 * tersebut masih aktif di sisi Mailgun sebelum kode ini disentuh. Lihat juga peringatan serupa
	 * di dokumentasi paket {@link ais.delivery.email.sender package-info}. JANGAN jadikan method
	 * ini contoh pola yang benar untuk mengirim email — pola yang benar ada di
	 * {@link #sendMailProcess}, yang membaca kredensial via
	 * {@code Common.getKonfigurasi("default_email_username"/"default_email_password", ...)}.
	 * </p>
	 *
	 * @param args tidak dipakai
	 */
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
