package ais.action.master.akunting;

/**
 * <h3>PersetujuanUangMukaAction — Alur Persetujuan Pengajuan Uang Muka</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link UangMukaAction} yang
 * mengaktifkan mode persetujuan untuk transaksi uang muka (advance payment).
 * Uang muka dalam konteks keuangan organisasi adalah pembayaran di muka yang
 * diberikan kepada pegawai atau vendor sebelum pekerjaan atau pembelian
 * diselesaikan, dengan harapan akan dipertanggungjawabkan kemudian. Kelas ini
 * menyediakan antarmuka bagi pejabat berwenang untuk menyetujui atau menolak
 * pengajuan uang muka sebelum dana dicairkan.</p>
 *
 * <p><b>Cara kerja:</b> Mewarisi semua logika dari {@link UangMukaAction}.
 * Konstruktor memanggil {@code super(true)} untuk mengaktifkan flag persetujuan.
 * Metode {@link #istilah()} di-override untuk mengembalikan "Persetujuan Uang Muka"
 * sebagai label tampilan. Pola ini identik dengan semua kelas
 * {@code Persetujuan*Action} dalam paket ini — tidak ada duplikasi logika bisnis.</p>
 *
 * <p><b>Alur persetujuan:</b> Staf keuangan mengajukan uang muka melalui
 * {@link UangMukaAction}, lalu pejabat berwenang membuka halaman ini untuk
 * melihat daftar pengajuan yang menunggu dan memberikan persetujuan.
 * Setelah disetujui, pengajuan dapat diproses lebih lanjut (posting jurnal,
 * pencairan, dsb.).</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanUangMukaAction extends UangMukaAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance {@code PersetujuanUangMukaAction} dalam mode persetujuan.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval dengan
	 * meneruskan {@code true} ke konstruktor {@link UangMukaAction#UangMukaAction(boolean)},
	 * yang mengaktifkan penyesuaian UI (tombol Setujui/Tolak) dan filter kueri
	 * (hanya transaksi yang belum disetujui) di kelas induk. Pemisahan kelas
	 * memudahkan pengelolaan hak akses berbasis role di konfigurasi Tbmrole.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini. Perubahan logika
	 * persetujuan dilakukan di {@link UangMukaAction}.</p>
	 */
	public PersetujuanUangMukaAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Uang Muka" untuk judul halaman dan UI.
	 *
	 * <p><b>Tujuan:</b> Membedakan tampilan halaman ini dari halaman input uang muka
	 * ({@link UangMukaAction}). Label yang tepat membantu pejabat approver memahami
	 * konteks halaman yang mereka buka — alur persetujuan, bukan alur input.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; deklarasi {@code throws Exception}
	 * untuk kompatibilitas kontrak metode induk.</p>
	 *
	 * @return string "Persetujuan Uang Muka"
	 * @throws Exception tidak pernah dilempar; untuk kompatibilitas kontrak induk
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Uang Muka";
	}
}
