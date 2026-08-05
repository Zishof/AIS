package ais.action.master.asset;

/**
 * <h3>PersetujuanPembayaranMasterAssetAction — Persetujuan Pembayaran Barang/Jasa Pengadaan</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link PembayaranPengadaanMasterAssetAction} yang
 * mengaktifkan mode persetujuan untuk pembayaran final barang atau jasa pengadaan aset.
 * Berbeda dengan down payment (DP) yang merupakan uang muka, pembayaran ini adalah pelunasan
 * atau pembayaran utama kepada vendor setelah barang diterima dan diverifikasi. Kelas ini
 * menyediakan antarmuka bagi pejabat berwenang untuk mengotorisasi transfer dana pelunasan
 * ke rekening vendor berdasarkan bukti penerimaan dan tagihan yang sudah diverifikasi.</p>
 *
 * <p><b>Cara kerja:</b> Mewarisi semua logika dari {@link PembayaranPengadaanMasterAssetAction}.
 * Konstruktor memanggil {@code super(true)} untuk mengaktifkan flag persetujuan.
 * Metode {@link #istilah()} mengembalikan "Persetujuan Pembayaran Barang/Jasa".</p>
 *
 * <p><b>Alur pengadaan:</b> Permintaan → Pemesanan → Penerimaan → Terima Tagihan →
 * Pembayaran Barang/Jasa → Persetujuan Pembayaran (kelas ini) → Transfer Dana → Posting Jurnal.</p>
 *
 * <p><b>Pemisahan tanggung jawab:</b> Staf keuangan mencatat pengajuan pembayaran via
 * {@link PembayaranPengadaanMasterAssetAction}; pejabat approver memvalidasi dan mengotorisasi
 * via kelas ini, tanpa duplikasi logika bisnis.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanPembayaranMasterAssetAction extends PembayaranPengadaanMasterAssetAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance dalam mode persetujuan pembayaran barang/jasa pengadaan.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval dengan meneruskan
	 * {@code true} ke konstruktor {@link PembayaranPengadaanMasterAssetAction}.
	 * Flag ini mengaktifkan UI persetujuan dengan tombol Setujui/Tolak, filter kueri khusus
	 * hanya untuk pembayaran yang menunggu otorisasi, serta menonaktifkan form input baru.
	 * Approver keuangan senior membuka halaman ini untuk menelaah dan mengotorisasi
	 * pembayaran vendor yang diajukan staf.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini. Perubahan alur persetujuan
	 * dilakukan di {@link PembayaranPengadaanMasterAssetAction}.</p>
	 */
	public PersetujuanPembayaranMasterAssetAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Pembayaran Barang/Jasa" untuk judul halaman dan UI.
	 *
	 * <p><b>Tujuan:</b> Memberi konteks halaman kepada approver — mereka sedang memverifikasi
	 * pembayaran final (bukan DP atau termin) kepada vendor untuk barang atau jasa yang
	 * telah diterima. Label ini tampil di judul halaman, breadcrumb, dan header dialog
	 * konfirmasi persetujuan untuk membedakan dari halaman pembayaran lainnya.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; {@code throws Exception}
	 * untuk kompatibilitas kontrak induk.</p>
	 *
	 * @return string "Persetujuan Pembayaran Barang/Jasa"
	 * @throws Exception tidak pernah dilempar
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Pembayaran Barang/Jasa";
	}
}
