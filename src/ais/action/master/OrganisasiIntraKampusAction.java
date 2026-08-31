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

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.OrganisasiIntraKampusPunyaMahasiswaHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.format1.akademik.LaporanPerOrganisasiMahasiswa;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.JabatanOrganisasiIntraKampus;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.OrganisasiIntraKampus;
import ais.database.model.OrganisasiIntraKampusPunyaMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk organisasi intra kampus. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchkode}, {@code Combobox
 * searchjurusan}, {@code Combobox searchfakultas}, {@code Textbox searchnamamhs}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian
 * ({@code onUploadData()}, {@code onSearchDefault()}); validasi/perhitungan ({@code
 * checkNamaOrganisasiIntraKampus()}); mutasi data ({@code onSave()}); pelaporan/ekspor ({@code
 * cetakDataCustomButton()}); operasi domain lain ({@code onJabatanOrganisasiMahasiswa()}, {@code onAdd()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class OrganisasiIntraKampusAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

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
	protected Textbox searchnim;
	protected AmbilDataDosenBanbox searchdosen;

	private Textbox nama;
	private Textbox keterangan;
	private Combobox jurusan;
	private Combobox fakultas;

	private MyDoublebox minimalIpk;
	private MyDoublebox minimalSks;
	private MyDoublebox minimalSkkm;
 
	// private boolean edit = false;
	// private boolean delete = false;

	private OrganisasiIntraKampus organisasiIntraKampus;
	private MyToolbarbuttonConfig add;
	private MyToolbarbuttonConfig uploadData;

	private Tabpanel jabatanOrganisasi;
	private Textbox namaEn;

	public void onJabatanOrganisasiMahasiswa(Event event) {
		if (jabatanOrganisasi.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jabatanOrganisasi);
			MyInclude iframe = new MyInclude("/pages/master/jabatan_organisasi_intra_kampus.zul");
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

			final Label label = new Label(
					ais.common.Common.getBahasaConfig("Proses upload data organisasiIntraKampus sedang berlangsung, harap menunggu.."));

			new Thread(new Runnable() {

				@Override
				public void run() {
					try {

					XSSFWorkbook workbook;
					try {
						workbook = new XSSFWorkbook(file.getAbsolutePath());

						for (XSSFSheet sheet : Common.getAllXSSFSheet(workbook)) {
							Session session = HibernateUtil.currentNativeSession();

							OrganisasiIntraKampus organisasiIntraKampus = (OrganisasiIntraKampus) session
									.createCriteria(OrganisasiIntraKampus.class)
									.add(Restrictions.ilike("kode", sheet.getSheetName().trim(), MatchMode.EXACT))
									.setMaxResults(1).uniqueResult();
							if (organisasiIntraKampus == null) {
								organisasiIntraKampus = new OrganisasiIntraKampus();
								organisasiIntraKampus.setNama(sheet.getSheetName().trim());
								organisasiIntraKampus.setKeterangan(sheet.getSheetName().trim());
								session.getTransaction().begin();
								session.save(organisasiIntraKampus);
								session.getTransaction().commit();
							}

							HibernateUtil.closeSession();

							int size = (sheet.getLastRowNum() + 1);
							for (int i = 0; i < (sheet.getLastRowNum() + 1); i++) {

								session = HibernateUtil.currentNativeSession();

								try {
									Mahasiswa mahasiswa = null;
									try {
										String nim = Common.getSheetContentAsString(sheet, 1, i);
										mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
												.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();

										if (mahasiswa == null) {
											nim = Common.getCellContent(Common.getCell(sheet, 1, i));
											mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
													.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
										}

										if (mahasiswa == null) {
											nim = Common.getCellContent(Common.getCell(sheet, 2, i));
											mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
													.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
										}

										if (mahasiswa == null) {
											nim = Common.getCellContent(Common.getCell(sheet, 3, i));
											mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
													.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
										}

									} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/OrganisasiIntraKampusAction.java:222");

									}

									if (mahasiswa == null) {
										continue;
									}

									Date mulai = Common.getSheetContentAsDate(sheet, 3, i);
									Date sampai = Common.getSheetContentAsDate(sheet, 4, i);
									JabatanOrganisasiIntraKampus jabatanOrganisasiIntraKampus = (JabatanOrganisasiIntraKampus) Common
											.getSheetContentAsObject(sheet, 5, i, JabatanOrganisasiIntraKampus.class);
									String keterangan = Common.getSheetContentAsString(sheet, 6, i);

									Boolean persetujuan = Common.getSheetContentAsBoolean(sheet, 8, i);

									OrganisasiIntraKampusPunyaMahasiswa organisasiIntraKampusPunyaMahasiswa = (OrganisasiIntraKampusPunyaMahasiswa) session
											.createCriteria(OrganisasiIntraKampusPunyaMahasiswa.class)
											.add(Restrictions.eq("mahasiswa", mahasiswa))
											.add(Restrictions.eq("organisasiIntraKampus", organisasiIntraKampus))
											.setMaxResults(1).uniqueResult();

									if (organisasiIntraKampusPunyaMahasiswa == null) {
										organisasiIntraKampusPunyaMahasiswa = new OrganisasiIntraKampusPunyaMahasiswa();
									}
									organisasiIntraKampusPunyaMahasiswa.setMahasiswa(mahasiswa);
									organisasiIntraKampusPunyaMahasiswa.setOrganisasiIntraKampus(organisasiIntraKampus);
									organisasiIntraKampusPunyaMahasiswa.setOleh(tbmuser.getUserId());
									organisasiIntraKampusPunyaMahasiswa.setTbmuser(tbmuser);
									organisasiIntraKampusPunyaMahasiswa
											.setDiubahDari(OrganisasiIntraKampusAction.class.getSimpleName());

									organisasiIntraKampusPunyaMahasiswa.setMulai(mulai);
									organisasiIntraKampusPunyaMahasiswa.setSampai(sampai);
									organisasiIntraKampusPunyaMahasiswa
											.setJabatanOrganisasiIntraKampus(jabatanOrganisasiIntraKampus);
									organisasiIntraKampusPunyaMahasiswa.setKeterangan(keterangan);
									organisasiIntraKampusPunyaMahasiswa.setPersetujuan(persetujuan);

									session.getTransaction().begin();
									session.saveOrUpdate(organisasiIntraKampusPunyaMahasiswa);
									session.getTransaction().commit();

									HibernateUtil.closeSession();

									label.setValue("Upload mahasiswa " + mahasiswa + " di organisasiIntraKampus "
											+ organisasiIntraKampus.getNama() + ".. "
											+ Common.numberFormat.get().format(i * 100.0 / size) + " %");

								} catch (Exception e1) {
									// TODO Auto-generated catch block

									HibernateUtil.closeSession();

									e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/OrganisasiIntraKampusAction.java:276");
								}
							}

						}

					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/OrganisasiIntraKampusAction.java:284");
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
				.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = cetakDataCustomButton("Download Data Mahasiswa", "/img/print.png");
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		String[] contents = new String[] { "id", "nama", "namaEn", "fakultas", "jurusan", "minimalIpk", "minimalSks",
				"minimalSkkm", "keterangan" };
		cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, OrganisasiIntraKampus.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible())); }
		Common.appendKeToolbar(upload, add, comp);

		if (uploadData != null) { uploadData.setVisible((add != null && add.isVisible())); }

		MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Organisasi Mahasiswa", "/img/print.png");
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				LaporanPerOrganisasiMahasiswa laporan = new LaporanPerOrganisasiMahasiswa();
				laporan.setTitle("Organisasi Mahasiswa");
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
					final List<OrganisasiIntraKampus> organisasiIntraKampuses = initCriteria(true).list();
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

								for (OrganisasiIntraKampus organisasiIntraKampus : organisasiIntraKampuses) {
									List<OrganisasiIntraKampusPunyaMahasiswa> data = session
											.createCriteria(OrganisasiIntraKampusPunyaMahasiswa.class)
											.add(Restrictions.eq("organisasiIntraKampus", organisasiIntraKampus))
											.createAlias("mahasiswa", "mahasiswa").addOrder(Order.asc("mahasiswa.nim"))
											.setMaxResults(1048576).list();

									if (!data.isEmpty()) {
										intbox.setValue(data.size());
										System.out.println("data = " + data.size());

										XSSFSheet sheet = workbook.createSheet(organisasiIntraKampus.getKode());
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

										for (OrganisasiIntraKampusPunyaMahasiswa o : data) {
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
												row.createCell(1).setCellValue(o.getMahasiswa().getNim());
												row.createCell(2).setCellValue(o.getMahasiswa().getNama());
												row.createCell(3).setCellValue(o.getMulai() == null ? ""
														: Common.dateFormat1.get().format(o.getMulai()));
												row.createCell(4).setCellValue(o.getSampai() == null ? ""
														: Common.dateFormat1.get().format(o.getSampai()));
												row.createCell(5)
														.setCellValue(o.getJabatanOrganisasiIntraKampus() == null ? ""
																: o.getJabatanOrganisasiIntraKampus().getNama());
												row.createCell(6).setCellValue(o.getKeterangan());

												LampiranLain lam = LampiranLain.ambil(o.getId(),
														OrganisasiIntraKampusPunyaMahasiswa.class.getName());

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

	/**
	 * Renderer lokal untuk layar/komponen {@link OrganisasiIntraKampusAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link OrganisasiIntraKampusAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see OrganisasiIntraKampusAction
	 */
	class OrganisasiIntraKampusRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final OrganisasiIntraKampus organisasiIntraKampus = (OrganisasiIntraKampus) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Common.clear(detail);
					if (detail.isOpen()) {
						OrganisasiIntraKampusPunyaMahasiswaHelper detailperkuliahanHelper = new OrganisasiIntraKampusPunyaMahasiswaHelper();
						detailperkuliahanHelper.display(organisasiIntraKampus, detail, addWindow);
					}
				}
			});

			new Label(organisasiIntraKampus.getKode()).setParent(arg0);
			Vbox a;
			(a = RevisiHelper.createNewRevisi(OrganisasiIntraKampus.class, organisasiIntraKampus,
					organisasiIntraKampus.getNama())).setParent(arg0);
			new Label(organisasiIntraKampus.getNamaEn()).setParent(a);

			new Label(organisasiIntraKampus.getFakultas() == null ? "Semua"
					: organisasiIntraKampus.getFakultas().getNama()).setParent(arg0);
			new Label(
					organisasiIntraKampus.getJurusan() == null ? "Semua" : organisasiIntraKampus.getJurusan().getNama())
					.setParent(arg0);

			new Label(Common.numberFormat.get().format(organisasiIntraKampus.getMinimalSks()) + " / "
					+ Common.numberFormat.get().format(organisasiIntraKampus.getMinimalIpk()) + " / "
					+ Common.numberFormat.get().format(organisasiIntraKampus.getMinimalSkkm())).setParent(arg0);

			new Label(organisasiIntraKampus.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			// button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(organisasiIntraKampus);
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
											Common.refreshDelete(organisasiIntraKampus);
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
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new OrganisasiIntraKampus());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(OrganisasiIntraKampus organisasiIntraKampus) {
		this.organisasiIntraKampus = organisasiIntraKampus;
		addWindow.setTitle(organisasiIntraKampus.getId() == null ? "Tambah Organisasi Intra Kampus" : "Ubah Organisasi Intra Kampus");
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
		row.appendChild(nama = new Textbox(organisasiIntraKampus.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Organisasi (dalam bhs inggris)"));
		row.appendChild(namaEn = new Textbox(organisasiIntraKampus.getNamaEn()));
		namaEn.setWidth("90%");

		Tbmuser tbmuser = Common.getCurrentUser();
		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		if (organisasiIntraKampus.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			organisasiIntraKampus.setFakultas(tbmuser.ambilFakultas());
		}
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		Common.selectComboItem(fakultas, organisasiIntraKampus.getFakultas());
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
		Common.pilihJurusan(jurusan, organisasiIntraKampus.getJurusan());

		if (organisasiIntraKampus.getJurusan() == null) {
			if (tbmuser.ambilJurusan() != null
					|| (tbmuser.getMahasiswa() != null && tbmuser.getMahasiswa().getJurusan() != null)) {
				Common.pilihJurusan(jurusan,
						tbmuser == null || tbmuser.ambilJurusan() == null ? tbmuser.getMahasiswa().getJurusan()
								: tbmuser.ambilJurusan());
				jurusan.setDisabled(true);
			} else {
				jurusan.setDisabled(false);
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal IPK"));
		row.appendChild(minimalIpk = new MyDoublebox(organisasiIntraKampus.getMinimalIpk()));
		minimalIpk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal SKS"));
		row.appendChild(minimalSks = new MyDoublebox(organisasiIntraKampus.getMinimalSks()));
		minimalSks.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal SKKM"));
		row.appendChild(minimalSkkm = new MyDoublebox(organisasiIntraKampus.getMinimalSkkm()));
		minimalSkkm.setWidth("90%");

		Common.initKeterangan(rows, "SKKM = Surat Keterangan Kredit Mahasiswa");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(organisasiIntraKampus.getKeterangan()));
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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Organisasi",
					"Kolom Nama Organisasi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Organisasi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkNamaOrganisasiIntraKampus();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Organisasi",
					"Nama Organisasi sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama organisasi yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (organisasiIntraKampus.getId() != null) {
			organisasiIntraKampus = (OrganisasiIntraKampus) session.load(OrganisasiIntraKampus.class,
					organisasiIntraKampus.getId());

		}

		organisasiIntraKampus.setMinimalSkkm(minimalSkkm.getValue());
		organisasiIntraKampus.setMinimalIpk(minimalIpk.getValue());
		organisasiIntraKampus.setMinimalSks(minimalSks.getValue());

		organisasiIntraKampus.setNama(nama.getValue());
		organisasiIntraKampus.setNamaEn(namaEn.getValue());
		organisasiIntraKampus.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		organisasiIntraKampus.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));

		organisasiIntraKampus.setKeterangan(keterangan.getValue());

		Common.refreshUpdate(session, organisasiIntraKampus);

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Criterion criterionMhs = Restrictions.sqlRestriction("true");
		if (!searchnim.getValue().trim().isEmpty() || !searchnamamhs.getValue().trim().isEmpty()) {
			String sql = "this_.id in (select organisasi_intra_kampus from organisasi_intra_kampus_punya_mahasiswa a inner join mahasiswa b on (a.mahasiswa = b.id) where organisasi_intra_kampus is not null and b.nama ilike '%"
					+ searchnamamhs.getValue().trim() + "%' and b.nim ilike '%" + searchnim.getValue().trim()
					+ "%' group by organisasi_intra_kampus)";
			criterionMhs = Restrictions.sqlRestriction(sql);
		}

		Criterion criterionDosenPa = Restrictions.sqlRestriction("true");
		if (searchdosen != null && searchdosen.getAttribute("dosen") != null) {
			Dosen dsn = (Dosen) searchdosen.getAttribute("dosen");
			String sql = "this_.id in (select organisasi_intra_kampus from organisasi_intra_kampus_punya_mahasiswa a inner join mahasiswa b on (a.mahasiswa = b.id) where organisasi_intra_kampus is not null and b.dosen = "
					+ dsn.getId() + " group by organisasi_intra_kampus)";
			criterionDosenPa = Restrictions.sqlRestriction(sql);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(OrganisasiIntraKampus.class);

		if (order)
			criteria.addOrder(Order.desc("id")); // pengajuan terkini di atas
		criteria.add(criterionMhs).add(criterionDosenPa)
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

		List<OrganisasiIntraKampus> organisasiIntraKampus = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(organisasiIntraKampus);
		grid.setRowRenderer(new OrganisasiIntraKampusRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaOrganisasiIntraKampus() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(OrganisasiIntraKampus.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.organisasiIntraKampus.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.organisasiIntraKampus.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
