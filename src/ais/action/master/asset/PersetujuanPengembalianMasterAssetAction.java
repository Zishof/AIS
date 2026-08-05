package ais.action.master.asset;

/**
 * <h3>PersetujuanPengembalianMasterAssetAction — Persetujuan Pengembalian Barang/Aset</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link PengembalianMasterAssetAction} yang
 * mengaktifkan mode persetujuan untuk pengembalian barang atau aset setelah masa peminjaman
 * atau penggunaan berakhir. Pengembalian aset adalah proses formal di mana peminjam
 * mengembalikan barang kepada pengelola aset, disertai pemeriksaan kondisi barang apakah
 * kembali dalam keadaan baik atau ada kerusakan. Pejabat berwenang harus mengotorisasi
 * berita acara pengembalian untuk menutup siklus peminjaman secara resmi dalam sistem.</p>
 *
 * <p><b>Cara kerja:</b> Mewarisi semua logika dari {@link PengembalianMasterAssetAction}.
 * Konstruktor memanggil {@code super(true)} untuk mengaktifkan flag persetujuan.
 * Metode {@link #istilah()} mengembalikan "Persetujuan Pengembalian Barang".</p>
 *
 * <p><b>Alur peminjaman:</b> Peminjaman → Persetujuan Peminjaman → Serah Terima →
 * Penggunaan → Pengembalian → Persetujuan Pengembalian (kelas ini) → Penutupan Siklus.</p>
 *
 * <p><b>Pemeriksaan kondisi:</b> Saat persetujuan, approver memverifikasi kondisi barang
 * dikembalikan, mencatat kerusakan jika ada, dan menentukan apakah dikenakan denda
 * atau biaya perbaikan kepada peminjam.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanPengembalianMasterAssetAction extends PengembalianMasterAssetAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance dalam mode persetujuan pengembalian barang/aset.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval dengan meneruskan
	 * {@code true} ke konstruktor {@link PengembalianMasterAssetAction}.
	 * Flag ini mengaktifkan UI persetujuan (tombol Setujui/Tolak), filter kueri khusus
	 * (hanya pengembalian yang menunggu otorisasi), serta menonaktifkan input baru.
	 * Manajer aset membuka halaman ini untuk memverifikasi kondisi barang yang dikembalikan
	 * dan mengotorisasi penutupan siklus peminjaman.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini. Perubahan alur persetujuan
	 * dilakukan di {@link PengembalianMasterAssetAction}.</p>
	 */
	public PersetujuanPengembalianMasterAssetAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Pengembalian Barang" untuk judul halaman dan UI.
	 *
	 * <p><b>Tujuan:</b> Memberi konteks kepada approver — mereka sedang memverifikasi dan
	 * mengotorisasi pengembalian barang dari peminjam, bukan mencatat pengembalian baru.
	 * Label ini tampil di judul halaman dan breadcrumb untuk membedakan dari halaman
	 * input pengembalian dan memastikan approver memahami konteks keputusan mereka.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; {@code throws Exception}
	 * untuk kompatibilitas kontrak induk.</p>
	 *
	 * @return string "Persetujuan Pengembalian Barang"
	 * @throws Exception tidak pernah dilempar
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Pengembalian Barang";
	}
}
