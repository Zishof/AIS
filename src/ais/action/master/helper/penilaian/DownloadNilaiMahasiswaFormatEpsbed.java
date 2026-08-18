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
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Jurusan;
import ais.database.model.Matakuliah;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DownloadNilaiMahasiswaFormatEpsbed extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private Combobox searchfakultas = new Combobox();

	private Combobox searchjurusan = new Combobox();
	private Intbox searchangkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
	private Combobox searchprogram = new Combobox();

	private Combobox searchsemester = new Combobox();

//	private Textbox nimMahasiswa = new Textbox();
	private Combobox ta = new Combobox();
	private Textbox namaMahasiswa = new Textbox();
	private Textbox kelas = new Textbox();

	private File file;

	private Textbox kodeMatakuliah;

	public DownloadNilaiMahasiswaFormatEpsbed() {
		super();
		try {

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

			Common.initPrograms(searchprogram);
			Common.generateTahunAjaranDanSemua(ta);

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
					"Membuka jendela Unduh Nilai Mahasiswa Format EPSBED",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Unduh Nilai Mahasiswa Format EPSBED.",
							"Periksa apakah data Fakultas, Jurusan, Program Studi, dan Tahun Ajaran pada master data sudah terisi dengan benar.",
							"Jika jendela tetap gagal terbuka setelah beberapa kali percobaan, hubungi Administrator." });
		}
	}

	public DownloadNilaiMahasiswaFormatEpsbed(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Unduh Nilai Mahasiswa Format EPSBED",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Unduh Nilai Mahasiswa Format EPSBED.",
							"Periksa apakah data Fakultas, Jurusan, Program Studi, dan Tahun Ajaran pada master data sudah terisi dengan benar.",
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
		// North JANGAN di-flex bareng Center: kalau di-flex, North hanya muat baris filter dan
		// Toolbar "Tampilkan Data"/"Ambil Data" di bawahnya TERPOTONG (tombol "cari" tak terlihat).
		// Beri tinggi pasti + autoscroll agar tombol SELALU tampil/terjangkau.
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

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Smt/Kelas"));

		Hbox hbox = new Hbox();
		hbox.appendChild(searchsemester);
		hbox.appendChild(kelas);
		row.appendChild(hbox);
		searchsemester.setReadonly(true);
		searchsemester.setCols(5);
		kelas.setCols(6);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("TA"));
		row.appendChild(ta);
		ta.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		row.appendChild(namaMahasiswa);
		namaMahasiswa.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));
		row.appendChild(kodeMatakuliah = new Textbox());
		kodeMatakuliah.setWidth("90%");

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

				try {
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "NILAI_PDPT.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/penilaian/DownloadNilaiMahasiswaFormatEpsbed.java:221");

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

		final Integer semester = searchsemester.getSelectedItem() == null ? null
				: (Integer) searchsemester.getSelectedItem().getValue();

		final String kel = kelas.getValue().trim();

		Common.clear(center);

		System.out.println("init spreadsheet running");
		final Jurusan jurusan = searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: (Jurusan) searchjurusan.getSelectedItem().getValue();

		final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

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

				rowhead.createCell(0).setCellValue("SMTR");
				rowhead.createCell(1).setCellValue("KODE PRODI");
				rowhead.createCell(2).setCellValue("KELAS");
				rowhead.createCell(3).setCellValue("KODE MATAKULIAH");
				rowhead.createCell(4).setCellValue("NAMA MATAKULIAH");
				rowhead.createCell(5).setCellValue("NIM");
				rowhead.createCell(6).setCellValue("NAMA");
				rowhead.createCell(7).setCellValue("NILAI");
				rowhead.createCell(8).setCellValue("ANGKA");
				rowhead.createCell(9).setCellValue("BOBOT");
				rowhead.createCell(10).setCellValue("SEMESTER");

				Session session = HibernateUtil.currentNativeSession();

				List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)

						.add(ta.getSelectedItem() == null || ta.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", ta.getSelectedItem().getValue()))

						.setMaxResults(1048576)

						.createAlias("matakuliahKonversi", "matakuliahKonversi", Criteria.LEFT_JOIN)
						.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)
						.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)

						.add(kel.trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("perkuliahan.kelas", kel, MatchMode.ANYWHERE))

						.add(kodeMatakuliah.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.or(
										Restrictions.or(
												Restrictions.ilike("matakuliahKonversi.nama",
														kodeMatakuliah.getValue().trim(), MatchMode.ANYWHERE),
												Restrictions.ilike("matakuliah.nama", kodeMatakuliah.getValue().trim(),
														MatchMode.ANYWHERE)),
										Restrictions.or(
												Restrictions.ilike("matakuliahKonversi.kode",
														kodeMatakuliah.getValue().trim(), MatchMode.ANYWHERE),
												Restrictions.ilike("matakuliah.kode", kodeMatakuliah.getValue().trim(),
														MatchMode.ANYWHERE))))

						.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
						.add(semester.equals(-1) ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("semester", semester))

						.createAlias("mahasiswa", "mahasiswa")

						.add(

								namaMahasiswa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.or(
												Restrictions.ilike("mahasiswa.nim", namaMahasiswa.getValue().trim(),
														MatchMode.ANYWHERE),
												Restrictions.ilike("mahasiswa.nama", namaMahasiswa.getValue().trim(),
														MatchMode.ANYWHERE)))

						.add(searchjurusan.getSelectedItem() == null
								|| searchjurusan.getSelectedItem().getValue() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("mahasiswa.jurusan", jurusan))

						.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)

						.add(searchfakultas.getSelectedItem() == null
								|| searchfakultas.getSelectedItem().getValue() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

						.add(searchprogram.getSelectedItem() == null
								|| searchprogram.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("mahasiswa.program",
												searchprogram.getSelectedItem().getValue()))

						.add(searchangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("mahasiswa.tahunangkatan", searchangkatan.getValue()))

						.addOrder(Order.asc("mahasiswa.nim")).addOrder(Order.asc("semester")).list();

				int size = detailperkuliahans.size();

				int rowIndex = 1;
				for (Detailperkuliahan detailperkuliahanLain : detailperkuliahans) {

					label.setValue("Sedang memproses data " + detailperkuliahanLain.toString() + " ("
							+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

					createData(session, detailperkuliahanLain.getSemester(), sheet, rowIndex, lockedNumericStyle,
							notLocked, detailperkuliahanLain);
					rowIndex++;

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
							"Menyimpan berkas Excel hasil unduhan Nilai Mahasiswa Format EPSBED",
							e,
							new String[] {
									"Pastikan berkas dengan nama yang sama tidak sedang dibuka oleh aplikasi lain (misalnya Microsoft Excel).",
									"Periksa ketersediaan ruang penyimpanan (disk space) pada server.",
									"Ulangi proses unduh data. Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." });
				}

				System.out.println("Your excel file has been generated! " );

				HibernateUtil.closeSession();

				detailperkuliahans.clear();
				label.setValue("");
				} catch (Exception e) {
					// FIX "gagal diam-diam"/hang selamanya: sebelumnya try ini TIDAK punya catch,
					// jadi exception apa pun (mis. query Hibernate gagal) akan lolos tak
					// tertangani keluar dari run() thread ini; label.setValue("") di atas tidak
					// pernah tereksekusi, sehingga popup progres bar tidak akan pernah tertutup
					// dan pengguna tidak pernah tahu prosesnya gagal.
					Common.tampilErrorJikaAdmin(e);
					label.setValue("Error: " + PesanFormalHelper.pesanGagalException(
							"pengunduhan (Excel) data Nilai Mahasiswa Format EPSBED", null, e,
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

	private void createData(Session session, int mySemester, XSSFSheet sheet, int rowIndex,
			XSSFCellStyle lockedNumericStyle, XSSFCellStyle notLocked, Detailperkuliahan detailperkuliahan) {

		Matakuliah matakuliah = detailperkuliahan.getMatakuliahKonversi() == null
				? (detailperkuliahan.getPerkuliahan() == null ? null
						: detailperkuliahan.getPerkuliahan().getMatakuliah())
				: detailperkuliahan.getMatakuliahKonversi();
		if (matakuliah != null && detailperkuliahan.getTahunAkademik() != null) {
			try {

				XSSFRow row = sheet.createRow(rowIndex);

				String id_smt = detailperkuliahan.getTahunAkademik().split("/")[0]
						+ (detailperkuliahan.getSemester() % 2 == 0 ? "2" : "1");

				XSSFCell cell = row.createCell(0);
				cell.setCellStyle(lockedNumericStyle);
				cell.setCellValue(id_smt);

				cell = row.createCell(1);
				cell.setCellStyle(lockedNumericStyle);
				cell.setCellValue(matakuliah.getJurusan().getKodeEpsbed());

				cell = row.createCell(2);
				cell.setCellStyle(lockedNumericStyle);
				cell.setCellValue(detailperkuliahan.getPerkuliahan() == null ? ""
						: detailperkuliahan.getPerkuliahan().getKelas());

				cell = row.createCell(3);
				cell.setCellStyle(lockedNumericStyle);
				cell.setCellValue(matakuliah.getKode());

				cell = row.createCell(4);
				cell.setCellStyle(lockedNumericStyle);
				cell.setCellValue(matakuliah.getNama());

				cell = row.createCell(5);
				cell.setCellStyle(lockedNumericStyle);
				cell.setCellValue(detailperkuliahan.getMahasiswa().getNim());

				cell = row.createCell(6);
				cell.setCellStyle(lockedNumericStyle);
				cell.setCellValue(detailperkuliahan.getMahasiswa().getNama());

				cell = row.createCell(7);
				cell.setCellStyle(notLocked);
				cell.setCellValue(detailperkuliahan.getNilaiHuruf());

				cell = row.createCell(8);
				cell.setCellStyle(notLocked);
				cell.setCellValue(detailperkuliahan.getTotalNilai());

				cell = row.createCell(9);
				cell.setCellStyle(notLocked);
				cell.setCellValue(detailperkuliahan.getTotalIP());

				cell = row.createCell(10);
				cell.setCellStyle(notLocked);
				cell.setCellValue(detailperkuliahan.getSemester());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/penilaian/DownloadNilaiMahasiswaFormatEpsbed.java:461");
				// TODO: handle exception
			}
		}
	}
}
