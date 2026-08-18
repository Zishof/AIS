package ais.action.master.akunting;

/**
 * <h3>PersetujuanKasKecilAction — Alur Persetujuan Transaksi Kas Kecil</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link KasKecilAction} yang
 * mengaktifkan mode persetujuan (approval mode) untuk transaksi petty cash
 * (kas kecil). Kelas ini digunakan pada alur otorisasi di mana transaksi kas
 * kecil yang diajukan oleh staf atau kepala bagian perlu mendapat persetujuan
 * dari pejabat berwenang sebelum dapat dicairkan atau dicatat secara resmi
 * di pembukuan. Mode persetujuan biasanya diakses oleh pengguna dengan role
 * khusus (kepala keuangan, direktur, dll.) yang berbeda dari pengguna yang
 * mengajukan transaksi.</p>
 *
 * <p><b>Cara kerja:</b> Tidak ada logika baru di kelas ini. Semua fungsionalitas
 * diwarisi dari {@link KasKecilAction}. Konstruktor memanggil {@code super(true)}
 * untuk mengaktifkan flag persetujuan. Metode {@link #istilah()} di-override
 * untuk mengembalikan label "Persetujuan Kas Kecil" yang membedakan tampilan
 * halaman ini dari halaman input kas kecil biasa. Pola ini konsisten dengan
 * semua kelas {@code Persetujuan*Action} lainnya dalam paket ini.</p>
 *
 * <p><b>Pola desain persetujuan:</b> Menggunakan template method pattern di mana
 * kelas induk menyediakan semua logika dengan variasi berdasarkan flag.
 * Pemisahan kelas memudahkan manajemen hak akses berbasis role: role "input"
 * mendapat akses {@link KasKecilAction}, role "approver" mendapat akses kelas
 * ini — tanpa duplikasi kode bisnis apapun.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanKasKecilAction extends KasKecilAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance {@code PersetujuanKasKecilAction} dalam mode persetujuan.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller ZK untuk alur persetujuan
	 * kas kecil dengan meneruskan flag {@code true} ke konstruktor induk
	 * {@link KasKecilAction#KasKecilAction(boolean)}. Flag ini mengaktifkan
	 * penyesuaian UI dan kueri khusus mode persetujuan di kelas induk, antara lain:
	 * menampilkan tombol Setujui/Tolak, memfilter hanya transaksi yang menunggu
	 * otorisasi, dan mencatat identitas penyetuju saat aksi persetujuan dijalankan.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini. Perubahan mode persetujuan
	 * dilakukan di {@link KasKecilAction}.</p>
	 */
	public PersetujuanKasKecilAction() {
		super(true);
	}

	/**
	 * Mengembalikan label tampilan "Persetujuan Kas Kecil" untuk judul halaman
	 * dan komponen UI lainnya yang membutuhkan nama modul.
	 *
	 * <p><b>Tujuan:</b> Membedakan tampilan ini dari halaman input kas kecil
	 * ({@link KasKecilAction} yang mengembalikan "Kas Kecil"). Label yang tepat
	 * membantu pengguna memahami konteks — mereka sedang di alur persetujuan,
	 * bukan alur input. Label ini juga digunakan di breadcrumb navigasi.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; deklarasi {@code throws Exception}
	 * mengikuti kontrak metode induk untuk kompatibilitas signature.</p>
	 *
	 * @return string "Persetujuan Kas Kecil"
	 * @throws Exception tidak pernah dilempar; untuk kompatibilitas kontrak induk
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Kas Kecil";
	}
}
