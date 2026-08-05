package ais.action.master.akunting.helper;

/*
 * ENHANCED_DASHBOARD_UIUX_HTML_CSS_2026_06_06
 * Baseline dari file upload terbaru, disusun ulang sesuai package /java/ais/...
 * Catatan: openSession tetap ditutup pada finally; currentSession tidak ditutup manual.
 * Grafik, tren, radar, dan spider web dipertahankan sebagai HTML/CSS agar ringan dan aman di ZK 5.5.
 */


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.MoveEvent;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.A;
import org.zkoss.zul.AbstractTreeModel;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

import ais.action.master.akunting.GrupTransaksiAction;
import ais.action.master.dashboard.helper.DashboardAkuntingHelper;
import ais.action.master.dashboard.helper.DashboardAkuntingTahunHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.JenisLaporan;
import ais.database.model.akunting.Transaksi;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.UIUtil;
import ais.ui.util.WaktuUtil;

/**
 * Menampilkan ringkasan transaksi akuntansi dan laporan pendukung agar posisi keuangan mudah dipantau.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardAkunting extends MyPortallayout {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9006490521125337935L;

	private SatuanKerja sk = null;
	private Date m = null;
	private Date s = null;
	private double nilaiSaldo = 0.0;

	private List<String> columnHeadersAdding = new ArrayList<String>();

	private EventListener dataAdding = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			Object[] objects = (Object[]) arg0.getData();
			Transaksi transaksi = (Transaksi) objects[0];
			XSSFRow row = (XSSFRow) objects[2];
			XSSFCellStyle hlink_style = (XSSFCellStyle) objects[7];

			XSSFCell cell = row.createCell(GrupTransaksiAction.contents.length);
			Double nilai = transaksi.getDebet() - transaksi.getKredit();
			nilaiSaldo += nilai;
			cell.setCellStyle(hlink_style);
			cell.setCellValue(nilaiSaldo < 0.0 ? "(" + Common.numberFormat.get().format(Math.abs(nilaiSaldo)) + ")"
					: Common.numberFormat.get().format(nilaiSaldo));

		}
	};

	private List<EventListener> eventListeners = new ArrayList<EventListener>();

	private EventListener reload = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			for (EventListener eventListener : eventListeners) {
				eventListener.onEvent(arg0);
			}
		}
	};

	private AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox;

	private MyDatebox mulai;

	private MyDatebox sampai;

	private List<JenisLaporan> jenisLaporans = new ArrayList<JenisLaporan>();

	public DasboardAkunting() throws Exception {
		super();
		columnHeadersAdding.add("Saldo");
		// setHeight("25000px");
		setWidth("100%");
		setMaximizedMode("whole");

		init();
	}

	@SuppressWarnings("unchecked")
	private void init() throws Exception {

		if (m == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 2);
			m = calendar.getTime();

			s = WaktuUtil.getDate();
		}

		MyPortalchildren portalchildren = new MyPortalchildren();
		portalchildren.setParent(DasboardAkunting.this);
		portalchildren.setWidth("100%");

		Panel panel = new ais.ui.util.MyPanelConfig();
		portalchildren.appendChild(panel);
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setMinimizable(false);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(panelchildren);
		toolbar.setWidth("100%");
		ambilDataSatuanKerjaBanbox = new AmbilDataSatuanKerjaBanbox();

		mulai = new MyDatebox(m);
		sampai = new MyDatebox(s);
		ambilDataSatuanKerjaBanbox.setCols(7);
		ambilDataSatuanKerjaBanbox.setValue(sk == null ? "Unit" : sk.getNama());
		ambilDataSatuanKerjaBanbox.setAttribute("satuanKerja", sk);
		ambilDataSatuanKerjaBanbox.setAttribute("myValue", sk);
		ambilDataSatuanKerjaBanbox.setReadonly(true);
		ambilDataSatuanKerjaBanbox.setParent(toolbar);

		ambilDataSatuanKerjaBanbox.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				Common.createDefaultTimer(reload);
			}
		});

		mulai.setReadonly(true);
		mulai.setCols(5);
		mulai.setParent(toolbar);
		mulai.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(reload);
			}
		});
		new MyLabelAgakKecil("sd").setParent(toolbar);
		sampai.setReadonly(true);
		sampai.setCols(5);
		sampai.setParent(toolbar);
		sampai.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(reload);
			}
		});

		List<JenisLaporan> jenisLaporansTemp = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(JenisLaporan.class)
						.add(Restrictions.eq("tampilDiDashboard", true)).addOrder(Order.asc("nama")),
				JenisLaporan.class);
		for (final JenisLaporan jenisLaporan : jenisLaporansTemp) {

			if (jenisLaporan.getTampilDiDashboard() && !jenisLaporans.contains(jenisLaporan)) {
				jenisLaporans.add(jenisLaporan);
			}

			final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig(jenisLaporan.getNama());
			checkboxConfig.setChecked(jenisLaporan.getTampilDiDashboard());
			checkboxConfig.setParent(toolbar);
			checkboxConfig.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (checkboxConfig.isChecked() && !jenisLaporans.contains(jenisLaporan)) {
						jenisLaporans.add(jenisLaporan);
					} else if (!checkboxConfig.isChecked()) {
						jenisLaporans.remove(jenisLaporan);
					}

					Common.createDefaultTimer(reload);
				}
			});
		}

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh.svg");
		refresh.setTooltiptext("Refresh");
		refresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(DasboardAkunting.this);
				DasboardAkunting.this.init();
			}
		});
		refresh.setParent(toolbar);

		appendDashboardAkuntingIntro(panelchildren);

		EventListener reloadPengajuan = new EventListener() {

			private void pengajuanBaru(final JenisLaporan jenisLaporan) throws Exception {

				final MyPortalchildren portalchildren = new MyPortalchildren();
				portalchildren.setParent(DasboardAkunting.this);
				portalchildren.setWidth("100%");

				Panel panel = new ais.ui.util.MyPanelConfig();
				portalchildren.appendChild(panel);
				panel.setTitle(jenisLaporan.getNama());
				panel.setBorder("none");
				panel.setCollapsible(false);
				panel.setClosable(false);
				panel.setMaximizable(false);
				panel.setMinimizable(false);
				panel.addEventListener("onMove", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						MoveEvent moveEvent = (MoveEvent) arg0;
						String left = moveEvent.getLeft();
						String top = moveEvent.getTop();

					}
				});
				panel.setStyle(
						"margin:10px 6px 14px 6px; border:1px solid #e2e8f0; border-radius:18px; overflow:hidden; background:#ffffff; box-shadow:0 12px 26px rgba(15,23,42,.07);");
				final Panelchildren panelchildren = new Panelchildren();
				panelchildren.setParent(panel);

				EventListener pengajuanBaruEventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.clear(panelchildren);
						appendDashboardAkuntingPanelDescription(panelchildren, jenisLaporan == null ? "Laporan Keuangan" : jenisLaporan.getNama());
						if (!jenisLaporans.contains(jenisLaporan)) {
							portalchildren.setVisible(false);
						}

						else {

							sk = (SatuanKerja) ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja");
							m = mulai.getValue();
							s = sampai.getValue();

							portalchildren.setVisible(true);

							Tabbox tabbox = new Tabbox();
							tabbox.setParent(panelchildren);
							Tabs tabs = new Tabs();
							tabs.setParent(tabbox);

							tabs.appendChild(new MyTabConfig("Laporan Keuangan"));

							MyTabConfig bulan;
							tabs.appendChild(bulan = new MyTabConfig("Komparasi Bulan"));

							MyTabConfig tahun;
							tabs.appendChild(tahun = new MyTabConfig("Komparasi Tahun"));

							Tabpanels tabpanels = new Tabpanels();
							tabpanels.setParent(tabbox);

							Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
							tabpanels.appendChild(tabpanelUtama);

							final Tabpanel tabpanelBulan = new ais.ui.util.MyTabpanel();
							tabpanels.appendChild(tabpanelBulan);
							bulan.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									if (tabpanelBulan.getChildren().isEmpty()) {
										int year = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
										DashboardAkuntingHelper.display(tabpanelBulan, sk, year, jenisLaporan);
									}
								}
							});

							final Tabpanel tabpanelTahun = new ais.ui.util.MyTabpanel();
							tabpanels.appendChild(tabpanelTahun);
							tahun.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									if (tabpanelTahun.getChildren().isEmpty()) {
										int year = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
										DashboardAkuntingTahunHelper.display(tabpanelTahun, sk, year - 5, year,
												jenisLaporan);
									}
								}
							});

							Row rowUtamapalingAwal = Common.tampilanScroll1(tabpanelUtama);
							rowUtamapalingAwal.getGrid().setSclass("dgrid");

							final Calendar tanggalSaldoAwal = Calendar.getInstance();
							tanggalSaldoAwal.setTime(m);
							tanggalSaldoAwal.set(Calendar.DATE, tanggalSaldoAwal.get(Calendar.DATE) - 1);

							Long satuan_kerja = sk == null || sk.getId() == null ? -1L : sk.getId();

							Session session = HibernateUtil.currentSession();

							String sql = "select\r\n f.id as urut_laporan,\r\n c.id as kelompok,\r\n"
									+ "c.urut as urut,\r\n max(f.keterangan) as laporan,\r\n"
									+ "max((trim(e.nama))) as jenis_laporan1,\r\n"
									+ "max((trim(e.keterangan))) as jenis_laporan2,\r\n"
									+ "max(c.keterangan) as kelompok_laporan,\r\n"
									+ "max(c.keterangan1) as kelompok_laporan1,\r\n d.kode as kode_akun,\r\n d.nama as nama_akun,\r\n"
									+ "(sum(case when date(a1.tanggal_transaksi) between date('1970-01-01') and date('"
									+ Common.databaseDateFormat.get().format(tanggalSaldoAwal.getTime())
									+ "') then (debet-kredit) else 0 end)) as saldo_awal,\r\n"
									+ "(sum(case when date(a1.tanggal_transaksi) between date('"
									+ Common.databaseDateFormat.get().format(m) + "') and date('"
									+ Common.databaseDateFormat.get().format(s) + "') then (debet) else 0 end)) as debet,\r\n"
									+ "(sum(case when date(a1.tanggal_transaksi) between date('"
									+ Common.databaseDateFormat.get().format(m) + "') and date('"
									+ Common.databaseDateFormat.get().format(s)
									+ "') then (kredit) else 0 end)) as kredit,\r\n"
									+ "(sum(case when date(a1.tanggal_transaksi) <= date('"
									+ Common.databaseDateFormat.get().format(s)
									+ "') then (debet-kredit) else 0 end)) as saldo_akhir,d.id as id_akun "
									+ "from akunting.transaksi a\r\n"
									+ "inner join akunting.grup_transaksi a1 on (a1.id=a.grup_transaksi)\r\n"
									+ "inner join akunting.kelompok_laporan_punya_akun b on (a.akun = b.akun)\r\n"
									+ "inner join akunting.kelompok_laporan c on (c.id = b.kelompok_laporan)\r\n"
									+ "inner join akunting.akun d on (a.akun = d.id)\r\n"
									+ "inner join akunting.master_grup_laporan e on (c.master_grup_laporan = e.id)\r\n"
									+ "inner join akunting.jenis_laporan f on (f.id = c.jenis_laporan)\r\n \r\n"
									+ "where (c.aktif is null or c.aktif)\r\n and a1.posting_history is not null\r\n";

							if (!satuan_kerja.equals(-1L)) {
								sql += "and a1.satuan_kerja = " + satuan_kerja;
							}
							sql += " and c.jenis_laporan=" + jenisLaporan.getId() + " \r\n"
									+ "and date(a1.tanggal_transaksi) between date('1970-01-01') and date('"
									+ Common.databaseDateFormat.get().format(s) + "')\r\n"
									+ "group by f.id,e.id,c.id,d.id\r\n"
									+ "order by f.id,e.nomor_urut,e.id,c.urut,c.id,max(b.nomorurut),urut, d.kode";

							List<Object[]> dataAkunting = session.createSQLQuery(sql).list();

							final TreeMap<String, Object[]> mapData = new TreeMap<String, Object[]>();

							int indexUrut = 89;
							for (Object[] objects : dataAkunting) {
								String jenis_laporan1 = objects[4] + "";
								String jenis_laporan2 = objects[5] + "";
								String kelompok_laporan = objects[6] + "";
								String kelompok_laporan1 = objects[7] + "";

								if (jenis_laporan1 == null || jenis_laporan1.trim().isEmpty()
										|| jenis_laporan1.trim().equalsIgnoreCase("null")) {
									jenis_laporan1 = jenisLaporan.getNama();
								}

								if (jenis_laporan2 == null || jenis_laporan2.trim().isEmpty()
										|| jenis_laporan2.trim().equalsIgnoreCase("null")) {
									jenis_laporan2 = jenis_laporan1;
								}

								if (kelompok_laporan == null || kelompok_laporan.trim().isEmpty()
										|| kelompok_laporan.trim().equalsIgnoreCase("null")) {
									kelompok_laporan = jenis_laporan2;
								}

								if (kelompok_laporan1 == null || kelompok_laporan1.trim().isEmpty()
										|| kelompok_laporan1.trim().equalsIgnoreCase("null")) {
									kelompok_laporan1 = kelompok_laporan;
								}

								String kode_akun = objects[8] + "";
								String nama_akun = objects[9] + "";

								Double saldo_awal = ((Number) objects[10]).doubleValue();
								Double debet = ((Number) objects[11]).doubleValue();
								Double kredit = ((Number) objects[12]).doubleValue();
								Double saldo_akhir = ((Number) objects[13]).doubleValue();

								Long id_akun = ((Number) objects[14]).longValue();

								String u1 = "000000" + indexUrut;

								String urutan = u1.substring(u1.length() - 4);

								String key = urutan + "-" + jenis_laporan1 + "__" + jenis_laporan2 + "__"
										+ kelompok_laporan + "__" + kelompok_laporan1 + "__" + kode_akun + " "
										+ nama_akun;
								key = key.trim();
								mapData.put(key, new Object[] { kode_akun + " " + nama_akun, saldo_awal, debet, kredit,
										saldo_akhir, key, id_akun + "" });
								indexUrut++;
							}

							Set<String> keys = new HashSet<String>();
							for (Object[] objects : dataAkunting) {
								String jenis_laporan1 = objects[4] + "";
								String jenis_laporan2 = objects[5] + "";
								String kelompok_laporan = objects[6] + "";
								String kelompok_laporan1 = objects[7] + "";

								if (jenis_laporan1 == null || jenis_laporan1.trim().isEmpty()
										|| jenis_laporan1.trim().equalsIgnoreCase("null")) {
									jenis_laporan1 = jenisLaporan.getNama();
								}

								if (jenis_laporan2 == null || jenis_laporan2.trim().isEmpty()
										|| jenis_laporan2.trim().equalsIgnoreCase("null")) {
									jenis_laporan2 = jenis_laporan1;
								}

								if (kelompok_laporan == null || kelompok_laporan.trim().isEmpty()
										|| kelompok_laporan.trim().equalsIgnoreCase("null")) {
									kelompok_laporan = jenis_laporan2;
								}

								if (kelompok_laporan1 == null || kelompok_laporan1.trim().isEmpty()
										|| kelompok_laporan1.trim().equalsIgnoreCase("null")) {
									kelompok_laporan1 = kelompok_laporan;
								}

								Double saldo_awal = ((Number) objects[10]).doubleValue();
								Double debet = ((Number) objects[11]).doubleValue();
								Double kredit = ((Number) objects[12]).doubleValue();
								Double saldo_akhir = ((Number) objects[13]).doubleValue();
								Long id_akun = ((Number) objects[14]).longValue();

								String key = jenis_laporan1 + "__" + jenis_laporan2 + "__" + kelompok_laporan + "__"
										+ kelompok_laporan1;
								key = key.trim();

								keys.add(key);

								String u1 = "000000" + keys.size();

								String urutan = u1.substring(u1.length() - 4);
								key = urutan + "-" + key;

								Object[] data = mapData.get(key);
								if (data == null) {
									data = new Object[] { kelompok_laporan1, saldo_awal, debet, kredit, saldo_akhir,
											key, id_akun + "" };
								} else {
									saldo_awal += (Double) data[1];
									debet += (Double) data[2];
									kredit += (Double) data[3];
									saldo_akhir += (Double) data[4];

									String idAkun = data[6] + "," + id_akun;

									data = new Object[] { kelompok_laporan1, saldo_awal, debet, kredit, saldo_akhir,
											key, idAkun };
								}
								mapData.put(key, data);

							}

							keys.clear();
							for (Object[] objects : dataAkunting) {
								String jenis_laporan1 = objects[4] + "";
								String jenis_laporan2 = objects[5] + "";
								String kelompok_laporan = objects[6] + "";

								if (jenis_laporan1 == null || jenis_laporan1.trim().isEmpty()
										|| jenis_laporan1.trim().equalsIgnoreCase("null")) {
									jenis_laporan1 = jenisLaporan.getNama();
								}

								if (jenis_laporan2 == null || jenis_laporan2.trim().isEmpty()
										|| jenis_laporan2.trim().equalsIgnoreCase("null")) {
									jenis_laporan2 = jenis_laporan1;
								}

								if (kelompok_laporan == null || kelompok_laporan.trim().isEmpty()
										|| kelompok_laporan.trim().equalsIgnoreCase("null")) {
									kelompok_laporan = jenis_laporan2;
								}

								Double saldo_awal = ((Number) objects[10]).doubleValue();
								Double debet = ((Number) objects[11]).doubleValue();
								Double kredit = ((Number) objects[12]).doubleValue();
								Double saldo_akhir = ((Number) objects[13]).doubleValue();
								Long id_akun = ((Number) objects[14]).longValue();

								String key = jenis_laporan1 + "__" + jenis_laporan2 + "__" + kelompok_laporan;
								key = key.trim();

								keys.add(key);

								String u1 = "000000" + keys.size();

								String urutan = u1.substring(u1.length() - 4);
								key = urutan + "-" + key;

								Object[] data = mapData.get(key);
								if (data == null) {
									data = new Object[] { kelompok_laporan, saldo_awal, debet, kredit, saldo_akhir, key,
											id_akun + "" };

								} else {
									saldo_awal += (Double) data[1];
									debet += (Double) data[2];
									kredit += (Double) data[3];
									saldo_akhir += (Double) data[4];

									String idAkun = data[6] + "," + id_akun;

									data = new Object[] { kelompok_laporan, saldo_awal, debet, kredit, saldo_akhir, key,
											idAkun };
								}
								mapData.put(key, data);

							}

							keys.clear();
							for (Object[] objects : dataAkunting) {
								String jenis_laporan1 = objects[4] + "";
								String jenis_laporan2 = objects[5] + "";

								if (jenis_laporan1 == null || jenis_laporan1.trim().isEmpty()
										|| jenis_laporan1.trim().equalsIgnoreCase("null")) {
									jenis_laporan1 = jenisLaporan.getNama();
								}

								if (jenis_laporan2 == null || jenis_laporan2.trim().isEmpty()
										|| jenis_laporan2.trim().equalsIgnoreCase("null")) {
									jenis_laporan2 = jenis_laporan1;
								}

								Double saldo_awal = ((Number) objects[10]).doubleValue();
								Double debet = ((Number) objects[11]).doubleValue();
								Double kredit = ((Number) objects[12]).doubleValue();
								Double saldo_akhir = ((Number) objects[13]).doubleValue();

								Long id_akun = ((Number) objects[14]).longValue();

								String key = jenis_laporan1 + "__" + jenis_laporan2;
								key = key.trim();

								keys.add(key);

								String u1 = "000000" + keys.size();

								String urutan = u1.substring(u1.length() - 4);
								key = urutan + "-" + key;

								Object[] data = mapData.get(key);
								if (data == null) {
									data = new Object[] { jenis_laporan2, saldo_awal, debet, kredit, saldo_akhir, key,
											id_akun + "" };
								} else {
									saldo_awal += (Double) data[1];
									debet += (Double) data[2];
									kredit += (Double) data[3];
									saldo_akhir += (Double) data[4];

									String idAkun = data[6] + "," + id_akun;

									data = new Object[] { jenis_laporan2, saldo_awal, debet, kredit, saldo_akhir, key,
											idAkun };
								}
								mapData.put(key, data);

							}

							keys.clear();
							for (Object[] objects : dataAkunting) {
								String jenis_laporan1 = objects[4] + "";
								if (jenis_laporan1 == null || jenis_laporan1.trim().isEmpty()
										|| jenis_laporan1.trim().equalsIgnoreCase("null")) {
									jenis_laporan1 = jenisLaporan.getNama();
								}

								Double saldo_awal = ((Number) objects[10]).doubleValue();
								Double debet = ((Number) objects[11]).doubleValue();
								Double kredit = ((Number) objects[12]).doubleValue();
								Double saldo_akhir = ((Number) objects[13]).doubleValue();
								Long id_akun = ((Number) objects[14]).longValue();
								String key = jenis_laporan1;
								key = key.trim();

								keys.add(key);

								String u1 = "000000" + keys.size();

								String urutan = u1.substring(u1.length() - 4);
								key = urutan + "-" + key;

								Object[] data = mapData.get(key);
								if (data == null) {
									data = new Object[] { jenis_laporan1, saldo_awal, debet, kredit, saldo_akhir, key,
											id_akun + "" };
								} else {
									saldo_awal += (Double) data[1];
									debet += (Double) data[2];
									kredit += (Double) data[3];
									saldo_akhir += (Double) data[4];

									String idAkun = data[6] + "," + id_akun;
									data = new Object[] { jenis_laporan1, saldo_awal, debet, kredit, saldo_akhir, key,
											idAkun };
								}
								mapData.put(key, data);

							}

							Row row = new Row();
							row.setValign("top");
							row.setParent(rowUtamapalingAwal.getParent());

							final Tree tree = new Tree();
							tree.setZclass("z-dottree");
							tree.setParent(row);

							MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download",
									"/img/svg/download.svg");
							toolbarbutton.setParent(rowUtamapalingAwal);
							toolbarbutton.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									UIUtil.downloadTree(tree);
								}
							});

							Treecols columns = new Treecols();

							columns.setParent(tree);

							Treecol column = new Treecol("Uraian");
							column.setParent(columns);

							column = new Treecol("Saldo Awal");
							column.setWidth("17%");
							column.setAlign("right");
							column.setParent(columns);

							column = new Treecol("Debet");
							column.setWidth("17%");
							column.setAlign("right");
							column.setParent(columns);

							column = new Treecol("Kredit");
							column.setWidth("17%");
							column.setAlign("right");
							column.setParent(columns);

							column = new Treecol("Saldo Akhir");
							column.setWidth("17%");
							column.setAlign("right");
							column.setParent(columns);

							tree.setModel(new AbstractTreeModel("") {

								/**
								 * 
								 */
								private static final long serialVersionUID = 1L;

								@Override
								public boolean isLeaf(Object arg0) {
									String key = "";
									try {
										Object[] objects = (Object[]) arg0;
										key = (String) objects[5];
									} catch (Exception e) {
										key = (String) arg0;
									}
									return key == null ? true : key.split("__").length == 5;
								}

								@Override
								public int getChildCount(Object arg0) {
									int count = 0;
									String key = "";
									try {
										Object[] objects = (Object[]) arg0;
										key = (String) objects[5];
									} catch (Exception e) {
										key = (String) arg0;
									}

									try {
										key = key.isEmpty() ? key : key.split("-", 2)[1];
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardAkunting.java:737");
										// TODO: handle exception
									}

									if (key.isEmpty()) {
										for (String k : mapData.keySet()) {
											if (k.split("__").length == 1) {
												count++;
											}
										}
									} else {
										for (String k : mapData.keySet()) {

											String sli = k.split("-", 2)[1];

											if ((k.split("__").length - 1) == key.split("__").length
													&& sli.startsWith(key)) {
												count++;
											}
										}
									}

									return count;
								}

								@Override
								public Object getChild(Object arg0, int arg1) {
									String key = "";
									try {
										Object[] objects = (Object[]) arg0;
										key = (String) objects[5];
									} catch (Exception e) {
										key = (String) arg0;
									}

									try {
										key = key.isEmpty() ? key : key.split("-", 2)[1];
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/DasboardAkunting.java:774");
										// TODO: handle exception
									}

									int count = 0;
									if (key.isEmpty()) {
										for (String k : mapData.keySet()) {
											if (k.split("__").length == 1) {

												if (count == arg1) {
													return mapData.get(k);
												}

												count++;
											}
										}
									} else {
										for (String k : mapData.keySet()) {

											String sli = k.split("-", 2)[1];

											if ((k.split("__").length - 1) == key.split("__").length
													&& sli.startsWith(key)) {

												if (count == arg1) {
													return mapData.get(k);
												}

												count++;
											}
										}
									}
									return null;
								}
							});

							tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

								@Override
								public void render(Treeitem treeitem, Object arg1) throws Exception {
									if (arg1 == null) {
										treeitem.setVisible(false);
										return;
									}

									Object[] objects = (Object[]) arg1;
									final String idAkun = (String) objects[6];
									Treerow treerow = new Treerow();
									treerow.setParent(treeitem);

									Treecell arg0 = new Treecell();
									arg0.setStyle("font-size:12px;color:blue;");
									arg0.setParent(treerow);

									A a = new A(objects[0] + "");
									a.setParent(arg0);
									a.setStyle("font-size:12px;color:blue;");
									a.addEventListener("onClick", new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {

											Common.displayWindow(
													"/pages/master/akunting/grup_transaksi.zul?akun=" + idAkun
															+ "&mulai=" + Common.dateFormat8.get().format(m) + "&sampai="
															+ Common.dateFormat8.get().format(s),
													true, "95%", "95%", null, "", false);

										}
									});

									Double saldo_awal = (Double) objects[1], debet = (Double) objects[2],
											kredit = (Double) objects[3], saldo_akhir = (Double) objects[4];

									arg0 = new Treecell();
									arg0.setParent(treerow);
									arg0.setStyle("font-size:14px;color:blue;");

									a = new A(saldo_awal < 0.0
											? "(" + Common.numberFormat.get().format(Math.abs(saldo_awal)) + ")"
											: Common.numberFormat.get().format(saldo_awal));
									a.setParent(arg0);
									a.setStyle("font-size:14px;color:blue;");
									a.addEventListener("onClick", new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											nilaiSaldo = 0.0;

											EventListener eventListener = (EventListener) Common.cetakDataCustomButton(
													Transaksi.class, new DataCriteriaWithColumn() {

														@Override
														public Object[] initCriteria(boolean order) {

															try {

																Criteria criteria = HibernateUtil.currentSession()
																		.createCriteria(Transaksi.class)

																		.add(Restrictions.or(
																				Restrictions.or(
																						Restrictions.gt("debet", 0.1),
																						Restrictions.lt("debet", -0.1)),
																				Restrictions.or(
																						Restrictions.gt("kredit", 0.1),
																						Restrictions.lt("kredit",
																								-0.1)))

																		)

																		.add(Restrictions.isNotNull("postingHistory"))
																		.add(Restrictions.isNotNull("grupTransaksi"))

																		.add(Restrictions.sqlRestriction(
																				"this_.akun in (" + idAkun + ")"))

																		.add(Restrictions.sqlRestriction(
																				"date(this_.tanggal_transaksi) between date('1970-01-01') and date('"
																						+ Common.databaseDateFormat.get()
																								.format(tanggalSaldoAwal
																										.getTime())
																						+ "')"))
																		.addOrder(Order.asc("tanggalTransaksi"));

																return new Object[] { criteria,
																		GrupTransaksiAction.contents };

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}
															return null;
														}

													}, null, "Download Data", "/img/svg/download.svg", columnHeadersAdding,
													dataAdding, false, null, "DATA TAMBAHAN",
													new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "" })
													.getAttribute("eventListener");

											eventListener.onEvent(null);

										}
									});

									arg0 = new Treecell();
									arg0.setParent(treerow);
									arg0.setStyle("font-size:14px;color:blue;");

									a = new A(debet < 0.0 ? "(" + Common.numberFormat.get().format(Math.abs(debet)) + ")"
											: Common.numberFormat.get().format(debet));
									a.setParent(arg0);
									a.setStyle("font-size:14px;color:blue;");
									a.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											nilaiSaldo = 0.0;
											EventListener eventListener = (EventListener) Common.cetakDataCustomButton(
													Transaksi.class, new DataCriteriaWithColumn() {

														@Override
														public Object[] initCriteria(boolean order) {

															try {

																Criteria criteria = HibernateUtil.currentSession()
																		.createCriteria(Transaksi.class)

																		.add(Restrictions.or(
																				Restrictions.gt("debet", 0.1),
																				Restrictions.lt("debet", -0.1)))

																		.add(Restrictions.isNotNull("postingHistory"))
																		.add(Restrictions.isNotNull("grupTransaksi"))

																		.add(Restrictions.sqlRestriction(
																				"this_.akun in (" + idAkun + ")"))

																		.add(Restrictions.sqlRestriction(
																				"date(this_.tanggal_transaksi) between date('"
																						+ Common.databaseDateFormat.get()
																								.format(m)
																						+ "') and date('"
																						+ Common.databaseDateFormat.get()
																								.format(s)
																						+ "')"))
																		.addOrder(Order.asc("tanggalTransaksi"));

																return new Object[] { criteria,
																		GrupTransaksiAction.contents };

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}
															return null;
														}

													}, null, "Download Data", "/img/svg/download.svg", columnHeadersAdding,
													dataAdding, false, null, "DATA TAMBAHAN",
													new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "" })
													.getAttribute("eventListener");

											eventListener.onEvent(null);

										}
									});

									arg0 = new Treecell();
									arg0.setParent(treerow);
									arg0.setStyle("font-size:14px;color:blue;");

									a = new A(kredit < 0.0 ? "(" + Common.numberFormat.get().format(Math.abs(kredit)) + ")"
											: Common.numberFormat.get().format(kredit));
									a.setParent(arg0);
									a.setStyle("font-size:14px;color:blue;");
									a.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											nilaiSaldo = 0.0;
											EventListener eventListener = (EventListener) Common.cetakDataCustomButton(
													Transaksi.class, new DataCriteriaWithColumn() {

														@Override
														public Object[] initCriteria(boolean order) {

															try {

																Criteria criteria = HibernateUtil.currentSession()
																		.createCriteria(Transaksi.class)

																		.add(Restrictions.or(
																				Restrictions.gt("kredit", 0.1),
																				Restrictions.lt("kredit", -0.1)))

																		.add(Restrictions.isNotNull("postingHistory"))
																		.add(Restrictions.isNotNull("grupTransaksi"))

																		.add(Restrictions.sqlRestriction(
																				"this_.akun in (" + idAkun + ")"))

																		.add(Restrictions.sqlRestriction(
																				"date(this_.tanggal_transaksi) between date('"
																						+ Common.databaseDateFormat.get()
																								.format(m)
																						+ "') and date('"
																						+ Common.databaseDateFormat.get()
																								.format(s)
																						+ "')"))
																		.addOrder(Order.asc("tanggalTransaksi"));

																return new Object[] { criteria,
																		GrupTransaksiAction.contents };

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}
															return null;
														}

													}, null, "Download Data", "/img/svg/download.svg", columnHeadersAdding,
													dataAdding, false, null, "DATA TAMBAHAN",
													new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "" })
													.getAttribute("eventListener");

											eventListener.onEvent(null);

										}
									});

									arg0 = new Treecell();
									arg0.setParent(treerow);
									arg0.setStyle("font-size:14px;color:blue;");

									a = new A(saldo_akhir < 0.0
											? "(" + Common.numberFormat.get().format(Math.abs(saldo_akhir)) + ")"
											: Common.numberFormat.get().format(saldo_akhir));
									a.setParent(arg0);
									a.setStyle("font-size:14px;color:blue;");
									a.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											nilaiSaldo = 0.0;
											EventListener eventListener = (EventListener) Common.cetakDataCustomButton(
													Transaksi.class, new DataCriteriaWithColumn() {

														@Override
														public Object[] initCriteria(boolean order) {

															try {

																Criteria criteria = HibernateUtil.currentSession()
																		.createCriteria(Transaksi.class)

																		.add(Restrictions.or(
																				Restrictions.or(
																						Restrictions.gt("debet", 0.1),
																						Restrictions.lt("debet", -0.1)),
																				Restrictions.or(
																						Restrictions.gt("kredit", 0.1),
																						Restrictions.lt("kredit",
																								-0.1)))

																		)

																		.add(Restrictions.isNotNull("postingHistory"))
																		.add(Restrictions.isNotNull("grupTransaksi"))

																		.add(Restrictions.sqlRestriction(
																				"this_.akun in (" + idAkun + ")"))

																		.add(Restrictions.sqlRestriction(
																				"date(this_.tanggal_transaksi) between date('1970-01-01') and date('"
																						+ Common.databaseDateFormat.get()
																								.format(s)
																						+ "')"))
																		.addOrder(Order.asc("tanggalTransaksi"));

																return new Object[] { criteria,
																		GrupTransaksiAction.contents };

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}
															return null;
														}

													}, null, "Download Data", "/img/svg/download.svg", columnHeadersAdding,
													dataAdding, false, null, "DATA TAMBAHAN",
													new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
															"", "", "", "", "" })
													.getAttribute("eventListener");

											eventListener.onEvent(null);

										}
									});
								}
							});

							dataAkunting = null;
						}
					}

				};

				pengajuanBaruEventListener.onEvent(null);

				eventListeners.add(pengajuanBaruEventListener);
			}

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (sk == null) {
					Sekolah sekolah = SekolahUtil.getSekolah();

					Tbmuser tbmuser = Common.getCurrentUser();

					PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
					sk = perguruanTinggi != null && perguruanTinggi.getSatuanKerja() != null
							&& perguruanTinggi.getDosenHarusPakaiSatuanKerja() ? perguruanTinggi.getSatuanKerja()
									: (tbmuser == null ? null : tbmuser.ambilSatuanKerja());

					if (sekolah != null && sekolah.getSatuanKerja() != null && sekolah.getGuruHarusPakaiSatuanKerja()) {
						sk = sekolah == null ? null : sekolah.getSatuanKerja();
					}
				}

				for (JenisLaporan jenisLaporan : jenisLaporans) {
					pengajuanBaru(jenisLaporan);
				}

			}
		};

		/* Filter toolbar sudah ter-render. Tunda load data ke timer berikutnya agar halaman tidak terblokir. */
		Common.createDefaultTimer(reloadPengajuan);

//		EventListener reloadLajur = new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//
//				MyPortalchildren portalchildren = new MyPortalchildren();
//				portalchildren.setParent(DasboardAkunting.this);
//				portalchildren.setWidth("100%");
//
//				Panel panel = new ais.ui.util.MyPanelConfig();
//				portalchildren.appendChild(panel);
//				panel.setTitle("Neraca Lajur");
//				panel.setBorder("none");
//				panel.setCollapsible(false);
//				panel.setClosable(false);
//				panel.setMaximizable(false);
//				panel.setMinimizable(false);
//				panel.addEventListener("onMove", new EventListener() {
//
//					@Override
//					public void onEvent(Event arg0) throws Exception {
//						MoveEvent moveEvent = (MoveEvent) arg0;
//						String left = moveEvent.getLeft();
//						String top = moveEvent.getTop();
//

//					}
//				});
//				panel.setStyle(
//						"margin-bottom:10px;border: 1px;border-style: solid;border-color: #dbdbd9;margin-left: 3px;margin-right: 3px;border-radius: 10px;");
//				Panelchildren panelchildren = new Panelchildren();
//				panelchildren.setParent(panel);
//
//				DasboardNeracaLajur dasboardNeracaLajur = new DasboardNeracaLajur();
//				dasboardNeracaLajur.setHeight("100%");
//				dasboardNeracaLajur.setWidth("100%");
//				dasboardNeracaLajur.setParent(panelchildren);
//			}
//
//		};
//
//		reloadLajur.onEvent(null);
//
//		EventListener reloadBukuBesar = new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//
//				MyPortalchildren portalchildren = new MyPortalchildren();
//				portalchildren.setParent(DasboardAkunting.this);
//				portalchildren.setWidth("100%");
//
//				Panel panel = new ais.ui.util.MyPanelConfig();
//				portalchildren.appendChild(panel);
//				panel.setTitle("Buku Besar");
//				panel.setBorder("none");
//				panel.setCollapsible(false);
//				panel.setClosable(false);
//				panel.setMaximizable(false);
//				panel.setMinimizable(false);
//				panel.addEventListener("onMove", new EventListener() {
//
//					@Override
//					public void onEvent(Event arg0) throws Exception {
//						MoveEvent moveEvent = (MoveEvent) arg0;
//						String left = moveEvent.getLeft();
//						String top = moveEvent.getTop();
//

//					}
//				});
//				panel.setStyle(
//						"margin-bottom:10px;border: 1px;border-style: solid;border-color: #dbdbd9;margin-left: 3px;margin-right: 3px;border-radius: 10px;");
//				Panelchildren panelchildren = new Panelchildren();
//				panelchildren.setParent(panel);
//
//				DasboardBukuBesar dasboardBukuBesar = new DasboardBukuBesar();
//				dasboardBukuBesar.setHeight("100%");
//				dasboardBukuBesar.setWidth("100%");
//				dasboardBukuBesar.setParent(panelchildren);
//			}
//
//		};
//
//		reloadBukuBesar.onEvent(null);

	}

	private void appendDashboardAkuntingIntro(Panelchildren parent) {
		if (parent == null) {
			return;
		}
		org.zkoss.zul.Html html = new org.zkoss.zul.Html("<div style=\"margin:10px 0 12px 0; padding:16px 18px; border-radius:18px; "
				+ "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); color:#ffffff; box-shadow:0 14px 30px rgba(15,23,42,.16);\">"
				+ "<div style=\"font-size:12px; text-transform:uppercase; letter-spacing:.12em; opacity:.88;\">Accounting Control Center</div>"
				+ "<div style=\"font-size:24px; font-weight:900; margin-top:5px;\">Dashboard Akunting</div>"
				+ "<div style=\"font-size:12px; line-height:1.6; opacity:.92; margin-top:7px; max-width:900px;\">"
				+ "Gunakan ringkasan ini untuk memantau laporan keuangan, komparasi bulanan, dan komparasi tahunan berdasarkan unit serta periode yang dipilih. "
				+ "Setiap panel menjelaskan fungsi informasinya agar pengguna dapat memahami laporan tanpa harus membaca seluruh detail transaksi.</div>"
				+ "</div>");
		html.setParent(parent);
	}

	private void appendDashboardAkuntingPanelDescription(Panelchildren parent, String title) {
		if (parent == null) {
			return;
		}
		String desc = getDashboardAkuntingPanelDescription(title);
		org.zkoss.zul.Html html = new org.zkoss.zul.Html("<div style=\"margin:0 0 12px 0; padding:10px 12px; "
				+ "border-radius:12px; background:#f8fafc; border:1px solid #e2e8f0; color:#475569; "
				+ "font-size:11.5px; line-height:1.55;\">"
				+ "<b style=\"color:#0f172a;\"></b> " + safeDashboardAkuntingHtml(desc) + "</div>");
		html.setParent(parent);
	}

	private String getDashboardAkuntingPanelDescription(String title) {
		if (title == null || title.trim().length() == 0) {
			return "menampilkan laporan keuangan sesuai jenis laporan yang dipilih.";
		}
		String t = title.toLowerCase(java.util.Locale.ENGLISH);
		if (t.indexOf("neraca") >= 0) {
			return "membantu melihat posisi aset, kewajiban, dan ekuitas agar kondisi keuangan lembaga dapat dipantau secara cepat.";
		}
		if (t.indexOf("laba") >= 0 || t.indexOf("rugi") >= 0 || t.indexOf("operasional") >= 0) {
			return "membantu membaca pendapatan dan beban sehingga pengguna mengetahui hasil operasional pada periode yang dipilih.";
		}
		if (t.indexOf("arus") >= 0 || t.indexOf("kas") >= 0) {
			return "membantu memantau pergerakan kas masuk dan keluar agar likuiditas lembaga mudah dikendalikan.";
		}
		return "menampilkan ringkasan laporan keuangan sesuai struktur akun. Gunakan tab komparasi bulan dan tahun untuk membandingkan perubahan nilai dari waktu ke waktu.";
	}

	private String safeDashboardAkuntingHtml(String value) {
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
