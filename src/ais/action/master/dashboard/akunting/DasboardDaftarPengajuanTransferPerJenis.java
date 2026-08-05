package ais.action.master.dashboard.akunting;

/*
 * ENHANCED_DASHBOARD_UIUX_HTML_CSS_2026_06_06
 * Baseline dari file upload terbaru, disusun ulang sesuai package /java/ais/...
 * Catatan: openSession tetap ditutup pada finally; currentSession tidak ditutup manual.
 * Grafik, tren, radar, dan spider web dipertahankan sebagai HTML/CSS agar ringan dan aman di ZK 5.5.
 */


/**
 * Mengelompokkan pengajuan transfer berdasarkan jenis transaksi agar kebutuhan dana yang paling sering muncul cepat terlihat.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardDaftarPengajuanTransferPerJenis extends DasboardDaftarPengajuanTransfer {

	private static final long serialVersionUID = 3557603220165512688L;

	public DasboardDaftarPengajuanTransferPerJenis() {
		super();
	}

	public DasboardDaftarPengajuanTransferPerJenis(String title, String border, boolean closable) {
		super(title, border, closable);
	}

	@Override
	protected boolean isJenisDashboard() {
		return true;
	}

	@Override
	protected String getJudulDashboard() {
		return "Dasbor Jenis Pengajuan Transfer";
	}

	@Override
	protected String getSubJudulDashboard() {
		return "Merangkum pengajuan berdasarkan jenis transaksi agar kelompok pembayaran yang paling membutuhkan perhatian lebih cepat terlihat.";
	}
}
