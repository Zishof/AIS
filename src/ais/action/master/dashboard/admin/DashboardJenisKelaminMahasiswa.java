package ais.action.master.dashboard.admin;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Auxhead;
import org.zkoss.zul.Auxheader;
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

import ais.action.maintenance.MainAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusKeluar;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardJenisKelaminMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Intbox mulai;
	private Intbox sampai;
	private Combobox searchStatusAwalMahasiswa;
	private Combobox searchstatus;
	private Combobox searchprogram;
	private Div center;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchTahunAjaran;
	private Combobox searchJenisSemester;
	private int width = 750;
	private int height = 100;
	private Combobox searchStatusKeluar;

	public DashboardJenisKelaminMahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardJenisKelaminMahasiswa(String title, String border, boolean closable) {
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
				"Jenis Kelamin Mahasiswa",
				"Sebaran mahasiswa menurut jenis kelamin, beserta grafiknya.");
		org.zkoss.zk.ui.Component saringanHost = hostPortal[0];
		center = (org.zkoss.zul.Div) hostPortal[1];

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(saringanHost);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Number m = (Number) HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.max("tahunangkatan")).uniqueResult();

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

		searchstatus = new Combobox();
		row.appendChild(new MyLabelConfig("Status/TA/Smt"));

		hbox = new Hbox();
		hbox.setParent(row);

		hbox.appendChild(searchstatus);
		Common.insertComboDanSemua(searchstatus, "nama", StatusMahasiswa.class);
		searchstatus.setCols(1);

		hbox.appendChild(searchTahunAjaran = new Combobox());
		Common.generateTahunAjaran(searchTahunAjaran);
		searchTahunAjaran.setCols(1);

		hbox.appendChild(searchJenisSemester = new Combobox());
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchJenisSemester.appendChild(comboitem);

		Common.selectComboItem(searchJenisSemester,
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		searchJenisSemester.setCols(1);
		searchJenisSemester.setReadonly(true);

		searchTahunAjaran.addEventListener("onChange", eventListener);
		searchJenisSemester.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		searchStatusKeluar = new Combobox();
		row.appendChild(new MyLabelConfig("Status Keluar"));
		row.appendChild(searchStatusKeluar);
		Common.insertComboDanSemua(searchStatusKeluar, "nama", StatusKeluar.class);
		searchStatusKeluar.setWidth("90%");
		row.setParent(rows);
		searchStatusKeluar.addEventListener("onChange", eventListener);

		searchprogram = new Combobox();
		row.appendChild(new MyLabelConfig("Program"));
		Common.initPrograms(searchprogram);
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.setReadonly(true);

		searchStatusAwalMahasiswa.addEventListener("onChange", eventListener);
		searchstatus.addEventListener("onChange", eventListener);
		searchprogram.addEventListener("onChange", eventListener);

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
				UIUtil.downloadGrid(DashboardJenisKelaminMahasiswa.this.grid);
			}
		});

		Common.createDefaultTimerNoBusy(new EventListener() {
			public void onEvent(Event e) throws Exception {
				reload();
			}
		});
	}

	private Criterion criteriaStatus;
	private StatusMahasiswa selectedStatusMahasiswa;
	protected Grid grid;

	private void reload() {
		criteriaStatus = Restrictions.sqlRestriction("true");
		selectedStatusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());

		if (selectedStatusMahasiswa != null) {

			final Label label = Common.displayLoadBar(new EventListener() {

				@SuppressWarnings({})
				@Override
				public void onEvent(Event arg0) throws Exception {
					doreload();
				}
			});

			new Thread(new Runnable() {

				@SuppressWarnings("unchecked")
				@Override
				public void run() {
					try {
						String ta = (String) searchTahunAjaran.getSelectedItem().getValue();
						String jenisSemester = (String) searchJenisSemester.getSelectedItem().getValue();
						List<Long> dataMhs = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.property("id"))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

						int size = dataMhs.size();
						int rowIndex = 1;

						List<Long> mhss = new ArrayList<Long>();

						for (Long generalValueObjectid : dataMhs) {
							Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(),
									generalValueObjectid);
							rowIndex++;
							label.setValue("Sedang memproses status " + mahasiswa.toString() + " ("
									+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

							Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), ta, jenisSemester,
									mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

							KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);

							HistoryStatusMahasiswa historyStatusMahasiswa = Common
									.getHistoryStatusMahasiswa(krsMahasiswa);
							if (selectedStatusMahasiswa == null || (historyStatusMahasiswa != null
									&& historyStatusMahasiswa.getStatusMahasiswa() != null && historyStatusMahasiswa
											.getStatusMahasiswa().getId().equals(selectedStatusMahasiswa.getId()))) {
								mhss.add(mahasiswa.getId());
							}
						}

						if (mhss.isEmpty()) {
							criteriaStatus = Restrictions.sqlRestriction("false");
						} else {
							criteriaStatus = Restrictions.in("id", mhss);
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardJenisKelaminMahasiswa.java:318");
					}
					label.setValue("");
				}
			}).start();

		} else {
			doreload();
		}
	}

	@SuppressWarnings("unchecked")
	private void doreload() {
		Common.clear(center);

		final StatusMahasiswa selectedStatusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());
		final StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) (searchStatusAwalMahasiswa
				.getSelectedItem() == null || searchStatusAwalMahasiswa.getSelectedItem().getValue() == null ? null
						: searchStatusAwalMahasiswa.getSelectedItem().getValue());

		final StatusKeluar statusKeluar = (StatusKeluar) (searchStatusKeluar.getSelectedItem() == null
				|| searchStatusKeluar.getSelectedItem().getValue() == null ? null
						: searchStatusKeluar.getSelectedItem().getValue());

		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

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

				Auxhead auxhead = new Auxhead();
				auxhead.setParent(grid);

				Auxheader auxheader = new Auxheader("Program Studi");
				auxheader.setColspan(2);
				auxheader.setParent(auxhead);

				for (int tahun = mul; tahun <= sam; tahun++) {

					auxheader = new Auxheader(tahun + "");
					auxheader.setColspan(2);
					auxheader.setParent(auxhead);

				}

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
					column = new MyColumnConfig("L");
					column.setAlign("center");
					column.setParent(columns);

					column.setParent(columns);
					column = new MyColumnConfig("P");
					column.setAlign("center");
					column.setParent(columns);
				}

				Rows rows = new Rows();
				rows.setParent(grid);

				SimpleCategoryModel categoryModel = new SimpleCategoryModel();
				categoryModel.clear();

				/* Donat komposisi total mahasiswa Laki-laki vs Perempuan (lintas prodi) dari
				 * data yang SUDAH dimuat & terfilter angkatan — tanpa query tambahan; render
				 * via DashboardUiKit.donut (HTML/CSS, tanpa JFreeChart). */
				double totLaki = 0.0, totPerempuan = 0.0;
				for (Jurusan jurAgg : jurusans) {
					List<Object[]> dataAgg = datas.get(jurAgg.getId());
					if (dataAgg == null) {
						continue;
					}
					for (Object[] o : dataAgg) {
						Object kel = o[2];
						double cnt = (o[0] instanceof Number) ? ((Number) o[0]).doubleValue() : 0.0;
						if (kel != null && "Laki-laki".equalsIgnoreCase(kel.toString())) {
							totLaki += cnt;
						} else if (kel != null && "Perempuan".equalsIgnoreCase(kel.toString())) {
							totPerempuan += cnt;
						}
					}
				}
				java.util.LinkedHashMap<String, Double> komposisiKelamin = new java.util.LinkedHashMap<String, Double>();
				if (totLaki > 0) {
					komposisiKelamin.put("Laki-laki", totLaki);
				}
				if (totPerempuan > 0) {
					komposisiKelamin.put("Perempuan", totPerempuan);
				}
				if (!komposisiKelamin.isEmpty()) {
					center.appendChild(new org.zkoss.zul.Html(ais.ui.util.DashboardUiKit.donut(
							"Komposisi Mahasiswa Laki-laki & Perempuan",
							"Porsi mahasiswa laki-laki dan perempuan dari seluruh program studi sesuai saringan.",
							komposisiKelamin, false, "Belum ada data untuk ditampilkan.")));
				}

				for (final Jurusan jurusan : jurusans) {
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBoldAja(jurusan.getFakultas().getNama()));
					row.appendChild(new MyLabelBoldAja(jurusan.getNama()));

					List<Object[]> data = datas.get(jurusan.getId());

					for (int tahun = mul; tahun <= sam; tahun++) {
						final int thn = tahun;

						Number jumlah = 0;
						for (Object[] o : data) {
							Object tahunangkatan = o[1];
							Object kelamin = o[2];
							if (tahunangkatan != null && Integer.parseInt(tahunangkatan.toString()) == tahun
									&& kelamin != null && kelamin.toString().equalsIgnoreCase("Laki-laki")) {
								jumlah = (Number) o[0];
								break;
							}
						}

						A a = new A(Common.numberFormat.get().format(jumlah));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								Common.displayWindow(
										"/common/dashboard/mahasiswa.zul?jurusan=" + jurusan.getId()
												+ (statusAwalMahasiswa == null ? ""
														: "&statusAwalMahasiswa=" + statusAwalMahasiswa.getId())
												+ (selectedStatusMahasiswa == null ? ""
														: "&selectedStatusMahasiswa=" + selectedStatusMahasiswa.getId())

												+ (statusKeluar == null ? "" : "&statusKeluar=" + statusKeluar.getId())

												+ "&tahunangkatan=" + thn + "&kelamin=Laki-laki"
												+ (program == null ? ""
														: "&program=" + URLEncoder.encode(program, "UTF-8")),
										true, "95%", "95%");

							}
						});

						categoryModel.setValue(jurusan.getNama(), tahun + " Laki-laki", jumlah.intValue());

						jumlah = 0;
						for (Object[] o : data) {
							Object tahunangkatan = o[1];
							Object kelamin = o[2];
							if (tahunangkatan != null && Integer.parseInt(tahunangkatan.toString()) == tahun
									&& kelamin != null && kelamin.toString().equalsIgnoreCase("Perempuan")) {
								jumlah = (Number) o[0];
								break;
							}
						}

						a = new A(Common.numberFormat.get().format(jumlah));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								Common.displayWindow(
										"/common/dashboard/mahasiswa.zul?jurusan=" + jurusan.getId()
												+ (statusAwalMahasiswa == null ? ""
														: "&statusAwalMahasiswa=" + statusAwalMahasiswa.getId())
												+ (selectedStatusMahasiswa == null ? ""
														: "&selectedStatusMahasiswa=" + selectedStatusMahasiswa.getId())

												+ (statusKeluar == null ? "" : "&statusKeluar=" + statusKeluar.getId())

												+ "&tahunangkatan=" + thn + "&kelamin=Perempuan"
												+ (program == null ? ""
														: "&program=" + URLEncoder.encode(program, "UTF-8")),
										true, "95%", "95%");

							}
						});

						categoryModel.setValue(jurusan.getNama(), tahun + " Perempuan", jumlah.intValue());

					}
				}

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.setSpans((((sam - mul) * 2) + 2) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModel, "Dasbor Jenis Kelamin Mahasiswa", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
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
					List<Object[]> data = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(criteriaStatus)

							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

							.add(statusAwalMahasiswa == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("statusAwalMahasiswa", statusAwalMahasiswa))

							.add(statusKeluar == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("statusKeluar", statusKeluar))

							.add(program == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("program", program))

							.setProjection(Projections.projectionList().add(Projections.rowCount())
									.add(Projections.groupProperty("tahunangkatan"))
									.add(Projections.groupProperty("kelamin")))

							.add(Restrictions.eq("jurusan", jurusan))
							.add(Restrictions.between("tahunangkatan", mul, sam)).list();

					datas.put(jurusan.getId(), data);
				}
				// session.disconnect();
				if (session.isOpen()) {session.disconnect();session.close();}
				HibernateUtil.closeSession();

				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}
}
