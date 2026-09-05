package ais.database.model.rab;

/**
 * Test harness tanpa JUnit dan tanpa basis data untuk kunci advisory lock ref
 * RAB ({@link PenggunaanAnggaran#kunciRef(String)}).
 *
 * <p>Advisory lock inilah yang mencegah <b>dua proses menulis ref RAB yang sama
 * secara bersamaan</b> (lihat {@code PenggunaanAnggaran.lockRef}). Yang dijaga di
 * sini bukan "kuncinya bekerja" — itu urusan harness ber-database — melainkan satu
 * keputusan yang mudah rusak diam-diam saat seseorang menyunting kelak:</p>
 *
 * <ol>
 *   <li><b>Prefiks namespace {@code rab-ref:} ada, deterministik, dan unik.</b>
 *       Seluruh pemakai {@code hashtext(...)} di basis kode berbagi SATU ruang
 *       kunci advisory global PostgreSQL. Tanpa prefiks, isi {@code ref} yang
 *       bebas ditentukan modul lain bisa memetakan ke hash yang sama dengan kunci
 *       fitur lain ({@code online-bmt:}, {@code bast-sinkron:}, {@code init:},
 *       {@code PMB_NO_UJIAN_SAVE_}) sehingga saling memblokir walau tak
 *       berhubungan — bahkan berpotensi deadlock.</li>
 * </ol>
 *
 * <p>Jalankan: {@code java ais.database.model.rab.PenggunaanAnggaranLockSelfTest}.
 * Keluar dengan kode 1 dan menyebut invarian yang dilanggar bila ada yang rusak.</p>
 *
 * <h3>Ruang lingkup — apa yang TIDAK diverifikasi di sini</h3>
 *
 * <p>Harness ini murni fungsi-string dan sengaja tidak menyentuh basis data.
 * Karena itu ia <b>tidak</b> menjamin hal-hal berikut, yang kerap dikira ikut
 * tercakup:</p>
 * <ul>
 *   <li>bahwa {@code pg_advisory_xact_lock()} benar-benar dieksekusi dan benar-benar
 *       menserialkan dua transaksi bersamaan atas {@code ref} yang sama;</li>
 *   <li>bahwa tidak terjadi tabrakan <i>hash</i>: {@code hashtext()} PostgreSQL
 *       memetakan string ke ruang 32-bit, sehingga dua string berbeda tetap bisa
 *       menghasilkan kunci yang sama. Yang dijamin harness ini hanyalah bahwa
 *       <i>string</i> kuncinya berbeda dan ber-namespace — bukan hash-nya;</li>
 *   <li>bahwa data penggunaan anggaran itu sendiri benar (nilai, workspace, sumber),
 *       apalagi bahwa realisasi tidak melampaui pagu. Tidak ada penjaga pagu di
 *       {@link PenggunaanAnggaran} — pelampauan pagu hanya dilaporkan setelah
 *       kejadian oleh {@code RealisasiBulananAction};</li>
 *   <li>ketahanan terhadap duplikat pada tingkat baris — itu ditangani oleh jalur
 *       lain di {@link PenggunaanAnggaran} (constraint unik
 *       {@code ref_penggunaan_anggaran}, {@code removeDuplicateRowsByRef}, dan retry
 *       sekali atas duplikat/{@code StaleStateException}).</li>
 * </ul>
 *
 * <p>Harness sekerabat untuk entitas yang sama, dengan lingkup berbeda:
 * {@code ais.action.master.generic.v2.test.PenggunaanAnggaranWorkflowSelfTest}
 * (memastikan generic-CRUD v2 dipaksa native-only dan kunci alaminya {@code ref})
 * dan {@code ais.action.master.generic.v2.test.PenggunaanAnggaranDatabaseSelfTest}
 * (perlu koneksi basis data; menguji paginasi, facet, dan riwayat).</p>
 *
 * @see PenggunaanAnggaran#kunciRef(String)
 */
public final class PenggunaanAnggaranLockSelfTest {

	/**
	 * Konstruktor privat untuk mencegah instansiasi — kelas ini hanya wadah
	 * {@code main} dan konstanta.
	 */
	private PenggunaanAnggaranLockSelfTest() { }

	/**
	 * Prefiks namespace fitur lain yang berbagi ruang kunci advisory yang sama.
	 *
	 * <p>Daftar ini adalah <i>inventaris manual</i> hasil penelusuran pemakai
	 * {@code hashtext(...)} di basis kode: {@code online-bmt:} (OnlineBmt),
	 * {@code bast-sinkron:} (PengadaanPosApiHelper), {@code init:} (InitIndex), dan
	 * {@code PMB_NO_UJIAN_SAVE_} (CommonPMB). Setiap kali fitur baru memakai
	 * advisory lock dengan prefiks sendiri, tambahkan prefiksnya ke sini agar
	 * pemeriksaan tabrakan pada {@link #main(String[])} tetap menyeluruh. Bila daftar
	 * ini tertinggal, harness tetap "LULUS" namun cakupannya menyempit diam-diam.</p>
	 */
	private static final String[] NAMESPACE_LAIN = {
			"online-bmt:",           // OnlineBmt
			"bast-sinkron:",         // PengadaanPosApiHelper
			"init:",                 // InitIndex
			"PMB_NO_UJIAN_SAVE_" };  // CommonPMB

	/**
	 * Pencacah invarian yang gagal selama satu kali eksekusi {@link #main(String[])}.
	 *
	 * <p>Bersifat {@code static} dan tidak pernah di-reset: harness dirancang untuk
	 * satu proses satu eksekusi, lalu {@code System.exit}.</p>
	 */
	private static int gagal = 0;

	/**
	 * Menegaskan satu invarian dan mencetak hasilnya.
	 *
	 * <p>Mencetak {@code "LULUS  "} atau {@code "GAGAL  "} diikuti {@code pesan}.
	 * Berbeda dengan {@code assert} JUnit, kegagalan <b>tidak</b> menghentikan
	 * eksekusi: pencacah {@link #gagal} dinaikkan lalu pemeriksaan berikutnya tetap
	 * dijalankan, sehingga satu kali jalan memperlihatkan <i>seluruh</i> invarian
	 * yang rusak sekaligus, bukan hanya yang pertama.</p>
	 *
	 * @param nilai hasil evaluasi invarian; {@code true} berarti lulus
	 * @param pesan uraian singkat invarian yang sedang diuji, dicetak apa adanya
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
	 * Menjalankan seluruh pemeriksaan invarian kunci advisory ref RAB dan menutup
	 * proses dengan kode keluar 1 bila ada yang dilanggar.
	 *
	 * <p>Urutan pemeriksaannya, dan alasan tiap kelompok ada:</p>
	 * <ol>
	 *   <li><b>Determinisme.</b> {@code kunciRef("RAB-2026-000123")} dipanggil dua
	 *       kali dan hasilnya harus identik. Bila suatu saat seseorang menyisipkan
	 *       komponen acak, cap waktu, atau {@code hashCode()} objek ke dalam
	 *       pembentukan kunci, dua proses bersamaan atas ref yang sama akan
	 *       memperoleh kunci berbeda — advisory lock berhenti menserialkan apa pun
	 *       dan duplikat ref bisa lolos kembali.</li>
	 *   <li><b>Membawa identitas ref dan memisahkan ref berbeda.</b> Kunci harus
	 *       masih memuat teks {@code ref} aslinya, dan dua ref berbeda
	 *       ({@code ...123} vs {@code ...124}) harus menghasilkan kunci berbeda.
	 *       Pemeriksaan kedua ini menangkap regresi ke arah sebaliknya: kunci yang
	 *       terlalu kasar (mis. konstanta global {@code "rab-ref"} saja) akan
	 *       menserialkan SELURUH penulisan RAB di satu antrean dan mengubah bug
	 *       korupsi data menjadi bug kemacetan.</li>
	 *   <li><b>Prefiks namespace ada dan unik.</b> Kunci harus diawali
	 *       {@code "rab-ref:"}, tidak boleh diawali prefiks fitur lain, dan tidak
	 *       ada prefiks fitur lain yang kebetulan diawali {@code "rab-ref:"}.</li>
	 *   <li><b>Anti-tabrakan atas ref bermusuhan.</b> Inti perbaikan dok. 107: untuk
	 *       tiap prefiks fitur lain dibentuk ref berbahaya seperti
	 *       {@code "online-bmt:999"} — yaitu ref yang isinya persis menyerupai kunci
	 *       utuh milik fitur lain. Sebelum perbaikan, {@code ref} dikunci mentah
	 *       sehingga string semacam itu memetakan ke kunci advisory yang SAMA dengan
	 *       fitur lain; setelah diberi prefiks, hasil {@code kunciRef(...)} dijamin
	 *       tidak lagi identik dengan kunci fitur lain tersebut. Perlu dicatat bahwa
	 *       yang dijamin adalah ketidaksamaan <i>string</i>; tabrakan pada tingkat
	 *       {@code hashtext()} 32-bit tetap mungkin secara teoretis dan berada di
	 *       luar jangkauan harness ini.</li>
	 *   <li><b>Stabilitas terhadap {@code null}.</b> {@code kunciRef(null)} harus
	 *       mengembalikan {@code "rab-ref:null"} tanpa melempar
	 *       {@code NullPointerException}. Dalam alur produksi kasus ini tidak
	 *       tercapai karena {@code PenggunaanAnggaran.lockRef} sudah menyaring ref
	 *       kosong lebih dulu lewat {@code hasText(...)}; invarian ini menjaga agar
	 *       {@code kunciRef} tetap aman dipakai ulang di konteks lain, dan agar
	 *       penambahan validasi yang melempar di dalamnya tidak masuk tanpa
	 *       disadari.</li>
	 * </ol>
	 *
	 * <p>Setelah seluruh pemeriksaan, dicetak ringkasan
	 * {@code "SEMUA INVARIAN KUNCI REF RAB TERJAGA"} atau
	 * {@code "ADA n INVARIAN YANG DILANGGAR"}. Bila ada kegagalan, proses keluar
	 * dengan kode 1 sehingga harness ini dapat dirangkai ke dalam skrip verifikasi
	 * berantai; bila semua lulus, {@code main} kembali normal (kode keluar 0).</p>
	 *
	 * @param args tidak dipakai
	 */
	public static void main(String[] args) {
		String kA = PenggunaanAnggaran.kunciRef("RAB-2026-000123");

		// 1. Deterministik: ref sama -> kunci sama.
		cek(kA.equals(PenggunaanAnggaran.kunciRef("RAB-2026-000123")),
				"kunci deterministik untuk ref yang sama");

		// 2. Membawa identitas ref dan membedakan ref yang berbeda.
		cek(kA.indexOf("RAB-2026-000123") >= 0, "kunci memuat ref");
		cek(!kA.equals(PenggunaanAnggaran.kunciRef("RAB-2026-000124")),
				"ref berbeda -> kunci berbeda (tidak saling menserialkan)");

		// 3. Prefiks namespace RAB ada dan unik terhadap seluruh pemakai lain.
		cek(kA.startsWith("rab-ref:"), "kunci memakai prefiks namespace rab-ref:");
		for (String lain : NAMESPACE_LAIN) {
			cek(!kA.startsWith(lain),
					"kunci RAB tidak memakai namespace fitur lain (" + lain + ")");
			cek(!lain.startsWith("rab-ref:"),
					"namespace fitur lain (" + lain + ") tidak menabrak rab-ref:");
		}

		// 4. Anti-tabrakan: ref mentah yang KEBETULAN sama persis dengan kunci utuh
		//    fitur lain tetap tak menabrak setelah diberi prefiks. Inilah inti
		//    perbaikan dok. 107 — sebelumnya ref dikunci mentah (tanpa prefiks).
		for (String lain : NAMESPACE_LAIN) {
			String refBerbahaya = lain + "999";        // mis. "online-bmt:999"
			cek(!PenggunaanAnggaran.kunciRef(refBerbahaya).equals(refBerbahaya),
					"ref '" + refBerbahaya + "' tidak lagi identik dengan kunci fitur lain");
		}

		// 5. Perilaku null stabil tanpa NPE (lockRef sudah menyaring ref kosong,
		//    tetapi kunciRef tetap tidak boleh melempar).
		cek("rab-ref:null".equals(PenggunaanAnggaran.kunciRef(null)),
				"ref null menghasilkan kunci stabil tanpa melempar NPE");

		System.out.println(gagal == 0
				? "SEMUA INVARIAN KUNCI REF RAB TERJAGA"
				: ("ADA " + gagal + " INVARIAN YANG DILANGGAR"));
		if (gagal > 0) {
			System.exit(1);
		}
	}
}
