package ais.action.master.asset;

/**
 * <h3>PersetujuanPermintaanPengadaanMasterAssetAction — Persetujuan Permintaan Pengadaan Barang/Jasa</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link PermintaanPengadaanMasterAssetAction} yang
 * mengaktifkan mode persetujuan untuk permintaan pengadaan barang atau jasa. Permintaan
 * pengadaan adalah dokumen awal dalam siklus pengadaan — biasanya disebut Purchase Requisition
 * (PR) — yang diajukan oleh unit kerja/departemen untuk meminta otorisasi pengadaan barang
 * atau jasa tertentu. Sebelum proses pemilihan vendor dan pemesanan dapat dimulai, permintaan
 * harus disetujui oleh atasan langsung atau pejabat keuangan untuk memverifikasi kebutuhan,
 * ketersediaan anggaran, dan kepatuhan terhadap rencana pengadaan.</p>
 *
 * <p><b>Cara kerja:</b> Mewarisi semua logika dari {@link PermintaanPengadaanMasterAssetAction}.
 * Konstruktor memanggil {@code super(true)} untuk mengaktifkan flag persetujuan.
 * Metode {@link #istilah()} mengembalikan "Persetujuan Pengadaan Barang/Jasa".</p>
 *
 * <p><b>Alur pengadaan:</b> Identifikasi Kebutuhan → Penyusunan PR → Persetujuan PR (kelas ini)
 * → Pemilihan Vendor → Pemesanan (PO/SPK) → Persetujuan PO → Penerimaan → Pembayaran.</p>
 *
 * <p><b>Gerbang anggaran:</b> Persetujuan permintaan pengadaan adalah gerbang pertama
 * dalam siklus pengadaan. Approver memverifikasi bahwa pengadaan sesuai dengan anggaran
 * yang disetujui dan mengikuti prosedur yang berlaku sebelum melanjutkan ke pemilihan vendor.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanPermintaanPengadaanMasterAssetAction extends PermintaanPengadaanMasterAssetAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance dalam mode persetujuan permintaan pengadaan barang/jasa.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval dengan meneruskan
	 * {@code true} ke konstruktor {@link PermintaanPengadaanMasterAssetAction}.
	 * Flag ini mengaktifkan UI persetujuan (tombol Setujui/Tolak), filter kueri khusus
	 * (hanya permintaan yang menunggu otorisasi), serta menonaktifkan form input PR baru.
	 * Atasan langsung atau pejabat keuangan membuka halaman ini untuk meninjau detail
	 * permintaan — jenis barang, jumlah, estimasi harga, justifikasi — dan mengotorisasi
	 * kelanjutan proses pengadaan.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini. Perubahan alur persetujuan
	 * dilakukan di {@link PermintaanPengadaanMasterAssetAction}.</p>
	 */
	public PersetujuanPermintaanPengadaanMasterAssetAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Pengadaan Barang/Jasa" untuk judul halaman dan UI.
	 *
	 * <p><b>Tujuan:</b> Memberi konteks kepada approver — mereka sedang mengotorisasi
	 * permintaan pengadaan (PR) yang merupakan titik awal siklus pembelian. Label ini
	 * tampil di judul halaman dan breadcrumb. Perhatikan bahwa label menghilangkan kata
	 * "Permintaan" dan langsung menyebut "Pengadaan" untuk keringkasan tampilan di UI.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; {@code throws Exception}
	 * untuk kompatibilitas kontrak induk.</p>
	 *
	 * @return string "Persetujuan Pengadaan Barang/Jasa"
	 * @throws Exception tidak pernah dilempar
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Pengadaan Barang/Jasa";
	}
}
