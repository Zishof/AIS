package ais.action.master.dashboard.admin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.action.master.MahasiswaAction;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

public class DashboardPencapaianPerkuliahan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Div center;
	private Combobox searchTahunAjaran;
	private Combobox searchprogram;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchJenisSemester;
	protected Grid grid;

	public DashboardPencapaianPerkuliahan() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardPencapaianPerkuliahan(String title, String border, boolean closable) {
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
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih saringan untuk menyesuaikan data yang ditampilkan.",
				"Pencapaian Perkuliahan",
				"Capaian materi/pertemuan perkuliahan per kelas, beserta grafiknya.");
		Component saringanHost = hostPortal[0];
		center = (Div) hostPortal[1];

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(saringanHost);
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

		Row row = new Row();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("TA/Smt"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		hbox.appendChild(searchTahunAjaran = new Combobox());
		Common.generateTahunAjaran(searchTahunAjaran);
		searchTahunAjaran.setCols(4);

		hbox.appendChild(searchJenisSemester = new Combobox());
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.SP);
		comboitem.setValue(Perkuliahan.SP);
		searchJenisSemester.appendChild(comboitem);

		Common.selectComboItem(searchJenisSemester,
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		searchJenisSemester.setCols(3);
		searchJenisSemester.setReadonly(true);

		searchTahunAjaran.addEventListener("onChange", eventListener);
		searchJenisSemester.addEventListener("onChange", eventListener);

		searchTahunAjaran.setReadonly(true);
		searchJenisSemester.setReadonly(true);

		searchprogram = new Combobox();
		row.appendChild(new MyLabelConfig("Program"));
		Common.initPrograms(searchprogram);
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.setReadonly(true);

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

		row = new Row();
		row.setParent(rows);
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
		toolbarbutton.setParent(row);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UIUtil.downloadGrid(DashboardPencapaianPerkuliahan.this.grid);
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

		final String smt = (String) (searchJenisSemester.getSelectedItem() == null
				|| searchJenisSemester.getSelectedItem().getValue() == null
						? (Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP)
						: searchJenisSemester.getSelectedItem().getValue());

		final Fakultas fak = (Fakultas) (searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue());
		final Jurusan jur = (Jurusan) (searchjurusan.getSelectedItem() == null ? null
				: searchjurusan.getSelectedItem().getValue());

		final TreeMap<Dosen, List<Perkuliahan>> dosensMap = new TreeMap<Dosen, List<Perkuliahan>>();

		final Label label = Common.displayLoadBar(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(center);

				grid = new Grid();
				grid.setSclass("dgrid");
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig("No.");
				column.setParent(columns);
				column.setWidth("5%");

				column = new MyColumnConfig("Dosen");
				column.setParent(columns);
				column.setWidth("15%");

				column = new MyColumnConfig("Matakuliah");
				column.setParent(columns);

				column.setParent(columns);
				column = new MyColumnConfig("SKS");
				column.setParent(columns);
				column.setWidth("5%");

				column.setParent(columns);
				column = new MyColumnConfig("Kelas");
				column.setParent(columns);
				column.setWidth("10%");

				column.setParent(columns);
				column = new MyColumnConfig("Smt");
				column.setParent(columns);
				column.setWidth("8%");

				column.setParent(columns);
				column = new MyColumnConfig("Jml. Mhs");
				column.setParent(columns);
				column.setWidth("8%");

				column.setParent(columns);
				column = new MyColumnConfig("Jml Pert.");
				column.setParent(columns);
				column.setWidth("8%");

				column.setParent(columns);
				column = new MyColumnConfig("Jml.Pencapaian");
				column.setParent(columns);
				column.setWidth("8%");

				column.setParent(columns);
				column = new MyColumnConfig("Jml.Absen");
				column.setParent(columns);
				column.setWidth("8%");

				column.setParent(columns);
				column = new MyColumnConfig("Jml.Diskusi");
				column.setParent(columns);
				column.setWidth("8%");

				Rows rows = new Rows();
				rows.setParent(grid);

				Session session = HibernateUtil.currentSession();
				int indeks = 1;
				for (final Dosen dosen : dosensMap.keySet()) {

					Row row = new Row();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBoldAja(indeks + ""));
					row.appendChild(new MyLabelBoldAja(dosen.getNama()));

					List<Perkuliahan> t1 = dosensMap.get(dosen);

					int i = 0;
					for (final Perkuliahan perkuliahan : t1) {
						if (i > 0) {
							row = new Row();
							row.setParent(rows);
							row.appendChild(new MyLabelBoldAja());
							row.appendChild(new MyLabelBoldAja());
						}

						row.appendChild(new Label(
								perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama()));

						row.appendChild(new Label(
								perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getSks() + ""));

						row.appendChild(new Label(perkuliahan.getKelas()));

						row.appendChild(new Label(perkuliahan.getSemester() + ""));

						Integer countSudahDisetujui = ((Number) session.createCriteria(Detailperkuliahan.class)
								.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
								.add(Restrictions.eq("perkuliahan", perkuliahan)).setProjection(Projections.rowCount())
								.uniqueResult()).intValue();
						A a;
						row.appendChild(a = new A(Common.numberFormat.get().format(countSudahDisetujui)));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								EventListener eventListener = (EventListener) Common
										.cetakDataCustomButton(Mahasiswa.class, new DataCriteriaWithColumn() {

											@Override
											public Object[] initCriteria(boolean order) {

												try {

													Criteria criteria = HibernateUtil.currentSession()
															.createCriteria(Detailperkuliahan.class)
															.add(Restrictions.eq("persetujuan",
																	Detailperkuliahan.DISETUJUI))
															.add(Restrictions.eq("perkuliahan", perkuliahan))
															.createCriteria("mahasiswa").addOrder(Order.asc("nim"));

													return new Object[] { criteria, MahasiswaAction.contents };

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

						List<Pertemuan> pertemuans = perkuliahan.ambilPertemuanList();

						row.appendChild(a = new A(Common.numberFormat.get().format(pertemuans.size())));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								EventListener eventListener = (EventListener) Common
										.cetakDataCustomButton(Pertemuan.class, new DataCriteriaWithColumn() {

											@Override
											public Object[] initCriteria(boolean order) {

												try {

													Criteria criteria = HibernateUtil.currentSession()
															.createCriteria(Pertemuan.class)
															.add(Restrictions.eq("perkuliahan", perkuliahan))
															.addOrder(Order.asc("pertemuanKe"));

													String[] contents = new String[] { "id", "indikator", "topik",
															"metodePembelajaran", "pengalamanBelajar",
															"waktupembelajaran", "tugasDanPenilaian", "catatan",
															"bukuRujukan1", "bukuRujukan2", "dosenTamu", "dosenTamu",
															"tanggal", "statusPertemuan", "ruang", "waktuMulai",
															"waktuSelesai" };

													return new Object[] { criteria, contents };

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

						int jumlahBerlalu = 0;
						int jumlahAbsensi = 0;
						Date sekarang = ais.ui.util.WaktuUtil.getDate();
						for (Pertemuan pertemuan : pertemuans) {

							if (pertemuan != null) {
								if (pertemuan.getAktif()) {
									if (pertemuan.getTanggal() != null && pertemuan.getTanggal().before(sekarang)) {
										jumlahBerlalu++;
									}
									if (pertemuan.getAbsensi() != null && !pertemuan.getAbsensi().isEmpty()) {
										jumlahAbsensi++;
									}
								}
							}
						}

						row.appendChild(a = new A(Common.numberFormat.get().format(jumlahBerlalu)));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								CommonReportHelper.onLaporanAbsensi(perkuliahan, true);

							}
						});

						row.appendChild(a = new A(Common.numberFormat.get().format(jumlahAbsensi)));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								CommonReportHelper.onLaporanAbsensi(perkuliahan, true, 7);

							}
						});

						row.appendChild(a = new A(
								Common.numberFormat.get().format((jumlahBerlalu * 100.0) / pertemuans.size()) + "%"));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								CommonReportHelper.onLaporanAbsensi(perkuliahan, true, 9);

							}
						});

						pertemuans = null;
						i++;

					}

					indeks++;
				}

			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				Session session = HibernateUtil.currentNativeSession();

				String tahunAjaran = (String) searchTahunAjaran.getSelectedItem().getValue();

				List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(smt.equalsIgnoreCase(Perkuliahan.SP)
								? Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
								: Restrictions
										.sqlRestriction("semester%2=" + (smt.equals(Perkuliahan.GENAP) ? "0" : "1")))

						.add(jur == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", jur))

						.createAlias("jurusan", "jurusan")

						.add(fak == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jurusan.fakultas", fak))

						.add(Restrictions.isNotNull("dosen1"))

						.add(Restrictions.isNull("perkuliahan_paralel"))
						.add(program == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("program", program))

						.add(Restrictions.eq("tahunAjaran", tahunAjaran)).list();

				for (Perkuliahan perkuliahan : perkuliahans) {

					Map<String, Dosen> map = perkuliahan.populateDosen();
					for (Dosen d : map.values()) {
						if (dosensMap.containsKey(d)) {
							List<Perkuliahan> treeMap = dosensMap.get(d);
							treeMap.add(perkuliahan);
						} else {
							List<Perkuliahan> perkuliahans2 = new ArrayList<Perkuliahan>();
							perkuliahans2.add(perkuliahan);
							dosensMap.put(d, perkuliahans2);
						}
					}

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
