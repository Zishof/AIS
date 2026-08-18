package ais.action.master.akunting;

/**
 * <h3>SemuaGrupTransaksiAction — Semua Jurnal Tersimpan (Termasuk Sudah Terposting)</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link GrupTransaksiAction} yang
 * mengaktifkan mode tampilan "Semua Jurnal" — menampilkan seluruh grup transaksi
 * (jurnal) yang tersimpan, baik yang belum diposting maupun yang sudah terposting
 * ke buku besar. Berbeda dengan {@link GrupTransaksiAction} versi default (hanya
 * draft/belum terposting), kelas ini digunakan pada halaman "semua_grup_transaksi.zul"
 * untuk memberikan visibilitas penuh atas histori transaksi keuangan.</p>
 *
 * <p><b>Cara kerja:</b> Kelas ini tidak memiliki logika sendiri. Seluruh fungsionalitas
 * diwarisi dari {@link GrupTransaksiAction} dan hanya berbeda pada argumen yang
 * diteruskan ke konstruktor induk. Konstruktor memanggil {@code super(true, false)},
 * di mana argumen pertama ({@code true}) mengaktifkan flag "semua transaksi" yang
 * menyebabkan kueri Hibernate tidak hanya mengambil transaksi belum terposting, tetapi
 * juga transaksi yang sudah memiliki {@code PostingHistory}. Argumen kedua ({@code false})
 * menandai bahwa ini bukan mode persetujuan (approval mode).</p>
 *
 * <p><b>Perbedaan dari GrupTransaksiAction (mode default):</b>
 * <ul>
 *   <li>Mode default (super(false, false)): hanya menampilkan draft jurnal yang belum
 *       terposting — digunakan di halaman "grup_transaksi.zul" (Draft Jurnal).</li>
 *   <li>Mode ini (super(true, false)): menampilkan semua jurnal termasuk yang sudah
 *       terposting — digunakan di halaman "semua_grup_transaksi.zul" (Semua Jurnal
 *       Tersimpan), berguna untuk audit, rekonsiliasi, dan pelaporan histori.</li>
 * </ul>
 * </p>
 *
 * <p><b>Halaman ZUL yang menggunakan kelas ini:</b>
 * {@code semua_grup_transaksi.zul} — halaman "Semua Jurnal Tersimpan" yang biasanya
 * diakses dari menu Akuntansi untuk melihat keseluruhan jurnal yang pernah dibuat,
 * lengkap dengan filter status posting, jenis jurnal, dan nominal.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan di kelas ini.
 * Penambahan perilaku khusus mode "semua" harus dilakukan di
 * {@link GrupTransaksiAction} dengan memeriksa flag sesuai. Java 1.7, ZKoss 5.5.
 * Serializable karena mewarisi {@code GenericAutowireComposer} ZK.</p>
 */
public class SemuaGrupTransaksiAction extends GrupTransaksiAction {

	/**
	 * ID serialisasi default yang dibutuhkan karena kelas ini mewarisi
	 * {@code Serializable} via rantai warisan ZK {@code GenericAutowireComposer}.
	 * Nilai ini tidak digunakan secara langsung dalam logika bisnis.
	 */
	private static final long serialVersionUID = -4685112302170330769L;

	/**
	 * Membuat instance {@code SemuaGrupTransaksiAction} dengan mode "semua jurnal"
	 * diaktifkan.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller ZK dalam mode tampilan "Semua
	 * Jurnal Tersimpan", yang berarti kueri data tidak hanya mengambil draft jurnal
	 * (belum terposting), tetapi juga jurnal yang sudah terposting ke buku besar.
	 * Mode ini cocok untuk halaman histori dan rekonsiliasi akuntansi.</p>
	 *
	 * <p><b>Cara kerja:</b> Meneruskan argumen {@code (true, false)} ke konstruktor
	 * {@link GrupTransaksiAction#GrupTransaksiAction(boolean, boolean)}.
	 * <ul>
	 *   <li>Argumen pertama {@code true}: mengaktifkan flag "tampil semua" di induk,
	 *       sehingga kriteria kueri tidak membatasi hanya pada transaksi yang belum
	 *       memiliki posting history — transaksi yang sudah diposting pun ikut
	 *       muncul dalam daftar.</li>
	 *   <li>Argumen kedua {@code false}: mematikan mode persetujuan (approval mode),
	 *       sehingga UI yang ditampilkan adalah tampilan standar (bukan tampilan
	 *       persetujuan dengan tombol Setujui/Tolak). Pengguna yang mengakses halaman
	 *       ini dapat melihat dan menelusuri semua jurnal tetapi tidak secara
	 *       langsung menjalankan alur persetujuan dari halaman ini.</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Tidak ada logika di konstruktor ini; delegasi penuh
	 * ke induk. Jika {@link GrupTransaksiAction} melempar exception pada inisialisasi,
	 * exception tersebut akan menyebar ke ZK framework yang akan menanganinya.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika semantik argumen konstruktor induk berubah,
	 * perbarui dokumentasi ini dan periksa apakah perlu menyesuaikan argumen.</p>
	 */
	public SemuaGrupTransaksiAction() {
		super(true, false);
	}

}
