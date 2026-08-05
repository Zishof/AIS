package ais.action.master.helper.penilaian;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataKelasBanbox;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Jurusan;
import ais.database.model.Kelas;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DownloadNilaiMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private Combobox searchfakultas = new Combobox();

	private Combobox searchjurusan = new Combobox();
	private Intbox searchangkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
	private Combobox searchprogram = new Combobox();
	private Combobox searchkurikulum = new Combobox();
	private Combobox searchsemester = new Combobox();

	private Textbox kodeMatakuliah = new Textbox();
	private Textbox namaMatakuliah = new Textbox();
	private Textbox nimMahasiswa = new Textbox();
	private Textbox namaMahasiswa = new Textbox();
	private Combobox searchstatus = new Combobox();

	private File file;

	private AmbilDataKelasBanbox searchkelas;

	public DownloadNilaiMahasiswa() {
		super();
		try {

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

			Common.initPrograms(searchprogram);

			class KurikulumEventListener implements EventListener {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(searchkurikulum);
					searchkurikulum.setSelectedItem(null);
					if (searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null) {
						return;
					}

					if (searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null) {
						return;
					}

					Jurusan myJurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
							|| searchjurusan.getSelectedItem().getValue() == null
							|| searchjurusan.getSelectedItem().getValue() == null ? null
									: searchjurusan.getSelectedItem().getValue());

					List<Kurikulum> kurikulums = HibernateUtil.currentSession().createCriteria(Kurikulum.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.addOrder(Order.desc("tahun")).createAlias("program", "program")
							.add(Restrictions.eq("program.nama", searchprogram.getSelectedItem().getValue()))
							.add(Restrictions.eq("jurusan", myJurusan)).list();

					for (Kurikulum kurikulum : kurikulums) {
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(kurikulum.getId() + "-" + kurikulum.getNama());
						comboitem.setValue(kurikulum);
						comboitem.setDescription(kurikulum.getNamaAsli() + " " + kurikulum.getTahun() + " "
								+ kurikulum.getTahunAkademik() + " " + kurikulum.getJenisSemester());
						searchkurikulum.appendChild(comboitem);
					}

				}

			}

			KurikulumEventListener kurikulumEventListener = new KurikulumEventListener();

			searchjurusan.addEventListener("onChange", kurikulumEventListener);
			searchprogram.addEventListener("onChange", kurikulumEventListener);

			MyComboitemConfig comboitemSemua = new MyComboitemConfig();
			comboitemSemua.setLabel("Semua Semester");
			comboitemSemua.setValue(-1);
			searchsemester.appendChild(comboitemSemua);
			for (int i = 1; i < 30; i++) {
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel(i + "");
				comboitem.setValue(i);
				searchsemester.appendChild(comboitem);
			}

			searchsemester.setSelectedIndex(0);

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Unduh Nilai Mahasiswa",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Unduh Nilai Mahasiswa.",
							"Periksa apakah data Jurusan, Program Studi, Kurikulum, dan Semester pada master data sudah terisi dengan benar.",
							"Jika jendela tetap gagal terbuka setelah beberapa kali percobaan, hubungi Administrator." });
		}
	}

	public DownloadNilaiMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Unduh Nilai Mahasiswa",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Unduh Nilai Mahasiswa.",
							"Periksa apakah data Jurusan, Program Studi, Kurikulum, dan Semester pada master data sudah terisi dengan benar.",
							"Jika jendela tetap gagal terbuka setelah beberapa kali percobaan, hubungi Administrator." });
		}
	}

	private void init() throws Exception {

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
		north.setHeight("200px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(searchangkatan);
		searchangkatan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setReadonly(true);
		searchprogram.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kurikulum"));
		row.appendChild(searchkurikulum);
		searchkurikulum.setReadonly(true);
		searchkurikulum.setWidth("90%");

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(searchsemester);
		searchsemester.setReadonly(true);
		searchsemester.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Matakuliah"));
		row.appendChild(kodeMatakuliah);
		kodeMatakuliah.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Matakuliah"));
		row.appendChild(namaMatakuliah);
		namaMatakuliah.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM Mahasiswa"));
		row.appendChild(nimMahasiswa);
		nimMahasiswa.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Mahasiswa"));
		row.appendChild(namaMahasiswa);
		namaMahasiswa.setWidth("90%");

		row = new MyFormRow();

		Common.insertComboDanSemua(searchstatus, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
		row.appendChild(searchstatus);
		searchstatus.setReadonly(true);
		searchsemester.setWidth("90%");
		Common.selectComboItem(searchstatus, null);
		searchstatus.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(searchkelas = new AmbilDataKelasBanbox());
		searchkelas.setWidth("90%");
		searchkelas.setReadonly(true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig search = new MyToolbarbuttonConfig("Tampilkan Data", "/img/svg/search.svg");
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		search.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Ambil Data", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final Kurikulum kurikulum = searchkurikulum.getSelectedItem() == null ? null
						: (Kurikulum) searchkurikulum.getSelectedItem().getValue();

				final Integer semester = searchsemester.getSelectedItem() == null ? null
						: (Integer) searchsemester.getSelectedItem().getValue();

				if (kurikulum == null) {
					MyMessageboxConfig.show("Kurikulum harus dipilih", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				if (semester == null) {
					MyMessageboxConfig.show("Semester harus dipilih", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				try {
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
							"NILAI_" + kurikulum.getNama() + "_semester_" + semester + ".xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/penilaian/DownloadNilaiMahasiswa.java:309");

				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

		final Kurikulum kurikulum = searchkurikulum.getSelectedItem() == null ? null
				: (Kurikulum) searchkurikulum.getSelectedItem().getValue();

		final Integer semester = searchsemester.getSelectedItem() == null ? null
				: (Integer) searchsemester.getSelectedItem().getValue();
		final Kelas kelas = (Kelas) searchkelas.getAttribute("kelas");

		Common.clear(center);

		if (kurikulum == null) {
			MyMessageboxConfig.show("Kurikulum harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		if (semester == null) {
			MyMessageboxConfig.show("Semester harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		System.out.println("init spreadsheet running");
		final Jurusan jurusan = searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: (Jurusan) searchjurusan.getSelectedItem().getValue();

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/data_" + URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
				lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				// lockedNumericStyle.setLocked(true);

				XSSFCellStyle notLocked = workbook.createCellStyle();
				notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				// notLocked.setLocked(false);

				XSSFSheet sheet = workbook.createSheet("NILAI");
				// sheet.protectSheet("passwordrahasia");
				sheet.setDefaultColumnWidth(18);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("ID DATA");
				rowhead.createCell(1).setCellValue("NIM MAHASISWA");
				rowhead.createCell(2).setCellValue("NAMA MAHASISWA");
				rowhead.createCell(3).setCellValue("ID MATAKULIAH");
				rowhead.createCell(4).setCellValue("MATAKULIAH");
				rowhead.createCell(5).setCellValue("SEMESTER");
				rowhead.createCell(6).setCellValue("NILAI (Huruf atau Angka)");
				rowhead.createCell(7).setCellValue("KETERANGAN");
				rowhead.createCell(8).setCellValue("");
				rowhead.createCell(9).setCellValue("PINDAH KE ID MATAKULIAH");
				rowhead.createCell(10).setCellValue("PINDAH KE SEMESTER");

				Session session = HibernateUtil.currentNativeSession();

				List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = session
						.createCriteria(KurikulumPunyaMatakuliah.class).createAlias("matakuliah", "matakuliah")
						.add(Restrictions.eq("kurikulum", kurikulum))
						.add(semester.equals(-1) ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("semester", semester))

						.add(Restrictions.and(
								kodeMatakuliah.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("matakuliah.kode", kodeMatakuliah.getValue().trim(),
												MatchMode.ANYWHERE),

								namaMatakuliah.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("matakuliah.nama", namaMatakuliah.getValue().trim(),
												MatchMode.ANYWHERE)))

						.add(Restrictions.isNotNull("matakuliah")).addOrder(Order.asc("semester"))
						.addOrder(Order.asc("matakuliah.kode")).list();

				StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
						|| searchstatus.getSelectedItem().getValue() == null ? null
								: searchstatus.getSelectedItem().getValue());

				Criterion criteriaStatus = Restrictions.sqlRestriction("true");
				if (statusMahasiswa != null) {
					String sql = "this_.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
							+ statusMahasiswa.getId() + " and tahunakademik = '" + Common.getCurrentTahunAkademik()
							+ "' and semester%2=" + (Common.isNowSemensterGanjil() ? 1 : 0) + ")";
					System.out.println("sql=>" + sql);
					criteriaStatus = Restrictions.sqlRestriction(sql);
				}

				List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(kelas != null && !kelas.getNama().trim().isEmpty()
								? Restrictions.ilike("kelas", kelas.getNama().trim(), MatchMode.EXACT)
								: Restrictions.sqlRestriction("true"))

						.add(criteriaStatus)

						.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))

						.add(Restrictions.and(
								nimMahasiswa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("nim", nimMahasiswa.getValue().trim(), MatchMode.ANYWHERE),

								namaMahasiswa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("nama", namaMahasiswa.getValue().trim(),
												MatchMode.ANYWHERE)))

						.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

						.add(searchjurusan.getSelectedItem() == null
								|| searchjurusan.getSelectedItem().getValue() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(Restrictions.eq("jurusan", jurusan),
												Restrictions.eq("jurusan", jurusan)))

						.add(searchfakultas.getSelectedItem() == null
								|| searchfakultas.getSelectedItem().getValue() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

						.add(searchprogram.getSelectedItem() == null
								|| searchprogram.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

						.add(searchangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunangkatan", searchangkatan.getValue()))

						.list();

				int size = mahasiswas.size() * kurikulumPunyaMatakuliahs.size();

				if (kurikulumPunyaMatakuliahs.isEmpty()) {
					size = mahasiswas.size();
				}

				int rowIndex = 1;
				int rowIndexPersen = 1;
				for (Mahasiswa mahasiswa : mahasiswas) {
					if (!kurikulumPunyaMatakuliahs.isEmpty()) {
						for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
							label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
									+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");
							int mySemester = kurikulumPunyaMatakuliah.getSemester();

							createData(session, mahasiswa, kurikulumPunyaMatakuliah.getMatakuliah(), mySemester, sheet,
									rowIndex, lockedNumericStyle, notLocked, null);

							rowIndex++;
						}
					} else if (!kodeMatakuliah.getValue().trim().isEmpty()) {

						Detailperkuliahan detailperkuliahanLain = (Detailperkuliahan) session
								.createCriteria(Detailperkuliahan.class)
								.createAlias("matakuliahKonversi", "matakuliahKonversi")
								.add(Restrictions.eq("mahasiswa", mahasiswa))
								.add(Restrictions.ilike("matakuliahKonversi.kode", kodeMatakuliah.getValue().trim(),
										MatchMode.EXACT))
								.setMaxResults(1).uniqueResult();
						label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
								+ Common.numberFormat.get().format(rowIndexPersen * 100.0 / size) + " %)");

						if (detailperkuliahanLain != null) {
							createData(session, mahasiswa, detailperkuliahanLain.getMatakuliahKonversi(),
									detailperkuliahanLain.getSemester(), sheet, rowIndex, lockedNumericStyle, notLocked,
									detailperkuliahanLain);
							rowIndex++;
						}
						rowIndexPersen++;
					}

				}

				Common.setStyled(sheet);sizedata.setValue(rowIndex + 1);

				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException(
							"Menyimpan berkas Excel hasil unduhan Nilai Mahasiswa",
							e,
							new String[] {
									"Pastikan berkas dengan nama yang sama tidak sedang dibuka oleh aplikasi lain (misalnya Microsoft Excel).",
									"Periksa ketersediaan ruang penyimpanan (disk space) pada server.",
									"Ulangi proses unduh data. Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." });
				}

				System.out.println("Your excel file has been generated! " );

				HibernateUtil.closeSession();

				mahasiswas.clear();
				label.setValue("");
				} catch (Exception e) {
					// FIX "gagal diam-diam"/hang selamanya: sebelumnya try ini TIDAK punya catch,
					// jadi exception apa pun (mis. query Hibernate gagal) akan lolos tak
					// tertangani keluar dari run() thread ini; label.setValue("") di atas tidak
					// pernah tereksekusi, sehingga popup progres bar tidak akan pernah tertutup
					// dan pengguna tidak pernah tahu prosesnya gagal.
					Common.tampilErrorJikaAdmin(e);
					label.setValue("Error: " + PesanFormalHelper.pesanGagalException(
							"pengunduhan (Excel) data Nilai Mahasiswa", null, e,
							new String[] {
									"Periksa kembali filter (Kurikulum, Semester, Matakuliah, Fakultas/Prodi) yang dipilih lalu ulangi.",
									"Pastikan koneksi ke database server dalam kondisi baik.",
									"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
							.replace("\n", " "));
				} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	private void createData(Session session, Mahasiswa mahasiswa, Matakuliah matakuliah, int mySemester,
			XSSFSheet sheet, int rowIndex, XSSFCellStyle lockedNumericStyle, XSSFCellStyle notLocked,
			Detailperkuliahan detailperkuliahanLain) {

		Detailperkuliahan detailperkuliahan = null;

		if (detailperkuliahanLain == null) {
			Detailperkuliahan detailperkuliahanKonversi = (Detailperkuliahan) session
					.createCriteria(Detailperkuliahan.class).add(Restrictions.eq("semester", mySemester))
					.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("matakuliahKonversi", matakuliah))
					.setMaxResults(1).uniqueResult();

			Detailperkuliahan detailperkuliahanBukanKonversi = (Detailperkuliahan) session
					.createCriteria(Detailperkuliahan.class).add(Restrictions.eq("semester", mySemester))
					.createAlias("perkuliahan", "perkuliahan").add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.eq("perkuliahan.matakuliah", matakuliah)).setMaxResults(1).uniqueResult();

			detailperkuliahan = detailperkuliahanBukanKonversi == null ? detailperkuliahanKonversi
					: detailperkuliahanBukanKonversi;
		} else {
			detailperkuliahan = detailperkuliahanLain;
		}

		XSSFRow row = sheet.createRow(rowIndex);

		XSSFCell cell = row.createCell(0);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(detailperkuliahan == null || detailperkuliahan.getId() == null ? -1L : detailperkuliahan.getId());

		cell = row.createCell(1);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(mahasiswa.getNim());

		cell = row.createCell(2);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(mahasiswa.getNama());

		cell = row.createCell(3);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(matakuliah.getId());

		cell = row.createCell(4);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(matakuliah.toString());

		cell = row.createCell(5);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(mySemester);

		cell = row.createCell(6);
		if (detailperkuliahan == null || detailperkuliahan.getPerkuliahan() == null) {
			cell.setCellStyle(notLocked);
		} else {
			cell.setCellStyle(lockedNumericStyle);
		}
		cell.setCellValue(detailperkuliahan == null ? 0.0 : detailperkuliahan.getTotalNilai());

		cell = row.createCell(7);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null ? "Bukan Konversi"
				: "Konversi");

		row.createCell(8).setCellValue("");

		cell = row.createCell(9);
		if (detailperkuliahan == null || (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null)) {
			cell.setCellStyle(lockedNumericStyle);
		} else {
			cell.setCellStyle(notLocked);
		}
		cell.setCellValue("");

		cell = row.createCell(10);
		if (detailperkuliahan == null || (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null)) {
			cell.setCellStyle(lockedNumericStyle);
		} else {
			cell.setCellStyle(notLocked);
		}
		cell.setCellValue("");

	}
}
