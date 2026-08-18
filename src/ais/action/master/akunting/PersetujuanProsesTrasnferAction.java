package ais.action.master.akunting;

/**
 * <h3>PersetujuanProsesTrasnferAction — Alur Persetujuan Proses Transfer Dana</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link ProsesTransferAction} yang
 * mengaktifkan mode persetujuan untuk proses transfer dana antar rekening atau
 * antar entitas keuangan. Transfer dana dalam sistem ini mencakup perpindahan
 * uang antar rekening bank, antar divisi, atau antar unit kerja yang membutuhkan
 * otorisasi sebelum benar-benar dieksekusi. Kelas ini menyediakan antarmuka bagi
 * pejabat berwenang (kepala keuangan, direktur keuangan) untuk menyetujui atau
 * menolak pengajuan transfer yang masuk.</p>
 *
 * <p><b>Catatan penamaan:</b> Nama kelas mengandung typo ("Trasfer" tanpa 'n')
 * yang merupakan warisan dari kode awal. Nama ini dipertahankan untuk kompatibilitas
 * karena digunakan dalam konfigurasi ZUL dan mapping bean ZK. Perubahan nama
 * memerlukan pembaruan di seluruh konfigurasi terkait.</p>
 *
 * <p><b>Cara kerja:</b> Mewarisi semua logika dari {@link ProsesTransferAction}.
 * Konstruktor memanggil {@code super(true)} untuk mengaktifkan flag persetujuan.
 * Metode {@link #istilah()} mengembalikan "Persetujuan Proses Transfer" sebagai
 * label tampilan. Pola ini konsisten dengan semua kelas {@code Persetujuan*Action}
 * dalam paket ini.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanProsesTrasnferAction extends ProsesTransferAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance {@code PersetujuanProsesTrasnferAction} dalam mode persetujuan.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval transfer
	 * dengan meneruskan {@code true} ke konstruktor
	 * {@link ProsesTransferAction#ProsesTransferAction(boolean)}.
	 * Flag ini mengaktifkan penyesuaian UI mode persetujuan dan filter kueri
	 * (hanya transfer yang belum diotorisasi) di kelas induk. Role approver
	 * dikonfigurasi terpisah dari role input di manajemen hak akses (Tbmrole).</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini. Seluruh logika persetujuan
	 * ada di {@link ProsesTransferAction}.</p>
	 */
	public PersetujuanProsesTrasnferAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Proses Transfer" untuk judul halaman dan UI.
	 *
	 * <p><b>Tujuan:</b> Membedakan tampilan halaman ini dari halaman input
	 * proses transfer ({@link ProsesTransferAction}). Label ini digunakan di
	 * judul halaman, breadcrumb, dan dialog konfirmasi persetujuan sehingga
	 * approver tahu mereka sedang di alur otorisasi transfer dana.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; deklarasi {@code throws Exception}
	 * untuk kompatibilitas kontrak metode induk.</p>
	 *
	 * @return string "Persetujuan Proses Transfer"
	 * @throws Exception tidak pernah dilempar; untuk kompatibilitas kontrak induk
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Proses Transfer";
	}
}
