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
import java.util.zip.ZipFile;
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
import org.zkoss.zul.Div;
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
import ais.ui.util.MyFormRow;
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
 * Helper terfokus untuk common file media. Tipe ini membungkus satu variasi kecil dari alur yang
 * lebih umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Common}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Logger log}, {@code String
 * COOKIE_PMB_BIODATA}, {@code String COOKIE_PMB_USERID}, {@code long MAKS_PIKSEL_GAMBAR}, {@code int
 * MAKS_BYTE_GAMBAR}; pembacaan/pencarian ({@code tampilCrudError()}, {@code uploadTugas()}, {@code
 * uploadTugas()}, {@code uploadTugas()}, {@code checkBackgroundUpload()}, {@code checkFaviconUpload()}); mutasi
 * data ({@code simpanKeDrive()}, {@code simpanKeDrive()}, {@code setJSONTemporary()}); operasi domain lain
 * ({@code safeTrim()}, {@code isBlank()}, {@code ensureDirectory()}, {@code copy()}, {@code
 * writeInputStreamToFile()}, {@code writeBlobToFile()}). Bagian lain dari kontrak tetap mengikuti kelas induk
 * atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> sesuai operasi yang dipanggil, utilitas dapat mengubah komponen UI, membaca/menulis
 * persistence atau berkas, dan memanggil layanan lain. Gunakan method kanonik di kelas ini melalui konteks
 * request/transaksi yang tepat, bukan menyalin implementasinya.</p>
 *
 * @see Common
 */
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class CommonFileMediaHelper extends Common {


	private static final Logger log = Logger.getLogger(CommonFileMediaHelper.class);
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
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonFileMediaHelper.java:463");
		}
	}




	public static void uploadTugas(final Tugas tugas, final Mahasiswa mahasiswa,
			final BiodataCalonMahasiswa biodataCalonMahasiswa, final EventListener eventListener) throws Exception {

		Pertemuan pa = null;
		if (tugas instanceof Pertemuan) {
			pa = (Pertemuan) tugas;
		} else if (tugas instanceof TugasPertemuan) {
			pa = ((TugasPertemuan) tugas).ambilPertemuan();
		}
		final Pertemuan p = pa;
		final MyWindow window = new MyWindow("Upload Tugas", "none", true);
		window.setWidth("600px");
		window.setHeight("240px");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		South southBatal = new South();
		ais.ui.util.ZkCompat.setFlex(southBatal, true);
		southBatal.setStyle("background: #ffffff; border-top: 1px solid #e2e8f0; padding: 8px; text-align: right;");
		southBatal.setParent(borderlayout);
		Toolbar toolbarBatal = new Toolbar();
		toolbarBatal.setStyle("background: transparent; border: none;");
		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		batal.setTooltiptext("Tutup tanpa upload");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		batal.setParent(toolbarBatal);
		toolbarBatal.setParent(southBatal);

		List<Long> indsMhsPerkuliahan = new ArrayList<Long>();
		for (GeneralValueObject mhs : AbsensiHelper.populateMahasiswaDariPertemuan(p)) {
			if (!tugas.getMhsYgTidakIkut().contains("," + mhs.getId() + ",")) {
				indsMhsPerkuliahan.add(mhs.getId());
			}
		}
		final AmbilDataMahasiswaBanbox ambilDataMahasiswaBanbox = new AmbilDataMahasiswaBanbox(indsMhsPerkuliahan);
		ambilDataMahasiswaBanbox.setValue(mahasiswa == null ? "" : mahasiswa.getNim() + " - " + mahasiswa.getNama());
		ambilDataMahasiswaBanbox.setAttribute("mahasiswa", mahasiswa);
		ambilDataMahasiswaBanbox.setAttribute("myValue", mahasiswa);
		ambilDataMahasiswaBanbox.setDisabled(mahasiswa != null);

		final AmbilDataCalonMahasiswaBanbox ambilDataCalonMahasiswaBanbox = new AmbilDataCalonMahasiswaBanbox();
		ambilDataCalonMahasiswaBanbox.setValue(biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getNama());
		ambilDataCalonMahasiswaBanbox.setAttribute("calonMahasiswa", biodataCalonMahasiswa);
		ambilDataCalonMahasiswaBanbox.setAttribute("myValue", biodataCalonMahasiswa);
		ambilDataCalonMahasiswaBanbox.setDisabled(biodataCalonMahasiswa != null);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(searchgrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setVisible(p.getJadwalUjianPMB() == null);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		row.appendChild(ambilDataMahasiswaBanbox);
		ambilDataMahasiswaBanbox.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(p.getJadwalUjianPMB() != null);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Calon Mahasiswa"));
		row.appendChild(ambilDataCalonMahasiswaBanbox);
		ambilDataCalonMahasiswaBanbox.setWidth("90%");

		MyToolbarbuttonConfig fileupload = new MyToolbarbuttonConfig("Upload Tugas / Ambil Tugas / Tambah Link Tugas",
				"/img/File-Upload-icon.png");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(fileupload);

		fileupload.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				final BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) ambilDataCalonMahasiswaBanbox
						.getAttribute("calonMahasiswa");
				final Mahasiswa mahasiswa = (Mahasiswa) ambilDataMahasiswaBanbox.getAttribute("mahasiswa");
				if (mahasiswa == null && biodataCalonMahasiswa == null) {
					PesanFormalHelper.tampilkanGagal("upload berkas tugas",
							"Data Mahasiswa/Calon Mahasiswa pemilik berkas belum dipilih, sehingga sistem tidak "
									+ "dapat mengaitkan berkas yang akan diunggah dengan pemiliknya.",
							new String[] { "Silakan pilih terlebih dahulu Mahasiswa atau Calon Mahasiswa yang "
									+ "bersangkutan sebelum mengunggah berkas." });
					return;
				}

				Calendar kemarin = ais.ui.util.WaktuUtil.getCalendar();
				if ((tugas.getSelesai() != null && tugas.getSelesai().before(kemarin.getTime()))
						|| (tugas.getMulai() != null && tugas.getMulai().after(kemarin.getTime()))) {
					MyMessageboxConfig.show(
							"Tugas telah terlambat atau belum mulai, sehingga Anda tidak bisa meng-upload tugas ini",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}
				if (tugas != null && mahasiswa != null && tugas.getSyaratMengumpulkanTugas() != null) {
					Detailperkuliahan detailperkuliahan = mahasiswa.ambilDetailperkuliahan(p.getPerkuliahan());
					if (detailperkuliahan == null) {
						mahasiswa.reInitDetailperkuliahan(HibernateUtil.currentSession());
						detailperkuliahan = mahasiswa.ambilDetailperkuliahan(p.getPerkuliahan());
					}
					// System.out.println(
					// "detailperkuliahan -> " + detailperkuliahan + ", " +
					// tugas.getSyaratMengumpulkanTugas());
					if (detailperkuliahan != null) {
						if (!SyaratUjianAction.checkSyaratSyaratUjian(tugas.getSyaratMengumpulkanTugas(),
								p.ambilVOPembelajaran(), mahasiswa, detailperkuliahan.getSemester(),
								tugas.getJudultugas())) {
							return;
						}
					}
				}

				if (tugas != null && mahasiswa != null && p != null && p.getStatusPertemuan() != null) {
					Session session = HibernateUtil.currentNativeSession();
					List<SyaratUjian> syaratUjians = ConstantValues
							.simpleList(
									session.createCriteria(SyaratUjian.class)
											.add(Restrictions.or(Restrictions.isNull("fakultas"),
													Restrictions.eq("fakultas", mahasiswa.getJurusan().getFakultas())))

											.add(Restrictions.or(Restrictions.isNull("jurusan"),
													Restrictions.eq("jurusan", mahasiswa.getJurusan())))

											.add(Restrictions.or(Restrictions.isNull("program"),
													Restrictions.eq("program", mahasiswa.getProgram())))

											.add(Restrictions.eq("statusPertemuan", p.getStatusPertemuan())),
									SyaratUjian.class);

					List<SyaratUjian> berlakuUntukSemuaTugas = ConstantValues.simpleList(session
							.createCriteria(SyaratUjian.class).add(Restrictions.eq("berlakuUntukSemuaTugas", true)),
							SyaratUjian.class);
					if (!berlakuUntukSemuaTugas.isEmpty()) {
						syaratUjians.addAll(berlakuUntukSemuaTugas);
					}

					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
					HibernateUtil.closeSession();

					System.out.println("syaratUjians -> " + syaratUjians.size());
					if (!syaratUjians.isEmpty()) {
						Detailperkuliahan detailperkuliahan = mahasiswa.ambilDetailperkuliahan(p.getPerkuliahan());
						if (detailperkuliahan == null) {
							mahasiswa.reInitDetailperkuliahan(HibernateUtil.currentSession());
							detailperkuliahan = mahasiswa.ambilDetailperkuliahan(p.getPerkuliahan());
						}

						System.out.println("detailperkuliahan -> " + detailperkuliahan);
						if (detailperkuliahan != null) {
							for (SyaratUjian syaratUjian : syaratUjians) {

								if (!SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian, p.ambilVOPembelajaran(),
										mahasiswa, detailperkuliahan.getSemester(), tugas.getJudultugas())) {
									syaratUjians = null;
									return;
								}
							}
						}
					}
					syaratUjians = null;
				}

				TugasFileContent tugasFileContent = tugas.ambilTugasFileContent(mahasiswa);
				if (tugasFileContent != null && tugasFileContent.getNilai() > 0.1) {
					MyMessageboxConfig.show(
							"Anda tidak perlu mengumpulkan tugas kembali, karena tugas Anda telah dinilai",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				if (ProfileUtil.chekSyarat(p.ambilVOPembelajaran(), mahasiswa, biodataCalonMahasiswa,
						tugas.getSyaratAkses())) {
					return;
				}

				final AmbilDataTugasFileContent ambilDataLampiranFileLain = new AmbilDataTugasFileContent(tugas, p,
						null, null, mahasiswa, biodataCalonMahasiswa);

//				ambilDataLampiranFileLain.setHeight("95%");
//				ambilDataLampiranFileLain.setWidth("90%");
				boolean mobile = Common.isMobile();
				ambilDataLampiranFileLain.setHeight("220px");
				ambilDataLampiranFileLain.setWidth(mobile ? "95%" : "850px");

				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataLampiranFileLain);
				ambilDataLampiranFileLain.onModal();
				ambilDataLampiranFileLain.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						try {

							Calendar kemarin = ais.ui.util.WaktuUtil.getCalendar();
							if ((tugas.getSelesai() != null && tugas.getSelesai().before(kemarin.getTime()))
									|| (tugas.getMulai() != null && tugas.getMulai().after(kemarin.getTime()))) {
								MyMessageboxConfig.show(
										"Tugas telah terlambat atau belum mulai, sehingga Anda tidak bisa meng-upload tugas ini",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								return;
							}

							if (arg0.getName().equalsIgnoreCase("baru")) {
								if (eventListener != null) {
									eventListener.onEvent(arg0);
								}
								window.detach();
							} else {
								TugasFileContent tugasFileContent = (TugasFileContent) arg0.getData();
								if (tugasFileContent != null) {
									Session session = null;
									Transaction transaction = null;

									try {
										session = StreamingHibernateUtil.getInstance().openSession();
										session.connection().setAutoCommit(false);

										final TugasFileContent copy = new TugasFileContent(tugas.getClass().getName());

										copy.setMahasiswa(mahasiswa == null ? -Common.randLong() : mahasiswa.getId());
										copy.setPertemuan(tugas.getId());
										copy.setBiodataCalonMahasiswa(biodataCalonMahasiswa == null ? -Common.randLong()
												: biodataCalonMahasiswa.getId());
										copy.setCopyDari(tugasFileContent);

										Tbmuser tbmuser = Common.getCurrentUser();
										Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
										Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
										Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
										String olehId = Common.generateOlehId(tbmuser);
										copy.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
										copy.setOlehId(olehId);
										copy.setOleh(tbmuser == null ? "external_update"
												: mahasiswa != null ? mahasiswa.getNama()
														: dosen != null ? dosen.getNama()
																: pegawai != null ? pegawai.getNama()
																		: (tbmuser.getUserNama()));

										transaction = session.beginTransaction();
										session.save(copy);
										transaction.commit();

										tugasFileContent = copy;

									} catch (Exception e) {
										if (transaction != null && transaction.isActive()) {
											try { transaction.rollback(); } catch (Exception rollbackError) { }
										}
										tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"penyimpanan salinan berkas tugas mahasiswa", e,
												new String[] {
														"Ulangi proses pengumpulan/pengambilan berkas tugas ini.",
														"Periksa apakah berkas yang dipilih masih tersedia di server." });
									} finally {
										if (session != null) {
											try { session.clear(); } catch (Exception clearError) { }
											try { session.disconnect(); } catch (Exception disconnectError) { }
											try { session.close(); } catch (Exception closeError) { }
										}
									}

									if (eventListener != null) {
										eventListener.onEvent(arg0);
									}
									window.detach();
								}

							}
						} catch (Exception e) {
							tampilErrorJikaAdmin(e);
							PesanFormalHelper.tampilkanGagalException("pengumpulan berkas tugas mahasiswa", e,
									new String[] { "Ulangi proses pengumpulan/pengambilan berkas tugas ini.",
											"Periksa kembali data tugas dan berkas yang dipilih." });
						}

						ambilDataLampiranFileLain.setVisible(false);
						ambilDataLampiranFileLain.detach();
					}
				});

			}
		});

		Common.initKeterangan(rows,
				"Jika file yang Anda upload lebih dari satu file, zip / compress / jadikan satu file terlebih dulu, kemudian baru di-upload");

		window.setVisible(true);
		window.onModal();

	}



	public static void uploadTugas(final Tugas tugas, final Siswa siswa, final CalonSiswa calonSiswa,
			final EventListener eventListener) throws Exception {

		Pertemuan pa = null;
		if (tugas instanceof Pertemuan) {
			pa = (Pertemuan) tugas;
		} else if (tugas instanceof TugasPertemuan) {
			pa = ((TugasPertemuan) tugas).ambilPertemuan();
		}
		final Pertemuan p = pa;

		final MyWindow window = new MyWindow("Upload file tugas perkuliahan", "none", true);
		window.setWidth("600px");
		window.setHeight("240px");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		South southBatal = new South();
		ais.ui.util.ZkCompat.setFlex(southBatal, true);
		southBatal.setStyle("background: #ffffff; border-top: 1px solid #e2e8f0; padding: 8px; text-align: right;");
		southBatal.setParent(borderlayout);
		Toolbar toolbarBatal = new Toolbar();
		toolbarBatal.setStyle("background: transparent; border: none;");
		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		batal.setTooltiptext("Tutup tanpa upload");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		batal.setParent(toolbarBatal);
		toolbarBatal.setParent(southBatal);

		List<Long> indsMhsPerkuliahan = new ArrayList<Long>();
		for (GeneralValueObject mhs : AbsensiSiswaHelper.populateSiswaDariPertemuan(p)) {
			indsMhsPerkuliahan.add(mhs.getId());
		}
		final AmbilDataSiswaBanbox ambilDataSiswaBanbox = new AmbilDataSiswaBanbox(indsMhsPerkuliahan);
		ambilDataSiswaBanbox.setValue(siswa == null ? "" : siswa.getNim() + " - " + siswa.getNama());
		ambilDataSiswaBanbox.setAttribute("siswa", siswa);
		ambilDataSiswaBanbox.setAttribute("myValue", siswa);
		ambilDataSiswaBanbox.setDisabled(siswa != null);

		final AmbilDataCalonSiswaBanbox ambilDataCalonSiswaBanbox = new AmbilDataCalonSiswaBanbox();
		ambilDataCalonSiswaBanbox.setValue(calonSiswa == null ? "" : calonSiswa.getNama());
		ambilDataCalonSiswaBanbox.setAttribute("calonSiswa", calonSiswa);
		ambilDataCalonSiswaBanbox.setAttribute("myValue", calonSiswa);
		ambilDataCalonSiswaBanbox.setDisabled(calonSiswa != null);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(searchgrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setVisible(p.getJadwalUjianPMB() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_siswa")));
		row.appendChild(ambilDataSiswaBanbox);
		ambilDataSiswaBanbox.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(p.getJadwalUjianPMB() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Calon Siswa"));
		row.appendChild(ambilDataCalonSiswaBanbox);
		ambilDataCalonSiswaBanbox.setWidth("90%");

		MyToolbarbuttonConfig fileupload = new MyToolbarbuttonConfig("Upload Tugas / Ambil Tugas / Tambah Link Tugas",
				"/img/File-Upload-icon.png");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(fileupload);

		fileupload.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				final CalonSiswa calonSiswa = (CalonSiswa) ambilDataCalonSiswaBanbox.getAttribute("calonSiswa");
				final Siswa siswa = (Siswa) ambilDataSiswaBanbox.getAttribute("siswa");
				if (siswa == null && calonSiswa == null) {
					PesanFormalHelper.tampilkanGagal("upload berkas tugas",
							"Data Siswa/Calon Siswa pemilik berkas belum dipilih, sehingga sistem tidak dapat "
									+ "mengaitkan berkas yang akan diunggah dengan pemiliknya.",
							new String[] { "Silakan pilih terlebih dahulu Siswa atau Calon Siswa yang bersangkutan "
									+ "sebelum mengunggah berkas." });
					return;
				}

				final AmbilDataTugasFileContent ambilDataLampiranFileLain = new AmbilDataTugasFileContent(tugas, p,
						siswa, calonSiswa, null, null);

				boolean mobile = Common.isMobile();
				ambilDataLampiranFileLain.setHeight("220px");
				ambilDataLampiranFileLain.setWidth(mobile ? "95%" : "850px");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataLampiranFileLain);
				ambilDataLampiranFileLain.onModal();
				ambilDataLampiranFileLain.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						// TODO Auto-generated method stub
						try {
							if (arg0.getName().equalsIgnoreCase("baru")) {
								if (eventListener != null) {
									eventListener.onEvent(arg0);
								}
								window.detach();
							} else {
								Object o = arg0.getData();
								if (o != null) {
									Session session = null;
									TugasFileContent tugasFileContent = null;
									try {
										session = StreamingHibernateUtil.getInstance().openSession();
										session.beginTransaction();
										tugasFileContent = ((TugasFileContent) (o instanceof TugasFileContent
												? session.get(TugasFileContent.class, ((TugasFileContent) o).getId())
												: session.createCriteria(TugasFileContent.class)
														.add(Restrictions.idEq(((Object[]) o)[0])).uniqueResult()));
										if (tugasFileContent == null) {
											throw new IllegalStateException("Berkas sumber tugas tidak ditemukan.");
										}

										if (!(o instanceof TugasFileContent) && ((Object[]) o).length == 1) {

										} else {

											final TugasFileContent copy = new TugasFileContent(
													tugas.getClass().getName());
											copy.setSiswa(siswa == null ? -Common.randLong() : siswa.getId());
											copy.setPertemuan(tugas.getId());
											copy.setCalonSiswa(
													calonSiswa == null ? -Common.randLong() : calonSiswa.getId());
											copy.setCopyDari(tugasFileContent);

											Tbmuser tbmuser = Common.getCurrentUser();
											Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
											Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
											Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
											String olehId = Common.generateOlehId(tbmuser);
											copy.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
											copy.setOlehId(olehId);
											copy.setOleh(tbmuser == null ? "external_update"
													: mahasiswa != null ? mahasiswa.getNama()
															: dosen != null ? dosen.getNama()
																	: pegawai != null ? pegawai.getNama()
																			: (tbmuser.getUserNama()));

											session.save(copy);

											tugasFileContent = copy;
										}
										session.getTransaction().commit();
									} catch (Exception e) {
										tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"penyimpanan salinan berkas tugas siswa", e,
												new String[] {
														"Ulangi proses pengumpulan/pengambilan berkas tugas ini.",
														"Periksa apakah berkas yang dipilih masih tersedia di server." });
										return;
									} finally {
										HibernateUtil.closeSessionQuietly(session);
									}

									if (eventListener != null) {
										eventListener.onEvent(arg0);
									}
									window.detach();
								}

							}
						} catch (Exception e) {
							tampilErrorJikaAdmin(e);
							PesanFormalHelper.tampilkanGagalException("pengumpulan berkas tugas siswa", e,
									new String[] { "Ulangi proses pengumpulan/pengambilan berkas tugas ini.",
											"Periksa kembali data tugas dan berkas yang dipilih." });
						}

						ambilDataLampiranFileLain.setVisible(false);
						ambilDataLampiranFileLain.detach();
					}
				});

			}
		});

		Common.initKeterangan(rows,
				"Jika file yang Anda upload lebih dari satu file, zip / compress / jadikan satu file terlebih dulu, kemudian baru di-upload");

		window.setVisible(true);
		window.onModal();

	}



	public static void uploadTugas(final Tugas tugas, final CalonSiswa calonSiswa, final EventListener eventListener)
			throws Exception {

		final MyWindow window = new MyWindow("Upload file tugas perkuliahan", "none", true);
		window.setWidth("600px");
		window.setHeight("240px");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		MyPanel panel = new MyPanel();
		panel.setParent(window);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Upload file tugas perkuliahan");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		South southBatal = new South();
		ais.ui.util.ZkCompat.setFlex(southBatal, true);
		southBatal.setStyle("background: #ffffff; border-top: 1px solid #e2e8f0; padding: 8px; text-align: right;");
		southBatal.setParent(borderlayout);
		Toolbar toolbarBatal = new Toolbar();
		toolbarBatal.setStyle("background: transparent; border: none;");
		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		batal.setTooltiptext("Tutup tanpa upload");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		batal.setParent(toolbarBatal);
		toolbarBatal.setParent(southBatal);

		final AmbilDataCalonSiswaBanbox ambilDataCalonSiswaBanbox = new AmbilDataCalonSiswaBanbox();
		ambilDataCalonSiswaBanbox
				.setValue(calonSiswa == null ? "" : calonSiswa.getNim() + " - " + calonSiswa.getNama());
		ambilDataCalonSiswaBanbox.setAttribute("calonSiswa", calonSiswa);
		ambilDataCalonSiswaBanbox.setAttribute("siswa", calonSiswa);
		ambilDataCalonSiswaBanbox.setDisabled(calonSiswa != null);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(center);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Calon Siswa"));
		row.appendChild(ambilDataCalonSiswaBanbox);
		ambilDataCalonSiswaBanbox.setWidth("90%");

		MyToolbarbuttonConfig fileupload = new MyToolbarbuttonConfig("Upload Tugas" + Common.ukuranLabelFileUpload(),
				"/img/File-Upload-icon.png");
		fileupload.setUpload(Common.ukuranFileUpload());

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Upload Tugas"));
		row.appendChild(fileupload);

		fileupload.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) arg0;
				try {
					CalonSiswa calonSiswa = (CalonSiswa) ambilDataCalonSiswaBanbox.getAttribute("calonSiswa");
					if (calonSiswa == null) {
						PesanFormalHelper.tampilkanGagal("upload berkas",
								"Data Calon Siswa pemilik berkas belum dipilih, sehingga sistem tidak dapat "
										+ "mengaitkan berkas yang akan diunggah dengan pemiliknya.",
								new String[] { "Silakan pilih terlebih dahulu Calon Siswa yang bersangkutan sebelum "
										+ "mengunggah berkas." });
						return;
					}
					Media media = uploadEvent.getMedia();
					if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
						return;
					Session session = StreamingHibernateUtil.getInstance().currentSession();

					TugasFileContent tugasFileContent = new TugasFileContent(tugas.getClass().getName());
					Blob blob = Common.getBlobFromMedia(media, session);
					tugasFileContent.setFoto(blob);
					tugasFileContent.setNama(media.getName());
					tugasFileContent.setFileMimeType(media.getContentType());

					tugasFileContent.setPertemuan(tugas.getId());
					tugasFileContent.setCalonSiswa(calonSiswa.getId());
					tugasFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());

					tugasFileContent.ubahRealNameSesuaiDenganNIM(calonSiswa);

					session.getTransaction().begin();
					session.save(tugasFileContent);
					session.getTransaction().commit();
					StreamingHibernateUtil.getInstance().closeSession();

					if (eventListener != null) {
						eventListener.onEvent(uploadEvent);
					}
					window.detach();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("upload berkas",
							"Berkas yang Bapak/Ibu unggah kemungkinan melebihi ukuran maksimum yang diperbolehkan, "
									+ "atau format berkas tidak sesuai dengan yang diminta sistem.",
							e, new String[] {
									"Periksa kembali ukuran dan format berkas, lalu unggah ulang.",
									"Jika berkas lebih dari satu, gabungkan (zip/compress) terlebih dahulu menjadi satu berkas." });
				}

			}
		});

		Common.initKeterangan(rows,
				"Jika file yang Anda upload lebih dari satu file, zip / compress / jadikan satu file terlebih dulu, kemudian baru di-upload");

		window.setVisible(true);
		window.onModal();

	}
	public static void copy(File file, File fileTujuan) throws Exception {
		FileInputStream fileInputStream = null;
		FileOutputStream fileOutputStream = null;
		try {
			if (file == null || fileTujuan == null) {
				return;
			}
			if (!file.exists() || !file.isFile()) {
				return;
			}
			File parent = fileTujuan.getParentFile();
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}
			try {
				if (file.getCanonicalPath().equals(fileTujuan.getCanonicalPath())) {
					return;
				}
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/CommonFileMediaHelper.java:1072");
			}
			File tempFile = new File(fileTujuan.getAbsolutePath() + ".tmp");
			fileInputStream = new FileInputStream(file);
			fileOutputStream = new FileOutputStream(tempFile);
			IOUtils.copy(fileInputStream, fileOutputStream);
			try {
				fileOutputStream.flush();
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/CommonFileMediaHelper.java:1080");
			}
			try {
				fileOutputStream.close();
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/CommonFileMediaHelper.java:1084");
			}
			fileOutputStream = null;
			if (fileTujuan.exists() && !fileTujuan.delete()) {
				// tetap lanjut rename; sebagian filesystem mengizinkan replace langsung.
			}
			if (!tempFile.renameTo(fileTujuan)) {
				fileInputStream.close();
				fileInputStream = null;
				fileInputStream = new FileInputStream(tempFile);
				fileOutputStream = new FileOutputStream(fileTujuan);
				IOUtils.copy(fileInputStream, fileOutputStream);
				tempFile.delete();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			throw e;
		} finally {
			if (fileInputStream != null) {
				try { fileInputStream.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonFileMediaHelper.java:1103"); }
			}
			if (fileOutputStream != null) {
				try { fileOutputStream.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonFileMediaHelper.java:1106"); }
			}
		}
	}




	public static void checkBackgroundUpload() {
		try {
			try {
				HibernateUtil.closeSession();
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/CommonFileMediaHelper.java:1118");
			}

			LampiranLain lampiranLain = LampiranLain.ambil(LampiranLain.BACKGROUND_DEPAN_1,
					LampiranLain.BACKGROUND_DEPAN_1_STR);

			if (lampiranLain != null) {
				File file = lampiranLain.ambilFile();
				// System.out.println("file bg 1 => " + file);
				if (file != null && file.exists()) {

					File img1 = new File(Common.REAL_PATH + "/img/bg.jpg");
					try {
						Common.copy(file, img1);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						tampilErrorJikaAdmin(e);
					}
				}
			}

			lampiranLain = LampiranLain.ambil(LampiranLain.BACKGROUND_DEPAN_2, LampiranLain.BACKGROUND_DEPAN_2_STR);

			if (lampiranLain != null) {
				File file = lampiranLain.ambilFile();
				// System.out.println("file bg 2 => " + file);
				if (file != null && file.exists()) {

					File img1 = new File(Common.REAL_PATH + "/img/main.jpg");
					try {
						Common.copy(file, img1);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						tampilErrorJikaAdmin(e);
					}
				}
			}

			lampiranLain = LampiranLain.ambil(LampiranLain.BANNER_1, LampiranLain.BANNER_1_STR);

			if (lampiranLain != null) {
				File file = lampiranLain.ambilFile();
				// System.out.println("file bg 1 => " + file);
				if (file != null && file.exists()) {

					File img1 = new File(Common.REAL_PATH + "/img/header.jpg");
					try {
						Common.copy(file, img1);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						tampilErrorJikaAdmin(e);
					}
				}
			}

		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
		} finally {
			try {
				HibernateUtil.closeSession();
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/CommonFileMediaHelper.java:1178");
			}
			try {
				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/CommonFileMediaHelper.java:1182");
			}
		}
	}



	public static void checkFaviconUpload() {
		try {
			try {
				HibernateUtil.closeSession();
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/CommonFileMediaHelper.java:1193");
			}

			LampiranLain lampiranLain = LampiranLain.ambil(LampiranLain.FAVICON, LampiranLain.FAVICON_STR);

			if (lampiranLain != null) {
				File file = lampiranLain.ambilFile();
				// System.out.println("file favicon => " + file);
				if (file != null && file.exists()) {
					File img1 = new File(Common.REAL_PATH + "/img/favicon.ico");
					try {
						Common.copy(file, img1);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						tampilErrorJikaAdmin(e);
					}
				}
			}

		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
		} finally {
			try {
				HibernateUtil.closeSession();
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/CommonFileMediaHelper.java:1217");
			}
			try {
				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/CommonFileMediaHelper.java:1221");
			}
		}
	}



	public static void writeInputStreamToFile(InputStream inputStream, File file) {
		CommonFileUtil.writeInputStreamToFile(inputStream, file);
	}



	public static void writeBlobToFile(Blob blob, File file) {

		CommonFileUtil.writeBlobToFile(blob, file);
	}



	public static File getCreateRandomFile() {
		return CommonFileUtil.getCreateRandomFile();
	}



	public static void unzipFolder(Path source, Path target) throws IOException {
		/* ZipInputStream membaca local header dan pada beberapa ZIP lama gagal untuk
		 * entry STORED yang memakai extended data descriptor. ZipFile memakai central
		 * directory sehingga kedua variasi (STORED/DEFLATED) dapat dibaca dengan benar. */
		ZipFile zip = null;
		try {
			zip = new ZipFile(source.toFile());
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
				Path newPath = zipSlipProtect(zipEntry, target);
				if (zipEntry.isDirectory() || zipEntry.getName().endsWith("/")) {
					Files.createDirectories(newPath);
					continue;
				}
				if (newPath.getParent() != null) {
					Files.createDirectories(newPath.getParent());
				}
				InputStream in = null;
				FileOutputStream out = null;
				try {
					in = zip.getInputStream(zipEntry);
					out = new FileOutputStream(newPath.toFile());
					byte[] buffer = new byte[16384];
					int read;
					while ((read = in.read(buffer)) != -1) {
						out.write(buffer, 0, read);
					}
				} finally {
					if (out != null) try { out.close(); } catch (IOException ignored) { }
					if (in != null) try { in.close(); } catch (IOException ignored) { }
				}
			}
		} finally {
			if (zip != null) try { zip.close(); } catch (IOException ignored) { }
		}

	}


	public static Path zipSlipProtect(ZipEntry zipEntry, Path targetDir) throws IOException {

		// test zip slip vulnerability
		// Path targetDirResolved = targetDir.resolve("../../" + zipEntry.getName());

		Path normalizedTarget = targetDir.toAbsolutePath().normalize();
		Path targetDirResolved = normalizedTarget.resolve(zipEntry.getName());

		// make sure normalized file still has targetDir as its prefix
		// else throws exception
		Path normalizePath = targetDirResolved.normalize();
		if (!normalizePath.startsWith(normalizedTarget)) {
			throw new IOException("Bad zip entry: " + zipEntry.getName());
		}

		return normalizePath;
	}



	public static List<File> extractZip(File file, String destinationname) throws Exception {
		return extractZip(file, destinationname, false);
	}



	public static List<File> extractZip(File file, String destinationname, boolean extrackFileOnDirectory)
			throws Exception {
		ZipInputStream zipinputstream = new ZipInputStream(new FileInputStream(file));
		byte[] buf = new byte[1024];
		List<File> files = new ArrayList<File>();
		ZipEntry zipentry = zipinputstream.getNextEntry();
		File fileRoot = null;
		while (zipentry != null) {
			String entryName = zipentry.getName();
			// System.out.println("entryname " + entryName);
			int n;
			FileOutputStream fileoutputstream;
			File newFile = new File(entryName);
			String directory = newFile.getParent();

			if (directory == null) {
				if (newFile.isDirectory())
					break;
			}

			File insideFile = new File(destinationname + "/" + entryName);
			if (fileRoot == null) {
				fileRoot = new File(insideFile.getAbsolutePath());
			}
			if (insideFile.isDirectory()) {
				insideFile.mkdirs();
				zipentry = zipinputstream.getNextEntry();
				continue;
			}
			String lastString = entryName.substring(entryName.length() - 1, entryName.length());
			// System.out.println("lastString " + lastString + " " +
			// lastString.equals("/"));
			if (lastString.equals("/")) {
				insideFile.mkdirs();
				zipentry = zipinputstream.getNextEntry();
				continue;
			}

			// System.out.println("insideFile " + insideFile.getAbsolutePath());
			insideFile.getParentFile().mkdirs();
			// insideFile.createNewFile();
			fileoutputstream = new FileOutputStream(insideFile);
			while ((n = zipinputstream.read(buf, 0, 1024)) > -1)
				fileoutputstream.write(buf, 0, n);

			fileoutputstream.close();
			zipinputstream.closeEntry();
			zipentry = zipinputstream.getNextEntry();
			files.add(insideFile);
		}
		zipinputstream.close();

		// System.out.println("fileRoot " + fileRoot + ", extrackFileOnDirectory
		// = " + extrackFileOnDirectory);
		if (extrackFileOnDirectory) {
			if (fileRoot != null && fileRoot.isDirectory()) {
				FileUtils.copyDirectory(fileRoot, new File(destinationname));
			}
		}

		return files;
	}



	public static Boolean createZip(List<String> filesName, List<File> filenames, File outFilename) {
		if (filenames.size() == 0) {
			return false;
		}
		// These are the files to include in the ZIP file

		// Create a buffer for reading the files
		byte[] buf = new byte[1024];

		try {
			outFilename.createNewFile();
			// Create the ZIP file
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename));

			int i = 0;
			// Compress the files
			for (File filename : filenames) {
				FileInputStream in = new FileInputStream(filename);

				// Add ZIP entry to output stream.
				out.putNextEntry(new ZipEntry(filesName.get(i)));

				// Transfer bytes from the file to the ZIP file
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}

				// Complete the entry
				out.closeEntry();
				in.close();
				i++;
			}

			// Complete the ZIP file
			out.close();
		} catch (IOException e) {
			Common.tampilErrorJikaAdmin(e);
			return false;
		}
		return true;
	}



	public static Boolean createZip(List<File> filenames, File outFilename) {
		if (filenames.size() == 0) {
			return false;
		}
		// These are the files to include in the ZIP file

		// Create a buffer for reading the files
		byte[] buf = new byte[1024];

		try {
			outFilename.createNewFile();
			// Create the ZIP file
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename));

//			int i = 0;
			// Compress the files
			for (File filename : filenames) {
				FileInputStream in = new FileInputStream(filename);

				// Add ZIP entry to output stream.
				out.putNextEntry(new ZipEntry(filename.getName()));

				// Transfer bytes from the file to the ZIP file
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}

				// Complete the entry
				out.closeEntry();
				in.close();
//				i++;
			}

			// Complete the ZIP file
			out.close();
		} catch (IOException e) {
			Common.tampilErrorJikaAdmin(e);
			return false;
		}
		return true;
	}



	public static void display(FileFoto alurFile) throws Exception {
		WindowViewerHelper.display(alurFile);
	}



	public static MyToolbarbuttonConfig simpanKeDrive(FileFoto fileFoto, File f, String folderName,
			EventListener eventListener) {
		return simpanKeDrive(fileFoto, f, folderName, null, eventListener);
	}



	public static MyToolbarbuttonConfig simpanKeDrive(FileFoto fileFoto, File f, String folderName,
			String folderNameLagi, EventListener eventListener) {
		return WindowViewerHelper.simpanKeDrive(fileFoto, f, folderName, folderNameLagi, eventListener);
	}



	public static String getFileExtension(File file) {
		String name = file.getName();
		int lastIndexOf = name.lastIndexOf(".");
		if (lastIndexOf == -1) {
			return ""; // empty extension
		}
		return name.substring(lastIndexOf + 1);
	}



	public static void previewFile(String url) throws Exception {
		final MyWindow window = new MyWindow("Preview File", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("99%");
		window.setWidth("99%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		final Iframe include = new Iframe();
		include.setParent(center);
		include.setAlign("center");
		include.setHeight("100%");
		include.setWidth("100%");
		include.setSrc(url);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setStyle("background: #ffffff; border-top: 1px solid #e2e8f0; padding: 8px;");
		south.setParent(borderlayout);
		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("float: right; background: transparent; border: none;");
		MyToolbarbuttonConfig tutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		tutup.setTooltiptext("Tutup");
		tutup.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		tutup.setParent(toolbar);
		toolbar.setParent(south);

		window.setVisible(true);
		window.onModal();
	}




	public static void createDownloadUploadFileLampiran(Rows rows, Mahasiswa mahasiswa, String jenis,
			String keterangan) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");

		row.setParent(rows);
		row.setValign("top");
		row.setAttribute("jenis", true);

		MyFormRow parentPreview = new MyFormRow();

		parentPreview.setParent(rows);

		Hbox hbox = new Hbox();
		hbox.setParent(parentPreview);

		createDownloadUploadFileLampiran(row, hbox, mahasiswa, jenis, keterangan);
	}



	public static void createDownloadUploadFileLampiran(Row row, Hbox parentPreview, GeneralValueObject mahasiswa,
			String jenis, String keterangan) {
		boolean harusPdf = false;
		Long myref = mahasiswa == null ? null : mahasiswa.getId();
		EventListener eventListener = null;
		Map<String, FileFotoLain> lampiranLains = null;
		boolean tidakTampilJurusan = false;
		boolean hanyaIcon = false;
		boolean usingId = false;
		boolean tampilUpload = true;

		Integer cutomUkuranUpload = null;
		boolean vertical = false;
		boolean janganPreviewDiLayarUtama = false;
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		FileFotoLain.createDownloadUpload(hbox, myref, jenis, keterangan, harusPdf, eventListener, lampiranLains,
				tidakTampilJurusan, hanyaIcon, usingId, tampilUpload, cutomUkuranUpload, vertical,
				janganPreviewDiLayarUtama, parentPreview, LampiranLainMahasiswa.class);

	}



	public static void createDownloadUploadFoto(Component row, Serializable ref, Class<? extends FileFotoLain> fileFoto,
			EventListener eventListener, boolean tampilUpload) throws Exception {
		boolean harusPdf = false;
		Map<String, FileFotoLain> lampiranLains = null;
		boolean tidakTampilJurusan = false;
		boolean hanyaIcon = false;
		boolean usingId = false;

		Hbox parentPreview = null;

		Integer cutomUkuranUpload = null;
		boolean vertical = false;
		boolean janganPreviewDiLayarUtama = true;
		Hbox hbox = new Hbox();
		hbox.setParent(row);

		FileFotoLain d = fileFoto.newInstance();

		String jenis = d.getJenis();
		String keterangan = "Foto Formal";

		FileFotoLain.createDownloadUpload(hbox, ref, jenis, keterangan, harusPdf, eventListener, lampiranLains,
				tidakTampilJurusan, hanyaIcon, usingId, tampilUpload, cutomUkuranUpload, vertical,
				janganPreviewDiLayarUtama, parentPreview, fileFoto);

	}



	public static void createDownloadUploadFoto(Component parent, GeneralValueObject generalValueObject,
			Class<? extends FileFotoLain> fileFoto, final EventListener eventListenerFotoUpload, boolean tampilUpload)
			throws Exception {

		Vbox vbox = new Vbox();
		vbox.setPack("top");
		vbox.setAlign("center");
		vbox.setHeight("100%");
		vbox.setWidth("100%");
		vbox.setParent(parent);

		// CSS untuk bingkai foto — diinjeksi sekali; browser mengabaikan tag style duplikat
		new Html(
			"<style id='css-foto-profil-frame'>" +
			".foto-profil-frame{" +
				"display:inline-block;border-radius:18px;overflow:hidden;" +
				"box-shadow:0 4px 18px rgba(0,0,0,.15);" +
				"border:3px solid #e2e8f0;background:#f8fafc;" +
				"transition:box-shadow .25s,border-color .25s;cursor:pointer;}" +
			".foto-profil-frame:hover{" +
				"box-shadow:0 8px 28px rgba(0,0,0,.22);border-color:#818cf8;}" +
			".gambar_profile{display:block!important;width:260px!important;" +
				"min-height:200px;object-fit:cover;object-position:top center;}" +
			".foto-profil-hint{font-size:11px;color:#94a3b8;text-align:center;" +
				"margin-top:8px;padding:0 4px;line-height:1.6;}" +
			"</style>"
		).setParent(vbox);

		// Bingkai foto dengan efek hover
		Div fotoFrame = new Div();
		fotoFrame.setSclass("foto-profil-frame");
		vbox.appendChild(fotoFrame);

		final Hbox hbox = new Hbox();
		hbox.setStyle("margin:0;padding:0;");
		fotoFrame.appendChild(hbox);

		// Cari foto langsung pakai class yang diberikan (bukan ProfileImageUtil) agar
		// tidak salah tipe — misal BiodataCalonMahasiswa punya Mahasiswa, ProfileImageUtil
		// akan salah lookup FotoMahasiswa padahal foto tersimpan sebagai FotoBiodataCalonMahasiswa.
		String url = ProfileImageUtil.getUrlFotoDariObject(generalValueObject, false);
		if (generalValueObject != null && generalValueObject.getId() != null && fileFoto != null
				&& !(generalValueObject instanceof FileFotoLain)) {
			try {
				FileFotoLain templateFoto = fileFoto.newInstance();
				Serializable refId = (generalValueObject instanceof Tbmuser)
						? ((Tbmuser) generalValueObject).getUserId()
						: generalValueObject.getId();
				FileFotoLain existingFoto = FileFotoLain.ambil(refId, templateFoto.getJenis(), fileFoto);
				if (existingFoto != null) {
					String directUrl = existingFoto.createLinkUri();
					if (directUrl != null && !directUrl.isEmpty()) {
						url = directUrl;
					}
				}
			} catch (Exception _exLookup) { ais.common.ErrorAuditUtil.record(_exLookup, "auto-audit(empty-catch) src/ais/common/CommonFileMediaHelper.java:1664"); /* abaikan, pakai url dari ProfileImageUtil */ }
		}

		Image img;
		hbox.appendChild(img = new Image(url));
		img.setWidth("260px");
		img.setSclass("gambar_profile");
		vbox.setTooltiptext("class -> " + (fileFoto != null ? fileFoto.getName() : "") + " generalValueObject "
				+ (generalValueObject != null ? generalValueObject.getNama() : "") + ", image url = " + url);

		// Keterangan di bawah foto
		new Html("<div class='foto-profil-hint'>&#x1F4F7; Klik tombol upload untuk mengganti foto</div>")
				.setParent(vbox);

		if (!(generalValueObject instanceof Tbmuser) && generalValueObject.getId() == null) {
			img.setSrc(Common.getRequestHostWithProtocol() + "/img/administrator-icon.png");
		} else if ((generalValueObject instanceof Tbmuser) && ((Tbmuser) generalValueObject).getUserId() == null) {
			img.setSrc(Common.getRequestHostWithProtocol() + "/img/administrator-icon.png");
		}

		final Class<? extends FileFotoLain> callbackFileFotoClass = fileFoto;
		Common.createDownloadUploadFoto(vbox,
				generalValueObject instanceof Tbmuser ? ((Tbmuser) generalValueObject).getUserId()
						: generalValueObject.getId() == null ? Common.randLong() : generalValueObject.getId(),
				fileFoto, new EventListener() {

					@Override
					public void onEvent(Event arg0a) throws Exception {

						try {
							Common.clear(hbox);
							FileFotoLain fileFoto = (FileFotoLain) arg0a.getData();
							String link = fileFoto.createLinkUri();

							if (fileFoto.getGdrive() != null) {
								link = fileFoto.exportGDriveUrl();
							} else if (fileFoto.getLink() != null
									&& fileFoto.getLink().toLowerCase().contains("dropbox")) {
								link = fileFoto.dropboxLinkRaw();
							}

							System.out.println("link -> " + link);

							Image img;
							hbox.appendChild(img = new Image(link));
							img.setWidth("260px");
							img.setSclass("gambar_profile");
						} catch (Exception e) {
							tampilErrorJikaAdmin(e);
						}

						if (eventListenerFotoUpload != null) {
							Object data = arg0a == null ? null : arg0a.getData();
							if (data == null || callbackFileFotoClass == null || callbackFileFotoClass.isInstance(data)) {
								eventListenerFotoUpload.onEvent(arg0a);
							}
						}

					}
				}, tampilUpload);

	}



	public static void createDownloadUploadFileLampiranBeasiswa(Hbox row,
			final MahasiswaBeasiswaPersyaratan mahasiswaBeasiswaPersyaratan, final String keterangan) {

		boolean harusPdf = false;
		Long myref = mahasiswaBeasiswaPersyaratan == null ? null : mahasiswaBeasiswaPersyaratan.getId();
		EventListener eventListener = null;
		Map<String, FileFotoLain> lampiranLains = null;
		boolean tidakTampilJurusan = false;
		boolean hanyaIcon = false;
		boolean usingId = false;
		boolean tampilUpload = true;

		Integer cutomUkuranUpload = null;
		boolean vertical = false;
		boolean janganPreviewDiLayarUtama = false;
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		FileFotoLain.createDownloadUpload(hbox, myref, LampiranBeasiswaMahasiswa.DEFAULT_JENIS, keterangan, harusPdf,
				eventListener, lampiranLains, tidakTampilJurusan, hanyaIcon, usingId, tampilUpload, cutomUkuranUpload,
				vertical, janganPreviewDiLayarUtama, null, LampiranBeasiswaMahasiswa.class);

	}



	public static void createDownloadUploadFileLampiranKkn(Component row,
			final MahasiswaKknPersyaratan mahasiswaKknPersyaratan, final String keterangan) {

		boolean harusPdf = false;
		Long myref = mahasiswaKknPersyaratan == null ? null : mahasiswaKknPersyaratan.getId();
		EventListener eventListener = null;
		Map<String, FileFotoLain> lampiranLains = null;
		boolean tidakTampilJurusan = false;
		boolean hanyaIcon = false;
		boolean usingId = false;
		boolean tampilUpload = true;

		Integer cutomUkuranUpload = null;
		boolean vertical = false;
		boolean janganPreviewDiLayarUtama = false;
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		FileFotoLain.createDownloadUpload(hbox, myref, LampiranKknMahasiswa.DEFAULT_JENIS, keterangan, harusPdf,
				eventListener, lampiranLains, tidakTampilJurusan, hanyaIcon, usingId, tampilUpload, cutomUkuranUpload,
				vertical, janganPreviewDiLayarUtama, null, LampiranKknMahasiswa.class);

	}



	public static void createDownloadUploadFileLampiranPkl(Component row,
			final MahasiswaPklPersyaratan mahasiswaPklPersyaratan, final String keterangan) {

		boolean harusPdf = false;
		Long myref = mahasiswaPklPersyaratan == null ? null : mahasiswaPklPersyaratan.getId();
		EventListener eventListener = null;
		Map<String, FileFotoLain> lampiranLains = null;
		boolean tidakTampilJurusan = false;
		boolean hanyaIcon = false;
		boolean usingId = false;
		boolean tampilUpload = true;

		Integer cutomUkuranUpload = null;
		boolean vertical = false;
		boolean janganPreviewDiLayarUtama = false;
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		FileFotoLain.createDownloadUpload(hbox, myref, LampiranPklMahasiswa.DEFAULT_JENIS, keterangan, harusPdf,
				eventListener, lampiranLains, tidakTampilJurusan, hanyaIcon, usingId, tampilUpload, cutomUkuranUpload,
				vertical, janganPreviewDiLayarUtama, null, LampiranPklMahasiswa.class);

	}



	public static String ukuranFileUpload() {
		return ukuranFileUpload(null);
	}



	public static String ukuranFileUpload(Integer cutomUkuranUpload) {
		String upload = (cutomUkuranUpload == null
				? "true,maxsize=" + Common.getKonfigurasi("ukuran_maksimal_file_diupload", "1024").getNilai()
				: "true,maxsize=" + cutomUkuranUpload);
		// // System.out.println(upload);
		return upload;
	}



	public static String ukuranLabelFileUpload() {
		return ukuranLabelFileUpload(null);
	}



	public static String ukuranLabelFileUpload(Integer cutomUkuranUpload) {
		int ukuran = 520;
		try {
			ukuran = Integer.parseInt(Common.getKonfigurasi("ukuran_maksimal_file_diupload", "1024").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonFileMediaHelper.java:1824");
			// TODO: handle exception
		}
		return cutomUkuranUpload == null ? " (maks " + Common.numberFormat.get().format(ukuran / 1024) + " Mb)"
				: " (maks " + Common.numberFormat.get().format(cutomUkuranUpload / 1024) + " Mb)";
	}


	public static File createWritableWorkDir(String prefix) throws IOException {
		String nama = (prefix == null || prefix.trim().isEmpty() ? "ecampus" : prefix.trim()) + "_"
				+ ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis();
		File dir = createWritableWorkDirInBase(new File("/opt/ecampus"), nama);
		if (dir != null) {
			return dir;
		}
		dir = createWritableWorkDirInBase(new File(System.getProperty("java.io.tmpdir")), nama);
		if (dir != null) {
			return dir;
		}
		throw new IOException("Gagal membuat folder kerja sementara untuk arsip lampiran. Folder /opt/ecampus dan "
				+ "java.io.tmpdir tidak dapat dibuat/ditulis. Periksa izin akses dan kapasitas disk server.");
	}

	private static File createWritableWorkDirInBase(File baseDir, String nama) {
		if (baseDir == null || nama == null || nama.trim().isEmpty()) {
			return null;
		}
		try {
			if (!baseDir.exists() && !baseDir.mkdirs()) {
				return null;
			}
			if (!baseDir.isDirectory() || !baseDir.canWrite()) {
				return null;
			}
			File dir = new File(baseDir, nama);
			if (!dir.exists() && !dir.mkdirs()) {
				return null;
			}
			return dir.isDirectory() && dir.canWrite() ? dir : null;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "CommonFileMediaHelper.createWritableWorkDirInBase");
			return null;
		}
	}



	public static void zipDir(String zipFileName, String dir) throws Exception {
		File dirObj = new File(dir);
		// Pastikan folder SUMBER ada (pemanggil mungkin belum membuatnya / tak ada lampiran) dan
		// folder INDUK file .zip ada, agar FileOutputStream tidak melempar FileNotFoundException.
		if (!dirObj.exists()) {
			dirObj.mkdirs();
		}
		File zipFile = new File(zipFileName);
		File indukZip = zipFile.getParentFile();
		if (indukZip != null && !indukZip.exists()) {
			indukZip.mkdirs();
		}
		// mkdirs() bisa GAGAL diam-diam (return false) bila folder induk tidak dapat dibuat/ditulis
		// (mis. /opt/ecampus tidak ada & tak berizin, atau disk penuh). Bila dibiarkan, FileOutputStream
		// melempar "FileNotFoundException: ... (No such file or directory)" yang membingungkan dan
		// mengotori log ECAMPUS. Ubah menjadi pesan yang JELAS & dapat ditindaklanjuti oleh operator.
		if (indukZip != null && !indukZip.exists()) {
			throw new IOException("Gagal membuat arsip: folder tujuan '" + indukZip.getAbsolutePath()
					+ "' tidak dapat dibuat/ditulis. Periksa keberadaan folder, izin akses, dan kapasitas disk server.");
		}
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
		try {
			addDir(dirObj, out);
		} finally {
			out.close();
		}
	}



	public static void addDir(File dirObj, ZipOutputStream out) throws IOException {
		File[] files = dirObj.listFiles();
		// listFiles() = null bila direktori tidak ada/bukan direktori → cegah NPE files.length.
		if (files == null) {
			return;
		}
		byte[] tmpBuf = new byte[1024];

		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				addDir(files[i], out);
				continue;
			}
			FileInputStream in = new FileInputStream(files[i].getAbsolutePath());
			// System.out.println(" Adding: " + files[i].getAbsolutePath());
			out.putNextEntry(new ZipEntry(files[i].getParentFile().getName() + "/" + files[i].getName()));
			int len;
			while ((len = in.read(tmpBuf)) > 0) {
				out.write(tmpBuf, 0, len);
			}
			out.closeEntry();
			in.close();
		}
	}



	/**
	 * Batas megapiksel gambar yang diproses server. Mencegah OOM
	 * {@code java.lang.OutOfMemoryError: Requested array size exceeds VM limit} akibat gambar
	 * rusak/raksasa (image bomb) yang MENDEKLARASIKAN dimensi sangat besar sehingga raster
	 * {@code width*height} melebihi batas array JVM (~2,1 miliar elemen). 100 MP jauh di atas
	 * foto ponsel nyata, tetapi menahan bom (mis. 60000x60000 = 3,6 miliar piksel).
	 */
	private static final long MAKS_PIKSEL_GAMBAR = 100L * 1000L * 1000L;

	/**
	 * Mengembalikan {@code [lebar, tinggi]} gambar dari HEADER saja (TIDAK men-decode raster,
	 * jadi TIDAK mengalokasikan array width*height), atau {@code null} bila bukan gambar yang
	 * dikenali / tak terbaca. Aman terhadap {@link OutOfMemoryError} (ditangkap sebagai Throwable —
	 * "Requested array size exceeds VM limit" adalah gagal PRE-alokasi, heap tetap sehat, aman
	 * ditangkap). Java 1.6/1.7 friendly (tanpa try-with-resources).
	 */
	public static int[] dimensiGambarAman(File file) {
		if (file == null || !file.exists() || !file.isFile()) {
			return null;
		}
		javax.imageio.stream.ImageInputStream iis = null;
		javax.imageio.ImageReader reader = null;
		try {
			iis = ImageIO.createImageInputStream(file);
			if (iis == null) {
				return null;
			}
			java.util.Iterator<javax.imageio.ImageReader> it = ImageIO.getImageReaders(iis);
			if (!it.hasNext()) {
				return null; // format tak dikenali -> bukan gambar
			}
			reader = it.next();
			reader.setInput(iis, true, true);
			int w = reader.getWidth(0);
			int h = reader.getHeight(0);
			if (w <= 0 || h <= 0) {
				return null;
			}
			return new int[] { w, h };
		} catch (Throwable t) { // termasuk OutOfMemoryError dari header korup
			return null;
		} finally {
			if (reader != null) {
				try {
					reader.dispose();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonFileMediaHelper.java:1935");
				}
			}
			if (iis != null) {
				try {
					iis.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonFileMediaHelper.java:1941");
				}
			}
		}
	}

	/**
	 * Membaca gambar dengan AMAN untuk jalur upload/resize: menolak ({@code null}) bila dimensi
	 * melebihi {@link #MAKS_PIKSEL_GAMBAR} atau file bukan gambar, dan menangkap
	 * {@link OutOfMemoryError}. Pakai ini menggantikan {@code ImageIO.read(file)} langsung agar
	 * tidak OOM oleh image bomb (dimensi dicek dari header dulu sebelum decode raster penuh).
	 */
	public static java.awt.image.BufferedImage bacaGambarAman(File file) {
		int[] dim = dimensiGambarAman(file);
		if (dim == null) {
			return null;
		}
		if ((long) dim[0] * (long) dim[1] > MAKS_PIKSEL_GAMBAR) {
			return null; // terlalu besar -> jangan decode (cegah OOM)
		}
		try {
			return ImageIO.read(file);
		} catch (Throwable t) {
			return null;
		}
	}

	/** Batas ukuran BERKAS gambar (terkompresi) yang dibaca dari stream ke memori. Berkas gambar
	 *  wajar jauh di bawah ini; menahan stream raksasa agar tak OOM saat dibuffer. */
	private static final int MAKS_BYTE_GAMBAR = 60 * 1024 * 1024;

	/**
	 * Versi byte[] dari {@link #bacaGambarAman(File)}: cek dimensi dari header lalu decode via
	 * ImageReader yang SAMA; tolak ({@code null}) bila dimensi &gt; batas / bukan gambar; tangkap
	 * {@link OutOfMemoryError}. Java 1.6/1.7 friendly.
	 */
	public static java.awt.image.BufferedImage bacaGambarAman(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return null;
		}
		javax.imageio.stream.ImageInputStream iis = null;
		javax.imageio.ImageReader reader = null;
		try {
			iis = ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes));
			if (iis == null) {
				return null;
			}
			java.util.Iterator<javax.imageio.ImageReader> it = ImageIO.getImageReaders(iis);
			if (!it.hasNext()) {
				return null;
			}
			reader = it.next();
			reader.setInput(iis, true, true);
			int w = reader.getWidth(0);
			int h = reader.getHeight(0);
			if (w <= 0 || h <= 0 || (long) w * (long) h > MAKS_PIKSEL_GAMBAR) {
				return null;
			}
			return reader.read(0); // dimensi sudah dibatasi -> aman decode
		} catch (Throwable t) {
			return null;
		} finally {
			if (reader != null) {
				try {
					reader.dispose();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonFileMediaHelper.java:2006");
				}
			}
			if (iis != null) {
				try {
					iis.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonFileMediaHelper.java:2012");
				}
			}
		}
	}

	/**
	 * Versi InputStream: membaca stream ke byte[] TERBATAS ({@link #MAKS_BYTE_GAMBAR}) lalu delegasi
	 * ke {@link #bacaGambarAman(byte[])}. Tidak menutup {@code in} (biar pemanggil yang kelola).
	 */
	public static java.awt.image.BufferedImage bacaGambarAman(java.io.InputStream in) {
		if (in == null) {
			return null;
		}
		try {
			java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream(64 * 1024);
			byte[] buf = new byte[8192];
			int read;
			int total = 0;
			while ((read = in.read(buf)) != -1) {
				total += read;
				if (total > MAKS_BYTE_GAMBAR) {
					return null; // stream terlalu besar -> tolak (cegah OOM buffer)
				}
				bout.write(buf, 0, read);
			}
			return bacaGambarAman(bout.toByteArray());
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * Cek apakah {@code file} gambar valid — TANPA men-decode raster penuh.
	 *
	 * <p><b>Kenapa diubah.</b> Versi lama memakai {@code ImageIO.read(...)} yang men-DECODE
	 * SELURUH piksel hanya untuk validasi — boros memori DAN rawan OOM
	 * {@code Requested array size exceeds VM limit}: gambar rusak/raksasa mendeklarasikan
	 * dimensi besar sehingga JVM mencoba mengalokasikan array raster {@code width*height} di atas
	 * batas VM. Selain itu {@code catch(Exception)} lama TIDAK menangkap {@code OutOfMemoryError}
	 * (subclass {@code Error}) sehingga OOM lolos & menggagalkan request. Kini hanya membaca
	 * dimensi dari header lalu memvalidasi batas megapiksel; oversized -&gt; dianggap BUKAN gambar
	 * valid sehingga pemanggil (ProfileImageUtil/AmbilMedia/CommonMedia) menolak/skip.</p>
	 */
	public static boolean isImage(File file) throws Exception {
		int[] dim = dimensiGambarAman(file);
		if (dim == null) {
			return false;
		}
		long totalPiksel = (long) dim[0] * (long) dim[1];
		if (totalPiksel > MAKS_PIKSEL_GAMBAR) {
			return false;
		}
		return true;
	}



	public static Label displayLoadBar(EventListener eventListener) {
		return LoadBarUtils.displayLoadBar(eventListener);
	}



	public static Label displayLoadBar(Component parent) {
		// Delegasi ke method utama dengan listener null
		return LoadBarUtils.displayLoadBar(parent);
	}



	public static Label displayLoadBar(Component parent, EventListener eventListener) {
		return LoadBarUtils.displayLoadBar(parent, eventListener);
	}



	public static Label displayLoadBar(Component parent, File fileName) {
		return LoadBarUtils.displayLoadBar(parent, fileName);
	}



	public static Label displayLoadBar(Component parent, File file, Component center, Intbox sizedata) {
		return LoadBarUtils.displayLoadBar(parent, file, center, sizedata);
	}



	public static Label displayLoadBar(Component parent, File file, Component center, Intbox sizedata, Intbox maxCol) {
		return LoadBarUtils.displayLoadBar(parent, file, center, sizedata, maxCol);
	}



	public static File getFileLocation(GeneralValueObject obj, String key) {

		return CommonJSONUtil.getFileLocation(obj, key);
	}



	public static File setJSONTemporary(GeneralValueObject obj, String key, JSONObject jsonObject) {
		return CommonJSONUtil.setJSONTemporary(obj, key, jsonObject);
	}



	public static void infoDiuploadOleh(String olehId, String oleh, Component parent) {
		UIClassHelper.infoDiuploadOleh(olehId, oleh, parent);
	}

}
