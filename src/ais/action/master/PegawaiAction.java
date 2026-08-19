package ais.action.master;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.employ.helper.MasaKerjaUtil;
import ais.action.master.employ.util.FormBiodataPegawaiUtil;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataPegawaiBanyak;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sekolah.GuruAction;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanKartuPegawai;
import ais.action.report.format1.payroll.LaporanPegawaiData;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonOnSearchdefault;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.PegawaiDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.JenisTenagaKependidikan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogLogin;
import ais.database.model.Pegawai;
import ais.database.model.PerguruanTinggi;
import ais.database.model.StatusPegawai;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.employ.GajiPokok;
import ais.database.model.file.FotoDosen;
import ais.database.model.file.FotoGuru;
import ais.database.model.file.FotoPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Guru;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

public class PegawaiAction extends GenericAutowireComposer
		implements CommonOnSearchdefault, DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyGrid grid;
	private Paging paging;
	private Textbox searchcode;
	private Textbox searchnama;
	private Combobox searchstatus;
	private Combobox searchPernahAbsen;
	private Checkbox searchbukanDosen;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private Checkbox searchaktif;
	private Checkbox searchnonaktif;

	private MyToolbarbuttonConfig add;
	private MyToolbarbuttonConfig addDosen;
	private MyToolbarbuttonConfig addGuru;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;
	private BiodataPegawaiAction biodataPegawaiAction = null;
	private BiodataDosenAction biodataDosenAction = null;

	private MyToolbarbuttonConfig downloadRfid;
	private MyToolbarbuttonConfig uploadRfid;

	private PerguruanTinggi perguruanTinggi;
	private StatusPegawai statusPegawai = null;

	public PegawaiAction() {
		super();
	}

	public PegawaiAction(StatusPegawai statusPegawai) {
		super();
		this.statusPegawai = statusPegawai;
	}

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

			for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
				Session session = HibernateUtil.currentNativeSession();
				try {

					String id = Common.getCellContent(Common.getCell(sheet, 0, i));
					String rfid = Common.getCellContent(Common.getCell(sheet, 1, i));

					if (id == null || id.trim().isEmpty()) {
						continue;
					}

					Pegawai pegawai = (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), Long.parseLong(rfid));
					System.out.println("id = " + id + " rfid = " + rfid + ", pegawai = " + pegawai);
					if (pegawai != null) {
						session.refresh(pegawai);
						pegawai.setIdfinger(rfid);
						session.getTransaction().begin();
						session.update(pegawai);
						session.getTransaction().commit();
					}

				} catch (Exception e) {
					session.getTransaction().rollback();
					Common.tampilErrorJikaAdmin(e);
					// Common.tampilErrorJikaAdmin(e);
				}

				HibernateUtil.closeSession();
			}

			MyMessageboxConfig.show("Update rfid berhasil dilakukan", "Pemberitahuan", MyMessageboxConfig.OK,
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
				for (Object[] pegawai : objects) {
					try {
						XSSFRow row = sheet.createRow(rowIndex);
						row.createCell(0).setCellValue(pegawai[0] + "");
						row.createCell(1).setCellValue(pegawai[1] + "");
						row.createCell(2).setCellValue(pegawai[2] + "");
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

	private SatuanKerja satuanKerjaOnSession;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Tabpanel manajemenKartuPegawai;

	public void onKartuPegawai(Event event) {
		if (manajemenKartuPegawai.getChildren().size() == 0) {
			LaporanKartuPegawai laporan = new LaporanKartuPegawai();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(manajemenKartuPegawai);
		}
	}

	private Tabpanel manajemenSatuanKerja;

	public void onSatuanKerja(Event event) {
		if (manajemenSatuanKerja.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenSatuanKerja);
			MyInclude iframe = new MyInclude("/pages/master/satuan_kerja_pegawai.zul");
			iframe.setParent(window);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public static String[] columns = FormBiodataPegawaiUtil.DATA;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}
		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Tbmuser tbmuser = Common.getCurrentUser();

		boolean[] ptYa = Common.chekPtAtauSekolah();
		boolean pt = ptYa[0];
		boolean ya = ptYa[1];

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (session.getAttribute("satuanKerjaOnSession") != null) {
			satuanKerjaOnSession = (SatuanKerja) session.getAttribute("satuanKerjaOnSession");
			session.removeAttribute("satuanKerjaOnSession");
		}

		SatuanKerja satuanKerjaData = satuanKerjaOnSession;
		if (satuanKerjaData != null && tbmuser != null && tbmuser.hakAkses() != null
				&& !tbmuser.hakAkses().getMelihatDataSatkerLain()) {
			searchparent.setValue(satuanKerjaData.getNama());
			searchparent.setAttribute("satuanKerja", satuanKerjaData);
			searchparent.setAttribute("myValue", satuanKerjaData);
			searchparent.setDisabled(true);
		}

		if (execution.getParameter("satuan_kerja") != null) {
			satuanKerjaOnSession = (SatuanKerja) HibernateUtil.currentSession().createCriteria(SatuanKerja.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("satuan_kerja")))).uniqueResult();
		}

		if (add != null) { add.setVisible(statusPegawai == null && CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE)); }
		if (add != null) { add.setTooltiptext("Tambah"); }

		if (addDosen != null) {
			addDosen.setVisible(pt && (add != null && add.isVisible()));
			addGuru.setVisible(ya && (add != null && add.isVisible()));
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		// Sembunyikan tombol "Upload Foto (NIM)" bila hak akses (baca+ubah+hapus) tak lengkap.
		ais.common.helper.UploadFotoMassalHelper.terapkanGateTombolUpload(self);
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);

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

		if (statusPegawai == null) {

			boolean merupakanAdmin = (tbmuser != null && tbmuser != null && tbmuser.hakAkses() != null
					&& tbmuser.hakAkses() != null && tbmuser != null && tbmuser.hakAkses() != null
					&& tbmuser.hakAkses().getRoleId() != null && (tbmuser != null && tbmuser.hakAkses() != null
							&& Common.getCurrentUser().hakAkses().getRoleId().equalsIgnoreCase(Tbmrole.ADMINISTRATOR)));
			if (uploadRfid != null) {
				uploadRfid.setVisible(Common.bolehKonfigurasi("aktifkan_upload_rfid_pegawai", Konfigurasi.TIDAK_AKTIF) && (add != null && add.isVisible()) && edit && merupakanAdmin);
				downloadRfid.setVisible(Common.bolehKonfigurasi("aktifkan_download_rfid_pegawai", Konfigurasi.TIDAK_AKTIF) && (add != null && add.isVisible()) && edit && merupakanAdmin);
			}
			if (searchbukanDosen != null) {
				searchbukanDosen.setVisible(pt);
			}

			Session session = HibernateUtil.currentSession();
			int count = ((Number) session.createCriteria(JenisTenagaKependidikan.class)
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (count == 0) {
				JenisTenagaKependidikan jenisTenagaKependidikan = new JenisTenagaKependidikan();
				jenisTenagaKependidikan.setNama("Pustakawan");
				jenisTenagaKependidikan.setKeterangan("Pustakawan");
				session.save(jenisTenagaKependidikan);

				jenisTenagaKependidikan = new JenisTenagaKependidikan();
				jenisTenagaKependidikan.setNama("Laboran/Teknisi/Analis/Operator/Programer");
				jenisTenagaKependidikan.setKeterangan("Laboran/Teknisi/Analis/Operator/Programer");
				session.save(jenisTenagaKependidikan);

				jenisTenagaKependidikan = new JenisTenagaKependidikan();
				jenisTenagaKependidikan.setNama("Administrasi");
				jenisTenagaKependidikan.setKeterangan("Administrasi");
				session.save(jenisTenagaKependidikan);

				jenisTenagaKependidikan = new JenisTenagaKependidikan();
				jenisTenagaKependidikan.setNama("Lainnya");
				jenisTenagaKependidikan.setKeterangan("Lainnya");
				session.save(jenisTenagaKependidikan);
			}

			Common.insertComboDanSemua(searchstatus, "nama", StatusPegawai.class, Restrictions.eq("aktif", true));

			List<String> columnHeadersAdding = new ArrayList<String>();
			columnHeadersAdding.add("Username");
			columnHeadersAdding.add("Jumlah Login");
			columnHeadersAdding.add("Sejarah Login");

			EventListener dataAdding = new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					Object[] objects = (Object[]) arg0.getData();
					Pegawai pegawai = (Pegawai) objects[0];

					XSSFRow row = (XSSFRow) objects[2];

					Session session = HibernateUtil.currentNativeSession();

					List<String> usernames = session.createCriteria(Tbmuser.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.isNotNull("userId")).add(Restrictions.ne("userId", ""))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("pegawai", pegawai)).setProjection(Projections.property("userId"))
							.list();
					String u = "";
					for (String user : usernames) {
						u += u.trim().isEmpty() ? user : "," + user;
					}

					Object[] logLogins = (Object[]) session.createCriteria(LogLogin.class)
							.add(Restrictions.eq("pegawai", pegawai)).add(Restrictions.isNotNull("login"))
							.setProjection(Projections.projectionList().add(Projections.max("login"))
									.add(Projections.rowCount()))
							.uniqueResult();

					Date maxLogin = (Date) (logLogins == null || logLogins.length == 0 || logLogins[0] == null ? null
							: logLogins[0]);
					Integer countLogin = ((Number) (logLogins == null || logLogins.length == 0 || logLogins[1] == null
							? 0
							: logLogins[1])).intValue();

					row.createCell(columns.length + 0).setCellValue(u);

					row.createCell(columns.length + 1).setCellValue(countLogin);

					row.createCell(columns.length + 2)
							.setCellValue(maxLogin == null ? "" : Common.dateFormat3.get().format(maxLogin));

					HibernateUtil.closeSession();
				}
			};

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Pegawai.class, this,
					"Download Pegawai", "/img/print.png", columnHeadersAdding, dataAdding, false, null, "DATA TAMBAHAN",
					columns);
			Common.appendKeToolbar(cetakToolbarbutton, add, comp);

			MyToolbarbuttonConfig cetakUpload = Common.uploadData(this, Pegawai.class, columns);
			Common.appendKeToolbar(cetakUpload, add, comp);
			cetakUpload.setVisible((add != null && add.isVisible()) && edit);

			if (satuanKerjaOnSession != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Pegawai", "/img/add_item.png");
				button.setDisabled(!edit);
				button.addEventListener("onClick", new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						Session session = HibernateUtil.currentSession();

						List<Pegawai> pegawais = session.createCriteria(Pegawai.class)
								.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
								.add(Restrictions.eq("satuanKerja", satuanKerjaOnSession)).list();

						AmbilDataPegawaiBanyak ambilDataPegawaiBanyak = new AmbilDataPegawaiBanyak(pegawais);
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
								.appendChild(ambilDataPegawaiBanyak);
						ambilDataPegawaiBanyak.setEventListener(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								List<Pegawai> pegawais = (List<Pegawai>) arg0.getData();

								for (Pegawai pegawai : pegawais) {
									pegawai.setSatuanKerja(satuanKerjaOnSession);
									Common.refreshSaveOrUpdate(pegawai);
								}

								onSearchDefault(arg0);
							}
						});
						ambilDataPegawaiBanyak.setWidth("850px");
						ambilDataPegawaiBanyak.setHeight("97%");
						ambilDataPegawaiBanyak.setVisible(true);
						ambilDataPegawaiBanyak.onModal();
					}

				});
				button.setParent(add.getParent());
			}

			MyToolbarbuttonConfig generatePasswordDosen = new MyToolbarbuttonConfig(
					pt || ya ? "Password Pegawai bukan Dosen/Guru" : "Password Pegawai", "/img/print.png");
			generatePasswordDosen.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show(
							"Anda akan membuatkan dan mengambil username dan password Pegawai bukan Dosen/Guru.",
							"Informasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										String strURL = Common
												.getKonfigurasi("ambil_kode_url", "https://dev.ecampus.id/ecampus/Api")
												.getNilai();

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
												.getPerguruanTinggiMedia(request, "background_login_perguruanTinggi_");

										final String filename = Sessions.getCurrent().getWebApp()
												.getRealPath("/tmp/user_password_pegawai_"
														+ URLEncoder.encode(Common.datetimeFormat2s.get()
																.format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
														+ ".xlsx");

										List<Pegawai> pegawais = initCriteria(true).add(Restrictions.isNotNull("nama"))
												.add(Restrictions.isNull("dosen")).add(Restrictions.isNull("guru"))
												.setMaxResults(1048576).list();

										// Kumpulkan id di UTAS EVENT (hindari akses entitas lintas-utas), lalu jalankan
										// pembuatan akun + penyusunan Excel + unduhan di UTAS LATAR dengan BAR PROGRES persen
										// (tidak membekukan UI seperti sebelumnya). Nilai yang bergantung pada HTTP request /
										// Perguruan Tinggi sudah dihitung di atas dan dioper sebagai String.
										java.util.List<Long> pegawaiIds = new java.util.ArrayList<Long>();
										for (Pegawai pegawai : pegawais) {
											if (pegawai != null && pegawai.getId() != null && pegawai.getNama() != null
													&& !pegawai.getNama().trim().isEmpty()) {
												pegawaiIds.add(pegawai.getId());
											}
										}

										String requestHost = Common.getRequestHostWithProtocol();
										ais.common.helper.DownloadPasswordPegawaiHelper.proses(pegawaiIds, filename, strURL,
												link, nama_pt, requestHost, background_login_PerguruanTinggi,
												background_PerguruanTinggi, logo_PerguruanTinggi, banner_PerguruanTinggi,
												perguruanTinggi.getMotto(), perguruanTinggi.getAlamat1(),
												perguruanTinggi.getTelepon(), perguruanTinggi.getEmail());
									}

								}
							});

				}
			});
			Common.appendKeToolbar(generatePasswordDosen, add, comp);

			// Hak akses dapat dikonfigurasi (daftar roleId dipisah koma via konfigurasi
			// "hak_akses_download_password_pegawai"; default HANYA Administrator/"am").
			// Lihat KonfigurasiNewAction span "Hak Akses Download Password Pegawai".
			generatePasswordDosen.setVisible((add != null && add.isVisible()) && edit
					&& Common.bolehUploadDataKonfigurasi("hak_akses_download_password_pegawai", Tbmrole.ADMINISTRATOR));

			// Tombol "Download Foto" lama DIHAPUS — digantikan "Download Foto Massal" (bulk ZIP,
			// multi-thread + bar progres) agar seragam. Handler onDownloadFoto lama dibiarkan (tak dipakai).
		}

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				LaporanPegawaiData laporanPegawaiData = new LaporanPegawaiData(initCriteria(true));
				laporanPegawaiData.setParent(page.getFirstRoot());
				laporanPegawaiData.setTitle("Data Pegawai");
				laporanPegawaiData.setWidth("95%");
				laporanPegawaiData.setHeight("95%");
				laporanPegawaiData.onModal();
			}
		});
		if (print != null) { print.setParent(add.getParent()); }
	        FilterLanjutHelper.setup(comp);
}

	@SuppressWarnings("unchecked")
	public void onDownloadFoto(Event event) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Pegawai> calonPegawai = initCriteria(true).list();
				File fileFolderLampiran = new File(
						"/opt/ecampus/foto_" + ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis());
				fileFolderLampiran.mkdirs();
				System.out.println("fileFolderLampiran => " + fileFolderLampiran.getAbsolutePath());

				for (Pegawai pegawai : calonPegawai) {

					try {
						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

						if (pegawai.getDosen() != null) {

							FotoDosen fotodosen = (FotoDosen) streamingSession.createCriteria(FotoDosen.class)
									.add(Restrictions.eq("dosen", pegawai.getDosen().getId())).setMaxResults(1)
									.uniqueResult();

							if (fotodosen != null) {
								File fileFoto = fotodosen.ambilFile();
								File fileCopy = new File(
										fileFolderLampiran.getAbsolutePath() + "/" + pegawai.getDosen().getMycode()
												+ "_" + pegawai.getDosen().getNama() + "_" + fileFoto.getName());
								System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
								FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
								FileInputStream fileInputStream = new FileInputStream(fileFoto);
								IOUtils.copyLarge(fileInputStream, fileOutputStream);
								fileInputStream.close();
								fileOutputStream.close();
							}

						}

						else if (pegawai.getGuru() != null) {

							FotoGuru fotodosen = (FotoGuru) streamingSession.createCriteria(FotoGuru.class)
									.add(Restrictions.eq("dosen", pegawai.getGuru().getId())).setMaxResults(1)
									.uniqueResult();

							if (fotodosen != null) {
								File fileFoto = fotodosen.ambilFile();
								File fileCopy = new File(
										fileFolderLampiran.getAbsolutePath() + "/" + pegawai.getGuru().getKode() + "_"
												+ pegawai.getGuru().getNama() + "_" + fileFoto.getName());
								System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
								FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
								FileInputStream fileInputStream = new FileInputStream(fileFoto);
								IOUtils.copyLarge(fileInputStream, fileOutputStream);
								fileInputStream.close();
								fileOutputStream.close();
							}

						}

						else {

							FotoPegawai fotopegawai = (FotoPegawai) streamingSession.createCriteria(FotoPegawai.class)
									.add(Restrictions.eq("pegawai", pegawai.getId())).setMaxResults(1).uniqueResult();

							if (fotopegawai != null) {
								File fileFoto = fotopegawai.ambilFile();
								File fileCopy = new File(fileFolderLampiran.getAbsolutePath() + "/"
										+ pegawai.getMycode() + "_" + pegawai.getNama() + "_" + fileFoto.getName());
								System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
								FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
								FileInputStream fileInputStream = new FileInputStream(fileFoto);
								IOUtils.copyLarge(fileInputStream, fileOutputStream);
								fileInputStream.close();
								fileOutputStream.close();
							}
						}

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e1) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/PegawaiAction.java:839");
					}

				}

				File fileFolderLampiranZip = new File(fileFolderLampiran.getAbsolutePath() + ".zip");
				Common.zipDir(fileFolderLampiranZip.getAbsolutePath(), fileFolderLampiran.getAbsolutePath());
				Filedownload.save(fileFolderLampiranZip, "application/zip");

			}
		}, "Harap tunggu.. sedang melakukan proses download foto..");

	}

//	@SuppressWarnings("unchecked")
//	Map<Serializable, Tbmuser> map = ConstantValues.ambilBerdasarClass(Tbmuser.class);

	public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public static Vbox masakerja(Pegawai pegawai) {
		Vbox vbox = new Vbox();

		if (pegawai.getTanggalMulaiPengalanKerja() != null) {

			String ActualDate = Common.databaseDateFormat.get().format(pegawai.getTanggalMulaiPengalanKerja());
			java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
			java.time.LocalDate currentdate = pegawai.getTanggalSampaiPengalanKerja() == null
					? java.time.LocalDate.now()
					: java.time.LocalDate
							.parse(Common.databaseDateFormat.get().format(pegawai.getTanggalSampaiPengalanKerja()));
			Period period = Period.between(dt, currentdate);

			new Label("Pengalaman Kerja : " + period.getYears() + " thn, " + period.getMonths() + " bln, "
					+ period.getDays() + " hr").setParent(vbox);
		}
		if (pegawai.getTanggalmasukHonorer() != null) {

			String ActualDate = Common.databaseDateFormat.get().format(pegawai.getTanggalmasukHonorer());
			java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
			java.time.LocalDate currentdate = pegawai.getTanggalkeluarHonorer() == null ? java.time.LocalDate.now()
					: java.time.LocalDate.parse(Common.databaseDateFormat.get().format(pegawai.getTanggalkeluarHonorer()));
			Period period = Period.between(dt, currentdate);

			new Label("Honor : " + period.getYears() + " thn, " + period.getMonths() + " bln, " + period.getDays()
					+ " hr").setParent(vbox);
		}
		if (pegawai.getTanggalmasukSemiTetap() != null) {

			String ActualDate = Common.databaseDateFormat.get().format(pegawai.getTanggalmasukSemiTetap());
			java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
			java.time.LocalDate currentdate = pegawai.getTanggalkeluarSemiTetap() == null ? java.time.LocalDate.now()
					: java.time.LocalDate.parse(Common.databaseDateFormat.get().format(pegawai.getTanggalkeluarSemiTetap()));
			Period period = Period.between(dt, currentdate);

			new Label("Semi Tetap : " + period.getYears() + " thn, " + period.getMonths() + " bln, " + period.getDays()
					+ " hr").setParent(vbox);
		}
		if (pegawai.getTanggalmasuk() != null) {

			String ActualDate = Common.databaseDateFormat.get().format(pegawai.getTanggalmasuk());
			java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
			java.time.LocalDate currentdate = pegawai.getTanggalkeluar() == null ? java.time.LocalDate.now()
					: java.time.LocalDate.parse(Common.databaseDateFormat.get().format(pegawai.getTanggalkeluar()));
			Period period = Period.between(dt, currentdate);

			new Label("Tetap : " + period.getYears() + " thn, " + period.getMonths() + " bln, " + period.getDays()
					+ " hr").setParent(vbox);
		}

		Period period = MasaKerjaUtil.masaKerja(pegawai);
		new Label("Masa kerja " + period.getYears() + " tahun " + period.getMonths() + " bulan " + period.getDays()
				+ " hari").setParent(vbox);

		return vbox;
	}

	class PegawaiRenderer extends ais.ui.util.MyRowRenderer {

		Date sekarang = WaktuUtil.getDate();
//		@SuppressWarnings("rawtypes")
//		private Collection user = ConstantValues.ambilBerdasarClass(Tbmuser.class).values();
//		@SuppressWarnings("rawtypes")
//		private Collection pangkats = ConstantValues.ambilBerdasarClass(KenaikanPangkat.class).values();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pegawai pegawai = (Pegawai) arg1;

			CommonMedia.tampilkanGambarKecil(pegawai).setParent(arg0);
			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelKecil(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(vbox);
			new MyLabelKecil(pegawai.getCode() == null ? "" : pegawai.getCode()).setParent(vbox);

			vbox = RevisiHelper.createNewRevisi(Pegawai.class, pegawai,
					pegawai.getDosen() == null ? pegawai.getNama() : pegawai.getDosen().getNama());
			vbox.setParent(arg0);

			if (pegawai.getTipePegawai() != null) {
				new MyLabelAgakKecil(pegawai.getTipePegawai().getNama()).setParent(vbox);
			} else if (pegawai.getGuru() != null) {
				new MyLabelAgakKecil("Guru").setParent(vbox);
			} else if (pegawai.getDosen() != null) {
				new MyLabelAgakKecil("Dosen").setParent(vbox);
			}
//			else if (pegawai != null) {
//
//				for (Object o : user) {
//					Tbmuser tbmuser = (Tbmuser) o;
//					if (tbmuser.getAktif() && tbmuser.hakAkses() != null && tbmuser.getPegawai() != null
//							&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
//						if (tbmuser.getAktif()) {
//							new MyLabelAgakKecil(tbmuser.hakAkses().getRoleName()).setParent(vbox);
//						}
//					}
//				}
//
//			}

			if (pegawai.getStatusKepegawaian() != null) {
				new MyLabelAgakKecil(pegawai.getStatusKepegawaian().getNama()).setParent(vbox);
			}

//			String socialMediaProfile = "";
//
//			for (Tbmuser tbmuser : map.values()) {
//				if (tbmuser != null && tbmuser.getAktif() && tbmuser.getPegawai() != null
//						&& tbmuser.getPegawai().getId() != null
//						&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
//					socialMediaProfile = socialMediaProfile.isEmpty() ? tbmuser.getSocialMediaProfile()
//							: "||" + tbmuser.getSocialMediaProfile();
//				}
//			}

//			pegawai.ambilGajiPokok(sekarang)

//			TbmuserAction.tampilkanSocialMediaProfile(vbox, socialMediaProfile);

			PegawaiAction.masakerja(pegawai).setParent(arg0);

//			List<KenaikanPangkat> kenaikanPangkats = pegawai.ambilKenaikanPangkat(sekarang, pangkats);

			new Label(pegawai.getTanggalmasuk() == null ? "" : Common.dateFormat1.get().format(pegawai.getTanggalmasuk()))
					.setParent(arg0);

//			JabatanFungsional jabatanFungsional = pegawai.ambilJabatanFungsional(kenaikanPangkats);
//			JabatanStruktural jabatanStruktural = pegawai.ambilJabatanStruktural(kenaikanPangkats);
//			Jabatan jabatan = pegawai.ambilJabatan(kenaikanPangkats);
//
//			new ais.ui.util.MyHtml("<font>"
//					+ (jabatanFungsional == null ? "" : jabatanFungsional.getNama() + "<br>")
//					+ (jabatanStruktural == null ? "" : jabatanStruktural.getNama() + "<br>")
//					+ (pegawai.getPendidikan() == null ? "" : pegawai.getPendidikan().getNama() + "<br>")
//					+ (jabatan == null ? "" : jabatan.getNama()) + "</font>").setParent(arg0);
//			kenaikanPangkats = null;

			new Label(pegawai.getTipePegawai() == null ? "" : pegawai.getTipePegawai().getNama()).setParent(arg0);

			new Label(pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama()).setParent(arg0);

			Pegawai atasan = pegawai.getAtasanlangsung();
			Pegawai atasan2 = pegawai.getAtasanlangsung2();
			Pegawai atasan3 = pegawai.getAtasanlangsung3();

			new Label((atasan == null ? "" : atasan.getNama()) + (atasan2 == null ? "" : ", " + atasan2.getNama())
					+ (atasan3 == null ? "" : ", " + atasan3.getNama())
					+ (pegawai.getAtasan() == null ? "" : " " + pegawai.getAtasan().getNama())
					+ (pegawai.getAtasanPendukung() == null ? "" : " " + pegawai.getAtasanPendukung().getNama())
					+ (pegawai.getAtasanPendukungCadangan() == null ? "" : " " + pegawai.getAtasanPendukungCadangan().getNama()))
					.setParent(arg0);

			new Label(pegawai.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(pegawai.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			if (pegawai.getStatusPegawai() != null && pegawai.getStatusPegawai().getNama() != null
					&& pegawai.getStatusPegawai().getNama().equalsIgnoreCase("Pensiun")) {
				checkbox.setDisabled(true);
			}

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pegawai.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(pegawai);
				}
			});

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Daftar Riwayat Hidup");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onEvent(Event event) throws Exception {

					Map parameters = ais.common.HashMapGenerator.getRand();
					Common.insertProperty(Pegawai.class, pegawai, parameters, "", 2);
					GajiPokok gajiPokok = pegawai.ambilGajiPokok(WaktuUtil.getDate());
					if (gajiPokok != null) {
						Common.insertProperty(GajiPokok.class, gajiPokok, parameters, "gp", 1);
					}
					parameters.put("id", pegawai.getId());
					pegawai.putPhoto(parameters);

					Report.generatePDFReport(Report.PDF, parameters, "employ/daftar_riwayat_hidup",
							ais.ui.util.WaktuUtil.getDate());
				}

			});
			aksiButtons.add(button);

			Tbmuser tbmuser = Common.getCurrentUser();

			button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit
					|| (tbmuser.ambilFakultas() != null && pegawai.getFakultas() != null
							&& tbmuser.ambilFakultas().getId().equals(pegawai.getFakultas().getId()))
					|| (tbmuser.ambilJurusan() != null && pegawai.getJurusan() != null
							&& tbmuser.ambilJurusan().getId().equals(pegawai.getJurusan().getId())));
			button.setTooltiptext("Ubah data");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

//					if (pegawai != null && pegawai.getDosen() != null) {
//
//						Dosen dosen = pegawai.getDosen();
//						HibernateUtil.currentSession().refresh(dosen);
//						biodataDosenAction = new BiodataDosenAction(dosen);
//						biodataDosenAction.setCommonOnSearchdefault(PegawaiAction.this);
//						biodataDosenAction.setHeight("99%");
//						biodataDosenAction.setWidth("90%");
//						page.getFirstRoot().appendChild(biodataDosenAction);
//						biodataDosenAction.setVisible(true);
//						biodataDosenAction.onModal();
//					}
//
//					else if (pegawai != null && pegawai.getGuru() != null) {
//						Guru guru = pegawai.getGuru();
//						HibernateUtil.currentSession().refresh(guru);
//						GuruAction.onAddExternal(null, new EventListener() {
//
//							@Override
//							public void onEvent(Event arg0) throws Exception {
//								// TODO Auto-generated method stub
//
//							}
//						}, guru, true);
//					}
//
//					else {

					biodataPegawaiAction = new BiodataPegawaiAction(pegawai);
					biodataPegawaiAction.setCommonOnSearchdefault(PegawaiAction.this);
					biodataPegawaiAction.setHeight("95%");
					biodataPegawaiAction.setWidth("90%");
					page.getFirstRoot().appendChild(biodataPegawaiAction);
					biodataPegawaiAction.setVisible(true);
					biodataPegawaiAction.onModal();
//					}
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setTooltiptext("Hapus data");
			button.setVisible(delete
					|| (tbmuser.ambilFakultas() != null && pegawai.getFakultas() != null
							&& tbmuser.ambilFakultas().getId().equals(pegawai.getFakultas().getId()))
					|| (tbmuser.ambilJurusan() != null && pegawai.getJurusan() != null
							&& tbmuser.ambilJurusan().getId().equals(pegawai.getJurusan().getId())));
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
											PegawaiDao pegawaiDao = DaoFactory.getInstance().getPegawaiDao();

											String sql1 = "delete from log_user_actifity where detail_log_login in (select id from detail_log_login where log_login in (select id from log_login where pegawai in (select id from pegawai where id = "
													+ pegawai.getId() + ")));";
											pegawaiDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from log_user_actifity where detail_log_login in (select id from detail_log_login where log_login in (select id from log_login where pegawai in (select id from pegawai where id = "
													+ pegawai.getId() + ")));";
											pegawaiDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from detail_log_login where log_login in (select id from log_login where pegawai in (select id from pegawai where id = "
													+ pegawai.getId() + "));";
											pegawaiDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from log_login where pegawai in (select id from pegawai where id = "
													+ pegawai.getId() + ");";
											pegawaiDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from log_user_actifity where detail_log_login in (select id from detail_log_login where log_login in (select id from log_login where tbmuser in (select userid from tbmuser where pegawai = "
													+ pegawai.getId() + " )));";

											pegawaiDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from log_user_actifity where detail_log_login in (select id from detail_log_login where log_login in (select id from log_login where tbmuser in (select userid from tbmuser where pegawai = "
													+ pegawai.getId() + " )));";
											pegawaiDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from detail_log_login where log_login in (select id from log_login where tbmuser in (select userid from tbmuser where pegawai = "
													+ pegawai.getId() + " ));";
											pegawaiDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from log_login where tbmuser in (select userid from tbmuser where pegawai = "
													+ pegawai.getId() + " );";
											pegawaiDao.getCurrentSession().createSQLQuery(sql1).executeUpdate();

											Session session = HibernateUtil.currentSession();
											String sql = "delete from employ.riwayat_kartu_identitas_pegawai where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.keluarga where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.riwayat_keluar_negeri_pegawai where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.riwayat_keterangan_lain_pegawai where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.riwayat_organisasi_kampus_pegawai where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.riwayat_organisasi_lain_pegawai where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.riwayat_organisasi_sekolah_pegawai where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.riwayat_pelatihan_pegawai where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.riwayat_pendidikan_pegawai where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.riwayat_tanda_jasa_pegawai where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql = "delete from employ.kenaikan_pangkat where status = false and pegawai = "
													+ pegawai.getId();
											session.createSQLQuery(sql).executeUpdate();

											sql1 = "delete from tbmuser where pegawai = " + pegawai.getId() + ";";
											session.createSQLQuery(sql1).executeUpdate();

											sql1 = "delete from biodata_pegawai where pegawai = " + pegawai.getId()
													+ ";";
											session.createSQLQuery(sql1).executeUpdate();

											Common.refreshDelete(pegawai);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena terdapat relasi data yang sudah disetujui atau karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");
			button.setVisible(approve);
			button.setTooltiptext("Setujui semua pengajuan");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin mensetujui semua pengajuan pegawai ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Session session = HibernateUtil.currentSession();
										String sql = "update employ.riwayat_kartu_identitas_pegawai set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.keluarga set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_keluar_negeri_pegawai set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_keterangan_lain_pegawai set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_organisasi_kampus_pegawai set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_organisasi_lain_pegawai set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_organisasi_sekolah_pegawai set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_pelatihan_pegawai set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_pendidikan_pegawai set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_tanda_jasa_pegawai set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.kenaikan_pangkat set status = true where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										onSearchDefault(event);

									}

								}
							});
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			button.setVisible(reject);
			button.setTooltiptext("Batalkan semua pengajuan");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan semua pengajuan pegawai ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Session session = HibernateUtil.currentSession();
										String sql = "update employ.riwayat_kartu_identitas_pegawai set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.keluarga set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_keluar_negeri_pegawai set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_keterangan_lain_pegawai set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_organisasi_kampus_pegawai set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_organisasi_lain_pegawai set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_organisasi_sekolah_pegawai set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_pelatihan_pegawai set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_pendidikan_pegawai set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.riwayat_tanda_jasa_pegawai set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										sql = "update employ.kenaikan_pangkat set status = false where pegawai = "
												+ pegawai.getId();
										session.createSQLQuery(sql).executeUpdate();

										onSearchDefault(event);

									}

								}
							});
				}

			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

		}
	}

	@SuppressWarnings("rawtypes")
	public String getKeterangan(Pegawai pegawai, Class clazz, String ket) {
		Session session = HibernateUtil.currentSession();

		Integer countDisetujuai = ((Number) session.createCriteria(clazz).add(Restrictions.eq("pegawai", pegawai))
				// .add(Restrictions.eq("status", true))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		Integer countBelumDisetujuai = ((Number) session.createCriteria(clazz).add(Restrictions.eq("pegawai", pegawai))
				// .add(Restrictions.eq("status", false))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		return ket + ": <font>" + Common.numberFormat.get().format(countBelumDisetujuai)
				+ "</font>, <font>" + Common.numberFormat.get().format(countDisetujuai)
				+ "</font>";
	}

	/**
	 * Unggah FOTO MASSAL pegawai: pilih banyak berkas foto (nama berkas = kode/NIP pegawai) ATAU satu
	 * berkas ZIP; setiap foto langsung dipasang ke pegawai yang cocok, lalu ringkasan ditampilkan.
	 */
	/** Unduh SEMUA foto pegawai sebagai satu ZIP (baca BLOB paralel maks 50 thread). Tombol "Download Foto Massal". */
	public void onDownloadFotoMassal(Event event) throws Exception {
		ais.common.helper.DownloadFotoMassalHelper.downloadFotoPegawaiMassal();
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
		int[] hasil = ais.common.helper.UploadFotoMassalHelper.uploadFotoPegawaiByNim(daftar);
		ais.ui.util.MyMessageboxConfig.show(ais.common.helper.UploadFotoMassalHelper.ringkasan(hasil),
				"Upload Foto Massal", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
	}

	public void onAdd(Event event) throws Exception {
		biodataPegawaiAction = new BiodataPegawaiAction(new Pegawai(), satuanKerjaOnSession);
		biodataPegawaiAction.setCommonOnSearchdefault(PegawaiAction.this);
		biodataPegawaiAction.setHeight("95%");
		biodataPegawaiAction.setWidth("90%");
		page.getFirstRoot().appendChild(biodataPegawaiAction);
		biodataPegawaiAction.setVisible(true);
		biodataPegawaiAction.onModal();
	}

	public void onAddDosen(Event event) throws Exception {
		biodataDosenAction = new BiodataDosenAction(new Dosen());
		biodataDosenAction.setCommonOnSearchdefault(PegawaiAction.this);
		biodataDosenAction.setHeight("90%");
		biodataDosenAction.setWidth("850px");
		page.getFirstRoot().appendChild(biodataDosenAction);
		biodataDosenAction.setVisible(true);
		biodataDosenAction.onModal();
	}

	public void onAddGuru(Event event) throws Exception {
		Guru guru = new Guru();
		GuruAction.onAddExternal(null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub

			}
		}, guru, true);
	}

	public Criteria initCriteria(boolean order) {

		Tbmuser tbmuser = Common.getCurrentUser();
		Pegawai existing = null;
		if (tbmuser != null && tbmuser.getPegawai() != null && !tbmuser.hakAkses().getMelihatDataPegawaiLain()) {
			existing = tbmuser.getPegawai();
		}

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pegawai.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(existing == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("id", existing.getId()))

				.add(statusPegawai != null ? Restrictions.eq("statusPegawai", statusPegawai)
						: searchaktif == null || searchaktif.isChecked()
								? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
								: Restrictions.sqlRestriction("true"))

				.add(statusPegawai != null ? Restrictions.eq("statusPegawai", statusPegawai)
						: searchnonaktif.isChecked() ? Restrictions.eq("aktif", false)
								: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.asc("nama"));

		criteria

				.add(searchbukanDosen != null && searchbukanDosen.isChecked()
						? Restrictions.and(Restrictions.isNull("guru"), Restrictions.isNull("dosen"))
						: Restrictions.sqlRestriction("true"))
				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								parent == null ? Restrictions.isNull("satuanKerja")
										: Restrictions.sqlRestriction("false"),
								Restrictions.in("satuanKerja", satuanKerjas)))
				.add(satuanKerjaOnSession == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("satuanKerja", satuanKerjaOnSession))
				.add(searchstatus == null || searchstatus.getSelectedItem() == null
						|| searchstatus.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("statusPegawai", searchstatus.getSelectedItem().getValue()))
				// .add(Restrictions.isNull("dosen"))
				.add(searchnama == null || searchnama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchcode == null || searchcode.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("code", searchcode.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("mycode", searchcode.getValue().trim(), MatchMode.ANYWHERE)));

		// Filter "Absensi": Pernah Absen / Belum Pernah Absen / Semua (default).
		// "Pernah Absen" = pegawai punya minimal satu catatan kehadiran harian
		// (status_kehadiran_karyawan_harian). Memakai subquery id pegawai.
		String pernahAbsen = searchPernahAbsen == null || searchPernahAbsen.getSelectedItem() == null
				|| searchPernahAbsen.getSelectedItem().getValue() == null ? ""
						: searchPernahAbsen.getSelectedItem().getValue().toString();
		if ("pernah".equals(pernahAbsen) || "belum".equals(pernahAbsen)) {
			DetachedCriteria subAbsen = DetachedCriteria.forClass(StatuskehadiranKaryawanHarian.class)
					.add(Restrictions.isNotNull("pegawai"))
					.setProjection(Projections.distinct(Projections.property("pegawai")));
			criteria.add("pernah".equals(pernahAbsen) ? Subqueries.propertyIn("id", subAbsen)
					: Subqueries.propertyNotIn("id", subAbsen));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		searchnonaktif.setDisabled(searchaktif.isChecked());
		searchaktif.setDisabled(searchnonaktif.isChecked());

		Common.initPaging(initCriteria(false), paging);

		List<Pegawai> pegawai = ConstantValues
				.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE).setFirstResult(
								Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
						Pegawai.class);
		System.out.println("pegawais size " + pegawai.size());
		ListModel strset = new SimpleListModel(pegawai);
		grid.setRowRenderer(new PegawaiRenderer());
		grid.setModelCheckMobile(strset);

		if (biodataPegawaiAction != null) {
			biodataPegawaiAction.detach();
		}
		if (biodataDosenAction != null) {
			biodataDosenAction.detach();
		}
	}

}
