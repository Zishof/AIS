package ais.action.master.asset;

/**
 * <h3>PersetujuanPeminjamanMasterAssetAction — Persetujuan Peminjaman Barang/Aset</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link PeminjamanMasterAssetAction} yang
 * mengaktifkan mode persetujuan untuk pengajuan peminjaman barang atau aset organisasi.
 * Peminjaman aset mencakup penggunaan sementara barang (kendaraan, peralatan, properti,
 * atau aset lainnya) oleh pihak internal maupun eksternal. Sebelum barang dapat
 * dipinjamkan secara resmi, pengajuan peminjaman harus mendapat otorisasi dari pejabat
 * berwenang — manajer aset atau kepala bagian — untuk memastikan ketersediaan barang,
 * kelayakan peminjam, dan kepatuhan terhadap kebijakan penggunaan aset organisasi.</p>
 *
 * <p><b>Cara kerja:</b> Mewarisi semua logika dari {@link PeminjamanMasterAssetAction}.
 * Konstruktor memanggil {@code super(true)} untuk mengaktifkan flag persetujuan.
 * Metode {@link #istilah()} mengembalikan "Persetujuan Peminjaman Barang".</p>
 *
 * <p><b>Alur peminjaman:</b> Pengajuan Peminjaman → Persetujuan Peminjaman (kelas ini) →
 * Serah Terima Barang → Penggunaan → Pengembalian → Persetujuan Pengembalian.</p>
 *
 * <p><b>Pemisahan tanggung jawab:</b> Staf/peminjam mengajukan via
 * {@link PeminjamanMasterAssetAction}; manajer aset mengotorisasi via kelas ini.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanPeminjamanMasterAssetAction extends PeminjamanMasterAssetAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance dalam mode persetujuan peminjaman barang/aset.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval dengan meneruskan
	 * {@code true} ke konstruktor {@link PeminjamanMasterAssetAction}.
	 * Flag ini mengaktifkan UI persetujuan (tombol Setujui/Tolak), filter kueri khusus
	 * (hanya peminjaman yang menunggu otorisasi), serta menonaktifkan form input baru.
	 * Manajer aset atau pejabat berwenang membuka halaman ini untuk meninjau detail
	 * peminjaman — peminjam, jenis barang, durasi, tujuan — sebelum mengotorisasi.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini. Perubahan alur persetujuan
	 * dilakukan di {@link PeminjamanMasterAssetAction}.</p>
	 */
	public PersetujuanPeminjamanMasterAssetAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Peminjaman Barang" untuk judul halaman dan UI.
	 *
	 * <p><b>Tujuan:</b> Memberi konteks kepada approver — mereka sedang mengotorisasi
	 * peminjaman sementara barang/aset, bukan pemakaian permanen atau penghapusan.
	 * Label ini tampil di judul halaman dan breadcrumb untuk memastikan approver
	 * memahami konteks pengajuan yang mereka tinjau.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; {@code throws Exception}
	 * untuk kompatibilitas kontrak induk.</p>
	 *
	 * @return string "Persetujuan Peminjaman Barang"
	 * @throws Exception tidak pernah dilempar
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Peminjaman Barang";
	}
}
