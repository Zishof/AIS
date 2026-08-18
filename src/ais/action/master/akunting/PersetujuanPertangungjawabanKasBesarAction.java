package ais.action.master.akunting;

/**
 * <h3>PersetujuanPertangungjawabanKasBesarAction — Persetujuan Pertanggungjawaban Kas Besar</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link PertangungjawabanKasBesarAction} yang
 * mengaktifkan mode persetujuan untuk pertanggungjawaban penggunaan dana dari kas besar.
 * Berbeda dengan pertanggungjawaban uang muka (yang berasal dari advance payment),
 * pertanggungjawaban kas besar melaporkan penggunaan dana langsung dari kas besar
 * (main cash) yang biasanya melibatkan jumlah yang lebih besar dan mekanisme pengawasan
 * yang lebih ketat. Kelas ini digunakan oleh pejabat keuangan senior untuk memverifikasi
 * dan menyetujui laporan penggunaan dana kas besar sebelum pembukuan ditutup.</p>
 *
 * <p><b>Cara kerja:</b> Mewarisi semua logika dari {@link PertangungjawabanKasBesarAction}.
 * Konstruktor memanggil {@code super(true)} untuk mengaktifkan flag persetujuan.
 * Metode {@link #istilah()} mengembalikan "Persetujuan Pertangungjawaban Kas Besar".
 * Pola ini konsisten dengan semua {@code Persetujuan*Action} dalam paket ini.</p>
 *
 * <p><b>Siklus kas besar:</b> Dana Talangan / Kas Besar (diajukan via
 * {@link DanaTalanganAction}) → Persetujuan Dana Talangan → Pencairan dari Kas Besar
 * → Penggunaan Dana → Pertanggungjawaban Kas Besar (diajukan via
 * {@link PertangungjawabanKasBesarAction}) → Persetujuan via kelas ini → Penutupan.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanPertangungjawabanKasBesarAction extends PertangungjawabanKasBesarAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance dalam mode persetujuan pertanggungjawaban kas besar.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval dengan
	 * meneruskan {@code true} ke konstruktor
	 * {@link PertangungjawabanKasBesarAction#PertangungjawabanKasBesarAction(boolean)}.
	 * Flag ini mengaktifkan UI persetujuan dan filter kueri khusus mode otorisasi
	 * di kelas induk. Pemisahan kelas memudahkan manajemen hak akses berbasis role.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini.</p>
	 */
	public PersetujuanPertangungjawabanKasBesarAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Pertangungjawaban Kas Besar" untuk UI.
	 *
	 * <p><b>Tujuan:</b> Memberikan konteks halaman — approver sedang memverifikasi
	 * penggunaan dana kas besar (bukan kas kecil atau uang muka), dengan jumlah
	 * yang biasanya lebih signifikan. Label ini muncul di judul halaman, breadcrumb,
	 * dan header dialog konfirmasi persetujuan.</p>
	 *
	 * <p><b>Catatan ejaan:</b> "Pertangungjawaban" (satu 'g') digunakan konsisten
	 * dalam sistem ini untuk kompatibilitas dengan konfigurasi yang sudah ada.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; {@code throws Exception}
	 * untuk kompatibilitas kontrak induk.</p>
	 *
	 * @return string "Persetujuan Pertangungjawaban Kas Besar"
	 * @throws Exception tidak pernah dilempar
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Pertangungjawaban Kas Besar";
	}
}
