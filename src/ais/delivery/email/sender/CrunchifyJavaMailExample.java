package ais.delivery.email.sender;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * @author Crunchify.com
 *
 * <p>
 * Contoh mandiri JavaMail API bergaya tutorial yang disalin dari artikel publik Crunchify.com,
 * mendemonstrasikan pengiriman email lewat SMTP Gmail (port 587, STARTTLS) dengan langkah-langkah
 * diberi label komentar "Step1"/"Step2"/"Step3" khas materi belajar. Sama seperti
 * {@link SendEmail} dan {@link SimpleMail}, kelas ini <b>tidak pernah dipanggil dari bagian lain
 * aplikasi AIS</b> (dikonfirmasi lewat pencarian referensi ke {@code CrunchifyJavaMailExample} di
 * seluruh {@code src/} — kelas ini hanya merujuk dirinya sendiri); satu-satunya cara menjalankannya
 * adalah {@link #main(String[])} langsung dari command line. Lihat dokumentasi paket
 * {@link ais.delivery.email.sender package-info} untuk peta lengkap kelas contoh serupa dan
 * perbandingannya dengan mesin produksi {@link MailSender}.
 * </p>
 *
 * <h2>PERINGATAN KEAMANAN — kredensial Gmail tertanam</h2>
 * <p>
 * Method {@link #kirim()} dan {@link #generateAndSendEmail()} sama-sama menanam KATA SANDI akun
 * Gmail ({@code zishof@gmail.com}) dalam bentuk teks polos langsung di panggilan
 * {@code transport.connect(...)}, sudah ter-commit ke riwayat SVN repositori ini. Dokumentasi ini
 * SENGAJA TIDAK menghapus atau mengubah kredensial tersebut — keputusan menghapus/merotasi berada
 * di luar cakupan pekerjaan dokumentasi murni dan sebaiknya ditinjau lebih dulu oleh pemilik akun
 * apakah kata sandi tersebut (atau App Password terkait, mengingat Gmail modern umumnya
 * mewajibkan App Password untuk login aplikasi pihak ketiga) masih aktif — bila aktif, SANGAT
 * disarankan segera dirotasi/dicabut dari panel keamanan akun Google mengingat sudah lama berada
 * di riwayat repositori dalam bentuk terbaca. JANGAN jadikan pola penyimpanan kredensial pada
 * kelas ini sebagai contoh yang benar untuk fitur baru — pola produksi yang benar ada di
 * {@link MailSender#sendMailProcess}, yang membaca kredensial dari konfigurasi runtime lewat
 * {@code Common.getKonfigurasi("default_email_username"/"default_email_password", ...)}.
 * </p>
 */

public class CrunchifyJavaMailExample {

	/** Properti transport SMTP (port, auth, STARTTLS) dibagi statis antar-method contoh ini. */
	static Properties mailServerProperties;
	/** Sesi JavaMail aktif, dibuat ulang setiap {@link #kirim()}/{@link #generateAndSendEmail()} dipanggil. */
	static Session getMailSession;
	/** Pesan yang sedang disusun; nama field ("generateMailMessage") mengikuti gaya penamaan tutorial sumber, bukan konvensi AIS. */
	static MimeMessage generateMailMessage;

	/**
	 * Menjalankan {@link #generateAndSendEmail()} lalu mencetak pesan sukses ke stdout. Satu-satunya
	 * cara kelas ini benar-benar dieksekusi; tidak dipanggil dari bagian lain aplikasi AIS.
	 *
	 * @param args tidak dipakai
	 * @throws AddressException   bila alamat pengirim/penerima yang ditulis tetap di kode tidak
	 *                             valid (dalam praktiknya tidak akan terjadi karena nilainya konstan)
	 * @throws MessagingException diteruskan dari kegagalan koneksi/autentikasi/pengiriman SMTP
	 */
	public static void main(String args[]) throws AddressException,
			MessagingException {
		generateAndSendEmail();
		System.out
				.println("\n\n ===> Your Java Program has just sent an Email successfully. Check your email..");
	}

	/**
	 * Identik dengan {@link #generateAndSendEmail()} (kode isinya diduplikasi persis, bukan
	 * delegasi) — mengirim satu email HTML tetap ("Greetings from Crunchify..") ke satu alamat
	 * penerima tetap ({@code fauzioke2003@gmail.com}) lewat SMTP Gmail dengan kredensial tertanam.
	 * Method ini tidak dipanggil dari {@link #main(String[])} (yang memanggil
	 * {@link #generateAndSendEmail()}) maupun dari kode lain di AIS — kemungkinan ditinggalkan
	 * sebagai varian nama method alternatif yang menyesuaikan konvensi penamaan Indonesia
	 * ("kirim") dibanding nama asli tutorial ("generateAndSendEmail").
	 *
	 * @throws AddressException   bila alamat pengirim/penerima tidak valid
	 * @throws MessagingException diteruskan dari kegagalan koneksi/autentikasi/pengiriman SMTP
	 *                             (termasuk kegagalan autentikasi bila kata sandi tertanam sudah
	 *                             tidak berlaku)
	 */
	public static void kirim() throws AddressException,
			MessagingException {

		// Step1
		System.out.println("\n 1st ===> setup Mail Server Properties..");
		mailServerProperties = System.getProperties();
		mailServerProperties.put("mail.smtp.port", "587");
		mailServerProperties.put("mail.smtp.auth", "true");
		mailServerProperties.put("mail.smtp.starttls.enable", "true");
		System.out
				.println("Mail Server Properties have been setup successfully..");

		// Step2
		System.out.println("\n\n 2nd ===> get Mail Session..");
		getMailSession = Session.getDefaultInstance(mailServerProperties, null);
		getMailSession.setDebug(true);
		generateMailMessage = new MimeMessage(getMailSession);
		generateMailMessage.setFrom(new InternetAddress("zishof@gmail.com"));
		generateMailMessage.addRecipient(Message.RecipientType.TO,
				new InternetAddress("fauzioke2003@gmail.com"));
		generateMailMessage.setSubject("Greetings from Crunchify..");
		String emailBody = "Test email by Crunchify.com JavaMail API example. "
				+ "<br><br> Regards, <br>Crunchify Admin";
		generateMailMessage.setContent(emailBody, "text/html");
		System.out.println("Mail Session has been created successfully..");

		// Step3
		System.out.println("\n\n 3rd ===> Get Session and Send mail");
		Transport transport = getMailSession.getTransport("smtp");

		// Enter your correct gmail UserID and Password
		// if you have 2FA enabled then provide App Specific Password
		transport.connect("smtp.gmail.com", "zishof@gmail.com", "Yani211171");
		transport.sendMessage(generateMailMessage,
				generateMailMessage.getAllRecipients());
		transport.close();
	}

	/**
	 * Implementasi sesungguhnya di balik {@link #main(String[])}: menyusun properti SMTP Gmail
	 * (port 587 + STARTTLS), membuat {@link MimeMessage} HTML tetap ("Greetings from
	 * Crunchify..") ke penerima tetap, lalu membuka koneksi lewat
	 * {@code transport.connect("smtp.gmail.com", "zishof@gmail.com", <kata sandi tertanam>)} dan
	 * mengirimkannya. Kode isinya identik persis dengan {@link #kirim()} (duplikasi, bukan
	 * dipanggil satu sama lain) — lihat peringatan keamanan pada javadoc kelas
	 * {@link CrunchifyJavaMailExample} soal kata sandi Gmail yang tertanam di method ini.
	 *
	 * @throws AddressException   bila alamat pengirim/penerima tidak valid
	 * @throws MessagingException diteruskan dari kegagalan koneksi/autentikasi/pengiriman SMTP
	 */
	public static void generateAndSendEmail() throws AddressException,
			MessagingException {

		// Step1
		System.out.println("\n 1st ===> setup Mail Server Properties..");
		mailServerProperties = System.getProperties();
		mailServerProperties.put("mail.smtp.port", "587");
		mailServerProperties.put("mail.smtp.auth", "true");
		mailServerProperties.put("mail.smtp.starttls.enable", "true");
		System.out
				.println("Mail Server Properties have been setup successfully..");

		// Step2
		System.out.println("\n\n 2nd ===> get Mail Session..");
		getMailSession = Session.getDefaultInstance(mailServerProperties, null);
		getMailSession.setDebug(true);
		generateMailMessage = new MimeMessage(getMailSession);
		generateMailMessage.setFrom(new InternetAddress("zishof@gmail.com"));
		generateMailMessage.addRecipient(Message.RecipientType.TO,
				new InternetAddress("fauzioke2003@gmail.com"));
		generateMailMessage.setSubject("Greetings from Crunchify..");
		String emailBody = "Test email by Crunchify.com JavaMail API example. "
				+ "<br><br> Regards, <br>Crunchify Admin";
		generateMailMessage.setContent(emailBody, "text/html");
		System.out.println("Mail Session has been created successfully..");

		// Step3
		System.out.println("\n\n 3rd ===> Get Session and Send mail");
		Transport transport = getMailSession.getTransport("smtp");

		// Enter your correct gmail UserID and Password
		// if you have 2FA enabled then provide App Specific Password
		transport.connect("smtp.gmail.com", "zishof@gmail.com", "Yani211171");
		transport.sendMessage(generateMailMessage,
				generateMailMessage.getAllRecipients());
		transport.close();
	}
}