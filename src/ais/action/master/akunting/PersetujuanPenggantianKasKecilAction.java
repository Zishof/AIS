package ais.action.master.akunting;

/**
 * <h3>PersetujuanPenggantianKasKecilAction — Alur Persetujuan Penggantian Kas Kecil</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link PenggantianKasKecilAction}
 * yang mengaktifkan mode persetujuan untuk pengisian ulang (reimbursement) kas kecil.
 * Penggantian kas kecil adalah proses di mana dana kas kecil yang telah habis terpakai
 * diajukan untuk diisi ulang dari kas besar atau rekening utama organisasi. Proses ini
 * membutuhkan otorisasi dari pejabat berwenang sebelum transfer dana dapat dilakukan.
 * Kelas ini menyediakan antarmuka khusus bagi approver untuk meninjau dan menyetujui
 * pengajuan penggantian kas kecil dari berbagai departemen atau unit kerja.</p>
 *
 * <p><b>Cara kerja:</b> Mewarisi semua logika dari {@link PenggantianKasKecilAction}.
 * Konstruktor memanggil {@code super(true)} untuk mengaktifkan flag persetujuan.
 * Metode {@link #istilah()} mengembalikan "Persetujuan Penggantian Kas Kecil".
 * Pola ini konsisten dengan semua {@code Persetujuan*Action} dalam paket ini.</p>
 *
 * <p><b>Konteks bisnis:</b> Dalam sistem keuangan yang baik, siklus kas kecil
 * melibatkan tiga tahap: pengeluaran (dicatat oleh pemegang kas), pengajuan
 * penggantian (oleh bagian keuangan), dan persetujuan penggantian (oleh pejabat
 * berwenang via kelas ini) sebelum dana diisi ulang.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanPenggantianKasKecilAction extends PenggantianKasKecilAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance dalam mode persetujuan penggantian kas kecil.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval dengan
	 * meneruskan {@code true} ke konstruktor
	 * {@link PenggantianKasKecilAction#PenggantianKasKecilAction(boolean)},
	 * yang menyesuaikan UI dan logika bisnis untuk alur otorisasi. Approver
	 * melihat daftar pengajuan yang menunggu dan dapat menindaklanjutinya.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini.</p>
	 */
	public PersetujuanPenggantianKasKecilAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Penggantian Kas Kecil" untuk UI.
	 *
	 * <p><b>Tujuan:</b> Memberi konteks halaman kepada pengguna — mereka sedang
	 * menyetujui pengisian ulang kas kecil, bukan mengajukan pengeluaran baru.
	 * Label digunakan di judul halaman, breadcrumb, dan dialog konfirmasi.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; {@code throws Exception}
	 * untuk kompatibilitas kontrak induk.</p>
	 *
	 * @return string "Persetujuan Penggantian Kas Kecil"
	 * @throws Exception tidak pernah dilempar
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Penggantian Kas Kecil";
	}
}
