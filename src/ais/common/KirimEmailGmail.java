package ais.common;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Potongan kode contoh/uji-coba mandiri (bukan bagian dari alur aplikasi AIS) yang
 * mendemonstrasikan pengiriman satu email percobaan lewat SMTP Gmail (host
 * {@code smtp.gmail.com}, port 587, STARTTLS) memakai JavaMail API murni, tanpa melalui mesin
 * produksi pengiriman email AIS ({@code ais.delivery.email.sender.MailSender}).
 *
 * <p>
 * Gaya penulisan (komentar bernomor langkah 1–6 dalam Bahasa Indonesia, nama variabel
 * {@code emailPenerima} yang masih berisi placeholder {@code "email_tujuan@contoh.com"} dengan
 * komentar "GANTI DENGAN EMAIL TUJUAN ASLI") menunjukkan kelas ini adalah skrip uji manual yang
 * ditulis untuk dijalankan langsung sebagai program {@code main} saat menguji konektivitas SMTP
 * Gmail, sebagaimana pola serupa yang sudah ditemukan pada kelas contoh JavaMail lain di paket
 * {@code ais.delivery.email.sender} (mis. {@code SimpleMail}, {@code CrunchifyJavaMailExample}) —
 * kelas-kelas tersebut juga terverifikasi tidak pernah dipanggil dari bagian lain aplikasi.
 * </p>
 *
 * <h2>Riwayat keamanan — kredensial SMTP tertanam (DIPERBAIKI 2026-09-01)</h2>
 * <p>
 * {@link #main(String[])} sebelumnya berisi kredensial Gmail nyata dalam bentuk teks polos
 * tertanam langsung di kode: alamat pengirim {@code noreply@uinbukittinggi.ac.id} dan sebuah
 * <i>App Password</i> Gmail 16 digit. Nilai literal itu sudah DIHAPUS — {@code username}/
 * {@code password} kini WAJIB disuplai lewat properti sistem ({@code -Dkirimemailgmail.username=...
 * -Dkirimemailgmail.password=...}) saat kelas ini dijalankan manual, dan {@code main} berhenti
 * dengan pesan kesalahan bila salah satunya kosong. <b>Tindak lanjut yang TETAP diperlukan di
 * luar perubahan kode ini:</b> App Password yang sebelumnya tertanam sudah lama berada di
 * riwayat SVN dan WAJIB dianggap bocor — SEGERA dicabut/diputar dari pengaturan keamanan akun
 * Google {@code noreply@uinbukittinggi.ac.id} oleh pemilik akun, terlepas dari perbaikan kode
 * ini (App Password lama tetap valid sampai dicabut manual di sisi Google, tidak otomatis
 * kedaluwarsa hanya karena dihapus dari kode).
 * </p>
 */
public class KirimEmailGmail {

    /**
     * Titik masuk baris perintah yang membangun sesi SMTP terautentikasi ke Gmail, menyusun satu
     * pesan teks percobaan bersubjek {@code "Testing Kirim Email via Java"}, lalu mengirimkannya
     * lewat {@link Transport#send(Message)} ke alamat placeholder {@code emailPenerima}. Kegagalan
     * pengiriman ({@link MessagingException}) dicetak ke konsol/stack trace dan direkam lewat
     * {@code ais.common.ErrorAuditUtil#record}, tanpa dilempar ulang.
     *
     * <p>
     * Lihat catatan keamanan pada Javadoc kelas terkait kredensial SMTP ({@code username}/
     * {@code password}) yang tertanam langsung di method ini.
     * </p>
     *
     * @param args argumen baris perintah (tidak dipakai)
     */
    public static void main(String[] args) {

        // 1. Konfigurasi Akun Pengirim -- WAJIB disuplai lewat properti sistem, tidak ada
        // default tertanam (lihat riwayat keamanan pada javadoc kelas).
        final String username = System.getProperty("kirimemailgmail.username", "");
        final String password = System.getProperty("kirimemailgmail.password", "");
        if (username.trim().isEmpty() || password.trim().isEmpty()) {
            System.out.println(
                    "Kredensial belum diisi. Jalankan dengan -Dkirimemailgmail.username=... -Dkirimemailgmail.password=...");
            return;
        }

        // 2. Konfigurasi Email Penerima
        String emailPenerima = "email_tujuan@contoh.com"; // GANTI DENGAN EMAIL TUJUAN ASLI

        // 3. Setup Properties Server SMTP Gmail
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true"); // TLS penting untuk koneksi aman
        
        // (Opsional) Untuk debugging agar terlihat log proses pengiriman di console
        // prop.put("mail.debug", "true"); 

        // 4. Membuat Sesi (Session) dengan Autentikasi
        Session session = Session.getInstance(prop,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            // 5. Membuat Pesan Email
            Message message = new MimeMessage(session);
            
            // Set Pengirim
            message.setFrom(new InternetAddress(username));
            
            // Set Penerima
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(emailPenerima)
            );
            
            // Set Judul (Subject)
            message.setSubject("Testing Kirim Email via Java");
            
            // Set Isi Pesan
            message.setText("Halo,\n\nIni adalah email percobaan yang dikirim menggunakan program Java.\n\nSalam,\nTim IT");

            // 6. Kirim Email
            System.out.println("Sedang mengirim email...");
            Transport.send(message);

            System.out.println("Email berhasil dikirim ke: " + emailPenerima);

        } catch (MessagingException e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/KirimEmailGmail.java:63");
            System.out.println("Gagal mengirim email. Cek koneksi internet atau kredensial.");
        }
    }
}