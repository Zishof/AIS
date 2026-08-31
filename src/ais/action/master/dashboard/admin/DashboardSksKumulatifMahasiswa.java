package ais.action.master.dashboard.admin;

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
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
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
/**
 * Komponen dashboard khusus untuk dashboard sks kumulatif mahasiswa. Kelas ini memilih variasi
 * data atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas
 * induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchStatusAwalMahasiswa},
 * {@code Combobox searchprogram}, {@code Div center}, {@code Combobox searchfakultas}, {@code Combobox
 * searchjurusan}, {@code Combobox searchTahunAjaran}, {@code Grid grid}, {@code int width};
 * inisialisasi/lifecycle ({@code init()}); pembacaan/pencarian ({@code reload()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardSksKumulatifMahasiswa extends MyWindow {

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

	public DashboardSksKumulatifMahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardSksKumulatifMahasiswa(String title, String border, boolean closable) {
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
				"SKS Kumulatif Mahasiswa",
				"Sebaran SKS kumulatif mahasiswa, beserta grafiknya.");
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

		// Number m = (Number)
		// HibernateUtil.currentSession().createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
		// .add(Restrictions.or(Restrictions.isNull("aktif"),
		// Restrictions.eq("aktif", true)))
		// .setProjection(Projections.max("tahunangkatan")).uniqueResult();
		//
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		// row.appendChild(new MyLabelConfig("Angkatan"));
		// Hbox hbox = new Hbox();
		// hbox.setParent(row);
		//
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

		searchprogram = new Combobox();
		row.appendChild(new MyLabelConfig("Program"));
		Common.initPrograms(searchprogram);
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.setReadonly(true);

		searchStatusAwalMahasiswa.addEventListener("onChange", eventListener);
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
				UIUtil.downloadGrid(DashboardSksKumulatifMahasiswa.this.grid);
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

		Fakultas fak = (Fakultas) (searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue());
		Jurusan jur = (Jurusan) (searchjurusan.getSelectedItem() == null ? null
				: searchjurusan.getSelectedItem().getValue());
		final List<Jurusan> jurusans = HibernateUtil.currentSession().createCriteria(Jurusan.class)
				.add(jur == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("id", jur.getId()))
				.add(fak == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("fakultas", fak))
				.addOrder(Order.asc("fakultas"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		final int mul = Integer.parseInt(ta.split("/")[0]) - 4;
		final int sam = mul + 4;

		final Map<Long, List<Object[]>> datas = new HashMap<Long, List<Object[]>>();

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

				auxheader = new Auxheader("Rata-rata SKS Kumulatif");
				auxheader.setColspan((sam - mul) + 1);
				auxheader.setParent(auxhead);

				auxheader = new Auxheader("Minimal SKS Kumulatif");
				auxheader.setColspan((sam - mul) + 1);
				auxheader.setParent(auxhead);

				auxheader = new Auxheader("Maksimal SKS Kumulatif");
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

				for (int tahun = mul; tahun <= sam; tahun++) {
					column.setParent(columns);
					column = new MyColumnConfig(tahun + "");
					column.setAlign("center");
					column.setParent(columns);
				}

				Rows rows = new Rows();
				rows.setParent(grid);

				SimpleCategoryModel categoryModelRataRata = new SimpleCategoryModel();
				categoryModelRataRata.clear();

				SimpleCategoryModel categoryModelMin = new SimpleCategoryModel();
				categoryModelMin.clear();

				SimpleCategoryModel categoryModelMax = new SimpleCategoryModel();
				categoryModelMax.clear();

				for (final Jurusan jurusan : jurusans) {
					MyFormRow row = new MyFormRow();
		row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBoldAja(jurusan.getFakultas().getNama()));
					row.appendChild(new MyLabelBoldAja(jurusan.getNama()));

					List<Object[]> dataIpk = datas.get(jurusan.getId());

					TreeMap<Integer, Object[]> data = new TreeMap<Integer, Object[]>();

					for (Integer tahun = mul; tahun <= sam; tahun++) {

						Number rataRata = 0;
						Number min = 0;
						Number max = 0;

						for (Object[] o : dataIpk) {
							Object tahunangkatan = o[3];
							if (tahunangkatan != null && tahunangkatan.toString().equalsIgnoreCase(tahun.toString())) {
								rataRata = (Number) o[0];
								min = (Number) o[1];
								max = (Number) o[2];
								break;
							}
						}

						data.put(tahun, new Object[] { rataRata, min, max });
					}

					for (final Integer tahun : data.keySet()) {
						Object[] d = data.get(tahun);
						Number ipk = (Number) d[0];
						A a = new A(Common.numberFormat.get().format(ipk == null ? 0.0 : ipk.doubleValue()));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								EventListener eventListener = (EventListener) Common
										.cetakDataCustomButton(KrsMahasiswa.class, new DataCriteriaWithColumn() {

											@Override
											public Object[] initCriteria(boolean order) {

												try {

													Criteria criteria = HibernateUtil.currentSession()
															.createCriteria(KrsMahasiswa.class)
															.add(Restrictions.eq("tahunAkademik",
																	Common.getCurrentTahunAkademik()))
															.add(Restrictions.sqlRestriction("semester%2="
																	+ (Common.isNowSemensterGanjil() ? 1 : 0) + ""))
															.add(Restrictions.isNull("semesterPendek"))
															.createCriteria("mahasiswa")

															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true)))
															.add(statusAwalMahasiswa == null
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("statusAwalMahasiswa",
																			statusAwalMahasiswa))

															.add(statusKeluar == null
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("statusKeluar", statusKeluar))

															.add(program == null ? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("program", program))

															.add(Restrictions.eq("jurusan", jurusan))
															.add(Restrictions.eq("tahunangkatan", tahun))
															.addOrder(Order.asc("nim"));

													return new Object[] { criteria, new String[] { "mahasiswa.nim",
															"mahasiswa.nama", "sksk", "ipk" } };

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

						categoryModelRataRata.setValue(jurusan.getNama(), tahun, ipk == null ? 0.0 : ipk.doubleValue());

					}

					for (final Integer tahun : data.keySet()) {
						Object[] d = data.get(tahun);
						Number ipk = (Number) d[1];
						A a = new A(Common.numberFormat.get().format(ipk == null ? 0.0 : ipk.doubleValue()));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								EventListener eventListener = (EventListener) Common
										.cetakDataCustomButton(KrsMahasiswa.class, new DataCriteriaWithColumn() {

											@Override
											public Object[] initCriteria(boolean order) {

												try {

													Criteria criteria = HibernateUtil.currentSession()
															.createCriteria(KrsMahasiswa.class)
															.addOrder(Order.asc("sksk"))
															.add(Restrictions.eq("tahunAkademik",
																	Common.getCurrentTahunAkademik()))
															.add(Restrictions.sqlRestriction("semester%2="
																	+ (Common.isNowSemensterGanjil() ? 1 : 0) + ""))
															.add(Restrictions.isNull("semesterPendek"))
															.createCriteria("mahasiswa")

															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true)))
															.add(statusAwalMahasiswa == null
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("statusAwalMahasiswa",
																			statusAwalMahasiswa))

															.add(statusKeluar == null
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("statusKeluar", statusKeluar))

															.add(program == null ? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("program", program))

															.add(Restrictions.eq("jurusan", jurusan))
															.add(Restrictions.eq("tahunangkatan", tahun));

													return new Object[] { criteria, new String[] { "mahasiswa.nim",
															"mahasiswa.nama", "sksk", "ipk" } };

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

						categoryModelMin.setValue(jurusan.getNama(), tahun, ipk == null ? 0.0 : ipk.doubleValue());

					}

					for (final Integer tahun : data.keySet()) {
						Object[] d = data.get(tahun);
						Number ipk = (Number) d[2];
						A a = new A(Common.numberFormat.get().format(ipk == null ? 0.0 : ipk.doubleValue()));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								EventListener eventListener = (EventListener) Common
										.cetakDataCustomButton(KrsMahasiswa.class, new DataCriteriaWithColumn() {

											@Override
											public Object[] initCriteria(boolean order) {

												try {

													Criteria criteria = HibernateUtil.currentSession()
															.createCriteria(KrsMahasiswa.class)
															.addOrder(Order.desc("sksk"))
															.add(Restrictions.eq("tahunAkademik",
																	Common.getCurrentTahunAkademik()))
															.add(Restrictions.sqlRestriction("semester%2="
																	+ (Common.isNowSemensterGanjil() ? 1 : 0) + ""))
															.add(Restrictions.isNull("semesterPendek"))
															.createCriteria("mahasiswa")

															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true)))
															.add(statusAwalMahasiswa == null
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("statusAwalMahasiswa",
																			statusAwalMahasiswa))

															.add(statusKeluar == null
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("statusKeluar", statusKeluar))

															.add(program == null ? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("program", program))

															.add(Restrictions.eq("jurusan", jurusan))
															.add(Restrictions.eq("tahunangkatan", tahun));

													return new Object[] { criteria, new String[] { "mahasiswa.nim",
															"mahasiswa.nama", "sksk", "ipk" } };

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

						categoryModelMax.setValue(jurusan.getNama(), tahun, ipk == null ? 0.0 : ipk.doubleValue());

					}
				}

				MyFormRow row = new MyFormRow();
		row.setValign("top");
				row.setParent(rows);
				row.setSpans((((sam - mul) * 3) + 2) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModelRataRata, String.valueOf("Rata-rata SKS Kumulatif Mahasiswa"), "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
row = new MyFormRow();
				row.setParent(rows);
				row.setSpans((((sam - mul) * 3) + 2) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModelMin, String.valueOf("Minimal SKS Kumulatif Mahasiswa"), "SKS kumulatif terendah ditampilkan agar mahasiswa yang perlu pendampingan akademik lebih cepat diketahui.", String.valueOf("bar")));



				row = new MyFormRow();
				row.setParent(rows);
				row.setSpans((((sam - mul) * 3) + 2) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModelMax, String.valueOf("Maksimal SKS Kumulatif Mahasiswa"), "SKS kumulatif tertinggi ditampilkan agar capaian pengambilan beban studi mudah dibandingkan.", String.valueOf("bar")));


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

					List<Object[]> dataIpk = session.createCriteria(KrsMahasiswa.class)
							.createAlias("mahasiswa", "mahasiswa")

							.add(statusKeluar == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("mahasiswa.statusKeluar", statusKeluar))

							.setProjection(Projections.projectionList().add(Projections.avg("sksk"))
									.add(Projections.min("sksk")).add(Projections.max("sksk"))
									.add(Projections.groupProperty("mahasiswa.tahunangkatan")))
							.add(Restrictions.eq("tahunAkademik", Common.getCurrentTahunAkademik()))
							.add(Restrictions
									.sqlRestriction("semester%2=" + (Common.isNowSemensterGanjil() ? 1 : 0) + ""))
							.add(Restrictions.isNull("semesterPendek"))

							.add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"),
									Restrictions.eq("mahasiswa.aktif", true)))
							.add(statusAwalMahasiswa == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("mahasiswa.statusAwalMahasiswa", statusAwalMahasiswa))

							.add(program == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("mahasiswa.program", program))

							.add(Restrictions.eq("mahasiswa.jurusan", jurusan)).list();

					datas.put(jurusan.getId(), dataIpk);
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
