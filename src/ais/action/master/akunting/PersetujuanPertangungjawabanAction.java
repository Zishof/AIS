package ais.action.master.akunting;

/**
 * <h3>PersetujuanPertangungjawabanAction — Alur Persetujuan Pertanggungjawaban Uang Muka</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link PertangungjawabanAction} yang
 * mengaktifkan mode persetujuan untuk laporan pertanggungjawaban uang muka.
 * Pertanggungjawaban uang muka adalah proses di mana penerima uang muka melaporkan
 * penggunaan dana yang diterimanya beserta bukti-bukti pengeluaran. Setelah laporan
 * diajukan, pejabat berwenang perlu memverifikasi dan menyetujui bahwa pertanggungjawaban
 * tersebut lengkap dan sesuai sebelum kasus uang muka ditutup secara akuntansi.</p>
 *
 * <p><b>Cara kerja:</b> Mewarisi semua logika dari {@link PertangungjawabanAction}.
 * Konstruktor memanggil {@code super(true)} untuk mengaktifkan flag persetujuan.
 * Metode {@link #istilah()} mengembalikan "Persetujuan Pertangungjawaban Uang Muka"
 * (perhatikan: satu 'g' di "Pertangungjawaban" adalah ejaan yang digunakan secara
 * konsisten dalam sistem ini). Pola ini konsisten dengan semua {@code Persetujuan*Action}
 * dalam paket ini.</p>
 *
 * <p><b>Siklus pertanggungjawaban uang muka:</b> Uang Muka (diajukan) →
 * Persetujuan Uang Muka (diotorisasi) → Pencairan → Pelaksanaan Kegiatan →
 * Laporan Pertanggungjawaban (diajukan via {@link PertangungjawabanAction}) →
 * Persetujuan Pertanggungjawaban (via kelas ini) → Penutupan Jurnal.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanPertangungjawabanAction extends PertangungjawabanAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance dalam mode persetujuan pertanggungjawaban uang muka.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval dengan
	 * meneruskan {@code true} ke konstruktor
	 * {@link PertangungjawabanAction#PertangungjawabanAction(boolean)},
	 * yang menyesuaikan UI (tombol Setujui/Tolak) dan filter kueri (hanya
	 * pertanggungjawaban yang belum diverifikasi) di kelas induk.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini.</p>
	 */
	public PersetujuanPertangungjawabanAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Pertangungjawaban Uang Muka" untuk UI.
	 *
	 * <p><b>Tujuan:</b> Memberikan konteks halaman — approver sedang memverifikasi
	 * laporan pertanggungjawaban penggunaan uang muka, bukan mengajukan uang muka
	 * baru. Label ini penting untuk membedakan tahap alur kerja kepada pengguna.</p>
	 *
	 * <p><b>Catatan ejaan:</b> "Pertangungjawaban" (satu 'g') adalah ejaan yang
	 * digunakan konsisten dalam seluruh sistem ini — dipertahankan untuk konsistensi
	 * tampilan dan kompatibilitas dengan konfigurasi yang sudah ada.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; {@code throws Exception}
	 * untuk kompatibilitas kontrak induk.</p>
	 *
	 * @return string "Persetujuan Pertangungjawaban Uang Muka"
	 * @throws Exception tidak pernah dilempar
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Pertangungjawaban Uang Muka";
	}
}
