package ais.action.master.helper.util;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.FormatNilai;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyMessageboxConfig;

public class PenilaianUtil {

	public static void downloadPenilaianKonversi(List<Long> detailperkuliahans) throws Exception {
		Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(9);
		spreadsheet.setMaxrows(detailperkuliahans.size() + 2);
		Worksheet sheet = spreadsheet.getSelectedSheet();

		int rowIndex = 0;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "ID");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "KODE");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "NAMA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "SEMESTER");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "NILAI");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "KODE ASAL");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "NAMA ASAL");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "SKS ASAL");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, "NILAI ASAL");

		rowIndex = 1;
		for (Long detailperkuliahanid : detailperkuliahans) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {
				if (detailperkuliahan.getMatakuliahKonversi() == null) {
					continue;
				}
				Matakuliah matakuliah = detailperkuliahan.getMatakuliahKonversi();
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, matakuliah.getId());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, matakuliah.getKode());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, matakuliah.getNama());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, detailperkuliahan.getSemester());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, detailperkuliahan.getTotalNilai());

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, detailperkuliahan.getKodeMatakuliahAsal());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, detailperkuliahan.getNamaMatakuliahAsal());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, detailperkuliahan.getSksAsal());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, detailperkuliahan.getNilaiHurufAsal());

				rowIndex++;
			}
		}

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		spreadsheet.getBook().write(bout);
		bout.close();
		String fileName = "template_daftar_nilai_konversi.xlsx";
		fileName = fileName.replaceAll(" ", "_");
		Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				fileName);
	}

	@SuppressWarnings("unchecked")
	public static void pindahkanSemuaKRS(Mahasiswa mahasiswa, Integer penambahan) throws Exception {
		if (mahasiswa == null || penambahan == null) {
			return;
		}
		Session session = HibernateUtil.currentSession();
		List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)
				.add(Restrictions.eq("mahasiswa", mahasiswa)).addOrder(Order.asc("semester")).addOrder(Order.asc("id"))
				.list();

		for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {
			if (detailperkuliahan != null && detailperkuliahan.getSemester() != null) {
				detailperkuliahan.setSemester(detailperkuliahan.getSemester() + penambahan);
				Common.refreshUpdate(session, detailperkuliahan);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void downloadSemuaKRS(Mahasiswa mahasiswa) throws Exception {

		List<Detailperkuliahan> detailperkuliahans = HibernateUtil.currentSession()
				.createCriteria(Detailperkuliahan.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.addOrder(Order.asc("semester")).addOrder(Order.asc("id")).list();

		Collection<Long> detailperkuliahans2 = mahasiswa.saringBerdasarNilaiDan0(mahasiswa.ambilDetailperkuliahan());

		Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(9);
		spreadsheet.setMaxrows(detailperkuliahans.size() + 2);
		Worksheet sheet = spreadsheet.getSelectedSheet();

		int rowIndex = 0;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "ID");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "KODE");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "NAMA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "SKS");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "SEMESTER");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "TAHAP");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "NILAI");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "IP");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, "HURUF");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, "T/A");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, "PERSETUJUAN");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11, "DOSEN");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12, "JENIS");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, "VALID");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14, "SKSK Per Smt");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15, "Nilai IP Per Smt");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, "IP Per Smt");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, "SKSK Total");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, "Nilai IP Total");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 19, "IPK");

		rowIndex = 1;
		int semester = 0;
		double ips = 0.0;
		double ipsTotal = 0.0;
		double sksk = 0.0;
		double skskTotal = 0.0;
		for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {
			if (detailperkuliahan != null) {

				Detailperkuliahan detailperkuliahan2 = null;
				for (Long sid : detailperkuliahans2) {
					Detailperkuliahan s = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
							sid.toString());
					if (s != null && s.getId() != null) {
						if (s.getId().equals(detailperkuliahan.getId())) {
							detailperkuliahan2 = s;
						}
					}
				}

				Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() != null
						? detailperkuliahan.getPerkuliahan().getMatakuliah()
						: detailperkuliahan.getMatakuliahKonversi();
				if (matakuliah != null) {

					Matakuliah[] matakuliahs = Common.getMatakuliahApakahEkivalen(matakuliah,
							mahasiswa == null ? null : mahasiswa.getNim(), true);
					matakuliah = matakuliahs[0];
					Matakuliah matakuliahAsli = matakuliahs[1];

					if (matakuliahAsli != null && matakuliah != null
							&& matakuliahAsli.getId().equals(matakuliah.getId())) {
						matakuliahAsli = null;
					}

					if (detailperkuliahan2 != null) {
						skskTotal += matakuliah.getSks().doubleValue();
						ipsTotal += detailperkuliahan2.getTotalIP() * matakuliah.getSks().doubleValue();
						if (!detailperkuliahan2.getSemester().equals(semester)) {
							sksk = matakuliah.getSks().doubleValue();
							ips = detailperkuliahan2.getTotalIP() * matakuliah.getSks().doubleValue();
							semester = detailperkuliahan2.getSemester();
						} else {
							sksk += matakuliah.getSks().doubleValue();
							ips += detailperkuliahan2.getTotalIP() * matakuliah.getSks().doubleValue();
						}
					}

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, matakuliah.getId()
							+ (matakuliahAsli == null ? "" : " (ekivalen: " + matakuliahAsli.getId() + ")"));
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, matakuliah.getKode()
							+ (matakuliahAsli == null ? "" : " (ekivalen: " + matakuliahAsli.getKode() + ")"));
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, matakuliah.getNama()
							+ (matakuliahAsli == null ? "" : " (ekivalen: " + matakuliahAsli.getNama() + ")"));
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, matakuliah.getSks()
							+ (matakuliahAsli == null ? "" : " (ekivalen: " + matakuliahAsli.getSks() + ")"));
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, detailperkuliahan.getSemester());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, detailperkuliahan.getTahap());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, detailperkuliahan.getTotalNilai());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, detailperkuliahan.getTotalIP());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, detailperkuliahan.getNilaiHuruf());

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, detailperkuliahan.getTahunAkademik());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10,
							detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI) ? "Ya" : "Tidak");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11,
							detailperkuliahan.getPerkuliahan() == null ? ""
									: detailperkuliahan.getPerkuliahan().populateDosen().values().toString());

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12,
							detailperkuliahan.getPerkuliahan() == null ? "Konversi"
									: (detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null ? "Bukan SP"
											: "SP"));

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13,
							detailperkuliahan2 == null ? "Tidak" : "Ya");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14, sksk);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15, ips);
					Double ipk = sksk < 0.01 ? 0.0 : ips / sksk;
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, detailperkuliahan2 == null ? "" : ipk);

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17,
							detailperkuliahan2 == null ? "" : skskTotal);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18,
							detailperkuliahan2 == null ? "" : ipsTotal);

					Double ipkTotal = skskTotal < 0.01 ? 0.0 : ipsTotal / skskTotal;
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 19,
							detailperkuliahan2 == null ? "" : ipkTotal);
					rowIndex++;
				}
			}
		}

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		spreadsheet.getBook().write(bout);
		bout.close();

		String fn = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/krs_" + mahasiswa.getNim() + "_" + mahasiswa.getNama() + "_"
						+ URLEncoder.encode(Common.dateFormat62.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

		File outputFile = new File(fn);
		ais.common.CommonFileUtil.ensureWritableFile(outputFile, Math.max(20L * 1024L * 1024L, bout.size() * 2L));
		FileOutputStream fileOut = null;
		try {
			fileOut = new FileOutputStream(outputFile);
			fileOut.write(bout.toByteArray());
		} finally {
			if (fileOut != null) fileOut.close();
		}

		Common.displayXlsx(fn, new Intbox(rowIndex), 20);

	}

	@SuppressWarnings("unchecked")
	public static void downloadSemuaKRS(String sks, Mahasiswa mahasiswa) throws Exception {

		List<Detailperkuliahan> detailperkuliahans = sks.isEmpty() ? new ArrayList<Detailperkuliahan>()
				: HibernateUtil.currentSession().createCriteria(Detailperkuliahan.class)
						.add(Restrictions.sqlRestriction("id in (" + sks + ")")).addOrder(Order.asc("semester"))
						.addOrder(Order.asc("id")).list();

		Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(9);
		spreadsheet.setMaxrows(detailperkuliahans.size() + 2);
		Worksheet sheet = spreadsheet.getSelectedSheet();

		int rowIndex = 0;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "ID");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "KODE");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "NAMA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "SKS");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "SEMESTER");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "TAHAP");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "NILAI");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "IP");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, "HURUF");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, "T/A");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, "PERSETUJUAN");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11, "DOSEN");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12, "JENIS");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, "SKSK Per Smt");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14, "Nilai IP Per Smt");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15, "IP Per Smt");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, "SKSK Total");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, "Nilai IP Total");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, "IPK");

		rowIndex = 1;
		double ips = 0.0;
		double ipsTotal = 0.0;
		double sksk = 0.0;
		double skskTotal = 0.0;
		for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {
			if (detailperkuliahan != null) {

				Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() != null
						? detailperkuliahan.getPerkuliahan().getMatakuliah()
						: detailperkuliahan.getMatakuliahKonversi();
				if (matakuliah != null) {

					Matakuliah[] matakuliahs = Common.getMatakuliahApakahEkivalen(matakuliah,
							mahasiswa == null ? null : mahasiswa.getNim(), true);
					matakuliah = matakuliahs[0];
					Matakuliah matakuliahAsli = matakuliahs[1];

					if (matakuliahAsli != null && matakuliah != null
							&& matakuliahAsli.getId().equals(matakuliah.getId())) {
						matakuliahAsli = null;
					}

					skskTotal += matakuliah.getSks().doubleValue();
					ipsTotal += detailperkuliahan.getTotalIP() * matakuliah.getSks().doubleValue();

					sksk = matakuliah.getSks().doubleValue();
					ips = detailperkuliahan.getTotalIP() * matakuliah.getSks().doubleValue();

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, matakuliah.getId()
							+ (matakuliahAsli == null ? "" : " (ekivalen: " + matakuliahAsli.getId() + ")"));
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, matakuliah.getKode()
							+ (matakuliahAsli == null ? "" : " (ekivalen: " + matakuliahAsli.getKode() + ")"));
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, matakuliah.getNama()
							+ (matakuliahAsli == null ? "" : " (ekivalen: " + matakuliahAsli.getNama() + ")"));
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, matakuliah.getSks()
							+ (matakuliahAsli == null ? "" : " (ekivalen: " + matakuliahAsli.getSks() + ")"));

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, detailperkuliahan.getSemester());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, detailperkuliahan.getTahap());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, detailperkuliahan.getTotalNilai());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, detailperkuliahan.getTotalIP());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, detailperkuliahan.getNilaiHuruf());

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, detailperkuliahan.getTahunAkademik());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10,
							detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI) ? "Ya" : "Tidak");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11,
							detailperkuliahan.getPerkuliahan() == null ? ""
									: detailperkuliahan.getPerkuliahan().populateDosen().values().toString());

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12,
							detailperkuliahan.getPerkuliahan() == null ? "Konversi"
									: (detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null ? "Bukan SP"
											: "SP"));

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, sksk);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14, ips);
					Double ipk = sksk < 0.01 ? 0.0 : ips / sksk;
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15, ipk);

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, skskTotal);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, ipsTotal);

					Double ipkTotal = skskTotal < 0.01 ? 0.0 : ipsTotal / skskTotal;
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, ipkTotal);
					rowIndex++;
				}
			}
		}

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		spreadsheet.getBook().write(bout);
		bout.close();

		String fn = Sessions.getCurrent().getWebApp().getRealPath("/tmp/krs_sks_"
				+ URLEncoder.encode(Common.dateFormat62.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

		File outputFile = new File(fn);
		ais.common.CommonFileUtil.ensureWritableFile(outputFile, Math.max(20L * 1024L * 1024L, bout.size() * 2L));
		FileOutputStream fileOut = null;
		try {
			fileOut = new FileOutputStream(outputFile);
			fileOut.write(bout.toByteArray());
		} finally {
			if (fileOut != null) fileOut.close();
		}

		Common.displayXlsx(fn, new Intbox(rowIndex), 19);

	}

	public static void downloadPenilaian(final Perkuliahan perkuliahan, final List<FormatNilai> formatNilais)
			throws Exception {

		final File file = new File(
				URLEncoder.encode("FORMAT_NILAI_PERKULIAHAN_" + perkuliahan.getId() + ".xlsx", "UTF-8"));
		if (!file.exists()) {
			file.createNewFile();
		}
		final Label label = Common.displayLoadBar(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot(),
				file);

		new Thread(new Runnable() {

			@Override
			public void run() {

				XSSFWorkbook workbook = new XSSFWorkbook();
				XSSFSheet sheet = workbook.createSheet("PENILAIAN");
				sheet.setDefaultColumnWidth(20);

				XSSFFont hlink_font = workbook.createFont();
				hlink_font.setColor(new XSSFColor(Color.RED));

				final XSSFCellStyle hlink_style = workbook.createCellStyle();
				hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				hlink_style.setFont(hlink_font);

				final XSSFCellStyle hlink_stylered = workbook.createCellStyle();
				hlink_stylered.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				hlink_stylered.setFillForegroundColor(new XSSFColor(Color.YELLOW));
				hlink_stylered.setFont(hlink_font);

				int rowIndex = 3;

				XSSFRow rowhead = sheet.createRow(rowIndex);
				XSSFCell cell;
				(cell = rowhead.createCell(1)).setCellValue(perkuliahan.infoSimple());
				cell.setCellStyle(hlink_stylered);

				rowIndex = 4;
				int colIndex = 3;
				rowhead = sheet.createRow(rowIndex);

				(cell = rowhead.createCell(1)).setCellValue("NIM");
				cell.setCellStyle(hlink_stylered);
				(cell = rowhead.createCell(2)).setCellValue("NAMA");
				cell.setCellStyle(hlink_stylered);

				XSSFRow row0 = sheet.createRow(0);

				for (FormatNilai formatNilai : formatNilais) {
					if (formatNilai.getPersen() > 0.01) {
						String col = formatNilai.getNama() + " \n" + formatNilai.getPersen() + "%";

						row0.createCell(colIndex).setCellValue(formatNilai.getStatusPertemuan().getId());
						(cell = rowhead.createCell(colIndex)).setCellValue(col);
						cell.setCellStyle(hlink_stylered);
						colIndex++;
					}
				}

				rowIndex = 5;
				colIndex = 1;

				for (Long detailperkuliahanid : perkuliahan.ambilDetailperkuliahan()) {
					Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
							.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
					if (detailperkuliahan != null) {
						Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();

						XSSFRow row = sheet.createRow(rowIndex);
						(cell = row.createCell(colIndex)).setCellValue(mahasiswa.getNim());
						cell.setCellStyle(hlink_style);

						row.createCell(colIndex + 1).setCellValue(mahasiswa.getNama().toUpperCase());

						int index = 2;
						for (FormatNilai formatNilai : formatNilais) {
							if (formatNilai.getPersen() > 0.01) {

								label.setValue("Loading nilai " + mahasiswa);
								Double nilai = detailperkuliahan.retreiveDetailNilai(formatNilai);
								(cell = row.createCell(colIndex + (index++)))
										.setCellValue(nilai == null ? 0.0 : nilai.doubleValue());
								cell.setCellStyle(hlink_style);
							}
						}

						rowIndex++;
					}
				}

				try {
					FileOutputStream fileOut = new FileOutputStream(file);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

				System.out.println("Your excel file has been generated! ");

				HibernateUtil.closeSession();

				label.setValue("");

			}
		}).start();

	}

	public static void uploadPenilaianKonversi(Mahasiswa mahasiswa, File file, Tbmuser tbmuser) throws Exception {

		XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
		XSSFSheet sheeta = workbook.getSheetAt(0);
		List<List<String>> datas = Common.getSheetContent(sheeta);

		try {

			for (List<String> strings : datas) {

				Session session = HibernateUtil.currentNativeSession();

				try {

					Matakuliah matakuliah = (Matakuliah) Common.getContentAsObject(strings.get(0), Matakuliah.class,
							null);

					if (matakuliah == null) {
						matakuliah = (Matakuliah) Common.getContentAsObject(strings.get(1), Matakuliah.class,
								Restrictions.and(Restrictions.eq("jurusan", mahasiswa.getJurusan()),
										Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))));
					}

					if (matakuliah == null) {
						matakuliah = (Matakuliah) Common.getContentAsObject(strings.get(1), Matakuliah.class,
								Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
					}

					if (matakuliah == null) {
						matakuliah = (Matakuliah) Common.getContentAsObject(strings.get(2), Matakuliah.class,
								Restrictions.and(Restrictions.eq("jurusan", mahasiswa.getJurusan()),
										Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))));
					}

					if (matakuliah == null) {
						System.out.println("Matakuliah tidak ditemukan");
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
						HibernateUtil.closeSession();
						continue;
					}

					Integer semester = Integer.parseInt(strings.get(3));
					String n = strings.get(4);
					Double nilai = 0.0;

					if (!Common.isNumber(n)) {
						NilaiHuruf nilaiHuruf = (NilaiHuruf) session.createCriteria(NilaiHuruf.class)
								.add(Restrictions.ilike("nilaiHuruf", n.trim())).setMaxResults(1).uniqueResult();
						if (nilaiHuruf != null) {
							nilai = (nilaiHuruf.getMulai() + nilaiHuruf.getSampai()) / 2.0;
						}
					} else {
						nilai = Double.parseDouble(n.trim());
					}

					String kodeAsal = strings.get(5);
					String namaAsal = strings.get(6);
					Integer sksAsal = 0;
					try {
						sksAsal = Integer.parseInt(strings.get(7));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/PenilaianUtil.java:580");

					}
					String nilaiAsal = strings.get(8);

					Detailperkuliahan detailperkuliahanBukan = (Detailperkuliahan) session
							.createCriteria(Detailperkuliahan.class).add(Restrictions.eq("mahasiswa", mahasiswa))
							.add(Restrictions.isNull("matakuliahKonversi")).createAlias("perkuliahan", "perkuliahan")
							.add(Restrictions.eq("perkuliahan.matakuliah", matakuliah)).setMaxResults(1).uniqueResult();
					if (detailperkuliahanBukan != null) {
						System.out.println("Matakuliah " + detailperkuliahanBukan + " sudah ada");
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
						HibernateUtil.closeSession();
						continue;
					}

					Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
							.createCriteria(Detailperkuliahan.class).add(Restrictions.eq("mahasiswa", mahasiswa))
							.add(Restrictions.eq("matakuliahKonversi", matakuliah)).setMaxResults(1).uniqueResult();
					System.out.println("Matakuliah " + detailperkuliahan + " sedang diproses");
					if (detailperkuliahan == null) {
						detailperkuliahan = new Detailperkuliahan(tbmuser, PenilaianUtil.class);
						detailperkuliahan.setMahasiswa(mahasiswa);
						detailperkuliahan.setMatakuliahKonversi(matakuliah);
					}

					detailperkuliahan.setTotalNilai(nilai);
					detailperkuliahan.setSemester(semester);
					detailperkuliahan.setNilaiHurufAsal(nilaiAsal);
					detailperkuliahan.setSksAsal(sksAsal);
					detailperkuliahan.setKodeMatakuliahAsal(kodeAsal);
					detailperkuliahan.setNamaMatakuliahAsal(namaAsal);

					NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(nilai, mahasiswa.getTahunangkatan(),
							mahasiswa.getJurusan(), mahasiswa.getJurusan().getFakultas(),
							detailperkuliahan.getTahunAkademik(),
							detailperkuliahan.getPerkuliahan() == null ? null
									: detailperkuliahan.getPerkuliahan().getGanjilGenap(),
							matakuliah == null ? "" : matakuliah.getKode(),
							matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
					detailperkuliahan.setTotalIP(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
					detailperkuliahan.setTotalNilai(nilai);
					detailperkuliahan.setNilaiHuruf(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
					detailperkuliahan.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());

					Double totalSementara = nilai;
					nilaiHuruf = Common.getNilaiHuruf(totalSementara,
							detailperkuliahan.getMahasiswa().getTahunangkatan(),
							detailperkuliahan.getMahasiswa().getJurusan(),
							detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
							detailperkuliahan.getTahunAkademik(),
							detailperkuliahan.getPerkuliahan() == null ? null
									: detailperkuliahan.getPerkuliahan().getGanjilGenap(),
							matakuliah == null ? "" : matakuliah.getKode(),
							matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

					detailperkuliahan.setTotalNilaiSementara(totalSementara);
					detailperkuliahan.setNilaiHurufSementara(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
					detailperkuliahan.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

					session.getTransaction().begin();
					session.saveOrUpdate(detailperkuliahan);
					session.getTransaction().commit();
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				// session.disconnect();
				if (session.isOpen()) {session.disconnect();session.close();}
				HibernateUtil.closeSession();
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);

		}

	}

	/** Baca nilai numerik dari sel kolom tertentu pada satu baris; aman (0.0 bila kosong/invalid). */
	private static Double bacaNilaiSel(List<String> strings, int col) {
		try {
			if (strings == null || col < 0 || col >= strings.size()) {
				return 0.0;
			}
			String dn = strings.get(col);
			if (dn == null || dn.trim().isEmpty()) {
				return 0.0;
			}
			return Double.parseDouble(dn.trim());
		} catch (Exception e) {
			return 0.0;
		}
	}

	/**
	 * Normalisasi nama kolom header untuk pencocokan ke {@code FormatNilai.getNama()}:
	 * buang bagian setelah baris-baru dan akhiran persen (mis. "CPMK022 \n32.0%" â†’ "cpmk022"),
	 * lalu di-trim &amp; huruf-kecil.
	 */
	private static String normalisasiNamaKolom(String h) {
		if (h == null) {
			return "";
		}
		String s = h.trim();
		int nl = s.indexOf('\n');
		if (nl >= 0) {
			s = s.substring(0, nl);
		}
		int cr = s.indexOf('\r');
		if (cr >= 0) {
			s = s.substring(0, cr);
		}
		// buang akhiran persen yang menempel, mis. "CPMK022 32.0%" / "CPMK022 32%"
		s = s.replaceAll("\\s*\\d+([.,]\\d+)?\\s*%\\s*$", "");
		return s.trim().toLowerCase();
	}

	public static void uploadPenilaian(Perkuliahan perkuliahan, File file, List<FormatNilai> formatNilais,
			EventListener onPerubahanNilai, EventListener eventListener) throws Exception {

		uploadPenilaianBaru(perkuliahan, file, formatNilais, onPerubahanNilai, eventListener);

	}

	public static void uploadPenilaianBaru(final Perkuliahan perkuliahan, final File file,
			final List<FormatNilai> formatNilais, final EventListener onPerubahanNilai,
			final EventListener eventListener) throws Exception {
		final Tbmuser tbmuser = Common.getCurrentUser();
		final List<String> errors = new ArrayList<String>();
		final Label downloadPath = new Label("");
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Nilai");
		final Label label = Common.displayLoadBar(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (onPerubahanNilai != null) {
					onPerubahanNilai.onEvent(null);
				}

				if (!downloadPath.getValue().isEmpty()) {
					try {
						Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain");
					} catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) PenilaianUtil download laporan"); }
				}

				MyMessageboxConfig.show(
						errors.isEmpty() ? "Tidak terdapat data yang berhasil diunggah."
								: errors.toString().replaceAll("\\[", "\n").replaceAll("\\]", ""),
						"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheeta = workbook.getSheetAt(0);

					List<List<String>> datas = Common.getSheetContent(sheeta);
					List<String> pertemuans = datas.get(0);

					System.out.println("pertemuans ->" + pertemuans);

					// Peta NAMA kolom (CPMK) -> indeks kolom, dibaca dari baris header yang sel
					// ke-2-nya berisi "NIM". Dipakai sebagai FALLBACK pencocokan kolom ke
					// FormatNilai ketika baris id-pertemuan (pertemuans) tidak ada/ tidak cocok â€”
					// mis. format Excel berheader "CPMK022 \n32.0%" yang FormatNilai-nya tidak
					// punya statusPertemuan. Tanpa ini, nilai tidak terbaca dan tersimpan 0.
					java.util.Map<String, Integer> kolomByNamaCpmk = new java.util.HashMap<String, Integer>();
					for (List<String> baris : datas) {
						if (baris == null || baris.size() < 2) {
							continue;
						}
						String selNim = baris.get(1);
						if (selNim != null && selNim.trim().equalsIgnoreCase("nim")) {
							for (int col = 3; col < baris.size(); col++) {
								String norm = normalisasiNamaKolom(baris.get(col));
								if (!norm.isEmpty() && !kolomByNamaCpmk.containsKey(norm)) {
									kolomByNamaCpmk.put(norm, Integer.valueOf(col));
								}
							}
							break;
						}
					}

					int size = (datas.size() + 1);

					Collection<Long> dataPerkuliahans = perkuliahan.ambilDetailperkuliahan();
					int i = 0;
					for (List<String> strings : datas) {
						try {
							i++;
							// Guard: baris yang kolomnya kurang dari 2 (mis. baris kosong/format ganjil)
							// membuat strings.get(1) melempar IndexOutOfBoundsException â†’ lewati baris.
							if (strings == null || strings.size() < 2) {
								continue;
							}
							String nim = strings.get(1);
							System.out.println("nim = " + nim);
							if (nim == null || nim.trim().isEmpty() || nim.trim().equalsIgnoreCase("nim")) {
								continue;
							}

							Detailperkuliahan detailperkuliahan = null;
							for (Long did : dataPerkuliahans) {
								Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject
										.ambilData(Detailperkuliahan.class, did.toString());
								if (d != null) {
									if (d.getMahasiswa() != null && d.getMahasiswa().getNim() != null
											&& d.getMahasiswa().getNim().equalsIgnoreCase(nim)) {
										detailperkuliahan = d;
										break;
									}
								}
							}
							System.out.println("detailperkuliahan = " + detailperkuliahan);
							if (detailperkuliahan != null) {

								label.setValue("Upload nilai " + nim + " " + detailperkuliahan.getMahasiswa().getNama()
										+ " (" + Common.numberFormat.get().format((i * 100.0) / size) + "%)");

								Double total = 0.0;
								for (FormatNilai formatNilai : formatNilais) {
									Double nilai = detailperkuliahan.retreiveDetailNilai(formatNilai);
									boolean cocok = false;

									// (1) Cocokkan kolom via id pertemuan pada baris header-id (format lama).
									for (int col = 3; col < strings.size(); col++) {
										String d = (pertemuans != null && col < pertemuans.size())
												? pertemuans.get(col) : null;
										if (d != null && !d.trim().isEmpty()) {
											Long idPertemuan;
											try {
												idPertemuan = (long) Double.parseDouble(d.trim());
											} catch (NumberFormatException nfeIdPertemuan) {
												// kolom ini bukan id pertemuan numerik (mis. label) â†’ lewati
												continue;
											}
											if (formatNilai.getStatusPertemuan() != null
													&& idPertemuan.equals(formatNilai.getStatusPertemuan().getId())) {
												nilai = bacaNilaiSel(strings, col);
												cocok = true;
												break;
											}
										}
									}

									// (2) FALLBACK: cocokkan kolom via NAMA (CPMK) pada baris header nama.
									// Diperlukan untuk format Excel berheader CPMK ("CPMK022 \n32.0%") yang
									// FormatNilai-nya tidak memiliki statusPertemuan, sehingga (1) tak pernah
									// cocok dan nilai sebelumnya selalu tersimpan 0.
									if (!cocok && formatNilai.getNama() != null) {
										Integer colNama = kolomByNamaCpmk
												.get(normalisasiNamaKolom(formatNilai.getNama()));
										if (colNama != null && colNama.intValue() < strings.size()) {
											nilai = bacaNilaiSel(strings, colNama.intValue());
											cocok = true;
										}
									}

									if (nilai == null) {
										nilai = 0.0;
									}
									// Total hanya dari komponen yang kolomnya berhasil dicocokkan (perilaku
									// lama dipertahankan: komponen tak tercocok tidak menambah total).
									if (cocok) {
										total += (nilai * (formatNilai.getPersen() / 100.0));
									}

									detailperkuliahan.populateDetailNilai(formatNilai, null, nilai, true, tbmuser);

								}
								Matakuliah matakuliah = detailperkuliahan == null ? null
										: detailperkuliahan.getPerkuliahan() != null
												? detailperkuliahan.getPerkuliahan().getMatakuliah()
												: detailperkuliahan.getMatakuliahKonversi();
								NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total,
										detailperkuliahan.getMahasiswa().getTahunangkatan(),
										detailperkuliahan.getMahasiswa().getJurusan(),
										detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
										detailperkuliahan.getTahunAkademik(),
										detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
												: Perkuliahan.GANJIL,
										matakuliah == null ? "" : matakuliah.getKode(),
										matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

								if (nilaiHuruf != null) {
									detailperkuliahan.setTotalIP(nilaiHuruf.getNilaiDiIPK());
									detailperkuliahan.setTotalNilai(total);
									detailperkuliahan.setNilaiHuruf(nilaiHuruf.getNilaiHuruf());
									detailperkuliahan.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());

									Double totalSementara = total;
									nilaiHuruf = Common.getNilaiHuruf(totalSementara,
											detailperkuliahan.getMahasiswa().getTahunangkatan(),
											detailperkuliahan.getMahasiswa().getJurusan(),
											detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
											detailperkuliahan.getTahunAkademik(),
											detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
													: Perkuliahan.GANJIL,
											matakuliah == null ? "" : matakuliah.getKode(),
											matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

									detailperkuliahan.setTotalNilaiSementara(totalSementara);
									detailperkuliahan.setNilaiHurufSementara(
											nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
									detailperkuliahan
											.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

									Session session = HibernateUtil.currentNativeSession();
									session.getTransaction().begin();
									Common.refreshUpdate(session, detailperkuliahan);
									session.getTransaction().commit();
									HibernateUtil.closeSession();
									HibernateUtil.closeSession();
									errors.add("Upload nilai SUKSES, " + detailperkuliahan.getMahasiswa().getNama()
											+ " nilai " + total + " " + nilaiHuruf.getNilaiHuruf());
									report.sukses(i, detailperkuliahan.getMahasiswa().getNim() + " " + detailperkuliahan.getMahasiswa().getNama(), "Nilai " + total + " (" + nilaiHuruf.getNilaiHuruf() + ")");
								} else {
									errors.add("Nilai huruf " + total
											+ " tidak ditemukan, gagal upload nilai mahasiswa dengan NIM " + nim
											+ " nama " + detailperkuliahan.getMahasiswa().getNama()
											+ " pada perkuliahan " + perkuliahan.infoSimple());
									report.gagal(i, "NIM: " + nim, "Nilai huruf untuk total " + total + " tidak ditemukan dalam konfigurasi nilai huruf.", "Pastikan konfigurasi nilai huruf (grade) untuk prodi/jurusan ini sudah lengkap.");
								}
							}

						} catch (Exception e) {
							String nimInfo = "Baris " + i;
							try { nimInfo = "NIM: " + strings.get(1); } catch (Exception eNim) {}
							errors.add("[GAGAL] " + nimInfo + " - " + e.getClass().getSimpleName() + ": " + e.getMessage());
							report.gagal(i, nimInfo, e, "Periksa data pada baris ini. Pastikan NIM mahasiswa valid dan format nilai benar.");
							Common.tampilErrorJikaAdmin(e);
						}

					}
					dataPerkuliahans = null;
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				try {
					java.io.File reportFile = report.simpanLaporan();
					downloadPath.setValue(reportFile.getAbsolutePath());
				} catch (Exception eReport) { ais.common.ErrorAuditUtil.record(eReport, "auto-audit(empty-catch) PenilaianUtil report gen"); }
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

}

