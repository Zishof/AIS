package ais.action.master.dashboard.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Komponen dashboard khusus untuk dashboard simple feeder form entry akm mahasiswa. Kelas ini
 * memilih variasi data atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan
 * dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchfakultas}, {@code
 * Combobox searchjurusan}, {@code Combobox tahunAkademik}, {@code Combobox semesterAbsensi}, {@code Combobox
 * searchsemester}, {@code Combobox searchprogram}, {@code Label angkatan}, {@code AmbilDataDosenBanbox
 * searchDosen}; inisialisasi/lifecycle ({@code initFakultas()}, {@code init()}, {@code initSpreadsheet()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class Dashboard_simple_feeder_form_entry_akm_mahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Label angkatan = new Label();
	private AmbilDataDosenBanbox searchDosen = new AmbilDataDosenBanbox();
	private Combobox searchstatus;
	private Combobox searchStatusAwalMahasiswa;
	private Center center = new Center();

	private File file;

	// private MyCheckboxConfig BelumDinilai;
	// private MyCheckboxConfig TelahDinilai;

	public Dashboard_simple_feeder_form_entry_akm_mahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public Dashboard_simple_feeder_form_entry_akm_mahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void initFakultas() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		initFakultas();

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		/* FIX 21-08-2026: tinggi panel filter kurang 52px sehingga baris toolbar
		 * (Proses/Download) terpotong di bagian bawah. Ditambah satu tinggi baris
		 * toolbar ZK. Autoscroll tetap aktif sebagai pengaman bila isi filter
		 * bertambah di kemudian hari. */
		north.setHeight("252px");
		north.setAutoscroll(true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		// searchfakultas.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// initSpreadsheet();
		// }
		// });

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");
		// searchjurusan.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// initSpreadsheet();
		// }
		// });

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);
		// tahunAkademik.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// initSpreadsheet();
		// }
		// });

		Common.insertCombo(searchstatus = new Combobox(), new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
		Common.insertCombo(searchStatusAwalMahasiswa = new Combobox(), "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		row.appendChild(searchstatus);
		searchstatus.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen PA"));
		row.appendChild(searchDosen);
		searchDosen.setWidth("90%");
		searchDosen.setWidth("90%");
		// searchDosen.setEventListener(new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// initSpreadsheet();
		// }
		// });

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setSelectedIndex(1);
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("90%");
		semesterAbsensi.setReadonly(true);

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester ke"));
		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");
		searchsemester.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatan);
		angkatan.setValue("(tahun angkatan : semua)");
		angkatan.setWidth("90%");
		Common.initPrograms(searchprogram);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal"));
		row.appendChild(searchStatusAwalMahasiswa);
		searchStatusAwalMahasiswa.setWidth("90%");

		Common.checkProgramString(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		// searchprogram.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// initSpreadsheet();
		// }
		// });

		final EventListener semesterEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final String tahunAkademik = (String) (Dashboard_simple_feeder_form_entry_akm_mahasiswa.this.tahunAkademik
						.getSelectedItem() == null ? null
								: Dashboard_simple_feeder_form_entry_akm_mahasiswa.this.tahunAkademik.getSelectedItem()
										.getValue());
				final String semester = (String) (Dashboard_simple_feeder_form_entry_akm_mahasiswa.this.semesterAbsensi
						.getSelectedItem() == null ? Perkuliahan.GANJIL
								: Dashboard_simple_feeder_form_entry_akm_mahasiswa.this.semesterAbsensi
										.getSelectedItem().getValue());

				final Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? -1
						: searchsemester.getSelectedItem().getValue());

				if (tahunAkademik == null) {
					return;
				}
				final Integer tahunAngkatan = Common.getTahunAngkatan(semesterKe, semester, tahunAkademik);
				final Boolean semuasemester = searchsemester.getSelectedItem() == null;
				if (!semuasemester) {
					angkatan.setValue("(tahun angkatan : " + tahunAngkatan + ")");
				}
			}
		};

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchsemester);
				searchsemester.setSelectedItem(null);
				angkatan.setValue("(tahun angkatan : semua)");
				if (semesterAbsensi.getSelectedItem() == null) {
					return;
				}
				Boolean genap = semesterAbsensi.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
				searchsemester.setDisabled(false);
				if (semesterAbsensi.getSelectedItem().getValue().equals(Perkuliahan.SP)) {
					MyComboitemConfig comboitem = new MyComboitemConfig();
					comboitem.setLabel("Semua");
					comboitem.setValue(null);
					searchsemester.appendChild(comboitem);
					searchsemester.setDisabled(true);
				} else if (genap) {
					for (int i : Common.genap) {
						if (i == 0)
							continue;
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				} else {
					for (int i : Common.ganjil) {
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				}

				if (searchsemester.getSelectedItem() == null) {
					searchsemester.setSelectedIndex(0);
				}

				semesterEventListener.onEvent(null);
			}
		};
		// semesterAbsensi.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// eventListener.onEvent(arg0);
		// initSpreadsheet();
		// }
		// });
		//
		searchsemester.addEventListener("onChange", semesterEventListener);
		tahunAkademik.addEventListener("onChange", semesterEventListener);

		semesterAbsensi.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		row.setParent(rows);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(row);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Proses", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initSpreadsheet();
					}
				});
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
							"Data Aktifitas Kuliah Mahasiswa (AKM).xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/Dashboard_simple_feeder_form_entry_akm_mahasiswa.java:391");

				}
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

		Common.clear(center);
		final String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());

		final String semester = (String) (this.semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? Perkuliahan.GANJIL
						: this.semesterAbsensi.getSelectedItem().getValue());

		final Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		final Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		final Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? -1
				: searchsemester.getSelectedItem().getValue());
		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final Dosen dosen = (Dosen) searchDosen.getAttribute("dosen");

		if (tahunAkademik == null || semesterKe == null) {
			return;
		}
		final Integer tahunAngkatan = Common.getTahunAngkatan(semesterKe, semester, tahunAkademik);
		final Boolean semuasemester = searchsemester.getSelectedItem() == null;
		if (!semuasemester) {
			angkatan.setValue("(tahun angkatan : " + tahunAngkatan + ")");
		}
		final int tahun = Integer.parseInt(StringUtils.split(tahunAkademik, "/")[0]);

		System.out.println("init spreadsheet running => tahun = " + tahun);

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);
		// final boolean telahDinilai = TelahDinilai.isChecked();
		// final boolean belumDinilai = BelumDinilai.isChecked();

		new Thread(new Runnable() {

			@Override
			public void run() {

				System.out.println("tahunAkademik = " + tahunAkademik);

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("AKM");
				sheet.setDefaultColumnWidth(25);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("NIM");
				rowhead.createCell(1).setCellValue("NAMA");
				rowhead.createCell(2).setCellValue("SEMESTER");
				rowhead.createCell(3).setCellValue("SKS");
				rowhead.createCell(4).setCellValue("IP SEMESTER");
				rowhead.createCell(5).setCellValue("SKS KUMULATIF");
				rowhead.createCell(6).setCellValue("IP KUMULATIF");
				rowhead.createCell(7).setCellValue("STATUS");

				Session session = HibernateUtil.getSessionFactory().openSession();
				try {

				Criteria criteria = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

				if (searchstatus.getSelectedItem() != null) {
					criteria = session.createCriteria(HistoryStatusMahasiswa.class)
							.setProjection(Projections.groupProperty("mahasiswa"))
							.add(Restrictions.eq("semester", semesterKe))
							.add(Restrictions.eq("statusMahasiswa", searchstatus.getSelectedItem().getValue()))
							.createCriteria("mahasiswa");
				}

				List<Mahasiswa> mahasiswas = criteria

						.add(searchStatusAwalMahasiswa.getSelectedItem() == null
								|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
								|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("statusAwalMahasiswa",
												searchStatusAwalMahasiswa.getSelectedItem().getValue()))

						.add(dosen != null ? Restrictions.eq("dosen", dosen.getId())
								: Restrictions.sqlRestriction("1=1"))

						.createAlias("jurusan", "jurusan")

						.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jurusan.fakultas", fakultas))

						.add(jurusan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", jurusan))

						.add(program == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("program", program))

						.add(semuasemester ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunangkatan", tahunAngkatan))

						.list();

				int size = mahasiswas.size();

				int rowIndex = 1;
				TreeSet<Mahasiswa> myMahasiswas = new TreeSet<Mahasiswa>(mahasiswas);
				for (Mahasiswa mahasiswa : myMahasiswas) {
					label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
							+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

					XSSFRow row = sheet.createRow(rowIndex);
					XSSFCell cell = row.createCell(0);
					cell.setCellValue(mahasiswa.getNim());

					cell = row.createCell(1);
					cell.setCellValue(mahasiswa.getNama());

					cell = row.createCell(2);
					cell.setCellValue(StringUtils.split(tahunAkademik, "/")[0]
							+ (semester.equalsIgnoreCase(Perkuliahan.GANJIL) ? "1" : "2"));

					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semesterKe, null, null);

					Double ipmhs = krsMahasiswa.getIps();
					Double ipkmhs = krsMahasiswa.getIpk();

					Integer sksmhss = krsMahasiswa.getSksYangDiambil();
					Integer sksmhs = krsMahasiswa.getSksk();

					cell = row.createCell(3);
					cell.setCellValue(sksmhss);

					cell = row.createCell(4);
					cell.setCellValue(ipmhs);

					cell = row.createCell(5);
					cell.setCellValue(sksmhs);

					cell = row.createCell(6);
					cell.setCellValue(ipkmhs);

					cell = row.createCell(7);

					HistoryStatusMahasiswa historyStatusMahasiswa = (HistoryStatusMahasiswa) session
							.createCriteria(HistoryStatusMahasiswa.class).add(Restrictions.eq("mahasiswa", mahasiswa))
							.add(Restrictions.eq("semester", semesterKe)).addOrder(Order.desc("id")).setMaxResults(1)
							.uniqueResult();

					if (historyStatusMahasiswa != null
							&& historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed() != null
							&& (historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim().equalsIgnoreCase("A")
									|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
											.equalsIgnoreCase("C")
									|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
											.equalsIgnoreCase("D")
									|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
											.equalsIgnoreCase("L")
									|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
											.equalsIgnoreCase("P")
									|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
											.equalsIgnoreCase("N")
									|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
											.equalsIgnoreCase("G")
									|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
											.equalsIgnoreCase("X")
									|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
											.equalsIgnoreCase("K"))

					) {
						cell.setCellValue(historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed());
					} else {
						cell.setCellValue("X");
					}

					rowIndex++;
				}

				Common.setStyled(sheet);
				sizedata.setValue(rowIndex + 1);

				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

				mahasiswas.clear();
				myMahasiswas = null;

				label.setValue("");
				} catch (Exception eThread) {
					Common.tampilErrorJikaAdmin(eThread);
				} finally {
					if (session != null) {
						try {
							session.clear();
						} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/Dashboard_simple_feeder_form_entry_akm_mahasiswa.java:612");
						}
						try {
							session.disconnect();
						} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/Dashboard_simple_feeder_form_entry_akm_mahasiswa.java:616");
						}
						try {
							if (session.isOpen())
								session.close();
						} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/Dashboard_simple_feeder_form_entry_akm_mahasiswa.java:621");
						}
					}
				}
			}

		}).start();

	}
}
