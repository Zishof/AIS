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



/**
 * Helper terfokus untuk common current session. Tipe ini membungkus satu variasi kecil dari alur
 * yang lebih umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan
 * implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Common}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Logger log}, {@code String
 * COOKIE_PMB_BIODATA}, {@code String COOKIE_PMB_USERID}; pembacaan/pencarian ({@code tampilCrudError()}, {@code
 * getSemesterString()}, {@code getRequestHost()}, {@code getRequestHost()}, {@code
 * getRequestHostWithProtocol()}, {@code getRequestHostWithProtocol()}); validasi/perhitungan ({@code
 * checkProgram()}, {@code checkProgramString()}, {@code checkProgramString()}); operasi domain lain ({@code
 * safeTrim()}, {@code isBlank()}, {@code ensureDirectory()}, {@code isSecure()}, {@code isNowSemensterGanjil()},
 * {@code isNowSemensterGanjil()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> sesuai operasi yang dipanggil, utilitas dapat mengubah komponen UI, membaca/menulis
 * persistence atau berkas, dan memanggil layanan lain. Gunakan method kanonik di kelas ini melalui konteks
 * request/transaksi yang tepat, bukan menyalin implementasinya.</p>
 *
 * @see Common
 */
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class CommonCurrentSessionHelper extends Common {


	private static final Logger log = Logger.getLogger(CommonCurrentSessionHelper.class);
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
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonCurrentSessionHelper.java:461");
		}
	}




	public static boolean isSecure(HttpServletRequest request) {
		boolean wajibHttps = false;
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi("wajib_https", Konfigurasi.TIDAK_AKTIF);
			wajibHttps = konfigurasi != null && Konfigurasi.AKTIF.equals(konfigurasi.getNilai());
		} catch (Exception e) {
			log.warn("Gagal membaca konfigurasi wajib_https", e);
		}
		return (request != null && request.isSecure()) || wajibHttps;
	}



	public static String getSemesterString() {
		return CommonHelperClass.getSemesterString();
	}



	public static String getRequestHost() {
		HttpServletRequest request = null;
		if (ExecutionsCtrl.getCurrent() != null) {
			request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		}
		if (request == null) {
			request = RequestContext.get();
		}
		return getRequestHost(request);
	}



	public static String getRequestHost(HttpServletRequest request) {
		String req = request.getRequestURL().toString();
		req = req.replaceAll(Common.isSecure(request) ? "https://" : "http://", "");
		req = req.split(":")[0];
		req = req.split("/")[0];
		return req;
	}



	public static String getRequestHostWithProtocol() {
		return getRequestHostWithProtocol(null);
	}



	public static String getRequestHostWithProtocol(HttpServletRequest req) {

		try {
			HttpServletRequest request = req;
			if (request == null) {
				org.zkoss.zk.ui.Execution _exec0 = ExecutionsCtrl.getCurrent();
				if (_exec0 != null) {
					request = (HttpServletRequest) _exec0.getNativeRequest();
				}
			}
			if (request != null) {
				CURRENT_URL = (Common.isSecure(request) ? "https://" : "http://") + request.getServerName()
						+ (request.getServerPort() == 80 || request.getServerPort() == 443 ? ""
								: ":" + request.getServerPort())
						+ request.getContextPath();
				// Common.getRequestLocalHostWithProtocol();
				return CURRENT_URL;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonCurrentSessionHelper.java:529");

		}

		try {

			if (CURRENT_URL_TEMP == null) {
				HttpServletRequest request = req;
				if (request == null) {
					org.zkoss.zk.ui.Execution _exec1 = ExecutionsCtrl.getCurrent();
					if (_exec1 != null) {
						request = (HttpServletRequest) _exec1.getNativeRequest();
					}
				}
				// request bisa null di sini (background thread / tanpa konteks HTTP, mis. AppStartupListener
				// atau webhook payment gateway) - jangan dereference, biarkan CURRENT_URL apa adanya lalu
				// jatuh ke konfigurasi default di bawah.
				if (request != null) {
					CURRENT_URL = (Common.isSecure(request) ? "https://" : "http://") + request.getServerName()
							+ (request.getServerPort() == 80 || request.getServerPort() == 443 ? ""
									: ":" + request.getServerPort())
							+ request.getContextPath();
				}

				Konfigurasi konfigCurrentUrl = Common.getKonfigurasi("CURRENT_URL", CURRENT_URL);
				CURRENT_URL_TEMP = (konfigCurrentUrl != null && konfigCurrentUrl.getNilai() != null)
						? konfigCurrentUrl.getNilai().trim() : CURRENT_URL;
				CURRENT_URL = CURRENT_URL_TEMP;
			} else {
				CURRENT_URL = CURRENT_URL_TEMP;
			}

			if (CURRENT_URL == null || CURRENT_URL.trim().isEmpty()) {
				Konfigurasi konfigCurrentUrl = Common.getKonfigurasi("CURRENT_URL", CURRENT_URL);
				CURRENT_URL = konfigCurrentUrl != null ? konfigCurrentUrl.getNilai() : CURRENT_URL;
			}

			return CURRENT_URL != null ? CURRENT_URL : "";
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(fallback-guard) src/ais/common/CommonCurrentSessionHelper.java:554");
			try {
				if (CURRENT_URL == null || CURRENT_URL.trim().isEmpty()) {
					Konfigurasi konfigCurrentUrl = Common.getKonfigurasi("CURRENT_URL", CURRENT_URL);
					CURRENT_URL = konfigCurrentUrl != null ? konfigCurrentUrl.getNilai() : CURRENT_URL;
				}
			} catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(fallback-guard2) src/ais/common/CommonCurrentSessionHelper.java:558");
			}
			return CURRENT_URL != null ? CURRENT_URL : "";
		}
	}



	public static String getRequestHostWithProtocolSimple() {
		return getRequestHostWithProtocolSimple(null);
	}



	public static String getRequestHostWithProtocolSimple(HttpServletRequest req) {

		try {
			HttpServletRequest request = req;
			if (request == null && ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}
			if (request != null) {
				CURRENT_URL_SIMPLE = (Common.isSecure(request) ? "https://" : "http://") + request.getServerName()
						+ (request.getServerPort() == 80 || request.getServerPort() == 443 ? ""
								: ":" + request.getServerPort());
				return CURRENT_URL_SIMPLE;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonCurrentSessionHelper.java:581");

		}

		// Tidak ada HttpServletRequest yang bisa dipakai (background thread seperti AppStartupListener,
		// atau konteks tanpa request/session) - kembalikan fallback yang sudah ada, jangan throw.
		return CURRENT_URL_SIMPLE != null ? CURRENT_URL_SIMPLE : "";
	}



	public static Boolean isNowSemensterGanjil() {
		return isNowSemensterGanjil(ais.ui.util.WaktuUtil.getDate());
	}



	public static Boolean isNowSemensterGanjil(Date tanggal) {
		Tbmuser tbmuser = Common.getCurrentUser();
		RencanaTahunAkademik rencanaTahunAkademik = RencanaTahunAkademikAction.getCurrentRencanaTahunAkademik(tbmuser,
				tanggal);
		if (rencanaTahunAkademik != null) {
			return rencanaTahunAkademik.getSemester().equals(Perkuliahan.GANJIL);
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(tanggal);

		Integer bulan = calendar.get(Calendar.MONTH);
		return bulan >= 5;
	}



	public static Boolean isNowSemensterGanjil(Tbmuser tbmuser, Date tanggal) {
		RencanaTahunAkademik rencanaTahunAkademik = RencanaTahunAkademikAction.getCurrentRencanaTahunAkademik(tbmuser,
				tanggal);
		if (rencanaTahunAkademik != null) {
			return rencanaTahunAkademik.getSemester().equals(Perkuliahan.GANJIL);
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(tanggal);

		Integer bulan = calendar.get(Calendar.MONTH);
		return bulan >= 5;
	}



	public static Boolean isNowSemensterGanjil(Sekolah sekolah, Date tanggal) {
		RencanaTahunAkademik rencanaTahunAkademik = RencanaTahunAkademikAction.getCurrentRencanaTahunAkademik(null,
				null, null, sekolah, null, null, null, tanggal, null, null);
		if (rencanaTahunAkademik != null) {
			return rencanaTahunAkademik.getSemester().equals(Perkuliahan.GANJIL);
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(tanggal);

		Integer bulan = calendar.get(Calendar.MONTH);
		return bulan >= 5;
	}



	public static Tbmuser getCurrentFromSpringUser() {
		return SecurityFilter.getCurrentFromSpringUser();
	}



	public static Tbmuser getCurrentFromUsername(String userName) {
		return SecurityFilter.getCurrentFromUsername(userName);
	}



	public static Session getManualSession() {
		return HibernateUtil.currentSession();
	}



	public static String getCurrentSessionId() {
		String sessionId = String.valueOf(Common.randLong());
		try {
			if (ExecutionsCtrl.getCurrent() != null && ExecutionsCtrl.getCurrent().getNativeRequest() != null) {
				HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
				HttpSession session = request.getSession(false);
				if (session != null && session.getId() != null) {
					sessionId = session.getId();
				}
			}
		} catch (Exception e) {
			log.debug("Tidak dapat mengambil session id aktif", e);
		}
		return sessionId;
	}



	public static Tbmuser getCurrentUser(HttpServletRequest request) {
		if (request == null) {
			return null;
		}

		Tbmuser users = null;
		try {
			HttpSession session = request.getSession(false);
			if (session != null) {
				users = (Tbmuser) session.getAttribute("mytbmuser");
				if (users == null) {
					users = (Tbmuser) session.getAttribute("usersTemp");
				}
			}

			if (users == null) {
				String param = request.getParameter("user");
				if (!isBlank(param)) {
					return getCurrentFromUsername(param);
				}
			}

			return GeneralValueObject.check(users);
		} catch (Exception e) {
			try {
				HttpSession session = request.getSession(false);
				if (session != null) {
					users = (Tbmuser) session.getAttribute("usersTemp");
					return GeneralValueObject.check(users);
				}
			} catch (Exception ee) {
				log.debug("Tidak dapat mengambil user dari session cadangan", ee);
			}
			log.debug("Tidak dapat mengambil current user dari request", e);
			return null;
		}
	}



	public static SatuanKerja getSatuanKerja() {
		SatuanKerja satuanKerja = null;
		try {

			Sekolah sekolah = SekolahUtil.getSekolah();
			if (sekolah != null && sekolah.getSatuanKerja() != null) {
				satuanKerja = sekolah.getSatuanKerja();
			}

			if (satuanKerja == null) {
				PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
				if (perguruanTinggi != null && perguruanTinggi.getSatuanKerja() != null) {
					satuanKerja = perguruanTinggi.getSatuanKerja();
				}
			}

			Tbmuser tbmuser = Common.getCurrentUser();
			SatuanKerja s = tbmuser == null ? null : tbmuser.ambilSatuanKerja();
			if (s != null) {
				satuanKerja = s;
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonCurrentSessionHelper.java:743");
			// TODO: handle exception
		}

		return satuanKerja;
	}



	public static Tbmuser getCurrentUser() {
		Tbmuser users = null;

		try {
			HttpServletRequest httpServletRequest = null;

			// 1. Ambil request dari ZK ExecutionsCtrl dengan aman
			// Perbaikan: Cek null pada getCurrent() dan gunakan instanceof untuk mencegah
			// ClassCastException
			try {
				if (ExecutionsCtrl.getCurrent() != null) {
					Object nativeRequest = ExecutionsCtrl.getCurrent().getNativeRequest();
					if (nativeRequest instanceof HttpServletRequest) {
						httpServletRequest = (HttpServletRequest) nativeRequest;
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonCurrentSessionHelper.java:768");
				// Abaikan, lanjut ke fallback RequestContext
			}

			// 2. Fallback via RequestContext
			if (httpServletRequest == null) {
				try {
					httpServletRequest = RequestContext.get();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonCurrentSessionHelper.java:776");
					// Abaikan
				}
			}

			// 3. Ambil user dari HttpSession
			// Perbaikan: Cek httpServletRequest != null untuk mencegah NullPointerException
			if (httpServletRequest != null) {
				try {
					// Tetap menggunakan getSession(true) sesuai dengan versi aslinya
					users = (Tbmuser) httpServletRequest.getSession(true).getAttribute("mytbmuser");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonCurrentSessionHelper.java:787");
					// Abaikan
				}
			}

			// 4. Fallback ke ZK Sessions
			// Perbaikan: Cek Sessions.getCurrent() != null dengan lebih rapi
			if (users == null) {
				try {
					if (Sessions.getCurrent() != null) {
						users = (Tbmuser) Sessions.getCurrent().getAttribute("usersTemp");
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonCurrentSessionHelper.java:799");
					// Abaikan
				}
			}

			// 5. Fallback via parameter "user" dari URL
			// Perbaikan: Digabung menjadi satu blok pengecekan untuk efisiensi
			if (users == null) {
				try {
					if (ExecutionsCtrl.getCurrent() != null) {
						String param = ExecutionsCtrl.getCurrent().getParameter("user");

						// Jika dari Executions kosong, coba ambil dari native request
						if (param == null || param.trim().isEmpty()) {
							Object nativeReq = ExecutionsCtrl.getCurrent().getNativeRequest();
							if (nativeReq instanceof HttpServletRequest) {
								param = ((HttpServletRequest) nativeReq).getParameter("user");
							}
						}

						// Jika param ditemukan, langsung return
						if (param != null && !param.trim().isEmpty()) {
							return getCurrentFromUsername(param);
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonCurrentSessionHelper.java:824");
					// Abaikan
				}
			}

			// 6. Validasi akhir dan return
			users = GeneralValueObject.check(users);
			return users;

		} catch (Exception e) {
			// Ultimate Fallback jika terjadi exception tak terduga (sama seperti aslinya)
			try {
				Tbmuser fallbackUser = null;
				if (Sessions.getCurrent() != null) {
					fallbackUser = (Tbmuser) Sessions.getCurrent().getAttribute("usersTemp");
				}
				return GeneralValueObject.check(fallbackUser);
			} catch (Exception ee) {
				return null;
			}
		} finally {
			// --- BLOK FINALLY ---
			// Catatan Penting: Di dalam konteks ini, jangan melakukan invalidate()
			// pada HttpSession atau Sessions ZK, karena akan menyebabkan user otomatis
			// ter-logout.
			// Blok ini disiapkan jika ke depannya ada object seperti InputStream,
			// koneksi DB (misal di dalam getCurrentFromUsername), atau ThreadLocal
			// yang secara spesifik harus dilepas (release) untuk mencegah memory leak.
		}
	}



	/**
	 * Apakah pengguna saat ini boleh mengakses dasbor/tombol "Neo Feeder". Kini berbasis <b>flag per-ROLE</b>
	 * {@code Tbmrole.getBolehAksesFeeder()} (menggantikan gerbang lama berbasis
	 * {@code Common.getKonfigurasi("admin_yg_boleh_kirim_ke_feeder", ...)}). Tetap dinonaktifkan pada konteks
	 * Sekolah (Feeder=fitur perguruan tinggi). Default flag (bila admin belum mengatur) mengikuti
	 * {@code Tbmrole}: aktif untuk ADMINISTRATOR, AKADEMIK, &amp; role bernama mengandung "akademik"/"admin".
	 */
	public static boolean getApakahAdminBolehAksesFeeder() {
		Sekolah sekolah = SekolahUtil.getSekolah();
		if (sekolah != null && sekolah.getId() != null) {
			return false;
		}
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser == null) {
			return false;
		}
		Tbmrole role = tbmuser.hakAkses();
		return role != null && Boolean.TRUE.equals(role.getBolehAksesFeeder());
	}

	/**
	 * Apakah pengguna saat ini boleh mengakses dasbor/tombol "SISTER". Berbasis flag per-ROLE
	 * {@code Tbmrole.getBolehAksesSister()}; dinonaktifkan pada konteks Sekolah (SISTER=fitur perguruan tinggi).
	 */
	public static boolean getApakahAdminBolehAksesSister() {
		Sekolah sekolah = SekolahUtil.getSekolah();
		if (sekolah != null && sekolah.getId() != null) {
			return false;
		}
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser == null) {
			return false;
		}
		Tbmrole role = tbmuser.hakAkses();
		return role != null && Boolean.TRUE.equals(role.getBolehAksesSister());
	}



	public static boolean getApakahAdminBolehUpload() {
		Sekolah sekolah = SekolahUtil.getSekolah();
		if (sekolah != null && sekolah.getId() != null) {
			return false;
		}
		String adminLain = Common.getKonfigurasi("admin_yg_boleh_upload", "").getNilai();
		return getApakahAdmin() || getApakahAdmin(adminLain);
	}



	public static boolean getApakahAdminBolehLihatSemuaPegawai() {
		String adminLain = Common.getKonfigurasi("admin_yg_boleh_lihat_semua_data_pegawai", "").getNilai();
		return getApakahAdmin() || getApakahAdmin(adminLain);
	}



	public static boolean getApakahAdminBolehLihatSemuaCuti() {
		String adminLain = Common.getKonfigurasi("admin_yg_boleh_lihat_semua_data_cuti_pegawai", "").getNilai();
		return getApakahAdmin() || getApakahAdmin(adminLain);
	}



	public static boolean getApakahAdminBolehKunci() {
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser == null || tbmuser.getUserId() == null) {
			return false;
		}
		String adminLain = Common.getKonfigurasi("admin_yg_boleh_kunci_nilai", "").getNilai();
		if (adminLain.trim().isEmpty()) {
			return true;
		}
		// KE-8/KE-9: hakAkses() bisa null (mis. user calon mahasiswa tanpa role) -> NPE saat
		// getRoleId(). Guard: tanpa role -> bukan admin yang boleh kunci.
		Tbmrole role = tbmuser.hakAkses();
		if (role == null || role.getRoleId() == null) {
			return false;
		}
		String[] kodes = adminLain.split(",");
		for (String c : kodes) {
			if (!c.trim().isEmpty() && c.trim().equalsIgnoreCase(role.getRoleId())) {
				return true;
			}
		}
		return false;
	}



	public static boolean getApakahAdmin(String kodeAdmin) {
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser == null || tbmuser.getUserId() == null) {
			return false;
		}
		Tbmrole tbmrole = tbmuser.hakAkses();
		if (kodeAdmin == null || kodeAdmin.trim().isEmpty()) {
			return tbmuser != null && tbmrole != null && tbmrole.getRoleId().equals(Tbmrole.ADMINISTRATOR);
		} else if (tbmuser != null && tbmrole != null && tbmrole.getRoleId() != null) {
			String[] kodes = kodeAdmin.split(",");
			for (String c : kodes) {
				if (!c.trim().isEmpty() && c.trim().equalsIgnoreCase(tbmrole.getRoleId())) {
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}



	public static boolean getApakahAdmin() {
		return getApakahAdmin(null);
	}



	public static boolean getApakahAdminLain() {

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser == null || tbmuser.getUserId() == null) {
			return false;
		}
		return tbmuser != null && tbmuser.hakAkses() != null
				&& tbmuser.hakAkses().getRoleId().equals(Tbmrole.ADMINISTRATOR);
	}



	public static boolean getApakahAdminLain(Tbmuser tbmuser) {

		if (tbmuser == null || tbmuser.getUserId() == null) {
			return false;
		}
		return tbmuser != null && tbmuser.hakAkses() != null
				&& tbmuser.hakAkses().getRoleId().equals(Tbmrole.ADMINISTRATOR);
	}



	public static void checkProgram(Combobox combobox) {
		Tbmuser users = (Tbmuser) Sessions.getCurrent().getAttribute("usersTemp");
		if (users != null) {
			Program program = users.ambilProgram();
			if (program != null) {
				selectComboItem(combobox, program);
				combobox.setDisabled(true);
			}
		}
	}



	public static void checkProgramString(Combobox combobox) {
		checkProgramString(combobox, false);
	}



	public static void checkProgramString(Combobox combobox, boolean termasukMhs) {
		Tbmuser users = Common.getCurrentUser();
		if (users != null) {
			Program program = users.ambilProgram();
			if (program != null) {
				selectComboItem(combobox, program.getNama());
				combobox.setDisabled(true);
			}

			if (termasukMhs && users.getMahasiswa() != null) {
				selectComboItem(combobox, users.getMahasiswa().getProgram());
				combobox.setDisabled(true);
			}
		}

		MyComboitemConfig comboitem = new MyComboitemConfig();
		comboitem.setLabel("=Program=");
		comboitem.setValue(null);
		combobox.appendChild(comboitem);

		if (combobox.getSelectedItem() == null) {
			combobox.setSelectedItem(comboitem);
		}
		combobox.setReadonly(true);
	}



	public static String getGeneratedBarCode(int digit) {
		return CommonGenerateHelper.getGeneratedBarCode(digit);
	}



	public static String getGeneratedBarCode() {
		return CommonGenerateHelper.getGeneratedBarCode();
	}



	public static String getGeneratedAngkaDigit(int digit) {
		return CommonGenerateHelper.getGeneratedAngkaDigit(digit);
	}



	public static Combobox generateJam(Combobox combobox) {
		if (combobox == null) {
			combobox = new Combobox();
		}

		return combobox;
	}



	public static String generateSingkatan(String asal) {
		if (asal == null || asal.trim().equals("")) {
			return "";
		}
		String[] ss = asal.split(" ");
		String hasil = "";
		for (String s : ss) {
			if (s.trim().equals(""))
				continue;
			hasil += s.trim().substring(0, 1).toUpperCase();
		}
		return hasil;
	}



	public static String getCurrentTahunAkademik() {
		return getCurrentTahunAkademik(ais.ui.util.WaktuUtil.getDate());
	}



	public static String getCurrentTahunAkademik(Date tanggal) {
		Tbmuser tbmuser = Common.getCurrentUser();
		return getCurrentTahunAkademik(tbmuser, tanggal);
	}



	public static String getCurrentTahunAkademik(Tbmuser tbmuser, Date tanggal) {

		RencanaTahunAkademik rencanaTahunAkademik = RencanaTahunAkademikAction.getCurrentRencanaTahunAkademik(tbmuser,
				tanggal);
		if (rencanaTahunAkademik != null) {
			return rencanaTahunAkademik.getNama();
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(tanggal);

		String current = "";
		if (calendar.get(Calendar.MONTH) > 5) {
			current = calendar.get(Calendar.YEAR) + "/" + (calendar.get(Calendar.YEAR) + 1);
		} else {
			current = (calendar.get(Calendar.YEAR) - 1) + "/" + (calendar.get(Calendar.YEAR));
		}
		return current;
	}



	public static String getCurrentTahunAkademik(Sekolah sekolah, Date tanggal) {

		RencanaTahunAkademik rencanaTahunAkademik = RencanaTahunAkademikAction.getCurrentRencanaTahunAkademik(null,
				null, null, sekolah, null, null, null, tanggal, null, null);
		if (rencanaTahunAkademik != null) {
			return rencanaTahunAkademik.getNama();
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(tanggal);

		String current = "";
		if (calendar.get(Calendar.MONTH) > 5) {
			current = calendar.get(Calendar.YEAR) + "/" + (calendar.get(Calendar.YEAR) + 1);
		} else {
			current = (calendar.get(Calendar.YEAR) - 1) + "/" + (calendar.get(Calendar.YEAR));
		}
		return current;
	}



	public static Combobox generateTahunAjaran(Combobox combobox) {
		if (combobox == null) {
			combobox = new Combobox();
		}
		String current = Common.getCurrentTahunAkademik();

		for (String value : tahunAngkatans) {
			Comboitem comboitem = new Comboitem();
			comboitem.setLabel(value);
			comboitem.setValue(value);
			combobox.appendChild(comboitem);
		}
		selectComboItem(combobox, current);
		combobox.setReadonly(true);
		return combobox;
	}



	public static Combobox generateTahunAjaranJuniJuli(Combobox combobox) {
		if (combobox == null) {
			combobox = new Combobox();
		}
		String current = Common.getCurrentTahunAkademik();

		for (String value : tahunAngkatans) {
			Comboitem comboitem = new Comboitem();
			comboitem.setLabel("Juni " + value.split("/")[0] + " sd Juli " + value.split("/")[1]);
			comboitem.setValue(value);
			combobox.appendChild(comboitem);
		}
		selectComboItem(combobox, current);
		combobox.setReadonly(true);
		return combobox;
	}



	public static Combobox generateTahunAjaranDanSemua(Combobox combobox) {
		if (combobox == null) {
			combobox = new Combobox();
		}

		for (String value : tahunAngkatans) {
			Comboitem comboitem = new Comboitem();
			comboitem.setLabel(value);
			comboitem.setValue(value);
			combobox.appendChild(comboitem);
		}
		combobox.setReadonly(true);

		if (Common.bolehKonfigurasi("bisa_pilih_semua_tahun_akademik")) {
			MyComboitemConfig comboitem = new MyComboitemConfig();
			comboitem.setLabel("=TA=");
			comboitem.setValue(null);
			combobox.appendChild(comboitem);
			selectComboItem(combobox, null);
		} else {
			selectComboItem(combobox, Common.getCurrentTahunAkademik());
		}
		return combobox;
	}



	public static Combobox generateTahunDanSemua(Combobox combobox) {
		if (combobox == null) {
			combobox = new Combobox();
		}

		for (Integer value : tahunAngkatansData) {
			Comboitem comboitem = new Comboitem();
			comboitem.setLabel(value.toString());
			comboitem.setValue(value);
			combobox.appendChild(comboitem);
		}
		combobox.setReadonly(true);

		if (Common.bolehKonfigurasi("bisa_pilih_semua_tahun")) {
			MyComboitemConfig comboitem = new MyComboitemConfig();
			comboitem.setLabel("Tahun");
			comboitem.setValue(null);
			combobox.appendChild(comboitem);
			selectComboItem(combobox, null);
		} else {
			selectComboItem(combobox, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
		}
		return combobox;
	}



	public static Combobox generateTahunAngkatan(Combobox combobox) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		return generateTahunAngkatan(combobox, calendar.get(Calendar.YEAR));
	}



	public static Combobox generateTahunAngkatan(Combobox combobox, Integer year) {
		if (combobox == null) {
			combobox = new Combobox();
		}
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		for (int i = calendar.get(Calendar.YEAR) - 20; i < calendar.get(Calendar.YEAR) + 20; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			combobox.appendChild(comboitem);
			if (year.equals(i)) {
				combobox.setSelectedItem(comboitem);
			}
		}
		return combobox;
	}



	public static Combobox generateTahunKelulusan(Combobox combobox, Integer year) {
		if (combobox == null) {
			combobox = new Combobox();
		}
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		for (int i = calendar.get(Calendar.YEAR) - 30; i <= calendar.get(Calendar.YEAR); i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i + "");
			combobox.appendChild(comboitem);

		}
		return combobox;
	}



	public static Boolean isMenuExist(Collection<Menu> menus, Menu menu) {

		for (Menu my : menus) {
			if (menu.getUrl() != null && my.getUrl() != null && menu.getUrl().trim().equals(my.getUrl().trim())) {
				return true;
			}
		}

		return false;
	}



	public static Perpustakaan getCurrentPerpustakaan() {
		return getCurrentPerpustakaan(false);
	}



	public static Perpustakaan getCurrentPerpustakaan(Boolean refresh) {

		try {
			org.zkoss.zk.ui.Session httpSession = Sessions.getCurrent();
			if (httpSession == null) {
				return null;
			}

			Perpustakaan perpustakaan = null;
			Pustakawan pustakawan = null;
			if (httpSession == null || httpSession.getAttribute("CurrentPustakawan") == null || refresh) {
				Tbmuser users = null;
				try {
					users = (Tbmuser) httpSession.getAttribute("mytbmuser");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonCurrentSessionHelper.java:1315");

				}

				if (users != null) {
					pustakawan = (Pustakawan) HibernateUtil.currentSession().createCriteria(Pustakawan.class)
							.add(Restrictions.eq("tbmuser", users)).addOrder(Order.desc("id")).setMaxResults(1)
							.uniqueResult();

					if (httpSession != null && pustakawan != null) {
						httpSession.setAttribute("CurrentPustakawan", pustakawan);
					} else if (httpSession != null && pustakawan == null) {

						int count = ((Number) HibernateUtil.currentSession().createCriteria(Perpustakaan.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.setProjection(Projections.rowCount()).uniqueResult()).intValue();

						// System.out.println("======================= Init
						// pustakawan " + pustakawan + ", count = " + count
						// + " ===============================");

						if (count == 1) {
							perpustakaan = (Perpustakaan) HibernateUtil.currentSession()
									.createCriteria(Perpustakaan.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.setMaxResults(1).uniqueResult();
							httpSession.setAttribute("CurrentPustakawan", perpustakaan);
							return perpustakaan;
						} else {
							httpSession.setAttribute("CurrentPustakawan", new Pustakawan());
						}
					}
				}
			} else {
				pustakawan = (Pustakawan) httpSession.getAttribute("CurrentPustakawan");
			}

			perpustakaan = pustakawan == null || pustakawan.getId() == null ? null : pustakawan.getPerpustakaan();

			return perpustakaan;
		} catch (Exception e) {
			return null;
		}
	}



	public static Toko getCurrentToko() {
		return getCurrentToko(false);
	}



	public static Toko getCurrentToko(Boolean refresh) {

		try {
			org.zkoss.zk.ui.Session httpSession = Sessions.getCurrent();
			if (httpSession == null) {
				return null;
			}

			Toko toko = null;
			Pedagang pedagang;
			if (httpSession == null || httpSession.getAttribute("CurrentPedagang") == null || refresh) {
				pedagang = (Pedagang) HibernateUtil.currentSession().createCriteria(Pedagang.class)
						.add(Restrictions.eq("tbmuser", getCurrentUser())).addOrder(Order.desc("id")).setMaxResults(1)
						.uniqueResult();

				if (httpSession != null && pedagang != null) {
					httpSession.setAttribute("CurrentPedagang", pedagang);
				} else if (httpSession != null && pedagang == null) {

					int count = ((Number) HibernateUtil.currentSession().createCriteria(Toko.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();

					// System.out.println("======================= Init pedagang
					// " + pedagang + ", count = " + count
					// + " ===============================");

					if (count == 1) {
						toko = (Toko) HibernateUtil.currentSession().createCriteria(Toko.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.setMaxResults(1).uniqueResult();
						httpSession.setAttribute("CurrentPedagang", toko);
						return toko;
					} else {
						httpSession.setAttribute("CurrentPedagang", new Pedagang());
					}
				}
			} else {
				pedagang = (Pedagang) httpSession.getAttribute("CurrentPedagang");
			}

			toko = pedagang == null || pedagang.getId() == null ? null : pedagang.getToko();

			return toko;
		} catch (Exception e) {
			return null;
		}
	}



	public static Koperasi getCurrentKoperasi() {
		return getCurrentKoperasi(false);
	}



	public static Koperasi getCurrentKoperasi(Boolean refresh) {

		try {
			org.zkoss.zk.ui.Session httpSession = Sessions.getCurrent();
			if (httpSession == null) {
				return null;
			}

			PengurusKoperasi pegawaiKoperasi = null;
			if (httpSession == null || httpSession.getAttribute("CurrentPegawaiKoperasi") == null || refresh) {
				pegawaiKoperasi = (PengurusKoperasi) HibernateUtil.currentSession()
						.createCriteria(PengurusKoperasi.class).add(Restrictions.eq("tbmuser", getCurrentUser()))
						.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

				// System.out.println("======================= Init
				// pegawaiKoperasi " + pegawaiKoperasi
				// + " ===============================");

				if (httpSession != null && pegawaiKoperasi != null) {
					httpSession.setAttribute("CurrentPegawaiKoperasi", pegawaiKoperasi);
				} else if (httpSession != null && pegawaiKoperasi == null) {
					httpSession.setAttribute("CurrentPegawaiKoperasi", new PengurusKoperasi());
				}
			} else {
				pegawaiKoperasi = (PengurusKoperasi) httpSession.getAttribute("CurrentPegawaiKoperasi");
			}

			return pegawaiKoperasi == null || pegawaiKoperasi.getId() == null ? null : pegawaiKoperasi.getKoperasi();
		} catch (Exception e) {
			return null;
		}
	}



	public static Pejabat getCurrentPejabat(JenisJabatan jenisJabatan) {

		try {
			Tbmuser tbmuser = getCurrentUser();
			org.zkoss.zk.ui.Session httpSession = Sessions.getCurrent();
			if (httpSession == null || tbmuser == null) {
				return null;
			}

			Pejabat pejabat = (Pejabat) ConstantValues.simpleObject(HibernateUtil.currentSession()
					.createCriteria(Pejabat.class).add(Restrictions.eq("jenisJabatan", jenisJabatan))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

					.add(Restrictions.or(
							Restrictions.or(
									Restrictions.ilike("jenisPengguna", "," + tbmuser.hakAkses().getRoleId() + ",",
											MatchMode.ANYWHERE),
									Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
											MatchMode.ANYWHERE)),
							Restrictions.and(
									Restrictions.or(Restrictions.isNotNull("pegawai"),
											Restrictions.or(Restrictions.isNotNull("guru"),
													Restrictions.isNotNull("dosen"))),
									Restrictions.or(Restrictions.eq("pegawai", tbmuser.getPegawai()),
											Restrictions.or(Restrictions.eq("dosen", tbmuser.getDosen()),
													Restrictions.eq("guru", tbmuser.getGuru()))))))

					.setMaxResults(1).addOrder(Order.desc("id")), Pejabat.class);

			System.out.println("======================= Init pejabat " + pejabat + "===============================");

			return pejabat;
		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
			return null;
		}
	}



	public static List<Pejabat> getCurrentPejabats(JenisJabatan jenisJabatan) {

		try {
			Tbmuser tbmuser = getCurrentUser();
			org.zkoss.zk.ui.Session httpSession = Sessions.getCurrent();
			if (httpSession == null || tbmuser == null) {
				return null;
			}

			List<Pejabat> pejabats = ConstantValues
					.simpleList(HibernateUtil.currentSession().createCriteria(Pejabat.class)

							.add(Restrictions.eq("jenisJabatan", jenisJabatan))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

							.add(Restrictions.or(
									Restrictions.or(
											Restrictions.ilike("jenisPengguna",
													"," + tbmuser.hakAkses().getRoleId() + ",", MatchMode.ANYWHERE),
											Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
													MatchMode.ANYWHERE)),
									Restrictions.and(
											Restrictions.or(Restrictions.isNotNull("pegawai"),
													Restrictions.or(Restrictions.isNotNull("guru"),
															Restrictions.isNotNull("dosen"))),
											Restrictions.or(Restrictions.eq("pegawai", tbmuser.getPegawai()),
													Restrictions.or(Restrictions.eq("dosen", tbmuser.getDosen()),
															Restrictions.eq("guru", tbmuser.getGuru()))))))

							, Pejabat.class);

			System.out.println("======================= Init pejabat " + pejabats + "===============================");

			return pejabats;
		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
			return null;
		}
	}



	public static boolean isMobile() {
		try {
			org.zkoss.zk.ui.Execution _execMob = ExecutionsCtrl.getCurrent();
			if (_execMob != null) {
				HttpServletRequest request = (HttpServletRequest) _execMob.getNativeRequest();

				if (request == null) {
					request = RequestContext.get();
				}
				return isMobile(request);
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}



	public static boolean isMobile(HttpServletRequest request) {
		if (request == null) return false;

		try {
			String param = request.getParameter("is_mobile");
			// // System.out.println("is_mobile => " + param);
			if (param != null && !param.trim().isEmpty() && param.trim().equalsIgnoreCase("true")) {
				request.getSession(true).setAttribute("is_mobile", true);
				return true;
			} else if (param != null && !param.trim().isEmpty() && param.trim().equalsIgnoreCase("false")) {
				request.getSession(true).setAttribute("is_mobile", false);
				return false;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonCurrentSessionHelper.java:1576");

		}

		try {
			// FIX NPE rutin: getAttribute("is_mobile") balik null bila belum pernah
			// di-set di sesi ini (kondisi NORMAL, bukan langka) -> .equals(true/false)
			// pada null melempar NPE di HAMPIR SETIAP pemanggilan isMobile(). Ambil
			// dulu ke variabel & cek null agar exception jadi genuinely langka.
			Object isMobileAttr = request.getSession(true).getAttribute("is_mobile");
			if (isMobileAttr != null) {
				if (isMobileAttr.equals(true)) {
					return true;
				}
				if (isMobileAttr.equals(false)) {
					return false;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonCurrentSessionHelper.java:1587");

		}

		try {
			String userAgent = request.getHeader("User-Agent");
			// // System.out.println("userAgent => " + userAgent);
			return userAgent.toLowerCase().indexOf("mobile") != -1;
		} catch (Exception e) {
			return false;
		}
	}



	public static boolean isAsliMobile() {
		try {
			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}

			if (request == null) {
				request = RequestContext.get();
			}
			return isAsliMobile(request);
		} catch (Exception e) {
			return false;
		}
	}



	public static boolean isAsliMobile(HttpServletRequest request) {
		try {
			String userAgent = request.getHeader("User-Agent");
			// // System.out.println("userAgent => " + userAgent);
			return userAgent.toLowerCase().indexOf("mobile") != -1;
		} catch (Exception e) {
			return false;
		}
	}



	public static Lokasi getCurrentLokasi() {
		HttpServletRequest request = null;
		if (ExecutionsCtrl.getCurrent() != null) {
			request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		}

		if (request == null) {
			request = RequestContext.get();
		}
		String remoteIp = request.getRemoteAddr();
		System.out.println("remoteIp = " + remoteIp);

		Lokasi lokasi = (Lokasi) ConstantValues
				.simpleObject(
						HibernateUtil
								.currentSession().createCriteria(Lokasi.class).add(
										Restrictions.or(
												Restrictions.or(
														Restrictions.or(
																Restrictions.or(Restrictions.eq("ip", remoteIp),
																		Restrictions.eq("ip1", remoteIp)),
																Restrictions.eq("ip2", remoteIp)),
														Restrictions.eq("ip3", remoteIp)),
												Restrictions.eq("ip4", remoteIp)))
								.setMaxResults(1),
						Lokasi.class);
		Sessions.getCurrent().setAttribute("lokasi", lokasi);
		return lokasi;
	}

}
