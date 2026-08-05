package ais.common;

/**
 * Utilitas bersama "Informasi Teknis" kegagalan payment gateway — satu pola untuk
 * SEMUA saluran pembayaran (BNI, BRI, BSI, Faspay, Doku, iPaymu, Jatelindo, CIMB,
 * Finpay, beserta varian Keranjang).
 * <p>
 * <b>Masalah yang diselesaikan.</b> Ketika request POST ke server bank/gateway
 * gagal (ditolak dengan kode status, gateway tak terjangkau, timeout, respons tak
 * bisa diparse, atau hasil gagal disimpan), pengguna selama ini hanya melihat alert
 * generik "Transaksi Gagal Dilakukan" tanpa tahu penyebabnya — sehingga tiket
 * keluhan selalu berbunyi "kenapa gagal?" dan admin harus membongkar log server.
 * Utilitas ini menjadi kanal penyampai detail: setiap titik gagal pada method
 * pengirim request MENCATAT detailnya via {@link #catat(String)}, lalu alert
 * kegagalan menampilkan {@link #pesanGagal()} yang berisi
 * {@code "Transaksi Gagal Dilakukan. Informasi Teknis: <detail>"}.
 * <p>
 * <b>Desain.</b> Detail disimpan pada {@link ThreadLocal} sehingga aman dipakai
 * banyak pengguna bersamaan (satu request ZK = satu thread). Kontraknya
 * baca-sekali: {@link #ambil()} mengembalikan detail lalu MENGHAPUSNYA, supaya
 * kegagalan lama tidak bocor ke transaksi berikutnya; {@link #bersihkan()} wajib
 * dipanggil di AWAL method pengirim request untuk alasan yang sama. Pencatatan ke
 * Error Log tetap tanggung jawab pemanggil ({@code ErrorAuditUtil.record}) — kelas
 * ini murni kanal pesan ke layar.
 * <p>
 * <b>Cara pakai di gateway baru</b> (tiga langkah): (1) panggil
 * {@code InfoTeknisPembayaran.bersihkan()} di awal method kirim; (2) di setiap
 * titik gagal — status ditolak, ConnectException, SocketTimeoutException,
 * ConnectTimeoutException (awas: subclass InterruptedIOException, bukan
 * ConnectException!), gagal parse, gagal simpan — panggil
 * {@code catat("...penyebab + URL + potongan respons...")} memakai
 * {@link #potong(String, int)} agar respons mentah tidak membanjiri alert;
 * (3) ganti alert generik menjadi {@code MyMessageboxConfig.show(
 * InfoTeknisPembayaran.pesanGagal(), ...)}.
 */
public final class InfoTeknisPembayaran {

	private InfoTeknisPembayaran() {
	}

	/** Detail kegagalan terakhir pada thread ini (baca-sekali via {@link #ambil()}). */
	private static final ThreadLocal<String> TERAKHIR = new ThreadLocal<String>();

	/** Bersihkan detail lama — WAJIB di awal setiap method pengirim request gateway. */
	public static void bersihkan() {
		TERAKHIR.remove();
	}

	/** Catat detail teknis kegagalan untuk thread ini (menimpa detail sebelumnya). */
	public static void catat(String info) {
		TERAKHIR.set(info);
	}

	/** Ambil detail kegagalan terakhir SEKALIGUS menghapusnya; {@code null} bila tak ada. */
	public static String ambil() {
		String s = TERAKHIR.get();
		TERAKHIR.remove();
		return s;
	}

	/**
	 * Pesan alert kegagalan transaksi + "Informasi Teknis" bila tersedia — dipakai
	 * seluruh alert "Transaksi Gagal Dilakukan" payment gateway.
	 */
	public static String pesanGagal() {
		String info = ambil();
		return "Transaksi Gagal Dilakukan."
				+ (info == null || info.trim().isEmpty() ? "" : " Informasi Teknis: " + info);
	}

	/** Potong teks panjang (respons mentah server) agar alert tetap terbaca. */
	public static String potong(String s, int maks) {
		if (s == null) return "";
		String bersih = s.trim();
		return bersih.length() <= maks ? bersih : bersih.substring(0, maks) + "…";
	}

	/**
	 * Bangun rincian teknis LANGSUNG dari sebuah {@link Throwable} (nama kelas + pesan,
	 * ditelusuri sampai akar rantai {@code getCause()}) — dipakai alert kegagalan yang
	 * MENANGKAP Exception langsung (mis. tombol "Cek Ulang" status pembayaran di
	 * {@code VirtualAccountBankAction}) dan TIDAK melalui pola catat()/ambil() di atas
	 * (yang butuh titik pencatatan manual di dalam method pengirim request gateway).
	 *
	 * <p>Beda dengan {@link #pesanGagal()}: method ini tidak baca/hapus {@link #TERAKHIR}
	 * dan tidak menyertakan prefix "Transaksi Gagal Dilakukan" — cocok dipakai sebagai
	 * argumen {@code {V1}} pada template alert milik pemanggil sendiri.</p>
	 *
	 * @param t exception yang ditangkap; boleh null (mengembalikan string kosong)
	 * @return "NamaKelasException: pesan — disebabkan oleh: NamaKelasCause: pesanCause — ..."
	 */
	public static String detailDariException(Throwable t) {
		if (t == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		Throwable cur = t;
		Throwable sebelumnya = null;
		int guard = 0;
		while (cur != null && cur != sebelumnya && guard++ < 6) {
			if (sb.length() > 0) {
				sb.append(" — disebabkan oleh: ");
			}
			sb.append(cur.getClass().getSimpleName());
			String pesan = cur.getMessage();
			if (pesan != null && !pesan.trim().isEmpty()) {
				sb.append(": ").append(potong(pesan, 300));
			}
			sebelumnya = cur;
			cur = cur.getCause();
		}
		return sb.toString();
	}
}
