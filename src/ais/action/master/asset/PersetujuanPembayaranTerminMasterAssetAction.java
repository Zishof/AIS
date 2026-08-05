package ais.action.master.asset;

/**
 * <h3>PersetujuanPembayaranTerminMasterAssetAction — Persetujuan Pembayaran Termin Pengadaan</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link PembayaranTerminMasterAssetAction} yang
 * mengaktifkan mode persetujuan untuk pembayaran termin kepada vendor pengadaan aset.
 * Pembayaran termin adalah pembayaran berkala/bertahap berdasarkan progress pekerjaan atau
 * jadwal yang disepakati dalam kontrak, biasanya untuk proyek besar seperti konstruksi,
 * pengembangan sistem, atau pengadaan barang dalam jumlah besar. Kelas ini menyediakan
 * antarmuka bagi pejabat berwenang untuk mengotorisasi pembayaran setiap termin
 * berdasarkan laporan progress yang sudah diverifikasi.</p>
 *
 * <p><b>Cara kerja:</b> Mewarisi semua logika dari {@link PembayaranTerminMasterAssetAction}.
 * Konstruktor memanggil {@code super(true)} untuk mengaktifkan flag persetujuan.
 * Metode {@link #istilah()} mengembalikan "Persetujuan Pembayaran Termin".</p>
 *
 * <p><b>Alur pembayaran termin:</b> Kontrak → DP (persetujuan terpisah) →
 * Termin 1 (progress 30%) → Persetujuan Termin 1 (kelas ini) → Transfer →
 * Termin 2 (progress 70%) → Persetujuan Termin 2 → Transfer → Pelunasan.</p>
 *
 * <p><b>Pemisahan tanggung jawab:</b> Staf keuangan mencatat termin via
 * {@link PembayaranTerminMasterAssetAction}; pejabat approver mengotorisasi via kelas ini.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanPembayaranTerminMasterAssetAction extends PembayaranTerminMasterAssetAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance dalam mode persetujuan pembayaran termin pengadaan.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval dengan meneruskan
	 * {@code true} ke konstruktor {@link PembayaranTerminMasterAssetAction}.
	 * Flag ini mengaktifkan UI persetujuan (tombol Setujui/Tolak), filter kueri khusus
	 * (hanya termin yang menunggu otorisasi), serta menonaktifkan input termin baru.
	 * Approver keuangan membuka halaman ini untuk memverifikasi laporan progress vendor
	 * dan mengotorisasi pembayaran sesuai jadwal termin kontrak.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini. Perubahan alur persetujuan
	 * dilakukan di {@link PembayaranTerminMasterAssetAction}.</p>
	 */
	public PersetujuanPembayaranTerminMasterAssetAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Pembayaran Termin" untuk judul halaman dan UI.
	 *
	 * <p><b>Tujuan:</b> Memberi konteks halaman kepada approver — mereka sedang mengotorisasi
	 * pembayaran termin (cicilan bertahap sesuai progress) kepada vendor, bukan DP ataupun
	 * pelunasan akhir. Label ini tampil di judul halaman dan breadcrumb untuk memastikan
	 * approver tidak keliru mengotorisasi jenis pembayaran yang salah.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; {@code throws Exception}
	 * untuk kompatibilitas kontrak induk.</p>
	 *
	 * @return string "Persetujuan Pembayaran Termin"
	 * @throws Exception tidak pernah dilempar
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Pembayaran Termin";
	}
}
