package ais.action.master.dashboard.admin;

import java.io.Serializable;
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

import ais.action.master.helper.MahasiswaPunyaKegiatanKemahasiswaanHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailKelompokKegiatanKemahasiswaan;
import ais.database.model.Fakultas;
import ais.database.model.JabatanKegiatanKemahasiswaan;
import ais.database.model.Jurusan;
import ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa;
import ais.database.model.KelompokKegiatanKemahasiswaan;
import ais.database.model.SkalaKegiatanKemahasiswaan;
import ais.database.model.StatusKeluar;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardKegiatanKemahasiswaanUmum extends MyWindow {

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

	public DashboardKegiatanKemahasiswaanUmum() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardKegiatanKemahasiswaanUmum(String title, String border, boolean closable) {
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
				"Kegiatan Kemahasiswaan",
				"Rekap kegiatan kemahasiswaan, beserta grafiknya.");
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
				UIUtil.downloadGrid(DashboardKegiatanKemahasiswaanUmum.this.grid);
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

		final List<String> tas = new ArrayList<String>();
		for (int tahun = mul; tahun <= sam; tahun++) {
			final String tahunAjaran = tahun + "/" + (tahun + 1);
			tas.add(tahunAjaran);
		}

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

				MyColumnConfig column = new MyColumnConfig("Aspek kegiatan");
				column.setParent(columns);
				column.setWidth("15%");

				column.setParent(columns);
				column = new MyColumnConfig("Detail Kegiatan");
				column.setParent(columns);
				column.setWidth("15%");

				column.setParent(columns);
				column = new MyColumnConfig("Jabatan");
				column.setParent(columns);
				column.setWidth("8%");

				column.setParent(columns);
				column = new MyColumnConfig("Skala");
				column.setParent(columns);
				column.setWidth("8%");

				for (String tahunAjaran : tas) {
					column.setParent(columns);
					column = new MyColumnConfig(tahunAjaran);
					column.setAlign("center");
					column.setParent(columns);
				}

				Rows rows = new Rows();
				rows.setParent(grid);

				SimpleCategoryModel categoryModel = new SimpleCategoryModel();
				categoryModel.clear();

				for (Object[] d : datas) {
					final KelompokKegiatanKemahasiswaan kelompokKegiatanKemahasiswaan = (KelompokKegiatanKemahasiswaan) d[0];
					final DetailKelompokKegiatanKemahasiswaan detailKelompokKegiatanKemahasiswaan = (DetailKelompokKegiatanKemahasiswaan) d[1];
					final JabatanKegiatanKemahasiswaan jabatanKegiatanKemahasiswaan = (JabatanKegiatanKemahasiswaan) d[2];
					final SkalaKegiatanKemahasiswaan skalaKegiatanKemahasiswaan = (SkalaKegiatanKemahasiswaan) d[3];

					TreeMap<String, Number> nilais = (TreeMap<String, Number>) d[4];

					MyFormRow row = new MyFormRow();
		row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBoldAja(kelompokKegiatanKemahasiswaan.getNama()));
					row.appendChild(new MyLabelBoldAja(detailKelompokKegiatanKemahasiswaan.getNama()));
					row.appendChild(new MyLabelBoldAja(
							jabatanKegiatanKemahasiswaan == null ? "" : jabatanKegiatanKemahasiswaan.getNama()));
					row.appendChild(new MyLabelBoldAja(
							skalaKegiatanKemahasiswaan == null ? "" : skalaKegiatanKemahasiswaan.getNama()));

					String nama = kelompokKegiatanKemahasiswaan.getNama() + "-"
							+ detailKelompokKegiatanKemahasiswaan.getNama();
					String jabatan = (jabatanKegiatanKemahasiswaan == null ? ""
							: jabatanKegiatanKemahasiswaan.getNama()) + "-"
							+ (skalaKegiatanKemahasiswaan == null ? "" : skalaKegiatanKemahasiswaan.getNama());

					for (final String tahunAjaran : tas) {
						Number number = nilais.get(tahunAjaran);
						Integer jumlah = number == null ? 0 : number.intValue();

						categoryModel.setValue(nama + jabatan, tahunAjaran, jumlah);

						A a = new A(Common.numberFormat.get().format(jumlah));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								final MyWindow window = new MyWindow("Data kegiatan Kemahasiswaan", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("750px");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								Center center = new Center();
								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								MahasiswaPunyaKegiatanKemahasiswaanHelper detailperkuliahanHelper = new MahasiswaPunyaKegiatanKemahasiswaanHelper(
										kelompokKegiatanKemahasiswaan, detailKelompokKegiatanKemahasiswaan,
										jabatanKegiatanKemahasiswaan, skalaKegiatanKemahasiswaan, tahunAjaran);
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
				row.setSpans((tas.size() + 4) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModel, String.valueOf("Kegiatan Kemahasiswaan"), "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				Session session = HibernateUtil.currentNativeSession();

				List<Object[]> data = session.createCriteria(KegiatanKemahasiswaanPunyaMahasiswa.class)

						.createAlias("kegiatanKemahasiswaan", "kegiatanKemahasiswaan")

						.setProjection(Projections.projectionList()
								.add(Projections
										.groupProperty("kegiatanKemahasiswaan.kelompokKegiatanKemahasiswaan.id"))
								.add(Projections
										.groupProperty("kegiatanKemahasiswaan.detailKelompokKegiatanKemahasiswaan.id"))
								.add(Projections.groupProperty("jabatanKegiatanKemahasiswaan.id"))
								.add(Projections.groupProperty("skalaKegiatanKemahasiswaan.id"))
								.add(Projections.groupProperty("kegiatanKemahasiswaan.tahunAkademik"))
								.add(Projections.rowCount()))

						.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
						.add(jur == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("mahasiswa.jurusan", jur))
						.add(fak == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jurusan.fakultas", fak))
						.add(program == null || program.trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("mahasiswa.program", program))
						.add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"),
								Restrictions.eq("mahasiswa.aktif", true)))

						.add(statusKeluar == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("mahasiswa.statusKeluar", statusKeluar))

						.add(Restrictions.in("kegiatanKemahasiswaan.tahunAkademik", tas))

						.addOrder(Order.asc("kegiatanKemahasiswaan.kelompokKegiatanKemahasiswaan"))
						.addOrder(Order.asc("kegiatanKemahasiswaan.detailKelompokKegiatanKemahasiswaan"))
						.addOrder(Order.asc("jabatanKegiatanKemahasiswaan"))
						.addOrder(Order.asc("skalaKegiatanKemahasiswaan"))
						.addOrder(Order.asc("kegiatanKemahasiswaan.tahunAkademik"))

						.list();

				List<String> kodes = new ArrayList<String>();
				TreeMap<String, Number> nilais = null;
				for (Object[] d : data) {
					KelompokKegiatanKemahasiswaan kelompokKegiatanKemahasiswaan = (KelompokKegiatanKemahasiswaan) ConstantValues
							.ambil(KelompokKegiatanKemahasiswaan.class.getName(), (Serializable) d[0]);
					DetailKelompokKegiatanKemahasiswaan detailKelompokKegiatanKemahasiswaan = (DetailKelompokKegiatanKemahasiswaan) ConstantValues
							.ambil(DetailKelompokKegiatanKemahasiswaan.class.getName(), (Serializable) d[1]);
					JabatanKegiatanKemahasiswaan jabatanKegiatanKemahasiswaan = (JabatanKegiatanKemahasiswaan) ConstantValues
							.ambil(JabatanKegiatanKemahasiswaan.class.getName(), (Serializable) d[2]);
					SkalaKegiatanKemahasiswaan skalaKegiatanKemahasiswaan = (SkalaKegiatanKemahasiswaan) ConstantValues
							.ambil(SkalaKegiatanKemahasiswaan.class.getName(), (Serializable) d[3]);

					String kodeUnik = kelompokKegiatanKemahasiswaan.getId() + "-"
							+ detailKelompokKegiatanKemahasiswaan.getId() + "-" + skalaKegiatanKemahasiswaan.getId()
							+ "-" + (jabatanKegiatanKemahasiswaan == null ? "" : jabatanKegiatanKemahasiswaan.getId());

					if (!kodes.contains(kodeUnik)) {
						nilais = new TreeMap<String, Number>();
						datas.add(new Object[] { kelompokKegiatanKemahasiswaan, detailKelompokKegiatanKemahasiswaan,
								jabatanKegiatanKemahasiswaan, skalaKegiatanKemahasiswaan, nilais });
						kodes.add(kodeUnik);
					}
					String tahunAkademik = (String) d[4];
					Number jumlah = (Number) d[5];
					nilais.put(tahunAkademik, jumlah);
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
