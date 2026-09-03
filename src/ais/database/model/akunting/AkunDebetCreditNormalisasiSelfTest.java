package ais.database.model.akunting;

/**
 * Test harness tanpa JUnit dan tanpa basis data untuk normalisasi sandi
 * {@code debit_credit} di {@link Akun#setDebetCredit(Integer)}.
 *
 * <p>Melindungi perbaikan integritas data 3 Sep 2026: sandi legacy {@code 2} (ditulis jalur
 * impor Accurate dan API {@code akunSimpan} untuk akun kredit) pernah lolos apa adanya ke
 * kolom {@code debit_credit}, membuat {@code LaporanKeuanganCoaHelper} mengalikan saldo akun
 * itu dengan {@code +2} alih-alih {@code -1} pada laporan keuangan cetak. Setter sekarang
 * menormalkan {@code 2} menjadi {@link Akun#CREDIT} sebelum menyimpannya ke field -- baris ini
 * memastikan regresi itu tidak diam-diam kembali.</p>
 *
 * <p>Jalankan: {@code java ais.database.model.akunting.AkunDebetCreditNormalisasiSelfTest}.
 * Keluar dengan kode 1 dan menyebut invarian yang dilanggar bila ada yang rusak.</p>
 */
public final class AkunDebetCreditNormalisasiSelfTest {

	/** Kelas utilitas murni: hanya berisi {@link #main(String[])}, tidak untuk diinstansiasi. */
	private AkunDebetCreditNormalisasiSelfTest() { }

	/**
	 * Pencacah invarian yang dilanggar selama satu kali eksekusi {@link #main(String[])}.
	 *
	 * <p>Dinaikkan oleh {@link #cek(boolean, String)} setiap kali sebuah invarian gagal, lalu
	 * dibaca di akhir {@code main} untuk memutuskan ringkasan dan kode keluar proses. Karena
	 * {@code static} dan tidak pernah direset, kelas ini memang hanya dirancang untuk sekali
	 * jalan lewat {@code main}, bukan untuk dipanggil berulang dari kode lain.</p>
	 */
	private static int gagal = 0;

	/**
	 * Memeriksa satu invarian dan melaporkan hasilnya ke {@code System.out}.
	 *
	 * <p>Pengganti minimal {@code assert}/JUnit: bila {@code nilai} benar, mencetak baris
	 * {@code "LULUS  <pesan>"}; bila salah, menaikkan pencacah {@link #gagal} dan mencetak
	 * {@code "GAGAL  <pesan>"}. Sengaja <b>tidak</b> melempar exception maupun menghentikan
	 * proses, supaya seluruh invarian tetap dievaluasi dalam satu kali jalan dan operator
	 * melihat daftar lengkap yang rusak, bukan hanya kegagalan pertama.</p>
	 *
	 * @param nilai hasil evaluasi invarian; {@code true} berarti invarian terjaga
	 * @param pesan uraian singkat invarian, dicetak apa adanya di belakang label LULUS/GAGAL
	 */
	private static void cek(boolean nilai, String pesan) {
		if (nilai) {
			System.out.println("LULUS  " + pesan);
		} else {
			gagal++;
			System.out.println("GAGAL  " + pesan);
		}
	}

	/**
	 * Menjalankan seluruh invarian normalisasi {@code debit_credit} lalu menyimpulkan hasilnya.
	 *
	 * <p>Enam invarian diperiksa berurutan, semuanya pada objek {@link Akun} biasa di memori
	 * (tanpa Hibernate, tanpa koneksi basis data, tanpa berkas konfigurasi):</p>
	 * <ol>
	 *   <li>konstanta {@link Akun#DEBET} tetap {@code 1} dan {@link Akun#CREDIT} tetap
	 *       {@code -1} &mdash; sandi kanonik tidak boleh berubah diam-diam, karena
	 *       {@code LaporanKeuanganCoaHelper} memakainya langsung sebagai pengali saldo;</li>
	 *   <li><b>inti perbaikan</b>: {@code setDebetCredit(2)} harus ternormalkan menjadi
	 *       {@link Akun#CREDIT}, bukan lolos apa adanya;</li>
	 *   <li>{@code setDebetCredit(DEBET)} dan {@code setDebetCredit(CREDIT)} tidak ikut
	 *       terpengaruh normalisasi &mdash; menjaga agar perbaikan tidak kebablasan;</li>
	 *   <li>{@code setDebetCredit(null)} tetap dibaca sebagai {@link Akun#CREDIT} oleh
	 *       getter &mdash; perilaku lama yang memang sengaja dipertahankan.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping.</b> Method ini menulis satu baris {@code LULUS}/{@code GAGAL} per
	 * invarian ke {@code System.out}, ditutup satu baris ringkasan, dan memanggil
	 * {@link System#exit(int)} dengan kode {@code 1} bila ada invarian yang dilanggar
	 * (kode {@code 0} bila semuanya lulus). Karena mematikan JVM, jangan panggil {@code main}
	 * ini dari kode aplikasi &mdash; hanya untuk dijalankan sebagai proses tersendiri.</p>
	 *
	 * @param args diabaikan; tidak ada opsi baris perintah
	 */
	public static void main(String[] args) {
		cek(Integer.valueOf(1).equals(Akun.DEBET), "konstanta Akun.DEBET tetap 1");
		cek(Integer.valueOf(-1).equals(Akun.CREDIT), "konstanta Akun.CREDIT tetap -1");

		Akun akun = new Akun();

		// Inti perbaikan: sandi legacy 2 (Accurate/akunSimpan) HARUS ternormalkan jadi -1,
		// bukan lolos apa adanya -- ini yang dulu menggandakan & membalik tanda saldo di
		// LaporanKeuanganCoaHelper (nilai = saldo * akun.getDebetCredit()).
		akun.setDebetCredit(Integer.valueOf(2));
		cek(Akun.CREDIT.equals(akun.getDebetCredit()),
				"setDebetCredit(2) ternormalkan menjadi Akun.CREDIT (-1), bukan lolos sebagai 2");

		akun.setDebetCredit(Akun.DEBET);
		cek(Akun.DEBET.equals(akun.getDebetCredit()), "setDebetCredit(DEBET) tetap 1, tidak ikut ternormalkan");

		akun.setDebetCredit(Akun.CREDIT);
		cek(Akun.CREDIT.equals(akun.getDebetCredit()), "setDebetCredit(-1) tetap -1 (sandi kanonik)");

		akun.setDebetCredit(null);
		cek(Akun.CREDIT.equals(akun.getDebetCredit()),
				"setDebetCredit(null) tetap dipaksa getter menjadi CREDIT (-1) -- perilaku lama tidak berubah");

		System.out.println(gagal == 0
				? "SEMUA INVARIAN NORMALISASI DEBIT_CREDIT TERJAGA"
				: ("ADA " + gagal + " INVARIAN YANG DILANGGAR"));
		if (gagal > 0) {
			System.exit(1);
		}
	}
}
