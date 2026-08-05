package ais.common;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class KirimEmailGmail {

    public static void main(String[] args) {

        // 1. Konfigurasi Akun Pengirim
        final String username = "noreply@uinbukittinggi.ac.id";
        // Password ini adalah App Password 16 digit yang Anda berikan
        final String password = "rrkl xmjw wktw kyml"; 

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