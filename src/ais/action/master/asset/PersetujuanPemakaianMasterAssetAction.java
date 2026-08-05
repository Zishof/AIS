package ais.action.master.asset;

/**
 * <h3>PersetujuanPemakaianMasterAssetAction — Alur Persetujuan Pemakaian Barang Aset</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link PemakaianMasterAssetAction} yang
 * mengaktifkan mode persetujuan untuk transaksi pemakaian barang dari inventaris aset.
 * Pemakaian barang adalah proses penggunaan barang/aset milik organisasi untuk keperluan
 * operasional, baik sementara maupun permanen. Sebelum barang dapat digunakan secara resmi,
 * pengajuan pemakaian harus mendapat persetujuan dari pejabat berwenang (kepala bagian,
 * manajer aset, atau direktur). Kelas ini menyediakan antarmuka khusus bagi pejabat approver
 * untuk meninjau dan menyetujui atau menolak pengajuan pemakaian barang yang masuk.</p>
 *
 * <p><b>Cara kerja:</b> Tidak ada logika baru di kelas ini. Seluruh fungsionalitas
 * diwarisi dari {@link PemakaianMasterAssetAction}. Konstruktor memanggil {@code super(true)}
 * untuk mengaktifkan flag persetujuan di induk. Metode {@link #istilah()} di-override
 * mengembalikan "Persetujuan Pemakaian Barang" sebagai label tampilan halaman.
 * Pola ini konsisten dengan semua kelas {@code Persetujuan*Action} dalam modul asset.</p>
 *
 * <p><b>Pola desain:</b> Pemisahan kelas memungkinkan pengaturan hak akses berbasis role
 * yang terpisah antara petugas yang mengajukan pemakaian ({@link PemakaianMasterAssetAction})
 * dan pejabat yang menyetujui (kelas ini). Tidak ada duplikasi logika bisnis.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanPemakaianMasterAssetAction extends PemakaianMasterAssetAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance dalam mode persetujuan pemakaian barang aset.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval dengan meneruskan
	 * {@code true} ke konstruktor {@link PemakaianMasterAssetAction#PemakaianMasterAssetAction(boolean)}.
	 * Flag ini mengaktifkan UI persetujuan (tombol Setujui/Tolak) dan filter kueri
	 * (hanya pemakaian yang menunggu otorisasi) di kelas induk. Approver membuka halaman ini
	 * untuk meninjau daftar pengajuan pemakaian dan memberikan keputusan.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini. Perubahan alur persetujuan
	 * dilakukan di {@link PemakaianMasterAssetAction}.</p>
	 */
	public PersetujuanPemakaianMasterAssetAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Pemakaian Barang" untuk judul halaman dan UI.
	 *
	 * <p><b>Tujuan:</b> Membedakan tampilan halaman ini dari halaman input pemakaian
	 * barang. Label ini memberi tahu approver bahwa mereka sedang di alur persetujuan,
	 * bukan alur pengajuan. Muncul di judul halaman, breadcrumb navigasi, dan header dialog.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; {@code throws Exception}
	 * untuk kompatibilitas kontrak induk.</p>
	 *
	 * @return string "Persetujuan Pemakaian Barang"
	 * @throws Exception tidak pernah dilempar
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Pemakaian Barang";
	}
}
