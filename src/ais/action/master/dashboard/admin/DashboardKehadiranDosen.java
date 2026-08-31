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

/**
 * Komponen dashboard khusus untuk dashboard kehadiran dosen. Kelas ini memilih variasi data atau
 * tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Div center}, {@code Combobox
 * searchTahunAjaran}, {@code Combobox searchprogram}, {@code Combobox searchfakultas}, {@code Combobox
 * searchjurusan}, {@code Combobox searchJenisSemester}, {@code Grid grid}; inisialisasi/lifecycle ({@code
 * init()}); pembacaan/pencarian ({@code reload()}); operasi domain lain ({@code appendDashboardIntroRow()},
 * {@code appendDashboardSummary()}, {@code escapeDashboardHtml()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardKehadiranDosen extends MyWindow {

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

	public DashboardKehadiranDosen() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardKehadiranDosen(String title, String border, boolean closable) {
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
				"Kehadiran Dosen",
				"Rekap kehadiran dosen di perkuliahan, beserta grafiknya.");
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
		appendDashboardIntroRow(rows, "Dashboard Kehadiran Dosen", "Menyajikan tingkat kehadiran dosen pada perkuliahan aktif berdasarkan tahun ajaran, semester, program, fakultas, dan prodi. Informasi ini membantu pimpinan akademik memantau kedisiplinan pengajaran, memastikan pertemuan kuliah berjalan, dan menentukan tindak lanjut jika ada kelas yang kurang terisi.");

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
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/svg/download.svg");
		toolbarbutton.setParent(row);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UIUtil.downloadGrid(DashboardKehadiranDosen.this.grid);
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
				org.zkoss.zul.Vbox dashboardBody = new org.zkoss.zul.Vbox();
				dashboardBody.setWidth("100%");
				dashboardBody.setHeight("100%");
				dashboardBody.setStyle("background:#f8fafc; padding:12px; box-sizing:border-box; overflow:auto;");
				dashboardBody.setParent(center);
				appendDashboardSummary(dashboardBody, "Dashboard Kehadiran Dosen", "Menyajikan tingkat kehadiran dosen pada perkuliahan aktif berdasarkan tahun ajaran, semester, program, fakultas, dan prodi. Informasi ini membantu pimpinan akademik memantau kedisiplinan pengajaran, memastikan pertemuan kuliah berjalan, dan menentukan tindak lanjut jika ada kelas yang kurang terisi.", "Kehadiran Dosen");
				grid.setWidth("100%");
				grid.setParent(dashboardBody);
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
				column = new MyColumnConfig("Kehadiran Dosen");
				column.setParent(columns);
				column.setWidth("8%");

				column.setParent(columns);
				column = new MyColumnConfig("% Dosen");
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

										}, null, "Download Data", "/img/svg/download.svg", null, null, false, null,
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
						Map<String, Integer> semuaStatusesDosen = new HashMap<String, Integer>();

						for (Pertemuan pertemuan : pertemuans) {
							if (pertemuan.getAktif()) {
								Map<String, Integer> statusesDosen = pertemuan.hitungStatusDosen(dosen);
								for (String key : statusesDosen.keySet()) {
									if (semuaStatusesDosen.containsKey(key)) {
										semuaStatusesDosen.put(key,
												semuaStatusesDosen.get(key) + statusesDosen.get(key));
									} else {
										semuaStatusesDosen.put(key, statusesDosen.get(key));
									}
								}
							}
						}

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

										}, null, "Download Data", "/img/svg/download.svg", null, null, false, null,
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

						Integer kehadiran = semuaStatusesDosen.get("M") == null ? 0 : semuaStatusesDosen.get("M");

						row.appendChild(
								a = new A(Common.numberFormat.get().format(countSudahDisetujui.equals(0) ? 0 : kehadiran)));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								CommonReportHelper.onLaporanAbsensi(perkuliahan, true, 7);

							}
						});

						row.appendChild(a = new A(Common.numberFormat.get().format(
								countSudahDisetujui.equals(0) ? 0 : (kehadiran * 100.0) / pertemuans.size()) + "%"));
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
								: Restrictions.and(Restrictions.isNull("statusSemesterPendek"),
										Restrictions.eq("ganjilGenap", smt)))

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

	private void appendDashboardIntroRow(Rows rows, String title, String description) {
		if (rows == null) {
			return;
		}
		Row row = new Row();
		row.setValign("top");
		row.setParent(rows);
		try {
			ais.ui.util.ZkCompat.setSpans(row, "8");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardKehadiranDosen.java:560");
		}
		org.zkoss.zul.Html html = new org.zkoss.zul.Html("<div style=\"margin:0 0 10px 0; padding:16px 18px; "
				+ "border-radius:16px; background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); color:#ffffff; "
				+ "box-shadow:0 12px 26px rgba(15,23,42,.16);\">"
				+ "<div style=\"font-size:18px; font-weight:800;\">" + escapeDashboardHtml(title) + "</div>"
				+ "<div style=\"font-size:12px; line-height:1.65; margin-top:6px; opacity:.92;\">"
				+ escapeDashboardHtml(description) + "</div></div>");
		row.appendChild(html);
	}

	private void appendDashboardSummary(org.zkoss.zk.ui.Component parent, String title, String description, String fokusData) {
		if (parent == null) {
			return;
		}
		org.zkoss.zul.Html html = new org.zkoss.zul.Html("<div style=\"margin:0 0 12px 0; padding:14px; "
				+ "border-radius:16px; background:#ffffff; border:1px solid #e2e8f0; "
				+ "box-shadow:0 10px 22px rgba(15,23,42,.06);\">"
				+ "<div style=\"font-size:15px; font-weight:800; color:#0f172a;\">" + escapeDashboardHtml(title) + "</div>"
				+ "<div style=\"font-size:12px; color:#475569; line-height:1.6; margin-top:6px;\">"
				+ escapeDashboardHtml(description) + "</div>"
				+ "<div style=\"display:flex; flex-wrap:wrap; gap:8px; margin-top:10px;\">"
				+ "<span style=\"padding:5px 9px; border-radius:999px; background:#eff6ff; color:#1d4ed8; font-size:11px; font-weight:700;\">"
				+ escapeDashboardHtml(fokusData) + "</span>"
				+ "<span style=\"padding:5px 9px; border-radius:999px; background:#f8fafc; color:#334155; font-size:11px; font-weight:700;\">Filter aktif menentukan isi tabel</span>"
				+ "<span style=\"padding:5px 9px; border-radius:999px; background:#ecfdf5; color:#166534; font-size:11px; font-weight:700;\">Angka dapat ditelusuri untuk detail</span>"
				+ "</div></div>");
		parent.appendChild(html);
	}

	private String escapeDashboardHtml(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;").replace("'", "&#39;");
	}


}
