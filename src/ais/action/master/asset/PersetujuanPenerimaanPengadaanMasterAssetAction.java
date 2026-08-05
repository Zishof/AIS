package ais.action.master.asset;

/**
 * <h3>PersetujuanPenerimaanPengadaanMasterAssetAction — Persetujuan Penerimaan Barang/Jasa</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link PenerimaanPengadaanMasterAssetAction} yang
 * mengaktifkan mode persetujuan untuk penerimaan barang atau jasa dari vendor. Penerimaan
 * pengadaan adalah proses formal pemeriksaan dan verifikasi bahwa barang/jasa yang dikirim
 * vendor sesuai dengan spesifikasi kontrak — kuantitas, kualitas, kondisi, dan ketepatan waktu.
 * Setelah tim penerima mencatat penerimaan fisik, berita acara penerimaan (BAST) harus
 * mendapat otorisasi dari pejabat berwenang sebelum proses pembayaran dapat dimulai.</p>
 *
 * <p><b>Cara kerja:</b> Mewarisi semua logika dari {@link PenerimaanPengadaanMasterAssetAction}.
 * Konstruktor memanggil {@code super(true)} untuk mengaktifkan flag persetujuan.
 * Metode {@link #istilah()} mengembalikan "Persetujuan Penerimaan Barang/Jasa".</p>
 *
 * <p><b>Alur pengadaan:</b> Pemesanan (SPK) → Pengiriman Vendor → Penerimaan Fisik →
 * Persetujuan Penerimaan (kelas ini) → Terima Tagihan → Pembayaran → Posting Jurnal.</p>
 *
 * <p><b>Kontrol kualitas:</b> Tahap persetujuan penerimaan adalah titik kritis karena
 * menentukan apakah tagihan vendor layak diproses. Jika barang tidak sesuai spec,
 * approver dapat menolak dan meminta vendor melakukan pengiriman ulang atau kompensasi.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanPenerimaanPengadaanMasterAssetAction extends PenerimaanPengadaanMasterAssetAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance dalam mode persetujuan penerimaan barang/jasa pengadaan.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval dengan meneruskan
	 * {@code true} ke konstruktor {@link PenerimaanPengadaanMasterAssetAction}.
	 * Flag ini mengaktifkan UI persetujuan (tombol Setujui/Tolak), filter kueri khusus
	 * (hanya penerimaan yang menunggu otorisasi), serta menonaktifkan form input penerimaan baru.
	 * Kepala bagian atau pejabat teknis membuka halaman ini untuk memverifikasi BAST
	 * dan mengotorisasi penerimaan barang/jasa dari vendor.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini. Perubahan alur persetujuan
	 * dilakukan di {@link PenerimaanPengadaanMasterAssetAction}.</p>
	 */
	public PersetujuanPenerimaanPengadaanMasterAssetAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Penerimaan Barang/Jasa" untuk judul halaman dan UI.
	 *
	 * <p><b>Tujuan:</b> Memberi konteks kepada approver — mereka sedang mengotorisasi
	 * berita acara serah terima (BAST) barang atau jasa dari vendor, bukan mencatat
	 * penerimaan fisik baru. Label ini tampil di judul halaman dan breadcrumb untuk
	 * memastikan approver mengetahui bahwa keputusan mereka memperbolehkan proses pembayaran.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; {@code throws Exception}
	 * untuk kompatibilitas kontrak induk.</p>
	 *
	 * @return string "Persetujuan Penerimaan Barang/Jasa"
	 * @throws Exception tidak pernah dilempar
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Penerimaan Barang/Jasa";
	}
}
