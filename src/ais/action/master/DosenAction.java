package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardDosen;
import ais.action.master.dashboard.admin.DashboardKegiatanKedosenanAdmin;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederJSONImport;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.impor.ImportFromEpsbedHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanDaftarHadirDosen;
import ais.action.report.format1.akademik.LaporanDaftarHadirDosenHarian;
import ais.action.report.format1.akademik.LaporanFormatEMISDosen;
import ais.action.report.format1.akademik.LaporanRekapDosenPerFakultasDanPerProdi;
import ais.action.report.format1.akademik.LaporanRekapDosenPerFakultasDanProdi;
import ais.action.report.format1.akademik.LaporanRekapDosenTetap;
import ais.action.report.format1.akademik.LaporanRekapDosenTidakTetap;
import ais.action.report.format1.akademik.LaporanRekapJumlahDosen;
import ais.action.report.format1.akademik.LaporanRekapJumlahDosenPerPendidikan;
import ais.action.report.format1.akademik.LaporanRuanganDosen;
import ais.action.report.format1.akademik.LaporanSKDosen;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonOnSearchdefault;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.sinta.SintaPtCrawler;
import ais.database.dao.DaoFactory;
import ais.database.dao.DosenDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.AsesorPenunjangKinerjaDosen;
import ais.database.model.BiodataDosen;
import ais.database.model.Dosen;
import ais.database.model.GolonganPns;
import ais.database.model.IkatanKerjaDosen;
import ais.database.model.JabatanFungsionalDosen;
import ais.database.model.JenisPendidikDanTenagaKependidikan;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.LembagaPengangkat;
import ais.database.model.Pegawai;
import ais.database.model.PerguruanTinggi;
import ais.database.model.StatusPegawai;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.employ.GajiPokok;
import ais.database.model.employ.Pendidikan;
import ais.database.model.file.FotoDosen;
import ais.database.model.file.FotoPegawai;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIframe;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk dosen. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code Paging paging},
 * {@code Textbox searchcode}, {@code Textbox searchnama}, {@code Combobox searchfakultas}, {@code Combobox
 * searchjurusan}, {@code Combobox searchstatus}, {@code Checkbox searchaktif}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onUploadRfid()}, {@code onDownloadRfid()}, {@code onUploadDBF()}, {@code onDownloadFoto()}, {@code
 * onDownloadFotoMassal()}, {@code onUploadFotoMassal()}); pelaporan/ekspor ({@code cetakDRHDosen()}); operasi
 * domain lain ({@code onDataSK()}, {@code onStatistik()}, {@code onKegiatanKedosenan()}, {@code onDataDosen()},
 * {@code onKonfigurasiBiodataDosen()}, {@code onLaporanSkDosen()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
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
public class DosenAction extends GenericAutowireComposer
		implements CommonOnSearchdefault, DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyGrid grid;
	private Paging paging;
	private Textbox searchcode;
	private Textbox searchnama;
	private Combobox searchfakultas;

	private Combobox searchjurusan;
	private Combobox searchstatus;
	private Checkbox searchaktif;
	private Checkbox searchjugaTampilkanDosenYgBolehMengajarProdiLain;

	private Combobox searchIkatanKerjaDosen;
	private Combobox searchJabatanFungsionalDosen;
	private Combobox searchJenisPendidikDanTenagaKependidikan;
	private Combobox searchGolonganPegawai;
	private Combobox searchLembagaPengangkat;

	private MyToolbarbuttonConfig add;

	private boolean edit = false;
	private boolean delete = false;
	private BiodataDosenAction biodataDosenAction;

	private MyToolbarbuttonConfig downloadRfid;
	private MyToolbarbuttonConfig uploadRfid;

	public void onUploadRfid(Event event) throws Exception {

		ForwardEvent forwardEvent = (ForwardEvent) event;
		Media media = ((UploadEvent) forwardEvent.getOrigin()).getMedia();
		if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
			return;
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
			final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload RFID Dosen");

			for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
				Session session = HibernateUtil.currentNativeSession();
				String id = null;
				String rfid = null;
				try {

					id = Common.getCellContent(Common.getCell(sheet, 0, i));
					rfid = Common.getCellContent(Common.getCell(sheet, 1, i));

					if (id == null || id.trim().isEmpty()) {
						continue;
					}

					Dosen dosen = (Dosen) ConstantValues.ambil(Dosen.class.getName(), Long.parseLong(rfid));
					System.out.println("id = " + id + " rfid = " + rfid + ", dosen = " + dosen);
					if (dosen != null) {
						session.refresh(dosen);
						dosen.setIdfinger(rfid);
						session.getTransaction().begin();
						session.update(dosen);
						session.getTransaction().commit();
						report.sukses(i, id + " | RFID=" + rfid, "idfinger diperbarui");
					} else {
						report.gagal(i, id + " | RFID=" + rfid, "Dosen tidak ditemukan", "Pastikan kolom ID mengacu pada ID Dosen yang valid");
					}

				} catch (Exception e) {
					report.gagal(i, (id != null ? id : "baris-" + i) + " | RFID=" + (rfid != null ? rfid : ""), e, "Periksa format NIP dan nilai RFID");
					session.getTransaction().rollback();
					Common.tampilErrorJikaAdmin(e);
					// Common.tampilErrorJikaAdmin(e);
				}

				HibernateUtil.closeSession();
			}

			try {
				Filedownload.save(report.simpanLaporan(), "text/plain");
			} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) DosenAction laporan rfid"); }
			MyMessageboxConfig.show(report.getRingkasan(), "Pemberitahuan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);

		} else {
			MyMessageboxConfig.show(
					"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
							+ media,
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
		}
	}

	@SuppressWarnings("unchecked")
	public void onDownloadRfid(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				ProjectionList projectionList = Projections.projectionList();
				projectionList.add(Projections.property("id"));
				projectionList.add(Projections.property("idfinger"));
				projectionList.add(Projections.property("nama"));

				List<Object[]> objects = initCriteria(true).setProjection(projectionList).list();

				final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/idfinger_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

				File file;
				(file = new File(filename)).createNewFile();

				XSSFWorkbook workbook = new XSSFWorkbook();
				XSSFSheet sheet = workbook.createSheet("DATA");
				sheet.setDefaultColumnWidth(20);
				int rowIndex = 0;

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("ID");
				rowhead.createCell(1).setCellValue("ID Finger/RFID");
				rowhead.createCell(2).setCellValue("Nama");
				rowIndex = 1;
				for (Object[] dosen : objects) {
					try {
						XSSFRow row = sheet.createRow(rowIndex);
						row.createCell(0).setCellValue(dosen[0] + "");
						row.createCell(1).setCellValue(dosen[1] + "");
						row.createCell(2).setCellValue(dosen[2] + "");
						rowIndex++;
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
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

				Filedownload.save(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

			}
		}, "Download ...");

	}

	protected Tabpanel skDosen;

	public void onDataSK(Event event) {

		if (skDosen.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(skDosen);
			MyInclude iframe = new MyInclude("/pages/master/penugasan_dosen_mengajar.zul");
			iframe.setParent(window);
		}
	}

	protected Tabpanel statistik;

	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {
			DashboardDosen include = new DashboardDosen();
			ais.ui.util.BaseDasbordPortal.mountWrapped(include, statistik,
				"Statistik Dosen", "Gambaran sebaran dosen per jabatan, kualifikasi, dan status kepegawaian.");
		}
	}

	private Tabpanel kegiatanDosenan;

	public void onKegiatanKedosenan(Event event) {
		if (kegiatanDosenan.getChildren().size() == 0) {
			DashboardKegiatanKedosenanAdmin window = new DashboardKegiatanKedosenanAdmin("", "none", false);
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, kegiatanDosenan,
				"Kegiatan Kedosenan", "Rekap seluruh kegiatan tridharma dosen: pengajaran, penelitian, dan pengabdian.");
		}
	}

	private Tabpanel laporanDosenHarian;
	private Tabpanel ruangDosen;
	private Tabpanel laporanPerDosen;
	private Tabpanel laporanSkDosen;
	private Tabpanel laporanRekapDosenTetap;
	private Tabpanel laporanRekapDosenTetapPerFakultasDanPerProdi;
	private Tabpanel laporanRekapDosenPerFakultasDanPerProdi;
	private Tabpanel laporanRekapDosenTidakTetap;
	private Tabpanel laporanRekapJumlahDosenPerPendidikan;
	private Tabpanel laporanRekapJumlahDosen;

	private Tabpanel konfigurasiBiodataDosen;
	private Long selectedJurusan = null;

	private Tabpanel dataDosen;
	private PerguruanTinggi perguruanTinggi;

	public void onDataDosen(Event event) {

		if (dataDosen.getChildren().size() == 0) {
			LaporanFormatEMISDosen laporan = new LaporanFormatEMISDosen();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(dataDosen);
		}
	}

	public void onKonfigurasiBiodataDosen(Event event) {
		if (konfigurasiBiodataDosen.getChildren().size() == 0) {
			ais.ui.util.MyButtonTabbox btnTab = ais.ui.util.MyButtonTabbox.buat(konfigurasiBiodataDosen, "100%", new int[] { 0 });

			{
				org.zkoss.zul.Div panel = btnTab.tambahTab(0, "Konfigurasi", "/img/svg/dashboard-speed.svg");
				MyIframe include = new MyIframe("/pages/master/konfigurasi_biodata_dosen.zul");
				include.setHeight("100%");
				include.setWidth("100%");
				include.setParent(panel);
			}
			btnTab.tambahTabLazy(1, "Jabatan Fungsional", "/img/svg/user-tie.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyIframe inc = new MyIframe("/pages/master/jabatan_fungsional_dosen.zul");
					inc.setHeight("100%"); inc.setWidth("100%"); inc.setParent(panel);
				}
			});
			btnTab.tambahTabLazy(2, "Ikatan Kerja", "/img/svg/user-box-line.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyIframe inc = new MyIframe("/pages/master/ikatan_kerja_dosen.zul");
					inc.setHeight("100%"); inc.setWidth("100%"); inc.setParent(panel);
				}
			});
			btnTab.tambahTabLazy(3, "Status Kepegawaian", "/img/svg/person-check.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyIframe inc = new MyIframe("/pages/master/status_kepegawaian.zul");
					inc.setHeight("100%"); inc.setWidth("100%"); inc.setParent(panel);
				}
			});
			btnTab.tambahTabLazy(4, "Jenis Pendidik & Tendik", "/img/svg/chalkboard-teacher-light.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyIframe inc = new MyIframe("/pages/master/jenis_pendidik_dan_tenaga_kependidikan.zul");
					inc.setHeight("100%"); inc.setWidth("100%"); inc.setParent(panel);
				}
			});
			btnTab.tambahTabLazy(5, "Lembaga Pengangkat", "/img/svg/user-group.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyIframe inc = new MyIframe("/pages/master/lembaga_pengangkat.zul");
					inc.setHeight("100%"); inc.setWidth("100%"); inc.setParent(panel);
				}
			});
			btnTab.tambahTabLazy(6, "Sumber Gaji", "/img/svg/money-bills.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyIframe inc = new MyIframe("/pages/master/sumber_gaji.zul");
					inc.setHeight("100%"); inc.setWidth("100%"); inc.setParent(panel);
				}
			});
			btnTab.tambahTabLazy(7, "Status Pegawai", "/img/svg/user-edit.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyIframe inc = new MyIframe("/pages/master/status_pegawai.zul");
					inc.setHeight("100%"); inc.setWidth("100%"); inc.setParent(panel);
				}
			});
			btnTab.tambahTabLazy(8, "Jabatan", "/img/svg/user-business.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyIframe inc = new MyIframe("/pages/master/jabatan.zul");
					inc.setHeight("100%"); inc.setWidth("100%"); inc.setParent(panel);
				}
			});
			btnTab.tambahTabLazy(9, "Tenaga Kependidikan", "/img/svg/users.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyIframe inc = new MyIframe("/pages/master/jenis_tenaga_kependidikan.zul");
					inc.setHeight("100%"); inc.setWidth("100%"); inc.setParent(panel);
				}
			});
			btnTab.tambahTabLazy(10, "Golongan PNS", "/img/svg/user-rectangle-light.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyIframe inc = new MyIframe("/pages/master/golongan_pns.zul");
					inc.setHeight("100%"); inc.setWidth("100%"); inc.setParent(panel);
				}
			});
		}
	}

	// private Boolean tampilkanTombolAktif = false;
	// private MyColumnConfig label_aktif;

	public void onUploadDBF(Event event) throws Exception {

		Clients.showBusy("Upload data mahasiswa .......");

		ForwardEvent forwardEvent = (ForwardEvent) event;
		Media media = ((UploadEvent) forwardEvent.getOrigin()).getMedia();
		if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
			return;
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload DBF Dosen");
		if (media.getName().trim().equalsIgnoreCase("TBDOS.DBF")) {
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

			ImportFromEpsbedHelper.doImport(file);

			String sql = ImportFromEpsbedHelper.read("dosen.sql");
			Session session = HibernateUtil.currentSession();
			session.createSQLQuery(sql).executeUpdate();
			report.sukses(1, media.getName(), "import selesai");
			try {
				Filedownload.save(report.simpanLaporan(), "text/plain");
			} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) DosenAction laporan dbf"); }
			MyMessageboxConfig.show(report.getRingkasan(), "Upload DBF", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
		} else {
			MyMessageboxConfig.show("File yang anda upload harus TBDOS.DBF" + media, "Error", MyMessageboxConfig.OK,
					MyMessageboxConfig.ERROR);
		}

		Clients.clearBusy();
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public static String[] contents = new String[] { "id", "code", "mycode", "nidn", "nuptk", "idfinger", "nama",
			"alamat", "kelamin", "tempatlahir", "tanggallahir", "pangkat", "golongan", "jabatan", "jurusan", "fakultas",
			"spesifikasiJabatan", "statusPegawai", "aktif", "pendidikan", "pendidikans1", "pendidikans2",
			"pendidikans3", "email", "telp", "sertifikasi", "nomorSertifikasi", "sesuaiBidangKeilmuan",
			"atasanlangsung", "googleScholar", "npwp", "hp", "ktp", "ikatanKerjaDosen" };

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		if (execution.getParameter("jurusan") != null) {
			selectedJurusan = Long.parseLong(execution.getParameter("jurusan"));
		} else if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		Common.insertComboDanSemua(searchLembagaPengangkat, new String[] { "nama" }, "keterangan",
				LembagaPengangkat.class, Restrictions.ne("nama", ""),
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertComboDanSemua(searchJabatanFungsionalDosen, new String[] { "nama" }, "keterangan",
				JabatanFungsionalDosen.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.ne("nama", ""));

		Common.insertComboDanSemua(searchJenisPendidikDanTenagaKependidikan, new String[] { "nama" }, "keterangan",
				JenisPendidikDanTenagaKependidikan.class, Restrictions.ne("nama", ""),
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertComboDanSemua(searchIkatanKerjaDosen, new String[] { "nama" }, "keterangan",
				IkatanKerjaDosen.class, Restrictions.ne("nama", ""),
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertComboDanSemua(searchGolonganPegawai, "nama", "keterangan", GolonganPns.class);

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(Pendidikan.class).add(Restrictions.eq("nama", "Sp-2"))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {
			Pendidikan pendidikan = new Pendidikan();
			pendidikan.setNama("Sp-2");
			pendidikan.setKeterangan("Sp-2");
			session.save(pendidikan);

			pendidikan = new Pendidikan();
			pendidikan.setNama("Sp-1");
			pendidikan.setKeterangan("Sp-1");
			session.save(pendidikan);

			pendidikan = new Pendidikan();
			pendidikan.setNama("Profesi");
			pendidikan.setKeterangan("Profesi");
			session.save(pendidikan);
		}

		count = ((Number) session.createCriteria(AsesorPenunjangKinerjaDosen.class)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		Tbmuser tbmuser = Common.getCurrentUser();
		if (count == 0) {
			AsesorPenunjangKinerjaDosen angket = new AsesorPenunjangKinerjaDosen();
			angket.setKode("A");
			angket.setNama("Asesor I");
			angket.setFakultas(tbmuser.ambilFakultas());
			angket.setJurusan(tbmuser.ambilJurusan());
			Common.refreshSaveOrUpdate(session, angket);

			angket = new AsesorPenunjangKinerjaDosen();
			angket.setFakultas(tbmuser.ambilFakultas());
			angket.setJurusan(tbmuser.ambilJurusan());
			angket.setKode("B");
			angket.setNama("Asesor II");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new AsesorPenunjangKinerjaDosen();
			angket.setFakultas(tbmuser.ambilFakultas());
			angket.setJurusan(tbmuser.ambilJurusan());
			angket.setKode("C");
			angket.setNama("Asesor III");
			Common.refreshSaveOrUpdate(session, angket);

		}

		Common.insertComboDanSemua(searchstatus, "nama", StatusPegawai.class, Restrictions.eq("aktif", true));

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("Username");
//		columnHeadersAdding.add("Jumlah Login");
//		columnHeadersAdding.add("Sejarah Login");

		EventListener dataAdding = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				Dosen dosen = (Dosen) objects[0];

				XSSFRow row = (XSSFRow) objects[2];

				Session session = HibernateUtil.currentNativeSession();

				List<String> usernames = session.createCriteria(Tbmuser.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.isNotNull("userId")).add(Restrictions.ne("userId", ""))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("dosen", dosen)).setProjection(Projections.property("userId")).list();
				String u = "";
				for (String user : usernames) {
					u += u.trim().isEmpty() ? user : "," + user;
				}

//				Object[] logLogins = (Object[]) session.createCriteria(LogLogin.class)
//						.add(Restrictions.eq("dosen", dosen)).add(Restrictions.isNotNull("login"))
//						.setProjection(
//								Projections.projectionList().add(Projections.max("login")).add(Projections.rowCount()))
//						.uniqueResult();
//
//				Date maxLogin = (Date) (logLogins == null || logLogins.length == 0 || logLogins[0] == null ? null
//						: logLogins[0]);
//				Integer countLogin = ((Number) (logLogins == null || logLogins.length == 0 || logLogins[1] == null ? 0
//						: logLogins[1])).intValue();
//
				row.createCell(contents.length + 0).setCellValue(u);
//
//				row.createCell(contents.length + 1).setCellValue(countLogin);
//
//				row.createCell(contents.length + 2)
//						.setCellValue(maxLogin == null ? "" : Common.dateFormat3.get().format(maxLogin));

				HibernateUtil.closeSession();
			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Dosen.class, this, "Download Dosen",
				"/img/print.png", columnHeadersAdding, dataAdding, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Dosen.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig generatePasswordDosen = new MyToolbarbuttonConfig("Password Dosen", "/img/print.png");
		generatePasswordDosen.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Anda akan membuatkan dan mengambil username dan password dosen.", "Informasi",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											String strURL = Common.getKonfigurasi("ambil_kode_url",
													"https://dev.ecampus.id/ecampus/Api").getNilai();

											String link = Common.getRequestHostWithProtocol() + "/Api";
											String nama_pt = perguruanTinggi.getNama();

											HttpServletRequest request = (HttpServletRequest) Executions.getCurrent()
													.getNativeRequest();

											String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
													.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
											String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
													.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
											String banner_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
													.getPerguruanTinggiMedia(request, "banner_perguruanTinggi_");
											String background_login_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
													.getPerguruanTinggiMedia(request,
															"background_login_perguruanTinggi_");

											String filename = Sessions.getCurrent().getWebApp()
													.getRealPath("/tmp/user_password_dosen_"
															+ URLEncoder.encode(Common.datetimeFormat2s.get()
																	.format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
															+ ".xlsx");

											boolean menggunkanaNidn = Common.bolehKonfigurasi("username_menggunkana_nidn", Konfigurasi.TIDAK_AKTIF);

											List<Dosen> dosens = initCriteria(true).add(Restrictions.isNotNull("nama"))
													.setMaxResults(1048576).list();

											XSSFWorkbook workbook = new XSSFWorkbook();
											XSSFSheet sheet = workbook.createSheet("DOSEN");
											sheet.setDefaultColumnWidth(20);
											int rowIndex = 0;

											XSSFRow rowhead = sheet.createRow((short) 0);
											rowhead.createCell(0).setCellValue("ID");
											rowhead.createCell(1).setCellValue("Username");
											rowhead.createCell(2).setCellValue("Password");
											rowhead.createCell(4).setCellValue("Prodi");
											rowhead.createCell(3).setCellValue("Fakultas");
											rowhead.createCell(5).setCellValue("Nama Lengkap");
											rowhead.createCell(6).setCellValue("Email");
											rowhead.createCell(7).setCellValue("HP");
											rowhead.createCell(8).setCellValue("Kode Install Mobile");

											for (Dosen dosen : dosens) {
												if (dosen.getNama() != null && !dosen.getNama().trim().isEmpty()) {
													rowIndex++;
													Session session = HibernateUtil.currentNativeSession();
													try {

														String nidn = dosen.getNidn();
														if (!menggunkanaNidn) {
															nidn = null;
														}

														Tbmuser tbmuser = (Tbmuser) session
																.createCriteria(Tbmuser.class)
																.add(Restrictions.or(Restrictions.isNull("aktif"),
																		Restrictions.eq("aktif", true)))
																.add(nidn == null || nidn.trim().isEmpty()
																		? Restrictions.sqlRestriction("true")
																		: Restrictions.eq("userId", nidn))
																.add(Restrictions.eq("dosen", dosen)).setMaxResults(1)
																.uniqueResult();
														if (tbmuser == null || tbmuser.getUserId() == null) {
															tbmuser = new Tbmuser();

															String newUsername = StringUtils.split(dosen.getNama(),
																	" ")[0] + "" + RandomStringUtils.randomNumeric(3);

															newUsername = newUsername.toLowerCase().trim();

															if (nidn != null && !nidn.trim().isEmpty()) {
																newUsername = nidn;
															}

															tbmuser.setUserId(newUsername);
															tbmuser.setEmail(dosen.getEmail());
//															tbmuser.setFakultas(dosen.getFakultas());
															tbmuser.setIs_encripted(true);
//															tbmuser.setJurusan(dosen.getJurusan());
															tbmuser.setRoot(false);
															tbmuser.setUserNama(dosen.getNama());
															String passw = RandomStringUtils.randomNumeric(5);
															if (nidn != null && !nidn.trim().isEmpty()) {
																passw = nidn;
															}

															tbmuser.setUserPassword(
																	Common.desEncrypter.get().encrypt(passw.trim()));
															tbmuser.setUserRole(ConstantValues.roleDosen);
															tbmuser.setUserShow(1);
															tbmuser.setDosen(dosen);

															session.getTransaction().begin();
															Common.refreshSaveOrUpdate(session, tbmuser);
															session.getTransaction().commit();

															Common.saveOrUpdateUserAccess(tbmuser, null,
																	tbmuser.getUserId(), passw.trim(),
																	tbmuser.getEmail());

														}

														XSSFRow row = sheet.createRow(rowIndex);
														row.createCell(0).setCellValue(dosen.getId());
														row.createCell(1).setCellValue(tbmuser.getUserId());

														try {
															row.createCell(2).setCellValue(Common.desEncrypter.get()
																	.decrypt(tbmuser.getUserPassword()));
														} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

														row.createCell(3)
																.setCellValue(tbmuser.ambilFakultas() == null ? ""
																		: tbmuser.ambilFakultas().getNama());
														row.createCell(4)
																.setCellValue(tbmuser.ambilJurusan() == null ? ""
																		: tbmuser.ambilJurusan().getNama());
														row.createCell(5).setCellValue(dosen.getNama());
														row.createCell(6).setCellValue(dosen.getEmail());
														row.createCell(7).setCellValue(dosen.getTelp());

														String hasil = "";
														try {

															String username = tbmuser.getUserId() + ";"
																	+ Common.getRequestHostWithProtocol();

															JSONObject postData = new JSONObject();
															postData.put("username", username);
															postData.put("link", link);
															postData.put("nama_pt", nama_pt);
															postData.put("login_bg_pt",
																	background_login_PerguruanTinggi);
															postData.put("bg_pt", background_PerguruanTinggi);
															postData.put("logo_pt", logo_PerguruanTinggi);
															postData.put("banner_pt", banner_PerguruanTinggi);

															postData.put("motto_pt", perguruanTinggi.getMotto());
															postData.put("alamat_pt", perguruanTinggi.getAlamat1());
															postData.put("telp_pt", perguruanTinggi.getTelepon());
															postData.put("email_pt", perguruanTinggi.getEmail());
															postData.put("action", "code");

															System.out.println("linkPost -> " + strURL);
															System.out.println("postData -> " + postData);

															String[] command = { "curl", "-d", postData.toString(),
																	"-H", "Content-Type: application/json", strURL };

															ProcessBuilder process = new ProcessBuilder(command);
															Process p;
															p = process.start();
															BufferedReader reader = new BufferedReader(
																	new InputStreamReader(p.getInputStream()));
															StringBuilder builder = new StringBuilder();
															String line = null;
															while ((line = reader.readLine()) != null) {
																builder.append(line);
																builder.append(System.getProperty("line.separator"));
															}
															hasil = builder.toString();

															System.out.println(hasil);

															JSONObject jsonObject = new JSONObject(hasil);

															row.createCell(8)
																	.setCellValue(jsonObject.isNull("code") ? ""
																			: jsonObject.get("code") + "");

														} catch (Exception e) {
															ais.common.Common.tampilErrorJikaAdmin(e);
														}

													} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
													HibernateUtil.closeSession();
												}
											}

											try {
												FileOutputStream fileOut = new FileOutputStream(filename);
												workbook.write(fileOut);
												fileOut.close();
											} catch (IOException e) {
												// TODO Auto-generated catch
												// block
												Common.tampilErrorJikaAdmin(e);
											}

											try {
												File file = new File(filename);
												Filedownload.save(new FileInputStream(file),
														"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
														file.getName());
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

										}
									});

								}

							}
						});

			}
		});
		Common.appendKeToolbar(generatePasswordDosen, add, comp);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		// Sembunyikan tombol "Upload Foto (NIM)" bila hak akses (baca+ubah+hapus) tak lengkap.
		ais.common.helper.UploadFotoMassalHelper.terapkanGateTombolUpload(self);

		boolean merupakanAdmin = (tbmuser != null && tbmuser != null && tbmuser.hakAkses() != null
				&& tbmuser.hakAkses() != null && tbmuser != null && tbmuser.hakAkses() != null
				&& tbmuser.hakAkses().getRoleId() != null && (tbmuser != null && tbmuser.hakAkses() != null
						&& Common.getCurrentUser().hakAkses().getRoleId().equalsIgnoreCase(Tbmrole.ADMINISTRATOR)));
		if (uploadRfid != null) {
			uploadRfid.setVisible(Common.bolehKonfigurasi("aktifkan_upload_rfid_dosen", Konfigurasi.TIDAK_AKTIF) && (add != null && add.isVisible()) && edit && merupakanAdmin);
			downloadRfid.setVisible(Common.bolehKonfigurasi("aktifkan_download_rfid_dosen", Konfigurasi.TIDAK_AKTIF) && (add != null && add.isVisible()) && edit && merupakanAdmin);
		}

		generatePasswordDosen.setVisible((add != null && add.isVisible()) && edit && tbmuser != null && tbmuser != null
				&& tbmuser.hakAkses() != null && tbmuser.hakAkses() != null && tbmuser != null
				&& tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null
				&& (tbmuser != null && tbmuser.hakAkses() != null
						&& Common.getCurrentUser().hakAkses().getRoleId().equalsIgnoreCase(Tbmrole.ADMINISTRATOR)));
		if (upload != null) { upload.setVisible(generatePasswordDosen.isVisible()); }

		if (tbmuser != null && tbmuser.ambilFakultas() != null) {

			Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());

			Common.clear(searchjurusan);

			Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", tbmuser.ambilFakultas()));

			// searchfakultas.setDisabled(true);
		} else {

			// searchfakultas.setDisabled(false);
		}

		if (tbmuser != null && tbmuser.ambilJurusan() != null) {

			Common.selectComboItem(searchjurusan, tbmuser.ambilJurusan());

			// searchjurusan.setDisabled(true);
		} else {

			// searchjurusan.setDisabled(false);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
		// uploadData.setVisible(add.isVisible() && edit);
		// downloadFormatDosen.setVisible(add.isVisible() && edit);
		// uploadDBF.setVisible(uploadData.isVisible());

		MyToolbarbuttonConfig singkron = new MyToolbarbuttonConfig("Singkronkan dg SINTA", "/img/favicon_sinta.png");
//		add.getParent().appendChild(singkron);
		singkron.setVisible(
				(add != null && add.isVisible()) && edit && perguruanTinggi != null && !perguruanTinggi.getKodeSinta().trim().isEmpty());

		singkron.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						MyMessageboxConfig.show("Singkron dengan data SINTA selesai dilakukan", "Informasi",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						onSearchDefault(arg0);
					}
				});

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							SintaPtCrawler.singkronkan(label, perguruanTinggi);
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
					}
				}).start();
			}
		});

		singkron = new MyToolbarbuttonConfig("Singkronkan dg pegawai", "/img/absensi_pmb.png");
		Common.appendKeToolbar(singkron, add, comp);
		if (singkron != null) { singkron.setVisible((add != null && add.isVisible()) && edit); }

		singkron.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Dosen> dosens = initCriteria(true).list();
						Session session = HibernateUtil.currentSession();
						// Laporan rinci per dosen (berhasil/gagal+penyebab teknis lengkap) - dulu
						// SATU error di mana pun (mis. FotoDosen/Tbmuser) mematikan seluruh loop
						// TANPA jejak (dosen sesudahnya tak pernah ikut diproses & tak ada yang
						// tahu). Sekarang tiap dosen dibungkus try/catch sendiri.
						ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Dosen dengan Pegawai");
						int nomorBaris = 0;
						for (Dosen dosen : dosens) {
							String kunci = dosen == null || dosen.getNama() == null ? "-" : dosen.getNama();
							try {
							if (!dosen.getNama().trim().isEmpty()) {
								Pegawai pegawai = (Pegawai) (session.createCriteria(Pegawai.class)

										.add(Restrictions.eq("dosen", dosen)).setMaxResults(1).uniqueResult());
								if (pegawai == null) {
									pegawai = new Pegawai();
									pegawai.setDosen(dosen);
									session.save(pegawai);

									Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
									FotoDosen fotoDosen = (FotoDosen) streamingSession.createCriteria(FotoDosen.class)
											.add(Restrictions.eq("dosen", dosen.getId())).setMaxResults(1)
											.uniqueResult();
									if (fotoDosen != null) {
										FotoPegawai fotoPegawai = new FotoPegawai();
										fotoPegawai.setNama(fotoDosen.getNama());
										fotoPegawai.setKeterangan(fotoDosen.getKeterangan());
										fotoPegawai.setPegawai(pegawai.getId());
										fotoPegawai.setFoto(fotoDosen.getFoto());

										streamingSession.getTransaction().begin();
										streamingSession.save(fotoPegawai);
										streamingSession.getTransaction().commit();
									}
									StreamingHibernateUtil.getInstance().closeSession();
								}

								try {
									Tbmuser tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.eq("dosen", dosen)).setMaxResults(1).uniqueResult();
									if (tbmuser == null || tbmuser.getUserId() == null) {
										tbmuser = new Tbmuser();

										String newUsername = StringUtils.split(dosen.getNama(), " ")[0] + ""
												+ RandomStringUtils.randomNumeric(3);

										newUsername = newUsername.toLowerCase().trim();

										tbmuser.setUserId(newUsername);
										tbmuser.setEmail(dosen.getEmail());
										tbmuser.setFakultas(dosen.getFakultas());
										tbmuser.setIs_encripted(true);
										tbmuser.setJurusan(dosen.getJurusan());
										tbmuser.setRoot(false);
										tbmuser.setUserNama(dosen.getNama());
										String passw = RandomStringUtils.randomNumeric(5);
										tbmuser.setUserPassword(Common.desEncrypter.get().encrypt(passw.trim()));
										tbmuser.setUserRole(ConstantValues.roleDosen);
										tbmuser.setUserShow(1);
										tbmuser.setDosen(dosen);
										tbmuser.setPegawai(pegawai);
										Common.refreshSaveOrUpdate(session, tbmuser);

										Common.saveOrUpdateUserAccess(tbmuser, null, tbmuser.getUserId(), passw.trim(),
												tbmuser.getEmail());

									} else {
										tbmuser.setPegawai(pegawai);
										Common.refreshSaveOrUpdate(session, tbmuser);
									}
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

								if (dosen.getPegawaiId() == null || !dosen.getPegawaiId().equals(pegawai.getId())) {
									dosen.setPegawaiId(pegawai.getId());
									Common.refreshSaveOrUpdate(session, dosen);
								}
							}
							laporan.catatBerhasil(nomorBaris, kunci, "Sinkronisasi berhasil");
							} catch (Exception eDosen) {
								ais.common.ErrorAuditUtil.record(eDosen, "auto-audit src/ais/action/master/DosenAction.java:singkronPegawaiBaris");
								laporan.catatGagalDetail(nomorBaris, kunci, eDosen);
							}
							nomorBaris++;
						}

						laporan.selesaikan(null);
					}
				});
			}
		});

		// Tombol "Download Foto" lama DIHAPUS — digantikan "Download Foto Massal" (bulk ZIP,
		// multi-thread + bar progres) agar seragam. Handler onDownloadFoto lama dibiarkan (tak dipakai).

		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Ambil dari feeder",
					"/img/Button-Refresh-icon.png");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin mengambil data dari feeder ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										String[] kon = EksporFromFeederAction.koneksi();
										final String ip = kon[0];
										final String port = kon[1];
										final String username = kon[2];
										final String password = kon[3];
										final String url = kon[4];

										if (!EksporFromFeederAction.exists(url)) {

											MyMessageboxConfig.show(
													ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."),
													"Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);
											return;
										}

										final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												if (arg0 != null && !arg0.getName().isEmpty()) {
													EksporFromFeederAction.display();
													MyMessageboxConfig.show(arg0.getName(), "Info",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}
												onSearchDefault(null);
											}
										});

										new Thread(new Runnable() {

											@Override
											public void run() {
												try {
													FeederConnector feederConnector = new FeederConnector(ip,
															Integer.parseInt(port), null);

													String token = feederConnector.getToken(username, password);
													System.out.println("TOKEN => " + token);

													if (token == null || token.trim().isEmpty()
															|| token.trim().toLowerCase().startsWith("error")) {
														myLabelProsesDetail
																.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
														return;
													}

													String or = "";
													Integer countInteger = feederConnector.getCount(token,
															"GetCountDosen", "");

													for (int index = 0; index <= countInteger; index += 500) {

														JSONArray dataDetailBiodataDosen = feederConnector.getData(
																"DetailBiodataDosen", token, or, "", "500", index + "");

														for (int i = 0; i < dataDetailBiodataDosen.length(); i++) {
															JSONObject dosen = dataDetailBiodataDosen.getJSONObject(i);
															FeederJSONImport.dosen(dosen);

															JSONArray dataPenugasanDosen = feederConnector.getData(
																	"GetListPenugasanDosen", token,
																	"id_dosen='" + dosen.getString("id_dosen") + "'",
																	"id_tahun_ajaran", "5000", "0");

															for (int j = 0; j < dataPenugasanDosen.length(); j++) {
																JSONObject penugasanDosen = dataPenugasanDosen
																		.getJSONObject(j);
																FeederJSONImport.dosen_pt(penugasanDosen);
															}
														}

													}

													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													// FIX "gagal diam-diam": sebelumnya exception di sini (mis. gagal
													// konek/parse port/parse data) hanya dicatat ke log admin lalu
													// progres diset "" (=SUKSES palsu) di luar try, menutupi kegagalan
													// dari pengguna (popup dianggap selesai normal padahal gagal).
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue(
															"Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																	"pengambilan data Dosen dari Neo Feeder",
																	null, e,
																	new String[] {
																			"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
																			"Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
																			"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
																	.replace("\n", " "));
												}
											}
										}).start();

									}

								}
							});

				}
			});
			Common.appendKeToolbar(buttonTagihan, add, comp);
		}
	        FilterLanjutHelper.setup(comp);
}

	@SuppressWarnings("unchecked")
	public void onDownloadFoto(Event event) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Dosen> calonDosen = initCriteria(true).list();
				File fileFolderLampiran = new File(
						"/opt/ecampus/foto_" + ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis());
				fileFolderLampiran.mkdirs();
				System.out.println("fileFolderLampiran => " + fileFolderLampiran.getAbsolutePath());

				for (Dosen dosen : calonDosen) {

					try {
						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

						FotoDosen fotodosen = (FotoDosen) streamingSession.createCriteria(FotoDosen.class)
								.add(Restrictions.eq("dosen", dosen.getId())).setMaxResults(1).uniqueResult();

						if (fotodosen != null) {
							File fileFoto = fotodosen.ambilFile();
							File fileCopy = new File(fileFolderLampiran.getAbsolutePath() + "/" + dosen.getNidn() + "_"
									+ dosen.getNama() + "_" + fileFoto.getName());
							System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
							FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
							FileInputStream fileInputStream = new FileInputStream(fileFoto);
							IOUtils.copyLarge(fileInputStream, fileOutputStream);
							fileInputStream.close();
							fileOutputStream.close();
						}

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e1) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/DosenAction.java:1362");
					}

				}

				File fileFolderLampiranZip = new File(fileFolderLampiran.getAbsolutePath() + ".zip");
				Common.zipDir(fileFolderLampiranZip.getAbsolutePath(), fileFolderLampiran.getAbsolutePath());
				Filedownload.save(fileFolderLampiranZip, "application/zip");

			}
		}, "Harap tunggu.. sedang melakukan proses download foto..");

	}

	// private MyToolbarbuttonConfig uploadData;
	// private MyToolbarbuttonConfig downloadFormatDosen;

	public void onLaporanSkDosen(Event event) {

		if (laporanSkDosen.getChildren().size() == 0) {
			LaporanSKDosen laporanDaftarHadirDosen = new LaporanSKDosen();
			laporanDaftarHadirDosen.setHeight("100%");
			laporanDaftarHadirDosen.setWidth("100%");
			laporanDaftarHadirDosen.setParent(laporanSkDosen);
		}
	}

	public void onLaporanPerDosen(Event event) {

		if (laporanPerDosen.getChildren().size() == 0) {
			LaporanDaftarHadirDosen laporanDaftarHadirDosen = new LaporanDaftarHadirDosen();
			laporanDaftarHadirDosen.setHeight("100%");
			laporanDaftarHadirDosen.setWidth("100%");
			laporanDaftarHadirDosen.setParent(laporanPerDosen);
		}
	}

	public void onLaporanDosenHarian(Event event) {

		if (laporanDosenHarian.getChildren().size() == 0) {
			LaporanDaftarHadirDosenHarian laporanDaftarHadirDosenHarian = new LaporanDaftarHadirDosenHarian();
			laporanDaftarHadirDosenHarian.setHeight("100%");
			laporanDaftarHadirDosenHarian.setWidth("100%");
			laporanDaftarHadirDosenHarian.setParent(laporanDosenHarian);
		}
	}

	public void onLaporanRuangDosen(Event event) {

		if (ruangDosen.getChildren().size() == 0) {
			LaporanRuanganDosen laporanRuanganDosen = new LaporanRuanganDosen();
			laporanRuanganDosen.setHeight("100%");
			laporanRuanganDosen.setWidth("100%");
			laporanRuanganDosen.setParent(ruangDosen);
		}
	}

	public void onRekapDosenTetap(Event event) {

		if (laporanRekapDosenTetap.getChildren().size() == 0) {
			LaporanRekapDosenTetap laporanRekapDosenTetap = new LaporanRekapDosenTetap();
			laporanRekapDosenTetap.setHeight("100%");
			laporanRekapDosenTetap.setWidth("100%");
			laporanRekapDosenTetap.setParent(this.laporanRekapDosenTetap);
		}
	}

	public void onRekapDosenTetapPerFakultasDanPerProdi(Event event) {

		if (laporanRekapDosenTetapPerFakultasDanPerProdi.getChildren().size() == 0) {
			LaporanRekapDosenPerFakultasDanProdi laporanRekapDosenTetap = new LaporanRekapDosenPerFakultasDanProdi();
			laporanRekapDosenTetap.setHeight("100%");
			laporanRekapDosenTetap.setWidth("100%");
			laporanRekapDosenTetap.setParent(this.laporanRekapDosenTetapPerFakultasDanPerProdi);
		}
	}

	public void onLaporanRekapDosenPerFakultasDanPerProdi(Event event) {

		if (laporanRekapDosenPerFakultasDanPerProdi.getChildren().size() == 0) {
			LaporanRekapDosenPerFakultasDanPerProdi laporanRekapDosenTetap = new LaporanRekapDosenPerFakultasDanPerProdi();
			laporanRekapDosenTetap.setHeight("100%");
			laporanRekapDosenTetap.setWidth("100%");
			laporanRekapDosenTetap.setParent(this.laporanRekapDosenPerFakultasDanPerProdi);
		}
	}

	public void onRekapDosenTidakTetap(Event event) {

		if (laporanRekapDosenTidakTetap.getChildren().size() == 0) {
			LaporanRekapDosenTidakTetap laporanDaftarHadirDosen = new LaporanRekapDosenTidakTetap();
			laporanDaftarHadirDosen.setHeight("100%");
			laporanDaftarHadirDosen.setWidth("100%");
			laporanDaftarHadirDosen.setParent(laporanRekapDosenTidakTetap);
		}
	}

	public void onRekapJumlahDosen(Event event) {

		if (laporanRekapJumlahDosen.getChildren().size() == 0) {
			LaporanRekapJumlahDosen laporanDaftarHadirDosen = new LaporanRekapJumlahDosen();
			laporanDaftarHadirDosen.setHeight("100%");
			laporanDaftarHadirDosen.setWidth("100%");
			laporanDaftarHadirDosen.setParent(laporanRekapJumlahDosen);
		}
	}

	public void onRekapJumlahDosenPerPendidikan(Event event) {

		if (laporanRekapJumlahDosenPerPendidikan.getChildren().size() == 0) {
			LaporanRekapJumlahDosenPerPendidikan laporanDaftarHadirDosen = new LaporanRekapJumlahDosenPerPendidikan();
			laporanDaftarHadirDosen.setHeight("100%");
			laporanDaftarHadirDosen.setWidth("100%");
			laporanDaftarHadirDosen.setParent(laporanRekapJumlahDosenPerPendidikan);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void cetakDRHDosen(final Dosen dosen) throws Exception {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentNativeSession();
				Pegawai pegawai = Pegawai.createDataPegawaiDariDosen(session, dosen);
				HibernateUtil.closeSession();
				Map parameters = ais.common.HashMapGenerator.getRand();
				Common.insertProperty(Pegawai.class, pegawai, parameters, "", 2);
				Common.insertProperty(Dosen.class, dosen, parameters, "", 2);
				GajiPokok gajiPokok = pegawai.ambilGajiPokok(WaktuUtil.getDate());
				if (gajiPokok != null) {
					Common.insertProperty(GajiPokok.class, gajiPokok, parameters, "gp", 1);
				}
				parameters.put("id", dosen.getId());
				parameters.put("peg_id", pegawai.getId());
				parameters.put("dos_id", dosen.getId());
				dosen.putPhoto(parameters);
				Report.generatePDFReport(Report.PDF, parameters, "employ/daftar_riwayat_hidup_dosen",
						ais.ui.util.WaktuUtil.getDate());
			}
		});
	}

//	@SuppressWarnings("unchecked")
//	Map<Serializable, Tbmuser> map = ConstantValues.ambilBerdasarClass(Tbmuser.class);

	/**
	 * Renderer lokal untuk layar/komponen {@link DosenAction}. Kelas ini menerjemahkan satu item data menjadi
	 * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DosenAction} dan dapat mengakses state kelas
	 * induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see DosenAction
	 */
	class DosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Dosen dosen = (Dosen) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						session.setAttribute("selectedDosen", dosen);
						MyInclude include = new MyInclude(
								"/pages/master/informasi_jadwal_ajar_dosen.zul?dosen=" + dosen.getId());
						include.setHeight("700px");
						include.setWidth("100%");
						detail.appendChild(include);
					}
				}
			});

			CommonMedia.tampilkanGambarKecil(dosen).setParent(arg0);
			Vbox a = new Vbox();
			a.setParent(arg0);
			new MyLabelKecil(dosen.getMycode() == null ? "" : dosen.getMycode()).setParent(a);
			new MyLabelKecil(dosen.getCode() == null ? "" : dosen.getCode()).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			new MyLabelKecil(dosen.getNidn() == null ? "" : dosen.getNidn()).setParent(a);
			new MyLabelKecil(dosen.getNuptk() == null ? "" : dosen.getNuptk()).setParent(a);

			a = RevisiHelper.createNewRevisi(Dosen.class, dosen, dosen.getNama());
			a.setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(a);

			Hbox myHbox = new Hbox();
			myHbox.setParent(vbox);
			Tbmuser u = Common.getCurrentUser();
			if (u != null && Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
				if (dosen.getFeeder() != null && !dosen.getFeeder().trim().isEmpty()) {
					myHbox.appendChild(new Image("/img/svg/check2-circle.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder valid"));
				} else {
					myHbox.appendChild(new Image("/img/svg/warning-outline.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
				}
			}

//			String socialMediaProfile = "";
//			for (Tbmuser tbmuser : map.values()) {
//				if (tbmuser != null && tbmuser.getAktif() && tbmuser.ambilDosen() != null
//						&& tbmuser.getDosen().getId() != null && tbmuser.getDosen().getId().equals(dosen.getId())) {
//					socialMediaProfile = socialMediaProfile.isEmpty() ? tbmuser.getSocialMediaProfile()
//							: "||" + tbmuser.getSocialMediaProfile();
//				}
//			}
//
//			TbmuserAction.tampilkanSocialMediaProfile(vbox, socialMediaProfile);

			new Label(dosen.getStatusPegawai() == null ? "" : dosen.getStatusPegawai().getNama()).setParent(arg0);
			new Label(dosen.getGolongan()).setParent(arg0);
			new Label((dosen.getIkatanKerjaDosen() == null ? "" : dosen.getIkatanKerjaDosen().getNama()) + " "
					+ (dosen.getStatusKepegawaian() == null ? "" : dosen.getStatusKepegawaian().getNama()))
					.setParent(arg0);
			new Label(dosen.getEmail()).setParent(arg0);
			new Label(dosen.getTelp()).setParent(arg0);
			// new Label(dosen.getAlamat()).setParent(arg0);

			new Label(dosen.getPangkat()).setParent(arg0);
			new Label(dosen.getJabatan()).setParent(arg0);

			new Label(dosen.getFakultas() == null ? "" : dosen.getFakultas().getNama()).setParent(arg0);

			new Label(dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama()).setParent(arg0);

			Dosen atasan = dosen.getAtasanlangsung() == null ? null
					: (Dosen) ConstantValues.ambil(Dosen.class.getName(), dosen.getAtasanlangsung());
			Vbox atasanVbox = new Vbox();
			atasanVbox.setWidth("90%");
			atasanVbox.setParent(arg0);
			final Vbox fotoDanNama = new Vbox();
			fotoDanNama.setWidth("100%");
			fotoDanNama.setParent(atasanVbox);
			if (atasan != null) {
				CommonMedia.tampilkanGambarKecil(atasan).setParent(fotoDanNama);

			}
			final AmbilDataDosenBanbox atasanlangsung;
			atasanVbox.appendChild(atasanlangsung = new AmbilDataDosenBanbox(false));
			atasanlangsung.setAttribute("dosen", atasan);
			atasanlangsung.setValue(atasan == null ? "" : atasan.getNama());
			atasanlangsung.setCols(4);
			atasanlangsung.setReadonly(true);
			atasanlangsung.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(fotoDanNama);

					Dosen atasanBaru = (Dosen) atasanlangsung.getAttribute("dosen");
					if (atasanBaru != null) {
						CommonMedia.tampilkanGambarKecil(atasanBaru).setParent(fotoDanNama);
					}

					dosen.setAtasanlangsung(atasanBaru == null ? null : atasanBaru.getId());
					Common.refreshUpdate(dosen);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Tampil");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(dosen.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					dosen.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(dosen);
				}
			});

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Daftar Riwayat Hidup");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					DosenAction.cetakDRHDosen(dosen);
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					biodataDosenAction = new BiodataDosenAction(dosen);
					biodataDosenAction.setCommonOnSearchdefault(DosenAction.this);
					biodataDosenAction.setHeight("99%");
					biodataDosenAction.setWidth("90%");
					page.getFirstRoot().appendChild(biodataDosenAction);
					biodataDosenAction.setVisible(true);
					biodataDosenAction.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setTooltiptext("Hapus data");
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
											DosenDao dosenDao = DaoFactory.getInstance().getDosenDao();

											String sql1 = "delete from log_user_actifity where detail_log_login in (select id from detail_log_login where log_login in (select id from log_login where dosen in (select id from dosen where id = "
													+ dosen.getId() + ")));";
											dosenDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from log_user_actifity where detail_log_login in (select id from detail_log_login where log_login in (select id from log_login where dosen in (select id from dosen where id = "
													+ dosen.getId() + ")));";
											dosenDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from detail_log_login where log_login in (select id from log_login where dosen in (select id from dosen where id = "
													+ dosen.getId() + "));";
											dosenDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from log_login where dosen in (select id from dosen where id = "
													+ dosen.getId() + ");";
											dosenDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from log_user_actifity where detail_log_login in (select id from detail_log_login where log_login in (select id from log_login where tbmuser in (select userid from tbmuser where dosen = "
													+ dosen.getId() + " )));";

											dosenDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from log_user_actifity where detail_log_login in (select id from detail_log_login where log_login in (select id from log_login where tbmuser in (select userid from tbmuser where dosen = "
													+ dosen.getId() + " )));";
											dosenDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from detail_log_login where log_login in (select id from log_login where tbmuser in (select userid from tbmuser where dosen = "
													+ dosen.getId() + " ));";
											dosenDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from log_login where tbmuser in (select userid from tbmuser where dosen = "
													+ dosen.getId() + " );";
											dosenDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from tbmuser where dosen = " + dosen.getId() + ";";
											dosenDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											BiodataDosen biodataDosen = (BiodataDosen) dosenDao.getCurrentSession()
													.createCriteria(BiodataDosen.class)
													.add(Restrictions.eq("dosen", dosen)).setMaxResults(1)
													.uniqueResult();
											if (biodataDosen != null) {
												dosenDao.getCurrentSession().delete(biodataDosen);
											}

											Common.refreshDelete(dosen);

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

	/**
	 * Unggah FOTO MASSAL dosen: pilih banyak berkas foto (nama berkas = NIDN) ATAU satu berkas ZIP;
	 * setiap foto langsung dipasang ke dosen yang cocok, lalu ringkasan ditampilkan.
	 */
	/** Unduh SEMUA foto dosen sebagai satu ZIP (baca BLOB paralel maks 50 thread). Tombol "Download Foto Massal". */
	public void onDownloadFotoMassal(Event event) throws Exception {
		ais.common.helper.DownloadFotoMassalHelper.downloadFotoDosenMassal();
	}

	public void onUploadFotoMassal(Event event) throws Exception {
		if (!ais.common.helper.UploadFotoMassalHelper.bolehUploadMassal()) {
			ais.ui.util.MyMessageboxConfig.show(
					"Anda tidak memiliki hak akses (baca, ubah, dan hapus) untuk mengunggah foto.", "Akses Ditolak",
					ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.EXCLAMATION);
			return;
		}
		org.zkoss.zk.ui.event.ForwardEvent forwardEvent = (org.zkoss.zk.ui.event.ForwardEvent) event;
		org.zkoss.zk.ui.event.UploadEvent uploadEvent = (org.zkoss.zk.ui.event.UploadEvent) forwardEvent.getOrigin();
		org.zkoss.util.media.Media[] medias = uploadEvent.getMedias();
		java.util.List<org.zkoss.util.media.Media> daftar = new java.util.ArrayList<org.zkoss.util.media.Media>();
		if (medias != null) {
			for (org.zkoss.util.media.Media m : medias) {
				if (m != null) {
					daftar.add(m);
				}
			}
		} else if (uploadEvent.getMedia() != null) {
			daftar.add(uploadEvent.getMedia());
		}
		if (daftar.isEmpty()) {
			ais.ui.util.MyMessageboxConfig.show("Tidak ada berkas foto yang dipilih.", "Informasi",
					ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
			return;
		}
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Foto Massal Dosen");
		for (int i = 0; i < daftar.size(); i++) {
			org.zkoss.util.media.Media m = daftar.get(i);
			String nama = (m != null && m.getName() != null && !m.getName().trim().isEmpty()) ? m.getName() : ("berkas-" + (i + 1));
			if (m != null && m.isBinary() && m.getName() != null && !m.getName().trim().isEmpty()) {
				report.sukses(i + 1, nama, "diajukan untuk upload");
			} else {
				report.gagal(i + 1, nama, "Berkas tidak valid (bukan biner atau nama berkas kosong)", "Pastikan berkas berupa gambar jpg/png dengan nama = NIDN dosen");
			}
		}
		int[] hasil = ais.common.helper.UploadFotoMassalHelper.uploadFotoDosenByNim(daftar);
		try {
			Filedownload.save(report.simpanLaporan(), "text/plain");
		} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) DosenAction laporan foto massal"); }
		ais.ui.util.MyMessageboxConfig.show(ais.common.helper.UploadFotoMassalHelper.ringkasan(hasil),
				"Upload Foto Massal", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
	}

	public void onAdd(Event event) throws Exception {
		biodataDosenAction = new BiodataDosenAction(new Dosen());
		biodataDosenAction.setCommonOnSearchdefault(DosenAction.this);
		biodataDosenAction.setHeight("90%");
		biodataDosenAction.setWidth("850px");
		page.getFirstRoot().appendChild(biodataDosenAction);
		biodataDosenAction.setVisible(true);
		biodataDosenAction.onModal();
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Dosen.class)

				.add(perguruanTinggi == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("perguruanTinggi", perguruanTinggi))

				.add(selectedJurusan != null ? Restrictions.eq("jurusan.id", selectedJurusan)
						: Restrictions.sqlRestriction("1=1"))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.asc("nama"));

		criteria.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
				? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("statusPegawai", searchstatus.getSelectedItem().getValue()))

				.add(searchLembagaPengangkat == null || searchLembagaPengangkat.getSelectedItem() == null
						|| searchLembagaPengangkat.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("lembagaPengangkat",
										searchLembagaPengangkat.getSelectedItem().getValue()))

				.add(searchJabatanFungsionalDosen == null || searchJabatanFungsionalDosen.getSelectedItem() == null
						|| searchJabatanFungsionalDosen.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jabatanFungsionalDosen",
										searchJabatanFungsionalDosen.getSelectedItem().getValue()))

				.add(searchJenisPendidikDanTenagaKependidikan == null
						|| searchJenisPendidikDanTenagaKependidikan.getSelectedItem() == null
						|| searchJenisPendidikDanTenagaKependidikan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisPendidikDanTenagaKependidikan",
										searchJenisPendidikDanTenagaKependidikan.getSelectedItem().getValue()))

				.add(searchIkatanKerjaDosen == null || searchIkatanKerjaDosen.getSelectedItem() == null
						|| searchIkatanKerjaDosen.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("ikatanKerjaDosen",
										searchIkatanKerjaDosen.getSelectedItem().getValue()))

				.add(searchGolonganPegawai == null || searchGolonganPegawai.getSelectedItem() == null
						|| searchGolonganPegawai.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("golonganPns", searchGolonganPegawai.getSelectedItem().getValue()))

				.add(Restrictions.or(
						searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false),
						searchjugaTampilkanDosenYgBolehMengajarProdiLain != null
								&& searchjugaTampilkanDosenYgBolehMengajarProdiLain.isChecked()
										? Restrictions.eq("milikUniversitas", true)
										: Restrictions.sqlRestriction("false")))

				.add(Restrictions.or(
						searchjugaTampilkanDosenYgBolehMengajarProdiLain != null
								&& searchjugaTampilkanDosenYgBolehMengajarProdiLain.isChecked()
										? Restrictions.eq("milikUniversitas", true)
										: Restrictions.sqlRestriction("false"),
						searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

				.add(searchnama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))

				.add(searchcode.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1") :

						Restrictions.or(Restrictions.ilike("mycode", searchcode.getValue(), MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("nidn", searchcode.getValue(), MatchMode.ANYWHERE),
										Restrictions.ilike("code", searchcode.getValue(), MatchMode.ANYWHERE))));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Dosen> dosen = ConstantValues
				.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE).setFirstResult(
								Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
						Dosen.class);
		ListModel strset = new SimpleListModel(dosen);
		grid.setRowRenderer(new DosenRenderer());
		grid.setModelCheckMobile(strset);

		if (biodataDosenAction != null) {
			biodataDosenAction.detach();
		}

	}

}
