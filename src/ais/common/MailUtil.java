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
 * <b>Riwayat keamanan (DIPERBAIKI 2026-09-01):</b> konstanta {@code USER}/{@code PASSWORD}
 * sebelumnya berisi kredensial akun email ({@code zishof@mail.eakademik.id} beserta kata
 * sandinya) yang ditulis langsung dan tetap (hardcoded) di kode sumber. Kelas ini terkonfirmasi
 * kode mati (tidak dipanggil dari bagian lain aplikasi AIS), tetapi kata sandi tetap tereskpos
 * di riwayat repositori selama bertahun-tahun. Kredensial literal itu sudah DIHAPUS — {@link
 * #USER}/{@link #PASSWORD} kini dibaca dari properti sistem JVM ({@code -Dmailutil.user=...
 * -Dmailutil.password=...}) saat class dimuat, dan {@link #sendMail()} melempar
 * {@link IllegalStateException} bila salah satunya belum diisi lewat flag tersebut ketika kelas
 * ini dijalankan manual. <b>Tindak lanjut yang TETAP diperlukan di luar perubahan kode ini:</b>
 * kata sandi yang sebelumnya tertanam harus dianggap bocor dan SEBAIKNYA dirotasi di sisi
 * penyedia email {@code mail.eakademik.id} bila akun tersebut masih aktif.
 * </p>
 */
public class MailUtil {

    /** Nama host server SMTP keluar yang dipakai untuk uji coba pengiriman. */
    public static final String SMTP_OUT_SERVER = "mail.eakademik.id";
    /**
     * Alamat email/username autentikasi SMTP, WAJIB disuplai lewat properti sistem
     * {@code -Dmailutil.user=...} saat menjalankan kelas ini secara manual — tidak ada nilai
     * default tertanam (lihat riwayat keamanan pada javadoc kelas).
     */
    public static final String USER = System.getProperty("mailutil.user", "");
    /**
     * Kata sandi akun {@link #USER}, WAJIB disuplai lewat properti sistem
     * {@code -Dmailutil.password=...} — tidak ada nilai default tertanam (lihat riwayat
     * keamanan pada javadoc kelas).
     */
    public static final String PASSWORD = System.getProperty("mailutil.password", "");

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
     * @throws IllegalStateException bila {@link #USER}/{@link #PASSWORD} belum diisi lewat
     *                    properti sistem {@code -Dmailutil.user}/{@code -Dmailutil.password}
     * @throws Exception diteruskan apa adanya dari kegagalan JavaMail (mis. autentikasi SMTP
     *                    gagal, host tidak terjangkau, atau alamat email tidak valid)
     */
    public static void sendMail() throws Exception {
        if (USER.trim().isEmpty() || PASSWORD.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Kredensial SMTP belum diisi. Jalankan dengan -Dmailutil.user=... -Dmailutil.password=...");
        }
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