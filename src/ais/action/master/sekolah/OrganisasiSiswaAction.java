package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.helper.OrganisasiSiswaPunyaSiswaHelper;
import ais.action.report.format1.sekolah.LaporanPerOrganisasiSiswa;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JabatanOrganisasiSiswa;
import ais.database.model.sekolah.OrganisasiSiswa;
import ais.database.model.sekolah.OrganisasiSiswaPunyaSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class OrganisasiSiswaAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Combobox searchsekolah;
	private Combobox searchyayasan;
	protected Textbox searchnamamhs;
	protected Textbox searchnim;
	protected AmbilDataGuruBanbox searchguru;

	private Textbox nama;
	private Textbox keterangan;
	private Combobox sekolah;
	private Combobox yayasan;

	private OrganisasiSiswa organisasiSiswa;
	private MyToolbarbuttonConfig add;
	private MyToolbarbuttonConfig uploadData;

	private Tabpanel jabatanOrganisasi;
	private Textbox namaEn;

	public void onJabatanOrganisasiSiswa(Event event) {
		if (jabatanOrganisasi.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jabatanOrganisasi);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/jabatan_organisasi_siswa.zul");
			iframe.setParent(window);
		}
	}

	public void onUploadData(Event event) throws Exception {

		final Tbmuser tbmuser = Common.getCurrentUser();

		ForwardEvent forwardEvent = (ForwardEvent) event;
		Media media = ((UploadEvent) forwardEvent.getOrigin()).getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
		if (media.getName().toLowerCase().endsWith("xlsx")) {

			InputStream inputStream = media.getStreamData();
			// System.out.println("media = " + media);
			final File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
			// System.out.println("file = " + file.getAbsolutePath());
			file.getParentFile().mkdirs();
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			int c;
			while ((c = inputStream.read()) != -1) {
				fileOutputStream.write(c);
			}
			fileOutputStream.close();
			inputStream.close();

			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data organisasiSiswa sedang berlangsung, harap menunggu.."));

			new Thread(new Runnable() {

				@Override
				public void run() {
					try {

					XSSFWorkbook workbook;
					try {
						workbook = new XSSFWorkbook(file.getAbsolutePath());

						for (XSSFSheet sheet : Common.getAllXSSFSheet(workbook)) {
							Session session = HibernateUtil.currentNativeSession();

							OrganisasiSiswa organisasiSiswa = (OrganisasiSiswa) session
									.createCriteria(OrganisasiSiswa.class)
									.add(Restrictions.ilike("kode", sheet.getSheetName().trim(), MatchMode.EXACT))
									.setMaxResults(1).uniqueResult();
							if (organisasiSiswa == null) {
								organisasiSiswa = new OrganisasiSiswa();
								organisasiSiswa.setNama(sheet.getSheetName().trim());
								organisasiSiswa.setKeterangan(sheet.getSheetName().trim());
								session.getTransaction().begin();
								session.save(organisasiSiswa);
								session.getTransaction().commit();
							}

							HibernateUtil.closeSession();

							int size = (sheet.getLastRowNum() + 1);
							for (int i = 0; i < (sheet.getLastRowNum() + 1); i++) {

								session = HibernateUtil.currentNativeSession();

								try {
									Siswa siswa = null;
									try {
										String nim = Common.getSheetContentAsString(sheet, 1, i);
										siswa = (Siswa) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
												.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();

										if (siswa == null) {
											nim = Common.getCellContent(Common.getCell(sheet, 1, i));
											siswa = (Siswa) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
													.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
										}

										if (siswa == null) {
											nim = Common.getCellContent(Common.getCell(sheet, 2, i));
											siswa = (Siswa) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
													.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
										}

										if (siswa == null) {
											nim = Common.getCellContent(Common.getCell(sheet, 3, i));
											siswa = (Siswa) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
													.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
										}

									} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/sekolah/OrganisasiSiswaAction.java:213");

									}

									if (siswa == null) {
										continue;
									}

									Date mulai = Common.getSheetContentAsDate(sheet, 3, i);
									Date sampai = Common.getSheetContentAsDate(sheet, 4, i);
									JabatanOrganisasiSiswa jabatanOrganisasiSiswa = (JabatanOrganisasiSiswa) Common
											.getSheetContentAsObject(sheet, 5, i, JabatanOrganisasiSiswa.class);
									String keterangan = Common.getSheetContentAsString(sheet, 6, i);

									Boolean persetujuan = Common.getSheetContentAsBoolean(sheet, 8, i);

									OrganisasiSiswaPunyaSiswa organisasiSiswaPunyaSiswa = (OrganisasiSiswaPunyaSiswa) session
											.createCriteria(OrganisasiSiswaPunyaSiswa.class)
											.add(Restrictions.eq("siswa", siswa))
											.add(Restrictions.eq("organisasiSiswa", organisasiSiswa)).setMaxResults(1)
											.uniqueResult();

									if (organisasiSiswaPunyaSiswa == null) {
										organisasiSiswaPunyaSiswa = new OrganisasiSiswaPunyaSiswa();
									}
									organisasiSiswaPunyaSiswa.setSiswa(siswa);
									organisasiSiswaPunyaSiswa.setOrganisasiSiswa(organisasiSiswa);
									organisasiSiswaPunyaSiswa.setOleh(tbmuser.getUserId());
									organisasiSiswaPunyaSiswa.setTbmuser(tbmuser);
									organisasiSiswaPunyaSiswa
											.setDiubahDari(OrganisasiSiswaAction.class.getSimpleName());

									organisasiSiswaPunyaSiswa.setMulai(mulai);
									organisasiSiswaPunyaSiswa.setSampai(sampai);
									organisasiSiswaPunyaSiswa.setJabatanOrganisasiSiswa(jabatanOrganisasiSiswa);
									organisasiSiswaPunyaSiswa.setKeterangan(keterangan);
									organisasiSiswaPunyaSiswa.setPersetujuan(persetujuan);

									session.getTransaction().begin();
									session.saveOrUpdate(organisasiSiswaPunyaSiswa);
									session.getTransaction().commit();

									HibernateUtil.closeSession();

									label.setValue(
											"Upload siswa " + siswa + " di organisasiSiswa " + organisasiSiswa.getNama()
													+ ".. " + Common.numberFormat.get().format(i * 100.0 / size) + " %");

								} catch (Exception e1) {
									// TODO Auto-generated catch block

									HibernateUtil.closeSession();

									e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/OrganisasiSiswaAction.java:266");
								}
							}

						}

					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/OrganisasiSiswaAction.java:274");
					}

					label.setValue("");
									} finally {
						ais.database.hibernate.HibernateUtil.closeSession();
					}
				}
			}).start();

			final Timer timer = new Timer(500);
			timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			timer.setRepeats(true);
			timer.addEventListener("onTimer", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Clients.showBusy(label.getValue());
					if (label.getValue().isEmpty()) {
						Clients.clearBusy();
						MyMessageboxConfig.show("Update data organisasi berhasil dilakukan", "Pemberitahuan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						timer.detach();
					}

				}
			});
			timer.start();

		} else {
			MyMessageboxConfig.show(
					"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
							+ media,
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		Tbmuser tbmuser = Common.getCurrentUser();
		jabatanOrganisasi.getLinkedTab()
				.setVisible(tbmuser != null && tbmuser.getSiswa() == null && tbmuser.ambilGuru() == null);

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		searchguru.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = cetakDataCustomButton("Download Data Siswa", "/img/print.png");
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		String[] contents = new String[] { "id", "nama", "namaEn", "yayasan", "sekolah", "keterangan" };
		cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, OrganisasiSiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible())); }
		Common.appendKeToolbar(upload, add, comp);

		if (uploadData != null) { uploadData.setVisible((add != null && add.isVisible())); }

		MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Organisasi Siswa", "/img/print.png");
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				LaporanPerOrganisasiSiswa laporan = new LaporanPerOrganisasiSiswa();
				laporan.setTitle("Organisasi Siswa");
				laporan.setClosable(true);
				laporan.setHeight("95%");
				laporan.setWidth("90%");
				laporan.setParent(page.getFirstRoot());
				laporan.onModal();
			}
		});
		if (cetak != null) { cetak.setParent(add.getParent()); }

	        FilterLanjutHelper.setup(comp);
}

	public MyToolbarbuttonConfig cetakDataCustomButton(String buttonLabel, String buttonImage) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				Clients.showBusy(label.getValue());

				final String filename = Sessions.getCurrent().getWebApp()
						.getRealPath("/tmp/cetak_data_"
								+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
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

							Clients.showBusy(label.getValue());
							System.out.println("label " + label.getValue());

							if (label.getValue().trim().equalsIgnoreCase("-")) {
								Clients.clearBusy();
								timer.detach();
							} else if (label.getValue().isEmpty()) {

								Center center = new Center();
								final MyWindow window = new MyWindow("Cetak Data", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("97%");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								System.out.println("loading file " + file.getAbsolutePath());
								Common.clear(center);
								Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
				Common.clear(center);spreadsheet.setParent(center);
								spreadsheet.setWidth("100%");
								spreadsheet.setHeight("100%");
								spreadsheet.setSrc("../../tmp/" + file.getName());
								spreadsheet.setMaxrows(intbox.getValue() + 1);
								spreadsheet.setMaxcolumns(9);
								ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

								South south = new South();
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								// toolbar.setHeight("25px");
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
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
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
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

					Clients.showBusy(label.getValue());
					final List<OrganisasiSiswa> organisasiSiswaes = initCriteria(true).list();
					new Thread(new Runnable() {

						@Override
						public void run() {

							try {

								Session session = HibernateUtil.currentSession();

								XSSFWorkbook workbook = new XSSFWorkbook();
								XSSFFont hlink_font = workbook.createFont();
								hlink_font.setUnderline(XSSFFont.U_SINGLE);
								hlink_font.setColor(new XSSFColor(Color.BLUE));

								final XSSFCellStyle hlink_style = workbook.createCellStyle();
								hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
								hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
								hlink_style.setFont(hlink_font);

								for (OrganisasiSiswa organisasiSiswa : organisasiSiswaes) {
									List<OrganisasiSiswaPunyaSiswa> data = session
											.createCriteria(OrganisasiSiswaPunyaSiswa.class)
											.add(Restrictions.eq("organisasiSiswa", organisasiSiswa))
											.createAlias("siswa", "siswa").addOrder(Order.asc("siswa.nim"))
											.setMaxResults(1048576).list();

									if (!data.isEmpty()) {
										intbox.setValue(data.size());
										System.out.println("data = " + data.size());

										XSSFSheet sheet = workbook.createSheet(organisasiSiswa.getKode());
										sheet.setDefaultColumnWidth(20);
										int rowIndex = 0;

										XSSFRow rowhead = sheet.createRow((short) 0);
										rowhead.createCell(0).setCellValue("No.");
										rowhead.createCell(1).setCellValue("NIM");
										rowhead.createCell(2).setCellValue("Nama");
										rowhead.createCell(3).setCellValue("Mulai");
										rowhead.createCell(4).setCellValue("Sampai");
										rowhead.createCell(5).setCellValue("Jabatan");
										rowhead.createCell(6).setCellValue("Keterangan");
										rowhead.createCell(7).setCellValue("SK");
										rowhead.createCell(8).setCellValue("Persetujuan");

										for (OrganisasiSiswaPunyaSiswa o : data) {
											try {
												rowIndex++;
												if (o == null) {
													continue;
												}
												label.setValue("Sedang memproses data " + o.toString() + " ("
														+ Common.numberFormat.get().format(rowIndex * 100.0 / data.size())
														+ " %)");

												XSSFRow row = sheet.createRow(rowIndex);

												row.createCell(0).setCellValue(rowIndex);
												row.createCell(1).setCellValue(o.getSiswa().getNim());
												row.createCell(2).setCellValue(o.getSiswa().getNama());
												row.createCell(3).setCellValue(o.getMulai() == null ? ""
														: Common.dateFormat1.get().format(o.getMulai()));
												row.createCell(4).setCellValue(o.getSampai() == null ? ""
														: Common.dateFormat1.get().format(o.getSampai()));
												row.createCell(5)
														.setCellValue(o.getJabatanOrganisasiSiswa() == null ? ""
																: o.getJabatanOrganisasiSiswa().getNama());
												row.createCell(6).setCellValue(o.getKeterangan());

												LampiranLain lam = LampiranLain.ambil(o.getId(),
														OrganisasiSiswaPunyaSiswa.class.getName());

												XSSFCell cell = row.createCell(7);

												if (lam != null) {

													String nama = lam.getNama();

													cell.setCellStyle(hlink_style);
													cell.setCellValue(nama);
													String url = CommonMedia.getFile(lam.getId(),
															LampiranLain.class.getName());
													XSSFHyperlink link = row.getSheet().getWorkbook()
															.getCreationHelper().createHyperlink(Hyperlink.LINK_URL);
													link.setAddress(url);
													cell.setHyperlink(link);
												}

												row.createCell(8).setCellValue(o.getPersetujuan());

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}
										}

										data.clear();
										data = null;
									}
								}

								try {
									FileOutputStream fileOut = new FileOutputStream(filename);
									workbook.write(fileOut);
									fileOut.close();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									Common.tampilErrorJikaAdmin(e);
								}
								System.out.println("Your excel file has been generated! ");

								label.setValue("");
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								label.setValue("-");
							}

						}
					}).start();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		return toolbarbutton;
	}

	class OrganisasiSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final OrganisasiSiswa organisasiSiswa = (OrganisasiSiswa) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Common.clear(detail);
					if (detail.isOpen()) {
						OrganisasiSiswaPunyaSiswaHelper detailperkuliahanHelper = new OrganisasiSiswaPunyaSiswaHelper();
						detailperkuliahanHelper.display(organisasiSiswa, detail, addWindow);
					}
				}
			});

			new Label(organisasiSiswa.getKode()).setParent(arg0);
			Vbox a;
			(a = RevisiHelper.createNewRevisi(OrganisasiSiswa.class, organisasiSiswa, organisasiSiswa.getNama()))
					.setParent(arg0);
			new Label(organisasiSiswa.getNamaEn()).setParent(a);

			new Label(organisasiSiswa.getYayasan() == null ? "Semua" : organisasiSiswa.getYayasan().getNama())
					.setParent(arg0);
			new Label(organisasiSiswa.getSekolah() == null ? "Semua" : organisasiSiswa.getSekolah().getNama())
					.setParent(arg0);

			new Label(organisasiSiswa.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			// button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(organisasiSiswa);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			// button.setVisible(delete);
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
											Common.refreshDelete(organisasiSiswa);
											onSearchDefault(event);
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
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new OrganisasiSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(OrganisasiSiswa organisasiSiswa) {
		this.organisasiSiswa = organisasiSiswa;
		addWindow.setTitle(organisasiSiswa.getId() == null ? "Tambah Organisasi Intra Kampus" : "Ubah Organisasi Intra Kampus");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Organisasi"));
		row.appendChild(nama = new Textbox(organisasiSiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Organisasi (dalam bhs inggris)"));
		row.appendChild(namaEn = new Textbox(organisasiSiswa.getNamaEn()));
		namaEn.setWidth("90%");

		Tbmuser tbmuser = Common.getCurrentUser();
		Common.initYayasanDanSekolahDanSemua(yayasan = new Combobox(), sekolah = new Combobox(), null, null);
		if (organisasiSiswa.getYayasan() == null && tbmuser.ambilYayasan() != null) {
			organisasiSiswa.setYayasan(tbmuser.ambilYayasan());
		}
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, organisasiSiswa.getYayasan());
		yayasan.setWidth("90%");

		if (yayasan.getSelectedItem() != null && yayasan.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(sekolah, new String[] { "nama", "kodeEpsbed" }, "jenjang", Sekolah.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("yayasan", yayasan, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(sekolah);
		sekolah.setWidth("90%");
		Common.pilihSekolah(sekolah, organisasiSiswa.getSekolah());

		if (organisasiSiswa.getSekolah() == null) {
			if (tbmuser.ambilSekolah() != null
					|| (tbmuser.getSiswa() != null && tbmuser.getSiswa().getSekolah() != null)) {
				Common.pilihSekolah(sekolah,
						tbmuser == null || tbmuser.ambilSekolah() == null ? tbmuser.getSiswa().getSekolah()
								: tbmuser.ambilSekolah());
				sekolah.setDisabled(true);
			} else {
				sekolah.setDisabled(false);
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(organisasiSiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Organisasi harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaOrganisasiSiswa();
		if (i) {
			MyMessageboxConfig.show("Nama Organisasi sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (organisasiSiswa.getId() != null) {
			organisasiSiswa = (OrganisasiSiswa) session.load(OrganisasiSiswa.class, organisasiSiswa.getId());

		}

		organisasiSiswa.setNama(nama.getValue());
		organisasiSiswa.setNamaEn(namaEn.getValue());
		organisasiSiswa.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null ? null
						: yayasan.getSelectedItem().getValue()));
		organisasiSiswa.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null ? null
						: sekolah.getSelectedItem().getValue()));

		organisasiSiswa.setKeterangan(keterangan.getValue());

		Common.refreshUpdate(session, organisasiSiswa);

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Criterion criterionMhs = Restrictions.sqlRestriction("true");
		if (!searchnim.getValue().trim().isEmpty() || !searchnamamhs.getValue().trim().isEmpty()) {
			String sql = "this_.id in (select sekolah.organisasi_siswa from sekolah.organisasi_siswa_punya_siswa a inner join siswa b on (a.siswa = b.id) where sekolah.organisasi_siswa is not null and b.nama ilike '%"
					+ searchnamamhs.getValue().trim() + "%' and b.nim ilike '%" + searchnim.getValue().trim()
					+ "%' group by sekolah.organisasi_siswa)";
			criterionMhs = Restrictions.sqlRestriction(sql);
		}

		Criterion criterionGuruPa = Restrictions.sqlRestriction("true");
		if (searchguru != null && searchguru.getAttribute("guru") != null) {
			Guru dsn = (Guru) searchguru.getAttribute("guru");
			String sql = "this_.id in (select sekolah.organisasi_siswa from sekolah.organisasi_siswa_punya_siswa a inner join siswa b on (a.siswa = b.id) where sekolah.organisasi_siswa is not null and b.guru = "
					+ dsn.getId() + " group by sekolah.organisasi_siswa)";
			criterionGuruPa = Restrictions.sqlRestriction(sql);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(OrganisasiSiswa.class);

		if (order)
			criteria.addOrder(Order.desc("id")); // pengajuan terkini di atas
		criteria.add(criterionMhs).add(criterionGuruPa)
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))
				.add(CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<OrganisasiSiswa> organisasiSiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(organisasiSiswa);
		grid.setRowRenderer(new OrganisasiSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaOrganisasiSiswa() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(OrganisasiSiswa.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.organisasiSiswa.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.organisasiSiswa.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
