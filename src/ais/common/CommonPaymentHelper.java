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
public class CommonPaymentHelper extends Common {


	private static final Logger log = Logger.getLogger(CommonPaymentHelper.class);
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
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonPaymentHelper.java:461");
		}
	}




	public static boolean checkBaypassStatusPembayaranMahasiswa(Integer semester, Integer tahap, Mahasiswa mahasiswa,
			JenisKegiatan jenisKegiatan) {

		if (jenisKegiatan != null && semester != null) {
			if (jenisKegiatan.getMinSmt() > semester || jenisKegiatan.getMaxSmt() < semester) {
				return true;
			}
		}

		List<JenisKegiatan> jenisKegiatans = new ArrayList<JenisKegiatan>();
		if (jenisKegiatan != null) {
			jenisKegiatans.add(jenisKegiatan);
		}
		return checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, jenisKegiatans);
	}



	public static boolean checkBaypassStatusPembayaranMahasiswa(Integer semester, Integer tahap, Mahasiswa mahasiswa,
			Collection<JenisKegiatan> jenisKegiatans) {

		Session session = HibernateUtil.currentNativeSession();

		Criterion criterionSemester = tahap == null || tahap.equals(0) ? Restrictions.eq("semester", semester)
				: Restrictions.sqlRestriction("true");

		Criterion criterionTahapan = tahap == null || tahap.equals(0) ? Restrictions.sqlRestriction("true")
				: Restrictions.eq("tahap", tahap);

		Date now = WaktuUtil.getDate();

		int count = 0;
		try {
			count = ((Number) session.createCriteria(BaypassPembayaranMahasiswa.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa))
					// Bandingkan HANYA bagian TANGGAL (bungkus kolom dgn DATE()) supaya pengecualian AKTIF PADA
					// hari mulai (berlaku_mulai) apa pun komponen JAM tersimpan. Tanpa DATE() pada kolom, bila
					// berlaku_mulai kebetulan tersimpan sbg timestamp berjam >00:00 (mis. impor lama), maka
					// "berlaku_mulai <= DATE(now)" bernilai FALSE di hari mulai → mahasiswa gagal cetak di hari
					// pertama pengecualian. DATE(kolom) menjamin inklusif di kedua ujung (mulai & sampai).
					.add(Restrictions.sqlRestriction("(berlaku_mulai is null or DATE(berlaku_mulai) <= DATE('"
							+ Common.databaseDateFormat.get().format(now)
							+ "'))  and  (berlaku_sampai is null or DATE(berlaku_sampai) >= DATE('"
							+ Common.databaseDateFormat.get().format(now) + "'))"))
					.add(jenisKegiatans == null || jenisKegiatans.isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.in("jenisKegiatan", jenisKegiatans),
									Restrictions.isNull("jenisKegiatan")))
					.add(criterionSemester).add(criterionTahapan).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();
		} catch (java.util.NoSuchElementException nse) {
			// JDBC protocol state corruption (ArrayDeque kosong) — asumsikan tidak ada bypass
			count = 0;
		} catch (IllegalStateException ise) {
			// "Received resultset tuples, but no field structure" — koneksi korup
			count = 0;
		}

		// System.out.println("checkBaypassStatusPembayaranMahasiswa ->
		// jenisKegiatan => " + jenisKegiatans + ", count => "
		// + count + ", mahasiswa => " + mahasiswa + ", semester => " +
		// semester);

		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
		return count != 0;

	}



	public static boolean checkStatusPembayaranMahasiswa(Integer semester, Integer tahap, Mahasiswa mahasiswa,
			boolean persetujuan, boolean sp) {
		return checkStatusPembayaranMahasiswa(semester, tahap, mahasiswa, true, persetujuan, sp);
	}



	public static boolean checkStatusPembayaranMahasiswa(Integer semester, Integer tahap, Mahasiswa mahasiswa,
			boolean check, boolean persetujuan, boolean sp) {

		Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
		Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
		Integer tahunAkademikMulai = Common.getTahunAkademik(semester, tahunAngkatanMhs, semesterMulai,
				mahasiswa.getSemesterMulai());

		String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
		if (Common.checkApakahMahasiswaBolehAmbilKrsLewatPengecualian(mahasiswa, tahunAkademik,
				semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL)) {
			return true;
		}

		boolean mahasiswabaruMengikutipersyaratanKrsSptMahasiswa = Common.bolehKonfigurasi("mahasiswa_baru_mengikuti_persyaratan_krs_spt_mahasiswa", Konfigurasi.TIDAK_AKTIF);

		if (semester == null || (!mahasiswabaruMengikutipersyaratanKrsSptMahasiswa && semester.intValue() == 1)
				|| semester.intValue() <= 0) {
			return true;
		}

		if (sp) {

			if (check) {
				if (!Common.bolehKonfigurasi("mahasiswa_harus_bayar_sebelum_isi_krs_sp")) {
					return true;
				}
			}

			String kodeItemBiaya = Common.getKonfigurasi("kode_item_biaya_mahasiswa_harus_bayar_sebelum_isi_krs_sp", "",
					semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(), mahasiswa.getProgram(),
					mahasiswa.getStatusAwalMahasiswa()).getNilai();
			if (kodeItemBiaya.trim().isEmpty()) {
				return true;
			}

			int kegiatan = mahasiswa.ambilJumlahCicilanPembayaran(kodeItemBiaya, semester);
			boolean hasil = kegiatan > 0;

			if (!hasil) {
				Collection<JenisKegiatan> jenisKegiatan = mahasiswa.ambilJenisKegiatans(semester, kodeItemBiaya);
				if (Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, jenisKegiatan)) {
					return true;
				}
			}

			return hasil;

		} else {

			if (check) {
				if (!Common
						.getKonfigurasi(persetujuan ? "mahasiswa_harus_bayar_sebelum_persetujuan_krs"
								: "mahasiswa_harus_bayar_sebelum_isi_krs", Konfigurasi.AKTIF)
						.getNilai().equals(Konfigurasi.AKTIF)) {
					return true;
				}
			}

			if (!persetujuan) {
				Session session = HibernateUtil.currentNativeSession();
				Double tagihanSyaratKrs = hitungTagihanMahasiswaSebagaiSyaratKrs(session, mahasiswa, semester);
				if (tagihanSyaratKrs < 0.01) {
					return true;
				}
				HibernateUtil.closeSession();

				String kodeItemBiaya = Common.getKonfigurasi("kode_item_biaya_mahasiswa_harus_bayar_sebelum_isi_krs",
						"", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(), mahasiswa.getProgram(),
						mahasiswa.getStatusAwalMahasiswa()).getNilai();
				// System.out.println("kodeItemBiaya -> " + kodeItemBiaya);
				if (!kodeItemBiaya.trim().isEmpty()) {

					List<String> kodes = new ArrayList<String>();
					for (String s : kodeItemBiaya.split(";")) {
						if (!s.trim().isEmpty()) {
							kodes.add(s.trim());
						}
					}

					// System.out.println("kodeItemBiaya kodes -> " +
					// kodes.size());

					if (!kodes.isEmpty()) {
						int kegiatan = mahasiswa.ambilJumlahCicilanPembayaran(kodeItemBiaya, semester);
						boolean hasil = kegiatan > 0;

						if (!hasil) {
							Collection<JenisKegiatan> jenisKegiatan = mahasiswa.ambilJenisKegiatans(semester,
									kodeItemBiaya);
							if (Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa,
									jenisKegiatan)) {
								return true;
							}
						}

						if (!hasil) {
							return false;
						}
					}

				}

			}

			if (CommonHelperClass.jenisKegiatansUntukKrs == null) {
				reloadJenisKegiatans();
			}

			List<Kegiatan> kegiatanDibayars = mahasiswa.ambilKegiatans(semester,
					CommonHelperClass.jenisKegiatansUntukKrs);

			Session session = HibernateUtil.currentNativeSession();
			if (!mahasiswabaruMengikutipersyaratanKrsSptMahasiswa && (semester != null
					&& (semester.equals(1) || semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 1)
							|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester()))
					&& kegiatanDibayars.isEmpty() && mahasiswa != null && mahasiswa.getNim() != null)) {

				if (Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa,
						ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU)) {
					return true;
				}

				BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues
						.simpleObject(session.createCriteria(BiodataCalonMahasiswa.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.sqlRestriction(
										"upper(trim(this_.nim)) = upper(trim('" + mahasiswa.getNim().trim() + "'))"))
								.setMaxResults(1), BiodataCalonMahasiswa.class);
				if (biodataCalonMahasiswa != null) {
					CommonHelperClass.jenisKegiatansUntukKrs.add(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
					kegiatanDibayars = biodataCalonMahasiswa.ambilKegiatans(semester,
							CommonHelperClass.jenisKegiatansUntukKrs);
				}

			} else {
				if (Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa,
						ConstantValues.PENDAFTARAN_MAHASISWA_LAMA)) {
					return true;
				}
			}

			boolean hasil = !kegiatanDibayars.isEmpty();

			HibernateUtil.closeSession();
			return hasil;
		}
	}



	public static boolean checkStatusPembayaranMahasiswaSebelumnya(Integer semester, Integer tahap,
			Mahasiswa mahasiswa) {
		return checkStatusPembayaranMahasiswaSebelumnya(semester, tahap, mahasiswa, false);
	}



	public static boolean checkStatusPembayaranMahasiswaSebelumnya(Integer semester, Integer tahap, Mahasiswa mahasiswa,
			boolean persetujuan) {
		return CommonHelperClass.checkStatusPembayaranMahasiswaSebelumnya(semester, tahap, mahasiswa, persetujuan);
	}



	public static boolean checkStatusPembayaranKegiatanMahasiswa(FormulirKegiatan formulirKegiatan,
			Mahasiswa mahasiswa) {
		return CommonHelperClass.checkStatusPembayaranKegiatanMahasiswa(formulirKegiatan, mahasiswa);
	}



	public static boolean checkStatusPembayaranMahasiswaSebelumnyaUntukPenilaian(Integer semester, Integer tahap,
			Mahasiswa mahasiswa, Double harusLunas, boolean termasukSmt1) {
		return CommonHelperClass.checkStatusPembayaranMahasiswaSebelumnyaUntukPenilaian(semester, tahap, mahasiswa,
				harusLunas, termasukSmt1);
	}



	public static boolean checkStatusPembayaranMahasiswaPengajuanSkripsi(
			FormatNilaiProposalSkripsi formatNilaiProposalSkripsi, Integer semester, Mahasiswa mahasiswa) {
		return CommonHelperClass.checkStatusPembayaranMahasiswaPengajuanSkripsi(formatNilaiProposalSkripsi, semester,
				mahasiswa);
	}



	public static boolean checkStatusPembayaranMahasiswaPengajuanSidang(FormatNilaiSkripsi formatNilaiSkripsi,
			Integer semester, Mahasiswa mahasiswa) {
		return CommonHelperClass.checkStatusPembayaranMahasiswaPengajuanSidang(formatNilaiSkripsi, semester, mahasiswa);
	}



	public static boolean checkStatusPembayaranMahasiswaPengajuanWisuda(Integer semester, Mahasiswa mahasiswa) {
		return CommonHelperClass.checkStatusPembayaranMahasiswaPengajuanWisuda(semester, mahasiswa);
	}



	public static void simpanCicilanDefaultTanpaSesseion(Kegiatan kegiatan, Double nominal, Date tanggalValidasi,
			String keterangan, JenisPembayaran jenisPembayaran, @SuppressWarnings("rawtypes") Collection detailBiayas) {
		Session session = HibernateUtil.currentNativeSession();

		try {
			session.getTransaction().begin();

			simpanCicilanTanpaMencicil(kegiatan, nominal, tanggalValidasi, keterangan, jenisPembayaran, detailBiayas,
					session);

			session.getTransaction().commit();
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
		}

		HibernateUtil.closeSession();
	}



	public static CicilanPembayaran simpanCicilanTanpaMencicil(Kegiatan kegiatan, Double nominal, Date tanggalValidasi,
			String keterangan, JenisPembayaran jenisPembayaran, @SuppressWarnings("rawtypes") Collection detailBiayas,
			Session session) {
		CicilanPembayaran cicilanPembayaran = null;
		try {
			PembayaranUtil.getInstance().getResetCicilanOld(session, kegiatan.getCalonMahasiswa(),
					kegiatan.getMahasiswa(), kegiatan.getSemster(), kegiatan.getJenisKegiatan(), kegiatan);

			if (detailBiayas == null || detailBiayas.isEmpty()) {
				cicilanPembayaran = new CicilanPembayaran(null);
				cicilanPembayaran.setKe(1);
				cicilanPembayaran.setKegiatan(kegiatan);
				cicilanPembayaran.setKeterangan(keterangan);
				cicilanPembayaran.setItemBiaya(null);
				cicilanPembayaran.setNilai(nominal);
				cicilanPembayaran.setTanggal(tanggalValidasi);
				cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
				Common.refreshSaveOrUpdate(session, cicilanPembayaran);

				if (cicilanPembayaran.getBuktiPembayaran() != null) {
					BuktiPembayaran buktiPembayaran = cicilanPembayaran.getBuktiPembayaran();
					buktiPembayaran.setCicilanPembayaran(cicilanPembayaran);
					Common.refreshUpdate(session, buktiPembayaran);
				}
			} else {
				int i = 1;
				for (Object o : detailBiayas) {
					DetailBiaya detailBiaya = null;
					PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;
					if (o instanceof DetailBiaya) {
						detailBiaya = (DetailBiaya) o;
					} else if (o instanceof PengaturanPembayaranBulanan) {
						pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
						detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();

					}

					cicilanPembayaran = new CicilanPembayaran(detailBiaya);

					cicilanPembayaran.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
					cicilanPembayaran.setItemBiaya(detailBiaya.getItemBiaya());

					if (pengaturanPembayaranBulanan != null) {
						if (kegiatan.getMahasiswa() != null) {
							cicilanPembayaran.setNilai(pengaturanPembayaranBulanan
									.ambilNominalModifikasi(kegiatan.getMahasiswa(), kegiatan.getSemster()));
						} else {
							cicilanPembayaran.setNilai(pengaturanPembayaranBulanan.getNominal());
						}
					} else {
						cicilanPembayaran.setNilai(detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
								: detailBiaya.getNilaiBiayaBaru());
					}

					cicilanPembayaran.setKe(i);
					cicilanPembayaran.setKegiatan(kegiatan);
					cicilanPembayaran.setKeterangan(keterangan);
					cicilanPembayaran.setTanggal(tanggalValidasi);
					cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
					Common.refreshSaveOrUpdate(session, cicilanPembayaran);

					if (cicilanPembayaran.getBuktiPembayaran() != null) {
						BuktiPembayaran buktiPembayaran = cicilanPembayaran.getBuktiPembayaran();
						buktiPembayaran.setCicilanPembayaran(cicilanPembayaran);
						Common.refreshUpdate(session, buktiPembayaran);
					}
					i++;
				}
			}
		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
		}

		return cicilanPembayaran;

	}



	public static void simpanCicilanDefaultTanpaSesseion(KegiatanTemporary kegiatanTemporary, Double nominal,
			Date tanggalValidasi, String keterangan, JenisPembayaran jenisPembayaran,
			@SuppressWarnings("rawtypes") Collection detailBiayas) {
		Session session = HibernateUtil.currentNativeSession();

		session.getTransaction().begin();

		simpanCicilanTanpaMencicil(kegiatanTemporary, nominal, tanggalValidasi, keterangan, jenisPembayaran,
				detailBiayas, session);

		session.getTransaction().commit();
		HibernateUtil.closeSession();
	}



	public static CicilanPembayaran simpanCicilanTanpaMencicil(KegiatanTemporary kegiatanTemporary, Double nominal,
			Date tanggalValidasi, String keterangan, JenisPembayaran jenisPembayaran,
			@SuppressWarnings("rawtypes") Collection detailBiayas, Session session) {

		session.createSQLQuery("delete from cicilan_pembayaran where kegiatan_temporary = " + kegiatanTemporary.getId())
				.executeUpdate();
		CicilanPembayaran cicilanPembayaran = null;
		if (detailBiayas == null || detailBiayas.isEmpty()) {
			cicilanPembayaran = new CicilanPembayaran(null);
			cicilanPembayaran.setKe(1);
			cicilanPembayaran.setKegiatanTemporary(kegiatanTemporary);
			cicilanPembayaran.setKeterangan(keterangan);
			cicilanPembayaran.setItemBiaya(null);
			cicilanPembayaran.setNilai(nominal);
			cicilanPembayaran.setTanggal(tanggalValidasi);
			cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
			Common.refreshSaveOrUpdate(session, cicilanPembayaran);

			if (cicilanPembayaran.getBuktiPembayaran() != null) {
				BuktiPembayaran buktiPembayaran = cicilanPembayaran.getBuktiPembayaran();
				buktiPembayaran.setCicilanPembayaran(cicilanPembayaran);
				Common.refreshUpdate(session, buktiPembayaran);
			}
		} else {
			int i = 1;
			for (Object o : detailBiayas) {
				DetailBiaya detailBiaya = null;
				PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;
				if (o instanceof DetailBiaya) {
					detailBiaya = (DetailBiaya) o;
				} else if (o instanceof PengaturanPembayaranBulanan) {
					pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
					detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();

				}
				cicilanPembayaran = new CicilanPembayaran(detailBiaya);

				if (pengaturanPembayaranBulanan != null) {
					if (kegiatanTemporary.getMahasiswa() != null) {
						cicilanPembayaran.setNilai(pengaturanPembayaranBulanan.ambilNominalModifikasi(
								kegiatanTemporary.getMahasiswa(), kegiatanTemporary.getSemster()));
					} else {
						cicilanPembayaran.setNilai(pengaturanPembayaranBulanan.getNominal());
					}
				} else {
					cicilanPembayaran.setNilai(detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
							: detailBiaya.getNilaiBiayaBaru());
				}

				cicilanPembayaran.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
				cicilanPembayaran.setItemBiaya(detailBiaya.getItemBiaya());

				cicilanPembayaran.setKe(i);
				cicilanPembayaran.setKegiatanTemporary(kegiatanTemporary);
				cicilanPembayaran.setKeterangan(keterangan);
				cicilanPembayaran.setTanggal(tanggalValidasi);
				cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
				Common.refreshSaveOrUpdate(session, cicilanPembayaran);

				if (cicilanPembayaran.getBuktiPembayaran() != null) {
					BuktiPembayaran buktiPembayaran = cicilanPembayaran.getBuktiPembayaran();
					buktiPembayaran.setCicilanPembayaran(cicilanPembayaran);
					Common.refreshUpdate(session, buktiPembayaran);
				}
				i++;
			}
		}
		return cicilanPembayaran;

	}



	public static List<CicilanPembayaran> ambilCicilanPembayarans(Session session, LogHostToHost logHostToHost,
			String kode, String nim, Date tanggal) {
		List<String> bulans = new ArrayList<String>();
		List<String> kodeItems = new ArrayList<String>();
		String item = logHostToHost.getItem();
		if (item != null && !item.trim().isEmpty()) {
			String[] ss = item.split("\\|");
			for (String sss : ss) {
				try {
					if (sss != null && !sss.trim().isEmpty()) {
						// System.out.println("sss ==> " + sss);
						String[] i = StringUtils.split(sss, '\\');
						bulans.add(i[2]);
						kodeItems.add(i[0]);
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}

		@SuppressWarnings("unchecked")
		List<CicilanPembayaran> cicilanPembayarans = session.createCriteria(CicilanPembayaran.class)
				.add(Restrictions.eq("kegiatan", logHostToHost.getKegiatan()))

				.createAlias("itemBiaya", "itemBiaya")

				.add(kodeItems.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.in("itemBiaya.kode", kodeItems))

				.createAlias("pengaturanPembayaranBulanan", "pengaturanPembayaranBulanan")
				.add(bulans.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.in("pengaturanPembayaranBulanan.namaBulan", bulans))
				.createAlias("kegiatan", "kegiatan").createAlias("kegiatan.mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("kegiatan.calonMahasiswa", "calonMahasiswa", Criteria.LEFT_JOIN)
				.add(Restrictions.or(
						Restrictions.or(Restrictions.eq("calonMahasiswa.noRegistrasi", kode),
								Restrictions.eq("calonMahasiswa.noUjian", kode)),
						Restrictions.eq("mahasiswa.nim", nim)))

				.add(Restrictions.sqlRestriction(
						"DATE(this_.tanggal) = DATE('" + Common.databaseDateFormat.get().format(tanggal) + "')"))
				.list();

		// System.out.println("nim = " + nim + ", kode = " + kode + ", bulans =
		// " + bulans + ", cicilanPembayarans = "
		// + cicilanPembayarans);

		return cicilanPembayarans;
	}



	public static List<CicilanPembayaranGagal> ambilCicilanPembayaranGagals(Session session,
			LogHostToHost logHostToHost, String kode, String nim, Date tanggal) {
		List<String> bulans = new ArrayList<String>();
		List<String> kodeItems = new ArrayList<String>();
		String item = logHostToHost.getItem();
		if (item != null && !item.trim().isEmpty()) {
			String[] ss = item.split("\\|");
			for (String sss : ss) {
				try {
					if (sss != null && !sss.trim().isEmpty()) {
						// System.out.println("sss ==> " + sss);
						String[] i = StringUtils.split(sss, '\\');
						bulans.add(i[2]);
						kodeItems.add(i[0]);
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}

		@SuppressWarnings("unchecked")
		List<CicilanPembayaranGagal> cicilanPembayaranGagals = session.createCriteria(CicilanPembayaranGagal.class)
				.add(Restrictions.eq("kegiatan", logHostToHost.getKegiatan()))

				.createAlias("itemBiaya", "itemBiaya")

				.add(kodeItems.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.in("itemBiaya.kode", kodeItems))

				.createAlias("pengaturanPembayaranBulanan", "pengaturanPembayaranBulanan")
				.add(bulans.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.in("pengaturanPembayaranBulanan.namaBulan", bulans))
				.createAlias("kegiatan", "kegiatan").createAlias("kegiatan.mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("kegiatan.calonMahasiswa", "calonMahasiswa", Criteria.LEFT_JOIN)
				.add(Restrictions.or(
						Restrictions.or(Restrictions.eq("calonMahasiswa.noRegistrasi", kode),
								Restrictions.eq("calonMahasiswa.noUjian", kode)),
						Restrictions.eq("mahasiswa.nim", nim)))

				.add(Restrictions.sqlRestriction(
						"DATE(this_.tanggal) = DATE('" + Common.databaseDateFormat.get().format(tanggal) + "')"))
				.list();

		// System.out.println("nim = " + nim + ", kode = " + kode + ", bulans =
		// " + bulans + ", cicilanPembayaranGagals = "
		// + cicilanPembayaranGagals);
		return cicilanPembayaranGagals;
	}



	public static CicilanPembayaranGagal copyCicilanPembayaranKeGagal(CicilanPembayaran cicilanPembayaran) {
		CicilanPembayaranGagal cicilanPembayaranGagal = new CicilanPembayaranGagal(cicilanPembayaran.getDetailBiaya());
		cicilanPembayaranGagal.setItemBiaya(cicilanPembayaran.getItemBiaya());
		cicilanPembayaranGagal.setJenisPembayaran(cicilanPembayaran.getJenisPembayaran());
		cicilanPembayaranGagal.setKe(cicilanPembayaran.getKe());
		cicilanPembayaranGagal.setKegiatan(cicilanPembayaran.getKegiatan());
		cicilanPembayaranGagal.setKeterangan(cicilanPembayaran.getKeterangan());
		cicilanPembayaranGagal.setNilai(cicilanPembayaran.getNilai());
		cicilanPembayaranGagal.setPengaturanPembayaranBulanan(cicilanPembayaran.getPengaturanPembayaranBulanan());
		cicilanPembayaranGagal.setPostingHistory(cicilanPembayaran.getPostingHistory());
		cicilanPembayaranGagal.setRekonsiliasiHostToHost(cicilanPembayaran.getRekonsiliasiHostToHost());
		cicilanPembayaranGagal.setValidator(cicilanPembayaran.getValidator());
		cicilanPembayaranGagal.setTanggal(cicilanPembayaran.getTanggal());
		return cicilanPembayaranGagal;
	}



	public static CicilanPembayaran copyCicilanPembayaranKeSukses(CicilanPembayaranGagal cicilanPembayaranGagal) {
		CicilanPembayaran cicilanPembayaran = new CicilanPembayaran(cicilanPembayaranGagal.getDetailBiaya());
		cicilanPembayaran.setItemBiaya(cicilanPembayaranGagal.getItemBiaya());
		cicilanPembayaran.setJenisPembayaran(cicilanPembayaranGagal.getJenisPembayaran());
		cicilanPembayaran.setKe(cicilanPembayaranGagal.getKe());
		cicilanPembayaran.setKegiatan(cicilanPembayaranGagal.getKegiatan());
		cicilanPembayaran.setKeterangan(cicilanPembayaranGagal.getKeterangan());
		cicilanPembayaran.setNilai(cicilanPembayaranGagal.getNilai());
		cicilanPembayaran.setPengaturanPembayaranBulanan(cicilanPembayaranGagal.getPengaturanPembayaranBulanan());
		cicilanPembayaran.setPostingHistory(cicilanPembayaranGagal.getPostingHistory());
		cicilanPembayaran.setRekonsiliasiHostToHost(cicilanPembayaranGagal.getRekonsiliasiHostToHost());
		cicilanPembayaran.setTanggal(cicilanPembayaranGagal.getTanggal());
		cicilanPembayaran.setValidator(cicilanPembayaranGagal.getValidator());
		return cicilanPembayaran;
	}



	public static Hbox initCicilan(final Map<Long, LampiranLain> buktiPembayarans, final Rows rowsCicilan,
			final Row row, int i, CicilanPembayaran cicilanPembayaran, MyToolbarbuttonConfig buttonHapus) {
		BuktiPembayaran buktiPembayaran = (BuktiPembayaran) row.getAttribute("buktiPembayaran");

		if (cicilanPembayaran.getBuktiPembayaran() != null) {
			buktiPembayaran = cicilanPembayaran.getBuktiPembayaran();
		}

		row.setValign("top");
		row.setAttribute("buktiPembayaran", buktiPembayaran);

		row.setParent(rowsCicilan);
		Vbox vbox = new Vbox();
		row.appendChild(vbox);
		vbox.appendChild(new Label("Ke-" + (i + 1) + ""));
		row.setValign("top");
		row.setAttribute("cicilanPembayaran", cicilanPembayaran);
		row.setValign("top");
		row.setAttribute("buttonHapus", buttonHapus);

		final Long idLampiran = cicilanPembayaran.getIdLampiran() == null ? Common.refSementara()
				: cicilanPembayaran.getIdLampiran();
		row.setValign("top");
		row.setAttribute("idLampiran", idLampiran);
		Hbox hboxLampiran = new Hbox();
		if (buktiPembayaran == null) {
			hboxLampiran.setVisible(cicilanPembayaran.getId() != null);
			final CicilanPembayaran tempCicilanPembayaran = cicilanPembayaran;
			LampiranLain.createDownloadUploadFileLain(hboxLampiran, idLampiran, "cicilanPembayaran", "Bukti Pembayaran",
					false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lainMahasiswa = (LampiranLain) arg0.getData();
							buktiPembayarans.put(idLampiran, lainMahasiswa);

							tempCicilanPembayaran.setIdLampiran(lainMahasiswa.getId());
							row.setValign("top");
							row.setAttribute("cicilanPembayaran", tempCicilanPembayaran);
						}
					}, null, false, true, true, true, null);
		} else {
			Hbox hbox = new Hbox();
			hbox.setParent(hboxLampiran);
			LampiranLain.createDownloadUploadFileLain(hbox, buktiPembayaran.getId(), BuktiPembayaran.class.getName(),
					"Bukti Pembayaran", true, null, null, false, false, false, false);
			// LampiranLain.createDownloadUploadFileLain(hboxLampiran,
			// buktiPembayaran.getId(),
			// BuktiPembayaran.class.getName(), "Bukti Pembayaran", true, null,
			// null, false, true, true, false,
			// null);
		}
		hboxLampiran.setParent(vbox);
		row.setValign("top");
		row.setAttribute("hboxLampiran", hboxLampiran);
		return hboxLampiran;

	}



	public static List<CicilanPembayaran> filterCicilanPembayaran(List<CicilanPembayaran> cicilanPembayarans) {
		Map<String, CicilanPembayaran> mapCicilanPembayaran = new HashMap<String, CicilanPembayaran>();
		for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
					.getPengaturanPembayaranBulanan();
			if (pengaturanPembayaranBulanan != null) {
				if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNilaiBisaDiubah()) {
					mapCicilanPembayaran.put(
							cicilanPembayaran.getId() + "-" + pengaturanPembayaranBulanan.getBulan() + "-"
									+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId(),
							cicilanPembayaran);
				} else {
					mapCicilanPembayaran.put(
							pengaturanPembayaranBulanan.getBulan() + "-"
									+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId(),
							cicilanPembayaran);
				}
			} else {
				mapCicilanPembayaran.put(cicilanPembayaran.getItemBiaya().getId() + "-"
						+ Common.numberFormat.get().format(cicilanPembayaran.getNilai()) + "-"
						+ Common.dateFormat.get().format(cicilanPembayaran.getTanggal()), cicilanPembayaran);
			}
		}
		// System.out.println("cicilanPembayarans=>" +
		// mapCicilanPembayaran.keySet());
		return new ArrayList<CicilanPembayaran>(mapCicilanPembayaran.values());
	}

}
