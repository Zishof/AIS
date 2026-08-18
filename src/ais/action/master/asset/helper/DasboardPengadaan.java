package ais.action.master.asset.helper;
import ais.ui.util.DashboardGridExportHelper;

/*
 * ENHANCED_DASHBOARD_UIUX_HTML_CSS_2026_06_06
 * Baseline dari file upload terbaru, disusun ulang sesuai package /java/ais/...
 * Catatan: openSession tetap ditutup pada finally; currentSession tidak ditutup manual.
 * Grafik, tren, radar, dan spider web dipertahankan sebagai HTML/CSS agar ringan dan aman di ZK 5.5.
 */


/* DASBOARD_PENGADAAN_V13_DIRECT_PURCHASE_PR_REALISASI_2026_05_28 */
/*
 * Fix popup detail: Window tidak lagi ditempel langsung ke MyPortallayout.
 * Popup ditempel ke Page/parent container yang valid agar tidak memicu:
 * Unsupported child for MyPortallayout: <Window>.
 *
 * Tambahan dashboard pengadaan/logistik:
 * - Audit Trail & Kelengkapan SOP Dokumen
 * - Throughput Approval & Konversi Pengadaan
 * - Exception Control Logistik & Pengadaan
 *
 * V13 tambahan:
 * - Card dan dashboard Pembelian Langsung dari PemesananPengadaanMasterAsset.pembelianLangsung == true.
 * - Card dan dashboard monitoring realisasi detail PR dari PermintaanPengadaanMasterAssetDetail.
 * - Monitoring realisasi via Uang Muka/Cash Advance, via PO, belum realisasi, dan penerimaan pembelian.
 */

/*
 * ENHANCED UIUX V9 GLOBAL FILTER - PENGADAAN CLICKABLE DETAIL
 * File sengaja dibuat dengan nama berbeda agar tidak replace versi lama.
 * Saat akan dipasang ke project, rename kembali menjadi DasboardPengadaan.java
 * karena public class di dalam file ini tetap: public class DasboardPengadaan.
 *
 * Adaptasi pola dari DasboardSop(2).java:
 * - panel "Dasbor" paling atas sebagai home/overview;
 * - hero/control center dan metric cards;
 * - dashboard analitik tambahan di bawah panel existing;
 * - panel existing di-retouch agar selaras;
 * - public static boolean debug = false untuk melihat error dashboard tambahan.
 * - semua angka KPI utama dibuat clickable dan membuka popup detail paging 10 baris.
 * - V9: filter global tanggal/satker/keyword seperti DasboardSurat_ENHANCED_UIUX_V9_GLOBAL_FILTER.
 * - V9: panel existing reloadPengajuan.onEvent(null) dirender setelah overview dan sebelum funnel analitik.
 * - V10 LOGISTIK: tambahan dashboard analitik logistik & pengadaan di bagian bawah.
 */

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.MoveEvent;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.A;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.asset.PembayaranPengadaanMasterAssetAction;
import ais.action.master.asset.PemesananPengadaanMasterAssetAction;
import ais.action.master.asset.PerjanjianKerjasamaMasterAssetAction;
import ais.action.master.asset.PermintaanPengadaanMasterAssetAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.PembayaranPengadaanMasterAsset;
import ais.database.model.asset.PembayaranPengadaanMasterAssetDetail;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PerjanjianKerjasamaMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Memantau permintaan, pemesanan, penerimaan, dan pembayaran pengadaan agar proses logistik lebih terkendali.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardPengadaan extends MyPortallayout {

	private static final long serialVersionUID = -9006490521125337935L;

	/**
	 * Global debug dashboard.
	 * Set true ketika ingin melihat stacktrace error render/query dashboard pengadaan.
	 * Default false agar production tetap bersih.
	 */
	public static boolean debug = false;

	private static final int DASHBOARD_SAMPLE_LIMIT = 500;
	private static final int DETAIL_PAGE_SIZE = 10;

	/* V9 GLOBAL FILTER + LEGACY RENDERER BRIDGE */
	private interface LegacyDashboardRenderer {
		void render() throws Exception;
	}

	private LegacyDashboardRenderer legacyDashboardRenderer;
	private MyPortallayout dashboardLegacyLayout;
	private Date dashboardGlobalMulaiV9;
	private Date dashboardGlobalSampaiV9;
	private ais.database.model.rab.SatuanKerja dashboardGlobalSatuanKerjaV9;
	private String dashboardGlobalKeywordV9;
	private boolean legacyDashboardRenderedInlineV9;

	public DasboardPengadaan() throws Exception {
		super();
		setWidth("100%");
		setMaximizedMode("whole");
		init();
	} 

	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Pengadaan");
		final EventListener reloadPengajuan = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {

				// 1. Pengajuan Pembelian Barang/Jasa
				buildDashboardPanel(new PanelConfig<PermintaanPengadaanMasterAsset>() {
					@Override
					public String getTitle() {
						return "Pengajuan Pembelian Barang/Jasa";
					}

					@Override
					public String[] getHeaders() {
						return new String[] { "Kode PR", "Wkt Pengajuan", "Unit Pemohon", "Jumlah", "" };
					}

					@Override
					public String[] getWidths() {
						return new String[] { "30%", null, null, null, "10%" };
					}

					@Override
					public String[] getAligns() {
						return new String[] { null, null, null, "right", null };
					}

					@Override
					public Criteria buildCriteria(Session session, String keyword, Object satker, boolean isOrder) {
						Criteria criteria = session.createCriteria(PermintaanPengadaanMasterAsset.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.isNull("disetujuiOleh"));
						applyCommonCriteria(criteria, keyword, satker, isOrder);
						return criteria;
					}

					@Override
					public void renderRow(Row row, final PermintaanPengadaanMasterAsset data) throws Exception {
						renderPermintaanPengadaanBase(row, data);
						row.appendChild(new MyLabelAgakKecil(Common.dateFormat.get().format(data.getTanggalPembuatan())));
						row.appendChild(new MyLabelAgakKecil(
								data.getSatuanKerja() != null ? data.getSatuanKerja().getNama() : ""));
						row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(data.getNilai())));
						renderPermintaanAction(row, data);
					}
				});

				// 2. Persetujuan Pembelian Barang/Jasa
				buildDashboardPanel(new PanelConfig<PermintaanPengadaanMasterAsset>() {
					@Override
					public String getTitle() {
						return "Persetujuan Pembelian Barang/Jasa";
					}

					@Override
					public String[] getHeaders() {
						return new String[] { "Kode PR", "Wkt Pengajuan", "Wkt Persetujuan", "Disetujui oleh", "Jumlah",
								"" };
					}

					@Override
					public String[] getWidths() {
						return new String[] { "30%", null, null, null, null, "10%" };
					}

					@Override
					public String[] getAligns() {
						return new String[] { null, null, null, null, "right", null };
					}

					@Override
					public Criteria buildCriteria(Session session, String keyword, Object satker, boolean isOrder) {
						Criteria criteria = session.createCriteria(PermintaanPengadaanMasterAsset.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.isNotNull("disetujuiOleh"))
								.add(Restrictions.isNotNull("tanggalPersetujuan"));
						applyCommonCriteria(criteria, keyword, satker, isOrder);
						return criteria;
					}

					@Override
					public void renderRow(Row row, final PermintaanPengadaanMasterAsset data) throws Exception {
						renderPermintaanPengadaanBase(row, data);
						row.appendChild(new MyLabelAgakKecil(Common.dateFormat.get().format(data.getTanggalPembuatan())));
						row.appendChild(new MyLabelAgakKecil(data.getTanggalPersetujuan() != null
								? Common.dateFormat.get().format(data.getTanggalPersetujuan())
								: ""));
						row.appendChild(new MyLabelAgakKecil(
								data.getDisetujuiOleh() != null ? data.getDisetujuiOleh().getUserNama() : ""));
						row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(data.getNilai())));
						renderPermintaanAction(row, data);
					}
				});

				// 3. Pengajuan Pemesanan Barang/Jasa (PO)
				buildDashboardPanel(new PanelConfig<PemesananPengadaanMasterAsset>() {
					@Override
					public String getTitle() {
						return "Pengajuan Pemesanan Barang/Jasa";
					}

					@Override
					public String[] getHeaders() {
						return new String[] { "Kode PO", "Wkt Pemesanan", "Unit Pemohon", "Jumlah", "" };
					}

					@Override
					public String[] getWidths() {
						return new String[] { "30%", null, null, null, "10%" };
					}

					@Override
					public String[] getAligns() {
						return new String[] { null, null, null, "right", null };
					}

					@Override
					public Criteria buildCriteria(Session session, String keyword, Object satker, boolean isOrder) {
						Criteria criteria = session.createCriteria(PemesananPengadaanMasterAsset.class)
								.add(Restrictions.isNull("disetujuiOleh"));
						applyCommonCriteria(criteria, keyword, satker, isOrder);
						return criteria;
					}

					@Override
					public void renderRow(Row row, final PemesananPengadaanMasterAsset data) throws Exception {
						renderPemesananPengadaanBase(row, data);
						row.appendChild(new MyLabelAgakKecil(Common.dateFormat.get().format(data.getTanggalPembuatan())));
						row.appendChild(new MyLabelAgakKecil(
								data.getSatuanKerja() != null ? data.getSatuanKerja().getNama() : ""));
						row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(data.getNilai())));
						renderPemesananAction(row, data);
					}
				});

				// 4. Persetujuan Pemesanan Barang/Jasa (PO)
				buildDashboardPanel(new PanelConfig<PemesananPengadaanMasterAsset>() {
					@Override
					public String getTitle() {
						return "Persetujuan Pemesanan Barang/Jasa";
					}

					@Override
					public String[] getHeaders() {
						return new String[] { "Kode PO", "Wkt Pemesanan", "Wkt Persetujuan", "Disetujui oleh", "Jumlah",
								"" };
					}

					@Override
					public String[] getWidths() {
						return new String[] { "30%", null, null, null, null, "10%" };
					}

					@Override
					public String[] getAligns() {
						return new String[] { null, null, null, null, "right", null };
					}

					@Override
					public Criteria buildCriteria(Session session, String keyword, Object satker, boolean isOrder) {
						Criteria criteria = session.createCriteria(PemesananPengadaanMasterAsset.class)
								.add(Restrictions.isNotNull("disetujuiOleh"))
								.add(Restrictions.isNotNull("tanggalPersetujuan"));
						applyCommonCriteria(criteria, keyword, satker, isOrder);
						return criteria;
					}

					@Override
					public void renderRow(Row row, final PemesananPengadaanMasterAsset data) throws Exception {
						renderPemesananPengadaanBase(row, data);
						row.appendChild(new MyLabelAgakKecil(Common.dateFormat.get().format(data.getTanggalPembuatan())));
						row.appendChild(new MyLabelAgakKecil(data.getTanggalPersetujuan() != null
								? Common.dateFormat.get().format(data.getTanggalPersetujuan())
								: ""));
						row.appendChild(new MyLabelAgakKecil(
								data.getDisetujuiOleh() != null ? data.getDisetujuiOleh().getUserNama() : ""));
						row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(data.getNilai())));
						renderPemesananAction(row, data);
					}
				});

				// 5. Pembayaran Pemesanan Barang/Jasa
				buildDashboardPanel(new PanelConfig<PembayaranPengadaanMasterAssetDetail>() {
					@Override
					public String getTitle() {
						return "Pembayaran Pemesanan Barang/Jasa";
					}

					@Override
					public String[] getHeaders() {
						return new String[] { "Kode PO", "Wkt Diterima", "Wkt Bayar", "Jumlah", "Penyedia", "" };
					}

					@Override
					public String[] getWidths() {
						return new String[] { "30%", null, null, null, null, "10%" };
					}

					@Override
					public String[] getAligns() {
						return new String[] { null, null, null, "right", null, null };
					}

					@Override
					public Criteria buildCriteria(Session session, String keyword, Object satker, boolean isOrder) {
						Criteria criteria = session.createCriteria(PembayaranPengadaanMasterAssetDetail.class)
								.createAlias("penerimaanPengadaanMasterAsset", "penerimaanPengadaanMasterAsset")
								.createAlias("pembayaranPengadaanMasterAsset", "pembayaranPengadaanMasterAsset")
								.createAlias("penerimaanPengadaanMasterAsset.pemesananPengadaanMasterAsset",
										"pemesananPengadaanMasterAsset")
								.add(Restrictions.isNotNull("pembayaranPengadaanMasterAsset.disetujuiOleh"))
								.add(Restrictions.isNotNull("pembayaranPengadaanMasterAsset.tanggalPersetujuan"));
						applyGlobalDateRange(criteria, "pembayaranPengadaanMasterAsset.tanggalPersetujuan");
						criteria
								.add(satker == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("pemesananPengadaanMasterAsset.satuanKerja", satker))
								.add(keyword.isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.or(
												Restrictions.ilike("pemesananPengadaanMasterAsset.keterangan", keyword,
														MatchMode.ANYWHERE),
												Restrictions.ilike("pemesananPengadaanMasterAsset.kode", keyword,
														MatchMode.ANYWHERE)));
						if (isOrder)
							criteria.addOrder(Order.desc("id"));
						return criteria;
					}

					@Override
					public void renderRow(Row row, final PembayaranPengadaanMasterAssetDetail data) throws Exception {
						final PembayaranPengadaanMasterAsset bayarAsset = data.getPembayaranPengadaanMasterAsset();
						Vbox a = RevisiHelper.createNewRevisi(PembayaranPengadaanMasterAsset.class, bayarAsset,
								bayarAsset.getKode());
						a.setParent(row);

						Vbox myvbox = new Vbox();
						myvbox.setParent(a);

						Hbox hbox = new Hbox();
						hbox.setParent(myvbox);

						LampiranLain.createDownloadUploadFileLain(hbox, bayarAsset.getId(),
								PembayaranPengadaanMasterAsset.class.getName(), "Bukti Pembayaran", false, null, null,
								false, false, false, false);

						row.appendChild(new MyLabelAgakKecil(
								data.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset().getKode()));
						row.appendChild(
								new MyLabelAgakKecil(
										data.getPenerimaanPengadaanMasterAsset().getTanggalPembuatan() != null
												? Common.dateFormat.get().format(
														data.getPenerimaanPengadaanMasterAsset().getTanggalPembuatan())
												: ""));
						row.appendChild(new MyLabelAgakKecil(bayarAsset.getTanggalPersetujuan() != null
								? Common.dateFormat.get().format(bayarAsset.getTanggalPersetujuan())
								: ""));
						row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(data.getDibayar())));
						row.appendChild(new MyLabelAgakKecil(
								bayarAsset.getPenyedia() != null ? bayarAsset.getPenyedia().getNama() : ""));

						Hbox hboxAction = new Hbox();
						hboxAction.setParent(row);

						MyToolbarbuttonConfig btnPrint = new MyToolbarbuttonConfig("", "/img/svg/printer.svg");
						btnPrint.setTooltiptext("Cetak Data");
						btnPrint.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								PembayaranPengadaanMasterAssetAction.cetak(bayarAsset);
							}
						});
						btnPrint.setParent(hboxAction);

						if (bayarAsset.getDisposisiSop() != null) {
							MyToolbarbuttonConfig btnSop = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
							btnSop.setTooltiptext("Lihat Alur SOP");
							btnSop.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									TampilanAlurSopAction.prosess(bayarAsset.getDisposisiSop().getId(), null, null,
											true, event.getTarget());
								}
							});
							btnSop.setParent(hboxAction);
						}
					}
				});

				// 6. Perjanjian Kerjasama
				buildDashboardPanel(new PanelConfig<PerjanjianKerjasamaMasterAsset>() {
					@Override
					public String getTitle() {
						return "Perjanjian Kerjasama";
					}

					@Override
					public String[] getHeaders() {
						return new String[] { "Kode PKS", "Wkt Pengajuan", "Wkt Persetujuan", "Disetujui oleh",
								"Jumlah", "" };
					}

					@Override
					public String[] getWidths() {
						return new String[] { "30%", null, null, null, null, "10%" };
					}

					@Override
					public String[] getAligns() {
						return new String[] { null, null, null, null, "right", null };
					}

					@Override
					public Criteria buildCriteria(Session session, String keyword, Object satker, boolean isOrder) {
						Criteria criteria = session.createCriteria(PerjanjianKerjasamaMasterAsset.class)
								.add(Restrictions.isNotNull("disetujuiOleh"))
								.add(Restrictions.isNotNull("tanggalPersetujuan"));
						applyCommonCriteria(criteria, keyword, satker, isOrder);
						return criteria;
					}

					@Override
					public void renderRow(Row row, final PerjanjianKerjasamaMasterAsset data) throws Exception {
						Vbox a = RevisiHelper.createNewRevisi(PerjanjianKerjasamaMasterAsset.class, data,
								data.getKode());
						a.setParent(row);

						a.appendChild(new Label(data.getJenisPerjanjianKerjasamaAsset() != null
								? data.getJenisPerjanjianKerjasamaAsset().getNama()
								: ""));
						a.appendChild(new Label(data.getKodeInvoice() != null ? data.getKodeInvoice() : ""));
						a.appendChild(new Label(
								data.getNomorPerjanjianKerjasama() != null ? data.getNomorPerjanjianKerjasama() : ""));

						Hbox hbox = new Hbox();
						hbox.setParent(a);
						LampiranLain.createDownloadUploadFileLain(hbox, data.getId(),
								PerjanjianKerjasamaMasterAsset.class.getName(), "Dokumen", false, null, null, false,
								false, false, false);

						row.appendChild(new MyLabelAgakKecil(Common.dateFormat.get().format(data.getTanggalPembuatan())));
						row.appendChild(new MyLabelAgakKecil(data.getTanggalPersetujuan() != null
								? Common.dateFormat.get().format(data.getTanggalPersetujuan())
								: ""));
						row.appendChild(new MyLabelAgakKecil(
								data.getDisetujuiOleh() != null ? data.getDisetujuiOleh().getUserNama() : ""));
						row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(data.getDp())));

						Hbox hboxAction = new Hbox();
						hboxAction.setParent(row);

						MyToolbarbuttonConfig btnPrint = new MyToolbarbuttonConfig("", "/img/svg/printer.svg");
						btnPrint.setTooltiptext("Cetak Data");
						btnPrint.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								PerjanjianKerjasamaMasterAssetAction.cetak(data);
							}
						});
						btnPrint.setParent(hboxAction);

						if (data.getDisposisiSop() != null) {
							MyToolbarbuttonConfig btnSop = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
							btnSop.setTooltiptext("Lihat Alur SOP");
							btnSop.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									TampilanAlurSopAction.prosess(data.getDisposisiSop().getId(), null, null, true,
											event.getTarget());
								}
							});
							btnSop.setParent(hboxAction);
						}
					}
				});

				// 7. Pembelian Barang/Jasa Ditolak
				buildDashboardPanel(new PanelConfig<PermintaanPengadaanMasterAsset>() {
					@Override
					public String getTitle() {
						return "Pembelian Barang/Jasa Ditolak";
					}

					@Override
					public String[] getHeaders() {
						return new String[] { "Kode PR", "Wkt Pengajuan", "Wkt Ditolak", "Ditolak oleh", "Jumlah", "" };
					}

					@Override
					public String[] getWidths() {
						return new String[] { "30%", null, null, null, null, "10%" };
					}

					@Override
					public String[] getAligns() {
						return new String[] { null, null, null, null, "right", null };
					}

					@Override
					public Criteria buildCriteria(Session session, String keyword, Object satker, boolean isOrder) {
						Criteria criteria = session.createCriteria(PermintaanPengadaanMasterAsset.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.isNotNull("ditolakOleh"))
								.add(Restrictions.isNotNull("tanggalDitolak"));
						applyCommonCriteria(criteria, keyword, satker, isOrder);
						return criteria;
					}

					@Override
					public void renderRow(Row row, final PermintaanPengadaanMasterAsset data) throws Exception {
						renderPermintaanPengadaanBase(row, data);
						row.appendChild(new MyLabelAgakKecil(Common.dateFormat.get().format(data.getTanggalPembuatan())));
						row.appendChild(new MyLabelAgakKecilBoldMerah(
								data.getTanggalDitolak() != null ? Common.dateFormat.get().format(data.getTanggalDitolak())
										: ""));
						row.appendChild(new MyLabelAgakKecilBoldMerah(
								data.getDitolakOleh() != null ? data.getDitolakOleh().getUserNama() : ""));
						row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(data.getNilai())));
						renderPermintaanAction(row, data);
					}
				});

				// 8. Pemesanan Barang/Jasa Ditolak
				buildDashboardPanel(new PanelConfig<PemesananPengadaanMasterAsset>() {
					@Override
					public String getTitle() {
						return "Pemesanan Barang/Jasa Ditolak";
					}

					@Override
					public String[] getHeaders() {
						return new String[] { "Kode PO", "Wkt Pemesanan", "Wkt Ditolak", "Ditolak oleh", "Jumlah", "" };
					}

					@Override
					public String[] getWidths() {
						return new String[] { "30%", null, null, null, null, "10%" };
					}

					@Override
					public String[] getAligns() {
						return new String[] { null, null, null, null, "right", null };
					}

					@Override
					public Criteria buildCriteria(Session session, String keyword, Object satker, boolean isOrder) {
						Criteria criteria = session.createCriteria(PemesananPengadaanMasterAsset.class)
								.add(Restrictions.isNotNull("ditolakOleh"))
								.add(Restrictions.isNotNull("tanggalDitolak"));
						applyCommonCriteria(criteria, keyword, satker, isOrder);
						return criteria;
					}

					@Override
					public void renderRow(Row row, final PemesananPengadaanMasterAsset data) throws Exception {
						renderPemesananPengadaanBase(row, data);
						row.appendChild(new MyLabelAgakKecil(Common.dateFormat.get().format(data.getTanggalPembuatan())));
						row.appendChild(new MyLabelAgakKecilBoldMerah(
								data.getTanggalDitolak() != null ? Common.dateFormat.get().format(data.getTanggalDitolak())
										: ""));
						row.appendChild(new MyLabelAgakKecilBoldMerah(
								data.getDitolakOleh() != null ? data.getDitolakOleh().getUserNama() : ""));
						row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(data.getNilai())));
						renderPemesananAction(row, data);
					}
				});

			}
		};

		legacyDashboardRenderer = new LegacyDashboardRenderer() {
			public void render() throws Exception {
				reloadPengajuan.onEvent(null);
			}
		};

		Calendar calendarDefault = ais.ui.util.WaktuUtil.getCalendar();
		calendarDefault.set(Calendar.YEAR, calendarDefault.get(Calendar.YEAR) - 1);
		renderDasborPengadaanInternal(calendarDefault.getTime(), ais.ui.util.WaktuUtil.getDate());
	}



	public static void setDebug(boolean debugMode) {
		debug = debugMode;
	}

	public static boolean isDebug() {
		return debug;
	}

	private void debugError(String context, Exception e) {
		if (!debug) {
			return;
		}
		try {
			System.err.println("[DasboardPengadaan DEBUG] " + context + " : " + (e == null ? "" : e.getMessage()));
			if (e != null) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/asset/helper/DasboardPengadaan.java:641");
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardPengadaan.java:643");
		}
	}


	// =======================================================================================
	// V9 GLOBAL FILTER + OVERVIEW LAYOUT (mengikuti pola DasboardSurat V9)
	// =======================================================================================
	private void setGlobalFilterPengadaanV9(Date mulai, Date sampai,
			ais.database.model.rab.SatuanKerja satuanKerja, String keyword) {
		dashboardGlobalMulaiV9 = mulai;
		dashboardGlobalSampaiV9 = sampai;
		dashboardGlobalSatuanKerjaV9 = satuanKerja;
		dashboardGlobalKeywordV9 = keyword == null ? "" : keyword.trim();
	}

	private Date getGlobalMulaiV9(Date fallback) {
		return dashboardGlobalMulaiV9 == null ? fallback : dashboardGlobalMulaiV9;
	}

	private Date getGlobalSampaiV9(Date fallback) {
		return dashboardGlobalSampaiV9 == null ? fallback : dashboardGlobalSampaiV9;
	}

	private ais.database.model.rab.SatuanKerja getGlobalSatuanKerjaV9() {
		return dashboardGlobalSatuanKerjaV9;
	}

	private String getGlobalKeywordV9() {
		return dashboardGlobalKeywordV9 == null ? "" : dashboardGlobalKeywordV9;
	}

	private MyPortallayout getCurrentDashboardPortalParent() {
		return dashboardLegacyLayout == null ? DasboardPengadaan.this : dashboardLegacyLayout;
	}

	private void renderDasborPengadaanInternal(final Date mulaiDefault, final Date sampaiDefault) throws Exception {
		MyPortalchildren portalchildren = new MyPortalchildren();
		portalchildren.setParent(DasboardPengadaan.this);
		portalchildren.setWidth("100%");
		portalchildren.setStyle("padding:5px; margin-bottom:12px; box-sizing:border-box;");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(portalchildren);
		panel.setTitle("Dasbor Pengadaan");
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setMinimizable(false);
		panel.setStyle("margin-bottom:14px; border:1px solid #e6edf5; border-radius:16px; "
				+ "background:#ffffff; box-shadow:0 12px 30px rgba(15,23,42,0.08); overflow:hidden;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);
		panelchildren.setStyle("padding:0; background:#f8fafc;");

		/*
		 * V11: Tab tunggal "Dasbor" dihapus karena tidak terpakai.
		 * Konten overview/filter/panel existing/analitik langsung ditempatkan pada Panelchildren
		 * agar UI lebih ringan dan tidak menampilkan tab kosong/tunggal.
		 */
		final Div body = new Div();
		body.setParent(panelchildren);
		body.setWidth("100%");
		body.setStyle("box-sizing:border-box; padding:14px; background:#f8fafc;");

		renderDasborPengadaanContent(body, mulaiDefault, sampaiDefault, null, "");
	}

	private void renderDasborPengadaanContent(final Div body, Date mulai, Date sampai,
			final ais.database.model.rab.SatuanKerja satuanKerja, String keyword) throws Exception {
		Common.clear(body);
		if (mulai == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
			mulai = calendar.getTime();
		}
		if (sampai == null) {
			sampai = ais.ui.util.WaktuUtil.getDate();
		}
		if (keyword == null) {
			keyword = "";
		}

		setGlobalFilterPengadaanV9(mulai, sampai, satuanKerja, keyword);
		legacyDashboardRenderedInlineV9 = false;

		renderDasborPengadaanHeroV9(body, mulai, sampai);
		renderDasborPengadaanFilterV9(body, mulai, sampai, satuanKerja, keyword);

		try {
			PengadaanDashboardData data = loadPengadaanDashboardData();
			renderDasborPengadaanOverviewV9(body, data);

			/*
			 * Sesuai request: reloadPengajuan.onEvent(null) ditampilkan setelah card-card
			 * overview dan sebelum renderFunnelProsesPengadaan(...), melalui bridge legacy ini.
			 */
			renderDashboardOperasionalGlobalV9(body);

			renderAnalitikPengadaanGlobalV9(body, data);
		} catch (Exception e) {
			debugError("renderDasborPengadaanContent", e);
			appendHtml(body,
					"<div style='padding:16px; margin-top:12px; border-radius:14px; background:#fff1f2; color:#991b1b; "
							+ "border:1px solid #fecdd3; font-weight:600;'>Dasbor pengadaan belum dapat dimuat. "
							+ "Aktifkan <b>debug = true</b> untuk melihat detail error di console.</div>");
		}
	}

	private void renderDashboardOperasionalGlobalV9(Div body) {
		if (legacyDashboardRenderer == null) {
			return;
		}
		appendHtml(body, sectionIntroHtmlV9("Dashboard Operasional Global",
				"Panel pengadaan existing di bawah ini sudah memakai filter global dashboard: tanggal mulai, tanggal sampai, satuan kerja, dan kata kunci pencarian. Bagian ini sengaja ditempatkan setelah overview agar user melihat ringkasan terlebih dahulu."));

		MyPortallayout legacyLayout = new MyPortallayout();
		legacyLayout.setParent(body);
		legacyLayout.setWidth("100%");
		legacyLayout.setMaximizedMode("whole");
		legacyLayout.setStyle("margin-top:10px; padding:0; background:transparent;");

		MyPortallayout previousLayout = dashboardLegacyLayout;
		dashboardLegacyLayout = legacyLayout;
		try {
			legacyDashboardRenderer.render();
			legacyDashboardRenderedInlineV9 = true;
		} catch (Exception e) {
			debugError("renderDashboardOperasionalGlobalV9", e);
			appendHtml(body, "<div style='padding:14px; margin-top:10px; border-radius:14px; background:#fff7ed; "
					+ "border:1px solid #fed7aa; color:#9a3412; font-size:12px; font-weight:700;'>"
					+ "Dashboard operasional global belum dapat dimuat. Aktifkan <b>debug = true</b> untuk melihat detail error di console.</div>");
		} finally {
			dashboardLegacyLayout = previousLayout;
		}
	}

	private void renderAnalitikPengadaanGlobalV9(Div body, PengadaanDashboardData data) {
		appendHtml(body, sectionIntroHtmlV9("Dashboard Analitik Pengadaan",
				"Analitik di bawah ini menggunakan filter global yang sama. Angka pada funnel, kartu risiko, health score, coverage SOP, dan daftar prioritas bisa diklik untuk melihat data detail paging 10 baris per halaman."));

		MyPortallayout analyticLayout = new MyPortallayout();
		analyticLayout.setParent(body);
		analyticLayout.setWidth("100%");
		analyticLayout.setMaximizedMode("whole");
		analyticLayout.setStyle("margin-top:10px; padding:0; background:transparent;");

		MyPortallayout previousLayout = dashboardLegacyLayout;
		dashboardLegacyLayout = analyticLayout;
		try {
			renderAnalyticDasborTambahan(data);
		} catch (Exception e) {
			debugError("renderAnalitikPengadaanGlobalV9", e);
			appendHtml(body, "<div style='padding:14px; margin-top:10px; border-radius:14px; background:#fff7ed; "
					+ "border:1px solid #fed7aa; color:#9a3412; font-size:12px; font-weight:700;'>"
					+ "Dashboard analitik belum dapat dimuat. Aktifkan <b>debug = true</b> untuk melihat detail error di console.</div>");
		} finally {
			dashboardLegacyLayout = previousLayout;
		}
	}

	private void renderDasborPengadaanHeroV9(Div parent, Date mulai, Date sampai) {
		String periode = formatDateSafe(mulai) + " s.d. " + formatDateSafe(sampai);
		appendHtml(parent, "<div style='position:relative; overflow:hidden; border-radius:18px; padding:22px; "
				+ "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); "
				+ "color:#ffffff; box-shadow:0 18px 38px rgba(29,78,216,0.22);'>"
				+ "<div style='position:absolute; width:240px; height:240px; right:-70px; top:-90px; border-radius:999px; background:rgba(255,255,255,0.13);'></div>"
				+ "<div style='position:absolute; width:160px; height:160px; right:120px; bottom:-92px; border-radius:999px; background:rgba(255,255,255,0.10);'></div>"
				+ "<div style='position:relative; z-index:2;'>"
				+ "<div style='font-size:12px; letter-spacing:.12em; text-transform:uppercase; opacity:.86; font-weight:700;'>Monitoring Pengadaan & Alur SOP</div>"
				+ "<div style='font-size:28px; line-height:1.18; font-weight:800; margin-top:7px;'>Dasbor Pengadaan Terpadu</div>"
				+ "<div style='font-size:13px; max-width:840px; opacity:.93; margin-top:8px;'>Ringkasan PR, PO, pembayaran, PKS, dokumen ditolak, coverage SOP, dan tindak lanjut pengadaan dalam satu halaman.</div>"
				+ "<div style='margin-top:14px; display:flex; gap:8px; flex-wrap:wrap;'>"
				+ badgeHtmlV9("Periode: " + escapeHtml(periode), "rgba(255,255,255,.16)", "#ffffff")
				+ badgeHtmlV9("Debug: " + (debug ? "AKTIF" : "NONAKTIF"), "rgba(255,255,255,.16)", "#ffffff")
				+ badgeHtmlV9("Detail: klik angka KPI", "rgba(255,255,255,.16)", "#ffffff")
				+ "</div></div></div>");
	}
 
	private void renderDasborPengadaanFilterV9(final Div parent, Date mulai, Date sampai,
			final ais.database.model.rab.SatuanKerja satuanKerja, String keyword) throws Exception {
		Div filterContainer = new Div();
		filterContainer.setParent(parent);
		filterContainer.setStyle("margin-top:14px; padding:14px; background:#ffffff; border:1px solid #e8eef6; "
				+ "border-radius:16px; box-shadow:0 10px 26px rgba(15,23,42,0.04); box-sizing:border-box;");

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(filterContainer);
		toolbar.setStyle("border:0; background:transparent; padding:0; display:flex; flex-wrap:wrap; align-items:center; gap:8px;");

		new MyLabelAgakKecil("Mulai:").setParent(toolbar);
		final MyDatebox dbMulai = new MyDatebox(mulai);
		dbMulai.setReadonly(true);
		dbMulai.setCols(5);
		dbMulai.setParent(toolbar);

		new MyLabelAgakKecil("Sampai:").setParent(toolbar);
		final MyDatebox dbSampai = new MyDatebox(sampai);
		dbSampai.setReadonly(true);
		dbSampai.setCols(5);
		dbSampai.setParent(toolbar);

		new MyLabelAgakKecil("Satker:").setParent(toolbar);
		final AmbilDataSatuanKerjaBanbox cbSatker = new AmbilDataSatuanKerjaBanbox();
		cbSatker.setCols(8);
		cbSatker.setReadonly(true);
		if (satuanKerja != null) {
			cbSatker.setValue(satuanKerja.getNama());
			cbSatker.setAttribute("satuanKerja", satuanKerja);
		}
		cbSatker.setParent(toolbar);

		new MyLabelAgakKecil("Cari:").setParent(toolbar);
		final Textbox txtKeyword = new Textbox();
		txtKeyword.setCols(14);
		txtKeyword.setValue(keyword == null ? "" : keyword);
		txtKeyword.setTooltiptext("Cari kode/keterangan PR, PO, pembayaran, atau PKS");
		txtKeyword.setParent(toolbar);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Tampilkan Dasbor", "/img/svg/search.svg");
		refresh.setTooltiptext("Refresh dasbor berdasarkan filter global");
		refresh.setStyle("font-weight:bold; color:#ffffff; background:#2563eb; border-radius:10px; padding:6px 14px; margin-left:4px;");
		refresh.setParent(toolbar);
		refresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				renderDasborPengadaanContent(parent, dbMulai.getValue(), dbSampai.getValue(),
						(ais.database.model.rab.SatuanKerja) cbSatker.getAttribute("satuanKerja"), txtKeyword.getValue());
			}
		});

		MyToolbarbuttonConfig sinkronisasi = new MyToolbarbuttonConfig("Sinkronisasi", "/img/svg/refresh.svg");
		sinkronisasi.setTooltiptext("Ambil ulang data pengajuan pengadaan terbaru langsung dari database");
		sinkronisasi.setStyle("font-weight:bold; color:#ffffff; background:#059669; border-radius:10px; "
				+ "padding:6px 14px; margin-left:4px;");
		sinkronisasi.setParent(toolbar);
		sinkronisasi.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				// Tutup sesi thread lama agar first-level cache tidak membuat data pengadaan
				// yang baru disimpan terlihat basi. Render berikutnya membuka sesi baru,
				// menjalankan backfill persetujuan SOP, lalu menghitung ulang seluruh KPI.
				try {
					HibernateUtil.closeSession();
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"DasboardPengadaan.sinkronisasi.tutupSessionLama");
				}
				renderDasborPengadaanContent(parent, dbMulai.getValue(), dbSampai.getValue(),
						(ais.database.model.rab.SatuanKerja) cbSatker.getAttribute("satuanKerja"), txtKeyword.getValue());
			}
		});
		txtKeyword.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				renderDasborPengadaanContent(parent, dbMulai.getValue(), dbSampai.getValue(),
						(ais.database.model.rab.SatuanKerja) cbSatker.getAttribute("satuanKerja"), txtKeyword.getValue());
			}
		});
	}

	private void renderDasborPengadaanOverviewV9(Div parent, PengadaanDashboardData d) throws Exception {
		appendHtml(parent, sectionIntroHtmlV9("Overview Seluruh Data Pengadaan",
				"Card-card di bawah ini memakai filter global. Semua angka utama dapat diklik untuk membuka popup detail data dengan paging 10 baris per halaman."));
		renderMetricCards(parent, d);
		renderDefinisiMetrikOverview(parent, d);
	}

	private String sectionIntroHtmlV9(String title, String desc) {
		return "<div style='margin-top:14px; margin-bottom:10px; padding:12px 14px; border-radius:14px; "
				+ "background:#ffffff; border:1px solid #e5e7eb; box-shadow:0 8px 20px rgba(15,23,42,.04);'>"
				+ "<div style='font-size:14px; font-weight:900; color:#0f172a;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:12px; color:#64748b; margin-top:4px; line-height:1.55;'>" + escapeHtml(desc) + "</div></div>";
	}

	private String badgeHtmlV9(String text, String bg, String color) {
		return "<span style='display:inline-block; border-radius:999px; padding:6px 10px; font-size:11px; font-weight:800; background:"
				+ bg + "; color:" + color + "; border:1px solid rgba(255,255,255,.18);'>" + text + "</span>";
	}

	private String formatDateSafe(Date date) {
		try {
			return date == null ? "-" : Common.dateFormat.get().format(date);
		} catch (Exception e) {
			return "-";
		}
	}

	private void applyGlobalDateRange(Criteria criteria, String fieldName) {
		if (criteria == null || fieldName == null || fieldName.trim().isEmpty()) {
			return;
		}
		if (dashboardGlobalMulaiV9 != null) {
			criteria.add(Restrictions.ge(fieldName, dashboardGlobalMulaiV9));
		}
		if (dashboardGlobalSampaiV9 != null) {
			criteria.add(Restrictions.le(fieldName, dashboardGlobalSampaiV9));
		}
	}

	private void applyGlobalSatker(Criteria criteria, String fieldName) {
		if (criteria == null || fieldName == null || fieldName.trim().isEmpty() || dashboardGlobalSatuanKerjaV9 == null) {
			return;
		}
		criteria.add(Restrictions.eq(fieldName, dashboardGlobalSatuanKerjaV9));
	}

	private void applyGlobalKeyword(Criteria criteria, String propA, String propB) {
		if (criteria == null) {
			return;
		}
		String keyword = getGlobalKeywordV9();
		if (keyword == null || keyword.trim().isEmpty()) {
			return;
		}
		if (propA != null && propB != null) {
			criteria.add(Restrictions.or(Restrictions.ilike(propA, keyword, MatchMode.ANYWHERE),
					Restrictions.ilike(propB, keyword, MatchMode.ANYWHERE)));
		} else if (propA != null) {
			criteria.add(Restrictions.ilike(propA, keyword, MatchMode.ANYWHERE));
		}
	}

	private void applyGlobalPengadaanFilter(Criteria criteria, String dateField, String satkerField,
			String keywordFieldA, String keywordFieldB) {
		applyGlobalDateRange(criteria, dateField);
		applyGlobalSatker(criteria, satkerField);
		applyGlobalKeyword(criteria, keywordFieldA, keywordFieldB);
	}

	// =======================================================================================
	// GENERAL DETAIL POPUP: semua angka KPI utama diarahkan ke method ini.
	// =======================================================================================
	private interface DetailCriteriaBuilder {
		Criteria build(Session session, boolean order) throws Exception;
	}

	private interface DetailDataProvider {
		int count(Session session) throws Exception;

		List list(Session session, int firstResult, int maxResults) throws Exception;
	}

	private interface DetailRowRenderer {
		void render(Row row, Object data) throws Exception;
	}

	private A createClickableNumber(String text, String style, final String title, final DetailDataProvider provider,
			final DetailRowRenderer renderer, final String[] headers, final String[] widths) {
		A a = new A();
		a.setLabel(text == null ? "0" : text);
		a.setStyle(style == null ? "font-weight:bold; cursor:pointer; text-decoration:none;" : style);
		a.setTooltiptext("Klik untuk melihat detail " + title);
		a.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				viewDetail(title, headers, widths, provider, renderer);
			}
		});
		return a;
	}


	private void attachPopupWindow(Window win) {
		if (win == null) {
			return;
		}

		try {
			if (DasboardPengadaan.this.getPage() != null) {
				win.setPage(DasboardPengadaan.this.getPage());
				return;
			}
		} catch (Exception e) {
			debugError("attachPopupWindow-setPage", e);
		}

		try {
			Component parent = DasboardPengadaan.this.getParent();
			while (parent != null && parent instanceof MyPortallayout) {
				parent = parent.getParent();
			}
			if (parent != null) {
				win.setParent(parent);
				return;
			}
		} catch (Exception e) {
			debugError("attachPopupWindow-setParent", e);
		}

		throw new IllegalStateException("Popup detail tidak dapat ditempel karena Page/Parent belum tersedia.");
	}

	private void viewDetail(final String title, final String[] headers, final String[] widths,
			final DetailDataProvider provider, final DetailRowRenderer renderer) {
		try {
			final Window win = new Window();
			win.setTitle(title);
			win.setWidth(Common.isMobile() ? "98%" : "92%");
			win.setHeight("86%");
			win.setClosable(true);
			win.setSizable(true);
			win.setPosition("center");
			win.setBorder("normal");
			attachPopupWindow(win);

			Div shell = new Div();
			shell.setStyle("padding:12px; background:#f8fafc; height:100%; box-sizing:border-box; overflow:auto;");
			shell.setParent(win);

			Div headerBox = new Div();
			headerBox.setStyle("display:flex; justify-content:space-between; align-items:center; gap:12px; flex-wrap:wrap;"
					+ "padding:12px 14px; border-radius:16px; color:#ffffff; background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);"
					+ "box-shadow:0 10px 24px rgba(37,99,235,.18); margin-bottom:10px;");
			headerBox.setParent(shell);
			appendHtml(headerBox, "<div><div style='font-size:12px; letter-spacing:.08em; text-transform:uppercase; opacity:.82;'>Detail Data</div>"
					+ "<div style='font-size:20px; font-weight:900; margin-top:3px;'>" + escapeHtml(title) + "</div>"
					+ "<div style='font-size:11px; opacity:.85; margin-top:3px;'>Paging otomatis 10 data per halaman.</div></div>");
			final Label info = new Label(ais.common.Common.getBahasaConfig("Memuat data..."));
			info.setStyle("font-size:12px; font-weight:bold; color:#ffffff; background:rgba(255,255,255,.16); border-radius:999px; padding:6px 10px;");
			info.setParent(headerBox);

			final Grid grid = new Grid();
			grid.setSclass("dgrid fgrid table-striped");
			grid.setWidth("100%");
			grid.setStyle("border:1px solid #e5e7eb; border-radius:14px; overflow:hidden; background:#ffffff;");
			grid.setParent(shell);

			Columns columns = new Columns();
			columns.setParent(grid);
			for (int i = 0; i < headers.length; i++) {
				Column column = new MyColumnConfig(headers[i]);
				if (widths != null && i < widths.length && widths[i] != null) {
					column.setWidth(widths[i]);
				}
				columns.appendChild(column);
			}

			final Rows rows = new Rows();
			rows.setParent(grid);

			final Paging paging = new Paging();
			paging.setPageSize(DETAIL_PAGE_SIZE);
			paging.setDetailed(true);
			paging.setStyle("margin-top:10px;");
			paging.setParent(shell);

			final EventListener reloadDetail = new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {
					Session session = null;
					try {
						session = HibernateUtil.currentNativeSession();
						rows.getChildren().clear();
						int total = provider.count(session);
						paging.setTotalSize(total);
						info.setValue(Common.numberFormat.get().format(total) + " data");
						int first = paging.getActivePage() * DETAIL_PAGE_SIZE;
						List list = provider.list(session, first, DETAIL_PAGE_SIZE);
						if (list == null || list.isEmpty()) {
							Row empty = new Row();
							empty.setParent(rows);
							empty.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak ada data")));
							for (int i = 1; i < headers.length; i++) {
								empty.appendChild(new Label("-"));
							}
							return;
						}
						for (Object data : list) {
							Row row = new Row();
							row.setParent(rows);
							renderer.render(row, data);
						}
					} catch (Exception e) {
						debugError("viewDetail " + title, e);
						rows.getChildren().clear();
						Row err = new Row();
						err.setParent(rows);
						err.appendChild(new Label(debug ? ("Error: " + e.getMessage()) : "Terjadi error saat memuat detail. Aktifkan debug untuk melihat stacktrace."));
						for (int i = 1; i < headers.length; i++) {
							err.appendChild(new Label("-"));
						}
					} finally {
						closeSessionSafely(session);
					}
				}
			};

			paging.addEventListener("onPaging", reloadDetail);
			reloadDetail.onEvent(null);
			win.doModal();
		} catch (Exception e) {
			debugError("viewDetail popup " + title, e);
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private DetailDataProvider detailProviderCriteria(final DetailCriteriaBuilder builder) {
		return new DetailDataProvider() {
			@Override
			public int count(Session session) throws Exception {
				Criteria c = builder.build(session, false);
				Object result = c.setProjection(Projections.rowCount()).uniqueResult();
				return (int) toLong(result);
			}

			@SuppressWarnings("unchecked")
			@Override
			public List list(Session session, int firstResult, int maxResults) throws Exception {
				Criteria c = builder.build(session, true);
				return c.setFirstResult(firstResult).setMaxResults(maxResults).list();
			}
		};
	}

	private DetailDataProvider detailProviderStaticList(final List source) {
		return new DetailDataProvider() {
			@Override
			public int count(Session session) throws Exception {
				return source == null ? 0 : source.size();
			}

			@Override
			public List list(Session session, int firstResult, int maxResults) throws Exception {
				List result = new ArrayList();
				if (source == null) {
					return result;
				}
				int end = Math.min(firstResult + maxResults, source.size());
				for (int i = firstResult; i < end; i++) {
					result.add(source.get(i));
				}
				return result;
			}
		};
	}

	private DetailDataProvider detailProviderRingkasanDokumen() {
		return new DetailDataProvider() {
			@Override
			public int count(Session session) throws Exception {
				return 4;
			}

			@Override
			public List list(Session session, int firstResult, int maxResults) throws Exception {
				List rows = new ArrayList();
				rows.add(new SummaryDetailItem("PR / Permintaan", countCriteria(criteriaPermintaanAktif(session)), "Total dokumen permintaan pengadaan aktif"));
				rows.add(new SummaryDetailItem("PO / Pemesanan", countCriteria(criteriaPemesanan(session)), "Total dokumen pemesanan pengadaan"));
				rows.add(new SummaryDetailItem("Pembayaran", countCriteria(criteriaPembayaranMaster(session)), "Total dokumen pembayaran pengadaan"));
				rows.add(new SummaryDetailItem("PKS", countCriteria(criteriaPks(session)), "Total dokumen perjanjian kerjasama"));
				return sliceList(rows, firstResult, maxResults);
			}
		};
	}

	private DetailDataProvider detailProviderCoverageSop() {
		return new DetailDataProvider() {
			@Override
			public int count(Session session) throws Exception {
				return 4;
			}

			@Override
			public List list(Session session, int firstResult, int maxResults) throws Exception {
				List rows = new ArrayList();
				long totalPr = countCriteria(criteriaPermintaanAktif(session));
				long prSop = countCriteria(criteriaPermintaanAktif(session).add(Restrictions.isNotNull("disposisiSop")));
				long totalPo = countCriteria(criteriaPemesanan(session));
				long poSop = countCriteria(criteriaPemesanan(session).add(Restrictions.isNotNull("disposisiSop")));
				long totalBayar = countCriteria(criteriaPembayaranMaster(session));
				long bayarSop = countCriteria(criteriaPembayaranMaster(session).add(Restrictions.isNotNull("disposisiSop")));
				long totalPks = countCriteria(criteriaPks(session));
				long pksSop = countCriteria(criteriaPks(session).add(Restrictions.isNotNull("disposisiSop")));
				rows.add(new SummaryDetailItem("PR / Permintaan", prSop + " / " + totalPr, percent((int) prSop, (int) totalPr) + "% memiliki SOP"));
				rows.add(new SummaryDetailItem("PO / Pemesanan", poSop + " / " + totalPo, percent((int) poSop, (int) totalPo) + "% memiliki SOP"));
				rows.add(new SummaryDetailItem("Pembayaran", bayarSop + " / " + totalBayar, percent((int) bayarSop, (int) totalBayar) + "% memiliki SOP"));
				rows.add(new SummaryDetailItem("PKS", pksSop + " / " + totalPks, percent((int) pksSop, (int) totalPks) + "% memiliki SOP"));
				return sliceList(rows, firstResult, maxResults);
			}
		};
	}

	private DetailDataProvider detailProviderAntrianPrPo() {
		return new DetailDataProvider() {
			@Override
			public int count(Session session) throws Exception {
				return (int) (countCriteria(criteriaPrPending(session)) + countCriteria(criteriaPoPending(session)));
			}

			@SuppressWarnings("unchecked")
			@Override
			public List list(Session session, int firstResult, int maxResults) throws Exception {
				List result = new ArrayList();
				int prCount = (int) countCriteria(criteriaPrPending(session));
				if (firstResult < prCount && result.size() < maxResults) {
					List<PermintaanPengadaanMasterAsset> prs = criteriaPrPending(session).addOrder(Order.desc("id"))
							.setFirstResult(firstResult).setMaxResults(maxResults).list();
					for (PermintaanPengadaanMasterAsset pr : prs) {
						result.add(toQueueItem("PR", pr));
					}
				}
				if (result.size() < maxResults) {
					int poFirst = firstResult < prCount ? 0 : firstResult - prCount;
					List<PemesananPengadaanMasterAsset> pos = criteriaPoPending(session).addOrder(Order.desc("id"))
							.setFirstResult(poFirst).setMaxResults(maxResults - result.size()).list();
					for (PemesananPengadaanMasterAsset po : pos) {
						result.add(toQueueItem("PO", po));
					}
				}
				return result;
			}
		};
	}

	private List sliceList(List source, int firstResult, int maxResults) {
		List result = new ArrayList();
		if (source == null) {
			return result;
		}
		int end = Math.min(firstResult + maxResults, source.size());
		for (int i = firstResult; i < end; i++) {
			result.add(source.get(i));
		}
		return result;
	}

	private ProcurementItem toQueueItem(String jenis, PermintaanPengadaanMasterAsset row) {
		ProcurementItem item = new ProcurementItem();
		item.jenis = jenis;
		item.kode = safeString(row.getKode());
		item.tanggal = safeDate(row.getTanggalPembuatan());
		item.pihak = row.getSatuanKerja() == null ? "-" : safeString(row.getSatuanKerja().getNama());
		item.nilai = toNumber(row.getNilai());
		item.keterangan = safeString(row.getKeterangan());
		item.status = "Menunggu Persetujuan";
		return item;
	}

	private ProcurementItem toQueueItem(String jenis, PemesananPengadaanMasterAsset row) {
		ProcurementItem item = new ProcurementItem();
		item.jenis = jenis;
		item.kode = safeString(row.getKode());
		item.tanggal = safeDate(row.getTanggalPembuatan());
		item.pihak = row.getPenyedia() == null ? "-" : safeString(row.getPenyedia().getNama());
		item.nilai = toNumber(row.getNilai());
		item.keterangan = safeString(row.getKodeInvoice());
		item.status = "Menunggu Persetujuan";
		return item;
	}

	private String[] detailHeadersPr() {
		return new String[] { "Kode PR", "Tanggal", "Unit Pemohon", "Nilai", "Status", "User", "Keterangan" };
	}

	private String[] detailWidthsPr() {
		return new String[] { "15%", "12%", "18%", "12%", "13%", "15%", null };
	}

	private String[] detailHeadersPo() {
		return new String[] { "Kode PO", "Tanggal", "Penyedia", "Nilai", "Status", "User", "Invoice / Keterangan" };
	}

	private String[] detailWidthsPo() {
		return new String[] { "15%", "12%", "18%", "12%", "13%", "15%", null };
	}

	private String[] detailHeadersPembayaran() {
		return new String[] { "Kode Bayar", "Kode PO", "Tgl Terima", "Tgl Bayar", "Penyedia", "Dibayar" };
	}

	private String[] detailWidthsPembayaran() {
		return new String[] { "16%", "16%", "13%", "13%", null, "15%" };
	}

	private String[] detailHeadersPks() {
		return new String[] { "Kode PKS", "Tanggal", "Persetujuan", "Penyedia/Jenis", "Nomor PKS", "DP" };
	}

	private String[] detailWidthsPks() {
		return new String[] { "15%", "12%", "12%", null, "18%", "12%" };
	}

	private String[] detailHeadersQueue() {
		return new String[] { "Jenis", "Kode", "Tanggal", "Pihak", "Nilai", "Status", "Keterangan" };
	}

	private String[] detailWidthsQueue() {
		return new String[] { "8%", "14%", "12%", "20%", "12%", "14%", null };
	}

	private String[] detailHeadersSummary() {
		return new String[] { "Kategori", "Jumlah / Nilai", "Keterangan" };
	}

	private String[] detailHeadersPermintaanDetail() {
		return new String[] { "Kode PR", "Tanggal", "Barang/Jasa", "Jumlah", "Harga", "Total", "Realisasi", "Unit", "Keterangan" };
	}

	private String[] detailWidthsPermintaanDetail() {
		return new String[] { "12%", "11%", null, "8%", "10%", "10%", "15%", "14%", null };
	}

	private String[] detailHeadersPenerimaan() {
		return new String[] { "Kode Penerimaan", "Tanggal", "Kode PO", "Penyedia", "Nilai", "Status", "Tagihan" };
	}

	private String[] detailWidthsPenerimaan() {
		return new String[] { "15%", "12%", "15%", null, "12%", "13%", "12%" };
	}

	private DetailRowRenderer rendererPrDetail() {
		return new DetailRowRenderer() {
			@Override
			public void render(Row row, Object data) throws Exception {
				PermintaanPengadaanMasterAsset pr = (PermintaanPengadaanMasterAsset) data;
				row.appendChild(new Label(safeString(pr.getKode())));
				row.appendChild(new Label(safeDate(pr.getTanggalPembuatan())));
				row.appendChild(new Label(pr.getSatuanKerja() == null ? "-" : safeString(pr.getSatuanKerja().getNama())));
				row.appendChild(new Label(formatNumber(toNumber(pr.getNilai()))));
				row.appendChild(new Label(getStatusPermintaan(pr)));
				row.appendChild(new Label(getUserProsesPermintaan(pr)));
				row.appendChild(new Label(safeString(pr.getKeterangan())));
			}
		};
	}

	private DetailRowRenderer rendererPoDetail() {
		return new DetailRowRenderer() {
			@Override
			public void render(Row row, Object data) throws Exception {
				PemesananPengadaanMasterAsset po = (PemesananPengadaanMasterAsset) data;
				row.appendChild(new Label(safeString(po.getKode())));
				row.appendChild(new Label(safeDate(po.getTanggalPembuatan())));
				row.appendChild(new Label(po.getPenyedia() == null ? "-" : safeString(po.getPenyedia().getNama())));
				row.appendChild(new Label(formatNumber(toNumber(po.getNilai()))));
				row.appendChild(new Label(getStatusPemesanan(po)));
				row.appendChild(new Label(getUserProsesPemesanan(po)));
				row.appendChild(new Label(safeString(po.getKodeInvoice())));
			}
		};
	}

	private DetailRowRenderer rendererPembayaranDetail() {
		return new DetailRowRenderer() {
			@Override
			public void render(Row row, Object data) throws Exception {
				PembayaranPengadaanMasterAssetDetail detail = (PembayaranPengadaanMasterAssetDetail) data;
				PembayaranPengadaanMasterAsset bayar = detail.getPembayaranPengadaanMasterAsset();
				row.appendChild(new Label(bayar == null ? "-" : safeString(bayar.getKode())));
				try {
					row.appendChild(new Label(detail.getPenerimaanPengadaanMasterAsset() != null
							&& detail.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset() != null
									? safeString(detail.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset().getKode()) : "-"));
				} catch (Exception e) {
					row.appendChild(new Label("-"));
				}
				row.appendChild(new Label(detail.getPenerimaanPengadaanMasterAsset() == null ? "-" : safeDate(detail.getPenerimaanPengadaanMasterAsset().getTanggalPembuatan())));
				row.appendChild(new Label(bayar == null ? "-" : safeDate(bayar.getTanggalPersetujuan())));
				row.appendChild(new Label(bayar == null || bayar.getPenyedia() == null ? "-" : safeString(bayar.getPenyedia().getNama())));
				row.appendChild(new Label(formatNumber(toNumber(detail.getDibayar()))));
			}
		};
	}

	private DetailRowRenderer rendererPksDetail() {
		return new DetailRowRenderer() {
			@Override
			public void render(Row row, Object data) throws Exception {
				PerjanjianKerjasamaMasterAsset pks = (PerjanjianKerjasamaMasterAsset) data;
				row.appendChild(new Label(safeString(pks.getKode())));
				row.appendChild(new Label(safeDate(pks.getTanggalPembuatan())));
				row.appendChild(new Label(safeDate(pks.getTanggalPersetujuan())));
				row.appendChild(new Label(pks.getJenisPerjanjianKerjasamaAsset() == null ? "-" : safeString(pks.getJenisPerjanjianKerjasamaAsset().getNama())));
				row.appendChild(new Label(safeString(pks.getNomorPerjanjianKerjasama())));
				row.appendChild(new Label(formatNumber(toNumber(pks.getDp()))));
			}
		};
	}

	private DetailRowRenderer rendererProcurementQueue() {
		return new DetailRowRenderer() {
			@Override
			public void render(Row row, Object data) throws Exception {
				ProcurementItem item = (ProcurementItem) data;
				row.appendChild(new Label(safeString(item.jenis)));
				row.appendChild(new Label(safeString(item.kode)));
				row.appendChild(new Label(safeString(item.tanggal)));
				row.appendChild(new Label(safeString(item.pihak)));
				row.appendChild(new Label(formatNumber(item.nilai)));
				row.appendChild(new Label(safeString(item.status)));
				row.appendChild(new Label(safeString(item.keterangan)));
			}
		};
	}

	private DetailRowRenderer rendererSummaryDetail() {
		return new DetailRowRenderer() {
			@Override
			public void render(Row row, Object data) throws Exception {
				SummaryDetailItem item = (SummaryDetailItem) data;
				row.appendChild(new Label(safeString(item.label)));
				row.appendChild(new Label(safeString(item.value)));
				row.appendChild(new Label(safeString(item.description)));
			}
		};
	}

	private DetailRowRenderer rendererPermintaanDetail() {
		return new DetailRowRenderer() {
			@Override
			public void render(Row row, Object data) throws Exception {
				PermintaanPengadaanMasterAssetDetail detail = (PermintaanPengadaanMasterAssetDetail) data;
				PermintaanPengadaanMasterAsset pr = detail.getPermintaanPengadaanMasterAsset();
				row.appendChild(new Label(pr == null ? "-" : safeString(pr.getKode())));
				row.appendChild(new Label(safeDate(detail.getTanggalPembuatan())));
				row.appendChild(new Label(detail.getMasterAsset() == null ? "-" : safeString(detail.getMasterAsset().getNama())));
				row.appendChild(new Label(formatNumber(toNumber(detail.getJumlah()))));
				row.appendChild(new Label(formatNumber(toNumber(detail.getHargaBeli()))));
				row.appendChild(new Label(formatNumber(toNumber(detail.getHargaTotal()))));
				row.appendChild(new Label(getStatusRealisasiPermintaanDetail(detail)));
				row.appendChild(new Label(detail.getSatuanKerja() == null ? "-" : safeString(detail.getSatuanKerja().getNama())));
				row.appendChild(new Label(safeString(detail.getKeterangan())));
			}
		};
	}

	private DetailRowRenderer rendererPenerimaanDetail() {
		return new DetailRowRenderer() {
			@Override
			public void render(Row row, Object data) throws Exception {
				PenerimaanPengadaanMasterAsset penerimaan = (PenerimaanPengadaanMasterAsset) data;
				PemesananPengadaanMasterAsset po = penerimaan.getPemesananPengadaanMasterAsset();
				row.appendChild(new Label(safeString(penerimaan.getKode())));
				row.appendChild(new Label(safeDate(penerimaan.getTanggalPembuatan())));
				row.appendChild(new Label(po == null ? "-" : safeString(po.getKode())));
				row.appendChild(new Label(penerimaan.getPenyedia() == null ? "-" : safeString(penerimaan.getPenyedia().getNama())));
				row.appendChild(new Label(formatNumber(toNumber(penerimaan.getNilai()))));
				row.appendChild(new Label(getStatusPenerimaan(penerimaan)));
				row.appendChild(new Label(safeString(penerimaan.getKodeTagihan())));
			}
		};
	}

	private String getStatusPermintaan(PermintaanPengadaanMasterAsset pr) {
		if (pr.getDitolakOleh() != null || pr.getTanggalDitolak() != null) {
			return "Ditolak";
		}
		if (pr.getDisetujuiOleh() != null || pr.getTanggalPersetujuan() != null) {
			return "Disetujui";
		}
		return "Pending";
	}

	private String getUserProsesPermintaan(PermintaanPengadaanMasterAsset pr) {
		try {
			if (pr.getDitolakOleh() != null) {
				return safeString(pr.getDitolakOleh().getUserNama());
			}
			if (pr.getDisetujuiOleh() != null) {
				return safeString(pr.getDisetujuiOleh().getUserNama());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardPengadaan.java:1486");
		}
		return "-";
	}

	private String getStatusPemesanan(PemesananPengadaanMasterAsset po) {
		if (po.getDitolakOleh() != null || po.getTanggalDitolak() != null) {
			return "Ditolak";
		}
		if (po.getDisetujuiOleh() != null || po.getTanggalPersetujuan() != null) {
			return "Disetujui";
		}
		return "Pending";
	}

	private String getUserProsesPemesanan(PemesananPengadaanMasterAsset po) {
		try {
			if (po.getDitolakOleh() != null) {
				return safeString(po.getDitolakOleh().getUserNama());
			}
			if (po.getDisetujuiOleh() != null) {
				return safeString(po.getDisetujuiOleh().getUserNama());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardPengadaan.java:1509");
		}
		return "-";
	}

	private String getStatusRealisasiPermintaanDetail(PermintaanPengadaanMasterAssetDetail detail) {
		if (detail == null) {
			return "-";
		}
		try {
			if (detail.getUangMuka() != null) {
				return "Uang Muka / Cash Advance";
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardPengadaan.java:1522");
		}
		try {
			if (detail.getPemesananPengadaanMasterAssetDetail() != null) {
				return "Pemesanan Pembelian / PO";
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardPengadaan.java:1528");
		}
		try {
			if (detail.getPerjanjianKerjasamaMasterAssetDetail() != null || detail.getPerjanjianKerjasamaMasterAsset() != null) {
				return "PKS / Kontrak";
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardPengadaan.java:1534");
		}
		try {
			if (detail.getAsset() != null) {
				return "Sudah Menjadi Aset";
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardPengadaan.java:1540");
		}
		return "Belum Realisasi";
	}

	private String getStatusPenerimaan(PenerimaanPengadaanMasterAsset penerimaan) {
		if (penerimaan == null) {
			return "-";
		}
		try {
			if (penerimaan.getDisetujuiOleh() != null || penerimaan.getTanggalPersetujuan() != null) {
				return "Disetujui";
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardPengadaan.java:1553");
		}
		return "Pending";
	}

	private PengadaanDashboardData renderInternalDasborOverview() throws Exception {
		final PengadaanDashboardData dashboardData = loadPengadaanDashboardData();

		MyPortalchildren portalchildren = new MyPortalchildren();
		portalchildren.setParent(getCurrentDashboardPortalParent());
		portalchildren.setWidth("100%");
		portalchildren.setStyle("padding:6px; box-sizing:border-box;");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setTitle("Dasbor");
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setMinimizable(false);
		panel.setStyle("margin-bottom:12px; border:1px solid #dbeafe; border-radius:18px; overflow:hidden;"
				+ "background:#ffffff; box-shadow:0 14px 28px rgba(15,23,42,.08);");
		panel.setParent(portalchildren);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setStyle("padding:0; background:#f6f8fb;");
		panelchildren.setParent(panel);

		renderHomeDasborContent(panelchildren, dashboardData);
		return dashboardData;
	}

	private void renderHomeDasborContent(Component parent, final PengadaanDashboardData d) throws Exception {
		if (parent == null) {
			return;
		}

		org.zkoss.zul.Div shell = new org.zkoss.zul.Div();
		shell.setWidth("100%");
		shell.setStyle("background:#f6f8fb; padding:14px; box-sizing:border-box; overflow:auto;");
		shell.setParent(parent);

		renderHeroDasbor(shell, d);
		renderMetricCards(shell, d);
		renderDefinisiMetrikOverview(shell, d);
	}

	private void renderHeroDasbor(Component parent, final PengadaanDashboardData d) {
		String subtitle = "Pantau pipeline PR, PO, pembayaran, PKS, coverage SOP, dan risiko bottleneck pengadaan dalam satu layar.";
		Div hero = new Div();
		hero.setStyle("position:relative; overflow:hidden; border-radius:18px; padding:22px 24px; color:#ffffff;"
				+ "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); box-shadow:0 14px 30px rgba(15,23,42,.18);");
		hero.setParent(parent);

		appendHtml(hero, "<div style='position:absolute; right:-60px; top:-70px; width:210px; height:210px; border-radius:999px; background:rgba(255,255,255,.12);'></div>"
				+ "<div style='position:absolute; right:90px; bottom:-70px; width:160px; height:160px; border-radius:999px; background:rgba(255,255,255,.10);'></div>"
				+ "<div style='position:relative; z-index:1; display:flex; justify-content:space-between; gap:16px; flex-wrap:wrap; align-items:center;'>"
				+ "<div><div style='font-size:12px; text-transform:uppercase; letter-spacing:.12em; opacity:.88;'>Procurement Control Center</div>"
				+ "<div style='font-size:28px; line-height:1.15; font-weight:800; margin-top:6px;'>Dasbor Pengadaan & SOP</div>"
				+ "<div style='font-size:13px; opacity:.90; margin-top:8px; max-width:760px;'>" + subtitle + "</div></div></div>");

		Div badgeBox = new Div();
		badgeBox.setStyle("position:relative; z-index:2; display:flex; gap:10px; flex-wrap:wrap; margin-top:16px;");
		badgeBox.setParent(hero);
		appendHeroBadge(badgeBox, "Dokumen Dipantau", String.valueOf(d.totalDokumen), detailProviderRingkasanDokumen(), rendererSummaryDetail(), detailHeadersSummary(), null, "Detail Dokumen Dipantau");
		appendHeroBadge(badgeBox, "Antrian PR/PO", String.valueOf(d.totalPending), detailProviderAntrianPrPo(), rendererProcurementQueue(), detailHeadersQueue(), detailWidthsQueue(), "Detail Antrian PR/PO");
		appendHeroBadge(badgeBox, "Coverage SOP", d.coverageSopPercent + "%", detailProviderCoverageSop(), rendererSummaryDetail(), detailHeadersSummary(), null, "Detail Coverage SOP Dokumen");
	}

	private void appendHeroBadge(Component parent, String label, String value, final DetailDataProvider provider,
			final DetailRowRenderer renderer, final String[] headers, final String[] widths, final String title) {
		Div card = new Div();
		card.setStyle("background:rgba(255,255,255,.16); border:1px solid rgba(255,255,255,.24); padding:12px 16px; border-radius:14px; min-width:120px; text-align:center;");
		card.setParent(parent);
		A a = createClickableNumber(value, "font-size:24px; font-weight:800; color:#ffffff; text-decoration:none; cursor:pointer; display:block;", title, provider, renderer, headers, widths);
		a.setParent(card);
		appendHtml(card, "<div style='font-size:11px; opacity:.85;'>" + escapeHtml(label) + "</div>");
	}

	private String buildHeroBadge(String label, int value) {
		return buildHeroBadge(label, String.valueOf(value));
	}

	private String buildHeroBadge(String label, String value) {
		return "<div style='background:rgba(255,255,255,.16); border:1px solid rgba(255,255,255,.24); padding:12px 16px; border-radius:14px; min-width:120px; text-align:center;'>"
				+ "<div style='font-size:24px; font-weight:800;'>" + escapeHtml(value) + "</div><div style='font-size:11px; opacity:.85;'>" + escapeHtml(label) + "</div></div>";
	}

	private void renderMetricCards(Component parent, PengadaanDashboardData d) {
		Div wrapper = new Div();
		wrapper.setStyle("display:flex; gap:12px; flex-wrap:wrap; margin-top:12px;");
		wrapper.setParent(parent);
		appendMetricCard(wrapper, "PR Pending", d.prPending, "Nilai " + formatNumber(d.nilaiPrPending), "#fee2e2", "#991b1b", "PR", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPrPending(session); if (order) c.addOrder(Order.desc("id")); return c; } }), rendererPrDetail(), detailHeadersPr(), detailWidthsPr(), "Detail PR Pending");
		appendMetricCard(wrapper, "PO Pending", d.poPending, "Nilai " + formatNumber(d.nilaiPoPending), "#ffedd5", "#9a3412", "PO", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPoPending(session); if (order) c.addOrder(Order.desc("id")); return c; } }), rendererPoDetail(), detailHeadersPo(), detailWidthsPo(), "Detail PO Pending");
		appendMetricCard(wrapper, "PR Disetujui", d.prApproved, "Siap lanjut pemesanan", "#dcfce7", "#166534", "OK", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPrApproved(session); if (order) c.addOrder(Order.desc("tanggalPersetujuan")); return c; } }), rendererPrDetail(), detailHeadersPr(), detailWidthsPr(), "Detail PR Disetujui");
		appendMetricCard(wrapper, "PO Disetujui", d.poApproved, "Siap penerimaan/pembayaran", "#dbeafe", "#1e40af", "✓", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPoApproved(session); if (order) c.addOrder(Order.desc("tanggalPersetujuan")); return c; } }), rendererPoDetail(), detailHeadersPo(), detailWidthsPo(), "Detail PO Disetujui");
		appendMetricCard(wrapper, "Pembayaran", d.paymentApproved, "Nilai " + formatNumber(d.nilaiPembayaranApproved), "#ede9fe", "#5b21b6", "Rp", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPembayaranDetailDisetujui(session); if (order) c.addOrder(Order.desc("id")); return c; } }), rendererPembayaranDetail(), detailHeadersPembayaran(), detailWidthsPembayaran(), "Detail Pembayaran Disetujui");
		appendMetricCard(wrapper, "PKS", d.pksApproved, "Kontrak disetujui", "#cffafe", "#155e75", "PK", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPksDisetujui(session); if (order) c.addOrder(Order.desc("tanggalPersetujuan")); return c; } }), rendererPksDetail(), detailHeadersPks(), detailWidthsPks(), "Detail PKS Disetujui");
		appendMetricCard(wrapper, "Pembelian Langsung", d.pembelianLangsung, "Nilai " + formatNumber(d.nilaiPembelianLangsung), "#fef3c7", "#92400e", "PL", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPembelianLangsung(session); if (order) c.addOrder(Order.desc("tanggalPembuatan")); return c; } }), rendererPoDetail(), detailHeadersPo(), detailWidthsPo(), "Detail Pembelian Langsung");
		appendMetricCard(wrapper, "Penerimaan Pembelian", d.penerimaanPembelian, "Barang/jasa diterima", "#ecfeff", "#155e75", "TR", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPenerimaan(session); if (order) c.addOrder(Order.desc("tanggalPembuatan")); return c; } }), rendererPenerimaanDetail(), detailHeadersPenerimaan(), detailWidthsPenerimaan(), "Detail Penerimaan Pembelian");
		appendMetricCard(wrapper, "Detail PR Terealisasi", d.permintaanDetailRealisasi, "UM " + d.permintaanDetailUangMuka + " / PO " + d.permintaanDetailPo, "#f0fdf4", "#166534", "RL", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPermintaanDetailRealisasi(session); if (order) c.addOrder(Order.desc("tanggalPembuatan")); return c; } }), rendererPermintaanDetail(), detailHeadersPermintaanDetail(), detailWidthsPermintaanDetail(), "Detail PR Terealisasi");
	}

	private void appendMetricCard(Component parent, String title, int value, String desc, String bg, String color, String icon,
			final DetailDataProvider provider, final DetailRowRenderer renderer, final String[] headers, final String[] widths, final String detailTitle) {
		Div card = new Div();
		card.setStyle("flex:1 1 150px; min-width:150px; background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; padding:14px;"
				+ "box-shadow:0 10px 22px rgba(15,23,42,.06);");
		card.setParent(parent);
		Div top = new Div();
		top.setStyle("display:flex; align-items:center; justify-content:space-between; gap:10px;");
		top.setParent(card);
		appendHtml(top, "<div style='width:38px; height:38px; border-radius:12px; display:flex; align-items:center; justify-content:center; font-weight:800; background:"
				+ bg + "; color:" + color + ";'>" + escapeHtml(icon) + "</div>");
		A a = createClickableNumber(String.valueOf(value), "font-size:26px; font-weight:800; color:#0f172a; text-decoration:none; cursor:pointer;", detailTitle, provider, renderer, headers, widths);
		a.setParent(top);
		appendHtml(card, "<div style='font-size:12px; color:#64748b; margin-top:10px;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:#94a3b8; margin-top:3px;'>" + escapeHtml(desc) + "</div>");
	}

	private String buildMetricCard(String title, int value, String desc, String bg, String color, String icon) {
		return "<div style='flex:1 1 150px; min-width:150px; background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; padding:14px;"
				+ "box-shadow:0 10px 22px rgba(15,23,42,.06);'>"
				+ "<div style='display:flex; align-items:center; justify-content:space-between; gap:10px;'>"
				+ "<div style='width:38px; height:38px; border-radius:12px; display:flex; align-items:center; justify-content:center; font-weight:800; background:"
				+ bg + "; color:" + color + ";'>" + escapeHtml(icon) + "</div>"
				+ "<div style='font-size:26px; font-weight:800; color:#0f172a;'>" + value + "</div></div>"
				+ "<div style='font-size:12px; color:#64748b; margin-top:10px;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:#94a3b8; margin-top:3px;'>" + escapeHtml(desc) + "</div></div>";
	}

	private void renderDefinisiMetrikOverview(Component parent, PengadaanDashboardData d) {
		String risiko = d.totalPendingAging7 > 0 ? "Ada " + d.totalPendingAging7 + " dokumen PR/PO pending lebih dari 7 hari." : "Tidak ada pending PR/PO lebih dari 7 hari pada data yang terdeteksi.";
		appendHtml(parent, "<div style='margin-top:12px; padding:10px 12px; border-radius:12px; background:#ffffff; border:1px solid #e5e7eb; color:#64748b; font-size:11px; line-height:1.55;'>"
				+ "<b>Catatan metrik:</b> <b>Pending</b> membaca dokumen yang belum disetujui dan belum ditolak. "
				+ "<b>Coverage SOP</b> membandingkan dokumen yang memiliki disposisi/alur SOP terhadap total PR, PO, pembayaran, dan PKS. "
				+ escapeHtml(risiko) + " Mode debug saat ini: <b>" + (debug ? "AKTIF" : "NONAKTIF") + "</b>."
				+ "</div>");
	}

	private void renderAnalyticDasborTambahan(final PengadaanDashboardData d) {
		String pcWidth = Common.isMobile() ? "100%" : "50%";

		MyPortalchildren pcTop = new MyPortalchildren();
		pcTop.setWidth("100%");
		pcTop.setStyle("padding:6px; box-sizing:border-box;");
		pcTop.setParent(getCurrentDashboardPortalParent());

		MyPortalchildren pcLeft = new MyPortalchildren();
		pcLeft.setWidth(pcWidth);
		pcLeft.setStyle("padding:6px; box-sizing:border-box;");
		pcLeft.setParent(getCurrentDashboardPortalParent());

		MyPortalchildren pcRight = new MyPortalchildren();
		pcRight.setWidth(pcWidth);
		pcRight.setStyle("padding:6px; box-sizing:border-box;");
		pcRight.setParent(getCurrentDashboardPortalParent());

		MyPortalchildren pcBottom = new MyPortalchildren();
		pcBottom.setWidth("100%");
		pcBottom.setStyle("padding:6px; box-sizing:border-box;");
		pcBottom.setParent(getCurrentDashboardPortalParent());

		renderFunnelProsesPengadaan(pcTop, d);
		renderHealthIndexPengadaan(pcTop, d);
		renderTopPrPending(pcLeft, d);
		renderTopPoPending(pcRight, d);
		renderSebaranSatker(pcLeft, d);
		renderSebaranPenyediaPembayaran(pcRight, d);
		renderCoverageSopPengadaan(pcBottom, d);
		renderWatchlistRisiko(pcBottom, d);
		renderExecutionPlan(pcBottom, d);

		// Dashboard tambahan khusus perspektif logistik & pengadaan.
		renderLogistikProcurementOverview(pcTop, d);
		renderAgingAntrianLogistik(pcLeft, d);
		renderNilaiTertahanPengadaan(pcRight, d);
		renderKonsentrasiVendorLogistik(pcBottom, d);
		renderLogistikActionPlan(pcBottom, d);

		// Dashboard tambahan V12: kontrol audit, throughput, dan exception logistik.
		renderAuditTrailGapPengadaan(pcBottom, d);
		renderThroughputApprovalPengadaan(pcBottom, d);
		renderExceptionControlLogistik(pcBottom, d);

		// Dashboard tambahan V13: pembelian langsung dan realisasi detail PR.
		renderPembelianLangsungDashboard(pcBottom, d);
		renderRealisasiPermintaanDashboard(pcBottom, d);
		renderPenerimaanProcurementDashboard(pcBottom, d);
	}

	private PengadaanDashboardData loadPengadaanDashboardData() {
		PengadaanDashboardData d = new PengadaanDashboardData();
		Session session = null;
		try {
			// Sembuhkan dulu kolom mentah disetujui_oleh untuk pengajuan yang SOP-nya SUDAH menyetujui.
			// Tanpa ini, pengajuan yang sudah disetujui via alur SOP baru terhitung di dasbor setelah
			// dibuka/di-refresh satu per satu (kolom mentah baru terisi saat entitas kebetulan tersimpan),
			// karena seluruh kriteria dasbor menyaring KOLOM MENTAH `disetujuiOleh`.
			// Idempoten, dibatasi per pemanggilan, dan memakai sesi sendiri (tidak mengganggu sesi render).
			try {
				SinkronPersetujuanSopPengadaanHelper.backfillPersetujuanPengadaan();
			} catch (Exception exSinkron) {
				ais.common.ErrorAuditUtil.record(exSinkron,
						"auto-audit src/ais/action/master/asset/helper/DasboardPengadaan.java:backfillPersetujuan");
			}

			session = HibernateUtil.currentNativeSession();

			d.prPending = (int) countCriteria(criteriaPrPending(session));
			d.prApproved = (int) countCriteria(criteriaPrApproved(session));
			d.prRejected = (int) countCriteria(criteriaPrRejected(session));
			d.poPending = (int) countCriteria(criteriaPoPending(session));
			d.poApproved = (int) countCriteria(criteriaPoApproved(session));
			d.poRejected = (int) countCriteria(criteriaPoRejected(session));
			d.paymentApproved = (int) countCriteria(criteriaPembayaranDetailDisetujui(session));
			d.pksApproved = (int) countCriteria(criteriaPksDisetujui(session));
			d.pembelianLangsung = (int) countCriteria(criteriaPembelianLangsung(session));
			d.penerimaanPembelian = (int) countCriteria(criteriaPenerimaan(session));
			d.permintaanDetailTotal = (int) countCriteria(criteriaPermintaanDetail(session));
			d.permintaanDetailUangMuka = (int) countCriteria(criteriaPermintaanDetailUangMuka(session));
			d.permintaanDetailPo = (int) countCriteria(criteriaPermintaanDetailPo(session));
			d.permintaanDetailRealisasi = (int) countCriteria(criteriaPermintaanDetailRealisasi(session));
			d.permintaanDetailBelumRealisasi = (int) countCriteria(criteriaPermintaanDetailBelumRealisasi(session));

			d.nilaiPrPending = toNumber(sumCriteria(criteriaPrPending(session), "nilai"));
			d.nilaiPoPending = toNumber(sumCriteria(criteriaPoPending(session), "nilai"));
			d.nilaiPembayaranApproved = toNumber(sumCriteria(criteriaPembayaranDetailDisetujui(session), "dibayar"));
			d.nilaiPksApproved = toNumber(sumCriteria(criteriaPksDisetujui(session), "dp"));
			d.nilaiPembelianLangsung = toNumber(sumCriteria(criteriaPembelianLangsung(session), "nilai"));

			d.totalPr = (int) countCriteria(criteriaPermintaanAktif(session));
			d.totalPo = (int) countCriteria(criteriaPemesanan(session));
			d.totalPembayaranMaster = (int) countCriteria(criteriaPembayaranMaster(session));
			d.totalPks = (int) countCriteria(criteriaPks(session));
			d.prAdaSop = (int) countCriteria(criteriaPermintaanAktif(session).add(Restrictions.isNotNull("disposisiSop")));
			d.poAdaSop = (int) countCriteria(criteriaPemesanan(session).add(Restrictions.isNotNull("disposisiSop")));
			d.pembayaranAdaSop = (int) countCriteria(criteriaPembayaranMaster(session).add(Restrictions.isNotNull("disposisiSop")));
			d.pksAdaSop = (int) countCriteria(criteriaPks(session).add(Restrictions.isNotNull("disposisiSop")));
			d.totalDokumen = d.totalPr + d.totalPo + d.totalPembayaranMaster + d.totalPks;
			d.totalAdaSop = d.prAdaSop + d.poAdaSop + d.pembayaranAdaSop + d.pksAdaSop;
			d.coverageSopPercent = percent(d.totalAdaSop, d.totalDokumen);
			d.totalPending = d.prPending + d.poPending;

			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, -7);
			Date lewat7Hari = cal.getTime();
			d.prPendingAging7 = (int) countCriteria(criteriaPrPending(session).add(Restrictions.le("tanggalPembuatan", lewat7Hari)));
			d.poPendingAging7 = (int) countCriteria(criteriaPoPending(session).add(Restrictions.le("tanggalPembuatan", lewat7Hari)));
			d.totalPendingAging7 = d.prPendingAging7 + d.poPendingAging7;

			d.prPendingAge03 = (int) countCriteria(criteriaPrPendingAge(session, 0, 3));
			d.prPendingAge47 = (int) countCriteria(criteriaPrPendingAge(session, 4, 7));
			d.prPendingAgeOver7 = (int) countCriteria(criteriaPrPendingAge(session, 8, -1));
			d.poPendingAge03 = (int) countCriteria(criteriaPoPendingAge(session, 0, 3));
			d.poPendingAge47 = (int) countCriteria(criteriaPoPendingAge(session, 4, 7));
			d.poPendingAgeOver7 = (int) countCriteria(criteriaPoPendingAge(session, 8, -1));
			d.prApprovalRate = percent(d.prApproved, d.totalPr);
			d.poApprovalRate = percent(d.poApproved, d.totalPo);
			d.prToPoConversionRate = percent(d.totalPo, d.prApproved);
			d.paymentToPoRate = percent(d.paymentApproved, d.poApproved);
			d.nilaiTotalPending = Double.valueOf(toNumber(d.nilaiPrPending).doubleValue() + toNumber(d.nilaiPoPending).doubleValue());

			cal = Calendar.getInstance();
			cal.add(Calendar.DATE, -30);
			Date tigaPuluhHari = cal.getTime();
			d.prRejected30 = (int) countCriteria(criteriaPrRejected(session).add(Restrictions.ge("tanggalDitolak", tigaPuluhHari)));
			d.poRejected30 = (int) countCriteria(criteriaPoRejected(session).add(Restrictions.ge("tanggalDitolak", tigaPuluhHari)));

			d.topPrPending = loadTopPrPending(session);
			d.topPoPending = loadTopPoPending(session);
			d.sebaranSatker = loadGroupRows(criteriaPermintaanAktif(session), "satuanKerja", "nilai", 8);
			d.sebaranPenyediaPembayaran = loadPenyediaPembayaranRows(session, 8);
		} catch (Exception e) {
			debugError("loadPengadaanDashboardData", e);
		} finally {
			closeSessionSafely(session);
		}
		return d;
	}

	@SuppressWarnings("unchecked")
	private List<ProcurementItem> loadTopPrPending(Session session) {
		List<ProcurementItem> items = new ArrayList<ProcurementItem>();
		try {
			List<PermintaanPengadaanMasterAsset> rows = criteriaPrPending(session).addOrder(Order.desc("nilai"))
					.setMaxResults(DASHBOARD_SAMPLE_LIMIT < 8 ? DASHBOARD_SAMPLE_LIMIT : 8).list();
			for (PermintaanPengadaanMasterAsset row : rows) {
				ProcurementItem item = new ProcurementItem();
				item.jenis = "PR";
				item.status = "Pending";
				item.kode = safeString(row.getKode());
				item.tanggal = safeDate(row.getTanggalPembuatan());
				item.pihak = row.getSatuanKerja() == null ? "-" : safeString(row.getSatuanKerja().getNama());
				item.nilai = toNumber(row.getNilai());
				item.keterangan = safeString(row.getKeterangan());
				items.add(item);
			}
		} catch (Exception e) {
			debugError("loadTopPrPending", e);
		}
		return items;
	}

	@SuppressWarnings("unchecked")
	private List<ProcurementItem> loadTopPoPending(Session session) {
		List<ProcurementItem> items = new ArrayList<ProcurementItem>();
		try {
			List<PemesananPengadaanMasterAsset> rows = criteriaPoPending(session).addOrder(Order.desc("nilai"))
					.setMaxResults(DASHBOARD_SAMPLE_LIMIT < 8 ? DASHBOARD_SAMPLE_LIMIT : 8).list();
			for (PemesananPengadaanMasterAsset row : rows) {
				ProcurementItem item = new ProcurementItem();
				item.jenis = "PO";
				item.status = "Pending";
				item.kode = safeString(row.getKode());
				item.tanggal = safeDate(row.getTanggalPembuatan());
				item.pihak = row.getPenyedia() == null ? "-" : safeString(row.getPenyedia().getNama());
				item.nilai = toNumber(row.getNilai());
				item.keterangan = safeString(row.getKodeInvoice());
				items.add(item);
			}
		} catch (Exception e) {
			debugError("loadTopPoPending", e);
		}
		return items;
	}

	@SuppressWarnings("unchecked")
	private List<GroupSummaryItem> loadGroupRows(Criteria criteria, String groupProperty, String sumProperty, int limit) {
		List<GroupSummaryItem> items = new ArrayList<GroupSummaryItem>();
		try {
			criteria.setProjection(Projections.projectionList().add(Projections.groupProperty(groupProperty))
					.add(Projections.rowCount()).add(Projections.sum(sumProperty)));
			criteria.setMaxResults(limit <= 0 ? 8 : limit);
			List<Object[]> rows = criteria.list();
			for (Object[] row : rows) {
				GroupSummaryItem item = new GroupSummaryItem();
				item.label = labelFromEntity(row[0]);
				item.count = (int) toLong(row[1]);
				item.value = toNumber(row[2]);
				items.add(item);
			}
		} catch (Exception e) {
			debugError("loadGroupRows " + groupProperty, e);
		}
		return items;
	}

	@SuppressWarnings("unchecked")
	private List<GroupSummaryItem> loadPenyediaPembayaranRows(Session session, int limit) {
		List<GroupSummaryItem> items = new ArrayList<GroupSummaryItem>();
		try {
			Criteria c = session.createCriteria(PembayaranPengadaanMasterAssetDetail.class)
					.createAlias("pembayaranPengadaanMasterAsset", "pembayaranPengadaanMasterAsset", Criteria.LEFT_JOIN)
					.add(Restrictions.isNotNull("pembayaranPengadaanMasterAsset.disetujuiOleh"))
					.add(Restrictions.isNotNull("pembayaranPengadaanMasterAsset.tanggalPersetujuan"));
			applyGlobalDateRange(c, "pembayaranPengadaanMasterAsset.tanggalPersetujuan");
			c.setProjection(Projections.projectionList().add(Projections.groupProperty("pembayaranPengadaanMasterAsset.penyedia"))
					.add(Projections.rowCount()).add(Projections.sum("dibayar")));
			c.setMaxResults(limit <= 0 ? 8 : limit);
			List<Object[]> rows = c.list();
			for (Object[] row : rows) {
				GroupSummaryItem item = new GroupSummaryItem();
				item.label = labelFromEntity(row[0]);
				item.count = (int) toLong(row[1]);
				item.value = toNumber(row[2]);
				items.add(item);
			}
		} catch (Exception e) {
			debugError("loadPenyediaPembayaranRows", e);
		}
		return items;
	}

	private void renderFunnelProsesPengadaan(Component parent, PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel(" Proses Pengadaan", parent);
		int max = getMaxValue(new int[] { d.prPending, d.prApproved, d.poPending, d.poApproved, d.paymentApproved, d.pksApproved });
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:12px;'>Ringkasan aliran dokumen dari permintaan sampai pembayaran/PKS berdasarkan query pengadaan existing. Angka dapat diklik untuk membuka popup detail.</div>");
		appendFunnelRow(pch, "PR Menunggu Persetujuan", d.prPending, max, "#dc2626", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPrPending(session); if (order) c.addOrder(Order.desc("id")); return c; } }), rendererPrDetail(), detailHeadersPr(), detailWidthsPr(), "Detail PR Menunggu Persetujuan");
		appendFunnelRow(pch, "PR Disetujui", d.prApproved, max, "#16a34a", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPrApproved(session); if (order) c.addOrder(Order.desc("tanggalPersetujuan")); return c; } }), rendererPrDetail(), detailHeadersPr(), detailWidthsPr(), "Detail PR Disetujui");
		appendFunnelRow(pch, "PO Menunggu Persetujuan", d.poPending, max, "#ea580c", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPoPending(session); if (order) c.addOrder(Order.desc("id")); return c; } }), rendererPoDetail(), detailHeadersPo(), detailWidthsPo(), "Detail PO Menunggu Persetujuan");
		appendFunnelRow(pch, "PO Disetujui", d.poApproved, max, "#2563eb", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPoApproved(session); if (order) c.addOrder(Order.desc("tanggalPersetujuan")); return c; } }), rendererPoDetail(), detailHeadersPo(), detailWidthsPo(), "Detail PO Disetujui");
		appendFunnelRow(pch, "Pembayaran Disetujui", d.paymentApproved, max, "#7c3aed", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPembayaranDetailDisetujui(session); if (order) c.addOrder(Order.desc("id")); return c; } }), rendererPembayaranDetail(), detailHeadersPembayaran(), detailWidthsPembayaran(), "Detail Pembayaran Disetujui");
		appendFunnelRow(pch, "PKS Disetujui", d.pksApproved, max, "#0891b2", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPksDisetujui(session); if (order) c.addOrder(Order.desc("tanggalPersetujuan")); return c; } }), rendererPksDetail(), detailHeadersPks(), detailWidthsPks(), "Detail PKS Disetujui");
	}

	private void renderHealthIndexPengadaan(Component parent, PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel("Indeks Kesehatan Pengadaan", parent);
		int pendingRisk = percent(d.totalPendingAging7, Math.max(1, d.totalPending));
		int rejectionRisk = percent(d.prRejected30 + d.poRejected30, Math.max(1, d.prRejected + d.poRejected + d.totalPending));
		int coverageBonus = d.coverageSopPercent;
		int healthScore = 100 - ((pendingRisk * 45) / 100) - ((rejectionRisk * 25) / 100) + ((coverageBonus * 20) / 100);
		if (healthScore < 0) {
			healthScore = 0;
		}
		if (healthScore > 100) {
			healthScore = 100;
		}
		String status = healthScore >= 80 ? "Sehat" : (healthScore >= 60 ? "Perlu Dipantau" : "Prioritas Perbaikan");
		String statusBg = healthScore >= 80 ? "#dcfce7" : (healthScore >= 60 ? "#fef3c7" : "#fee2e2");
		String statusColor = healthScore >= 80 ? "#166534" : (healthScore >= 60 ? "#92400e" : "#991b1b");

		Div wrap = new Div();
		wrap.setStyle("display:flex; gap:14px; flex-wrap:wrap; align-items:stretch;");
		wrap.setParent(pch);

		Div scoreCard = new Div();
		scoreCard.setStyle("flex:1 1 230px; border-radius:16px; padding:16px; color:#ffffff; background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); box-shadow:0 12px 24px rgba(37,99,235,.18);");
		scoreCard.setParent(wrap);
		appendHtml(scoreCard, "<div style='font-size:11px; letter-spacing:.08em; text-transform:uppercase; opacity:.82;'>Procurement Health Score</div>");
		A score = createClickableNumber(String.valueOf(healthScore), "font-size:46px; line-height:1; font-weight:900; margin-top:10px; color:#ffffff; text-decoration:none; display:block; cursor:pointer;", "Detail Indeks Kesehatan Pengadaan", detailProviderHealthSummary(d, pendingRisk, rejectionRisk, healthScore, status), rendererSummaryDetail(), detailHeadersSummary(), null);
		score.setParent(scoreCard);
		appendHtml(scoreCard, "<div style='display:inline-block; margin-top:12px; border-radius:999px; background:" + statusBg + "; color:" + statusColor + "; padding:5px 10px; font-size:11px; font-weight:800;'>" + status + "</div>");

		Div gaugeBox = new Div();
		gaugeBox.setStyle("flex:2 1 420px;");
		gaugeBox.setParent(wrap);
		appendMiniGauge(gaugeBox, "Pending Aging > 7 Hari", pendingRisk, "Rasio PR/PO pending yang berumur lebih dari 7 hari.", "#dc2626", detailProviderAgingPrPo(), rendererProcurementQueue(), detailHeadersQueue(), detailWidthsQueue(), "Detail Pending Aging > 7 Hari");
		appendMiniGauge(gaugeBox, "Risiko Penolakan 30 Hari", rejectionRisk, "Proporsi dokumen yang ditolak 30 hari terakhir.", "#ea580c", detailProviderRejected30PrPo(), rendererProcurementQueue(), detailHeadersQueue(), detailWidthsQueue(), "Detail Penolakan 30 Hari");
		appendMiniGauge(gaugeBox, "Coverage SOP", d.coverageSopPercent, "Dokumen yang sudah punya jejak disposisi/alur SOP.", "#16a34a", detailProviderCoverageSop(), rendererSummaryDetail(), detailHeadersSummary(), null, "Detail Coverage SOP Dokumen");
	}

	private void renderTopPrPending(Component parent, PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel("Top PR Pending Berdasarkan Nilai", parent);
		renderProcurementList(pch, d.topPrPending, "Unit Pemohon", "Belum ada PR pending yang dapat ditampilkan.");
	}

	private void renderTopPoPending(Component parent, PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel("Top PO Pending Berdasarkan Nilai", parent);
		renderProcurementList(pch, d.topPoPending, "Penyedia", "Belum ada PO pending yang dapat ditampilkan.");
	}

	private void renderProcurementList(Component parent, List<ProcurementItem> items, String pihakLabel, String emptyMessage) {
		if (items == null || items.isEmpty()) {
			appendEmptyState(parent, emptyMessage);
			return;
		}
		Div box = new Div();
		box.setStyle("display:flex; flex-direction:column; gap:9px;");
		box.setParent(parent);
		int no = 1;
		for (final ProcurementItem item : items) {
			Div card = new Div();
			card.setStyle("padding:11px; border:1px solid #e5e7eb; border-radius:14px; background:#f8fafc;");
			card.setParent(box);
			Div line = new Div();
			line.setStyle("display:flex; justify-content:space-between; gap:10px; align-items:flex-start;");
			line.setParent(card);
			appendHtml(line, "<div><div style='font-size:12px; font-weight:900; color:#0f172a;'>" + no + ". " + escapeHtml(item.kode) + "</div>"
					+ "<div style='font-size:11px; color:#64748b; margin-top:4px;'>" + escapeHtml(pihakLabel) + ": " + escapeHtml(item.pihak) + " · " + escapeHtml(item.tanggal) + "</div>"
					+ (item.keterangan == null || item.keterangan.equals("-") ? "" : "<div style='font-size:11px; color:#94a3b8; margin-top:3px;'>" + escapeHtml(item.keterangan) + "</div>")
					+ "</div>");
			List one = new ArrayList();
			one.add(item);
			A a = createClickableNumber(formatNumber(item.nilai), "font-size:12px; font-weight:900; color:#1d4ed8; white-space:nowrap; text-decoration:none; cursor:pointer;", "Detail " + safeString(item.jenis) + " " + safeString(item.kode), detailProviderStaticList(one), rendererProcurementQueue(), detailHeadersQueue(), detailWidthsQueue());
			a.setParent(line);
			no++;
		}
	}

	private void renderSebaranSatker(Component parent, PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel("Sebaran PR per Satuan Kerja", parent);
		renderGroupSummaryList(pch, d.sebaranSatker, "Belum ada data satuan kerja yang dapat ditampilkan.", "#2563eb");
	}

	private void renderSebaranPenyediaPembayaran(Component parent, PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel("Top Penyedia dari Pembayaran Disetujui", parent);
		renderGroupSummaryList(pch, d.sebaranPenyediaPembayaran, "Belum ada data pembayaran penyedia yang dapat ditampilkan.", "#7c3aed");
	}

	private void renderGroupSummaryList(Component parent, List<GroupSummaryItem> items, String emptyMessage, String color) {
		if (items == null || items.isEmpty()) {
			appendEmptyState(parent, emptyMessage);
			return;
		}
		int max = 0;
		for (GroupSummaryItem item : items) {
			if (item.count > max) {
				max = item.count;
			}
		}
		int no = 1;
		for (final GroupSummaryItem item : items) {
			int pct = max <= 0 ? 0 : (int) Math.round((item.count * 100.0) / max);
			if (pct < 4 && item.count > 0) {
				pct = 4;
			}
			Div row = new Div();
			row.setStyle("padding:10px 0; border-bottom:1px solid #f1f5f9;");
			row.setParent(parent);
			Div top = new Div();
			top.setStyle("display:flex; justify-content:space-between; gap:12px; align-items:center;");
			top.setParent(row);
			appendHtml(top, "<div style='font-size:12px; font-weight:700; color:#334155;'>" + no + ". " + escapeHtml(item.label) + "</div>");
			List one = new ArrayList();
			one.add(new SummaryDetailItem(item.label, item.count, "Total nilai: " + formatNumber(item.value)));
			A a = createClickableNumber(String.valueOf(item.count), "font-size:12px; font-weight:800; color:#0f172a; text-decoration:none; cursor:pointer;", "Detail " + item.label, detailProviderStaticList(one), rendererSummaryDetail(), detailHeadersSummary(), null);
			a.setParent(top);
			appendHtml(row, "<div style='font-size:11px; color:#64748b; margin-top:4px;'>Total nilai: <b>" + formatNumber(item.value) + "</b></div>"
					+ "<div style='margin-top:7px; height:8px; background:#e2e8f0; border-radius:999px; overflow:hidden;'>"
					+ "<div style='height:8px; width:" + pct + "%; background:" + color + "; border-radius:999px;'></div></div>");
			no++;
		}
	}

	private void renderCoverageSopPengadaan(Component parent, PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel("Coverage Alur SOP Dokumen Pengadaan", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:12px;'>Memastikan setiap dokumen utama pengadaan memiliki jejak SOP/disposisi yang dapat ditelusuri. Angka coverage dapat diklik.</div>");
		appendCoverageRow(pch, "PR / Permintaan", d.prAdaSop, d.totalPr, "#2563eb", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPermintaanAktif(session).add(Restrictions.isNotNull("disposisiSop")); if (order) c.addOrder(Order.desc("id")); return c; } }), rendererPrDetail(), detailHeadersPr(), detailWidthsPr(), "Detail PR yang Memiliki SOP");
		appendCoverageRow(pch, "PO / Pemesanan", d.poAdaSop, d.totalPo, "#16a34a", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPemesanan(session).add(Restrictions.isNotNull("disposisiSop")); if (order) c.addOrder(Order.desc("id")); return c; } }), rendererPoDetail(), detailHeadersPo(), detailWidthsPo(), "Detail PO yang Memiliki SOP");
		appendCoverageRow(pch, "Pembayaran", d.pembayaranAdaSop, d.totalPembayaranMaster, "#7c3aed", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPembayaranMaster(session).add(Restrictions.isNotNull("disposisiSop")); if (order) c.addOrder(Order.desc("id")); return c; } }), rendererPembayaranMasterDetail(), detailHeadersPembayaranMaster(), detailWidthsPembayaranMaster(), "Detail Pembayaran yang Memiliki SOP");
		appendCoverageRow(pch, "PKS", d.pksAdaSop, d.totalPks, "#0891b2", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPks(session).add(Restrictions.isNotNull("disposisiSop")); if (order) c.addOrder(Order.desc("id")); return c; } }), rendererPksDetail(), detailHeadersPks(), detailWidthsPks(), "Detail PKS yang Memiliki SOP");
	}

	private void renderWatchlistRisiko(Component parent, PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel("Watchlist Risiko & Tindak Lanjut", parent);
		Div wrap = new Div();
		wrap.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr)); gap:12px;");
		wrap.setParent(pch);
		appendPressureCard(wrap, "PR Pending > 7 Hari", d.prPendingAging7, "Perlu eskalasi approval PR.", "#fee2e2", "#991b1b", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPrPending(session).add(Restrictions.le("tanggalPembuatan", dateOffset(-7))); if (order) c.addOrder(Order.asc("tanggalPembuatan")); return c; } }), rendererPrDetail(), detailHeadersPr(), detailWidthsPr(), "Detail PR Pending Lebih dari 7 Hari");
		appendPressureCard(wrap, "PO Pending > 7 Hari", d.poPendingAging7, "Cek negosiasi/approval PO.", "#ffedd5", "#9a3412", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPoPending(session).add(Restrictions.le("tanggalPembuatan", dateOffset(-7))); if (order) c.addOrder(Order.asc("tanggalPembuatan")); return c; } }), rendererPoDetail(), detailHeadersPo(), detailWidthsPo(), "Detail PO Pending Lebih dari 7 Hari");
		appendPressureCard(wrap, "PR Ditolak 30 Hari", d.prRejected30, "Evaluasi kualitas permintaan.", "#fef3c7", "#92400e", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPrRejected(session).add(Restrictions.ge("tanggalDitolak", dateOffset(-30))); if (order) c.addOrder(Order.desc("tanggalDitolak")); return c; } }), rendererPrDetail(), detailHeadersPr(), detailWidthsPr(), "Detail PR Ditolak 30 Hari");
		appendPressureCard(wrap, "PO Ditolak 30 Hari", d.poRejected30, "Evaluasi penyedia/spesifikasi.", "#ede9fe", "#5b21b6", detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPoRejected(session).add(Restrictions.ge("tanggalDitolak", dateOffset(-30))); if (order) c.addOrder(Order.desc("tanggalDitolak")); return c; } }), rendererPoDetail(), detailHeadersPo(), detailWidthsPo(), "Detail PO Ditolak 30 Hari");
	}

	private void renderExecutionPlan(Component parent, PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel("Prioritas Eksekusi Pengadaan", parent);
		String prioritas1 = d.totalPendingAging7 > 0 ? "Eskalasi " + d.totalPendingAging7 + " PR/PO pending lebih dari 7 hari agar SLA proses tidak menumpuk."
				: "Tidak ada PR/PO pending lebih dari 7 hari pada data yang terdeteksi.";
		String prioritas2 = d.coverageSopPercent < 80 ? "Perbaiki coverage SOP dokumen pengadaan; saat ini baru " + d.coverageSopPercent + "%."
				: "Coverage SOP relatif baik di " + d.coverageSopPercent + "%, tetap pastikan dokumen baru konsisten memakai alur SOP.";
		String prioritas3 = (d.prRejected30 + d.poRejected30) > 0 ? "Review " + (d.prRejected30 + d.poRejected30) + " dokumen yang ditolak 30 hari terakhir untuk menemukan pola masalah."
				: "Belum ada penolakan 30 hari terakhir yang perlu diprioritaskan.";
		String prioritas4 = d.topPrPending != null && !d.topPrPending.isEmpty() ? "Prioritaskan PR bernilai besar: " + d.topPrPending.get(0).kode + "."
				: "Belum ada PR pending bernilai besar yang perlu diprioritaskan.";

		String html = "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(230px,1fr)); gap:12px;'>"
				+ buildActionPlanCard("1", "Kontrol SLA", prioritas1, "#fee2e2", "#991b1b")
				+ buildActionPlanCard("2", "Disiplin SOP", prioritas2, "#dbeafe", "#1e40af")
				+ buildActionPlanCard("3", "Evaluasi Penolakan", prioritas3, "#fef3c7", "#92400e")
				+ buildActionPlanCard("4", "Prioritas Nilai", prioritas4, "#ecfdf5", "#166534")
				+ "</div>";
		appendHtml(pch, html);
	}


	// =======================================================================================
	// DASHBOARD TAMBAHAN: LOGISTIK & PENGADAAN
	// =======================================================================================

	private void renderAuditTrailGapPengadaan(Component parent, final PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel("Audit Trail & Kelengkapan SOP Dokumen Pengadaan", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:12px;'>"
				+ "membantu memastikan setiap dokumen pengadaan memiliki jejak SOP/disposisi agar lebih siap diaudit. Angka pada kartu dapat diklik untuk melihat detail dokumen.</div>");
		Div wrap = new Div();
		wrap.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(200px,1fr)); gap:12px;");
		wrap.setParent(pch);

		appendPressureCard(wrap, "PR Tanpa SOP", Math.max(0, d.totalPr - d.prAdaSop), "Permintaan pengadaan yang belum memiliki alur SOP.", "#fee2e2", "#991b1b",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPermintaanAktif(session).add(Restrictions.isNull("disposisiSop")); if (order) c.addOrder(Order.desc("id")); return c; } }),
				rendererPrDetail(), detailHeadersPr(), detailWidthsPr(), "Detail PR Tanpa SOP");

		appendPressureCard(wrap, "PO Tanpa SOP", Math.max(0, d.totalPo - d.poAdaSop), "Pemesanan yang belum memiliki alur SOP.", "#ffedd5", "#9a3412",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPemesanan(session).add(Restrictions.isNull("disposisiSop")); if (order) c.addOrder(Order.desc("id")); return c; } }),
				rendererPoDetail(), detailHeadersPo(), detailWidthsPo(), "Detail PO Tanpa SOP");

		appendPressureCard(wrap, "Pembayaran Tanpa SOP", Math.max(0, d.totalPembayaranMaster - d.pembayaranAdaSop), "Dokumen pembayaran yang belum memiliki alur SOP.", "#ede9fe", "#5b21b6",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPembayaranMaster(session).add(Restrictions.isNull("disposisiSop")); if (order) c.addOrder(Order.desc("id")); return c; } }),
				rendererPembayaranMasterDetail(), detailHeadersPembayaranMaster(), detailWidthsPembayaranMaster(), "Detail Pembayaran Tanpa SOP");

		appendPressureCard(wrap, "PKS Tanpa SOP", Math.max(0, d.totalPks - d.pksAdaSop), "Kontrak/PKS yang belum memiliki alur SOP.", "#ecfeff", "#155e75",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPks(session).add(Restrictions.isNull("disposisiSop")); if (order) c.addOrder(Order.desc("id")); return c; } }),
				rendererPksDetail(), detailHeadersPks(), detailWidthsPks(), "Detail PKS Tanpa SOP");
	}

	private void renderThroughputApprovalPengadaan(Component parent, final PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel("Throughput Approval & Konversi Pengadaan", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:12px;'>"
				+ "Melihat kelancaran proses dari PR, PO, sampai pembayaran. Persentase dapat diklik untuk membuka data pendukung.</div>");
		Div wrap = new Div();
		wrap.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(210px,1fr)); gap:12px;");
		wrap.setParent(pch);

		appendClickableValueCard(wrap, "Approval Rate PR", d.prApprovalRate + "%", d.prApproved + " PR disetujui dari " + d.totalPr + " total PR.", "#ecfdf5", "#166534",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPrApproved(session); if (order) c.addOrder(Order.desc("tanggalPersetujuan")); return c; } }),
				rendererPrDetail(), detailHeadersPr(), detailWidthsPr(), "Detail Approval Rate PR");

		appendClickableValueCard(wrap, "Approval Rate PO", d.poApprovalRate + "%", d.poApproved + " PO disetujui dari " + d.totalPo + " total PO.", "#dbeafe", "#1e40af",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPoApproved(session); if (order) c.addOrder(Order.desc("tanggalPersetujuan")); return c; } }),
				rendererPoDetail(), detailHeadersPo(), detailWidthsPo(), "Detail Approval Rate PO");

		appendClickableValueCard(wrap, "Konversi PR ke PO", d.prToPoConversionRate + "%", d.totalPo + " PO dibanding " + d.prApproved + " PR disetujui.", "#fef3c7", "#92400e",
				detailProviderStaticList(buildConversionSummaryRows(d)), rendererSummaryDetail(), detailHeadersSummary(), null, "Ringkasan Konversi PR ke PO");

		appendClickableValueCard(wrap, "Konversi PO ke Pembayaran", d.paymentToPoRate + "%", d.paymentApproved + " pembayaran dibanding " + d.poApproved + " PO disetujui.", "#f5f3ff", "#5b21b6",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPembayaranDetailDisetujui(session); if (order) c.addOrder(Order.desc("id")); return c; } }),
				rendererPembayaranDetail(), detailHeadersPembayaran(), detailWidthsPembayaran(), "Detail Konversi PO ke Pembayaran");
	}

	private List buildConversionSummaryRows(PengadaanDashboardData d) {
		List rows = new ArrayList();
		rows.add(new SummaryDetailItem("PR Disetujui", d.prApproved, "Basis pembanding untuk konversi ke PO."));
		rows.add(new SummaryDetailItem("Total PO", d.totalPo, "Jumlah dokumen PO pada filter aktif."));
		rows.add(new SummaryDetailItem("PO Disetujui", d.poApproved, "PO yang sudah melewati approval."));
		rows.add(new SummaryDetailItem("Pembayaran Disetujui", d.paymentApproved, "Pembayaran yang sudah disetujui pada filter aktif."));
		rows.add(new SummaryDetailItem("Konversi PR ke PO", d.prToPoConversionRate + "%", d.totalPo + " PO dibanding " + d.prApproved + " PR disetujui."));
		rows.add(new SummaryDetailItem("Konversi PO ke Pembayaran", d.paymentToPoRate + "%", d.paymentApproved + " pembayaran dibanding " + d.poApproved + " PO disetujui."));
		return rows;
	}

	private void renderExceptionControlLogistik(Component parent, final PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel("Exception Control Logistik & Pengadaan", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:12px;'>"
				+ "Pusat kendali exception untuk item yang perlu diprioritaskan: aging kritis, penolakan, nilai pending, dan gap audit trail.</div>");
		Div wrap = new Div();
		wrap.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(210px,1fr)); gap:12px;");
		wrap.setParent(pch);

		appendPressureCard(wrap, "Pending > 7 Hari", d.totalPendingAging7, "PR/PO pending yang sudah melewati 7 hari.", "#fee2e2", "#991b1b",
				detailProviderAgingPrPo(), rendererProcurementQueue(), detailHeadersQueue(), detailWidthsQueue(), "Detail Pending PR/PO Lebih dari 7 Hari");

		appendPressureCard(wrap, "Ditolak 30 Hari", d.prRejected30 + d.poRejected30, "Dokumen PR/PO ditolak 30 hari terakhir.", "#ffedd5", "#9a3412",
				detailProviderRejected30PrPo(), rendererProcurementQueue(), detailHeadersQueue(), detailWidthsQueue(), "Detail PR/PO Ditolak 30 Hari");

		appendPressureCard(wrap, "Total Antrian", d.totalPending, "Gabungan PR dan PO pending yang perlu dipantau.", "#eef2ff", "#3730a3",
				detailProviderAntrianPrPo(), rendererProcurementQueue(), detailHeadersQueue(), detailWidthsQueue(), "Detail Total Antrian PR/PO");

		appendClickableValueCard(wrap, "Nilai Pending", formatNumber(d.nilaiTotalPending), "Nilai gabungan PR/PO yang tertahan di pipeline.", "#f8fafc", "#0f172a",
				detailProviderAntrianPrPo(), rendererProcurementQueue(), detailHeadersQueue(), detailWidthsQueue(), "Detail Nilai Pending PR/PO");
	}

	private void renderLogistikProcurementOverview(Component parent, final PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel("Control Tower Logistik & Pengadaan", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:12px;'>"
				+ "Ringkasan ini membantu melihat kesiapan proses dari PR, PO, pembayaran, sampai kontrol logistik secara ringkas. Semua angka utama dapat diklik untuk melihat data detail.</div>");

		Div wrap = new Div();
		wrap.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(210px,1fr)); gap:12px;");
		wrap.setParent(pch);

		appendClickableValueCard(wrap, "Approval Rate PR", d.prApprovalRate + "%", "Rasio PR disetujui terhadap total PR pada filter aktif.", "#dcfce7", "#166534",
				detailProviderLogistikSummary(d), rendererSummaryDetail(), detailHeadersSummary(), null, "Detail Ringkasan Logistik Pengadaan");
		appendClickableValueCard(wrap, "Approval Rate PO", d.poApprovalRate + "%", "Rasio PO disetujui terhadap total PO pada filter aktif.", "#dbeafe", "#1e40af",
				detailProviderLogistikSummary(d), rendererSummaryDetail(), detailHeadersSummary(), null, "Detail Ringkasan Logistik Pengadaan");
		appendClickableValueCard(wrap, "PR ke PO", d.prToPoConversionRate + "%", "Indikasi konversi permintaan menjadi pemesanan.", "#ede9fe", "#5b21b6",
				detailProviderLogistikSummary(d), rendererSummaryDetail(), detailHeadersSummary(), null, "Detail Konversi PR ke PO");
		appendClickableValueCard(wrap, "PO ke Bayar", d.paymentToPoRate + "%", "Indikasi PO disetujui yang sudah masuk pembayaran.", "#cffafe", "#155e75",
				detailProviderLogistikSummary(d), rendererSummaryDetail(), detailHeadersSummary(), null, "Detail Konversi PO ke Pembayaran");
	}

	private void renderAgingAntrianLogistik(Component parent, final PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel("Aging Antrian Logistik PR/PO", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:12px;'>"
				+ "Pisahkan antrian baru, mulai menua, dan kritis agar tim pengadaan lebih mudah menentukan eskalasi.</div>");
		Div wrap = new Div();
		wrap.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(190px,1fr)); gap:12px;");
		wrap.setParent(pch);

		appendPressureCard(wrap, "PR 0-3 Hari", d.prPendingAge03, "Antrian PR baru / masih normal.", "#ecfeff", "#155e75",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPrPendingAge(session, 0, 3); if (order) c.addOrder(Order.desc("tanggalPembuatan")); return c; } }), rendererPrDetail(), detailHeadersPr(), detailWidthsPr(), "Detail PR Pending 0-3 Hari");
		appendPressureCard(wrap, "PR 4-7 Hari", d.prPendingAge47, "Mulai perlu dipantau.", "#fef3c7", "#92400e",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPrPendingAge(session, 4, 7); if (order) c.addOrder(Order.asc("tanggalPembuatan")); return c; } }), rendererPrDetail(), detailHeadersPr(), detailWidthsPr(), "Detail PR Pending 4-7 Hari");
		appendPressureCard(wrap, "PR > 7 Hari", d.prPendingAgeOver7, "Prioritas eskalasi approval PR.", "#fee2e2", "#991b1b",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPrPendingAge(session, 8, -1); if (order) c.addOrder(Order.asc("tanggalPembuatan")); return c; } }), rendererPrDetail(), detailHeadersPr(), detailWidthsPr(), "Detail PR Pending Lebih dari 7 Hari");

		appendPressureCard(wrap, "PO 0-3 Hari", d.poPendingAge03, "Antrian PO baru / masih normal.", "#eef2ff", "#3730a3",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPoPendingAge(session, 0, 3); if (order) c.addOrder(Order.desc("tanggalPembuatan")); return c; } }), rendererPoDetail(), detailHeadersPo(), detailWidthsPo(), "Detail PO Pending 0-3 Hari");
		appendPressureCard(wrap, "PO 4-7 Hari", d.poPendingAge47, "Cek penyedia/negosiasi/approval.", "#ffedd5", "#9a3412",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPoPendingAge(session, 4, 7); if (order) c.addOrder(Order.asc("tanggalPembuatan")); return c; } }), rendererPoDetail(), detailHeadersPo(), detailWidthsPo(), "Detail PO Pending 4-7 Hari");
		appendPressureCard(wrap, "PO > 7 Hari", d.poPendingAgeOver7, "Prioritas eskalasi PO.", "#fce7f3", "#9d174d",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPoPendingAge(session, 8, -1); if (order) c.addOrder(Order.asc("tanggalPembuatan")); return c; } }), rendererPoDetail(), detailHeadersPo(), detailWidthsPo(), "Detail PO Pending Lebih dari 7 Hari");
	}

	private void renderNilaiTertahanPengadaan(Component parent, final PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel("Nilai Tertahan dalam Pipeline Pengadaan", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:12px;'>"
				+ "Memantau nilai rupiah yang masih tertahan pada PR/PO pending supaya prioritas approval dapat dilihat dari sisi dampak biaya.</div>");
		Div wrap = new Div();
		wrap.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(210px,1fr)); gap:12px;");
		wrap.setParent(pch);

		appendClickableValueCard(wrap, "Nilai PR Pending", formatNumber(d.nilaiPrPending), "Total nilai PR yang belum disetujui/ditolak.", "#fee2e2", "#991b1b",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPrPending(session); if (order) c.addOrder(Order.desc("nilai")); return c; } }), rendererPrDetail(), detailHeadersPr(), detailWidthsPr(), "Detail Nilai PR Pending");
		appendClickableValueCard(wrap, "Nilai PO Pending", formatNumber(d.nilaiPoPending), "Total nilai PO yang belum disetujui/ditolak.", "#ffedd5", "#9a3412",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPoPending(session); if (order) c.addOrder(Order.desc("nilai")); return c; } }), rendererPoDetail(), detailHeadersPo(), detailWidthsPo(), "Detail Nilai PO Pending");
		appendClickableValueCard(wrap, "Total Nilai Pending", formatNumber(d.nilaiTotalPending), "Gabungan nilai PR dan PO pending.", "#f8fafc", "#0f172a",
				detailProviderAntrianPrPo(), rendererProcurementQueue(), detailHeadersQueue(), detailWidthsQueue(), "Detail Total Nilai Pending PR/PO");
		appendClickableValueCard(wrap, "Nilai Pembayaran", formatNumber(d.nilaiPembayaranApproved), "Total pembayaran yang sudah disetujui.", "#ecfdf5", "#166534",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPembayaranDetailDisetujui(session); if (order) c.addOrder(Order.desc("id")); return c; } }), rendererPembayaranDetail(), detailHeadersPembayaran(), detailWidthsPembayaran(), "Detail Nilai Pembayaran Disetujui");
	}

	private void renderKonsentrasiVendorLogistik(Component parent, PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel("Konsentrasi Penyedia & Beban Logistik", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:12px;'>"
				+ "Gunakan untuk melihat apakah realisasi pembayaran terlalu terkonsentrasi pada penyedia tertentu. Angka jumlah transaksi dapat diklik.</div>");
		renderGroupSummaryList(pch, d.sebaranPenyediaPembayaran, "Belum ada data penyedia dari pembayaran disetujui pada filter aktif.", "#0891b2");
	}

	private void renderLogistikActionPlan(Component parent, PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel("Prioritas Eksekusi Logistik & Pengadaan", parent);
		String p1 = d.prPendingAgeOver7 > 0 || d.poPendingAgeOver7 > 0
				? "Eskalasi dokumen yang sudah masuk kategori aging kritis sebelum berdampak ke jadwal pengadaan."
				: "Aging kritis pada PR/PO relatif aman berdasarkan filter aktif.";
		String p2 = d.nilaiTotalPending.doubleValue() > 0.0
				? "Dahulukan review dokumen bernilai besar agar dana tertahan dalam pipeline dapat segera diputuskan."
				: "Belum ada nilai pending yang terdeteksi pada filter aktif.";
		String p3 = d.paymentToPoRate < 70
				? "Cek rantai PO sampai pembayaran; rasio pembayaran terhadap PO disetujui masih perlu dipantau."
				: "Rantai PO ke pembayaran relatif baik, tetap cek kelengkapan bukti bayar dan penerimaan.";
		String p4 = d.coverageSopPercent < 90
				? "Lengkapi jejak SOP agar setiap dokumen pengadaan mudah diaudit."
				: "Jejak SOP relatif baik; pertahankan konsistensi pada dokumen baru.";
		String html = "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(230px,1fr)); gap:12px;'>"
				+ buildActionPlanCard("A", "Aging Kritis", p1, "#fee2e2", "#991b1b")
				+ buildActionPlanCard("B", "Nilai Pending", p2, "#fef3c7", "#92400e")
				+ buildActionPlanCard("C", "PO ke Pembayaran", p3, "#dbeafe", "#1e40af")
				+ buildActionPlanCard("D", "Audit Trail SOP", p4, "#ecfdf5", "#166534")
				+ "</div>";
		appendHtml(pch, html);
	}

	private void renderPembelianLangsungDashboard(Component parent, final PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel("Pembelian Langsung", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:12px;'>"
				+ "Memantau dokumen PO yang ditandai <b>pembelian langsung</b> agar nilai dan prosesnya tetap terkendali meski tidak melalui alur PR biasa.</div>");
		Div wrap = new Div();
		wrap.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(210px,1fr)); gap:12px;");
		wrap.setParent(pch);

		appendClickableValueCard(wrap, "Jumlah Pembelian Langsung", String.valueOf(d.pembelianLangsung),
				"Data dari PemesananPengadaanMasterAsset.pembelianLangsung = true.", "#fef3c7", "#92400e",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPembelianLangsung(session); if (order) c.addOrder(Order.desc("tanggalPembuatan")); return c; } }),
				rendererPoDetail(), detailHeadersPo(), detailWidthsPo(), "Detail Pembelian Langsung");
		appendClickableValueCard(wrap, "Nilai Pembelian Langsung", formatNumber(d.nilaiPembelianLangsung),
				"Total nilai pembelian langsung pada filter aktif.", "#ffedd5", "#9a3412",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPembelianLangsung(session); if (order) c.addOrder(Order.desc("nilai")); return c; } }),
				rendererPoDetail(), detailHeadersPo(), detailWidthsPo(), "Detail Nilai Pembelian Langsung");
		appendClickableValueCard(wrap, "Penerimaan dari Pembelian", String.valueOf(d.penerimaanPembelian),
				"Monitoring penerimaan barang/jasa sebagai kontrol realisasi logistik.", "#ecfeff", "#155e75",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPenerimaan(session); if (order) c.addOrder(Order.desc("tanggalPembuatan")); return c; } }),
				rendererPenerimaanDetail(), detailHeadersPenerimaan(), detailWidthsPenerimaan(), "Detail Penerimaan Pembelian");
	}

	private void renderRealisasiPermintaanDashboard(Component parent, final PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel("Monitoring Realisasi Detail PR", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:12px;'>"
				+ "Mengawasi detail permintaan pengadaan: apakah sudah direalisasikan melalui Uang Muka/Cash Advance, melalui PO, atau masih belum terealisasi.</div>");
		Div wrap = new Div();
		wrap.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(210px,1fr)); gap:12px;");
		wrap.setParent(pch);

		appendPressureCard(wrap, "Total Detail PR", d.permintaanDetailTotal, "Seluruh detail item PR pada filter aktif.", "#f8fafc", "#0f172a",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPermintaanDetail(session); if (order) c.addOrder(Order.desc("tanggalPembuatan")); return c; } }),
				rendererPermintaanDetail(), detailHeadersPermintaanDetail(), detailWidthsPermintaanDetail(), "Detail Seluruh Item PR");
		appendPressureCard(wrap, "Realisasi via Uang Muka", d.permintaanDetailUangMuka, "Detail PR yang memakai cash advance / uang muka.", "#ede9fe", "#5b21b6",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPermintaanDetailUangMuka(session); if (order) c.addOrder(Order.desc("tanggalPembuatan")); return c; } }),
				rendererPermintaanDetail(), detailHeadersPermintaanDetail(), detailWidthsPermintaanDetail(), "Detail PR Realisasi via Uang Muka");
		appendPressureCard(wrap, "Realisasi via PO", d.permintaanDetailPo, "Detail PR yang sudah terhubung ke pemesanan pembelian/PO.", "#dbeafe", "#1e40af",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPermintaanDetailPo(session); if (order) c.addOrder(Order.desc("tanggalPembuatan")); return c; } }),
				rendererPermintaanDetail(), detailHeadersPermintaanDetail(), detailWidthsPermintaanDetail(), "Detail PR Realisasi via PO");
		appendPressureCard(wrap, "Belum Realisasi", d.permintaanDetailBelumRealisasi, "Detail PR yang belum punya uang muka dan belum terhubung PO.", "#fee2e2", "#991b1b",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPermintaanDetailBelumRealisasi(session); if (order) c.addOrder(Order.asc("tanggalPembuatan")); return c; } }),
				rendererPermintaanDetail(), detailHeadersPermintaanDetail(), detailWidthsPermintaanDetail(), "Detail PR Belum Realisasi");
	}

	private void renderPenerimaanProcurementDashboard(Component parent, final PengadaanDashboardData d) {
		Panelchildren pch = createModernPanel("Kontrol Penerimaan Pembelian & Logistik", parent);
		appendHtml(pch, "<div style='font-size:12px; color:#64748b; margin-bottom:12px;'>"
				+ "membantu membandingkan PO/pembelian langsung terhadap penerimaan barang/jasa agar tim logistik dapat melihat bottleneck setelah pemesanan.</div>");
		Div wrap = new Div();
		wrap.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(210px,1fr)); gap:12px;");
		wrap.setParent(pch);

		appendClickableValueCard(wrap, "Penerimaan Pembelian", String.valueOf(d.penerimaanPembelian),
				"Dokumen penerimaan barang/jasa pada filter aktif.", "#ecfeff", "#155e75",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPenerimaan(session); if (order) c.addOrder(Order.desc("tanggalPembuatan")); return c; } }),
				rendererPenerimaanDetail(), detailHeadersPenerimaan(), detailWidthsPenerimaan(), "Detail Penerimaan Pembelian");
		appendClickableValueCard(wrap, "Rasio Detail PR Terealisasi", percent(d.permintaanDetailRealisasi, d.permintaanDetailTotal) + "%",
				"Persentase item PR yang sudah direalisasi via Uang Muka atau PO.", "#dcfce7", "#166534",
				detailProviderRealisasiPrSummary(d), rendererSummaryDetail(), detailHeadersSummary(), null, "Ringkasan Realisasi Detail PR");
		appendClickableValueCard(wrap, "Belum Realisasi", String.valueOf(d.permintaanDetailBelumRealisasi),
				"Prioritas follow-up: detail PR tanpa uang muka dan tanpa PO.", "#fff1f2", "#9f1239",
				detailProviderCriteria(new DetailCriteriaBuilder() { public Criteria build(Session session, boolean order) { Criteria c = criteriaPermintaanDetailBelumRealisasi(session); if (order) c.addOrder(Order.asc("tanggalPembuatan")); return c; } }),
				rendererPermintaanDetail(), detailHeadersPermintaanDetail(), detailWidthsPermintaanDetail(), "Detail PR Belum Realisasi");
	}

	private DetailDataProvider detailProviderRealisasiPrSummary(final PengadaanDashboardData d) {
		List list = new ArrayList();
		list.add(new SummaryDetailItem("Total Detail PR", d.permintaanDetailTotal, "Jumlah detail PermintaanPengadaanMasterAssetDetail pada filter aktif"));
		list.add(new SummaryDetailItem("Realisasi via Uang Muka", d.permintaanDetailUangMuka, "Detail dengan uangMuka != null"));
		list.add(new SummaryDetailItem("Realisasi via PO", d.permintaanDetailPo, "Detail dengan pemesananPengadaanMasterAssetDetail != null"));
		list.add(new SummaryDetailItem("Terealisasi", d.permintaanDetailRealisasi, "Detail dengan uang muka atau PO"));
		list.add(new SummaryDetailItem("Belum Realisasi", d.permintaanDetailBelumRealisasi, "Detail yang belum memiliki uang muka dan belum terhubung PO"));
		list.add(new SummaryDetailItem("Rasio Realisasi", percent(d.permintaanDetailRealisasi, d.permintaanDetailTotal) + "%", "Persentase item PR yang sudah diproses"));
		return detailProviderStaticList(list);
	}

	private void appendClickableValueCard(Component parent, String title, String value, String desc, String bg, String color,
			final DetailDataProvider provider, final DetailRowRenderer renderer, final String[] headers, final String[] widths,
			final String detailTitle) {
		Div card = new Div();
		card.setStyle("border-radius:16px; padding:14px; background:" + bg + "; border:1px solid rgba(15,23,42,.08); min-height:104px;");
		card.setParent(parent);
		A a = createClickableNumber(value, "font-size:26px; line-height:1.05; font-weight:900; color:" + color + "; text-decoration:none; cursor:pointer; display:block;", detailTitle, provider, renderer, headers, widths);
		a.setParent(card);
		appendHtml(card, "<div style='font-size:12px; font-weight:900; color:" + color + "; margin-top:9px;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:" + color + "; opacity:.82; margin-top:5px; line-height:1.45;'>" + escapeHtml(desc) + "</div>");
	}

	private DetailDataProvider detailProviderLogistikSummary(final PengadaanDashboardData d) {
		List list = new ArrayList();
		list.add(new SummaryDetailItem("Approval Rate PR", d.prApprovalRate + "%", d.prApproved + " PR disetujui dari " + d.totalPr + " total PR"));
		list.add(new SummaryDetailItem("Approval Rate PO", d.poApprovalRate + "%", d.poApproved + " PO disetujui dari " + d.totalPo + " total PO"));
		list.add(new SummaryDetailItem("Konversi PR ke PO", d.prToPoConversionRate + "%", d.totalPo + " PO dibanding " + d.prApproved + " PR disetujui"));
		list.add(new SummaryDetailItem("Konversi PO ke Pembayaran", d.paymentToPoRate + "%", d.paymentApproved + " pembayaran dibanding " + d.poApproved + " PO disetujui"));
		list.add(new SummaryDetailItem("Nilai Pending", formatNumber(d.nilaiTotalPending), "Gabungan nilai PR dan PO pending"));
		return detailProviderStaticList(list);
	}

	private void appendFunnelRow(Component parent, String label, int value, int max, String color,
			final DetailDataProvider provider, final DetailRowRenderer renderer, final String[] headers, final String[] widths,
			final String detailTitle) {
		int pct = max <= 0 ? 0 : (int) Math.round((value * 100.0) / max);
		if (pct < 4 && value > 0) {
			pct = 4;
		}
		Div row = new Div();
		row.setStyle("display:flex; align-items:center; gap:10px; margin:10px 0;");
		row.setParent(parent);
		appendHtml(row, "<div style='width:190px; font-size:12px; color:#334155; font-weight:700;'>" + escapeHtml(label) + "</div>"
				+ "<div style='flex:1; background:#e5e7eb; border-radius:999px; height:12px; overflow:hidden;'>"
				+ "<div style='height:12px; width:" + pct + "%; background:" + color + "; border-radius:999px;'></div></div>");
		A a = createClickableNumber(String.valueOf(value), "width:46px; text-align:right; font-size:13px; font-weight:800; color:#0f172a; text-decoration:none; cursor:pointer; display:block;", detailTitle, provider, renderer, headers, widths);
		a.setParent(row);
	}

	private void appendMiniGauge(Component parent, String title, int pct, String desc, String color,
			final DetailDataProvider provider, final DetailRowRenderer renderer, final String[] headers, final String[] widths,
			final String detailTitle) {
		if (pct < 0) {
			pct = 0;
		}
		if (pct > 100) {
			pct = 100;
		}
		Div box = new Div();
		box.setStyle("padding:8px 0; border-bottom:1px solid #e2e8f0;");
		box.setParent(parent);
		Div top = new Div();
		top.setStyle("display:flex; justify-content:space-between; gap:10px; align-items:center;");
		top.setParent(box);
		appendHtml(top, "<div><div style='font-size:12px; font-weight:800; color:#0f172a;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:#64748b; margin-top:2px;'>" + escapeHtml(desc) + "</div></div>");
		A a = createClickableNumber(pct + "%", "font-size:13px; font-weight:900; color:#0f172a; text-decoration:none; cursor:pointer;", detailTitle, provider, renderer, headers, widths);
		a.setParent(top);
		appendHtml(box, "<div style='margin-top:7px; height:9px; border-radius:999px; background:#e2e8f0; overflow:hidden;'>"
				+ "<div style='height:9px; width:" + pct + "%; border-radius:999px; background:" + color + ";'></div></div>");
	}

	private void appendCoverageRow(Component parent, String label, int adaSop, int total, String color,
			final DetailDataProvider provider, final DetailRowRenderer renderer, final String[] headers, final String[] widths,
			final String detailTitle) {
		int pct = percent(adaSop, total);
		Div row = new Div();
		row.setStyle("display:flex; align-items:center; gap:10px; margin:10px 0;");
		row.setParent(parent);
		appendHtml(row, "<div style='width:160px; font-size:12px; color:#334155; font-weight:800;'>" + escapeHtml(label) + "</div>"
				+ "<div style='flex:1; background:#e5e7eb; border-radius:999px; height:12px; overflow:hidden;'>"
				+ "<div style='height:12px; width:" + pct + "%; background:" + color + "; border-radius:999px;'></div></div>");
		A a = createClickableNumber(adaSop + "/" + total + " (" + pct + "%)", "width:120px; text-align:right; font-size:12px; font-weight:800; color:#0f172a; text-decoration:none; cursor:pointer; display:block;", detailTitle, provider, renderer, headers, widths);
		a.setParent(row);
	}

	private void appendPressureCard(Component parent, String title, int value, String desc, String bg, String color,
			final DetailDataProvider provider, final DetailRowRenderer renderer, final String[] headers, final String[] widths,
			final String detailTitle) {
		Div card = new Div();
		card.setStyle("border-radius:14px; padding:13px; background:" + bg + "; border:1px solid rgba(15,23,42,.08);");
		card.setParent(parent);
		A a = createClickableNumber(String.valueOf(value), "font-size:25px; line-height:1; font-weight:900; color:" + color + "; text-decoration:none; cursor:pointer; display:block;", detailTitle, provider, renderer, headers, widths);
		a.setParent(card);
		appendHtml(card, "<div style='font-size:12px; font-weight:900; color:" + color + "; margin-top:8px;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:" + color + "; opacity:.82; margin-top:5px;'>" + escapeHtml(desc) + "</div>");
	}

	private DetailDataProvider detailProviderHealthSummary(final PengadaanDashboardData d, final int pendingRisk,
			final int rejectionRisk, final int healthScore, final String status) {
		List list = new ArrayList();
		list.add(new SummaryDetailItem("Health Score", healthScore, status));
		list.add(new SummaryDetailItem("Pending Aging > 7 Hari", pendingRisk + "%", d.totalPendingAging7 + " dari " + d.totalPending + " PR/PO pending"));
		list.add(new SummaryDetailItem("Risiko Penolakan 30 Hari", rejectionRisk + "%", (d.prRejected30 + d.poRejected30) + " dokumen ditolak 30 hari terakhir"));
		list.add(new SummaryDetailItem("Coverage SOP", d.coverageSopPercent + "%", d.totalAdaSop + " dari " + d.totalDokumen + " dokumen memiliki SOP"));
		return detailProviderStaticList(list);
	}

	private DetailDataProvider detailProviderAgingPrPo() {
		return new DetailDataProvider() {
			@Override
			public int count(Session session) throws Exception {
				Date batas = dateOffset(-7);
				return (int) (countCriteria(criteriaPrPending(session).add(Restrictions.le("tanggalPembuatan", batas)))
						+ countCriteria(criteriaPoPending(session).add(Restrictions.le("tanggalPembuatan", batas))));
			}

			@SuppressWarnings("unchecked")
			@Override
			public List list(Session session, int firstResult, int maxResults) throws Exception {
				List result = new ArrayList();
				Date batas = dateOffset(-7);
				int prCount = (int) countCriteria(criteriaPrPending(session).add(Restrictions.le("tanggalPembuatan", batas)));
				if (firstResult < prCount) {
					List<PermintaanPengadaanMasterAsset> prs = criteriaPrPending(session).add(Restrictions.le("tanggalPembuatan", batas)).addOrder(Order.asc("tanggalPembuatan"))
							.setFirstResult(firstResult).setMaxResults(maxResults).list();
					for (PermintaanPengadaanMasterAsset pr : prs) {
						result.add(toQueueItem("PR", pr));
					}
				}
				if (result.size() < maxResults) {
					int poFirst = firstResult < prCount ? 0 : firstResult - prCount;
					List<PemesananPengadaanMasterAsset> pos = criteriaPoPending(session).add(Restrictions.le("tanggalPembuatan", batas)).addOrder(Order.asc("tanggalPembuatan"))
							.setFirstResult(poFirst).setMaxResults(maxResults - result.size()).list();
					for (PemesananPengadaanMasterAsset po : pos) {
						result.add(toQueueItem("PO", po));
					}
				}
				return result;
			}
		};
	}

	private DetailDataProvider detailProviderRejected30PrPo() {
		return new DetailDataProvider() {
			@Override
			public int count(Session session) throws Exception {
				Date batas = dateOffset(-30);
				return (int) (countCriteria(criteriaPrRejected(session).add(Restrictions.ge("tanggalDitolak", batas)))
						+ countCriteria(criteriaPoRejected(session).add(Restrictions.ge("tanggalDitolak", batas))));
			}

			@SuppressWarnings("unchecked")
			@Override
			public List list(Session session, int firstResult, int maxResults) throws Exception {
				List result = new ArrayList();
				Date batas = dateOffset(-30);
				int prCount = (int) countCriteria(criteriaPrRejected(session).add(Restrictions.ge("tanggalDitolak", batas)));
				if (firstResult < prCount) {
					List<PermintaanPengadaanMasterAsset> prs = criteriaPrRejected(session).add(Restrictions.ge("tanggalDitolak", batas)).addOrder(Order.desc("tanggalDitolak"))
							.setFirstResult(firstResult).setMaxResults(maxResults).list();
					for (PermintaanPengadaanMasterAsset pr : prs) {
						ProcurementItem item = toQueueItem("PR", pr);
						item.status = "Ditolak";
						result.add(item);
					}
				}
				if (result.size() < maxResults) {
					int poFirst = firstResult < prCount ? 0 : firstResult - prCount;
					List<PemesananPengadaanMasterAsset> pos = criteriaPoRejected(session).add(Restrictions.ge("tanggalDitolak", batas)).addOrder(Order.desc("tanggalDitolak"))
							.setFirstResult(poFirst).setMaxResults(maxResults - result.size()).list();
					for (PemesananPengadaanMasterAsset po : pos) {
						ProcurementItem item = toQueueItem("PO", po);
						item.status = "Ditolak";
						result.add(item);
					}
				}
				return result;
			}
		};
	}

	private String[] detailHeadersPembayaranMaster() {
		return new String[] { "Kode Bayar", "Tgl Persetujuan", "Penyedia", "Disetujui Oleh", "Status SOP" };
	}

	private String[] detailWidthsPembayaranMaster() {
		return new String[] { "18%", "16%", "24%", "20%", null };
	}

	private DetailRowRenderer rendererPembayaranMasterDetail() {
		return new DetailRowRenderer() {
			@Override
			public void render(Row row, Object data) throws Exception {
				PembayaranPengadaanMasterAsset bayar = (PembayaranPengadaanMasterAsset) data;
				row.appendChild(new Label(safeString(bayar.getKode())));
				row.appendChild(new Label(safeDate(bayar.getTanggalPersetujuan())));
				row.appendChild(new Label(bayar.getPenyedia() == null ? "-" : safeString(bayar.getPenyedia().getNama())));
				row.appendChild(new Label(bayar.getDisetujuiOleh() == null ? "-" : safeString(bayar.getDisetujuiOleh().getUserNama())));
				row.appendChild(new Label(bayar.getDisposisiSop() == null ? "Belum ada SOP" : "Ada SOP"));
			}
		};
	}

	private Date dateOffset(int days) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, days);
		return cal.getTime();
	}

	private String buildFunnelRow(String label, int value, int max, String color) {
		int pct = max <= 0 ? 0 : (int) Math.round((value * 100.0) / max);
		if (pct < 4 && value > 0) {
			pct = 4;
		}
		return "<div style='display:flex; align-items:center; gap:10px; margin:10px 0;'>"
				+ "<div style='width:190px; font-size:12px; color:#334155; font-weight:700;'>" + escapeHtml(label) + "</div>"
				+ "<div style='flex:1; background:#e5e7eb; border-radius:999px; height:12px; overflow:hidden;'>"
				+ "<div style='height:12px; width:" + pct + "%; background:" + color + "; border-radius:999px;'></div></div>"
				+ "<div style='width:46px; text-align:right; font-size:13px; font-weight:800; color:#0f172a;'>" + value + "</div></div>";
	}

	private String buildMiniGauge(String title, int pct, String desc, String color) {
		if (pct < 0) {
			pct = 0;
		}
		if (pct > 100) {
			pct = 100;
		}
		return "<div style='padding:8px 0; border-bottom:1px solid #e2e8f0;'>"
				+ "<div style='display:flex; justify-content:space-between; gap:10px; align-items:center;'>"
				+ "<div><div style='font-size:12px; font-weight:800; color:#0f172a;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:#64748b; margin-top:2px;'>" + escapeHtml(desc) + "</div></div>"
				+ "<div style='font-size:13px; font-weight:900; color:#0f172a;'>" + pct + "%</div></div>"
				+ "<div style='margin-top:7px; height:9px; border-radius:999px; background:#e2e8f0; overflow:hidden;'>"
				+ "<div style='height:9px; width:" + pct + "%; border-radius:999px; background:" + color + ";'></div></div></div>";
	}

	private String buildCoverageRow(String label, int adaSop, int total, String color) {
		int pct = percent(adaSop, total);
		return "<div style='display:flex; align-items:center; gap:10px; margin:10px 0;'>"
				+ "<div style='width:160px; font-size:12px; color:#334155; font-weight:800;'>" + escapeHtml(label) + "</div>"
				+ "<div style='flex:1; background:#e5e7eb; border-radius:999px; height:12px; overflow:hidden;'>"
				+ "<div style='height:12px; width:" + pct + "%; background:" + color + "; border-radius:999px;'></div></div>"
				+ "<div style='width:110px; text-align:right; font-size:12px; font-weight:800; color:#0f172a;'>" + adaSop + "/" + total + " (" + pct + "%)</div></div>";
	}

	private String buildPressureCard(String title, int value, String desc, String bg, String color) {
		return "<div style='border-radius:14px; padding:13px; background:" + bg + "; border:1px solid rgba(15,23,42,.08);'>"
				+ "<div style='font-size:25px; line-height:1; font-weight:900; color:" + color + ";'>" + value + "</div>"
				+ "<div style='font-size:12px; font-weight:900; color:" + color + "; margin-top:8px;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:11px; color:" + color + "; opacity:.82; margin-top:5px;'>" + escapeHtml(desc) + "</div></div>";
	}

	private String buildActionPlanCard(String no, String title, String desc, String bg, String color) {
		return "<div style='border-radius:16px; padding:14px; background:" + bg + "; border:1px solid rgba(15,23,42,.08); min-height:105px;'>"
				+ "<div style='display:flex; justify-content:space-between; gap:12px; align-items:center;'>"
				+ "<div style='font-size:12px; font-weight:900; color:" + color + ";'>" + escapeHtml(title) + "</div>"
				+ "<div style='width:28px; height:28px; border-radius:999px; background:#ffffff; color:" + color + "; display:flex; align-items:center; justify-content:center; font-weight:900;'>" + escapeHtml(no) + "</div></div>"
				+ "<div style='font-size:12px; color:" + color + "; line-height:1.45; margin-top:10px;'>" + escapeHtml(desc) + "</div></div>";
	}

	private Panelchildren createModernPanel(String title, Component parent) {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setTitle(title);
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setMinimizable(false);
		panel.setStyle("margin-bottom:12px; border:1px solid #e5e7eb; border-radius:16px; overflow:hidden;"
				+ "background:#ffffff; box-shadow:0 12px 24px rgba(15,23,42,.07);");
		panel.setParent(parent);

		Panelchildren pch = new Panelchildren();
		pch.setStyle("padding:14px; background:#ffffff;");
		pch.setParent(panel);
		appendPanelDescriptionEndUserV27(pch, title);
		return pch;
	}

	private void appendHtml(Component parent, String html) {
		org.zkoss.zul.Html h = new org.zkoss.zul.Html(html);
		h.setParent(parent);
	}

	private void appendEmptyState(Component parent, String message) {
		appendHtml(parent, "<div style='padding:18px; border-radius:14px; background:#f8fafc; border:1px dashed #cbd5e1;"
				+ "text-align:center; color:#64748b; font-size:12px;'>" + escapeHtml(message) + "</div>");
	}

	private int getMaxValue(int[] values) {
		int max = 0;
		if (values == null) {
			return max;
		}
		for (int i = 0; i < values.length; i++) {
			if (values[i] > max) {
				max = values[i];
			}
		}
		return max;
	}

	private int percent(int value, int total) {
		if (total <= 0 || value <= 0) {
			return 0;
		}
		return (int) Math.round((value * 100.0) / total);
	}

	private Criteria criteriaPermintaanAktif(Session session) {
		Criteria c = session.createCriteria(PermintaanPengadaanMasterAsset.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		applyGlobalPengadaanFilter(c, "tanggalPembuatan", "satuanKerja", "keterangan", "kode");
		return c;
	}

	private Criteria criteriaPrPending(Session session) {
		return criteriaPermintaanAktif(session).add(Restrictions.isNull("disetujuiOleh")).add(Restrictions.isNull("ditolakOleh"));
	}

	private Criteria criteriaPrApproved(Session session) {
		return criteriaPermintaanAktif(session).add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.isNotNull("tanggalPersetujuan"));
	}

	private Criteria criteriaPrRejected(Session session) {
		return criteriaPermintaanAktif(session).add(Restrictions.isNotNull("ditolakOleh")).add(Restrictions.isNotNull("tanggalDitolak"));
	}

	private Criteria criteriaPemesanan(Session session) {
		Criteria c = session.createCriteria(PemesananPengadaanMasterAsset.class);
		applyGlobalPengadaanFilter(c, "tanggalPembuatan", "satuanKerja", "keterangan", "kode");
		return c;
	}

	private Criteria criteriaPoPending(Session session) {
		return criteriaPemesanan(session).add(Restrictions.isNull("disetujuiOleh")).add(Restrictions.isNull("ditolakOleh"));
	}

	private Criteria criteriaPoApproved(Session session) {
		return criteriaPemesanan(session).add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.isNotNull("tanggalPersetujuan"));
	}

	private Criteria criteriaPoRejected(Session session) {
		return criteriaPemesanan(session).add(Restrictions.isNotNull("ditolakOleh")).add(Restrictions.isNotNull("tanggalDitolak"));
	}

	private Criteria criteriaPembelianLangsung(Session session) {
		return criteriaPemesanan(session).add(Restrictions.eq("pembelianLangsung", Boolean.TRUE));
	}

	private Criteria criteriaPenerimaan(Session session) {
		Criteria c = session.createCriteria(PenerimaanPengadaanMasterAsset.class)
				.createAlias("pemesananPengadaanMasterAsset", "pemesananPengadaanMasterAsset", Criteria.LEFT_JOIN);
		applyGlobalDateRange(c, "tanggalPembuatan");
		if (dashboardGlobalSatuanKerjaV9 != null) {
			c.add(Restrictions.or(Restrictions.eq("satuanKerja", dashboardGlobalSatuanKerjaV9),
					Restrictions.eq("pemesananPengadaanMasterAsset.satuanKerja", dashboardGlobalSatuanKerjaV9)));
		}
		String keyword = getGlobalKeywordV9();
		if (keyword != null && !keyword.trim().isEmpty()) {
			c.add(Restrictions.or(Restrictions.ilike("kode", keyword, MatchMode.ANYWHERE),
					Restrictions.or(Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE),
							Restrictions.or(Restrictions.ilike("kodeTagihan", keyword, MatchMode.ANYWHERE),
									Restrictions.ilike("pemesananPengadaanMasterAsset.kode", keyword, MatchMode.ANYWHERE)))));
		}
		return c;
	}

	private Criteria criteriaPermintaanDetail(Session session) {
		Criteria c = session.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
				.createAlias("permintaanPengadaanMasterAsset", "permintaanPengadaanMasterAsset", Criteria.LEFT_JOIN)
				.createAlias("masterAsset", "masterAsset", Criteria.LEFT_JOIN);
		applyGlobalDateRange(c, "tanggalPembuatan");
		applyGlobalSatker(c, "satuanKerja");
		String keyword = getGlobalKeywordV9();
		if (keyword != null && !keyword.trim().isEmpty()) {
			c.add(Restrictions.or(Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE),
					Restrictions.or(Restrictions.ilike("masterAsset.nama", keyword, MatchMode.ANYWHERE),
							Restrictions.or(Restrictions.ilike("permintaanPengadaanMasterAsset.kode", keyword, MatchMode.ANYWHERE),
									Restrictions.ilike("permintaanPengadaanMasterAsset.keterangan", keyword, MatchMode.ANYWHERE)))));
		}
		return c;
	}

	private Criteria criteriaPermintaanDetailUangMuka(Session session) {
		return criteriaPermintaanDetail(session).add(Restrictions.isNotNull("uangMuka"));
	}

	private Criteria criteriaPermintaanDetailPo(Session session) {
		return criteriaPermintaanDetail(session).add(Restrictions.isNotNull("pemesananPengadaanMasterAssetDetail"));
	}

	private Criteria criteriaPermintaanDetailRealisasi(Session session) {
		return criteriaPermintaanDetail(session).add(Restrictions.or(Restrictions.isNotNull("uangMuka"),
				Restrictions.isNotNull("pemesananPengadaanMasterAssetDetail")));
	}

	private Criteria criteriaPermintaanDetailBelumRealisasi(Session session) {
		return criteriaPermintaanDetail(session).add(Restrictions.isNull("uangMuka"))
				.add(Restrictions.isNull("pemesananPengadaanMasterAssetDetail"));
	}

	private Criteria criteriaPembayaranDetailDisetujui(Session session) {
		Criteria c = session.createCriteria(PembayaranPengadaanMasterAssetDetail.class)
				.createAlias("pembayaranPengadaanMasterAsset", "pembayaranPengadaanMasterAsset")
				.createAlias("penerimaanPengadaanMasterAsset", "penerimaanPengadaanMasterAsset", Criteria.LEFT_JOIN)
				.createAlias("penerimaanPengadaanMasterAsset.pemesananPengadaanMasterAsset", "pemesananPengadaanMasterAsset", Criteria.LEFT_JOIN)
				.add(Restrictions.isNotNull("pembayaranPengadaanMasterAsset.disetujuiOleh"))
				.add(Restrictions.isNotNull("pembayaranPengadaanMasterAsset.tanggalPersetujuan"));
		applyGlobalDateRange(c, "pembayaranPengadaanMasterAsset.tanggalPersetujuan");
		applyGlobalSatker(c, "pemesananPengadaanMasterAsset.satuanKerja");
		String keyword = getGlobalKeywordV9();
		if (keyword != null && !keyword.trim().isEmpty()) {
			c.add(Restrictions.or(Restrictions.ilike("pemesananPengadaanMasterAsset.keterangan", keyword, MatchMode.ANYWHERE),
					Restrictions.ilike("pemesananPengadaanMasterAsset.kode", keyword, MatchMode.ANYWHERE)));
		}
		return c;
	}

	private Criteria criteriaPembayaranMaster(Session session) {
		Criteria c = session.createCriteria(PembayaranPengadaanMasterAsset.class);
		applyGlobalDateRange(c, "tanggalPersetujuan");
		applyGlobalKeyword(c, "keterangan", "kode");
		return c;
	}

	private Criteria criteriaPks(Session session) {
		Criteria c = session.createCriteria(PerjanjianKerjasamaMasterAsset.class);
		applyGlobalPengadaanFilter(c, "tanggalPembuatan", "satuanKerja", "keterangan", "kode");
		return c;
	}

	private Criteria criteriaPksDisetujui(Session session) {
		return criteriaPks(session).add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.isNotNull("tanggalPersetujuan"));
	}


	private Criteria criteriaPrPendingAge(Session session, int minAgeInclusive, int maxAgeInclusive) {
		Criteria c = criteriaPrPending(session);
		applyAgingRange(c, "tanggalPembuatan", minAgeInclusive, maxAgeInclusive);
		return c;
	}

	private Criteria criteriaPoPendingAge(Session session, int minAgeInclusive, int maxAgeInclusive) {
		Criteria c = criteriaPoPending(session);
		applyAgingRange(c, "tanggalPembuatan", minAgeInclusive, maxAgeInclusive);
		return c;
	}

	private void applyAgingRange(Criteria criteria, String fieldName, int minAgeInclusive, int maxAgeInclusive) {
		if (criteria == null || fieldName == null) {
			return;
		}
		if (maxAgeInclusive >= 0) {
			criteria.add(Restrictions.ge(fieldName, dateOffset(-maxAgeInclusive)));
		}
		if (minAgeInclusive > 0) {
			criteria.add(Restrictions.lt(fieldName, dateOffset(-(minAgeInclusive - 1))));
		}
	}

	private long countCriteria(Criteria criteria) {
		Object result = criteria.setProjection(Projections.rowCount()).uniqueResult();
		return toLong(result);
	}

	private Number sumCriteria(Criteria criteria, String propertyName) {
		Object result = criteria.setProjection(Projections.sum(propertyName)).uniqueResult();
		return toNumber(result);
	}

	private String safeString(Object value) {
		if (value == null) {
			return "-";
		}
		String text = String.valueOf(value).trim();
		return text.length() == 0 || "null".equalsIgnoreCase(text) ? "-" : text;
	}

	private String safeDate(Date date) {
		return date == null ? "-" : Common.dateFormat.get().format(date);
	}

	private String formatNumber(Number value) {
		if (value == null) {
			return "0";
		}
		return Common.numberFormat.get().format(value.doubleValue());
	}

	private long toLong(Object value) {
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		return 0L;
	}

	private Number toNumber(Object value) {
		return value instanceof Number ? (Number) value : Double.valueOf(0.0);
	}

	private String labelFromEntity(Object entity) {
		if (entity == null) {
			return "Tidak Diketahui / Kosong";
		}
		try {
			Object nama = entity.getClass().getMethod("getNama").invoke(entity);
			return safeString(nama);
		} catch (Exception e) {
			return safeString(entity);
		}
	}

	private String escapeHtml(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}

	private void closeSessionSafely(Session session) {
		ais.ui.util.DashboardModernHtmlUtil.closeOpenedSession(session);
	}

	private class PengadaanDashboardData {
		int prPending;
		int prApproved;
		int prRejected;
		int poPending;
		int poApproved;
		int poRejected;
		int paymentApproved;
		int pksApproved;
		int pembelianLangsung;
		int penerimaanPembelian;
		int permintaanDetailTotal;
		int permintaanDetailUangMuka;
		int permintaanDetailPo;
		int permintaanDetailRealisasi;
		int permintaanDetailBelumRealisasi;
		int totalPr;
		int totalPo;
		int totalPembayaranMaster;
		int totalPks;
		int totalDokumen;
		int prAdaSop;
		int poAdaSop;
		int pembayaranAdaSop;
		int pksAdaSop;
		int totalAdaSop;
		int coverageSopPercent;
		int totalPending;
		int prPendingAging7;
		int poPendingAging7;
		int totalPendingAging7;
		int prPendingAge03;
		int prPendingAge47;
		int prPendingAgeOver7;
		int poPendingAge03;
		int poPendingAge47;
		int poPendingAgeOver7;
		int prApprovalRate;
		int poApprovalRate;
		int prToPoConversionRate;
		int paymentToPoRate;
		int prRejected30;
		int poRejected30;
		Number nilaiPrPending = Double.valueOf(0.0);
		Number nilaiPoPending = Double.valueOf(0.0);
		Number nilaiPembayaranApproved = Double.valueOf(0.0);
		Number nilaiPksApproved = Double.valueOf(0.0);
		Number nilaiPembelianLangsung = Double.valueOf(0.0);
		Number nilaiTotalPending = Double.valueOf(0.0);
		List<ProcurementItem> topPrPending = new ArrayList<ProcurementItem>();
		List<ProcurementItem> topPoPending = new ArrayList<ProcurementItem>();
		List<GroupSummaryItem> sebaranSatker = new ArrayList<GroupSummaryItem>();
		List<GroupSummaryItem> sebaranPenyediaPembayaran = new ArrayList<GroupSummaryItem>();
	}

	private class ProcurementItem {
		String jenis;
		String kode;
		String tanggal;
		String pihak;
		String status;
		String keterangan;
		Number nilai = Double.valueOf(0.0);
	}

	private class SummaryDetailItem {
		String label;
		String value;
		String description;

		SummaryDetailItem(String label, Object value, String description) {
			this.label = safeString(label);
			this.value = value == null ? "0" : safeString(value);
			this.description = safeString(description);
		}
	}

	private class GroupSummaryItem {
		String label;
		int count;
		Number value = Double.valueOf(0.0);
	}

	// =======================================================================================
	// HELPER CLASSES & METHODS FOR HIGH EFFICIENCY
	// =======================================================================================

	private abstract class PanelConfig<T> {
		public abstract String getTitle();

		public abstract String[] getHeaders();

		public abstract String[] getWidths();

		public abstract String[] getAligns();

		public abstract Criteria buildCriteria(Session session, String keyword, Object satker, boolean isOrder);

		public abstract void renderRow(Row row, T data) throws Exception;

		// Common filter for standard models
		protected void applyCommonCriteria(Criteria criteria, String keyword, Object satker, boolean isOrder) {
			applyGlobalDateRange(criteria, "tanggalPembuatan");
			criteria.add(satker == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("satuanKerja", satker));
			criteria.add(keyword == null || keyword.isEmpty() ? Restrictions.sqlRestriction("true")
					: Restrictions.or(Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE),
							Restrictions.ilike("kode", keyword, MatchMode.ANYWHERE)));
			if (isOrder) {
				criteria.addOrder(Order.desc("id"));
			}
		}
	}

	private <T> void buildDashboardPanel(final PanelConfig<T> config) throws Exception {
		MyPortalchildren portalchildren = new MyPortalchildren();
		portalchildren.setParent(getCurrentDashboardPortalParent());
		portalchildren.setWidth(Common.isMobile() ? "100%" : "50%");
		portalchildren.setStyle("padding:6px; box-sizing:border-box;");

		Panel panel = new ais.ui.util.MyPanelConfig();
		portalchildren.appendChild(panel);
		panel.setTitle(Common.getBahasaConfig(config.getTitle()));
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setMinimizable(false);
		panel.addEventListener("onMove", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				MoveEvent moveEvent = (MoveEvent) arg0;

			}
		});
		panel.setStyle("margin-bottom:12px; border:1px solid #e5e7eb; border-radius:16px; overflow:hidden;"
				+ "background:#ffffff; box-shadow:0 12px 24px rgba(15,23,42,.07);");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setStyle("background:#ffffff; padding:10px;");
		panelchildren.setParent(panel);

		Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
		rowUtamapalingAwal.getGrid().setSclass("dgrid");

		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("border:0; background:#f8fafc; border-radius:14px; padding:8px; margin-bottom:8px;"
				+ "box-shadow:inset 0 0 0 1px #e5e7eb;");
		toolbar.setParent(rowUtamapalingAwal);

		new MyLabelAgakKecil("Satker:").setParent(toolbar);
		final AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox = new AmbilDataSatuanKerjaBanbox();
		ambilDataSatuanKerjaBanbox.setCols(7);
		ambilDataSatuanKerjaBanbox.setReadonly(true);
		ambilDataSatuanKerjaBanbox.setStyle("border-radius:8px;");
		if (getGlobalSatuanKerjaV9() != null) {
			ambilDataSatuanKerjaBanbox.setValue(getGlobalSatuanKerjaV9().getNama());
			ambilDataSatuanKerjaBanbox.setAttribute("satuanKerja", getGlobalSatuanKerjaV9());
		}
		ambilDataSatuanKerjaBanbox.setParent(toolbar);

		new MyLabelAgakKecil("Tgl:").setParent(toolbar);
		final MyDatebox searchMulai = new MyDatebox(getGlobalMulaiV9(null));
		searchMulai.setCols(5);
		searchMulai.setReadonly(true);
		searchMulai.setStyle("border-radius:8px;");
		searchMulai.setParent(toolbar);
		new MyLabelAgakKecil("sd").setParent(toolbar);
		final MyDatebox searchSampai = new MyDatebox(getGlobalSampaiV9(null));
		searchSampai.setCols(5);
		searchSampai.setReadonly(true);
		searchSampai.setStyle("border-radius:8px;");
		searchSampai.setParent(toolbar);

		new MyLabelAgakKecil("Cari:").setParent(toolbar);
		final Textbox cari = new Textbox();
		cari.setCols(5);
		cari.setValue(getGlobalKeywordV9());
		cari.setStyle("border-radius:8px;");
		cari.setParent(toolbar);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh.svg");
		refresh.setTooltiptext("Refresh");
		refresh.setStyle("font-weight:bold; color:#ffffff; background:#2563eb; border-radius:10px; padding:6px 12px;");
		refresh.setParent(toolbar);

		final Paging paging = new Paging();
		Row rowUtama = new Row();
		rowUtama.setParent(rowUtamapalingAwal.getParent());
		rowUtama.appendChild(paging);

		Row rowUtamaData = new Row();
		rowUtamaData.setParent(rowUtamapalingAwal.getParent());

		// MEMORY FIX: Create the Grid only ONCE
		final Grid grid = new Grid();
		grid.setSclass("dgrid fgrid");
		grid.setStyle("min-height:100px; border:0; background:transparent; border-radius:14px;");
		grid.setMold("paging");
		grid.setPageSize(10);
		if (grid.getPagingChild() != null) {
			grid.getPagingChild().setMold("os");
		}
		grid.setParent(rowUtamaData);

		Columns columns = new Columns();
		columns.setParent(grid);

		final String[] headers = config.getHeaders();
		String[] widths = config.getWidths();
		String[] aligns = config.getAligns();

		for (int i = 0; i < headers.length; i++) {
			Column column = new MyColumnConfig(headers[i]);
			if (widths != null && i < widths.length && widths[i] != null)
				column.setWidth(widths[i]);
			if (aligns != null && i < aligns.length && aligns[i] != null)
				column.setAlign(aligns[i]);
			columns.appendChild(column);
		}

		final Rows rows = new Rows();
		rows.setParent(grid);

		EventListener dataSearchDefault = new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = null;
				try {
					session = HibernateUtil.currentNativeSession();
					final Session finalSession = session;

					// MEMORY FIX: Only clear the specific rows, DO NOT destroy the Grid.
					rows.getChildren().clear();

					final String keyword = cari.getValue().trim();
					final Object satker = ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja");
					setGlobalFilterPengadaanV9(searchMulai.getValue(), searchSampai.getValue(),
							(ais.database.model.rab.SatuanKerja) satker, keyword);

					DataCriteria dataCriteria = new DataCriteria() {
						@Override
						public Criteria initCriteria(boolean order) {
							// Using inner session getter ensuring safe thread access
							return config.buildCriteria(finalSession, keyword, satker, order);
						}
					};

					Common.initPaging5((Criteria) dataCriteria.initCriteria(false), paging);

					List<T> resultsList = ((Criteria) dataCriteria.initCriteria(true))
							.setFirstResult(5 * ((paging == null ? 0 : paging.getActivePage()))).setMaxResults(5)
							.list();

					if (resultsList == null || resultsList.isEmpty()) {
						Row rowKosong = new Row();
						rowKosong.setParent(rows);
						rowKosong.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak ada data")));
						for (int i = 1; i < headers.length; i++) {
							rowKosong.appendChild(new Label("-"));
						}
						return;
					}

					for (T data : resultsList) {
						Row row = new Row();
						row.setParent(rows);
						config.renderRow(row, data);
					}
				} catch (Exception e) {
					debugError("Gagal render panel " + config.getTitle(), e);
				} finally {
					closeSessionSafely(session);
				}
			}
		};

		Common.initPaging5(paging, dataSearchDefault);
		dataSearchDefault.onEvent(null); // Load initially

		refresh.addEventListener("onClick", dataSearchDefault);
		cari.addEventListener("onOK", dataSearchDefault);
		searchMulai.addEventListener("onChange", dataSearchDefault);
		searchSampai.addEventListener("onChange", dataSearchDefault);
		ambilDataSatuanKerjaBanbox.setEventListener(dataSearchDefault);
	}

	// Helper Renderer Methods to avoid further repetition
	private void renderPermintaanPengadaanBase(Row row, PermintaanPengadaanMasterAsset data) throws Exception {
		Vbox a = RevisiHelper.createNewRevisi(PermintaanPengadaanMasterAsset.class, data, data.getKode());
		a.setParent(row);

		Hbox hbox = new Hbox();
		hbox.setParent(a);
		LampiranLain.createDownloadUploadFileLain(hbox, data.getId(), PermintaanPengadaanMasterAsset.class.getName(),
				"Lampiran", false, null, null, false, false, false, false);

		if (data.getWorkspace() != null) {
			new MyLabelAgakKecil(data.getWorkspace().getKode() + "-" + data.getWorkspace().getNama()).setParent(a);
		}
		if (data.getPemesananPengadaanMasterAsset() != null) {
			new MyLabelAgakKecil(data.getPemesananPengadaanMasterAsset().getKode()).setParent(a);
		}
	}

	private void renderPermintaanAction(Row row, final PermintaanPengadaanMasterAsset data) {
		Hbox hboxAction = new Hbox();
		hboxAction.setParent(row);

		MyToolbarbuttonConfig btnPrint = new MyToolbarbuttonConfig("", "/img/svg/printer.svg");
		btnPrint.setTooltiptext("Cetak Data");
		btnPrint.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				PermintaanPengadaanMasterAssetAction.cetak(data);
			}
		});
		btnPrint.setParent(hboxAction);

		if (data.getDisposisiSop() != null) {
			MyToolbarbuttonConfig btnSop = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
			btnSop.setTooltiptext("Lihat Alur SOP");
			btnSop.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					TampilanAlurSopAction.prosess(data.getDisposisiSop().getId(), null, null, true, event.getTarget());
				}
			});
			btnSop.setParent(hboxAction);
		}
	}

	private void renderPemesananPengadaanBase(Row row, PemesananPengadaanMasterAsset data) throws Exception {
		Vbox a = RevisiHelper.createNewRevisi(PemesananPengadaanMasterAsset.class, data, data.getKode());
		a.setParent(row);

		a.appendChild(new MyLabelAgakKecil(data.getPenyedia() != null ? data.getPenyedia().getNama() : ""));
		a.appendChild(new MyLabelAgakKecil(
				data.getJenisPemesananPengadaanAsset() != null ? data.getJenisPemesananPengadaanAsset().getNama()
						: ""));
		a.appendChild(new MyLabelAgakKecil(data.getKodeInvoice() != null ? data.getKodeInvoice() : ""));
		a.appendChild(new MyLabelAgakKecil(
				data.getPerjanjianKerjasamaMasterAsset() != null ? data.getPerjanjianKerjasamaMasterAsset().getKode()
						: ""));
	}

	private void renderPemesananAction(Row row, final PemesananPengadaanMasterAsset data) {
		Hbox hboxAction = new Hbox();
		hboxAction.setParent(row);

		MyToolbarbuttonConfig btnPrint = new MyToolbarbuttonConfig("", "/img/svg/printer.svg");
		btnPrint.setTooltiptext("Cetak Data");
		btnPrint.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				PemesananPengadaanMasterAssetAction.cetak(data);
			}
		});
		btnPrint.setParent(hboxAction);

		if (data.getDisposisiSop() != null) {
			MyToolbarbuttonConfig btnSop = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
			btnSop.setTooltiptext("Lihat Alur SOP");
			btnSop.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					TampilanAlurSopAction.prosess(data.getDisposisiSop().getId(), null, null, true, event.getTarget());
				}
			});
			btnSop.setParent(hboxAction);
		}
	}

	private void appendPanelDescriptionEndUserV27(Panelchildren parent, String title) {
		if (parent == null) {
			return;
		}
		String desc = dashboardPanelDescriptionEndUserV27(title);
		if (desc == null || desc.trim().length() == 0) {
			return;
		}
		org.zkoss.zul.Html html = new org.zkoss.zul.Html("<div style=\"margin:0 0 12px 0; padding:10px 12px; "
				+ "border-radius:12px; background:#f8fafc; border:1px solid #e2e8f0; color:#475569; "
				+ "font-size:11.5px; line-height:1.55;\">"
				+ "<b style=\"color:#0f172a;\"></b> " + safeDashboardHtmlV27(desc) + "</div>");
		html.setParent(parent);
	}

	private String dashboardPanelDescriptionEndUserV27(String title) {
		if (title == null) {
			return "membantu menampilkan ringkasan data agar pengguna dapat memahami kondisi utama tanpa membaca seluruh tabel satu per satu.";
		}
		String t = title.toLowerCase(java.util.Locale.ENGLISH);
		if (t.indexOf("tren") >= 0 || t.indexOf("trend") >= 0 || t.indexOf("bulanan") >= 0 || t.indexOf("harian") >= 0) {
			return "memperlihatkan perubahan data dari waktu ke waktu, sehingga pengguna dapat melihat kapan aktivitas naik, turun, atau perlu perhatian.";
		}
		if (t.indexOf("") >= 0 || t.indexOf("alur") >= 0 || t.indexOf("pipeline") >= 0 || t.indexOf("progress") >= 0) {
			return "menunjukkan tahapan proses dari awal sampai akhir. Gunakan untuk mengetahui tahap mana yang paling banyak menunggu tindak lanjut.";
		}
		if (t.indexOf("risiko") >= 0 || t.indexOf("prioritas") >= 0 || t.indexOf("watchlist") >= 0 || t.indexOf("terlambat") >= 0) {
			return "menyoroti data yang perlu segera diperiksa. Daftar ini membantu pengguna menentukan pekerjaan mana yang paling penting diselesaikan lebih dulu.";
		}
		if (t.indexOf("komposisi") >= 0 || t.indexOf("distribusi") >= 0 || t.indexOf("sebaran") >= 0 || t.indexOf("kategori") >= 0) {
			return "membagi data berdasarkan kelompok atau kategori. Tujuannya agar pengguna dapat mengetahui bagian mana yang paling besar atau paling dominan.";
		}
		if (t.indexOf("top") >= 0 || t.indexOf("ranking") >= 0 || t.indexOf("peringkat") >= 0 || t.indexOf("terbesar") >= 0) {
			return "menampilkan urutan data terbesar atau paling sering muncul. Gunakan untuk melihat fokus utama yang membutuhkan perhatian atau evaluasi.";
		}
		if (t.indexOf("radar") >= 0 || t.indexOf("spider") >= 0 || t.indexOf("kesehatan") >= 0 || t.indexOf("health") >= 0) {
			return "merangkum beberapa indikator penting dalam satu tampilan sederhana. Semakin seimbang nilainya, semakin baik kondisi yang dipantau.";
		}
		if (t.indexOf("saldo") >= 0 || t.indexOf("kas") >= 0 || t.indexOf("keuangan") >= 0 || t.indexOf("nominal") >= 0 || t.indexOf("pembayaran") >= 0 || t.indexOf("piutang") >= 0) {
			return "membantu memantau nilai uang, pembayaran, atau kewajiban yang belum selesai agar keputusan keuangan dapat dilakukan lebih cepat.";
		}
		if (t.indexOf("aset") >= 0 || t.indexOf("inventaris") >= 0 || t.indexOf("barang") >= 0 || t.indexOf("pengadaan") >= 0) {
			return "membantu memantau kondisi aset, barang, atau proses pengadaan agar barang yang dibutuhkan dapat dilacak dengan lebih jelas.";
		}
		if (t.indexOf("pegawai") >= 0 || t.indexOf("kinerja") >= 0 || t.indexOf("lkp") >= 0) {
			return "membantu melihat aktivitas dan kinerja pegawai dalam bentuk ringkas sehingga pimpinan mudah memantau capaian kerja.";
		}
		if (t.indexOf("mahasiswa") >= 0 || t.indexOf("siswa") >= 0 || t.indexOf("akademik") >= 0) {
			return "membantu memantau data akademik dan aktivitas peserta didik agar perkembangan dan potensi masalah dapat terlihat lebih cepat.";
		}
		return "membantu menampilkan ringkasan data penting dalam bentuk yang mudah dibaca, sehingga pengguna dapat memahami kondisi utama dengan cepat.";
	}

	private String safeDashboardHtmlV27(String value) {
		if (value == null) {
			return "";
		}
		String s = value;
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		s = s.replace("\"", "&quot;");
		s = s.replace("'", "&#39;");
		return s;
	}


}
