package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.model.impl.BookHelper;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.BankSoalAction;
import ais.action.master.helper.generic.AmbilDataBankSoalBanyak;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.BankSoalDetail;
import ais.database.model.GeneralValueObject;
import ais.database.model.PenjelasanBankSoal;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class DetailGrupSoalHelper implements DataLoader {

	private Grid grid;

	private Textbox cari;

	private Paging paging;
	private Tbmuser tbmuser;
	protected int countHasil = 0;
	private PenjelasanBankSoal penjelasanBankSoal;

	public DetailGrupSoalHelper() {
		tbmuser = Common.getCurrentUser();
	}

	public void display(final PenjelasanBankSoal penjelasanBankSoal, Component detail) {
		this.penjelasanBankSoal = penjelasanBankSoal;
		tbmuser = Common.getCurrentUser();

		final Groupbox groupbox = new Groupbox();
		groupbox.setParent(detail);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Soal", "/img/new.gif");
		button.setVisible(tbmuser.getMahasiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Long> bankSoals = HibernateUtil.currentSession().createCriteria(BankSoal.class)
						.setProjection(Projections.property("id"))
						.add(Restrictions.eq("penjelasanBankSoal", penjelasanBankSoal)).list();

				AmbilDataBankSoalBanyak window = new AmbilDataBankSoalBanyak(bankSoals,
						penjelasanBankSoal.getJenisKoreksi(), null, null);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("90%");
				window.setHeight("95%");

				window.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<BankSoal> bankSoals = (List<BankSoal>) arg0.getData();
						if (bankSoals != null) {

							Session session = HibernateUtil.currentSession();

							for (BankSoal bankSoal : bankSoals) {
								bankSoal.setPenjelasanBankSoal(penjelasanBankSoal);
								Common.refreshUpdate(session, penjelasanBankSoal);
							}

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(true);
								}
							});

						}
					}
				});

				window.onModal();

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Soal Baru", "/img/svg/addthis.svg");
		button.setVisible(tbmuser.getMahasiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				BankSoal bankSoal = new BankSoal();
				bankSoal.setJenisKoreksi(penjelasanBankSoal.getJenisKoreksi());
				bankSoal.setFakultas(penjelasanBankSoal.getFakultas());
				bankSoal.setJurusan(penjelasanBankSoal.getJurusan());
				bankSoal.setDosen(penjelasanBankSoal.getDosen());
				bankSoal.setGuru(penjelasanBankSoal.getGuru());
				bankSoal.setSatuanKerja(penjelasanBankSoal.getSatuanKerja());
				bankSoal.setPenjelasanBankSoal(penjelasanBankSoal);
				bankSoal.setJenisKoreksi(penjelasanBankSoal.getJenisKoreksi());

				BankSoalAction.onAddExternal(event, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						BankSoal bankSoal = (BankSoal) arg0.getData();
						Session session = HibernateUtil.currentSession();

						bankSoal.setPenjelasanBankSoal(penjelasanBankSoal);
						Common.refreshUpdate(session, bankSoal);
						session.flush();

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(true);
							}
						});

					}
				}, bankSoal, penjelasanBankSoal.getJenisKoreksi());
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Download", "/img/excel.png");
		button.setVisible(tbmuser.getMahasiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(), "/img/excel.png");
		button.setVisible(tbmuser.getMahasiswa() == null);
		button.setUpload(Common.ukuranFileUpload());
		button.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				uploadSoal(media, penjelasanBankSoal);
			}
		});

		button.setParent(toolbar);

		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
		button.setVisible(tbmuser.getMahasiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil.currentSession();
										List<BankSoal> bankSoals = session.createCriteria(BankSoal.class)
												.add(Restrictions.eq("penjelasanBankSoal", penjelasanBankSoal)).list();
										for (BankSoal bankSoal : bankSoals) {
											session.delete(bankSoal);
										}

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												loadData(true);
											}
										});

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
									}

								}

							}
						});
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		button.setAttribute("janganDisabled", true);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(true);

			}
		});

		button.setParent(toolbar);

		cari = new Textbox();
		cari.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		toolbar.appendChild(cari);
		button = new MyToolbarbuttonConfig("", "/img/search.png");
		button.setAttribute("janganDisabled", true);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}
		});

		button.setParent(toolbar);

		paging = new Paging();
		Common.initPaging1(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);

			}
		});
		paging.setParent(groupbox);

		grid = new MyGrid();

		grid.setSclass("fgrid");
		grid.setParent(groupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setMold("paging");
		grid.setPageSize(5000);
		grid.setStyle("min-height:1400px");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("");
		column.setParent(columns);

		loadData(null);

	}

	public class DetailUjianRenderer extends ais.ui.util.MyRowRenderer {

		private EventListener ubahEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		};

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			BankSoal bankSoal = (BankSoal) arg1;
			DetailUjianHelper.tampilSoalDanJawaban(arg0, bankSoal, null, tbmuser, true, true, true, ubahEventListener,
					tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null,
					tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null);
		}

	}

	@SuppressWarnings("unchecked")
	public static void doDownload(Criteria criteria) throws Exception {

		List<BankSoal> bankSoals = criteria.list();

		Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(20);
		spreadsheet.setMaxrows(bankSoals.size() + 5);
		final String color = "#000000";

		Worksheet sheet = spreadsheet.getSelectedSheet();

		int rowIndex = 4;
		int colIndex = 5;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, Common.getBahasaConfig("SOAL"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, Common.getBahasaConfig("BENAR"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, Common.getBahasaConfig("NILAI SKOR BENAR"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, Common.getBahasaConfig("NILAI SKOR SALAH"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, Common.getBahasaConfig("NILAI SKOR DEFAULT"));
		for (int i = ((int) 'A'); i <= ((int) 'J'); i++) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), "JWB_" + ((char) i));
		}
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), Common.getBahasaConfig("PENJELASAN"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex),
				Common.getBahasaConfig("TAMPIL PENJELASAN SAAT UJIAN"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), Common.getBahasaConfig("JENIS"));

		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);

		Utils.setColumnWidth(sheet, 1, 450);
		Utils.setColumnWidth(sheet, 2, 30);
		Utils.setColumnWidth(sheet, 0, 25);
		Utils.setRowHeight(sheet, 4, 40);
		Utils.setRowHeight(sheet, 2, 1);
		try {

			ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 1, Common.getBahasaConfig("DAFTAR SOAL"));
			Utils.setRowHeight(sheet, 1, 130);
			ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
			Cell cell = Utils.getCell(sheet, 1, 1);
			cell.getCellStyle().setWrapText(true);
			cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 1, 1, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(1, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				BookHelper.BORDER_FULL, BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(1, rowIndex + 1, spreadsheet.getMaxcolumns() - 1, rowIndex + 1), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);

		rowIndex = 5;
		colIndex = 1;
		for (BankSoal bankSoal : bankSoals) {

			colIndex = 1;
			if (bankSoal == null) {
				continue;
			}
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, bankSoal.getId());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, bankSoal.getSoal());

			List<Long> bankSoalDetail = bankSoal == null || bankSoal.getId() == null ? new ArrayList<Long>()
					: bankSoal.ambilBankSoalDetail(true);

			if (bankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA)) {
				String benar = "";
				for (Long detailid : bankSoalDetail) {
					BankSoalDetail detail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							detailid.toString());
					if (detail != null) {
						if (detail.getBetul() != null && detail.getBetul()) {
							benar += (benar.equals("") ? detail.getHuruf() : "," + detail.getHuruf());
						}
					}
				}
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, benar);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, bankSoal.getSkor());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, bankSoal.getSkorSalah());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, bankSoal.getSkorDefault());
				for (Long detailid : bankSoalDetail) {
					BankSoalDetail detail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							detailid.toString());
					if (detail != null) {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex,
								detail.getJawaban() == null ? "" : detail.getJawaban());
					}
				}

			} else {
				for (Long detailid : bankSoalDetail) {
					BankSoalDetail detail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							detailid.toString());
					if (detail != null) {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex,
								detail.getEssay() == null ? "" : detail.getEssay());
					}
				}
			}

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16,
					bankSoal.getPenjelasanBankSoal() == null ? "" : bankSoal.getPenjelasanBankSoal().toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, bankSoal.getTampilPenjelasanSaatUjian());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, bankSoal.getJenis());
			rowIndex++;

		}

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		spreadsheet.getBook().write(bout);
		bout.close();
		String fileName = "bank_soal__.xlsx";
		fileName = fileName.replaceAll(" ", "_");
		Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				fileName);
	}

	@SuppressWarnings("unchecked")
	public static void doDownload(PenjelasanBankSoal penjelasanBankSoal, Criteria criteria) throws Exception {

		List<BankSoal> bankSoals = criteria.list();

		Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(20);
		spreadsheet.setMaxrows(bankSoals.size() + 5);
		final String color = "#000000";

		Worksheet sheet = spreadsheet.getSelectedSheet();

		int rowIndex = 4;
		int colIndex = 5;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, Common.getBahasaConfig("SOAL"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, Common.getBahasaConfig("BENAR"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, Common.getBahasaConfig("NILAI SKOR BENAR"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, Common.getBahasaConfig("NILAI SKOR SALAH"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, Common.getBahasaConfig("NILAI SKOR DEFAULT"));
		for (int i = ((int) 'A'); i <= ((int) 'J'); i++) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), "JWB_" + ((char) i));
		}
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), Common.getBahasaConfig("PENJELASAN"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex),
				Common.getBahasaConfig("TAMPIL PENJELASAN SAAT UJIAN"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), Common.getBahasaConfig("JENIS"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), Common.getBahasaConfig("NO."));

		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		if (penjelasanBankSoal != null) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, 0, 0, penjelasanBankSoal.getId());
		}
		Utils.setColumnWidth(sheet, 1, 450);
		Utils.setColumnWidth(sheet, 2, 30);
		Utils.setColumnWidth(sheet, 0, 25);
		Utils.setRowHeight(sheet, 4, 40);
		Utils.setRowHeight(sheet, 2, 1);
		try {

			ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 1, Common.getBahasaConfig("DAFTAR SOAL UJIAN"));
			Utils.setRowHeight(sheet, 1, 130);
			ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
			Cell cell = Utils.getCell(sheet, 1, 1);
			cell.getCellStyle().setWrapText(true);
			cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 1, 1, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(1, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				BookHelper.BORDER_FULL, BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(1, rowIndex + 1, spreadsheet.getMaxcolumns() - 1, rowIndex + 1), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);

		rowIndex = 5;
		colIndex = 1;
		for (BankSoal bankSoal : bankSoals) {
			colIndex = 1;
			if (bankSoal == null) {
				continue;
			}
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, bankSoal.getId());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, bankSoal.getSoal());

			List<Long> bankSoalDetail = bankSoal == null || bankSoal.getId() == null ? new ArrayList<Long>()
					: bankSoal.ambilBankSoalDetail(true);

			if (bankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA)) {
				String benar = "";
				for (Long detailid : bankSoalDetail) {
					BankSoalDetail detail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							detailid.toString());
					if (detail != null) {
						if (detail.getBetul() != null && detail.getBetul()) {
							benar += (benar.equals("") ? detail.getHuruf() : "," + detail.getHuruf());
						}
					}
				}
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, benar);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, bankSoal.getSkor());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, bankSoal.getSkorSalah());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, bankSoal.getSkorDefault());
				for (Long detailid : bankSoalDetail) {
					BankSoalDetail detail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							detailid.toString());
					if (detail != null) {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex,
								detail.getJawaban() == null ? "" : detail.getJawaban());
					}
				}

			} else {
				for (Long detailid : bankSoalDetail) {
					BankSoalDetail detail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							detailid.toString());
					if (detail != null) {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex,
								detail.getEssay() == null ? "" : detail.getEssay());
					}
				}
			}

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16,
					bankSoal.getPenjelasanBankSoal() == null ? "" : bankSoal.getPenjelasanBankSoal().toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, bankSoal.getTampilPenjelasanSaatUjian());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, bankSoal.getJenis());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 19, penjelasanBankSoal.getNomorUrut());
			rowIndex++;

		}

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		spreadsheet.getBook().write(bout);
		bout.close();
		String fileName = "template_ujian_"
				+ (penjelasanBankSoal == null ? "" : penjelasanBankSoal.getId() + "_" + penjelasanBankSoal.getNama())
				+ "_.xlsx";
		fileName = fileName.replaceAll(" ", "_");
		Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				fileName);
	}

	public static void doUpload(Media media, final DataLoader dataLoader) throws Exception {
		if (media.getName().toLowerCase().endsWith("xlsx")) {

			InputStream inputStream = media.getStreamData();
			// System.out.println("media = " + media);
			File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
			// System.out.println("file = " + file.getAbsolutePath());
			file.getParentFile().mkdirs();
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			int c;
			while ((c = inputStream.read()) != -1) {
				fileOutputStream.write(c);
			}
			fileOutputStream.close();
			inputStream.close();

			XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
			XSSFSheet sheet = workbook.getSheetAt(0);

			List<List<String>> objects = Common.getSheetContent(sheet);

			int terupload = 0;
			Session session = HibernateUtil.currentNativeSession();
			for (List<String> strings : objects) {

				try {
					String id = strings.get(0);
					String soal = strings.get(1);

					if (soal != null) {
						soal = soal.trim();
					} else {
						soal = "";
					}

					if (soal != null && !soal.isEmpty() && !soal.equalsIgnoreCase("soal")
							&& !soal.equalsIgnoreCase("DAFTAR SOAL UJIAN")) {

						String benar = strings.get(2);
						String skor = strings.get(3);
						String skorSalah = strings.get(4);
						String benarDefault = strings.get(5);
						String[] betuls = benar.split(",");

						String penjelasan = "";
						try {
							penjelasan = strings.get(16);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:636");

						}

						PenjelasanBankSoal penjelasanBankSoal = (PenjelasanBankSoal) Common
								.getContentAsObject(penjelasan, PenjelasanBankSoal.class, null);

						Boolean tampilPenjelasanSaatUjian = false;
						try {
							tampilPenjelasanSaatUjian = Boolean.parseBoolean(strings.get(17).trim());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:646");

						}

						String jenis = BankSoal.PILIHAN_GANDA;
						try {
							jenis = strings.get(18);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:653");

						}

						if (id != null) {
							id = org.apache.commons.lang3.StringUtils.replace(id, ".", "");
						}

						System.out.println("id " + id + ", soal " + soal);
						System.out.println("benar " + benar + ", skor " + skor + ", skorSalah " + skorSalah
								+ ", benarDefault " + benarDefault + ", jenis = " + jenis);

						BankSoal newBankSoal = (BankSoal) session.createCriteria(BankSoal.class)
								.add(Restrictions.or(
										id == null || !Common.isNumber(id) ? Restrictions.sqlRestriction("false")
												: Restrictions.eq("id", Long.parseLong(id.trim())),
										Restrictions.ilike("soal", soal, MatchMode.EXACT)))
								.setMaxResults(1).uniqueResult();

						if (newBankSoal == null) {
							newBankSoal = new BankSoal();
						}
						try {
							newBankSoal.setSkor(Double.parseDouble(skor.trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:677");

						}
						try {
							newBankSoal.setSkorSalah(Double.parseDouble(skorSalah.trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:682");

						}
						try {
							newBankSoal.setSkorDefault(Double.parseDouble(benarDefault.trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:687");

						}

						newBankSoal.setJenis(jenis);
						newBankSoal.setKeterangan("");
						newBankSoal.setSoal(soal);
						newBankSoal.setPenjelasanBankSoal(penjelasanBankSoal);
						newBankSoal.setTampilPenjelasanSaatUjian(tampilPenjelasanSaatUjian);
						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, newBankSoal);
						session.getTransaction().commit();
						int jumlahJawaban = 0;
						if (newBankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA)) {
							int j = 6;
							for (int i = ((int) 'A'); i <= ((int) 'J'); i++) {
								try {
									String huruf = ((char) i) + "";
									String jawaban = strings.get(j);
									if (jawaban != null && !jawaban.trim().equals("")) {
										BankSoalDetail bankSoalDetail = (BankSoalDetail) session
												.createCriteria(BankSoalDetail.class)
												.add(Restrictions.eq("bankSoal", newBankSoal))
												.add(Restrictions.eq("huruf", huruf)).setMaxResults(1).uniqueResult();
										if (bankSoalDetail == null) {
											bankSoalDetail = new BankSoalDetail();
										}

										bankSoalDetail.setBankSoal(newBankSoal);
										boolean betul = false;
										for (String s : betuls) {
											betul |= (s != null && s.trim().equalsIgnoreCase(huruf));
										}
										bankSoalDetail.setBetul(betul);
										bankSoalDetail.setEssay("");
										bankSoalDetail.setHuruf(huruf);
										bankSoalDetail.setJawaban(jawaban);
										bankSoalDetail.setKeterangan("");

										session.getTransaction().begin();
										Common.refreshSaveOrUpdate(session, bankSoalDetail);
										session.getTransaction().commit();
									}
									j++;
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:731");

								}
							}

						} else {
							BankSoalDetail bankSoalDetail = (BankSoalDetail) session
									.createCriteria(BankSoalDetail.class).add(Restrictions.eq("bankSoal", newBankSoal))
									.setMaxResults(1).uniqueResult();
							if (bankSoalDetail == null) {
								bankSoalDetail = new BankSoalDetail();
							}
							bankSoalDetail.setBankSoal(newBankSoal);
							bankSoalDetail.setBetul(true);
							bankSoalDetail.setEssay(benar);
							bankSoalDetail.setHuruf("");
							bankSoalDetail.setJawaban("");
							bankSoalDetail.setKeterangan("");

							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, bankSoalDetail);
							session.getTransaction().commit();
						}

						Integer count = ((Number) HibernateUtil.currentSession().createCriteria(BankSoalDetail.class)
								.add(Restrictions.eq("bankSoal", newBankSoal)).add(Restrictions.eq("betul", true))
								.setProjection(Projections.rowCount()).uniqueResult()).intValue();
						if (count > 1) {
							newBankSoal.setJenisPilihanGanda(BankSoal.COMBINATION_CHOICE);
						} else if (jumlahJawaban == 2) {
							newBankSoal.setJenisPilihanGanda(BankSoal.BENAR_SALAH);
						} else {
							newBankSoal.setJenisPilihanGanda(BankSoal.MULTIPLE_COICE);
						}

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, newBankSoal);
						session.getTransaction().commit();

						terupload++;
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailGrupSoalHelper.java:773");
				}

			}
			HibernateUtil.closeSession();

			MyMessageboxConfig.show("Upload soal telah selesai dilakukan, " + terupload + " terupload", "Pemberitahuan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							dataLoader.loadData(true);
						}
					});

		} else {
			MyMessageboxConfig.show(
					"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
							+ media,
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
		}
	}

	public static void doUpload(Media media, PenjelasanBankSoal penjelasanBankSoal, final DataLoader dataLoader)
			throws Exception {
		if (media.getName().toLowerCase().endsWith("xlsx")) {

			InputStream inputStream = media.getStreamData();
			// System.out.println("media = " + media);
			File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
			// System.out.println("file = " + file.getAbsolutePath());
			file.getParentFile().mkdirs();
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			int c;
			while ((c = inputStream.read()) != -1) {
				fileOutputStream.write(c);
			}
			fileOutputStream.close();
			inputStream.close();

			XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
			XSSFSheet sheet = workbook.getSheetAt(0);

			List<List<String>> objects = Common.getSheetContent(sheet);

			int terupload = 0;
			Session session = HibernateUtil.currentNativeSession();
			for (List<String> strings : objects) {

				try {
					String id = strings.get(0);
					String soal = strings.get(1);

					if (soal != null) {
						soal = soal.trim();
					} else {
						soal = "";
					}

					if (soal != null && !soal.isEmpty() && !soal.equalsIgnoreCase("soal")
							&& !soal.equalsIgnoreCase("DAFTAR SOAL UJIAN")) {

						String benar = strings.get(2);
						String skor = strings.get(3);
						String skorSalah = strings.get(4);
						String benarDefault = strings.get(5);
						String[] betuls = benar.split(",");

						Boolean tampilPenjelasanSaatUjian = false;
						try {
							tampilPenjelasanSaatUjian = Boolean.parseBoolean(strings.get(17).trim());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:844");

						}

						String jenis = BankSoal.PILIHAN_GANDA;
						try {
							jenis = strings.get(18);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:851");

						}

						String no = "0";
						try {
							no = strings.get(19);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:858");

						}

						if (id != null) {
							id = org.apache.commons.lang3.StringUtils.replace(id, ".", "");
						}

						System.out.println("id " + id + ", soal " + soal);
						System.out.println("benar " + benar + ", skor " + skor + ", skorSalah " + skorSalah
								+ ", benarDefault " + benarDefault + ", jenis = " + jenis);

						BankSoal newBankSoal = (BankSoal) session.createCriteria(BankSoal.class)
								.add(Restrictions.eq("penjelasanBankSoal", penjelasanBankSoal))
								.add(Restrictions.or(
										id == null || !Common.isNumber(id) ? Restrictions.sqlRestriction("false")
												: Restrictions.eq("id", Long.parseLong(id.trim())),
										Restrictions.ilike("soal", soal, MatchMode.EXACT)))
								.setMaxResults(1).uniqueResult();

						if (newBankSoal == null) {
							newBankSoal = new BankSoal();
						}
						try {
							newBankSoal.setSkor(Double.parseDouble(skor.trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:883");

						}
						try {
							newBankSoal.setSkorSalah(Double.parseDouble(skorSalah.trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:888");

						}
						try {
							newBankSoal.setSkorDefault(Double.parseDouble(benarDefault.trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:893");

						}

						try {
							newBankSoal.setNomorUrut(Integer.parseInt(no));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:899");

						}

						newBankSoal.setJenis(jenis);
						newBankSoal.setKeterangan("");
						newBankSoal.setSoal(soal);
						newBankSoal.setPenjelasanBankSoal(penjelasanBankSoal);
						newBankSoal.setTampilPenjelasanSaatUjian(tampilPenjelasanSaatUjian);

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, newBankSoal);
						session.getTransaction().commit();

						int jumlahJawaban = 0;
						if (newBankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA)) {
							int j = 6;
							for (int i = ((int) 'A'); i <= ((int) 'J'); i++) {
								try {
									String huruf = ((char) i) + "";
									String jawaban = strings.get(j);
									if (jawaban != null && !jawaban.trim().equals("")) {
										BankSoalDetail bankSoalDetail = (BankSoalDetail) session
												.createCriteria(BankSoalDetail.class)
												.add(Restrictions.eq("bankSoal", newBankSoal))
												.add(Restrictions.eq("huruf", huruf)).setMaxResults(1).uniqueResult();
										if (bankSoalDetail == null) {
											bankSoalDetail = new BankSoalDetail();
										}

										bankSoalDetail.setBankSoal(newBankSoal);
										boolean betul = false;
										for (String s : betuls) {
											betul |= (s != null && s.trim().equalsIgnoreCase(huruf));
										}
										bankSoalDetail.setBetul(betul);
										bankSoalDetail.setEssay("");
										bankSoalDetail.setHuruf(huruf);
										bankSoalDetail.setJawaban(jawaban);
										bankSoalDetail.setKeterangan("");

										session.getTransaction().begin();
										Common.refreshSaveOrUpdate(session, bankSoalDetail);
										session.getTransaction().commit();
										jumlahJawaban++;
									}
									j++;
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:946");

								}
							}

						} else {
							BankSoalDetail bankSoalDetail = (BankSoalDetail) session
									.createCriteria(BankSoalDetail.class).add(Restrictions.eq("bankSoal", newBankSoal))
									.setMaxResults(1).uniqueResult();
							if (bankSoalDetail == null) {
								bankSoalDetail = new BankSoalDetail();
							}
							bankSoalDetail.setBankSoal(newBankSoal);
							bankSoalDetail.setBetul(true);
							bankSoalDetail.setEssay(benar);
							bankSoalDetail.setHuruf("");
							bankSoalDetail.setJawaban("");
							bankSoalDetail.setKeterangan("");

							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, bankSoalDetail);
							session.getTransaction().commit();
						}

						Integer count = ((Number) HibernateUtil.currentSession().createCriteria(BankSoalDetail.class)
								.add(Restrictions.eq("bankSoal", newBankSoal)).add(Restrictions.eq("betul", true))
								.setProjection(Projections.rowCount()).uniqueResult()).intValue();
						if (count > 1) {
							newBankSoal.setJenisPilihanGanda(BankSoal.COMBINATION_CHOICE);
						} else if (jumlahJawaban == 2) {
							newBankSoal.setJenisPilihanGanda(BankSoal.BENAR_SALAH);
						} else {
							newBankSoal.setJenisPilihanGanda(BankSoal.MULTIPLE_COICE);
						}

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, newBankSoal);
						session.getTransaction().commit();

						terupload++;
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailGrupSoalHelper.java:988");
				}

			}
			HibernateUtil.closeSession();

			MyMessageboxConfig.show("Upload soal telah selesai dilakukan, " + terupload + " terupload", "Pemberitahuan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							dataLoader.loadData(true);
						}
					});

		} else {
			MyMessageboxConfig.show(
					"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
							+ media,
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
		}
	}

	private void initSpreadsheet() throws Exception {
		DetailGrupSoalHelper.doDownload(penjelasanBankSoal,
				HibernateUtil.currentSession().createCriteria(BankSoal.class)
						.add(Restrictions.eq("penjelasanBankSoal", penjelasanBankSoal)).addOrder(Order.asc("nomorUrut"))
						.addOrder(Order.asc("id")));
	}

	private void uploadSoal(Media media, PenjelasanBankSoal penjelasanBankSoal) throws Exception {
		DetailGrupSoalHelper.doUpload(media, penjelasanBankSoal, this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void loadData(Object value) {

		List<BankSoal> bankSoals = HibernateUtil.currentSession().createCriteria(BankSoal.class)
				.add(Restrictions.eq("penjelasanBankSoal", penjelasanBankSoal)).addOrder(Order.asc("nomorUrut"))
				.addOrder(Order.asc("id"))
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_1 * (paging == null ? 0 : paging.getActivePage()))
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE_1).list();

		ListModel strset = new SimpleListModel(bankSoals);
		grid.setRowRenderer(new DetailUjianRenderer());
		grid.setModel(strset);
		grid.setSclass("fgrid");
		grid.setOddRowSclass("non-odd");

		try {
			paging.setPageSize(Common.ROWS_COUNT_ON_PAGE_1);
			paging.setMold("os");
			paging.setTotalSize(bankSoals.size());
			paging.setVisible(bankSoals.size() > Common.ROWS_COUNT_ON_PAGE_1);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:1043");

		}
	}

}
