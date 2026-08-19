package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.JenisTinggalMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanRekapJumlahMahasiswaPerTempatTinggalSaatKuliah extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAjaran;
	private Combobox ganjilGenap;
	private Combobox status;

	private File file;

	private Combobox program;

	public LaporanRekapJumlahMahasiswaPerTempatTinggalSaatKuliah() {
		super();
		try {

			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Jumlah Mahasiswa Per Tempat Tinggal Saat Kuliah", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapJumlahMahasiswaPerTempatTinggalSaatKuliah(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Jumlah Mahasiswa Per Tempat Tinggal Saat Kuliah", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void init() throws Exception {
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		// FIX toolbar/tombol tidak tampil: pada ZK5 region North memakai tinggi bawaan
		// (+-100px); dengan flex=true isinya diregangkan ke tinggi tersebut sehingga
		// Toolbar yang diletakkan DI BAWAH grid filter ikut terpotong. Disamakan dengan
		// layar sejenis yang sudah benar (DownloadMahasiswa, DownloadKrs, DownloadNilai):
		// flex dimatikan + tinggi eksplisit. Autoscroll sebagai pengaman bila isi bertambah.
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");
		

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
		row.appendChild(tahunAjaran);
		tahunAjaran.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		ganjilGenap = new Combobox();
		row.appendChild(ganjilGenap);
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		ganjilGenap.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		ganjilGenap.appendChild(comboitem);
		Common.selectComboItem(ganjilGenap, Common.getSemesterString());
		ganjilGenap.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		status = new Combobox();
		Common.insertCombo(status, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
		row.appendChild(status);
		status.setWidth("90%");
		Common.selectComboItem(status, ConstantValues.AKTIF);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		program = Common.initPrograms(null);
		row.appendChild(program);
		program.setWidth("90%");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig search = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				System.out.println("search");
				initSpreadsheet();
			}
		});
		search.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("CETAK", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				try {
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
							"REKAPITULASI JUMLAH MAHASISWA.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanRekapJumlahMahasiswaPerTempatTinggalSaatKuliah.java:184");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Jumlah Mahasiswa Per Tempat Tinggal Saat Kuliah", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
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

		initSpreadsheet();
	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

		Common.clear(center);
		System.out.println("init spreadsheet running");

		if (tahunAjaran.getSelectedItem() == null || ganjilGenap.getSelectedItem() == null) {
			return;
		}

		final String ta = (String) tahunAjaran.getSelectedItem().getValue();
		final String smt = (String) ganjilGenap.getSelectedItem().getValue();
		final StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (status.getSelectedItem() == null || status.getSelectedItem().getValue() == null ? null
				: status.getSelectedItem().getValue());
		final Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? null
				: searchfakultas.getSelectedItem().getValue());
		final Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null || searchjurusan.getSelectedItem().getValue()==null ? null
				: searchjurusan.getSelectedItem().getValue());

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/data_" + URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

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

				rowhead.createCell(0).setCellValue("No.");
				rowhead.createCell(1).setCellValue("Smt");

				int firstRow = rowhead.getRowNum();
				int lastRow = 1;
				int firstCol = 0;
				int lastCol = 0;
				System.out.println("Sangat Atas firstRow " + firstRow + ", lastRow = " + lastRow + ", firstCol = "
						+ firstCol + ", lastCol = " + lastCol);
				sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));

				firstRow = rowhead.getRowNum();
				lastRow = 1;
				firstCol = 1;
				lastCol = 1;
				System.out.println("Sangat Atas firstRow " + firstRow + ", lastRow = " + lastRow + ", firstCol = "
						+ firstCol + ", lastCol = " + lastCol);
				sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));

				Session session = ais.action.report.Report.openNativeSession();
				List<JenisTinggalMahasiswa> jenisTinggalMahasiswaes = session
						.createCriteria(JenisTinggalMahasiswa.class).addOrder(Order.asc("nama")).list();
				jenisTinggalMahasiswaes.add(null);
				int qtyLk = 2;

				for (JenisTinggalMahasiswa jenisTinggalMahasiswa : jenisTinggalMahasiswaes) {

					rowhead.createCell(qtyLk).setCellValue(
							jenisTinggalMahasiswa == null ? "Tidak dipilih" : jenisTinggalMahasiswa.getNama());

					firstRow = rowhead.getRowNum();
					lastRow = rowhead.getRowNum();
					firstCol = qtyLk;
					lastCol = (qtyLk + 3) - 1;
					System.out.println("Atas firstRow " + firstRow + ", lastRow = " + lastRow + ", firstCol = "
							+ firstCol + ", lastCol = " + lastCol);
					sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));

					rowhead1.createCell(qtyLk).setCellValue("L");
					rowhead1.createCell(qtyLk + 1).setCellValue("P");
					rowhead1.createCell(qtyLk + 2).setCellValue("JML");

					qtyLk += 3;

				}

				rowhead.createCell(qtyLk).setCellValue("TOTAL");
				firstRow = rowhead.getRowNum();
				lastRow = rowhead.getRowNum();
				firstCol = qtyLk;
				lastCol = qtyLk + 2;
				System.out.println("Sangat Samping firstRow " + firstRow + ", lastRow = " + lastRow + ", firstCol = "
						+ firstCol + ", lastCol = " + lastCol);
				sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));

				rowhead1.createCell(qtyLk).setCellValue("L");

				qtyLk++;

				rowhead1.createCell(qtyLk).setCellValue("P");

				qtyLk++;

				rowhead1.createCell(qtyLk).setCellValue("JML");

				qtyLk++;

				int rowIndex = 2;
				int semester = smt.equalsIgnoreCase(Perkuliahan.GANJIL) ? 1 : 2;
				int jumlahKolomTotal = 2;
				for (int i = 1; i <= 7; i++) {
					try {
						XSSFRow row = sheet.createRow(rowIndex);

						rowIndex++;

						row.createCell(0).setCellValue(i);
						row.createCell(1).setCellValue(semester);

						label.setValue("Sedang memproses data semester " + semester + " ("
								+ Common.numberFormat.get().format(i * 100.0 / 7) + " %)");

						qtyLk = 2;
						int jumlahKolom = 2;
						int totalLaki = 0;
						int totalPerempuan = 0;
						for (JenisTinggalMahasiswa jenisTinggalMahasiswa : jenisTinggalMahasiswaes) {

							jumlahKolom++;
							try {

								Integer jmlLaki = ((Number) session
										.createSQLQuery("select count(*) as jumlah from history_status_mahasiswa a "
												+ "inner join mahasiswa b on (a.mahasiswa=b.id) "
												+ "inner join jurusan b1 on (b.jurusan=b1.id) "
												+ "left join biodata_mahasiswa c on (c.mahasiswa=b.id) where 1=1 "
												+ (statusMahasiswa == null ? ""
														: " and a.status_mahasiswa=" + statusMahasiswa.getId())
												+ " and a.semester=" + semester + " and a.tahunakademik='" + ta + "' "
												+ "and b.kelamin ilike '%Laki-laki%' "
												+ (program.getSelectedItem() == null||program.getSelectedItem().getValue() == null ? ""
														: " and b.program = '" + program.getSelectedItem().getValue()
																+ "' ")
												+ (fakultas == null ? "" : " and b1.fakultas=" + fakultas.getId())
												+ (jurusan == null ? "" : " and b.jurusan=" + jurusan.getId())

												+ (jenisTinggalMahasiswa == null
														? " and c.jenis_tinggal_mahasiswa is null "
														: " and c.jenis_tinggal_mahasiswa="
																+ jenisTinggalMahasiswa.getId()))
										.uniqueResult()).intValue();

								Integer jmlPerempuan = ((Number) session
										.createSQLQuery("select count(*) as jumlah from history_status_mahasiswa a "
												+ "inner join mahasiswa b on (a.mahasiswa=b.id) "
												+ "inner join jurusan b1 on (b.jurusan=b1.id) "
												+ "left join biodata_mahasiswa c on (c.mahasiswa=b.id) where 1=1 "
												+ (statusMahasiswa == null ? ""
														: " and a.status_mahasiswa=" + statusMahasiswa.getId())
												+ " and a.semester=" + semester + " and a.tahunakademik='" + ta + "' "
												+ "and b.kelamin ilike '%Perempuan%' "
												+ (program.getSelectedItem() == null||program.getSelectedItem().getValue() == null ? ""
														: " and b.program = '" + program.getSelectedItem().getValue()
																+ "' ")

												+ (fakultas == null ? "" : " and b1.fakultas=" + fakultas.getId())
												+ (jurusan == null ? "" : " and b.jurusan=" + jurusan.getId())

												+ (jenisTinggalMahasiswa == null
														? " and c.jenis_tinggal_mahasiswa is null "
														: " and c.jenis_tinggal_mahasiswa="
																+ jenisTinggalMahasiswa.getId()))
										.uniqueResult()).intValue();

								row.createCell(qtyLk).setCellValue(jmlLaki);
								row.createCell(qtyLk + 1).setCellValue(jmlPerempuan);
								row.createCell(qtyLk + 2).setCellValue((jmlLaki + jmlPerempuan));

								totalLaki += jmlLaki;
								totalPerempuan += jmlPerempuan;

								System.out.println("qtyLk = " + qtyLk);

								qtyLk += 3;
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e); 
								PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Jumlah Mahasiswa Per Tempat Tinggal Saat Kuliah", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
							}

							jumlahKolomTotal = jumlahKolom;
						}

						row.createCell(qtyLk).setCellValue(totalLaki);
						row.createCell(qtyLk + 1).setCellValue(totalPerempuan);
						row.createCell(qtyLk + 2).setCellValue((totalLaki + totalPerempuan));

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e); 
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Jumlah Mahasiswa Per Tempat Tinggal Saat Kuliah", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
					}

					semester += 2;
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

				for (int i = 2; i < ((jumlahKolomTotal * 3) - 1); i++) {
					XSSFCell cell = row.createCell(i);
					String strFormula = "SUM(" + Common.ALPABED[i] + "3:" + Common.ALPABED[i] + "9)";
					cell.setCellType(XSSFCell.CELL_TYPE_FORMULA);
					cell.setCellFormula(strFormula);
				}

				rowIndex++;

				ais.action.report.helper.LoadingReportUtil.selesai(label);
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

			}
		}).start();

	}
}
