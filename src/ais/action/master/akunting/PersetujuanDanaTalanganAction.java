package ais.action.master.akunting;

/**
 * <h3>PersetujuanDanaTalanganAction — Alur Persetujuan Dana Talangan / Kas Besar</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link DanaTalanganAction} yang
 * mengaktifkan mode persetujuan untuk transaksi dana talangan (bridging fund)
 * dan kas besar. Dana talangan adalah dana yang dipinjamkan sementara dari
 * kas organisasi untuk kebutuhan mendesak, yang harus dikembalikan atau
 * dipertanggungjawabkan dalam jangka waktu tertentu. Kelas ini menyediakan
 * antarmuka khusus bagi pejabat berwenang untuk mengotorisasi pengajuan dana
 * talangan sebelum dana dapat dicairkan dari kas besar.</p>
 *
 * <p><b>Cara kerja:</b> Mewarisi semua logika dari {@link DanaTalanganAction}.
 * Konstruktor memanggil {@code super(true)} untuk mengaktifkan flag persetujuan.
 * Metode {@link #istilah()} mengembalikan "Persetujuan Dana Talangan / Kas Besar"
 * — label gabungan yang mencerminkan bahwa sumber dana talangan adalah kas besar.
 * Pola ini konsisten dengan semua {@code Persetujuan*Action} dalam paket ini.</p>
 *
 * <p><b>Konteks bisnis:</b> Dana talangan sering digunakan dalam situasi di mana
 * tagihan atau kebutuhan mendesak datang sebelum anggaran resmi cair. Persetujuan
 * memastikan penggunaan dana talangan terkontrol dan dapat diaudit. Setelah
 * disetujui, transaksi dapat diposting ke jurnal akuntansi sebagai hutang internal
 * yang harus dilunasi.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class PersetujuanDanaTalanganAction extends DanaTalanganAction {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	/**
	 * Membuat instance dalam mode persetujuan dana talangan.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval dengan
	 * meneruskan {@code true} ke konstruktor {@link DanaTalanganAction#DanaTalanganAction(boolean)}.
	 * Flag ini menyesuaikan UI dan logika bisnis untuk alur otorisasi dana talangan,
	 * termasuk menampilkan tombol Setujui/Tolak dan memfilter transaksi yang
	 * masih menunggu otorisasi.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini. Perubahan alur persetujuan
	 * dilakukan di {@link DanaTalanganAction}.</p>
	 */
	public PersetujuanDanaTalanganAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Dana Talangan / Kas Besar" untuk UI.
	 *
	 * <p><b>Tujuan:</b> Memberi konteks halaman kepada approver — mereka sedang
	 * mengotorisasi penggunaan dana talangan dari kas besar. Label gabungan
	 * ("Dana Talangan / Kas Besar") mencerminkan bahwa transaksi ini melibatkan
	 * dua aspek: dana yang ditalangi dan sumber pendanaannya dari kas besar.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; {@code throws Exception}
	 * untuk kompatibilitas kontrak induk.</p>
	 *
	 * @return string "Persetujuan Dana Talangan / Kas Besar"
	 * @throws Exception tidak pernah dilempar
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Dana Talangan / Kas Besar";
	}
}
