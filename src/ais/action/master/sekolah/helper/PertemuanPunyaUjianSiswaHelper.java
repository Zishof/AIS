package ais.action.master.sekolah.helper;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.jsoup.Jsoup;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.SertifikatAction;
import ais.action.master.UjianAction;
import ais.action.master.dashboard.admin.RekapHasilUjian;
import ais.action.master.dashboard.admin.RekapHasilUjianPerVoPertemuan;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.DetailUjianHelper;
import ais.action.master.helper.FormatPenilaianHelper;
import ais.action.master.helper.HasilUjianHelper;
import ais.action.master.helper.HasilUjianSiswaHelper;
import ais.action.master.helper.ProsesUjianHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.RevisiPertemuanPunyaUjianHelper;
import ais.action.master.helper.TampilDetailNilaiInterface;
import ais.action.master.helper.generic.AmbilDataUjianBanyak;
import ais.common.Common;
import ais.common.CommonEmail;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.Dosen;
import ais.database.model.FormatNilai;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.HasilUjianMahasiswaDetail;
import ais.database.model.Konfigurasi;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.RuangPaketPMB;
import ais.database.model.Tbmuser;
import ais.database.model.Ujian;
import ais.database.model.UjianPunyaSoal;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.DetailGrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.DetailGrupPenilaian;
import ais.database.model.sekolah.DetailJenisPenilaian;
import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;
import ais.database.model.sekolah.JenisPenilaian;
import ais.database.model.sekolah.KategoriItemPenilaianSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyArrayList;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHashMap;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTimebox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.SmartDateTimeUtil;

public class PertemuanPunyaUjianSiswaHelper implements DataLoader {

	private MyGrid grid;
	private Pertemuan pertemuan;

	private Siswa siswa = null;

	private CalonSiswa calonSiswa = null;

	public PertemuanPunyaUjianSiswaHelper(Siswa siswa, CalonSiswa calonSiswa) {
		this.siswa = siswa;
		this.calonSiswa = calonSiswa;
	}

	public MyToolbarbuttonConfig prosesUlangSoal(final Pertemuan pertemuan, String buttonLabel, String buttonImage) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Pilih Tanggal Ujian", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("95%");
				window.setWidth("600px");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("20%");
				column.setParent(columns);
				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();

				rows.setParent(grid);

				final Combobox fakultas;
				final Combobox jurusan;
				fakultas = new Combobox();
				jurusan = new Combobox();
				Common.initYayasanDanSekolahDanSemua(fakultas, jurusan, null, null);

				final MyDatebox start;
				final MyDatebox end;
				start = new MyDatebox();
				end = new MyDatebox();

				if (start != null) start.setReadonly(true);
				if (end != null) end.setReadonly(true);

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				if (start != null) start.setValue(calendar.getTime());
				calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 3);
				if (end != null) end.setValue(calendar.getTime());

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
				row.appendChild(fakultas);
				fakultas.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
				row.appendChild(jurusan);
				jurusan.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
				row.appendChild(start);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
				row.appendChild(end);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
				final AmbilDataDosenBanbox dosen;
				row.appendChild(dosen = new AmbilDataDosenBanbox());
				dosen.setWidth("90%");
				dosen.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final Checkbox hanya;
				row.appendChild(hanya = new Checkbox("Hanya ujian di pertemuan \"" + pertemuan.info() + "\""));
				hanya.setChecked(true);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						fakultas.setDisabled(hanya.isChecked());
						jurusan.setDisabled(hanya.isChecked());
						if (start != null) start.setDisabled(hanya.isChecked());
						if (end != null) end.setDisabled(hanya.isChecked());
						dosen.setDisabled(hanya.isChecked());
					}
				};

				hanya.addEventListener("onClick", eventListener);
				eventListener.onEvent(null);

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				// toolbar.setHeight("25px");
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Proses", "/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						final Yayasan fak = (Yayasan) (fakultas.getSelectedItem() == null ? null
								: fakultas.getSelectedItem().getValue());
						final Sekolah jur = (Sekolah) (jurusan.getSelectedItem() == null ? null
								: jurusan.getSelectedItem().getValue());

						final boolean hny = hanya.isChecked();

						final Dosen dsn = (Dosen) (hny ? null : dosen.getAttribute("dosen"));

						window.detach();

						final Label labelmy = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
						final Intbox intbox = new Intbox(10);
						final Intbox colS = new Intbox(10);
						Clients.showBusy(labelmy.getValue());

						final String filename = Sessions.getCurrent().getWebApp()
								.getRealPath("/tmp/cetak_data_" + URLEncoder.encode(
										Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
										+ ".xlsx");
						final File file;
						(file = new File(filename)).createNewFile();

						final Timer timer = new Timer(200);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						timer.setRepeats(true);
						timer.addEventListener("onTimer", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								try {

									Clients.showBusy(labelmy.getValue());
									System.out.println("label " + labelmy.getValue());

									if (labelmy.getValue().trim().equalsIgnoreCase("-")) {
										Clients.clearBusy();
										timer.detach();
									} else if (labelmy.getValue().isEmpty()) {

										Center center = new Center();
										final MyWindow window = new MyWindow("Cetak Data", "none", true);
										window.setParent(
												ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
										window.setHeight("97%");
										window.setWidth("90%");

										Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
										borderlayout.setParent(window);

										ais.ui.util.ZkCompat.setFlex(center, true);
										center.setParent(borderlayout);

										System.out.println("loading file " + file.getAbsolutePath());
										if (center != null) {
											Common.clear(center);
										}
										Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
										if (center != null) {
											Common.clear(center);
										}
										spreadsheet.setParent(center);
										spreadsheet.setWidth("100%");
										spreadsheet.setHeight("100%");
										spreadsheet.setSrc("../../tmp/" + file.getName());

										spreadsheet.setMaxrows(intbox.getValue() + 3);
										spreadsheet.setMaxcolumns(colS.getValue());
										ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

										South south = new South();
										south.setParent(borderlayout);

										Toolbar toolbar = new Toolbar();
										// toolbar.setHeight("25px");
										toolbar.setParent(south);
										MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup",
												"/img/cancel.gif");
										cancel.setTooltiptext("Tutup");
										cancel.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												window.detach();
											}
										});
										cancel.setParent(toolbar);

										MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
												"/img/excel.png");
										print.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {

												try {
													Filedownload.save(new FileInputStream(file),
															"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
															file.getName());
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PertemuanPunyaUjianSiswaHelper.java:364");

												}
											}
										});
										print.setParent(toolbar);

										window.setVisible(true);
										window.onModal();

										Clients.clearBusy();
										timer.detach();
									}

								} catch (Exception e) {
									Clients.clearBusy();
								}

							}
						});
						timer.start();

						try {

							Clients.showBusy(labelmy.getValue());

							new Thread(new Runnable() {

								@SuppressWarnings({ "unchecked" })
								@Override
								public void run() {
									try {

									try {
										XSSFWorkbook workbook = new XSSFWorkbook();
										XSSFSheet sheet = workbook.createSheet("DATA SOAL PESERTA");
										sheet.setDefaultColumnWidth(20);
										XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
										lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
										lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.RED));
										lockedNumericStyle.setLocked(true);

										Session session = HibernateUtil.currentNativeSession();
										List<PertemuanPunyaUjian> pertemuanPunyaUjians = hny
												? ConstantValues.simpleList(
														session.createCriteria(PertemuanPunyaUjian.class)
																.add(Restrictions.eq("pertemuan", pertemuan)),
														PertemuanPunyaUjian.class)
												: ConstantValues.simpleList(session
														.createCriteria(PertemuanPunyaUjian.class)

														.createAlias("ujian", "ujian")

														.add(fak == null ? Restrictions.sqlRestriction("true")
																: Restrictions.eq("ujian.fakultas", fak))
														.add(jur == null ? Restrictions.sqlRestriction("true")
																: Restrictions.eq("ujian.jurusan", jur))

														.add(Restrictions
																.sqlRestriction("date(this_.mulai_ujian) between date('"
																		+ Common.databaseDateFormat.get()
																				.format(start.getValue())
																		+ "') and date('" + Common.databaseDateFormat.get()
																				.format(end.getValue())
																		+ "')"))

														, PertemuanPunyaUjian.class);

										XSSFRow rowhead = sheet.createRow((short) 0);
										rowhead.createCell(0).setCellValue("NIM/NO REG");
										rowhead.createCell(1).setCellValue("NAMA");
										rowhead.createCell(2).setCellValue(Common.getBahasaConfig("FAKULTAS"));
										rowhead.createCell(3).setCellValue(Common.getBahasaConfig("JURUSAN"));
										rowhead.createCell(4).setCellValue("STATUS AWAL");
										rowhead.createCell(5).setCellValue("ANGKATAN");

										rowhead.createCell(6).setCellValue("UJIAN");
										rowhead.createCell(7).setCellValue("SOAL");
										rowhead.createCell(8).setCellValue("JAWABAN HURUF");
										rowhead.createCell(9).setCellValue("JAWABAN TEKS");
										rowhead.createCell(10).setCellValue("BETUL");

										int size = pertemuanPunyaUjians.size();
										int rowIndexMhs = 0;
										int rowIndex = 0;

										for (PertemuanPunyaUjian pertemuanPunyaUjian : pertemuanPunyaUjians) {
											Pertemuan pertemuan = pertemuanPunyaUjian.getPertemuan();
											if (pertemuan != null
													&& (dsn == null || (dsn.getId() != null && pertemuanPunyaUjian
															.getPertemuan().ambilDosenId().contains(dsn.getId())))) {

												List<Long> ujianPunyaSoalsTemp = pertemuanPunyaUjian.getUjian()
														.ambilUjianPunyaSoal(pertemuanPunyaUjian, true);

												System.out.println(
														"ujianPunyaSoalsTemp -> " + ujianPunyaSoalsTemp.size());

												rowIndexMhs++;

												labelmy.setValue("Sedang memproses data "
														+ pertemuanPunyaUjian.getUjian().getNama() + " ("
														+ Common.numberFormat.get().format(rowIndexMhs * 100.0 / size)
														+ " %)");

												if (pertemuan != null) {

													if (pertemuan.getJadwalUjianPMB() != null) {
														List<CalonSiswa> calonSiswas = ConstantValues
																.simpleList(
																		session.createCriteria(RuangPaketPMB.class)
																				.setProjection(Projections
																						.property("calonSiswa.id"))
																				.createAlias("ruangPMB", "ruangPMB")
																				.createAlias("calonSiswa", "calonSiswa")
																				.add(Restrictions.eq(
																						"ruangPMB.ujianPMB",
																						pertemuan.getJadwalUjianPMB()
																								.getUjianPMB()))
																				.add(pertemuan
																						.getJadwalUjianPMB()
																						.getPaket() == null
																								? Restrictions
																										.sqlRestriction(
																												"true")
																								: Restrictions.eq(
																										"calonSiswa.paket",
																										pertemuan
																												.getJadwalUjianPMB()
																												.getPaket()))
																				.addOrder(Common.bolehKonfigurasi("absensi_urut_berdasarkan_nim")
																								? Order.asc(
																										"calonSiswa.noRegistrasi")
																								: Order.asc(
																										"calonSiswa.nama")),
																		CalonSiswa.class, false);

														for (CalonSiswa calonSiswa : calonSiswas) {
															HasilUjianMahasiswa hasilUjianMahasiswa = HasilUjianMahasiswa
																	.ambilByKey(pertemuanPunyaUjian, null, null, siswa,
																			calonSiswa);

															labelmy.setValue("Sedang memproses data "
																	+ pertemuanPunyaUjian.getUjian().getNama() + "-"
																	+ calonSiswa.getNama() + " (" + Common.numberFormat.get()
																			.format(rowIndexMhs * 100.0 / size)
																	+ " %)");

															MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa
																	.ambilUjianPunyaSoals(
																			pertemuanPunyaUjian.getJmlDitampilkan(),
																			null, true);

															if (ujianPunyaSoals.isEmpty()) {
																ujianPunyaSoals = ProsesUjianHelper.randomPosisiton(
																		ujianPunyaSoalsTemp,
																		pertemuanPunyaUjian.getRandom(), null,
																		pertemuanPunyaUjian.getJmlDitampilkan());
															}

															ProsesUjianHelper.chekPosisitonJikaKurang(
																	ujianPunyaSoalsTemp, ujianPunyaSoals,
																	pertemuanPunyaUjian.getJmlDitampilkan());

															MyHashMap<Long, Set<Long>> hasilUjianMahasiswaDetailsa = new MyHashMap<Long, Set<Long>>(
																	pertemuanPunyaUjian.getJmlDitampilkan());

															if (hasilUjianMahasiswa != null) {
																hasilUjianMahasiswaDetailsa = hasilUjianMahasiswa
																		.ambilHasilUjianMahasiswaDetail(true,
																				pertemuanPunyaUjian.getJmlDitampilkan(),
																				null, ujianPunyaSoals);

															}

															for (Long ujianPunyaSoalid : ujianPunyaSoals) {

																UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
																		.ambilData(UjianPunyaSoal.class,
																				ujianPunyaSoalid.toString());
																if (ujianPunyaSoal != null) {
																	Set<Long> s = hasilUjianMahasiswaDetailsa
																			.get(ujianPunyaSoal.getBankSoal().getId());
																	HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail;
																	if (s == null || s.isEmpty()) {

																		try {
																			myHasilUjianMahasiswaDetail = new HasilUjianMahasiswaDetail();
																			myHasilUjianMahasiswaDetail.setBankSoal(
																					ujianPunyaSoal.getBankSoal());
																			myHasilUjianMahasiswaDetail
																					.setHasilUjianMahasiswa(
																							hasilUjianMahasiswa);
																			myHasilUjianMahasiswaDetail
																					.setUjianPunyaSoal(ujianPunyaSoal);
																			myHasilUjianMahasiswaDetail.setNilai(
																					ujianPunyaSoal.getBankSoal()
																							.getSkorDefault());

																			session.getTransaction().begin();
																			session.save(myHasilUjianMahasiswaDetail);
																			session.getTransaction().commit();

																			Set<Long> hasilUjianMahasiswaDetails = new HashSet<Long>();
																			hasilUjianMahasiswaDetails
																					.add(myHasilUjianMahasiswaDetail
																							.getId());
																			hasilUjianMahasiswaDetailsa.put(
																					myHasilUjianMahasiswaDetail
																							.getBankSoal().getId(),
																					hasilUjianMahasiswaDetails);
																			GeneralValueObject.masukkanData(
																					HasilUjianMahasiswaDetail.class,
																					myHasilUjianMahasiswaDetail);
																		} catch (Exception e) {
																			myHasilUjianMahasiswaDetail = null;
																			Common.tampilErrorJikaAdmin(e);
																		}

																	} else {
																		Long myHasilUjianMahasiswaDetailid = s
																				.iterator().next();
																		myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
																				.ambilData(
																						HasilUjianMahasiswaDetail.class,
																						myHasilUjianMahasiswaDetailid
																								.toString());
																	}
																	rowIndex++;
																	XSSFRow row = sheet.createRow(rowIndex);
																	XSSFCell cell = row.createCell(0);
																	cell.setCellValue(calonSiswa.getNoRegistrasi());

																	cell = row.createCell(1);
																	cell.setCellValue(calonSiswa.getNama());

																	Sekolah sekolah = calonSiswa.getSekolah();

																	cell = row.createCell(2);
																	cell.setCellValue(sekolah == null ? ""
																			: sekolah.getYayasan().getNama());

																	cell = row.createCell(3);
																	cell.setCellValue(
																			sekolah == null ? "" : sekolah.getNama());

																	cell = row.createCell(4);
																	cell.setCellValue(calonSiswa.getStatusSiswa());

																	cell = row.createCell(5);
																	cell.setCellValue(calonSiswa.getTahunMasuk());

																	cell = row.createCell(6);
																	cell.setCellValue(
																			pertemuanPunyaUjian.getUjian().getNama());

																	String soal = ujianPunyaSoal.getBankSoal()
																			.getSoal();
																	try {
																		soal = Jsoup.parse(soal).text();
																	} catch (Exception e) {
																		e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PertemuanPunyaUjianSiswaHelper.java:625");
																	}

																	cell = row.createCell(7);
																	cell.setCellValue(soal);

																	cell = row.createCell(8);
																	cell.setCellValue(
																			myHasilUjianMahasiswaDetail == null
																					|| myHasilUjianMahasiswaDetail
																							.getBankSoalDetail() == null
																									? ""
																									: (myHasilUjianMahasiswaDetail
																											.getUjianPunyaSoal()
																											.getUjian()
																											.getTampilanHurufDiPilihanJawaban()
																													? myHasilUjianMahasiswaDetail
																															.getBankSoalDetail()
																															.getHuruf()
																															+ ""
																													: ""));

																	cell = row.createCell(9);
																	cell.setCellValue(
																			myHasilUjianMahasiswaDetail == null
																					|| myHasilUjianMahasiswaDetail
																							.getBankSoalDetail() == null
																									? (myHasilUjianMahasiswaDetail != null
																											? myHasilUjianMahasiswaDetail
																													.getJawaban()
																											: "")
																									: myHasilUjianMahasiswaDetail
																											.getBankSoalDetail()
																											.getJawaban());

																	cell = row.createCell(10);
																	cell.setCellValue(
																			myHasilUjianMahasiswaDetail == null
																					|| myHasilUjianMahasiswaDetail
																							.getBankSoalDetail() == null
																									? ""
																									: myHasilUjianMahasiswaDetail
																											.getBankSoalDetail()
																											.getBetul()
																											.toString());
																}
															}
														}

													} else {

														List<Siswa> siswas = pertemuan.ambilSiswa();

														for (Siswa siswa : siswas) {
															HasilUjianMahasiswa hasilUjianMahasiswa = HasilUjianMahasiswa
																	.ambilByKey(pertemuanPunyaUjian, null, null, siswa,
																			calonSiswa);

															labelmy.setValue("Sedang memproses data "
																	+ pertemuanPunyaUjian.getUjian().getNama() + " "
																	+ siswa.getNama() + " (" + Common.numberFormat.get()
																			.format(rowIndexMhs * 100.0 / size)
																	+ " %)");

															MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa
																	.ambilUjianPunyaSoals(
																			pertemuanPunyaUjian.getJmlDitampilkan(),
																			null, true);

															if (ujianPunyaSoals.isEmpty()) {
																ujianPunyaSoals = ProsesUjianHelper.randomPosisiton(
																		ujianPunyaSoalsTemp,
																		pertemuanPunyaUjian.getRandom(), null,
																		pertemuanPunyaUjian.getJmlDitampilkan());
															}

															ProsesUjianHelper.chekPosisitonJikaKurang(
																	ujianPunyaSoalsTemp, ujianPunyaSoals,
																	pertemuanPunyaUjian.getJmlDitampilkan());

															MyHashMap<Long, Set<Long>> hasilUjianMahasiswaDetailsa = new MyHashMap<Long, Set<Long>>(
																	pertemuanPunyaUjian.getJmlDitampilkan());

															if (hasilUjianMahasiswa != null) {
																hasilUjianMahasiswaDetailsa = hasilUjianMahasiswa
																		.ambilHasilUjianMahasiswaDetail(true,
																				pertemuanPunyaUjian.getJmlDitampilkan(),
																				null, ujianPunyaSoals);

															}

															for (Long ujianPunyaSoalid : ujianPunyaSoals) {

																UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
																		.ambilData(UjianPunyaSoal.class,
																				ujianPunyaSoalid.toString());
																if (ujianPunyaSoal != null) {
																	Set<Long> s = hasilUjianMahasiswaDetailsa
																			.get(ujianPunyaSoal.getBankSoal().getId());
																	HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail;
																	if (s == null || s.isEmpty()) {

																		try {
																			myHasilUjianMahasiswaDetail = new HasilUjianMahasiswaDetail();
																			myHasilUjianMahasiswaDetail.setBankSoal(
																					ujianPunyaSoal.getBankSoal());
																			myHasilUjianMahasiswaDetail
																					.setHasilUjianMahasiswa(
																							hasilUjianMahasiswa);
																			myHasilUjianMahasiswaDetail
																					.setUjianPunyaSoal(ujianPunyaSoal);
																			myHasilUjianMahasiswaDetail.setNilai(
																					ujianPunyaSoal.getBankSoal()
																							.getSkorDefault());

																			session.getTransaction().begin();
																			session.save(myHasilUjianMahasiswaDetail);
																			session.getTransaction().commit();

																			Set<Long> hasilUjianMahasiswaDetails = new HashSet<Long>();
																			hasilUjianMahasiswaDetails
																					.add(myHasilUjianMahasiswaDetail
																							.getId());
																			hasilUjianMahasiswaDetailsa.put(
																					myHasilUjianMahasiswaDetail
																							.getBankSoal().getId(),
																					hasilUjianMahasiswaDetails);
																			GeneralValueObject.masukkanData(
																					HasilUjianMahasiswaDetail.class,
																					myHasilUjianMahasiswaDetail);
																		} catch (Exception e) {
																			myHasilUjianMahasiswaDetail = null;
																			Common.tampilErrorJikaAdmin(e);
																		}

																	} else {
																		Long myHasilUjianMahasiswaDetailid = s
																				.iterator().next();
																		myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
																				.ambilData(
																						HasilUjianMahasiswaDetail.class,
																						myHasilUjianMahasiswaDetailid
																								.toString());
																	}
																	rowIndex++;
																	XSSFRow row = sheet.createRow(rowIndex);
																	XSSFCell cell = row.createCell(0);
																	cell.setCellValue(siswa.getNim());

																	cell = row.createCell(1);
																	cell.setCellValue(siswa.getNama());

																	cell = row.createCell(2);
																	cell.setCellValue(
																			siswa.getSekolah().getYayasan().getNama());

																	cell = row.createCell(3);
																	cell.setCellValue(siswa.getSekolah().getNama());

																	cell = row.createCell(4);
																	cell.setCellValue(siswa.getStatusAwalSiswa() == null
																			? ""
																			: siswa.getStatusAwalSiswa().getNama());

																	cell = row.createCell(5);
																	cell.setCellValue(siswa.getTahunMasuk());

																	cell = row.createCell(6);
																	cell.setCellValue(
																			pertemuanPunyaUjian.getUjian().getNama());

																	String soal = ujianPunyaSoal.getBankSoal()
																			.getSoal();
																	try {
																		soal = Jsoup.parse(soal).text();
																	} catch (Exception e) {
																		e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PertemuanPunyaUjianSiswaHelper.java:801");
																	}

																	cell = row.createCell(7);
																	cell.setCellValue(soal);

																	cell = row.createCell(8);
																	cell.setCellValue(
																			myHasilUjianMahasiswaDetail == null
																					|| myHasilUjianMahasiswaDetail
																							.getBankSoalDetail() == null
																									? ""
																									: myHasilUjianMahasiswaDetail
																											.getBankSoalDetail()
																											.getHuruf());

																	cell = row.createCell(9);
																	cell.setCellValue(
																			myHasilUjianMahasiswaDetail == null
																					|| myHasilUjianMahasiswaDetail
																							.getBankSoalDetail() == null
																									? (myHasilUjianMahasiswaDetail != null
																											? myHasilUjianMahasiswaDetail
																													.getJawaban()
																											: "")
																									: myHasilUjianMahasiswaDetail
																											.getBankSoalDetail()
																											.getJawaban());

																	cell = row.createCell(10);
																	cell.setCellValue(
																			myHasilUjianMahasiswaDetail == null
																					|| myHasilUjianMahasiswaDetail
																							.getBankSoalDetail() == null
																									? ""
																									: myHasilUjianMahasiswaDetail
																											.getBankSoalDetail()
																											.getBetul()
																											.toString());
																}
															}
														}
													}
												}

											}
										}

										intbox.setValue(rowIndex + 1);

										try {
											FileOutputStream fileOut = new FileOutputStream(filename);
											workbook.write(fileOut);
											fileOut.close();
										} catch (IOException e) {
											Common.tampilErrorJikaAdmin(e);
										}
										System.out.println("Your excel file has been generated! ");

										labelmy.setValue("");
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										labelmy.setValue("-");
									}
									HibernateUtil.closeSession();
																	} finally {
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}).start();

						} catch (Exception e) {
							// TODO Auto-generated catch block
							Common.tampilErrorJikaAdmin(e);
						}

					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});

		return toolbarbutton;
	}

	public static Label tampilBolekIkutUjianAtauTidak(Row arg0, final PertemuanPunyaUjian pertemuanPunyaUjian,
			final Siswa siswa, final CalonSiswa calonSiswa, final HasilUjianMahasiswa hasilUjianMahasiswa,
			final EventListener eventListener) {
		Label label = null;
		Pertemuan pertemuan = pertemuanPunyaUjian.getPertemuan();
//		if (pertemuan.getJadwalUjianPMB() != null && pertemuanPunyaUjian.getSekolah() != null && calonSiswa != null
//				&& !calonSiswa.populatePilihanSekolahIds().contains(pertemuanPunyaUjian.getSekolah().getId())) {
//			arg0.appendChild(
//					label = new MyLabelKecil("Anda tidak bisa mengikuti ujian ini, karena ujian ini hanya untuk "
//							+ Common.getBahasaConfig("jurusan") + " " + pertemuanPunyaUjian.getSekolah().getNama()));
//		} else if (pertemuan.getJadwalUjianPMB() != null && pertemuanPunyaUjian.getYayasan() != null
//				&& calonSiswa != null
//				&& !calonSiswa.populatePilihanYayasanIds().contains(pertemuanPunyaUjian.getYayasan().getId())) {
//			arg0.appendChild(
//					label = new MyLabelKecil("Anda tidak bisa mengikuti ujian ini, karena ujian ini hanya untuk "
//							+ Common.getBahasaConfig("fakultas") + " " + pertemuanPunyaUjian.getYayasan().getNama()));
//		} else

		if (pertemuan.getJadwalUjianPMB() != null && pertemuanPunyaUjian.getSekolah() != null && siswa != null
				&& siswa.getSekolah() != null
				&& !siswa.getSekolah().getId().equals(pertemuanPunyaUjian.getSekolah().getId())) {
			arg0.appendChild(
					label = new MyLabelKecil("Anda tidak bisa mengikuti ujian ini, karena ujian ini hanya untuk "
							+ Common.getBahasaConfig("jurusan") + " " + pertemuanPunyaUjian.getSekolah().getNama()));
		} else if (pertemuan.getJadwalUjianPMB() != null && pertemuanPunyaUjian.getYayasan() != null && siswa != null
				&& siswa.getSekolah() != null && siswa.getSekolah().getYayasan() != null
				&& !siswa.getSekolah().getYayasan().getId().equals(pertemuanPunyaUjian.getYayasan().getId())) {
			arg0.appendChild(
					label = new MyLabelKecil("Anda tidak bisa mengikuti ujian ini, karena ujian ini hanya untuk "
							+ Common.getBahasaConfig("fakultas") + " " + pertemuanPunyaUjian.getYayasan().getNama()));
		} else {

			if ((pertemuanPunyaUjian.getMulaiUjian() == null
					|| pertemuanPunyaUjian.getMulaiUjian().before(ais.ui.util.WaktuUtil.getDate()))
					&& (pertemuanPunyaUjian.getSampaiUjian() == null
							|| pertemuanPunyaUjian.getSampaiUjian().after(ais.ui.util.WaktuUtil.getDate()))
					|| (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getLengkapiJawaban())) {

				final boolean masihBolehIkut = (hasilUjianMahasiswa == null
						|| hasilUjianMahasiswa.getJumlahIkut() < pertemuanPunyaUjian.getJumlahBolehIkut())
						|| (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getLengkapiJawaban());

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(!masihBolehIkut ? "Lihat Hasil" : "Ikut Ujian",
						!masihBolehIkut ? "/img/eye-icon.png" : "/img/stock_data_edit_table.png");
				button.setOrient("vertical");
				Tbmuser tbmuser = Common.getCurrentUser();
				button.setVisible(siswa != null || calonSiswa != null || tbmuser.getSiswa() != null
						|| tbmuser.getCalonSiswa() != null);
				button.setTooltiptext(!masihBolehIkut ? "Lihat Hasil" : "Ikut Ujian");

				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						eventListener.onEvent(event);
						Tbmuser tbmuser = Common.getCurrentUser();
						if (!masihBolehIkut) {
							ProsesUjianHelper.tampil(null, null, tbmuser.getSiswa(), tbmuser.getCalonSiswa(),
									pertemuanPunyaUjian, true, eventListener, true);
							return;
						}

						ProsesUjianHelper.ikut(null, null, tbmuser.getSiswa(), tbmuser.getCalonSiswa(),
								pertemuanPunyaUjian, hasilUjianMahasiswa, true, eventListener);

					}
				});

				if ((siswa != null || calonSiswa != null || tbmuser.getSiswa() != null
						|| tbmuser.getCalonSiswa() != null)) {
					if (button.getLabel().equalsIgnoreCase("Lihat Hasil")
							&& !(pertemuanPunyaUjian.getLihatJawabanSetelahUjian()
									|| pertemuanPunyaUjian.getLihatNilaiSetelahUjian())) {
						button.setVisible(false);
					}
				}

				if (!masihBolehIkut && !(pertemuanPunyaUjian.getLihatJawabanSetelahUjian()
						|| pertemuanPunyaUjian.getLihatNilaiSetelahUjian())) {
					button.setVisible(false);
				}

				Hbox toolbar = new Hbox();
				button.setParent(toolbar);
				toolbar.setParent(arg0);

			} else {

				if (pertemuanPunyaUjian.getMulaiUjian() != null
						&& pertemuanPunyaUjian.getMulaiUjian().after(ais.ui.util.WaktuUtil.getDate())) {
					arg0.appendChild(label = new MyLabelKecil("Ujian belum mulai, ujian akan dimulai "
							+ SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getMulaiUjian(), null) + " "
							+ Common.dateFormat5.get().format(pertemuanPunyaUjian.getMulaiUjian())));
				} else if (pertemuanPunyaUjian.getSampaiUjian() != null
						&& pertemuanPunyaUjian.getSampaiUjian().before(ais.ui.util.WaktuUtil.getDate())) {
					arg0.appendChild(label = new MyLabelKecil("Ujian telah terlewat, ujian telah berakhir "
							+ SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getSampaiUjian(), null) + " "
							+ Common.dateFormat5.get().format(pertemuanPunyaUjian.getSampaiUjian())));
				} else {
					arg0.appendChild(label = new MyLabelKecil("Ujian telah terlewat atau belum mulai"));
				}
				label.setStyle("font-size:11px;color:red;");
			}
		}

		return label;
	}

	class DetailPertemuanRenderer extends ais.ui.util.MyRowRenderer {

		private DetailUjianHelper detailUjianHelper = new DetailUjianHelper();

		@SuppressWarnings({ "deprecation", "unchecked" })
		@Override
		public void render(final Row arg0, Object data) throws Exception {
			Session session = HibernateUtil.currentSession();
			Tbmuser tbmuser = Common.getCurrentUser();
			final PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) data;
			if (pertemuan != null) {
				pertemuan.masukkanData("ujian_" + pertemuanPunyaUjian.getId());
			}
			final HasilUjianMahasiswa hasilUjianMahasiswa = HasilUjianMahasiswa.ambilByKey(pertemuanPunyaUjian, null,
					null, siswa, calonSiswa);
			if (pertemuanPunyaUjian.getUjian() == null && pertemuanPunyaUjian.getId() != null) {
				HibernateUtil.currentSession().refresh(pertemuanPunyaUjian);
				ProsesUjianHelper.kuotaUjian.remove(hasilUjianMahasiswa.getKeyhasil());
			}

			final Ujian ujian = pertemuanPunyaUjian.getUjian();
			HasilUjianHelper.reinitUjian(ujian, pertemuan);

			if (siswa == null && calonSiswa == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null) {
				final MyDetail detail = new MyDetail();
				detail.setParent(arg0);

				detail.addEventListener("onOpen", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (detail != null) {
							Common.clear(detail);
						}
						if (detail.isOpen()) {

							boolean tampilMenuSoalDiManajemenUjian = Common.bolehKonfigurasi("tampil_menu_soal_di_manajemen_ujian");
							detailUjianHelper.display(ujian, detail, pertemuan, pertemuanPunyaUjian,
									tampilMenuSoalDiManajemenUjian, false);
						}

					}
				});
			} else {
				new Label().setParent(arg0);
			}

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			RevisiHelper.createNewRevisi(PertemuanPunyaUjian.class, pertemuanPunyaUjian, ujian.getNama())
					.setParent(vbox);

			Number tg = pertemuanPunyaUjian.ambilJumlahHasilUjianMahasiswaTelahIkut(false);
			MyLabelKecil labelKecil = new MyLabelKecil(
					"Ikut Ujian : " + Common.numberFormat.get().format(tg.intValue()) + " peserta");
			labelKecil.setStyle("font-size:8px;color:blue;");
			labelKecil.setParent(vbox);

			if (siswa == null && calonSiswa == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null) {

				final MyCheckboxConfig otomatisMunculKetikaBelumSelesai = new MyCheckboxConfig(
						"Apabila peserta belum selesai ujian dan tiba-tiba terputus koneksi / baterai ponselnya habis / browser-nya crash dan bermasalah dll, saat login ulang, secara otomatis tampilan ujian akan muncul dengan melanjutkan waktu terakhir berhenti.");
				otomatisMunculKetikaBelumSelesai.setParent(vbox);
				otomatisMunculKetikaBelumSelesai.setChecked(pertemuanPunyaUjian.getOtomatisMunculKetikaBelumSelesai());
				otomatisMunculKetikaBelumSelesai.setDisabled(siswa != null);
				otomatisMunculKetikaBelumSelesai.setStyle("font-size:8px");
				otomatisMunculKetikaBelumSelesai.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pertemuanPunyaUjian
								.setOtomatisMunculKetikaBelumSelesai(otomatisMunculKetikaBelumSelesai.isChecked());
						Common.refreshUpdate(session, pertemuanPunyaUjian);
					}
				});

				final MyCheckboxConfig tidakDiaktifkanTombolKembali = new MyCheckboxConfig(
						"Peserta tidak boleh melihat atau kembali ke soal sebelumnya. Misal : peserta sudah berada di soal nomor 5, tidak bisa kembali lagi ke soal nomor 3.");
				tidakDiaktifkanTombolKembali.setParent(vbox);
				tidakDiaktifkanTombolKembali.setChecked(pertemuanPunyaUjian.getTidakDiaktifkanTombolKembali());
				tidakDiaktifkanTombolKembali.setDisabled(siswa != null);
				tidakDiaktifkanTombolKembali.setStyle("font-size:8px");
				tidakDiaktifkanTombolKembali.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pertemuanPunyaUjian.setTidakDiaktifkanTombolKembali(tidakDiaktifkanTombolKembali.isChecked());
						Common.refreshUpdate(session, pertemuanPunyaUjian);
					}
				});

			}

			if (calonSiswa != null || siswa != null) {
				if (pertemuanPunyaUjian.getYayasan() != null) {
					new MyLabelAgakKecil(
							Common.getBahasaConfig("Yayasan") + " : " + pertemuanPunyaUjian.getYayasan().getNama())
							.setParent(vbox);
				}
				if (pertemuanPunyaUjian.getSekolah() != null) {
					new MyLabelAgakKecil(
							Common.getBahasaConfig("Sekolah") + " : " + pertemuanPunyaUjian.getSekolah().getNama())
							.setParent(vbox);
				}

				if (hasilUjianMahasiswa != null && !hasilUjianMahasiswa.getKeterangan().isEmpty()) {
					new MyLabelAgakKecil("Keterangan : " + hasilUjianMahasiswa.getKeterangan()).setParent(vbox);
				}

			} else if (pertemuan.getJadwalUjianPMB() != null) {

				final Combobox fak = new Combobox();
				final Combobox jur = new Combobox();

				Common.initYayasanDanSekolahDanSemua(fak, jur, null, null);

				fak.setParent(vbox);
				jur.setParent(vbox);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pertemuanPunyaUjian.setYayasan(
								(Yayasan) (fak.getSelectedItem() == null ? null : fak.getSelectedItem().getValue()));
						pertemuanPunyaUjian.setSekolah(
								(Sekolah) (jur.getSelectedItem() == null ? null : jur.getSelectedItem().getValue()));
						Common.refreshUpdate(pertemuanPunyaUjian);
					}
				};

				fak.addEventListener("onChange", eventListener);
				jur.addEventListener("onChange", eventListener);

				Common.selectComboItem(fak, pertemuanPunyaUjian.getYayasan());
				Common.selectComboItem(jur, pertemuanPunyaUjian.getSekolah());

				if (ujian.getYayasan() != null) {
					fak.setDisabled(true);
				}
				if (ujian.getSekolah() != null) {
					jur.setDisabled(true);
				}
			}

			RevisiHelper.createNewRevisi(Ujian.class, ujian,
					Common.getBahasaConfig(ujian.getJenis()) + " / " + Common.getBahasaConfig(ujian.getJenisKoreksi())
							+ " / " + Common.getBahasaConfig(ujian.getLevel()) + " / "
							+ Common.numberFormat.get().format(ujian.getNilaiLulus()))
					.setParent(arg0);

			if (pertemuanPunyaUjian.getLihatNilaiSetelahUjian()) {
				Hbox hbox = new Hbox();
				hbox.setParent(arg0);
				new Label(hasilUjianMahasiswa == null ? ""
						: Common.numberFormat.get().format(hasilUjianMahasiswa.getJawabanBenar())).setParent(hbox);
				new Label(hasilUjianMahasiswa == null ? "Belum pernah ikut"
						: Common.numberFormat.get().format(hasilUjianMahasiswa.getJumlahIkut()) + " kali").setParent(hbox);

				hbox = new Hbox();
				hbox.setParent(arg0);
				new Label((hasilUjianMahasiswa == null || hasilUjianMahasiswa.getNilai() == null ? ""
						: Common.numberFormat.get().format(hasilUjianMahasiswa.getNilai()))
						+ (ujian.getJenis().equalsIgnoreCase(BankSoal.ESAY) ? ""
								: " / " + (hasilUjianMahasiswa == null || hasilUjianMahasiswa.getNilai() == null ? ""
										: (hasilUjianMahasiswa.getLulus() ? Common.getBahasaConfig("Lulus")
												: Common.getBahasaConfig("Tidak Lulus")))))
						.setParent(hbox);
				new Label(hasilUjianMahasiswa == null ? ""
						: Common.numberFormat.get().format(pertemuanPunyaUjian.getJumlahBolehIkut()) + " kali")
						.setParent(hbox);
			} else {
				new Label(hasilUjianMahasiswa == null ? "Belum pernah ikut"
						: Common.numberFormat.get().format(hasilUjianMahasiswa.getJumlahIkut()) + " kali").setParent(arg0);
				new Label(hasilUjianMahasiswa == null ? ""
						: Common.numberFormat.get().format(pertemuanPunyaUjian.getJumlahBolehIkut()) + " kali")
						.setParent(arg0);
			}

			if (pertemuanPunyaUjian.getJmlDitampilkan() == null || pertemuanPunyaUjian.getJmlDitampilkan() <= 0) {
				session = HibernateUtil.currentSession();
				List<Long> d = pertemuanPunyaUjian.getUjian().ambilUjianPunyaSoal(pertemuanPunyaUjian, false);
				int jmlDitampilkan = d.size();
				d = null;
				if (jmlDitampilkan > 0) {
					pertemuanPunyaUjian.setJmlDitampilkan(jmlDitampilkan);
					Common.refreshUpdate(session, (pertemuanPunyaUjian));
				}
			}

			if (siswa != null || calonSiswa != null || tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null) {

				try {
					new Label((pertemuanPunyaUjian.getJmlDitampilkan() == null ? ""
							: Common.numberFormat.get().format(pertemuanPunyaUjian.getJmlDitampilkan()))
							+ (hasilUjianMahasiswa == null || hasilUjianMahasiswa.getJumlahSoal() == null ? ""
									: " / " + Common.numberFormat.get().format(hasilUjianMahasiswa.getJumlahSoal())))
							.setParent(arg0);

					new Label(pertemuanPunyaUjian.getDibatasiWaktu() != null && pertemuanPunyaUjian.getDibatasiWaktu()
							? "Ya"
							: "Tidak").setParent(arg0);

					new Label(pertemuanPunyaUjian.getDibatasiWaktu() == null || !pertemuanPunyaUjian.getDibatasiWaktu()
							|| pertemuanPunyaUjian.getLama() == null ? ""
									: Common.timeFormat1.get().format(pertemuanPunyaUjian.getLama()))
							.setParent(arg0);

					new Label(pertemuanPunyaUjian.getDibatasiWaktu() == null || !pertemuanPunyaUjian.getDibatasiWaktu()
							|| pertemuanPunyaUjian.getMulaiUjian() == null
									? ""
									: (SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getMulaiUjian(), null)
											+ (Common.dateFormat3.get().format(pertemuanPunyaUjian.getMulaiUjian())
													.endsWith("00:00:00")
															? Common.dateFormat1.get()
																	.format(pertemuanPunyaUjian.getMulaiUjian())
															: Common.dateFormat3.get()
																	.format(pertemuanPunyaUjian.getMulaiUjian()))))
							.setParent(arg0);

					new Label(pertemuanPunyaUjian.getDibatasiWaktu() == null || !pertemuanPunyaUjian.getDibatasiWaktu()
							|| pertemuanPunyaUjian.getSampaiUjian() == null
									? ""
									: (SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getSampaiUjian(), null)
											+ (Common.dateFormat3.get().format(pertemuanPunyaUjian.getSampaiUjian())
													.endsWith("00:00:00")
															? Common.dateFormat1.get()
																	.format(pertemuanPunyaUjian.getSampaiUjian())
															: Common.dateFormat3.get()
																	.format(pertemuanPunyaUjian.getSampaiUjian()))))
							.setParent(arg0);

					new Label(pertemuanPunyaUjian.getFormatNilai() == null
							|| pertemuanPunyaUjian.getFormatNilai().getStatusPertemuan() == null
									? ""
									: pertemuanPunyaUjian.getFormatNilai().getNama() + " ("
											+ Common.numberFormat.get().format(pertemuanPunyaUjian.getProsentase()) + "%)")
							.setParent(arg0);

					new Label(ujian.getAktif() ? "Ya" : "Tidak").setParent(arg0);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadData(null);
					}
				};

				PertemuanPunyaUjianSiswaHelper.tampilBolekIkutUjianAtauTidak(arg0, pertemuanPunyaUjian, siswa,
						calonSiswa, hasilUjianMahasiswa, eventListener);

			} else {

				final MyLabelAgakKecil agakKecil = new MyLabelAgakKecil();
				agakKecil.setStyle("font-size:9px;color:red");
				vbox = new Vbox();
				vbox.setParent(arg0);
				final Intbox jml = new Intbox(pertemuanPunyaUjian.getJmlDitampilkan());
				vbox.appendChild(new Hbox(
						new Component[] { new MyLabelAgakKecil("Ditampilkan:"), jml, new MyLabelAgakKecil("soal") }));

				jml.setCols(1);
				jml.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						List<Long> bankSoals = pertemuanPunyaUjian.getUjian().ambilBankSoal(pertemuanPunyaUjian, false);
						int jumlah = bankSoals.size();
						bankSoals = null;

						System.out.println("jumlah soal => " + jumlah + ", input => " + jml.getValue());

						if (jumlah == 0) {
							MyMessageboxConfig.show(
									"Soal ujian harus diinput terlebih dahulu sebelum jumlah soal yang diujikan dapat ditentukan. Langkah yang dapat dilakukan: (1) klik tombol detail atau tanda plus di sebelah kiri; (2) buat dan simpan soal ujian terlebih dahulu; (3) tentukan kembali jumlah soal yang akan diujikan.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							jml.setValue(0);
							return;
						}

						if (jml.getValue() != null && jml.getValue() > jumlah) {
							MyMessageboxConfig.showFormat(
									"Jumlah soal ujian maksimal yang bisa diujikan adalah {V1}. Langkah yang dapat dilakukan: (1) periksa kembali jumlah soal yang tersedia; (2) masukkan jumlah soal yang tidak melebihi jumlah tersebut; (3) simpan kembali pengaturan jumlah soal.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, jumlah);
							jml.setValue((jumlah));
							pertemuanPunyaUjian.setJmlDitampilkan(jml.getValue());
							Common.refreshUpdate(session, (pertemuanPunyaUjian));

							return;
						}

						pertemuanPunyaUjian.setJmlDitampilkan(jml.getValue());
						Common.refreshUpdate(session, (pertemuanPunyaUjian));

						if (pertemuanPunyaUjian.getJmlDitampilkan() < 1) {
							agakKecil.setValue("Jml soal tidak boleh 0");
						} else {
							agakKecil.setValue("");
						}
					}
				});

				vbox.appendChild(agakKecil);
				if (pertemuanPunyaUjian.getJmlDitampilkan() < 1) {
					agakKecil.setValue("Jumlah soal tidak boleh 0");
					MyButtonConfig samakan;
					vbox.appendChild(samakan = new MyButtonConfig("Samakan dg jml soal tersedia"));
					samakan.setStyle("font-size:9px;");
					samakan.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Session session = HibernateUtil.currentSession();
							List<Long> bankSoals = pertemuanPunyaUjian.getUjian().ambilBankSoal(pertemuanPunyaUjian,
									false);
							int jumlah = bankSoals.size();
							bankSoals = null;
							if (jumlah == 0) {
								MyMessageboxConfig.show(
										"Soal ujian harus diinput terlebih dahulu sebelum jumlah soal yang diujikan dapat ditentukan. Langkah yang dapat dilakukan: (1) klik tombol detail atau tanda plus di sebelah kiri; (2) buat dan simpan soal ujian terlebih dahulu; (3) tentukan kembali jumlah soal yang akan diujikan.",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								jml.setValue(0);
								return;
							}
							jml.setValue((jumlah));
							pertemuanPunyaUjian.setJmlDitampilkan(jml.getValue());
							Common.refreshUpdate(session, (pertemuanPunyaUjian));
							if (pertemuanPunyaUjian.getJmlDitampilkan() < 1) {
								agakKecil.setValue("Jml soal tidak boleh 0");
							} else {
								agakKecil.setValue("");
								arg0.getTarget().setVisible(false);
							}
						}
					});
				}

				// else if (count > 0) {
				// agakKecil.setValue("Jumlah soal yg ditampilkan "
				// +
				// Common.numberFormat.get().format(pertemuanPunyaUjian.getJmlDitampilkan())
				// + " dan tidak bisa diubah ketika peserta telah melakukan
				// ujian");
				// }

				else {
					agakKecil.setValue("");
				}

				final Intbox jumlahBolehIkut = new Intbox(pertemuanPunyaUjian.getJumlahBolehIkut());
				vbox.appendChild(new Hbox(new Component[] { new MyLabelAgakKecil("Boleh ikut ujian sebanyak :"),
						jumlahBolehIkut, new MyLabelAgakKecil("kali") }));
				jumlahBolehIkut.setCols(1);
				jumlahBolehIkut.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();

						if (jumlahBolehIkut.getValue() < 1) {
							MyMessageboxConfig.show(
									"Jumlah minimal boleh ikut ujian adalah sebanyak 1 kali. Langkah yang dapat dilakukan: (1) periksa kembali nilai yang Anda masukkan; (2) masukkan angka minimal 1; (3) simpan kembali pengaturan jumlah boleh ikut ujian.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							jumlahBolehIkut.setValue((1));
							pertemuanPunyaUjian.setJumlahBolehIkut(jumlahBolehIkut.getValue());
							Common.refreshUpdate(session, (pertemuanPunyaUjian));
							return;
						}

						pertemuanPunyaUjian.setJumlahBolehIkut(jumlahBolehIkut.getValue());
						Common.refreshUpdate(session, (pertemuanPunyaUjian));

					}
				});

				vbox = new Vbox();
				vbox.setParent(arg0);

				final MyCheckboxConfig dibatasiWaktu = new MyCheckboxConfig("Ujian ini dibatasi waktu");
				dibatasiWaktu.setStyle("font-size:9px;");
				dibatasiWaktu.setParent(vbox);
				dibatasiWaktu.setChecked(pertemuanPunyaUjian.getDibatasiWaktu());
				dibatasiWaktu.setDisabled(siswa != null);

				dibatasiWaktu.setVisible(Common.bolehKonfigurasi("tampilkan_ujian_dibatasi_waktu"));
				if (!dibatasiWaktu.isVisible()) {
					pertemuanPunyaUjian.setDibatasiWaktu(true);
				}

				final MyCheckboxConfig lihatJawabanSetelahUjian = new MyCheckboxConfig(
						"Peserta bisa melihat jawaban setelah ujian");
				lihatJawabanSetelahUjian.setStyle("font-size:9px;");
				lihatJawabanSetelahUjian.setParent(vbox);
				lihatJawabanSetelahUjian.setChecked(pertemuanPunyaUjian.getLihatJawabanSetelahUjian());
				lihatJawabanSetelahUjian.setDisabled(siswa != null);

				final MyCheckboxConfig lihatNilaiSetelahUjian = new MyCheckboxConfig(
						"Peserta bisa melihat nilai setelah ujian");
				lihatNilaiSetelahUjian.setStyle("font-size:9px;");
				lihatNilaiSetelahUjian.setParent(vbox);
				lihatNilaiSetelahUjian.setChecked(pertemuanPunyaUjian.getLihatNilaiSetelahUjian());
				lihatNilaiSetelahUjian.setDisabled(siswa != null);

				final MyCheckboxConfig random = new MyCheckboxConfig("Random / Urutan nomor soal diacak");
				random.setStyle("font-size:9px;");
				random.setParent(vbox);
				random.setChecked(pertemuanPunyaUjian.getRandom());
				random.setDisabled(siswa != null);

				vbox = new Vbox();
				vbox.setParent(arg0);
				final MyTimebox lama = new MyTimebox(pertemuanPunyaUjian.getLama());
				lama.setFormat(Common.timeFormat1.get().toPattern());
				lama.setParent(vbox);
				// lama.setWidth("90%");
				lama.setDisabled(siswa != null);
				lama.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pertemuanPunyaUjian.setLama(lama.getValue());
						Common.refreshUpdate(session, (pertemuanPunyaUjian));

					}
				});

				final MyCheckboxConfig tiapSoal = new MyCheckboxConfig(
						"Waktu berlaku untuk setiap soal, apabila opsi ini tidak dipilih, maka waktu berlaku untuk seluruh soal");
				tiapSoal.setStyle("font-size:9px");
				tiapSoal.setParent(vbox);
				tiapSoal.setChecked(pertemuanPunyaUjian.getTiapSoal());
				tiapSoal.setDisabled(siswa != null);

				tiapSoal.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pertemuanPunyaUjian.setTiapSoal(tiapSoal.isChecked());
						Common.refreshUpdate(session, pertemuanPunyaUjian);
					}
				});

				// new Label(ujian.getMulaiUjian() == null ? ""
				// : Common.dateFormat3.get().format(ujian.getMulaiUjian()))
				// .setParent(arg0);
				final MyDatebox mulaiUjian = new MyDatebox(pertemuanPunyaUjian.getMulaiUjian());
				mulaiUjian.setFormat(Common.dateFormat.get().toPattern());

				mulaiUjian.setParent(arg0);
				mulaiUjian.setWidth("90%");
				mulaiUjian.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pertemuanPunyaUjian.setMulaiUjian(mulaiUjian.getValue());
						Common.refreshUpdate(session, (pertemuanPunyaUjian));

					}
				});

				final MyDatebox sampaiUjian = new MyDatebox(pertemuanPunyaUjian.getSampaiUjian());
				sampaiUjian.setFormat(Common.dateFormat.get().toPattern());

				sampaiUjian.setParent(arg0);
				sampaiUjian.setWidth("90%");
				sampaiUjian.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pertemuanPunyaUjian.setSampaiUjian(sampaiUjian.getValue());
						Common.refreshUpdate(session, (pertemuanPunyaUjian));

					}
				});

				EventListener dibatasiWaktuEventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lama.setDisabled(!dibatasiWaktu.isChecked());
						lama.setValue(dibatasiWaktu.isChecked() ? pertemuanPunyaUjian.getLama() : null);

						mulaiUjian.setDisabled(!dibatasiWaktu.isChecked());
						mulaiUjian.setValue(dibatasiWaktu.isChecked() ? pertemuanPunyaUjian.getMulaiUjian() : null);

						sampaiUjian.setDisabled(!dibatasiWaktu.isChecked());
						sampaiUjian.setValue(dibatasiWaktu.isChecked() ? pertemuanPunyaUjian.getSampaiUjian() : null);

						if (siswa != null) {
							sampaiUjian.setDisabled(true);
							mulaiUjian.setDisabled(true);
							lama.setDisabled(true);
						}
					}
				};
				dibatasiWaktu.addEventListener("onCheck", dibatasiWaktuEventListener);
				dibatasiWaktuEventListener.onEvent(null);

				dibatasiWaktu.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pertemuanPunyaUjian.setDibatasiWaktu(dibatasiWaktu.isChecked());

						if (dibatasiWaktu.isChecked()) {
							pertemuanPunyaUjian.setSampaiUjian(sampaiUjian.getValue());
							pertemuanPunyaUjian.setMulaiUjian(mulaiUjian.getValue());
							pertemuanPunyaUjian.setLama(lama.getValue());
						}
						Common.refreshUpdate(session, (pertemuanPunyaUjian));

					}
				});

				lihatJawabanSetelahUjian.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pertemuanPunyaUjian.setLihatJawabanSetelahUjian(lihatJawabanSetelahUjian.isChecked());
						Common.refreshUpdate(session, pertemuanPunyaUjian);

					}
				});

				lihatNilaiSetelahUjian.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						pertemuanPunyaUjian.setLihatNilaiSetelahUjian(lihatNilaiSetelahUjian.isChecked());
						Common.refreshUpdate(session, pertemuanPunyaUjian);

					}
				});

				random.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event a) throws Exception {
						Session session = HibernateUtil.currentSession();
						pertemuanPunyaUjian.setRandom(random.isChecked());
						Common.refreshUpdate(session, pertemuanPunyaUjian);
						if (arg0 != null) {
							Common.clear(arg0);
						}
						render(arg0, pertemuanPunyaUjian);
					}
				});

				vbox = new Vbox();
				vbox.setParent(arg0);

				if (pertemuan.getPerkuliahan() != null) {

					Hbox hboxP = new Hbox();
					final Combobox formatNilai = new Combobox();

					formatNilai.setWidth("92px");
					MyComboitemConfig comboitemTidakAda = new MyComboitemConfig("Tidak Ada");
					comboitemTidakAda.setValue(null);
					formatNilai.appendChild(comboitemTidakAda);
					for (FormatNilai nilai : Common.getFormatNilais(session, pertemuan.getPerkuliahan())) {
						if (nilai.getStatusPertemuan() != null) {
							org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
							comboitem.setValue(nilai);
							comboitem.setLabel(
									nilai.getNama() + " (" + Common.numberFormat.get().format(nilai.getPersen()) + "%)");
							formatNilai.appendChild(comboitem);
						}
					}
					formatNilai.setParent(hboxP);
					if (pertemuanPunyaUjian.getFormatNilai() == null) {
						formatNilai.setSelectedItem(comboitemTidakAda);
					} else {
						Common.selectComboItem(formatNilai, pertemuanPunyaUjian.getFormatNilai());
					}
					formatNilai.setReadonly(true);
					formatNilai.setDisabled(pertemuan.getPerkuliahan().getDikunci() != null);
					if (pertemuan.getPerkuliahan().getDikunci() != null) {
						new MyLabelKecil("Penilaian sudah dikunci").setParent(vbox);
						if (pertemuanPunyaUjian.getFormatNilai() != null) {
							new MyLabelKecil(
									"Nilai otomatis masuk ke " + pertemuanPunyaUjian.getFormatNilai().getNama())
									.setParent(vbox);
						}
					}

					hboxP.setParent(vbox);
					final MyDoublebox prosentase = new MyDoublebox(pertemuanPunyaUjian.getProsentase());
					prosentase.setDisabled(pertemuan.getPerkuliahan().getDikunci() != null);
					prosentase.setCols(2);
					final Label labelbobot;
					hboxP.appendChild(labelbobot = new Label(ais.common.Common.getBahasaConfig(" bobot ")));
					prosentase.setParent(hboxP);
					hboxP.appendChild(new Label(" "));

					prosentase.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							pertemuanPunyaUjian.setProsentase(prosentase.getValue());
							Common.refreshUpdate(pertemuanPunyaUjian);
						}
					});

					final MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Singkronkan Nilai",
							"/img/Configure.gif");
					button.setParent(vbox);
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							ais.common.GradingHelper.hitungNilaiBerdasarkanFormatNilai(pertemuan.getPerkuliahan(),
									pertemuanPunyaUjian.getFormatNilai());
						}
					});

					prosentase.setVisible(pertemuanPunyaUjian.getFormatNilai() != null);
					button.setVisible(pertemuanPunyaUjian.getFormatNilai() != null);
					labelbobot.setVisible(pertemuanPunyaUjian.getFormatNilai() != null);

					formatNilai.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							final FormatNilai fn = (FormatNilai) (formatNilai.getSelectedItem() == null ? null
									: formatNilai.getSelectedItem().getValue());

							Session session = HibernateUtil.currentSession();
							pertemuanPunyaUjian.setFormatNilai(fn);
							try {
								Common.refreshUpdate(session, (pertemuanPunyaUjian));
							} catch (Exception eSimpan) {
								// FIX akar masalah ConstraintViolationException (pola sama dgn
								// TugasMandiriHelper): format nilai yang dipilih bisa saja sudah
								// dihapus admin lain sesaat sebelum combobox ini disimpan (race
								// condition lintas sesi) -- sebelumnya meledak mentah tanpa pesan
								// yang bisa dipahami user. Tangkap, rollback, catat, beri tahu user.
								try {
									if (session.getTransaction() != null && session.getTransaction().isActive()) {
										session.getTransaction().rollback();
									}
								} catch (Exception eRollback) { ais.common.ErrorAuditUtil.record(eRollback,
										"auto-audit(rollback-gagal) src/ais/action/master/sekolah/helper/PertemuanPunyaUjianSiswaHelper.java onFormatNilaiChange"); }
								ais.common.ErrorAuditUtil.record(eSimpan,
										"PertemuanPunyaUjianSiswaHelper: gagal simpan format nilai untuk PertemuanPunyaUjian id="
												+ (pertemuanPunyaUjian == null ? "null" : pertemuanPunyaUjian.getId()));
								MyMessageboxConfig.show(
										"Mohon maaf, gagal menyimpan format nilai karena ada data terkait yang tidak konsisten. "
												+ "Silakan muat ulang (refresh) halaman ini dan coba lagi. Jika masih gagal, hubungi Administrator.",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								return;
							}
							prosentase.setVisible(pertemuanPunyaUjian.getFormatNilai() != null);
							button.setVisible(pertemuanPunyaUjian.getFormatNilai() != null);
							labelbobot.setVisible(pertemuanPunyaUjian.getFormatNilai() != null);
						}

					});
				} else {

					Hbox hboxP = new Hbox();
					hboxP.setParent(vbox);

					if (pertemuan.getJadwalPelajaran() != null) {
						final Combobox formatNilai = new Combobox();

						formatNilai.setWidth("92px");
						MyComboitemConfig comboitemTidakAda = new MyComboitemConfig("Tidak Ada");
						comboitemTidakAda.setValue(null);
						formatNilai.appendChild(comboitemTidakAda);

						KelasSiswa kelasSiswa = pertemuan.getJadwalPelajaran().getKelas();

						JenisPenilaian jenisPenilaian = pertemuan.getJadwalPelajaran().getMatapelajaran()
								.getJenisPenilaian();
						if (pertemuan.getJadwalPelajaran().getKurikulumPunyaMatapelajaran() != null
								&& pertemuan.getJadwalPelajaran().getKurikulumPunyaMatapelajaran()
										.getKurikulumSekolah() != null
								&& pertemuan.getJadwalPelajaran().getKurikulumPunyaMatapelajaran().getKurikulumSekolah()
										.getJenisPenilaian() != null) {
							jenisPenilaian = pertemuan.getJadwalPelajaran().getKurikulumPunyaMatapelajaran()
									.getKurikulumSekolah().getJenisPenilaian();
						}

						List<GrupPenilaian> grupPenilaians = ConstantValues.simpleList(
								session.createCriteria(DetailJenisPenilaian.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("jenisPenilaian", jenisPenilaian))
										.setProjection(Projections.groupProperty("grupPenilaian.id")),
								GrupPenilaian.class, false);

						for (GrupPenilaian grupPenilaian : grupPenilaians) {

							if (grupPenilaian != null && kelasSiswa != null && kelasSiswa.getTingkat() > 0
									&& grupPenilaian.getKhususTingkat() != null
									&& !grupPenilaian.getKhususTingkat().equals(kelasSiswa.getTingkat())) {
								continue;
							}

							List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = ConstantValues
									.simpleList(
											session.createCriteria(DetailGrupPenilaian.class)
													.add(Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", true)))
													.add(Restrictions.isNotNull("grupKategoriItemPenilaianSiswa"))
													.setProjection(Projections
															.groupProperty("grupKategoriItemPenilaianSiswa.id"))
													.add(Restrictions.eq("grupPenilaian", grupPenilaian)),
											GrupKategoriItemPenilaianSiswa.class, false);

							if (grupKategoriItemPenilaianSiswas.isEmpty()) {
								return;
							}

							Collections.sort(grupKategoriItemPenilaianSiswas);
							for (GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {

								if (grupKategoriItemPenilaianSiswa != null && kelasSiswa != null
										&& kelasSiswa.getTingkat() > 0
										&& grupKategoriItemPenilaianSiswa.getKhususTingkat() != null
										&& !grupKategoriItemPenilaianSiswa.getKhususTingkat()
												.equals(kelasSiswa.getTingkat())) {
									continue;
								}

								List<KategoriItemPenilaianSiswa> kategoriItemPenilaianSiswasId = ConstantValues
										.simpleList(session.createCriteria(DetailGrupKategoriItemPenilaianSiswa.class)

												.add(Restrictions.eq("grupKategoriItemPenilaianSiswa",
														grupKategoriItemPenilaianSiswa))
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))

												.setProjection(
														Projections.groupProperty("kategoriItemPenilaianSiswa.id")),
												KategoriItemPenilaianSiswa.class, false);

								List<JenisItemPenilaianSiswa> jenisItemPenilaianSiswas = ConstantValues
										.simpleList(
												session.createCriteria(JenisItemPenilaianSiswa.class)
														.createAlias("kategoriItemPenilaianSiswa",
																"kategoriItemPenilaianSiswa")
														.addOrder(Order.asc("kategoriItemPenilaianSiswa.kode"))
														.addOrder(Order.asc("nomorUrut"))
														.add(Restrictions.in("kategoriItemPenilaianSiswa",
																kategoriItemPenilaianSiswasId))
														.add(Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true))),
												JenisItemPenilaianSiswa.class);
								for (JenisItemPenilaianSiswa jenisItemPenilaianSiswa : jenisItemPenilaianSiswas) {

									if (jenisItemPenilaianSiswa.getTipeDataInputan()
											.equals(JenisItemPenilaianSiswa.ANGKA)
											|| jenisItemPenilaianSiswa.getTipeDataInputan()
													.equals(JenisItemPenilaianSiswa.TEXT_ANGKA)) {

										org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
										comboitem.setValue(jenisItemPenilaianSiswa);
										comboitem.setLabel(jenisItemPenilaianSiswa.getNama() + " ("
												+ jenisItemPenilaianSiswa.getKode() + ")");
										comboitem.setDescription(grupKategoriItemPenilaianSiswa.getNama() + " ("
												+ grupPenilaian.getNama() + ")");

										comboitem.setAttribute("grupKategoriItemPenilaianSiswa",
												grupKategoriItemPenilaianSiswa);

										comboitem.setAttribute("grupPenilaian", grupPenilaian);

										formatNilai.appendChild(comboitem);
									}

								}

							}

						}

						formatNilai.setParent(vbox);
						if (pertemuanPunyaUjian.getJenisItemPenilaianSiswa() == null) {
							formatNilai.setSelectedItem(comboitemTidakAda);
						} else {
							Common.selectComboItem(formatNilai, pertemuanPunyaUjian.getJenisItemPenilaianSiswa());
						}
						formatNilai.setReadonly(true);
						formatNilai.setDisabled(pertemuan.getJadwalPelajaran().getDikunci() != null);
						if (pertemuan.getJadwalPelajaran().getDikunci() != null) {
							new MyLabelKecil("Penilaian sudah dikunci").setParent(vbox);
							if (pertemuanPunyaUjian.getJenisItemPenilaianSiswa() != null) {
								new MyLabelKecil("Nilai otomatis masuk ke "
										+ pertemuanPunyaUjian.getJenisItemPenilaianSiswa().getNama() + " "
										+ (pertemuanPunyaUjian.getJenisItemPenilaianSiswa()
												.getKategoriItemPenilaianSiswa() == null ? ""
														: " " + pertemuanPunyaUjian.getJenisItemPenilaianSiswa()
																.getKategoriItemPenilaianSiswa().getNama())

								).setParent(vbox);
							}
						}

						final MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Singkronkan Nilai",
								"/img/Configure.gif");
						button.setParent(vbox);
						button.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								ais.common.GradingHelper.hitungNilaiBerdasarkanJenisItemPenilaianSiswa(
										pertemuan.getJadwalPelajaran(),
										pertemuanPunyaUjian.getGrupKategoriItemPenilaianSiswa(),
										pertemuanPunyaUjian.getGrupPenilaian(),
										pertemuanPunyaUjian.getJenisItemPenilaianSiswa());
							}
						});
						button.setVisible(pertemuanPunyaUjian.getJenisItemPenilaianSiswa() != null);

						formatNilai.addEventListener("onChange", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								JenisItemPenilaianSiswa fn = (JenisItemPenilaianSiswa) (formatNilai
										.getSelectedItem() == null ? null : formatNilai.getSelectedItem().getValue());

								GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa = (GrupKategoriItemPenilaianSiswa) (formatNilai
										.getSelectedItem() == null ? null
												: formatNilai.getSelectedItem()
														.getAttribute("grupKategoriItemPenilaianSiswa"));

								GrupPenilaian grupPenilaian = (GrupPenilaian) (formatNilai.getSelectedItem() == null
										? null
										: formatNilai.getSelectedItem().getAttribute("grupPenilaian"));

								Session session = HibernateUtil.currentSession();
								pertemuanPunyaUjian.setJenisItemPenilaianSiswa(fn);
								pertemuanPunyaUjian.setGrupKategoriItemPenilaianSiswa(grupKategoriItemPenilaianSiswa);
								pertemuanPunyaUjian.setGrupPenilaian(grupPenilaian);
								Common.refreshUpdate(session, (pertemuanPunyaUjian));

								button.setVisible(pertemuanPunyaUjian.getJenisItemPenilaianSiswa() != null);
							}

						});

					}

					final MyDoublebox prosentase = new MyDoublebox(pertemuanPunyaUjian.getProsentase());
					prosentase.setCols(2);
					hboxP.appendChild(new Label(ais.common.Common.getBahasaConfig("Bobot ")));
					prosentase.setParent(hboxP);
					hboxP.appendChild(new Label(" "));

					prosentase.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							pertemuanPunyaUjian.setProsentase(prosentase.getValue());
							Common.refreshUpdate(pertemuanPunyaUjian);
						}
					});
				}

				final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
				checkbox.setChecked(ujian.getAktif());
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ujian.setAktif(checkbox.isChecked());
						Common.refreshSaveOrUpdate(ujian);
					}
				});

				Vbox vb = new Vbox();
				vb.setParent(arg0);
				Hbox hb = new Hbox();
				hb.setParent(vb);
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Sertifikat", "/img/certificate-icon.png");
				button.setOrient("vertical");

				button.setVisible(hasilUjianMahasiswa != null && ujian != null && hasilUjianMahasiswa.getLulus()
						&& ujian.getSertifikat() != null);
				button.setTooltiptext("Sertifikat");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						SertifikatAction.cetakSertifikat(hasilUjianMahasiswa);
					}
				});
				button.setParent(hb);

				if (pertemuanPunyaUjian != null) {
					button = new MyToolbarbuttonConfig("Hasil", "/img/album.png");
					button.setOrient("vertical");
					button.setVisible(tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
							&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null);
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							HasilUjianSiswaHelper hasilUjianMahasiswaHelper = new HasilUjianSiswaHelper(pertemuan);
							Window window = new Window("Hasil Ujian " + ujian.getNama() + " - " + pertemuan.toString(),
									"none", true);
							window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							window.setHeight("98%");
							window.setWidth("95%");
							hasilUjianMahasiswaHelper.display(pertemuanPunyaUjian, window);
							window.onModal();
						}
					});
					button.setParent(hb);
				}

				button = new MyToolbarbuttonConfig("Preview", "/img/eye-icon.png");
				button.setOrient("vertical");
				button.setVisible(tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
						&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null);
				button.setTooltiptext("Preview");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Tbmuser tbmuser = Common.getCurrentUser();
						ProsesUjianHelper.ikut(null, null, tbmuser.getSiswa(), tbmuser.getCalonSiswa(),
								pertemuanPunyaUjian, hasilUjianMahasiswa, true, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										loadData(null);
									}
								});
					}
				});
				button.setParent(hb);

				hb = new Hbox();
				hb.setParent(vb);

				button = new MyToolbarbuttonConfig("Ubah", "/img/svg/edit-box-line.svg");
				button.setOrient("vertical");
				button.setVisible(tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
						&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null);
				button.setTooltiptext("Ubah Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						UjianAction.onAddExternal(event, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										loadData(true);
									}
								}, "Loading..", false, 1500);

							}
						}, ujian, pertemuan == null ? null : pertemuan.untuk());
					}
				});
				button.setParent(hb);

				button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
				button.setOrient("vertical");
				button.setVisible(tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
						&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null);
				// button.setDisabled(count > 0);
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												Session session = HibernateUtil.currentSession();

												String sql = "delete from hasil_ujian_mahasiswa where pertemuan_punya_ujian = "
														+ pertemuanPunyaUjian.getId();

												session.createSQLQuery(sql).executeUpdate();

												Common.refreshDelete(session, pertemuanPunyaUjian);

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														loadData(true);
													}
												}, "Loading..", false, 1500);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(
														"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
																+ e.getMessage());
											}

										}

									}
								});

					}

				});
				button.setParent(hb);

			}

			if (hasilUjianMahasiswa != null && (siswa != null || calonSiswa != null || tbmuser.getSiswa() != null
					|| tbmuser.getCalonSiswa() != null)) {

				int kuota = 120;
				try {
					kuota = Integer.parseInt(Common.getKonfigurasi("kuota_ujian", kuota + "").getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PertemuanPunyaUjianSiswaHelper.java:2016");
					// TODO: handle exception
				}

				if (kuota <= ProsesUjianHelper.kuotaUjian.size()
						&& !ProsesUjianHelper.kuotaUjian.contains(hasilUjianMahasiswa.getKeyhasil())) {
					Common.freeze(arg0, true);

					if (arg0 != null) {

						Common.clear(arg0);

					}
					ais.ui.util.ZkCompat.setSpans(arg0, "10");
					Label lbl = new Label(
							"Maaf, kuota ujian masih penuh, jangan ditutup dan tunggu beberapa waktu untuk ikut kembali ujian. Klik tombol \"Lihat Peserta Ujian\" untuk mengetahui peserta yang saat ini sedang ujian.");
					arg0.appendChild(lbl);
					lbl.setStyle("font-size:15px;color:red;");

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(false);
						}
					}, "", false, 5000);

					return;
				}

			}

			if (siswa != null || calonSiswa != null || tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null) {
				Long id = siswa != null ? siswa.getId()
						: calonSiswa != null ? calonSiswa.getId()
								: tbmuser.getSiswa() != null ? tbmuser.getSiswa().getId()
										: tbmuser.getCalonSiswa() != null ? tbmuser.getCalonSiswa().getId() : null;

				if (id != null && (ujian != null && !ujian.getAktif())
						|| pertemuanPunyaUjian.getMhsYgTidakIkut().contains("," + id + ",")) {
					Common.freeze(arg0, true);

					if (arg0 != null) {

						Common.clear(arg0);

					}
					ais.ui.util.ZkCompat.setSpans(arg0, "10");
					Label lbl = new Label("Anda tidak diizinkan ikut ujian \"" + ujian.getNama() + "\"");
					arg0.appendChild(lbl);
					lbl.setStyle("font-size:15px;color:red;");

				}
			}
		}
	}

	public void loadData(Object value) {
		// Session session = HibernateUtil.currentSession();
		// List<PertemuanPunyaUjian> pertemuanPunyaUjian =
		// session.createCriteria(PertemuanPunyaUjian.class)
		// .createAlias("ujian",
		// "ujian").addOrder(Order.asc("id")).add(Restrictions.eq("pertemuan",
		// pertemuan))
		// .list();
		//
		// Collection<PertemuanPunyaUjian> pertemuanPunyaUjians =
		// pertemuan.ambilPertemuanPunyaUjianTotal()
		// .values();

		if (value != null && value.equals(true)) {
			pertemuan.belum("pertemuan_punya_Ujian");
		}
		Tbmuser tbmuser = Common.getCurrentUser();
		List<PertemuanPunyaUjian> pertemuanPunyaUjian = new ArrayList<PertemuanPunyaUjian>(
				pertemuan.ambilPertemuanPunyaUjianTotal(tbmuser).values());

		ListModel strset = new SimpleListModel(pertemuanPunyaUjian);
		grid.setRowRenderer(new DetailPertemuanRenderer());
		grid.setModelCheckMobile(strset);

	}

	@SuppressWarnings("unchecked")
	public void display(final Pertemuan pertemuan, final Component component) {
		this.pertemuan = pertemuan;
		if (component != null) {
			Common.clear(component);
		}

		Tbmuser tbmuser = Common.getCurrentUser();

		Div div = new Div();
		div.setStyle("min-height:3600px");
		div.setWidth("100%");
		div.setParent(component);

//		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
//		borderlayout.setParent(component);
//		Center center = new Center();
//		center.setParent(borderlayout);
//		ais.ui.util.ZkCompat.setFlex(center, true);
//
//		North north = new North();
//		north.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(div);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Bahan Ujian", "/img/new.gif");
		button.setVisible(
				siswa == null && calonSiswa == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				List<Ujian> ujians = HibernateUtil.currentSession().createCriteria(PertemuanPunyaUjian.class)
						.add(Restrictions.eq("pertemuan", pertemuan)).setProjection(Projections.property("ujian"))
						.list();

				AmbilDataUjianBanyak window = new AmbilDataUjianBanyak(ujians, pertemuan.untuk(),
						pertemuan.getPerkuliahan() == null ? null : pertemuan.getPerkuliahan().getMatakuliah(),
						pertemuan.getJadwalPelajaran() == null ? null
								: pertemuan.getJadwalPelajaran().getMatapelajaran());

				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("90%");
				window.setHeight("95%");

				window.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						List<Ujian> ujians = (List<Ujian>) arg0.getData();

						if (ujians != null) {
							Session session = HibernateUtil.currentSession();

							Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);

							Calendar waktu = ais.ui.util.WaktuUtil.getCalendar();
							waktu.set(Calendar.SECOND, 0);
							waktu.set(Calendar.HOUR_OF_DAY, 0);
							waktu.set(Calendar.MINUTE, 30);

							for (Ujian ujian : ujians) {

								PertemuanPunyaUjian pertemuanPunyaUjian = new PertemuanPunyaUjian();
								pertemuanPunyaUjian.setUjian(ujian);
								pertemuanPunyaUjian.setPertemuan(pertemuan);
								pertemuanPunyaUjian.setDibatasiWaktu(true);
								pertemuanPunyaUjian.setLama(waktu.getTime());
								pertemuanPunyaUjian.setMulaiUjian(ais.ui.util.WaktuUtil.getDate());
								pertemuanPunyaUjian.setSampaiUjian(calendar.getTime());

								session.save(pertemuanPunyaUjian);
								CommonEmail.infoAdaUjianPerkuliahan(pertemuan, ujian);
								ais.common.CommonNotifikasi.infoUjianBaru(pertemuan, ujian);
							}

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(true);
								}
							}, "Loading..", false, 1500);

						}

					}
				});

				window.onModal();
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Buat Ujian", "/img/new.gif");
		button.setVisible(
				siswa == null && calonSiswa == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				Ujian ujian = new Ujian();
				ujian.setDosen(pertemuan.getPerkuliahan() == null ? null : pertemuan.getPerkuliahan().getDosen1());
				ujian.setMatakuliah(
						pertemuan.getPerkuliahan() == null ? null : pertemuan.getPerkuliahan().getMatakuliah());

				ujian.setGuru(pertemuan.getJadwalPelajaran() == null ? null : pertemuan.getJadwalPelajaran().getGuru());
				ujian.setMatapelajaran(pertemuan.getJadwalPelajaran() == null ? null
						: pertemuan.getJadwalPelajaran().getMatapelajaran());

				UjianAction.onAddExternal(event, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Ujian ujian = (Ujian) arg0.getData();
						if (ujian != null) {

							Session session = HibernateUtil.currentSession();

							Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);

							Calendar waktu = ais.ui.util.WaktuUtil.getCalendar();
							waktu.set(Calendar.SECOND, 0);
							waktu.set(Calendar.HOUR_OF_DAY, 0);
							waktu.set(Calendar.MINUTE, 30);

							PertemuanPunyaUjian pertemuanPunyaUjian = new PertemuanPunyaUjian();
							pertemuanPunyaUjian.setUjian(ujian);
							pertemuanPunyaUjian.setPertemuan(pertemuan);
							pertemuanPunyaUjian.setDibatasiWaktu(true);
							pertemuanPunyaUjian.setLama(waktu.getTime());
							pertemuanPunyaUjian.setMulaiUjian(ais.ui.util.WaktuUtil.getDate());
							pertemuanPunyaUjian.setSampaiUjian(calendar.getTime());

							session.save(pertemuanPunyaUjian);

							CommonEmail.infoAdaUjianPerkuliahan(pertemuan, ujian);
							ais.common.CommonNotifikasi.infoUjianBaru(pertemuan, ujian);

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(true);
								}
							}, "Loading..", false, 1500);

						}
					}
				}, ujian, pertemuan.untuk());
			}
		});
		button.setParent(toolbar);

		if (pertemuan.getPerkuliahan() != null) {
			final Perkuliahan perkuliahan = pertemuan.getPerkuliahan();
			if (perkuliahan != null && !perkuliahan.getSembunyikanFormatPenilaian()) {
				final MyToolbarbuttonConfig buttonFormatNilai = new MyToolbarbuttonConfig("Format Nilai",
						"/img/svg/edit-box-line.svg");
				buttonFormatNilai.setParent(toolbar);
				buttonFormatNilai.setVisible(perkuliahan.getDikunci() == null && siswa == null && calonSiswa == null
						&& tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null);
				buttonFormatNilai.addEventListener("onClick", new EventListener() {

					FormatPenilaianHelper formatPenilaianHelper = new FormatPenilaianHelper();

					@Override
					public void onEvent(Event event) throws Exception {

						MyWindow addWindow = new MyWindow();
						addWindow.setHeight("95%");
						addWindow.setWidth("700px");
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);

						formatPenilaianHelper.display(perkuliahan, addWindow, new TampilDetailNilaiInterface() {

							@Override
							public void realoadNilai(final Perkuliahan perkuliahan) {

								Common.realoadNilai(perkuliahan, perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi(),
										new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														loadData(true);
													}
												}, "Loading..", false, 1500);

											}
										}, null);

							}
						});
					}

				});
			}
		}

		Common.bolehKonfigurasi("tampilkan_rekap_hasil_ujian");

		MyToolbarbuttonConfig buttonFormatNilai = new MyToolbarbuttonConfig("Rekap Hasil Ujian",
				"/img/svg/edit-box-line.svg");
		buttonFormatNilai.setVisible(
				siswa == null && calonSiswa == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null);
		buttonFormatNilai.setParent(toolbar);
		buttonFormatNilai.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				RekapHasilUjian addWindow = new RekapHasilUjian(pertemuan);
				addWindow.setClosable(true);
				addWindow.setTitle("Rekap Hasil Ujian");
				addWindow.setHeight("95%");
				addWindow.setWidth("90%");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
				addWindow.onModal();
			}

		});

		buttonFormatNilai = new MyToolbarbuttonConfig("Rekap Semua Hasil Ujian", "/img/svg/edit-box-line.svg");
		buttonFormatNilai.setVisible(
				siswa == null && calonSiswa == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null);
		buttonFormatNilai.setParent(toolbar);
		buttonFormatNilai.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				RekapHasilUjianPerVoPertemuan addWindow = new RekapHasilUjianPerVoPertemuan(false,
						pertemuan == null ? null : pertemuan.ambilVOPembelajaran());
				addWindow.setClosable(true);
				addWindow.setTitle("Rekap Semua Hasil Ujian");
				addWindow.setHeight("95%");
				addWindow.setWidth("90%");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
				addWindow.onModal();
			}

		});

//		if (siswa != null || calonSiswa != null || tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null) {
//			button = new MyToolbarbuttonConfig("Rekap Hasil Ujian", "/img/Document-Text-icon.png");
//			button.addEventListener("onClick", new EventListener() {
//
//				@Override
//				public void onEvent(Event arg0) throws Exception {
//					try {
//
//						RekapHasilUjianMahasiswa addWindow = new RekapHasilUjianMahasiswa(false, siswa,
//								calonSiswa, pertemuan == null ? null : pertemuan.ambilVOPembelajaran());
//						addWindow.setClosable(true);
//						addWindow.setTitle("Rekap Hasil Ujian");
//						addWindow.setHeight("95%");
//						addWindow.setWidth("90%");
//						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
//						addWindow.onModal();
//
//					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PertemuanPunyaUjianSiswaHelper.java:2370");
//						e.printStackTrace();
//					}
//				}
//			});
//			button.setParent(toolbar);
//		}

		button = prosesUlangSoal(pertemuan, "Singkronkan Soal Peserta", "/img/svg/refresh-cw.svg");
		button.setVisible(
				siswa == null && calonSiswa == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null);
		button.setParent(toolbar);

		// Tombol Rekap: rekap pelanggaran pengawasan ujian (anti-curang) per peserta.
		button = new MyToolbarbuttonConfig("Rekap", "/img/print.png");
		button.setTooltiptext("Rekap pengawasan ujian (jumlah & log pelanggaran per peserta)");
		button.setVisible(
				siswa == null && calonSiswa == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ais.database.model.PertemuanPunyaUjian ppu = (ais.database.model.PertemuanPunyaUjian) ais.database.hibernate.HibernateUtil
						.currentSession().createCriteria(ais.database.model.PertemuanPunyaUjian.class)
						.add(org.hibernate.criterion.Restrictions.eq("pertemuan", pertemuan)).setMaxResults(1)
						.uniqueResult();
				if (ppu != null) {
					ais.action.master.helper.RekapPengawasanUjianHelper.tampilkanRekap(ppu);
				}
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.setVisible(
				siswa == null && calonSiswa == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiPertemuanPunyaUjianHelper revisiHelper = new RevisiPertemuanPunyaUjianHelper(pertemuan,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(true);
							}
						});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Lihat Peserta Ujian", "/img/eye-icon.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.displayWindow("/pages/master/hasil_ujian_mahasiswa.zul", true, "95%",
						Common.isMobile() ? "100%" : "950px", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(false);
							}
						});
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(true);
			}
		});

		button.setParent(toolbar);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("min-height:3600px");
		grid.setMold("paging");
		grid.setPageSize(5);
		grid.setSclass("fgrid");
		grid.setParent(div);
		grid.getPagingChild().setMold("os");
		grid.setPagingPosition("top");

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth(
				siswa == null && calonSiswa == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
						? "40px"
						: "0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ujian");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Skor/Jml.Ikut.Ujian");
		column.setWidth("12%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai/Maks.blh.Ikut");
		column.setWidth((Common.bolehKonfigurasi("nilai_ujian_ditampilkan_ke_siswa")
				&& (siswa != null || calonSiswa != null || tbmuser.getSiswa() != null
						|| tbmuser.getCalonSiswa() != null)) ? "12%" : "0px");
		column.setVisible(
				siswa == null && calonSiswa == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel(
				(siswa != null || calonSiswa != null || tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null)
						? "Jml.Soal/Maks.Skor"
						: "Jml Soal");
		column.setWidth(
				siswa != null || calonSiswa != null || tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null
						? "12%"
						: "10%");
		column.setVisible(
				siswa == null && calonSiswa == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dibatasi Wkt");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Lama");
		column.setWidth("12%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dimulai");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sampai");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai masuk ke");
		column.setWidth("14%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Aktif");
		column.setWidth(
				siswa == null && calonSiswa == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
						? "40px"
						: "0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("8%");

		loadData(null);

	}

}
