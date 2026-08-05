package ais.action.report.helper;

/**
 * <h2>AngketDosenMetodologiUtil &mdash; sumber tunggal narasi metodologi skor Angket Penilaian Dosen</h2>
 *
 * <p><b>Latar belakang masalah yang melatarbelakangi class ini.</b> Modul Angket Penilaian Dosen pada
 * aplikasi ini memiliki DUA laporan resmi yang sama-sama menampilkan skor evaluasi dosen, namun dengan
 * cakupan agregasi data yang BERBEDA secara sengaja (bukan bug):</p>
 * <ol>
 *   <li><b>Laporan "Rangking Penilaian Mahasiswa atas Kinerja Dosen"</b> (dibangun oleh
 *   {@link ais.action.report.format1.akademik.LaporanRekapAngketDosenPerJurusanWindow}, dirender oleh
 *   berkas {@code webapp/report/rangking_angket_dosen_per_prodi.jrxml}) &mdash; query laporan ini
 *   melakukan {@code GROUP BY jurusan, dosen}, sehingga skor seorang dosen dihitung TERPISAH untuk
 *   setiap Program Studi, HANYA dari jawaban mahasiswa Program Studi bersangkutan.</li>
 *   <li><b>Laporan "Hasil Angket Penilaian Mahasiswa atas Kinerja Dosen"</b> (dibangun oleh
 *   {@link ais.action.report.format1.akademik.LaporanAngketDosenPerDosenSajaWindow}, dirender oleh
 *   berkas {@code webapp/report/rekap_angket_dosen_per_dosen.jrxml}) &mdash; group Jasper pada laporan
 *   ini hanya {@code dosen} (tanpa jurusan), sehingga skor dihitung GABUNGAN (pooled) dari SELURUH
 *   kelas yang diampu dosen tersebut lintas Program Studi.</li>
 * </ol>
 *
 * <p><b>Akibat bagi pengguna laporan.</b> Untuk dosen yang HANYA mengampu satu Program Studi, kedua
 * laporan menghasilkan angka identik. Namun untuk dosen yang mengampu LEBIH DARI SATU Program Studi,
 * kedua laporan akan menampilkan angka yang berbeda &mdash; secara matematis, skor pada laporan
 * "Hasil Angket" (pooled) selalu berada DI ANTARA skor-skor per-Program-Studi pada laporan "Rangking"
 * (merupakan rata-rata tertimbang berdasarkan jumlah mahasiswa responden pada tiap Program Studi).
 * Tanpa penjelasan eksplisit, perbedaan ini dapat disalahartikan oleh pembaca laporan (dosen, kaprodi,
 * atau pimpinan) sebagai kesalahan hitung sistem, padahal keduanya sah dan valid untuk tujuan analisis
 * yang berbeda: laporan Rangking menjawab pertanyaan "bagaimana penilaian dosen ini menurut mahasiswa
 * Program Studi X saja", sedangkan laporan Hasil Angket menjawab "bagaimana penilaian dosen ini secara
 * keseluruhan lintas semua kelas yang diampu".</p>
 *
 * <p><b>Tujuan &amp; tanggung jawab class ini.</b> Sebelum class ini dibuat, narasi penjelasan tersebut
 * ditulis TERPISAH dan hampir duplikat di empat lokasi berbeda: catatan ringkas pada panel ZK kedua
 * Window, serta blok teks formal pada title band kedua berkas {@code .jrxml}. Duplikasi semacam ini
 * adalah sumber risiko pemeliharaan klasik &mdash; jika suatu saat rumus perhitungan berubah, redaksi
 * perlu diperhalus, atau ditemukan langkah validasi tambahan, seluruh EMPAT lokasi harus diingat dan
 * diubah secara konsisten; jika satu lokasi terlewat, penjelasan pada satu laporan bisa menjadi usang
 * atau bahkan bertentangan dengan laporan pasangannya. Class ini MENGHILANGKAN duplikasi tersebut
 * dengan menyediakan SATU method builder privat, {@link #bangunKeteranganFormal}, yang menyusun narasi
 * lengkap (definisi cakupan, rumus, alasan perbedaan, dan tiga langkah validasi) dari sepasang parameter
 * "perspektif" (nama &amp; cakupan laporan yang sedang ditampilkan, serta nama &amp; cakupan laporan
 * pembanding). Keempat method publik ({@link #keteranganPanelRangking()},
 * {@link #keteranganPanelFull()}, {@link #keteranganJrxmlRangking()}, {@link #keteranganJrxmlFull()})
 * hanyalah pemanggil tipis (thin wrapper) terhadap builder tersebut dengan argumen yang ditukar-balik
 * sesuai arah laporan, sehingga redaksi inti hanya perlu dijaga konsistensinya di SATU tempat.</p>
 *
 * <p><b>Pola integrasi dengan JasperReports (penting untuk pemeliharaan berkas {@code .jrxml}).</b>
 * Blok teks formal TIDAK LAGI ditulis sebagai literal {@code <text><![CDATA[...]]></text>} di dalam
 * berkas {@code .jrxml} (pola lama, mudah kadaluarsa). Sebagai gantinya, kedua berkas {@code .jrxml}
 * mendeklarasikan parameter bertipe {@code java.lang.String} bernama {@code keterangan_metodologi} dan
 * merendernya melalui elemen {@code <textField isStretchWithOverflow="true">} yang nilainya diisi saat
 * runtime oleh method {@code generateParameter()} pada Window Java masing-masing, memanggil
 * {@link #keteranganJrxmlRangking()} atau {@link #keteranganJrxmlFull()}. Dengan pola ini, mengubah
 * redaksi penjelasan CUKUP dengan mengedit method di class ini &mdash; TIDAK perlu menyentuh berkas
 * {@code .jrxml} maupun mengompilasi ulang template Jasper, kecuali jika ukuran/posisi elemen visual
 * (bukan isi teks) yang perlu disesuaikan.</p>
 *
 * <p><b>Panduan pemeliharaan ke depan.</b> (1) Jika redaksi/rumus/langkah validasi perlu diperbarui,
 * ubah HANYA {@link #bangunKeteranganFormal} (untuk versi formal/JRXML) atau method
 * {@code keteranganPanelXxx} (untuk versi ringkas/panel) &mdash; perubahan otomatis berlaku ke kedua
 * arah laporan. (2) Jika di kemudian hari ditambahkan laporan angket dosen KETIGA dengan cakupan
 * agregasi berbeda lagi (mis. per-fakultas), tambahkan method publik baru yang memanggil
 * {@link #bangunKeteranganFormal} dengan parameter cakupan yang sesuai &mdash; JANGAN menulis ulang
 * narasi dari nol. (3) Konstanta nama laporan ({@link #NAMA_LAPORAN_RANGKING},
 * {@link #NAMA_LAPORAN_FULL}) SEHARUSNYA selalu sinkron dengan judul aktual pada masing-masing
 * {@code .jrxml} (elemen {@code <text>} pada title band) &mdash; jika judul laporan diganti, perbarui
 * konstanta ini juga agar narasi silang-rujuk tetap akurat.</p>
 *
 * <p><b>Kompatibilitas.</b> Class ini murni statis (tidak boleh diinstansiasi, lihat konstruktor
 * privat), tidak menyimpan state, dan tidak membuka koneksi/sesi apa pun &mdash; aman dipanggil dari
 * thread ZK maupun proses laporan latar. Ditulis dengan gaya Java 1.6/1.7 (tanpa lambda, tanpa
 * {@code StringBuilder} berantai yang memerlukan fitur baru, tanpa text block Java 15+) agar kompatibel
 * dengan target kompilasi proyek.</p>
 */
public final class AngketDosenMetodologiUtil {

	private AngketDosenMetodologiUtil() {
	}

	/**
	 * Nama resmi laporan "Rangking" persis seperti judul yang tercetak pada title band
	 * {@code rangking_angket_dosen_per_prodi.jrxml}. Dipakai sebagai bahan rujukan-silang di narasi
	 * laporan pasangannya (Hasil Angket) sehingga nama laporan yang disebut selalu konsisten.
	 */
	public static final String NAMA_LAPORAN_RANGKING = "Rangking Penilaian Mahasiswa atas Kinerja Dosen";

	/**
	 * Nama resmi laporan "Hasil Angket" (FULL) persis seperti judul yang tercetak pada title band
	 * {@code rekap_angket_dosen_per_dosen.jrxml}. Dipakai sebagai bahan rujukan-silang di narasi
	 * laporan pasangannya (Rangking) sehingga nama laporan yang disebut selalu konsisten.
	 */
	public static final String NAMA_LAPORAN_FULL = "Hasil Angket Penilaian Mahasiswa atas Kinerja Dosen";

	/** Rumus perhitungan skor, IDENTIK pada kedua query laporan &mdash; dikutip apa adanya di narasi. */
	private static final String RUMUS = "(Σ(Nilai x Bobot) x 100) / (ΣTotal x Jumlah Pilihan)";

	/**
	 * Menyusun catatan RINGKAS berformat HTML untuk ditampilkan pada panel konfigurasi (ZK, sisi
	 * West/form) laporan Rangking, SEBELUM pengguna menekan tombol "Lihat Laporan". Tujuannya memberi
	 * peringatan dini agar pengguna tidak terkejut saat membandingkan angka dengan laporan Hasil Angket,
	 * sekaligus mengarahkan ke penjelasan lengkap yang tercetak otomatis pada PDF (lihat
	 * {@link #keteranganJrxmlRangking()}). Dipakai oleh
	 * {@link ais.action.report.format1.akademik.LaporanRekapAngketDosenPerJurusanWindow} melalui
	 * {@code ais.common.Common#initKeteranganHtml(org.zkoss.zul.Rows, String)}.
	 *
	 * @return teks HTML singkat (aman dirender {@code ais.ui.util.MyHtml}), tidak pernah {@code null}
	 */
	public static String keteranganPanelRangking() {
		return "<b>Catatan:</b> Skor pada laporan ini dihitung <b>TERPISAH per Program Studi</b> "
				+ "(hanya dari mahasiswa Program Studi ybs). Untuk dosen yang mengampu lebih dari satu "
				+ "Program Studi, skor dapat berbeda dari laporan <i>" + NAMA_LAPORAN_FULL + "</i> (skor "
				+ "gabungan lintas Program Studi) &mdash; perbedaan ini <b>bukan kesalahan perhitungan</b>. "
				+ "Penjelasan metodologi lengkap beserta langkah validasi tercantum pada laporan (PDF) "
				+ "yang dihasilkan.";
	}

	/**
	 * Menyusun catatan RINGKAS berformat HTML untuk ditampilkan pada panel konfigurasi (ZK, sisi
	 * West/form) laporan Hasil Angket (FULL), SEBELUM pengguna menekan tombol "Lihat Laporan". Berlaku
	 * simetris terhadap {@link #keteranganPanelRangking()} &mdash; arah rujukan ditukar (laporan ini
	 * pooled, mengarahkan ke laporan Rangking untuk rincian per Program Studi). Dipakai oleh
	 * {@link ais.action.report.format1.akademik.LaporanAngketDosenPerDosenSajaWindow} melalui
	 * {@code ais.common.Common#initKeteranganHtml(org.zkoss.zul.Rows, String)}.
	 *
	 * @return teks HTML singkat (aman dirender {@code ais.ui.util.MyHtml}), tidak pernah {@code null}
	 */
	public static String keteranganPanelFull() {
		return "<b>Catatan:</b> Skor pada laporan ini adalah <b>skor GABUNGAN (pooled)</b> dari seluruh "
				+ "kelas/Program Studi yang diampu dosen. Untuk melihat skor per Program Studi secara "
				+ "terpisah, gunakan laporan <i>" + NAMA_LAPORAN_RANGKING + "</i>. Perbedaan angka antar "
				+ "kedua laporan &mdash; bagi dosen yang mengampu lebih dari satu Program Studi &mdash; "
				+ "<b>bukan kesalahan perhitungan</b>. Penjelasan metodologi lengkap beserta langkah "
				+ "validasi tercantum pada laporan (PDF) yang dihasilkan.";
	}

	/**
	 * Menyusun narasi FORMAL lengkap (definisi cakupan, rumus, alasan perbedaan, tiga langkah validasi)
	 * untuk dicetak pada title band {@code rangking_angket_dosen_per_prodi.jrxml}, sudut pandang laporan
	 * Rangking (per Program Studi) merujuk ke laporan Hasil Angket (pooled) sebagai pembanding. Nilai
	 * kembalian dikirim sebagai parameter Jasper {@code keterangan_metodologi} melalui
	 * {@code generateParameter()} pada
	 * {@link ais.action.report.format1.akademik.LaporanRekapAngketDosenPerJurusanWindow} &mdash;
	 * berkas {@code .jrxml} TIDAK menyimpan redaksi ini secara literal (lihat javadoc kelas).
	 *
	 * @return teks formal multi-paragraf, tidak pernah {@code null}
	 */
	public static String keteranganJrxmlRangking() {
		return bangunKeteranganFormal(NAMA_LAPORAN_RANGKING, "TERPISAH untuk setiap Program Studi, yaitu "
				+ "rata-rata tertimbang jawaban mahasiswa YANG BERASAL DARI PROGRAM STUDI TERSEBUT SAJA "
				+ "terhadap dosen yang bersangkutan", NAMA_LAPORAN_FULL, "secara GABUNGAN (pooled) dari "
				+ "SELURUH kelas yang diampu dosen yang bersangkutan lintas Program Studi, bukan per "
				+ "Program Studi");
	}

	/**
	 * Menyusun narasi FORMAL lengkap (definisi cakupan, rumus, alasan perbedaan, tiga langkah validasi)
	 * untuk dicetak pada title band {@code rekap_angket_dosen_per_dosen.jrxml}, sudut pandang laporan
	 * Hasil Angket (pooled) merujuk ke laporan Rangking (per Program Studi) sebagai pembanding. Nilai
	 * kembalian dikirim sebagai parameter Jasper {@code keterangan_metodologi} melalui
	 * {@code generateParameter()} pada
	 * {@link ais.action.report.format1.akademik.LaporanAngketDosenPerDosenSajaWindow} &mdash; berkas
	 * {@code .jrxml} TIDAK menyimpan redaksi ini secara literal (lihat javadoc kelas).
	 *
	 * @return teks formal multi-paragraf, tidak pernah {@code null}
	 */
	public static String keteranganJrxmlFull() {
		return bangunKeteranganFormal(NAMA_LAPORAN_FULL, "secara GABUNGAN (pooled) dari SELURUH "
				+ "kelas/Program Studi yang diampu oleh dosen yang bersangkutan pada periode yang dipilih",
				NAMA_LAPORAN_RANGKING, "TERPISAH untuk setiap Program Studi berdasarkan mahasiswa Program "
				+ "Studi yang bersangkutan saja");
	}

	/**
	 * Builder INTI (satu-satunya tempat redaksi formal ditulis) yang dipakai bersama oleh
	 * {@link #keteranganJrxmlRangking()} dan {@link #keteranganJrxmlFull()}. Menyusun narasi dua
	 * paragraf: paragraf pertama menjelaskan cakupan perhitungan laporan yang sedang ditampilkan
	 * ({@code namaLaporanIni}/{@code cakupanIni}), membandingkannya dengan laporan pasangan
	 * ({@code namaLaporanLain}/{@code cakupanLain}), dan menegaskan bahwa perbedaan angka BUKAN
	 * kesalahan hitung; paragraf kedua berisi TIGA langkah validasi yang dapat ditempuh pembaca laporan
	 * secara mandiri sebelum menyimpulkan adanya anomali data.
	 *
	 * <p>Parameter {@code cakupanIni}/{@code cakupanLain} disisipkan langsung ke tengah kalimat
	 * ("Skor pada laporan ini dihitung " + cakupanIni + ", dengan rumus..."), sehingga PENULIS pemanggil
	 * (lihat {@link #keteranganJrxmlRangking()}/{@link #keteranganJrxmlFull()}) bertanggung jawab
	 * memastikan potongan kalimat yang dikirim menyambung secara gramatikal dengan kalimat pembuka di
	 * method ini.</p>
	 *
	 * @param namaLaporanIni  nama laporan yang narasinya sedang disusun (akan tampil di judulnya sendiri)
	 * @param cakupanIni      potongan kalimat penjelas cakupan perhitungan laporan ini
	 * @param namaLaporanLain nama laporan pembanding yang dirujuk sebagai sumber angka yang berbeda
	 * @param cakupanLain     potongan kalimat penjelas cakupan perhitungan laporan pembanding
	 * @return narasi formal lengkap siap cetak, tidak pernah {@code null}
	 */
	private static String bangunKeteranganFormal(String namaLaporanIni, String cakupanIni,
			String namaLaporanLain, String cakupanLain) {
		StringBuilder sb = new StringBuilder();
		sb.append("Skor pada laporan ini dihitung ").append(cakupanIni).append(", dengan rumus: ")
				.append(RUMUS).append(". Bagi dosen yang mengampu perkuliahan pada LEBIH DARI SATU ")
				.append("Program Studi dalam periode yang sama, skor pada laporan ini secara WAJAR dapat ")
				.append("berbeda dari skor pada laporan \"").append(namaLaporanLain)
				.append("\", karena laporan tersebut menghitung skor ").append(cakupanLain)
				.append(". Perbedaan angka pada kedua laporan BUKAN merupakan kesalahan perhitungan, ")
				.append("melainkan konsekuensi metodologis dari perbedaan cakupan data sesuai tujuan ")
				.append("analisis masing-masing laporan.\n\n");
		sb.append("LANGKAH VALIDASI: (1) Untuk dosen yang HANYA mengampu SATU Program Studi, skor pada ")
				.append("laporan ini harus SAMA PERSIS dengan skor pada laporan \"").append(namaLaporanLain)
				.append("\"; ketidaksamaan pada kondisi ini mengindikasikan anomali data dan agar ")
				.append("dilaporkan kepada administrator sistem. (2) Untuk dosen yang mengampu LEBIH DARI ")
				.append("SATU Program Studi, skor gabungan pada laporan \"").append(NAMA_LAPORAN_FULL)
				.append("\" secara matematis akan berada DI ANTARA skor-skor per-Program-Studi pada ")
				.append("laporan \"").append(NAMA_LAPORAN_RANGKING).append("\" (merupakan rata-rata ")
				.append("tertimbang berdasarkan jumlah mahasiswa responden pada masing-masing Program ")
				.append("Studi); semakin banyak responden pada satu Program Studi, semakin dekat skor ")
				.append("gabungan tersebut kepada skor Program Studi itu. (3) Pastikan parameter Tahun ")
				.append("Akademik, Semester, Status Angket Aktif, dan Masa Perkuliahan yang digunakan ")
				.append("SAMA pada kedua laporan sebelum membandingkan angkanya.");
		return sb.toString();
	}
}
