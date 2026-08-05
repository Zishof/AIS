package ais.action.master.sekolah;


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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
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
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.BiodataPegawaiAction;
import ais.action.master.KonfigurasiTampilanGuruAction;
import ais.action.master.helper.AmbilDataKecamatanBanbox;
import ais.action.master.helper.AmbilDataNegaraBanbox;
import ais.action.master.helper.MainHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.format1.sekolah.LaporanSKGuru;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Agama;
import ais.database.model.GeneralValueObject;
import ais.database.model.GolonganPns;
import ais.database.model.JenisPendidikDanTenagaKependidikan;
import ais.database.model.Konfigurasi;
import ais.database.model.LembagaPengangkat;
import ais.database.model.LogLogin;
import ais.database.model.Negara;
import ais.database.model.Pegawai;
import ais.database.model.PekerjaanOrangTua;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusKepegawaian;
import ais.database.model.StatusPegawai;
import ais.database.model.SumberGaji;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.Wilayah;
import ais.database.model.employ.Pendidikan;
import ais.database.model.file.FotoGuru;
import ais.database.model.file.FotoPegawai;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JenisGuru;
import ais.database.model.sekolah.PenugasanGuruMengajar;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIframe;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;

import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class GuruAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchkode;
	private Textbox searchnama;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Checkbox searchaktif;

	private Textbox nama;
	private Combobox sekolah;
	private Combobox agama;
	private Textbox alamatEmail;
	private Textbox alamatGuru;

	private Combobox jenisKelamin;
	private MyDatebox tanggalLahir;
	private Textbox tempatLahir;

	private Combobox kewarganegaraan;
	private Textbox panggilan;
	private Combobox statusPegawai;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;
	private boolean pegawaiDiinput = true;

	private Guru guru;
	private MyToolbarbuttonConfig add;
	private AmbilDataNegaraBanbox negara;

	private Combobox yayasan;
	private Textbox teleponGuru;
	private Combobox jenisGuru;
	private Textbox nip;
	private Textbox kode;
	private EventListener eventListener = null;
	protected FotoGuru fotoGuru;

	private Textbox lintang;
	private Textbox bujur;

	private BiodataPegawaiAction biodataPegawaiAction = null;

	public final static String[] DEFAULT_TIDAK_WAJIB = new String[] { "nuptk", "namaAr", "namaCh", "jenisKelamin",
			"tempatLahir", "tanggalLahir", "nip", "statusKepegawaian", "jenisPendidikDanTenagaKependidikan", "agama",
			"alamatGuru", "rt", "rw", "dusun", "kelurahan", "kecamatan", "kodePos", "teleponGuru", "hp", "alamatEmail",
			"gelarDepan", "gelarBelakang", "pendidikan", "jurusan", "sertifikasi", "tmtKerja", "tugasTambahan",
			"mengajar", "jamTugasTambahan", "jjm", "totalJjm", "siswa", "kompetensi", "skCpns", "tglSkCpns", "skAngkat",
			"tmtSkAngkat", "lembagaPengangkat", "golonganPegawai", "sumberGaji", "namaIbu", "statusNikah",
			"namaSuamiIstri", "nipSuamiIstri", "pekerjaanSuamiIstri", "tmtPns", "sudahLisensiKepalaSekolah",
			"pernahDiklatKepengawasan", "keahlianBraille", "keahlianBahasaIsyarat", "npwp", "namaWajibPajak",
			"kewarganegaraan", "bank", "nomorRekeningBank", "rekeningAtasNama", "nik", "kk", "karpeg", "karisKarsu",
			"lintang", "bujur", "nuks", "aktif", "kode", "panggilan", "jenisGuru", "bahasa", "milikUniversitas",
			"namaAyah", "pekerjaanAyah", "pekerjaanIbu", "negara", "oleh", "olehId", "pegawai", "pegawaiId", "sekolah",
			"statusPegawai", "tanggal_dirubah", "yayasan", "keterangan", "sekolah1", "sekolah2", "sekolah3",
			"idfinger" };

	public final static String[] DATA = new String[] { "id", "namaGuru", "namaAr", "namaCh", "nuptk", "jenisKelamin",
			"tempatLahir", "tanggalLahir", "nip", "statusKepegawaian", "jenisPendidikDanTenagaKependidikan", "agama",
			"alamatGuru", "rt", "rw", "dusun", "kelurahan", "kecamatan", "kodePos", "teleponGuru", "hp", "alamatEmail",
			"gelarDepan", "gelarBelakang", "pendidikan", "jurusan", "sertifikasi", "tmtKerja", "tugasTambahan",
			"mengajar", "jamTugasTambahan", "jjm", "totalJjm", "siswa", "kompetensi", "skCpns", "tglSkCpns", "skAngkat",
			"tmtSkAngkat", "lembagaPengangkat", "golonganPegawai", "sumberGaji", "namaIbu", "statusNikah",
			"namaSuamiIstri", "nipSuamiIstri", "pekerjaanSuamiIstri", "tmtPns", "sudahLisensiKepalaSekolah",
			"pernahDiklatKepengawasan", "keahlianBraille", "keahlianBahasaIsyarat", "npwp", "namaWajibPajak",
			"kewarganegaraan", "bank", "nomorRekeningBank", "rekeningAtasNama", "nik", "kk", "karpeg", "karisKarsu",
			"lintang", "bujur", "nuks", "aktif", "kode", "panggilan", "jenisGuru", "bahasa", "milikUniversitas",
			"namaAyah", "pekerjaanAyah", "pekerjaanIbu", "negara", "oleh", "olehId", "pegawai", "pegawaiId", "sekolah",
			"statusPegawai", "tanggal_dirubah", "yayasan", "keterangan", "sekolah1", "sekolah2", "sekolah3",
			"idfinger" };

	public final static String[] DATA_DESC = new String[] { "ID", "Nama", "Nama (Aksara Arab)",
			"Nama (Aksara Tionghoa)", "NUPTK", "Jenis Kelamin", "Tempat Lahir", "Tanggal Lahir", "NIP (PNS)",
			"Status Kepegawaian", "Jenis PTK", "Agama", "Alamat Jalan", "RT", "RW", "Nama Dusun", "Kelurahan",
			"Kecamatan", "Kode Pos", "Telepon", "HP", "Email", "Gelar Depan", "Gelar Belakang", "Pendidikan Terakhir",
			"Jurusan/Prodi", "Sertifikasi", "TMT Kerja", "Tugas Tambahan", "Mengajar", "Jam Tugas Tambahan",
			"Jumlah Jam Mengajar (JJM)", "Total Jumlah Jam Mengajar (JJM)", "Siswa", "Kompetensi", "SK CPNS",
			"Tanggal CPNS", "SK Pengangkatan", "TMT Pengangkatan", "Lembaga Pengangkatan", "Pangkat Golongan",
			"Sumber Gaji", "Nama Ibu Kandung", "Status Perkawinan", "Nama Suami/Istri", "NIP Suami/Istri",
			"Pekerjaan Suami/Istri", "TMT PNS", "Sudah Lisensi Kepala Sekolah", "Pernah Diklat Kepengawasan",
			"Keahlian Braille", "Keahlian Bahasa Isyarat", "NPWP", "Nama Wajib Pajak", "Kewarganegaraan", "Bank",
			"Nomor Rekening Bank", "Rekening Atas Nama", "NIK", "No KK", "Karpeg", "Karis/Karsu",
			"Koordinat Lintang Google Map", "Koordinat Bujur Google Map", "Nomor Unik Kepala Sekolah (NUKS)", "Status",
			"Kode Guru", "Panggilan", "Jenis Guru", "Bahasa", "Milik Yayasan", "Nama Ayah", "Pekerjaan Ayah",
			"Pekerjaan Ibu", "Negara", "Oleh", "Oleh Id", "Pegawai", "Pegawai Id", "Sekolah", "Status Pegawai",
			"Tangal Dirubah", "Yayasan", "Keterangan", "Juga mengajar di (I)", "Juga mengajar di (II)",
			"Juga mengajar di (III)", "Kode Finger / RFID" };

	public final static TreeMap<String, String> MAPPING_DATA = new TreeMap<String, String>();

	static {
		for (int i = 0; i < DATA.length; i++) {
			try {
				MAPPING_DATA.put(DATA[i], DATA_DESC[i]);
			} catch (Exception e) {
				System.out.println("error key " + DATA[i]);
			}
		}

		System.out.println("MAPPING_DATA => " + MAPPING_DATA);
	}

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

			for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
				Session session = HibernateUtil.currentNativeSession();
				try {

					String id = Common.getCellContent(Common.getCell(sheet, 0, i));
					String rfid = Common.getCellContent(Common.getCell(sheet, 1, i));

					if (id == null || id.trim().isEmpty()) {
						continue;
					}

					Guru guru = (Guru) ConstantValues.ambil(Guru.class.getName(), Long.parseLong(rfid));
					System.out.println("id = " + id + " rfid = " + rfid + ", guru = " + guru);
					if (guru != null) {
						session.refresh(guru);
						guru.setIdfinger(rfid);
						session.getTransaction().begin();
						session.update(guru);
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
				for (Object[] guru : objects) {
					try {
						XSSFRow row = sheet.createRow(rowIndex);
						row.createCell(0).setCellValue(guru[0] + "");
						row.createCell(1).setCellValue(guru[1] + "");
						row.createCell(2).setCellValue(guru[2] + "");
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

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private PerguruanTinggi perguruanTinggi;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		// Sembunyikan tombol "Upload Foto (NIM)" bila hak akses (baca+ubah+hapus) tak lengkap.
		ais.common.helper.UploadFotoMassalHelper.terapkanGateTombolUpload(self);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Tbmuser tbmuser = Common.getCurrentUser();

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("Username");
		columnHeadersAdding.add("Jumlah Login");
		columnHeadersAdding.add("Sejarah Login");

		EventListener dataAdding = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				Guru guru = (Guru) objects[0];

				XSSFRow row = (XSSFRow) objects[2];

				Session session = HibernateUtil.currentNativeSession();

				List<String> usernames = session.createCriteria(Tbmuser.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.isNotNull("userId")).add(Restrictions.ne("userId", ""))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("guru", guru)).setProjection(Projections.property("userId")).list();
				String u = "";
				for (String user : usernames) {
					u += u.trim().isEmpty() ? user : "," + user;
				}

				Object[] logLogins = (Object[]) session.createCriteria(LogLogin.class).createAlias("tbmuser", "tbmuser")
						.add(Restrictions.eq("tbmuser.guru", guru)).add(Restrictions.isNotNull("login"))
						.setProjection(
								Projections.projectionList().add(Projections.max("login")).add(Projections.rowCount()))
						.uniqueResult();

				Date maxLogin = (Date) (logLogins == null || logLogins.length == 0 || logLogins[0] == null ? null
						: logLogins[0]);
				Integer countLogin = ((Number) (logLogins == null || logLogins.length == 0 || logLogins[1] == null ? 0
						: logLogins[1])).intValue();

				row.createCell(DATA.length + 0).setCellValue(u);

				row.createCell(DATA.length + 1).setCellValue(countLogin);

				row.createCell(DATA.length + 2)
						.setCellValue(maxLogin == null ? "" : Common.dateFormat3.get().format(maxLogin));

				HibernateUtil.closeSession();
			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Guru.class, this, "Download Guru",
				"/img/print.png", columnHeadersAdding, dataAdding, false, null, "DATA TAMBAHAN", DATA);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Guru.class, DATA);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		// Tombol "Download Foto" lama DIHAPUS — digantikan "Download Foto Massal" (bulk ZIP,
		// multi-thread + bar progres) agar seragam. Handler onDownloadFoto lama dibiarkan (tak dipakai).

		MyToolbarbuttonConfig generatePasswordGuru = new MyToolbarbuttonConfig("Password Guru", "/img/print.png");
		generatePasswordGuru.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Anda akan membuatkan dan mengambil username dan password Guru.", "Informasi",
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
													.getRealPath("/tmp/user_password_guru_"
															+ URLEncoder.encode(Common.datetimeFormat2s.get()
																	.format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
															+ ".xlsx");

											boolean menggunakanKode = Common.bolehKonfigurasi("menggunakan_kode_generate_guru", Konfigurasi.TIDAK_AKTIF);

											List<Guru> gurus = initCriteria(true).add(Restrictions.isNotNull("nama"))
													.setMaxResults(1048576).list();

											XSSFWorkbook workbook = new XSSFWorkbook();
											XSSFSheet sheet = workbook.createSheet("Guru");
											sheet.setDefaultColumnWidth(20);
											int rowIndex = 0;

											XSSFRow rowhead = sheet.createRow((short) 0);
											rowhead.createCell(0).setCellValue("ID");
											rowhead.createCell(1).setCellValue("Username");
											rowhead.createCell(2).setCellValue("Password");
											rowhead.createCell(3).setCellValue("Nama Lengkap");
											rowhead.createCell(4).setCellValue("Email");
											rowhead.createCell(5).setCellValue("HP");
											rowhead.createCell(6).setCellValue("Kode Install Mobile");

											for (Guru guru : gurus) {
												if (guru.getNama() != null && !guru.getNama().trim().isEmpty()) {
													rowIndex++;
													Session session = HibernateUtil.currentNativeSession();

													Tbmuser tbmuser = null;

													try {

														if (menggunakanKode && !guru.getKode().isEmpty()) {

															tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"),
																			Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("userId", guru.getKode()))
																	.add(Restrictions.eq("guru", guru)).setMaxResults(1)
																	.uniqueResult();

															if (tbmuser == null || tbmuser.getUserId() == null) {
																tbmuser = new Tbmuser();

																String newUsername = guru.getKode();

																int ada = ((Number) session
																		.createCriteria(Tbmuser.class)
																		.add(Restrictions.or(
																				Restrictions.isNull("aktif"),
																				Restrictions.eq("aktif", true)))
																		.add(Restrictions.eq("userId", newUsername))
																		.setProjection(Projections.rowCount())
																		.uniqueResult()).intValue();
																if (ada > 0) {
																	newUsername = newUsername
																			+ RandomStringUtils.randomNumeric(3);
																}

																newUsername = newUsername.toLowerCase().trim();

																tbmuser.setUserId(newUsername);
																tbmuser.setEmail(guru.getAlamatEmail());
																tbmuser.setYayasan(guru.getYayasan());
																tbmuser.setIs_encripted(true);
																tbmuser.setSekolah(guru.getSekolah());
																tbmuser.setRoot(false);
																tbmuser.setUserNama(guru.getNama());
																String passw = RandomStringUtils.randomNumeric(5);
																tbmuser.setUserPassword(
																		Common.desEncrypter.get().encrypt(passw.trim()));
																tbmuser.setUserRole(ConstantValues.roleGuru);
																tbmuser.setUserShow(1);
																tbmuser.setGuru(guru);

																session.getTransaction().begin();
																Common.refreshSaveOrUpdate(session, tbmuser);
																session.getTransaction().commit();

																Common.saveOrUpdateUserAccess(tbmuser, null,
																		tbmuser.getUserId(), passw.trim(),
																		tbmuser.getEmail());

															}

														} else {

															tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"),
																			Restrictions.eq("aktif", true)))
																	.add(Restrictions.eq("guru", guru)).setMaxResults(1)
																	.uniqueResult();
															if (tbmuser == null || tbmuser.getUserId() == null) {
																tbmuser = new Tbmuser();

																String newUsername = StringUtils.split(guru.getNama(),
																		" ")[0] + ""
																		+ RandomStringUtils.randomNumeric(3);

																newUsername = newUsername.toLowerCase().trim();

																tbmuser.setUserId(newUsername);
																tbmuser.setEmail(guru.getAlamatEmail());
																tbmuser.setYayasan(guru.getYayasan());
																tbmuser.setIs_encripted(true);
																tbmuser.setSekolah(guru.getSekolah());
																tbmuser.setRoot(false);
																tbmuser.setUserNama(guru.getNama());
																String passw = RandomStringUtils.randomNumeric(5);
																tbmuser.setUserPassword(
																		Common.desEncrypter.get().encrypt(passw.trim()));
																tbmuser.setUserRole(ConstantValues.roleGuru);
																tbmuser.setUserShow(1);
																tbmuser.setGuru(guru);

																session.getTransaction().begin();
																Common.refreshSaveOrUpdate(session, tbmuser);
																session.getTransaction().commit();

																Common.saveOrUpdateUserAccess(tbmuser, null,
																		tbmuser.getUserId(), passw.trim(),
																		tbmuser.getEmail());

															}

														}
													} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

													if (tbmuser != null) {
														XSSFRow row = sheet.createRow(rowIndex);
														row.createCell(0).setCellValue(guru.getId());
														row.createCell(1).setCellValue(tbmuser.getUserId());

														try {
															row.createCell(2).setCellValue(Common.desEncrypter.get()
																	.decrypt(tbmuser.getUserPassword()));
														} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

														row.createCell(3).setCellValue(guru.getNama());
														row.createCell(4).setCellValue(guru.getAlamatEmail());
														row.createCell(5).setCellValue(guru.getTeleponGuru());

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

															row.createCell(6)
																	.setCellValue(jsonObject.isNull("code") ? ""
																			: jsonObject.get("code") + "");
														} catch (Exception e) {
															ais.common.Common.tampilErrorJikaAdmin(e);
														}

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
		Common.appendKeToolbar(generatePasswordGuru, add, comp);

		MyToolbarbuttonConfig singkron = new MyToolbarbuttonConfig("Singkronkan dg pegawai", "/img/absensi_pmb.png");
		Common.appendKeToolbar(singkron, add, comp);
		if (singkron != null) { singkron.setVisible((add != null && add.isVisible()) && edit); }

		boolean merupakanAdmin = (tbmuser != null && tbmuser != null && tbmuser.hakAkses() != null
				&& tbmuser.hakAkses() != null && tbmuser != null && tbmuser.hakAkses() != null
				&& tbmuser.hakAkses().getRoleId() != null && (tbmuser != null && tbmuser.hakAkses() != null
						&& Common.getCurrentUser().hakAkses().getRoleId().equalsIgnoreCase(Tbmrole.ADMINISTRATOR)));
		if (uploadRfid != null) {
			uploadRfid.setVisible(Common.bolehKonfigurasi("aktifkan_upload_rfid_guru", Konfigurasi.TIDAK_AKTIF) && (add != null && add.isVisible()) && edit && merupakanAdmin);
			downloadRfid.setVisible(Common.bolehKonfigurasi("aktifkan_download_rfid_guru", Konfigurasi.TIDAK_AKTIF) && (add != null && add.isVisible()) && edit && merupakanAdmin);
		}

		singkron.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Guru dengan Pegawai");
						List<Guru> gurus = initCriteria(true).list();
						Session session = HibernateUtil.currentSession();
						int baris = 1;
						for (Guru guru : gurus) {
							if (!guru.getNama().trim().isEmpty()) {
								String kunci = String.valueOf(guru);
								try {
								Pegawai pegawai = (Pegawai) (session.createCriteria(Pegawai.class)

										.add(Restrictions.eq("guru", guru)).setMaxResults(1).uniqueResult());
								if (pegawai == null) {
									pegawai = new Pegawai();
									pegawai.setGuru(guru);
									session.save(pegawai);

									Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
									FotoGuru fotoGuru = (FotoGuru) streamingSession.createCriteria(FotoGuru.class)
											.add(Restrictions.eq("guru", guru.getId())).setMaxResults(1).uniqueResult();
									if (fotoGuru != null) {
										FotoPegawai fotoPegawai = new FotoPegawai();
										fotoPegawai.setNama(fotoGuru.getNama());
										fotoPegawai.setKeterangan(fotoGuru.getKeterangan());
										fotoPegawai.setPegawai(pegawai.getId());
										fotoPegawai.setFoto(fotoGuru.getFoto());

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
											.add(Restrictions.eq("guru", guru)).setMaxResults(1).uniqueResult();
									if (tbmuser == null || tbmuser.getUserId() == null) {
										tbmuser = new Tbmuser();

										String newUsername = StringUtils.split(guru.getNama(), " ")[0] + ""
												+ RandomStringUtils.randomNumeric(3);

										newUsername = newUsername.toLowerCase().trim();

										tbmuser.setUserId(newUsername);
										tbmuser.setEmail(guru.getAlamatEmail());
										tbmuser.setSekolah(guru.getSekolah());
										tbmuser.setIs_encripted(true);
										tbmuser.setYayasan(guru.getYayasan());
										tbmuser.setRoot(false);
										tbmuser.setUserNama(guru.getNama());
										String passw = RandomStringUtils.randomNumeric(5);
										tbmuser.setUserPassword(Common.desEncrypter.get().encrypt(passw.trim()));
										tbmuser.setUserRole(ConstantValues.roleGuru);
										tbmuser.setUserShow(1);
										tbmuser.setGuru(guru);
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

								if (guru.getPegawaiId() == null || !guru.getPegawaiId().equals(pegawai.getId())) {
									guru.setPegawaiId(pegawai.getId());
									Common.refreshSaveOrUpdate(session, guru);
								}
								laporan.catatBerhasil(baris - 1, kunci, "Sinkronisasi berhasil");
								} catch (Exception ePerGuru) {
									Common.tampilErrorJikaAdmin(ePerGuru);
									laporan.catatGagalDetail(baris - 1, kunci, ePerGuru);
								}
								baris++;
							}
						}

						laporan.selesaikan(null);
					}
				});
			}
		});
	        FilterLanjutHelper.setup(comp);
}

	@SuppressWarnings("unchecked")
	public void onDownloadFoto(Event event) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Guru> calonGuru = initCriteria(true).list();
				File fileFolderLampiran = new File(
						"/opt/ecampus/foto_" + ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis());
				fileFolderLampiran.mkdirs();
				System.out.println("fileFolderLampiran => " + fileFolderLampiran.getAbsolutePath());

				for (Guru guru : calonGuru) {

					try {
						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

						FotoGuru fotoguru = (FotoGuru) streamingSession.createCriteria(FotoGuru.class)
								.add(Restrictions.eq("guru", guru.getId())).setMaxResults(1).uniqueResult();

						if (fotoguru != null) {
							File fileFoto = fotoguru.ambilFile();
							File fileCopy = new File(fileFolderLampiran.getAbsolutePath() + "/" + guru.getKode() + "_"
									+ guru.getNama() + "_" + fileFoto.getName());
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
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/GuruAction.java:904");
					}

				}

				File fileFolderLampiranZip = new File(fileFolderLampiran.getAbsolutePath() + ".zip");
				Common.zipDir(fileFolderLampiranZip.getAbsolutePath(), fileFolderLampiran.getAbsolutePath());
				Filedownload.save(fileFolderLampiranZip, "application/zip");

			}
		}, "Harap tunggu.. sedang melakukan proses download foto..");

	}

//
//	@SuppressWarnings("unchecked")
//	Map<Serializable, Tbmuser> map = ConstantValues.ambilBerdasarClass(Tbmuser.class);
	private Combobox bahasa;
	private Textbox nuptk;
	private Combobox statusKepegawaian;
	private Combobox jenisPendidikDanTenagaKependidikan;
	private Textbox rt;
	private Textbox rw;
	private Textbox dusun;
	private Textbox kelurahan;
	private AmbilDataKecamatanBanbox kecamatan;
	private Textbox kodePos;
	private Textbox hp;
	private Combobox sumberGaji;
	private Combobox lembagaPengangkat;
	private MyDatebox tmtSkAngkat;
	private Textbox skAngkat;
	private Combobox golonganPegawai;
	private MyDatebox tmtPns;
	private MyDatebox tglSkCpns;
	private Textbox skCpns;
	private Textbox namaAyah;
	private Combobox pekerjaanAyah;
	private Textbox namaIbu;
	private Combobox pekerjaanIbu;
	private Combobox statusNikah;
	private Textbox namaSuamiIstri;
	private Textbox nipSuamiIstri;
	private Combobox pekerjaanSuamiIstri;
	private Textbox gelarDepan;
	private Textbox gelarBelakang;
	private Combobox pendidikan;
	private Textbox jurusan;
	private MyDatebox tmtKerja;
	private Textbox tugasTambahan;
	private Textbox mengajar;
	private Textbox jamTugasTambahan;
	private Textbox nik;
	private Textbox kk;
	private Textbox karpeg;
	private Textbox karisKarsu;
	private Textbox nuks;
	private Textbox bank;
	private Textbox nomorRekeningBank;
	private Textbox rekeningAtasNama;
	private MyCheckboxConfig sudahLisensiKepalaSekolah;
	private MyCheckboxConfig pernahDiklatKepengawasan;
	private MyCheckboxConfig keahlianBraille;
	private MyCheckboxConfig keahlianBahasaIsyarat;
	private Textbox npwp;
	private Textbox namaWajibPajak;
	private Textbox jjm;
	private Textbox totalJjm;
	private Textbox siswa;
	private Textbox kompetensi;
	private Textbox sertifikasi;
	private Textbox namaAr;
	private Textbox namaCh;
	private Combobox sekolah1;
	private Combobox sekolah2;
	private Combobox sekolah3;
	private boolean tampilPegawai = true;

	class GuruRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Guru guru = (Guru) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (detail.isOpen()) {

						Common.clear(detail);

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								ais.ui.util.MyButtonTabbox btnTabDetail = ais.ui.util.MyButtonTabbox.buat(detail, "100%", new int[] { 0 });
								{
									org.zkoss.zul.Div panel = btnTabDetail.tambahTab(0, "Sebagai Guru Wali", "/img/svg/chalkboard-user.svg");
									MyInclude iframe = new MyInclude("/pages/master/sekolah/siswa_id.zul?guruPembina=" + guru.getId());
									iframe.setHeight("100%"); iframe.setWidth("100%"); iframe.setParent(panel);
								}
								{
									org.zkoss.zul.Div panel = btnTabDetail.tambahTab(1, "Sebagai Guru BK", "/img/svg/comment-2-text-line.svg");
									MyInclude iframe = new MyInclude("/pages/master/sekolah/siswa_id.zul?guruBk=" + guru.getId());
									iframe.setHeight("100%"); iframe.setWidth("100%"); iframe.setParent(panel);
								}
							}
						});

					}
				}
			});

			CommonMedia.tampilkanGambarKecil(guru).setParent(arg0);

			Vbox a = new Vbox();
			a.setParent(arg0);
			new Label(guru.getKode()).setParent(a);
			new Label(guru.getNuptk()).setParent(a);
			new Label(guru.getNik()).setParent(a);

			(a = RevisiHelper.createNewRevisi(Guru.class, guru, guru.getNamaGuru())).setParent(arg0);

			if (!guru.getNamaGuru().equalsIgnoreCase(guru.getNamaAr())) {
				new Label(guru.getNamaAr()).setParent(a);
			}
			if (!guru.getNamaGuru().equalsIgnoreCase(guru.getNamaCh())) {
				new Label(guru.getNamaCh()).setParent(a);
			}

			new Label(guru.getYayasan() == null ? "" : guru.getYayasan().getNama()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			new Label(guru.getSekolah() == null ? "" : guru.getSekolah().getNama()).setParent(vbox);

			if (guru.getSekolah1() != null) {
				new Label(guru.getSekolah1().getNama()).setParent(vbox);
			}
			if (guru.getSekolah2() != null) {
				new Label(guru.getSekolah2().getNama()).setParent(vbox);
			}
			if (guru.getSekolah3() != null) {
				new Label(guru.getSekolah3().getNama()).setParent(vbox);
			}

			new Label(guru.getStatusPegawai() == null ? "" : guru.getStatusPegawai().getNama()).setParent(arg0);
			new Label(guru.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(guru.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					guru.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(guru);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, guru, GuruAction.this).setParent(arg0);

		}

	}

	public static void onAddExternal(Event event, EventListener eventListener, Guru guru, boolean tampilPegawai)
			throws Exception {
		GuruAction guruAction = new GuruAction();
		guruAction.eventListener = eventListener;
		guruAction.tampilPegawai = tampilPegawai;
		guruAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(guruAction.addWindow);
		guruAction.addWindow.setHeight("95%");
		guruAction.addWindow.setWidth("1050px");
		guruAction.pegawaiDiinput = false;
		guruAction.init(guru);

		guruAction.addWindow.setVisible(true);
		guruAction.addWindow.onModal();
	}

	/**
	 * Unggah FOTO MASSAL guru: pilih banyak berkas foto (nama berkas = NIP) ATAU satu berkas ZIP;
	 * setiap foto langsung dipasang ke guru yang cocok, lalu ringkasan ditampilkan.
	 */
	/** Unduh SEMUA foto guru sebagai satu ZIP (baca BLOB paralel maks 50 thread). Tombol "Download Foto Massal". */
	public void onDownloadFotoMassal(Event event) throws Exception {
		ais.common.helper.DownloadFotoMassalHelper.downloadFotoGuruMassal();
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
		int[] hasil = ais.common.helper.UploadFotoMassalHelper.uploadFotoGuruByNim(daftar);
		ais.ui.util.MyMessageboxConfig.show(ais.common.helper.UploadFotoMassalHelper.ringkasan(hasil),
				"Upload Foto Massal", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
	}

	public void onAdd(Event event) throws Exception {
		init(new Guru());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		guru = (Guru) obj;
		init(guru);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public static class ManagingSekolahYangDiajar {

		public Guru guru;

		public ManagingSekolahYangDiajar() {

		}

		public Tabpanel init(Guru g) throws Exception {
			guru = g;
			final Tabpanel panel = new ais.ui.util.MyTabpanel();
			panel.setWidth("100%");
			panel.setHeight("100%");

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(panel);
			borderlayout.setStyle("border:0px;");
			borderlayout.setHeight("100%");
			borderlayout.setWidth("100%");

			North north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, true);

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(north);

			toolbar.appendChild(new MyLabelConfig("Tahun Ajaran"));
			final Combobox ta = Common.generateTahunAjaranDanSemua(null);
			Common.selectComboItem(ta, Common.getCurrentTahunAkademik());
			toolbar.appendChild(ta);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("SK", "/img/svg/printer.svg");
			button.setTooltiptext("Cetak SK");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					LaporanSKGuru buktiPengeluaranKas = new LaporanSKGuru(guru);
					buktiPengeluaranKas.setTitle("Laporan");
					buktiPengeluaranKas.setClosable(true);
					buktiPengeluaranKas.setHeight("90%");
					buktiPengeluaranKas.setWidth("1200px");
					buktiPengeluaranKas.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					buktiPengeluaranKas.onModal();
				}
			});
			button.setParent(toolbar);

			final Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(center);
					reloadGuru(center, guru,
							(String) (ta.getSelectedItem() == null ? null : ta.getSelectedItem().getValue()), null);
				}
			};

			ta.addEventListener("onChange", eventListener);

			eventListener.onEvent(null);

			return panel;
		}

	}

	private ManagingSekolahYangDiajar managingSekolahYangDiajar = new ManagingSekolahYangDiajar();
	private Textbox idfinger;
	private MyLabelConfig labelKode;
	private MyLabelConfig labelNip;
	private MyLabelConfig labelNuptk;

	@SuppressWarnings({ "unchecked" })
	public static void reloadGuru(final Center center, final Guru guru, final String ta, final String smt) {

		Tbmuser tbmuser = Common.getCurrentUser();
		boolean upload_SK_oleh_admin = Common.bolehKonfigurasi("upload_SK_oleh_admin", Konfigurasi.TIDAK_AKTIF);

		Common.clear(center);
		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("45px");
		column.setLabel("");
		columns.appendChild(column);
		column = new MyColumnConfig();
		column.setLabel("Tahun Ajaran");
		columns.appendChild(column);
		column = new MyColumnConfig();
		column.setWidth("30%");
		column.setLabel("Jenis Semester");
		columns.appendChild(column);

		column = new MyColumnConfig();
		column.setVisible(!upload_SK_oleh_admin || (tbmuser != null && tbmuser.ambilGuru() == null));
		column.setWidth("30%");
		column.setLabel("Upload SK Gabungan");
		columns.appendChild(column);

		if (guru == null || guru.getId() == null) {
			return;
		}

		Criterion criterion = Restrictions.eq("guru", guru);
		criterion = Restrictions.or(criterion, Restrictions.eq("guru2", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru3", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru4", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru5", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru6", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru7", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru8", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru9", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru10", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru11", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru12", guru));

		Session session = HibernateUtil.currentSession();

		List<JadwalPelajaran> jadwalPelajarans = ConstantValues.simpleList(

				session.createCriteria(JadwalPelajaran.class)

						.add(criterion)

						.add(Restrictions.eq("tahunAjaran", ta)),
				JadwalPelajaran.class);

		TreeSet<String> tahunAkademiks = new TreeSet<String>();
		for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans) {
			String d = jadwalPelajaran.getTahunAjaran() + ";" + jadwalPelajaran.getSemester() + ";";
			tahunAkademiks.add(d);
		}

		Rows rows = new Rows();
		rows.setParent(grid);

		for (String s : tahunAkademiks) {
			try {
				String[] objs = s.split(";");
				final String tahun = objs[0];
				final String jenisSemesterNumber = objs[1];
				final String jenisSemester = jenisSemesterNumber.equals("1") ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
				if ((ta == null || (tahun != null && ta.equalsIgnoreCase(tahun)))
						&& (smt == null || (jenisSemester != null && smt.equalsIgnoreCase(jenisSemester)))) {

					final MyDetail detail = new MyDetail();
					detail.addEventListener("onOpen", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Tbmuser tbmuser = Common.getCurrentUser();
							boolean upload_SK_oleh_admin = Common.bolehKonfigurasi("upload_SK_oleh_admin", Konfigurasi.TIDAK_AKTIF);

							Common.clear(detail);
							if (detail.isOpen()) {
								ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
								groupbox.setStyle("min-height: 1200px;");
								groupbox.setParent(detail);
								groupbox.appendChild(new MyCaptionStyled("Penugasan Guru Mengajar"));
								MyGrid grid = new MyGrid();
								grid.setWidth("100%");
								grid.setParent(groupbox);
								grid.setWidth("100%");
								grid.setHeight("100%");
								grid.setStyle("min-height: 1200px;");
								grid.setSclass("dgrid");

								org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
								columns.setParent(grid);
								MyColumnConfig column = new MyColumnConfig();
								column.setWidth("15%");
								column.setLabel("Yayasan");
								columns.appendChild(column);
								column = new MyColumnConfig();
								column.setWidth("15%");
								column.setLabel("Sekolah");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setWidth("15%");
								column.setLabel("Jml Matpel (JML_MATPEL)");
								columns.appendChild(column);
								column = new MyColumnConfig();

								column = new MyColumnConfig();
								column.setWidth("10%");
								column.setLabel("Jml Jdw (JML_JDW)");
								columns.appendChild(column);
								column = new MyColumnConfig();

								column = new MyColumnConfig();
								column.setWidth("10%");
								column.setLabel("Jml JP (JML_JP)");
								columns.appendChild(column);
								column = new MyColumnConfig();

								column = new MyColumnConfig();
								column.setWidth("15%");
								column.setLabel("No Surat Tugas");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setWidth("15%");
								column.setLabel("Tgl. Surat Tugas");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setWidth("15%");
								column.setLabel("TMT Surat Tugas");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setVisible(
										!upload_SK_oleh_admin || (tbmuser != null && tbmuser.ambilGuru() == null));
								column.setWidth("10%");
								column.setLabel("Upload SK");
								columns.appendChild(column);

								String sql = "select \n" + "max(c.nama) as yayasan, \n" + "max(b.nama) as sekolah, \n"
										+ "a.program, max(b.id) as sekolahId \n" + "from (\n"
										+ "\tselect max(bb.sekolah_id) sekolah, max(bb.matapelajaran_id) matapelajaran, max(bb.program) as program \n"
										+ "\tfrom sekolah.jadwal_pelajaran bb  \n" + "\twhere (bb.guru_id = "
										+ (guru == null || guru.getId() == null ? -1L : guru.getId()) + " or bb.guru2_id = "
										+ (guru == null || guru.getId() == null ? -1L : guru.getId()) + " or bb.guru3_id = "
										+ (guru == null || guru.getId() == null ? -1L : guru.getId()) + " or bb.guru4_id = "
										+ (guru == null || guru.getId() == null ? -1L : guru.getId()) + " or bb.guru5_id = "
										+ (guru == null || guru.getId() == null ? -1L : guru.getId()) + " or bb.guru6_id = "
										+ (guru == null || guru.getId() == null ? -1L : guru.getId()) + " or bb.guru7_id = "
										+ (guru == null || guru.getId() == null ? -1L : guru.getId()) + " or bb.guru8_id = "
										+ (guru == null || guru.getId() == null ? -1L : guru.getId()) + " or bb.guru9_id = "
										+ (guru == null || guru.getId() == null ? -1L : guru.getId()) + " or bb.guru10_id = "
										+ (guru == null || guru.getId() == null ? -1L : guru.getId()) + " or bb.guru11_id = "
										+ (guru == null || guru.getId() == null ? -1L : guru.getId()) + " or bb.guru12_id = "
										+ (guru == null || guru.getId() == null ? -1L : guru.getId()) + ") \n" + "\tand bb.tahun_ajaran = '"
										+ tahun + "' \n" + "\tand bb.semester = " + jenisSemesterNumber + "\n"
										+ "\tgroup by bb.id\n" + ") a\n"
										+ "left join sekolah.sekolah b on (a.sekolah = b.id) \n"
										+ "left join sekolah.yayasan c on (b.yayasan_id = c.id) \n"
										+ "left join sekolah.matapelajaran d on (a.matapelajaran = d.id)   \n"
										+ "group by b.yayasan_id, a.sekolah, a.program";

								System.out.println("sql => " + sql);

								Session session = HibernateUtil.currentSession();
								List<Object[]> hasils = session.createSQLQuery(sql).list();

								Rows rows = new Rows();
								rows.setParent(grid);
								for (Object[] objects : hasils) {

									Long idSekolah = ((Number) objects[3]).longValue();

									String program = (String) objects[2];
									final PenugasanGuruMengajar penugasanGuruMengajar = Common
											.getPenugasanGuruMengajar(idSekolah, program, tahun, jenisSemester, guru);

									if (penugasanGuruMengajar.getGuru() == null) {
										penugasanGuruMengajar.setGuru(guru);
										Common.refreshUpdate(session, penugasanGuruMengajar);
									}

									List<JadwalPelajaran> jadwalPelajarans = ConstantValues.simpleList(session
											.createCriteria(JadwalPelajaran.class)
											.add(Restrictions.eq("tahunAjaran", ta))
											.add(Restrictions.eq("semester", Integer.parseInt(jenisSemesterNumber)))
											.add(Restrictions.eq("sekolah.id", idSekolah))

											.add(Restrictions.or(Restrictions.eq("guru12", guru), Restrictions.or(
													Restrictions.eq("guru11", guru),
													Restrictions.or(Restrictions.eq("guru10", guru), Restrictions.or(
															Restrictions.eq("guru9", guru),
															Restrictions.or(Restrictions.eq("guru8", guru), Restrictions
																	.or(Restrictions.eq("guru7", guru), Restrictions.or(
																			Restrictions.eq("guru6", guru),
																			Restrictions.or(
																					Restrictions.eq("guru5", guru),
																					Restrictions.or(
																							Restrictions.eq("guru4",
																									guru),
																							Restrictions.or(Restrictions
																									.eq("guru3", guru),
																									Restrictions.or(
																											Restrictions
																													.eq("guru",
																															guru),
																											Restrictions
																													.eq("guru2",
																															guru))))))))))))

											)

											, JadwalPelajaran.class);

									double jmlJp = 0.0;
									Set<Long> jmlMatpel = new HashSet<Long>();
									for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans) {
										jmlMatpel.add(jadwalPelajaran.getMatapelajaran().getId());

										if (jadwalPelajaran.getJamPelajaran() != null
												&& jadwalPelajaran.getGuru() != null
												&& jadwalPelajaran.getGuru().getId().equals(guru.getId())) {
											jmlJp += jadwalPelajaran.getJamPelajaran().getJp();
										}
										if (jadwalPelajaran.getJamPelajaran2() != null
												&& jadwalPelajaran.getGuru2() != null
												&& jadwalPelajaran.getGuru2().getId().equals(guru.getId())) {
											jmlJp += jadwalPelajaran.getJamPelajaran2().getJp();
										}
										if (jadwalPelajaran.getJamPelajaran3() != null
												&& jadwalPelajaran.getGuru3() != null
												&& jadwalPelajaran.getGuru3().getId().equals(guru.getId())) {
											jmlJp += jadwalPelajaran.getJamPelajaran3().getJp();
										}
										if (jadwalPelajaran.getJamPelajaran4() != null
												&& jadwalPelajaran.getGuru4() != null
												&& jadwalPelajaran.getGuru4().getId().equals(guru.getId())) {
											jmlJp += jadwalPelajaran.getJamPelajaran4().getJp();
										}
										if (jadwalPelajaran.getJamPelajaran5() != null
												&& jadwalPelajaran.getGuru5() != null
												&& jadwalPelajaran.getGuru5().getId().equals(guru.getId())) {
											jmlJp += jadwalPelajaran.getJamPelajaran5().getJp();
										}
										if (jadwalPelajaran.getJamPelajaran6() != null
												&& jadwalPelajaran.getGuru6() != null
												&& jadwalPelajaran.getGuru6().getId().equals(guru.getId())) {
											jmlJp += jadwalPelajaran.getJamPelajaran6().getJp();
										}
										if (jadwalPelajaran.getJamPelajaran7() != null
												&& jadwalPelajaran.getGuru7() != null
												&& jadwalPelajaran.getGuru7().getId().equals(guru.getId())) {
											jmlJp += jadwalPelajaran.getJamPelajaran7().getJp();
										}
										if (jadwalPelajaran.getJamPelajaran8() != null
												&& jadwalPelajaran.getGuru8() != null
												&& jadwalPelajaran.getGuru8().getId().equals(guru.getId())) {
											jmlJp += jadwalPelajaran.getJamPelajaran8().getJp();
										}
										if (jadwalPelajaran.getJamPelajaran9() != null
												&& jadwalPelajaran.getGuru9() != null
												&& jadwalPelajaran.getGuru9().getId().equals(guru.getId())) {
											jmlJp += jadwalPelajaran.getJamPelajaran9().getJp();
										}
										if (jadwalPelajaran.getJamPelajaran10() != null
												&& jadwalPelajaran.getGuru10() != null
												&& jadwalPelajaran.getGuru10().getId().equals(guru.getId())) {
											jmlJp += jadwalPelajaran.getJamPelajaran10().getJp();
										}
										if (jadwalPelajaran.getJamPelajaran11() != null
												&& jadwalPelajaran.getGuru11() != null
												&& jadwalPelajaran.getGuru11().getId().equals(guru.getId())) {
											jmlJp += jadwalPelajaran.getJamPelajaran11().getJp();
										}
										if (jadwalPelajaran.getJamPelajaran12() != null
												&& jadwalPelajaran.getGuru12() != null
												&& jadwalPelajaran.getGuru12().getId().equals(guru.getId())) {
											jmlJp += jadwalPelajaran.getJamPelajaran12().getJp();
										}
									}

									MyFormRow row = new MyFormRow();
									row.setValign("top");
									row.setParent(rows);

									Vbox a = RevisiHelper.createNewRevisi(PenugasanGuruMengajar.class,
											penugasanGuruMengajar, objects[0] == null ? "-" : objects[0].toString());
									a.setParent(row);

									Vbox vbox = new Vbox();
									vbox.setParent(a);

									row.appendChild(new Label(objects[1] == null ? "" : objects[1].toString()));

									row.appendChild(new Label(Common.numberFormat.get().format(jmlMatpel.size())));

									row.appendChild(new Label(Common.numberFormat.get().format(jadwalPelajarans.size())));

									row.appendChild(new Label(Common.numberFormat.get().format(jmlJp)));

									final Textbox kode = new Textbox(penugasanGuruMengajar.getKode());
									kode.setWidth("90%");
									row.appendChild(kode);

									final MyDatebox tanggalSuratTugas = new MyDatebox(
											penugasanGuruMengajar.getTanggalSuratTugas());
									tanggalSuratTugas.setWidth("90%");
									row.appendChild(tanggalSuratTugas);

									final MyDatebox tmtSuratTugas = new MyDatebox(
											penugasanGuruMengajar.getTmtSuratTugas());
									tmtSuratTugas.setWidth("90%");
									row.appendChild(tmtSuratTugas);

									final PenugasanGuruMengajar tempPenugasanGuruMengajar = penugasanGuruMengajar;

									Hbox hbox = new Hbox();
									hbox.setVisible(
											!upload_SK_oleh_admin || (tbmuser != null && tbmuser.ambilGuru() == null));
									LampiranLain.createDownloadUploadFileLain(hbox, penugasanGuruMengajar.getId(),
											"sk_penugasan_pengajaran_guru", "SK", false, new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {

												}
											}, null, false, false, false, true);
									hbox.setParent(row);

									EventListener eventListener = new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											tempPenugasanGuruMengajar.setKode(kode.getValue());
											tempPenugasanGuruMengajar.setTmtSuratTugas(tmtSuratTugas.getValue());
											tempPenugasanGuruMengajar
													.setTanggalSuratTugas(tanggalSuratTugas.getValue());
											tempPenugasanGuruMengajar.setGuru(guru);
											Common.refreshUpdate(tempPenugasanGuruMengajar);
										}
									};

									kode.addEventListener("onChange", eventListener);
									tanggalSuratTugas.addEventListener("onChange", eventListener);
									tmtSuratTugas.addEventListener("onChange", eventListener);

								}

							}

						}

					});
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					detail.setParent(row);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(tahun));
					row.appendChild(new ais.ui.util.MyLabelConfig(jenisSemester));

					String kode = Common.maxPanjangAkhir("000000000000000000000" + guru.getId() + "00"
							+ StringUtils.split(tahun, "/")[0] + jenisSemesterNumber, 14);
					kode = "1" + kode;
					Long l = Long.parseLong(kode);

					System.out.println("l -> " + l);

					Vbox myVbox = new Vbox();
					myVbox.setParent(row);
					Hbox hbox = new Hbox();
					hbox.setVisible(!upload_SK_oleh_admin || (tbmuser != null && tbmuser.ambilGuru() == null));
					LampiranLain.createDownloadUploadFileLain(hbox, l, "sk_penugasan_pengajaran_guru_gabungan",
							"SK Gabungan", false, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

								}
							}, null, false, false, false, true);
					hbox.setParent(myVbox);

				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

	}

	private void init(final Guru guru) throws Exception {
		this.guru = guru;
		addWindow.setTitle(guru.getId() == null ? "Tambah Guru" : "Ubah Guru");
		Common.clear(addWindow);

		Borderlayout borderlayoutUtama = new ais.ui.util.MyBorderlayout();
		borderlayoutUtama.setParent(addWindow);

		Center centerUtama = new Center();
		centerUtama.setParent(borderlayoutUtama);
		ais.ui.util.ZkCompat.setFlex(centerUtama, true);

		final ais.ui.util.MyButtonTabbox btnTab = ais.ui.util.MyButtonTabbox.buat(centerUtama, "100%", new int[] { 0 });
		final int[] idx = {0};

		final org.zkoss.zul.Div panelGuru = btnTab.tambahTab(idx[0]++, "Data Guru", "/img/svg/user-tie.svg");

		{
			final int myIdx = idx[0]++;
			org.zkoss.zul.Div panelMengajar = btnTab.tambahTab(myIdx, "Penugasan Mengajar", "/img/svg/chalkboard-teacher-light.svg");
			Tabpanel tabpanelJurusans = managingSekolahYangDiajar.init(guru);
			for (org.zkoss.zk.ui.Component kid : new java.util.ArrayList<org.zkoss.zk.ui.Component>(tabpanelJurusans.getChildren())) {
				kid.setParent(panelMengajar);
			}
			btnTab.onSetiapPilih(myIdx, new EventListener() {
				@Override public void onEvent(Event event) throws Exception {
					if (onSave(event)) {
						Guru dsn = GuruAction.this.guru;
						if (dsn == null || dsn.getId() == null) {
							btnTab.pilih(0);
						} else {
							managingSekolahYangDiajar.guru = dsn;
						}
					} else {
						btnTab.pilih(0);
					}
				}
			});
		}

		if (tampilPegawai) {
			final int myIdx = idx[0]++;
			final org.zkoss.zul.Div panelKepegawian = btnTab.tambahTab(myIdx, "Data Kepegawaian", "/img/svg/user-business.svg");
			biodataPegawaiAction = null;
			btnTab.onSetiapPilih(myIdx, new EventListener() {
				@Override public void onEvent(Event event) throws Exception {
					if (!onSave(event)) {
						btnTab.pilih(0);
					} else if (panelKepegawian.getChildren().isEmpty()) {
						Pegawai pegawai = Pegawai.createDataPegawaiDariGuru(GuruAction.this.guru);
						biodataPegawaiAction = new BiodataPegawaiAction(pegawai, false);
						biodataPegawaiAction.setHeight("100%");
						biodataPegawaiAction.setWidth("100%");
						biodataPegawaiAction.setBorder("none");
						biodataPegawaiAction.setTitle("");
						panelKepegawian.appendChild(biodataPegawaiAction);
					}
				}
			});
		}

		{
			final int myIdx = idx[0]++;
			final org.zkoss.zul.Div panelMobile = btnTab.tambahTab(myIdx, "Mobile", "/img/svg/phone.svg");
			boolean mobileVisible = Common.getApakahAdmin() && Common.bolehKonfigurasi("tampilkan_mobile_di_profile_guru");
			btnTab.setVisibleTombol(myIdx, mobileVisible);
			btnTab.onSetiapPilih(myIdx, new EventListener() {
				@Override public void onEvent(Event arg0) throws Exception {
					if (panelMobile.getChildren().isEmpty()) {
						if (!onSave(arg0)) {
							btnTab.pilih(0);
						} else {
							Tbmuser userId = (Tbmuser) HibernateUtil.currentSession().createCriteria(Tbmuser.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("guru", GuruAction.this.guru))
									.addOrder(Order.desc("tanggal_dirubah")).setMaxResults(1).uniqueResult();
							if (userId == null) {
								MyMessageboxConfig.show("Data / akun pengguna untuk guru ini tidak ditemukan", "Peringatan",
										MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								return;
							}
							MainHelper.onDapatkanKode(userId, panelMobile, false);
						}
					}
				}
			});
		}

		boolean tampilRiwayatMediaSosial = Common.bolehKonfigurasi("tampilRiwayatMediaSosial");
		if (tampilRiwayatMediaSosial && guru != null && guru.getId() != null) {
			final int myIdx = idx[0]++;
			final org.zkoss.zul.Div panelSosial = btnTab.tambahTab(myIdx, "Media Sosial", "/img/svg/comment-2-text-line.svg");
			final ais.ui.util.MyTabConfig dummyTab = new ais.ui.util.MyTabConfig();
			Common.displaySocialMedia(dummyTab, panelSosial, null, null, guru, null);
			btnTab.onSetiapPilih(myIdx, new EventListener() {
				@Override public void onEvent(Event arg0) throws Exception {
					org.zkoss.zk.ui.event.Events.sendEvent(new Event("onClick", dummyTab));
				}
			});
		}

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setHeight("100%");
		borderlayout.setWidth("100%");
		borderlayout.setParent(panelGuru);

		West west = new West();
		west.setStyle("border:0px;");
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("270px");
		west.setParent(borderlayout);

		fotoGuru = null;
		Common.createDownloadUploadFoto(west, guru, FotoGuru.class, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				fotoGuru = (FotoGuru) arg0.getData();
			}
		}, true);

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

		boolean admin = Common.getApakahAdmin();

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		String statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "kode", labelKode = new MyLabelConfig());
		kode = new Textbox(guru.getKode());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? kode
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getKode())
								: kode);
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "nip", labelNip = new MyLabelConfig());
		nip = new Textbox(guru.getNip());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? nip
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getNip())
								: nip);
		nip.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "nuptk", labelNuptk = new MyLabelConfig());
		nuptk = new Textbox(guru.getNuptk());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? nuptk
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getNuptk())
								: nuptk);
		nuptk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "namaGuru");
		nama = new Textbox(guru.getNamaGuru());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? nama
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getNamaGuru())
								: nama);
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "panggilan");
		panggilan = new Textbox(guru.getPanggilan());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? panggilan
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getPanggilan())
								: panggilan);
		panggilan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "namaAr");
		namaAr = new Textbox(guru.getNamaAr());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? namaAr
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getNamaAr())
								: namaAr);
		namaAr.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "namaCh");
		namaCh = new Textbox(guru.getNamaCh());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? namaCh
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getNamaCh())
								: namaCh);
		namaCh.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "gelarDepan");
		gelarDepan = new Textbox(guru.getGelarDepan());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? gelarDepan
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getGelarDepan())
								: gelarDepan);
		gelarDepan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "gelarBelakang");
		gelarBelakang = new Textbox(guru.getGelarBelakang());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? gelarBelakang
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getGelarBelakang())
								: gelarBelakang);
		gelarBelakang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "statusKepegawaian");
		statusKepegawaian = new Combobox();
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? statusKepegawaian
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(
								guru.getStatusKepegawaian() == null ? "" : guru.getStatusKepegawaian().getNama())
								: statusKepegawaian);
		Common.insertCombo(statusKepegawaian, new String[] { "nama" }, StatusKepegawaian.class,
				Restrictions.ne("nama", ""),
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(statusKepegawaian, guru.getStatusKepegawaian());
		statusKepegawaian.setWidth("90%");
		statusKepegawaian.setReadonly(true);

		jenisKelamin = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig();
		comboitem.setLabel("Laki-laki");
		comboitem.setValue("Laki-laki");
		jenisKelamin.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Perempuan");
		comboitem.setValue("Perempuan");
		jenisKelamin.appendChild(comboitem);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "jenisKelamin");
		Common.selectComboItem(jenisKelamin, guru.getJenisKelamin());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? jenisKelamin
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getJenisKelamin())
								: jenisKelamin);
		jenisKelamin.setWidth("90%");
		jenisKelamin.setReadonly(true);

		row = new MyFormRow();
		statusNikah = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Belum Kawin");
		comboitem.setValue("Belum Kawin");
		statusNikah.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Kawin");
		comboitem.setValue("Kawin");
		statusNikah.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Janda/Duda");
		comboitem.setValue("Janda/Duda");
		statusNikah.appendChild(comboitem);
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "statusNikah");
		Common.selectComboItem(statusNikah, guru.getStatusNikah());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? statusNikah
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getStatusNikah())
								: statusNikah);
		statusNikah.setWidth("90%");
		statusNikah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "tempatLahir");

		tempatLahir = new Textbox(guru.getTempatLahir() == null ? "" : guru.getTempatLahir());
		tanggalLahir = new MyDatebox(guru.getTanggalLahir());

		Hbox hbox = new Hbox();
		hbox.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? tempatLahir
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getTempatLahir())
								: tempatLahir);
		hbox.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? tanggalLahir
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(
								guru.getTanggalLahir() == null ? "" : Common.dateFormat2.get().format(guru.getTanggalLahir()))
								: tanggalLahir);
		row.appendChild(hbox);
		tempatLahir.setCols(15);

		ttd = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanda Tangan (PNG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, guru.getId(), LampiranLain.TTD_GURU, "Tanda Tangan", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ttd = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true, null, false, false);

		hbox.setParent(row);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "yayasan");
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? yayasan
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getYayasan() == null ? "" : guru.getYayasan().getNama())
								: yayasan);
		Common.selectComboItem(yayasan, guru.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "sekolah");
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? sekolah
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getSekolah() == null ? "" : guru.getSekolah().getNama())
								: sekolah);
		Common.pilihSekolah(sekolah, guru.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		Tbmuser tbmuser = Common.getCurrentUser();

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "idfinger");
		idfinger = new Textbox(guru.getIdfinger());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? idfinger
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getIdfinger())
								: idfinger);
		idfinger.setWidth("90%");
		idfinger.setDisabled(tbmuser == null || tbmuser.ambilDosen() != null || tbmuser.getMahasiswa() != null);
		if (Common.bolehKonfigurasi("tampilkan_link_login_oleh_admin_di_data_guru")) {
			if (guru.getId() != null && Common.getApakahAdmin()) {

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Link")));

				final A a = new A("Tampilkan Link");
				a.setHref("");
				row.appendChild(a);

				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String userId = (String) HibernateUtil.currentSession().createCriteria(Tbmuser.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.setProjection(Projections.property("userId"))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("guru", guru)).addOrder(Order.desc("tanggal_dirubah"))
								.setMaxResults(1).uniqueResult();
						if (userId == null) {
							MyMessageboxConfig.show("Data / akun pengguna untuk guru ini tidak ditemukan", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}
						String code = Common.getRequestHostWithProtocol() + "/m?q=" + URLEncoder.encode(
								Common.desEncrypter.get().encrypt(userId + "-user-abcdefghijklmnopqrstuvwxyz"), "UTF-8");
						a.setLabel(code);
						a.setHref(Common.getRequestHostWithProtocol() + "/logoff?param="
								+ URLEncoder.encode(code, "UTF-8"));
					}
				});

				Common.initKeterangan(rows, "Link ini bisa digunakan untuk login tanpa menggunakan password");
			}
		}
		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "sekolah1");
		sekolah1 = new Combobox();
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? sekolah1
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(
										guru.getSekolah1() == null ? "" : guru.getSekolah1().getNama())
								: sekolah1);
		Common.insertComboDanSemua(sekolah1, new String[] { "nama" }, "keterangan", Sekolah.class, "Tidak Ada",
				Restrictions.eq("aktif", true));
		Common.selectComboItem(sekolah1, guru.getSekolah1());
		sekolah1.setWidth("90%");
		sekolah1.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "sekolah2");
		sekolah2 = new Combobox();
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? sekolah2
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(
										guru.getSekolah2() == null ? "" : guru.getSekolah2().getNama())
								: sekolah2);
		Common.insertComboDanSemua(sekolah2, new String[] { "nama" }, "keterangan", Sekolah.class, "Tidak Ada",
				Restrictions.eq("aktif", true));
		Common.selectComboItem(sekolah2, guru.getSekolah2());
		sekolah2.setWidth("90%");
		sekolah2.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "sekolah3");
		sekolah3 = new Combobox();
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? sekolah3
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(
										guru.getSekolah3() == null ? "" : guru.getSekolah3().getNama())
								: sekolah3);
		Common.insertComboDanSemua(sekolah3, new String[] { "nama" }, "keterangan", Sekolah.class, "Tidak Ada",
				Restrictions.eq("aktif", true));
		Common.selectComboItem(sekolah3, guru.getSekolah3());
		sekolah3.setWidth("90%");
		sekolah3.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "statusPegawai");
		statusPegawai = new Combobox();
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? statusPegawai
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(
										guru.getStatusPegawai() == null ? "" : guru.getStatusPegawai().getNama())
								: statusPegawai);
		Common.insertCombo(statusPegawai, "nama", "keterangan", StatusPegawai.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(statusPegawai, guru.getStatusPegawai());
		statusPegawai.setWidth("90%");
		statusPegawai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "tmtKerja");
		tmtKerja = new MyDatebox(guru.getTmtKerja());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? tmtKerja
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(
										guru.getTmtKerja() == null ? "" : Common.dateFormat2.get().format(guru.getTmtKerja()))
								: tmtKerja);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "skCpns");
		skCpns = new Textbox(guru.getSkCpns());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? skCpns
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getSkCpns())
								: skCpns);
		skCpns.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "tglSkCpns");
		tglSkCpns = new MyDatebox(guru.getTglSkCpns());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? tglSkCpns
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(
								guru.getTglSkCpns() == null ? "" : Common.dateFormat2.get().format(guru.getTglSkCpns()))
								: tglSkCpns);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "tmtPns");
		tmtPns = new MyDatebox(guru.getTmtPns());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? tmtPns
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(
										guru.getTmtPns() == null ? "" : Common.dateFormat2.get().format(guru.getTmtPns()))
								: tmtPns);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "golonganPegawai");
		golonganPegawai = new Combobox();
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? golonganPegawai
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(
										guru.getGolonganPegawai() == null ? "" : guru.getGolonganPegawai().getNama())
								: golonganPegawai);
		Common.insertComboDanSemua(golonganPegawai, new String[] { "kode", "nama" }, "keterangan", GolonganPns.class,
				"=Tanpa Golongan=", Restrictions.eq("aktif", true));
		Common.selectComboItem(golonganPegawai, guru.getGolonganPegawai());
		golonganPegawai.setWidth("90%");
		golonganPegawai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "skAngkat");
		skAngkat = new Textbox(guru.getSkAngkat());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? skAngkat
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getSkAngkat())
								: skAngkat);
		skAngkat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "tmtSkAngkat");
		tmtSkAngkat = new MyDatebox(guru.getTmtSkAngkat());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? tmtSkAngkat
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(
								guru.getTmtSkAngkat() == null ? "" : Common.dateFormat2.get().format(guru.getTmtSkAngkat()))
								: tmtSkAngkat);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "lembagaPengangkat");
		lembagaPengangkat = new Combobox();
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? lembagaPengangkat
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(
								guru.getLembagaPengangkat() == null ? "" : guru.getLembagaPengangkat().getNama())
								: lembagaPengangkat);
		Common.insertCombo(lembagaPengangkat, new String[] { "nama" }, LembagaPengangkat.class,
				Restrictions.ne("nama", ""),
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(lembagaPengangkat, guru.getLembagaPengangkat());
		lembagaPengangkat.setWidth("90%");
		lembagaPengangkat.setReadonly(true);

		row = new MyFormRow();

		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "sumberGaji");
		sumberGaji = new Combobox();
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? sumberGaji
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(
										guru.getSumberGaji() == null ? "" : guru.getSumberGaji().getNama())
								: sumberGaji);
		Common.insertCombo(sumberGaji, new String[] { "nama" }, SumberGaji.class, Restrictions.ne("nama", ""),
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(sumberGaji, guru.getSumberGaji());
		sumberGaji.setWidth("90%");
		sumberGaji.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "jenisPendidikDanTenagaKependidikan");
		jenisPendidikDanTenagaKependidikan = new Combobox();
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
				? jenisPendidikDanTenagaKependidikan
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getJenisPendidikDanTenagaKependidikan() == null ? ""
										: guru.getJenisPendidikDanTenagaKependidikan().getNama())
								: jenisPendidikDanTenagaKependidikan);
		Common.insertCombo(jenisPendidikDanTenagaKependidikan, new String[] { "nama" },
				JenisPendidikDanTenagaKependidikan.class, Restrictions.ne("nama", ""),
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jenisPendidikDanTenagaKependidikan, guru.getJenisPendidikDanTenagaKependidikan());
		jenisPendidikDanTenagaKependidikan.setWidth("90%");
		jenisPendidikDanTenagaKependidikan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "pendidikan");
		pendidikan = new Combobox();
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? pendidikan
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(
										guru.getPendidikan() == null ? "" : guru.getPendidikan().getNama())
								: pendidikan);
		Common.insertCombo(pendidikan, "nama", Pendidikan.class);
		Common.selectComboItem(pendidikan, guru.getPendidikan());
		pendidikan.setWidth("90%");
		pendidikan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "jurusan");
		jurusan = new Textbox(guru.getJurusan());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? jurusan
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getJurusan())
								: jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "jenisGuru");
		jenisGuru = new Combobox();
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? jenisGuru
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(
										guru.getJenisGuru() == null ? "" : guru.getJenisGuru().getNama())
								: jenisGuru);
		Common.insertCombo(jenisGuru, new String[] { "nama", "sekolah" }, JenisGuru.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jenisGuru, guru.getJenisGuru());
		jenisGuru.setWidth("90%");
		jenisGuru.setReadonly(true);

		EventListener eventListenerJenisGuru = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				Common.insertCombo(jenisGuru, new String[] { "nama", "sekolah" }, JenisGuru.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				Common.selectComboItem(jenisGuru, guru.getJenisGuru());
			}
		};

		sekolah.addEventListener("onChange", eventListenerJenisGuru);

		Common.createDefaultTimer(eventListenerJenisGuru);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "agama");
		agama = new Combobox();
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? agama
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getAgama() == null ? "" : guru.getAgama().getNama())
								: agama);
		Common.insertCombo(agama, "nama", "keterangan", Agama.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(agama, guru.getAgama());
		agama.setWidth("90%");
		agama.setReadonly(true);

		kewarganegaraan = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(ais.database.model.Mahasiswa.WNI);
		comboitem.setValue(ais.database.model.Mahasiswa.WNI);
		kewarganegaraan.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(ais.database.model.Mahasiswa.WNA);
		comboitem.setValue(ais.database.model.Mahasiswa.WNA);
		kewarganegaraan.appendChild(comboitem);
		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "kewarganegaraan");
		Common.selectComboItem(kewarganegaraan, guru.getKewarganegaraan());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? kewarganegaraan
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getKewarganegaraan())
								: kewarganegaraan);
		kewarganegaraan.setWidth("90%");
		kewarganegaraan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "negara");
		negara = new AmbilDataNegaraBanbox();
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? negara
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getNegara() == null ? "" : guru.getNegara().getNama())
								: negara);

		try {
			negara.setAttribute("negara", guru.getNegara() == null ? ConstantValues.INDONESIA : guru.getNegara());
			negara.setValue((guru.getNegara() == null ? ConstantValues.INDONESIA : guru.getNegara()).getNamaNegara());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/GuruAction.java:2389");
			// TODO: handle exception
		}

		negara.setReadonly(true);
		negara.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "teleponGuru");
		teleponGuru = new Textbox(guru.getTeleponGuru());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? teleponGuru
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getTeleponGuru())
								: teleponGuru);
		teleponGuru.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "hp");
		hp = new Textbox(guru.getHp());
		row.appendChild(
				admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? hp
						: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
								|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
										? new MyLabelAgakKecilBold(guru.getHp())
										: hp);
		hp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "alamatEmail");
		alamatEmail = new Textbox(guru.getAlamatEmail());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? alamatEmail
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getAlamatEmail())
								: alamatEmail);
		alamatEmail.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ConstantValues.penggunaanLabelBahasa);
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "bahasa");
		bahasa = new Combobox();
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? bahasa
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getBahasa())
								: bahasa);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Tbmuser.INDONESIA);
		comboitem.setValue(Tbmuser.INDONESIA);
		bahasa.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Tbmuser.ENGLISH);
		comboitem.setValue(Tbmuser.ENGLISH);
		bahasa.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Tbmuser.ARAB);
		comboitem.setValue(Tbmuser.ARAB);
		bahasa.appendChild(comboitem);
		bahasa.setWidth("90%");
		bahasa.setReadonly(true);

		Common.selectComboItem(bahasa, guru.getBahasa());

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "nik");
		nik = new Textbox(guru.getNik());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? nik
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getNik())
								: nik);
		nik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "kk");
		kk = new Textbox(guru.getKk());
		row.appendChild(
				admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? kk
						: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
								|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
										? new MyLabelAgakKecilBold(guru.getKk())
										: kk);
		kk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "karpeg");
		karpeg = new Textbox(guru.getKarpeg());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? karpeg
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getKarpeg())
								: karpeg);
		karpeg.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "karisKarsu");
		karisKarsu = new Textbox(guru.getKarisKarsu());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? karisKarsu
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getKarisKarsu())
								: karisKarsu);
		karisKarsu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "sudahLisensiKepalaSekolah");
		sudahLisensiKepalaSekolah = new MyCheckboxConfig("Sudah Lisensi Kepala Sekolah");
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? sudahLisensiKepalaSekolah
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getSudahLisensiKepalaSekolah() ? "Ya" : "Yidak")
								: sudahLisensiKepalaSekolah);
		sudahLisensiKepalaSekolah.setChecked(guru.getSudahLisensiKepalaSekolah());

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "nuks");
		nuks = new Textbox(guru.getNuks());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? nuks
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getNuks())
								: nuks);
		nuks.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "pernahDiklatKepengawasan");
		pernahDiklatKepengawasan = new MyCheckboxConfig("Pernah Diklat Kepengawasan");
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? pernahDiklatKepengawasan
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getPernahDiklatKepengawasan() ? "Ya" : "Yidak")
								: pernahDiklatKepengawasan);
		pernahDiklatKepengawasan.setChecked(guru.getPernahDiklatKepengawasan());

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "keahlianBraille");
		keahlianBraille = new MyCheckboxConfig("Keahlian Braille");
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? keahlianBraille
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getKeahlianBraille() ? "Ya" : "Yidak")
								: keahlianBraille);
		keahlianBraille.setChecked(guru.getKeahlianBraille());

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "keahlianBahasaIsyarat");
		keahlianBahasaIsyarat = new MyCheckboxConfig("Keahlian Bahasa Isyarat");
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? keahlianBahasaIsyarat
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getKeahlianBahasaIsyarat() ? "Ya" : "Yidak")
								: keahlianBahasaIsyarat);
		keahlianBahasaIsyarat.setChecked(guru.getKeahlianBahasaIsyarat());

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "bank");
		bank = new Textbox(guru.getBank());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? bank
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getBank())
								: bank);
		bank.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "nomorRekeningBank");
		nomorRekeningBank = new Textbox(guru.getNomorRekeningBank());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? nomorRekeningBank
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getNomorRekeningBank())
								: nomorRekeningBank);
		nomorRekeningBank.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "rekeningAtasNama");
		rekeningAtasNama = new Textbox(guru.getRekeningAtasNama());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? rekeningAtasNama
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getRekeningAtasNama())
								: rekeningAtasNama);
		rekeningAtasNama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "npwp");
		npwp = new Textbox(guru.getNpwp());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? npwp
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getNpwp())
								: npwp);
		npwp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "namaWajibPajak");
		namaWajibPajak = new Textbox(guru.getNamaWajibPajak());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? namaWajibPajak
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getNamaWajibPajak())
								: namaWajibPajak);
		namaWajibPajak.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "alamatGuru");
		alamatGuru = new Textbox(guru.getAlamatGuru());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? alamatGuru
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getAlamatGuru())
								: alamatGuru);
		alamatGuru.setWidth("90%");
		alamatGuru.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "rt");
		rt = new Textbox(guru.getRt());
		row.appendChild(
				admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? rt
						: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
								|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
										? new MyLabelAgakKecilBold(guru.getRt())
										: rt);
		rt.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "rw");
		rw = new Textbox(guru.getRw());
		row.appendChild(
				admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? rw
						: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
								|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
										? new MyLabelAgakKecilBold(guru.getRw())
										: rw);
		rw.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "dusun");
		dusun = new Textbox(guru.getDusun());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? dusun
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getDusun())
								: dusun);
		dusun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "kelurahan");
		kelurahan = new Textbox(guru.getKelurahan());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? kelurahan
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getKelurahan())
								: kelurahan);
		kelurahan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "kecamatan");
		kecamatan = new AmbilDataKecamatanBanbox();
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? kecamatan
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(
										guru.getKecamatan() == null ? "" : guru.getKecamatan().getNama())
								: kecamatan);
		kecamatan.setWidth("90%");
		kecamatan.setAttribute("wilayah", guru.getKecamatan());
		kecamatan.setValue(guru.getKecamatan() == null ? "" : guru.getKecamatan().getNama());
		kecamatan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "kodePos");
		kodePos = new Textbox(guru.getKodePos());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? kodePos
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getKodePos())
								: kodePos);
		kodePos.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "lintang");
		lintang = new Textbox(guru.getLintang());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? lintang
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getLintang())
								: lintang);
		lintang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "bujur");
		bujur = new Textbox(guru.getBujur());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? bujur
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getBujur())
								: bujur);
		bujur.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "namaAyah");
		namaAyah = new Textbox(guru.getNamaAyah() == null ? "" : guru.getNamaAyah());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? namaAyah
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getNamaAyah())
								: namaAyah);
		namaAyah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "pekerjaanAyah");
		pekerjaanAyah = new Combobox();
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? pekerjaanAyah
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(
										guru.getPekerjaanAyah() == null ? "" : guru.getPekerjaanAyah().getNama())
								: pekerjaanAyah);
		Common.insertCombo(pekerjaanAyah, "nama", PekerjaanOrangTua.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(pekerjaanAyah, guru.getPekerjaanAyah());
		pekerjaanAyah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "namaIbu");
		namaIbu = new Textbox(guru.getNamaIbu() == null ? "" : guru.getNamaIbu());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? namaIbu
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getNamaIbu())
								: namaIbu);
		namaIbu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "pekerjaanIbu");// row.appendChild(new
		pekerjaanIbu = new Combobox();
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? pekerjaanIbu
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(
										guru.getPekerjaanIbu() == null ? "" : guru.getPekerjaanIbu().getNama())
								: pekerjaanIbu);
		Common.insertCombo(pekerjaanIbu, "nama", PekerjaanOrangTua.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(pekerjaanIbu, guru.getPekerjaanIbu());
		pekerjaanIbu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "namaSuamiIstri");
		namaSuamiIstri = new Textbox(guru.getNamaSuamiIstri());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? namaSuamiIstri
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getNamaSuamiIstri())
								: namaSuamiIstri);
		namaSuamiIstri.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "nipSuamiIstri");
		nipSuamiIstri = new Textbox(guru.getNipSuamiIstri());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? nipSuamiIstri
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getNipSuamiIstri())
								: nipSuamiIstri);
		nipSuamiIstri.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "pekerjaanSuamiIstri");
		pekerjaanSuamiIstri = new Combobox();
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? pekerjaanSuamiIstri
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(
								guru.getPekerjaanSuamiIstri() == null ? "" : guru.getPekerjaanSuamiIstri().getNama())
								: pekerjaanSuamiIstri);
		Common.insertCombo(pekerjaanSuamiIstri, "nama", PekerjaanOrangTua.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(pekerjaanSuamiIstri, guru.getPekerjaanSuamiIstri());
		pekerjaanAyah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "tugasTambahan");
		tugasTambahan = new Textbox(guru.getTugasTambahan());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? tugasTambahan
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getTugasTambahan())
								: tugasTambahan);
		tugasTambahan.setWidth("90%");
		tugasTambahan.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "mengajar");
		mengajar = new Textbox(guru.getMengajar());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? mengajar
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getMengajar())
								: mengajar);
		mengajar.setWidth("90%");
		mengajar.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "jamTugasTambahan");
		jamTugasTambahan = new Textbox(guru.getJamTugasTambahan());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? jamTugasTambahan
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getJamTugasTambahan())
								: jamTugasTambahan);
		jamTugasTambahan.setWidth("90%");
		jamTugasTambahan.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "jjm");
		jjm = new Textbox(guru.getJjm());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? jjm
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getJjm())
								: jjm);
		jjm.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "totalJjm");
		totalJjm = new Textbox(guru.getTotalJjm());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? totalJjm
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getTotalJjm())
								: totalJjm);
		totalJjm.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "siswa");
		siswa = new Textbox(guru.getSiswa());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? siswa
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(guru.getSiswa())
								: siswa);
		siswa.setWidth("90%");
		siswa.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "kompetensi");
		kompetensi = new Textbox(guru.getKompetensi());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? kompetensi
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getKompetensi())
								: kompetensi);
		kompetensi.setWidth("90%");
		kompetensi.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "sertifikasi");
		sertifikasi = new Textbox(guru.getSertifikasi());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? sertifikasi
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getSertifikasi())
								: sertifikasi);
		sertifikasi.setWidth("90%");
		sertifikasi.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		statusWajibIsi = Common.checkApakahLabelGuruTampil(row, "keterangan");
		keterangan = new Textbox(guru.getKeterangan());
		row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? keterangan
				: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
								? new MyLabelAgakKecilBold(guru.getKeterangan())
								: keterangan);
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayoutUtama);

		Toolbar toolbar = new Toolbar();
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

					if (biodataPegawaiAction != null) {
						Common.createDefaultTimerNoBusy(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								biodataPegawaiAction.save(arg0);
							}
						}, "", false, 500);
					}

					if (GuruAction.this.eventListener != null) {
						GuruAction.this.eventListener.onEvent(new Event("", event.getTarget(), guru));
					}

				}
			}
		});
		save.setParent(toolbar);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Guru harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Yayasan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Sekolah harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (!kode.getValue().trim().equals("")) {

			Integer kotaCount = null;
			Session session = HibernateUtil.currentSession();
			kotaCount = ((Number) session.createCriteria(Guru.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount()).add(Restrictions.eq("kode", kode.getValue().trim()))
					.add(this.guru.getId() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ne("id", this.guru.getId()))
					.uniqueResult()).intValue();
			if (kotaCount > 0) {
				MyMessageboxConfig.show(labelKode.getValue() + " guru tidak boleh sama", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		if (!nip.getValue().trim().equals("")) {

			Integer kotaCount = null;
			Session session = HibernateUtil.currentSession();
			kotaCount = ((Number) session.createCriteria(Guru.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount()).add(Restrictions.eq("nip", nip.getValue().trim()))
					.add(this.guru.getId() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ne("id", this.guru.getId()))
					.uniqueResult()).intValue();
			if (kotaCount > 0) {
				MyMessageboxConfig.show(labelNip.getValue() + " guru tidak boleh sama", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		if (!nuptk.getValue().trim().equals("")) {

			Integer kotaCount = null;
			Session session = HibernateUtil.currentSession();
			kotaCount = ((Number) session.createCriteria(Guru.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount()).add(Restrictions.eq("nuptk", nuptk.getValue().trim()))
					.add(this.guru.getId() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ne("id", this.guru.getId()))
					.uniqueResult()).intValue();
			if (kotaCount > 0) {
				MyMessageboxConfig.show(labelNuptk.getValue() + " guru tidak boleh sama", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		if (guru.getId() != null) {
			guru = (Guru) session.load(Guru.class, guru.getId());

		}

		guru.setNamaGuru(nama.getValue());
		guru.setNamaAr(namaAr.getValue());
		guru.setNamaCh(namaCh.getValue());
		guru.setPanggilan(panggilan.getValue());
		guru.setJenisKelamin(jenisKelamin.getValue());
		guru.setTempatLahir(tempatLahir.getValue());
		guru.setTanggalLahir(tanggalLahir.getValue());
		guru.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		guru.setAgama((Agama) (agama.getSelectedItem() == null ? null : agama.getSelectedItem().getValue()));
		guru.setKewarganegaraan(kewarganegaraan.getValue());
		guru.setNegara((Negara) negara.getAttribute("negara"));
		if (statusPegawai != null)
			guru.setStatusPegawai((StatusPegawai) (statusPegawai.getSelectedItem() == null ? null
					: statusPegawai.getSelectedItem().getValue()));
		guru.setJenisGuru(
				(JenisGuru) (jenisGuru.getSelectedItem() == null ? null : jenisGuru.getSelectedItem().getValue()));
		guru.setTeleponGuru(teleponGuru.getValue());
		guru.setAlamatEmail(alamatEmail.getValue());
		guru.setNip(nip.getValue());
		guru.setKode(kode.getValue());
		guru.setAlamatGuru(alamatGuru.getValue());
		guru.setKeterangan(keterangan.getValue());
		guru.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		guru.setBahasa((String) (bahasa.getSelectedItem() == null ? null : bahasa.getSelectedItem().getValue()));
		guru.setLintang(lintang.getValue());
		guru.setBujur(bujur.getValue());
		guru.setNuptk(nuptk.getValue());
		guru.setNik(nik.getValue());
		guru.setStatusKepegawaian((StatusKepegawaian) (statusKepegawaian.getSelectedItem() == null ? null
				: statusKepegawaian.getSelectedItem().getValue()));
		guru.setJenisPendidikDanTenagaKependidikan(
				(JenisPendidikDanTenagaKependidikan) (jenisPendidikDanTenagaKependidikan.getSelectedItem() == null
						? null
						: jenisPendidikDanTenagaKependidikan.getSelectedItem().getValue()));

		guru.setSumberGaji(
				(SumberGaji) (sumberGaji.getSelectedItem() == null ? null : sumberGaji.getSelectedItem().getValue()));

		guru.setTmtPns(tmtPns.getValue());

		guru.setSkAngkat(skAngkat.getValue());
		guru.setSkCpns(skCpns.getValue());
		guru.setTglSkCpns(tglSkCpns.getValue());
		guru.setTmtSkAngkat(tmtSkAngkat.getValue());

		guru.setGolonganPegawai((GolonganPns) (golonganPegawai.getSelectedItem() == null ? null
				: golonganPegawai.getSelectedItem().getValue()));

		guru.setLembagaPengangkat((LembagaPengangkat) (lembagaPengangkat.getSelectedItem() == null ? null
				: lembagaPengangkat.getSelectedItem().getValue()));

		guru.setHp(hp.getValue());
//		"rt",
		guru.setRt(rt.getValue());
//		"rw",
		guru.setRw(rw.getValue());
//		"dusun",
		guru.setDusun(dusun.getValue());
//		"kelurahan",
		guru.setKelurahan(kelurahan.getValue());
//		"kecamatan",
		guru.setKecamatan((Wilayah) kecamatan.getAttribute("wilayah"));
//		"kodePos",
		guru.setKodePos(kodePos.getValue());

		guru.setGelarBelakang(gelarBelakang.getValue());
		guru.setGelarDepan(gelarDepan.getValue());

		guru.setPendidikan(
				(Pendidikan) (pendidikan.getSelectedItem() == null ? null : pendidikan.getSelectedItem().getValue()));

		guru.setJurusan(jurusan.getValue());

		guru.setNamaSuamiIstri(namaSuamiIstri.getValue());
		guru.setNipSuamiIstri(nipSuamiIstri.getValue());
		guru.setPekerjaanSuamiIstri((PekerjaanOrangTua) (pekerjaanSuamiIstri.getSelectedItem() == null ? null
				: pekerjaanSuamiIstri.getSelectedItem().getValue()));

		guru.setStatusNikah(
				(String) (statusNikah.getSelectedItem() == null ? null : statusNikah.getSelectedItem().getValue()));

		guru.setNamaAyah(namaAyah.getValue());
		guru.setPekerjaanAyah((PekerjaanOrangTua) (pekerjaanAyah.getSelectedItem() == null ? null
				: pekerjaanAyah.getSelectedItem().getValue()));
		guru.setNamaIbu(namaIbu.getValue());
		guru.setPekerjaanIbu((PekerjaanOrangTua) (pekerjaanIbu.getSelectedItem() == null ? null
				: pekerjaanIbu.getSelectedItem().getValue()));

		guru.setTmtKerja(tmtKerja.getValue());
		guru.setTugasTambahan(tugasTambahan.getValue());
		guru.setMengajar(mengajar.getValue());
		guru.setJamTugasTambahan(jamTugasTambahan.getValue());
		guru.setJjm(jjm.getValue());
		guru.setTotalJjm(totalJjm.getValue());
		guru.setKk(kk.getValue());

		guru.setKarpeg(karpeg.getValue());
		guru.setKarisKarsu(karisKarsu.getValue());
		guru.setNuks(nuks.getValue());
		guru.setBank(bank.getValue());
		guru.setNomorRekeningBank(nomorRekeningBank.getValue());
		guru.setRekeningAtasNama(rekeningAtasNama.getValue());

		guru.setSudahLisensiKepalaSekolah(sudahLisensiKepalaSekolah.isChecked());

		guru.setPernahDiklatKepengawasan(pernahDiklatKepengawasan.isChecked());
		guru.setKeahlianBahasaIsyarat(keahlianBahasaIsyarat.isChecked());
		guru.setKeahlianBraille(keahlianBraille.isChecked());

		guru.setNpwp(npwp.getValue());
		guru.setNamaWajibPajak(namaWajibPajak.getValue());
		guru.setSiswa(siswa.getValue());
		guru.setKompetensi(kompetensi.getValue());
		guru.setSertifikasi(sertifikasi.getValue());

		guru.setSekolah1((Sekolah) (sekolah1.getSelectedItem() == null ? null : sekolah1.getSelectedItem().getValue()));
		guru.setSekolah2((Sekolah) (sekolah2.getSelectedItem() == null ? null : sekolah2.getSelectedItem().getValue()));
		guru.setSekolah3((Sekolah) (sekolah3.getSelectedItem() == null ? null : sekolah3.getSelectedItem().getValue()));
		guru.setIdfinger(idfinger.getValue().trim());

		List<String> daftarWajibDiisi = KonfigurasiTampilanGuruAction.dataYangWajibDiisi();
		for (String key : daftarWajibDiisi) {
			if (Common.checkIsNull(Guru.class, guru, key)) {

				MyMessageboxConfig.show(
						"Biodata Guru harus dilengkapi. Data \"" + KonfigurasiTampilanGuruAction.keyDesc(key)
								+ "\" masih belum terisi dengan benar",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		Common.refreshSaveOrUpdate(session, guru);

		// FIX akar masalah GenericJDBCException "could not update: Guru#..." (KE-4): flush()
		// di sini sebelumnya TIDAK dibungkus try/catch, padahal ini titik yang memaksa UPDATE
		// baris Guru benar-benar dieksekusi ke database. Kalau baris ini sedang dikunci proses
		// lain (mis. guru yang sama sedang diedit bersamaan) dan PostgreSQL membatalkan
		// statement karena statement_timeout, exception mentah keluar tak tertangani dan
		// transaksi currentSession() jadi "aborted" utk sisa request. Tangkap di sini dgn
		// rollback + pesan jelas, konsisten dgn pola yg sudah dipakai di titik lain file ini.
		try {
			session.flush();
		} catch (Exception eFlush) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) { ais.common.ErrorAuditUtil.record(eRollback,
					"auto-audit(rollback-gagal) src/ais/action/master/sekolah/GuruAction.java onSave");
			}
			session.clear();
			ais.common.ErrorAuditUtil.record(eFlush,
					"auto-audit(lock-timeout) src/ais/action/master/sekolah/GuruAction.java onSave");
			MyMessageboxConfig.show(
					"Mohon maaf, data Guru ini sedang diproses oleh pengguna lain di waktu yang bersamaan sehingga penyimpanan gagal. "
							+ "Langkah yang dapat dilakukan: (1) tunggu beberapa saat; (2) muat ulang (refresh) halaman ini; (3) ulangi proses simpan. "
							+ "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		try {
			if (pegawaiDiinput) {
				if (guru != null && guru.getId() != null) {
					Pegawai pegawai = (Pegawai) ConstantValues.simpleObject(
							session.createCriteria(Pegawai.class).add(Restrictions.eq("guru", guru)).setMaxResults(1),
							Pegawai.class);
					if (pegawai == null) {
						pegawai = new Pegawai();
						pegawai.setGuru(guru);
						session.save(pegawai);
						session.flush();

						guru.setPegawai(pegawai);
						guru.setPegawaiId(pegawai.getId());
						// FIX "createCriteria is not valid without active transaction" (KE-1): `session`
						// di sini adalah HibernateUtil.currentSession() -- transaksinya dikelola OTOMATIS
						// oleh framework sepanjang request (dimulai di awal, di-commit di akhir).
						// Memanggil getTransaction().begin()/commit() manual di sini MENGAKHIRI transaksi
						// itu lebih awal; kode ZK lain yang memakai currentSession() belakangan dalam
						// request/klik yang sama (mis. tab "Data Mobile") lalu gagal krn tidak ada
						// transaksi aktif lagi. Cukup flush() spt pola yg sudah dipakai di atas (baris
						// Common.refreshSaveOrUpdate(session, guru); session.flush();) -- perubahan tetap
						// langsung terlihat tanpa mengakhiri transaksi request.
						Common.refreshUpdate(session, guru);
						session.flush();

						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
						FotoGuru fotoGuru = (FotoGuru) streamingSession.createCriteria(FotoGuru.class)
								.add(Restrictions.eq("guru", guru.getId())).setMaxResults(1).uniqueResult();
						if (fotoGuru != null) {
							FotoPegawai fotoPegawai = new FotoPegawai();
							fotoPegawai.setNama(fotoGuru.getNama());
							fotoPegawai.setKeterangan(fotoGuru.getKeterangan());
							fotoPegawai.setPegawai(pegawai.getId());
							fotoPegawai.setFoto(fotoGuru.getFoto());

							streamingSession.getTransaction().begin();
							streamingSession.save(fotoPegawai);
							streamingSession.getTransaction().commit();
						}
						StreamingHibernateUtil.getInstance().closeSession();
					}
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		if (fotoGuru != null) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.refresh(fotoGuru);
			fotoGuru.setGuru(guru.getId());
			streamingSession.getTransaction().begin();
			streamingSession.update(fotoGuru);
			streamingSession.getTransaction().commit();

			StreamingHibernateUtil.getInstance().closeSession();
		}

		if (ttd != null && ttd.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(ttd);
				ttd.setRef(guru.getId());

				session.getTransaction().begin();
				session.update(ttd);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		return true;
	}

	protected LampiranLain ttd;

	public Criteria initCriteria(boolean order) {
		// currentSession (ThreadLocalSessionContext) menuntut transaksi aktif untuk createCriteria
		// ("createCriteria is not valid without active transaction"). Pakai native session yang
		// boleh createCriteria tanpa transaksi aktif & dijamin open.
		Session session = HibernateUtil.currentNativeSession();
		Criteria criteria = session.createCriteria(Guru.class).add(Restrictions.isNotNull("sekolah"))
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("namaGuru"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("namaGuru", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nuptk", searchkode.getValue().trim(), MatchMode.ANYWHERE))

				)

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1") :

								Restrictions.or(CommonSearchFilterHelper.eqSelectedWithId("sekolah3", searchsekolah, false),
										Restrictions.or(
												CommonSearchFilterHelper.eqSelectedWithId("sekolah2", searchsekolah, false),
												Restrictions.or(
														CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false),
														CommonSearchFilterHelper.eqSelectedWithId("sekolah1", searchsekolah, false))))

				)

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchnama == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);

		List<Guru> guru = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(guru);
		grid.setRowRenderer(new GuruRenderer());
		grid.setModelCheckMobile(strset);

	}

	private Tabpanel konfigurasiBiodataGuru;

	public void onKonfigurasiBiodataGuru(Event event) {
		if (konfigurasiBiodataGuru.getChildren().size() == 0) {

			ais.ui.util.MyButtonTabbox btnTabKfg = ais.ui.util.MyButtonTabbox.buat(konfigurasiBiodataGuru, "100%", new int[] { 0 });
			{
				org.zkoss.zul.Div panel = btnTabKfg.tambahTab(0, "Konfigurasi", "/img/svg/dashboard-speed.svg");
				MyIframe include = new MyIframe("/pages/master/konfigurasi_guru.zul");
				include.setHeight("100%"); include.setWidth("100%"); include.setParent(panel);
			}
			btnTabKfg.tambahTabLazy(1, "Ikatan Kerja", "/img/svg/clipboard-check.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyIframe inc = new MyIframe("/pages/master/ikatan_kerja_dosen.zul");
					inc.setHeight("100%"); inc.setWidth("100%"); inc.setParent(panel);
				}
			});
			btnTabKfg.tambahTabLazy(2, "Status Kepegawaian", "/img/svg/user-pen.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyIframe inc = new MyIframe("/pages/master/status_kepegawaian.zul");
					inc.setHeight("100%"); inc.setWidth("100%"); inc.setParent(panel);
				}
			});
			btnTabKfg.tambahTabLazy(3, "Jenis Pendidik Dan Tenaga Kependidikan", "/img/svg/chalkboard-user.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyIframe inc = new MyIframe("/pages/master/jenis_pendidik_dan_tenaga_kependidikan.zul");
					inc.setHeight("100%"); inc.setWidth("100%"); inc.setParent(panel);
				}
			});
			btnTabKfg.tambahTabLazy(4, "Lembaga Pengangkat", "/img/svg/user-tie.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyIframe inc = new MyIframe("/pages/master/lembaga_pengangkat.zul");
					inc.setHeight("100%"); inc.setWidth("100%"); inc.setParent(panel);
				}
			});
			btnTabKfg.tambahTabLazy(5, "Sumber Gaji", "/img/svg/money-bills.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyIframe inc = new MyIframe("/pages/master/sumber_gaji.zul");
					inc.setHeight("100%"); inc.setWidth("100%"); inc.setParent(panel);
				}
			});
			btnTabKfg.tambahTabLazy(6, "Status Pegawai", "/img/svg/person-check.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyIframe inc = new MyIframe("/pages/master/status_pegawai.zul");
					inc.setHeight("100%"); inc.setWidth("100%"); inc.setParent(panel);
				}
			});
			btnTabKfg.tambahTabLazy(7, "Jabatan", "/img/svg/user-business.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyIframe inc = new MyIframe("/pages/master/jabatan.zul");
					inc.setHeight("100%"); inc.setWidth("100%"); inc.setParent(panel);
				}
			});
			btnTabKfg.tambahTabLazy(8, "Tenaga Kependidikan", "/img/svg/users.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyIframe inc = new MyIframe("/pages/master/jenis_tenaga_kependidikan.zul");
					inc.setHeight("100%"); inc.setWidth("100%"); inc.setParent(panel);
				}
			});
			btnTabKfg.tambahTabLazy(9, "Golongan PNS", "/img/svg/user-rectangle-light.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyIframe inc = new MyIframe("/pages/master/golongan_pns.zul");
					inc.setHeight("100%"); inc.setWidth("100%"); inc.setParent(panel);
				}
			});
		}
	}
}
