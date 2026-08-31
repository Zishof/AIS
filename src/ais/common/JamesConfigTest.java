package ais.common;

/*
=====================================================================

  JamesConfigTest.java
  
  Created by Claude Duguay
  Copyright (c) 2003
  
=====================================================================
*/

/**
 * Program uji coba berdiri sendiri (header berkas menyebut penulis asli "Claude Duguay", hak cipta
 * 2003 — bukan kode buatan tim AIS) untuk memverifikasi konfigurasi mail server yang mendukung
 * POP3+SMTP (nama kelas mengindikasikan pengujian terhadap Apache James, sebuah mail server Java
 * yang populer pada masanya). Kelas ini TIDAK dipanggil dari bagian lain aplikasi AIS — hanya
 * berjalan bila dieksekusi langsung sebagai program Java (mis. {@code java ais.common.JamesConfigTest}).
 *
 * <p>
 * Skenario yang diuji: (1) buat tiga klien {@link MailClient} ("red", "green", "blue") yang
 * seluruhnya memakai kredensial/host yang SAMA ({@code "zishof"}/{@code "mail.eakademik.id"}) —
 * dinamai per warna murni sebagai label peran dalam skenario uji, bukan tiga akun berbeda; (2)
 * kosongkan inbox milik klien "blue" lewat {@link MailClient#checkInbox(int)} bermode
 * {@link MailClient#CLEAR_MESSAGES}; (3) kirim satu pesan dari "red" dan satu dari "green", keduanya
 * ditujukan ke alamat yang sama ({@code zishof@mail.eakademik.id}); (4) tampilkan sekaligus kosongkan
 * kembali inbox "blue" lewat mode {@link MailClient#SHOW_AND_CLEAR}, dengan ekspektasi kedua pesan
 * yang baru dikirim muncul di sana. Jeda {@link Thread#sleep(long)} 500 md disisipkan di antara
 * langkah kirim dan baca untuk memberi waktu server memproses pesan sebelum inbox diperiksa.
 * </p>
 *
 * <p>
 * Karena host ({@code mail.eakademik.id}) dan akun ({@code zishof}) ditulis langsung sebagai literal
 * di kode (bukan dibaca dari konfigurasi), kelas ini hanya dapat dijalankan bermakna terhadap
 * lingkungan mail server spesifik tersebut; dianggap kode uji/diagnostik peninggalan, bukan bagian
 * dari alur produksi.
 * </p>
 */
public class JamesConfigTest {
	/**
	 * Menjalankan skenario uji konfigurasi mail server: bersihkan inbox "blue", kirim dua pesan
	 * (dari "red" dan "green") ke alamat yang sama, lalu tampilkan+bersihkan kembali inbox "blue"
	 * untuk memverifikasi kedua pesan diterima. Lihat javadoc kelas untuk uraian skenario lengkap.
	 *
	 * @param args argumen baris perintah; tidak dipakai
	 * @throws Exception diteruskan dari kegagalan koneksi/pengiriman/pembacaan mail ({@link
	 *                    javax.mail.MessagingException}) atau dari {@link Thread#sleep(long)}
	 */
	public static void main(String[] args) throws Exception {
		// CREATE CLIENT INSTANCES
		MailClient redClient = new MailClient("zishof", "mail.eakademik.id");
		MailClient greenClient = new MailClient("zishof", "mail.eakademik.id");
		MailClient blueClient = new MailClient("zishof", "mail.eakademik.id");

		// CLEAR EVERYBODY'S INBOX
//		redClient.checkInbox(MailClient.CLEAR_MESSAGES);
//		greenClient.checkInbox(MailClient.CLEAR_MESSAGES);
		blueClient.checkInbox(MailClient.CLEAR_MESSAGES);
		Thread.sleep(500); // Let the server catch up

		// SEND A COUPLE OF MESSAGES TO BLUE (FROM RED AND GREEN)
		redClient.sendMessage("zishof@mail.eakademik.id", "Testing blue from red", "This is a test message");
		greenClient.sendMessage("zishof@mail.eakademik.id", "Testing blue from green", "This is a test message");
		Thread.sleep(500); // Let the server catch up

		// LIST MESSAGES FOR BLUE (EXPECT MESSAGES FROM RED AND GREEN)
		blueClient.checkInbox(MailClient.SHOW_AND_CLEAR);
	}
}
