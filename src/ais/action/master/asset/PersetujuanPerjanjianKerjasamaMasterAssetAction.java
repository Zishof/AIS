package ais.action.master.asset;

/**
 * <h3>PersetujuanPerjanjianKerjasamaMasterAssetAction — Persetujuan Perjanjian Kerjasama Aset</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link PerjanjianKerjasamaMasterAssetAction} yang
 * mengaktifkan mode persetujuan untuk perjanjian kerjasama terkait aset organisasi. Perjanjian
 * kerjasama aset mencakup berbagai bentuk kerja sama seperti: joint use agreement (berbagi
 * penggunaan aset), operating lease (sewa operasional), build-operate-transfer (BOT),
 * atau perjanjian pengelolaan bersama. Dokumen perjanjian ini memiliki implikasi hukum
 * dan finansial jangka panjang sehingga memerlukan otorisasi dari pimpinan tertinggi
 * sebelum dapat ditandatangani dan diimplementasikan.</p>
 *
 * <p><b>Cara kerja:</b> Mewarisi semua logika dari {@link PerjanjianKerjasamaMasterAssetAction}.
 * Konstruktor memanggil {@code super(true)} untuk mengaktifkan flag persetujuan.
 * Metode {@link #istilah()} mengembalikan "Persetujuan Perjanjian Kerjasama".</p>
 *
 * <p><b>Alur perjanjian:</b> Negosiasi → Penyusunan Draft Perjanjian → Review Legal →
 * Persetujuan Perjanjian (kelas ini) → Penandatanganan → Implementasi → Monitoring.</p>
 *
 * <p><b>Urgensi kontrol:</b> Perjanjian kerjasama biasanya berdurasi multi-tahun dan
 * melibatkan nilai yang signifikan. Persetujuan formal diperlukan untuk memastikan
 * kepatuhan terhadap regulasi, kebijakan organisasi, dan perlindungan kepentingan aset.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanPerjanjianKerjasamaMasterAssetAction extends PerjanjianKerjasamaMasterAssetAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance dalam mode persetujuan perjanjian kerjasama aset.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval dengan meneruskan
	 * {@code true} ke konstruktor {@link PerjanjianKerjasamaMasterAssetAction}.
	 * Flag ini mengaktifkan UI persetujuan (tombol Setujui/Tolak), filter kueri khusus
	 * (hanya perjanjian yang menunggu otorisasi), serta menonaktifkan form input perjanjian baru.
	 * Pimpinan atau direktur membuka halaman ini untuk meninjau klausul perjanjian,
	 * durasi, nilai, dan mitra kerjasama sebelum memberikan otorisasi.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini. Perubahan alur persetujuan
	 * dilakukan di {@link PerjanjianKerjasamaMasterAssetAction}.</p>
	 */
	public PersetujuanPerjanjianKerjasamaMasterAssetAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Perjanjian Kerjasama" untuk judul halaman dan UI.
	 *
	 * <p><b>Tujuan:</b> Memberi konteks kepada approver — mereka sedang mengotorisasi
	 * dokumen perjanjian kerjasama yang memiliki kekuatan hukum mengikat. Label ini
	 * tampil di judul halaman dan breadcrumb untuk memastikan approver menyadari
	 * bobot hukum dari otorisasi yang mereka berikan.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; {@code throws Exception}
	 * untuk kompatibilitas kontrak induk.</p>
	 *
	 * @return string "Persetujuan Perjanjian Kerjasama"
	 * @throws Exception tidak pernah dilempar
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Perjanjian Kerjasama";
	}
}
