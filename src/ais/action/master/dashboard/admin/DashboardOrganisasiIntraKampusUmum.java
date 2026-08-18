package ais.action.master.dashboard.admin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
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
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.MahasiswaPunyaOrganisasiIntraKampusHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.JabatanOrganisasiIntraKampus;
import ais.database.model.Jurusan;
import ais.database.model.OrganisasiIntraKampus;
import ais.database.model.OrganisasiIntraKampusPunyaMahasiswa;
import ais.database.model.StatusKeluar;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardOrganisasiIntraKampusUmum extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Div center;
	private Intbox mulai;
	private Intbox sampai;
	private Combobox searchprogram;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	protected Grid grid;
	private int width = 750;
	private int height = 100;
	private Combobox searchStatusKeluar;

	public DashboardOrganisasiIntraKampusUmum() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardOrganisasiIntraKampusUmum(String title, String border, boolean closable) {
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

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		org.zkoss.zk.ui.Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih saringan untuk menyesuaikan data yang ditampilkan.",
				"Organisasi Intra Kampus",
				"Rekap organisasi intra kampus mahasiswa, beserta grafiknya.");
		org.zkoss.zk.ui.Component saringanHost = hostPortal[0];
		center = (org.zkoss.zul.Div) hostPortal[1];

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(saringanHost);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Tahun Akademik"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}
		};

		mulai = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 3);
		mulai.setCols(4);
		hbox.appendChild(mulai);

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));

		sampai = new Intbox(mulai.getValue() + 3);
		sampai.setCols(4);
		hbox.appendChild(sampai);

		mulai.addEventListener("onChange", eventListener);
		sampai.addEventListener("onChange", eventListener);

		searchprogram = new Combobox();
		row.appendChild(new MyLabelConfig("Program"));
		Common.initPrograms(searchprogram);
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.setReadonly(true);

		searchprogram.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		searchStatusKeluar = new Combobox();
		row.appendChild(new MyLabelConfig("Status Keluar"));
		row.appendChild(searchStatusKeluar);
		Common.insertComboDanSemua(searchStatusKeluar, "nama", StatusKeluar.class);
		searchStatusKeluar.setWidth("90%");
		row.setParent(rows);searchStatusKeluar.addEventListener("onChange", eventListener);

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



		row = new MyFormRow();
		row.setParent(rows);
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
		toolbarbutton.setParent(row);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UIUtil.downloadGrid(DashboardOrganisasiIntraKampusUmum.this.grid);
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

		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());
		final StatusKeluar statusKeluar = (StatusKeluar) (searchStatusKeluar.getSelectedItem() == null
				|| searchStatusKeluar.getSelectedItem().getValue() == null ? null
						: searchStatusKeluar.getSelectedItem().getValue());
		final Fakultas fak = (Fakultas) (searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue());
		final Jurusan jur = (Jurusan) (searchjurusan.getSelectedItem() == null ? null
				: searchjurusan.getSelectedItem().getValue());

		final int mul = mulai.getValue() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 7
				: mulai.getValue();
		final int sam = sampai.getValue() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
				: sampai.getValue();

		final List<Object[]> datas = new ArrayList<Object[]>();

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

				MyColumnConfig column = new MyColumnConfig("Organisasi");
				column.setParent(columns);
				column.setWidth("15%");

				column.setParent(columns);
				column = new MyColumnConfig("Jabatan");
				column.setParent(columns);
				column.setWidth("10%");

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

				for (Object[] d : datas) {
					final OrganisasiIntraKampus organisasiIntraKampus = (OrganisasiIntraKampus) d[0];
					final JabatanOrganisasiIntraKampus jabatanOrganisasiIntraKampus = (JabatanOrganisasiIntraKampus) d[1];

					TreeMap<Integer, Number> nilais = (TreeMap<Integer, Number>) d[2];

					MyFormRow row = new MyFormRow();
		row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBoldAja(organisasiIntraKampus.getNama()));
					row.appendChild(new MyLabelBoldAja(
							jabatanOrganisasiIntraKampus == null ? "" : jabatanOrganisasiIntraKampus.getNama()));

					String nama = organisasiIntraKampus.getNama() + (jabatanOrganisasiIntraKampus == null ? ""
							: "-" + jabatanOrganisasiIntraKampus.getNama());

					for (int tahun = mul; tahun <= sam; tahun++) {
						final int thn = tahun;
						Number number = nilais.get(tahun);
						Integer jumlah = number == null ? 0 : number.intValue();

						categoryModel.setValue(nama, tahun, jumlah);

						A a = new A(Common.numberFormat.get().format(jumlah));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								final MyWindow window = new MyWindow("Data Organisasi Mahasiswa", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("750px");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								Center center = new Center();
								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								MahasiswaPunyaOrganisasiIntraKampusHelper detailperkuliahanHelper = new MahasiswaPunyaOrganisasiIntraKampusHelper(
										organisasiIntraKampus, jabatanOrganisasiIntraKampus, thn);
								detailperkuliahanHelper.display(null, center);

								South south = new South();
								ais.ui.util.ZkCompat.setFlex(south, true);
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								// toolbar.setHeight("25px");
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
								cancel.setTooltiptext("Tutup");
								cancel.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								window.onModal();
							}
						});
					}
				}

				MyFormRow row = new MyFormRow();
		row.setValign("top");
				row.setParent(rows);
				row.setSpans(((sam - mul) + 2) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModel, String.valueOf("Organisasi Mahasiswa"), "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				Session session = HibernateUtil.currentNativeSession();

				List<Object[]> data = session.createCriteria(OrganisasiIntraKampusPunyaMahasiswa.class)

						.setProjection(
								Projections.projectionList().add(Projections.groupProperty("organisasiIntraKampus"))
										.add(Projections.groupProperty("jabatanOrganisasiIntraKampus"))
										.add(Projections.groupProperty("tahun")).add(Projections.rowCount()))

						.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
						.add(jur == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("mahasiswa.jurusan", jur))
						.add(fak == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jurusan.fakultas", fak))
						.add(program == null || program.trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("mahasiswa.program", program))
						.add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"),
								Restrictions.eq("mahasiswa.aktif", true)))

						.add(Restrictions.between("tahun", mul, sam))

						.add(statusKeluar == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("mahasiswa.statusKeluar", statusKeluar))

						.addOrder(Order.asc("organisasiIntraKampus"))
						.addOrder(Order.asc("jabatanOrganisasiIntraKampus")).addOrder(Order.asc("tahun"))

						.list();

				List<String> kodes = new ArrayList<String>();
				TreeMap<Integer, Number> nilais = null;
				for (Object[] d : data) {
					OrganisasiIntraKampus organisasiIntraKampus = (OrganisasiIntraKampus) d[0];
					JabatanOrganisasiIntraKampus jabatanOrganisasiIntraKampus = (JabatanOrganisasiIntraKampus) d[1];

					String kodeUnik = organisasiIntraKampus.getId()
							+ (jabatanOrganisasiIntraKampus == null ? "" : "-" + jabatanOrganisasiIntraKampus.getId());

					if (!kodes.contains(kodeUnik)) {
						nilais = new TreeMap<Integer, Number>();
						datas.add(new Object[] { organisasiIntraKampus, jabatanOrganisasiIntraKampus, nilais });
						kodes.add(kodeUnik);
					}
					Integer tahun = (Integer) d[2];
					Number jumlah = (Number) d[3];
					nilais.put(tahun, jumlah);
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
