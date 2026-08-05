package ais.action.master;


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

import ais.action.master.helper.OrganisasiDosenPunyaDosenHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.format1.akademik.LaporanPerOrganisasiDosen;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.JabatanOrganisasiDosen;
import ais.database.model.Jurusan;
import ais.database.model.LevelOrganisasiDosen;
import ais.database.model.OrganisasiDosen;
import ais.database.model.OrganisasiDosenPunyaDosen;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class OrganisasiDosenAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Combobox searchjurusan;
	private Combobox searchfakultas;
	protected Textbox searchnamamhs;
	protected Textbox searchnidn;

	private Textbox nama;
	private Textbox keterangan;
	private Combobox jurusan;
	private Combobox levelOrganisasiDosen;
	private Combobox fakultas;

	// private boolean edit = false;
	// private boolean delete = false;

	private OrganisasiDosen organisasiDosen;
	private MyToolbarbuttonConfig add;

	private Tabpanel jabatanOrganisasi;

	public void onJabatanOrganisasiDosen(Event event) {
		if (jabatanOrganisasi.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jabatanOrganisasi);
			MyInclude iframe = new MyInclude("/pages/master/jabatan_organisasi_dosen.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel level;
	private Textbox namaEn;

	public void onLevelOrganisasiDosen(Event event) {
		if (level.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(level);
			MyInclude iframe = new MyInclude("/pages/master/level_organisasi_dosen.zul");
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

			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data organisasiDosen sedang berlangsung, harap menunggu.."));

			new Thread(new Runnable() {

				@Override
				public void run() {
					try {

					XSSFWorkbook workbook;
					try {
						workbook = new XSSFWorkbook(file.getAbsolutePath());

						for (XSSFSheet sheet : Common.getAllXSSFSheet(workbook)) {
							Session session = HibernateUtil.currentNativeSession();

							OrganisasiDosen organisasiDosen = (OrganisasiDosen) session
									.createCriteria(OrganisasiDosen.class)
									.add(Restrictions.ilike("kode", sheet.getSheetName().trim(), MatchMode.EXACT))
									.setMaxResults(1).uniqueResult();
							if (organisasiDosen == null) {
								organisasiDosen = new OrganisasiDosen();
								organisasiDosen.setNama(sheet.getSheetName().trim());
								organisasiDosen.setKeterangan(sheet.getSheetName().trim());
								session.getTransaction().begin();
								session.save(organisasiDosen);
								session.getTransaction().commit();
							}

							HibernateUtil.closeSession();

							int size = (sheet.getLastRowNum() + 1);
							for (int i = 0; i < (sheet.getLastRowNum() + 1); i++) {

								session = HibernateUtil.currentNativeSession();

								try {
									Dosen dosen = null;
									try {
										String nidn = Common.getSheetContentAsString(sheet, 1, i);
										dosen = (Dosen) session.createCriteria(Dosen.class)
												.add(Restrictions.eq("nidn", nidn)).setMaxResults(1).uniqueResult();

										if (dosen == null) {
											nidn = Common.getCellContent(Common.getCell(sheet,1, i));
											dosen = (Dosen) session.createCriteria(Dosen.class)
													.add(Restrictions.eq("nidn", nidn)).setMaxResults(1).uniqueResult();
										}

										if (dosen == null) {
											nidn = Common.getCellContent(Common.getCell(sheet,2, i));
											dosen = (Dosen) session.createCriteria(Dosen.class)
													.add(Restrictions.eq("nidn", nidn)).setMaxResults(1).uniqueResult();
										}

										if (dosen == null) {
											nidn = Common.getCellContent(Common.getCell(sheet,3, i));
											dosen = (Dosen) session.createCriteria(Dosen.class)
													.add(Restrictions.eq("nidn", nidn)).setMaxResults(1).uniqueResult();
										}

									} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/OrganisasiDosenAction.java:227");

									}

									if (dosen == null) {
										continue;
									}

									Date mulai = Common.getSheetContentAsDate(sheet, 3, i);
									Date sampai = Common.getSheetContentAsDate(sheet, 4, i);
									JabatanOrganisasiDosen jabatanOrganisasiDosen = (JabatanOrganisasiDosen) Common
											.getSheetContentAsObject(sheet, 5, i, JabatanOrganisasiDosen.class);
									String keterangan = Common.getSheetContentAsString(sheet, 6, i);

									Boolean persetujuan = Common.getSheetContentAsBoolean(sheet, 8, i);

									OrganisasiDosenPunyaDosen organisasiDosenPunyaDosen = (OrganisasiDosenPunyaDosen) session
											.createCriteria(OrganisasiDosenPunyaDosen.class)
											.add(Restrictions.eq("dosen", dosen))
											.add(Restrictions.eq("organisasiDosen", organisasiDosen)).setMaxResults(1)
											.uniqueResult();

									if (organisasiDosenPunyaDosen == null) {
										organisasiDosenPunyaDosen = new OrganisasiDosenPunyaDosen();
									}
									organisasiDosenPunyaDosen.setDosen(dosen);
									organisasiDosenPunyaDosen.setOrganisasiDosen(organisasiDosen);
									organisasiDosenPunyaDosen.setOleh(tbmuser.getUserId());
									organisasiDosenPunyaDosen.setTbmuser(tbmuser);
									organisasiDosenPunyaDosen
											.setDiubahDari(OrganisasiDosenAction.class.getSimpleName());

									organisasiDosenPunyaDosen.setMulai(mulai);
									organisasiDosenPunyaDosen.setSampai(sampai);
									organisasiDosenPunyaDosen.setJabatanOrganisasiDosen(jabatanOrganisasiDosen);
									organisasiDosenPunyaDosen.setKeterangan(keterangan);
									organisasiDosenPunyaDosen.setPersetujuan(persetujuan);

									session.getTransaction().begin();
									session.saveOrUpdate(organisasiDosenPunyaDosen);
									session.getTransaction().commit();

									HibernateUtil.closeSession();

									label.setValue(
											"Upload dosen " + dosen + " di organisasiDosen " + organisasiDosen.getNama()
													+ ".. " + Common.numberFormat.get().format(i * 100.0 / size) + " %");

								} catch (Exception e1) {
									// TODO Auto-generated catch block

									HibernateUtil.closeSession();

									e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/OrganisasiDosenAction.java:280");
								}
							}

						}

					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/OrganisasiDosenAction.java:288");
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

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(LevelOrganisasiDosen.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			LevelOrganisasiDosen levelOrganisasiDosen = new LevelOrganisasiDosen();
			levelOrganisasiDosen.setNama("Internasional");
			levelOrganisasiDosen.setKeterangan("Level Internasional");
			session.save(levelOrganisasiDosen);

			levelOrganisasiDosen = new LevelOrganisasiDosen();
			levelOrganisasiDosen.setNama("Nasional");
			levelOrganisasiDosen.setKeterangan("Level Nasional");
			session.save(levelOrganisasiDosen);

			levelOrganisasiDosen = new LevelOrganisasiDosen();
			levelOrganisasiDosen.setNama("Lokal");
			levelOrganisasiDosen.setKeterangan("Level Lokal");
			session.save(levelOrganisasiDosen);
		}

		count = ((Number) session.createCriteria(JabatanOrganisasiDosen.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			JabatanOrganisasiDosen jabatanOrganisasiDosen = new JabatanOrganisasiDosen();
			jabatanOrganisasiDosen.setNama("Ketua");
			jabatanOrganisasiDosen.setKeterangan("Ketua");
			session.save(jabatanOrganisasiDosen);

			jabatanOrganisasiDosen = new JabatanOrganisasiDosen();
			jabatanOrganisasiDosen.setNama("Pengurus");
			jabatanOrganisasiDosen.setKeterangan("Pengurus");
			session.save(jabatanOrganisasiDosen);

			jabatanOrganisasiDosen = new JabatanOrganisasiDosen();
			jabatanOrganisasiDosen.setNama("Anggota");
			jabatanOrganisasiDosen.setKeterangan("Anggota");
			session.save(jabatanOrganisasiDosen);
		}

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		if (add != null) { add.setTooltiptext("Tambah"); }

		// edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		// delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = cetakDataCustomButton("Download Data Dosen", "/img/print.png");
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		String[] contents = new String[] { "id", "nama", "namaEn", "fakultas", "jurusan", "keterangan" };
		cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, OrganisasiDosen.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible())); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Organisasi Dosen", "/img/print.png");
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				LaporanPerOrganisasiDosen laporan = new LaporanPerOrganisasiDosen();
				laporan.setTitle("Organisasi Dosen");
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
											Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
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
					final List<OrganisasiDosen> organisasiDosenes = initCriteria(true).list();
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

								for (OrganisasiDosen organisasiDosen : organisasiDosenes) {
									List<OrganisasiDosenPunyaDosen> data = session
											.createCriteria(OrganisasiDosenPunyaDosen.class)
											.add(Restrictions.eq("organisasiDosen", organisasiDosen))
											.createAlias("dosen", "dosen").addOrder(Order.asc("dosen.nidn"))
											.setMaxResults(1048576).list();

									if (!data.isEmpty()) {
										intbox.setValue(data.size());
										System.out.println("data = " + data.size());

										XSSFSheet sheet = workbook.createSheet(organisasiDosen.getKode());
										sheet.setDefaultColumnWidth(20);
										int rowIndex = 0;

										XSSFRow rowhead = sheet.createRow((short) 0);
										rowhead.createCell(0).setCellValue("No.");
										rowhead.createCell(1).setCellValue("NIDN");
										rowhead.createCell(2).setCellValue("Nama");
										rowhead.createCell(3).setCellValue("Mulai");
										rowhead.createCell(4).setCellValue("Sampai");
										rowhead.createCell(5).setCellValue("Jabatan");
										rowhead.createCell(6).setCellValue("Keterangan");
										rowhead.createCell(7).setCellValue("SK");
										rowhead.createCell(8).setCellValue("Persetujuan");

										for (OrganisasiDosenPunyaDosen o : data) {
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
												row.createCell(1).setCellValue(o.getDosen().getNidn());
												row.createCell(2).setCellValue(o.getDosen().getNama());
												row.createCell(3).setCellValue(o.getMulai() == null ? ""
														: Common.dateFormat1.get().format(o.getMulai()));
												row.createCell(4).setCellValue(o.getSampai() == null ? ""
														: Common.dateFormat1.get().format(o.getSampai()));
												row.createCell(5)
														.setCellValue(o.getJabatanOrganisasiDosen() == null ? ""
																: o.getJabatanOrganisasiDosen().getNama());
												row.createCell(6).setCellValue(o.getKeterangan());

												LampiranLain lam = LampiranLain.ambil(o.getId(),
														OrganisasiDosenPunyaDosen.class.getName());

												XSSFCell cell = row.createCell(7);

												if (lam != null) {

													String nama = lam.getNama();

													cell.setCellStyle(hlink_style);
													cell.setCellValue(nama);
													String url = CommonMedia.getFile(lam.getId(),
															LampiranLain.class.getName());
													XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper().createHyperlink(Hyperlink.LINK_URL);
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
								System.out.println(
										"Your excel file has been generated! " );

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

	class OrganisasiDosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final OrganisasiDosen organisasiDosen = (OrganisasiDosen) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Common.clear(detail);
					if (detail.isOpen()) {
						OrganisasiDosenPunyaDosenHelper detailperkuliahanHelper = new OrganisasiDosenPunyaDosenHelper();
						detailperkuliahanHelper.display(organisasiDosen, detail, addWindow);
					}
				}
			});

			new Label(organisasiDosen.getKode()).setParent(arg0);
			Vbox a;
			(a = RevisiHelper.createNewRevisi(OrganisasiDosen.class, organisasiDosen, organisasiDosen.getNama()))
					.setParent(arg0);
			new Label(organisasiDosen.getNamaEn()).setParent(a);

			new Label(organisasiDosen.getLevelOrganisasiDosen() == null ? ""
					: organisasiDosen.getLevelOrganisasiDosen().getNama()).setParent(arg0);

			new Label(organisasiDosen.getFakultas() == null ? "Semua" : organisasiDosen.getFakultas().getNama())
					.setParent(arg0);
			new Label(organisasiDosen.getJurusan() == null ? "Semua" : organisasiDosen.getJurusan().getNama())
					.setParent(arg0);

			new Label(organisasiDosen.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			// button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(organisasiDosen);
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
											Common.refreshDelete(organisasiDosen);
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
		init(new OrganisasiDosen());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(OrganisasiDosen organisasiDosen) {
		this.organisasiDosen = organisasiDosen;
		addWindow.setTitle(organisasiDosen.getId() == null ? "Tambah Organisasi Dosen" : "Ubah Organisasi Dosen");
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
		row.appendChild(nama = new Textbox(organisasiDosen.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Organisasi (dalam bhs inggris)"));
		row.appendChild(namaEn = new Textbox(organisasiDosen.getNamaEn()));
		namaEn.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tingkat / Level Organisasi Dosen"));
		row.appendChild(levelOrganisasiDosen = new Combobox());
		Common.insertCombo(levelOrganisasiDosen, "nama", LevelOrganisasiDosen.class);
		Common.selectComboItem(levelOrganisasiDosen, organisasiDosen.getLevelOrganisasiDosen());
		levelOrganisasiDosen.setWidth("90%");
		levelOrganisasiDosen.setReadonly(true);

		Tbmuser tbmuser = Common.getCurrentUser();
		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		if (organisasiDosen.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			organisasiDosen.setFakultas(tbmuser.ambilFakultas());
		}
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		Common.selectComboItem(fakultas, organisasiDosen.getFakultas());
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, organisasiDosen.getJurusan());

		if (organisasiDosen.getJurusan() == null) {
			if (tbmuser.ambilJurusan() != null
					|| (tbmuser.ambilDosen() != null && tbmuser.ambilDosen().getJurusan() != null)) {
				Common.pilihJurusan(jurusan,
						tbmuser == null || tbmuser.ambilJurusan() == null ? tbmuser.ambilDosen().getJurusan()
								: tbmuser.ambilJurusan());
				jurusan.setDisabled(true);
			} else {
				jurusan.setDisabled(false);
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(organisasiDosen.getKeterangan()));
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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Organisasi Dosen",
					"Kolom Nama Organisasi Dosen belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Organisasi Dosen.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (levelOrganisasiDosen.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Level / Tingkat Organisasi Dosen",
					"Kolom Level / Tingkat Organisasi Dosen belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Level / Tingkat Organisasi Dosen.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkNamaOrganisasiDosen();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Organisasi Dosen",
					"Nama Organisasi Dosen sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama organisasi dosen yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (organisasiDosen.getId() != null) {
			organisasiDosen = (OrganisasiDosen) session.load(OrganisasiDosen.class, organisasiDosen.getId());

		}

		organisasiDosen
				.setLevelOrganisasiDosen((LevelOrganisasiDosen) levelOrganisasiDosen.getSelectedItem().getValue());
		organisasiDosen.setNama(nama.getValue());
		organisasiDosen.setNamaEn(namaEn.getValue());

		organisasiDosen.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		organisasiDosen.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));

		organisasiDosen.setKeterangan(keterangan.getValue());

		Common.refreshUpdate(session, organisasiDosen);

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Criterion criterionMhs = Restrictions.sqlRestriction("true");
		if (!searchnidn.getValue().trim().isEmpty() || !searchnamamhs.getValue().trim().isEmpty()) {
			String sql = "this_.id in (select organisasi_dosen from organisasi_dosen_punya_dosen a inner join dosen b on (a.dosen = b.id) where organisasi_dosen is not null and b.nama ilike '%"
					+ searchnamamhs.getValue().trim() + "%' and b.nidn ilike '%" + searchnidn.getValue().trim()
					+ "%' group by organisasi_dosen)";
			criterionMhs = Restrictions.sqlRestriction(sql);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(OrganisasiDosen.class);

		if (order)
			criteria.addOrder(Order.desc("id")); // pengajuan terkini di atas
		criteria.add(criterionMhs)
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<OrganisasiDosen> organisasiDosen = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(organisasiDosen);
		grid.setRowRenderer(new OrganisasiDosenRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaOrganisasiDosen() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(OrganisasiDosen.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.organisasiDosen.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.organisasiDosen.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
