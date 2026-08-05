package ais.action.master.asset;

/**
 * <h3>PersetujuanPembayaranDpMasterAssetAction — Persetujuan Pembayaran Down Payment Pengadaan</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link PembayaranDpMasterAssetAction} yang
 * mengaktifkan mode persetujuan untuk pembayaran down payment (DP) kepada vendor pengadaan
 * aset. Down payment adalah pembayaran awal sebagian nilai kontrak sebelum barang atau jasa
 * dikirimkan, biasanya 20–30% dari total nilai kontrak. Kelas ini menyediakan antarmuka
 * bagi pejabat berwenang — seperti direktur keuangan atau kepala bagian keuangan — untuk
 * mengotorisasi pengajuan pembayaran DP sebelum dana ditransfer ke rekening vendor.</p>
 *
 * <p><b>Cara kerja:</b> Mewarisi semua logika tampilan, kueri, dan alur kerja dari
 * {@link PembayaranDpMasterAssetAction}. Konstruktor memanggil {@code super(true)} untuk
 * mengaktifkan flag persetujuan di kelas induk. Metode {@link #istilah()} mengembalikan
 * "Persetujuan Pembayaran DP" sebagai label tampilan halaman untuk membedakan dari
 * halaman input DP biasa.</p>
 *
 * <p><b>Alur pengadaan:</b> Permintaan → Pemesanan (SPK) → Persetujuan Pemesanan →
 * Pembayaran DP → Persetujuan DP (kelas ini) → Transfer Dana → Penerimaan Barang →
 * Pembayaran Termin/Pelunasan.</p>
 *
 * <p><b>Pemisahan tanggung jawab:</b> Dengan memisahkan kelas approver dari kelas input,
 * sistem dapat mengatur hak akses (role-based access control) yang berbeda tanpa
 * menduplikasi logika bisnis. Staf pengadaan menggunakan {@link PembayaranDpMasterAssetAction},
 * pejabat approver menggunakan kelas ini.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanPembayaranDpMasterAssetAction extends PembayaranDpMasterAssetAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance dalam mode persetujuan pembayaran DP pengadaan.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval dengan meneruskan
	 * {@code true} ke konstruktor {@link PembayaranDpMasterAssetAction}.
	 * Flag ini mengaktifkan UI persetujuan (tombol Setujui/Tolak), filter kueri khusus
	 * (hanya DP yang belum disetujui), serta menonaktifkan operasi input baru.
	 * Approver keuangan membuka halaman ini untuk meninjau dan mengotorisasi pengajuan
	 * pembayaran DP yang masuk dari tim pengadaan.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini. Perubahan alur persetujuan
	 * dilakukan di {@link PembayaranDpMasterAssetAction}.</p>
	 */
	public PersetujuanPembayaranDpMasterAssetAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Pembayaran DP" untuk judul halaman dan UI.
	 *
	 * <p><b>Tujuan:</b> Memberi konteks halaman kepada approver — mereka sedang mengotorisasi
	 * pembayaran DP ke vendor, bukan mencatat pengajuan DP baru. "DP" adalah singkatan
	 * Down Payment (uang muka vendor sebelum pengiriman). Label digunakan di judul halaman
	 * dan breadcrumb navigasi untuk menghindari kebingungan dengan halaman input.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; {@code throws Exception}
	 * untuk kompatibilitas kontrak induk.</p>
	 *
	 * @return string "Persetujuan Pembayaran DP"
	 * @throws Exception tidak pernah dilempar
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Pembayaran DP";
	}
}
