package ais.common;

/*
=====================================================================

  MailClient.java
  
  Created by Claude Duguay
  Copyright (c) 2003
  
=====================================================================
*/

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * Klien JavaMail generik peninggalan (header berkas menyebut penulis asli "Claude Duguay",
 * hak cipta 2003 — bukan kode buatan tim AIS, kemungkinan besar diadaptasi dari contoh/tutorial
 * JavaMail API populer pada masanya) yang membungkus operasi POP3 (baca/hapus inbox) dan SMTP
 * (kirim pesan) sederhana dalam satu objek. Kelas ini HANYA dipakai oleh {@link JamesConfigTest},
 * yaitu program uji-coba berdiri sendiri untuk memverifikasi konfigurasi mail server (kemungkinan
 * Apache James, mengingat nama kelas {@code JamesConfigTest}) — bukan bagian dari mesin pengiriman
 * email produksi AIS (bandingkan dengan {@link ais.delivery.email.sender.MailSender} yang dipakai
 * seluruh aplikasi untuk kirim email/notifikasi sungguhan).
 *
 * <p>
 * <b>Desain otentikasi</b> — kelas ini meng-extend {@link Authenticator} dan mengimplementasikan
 * {@link #getPasswordAuthentication()} agar sesi JavaMail ({@link #session}) dapat melakukan
 * otentikasi POP3/SMTP otomatis saat dibutuhkan. Perhatikan bahwa konstruktor membentuk kredensial
 * lewat {@code new PasswordAuthentication(user, user)} — artinya nilai password yang dipakai SAMA
 * PERSIS dengan nilai username yang diberikan pemanggil. Ini bukan kredensial tertanam dalam arti
 * nilai rahasia yang di-hardcode di kode sumber (nilainya berasal dari parameter pemanggil, bukan
 * literal), namun merupakan pola otentikasi yang secara inheren lemah (password == username) —
 * wajar untuk skrip uji coba lokal peninggalan tahun 2003, tetapi TIDAK layak dipakai sebagai
 * contoh pola otentikasi di kode produksi mana pun.
 * </p>
 *
 * <p>
 * Properti sesi JavaMail yang dipasang: {@code mail.store.protocol=pop3} (untuk membaca inbox lewat
 * {@link #checkInbox(int)}) dan {@code mail.transport.protocol=smtp} (untuk mengirim pesan lewat
 * {@link #sendMessage(String, String, String)}); alamat host mail server dan user diberikan lewat
 * konstruktor, bukan lewat konfigurasi terpusat AIS ({@code ais.common.Common#getKonfigurasi}) —
 * konsisten dengan sifat kelas ini sebagai skrip berdiri sendiri, bukan komponen aplikasi.
 * </p>
 */
public class MailClient extends Authenticator {
	/** Mode {@link #checkInbox(int)}: hanya menampilkan (mencetak ke konsol) isi pesan di inbox tanpa menghapusnya. */
	public static final int SHOW_MESSAGES = 1;
	/** Mode {@link #checkInbox(int)}: menandai seluruh pesan di inbox untuk dihapus tanpa menampilkannya. */
	public static final int CLEAR_MESSAGES = 2;
	/** Mode {@link #checkInbox(int)}: kombinasi menampilkan lalu menghapus seluruh pesan di inbox (jumlah dari {@link #SHOW_MESSAGES} + {@link #CLEAR_MESSAGES}). */
	public static final int SHOW_AND_CLEAR = SHOW_MESSAGES + CLEAR_MESSAGES;

	/** Alamat email pengirim, dibentuk dari {@code user + '@' + host} yang diberikan ke konstruktor. */
	protected String from;
	/** Sesi JavaMail yang menyimpan properti koneksi POP3/SMTP dan dipakai untuk membuka {@link Store}/mengirim {@link Transport}. */
	protected Session session;
	/** Kredensial otentikasi POP3/SMTP; lihat catatan keamanan pada javadoc kelas mengenai pola password == username. */
	protected PasswordAuthentication authentication;

	/** Seperti {@link #MailClient(String, String, boolean)} dengan mode debug JavaMail dimatikan ({@code debug=false}). */
	public MailClient(String user, String host) {
		this(user, host, false);
	}

	/**
	 * Membangun klien mail baru: menyusun alamat pengirim ({@code user@host}), kredensial otentikasi
	 * (password disamakan dengan username — lihat catatan keamanan pada javadoc kelas), dan sesi
	 * JavaMail dengan protokol POP3 untuk penyimpanan (store) serta SMTP untuk pengiriman
	 * (transport).
	 *
	 * @param user  nama pengguna mail (juga dipakai sebagai password otentikasi)
	 * @param host  alamat host mail server (dipakai untuk POP3 maupun SMTP)
	 * @param debug bila {@code true}, mengaktifkan log debug protokol JavaMail (properti
	 *              {@code mail.debug})
	 */
	public MailClient(String user, String host, boolean debug) {
		from = user + '@' + host;
		authentication = new PasswordAuthentication(user, user);
		Properties props = new Properties();
		props.put("mail.user", user);
		props.put("mail.host", host);
		props.put("mail.debug", debug ? "true" : "false");
		props.put("mail.store.protocol", "pop3");
		props.put("mail.transport.protocol", "smtp");
		session = Session.getInstance(props, this);
	}

	/**
	 * Callback dari {@link Authenticator}, dipanggil otomatis oleh JavaMail saat sesi memerlukan
	 * kredensial POP3/SMTP.
	 *
	 * @return kredensial {@link #authentication} yang dibentuk di konstruktor
	 */
	public PasswordAuthentication getPasswordAuthentication() {
		return authentication;
	}

	/**
	 * Mengirim satu pesan teks polos lewat SMTP menggunakan {@link #session}. Alamat pengirim selalu
	 * {@link #from}, jadi pemanggil hanya perlu memberi tujuan, subjek, dan isi.
	 *
	 * @param to      alamat email tujuan
	 * @param subject judul pesan
	 * @param content isi pesan (teks polos, bukan HTML)
	 * @throws MessagingException bila pengiriman SMTP gagal (mis. otentikasi ditolak, host tidak
	 *                            terjangkau)
	 */
	public void sendMessage(String to, String subject, String content) throws MessagingException {
		System.out.println("SENDING message from " + from + " to " + to);
		
		MimeMessage msg = new MimeMessage(session);
		msg.addRecipients(Message.RecipientType.TO, to);
		msg.setSubject(subject);
		msg.setText(content);
		Transport.send(msg);
	}

	/**
	 * Membuka inbox POP3 milik klien ini dan, tergantung {@code mode}, menampilkan isi pesan ke
	 * konsol dan/atau menandainya untuk dihapus. {@code mode} adalah bitmask dari {@link
	 * #SHOW_MESSAGES} dan {@link #CLEAR_MESSAGES} (atau {@link #SHOW_AND_CLEAR} untuk keduanya); nilai
	 * {@code 0} membuat method langsung kembali tanpa membuka koneksi apa pun. Penghapusan bersifat
	 * final: pesan yang ditandai {@link Flags.Flag#DELETED} benar-benar dibuang saat
	 * {@code inbox.close(true)} dipanggil di akhir method.
	 *
	 * @param mode kombinasi bit {@link #SHOW_MESSAGES}/{@link #CLEAR_MESSAGES}/{@link
	 *             #SHOW_AND_CLEAR}, atau {@code 0} untuk tidak melakukan apa pun
	 * @throws MessagingException bila koneksi POP3 atau operasi folder gagal
	 * @throws IOException        bila gagal membaca isi salah satu pesan
	 */
	public void checkInbox(int mode) throws MessagingException, IOException {
		if (mode == 0)
			return;
		boolean show = (mode & SHOW_MESSAGES) > 0;
		boolean clear = (mode & CLEAR_MESSAGES) > 0;
		String action = (show ? "Show" : "") + (show && clear ? " and " : "") + (clear ? "Clear" : "");
		System.out.println(action + " INBOX for " + from);
		Store store = session.getStore();
		store.connect();
		Folder root = store.getDefaultFolder();
		Folder inbox = root.getFolder("inbox");
		inbox.open(Folder.READ_WRITE);
		Message[] msgs = inbox.getMessages();
		if (msgs.length == 0 && show) {
			System.out.println("No messages in inbox");
		}
		for (int i = 0; i < msgs.length; i++) {
			MimeMessage msg = (MimeMessage) msgs[i];
			if (show) {
				System.out.println("    From: " + msg.getFrom()[0]);
				System.out.println(" Subject: " + msg.getSubject());
				System.out.println(" Content: " + msg.getContent());
			}
			if (clear) {
				msg.setFlag(Flags.Flag.DELETED, true);
			}
		}
		inbox.close(true);
		store.close();
		
	}
}
