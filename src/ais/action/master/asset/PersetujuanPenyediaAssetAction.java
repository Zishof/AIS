package ais.action.master.asset;

/**
 * <h3>PersetujuanPenyediaAssetAction — Persetujuan Pendaftaran Penyedia (Vendor) Aset</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link PenyediaAssetAction} yang mengaktifkan
 * mode persetujuan untuk pendaftaran penyedia (vendor) dalam modul pengadaan aset. Penyedia
 * aset adalah perusahaan atau individu yang menjual barang atau memberikan jasa kepada
 * organisasi. Sebelum vendor dapat dipilih dalam transaksi pengadaan (pemesanan, pembayaran,
 * kontrak), data vendor harus diverifikasi dan disetujui oleh pejabat berwenang untuk
 * memastikan legalitas, kualifikasi, dan kelayakan vendor tersebut berdasarkan kebijakan
 * pengadaan organisasi.</p>
 *
 * <p><b>Cara kerja:</b> Mewarisi semua logika dari {@link PenyediaAssetAction}.
 * Konstruktor memanggil {@code super(true)} untuk mengaktifkan flag persetujuan.
 * Metode {@link #istilah()} mengembalikan "Persetujuan Penyedia (Vendor)".</p>
 *
 * <p><b>Proses kualifikasi vendor:</b> Pendaftaran Vendor → Kelengkapan Dokumen (NPWP, SIUP,
 * akta, rekening bank) → Persetujuan Vendor (kelas ini) → Vendor Aktif → Dapat Dipilih
 * dalam Transaksi Pengadaan.</p>
 *
 * <p><b>Kontrol risiko:</b> Verifikasi vendor mencegah transaksi dengan pihak yang tidak
 * berkualifikasi, fiktif, atau masuk daftar hitam. Approver perlu memeriksa dokumen
 * legalitas dan rekam jejak vendor sebelum mengotorisasi.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanPenyediaAssetAction extends PenyediaAssetAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance dalam mode persetujuan pendaftaran penyedia (vendor).
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval dengan meneruskan
	 * {@code true} ke konstruktor {@link PenyediaAssetAction}.
	 * Flag ini mengaktifkan UI persetujuan (tombol Setujui/Tolak), filter kueri khusus
	 * (hanya vendor yang belum disetujui/menunggu verifikasi), serta menonaktifkan
	 * form pendaftaran vendor baru. Pejabat pengadaan atau panitia kualifikasi vendor
	 * membuka halaman ini untuk meninjau data dan dokumen vendor sebelum mengotorisasi.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini. Perubahan alur persetujuan
	 * dilakukan di {@link PenyediaAssetAction}.</p>
	 */
	public PersetujuanPenyediaAssetAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Penyedia (Vendor)" untuk judul halaman dan UI.
	 *
	 * <p><b>Tujuan:</b> Memberi konteks kepada approver — mereka sedang memverifikasi
	 * dan mengkualifikasi calon vendor untuk pengadaan aset, bukan sekadar menambah
	 * kontak baru. Label ini tampil di judul halaman dan breadcrumb. Tambahan "(Vendor)"
	 * dalam tanda kurung membantu pengguna yang tidak familiar dengan terminologi
	 * "penyedia" memahami konteks transaksinya.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; {@code throws Exception}
	 * untuk kompatibilitas kontrak induk.</p>
	 *
	 * @return string "Persetujuan Penyedia (Vendor)"
	 * @throws Exception tidak pernah dilempar
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Penyedia (Vendor)";
	}
}
