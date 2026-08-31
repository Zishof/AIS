package ais.delivery.email.sender;

import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;

import java.util.Properties;

/**
 * Contoh mandiri JavaMail API dengan autentikasi SMTP manual ({@link Authenticator}) — berasal
 * dari masa awal proyek, mendemonstrasikan pola kirim email lewat akun SMTP berkredensial memakai
 * server lama {@code mail.zishof.com}. Sama seperti {@link SendEmail} dan
 * {@link CrunchifyJavaMailExample}, kelas ini <b>tidak pernah dipanggil dari bagian lain aplikasi
 * AIS</b> (dikonfirmasi lewat pencarian referensi ke {@code SimpleMail.} di seluruh {@code src/} —
 * baik method {@link #kirim(String, String, String)} maupun {@link #test()} tidak punya pemanggil
 * eksternal); satu-satunya jalan menjalankannya adalah {@link #main(String[])} langsung dari
 * command line. Lihat dokumentasi paket {@link ais.delivery.email.sender package-info} untuk peta
 * lengkap kelas contoh serupa dan perbandingannya dengan mesin produksi {@link MailSender}.
 *
 * <h2>PERINGATAN KEAMANAN — kredensial tertanam</h2>
 * <p>
 * Konstanta {@link #SMTP_AUTH_USER} dan {@link #SMTP_AUTH_PWD} di bawah berisi alamat email dan
 * KATA SANDI dalam bentuk teks polos, ditulis langsung di kode sumber yang sudah ter-commit ke
 * riwayat SVN repositori ini. Dokumentasi ini SENGAJA TIDAK menghapus atau mengubah kredensial
 * tersebut — keputusan menghapus/merotasi kredensial berada di luar cakupan pekerjaan dokumentasi
 * murni dan sebaiknya ditinjau lebih dulu oleh pemilik akun {@code fauzi@zishof.com} apakah kata
 * sandi tersebut masih aktif di penyedia (bila aktif, SANGAT disarankan segera dirotasi mengingat
 * sudah lama berada di riwayat repositori dalam bentuk terbaca). JANGAN jadikan pola penyimpanan
 * kredensial pada kelas ini sebagai contoh yang benar untuk fitur baru — pola produksi yang benar
 * ada di {@link MailSender#sendMailProcess}, yang membaca kredensial SMTP dari konfigurasi runtime
 * lewat {@code Common.getKonfigurasi("default_email_username"/"default_email_password", ...)},
 * bukan dari konstanta di kode sumber.
 * </p>
 */
public class SimpleMail {

	/** Host SMTP server lama (peninggalan) yang dipakai contoh ini — bukan host produksi AIS saat ini. */
	private static final String SMTP_HOST_NAME = "mail.zishof.com";
	/** Alamat akun SMTP untuk autentikasi — lihat peringatan keamanan pada javadoc kelas {@link SimpleMail}. */
	private static final String SMTP_AUTH_USER = "fauzi@zishof.com";
	/** Kata sandi akun SMTP dalam bentuk teks polos — lihat peringatan keamanan pada javadoc kelas {@link SimpleMail}. */
	private static final String SMTP_AUTH_PWD = "jangannakal12";

	/**
	 * Menjalankan {@link #test()} — mengirim satu email uji coba teks-polos ke alamat email
	 * pribadi pengembang yang ditulis langsung di kode. Ini adalah satu-satunya cara kelas ini
	 * dijalankan; tidak dipanggil dari bagian lain aplikasi.
	 *
	 * @param args tidak dipakai
	 * @throws Exception diteruskan apa adanya dari {@link #test()} (mis. kegagalan koneksi SMTP
	 *                    atau autentikasi ditolak)
	 */
	public static void main(String[] args) throws Exception {
		new SimpleMail().test();
	}

	/**
	 * Mengirim satu email HTML ke satu atau beberapa {@code recipients} (dipisah koma) lewat akun
	 * SMTP {@link #SMTP_AUTH_USER}/{@link #SMTP_AUTH_PWD} pada host {@link #SMTP_HOST_NAME},
	 * dengan alamat pengirim tetap {@code no-reply@ecampus.id} (BERBEDA dari akun autentikasi —
	 * pola "mail-from" berbeda dari akun SMTP login ini umum dipakai penyedia yang mengizinkan
	 * relay atas nama domain lain, tetapi bergantung sepenuhnya pada kebijakan penyedia
	 * {@code mail.zishof.com} apakah diizinkan). Method ini TIDAK pernah dipanggil dari kode
	 * aplikasi lain di AIS — bandingkan dengan pola produksi di
	 * {@link MailSender#sendMailProcess} yang menjadi jalur nyata pengiriman email aplikasi.
	 *
	 * @param subject    judul email
	 * @param content    isi email, dikirim sebagai {@code text/html}
	 * @param recipients satu alamat, atau beberapa alamat dipisah koma (dideteksi lewat
	 *                   {@code recipients.indexOf(',') > 0})
	 * @throws Exception diteruskan apa adanya dari kegagalan koneksi/autentikasi/pengiriman SMTP
	 *                    (tidak ditangkap sama sekali di method ini)
	 */
	public static void kirim(String subject, String content, String recipients)
			throws Exception {
		Properties props = new Properties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.host", SMTP_HOST_NAME);
		props.put("mail.smtp.auth", "true");

		Authenticator auth = new SMTPAuthenticator();
		Session mailSession = Session.getDefaultInstance(props, auth);
		// uncomment for debugging infos to stdout
		mailSession.setDebug(false);
		Transport transport = mailSession.getTransport();

		MimeMessage message = new MimeMessage(mailSession);
		message.setSubject(subject);
		message.setContent(content, "text/html");
		message.setFrom(new InternetAddress("no-reply@ecampus.id"));
		if (recipients.indexOf(',') > 0)
			message.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(recipients));
		else
			message.setRecipient(Message.RecipientType.TO, new InternetAddress(
					recipients));

		transport.connect();
		transport.sendMessage(message,
				message.getRecipients(Message.RecipientType.TO));
		transport.close();
	}

	/**
	 * Sama seperti {@link #kirim(String, String, String)}, tetapi khusus mengirim SATU email uji
	 * coba berjudul tetap ("Lupa Password") berisi teks polos ("This is a test") ke satu alamat
	 * penerima tetap ({@code fauzioke2003@gmail.com}, alamat pribadi pengembang, ditulis langsung
	 * di kode). Judul "Lupa Password" menunjukkan kelas ini kemungkinan pernah dipakai sebagai
	 * purwarupa untuk fitur forgot-password sebelum digantikan oleh
	 * {@link MailHelper}/{@link MailSender} yang menjadi implementasi produksi sesungguhnya —
	 * tidak ada pemanggilan nyata ke method ini dari kode aplikasi manapun saat ini.
	 *
	 * @throws Exception diteruskan apa adanya dari kegagalan koneksi/autentikasi/pengiriman SMTP
	 */
	public void test() throws Exception {
		Properties props = new Properties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.host", SMTP_HOST_NAME);
		props.put("mail.smtp.auth", "true");

		Authenticator auth = new SMTPAuthenticator();
		Session mailSession = Session.getDefaultInstance(props, auth);
		// uncomment for debugging infos to stdout
		mailSession.setDebug(false);
		Transport transport = mailSession.getTransport();

		MimeMessage message = new MimeMessage(mailSession);
		message.setSubject("Lupa Password");
		message.setContent("This is a test", "text/plain");
		message.setFrom(new InternetAddress("no-reply@ecampus.id"));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(
				"fauzioke2003@gmail.com"));

		transport.connect();
		transport.sendMessage(message,
				message.getRecipients(Message.RecipientType.TO));
		transport.close();
	}

	/**
	 * Penyedia kredensial SMTP statis untuk JavaMail — mengembalikan pasangan
	 * {@link #SMTP_AUTH_USER}/{@link #SMTP_AUTH_PWD} yang sama untuk setiap permintaan autentikasi,
	 * tanpa memandang konteks (tidak menerima parameter/host/port dari pemanggil). Ini adalah pola
	 * {@link Authenticator} paling sederhana yang mungkin di JavaMail API; pola produksi AIS di
	 * {@link MailSender#sendMailProcess} memakai anonymous {@code Authenticator} serupa tetapi
	 * dengan kredensial dibaca dari konfigurasi runtime, bukan konstanta kelas.
	 */
	private static class SMTPAuthenticator extends javax.mail.Authenticator {
		public PasswordAuthentication getPasswordAuthentication() {
			String username = SMTP_AUTH_USER;
			String password = SMTP_AUTH_PWD;
			return new PasswordAuthentication(username, password);
		}
	}
}