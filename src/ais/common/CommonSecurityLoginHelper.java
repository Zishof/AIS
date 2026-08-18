package ais.common;

import ais.database.model.sekolah.Siswa;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.Writer;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Blob;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.hibernate.type.StringType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.zkoss.calendar.impl.SimpleCalendarEvent;
import org.zkoss.poi.ss.usermodel.DataFormatter;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
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
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.A;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;
import org.zkoss.zul.Window;

import ais.action.master.BiodataDosenAction;
import ais.action.master.KonfigurasiTampilanBiodataDosenAction;
import ais.action.master.KonfigurasiTampilanGuruAction;
import ais.action.master.KonfigurasiTampilanPegawaiAction;
import ais.action.master.MahasiswaRequestTugasAkhirAction;
import ais.action.master.RencanaTahunAkademikAction;
import ais.action.master.SkripsiAction;
import ais.action.master.SyaratUjianAction;
import ais.action.master.TampilanELearningAction;
import ais.action.master.employ.util.FormBiodataPegawaiUtil;
import ais.action.master.helper.AbsensiHelper;
import ais.action.master.helper.AmbilDataCalonMahasiswaBanbox;
import ais.action.master.helper.AmbilDataKecamatanBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.BukuBahanAjarHelper;
import ais.action.master.helper.CetakAlbumWisudaAdminWindow;
import ais.action.master.helper.CetakAlbumWisudaMahasiswaHelper;
import ais.action.master.helper.ChangePasswordWindow;
import ais.action.master.helper.GenerateNoKursiDanNoRegistrasiWindow;
import ais.action.master.helper.GenerateNoKursiWindow;
import ais.action.master.helper.GenerateUndanganWisudaWindow;
import ais.action.master.helper.HistoryStatusMahasiswaUtil;
import ais.action.master.helper.KrsDanSkripsiHelper;
import ais.action.master.helper.KrsDetailHelper;
import ais.action.master.helper.MainHelper;
import ais.action.master.helper.PerkuliahanPunyaItemHelper;
import ais.action.master.helper.RekapitulasiAudioHelper;
import ais.action.master.helper.RekapitulasiMateriHelper;
import ais.action.master.helper.RekapitulasiTugasHelper;
import ais.action.master.helper.RekapitulasiUjianHelper;
import ais.action.master.helper.RekapitulasiVideoHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.TugasKelompokHelper;
import ais.action.master.helper.generic.AmbilDataTugasFileContent;
import ais.action.master.helper.generic.AngketGuruWindow;
import ais.action.master.helper.profile.ProfileUtil;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.kursus.helper.KursusUtil;
import ais.action.master.library.util.LibraryUtil;
import ais.action.master.pmb.statistik.LaporanDaftarUlangMahasiswaBaru;
import ais.action.master.pmb.statistik.LaporanLulusMahasiswaBaru;
import ais.action.master.pmb.statistik.LaporanPendaftarMahasiswaBaru;
import ais.action.master.pmb.statistik.LaporanRekapJenisSeleksiMahasiswaBaru;
import ais.action.master.sekolah.GuruAction;
import ais.action.master.sekolah.helper.AbsensiSiswaHelper;
import ais.action.master.sekolah.helper.AmbilDataCalonSiswaBanbox;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.master.sekolah.helper.BukuBahanAjarMatapelajaranHelper;
import ais.action.master.sekolah.helper.JadwalPelajaranPunyaItemHelper;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.master.sop.helper.SopUtil;
import ais.action.report.format1.akademik.LaporanAlbumMahasiswaPerProdiDanAngkatan;
import ais.action.report.format1.akademik.LaporanAlbumMahasiswaWisuda;
import ais.action.report.format1.akademik.LaporanAlbumProfileWisuda;
import ais.action.report.format1.akademik.LaporanAngketDosenPerDosenWindow;
import ais.action.report.format1.akademik.LaporanMonitorJadwalPelajaran;
import ais.action.report.format1.akademik.LaporanRekamanNilai;
import ais.action.report.format1.employ.LaporanDaftarPegawai;
import ais.action.report.format1.employ.LaporanDaftarUrutKepangkatan;
import ais.action.report.format1.employ.RekapJumlahPegawaiBaseGolongan;
import ais.action.report.format1.employ.RekapJumlahPegawaiBaseUnitKerja;
import ais.action.report.format1.employ.StatistikJumlahPegawaiBaseJabatanFungsional;
import ais.action.report.format1.employ.StatistikJumlahPegawaiBaseStatus;
import ais.action.report.format1.employ.StatistikJumlahPegawaiBaseTahun;
import ais.action.report.format1.employ.StatistikJumlahPegawaiPerJenisKelamin;
import ais.action.report.format1.employ.StatistikJumlahPegawaiPerPendidikan;
import ais.action.report.format1.payroll.LaporanTransaksiPegawai;
import ais.action.report.format1.sekolah.LaporanAlbumSiswa;
import ais.action.report.helper.absen.LaporanDaftarHadirWindow;
import ais.action.report.helper.akademik.LaporanDaftarHadirDosen;
import ais.action.report.helper.akademik.LaporanRekapPenilaianMahasiswaWindow;
import ais.action.report.helper.keuangan.LaporanRekapHostToHostWindow;
import ais.action.report.helper.keuangan.LaporanRekapMahasiswaBelumBayarWindow;
import ais.action.report.helper.keuangan.LaporanRekapMahasiswaSudahBayarWindow;
import ais.action.report.helper.keuangan.LaporanRekapPerJenisBiayaWindow;
import ais.action.report.helper.keuangan.LaporanRekapPerPembayaranWindow;
import ais.action.report.helper.keuangan.LaporanRekapPerPembayarandgnPenguranganWindow;
import ais.action.report.helper.keuangan.LaporanRekapPerProdiDenganPenguranganPerValidatorWindow;
import ais.action.report.helper.keuangan.LaporanRekapPerProdiDenganPenguranganWindow;
import ais.action.report.helper.keuangan.LaporanRekapPerProdiWindow;
import ais.action.report.helper.mahasiswa.LaporanDataMahasiswaWindow;
import ais.action.report.helper.nilai.LaporanDaftarNilaiWindow;
import ais.action.report.helper.nilai.LaporanDaftarPrestasiBelajarWindow;
import ais.action.report.helper.nilai.LaporanKartuHasilStudiMahasiswaWindow;
import ais.action.report.helper.pdf.GenerateValidasiLaporanWindow;
import ais.action.report.helper.pdf.LaporanAbsensiUjianWindow;
import ais.action.report.helper.pdf.LaporanAbsensiWindow;
import ais.action.report.helper.pdf.LaporanBeritaAcaraSkripsiWindow;
import ais.action.report.helper.pdf.LaporanCoverAbsensiWindow;
import ais.action.report.helper.pdf.LaporanDaftarHadirDosenHarianWindow;
import ais.action.report.helper.pdf.LaporanDaftarHadirDosenWindow;
import ais.action.report.helper.pdf.LaporanDaftarHadirUjianSidangWindow;
import ais.action.report.helper.pdf.LaporanDataPegawaiNamaAlamatWindow;
import ais.action.report.helper.pdf.LaporanJadwalPerkuliahanWindow;
import ais.action.report.helper.pdf.LaporanJadwalUasWindow;
import ais.action.report.helper.pdf.LaporanKHSSemesterPendekWindow;
import ais.action.report.helper.pdf.LaporanKHSWindow;
import ais.action.report.helper.pdf.LaporanKurikulumWindow;
import ais.action.report.helper.pdf.LaporanNilaiUjianSidangSkripsiWindow;
import ais.action.report.helper.pdf.LaporanRekapJumlahMhsFakWindow;
import ais.action.report.helper.pdf.LaporanRekapitulasiAlumniJurusanWindow;
import ais.action.report.helper.pdf.LaporanRekapitulasiDosenPerPendidikanWindow;
import ais.action.report.helper.pdf.LaporanRekapitulasiDosenWindow;
import ais.action.report.helper.pdf.LaporanRekapitulasiItemBiayaWindow;
import ais.action.report.helper.pdf.LaporanRekapitulasiMahasiswaWindow;
import ais.action.report.helper.pdf.LaporanRekapitulasiPAWindow;
import ais.action.report.helper.pdf.LaporanRekapitulasiPMDKWindow;
import ais.action.report.helper.pdf.LaporanRekapitulasiValidasiKeuanganWindow;
import ais.action.report.helper.pdf.LaporanSKSDosenWindow;
import ais.action.report.helper.pdf.LaporanTranskipAkademikKonversiWindow;
import ais.action.report.helper.pdf.LaporanTranskipAkademikWindow;
import ais.action.report.helper.statistik.LaporanDaftarStatusAwalMahasiswa;
import ais.action.report.helper.statistik.LaporanProporsiJumlahmahasiswapendaftar;
import ais.action.ws.util.PembayaranUtil;
import ais.database.dao.DaoFactory;
import ais.database.dao.PegawaiDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.AccessedUsers;
import ais.database.model.BaypassPembayaranMahasiswa;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.BuktiPembayaran;
import ais.database.model.CicilanPembayaran;
import ais.database.model.CicilanPembayaranGagal;
import ais.database.model.CommonVO;
import ais.database.model.Dashboard;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailLogLogin;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.FormatNilai;
import ais.database.model.FormatNilaiProposalSkripsi;
import ais.database.model.FormatNilaiSkripsi;
import ais.database.model.FormulirKegiatan;
import ais.database.model.GeneralValueObject;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisNilaiHurufMatakuliah;
import ais.database.model.JenisPembayaran;
import ais.database.model.Jenjang;
import ais.database.model.Judisium;
import ais.database.model.Jurusan;
import ais.database.model.KalenderAkademik;
import ais.database.model.Kegiatan;
import ais.database.model.KegiatanTemporary;
import ais.database.model.Kkn;
import ais.database.model.Komentar;
import ais.database.model.Konfigurasi;
import ais.database.model.Kota;
import ais.database.model.KrsMahasiswa;
import ais.database.model.LabelBahasa;
import ais.database.model.LogHostToHost;
import ais.database.model.LogLogin;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Matakuliah;
import ais.database.model.MatakuliahPrasyarat;
import ais.database.model.MatakuliahPunyaBukuBahanAjar;
import ais.database.model.Menu;
import ais.database.model.NilaiHuruf;
import ais.database.model.OrangTua;
import ais.database.model.OrganisasiIntraKampus;
import ais.database.model.ParameterUmum;
import ais.database.model.Pegawai;
import ais.database.model.PembatasanNilaiIPKUntukPengambilanKRS;
import ais.database.model.PembombotanNilai;
import ais.database.model.PendaftaranCutiMahasiswa;
import ais.database.model.PendapatanOrangTua;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.PengecualianKknMahasiswa;
import ais.database.model.PengecualianPklMahasiswa;
import ais.database.model.PenugasanDosenMengajar;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.PerkuliahanPunyaItem;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaDiskusi;
import ais.database.model.Pkl;
import ais.database.model.Program;
import ais.database.model.Propinsi;
import ais.database.model.RencanaTahunAkademik;
import ais.database.model.RoleAccess;
import ais.database.model.Ruang;
import ais.database.model.Skripsi;
import ais.database.model.SocialMediaCommonModel;
import ais.database.model.Staff;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusPertemuan;
import ais.database.model.SyaratUjian;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.TemplatePerkuliahan;
import ais.database.model.TemplatePerkuliahanDetail;
import ais.database.model.TextBerjalan;
import ais.database.model.Tugas;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.UserAccess;
import ais.database.model.UserRole;
import ais.database.model.VOPembelajaran;
import ais.database.model.VoKunci;
import ais.database.model.Wilayah;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.beasiswa.MahasiswaBeasiswaPersyaratan;
import ais.database.model.beasiswa.MahasiswaDaftarBeasiswa;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranBeasiswaMahasiswa;
import ais.database.model.file.LampiranKknMahasiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.LampiranLainMahasiswa;
import ais.database.model.file.LampiranPklMahasiswa;
import ais.database.model.file.TugasFileContent;
import ais.database.model.inventory.Pedagang;
import ais.database.model.inventory.Toko;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.kkn.MahasiswaKknPersyaratan;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.Koperasi;
import ais.database.model.koperasi.PengurusKoperasi;
import ais.database.model.kursus.KomponenDataProdukKursus;
import ais.database.model.kursus.PesertaKursus;
import ais.database.model.library.Anggota;
import ais.database.model.library.HariLiburPerpustakaan;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.Pustakawan;
import ais.database.model.payroll.LiburNasional;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.pkl.MahasiswaPklPersyaratan;
import ais.database.model.rab.Pejabat;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.recruitment.CalonPegawai;
import ais.database.model.sekolah.AbsenPiket;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.DetailJenisPenilaian;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JadwalPelajaranPunyaItem;
import ais.database.model.sekolah.JenisPenilaian;
import ais.database.model.sekolah.KurikulumPunyaMatapelajaran;
import ais.database.model.sekolah.MatapelajaranPunyaBukuBahanAjar;
import ais.database.model.sekolah.PenugasanGuruMengajar;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.sirkulasisurat.PeminjamSurat;
import ais.database.model.sop.DataSop;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHboxStyled;
import ais.ui.util.MyIframe;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPanel;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyTreeitemConfig;
import ais.ui.util.MyVboxStyled;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLType;



@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class CommonSecurityLoginHelper extends Common {

	private static String getCookieValue(HttpServletRequest request, String cookieName) {
		if (request == null || cookieName == null || cookieName.trim().length() == 0) {
			return null;
		}
		try {
			Cookie[] cookies = request.getCookies();
			if (cookies == null) {
				return null;
			}
			for (int i = 0; i < cookies.length; i++) {
				Cookie cookie = cookies[i];
				if (cookie != null && cookieName.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonSecurityLoginHelper.java:450");
		}
		return null;
	}


	private static final Logger log = Logger.getLogger(CommonSecurityLoginHelper.class);
	private static final String COOKIE_PMB_BIODATA = "biodataCalonMahasiswa";
	private static final String COOKIE_PMB_USERID = "userid";

	private static String safeTrim(String value) {
		return value == null ? "" : value.trim();
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}

	private static boolean ensureDirectory(File directory) {
		if (directory == null) {
			return false;
		}
		if (directory.exists()) {
			return directory.isDirectory();
		}
		return directory.mkdirs();
	}

	private static void tampilCrudError(Exception e, String pesan) {
		Common.tampilErrorJikaAdmin(e);
		String detail = e == null || e.getMessage() == null ? "" : "\n" + e.getMessage();
		try {
			MyMessageboxConfig.show(pesan + detail);
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonSecurityLoginHelper.java:483");
		}
	}




	public static void doCheckSecurity() {
		CommonPrivilages.doCheckPrevilagesRead();
	}



	public static void doLogin(Tbmuser tbmuser, String linkProfile, String callback_url) throws Exception {
		doLogin(tbmuser, linkProfile, null, callback_url);
	}



	public static void doLogin(Mahasiswa mahasiswa, String linkProfile, String callback_url) throws Exception {
		doLogin(mahasiswa, linkProfile, null, callback_url);
	}



	public static void doLogin(Siswa siswa, String linkProfile, String callback_url) throws Exception {
		doLogin(siswa, linkProfile, null, callback_url);
	}



	public static void doLogin(Tbmuser tbmuser, String linkProfile, String parameter, String callback_url)
			throws Exception {
		try {

			Clients.confirmClose(null);

			String[] block = ConstantValues.grupPenggunaBlok.trim().split(",");
			Set<String> blockJenisPengguna = new HashSet<String>();
			for (String s : block) {
				if (!s.trim().isEmpty()) {
					blockJenisPengguna.add(s.trim().toLowerCase());
				}
			}
			if (blockJenisPengguna.contains(tbmuser.hakAkses().getRoleId().toLowerCase())) {
				System.out.println("[SOCIAL-LOGIN] DITOLAK: Tbmuser id=" + tbmuser.getId() + " userId="
						+ tbmuser.getUserId() + " role=" + tbmuser.hakAkses().getRoleId()
						+ " diblokir dari login sosial (konfigurasi grup_pengguna_blok).");
				MyMessageboxConfig.show(
						"Jenis pengguna \"" + tbmuser.hakAkses().getRoleName()
								+ "\" tidak diizinkan login menggunakan media sosial",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.goLogoff();
							}
						});
				return;
			}
		} catch (Exception e) {
			System.out.println("[SOCIAL-LOGIN] GAGAL: exception saat cek blokir role utk Tbmuser userId="
					+ tbmuser.getUserId() + " -- " + e);
			Common.tampilErrorJikaAdmin(e);
		}
		System.out.println("[SOCIAL-LOGIN] lolos cek blokir role, panggil SecurityFilter.doAutoLogin utk userId="
				+ tbmuser.getUserId());
		boolean hasilAutoLogin = SecurityFilter.doAutoLogin(tbmuser.getUserId(),
				Common.desEncrypter.get().decrypt(tbmuser.getUserPassword()), false, linkProfile);
		String targetRedirect = callback_url == null ? "main" + (parameter == null ? "" : parameter) : callback_url;
		System.out.println("[SOCIAL-LOGIN] hasil doAutoLogin utk Tbmuser userId=" + tbmuser.getUserId() + " => "
				+ hasilAutoLogin + " -- redirect ke \"" + targetRedirect + "\""
				+ (hasilAutoLogin ? "" : " (PERINGATAN: doAutoLogin GAGAL tapi tetap redirect seolah sukses)"));
		ExecutionsCtrl.sendRedirect(targetRedirect);

	}



	public static void doLogin(Mahasiswa mahasiswa, String linkProfile, String parameter, String callback_url)
			throws Exception {
		try {

			Clients.confirmClose(null);

			String[] block = ConstantValues.grupPenggunaBlok.trim().split(",");
			Set<String> blockJenisPengguna = new HashSet<String>();
			for (String s : block) {
				if (!s.trim().isEmpty()) {
					blockJenisPengguna.add(s.trim().toLowerCase());
				}
			}
			if (blockJenisPengguna.contains("mhs")) {
				System.out.println("[SOCIAL-LOGIN] DITOLAK: Mahasiswa id=" + mahasiswa.getId() + " nim="
						+ mahasiswa.getNim() + " diblokir dari login sosial (konfigurasi grup_pengguna_blok berisi \"mhs\").");
				MyMessageboxConfig.show("Jenis pengguna \"Mahasiswa\" tidak diizinkan login menggunakan media sosial",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.goLogoff();
							}
						});
				return;
			}
		} catch (Exception e) {
			System.out.println("[SOCIAL-LOGIN] GAGAL: exception saat cek blokir role utk Mahasiswa nim="
					+ mahasiswa.getNim() + " -- " + e);
			Common.tampilErrorJikaAdmin(e);
		}
		System.out.println("[SOCIAL-LOGIN] lolos cek blokir role, panggil SecurityFilter.doAutoLogin utk nim="
				+ mahasiswa.getNim());
		boolean hasilAutoLogin = SecurityFilter.doAutoLogin(mahasiswa.getNim(),
				Common.desEncrypter.get().decrypt(mahasiswa.getPass()), false, linkProfile);
		String targetRedirect = callback_url == null ? "main" + (parameter == null ? "" : parameter) : callback_url;
		System.out.println("[SOCIAL-LOGIN] hasil doAutoLogin utk Mahasiswa nim=" + mahasiswa.getNim() + " => "
				+ hasilAutoLogin + " -- redirect ke \"" + targetRedirect + "\""
				+ (hasilAutoLogin ? "" : " (PERINGATAN: doAutoLogin GAGAL tapi tetap redirect seolah sukses)"));
		ExecutionsCtrl.sendRedirect(targetRedirect);
	}



	public static void doLogin(Siswa siswa, String linkProfile, String parameter, String callback_url)
			throws Exception {
		try {

			Clients.confirmClose(null);

			String[] block = ConstantValues.grupPenggunaBlok.trim().split(",");
			Set<String> blockJenisPengguna = new HashSet<String>();
			for (String s : block) {
				if (!s.trim().isEmpty()) {
					blockJenisPengguna.add(s.trim().toLowerCase());
				}
			}
			if (blockJenisPengguna.contains("mhs")) {
				System.out.println("[SOCIAL-LOGIN] DITOLAK: Siswa id=" + siswa.getId() + " nomorInduk="
						+ siswa.getNomorInduk() + " diblokir dari login sosial (konfigurasi grup_pengguna_blok berisi \"mhs\").");
				MyMessageboxConfig.show("Jenis pengguna \"Siswa\" tidak diizinkan login menggunakan media sosial",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.goLogoff();
							}
						});
				return;
			}
		} catch (Exception e) {
			System.out.println("[SOCIAL-LOGIN] GAGAL: exception saat cek blokir role utk Siswa nomorInduk="
					+ siswa.getNomorInduk() + " -- " + e);
			Common.tampilErrorJikaAdmin(e);
		}
		System.out.println("[SOCIAL-LOGIN] lolos cek blokir role, panggil SecurityFilter.doAutoLogin utk nomorIndukNasional="
				+ siswa.getNomorIndukNasional());
		boolean hasilAutoLogin = SecurityFilter.doAutoLogin(siswa.getNomorIndukNasional(),
				Common.desEncrypter.get().decrypt(siswa.getPass()), false, linkProfile);
		String targetRedirect = callback_url == null ? "main" + (parameter == null ? "" : parameter) : callback_url;
		System.out.println("[SOCIAL-LOGIN] hasil doAutoLogin utk Siswa nomorInduk=" + siswa.getNomorInduk() + " => "
				+ hasilAutoLogin + " -- redirect ke \"" + targetRedirect + "\""
				+ (hasilAutoLogin ? "" : " (PERINGATAN: doAutoLogin GAGAL tapi tetap redirect seolah sukses)"));
		ExecutionsCtrl.sendRedirect(targetRedirect);
	}



	public static boolean isPmbCookieLoginEnabled() {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi(KONFIG_PMB_LOGIN_COOKIE, Konfigurasi.TIDAK_AKTIF);
			return konfigurasi != null && konfigurasi.getNilai() != null
					&& Konfigurasi.AKTIF.equalsIgnoreCase(konfigurasi.getNilai().trim());
		} catch (Exception e) {
			return false;
		}
	}



	public static BiodataCalonMahasiswa isLogin() {
		HttpServletRequest request = null;
		if (ExecutionsCtrl.getCurrent() != null) {
			request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		}
		if (request == null) {
			request = RequestContext.get();
		}
		return isLogin(request);
	}



	public static BiodataCalonMahasiswa isLogin(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		try {
			HttpSession httpSession = request.getSession(false);
			if (httpSession != null) {
				Object biodataSession = httpSession.getAttribute("BiodataCalonMahasiswa");
				if (biodataSession instanceof BiodataCalonMahasiswa) {
					return (BiodataCalonMahasiswa) biodataSession;
				}
			}

			if (!isPmbCookieLoginEnabled()) {
				return null;
			}

			String encryptedId = getCookieValue(request, COOKIE_PMB_BIODATA);
			if (encryptedId == null || encryptedId.trim().length() == 0) {
				return null;
			}

			String idData = Common.desEncrypter.get().decrypt(encryptedId);
			if (idData == null || idData.trim().length() == 0 || "-1".equalsIgnoreCase(idData.trim())) {
				return null;
			}

			BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues
					.ambil(BiodataCalonMahasiswa.class.getName(), Long.parseLong(idData.trim()), true);
			if (biodataCalonMahasiswa != null) {
				HttpSession session = request.getSession(true);
				session.setAttribute("BiodataCalonMahasiswa", biodataCalonMahasiswa);
				Tbmuser tbmuser = new Tbmuser(biodataCalonMahasiswa);
				session.setAttribute("mytbmuser", tbmuser);
				session.setAttribute("usersTemp", tbmuser);
				session.setAttribute("user", tbmuser);
			}
			return biodataCalonMahasiswa;
		} catch (Exception e) {
			return null;
		}
	}



	public static void setLogout() {

		try {
			HttpServletRequest request = null;
			HttpServletResponse response = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
				response = (HttpServletResponse) ExecutionsCtrl.getCurrent().getNativeResponse();
			}
			if (request == null) {
				request = RequestContext.get();
			}
			if (response == null) {
				response = ResponseContext.get();
			}
			setLogout(request, response);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonSecurityLoginHelper.java:708");

		}
	}



	public static void setLogout(HttpServletRequest request, HttpServletResponse response) {
		try {
			HttpSession session = request == null ? null : request.getSession(false);
			if (session != null) {
				session.removeAttribute("BiodataCalonMahasiswa");
				session.removeAttribute("mytbmuser");
				session.removeAttribute("usersTemp");
				session.removeAttribute("user");
				session.invalidate();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonSecurityLoginHelper.java:725");

		}

		clearPmbLoginCookies(request, response);
	}



	public static void setLogin(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		try {
			HttpServletRequest request = null;
			HttpServletResponse res = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
				res = (HttpServletResponse) ExecutionsCtrl.getCurrent().getNativeResponse();
			}
			if (request == null) {
				request = RequestContext.get();
				res = ResponseContext.get();
			}
			setLogin(request, res, biodataCalonMahasiswa);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonSecurityLoginHelper.java:747");
//			tampilErrorJikaAdmin(e);
		}
	}



	public static void setLogin(HttpServletRequest request, HttpServletResponse res,
			BiodataCalonMahasiswa biodataCalonMahasiswa) {
		if (request == null || biodataCalonMahasiswa == null) {
			return;
		}
		try {

			HttpSession session = request.getSession(true);
			session.setAttribute("BiodataCalonMahasiswa", biodataCalonMahasiswa);
			Tbmuser tbmuser = new Tbmuser(biodataCalonMahasiswa);
			session.setAttribute("mytbmuser", tbmuser);
			session.setAttribute("usersTemp", tbmuser);
			session.setAttribute("user", tbmuser);

		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
		}

		if (res == null || !isPmbCookieLoginEnabled()) {
			return;
		}

		try {
			Cookie cookieUsername = new Cookie(COOKIE_PMB_BIODATA,
					Common.desEncrypter.get().encrypt(biodataCalonMahasiswa.getId().toString()));
			cookieUsername.setMaxAge(15552000);
			cookieUsername.setPath("/");
			cookieUsername.setSecure(request.isSecure());
			res.addCookie(cookieUsername);

			String noRegistrasi = biodataCalonMahasiswa.getNoRegistrasi() == null ? ""
					: biodataCalonMahasiswa.getNoRegistrasi();
			cookieUsername = new Cookie(COOKIE_PMB_USERID, noRegistrasi);
			cookieUsername.setMaxAge(15552000);
			cookieUsername.setPath("/");
			cookieUsername.setSecure(request.isSecure());
			res.addCookie(cookieUsername);

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonSecurityLoginHelper.java:792");
//			tampilErrorJikaAdmin(e);
		}
	}



	public static CalonSiswa isLoginCalonSiswa() {
		try {

			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}

			if (request == null) {
				request = RequestContext.get();
			}
			return (CalonSiswa) request.getSession(true).getAttribute("CalonSiswa");
		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
			return null;
		}
	}



	public static CalonSiswa isLoginCalonSiswa(HttpServletRequest request) {
		try {
			return (CalonSiswa) request.getSession(true).getAttribute("CalonSiswa");
		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
			return null;
		}
	}



	public static void setLogoutCalonSiswa() {
		HttpServletRequest request = null;
		if (ExecutionsCtrl.getCurrent() != null) {
			request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		}

		if (request == null) {
			request = RequestContext.get();
		}
		HttpServletResponse response = null;
		if (ExecutionsCtrl.getCurrent() != null) {
			response = (HttpServletResponse) ExecutionsCtrl.getCurrent().getNativeResponse();
		}

		if (response == null) {
			response = ResponseContext.get();
		}
		setLogoutCalonSiswa(request, response);
	}



	public static void setLogoutCalonSiswa(HttpServletRequest request, HttpServletResponse response) {
		try {

			HttpSession session = request.getSession(true);
			session.setAttribute("CalonSiswa", null);
			session.setAttribute("mytbmuser", null);
			session.setAttribute("usersTemp", null);
			session.setAttribute("user", null);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonSecurityLoginHelper.java:860");

		}

		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				Cookie cookie = cookies[i];
				cookie.setValue("");
				cookie.setPath("/");
				cookie.setMaxAge(0);
				response.addCookie(cookie);
			}
		}
	}



	public static PenyediaAsset isLoginPenyediaAsset() {
		try {

			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}

			if (request == null) {
				request = RequestContext.get();
			}
			return (PenyediaAsset) request.getSession(true).getAttribute("PenyediaAsset");
		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
			return null;
		}
	}



	public static void setLogoutPenyediaAsset() {
		try {
			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}

			if (request == null) {
				request = RequestContext.get();
			}
			HttpSession session = request.getSession(true);
			session.setAttribute("PenyediaAsset", null);
			session.setAttribute("mytbmuser", null);
			session.setAttribute("usersTemp", null);
			session.setAttribute("user", null);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonSecurityLoginHelper.java:913");

		}
	}



	public static CalonPegawai isLoginCalonPegawai() {
		try {

			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}

			if (request == null) {
				request = RequestContext.get();
			}
			return (CalonPegawai) request.getSession(true).getAttribute("CalonPegawai");
		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
			return null;
		}
	}



	public static void setLogoutCalonPegawai() {
		try {
			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}

			if (request == null) {
				request = RequestContext.get();
			}
			HttpSession session = request.getSession(true);
			session.setAttribute("CalonPegawai", null);
			session.setAttribute("mytbmuser", null);
			session.setAttribute("usersTemp", null);
			session.setAttribute("user", null);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonSecurityLoginHelper.java:955");

		}
	}



	public static void setLogin(CalonSiswa calonSiswa) {
		try {
			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}

			if (request == null) {
				request = RequestContext.get();
			}
			setLogin(request, calonSiswa);
		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
		}
	}



	public static void setLogin(HttpServletRequest request, CalonSiswa calonSiswa) {
		try {

			HttpSession session = request.getSession(true);
			session.setAttribute("CalonSiswa", calonSiswa);
			Tbmuser tbmuser = new Tbmuser(calonSiswa);
			session.setAttribute("mytbmuser", tbmuser);
			session.setAttribute("usersTemp", tbmuser);
			session.setAttribute("user", tbmuser);
		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
		}
	}



	public static void setLogin(HttpServletRequest request, HttpServletResponse res, CalonSiswa calonSiswa) {
		try {

			HttpSession session = request.getSession(true);
			session.setAttribute("CalonSiswa", calonSiswa);
			Tbmuser tbmuser = new Tbmuser(calonSiswa);
			session.setAttribute("mytbmuser", tbmuser);
			session.setAttribute("usersTemp", tbmuser);
			session.setAttribute("user", tbmuser);

		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
		}

		try {
			Cookie cookieUsername = new Cookie("calonSiswa",
					Common.desEncrypter.get().encrypt(calonSiswa.getId().toString()));
			cookieUsername.setMaxAge(15552000);

			res.addCookie(cookieUsername);

			cookieUsername = new Cookie("userid", calonSiswa.getNoRegistrasi());
			cookieUsername.setMaxAge(15552000);

			res.addCookie(cookieUsername);

		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
		}
	}



	public static String tampilErrorJikaAdmin(Exception ex) {
		// ex.printStackTrace();
		return tampilErrorJikaAdmin(ex, "", false);
	}



	public static String tampilErrorJikaAdmin(Exception ex, String info, boolean download) {

		return CommonHelperClass.tampilErrorJikaAdmin(ex, info, download);

	}



	public static EventListener downloadError(Exception ex) {
		return CommonHelperClass.downloadError(ex);
	}



    public static boolean checkLogin(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        Session session = null;
        try {
            session = HibernateUtil.currentNativeSession();
            Tbmuser users = (Tbmuser) session.createCriteria(Tbmuser.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                    .add(Restrictions.eq("userId", username)).setMaxResults(1).uniqueResult();
            Mahasiswa mahasiswa = null;
            if (users == null) {
                String mypassword = Common.desEncrypter.get().encrypt(password);
                mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class)
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                        .add(Restrictions.eq("nim", username)).add(Restrictions.eq("pass", mypassword))
                        .setMaxResults(1).uniqueResult();
            }

            if (users == null) {
                return mahasiswa != null;
            }

            String pwd = "";
            try {
                pwd = Common.desEncrypter.get().decrypt(users.getUserPassword() == null ? "" : users.getUserPassword().trim());
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
            }
            return password.equals(pwd);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return false;
        } finally {
            Common.closeNativeSessionQuietly(session);
        }
    }



    public static String kirimLupaPassword(String username) throws Exception {
        JSONArray userIds = new JSONArray();
        userIds.put(username);
        String hasil = "";
        Session session = null;

        String emailUser = null;
        String password;
        String passwordDecript;
        String subject = Common.getKonfigurasi("default_title_forgot_password",
                "Pemberitahuan password untuk login ke Sistem Informasi Akademik ").getNilai();
        String body = null;
        String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
        Mahasiswa mahasiswa = null;
        Tbmuser user = null;
        Siswa siswa = null;
        try {
            session = HibernateUtil.currentNativeSession();
            user = (Tbmuser) session.createCriteria(Tbmuser.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                    .add(Restrictions.eq("userId", username)).uniqueResult();
            mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                    .add(Restrictions.eq("nim", username)).uniqueResult();
            siswa = (Siswa) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
                    .add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
                    .add(Restrictions.eq("nomorIndukNasional", username)).uniqueResult();
            Dosen dosen = user == null ? null : user.getDosen();
            Pegawai pegawai = user == null ? null : user.getPegawai();

            if (dosen != null) {
                if (dosen.getEmail() == null || dosen.getEmail().trim().length() == 0
                        || !Common.isValidEmailAddress(dosen.getEmail().trim())) {
                    hasil = "Email anda belum terdaftar atau tidak sesuai, silahkan hubungi admin untuk memasukkan email anda";
                }
                emailUser = dosen.getEmail();
                password = user.getUserPassword();
                passwordDecript = Common.desEncrypter.get().decrypt(password);
                body = "ID pengguna anda : " + user.getUserId() + " . Kata sandi : " + passwordDecript;
            } else if (pegawai != null) {
                if (pegawai.getEmail() == null || pegawai.getEmail().trim().length() == 0
                        || !Common.isValidEmailAddress(pegawai.getEmail().trim())) {
                    hasil = "Email anda belum terdaftar atau tidak sesuai, silahkan hubungi admin untuk memasukkan email anda";
                }
                emailUser = pegawai.getEmail();
                password = user.getUserPassword();
                passwordDecript = Common.desEncrypter.get().decrypt(password);
                body = "ID pengguna anda : " + user.getUserId() + " . Kata sandi : " + passwordDecript;
            } else if (user != null) {
                if (user.getEmail() == null || user.getEmail().trim().length() == 0
                        || !Common.isValidEmailAddress(user.getEmail().trim())) {
                    hasil = "Email anda belum terdaftar atau tidak sesuai, silahkan hubungi admin untuk memasukkan email anda";
                }
                emailUser = user.getEmail();
                password = user.getUserPassword();
                passwordDecript = Common.desEncrypter.get().decrypt(password);
                body = "ID pengguna anda : " + user.getUserId() + " . Kata sandi : " + passwordDecript;
            } else {
                if (mahasiswa == null && siswa == null) {
                    hasil = "Id pengguna tidak ditemukan";
                }
                if (mahasiswa != null) {
                    if (mahasiswa.getEmail() == null || mahasiswa.getEmail().trim().length() == 0
                            || !Common.isValidEmailAddress(mahasiswa.getEmail().trim())) {
                        hasil = "Email anda belum di terdaftar atau tidak sesuai, silahkan hubungi admin untuk memasukkan email anda";
                    }
                    emailUser = mahasiswa.getEmail() == null ? null : mahasiswa.getEmail().split(",")[0];
                    password = mahasiswa.getPass();
                    passwordDecript = Common.desEncrypter.get().decrypt(password);
                    body = "Username anda : " + mahasiswa.getNim() + " . Password : " + passwordDecript;
                }
                if (siswa != null) {
                    if (siswa.getAlamatEmail() == null || siswa.getAlamatEmail().trim().length() == 0
                            || !Common.isValidEmailAddress(siswa.getAlamatEmail().trim())) {
                        hasil = "Email anda belum di terdaftar atau tidak sesuai, silahkan hubungi admin untuk memasukkan email anda";
                    }
                    emailUser = siswa.getAlamatEmail();
                    password = siswa.getPass();
                    passwordDecript = Common.desEncrypter.get().decrypt(password);
                    body = "Username anda : " + (siswa.getNomorInduk() == null ? username : siswa.getNomorInduk())
                            + " . Password : " + passwordDecript;
                }
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        } finally {
            Common.closeNativeSessionQuietly(session);
        }

        if (hasil != null && hasil.trim().length() > 0) {
            return hasil;
        }
        try {
            try {
                body += "<br><br>Silahkan login kembali ke " + Common.getRequestHostWithProtocol();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonSecurityLoginHelper.java:1185");
            }
            MailSender.sendMail(userIds, subject, body, sender, emailUser,
                    siswa != null ? siswa : mahasiswa != null ? mahasiswa : user);
            hasil = "Password anda telah dikirim ke email anda (" + emailUser
                    + "), silahkan cek email anda di inbox, atau juga mungkin bisa jadi masuk di spam.";
            return hasil;
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return "Terjadi kesalahan : " + e.getMessage();
        }
    }

}
