package ais.action.master;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.TreeSet;

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
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailKelompokKegiatanKedosenan;
import ais.database.model.JabatanKegiatanKedosenan;
import ais.database.model.ParameterUmum;
import ais.database.model.SkalaKegiatanKedosenan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class NilaiKegiatanKedosenanAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private MyToolbarbuttonConfig find;

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

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig download = new MyToolbarbuttonConfig("Download", "/img/excel.png");
		download.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				final String filename = Sessions.getCurrent().getWebApp()
						.getRealPath("/tmp/cetak_data_"
								+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
								+ ".xlsx");
				final File file;
				(file = new File(filename)).createNewFile();

				XSSFWorkbook workbook = new XSSFWorkbook();
				XSSFSheet sheet = workbook.createSheet("Angka Kredit");
				sheet.setDefaultColumnWidth(20);

				XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
				lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				lockedNumericStyle.setLocked(true);

				XSSFCellStyle notLocked = workbook.createCellStyle();
				notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

				XSSFRow rowhead = sheet.createRow((short) 0);
				XSSFCell cellHead = rowhead.createCell(0);
				cellHead.setCellStyle(notLocked);
				cellHead.setCellValue("ID");
				cellHead = rowhead.createCell(1);
				cellHead.setCellStyle(notLocked);
				cellHead.setCellValue("Nama Aspek");
				cellHead = rowhead.createCell(2);
				cellHead.setCellStyle(notLocked);
				cellHead.setCellValue("Rincian Aspek");
				cellHead = rowhead.createCell(3);
				cellHead.setCellStyle(notLocked);
				cellHead.setCellValue("Jabatan/Status");

				Session session = HibernateUtil.currentSession();
				List<DetailKelompokKegiatanKedosenan> detailKelompokKegiatanKedosenans = initCriteria(true).list();
				List<SkalaKegiatanKedosenan> kegiatanKedosenans = session.createCriteria(SkalaKegiatanKedosenan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("nomorUrut")).list();

				int colIndex = 3;
				for (SkalaKegiatanKedosenan skalaKegiatanKedosenan : kegiatanKedosenans) {
					colIndex++;
					XSSFCell cell = rowhead.createCell(colIndex);
					cell.setCellStyle(notLocked);
					cell.setCellValue(skalaKegiatanKedosenan.getId() + "-" + skalaKegiatanKedosenan.getNama());
				}

				int rowIndex = 0;

				for (DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan : detailKelompokKegiatanKedosenans) {

					TreeSet<JabatanKegiatanKedosenan> jabatanKegiatanKedosenans = new TreeSet<JabatanKegiatanKedosenan>(
							detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans());

					if (!jabatanKegiatanKedosenans.isEmpty()) {
						for (JabatanKegiatanKedosenan jabatanKegiatanKedosenan : jabatanKegiatanKedosenans) {
							rowIndex++;
							if (detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans()
									.contains(jabatanKegiatanKedosenan)) {
								XSSFRow hssfRow = sheet.createRow(rowIndex);
								XSSFCell cell = hssfRow.createCell(0);
								cell.setCellStyle(notLocked);
								cell.setCellValue(detailKelompokKegiatanKedosenan.getId());
								cell = hssfRow.createCell(1);
								cell.setCellStyle(notLocked);
								cell.setCellValue(
										detailKelompokKegiatanKedosenan.getKelompokKegiatanKedosenan().getNama());
								cell = hssfRow.createCell(2);
								cell.setCellStyle(notLocked);
								cell.setCellValue(detailKelompokKegiatanKedosenan.getNama());
								cell = hssfRow.createCell(3);
								cell.setCellStyle(notLocked);
								cell.setCellValue(
										jabatanKegiatanKedosenan.getId() + "-" + jabatanKegiatanKedosenan.getNama());

								colIndex = 3;
								for (SkalaKegiatanKedosenan skalaKegiatanKedosenan : kegiatanKedosenans) {
									colIndex++;
									if (detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans()
											.contains(skalaKegiatanKedosenan)) {
										String kodeUnik = "pengaturan_beban_sks_kegiatan_dosen_"
												+ detailKelompokKegiatanKedosenan.getKelompokKegiatanKedosenan().getId()
												+ "_" + detailKelompokKegiatanKedosenan.getId() + "_"
												+ skalaKegiatanKedosenan.getId() + "_"
												+ (jabatanKegiatanKedosenan == null ? ""
														: jabatanKegiatanKedosenan.getId());

										ParameterUmum konfigurasi = Common.getParameterUmum(kodeUnik, "0.0");

										XSSFCell cellRow = hssfRow.createCell(colIndex);
										cellRow.setCellStyle(lockedNumericStyle);
										cellRow.setCellValue(konfigurasi.getNilai());
									}
								}
							}
						}

					} else {
						rowIndex++;
						XSSFRow hssfRow = sheet.createRow(rowIndex);
						XSSFCell cell = hssfRow.createCell(0);
						cell.setCellStyle(notLocked);
						cell.setCellValue(detailKelompokKegiatanKedosenan.getId());
						cell = hssfRow.createCell(1);
						cell.setCellStyle(notLocked);
						cell.setCellValue(detailKelompokKegiatanKedosenan.getKelompokKegiatanKedosenan().getNama());
						cell = hssfRow.createCell(2);
						cell.setCellStyle(notLocked);
						cell.setCellValue(detailKelompokKegiatanKedosenan.getNama());
						cell = hssfRow.createCell(3);
						cell.setCellValue("");

						colIndex = 3;
						for (SkalaKegiatanKedosenan skalaKegiatanKedosenan : kegiatanKedosenans) {
							colIndex++;
							if (detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans()
									.contains(skalaKegiatanKedosenan)) {
								String kodeUnik = "pengaturan_beban_sks_kegiatan_dosen_"
										+ detailKelompokKegiatanKedosenan.getKelompokKegiatanKedosenan().getId() + "_"
										+ detailKelompokKegiatanKedosenan.getId() + "_" + skalaKegiatanKedosenan.getId()
										+ "_";

								ParameterUmum konfigurasi = Common.getParameterUmum(kodeUnik, "0.0");
								XSSFCell cellRow = hssfRow.createCell(colIndex);
								cellRow.setCellStyle(lockedNumericStyle);
								cellRow.setCellValue(konfigurasi.getNilai());
							}
						}
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

				try {
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.getName());
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		});
		Common.appendKeToolbar(download, find, comp);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		if (upload != null) { upload.setUpload(Common.ukuranFileUpload()); }
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							final Label peringatan = new Label("");

							final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
							Clients.showBusy(label.getValue());
							final Timer timer = new Timer(200);
							timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							timer.setRepeats(true);
							timer.addEventListener("onTimer", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Clients.showBusy(label.getValue());
									if (label.getValue().isEmpty()) {
										System.out.println("loading file " + file.getAbsolutePath());
										MyMessageboxConfig.show("Upload data berhasil dilakukan."
												+ (peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()),
												"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
												new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														onSearchDefault(arg0);
													}
												});
										Clients.clearBusy();
										timer.detach();
									}

								}
							});
							timer.start();

							new Thread(new Runnable() {

								@Override
								public void run() {
									try {

									try {

										XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
										XSSFSheet sheet = workbook.getSheetAt(0);

										Session session = HibernateUtil.currentNativeSession();

										int columnCount = sheet.getRow(0).getLastCellNum();
										int rowCount = (sheet.getLastRowNum() + 1);
										for (int i = 1; i < rowCount; i++) {
											try {

												DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan = (DetailKelompokKegiatanKedosenan) Common
														.getSheetContentAsObject(sheet, 0, i,
																DetailKelompokKegiatanKedosenan.class);

												if (detailKelompokKegiatanKedosenan != null) {

													JabatanKegiatanKedosenan jabatanKegiatanKedosenan = (JabatanKegiatanKedosenan) Common
															.getSheetContentAsObject(sheet, 3, i,
																	JabatanKegiatanKedosenan.class);

													for (int j = 1; j < columnCount; j++) {
														try {
															SkalaKegiatanKedosenan skalaKegiatanKedosenan = (SkalaKegiatanKedosenan) Common
																	.getSheetContentAsObject(sheet, j, 0,
																			SkalaKegiatanKedosenan.class);
															if (skalaKegiatanKedosenan != null) {

																Double nilai = Common.getSheetContentAsDouble(sheet, j,
																		i);
																if (nilai != null && nilai > 0.1) {

																	String kodeUnik = "pengaturan_beban_sks_kegiatan_dosen_"
																			+ detailKelompokKegiatanKedosenan
																					.getKelompokKegiatanKedosenan()
																					.getId()
																			+ "_"
																			+ detailKelompokKegiatanKedosenan.getId()
																			+ "_" + skalaKegiatanKedosenan.getId() + "_"
																			+ (jabatanKegiatanKedosenan == null ? ""
																					: jabatanKegiatanKedosenan.getId());

																	ParameterUmum konfigurasi = Common
																			.getParameterUmum(kodeUnik, "0.0");
																	konfigurasi.setNilai(nilai.toString());

																	session.getTransaction().begin();
																	Common.refreshSaveOrUpdate(session, konfigurasi);
																	session.getTransaction().commit();
																}

															}
														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);
														}

													}
												}
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}

										}
									} catch (Exception e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/NilaiKegiatanKedosenanAction.java:385");
									}

									HibernateUtil.closeSession();

									label.setValue("");
																	} finally {
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}).start();

						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		Common.appendKeToolbar(upload, find, comp);
	}

	class NilaiKegiatanKedosenanRenderer extends ais.ui.util.MyRowRenderer {

		private void tampilRow(Rows rows, TreeSet<SkalaKegiatanKedosenan> skalaKegiatanKedosenans,
				DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan,
				JabatanKegiatanKedosenan jabatanKegiatanKedosenan) {

			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			row.appendChild(
					new MyLabelKecil(jabatanKegiatanKedosenan == null ? "" : jabatanKegiatanKedosenan.getNama()));
			for (SkalaKegiatanKedosenan skalaKegiatanKedosenan : skalaKegiatanKedosenans) {
				Hbox hbox = new Hbox();
				row.appendChild(hbox);
				hbox.appendChild(new MyLabelKecil(skalaKegiatanKedosenan.getNama()));

				String kodeUnik = "pengaturan_beban_sks_kegiatan_dosen_"
						+ detailKelompokKegiatanKedosenan.getKelompokKegiatanKedosenan().getId() + "_"
						+ detailKelompokKegiatanKedosenan.getId() + "_" + skalaKegiatanKedosenan.getId() + "_"
						+ (jabatanKegiatanKedosenan == null ? "" : jabatanKegiatanKedosenan.getId());

				final ParameterUmum konfigurasi = Common.getParameterUmum(kodeUnik, "0.0");

				Double n = 0.0;
				try {
					n = Double.parseDouble(konfigurasi.getNilai());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NilaiKegiatanKedosenanAction.java:444");
					// TODO: handle exception
				}

				final MyDoublebox nilai = new MyDoublebox(n);
				nilai.setWidth("90%");

				hbox.appendChild(nilai);

				nilai.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						konfigurasi.setNilai(nilai.getValue() == null ? "0.0" : nilai.getValue().toString());
						Common.refreshUpdate(konfigurasi);
					}
				});
			}
		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub

			final DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan = (DetailKelompokKegiatanKedosenan) arg1;

			new Label(detailKelompokKegiatanKedosenan.getKelompokKegiatanKedosenan().getNama()).setParent(arg0);
			new Label(detailKelompokKegiatanKedosenan.getNama()).setParent(arg0);

			TreeSet<SkalaKegiatanKedosenan> skalaKegiatanKedosenans = new TreeSet<SkalaKegiatanKedosenan>(
					detailKelompokKegiatanKedosenan.getSkalaKegiatanKedosenans());

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(arg0);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Rows rows = new Rows();
			rows.setParent(grid);

			if (!detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans().isEmpty()) {
				TreeSet<JabatanKegiatanKedosenan> jabatanKegiatanKedosenans = new TreeSet<JabatanKegiatanKedosenan>(
						detailKelompokKegiatanKedosenan.getJabatanKegiatanKedosenans());

				for (JabatanKegiatanKedosenan jabatanKegiatanKedosenan : jabatanKegiatanKedosenans) {
					tampilRow(rows, skalaKegiatanKedosenans, detailKelompokKegiatanKedosenan, jabatanKegiatanKedosenan);
				}

				jabatanKegiatanKedosenans = null;

			} else {
				tampilRow(rows, skalaKegiatanKedosenans, detailKelompokKegiatanKedosenan, null);
			}
			skalaKegiatanKedosenans = null;
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DetailKelompokKegiatanKedosenan.class)
				.createAlias("kelompokKegiatanKedosenan", "kelompokKegiatanKedosenan");

		if (order)
			criteria.addOrder(Order.asc("kelompokKegiatanKedosenan.nomorUrut")).addOrder(Order.asc("nomorUrut"))
					.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DetailKelompokKegiatanKedosenan> nilaiKegiatanKedosenan = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(nilaiKegiatanKedosenan);
		grid.setRowRenderer(new NilaiKegiatanKedosenanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
