package ais.action.master.akunting;

/**
 * <h3>PersetujuanProsesTransferStandingInstructionAction — Persetujuan Standing Instruction</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link ProsesTransferStandingInstructionAction}
 * yang mengaktifkan mode persetujuan untuk proses transfer berdasarkan standing instruction.
 * Standing Instruction adalah instruksi transfer rutin yang sudah dikonfigurasi sebelumnya
 * (misalnya pembayaran gaji otomatis, pembayaran cicilan rutin, atau transfer dana ke
 * rekening tertentu pada jadwal tertentu). Meskipun bersifat rutin, setiap eksekusi
 * standing instruction tetap memerlukan otorisasi dari pejabat berwenang sebelum
 * transaksi dieksekusi, untuk memastikan akurasi jumlah dan validitas instruksi.</p>
 *
 * <p><b>Cara kerja:</b> Mewarisi semua logika dari
 * {@link ProsesTransferStandingInstructionAction}. Konstruktor memanggil {@code super(true)}
 * untuk mengaktifkan flag persetujuan. Metode {@link #istilah()} mengembalikan
 * "Persetujuan Standing Instruction". Pola ini konsisten dengan semua
 * {@code Persetujuan*Action} dalam paket ini.</p>
 *
 * <p><b>Konteks bisnis standing instruction:</b> Proses dimulai dari
 * {@link StandingInstructionAction} (konfigurasi instruksi) →
 * {@link ProsesTransferStandingInstructionAction} (eksekusi pengajuan) →
 * kelas ini (persetujuan) → eksekusi transfer ke bank. Dengan alur tiga tahap
 * ini, organisasi memiliki kontrol penuh atas transfer rutin sekalipun.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanProsesTransferStandingInstructionAction extends ProsesTransferStandingInstructionAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance dalam mode persetujuan standing instruction.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval dengan
	 * meneruskan {@code true} ke konstruktor
	 * {@link ProsesTransferStandingInstructionAction#ProsesTransferStandingInstructionAction(boolean)}.
	 * Flag ini mengaktifkan UI persetujuan (tombol Setujui/Tolak) dan filter kueri
	 * (hanya instruksi yang menunggu otorisasi) di kelas induk.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini.</p>
	 */
	public PersetujuanProsesTransferStandingInstructionAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Standing Instruction" untuk UI.
	 *
	 * <p><b>Tujuan:</b> Memberi konteks halaman kepada approver — mereka sedang
	 * mengotorisasi eksekusi standing instruction yang sudah dikonfigurasi, bukan
	 * membuat instruksi transfer baru. Label singkat "Standing Instruction" (tanpa
	 * "Proses Transfer") digunakan untuk keringkasan di breadcrumb dan judul halaman.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; {@code throws Exception}
	 * untuk kompatibilitas kontrak induk.</p>
	 *
	 * @return string "Persetujuan Standing Instruction"
	 * @throws Exception tidak pernah dilempar
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Standing Instruction";
	}
}
