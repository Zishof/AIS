package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.util.CellRangeAddress;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
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
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.NilaiHuruf;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanRekapNilaiHurufMahasiswaProdi extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAjaran;
	private Combobox ganjilGenap;

	private File file;

	public LaporanRekapNilaiHurufMahasiswaProdi() {
		super();
		try {
			Common.initFakultasDanJurusan(null, null, searchfakultas, searchjurusan);
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Nilai Huruf Mahasiswa Prodi", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapNilaiHurufMahasiswaProdi(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			Common.initFakultasDanJurusan(null, null, searchfakultas, searchjurusan);
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Nilai Huruf Mahasiswa Prodi", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
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
		north.setHeight("160px");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas *"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi *"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
		row.appendChild(tahunAjaran);
		tahunAjaran.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		ganjilGenap = new Combobox();
		row.appendChild(ganjilGenap);
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		ganjilGenap.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		ganjilGenap.appendChild(comboitem);
		Common.selectComboItem(ganjilGenap, Common.getSemesterString());
		ganjilGenap.setReadonly(true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig search = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				System.out.println("search");
				initSpreadsheet();
			}
		});
		search.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Ambil File", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				try {
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
							"REKAPITULASI NILAI HURUF MAHASISWA.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanRekapNilaiHurufMahasiswaProdi.java:171");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Nilai Huruf Mahasiswa Prodi", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});

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

		Common.clear(center);
		System.out.println("init spreadsheet running");

		if (tahunAjaran.getSelectedItem() == null || ganjilGenap.getSelectedItem() == null) {
			return;
		}
		if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null)
			return;
		if (searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null)
			return;

		final String ta = (String) tahunAjaran.getSelectedItem().getValue();
		final String smt = (String) ganjilGenap.getSelectedItem().getValue();
		final Fakultas fak = (Fakultas) (searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue());
		final Jurusan jur = (Jurusan) (searchjurusan.getSelectedItem() == null ? null
				: searchjurusan.getSelectedItem().getValue());
		final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		new Thread(new Runnable() {

			@Override
			public void run() {

				XSSFWorkbook workbook = new XSSFWorkbook();
				XSSFSheet sheet = workbook.createSheet("DATA");
				sheet.setDefaultColumnWidth(7);

				XSSFRow rowhead = sheet.createRow((short) 0);
				XSSFRow rowhead1 = sheet.createRow((short) 1);
				XSSFRow rowhead2 = sheet.createRow((short) 2);

				rowhead.createCell(0).setCellValue("No.");
				rowhead.createCell(1).setCellValue("Nilai Huruf");

				int firstRow = rowhead.getRowNum();
				int lastRow = 2;
				int firstCol = 0;
				int lastCol = 0;
				System.out.println("Sangat Atas firstRow " + firstRow + ", lastRow = " + lastRow + ", firstCol = "
						+ firstCol + ", lastCol = " + lastCol);
				sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));

				firstRow = rowhead.getRowNum();
				lastRow = 2;
				firstCol = 1;
				lastCol = 1;
				System.out.println("Sangat Atas firstRow " + firstRow + ", lastRow = " + lastRow + ", firstCol = "
						+ firstCol + ", lastCol = " + lastCol);
				sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));

				Session session = ais.action.report.Report.openNativeSession();
				List<Fakultas> fakultases = session.createCriteria(Fakultas.class)
						.add(fak != null ? Restrictions.eq("id", fak.getId()) : Restrictions.sqlRestriction("true"))
						.addOrder(Order.asc("nama")).list();

				int qtyLk = 2;

				for (Fakultas fakultas : fakultases) {
					try {
						List<Jurusan> jurusans = session.createCriteria(Jurusan.class)
								.add(jur != null ? Restrictions.eq("id", jur.getId())
										: Restrictions.sqlRestriction("true"))
								.add(Restrictions.eq("fakultas", fakultas)).list();
						rowhead.createCell(qtyLk).setCellValue(fakultas.getNama());

						firstRow = rowhead.getRowNum();
						lastRow = rowhead.getRowNum();
						firstCol = qtyLk;
						lastCol = (qtyLk + (jurusans.size() * 2)) - 1;
						System.out.println("Atas firstRow " + firstRow + ", lastRow = " + lastRow + ", firstCol = "
								+ firstCol + ", lastCol = " + lastCol);
						try {
							sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanRekapNilaiHurufMahasiswaProdi.java:267");
							// TODO: handle exception
						}

						for (Jurusan jurusan : jurusans) {
							rowhead1.createCell(qtyLk).setCellValue(jurusan.getNama());

							rowhead2.createCell(qtyLk).setCellValue("JML");
							rowhead2.createCell(qtyLk + 1).setCellValue("%");

							firstRow = rowhead1.getRowNum();
							lastRow = rowhead1.getRowNum();
							firstCol = qtyLk;
							lastCol = (qtyLk + 2) - 1;
							System.out.println("Bawah firstRow " + firstRow + ", lastRow = " + lastRow + ", firstCol = "
									+ firstCol + ", lastCol = " + lastCol);
							sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));

							qtyLk += 2;
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapNilaiHurufMahasiswaProdi.java:288");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Nilai Huruf Mahasiswa Prodi", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
					}
				}

				rowhead.createCell(qtyLk).setCellValue("TOTAL");
				firstRow = rowhead.getRowNum();
				lastRow = 1;
				firstCol = qtyLk;
				lastCol = qtyLk + 2;
				System.out.println("Sangat Samping firstRow " + firstRow + ", lastRow = " + lastRow + ", firstCol = "
						+ firstCol + ", lastCol = " + lastCol);
				sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));

				rowhead2.createCell(qtyLk).setCellValue("JML");

				qtyLk++;

				rowhead2.createCell(qtyLk).setCellValue("%");

				qtyLk++;

				// rowhead2.createCell(qtyLk).setCellValue("JML");
				//
				// qtyLk++;

				int rowIndex = 3;
				int semester = smt.equalsIgnoreCase(Perkuliahan.GANJIL) ? 1 : 0;
				int jumlahKolomTotal = 2;
				List<String> nilaiHuruf = session.createCriteria(NilaiHuruf.class)
						.setProjection(Projections.groupProperty("nilaiHuruf")).addOrder(Order.asc("nilaiHuruf"))
						.add(Restrictions.ne("nilaiHuruf", "")).add(Restrictions.isNotNull("nilaiHuruf")).list();

				TreeSet<String> nilais = new TreeSet<String>();
				for (String s : nilaiHuruf) {
					if (s != null && !s.trim().isEmpty()) {
						nilais.add(s.trim());
					}
				}

				int i = 0;
				for (String nh : nilais) {
					try {

						XSSFRow row = sheet.createRow(rowIndex);

						rowIndex++;

						row.createCell(0).setCellValue((i + 1));
						row.createCell(1).setCellValue(nh);

						label.setValue("Sedang memproses data nilai huruf " + nh + " ("
								+ Common.numberFormat.get().format(i * 100.0 / 7) + " %)");

						qtyLk = 2;
						int jumlahKolom = 2;

						for (Fakultas fakultas : fakultases) {

							List<Jurusan> jurusans = session.createCriteria(Jurusan.class)
									.add(jur != null ? Restrictions.eq("id", jur.getId())
											: Restrictions.sqlRestriction("true"))
									.add(Restrictions.eq("fakultas", fakultas)).list();
							for (Jurusan jurusan : jurusans) {

								Integer jml = ((Number) session.createCriteria(Detailperkuliahan.class)

										.add(Restrictions.eq("nilaiHuruf", nh))

										.setProjection(Projections.rowCount())

										.createAlias("mahasiswa", "mahasiswa")

										.add(Restrictions.eq("tahunAkademik", ta))

										.add(Restrictions.sqlRestriction("this_.semester % 2=" + semester))

										.add(Restrictions.eq("mahasiswa.jurusan", jurusan))

										.uniqueResult()).intValue();

								jumlahKolom++;

								row.createCell(qtyLk).setCellValue(jml);

								XSSFCell cell = row.createCell(qtyLk + 1);
								String strFormula = "(" + Common.ALPABED[qtyLk] + (i + 4) + "*100.0)/"
										+ Common.ALPABED[qtyLk] + (nilais.size() + 4);
								cell.setCellType(XSSFCell.CELL_TYPE_FORMULA);
								cell.setCellFormula(strFormula);

								qtyLk += 2;

							}

							jumlahKolomTotal = jumlahKolom;
						}

						Integer jml = ((Number) session.createCriteria(Detailperkuliahan.class)

								.add(Restrictions.eq("nilaiHuruf", nh))

								.setProjection(Projections.rowCount())

								.add(Restrictions.eq("tahunAkademik", ta))

								.add(Restrictions.sqlRestriction("this_.semester %2=" + semester))

								.uniqueResult()).intValue();

						row.createCell(qtyLk).setCellValue(jml);

						XSSFCell cell = row.createCell(qtyLk + 1);
						String strFormula = "(" + Common.ALPABED[qtyLk] + (i + 4) + "*100.0)/" + Common.ALPABED[qtyLk]
								+ (nilais.size() + 4);
						cell.setCellType(XSSFCell.CELL_TYPE_FORMULA);
						cell.setCellFormula(strFormula);

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Nilai Huruf Mahasiswa Prodi", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
					}
					i++;
				}

				XSSFRow row = sheet.createRow(rowIndex);
				row.createCell(0).setCellValue("TOTAL");

				firstRow = rowIndex;
				lastRow = rowIndex;
				firstCol = 0;
				lastCol = 1;
				System.out.println("Sangat Samping firstRow " + firstRow + ", lastRow = " + lastRow + ", firstCol = "
						+ firstCol + ", lastCol = " + lastCol);
				sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));

				for (i = 2; i < ((jumlahKolomTotal * 2)); i++) {
					XSSFCell cell = row.createCell(i);
					String strFormula = "SUM(" + Common.ALPABED[i] + "4:" + Common.ALPABED[i] + "" + (nilais.size() + 3)
							+ ")";
					cell.setCellType(XSSFCell.CELL_TYPE_FORMULA);
					cell.setCellFormula(strFormula);
				}

				rowIndex++;

				row = sheet.createRow(rowIndex);
				row.createCell(0).setCellValue("IPK Rata-Rata");

				qtyLk = 2;
				for (Fakultas fakultas : fakultases) {

					List<Jurusan> jurusans = session.createCriteria(Jurusan.class)
							.add(jur != null ? Restrictions.eq("id", jur.getId()) : Restrictions.sqlRestriction("true"))
							.add(Restrictions.eq("fakultas", fakultas)).list();
					for (Jurusan jurusan : jurusans) {
						Number jml = ((Number) session.createCriteria(KrsMahasiswa.class)

								.setProjection(Projections.avg("ipk")).add(Restrictions.gt("ipk", 0.1))

								.createAlias("mahasiswa", "mahasiswa")

								.add(Restrictions.eq("tahunAkademik", ta))

								.add(Restrictions.sqlRestriction("this_.semester % 2=" + semester))

								.add(Restrictions.eq("mahasiswa.jurusan", jurusan))

								.uniqueResult());

						row.createCell(qtyLk).setCellValue(jml == null ? 0.0 : jml.doubleValue());

						qtyLk += 2;
					}
				}

				Number jml = ((Number) session.createCriteria(KrsMahasiswa.class)

						.setProjection(Projections.avg("ipk")).add(Restrictions.gt("ipk", 0.1))

						.add(Restrictions.eq("tahunAkademik", ta))

						.add(Restrictions.sqlRestriction("this_.semester % 2=" + semester))

						.uniqueResult());

				row.createCell(qtyLk).setCellValue(jml == null ? 0.0 : jml.doubleValue());

				rowIndex++;

				Common.setStyled(sheet);sizedata.setValue(rowIndex + 1);

				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

				System.out.println("Your excel file has been generated! " );

				ais.action.report.Report.closeCurrentSessionQuietly();

				ais.action.report.helper.LoadingReportUtil.selesai(label);
			}
		}).start();

	}
}
