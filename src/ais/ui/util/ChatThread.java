package ais.ui.util;

import java.util.ArrayList;
import java.util.List;

import ais.action.master.chat.ChatUsers;
import ais.common.Common;

/**
 * Thread latar belakang (worker) untuk fitur chat AIS: secara berkala (setiap 2 detik) memeriksa
 * pesan masuk untuk sekumpulan {@link ChatUsers} yang didaftarkan padanya lewat {@link
 * #chatUsers}. Rancangannya adalah satu instance {@code ChatThread} dapat mengawasi banyak
 * pengguna chat sekaligus dalam satu loop polling, alih-alih membuat satu thread per pengguna.
 *
 * <p>
 * Catatan: konstruktor {@link #ChatThread()} TIDAK lagi memulai thread secara otomatis (baris
 * {@code new Thread(this).start()} dikomentari) — pemanggil bertanggung jawab membungkus
 * instance ini dalam {@link Thread} dan memanggil {@code start()} sendiri bila loop polling
 * memang diperlukan.
 * </p>
 */
public class ChatThread implements Runnable {

	/** Daftar pengguna chat yang pesannya diperiksa pada setiap iterasi loop {@link #run()}. */
	public List<ChatUsers> chatUsers = new ArrayList<ChatUsers>();

	/** Saklar keberlangsungan loop {@link #run()}; diset {@code false} oleh {@link #onExit()} untuk menghentikan thread. */
	private Boolean running = true;

	/**
	 * Membuat instance baru. Tidak memulai thread apa pun secara otomatis — lihat catatan
	 * kelas di atas.
	 */
	public ChatThread() {
//		new Thread(this).start();
	}

	/**
	 * Loop utama worker: selama {@link #running} bernilai {@code true}, tidur 2 detik lalu
	 * memanggil {@link ChatUsers#checkPesan()} untuk setiap entri di {@link #chatUsers}.
	 * Kegagalan pada satu iterasi (mis. exception saat memeriksa pesan) ditangkap dan dicatat
	 * lewat {@link Common#tampilErrorJikaAdmin(Exception)} tanpa menghentikan loop.
	 */
	@Override
	public void run() {

		// log.info("init chat..............................");

		while (running) {
			try {
				Thread.sleep(2000);

				for (ChatUsers chat : chatUsers) {
					chat.checkPesan();
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

	}

	/**
	 * Menghentikan loop {@link #run()} dengan menandai {@link #running} menjadi {@code false};
	 * thread akan berhenti pada akhir iterasi tidur (jeda 2 detik) yang sedang berjalan.
	 */
	public void onExit() {
		System.out.println("================================= On Close ==============================");
		running = false;
	}

}
