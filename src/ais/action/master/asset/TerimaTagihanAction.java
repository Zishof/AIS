package ais.action.master.asset;

/**
 * <h3>TerimaTagihanAction — Persetujuan Penerimaan Tagihan Barang / Jasa</h3>
 *
 * <p><b>Untuk apa:</b> Subkelas delegator dari {@link SaldoAwalMasterAssetAction} yang
 * mengaktifkan mode persetujuan penerimaan tagihan barang dan jasa. Dalam alur pengadaan
 * aset, setelah barang/jasa diterima secara fisik (via {@link PenerimaanPengadaanMasterAssetAction}),
 * tagihan dari vendor perlu diverifikasi dan disetujui secara formal oleh pejabat keuangan
 * sebelum pembayaran dapat diproses. Kelas ini menyediakan antarmuka bagi approver untuk
 * meninjau tagihan yang masuk, memverifikasi kesesuaian dengan pesanan dan penerimaan,
 * lalu memberikan persetujuan untuk proses pembayaran selanjutnya.</p>
 *
 * <p><b>Cara kerja:</b> Mewarisi semua logika dari {@link SaldoAwalMasterAssetAction}.
 * Konstruktor memanggil {@code super(true)} untuk mengaktifkan flag persetujuan tagihan.
 * Metode {@link #istilah()} mengembalikan "Persetujuan Terima Tagihan Barang / Jasa".
 * Penggunaan {@link SaldoAwalMasterAssetAction} sebagai induk menunjukkan bahwa tagihan
 * diperlakukan sebagai saldo awal atau hutang kepada vendor dalam pembukuan aset.</p>
 *
 * <p><b>Konteks bisnis:</b> Alur lengkap: Permintaan Pengadaan → Pemesanan → Penerimaan
 * Fisik → Tagihan Vendor → Terima Tagihan (kelas ini — verifikasi dan persetujuan tagihan)
 * → Pembayaran → Posting Jurnal.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Tidak ada state tambahan. Java 1.7, ZKoss 5.5.</p>
 */
public class TerimaTagihanAction extends SaldoAwalMasterAssetAction {
	protected org.zkoss.zul.Tabpanel tabDasborTagihan;

	/**
	 * <b>Tujuan:</b> Memuat dasbor "Terima Tagihan" ke dalam tab pertama.
	 *
	 * <b>Cara kerja:</b> Lazy initialization seperti tab dasbor lain pada modul ini --
	 * komponen hanya dibuat sekali, diperiksa lewat {@code getChildren().size() == 0}.
	 * Dipicu dua kali jalur: {@code onCreate} pada tabpanel (karena tab pertama yang
	 * terpilih tidak pernah menerima onClick) dan {@code onClick} pada tab-nya.
	 *
	 * <b>Sumber angka:</b> {@code PengadaanTahapDashboard} memanggil aksi yang sama
	 * dengan dasbor POS, sehingga angka di ZKoss dan di POS tidak mungkin berbeda.
	 *
	 * <b>Pemeliharaan:</b> pastikan field {@code tabDasborTagihan} terwire di ZUL
	 * lewat {@code id="tabDasborTagihan"} pada elemen {@code <tabpanel>}.
	 *
	 * @param event event ZK pemicu; tidak dipakai langsung
	 */
	public void onDasborTagihan(org.zkoss.zk.ui.event.Event event) {
		if (tabDasborTagihan == null) {
			return;
		}
		if (tabDasborTagihan.getChildren().size() == 0) {
			ais.action.master.asset.helper.PengadaanTahapDashboard dashboard =
					new ais.action.master.asset.helper.PengadaanTahapDashboard("tagihan");
			dashboard.setHeight("100%");
			dashboard.setWidth("100%");
			dashboard.setParent(tabDasborTagihan);
		}
	}


	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = 127321746877617764L;

	/**
	 * Membuat instance dalam mode persetujuan penerimaan tagihan.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi controller dalam mode approval tagihan dengan
	 * meneruskan {@code true} ke konstruktor
	 * {@link SaldoAwalMasterAssetAction#SaldoAwalMasterAssetAction(boolean)}.
	 * Flag ini mengaktifkan UI persetujuan dan filter tampilan khusus tagihan yang
	 * menunggu verifikasi. Approver keuangan membuka halaman ini untuk memeriksa dan
	 * menyetujui tagihan dari vendor sebelum diproses ke pembayaran.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak ada logika di sini. Perubahan alur persetujuan
	 * tagihan dilakukan di {@link SaldoAwalMasterAssetAction}.</p>
	 */
	public TerimaTagihanAction() {
		super(true);
	}

	/**
	 * Mengembalikan label "Persetujuan Terima Tagihan Barang / Jasa" untuk UI.
	 *
	 * <p><b>Tujuan:</b> Memberi konteks halaman kepada approver keuangan — mereka sedang
	 * memverifikasi dan menyetujui tagihan dari vendor untuk barang atau jasa yang sudah
	 * diterima secara fisik. Label ini membedakan halaman ini dari halaman saldo awal aset
	 * biasa ({@link SaldoAwalMasterAssetAction}).</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak melempar exception; {@code throws Exception}
	 * untuk kompatibilitas kontrak induk.</p>
	 *
	 * @return string "Persetujuan Terima Tagihan Barang / Jasa"
	 * @throws Exception tidak pernah dilempar
	 */
	@Override
	public String istilah() throws Exception {
		return "Persetujuan Terima Tagihan Barang / Jasa";
	}

}
