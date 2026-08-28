package ais.common;

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
public class CommonAcademicSyncHelper extends Common {


	private static final Logger log = Logger.getLogger(CommonAcademicSyncHelper.class);
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
			MyMessageboxConfig.showFormat("{V1}{V2}", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, pesan, detail);
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonAcademicSyncHelper.java:462");
		}
	}




	public static void initDefaultJudisium() {
		Session session = HibernateUtil.currentSession();
		Integer count = ((Number) session.createCriteria(Judisium.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			Judisium judisium = new Judisium();
			judisium.setNama("Kumlaude");
			judisium.setNilaiMulai(3.495);
			judisium.setNilaiSampai(4.0);
			session.save(judisium);

			judisium = new Judisium();
			judisium.setNama("Amat Baik");
			judisium.setNilaiMulai(2.75);
			judisium.setNilaiSampai(3.494);
			session.save(judisium);

			judisium = new Judisium();
			judisium.setNama("Baik");
			judisium.setNilaiMulai(2.00);
			judisium.setNilaiSampai(2.7499999999);
			session.save(judisium);
		}
	}



	public static Perkuliahan checkKelasJadwalPerkuliahan(Long id, Jurusan jurusan, String program, String hari,
			Double mulai, Double selesai, String tahunAjaran, String jenisSemester, String kelas, Integer semester,
			Html tampilWarning, Integer semesterpendek, Boolean minggu1, Boolean minggu2, Boolean minggu3,
			Boolean minggu4, Boolean minggu5, Date perkuliahanDimulai, Date perkuliahanSampai, Matakuliah matakuliah)
			throws Exception {

		if (kelas == null || kelas.trim().isEmpty() || hari == null || hari.isEmpty() || mulai == null
				|| selesai == null) {
			return null;
		}

		Session session = HibernateUtil.currentNativeSession();
		List<Perkuliahan> counts = null;

		if (id == null) {
			counts = ConstantValues
					.simpleList(
							session.createCriteria(Perkuliahan.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

									.add(Common.getMulaiSampaiCriterion(perkuliahanDimulai, perkuliahanSampai))

									.add(Restrictions.sqlRestriction("(minggu1 = " + minggu1 + " or minggu2 = "
											+ minggu2 + " or minggu3 = " + minggu3 + " or minggu4 = " + minggu4
											+ " or minggu5 = " + minggu5 + ")"))

									.add(Restrictions.eq("ganjilGenap", jenisSemester.toString()))

									.add(semesterpendek == null ? Restrictions.isNull("statusSemesterPendek")
											: Restrictions.eq("statusSemesterPendek", semesterpendek))

									.add(Restrictions.sqlRestriction("true")).add(Restrictions.eq("jurusan", jurusan))
									.add(Restrictions.eq("program", program))
									.add(Restrictions.eq("tahunAjaran", tahunAjaran))
									.add(Restrictions.eq("semester", semester))
									.add(Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT))
									.add(hari == null || hari.trim().equals("")
											? Restrictions.or(
													Restrictions.and(Restrictions.isNull("hari"),
															Restrictions.sqlRestriction("true")),
													Restrictions.and(Restrictions.eq("hari", ""),
															Restrictions.sqlRestriction("true")))
											: Restrictions.eq("hari", hari))

									.add(Common.createOrJadwalMulaiSelesai(mulai, selesai)),
							Perkuliahan.class);
		} else {
			counts = ConstantValues.simpleList(
					session.createCriteria(Perkuliahan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

							.add(Restrictions.ne("id", id))

							.add(Common.getMulaiSampaiCriterion(perkuliahanDimulai, perkuliahanSampai))

							.add(Restrictions.eq("ganjilGenap", jenisSemester.toString()))

							.add(Restrictions.sqlRestriction(
									"(minggu1 = " + minggu1 + " or minggu2 = " + minggu2 + " or minggu3 = " + minggu3
											+ " or minggu4 = " + minggu4 + " or minggu5 = " + minggu5 + ")"))

							.add(semesterpendek == null ? Restrictions.isNull("statusSemesterPendek")
									: Restrictions.eq("statusSemesterPendek", semesterpendek))

							.add(Restrictions.sqlRestriction("true")).add(Restrictions.eq("program", program))
							.add(Restrictions.eq("jurusan", jurusan)).add(Restrictions.eq("tahunAjaran", tahunAjaran))
							.add(Restrictions.eq("semester", semester))
							.add(Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT))
							.add(hari == null || hari.trim().equals("") ? Restrictions.or(
									Restrictions.and(Restrictions.isNull("hari"), Restrictions.sqlRestriction("true")),
									Restrictions.and(Restrictions.eq("hari", ""), Restrictions.sqlRestriction("true")))
									: Restrictions.eq("hari", hari))
							.add(Common.createOrJadwalMulaiSelesai(mulai, selesai)),
					Perkuliahan.class);
		}

		System.out.println("checkKelasJadwalPerkuliahan -> " + counts);
		if (counts != null) {
			for (Perkuliahan count : counts) {

				if (tampilWarning == null) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, Bapak/Ibu. Terjadi bentrok jadwal di Kelas Perkuliahan. Jadwal sudah ada untuk Program Studi {V1}, program {V2}, semester {V3}, kelas {V4}, pada hari {V5} dari pukul {V6} sampai pukul {V7} oleh Dosen {V8}, matakuliah {V9}, kelas {V10} {V11}. Langkah yang dapat dilakukan: (1) periksa kembali jadwal yang mengalami bentrok; (2) sesuaikan hari, jam, kelas, atau dosen pengampu; (3) simpan ulang jadwal setelah tidak ada bentrok.",
							"Peringatan Bentrok di Kelas Perkuliahan", 1, MyMessageboxConfig.EXCLAMATION,
							jurusan.getNama(), program, count.getSemester(), kelas, count.getHari(),
							count.getWaktuMulai(), count.getWaktuSelesai(),
							(count.getDosen1() == null ? "" : count.getDosen1().getNama()),
							(count.getMatakuliah() == null ? "" : count.getMatakuliah().getNama()),
							count.getSemester(), count.getKelas());
				} else {

					String val = tampilWarning.getContent() + "<li><font color='red'>";
					val += "Peringatan Bentrok di Kelas Perkuliahan..<br>Jadwal sudah ada untuk Prodi "
							+ jurusan.getNama() + ", program " + program + ", semester '" + count.getSemester()
							+ "', kelas " + kelas + ", dan hari '" + count.getHari() + "' dari jam '"
							+ count.getWaktuMulai() + "' sampai jam " + count.getWaktuSelesai() + "' oleh Dosen "
							+ (count.getDosen1() == null ? "" : count.getDosen1().getNama()) + ", matakuliah "
							+ (count.getMatakuliah() == null ? "" : count.getMatakuliah().getNama()) + ", kelas "
							+ count.getSemester() + " " + count.getKelas() + ".. </font></li>";

					tampilWarning.setContent(val);
				}
			}
		}

		HibernateUtil.closeSession();

		return counts == null || counts.isEmpty() ? null : counts.get(0);
	}



	public static TemplatePerkuliahanDetail checkKelasJadwalTemplatePerkuliahanDetail(
			TemplatePerkuliahan templatePerkuliahan, Long id, Jurusan jurusan, String program, String hari,
			Double mulai, Double selesai, String jenisSemester, String kelas, Integer semester, Matakuliah matakuliah,
			Html tampilWarning) throws Exception {

		if (kelas == null) {
			return null;
		}

		Session session = HibernateUtil.currentNativeSession();
		TemplatePerkuliahanDetail count = null;

		// System.out.println("jurusan = " + jurusan);
		// System.out.println("program = " + program);
		// System.out.println("semester = " + semester);
		// System.out.println("kelas = " + kelas);
		// System.out.println("hari = " + hari);

		// System.out.println("mulai = " + mulai);
		// System.out.println("selesai = " + selesai);
		// System.out.println("id = " + id);

		if (id == null) {
			count = (TemplatePerkuliahanDetail) (session.createCriteria(TemplatePerkuliahanDetail.class)
					.add(jenisSemester.toString().equalsIgnoreCase(Perkuliahan.GENAP)
							? Restrictions.in("semester", Common.genap)
							: Restrictions.in("semester", Common.ganjil))
					.add(Restrictions.eq("matakuliah", matakuliah)).add(Restrictions.eq("jurusan", jurusan))
					.add(Restrictions.eq("program", program))
					.add(Restrictions.eq("templatePerkuliahan",
							templatePerkuliahan))
					.add(Restrictions.eq("semester", semester))
					.add(Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT))
					.add(hari == null || hari.trim().equals("")
							? Restrictions.or(
									Restrictions.and(
											Restrictions.and(Restrictions.isNull("hari"),
													Restrictions.eq("matakuliah", matakuliah)),
											Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT)),
									Restrictions.and(
											Restrictions.and(Restrictions.eq("hari", ""),
													Restrictions.eq("matakuliah", matakuliah)),
											Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT)))
							: Restrictions.eq("hari", hari))
					.add(mulai == null && selesai == null
							? Restrictions.and(
									(Restrictions.and(
											Restrictions.and(Restrictions.isNull("waktuMulai"),
													Restrictions.eq("matakuliah", matakuliah)),
											Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT))),
									Restrictions.and(
											Restrictions.and(Restrictions.isNull("waktuSelesai"),
													Restrictions.eq("matakuliah", matakuliah)),
											Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT)))
							: mulai == null
									? Restrictions.and(
											Restrictions.and(Restrictions.isNull("waktuMulai"),
													Restrictions.eq("matakuliah", matakuliah)),
											Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT))
									: selesai == null
											? Restrictions.and(
													Restrictions.and(Restrictions.isNull("waktuSelesai"),
															Restrictions.eq("matakuliah", matakuliah)),
													Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT))
											: Common.createOrJadwalMulaiSelesai(mulai, selesai))
					.setMaxResults(1).uniqueResult());
		} else {
			count = (TemplatePerkuliahanDetail) (session.createCriteria(TemplatePerkuliahanDetail.class)
					.add(Restrictions.ne("id", id))
					.add(jenisSemester.toString().equalsIgnoreCase(Perkuliahan.GENAP)
							? Restrictions.in("semester", Common.genap)
							: Restrictions.in("semester", Common.ganjil))
					.add(Restrictions.eq("matakuliah", matakuliah)).add(Restrictions.eq("program", program))
					.add(Restrictions.eq("jurusan", jurusan))
					.add(Restrictions.eq("templatePerkuliahan",
							templatePerkuliahan))
					.add(Restrictions.eq("semester", semester))
					.add(Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT)).add(
							hari == null || hari.trim().equals("")
									? Restrictions.or(
											Restrictions.and(
													Restrictions.and(Restrictions.isNull("hari"),
															Restrictions.eq("matakuliah", matakuliah)),
													Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT)),
											Restrictions.and(
													Restrictions.and(Restrictions.eq("hari", ""),
															Restrictions.eq("matakuliah", matakuliah)),
													Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT)))
									: Restrictions.eq("hari", hari))
					.add(mulai == null && selesai == null
							? Restrictions.and(
									(Restrictions.and(
											Restrictions.and(Restrictions.isNull("waktuMulai"),
													Restrictions.eq("matakuliah", matakuliah)),
											Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT))),
									Restrictions.and(
											Restrictions.and(Restrictions.isNull("waktuSelesai"),
													Restrictions.eq("matakuliah", matakuliah)),
											Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT)))
							: mulai == null
									? Restrictions.and(
											Restrictions.and(Restrictions.isNull("waktuMulai"),
													Restrictions.eq("matakuliah", matakuliah)),
											Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT))
									: selesai == null
											? Restrictions.and(
													Restrictions.and(Restrictions.isNull("waktuSelesai"),
															Restrictions.eq("matakuliah", matakuliah)),
													Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT))
											: Common.createOrJadwalMulaiSelesai(mulai, selesai))
					.setMaxResults(1).uniqueResult());
		}

		HibernateUtil.closeSession();

		if (count != null) {

			if (count.getRuang() == null && count.getHari() == null && count.getWaktuMulai() == null
					&& count.getDosen1() == null) {
				return null;
			}

			if (tampilWarning == null) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, Bapak/Ibu. Jadwal sudah ada untuk Program Studi {V1}, program {V2}, semester {V3}, kelas {V4}, pada hari {V5} dari pukul {V6} sampai pukul {V7} oleh Dosen {V8}, matakuliah {V9}, kelas {V10} {V11}. Catatan: apabila waktu selesai suatu jadwal template perkuliahan sama dengan waktu mulai jadwal lain atau sebaliknya, mohon tambahkan 1 menit pada waktu mulai atau kurangkan 1 menit pada waktu selesai. Sebagai contoh, apabila waktu selesai suatu jadwal adalah pukul 09.10, maka waktu mulai jadwal berikutnya dibuat pukul 09.11. Langkah yang dapat dilakukan: (1) periksa kembali jadwal yang mengalami bentrok; (2) sesuaikan waktu mulai atau waktu selesai sesuai catatan di atas; (3) simpan ulang jadwal setelah tidak ada bentrok.",
						"Peringatan Check Kelas Jadwal TemplatePerkuliahanDetail", 1, MyMessageboxConfig.EXCLAMATION,
						jurusan.getNama(), program, count.getSemester(), kelas, count.getHari(),
						count.getWaktuMulai(), count.getWaktuSelesai(),
						(count.getDosen1() == null ? "" : count.getDosen1().getNama()),
						(count.getMatakuliah() == null ? "" : count.getMatakuliah().getNama()),
						count.getSemester(), count.getKelas());
			} else {

				if (kelas == null || count.getHari() == null || count.getWaktuMulai() == null) {
					return null;
				}

				String val = tampilWarning.getContent() + "<li><font color='red'>";
				val += "Jadwal sudah ada untuk Prodi " + jurusan.getNama() + ", program " + program + ", semester '"
						+ count.getSemester() + "', kelas " + kelas + ", dan hari '" + count.getHari() + "' dari jam '"
						+ count.getWaktuMulai() + "' sampai jam " + count.getWaktuSelesai() + "' oleh Dosen "
						+ (count.getDosen1() == null ? "" : count.getDosen1().getNama()) + ", matakuliah "
						+ (count.getMatakuliah() == null ? "" : count.getMatakuliah().getNama()) + ", kelas "
						+ count.getSemester() + " " + count.getKelas()
						+ ". \n\nCatatan: Jika masalahnya adalah waktu selesai suatu jadwal template perkuliahan sama dengan waktu mulai atau sebaliknya. Tambahkan waktu mulai 1 menit atau kurangkan waktu selesai 1 menit. Misalnya: waktu selesai suatu jadwal template perkuliahan jam 9.10, maka anda harus membuat waktu mulai jadwal template perkuliahan yang lain jam 9.11 (dilebihkan 1 menit)";

				val += "</font></li>";
				tampilWarning.setContent(val);
			}
		}

		return count;
	}



	public static PembatasanNilaiIPKUntukPengambilanKRS getIpkUntukPengambilanKRS(Mahasiswa mahasiswa, Integer semester,
			Integer tahunAngkatan, Fakultas fakultas, Jurusan jurusan, String program, Integer semesterPendek) {
		try {
			return (PembatasanNilaiIPKUntukPengambilanKRS) getIpkUntukPengambilanKRSDenganIPLast(mahasiswa, semester,
					tahunAngkatan, fakultas, jurusan, program, semesterPendek)[0];
		} catch (Exception e) {
			return null;
		}
	}



	public static Object[] getIpkUntukPengambilanKRSDenganIPLast(Mahasiswa mahasiswa, Integer semester,
			Integer tahunAngkatan, Fakultas fakultas, Jurusan jurusan, String program, Integer semesterPendek) {
		Session session = HibernateUtil.currentSession();
		int countBatas = ((Number) session.createCriteria(PembatasanNilaiIPKUntukPengambilanKRS.class)
				.add(semesterPendek == null ? Restrictions.isNull("semesterPendek")
						: Restrictions.eq("semesterPendek", semesterPendek))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("mahasiswa", mahasiswa)).setProjection(Projections.rowCount()).uniqueResult())
				.intValue();
		if (semester <= 1 && countBatas == 0) {
			return null;
		}

		Double iplast = Common.ipTerakhir(mahasiswa, semester);

		if (countBatas > 0) {

			PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRS = (PembatasanNilaiIPKUntukPengambilanKRS)

			ConstantValues.simpleObject(
					session.createCriteria(PembatasanNilaiIPKUntukPengambilanKRS.class)
							.add(semesterPendek == null ? Restrictions.isNull("semesterPendek")
									: Restrictions.eq("semesterPendek", semesterPendek))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("mahasiswa", mahasiswa)).createAlias("program", "program")
							.add(Restrictions.eq("fakultas", fakultas)).add(Restrictions.eq("jurusan", jurusan))
							.add(Restrictions.eq("program.nama", program))
							.add(Restrictions.le("minimumAngkatan", tahunAngkatan))
							.add(Restrictions.le("batasTerendahIPK", iplast)).addOrder(Order.desc("minimumAngkatan"))
							.addOrder(Order.desc("batasTerendahIPK")).setMaxResults(1),
					PembatasanNilaiIPKUntukPengambilanKRS.class);

			if (pembatasanNilaiIPKUntukPengambilanKRS == null) {
				pembatasanNilaiIPKUntukPengambilanKRS = (PembatasanNilaiIPKUntukPengambilanKRS) ConstantValues
						.simpleObject(
								session.createCriteria(PembatasanNilaiIPKUntukPengambilanKRS.class)
										.add(semesterPendek == null ? Restrictions.isNull("semesterPendek")
												: Restrictions.eq("semesterPendek", semesterPendek))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("mahasiswa", mahasiswa))
										.add(Restrictions.eq("fakultas", fakultas))
										.add(Restrictions.eq("jurusan", jurusan))
										.add(Restrictions.le("minimumAngkatan", tahunAngkatan))
										.add(Restrictions.le("batasTerendahIPK", iplast))
										.addOrder(Order.desc("minimumAngkatan"))
										.addOrder(Order.desc("batasTerendahIPK")).setMaxResults(1),
								PembatasanNilaiIPKUntukPengambilanKRS.class);
			}

			if (pembatasanNilaiIPKUntukPengambilanKRS == null) {
				pembatasanNilaiIPKUntukPengambilanKRS = (PembatasanNilaiIPKUntukPengambilanKRS) ConstantValues
						.simpleObject(
								session.createCriteria(PembatasanNilaiIPKUntukPengambilanKRS.class)
										.add(semesterPendek == null ? Restrictions.isNull("semesterPendek")
												: Restrictions.eq("semesterPendek", semesterPendek))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("mahasiswa", mahasiswa))
										.add(Restrictions.eq("fakultas", fakultas)).createAlias("program", "program")
										.add(Restrictions.eq("program.nama", program))
										.add(Restrictions.le("minimumAngkatan", tahunAngkatan))
										.add(Restrictions.le("batasTerendahIPK", iplast))
										.addOrder(Order.desc("minimumAngkatan"))
										.addOrder(Order.desc("batasTerendahIPK")).setMaxResults(1),
								PembatasanNilaiIPKUntukPengambilanKRS.class);
			}

			if (pembatasanNilaiIPKUntukPengambilanKRS == null) {
				pembatasanNilaiIPKUntukPengambilanKRS = (PembatasanNilaiIPKUntukPengambilanKRS) ConstantValues
						.simpleObject(
								session.createCriteria(PembatasanNilaiIPKUntukPengambilanKRS.class)
										.add(semesterPendek == null ? Restrictions.isNull("semesterPendek")
												: Restrictions.eq("semesterPendek", semesterPendek))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("mahasiswa", mahasiswa))
										.add(Restrictions.eq("fakultas", fakultas))
										.add(Restrictions.le("minimumAngkatan", tahunAngkatan))
										.add(Restrictions.le("batasTerendahIPK", iplast))
										.addOrder(Order.desc("minimumAngkatan"))
										.addOrder(Order.desc("batasTerendahIPK")).setMaxResults(1),
								PembatasanNilaiIPKUntukPengambilanKRS.class);
			}

			if (pembatasanNilaiIPKUntukPengambilanKRS == null) {
				pembatasanNilaiIPKUntukPengambilanKRS = (PembatasanNilaiIPKUntukPengambilanKRS) ConstantValues
						.simpleObject(
								session.createCriteria(PembatasanNilaiIPKUntukPengambilanKRS.class)
										.add(semesterPendek == null ? Restrictions.isNull("semesterPendek")
												: Restrictions.eq("semesterPendek", semesterPendek))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("mahasiswa", mahasiswa))
										.add(Restrictions.eq("fakultas", fakultas))
										.add(Restrictions.le("batasTerendahIPK", iplast))
										.addOrder(Order.desc("minimumAngkatan"))
										.addOrder(Order.desc("batasTerendahIPK")).setMaxResults(1),
								PembatasanNilaiIPKUntukPengambilanKRS.class);
			}

			if (pembatasanNilaiIPKUntukPengambilanKRS == null) {
				pembatasanNilaiIPKUntukPengambilanKRS = (PembatasanNilaiIPKUntukPengambilanKRS) ConstantValues
						.simpleObject(
								session.createCriteria(PembatasanNilaiIPKUntukPengambilanKRS.class)
										.add(semesterPendek == null ? Restrictions.isNull("semesterPendek")
												: Restrictions.eq("semesterPendek", semesterPendek))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("mahasiswa", mahasiswa)).createAlias("program", "program")
										.add(Restrictions.eq("program.nama", program))
										.add(Restrictions.le("minimumAngkatan", tahunAngkatan))
										.add(Restrictions.le("batasTerendahIPK", iplast))
										.addOrder(Order.desc("minimumAngkatan"))
										.addOrder(Order.desc("batasTerendahIPK")).setMaxResults(1),
								PembatasanNilaiIPKUntukPengambilanKRS.class);
			}

			if (pembatasanNilaiIPKUntukPengambilanKRS == null) {
				pembatasanNilaiIPKUntukPengambilanKRS = (PembatasanNilaiIPKUntukPengambilanKRS) ConstantValues
						.simpleObject(
								session.createCriteria(PembatasanNilaiIPKUntukPengambilanKRS.class)
										.add(semesterPendek == null ? Restrictions.isNull("semesterPendek")
												: Restrictions.eq("semesterPendek", semesterPendek))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("mahasiswa", mahasiswa))
										.add(Restrictions.le("minimumAngkatan", tahunAngkatan))
										.add(Restrictions.le("batasTerendahIPK", iplast))
										.addOrder(Order.desc("minimumAngkatan"))
										.addOrder(Order.desc("batasTerendahIPK")).setMaxResults(1),
								PembatasanNilaiIPKUntukPengambilanKRS.class);
			}

			// System.out.println("pembatasanNilaiIPKUntukPengambilanKRS = " +
			// pembatasanNilaiIPKUntukPengambilanKRS
			// + ", iplast = " + iplast + ", semester = " + semester + ",
			// mahasiswa = " + mahasiswa);
			return new Object[] { pembatasanNilaiIPKUntukPengambilanKRS, iplast };

		} else {

			PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRS = (PembatasanNilaiIPKUntukPengambilanKRS) ConstantValues
					.simpleObject(session.createCriteria(PembatasanNilaiIPKUntukPengambilanKRS.class)
							.add(Restrictions.isNull("mahasiswa"))
							.add(semesterPendek == null ? Restrictions.isNull("semesterPendek")
									: Restrictions.eq("semesterPendek", semesterPendek))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.createAlias("program", "program").add(Restrictions.eq("fakultas", fakultas))
							.add(Restrictions.eq("jurusan", jurusan)).add(Restrictions.eq("program.nama", program))
							.add(Restrictions.le("minimumAngkatan", tahunAngkatan))
							.add(Restrictions.le("batasTerendahIPK", iplast)).addOrder(Order.desc("minimumAngkatan"))
							.addOrder(Order.desc("batasTerendahIPK")).setMaxResults(1),
							PembatasanNilaiIPKUntukPengambilanKRS.class);

			if (pembatasanNilaiIPKUntukPengambilanKRS == null) {
				pembatasanNilaiIPKUntukPengambilanKRS = (PembatasanNilaiIPKUntukPengambilanKRS) ConstantValues
						.simpleObject(
								session.createCriteria(PembatasanNilaiIPKUntukPengambilanKRS.class)
										.add(Restrictions.isNull("mahasiswa"))
										.add(semesterPendek == null ? Restrictions.isNull("semesterPendek")
												: Restrictions.eq("semesterPendek", semesterPendek))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("fakultas", fakultas))
										.add(Restrictions.eq("jurusan", jurusan))
										.add(Restrictions.le("minimumAngkatan", tahunAngkatan))
										.add(Restrictions.le("batasTerendahIPK", iplast))
										.addOrder(Order.desc("minimumAngkatan"))
										.addOrder(Order.desc("batasTerendahIPK")).setMaxResults(1),
								PembatasanNilaiIPKUntukPengambilanKRS.class);
			}

			if (pembatasanNilaiIPKUntukPengambilanKRS == null) {
				pembatasanNilaiIPKUntukPengambilanKRS = (PembatasanNilaiIPKUntukPengambilanKRS) ConstantValues
						.simpleObject(
								session.createCriteria(PembatasanNilaiIPKUntukPengambilanKRS.class)
										.add(Restrictions.isNull("mahasiswa"))
										.add(semesterPendek == null ? Restrictions.isNull("semesterPendek")
												: Restrictions.eq("semesterPendek", semesterPendek))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("fakultas", fakultas)).createAlias("program", "program")
										.add(Restrictions.eq("program.nama", program))
										.add(Restrictions.le("minimumAngkatan", tahunAngkatan))
										.add(Restrictions.le("batasTerendahIPK", iplast))
										.addOrder(Order.desc("minimumAngkatan"))
										.addOrder(Order.desc("batasTerendahIPK")).setMaxResults(1),
								PembatasanNilaiIPKUntukPengambilanKRS.class);
			}

			if (pembatasanNilaiIPKUntukPengambilanKRS == null) {
				pembatasanNilaiIPKUntukPengambilanKRS = (PembatasanNilaiIPKUntukPengambilanKRS) ConstantValues
						.simpleObject(
								session.createCriteria(PembatasanNilaiIPKUntukPengambilanKRS.class)
										.add(Restrictions.isNull("mahasiswa"))
										.add(semesterPendek == null ? Restrictions.isNull("semesterPendek")
												: Restrictions.eq("semesterPendek", semesterPendek))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("fakultas", fakultas))
										.add(Restrictions.le("minimumAngkatan", tahunAngkatan))
										.add(Restrictions.le("batasTerendahIPK", iplast))
										.addOrder(Order.desc("minimumAngkatan"))
										.addOrder(Order.desc("batasTerendahIPK")).setMaxResults(1),
								PembatasanNilaiIPKUntukPengambilanKRS.class);
			}

			if (pembatasanNilaiIPKUntukPengambilanKRS == null) {
				pembatasanNilaiIPKUntukPengambilanKRS = (PembatasanNilaiIPKUntukPengambilanKRS) ConstantValues
						.simpleObject(
								session.createCriteria(PembatasanNilaiIPKUntukPengambilanKRS.class)
										.add(Restrictions.isNull("mahasiswa"))
										.add(semesterPendek == null ? Restrictions.isNull("semesterPendek")
												: Restrictions.eq("semesterPendek", semesterPendek))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("fakultas", fakultas))
										.add(Restrictions.le("batasTerendahIPK", iplast))
										.addOrder(Order.desc("minimumAngkatan"))
										.addOrder(Order.desc("batasTerendahIPK")).setMaxResults(1),
								PembatasanNilaiIPKUntukPengambilanKRS.class);
			}

			if (pembatasanNilaiIPKUntukPengambilanKRS == null) {
				pembatasanNilaiIPKUntukPengambilanKRS = (PembatasanNilaiIPKUntukPengambilanKRS) ConstantValues
						.simpleObject(
								session.createCriteria(PembatasanNilaiIPKUntukPengambilanKRS.class)
										.add(Restrictions.isNull("mahasiswa"))
										.add(semesterPendek == null ? Restrictions.isNull("semesterPendek")
												: Restrictions.eq("semesterPendek", semesterPendek))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.createAlias("program", "program").add(Restrictions.eq("program.nama", program))
										.add(Restrictions.le("minimumAngkatan", tahunAngkatan))
										.add(Restrictions.le("batasTerendahIPK", iplast))
										.addOrder(Order.desc("minimumAngkatan"))
										.addOrder(Order.desc("batasTerendahIPK")).setMaxResults(1),
								PembatasanNilaiIPKUntukPengambilanKRS.class);
			}

			if (pembatasanNilaiIPKUntukPengambilanKRS == null) {
				pembatasanNilaiIPKUntukPengambilanKRS = (PembatasanNilaiIPKUntukPengambilanKRS) ConstantValues
						.simpleObject(
								session.createCriteria(PembatasanNilaiIPKUntukPengambilanKRS.class)
										.add(Restrictions.isNull("mahasiswa"))
										.add(semesterPendek == null ? Restrictions.isNull("semesterPendek")
												: Restrictions.eq("semesterPendek", semesterPendek))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.le("minimumAngkatan", tahunAngkatan))
										.add(Restrictions.le("batasTerendahIPK", iplast))
										.addOrder(Order.desc("minimumAngkatan"))
										.addOrder(Order.desc("batasTerendahIPK")).setMaxResults(1),
								PembatasanNilaiIPKUntukPengambilanKRS.class);
			}

			// System.out.println("pembatasanNilaiIPKUntukPengambilanKRS = " +
			// pembatasanNilaiIPKUntukPengambilanKRS
			// + ", iplast = " + iplast + ", semester = " + semester + ",
			// mahasiswa = " + mahasiswa);

			return new Object[] { pembatasanNilaiIPKUntukPengambilanKRS, iplast };
		}
	}



	public static void createDefaultFormatNilai(Perkuliahan perkuliahan) {

		PembombotanNilai selectedPembombotanNilai = perkuliahan.getPembombotanNilai();
		Session session = HibernateUtil.currentSession();

		FormatNilai formatNilaiAbsen = (FormatNilai) session.createCriteria(FormatNilai.class)
				.add(Restrictions.eq("perkuliahan", perkuliahan))
				.add(Restrictions.eq("statusPertemuan", ConstantValues.ABSEN)).setMaxResults(1).uniqueResult();

		FormatNilai formatNilaiForm = (FormatNilai) session.createCriteria(FormatNilai.class)
				.add(Restrictions.eq("perkuliahan", perkuliahan))
				.add(Restrictions.eq("statusPertemuan", ConstantValues.FORM)).setMaxResults(1).uniqueResult();

		FormatNilai formatNilaiUts = (FormatNilai) session.createCriteria(FormatNilai.class)
				.add(Restrictions.eq("perkuliahan", perkuliahan))
				.add(Restrictions.eq("statusPertemuan", ConstantValues.UTS)).setMaxResults(1).uniqueResult();

		FormatNilai formatNilaiUas = (FormatNilai) session.createCriteria(FormatNilai.class)
				.add(Restrictions.eq("perkuliahan", perkuliahan))
				.add(Restrictions.eq("statusPertemuan", ConstantValues.UAS)).setMaxResults(1).uniqueResult();

		if (formatNilaiAbsen == null) {
			formatNilaiAbsen = new FormatNilai();
			formatNilaiAbsen.setPersen(selectedPembombotanNilai.getAbsen().doubleValue());
			formatNilaiAbsen.setPerkuliahan(perkuliahan);
			formatNilaiAbsen.setStatusPertemuan(ConstantValues.ABSEN);
			session.save(formatNilaiAbsen);
		}

		if (formatNilaiForm == null) {
			formatNilaiForm = new FormatNilai();
			formatNilaiForm.setPersen(selectedPembombotanNilai.getForm().doubleValue());
			formatNilaiForm.setPerkuliahan(perkuliahan);
			formatNilaiForm.setStatusPertemuan(ConstantValues.FORM);
			session.save(formatNilaiForm);
		}

		if (formatNilaiUts == null) {
			formatNilaiUts = new FormatNilai();
			formatNilaiUts.setPersen(selectedPembombotanNilai.getUts().doubleValue());
			formatNilaiUts.setPerkuliahan(perkuliahan);
			formatNilaiUts.setStatusPertemuan(ConstantValues.UTS);
			session.save(formatNilaiUts);
		}

		if (formatNilaiUas == null) {
			formatNilaiUas = new FormatNilai();
			formatNilaiUas.setPersen(selectedPembombotanNilai.getUas().doubleValue());
			formatNilaiUas.setPerkuliahan(perkuliahan);
			formatNilaiUas.setStatusPertemuan(ConstantValues.UAS);
			session.save(formatNilaiUas);
		}

	}



	public static void singkronisasiStatusMahasiswa(DataCriteria dataCriteria, String tahunAkademik,
			String jenisSemester, Integer mulai, Integer sampai, boolean statusMhs, boolean statusKrs,
			boolean perkuliahanMhs, boolean pembayaranMhs) {
		singkronisasiStatusMahasiswa(null, dataCriteria, tahunAkademik, jenisSemester, mulai, sampai, statusMhs,
				statusKrs, perkuliahanMhs, pembayaranMhs, false);
	}




	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static ais.common.LaporanUpload singkronisasiStatusMahasiswa(Label label, DataCriteria dataCriteria, String tahunAkademik,
			String jenisSemester, Integer mulai, Integer sampai, boolean statusMhs, boolean statusKrs,
			boolean perkuliahanMhs, boolean pembayaranMhs, boolean nonAktifkan) {
		final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Status Mahasiswa");
		Session session = null;
		List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();
		try {
			session = HibernateUtil.currentNativeSession();
			Object d = dataCriteria == null ? null : dataCriteria.initCriteria(true);
			if (d instanceof Criteria) {
				Criteria criteria = (Criteria) d;
				if (mulai != null && sampai != null) {
					criteria.add(Restrictions.between("tahunangkatan", mulai, sampai));
				}
				List<Mahasiswa> data = criteria.list();
				if (data != null) {
					mahasiswas.addAll(data);
				}
			} else if (d instanceof List) {
				mahasiswas.addAll((List) d);
			} else {
				List<Mahasiswa> data = session.createCriteria(Mahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE))).list();
				if (data != null) {
					mahasiswas.addAll(data);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			laporan.tambahCatatan("Gagal mengambil daftar mahasiswa yang akan disinkronkan: "
					+ ais.common.LaporanUpload.detailTeknisException(e));
		} finally {
			Common.closeNativeSessionQuietly(session);
		}

		int i = 0;
		int size = mahasiswas.size() == 0 ? 1 : mahasiswas.size();
		for (Mahasiswa mahasiswa : mahasiswas) {
			Session prosesSession = null;
			String kunciMhs = mahasiswa == null ? "-"
					: (mahasiswa.getNim() != null && !mahasiswa.getNim().trim().isEmpty() ? mahasiswa.getNim()
							: String.valueOf(mahasiswa));
			try {
				if (statusMhs || nonAktifkan) {
					HistoryStatusMahasiswaUtil.singkronisasiStatusMahasiswa(label, mahasiswa, tahunAkademik, jenisSemester,
							nonAktifkan);
				}

				if (statusKrs) {
					Common.singkronkanKrsMahasiswa(mahasiswa, mahasiswa.currentSemester(), mahasiswa.currentTahapan(), null,
							true);
				}

				if (perkuliahanMhs || pembayaranMhs) {
					prosesSession = HibernateUtil.currentNativeSession();
					if (perkuliahanMhs) {
						mahasiswa.reInitDetailperkuliahan(prosesSession);
					}
					if (pembayaranMhs) {
						mahasiswa.reInitKegiatan(prosesSession);
					}
				}

				if (label != null) {
					label.setValue("(" + (Common.numberFormat.get().format(i * 100.0 / size))
							+ " %) sinkronisasi data mahasiswa " + mahasiswa + " ..");
				}
				laporan.catatBerhasil(i, kunciMhs, "Sinkronisasi berhasil");
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				laporan.catatGagalDetail(i, kunciMhs, e);
			} finally {
				Common.closeNativeSessionQuietly(prosesSession);
			}
			i++;
		}
		mahasiswas.clear();
		if (label != null) {
			label.setValue("");
		}
		return laporan;
	}

	public static void hapusMatakuliahYangMelebihiKetentuan(Mahasiswa mahasiswa, Integer semester,
			final Integer tahapan, Integer semesterPendek, Integer jumlah) {
		if (mahasiswa == null || semester == null || jumlah == null) {
			return;
		}

		Integer tahapanAman = tahapan == null ? Integer.valueOf(0) : tahapan;
		int guard = 0;
		while (guard < 100) {
			Double[] batas = Common.getMinDanMaxIPK(mahasiswa, semester, semesterPendek);
			Integer maxsks = batas == null || batas.length == 0 || batas[0] == null ? Integer.valueOf(0)
					: Integer.valueOf(batas[0].intValue());
			if ((maxsks.intValue() - jumlah.intValue()) >= 0) {
				break;
			}

			Session session = null;
			Transaction transaction = null;
			boolean berhasilHapus = false;
			try {
				Criterion criterionSemester = tahapanAman.equals(Integer.valueOf(0)) ? Restrictions.eq("semester", semester)
						: Restrictions.sqlRestriction("true");
				Criterion criterionTahapan = tahapanAman.equals(Integer.valueOf(0)) ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahap", tahapanAman);

				session = HibernateUtil.currentNativeSession();
				Detailperkuliahan sudahDiambil = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
						.add(Restrictions.isNull("ikutiPerkuliahan")).add(criterionSemester)
						.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.lt("totalNilai", Double.valueOf(0.5)))
						.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)
						.createAlias("perkuliahan.kurikulumPunyaMatakuliah", "kurikulumPunyaMatakuliah", Criteria.LEFT_JOIN)
						.add(criterionTahapan).addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

				if (sudahDiambil == null) {
					break;
				}

				transaction = session.getTransaction();
				if (transaction == null || !transaction.isActive()) {
					transaction = session.beginTransaction();
				}
				Common.refreshDelete(session, sudahDiambil);
				transaction.commit();
				berhasilHapus = true;
			} catch (Exception e) {
				Common.rollbackQuietly(transaction);
				Common.tampilErrorJikaAdmin(e);
				break;
			} finally {
				Common.closeNativeSessionQuietly(session);
			}

			if (!berhasilHapus) {
				break;
			}
			guard++;
		}
	}

	public static HistoryStatusMahasiswa currentStatus(Mahasiswa mahasiswa) {
		KrsMahasiswa krsMahasiswa = ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, mahasiswa.currentSemester(),
				mahasiswa.currentTahapan(), null);
		return ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa);
	}



	public static HistoryStatusMahasiswa currentStatus(Mahasiswa mahasiswa, Integer tahap) {
		KrsMahasiswa krsMahasiswa = ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, mahasiswa.currentSemester(), tahap, null);
		return ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa);
	}



	public static HistoryStatusMahasiswa currentStatus(Mahasiswa mahasiswa, String tahunAkademik, Integer semester) {
		Integer tahap = mahasiswa == null ? null : mahasiswa.currentTahapan();

		KrsMahasiswa krsMahasiswa = ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, semester, tahap, null);

		return ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa);
	}



	public static Boolean checkMatakuliahPrasyarat(Matakuliah matakuliah, Mahasiswa mahasiswa, Integer semester)
			throws Exception {
		Session session = HibernateUtil.currentSession();

		List<MatakuliahPrasyarat> matakuliahPrasyarats = ConstantValues
				.simpleList(session.createCriteria(MatakuliahPrasyarat.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("matakuliah", matakuliah)), MatakuliahPrasyarat.class);

		if (!matakuliahPrasyarats.isEmpty()) {
			List<MatakuliahPrasyarat> yangDiatasMinimalSks = new ArrayList<MatakuliahPrasyarat>();
			for (MatakuliahPrasyarat matakuliahPrasyarat : matakuliahPrasyarats) {
				if (matakuliahPrasyarat.getMinimalSks() > 0 || matakuliahPrasyarat.getMinimalIpk() > 0.01) {
					yangDiatasMinimalSks.add(matakuliahPrasyarat);
				}
			}
			// System.out.println(
			// "matakuliahPrasyarat = " + matakuliahPrasyarats + "
			// yangDiatasMinimalSks " + yangDiatasMinimalSks);
			String syarat = "";
			if (!yangDiatasMinimalSks.isEmpty()) {
//				KrsMahasiswa krsMahasiswa = mahasiswa.ambilDefaultKrsMahasiswa(semester, null, null, session);
				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);
				if (krsMahasiswa != null) {
					for (MatakuliahPrasyarat matakuliahPrasyarat : yangDiatasMinimalSks) {

						String mk = matakuliahPrasyarat == null
								|| matakuliahPrasyarat.getMatakuliahPrasyarat() == null ? ""
										: "Kode = " + matakuliahPrasyarat.getMatakuliahPrasyarat().getKode() + ", nama = "
												+ matakuliahPrasyarat.getMatakuliahPrasyarat().getNama();

						if (matakuliahPrasyarat.getMatakuliahPrasyarat2() != null) {
							mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat2().getKode() + "-"
									+ matakuliahPrasyarat.getMatakuliahPrasyarat2().getNama();
						}
						if (matakuliahPrasyarat.getMatakuliahPrasyarat3() != null) {
							mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat3().getKode() + "-"
									+ matakuliahPrasyarat.getMatakuliahPrasyarat3().getNama();
						}
						if (matakuliahPrasyarat.getMatakuliahPrasyarat4() != null) {
							mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat4().getKode() + "-"
									+ matakuliahPrasyarat.getMatakuliahPrasyarat4().getNama();
						}
						if (matakuliahPrasyarat.getMatakuliahPrasyarat5() != null) {
							mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat5().getKode() + "-"
									+ matakuliahPrasyarat.getMatakuliahPrasyarat5().getNama();
						}
						if (matakuliahPrasyarat.getMatakuliahPrasyarat6() != null) {
							mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat6().getKode() + "-"
									+ matakuliahPrasyarat.getMatakuliahPrasyarat6().getNama();
						}
						if (matakuliahPrasyarat.getMatakuliahPrasyarat7() != null) {
							mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat7().getKode() + "-"
									+ matakuliahPrasyarat.getMatakuliahPrasyarat7().getNama();
						}
						if (matakuliahPrasyarat.getMatakuliahPrasyarat8() != null) {
							mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat8().getKode() + "-"
									+ matakuliahPrasyarat.getMatakuliahPrasyarat8().getNama();
						}
						if (matakuliahPrasyarat.getMatakuliahPrasyarat9() != null) {
							mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat9().getKode() + "-"
									+ matakuliahPrasyarat.getMatakuliahPrasyarat9().getNama();
						}
						if (matakuliahPrasyarat.getMatakuliahPrasyarat10() != null) {
							mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat10().getKode() + "-"
									+ matakuliahPrasyarat.getMatakuliahPrasyarat10().getNama();
						}

						if (matakuliahPrasyarat.getMinimalSks() > krsMahasiswa.getSksk()) {

							syarat += (mk + ", minimal sks boleh ambil = "
									+ Common.numberFormat.get().format(matakuliahPrasyarat.getMinimalSks()))
									+ ", sedangkan sks yang terkumpul di smt " + semester + " = "
									+ Common.numberFormat.get().format(krsMahasiswa.getSksk()) + ".\n";
						}

						if (matakuliahPrasyarat.getMinimalIpk() > krsMahasiswa.getIpk()) {

							syarat += (mk + ", minimal IPK boleh ambil = "
									+ Common.numberFormat.get().format(matakuliahPrasyarat.getMinimalIpk()))
									+ ", sedangkan IPK yang terkumpul di smt " + semester + " = "
									+ Common.numberFormat.get().format(krsMahasiswa.getIpk()) + ".\n";
						}
					}
				}
			}

			for (MatakuliahPrasyarat matakuliahPrasyarat : matakuliahPrasyarats) {
				// Prasyarat opsional: baris tanpa MK prasyarat tak punya syarat MK untuk dicek → lewati.
				if (matakuliahPrasyarat.getMatakuliahPrasyarat() == null) {
					continue;
				}

				List<String> kodes = new ArrayList<String>();
				List<Long> ids = new ArrayList<Long>();

				if (matakuliahPrasyarat.getHanyaBerdasarkanKode()) {
					kodes.add(matakuliahPrasyarat.getMatakuliahPrasyarat().getKode());
					if (matakuliahPrasyarat.getMatakuliahPrasyarat2() != null) {
						kodes.add(matakuliahPrasyarat.getMatakuliahPrasyarat2().getKode());
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat3() != null) {
						kodes.add(matakuliahPrasyarat.getMatakuliahPrasyarat3().getKode());
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat4() != null) {
						kodes.add(matakuliahPrasyarat.getMatakuliahPrasyarat4().getKode());
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat5() != null) {
						kodes.add(matakuliahPrasyarat.getMatakuliahPrasyarat5().getKode());
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat6() != null) {
						kodes.add(matakuliahPrasyarat.getMatakuliahPrasyarat6().getKode());
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat7() != null) {
						kodes.add(matakuliahPrasyarat.getMatakuliahPrasyarat7().getKode());
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat8() != null) {
						kodes.add(matakuliahPrasyarat.getMatakuliahPrasyarat8().getKode());
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat9() != null) {
						kodes.add(matakuliahPrasyarat.getMatakuliahPrasyarat9().getKode());
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat10() != null) {
						kodes.add(matakuliahPrasyarat.getMatakuliahPrasyarat10().getKode());
					}
				} else {
					ids.add(matakuliahPrasyarat.getMatakuliahPrasyarat().getId());
					if (matakuliahPrasyarat.getMatakuliahPrasyarat2() != null) {
						ids.add(matakuliahPrasyarat.getMatakuliahPrasyarat2().getId());
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat3() != null) {
						ids.add(matakuliahPrasyarat.getMatakuliahPrasyarat3().getId());
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat4() != null) {
						ids.add(matakuliahPrasyarat.getMatakuliahPrasyarat4().getId());
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat5() != null) {
						ids.add(matakuliahPrasyarat.getMatakuliahPrasyarat5().getId());
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat6() != null) {
						ids.add(matakuliahPrasyarat.getMatakuliahPrasyarat6().getId());
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat7() != null) {
						ids.add(matakuliahPrasyarat.getMatakuliahPrasyarat7().getId());
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat8() != null) {
						ids.add(matakuliahPrasyarat.getMatakuliahPrasyarat8().getId());
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat9() != null) {
						ids.add(matakuliahPrasyarat.getMatakuliahPrasyarat9().getId());
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat10() != null) {
						ids.add(matakuliahPrasyarat.getMatakuliahPrasyarat10().getId());
					}
				}

				int count = 0;

				List<Long> details = mahasiswa == null ? new ArrayList<Long>() : mahasiswa.ambilDetailperkuliahan();
				for (Long detailperkuliahanid : details) {
					Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
							.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
					if (detailperkuliahan != null) {
						if (detailperkuliahan.getPersetujuan() == null
								|| !detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {
							continue;
						}
						Matakuliah mk = detailperkuliahan.getMatakuliahKonversi() != null
								? detailperkuliahan.getMatakuliahKonversi()
								: detailperkuliahan.getPerkuliahan() != null
										? detailperkuliahan.getPerkuliahan().getMatakuliah()
										: null;
						if (mk == null) {
							continue;
						}

						if (detailperkuliahan.getTotalNilai() == null
								|| detailperkuliahan.getTotalNilai() < matakuliahPrasyarat.getMinimalNilaiLulus()) {
							continue;
						}

						if (matakuliahPrasyarat.getHanyaBerdasarkanKode()) {
							if (kodes.contains(mk.getKode())) {
								count++;
							}
						} else {
							if (ids.contains(mk.getId())) {
								count++;
							}
						}
					}
				}
				details = null;

				// System.out.println("matakuliahPrasyarat kodes = " + kodes + "
				// ids = " + ids + ", count " + count);

				if (count == 0) {

					String mk = matakuliahPrasyarat == null ? ""
							: "Kode = " + matakuliahPrasyarat.getMatakuliahPrasyarat().getKode() + ", nama = "
									+ matakuliahPrasyarat.getMatakuliahPrasyarat().getNama();

					if (matakuliahPrasyarat.getMatakuliahPrasyarat2() != null) {
						mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat2().getKode() + "-"
								+ matakuliahPrasyarat.getMatakuliahPrasyarat2().getNama();
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat3() != null) {
						mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat3().getKode() + "-"
								+ matakuliahPrasyarat.getMatakuliahPrasyarat3().getNama();
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat4() != null) {
						mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat4().getKode() + "-"
								+ matakuliahPrasyarat.getMatakuliahPrasyarat4().getNama();
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat5() != null) {
						mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat5().getKode() + "-"
								+ matakuliahPrasyarat.getMatakuliahPrasyarat5().getNama();
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat6() != null) {
						mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat6().getKode() + "-"
								+ matakuliahPrasyarat.getMatakuliahPrasyarat6().getNama();
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat7() != null) {
						mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat7().getKode() + "-"
								+ matakuliahPrasyarat.getMatakuliahPrasyarat7().getNama();
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat8() != null) {
						mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat8().getKode() + "-"
								+ matakuliahPrasyarat.getMatakuliahPrasyarat8().getNama();
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat9() != null) {
						mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat9().getKode() + "-"
								+ matakuliahPrasyarat.getMatakuliahPrasyarat9().getNama();
					}
					if (matakuliahPrasyarat.getMatakuliahPrasyarat10() != null) {
						mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat10().getKode() + "-"
								+ matakuliahPrasyarat.getMatakuliahPrasyarat10().getNama();
					}

					syarat += (mk + ", minimal nilai lulus = "
							+ Common.numberFormat.get().format(matakuliahPrasyarat.getMinimalNilaiLulus())) + ".\n";
				}
			}
			if (syarat.isEmpty()) {
				return true;
			}

			MyMessageboxConfig.showFormat(
					"Mohon maaf, Bapak/Ibu. Mahasiswa dengan NIM {V1} atas nama {V2} belum diperkenankan mengambil matakuliah {V3} - {V4}, karena belum mengambil atau belum lulus salah satu matakuliah prasyarat. Rincian:\n\n{V5}\n\nLangkah yang dapat dilakukan: (1) periksa kembali matakuliah prasyarat yang disyaratkan; (2) pastikan mahasiswa telah mengambil dan lulus seluruh prasyarat; (3) ulangi proses setelah prasyarat terpenuhi.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, mahasiswa.getNim(),
					mahasiswa.getNama(), matakuliah.getKode(), matakuliah.getNama(), syarat);
			return false;

		}
		matakuliahPrasyarats = null;
		return true;
	}



	public static void createNilai(FormatNilai formatNilai, Detailperkuliahan detailperkuliahan, Double jumlah) {
		Tbmuser tbmuser = Common.getCurrentUser();
		detailperkuliahan.populateDetailNilai(formatNilai, null, jumlah, false, tbmuser);
	}



	public static boolean checkApakahMemenuhiSyaratBeasiswa(MahasiswaDaftarBeasiswa mahasiswaDaftarBeasiswa) {
		Boolean memenuhiSyarat = true;
		try {

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswaDaftarBeasiswa.getMahasiswa());

			if (mahasiswaDaftarBeasiswa.getBeasiswa().getBatasanIP() > 0.1) {
				Double IPK = krsMahasiswa.getIpk();
				memenuhiSyarat = (IPK >= mahasiswaDaftarBeasiswa.getBeasiswa().getBatasanIP() && memenuhiSyarat);
			}

			if (mahasiswaDaftarBeasiswa.getBeasiswa().getBatasanSkkp() > 0.1) {
				Double angkaKredit = Common.hitungAngkaKredit(mahasiswaDaftarBeasiswa.getMahasiswa());
				memenuhiSyarat = (angkaKredit >= mahasiswaDaftarBeasiswa.getBeasiswa().getBatasanSkkp()
						&& memenuhiSyarat);
			}

			if (mahasiswaDaftarBeasiswa.getBeasiswa().getBatasanSks() > 0.1) {
				Integer sks = krsMahasiswa.getSksk();
				memenuhiSyarat = (sks >= mahasiswaDaftarBeasiswa.getBeasiswa().getBatasanSks().intValue()
						&& memenuhiSyarat);
			}

			if (mahasiswaDaftarBeasiswa.getBeasiswa().getPenghasilanOrangTua() > 0L) {
				Session session = HibernateUtil.currentSession();

				PendapatanOrangTua pendapatanOrtu = (PendapatanOrangTua) session.createCriteria(BiodataMahasiswa.class)
						.setProjection(Projections.property("pendapatanOrtu"))
						.add(Restrictions.eq("mahasiswa", mahasiswaDaftarBeasiswa.getMahasiswa())).uniqueResult();

				Long mulai = pendapatanOrtu == null ? 0L : pendapatanOrtu.getMulaiDari().longValue();
				Long sampai = pendapatanOrtu == null ? 0L : pendapatanOrtu.getSampai().longValue();

				memenuhiSyarat = (sampai >= mahasiswaDaftarBeasiswa.getBeasiswa().getPenghasilanOrangTua()
						&& mulai <= mahasiswaDaftarBeasiswa.getBeasiswa().getPenghasilanOrangTua()) && memenuhiSyarat;
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return memenuhiSyarat;
	}



	public static boolean checkApakahMemenuhiSyaratOrganisasiKemahasiswaan(Mahasiswa mahasiswa,
			OrganisasiIntraKampus organisasiIntraKampus) {
		Boolean memenuhiSyarat = true;
		try {

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);

			if (organisasiIntraKampus.getMinimalIpk() > 0.1) {
				Double IPK = krsMahasiswa.getIpk();
				memenuhiSyarat = (IPK >= organisasiIntraKampus.getMinimalIpk() && memenuhiSyarat);
			}

			if (organisasiIntraKampus.getMinimalSkkm() > 0.1) {
				Double angkaKredit = Common.hitungAngkaKredit(mahasiswa);
				memenuhiSyarat = (angkaKredit >= organisasiIntraKampus.getMinimalSkkm() && memenuhiSyarat);
			}

			if (organisasiIntraKampus.getMinimalSks() > 0.1) {
				Integer sks = krsMahasiswa.getSksk();
				memenuhiSyarat = (sks >= organisasiIntraKampus.getMinimalSks().intValue() && memenuhiSyarat);
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return memenuhiSyarat;
	}



	public static KrsMahasiswa ambilDataKrsMahasiswa(Session session, Mahasiswa mahasiswa, Integer semester,
			Integer tahapan) {
		Criterion criterionSemester = tahapan == null || tahapan.equals(0) ? Restrictions.eq("semester", semester)
				: Restrictions.sqlRestriction("true");

		Criterion criterionTahapan = tahapan == null || tahapan.equals(0) ? Restrictions.sqlRestriction("true")
				: Restrictions.eq("tahapan", tahapan);

		KrsMahasiswa krsMahasiswa = (KrsMahasiswa) session.createCriteria(KrsMahasiswa.class)
				.add(Restrictions.eq("mahasiswa", mahasiswa)).add(criterionSemester).add(criterionTahapan)
				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

		if (krsMahasiswa == null) {
			krsMahasiswa = new KrsMahasiswa();
			krsMahasiswa.setMahasiswa(mahasiswa);
			// krsMahasiswa.setMaksSks(0);
			krsMahasiswa.setSemester(semester);
			krsMahasiswa.setTahapan(tahapan);
			krsMahasiswa.setSksYangDiambil(0);
			// krsMahasiswa.setMinip(0.0);
			// krsMahasiswa.setIplast(0.0);
			Common.refreshSaveOrUpdate(session, krsMahasiswa);
		}

		return krsMahasiswa;
	}



	public static KrsMahasiswa singkronkanKrsMahasiswaRefresh(Mahasiswa mahasiswa) {
		Integer semester = mahasiswa.currentSemester();
		Integer tahapan = mahasiswa.currentTahapan(semester);
		return singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, null, true);
	}



	public static KrsMahasiswa singkronkanKrsMahasiswa(Mahasiswa mahasiswa) {
		Integer semester = mahasiswa.getSemesterLulus() != null
				&& mahasiswa.getSemesterLulus() >= mahasiswa.currentSemester() ? mahasiswa.getSemesterLulus()
						: mahasiswa.currentSemester();
		Integer tahapan = mahasiswa.currentTahapan(semester);
		return singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, null, false);
	}

	/**
	 * Mengambil KRS tersimpan untuk kebutuhan tampilan tanpa menghitung ulang dan
	 * tanpa menulis ke database. Jalur ini mencegah render halaman memegang lock
	 * baris KRS/kegiatan dan bersaing dengan proses simpan pengguna.
	 */
	public static KrsMahasiswa ambilKrsMahasiswaTanpaSinkronisasi(Mahasiswa mahasiswa) {
		if (mahasiswa == null) {
			return new KrsMahasiswa();
		}
		Integer semesterSaatIni = mahasiswa.currentSemester();
		Integer semester = mahasiswa.getSemesterLulus() != null && semesterSaatIni != null
				&& mahasiswa.getSemesterLulus().intValue() >= semesterSaatIni.intValue()
						? mahasiswa.getSemesterLulus() : semesterSaatIni;
		Integer tahapan = mahasiswa.currentTahapan(semester);
		return ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, semester, tahapan, null);
	}

	public static KrsMahasiswa ambilKrsMahasiswaTanpaSinkronisasi(Mahasiswa mahasiswa, Integer semester,
			Integer tahapan, Integer semesterPendek) {
		return KrsDanSkripsiHelper.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, semester, tahapan,
				semesterPendek);
	}



	public static KrsMahasiswa singkronkanKrsMahasiswa(Mahasiswa mahasiswa, Integer semester, Integer tahapan,
			Integer semesterPendek) {
		return singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek, false);
	}



	public static void checkDosenPa(KrsMahasiswa krsMahasiswa) {
		Dosen dosen = krsMahasiswa.getDosenPa();
		if (dosen == null && krsMahasiswa.getMahasiswa() != null && krsMahasiswa.getMahasiswa().getDosen() != null) {
			dosen = new Dosen(krsMahasiswa.getMahasiswa().getDosen());
			if (dosen != null) {
				krsMahasiswa.setDosenPa(dosen);
				Common.refreshUpdate(krsMahasiswa);
			}

		}
	}



	public static void checkKelas(KrsMahasiswa krsMahasiswa) {
		String kelas = krsMahasiswa.getKelas();
		if (kelas == null && krsMahasiswa.getMahasiswa() != null) {
			kelas = krsMahasiswa.getMahasiswa().getKelas();
			if (kelas != null) {
				krsMahasiswa.setKelas(kelas);
				Common.refreshUpdate(krsMahasiswa);

			}

		}
	}



	public static KrsMahasiswa singkronkanKrsMahasiswa(Mahasiswa mahasiswa, Integer semester, Integer tahapan,
			Integer semesterPendek, boolean keDatabase) {
		return singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek, keDatabase, false);
	}



	public static KrsMahasiswa singkronkanKrsMahasiswa(Mahasiswa mahasiswa, Integer semester, Integer tahapan,
			Integer semesterPendek, boolean keDatabase, boolean dosenPaDefault) {
		return singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek, keDatabase, dosenPaDefault, false);
	}



	public static KrsMahasiswa singkronkanKrsMahasiswa(Mahasiswa mahasiswa, Integer semester, Integer tahapan,
			Integer semesterPendek, boolean keDatabase, boolean dosenPaDefault, boolean jikaTidakAdaKembali) {
		return KrsDanSkripsiHelper.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek, keDatabase,
				dosenPaDefault, jikaTidakAdaKembali);
	}



	public static Detailperkuliahan checkApakahSudahMengambilKrsSeminarSkripsiDan(Mahasiswa mahasiswa,
			String label_seminar_skripsi) {
		return KrsDanSkripsiHelper.checkApakahSudahMengambilKrsSeminarSkripsiDan(mahasiswa, label_seminar_skripsi);
	}



	public static Detailperkuliahan checkApakahSudahMengambilKrsSeminarSkripsi(Mahasiswa mahasiswa, Integer semester,
			String label_seminar_skripsi) {
		return KrsDanSkripsiHelper.checkApakahSudahMengambilKrsSeminarSkripsi(mahasiswa, semester,
				label_seminar_skripsi);
	}



	public static List<FormatNilai> getFormatNilais(List<Perkuliahan> perkuliahans) {
		if (perkuliahans == null) {
			return new ArrayList<FormatNilai>();
		}

		List<FormatNilai> formatNilaisAll = new ArrayList<FormatNilai>();
		Session session = HibernateUtil.currentNativeSession();
		for (Perkuliahan perkuliahan : perkuliahans) {
			List<FormatNilai> formatNilais = perkuliahan.ambilFormatNilai(session, false);
			formatNilaisAll.addAll(formatNilais);
		}
		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
		return formatNilaisAll;
	}



	public static List<FormatNilai> getFormatNilais(Perkuliahan perkuliahan) {
		if (perkuliahan == null) {
			return new ArrayList<FormatNilai>();
		}
		Session session = HibernateUtil.currentNativeSession();
		List<FormatNilai> formatNilais = perkuliahan.ambilFormatNilai(session, false);
		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
		return formatNilais;
	}



	public static List<FormatNilai> getFormatNilais(Perkuliahan perkuliahan, boolean refresh) {
		if (perkuliahan == null || perkuliahan.getId() == null) {
			return new ArrayList<FormatNilai>();
		}
		Session session = HibernateUtil.currentNativeSession();
		List<FormatNilai> formatNilais = perkuliahan.ambilFormatNilai(session, refresh);
		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
		return formatNilais;
	}



	public static List<FormatNilai> getFormatNilais(Session session, Perkuliahan perkuliahan) {
		if (perkuliahan == null) {
			return new ArrayList<FormatNilai>();
		}
		List<FormatNilai> formatNilais = perkuliahan.ambilFormatNilai(session, false);
		return formatNilais;
	}



	public static Double hitungTagihanMahasiswaSebagaiSyaratKrs(Session session, Mahasiswa mahasiswa,
			Integer semester) {
		return CommonHelperClass.hitungTagihanMahasiswaSebagaiSyaratKrs(session, mahasiswa, semester);
	}



	public static Boolean checkPembayaranSebelumKRSSudahMemenuhi(Mahasiswa mahasiswa, Integer semester, Integer tahap,
			boolean persetujuan) {
		return CommonHelperClass.checkPembayaranSebelumKRSSudahMemenuhi(mahasiswa, semester, tahap, persetujuan);
	}



	public static void updateNilaiKonversi(Detailperkuliahan detailperkuliahan, Double nilai, Session session) {

		Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() != null
				? detailperkuliahan.getPerkuliahan().getMatakuliah()
				: detailperkuliahan.getMatakuliahKonversi();

		detailperkuliahan.setTotalNilai(nilai);
		NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(nilai, detailperkuliahan.getMahasiswa().getTahunangkatan(),
				detailperkuliahan.getMahasiswa().getJurusan(),
				detailperkuliahan.getMahasiswa().getJurusan().getFakultas(), detailperkuliahan.getTahunAkademik(),
				detailperkuliahan.getPerkuliahan() == null ? null : detailperkuliahan.getPerkuliahan().getGanjilGenap(),
				matakuliah == null ? "" : matakuliah.getKode(),
				matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
		detailperkuliahan.setTotalIP(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
		detailperkuliahan.setNilaiHuruf(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
		detailperkuliahan.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());

		Double totalSementara = nilai;
		nilaiHuruf = Common.getNilaiHuruf(totalSementara, detailperkuliahan.getMahasiswa().getTahunangkatan(),
				detailperkuliahan.getMahasiswa().getJurusan(),
				detailperkuliahan.getMahasiswa().getJurusan().getFakultas(), detailperkuliahan.getTahunAkademik(),
				detailperkuliahan.getPerkuliahan() == null ? null : detailperkuliahan.getPerkuliahan().getGanjilGenap(),
				matakuliah == null ? "" : matakuliah.getKode(),
				matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

		detailperkuliahan.setTotalNilaiSementara(totalSementara);
		detailperkuliahan.setNilaiHurufSementara(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
		detailperkuliahan.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

		Common.refreshUpdate(session, (detailperkuliahan));
	}



	public static void singkronisasiStatusMahasiswa(Label label, Mahasiswa mahasiswa, String tahunAkademikParam,
			String jenisSemester, boolean nonAktifkan) {
		HistoryStatusMahasiswaUtil.singkronisasiStatusMahasiswa(label, mahasiswa, tahunAkademikParam, jenisSemester,
				nonAktifkan);
	}



    public static void singkronisasiKRSMahasiswa(Label label) {
        Session session = null;
        List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();
        try {
            session = HibernateUtil.currentNativeSession();
            @SuppressWarnings("unchecked")
            List<Mahasiswa> data = session.createCriteria(Mahasiswa.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE))).list();
            if (data != null) {
                mahasiswas.addAll(data);
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        } finally {
            Common.closeNativeSessionQuietly(session);
        }

        int i = 0;
        int size = mahasiswas.size() == 0 ? 1 : mahasiswas.size();
        for (Mahasiswa mahasiswa : mahasiswas) {
            try {
                Common.singkronkanKrsMahasiswa(mahasiswa);
                if (label != null) {
                    label.setValue("(" + (Common.numberFormat.get().format(i * 100.0 / size))
                            + " %) sinkronisasi status mahasiswa " + mahasiswa + " ..");
                }
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
            }
            i++;
        }
        if (label != null) {
            label.setValue("");
        }
    }


    public static PenugasanDosenMengajar getPenugasanDosenMengajar(Long idJurusan, String program, String tahun,
            String jenisSemester, Integer sks, Dosen dosen) {
        if (idJurusan == null || dosen == null) {
            return null;
        }
        Session session = null;
        Transaction tx = null;
        boolean mulaiTransaksi = false;
        PenugasanDosenMengajar penugasanDosenMengajar = null;
        try {
            session = HibernateUtil.currentNativeSession();
            penugasanDosenMengajar = (PenugasanDosenMengajar) session.createCriteria(PenugasanDosenMengajar.class)
                    .add(Restrictions.eq("jurusan.id", idJurusan)).add(Restrictions.eq("program", program))
                    .add(Restrictions.eq("tahunAkademik", tahun)).add(Restrictions.eq("semester", jenisSemester))
                    .add(Restrictions.eq("dosen", dosen)).setMaxResults(1).uniqueResult();

            if (penugasanDosenMengajar == null) {
                penugasanDosenMengajar = new PenugasanDosenMengajar();
                penugasanDosenMengajar.setJurusan(new Jurusan(idJurusan));
                penugasanDosenMengajar.setProgram(program);
                penugasanDosenMengajar.setSemester(jenisSemester);
                penugasanDosenMengajar.setTahunAkademik(tahun);
                penugasanDosenMengajar.setNama(tahun + "-" + jenisSemester);
                penugasanDosenMengajar.setDosen(dosen);
                penugasanDosenMengajar.setSks(sks);
                tx = session.getTransaction();
                if (tx == null || !tx.isActive()) {
                    tx = session.beginTransaction();
                    mulaiTransaksi = true;
                }
                session.save(penugasanDosenMengajar);
                if (mulaiTransaksi && tx != null && tx.isActive()) {
                    tx.commit();
                }
            } else if (penugasanDosenMengajar.getSks() == null || !penugasanDosenMengajar.getSks().equals(sks)) {
                penugasanDosenMengajar.setSks(sks);
                tx = session.getTransaction();
                if (tx == null || !tx.isActive()) {
                    tx = session.beginTransaction();
                    mulaiTransaksi = true;
                }
                session.update(penugasanDosenMengajar);
                if (mulaiTransaksi && tx != null && tx.isActive()) {
                    tx.commit();
                }
            }
        } catch (Exception e) {
            try {
                if (mulaiTransaksi && tx != null && tx.isActive()) {
                    tx.rollback();
                }
            } catch (Exception rollbackError) {
                Common.tampilErrorJikaAdmin(rollbackError);
            }
            Common.tampilErrorJikaAdmin(e);
        } finally {
            Common.closeNativeSessionQuietly(session);
        }
        return penugasanDosenMengajar;
    }

    public static PenugasanGuruMengajar getPenugasanGuruMengajar(Long idSekolah, String program, String tahun,
            String jenisSemester, Guru guru) {
        if (guru == null || guru.getId() == null || idSekolah == null) {
            return null;
        }
        Session session = null;
        Transaction tx = null;
        boolean mulaiTransaksi = false;
        PenugasanGuruMengajar penugasanGuruMengajar = null;
        try {
            session = HibernateUtil.currentNativeSession();
            penugasanGuruMengajar = (PenugasanGuruMengajar) session.createCriteria(PenugasanGuruMengajar.class)
                    .add(Restrictions.eq("sekolah.id", idSekolah))
                    .add(program == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("program", program))
                    .add(Restrictions.eq("tahunAkademik", tahun)).add(Restrictions.eq("semester", jenisSemester))
                    .add(Restrictions.eq("guru", guru)).setMaxResults(1).uniqueResult();

            if (penugasanGuruMengajar == null) {
                penugasanGuruMengajar = new PenugasanGuruMengajar();
                penugasanGuruMengajar.setSekolah(new Sekolah(idSekolah));
                penugasanGuruMengajar.setProgram(program);
                penugasanGuruMengajar.setSemester(jenisSemester);
                penugasanGuruMengajar.setTahunAkademik(tahun);
                penugasanGuruMengajar.setNama(tahun + "-" + jenisSemester);
                penugasanGuruMengajar.setGuru(guru);
                tx = session.getTransaction();
                if (tx == null || !tx.isActive()) {
                    tx = session.beginTransaction();
                    mulaiTransaksi = true;
                }
                session.save(penugasanGuruMengajar);
                if (mulaiTransaksi && tx != null && tx.isActive()) {
                    tx.commit();
                }
            }
        } catch (Exception e) {
            try {
                if (mulaiTransaksi && tx != null && tx.isActive()) {
                    tx.rollback();
                }
            } catch (Exception rollbackError) {
                Common.tampilErrorJikaAdmin(rollbackError);
            }
            Common.tampilErrorJikaAdmin(e);
        } finally {
            Common.closeNativeSessionQuietly(session);
        }
        return penugasanGuruMengajar;
    }

}
