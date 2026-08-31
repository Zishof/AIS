package ais.action.master.dashboard.admin;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.zkoss.zul.Auxhead;
import org.zkoss.zul.Auxheader;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;

import ais.action.master.MahasiswaAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusKeluar;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

import ais.ui.util.DashboardModernHtmlUtil;
/**
 * Komponen dashboard khusus untuk dashboard krs mahasiswa. Kelas ini memilih variasi data atau
 * tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchStatusAwalMahasiswa},
 * {@code Combobox searchprogram}, {@code Div center}, {@code Combobox searchfakultas}, {@code Combobox
 * searchjurusan}, {@code Combobox searchTahunAjaran}, {@code Combobox searchJenisSemester}, {@code Combobox
 * searchangkatan}; inisialisasi/lifecycle ({@code init()}); pembacaan/pencarian ({@code reload()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardKrsMahasiswa extends MyWindow {

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
	private Combobox searchJenisSemester;
	private Combobox searchangkatan;
	protected Grid grid;
	private int width = 750;
	private int height = 100;
	private Combobox searchStatusKeluar;

	public DashboardKrsMahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardKrsMahasiswa(String title, String border, boolean closable) {
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
				"KRS Mahasiswa",
				"Rekap pengambilan KRS mahasiswa, beserta grafiknya.");
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

		searchStatusAwalMahasiswa = new Combobox();
		row.appendChild(new MyLabelConfig("Status Awal"));
		row.appendChild(searchStatusAwalMahasiswa);
		Common.insertComboDanSemua(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		searchStatusAwalMahasiswa.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("(-) Angkatan"));
		searchangkatan = new Combobox();
		row.appendChild(searchangkatan);
		searchangkatan.setWidth("90%");
		searchangkatan.setReadonly(true);
		for (int i = 0; i < 12; i++) {
			Comboitem a;
			searchangkatan.appendChild(a = new Comboitem((i + 1) + ""));
			a.setValue(i);
		}
		Common.selectComboItem(searchangkatan, 3);
		searchangkatan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		searchStatusKeluar = new Combobox();
		row.appendChild(new MyLabelConfig("Status Keluar"));
		row.appendChild(searchStatusKeluar);
		Common.insertComboDanSemua(searchStatusKeluar, new String[] { "nama" }, "keterangan", StatusKeluar.class,
				"=Masih Aktif=", Restrictions.sqlRestriction("true"));
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
				UIUtil.downloadGrid(DashboardKrsMahasiswa.this.grid);
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

		final StatusKeluar statusKeluar = (StatusKeluar) (searchStatusKeluar.getSelectedItem() == null
				|| searchStatusKeluar.getSelectedItem().getValue() == null ? null
						: searchStatusKeluar.getSelectedItem().getValue());

		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final String ta = (String) (searchTahunAjaran.getSelectedItem() == null
				|| searchTahunAjaran.getSelectedItem().getValue() == null ? null
						: searchTahunAjaran.getSelectedItem().getValue());

		final String smt = (String) (searchJenisSemester.getSelectedItem() == null
				|| searchJenisSemester.getSelectedItem().getValue() == null
						? (Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP)
						: searchJenisSemester.getSelectedItem().getValue());

		Fakultas fak = (Fakultas) (searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue());
		Jurusan jur = (Jurusan) (searchjurusan.getSelectedItem() == null ? null
				: searchjurusan.getSelectedItem().getValue());
		final List<Jurusan> jurusans = HibernateUtil.currentSession().createCriteria(Jurusan.class)
				.add(jur == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("id", jur.getId()))
				.add(fak == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("fakultas", fak))
				.addOrder(Order.asc("fakultas"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		final Map<Long, List<Object[]>> datas = new HashMap<Long, List<Object[]>>();
		final Map<Long, List<Object[]>> datasJml = new HashMap<Long, List<Object[]>>();

		Integer j = (Integer) searchangkatan.getSelectedItem().getValue();
		final int mul = Integer.parseInt(ta.split("/")[0]) - j;
		final int sam = mul + j;

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

				auxheader = new Auxheader("Mahasiswa Telah Ambil KRS");
				auxheader.setColspan((sam - mul) + 1);
				auxheader.setParent(auxhead);

				auxheader = new Auxheader("Mahasiswa Belum Ambil KRS");
				auxheader.setColspan((sam - mul) + 1);
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

				for (int tahun = mul; tahun <= sam; tahun++) {
					column.setParent(columns);
					column = new MyColumnConfig(tahun + "");
					column.setAlign("center");
					column.setParent(columns);
				}

				for (int tahun = mul; tahun <= sam; tahun++) {
					column.setParent(columns);
					column = new MyColumnConfig(tahun + "");
					column.setAlign("center");
					column.setParent(columns);
				}

				Rows rows = new Rows();
				rows.setParent(grid);

				SimpleCategoryModel categoryModelTelahAmbil = new SimpleCategoryModel();
				categoryModelTelahAmbil.clear();

				SimpleCategoryModel categoryModelBelumAmbil = new SimpleCategoryModel();
				categoryModelBelumAmbil.clear();

				for (final Jurusan jurusan : jurusans) {
					MyFormRow row = new MyFormRow();
		row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBoldAja(jurusan.getFakultas().getNama()));
					row.appendChild(new MyLabelBoldAja(jurusan.getNama()));

					List<Object[]> dataJumlahMhs = datas.get(jurusan.getId());
					List<Object[]> countAmbils = datasJml.get(jurusan.getId());

					Map<Integer, List<Long>> mapsdata = new HashMap<Integer, List<Long>>();
					for (Object[] d : countAmbils) {
						Number tahunangkatan = (Number) d[1];
						if (tahunangkatan != null) {
							Number mhs = (Number) d[0];
							if (mapsdata.keySet().contains(tahunangkatan.intValue())) {
								mapsdata.get(tahunangkatan.intValue()).add(mhs.longValue());
							} else {
								List<Long> longs = new ArrayList<Long>();
								longs.add(mhs.longValue());
								mapsdata.put(tahunangkatan.intValue(), longs);
							}
						}
					}

					TreeMap<Integer, Object[]> data = new TreeMap<Integer, Object[]>();
					for (int tahun = mul; tahun <= sam; tahun++) {

						Number count = 0;
						for (Object[] o : dataJumlahMhs) {
							Object tahunangkatan = o[1];
							if (tahunangkatan != null && Integer.parseInt(tahunangkatan.toString()) == tahun) {
								count = (Number) o[0];
								break;
							}
						}

						List<Long> countAmbil = mapsdata.get(tahun);
						if (countAmbil == null) {
							countAmbil = new ArrayList<Long>();
						}

						Integer belumAmbil = count.intValue() - countAmbil.size();

						data.put(tahun, new Object[] { count, countAmbil, belumAmbil });
					}

					for (final Integer tahun : data.keySet()) {
						Object[] d = data.get(tahun);
						final List<Long> countAmbil = (List<Long>) d[1];
						A a = new A(countAmbil.size() + "");
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
															.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
															.add(countAmbil.isEmpty()
																	? Restrictions.sqlRestriction("false")
																	: Restrictions.in("id", countAmbil))

															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true)))
															.add(statusAwalMahasiswa == null
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("statusAwalMahasiswa",
																			statusAwalMahasiswa))

															.add(statusKeluar == null
																	? Restrictions.isNull("statusKeluar")
																	: Restrictions.eq("statusKeluar", statusKeluar))

															.add(program == null ? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("program", program))

															.add(Restrictions.eq("jurusan", jurusan))
															.add(Restrictions.eq("tahunangkatan", tahun))
															.addOrder(Order.asc("nim"));

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

						categoryModelTelahAmbil.setValue(jurusan.getNama(), tahun, countAmbil.size());

					}

					for (final Integer tahun : data.keySet()) {
						Object[] d = data.get(tahun);
						Number total = (Number) d[0];
						final List<Long> countAmbil = (List<Long>) d[1];
						A a = new A((total.intValue() - countAmbil.size()) + "");
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
															.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
															.add(countAmbil.isEmpty()
																	? Restrictions.sqlRestriction("true")
																	: Restrictions
																			.not(Restrictions.in("id", countAmbil)))

															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true)))
															.add(statusAwalMahasiswa == null
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("statusAwalMahasiswa",
																			statusAwalMahasiswa))

															.add(statusKeluar == null
																	?  Restrictions.isNull("statusKeluar")
																	: Restrictions.eq("statusKeluar", statusKeluar))

															.add(program == null ? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("program", program))

															.add(Restrictions.eq("jurusan", jurusan))
															.add(Restrictions.eq("tahunangkatan", tahun))
															.addOrder(Order.asc("nim"));

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

						categoryModelBelumAmbil.setValue(jurusan.getNama(), tahun, countAmbil.size());

					}
				}

				MyFormRow row = new MyFormRow();
		row.setValign("top");
				row.setParent(rows);
				row.setSpans((((sam - mul) * 2) + 2) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModelTelahAmbil, String.valueOf("Mahasiswa yang telah ambil KRS"), "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
row = new MyFormRow();
				row.setParent(rows);
				row.setSpans((((sam - mul) * 2) + 2) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModelBelumAmbil, String.valueOf("Mahasiswa yang belum ambil KRS"), "Jumlah mahasiswa yang belum mengambil KRS ditampilkan agar tindak lanjut akademik lebih mudah dilakukan.", String.valueOf("bar")));


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

					List<Object[]> dataJumlahMhs = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(statusAwalMahasiswa == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("statusAwalMahasiswa", statusAwalMahasiswa))

							.add(statusKeluar == null ? Restrictions.isNull("statusKeluar")
									: Restrictions.eq("statusKeluar", statusKeluar))

							.add(program == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("program", program))

							.setProjection(Projections.projectionList().add(Projections.rowCount())
									.add(Projections.groupProperty("tahunangkatan")))
							.add(Restrictions.between("tahunangkatan", mul, sam))
							.add(Restrictions.eq("jurusan", jurusan)).list();

					Criteria crit = session.createCriteria(Detailperkuliahan.class);

					if (smt.equalsIgnoreCase(Perkuliahan.SP)) {
						crit.createAlias("perkuliahan", "perkuliahan");
					}

					crit.add(Restrictions.eq("tahunAkademik", ta))
							.add(smt.equalsIgnoreCase(Perkuliahan.SP)
									? Restrictions.eq("perkuliahan.statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
									: Restrictions.sqlRestriction(
											"semester%2=" + (smt.equals(Perkuliahan.GENAP) ? "0" : "1")))

							.createAlias("mahasiswa", "mahasiswa")

							.setProjection(Projections.projectionList().add(Projections.groupProperty("mahasiswa.id"))
									.add(Projections.groupProperty("mahasiswa.tahunangkatan")))

							.add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"),
									Restrictions.eq("mahasiswa.aktif", true)))
							.add(statusAwalMahasiswa == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("mahasiswa.statusAwalMahasiswa", statusAwalMahasiswa))

							.add(statusKeluar == null ? Restrictions.isNull("mahasiswa.statusKeluar")
									: Restrictions.eq("mahasiswa.statusKeluar", statusKeluar))

							.add(program == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("mahasiswa.program", program))

							.add(Restrictions.eq("mahasiswa.jurusan", jurusan))
							.add(Restrictions.between("mahasiswa.tahunangkatan", mul, sam));

					List<Object[]> countAmbils = crit.list();

					datas.put(jurusan.getId(), dataJumlahMhs);
					datasJml.put(jurusan.getId(), countAmbils);
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
