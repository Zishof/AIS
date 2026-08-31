package ais.delivery.email.sender;

//File Name SendEmail.java

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Contoh mandiri "hello world" JavaMail API tanpa autentikasi SMTP — berasal dari masa awal
 * proyek, mendemonstrasikan cara paling dasar menyusun dan mengirim satu {@link MimeMessage} lewat
 * host SMTP yang diasumsikan menerima relay tanpa login (properti {@code mail.smtp.host} diisi
 * begitu saja, tidak ada {@code mail.smtp.auth}). Kelas ini <b>tidak pernah dipanggil dari bagian
 * lain aplikasi AIS</b> — dikonfirmasi lewat pencarian referensi ke seluruh nama kelasnya di
 * {@code src/}; satu-satunya jalan menjalankannya adalah mengeksekusi {@link #main(String[])}
 * langsung dari command line, yang tidak terjadi pada deployment web AIS yang sesungguhnya. Lihat
 * dokumentasi paket {@link ais.delivery.email.sender package-info} untuk peta lengkap kelas contoh
 * serupa ({@link SimpleMail}, {@link CrunchifyJavaMailExample}) dan perbandingannya dengan mesin
 * produksi {@link MailSender}.
 *
 * <p>
 * Nilai {@code to}, {@code from}, dan {@code host} ditulis langsung di kode (bukan dibaca dari
 * konfigurasi), termasuk alamat email pribadi milik salah satu pengembang proyek
 * ({@code fauzioke2003@gmail.com}) sebagai penerima uji coba tetap. Berbeda dari
 * {@link SimpleMail}/{@link CrunchifyJavaMailExample}, kelas ini TIDAK menanam kata sandi apa pun
 * (host {@code ecampus.id} yang diasumsikan menerima tanpa autentikasi), sehingga tidak membawa
 * risiko kebocoran kredensial — hanya kebocoran alamat email pribadi ke riwayat sumber. Kelas ini
 * dipertahankan dalam repositori sebagai referensi historis/contoh minimal JavaMail API, bukan
 * sebagai kode yang siap disalin untuk fitur baru; pola produksi yang benar untuk mengirim email
 * di AIS ada di {@link MailSender#sendMailProcess}, yang membaca host/kredensial dari konfigurasi
 * runtime ({@code Common.getKonfigurasi}) dan menangani gerbang aktif/nonaktif, dedup notifikasi,
 * serta audit hasil pengiriman — tiga hal yang sama sekali tidak dilakukan kelas sederhana ini.
 * </p>
 */
public class SendEmail
{
/**
 * Menyusun satu {@link MimeMessage} berisi teks tetap ("This is actual message") dan mencoba
 * mengirimkannya lewat {@link Transport#send(Message)} ke alamat penerima yang ditulis langsung
 * di kode ({@code fauzioke2003@gmail.com}). Kegagalan pengiriman ({@link MessagingException})
 * ditangkap, dicetak ke {@code stderr}, dan dicatat lewat
 * {@code ais.common.ErrorAuditUtil#record} — TIDAK dilempar ulang, sehingga proses {@code main}
 * akan tetap keluar dengan status normal (0) walau pengiriman gagal.
 *
 * @param args tidak dipakai
 */
public static void main(String [] args)
{
   // Recipient's email ID needs to be mentioned.
   String to = "fauzioke2003@gmail.com";

   // Sender's email ID needs to be mentioned
   String from = "no-reply@ecampus.id";

   // Assuming you are sending email from localhost
   String host = "ecampus.id";

   // Get system properties
   Properties properties = System.getProperties();

   // Setup mail server
   properties.setProperty("mail.smtp.host", host);

   // Get the default Session object.
   Session session = Session.getDefaultInstance(properties);

   try{
      // Create a default MimeMessage object.
      MimeMessage message = new MimeMessage(session);

      // Set From: header field of the header.
      message.setFrom(new InternetAddress(from));

      // Set To: header field of the header.
      message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

      // Set Subject: header field
      message.setSubject("This is the Subject Line!");

      // Now set the actual message
      message.setText("This is actual message");

      // Send message
//      transport.connect("mail.zishof.com", "fauzi@zishof.com",
//				"jangannakal12");
      Transport.send(message);
      System.out.println("Sent message successfully....");
   }catch (MessagingException mex) {
      mex.printStackTrace(); ais.common.ErrorAuditUtil.record(mex, "auto-audit src/ais/delivery/email/sender/SendEmail.java:58");
   }
}
}
