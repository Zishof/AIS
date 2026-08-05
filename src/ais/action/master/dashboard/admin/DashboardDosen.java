package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

import java.util.List;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.maintenance.MainAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardDosen extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Center center;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private int width = 750;
	private int height = 100;

	public DashboardDosen() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardDosen(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {
		// Tombol ekspor TIDAK lagi mengambang di atas tab; dipasang di dalam tab pertama (lihat
		// bawah, setelah tabpanel1 dibuat) sebagai satu button-group menyatu dengan dashboard.
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Data");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Beban SKS");
		tab2.setParent(tabs);

		MyTabConfig tab3 = new MyTabConfig("Kegiatan");
		tab3.setParent(tabs);

		MyTabConfig tab4 = new MyTabConfig("Organisasi");
		tab4.setParent(tabs);

		MyTabConfig tab5 = new MyTabConfig("Prestasi");
		tab5.setParent(tabs);

		MyTabConfig tab6 = new MyTabConfig("Karya");
		tab6.setParent(tabs);

		MyTabConfig tab51 = new MyTabConfig("Publikasi Ilmiah");
		tab51.setParent(tabs);

		MyTabConfig tab7 = new MyTabConfig("Penelitian");
		tab7.setParent(tabs);

		MyTabConfig tab8 = new MyTabConfig("Pengabdian");
		tab8.setParent(tabs);

		MyTabConfig tab9 = new MyTabConfig("Penulis Buku");
		tab9.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);
		tabpanel1.setHeight("30330px");
		// Grup tombol Cetak PDF + Ekspor Excel DI DALAM tab pertama (menyatu dengan dashboard),
		// mengekspor seluruh tabel di semua tab (sumberGrid = this).
		DashboardGridExportHelper.pasangGrup(tabpanel1, this, "Dosen");

		final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);
		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel2.getChildren().size() == 0) {
					DashboardBebanSksDosen dashboardBebanSKSDosen = new DashboardBebanSksDosen();
					dashboardBebanSKSDosen.setHeight("100%");
					dashboardBebanSKSDosen.setWidth("100%");
					dashboardBebanSKSDosen.setParent(tabpanel2);
				}
			}
		});

		final Tabpanel tabpanel3 = new ais.ui.util.MyTabpanel();
		tabpanel3.setParent(tabpanels);
		tab3.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel3.getChildren().size() == 0) {
					DashboardKegiatanKedosenanUmum dashboardKegiatanKedosenanUmum = new DashboardKegiatanKedosenanUmum();
					dashboardKegiatanKedosenanUmum.setHeight("100%");
					dashboardKegiatanKedosenanUmum.setWidth("100%");

					dashboardKegiatanKedosenanUmum.setParent(tabpanel3);
				}
			}
		});

		final Tabpanel tabpanel4 = new ais.ui.util.MyTabpanel();
		tabpanel4.setParent(tabpanels);
		tab4.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel4.getChildren().size() == 0) {
					DashboardOrganisasiDosenUmum dashboardOrganisasiDosenUmum = new DashboardOrganisasiDosenUmum();
					dashboardOrganisasiDosenUmum.setHeight("100%");
					dashboardOrganisasiDosenUmum.setWidth("100%");
					dashboardOrganisasiDosenUmum.setParent(tabpanel4);
				}
			}
		});

		final Tabpanel tabpanel5 = new ais.ui.util.MyTabpanel();
		tabpanel5.setParent(tabpanels);
		tab5.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel5.getChildren().size() == 0) {
					DashboardPrestasiDosenUmum dashboardPrestasiDosenUmum = new DashboardPrestasiDosenUmum();
					dashboardPrestasiDosenUmum.setHeight("100%");
					dashboardPrestasiDosenUmum.setWidth("100%");

					dashboardPrestasiDosenUmum.setParent(tabpanel5);
				}
			}
		});

		final Tabpanel tabpanel6 = new ais.ui.util.MyTabpanel();
		tabpanel6.setParent(tabpanels);
		tab6.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel6.getChildren().size() == 0) {
					DashboardKaryaDosenUmum dashboardKaryaDosenUmum = new DashboardKaryaDosenUmum();
					dashboardKaryaDosenUmum.setHeight("100%");
					dashboardKaryaDosenUmum.setWidth("100%");

					dashboardKaryaDosenUmum.setParent(tabpanel6);
				}
			}
		});

		final Tabpanel tabpanel51 = new ais.ui.util.MyTabpanel();
		tabpanel51.setParent(tabpanels);
		tab51.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel51.getChildren().size() == 0) {
					DashboardPublikasiIlmiah dashboardPublikasiIlmiah = new DashboardPublikasiIlmiah();
					dashboardPublikasiIlmiah.setHeight("100%");
					dashboardPublikasiIlmiah.setWidth("100%");
					dashboardPublikasiIlmiah.setParent(tabpanel51);
				}
			}
		});

		final Tabpanel tabpanel7 = new ais.ui.util.MyTabpanel();
		tabpanel7.setParent(tabpanels);
		tab7.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel7.getChildren().size() == 0) {
					DashboardPenelitianDanPengabdian dashboardPenelitianDanPengabdian = new DashboardPenelitianDanPengabdian(
							ConstantValues.PENELITIAN);
					dashboardPenelitianDanPengabdian.setHeight("100%");
					dashboardPenelitianDanPengabdian.setWidth("100%");
					dashboardPenelitianDanPengabdian.setParent(tabpanel7);
				}
			}
		});

		final Tabpanel tabpanel8 = new ais.ui.util.MyTabpanel();
		tabpanel8.setParent(tabpanels);
		tab8.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel8.getChildren().size() == 0) {
					DashboardPenelitianDanPengabdian dashboardPenelitianDanPengabdian = new DashboardPenelitianDanPengabdian(
							ConstantValues.PENGABDIAN);
					dashboardPenelitianDanPengabdian.setHeight("100%");
					dashboardPenelitianDanPengabdian.setWidth("100%");
					dashboardPenelitianDanPengabdian.setParent(tabpanel8);
				}
			}
		});

		final Tabpanel tabpanel9 = new ais.ui.util.MyTabpanel();
		tabpanel9.setParent(tabpanels);
		tab9.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel9.getChildren().size() == 0) {
					DashboardBukuAjar dashboardBukuAjar = new DashboardBukuAjar();
					dashboardBukuAjar.setHeight("100%");
					dashboardBukuAjar.setWidth("100%");
					dashboardBukuAjar.setParent(tabpanel9);
				}
			}
		});

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(tabpanel1);
		borderlayout.setStyle("min-height:25000px");
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getUserId() != null) {
			Integer desktopHeight = MainAction.desktopHeights.get(tbmuser.getUserId());
			if (desktopHeight != null) {
				borderlayout.setStyle("min-height:" + (desktopHeight * 0.9) + "px");
			}
		}

		North north = new North();
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setParent(borderlayout);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}

		};

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		row.appendChild(new MyLabelConfig("Fakultas / Prodi"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		hbox.appendChild(searchfakultas);
		hbox.appendChild(searchjurusan);

		searchfakultas.addEventListener("onChange", eventListener);
		searchjurusan.addEventListener("onChange", eventListener);
		searchfakultas.setCols(2);
		searchjurusan.setCols(2);

		center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		Common.createDefaultTimerNoBusy(new EventListener() {
			public void onEvent(Event e) throws Exception {
				reload();
			}
		});
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void reload() {
		Common.clear(center);
		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Fakultas");
		column.setParent(columns);

		column.setParent(columns);
		column = new MyColumnConfig("Jurusan");
		column.setParent(columns);

		column.setParent(columns);
		column = new MyColumnConfig("Jumlah Dosen");
		column.setAlign("center");
		column.setParent(columns);

		Fakultas fak = (Fakultas) (searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue());
		Jurusan jur = (Jurusan) (searchjurusan.getSelectedItem() == null ? null
				: searchjurusan.getSelectedItem().getValue());
		List<Jurusan> jurusans = HibernateUtil.currentSession().createCriteria(Jurusan.class)
				.add(jur == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("id", jur.getId()))
				.add(fak == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("fakultas", fak))
				.addOrder(Order.asc("fakultas"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		Rows rows = new Rows();
		rows.setParent(grid);

		SimpleCategoryModel categoryModel = new SimpleCategoryModel();
		categoryModel.clear();

		for (final Jurusan jurusan : jurusans) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new MyLabelBoldAja(jurusan.getFakultas().getNama()));
			row.appendChild(new MyLabelBoldAja(jurusan.getNama()));

			Integer count = ((Number) HibernateUtil.currentSession().createCriteria(Dosen.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount()).add(Restrictions.eq("jurusan", jurusan)).uniqueResult())
					.intValue();

			categoryModel.setValue(jurusan.getNama(), "", count);

			A a = new A(count + "");
			a.setStyle("font-size:12px;");
			a.setParent(row);
			a.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Common.displayWindow("/common/dashboard/dosen.zul?jurusan=" + jurusan.getId(), true, "650px",
							"95%");

				}
			});

		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");

		row.setParent(rows);
		row.setSpans((3) + "");
		row.setAlign("center");

		row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModel, "Dasbor Dosen", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
}
}
