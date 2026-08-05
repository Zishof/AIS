package ais.action.master.asset;

/**
 * <h3>PersetujuanPenghapusanMasterAssetAction — Persetujuan Penghapusan Barang/Aset</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link PenghapusanMasterAssetAction} yang
 * mengaktifkan mode persetujuan untuk penghapusan (disposal/write-off) barang atau aset
 * dari inventaris organisasi. Penghapusan aset adalah keputusan penting yang bersifat
 * permanen dan tidak dapat dibatalkan — barang akan dihapus dari buku aset dan tidak
 * lagi menjadi milik organisasi. Proses penghapusan biasanya dilakukan karena:
 * aset sudah rusak total, masa manfaat ekonomis habis, dijual/dilelang, atau hilang.
 * Oleh karena itu, penghapusan memerlukan otorisasi dari pejabat tertinggi yang berwenang.</p>
 *
 * <p><b>Cara kerja:</b> Mewarisi semua logika dari {@link PenghapusanMasterAssetAction}.
 * Konstruktor memanggil {@code super(true)} untuk mengaktifkan flag persetujuan.
 * Metode {@link #istilah()} mengembalikan "Persetujuan Penghapusan Barang".</p>
 *
 * <p><b>Alur penghapusan:</b> Pengajuan Penghapusan → Evaluasi Kondisi → Penilaian Nilai Sisa →
 * Persetujuan Penghapusan (kelas ini) → Hapus dari Inventaris → Posting Jurnal Penghapusan.</p>
 *
 * <p><b>Konsekuensi finansial:</b> Penghapusan berdampak pada laporan keuangan —
 * nilai buku aset dihapus dan kerugian/keuntungan penghapusan dicatat sebagai beban/pendapatan
 * non-operasional. Karena itu level approver biasanya lebih tinggi dari transaksi lain.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanPenghapusanMasterAssetAction extends PenghapusanMasterAssetAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance dalam mode persetujuan penghapusan barang/aset.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval dengan meneruskan
	 * {@code true} ke konstruktor {@link PenghapusanMasterAssetAction}.
	 * Flag ini mengaktifkan UI persetujuan (tombol Setujui/Tolak), filter kueri khusus
	 * (hanya pengajuan penghapusan yang menunggu otorisasi), serta menonaktifkan form input baru.
	 * Pejabat berwenang (direktur atau pimpinan) membuka halaman ini untuk meninjau
	 * alasan penghapusan, nilai buku tersisa, dan memberikan otorisasi akhir.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini. Perubahan alur persetujuan
	 * dilakukan di {@link PenghapusanMasterAssetAction}.</p>
	 */
	public PersetujuanPenghapusanMasterAssetAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Penghapusan Barang" untuk judul halaman dan UI.
	 *
	 * <p><b>Tujuan:</b> Memberi konteks kepada approver — mereka sedang mengotorisasi
	 * penghapusan permanen aset dari inventaris, sebuah keputusan tidak dapat dibatalkan.
	 * Label ini tampil di judul halaman dan breadcrumb untuk memastikan approver
	 * menyadari bobot keputusan sebelum mengklik Setujui.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; {@code throws Exception}
	 * untuk kompatibilitas kontrak induk.</p>
	 *
	 * @return string "Persetujuan Penghapusan Barang"
	 * @throws Exception tidak pernah dilempar
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Penghapusan Barang";
	}
}
