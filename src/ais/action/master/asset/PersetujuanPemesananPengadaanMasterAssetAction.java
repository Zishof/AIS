package ais.action.master.asset;

/**
 * <h3>PersetujuanPemesananPengadaanMasterAssetAction — Persetujuan Pemesanan Barang/Jasa</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link PemesananPengadaanMasterAssetAction} yang
 * mengaktifkan mode persetujuan untuk pemesanan (purchase order / SPK) pengadaan barang
 * dan jasa. Pemesanan adalah dokumen kontrak formal yang diterbitkan kepada vendor setelah
 * negosiasi harga dan spesifikasi selesai. Sebelum SPK dapat diterbitkan dan dikirim ke
 * vendor, pemesanan harus mendapat otorisasi dari pejabat berwenang untuk memastikan
 * kesesuaian anggaran, spesifikasi, dan kepatuhan terhadap prosedur pengadaan.</p>
 *
 * <p><b>Cara kerja:</b> Mewarisi semua logika dari {@link PemesananPengadaanMasterAssetAction}.
 * Konstruktor memanggil {@code super(true)} untuk mengaktifkan flag persetujuan.
 * Metode {@link #istilah()} mengembalikan "Persetujuan Pemesanan Barang/Jasa".</p>
 *
 * <p><b>Alur pengadaan:</b> Permintaan Pengadaan → Persetujuan Permintaan →
 * Seleksi Vendor → Pemesanan/SPK → Persetujuan Pemesanan (kelas ini) → DP →
 * Penerimaan Barang → Pembayaran.</p>
 *
 * <p><b>Pemisahan tanggung jawab:</b> Staf pengadaan membuat pemesanan via
 * {@link PemesananPengadaanMasterAssetAction}; pejabat berwenang mengotorisasi via kelas ini.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanPemesananPengadaanMasterAssetAction extends PemesananPengadaanMasterAssetAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance dalam mode persetujuan pemesanan barang/jasa pengadaan.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval dengan meneruskan
	 * {@code true} ke konstruktor {@link PemesananPengadaanMasterAssetAction}.
	 * Flag ini mengaktifkan UI persetujuan (tombol Setujui/Tolak), filter kueri khusus
	 * (hanya pemesanan yang menunggu otorisasi), serta menonaktifkan form input pemesanan baru.
	 * Pejabat pengadaan atau direktur membuka halaman ini untuk meninjau detail SPK — vendor,
	 * harga, spesifikasi — sebelum memberikan otorisasi penerbitan dokumen.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini. Perubahan alur persetujuan
	 * dilakukan di {@link PemesananPengadaanMasterAssetAction}.</p>
	 */
	public PersetujuanPemesananPengadaanMasterAssetAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Pemesanan Barang/Jasa" untuk judul halaman dan UI.
	 *
	 * <p><b>Tujuan:</b> Memberi konteks kepada approver — mereka sedang mengotorisasi
	 * dokumen pemesanan (SPK/PO) yang akan dikirim ke vendor, bukan sekadar melihat
	 * daftar permintaan. Label ini membedakan halaman dari permintaan pengadaan
	 * dan memastikan approver sadar bahwa keputusan mereka berakibat hukum (penerbitan kontrak).</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; {@code throws Exception}
	 * untuk kompatibilitas kontrak induk.</p>
	 *
	 * @return string "Persetujuan Pemesanan Barang/Jasa"
	 * @throws Exception tidak pernah dilempar
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Pemesanan Barang/Jasa";
	}
}
