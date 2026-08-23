package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
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
import org.hibernate.metadata.ClassMetadata;
import org.json.JSONArray;
import org.json.JSONObject;
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
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Decimalbox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.West;

import ais.action.master.dashboard.admin.DashboardKegiatanKemahasiswaanAdmin;
import ais.action.master.helper.FilterLanjutHelper;
import ais.action.master.dashboard.admin.DashboardMahasiswa;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.NeoFeederProgressHelper;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.feeder.util.FeederJSONImport;
import ais.action.master.feeder.util.FeederUtil;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataKelasBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.AmbilDataNegaraBanbox;
import ais.action.master.helper.AmbilDataPerguruanTinggiLainBanbox;
import ais.action.master.helper.HistoryStatusMahasiswaUtil;
import ais.action.master.helper.KrsDetailHelper;
import ais.action.master.helper.ParameterTambahanAlumniListener;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.RevisiMahasiswaHelper;
import ais.action.master.helper.TampilStudiMahasiswaHelper;
import ais.action.master.helper.impor.ImportFromEpsbedHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.generic.v2.adapter.MahasiswaExistingBusinessRules;
import ais.action.report.CommonReportHelper;
import ais.action.report.format1.akademik.LaporanAlbumMahasiswaPerProdiDanAngkatan;
import ais.action.report.format1.akademik.LaporanFormatEMIS;
import ais.action.report.format1.akademik.LaporanFormatPTKKN;
import ais.action.report.format1.akademik.LaporanKHS;
import ais.action.report.format1.akademik.LaporanKartuMahasiswa;
import ais.action.report.format1.akademik.LaporanRekapJumlahMahasiswa;
import ais.action.report.format1.akademik.LaporanSurat;
import ais.action.report.format1.akademik.LaporanTranskipAkademik;
import ais.action.report.helper.nilai.LaporanDaftarPrestasiBelajarWindow;
import ais.common.AsyncTaskManager;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPMB;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.MD5;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.OjsHibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.CommonVO;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.JenisPembiayaanMahasiswa;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jenjang;
import ais.database.model.Judisium;
import ais.database.model.Jurusan;
import ais.database.model.Kelas;
import ais.database.model.Konfigurasi;
import ais.database.model.Konsentrasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Negara;
import ais.database.model.Pekerjaan;
import ais.database.model.Penghasilan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.PerguruanTinggiLain;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusDomisiliSetelahLulus;
import ais.database.model.StatusKeluar;
import ais.database.model.StatusMahasiswa;
import ais.database.model.StatusPekerjaanSetelahLulus;
import ais.database.model.StatusSetelahLulus;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.UserAccess;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoMahasiswa;
import ais.database.model.file.FotoMahasiswaLulus;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.LampiranLainMahasiswa;
import ais.database.model.ojs.Journals;
import ais.database.model.ojs.Roles;
import ais.database.model.ojs.Users;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class MahasiswaAction extends GenericAutowireComposer implements DataLoader, DataCriteria, DataSearchDefault {

	/**
	 *
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;

	private MyGrid grid;
	private Textbox searchnim;
	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Decimalbox searchtahun;
	private Decimalbox searchtahunLulus;
	private Decimalbox searchsemesterLulus;
	private Decimalbox searchMasa;
	protected Combobox searchTahunAjaran;
	protected Combobox searchJenisSemester;
	private Combobox searchsemesterawal;
	private Combobox searchprogram;
	private Combobox searchstatus;
	private Checkbox searchaktif;
	private Combobox searchkewarganegaraan;

	private Combobox searchStatusAwalMahasiswa;
	private Combobox searchjenjang;
	private Combobox kewarganegaraan;
	private Combobox searchJenisSeleksi;

	private Combobox searchstatusKeluar;
	private Combobox searchpredikatKelulusan;
	private Combobox searchstatusSetelahLulus;
	private Combobox searchstatusPekerjaanSetelahLulus;
	private Combobox searchstatusDomisiliSetelahLulus;

	private MyCheckboxConfig searchdosenPA;
	private MyCheckboxConfig searchAdadosenPA;
	private MyCheckboxConfig searchTidakKelas;
	private MyCheckboxConfig searchTidakjenisKelamin;
	private MyCheckboxConfig searchAdaKelas;
	private MyCheckboxConfig searchBelumMasukFeeder;
	private MyCheckboxConfig searchMasukFeeder;
	private AmbilDataDosenBanbox searchdosen;
	protected AmbilDataKelasBanbox searchkelas;
	private MyCheckboxConfig searchFacebook;
	private MyCheckboxConfig searchGoogle;
	private MyCheckboxConfig searchTwitter;
	private MyCheckboxConfig searchLinkedin;
	private MyCheckboxConfig dataCalonNggakValid;

	private Textbox nim;
	private Label ktp;
	private Textbox password;
	private MyCheckboxConfig aktif;

	private Textbox userOrtu;
	private Textbox passOrtu;
	private Textbox nama;
	private Label alamat;
	private Decimalbox tahunangkatan;
	private MyDatebox tanggalMasuk;
	private Textbox tempatlahir;
	private MyDatebox tanggallahir;
	private Combobox kelamin;
	private Combobox jenisSeleksi;
	private Combobox jenisPembiayaanMahasiswa;
	// private Label telp;
	protected AmbilDataKelasBanbox kelas;
	private Combobox fakultas;
	private Combobox jurusan;
	private Label status;
	private Combobox program;
	private AmbilDataNegaraBanbox negara;
	private Combobox semesterMulai;
	private Combobox konsentrasi;
	private Combobox waktuKuliah;
	private Textbox keterangan;

	private Decimalbox berat_badan;
	private Decimalbox tinggi_badan;
	private Textbox golongan_darah;

	private MyDatebox tanggalLulus;
	private Combobox statusAwalMahasiswa;

	private Decimalbox smtStatusAwal;
	private Combobox statusAwalMahasiswaSetelahSmtTertentu;
	private Decimalbox smtStatusAwalLagi;
	private Combobox statusAwalMahasiswaSetelahSmtTertentuLagi;

	private Combobox statusKeluar;
	private Textbox noIjazah1;
	private Textbox noIjazah2;
	private Textbox noAkta1;
	private Textbox noAkta2;
	private Intbox tahunWisuda;
	private Textbox judulSkripsi;
	private MyDatebox tanggalYudisium;
	private Combobox tahunLulus;
	private Combobox semesterLulus;

	private MyDatebox blnAwalBimbingan;
	private MyDatebox blnAkhirBimbingan;

	// private Combobox beasiswaMahasiswaMiskin;
	// private Combobox beasiswaBidikMisi;
	// private Combobox beasiswaLain;
	// private Textbox keteranganBeasiswa;

	private Mahasiswa mahasiswa;
	// private DosenPembimbingAkademik dosenPembimbingAkademik;
	private BiodataMahasiswaAction biodataMahasiswaAction = null;
	private MyToolbarbuttonConfig add;
	private Div filterLanjutOverlay;
	private Div containerBtnLanjut;

	private boolean edit = false;
	private boolean delete = false;

	private Tabpanel manajemenKelas;
	private Tabpanel manajemenAsrama;
	private Tabpanel manajemenPerkuliahanMahasiswa;
	private Tabpanel manajemenDosenPA;
	private Tabpanel kartuHasilStudi;
	// private Tabpanel kartuHasilStudiType1;

	private Tabpanel transkripAkademik;
	private Tabpanel prestasiBelajar;
	private Tabpanel manajemenFormTambahan;
	private Tabpanel manajemenKartuMahasiswa;
	// private Tabpanel transkripAkademikBeda;
	// private Tabpanel transkripAkademik4Kolom;
	// private Tabpanel rekamanNilai;
	// private Tabpanel rekamanNilaiPerProdi;
	// private Tabpanel rekamanNilaiKelompok;
	// private Tabpanel rekamanNilaiKelompokPerProdi;
	// private Tabpanel rekamanNilaiKelompokType1;
	// private Tabpanel rekamanNilai2Kolom;

	private Tabpanel dataMahasiswa;
	private Tabpanel manajemenKelompok;
	private Tabpanel manajemenKelompokStatus;
	private Tabpanel manajemenProgram;
	private Tabpanel manajemenKelompokStatusKeluar;
	private Tabpanel kegiatanMahasiswa;
	private Tabpanel operatorSeluler;
	private Tabpanel rekapJumlahMahasiswa;

	private Tabpanel alatTransport;
	private Tabpanel jenisTinggal;
	private Tabpanel pekerjaanOrtu;
	private Tabpanel penghasilanOrtu;
	private Tabpanel pendidikanOrtu;
	// private Tabpanel rekapJumlahMahasiswaAngkatan;
	// private Tabpanel rekapJumlahAlumni;
	// private Tabpanel rekapDataMahasiswa;
	private Tabpanel suratMahasiswa;
	// private Tabpanel organisasiMahasiswa;
	private Tabpanel kegiatanMahasiswaan;
	private Tabpanel albumMahasiswa;
	private MyToolbarbuttonConfig uploadPassword;
	private MyToolbarbuttonConfig uploadUKT;
	private MyToolbarbuttonConfig downloadPassword;
	private MyToolbarbuttonConfig uploadData;

	private MyToolbarbuttonConfig downloadRfid;
	private MyToolbarbuttonConfig uploadRfid;

	private MyToolbarbuttonConfig downloadFormatMahasiwa;
	private MyToolbarbuttonConfig synchronizeStatus;
	private MyTabConfig tabPindahan;
	private Textbox pindahanDariKampus;
	private Intbox sksYangDiakui;
	private Textbox namaProdiPindah;
	private Intbox sksYangDiakuiPindahProdi;
	private MyDatebox tanggalPindah;
	private Combobox pindahDariKampusLamaDiSemester;
	private Combobox pindahKeKampusIniMasukSemester;
	private Textbox keteranganPindah;
	private Textbox nimPindahan;

	private MyTabConfig tabAlihProdi;
	private MyDatebox tanggalPindahProdi;
	private Combobox pindahKeProdiIniMasukSemester;
	private Textbox keteranganPindahProdi;

	private Textbox feeder;
	private Textbox idRegPd;
	private Textbox lockId;

	protected Tabpanel statistik;

	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {
			DashboardMahasiswa include = new DashboardMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(include, statistik,
				"Statistik Mahasiswa", "Gambaran sebaran mahasiswa per prodi, angkatan, jenis kelamin, dan status aktif.");
		}
	}

	public static String[] contents = new String[] { "id", "nim", "nama", "namaArab", "namaTionghoa", "alamat", "telp",
			"email", "tahunangkatan", "tanggalMasuk", "tanggalKegiatanBelajarMengajar", "tempatlahir", "tanggallahir",
			"tanggallahirManual", "kelamin", "jurusan", "konsentrasi", "jenjang", "semesterMulai", "program", "agama",
			"warganegara", "negara", "statusAwalMahasiswa", "smtStatusAwal", "statusAwalMahasiswaSetelahSmtTertentu",
			"smtStatusAwalLagi", "statusAwalMahasiswaSetelahSmtTertentuLagi", "merupakanPindahan", "pindahanDariKampus",
			"namaProdiPindah", "nimPindahan", "pindahDariKampusLamaDiSemester", "pindahKeKampusIniMasukSemester",
			"tanggalPindah", "sksYangDiakui", "keteranganPindah", "merupakanAlihProdi", "nimLamaSebelumPindah",
			"sksYangDiakuiPindahProdi", "pindahKeProdiIniMasukSemester", "tanggalPindahProdi", "keteranganPindahProdi",
			"statusKeluar", "noIjazah1", "noIjazah2", "noAkta1", "noAkta2", "tahunWisuda", "tahunLulus",
			"semesterLulus", "tanggalLulus", "tanggalYudisium", "tanggalSkRektor", "judulSkripsi", "predikatKelulusan",
			"beasiswaMahasiswaMiskin", "beasiswaBidikMisi", "beasiswaLain", "facebookId", "googleId", "twitterId",
			"linkedinId", "jenisSeleksi", "jenisPembiayaanMahasiswa", "usernameOjs", "statusSetelahLulus",
			"jenisPembiayaanMahasiswa", "statusDomisiliSetelahLulus", "feeder", "idRegPd", "lockId", "token",
			"masaStudi", "linkValidasiEksternal", "nomorSkpi", "idfinger", "ubahPasword" };

	private AmbilDataMahasiswaBanbox alihProdiMahasiswa;
	private Combobox predikatKelulusan;
	private EventListener eventListener;
	private Row rowParameterTambahan;
	private ParameterTambahanAlumniListener parameterTambahanAlumniListener;
	private Tbmuser tbmuser;
	private PerguruanTinggi perguruanTinggi;
	private String kelaminData = null;

	@SuppressWarnings("unchecked")
	public void onDownloadLampiran(Event event) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Mahasiswa> calonMahasiswa = initCriteria(true).list();
				File fileFolderLampiran = new File(
						"/opt/ecampus/lampiran_" + ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis());
				fileFolderLampiran.mkdirs();
				System.out.println("fileFolderLampiran => " + fileFolderLampiran.getAbsolutePath());

				class FileDownloadHelper {
					public File download(String jenis, Mahasiswa mahasiswa, File fileFolderCalon) {
						File fileCopy = null;

						File folderOut = new File(Common.REAL_PATH + "/media/");
						try {
							folderOut.mkdirs();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/MahasiswaAction.java:444");
							// TODO: handle exception
						}

						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
						try {

							int jumlah = ((Number) streamingSession.createCriteria(LampiranLainMahasiswa.class)
									.setProjection(Projections.rowCount()).add(Restrictions.eq("jenis", jenis))
									.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1)
									.uniqueResult()).intValue();
							if (jumlah > 0) {
								LampiranLainMahasiswa lampiranLainMahasiswa = (LampiranLainMahasiswa) streamingSession
										.createCriteria(LampiranLainMahasiswa.class)
										.add(Restrictions.eq("mahasiswa", mahasiswa.getId()))
										.add(Restrictions.eq("jenis", jenis)).setMaxResults(1).uniqueResult();
								if (lampiranLainMahasiswa.getGdrive() != null) {
									fileCopy = new File(fileFolderCalon.getAbsolutePath() + "/" + jenis + "_"
											+ mahasiswa.getNim() + ".txt");
									ais.common.BacaTulisUtil.tulis(fileCopy, lampiranLainMahasiswa.forwardGDriveUrl());
								} else {

									File file;
									if (lampiranLainMahasiswa.getGdrive() != null
											&& !lampiranLainMahasiswa.getGdrive().trim().isEmpty()) {
										file = new File(folderOut.getAbsolutePath() + "/"
												+ URLEncoder.encode(lampiranLainMahasiswa.getNama(), "UTF-8") + ".txt");
										FileUtils.writeStringToFile(file, lampiranLainMahasiswa.forwardGDriveUrl());
									} else if (lampiranLainMahasiswa.getLink() != null
											&& !lampiranLainMahasiswa.getLink().trim().isEmpty()) {
										file = new File(folderOut.getAbsolutePath() + "/"
												+ URLEncoder.encode(lampiranLainMahasiswa.getNama(), "UTF-8") + ".txt");
										FileUtils.writeStringToFile(file, lampiranLainMahasiswa.getLink().trim());
									} else {
										file = lampiranLainMahasiswa.ambilFile();
									}

									fileCopy = new File(
											fileFolderCalon.getAbsolutePath() + "/" + jenis + "_" + file.getName());
									System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
									FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
									FileInputStream fileInputStream = new FileInputStream(file);
									IOUtils.copyLarge(fileInputStream, fileOutputStream);
									fileInputStream.close();
									fileOutputStream.close();
								}
							}

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}

						StreamingHibernateUtil.getInstance().closeSession();

						return fileCopy;
					}
				}

				FileDownloadHelper downloadHelper = new FileDownloadHelper();

				for (Mahasiswa mahasiswa : calonMahasiswa) {
					File fileFolderCalon = new File(fileFolderLampiran.getAbsolutePath() + "/"
							+ URLEncoder.encode(mahasiswa.getNim() + "_" + mahasiswa.getNama(), "UTF-8"));
					fileFolderCalon.mkdirs();
					System.out.println("fileFolderCalon => " + fileFolderCalon.getAbsolutePath());

					try {
						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

						FotoMahasiswa fotomahasiswa = (FotoMahasiswa) streamingSession
								.createCriteria(FotoMahasiswa.class)
								.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult();

						if (fotomahasiswa != null && fotomahasiswa.getGdrive() != null) {
							File fileCopy = new File(
									fileFolderCalon.getAbsolutePath() + "/FOTO_" + mahasiswa.getNim() + ".txt");
							ais.common.BacaTulisUtil.tulis(fileCopy, fotomahasiswa.forwardGDriveUrl());
						} else if (fotomahasiswa != null) {
							File fileFoto = fotomahasiswa.ambilFile();
							File fileCopy = new File(fileFolderCalon.getAbsolutePath() + "/FOTO_" + fileFoto.getName());
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
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/MahasiswaAction.java:535");
					}

					downloadHelper.download(LampiranLainMahasiswa.IJAZAH, mahasiswa, fileFolderCalon);
					downloadHelper.download(LampiranLainMahasiswa.TRANSKRIP_NILAI, mahasiswa, fileFolderCalon);
					downloadHelper.download(LampiranLainMahasiswa.KTP, mahasiswa, fileFolderCalon);
					downloadHelper.download(LampiranLainMahasiswa.AKTE, mahasiswa, fileFolderCalon);
					downloadHelper.download(LampiranLainMahasiswa.SURAT_PENUNJUKAN_PENGURUS_ORGANISASI, mahasiswa,
							fileFolderCalon);
					downloadHelper.download(LampiranLainMahasiswa.NPWP, mahasiswa, fileFolderCalon);
					downloadHelper.download(LampiranLainMahasiswa.KK, mahasiswa, fileFolderCalon);
					downloadHelper.download(LampiranLainMahasiswa.KTP_AYAH, mahasiswa, fileFolderCalon);
					downloadHelper.download(LampiranLainMahasiswa.KTP_IBU, mahasiswa, fileFolderCalon);
					downloadHelper.download(LampiranLainMahasiswa.KTP_WALI, mahasiswa, fileFolderCalon);
					downloadHelper.download(LampiranLainMahasiswa.LAMPIRAN_1, mahasiswa, fileFolderCalon);
					downloadHelper.download(LampiranLainMahasiswa.LAMPIRAN_2, mahasiswa, fileFolderCalon);
					downloadHelper.download(LampiranLainMahasiswa.LAMPIRAN_3, mahasiswa, fileFolderCalon);
					downloadHelper.download(LampiranLainMahasiswa.LAMPIRAN_4, mahasiswa, fileFolderCalon);
					downloadHelper.download(LampiranLainMahasiswa.LAMPIRAN_5, mahasiswa, fileFolderCalon);

					Session session = HibernateUtil.currentSession();
					String parameterTambahanInds = (String) session.createCriteria(BiodataMahasiswa.class)
							.addOrder(Order.desc("id")).add(Restrictions.eq("mahasiswa", mahasiswa))
							.setProjection(Projections.property("parameterTambahanInds")).setMaxResults(1)
							.uniqueResult();
					Long inds = (Long) session.createCriteria(BiodataMahasiswa.class).addOrder(Order.desc("id"))
							.add(Restrictions.eq("mahasiswa", mahasiswa)).setProjection(Projections.property("id"))
							.setMaxResults(1).uniqueResult();

					if (inds != null && parameterTambahanInds != null && !parameterTambahanInds.trim().isEmpty()) {
						String[] spl = parameterTambahanInds.split("\n");
						for (String d : spl) {
							String[] value = d.split("<=>");
							String jenis = value.length > 0 ? value[0].trim() : "";
							String val = value.length > 1 ? value[1].trim() : "";
							String url = value.length > 2 ? value[2].trim() : "";
							if (!url.trim().isEmpty()) {

								File fileCopy = null;

								LampiranLain lam = LampiranLain.ambil(inds, jenis);
								if (lam != null && lam.getGdrive() != null) {
									fileCopy = new File(fileFolderCalon.getAbsolutePath() + "/" + lam.getJenis() + "_"
											+ mahasiswa.getNim() + ".txt");
									ais.common.BacaTulisUtil.tulis(fileCopy, lam.forwardGDriveUrl());
								} else if (lam != null) {
									File file = lam.ambilFile();
									fileCopy = new File(fileFolderCalon.getAbsolutePath() + "/"
											+ URLEncoder.encode(val, "UTF-8") + "_" + file.getName());
									System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
									FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
									FileInputStream fileInputStream = new FileInputStream(file);
									IOUtils.copyLarge(fileInputStream, fileOutputStream);
									fileInputStream.close();
									fileOutputStream.close();
								}

							}
						}
					}
				}

				File fileFolderLampiranZip = new File(fileFolderLampiran.getAbsolutePath() + ".zip");
				Common.zipDir(fileFolderLampiranZip.getAbsolutePath(), fileFolderLampiran.getAbsolutePath());
				Filedownload.save(fileFolderLampiranZip, "application/zip");

			}
		}, "Harap tunggu.. sedang melakukan proses download lampiran..");

	}

	public void onDownloadFoto(Event event) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Mahasiswa> calonMahasiswa = ConstantValues.simpleList(initCriteria(true), Mahasiswa.class);
				File fileFolderLampiran = new File(Common.ambilREAL_PATH_REPORT() + "/foto_"
						+ ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis());
				fileFolderLampiran.mkdirs();
				System.out.println("fileFolderLampiran => " + fileFolderLampiran.getAbsolutePath());

				for (Mahasiswa mahasiswa : calonMahasiswa) {

					try {

						FileFotoLain fotomahasiswa = FileFotoLain.ambil(mahasiswa.getId(), FotoMahasiswa.DEFAULT_JENIS,
								FotoMahasiswa.class);

						if (fotomahasiswa != null && fotomahasiswa.getGdrive() != null) {
							File fileCopy = new File(fileFolderLampiran.getAbsolutePath() + "/" + mahasiswa.getNim()
									+ "_" + mahasiswa.getNama() + ".txt");
							ais.common.BacaTulisUtil.tulis(fileCopy, fotomahasiswa.forwardGDriveUrl());
						} else if (fotomahasiswa != null) {
							File fileFoto = fotomahasiswa.ambilFile();
							String jpg = "jpg";
							if (fileFoto.getName().toLowerCase().endsWith("png")) {
								jpg = "png";
							} else if (fileFoto.getName().toLowerCase().endsWith("jpeg")) {
								jpg = "jpeg";
							} else if (fileFoto.getName().toLowerCase().endsWith("gif")) {
								jpg = "gif";
							} else if (fileFoto.getName().toLowerCase().endsWith("tiff")) {
								jpg = "tiff";
							}

							File fileCopy = new File(fileFolderLampiran.getAbsolutePath() + "/" + mahasiswa.getNim()
									+ "_" + mahasiswa.getNama() + "." + jpg);
							System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
							FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
							FileInputStream fileInputStream = new FileInputStream(fileFoto);
							IOUtils.copyLarge(fileInputStream, fileOutputStream);
							fileInputStream.close();
							fileOutputStream.close();
						}

					} catch (Exception e1) {
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/MahasiswaAction.java:653");
					}

				}

				File fileFolderLampiranZip = new File(fileFolderLampiran.getAbsolutePath() + ".zip");
				Common.zipDir(fileFolderLampiranZip.getAbsolutePath(), fileFolderLampiran.getAbsolutePath());
				Filedownload.save(fileFolderLampiranZip, "application/zip");

			}
		}, "Harap tunggu.. sedang melakukan proses download foto..");

	}

	/**
	 * Unggah FOTO MASSAL mahasiswa: admin memilih banyak berkas foto sekaligus yang NAMA BERKAS-nya
	 * adalah NIM (mis. {@code 12345.jpg}, {@code 45666.png}). Setiap foto langsung dipasang sebagai
	 * foto profil mahasiswa yang NIM-nya cocok, lalu ringkasan hasil (berhasil / NIM tak ditemukan /
	 * gagal) ditampilkan. Dipicu tombol toolbar "Upload Foto (NIM)" (upload multiple).
	 */
	/** Unduh SEMUA foto mahasiswa sebagai satu ZIP (baca BLOB paralel maks 50 thread). Tombol "Download Foto Massal". */
	public void onDownloadFotoMassal(Event event) throws Exception {
		ais.common.helper.DownloadFotoMassalHelper.downloadFotoMahasiswaMassal();
	}

	public void onUploadFotoMassal(Event event) throws Exception {
		if (!ais.common.helper.UploadFotoMassalHelper.bolehUploadMassal()) {
			ais.ui.util.MyMessageboxConfig.show(
					"Mohon maaf, Bapak/Ibu belum memiliki hak akses (baca, ubah, dan hapus) yang diperlukan untuk mengunggah foto mahasiswa. Langkah yang dapat dilakukan: (1) pastikan Bapak/Ibu masuk menggunakan akun yang berwenang; (2) hubungi administrator sistem untuk memohon penambahan hak akses pada menu ini; (3) setelah hak akses diberikan, silakan mencoba kembali proses unggah foto.",
					"Akses Ditolak", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.EXCLAMATION);
			return;
		}
		ForwardEvent forwardEvent = (ForwardEvent) event;
		UploadEvent uploadEvent = (UploadEvent) forwardEvent.getOrigin();
		Media[] medias = uploadEvent.getMedias();
		java.util.List<Media> daftar = new java.util.ArrayList<Media>();
		if (medias != null) {
			for (Media m : medias) {
				if (m != null) {
					daftar.add(m);
				}
			}
		} else if (uploadEvent.getMedia() != null) {
			daftar.add(uploadEvent.getMedia());
		}
		if (daftar.isEmpty()) {
			MyMessageboxConfig.show(
					"Mohon maaf, belum ada berkas foto yang dipilih untuk diunggah. Langkah yang dapat dilakukan: (1) klik tombol unggah lalu pilih satu atau beberapa berkas foto mahasiswa; (2) pastikan nama berkas sesuai dengan NIM mahasiswa yang bersangkutan; (3) setelah berkas terpilih, silakan lanjutkan proses unggah.",
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		/*
		 * Laporan per BERKAS: sebelumnya hanya angka ringkasan (berhasil/tak ditemukan/gagal)
		 * sehingga pengguna tidak tahu foto MANA yang tidak masuk maupun sebabnya. Kini tiap
		 * berkas dicatat, rinciannya otomatis terunduh sebagai berkas teks, dan kotak pesan
		 * menampilkan jumlah berhasil/gagal/dilewati.
		 */
		ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Upload Foto Mahasiswa");
		StringBuilder sumber = new StringBuilder();
		for (int i = 0; i < daftar.size(); i++) {
			if (i > 0) {
				sumber.append(", ");
			}
			sumber.append(daftar.get(i).getName());
		}
		laporan.setNamaBerkasSumber(sumber.toString());

		ais.common.helper.UploadFotoMassalHelper.uploadFotoMahasiswaByNim(daftar, laporan);
		laporan.selesaikan(null);
	}

	public void onUploadPassword(Event event) throws Exception {

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

			ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Password Mahasiswa");
			for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
				Session session = HibernateUtil.currentNativeSession();
				try {

					String nim = Common.getCellContent(Common.getCell(sheet, 0, i));
					String password = Common.getCellContent(Common.getCell(sheet, 1, i));

					if (nim == null) {
						continue;
					}

					Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.simpleObject(
							session.createCriteria(Mahasiswa.class).add(Restrictions.eq("nim", nim)).setMaxResults(1),
							Mahasiswa.class);
					System.out.println("nim = " + nim + ", mahasiswa = " + mahasiswa); // KEAMANAN: password TIDAK dicetak ke log
					if (mahasiswa != null) {
						mahasiswa.setPass(Common.desEncrypter.get().encrypt(password));
						session.getTransaction().begin();
						session.update(mahasiswa);
						session.getTransaction().commit();
						report.sukses(i, nim, "Password diperbarui");
					} else {
						report.gagal(i, nim, "Mahasiswa tidak ditemukan", "Periksa NIM pada berkas Excel.");
					}

				} catch (Exception e) {
					session.getTransaction().rollback();
					Common.tampilErrorJikaAdmin(e);
					// Common.tampilErrorJikaAdmin(e);
					report.gagal(i, "baris-" + i, e, "Periksa NIM dan format berkas.");
				}

				HibernateUtil.closeSession();
			}

			try { Filedownload.save(report.simpanLaporan(), "text/plain"); } catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) MahasiswaAction laporan"); }
			MyMessageboxConfig.show(
					"Pembaruan kata sandi telah berhasil dilakukan. " + report.getRingkasan(),
					"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

		} else {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, berkas yang Bapak/Ibu unggah (\"{V1}\") harus berformat Excel Open XML Spreadsheet (xlsx). Langkah yang dapat dilakukan: (1) buka berkas Excel tersebut melalui aplikasi Microsoft Excel; (2) pilih menu Save As, kemudian pilih jenis berkas Excel Open XML Spreadsheet (xlsx); (3) simpan berkas dan silakan unggah kembali berkas dengan format tersebut.",
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR, media);
		}
	}

	public void onUploadUKT(Event event) throws Exception {

		ForwardEvent forwardEvent = (ForwardEvent) event;
		final Media media = ((UploadEvent) forwardEvent.getOrigin()).getMedia();
		if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
			return;
		if (media.getName().toLowerCase().endsWith("xlsx")) {

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
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

					ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload UKT Mahasiswa");
					for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
						Session session = HibernateUtil.currentNativeSession();
						try {

							Mahasiswa mahasiswa = (Mahasiswa) Common.getSheetContentAsObject(sheet, 0, i,
									Mahasiswa.class);
							StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) Common
									.getSheetContentAsObject(sheet, 2, i, StatusAwalMahasiswa.class);

							if (mahasiswa == null || statusAwalMahasiswa == null) {
								continue;
							}

							System.out.println(
									" mahasiswa = " + mahasiswa + ", statusAwalMahasiswa = " + statusAwalMahasiswa);
							if (mahasiswa != null) {
								mahasiswa.setStatusAwalMahasiswa(statusAwalMahasiswa);
								session.getTransaction().begin();
								session.update(mahasiswa);
								session.getTransaction().commit();
								report.sukses(i, mahasiswa.getNim() + " - " + mahasiswa.getNama(), "UKT diperbarui");
							}

						} catch (Exception e) {
							session.getTransaction().rollback();
							Common.tampilErrorJikaAdmin(e);
							// Common.tampilErrorJikaAdmin(e);
							report.gagal(i, "baris-" + i, e, "Periksa data UKT pada baris ini.");
						}

						HibernateUtil.closeSession();
					}

					try { Filedownload.save(report.simpanLaporan(), "text/plain"); } catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) MahasiswaAction laporan"); }
					MyMessageboxConfig.show(
							"Pembaruan data UKT (Uang Kuliah Tunggal) telah berhasil dilakukan. " + report.getRingkasan(),
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				}
			});

		} else {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, berkas yang Bapak/Ibu unggah (\"{V1}\") harus berformat Excel Open XML Spreadsheet (xlsx). Langkah yang dapat dilakukan: (1) buka berkas Excel tersebut melalui aplikasi Microsoft Excel; (2) pilih menu Save As, kemudian pilih jenis berkas Excel Open XML Spreadsheet (xlsx); (3) simpan berkas dan silakan unggah kembali berkas dengan format tersebut.",
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR, media);
		}
	}

	public void onUploadStatus(UploadEvent event) throws Exception {

		final Media media = event.getMedia();
		if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
			return;
		if (media.getName().toLowerCase().endsWith("xlsx")) {

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
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

					ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Status Mahasiswa");
					for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
						Session session = HibernateUtil.currentNativeSession();
						try {

							Mahasiswa mahasiswa = (Mahasiswa) Common.getSheetContentAsObject(sheet, 1, i,
									Mahasiswa.class);
							StatusMahasiswa statusMahasiswa = (StatusMahasiswa) Common.getSheetContentAsObject(sheet, 4,
									i, StatusMahasiswa.class);
							String tahunAkademik = Common.getSheetContentAsString(sheet, 5, i);
							String ganjilGenap = Common.getSheetContentAsString(sheet, 6, i);

							Integer smt = Common.getSheetContentAsInteger(sheet, 7, i);
							if (mahasiswa != null && ganjilGenap != null && !ganjilGenap.trim().isEmpty()
									&& tahunAkademik != null) {
								Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
								String ta = tahunAkademik;
								Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
								smt = Common.getSemester(tahunAngkatanMhs, ganjilGenap,
										mahasiswa.getPindahKeKampusIniMasukSemester(), tahun,
										mahasiswa.getSemesterMulai());
							}

							Date tanggalStatus = Common.getSheetContentAsDate(sheet, 8, i);
							String keterangan = Common.getSheetContentAsString(sheet, 10, i);

							StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) Common
									.getSheetContentAsObject(sheet, 9, i, StatusAwalMahasiswa.class);

							System.out.println(" mahasiswa = " + mahasiswa + ", statusMahasiswa = " + statusMahasiswa
									+ " StatusAwalMahasiswa = " + statusAwalMahasiswa + " smt " + smt
									+ " tahunAkademik " + tahunAkademik);

							if (mahasiswa == null || statusMahasiswa == null || tahunAkademik == null
									|| tahunAkademik.trim().isEmpty() || smt == null) {
								continue;
							}
							KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, smt, null, null);
							HistoryStatusMahasiswa historyStatusMahasiswa = HistoryStatusMahasiswaUtil
									.getHistoryStatusMahasiswa(krsMahasiswa, true);

							historyStatusMahasiswa.setStatusMahasiswa(statusMahasiswa);
							historyStatusMahasiswa.setTanggalStatus(tanggalStatus);
							historyStatusMahasiswa.setStatusAwalMahasiswa(statusAwalMahasiswa);
							historyStatusMahasiswa.setKeterangan(keterangan);

							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, historyStatusMahasiswa);
							session.getTransaction().commit();
							report.sukses(i, mahasiswa.getNim(), "Status mahasiswa diperbarui");

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							report.gagal(i, "baris-" + i, e, "Periksa data status pada baris ini.");
						}

						HibernateUtil.closeSession();
					}

					try { Filedownload.save(report.simpanLaporan(), "text/plain"); } catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) MahasiswaAction laporan"); }
					MyMessageboxConfig.show(
							"Pembaruan status mahasiswa telah berhasil dilakukan. " + report.getRingkasan(),
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				}
			});

		} else {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, berkas yang Bapak/Ibu unggah (\"{V1}\") harus berformat Excel Open XML Spreadsheet (xlsx). Langkah yang dapat dilakukan: (1) buka berkas Excel tersebut melalui aplikasi Microsoft Excel; (2) pilih menu Save As, kemudian pilih jenis berkas Excel Open XML Spreadsheet (xlsx); (3) simpan berkas dan silakan unggah kembali berkas dengan format tersebut.",
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR, media);
		}
	}

	@SuppressWarnings("unchecked")
	public void onDownloadPassword(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				ProjectionList projectionList = Projections.projectionList();
				projectionList.add(Projections.property("nim"));
				projectionList.add(Projections.property("pass"));

				List<Object[]> objects = initCriteria(true).setProjection(projectionList).list();

				final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/password_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

				File file;
				(file = new File(filename)).createNewFile();

				XSSFWorkbook workbook = new XSSFWorkbook();
				XSSFSheet sheet = workbook.createSheet("DATA");
				sheet.setDefaultColumnWidth(20);
				int rowIndex = 0;

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("NIM");
				rowhead.createCell(1).setCellValue("PASSWORD");
				rowIndex = 1;
				for (Object[] mahasiswa : objects) {
					try {
						XSSFRow row = sheet.createRow(rowIndex);
						row.createCell(0).setCellValue(mahasiswa[0] + "");
						row.createCell(1).setCellValue(Common.desEncrypter.get().decrypt(mahasiswa[1] + ""));
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

			ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload RFID Mahasiswa");
			for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
				Session session = HibernateUtil.currentNativeSession();
				try {

					String nim = Common.getCellContent(Common.getCell(sheet, 0, i));
					String rfid = Common.getCellContent(Common.getCell(sheet, 1, i));

					if (nim == null || nim.trim().isEmpty()) {
						continue;
					}

					Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.simpleObject(
							session.createCriteria(Mahasiswa.class).add(Restrictions.eq("nim", nim)).setMaxResults(1),
							Mahasiswa.class);
					System.out.println("nim = " + nim + " rfid = " + rfid + ", mahasiswa = " + mahasiswa);
					if (mahasiswa != null) {
						mahasiswa.setIdfinger(rfid);
						session.getTransaction().begin();
						session.update(mahasiswa);
						session.getTransaction().commit();
						report.sukses(i, nim + " / " + rfid, "RFID diperbarui");
					} else {
						report.gagal(i, nim, "Mahasiswa tidak ditemukan", "Periksa NIM pada berkas Excel.");
					}

				} catch (Exception e) {
					session.getTransaction().rollback();
					Common.tampilErrorJikaAdmin(e);
					// Common.tampilErrorJikaAdmin(e);
					report.gagal(i, "baris-" + i, e, "Periksa NIM dan format berkas.");
				}

				HibernateUtil.closeSession();
			}

			try { Filedownload.save(report.simpanLaporan(), "text/plain"); } catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) MahasiswaAction laporan"); }
			MyMessageboxConfig.show(
					"Pembaruan data RFID telah berhasil dilakukan. " + report.getRingkasan(),
					"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

		} else {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, berkas yang Bapak/Ibu unggah (\"{V1}\") harus berformat Excel Open XML Spreadsheet (xlsx). Langkah yang dapat dilakukan: (1) buka berkas Excel tersebut melalui aplikasi Microsoft Excel; (2) pilih menu Save As, kemudian pilih jenis berkas Excel Open XML Spreadsheet (xlsx); (3) simpan berkas dan silakan unggah kembali berkas dengan format tersebut.",
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR, media);
		}
	}

	@SuppressWarnings("unchecked")
	public void onDownloadRfid(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				ProjectionList projectionList = Projections.projectionList();
				projectionList.add(Projections.property("nim"));
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

				rowhead.createCell(0).setCellValue("NIM");
				rowhead.createCell(1).setCellValue("ID Finger/RFID");
				rowhead.createCell(2).setCellValue("Nama");
				rowIndex = 1;
				for (Object[] mahasiswa : objects) {
					try {
						XSSFRow row = sheet.createRow(rowIndex);
						row.createCell(0).setCellValue(mahasiswa[0] + "");
						row.createCell(1).setCellValue(mahasiswa[1] + "");
						row.createCell(2).setCellValue(mahasiswa[2] + "");
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

	public void onSynchronizeStatus(Event event) throws Exception {

		final MyWindow window = new MyWindow("Pilih Tahun Akademik dan Semester", "none", true);
		window.setParent(page.getFirstRoot());
		window.setHeight("95%");
		window.setWidth("600px");
		final Combobox tahunAkademik = new Combobox();
		Common.generateTahunAjaran(tahunAkademik);
		final Combobox genapGanjil = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		genapGanjil.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		genapGanjil.appendChild(comboitem);

		// SEMESTER PENDEK: buat/segarkan KRS SP + status ber-flag sp sehingga mahasiswa
		// dapat mengambil KRS Semester Antara meskipun status semester regulernya belum
		// aktif (status SP disimpan terpisah dari status reguler).
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.SP);
		comboitem.setValue(Perkuliahan.SP);
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

		Common.selectComboItem(genapGanjil, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan Mulai *"));
		final Intbox mulai;
		row.appendChild(mulai = new Intbox(tahun - 4));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan Sampai *"));
		final Intbox sampai;
		row.appendChild(sampai = new Intbox(tahun));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final MyCheckboxConfig statusMhs;
		row.appendChild(statusMhs = new MyCheckboxConfig("Status Mahasiswa"));
		statusMhs.setChecked(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final MyCheckboxConfig statusKrs;
		row.appendChild(statusKrs = new MyCheckboxConfig("KRS/KHS Mahasiswa"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final MyCheckboxConfig perkuliahanMhs;
		row.appendChild(perkuliahanMhs = new MyCheckboxConfig("Perkuliahan Mahasiswa"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final MyCheckboxConfig pembayaranMhs;
		row.appendChild(pembayaranMhs = new MyCheckboxConfig("Pembayaran Mahasiswa"));

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("tampilkan_yang_belum_telah_ambil_krs"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final MyCheckboxConfig nonAktifkan;
		row.appendChild(nonAktifkan = new MyCheckboxConfig("Non aktifkan/aktifkan yang belum/telah ambil KRS"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final MyCheckboxConfig temporaray;
		row.appendChild(temporaray = new MyCheckboxConfig("Reload data temporary"));

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
				window.detach();

				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses singkronisasi status mahasiswa"));
				final java.util.concurrent.atomic.AtomicReference<ais.common.LaporanUpload> laporanRef = new java.util.concurrent.atomic.AtomicReference<ais.common.LaporanUpload>();

				new Thread(new Runnable() {

					@Override
					public void run() {

						if (temporaray.isChecked()) {
							ConstantValues.reInitDataDiMemory(true);
						}

						laporanRef.set(Common.singkronisasiStatusMahasiswa(label, MahasiswaAction.this,
								tahunAkademik.getSelectedItem() == null
										|| tahunAkademik.getSelectedItem().getValue() == null ? null
												: tahunAkademik.getSelectedItem().getValue().toString(),
								genapGanjil.getSelectedItem() == null
										|| genapGanjil.getSelectedItem().getValue() == null ? null
												: genapGanjil.getSelectedItem().getValue().toString(),
								mulai.getValue() == null ? 0 : mulai.getValue(),
								sampai.getValue() == null ? 0 : sampai.getValue(), statusMhs.isChecked(),
								statusKrs.isChecked(), perkuliahanMhs.isChecked(), pembayaranMhs.isChecked(),
								nonAktifkan.isChecked()));
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
							timer.detach();
							ais.common.LaporanUpload laporan = laporanRef.get();
							if (laporan != null) {
								laporan.selesaikan(null);
							} else {
								MyMessageboxConfig.show(
										"Sinkronisasi status mahasiswa telah berhasil dilakukan. Terima kasih, Bapak/Ibu, seluruh data status mahasiswa telah diselaraskan dengan baik.",
										"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
						}

					}
				});
				timer.start();
			}
		});
		save.setParent(toolbar);

		final String[] contents = new String[] { "id", "mahasiswa.nim", "mahasiswa.nama", "mahasiswa.jurusan.nama",
				"statusMahasiswa.nama", "tahunAkademik", "ganjilGenap", "semester", "tanggalStatus",
				"statusAwalMahasiswa", "keterangan" };
		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("Info KRS");
		columnHeadersAdding.add("Info KRS SP");

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				HistoryStatusMahasiswa historyStatusMahasiswa = (HistoryStatusMahasiswa) (objects[0]);

				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa,
						historyStatusMahasiswa.getSemester(), historyStatusMahasiswa.getTahap(), null);

				String d = KrsDetailHelper.rubahKeteranganPengambilanKRSBersih(mahasiswa,
						historyStatusMahasiswa.getSemester(), historyStatusMahasiswa.getTahap(), null, krsMahasiswa,
						false);

				krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, historyStatusMahasiswa.getSemester(),
						historyStatusMahasiswa.getTahap(), Perkuliahan.SEMESTER_PENDEK);
				String dSp = KrsDetailHelper.rubahKeteranganPengambilanKRSBersih(mahasiswa,
						historyStatusMahasiswa.getSemester(), historyStatusMahasiswa.getTahap(),
						Perkuliahan.SEMESTER_PENDEK, krsMahasiswa, false);

				XSSFRow row = (XSSFRow) objects[2];

				if (row != null) {
					row.createCell(contents.length).setCellValue(d);
					row.createCell(contents.length + 1).setCellValue(dSp);
				}
			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(HistoryStatusMahasiswa.class,
				new DataCriteria() {

					@Override
					public Criteria initCriteria(boolean order) {

						String jenisSmtStatus = (String) genapGanjil.getSelectedItem().getValue();

						Session session = HibernateUtil.currentSession();
						return session.createCriteria(HistoryStatusMahasiswa.class)
								.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
								.createAlias("jurusan.fakultas", "fakultas")
								.add(perguruanTinggi == null || perguruanTinggi.getId() == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi))
								.add(Restrictions.between("mahasiswa.tahunangkatan", mulai.getValue(),
										sampai.getValue()))
								.add(Restrictions.eq("tahunAkademik", tahunAkademik.getSelectedItem().getValue()))
								.add(Restrictions.eq("ganjilGenap", jenisSmtStatus));
					}
				}, "Download Status Mahasiswa", "/img/print.png", columnHeadersAdding, dataAdding, false, null, "",
				contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload Status Mahasiswa", "/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onUploadStatus((UploadEvent) arg0);
			}
		});
		upload.setVisible((add != null && add.isVisible()) && edit && delete);
		toolbar.appendChild(upload);

		window.onModal();

	}

	public void onSuratMahasiswa(Event event) {

		if (suratMahasiswa.getChildren().size() == 0) {
			LaporanSurat laporan = new LaporanSurat();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(suratMahasiswa);
		}
	}

	public void onAlbumMahasiswa(Event event) {

		if (albumMahasiswa.getChildren().size() == 0) {
			LaporanAlbumMahasiswaPerProdiDanAngkatan laporan = new LaporanAlbumMahasiswaPerProdiDanAngkatan();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(albumMahasiswa);
		}
	}

	public void onRekapJumlahMahasiswa(Event event) {

		if (rekapJumlahMahasiswa.getChildren().size() == 0) {
			LaporanRekapJumlahMahasiswa laporan = new LaporanRekapJumlahMahasiswa();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(rekapJumlahMahasiswa);
		}
	}

	public void onDataMahasiswa(Event event) {

		if (dataMahasiswa.getChildren().size() == 0) {

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(dataMahasiswa);
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			// MyTabConfig tab0 = new MyTabConfig("Proses Nilai Mahasiswa");
			// tab0.setParent(tabs);

			MyTabConfig tab1 = new MyTabConfig("Format Data EMIS");
			tab1.setParent(tabs);

			MyTabConfig tab2 = new MyTabConfig("Format Data EPSBED");
			tab2.setParent(tabs);

			MyTabConfig tab3 = new MyTabConfig("Format Data PTKKN");
			tab3.setParent(tabs);

			// MyTabConfig tab2 = new MyTabConfig("Rekap Jumlah Mahasiswa per
			// Angkatan");
			// tab2.setParent(tabs);
			//
			// MyTabConfig tab3 = new MyTabConfig("Data Mahasiswa");
			// tab3.setParent(tabs);
			//
			// MyTabConfig tab4 = new MyTabConfig("Data Alumni");
			// tab4.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			// Tabpanel tabpanel0 = new ais.ui.util.MyTabpanel();
			// tabpanel0.setParent(tabpanels);

			// Iframe iframe = new Iframe();
			// iframe.setHeight("100%");
			// iframe.setWidth("100%");
			// iframe.setParent(tabpanel0);
			// iframe.setSrc("/pages/master/epsbed/update_data_mahasiswa.zul");

			Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
			tabpanel1.setParent(tabpanels);

			Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
			tabpanel2.setParent(tabpanels);

			Tabpanel tabpanel3 = new ais.ui.util.MyTabpanel();
			tabpanel3.setParent(tabpanels);

			LaporanFormatEMIS laporan = new LaporanFormatEMIS();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(tabpanel1);

			Iframe iframe = new Iframe();
			iframe.setHeight("100%");
			iframe.setWidth("100%");
			iframe.setParent(tabpanel2);
			iframe.setSrc("/pages/master/epsbed/master_mahasiswa.zul");

			LaporanFormatPTKKN laporan1 = new LaporanFormatPTKKN();
			laporan1.setHeight("100%");
			laporan1.setWidth("100%");
			laporan1.setParent(tabpanel3);
		}
	}

	// public void onRekapJumlahMahasiswaAngkatan(Event event) {
	//
	// if (rekapJumlahMahasiswaAngkatan.getChildren().size() == 0) {
	// LaporanRekapJumlahMahasiswaAngkatan laporan = new
	// LaporanRekapJumlahMahasiswaAngkatan();
	// laporan.setHeight("100%");
	// laporan.setWidth("100%");
	// laporan.setParent(rekapJumlahMahasiswaAngkatan);
	// }
	// }

	// public void onRekapJumlahAlumni(Event event) {
	//
	// if (rekapJumlahAlumni.getChildren().size() == 0) {
	// LaporanRekapitulasiAlumniJurusan laporan = new
	// LaporanRekapitulasiAlumniJurusan();
	// laporan.setHeight("100%");
	// laporan.setWidth("100%");
	// laporan.setParent(rekapJumlahAlumni);
	// }
	// }

	// public void onRekapDataMahasiswa(Event event) {
	//
	// if (rekapDataMahasiswa.getChildren().size() == 0) {
	// LaporanRekapitulasiMahasiswa laporan = new
	// LaporanRekapitulasiMahasiswa();
	// laporan.setHeight("100%");
	// laporan.setWidth("100%");
	// laporan.setParent(rekapDataMahasiswa);
	// }
	// }

	// private Tabpanel kartuHasilStudiPerProdi;
	//
	// public void onTampilKHSPerProdi(Event event) {
	//
	// if (kartuHasilStudiPerProdi.getChildren().size() == 0) {
	// LaporanKHSPerProdiDanAngkatan laporanKHS = new
	// LaporanKHSPerProdiDanAngkatan();
	// laporanKHS.setHeight("100%");
	// laporanKHS.setWidth("100%");
	// laporanKHS.setParent(kartuHasilStudiPerProdi);
	// }
	// }

	public void onTampilKHS(Event event) {

		if (kartuHasilStudi.getChildren().size() == 0) {
			LaporanKHS laporanKHS = new LaporanKHS();
			laporanKHS.setHeight("100%");
			laporanKHS.setWidth("100%");
			laporanKHS.setParent(kartuHasilStudi);
		}
	}

	// public void onTampilKHSType1(Event event) {
	//
	// if (kartuHasilStudiType1.getChildren().size() == 0) {
	// LaporanKHSType1 laporanKHS = new LaporanKHSType1();
	// laporanKHS.setHeight("100%");
	// laporanKHS.setWidth("100%");
	// laporanKHS.setParent(kartuHasilStudiType1);
	// }
	// }

	public void onTampilTranskripAkademik(Event event) {

		if (transkripAkademik.getChildren().size() == 0) {
			LaporanTranskipAkademik laporanTranskipAkademik = new LaporanTranskipAkademik();
			laporanTranskipAkademik.setHeight("100%");
			laporanTranskipAkademik.setWidth("100%");
			laporanTranskipAkademik.setParent(transkripAkademik);
		}
	}

	public void onTampilPrestasi(Event event) {

		if (prestasiBelajar.getChildren().size() == 0) {
			LaporanDaftarPrestasiBelajarWindow laporanTranskipAkademik = new LaporanDaftarPrestasiBelajarWindow();
			laporanTranskipAkademik.setHeight("100%");
			laporanTranskipAkademik.setWidth("100%");
			laporanTranskipAkademik.setParent(prestasiBelajar);
		}
	}

	// public void onTampilTranskripAkademikBeda(Event event) {
	//
	// if (transkripAkademikBeda.getChildren().size() == 0) {
	// LaporanTranskipAkademikBeda laporanTranskipAkademikBeda = new
	// LaporanTranskipAkademikBeda();
	// laporanTranskipAkademikBeda.setHeight("100%");
	// laporanTranskipAkademikBeda.setWidth("100%");
	// laporanTranskipAkademikBeda.setParent(transkripAkademikBeda);
	// }
	// }

	// public void onTampilTranskrip_Akademik(Event event) {
	//
	// if (transkripAkademik4Kolom.getChildren().size() == 0) {
	// LaporanTranskipAkademik4Kolom laporanTranskipAkademik4Kolom = new
	// LaporanTranskipAkademik4Kolom();
	// laporanTranskipAkademik4Kolom.setHeight("100%");
	// laporanTranskipAkademik4Kolom.setWidth("100%");
	// laporanTranskipAkademik4Kolom.setParent(transkripAkademik4Kolom);
	// }
	// }

	// public void onTampilRekamanNilai(Event event) {
	//
	// if (rekamanNilai.getChildren().size() == 0) {
	// LaporanRekamanNilai laporanRekamanNilai = new LaporanRekamanNilai();
	// laporanRekamanNilai.setHeight("100%");
	// laporanRekamanNilai.setWidth("100%");
	// laporanRekamanNilai.setParent(rekamanNilai);
	// }
	// }

	// public void onTampilRekamanNilaiKelompok(Event event) {
	//
	// if (rekamanNilaiKelompok.getChildren().size() == 0) {
	// LaporanRekamanNilaiKelompok laporanRekamanNilai = new
	// LaporanRekamanNilaiKelompok();
	// laporanRekamanNilai.setHeight("100%");
	// laporanRekamanNilai.setWidth("100%");
	// laporanRekamanNilai.setParent(rekamanNilaiKelompok);
	// }
	// }

	// public void onTampilRekamanNilaiPerProdidanAngkatan(Event event) {
	//
	// if (rekamanNilaiPerProdi.getChildren().size() == 0) {
	// LaporanRekamanNilaiPerProdiDanAngkatan laporanRekamanNilai = new
	// LaporanRekamanNilaiPerProdiDanAngkatan();
	// laporanRekamanNilai.setHeight("100%");
	// laporanRekamanNilai.setWidth("100%");
	// laporanRekamanNilai.setParent(rekamanNilaiPerProdi);
	// }
	// }

	// public void onTampilRekamanNilaiKelompokPerProdidanAngkatan(Event event)
	// {
	//
	// if (rekamanNilaiKelompokPerProdi.getChildren().size() == 0) {
	// LaporanRekamanNilaiKelompokPerProdiDanAngkatan laporanRekamanNilai = new
	// LaporanRekamanNilaiKelompokPerProdiDanAngkatan();
	// laporanRekamanNilai.setHeight("100%");
	// laporanRekamanNilai.setWidth("100%");
	// laporanRekamanNilai.setParent(rekamanNilaiKelompokPerProdi);
	// }
	// }

	// public void onTampilRekamanNilaiKelompokType1(Event event) {
	//
	// if (rekamanNilaiKelompokType1.getChildren().size() == 0) {
	// LaporanRekamanNilaiKelompokType1 laporanRekamanNilai = new
	// LaporanRekamanNilaiKelompokType1();
	// laporanRekamanNilai.setHeight("100%");
	// laporanRekamanNilai.setWidth("100%");
	// laporanRekamanNilai.setParent(rekamanNilaiKelompokType1);
	// }
	// }

	// public void onTampilRekamanNilai2Kolom(Event event) {
	//
	// if (rekamanNilai2Kolom.getChildren().size() == 0) {
	// LaporanRekamanNilai2Kolom laporanRekamanNilai = new
	// LaporanRekamanNilai2Kolom();
	// laporanRekamanNilai.setHeight("100%");
	// laporanRekamanNilai.setWidth("100%");
	// laporanRekamanNilai.setParent(rekamanNilai2Kolom);
	// }
	// }

	public void onKartuMahasiswa(Event event) {
		if (manajemenKartuMahasiswa.getChildren().size() == 0) {
			LaporanKartuMahasiswa laporan = new LaporanKartuMahasiswa();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(manajemenKartuMahasiswa);
		}
	}

	// public void onOrganisasiMahasiswa(Event event) {
	// if (organisasiMahasiswa.getChildren().size() == 0) {
	// MyWindow window = new MyWindow("", "none", false);
	// window.setHeight("100%");
	// window.setWidth("100%");
	// window.setParent(organisasiMahasiswa);
	// MyInclude iframe = new
	// MyInclude("/pages/master/organisasi_intra_kampus.zul");
	// iframe.setParent(window);
	// }
	// }

	public void onKegiatanKemahasiswaan(Event event) {
		if (kegiatanMahasiswaan.getChildren().size() == 0) {
			DashboardKegiatanKemahasiswaanAdmin window = new DashboardKegiatanKemahasiswaanAdmin("", "none", false);
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, kegiatanMahasiswaan,
				"Kegiatan Kemahasiswaan", "Rekap seluruh kegiatan organisasi dan prestasi kemahasiswaan semua mahasiswa.");
		}
	}

	public void onKegiatanMahasiswa(Event event) {
		if (kegiatanMahasiswa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kegiatanMahasiswa);
			MyInclude iframe = new MyInclude("/pages/master/formulir_kegiatan.zul");
			iframe.setParent(window);
		}
	}

	public void onOperatorSeluler(Event event) {
		if (operatorSeluler.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(operatorSeluler);
			MyInclude iframe = new MyInclude("/pages/master/operator_seluler.zul");
			iframe.setParent(window);
		}
	}

	public void onAlatTransport(Event event) {
		if (alatTransport.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(alatTransport);
			MyInclude iframe = new MyInclude("/pages/master/alat_transportasi_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public void onJenisTinggal(Event event) {
		if (jenisTinggal.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jenisTinggal);
			MyInclude iframe = new MyInclude("/pages/master/jenis_tinggal_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public void onPekerjaanOrtu(Event event) {
		if (pekerjaanOrtu.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(pekerjaanOrtu);
			MyInclude iframe = new MyInclude("/pages/master/pekerjaan.zul");
			iframe.setParent(window);
		}
	}

	public void onPenghasilanOrtu(Event event) {
		if (penghasilanOrtu.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(penghasilanOrtu);
			MyInclude iframe = new MyInclude("/pages/master/penghasilan.zul");
			iframe.setParent(window);
		}
	}

	public void onPendidikanOrtu(Event event) {
		if (pendidikanOrtu.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(pendidikanOrtu);
			MyInclude iframe = new MyInclude("/pages/master/pendidikan_ortu.zul");
			iframe.setParent(window);
		}
	}

	public void onFormTambahan(Event event) {
		if (manajemenFormTambahan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenFormTambahan);
			MyInclude iframe = new MyInclude("/pages/master/parameter_tambahan_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public void onPerkuliahanMahasiswa(Event event) {
		if (manajemenPerkuliahanMahasiswa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenPerkuliahanMahasiswa);
			MyInclude iframe = new MyInclude("/pages/master/kalender/perkuliahan/manajemen_penjadwalan_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenKelas(Event event) {
		if (manajemenKelas.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenKelas);
			MyInclude iframe = new MyInclude("/pages/master/kelas.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenKelompok(Event event) {
		if (manajemenKelompok.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenKelompok);
			MyInclude iframe = new MyInclude("/pages/master/kelompok_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenKelompokStatus(Event event) {
		if (manajemenKelompokStatus.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenKelompokStatus);
			MyInclude iframe = new MyInclude("/pages/master/kelompok_status_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenProgram(Event event) {
		if (manajemenProgram.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenProgram);
			MyInclude iframe = new MyInclude("/pages/master/program_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenKelompokStatusKeluar(Event event) {
		if (manajemenKelompokStatusKeluar.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenKelompokStatusKeluar);
			MyInclude iframe = new MyInclude("/pages/master/kelompok_status_keluar_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenAsrama(Event event) {
		if (manajemenAsrama.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenAsrama);
			MyInclude iframe = new MyInclude("/pages/master/asrama.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenDosenPA(Event event) {
		if (manajemenDosenPA.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenDosenPA);
			MyInclude iframe = new MyInclude("/pages/master/dosen_pembimbing_akademik.zul");
			iframe.setParent(window);
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
		tbmuser = Common.getCurrentUser();
		super.doAfterCompose(comp);
		Common.initLaguage();

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		// Sembunyikan tombol "Upload Foto (NIM)" bila hak akses (baca+ubah+hapus) tak lengkap.
		ais.common.helper.UploadFotoMassalHelper.terapkanGateTombolUpload(self);

		if (execution.getParameter("jurusan") == null) {
			if (tbmuser == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
				Common.goLogoff();
				return;
			}
		}
		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		if (add != null) { add.setVisible(tbmuser != null && CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE)); }
		if (add != null) { add.setTooltiptext("Tambah"); }

		searchkelas.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (searchkewarganegaraan != null) {
			MyComboitemConfig comboitem = new MyComboitemConfig();
			comboitem.setLabel(ais.database.model.Mahasiswa.WNI);
			comboitem.setValue(ais.database.model.Mahasiswa.WNI);
			searchkewarganegaraan.appendChild(comboitem);
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(ais.database.model.Mahasiswa.WNA);
			comboitem.setValue(ais.database.model.Mahasiswa.WNA);
			searchkewarganegaraan.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Semua");
			comboitem.setValue(null);
			searchkewarganegaraan.appendChild(comboitem);

			searchkewarganegaraan.setSelectedItem(comboitem);
			searchkewarganegaraan.setReadonly(true);
		}

		if (searchJenisSemester != null) { searchJenisSemester.setReadonly(true); }
		if (searchTahunAjaran != null) { searchTahunAjaran.setReadonly(true); }

		Common.generateTahunAjaran(searchTahunAjaran);
		MyComboitemConfig comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		searchJenisSemester.appendChild(comboitem);

		Common.selectComboItem(searchJenisSemester,
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(StatusSetelahLulus.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			StatusSetelahLulus statusSetelahLulus = new StatusSetelahLulus();
			statusSetelahLulus.setKode("1");
			statusSetelahLulus.setNama("Bekerja/Wirausaha");
			session.save(statusSetelahLulus);

			statusSetelahLulus = new StatusSetelahLulus();
			statusSetelahLulus.setKode("2");
			statusSetelahLulus.setNama("Melanjutkan Studi ke Jenjang Lebih Tinggi");
			session.save(statusSetelahLulus);

			statusSetelahLulus = new StatusSetelahLulus();
			statusSetelahLulus.setKode("3");
			statusSetelahLulus.setNama("Menuntut Ilmu di Pesantren");
			session.save(statusSetelahLulus);

			statusSetelahLulus = new StatusSetelahLulus();
			statusSetelahLulus.setKode("4");
			statusSetelahLulus.setNama("Menganggur");
			session.save(statusSetelahLulus);

			statusSetelahLulus = new StatusSetelahLulus();
			statusSetelahLulus.setKode("5");
			statusSetelahLulus.setNama("Lainnya");
			session.save(statusSetelahLulus);
		}

		count = ((Number) session.createCriteria(StatusPekerjaanSetelahLulus.class)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {
			StatusPekerjaanSetelahLulus statusPekerjaanSetelahLulus = new StatusPekerjaanSetelahLulus();
			statusPekerjaanSetelahLulus.setKode("1");
			statusPekerjaanSetelahLulus.setNama("PNS (selain Guru, Dosen, Dokter/Medis)");
			session.save(statusPekerjaanSetelahLulus);

			statusPekerjaanSetelahLulus = new StatusPekerjaanSetelahLulus();
			statusPekerjaanSetelahLulus.setKode("2");
			statusPekerjaanSetelahLulus.setNama("Guru");
			session.save(statusPekerjaanSetelahLulus);

			statusPekerjaanSetelahLulus = new StatusPekerjaanSetelahLulus();
			statusPekerjaanSetelahLulus.setKode("3");
			statusPekerjaanSetelahLulus.setNama("Dosen");
			session.save(statusPekerjaanSetelahLulus);

			statusPekerjaanSetelahLulus = new StatusPekerjaanSetelahLulus();
			statusPekerjaanSetelahLulus.setKode("4");
			statusPekerjaanSetelahLulus.setNama("Dokter/Tenaga Medis");
			session.save(statusPekerjaanSetelahLulus);

			statusPekerjaanSetelahLulus = new StatusPekerjaanSetelahLulus();
			statusPekerjaanSetelahLulus.setKode("5");
			statusPekerjaanSetelahLulus.setNama("Pegawai Swasta");
			session.save(statusPekerjaanSetelahLulus);

			statusPekerjaanSetelahLulus = new StatusPekerjaanSetelahLulus();
			statusPekerjaanSetelahLulus.setKode("6");
			statusPekerjaanSetelahLulus.setNama("Wiraswasta/Wirausaha");
			session.save(statusPekerjaanSetelahLulus);

			statusPekerjaanSetelahLulus = new StatusPekerjaanSetelahLulus();
			statusPekerjaanSetelahLulus.setKode("7");
			statusPekerjaanSetelahLulus.setNama("Lainnya");
			session.save(statusPekerjaanSetelahLulus);
		}

		count = ((Number) session.createCriteria(StatusDomisiliSetelahLulus.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			StatusDomisiliSetelahLulus statusDomisiliSetelahLulus = new StatusDomisiliSetelahLulus();
			statusDomisiliSetelahLulus.setKode("1");
			statusDomisiliSetelahLulus.setNama("Kembali ke Kota Asal");
			statusDomisiliSetelahLulus.setKeterangan("Kembali ke Kota Asal");
			session.save(statusDomisiliSetelahLulus);

			statusDomisiliSetelahLulus = new StatusDomisiliSetelahLulus();
			statusDomisiliSetelahLulus.setKode("2");
			statusDomisiliSetelahLulus.setNama("Tetap Tinggal di Kota dimana Perguruan Tinggi Berada");
			statusDomisiliSetelahLulus.setKeterangan("Tetap Tinggal di Kota dimana Perguruan Tinggi Berada");
			session.save(statusDomisiliSetelahLulus);

			statusDomisiliSetelahLulus = new StatusDomisiliSetelahLulus();
			statusDomisiliSetelahLulus.setKode("3");
			statusDomisiliSetelahLulus.setNama("Pindah ke Kota Lain");
			statusDomisiliSetelahLulus.setKeterangan("Pindah ke Kota Lain");
			session.save(statusDomisiliSetelahLulus);
		}

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		searchsemesterawal.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		searchsemesterawal.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchsemesterawal.appendChild(comboitem);
		if (searchsemesterawal != null) { searchsemesterawal.setSelectedItem(comboitem); }
		if (searchsemesterawal != null) { searchsemesterawal.setReadonly(true); }

		Common.insertComboDanSemua(searchJenisSeleksi, "nama", JenisSeleksi.class, Restrictions.eq("aktif", true));

		Common.insertComboDanSemua(searchstatusKeluar, "nama", StatusKeluar.class);
		Common.insertComboDanSemua(searchpredikatKelulusan, "nama", Judisium.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(searchstatusSetelahLulus, "nama", StatusSetelahLulus.class);
		Common.insertComboDanSemua(searchstatusPekerjaanSetelahLulus, "nama", StatusPekerjaanSetelahLulus.class);
		Common.insertComboDanSemua(searchstatusDomisiliSetelahLulus, "nama", StatusDomisiliSetelahLulus.class);

		if (add != null && (edit || Common.bolehKonfigurasi("aktifkan_download_data_mahasiswa"))) {
			MahasiswaAction.createUploadDanDownloadData(add.getParent(), new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			}, this, false, edit);
		}

		String[] contents = new String[] { "nim", "nama", "statusAwalMahasiswa", "jurusan" };

		MyToolbarbuttonConfig ukt = Common.cetakDataCustomButton(Mahasiswa.class, this, "Download UKT",
				"/img/print.png", contents);
		ukt.setVisible(edit && Common.bolehKonfigurasi("tampilkan_form_download_dan_upload_ukt", Konfigurasi.TIDAK_AKTIF));
		FilterLanjutHelper.tambahBtn(containerBtnLanjut, add, ukt);

		if (uploadUKT != null) {
			uploadUKT.setVisible(ukt.isVisible());
		}

		MyToolbarbuttonConfig generatePasswordMahasiswa = new MyToolbarbuttonConfig("Download Password",
				"/img/print.png");
		generatePasswordMahasiswa.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show(
						"Bapak/Ibu akan memperoleh nama pengguna (username) dan kata sandi (password) mahasiswa. Apabila Bapak/Ibu telah yakin untuk melanjutkan, silakan tekan tombol OK; atau tekan tombol Batal (Cancel) untuk membatalkan proses ini.",
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

									Session session = HibernateUtil.currentSession();
									int count = ((Number) session.createCriteria(Mahasiswa.class)
											.add(Restrictions.or(Restrictions.isNull("is_encripted"),
													Restrictions.eq("is_encripted", false)))
											.setProjection(Projections.rowCount()).uniqueResult()).intValue();
									if (count > 0) {
										List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class)
												.add(Restrictions.or(Restrictions.isNull("is_encripted"),
														Restrictions.eq("is_encripted", false)))
												.list();
										for (Mahasiswa mahasiswa : mahasiswas) {
											mahasiswa.setIs_encripted(true);
											mahasiswa.setPass(Common.desEncrypter.get().encrypt(mahasiswa.getPass()));
											session.update(mahasiswa);
										}
									}

									String filename = Sessions.getCurrent().getWebApp()
											.getRealPath("/tmp/user_password_mahasiswa_"
													+ URLEncoder.encode(Common.datetimeFormat2s.get()
															.format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
													+ ".xlsx");

									List<Mahasiswa> mahasiswas = initCriteria(true).add(Restrictions.isNotNull("nim"))
											.add(Restrictions.ne("nim", "")).setMaxResults(1048576).list();

									XSSFWorkbook workbook = new XSSFWorkbook();
									XSSFSheet sheet = workbook.createSheet("MAHASISWA");
									sheet.setDefaultColumnWidth(20);
									int rowIndex = 0;

									XSSFRow rowhead = sheet.createRow((short) 0);
									rowhead.createCell(0).setCellValue("Username");
									rowhead.createCell(1).setCellValue("Password");
									rowhead.createCell(2).setCellValue("Prodi");
									rowhead.createCell(3).setCellValue("Fakultas");
									rowhead.createCell(4).setCellValue("Nama Lengkap");
									rowhead.createCell(5).setCellValue("Kode Install Mobile");

									for (Mahasiswa mahasiswa : mahasiswas) {
										if (mahasiswa.getNama() != null && !mahasiswa.getNama().trim().isEmpty()) {
											rowIndex++;

											boolean sudahdiubah = false;

											try {
												if (mahasiswa.getPass() == null || mahasiswa.getPass().trim().isEmpty()
														|| mahasiswa.getPass().trim().equals("uRywMowySCU=")) {
													mahasiswa.setIs_encripted(true);
													mahasiswa.setPass(Common.desEncrypter.get().encrypt(mahasiswa.getNim()));
													Common.refreshUpdate(mahasiswa);
													sudahdiubah = true;
												}
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

											if (!sudahdiubah) {
												try {

													String desc2x = Common.desEncrypter.get()
															.decrypt(Common.desEncrypter.get().decrypt(mahasiswa.getPass()));
													System.out.println("desc2x = " + desc2x);
													if (desc2x != null && !desc2x.trim().isEmpty()) {
														mahasiswa.setIs_encripted(true);
														mahasiswa.setPass(
																Common.desEncrypter.get().encrypt(mahasiswa.getNim()));
														Common.refreshUpdate(mahasiswa);
														sudahdiubah = true;
													}
												} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
											}

											if (!sudahdiubah) {
												try {

													String desc1x = Common.desEncrypter.get().decrypt(mahasiswa.getPass());
													System.out
															.println("desc1x = " + desc1x + " " + mahasiswa.getPass());
													if (desc1x != null && desc1x.trim().isEmpty()) {
														mahasiswa.setIs_encripted(true);
														mahasiswa.setPass(
																Common.desEncrypter.get().encrypt(mahasiswa.getNim()));
														Common.refreshUpdate(mahasiswa);
														sudahdiubah = true;
													}
												} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

											}

											XSSFRow row = sheet.createRow(rowIndex);
											row.createCell(0).setCellValue(mahasiswa.getNim());

											try {
												row.createCell(1)
														.setCellValue(Common.desEncrypter.get().decrypt(mahasiswa.getPass()));
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

											row.createCell(2)
													.setCellValue(mahasiswa.getJurusan() == null
															|| mahasiswa.getJurusan().getFakultas() == null ? ""
																	: mahasiswa.getJurusan().getFakultas().getNama());
											row.createCell(3).setCellValue(mahasiswa.getJurusan() == null ? ""
													: mahasiswa.getJurusan().getNama());
											row.createCell(4).setCellValue(mahasiswa.getNama());

											String hasil = "";
											try {

												String username = mahasiswa.getNim() + ";"
														+ Common.getRequestHostWithProtocol();

												JSONObject postData = new JSONObject();
												postData.put("username", username);
												postData.put("link", link);
												postData.put("nama_pt", nama_pt);
												postData.put("login_bg_pt", background_login_PerguruanTinggi);
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

												String[] command = { "curl", "-d", postData.toString(), "-H",
														"Content-Type: application/json", strURL };

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

												row.createCell(5).setCellValue(
														jsonObject.isNull("code") ? "" : jsonObject.get("code") + "");
											} catch (Exception e) {
												ais.common.Common.tampilErrorJikaAdmin(e);
											}
										}
									}

									try {
										FileOutputStream fileOut = new FileOutputStream(filename);
										workbook.write(fileOut);
										fileOut.close();
									} catch (IOException e) {
										// TODO Auto-generated
										// catch
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

							}
						});

			}
		});
		FilterLanjutHelper.tambahBtn(containerBtnLanjut, add, generatePasswordMahasiswa);

		// Tombol "Upload Password" (unggah Excel utk set password mahasiswa massal). Field
		// `uploadPassword` + handler onUploadPassword SUDAH ada, tapi tombolnya tak pernah dibuat ->
		// tak pernah muncul. Dibuat di sini via VARIABEL LOKAL (sengaja BUKAN field `uploadPassword`
		// agar blok lama `if (uploadPassword != null){...}` — yang menyentuh downloadPassword/
		// uploadData yang juga null — tetap DORMAN & tidak NPE). Forward ke handler onUploadPassword;
		// tampil sesuai konfig + hak akses yang sama dengan "Download Password".
		MyToolbarbuttonConfig uploadPasswordMahasiswa = new MyToolbarbuttonConfig("Upload Password", "/img/upload.gif");
		uploadPasswordMahasiswa.setUpload(Common.ukuranFileUpload());
		uploadPasswordMahasiswa.setTooltiptext("Unggah berkas Excel (.xlsx) untuk mengatur password mahasiswa secara massal");
		uploadPasswordMahasiswa.addForward("onUpload", self, "onUploadPassword");
		FilterLanjutHelper.tambahBtn(containerBtnLanjut, add, uploadPasswordMahasiswa);
		uploadPasswordMahasiswa.setVisible(Common.bolehKonfigurasi("aktifkan_upload_password_mahasiswa", Konfigurasi.AKTIF)
				&& (add != null && add.isVisible()) && edit);

		MyToolbarbuttonConfig generatePasswordOrangTua = new MyToolbarbuttonConfig("Password Orang Tua",
				"/img/print.png");
		if (generatePasswordOrangTua != null) { generatePasswordOrangTua.setVisible(false); }
		generatePasswordOrangTua.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show(
						"Bapak/Ibu akan membuatkan sekaligus memperoleh nama pengguna (username) dan kata sandi (password) orang tua mahasiswa. Apabila Bapak/Ibu telah yakin untuk melanjutkan, silakan tekan tombol OK; atau tekan tombol Batal (Cancel) untuk membatalkan proses ini.",
						"Informasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											final String filename = Sessions.getCurrent().getWebApp()
													.getRealPath("/tmp/user_password_dosen_"
															+ URLEncoder.encode(Common.datetimeFormat2s.get()
																	.format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
															+ ".xlsx");

											List<Mahasiswa> mahasiswas = initCriteria(true)
													.add(Restrictions.isNotNull("nim")).add(Restrictions.ne("nim", ""))
													.setMaxResults(1048576).list();

											XSSFWorkbook workbook = new XSSFWorkbook();
											XSSFSheet sheet = workbook.createSheet("DOSEN");
											sheet.setDefaultColumnWidth(20);
											int rowIndex = 0;

											XSSFRow rowhead = sheet.createRow((short) 0);
											rowhead.createCell(0).setCellValue("ID");
											rowhead.createCell(1).setCellValue("Username Orang Tua");
											rowhead.createCell(2).setCellValue("Password Orang Tua");
											rowhead.createCell(3).setCellValue("Fakultas");
											rowhead.createCell(4).setCellValue("Prodi");
											rowhead.createCell(5).setCellValue("Program");
											rowhead.createCell(6).setCellValue("NIM Mahasiswa");
											rowhead.createCell(7).setCellValue("Nama Mahasiswa");
											rowhead.createCell(8).setCellValue("Email");
											rowhead.createCell(9).setCellValue("HP");

											for (Mahasiswa mahasiswa : mahasiswas) {
												if (mahasiswa.getNama() != null
														&& !mahasiswa.getNama().trim().isEmpty()) {
													rowIndex++;
													Session session = HibernateUtil.currentNativeSession();
													try {

														if (mahasiswa.getUserOrtu() == null
																|| mahasiswa.getUserOrtu().trim().isEmpty()) {

															String newUsername = StringUtils.split(mahasiswa.getNama(),
																	" ")[0] + "" + RandomStringUtils.randomNumeric(5);
															String passw = RandomStringUtils.randomNumeric(5);

															newUsername = newUsername.toLowerCase().trim();

															mahasiswa.setUserOrtu(newUsername);
															mahasiswa.setPassOrtu(
																	Common.desEncrypter.get().encrypt(passw.trim()));

															session.getTransaction().begin();
															Common.refreshUpdate(session, mahasiswa);
															session.getTransaction().commit();
														}

														XSSFRow row = sheet.createRow(rowIndex);
														row.createCell(0).setCellValue(mahasiswa.getId());
														row.createCell(1).setCellValue(mahasiswa.getUserOrtu());

														try {
															row.createCell(2).setCellValue(Common.desEncrypter.get()
																	.decrypt(mahasiswa.getPassOrtu()));
														} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

														row.createCell(3).setCellValue(
																mahasiswa.getJurusan().getFakultas().getNama());
														row.createCell(4)
																.setCellValue(mahasiswa.getJurusan().getNama());
														row.createCell(5).setCellValue(mahasiswa.getProgram());
														row.createCell(6).setCellValue(mahasiswa.getNim());
														row.createCell(7).setCellValue(mahasiswa.getNama());
														row.createCell(8).setCellValue(mahasiswa.getEmail());
														row.createCell(9).setCellValue(mahasiswa.getTelp());
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
		FilterLanjutHelper.tambahBtn(containerBtnLanjut, add, generatePasswordOrangTua);

		boolean merupakanAdmin = (tbmuser != null && tbmuser != null && tbmuser.hakAkses() != null
				&& tbmuser.hakAkses() != null && tbmuser != null && tbmuser.hakAkses() != null
				&& tbmuser.hakAkses().getRoleId() != null && (tbmuser != null && tbmuser.hakAkses() != null
						&& Common.getCurrentUser().hakAkses().getRoleId().equalsIgnoreCase(Tbmrole.ADMINISTRATOR)));

		generatePasswordMahasiswa
				.setVisible(Common.bolehKonfigurasi("aktifkan_generate_password_mahasiswa") && (add != null && add.isVisible()) && edit);

		if (uploadPassword != null) {
			uploadPassword.setVisible(Common.bolehKonfigurasi("aktifkan_upload_password_mahasiswa") && (add != null && add.isVisible()) && edit && merupakanAdmin);
			downloadPassword
					.setVisible(Common.bolehKonfigurasi("aktifkan_download_password_mahasiswa", Konfigurasi.TIDAK_AKTIF) && (add != null && add.isVisible()) && edit && merupakanAdmin);
			uploadData.setVisible(Common.bolehKonfigurasi("aktifkan_upload_data_mahasiswa") && (add != null && add.isVisible()) && edit && merupakanAdmin);

			downloadFormatMahasiwa
					.setVisible(Common.bolehKonfigurasi("aktifkan_download_format_mahasiwa") && (add != null && add.isVisible()) && edit);

			List<String> adminLain = new ArrayList<String>();
			Collections.addAll(adminLain,
					Common.getKonfigurasi("admin_lain_yg_boleh_singkonkan_status", "").getNilai().split(";"));

			boolean merupakanAdminLain = (tbmuser != null && tbmuser != null && tbmuser.hakAkses() != null
					&& tbmuser.hakAkses() != null && tbmuser != null && tbmuser.hakAkses() != null
					&& tbmuser.hakAkses().getRoleId() != null && (tbmuser != null && tbmuser.hakAkses() != null
							&& adminLain.contains(tbmuser.hakAkses().getRoleId())));

			synchronizeStatus.setVisible(
					Common.bolehKonfigurasi("aktifkan_synchronize_status") && (add != null && add.isVisible()) && edit && (merupakanAdmin || merupakanAdminLain));
		}

		if (uploadRfid != null) {
			uploadRfid.setVisible(Common.bolehKonfigurasi("aktifkan_upload_rfid_mahasiswa", Konfigurasi.TIDAK_AKTIF) && (add != null && add.isVisible()) && edit && merupakanAdmin);
			downloadRfid.setVisible(Common.bolehKonfigurasi("aktifkan_download_rfid_mahasiswa", Konfigurasi.TIDAK_AKTIF) && (add != null && add.isVisible()) && edit && merupakanAdmin);
		}

		generatePasswordOrangTua
				.setVisible(Common.bolehKonfigurasi("aktifkan_download_password_orang_tua") && (add != null && add.isVisible()) && edit);

		Common.initPrograms(searchprogram);
		Common.insertComboDanSemua(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertComboDanSemua(searchstatus, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen mydosen = tbmuser.ambilDosen();
			searchdosen.setValue(mydosen.getNama());
			searchdosen.setAttribute("myValue", mydosen);
			searchdosen.setAttribute("dosen", mydosen);
			searchdosen.setDisabled(true);
		}
		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		if (execution.getParameter("statusKeluar") != null) {
			Long selectedStatusKeluar = Long.parseLong(execution.getParameter("statusKeluar"));
			Common.selectComboItem(searchstatusKeluar, new StatusKeluar(selectedStatusKeluar));
		}

		if (execution.getParameter("jurusan") != null) {
			Long selectedJurusan = Long.parseLong(execution.getParameter("jurusan"));
			Common.selectComboItem(searchjurusan, new Jurusan(selectedJurusan));
		}
		if (execution.getParameter("statusAwalMahasiswa") != null) {
			Long selectedStatusAwalMahasiswa = Long.parseLong(execution.getParameter("statusAwalMahasiswa"));
			Common.selectComboItem(searchStatusAwalMahasiswa, new StatusAwalMahasiswa(selectedStatusAwalMahasiswa));
		}

		if (execution.getParameter("kelamin") != null) {
			kelaminData = execution.getParameter("kelamin");

		}

		if (execution.getParameter("tahunangkatan") != null) {
			Integer selectedTahunangkatan = Integer.parseInt(execution.getParameter("tahunangkatan"));
			searchtahun.setValue(new BigDecimal(selectedTahunangkatan));
		}
		if (execution.getParameter("tahunLulus") != null) {
			Integer selectedTahunLulus = Integer.parseInt(execution.getParameter("tahunLulus"));
			searchtahunLulus.setValue(new BigDecimal(selectedTahunLulus));
		}
		if (execution.getParameter("masa") != null) {
			Integer masa = Integer.parseInt(execution.getParameter("masa"));
			searchMasa.setValue(new BigDecimal(masa));
		}
		if (execution.getParameter("semesterLulus") != null) {
			Integer masa = Integer.parseInt(execution.getParameter("semesterLulus"));
			searchsemesterLulus.setValue(new BigDecimal(masa));
		}

		if (execution.getParameter("program") != null) {
			String selectedProgram = execution.getParameter("program");
			Common.selectComboItem(searchStatusAwalMahasiswa, selectedProgram);
		}
		if (execution.getParameter("selectedStatusMahasiswa") != null) {
			Long selectedStatusMahasiswa = Long.parseLong(execution.getParameter("selectedStatusMahasiswa"));
			Common.selectComboItem(searchstatus, new StatusMahasiswa(selectedStatusMahasiswa));
		}
		if (execution.getParameter("statusKeluar") != null) {
			Common.selectComboItem(searchstatusKeluar,
					new StatusKeluar(Long.parseLong(execution.getParameter("statusKeluar"))));
		}
		if (execution.getParameter("predikatKelulusan") != null) {
			Common.selectComboItem(searchpredikatKelulusan,
					new Judisium(Long.parseLong(execution.getParameter("predikatKelulusan"))));
		}
		if (execution.getParameter("statusSetelahLulus") != null) {
			Common.selectComboItem(searchstatusSetelahLulus,
					new StatusSetelahLulus(Long.parseLong(execution.getParameter("statusSetelahLulus"))));
		}
		if (execution.getParameter("statusPekerjaanSetelahLulus") != null) {
			Common.selectComboItem(searchstatusPekerjaanSetelahLulus, new StatusPekerjaanSetelahLulus(
					Long.parseLong(execution.getParameter("statusPekerjaanSetelahLulus"))));
		}
		if (execution.getParameter("statusDomisiliSetelahLulus") != null) {
			Common.selectComboItem(searchstatusDomisiliSetelahLulus, new StatusDomisiliSetelahLulus(
					Long.parseLong(execution.getParameter("statusDomisiliSetelahLulus"))));
		}

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		{
			if (uploadData != null) uploadData.setDisabled(!(add != null && add.isVisible()) || !edit);
			if (downloadFormatMahasiwa != null) downloadFormatMahasiwa.setDisabled(!(add != null && add.isVisible()) || !edit);

			if (downloadFormatMahasiwa != null) downloadFormatMahasiwa.setVisible(false);
			if (uploadData != null) uploadData.setVisible(false);

			MyToolbarbuttonConfig downloadLampiran = new MyToolbarbuttonConfig("Lampiran", "/img/attachment-icon.png");
			downloadLampiran.setVisible((add != null && add.isVisible()) && edit);
			downloadLampiran.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onDownloadLampiran(arg0);
				}
			});
			FilterLanjutHelper.tambahBtn(containerBtnLanjut, add, downloadLampiran);

			// Tombol "Download Foto" lama DIHAPUS — digantikan tombol "Download Foto Massal" (bulk ZIP,
			// multi-thread + bar progres) di toolbar utama, agar seragam antar-halaman. Handler
			// onDownloadFoto lama dibiarkan (tak dipakai, tak mengganggu).

			MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Export ke OJS", "/img/corner.gif");
			FilterLanjutHelper.tambahBtn(containerBtnLanjut, add, exportKeOjs);
			exportKeOjs.setVisible((add != null && add.isVisible()) && edit && Common.bolehKonfigurasi("terhubung_ke_ojs", Konfigurasi.TIDAK_AKTIF));
			exportKeOjs.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.createDefaultTimer(new EventListener() {

						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Mahasiswa> tbmusers = initCriteria(true).list();

							Session ojSession = OjsHibernateUtil.getInstance().currentSession();
							List<Journals> journals = ojSession.createCriteria(Journals.class).list();
							for (Mahasiswa mahasiswa : tbmusers) {
								MahasiswaAction.updateUser(ojSession, mahasiswa, journals);
							}

							OjsHibernateUtil.getInstance().closeSession();

							MyMessageboxConfig.show(
									"Proses ekspor data ke OJS (Open Journal Systems) telah berhasil dilakukan. Terima kasih, Bapak/Ibu, seluruh data telah dikirim dan tersimpan dengan baik.",
									"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

						}
					});
				}
			});

			if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

				MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Krm ke Feeder",
						"/img/Finance-Invoice-icon.png");
				buttonTagihan.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						MyMessageboxConfig.show(
								"Apakah Bapak/Ibu yakin ingin mengirim data ke Feeder (PDDikti)? Proses ini akan mengirimkan data mahasiswa ke server Feeder dan dapat memerlukan beberapa saat. Silakan tekan tombol OK untuk melanjutkan, atau tekan tombol Batal (Cancel) untuk membatalkan.",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
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

												ais.action.master.feeder.util.NeoFeederPesanFormalHelper.tampilkanGagalKoneksi(ip, port,
											Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF),
											"Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons).");
												return;
											}

											final List<String> errorLog = new ArrayList<String>();
											final Label myLabelProsesDetail = NeoFeederProgressHelper
													.show("Sinkronisasi Neo Feeder", new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															if (arg0 != null && !arg0.getName().isEmpty()) {
																EksporFromFeederAction.display();
																MyMessageboxConfig.show(arg0.getName(), "Info",
																		MyMessageboxConfig.OK,
																		MyMessageboxConfig.EXCLAMATION);
															}

															if (!errorLog.isEmpty()) {
																String err = "";
																for (String s : errorLog) {
																	err += err.isEmpty() ? s
																			: "\n----------------------------------------------------------------------------------------------------------\n"
																					+ s;
																}

																MyMessageboxConfig.show(
																		"Mohon maaf, telah terjadi kesalahan (error) selama proses berlangsung. Catatan kesalahan (error log) akan otomatis diunduh ke perangkat Bapak/Ibu. Langkah yang dapat dilakukan: (1) buka berkas catatan kesalahan yang telah diunduh; (2) periksa keterangan kesalahan yang tercantum; (3) apabila diperlukan, sampaikan berkas tersebut kepada administrator sistem untuk penanganan lebih lanjut.",
																		"Error Terjadi", MyMessageboxConfig.OK,
																		MyMessageboxConfig.EXCLAMATION);

																File file = new File(Common.REAL_PATH + "/tmp/error_"
																		+ Common.randLong() + ".txt");
																if (!file.getParentFile().exists()) {
																	file.getParentFile().mkdirs();
																}
																FileUtils.writeStringToFile(file, err);
																Filedownload.save(file, "text/plain");
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
															// FIX IllegalStateException "Components can be accessed only in event listeners":
															// .setValue() langsung dari thread latar tidak aman -- WAJIB lewat
															// NeoFeederProgressHelper.updateProgres().
															ais.action.master.feeder.util.NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
																	"Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
															return;
														}

														FeederExporter feederImporter = new FeederExporter(
																feederConnector, token, null, null, null);

														List<Mahasiswa> tbmusers = ConstantValues
																.simpleList(initCriteria(true), Mahasiswa.class);
														int size = tbmusers.size();
														int index = 1;
														for (Mahasiswa mahasiswa : tbmusers) {
															ais.action.master.feeder.util.NeoFeederProgressHelper.updateProgres(myLabelProsesDetail, "Memproses "
																	+ mahasiswa.getNim() + " " + mahasiswa.getNama()
																	+ " ("
																	+ Common.numberFormat.get().format((index * 100.0) / size)
																	+ "%");
															index++;
															// FIX "satu mahasiswa gagal menghentikan seluruh batch": bungkus per-item.
															try {
																exportKeFeeder(mahasiswa, feederImporter, token,
																	feederConnector, errorLog);
															} catch (Exception exSatu) {
																ais.common.Common.tampilErrorJikaAdmin(exSatu);
																errorLog.add("[" + mahasiswa.getNim() + " " + mahasiswa.getNama()
																	+ "] Gagal mengirim data Mahasiswa ke Neo Feeder: " + exSatu.getMessage());
															}
														}
														tbmusers.clear();
														tbmusers = null;
														// FIX "gagal diam-diam": setValue("") sebelumnya dijalankan TANPA SYARAT
														// di luar try (bahkan setelah exception) -- popup selalu menutup seolah sukses.
														ais.action.master.feeder.util.NeoFeederProgressHelper.updateProgres(myLabelProsesDetail, "");
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
														ais.action.master.feeder.util.NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
															"Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																"pengiriman data Mahasiswa ke Neo Feeder", null, e,
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
				FilterLanjutHelper.tambahBtn(containerBtnLanjut, add, buttonTagihan);

				buttonTagihan = new MyToolbarbuttonConfig("Ambil dr feeder", "/img/Button-Refresh-icon.png");
				buttonTagihan.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						MyMessageboxConfig.show(
								"Apakah Bapak/Ibu yakin ingin mengambil data dari Feeder (PDDikti)? Proses ini akan menarik data mahasiswa dari server Feeder dan dapat memerlukan beberapa saat. Silakan tekan tombol OK untuk melanjutkan, atau tekan tombol Batal (Cancel) untuk membatalkan.",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
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

												ais.action.master.feeder.util.NeoFeederPesanFormalHelper.tampilkanGagalKoneksi(ip, port,
											Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF),
											"Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons).");
												return;
											}

											final Label myLabelProsesDetail = NeoFeederProgressHelper
													.show("Sinkronisasi Neo Feeder", new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															if (arg0 != null && !arg0.getName().isEmpty()) {
																EksporFromFeederAction.display();
																MyMessageboxConfig.show(arg0.getName(), "Info",
																		MyMessageboxConfig.OK,
																		MyMessageboxConfig.EXCLAMATION);
															}
															onSearchDefault(null);
														}
													});

											new Thread(new Runnable() {

												@SuppressWarnings("unchecked")
												@Override
												public void run() {
													try {
													try {
														FeederConnector feederConnector = new FeederConnector(ip,
																Integer.parseInt(port), null);

														String token = feederConnector.getToken(username, password);
														System.out.println("TOKEN => " + token);

														if (token == null || token.trim().isEmpty()
																|| token.trim().toLowerCase().startsWith("error")) {
															// FIX IllegalStateException "Components can be accessed only in
															// event listeners": thread latar tak punya konteks eksekusi ZK
															// aktif -- Label.setValue() langsung memicu smartUpdate() yang
															// butuh itu. Pakai NeoFeederProgressHelper.updateProgres()
															// (menjadwalkan lewat Executions.schedule() pada Desktop yang
															// direkam saat popup dibuat, lihat javadoc kelas tsb).
															ais.action.master.feeder.util.NeoFeederProgressHelper.updateProgres(
																	myLabelProsesDetail, "Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
															return;
														}

														String filter = "";

														if (!searchnim.getValue().trim().isEmpty()) {
															for (String o : searchnim.getValue().trim().split(";")) {
																String s = "upper(trim(nipd)) = upper(trim('" + o
																		+ "'))";
																filter += filter.isEmpty() ? s : " or " + s;
															}
														}

														if (!searchnama.getValue().trim().isEmpty()) {
															for (String o : searchnama.getValue().trim().split(";")) {
																String s = "upper(trim(nama_mahasiswa)) = upper(trim('"
																		+ o + "'))";
																filter += filter.isEmpty() ? s : " or " + s;
															}
														}

														if (searchtahun.getValue() != null) {
															String s = "id_periode='"
																	+ searchtahun.getValue().intValue() + "1'";
															filter += filter.isEmpty() ? s : " and " + s;
														}

														Jurusan jur = (Jurusan) (searchjurusan.getSelectedItem() == null
																|| searchjurusan.getSelectedItem().getValue() == null
																		? null
																		: searchjurusan.getSelectedItem().getValue());
														if (jur != null && jur.getFeeder() != null
																&& !jur.getFeeder().isEmpty()) {
															String s = "id_prodi='" + jur.getFeeder() + "'";
															filter += filter.isEmpty() ? s : " and " + s;
														}

												Integer countInteger = feederConnector.getCount(token,
														"GetCountMahasiswa",
														filter.replaceAll("nipd", "a.nim"));
												if (countInteger == null || countInteger.intValue() < 0) {
													countInteger = Integer.valueOf(0);
												}

														System.out.println("results countInteger -> " + countInteger);

														for (int index = 0; index <= countInteger; index += 500) {

															JSONArray dataMhsPt = feederConnector.getData(
																	"GetListMahasiswa", token, filter, "", "500",
																	index + "");

															System.out.println(
																	"results dataMhsPt -> " + dataMhsPt.length());

															for (int i = 0; i < dataMhsPt.length(); i++) {
																JSONObject jsonObject = dataMhsPt.getJSONObject(i);

																System.out.println(
																		"results GetListMahasiswa -> " + jsonObject);

																try {
																	String nipd = jsonObject.get("nipd") + "";
																	Mahasiswa mahasiswa = ConstantValues
																			.ambilByNim(nipd);
																	if (mahasiswa != null) {

																		String id_mahasiswa = jsonObject
																				.getString("id_mahasiswa");
																		if (mahasiswa.getFeeder() == null || !mahasiswa
																				.getFeeder()
																				.equalsIgnoreCase(id_mahasiswa)) {
																			mahasiswa.setFeeder(id_mahasiswa);
																			Session session = HibernateUtil
																					.currentNativeSession();
																			session.getTransaction().begin();
																			Common.refreshUpdate(session, mahasiswa);
																			session.getTransaction().commit();
																			// session.disconnect();
																			ais.common.Common.closeOpenedSession(session);
																		}

																		String id_registrasi_mahasiswa = jsonObject
																				.getString("id_registrasi_mahasiswa");
																		if (mahasiswa.getIdRegPd() == null
																				|| (!mahasiswa.getIdRegPd()
																						.equalsIgnoreCase(
																								id_registrasi_mahasiswa))) {
																			mahasiswa.setIdRegPd(
																					id_registrasi_mahasiswa);
																			Session session = HibernateUtil
																					.currentNativeSession();
																			session.getTransaction().begin();
																			Common.refreshUpdate(session, mahasiswa);
																			session.getTransaction().commit();
																			// session.disconnect();
																			ais.common.Common.closeOpenedSession(session);
																		}
																	}
																} catch (Exception e) {
																	ais.common.Common.tampilErrorJikaAdmin(e);
																}

																JSONArray dataMhs = feederConnector.getData(
																		"GetBiodataMahasiswa", token,
																		"id_mahasiswa='" + jsonObject
																				.getString("id_mahasiswa").trim() + "'",
																		"", "10", index + "");

																System.out.println("results GetBiodataMahasiswa -> "
																		+ dataMhs.length());

																if (dataMhs.length() > 0) {
																	for (int j = 0; j < dataMhs.length(); j++) {
																		try {
																			JSONObject jsonObjectMhs = dataMhs
																					.getJSONObject(j);

																			JSONArray dataMhsLulusDo = feederConnector
																					.getData("GetListMahasiswaLulusDO",
																							token,
																							"id_mahasiswa='"
																									+ jsonObjectMhs
																											.getString(
																													"id_mahasiswa")
																									+ "'",
																							"", "1", index + "");

																			System.out.println(
																					"results GetListMahasiswaLulusDO -> "
																							+ dataMhsLulusDo);

																			JSONObject jsonObjectMhsLulsuDo = dataMhsLulusDo
																					.length() > 0
																							? dataMhsLulusDo
																									.getJSONObject(0)
																							: null;

																			if (jsonObjectMhsLulsuDo != null) {
																				Iterator<String> it = jsonObjectMhsLulsuDo
																						.keys();
																				while (it.hasNext()) {
																					String key = it.next();
																					jsonObjectMhs.put(key,
																							jsonObjectMhsLulsuDo
																									.get(key));
																				}
																			}

																			System.out.println(
																					"results GetBiodataMahasiswa -> "
																							+ jsonObjectMhs);

																			FeederJSONImport.mahasiswa(jsonObject);
																			FeederJSONImport
																					.mahasiswa_aja(jsonObjectMhs);
																			String key = (jsonObject.get("nipd") + "")
																					.trim();
																			String nama = (jsonObjectMhs
																					.get("nama_mahasiswa") + "").trim();
																			ais.action.master.feeder.util.NeoFeederProgressHelper.updateProgres(myLabelProsesDetail, "Memproses "
																					+ key + " " + nama + " ("
																					+ Common.numberFormat.get().format(
																							((index + i) * 100.0)
																									/ countInteger)
																					+ "%");
																		} catch (Exception e) {
																			ais.common.Common.tampilErrorJikaAdmin(e);
																		}
																	}
																} else {
																	try {

																		JSONArray dataMhsLulusDo = feederConnector
																				.getData("GetListMahasiswaLulusDO",
																						token,
																						"id_mahasiswa='"
																								+ jsonObject.getString(
																										"id_mahasiswa")
																								+ "'",
																						"", "1", index + "");

																		System.out.println(
																				"results GetListMahasiswaLulusDO -> "
																						+ dataMhsLulusDo);

																		JSONObject jsonObjectMhsLulsuDo = dataMhsLulusDo
																				.length() > 0
																						? dataMhsLulusDo
																								.getJSONObject(0)
																						: null;

																		if (jsonObjectMhsLulsuDo != null) {
																			Iterator<String> it = jsonObjectMhsLulsuDo
																					.keys();
																			while (it.hasNext()) {
																				String key = it.next();
																				jsonObject.put(key,
																						jsonObjectMhsLulsuDo.get(key));
																			}
																		}

																		FeederJSONImport.mahasiswa(jsonObject);

																		try {
																			FeederJSONImport.mahasiswa_aja(jsonObject);
																		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

																		String key = (jsonObject.get("nipd") + "")
																				.trim();
																		String nama = (jsonObject.get("nama_mahasiswa")
																				+ "").trim();
																		ais.action.master.feeder.util.NeoFeederProgressHelper.updateProgres(myLabelProsesDetail, "Memproses " + key
																				+ " " + nama + " ("
																				+ Common.numberFormat.get()
																						.format(((index + i) * 100.0)
																								/ countInteger)
																				+ "%");
																	} catch (Exception e) {
																		ais.common.Common.tampilErrorJikaAdmin(e);
																	}
																}
															}

														}

													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}

													ais.action.master.feeder.util.NeoFeederProgressHelper.updateProgres(myLabelProsesDetail, "");
																									} finally {
														ais.database.hibernate.HibernateUtil.closeSession();
													}
												}
											}).start();

										}

									}
								});

					}
				});
				FilterLanjutHelper.tambahBtn(containerBtnLanjut, add, buttonTagihan);

				buttonTagihan = new MyToolbarbuttonConfig("Ambil Nilai", "/img/Finance-Invoice-icon.png");

				buttonTagihan.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

//						if (searchtahun.getValue() == null || searchtahun.getValue().intValue() < 1000) {
//							MyMessageboxConfig.show(
//									"Sebelum mengambil data nilai mahasiswa, tahun angkatan mahasiswa harus diisi",
//									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
//							searchtahun.focus();
//							return;
//						}

						MyMessageboxConfig.show(
								"Perlu Bapak/Ibu ketahui, data nilai yang telah diinputkan pada sistem atau nilai mahasiswa yang lebih besar dari 0 (nol) tidak akan diambil dari Feeder. Hanya data perkuliahan yang belum dinilai saja yang akan diambil dari Feeder, sehingga nilai yang telah ada tidak akan tertimpa.\n\nApakah Bapak/Ibu yakin ingin melanjutkan proses pengambilan data nilai ini? Silakan tekan tombol OK untuk melanjutkan, atau tekan tombol Batal (Cancel) untuk membatalkan.",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {

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

												ais.action.master.feeder.util.NeoFeederPesanFormalHelper.tampilkanGagalKoneksi(ip, port,
											Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF),
											"Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons).");
												return;
											}

											final List<String> errorLog = new ArrayList<String>();
											final Label myLabelProsesDetail = NeoFeederProgressHelper
													.show("Sinkronisasi Neo Feeder", new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															if (arg0 != null && !arg0.getName().isEmpty()) {
																EksporFromFeederAction.display();
																MyMessageboxConfig.show(arg0.getName(), "Info",
																		MyMessageboxConfig.OK,
																		MyMessageboxConfig.EXCLAMATION);
															}

															if (!errorLog.isEmpty()) {
																String err = "";
																for (String s : errorLog) {
																	err += err.isEmpty() ? s
																			: "\n----------------------------------------------------------------------------------------------------------\n"
																					+ s;
																}

																MyMessageboxConfig.show(
																		"Mohon maaf, telah terjadi kesalahan (error) selama proses berlangsung. Catatan kesalahan (error log) akan otomatis diunduh ke perangkat Bapak/Ibu. Langkah yang dapat dilakukan: (1) buka berkas catatan kesalahan yang telah diunduh; (2) periksa keterangan kesalahan yang tercantum; (3) apabila diperlukan, sampaikan berkas tersebut kepada administrator sistem untuk penanganan lebih lanjut.",
																		"Error Terjadi", MyMessageboxConfig.OK,
																		MyMessageboxConfig.EXCLAMATION);

																File file = new File(Common.REAL_PATH + "/tmp/error_"
																		+ Common.randLong() + ".txt");
																if (!file.getParentFile().exists()) {
																	file.getParentFile().mkdirs();
																}
																FileUtils.writeStringToFile(file, err);
																Filedownload.save(file, "text/plain");
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
															// FIX IllegalStateException "Components can be accessed only in event listeners":
															// .setValue() langsung dari thread latar tidak aman -- WAJIB lewat
															// NeoFeederProgressHelper.updateProgres().
															ais.action.master.feeder.util.NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
																	"Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
															return;
														}

														List<Mahasiswa> tbmusers = ConstantValues
																.simpleList(initCriteria(true), Mahasiswa.class);
														int size = tbmusers.size();
														int index = 1;
														for (Mahasiswa mahasiswa : tbmusers) {
															ais.action.master.feeder.util.NeoFeederProgressHelper.updateProgres(myLabelProsesDetail, "Memproses "
																	+ mahasiswa.getNim() + " " + mahasiswa.getNama()
																	+ " ("
																	+ Common.numberFormat.get().format((index * 100.0) / size)
																	+ "%");
															index++;
															try {
																MahasiswaAction.ambilNilaiDariFeeder(feederConnector,
																		token, 0, mahasiswa, tbmuser, null);
															} catch (Exception e) {
																ais.common.Common.tampilErrorJikaAdmin(e);
															}
														}
														tbmusers.clear();
														tbmusers = null;
														// FIX "gagal diam-diam": setValue("") sebelumnya dijalankan TANPA SYARAT
														// di luar try (bahkan setelah exception) -- popup selalu menutup seolah sukses.
														ais.action.master.feeder.util.NeoFeederProgressHelper.updateProgres(myLabelProsesDetail, "");
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
														ais.action.master.feeder.util.NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
															"Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																"pengambilan data Nilai Mahasiswa dari Neo Feeder", null, e,
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
				FilterLanjutHelper.tambahBtn(containerBtnLanjut, add, buttonTagihan);
			}

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
			button.setDisabled(!edit);
			button.setVisible((add != null && add.isVisible()) && edit);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					RevisiMahasiswaHelper revisiHelper = new RevisiMahasiswaHelper(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
								}
							});
						}
					});
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
					revisiHelper.setVisible(true);
					revisiHelper.onModal();

				}

			});
			FilterLanjutHelper.tambahBtn(containerBtnLanjut, add, button);
		}

		if (Common.bolehKonfigurasi("upload_download_email_mahasiswa", Konfigurasi.TIDAK_AKTIF)) {

			String[] contents1 = new String[] { "id", "mahasiswa.nim", "mahasiswa.nama", "email", "emailAtasan" };
			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(BiodataMahasiswa.class, new DataCriteria() {

				@SuppressWarnings("unchecked")
				@Override
				public Criteria initCriteria(boolean order) {

					List<Long> mhs = MahasiswaAction.this.initCriteria(true).setProjection(Projections.property("id"))
							.list();

					return HibernateUtil.currentSession().createCriteria(BiodataMahasiswa.class)
							.add(mhs.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.in("mahasiswa.id", mhs));
				}
			}, contents1);
			cetakToolbarbutton.setLabel("Download Email");
			FilterLanjutHelper.tambahBtn(containerBtnLanjut, add, cetakToolbarbutton);

			MyToolbarbuttonConfig upload = Common.uploadData(this, BiodataMahasiswa.class, contents1);
			upload.setLabel("Upload Email");
			upload.setVisible((add != null && add.isVisible()) && edit && delete);
			FilterLanjutHelper.tambahBtn(containerBtnLanjut, add, upload);

		}

		contents = new String[] { "nim", "nama", "dosenPa.nidn", "dosenPa.code", "dosenPa.nama", "jurusan.nama",
				"tahunangkatan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Mahasiswa.class, this, "Dosen PA",
				"/img/print.png", contents);
		FilterLanjutHelper.tambahBtn(containerBtnLanjut, add, cetakToolbarbutton);

		// Semua baris filter masuk popup — area filter utama disembunyikan,
		// hanya tombol "⋮ Filter" yang tampil di header.
		FilterLanjutHelper.setup(comp);
	}

	public static void ambilPerkuliahanDariFeeder(FeederConnector feederConnector, String token, String id_kls)
			throws Exception {

		String filter = "id_kelas_kuliah='" + id_kls + "'";
		JSONArray dataDetailKelasKuliah = feederConnector.getData("GetDetailKelasKuliah", token, filter, "", "1000",
				"");
		for (int i = 0; i < dataDetailKelasKuliah.length(); i++) {
			JSONObject kelas_kuliah = dataDetailKelasKuliah.getJSONObject(i);

			System.out.println(" kelas_kuliah = " + kelas_kuliah);
			FeederJSONImport.perkuliahan(kelas_kuliah);
		}

	}

	/**
	 * Impor kelas kuliah LENGKAP dari feeder: jadwal/kelas + dosen pengajar + peserta
	 * (mahasiswa/KRS). Dipakai tombol "Ambil dari Feeder" di Jadwal Perkuliahan agar sekali
	 * proses juga mengisi data dosen dan mahasiswa kelas tersebut.
	 */
	public static void ambilKelasLengkapDariFeeder(FeederConnector feederConnector, String token, String id_kls,
			Tbmuser tbmuser) throws Exception {
		// 1) Kelas kuliah (jadwal perkuliahan)
		ambilPerkuliahanDariFeeder(feederConnector, token, id_kls);
		// 2) Dosen pengajar kelas
		ambilDosenPengajarKelasDariFeeder(feederConnector, token, id_kls);
		// 3) Peserta kelas (mahasiswa / KRS)
		ambilPesertaKelasDariFeeder(feederConnector, token, id_kls, tbmuser);
	}

	/**
	 * Impor dosen pengajar sebuah kelas kuliah dari feeder (GetDosenPengajarKelasKuliah), lalu
	 * tempatkan ke slot dosen1..dosen10 pada Perkuliahan bila belum terdaftar. Logika mengikuti
	 * bagian dosen pada {@link #ambilNilaiDariFeeder}.
	 */
	public static void ambilDosenPengajarKelasDariFeeder(FeederConnector feederConnector, String token, String id_kls)
			throws Exception {
		String filter = "id_kelas_kuliah='" + id_kls + "'";
		JSONArray dataDosenPengajarKelasKuliah = feederConnector.getData("GetDosenPengajarKelasKuliah", token, filter,
				"", "1000", "");

		for (int i = 0; i < dataDosenPengajarKelasKuliah.length(); i++) {
			JSONObject id_aktivitas_mengajar = dataDosenPengajarKelasKuliah.getJSONObject(i);

			Session session = HibernateUtil.currentNativeSession();
			try {
				if (id_aktivitas_mengajar.isNull("id_dosen")) {
					continue;
				}
				Dosen dosen = FeederUtil.getDataByFeeder(session, id_aktivitas_mengajar.getString("id_dosen").trim(),
						Dosen.class);
				if (dosen == null) {
					continue;
				}

				Perkuliahan perkuliahan = FeederUtil.getDataByFeeder(session, id_kls, Perkuliahan.class);
				if (perkuliahan == null) {
					continue;
				}

				if (!perkuliahan.populateDosenBuId().contains(dosen.getId())) {

					if (perkuliahan.getDosen1() == null) {
						perkuliahan.setDosen1(dosen);
					} else if (perkuliahan.getDosen2() == null) {
						perkuliahan.setDosen2(dosen);
					} else if (perkuliahan.getDosen3() == null) {
						perkuliahan.setDosen3(dosen);
					} else if (perkuliahan.getDosen4() == null) {
						perkuliahan.setDosen4(dosen);
					} else if (perkuliahan.getDosen5() == null) {
						perkuliahan.setDosen5(dosen);
					} else if (perkuliahan.getDosen6() == null) {
						perkuliahan.setDosen6(dosen);
					} else if (perkuliahan.getDosen7() == null) {
						perkuliahan.setDosen7(dosen);
					} else if (perkuliahan.getDosen8() == null) {
						perkuliahan.setDosen8(dosen);
					} else if (perkuliahan.getDosen9() == null) {
						perkuliahan.setDosen9(dosen);
					} else if (perkuliahan.getDosen10() == null) {
						perkuliahan.setDosen10(dosen);
					}

					if (session == null || !session.isOpen()) {
						session = HibernateUtil.currentNativeSession();
					}
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, perkuliahan);
					session.getTransaction().commit();
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			} finally {
				ais.common.Common.closeOpenedSession(session);
			}
		}
	}

	/**
	 * Impor peserta (mahasiswa/KRS) sebuah kelas kuliah dari feeder (GetPesertaKelasKuliah). Tiap
	 * peserta di-import lewat {@link FeederJSONImport#detailperkuliahan} yang membuat Detailperkuliahan
	 * (mengaitkan mahasiswa ke perkuliahan) selama mahasiswa &amp; kelasnya sudah ada di sistem lokal.
	 */
	public static void ambilPesertaKelasDariFeeder(FeederConnector feederConnector, String token, String id_kls,
			Tbmuser tbmuser) throws Exception {
		String filter = "id_kelas_kuliah='" + id_kls + "'";
		JSONArray dataPeserta = feederConnector.getData("GetPesertaKelasKuliah", token, filter, "", "10000", "");

		for (int i = 0; i < dataPeserta.length(); i++) {
			JSONObject peserta = dataPeserta.getJSONObject(i);
			try {
				// Pastikan id_kelas_kuliah tersedia agar detailperkuliahan bisa menautkan ke Perkuliahan.
				if (peserta.isNull("id_kelas_kuliah")) {
					peserta.put("id_kelas_kuliah", id_kls);
				}
				FeederJSONImport.detailperkuliahan(peserta, tbmuser, "Import Peserta -> ");
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	public static void ambilNilaiDariFeeder(FeederConnector feederConnector, String token, int index,
			Mahasiswa mahasiswa, Tbmuser tbmuser, String smtTa) throws Exception {
		ambilNilaiDariFeeder(feederConnector, token, index, mahasiswa, tbmuser, smtTa, null);
	}

	public static void ambilNilaiDariFeeder(FeederConnector feederConnector, String token, int index,
			final Mahasiswa mahasiswa, Tbmuser tbmuser, String smtTa, Perkuliahan perkuliahanParent) throws Exception {
		if (mahasiswa.getIdRegPd() != null && !mahasiswa.getIdRegPd().trim().isEmpty()) {

			String filter = "id_registrasi_mahasiswa='" + mahasiswa.getIdRegPd() + "'";

			JSONArray dataNilaiTransferPendidikanMahasiswa = feederConnector
					.getData("GetNilaiTransferPendidikanMahasiswa", token, filter, "", "1000", "");
			System.out
					.println("dataNilaiTransferPendidikanMahasiswa = " + dataNilaiTransferPendidikanMahasiswa.length());
			for (int i = 0; i < dataNilaiTransferPendidikanMahasiswa.length(); i++) {
				JSONObject nilai = dataNilaiTransferPendidikanMahasiswa.getJSONObject(i);
				FeederJSONImport.detailperkuliahan(nilai, tbmuser, "Import -> ");

			}

			filter = (perkuliahanParent == null ? "" : "id_kelas='" + perkuliahanParent.getFeeder() + "' AND ")
					+ "id_registrasi_mahasiswa='" + mahasiswa.getIdRegPd() + "'";

			if (smtTa != null && !smtTa.trim().isEmpty()) {
				filter += " AND id_periode = '" + smtTa + "'";
			}

			JSONArray dataListNilaiPerkuliahanKelas = feederConnector.getData("GetRiwayatNilaiMahasiswa", token, filter,
					"", "1000", "");

			if (smtTa != null && !smtTa.trim().isEmpty()) {
				System.out.println(
						"dataListNilaiPerkuliahanKelas = " + dataListNilaiPerkuliahanKelas + " filter = " + filter);
			} else {
				System.out.println("dataListNilaiPerkuliahanKelas = " + dataListNilaiPerkuliahanKelas.length()
						+ " filter = " + filter);
			}

			for (int j = 0; j < dataListNilaiPerkuliahanKelas.length(); j++) {
				JSONObject nilai = dataListNilaiPerkuliahanKelas.getJSONObject(j);

				String id_kelas = nilai.getString("id_kelas").trim();
				if (perkuliahanParent == null) {
					Integer semesterPendek = null;
					try {

						if (!nilai.isNull("id_semester")) {
							String idSmt = nilai.getString("id_semester");
							Integer s = Integer.parseInt(idSmt.substring(4, 5));
							if (s.equals(3)) {
								semesterPendek = Perkuliahan.SEMESTER_PENDEK;
							}
						}

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/MahasiswaAction.java:3455");
						// TODO: handle exception
					}

					try {

						if (!nilai.isNull("id_periode")) {
							String idSmt = nilai.getString("id_periode");
							Integer s = Integer.parseInt(idSmt.substring(4, 5));
							if (s.equals(3)) {
								semesterPendek = Perkuliahan.SEMESTER_PENDEK;
							}
						}

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/MahasiswaAction.java:3469");
						// TODO: handle exception
					}

					Session session = HibernateUtil.currentNativeSession();
					Perkuliahan p = FeederUtil.getDataByFeeder(session, id_kelas, Perkuliahan.class);
					if (p != null && p.getStatusSemesterPendek() == null && semesterPendek != null) {
						p.setStatusSemesterPendek(semesterPendek);
						session.getTransaction().begin();
						session.update(p);
						session.getTransaction().commit();
					}
					System.out.println(mahasiswa + " perkuliahan = " + p + " semesterPendek " + semesterPendek);
					// session.disconnect();
					ais.common.Common.closeOpenedSession(session);
					if (p == null) {
						MahasiswaAction.ambilPerkuliahanDariFeeder(feederConnector, token, id_kelas);
					}

					filter = "id_kelas_kuliah='" + id_kelas + "'";
					JSONArray dataDosenPengajarKelasKuliah = feederConnector.getData("GetDosenPengajarKelasKuliah",
							token, filter, "", "1000", "");

					for (int i = 0; i < dataDosenPengajarKelasKuliah.length(); i++) {
						JSONObject id_aktivitas_mengajar = dataDosenPengajarKelasKuliah.getJSONObject(i);

						System.out.println(" id_aktivitas_mengajar = " + id_aktivitas_mengajar);

						session = HibernateUtil.currentNativeSession();
						Dosen dosen = FeederUtil.getDataByFeeder(session,
								id_aktivitas_mengajar.getString("id_dosen").trim(), Dosen.class);
						if (dosen == null) {
							// session.disconnect();
							ais.common.Common.closeOpenedSession(session);
							continue;
						}

						Perkuliahan perkuliahan = FeederUtil.getDataByFeeder(session, id_kelas, Perkuliahan.class);
						if (perkuliahan == null) {
							// session.disconnect();
							ais.common.Common.closeOpenedSession(session);
							continue;
						}

						if (!perkuliahan.populateDosenBuId().contains(dosen.getId())) {

							if (perkuliahan.getDosen1() == null) {
								perkuliahan.setDosen1(dosen);
							} else if (perkuliahan.getDosen2() == null) {
								perkuliahan.setDosen2(dosen);
							} else if (perkuliahan.getDosen3() == null) {
								perkuliahan.setDosen3(dosen);
							} else if (perkuliahan.getDosen4() == null) {
								perkuliahan.setDosen4(dosen);
							} else if (perkuliahan.getDosen5() == null) {
								perkuliahan.setDosen5(dosen);
							} else if (perkuliahan.getDosen6() == null) {
								perkuliahan.setDosen6(dosen);
							} else if (perkuliahan.getDosen7() == null) {
								perkuliahan.setDosen7(dosen);
							} else if (perkuliahan.getDosen8() == null) {
								perkuliahan.setDosen8(dosen);
							} else if (perkuliahan.getDosen9() == null) {
								perkuliahan.setDosen9(dosen);
							} else if (perkuliahan.getDosen10() == null) {
								perkuliahan.setDosen10(dosen);
							}

							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, perkuliahan);
							session.getTransaction().commit();
						}
						// session.disconnect();
						ais.common.Common.closeOpenedSession(session);
					}

				}

				FeederJSONImport.detailperkuliahan(nilai, tbmuser, "Import -> ");

			}

			new Thread(new Runnable() {

				@Override
				public void run() {
					mahasiswa.reInit();
					for (int smt = 1; smt <= mahasiswa.currentSemester(); smt++) {
						Common.singkronkanKrsMahasiswa(mahasiswa, smt, null, null, true);
					}
				}
			}).start();
		}
	}

	public static List<String> getColumnAdding(boolean alumni) {
		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("DOSEN PA");
		columnHeadersAdding.add("KELAS");

		columnHeadersAdding.add("Nomor KTP");
		columnHeadersAdding.add("Nama Ayah");
		columnHeadersAdding.add("Tanggal Lahir Ayah".toUpperCase());
		columnHeadersAdding.add("Jenis Pekerjaan Ayah".toUpperCase());
		columnHeadersAdding.add("Rata-rata penghasilan ayah".toUpperCase());
		columnHeadersAdding.add("Jenjang Pendidikan Ayah".toUpperCase());
		columnHeadersAdding.add("Nama Ibu");
		columnHeadersAdding.add("Tanggal Lahir Ibu".toUpperCase());
		columnHeadersAdding.add("Jenis Pekerjaan Ibu".toUpperCase());
		columnHeadersAdding.add("Rata-rata penghasilan Ibu".toUpperCase());
		columnHeadersAdding.add("Jenjang Pendidikan Ibu".toUpperCase());
		columnHeadersAdding.add("Telp. Rumah".toUpperCase());
		columnHeadersAdding.add("HP".toUpperCase());

		columnHeadersAdding.add("Status".toUpperCase());

		columnHeadersAdding.add("Nomor KTP Ayah".toUpperCase());
		columnHeadersAdding.add("Nomor KTP Ibu".toUpperCase());

		columnHeadersAdding.add("Nomor HP/Telp Ayah".toUpperCase());
		columnHeadersAdding.add("Nomor HP/Telp Ibu".toUpperCase());

		columnHeadersAdding.add("NPSN (Nomor Pokok Sekolah Nasional)");
		columnHeadersAdding.add("Asal Pendidikan");
		columnHeadersAdding.add("Alamat Pendidikan Sebelumnya");
		columnHeadersAdding.add("NPWP");
		columnHeadersAdding.add("No. KK");
		columnHeadersAdding.add("RT");
		columnHeadersAdding.add("RW");
		columnHeadersAdding.add("Kode Pos");
		columnHeadersAdding.add("Dusun/Kampung");
		columnHeadersAdding.add("Desa/Kelurahan");
		columnHeadersAdding.add("Kode Kecamatan".toUpperCase());
		columnHeadersAdding.add("Kecamatan".toUpperCase());
		columnHeadersAdding.add("Kota/Kabupaten".toUpperCase());
		columnHeadersAdding.add("Propinsi".toUpperCase());
		columnHeadersAdding.add("Kewarganegaraan".toUpperCase());
		columnHeadersAdding.add("Negara".toUpperCase());

		columnHeadersAdding.add("Status Perkawinan".toUpperCase());
		columnHeadersAdding.add("Jenis Tinggal Mahasiswa".toUpperCase());
		columnHeadersAdding.add("Alat Transportasi Mahasiswa".toUpperCase());

		columnHeadersAdding.add("Ukuran Jaket");
		columnHeadersAdding.add("Tinggi Badan");
		columnHeadersAdding.add("Berat Badan");

		columnHeadersAdding.add("NIRM");
		columnHeadersAdding.add("NISN");
		columnHeadersAdding.add("Nama Sesuai Ijazah");
		columnHeadersAdding.add("Operator HP");

		columnHeadersAdding.add("Foto");
		columnHeadersAdding.add(LampiranLainMahasiswa.IJAZAH);
		columnHeadersAdding.add(LampiranLainMahasiswa.TRANSKRIP_NILAI);
		columnHeadersAdding.add(LampiranLainMahasiswa.KTP);
		columnHeadersAdding.add(LampiranLainMahasiswa.LAMPIRAN_1);
		columnHeadersAdding.add(LampiranLainMahasiswa.LAMPIRAN_2);
		columnHeadersAdding.add(LampiranLainMahasiswa.LAMPIRAN_3);
		columnHeadersAdding.add(LampiranLainMahasiswa.LAMPIRAN_4);
		columnHeadersAdding.add(LampiranLainMahasiswa.LAMPIRAN_5);
		columnHeadersAdding.add(LampiranLainMahasiswa.LAMPIRAN_6);

		columnHeadersAdding.add(LampiranLainMahasiswa.NPWP);
		columnHeadersAdding.add(LampiranLainMahasiswa.SURAT_PENUNJUKAN_PENGURUS_ORGANISASI);
		columnHeadersAdding.add(LampiranLainMahasiswa.KK);
		columnHeadersAdding.add(LampiranLainMahasiswa.KTP_AYAH);
		columnHeadersAdding.add(LampiranLainMahasiswa.KTP_IBU);
		columnHeadersAdding.add(LampiranLainMahasiswa.KTP_WALI);
		columnHeadersAdding.add(LampiranLainMahasiswa.AKTE);

		columnHeadersAdding.add("SEMESTER");
		columnHeadersAdding.add("TAHAP");
		columnHeadersAdding.add("IPS");
		columnHeadersAdding.add("IPK");
		columnHeadersAdding.add("SKS");
		columnHeadersAdding.add("SKSK");
		// columnHeadersAdding.add("MK DINILAI");
		// columnHeadersAdding.add("MK BELUM DINILAI");
		// columnHeadersAdding.add("TOTAL MK");
		// columnHeadersAdding.add("MK KUMULATIF DINILAI");
		// columnHeadersAdding.add("MK KUMULATIF BELUM DINILAI");
		// columnHeadersAdding.add("TOTAL MK KUMULATIF");
		// columnHeadersAdding.add("DATA MK BELUM DINILAI");
		// columnHeadersAdding.add("DATA MK TELAH DINILAI");

		columnHeadersAdding.add("Jumlah Login");
		columnHeadersAdding.add("Sejarah Login");

		columnHeadersAdding.add("Masa Studi Tahun");
		columnHeadersAdding.add("Masa Studi Bulan");
		columnHeadersAdding.add("Masa Studi Hari");
		columnHeadersAdding.add("Masa Studi Deskripsi");

		columnHeadersAdding.add("No. Registrasi Calon Mhs");
		columnHeadersAdding.add("No. Ujian Calon Mhs");
		columnHeadersAdding.add("Gelombang Calon Mhs");
		columnHeadersAdding.add("Jenis Seleksi Calon Mhs");
		columnHeadersAdding.add("Paket Calon Mhs");

		if (alumni) {
			columnHeadersAdding.add("Link Alumni");
			columnHeadersAdding.add("Link Pengguna Alumni");
		} else {
			columnHeadersAdding.add("Link Mahasiswa");
		}

		return columnHeadersAdding;
	}

	public static class DataAddingMahasiswa implements EventListener {

		private Boolean alumni;
		private String[] contents;

		public DataAddingMahasiswa(Boolean alumni, String[] contents) {
			this.alumni = alumni;
			this.contents = contents;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {
			Object[] objects = (Object[]) arg0.getData();
			Skripsi skripsi = null;
			if (objects[0] instanceof Skripsi) {
				skripsi = (Skripsi) objects[0];
			}
			Mahasiswa mahasiswa = (Mahasiswa) (skripsi == null ? objects[0] : skripsi.getMahasiswa());
			StatusMahasiswa statusMahasiswa = skripsi == null
					? ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa()
					: ais.action.master.helper.HistoryStatusMahasiswaUtil
							.currentStatus(mahasiswa, skripsi.getTahunAkademik(), skripsi.getSemester())
							.getStatusMahasiswa();
			// Long id = (Long) objects[1];
			XSSFRow row = (XSSFRow) objects[2];
			XSSFWorkbook workbook = (XSSFWorkbook) objects[3];

			XSSFFont hlink_font = workbook.createFont();
			hlink_font.setUnderline(XSSFFont.U_SINGLE);
			hlink_font.setColor(new XSSFColor(Color.BLUE));

			final XSSFCellStyle hlink_style = workbook.createCellStyle();
			hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
			hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
			hlink_style.setFont(hlink_font);

			XSSFRow rowTambahan = (XSSFRow) objects[4];
			XSSFRow rowheadTambahan = (XSSFRow) objects[5];

			BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
			BiodataCalonMahasiswa biodataCalonMahasiswa = mahasiswa.getBiodataCalonMahasiswaData();

			if (rowTambahan != null) {
				rowTambahan.createCell(0).setCellValue(mahasiswa.getId());
				rowTambahan.createCell(1).setCellValue(mahasiswa.getNim());
				rowTambahan.createCell(2).setCellValue(biodataMahasiswa.getNisn());
				rowTambahan.createCell(3).setCellValue(biodataMahasiswa.getNpwp());
				rowTambahan.createCell(4).setCellValue(biodataMahasiswa.getNoIdentitas());
				rowTambahan.createCell(5).setCellValue(mahasiswa.getNama());

				int j = 0;
				if (!alumni) {
					for (CommonVO commonVO : biodataMahasiswa.ambilDataParameterTambahan()) {
						int indexCol = j + 6;
						j++;
						String lbl = commonVO.getName();
						String url = commonVO.getName2();
						String val = commonVO.getName1();

						if (rowheadTambahan != null) {
							XSSFCell hssfCell = rowheadTambahan.getCell(indexCol);
							if (hssfCell == null) {
								rowheadTambahan.createCell(indexCol).setCellValue(lbl);
							}
						}

						XSSFCell cellTambahan = rowTambahan.createCell(indexCol);
						cellTambahan.setCellValue(val);
						if (url != null && !url.trim().isEmpty()) {
							cellTambahan.setCellStyle(hlink_style);
							XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
									.createHyperlink(Hyperlink.LINK_URL);
							link.setAddress(url);
							cellTambahan.setHyperlink(link);
						}
					}
				}

				for (CommonVO commonVO : biodataMahasiswa.ambilDataParameterTambahanAlumni()) {
					int indexCol = j + 6;
					j++;
					String lbl = commonVO.getName();
					String url = commonVO.getName2();
					String val = commonVO.getName1();

					if (rowheadTambahan != null) {
						XSSFCell hssfCell = rowheadTambahan.getCell(indexCol);
						if (hssfCell == null) {
							rowheadTambahan.createCell(indexCol).setCellValue(lbl);
						}
					}

					XSSFCell cellTambahan = rowTambahan.createCell(indexCol);
					cellTambahan.setCellValue(val);
					if (url != null && !url.trim().isEmpty()) {
						cellTambahan.setCellStyle(hlink_style);
						XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
								.createHyperlink(Hyperlink.LINK_URL);
						link.setAddress(url);
						cellTambahan.setHyperlink(link);
					}
				}
			}

			KrsMahasiswa krsMahasiswa = skripsi == null ? Common.singkronkanKrsMahasiswa(mahasiswa)
					: Common.singkronkanKrsMahasiswa(mahasiswa, skripsi.getSemester(), null, null);

			row.createCell(contents.length)
					.setCellValue(krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNama());

			row.createCell(contents.length + 1).setCellValue(krsMahasiswa.getKelas());

			row.createCell(contents.length + 2)
					.setCellValue(biodataMahasiswa == null ? "" : biodataMahasiswa.getNoIdentitas());
			row.createCell(contents.length + 3)
					.setCellValue(biodataMahasiswa == null ? "" : biodataMahasiswa.getNamaAyah());
			row.createCell(contents.length + 4)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getTanggalLahirAyah() == null ? ""
							: Common.dateFormat1.get().format(biodataMahasiswa.getTanggalLahirAyah()));
			row.createCell(contents.length + 5)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getJenisPekerjaanAyah() == null ? ""
							: biodataMahasiswa.getJenisPekerjaanAyah().getNama());

			row.createCell(contents.length + 6)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getJenisPenghasilanAyah() == null ? ""
							: biodataMahasiswa.getJenisPenghasilanAyah().getNama());
			row.createCell(contents.length + 7)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getJenjangPendidikanAyah() == null ? ""
							: biodataMahasiswa.getJenjangPendidikanAyah().getNama());

			row.createCell(contents.length + 8)
					.setCellValue(biodataMahasiswa == null ? "" : biodataMahasiswa.getNamaIbu());
			row.createCell(contents.length + 9)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getTanggalLahirIbu() == null ? ""
							: Common.dateFormat1.get().format(biodataMahasiswa.getTanggalLahirIbu()));
			row.createCell(contents.length + 10)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getJenisPekerjaanIbu() == null ? ""
							: biodataMahasiswa.getJenisPekerjaanIbu().getNama());

			row.createCell(contents.length + 11)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getJenisPenghasilanIbu() == null ? ""
							: biodataMahasiswa.getJenisPenghasilanIbu().getNama());
			row.createCell(contents.length + 12)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getJenjangPendidikanIbu() == null ? ""
							: biodataMahasiswa.getJenjangPendidikanIbu().getNama());

			row.createCell(contents.length + 13)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getTeleponRumah() == null ? ""
							: biodataMahasiswa.getTeleponRumah());

			row.createCell(contents.length + 14).setCellValue(
					biodataMahasiswa == null || biodataMahasiswa.getHp() == null ? "" : biodataMahasiswa.getHp());

			row.createCell(contents.length + 15).setCellValue(statusMahasiswa == null ? "" : statusMahasiswa.getNama());

			row.createCell(contents.length + 16)
					.setCellValue(biodataMahasiswa == null ? "" : biodataMahasiswa.getNikAyah());

			row.createCell(contents.length + 17)
					.setCellValue(biodataMahasiswa == null ? "" : biodataMahasiswa.getNikIbu());

			row.createCell(contents.length + 18)
					.setCellValue(biodataMahasiswa == null ? "" : biodataMahasiswa.getTelpAyah());

			row.createCell(contents.length + 19)
					.setCellValue(biodataMahasiswa == null ? "" : biodataMahasiswa.getTelpIbu());

			row.createCell(contents.length + 20).setCellValue(
					biodataMahasiswa == null || biodataMahasiswa.getNpsn() == null ? "" : biodataMahasiswa.getNpsn());

			row.createCell(contents.length + 21)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getAsalSma() == null ? ""
							: biodataMahasiswa.getAsalSma());
			row.createCell(contents.length + 22)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getAlamatAsalSma() == null ? ""
							: biodataMahasiswa.getAlamatAsalSma());

			row.createCell(contents.length + 23).setCellValue(
					biodataMahasiswa == null || biodataMahasiswa.getNpwp() == null ? "" : biodataMahasiswa.getNpwp());

			row.createCell(contents.length + 24).setCellValue(
					biodataMahasiswa == null || biodataMahasiswa.getNoKK() == null ? "" : biodataMahasiswa.getNoKK());

			row.createCell(contents.length + 25).setCellValue(
					biodataMahasiswa == null || biodataMahasiswa.getRt() == null ? "" : biodataMahasiswa.getRt());

			row.createCell(contents.length + 26).setCellValue(
					biodataMahasiswa == null || biodataMahasiswa.getRw() == null ? "" : biodataMahasiswa.getRw());

			row.createCell(contents.length + 27)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getKodepos() == null ? ""
							: biodataMahasiswa.getKodepos());

			row.createCell(contents.length + 28).setCellValue(
					biodataMahasiswa == null || biodataMahasiswa.getDusun() == null ? "" : biodataMahasiswa.getDusun());

			row.createCell(contents.length + 29)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getKelurahan() == null ? ""
							: biodataMahasiswa.getKelurahan());

			row.createCell(contents.length + 30)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getKecamatan() == null ? ""
							: (biodataMahasiswa.getKecamatan().getKode()));

			row.createCell(contents.length + 31)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getKecamatan() == null ? ""
							: biodataMahasiswa.getKecamatan().getNama());

			row.createCell(contents.length + 32)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getKota() == null ? ""
							: biodataMahasiswa.getKota().getNama());

			row.createCell(contents.length + 33)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getPropinsi() == null ? ""
							: biodataMahasiswa.getPropinsi().getNama());

			row.createCell(contents.length + 34).setCellValue(
					mahasiswa == null || mahasiswa.getWarganegara() == null ? "" : mahasiswa.getWarganegara());

			row.createCell(contents.length + 35).setCellValue(
					mahasiswa == null || mahasiswa.getNegara() == null ? "" : mahasiswa.getNegara().getNamaNegara());

			row.createCell(contents.length + 36)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getStatusNikah() == null ? ""
							: biodataMahasiswa.getStatusNikah().equals(0) ? "Belum Nikah"
									: biodataMahasiswa.getStatusNikah().equals(1) ? "Nikah"
											: biodataMahasiswa.getStatusNikah().equals(2) ? "Janda"
													: biodataMahasiswa.getStatusNikah().equals(3) ? "Duda" : "");

			row.createCell(contents.length + 37)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getJenisTinggalMahasiswa() == null ? ""
							: biodataMahasiswa.getJenisTinggalMahasiswa().getNama());

			row.createCell(contents.length + 38).setCellValue(
					biodataMahasiswa == null || biodataMahasiswa.getAlatTransportasiMahasiswa() == null ? ""
							: biodataMahasiswa.getAlatTransportasiMahasiswa().getNama());

			row.createCell(contents.length + 39)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getUkuranJaket() == null ? ""
							: biodataMahasiswa.getUkuranJaket());

			row.createCell(contents.length + 40)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getTinggiBadan() == null ? ""
							: biodataMahasiswa.getTinggiBadan().toString());

			row.createCell(contents.length + 41)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getBeratBadan() == null ? ""
							: biodataMahasiswa.getBeratBadan().toString());

			row.createCell(contents.length + 42).setCellValue(
					biodataMahasiswa == null || biodataMahasiswa.getNirm() == null ? "" : biodataMahasiswa.getNirm());

			row.createCell(contents.length + 43).setCellValue(
					biodataMahasiswa == null || biodataMahasiswa.getNisn() == null ? "" : biodataMahasiswa.getNisn());

			row.createCell(contents.length + 44)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getNamaUntukIjazah() == null ? ""
							: biodataMahasiswa.getNamaUntukIjazah());

			row.createCell(contents.length + 45)
					.setCellValue(biodataMahasiswa == null || biodataMahasiswa.getHpProvider() == null ? ""
							: biodataMahasiswa.getHpProvider());

			class DataAddingHelper {
				public void process(XSSFRow row, int index, Mahasiswa mahasiswa, String jenis) throws Exception {
					Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

					Long ids = (Long) (streamingSession.createCriteria(LampiranLainMahasiswa.class)
							.setProjection(Projections.property("id")).add(Restrictions.eq("jenis", jenis))
							.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult());

					XSSFCell cell = row.createCell(index);

					if (ids != null) {

//						String nama = (String) (streamingSession.createCriteria(LampiranLainMahasiswa.class)
//								.setProjection(Projections.property("nama")).add(Restrictions.eq("jenis", jenis))
//								.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult());

						cell.setCellStyle(hlink_style);
						cell.setCellValue(jenis);
						String url = CommonMedia.getFile(ids, LampiranLainMahasiswa.class.getName());
						XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
								.createHyperlink(Hyperlink.LINK_URL);
						link.setAddress(url);
						cell.setHyperlink(link);
					}

					StreamingHibernateUtil.getInstance().closeSession();
				}
			}
			XSSFCell cell = row.createCell(contents.length + 46);

			try {
				Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

				FotoMahasiswa fotomahasiswa = (FotoMahasiswa) streamingSession.createCriteria(FotoMahasiswa.class)
						.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult();

				if (fotomahasiswa != null) {
					cell.setCellStyle(hlink_style);
					cell.setCellValue("Foto");
					String url = fotomahasiswa.getGdrive() != null ? fotomahasiswa.forwardGDriveUrl()
							: CommonMedia.getFile(fotomahasiswa.getId(), FotoMahasiswa.class.getName());
					XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
							.createHyperlink(Hyperlink.LINK_URL);
					link.setAddress(url);
					cell.setHyperlink(link);

				}

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e1) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/MahasiswaAction.java:3993");
			}

			DataAddingHelper dataAddingHelper = new DataAddingHelper();

			dataAddingHelper.process(row, contents.length + 47, mahasiswa, LampiranLainMahasiswa.IJAZAH);
			dataAddingHelper.process(row, contents.length + 48, mahasiswa, LampiranLainMahasiswa.TRANSKRIP_NILAI);
			dataAddingHelper.process(row, contents.length + 49, mahasiswa, LampiranLainMahasiswa.KTP);
			dataAddingHelper.process(row, contents.length + 50, mahasiswa, LampiranLainMahasiswa.LAMPIRAN_1);
			dataAddingHelper.process(row, contents.length + 51, mahasiswa, LampiranLainMahasiswa.LAMPIRAN_2);
			dataAddingHelper.process(row, contents.length + 52, mahasiswa, LampiranLainMahasiswa.LAMPIRAN_3);
			dataAddingHelper.process(row, contents.length + 53, mahasiswa, LampiranLainMahasiswa.LAMPIRAN_4);
			dataAddingHelper.process(row, contents.length + 54, mahasiswa, LampiranLainMahasiswa.LAMPIRAN_5);
			dataAddingHelper.process(row, contents.length + 55, mahasiswa, LampiranLainMahasiswa.LAMPIRAN_6);

			dataAddingHelper.process(row, contents.length + 56, mahasiswa, LampiranLainMahasiswa.NPWP);
			dataAddingHelper.process(row, contents.length + 57, mahasiswa,
					LampiranLainMahasiswa.SURAT_PENUNJUKAN_PENGURUS_ORGANISASI);
			dataAddingHelper.process(row, contents.length + 58, mahasiswa, LampiranLainMahasiswa.KK);
			dataAddingHelper.process(row, contents.length + 59, mahasiswa, LampiranLainMahasiswa.KTP_AYAH);
			dataAddingHelper.process(row, contents.length + 60, mahasiswa, LampiranLainMahasiswa.KTP_IBU);
			dataAddingHelper.process(row, contents.length + 61, mahasiswa, LampiranLainMahasiswa.KTP_WALI);
			dataAddingHelper.process(row, contents.length + 62, mahasiswa, LampiranLainMahasiswa.AKTE);

			row.createCell(contents.length + 63)
					.setCellValue(skripsi == null ? mahasiswa.currentSemester() : skripsi.getSemester());
			row.createCell(contents.length + 64).setCellValue(skripsi == null ? mahasiswa.currentTahapan() : 0);

			cell = row.createCell(contents.length + 65);
			cell.setCellValue(krsMahasiswa.getIps());

			cell = row.createCell(contents.length + 66);
			cell.setCellValue(krsMahasiswa.getIpk());

			cell = row.createCell(contents.length + 67);
			cell.setCellValue(krsMahasiswa.getSksYangDiambil());

			cell = row.createCell(contents.length + 68);
			cell.setCellValue(krsMahasiswa.getSksk());
//
//			List<Date> logLogins = session.createCriteria(LogLogin.class).add(Restrictions.eq("mahasiswa", mahasiswa))
//					.add(Restrictions.isNotNull("login")).addOrder(Order.desc("id"))
//					.setProjection(Projections.property("login")).setMaxResults(100).list();

//			String lgn = "";
//			for (Date d : logLogins) {
//				lgn += lgn.trim().isEmpty() ? Common.dateFormat3.get().format(d) : "," + Common.dateFormat3.get().format(d);
//			}

//			row.createCell(contents.length + 69).setCellValue(logLogins.size());

//			row.createCell(contents.length + 70).setCellValue(lgn);

			Jurusan jurusan = mahasiswa.getJurusan();

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			String ActualDate = Common.databaseDateFormat.get().format(mahasiswa.getTanggalKegiatanBelajarMengajar());
			java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
			java.time.LocalDate currentdate = mahasiswa.getTanggalLulus() == null ? java.time.LocalDate.now()
					: java.time.LocalDate.parse(Common.databaseDateFormat.get().format(mahasiswa.getTanggalLulus()));
			Period period = Period.between(dt, currentdate);
			System.out.println("Years " + period.getYears()); // Years 2
			System.out.println("Months " + period.getMonths()); // Months 1
			System.out.println("Days " + period.getDays()); // Days 11

			int batasSemester = (jurusan != null && jurusan.getJenjang() != null
					&& jurusan.getJenjang().getJumlahSemesterMaksimal() != null
							? jurusan.getJenjang().getJumlahSemesterMaksimal()
							: 0);

			Calendar calendarMasaAwal = ais.ui.util.WaktuUtil.getCalendar();
			calendarMasaAwal.setTime(mahasiswa.getTanggalKegiatanBelajarMengajar());

			Calendar calendarMasaAkhir = ais.ui.util.WaktuUtil.getCalendar();
			calendarMasaAkhir.set(Calendar.DATE, calendarMasaAwal.get(Calendar.DATE) + (178 * batasSemester));

			ActualDate = Common.databaseDateFormat.get().format(calendarMasaAkhir.getTime());
			dt = java.time.LocalDate.parse(ActualDate, formatter);
			currentdate = java.time.LocalDate.now();

			row.createCell(contents.length + 71).setCellValue(period.getYears());

			row.createCell(contents.length + 72).setCellValue(period.getMonths());
			row.createCell(contents.length + 73).setCellValue(period.getDays());
			row.createCell(contents.length + 74).setCellValue(mahasiswa.ambilMasaStudi());

			row.createCell(contents.length + 75)
					.setCellValue(biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getNoRegistrasi());
			row.createCell(contents.length + 76)
					.setCellValue(biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getNoUjian());
			row.createCell(contents.length + 77).setCellValue(
					biodataCalonMahasiswa == null || biodataCalonMahasiswa.getGelombangPendaftaran() == null ? ""
							: biodataCalonMahasiswa.getGelombangPendaftaran().getNama());
			row.createCell(contents.length + 78)
					.setCellValue(biodataCalonMahasiswa == null || biodataCalonMahasiswa.getJenisSeleksi() == null ? ""
							: biodataCalonMahasiswa.getJenisSeleksi().getNama());
			row.createCell(contents.length + 79)
					.setCellValue(biodataCalonMahasiswa == null || biodataCalonMahasiswa.getPaket() == null ? ""
							: biodataCalonMahasiswa.getPaket().getNama());

			if (alumni) {
				String code = Common.getRequestHostWithProtocol() + "/m?q=" + URLEncoder.encode(
						Common.desEncrypter.get().encrypt(mahasiswa.getId() + "-Alumni-abcdefghijklmnopqrstuvwxyz"), "UTF-8");
				row.createCell(contents.length + 80).setCellValue(code);
				code = Common.getRequestHostWithProtocol() + "/m?q=" + URLEncoder.encode(
						Common.desEncrypter.get().encrypt(mahasiswa.getId() + "-PenggunaLulusan-abcdefghijklmnopqrstuvwxyz"),
						"UTF-8");
				row.createCell(contents.length + 81).setCellValue(code);
			} else {
				String code = Common.getRequestHostWithProtocol() + "/m?q=" + URLEncoder.encode(
						Common.desEncrypter.get().encrypt(mahasiswa.getId() + "-Mahasiswa-abcdefghijklmnopqrstuvwxyz"),
						"UTF-8");
				row.createCell(contents.length + 80).setCellValue(code);
			}

			HibernateUtil.closeSession();
		}
	}

	public static void createUploadDanDownloadData(Component parent, final EventListener eventListener,
			DataCriteria dataCriteria, final boolean alumni, boolean downloadJuga) {

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser == null || tbmuser.getUserId() == null) {
			return;
		}

		List<String> columnHeadersAdding = MahasiswaAction.getColumnAdding(alumni);
		MahasiswaAction.DataAddingMahasiswa dataAdding = new MahasiswaAction.DataAddingMahasiswa(alumni, contents);

		List<String> columnHeadersAddingTambahan = new ArrayList<String>();
		columnHeadersAddingTambahan.add("ID");
		columnHeadersAddingTambahan.add("NIM");
		columnHeadersAddingTambahan.add("NISN");
		columnHeadersAddingTambahan.add("NPWP");
		columnHeadersAddingTambahan.add("NIK");
		columnHeadersAddingTambahan.add("Nama");

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Mahasiswa.class, dataCriteria,
				"Download " + (alumni ? "Alumni" : "Mahasiswa"), "/img/print.png", columnHeadersAdding, dataAdding,
				true, columnHeadersAddingTambahan, (alumni ? "Data Tracer Alumni" : "Data Lain"), contents);
		parent.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig(
				"Upload " + (alumni ? "Alumni" : "Mahasiswa") + Common.ukuranLabelFileUpload(), "/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;
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
							MahasiswaAction.uploadDataMahasiswa(file, eventListener, contents);
						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, berkas yang Bapak/Ibu unggah (\"{V1}\") harus berformat Excel Open XML Spreadsheet (xlsx). Langkah yang dapat dilakukan: (1) buka berkas Excel tersebut melalui aplikasi Microsoft Excel; (2) pilih menu Save As, kemudian pilih jenis berkas Excel Open XML Spreadsheet (xlsx); (3) simpan berkas dan silakan unggah kembali berkas dengan format tersebut.",
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR, media);
				}
			}
		});
		if (downloadJuga && Common.bolehKonfigurasi("aktifkan_upload_data_mahasiswa") && Common.getApakahAdmin()
				&& Common.bolehUploadDataKonfigurasi("hak_akses_upload_data_mahasiswa")) {
			parent.appendChild(upload);
		}
	}

	public static void updateUser(Session ojSession, Mahasiswa mahasiswa, List<Journals> journals) {

		String username = mahasiswa.getUsernameOjs();
		if (username == null) {
			return;
		}
		String email = mahasiswa.getEmail().split(",")[0].trim();
		if (email == null || email.trim().isEmpty()) {
			email = username + "@email.com";
		}
		Users users = (Users) ojSession.createCriteria(Users.class).add(Restrictions.eq("username", username))
				.setMaxResults(1).uniqueResult();
		if (users == null) {
			users = new Users();
			users.setFirstName(Common.maxPanjang(mahasiswa.getNama(), 40));
			users.setUsername(username);

			int count = ((Number) ojSession.createCriteria(Users.class).setProjection(Projections.rowCount())
					.add(Restrictions.eq("email", email)).uniqueResult()).intValue();
			if (count > 0) {
				users.setEmail((++Common.increments) + "_" + email);
			} else {
				users.setEmail(email);
			}

		}

		try {
			String password = Common.desEncrypter.get().decrypt(mahasiswa.getPass());
			password = MD5.crypt(users.getUsername() + password);
			users.setPassword(password);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		ojSession.getTransaction().begin();
		ojSession.saveOrUpdate(users);
		ojSession.getTransaction().commit();

		for (Journals journal : journals) {
			int size = ((Number) ojSession.createCriteria(Roles.class).setProjection(Projections.rowCount())
					.add(Restrictions.sqlRestriction("user_id=" + users.getUserId()))
					.add(Restrictions.sqlRestriction("journal_id=" + journal.getJournalId()))
					.add(Restrictions.sqlRestriction("role_id=" + Roles.ROLE_ID_READER)).uniqueResult()).intValue();

			if (size == 0) {
				String sql = "INSERT INTO roles(journal_id, user_id, role_id) VALUES (" + journal.getJournalId() + ", "
						+ users.getUserId() + ", " + Roles.ROLE_ID_READER + ");";
				ojSession.createSQLQuery(sql).executeUpdate();
			}
		}
	}

	public void onImport(Event event) throws Exception {
		UploadEvent uploadEvent = (UploadEvent) event;

		String fileName = uploadEvent.getName();
		if (!fileName.equalsIgnoreCase("MSMHS.DBF")) {
			MyMessageboxConfig.show(
					"Mohon maaf, berkas yang diunggah tidak sesuai. Berkas yang harus diunggah adalah berkas dengan nama MSMHS.DBF. Langkah yang dapat dilakukan: (1) pastikan Bapak/Ibu memilih berkas dengan nama MSMHS.DBF; (2) periksa kembali sumber berkas ekspor EPSBED yang digunakan; (3) setelah berkas sesuai, silakan mengunggah kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		}

		InputStream inputStream = uploadEvent.getMedia().getStreamData();

		File file = new File(application.getRealPath("/temp/"));
		if (file == null || !file.exists()) {
			file.mkdirs();
		}

		File filedata = new File(file.getAbsolutePath() + "/" + (++Common.increments) + "_" + fileName);
		filedata.createNewFile();

		Common.writeInputStreamToFile(inputStream, filedata);

		ImportFromEpsbedHelper.doImport(filedata, null, null, null);

		Session session = HibernateUtil.currentSession();
		String sql = ImportFromEpsbedHelper.read("mahasiswa.sql");
		session.createSQLQuery(sql).executeUpdate();

		MyMessageboxConfig.show(
				"Mohon perhatian, Bapak/Ibu, data Import Mahasiswa harus diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pastikan berkas sumber Import Mahasiswa telah tersedia dan sesuai; (2) lengkapi data yang diperlukan sebelum melanjutkan proses; (3) setelah data terisi, silakan menjalankan kembali proses Import Mahasiswa.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
	}

	private String ambilNimSemulaDariDatabase(Mahasiswa dataMahasiswa) {
		try {
			if (dataMahasiswa == null || dataMahasiswa.getId() == null) return null;
			Mahasiswa tersimpan = (Mahasiswa) HibernateUtil.currentSession().get(Mahasiswa.class, dataMahasiswa.getId());
			if (tersimpan != null && tersimpan.getNim() != null && !tersimpan.getNim().trim().isEmpty()) {
				return tersimpan.getNim().trim();
			}
			BiodataCalonMahasiswa biodata = tersimpan == null ? null : tersimpan.getBiodataCalonMahasiswaData();
			return CommonPMB.ambilNimTersimpanDariRiwayatPmb(HibernateUtil.currentSession(), biodata, tersimpan);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private void tampilkanDialogEditNim(final Mahasiswa sumber) throws Exception {
		if (sumber == null || sumber.getId() == null) return;
		final MyWindow window = new MyWindow("Edit NIM Mahasiswa", "normal", true);
		window.setParent(page.getFirstRoot());
		window.setWidth("420px");
		window.setHeight("190px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig kolom = new MyColumnConfig();
		kolom.setWidth("32%");
		kolom.setParent(columns);
		kolom = new MyColumnConfig();
		kolom.setParent(columns);
		Rows rows = new Rows();
		rows.setParent(grid);
		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM Baru"));
		final Textbox nimBaru = new Textbox(sumber.getNim() == null ? "" : sumber.getNim().trim());
		nimBaru.setWidth("95%");
		row.appendChild(nimBaru);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		batal.setParent(toolbar);
		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (simpanPerubahanNim(sumber.getId(), nimBaru.getValue())) {
					window.detach();
					onSearchDefault(null);
				}
			}
		});
		simpan.setParent(toolbar);
		window.setVisible(true);
		window.onModal();
	}

	@SuppressWarnings("unchecked")
	private boolean simpanPerubahanNim(Long mahasiswaId, String nimBaruInput) throws Exception {
		String nimBaru = nimBaruInput == null ? "" : nimBaruInput.trim();
		if (nimBaru.isEmpty()) {
			MyMessageboxConfig.show("NIM baru tidak boleh kosong.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		Session session = HibernateUtil.currentSession();
		Mahasiswa target = (Mahasiswa) session.get(Mahasiswa.class, mahasiswaId);
		if (target == null) return false;
		String nimLama = target.getNim() == null ? "" : target.getNim().trim();
		if (nimBaru.equals(nimLama)) return true;
		Number jumlahDuplikat = (Number) session.createCriteria(Mahasiswa.class)
				.add(Restrictions.eq("nim", nimBaru))
				.add(Restrictions.ne("id", target.getId()))
				.setProjection(Projections.rowCount()).uniqueResult();
		if (jumlahDuplikat != null && jumlahDuplikat.intValue() > 0) {
			MyMessageboxConfig.showFormat(
					"NIM \"{V1}\" sudah digunakan mahasiswa lain. Silakan gunakan NIM lain.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, nimBaru);
			return false;
		}

		String passwordTeks = nimBaru;
		try {
			String passLama = target.getPass() == null ? "" : Common.desEncrypter.get().decrypt(target.getPass());
			passwordTeks = passLama == null || passLama.trim().isEmpty() || passLama.trim().equals(nimLama)
					? nimBaru : passLama;
			if (passLama == null || passLama.trim().isEmpty() || passLama.trim().equals(nimLama)) {
				target.setPass(Common.desEncrypter.get().encrypt(nimBaru));
			}
		} catch (Exception e) {
			passwordTeks = nimBaru;
			target.setPass(Common.desEncrypter.get().encrypt(nimBaru));
		}

		UserAccess userLama = nimLama.isEmpty() ? null : (UserAccess) session.createCriteria(UserAccess.class)
				.add(Restrictions.eq("username", nimLama)).setMaxResults(1).uniqueResult();
		UserAccess userBaru = (UserAccess) session.createCriteria(UserAccess.class)
				.add(Restrictions.eq("username", nimBaru)).setMaxResults(1).uniqueResult();
		if (userLama != null && userBaru == null) {
			userLama.setUsername(nimBaru);
			userLama.setFirstName(nimBaru);
			Common.refreshSaveOrUpdate(session, userLama);
		}

		target.setNim(nimBaru);
		Common.refreshSaveOrUpdate(session, target);

		List<BiodataCalonMahasiswa> calonTerkait = session.createCriteria(BiodataCalonMahasiswa.class)
				.add(Restrictions.eq("mahasiswa", target)).list();
		if (!nimLama.isEmpty()) {
			calonTerkait.addAll(session.createCriteria(BiodataCalonMahasiswa.class)
					.add(Restrictions.eq("nim", nimLama)).list());
		}
		for (BiodataCalonMahasiswa calon : calonTerkait) {
			calon.setMahasiswa(target);
			calon.setNim(nimBaru);
			Common.refreshSaveOrUpdate(session, calon);
		}

		List<Tbmuser> pengguna = session.createCriteria(Tbmuser.class)
				.add(Restrictions.eq("mahasiswa", target)).list();
		for (Tbmuser user : pengguna) {
			user.setUserId(nimBaru);
			Common.refreshSaveOrUpdate(session, user);
		}

		Common.saveOrUpdateUserAccess(null, target, nimBaru, passwordTeks, target.getEmail());
		MyMessageboxConfig.showFormat("NIM berhasil diubah dari \"{V1}\" menjadi \"{V2}\".",
				"Berhasil", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
				nimLama.isEmpty() ? "-" : nimLama, nimBaru);
		return true;
	}

	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {

			try {

				arg0.setValign("top");
				// TODO Auto-generated method stub
				final Mahasiswa mahasiswa = (Mahasiswa) arg1;
				final StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil
						.currentStatus(mahasiswa).ambilStatusMahasiswa(mahasiswa.currentSemester());
				if (selectedStatusMahasiswa != null && statusMahasiswa != null
						&& !statusMahasiswa.getId().equals(selectedStatusMahasiswa.getId())) {
					arg0.setVisible(false);
				}

				final MyDetail detail = new MyDetail();
				detail.setParent(arg0);
				detail.addEventListener("onOpen", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.clear(detail);
						if (detail.isOpen()) {
							MahasiswaAction.this.session.setAttribute("selectedMahasiswa", mahasiswa);
							MyInclude include = new MyInclude(
									"/pages/master/kalender_mahasiswa.zul?selectedMahasiswa=" + mahasiswa.getId());
							include.setHeight("700px");
							include.setWidth("100%");
							detail.appendChild(include);
						}
					}
				});

				try {
					CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(arg0);
				} catch (Exception e) {
					new MyLabelKecil().setParent(arg0);
					ais.common.Common.tampilErrorJikaAdmin(e);
				}

				String nim = mahasiswa.getNim();

				Vbox a = RevisiHelper.createNewRevisi(Mahasiswa.class, mahasiswa,
						mahasiswa.getNimBaruPindah() == null || mahasiswa.getNimBaruPindah().trim().isEmpty() ? nim
								: (nim + " alih prodi ke " + mahasiswa.getNimBaruPindah()));

				Vbox vbox = new Vbox();
				a.setParent(vbox);

				Hbox myHbox = new Hbox();
				myHbox.setParent(vbox);

				if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
						&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

					if (mahasiswa.getIdRegPd() != null && !mahasiswa.getIdRegPd().trim().isEmpty()) {
						myHbox.appendChild(new Image("/img/svg/check2-circle.svg"));
						myHbox.appendChild(new MyLabelKecilSekali("Feeder valid"));
					} else {
						myHbox.appendChild(new Image("/img/svg/warning-outline.svg"));
						myHbox.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
					}

					MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Krm ke feeder",
							"/img/Finance-Invoice-icon.png");
					buttonTagihan.setStyle("font-size:8px;");
					buttonTagihan.setParent(vbox);
					buttonTagihan.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							MyMessageboxConfig.show(
									"Apakah Bapak/Ibu yakin ingin mengirim data ke Feeder (PDDikti)? Proses ini akan mengirimkan data mahasiswa ke server Feeder dan dapat memerlukan beberapa saat. Silakan tekan tombol OK untuk melanjutkan, atau tekan tombol Batal (Cancel) untuk membatalkan.",
									"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
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

													ais.action.master.feeder.util.NeoFeederPesanFormalHelper.tampilkanGagalKoneksi(ip, port,
											Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF),
											"Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons).");
													return;
												}

												final List<String> errorLog = new ArrayList<String>();

												final Label myLabelProsesDetail = NeoFeederProgressHelper
														.show("Sinkronisasi Neo Feeder", new EventListener() {

															@Override
															public void onEvent(Event arg0) throws Exception {
																if (arg0 != null && !arg0.getName().isEmpty()) {
																	EksporFromFeederAction.display();
																	MyMessageboxConfig.show(arg0.getName(), "Info",
																			MyMessageboxConfig.OK,
																			MyMessageboxConfig.EXCLAMATION);
																}

																if (!errorLog.isEmpty()) {
																	String err = "";
																	for (String s : errorLog) {
																		err += err.isEmpty() ? s
																				: "\n----------------------------------------------------------------------------------------------------------\n"
																						+ s;
																	}

																	MyMessageboxConfig.show(err, "Error Terjadi",
																			MyMessageboxConfig.OK,
																			MyMessageboxConfig.EXCLAMATION);

																	File file = new File(
																			Common.REAL_PATH + "/tmp/error_"
																					+ Common.randLong() + ".txt");

																	if (!file.getParentFile().exists()) {
																		file.getParentFile().mkdirs();
																	}
																	FileUtils.writeStringToFile(file, err);
																	Filedownload.save(file, "text/plain");
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
																// FIX IllegalStateException "Components can be accessed only in event listeners":
																// .setValue() langsung dari thread latar tidak aman -- WAJIB lewat
																// NeoFeederProgressHelper.updateProgres().
																ais.action.master.feeder.util.NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
																		"Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
																return;
															}

															FeederExporter feederImporter = new FeederExporter(
																	feederConnector, token, null, null, null);
															ais.action.master.feeder.util.NeoFeederProgressHelper.updateProgres(myLabelProsesDetail, "Mengirim data " + mahasiswa);
															// FIX "satu mahasiswa gagal menghentikan proses tanpa pesan": pindah ke try khusus
															// agar exception dari exportKeFeeder tertangkap catch di bawah, bukan lolos diam.
															exportKeFeeder(mahasiswa, feederImporter, token,
																	feederConnector, errorLog);
															// FIX "gagal diam-diam": setValue("") sebelumnya dijalankan TANPA SYARAT di luar
															// try (bahkan setelah exception) -- popup progres selalu menutup seolah sukses.
															ais.action.master.feeder.util.NeoFeederProgressHelper.updateProgres(myLabelProsesDetail, "");

														} catch (Exception e) {
															ais.common.Common.tampilErrorJikaAdmin(e);
															ais.action.master.feeder.util.NeoFeederProgressHelper.updateProgres(myLabelProsesDetail,
																"Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																	"pengiriman data Mahasiswa \"" + mahasiswa.getNim() + " " + mahasiswa.getNama() + "\" ke Neo Feeder",
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

				}

				if (dataCalonNggakValid.isChecked()) {
					Session session = HibernateUtil.currentSession();
					Object[] n = (Object[]) session.createCriteria(BiodataCalonMahasiswa.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1)
							.setProjection(Projections.projectionList().add(Projections.property("noRegistrasi"))
									.add(Projections.property("nama")))
							.uniqueResult();
					if (n != null && n.length == 2) {
						new MyLabelKecil("No Reg: " + n[0]).setParent(vbox);
						new MyLabelKecil("Nama Reg: " + n[1]).setParent(vbox);
					} else {
						new MyLabelKecil("Data calon mahasiswa tidak ditemukan").setParent(vbox);
					}
				}

//				TbmuserAction.tampilkanSocialMediaProfile(vbox, mahasiswa.getSocialMediaProfile());
				vbox.setParent(arg0);

				vbox = new Vbox();
				vbox.setParent(arg0);
				new Label(mahasiswa.getNama()).setParent(vbox);

				if (!mahasiswa.getNamaArab().isEmpty()) {
					new Label(mahasiswa.getNamaArab()).setParent(vbox);
				}
				if (!mahasiswa.getNamaTionghoa().isEmpty()) {
					new Label(mahasiswa.getNamaTionghoa()).setParent(vbox);
				}

				mahasiswa.tampilkanHp(vbox);
				mahasiswa.tampilkanEmail(vbox);

				try {
					new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);
				} catch (Exception e) {
					HibernateUtil.currentSession().refresh(mahasiswa);
					new MyLabelKecil().setParent(arg0);
					ais.common.Common.tampilErrorJikaAdmin(e);
				}

				Integer semester = mahasiswa.currentSemester();
				Integer tahap = (ConstantValues.aktifkanTahapan
						&& ConstantValues.getJumlahTahapan(mahasiswa.getProgram(), mahasiswa.getJurusan()) > 2)
								? mahasiswa.currentTahapan()
								: null;
				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahap, null);

				new Label(mahasiswa.getKelas().trim().isEmpty() ? (krsMahasiswa == null ? "" : krsMahasiswa.getKelas())
						: mahasiswa.getKelas()).setParent(arg0);

				new Label(mahasiswa.getWarganegara() == null ? "" : mahasiswa.getWarganegara()).setParent(arg0);

				new Label(mahasiswa.getNegara() == null ? "" : mahasiswa.getNegara().getNamaNegara()).setParent(arg0);

				final Vbox labelDosenHbox = new Vbox();
				labelDosenHbox.setParent(arg0);

				new Label(mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getNama()).setParent(arg0);
				new Label(mahasiswa.getJurusan() == null ? ""
						: mahasiswa.getJurusan().getNama() + (mahasiswa.getKonsentrasi() == null ? ""
								: " / " + mahasiswa.getKonsentrasi().getNama()))
						.setParent(arg0);

				final Html status = new ais.ui.util.MyHtml("..");
				status.setParent(arg0);

				final Html htmlKrs = new ais.ui.util.MyHtml("");
				htmlKrs.setParent(arg0);

				final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
						new java.util.ArrayList<org.zkoss.zk.ui.Component>();

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/online-icon_access.png");
				button.setTooltiptext("Biodata Mahasiswa");
				button.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();

						CommonReportHelper.onCetakBiodataMahasiswa(biodataMahasiswa);
					}
				});
				aksiButtons.add(button);

				button = new MyToolbarbuttonConfig("", "/img/upload.gif");
				button.setVisible(tbmuser != null);
				button.setTooltiptext("KRS — Kartu Studi Mahasiswa");
				button.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								TampilStudiMahasiswaHelper tampilStudiMahasiswaHelper = new TampilStudiMahasiswaHelper(
										null, null, false, edit);
								tampilStudiMahasiswaHelper.tampil(mahasiswa, MahasiswaAction.this, false, null);
							}
						});
					}

				});
				aksiButtons.add(button);

				final MyToolbarbuttonConfig buttonEdit = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
				buttonEdit.setTooltiptext("Edit Data");
				buttonEdit.setVisible(tbmuser != null && edit);
				buttonEdit.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						init(mahasiswa);
						addWindow.setVisible(true);
						addWindow.onModal();
					}

				});
				aksiButtons.add(buttonEdit);

				final MyToolbarbuttonConfig buttonEditNim = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
				buttonEditNim.setTooltiptext("Edit NIM");
				buttonEditNim.setVisible(tbmuser != null && edit);
				buttonEditNim.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						tampilkanDialogEditNim(mahasiswa);
					}
				});
				aksiButtons.add(buttonEditNim);

				final MyToolbarbuttonConfig buttonDelete = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				buttonDelete.setSclass("ais-row-btn-danger");
				buttonDelete.setTooltiptext("Hapus Data");
				buttonDelete.setVisible(tbmuser != null && delete && mahasiswa.getDikunci() == null);
				buttonDelete.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show(
								"Apakah Bapak/Ibu yakin ingin menghapus data ini? Mohon diperhatikan bahwa data yang telah dihapus tidak dapat dikembalikan lagi. Silakan tekan tombol OK untuk melanjutkan penghapusan, atau tekan tombol Batal (Cancel) untuk membatalkan.",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@SuppressWarnings("unchecked")
									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												Session session = HibernateUtil.currentSession();
												List<BiodataMahasiswa> biodataMahasiswas = session
														.createCriteria(BiodataMahasiswa.class)
														.add(Restrictions.eq("mahasiswa", mahasiswa)).list();

												for (BiodataMahasiswa biodataMahasiswa : biodataMahasiswas) {
													session.delete(biodataMahasiswa);
												}

												Common.refreshDelete(mahasiswa);

												onSearchDefault(event);
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.showFormat(
														"Mohon maaf, data ini tidak dapat dihapus karena masih memiliki keterkaitan (relasi) dengan data lainnya. Keterangan kesalahan (error) yang tercatat adalah sebagai berikut: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepaskan terlebih dahulu data-data lain yang terkait dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) setelah keterkaitan tersebut diselesaikan, silakan mencoba kembali proses penghapusan.",
														"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
														e.getMessage());
											}

										}

									}
								});

					}
				});
				aksiButtons.add(buttonDelete);

				// Tombol Kunci / Buka Kunci via temp container lalu pindah ke list
				Hbox tempKunci = new Hbox();
				GeneralValueObject.tampilKunci(tempKunci, mahasiswa, tbmuser, new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						onSearchDefault(event);
					}
				}, false);
				aksiButtons.addAll(new java.util.ArrayList<org.zkoss.zk.ui.Component>(tempKunci.getChildren()));

				if (tbmuser != null && Common.bolehKonfigurasi("tampilkan_cetak_tagihan_di_pendatan_mahasiswa", Konfigurasi.TIDAK_AKTIF)) {

					MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("",
							"/img/Finance-Invoice-icon.png");
					buttonTagihan.setTooltiptext("Cetak Tagihan");
					buttonTagihan.addEventListener("onClick", new EventListener() {

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

							Common.selectComboItem(genapGanjil,
									Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

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
							MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Cetak Tagihan", "/img/save.gif");
							save.setTooltiptext("Proses");
							save.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									window.detach();

									Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(),
											tahunAkademik.getValue(),
											genapGanjil.getSelectedItem().getValue().toString(),
											mahasiswa.getPindahKeKampusIniMasukSemester(),
											mahasiswa.getSemesterMulai());

									CommonReportHelper.prosesSuratTagihan(mahasiswa, tahunAkademik.getValue(), smt,
											null);
								}
							});
							save.setParent(toolbar);

							window.onModal();

						}
					});
					aksiButtons.add(buttonTagihan);

					buttonTagihan = new MyToolbarbuttonConfig("", "/img/print.png");
					buttonTagihan.setTooltiptext("Cetak Kartu Ujian");
					buttonTagihan.addEventListener("onClick", new EventListener() {

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

							Common.selectComboItem(genapGanjil,
									Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

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

							row = new MyFormRow();
							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Ujian"));
							final Combobox jenisUjian;
							row.appendChild(jenisUjian = new Combobox());

							comboitem = new MyComboitemConfig();
							comboitem.setLabel(ConstantValues.UAS.getNama());
							comboitem.setValue(ConstantValues.UAS);
							jenisUjian.appendChild(comboitem);

							comboitem = new MyComboitemConfig();
							comboitem.setLabel(ConstantValues.UTS.getNama());
							comboitem.setValue(ConstantValues.UTS);
							jenisUjian.appendChild(comboitem);

							jenisUjian.setSelectedIndex(0);

							jenisUjian.setWidth("90%");
							jenisUjian.setReadonly(true);

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
							MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Cetak Kartu Ujian",
									"/img/save.gif");
							save.setTooltiptext("Proses");
							save.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									window.detach();

									Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(),
											tahunAkademik.getValue(),
											genapGanjil.getSelectedItem().getValue().toString(),
											mahasiswa.getPindahKeKampusIniMasukSemester(),
											mahasiswa.getSemesterMulai());

									if (jenisUjian.getValue().equalsIgnoreCase(ConstantValues.UTS.getNama())) {
										CommonReportHelper.cetakUTS(mahasiswa, smt, null,
												(String) tahunAkademik.getSelectedItem().getValue(), null, false, true);
									} else {
										CommonReportHelper.cetakUAS(mahasiswa, smt, null,
												(String) tahunAkademik.getSelectedItem().getValue(), null, false, true);
									}

								}
							});
							save.setParent(toolbar);

							window.onModal();

						}
					});
					aksiButtons.add(buttonTagihan);
				}

				// Susun semua tombol: max 3 per baris, rata tengah
				ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

				if (mahasiswa.getDosen() == null && krsMahasiswa.getDosenPa() != null) {
					mahasiswa.setDosen(krsMahasiswa.getDosenPa().getId());
				}

				htmlKrs.setContent(mahasiswa.getStatusKeluar() == null
						? mahasiswa.rubahKeteranganPengambilanKRS(krsMahasiswa.getSemester(), krsMahasiswa.getTahapan(),
								krsMahasiswa.getSemesterPendek(), krsMahasiswa, false)
						: (mahasiswa.getStatusKeluar().getNama()
								+ (mahasiswa.getPredikatKelulusan() == null ? ""
										: " / " + mahasiswa.getPredikatKelulusan().getNama())

								+ (mahasiswa.getStatusSetelahLulus() == null ? ""
										: " / " + mahasiswa.getStatusSetelahLulus().getNama())

								+ (mahasiswa.getStatusPekerjaanSetelahLulus() == null ? ""
										: " / " + mahasiswa.getStatusPekerjaanSetelahLulus().getNama())

								+ (mahasiswa.getStatusDomisiliSetelahLulus() == null ? ""
										: " / " + mahasiswa.getStatusDomisiliSetelahLulus().getNama())

						));

				status.setContent(
						(mahasiswa.getStatusKeluar() == null ? "" : mahasiswa.getStatusKeluar().getNama() + "/")
								+ (statusMahasiswa == null ? "" : statusMahasiswa.getNama()) + "/"
								+ (mahasiswa.getKelompokMahasiswa() != null ? mahasiswa.getKelompokMahasiswa().getNama()
										: (mahasiswa.getStatusAwalMahasiswa() == null ? ""
												: mahasiswa.getStatusAwalMahasiswa().getNama()))
								+ ((statusMahasiswa != null
										&& statusMahasiswa.getNama().equalsIgnoreCase(ConstantValues.LULUS.getNama()))
												? ""
												: "/" + mahasiswa.currentSemester() + "/" + mahasiswa.getSemesterMulai()
														+ ((ConstantValues.aktifkanTahapanTerhubungKeKeuangan
																&& tahap != null && tahap > 0) ? "/ Thp:" + tahap
																		: "")));

				Common.checkDosenPa(krsMahasiswa);
				Dosen dosen = krsMahasiswa.getDosenPa();

				try {
					if (dosen != null) {
						if (mahasiswa.getDosen() == null) {
							krsMahasiswa.setDosenPa(null);
							dosen = null;
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				if (dosen != null) {
					CommonMedia.tampilkanGambarKecil(dosen).setParent(labelDosenHbox);
				}

				Label labelDosen = new Label();
				labelDosen.setParent(labelDosenHbox);
				labelDosen.setValue(dosen == null ? "Tidak mempunyai dosen PA" : dosen.getNama());
				if (dosen == null) {
					labelDosen.setStyle("font-weight:bold;color:red");
				}

			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

	}

	public static void exportKeFeeder(Mahasiswa mahasiswa, FeederExporter feederImporter, String token,
			FeederConnector feederConnector, List<String> errorLog) {
		try {
			feederImporter.mahasiswa(mahasiswa, errorLog);
			// FIX "id_mahasiswa tidak boleh kosong": mahasiswa(...) menelan exception-nya sendiri
			// (Common.tampilErrorJikaAdmin) bila InsertBiodataMahasiswa gagal (mis. NISN kosong,
			// NIK sudah digunakan), sehingga mahasiswa.getFeeder() tetap null/kosong. Sebelumnya
			// mahasiswa_pt(...) tetap dipanggil tanpa syarat, mengirim id_mahasiswa="" ke Feeder --
			// error kedua yang membingungkan ini menutupi penyebab sebenarnya (biodata gagal).
			if (mahasiswa.getFeeder() != null && !mahasiswa.getFeeder().trim().isEmpty()) {
				feederImporter.mahasiswa_pt(mahasiswa, errorLog);
			} else if (errorLog != null) {
				errorLog.add(mahasiswa.getNim() + " " + mahasiswa.getNama()
						+ ": Riwayat Pendidikan dilewati karena Biodata Mahasiswa belum berhasil dikirim/ditemukan"
						+ " di Neo Feeder (id_mahasiswa kosong). Perbaiki dahulu kegagalan pengiriman Biodata"
						+ " di atas sebelum mengirim ulang.");
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void onAddExternal(Event event, EventListener eventListener, Mahasiswa mahasiswa) throws Exception {
		MahasiswaAction mahasiswaAction = new MahasiswaAction();
		mahasiswaAction.eventListener = eventListener;
		mahasiswaAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(mahasiswaAction.addWindow);
		mahasiswaAction.addWindow.setHeight("99%");
		mahasiswaAction.addWindow.setWidth("90%");

		mahasiswaAction.init(mahasiswa);

		mahasiswaAction.addWindow.setVisible(true);
		mahasiswaAction.addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new Mahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private Borderlayout initBiodata(Mahasiswa mahasiswa) throws Exception {
		if (mahasiswa == null || mahasiswa.getId() == null) {
			return new ais.ui.util.MyBorderlayout();
		}
		Borderlayout borderlayout = biodataMahasiswaAction.initMain(mahasiswa);

		return borderlayout;
	}

	private Borderlayout initLoginOrtu(final Mahasiswa mahasiswa) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Username Orang Tua"));
		row.appendChild(userOrtu = new Textbox(mahasiswa.getUserOrtu()));
		userOrtu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Password Orang Tua"));
		row.appendChild(
				passOrtu = new Textbox(mahasiswa.getPassOrtu() == null || mahasiswa.getPassOrtu().trim().equals("") ? ""
						: Common.desEncrypter.get().decrypt(mahasiswa.getPassOrtu())));
		passOrtu.setWidth("90%");
		passOrtu.setType("password");

		if (mahasiswa.getDikunci() != null) {
			Common.freezeGanti(center, true);
		}

		return borderlayout;
	}

	private Map<String, LampiranLain> lampiranLains = new HashMap<String, LampiranLain>();
	private StatusMahasiswa selectedStatusMahasiswa = null;
	private AmbilDataDosenBanbox dosen;
	private Textbox usernameOjs;
	private MyTextbox tanggallahirManual = null;
	private Textbox skDo;
	private Combobox statusDomisiliSetelahLulus;
	private Combobox statusSetelahLulus;
	private Combobox statusPekerjaanSetelahLulus;
	private MyCheckboxConfig ubahKeSemuaSemester;
	private Textbox linkValidasiEksternal;
	private MyDatebox tanggalSkRektor;
	private Label labelWaktuBelajar;
	private Textbox nomorSkpi;
	private Textbox idfinger;
	protected FotoMahasiswa fileFoto = null;
	protected FotoMahasiswaLulus fileFotoLulus = null;
	private Criterion criteriaStatus;
	private MyTextbox batasStudi;
	private Textbox statusKrs;
	private AmbilDataPerguruanTinggiLainBanbox pindahanDari;
	private MyDatebox tanggalWisuda;
	private MyCheckboxConfig statusAwalSelaluIkutDataUtama;
	private MyCheckboxConfig dosenPaSelaluSama;
	private MyCheckboxConfig kelasSelaluSama;
	private Textbox judulSkripsiEn;
	private MyCheckboxConfig pindahkanKrsDanNilaiKeMahasiswaAlihProdi;
	private Textbox emailAtasan;
	private ais.action.master.helper.AtasanMahasiswaHelper atasanHelper;
	private Textbox namaArab;
	private Textbox namaTionghoa;
	private MyCheckboxConfig programSelaluIkutDataUtama;
	private MyDatebox tanggalKegiatanBelajarMengajar;
	private MyCheckboxConfig tidakAdaTagihan;

	private Tabbox initAlumni(final Mahasiswa mahasiswa) throws Exception {

		Tabbox tabbox = new Tabbox();
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");
		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabData = new MyTabConfig("Data Alumni");
		tabData.setParent(tabs);

		MyTabConfig tabAlumni = new MyTabConfig("Angket Penilaian Alumni");
		tabAlumni.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel);
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

		rowParameterTambahan = new MyFormRow();
		rowParameterTambahan.setVisible(false);
		rowParameterTambahan.setStyle("border:0px;background: transparent;");
		rowParameterTambahan.setParent(rows);

		List<Row> parameterRows = new ArrayList<Row>();

		BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
		parameterTambahanAlumniListener = new ParameterTambahanAlumniListener(biodataMahasiswa, parameterRows,
				lampiranLains, rows, false);

		boolean visible = parameterTambahanAlumniListener.check();
		rowParameterTambahan.setVisible(visible);

		parameterTambahanAlumniListener.onEvent(null);

		final Tabpanel tabpanelAlumni = new ais.ui.util.MyTabpanel();
		tabpanelAlumni.setParent(tabpanels);
		tabAlumni.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelAlumni.getChildren().isEmpty()) {
					MyInclude iframe = new MyInclude(
							"/common/checklist_penilaian_umum.zul?mahasiswa=" + mahasiswa.getId());
					tabpanelAlumni.appendChild(iframe);
				}
			}
		});

		return tabbox;
	}

	private Borderlayout initKelulusan(final Mahasiswa mahasiswa) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		West west = new West();
		west.setStyle("border:0px;");
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("270px");
		west.setParent(borderlayout);
		fileFotoLulus = null;
		Common.createDownloadUploadFoto(west, mahasiswa, FotoMahasiswaLulus.class, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				fileFotoLulus = (FotoMahasiswaLulus) arg0.getData();
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

		// Form INTI "Informasi Kelulusan" via FormKelulusanHelper (reuse dengan SkripsiAction).
		// row tetap dideklarasikan karena bagian Skripsi/Judul di bawah masih memakainya.
		MyFormRow row = null;
		ais.action.master.helper.FormKelulusanHelper.Komponen kelulusanKomp = ais.action.master.helper.FormKelulusanHelper.build(rows, mahasiswa, tbmuser);
		statusKeluar = kelulusanKomp.statusKeluar;
		predikatKelulusan = kelulusanKomp.predikatKelulusan;
		statusSetelahLulus = kelulusanKomp.statusSetelahLulus;
		statusPekerjaanSetelahLulus = kelulusanKomp.statusPekerjaanSetelahLulus;
		emailAtasan = kelulusanKomp.emailAtasan;
		statusDomisiliSetelahLulus = kelulusanKomp.statusDomisiliSetelahLulus;
		noIjazah1 = kelulusanKomp.noIjazah1;
		noIjazah2 = kelulusanKomp.noIjazah2;
		noAkta1 = kelulusanKomp.noAkta1;
		nomorSkpi = kelulusanKomp.nomorSkpi;
		tahunLulus = kelulusanKomp.tahunLulus;
		tanggalLulus = kelulusanKomp.tanggalLulus;
		semesterLulus = kelulusanKomp.semesterLulus;
		batasStudi = kelulusanKomp.batasStudi;
		tahunWisuda = kelulusanKomp.tahunWisuda;
		tanggalWisuda = kelulusanKomp.tanggalWisuda;
		tanggalYudisium = kelulusanKomp.tanggalYudisium;
		noAkta2 = kelulusanKomp.noAkta2;
		tanggalSkRektor = kelulusanKomp.tanggalSkRektor;
		skDo = kelulusanKomp.skDo;

		Object[] juduls = (Object[]) (mahasiswa.getId() != null ? HibernateUtil.currentSession()
				.createCriteria(Skripsi.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.setProjection(Projections.projectionList().add(Projections.property("judul"))

						.add(Projections.property("awalBimbingan")).add(Projections.property("akhirBimbingan"))
						.add(Projections.property("judulen"))

				).addOrder(Order.desc("id")).setMaxResults(1).uniqueResult() : null);
		String judul = juduls == null || juduls.length == 0 ? null : juduls[0].toString();
		Date awal = (Date) (juduls == null || juduls.length == 0 ? null : juduls[1]);
		Date akhir = (Date) (juduls == null || juduls.length == 0 ? null : juduls[2]);
		String judulen = juduls == null || juduls.length == 0 ? null : juduls[3].toString();
		if (judul != null && !judul.trim().isEmpty()) {
			mahasiswa.setJudulSkripsi(judul);
		}
		if (judulen != null && !judulen.trim().isEmpty()) {
			mahasiswa.setJudulSkripsiEn(judulen);
		}
		if (awal != null) {
			mahasiswa.setBlnAwalBimbingan(awal);
		}
		if (akhir != null) {
			mahasiswa.setBlnAkhirBimbingan(akhir);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Awal Bimbingan"));
		row.appendChild(blnAwalBimbingan = new MyDatebox(mahasiswa.getBlnAwalBimbingan()));
		blnAwalBimbingan.setWidth("90%");
		if (awal != null) {
			blnAwalBimbingan.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Akhir Bimbingan"));
		row.appendChild(blnAkhirBimbingan = new MyDatebox(mahasiswa.getBlnAkhirBimbingan()));
		blnAkhirBimbingan.setWidth("90%");
		if (akhir != null) {
			blnAkhirBimbingan.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Judul " + Common.getKonfigurasi("label_skripsi", "skripsi").getNilai()));
		row.appendChild(judulSkripsi = new Textbox(mahasiswa.getJudulSkripsi()));
		judulSkripsi.setWidth("90%");
		judulSkripsi.setRows(5);
		if (judul != null && !judul.trim().isEmpty()) {
			judulSkripsi.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Judul " + Common.getKonfigurasi("label_skripsi", "skripsi").getNilai() + " (English)"));
		row.appendChild(judulSkripsiEn = new Textbox(mahasiswa.getJudulSkripsiEn()));
		judulSkripsiEn.setWidth("90%");
		judulSkripsiEn.setRows(5);
		if (judulen != null && !judulen.trim().isEmpty()) {
			judulSkripsiEn.setDisabled(true);
		}

		// Daftar ATASAN / pengguna lulusan (JSON) — editor "Tambah Atasan" reusable.
		row = new MyFormRow();
		row.setValign("top");
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		org.zkoss.zul.Vbox boxAtasan = new org.zkoss.zul.Vbox();
		boxAtasan.setWidth("100%");
		row.appendChild(boxAtasan);
		atasanHelper = new ais.action.master.helper.AtasanMahasiswaHelper();
		atasanHelper.render(boxAtasan, mahasiswa.getAtasans());

		return borderlayout;
	}

	private Borderlayout initPindahan(final Mahasiswa mahasiswa) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pindahan Dari Kampus"));
		row.appendChild(pindahanDari = new AmbilDataPerguruanTinggiLainBanbox());
		pindahanDari.setWidth("90%");
		pindahanDari.setValue(mahasiswa.getPindahanDari() == null ? "" : mahasiswa.getPindahanDari().getNama());
		pindahanDari.setAttribute("perguruanTinggiLain", mahasiswa.getPindahanDari());
		pindahanDari.setAttribute("myValue", mahasiswa.getPindahanDari());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Atau Nama Pindahan Dari Kampus"));
		row.appendChild(pindahanDariKampus = new Textbox(mahasiswa.getPindahanDariKampus()));
		pindahanDariKampus.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Prodi Lama"));
		row.appendChild(namaProdiPindah = new Textbox(mahasiswa.getNamaProdiPindah()));
		namaProdiPindah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM Pindahan"));
		row.appendChild(nimPindahan = new Textbox(mahasiswa.getNimPindahan()));
		nimPindahan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal masuk kampus ini"));
		row.appendChild(tanggalPindah = new MyDatebox(mahasiswa.getTanggalPindah()));
		tanggalPindah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pindah dari kampus lama di semester"));
		row.appendChild(pindahDariKampusLamaDiSemester = new Combobox());
		pindahDariKampusLamaDiSemester.setWidth("90%");
		pindahDariKampusLamaDiSemester.setReadonly(true);

		Integer jumlah_semester = mahasiswa == null || mahasiswa.getJurusan() == null ? 8
				: mahasiswa.getJurusan().getJenjang().getJumlahSemester();

		for (int i = 0; i <= jumlah_semester; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			pindahDariKampusLamaDiSemester.appendChild(comboitem);
		}

		Common.selectComboItem(pindahDariKampusLamaDiSemester, mahasiswa.getPindahDariKampusLamaDiSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pindah ke kampus ini diterima di semester"));
		row.appendChild(pindahKeKampusIniMasukSemester = new Combobox());
		pindahKeKampusIniMasukSemester.setWidth("90%");

		for (int i = 0; i <= jumlah_semester; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			pindahKeKampusIniMasukSemester.appendChild(comboitem);
		}

		Common.selectComboItem(pindahKeKampusIniMasukSemester, mahasiswa.getPindahKeKampusIniMasukSemester());
		pindahKeKampusIniMasukSemester.setReadonly(true);

		if (pindahKeKampusIniMasukSemester.getSelectedItem() == null) {
			pindahKeKampusIniMasukSemester.setSelectedIndex(0);
		}
		if (pindahDariKampusLamaDiSemester.getSelectedItem() == null) {
			pindahDariKampusLamaDiSemester.setSelectedIndex(0);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun akademik " + Common.getCurrentTahunAkademik() + "/"
				+ (Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP) + " semester"));
		final Label currentSemester = new Label();
		row.appendChild(currentSemester);
		EventListener eventListenerCurrentSemester = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					// Ambil data dari komponen UI
					Integer tahunAngkatanMhs = tahunangkatan.getValue().intValue();
					String jenisSemesterSaatIni = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL
							: Perkuliahan.GENAP;

					// Hati-hati: Penamaan variabel UI di sini sering membingungkan
					// semesterMulai.getValue() -> Mengandung "Ganjil" atau "Genap" (Konteks: Musim
					// Masuk)
					String seasonMasuk = (String) semesterMulai.getSelectedItem().getValue();

					// pindahKeKampusIniMasukSemester.getValue() -> Mengandung Angka "1", "2", dst
					// (Konteks: Semester Mulai Angka)
					Integer angkaSemesterMulai = (Integer) pindahKeKampusIniMasukSemester.getSelectedItem().getValue();

					// Panggil fungsi getSemester yang telah dioptimasi
					int semester = Common.getSemester(tahunAngkatanMhs, jenisSemesterSaatIni, angkaSemesterMulai,
							seasonMasuk);

					currentSemester.setValue(String.valueOf(semester));
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};
		tahunangkatan.addEventListener("onChange", eventListenerCurrentSemester);
		semesterMulai.addEventListener("onChange", eventListenerCurrentSemester);
		pindahKeKampusIniMasukSemester.addEventListener("onChange", eventListenerCurrentSemester);
		eventListenerCurrentSemester.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("SKS yang diakui"));
		row.appendChild(sksYangDiakui = new Intbox(mahasiswa.getSksYangDiakui()));
		sksYangDiakui.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan Pindah"));
		row.appendChild(keteranganPindah = new Textbox(mahasiswa.getKeteranganPindah()));
		keteranganPindah.setWidth("90%");
		keteranganPindah.setRows(6);

		if (mahasiswa.getDikunci() != null) {
			Common.freezeGanti(center, true);
		}

		return borderlayout;
	}

	private Borderlayout initAlihProdi(final Mahasiswa mahasiswa) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		// MyFormRow row = new MyFormRow();row.setValign("top");
		//		// row.setParent(rows);
		// row.appendChild(new ais.ui.util.MyLabelConfig("NIM Lama Sebelum Alih
		// Prodi"));
		// row.appendChild(nimLamaSebelumPindah = new Textbox(mahasiswa
		// .getNimLamaSebelumPindah()));
		// nimLamaSebelumPindah.setWidth("90%");

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM Lama Sebelum Alih Prodi"));
		row.appendChild(alihProdiMahasiswa = new AmbilDataMahasiswaBanbox());
		alihProdiMahasiswa.setAttribute("mahasiswa", mahasiswa.getAlihProdiMahasiswa());
		alihProdiMahasiswa.setValue(
				mahasiswa.getAlihProdiMahasiswa() == null ? "" : mahasiswa.getAlihProdiMahasiswa().toString());
		alihProdiMahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal alih prodi"));
		row.appendChild(tanggalPindahProdi = new MyDatebox(mahasiswa.getTanggalPindahProdi()));
		tanggalPindahProdi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alih ke prodi yang baru masuk semester"));
		row.appendChild(pindahKeProdiIniMasukSemester = new Combobox());
		pindahKeProdiIniMasukSemester.setWidth("90%");

		Integer jumlah_semester = mahasiswa == null || mahasiswa.getJurusan() == null ? 8
				: mahasiswa.getJurusan().getJenjang().getJumlahSemester();

		for (int i = 0; i <= jumlah_semester; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			pindahKeProdiIniMasukSemester.appendChild(comboitem);
		}

		Common.selectComboItem(pindahKeProdiIniMasukSemester, mahasiswa.getPindahKeProdiIniMasukSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("SKS yang diakui"));
		row.appendChild(sksYangDiakuiPindahProdi = new Intbox(mahasiswa.getSksYangDiakuiPindahProdi()));
		sksYangDiakuiPindahProdi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(pindahkanKrsDanNilaiKeMahasiswaAlihProdi = new MyCheckboxConfig(
				"Pindahkan KRS dan nilai ke mahasiswa alih prodi"));
		pindahkanKrsDanNilaiKeMahasiswaAlihProdi.setChecked(mahasiswa.getPindahkanKrsDanNilaiKeMahasiswaAlihProdi());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan Alih Prodi"));
		row.appendChild(keteranganPindahProdi = new Textbox(mahasiswa.getKeteranganPindahProdi()));
		keteranganPindahProdi.setWidth("90%");
		keteranganPindahProdi.setRows(6);

		if (mahasiswa.getDikunci() != null) {
			Common.freezeGanti(center, true);
		}

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private Borderlayout initMain(final Mahasiswa mahasiswa) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		if (mahasiswa.getId() != null) {
			HibernateUtil.currentSession().refresh(mahasiswa);
		}
		BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();

		West west = new West();
		west.setStyle("border:0px;");
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("270px");
		west.setParent(borderlayout);
		fileFoto = null;
		Common.createDownloadUploadFoto(west, mahasiswa, FotoMahasiswa.class, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				fileFoto = (FotoMahasiswa) arg0.getData();
			}
		}, true);

		program = Common.initPrograms(program);

		statusAwalMahasiswa = new Combobox();
		Common.insertCombo(statusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		statusAwalMahasiswaSetelahSmtTertentu = new Combobox();
		Common.insertComboDanSemua(statusAwalMahasiswaSetelahSmtTertentu, new String[] { "nama" }, "keterangan",
				StatusAwalMahasiswa.class, "== Status Semester Tertentu Ikut Status Default ==",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		statusAwalMahasiswaSetelahSmtTertentuLagi = new Combobox();
		Common.insertComboDanSemua(statusAwalMahasiswaSetelahSmtTertentuLagi, new String[] { "nama" }, "keterangan",
				StatusAwalMahasiswa.class, "== Status Semester Tertentu Ikut Status Default ==",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		fakultas = new Combobox();
		jurusan = new Combobox();
		konsentrasi = new Combobox();
		Common.insertCombo(fakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		class FakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(jurusan);
				jurusan.setSelectedItem(null);
				if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));

			}

		}

		fakultas.addEventListener("onChange", new FakultasEventListener());

		class JurusanEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(konsentrasi);
				konsentrasi.setSelectedItem(null);
				if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(konsentrasi, "nama", Konsentrasi.class,
						CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false));
			}

		}
		jurusan.addEventListener("onChange", new JurusanEventListener());
		semesterMulai = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterMulai.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterMulai.appendChild(comboitem);

		waktuKuliah = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("PAGI");
		comboitem.setValue("PAGI");
		waktuKuliah.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("SIANG");
		comboitem.setValue("SIANG");
		waktuKuliah.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("SORE");
		comboitem.setValue("SORE");
		waktuKuliah.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("MALAM");
		comboitem.setValue("MALAM");
		waktuKuliah.appendChild(comboitem);

		kelamin = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Laki-laki");
		comboitem.setValue("Laki-laki");
		kelamin.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Perempuan");
		comboitem.setValue("Perempuan");
		kelamin.appendChild(comboitem);

		kewarganegaraan = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(ais.database.model.Mahasiswa.WNI);
		comboitem.setValue(ais.database.model.Mahasiswa.WNI);
		kewarganegaraan.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(ais.database.model.Mahasiswa.WNA);
		comboitem.setValue(ais.database.model.Mahasiswa.WNA);
		kewarganegaraan.appendChild(comboitem);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM *"));
		row.appendChild(nim = new Textbox(mahasiswa.getNim() == null ? "" : mahasiswa.getNim()));
		nim.setWidth("90%");

		Common.initKeterangan(rows, "* Nomor Induk Mahasiswa (NIM) digunakan usebagai username untuk login mahasiswa");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Password (Hanya Super Admin)"));
		row.appendChild(password = new Textbox(mahasiswa.getPass() == null || mahasiswa.getPass().trim().equals("") ? ""
				: Common.desEncrypter.get().decrypt(mahasiswa.getPass())));
		password.setWidth("90%");
		password.setType("password");
		password.setDisabled(!Common.getApakahAdmin());

		boolean terhubungKeOjs = Common.bolehKonfigurasi("terhubung_ke_ojs", Konfigurasi.TIDAK_AKTIF);

		row = new MyFormRow();
		row.setVisible(terhubungKeOjs);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("OJS Username"));
		row.appendChild(usernameOjs = new Textbox(mahasiswa.getUsernameOjs()));
		usernameOjs.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(aktif = new MyCheckboxConfig("Mahasiswa ini Aktif (bisa login)"));
		aktif.setChecked(mahasiswa.getAktif());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tidakAdaTagihan = new MyCheckboxConfig("Mahasiswa ini tidak ada tagihan (Semua tagihan 0)"));
		tidakAdaTagihan.setChecked(mahasiswa.getTidakAdaTagihan());

		nim.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (password.getValue().trim().equals("")) {
					password.setValue(nim.getValue().trim());
				}
			}
		});

		if (Common.bolehKonfigurasi("tampilkan_link_login_oleh_admin_di_data_mahasiswa")) {
			if (mahasiswa.getId() != null) {

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Link"));

				final A a = new A("Tampilkan Link");
				a.setHref("");
				row.appendChild(a);

				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String code = mahasiswa.urlLogin();
						a.setLabel(code);
						a.setHref(Common.getRequestHostWithProtocol() + "/logoff?param="
								+ URLEncoder.encode(code, "UTF-8"));
					}
				});

				Common.initKeterangan(rows,
						"Link ini bisa digunakan untuk login tanpa menggunakan password, Misal: bisa digunakan untuk Review SPADA");
			}
		}
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Finger Print"));
		row.appendChild(idfinger = new Textbox(mahasiswa.getIdfinger()));
		idfinger.setWidth("90%");

		// private Textbox password;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama *"));
		row.appendChild(nama = new Textbox(mahasiswa.getNama() == null ? "" : mahasiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama (Aksara Arab)"));
		row.appendChild(namaArab = new Textbox(mahasiswa.getNamaArab()));
		namaArab.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama (Aksara Tionghoa)"));
		row.appendChild(namaTionghoa = new Textbox(mahasiswa.getNamaTionghoa()));
		namaTionghoa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		row.appendChild(tahunangkatan = new Decimalbox(new BigDecimal(
				mahasiswa.getTahunangkatan() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
						: mahasiswa.getTahunangkatan())));
		tahunangkatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Masuk"));
		row.appendChild(tanggalMasuk = new MyDatebox(mahasiswa.getTanggalMasuk()));
		tanggalMasuk.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai Kegiatan Belajar"));
		Vbox vboxTglMasuk = new Vbox();
		row.appendChild(vboxTglMasuk);
		vboxTglMasuk.appendChild(
				tanggalKegiatanBelajarMengajar = new MyDatebox(mahasiswa.getTanggalKegiatanBelajarMengajar()));
		tanggalKegiatanBelajarMengajar.setReadonly(true);
		vboxTglMasuk.appendChild(labelWaktuBelajar = new Label());
		EventListener listener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				mahasiswa.setTanggalKegiatanBelajarMengajar(tanggalKegiatanBelajarMengajar.getValue());

				labelWaktuBelajar.setValue(mahasiswa.ambilMasaStudi());
			}
		};

		jenisSeleksi = new Combobox();
		Common.insertCombo(jenisSeleksi, "nama", "deskripsi", JenisSeleksi.class, Restrictions.eq("aktif", true));
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jalur Pendaftaran *"));
		row.appendChild(jenisSeleksi);
		Common.selectComboItem(jenisSeleksi, mahasiswa.getJenisSeleksi());
		jenisSeleksi.setReadonly(true);

		jenisPembiayaanMahasiswa = new Combobox();
		Common.insertCombo(jenisPembiayaanMahasiswa, "nama", "keterangan", JenisPembiayaanMahasiswa.class,
				Restrictions.eq("aktif", true));
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembiayaan Mahasiswa *"));
		row.appendChild(jenisPembiayaanMahasiswa);
		Common.selectComboItem(jenisPembiayaanMahasiswa, mahasiswa.getJenisPembiayaanMahasiswa());
		jenisPembiayaanMahasiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.selectComboItem(program,
				mahasiswa.getProgram() == null || mahasiswa.getProgram().trim().equals("") ? "Reguler"
						: mahasiswa.getProgram());
		row.appendChild(program);
		program.setWidth("90%");
		program.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(programSelaluIkutDataUtama = new MyCheckboxConfig("Program Selalu Ikut Data Mahasiswa"));
		programSelaluIkutDataUtama.setChecked(mahasiswa.getProgramSelaluIkutDataUtama());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kewarganegaraan"));
		Common.selectComboItem(kewarganegaraan, mahasiswa.getWarganegara());
		row.appendChild(kewarganegaraan);
		kewarganegaraan.setWidth("90%");
		kewarganegaraan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Asal Negara"));
		row.appendChild(negara = new AmbilDataNegaraBanbox());

		try {
			negara.setAttribute("negara",
					mahasiswa.getNegara() == null ? ConstantValues.INDONESIA : mahasiswa.getNegara());
			negara.setValue(
					(mahasiswa.getNegara() == null ? ConstantValues.INDONESIA : mahasiswa.getNegara()).getNamaNegara());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/MahasiswaAction.java:5843");
			// TODO: handle exception
		}
		negara.setReadonly(true);
		negara.setWidth("90%");

		Tbmuser tbmuser = Common.getCurrentUser();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));

		Common.selectComboItem(true, fakultas,
				mahasiswa.getJurusan() == null ? tbmuser.ambilFakultas() : mahasiswa.getJurusan().getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		fakultas.setReadonly(true);
		// fakultas.setDisabled(false);

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.selectComboItem(true, jurusan,
				mahasiswa.getJurusan() == null ? tbmuser.ambilJurusan() : mahasiswa.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		jurusan.setReadonly(true);
		// jurusan.setDisabled(false);

		tanggalKegiatanBelajarMengajar.addEventListener("onChange", listener);
		jurusan.addEventListener("onChange", listener);
		listener.onEvent(null);

		Common.insertCombo(konsentrasi, new String[] { "nama" }, "namaEnglish", Konsentrasi.class,
				Restrictions.eq("jurusan",
						mahasiswa.getJurusan() == null ? tbmuser.ambilJurusan() : mahasiswa.getJurusan()),
				Restrictions.sqlRestriction("true"));

		row = new MyFormRow();
		row.setVisible(Common.getKonfigurasi("tampil_konsentrasi_mahasiswa", Konfigurasi.AKTIF).getNilai().trim()
				.equalsIgnoreCase(Konfigurasi.AKTIF));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Konsentrasi"));
		konsentrasi.setSelectedItem(null);
		Common.selectComboItem(true, konsentrasi, mahasiswa.getKonsentrasi());
		row.appendChild(konsentrasi);
		konsentrasi.setWidth("90%");
		konsentrasi.setReadonly(true);

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen PA / Kelas"));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(dosen = new AmbilDataDosenBanbox());
		dosen.setValue(
				krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? "" : (krsMahasiswa.getDosenPa().getNama()));
		dosen.setAttribute("dosen", krsMahasiswa == null ? null : krsMahasiswa.getDosenPa());
		dosen.setAttribute("myValue", krsMahasiswa == null ? null : krsMahasiswa.getDosenPa());
		dosen.setWidth("150px");

		if (tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen mydosen = tbmuser.ambilDosen();
			dosen.setValue(mydosen.getNama());
			dosen.setAttribute("myValue", mydosen);
			dosen.setAttribute("dosen", mydosen);
			dosen.setDisabled(true);
		}

		hbox.appendChild(new Label(" / "));

		kelas = new AmbilDataKelasBanbox();
		hbox.appendChild(kelas);
		kelas.setValue(mahasiswa.getKelas());
		dosen.setWidth("100px");

		ubahKeSemuaSemester = new MyCheckboxConfig("Ubah Dosen Pembimbing Akademik dan Kelas ke semua semester");
		hbox.appendChild(ubahKeSemuaSemester);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(dosenPaSelaluSama = new MyCheckboxConfig("Dosen PA selalu Ikut Data Mahasiswa"));
		dosenPaSelaluSama.setChecked(mahasiswa.getDosenPaSelaluSama());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(kelasSelaluSama = new MyCheckboxConfig("Kelas selalu Ikut Data Mahasiswa"));
		kelasSelaluSama.setChecked(mahasiswa.getKelasSelaluSama());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kelamin"));
		Common.selectComboItem(kelamin, mahasiswa.getKelamin());
		row.appendChild(kelamin);
		kelamin.setWidth("90%");
		kelamin.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tempat / Tanggal Lahir"));
		Box hboxa = Common.isMobile() ? new Vbox() : new Hbox();
		hboxa.appendChild(
				tempatlahir = new Textbox(mahasiswa.getTempatlahir() == null ? "" : mahasiswa.getTempatlahir()));
		hboxa.appendChild(tanggallahir = new MyDatebox(
				mahasiswa.getTanggallahir() == null ? ais.ui.util.WaktuUtil.getDate() : mahasiswa.getTanggallahir()));
		row.appendChild(hboxa);
		tempatlahir.setCols(15);

		tanggallahirManual = null;
		if (Common.bolehKonfigurasi("tanggal_lahir_manual_tampil_di_mahasiswa", Konfigurasi.TIDAK_AKTIF)) {
			hboxa.appendChild(new ais.ui.util.MyLabelConfig(" atau tgl lahir ketik manual : "));
			hboxa.appendChild(tanggallahirManual = new MyTextbox(mahasiswa.getTanggallahirManual()));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tinggi Badan / Berat Badan / Golongan darah"));
		berat_badan = new Decimalbox(
				new BigDecimal(mahasiswa == null || mahasiswa.getBerat_badan() == null
						? (biodataMahasiswa == null || biodataMahasiswa.getBeratBadan() == null ? 0
								: biodataMahasiswa.getBeratBadan())
						: mahasiswa.getBerat_badan()));
		berat_badan.setCols(3);
		tinggi_badan = new Decimalbox(
				new BigDecimal(mahasiswa == null || mahasiswa.getTinggi_badan() == null
						? (biodataMahasiswa == null || biodataMahasiswa.getTinggiBadan() == null ? 0
								: biodataMahasiswa.getTinggiBadan())
						: mahasiswa.getTinggi_badan()));
		tinggi_badan.setCols(3);
		golongan_darah = new Textbox(mahasiswa.getGolongan_darah() == null
				? (biodataMahasiswa == null ? "" : biodataMahasiswa.getGolonganDarah())
				: mahasiswa.getGolongan_darah());
		golongan_darah.setCols(3);
		row.appendChild(new Hbox(
				new Component[] { tinggi_badan, new Label("Cm / "), berat_badan, new Label("Kg / "), golongan_darah }));

		BiodataCalonMahasiswa biodataCalonMahasiswa = mahasiswa.getBiodataCalonMahasiswaData();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No.Reg. Calon Mhs"));
		row.appendChild(new Label(biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getNoRegistrasi()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No.Ujian Calon Mhs"));
		row.appendChild(new Label(biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getNoUjian()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang Calon Mhs"));
		row.appendChild(
				new Label(biodataCalonMahasiswa == null || biodataCalonMahasiswa.getGelombangPendaftaran() == null ? ""
						: biodataCalonMahasiswa.getGelombangPendaftaran().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Paket Calon Mhs"));
		row.appendChild(new Label(biodataCalonMahasiswa == null || biodataCalonMahasiswa.getPaket() == null ? ""
				: biodataCalonMahasiswa.getPaket().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Agama"));
		row.appendChild(new Label(biodataMahasiswa == null || biodataMahasiswa.getAgama() == null ? ""
				: biodataMahasiswa.getAgama().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telp."));
		Label telp;
		row.appendChild(telp = new Label(mahasiswa.getTelp() == null ? "" : mahasiswa.getTelp()));
		telp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Email"));
		Label aa;
		row.appendChild(aa = new Label(mahasiswa.getEmail() == null ? "" : mahasiswa.getEmail()));
		// //
		// email.setConstraint("/.+@.+\\.[a-z]+/: Format email harus sesuai");
		aa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. KTP"));
		row.appendChild(ktp = new Label(biodataMahasiswa == null ? "" : biodataMahasiswa.getNoIdentitas()));
		ktp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NISN"));
		row.appendChild(new ais.ui.util.MyLabelConfig(biodataMahasiswa == null ? "" : biodataMahasiswa.getNisn()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NPWP"));
		row.appendChild(new ais.ui.util.MyLabelConfig(biodataMahasiswa == null ? "" : biodataMahasiswa.getNpwp()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat"));
		row.appendChild(alamat = new Label(
				mahasiswa.getAlamat() == null ? (biodataMahasiswa == null ? "" : biodataMahasiswa.getAlamat())
						: mahasiswa.getAlamat()));
		alamat.setWidth("90%");
		alamat.setMultiline(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		row.appendChild(
				new ais.ui.util.MyLabelConfig(mahasiswa.getJenjang() == null ? "" : mahasiswa.getJenjang().getNama()));

		row = new MyFormRow();
		row.setVisible(mahasiswa != null && mahasiswa.getId() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
		row.appendChild(status = new Label(mahasiswa != null && mahasiswa.getId() != null
				? ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa()
						.getNama()
				: ""));
		status.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai belajar di semester"));
		Common.selectComboItem(semesterMulai, mahasiswa.getSemesterMulai());
		row.appendChild(semesterMulai);
		semesterMulai.setWidth("90%");
		semesterMulai.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("waktu_perkuliahan_pagi_siang_sore_malam_ditampilkan"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Kuliah"));
		Common.selectComboItem(waktuKuliah, mahasiswa.getWaktuKuliah());
		row.appendChild(waktuKuliah);
		waktuKuliah.setWidth("90%");
		waktuKuliah.setReadonly(true);

		tbmuser = Common.getCurrentUser();

		String statusAwalMahasiswaHanyaBolehDiubahOleh = Common
				.getKonfigurasi("status_awal_mahasiswa_hanya_boleh_diubah_oleh", "").getNilai();

		List<String> stringsStatusAwal = new ArrayList<String>();
		for (String s : statusAwalMahasiswaHanyaBolehDiubahOleh.split(";")) {
			stringsStatusAwal.add(s.trim());
		}

		final boolean bolehUbahStatusAwal = statusAwalMahasiswaHanyaBolehDiubahOleh.isEmpty() || (tbmuser != null
				&& tbmuser.hakAkses() != null && stringsStatusAwal.contains(tbmuser.hakAkses().getRoleId()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal"));
		Common.selectComboItem(statusAwalMahasiswa, mahasiswa.getStatusAwalMahasiswa());
		if (bolehUbahStatusAwal) {
			row.appendChild(statusAwalMahasiswa);
		} else {
			row.appendChild(new Label(
					mahasiswa.getStatusAwalMahasiswa() == null ? "" : mahasiswa.getStatusAwalMahasiswa().getNama()));
		}
		statusAwalMahasiswa.setWidth("90%");
		statusAwalMahasiswa.setReadonly(true);

		if (statusAwalMahasiswa.getSelectedItem() == null) {
			try {
				List comboitems = statusAwalMahasiswa.getChildren();
				for (Object item : comboitems) {
					if (item instanceof Comboitem) {
						Comboitem c = (Comboitem) item;
						if (c.getLabel() != null && c.getLabel().toLowerCase().contains("baru")) {
							statusAwalMahasiswa.setSelectedItem(c);
							break;
						}
					}
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}

		statusAwalMahasiswa.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				mahasiswa.setStatusAwalMahasiswa(
						(StatusAwalMahasiswa) (statusAwalMahasiswa.getSelectedItem() == null ? null
								: statusAwalMahasiswa.getSelectedItem().getValue()));
				tabPindahan.setVisible(tampilkanTabPindahan(mahasiswa));
				tabAlihProdi.setVisible(tampilkanTabAlihProdi(mahasiswa));
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		statusAwalSelaluIkutDataUtama = new MyCheckboxConfig("Status Awal Selalu Ikut Data Mahasiswa");
		if (bolehUbahStatusAwal) {
			row.appendChild(statusAwalSelaluIkutDataUtama);
		} else {
			row.appendChild(new Label("Status Awal Selalu Ikut Data Mahasiswa -> "
					+ (mahasiswa.getStatusAwalSelaluIkutDataUtama() ? "Ya" : "Tidak")));
		}
		statusAwalSelaluIkutDataUtama.setChecked(mahasiswa.getStatusAwalSelaluIkutDataUtama());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal Setelah Smt Tertentu (I)"));
		Common.selectComboItem(statusAwalMahasiswaSetelahSmtTertentu,
				mahasiswa.getStatusAwalMahasiswaSetelahSmtTertentu());

		Hbox b = new Hbox(new Component[] { new ais.ui.util.MyLabelConfig("Smt"),
				smtStatusAwal = new Decimalbox(
						mahasiswa.getSmtStatusAwal() == null ? null : new BigDecimal(mahasiswa.getSmtStatusAwal())),
				new ais.ui.util.MyLabelConfig("Status"), statusAwalMahasiswaSetelahSmtTertentu });
		if (bolehUbahStatusAwal) {
			row.appendChild(b);
		} else {
			row.appendChild(new Hbox(new Component[] { new ais.ui.util.MyLabelConfig("Smt"),
					new Label(mahasiswa.getSmtStatusAwal() == null ? "" : mahasiswa.getSmtStatusAwal().toString()),
					new ais.ui.util.MyLabelConfig("Status"),
					new Label(mahasiswa.getStatusAwalMahasiswaSetelahSmtTertentu() == null ? ""
							: mahasiswa.getStatusAwalMahasiswaSetelahSmtTertentu().toString()) }));
		}
		smtStatusAwal.setCols(2);
		statusAwalMahasiswaSetelahSmtTertentu.setCols(10);
		statusAwalMahasiswaSetelahSmtTertentu.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal Setelah Smt Tertentu (II)"));
		Common.selectComboItem(statusAwalMahasiswaSetelahSmtTertentuLagi,
				mahasiswa.getStatusAwalMahasiswaSetelahSmtTertentuLagi());
		b = new Hbox(new Component[] { new ais.ui.util.MyLabelConfig("Smt"),
				smtStatusAwalLagi = new Decimalbox(mahasiswa.getSmtStatusAwalLagi() == null ? null
						: new BigDecimal(mahasiswa.getSmtStatusAwalLagi())),
				new ais.ui.util.MyLabelConfig("Status"), statusAwalMahasiswaSetelahSmtTertentuLagi });
		if (bolehUbahStatusAwal) {
			row.appendChild(b);
		} else {
			row.appendChild(new Hbox(new Component[] { new ais.ui.util.MyLabelConfig("Smt"),
					new Label(mahasiswa.getSmtStatusAwalLagi() == null ? ""
							: mahasiswa.getSmtStatusAwalLagi().toString()),
					new ais.ui.util.MyLabelConfig("Status"),
					new Label(mahasiswa.getStatusAwalMahasiswaSetelahSmtTertentuLagi() == null ? ""
							: mahasiswa.getStatusAwalMahasiswaSetelahSmtTertentuLagi().toString()) }));
		}
		smtStatusAwalLagi.setCols(2);
		statusAwalMahasiswaSetelahSmtTertentuLagi.setCols(10);
		statusAwalMahasiswaSetelahSmtTertentuLagi.setReadonly(true);

		// ===== RIWAYAT STATUS AWAL (UKT) PER-SEMESTER =====
		if (mahasiswa != null && mahasiswa.getId() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Riwayat Status Awal\n(UKT) Per-Semester"));

			Vbox vboxRiwayat = new Vbox();
			vboxRiwayat.setWidth("100%");
			row.appendChild(vboxRiwayat);

			// Batasi s.d. smt aktif atau smt lulus
			Integer _smtMax = mahasiswa.getSemesterLulus();
			if (_smtMax == null || _smtMax <= 0) _smtMax = mahasiswa.currentSemester();
			final Integer smtMaxFinal = _smtMax;

			final org.zkoss.zul.Grid gridStatusAwal = new org.zkoss.zul.Grid();
			gridStatusAwal.setWidth("100%");
			gridStatusAwal.setStyle("border:1px solid #e2e8f0;margin-top:2px;font-size:12px");

			org.zkoss.zul.Columns gridCols = new org.zkoss.zul.Columns();
			gridCols.setSizable(true);
			gridCols.setParent(gridStatusAwal);
			org.zkoss.zul.Column colTa = new org.zkoss.zul.Column("TA"); colTa.setWidth("90px"); colTa.setParent(gridCols);
			org.zkoss.zul.Column colSmt = new org.zkoss.zul.Column("Smt"); colSmt.setWidth("80px"); colSmt.setParent(gridCols);
			org.zkoss.zul.Column colStatus = new org.zkoss.zul.Column("Status"); colStatus.setWidth("85px"); colStatus.setParent(gridCols);
			org.zkoss.zul.Column colStatusAwal = new org.zkoss.zul.Column("Status Awal (UKT)"); colStatusAwal.setParent(gridCols);
			org.zkoss.zul.Column colProgram = new org.zkoss.zul.Column("Program"); colProgram.setWidth("75px"); colProgram.setParent(gridCols);
			final java.util.List<StatusAwalMahasiswa> statusAwalOpts = new java.util.ArrayList<StatusAwalMahasiswa>();
			if (bolehUbahStatusAwal) {
				org.zkoss.zul.Column colPaksa = new org.zkoss.zul.Column("Paksa"); colPaksa.setWidth("105px"); colPaksa.setParent(gridCols);
				Session sessionSaOpts = null;
				try {
					sessionSaOpts = HibernateUtil.openSession();
					sessionSaOpts.setFlushMode(org.hibernate.FlushMode.MANUAL);
					@SuppressWarnings("unchecked")
					java.util.List<StatusAwalMahasiswa> saOpts = sessionSaOpts.createCriteria(StatusAwalMahasiswa.class)
							.addOrder(Order.asc("nama")).list();
					statusAwalOpts.addAll(saOpts);
				} catch (Exception eSaOpts) {
					Common.tampilErrorJikaAdmin(eSaOpts);
				} finally {
					if (sessionSaOpts != null && sessionSaOpts.isOpen()) sessionSaOpts.close();
				}
			}

			final Rows rowsStatusAwal = new Rows();
			rowsStatusAwal.setParent(gridStatusAwal);

			tampilkanRiwayatStatusAwalDariKrsReguler(rowsStatusAwal, mahasiswa, smtMaxFinal,
					bolehUbahStatusAwal, statusAwalOpts, false);
			gridStatusAwal.setParent(vboxRiwayat);

			// Tombol Sinkronkan Status Awal - terapkan ulang aturan (I)/(II) ke semua semester
			if (bolehUbahStatusAwal) {
				MyToolbarbuttonConfig btnSinkronStatusAwal = new MyToolbarbuttonConfig(
						"Sinkronkan Status Awal", "/img/Button-Refresh-icon.png");
				btnSinkronStatusAwal.setTooltiptext(
						"Terapkan ulang aturan Status Awal (I)/(II) ke seluruh riwayat semester mahasiswa ini");
				btnSinkronStatusAwal.setStyle("margin-top:4px");
				btnSinkronStatusAwal.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Session sessionSync = null;
						try {
							sessionSync = HibernateUtil.getSessionFactory().openSession();
							sessionSync.setFlushMode(org.hibernate.FlushMode.MANUAL);
							@SuppressWarnings("unchecked")
							java.util.List<HistoryStatusMahasiswa> semua = sessionSync
									.createCriteria(HistoryStatusMahasiswa.class)
									.add(Restrictions.eq("mahasiswa", mahasiswa))
									.add(Restrictions.isNull("sp"))
									.list();
							int updated = 0;
							for (HistoryStatusMahasiswa hist : semua) {
								if (hist.getSemester() == null) continue;
								StatusAwalMahasiswa corrected = HistoryStatusMahasiswa.ambilStatusAwal(
										mahasiswa, hist.getSemester(), null);
								if (corrected != null && (hist.getStatusAwalMahasiswa() == null
										|| !corrected.getId().equals(hist.getStatusAwalMahasiswa().getId()))) {
									hist.setStatusAwalMahasiswa(corrected);
									sessionSync.getTransaction().begin();
									sessionSync.update(hist);
									sessionSync.getTransaction().commit();
									updated++;
								}
							}
							MyMessageboxConfig.show(
									updated + " semester diperbarui status awal (UKT)-nya.",
									"Sinkronisasi Selesai", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							// Refresh tampilan grid dari sumber yang sama dengan tab KRS reguler.
							rowsStatusAwal.getChildren().clear();
							tampilkanRiwayatStatusAwalDariKrsReguler(rowsStatusAwal, mahasiswa, smtMaxFinal,
									bolehUbahStatusAwal, statusAwalOpts, true);
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							MyMessageboxConfig.show("Gagal sinkronisasi: " + e.getMessage(),
									"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
						} finally {
							if (sessionSync != null && sessionSync.isOpen()) sessionSync.close();
						}
					}
				});
				btnSinkronStatusAwal.setParent(vboxRiwayat);
			}
		}

//		if (mahasiswa.getKelompokMahasiswa() != null) {
//			Common.selectComboItem(statusAwalMahasiswa, aa);
//			statusAwalMahasiswa.setDisabled(true);
//			statusAwalSelaluIkutDataUtama.setDisabled(true);
//			statusAwalMahasiswaSetelahSmtTertentu.setDisabled(true);
//			statusAwalMahasiswaSetelahSmtTertentuLagi.setDisabled(true);
//
//			smtStatusAwal.setDisabled(true);
//			smtStatusAwalLagi.setDisabled(true);
//		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor KIP"));
		row.appendChild(statusKrs = new Textbox(mahasiswa.getStatusKrs()));
		statusKrs.setWidth("90%");

		Common.initKeterangan(rows, "Untuk mahasiswa yang mendapat Kartu Indonesia Pintar (KIP)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(mahasiswa.getKeterangan() == null ? "" : mahasiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(4);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Link Validasi Eksternal"));
		row.appendChild(linkValidasiEksternal = new Textbox(mahasiswa.getLinkValidasiEksternal()));
		linkValidasiEksternal.setWidth("90%");
		linkValidasiEksternal.setRows(3);

		row = new MyFormRow();
		row.setVisible(Common.getApakahAdminBolehAksesFeeder());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Feeder (untuk data PDDKITI)"));
		row.appendChild(feeder = new Textbox(mahasiswa.getFeeder()));
		feeder.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(Common.getApakahAdminBolehAksesFeeder());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("ID Reg Pd (untuk data PDDKITI)"));
		row.appendChild(idRegPd = new Textbox(mahasiswa.getIdRegPd()));
		idRegPd.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(Common.getApakahAdmin());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lock Id (untuk data EMIS)"));
		row.appendChild(lockId = new Textbox(mahasiswa.getLockId()));
		lockId.setWidth("90%");

		if (mahasiswa.getDikunci() != null) {
			Common.freezeGanti(center, true);
		}

		return borderlayout;
	}

	private Borderlayout initBeasiswa(final Mahasiswa mahasiswa) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyWindow window = new MyWindow("", "none", false);
		window.setHeight("100%");
		window.setWidth("100%");
		window.setParent(center);
		MyInclude iframe = new MyInclude("/pages/master/beasiswa/beasiswa_utk_mhs.zul?mahasiswa=" + mahasiswa.getId());
		iframe.setParent(window);

		return borderlayout;
	}

	private Borderlayout initCuti(final Mahasiswa mahasiswa) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyWindow window = new MyWindow("", "none", false);
		window.setHeight("100%");
		window.setWidth("100%");
		window.setParent(center);
		MyInclude iframe = new MyInclude("/pages/master/pendaftaran_cuti_mahasiswa.zul?mahasiswa=" + mahasiswa.getId());
		iframe.setParent(window);

		return borderlayout;
	}

	private void init(final Mahasiswa mahasiswa) throws Exception {
		initInternal(reloadMahasiswaForEdit(mahasiswa));
	}

	private Mahasiswa reloadMahasiswaForEdit(Mahasiswa mahasiswa) {
		if (mahasiswa == null) {
			return new Mahasiswa();
		}
		try {
			if (mahasiswa.getId() != null) {
				Mahasiswa fresh = (Mahasiswa) HibernateUtil.currentSession().get(Mahasiswa.class, mahasiswa.getId());
				if (fresh != null) {
					return fresh;
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) MahasiswaAction.reloadMahasiswaForEdit");
		}
		return mahasiswa;
	}

	private boolean tampilkanTabPindahan(Mahasiswa mahasiswa) {
		if (mahasiswa == null) {
			return false;
		}
		try {
			StatusAwalMahasiswa statusAwal = mahasiswa.getStatusAwalMahasiswa();
			return statusAwal != null && Boolean.TRUE.equals(statusAwal.getPindahan());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) MahasiswaAction.tampilkanTabPindahan");
			return false;
		}
	}

	private boolean tampilkanTabAlihProdi(Mahasiswa mahasiswa) {
		if (mahasiswa == null) {
			return false;
		}
		try {
			StatusAwalMahasiswa statusAwal = mahasiswa.getStatusAwalMahasiswa();
			return statusAwal != null && Boolean.TRUE.equals(statusAwal.getAlihProdi());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) MahasiswaAction.tampilkanTabAlihProdi");
			return false;
		}
	}

	private void initInternal(final Mahasiswa mahasiswa) throws Exception {
		try {
			this.mahasiswa = mahasiswa;
			biodataMahasiswaAction = new BiodataMahasiswaAction();
			biodataMahasiswaAction.setTampilFotoBiodata(false);

			Common.clear(addWindow);

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(center);
			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			final MyTabConfig tabData = new MyTabConfig("Data Mahasiswa");
			tabData.setParent(tabs);

			tabPindahan = new MyTabConfig("Keterangan Mahasiswa Pindahan");
			tabPindahan.setVisible(tampilkanTabPindahan(mahasiswa));
			tabPindahan.setParent(tabs);

			tabAlihProdi = new MyTabConfig("Keterangan Mahasiswa Alih Prodi");
			tabAlihProdi.setVisible(tampilkanTabAlihProdi(mahasiswa));
			tabAlihProdi.setParent(tabs);

			MyTabConfig tabBiodata = new MyTabConfig("Biodata Lengkap");
			tabBiodata.setParent(tabs);

			MyTabConfig tabBeasiswa = new MyTabConfig("Beasiswa");
			tabBeasiswa.setParent(tabs);

			MyTabConfig tabCuti = new MyTabConfig("Cuti");
			tabCuti.setParent(tabs);

			MyTabConfig tabInformasi = new MyTabConfig("Informasi Kelulusan");
			tabInformasi.setParent(tabs);

			MyTabConfig tabAlumni = new MyTabConfig("Informasi Alumni");
			tabAlumni.setParent(tabs);

			MyTabConfig tabLoginOrtu = new MyTabConfig("Login Orang Tua");
			tabLoginOrtu.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			tabpanel.setParent(tabpanels);
			tabpanel.appendChild(initMain(mahasiswa));

			final Tabpanel tabpanelPindahan = new ais.ui.util.MyTabpanel();
			tabpanelPindahan.setParent(tabpanels);
			tabpanelPindahan.appendChild(initPindahan(mahasiswa));

			final Tabpanel tabpanelAlihProdi = new ais.ui.util.MyTabpanel();
			tabpanelAlihProdi.setParent(tabpanels);
			tabpanelAlihProdi.appendChild(initAlihProdi(mahasiswa));

			final Tabpanel tabpanelBiodata = new ais.ui.util.MyTabpanel();
			tabpanelBiodata.setParent(tabpanels);
			EventListener tabBiodataListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelBiodata.getChildren().isEmpty()) {
						if (!onSave(arg0, false)) {
							tabData.setSelected(true);
							Common.clear(tabpanelBiodata);
							return;
						}
						tabpanelBiodata.appendChild(initBiodata(mahasiswa));
					}
				}
			};
			tabBiodata.addEventListener("onClick", tabBiodataListener);
			tabBiodata.addEventListener("onSelect", tabBiodataListener);

			final Tabpanel tabpanelBeasiswa = new ais.ui.util.MyTabpanel();
			tabpanelBeasiswa.setParent(tabpanels);

			EventListener tabBeasiswaListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelBeasiswa.getChildren().isEmpty()) {
						if (MahasiswaAction.this.mahasiswa == null || MahasiswaAction.this.mahasiswa.getId() == null) {
							if (!onSave(arg0, false)) {
								tabData.setSelected(true);
								Common.clear(tabpanelBeasiswa);
								return;
							}
						}
						tabpanelBeasiswa.appendChild(initBeasiswa(MahasiswaAction.this.mahasiswa));
					}
				}
			};
			tabBeasiswa.addEventListener("onClick", tabBeasiswaListener);
			tabBeasiswa.addEventListener("onSelect", tabBeasiswaListener);

			final Tabpanel tabpanelCuti = new ais.ui.util.MyTabpanel();
			tabpanelCuti.setParent(tabpanels);

			EventListener tabCutiListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelCuti.getChildren().isEmpty()) {
						if (MahasiswaAction.this.mahasiswa == null || MahasiswaAction.this.mahasiswa.getId() == null) {
							if (!onSave(arg0, false)) {
								tabData.setSelected(true);
								Common.clear(tabpanelCuti);
								return;
							}
						}
						tabpanelCuti.appendChild(initCuti(MahasiswaAction.this.mahasiswa));
					}
				}
			};
			tabCuti.addEventListener("onClick", tabCutiListener);
			tabCuti.addEventListener("onSelect", tabCutiListener);

			tabpanel = new ais.ui.util.MyTabpanel();
			tabpanel.setParent(tabpanels);
			tabpanel.appendChild(initKelulusan(mahasiswa));

			final Tabpanel tabpanelAlumni = new ais.ui.util.MyTabpanel();
			tabpanelAlumni.setParent(tabpanels);

			EventListener tabAlumniListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelAlumni.getChildren().isEmpty()) {
						if (MahasiswaAction.this.mahasiswa == null || MahasiswaAction.this.mahasiswa.getId() == null) {
							if (!onSave(arg0, false)) {
								tabData.setSelected(true);
								Common.clear(tabpanelAlumni);
								return;
							}
						}
						tabpanelAlumni.appendChild(initAlumni(MahasiswaAction.this.mahasiswa));
					}
				}
			};
			tabAlumni.addEventListener("onClick", tabAlumniListener);
			tabAlumni.addEventListener("onSelect", tabAlumniListener);

			tabpanel = new ais.ui.util.MyTabpanel();
			tabpanel.setParent(tabpanels);
			tabpanel.appendChild(initLoginOrtu(mahasiswa));

			ais.ui.util.MyButtonTabbox.gantiTabboxNative(tabbox, new int[] { 1 });

			South south = new South();
			ais.ui.util.ZkCompat.setFlex(south, true);
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			// toolbar.setHeight("25px");
			toolbar.setParent(south);
			final MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					addWindow.setVisible(false);

					if (MahasiswaAction.this.eventListener != null) {
						MahasiswaAction.this.eventListener
								.onEvent(new Event("", cancel, MahasiswaAction.this.mahasiswa));
					}
				}
			});
			cancel.setParent(toolbar);
			final MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (onSave(event, true)) {
						addWindow.setVisible(false);

						if (MahasiswaAction.this.eventListener != null) {
							MahasiswaAction.this.eventListener
									.onEvent(new Event("", save, MahasiswaAction.this.mahasiswa));
						}
					}
				}
			});
			save.setParent(toolbar);
			borderlayout.setParent(addWindow);

			if (mahasiswa.getDikunci() != null) {
				save.setDisabled(true);
				save.setTooltiptext("Data mahasiswa sedang dikunci oleh "
						+ mahasiswa.getDikunci().toString() + ". Silakan buka kunci terlebih dahulu untuk menyimpan.");
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

	}

	private boolean checkNim(String nim) {
		Session session = HibernateUtil.currentSession();
		Integer count = 0;
		if (mahasiswa == null || mahasiswa.getId() == null) {
			count = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.eq("nim", nim.trim()))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		} else {
			count = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.eq("nim", nim.trim()))
					.add(Restrictions.ne("id", mahasiswa.getId())).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();
		}

		return !count.equals(0);
	}

	public boolean onSave(Event event, final boolean reload) throws Exception {

		if (nim.getValue().trim().equals("")) {
			String nimSemula = ambilNimSemulaDariDatabase(mahasiswa);
			if (nimSemula != null && !nimSemula.trim().isEmpty()) {
				nim.setValue(nimSemula.trim());
			}
		}

		if (nim.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon maaf, NIM (Nomor Induk Mahasiswa) belum diisi. Langkah yang dapat dilakukan: (1) isikan NIM mahasiswa pada kolom yang tersedia; (2) pastikan NIM sesuai dengan data resmi mahasiswa; (3) setelah NIM terisi, silakan menyimpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon maaf, Nama mahasiswa belum diisi. Langkah yang dapat dilakukan: (1) isikan nama lengkap mahasiswa pada kolom yang tersedia; (2) pastikan penulisan nama sesuai dengan dokumen resmi; (3) setelah nama terisi, silakan menyimpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Program (jenjang/program studi) belum dipilih. Langkah yang dapat dilakukan: (1) pilih salah satu Program pada daftar pilihan yang tersedia; (2) pastikan Program yang dipilih sesuai dengan data mahasiswa; (3) setelah Program terpilih, silakan menyimpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kewarganegaraan.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Kewarganegaraan belum dipilih. Langkah yang dapat dilakukan: (1) pilih Kewarganegaraan mahasiswa pada daftar pilihan yang tersedia; (2) pastikan pilihan sesuai dengan data resmi mahasiswa; (3) setelah Kewarganegaraan terpilih, silakan menyimpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, {V1} belum dipilih. Langkah yang dapat dilakukan: (1) pilih {V1} mahasiswa pada daftar pilihan yang tersedia; (2) pastikan pilihan sesuai dengan data mahasiswa; (3) setelah {V1} terpilih, silakan menyimpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
					Common.getBahasaConfig("Jurusan"));
			return false;
		}

		if (semesterMulai.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Semester mulai belajar belum dipilih. Langkah yang dapat dilakukan: (1) pilih Semester mulai belajar mahasiswa pada daftar pilihan yang tersedia; (2) pastikan semester yang dipilih sesuai dengan riwayat masuk mahasiswa; (3) setelah semester terpilih, silakan menyimpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (tahunangkatan.getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Tahun angkatan belum diisi. Langkah yang dapat dilakukan: (1) isikan Tahun angkatan mahasiswa pada kolom yang tersedia; (2) pastikan tahun yang diisi sesuai dengan data resmi mahasiswa; (3) setelah tahun angkatan terisi, silakan menyimpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (tanggalMasuk.getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Tanggal masuk perguruan tinggi belum diisi. Langkah yang dapat dilakukan: (1) isikan Tanggal masuk perguruan tinggi pada kolom tanggal yang tersedia; (2) pastikan tanggal yang diisi sesuai dengan dokumen resmi mahasiswa; (3) setelah tanggal terisi, silakan menyimpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (statusKeluar != null && statusKeluar.getSelectedItem() != null
				&& statusKeluar.getSelectedItem().getValue() != null) {
			if (semesterLulus != null && (semesterLulus.getSelectedItem() == null
					|| semesterLulus.getSelectedItem().getValue() == null)) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, apabila status keluar mahasiswa telah diisi (dinyatakan \"{V1}\"), maka data \"Semester Lulus / Keluar / Mengundurkan Diri / DO\" wajib diisi secara benar. Langkah yang dapat dilakukan: (1) buka bagian data Semester Lulus / Keluar / Mengundurkan Diri / DO; (2) pilih semester yang sesuai dengan status keluar mahasiswa tersebut; (3) setelah data terisi dengan benar, silakan menyimpan kembali data ini.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						((StatusKeluar) statusKeluar.getSelectedItem().getValue()).getNama());
				return false;
			}
		}

		if (checkNim(nim.getValue().trim())) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, mahasiswa dengan NIM \"{V1}\" dan nama \"{V2}\" telah terdaftar sebelumnya di dalam basis data, sehingga data ini tidak dapat disimpan ulang. Langkah yang dapat dilakukan: (1) periksa kembali NIM yang Bapak/Ibu masukkan; (2) apabila ingin mengubah data mahasiswa tersebut, gunakan fitur Ubah pada data yang telah ada; (3) apabila NIM keliru, perbaiki NIM kemudian simpan kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, nim.getValue().trim(),
					nama.getValue());
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (mahasiswa.getId() != null) {
			mahasiswa = (Mahasiswa) session.load(Mahasiswa.class, mahasiswa.getId());
		}
		mahasiswa.setSemesterLulus(
				(Integer) (semesterLulus.getValue() == null ? null : semesterLulus.getSelectedItem().getValue()));
		mahasiswa.setStatusAwalMahasiswa((StatusAwalMahasiswa) (statusAwalMahasiswa.getSelectedItem() == null ? null
				: statusAwalMahasiswa.getSelectedItem().getValue()));

		if (mahasiswa.getMerupakanPindahan() && pindahanDariKampus.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"Mohon maaf, karena mahasiswa ini merupakan mahasiswa pindahan, maka data asal kampus (pindah dari kampus) wajib diisi. Langkah yang dapat dilakukan: (1) buka tab Pindahan; (2) isikan nama kampus asal mahasiswa pindahan pada kolom yang tersedia; (3) setelah data terisi, silakan menyimpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			tabPindahan.setSelected(true);
			pindahanDariKampus.focus();
			return false;
		}

		if (mahasiswa.getMerupakanPindahan() && namaProdiPindah.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"Mohon maaf, karena mahasiswa ini merupakan mahasiswa pindahan, maka nama program studi lama (prodi asal) wajib diisi. Langkah yang dapat dilakukan: (1) buka tab Pindahan; (2) isikan nama program studi lama mahasiswa pada kolom yang tersedia; (3) setelah data terisi, silakan menyimpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			tabPindahan.setSelected(true);
			sksYangDiakui.focus();
			return false;
		}

		if (statusKeluar.getSelectedItem() != null && statusKeluar.getSelectedItem().getValue() != null
				&& (semesterLulus.getValue() == null || semesterLulus.getSelectedItem().getValue() == null)) {
			MyMessageboxConfig.show(
					"Mohon maaf, apabila status keluar (lulus) mahasiswa telah diisi, maka data Semester keluar / lulus / DO wajib diisi. Langkah yang dapat dilakukan: (1) buka bagian data Semester keluar / lulus / DO; (2) pilih semester yang sesuai dengan status keluar mahasiswa; (3) setelah data terisi, silakan menyimpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (mahasiswa.getMerupakanPindahan() && nimPindahan.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"Mohon maaf, karena mahasiswa ini merupakan mahasiswa pindahan, maka NIM lama (NIM asal) wajib diisi. Langkah yang dapat dilakukan: (1) buka tab Pindahan; (2) isikan NIM lama mahasiswa pada kolom yang tersedia; (3) setelah data terisi, silakan menyimpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			tabPindahan.setSelected(true);
			nimPindahan.focus();
			return false;
		}

		if (mahasiswa.getMerupakanPindahan() && (sksYangDiakui.getValue() == null)) {
			MyMessageboxConfig.show(
					"Mohon maaf, karena mahasiswa ini merupakan mahasiswa pindahan, maka jumlah SKS yang diakui wajib diisi. Langkah yang dapat dilakukan: (1) buka tab Pindahan; (2) isikan jumlah SKS yang diakui pada kolom yang tersedia; (3) setelah data terisi, silakan menyimpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			tabPindahan.setSelected(true);
			sksYangDiakui.focus();
			return false;
		}

		if (mahasiswa.getMerupakanPindahan() && tanggalPindah.getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, karena mahasiswa ini merupakan mahasiswa pindahan, maka tanggal pindah wajib diisi. Langkah yang dapat dilakukan: (1) buka tab Pindahan; (2) isikan tanggal pindah mahasiswa pada kolom tanggal yang tersedia; (3) setelah data terisi, silakan menyimpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			tabPindahan.setSelected(true);
			tanggalPindah.focus();
			return false;
		}

		// if (mahasiswa.getMerupakanAlihProdi() &&
		// alihProdiMahasiswa.getAttribute("mahasiswa") == null) {
		// MyMessageboxConfig.show("Apabila mahasiswa alih prodi, NIM lama
		// sebelum beralih prodi harus diisi",
		// "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// tabAlihProdi.setSelected(true);
		// alihProdiMahasiswa.focus();
		// return false;
		// }

		// if (mahasiswa.getMerupakanAlihProdi()
		// && (sksYangDiakuiPindahProdi.getValue() == null ||
		// sksYangDiakuiPindahProdi.getValue().equals(0))) {
		// MyMessageboxConfig.show("Apabila mahasiswa alih prodi, jumlah SKS
		// yang diakui harus diisi", "Peringatan",
		// MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// tabAlihProdi.setSelected(true);
		// sksYangDiakuiPindahProdi.focus();
		// return false;
		// }

		if (mahasiswa.getMerupakanAlihProdi() && tanggalPindahProdi.getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, karena mahasiswa ini merupakan mahasiswa alih program studi, maka tanggal alih program studi wajib diisi. Langkah yang dapat dilakukan: (1) buka tab Alih Prodi; (2) isikan tanggal alih program studi pada kolom tanggal yang tersedia; (3) setelah data terisi, silakan menyimpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			tabAlihProdi.setSelected(true);
			tanggalPindahProdi.focus();
			return false;
		}

		if (userOrtu.getValue() != null && !userOrtu.getValue().trim().equals("")) {
			if (passOrtu.getValue().trim().equals("")) {
				MyMessageboxConfig.show(
						"Mohon maaf, karena nama pengguna (username) orang tua telah diisi, maka kata sandi (password) orang tua wajib diisi. Langkah yang dapat dilakukan: (1) isikan kata sandi orang tua pada kolom yang tersedia; (2) pastikan kata sandi mudah diingat namun tetap aman; (3) setelah kata sandi terisi, silakan menyimpan kembali data ini.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}

			boolean i = Common.checkUsername(userOrtu.getValue(), null, mahasiswa.getId());
			if (i) {
				MyMessageboxConfig.show(
						"Mohon maaf, nama pengguna (username) orang tua yang Bapak/Ibu masukkan telah digunakan oleh pengguna lain. Langkah yang dapat dilakukan: (1) tentukan nama pengguna lain yang belum pernah digunakan; (2) sebaiknya gunakan kombinasi yang unik agar tidak bentrok; (3) setelah nama pengguna diperbaiki, silakan menyimpan kembali data ini.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}

			mahasiswa.setUserOrtu(userOrtu.getValue().trim());
			mahasiswa.setPassOrtu(Common.desEncrypter.get().encrypt(passOrtu.getValue().trim()));

			try {
				Common.saveOrUpdateUserAccess(null, mahasiswa, mahasiswa.getUserOrtu(), passOrtu.getValue().trim(),
						mahasiswa.getEmail());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (predikatKelulusan != null) {
			mahasiswa.setPredikatKelulusan((Judisium) (predikatKelulusan.getSelectedItem() == null ? null
					: predikatKelulusan.getSelectedItem().getValue()));
		}

		mahasiswa.setStatusDomisiliSetelahLulus(
				(StatusDomisiliSetelahLulus) (statusDomisiliSetelahLulus.getSelectedItem() == null ? null
						: statusDomisiliSetelahLulus.getSelectedItem().getValue()));

		mahasiswa.setStatusSetelahLulus((StatusSetelahLulus) (statusSetelahLulus.getSelectedItem() == null ? null
				: statusSetelahLulus.getSelectedItem().getValue()));

		mahasiswa.setStatusPekerjaanSetelahLulus(
				(StatusPekerjaanSetelahLulus) (statusPekerjaanSetelahLulus.getSelectedItem() == null ? null
						: statusPekerjaanSetelahLulus.getSelectedItem().getValue()));

		mahasiswa.setUsernameOjs(usernameOjs.getValue());
		mahasiswa.setFeeder(feeder.getValue().trim());
		mahasiswa.setIdRegPd(idRegPd.getValue().trim());

		mahasiswa.setSksYangDiakui(sksYangDiakui.getValue());
		mahasiswa.setSksYangDiakuiPindahProdi(sksYangDiakuiPindahProdi.getValue());

		mahasiswa.setNamaProdiPindah(namaProdiPindah.getValue().trim());

		mahasiswa.setBlnAwalBimbingan(blnAwalBimbingan.getValue());
		mahasiswa.setBlnAkhirBimbingan(blnAkhirBimbingan.getValue());

		mahasiswa.setStatusKeluar((StatusKeluar) (statusKeluar.getSelectedItem() == null ? null
				: statusKeluar.getSelectedItem().getValue()));

		mahasiswa.setTanggalMasuk(tanggalMasuk.getValue());
		mahasiswa.setTanggalKegiatanBelajarMengajar(tanggalKegiatanBelajarMengajar.getValue());
		mahasiswa.setTanggalSkRektor(tanggalSkRektor.getValue());

		if (mahasiswa.getMerupakanAlihProdi()) {
			mahasiswa.setAlihProdiMahasiswa((Mahasiswa) alihProdiMahasiswa.getAttribute("mahasiswa"));
		} else {
			mahasiswa.setAlihProdiMahasiswa(null);
		}
		mahasiswa.setJenisSeleksi((JenisSeleksi) (jenisSeleksi.getSelectedItem() == null ? null
				: jenisSeleksi.getSelectedItem().getValue()));
		mahasiswa.setJenisPembiayaanMahasiswa(
				(JenisPembiayaanMahasiswa) (jenisPembiayaanMahasiswa.getSelectedItem() == null ? null
						: jenisPembiayaanMahasiswa.getSelectedItem().getValue()));
		mahasiswa.setTanggalPindahProdi(tanggalPindahProdi.getValue());
		mahasiswa
				.setPindahKeProdiIniMasukSemester((Integer) (pindahKeProdiIniMasukSemester.getSelectedItem() == null ? 0
						: pindahKeProdiIniMasukSemester.getSelectedItem().getValue()));

		mahasiswa.setKeteranganPindahProdi(keteranganPindahProdi.getValue());

		mahasiswa.setNimPindahan(nimPindahan.getValue());
		mahasiswa.setPindahanDariKampus(pindahanDariKampus.getValue());
		mahasiswa.setPindahDariKampusLamaDiSemester(
				(Integer) (pindahDariKampusLamaDiSemester.getSelectedItem() == null ? 0
						: pindahDariKampusLamaDiSemester.getSelectedItem().getValue()));
		mahasiswa.setPindahKeKampusIniMasukSemester(
				(Integer) (pindahKeKampusIniMasukSemester.getSelectedItem() == null ? 0
						: pindahKeKampusIniMasukSemester.getSelectedItem().getValue()));
		mahasiswa.setTanggalPindah(tanggalPindah.getValue());

		mahasiswa.setSemesterLulus(
				(Integer) (semesterLulus.getValue() == null ? null : semesterLulus.getSelectedItem().getValue()));
		mahasiswa.setTahunLulus(
				(Integer) (tahunLulus.getValue() == null ? null : tahunLulus.getSelectedItem().getValue()));
		mahasiswa.setKtp(ktp.getValue());
		mahasiswa.setWarganegara((String) kewarganegaraan.getSelectedItem().getValue());

		mahasiswa.setNegara((Negara) (negara.getAttribute("negara")));
		mahasiswa.setKeterangan(keterangan.getValue());
		mahasiswa.setTanggalLulus(tanggalLulus.getValue());
		mahasiswa.setTanggalYudisium(tanggalYudisium.getValue());
		mahasiswa.setTanggalWisuda(tanggalWisuda.getValue());
		mahasiswa.setKonsentrasi((Konsentrasi) (konsentrasi.getSelectedItem() == null ? null
				: konsentrasi.getSelectedItem().getValue()));
		mahasiswa.setWaktuKuliah(
				(String) (waktuKuliah.getSelectedItem() == null ? null : waktuKuliah.getSelectedItem().getValue()));

		mahasiswa.setNim(nim.getValue().trim());

		mahasiswa.setAlamat(alamat.getValue());
		mahasiswa
				.setKelamin(kelamin.getSelectedItem() == null ? null : kelamin.getSelectedItem().getValue().toString());
		mahasiswa.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		mahasiswa.setNama(nama.getValue());
		mahasiswa.setTanggallahir(tanggallahir.getValue());
		mahasiswa.setTempatlahir(tempatlahir.getValue());
		mahasiswa.setTahunangkatan(tahunangkatan.getValue() == null ? null : tahunangkatan.getValue().intValue());

		mahasiswa.setSemesterMulai(
				(String) (semesterMulai.getSelectedItem() == null ? null : semesterMulai.getSelectedItem().getValue()));

		mahasiswa.setStatusAwalMahasiswa((StatusAwalMahasiswa) (statusAwalMahasiswa.getSelectedItem() == null ? null
				: statusAwalMahasiswa.getSelectedItem().getValue()));

		mahasiswa.setPass(Common.desEncrypter.get().encrypt(password.getValue().trim()));

		mahasiswa.setBerat_badan(berat_badan.getValue() == null ? null : berat_badan.getValue().intValue());
		mahasiswa.setTinggi_badan(tinggi_badan.getValue() == null ? null : tinggi_badan.getValue().intValue());
		mahasiswa.setGolongan_darah(golongan_darah.getValue() == null ? null : golongan_darah.getValue().trim());
		mahasiswa.setProgram((String) program.getSelectedItem().getValue());

		mahasiswa.setNoIjazah1(noIjazah1.getValue().trim());
		mahasiswa.setNoIjazah2(noIjazah2.getValue().trim());
		mahasiswa.setNoAkta1(noAkta1.getValue().trim());
		mahasiswa.setNoAkta2(noAkta2.getValue().trim());
		mahasiswa.setSkDo(skDo.getValue().trim());
		mahasiswa.setTahunWisuda(tahunWisuda.getValue());
		mahasiswa.setJudulSkripsi(judulSkripsi.getValue().trim());
		mahasiswa.setTanggalYudisium(tanggalYudisium.getValue());
		mahasiswa.setLockId(lockId.getValue().trim());
		mahasiswa.setLinkValidasiEksternal(linkValidasiEksternal.getValue());

		if (tanggallahirManual != null) {
			mahasiswa.setTanggallahirManual(tanggallahirManual.getValue());
		}

		mahasiswa.setAktif(aktif.isChecked());
		mahasiswa.setTidakAdaTagihan(tidakAdaTagihan.isChecked());

		if (dosen.getAttribute("myValue") != null) {
			mahasiswa.setDosen(((Dosen) dosen.getAttribute("myValue")).getId());
		}
		mahasiswa.setKelas(kelas.getValue());
		mahasiswa.setNomorSkpi(nomorSkpi.getValue());
		mahasiswa.setIdfinger(idfinger.getValue());
		mahasiswa.setBatasStudi(batasStudi.getValue());
		mahasiswa.setStatusKrs(statusKrs.getValue());

		mahasiswa.setPindahanDari((PerguruanTinggiLain) pindahanDari.getAttribute("perguruanTinggiLain"));

		mahasiswa.setStatusAwalSelaluIkutDataUtama(statusAwalSelaluIkutDataUtama.isChecked());
		mahasiswa.setDosenPaSelaluSama(dosenPaSelaluSama.isChecked());
		mahasiswa.setKelasSelaluSama(kelasSelaluSama.isChecked());

		mahasiswa.setJudulSkripsiEn(judulSkripsiEn.getValue());

		// Simpan daftar atasan (JSON) yang diedit di tab Kelulusan.
		if (atasanHelper != null) {
			mahasiswa.setAtasans(atasanHelper.serialize());
		}

		mahasiswa.setStatusAwalMahasiswaSetelahSmtTertentu(
				(StatusAwalMahasiswa) (statusAwalMahasiswaSetelahSmtTertentu.getSelectedItem() == null ? null
						: statusAwalMahasiswaSetelahSmtTertentu.getSelectedItem().getValue()));
		mahasiswa.setSmtStatusAwal(smtStatusAwal.getValue() == null ? null : smtStatusAwal.getValue().intValue());
		mahasiswa.setStatusAwalMahasiswaSetelahSmtTertentuLagi(
				(StatusAwalMahasiswa) (statusAwalMahasiswaSetelahSmtTertentuLagi.getSelectedItem() == null ? null
						: statusAwalMahasiswaSetelahSmtTertentuLagi.getSelectedItem().getValue()));

		mahasiswa.setSmtStatusAwalLagi(
				smtStatusAwalLagi.getValue() == null ? null : smtStatusAwalLagi.getValue().intValue());

		mahasiswa.setPindahkanKrsDanNilaiKeMahasiswaAlihProdi(pindahkanKrsDanNilaiKeMahasiswaAlihProdi.isChecked());

		mahasiswa.setNamaArab(namaArab.getValue());
		mahasiswa.setNamaTionghoa(namaTionghoa.getValue());

		mahasiswa.setProgramSelaluIkutDataUtama(programSelaluIkutDataUtama.isChecked());

		// Sumber aturan bisnis bersama: validasi yang sama juga dijalankan oleh
		// MahasiswaGenericCrudAdapter pada New UI, tanpa ketergantungan komponen ZK.
		List<String> newUiSharedRuleErrors = MahasiswaExistingBusinessRules.validate(session, mahasiswa);
		if (!newUiSharedRuleErrors.isEmpty()) {
			String firstRuleError = String.valueOf(newUiSharedRuleErrors.get(0));
			int separator = firstRuleError.indexOf(':');
			MyMessageboxConfig.show(separator < 0 ? firstRuleError : firstRuleError.substring(separator + 1),
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		MahasiswaExistingBusinessRules.applyPersistenceDefaults(mahasiswa);

		StatusKeluar sk = (StatusKeluar) (statusKeluar == null || statusKeluar.getSelectedItem() == null ? null
				: statusKeluar.getSelectedItem().getValue());
		Integer smtLus = (Integer) (semesterLulus == null || semesterLulus.getSelectedItem() == null ? null
				: semesterLulus.getSelectedItem().getValue());
		Jurusan jrs = (Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
				? null
				: jurusan.getSelectedItem().getValue());

		if (sk != null && smtLus != null && jrs != null && sk.getId() != null && sk.getId().equals(1L)) {
			if ((jrs.getJenjang() != null && !mahasiswa.getMerupakanPindahan() && !mahasiswa.getMerupakanAlihProdi()
					&& jrs.getJenjang().getJumlahSemesterLulus() > smtLus)) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, Semester lulus yang Bapak/Ibu masukkan ({V2}) belum memenuhi ketentuan. Semester lulus minimal untuk jenjang ini adalah {V1}. Langkah yang dapat dilakukan: (1) periksa kembali jumlah semester lulus yang diisikan; (2) sesuaikan nilai Semester lulus agar sekurang-kurangnya {V1}; (3) setelah nilai sesuai, silakan menyimpan kembali data ini.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						jrs.getJenjang().getJumlahSemesterLulus(), smtLus);
				return false;
			}
		}

		if (mahasiswa.getId() != null) {
			if (mahasiswa.getPass() == null) {
				mahasiswa.setPass(Common.desEncrypter.get().encrypt(mahasiswa.getNim()));
				mahasiswa.setIs_encripted(true);
			}
			Common.refreshUpdate(session, mahasiswa);
		} else {
			mahasiswa.setIs_encripted(true);
			mahasiswa.setPass(Common.desEncrypter.get().encrypt(mahasiswa.getNim()));
			Common.refreshSaveOrUpdate(session, mahasiswa);
		}

		if (mahasiswa.getAlihProdiMahasiswa() != null) {
			Mahasiswa mahasiswaLama = mahasiswa.getAlihProdiMahasiswa();
			session.refresh(mahasiswaLama);
			mahasiswaLama.setNimBaruPindah(mahasiswa.getNim());
			Common.refreshUpdate(session, mahasiswaLama);
		} else if (alihProdiMahasiswa.getAttribute("mahasiswa") != null) {
			Mahasiswa mahasiswaLama = (Mahasiswa) alihProdiMahasiswa.getAttribute("mahasiswa");
			session.refresh(mahasiswaLama);
			mahasiswaLama.setNimBaruPindah(null);
			Common.refreshUpdate(session, mahasiswaLama);
		}

		session.flush();

		try {
			biodataMahasiswaAction.onSave(mahasiswa, false, emailAtasan.getValue(), parameterTambahanAlumniListener);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (mahasiswa.getAlihProdiMahasiswa() != null && pindahkanKrsDanNilaiKeMahasiswaAlihProdi.isChecked()) {
					List<Long> detailperkuliahansId = mahasiswa.getAlihProdiMahasiswa().ambilDetailperkuliahan();
					Session session = HibernateUtil.currentSession();
					for (Long oid : detailperkuliahansId) {
						Detailperkuliahan o = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								oid.toString());
						if (o != null) {
							session.refresh(o);
							o.setMahasiswa(mahasiswa);
							Common.refreshUpdate(session, o);
							session.flush();
						}
					}
					mahasiswa.reInitDetailperkuliahan(session);
					Common.singkronkanKrsMahasiswaRefresh(mahasiswa);
					mahasiswa.getAlihProdiMahasiswa().reInitDetailperkuliahan(session);
					Common.singkronkanKrsMahasiswaRefresh(mahasiswa.getAlihProdiMahasiswa());
				} else if (mahasiswa.getAlihProdiMahasiswa() != null
						&& !pindahkanKrsDanNilaiKeMahasiswaAlihProdi.isChecked()) {
					List<Long> detailperkuliahansId = mahasiswa.ambilDetailperkuliahan();
					Session session = HibernateUtil.currentSession();
					for (Long oid : detailperkuliahansId) {
						Detailperkuliahan o = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								oid.toString());
						if (o != null) {
							session.refresh(o);
							o.setMahasiswa(mahasiswa.getAlihProdiMahasiswa());
							Common.refreshUpdate(session, o);
							session.flush();
						}
					}
					mahasiswa.reInitDetailperkuliahan(session);
					Common.singkronkanKrsMahasiswaRefresh(mahasiswa);
					mahasiswa.getAlihProdiMahasiswa().reInitDetailperkuliahan(session);
					Common.singkronkanKrsMahasiswaRefresh(mahasiswa.getAlihProdiMahasiswa());
				}

				if (fileFoto != null && fileFoto.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(fileFoto);
						fileFoto.setMahasiswa(mahasiswa.getId());

						session.getTransaction().begin();
						session.update(fileFoto);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}

				}

				if (fileFotoLulus != null && fileFotoLulus.getId() != null) {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(fileFotoLulus);
						fileFotoLulus.setMahasiswa(mahasiswa.getId());

						session.getTransaction().begin();
						session.update(fileFotoLulus);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}

				}

				List<Integer> smt = new ArrayList<Integer>();
				if (ubahKeSemuaSemester.isChecked()) {
					for (int i = 1; i <= mahasiswa.currentSemester(); i++) {
						smt.add(i);
					}
				} else {
					smt.add(mahasiswa.currentSemester());
				}

				Dosen dosenPa = (Dosen) dosen.getAttribute("myValue");
				String kelas = mahasiswa.getKelas();
				for (Integer s : smt) {
					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, s, null, null);
					if (((dosenPa != null && krsMahasiswa.getDosenPa() == null)
							|| (dosenPa != null && !krsMahasiswa.getDosenPa().getId().equals(dosenPa.getId()))
							|| !krsMahasiswa.getKelas().equalsIgnoreCase(kelas))) {
						krsMahasiswa.setDosenPa(dosenPa);
						krsMahasiswa.setKelas(kelas);
						Common.refreshSaveOrUpdate(krsMahasiswa);
					}

					Common.saveOrUpdateUserAccess(null, mahasiswa, mahasiswa.getNim(), password.getValue().trim(),
							mahasiswa.getEmail());
				}

				paksaRiwayatStatusTetapAktifJikaStatusAwalDariKelompok(mahasiswa);

				if (reload) {

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							onSearchDefault(null);

						}
					});

				}

			}
		});

		return true;
	}

	private static void paksaRiwayatStatusTetapAktifJikaStatusAwalDariKelompok(Mahasiswa mahasiswa) {
		if (mahasiswa == null || mahasiswa.getId() == null || ConstantValues.AKTIF == null) {
			return;
		}
		try {
			if (mahasiswa.getKelompokMahasiswa() == null
					|| (mahasiswa.getKelompokMahasiswa().getStatusAwalMahasiswa() == null
							&& mahasiswa.getKelompokMahasiswa().getStatusAwalMahasiswa2() == null
							&& mahasiswa.getKelompokMahasiswa().getStatusAwalMahasiswa3() == null)) {
				return;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return;
		}

		Session session = null;
		org.hibernate.Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Mahasiswa mhs = (Mahasiswa) session.get(Mahasiswa.class, mahasiswa.getId());
			if (mhs == null || mhs.getKelompokMahasiswa() == null) {
				return;
			}
			@SuppressWarnings("unchecked")
			List<HistoryStatusMahasiswa> histories = session.createCriteria(HistoryStatusMahasiswa.class)
					.add(Restrictions.eq("mahasiswa", mhs))
					.add(Restrictions.isNull("sp"))
					.list();
			tx = session.beginTransaction();
			for (HistoryStatusMahasiswa history : histories) {
				if (history == null || statusFinalMahasiswa(history.getStatusMahasiswa())) {
					continue;
				}
				if (!statusMahasiswaSama(history.getStatusMahasiswa(), ConstantValues.AKTIF)) {
					history.setStatusMahasiswa(ConstantValues.AKTIF);
					session.saveOrUpdate(history);
				}
			}
			tx.commit();
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception ignored) {
					ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) MahasiswaAction.paksaAktif.rollback");
				}
			}
			Common.tampilErrorJikaAdmin(e);
		} finally {
			ais.database.hibernate.HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static boolean statusFinalMahasiswa(StatusMahasiswa statusMahasiswa) {
		return statusMahasiswaSama(statusMahasiswa, ConstantValues.LULUS)
				|| statusMahasiswaSama(statusMahasiswa, ConstantValues.DROP_OUT)
				|| statusMahasiswaSama(statusMahasiswa, ConstantValues.KELUAR);
	}

	private static boolean statusMahasiswaSama(StatusMahasiswa satu, StatusMahasiswa dua) {
		if (satu == null || dua == null || satu.getId() == null || dua.getId() == null) {
			return satu == dua;
		}
		return satu.getId().equals(dua.getId());
	}

	private static void tampilkanRiwayatStatusAwalDariKrsReguler(final Rows rowsStatusAwal,
			final Mahasiswa mahasiswa, Integer smtMaxFinal, final boolean bolehUbahStatusAwal,
			final java.util.List<StatusAwalMahasiswa> statusAwalOpts, boolean refreshHistory) {
		java.util.List<KrsMahasiswa> krsReguler = ambilKrsRegulerUntukRiwayatStatusAwal(mahasiswa, smtMaxFinal);
		for (KrsMahasiswa krs : krsReguler) {
			HistoryStatusMahasiswa hist = null;
			try {
				hist = HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(krs, refreshHistory);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			tambahBarisRiwayatStatusAwal(rowsStatusAwal, mahasiswa, krs, hist, bolehUbahStatusAwal, statusAwalOpts);
		}
		if (krsReguler.isEmpty()) {
			Row rowGrid = new Row();
			rowGrid.setParent(rowsStatusAwal);
			org.zkoss.zul.Cell cell = new org.zkoss.zul.Cell();
			cell.setColspan(bolehUbahStatusAwal ? 6 : 5);
			cell.setParent(rowGrid);
			new Label("Belum ada data KRS reguler untuk riwayat status per-semester.").setParent(cell);
		}
	}

	private static java.util.List<KrsMahasiswa> ambilKrsRegulerUntukRiwayatStatusAwal(Mahasiswa mahasiswa,
			Integer smtMaxFinal) {
		java.util.List<KrsMahasiswa> hasil = new java.util.ArrayList<KrsMahasiswa>();
		if (mahasiswa == null || mahasiswa.getId() == null) {
			return hasil;
		}
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			session.setFlushMode(org.hibernate.FlushMode.MANUAL);
			@SuppressWarnings("unchecked")
			java.util.List<KrsMahasiswa> raw = session.createCriteria(KrsMahasiswa.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.isNull("semesterPendek"))
					.add(Restrictions.gt("semester", Integer.valueOf(0)))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
					.addOrder(Order.asc("semester"))
					.addOrder(Order.asc("id"))
					.list();
			java.util.LinkedHashMap<Integer, KrsMahasiswa> perSemester =
					new java.util.LinkedHashMap<Integer, KrsMahasiswa>();
			for (KrsMahasiswa krs : raw) {
				if (krs == null || krs.getSemester() == null || krs.getSemester() <= 0) {
					continue;
				}
				if (smtMaxFinal != null && smtMaxFinal > 0 && krs.getSemester() > smtMaxFinal) {
					continue;
				}
				if (!perSemester.containsKey(krs.getSemester())) {
					krs.setMahasiswa(mahasiswa);
					perSemester.put(krs.getSemester(), krs);
				}
			}
			hasil.addAll(perSemester.values());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null && session.isOpen()) {
				session.close();
			}
		}
		return hasil;
	}

	private static void tambahBarisRiwayatStatusAwal(final Rows rowsStatusAwal, final Mahasiswa mahasiswa,
			final KrsMahasiswa krs, HistoryStatusMahasiswa hist, boolean bolehUbahStatusAwal,
			final java.util.List<StatusAwalMahasiswa> statusAwalOpts) {
		Row rowGrid = new Row();
		rowGrid.setParent(rowsStatusAwal);
		Integer semesterKrs = krs == null ? null : krs.getSemester();
		String tahunAkademikKrs = krs == null ? "" : krs.getTahunAkademik();
		new Label(tahunAkademikKrs != null ? tahunAkademikKrs : "").setParent(rowGrid);
		new Label(semesterKrs != null ? "Smt " + semesterKrs : "").setParent(rowGrid);
		new Label(hist != null && hist.getStatusMahasiswa() != null ? hist.getStatusMahasiswa().getNama() : "")
				.setParent(rowGrid);

		int smtData = semesterKrs != null ? semesterKrs : 0;
		StatusAwalMahasiswa harusnya = HistoryStatusMahasiswa.ambilStatusAwal(mahasiswa, smtData, null);
		String namaStatusAwal = hist != null && hist.getStatusAwalMahasiswa() != null
				? hist.getStatusAwalMahasiswa().getNama() : "(kosong)";
		final Label lblStatusAwal = new Label(namaStatusAwal);
		boolean tidakSesuai = harusnya != null && hist != null && hist.getStatusAwalMahasiswa() != null
				&& !harusnya.getId().equals(hist.getStatusAwalMahasiswa().getId());
		boolean kosong = hist != null && hist.getStatusAwalMahasiswa() == null && harusnya != null;
		if (tidakSesuai || kosong) {
			lblStatusAwal.setStyle("color:#dc2626;font-weight:bold");
			lblStatusAwal.setTooltiptext("Seharusnya: " + (harusnya != null ? harusnya.getNama() : "-"));
		}
		if (bolehUbahStatusAwal) {
			final Long histId = hist != null ? hist.getId() : null;
			final Long krsId = krs != null ? krs.getId() : null;
			final Long existingId = hist != null && hist.getStatusAwalMahasiswa() != null
					? hist.getStatusAwalMahasiswa().getId() : null;
			final org.zkoss.zul.Combobox cbSA = new org.zkoss.zul.Combobox();
			cbSA.setWidth("95%");
			cbSA.setReadonly(true);
			cbSA.setVisible(false);
			for (StatusAwalMahasiswa sa : statusAwalOpts) {
				org.zkoss.zul.Comboitem ci = cbSA.appendItem(sa.getNama());
				ci.setValue(sa.getId());
				if (sa.getId().equals(existingId)) {
					cbSA.setSelectedItem(ci);
				}
			}
			org.zkoss.zul.Div divSA = new org.zkoss.zul.Div();
			divSA.setWidth("100%");
			lblStatusAwal.setParent(divSA);
			cbSA.setParent(divSA);
			divSA.setParent(rowGrid);
			new Label(hist != null && hist.getProgram() != null ? hist.getProgram()
					: (mahasiswa != null ? mahasiswa.getProgram() : "")).setParent(rowGrid);
			final org.zkoss.zul.Button btnSimpan = new org.zkoss.zul.Button("Simpan");
			btnSimpan.setDisabled(true);
			btnSimpan.setStyle("font-size:11px");
			final org.zkoss.zul.Checkbox chkPaksa = new org.zkoss.zul.Checkbox("Paksa");
			chkPaksa.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event ev) throws Exception {
					boolean c = chkPaksa.isChecked();
					lblStatusAwal.setVisible(!c);
					cbSA.setVisible(c);
					btnSimpan.setDisabled(!c);
				}
			});
			btnSimpan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event ev) throws Exception {
					org.zkoss.zul.Comboitem sel = cbSA.getSelectedItem();
					if (sel == null) {
						MyMessageboxConfig.show("Pilih Status Awal (UKT) terlebih dahulu.", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}
					Long newSaId = (Long) sel.getValue();
					Session sp = null;
					org.hibernate.Transaction tx = null;
					try {
						sp = HibernateUtil.openSession();
						sp.setFlushMode(org.hibernate.FlushMode.MANUAL);
						HistoryStatusMahasiswa hp = histId == null ? null
								: (HistoryStatusMahasiswa) sp.get(HistoryStatusMahasiswa.class, histId);
						if (hp == null && krsId != null) {
							KrsMahasiswa krsReload = (KrsMahasiswa) sp.get(KrsMahasiswa.class, krsId);
							if (krsReload != null) {
								hp = HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(krsReload, true);
							}
						}
						if (hp == null) {
							return;
						}
						StatusAwalMahasiswa ns = (StatusAwalMahasiswa) sp.get(StatusAwalMahasiswa.class, newSaId);
						hp.setStatusAwalMahasiswa(ns);
						tx = sp.beginTransaction();
						sp.saveOrUpdate(hp);
						tx.commit();
						lblStatusAwal.setValue(sel.getLabel());
						lblStatusAwal.setStyle("");
						lblStatusAwal.setTooltiptext("");
						lblStatusAwal.setVisible(true);
						cbSA.setVisible(false);
						chkPaksa.setChecked(false);
						btnSimpan.setDisabled(true);
					} catch (Exception ex) {
						if (tx != null && tx.isActive()) {
							tx.rollback();
						}
						Common.tampilErrorJikaAdmin(ex);
					} finally {
						if (sp != null && sp.isOpen()) {
							sp.close();
						}
					}
				}
			});
			org.zkoss.zul.Hbox hPaksa = new org.zkoss.zul.Hbox();
			hPaksa.setAlign("center");
			hPaksa.setSpacing("4px");
			chkPaksa.setParent(hPaksa);
			btnSimpan.setParent(hPaksa);
			hPaksa.setParent(rowGrid);
		} else {
			lblStatusAwal.setParent(rowGrid);
			new Label(hist != null && hist.getProgram() != null ? hist.getProgram()
					: (mahasiswa != null ? mahasiswa.getProgram() : "")).setParent(rowGrid);
		}
	}

	public static void uploadDataMahasiswa(final File file, final EventListener eventListener, final String[] contents)
			throws Exception {

		final Label peringatan = new Label("");

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Data Mahasiswa");
		final Label downloadPath = new Label("");
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
					if (!downloadPath.getValue().isEmpty()) {
						try { Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); } catch (Exception eD) { ais.common.ErrorAuditUtil.record(eD, "auto-audit(empty-catch) MahasiswaAction laporan-download"); }
					}
					MyMessageboxConfig.showFormatCb(
							"Proses unggah (upload) data mahasiswa telah berhasil dilakukan. Terima kasih, Bapak/Ibu, seluruh data mahasiswa telah tersimpan dengan baik.{V1}",
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener,
							"\n" + report.getRingkasan() + (peringatan.getValue().isEmpty() ? "" : "\n\nCatatan: " + peringatan.getValue()));
					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();

		new Thread(new Runnable() {

			@Override
			public void run() {

				Session session = null;
				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					ClassMetadata classMetadata = HibernateUtil.getClassMetadata(Mahasiswa.class);
					session = HibernateUtil.getSessionFactory().openSession();
					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						@SuppressWarnings("rawtypes")
						Map datum = null;
						try {
							if (session == null || !session.isOpen()) {
								session = HibernateUtil.getSessionFactory().openSession();
							}

							Long id = Common.getSheetContentAsLong(sheet, 0, i);
							Mahasiswa mahasiswa = id == null || id.equals(-1L) ? null
									: (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.idEq(id))
											.uniqueResult();
							String nim = Common.getSheetContentAsString(sheet, 1, i);

							if (mahasiswa == null) {
								mahasiswa = nim == null || nim.equals("-----") || nim.trim().isEmpty() ? null
										: (Mahasiswa) session.createCriteria(Mahasiswa.class)
												.add(Restrictions.eq("nim", nim)).addOrder(Order.desc("id"))
												.setMaxResults(1).uniqueResult();

								if (mahasiswa != null) {
									continue;
								}
							}

							if (mahasiswa == null) {
								mahasiswa = new Mahasiswa();
							}

							datum = Common.setObjectValues(classMetadata, mahasiswa, contents, 1, sheet, i);

							if (mahasiswa.getJurusan() == null) {
								System.out.println("jurusan nggak ketemu --> datum=>" + datum);
							} else {

								if (session == null || !session.isOpen()) {
									session = HibernateUtil.getSessionFactory().openSession();
								}
								session.getTransaction().begin();
								session.saveOrUpdate(mahasiswa);
								session.getTransaction().commit();

								BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();

								String ktp = Common.getSheetContentAsString(sheet, contents.length + 2, i);
								String namaAyah = Common.getSheetContentAsString(sheet, contents.length + 3, i);
								Date tanggalLahirAyah = Common.getSheetContentAsDate(sheet, contents.length + 4, i);
								Pekerjaan jenisPekerjaanAyah = (Pekerjaan) Common.getSheetContentAsObject(sheet,
										contents.length + 5, i, Pekerjaan.class);
								Penghasilan jenisPenghasilanAyah = (Penghasilan) Common.getSheetContentAsObject(sheet,
										contents.length + 6, i, Penghasilan.class);
								Jenjang jenjangPendidikanAyah = (Jenjang) Common.getSheetContentAsObject(sheet,
										contents.length + 7, i, Jenjang.class);

								String namaIbu = Common.getSheetContentAsString(sheet, contents.length + 8, i);

								Date tanggalLahirIbu = Common.getSheetContentAsDate(sheet, contents.length + 9, i);
								Pekerjaan jenisPekerjaanIbu = (Pekerjaan) Common.getSheetContentAsObject(sheet,
										contents.length + 10, i, Pekerjaan.class);
								Penghasilan jenisPenghasilanIbu = (Penghasilan) Common.getSheetContentAsObject(sheet,
										contents.length + 11, i, Penghasilan.class);
								Jenjang jenjangPendidikanIbu = (Jenjang) Common.getSheetContentAsObject(sheet,
										contents.length + 12, i, Jenjang.class);

								String teleponRumah = Common.getSheetContentAsString(sheet, contents.length + 13, i);
								String hp = Common.getSheetContentAsString(sheet, contents.length + 14, i);

								String ktpAyah = Common.getSheetContentAsString(sheet, contents.length + 16, i);
								String ktpIbu = Common.getSheetContentAsString(sheet, contents.length + 17, i);

								String npwp = Common.getSheetContentAsString(sheet, contents.length + 23, i);

								String nirm = Common.getSheetContentAsString(sheet, contents.length + 42, i);
								String nisn = Common.getSheetContentAsString(sheet, contents.length + 43, i);

								biodataMahasiswa.setNoIdentitas(ktp);
								biodataMahasiswa.setNamaAyah(namaAyah);
								biodataMahasiswa.setJenisPekerjaanAyah(jenisPekerjaanAyah);
								biodataMahasiswa.setTanggalLahirAyah(tanggalLahirAyah);
								biodataMahasiswa.setJenisPenghasilanAyah(jenisPenghasilanAyah);
								biodataMahasiswa.setJenjangPendidikanAyah(jenjangPendidikanAyah);

								biodataMahasiswa.setNamaIbu(namaIbu);
								biodataMahasiswa.setJenisPekerjaanIbu(jenisPekerjaanIbu);
								biodataMahasiswa.setTanggalLahirIbu(tanggalLahirIbu);
								biodataMahasiswa.setJenisPekerjaanIbu(jenisPekerjaanIbu);
								biodataMahasiswa.setJenjangPendidikanIbu(jenjangPendidikanIbu);
								biodataMahasiswa.setJenisPenghasilanIbu(jenisPenghasilanIbu);

								biodataMahasiswa.setTeleponRumah(teleponRumah);
								biodataMahasiswa.setHp(hp);
								biodataMahasiswa.setNirm(nirm);
								biodataMahasiswa.setNikAyah(ktpAyah);
								biodataMahasiswa.setNikIbu(ktpIbu);
								biodataMahasiswa.setNisn(nisn);
								biodataMahasiswa.setNpwp(npwp);

								if (session == null || !session.isOpen()) {
									session = HibernateUtil.getSessionFactory().openSession();
								}
								session.getTransaction().begin();
								session.saveOrUpdate(biodataMahasiswa);
								session.getTransaction().commit();

								label.setValue("Upload data \"" + mahasiswa.getNim() + " - " + mahasiswa.getNama()
										+ "\" (" + Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

								StatusMahasiswa statusMahasiswa = (StatusMahasiswa) Common
										.getSheetContentAsObject(sheet, contents.length + 15, i, StatusMahasiswa.class);

								System.out.println("statusMahasiswa = " + statusMahasiswa + ", ktp = " + ktp
										+ " namaAyah = " + namaAyah + ", namaIbu = " + namaIbu + ", teleponRumah = "
										+ teleponRumah + ", hp " + hp + ", ktpAyah = " + ktpAyah + ", ktpIbu = "
										+ ktpIbu + ", nisn " + nisn + " npwp " + npwp);

								if (statusMahasiswa != null) {
									if (session == null || !session.isOpen()) {
										session = HibernateUtil.getSessionFactory().openSession();
									}
									HistoryStatusMahasiswa historyStatusMahasiswa = (HistoryStatusMahasiswa) session
											.createCriteria(HistoryStatusMahasiswa.class)
											.add(Restrictions.eq("mahasiswa", mahasiswa))
											.add(Restrictions.eq("semester", mahasiswa.currentSemester()))
											.setMaxResults(1).uniqueResult();

									KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa,
											mahasiswa.currentSemester(), mahasiswa.currentTahapan(), null);

									if (historyStatusMahasiswa == null) {
										historyStatusMahasiswa = new HistoryStatusMahasiswa(
												Common.getCurrentTahunAkademik(), krsMahasiswa.getSksBukanKonversi(),
												krsMahasiswa == null ? null : krsMahasiswa.getSemesterPendek());
										historyStatusMahasiswa.setMahasiswa(mahasiswa);
										historyStatusMahasiswa.setSemester(mahasiswa.currentSemester());
									}
									historyStatusMahasiswa.setSks(krsMahasiswa.getSksBukanKonversi());
									historyStatusMahasiswa.setStatusMahasiswa(statusMahasiswa);
									if (session == null || !session.isOpen()) {
										session = HibernateUtil.getSessionFactory().openSession();
									}
									session.getTransaction().begin();
									session.saveOrUpdate(historyStatusMahasiswa);
									session.getTransaction().commit();

									historyStatusMahasiswa.write("tulis ulang dari " + this.getClass().getName());
								}
								report.sukses(i, mahasiswa.getNim() + " - " + mahasiswa.getNama(), "Data mahasiswa disimpan");
							}

						} catch (Exception e) {
							System.out.println("error --> datum=>" + datum);
							Common.tampilErrorJikaAdmin(e);
							report.gagal(i, "baris-" + i, e, "Periksa data mahasiswa pada baris ini.");
							try {
								HibernateUtil.rollbackTransaction();
							} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/MahasiswaAction.java:7240");

							}
							try {
								if (session != null && session.isOpen() && session.getTransaction() != null
										&& session.getTransaction().isActive()) {
									session.getTransaction().rollback();
								}
							} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/MahasiswaAction.java:7248");

							}
						}
					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/MahasiswaAction.java:7255");
				} finally {
					if (session != null && session.isOpen()) {
						try {
							session.clear();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/MahasiswaAction.java:7260");
						}
						try {
							session.disconnect();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/MahasiswaAction.java:7264");
						}
						try {
							session.close();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/MahasiswaAction.java:7268");
						}
					}
				}

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
				} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) MahasiswaAction laporan"); }
				label.setValue("");
			}
		}).start();
	}
	
	
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		return initCriteria( session ,  order);
	}

	/**
	 * Bersihkan isian Decimalbox pencarian yang tidak valid (mis. teks non-angka) agar
	 * {@code getValue()} yang dipanggil berulang kali di {@link #initCriteria(Session, boolean)}
	 * tidak melempar {@link org.zkoss.zk.ui.WrongValueException}. Diperlakukan sama seperti
	 * kosong (tidak difilter), bukan menggagalkan seluruh pencarian.
	 */
	private void bersihkanDecimalboxTidakValid(Decimalbox box) {
		if (box == null) {
			return;
		}
		try {
			box.getValue();
		} catch (org.zkoss.zk.ui.WrongValueException e) {
			try { box.setRawValue(null); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/MahasiswaAction.java:bersihkanDecimalboxTidakValid-rawvalue"); }
			try { box.setValue((java.math.BigDecimal) null); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/MahasiswaAction.java:bersihkanDecimalboxTidakValid-setvalue"); }
			try { box.clearErrorMessage(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/MahasiswaAction.java:bersihkanDecimalboxTidakValid-clearerror"); }
		}
	}

	public Criteria initCriteria(Session session , boolean order) {

		// FIX WrongValueException "You must specify a number": Decimalbox pencarian ini dipakai
		// langsung (searchtahun.getValue(), dst.) di dalam rangkaian Criteria.add(...) di bawah
		// tanpa penjagaan -- kalau user mengetik teks non-angka (mis. hasil paste keliru), getValue()
		// meledak dan menggagalkan seluruh pencarian. Bersihkan dulu isian yang tidak valid supaya
		// diperlakukan sbg "tidak difilter" (sama seperti kosong), bukan error.
		bersihkanDecimalboxTidakValid(searchtahun);
		bersihkanDecimalboxTidakValid(searchtahunLulus);
		bersihkanDecimalboxTidakValid(searchsemesterLulus);
		bersihkanDecimalboxTidakValid(searchMasa);

		Dosen dosen = (Dosen) searchdosen.getAttribute("myValue");
		Kelas kelas = (Kelas) searchkelas.getAttribute("kelas");

		Criteria criteria = session.createCriteria(Mahasiswa.class)

				.add(kelaminData != null && !kelaminData.trim().isEmpty() ? Restrictions.eq("kelamin", kelaminData)
						: Restrictions.sqlRestriction("true"))

				.add(searchkewarganegaraan == null || searchkewarganegaraan.getSelectedItem() == null
						|| searchkewarganegaraan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("warganegara", searchkewarganegaraan.getSelectedItem().getValue()))

				.add(searchaktif == null ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: searchaktif != null && searchaktif.isChecked()
								? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
								: Restrictions.sqlRestriction("true"))

				.add(dosen != null ? Restrictions.eq("dosen", dosen.getId()) : Restrictions.sqlRestriction("1=1"))
				.add(searchdosenPA.isChecked() ? Restrictions.isNull("dosen") : Restrictions.sqlRestriction("1=1"))
				.add(searchAdadosenPA.isChecked() ? Restrictions.isNotNull("dosen")
						: Restrictions.sqlRestriction("1=1"))

				.add(kelas != null && !kelas.getNama().trim().isEmpty()
						? Restrictions.ilike("kelas", kelas.getNama().trim(), MatchMode.EXACT)
						: Restrictions.sqlRestriction("true"))

				.add(searchTidakKelas.isChecked()
						? Restrictions.or(Restrictions.isNull("kelas"), Restrictions.eq("kelas", ""))
						: Restrictions.sqlRestriction("1=1"))

				.add(searchAdaKelas.isChecked()
						? Restrictions.and(Restrictions.isNotNull("kelas"), Restrictions.ne("kelas", ""))
						: Restrictions.sqlRestriction("1=1"))

				.add(dataCalonNggakValid.isChecked()
						? Restrictions.sqlRestriction(
								" this_.id not in (select mahasiswa from biodata_calon_mahasiswa where tahun="
										+ searchtahun.getValue() + " and mahasiswa is not null group by mahasiswa)")
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));

		criteria.add(criteriaStatus)

				.add(searchFacebook.isChecked()
						? Restrictions.and(Restrictions.isNotNull("facebookId"), Restrictions.ne("facebookId", ""))
						: Restrictions.sqlRestriction("true"))

				.add(searchBelumMasukFeeder != null && searchBelumMasukFeeder.isChecked()
						? Restrictions.or(Restrictions.eq("idRegPd", ""),
								Restrictions.or(Restrictions.isNull("idRegPd"),
										Restrictions.or(Restrictions.isNull("feeder"), Restrictions.eq("feeder", ""))))

						: Restrictions.sqlRestriction("true"))

				.add(searchMasukFeeder != null && searchMasukFeeder.isChecked()
						? Restrictions.not(Restrictions.or(Restrictions.eq("idRegPd", ""),
								Restrictions.or(Restrictions.isNull("idRegPd"),
										Restrictions.or(Restrictions.isNull("feeder"), Restrictions.eq("feeder", "")))))

						: Restrictions.sqlRestriction("true"))

				.add(searchTidakjenisKelamin.isChecked()
						? Restrictions.not(Restrictions.in("kelamin", new String[] { "Laki-laki", "Perempuan" }))

						: Restrictions.sqlRestriction("true"))

				.add(searchGoogle.isChecked()
						? Restrictions.and(Restrictions.isNotNull("googleId"), Restrictions.ne("googleId", ""))
						: Restrictions.sqlRestriction("true"))

				.add(searchTwitter.isChecked()
						? Restrictions.and(Restrictions.isNotNull("twitterId"), Restrictions.ne("twitterId", ""))
						: Restrictions.sqlRestriction("true"))

				.add(searchLinkedin.isChecked()
						? Restrictions.and(Restrictions.isNotNull("linkedinId"), Restrictions.ne("linkedinId", ""))
						: Restrictions.sqlRestriction("true"))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchnim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nim", searchnim.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.createAlias("kelompokMahasiswa", "kelompokMahasiswa", Criteria.LEFT_JOIN)

				// searchstatus.getSelectedItem().getValue()))
				.add(searchStatusAwalMahasiswa.getSelectedItem() == null
						|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
						|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.or(
										Restrictions.eq("kelompokMahasiswa.statusAwalMahasiswa3",
												searchStatusAwalMahasiswa.getSelectedItem().getValue()),
										Restrictions.or(
												Restrictions.eq("kelompokMahasiswa.statusAwalMahasiswa2",
														searchStatusAwalMahasiswa.getSelectedItem().getValue()),

												Restrictions.or(
														Restrictions.eq("kelompokMahasiswa.statusAwalMahasiswa",
																searchStatusAwalMahasiswa.getSelectedItem().getValue()),
														Restrictions.eq("statusAwalMahasiswa", searchStatusAwalMahasiswa
																.getSelectedItem().getValue()))))

				)

				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunangkatan", searchtahun.getValue().intValue()))

				.add(searchtahunLulus.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunLulus", searchtahunLulus.getValue().intValue()))

				.add(searchsemesterLulus.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("semesterLulus", searchsemesterLulus.getValue().intValue()))

				.add(searchsemesterawal.getSelectedItem() == null
						|| searchsemesterawal.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("semesterMulai", searchsemesterawal.getSelectedItem().getValue()))

				.add(searchJenisSeleksi.getSelectedItem() == null
						|| searchJenisSeleksi.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jenisSeleksi", searchJenisSeleksi.getSelectedItem().getValue()))

				.add(searchstatusKeluar.getSelectedItem() == null
						|| searchstatusKeluar.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("statusKeluar", searchstatusKeluar.getSelectedItem().getValue()))

				.add(searchpredikatKelulusan.getSelectedItem() == null
						|| searchpredikatKelulusan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("predikatKelulusan",
										searchpredikatKelulusan.getSelectedItem().getValue()))

				.add(searchstatusSetelahLulus.getSelectedItem() == null
						|| searchstatusSetelahLulus.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("statusSetelahLulus",
										searchstatusSetelahLulus.getSelectedItem().getValue()))

				.add(searchstatusPekerjaanSetelahLulus.getSelectedItem() == null
						|| searchstatusPekerjaanSetelahLulus.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("statusPekerjaanSetelahLulus",
										searchstatusPekerjaanSetelahLulus.getSelectedItem().getValue()))

				.add(searchstatusDomisiliSetelahLulus.getSelectedItem() == null
						|| searchstatusDomisiliSetelahLulus.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("statusDomisiliSetelahLulus",
										searchstatusDomisiliSetelahLulus.getSelectedItem().getValue()))

				.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

				.add(searchjenjang.getSelectedItem() == null || searchjenjang.getSelectedItem().getValue() == null
						|| searchjenjang.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jenjang", searchjenjang.getSelectedItem().getValue()))

				.add(searchMasa.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.sqlRestriction(
								"this_.tahunlulus is not null and (this_.tahunlulus-this_.tahunangkatan)="
										+ searchMasa.getValue().intValue()));

		if (perguruanTinggi != null) {
			criteria.createAlias("jurusan.fakultas", "fakultas", Criteria.LEFT_JOIN)
					.add(Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi));
		}

		return criteria;
	}


	
	public void onBukaPencarianLanjut(Event event) {
		FilterLanjutHelper.buka(filterLanjutOverlay);
	}

	public void onTutupPencarianLanjut(Event event) {
		FilterLanjutHelper.tutup(filterLanjutOverlay);
	}

	public void onTerapkanPencarianLanjut(Event event) {
		FilterLanjutHelper.tutup(filterLanjutOverlay);
		onSearchDefault(event);
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(final Event event) {
		if (searchnama == null) {
			return;
		}

		if (dataCalonNggakValid != null && dataCalonNggakValid.isChecked() && searchtahun != null
				&& searchtahun.getValue() == null) {
			try {
				MyMessageboxConfig.show(
						"Mohon maaf, apabila pilihan menampilkan mahasiswa yang tidak valid dengan calon mahasiswa diaktifkan, maka Tahun angkatan wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Tahun angkatan pada kolom pencarian yang tersedia; (2) pastikan tahun angkatan sesuai dengan data yang ingin ditampilkan; (3) setelah tahun angkatan terisi, silakan melakukan pencarian kembali.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
			return;
		}

		criteriaStatus = Restrictions.sqlRestriction("true");
		selectedStatusMahasiswa = (StatusMahasiswa) (searchstatus != null && searchstatus.getSelectedItem() != null
				&& searchstatus.getSelectedItem().getValue() != null ? searchstatus.getSelectedItem().getValue() : null);
		int activePageVal = paging == null ? 0 : paging.getActivePage();

		try {
			if (selectedStatusMahasiswa != null) {
				final String ta = searchTahunAjaran != null && searchTahunAjaran.getSelectedItem() != null
						&& searchTahunAjaran.getSelectedItem().getValue() != null
								? searchTahunAjaran.getSelectedItem().getValue().toString() : null;
				final String jenisSemester = searchJenisSemester != null
						&& searchJenisSemester.getSelectedItem() != null
						&& searchJenisSemester.getSelectedItem().getValue() != null
								? searchJenisSemester.getSelectedItem().getValue().toString() : null;

				// Kandidat status HANYA dari mahasiswa yang cocok dengan SELURUH filter LAIN (fakultas,
				// prodi, angkatan, program, dll.) — BUKAN semua mahasiswa aktif — agar tidak memuat &
				// menghitung status puluhan ribu baris (lambat/timeout). Saat ini criteriaStatus masih
				// "true", sehingga initCriteria hanya menerapkan filter non-status. distinct: alias
				// LEFT JOIN (kelompokMahasiswa/jurusan) berpotensi menggandakan id.
				List<Long> dataMhs = initCriteria(false)
						.setProjection(Projections.distinct(Projections.property("id"))).list();

				List<Long> mhss = new ArrayList<Long>();
				for (Long generalValueObjectid : dataMhs) {
					Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(),
							generalValueObjectid);
					if (mahasiswa != null) {
						Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), ta, jenisSemester,
								mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
						KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);
						HistoryStatusMahasiswa historyStatusMahasiswa = Common.getHistoryStatusMahasiswa(krsMahasiswa);
						if (historyStatusMahasiswa != null && historyStatusMahasiswa.getStatusMahasiswa() != null
								&& historyStatusMahasiswa.getStatusMahasiswa().getId()
										.equals(selectedStatusMahasiswa.getId())) {
							mhss.add(mahasiswa.getId());
						}
					}
				}
				if (mhss.isEmpty()) {
					criteriaStatus = Restrictions.sqlRestriction("false");
				} else {
					// JANGAN drop filter saat >1000 (dulu: criteriaStatus="true" → SEMUA data tampil =
					// filter Status seolah tak berfungsi). Terapkan IN via SQL literal (id bertipe Long,
					// aman dari injeksi) agar tidak terkena batas jumlah parameter bind Hibernate/JDBC
					// walau daftar id sangat banyak.
					StringBuilder inIds = new StringBuilder();
					for (Long id : mhss) {
						if (id == null) {
							continue;
						}
						if (inIds.length() > 0) {
							inIds.append(',');
						}
						inIds.append(id.longValue());
					}
					criteriaStatus = inIds.length() == 0 ? Restrictions.sqlRestriction("false")
							: Restrictions.sqlRestriction("this_.id in (" + inIds + ")");
				}
			}

			Common.initPaging(initCriteria(false), paging);
			List<Mahasiswa> listHasil = ConstantValues.simpleList(
					initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
							.setFirstResult(Common.ROWS_COUNT_ON_PAGE * activePageVal),
					Mahasiswa.class);

			if (grid != null) {
				ListModel strset = new SimpleListModel(listHasil);
				grid.setRowRenderer(new MahasiswaRenderer());
				grid.setModelCheckMobile(strset);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@Override
	public void loadData(Object value) {
		try {
			onSearchDefault(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

}
