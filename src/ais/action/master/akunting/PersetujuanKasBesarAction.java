package ais.action.master.akunting;

/**
 * <h3>PersetujuanKasBesarAction — Alur Persetujuan Transaksi Kas Besar</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link KasBesarAction} yang
 * mengaktifkan mode persetujuan (approval mode) untuk transaksi kas besar.
 * Pada mode ini, halaman ZUL yang terikat ke kelas ini menampilkan daftar
 * transaksi kas besar yang menunggu persetujuan dari pejabat berwenang,
 * biasanya kepala keuangan atau direktur keuangan. Kelas ini digunakan
 * pada alur otorisasi transaksi sebelum transaksi dapat diproses lebih lanjut
 * atau diposting ke jurnal akuntansi.</p>
 *
 * <p><b>Cara kerja:</b> Tidak ada logika baru di kelas ini. Seluruh fungsi
 * diwarisi dari {@link KasBesarAction}. Konstruktor memanggil
 * {@code super(true)} untuk mengaktifkan flag persetujuan di kelas induk,
 * yang akan menyesuaikan tampilan UI (menampilkan tombol Setujui/Tolak),
 * filter kueri (hanya menampilkan transaksi yang perlu disetujui), dan
 * logika bisnis terkait alur otorisasi. Metode {@link #istilah()} di-override
 * untuk mengembalikan label tampilan yang tepat ("Persetujuan Kas Besar")
 * sebagai judul halaman dan breadcrumb navigasi.</p>
 *
 * <p><b>Pola desain Persetujuan*:</b> Seluruh kelas {@code Persetujuan*Action}
 * dalam paket ini mengikuti pola yang sama: mewarisi Action utama, memanggil
 * {@code super(true)} di konstruktor, dan meng-override {@link #istilah()}.
 * Ini memungkinkan satu kelas induk menangani dua mode sekaligus (input dan
 * persetujuan) tanpa duplikasi logika, hanya dengan perbedaan flag tunggal.
 * Pola ini juga memudahkan konfigurasi hak akses berbasis role: role input
 * mendapatkan akses ke {@link KasBesarAction}, sedangkan role approver mendapatkan
 * akses ke {@link PersetujuanKasBesarAction}.</p>
 *
 * <p><b>Halaman ZUL terkait:</b> Biasanya terikat ke file ZUL tersendiri
 * (misalnya {@code persetujuan_kas_besar.zul}) yang dapat memiliki tampilan
 * berbeda dari {@code kas_besar.zul} meskipun menggunakan logika yang sama.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Perubahan
 * logika persetujuan harus dilakukan di {@link KasBesarAction} dengan
 * memeriksa flag persetujuan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanKasBesarAction extends KasBesarAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 * Nilai ini tidak digunakan dalam logika bisnis.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance {@code PersetujuanKasBesarAction} dalam mode persetujuan.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller ZK dalam mode approval, sehingga
	 * halaman yang diikat ke kelas ini berfungsi sebagai antarmuka persetujuan
	 * transaksi kas besar, bukan antarmuka input.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil konstruktor {@link KasBesarAction#KasBesarAction(boolean)}
	 * dengan argumen {@code true}, yang mengaktifkan flag persetujuan di kelas induk.
	 * Flag ini akan digunakan oleh {@link KasBesarAction} untuk:
	 * <ul>
	 *   <li>Menyesuaikan kueri Hibernate agar hanya mengambil transaksi yang
	 *       menunggu persetujuan (belum disetujui).</li>
	 *   <li>Menampilkan atau menyembunyikan tombol dan kolom yang hanya relevan
	 *       untuk mode persetujuan (misalnya tombol Setujui, Tolak).</li>
	 *   <li>Menerapkan logika bisnis khusus persetujuan, seperti pencatatan
	 *       siapa yang menyetujui dan kapan.</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Delegasi penuh ke konstruktor induk. Exception
	 * dari induk akan menyebar ke ZK framework.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika parameter konstruktor induk berubah, perbarui
	 * panggilan ini. Tidak ada logika tambahan di konstruktor ini.</p>
	 */
	public PersetujuanKasBesarAction() {
		super(true);
	}

	/**
	 * Mengembalikan label tampilan "Persetujuan Kas Besar" sebagai istilah/nama
	 * modul yang digunakan di judul halaman, breadcrumb, dan pesan UI.
	 *
	 * <p><b>Tujuan:</b> Membedakan tampilan halaman ini dari halaman induk
	 * ({@link KasBesarAction}) di antarmuka pengguna. Label "Persetujuan Kas Besar"
	 * memberi tahu pengguna bahwa mereka sedang berada di alur persetujuan,
	 * bukan alur input transaksi.</p>
	 *
	 * <p><b>Cara kerja:</b> Mengembalikan string literal "Persetujuan Kas Besar"
	 * yang digunakan oleh framework halaman untuk mengisi judul, breadcrumb navigasi,
	 * dan mungkin judul popup/dialog. Metode ini di-override dari kelas induk
	 * {@link KasBesarAction#istilah()} yang mengembalikan "Kas Besar".</p>
	 *
	 * <p><b>Penanganan error:</b> Mendeklarasikan {@code throws Exception} mengikuti
	 * kontrak kelas induk, meskipun implementasi ini tidak melempar exception apapun.
	 * Deklarasi ini diperlukan agar kompatibel dengan signature method di induk.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Perbarui string jika nama modul berubah sesuai
	 * kebijakan penamaan organisasi. Tidak ada ketergantungan eksternal.</p>
	 *
	 * @return string "Persetujuan Kas Besar" sebagai label tampilan modul ini
	 * @throws Exception tidak pernah dilempar; deklarasi untuk kompatibilitas kontrak
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Kas Besar";
	}
}
