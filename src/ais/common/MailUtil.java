package ais.common;

import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Utilitas kirim email berdiri sendiri (standalone) yang memakai JavaMail API secara langsung
 * dengan konfigurasi SMTP dan penerima yang <b>ditulis tetap (hardcoded)</b> di kode sumber,
 * ditujukan untuk uji coba konektivitas SMTP satu domain tertentu ({@code mail.eakademik.id})
 * dan bukan untuk dipakai sebagai jalur pengiriman email produksi bagi pengguna aplikasi.
 *
 * <p>
 * <b>Perbandingan dengan {@code ais.delivery.email.sender.MailSender}</b> (mesin pengiriman
 * email/notifikasi resmi AIS, lihat javadoc kelas tersebut untuk detail lengkap): {@code
 * MailSender} adalah pusat pengiriman produksi yang menangani banyak kanal (email, WhatsApp,
 * push), mendukung banyak penerima dinamis, lampiran, deduplikasi notifikasi, integrasi
 * Brevo/SendinBlue, konfigurasi server yang dibaca dari database ({@code Konfigurasi}), serta
 * dijalankan lewat pool thread khusus untuk membatasi konkurensi SMTP. Kelas {@code MailUtil}
 * ini jauh lebih sederhana: satu method {@link #sendMail()} yang mengirim SATU email uji coba
 * dengan subjek, isi, dan alamat pengirim/penerima yang seluruhnya tertulis tetap di kode,
 * dijalankan langsung secara sinkron tanpa pool thread maupun pencatatan riwayat notifikasi ke
 * database. Kelas ini tidak dipanggil dari alur bisnis AIS yang sesungguhnya; sifatnya adalah
 * alat bantu pengembang untuk memverifikasi bahwa kredensial dan koneksi SMTP suatu domain
 * masih berfungsi.
 * </p>
 *
 * <p>
 * <b>PERINGATAN KEAMANAN:</b> konstanta {@link #USER} dan {@link #PASSWORD} berisi kredensial
 * akun email (alamat {@code zishof@mail.eakademik.id} beserta kata sandinya) yang ditulis
 * langsung dan tetap (hardcoded) di kode sumber ini, bukan dibaca dari berkas konfigurasi atau
 * secret store. Bila akun tersebut masih aktif di server produksi ({@code mail.eakademik.id}),
 * ini merupakan kebocoran kredensial nyata di dalam repositori — siapa pun dengan akses baca ke
 * kode sumber dapat memakai akun email tersebut. Method {@link #sendMail()} juga mengirim email
 * uji ke alamat penerima yang ditulis tetap di kode. Javadoc ini TIDAK mengubah nilai-nilai
 * tersebut sesuai instruksi; lihat ringkasan laporan terkait untuk detail lokasi baris agar
 * dapat ditindaklanjuti (mis. pencabutan/penggantian kata sandi serta pemindahan kredensial ke
 * luar kode sumber).
 * </p>
 */
public class MailUtil {

    /** Nama host server SMTP keluar yang dipakai untuk uji coba pengiriman. */
    public static final String SMTP_OUT_SERVER = "mail.eakademik.id";
    /**
     * Alamat email/username autentikasi SMTP yang ditulis tetap di kode sumber.
     * <b>Kredensial tertanam — lihat peringatan keamanan pada javadoc kelas.</b>
     */
    public static final String USER = "zishof@mail.eakademik.id"; // godaddy domain
    /**
     * Kata sandi akun {@link #USER} yang ditulis tetap di kode sumber.
     * <b>Kredensial tertanam — lihat peringatan keamanan pada javadoc kelas.</b>
     */
    public static final String PASSWORD = "zishof";

    /**
     * Titik masuk uji coba manual: memanggil {@link #sendMail()} untuk mengirim satu email
     * percobaan, dan bila terjadi kegagalan, exception ditangani lewat
     * {@link Common#tampilErrorJikaAdmin(Exception)} (menampilkan detail error hanya bila
     * pemanggil berperan sebagai admin) alih-alih dibiarkan menggagalkan proses.
     *
     * @param args argumen baris perintah, tidak dipakai
     */
    public static void main(String[] args) {
        try {
            sendMail();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Common.tampilErrorJikaAdmin(e);
        }
    }

    /**
     * Mengirim satu email HTML percobaan secara sinkron lewat SMTP memakai JavaMail API murni
     * (tanpa melalui mesin {@code MailSender}). Properti sesi ({@code mail.transport.protocol},
     * {@code mail.host}, autentikasi, port {@code 9025}) diatur langsung ke objek
     * {@link System#getProperties()} (properti sistem proses, bukan objek {@link Properties}
     * lokal yang berdiri sendiri), subjek dan isi pesan ditulis tetap di kode ({@code "Hi Test"}
     * / {@code "Hi clay"}), dan penerima juga ditulis tetap di kode. Koneksi ke server SMTP
     * dibuat dan ditutup sepenuhnya di dalam method ini (connect → kirim → close), tanpa
     * penanganan retry maupun pencatatan hasil pengiriman ke mana pun.
     *
     * @throws Exception diteruskan apa adanya dari kegagalan JavaMail (mis. autentikasi SMTP
     *                    gagal, host tidak terjangkau, atau alamat email tidak valid)
     */
    public static void sendMail() throws Exception {
        Properties props = System.getProperties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.host", SMTP_OUT_SERVER);
 
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "9025");
        props.setProperty("mail.user", USER);
        props.setProperty("mail.password", PASSWORD);
 
        Session mailSession = Session.getDefaultInstance(props, null);
        mailSession.setDebug(false);
        Transport transport = mailSession.getTransport("smtp");
        MimeMessage message = new MimeMessage(mailSession);
        message.setSentDate(new java.util.Date());
        message.setSubject("Hi Test");
        message.setFrom(new InternetAddress(USER));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress("fauzioke2003@gmail.com"));
        MimeMultipart multipart = new MimeMultipart("related");
 
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent("Hi clay", "text/html");
 
        multipart.addBodyPart(messageBodyPart);
        message.setContent(multipart);
 
        transport.connect(SMTP_OUT_SERVER, USER, PASSWORD);
        transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
        transport.close();
    }
}