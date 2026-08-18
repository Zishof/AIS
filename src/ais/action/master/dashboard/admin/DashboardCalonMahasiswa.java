package ais.action.master.dashboard.admin;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
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
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
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
import ais.action.master.pmb.CetakRegistrasiAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardCalonMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Intbox mulai;
	private Intbox sampai;
	private Combobox searchStatusAwalMahasiswa;
	private Combobox searchprogram;
	private Center center;
	private Combobox jenis;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	protected Grid grid;

	private int width = 750;
	private int height = 100;

	public DashboardCalonMahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardCalonMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {
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

		MyTabConfig tab2 = new MyTabConfig("Progres");
		tab2.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);
		tabpanel1.setHeight("30330px");

		final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);
		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel2.getChildren().size() == 0) {

					MyWindow window = new MyWindow("", "none", false);
					window.setHeight("100%");
					window.setWidth("100%");
					window.setParent(tabpanel2);
					MyInclude iframe = new MyInclude("/pages/pmb/statistik/rekap_pendaftar_spmb_semua.zul");
					iframe.setParent(window);

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

		Number m = (Number) HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.max("tahun")).uniqueResult();

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Angkatan"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}
		};

		mulai = new Intbox((m == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) : m.intValue()) - 7);
		mulai.setCols(2);
		hbox.appendChild(mulai);

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));

		sampai = new Intbox(mulai.getValue() + 7);
		sampai.setCols(2);
		hbox.appendChild(sampai);

		mulai.addEventListener("onChange", eventListener);
		sampai.addEventListener("onChange", eventListener);

		searchStatusAwalMahasiswa = new Combobox();
		row.appendChild(new MyLabelConfig("Status Awal"));
		row.appendChild(searchStatusAwalMahasiswa);
		Common.insertComboDanSemua(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		searchStatusAwalMahasiswa.setWidth("90%");

		searchprogram = new Combobox();
		row.appendChild(new MyLabelConfig("Program"));
		Common.initPrograms(searchprogram);
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.setReadonly(true);

		jenis = new Combobox();
		row.appendChild(new MyLabelConfig("Jenis"));
		MyComboitemConfig comboitem = new MyComboitemConfig();
		comboitem.setLabel("Lulus Seleksi");
		comboitem.setValue("prodiLulus");
		jenis.appendChild(comboitem);
		jenis.setSelectedItem(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Peminat Pilihan I");
		comboitem.setValue("prodi1");
		jenis.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Peminat Pilihan II");
		comboitem.setValue("prodi2");
		jenis.appendChild(comboitem);

		if (Common.bolehKonfigurasi("pilihan_peminat_3", Konfigurasi.TIDAK_AKTIF)) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Peminat Pilihan III");
			comboitem.setValue("prodi3");
			jenis.appendChild(comboitem);
		}

		if (Common.bolehKonfigurasi("pilihan_peminat_4", Konfigurasi.TIDAK_AKTIF)) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Peminat Pilihan IV");
			comboitem.setValue("prodi4");
			jenis.appendChild(comboitem);
		}

		if (Common.bolehKonfigurasi("pilihan_peminat_5", Konfigurasi.TIDAK_AKTIF)) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Peminat Pilihan V");
			comboitem.setValue("prodi5");
			jenis.appendChild(comboitem);
		}

		row.appendChild(jenis);
		jenis.setWidth("90%");
		jenis.setReadonly(true);

		searchStatusAwalMahasiswa.addEventListener("onChange", eventListener);
		searchprogram.addEventListener("onChange", eventListener);
		jenis.addEventListener("onChange", eventListener);

		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		row.appendChild(new MyLabelConfig("Fakultas / Prodi"));
		hbox = new Hbox();
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

		row = new MyFormRow();
		row.setParent(rows);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
		toolbarbutton.setParent(row);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UIUtil.downloadGrid(DashboardCalonMahasiswa.this.grid);
			}
		});

		Common.createDefaultTimerNoBusy(new EventListener() {
			public void onEvent(Event e) throws Exception {
				reload();
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void reload() {
		Common.clear(center);

		final StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) (searchStatusAwalMahasiswa
				.getSelectedItem() == null || searchStatusAwalMahasiswa.getSelectedItem().getValue() == null ? null
						: searchStatusAwalMahasiswa.getSelectedItem().getValue());
		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final String jen = (String) (jenis.getSelectedItem() == null || jenis.getSelectedItem().getValue() == null
				? null
				: jenis.getSelectedItem().getValue());

		final Map<Long, List<Object[]>> datas = new HashMap<Long, List<Object[]>>();
		Fakultas fak = (Fakultas) (searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue());
		Jurusan jur = (Jurusan) (searchjurusan.getSelectedItem() == null ? null
				: searchjurusan.getSelectedItem().getValue());
		final List<Jurusan> jurusans = HibernateUtil.currentSession().createCriteria(Jurusan.class)
				.add(jur == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("id", jur.getId()))
				.add(fak == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("fakultas", fak))
				.addOrder(Order.asc("fakultas"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		final int mul = mulai.getValue() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 7
				: mulai.getValue();
		final int sam = sampai.getValue() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
				: sampai.getValue();

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);

				grid = new Grid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig("Fakultas");
				column.setParent(columns);
				column.setWidth("15%");

				column.setParent(columns);
				column = new MyColumnConfig("Jurusan");
				column.setParent(columns);

				column.setWidth("15%");

				for (int tahun = mul; tahun <= sam; tahun++) {
					column.setParent(columns);
					column = new MyColumnConfig(tahun + "");
					column.setAlign("center");
					column.setParent(columns);
				}

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

					List<Object[]> data = datas.get(jurusan.getId());

					for (int tahun = mul; tahun <= sam; tahun++) {
						final int thn = tahun;
						Number count = 0;
						for (Object[] o : data) {
							Object tahunangkatan = o[1];
							if (tahunangkatan != null && Integer.parseInt(tahunangkatan.toString()) == tahun) {
								count = (Number) o[0];
								break;
							}
						}
						categoryModel.setValue(jurusan.getNama(), tahun, count);
						A a = new A(count + "");
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								EventListener eventListener = (EventListener) Common.cetakDataCustomButton(
										BiodataCalonMahasiswa.class, new DataCriteriaWithColumn() {

											@Override
											public Object[] initCriteria(boolean order) {

												try {

													Criteria criteria = HibernateUtil.currentSession()
															.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

															.add(statusAwalMahasiswa == null
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("statusAwalMahasiswa",
																			statusAwalMahasiswa))

															.add(program == null ? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("program", program))

															.add(Restrictions.eq(jen, jurusan))
															.add(Restrictions.eq("tahun", thn));

													return new Object[] { criteria, CetakRegistrasiAction.contents };

												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
												}
												return null;
											}

										}, null, "Download Data", "/img/print.png", null, null, false, null,
										"DATA TAMBAHAN",
										new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "" })
										.getAttribute("eventListener");

								eventListener.onEvent(null);

							}
						});
					}
				}

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.setSpans(((sam - mul) + 2) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModel, "Dasbor Calon Mahasiswa", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				Session session = HibernateUtil.currentNativeSession();

				int i = 1;
				for (final Jurusan jurusan : jurusans) {
					label.setValue("Sedang memproses data di prodi " + jurusan.getNama() + " ("
							+ Common.numberFormat.get().format((i * 100.0) / jurusans.size()) + ")");
					i++;
					List<Object[]> data = session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

							.add(statusAwalMahasiswa == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("statusAwalMahasiswa", statusAwalMahasiswa))

							.add(program == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("program", program))

							.setProjection(Projections.projectionList().add(Projections.rowCount())
									.add(Projections.groupProperty("tahun")))

							.add(Restrictions.eq(jen, jurusan)).add(Restrictions.between("tahun", mul, sam)).list();

					datas.put(jurusan.getId(), data);
				}
				HibernateUtil.closeSession();

				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}
}
