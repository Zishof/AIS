package ais.action.master;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Konfigurasi;
import ais.database.model.Matakuliah;
import ais.database.model.MatakuliahEkivalen;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class MatakuliahEkivalenAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchekivalen;

	private AmbilDataMatakuliahBanbox ekivalen;
	private AmbilDataMatakuliahBanbox matakuliah;
	private Textbox khususUntukNim;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private MatakuliahEkivalen matakuliahEkivalen;
	private MyToolbarbuttonConfig add;
	private AmbilDataMatakuliahBanbox matakuliahEkivalen2;
	private AmbilDataMatakuliahBanbox matakuliahEkivalen5;
	private AmbilDataMatakuliahBanbox matakuliahEkivalen4;
	private AmbilDataMatakuliahBanbox matakuliahEkivalen3;

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

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "matakuliah", "matakuliahEkivalen", "khususUntukNim", "aktif",
				"keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, MatakuliahEkivalen.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Tbmuser tbmuser = Common.getCurrentUser();
		MyToolbarbuttonConfig bersihkan = bersihkanKrsMahasiswaDouble("Lihat data KRS Double karena Ekivalen",
				"/img/excel.png");
		bersihkan.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null
				&& Common.bolehKonfigurasi("tampilkan_tombol_bersihkan_krs_double"));

		Common.appendKeToolbar(bersihkan, add, comp);
	}

	public MyToolbarbuttonConfig bersihkanKrsMahasiswaDouble(String buttonLabel, String buttonImage) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Pilih Tahun Akademik dan Semester", "none", true);
				window.setParent(page.getFirstRoot());
				window.setHeight("300px");
				window.setWidth("600px");
				final Combobox tahunAkademik = new Combobox();
				Common.generateTahunAjaranDanSemua(tahunAkademik);
				final Combobox genapGanjil = new Combobox();
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel(Perkuliahan.GENAP);
				comboitem.setValue(Perkuliahan.GENAP);
				genapGanjil.appendChild(comboitem);
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(Perkuliahan.GANJIL);
				comboitem.setValue(Perkuliahan.GANJIL);
				genapGanjil.appendChild(comboitem);

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Semua");
				comboitem.setValue(null);
				genapGanjil.appendChild(comboitem);

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

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
				row.appendChild(tahunAkademik);
				tahunAkademik.setWidth("90%");
				tahunAkademik.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
				row.appendChild(genapGanjil);
				genapGanjil.setWidth("90%");
				genapGanjil.setReadonly(true);

				Common.selectComboItem(genapGanjil, null);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig cariID;
				row.appendChild(cariID = new MyCheckboxConfig(
						"Cari kesamaan matakuliah berdasarkan ID, jika tidak dipilih akan mencari berdasar kode matakuliah"));
				cariID.setChecked(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig utamakanYangAdaDosenNya;
				row.appendChild(utamakanYangAdaDosenNya = new MyCheckboxConfig("Utamakan yang ada dosen-nya"));
				utamakanYangAdaDosenNya.setChecked(true);

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

					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Proses", "/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
					@SuppressWarnings({ "unchecked" })
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();

						final boolean cariBerdasarID = cariID.isChecked();
						final boolean utamakanDosen = utamakanYangAdaDosenNya.isChecked();

						String mkEkivalen = "";
						List<MatakuliahEkivalen> matakuliahEkivalens = initCriteria(true).list();
						for (MatakuliahEkivalen ekivalen : matakuliahEkivalens) {
							if (cariBerdasarID) {

								mkEkivalen += mkEkivalen.isEmpty()
										? "c.id in (" + ekivalen.getMatakuliah().getId() + ","
												+ ekivalen.getMatakuliahEkivalen().getId() + ")"
										: " or c.id in (" + ekivalen.getMatakuliah().getId() + ","
												+ ekivalen.getMatakuliahEkivalen().getId() + ")";
							} else {
								mkEkivalen += mkEkivalen.isEmpty()
										? "c.kode in ('" + ekivalen.getMatakuliah().getKode() + "','"
												+ ekivalen.getMatakuliahEkivalen().getKode() + "')"
										: " or c.kode in ('" + ekivalen.getMatakuliah().getKode() + "','"
												+ ekivalen.getMatakuliahEkivalen().getKode() + "')";
							}
						}
						final String mkYgEkivalen = mkEkivalen.isEmpty() ? "" : "(" + mkEkivalen + ")";

						if (mkYgEkivalen.isEmpty()) {
							MyMessageboxConfig.show("Ekivalen tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						final List<Long> dataDihapus = new ArrayList<Long>();

						final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
						final Intbox intbox = new Intbox(10);
						Clients.showBusy(label.getValue());

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

									Clients.showBusy(label.getValue());
									System.out.println("label " + label.getValue());

									if (label.getValue().trim().equalsIgnoreCase("-")) {
										Clients.clearBusy();
										timer.detach();
									} else if (label.getValue().isEmpty()) {

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
										Common.clear(center);
										Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
										Common.clear(center);
										spreadsheet.setParent(center);
										spreadsheet.setWidth("100%");
										spreadsheet.setHeight("100%");
										spreadsheet.setSrc("../../tmp/" + file.getName());

										spreadsheet.setMaxrows(intbox.getValue() + 3);
										spreadsheet.setMaxcolumns(8);
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
												} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
											}
										});
										print.setParent(toolbar);

										final int jumlahDouble = dataDihapus.size();
										MyToolbarbuttonConfig proses = new MyToolbarbuttonConfig(
												"Proses pembersihan data double", "/img/excel.png");
										proses.setVisible(jumlahDouble > 0);
										proses.setTooltiptext(
												"Menghapus permanen salah satu data KRS ganda yang ditandai warna merah");
										proses.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												final String daftarId;
												{
													StringBuilder sb = new StringBuilder();
													for (Long id : dataDihapus) {
														if (sb.length() > 0) {
															sb.append(",");
														}
														sb.append(id);
													}
													daftarId = sb.toString();
												}
												MyMessageboxConfig.show(
														"Mohon perhatian Bapak/Ibu. Tindakan ini akan MENGHAPUS SECARA PERMANEN sebanyak "
																+ jumlahDouble
																+ " baris data KRS (detail perkuliahan) yang ditandai dengan warna merah, yaitu salah satu dari setiap pasangan/kelompok data ganda (double)."
																+ "\n\nKonsekuensi yang perlu Bapak/Ibu pahami sebelum melanjutkan:"
																+ "\n1. Penghapusan bersifat PERMANEN dan TIDAK DAPAT DIBATALKAN maupun dikembalikan. Berbeda dengan penonaktifan, data yang telah dihapus tidak lagi tersimpan di basis data."
																+ "\n2. Apabila baris KRS yang dihapus ternyata sudah memiliki nilai, kehadiran, atau rekaman akademik lain, keterkaitan tersebut dapat ikut hilang atau menjadi tidak lengkap."
																+ "\n3. Sangat disarankan Bapak/Ibu MENGUNDUH data terlebih dahulu (tombol \"Download Data\") sebagai cadangan sebelum melanjutkan."
																+ "\n\nApakah Bapak/Ibu benar-benar yakin ingin menghapus permanen " + jumlahDouble
																+ " baris data KRS ganda tersebut?",
														"Konfirmasi Penghapusan Permanen Data Ganda",
														org.zkoss.zul.Messagebox.YES | org.zkoss.zul.Messagebox.NO,
														MyMessageboxConfig.QUESTION, new EventListener() {
															@Override
															public void onEvent(Event ev) throws Exception {
																if (!"onYes".equals(ev.getName())) {
																	return;
																}
																if (daftarId.isEmpty()) {
																	return;
																}
																String sql = "delete from detailperkuliahan where id in (" + daftarId + ")";
																HibernateUtil.currentSession().createSQLQuery(sql).executeUpdate();
																onSearchDefault(ev);
																window.detach();
															}
														});
											}
										});
										proses.setParent(toolbar);

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

							new Thread(new Runnable() {

								@Override
								public void run() {
									try {

									try {
										XSSFWorkbook workbook = new XSSFWorkbook();
										XSSFSheet sheet = workbook.createSheet("DATA KRS EKIVALEN");
										sheet.setDefaultColumnWidth(20);
										XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
										lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
										lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.RED));
										lockedNumericStyle.setLocked(true);

										Session session = HibernateUtil.currentNativeSession();

										String sql = "select a.id,a.mahasiswa from detailperkuliahan a  \n"
												+ "left join perkuliahan b on (a.perkuliahan=b.id) \n"
												+ "inner join matakuliah c on (a.matakuliah_konversi=c.id or b.matakuliah=c.id) \n"
												+ "where " + mkYgEkivalen + " and a.mahasiswa is not null and "
												+ (tahunAkademik.getSelectedItem() == null
														|| tahunAkademik.getSelectedItem().getValue() == null
																? "1=1"
																: "a.tahunakademik='"
																		+ tahunAkademik.getSelectedItem().getValue()
																		+ "' ")

												+ (genapGanjil.getSelectedItem() == null
														|| genapGanjil.getSelectedItem().getValue() == null
																? ""
																: " and b.ganjil_genap = '"
																		+ genapGanjil.getSelectedItem().getValue().toString().replace("'", "''") + "'")
												+ " order by a.mahasiswa"
												+ (utamakanDosen ? ",b.dosen1 desc" : ",a.total_nilai")
												+ (utamakanDosen ? ",a.total_nilai" : ",b.dosen1 desc");
										List<Object[]> data = session.createSQLQuery(sql).list();
										intbox.setValue(data.size());
										System.out.println("sql = " + sql + "\n\ndata = " + data.size());

										int rowIndex = 0;

										XSSFRow rowhead = sheet.createRow((short) 0);
										String[] columns = new String[] { "id", "mahasiswa", "matakuliah",
												"tahun akademik", "semester", "nilai", "huruf", "dosen" };
										for (int i = 0; i < columns.length; i++) {
											rowhead.createCell(i).setCellValue(columns[i].toUpperCase());
										}

										Map<Long, List<Long>> datas = new HashMap<Long, List<Long>>();
										for (Object[] o : data) {
											try {
												Long det = Long.parseLong(o[0].toString());
												Long mhs = Long.parseLong(o[1].toString());
												if (datas.containsKey(mhs)) {
													datas.get(mhs).add(det);
												} else {
													List<Long> dets = new ArrayList<Long>();
													dets.add(det);
													datas.put(mhs, dets);
												}
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
										}

										Map<Long, List<Long>> datasFilter = new HashMap<Long, List<Long>>();
										for (Long mhs : datas.keySet()) {
											List<Long> dets = datas.get(mhs);
											if (dets.size() > 1) {
												datasFilter.put(mhs, dets);
											}
										}

										for (Long mhs : datasFilter.keySet()) {
											int indexKe = 0;
											for (Long det : datasFilter.get(mhs)) {
												try {

													Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
															.createCriteria(Detailperkuliahan.class)
															.add(Restrictions.idEq(det)).uniqueResult();

													System.out.println("det = " + det + ", detailperkuliahans = "
															+ detailperkuliahan);

													rowIndex++;
													XSSFRow row = sheet.createRow(rowIndex);
													XSSFCell cell0 = row.createCell(0);
													if (indexKe == 0) {
														dataDihapus.add(detailperkuliahan.getId());
														cell0.setCellStyle(lockedNumericStyle);
													}
													indexKe++;

													label.setValue("Sedang memproses data " + detailperkuliahan + " ");

													cell0.setCellValue(detailperkuliahan.getId());
													row.createCell(1)
															.setCellValue(detailperkuliahan.getMahasiswa() == null ? ""
																	: detailperkuliahan.getMahasiswa().toString());
													row.createCell(2)
															.setCellValue(detailperkuliahan.getPerkuliahan() == null
																	? (detailperkuliahan.getMatakuliahKonversi() == null
																			? ""
																			: detailperkuliahan.getMatakuliahKonversi()
																					.toString())
																	: detailperkuliahan.getPerkuliahan().toString());
													row.createCell(3)
															.setCellValue(detailperkuliahan.getTahunAkademik());
													row.createCell(4).setCellValue(detailperkuliahan.getSemester());
													row.createCell(5).setCellValue(detailperkuliahan.getTotalNilai());
													row.createCell(6).setCellValue(detailperkuliahan.getNilaiHuruf());

													row.createCell(7)
															.setCellValue(detailperkuliahan.getPerkuliahan() != null
																	? detailperkuliahan.getPerkuliahan().populateDosen()
																			.values().toString()
																	: "");

												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
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
										System.out.println("Your excel file has been generated! ");
										data.clear();
										data = null;
										label.setValue("");
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										label.setValue("-");
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

	class MatakuliahEkivalenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final MatakuliahEkivalen matakuliahEkivalen = (MatakuliahEkivalen) arg1;

			RevisiHelper
					.createNewRevisi(MatakuliahEkivalen.class, matakuliahEkivalen,
							matakuliahEkivalen.getMatakuliah() == null ? ""
									: matakuliahEkivalen.getMatakuliah().getId() + "-"
											+ matakuliahEkivalen.getMatakuliah().getKode() + "-"
											+ matakuliahEkivalen.getMatakuliah().getNama() + " ("
											+ matakuliahEkivalen.getMatakuliah().getJurusan().getNama() + ")")
					.setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			new Label(matakuliahEkivalen.getMatakuliahEkivalen() == null ? ""
					: matakuliahEkivalen.getMatakuliahEkivalen().getId() + "-"
							+ matakuliahEkivalen.getMatakuliahEkivalen().getKode() + "-"
							+ matakuliahEkivalen.getMatakuliahEkivalen().getNama() + " ("
							+ matakuliahEkivalen.getMatakuliahEkivalen().getJurusan().getNama() + ")")
					.setParent(vbox);

			new Label(matakuliahEkivalen.getMatakuliahEkivalen2() == null ? ""
					: matakuliahEkivalen.getMatakuliahEkivalen2().getId() + "-"
							+ matakuliahEkivalen.getMatakuliahEkivalen2().getKode() + "-"
							+ matakuliahEkivalen.getMatakuliahEkivalen2().getNama() + " ("
							+ matakuliahEkivalen.getMatakuliahEkivalen2().getJurusan().getNama() + ")")
					.setParent(vbox);

			new Label(matakuliahEkivalen.getMatakuliahEkivalen3() == null ? ""
					: matakuliahEkivalen.getMatakuliahEkivalen3().getId() + "-"
							+ matakuliahEkivalen.getMatakuliahEkivalen3().getKode() + "-"
							+ matakuliahEkivalen.getMatakuliahEkivalen3().getNama() + " ("
							+ matakuliahEkivalen.getMatakuliahEkivalen3().getJurusan().getNama() + ")")
					.setParent(vbox);

			new Label(matakuliahEkivalen.getMatakuliahEkivalen4() == null ? ""
					: matakuliahEkivalen.getMatakuliahEkivalen4().getId() + "-"
							+ matakuliahEkivalen.getMatakuliahEkivalen4().getKode() + "-"
							+ matakuliahEkivalen.getMatakuliahEkivalen4().getNama() + " ("
							+ matakuliahEkivalen.getMatakuliahEkivalen4().getJurusan().getNama() + ")")
					.setParent(vbox);

			new Label(matakuliahEkivalen.getMatakuliahEkivalen5() == null ? ""
					: matakuliahEkivalen.getMatakuliahEkivalen5().getId() + "-"
							+ matakuliahEkivalen.getMatakuliahEkivalen5().getKode() + "-"
							+ matakuliahEkivalen.getMatakuliahEkivalen5().getNama() + " ("
							+ matakuliahEkivalen.getMatakuliahEkivalen5().getJurusan().getNama() + ")")
					.setParent(vbox);

			new Label(matakuliahEkivalen.getMatakuliah().getSks() + "").setParent(arg0);
			vbox = new Vbox();
			vbox.setParent(arg0);
			if (matakuliahEkivalen.getMatakuliahEkivalen() != null) {
				new Label(matakuliahEkivalen.getMatakuliahEkivalen().getSks() + "").setParent(vbox);
			}
			if (matakuliahEkivalen.getMatakuliahEkivalen2() != null) {
				new Label(matakuliahEkivalen.getMatakuliahEkivalen2().getSks() + "").setParent(vbox);
			}
			if (matakuliahEkivalen.getMatakuliahEkivalen3() != null) {
				new Label(matakuliahEkivalen.getMatakuliahEkivalen3().getSks() + "").setParent(vbox);
			}
			if (matakuliahEkivalen.getMatakuliahEkivalen4() != null) {
				new Label(matakuliahEkivalen.getMatakuliahEkivalen4().getSks() + "").setParent(vbox);
			}
			if (matakuliahEkivalen.getMatakuliahEkivalen5() != null) {
				new Label(matakuliahEkivalen.getMatakuliahEkivalen5().getSks() + "").setParent(vbox);
			}

			new Label(matakuliahEkivalen.getKhususUntukNim()).setParent(arg0);
			new Label(matakuliahEkivalen.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(matakuliahEkivalen.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					matakuliahEkivalen.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(matakuliahEkivalen);
				}
			});

			// Kolom aksi rapi: semua tombol dibungkus kebab popup (⋯) via UIHelper.buatBarisAksi.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(matakuliahEkivalen);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
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
											Common.refreshDelete(matakuliahEkivalen);
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
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new MatakuliahEkivalen());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(MatakuliahEkivalen matakuliahEkivalen) {
		this.matakuliahEkivalen = matakuliahEkivalen;
		addWindow.setTitle(matakuliahEkivalen.getId() == null ? "Tambah Matakuliah Ekivalen" : "Ubah Matakuliah Ekivalen");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah *"));
		row.appendChild(matakuliah = new AmbilDataMatakuliahBanbox());
		matakuliah.setAttribute("matakuliah", matakuliahEkivalen.getMatakuliah());
		matakuliah.setValue(matakuliahEkivalen.getMatakuliah() == null ? ""
				: matakuliahEkivalen.getMatakuliah().getKode() + "-" + matakuliahEkivalen.getMatakuliah().getNama());
		matakuliah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ekivalen *"));
		row.appendChild(ekivalen = new AmbilDataMatakuliahBanbox());
		ekivalen.setAttribute("matakuliah", matakuliahEkivalen.getMatakuliahEkivalen());
		ekivalen.setValue(matakuliahEkivalen.getMatakuliahEkivalen() == null ? ""
				: matakuliahEkivalen.getMatakuliahEkivalen().getKode() + "-"
						+ matakuliahEkivalen.getMatakuliahEkivalen().getNama());
		ekivalen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dan juga ekivalen dg (2)"));
		row.appendChild(matakuliahEkivalen2 = new AmbilDataMatakuliahBanbox());
		matakuliahEkivalen2.setAttribute("matakuliah", matakuliahEkivalen.getMatakuliahEkivalen2());
		matakuliahEkivalen2.setValue(matakuliahEkivalen.getMatakuliahEkivalen2() == null ? ""
				: matakuliahEkivalen.getMatakuliahEkivalen2().getKode() + "-"
						+ matakuliahEkivalen.getMatakuliahEkivalen2().getNama());
		matakuliahEkivalen2.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dan juga ekivalen dg (3)"));
		row.appendChild(matakuliahEkivalen3 = new AmbilDataMatakuliahBanbox());
		matakuliahEkivalen3.setAttribute("matakuliah", matakuliahEkivalen.getMatakuliahEkivalen3());
		matakuliahEkivalen3.setValue(matakuliahEkivalen.getMatakuliahEkivalen3() == null ? ""
				: matakuliahEkivalen.getMatakuliahEkivalen3().getKode() + "-"
						+ matakuliahEkivalen.getMatakuliahEkivalen3().getNama());
		matakuliahEkivalen3.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dan juga ekivalen dg (4)"));
		row.appendChild(matakuliahEkivalen4 = new AmbilDataMatakuliahBanbox());
		matakuliahEkivalen4.setAttribute("matakuliah", matakuliahEkivalen.getMatakuliahEkivalen4());
		matakuliahEkivalen4.setValue(matakuliahEkivalen.getMatakuliahEkivalen4() == null ? ""
				: matakuliahEkivalen.getMatakuliahEkivalen4().getKode() + "-"
						+ matakuliahEkivalen.getMatakuliahEkivalen4().getNama());
		matakuliahEkivalen4.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dan juga ekivalen dg (5)"));
		row.appendChild(matakuliahEkivalen5 = new AmbilDataMatakuliahBanbox());
		matakuliahEkivalen5.setAttribute("matakuliah", matakuliahEkivalen.getMatakuliahEkivalen5());
		matakuliahEkivalen5.setValue(matakuliahEkivalen.getMatakuliahEkivalen5() == null ? ""
				: matakuliahEkivalen.getMatakuliahEkivalen5().getKode() + "-"
						+ matakuliahEkivalen.getMatakuliahEkivalen5().getNama());
		matakuliahEkivalen5.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Khusus Untuk Nim"));
		row.appendChild(khususUntukNim = new Textbox(matakuliahEkivalen.getKhususUntukNim()));
		khususUntukNim.setWidth("90%");
		khususUntukNim.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(matakuliahEkivalen.getKeterangan()));
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
		if (matakuliah.getAttribute("matakuliah") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Matakuliah",
					"Kolom Matakuliah belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Matakuliah.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (ekivalen.getAttribute("matakuliah") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Ekivalen",
					"Kolom Ekivalen belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Ekivalen.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (matakuliahEkivalen.getId() != null) {
			matakuliahEkivalen = (MatakuliahEkivalen) session.load(MatakuliahEkivalen.class,
					matakuliahEkivalen.getId());
		}

		matakuliahEkivalen.setMatakuliah((Matakuliah) matakuliah.getAttribute("matakuliah"));
		matakuliahEkivalen.setMatakuliahEkivalen((Matakuliah) ekivalen.getAttribute("matakuliah"));

		matakuliahEkivalen.setMatakuliahEkivalen2((Matakuliah) matakuliahEkivalen2.getAttribute("matakuliah"));
		matakuliahEkivalen.setMatakuliahEkivalen3((Matakuliah) matakuliahEkivalen3.getAttribute("matakuliah"));
		matakuliahEkivalen.setMatakuliahEkivalen4((Matakuliah) matakuliahEkivalen4.getAttribute("matakuliah"));
		matakuliahEkivalen.setMatakuliahEkivalen5((Matakuliah) matakuliahEkivalen5.getAttribute("matakuliah"));

		matakuliahEkivalen.setKhususUntukNim(khususUntukNim.getValue().trim());
		matakuliahEkivalen.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, matakuliahEkivalen);

		matakuliahEkivalen.getMatakuliah().reInitEkivalen();
		matakuliahEkivalen.getMatakuliahEkivalen().reInitEkivalen();
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(MatakuliahEkivalen.class).createAlias("matakuliah", "matakuliah")
				.createAlias("matakuliahEkivalen", "matakuliahEkivalen");

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.or(
						Restrictions.ilike("matakuliah.kode", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("matakuliah.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE)))

				.add(searchekivalen.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("matakuliahEkivalen.kode", searchekivalen.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("matakuliahEkivalen.nama", searchekivalen.getValue().trim(),
										MatchMode.ANYWHERE)));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<MatakuliahEkivalen> matakuliahEkivalen = ConstantValues.simpleList(
				initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
				MatakuliahEkivalen.class);
		ListModel strset = new SimpleListModel(matakuliahEkivalen);
		grid.setRowRenderer(new MatakuliahEkivalenRenderer());
		grid.setModelCheckMobile(strset);

	}

}
