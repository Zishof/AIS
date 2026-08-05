package ais.action.master.dashboard.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.Type;
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
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.NilaiHuruf;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusKeluar;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardNilaiMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	// private Intbox mulai;
	// private Intbox sampai;
	private Combobox searchStatusAwalMahasiswa;
	private Combobox searchprogram;
	private Div center;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchTahunAjaran;
	protected Grid grid;
	private int width = 750;
	private int height = 100;
	private Combobox searchStatusKeluar;

	public DashboardNilaiMahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardNilaiMahasiswa(String title, String border, boolean closable) {
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
				"Nilai Mahasiswa",
				"Sebaran nilai mahasiswa, beserta grafiknya.");
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

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}
		};

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("TA"));

		row.appendChild(searchTahunAjaran = new Combobox());
		searchTahunAjaran.setWidth("90%");
		Common.generateTahunAjaran(searchTahunAjaran);
		searchTahunAjaran.addEventListener("onChange", eventListener);

		searchStatusAwalMahasiswa = new Combobox();
		row.appendChild(new MyLabelConfig("Status Awal"));
		row.appendChild(searchStatusAwalMahasiswa);
		Common.insertComboDanSemua(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		searchStatusAwalMahasiswa.setWidth("90%");

		row = new MyFormRow();
		searchStatusKeluar = new Combobox();
		row.appendChild(new MyLabelConfig("Status Keluar"));
		row.appendChild(searchStatusKeluar);
		Common.insertComboDanSemua(searchStatusKeluar, "nama", StatusKeluar.class);
		searchStatusKeluar.setWidth("90%");
		row.setParent(rows);searchStatusKeluar.addEventListener("onChange", eventListener);

		searchprogram = new Combobox();
		row.appendChild(new MyLabelConfig("Program"));
		Common.initPrograms(searchprogram);
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.setReadonly(true);

		searchStatusAwalMahasiswa.addEventListener("onChange", eventListener);
		searchprogram.addEventListener("onChange", eventListener);

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



		row = new MyFormRow();
		row.setParent(rows);
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
		toolbarbutton.setParent(row);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UIUtil.downloadGrid(DashboardNilaiMahasiswa.this.grid);
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
		final StatusKeluar statusKeluar = (StatusKeluar) (searchStatusKeluar.getSelectedItem() == null
				|| searchStatusKeluar.getSelectedItem().getValue() == null ? null
						: searchStatusKeluar.getSelectedItem().getValue());
		final String ta = (String) (searchTahunAjaran.getSelectedItem() == null
				|| searchTahunAjaran.getSelectedItem().getValue() == null ? null
						: searchTahunAjaran.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();
		List<String> nilaisTemp = session.createCriteria(NilaiHuruf.class).add(Restrictions.isNotNull("nilaiHuruf"))
				.setProjection(Projections.property("nilaiHuruf")).addOrder(Order.desc("nilaiDiIPK")).list();
		final List<String> nilais = new ArrayList<String>();
		for (String n : nilaisTemp) {
			if (!nilais.contains(n.toUpperCase().trim())) {
				nilais.add(n.toUpperCase().trim());
			}
		}
		nilais.add("Belum");

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

				auxheader = new Auxheader("Ganjil");
				auxheader.setColspan(nilais.size());
				auxheader.setParent(auxhead);

				auxheader = new Auxheader("Genap");
				auxheader.setColspan(nilais.size());
				auxheader.setParent(auxhead);

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig("Fakultas");
				column.setParent(columns);
				column.setWidth("15%");

				column.setParent(columns);
				column = new MyColumnConfig("Jurusan");
				column.setParent(columns);
				column.setWidth("15%");

				for (String n : nilais) {
					column.setParent(columns);
					column = new MyColumnConfig(n);
					column.setAlign("center");
					column.setParent(columns);
				}

				for (String n : nilais) {
					column.setParent(columns);
					column = new MyColumnConfig(n);
					column.setAlign("center");
					column.setParent(columns);
				}

				Rows rows = new Rows();
				rows.setParent(grid);

				SimpleCategoryModel categoryModelGanjil = new SimpleCategoryModel();
				categoryModelGanjil.clear();

				SimpleCategoryModel categoryModelGenap = new SimpleCategoryModel();
				categoryModelGenap.clear();

				for (final Jurusan jurusan : jurusans) {
					MyFormRow row = new MyFormRow();
		row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBoldAja(jurusan.getFakultas().getNama()));
					row.appendChild(new MyLabelBoldAja(jurusan.getNama()));

					List<Object[]> jumlahs = datas.get(jurusan.getId());

					for (final String n : nilais) {

						Number jumlah = 0;
						for (Object[] o : jumlahs) {
							Object nilaiHuruf = o[2];
							if (nilaiHuruf != null && nilaiHuruf.toString().equalsIgnoreCase(n)) {
								jumlah = (Number) o[0];
								break;
							} else if (n.equalsIgnoreCase("Belum")
									&& (nilaiHuruf == null || nilaiHuruf.toString().trim().isEmpty())) {
								jumlah = (Number) o[0];
							}
						}

						A a = new A(Common.numberFormat.get().format(jumlah == null ? 0 : jumlah.intValue()));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								EventListener eventListener = (EventListener) Common
										.cetakDataCustomButton(Detailperkuliahan.class, new DataCriteriaWithColumn() {

											@Override
											public Object[] initCriteria(boolean order) {

												try {

													Criteria criteria = HibernateUtil.currentSession()
															.createCriteria(Detailperkuliahan.class)
															.add(Restrictions.eq("persetujuan",
																	Detailperkuliahan.DISETUJUI))
															.add(Restrictions.sqlRestriction("this_.semester%2=1"))
															.add(n.equals("Belum") ? Restrictions.lt("totalNilai", 0.1)
																	: Restrictions.ilike("nilaiHuruf", n))
															.add(Restrictions.eq("tahunAkademik", ta))
															.createAlias("mahasiswa", "mahasiswa")

															.add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"),
																	Restrictions.eq("mahasiswa.aktif", true)))
															.add(statusAwalMahasiswa == null
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("mahasiswa.statusAwalMahasiswa",
																			statusAwalMahasiswa))

															.add(statusKeluar == null
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("mahasiswa.statusKeluar",
																			statusKeluar))

															.add(program == null ? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("mahasiswa.program", program))

															.add(Restrictions.eq("mahasiswa.jurusan", jurusan));

													return new Object[] { criteria,
															new String[] { "mahasiswa.nim", "mahasiswa.nama", "nama",
																	"semester", "totalNilai", "nilaiHuruf" } };

												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
												}
												return null;
											}

										}, null, "Download Data", "/img/print.png", null, null, false, null,
												"DATA TAMBAHAN",
												new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "" })
										.getAttribute("eventListener");

								eventListener.onEvent(null);

							}
						});

						categoryModelGanjil.setValue(jurusan.getNama(), n, jumlah.intValue());

					}
					for (final String n : nilais) {

						Number jumlah = 0;
						for (Object[] o : jumlahs) {
							Object nilaiHuruf = o[2];
							if (nilaiHuruf != null && nilaiHuruf.toString().equalsIgnoreCase(n)) {
								jumlah = (Number) o[1];
								break;
							} else if (n.equalsIgnoreCase("Belum")
									&& (nilaiHuruf == null || nilaiHuruf.toString().trim().isEmpty())) {
								jumlah = (Number) o[1];
							}
						}

						A a = new A(Common.numberFormat.get().format(jumlah == null ? 0 : jumlah.intValue()));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								EventListener eventListener = (EventListener) Common
										.cetakDataCustomButton(Detailperkuliahan.class, new DataCriteriaWithColumn() {

											@Override
											public Object[] initCriteria(boolean order) {

												try {

													Criteria criteria = HibernateUtil.currentSession()
															.createCriteria(Detailperkuliahan.class)
															.add(Restrictions.eq("persetujuan",
																	Detailperkuliahan.DISETUJUI))
															.add(Restrictions.sqlRestriction("this_.semester%2=0"))
															.add(n.equals("Belum") ? Restrictions.lt("totalNilai", 0.1)
																	: Restrictions.ilike("nilaiHuruf", n))
															.add(Restrictions.eq("tahunAkademik", ta))
															.createAlias("mahasiswa", "mahasiswa")

															.add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"),
																	Restrictions.eq("mahasiswa.aktif", true)))
															.add(statusAwalMahasiswa == null
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("mahasiswa.statusAwalMahasiswa",
																			statusAwalMahasiswa))

															.add(statusKeluar == null
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("mahasiswa.statusKeluar",
																			statusKeluar))

															.add(program == null ? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("mahasiswa.program", program))

															.add(Restrictions.eq("mahasiswa.jurusan", jurusan));

													return new Object[] { criteria,
															new String[] { "mahasiswa.nim", "mahasiswa.nama", "nama",
																	"semester", "totalNilai", "nilaiHuruf" } };

												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
												}
												return null;
											}

										}, null, "Download Data", "/img/print.png", null, null, false, null,
												"DATA TAMBAHAN",
												new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "" })
										.getAttribute("eventListener");

								eventListener.onEvent(null);

							}
						});

						categoryModelGenap.setValue(jurusan.getNama(), n, jumlah.intValue());

					}

				}

				MyFormRow row = new MyFormRow();
		row.setValign("top");
				row.setParent(rows);
				row.setSpans(((nilais.size() * 2) + 2) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModelGanjil, String.valueOf("Nilai Semester Ganjil"), "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
row = new MyFormRow();
				row.setParent(rows);
				row.setSpans(((nilais.size() * 2) + 2) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModelGenap, String.valueOf("Nilai Semester Genap"), "Sebaran nilai semester genap ditampilkan agar perkembangan hasil belajar mudah dibandingkan.", String.valueOf("bar")));


			}
		});

		new Thread(new Runnable() {

			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				try {

				Session session = HibernateUtil.currentNativeSession();

				int i = 1;
				for (final Jurusan jurusan : jurusans) {
					label.setValue("Sedang memproses data di prodi " + jurusan.getNama() + " ("
							+ Common.numberFormat.get().format((i * 100.0) / jurusans.size()) + ")");
					i++;

					String sql = "sum(case when this_.semester%2=1 then 1 else 0 end) as ganjil,"
							+ "sum(case when this_.semester%2=0 then 1 else 0 end) as genap";

					List<Object[]> jumlahs = session.createCriteria(Detailperkuliahan.class)
							.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
							.add(Restrictions.eq("tahunAkademik", ta)).createAlias("mahasiswa", "mahasiswa")

							.add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"),
									Restrictions.eq("mahasiswa.aktif", true)))
							.add(statusAwalMahasiswa == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("mahasiswa.statusAwalMahasiswa", statusAwalMahasiswa))

							.add(statusKeluar == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("mahasiswa.statusKeluar", statusKeluar))

							.add(program == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("mahasiswa.program", program))
							.setProjection(Projections.projectionList()
									.add(Projections.sqlProjection(sql, new String[] { "ganjil", "genap" },
											new Type[] { org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE }))
									.add(Projections.groupProperty("nilaiHuruf")))
							.add(Restrictions.eq("mahasiswa.jurusan", jurusan)).list();

					datas.put(jurusan.getId(), jumlahs);
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
