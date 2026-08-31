package ais.common;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.zip.ZipOutputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.zkoss.calendar.impl.SimpleCalendarEvent;
import org.zkoss.poi.ss.usermodel.DataFormatter;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
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
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;
import org.zkoss.zul.Window;

import ais.action.master.BiodataDosenAction;
import ais.action.master.KonfigurasiTampilanBiodataDosenAction;
import ais.action.master.KonfigurasiTampilanGuruAction;
import ais.action.master.KonfigurasiTampilanPegawaiAction;
import ais.action.master.employ.util.FormBiodataPegawaiUtil;
import ais.action.master.helper.AmbilDataKecamatanBanbox;
import ais.action.master.helper.CetakAlbumWisudaAdminWindow;
import ais.action.master.helper.CetakAlbumWisudaMahasiswaHelper;
import ais.action.master.helper.ChangePasswordWindow;
import ais.action.master.helper.GenerateNoKursiDanNoRegistrasiWindow;
import ais.action.master.helper.GenerateNoKursiWindow;
import ais.action.master.helper.GenerateUndanganWisudaWindow;
import ais.action.master.helper.HistoryStatusMahasiswaUtil;
import ais.action.master.helper.KrsDetailHelper;
import ais.action.master.helper.MainHelper;
import ais.action.master.helper.generic.AngketGuruWindow;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.library.util.LibraryUtil;
import ais.action.master.pmb.statistik.LaporanDaftarUlangMahasiswaBaru;
import ais.action.master.pmb.statistik.LaporanLulusMahasiswaBaru;
import ais.action.master.pmb.statistik.LaporanPendaftarMahasiswaBaru;
import ais.action.master.pmb.statistik.LaporanRekapJenisSeleksiMahasiswaBaru;
import ais.action.master.sekolah.GuruAction;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.report.format1.akademik.LaporanAlbumMahasiswaWisuda;
import ais.action.report.format1.akademik.LaporanAlbumProfileWisuda;
import ais.action.report.format1.akademik.LaporanAngketDosenPerDosenWindow;
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
import ais.database.dao.DaoFactory;
import ais.database.dao.PegawaiDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.AccessedUsers;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.CicilanPembayaranGagal;
import ais.database.model.CommonVO;
import ais.database.model.Dashboard;
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
import ais.database.model.LogHostToHost;
import ais.database.model.LogLogin;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Menu;
import ais.database.model.NilaiHuruf;
import ais.database.model.OrangTua;
import ais.database.model.OrganisasiIntraKampus;
import ais.database.model.ParameterUmum;
import ais.database.model.Pegawai;
import ais.database.model.PembatasanNilaiIPKUntukPengambilanKRS;
import ais.database.model.PendaftaranCutiMahasiswa;
import ais.database.model.PengecualianKknMahasiswa;
import ais.database.model.PengecualianPklMahasiswa;
import ais.database.model.PenugasanDosenMengajar;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Pkl;
import ais.database.model.Program;
import ais.database.model.Propinsi;
import ais.database.model.RencanaTahunAkademik;
import ais.database.model.RoleAccess;
import ais.database.model.Ruang;
import ais.database.model.SocialMediaCommonModel;
import ais.database.model.Staff;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.database.model.StatusPertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.TemplatePerkuliahan;
import ais.database.model.TemplatePerkuliahanDetail;
import ais.database.model.TextBerjalan;
import ais.database.model.Tugas;
import ais.database.model.UserAccess;
import ais.database.model.UserRole;
import ais.database.model.VOPembelajaran;
import ais.database.model.Wilayah;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.beasiswa.MahasiswaBeasiswaPersyaratan;
import ais.database.model.beasiswa.MahasiswaDaftarBeasiswa;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.inventory.Toko;
import ais.database.model.kkn.MahasiswaKknPersyaratan;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.Koperasi;
import ais.database.model.kursus.KomponenDataProdukKursus;
import ais.database.model.kursus.PesertaKursus;
import ais.database.model.library.Anggota;
import ais.database.model.library.HariLiburPerpustakaan;
import ais.database.model.library.Perpustakaan;
import ais.database.model.payroll.LiburNasional;
import ais.database.model.pkl.MahasiswaPklPersyaratan;
import ais.database.model.rab.Pejabat;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.recruitment.CalonPegawai;
import ais.database.model.sekolah.AbsenPiket;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.DetailJenisPenilaian;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.PenugasanGuruMengajar;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.sirkulasisurat.PeminjamSurat;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLType;

/**
 * Pustaka utilitas STATIS terbesar dan paling sentral di ECAMPUS—"kotak peralatan" yang dipakai
 * hampir seluruh Action, helper, JSP, dan API. Berisi ratusan method bantu lintas-domain: format
 * tanggal/angka/uang, manipulasi string, akses pengguna &amp; sesi, konfigurasi sistem, kriptografi
 * ringan (HMAC/hash), I/O berkas &amp; ZIP, koneksi HTTP, pembentukan kriteria Hibernate, hingga
 * pembungkus pesan UI ZK.
 *
 * <h3>Untuk apa class ini</h3>
 * Daripada menyebar fungsi-fungsi umum ke banyak tempat (dan menduplikasinya), seluruh logika
 * "sering dipakai, tidak spesifik satu modul" dipusatkan di sini. Pemanggil cukup
 * {@code Common.<method>(...)} tanpa instansiasi. Karena begitu banyak kode bergantung padanya,
 * Common adalah salah satu titik paling kritis untuk dijaga stabilitasnya.
 *
 * <h3>Cara menavigasi class sebesar ini</h3>
 * File ini sangat besar (~10.5 ribu baris, ratusan method). Sebagai peta kasar, method-method-nya
 * mengelompok ke beberapa tema:
 * <ul>
 *   <li><b>Tanggal &amp; waktu</b> — pemformatan/parse ({@code dateFormat*}), perhitungan selisih,
 *   nama hari/bulan dalam bahasa Indonesia.</li>
 *   <li><b>Angka &amp; uang</b> — format ribuan, terbilang, pembulatan, parsing aman.</li>
 *   <li><b>String</b> — potong, bersihkan, validasi ({@code isNumber}, dll.), encode/escape.</li>
 *   <li><b>Pengguna &amp; sesi</b> — {@code getCurrentUser}, akses {@link HttpSession}/cookie,
 *   peran/hak akses, konteks PMB (login via cookie).</li>
 *   <li><b>Konfigurasi</b> — {@code getKonfigurasi(...)} membaca entitas {@code Konfigurasi}
 *   (sering ber-cache), pembacaan properti, path aplikasi ({@code getPath}, {@code ROOT}).</li>
 *   <li><b>Hibernate</b> — pembentukan {@link Criteria}, eksekusi query, helper introspeksi
 *   properti.</li>
 *   <li><b>I/O &amp; jaringan</b> — baca/tulis berkas, ZIP, koneksi HTTP/URL, BLOB.</li>
 *   <li><b>Keamanan</b> — HMAC ({@link Mac}/{@link SecretKeySpec}), hashing, encode.</li>
 *   <li><b>UI ZK</b> — pembungkus messagebox/notifikasi, util tampilan.</li>
 * </ul>
 *
 * <h3>Konvensi penting</h3>
 * <ul>
 *   <li><b>Multi-tenant.</b> Banyak nilai (path, identitas instansi) berasal dari konfigurasi/konteks;
 *   JANGAN hardcode nama instansi/tenant—ambil lewat method Common yang sesuai.</li>
 *   <li><b>Defensif.</b> Mayoritas method menelan/menangani exception dan mengembalikan nilai aman
 *   (string kosong, null, atau default) agar kegagalan util tidak menjatuhkan alur bisnis.</li>
 *   <li><b>Session Hibernate.</b> Yang membuka {@code openSession()} menutupnya di {@code finally};
 *   yang memakai {@code currentSession()} tidak menutup manual (lihat {@code HibernateUtil}).</li>
 *   <li><b>Kompatibilitas.</b> Java 1.6/1.7 + ZK 5.5 (tanpa lambda/stream/diamond).</li>
 * </ul>
 *
 * <h3>Pemeliharaan &amp; dokumentasi bertahap</h3>
 * Karena ukurannya, dokumentasi Javadoc per-method pada class ini dilengkapi BERTAHAP per sub-batch
 * (lihat catatan program dokumentasi). Saat menambah method baru di sini, ikuti gaya Javadoc yang
 * sama (Tujuan / Cara kerja / Parameter / Return / Error / Pemeliharaan) dan tempatkan pada
 * kelompok tema yang relevan. Pertimbangkan: bila sebuah method tumbuh menjadi domain tersendiri,
 * pisahkan ke util khusus agar Common tidak terus membengkak.
 */
public class Common {

	private static final Logger log = Logger.getLogger(Common.class);

	public static final String KONFIG_PMB_LOGIN_COOKIE = "pmb_login_logout_menggunakan_cookie";
	private static final String COOKIE_PMB_BIODATA = "biodataCalonMahasiswa";
	private static final String COOKIE_PMB_USERID = "userid";

	public static String ROOT = "/ais";
	public static String REAL_PATH = "";
	public static String REAL_PATH_REPORT_TEMP = "";
	private static String REAL_PATH_REPORT_REAL = "";
	private static String directoryReportBersama = null;

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

	/**
	 * Menutup session yang dibuat manual melalui openSession() atau
	 * currentNativeSession(). Jangan digunakan untuk currentSession(), karena
	 * lifecycle-nya dikelola otomatis. Dibuat terpusat agar action class tidak
	 * perlu mengulang clear/disconnect/close.
	 */
	public static void closeOpenedSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (!session.isOpen()) {
				return;
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(session status gagal dibaca) src/ais/common/Common.java:closeOpenedSession");
			return;
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:428");
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:432");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:436");
		}
	}

	/**
	 * Rollback aman untuk action class yang memakai transaction manual.
	 */
	public static void rollbackQuietly(Transaction transaction) {
		try {
			if (transaction != null && transaction.isActive()) {
				transaction.rollback();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:455");
		}
	}

	/**
	 * Menutup session manual dengan aman tanpa melempar exception baru.
	 * Cocok dipakai untuk session hasil openSession() atau currentNativeSession().
	 */
	public static void closeNativeSessionQuietly(Session session) {
		try {
			// Panggilan bisa balapan dengan reInitProgram/lifecycle request normal yang sudah
			// menutup session native ini lebih dulu -- clear()/close() pada session yang sudah
			// closed melempar SessionException ("Session is closed!"/"already closed"). Cek
			// isOpen() dulu supaya method ini benar-benar no-op bila session sudah tertutup,
			// bukan cuma mengandalkan catch di bawah untuk meredam exception-nya.
			if (session != null && session.isOpen()) {
				try {
					session.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:468");
				}
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:472");
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:476");
				}
			}
		} finally {
			try {
				HibernateUtil.closeSession();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:482");
			}
		}
	}


	public static boolean pakaiDirReportTergabung() {
		return directoryReportBersama != null && !directoryReportBersama.trim().isEmpty();
	}

	public static synchronized String ambilREAL_PATH_REPORT() {
		if (isBlank(REAL_PATH_REPORT_REAL)) {
			REAL_PATH_REPORT_REAL = safeTrim(REAL_PATH_REPORT_TEMP);
		}

		try {
			if (directoryReportBersama == null) {
				Konfigurasi konfigurasi = Common.getKonfigurasi("directory_report_bersama", "");
				directoryReportBersama = konfigurasi == null ? "" : safeTrim(konfigurasi.getNilai());
			}

			if (!isBlank(directoryReportBersama)) {
				REAL_PATH_REPORT_REAL = directoryReportBersama;
				File reportDirectory = new File(REAL_PATH_REPORT_REAL);
				if (!ensureDirectory(reportDirectory)) {
					log.warn("Folder directory_report_bersama tidak valid atau tidak dapat dibuat: "
							+ REAL_PATH_REPORT_REAL);
				}
			}
		} catch (Exception e) {
			log.warn("Gagal mengambil konfigurasi directory_report_bersama", e);
		}

		return safeTrim(REAL_PATH_REPORT_REAL);
	}

	public static String formatNumber(Double totalNilai, int fa) {
		return numberFormat.get().format(totalNilai == null ? Double.valueOf(0) : totalNilai);
	}

	public static String CURRENT_URL = "";
	public static String CURRENT_URL_SIMPLE = "";
	// public static String CURRENT_LOCAL_URL = "";

	public static File folder = null;

	public static synchronized void initTemp() {
		if (folder != null && folder.exists() && folder.isDirectory()) {
			return;
		}

		folder = null;
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi("lokasi_penyimpanan_file_data", "/backup2/backup_file");
			String fileLokasi = konfigurasi == null ? "" : safeTrim(konfigurasi.getNilai());
			if (isBlank(fileLokasi)) {
				return;
			}

			File baseDirectory = new File(fileLokasi);
			if (!baseDirectory.exists() || !baseDirectory.isDirectory()) {
				log.warn("Folder lokasi_penyimpanan_file_data tidak ditemukan atau bukan folder: " + fileLokasi);
				return;
			}

			File targetDirectory = new File(baseDirectory.getAbsolutePath() + safeTrim(Common.REAL_PATH));
			if (ensureDirectory(targetDirectory)) {
				folder = targetDirectory;
			} else {
				log.warn("Folder temporary tidak dapat dibuat: " + targetDirectory.getAbsolutePath());
			}
		} catch (Exception e) {
			log.warn("Gagal inisialisasi folder temporary", e);
			folder = null;
		}
	}

	public static int MAX_RESULT_BIG = 15000;

	// 1. OPTIMASI MEMORI: Buat satu instance Locale saja untuk dipakai bersama
	public static final Locale LOCALE_ID = new Locale("in", "ID");
	public static final String ISO_8601_24H_FULL_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

	// 2. HELPER METHOD: Agar kode tetap pendek dan rapi
	private static ThreadLocal<SimpleDateFormat> createSDF(final String pattern, final Locale locale) {
		return new ThreadLocal<SimpleDateFormat>() {
			@Override
			protected SimpleDateFormat initialValue() {
				if (locale == null) {
					return new SimpleDateFormat(pattern);
				}
				return new SimpleDateFormat(pattern, locale);
			}
		};
	}

	// 3. DEKLARASI THREAD-SAFE FORMATTER (Menggunakan Helper di atas)
	public static final ThreadLocal<SimpleDateFormat> timeFormat = createSDF("HH:mm", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> timeFormatMinute = createSDF("mm", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> timeFormat2 = createSDF("HH.mm", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> datetimeFormat1s = createSDF("ddMMyyHHmmss", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> datetimeFormat2s = createSDF("yyMMddHHmmss", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> datetimeFormat3s = createSDF("yyMMddHHmm", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> timeFormat1 = createSDF("HH:mm:ss", LOCALE_ID);

	public static final ThreadLocal<SimpleDateFormat> parseFormatTime = createSDF("hh:mm:ss a", null);

	public static final ThreadLocal<SimpleDateFormat> dateFormatTahun = createSDF("yyyy", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat1 = createSDF("dd-MM-yyyy", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat11 = createSDF("dd/MM/yy", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat112 = createSDF("dd/MM/yyyy", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> simpleDateFormat = createSDF("yyMM", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> simpleDateFormat1 = createSDF("ddMM", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> simpleDateFormat2 = createSDF("dd/MM", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat = createSDF("dd-MM-yyyy HH:mm", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormatInput = createSDF("yyyy-MM-dd'T'HH:mm", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormatInput1 = createSDF("yyyy-MM-dd'T'HH:mm:s", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat53 = createSDF("dd-MM-yy HH:mm", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat3 = createSDF("dd-MM-yyyy HH:mm:ss", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat31 = createSDF("dd/MM/yy HH:mm:ss", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat32 = createSDF("dd-MM-yyyy_HH.mm.ss", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat33 = createSDF("dd/MM/yy HH:mm", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat4 = createSDF("EEEEE, dd-MM-yyyy", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat41 = createSDF("EEEEE, dd-MM-yy", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat8 = createSDF("yyyyMMdd", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat83 = createSDF("yyMMdd", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat84 = createSDF("yyMMddHHmmss", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat82 = createSDF("yyyyMM", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat81 = createSDF("ddMMyyyy", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat85 = createSDF("ddMMyy", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormatWeek = createSDF("YYYY-ww", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat9 = createSDF("yyyyMMddHHmmss", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat4Week = createSDF("EEEEE", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat5 = createSDF("EEEEE, dd-MM-yyyy HH:mm:ss", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat51 = createSDF("EEEEE, dd-MM-yyyy HH:mm", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormatHari = createSDF("EEEEE", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormatTgl = createSDF("dd", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormatBln = createSDF("MMMMM", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormatThn = createSDF("yyyy", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat7 = createSDF("EEEEE, dd-MM-yyyy HH:mm:ss.SS",
			LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat6 = createSDF("EEEEE, dd MMMMM yyyy", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat62 = createSDF("EEEEE_dd_MMMMM_yyyy_HH_mm_ss",
			LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat61 = createSDF("EEEEE, dd MMMMM yyyy HH:mm:ss",
			LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> dateFormat2 = createSDF("dd MMMMM yyyy", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> monthFormat2 = createSDF("MMMMM", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> monthFormat21 = createSDF("MMM", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> databaseDateFormat = createSDF("yyyy-MM-dd", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> databaseDateFormat11 = createSDF("yyyy/MM/dd", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> databaseDateFormat1 = createSDF("yyyy-MM-dd HH:mm:ss", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> databaseDateFormat2 = createSDF("yyyy-MM-dd HH:mm", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> iso8601 = createSDF(ISO_8601_24H_FULL_FORMAT, null);
	public static final ThreadLocal<SimpleDateFormat> importDateFormat = createSDF("dd/MM/yyyy", LOCALE_ID);
	public static final ThreadLocal<SimpleDateFormat> formatTahunTanggal = createSDF("yyyy-DDD", null);

	public static final ThreadLocal<SimpleDateFormat> dateFormat2En = createSDF("dd MMMMM yyyy", Locale.ENGLISH);
	public static final ThreadLocal<SimpleDateFormat> monthFormat2En = createSDF("MMMMM", Locale.ENGLISH);

	// 4. NUMBER FORMAT KHUSUS ENGLISH (Dengan setting maximum fraction 2)
	public static final ThreadLocal<NumberFormat> numberFormatEn = new ThreadLocal<NumberFormat>() {
		@Override
		protected NumberFormat initialValue() {
			NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
			nf.setMaximumFractionDigits(2); // Setting dilakukan di sini
			return nf;
		}
	};

	public static final ThreadLocal<NumberFormat> numberFormat = new ThreadLocal<NumberFormat>() {
		@Override
		protected NumberFormat initialValue() {
			NumberFormat nf = NumberFormat.getNumberInstance(new Locale("in", "ID"));
			nf.setMaximumFractionDigits(3); // Setting di sini
			return nf;
		}
	};

	public static final ThreadLocal<NumberFormat> numberFormat1 = new ThreadLocal<NumberFormat>() {
		@Override
		protected NumberFormat initialValue() {
			NumberFormat nf = NumberFormat.getNumberInstance(new Locale("in", "ID"));
			nf.setMaximumFractionDigits(0); // Setting di sini
			return nf;
		}
	};

	public static final ThreadLocal<NumberFormat> numberFormat2 = new ThreadLocal<NumberFormat>() {
		@Override
		protected NumberFormat initialValue() {
			NumberFormat nf = NumberFormat.getNumberInstance(new Locale("in", "ID"));
			nf.setMaximumFractionDigits(2); // Setting di sini
			return nf;
		}
	};

	public static int MAX_RESULT = 100;
	public static int MAX_RESULT_100 = 100;
	public static int MAX_RESULT_50 = 50;
	public static int MAX_RESULT_20 = 20;
	// public static int MAX_RESULT_300 = 300;
	public static int MAX_RESULT_500 = 500;
	public static int MAX_RESULT_1000 = 1000;
	public static String ROOT_UPLOAD;
	public static int ROWS_COUNT_ON_PAGE = 10;
	public static int ROWS_COUNT_ON_PAGE_5 = 5;
	public static int ROWS_COUNT_ON_PAGE_1 = 1;
	public static int ROWS_COUNT_ON_PAGE_50 = 50;
	public static int ROWS_COUNT_ON_PAGE_25 = 25;
	public static int ROWS_COUNT_ON_PAGE_100 = 100;
	public static int ROWS_COUNT_ON_PAGE_10 = 10;
	public static int ROWS_COUNT_ON_PAGE_15 = 15;

	public static final String DES_PASS_PHRASE = "AIS_UIN";

	// Ubah menjadi ThreadLocal
	public static final ThreadLocal<DesEncrypter> desEncrypter = new ThreadLocal<DesEncrypter>() {
		@Override
		protected DesEncrypter initialValue() {
			return new DesEncrypter(DES_PASS_PHRASE);
		}
	};

	/**
	 * Bersihkan ThreadLocal "berat" milik thread saat ini. Khususnya {@link #desEncrypter}
	 * yang nilainya class webapp ({@code DesEncrypter}); bila tidak di-remove pada thread
	 * latar berumur panjang (timer/scheduler), Tomcat memperingatkan kebocoran classloader
	 * saat webapp di-stop/redeploy. Dipanggil dari thread latar (UserOnlineCounter,
	 * SessionCounter) di akhir kerja. ThreadLocal SimpleDateFormat/NumberFormat bernilai
	 * class JDK sehingga tidak menyandera classloader (numberFormat ikut dibersihkan murah).
	 */
	public static void bersihkanThreadLocalThreadIni() {
		try { desEncrypter.remove(); } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/Common.java:715"); }
		try { numberFormat.remove(); } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/Common.java:716"); }
		try { numberFormat1.remove(); } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/Common.java:717"); }
		try { numberFormat2.remove(); } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/Common.java:718"); }
		try { numberFormatEn.remove(); } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/Common.java:719"); }
	}

	public static Integer[] ganjil = { 1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23 };
	public static Integer[] genap = { 0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22 };

	public static String ROMAWI[] = { "0", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII",
			"XIII", "XIV", "XV", "XVI", "XVII" };

	public static String ROMAWI_TANPA_NOL[] = { "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI",
			"XII", "XIII", "XIV", "XV", "XVI", "XVII" };

	public static String ALPABED[] = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P",
			"Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "AA", "AB", "AC", "AD", "AE", "AF", "AG", "AH", "AI",
			"AJ", "AK", "AL", "AM", "AN", "AO", "AP", "AQ", "AR", "AS", "AT", "AU", "AV", "AW", "AX", "AY", "AZ", "BA",
			"BB", "BC", "BD", "BE", "BF", "BG", "BH", "BI", "BJ", "BK", "BL", "BM", "BN", "BO", "BP", "BQ", "BR", "BS",
			"BT", "BU", "BV", "BW", "BX", "BY", "BZ", "CA", "CB", "CC", "CD", "CE", "CF", "CG", "CH", "CI", "CJ", "CK",
			"CL", "CM", "CN", "CO", "CP", "CQ", "CR", "CS", "CT", "CU", "CV", "CW", "CX", "CY", "CZ", "DA", "DB", "DC",
			"DD", "DE", "DF", "DG", "DH", "DI", "DJ", "DK", "DL", "DM", "DN", "DO", "DP", "DQ", "DR", "DS", "DT", "DU",
			"DV", "DW", "DX", "DY", "DZ", "EA", "EB", "EC", "ED", "EE", "EF", "EG", "EH", "EI", "EJ", "EK", "EL", "EM",
			"EN", "EO", "EP", "EQ", "ER", "ES", "ET", "EU", "EV", "EW", "EX", "EY", "EZ", "FA", "FB", "FC", "FD", "FE",
			"FF", "FG", "FH", "FI", "FJ", "FK", "FL", "FM", "FN", "FO", "FP", "FQ", "FR", "FS", "FT", "FU", "FV", "FW",
			"FX", "FY", "FZ", "GA", "GB", "GC", "GD", "GE", "GF", "GG", "GH", "GI", "GJ", "GK", "GL", "GM", "GN", "GO",
			"GP", "GQ", "GR", "GS", "GT", "GU", "GV", "GW", "GX", "GY", "GZ" };

	public static String BULAN[] = { "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus",
			"September", "Oktober", "November", "Desember" };

	public static Long increments = 0L;

	public static String capitailizeWord(String str) {
		if (str == null || str.length() == 0) {
			return "";
		}

		StringBuilder builder = new StringBuilder(str.length());
		char previous = ' ';
		for (int i = 0; i < str.length(); i++) {
			char current = str.charAt(i);
			if (previous == ' ' && current != ' ') {
				builder.append(Character.toUpperCase(current));
			} else {
				builder.append(Character.toLowerCase(current));
			}
			previous = current;
		}

		return builder.toString().trim();
	}

	public static String getDeskripsiPerkuliahan(Perkuliahan perkuliahan) {
		return CommonUiFactoryHelper.getDeskripsiPerkuliahan(perkuliahan);
	}

	public static boolean isSecure(HttpServletRequest request) {
		return CommonCurrentSessionHelper.isSecure(request);
	}

	public static Box getDeskripsiPerkuliahanHbox(Perkuliahan perkuliahan) throws Exception {
		return CommonUiFactoryHelper.getDeskripsiPerkuliahanHbox(perkuliahan);
	}

	public static Box getDeskripsiPerkuliahanHbox(Perkuliahan perkuliahan, boolean tampilStatistik) throws Exception {
		return CommonUiFactoryHelper.getDeskripsiPerkuliahanHbox(perkuliahan, tampilStatistik);
	}

	public static Box getDeskripsiPerkuliahanHbox(final VOPembelajaran voPembelajaran, final boolean tampilStatistik,
			final boolean horizontal, final Row rowData, final EventListener eventListener, final boolean refresh)
			throws Exception {
		return CommonUiFactoryHelper.getDeskripsiPerkuliahanHbox(voPembelajaran, tampilStatistik, horizontal, rowData,
				eventListener, refresh);
	}


	public static Box getDeskripsiJadwalPelajaranHbox(JadwalPelajaran jadwalPelajaran) throws Exception {
		return CommonUiFactoryHelper.getDeskripsiJadwalPelajaranHbox(jadwalPelajaran);
	}

	public static Box getDeskripsiJadwalPelajaranHbox(JadwalPelajaran jadwalPelajaran, boolean tampilStatistik)
			throws Exception {
		return CommonUiFactoryHelper.getDeskripsiJadwalPelajaranHbox(jadwalPelajaran, tampilStatistik);
	}

	public static Box getDeskripsiJadwalPelajaranHbox(final JadwalPelajaran jadwalPelajaran,
            final boolean tampilStatistik, final boolean horizontal, final Row rowData) throws Exception {
        return CommonUiFactoryHelper.getDeskripsiJadwalPelajaranHbox(jadwalPelajaran, tampilStatistik, horizontal,
                rowData);
    }

	public static void initPaging(Paging paging, EventListener eventListener) {
		CommonPagingHelper.initPaging(paging, eventListener);
	}

	public static void initPaging(Criteria criteria, Paging paging) {
		CommonPagingHelper.initPaging(criteria, paging);
	}

	public static void initPaging(Criteria criteria, final Paging paging, final EventListener eventListener) {
		CommonPagingHelper.initPaging(criteria, paging, eventListener);
	}

	public static void initPaging(Criteria criteria, final Paging paging, final EventListener eventListener,
			Projection p) {
		CommonPagingHelper.initPaging(criteria, paging, eventListener, p);
	}

	public static void initPaging5(Paging paging, EventListener eventListener) {
		CommonPagingHelper.initPaging5(paging, eventListener);
	}

	public static void initPaging5(Criteria criteria, Paging paging) {
		CommonPagingHelper.initPaging5(criteria, paging);
	}

	public static void initPaging5(Criteria criteria, Paging paging, EventListener eventListener) {
		CommonPagingHelper.initPaging5(criteria, paging, eventListener);
	}

	public static void initPagingCustom(Paging paging, EventListener eventListener, int displayPerPage) {
		CommonPagingHelper.initPagingCustom(paging, eventListener, displayPerPage);
	}

	public static void initPagingCustom(Criteria criteria, Paging paging, int displayPerPage) {
		CommonPagingHelper.initPagingCustom(criteria, paging, displayPerPage);
	}

	public static void initPagingCustom(Criteria criteria, Paging paging, EventListener eventListener,
			int displayPerPage) {
		CommonPagingHelper.initPagingCustom(criteria, paging, eventListener, displayPerPage);
	}

	public static void initPaging1(Paging paging, EventListener eventListener) {
		CommonPagingHelper.initPaging1(paging, eventListener);
	}

	public static void initPaging1(Criteria criteria, Paging paging) {
		CommonPagingHelper.initPaging1(criteria, paging);
	}

	public static void initPaging1(Criteria criteria, Paging paging, EventListener eventListener) {
		CommonPagingHelper.initPaging1(criteria, paging, eventListener);
	}

	public static void initPaging15(Paging paging, EventListener eventListener) {
		CommonPagingHelper.initPaging15(paging, eventListener);
	}

	public static void initPaging15(Criteria criteria, Paging paging) {
		CommonPagingHelper.initPaging15(criteria, paging);
	}

	public static void initPaging15(Criteria criteria, Paging paging, EventListener eventListener) {
		CommonPagingHelper.initPaging15(criteria, paging, eventListener);
	}

	public static void initPaging10(Paging paging, EventListener eventListener) {
		CommonPagingHelper.initPaging10(paging, eventListener);
	}

	public static void initPaging10(Criteria criteria, Paging paging) {
		CommonPagingHelper.initPaging10(criteria, paging);
	}

	public static void initPaging10(Criteria criteria, Paging paging, EventListener eventListener) {
		CommonPagingHelper.initPaging10(criteria, paging, eventListener);
	}

	public static void initPaging100(Paging paging, EventListener eventListener) {
		CommonPagingHelper.initPaging100(paging, eventListener);
	}

	public static void initPaging100(Criteria criteria, Paging paging) {
		CommonPagingHelper.initPaging100(criteria, paging);
	}

	public static void initPaging100(Criteria criteria, Paging paging, EventListener eventListener) {
		CommonPagingHelper.initPaging100(criteria, paging, eventListener);
	}

	public static void initPaging50(Paging paging, EventListener eventListener) {
		CommonPagingHelper.initPaging50(paging, eventListener);
	}

	public static void initPaging50(Criteria criteria, Paging paging) {
		CommonPagingHelper.initPaging50(criteria, paging);
	}

	public static void initPaging50(Criteria criteria, Paging paging, EventListener eventListener) {
		CommonPagingHelper.initPaging50(criteria, paging, eventListener);
	}

	public static void initPaging25(Paging paging, EventListener eventListener) {
		CommonPagingHelper.initPaging25(paging, eventListener);
	}

	public static void initPaging25(Criteria criteria, Paging paging) {
		CommonPagingHelper.initPaging25(criteria, paging);
	}

	public static void initPaging25(Criteria criteria, Paging paging, EventListener eventListener) {
		CommonPagingHelper.initPaging25(criteria, paging, eventListener);
	}

	public static void initPaging25(Criteria criteria, Paging paging, EventListener eventListener,
			Projection projection) {
		CommonPagingHelper.initPaging25(criteria, paging, eventListener, projection);
	}

	@SuppressWarnings("unchecked")
	public static void sisipkanSemuaDiCombo(Combobox combobox, Object defaultValue) {
		List<Component> components = combobox.getChildren();
		boolean ada = false;
		Comboitem comboitem = new Comboitem("Semua");
		for (Component component : components) {
			if (component instanceof Comboitem) {
				Comboitem item = (Comboitem) component;
				if (item.getLabel() != null && item.getLabel().trim().equalsIgnoreCase("Semua")) {
					ada = true;
					comboitem = item;
					break;
				}
			}
		}

		if (!ada) {
			comboitem.setValue(defaultValue);
			combobox.appendChild(comboitem);
		}
		if (combobox.getSelectedItem() == null) {
			combobox.setSelectedItem(comboitem);
		}
	}

	public static void reloadJenisKegiatans() {
		CommonHelperClass.reloadJenisKegiatans();
	}

	public static Matakuliah[] getMatakuliahApakahEkivalen(Matakuliah matakuliah, String nim, boolean refresh) {
		return CommonHelperClass.getMatakuliahApakahEkivalen(matakuliah, nim, refresh);
	}

	public static void reloadJenisKegiatans(Session session) {
		CommonHelperClass.reloadJenisKegiatans(session);
	}

	public static Combobox initJenisPembayaranMahasiswa(Combobox jenisPembayaranMahasiswa) {
		return CommonComboLanguageHelper.initJenisPembayaranMahasiswa(jenisPembayaranMahasiswa);
	}

	public static Combobox initJenisPembayaranBiodataCalonMahasiswa(Combobox jenisPembayaranMahasiswa) {
		return CommonComboLanguageHelper.initJenisPembayaranBiodataCalonMahasiswa(jenisPembayaranMahasiswa);
	}

	public static Combobox initJenisPembayaranMahasiswaDanBiodataCalonMahasiswa(Combobox jenisPembayaranMahasiswa) {
		return CommonComboLanguageHelper.initJenisPembayaranMahasiswaDanBiodataCalonMahasiswa(jenisPembayaranMahasiswa);
	}

	public static Combobox initJenisSemester(Combobox jenisSemester) {
		return CommonComboLanguageHelper.initJenisSemester(jenisSemester);
	}

	public static Combobox initJenisSemester(Combobox jenisSemester, boolean sp) {
		return CommonComboLanguageHelper.initJenisSemester(jenisSemester, sp);
	}

	public static String getSemesterString() {
		return CommonCurrentSessionHelper.getSemesterString();
	}

	public static TreeMap<Integer, String[]> generateStatusSemester(Mahasiswa mahasiswa) {
		return CommonComboLanguageHelper.generateStatusSemester(mahasiswa);
	}

	public static List<String[]> generateSemestersForGrid(Mahasiswa mahasiswa, int mulai, int sampai,
			Integer semesterPendek) {
		return CommonComboLanguageHelper.generateSemestersForGrid(mahasiswa, mulai, sampai, semesterPendek);
	}

	public static List<String[]> generateSemestersForGridTahapan(Mahasiswa mahasiswa, int jumlahTahapan) {
		return CommonComboLanguageHelper.generateSemestersForGridTahapan(mahasiswa, jumlahTahapan);
	}

	public static Combobox createComboKonfigurasi(Combobox conf) {
		return CommonComboLanguageHelper.createComboKonfigurasi(conf);
	}

	public static Boolean checkApakahDosenBolehMenilai(Dosen dosen, Tbmuser tbmuser, String tahunAkademik,
			String jenisSemester) {
		return CommonHelperClass.checkApakahDosenBolehMenilai(dosen, tbmuser, tahunAkademik, jenisSemester);
	}

	public static Boolean checkApakahMahasiswaBolehAmbilKrsLewatPengecualian(Mahasiswa mahasiswa, String tahunAkademik,
			String jenisSemester) {
		return CommonHelperClass.checkApakahMahasiswaBolehAmbilKrsLewatPengecualian(mahasiswa, tahunAkademik,
				jenisSemester);
	}

	public static Boolean checkUsername(String username, String userId, Long id) {
		return CommonValidationHelper.checkUsername(username, userId, id);
	}

	public static void initFakultasDanJurusan(final Combobox fakultas, final Combobox jurusan,
			final Combobox searchfakultas, final Combobox searchjurusan) {
		CommonComboLanguageHelper.initFakultasDanJurusan(fakultas, jurusan, searchfakultas, searchjurusan);
	}

	public static void initFakultasDanJurusanDanSemua(Combobox fakultas, Combobox jurusan) {
		CommonComboLanguageHelper.initFakultasDanJurusanDanSemua(fakultas, jurusan);
	}

	public static void initFakultasDanJurusanDanSemua(Combobox fakultas, Combobox jurusan, Combobox searchfakultas,
			Combobox searchjurusan) {
		CommonComboLanguageHelper.initFakultasDanJurusanDanSemua(fakultas, jurusan, searchfakultas, searchjurusan);
	}

	public static void initFakultasDanJurusanDanSemua(Combobox fakultas, Combobox jurusan, Combobox searchfakultas,
			Combobox searchjurusan, boolean pilih) {
		CommonComboLanguageHelper.initFakultasDanJurusanDanSemua(fakultas, jurusan, searchfakultas, searchjurusan,
				pilih);
	}

	public static void initYayasanDanSekolahDanSemua(Combobox yayasan, Combobox sekolah, Combobox searchyayasan,
			Combobox searchsekolah) {
		CommonComboLanguageHelper.initYayasanDanSekolahDanSemua(yayasan, sekolah, searchyayasan, searchsekolah);
	}

	public static void initYayasanDanSekolahDanSemua(Combobox yayasan, Combobox sekolah, Combobox searchyayasan,
			Combobox searchsekolah, boolean pilih, boolean otomatisPilih) {
		CommonComboLanguageHelper.initYayasanDanSekolahDanSemua(yayasan, sekolah, searchyayasan, searchsekolah, pilih,
				otomatisPilih);
	}

	/**
	 * Memuat daftar Sekolah milik {@code yayasan} ke combo. Dipakai setelah memilih
	 * yayasan SECARA PROGRAMATIS ({@code selectComboItem} tidak memicu onChange), agar
	 * combo Sekolah terisi sekolah-sekolah di bawah yayasan tsb. {@code yayasan} null =
	 * semua sekolah. Lihat {@link ais.common.InitComboUtil#muatSekolahMilikYayasan}.
	 */
	public static void muatSekolahMilikYayasan(Combobox sekolah, ais.database.model.sekolah.Yayasan yayasan) {
		InitComboUtil.muatSekolahMilikYayasan(sekolah, yayasan);
	}

	/**
	 * Memilih sebuah Sekolah pada combo Sekolah dengan JAMINAN combo terisi. Pengganti aman
	 * untuk {@code selectComboItem(sekolah, ...)} pada seluruh form modul sekolah yang memakai
	 * pasangan combo Yayasan + Sekolah.
	 *
	 * <p><b>Masalah yang diperbaiki (bahasa sederhana):</b> saat membuka data lama (edit),
	 * Yayasan dipilih SECARA PROGRAMATIS. {@code selectComboItem} hanya menandai item terpilih
	 * dan TIDAK memicu {@code onChange}, sehingga daftar Sekolah milik yayasan tsb tidak ikut
	 * dimuat — akibatnya combo Sekolah tampil kosong walau Yayasan sudah terpilih. Method ini
	 * memastikan: bila Sekolah yang hendak dipilih BELUM ada sebagai pilihan di combo (mis. combo
	 * kosong atau terisi yayasan lain), daftar Sekolah milik yayasan dari sekolah tsb dimuat lebih
	 * dulu, baru sekolahnya dipilih. Bila sekolah sudah ada sebagai pilihan (combo sudah benar),
	 * cukup dipilih tanpa memuat ulang — sehingga aman untuk combo yang sengaja berisi semua
	 * sekolah.
	 *
	 * <p>Menerima {@code Object} agar bisa langsung menggantikan pemanggilan
	 * {@code selectComboItem(sekolah, ekspresi)} tanpa mengubah ekspresi (yang kadang berupa
	 * ternary). Nilai selain Sekolah (mis. {@code null}) diperlakukan seperti {@code selectComboItem}
	 * biasa. Aman terhadap {@code null} pada combo maupun nilai.
	 *
	 * @param sekolah combobox Sekolah tujuan (boleh null → tidak melakukan apa-apa).
	 * @param nilai Sekolah yang akan dipilih (boleh null / bukan Sekolah).
	 */
	public static void pilihSekolah(Combobox sekolah, Object nilai) {
		if (sekolah == null) {
			return;
		}
		if (nilai instanceof ais.database.model.sekolah.Sekolah) {
			ais.database.model.sekolah.Sekolah sk = (ais.database.model.sekolah.Sekolah) nilai;
			if (sk.getId() != null && sk.getYayasan() != null && !comboBerisiSekolah(sekolah, sk)) {
				InitComboUtil.muatSekolahMilikYayasan(sekolah, sk.getYayasan());
			}
		}
		selectComboItem(sekolah, nilai);
	}

	/** Apakah combo sudah memuat Sekolah dengan id yang sama (sehingga bisa langsung dipilih)? */
	private static boolean comboBerisiSekolah(Combobox combo, ais.database.model.sekolah.Sekolah sk) {
		if (combo == null || sk == null || sk.getId() == null) {
			return false;
		}
		try {
			for (Object o : combo.getItems()) {
				org.zkoss.zul.Comboitem item = (org.zkoss.zul.Comboitem) o;
				if (item != null && item.getValue() instanceof ais.database.model.sekolah.Sekolah
						&& sk.getId().equals(((ais.database.model.sekolah.Sekolah) item.getValue()).getId())) {
					return true;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:1110");
		}
		return false;
	}

	/**
	 * Memuat daftar Jurusan milik {@code fakultas} ke combo (analog {@link #muatSekolahMilikYayasan}
	 * untuk perguruan tinggi). {@code fakultas} null = semua jurusan.
	 * Lihat {@link ais.common.InitComboUtil#muatJurusanMilikFakultas}.
	 */
	public static void muatJurusanMilikFakultas(Combobox jurusan, ais.database.model.Fakultas fakultas) {
		InitComboUtil.muatJurusanMilikFakultas(jurusan, fakultas);
	}

	/**
	 * Memilih sebuah Jurusan pada combo Jurusan dengan JAMINAN combo terisi. Pengganti aman untuk
	 * {@code selectComboItem(jurusan, ...)} pada seluruh form yang memakai pasangan combo
	 * Fakultas + Jurusan (analog {@link #pilihSekolah} untuk perguruan tinggi).
	 *
	 * <p><b>Masalah yang diperbaiki:</b> saat membuka data lama (edit), Fakultas dipilih SECARA
	 * PROGRAMATIS. {@code selectComboItem} tidak memicu {@code onChange}, sehingga daftar Jurusan
	 * milik fakultas tsb tidak ikut dimuat dan combo Jurusan tampil kosong. Method ini memastikan
	 * bila Jurusan yang hendak dipilih BELUM ada sebagai pilihan di combo, daftar Jurusan milik
	 * fakultas dari jurusan tsb dimuat lebih dulu, baru jurusannya dipilih. Bila sudah ada, cukup
	 * dipilih tanpa memuat ulang (aman untuk combo yang sengaja berisi semua jurusan).
	 *
	 * @param jurusan combobox Jurusan tujuan (boleh null → tidak melakukan apa-apa).
	 * @param nilai Jurusan yang akan dipilih (boleh null / bukan Jurusan).
	 */
	public static void pilihJurusan(Combobox jurusan, Object nilai) {
		if (jurusan == null) {
			return;
		}
		if (nilai instanceof Jurusan) {
			Jurusan jr = (Jurusan) nilai;
			if (jr.getId() != null && jr.getFakultas() != null && !comboBerisiJurusan(jurusan, jr)) {
				InitComboUtil.muatJurusanMilikFakultas(jurusan, jr.getFakultas());
			}
		}
		selectComboItem(jurusan, nilai);
	}

	/** Apakah combo sudah memuat Jurusan dengan id yang sama (sehingga bisa langsung dipilih)? */
	private static boolean comboBerisiJurusan(Combobox combo, Jurusan jr) {
		if (combo == null || jr == null || jr.getId() == null) {
			return false;
		}
		try {
			for (Object o : combo.getItems()) {
				org.zkoss.zul.Comboitem item = (org.zkoss.zul.Comboitem) o;
				if (item != null && item.getValue() instanceof Jurusan
						&& jr.getId().equals(((Jurusan) item.getValue()).getId())) {
					return true;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:1165");
		}
		return false;
	}

	public static void initJurusanDanSemua(Combobox jurusan, Jenjang jenjang) {
		CommonComboLanguageHelper.initJurusanDanSemua(jurusan, jenjang);
	}

	public static void initJurusanDanSemua(Combobox jurusan, Jenjang jenjang, String label) {
		CommonComboLanguageHelper.initJurusanDanSemua(jurusan, jenjang, label);
	}

	public static void freeze(Component comp, boolean freeze) {
		InitComboUtil.freeze(comp, freeze);
	}

	public static void freezeGanti(Component comp, boolean freeze) {
		InitComboUtil.freezeGanti(comp, freeze);
	}

	public static void freezeGanti(Component... components) {
		InitComboUtil.freezeGanti(components);
	}

	public static void masukkanListener(Component comp, EventListener eventListener) {
		InitComboUtil.masukkanListener(comp, eventListener);
	}

	public static Combobox createComboJenisPembayaran(Combobox jenisPembayaran) {
		return CommonComboLanguageHelper.createComboJenisPembayaran(jenisPembayaran);
	}

	public static Combobox createComboJenisPembayaranDanSemua(Combobox jenisPembayaran) {
		return CommonComboLanguageHelper.createComboJenisPembayaranDanSemua(jenisPembayaran);
	}

	public static LangUtil initLaguage() {
		return CommonComboLanguageHelper.initLaguage();
	}

	public static String getBahasaConfig(String defaultBahasa) {
		if (HeadlessActionContext.isActive()) return defaultBahasa;
		return CommonComboLanguageHelper.getBahasaConfig(defaultBahasa);
	}

	public static String getBahasaConfig(String prefix, String defaultBahasa) {
		if (HeadlessActionContext.isActive()) return defaultBahasa;
		return CommonComboLanguageHelper.getBahasaConfig(prefix, defaultBahasa);
	}

	/** Versi escape untuk embedding di dalam string JS (single-quote context).
	 *  Aman dari: broken string ('), premature </script>, backslash injection. */
	public static String getBahasaConfigJS(String defaultBahasa) {
		return getBahasaConfig(defaultBahasa)
				.replace("\\", "\\\\")
				.replace("'", "\\'")
				.replace("</", "<\\/");
	}

	/** Versi escape untuk embedding di dalam string JS (double-quote context / out.print). */
	public static String getBahasaConfigJSQ(String defaultBahasa) {
		return getBahasaConfig(defaultBahasa)
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("'", "\\'")
				.replace("</", "<\\/");
	}

	/** Escape string Java sembarang agar aman di dalam string JS single-quote.
	 *  Gunakan untuk variabel dinamis (bukan kunci bahasa statis). */
	public static String jsEscape(String value) {
		if (value == null) return "";
		return value
				.replace("\\", "\\\\")
				.replace("'", "\\'")
				.replace("</", "<\\/");
	}

	public static String currentLang() {
		String currentLang = null;
		try {
			// Dipanggil juga dari render awal index.jsp/login2.jsp untuk pengunjung anonim, dan dari
			// thread latar belakang (laporan/notifikasi) tanpa Execution ZK yang mapan.
			// Sessions.getCurrent(true) ternyata bisa NPE (bukan sekadar mengembalikan null) bila
			// dipanggil TANPA Execution aktif, karena implementasinya mengakses Execution.getDesktop()
			// secara internal. Cek Executions.getCurrent() != null lebih dulu (pola aman non-throwing
			// yang sudah dipakai di banyak tempat lain di codebase ini) agar Sessions.getCurrent(true)
			// hanya dipanggil ketika benar-benar ada Execution; fallback ke bahasa default tetap jalan
			// seperti biasa saat tidak ada Execution/sesi.
			if (Executions.getCurrent() != null) {
				org.zkoss.zk.ui.Session currentSession = Sessions.getCurrent(true);
				if (currentSession != null) {
					currentLang = (String) currentSession.getAttribute("current_lang");
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:1218");

		}

		// Konteks JSP MURNI (mis. modul PMB/PSB) TIDAK punya ZK Execution, sehingga blok di atas
		// tak pernah mengembalikan nilai -> bahasa "nyangkut" di Indonesia walau pengguna sudah
		// memilih bendera. Baca current_lang dari HttpSession (di-set SecurityFilter.terapkanBahasaDariUrl
		// / initBahasaParameter) via RequestContext (di-set FilterJSP untuk semua request /*).
		if (currentLang == null || currentLang.trim().length() == 0) {
			try {
				javax.servlet.http.HttpServletRequest req = RequestContext.get();
				if (req != null) {
					javax.servlet.http.HttpSession sess = req.getSession(false);
					if (sess != null) {
						Object v = sess.getAttribute("current_lang");
						if (v != null && v.toString().trim().length() > 0) {
							currentLang = v.toString();
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:currentLang-httpsession"); }
		}

		if (currentLang == null) {
			currentLang = Tbmuser.INDONESIA;
		}
		return currentLang;
	}

	public static void initBahasaParameter(String lang) {
		CommonComboLanguageHelper.initBahasaParameter(lang);
	}

	/**
	 * Kode bahasa AKTIF ("id"/"en"/"ar"/"zh") untuk MENYOROT pemilih bendera secara konsisten di seluruh
	 * ZK &amp; JSP. Sumber kebenaran utama = sesi ({@code current_lang}). Namun bila sesi masih default
	 * Indonesia PADAHAL pengguna yang login punya pilihan bahasa TERSIMPAN (kolom {@code bahasa}) yang
	 * berbeda, pakai nilai tersimpan itu — sehingga bendera tidak "nyangkut" di Indonesia ketika sesi belum
	 * sinkron dengan pilihan yang sudah dipersist ke DB. Dengan begitu bendera yang tersorot SELALU cocok
	 * dengan bahasa yang benar-benar ditampilkan.
	 */
	public static String kodeBahasaAktif() {
		String lang = null;
		try {
			lang = currentLang();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:1244");
		}
		try {
			boolean sesiKosongAtauIndo = lang == null || lang.trim().length() == 0
					|| Tbmuser.INDONESIA.equalsIgnoreCase(lang);
			if (sesiKosongAtauIndo) {
				Tbmuser u = getCurrentUser();
				if (u != null) {
					String db = u.getBahasa();
					if (db != null && db.trim().length() > 0) {
						lang = db;
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:1258");
		}
		if (lang != null) {
			if (Tbmuser.ENGLISH.equalsIgnoreCase(lang)) {
				return "en";
			}
			if (Tbmuser.ARAB.equalsIgnoreCase(lang)) {
				return "ar";
			}
			if (Tbmuser.MANDARIN.equalsIgnoreCase(lang)) {
				return "zh";
			}
		}
		return "id";
	}

	/**
	 * Terjemahkan DATA DINAMIS (mis. judul/konten pengumuman, nama kegiatan) ke bahasa aktif memakai
	 * kamus internal — <b>TANPA menyimpan ke tabel LabelBahasa</b> (hanya diterjemah saat tampil). Bila
	 * bahasa aktif Indonesia atau teks kosong, dikembalikan apa adanya.
	 */
	public static String terjemahDinamis(String teks) {
		try {
			if (teks == null || teks.trim().length() == 0) {
				return teks;
			}
			String lang = currentLang();
			if (lang == null || lang.equalsIgnoreCase(ais.database.model.Tbmuser.INDONESIA)) {
				return teks;
			}
			return ais.common.AiTerjemah.terjemah(teks, kodeTerjemahDinamis(lang));
		} catch (Exception e) {
			return teks;
		}
	}

	private static String kodeTerjemahDinamis(String lang) {
		if (lang != null) {
			if (lang.equalsIgnoreCase(ais.database.model.Tbmuser.ARAB)) {
				return "arab";
			}
			if (lang.equalsIgnoreCase(ais.database.model.Tbmuser.MANDARIN)) {
				return "mandarin";
			}
		}
		return "english";
	}

	/** Seperti {@link #terjemahDinamis(String)} namun SADAR-TAG untuk konten HTML (mis. isi pengumuman). */
	public static String terjemahDinamisHtml(String html) {
		try {
			if (html == null || html.trim().length() == 0) {
				return html;
			}
			String lang = currentLang();
			if (lang == null || lang.equalsIgnoreCase(ais.database.model.Tbmuser.INDONESIA)) {
				return html;
			}
			return ais.common.KamusBahasaInternal.terjemahHtml(html, kodeTerjemahDinamis(lang));
		} catch (Exception e) {
			return html;
		}
	}

	public static String getBahasa(String key) {
		return CommonComboLanguageHelper.getBahasa(key);
	}

	/**
	 * <h3>Terjemahan MULTI-BAHASA inline: Indonesia / English / Arab</h3>
	 *
	 * <p>Mengembalikan teks sesuai bahasa aktif pengguna. Bila kunci (dibentuk dari teks Indonesia) belum
	 * ada di tabel {@code LabelBahasa}, otomatis meng-INSERT satu baris berisi KETIGA bahasa sekaligus dari
	 * argumen. Cocok untuk pesan/alert agar developer menyediakan terjemahan lengkap langsung di kode;
	 * masing-masing kampus tetap dapat menyunting terjemahan lewat data DB.</p>
	 *
	 * <p>Contoh: {@code MyMessageboxConfig.show(Common.bahasa("Data berhasil disimpan.", "Data saved.", "..."))}.</p>
	 *
	 * @param indonesia teks Bahasa Indonesia (WAJIB &mdash; juga kunci &amp; fallback)
	 * @param english   teks Bahasa Inggris (boleh kosong &rarr; fallback Indonesia)
	 * @param arab      teks Bahasa Arab (boleh kosong &rarr; fallback Indonesia)
	 * @return teks sesuai bahasa aktif
	 */
	public static String bahasa(String indonesia, String english, String arab) {
		return CommonComboLanguageHelper.bahasa(indonesia, english, arab);
	}

	/**
	 * Seperti {@link #bahasa(String, String, String)} namun mendukung placeholder {@code {V1}},{@code {V2}},...
	 * yang disubstitusi dengan {@code args} setelah pemilihan bahasa (auto-insert 3 bahasa bila belum ada).
	 * Contoh: {@code Common.bahasaFormat("Data \"{V1}\" tersimpan.", "Data \"{V1}\" saved.", "...", nama)}.
	 */
	public static String bahasaFormat(String indonesia, String english, String arab, Object... args) {
		return CommonComboLanguageHelper.bahasaFormat(indonesia, english, arab, args);
	}

	public static String getReq() {
		return CommonComboLanguageHelper.getReq();
	}

	public static Combobox createComboBulan(Combobox conf) {
		return CommonComboLanguageHelper.createComboBulan(conf);
	}

	public static Pertemuan ambilPertemuan(StatusPertemuan statusPertemuan, Perkuliahan perkuliahan) {
		return CommonMaintenanceHelper.ambilPertemuan(statusPertemuan, perkuliahan);
	}


	public static void uploadTugas(final Tugas tugas, final Mahasiswa mahasiswa,
			final BiodataCalonMahasiswa biodataCalonMahasiswa, final EventListener eventListener) throws Exception {
		CommonFileMediaHelper.uploadTugas(tugas, mahasiswa, biodataCalonMahasiswa, eventListener);
	}

	public static void uploadTugas(final Tugas tugas, final Siswa siswa, final CalonSiswa calonSiswa,
			final EventListener eventListener) throws Exception {
		CommonFileMediaHelper.uploadTugas(tugas, siswa, calonSiswa, eventListener);
	}

	public static void uploadTugas(final Tugas tugas, final CalonSiswa calonSiswa, final EventListener eventListener)
			throws Exception {
		CommonFileMediaHelper.uploadTugas(tugas, calonSiswa, eventListener);
	}

	@SuppressWarnings({ })
	public static Set<Long> checkStatusAbsensi(final Mahasiswa mahasiswa, Integer semester, Integer semesterPendek,
			final String ujian) {
		return CommonAcademicValidationHelper.checkStatusAbsensi(mahasiswa, semester, semesterPendek, ujian);
	}


	public static ParameterUmum getParameterUmum(String nama, String defaultValue) {
		return CommonUiFactoryHelper.getParameterUmum(nama, defaultValue);
	}

	public static ParameterUmum getParameterUmum(String nama, String defaultValue, String info1, String info2,
			String info3) {
		return CommonUiFactoryHelper.getParameterUmum(nama, defaultValue, info1, info2, info3);
	}

	public static Map<String, Program> programs = new HashMap<String, Program>();

	public static void reInitProgram() {
		CommonMaintenanceHelper.reInitProgram();
	}


	public static Map<Date, HariLiburPerpustakaan> hariLiburPerpustakaans = new HashMap<Date, HariLiburPerpustakaan>();

	public static void reInitHariLibur() {
		CommonMaintenanceHelper.reInitHariLibur();
	}


	/** True saat webapp berhenti/di-reload (di-set AppStartupListener.contextDestroyed). */
	public static volatile boolean aplikasiSedangBerhenti = false;
	private static volatile Thread initCacheThread = null;

	/**
	 * Dipanggil dari AppStartupListener.contextDestroyed agar thread init-cache
	 * yang masih tertunda TIDAK menyentuh classloader webapp yang sudah berhenti
	 * (mencegah "web application instance has been stopped already" + classloader
	 * pinning saat shutdown/redeploy/reload context.xml).
	 */
	public static void tandaiAplikasiBerhenti() {
		aplikasiSedangBerhenti = true;
		Thread t = initCacheThread;
		if (t != null) {
			try {
				t.interrupt();
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/Common.java:1428");
			}
		}
	}

	static {
		initCacheThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000 * 15);
				} catch (InterruptedException interupsi) {
					Thread.currentThread().interrupt();
					return; // webapp berhenti saat menunggu -> keluar diam-diam
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				// Jangan membangun SessionFactory / load class bila webapp sedang
				// berhenti atau di-reload: classloader sudah mati sehingga akan
				// melempar IllegalStateException + menyandera classloader.
				if (aplikasiSedangBerhenti || Thread.currentThread().isInterrupted()) {
					return;
				}

				try {
					reInitProgram();
					reInitHariLibur();
				} catch (Throwable t) {
					// Saat context stop/redeploy, akses classloader mati melempar
					// IllegalStateException. Diamkan bila memang sedang berhenti.
					if (!aplikasiSedangBerhenti) {
						t.printStackTrace(); ais.common.ErrorAuditUtil.record(t, "auto-audit src/ais/common/Common.java:1460");
					}
				}
			}
		}, "AIS-Common-InitCache");
		initCacheThread.setDaemon(true);
		initCacheThread.start();
	}

	public static JenisKegiatan getJenisKegiatan(String namaKegiatan) {
		return CommonUiFactoryHelper.getJenisKegiatan(namaKegiatan);
	}

	public static JenisKegiatan getJenisKodeKegiatan(String kodeJenisKegiatan) {
		return CommonUiFactoryHelper.getJenisKodeKegiatan(kodeJenisKegiatan);
	}

	@SuppressWarnings("rawtypes")
	public static void initSequence(Class clazz, String squenceName) {
		CommonMaintenanceHelper.initSequence(clazz, squenceName);
	}


	public static Map<String, String> responseCode = new HashMap<String, String>();

	public static String CURRENT_URL_TEMP = null;
	public static String CURRENT_URL_TEMP_SIMPLE = null;
	public static String CURRENT_LOCAL_URL_TEMP = null;

	static {
		responseCode.put("00", "Sukses");
		responseCode.put("01", "Alamat IP tidak diizinkan");
		responseCode.put("02", "NIM tidak ditemukan");
		responseCode.put("03", "Pembayaran telah dilah dilakukan");
		responseCode.put("04", "Pembayaran terlambat");
		responseCode.put("05", "Pembayaran tidak mencukupi");
	}

	public static Combobox createComboHari() {
		return CommonComboLanguageHelper.createComboHari();
	}

	public static Combobox createComboHariDanSemua() {
		return CommonComboLanguageHelper.createComboHariDanSemua();
	}

	public static void launchMenu(West navigasi, MyToolbarbuttonConfig menuService, Menu menu, LogLogin login)
			throws Exception {
		Tabbox iframe = (Tabbox) Sessions.getCurrent().getAttribute("iframe");
		launchMenu(navigasi, menuService, iframe, menu, login);
	}

	public static void launchMenu(West navigasi, MyToolbarbuttonConfig menuService, Tabbox iframe, Menu menu,
			LogLogin login) throws Exception {

		if (menu.getBukaHalamanBaru()) {
			Tbmuser tbmuser = Common.getCurrentUser();
			Executions.getCurrent()
					.sendRedirect(
							(menu.getUrl().startsWith("http") ? menu.getUrl()
									: (Common.getRequestHostWithProtocol() + menu.getUrl()))
									+ (tbmuser == null ? ""
											: "?uid=" + URLEncoder.encode(
													Common.desEncrypter.get().encrypt(tbmuser.getUserId()), "UTF-8")),
							"_blank");
			return;
		}

		if (menu.getUrl() != null && menu.getUrl().trim().endsWith("biodata_dosen.zul")) {
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser == null || tbmuser.ambilDosen() == null) {
				PesanFormalHelper.tampilkanGagal("akses halaman biodata Dosen",
						"Akun yang sedang Bapak/Ibu gunakan untuk masuk (login) bukan merupakan akun dengan hak "
								+ "akses Dosen, sehingga menu ini tidak dapat diakses.",
						new String[] { "Silakan keluar (logout), lalu masuk kembali menggunakan akun Dosen.",
								"Bila Bapak/Ibu seharusnya memiliki akun Dosen namun tidak bisa login, hubungi "
										+ "Administrator Sistem." });
				return;
			}
		}

		if (menu.getUrl().trim().equals("profil")) {
			Tbmuser tbmuser = Common.getCurrentUser();
			MainHelper.onUbahBiodata(tbmuser, null);
		}

		else if (menu.getUrl().trim().equals("rubah_password")) {
			ChangePasswordWindow window = new ChangePasswordWindow(false, true);
			window.setVisible(true);
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			window.setHeight("300px");
			window.setWidth("500px");
			window.onModal();

		} else if (menu.getUrl().trim().equals("daftar_sidang_atau_munaqosah")) {
			tampilkanDaftarSkripsi();
		} else if (menu.getUrl().trim().equals("proposal_sidang_atau_munaqosah")) {
			tampilkanTugasAkhir();
		} else if (menu.getUrl().trim().equals("laporanRekapHostToHostWindow")) {
			LaporanRekapHostToHostWindow window = new LaporanRekapHostToHostWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanDaftarHadirDosen")) {
			LaporanDaftarHadirDosen window = new LaporanDaftarHadirDosen();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanRekapPenilaianMahasiswaWindow")) {
			LaporanRekapPenilaianMahasiswaWindow window = new LaporanRekapPenilaianMahasiswaWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanRekamanNilai")) {
			LaporanRekamanNilai window = new LaporanRekamanNilai();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("rekap_jumlah_dosen_semua")) {
			LaporanRekapitulasiDosenWindow window = new LaporanRekapitulasiDosenWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("rekapDosenPendidikan")) {
			LaporanRekapitulasiDosenPerPendidikanWindow window = new LaporanRekapitulasiDosenPerPendidikanWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("rekapDataPmdk")) {
			LaporanRekapitulasiPMDKWindow window = new LaporanRekapitulasiPMDKWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("rekapAngketDosenPerDosen")) {
			LaporanAngketDosenPerDosenWindow window = new LaporanAngketDosenPerDosenWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("rekapDosenPa")) {
			LaporanRekapitulasiPAWindow window = new LaporanRekapitulasiPAWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanDataMahasiswaWindow")) {
			LaporanDataMahasiswaWindow window = new LaporanDataMahasiswaWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanDaftarPegawaiNamaAlamat")) {
			LaporanDataPegawaiNamaAlamatWindow window = new LaporanDataPegawaiNamaAlamatWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("statistikJumlahPegawaiPerJenisKelaminWindow")) {
			StatistikJumlahPegawaiPerJenisKelamin window = new StatistikJumlahPegawaiPerJenisKelamin();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("statistikJumlahPegawaiBaseStatusWindow")) {
			StatistikJumlahPegawaiBaseStatus window = new StatistikJumlahPegawaiBaseStatus();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);

		} else if (menu.getUrl().trim().equals("statistikJumlahPegawaiBaseJabatanFungsionalWindow")) {
			StatistikJumlahPegawaiBaseJabatanFungsional window = new StatistikJumlahPegawaiBaseJabatanFungsional();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("statistikJumlahPegawaiBaseTahunWindow")) {
			StatistikJumlahPegawaiBaseTahun window = new StatistikJumlahPegawaiBaseTahun();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("rekapJumlahPegawaiBaseUnitKerjaWindow")) {
			RekapJumlahPegawaiBaseUnitKerja window = new RekapJumlahPegawaiBaseUnitKerja();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("rekapJumlahPegawaiBaseGolonganWindow")) {
			RekapJumlahPegawaiBaseGolongan window = new RekapJumlahPegawaiBaseGolongan();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);

		} else if (menu.getUrl().trim().equals("statistikJumlahPegawaiPerPendidikanWindow")) {
			StatistikJumlahPegawaiPerPendidikan window = new StatistikJumlahPegawaiPerPendidikan();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);

		} else if (menu.getUrl().trim().equals("laporanDaftarPegawaiWindow")) {
			LaporanDaftarPegawai window = new LaporanDaftarPegawai();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanDaftarUrutKepangkatanWindow")) {
			LaporanDaftarUrutKepangkatan window = new LaporanDaftarUrutKepangkatan();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanRekapMahasiswaSudahBayarWindow")) {
			LaporanRekapMahasiswaSudahBayarWindow window = new LaporanRekapMahasiswaSudahBayarWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanRekapMahasiswaBelumBayarWindow")) {
			LaporanRekapMahasiswaBelumBayarWindow window = new LaporanRekapMahasiswaBelumBayarWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanRekapPerProdiDenganPenguranganPerValidatorWindow")) {
			LaporanRekapPerProdiDenganPenguranganPerValidatorWindow window = new LaporanRekapPerProdiDenganPenguranganPerValidatorWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanRekapPerProdiDenganPenguranganWindow")) {
			LaporanRekapPerProdiDenganPenguranganWindow window = new LaporanRekapPerProdiDenganPenguranganWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanKartuHasilStudiMahasiswaWindow")) {
			LaporanKartuHasilStudiMahasiswaWindow window = new LaporanKartuHasilStudiMahasiswaWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanRekapPerProdiWindow")) {
			LaporanRekapPerProdiWindow window = new LaporanRekapPerProdiWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanDaftarHadirWindow")) {
			LaporanDaftarHadirWindow window = new LaporanDaftarHadirWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanDaftarNilaiWindow")) {
			LaporanDaftarNilaiWindow window = new LaporanDaftarNilaiWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanDaftarPrestasiBelajarWindow")) {
			LaporanDaftarPrestasiBelajarWindow window = new LaporanDaftarPrestasiBelajarWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanRekapPerPembayaranWindow")) {
			LaporanRekapPerPembayaranWindow window = new LaporanRekapPerPembayaranWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanRekapPerJenisBiayaWindow")) {
			LaporanRekapPerJenisBiayaWindow window = new LaporanRekapPerJenisBiayaWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("LaporanRekapPerPembayarandgnPenguranganWindow")) {
			LaporanRekapPerPembayarandgnPenguranganWindow window = new LaporanRekapPerPembayarandgnPenguranganWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("generateValidasiLaporanWindow")) {
			GenerateValidasiLaporanWindow window = new GenerateValidasiLaporanWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("kurikulum")) {
			LaporanKurikulumWindow window = new LaporanKurikulumWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("rekap_jumlah_mahasiswa_fakultas")) {
			LaporanRekapJumlahMhsFakWindow window = new LaporanRekapJumlahMhsFakWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanJadwalUAS")) {
			LaporanJadwalUasWindow window = new LaporanJadwalUasWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanAbsensiUjian")) {
			LaporanAbsensiUjianWindow window = new LaporanAbsensiUjianWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("daftarHadirDosen")) {
			LaporanDaftarHadirDosenHarianWindow window = new LaporanDaftarHadirDosenHarianWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanAbsensi")) {
			LaporanAbsensiWindow window = new LaporanAbsensiWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("perStatus")) {
			MyWindow window = ((MyWindow) ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getAttribute("perStatus"));
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanBeritaAcaraSkripsi")) {
			LaporanBeritaAcaraSkripsiWindow window = new LaporanBeritaAcaraSkripsiWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanDaftarHadirUjianSidang")) {
			LaporanDaftarHadirUjianSidangWindow window = new LaporanDaftarHadirUjianSidangWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanDaftarHadirDosenSemua")) {
			LaporanDaftarHadirDosenWindow window = new LaporanDaftarHadirDosenWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanUjianSidangSkripsi")) {
			LaporanNilaiUjianSidangSkripsiWindow window = new LaporanNilaiUjianSidangSkripsiWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanCoverAbsensi")) {
			LaporanCoverAbsensiWindow window = new LaporanCoverAbsensiWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanSKSDosenWindow")) {
			LaporanSKSDosenWindow window = new LaporanSKSDosenWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanTranskripAkademik")) {
			LaporanTranskipAkademikWindow window = new LaporanTranskipAkademikWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanKHS")) {
			LaporanKHSWindow window = new LaporanKHSWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanPerkuliahan")) {
			LaporanJadwalPerkuliahanWindow window = new LaporanJadwalPerkuliahanWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("perJenjangPendidikan")) {
			MyWindow window = ((MyWindow) ExecutionsCtrl.getCurrentCtrl().getCurrentPage()
					.getAttribute("perJenjangPendidikan"));
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("perKualifikasiPelamar")) {
			MyWindow window = ((MyWindow) ExecutionsCtrl.getCurrentCtrl().getCurrentPage()
					.getAttribute("perKualifikasiPelamar"));
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanRegistrasiWisuda")) {
			GenerateNoKursiDanNoRegistrasiWindow window = new GenerateNoKursiDanNoRegistrasiWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("generateNoKursi")) {
			GenerateNoKursiWindow window = new GenerateNoKursiWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("cetakAlbumWisudaAdmin")) {
			CetakAlbumWisudaAdminWindow window = new CetakAlbumWisudaAdminWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("cetakAlbumWisudaMahasiswa")) {
			new CetakAlbumWisudaMahasiswaHelper();
		} else if (menu.getUrl().trim().equals("cetakUndanganWisuda")) {
			GenerateUndanganWisudaWindow window = new GenerateUndanganWisudaWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("rekapitulasiValidasiKeuangan")) {
			LaporanRekapitulasiValidasiKeuanganWindow window = new LaporanRekapitulasiValidasiKeuanganWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("rekapitulasiAlumniJurusan")) {
			LaporanRekapitulasiAlumniJurusanWindow window = new LaporanRekapitulasiAlumniJurusanWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("rekapitulasiItemBiaya")) {
			LaporanRekapitulasiItemBiayaWindow window = new LaporanRekapitulasiItemBiayaWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("rekapitulasiDataMahasiswa")) {
			LaporanRekapitulasiMahasiswaWindow window = new LaporanRekapitulasiMahasiswaWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanKHSSemesterPendek")) {
			LaporanKHSSemesterPendekWindow window = new LaporanKHSSemesterPendekWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanTranskripAkademikKonversi")) {
			LaporanTranskipAkademikKonversiWindow window = new LaporanTranskipAkademikKonversiWindow();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanProporsiJumlahmahasiswapendaftar")) {
			LaporanProporsiJumlahmahasiswapendaftar window = new LaporanProporsiJumlahmahasiswapendaftar();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);
		} else if (menu.getUrl().trim().equals("laporanDaftarStatusAwalMahasiswa")) {
			LaporanDaftarStatusAwalMahasiswa window = new LaporanDaftarStatusAwalMahasiswa();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);

		} else if (menu.getUrl().trim().equals("laporanRekapJenisSeleksiMahasiswaBaru")) {
			LaporanRekapJenisSeleksiMahasiswaBaru window = new LaporanRekapJenisSeleksiMahasiswaBaru();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);

		} else if (menu.getUrl().trim().equals("laporanLulusMahasiswaBaru")) {
			LaporanLulusMahasiswaBaru window = new LaporanLulusMahasiswaBaru();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);

		} else if (menu.getUrl().trim().equals("laporanDaftarUlangMahasiswaBaru")) {
			LaporanDaftarUlangMahasiswaBaru window = new LaporanDaftarUlangMahasiswaBaru();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);

		} else if (menu.getUrl().trim().equals("laporanPendaftarMahasiswaBaru")) {
			LaporanPendaftarMahasiswaBaru window = new LaporanPendaftarMahasiswaBaru();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);

		} else if (menu.getUrl().trim().equals("laporanAlbumMahasiswaWisuda")) {
			LaporanAlbumMahasiswaWisuda window = new LaporanAlbumMahasiswaWisuda();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);

		} else if (menu.getUrl().trim().equals("laporanAlbumProfileWisuda")) {
			LaporanAlbumProfileWisuda window = new LaporanAlbumProfileWisuda();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);

		} else if (menu.getUrl().trim().equals(LaporanTransaksiPegawai.class.getName())) {
			LaporanTransaksiPegawai window = new LaporanTransaksiPegawai();
			Common.insertToTab(navigasi, menuService, iframe, menu, window, login);

		} else {
			Common.insertToTab(navigasi, menuService, iframe, menu, login);
		}

	}

	public static void pilihMenu(final West navigasi, final MyToolbarbuttonConfig menuService) {
		if (Common.bolehKonfigurasi("otomatis_tertutup_menu_jika_buka_halaman")) {
			Common.createDefaultTimerNoBusy(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					try {
						if (navigasi != null) {
							navigasi.setOpen(false);
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:1795");
						// TODO: handle exception
					}
					try {
						if (menuService != null) {
							menuService.setVisible(true);
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:1802");
						// TODO: handle exception
					}
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	public static Tab insertUrl(West navigasi, MyToolbarbuttonConfig menuService, Tabbox tabbox, String label,
			String url, String image, Component component) {

		Common.pilihMenu(navigasi, menuService);

		List<Tab> tabs = tabbox.getTabs().getChildren();
		boolean ada = false;
		for (Tab tab : tabs) {
			if (tab.getAttribute("url") != null && (tab.getAttribute("url") instanceof String)) {
				String m = (String) tab.getAttribute("url");
				if (m.equals(url)) {
					ada = true;
					tab.setSelected(true);
					return tab;
				}
			}

			else if (tab.getLabel().equalsIgnoreCase(label)) {
				ada = true;
				tab.setSelected(true);
				return tab;
			}
		}

		MyTabConfig tab = new MyTabConfig(label, image);
		if (!ada) {

			tab.setAttribute("url", url);
			tab.setClosable(true);
			tab.setSelected(true);
			tabbox.getTabs().appendChild(tab);
			final Tabpanel tabpanel = new ais.ui.util.MyTabpanel();

			if (component != null) {

				if (component instanceof Window) {
					tabpanel.appendChild(component);
				} else {
					Common.tampilanScroll(tabpanel).appendChild(component);
				}

			} else {

				// Jangan panggil Class.forName untuk path ZUL — langsung pakai MyInclude
				boolean isZulPath = url.startsWith("/") || url.endsWith(".zul");
				if (!isZulPath) try {

					MyWindow window = (MyWindow) Class.forName(url).newInstance();
					window.setHeight("100%");
					window.setWidth("100%");
					tabpanel.appendChild(window);

					return tab;
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:1862");

				}

				MyInclude include = new MyInclude(url);
				include.setWidth("100%");
				include.setHeight("100%");
				tabpanel.appendChild(include);

			}

			tabbox.getTabpanels().appendChild(tabpanel);

		}
		return tab;
	}

	public static void insertToTab(West navigasi, MyToolbarbuttonConfig menuService, final Tabbox iframe,
			final Menu menu, final LogLogin login) {
		CommonMenuAccessHelper.insertToTab(navigasi, menuService, iframe, menu, login);
	}

	public static boolean checkBaypassStatusPembayaranMahasiswa(Integer semester, Integer tahap, Mahasiswa mahasiswa,
			JenisKegiatan jenisKegiatan) {
		return CommonPaymentHelper.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, jenisKegiatan);
	}

	public static boolean checkBaypassStatusPembayaranMahasiswa(Integer semester, Integer tahap, Mahasiswa mahasiswa,
			Collection<JenisKegiatan> jenisKegiatans) {
		return CommonPaymentHelper.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, jenisKegiatans);
	}

	public static boolean checkStatusPembayaranMahasiswa(Integer semester, Integer tahap, Mahasiswa mahasiswa,
			boolean persetujuan, boolean sp) {
		return CommonPaymentHelper.checkStatusPembayaranMahasiswa(semester, tahap, mahasiswa, persetujuan, sp);
	}

	public static boolean checkStatusPembayaranMahasiswa(Integer semester, Integer tahap, Mahasiswa mahasiswa,
			boolean check, boolean persetujuan, boolean sp) {
		return CommonPaymentHelper.checkStatusPembayaranMahasiswa(semester, tahap, mahasiswa, check, persetujuan, sp);
	}

	public static boolean checkStatusPembayaranMahasiswaSebelumnya(Integer semester, Integer tahap,
			Mahasiswa mahasiswa) {
		return CommonPaymentHelper.checkStatusPembayaranMahasiswaSebelumnya(semester, tahap, mahasiswa);
	}

	public static boolean checkStatusPembayaranMahasiswaSebelumnya(Integer semester, Integer tahap, Mahasiswa mahasiswa,
			boolean persetujuan) {
		return CommonPaymentHelper.checkStatusPembayaranMahasiswaSebelumnya(semester, tahap, mahasiswa, persetujuan);
	}

	public static boolean checkStatusPembayaranKegiatanMahasiswa(FormulirKegiatan formulirKegiatan,
			Mahasiswa mahasiswa) {
		return CommonPaymentHelper.checkStatusPembayaranKegiatanMahasiswa(formulirKegiatan, mahasiswa);
	}

	public static boolean checkStatusPembayaranMahasiswaSebelumnyaUntukPenilaian(Integer semester, Integer tahap,
			Mahasiswa mahasiswa, Double harusLunas, boolean termasukSmt1) {
		return CommonPaymentHelper.checkStatusPembayaranMahasiswaSebelumnyaUntukPenilaian(semester, tahap, mahasiswa,
				harusLunas, termasukSmt1);
	}

	public static boolean checkStatusPembayaranMahasiswaPengajuanSkripsi(
			FormatNilaiProposalSkripsi formatNilaiProposalSkripsi, Integer semester, Mahasiswa mahasiswa) {
		return CommonPaymentHelper.checkStatusPembayaranMahasiswaPengajuanSkripsi(formatNilaiProposalSkripsi, semester,
				mahasiswa);
	}

	public static boolean checkStatusPembayaranMahasiswaPengajuanSidang(FormatNilaiSkripsi formatNilaiSkripsi,
			Integer semester, Mahasiswa mahasiswa) {
		return CommonPaymentHelper.checkStatusPembayaranMahasiswaPengajuanSidang(formatNilaiSkripsi, semester,
				mahasiswa);
	}

	public static boolean checkStatusPembayaranMahasiswaPengajuanWisuda(Integer semester, Mahasiswa mahasiswa) {
		return CommonPaymentHelper.checkStatusPembayaranMahasiswaPengajuanWisuda(semester, mahasiswa);
	}

	public static List<Long> getDetailperkuliahansSudahDinilai(Mahasiswa mahasiswa, Integer semester,
			final Integer tahapan, Integer semesterPendek, Boolean hitungSemua) {

		List<Long> detailperkuliahans = KrsDetailHelper.ambilDetailperkuliahan(mahasiswa, semester, tahapan,
				semesterPendek, false, hitungSemua, Detailperkuliahan.DISETUJUI, true, false);

		return saringMatakuliahyangPalingBesarNilainya(detailperkuliahans,
				mahasiswa == null ? null : mahasiswa.getNim());

	}

	public static List<Long> getDetailperkuliahansBelumDinilai(Mahasiswa mahasiswa, Integer semester,
			final Integer tahapan, Integer semesterPendek, Boolean hitungSemua) {

		List<Long> detailperkuliahans = KrsDetailHelper.ambilDetailperkuliahan(mahasiswa, semester, tahapan,
				semesterPendek, false, hitungSemua, Detailperkuliahan.DISETUJUI, false, true);

		return saringMatakuliahyangPalingBesarNilainya(detailperkuliahans,
				mahasiswa == null ? null : mahasiswa.getNim());
	}

	public static List<Long> getIkutDetailperkuliahans(Mahasiswa mahasiswa, Integer semester, final Integer tahapan,
			Integer persetujuan, Integer semesterPendek, Boolean hitungSemua) {
		return CommonAcademicKrsNilaiHelper.getIkutDetailperkuliahans(mahasiswa, semester, tahapan, persetujuan,
				semesterPendek, hitungSemua);
	}

	public static List<Long> getDetailperkuliahans(Mahasiswa mahasiswa, Integer semester, Integer persetujuan,
			Integer semesterPendek, boolean remedial, Boolean hitungSemua, Boolean reload) {
		return getDetailperkuliahans(mahasiswa, semester, persetujuan, semesterPendek, remedial, hitungSemua, false,
				reload);
	}

	public static List<Long> getDetailperkuliahans(Mahasiswa mahasiswa, Integer semester, Integer persetujuan,
			Integer semesterPendek, boolean remedial, Boolean hitungSemua, Boolean saring, Boolean reload) {
		return getDetailperkuliahans(mahasiswa, semester, 0, persetujuan, semesterPendek, remedial, hitungSemua, saring,
				reload);
	}

	@SuppressWarnings({})
	public static List<Long> getDetailperkuliahans(Mahasiswa mahasiswa, Integer semester, Integer tahapan,
			Integer persetujuan, Integer semesterPendek, boolean remedial, Boolean hitungSemua, Boolean saring,
			Boolean reload) {

		try {

			if (reload) {
				mahasiswa.reInitDetailperkuliahan(HibernateUtil.currentSession());
				mahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek, true, true)
						.getMahasiswa();
			}

			List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
					remedial, hitungSemua, persetujuan);

			List<Long> d = saring
					? saringMatakuliahyangPalingBesarNilainya(detailperkuliahans,
							mahasiswa == null ? null : mahasiswa.getNim())
					: detailperkuliahans;
			return d;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return new ArrayList<Long>();
		}
	}

	public static List<Long> saringMatakuliahyangPalingBesarNilainya(List<Long> detailperkuliahans, String nim) {
		return CommonAcademicKrsNilaiHelper.saringMatakuliahyangPalingBesarNilainya(detailperkuliahans, nim);
	}

	public static void insertToTab(final West navigasi, final MyToolbarbuttonConfig menuService, Tabbox iframe,
			final Menu menu, MyWindow window, final LogLogin login) {
		CommonMenuAccessHelper.insertToTab(navigasi, menuService, iframe, menu, window, login);
	}

	public static String getRequestHost() {
		return CommonCurrentSessionHelper.getRequestHost();
	}

	public static String getRequestHost(HttpServletRequest request) {
		return CommonCurrentSessionHelper.getRequestHost(request);
	}

	public static String getRequestHostWithProtocol() {
		return CommonCurrentSessionHelper.getRequestHostWithProtocol();
	}

	public static String getRequestHostWithProtocol(HttpServletRequest req) {
		return CommonCurrentSessionHelper.getRequestHostWithProtocol(req);
	}

	public static String getRequestHostWithProtocolSimple() {
		return CommonCurrentSessionHelper.getRequestHostWithProtocolSimple();
	}

	public static String getRequestHostWithProtocolSimple(HttpServletRequest req) {
		return CommonCurrentSessionHelper.getRequestHostWithProtocolSimple(req);
	}

	public static AccessedUsers setUserAccess(HttpServletRequest request) {
		return CommonMenuAccessHelper.setUserAccess(request);
	}

	public static AccessedUsers setUserAccess(Session mySession, HttpServletRequest request) {
		return CommonMenuAccessHelper.setUserAccess(mySession, request);
	}

	public static void setFooterText(HttpServletRequest request, A a) {
		CommonMenuAccessHelper.setFooterText(request, a);
	}

	public static void removeUserAccess() {
		HttpSession httpSession = (HttpSession) Sessions.getCurrent().getNativeSession();

		Session mySession = HibernateUtil.currentSession();
		AccessedUsers accessedUsers = (AccessedUsers) mySession.createCriteria(AccessedUsers.class)
				.add(Restrictions.eq("nama", httpSession.getId())).setMaxResults(1).uniqueResult();
		if (accessedUsers != null) {
			mySession.delete(accessedUsers);
		}
	}

	public static Konfigurasi checkKonfigurasiBigIcon() {
		Session session = HibernateUtil.currentSession();
		Transaction transaksi = session.getTransaction();
		boolean transaksiLokal = transaksi == null || !transaksi.isActive();
		Konfigurasi konfigurasi = null;
		try {
			if (transaksiLokal) {
				transaksi = session.beginTransaction();
			}
			konfigurasi = (Konfigurasi) ConstantValues.simpleObject(
					session.createCriteria(Konfigurasi.class).addOrder(Order.desc("id"))
						.add(Restrictions.eq("nama", ConstantValues.BIG_ICON)).setMaxResults(1),
					Konfigurasi.class);
			if (konfigurasi == null) {
				konfigurasi = new Konfigurasi();
				konfigurasi.setInfo1("false");
				konfigurasi.setNama(ConstantValues.BIG_ICON);
				session.save(konfigurasi);
			}
			if (transaksiLokal && transaksi != null && transaksi.isActive()) {
				transaksi.commit();
			}
		} catch (Exception e) {
			if (transaksiLokal && transaksi != null && transaksi.isActive()) {
				try { transaksi.rollback(); } catch (Exception abaikan) { }
			}
			ErrorAuditUtil.record(e, "Common.checkKonfigurasiBigIcon");
			konfigurasi = new Konfigurasi();
			konfigurasi.setInfo1("false");
			konfigurasi.setNama(ConstantValues.BIG_ICON);
		}
		return konfigurasi;
	}

	public static void reloadNilaiCurrentNilai(Mahasiswa mahasiswa, final Boolean reload) {
		CommonAcademicKrsNilaiHelper.reloadNilaiCurrentNilai(mahasiswa, reload);
	}

	public static void reloadNilai(final Mahasiswa mahasiswa, final Integer semester, final Boolean reload) {
		CommonAcademicKrsNilaiHelper.reloadNilai(mahasiswa, semester, reload);
	}

	public static void setting() throws Exception {
//		HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();

		REAL_PATH = Sessions.getCurrent().getWebApp().getRealPath("/");
		REAL_PATH_REPORT_REAL = Sessions.getCurrent().getWebApp().getRealPath("/report");

	}

	public static Boolean isNowSemensterGanjil() {
		return CommonCurrentSessionHelper.isNowSemensterGanjil();
	}

	public static Boolean isNowSemensterGanjil(Date tanggal) {
		return CommonCurrentSessionHelper.isNowSemensterGanjil(tanggal);
	}

	public static Boolean isNowSemensterGanjil(Tbmuser tbmuser, Date tanggal) {
		return CommonCurrentSessionHelper.isNowSemensterGanjil(tbmuser, tanggal);
	}

	public static Boolean isNowSemensterGanjil(Sekolah sekolah, Date tanggal) {
		return CommonCurrentSessionHelper.isNowSemensterGanjil(sekolah, tanggal);
	}

	public static Tbmuser getCurrentFromSpringUser() {
		return CommonCurrentSessionHelper.getCurrentFromSpringUser();
	}

	public static Tbmuser getCurrentFromUsername(String userName) {
		return CommonCurrentSessionHelper.getCurrentFromUsername(userName);
	}

	public static void copy(File file, File fileTujuan) throws Exception {
		CommonFileMediaHelper.copy(file, fileTujuan);
	}

	public static void checkLogoUpload() {
		CommonBrandingMediaHelper.checkLogoUpload();
	}

	public static void checkBackgroundUpload() {
		CommonFileMediaHelper.checkBackgroundUpload();
	}

	public static void checkFaviconUpload() {
		CommonFileMediaHelper.checkFaviconUpload();
	}

	public static void encripSemua() throws Exception {
		DataProcessor.encripSemua();
	}

	public static String[] haris = { "Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jum'at", "Sabtu" };
	public static Locale locale = new Locale("in", "ID");
	public static Locale localeEn = new Locale("en", "EN");

	private static boolean hasInitialQuery = false;

	public static Session getManualSession() {
		return CommonCurrentSessionHelper.getManualSession();
	}

	public static void fastChannelCopy(ReadableByteChannel src, WritableByteChannel dest) throws IOException {
		CommonFileUtil.fastChannelCopy(src, dest);

	}

	public static void writeInputStreamToFile(InputStream inputStream, File file) {
		CommonFileMediaHelper.writeInputStreamToFile(inputStream, file);
	}

	public static void writeBlobToFile(Blob blob, File file) {
		CommonFileMediaHelper.writeBlobToFile(blob, file);
	}

	public static File getCreateRandomFile() {
		return CommonFileMediaHelper.getCreateRandomFile();
	}

	public static String getCurrentSessionId() {
		return CommonCurrentSessionHelper.getCurrentSessionId();
	}

	/**
	 * Mengambil pengguna ({@link Tbmuser}) yang sedang login berdasarkan {@link HttpServletRequest}.
	 *
	 * <p><b>Tujuan.</b> Memberi identitas pengguna aktif pada konteks yang memegang objek request
	 * (mis. servlet/JSP/API), terlepas dari konteks ZK. Ini fondasi pemeriksaan hak akses,
	 * pencatatan audit "oleh siapa", dan personalisasi tampilan.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.getCurrentUser(request)}
	 * yang membaca pengguna dari atribut sesi HTTP terkait request tersebut. Logika sesungguhnya
	 * dipusatkan di helper agar konsisten dengan jalur ZK.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code request} = request HTTP aktif. Mengembalikan
	 * {@link Tbmuser} yang login, atau {@code null} bila sesi tidak punya pengguna (belum login/expired).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pakai overload ini di luar konteks ZK; di dalam request ZK gunakan
	 * {@link #getCurrentUser()}. Selalu antisipasi {@code null}.</p>
	 *
	 * @param request request HTTP aktif
	 * @return pengguna yang login, atau {@code null}
	 */
	public static Tbmuser getCurrentUser(HttpServletRequest request) {
		return CommonCurrentSessionHelper.getCurrentUser(request);
	}

	/**
	 * Mengambil {@link SatuanKerja} (unit kerja) yang sedang aktif pada konteks pengguna.
	 *
	 * <p><b>Tujuan.</b> Banyak data bersifat per-unit-kerja (multi-tenant/multi-unit); method ini
	 * memberi unit kerja aktif agar query &amp; tampilan terfilter sesuai konteks pengguna tanpa
	 * hardcode.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.getSatuanKerja()} yang
	 * menentukan unit kerja dari sesi/pengguna aktif.</p>
	 *
	 * <p><b>Return &amp; pemeliharaan.</b> Mengembalikan {@link SatuanKerja} aktif atau {@code null}
	 * bila tak ada konteks. Jangan mengasumsikan unit kerja tunggal—selalu lewat method ini agar
	 * mendukung lingkungan multi-unit.</p>
	 *
	 * @return unit kerja aktif, atau {@code null}
	 */
	public static SatuanKerja getSatuanKerja() {
		return CommonCurrentSessionHelper.getSatuanKerja();
	}

	/**
	 * Mengambil pengguna ({@link Tbmuser}) yang sedang login dari konteks SAAT INI (ZK/thread).
	 *
	 * <p><b>Tujuan.</b> Varian tanpa argumen yang dipakai mayoritas kode aplikasi (Action/helper ZK)
	 * untuk mengetahui "siapa yang sedang memakai sistem". Dipakai luas oleh audit
	 * ({@code DataUtil.ubahDataHistory}), pemeriksaan hak akses, dan personalisasi.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.getCurrentUser()} yang
	 * menemukan pengguna dari konteks eksekusi ZK/sesi aktif tanpa perlu objek request eksplisit.</p>
	 *
	 * <p><b>Return &amp; pemeliharaan.</b> Mengembalikan {@link Tbmuser} aktif, atau {@code null} bila
	 * tidak ada (mis. dipanggil dari thread latar tanpa konteks pengguna). Pada pekerjaan latar yang
	 * butuh identitas, teruskan {@link Tbmuser} secara eksplisit alih-alih bergantung pada method ini.
	 * Selalu antisipasi {@code null}.</p>
	 *
	 * @return pengguna yang login pada konteks saat ini, atau {@code null}
	 */
	public static Tbmuser getCurrentUser() {
		return CommonCurrentSessionHelper.getCurrentUser();
	}

	public static boolean getApakahAdminBolehAksesFeeder() {
		return CommonCurrentSessionHelper.getApakahAdminBolehAksesFeeder();
	}

	/** Apakah pengguna saat ini boleh mengakses dasbor/tombol "SISTER" (berbasis flag per-role Tbmrole). */
	public static boolean getApakahAdminBolehAksesSister() {
		return CommonCurrentSessionHelper.getApakahAdminBolehAksesSister();
	}

	public static boolean getApakahAdminBolehUpload() {
		return CommonCurrentSessionHelper.getApakahAdminBolehUpload();
	}

	public static boolean getApakahAdminBolehLihatSemuaPegawai() {
		return CommonCurrentSessionHelper.getApakahAdminBolehLihatSemuaPegawai();
	}

	public static boolean getApakahAdminBolehLihatSemuaCuti() {
		return CommonCurrentSessionHelper.getApakahAdminBolehLihatSemuaCuti();
	}

	public static boolean getApakahAdminBolehKunci() {
		return CommonCurrentSessionHelper.getApakahAdminBolehKunci();
	}

	/**
	 * Memeriksa apakah pengguna aktif adalah admin dengan KODE peran tertentu.
	 *
	 * <p><b>Tujuan.</b> Pemeriksaan hak akses berbutir-halus berdasarkan kode admin spesifik
	 * (mis. admin keuangan, admin akademik). Banyak tampilan/menu dan tombol aksi muncul/aktif hanya
	 * bila pengguna memegang kode admin yang sesuai—method ini sumber keputusannya.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.getApakahAdmin(kodeAdmin)}
	 * yang mengevaluasi peran/hak pengguna aktif terhadap {@code kodeAdmin}. Logika dipusatkan di
	 * helper agar definisi "admin" konsisten lintas modul.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code kodeAdmin} = kode peran admin yang diuji. Mengembalikan
	 * {@code true} bila pengguna aktif memegang peran tsb, {@code false} bila tidak (termasuk bila
	 * tidak ada pengguna aktif).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pakai overload ini ketika perlu membedakan jenis admin; pakai
	 * {@link #getApakahAdmin()} untuk pengecekan "admin apa pun". Hindari menyebar perbandingan kode
	 * admin secara hardcode—definisikan konstanta kode admin di satu tempat.</p>
	 *
	 * @param kodeAdmin kode peran admin yang diuji
	 * @return {@code true} bila pengguna aktif memegang peran admin tsb
	 */
	public static boolean getApakahAdmin(String kodeAdmin) {
		return CommonCurrentSessionHelper.getApakahAdmin(kodeAdmin);
	}

	/**
	 * Memeriksa apakah pengguna aktif berstatus admin (jenis apa pun).
	 *
	 * <p><b>Tujuan.</b> Pengecekan kasar "apakah ini administrator" untuk membuka fitur yang
	 * diperuntukkan bagi semua jenis admin (mis. menu pengelolaan umum), tanpa mempersoalkan kode
	 * admin spesifik.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.getApakahAdmin()} yang
	 * memeriksa peran pengguna aktif dari sesi. Hasilnya konsisten dengan
	 * {@link #getApakahAdmin(String)} pada level "ada/tidaknya status admin".</p>
	 *
	 * <p><b>Return &amp; pemeliharaan.</b> Mengembalikan {@code true} bila pengguna aktif adalah admin,
	 * selain itu {@code false} (termasuk saat tak ada pengguna login). Untuk kontrol akses yang lebih
	 * ketat per-fungsi, gunakan varian ber-kode atau pemeriksaan hak akses menu.</p>
	 *
	 * @return {@code true} bila pengguna aktif adalah admin
	 */
	public static boolean getApakahAdmin() {
		return CommonCurrentSessionHelper.getApakahAdmin();
	}

	/**
	 * <h3>Pisahkan field domain MULTI-DOMAIN (dipisah koma) menjadi daftar token bersih</h3>
	 *
	 * <p><b>Tujuan.</b> Satu institusi (PerguruanTinggi/Sekolah/Yayasan/Pendaftar) kini boleh memiliki
	 * BEBERAPA domain sekaligus, ditulis pada satu field {@code domain} dipisah tanda koma, mis.
	 * {@code "ecampus.a.ac.id, ecampus.b.ac.id, ecampus.c.ac.id"}. Method ini memecah string tersebut
	 * menjadi daftar domain individual agar bisa didaftarkan satu-per-satu ke peta {@code domain->entitas}
	 * (lihat {@code reInitByDomain()} pada tiap *Action) sehingga request dari domain mana pun tercocokkan.</p>
	 *
	 * <p><b>Cara kerja.</b> Split by koma, tiap token di-{@code trim()}; token kosong dibuang. Aman untuk
	 * domain tunggal (mengembalikan 1 elemen) maupun {@code null}/kosong (mengembalikan daftar kosong).</p>
	 *
	 * @param domainField isi field {@code domain} (boleh berisi satu atau banyak domain dipisah koma)
	 * @return daftar domain individual yang sudah di-trim &amp; tidak kosong (tak pernah {@code null})
	 */
	public static java.util.List<String> pisahDomain(String domainField) {
		java.util.List<String> hasil = new java.util.ArrayList<String>();
		if (domainField == null) {
			return hasil;
		}
		String[] bagian = domainField.split(",");
		for (int i = 0; i < bagian.length; i++) {
			String d = bagian[i] == null ? "" : bagian[i].trim();
			if (d.length() > 0) {
				hasil.add(d);
			}
		}
		return hasil;
	}

	/**
	 * <h3>Label jenis semester sebuah Perkuliahan: "SP" / "Ganjil" / "Genap"</h3>
	 *
	 * <p><b>Tujuan.</b> Ditampilkan di daftar (kolom SEMESTER) supaya pengguna tahu perkuliahan ini
	 * Ganjil, Genap, atau Semester Pendek (SP).</p>
	 *
	 * <p><b>Logika.</b> (1) bila {@code statusSemesterPendek == SEMESTER_PENDEK} → "SP"; (2) selain itu
	 * pakai {@code ganjilGenap} bila terisi; (3) fallback dari nomor semester (ganjil→Ganjil, genap→Genap).
	 * Mengembalikan string kosong bila perkuliahan {@code null} &amp; data tak cukup.</p>
	 *
	 * @param perkuliahan perkuliahan yang dicek
	 * @return "SP" / "Ganjil" / "Genap" / "" (tak pernah {@code null})
	 */
	public static String labelJenisSemester(ais.database.model.Perkuliahan perkuliahan) {
		if (perkuliahan == null) {
			return "";
		}
		if (perkuliahan.getStatusSemesterPendek() != null
				&& perkuliahan.getStatusSemesterPendek().equals(ais.database.model.Perkuliahan.SEMESTER_PENDEK)) {
			return "SP";
		}
		String gg = perkuliahan.getGanjilGenap();
		if (gg != null && gg.trim().length() > 0) {
			return gg.trim();
		}
		Integer smt = perkuliahan.getSemester();
		if (smt != null) {
			return smt.intValue() % 2 == 0 ? ais.database.model.Perkuliahan.GENAP
					: ais.database.model.Perkuliahan.GANJIL;
		}
		return "";
	}

	/**
	 * <h3>Bangun pesan MULTI-BAHASA berparameter (untuk alert/notifikasi &amp; JSP)</h3>
	 *
	 * <p><b>Tujuan.</b> Mendukung terjemahan pesan yang mengandung nilai dinamis TANPA menjadikan setiap
	 * variasi nilai sebagai kunci terjemahan tersendiri. Teks ditulis sebagai <b>TEMPLATE</b> dengan
	 * placeholder {@code {V1}}, {@code {V2}}, dst. Template inilah yang diterjemahkan via kamus DB
	 * ({@link #getBahasaConfig(String)} — bisa diisi berbeda tiap kampus), lalu placeholder disubstitusi
	 * dengan nilai aktual.</p>
	 *
	 * <p><b>Contoh.</b>
	 * {@code Common.pesan("Data \"{V1}\" berhasil disimpan pada {V2}.", nama, tanggal)}. Cukup satu kunci
	 * terjemahan {@code "Data \"{V1}\" berhasil disimpan pada {V2}."} untuk semua nilai nama/tanggal.</p>
	 *
	 * <p><b>Cara kerja.</b> (1) terjemahkan {@code template} ke bahasa aktif via {@code getBahasaConfig}
	 * (idempotent — aman dipanggil ulang); (2) ganti {@code {V1}}..{@code {Vn}} berurutan dengan
	 * {@code args}. {@code null} menjadi string kosong. Aman untuk {@code args} kosong (tanpa parameter).</p>
	 *
	 * @param template teks template berisi placeholder {@code {V1}},{@code {V2}},... (boleh tanpa placeholder)
	 * @param args     nilai pengganti berurutan untuk {@code {V1}},{@code {V2}},...
	 * @return pesan yang sudah diterjemahkan &amp; tersubstitusi
	 */
	public static String pesan(String template, Object... args) {
		String hasil = getBahasaConfig(template);
		if (hasil != null && args != null) {
			for (int i = 0; i < args.length; i++) {
				hasil = hasil.replace("{V" + (i + 1) + "}", args[i] == null ? "" : String.valueOf(args[i]));
			}
		}
		return hasil == null ? "" : hasil;
	}

	/**
	 * Memeriksa apakah pengguna aktif adalah "admin lain" (admin di luar kategori utama) untuk konteks
	 * saat ini.
	 *
	 * <p><b>Tujuan.</b> Beberapa instalasi membedakan admin "utama" dari admin "lain" (peran admin
	 * tambahan/khusus) yang memiliki cakupan akses berbeda. Method ini memutuskan apakah pengguna
	 * aktif termasuk kategori "lain" tersebut, dipakai untuk menyesuaikan tampilan/aksi.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.getApakahAdminLain()}
	 * yang mengevaluasi pengguna aktif dari sesi.</p>
	 *
	 * <p><b>Return &amp; pemeliharaan.</b> Mengembalikan {@code true}/{@code false}. Untuk menguji
	 * pengguna tertentu (bukan yang aktif), pakai {@link #getApakahAdminLain(Tbmuser)}. Jaga agar
	 * definisi "admin lain" tetap satu sumber di helper.</p>
	 *
	 * @return {@code true} bila pengguna aktif tergolong admin lain
	 */
	public static boolean getApakahAdminLain() {
		return CommonCurrentSessionHelper.getApakahAdminLain();
	}

	/**
	 * Memeriksa apakah {@link Tbmuser} TERTENTU tergolong "admin lain".
	 *
	 * <p><b>Tujuan.</b> Varian eksplisit dari {@link #getApakahAdminLain()} yang menguji pengguna yang
	 * diberikan, bukan pengguna sesi aktif. Berguna pada proses latar/laporan atau saat menilai hak
	 * akses pengguna lain (mis. dalam daftar pengelolaan pengguna).</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.getApakahAdminLain(tbmuser)}
	 * dengan pengguna yang diberikan.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code tbmuser} = pengguna yang diuji (idealnya non-null).
	 * Mengembalikan {@code true} bila tergolong admin lain. Antisipasi perilaku bila {@code tbmuser}
	 * null sesuai implementasi helper.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Gunakan varian ber-parameter ini di konteks non-interaktif agar tidak
	 * bergantung pada sesi aktif.</p>
	 *
	 * @param tbmuser pengguna yang diuji
	 * @return {@code true} bila pengguna tsb tergolong admin lain
	 */
	public static boolean getApakahAdminLain(Tbmuser tbmuser) {
		return CommonCurrentSessionHelper.getApakahAdminLain(tbmuser);
	}

	/**
	 * Mengisi/menyaring sebuah {@link Combobox} program studi sesuai konteks &amp; hak akses pengguna.
	 *
	 * <p><b>Tujuan.</b> Banyak form menampilkan pilihan "program" (program studi/jenjang). Method ini
	 * memuat opsi yang RELEVAN bagi pengguna aktif—mis. hanya program yang ia berhak akses—sehingga
	 * tampilan multi-tenant aman tanpa pengisian manual di tiap form.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.checkProgram(combobox)}
	 * yang menentukan daftar program dari konteks/hak akses pengguna aktif lalu mengisi item combobox
	 * (model berbasis entitas program). Logika dipusatkan agar penyaringan akses konsisten.</p>
	 *
	 * <p><b>Parameter &amp; efek.</b> {@code combobox} = komponen ZK yang akan diisi (dimodifikasi
	 * langsung; tidak ada nilai kembalian). Pemanggil cukup menyediakan komponen kosong.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Varian ini memuat item ber-nilai entitas; bila butuh nilai berupa string
	 * (mis. untuk kriteria/parameter), pakai {@link #checkProgramString(Combobox)}. Jangan mengisi
	 * program secara hardcode di form—gunakan method ini agar mengikuti hak akses.</p>
	 *
	 * @param combobox combobox program yang akan diisi (dimodifikasi langsung)
	 */
	public static void checkProgram(Combobox combobox) {
		CommonCurrentSessionHelper.checkProgram(combobox);
	}

	/**
	 * Mengisi {@link Combobox} program dengan item ber-nilai STRING, sesuai konteks pengguna.
	 *
	 * <p><b>Tujuan.</b> Sama seperti {@link #checkProgram(Combobox)} tetapi item bernilai string—cocok
	 * ketika nilai program akan dipakai sebagai parameter query/kriteria atau disimpan sebagai teks,
	 * bukan sebagai referensi entitas.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.checkProgramString(combobox)}
	 * yang memuat opsi program (string) terfilter hak akses pengguna aktif.</p>
	 *
	 * <p><b>Parameter &amp; efek.</b> {@code combobox} = komponen yang diisi langsung. Tanpa nilai
	 * kembalian.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Untuk menyertakan/menyaring mahasiswa dalam daftar, pakai overload
	 * {@link #checkProgramString(Combobox, boolean)}. Konsisten gunakan method ini agar daftar program
	 * mengikuti aturan akses multi-tenant.</p>
	 *
	 * @param combobox combobox program (nilai string) yang akan diisi
	 */
	public static void checkProgramString(Combobox combobox) {
		CommonCurrentSessionHelper.checkProgramString(combobox);
	}

	/**
	 * Mengisi {@link Combobox} program (nilai string), dengan opsi menyertakan konteks mahasiswa.
	 *
	 * <p><b>Tujuan.</b> Varian {@link #checkProgramString(Combobox)} yang dapat menyesuaikan daftar
	 * program berdasarkan apakah konteks mahasiswa diperhitungkan ({@code termasukMhs})—mis. membatasi
	 * pilihan ke program yang relevan bagi mahasiswa tertentu vs. seluruh program yang dapat diakses
	 * admin.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonCurrentSessionHelper.checkProgramString(combobox, termasukMhs)} yang menentukan
	 * daftar opsi sesuai flag.</p>
	 *
	 * <p><b>Parameter &amp; efek.</b> {@code combobox} = komponen yang diisi langsung;
	 * {@code termasukMhs} = bila {@code true}, perhitungkan konteks mahasiswa dalam menyaring opsi.
	 * Tanpa nilai kembalian.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pilih nilai {@code termasukMhs} sesuai peran pemakai form agar daftar
	 * tidak terlalu sempit/lebar. Logika penyaringan dijaga di helper.</p>
	 *
	 * @param combobox    combobox program (nilai string) yang akan diisi
	 * @param termasukMhs {@code true} untuk memperhitungkan konteks mahasiswa
	 */
	public static void checkProgramString(Combobox combobox, boolean termasukMhs) {
		CommonCurrentSessionHelper.checkProgramString(combobox, termasukMhs);
	}

	/**
	 * Mengambil {@link Menu} yang sedang aktif/dibuka oleh pengguna pada konteks saat ini.
	 *
	 * <p><b>Tujuan.</b> Mengetahui menu aktif penting untuk pemeriksaan hak akses berbasis menu,
	 * penyorotan navigasi, dan pencatatan aktivitas "di menu mana" suatu aksi dilakukan.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonMenuAccessHelper.getCurrentMenu()} yang
	 * menentukan menu dari konteks navigasi/sesi ZK.</p>
	 *
	 * <p><b>Return &amp; pemeliharaan.</b> Mengembalikan {@link Menu} aktif, atau {@code null} bila
	 * tidak ada konteks menu (mis. dipanggil di luar alur menu). Selalu antisipasi {@code null}.</p>
	 *
	 * @return menu aktif saat ini, atau {@code null}
	 */
	public static Menu getCurrentMenu() {
		return CommonMenuAccessHelper.getCurrentMenu();
	}

	/**
	 * Menghasilkan string barcode acak/unik sepanjang {@code digit} digit.
	 *
	 * <p><b>Tujuan.</b> Membuat kode batang untuk keperluan seperti kartu, label, atau identifier
	 * cetak. Memusatkan pembuatannya menjaga format/panjang konsisten.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonGenerateHelper.getGeneratedBarCode(digit)}
	 * yang membangun string sepanjang {@code digit} digit.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code digit} = panjang yang diinginkan. Mengembalikan string
	 * barcode.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Keunikan tidak dijamin lintas waktu hanya dari panjang—bila dipakai
	 * sebagai kunci unik, pasangkan dengan pemeriksaan keunikan di DB (lihat pola
	 * {@code KodeUnikUtil}). Untuk panjang default, pakai {@link #getGeneratedBarCode()}.</p>
	 *
	 * @param digit panjang barcode (jumlah digit)
	 * @return string barcode yang dihasilkan
	 */
	public static String getGeneratedBarCode(int digit) {
		return CommonGenerateHelper.getGeneratedBarCode(digit);
	}

	/**
	 * Menghasilkan string barcode dengan panjang DEFAULT.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Varian tanpa argumen dari {@link #getGeneratedBarCode(int)};
	 * mendelegasikan ke {@code CommonGenerateHelper.getGeneratedBarCode()} memakai panjang baku yang
	 * ditetapkan helper. Dipakai bila pemanggil tidak peduli panjang spesifik.</p>
	 *
	 * <p><b>Return &amp; pemeliharaan.</b> Mengembalikan string barcode panjang default. Untuk
	 * mengontrol panjang, pakai overload ber-parameter. Catatan keunikan sama seperti overload-nya.</p>
	 *
	 * @return string barcode panjang default
	 */
	public static String getGeneratedBarCode() {
		return CommonGenerateHelper.getGeneratedBarCode();
	}

	/**
	 * Menghasilkan string ANGKA acak sepanjang {@code digit} digit.
	 *
	 * <p><b>Tujuan.</b> Membuat rangkaian angka (mis. OTP sederhana, nomor acak, kode numerik) dengan
	 * panjang tertentu—berbeda dari barcode yang dapat berisi format khusus, ini murni digit.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonGenerateHelper.getGeneratedAngkaDigit(digit)}.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code digit} = jumlah digit. Mengembalikan string berisi angka
	 * sepanjang itu.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Bukan untuk keperluan kriptografis (gunakan generator aman bila perlu).
	 * Untuk identifier unik, pasangkan dengan pemeriksaan keunikan.</p>
	 *
	 * @param digit jumlah digit angka
	 * @return string angka yang dihasilkan
	 */
	public static String getGeneratedAngkaDigit(int digit) {
		return CommonGenerateHelper.getGeneratedAngkaDigit(digit);
	}

	/**
	 * Menyinkronkan pilihan fakultas dan jurusan antar-{@link Combobox} (cascading selection).
	 *
	 * <p><b>Tujuan.</b> Pada form akademik, pilihan fakultas dan jurusan saling bergantung (jurusan
	 * harus milik fakultas terpilih). Method ini menangani logika kaskade itu—menyelaraskan ketiga
	 * combobox agar konsisten saat salah satu berubah.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboSelectionHelper.selectFakultasAndJurusan(combobox, jurusan, fakultas)} yang
	 * menyetel/menyaring item jurusan sesuai fakultas (dan/atau sebaliknya) berdasarkan pilihan yang
	 * ada. Komponen dimodifikasi langsung.</p>
	 *
	 * <p><b>Parameter &amp; efek.</b> {@code combobox} = combobox acuan/sumber pilihan; {@code jurusan}
	 * dan {@code fakultas} = combobox yang diselaraskan. Tanpa nilai kembalian.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Urutan/peran ketiga argumen penting—ikuti pemakaian yang ada agar
	 * kaskade benar. Logika relasi fakultas↔jurusan dijaga di helper; jangan menyalin logika kaskade
	 * ke tiap form.</p>
	 *
	 * @param combobox combobox acuan
	 * @param jurusan  combobox jurusan yang diselaraskan
	 * @param fakultas combobox fakultas yang diselaraskan
	 */
	public static void selectFakultasAndJurusan(Combobox combobox, Combobox jurusan, Combobox fakultas) {
		CommonComboSelectionHelper.selectFakultasAndJurusan(combobox, jurusan, fakultas);
	}

	/**
	 * Mengisi {@link Combobox} dengan pilihan JAM (jam perkuliahan/operasional) lalu mengembalikannya.
	 *
	 * <p><b>Tujuan.</b> Menstandarkan pilihan jam di berbagai form (jadwal, presensi, dll.) agar tidak
	 * tiap form membangun daftar jam sendiri-sendiri.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.generateJam(combobox)}
	 * yang menambahkan item-item jam ke combobox sesuai konfigurasi/konteks, lalu mengembalikan
	 * combobox yang sama untuk memudahkan pemakaian berantai.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code combobox} = komponen yang diisi. Mengembalikan combobox
	 * yang sama (sudah terisi).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pola "isi lalu kembalikan komponen" konsisten dengan keluarga
	 * {@code generate*} lain di class ini; pertahankan agar pemanggil dapat merangkai pemanggilan.</p>
	 *
	 * @param combobox combobox yang akan diisi pilihan jam
	 * @return combobox yang sama setelah diisi
	 */
	public static Combobox generateJam(Combobox combobox) {
		return CommonCurrentSessionHelper.generateJam(combobox);
	}

	/**
	 * Membuat SINGKATAN dari sebuah string sumber (mis. nama instansi/unit) .
	 *
	 * <p><b>Tujuan.</b> Menghasilkan akronim/singkatan ringkas untuk tampilan terbatas ruang (header,
	 * label, kode) secara konsisten di seluruh aplikasi.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.generateSingkatan(asal)}
	 * yang menyusun singkatan—umumnya mengambil huruf awal tiap kata penting dari {@code asal}.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code asal} = teks sumber. Mengembalikan string singkatannya.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Aturan pembentukan singkatan (kata yang diabaikan, huruf besar, dll.)
	 * dijaga di helper; ubah di sana agar seragam. Antisipasi {@code asal} null/kosong sesuai
	 * implementasi helper.</p>
	 *
	 * @param asal teks sumber
	 * @return singkatan dari teks sumber
	 */
	public static String generateSingkatan(String asal) {
		return CommonCurrentSessionHelper.generateSingkatan(asal);
	}

	public static java.util.TreeSet<String> tahunAngkatans = new TreeSet<String>();
	public static java.util.TreeSet<Integer> tahunAngkatansData = new TreeSet<Integer>();
	static {
		for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + 5; i >= ais.ui.util.WaktuUtil
				.getCalendar().get(Calendar.YEAR) - 15; i--) {
			String value = (i - 1) + "/" + (i);
			tahunAngkatans.add(value);
			tahunAngkatansData.add(i);
		}
	}

	/**
	 * Menentukan TAHUN AKADEMIK aktif untuk konteks saat ini (mis. "2025/2026").
	 *
	 * <p><b>Tujuan.</b> Tahun akademik adalah dimensi yang memfilter hampir semua data akademik
	 * (KRS, nilai, pembayaran per-periode, dll.). Method ini menyediakan "periode berjalan" sebagai
	 * default agar tampilan/query menyaring data ke tahun yang relevan tanpa hardcode.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.getCurrentTahunAkademik()}
	 * yang menentukan tahun akademik berjalan berdasarkan tanggal sekarang dan/atau konfigurasi periode
	 * institusi (kalender akademik). Logika dipusatkan agar definisi "tahun akademik berjalan" seragam.</p>
	 *
	 * <p><b>Return &amp; pemeliharaan.</b> Mengembalikan string tahun akademik (format institusi).
	 * Untuk menentukan tahun akademik pada TANGGAL atau pengguna/sekolah tertentu, pakai overload yang
	 * sesuai. Jangan menghitung tahun akademik secara manual di pemanggil—gunakan method ini agar
	 * konsisten dengan aturan kalender akademik.</p>
	 *
	 * @return tahun akademik berjalan (format institusi)
	 */
	public static String getCurrentTahunAkademik() {
		return CommonCurrentSessionHelper.getCurrentTahunAkademik();
	}

	/**
	 * Menentukan tahun akademik yang berlaku pada sebuah TANGGAL tertentu.
	 *
	 * <p><b>Tujuan.</b> Memetakan tanggal apa pun (mis. tanggal transaksi/kegiatan historis) ke tahun
	 * akademik yang sesuai, penting untuk pelaporan/penelusuran lintas-periode yang tidak boleh
	 * memakai "tahun berjalan".</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.getCurrentTahunAkademik(tanggal)}
	 * yang menempatkan {@code tanggal} pada kalender akademik institusi.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code tanggal} = tanggal acuan. Mengembalikan string tahun
	 * akademik untuk tanggal tsb.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pakai overload ini untuk data historis; antisipasi {@code tanggal} null
	 * sesuai implementasi helper.</p>
	 *
	 * @param tanggal tanggal acuan
	 * @return tahun akademik yang berlaku pada tanggal tsb
	 */
	public static String getCurrentTahunAkademik(Date tanggal) {
		return CommonCurrentSessionHelper.getCurrentTahunAkademik(tanggal);
	}

	/**
	 * Menentukan tahun akademik pada tanggal tertentu dalam konteks {@link Tbmuser} tertentu.
	 *
	 * <p><b>Tujuan.</b> Tahun akademik dapat bergantung pada konteks pengguna (mis. unit/jenjang yang
	 * berbeda kalender). Overload ini memetakan tanggal ke tahun akademik sesuai konteks pengguna yang
	 * diberikan—berguna pada proses latar yang memproses banyak pengguna.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonCurrentSessionHelper.getCurrentTahunAkademik(tbmuser, tanggal)}.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code tbmuser} = konteks pengguna; {@code tanggal} = tanggal
	 * acuan. Mengembalikan string tahun akademik.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Gunakan bila perhitungan tidak boleh bergantung pada sesi aktif. Pastikan
	 * {@code tbmuser} mewakili konteks yang benar.</p>
	 *
	 * @param tbmuser konteks pengguna
	 * @param tanggal tanggal acuan
	 * @return tahun akademik untuk konteks &amp; tanggal tsb
	 */
	public static String getCurrentTahunAkademik(Tbmuser tbmuser, Date tanggal) {
		return CommonCurrentSessionHelper.getCurrentTahunAkademik(tbmuser, tanggal);
	}

	/**
	 * Menentukan tahun akademik pada tanggal tertentu dalam konteks {@link Sekolah} tertentu.
	 *
	 * <p><b>Tujuan.</b> Untuk lingkungan sekolah (jalur persekolahan), kalender akademik ditentukan
	 * per-{@link Sekolah}. Overload ini memetakan tanggal ke tahun akademik sesuai sekolah yang
	 * diberikan, mendukung multi-sekolah dalam satu sistem.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonCurrentSessionHelper.getCurrentTahunAkademik(sekolah, tanggal)}.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code sekolah} = konteks sekolah; {@code tanggal} = tanggal
	 * acuan. Mengembalikan string tahun akademik.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Jangan mengasumsikan satu kalender global untuk semua sekolah—gunakan
	 * overload ini agar benar di lingkungan multi-sekolah.</p>
	 *
	 * @param sekolah konteks sekolah
	 * @param tanggal tanggal acuan
	 * @return tahun akademik untuk sekolah &amp; tanggal tsb
	 */
	public static String getCurrentTahunAkademik(Sekolah sekolah, Date tanggal) {
		return CommonCurrentSessionHelper.getCurrentTahunAkademik(sekolah, tanggal);
	}

	/**
	 * Mengisi {@link Combobox} dengan daftar TAHUN AJARAN dan mengembalikannya.
	 *
	 * <p><b>Tujuan.</b> Menstandarkan pilihan tahun ajaran (mis. "2024/2025") di banyak form pencarian
	 * &amp; entri, agar rentang dan formatnya konsisten serta selaras dengan kalender akademik.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.generateTahunAjaran(combobox)}
	 * yang menambahkan item tahun ajaran (umumnya rentang beberapa tahun di sekitar tahun berjalan,
	 * lihat juga field statis {@code tahunAngkatans}) lalu mengembalikan combobox tsb.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code combobox} = komponen yang diisi. Mengembalikan combobox
	 * yang sama (terisi) untuk pemakaian berantai.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Untuk varian yang dimulai dari periode Juni–Juli, atau yang menyertakan
	 * opsi "Semua", pakai overload terkait di bawah. Rentang tahun dijaga di helper/field statis.</p>
	 *
	 * @param combobox combobox yang akan diisi tahun ajaran
	 * @return combobox yang sama setelah diisi
	 */
	public static Combobox generateTahunAjaran(Combobox combobox) {
		return CommonCurrentSessionHelper.generateTahunAjaran(combobox);
	}

	/**
	 * Mengisi {@link Combobox} tahun ajaran dengan batas periode Juni–Juli.
	 *
	 * <p><b>Tujuan.</b> Sebagian institusi memulai tahun ajaran pada pertengahan tahun (Juni/Juli).
	 * Varian ini menyusun pilihan tahun ajaran dengan batas tersebut agar penyaringan data sesuai
	 * kalender yang berbeda dari Januari–Desember.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonCurrentSessionHelper.generateTahunAjaranJuniJuli(combobox)}.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code combobox} = komponen yang diisi. Mengembalikan combobox
	 * yang sama.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pilih varian Juni–Juli hanya bila kalender institusi memang demikian;
	 * memilih varian yang salah menggeser penyaringan periode.</p>
	 *
	 * @param combobox combobox yang akan diisi
	 * @return combobox yang sama setelah diisi
	 */
	public static Combobox generateTahunAjaranJuniJuli(Combobox combobox) {
		return CommonCurrentSessionHelper.generateTahunAjaranJuniJuli(combobox);
	}

	/**
	 * Mengisi {@link Combobox} tahun ajaran PLUS opsi "Semua".
	 *
	 * <p><b>Tujuan.</b> Pada form pencarian/laporan sering dibutuhkan opsi "Semua tahun" untuk tidak
	 * menyaring per-tahun. Varian ini menambahkan opsi tersebut di samping daftar tahun ajaran.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonCurrentSessionHelper.generateTahunAjaranDanSemua(combobox)} yang menyisipkan entri
	 * "Semua" (nilai netral) bersama daftar tahun.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code combobox} = komponen yang diisi. Mengembalikan combobox
	 * yang sama.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pastikan logika query pemanggil memperlakukan nilai "Semua" sebagai
	 * "tanpa filter tahun". Pakai pada form pencarian, bukan form entri yang wajib satu tahun.</p>
	 *
	 * @param combobox combobox yang akan diisi
	 * @return combobox yang sama setelah diisi
	 */
	public static Combobox generateTahunAjaranDanSemua(Combobox combobox) {
		return CommonCurrentSessionHelper.generateTahunAjaranDanSemua(combobox);
	}

	/**
	 * Mengisi {@link Combobox} daftar TAHUN (angka) plus opsi "Semua".
	 *
	 * <p><b>Tujuan.</b> Varian untuk pilihan tahun tunggal (bukan format ajaran "x/y") disertai opsi
	 * "Semua"—mis. untuk filter berbasis tahun kalender.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.generateTahunDanSemua(combobox)}
	 * yang mengisi tahun (lihat {@code tahunAngkatansData}) plus entri "Semua".</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code combobox} = komponen yang diisi. Mengembalikan combobox
	 * yang sama.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Bedakan dengan {@link #generateTahunAjaranDanSemua(Combobox)} (format
	 * ajaran). Pilih sesuai kebutuhan filter (tahun tunggal vs tahun ajaran).</p>
	 *
	 * @param combobox combobox yang akan diisi
	 * @return combobox yang sama setelah diisi
	 */
	public static Combobox generateTahunDanSemua(Combobox combobox) {
		return CommonCurrentSessionHelper.generateTahunDanSemua(combobox);
	}

	/**
	 * Mengisi {@link Combobox} dengan daftar TAHUN ANGKATAN (default rentang di sekitar tahun berjalan).
	 *
	 * <p><b>Tujuan.</b> Pilihan angkatan dipakai luas pada data mahasiswa/siswa. Method ini menyusun
	 * daftar angkatan standar agar konsisten lintas form.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.generateTahunAngkatan(combobox)}
	 * yang mengisi rentang angkatan baku (lihat field statis {@code tahunAngkatans}/{@code tahunAngkatansData}
	 * yang dibangun dari tahun berjalan +5 hingga −15).</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code combobox} = komponen yang diisi. Mengembalikan combobox
	 * yang sama.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Untuk menentukan tahun acuan sendiri, pakai
	 * {@link #generateTahunAngkatan(Combobox, Integer)}. Rentang default dijaga di field statis/helper.</p>
	 *
	 * @param combobox combobox yang akan diisi
	 * @return combobox yang sama setelah diisi
	 */
	public static Combobox generateTahunAngkatan(Combobox combobox) {
		return CommonCurrentSessionHelper.generateTahunAngkatan(combobox);
	}

	/**
	 * Mengisi {@link Combobox} tahun angkatan dengan tahun ACUAN tertentu.
	 *
	 * <p><b>Tujuan.</b> Varian {@link #generateTahunAngkatan(Combobox)} yang memungkinkan pemanggil
	 * menetapkan tahun pusat rentang ({@code year})—berguna saat konteksnya bukan tahun berjalan
	 * (mis. menampilkan angkatan relatif terhadap tahun data tertentu).</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonCurrentSessionHelper.generateTahunAngkatan(combobox, year)} yang membangun rentang
	 * angkatan di sekitar {@code year}.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code combobox} = komponen yang diisi; {@code year} = tahun
	 * acuan rentang. Mengembalikan combobox yang sama.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Antisipasi {@code year} null sesuai implementasi helper (kemungkinan
	 * fallback ke tahun berjalan).</p>
	 *
	 * @param combobox combobox yang akan diisi
	 * @param year     tahun acuan rentang angkatan
	 * @return combobox yang sama setelah diisi
	 */
	public static Combobox generateTahunAngkatan(Combobox combobox, Integer year) {
		return CommonCurrentSessionHelper.generateTahunAngkatan(combobox, year);
	}

	/**
	 * Mengisi {@link Combobox} dengan daftar TAHUN KELULUSAN di sekitar tahun acuan.
	 *
	 * <p><b>Tujuan.</b> Pilihan tahun kelulusan dipakai pada data alumni/wisuda. Method ini menyusun
	 * rentang tahun kelulusan yang relevan terhadap {@code year}.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonCurrentSessionHelper.generateTahunKelulusan(combobox, year)} yang membangun rentang
	 * tahun kelulusan di sekitar tahun acuan.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code combobox} = komponen yang diisi; {@code year} = tahun
	 * acuan. Mengembalikan combobox yang sama.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Konsisten dengan keluarga {@code generateTahun*} lainnya (isi lalu
	 * kembalikan komponen). Antisipasi {@code year} null.</p>
	 *
	 * @param combobox combobox yang akan diisi
	 * @param year     tahun acuan rentang kelulusan
	 * @return combobox yang sama setelah diisi
	 */
	public static Combobox generateTahunKelulusan(Combobox combobox, Integer year) {
		return CommonCurrentSessionHelper.generateTahunKelulusan(combobox, year);
	}

	/**
	 * Memeriksa apakah sebuah {@link Menu} ada di dalam sebuah {@link Tree} menu (komponen navigasi ZK).
	 *
	 * <p><b>Tujuan.</b> Dipakai pada kontrol akses/navigasi untuk menentukan apakah suatu menu
	 * tertentu termuat dalam pohon menu yang sedang ditampilkan kepada pengguna—mis. untuk
	 * mengaktifkan/menyorot menu atau memutuskan boleh-tidaknya suatu navigasi.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonMenuAccessHelper.isMenuExist(tree, menu)}
	 * yang menelusuri node-node {@link Tree} dan membandingkannya dengan {@code menu}. Logika
	 * pencarian menu dipusatkan di helper akses-menu.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code tree} = komponen pohon menu ZK; {@code menu} = menu yang
	 * dicari. Mengembalikan {@link Boolean} {@code true} bila menu ditemukan dalam pohon, selain itu
	 * {@code false}.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Untuk pengecekan terhadap koleksi menu (bukan komponen Tree), pakai
	 * {@link #isMenuExist(Collection, Menu)}. Tipe kembalian boxed {@link Boolean} dipertahankan demi
	 * kompatibilitas pemanggil lama—antisipasi auto-unboxing.</p>
	 *
	 * @param tree komponen pohon menu ZK
	 * @param menu menu yang dicari
	 * @return {@code true} bila menu ada di dalam pohon
	 */
	@SuppressWarnings({})
	public static Boolean isMenuExist(Tree tree, Menu menu) {
		return CommonMenuAccessHelper.isMenuExist(tree, menu);
	}

	/**
	 * Memeriksa apakah sebuah {@link Menu} terdapat dalam sebuah {@link Collection} menu.
	 *
	 * <p><b>Tujuan.</b> Varian berbasis koleksi dari {@link #isMenuExist(Tree, Menu)}: berguna saat
	 * daftar menu yang dapat diakses pengguna sudah berupa kumpulan objek (mis. hasil query hak akses)
	 * dan kita ingin tahu apakah suatu menu termasuk di dalamnya.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonMenuAccessHelper.isMenuExist(menus, menu)}
	 * yang memeriksa keanggotaan {@code menu} di {@code menus} (umumnya berdasarkan identitas/ id menu,
	 * bukan sekadar referensi objek).</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code menus} = koleksi menu (mis. menu yang berhak diakses);
	 * {@code menu} = menu yang diuji. Mengembalikan {@code true} bila ditemukan.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pastikan koleksi yang dikirim memang merepresentasikan cakupan yang
	 * dimaksud (mis. menu pengguna aktif). Antisipasi {@code menus} null sesuai implementasi helper.</p>
	 *
	 * @param menus koleksi menu
	 * @param menu  menu yang diuji
	 * @return {@code true} bila menu ada di koleksi
	 */
	public static Boolean isMenuExist(Collection<Menu> menus, Menu menu) {
		return CommonMenuAccessHelper.isMenuExist(menus, menu);
	}

	/**
	 * Memeriksa apakah sebuah string merepresentasikan angka yang valid.
	 *
	 * <p><b>Tujuan.</b> Validasi ringan yang dipakai sangat luas sebelum mem-parse string menjadi
	 * angka (mis. id, key numerik, input form), untuk menghindari {@link NumberFormatException} dan
	 * cabang logika yang keliru. Dipakai antara lain oleh {@code DataUtil.ambilData} untuk memutuskan
	 * apakah key bisa dijadikan id.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonValidationHelper.isNumber(text)} yang
	 * memeriksa pola numerik string. Logika dipusatkan di helper agar definisi "angka" konsisten di
	 * seluruh aplikasi.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code text} = string yang diuji. Mengembalikan {@link Boolean}
	 * {@code true} bila merupakan angka valid, {@code false} bila tidak.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Tipe kembalian {@link Boolean} (boxed) dipertahankan demi kompatibilitas
	 * pemanggil lama; perlakukan dengan aman dalam kondisi auto-unboxing. Bila perlu mendukung format
	 * angka khusus (desimal/locale), ubah di helper, bukan di sini.</p>
	 *
	 * @param text string yang diuji
	 * @return {@code true} bila string adalah angka valid
	 */
	public static Boolean isNumber(String text) {
		return CommonValidationHelper.isNumber(text);
	}

	/**
	 * Mengambil {@link Tbmuser} aktif langsung dari atribut sesi ZK ({@code "users"}).
	 *
	 * <p><b>Tujuan.</b> Akses cepat ke pengguna login yang disimpan di sesi ZK saat ini. Berbeda dari
	 * {@link #getCurrentUser()} yang melalui helper, varian ini membaca atribut sesi secara langsung.</p>
	 *
	 * <p><b>Cara kerja.</b> Mengambil sesi ZK saat ini ({@code Sessions.getCurrent()}) lalu mengembalikan
	 * atribut {@code "users"} yang di-cast ke {@link Tbmuser}.</p>
	 *
	 * <p><b>Return &amp; penanganan error.</b> Mengembalikan {@link Tbmuser} bila ada di sesi.
	 * <b>Perhatian:</b> bila tidak ada konteks sesi ZK ({@code Sessions.getCurrent()} null) atau
	 * atribut belum diset, pemanggilan dapat menghasilkan {@code null} atau melempar—gunakan di dalam
	 * konteks request ZK yang sudah pasti memiliki sesi.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Untuk akses yang lebih defensif/portabel lintas konteks, lebih baik
	 * memakai {@link #getCurrentUser()}. Pertahankan nama atribut {@code "users"} konsisten dengan
	 * tempat ia diset saat login.</p>
	 *
	 * @return pengguna aktif dari sesi ZK, atau {@code null} bila tidak ada
	 */
	public static Tbmuser getTbmuser() {
		return (Tbmuser) Sessions.getCurrent().getAttribute("users");
	}

	/**
	 * Membangun/mengisi sebuah {@link Combobox} dengan daftar PROGRAM (program studi/jenjang) dari
	 * cache program, lalu menyaringnya sesuai hak akses.
	 *
	 * <p><b>Tujuan.</b> Menyediakan combobox program yang siap pakai di form pencarian/entri, dengan
	 * label ramah-pengguna dan nilai berupa kode program—sambil memastikan daftar terfilter sesuai
	 * hak akses pengguna aktif (multi-tenant).</p>
	 *
	 * <h4>Cara kerja</h4>
	 * <ol>
	 *   <li>Bila cache statis {@code Common.programs} kosong, panggil {@code reInitProgram()} untuk
	 *   memuatnya (lazy init).</li>
	 *   <li>Bila {@code searchprogram} null, buat {@link Combobox} baru; bila tidak,
	 *   {@code Common.clear(...)} dulu agar tidak ada item ganda saat dipanggil ulang.</li>
	 *   <li>Iterasi {@code programs}: untuk tiap kode program, buat {@code MyComboitemConfig} dengan
	 *   label = {@code Program.getNamaBaru()} (atau kode bila objek null) dan value = kode program.</li>
	 *   <li>Terakhir {@link #checkProgramString(Combobox)} untuk menyaring item sesuai hak akses
	 *   pengguna, lalu mengembalikan combobox.</li>
	 * </ol>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code searchprogram} = combobox yang akan diisi (boleh null →
	 * dibuat baru). Mengembalikan combobox terisi &amp; terfilter.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Berbeda dari {@link #checkProgramString(Combobox)} yang murni delegasi,
	 * method ini punya LOGIKA NYATA (lazy-init cache, clear, pembentukan item) — perubahan label/nilai
	 * item program sebaiknya dilakukan di sini. Pemanggilan ulang aman karena ada {@code clear}.</p>
	 *
	 * @param searchprogram combobox yang akan diisi (boleh null)
	 * @return combobox terisi daftar program &amp; terfilter hak akses
	 */
	public static Combobox initPrograms(Combobox searchprogram) {
		if (Common.programs.isEmpty()) {
			Common.reInitProgram();
		}
		if (searchprogram == null) {
			searchprogram = new Combobox();
		} else {
			Common.clear(searchprogram);
		}
		for (String p : Common.programs.keySet()) {
			MyComboitemConfig comboitem = new MyComboitemConfig();
			Program pp = Common.programs.get(p);
			comboitem.setLabel(pp == null ? p : pp.getNamaBaru());
			comboitem.setValue(p);
			searchprogram.appendChild(comboitem);
		}
		Common.checkProgramString(searchprogram);
		return searchprogram;
	}

	/**
	 * Mengisi BANYAK {@link Combobox} sekaligus, masing-masing dari entitas yang ditentukan.
	 *
	 * <p><b>Tujuan.</b> Banyak form punya beberapa combobox referensi (mis. fakultas, jurusan,
	 * program) yang perlu diisi saat inisialisasi. Method ini mengisi semuanya dalam satu panggilan,
	 * mengurangi kode boilerplate berulang di {@code doAfterCompose}.</p>
	 *
	 * <p><b>Cara kerja.</b> No-op bila {@code comboboxs} null. Untuk tiap indeks {@code i}, memanggil
	 * {@link #insertCombo(Combobox, String, Class)} dengan combobox, properti label, dan kelas entitas
	 * yang berkorespondensi. Ketiga array di-iterasi paralel berdasarkan posisi.</p>
	 *
	 * <p><b>Parameter.</b> {@code comboboxs} = array komponen; {@code properties} = array nama properti
	 * yang dijadikan label; {@code classes} = array kelas entitas sumber data. Ketiganya HARUS sejajar
	 * (panjang &amp; urutan sama).</p>
	 *
	 * <p><b>Pemeliharaan &amp; risiko.</b> Tidak ada pengecekan kesamaan panjang array—bila
	 * {@code properties}/{@code classes} lebih pendek dari {@code comboboxs}, akan terjadi
	 * {@link ArrayIndexOutOfBoundsException}. Pastikan ketiganya konsisten. Untuk menambahkan kriteria
	 * filter per-combobox, pakai overload {@link #initCombos(Combobox[], String[], Class[], Criterion[])}.</p>
	 *
	 * @param comboboxs  array combobox yang diisi
	 * @param properties array nama properti label (sejajar)
	 * @param classes    array kelas entitas sumber (sejajar)
	 */
	@SuppressWarnings("rawtypes")
	public static void initCombos(Combobox[] comboboxs, String[] properties, Class[] classes) {
		if (comboboxs == null)
			return;
		for (int i = 0; i < comboboxs.length; i++) {
			if (!parameterInitCombosLengkap(i, comboboxs, properties, classes, null)) {
				continue;
			}
			insertCombo(comboboxs[i], properties[i], classes[i]);
		}
	}

	/**
	 * Periksa kelengkapan parameter satu baris {@code initCombos} sebelum dipakai.
	 *
	 * <p><b>Alasan.</b> {@code initCombos} DULU langsung mengindeks {@code properties[i]},
	 * {@code classes[i]}, dan {@code criterions[i]} hanya berbekal panjang {@code comboboxs}.
	 * Bila pemanggil mengirim array yang lebih pendek (atau null) -- mudah terjadi karena
	 * ketiga/keempat array harus dijaga sejajar secara manual -- yang muncul adalah
	 * NullPointerException/ArrayIndexOutOfBoundsException di dalam Common, sehingga SELURUH
	 * combobox pada layar itu gagal terisi dan layar tampak kosong tanpa petunjuk apa pun.</p>
	 *
	 * <p><b>Perilaku sekarang.</b> Baris yang parameternya tidak lengkap DILEWATI (combobox lain
	 * tetap terisi seperti biasa) dan ketidaksesuaiannya dicatat sekali ke audit agar bug
	 * pemanggilnya tetap terlihat -- bukan disembunyikan. Untuk pemanggil yang arraynya sudah
	 * benar, tidak ada perubahan perilaku sama sekali.</p>
	 */
	@SuppressWarnings({ "rawtypes" })
	private static boolean parameterInitCombosLengkap(int index, Combobox[] comboboxs, String[] properties,
			Class[] classes, Criterion[] criterions) {
		String kurang = null;
		if (properties == null || index >= properties.length) {
			kurang = "properties";
		} else if (classes == null || index >= classes.length) {
			kurang = "classes";
		} else if (criterions != null && index >= criterions.length) {
			kurang = "criterions";
		}
		if (kurang == null) {
			return true;
		}
		ais.common.ErrorAuditUtil.record(
				new Exception("initCombos: array '" + kurang + "' lebih pendek dari comboboxs (butuh index " + index
						+ ", comboboxs=" + (comboboxs == null ? 0 : comboboxs.length)
						+ ", properties=" + (properties == null ? -1 : properties.length)
						+ ", classes=" + (classes == null ? -1 : classes.length)
						+ ", criterions=" + (criterions == null ? -1 : criterions.length) + ")"),
				"auto-audit src/ais/common/Common.java:initCombos-parameter-tidak-sejajar");
		return false;
	}

	/**
	 * Mengisi banyak {@link Combobox} sekaligus DENGAN kriteria filter per-combobox.
	 *
	 * <p><b>Tujuan.</b> Sama seperti {@link #initCombos(Combobox[], String[], Class[])}, tetapi tiap
	 * combobox dapat memiliki {@link Criterion} penyaring sendiri—mis. hanya data aktif, atau hanya
	 * milik unit tertentu. Berguna ketika daftar referensi perlu dipersempit per-konteks.</p>
	 *
	 * <p><b>Cara kerja.</b> No-op bila {@code comboboxs} null. Untuk tiap indeks, memanggil
	 * {@link #insertCombo(Combobox, String, Class, Criterion...)} dengan kriteria yang berkorespondensi
	 * ({@code criterions[i]}). Keempat array di-iterasi paralel.</p>
	 *
	 * <p><b>Parameter.</b> {@code comboboxs}, {@code properties}, {@code classes}, dan
	 * {@code criterions} = array sejajar (komponen, properti label, kelas entitas, kriteria filter).</p>
	 *
	 * <p><b>Pemeliharaan &amp; risiko.</b> Seperti overload tanpa kriteria, panjang array harus
	 * konsisten untuk menghindari {@link ArrayIndexOutOfBoundsException}. Pastikan tiap
	 * {@code criterions[i]} cocok dengan {@code classes[i]} (properti yang dirujuk ada di entitas itu).</p>
	 *
	 * @param comboboxs  array combobox yang diisi
	 * @param properties array nama properti label (sejajar)
	 * @param classes    array kelas entitas sumber (sejajar)
	 * @param criterions array kriteria filter per-combobox (sejajar)
	 */
	@SuppressWarnings({ "rawtypes" })
	public static void initCombos(Combobox[] comboboxs, String[] properties, Class[] classes, Criterion[] criterions) {
		if (comboboxs == null)
			return;
		for (int i = 0; i < comboboxs.length; i++) {
			if (!parameterInitCombosLengkap(i, comboboxs, properties, classes, criterions)) {
				continue;
			}
			insertCombo(comboboxs[i], properties[i], classes[i], criterions[i]);
		}
	}

	/**
	 * Mengisi {@link Combobox} dengan item dari sebuah entitas, label dari satu {@code property},
	 * difilter NOL ATAU LEBIH {@link Criterion}.
	 *
	 * <p><b>Tujuan.</b> Method paling umum dari keluarga {@code insertCombo}: memuat data referensi
	 * ke combobox (mis. daftar jurusan/ruang/jenis) tanpa pemanggil perlu menulis query/iterasi item
	 * sendiri. Varargs {@code criterions} memungkinkan penyaringan fleksibel (0..n syarat).</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertCombo(combobox, property, clazz, criterions)} yang membuka
	 * query atas {@code clazz}, menerapkan semua {@code criterions}, lalu menambahkan item ke combobox
	 * dengan label = nilai {@code property} tiap baris dan value = entitasnya. Komponen diisi langsung
	 * (tanpa nilai kembalian).</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox} = komponen target; {@code property} = nama properti yang
	 * dijadikan label; {@code clazz} = kelas entitas sumber; {@code criterions} = filter opsional.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Bila sudah memegang {@link Criteria} dari luar (mis. agar tidak membuka
	 * session baru), pakai overload {@link #insertCombo(Combobox, String, Class, Criteria)}. Pastikan
	 * {@code property} ada di {@code clazz}. Tipe raw {@code Class} dipertahankan demi pemanggil lama.</p>
	 *
	 * @param combobox   komponen target
	 * @param property   nama properti label
	 * @param clazz      kelas entitas sumber
	 * @param criterions filter opsional (0..n)
	 */
	@SuppressWarnings({ "rawtypes" })
	public static void insertCombo(Combobox combobox, String property, Class clazz, Criterion... criterions) {
		CommonComboInsertHelper.insertCombo(combobox, property, clazz, criterions);
	}

	/**
	 * Mengisi {@link Combobox} dari sebuah entitas dengan SATU {@link Criterion} filter.
	 *
	 * <p><b>Tujuan.</b> Varian yang menerima tepat satu kriteria—bentuk paling sering dipakai ketika
	 * penyaringan cukup satu syarat (mis. {@code Restrictions.eq("aktif", true)}). Lebih eksplisit
	 * daripada varargs untuk kasus tunggal.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertCombo(combobox, property, clazz, criterion)} yang memuat data
	 * terfilter ke combobox (label dari {@code property}, value entitas).</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox}/{@code property}/{@code clazz} seperti overload umum;
	 * {@code criterion} = satu syarat filter.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Untuk urutan hasil tertentu, pakai overload yang menerima {@link Order}.
	 * Untuk tanpa filter, pakai {@link #insertCombo(Combobox, String, Class)}.</p>
	 *
	 * @param combobox  komponen target
	 * @param property  nama properti label
	 * @param clazz     kelas entitas sumber
	 * @param criterion satu syarat filter
	 */
	public static void insertCombo(Combobox combobox, String property, Class<?> clazz, Criterion criterion) {
		CommonComboInsertHelper.insertCombo(combobox, property, clazz, criterion);
	}

	/**
	 * Mengisi {@link Combobox} dari sebuah entitas dengan filter sekaligus URUTAN ({@link Order}).
	 *
	 * <p><b>Tujuan.</b> Selain menyaring, varian ini mengatur URUTAN tampilan item (mis. menurut nama
	 * A→Z, atau menurut kode). Penting untuk daftar yang harus tampil terurut agar mudah dibaca/dipilih
	 * pengguna.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertCombo(combobox, property, clazz, order, criterion)} yang
	 * menerapkan {@code order} dan {@code criterion} pada query lalu mengisi combobox.</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox}/{@code property}/{@code clazz} seperti biasa; {@code order}
	 * = urutan hasil; {@code criterion} = syarat filter.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pastikan properti yang dipakai {@code order} ada di {@code clazz}.
	 * Bila tidak butuh urutan khusus, pakai overload tanpa {@link Order}.</p>
	 *
	 * @param combobox  komponen target
	 * @param property  nama properti label
	 * @param clazz     kelas entitas sumber
	 * @param order     urutan hasil
	 * @param criterion syarat filter
	 */
	public static void insertCombo(Combobox combobox, String property, Class<?> clazz, Order order,
			Criterion criterion) {
		CommonComboInsertHelper.insertCombo(combobox, property, clazz, order, criterion);
	}

	/**
	 * Mengisi {@link Combobox} dengan label GABUNGAN beberapa bagian, memakai pemisah {@code deskripsi}.
	 *
	 * <p><b>Tujuan.</b> Kadang label item perlu lebih informatif daripada satu properti—mis.
	 * "Kode - Nama". Varian ini menyisipkan {@code deskripsi} sebagai pemisah/format tambahan pada
	 * label, sambil tetap mendukung filter varargs.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertCombo(combobox, property, deskripsi, clazz, criterions)} yang
	 * memuat data terfilter dan menyusun label memakai {@code property} beserta {@code deskripsi}.</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox}/{@code property}/{@code clazz}/{@code criterions} seperti
	 * overload umum; {@code deskripsi} = teks pemisah/format label tambahan.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Makna persis {@code deskripsi} (pemisah vs. properti kedua) ditentukan
	 * helper—ikuti pemakaian yang ada. Tipe raw {@code Class} dipertahankan demi pemanggil lama.</p>
	 *
	 * @param combobox   komponen target
	 * @param property   nama properti label utama
	 * @param deskripsi  teks pemisah/format label tambahan
	 * @param clazz      kelas entitas sumber
	 * @param criterions filter opsional (0..n)
	 */
	@SuppressWarnings({ "rawtypes" })
	public static void insertCombo(Combobox combobox, String property, String deskripsi, Class clazz,
			Criterion... criterions) {
		CommonComboInsertHelper.insertCombo(combobox, property, deskripsi, clazz, criterions);
	}

	/**
	 * Mengisi {@link Combobox} memakai {@link Criteria} yang SUDAH dibuat pemanggil (tidak membuka
	 * session baru).
	 *
	 * <p><b>Tujuan.</b> Untuk kasus di mana pemanggil sudah memegang {@code Criteria} (mis. dibangun
	 * dari session/transaksi tertentu), varian ini memakainya langsung—penting agar TIDAK membuka
	 * session baru yang dapat memboroskan koneksi pool atau memutus konteks transaksi.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertCombo(combobox, property, clazz, criteria)} yang
	 * mengeksekusi {@code criteria} apa adanya lalu mengisi item combobox (label dari {@code property}).</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox}/{@code property}/{@code clazz} seperti biasa;
	 * {@code criteria} = kriteria siap-eksekusi milik pemanggil.</p>
	 *
	 * <p><b>Pemeliharaan (PENTING).</b> Seperti tertulis di kode, method ini menerima {@link Criteria}
	 * dari luar dan TIDAK boleh membuka session baru—jangan mengubahnya menjadi membuka session
	 * sendiri. Siklus hidup session yang menaungi {@code criteria} dikelola pemanggil.</p>
	 *
	 * @param combobox komponen target
	 * @param property nama properti label
	 * @param clazz    kelas entitas sumber
	 * @param criteria kriteria siap-eksekusi milik pemanggil
	 */
	public static void insertCombo(Combobox combobox, String property, Class<?> clazz, Criteria criteria) {
		CommonComboInsertHelper.insertCombo(combobox, property, clazz, criteria);
	}

	/**
	 * Mengisi {@link Combobox} dari sebuah entitas TANPA filter (semua baris).
	 *
	 * <p><b>Tujuan.</b> Bentuk paling sederhana: memuat SELURUH baris entitas {@code clazz} ke
	 * combobox. Cocok untuk daftar referensi kecil yang tidak perlu disaring (mis. daftar status
	 * tetap).</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertCombo(combobox, property, clazz)} yang memuat semua data dan
	 * mengisi item (label dari {@code property}, value entitas).</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox} = target; {@code property} = properti label; {@code clazz}
	 * = entitas sumber.</p>
	 *
	 * <p><b>Pemeliharaan &amp; kinerja.</b> Hindari untuk entitas BERVOLUME BESAR—memuat semua baris ke
	 * combobox boros memori &amp; lambat; gunakan overload ber-{@link Criterion} untuk menyaring.</p>
	 *
	 * @param combobox komponen target
	 * @param property nama properti label
	 * @param clazz    kelas entitas sumber
	 */
	public static void insertCombo(Combobox combobox, String property, Class<?> clazz) {
		CommonComboInsertHelper.insertCombo(combobox, property, clazz);
	}

	/**
	 * Mengisi {@link Combobox} memakai komponen item ber-konfigurasi khusus ({@code MyConfig}),
	 * dengan filter opsional.
	 *
	 * <p><b>Tujuan.</b> Varian yang membangun item combobox memakai tipe komponen "MyConfig" internal
	 * (mis. {@code MyComboitemConfig}) alih-alih comboitem standar—memberi perilaku/tampilan konsisten
	 * dengan komponen kustom AIS. Dipakai bila combobox perlu fitur tambahan yang dibawa komponen
	 * MyConfig.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertComboMyConfig(combobox, property, clazz, criterions)} yang
	 * memuat data terfilter dan menambahkan item ber-MyConfig.</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox}/{@code property}/{@code clazz} seperti biasa;
	 * {@code criterions} = filter opsional (0..n).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pilih varian MyConfig hanya bila memang perlu komponen item kustom;
	 * untuk item standar gunakan {@code insertCombo} biasa agar lebih ringan.</p>
	 *
	 * @param combobox   komponen target
	 * @param property   nama properti label
	 * @param clazz      kelas entitas sumber
	 * @param criterions filter opsional (0..n)
	 */
	public static void insertComboMyConfig(Combobox combobox, String property, Class<?> clazz,
			Criterion... criterions) {
		CommonComboInsertHelper.insertComboMyConfig(combobox, property, clazz, criterions);
	}

	/**
	 * Mengisi {@link Combobox} memakai item ber-konfigurasi khusus ({@code MyConfig}) TANPA filter.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Sama seperti
	 * {@link #insertComboMyConfig(Combobox, String, Class, Criterion...)} namun memuat SEMUA baris
	 * (tanpa kriteria). Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertComboMyConfig(combobox, property, clazz)}.</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox} = target; {@code property} = properti label; {@code clazz}
	 * = entitas sumber.</p>
	 *
	 * <p><b>Pemeliharaan &amp; kinerja.</b> Sama dengan varian tanpa filter lainnya—hindari untuk
	 * entitas bervolume besar. Pakai komponen MyConfig hanya bila perilaku kustomnya dibutuhkan.</p>
	 *
	 * @param combobox komponen target
	 * @param property nama properti label
	 * @param clazz    kelas entitas sumber
	 */
	public static void insertComboMyConfig(Combobox combobox, String property, Class<?> clazz) {
		CommonComboInsertHelper.insertComboMyConfig(combobox, property, clazz);
	}

	/**
	 * Mengisi {@link Combobox} dari sebuah entitas dengan tambahan opsi "Semua", memakai {@link Criteria}
	 * dari pemanggil (tidak membuka session baru).
	 *
	 * <p><b>Tujuan.</b> Sama seperti {@code insertCombo} biasa, namun menambahkan satu item "Semua"
	 * (nilai netral/kosong) di awal daftar—pola umum pada form PENCARIAN agar pengguna bisa memilih
	 * "tidak menyaring berdasarkan field ini". Varian ini memakai {@link Criteria} yang sudah dibangun
	 * pemanggil.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertComboDanSemua(combobox, property, clazz, criteria)} yang
	 * menyisipkan item "Semua" lalu mengeksekusi {@code criteria} dan menambahkan item data (label dari
	 * {@code property}). Komponen diisi langsung.</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox}/{@code property}/{@code clazz} seperti biasa; {@code criteria}
	 * = kriteria siap-eksekusi milik pemanggil.</p>
	 *
	 * <p><b>Pemeliharaan (PENTING).</b> Seperti dicatat di kode, menerima {@link Criteria} dari luar dan
	 * TIDAK boleh membuka session baru. Pastikan logika query memperlakukan nilai item "Semua" sebagai
	 * "tanpa filter". Pakai pada form pencarian, bukan entri wajib.</p>
	 *
	 * @param combobox komponen target
	 * @param property nama properti label
	 * @param clazz    kelas entitas sumber
	 * @param criteria kriteria siap-eksekusi milik pemanggil
	 */
	public static void insertComboDanSemua(Combobox combobox, String property, Class<?> clazz, Criteria criteria) {
		CommonComboInsertHelper.insertComboDanSemua(combobox, property, clazz, criteria);
	}

	/**
	 * Mengisi {@link Combobox} dari sebuah entitas dengan opsi "Semua", TANPA filter (semua baris).
	 *
	 * <p><b>Tujuan.</b> Bentuk paling sederhana dari keluarga "DanSemua": memuat seluruh baris entitas
	 * plus item "Semua" di awal. Cocok untuk filter pencarian atas referensi kecil.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertComboDanSemua(combobox, property, clazz)} yang menyisipkan
	 * "Semua" lalu seluruh data (label dari {@code property}).</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox} = target; {@code property} = properti label; {@code clazz}
	 * = entitas sumber.</p>
	 *
	 * <p><b>Pemeliharaan &amp; kinerja.</b> Hindari untuk entitas bervolume besar (memuat semua baris).
	 * Untuk menyaring, pakai overload ber-{@link Criterion}/{@link Criteria}.</p>
	 *
	 * @param combobox komponen target
	 * @param property nama properti label
	 * @param clazz    kelas entitas sumber
	 */
	public static void insertComboDanSemua(Combobox combobox, String property, Class<?> clazz) {
		CommonComboInsertHelper.insertComboDanSemua(combobox, property, clazz);
	}

	/**
	 * Mengisi {@link Combobox} dengan opsi "Semua" dan LABEL "Semua" yang dapat disesuaikan
	 * ({@code keterangan}), tanpa filter.
	 *
	 * <p><b>Tujuan.</b> Memungkinkan teks item netral diubah dari sekadar "Semua" menjadi sesuatu yang
	 * lebih kontekstual (mis. "Semua Jurusan", "- Pilih -"), sehingga UI lebih jelas bagi pengguna.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertComboDanSemua(combobox, property, keterangan, clazz)} yang
	 * memakai {@code keterangan} sebagai label item netral, lalu memuat seluruh data.</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox}/{@code property}/{@code clazz} seperti biasa;
	 * {@code keterangan} = label untuk item "Semua"/netral.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pastikan nilai item netral tetap dikenali sebagai "tanpa filter" oleh
	 * query. Untuk menambah penyaringan, pakai overload ber-{@link Criterion}.</p>
	 *
	 * @param combobox   komponen target
	 * @param property   nama properti label
	 * @param keterangan label item netral ("Semua")
	 * @param clazz      kelas entitas sumber
	 */
	public static void insertComboDanSemua(Combobox combobox, String property, String keterangan, Class<?> clazz) {
		CommonComboInsertHelper.insertComboDanSemua(combobox, property, keterangan, clazz);
	}

	/**
	 * Mengisi {@link Combobox} dengan opsi "Semua" berlabel kustom DAN satu {@link Criterion} filter.
	 *
	 * <p><b>Tujuan.</b> Gabungan dua kebutuhan: label item netral yang ramah ({@code keterangan}) dan
	 * penyaringan data dengan satu syarat ({@code criterion})—mis. "Semua Kelas Aktif" yang hanya
	 * memuat kelas berstatus aktif.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertComboDanSemua(combobox, property, keterangan, clazz, criterion)}
	 * yang menyisipkan item netral berlabel {@code keterangan} lalu memuat data terfilter.</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox}/{@code property}/{@code clazz} seperti biasa;
	 * {@code keterangan} = label item netral; {@code criterion} = syarat filter.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pastikan {@code criterion} merujuk properti yang ada di {@code clazz}.</p>
	 *
	 * @param combobox   komponen target
	 * @param property   nama properti label
	 * @param keterangan label item netral ("Semua")
	 * @param clazz      kelas entitas sumber
	 * @param criterion  syarat filter
	 */
	public static void insertComboDanSemua(Combobox combobox, String property, String keterangan, Class<?> clazz,
			Criterion criterion) {
		CommonComboInsertHelper.insertComboDanSemua(combobox, property, keterangan, clazz, criterion);
	}

	/**
	 * Mengisi {@link Combobox} dengan opsi "Semua" (label default) DAN satu {@link Criterion} filter.
	 *
	 * <p><b>Tujuan.</b> Varian "DanSemua" dengan label netral baku tetapi tetap menyaring data dengan
	 * satu syarat. Bentuk yang paling sering dipakai pada filter pencarian atas data terbatas (mis.
	 * hanya yang aktif).</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertComboDanSemua(combobox, property, clazz, criterion)} yang
	 * menyisipkan item "Semua" lalu memuat data terfilter.</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox}/{@code property}/{@code clazz} seperti biasa;
	 * {@code criterion} = syarat filter.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Untuk label item netral kustom, pakai overload yang menerima
	 * {@code keterangan}.</p>
	 *
	 * @param combobox  komponen target
	 * @param property  nama properti label
	 * @param clazz     kelas entitas sumber
	 * @param criterion syarat filter
	 */
	public static void insertComboDanSemua(Combobox combobox, String property, Class<?> clazz, Criterion criterion) {
		CommonComboInsertHelper.insertComboDanSemua(combobox, property, clazz, criterion);
	}

	/**
	 * Mengisi {@link Combobox} dari sebuah entitas (tanpa filter) sambil menetapkan {@code style} CSS
	 * pada item/komponen.
	 *
	 * <p><b>Tujuan.</b> Selain memuat data, varian ini menyetel gaya tampilan ({@code style})—mis.
	 * lebar, warna, font—agar combobox tertentu tampil sesuai kebutuhan UI tanpa pengaturan terpisah.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertCombo(combobox, property, clazz, style)} yang memuat data
	 * (label dari {@code property}) dan menerapkan {@code style}.</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox}/{@code property}/{@code clazz} seperti biasa; {@code style}
	 * = string CSS inline.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Utamakan styling lewat sclass/CSS terpusat bila memungkinkan agar
	 * konsisten; inline {@code style} cocok untuk penyesuaian setempat.</p>
	 *
	 * @param combobox komponen target
	 * @param property nama properti label
	 * @param clazz    kelas entitas sumber
	 * @param style    string CSS inline
	 */
	public static void insertCombo(Combobox combobox, String property, Class<?> clazz, String style) {
		CommonComboInsertHelper.insertCombo(combobox, property, clazz, style);
	}

	/**
	 * Mengisi {@link Combobox} (tanpa filter) dengan label gabungan {@code property} + {@code deskripsi}.
	 *
	 * <p><b>Tujuan.</b> Varian tanpa filter yang menyusun label lebih informatif memakai {@code deskripsi}
	 * sebagai pemisah/format tambahan (mis. "Kode - Nama"), memuat seluruh baris entitas.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertCombo(combobox, property, deskripsi, clazz)} yang memuat data
	 * dan menyusun label dari {@code property} beserta {@code deskripsi}.</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox}/{@code property}/{@code clazz} seperti biasa; {@code deskripsi}
	 * = teks pemisah/format label.</p>
	 *
	 * <p><b>Pemeliharaan &amp; kinerja.</b> Tanpa filter—hindari untuk entitas besar. Makna persis
	 * {@code deskripsi} ditentukan helper; ikuti pemakaian yang ada.</p>
	 *
	 * @param combobox  komponen target
	 * @param property  nama properti label utama
	 * @param deskripsi teks pemisah/format label tambahan
	 * @param clazz     kelas entitas sumber
	 */
	public static void insertCombo(Combobox combobox, String property, String deskripsi, Class<?> clazz) {
		CommonComboInsertHelper.insertCombo(combobox, property, deskripsi, clazz);
	}

	/**
	 * Mengisi {@link Combobox} dengan label gabungan dari BEBERAPA properti, difilter varargs
	 * {@link Criterion}.
	 *
	 * <p><b>Tujuan.</b> Varian "multi-properti": label tiap item disusun dari beberapa field sekaligus
	 * (mis. {@code {"kode","nama"}} → "001 - Budi"). Berguna ketika satu kolom saja tidak cukup
	 * mengidentifikasi pilihan bagi pengguna.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertCombo(combobox, properties, clazz, criterions)} yang memuat
	 * data terfilter dan menyusun label dari array {@code properties} (urutan menentukan susunan label).</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox} = target; {@code properties} = array nama properti label;
	 * {@code clazz} = entitas sumber; {@code criterions} = filter opsional (0..n).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Semua nama di {@code properties} harus ada di {@code clazz}. Format
	 * penggabungan (pemisah) ditentukan helper. Tipe raw {@code Class} demi pemanggil lama.</p>
	 *
	 * @param combobox   komponen target
	 * @param properties array nama properti label
	 * @param clazz      kelas entitas sumber
	 * @param criterions filter opsional (0..n)
	 */
	@SuppressWarnings({ "rawtypes" })
	public static void insertCombo(Combobox combobox, String[] properties, Class clazz, Criterion... criterions) {
		CommonComboInsertHelper.insertCombo(combobox, properties, clazz, criterions);
	}

	/**
	 * Mengisi {@link Combobox} dengan label multi-properti dan SATU {@link Criterion} filter.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Varian multi-properti dengan tepat satu syarat filter;
	 * mendelegasikan ke {@code CommonComboInsertHelper.insertCombo(combobox, properties, clazz, criterion)}.
	 * Label disusun dari {@code properties}, data disaring oleh {@code criterion}.</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox}/{@code properties}/{@code clazz} seperti varian varargs;
	 * {@code criterion} = satu syarat filter.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Untuk 0 atau &gt;1 syarat, pakai overload varargs. Pastikan properti
	 * label &amp; properti yang difilter ada di {@code clazz}.</p>
	 *
	 * @param combobox   komponen target
	 * @param properties array nama properti label
	 * @param clazz      kelas entitas sumber
	 * @param criterion  satu syarat filter
	 */
	public static void insertCombo(Combobox combobox, String[] properties, Class<?> clazz, Criterion criterion) {
		CommonComboInsertHelper.insertCombo(combobox, properties, clazz, criterion);
	}

	/**
	 * Mengisi {@link Combobox} dengan label multi-properti + {@code deskripsi}, difilter varargs.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Menggabungkan beberapa properti DAN teks {@code deskripsi}
	 * (pemisah/format tambahan) untuk membentuk label, dengan filter 0..n. Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertCombo(combobox, properties, deskripsi, clazz, criterions)}.</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox}/{@code properties}/{@code clazz}/{@code criterions} seperti
	 * varian multi-properti; {@code deskripsi} = teks pemisah/format label.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Makna {@code deskripsi} ditentukan helper; ikuti pemakaian yang ada.</p>
	 *
	 * @param combobox   komponen target
	 * @param properties array nama properti label
	 * @param deskripsi  teks pemisah/format label
	 * @param clazz      kelas entitas sumber
	 * @param criterions filter opsional (0..n)
	 */
	@SuppressWarnings({ "rawtypes" })
	public static void insertCombo(Combobox combobox, String[] properties, String deskripsi, Class clazz,
			Criterion... criterions) {
		CommonComboInsertHelper.insertCombo(combobox, properties, deskripsi, clazz, criterions);
	}

	/**
	 * Mengisi {@link Combobox} multi-properti + {@code deskripsi}, dengan opsi "Semua", difilter varargs.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Gabungan label multi-properti, format {@code deskripsi}, dan
	 * item netral "Semua" untuk form pencarian; mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertComboDanSemua(combobox, properties, deskripsi, clazz, criterions)}.
	 * Item "Semua" disisipkan, lalu data terfilter dimuat.</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox}/{@code properties}/{@code deskripsi}/{@code clazz}/
	 * {@code criterions} seperti varian sebelumnya.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Label item netral default; untuk label kustom pakai overload ber-
	 * {@code labelTidakDipilih}. Pastikan query mengenali nilai "Semua" sebagai tanpa-filter.</p>
	 *
	 * @param combobox   komponen target
	 * @param properties array nama properti label
	 * @param deskripsi  teks pemisah/format label
	 * @param clazz      kelas entitas sumber
	 * @param criterions filter opsional (0..n)
	 */
	@SuppressWarnings({ "rawtypes" })
	public static void insertComboDanSemua(Combobox combobox, String[] properties, String deskripsi, Class clazz,
			Criterion... criterions) {
		CommonComboInsertHelper.insertComboDanSemua(combobox, properties, deskripsi, clazz, criterions);
	}

	/**
	 * Varian {@code insertComboDanSemua} multi-properti dengan LABEL item netral kustom
	 * ({@code labelTidakDipilih}), difilter varargs.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Sama seperti overload di atas, tetapi teks item netral dapat
	 * ditentukan ({@code labelTidakDipilih}, mis. "- Semua -", "- Pilih -"). Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertComboDanSemua(combobox, properties, deskripsi, clazz,
	 * labelTidakDipilih, criterions)}.</p>
	 *
	 * <p><b>Parameter.</b> Seperti varian sebelumnya plus {@code labelTidakDipilih} = teks item netral.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Konsistenkan label item netral antar-form sejenis demi UX seragam.</p>
	 *
	 * @param combobox          komponen target
	 * @param properties        array nama properti label
	 * @param deskripsi         teks pemisah/format label
	 * @param clazz             kelas entitas sumber
	 * @param labelTidakDipilih teks item netral ("Semua"/"Pilih")
	 * @param criterions        filter opsional (0..n)
	 */
	@SuppressWarnings({ "rawtypes" })
	public static void insertComboDanSemua(Combobox combobox, String[] properties, String deskripsi, Class clazz,
			String labelTidakDipilih, Criterion... criterions) {
		CommonComboInsertHelper.insertComboDanSemua(combobox, properties, deskripsi, clazz, labelTidakDipilih,
				criterions);
	}

	/**
	 * Varian {@code insertComboDanSemua} multi-properti, label item netral kustom, memakai
	 * {@link Criteria} dari pemanggil (tidak membuka session baru).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Gabungan paling lengkap: label multi-properti +
	 * {@code deskripsi}, item netral berlabel {@code labelTidakDipilih}, dan {@link Criteria} siap pakai
	 * milik pemanggil. Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertComboDanSemua(combobox, properties, deskripsi, clazz,
	 * labelTidakDipilih, criteria)}.</p>
	 *
	 * <p><b>Parameter.</b> Seperti varian {@code Criterion...} namun dengan {@code criteria} sebagai
	 * sumber filter.</p>
	 *
	 * <p><b>Pemeliharaan (PENTING).</b> Karena menerima {@link Criteria} dari luar, TIDAK boleh membuka
	 * session baru—siklus hidupnya milik pemanggil.</p>
	 *
	 * @param combobox          komponen target
	 * @param properties        array nama properti label
	 * @param deskripsi         teks pemisah/format label
	 * @param clazz             kelas entitas sumber
	 * @param labelTidakDipilih teks item netral
	 * @param criteria          kriteria siap-eksekusi milik pemanggil
	 */
	@SuppressWarnings({ "rawtypes" })
	public static void insertComboDanSemua(Combobox combobox, String[] properties, String deskripsi, Class clazz,
			String labelTidakDipilih, Criteria criteria) {
		CommonComboInsertHelper.insertComboDanSemua(combobox, properties, deskripsi, clazz, labelTidakDipilih,
				criteria);
	}

	/**
	 * Mengisi {@link Combobox} dengan label multi-properti, TANPA filter (semua baris).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Bentuk sederhana multi-properti tanpa kriteria; mendelegasikan
	 * ke {@code CommonComboInsertHelper.insertCombo(combobox, properties, clazz)}. Memuat seluruh baris
	 * dengan label gabungan dari {@code properties}.</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox} = target; {@code properties} = array properti label;
	 * {@code clazz} = entitas sumber.</p>
	 *
	 * <p><b>Pemeliharaan &amp; kinerja.</b> Tanpa filter—hindari untuk entitas besar.</p>
	 *
	 * @param combobox   komponen target
	 * @param properties array nama properti label
	 * @param clazz      kelas entitas sumber
	 */
	public static void insertCombo(Combobox combobox, String[] properties, Class<?> clazz) {
		CommonComboInsertHelper.insertCombo(combobox, properties, clazz);
	}

	/**
	 * Mengisi {@link Combobox} multi-properti dengan opsi "Semua", TANPA filter.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Versi "DanSemua" sederhana multi-properti; mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertComboDanSemua(combobox, properties, clazz)}. Menyisipkan item
	 * "Semua" lalu seluruh data dengan label gabungan.</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox}/{@code properties}/{@code clazz} seperti varian multi-properti.</p>
	 *
	 * <p><b>Pemeliharaan &amp; kinerja.</b> Tanpa filter—hindari entitas besar. Untuk filter, pakai
	 * overload ber-{@link Criterion}/{@link Criteria}.</p>
	 *
	 * @param combobox   komponen target
	 * @param properties array nama properti label
	 * @param clazz      kelas entitas sumber
	 */
	public static void insertComboDanSemua(Combobox combobox, String[] properties, Class<?> clazz) {
		CommonComboInsertHelper.insertComboDanSemua(combobox, properties, clazz);
	}

	/**
	 * Mengisi {@link Combobox} multi-properti + {@code deskripsi}, TANPA filter.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Label gabungan beberapa properti dengan format {@code deskripsi},
	 * memuat seluruh baris; mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertCombo(combobox, properties, deskripsi, clazz)}.</p>
	 *
	 * <p><b>Parameter.</b> {@code combobox}/{@code properties}/{@code clazz} seperti biasa; {@code deskripsi}
	 * = teks pemisah/format label.</p>
	 *
	 * <p><b>Pemeliharaan &amp; kinerja.</b> Tanpa filter—hindari entitas besar; makna {@code deskripsi}
	 * ditentukan helper.</p>
	 *
	 * @param combobox   komponen target
	 * @param properties array nama properti label
	 * @param deskripsi  teks pemisah/format label
	 * @param clazz      kelas entitas sumber
	 */
	public static void insertCombo(Combobox combobox, String[] properties, String deskripsi, Class<?> clazz) {
		CommonComboInsertHelper.insertCombo(combobox, properties, deskripsi, clazz);
	}

	/**
	 * Mengisi {@link Combobox} dari sebuah {@link List} objek yang SUDAH ada (bukan query DB), label
	 * dari satu {@code property}.
	 *
	 * <p><b>Tujuan.</b> Berbeda dari keluarga {@code insertCombo} yang men-query entitas, varian
	 * {@code insertComboItems} mengisi combobox dari koleksi objek yang sudah dimiliki pemanggil (mis.
	 * hasil perhitungan, sublist tersaring, atau data non-Hibernate). Tidak menyentuh DB sama sekali.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertComboItems(combo, property, items)} yang membuat item untuk
	 * tiap elemen {@code items}, dengan label = nilai {@code property} objek dan value = objeknya.</p>
	 *
	 * <p><b>Parameter.</b> {@code combo} = target; {@code property} = nama properti label;
	 * {@code items} = daftar objek sumber.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pakai ini bila data sudah di memori—lebih efisien daripada query ulang.
	 * Pastikan tiap elemen punya getter untuk {@code property}.</p>
	 *
	 * @param combo    komponen target
	 * @param property nama properti label
	 * @param items    daftar objek sumber
	 */
	public static void insertComboItems(Combobox combo, String property, List<?> items) {
		CommonComboInsertHelper.insertComboItems(combo, property, items);
	}

	/**
	 * Mengisi {@link Combobox} dari {@link List} memakai item komponen ber-konfigurasi khusus
	 * ({@code MyConfig}).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti {@link #insertComboItems(Combobox, String, List)},
	 * tetapi item dibangun memakai komponen "MyConfig" internal (mis. {@code MyComboitemConfig}) untuk
	 * perilaku/tampilan kustom AIS. Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertComboItemsMyConfig(combo, property, items)}.</p>
	 *
	 * <p><b>Parameter.</b> {@code combo} = target; {@code property} = properti label; {@code items} =
	 * daftar objek sumber.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pilih varian MyConfig hanya bila fitur komponen kustom diperlukan.</p>
	 *
	 * @param combo    komponen target
	 * @param property nama properti label
	 * @param items    daftar objek sumber
	 */
	public static void insertComboItemsMyConfig(Combobox combo, String property, List<?> items) {
		CommonComboInsertHelper.insertComboItemsMyConfig(combo, property, items);
	}

	/**
	 * Mengisi {@link Combobox} dari {@link List} objek bertipe CommonVO (value-object umum), tanpa
	 * properti label eksplisit.
	 *
	 * <p><b>Tujuan.</b> Untuk daftar berupa objek value-object standar AIS (CommonVO) yang sudah
	 * membawa label/nilainya sendiri, sehingga pemanggil tak perlu menyebut nama properti label.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertComboItemsCommonVO(combo, items)} yang membentuk item dari
	 * tiap CommonVO (label/value diambil dari konvensi VO tsb).</p>
	 *
	 * <p><b>Parameter.</b> {@code combo} = target; {@code items} = daftar CommonVO.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pastikan elemen benar-benar CommonVO sesuai harapan helper; untuk objek
	 * umum dengan properti tertentu, pakai overload ber-{@code property}.</p>
	 *
	 * @param combo komponen target
	 * @param items daftar objek CommonVO
	 */
	public static void insertComboItemsCommonVO(Combobox combo, List<?> items) {
		CommonComboInsertHelper.insertComboItemsCommonVO(combo, items);
	}

	/**
	 * Mengisi {@link Combobox} dari {@link List} (label satu {@code property}) sambil menetapkan
	 * {@code style} CSS.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti {@link #insertComboItems(Combobox, String, List)} plus
	 * pengaturan gaya tampilan ({@code style}). Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertComboItems(combo, property, items, style)}.</p>
	 *
	 * <p><b>Parameter.</b> {@code combo}/{@code property}/{@code items} seperti biasa; {@code style} =
	 * string CSS inline.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Utamakan sclass/CSS terpusat bila styling dipakai berulang.</p>
	 *
	 * @param combo    komponen target
	 * @param property nama properti label
	 * @param items    daftar objek sumber
	 * @param style    string CSS inline
	 */
	public static void insertComboItems(Combobox combo, String property, List<?> items, String style) {
		CommonComboInsertHelper.insertComboItems(combo, property, items, style);
	}

	/**
	 * Mengisi {@link Combobox} dari {@link List} dengan label {@code property} + {@code deskripsi}.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Menyusun label lebih informatif (property digabung
	 * {@code deskripsi}) dari koleksi objek di memori. Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertComboItems(combo, property, deskripsi, items)}.</p>
	 *
	 * <p><b>Parameter.</b> {@code combo}/{@code property}/{@code items} seperti biasa; {@code deskripsi}
	 * = teks pemisah/format label.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Makna {@code deskripsi} ditentukan helper; ikuti pemakaian yang ada.</p>
	 *
	 * @param combo     komponen target
	 * @param property  nama properti label utama
	 * @param deskripsi teks pemisah/format label
	 * @param items     daftar objek sumber
	 */
	public static void insertComboItems(Combobox combo, String property, String deskripsi, List<?> items) {
		CommonComboInsertHelper.insertComboItems(combo, property, deskripsi, items);
	}

	/**
	 * Mengisi {@link Combobox} dari {@link List} dengan label gabungan BEBERAPA properti.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Varian multi-properti berbasis koleksi memori; label tiap item
	 * disusun dari array {@code properties}. Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertComboItems(combo, properties, items)}.</p>
	 *
	 * <p><b>Parameter.</b> {@code combo} = target; {@code properties} = array properti label;
	 * {@code items} = daftar objek sumber.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Tiap elemen harus punya getter untuk semua {@code properties}.</p>
	 *
	 * @param combo      komponen target
	 * @param properties array nama properti label
	 * @param items      daftar objek sumber
	 */
	public static void insertComboItems(Combobox combo, String properties[], List<?> items) {
		CommonComboInsertHelper.insertComboItems(combo, properties, items);
	}

	/**
	 * Mengisi {@link Combobox} dari {@link List}, label multi-properti + {@code deskripsi}.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Menggabungkan beberapa properti dan format {@code deskripsi}
	 * untuk label, dari koleksi memori. Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertComboItems(combo, properties, deskripsi, items)}.</p>
	 *
	 * <p><b>Parameter.</b> {@code combo}/{@code properties}/{@code items} seperti biasa; {@code deskripsi}
	 * = teks pemisah/format label.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Untuk menambahkan item netral "tidak dipilih", pakai overload ber-
	 * {@code labelTidakDipilih}.</p>
	 *
	 * @param combo      komponen target
	 * @param properties array nama properti label
	 * @param deskripsi  teks pemisah/format label
	 * @param items      daftar objek sumber
	 */
	public static void insertComboItems(Combobox combo, String properties[], String deskripsi, List<?> items) {
		CommonComboInsertHelper.insertComboItems(combo, properties, deskripsi, items);
	}

	/**
	 * Mengisi {@link Combobox} dari {@link List} (label multi-properti + {@code deskripsi}) dengan item
	 * netral berlabel {@code labelTidakDipilih}.
	 *
	 * <p><b>Tujuan.</b> Varian paling lengkap dari {@code insertComboItems}: label gabungan beberapa
	 * properti + format {@code deskripsi}, ditambah item "tidak dipilih"/"Semua" berlabel kustom di
	 * awal—cocok untuk filter pencarian berbasis koleksi memori.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboInsertHelper.insertComboItems(combo, properties, deskripsi, items, labelTidakDipilih)}
	 * yang menyisipkan item netral lalu seluruh elemen {@code items}.</p>
	 *
	 * <p><b>Parameter.</b> {@code combo}/{@code properties}/{@code deskripsi}/{@code items} seperti
	 * varian sebelumnya; {@code labelTidakDipilih} = teks item netral.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pastikan logika pemanggil memperlakukan pilihan item netral sebagai
	 * "tanpa filter". Konsistenkan teks {@code labelTidakDipilih} antar-form.</p>
	 *
	 * @param combo             komponen target
	 * @param properties        array nama properti label
	 * @param deskripsi         teks pemisah/format label
	 * @param items             daftar objek sumber
	 * @param labelTidakDipilih teks item netral
	 */
	public static void insertComboItems(Combobox combo, String properties[], String deskripsi, List<?> items,
			String labelTidakDipilih) {
		CommonComboInsertHelper.insertComboItems(combo, properties, deskripsi, items, labelTidakDipilih);
	}

	/**
	 * Memvalidasi sekumpulan properti/komponen input form sebelum diproses (mis. sebelum simpan).
	 *
	 * <p><b>Tujuan.</b> Menyatukan pengecekan validasi—mis. field wajib terisi, format benar—dalam satu
	 * panggilan ringkas. Komentar "Mengoptimasi ... untuk performa dan keamanan Memory" menandai bahwa
	 * implementasinya sengaja hemat (menghindari pembuatan objek/exception yang tidak perlu).</p>
	 *
	 * <p><b>Cara kerja.</b> Menerima varargs {@code properties} (komponen/objek yang divalidasi) lalu
	 * mendelegasikan ke {@code CommonMenuAccessHelper.checkValidasi(properties)} yang mengevaluasi tiap
	 * elemen sesuai aturan validasinya dan mengembalikan status keseluruhan.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code properties} = daftar item yang divalidasi (komponen ZK
	 * dan/atau nilai). Mengembalikan {@link Boolean} {@code true} bila semua valid.</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Mendeklarasikan {@code throws Exception}—pemanggil
	 * harus menangani/meneruskan; validasi yang gagal dapat memunculkan pesan ke pengguna. Aturan
	 * validasi dijaga di helper; tambahkan jenis pengecekan baru di sana agar seragam.</p>
	 *
	 * @param properties item-item yang divalidasi (varargs)
	 * @return {@code true} bila semua valid
	 * @throws Exception bila proses validasi menemui kondisi yang harus dilaporkan
	 */
	// Mengoptimasi fungsi Check Validasi untuk performa dan keamanan Memory
	public static Boolean checkValidasi(Object... properties) throws Exception {
		return CommonMenuAccessHelper.checkValidasi(properties);
	}

	/**
	 * Mengosongkan seluruh anak (children) sebuah {@link Component} ZK (overload paling ringkas).
	 *
	 * <p><b>Tujuan.</b> Membersihkan isi komponen kontainer (mis. combobox, grid, box) sebelum diisi
	 * ulang—mencegah item ganda saat sebuah panel/daftar dibangun berkali-kali.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@link #clear(Component, String)} dengan {@code nama}
	 * null, yang berarti "lepaskan semua anak tanpa filter atribut".</p>
	 *
	 * <p><b>Parameter &amp; pemeliharaan.</b> {@code comp} = komponen yang dikosongkan (aman bila null).
	 * Untuk hanya melepas anak ber-atribut tertentu, pakai overload {@link #clear(Component, String)}.</p>
	 *
	 * @param comp komponen yang anaknya akan dilepas
	 */
	public static void clear(Component comp) {
		clear(comp, null);
	}

	/**
	 * Mengosongkan anak sebuah {@link Component}, opsional hanya yang memiliki atribut {@code nama}.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Varian {@link #clear(Component)} dengan filter atribut:
	 * mendelegasikan ke {@link #clear(Component, String, int)} dengan {@code index=0}. Bila {@code nama}
	 * non-null, hanya anak yang memiliki atribut tsb yang dilepas.</p>
	 *
	 * <p><b>Parameter &amp; pemeliharaan.</b> {@code comp} = kontainer; {@code nama} = nama atribut
	 * penanda (null = semua). Lihat catatan pada {@link #clear(Component, String, int)} soal pola
	 * iterasi.</p>
	 *
	 * @param comp komponen yang anaknya akan dilepas
	 * @param nama nama atribut penanda (null = semua anak)
	 */
	public static void clear(Component comp, String nama) {
		clear(comp, nama, 0);
	}

	/**
	 * Melepas anak-anak sebuah {@link Component} mulai dari posisi {@code index}, opsional terfilter
	 * atribut—implementasi inti keluarga {@code clear}.
	 *
	 * <p><b>Tujuan.</b> Menghapus children komponen kontainer secara defensif sehingga komponen dapat
	 * diisi ulang dengan bersih, tanpa NPE meski struktur anak tak terduga.</p>
	 *
	 * <p><b>Cara kerja.</b> No-op bila {@code comp} atau {@code getChildren()} null. Mengambil jumlah
	 * anak awal, lalu mengulang sebanyak itu; pada tiap iterasi, bila {@code nama} null ATAU komponen
	 * memiliki atribut {@code nama}, anak pada posisi {@code index} di-{@code detach()}. Karena
	 * {@code detach} mengubah daftar anak, melepas pada {@code index} tetap secara berulang akan
	 * "menggeser" anak berikutnya ke posisi itu—pola yang dipakai untuk menyapu anak.</p>
	 *
	 * <p><b>Parameter.</b> {@code comp} = kontainer; {@code nama} = atribut penanda (null = semua);
	 * {@code index} = posisi anak yang dilepas tiap iterasi.</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Tiap {@code detach} dibungkus try/catch kosong agar
	 * struktur tak terduga tidak menghentikan proses. Hati-hati: kombinasi {@code index} &gt; 0 dengan
	 * filter {@code nama} bersifat halus—andalkan pola pemakaian yang sudah ada; perubahan logika di
	 * sini berdampak ke semua pemanggil {@code clear}.</p>
	 *
	 * @param comp  komponen yang anaknya akan dilepas (aman bila null)
	 * @param nama  nama atribut penanda (null = semua anak)
	 * @param index posisi anak yang dilepas pada tiap iterasi
	 */
	public static void clear(Component comp, String nama, int index) {
		if (comp == null || comp.getChildren() == null)
			return;
		int posisi = index < 0 ? 0 : index;
		/*
		 * OPTIMASI PERFORMA (snapshot 12-19/08/2026: Common.clear adalah frame kode aplikasi
		 * TERBANYAK yang sedang RUNNABLE, 400+ kemunculan).
		 *
		 * Versi lama memanggil comp.getChildren().size() dan .get(posisi) BERULANG di dalam
		 * loop. Children ZK adalah list ber-akses SEKUENSIAL (AbstractSequentialList): get(i)
		 * berbiaya O(i), sehingga pola lama berperilaku O(n^2). Pada kontainer berisi ribuan
		 * anak (grid/rows) ini menghabiskan detik-detik CPU, dan celakanya dilakukan SAMBIL
		 * MEMEGANG kunci desktop ZK -- seluruh request lain untuk desktop yang sama ikut antre
		 * di UiEngineImpl.doActivate (terlihat pada 83-87 thread ajp-nio per snapshot).
		 *
		 * Perbaikan: ambil SATU salinan daftar anak lalu lepas satu per satu -> O(n). Perilaku
		 * dipertahankan persis: anak sebelum "index" dilewati, Paging dan anak yang tidak cocok
		 * filter "nama" tetap dipertahankan, dan tiap detach tetap dibungkus try/catch sendiri.
		 */
		java.util.List<?> anakAwal;
		try {
			anakAwal = new java.util.ArrayList<Object>(comp.getChildren());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "Common.clear.salinAnak");
			return;
		}
		for (int i = posisi; i < anakAwal.size(); i++) {
			try {
				Object o = anakAwal.get(i);
				if (!(o instanceof Component)) {
					continue;
				}
				Component child = (Component) o;
				if (child instanceof Paging || (nama != null && child.getAttribute(nama) == null)) {
					// Paging dikelola ZK. Anak yang tidak cocok filter juga dipertahankan.
					continue;
				}
				child.detach();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:4065");
				// Struktur dapat berubah dari listener ZK ketika detach; anak berikutnya
				// tetap diproses karena kita berjalan di atas salinan.
			}
		}
	}

	/**
	 * Memilih (men-select) item pada {@link Combobox} yang nilainya cocok {@code value}, dengan opsi
	 * MENAMBAHKAN item bila belum ada.
	 *
	 * <p><b>Tujuan.</b> Saat memuat data ke form (mode ubah), nilai tersimpan harus tersorot di
	 * combobox. Bila nilai itu tidak ada di daftar (mis. data lama/nonaktif yang tak lagi dimuat),
	 * flag {@code jikaDataTidakDitemukanDitambahkan} memungkinkan menambahkannya agar tetap tampil dan
	 * terpilih—mencegah kehilangan nilai saat edit.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboSelectionHelper.selectComboItem(jikaDataTidakDitemukanDitambahkan, combo, value)}
	 * yang mencari item ber-nilai {@code value}; bila ketemu, dipilih; bila tidak dan flag {@code true},
	 * item baru ditambahkan lalu dipilih.</p>
	 *
	 * <p><b>Parameter.</b> {@code jikaDataTidakDitemukanDitambahkan} = tambahkan item bila tak ada;
	 * {@code combo} = komponen; {@code value} = nilai yang dipilih.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Urutan parameter pada overload ini (flag di depan) berbeda dari
	 * {@link #selectComboItem(Combobox, Object, boolean)} (flag di belakang)—keduanya setara, perhatikan
	 * urutan saat memanggil.</p>
	 *
	 * @param jikaDataTidakDitemukanDitambahkan tambahkan item baru bila nilai tak ditemukan
	 * @param combo                              komponen combobox
	 * @param value                              nilai yang akan dipilih
	 */
	public static void selectComboItem(boolean jikaDataTidakDitemukanDitambahkan, Combobox combo, Object value) {
		CommonComboSelectionHelper.selectComboItem(jikaDataTidakDitemukanDitambahkan, combo, value);
	}

	/**
	 * Memilih item pada {@link Combobox} yang cocok {@code value} (tanpa menambah bila tak ada).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Bentuk paling umum: menyorot item ber-nilai {@code value} bila
	 * ada di daftar. Mendelegasikan ke {@code CommonComboSelectionHelper.selectComboItem(combo, value)}.
	 * Bila nilai tak ada, tidak ada yang terpilih (tidak ditambahkan).</p>
	 *
	 * <p><b>Parameter.</b> {@code combo} = komponen; {@code value} = nilai yang dipilih.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Bila nilai mungkin tidak ada di daftar tetapi harus tetap tampil, pakai
	 * overload yang menerima flag "tambahkan bila tak ditemukan".</p>
	 *
	 * @param combo komponen combobox
	 * @param value nilai yang akan dipilih
	 */
	public static void selectComboItem(Combobox combo, Object value) {
		CommonComboSelectionHelper.selectComboItem(combo, value);
	}

	/**
	 * Memilih item pada {@link Combobox} cocok {@code value}, dengan flag tambah-bila-tak-ada
	 * (flag di akhir).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Setara dengan
	 * {@link #selectComboItem(boolean, Combobox, Object)} namun urutan parameter berbeda (flag di
	 * belakang) demi kenyamanan pemanggil. Mendelegasikan ke
	 * {@code CommonComboSelectionHelper.selectComboItem(combo, value, jikaDataTidakDitemukanDitambahkan)}.</p>
	 *
	 * <p><b>Parameter.</b> {@code combo} = komponen; {@code value} = nilai dipilih;
	 * {@code jikaDataTidakDitemukanDitambahkan} = tambahkan item bila tak ada.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Adanya dua urutan parameter rawan keliru—pastikan memanggil overload yang
	 * dimaksud. Perilakunya identik.</p>
	 *
	 * @param combo                              komponen combobox
	 * @param value                              nilai yang akan dipilih
	 * @param jikaDataTidakDitemukanDitambahkan  tambahkan item baru bila nilai tak ditemukan
	 */
	@SuppressWarnings({})
	public static void selectComboItem(Combobox combo, Object value, boolean jikaDataTidakDitemukanDitambahkan) {
		CommonComboSelectionHelper.selectComboItem(combo, value, jikaDataTidakDitemukanDitambahkan);
	}

	/**
	 * Mengekstrak (unzip) seluruh isi sebuah folder/arsip dari {@code source} ke {@code target}.
	 *
	 * <p><b>Tujuan.</b> Operasi unzip tingkat-folder untuk kebutuhan seperti impor data, pemulihan,
	 * atau pemrosesan paket berkas. Memusatkan logika ekstraksi menjaga konsistensi penanganan path.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonFileMediaHelper.unzipFolder(source, target)}
	 * yang membaca entri arsip dan menuliskannya ke direktori {@code target}.</p>
	 *
	 * <p><b>Parameter &amp; error.</b> {@code source} = path sumber; {@code target} = direktori tujuan.
	 * Melempar {@link IOException} bila baca/tulis gagal—pemanggil harus menangani.</p>
	 *
	 * <p><b>Pemeliharaan &amp; keamanan.</b> Pastikan ekstraksi memakai proteksi zip-slip (lihat
	 * {@link #zipSlipProtect(ZipEntry, Path)}) agar entri tidak menulis di luar {@code target}.</p>
	 *
	 * @param source path sumber arsip/folder
	 * @param target direktori tujuan ekstraksi
	 * @throws IOException bila operasi berkas gagal
	 */
	public static void unzipFolder(Path source, Path target) throws IOException {
		CommonFileMediaHelper.unzipFolder(source, target);
	}

	/**
	 * Menghitung path tujuan yang AMAN untuk sebuah {@link ZipEntry}, mencegah serangan "zip slip".
	 *
	 * <p><b>Tujuan.</b> Pertahanan keamanan penting saat mengekstrak arsip: entri jahat bisa memuat
	 * path seperti {@code ../../etc/passwd} untuk menulis di luar direktori tujuan. Method ini
	 * memastikan path hasil tetap berada DI DALAM {@code targetDir}.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonFileMediaHelper.zipSlipProtect(zipEntry, targetDir)}
	 * yang menormalkan path gabungan dan memverifikasi bahwa ia berawalan {@code targetDir}; bila tidak,
	 * melempar {@link IOException} (menolak entri berbahaya).</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code zipEntry} = entri arsip; {@code targetDir} = direktori
	 * tujuan tepercaya. Mengembalikan {@link Path} aman untuk menulis entri tsb.</p>
	 *
	 * <p><b>Pemeliharaan (PENTING).</b> SELALU pakai method ini saat mengekstrak arsip dari sumber tak
	 * tepercaya; jangan menggabung path entri secara langsung. Melempar {@link IOException} bila entri
	 * mencoba keluar dari {@code targetDir}.</p>
	 *
	 * @param zipEntry  entri arsip
	 * @param targetDir direktori tujuan tepercaya
	 * @return path aman di dalam {@code targetDir}
	 * @throws IOException bila entri mencoba menulis di luar direktori tujuan
	 */
	// protect zip slip attack
	public static Path zipSlipProtect(ZipEntry zipEntry, Path targetDir) throws IOException {
		return CommonFileMediaHelper.zipSlipProtect(zipEntry, targetDir);
	}

	/**
	 * Mengekstrak sebuah berkas ZIP ke folder tujuan dan mengembalikan daftar berkas hasil.
	 *
	 * <p><b>Tujuan.</b> Membongkar arsip ZIP (mis. unggahan pengguna, paket impor) menjadi berkas-berkas
	 * konkret yang lalu dapat diproses, sambil memberikan daftar berkas yang dihasilkan untuk
	 * iterasi/validasi lanjutan.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonFileMediaHelper.extractZip(file, destinationname)}
	 * yang membaca tiap entri dan menuliskannya ke {@code destinationname} (idealnya dengan proteksi
	 * zip-slip), mengumpulkan {@link File} hasil ke dalam {@link List}.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code file} = berkas ZIP; {@code destinationname} = folder
	 * tujuan. Mengembalikan daftar {@link File} yang diekstrak.</p>
	 *
	 * <p><b>Pemeliharaan &amp; error.</b> Melempar {@code Exception}. Untuk meratakan hasil ke satu
	 * direktori (mengabaikan struktur folder dalam ZIP), pakai overload ber-flag
	 * {@link #extractZip(File, String, boolean)}.</p>
	 *
	 * @param file            berkas ZIP sumber
	 * @param destinationname folder tujuan ekstraksi
	 * @return daftar berkas hasil ekstraksi
	 * @throws Exception bila ekstraksi gagal
	 */
	public static List<File> extractZip(File file, String destinationname) throws Exception {
		return CommonFileMediaHelper.extractZip(file, destinationname);
	}

	/**
	 * Mengekstrak berkas ZIP ke folder tujuan, dengan opsi MERATAKAN berkas ke satu direktori.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti {@link #extractZip(File, String)} namun
	 * {@code extrackFileOnDirectory} menentukan apakah berkas diletakkan langsung di direktori tujuan
	 * (mengabaikan sub-folder dalam ZIP) atau mempertahankan struktur. Mendelegasikan ke
	 * {@code CommonFileMediaHelper.extractZip(file, destinationname, extrackFileOnDirectory)}.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code file} = ZIP; {@code destinationname} = folder tujuan;
	 * {@code extrackFileOnDirectory} = ratakan semua berkas ke satu direktori bila {@code true}.
	 * Mengembalikan daftar {@link File} hasil.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Hati-hati saat meratakan: berkas bernama sama di sub-folder berbeda dapat
	 * saling menimpa. Melempar {@code Exception} bila gagal.</p>
	 *
	 * @param file                   berkas ZIP sumber
	 * @param destinationname        folder tujuan
	 * @param extrackFileOnDirectory ratakan berkas ke satu direktori bila {@code true}
	 * @return daftar berkas hasil ekstraksi
	 * @throws Exception bila ekstraksi gagal
	 */
	public static List<File> extractZip(File file, String destinationname, boolean extrackFileOnDirectory)
			throws Exception {
		return CommonFileMediaHelper.extractZip(file, destinationname, extrackFileOnDirectory);
	}

	/**
	 * Membuat berkas ZIP dari sejumlah berkas, dengan NAMA ENTRI yang ditentukan terpisah.
	 *
	 * <p><b>Tujuan.</b> Mengemas beberapa berkas menjadi satu arsip (mis. untuk unduhan/ekspor),
	 * sambil mengontrol nama tiap entri di dalam ZIP secara eksplisit—berguna bila nama tampil harus
	 * berbeda dari nama berkas fisik.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonFileMediaHelper.createZip(filesName, filenames, outFilename)} yang menulis tiap
	 * {@code filenames[i]} ke arsip memakai nama {@code filesName[i]}.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code filesName} = daftar nama entri (sejajar);
	 * {@code filenames} = daftar berkas fisik; {@code outFilename} = berkas ZIP keluaran. Mengembalikan
	 * {@link Boolean} {@code true} bila sukses.</p>
	 *
	 * <p><b>Pemeliharaan &amp; risiko.</b> {@code filesName} dan {@code filenames} harus sejajar
	 * (panjang &amp; urutan sama). Untuk memakai nama berkas asli sebagai nama entri, pakai overload
	 * {@link #createZip(List, File)}.</p>
	 *
	 * @param filesName   daftar nama entri di dalam ZIP (sejajar dengan {@code filenames})
	 * @param filenames   daftar berkas fisik yang diarsipkan
	 * @param outFilename berkas ZIP keluaran
	 * @return {@code true} bila pembuatan ZIP sukses
	 */
	public static Boolean createZip(List<String> filesName, List<File> filenames, File outFilename) {
		return CommonFileMediaHelper.createZip(filesName, filenames, outFilename);
	}

	/**
	 * Membuat berkas ZIP dari sejumlah berkas, memakai NAMA BERKAS ASLI sebagai nama entri.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Bentuk sederhana dari {@link #createZip(List, List, File)}:
	 * nama entri di dalam ZIP mengikuti nama berkas fisik. Mendelegasikan ke
	 * {@code CommonFileMediaHelper.createZip(filenames, outFilename)}.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code filenames} = daftar berkas yang diarsipkan;
	 * {@code outFilename} = berkas ZIP keluaran. Mengembalikan {@code true} bila sukses.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Bila ada berkas dengan nama sama dari folder berbeda, entri dapat
	 * bertabrakan—pakai overload ber-{@code filesName} untuk memberi nama unik.</p>
	 *
	 * @param filenames   daftar berkas fisik yang diarsipkan
	 * @param outFilename berkas ZIP keluaran
	 * @return {@code true} bila pembuatan ZIP sukses
	 */
	public static Boolean createZip(List<File> filenames, File outFilename) {
		return CommonFileMediaHelper.createZip(filenames, outFilename);
	}

	/**
	 * Menjalankan kueri inisialisasi awal aplikasi SATU KALI (guard idempoten).
	 *
	 * <p><b>Tujuan.</b> Tempat terpusat untuk menjalankan kueri yang hanya perlu dieksekusi sekali saat
	 * startup (mis. pengaturan level isolasi transaksi, pembuatan tabel/seed historis). Saat ini badan
	 * kueri sebagian besar dinonaktifkan (dikomentari) sehingga method efektif hanya menandai bahwa
	 * inisialisasi telah berjalan; struktur dipertahankan agar mudah mengaktifkan kembali bila perlu.</p>
	 *
	 * <p><b>Cara kerja.</b> Memeriksa flag statis {@code hasInitialQuery}; bila sudah {@code true},
	 * langsung kembali (idempoten—aman dipanggil berulang). Bila belum, mencatat log, (opsional)
	 * menjalankan kueri yang tersedia, lalu menyetel {@code hasInitialQuery = true} agar tidak berulang
	 * dalam umur JVM.</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Bila mengaktifkan kembali blok kueri yang
	 * dikomentari, bungkus tiap statement dengan try/catch agar satu kegagalan tidak menghentikan
	 * startup, dan kelola session/transaksi sesuai aturan {@code HibernateUtil}. Hati-hati: menyetel
	 * flag {@code true} meski kueri gagal berarti kueri tak akan diulang—pertimbangkan baik-baik untuk
	 * kueri yang krusial.</p>
	 */
	public static void executeInitialQuery() {
		if (hasInitialQuery)
			return;

		log.info("execute Initial Query ...........................................");

		// Session session = HibernateUtil.currentSession();
		//
		// String sql = "set global transaction isolation level read
		// committed;";
		// session.createSQLQuery(sql).executeUpdate();
		//
		// sql = "set session transaction isolation level read committed;";
		// session.createSQLQuery(sql).executeUpdate();
		//
		// sql = "SELECT @@global.tx_isolation;";
		// session.createSQLQuery(sql).list();
		//
		// sql = "SELECT @@tx_isolation;";
		// session.createSQLQuery(sql).list();
		//
		// List<String> quries = new ArrayList<String>();
		// quries.add("create table judisium ( id double , judisium varchar
		// (255), nilai_mulai double , "
		// + "nilai_sampai double )");
		//
		// quries.add(
		// "insert into judisium (id, judisium, nilai_mulai, nilai_sampai)
		// values('1','Buruk','0.0000','2.7499')");
		// quries.add(
		// "insert into judisium (id, judisium, nilai_mulai, nilai_sampai)
		// values('2','Sangat Memuaskan','2.7500','3.4999')");
		// quries.add(
		// "insert into judisium (id, judisium, nilai_mulai, nilai_sampai)
		// values('3','Cumlaude','3.5000','4.0000')");
		//
		// for (String q : quries) {
		// try {
		// int i = session.createSQLQuery(q).executeUpdate();
		// log.info("result = " + i);
		// } catch (HibernateException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:4355");
		// // TODO Auto-generated catch block
		// // Common.tampilErrorJikaAdmin(e);
		// }
		// }

		hasInitialQuery = true;
	}

	/**
	 * Menghitung SEMESTER (ke berapa) seorang mahasiswa pada suatu tahun akademik—overload paling lengkap.
	 *
	 * <p><b>Tujuan.</b> Semester berjalan mahasiswa bukan data tersimpan melainkan TURUNAN dari
	 * angkatan, tahun akademik, jenis semester (ganjil/genap), dan kapan ia mulai/masuk. Method ini
	 * menjadi sumber kebenaran perhitungan tsb—dipakai luas untuk KRS, tagihan per-semester, dan
	 * pelaporan.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicKrsNilaiHelper.getSemester(tahunAngkatanMhs, tahunAkademik, jenisSemester,
	 * mulaiSemester, masukDiSemester)} yang menghitung selisih periode antara angkatan dan tahun
	 * akademik target, disesuaikan jenis semester serta titik masuk mahasiswa.</p>
	 *
	 * <p><b>Parameter.</b> {@code tahunAngkatanMhs} = tahun angkatan; {@code tahunAkademik} = tahun
	 * akademik acuan; {@code jenisSemester} = ganjil/genap; {@code mulaiSemester} = semester awal
	 * (mis. 1); {@code masukDiSemester} = penanda semester masuk (mis. untuk mahasiswa pindahan/transfer).</p>
	 *
	 * <p><b>Return &amp; pemeliharaan.</b> Mengembalikan nomor semester ({@link Integer}). Untuk konteks
	 * tanpa tahun akademik eksplisit, pakai overload lain. Logika perhitungan dipusatkan di helper—ubah
	 * di sana agar konsisten dengan KRS/nilai.</p>
	 *
	 * @param tahunAngkatanMhs tahun angkatan mahasiswa
	 * @param tahunAkademik    tahun akademik acuan
	 * @param jenisSemester    jenis semester (ganjil/genap)
	 * @param mulaiSemester    semester awal
	 * @param masukDiSemester  penanda semester masuk
	 * @return nomor semester mahasiswa pada tahun akademik tsb
	 */
	public static Integer getSemester(Integer tahunAngkatanMhs, String tahunAkademik, String jenisSemester,
			Integer mulaiSemester, String masukDiSemester) {
		return CommonAcademicKrsNilaiHelper.getSemester(tahunAngkatanMhs, tahunAkademik, jenisSemester, mulaiSemester,
				masukDiSemester);
	}

	/**
	 * Menghitung semester mahasiswa TANPA tahun akademik eksplisit (memakai konteks/tahun berjalan).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Varian {@link #getSemester(Integer, String, String, Integer, String)}
	 * yang tidak menerima {@code tahunAkademik}—helper menentukannya dari konteks berjalan. Mendelegasikan
	 * ke {@code CommonAcademicKrsNilaiHelper.getSemester(tahunAngkatanMhs, jenisSemester, mulaiSemester,
	 * masukDiSemester)}.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code tahunAngkatanMhs}, {@code jenisSemester},
	 * {@code mulaiSemester}, {@code masukDiSemester} seperti overload lengkap. Mengembalikan nomor
	 * semester ({@link Integer}).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pakai bila tahun akademik = berjalan; untuk perhitungan historis pakai
	 * overload yang menerima {@code tahunAkademik} atau {@code tahun}.</p>
	 *
	 * @param tahunAngkatanMhs tahun angkatan mahasiswa
	 * @param jenisSemester    jenis semester (ganjil/genap)
	 * @param mulaiSemester    semester awal
	 * @param masukDiSemester  penanda semester masuk
	 * @return nomor semester mahasiswa
	 */
	public static Integer getSemester(Integer tahunAngkatanMhs, String jenisSemester, Integer mulaiSemester,
			String masukDiSemester) {
		return CommonAcademicKrsNilaiHelper.getSemester(tahunAngkatanMhs, jenisSemester, mulaiSemester,
				masukDiSemester);
	}

	/**
	 * Menghitung semester mahasiswa berdasarkan TAHUN (angka) acuan, bukan string tahun akademik.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Varian yang menerima {@code tahun} berupa {@link Integer}
	 * (mis. 2025) sebagai acuan periode—berguna saat perhitungan berbasis tahun kalender. Mendelegasikan
	 * ke {@code CommonAcademicKrsNilaiHelper.getSemester(tahunAngkatanMhs, jenisSemester, mulaiSemester,
	 * tahun, masukDiSemester)}.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> Seperti overload lain plus {@code tahun} = tahun acuan (angka).
	 * Mengembalikan nomor semester ({@link Integer}).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pastikan makna {@code tahun} (kalender vs awal tahun ajaran) sesuai
	 * konvensi helper agar hasil konsisten dengan overload berbasis string.</p>
	 *
	 * @param tahunAngkatanMhs tahun angkatan mahasiswa
	 * @param jenisSemester    jenis semester (ganjil/genap)
	 * @param mulaiSemester    semester awal
	 * @param tahun            tahun acuan (angka)
	 * @param masukDiSemester  penanda semester masuk
	 * @return nomor semester mahasiswa
	 */
	public static Integer getSemester(Integer tahunAngkatanMhs, String jenisSemester, Integer mulaiSemester,
			Integer tahun, String masukDiSemester) {
		return CommonAcademicKrsNilaiHelper.getSemester(tahunAngkatanMhs, jenisSemester, mulaiSemester, tahun,
				masukDiSemester);
	}

	/**
	 * Menghitung TAHAPAN studi mahasiswa (mis. tahun ke-, jenjang tahapan) berdasarkan tahun acuan.
	 *
	 * <p><b>Tujuan.</b> Selain semester, beberapa aturan akademik memakai konsep "tahapan" (mis.
	 * pengelompokan per tahun studi/blok). Method ini menghitung tahapan turunan dari angkatan, jenis
	 * semester, awal semester, dan tahun acuan—paralel dengan keluarga {@code getSemester}.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicKrsNilaiHelper.getTahapan(tahunAngkatanMhs, jenisSemester, mulaiSemester,
	 * tahun, masukDiSemester)}.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> Parameter sama seperti
	 * {@link #getSemester(Integer, String, Integer, Integer, String)}. Mengembalikan nomor tahapan
	 * ({@link Integer}).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Definisi "tahapan" bergantung aturan institusi; dijaga di helper. Gunakan
	 * konsisten dengan {@code getSemester} agar tidak terjadi ketidakcocokan periode.</p>
	 *
	 * @param tahunAngkatanMhs tahun angkatan mahasiswa
	 * @param jenisSemester    jenis semester (ganjil/genap)
	 * @param mulaiSemester    semester awal
	 * @param tahun            tahun acuan (angka)
	 * @param masukDiSemester  penanda semester masuk
	 * @return nomor tahapan studi mahasiswa
	 */
	public static Integer getTahapan(Integer tahunAngkatanMhs, String jenisSemester, Integer mulaiSemester,
			Integer tahun, String masukDiSemester) {
		return CommonAcademicKrsNilaiHelper.getTahapan(tahunAngkatanMhs, jenisSemester, mulaiSemester, tahun,
				masukDiSemester);
	}

	/**
	 * Membaca isi satu sel Excel ({@link XSSFSheet}) pada posisi {@code col}/{@code row} sebagai
	 * {@link Double}.
	 *
	 * <p><b>Tujuan.</b> Memudahkan impor data dari berkas Excel (.xlsx): membaca nilai numerik sel
	 * sebagai {@code Double} tanpa pemanggil perlu menangani tipe sel POI yang rumit.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonExcelContentHelper.getSheetContentAsDouble(sheet, col, row)} yang mengambil sel dan
	 * mengonversinya ke {@code Double} secara defensif.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code sheet} = lembar kerja; {@code col}/{@code row} = indeks
	 * kolom/baris. Mengembalikan nilai {@link Double}, atau kemungkinan {@code null}/0 bila sel kosong/
	 * non-numerik (sesuai implementasi helper).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Indeks {@code col}/{@code row} basis-0 sesuai POI—pastikan konsisten.
	 * Untuk tipe lain pakai {@code getSheetContentAsInteger/Long/String/Boolean/Date}.</p>
	 *
	 * @param sheet lembar kerja Excel
	 * @param col   indeks kolom
	 * @param row   indeks baris
	 * @return isi sel sebagai {@link Double}
	 */
	public static Double getSheetContentAsDouble(XSSFSheet sheet, Integer col, Integer row) {
		return CommonExcelContentHelper.getSheetContentAsDouble(sheet, col, row);
	}

	/**
	 * Membaca isi satu sel Excel sebagai {@link Integer}.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti {@link #getSheetContentAsDouble(XSSFSheet, Integer, Integer)}
	 * namun hasilnya {@link Integer}. Mendelegasikan ke
	 * {@code CommonExcelContentHelper.getSheetContentAsInteger(sheet, col, row)} yang mengonversi nilai
	 * sel ke bilangan bulat secara defensif.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code sheet}/{@code col}/{@code row} seperti biasa. Mengembalikan
	 * {@link Integer} (kemungkinan null/0 bila sel kosong/non-numerik).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Nilai desimal akan dipangkas/di-handle sesuai helper; untuk presisi penuh
	 * pakai varian {@code Double}.</p>
	 *
	 * @param sheet lembar kerja Excel
	 * @param col   indeks kolom
	 * @param row   indeks baris
	 * @return isi sel sebagai {@link Integer}
	 */
	public static Integer getSheetContentAsInteger(XSSFSheet sheet, Integer col, Integer row) {
		return CommonExcelContentHelper.getSheetContentAsInteger(sheet, col, row);
	}

	/**
	 * Membaca isi satu sel Excel sebagai {@link Long}.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Varian untuk nilai bilangan bulat besar (mis. NIK/nomor
	 * panjang) yang melampaui jangkauan {@link Integer}. Mendelegasikan ke
	 * {@code CommonExcelContentHelper.getSheetContentAsLong(sheet, col, row)}.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code sheet}/{@code col}/{@code row} seperti biasa. Mengembalikan
	 * {@link Long} (kemungkinan null/0 bila sel kosong/non-numerik).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Untuk nomor yang sebenarnya teks (mis. NIK berawalan 0), pakai
	 * {@code getSheetContentAsString} agar angka di depan tidak hilang.</p>
	 *
	 * @param sheet lembar kerja Excel
	 * @param col   indeks kolom
	 * @param row   indeks baris
	 * @return isi sel sebagai {@link Long}
	 */
	public static Long getSheetContentAsLong(XSSFSheet sheet, Integer col, Integer row) {
		return CommonExcelContentHelper.getSheetContentAsLong(sheet, col, row);
	}

	/**
	 * Membaca SELURUH isi lembar Excel menjadi matriks {@code List<List<String>>} (baris × kolom).
	 *
	 * <p><b>Tujuan.</b> Memuat seluruh tabel dari sebuah {@link XSSFSheet} dalam satu panggilan sebagai
	 * teks, memudahkan impor massal/iterasi baris demi baris tanpa berurusan dengan API POI di pemanggil.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonExcelContentHelper.getSheetContent(sheet)} yang
	 * menelusuri baris dan kolom, mengonversi tiap sel ke string (lihat {@link #getCellContent}), dan
	 * menyusunnya menjadi daftar baris berisi daftar kolom.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code sheet} = lembar kerja. Mengembalikan matriks string
	 * (baris luar, kolom dalam).</p>
	 *
	 * <p><b>Pemeliharaan &amp; kinerja.</b> Untuk lembar sangat besar, memuat semua ke memori bisa berat—
	 * pertimbangkan pemrosesan streaming bila perlu. Semua nilai berupa string; konversi tipe dilakukan
	 * pemanggil.</p>
	 *
	 * @param sheet lembar kerja Excel
	 * @return matriks isi lembar sebagai {@code List<List<String>>}
	 */
	public static List<List<String>> getSheetContent(XSSFSheet sheet) {
		return CommonExcelContentHelper.getSheetContent(sheet);
	}

	public static final DataFormatter df = new DataFormatter();

	/**
	 * Mengonversi sebuah sel Excel ({@link XSSFCell}) menjadi representasi STRING-nya, apa pun tipe selnya.
	 *
	 * <p><b>Tujuan.</b> Membaca nilai sel secara seragam sebagai teks—menangani perbedaan tipe sel POI
	 * (string, numerik, boolean, error, formula, kosong) dalam satu tempat sehingga pemanggil tidak
	 * perlu mengurus {@code switch} tipe sel berulang kali. Ini fondasi {@link #getSheetContent(XSSFSheet)}.</p>
	 *
	 * <h4>Cara kerja</h4>
	 * <ul>
	 *   <li>Mengembalikan {@code ""} bila {@code cell} null.</li>
	 *   <li>Memilih konversi berdasarkan {@code cell.getCellType()}: STRING → rich text; NUMERIC →
	 *   diformat via {@code Common.decimalFormat}; BLANK → ""; BOOLEAN/ERROR → string nilainya; FORMULA →
	 *   memakai {@code getCachedFormulaResultType()} untuk memutuskan numerik/string hasil formula;
	 *   default → {@code DataFormatter.formatCellValue}.</li>
	 * </ul>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code cell} = sel yang dibaca (boleh null). Mengembalikan isi
	 * sel sebagai string; {@code ""} bila null/blank.</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Seluruh konversi dibungkus try/catch yang menelan
	 * exception (mengembalikan apa yang sempat terbaca) agar satu sel bermasalah tidak menghentikan
	 * pembacaan lembar. Memakai {@code decimalFormat} ThreadLocal untuk format numerik konsisten. Bila
	 * menambah penanganan tipe baru, lakukan di {@code switch} ini agar seragam dengan pembacaan lembar.</p>
	 *
	 * @param cell sel Excel yang dibaca (boleh null)
	 * @return isi sel sebagai string ("" bila null/blank)
	 */
	public static String getCellContent(XSSFCell cell) {
		String content = "";
		if (cell == null) {
			return content;
		}
		try {

			switch (cell.getCellType()) {
			case XSSFCell.CELL_TYPE_STRING:
				content = cell.getRichStringCellValue().getString();
				break;
			case XSSFCell.CELL_TYPE_NUMERIC:
				if (org.zkoss.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
					content = df.formatCellValue(cell);
				} else {
					double numV = cell.getNumericCellValue();
					long numL = (long) numV;
					content = (numV == (double) numL)
							? String.valueOf(numL)
							: java.math.BigDecimal.valueOf(numV).stripTrailingZeros().toPlainString();
				}
				break;
			case XSSFCell.CELL_TYPE_BLANK:
				content = "";
				break;
			case XSSFCell.CELL_TYPE_BOOLEAN:
				content = cell.getBooleanCellValue() + "";
				break;
			case XSSFCell.CELL_TYPE_ERROR:
				content = cell.getErrorCellValue() + "";
				break;
			case XSSFCell.CELL_TYPE_FORMULA:
				switch (cell.getCachedFormulaResultType()) {
				case XSSFCell.CELL_TYPE_NUMERIC:
					if (org.zkoss.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
						content = df.formatCellValue(cell);
					} else {
						double numV = cell.getNumericCellValue();
						long numL = (long) numV;
						content = (numV == (double) numL)
								? String.valueOf(numL)
								: java.math.BigDecimal.valueOf(numV).stripTrailingZeros().toPlainString();
					}
					break;
				case XSSFCell.CELL_TYPE_STRING:
					content = cell.getRichStringCellValue().getString();
					break;
				}
				break;
			default:
				content = df.formatCellValue(cell);
				break;
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:4662");
//			tampilErrorJikaAdmin(e);
		}
		return content;
	}

	/**
	 * Membaca isi satu sel Excel sebagai {@link Boolean}.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Membaca nilai sel sebagai boolean (mis. kolom "ya/tidak",
	 * "true/false") untuk impor. Mendelegasikan ke
	 * {@code CommonExcelContentHelper.getSheetContentAsBoolean(sheet, col, row)} yang menafsirkan isi sel
	 * menjadi {@link Boolean} secara defensif.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code sheet}/{@code col}/{@code row} = lembar &amp; posisi.
	 * Mengembalikan {@link Boolean} (kemungkinan null bila sel kosong/tak dikenali).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Aturan penafsiran teks→boolean dijaga di helper; pastikan format kolom di
	 * template impor konsisten dengan aturan tsb.</p>
	 *
	 * @param sheet lembar kerja Excel
	 * @param col   indeks kolom
	 * @param row   indeks baris
	 * @return isi sel sebagai {@link Boolean}
	 */
	public static Boolean getSheetContentAsBoolean(XSSFSheet sheet, Integer col, Integer row) {
		return CommonExcelContentHelper.getSheetContentAsBoolean(sheet, col, row);
	}

	/**
	 * Membaca isi satu sel Excel sebagai {@link String}.
	 *
	 * <p><b>Tujuan.</b> Mengambil nilai sel apa adanya sebagai teks—pilihan teraman untuk kolom yang
	 * harus mempertahankan format asli (mis. NIK/kode berawalan 0, nomor telepon) yang akan rusak bila
	 * dibaca sebagai angka.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonExcelContentHelper.getSheetContentAsString(sheet, col, row)} yang mengonversi sel ke
	 * string (umumnya lewat logika serupa {@link #getCellContent(XSSFCell)}).</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code sheet}/{@code col}/{@code row} = lembar &amp; posisi.
	 * Mengembalikan isi sel sebagai {@link String} (kemungkinan "" bila kosong).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Untuk angka yang benar-benar numerik, pakai varian {@code Integer/Long/
	 * Double}; untuk identitas/kode, varian {@code String} ini lebih aman.</p>
	 *
	 * @param sheet lembar kerja Excel
	 * @param col   indeks kolom
	 * @param row   indeks baris
	 * @return isi sel sebagai {@link String}
	 */
	public static String getSheetContentAsString(XSSFSheet sheet, Integer col, Integer row) {
		return CommonExcelContentHelper.getSheetContentAsString(sheet, col, row);
	}

	/**
	 * Membaca isi satu sel Excel sebagai {@link Date} (format tanggal tampilan).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Mengambil nilai sel berformat tanggal dan mengonversinya ke
	 * {@link Date} Java. Mendelegasikan ke {@code CommonExcelContentHelper.getSheetContentAsDate}.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code sheet}/{@code col}/{@code row} = lembar &amp; posisi.
	 * Mengembalikan {@link Date} atau {@code null} bila sel kosong/bukan tanggal.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Untuk penyimpanan ke database, pakai {@link #getSheetContentAsDateDatabase}
	 * yang menggunakan zona waktu database.</p>
	 *
	 * @param sheet lembar kerja Excel
	 * @param col   indeks kolom (basis-0)
	 * @param row   indeks baris (basis-0)
	 * @return isi sel sebagai {@link Date}, atau {@code null}
	 */
	public static Date getSheetContentAsDate(XSSFSheet sheet, Integer col, Integer row) {
		return CommonExcelContentHelper.getSheetContentAsDate(sheet, col, row);
	}

	/**
	 * Membaca isi satu sel Excel sebagai {@link Date} yang siap disimpan ke database.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti {@link #getSheetContentAsDate} namun menerapkan
	 * normalisasi zona waktu / tengah-malam agar nilai tidak bergeser satu hari saat disimpan.
	 * Mendelegasikan ke {@code CommonExcelContentHelper.getSheetContentAsDateDatabase}.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Gunakan ini alih-alih {@link #getSheetContentAsDate} bila nilai
	 * langsung di-set ke entitas Hibernate untuk menghindari off-by-one hari akibat timezone.</p>
	 *
	 * @param sheet lembar kerja Excel
	 * @param col   indeks kolom (basis-0)
	 * @param row   indeks baris (basis-0)
	 * @return {@link Date} ternormalisasi siap simpan ke database, atau {@code null}
	 */
	public static Date getSheetContentAsDateDatabase(XSSFSheet sheet, Integer col, Integer row) {
		return CommonExcelContentHelper.getSheetContentAsDateDatabase(sheet, col, row);
	}

	/**
	 * Membaca isi satu sel Excel dan mengonversinya menjadi entitas/objek Hibernate berdasarkan {@code Class}.
	 *
	 * <p><b>Tujuan.</b> Impor data dari Excel yang berisi kode/nama yang merujuk ke entitas — mis. kode
	 * jurusan, nama dosen — secara otomatis dicari di database dan dikembalikan sebagai objek.</p>
	 *
	 * <p><b>Cara kerja.</b> Membaca teks sel lalu memanggil
	 * {@code CommonExcelContentHelper.getSheetContentAsObject(sheet, col, row, clazz)} yang mencocokkan
	 * nilai dengan entitas via {@link #getContentAsObject}. Tanpa {@link Criterion} tambahan.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Bila pencarian perlu dibatasi (mis. hanya entitas aktif), pakai overload
	 * dengan {@code criterion}.</p>
	 *
	 * @param sheet lembar kerja Excel
	 * @param col   indeks kolom (basis-0)
	 * @param row   indeks baris (basis-0)
	 * @param clazz kelas entitas tujuan
	 * @return entitas yang cocok, atau {@code null}
	 */
	@SuppressWarnings({ "rawtypes" })
	public static Object getSheetContentAsObject(XSSFSheet sheet, Integer col, Integer row, Class clazz) {
		return CommonExcelContentHelper.getSheetContentAsObject(sheet, col, row, clazz);
	}

	/**
	 * Mencari entitas Hibernate berdasarkan nilai teks dan kelas, dengan pemeriksaan ID (overload default).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Pintasan ke {@link #getContentAsObject(String, Class, Criterion, boolean)}
	 * dengan {@code checkId=true}: mencoba cocokkan {@code data} ke ID entitas terlebih dahulu,
	 * kemudian ke field lain sesuai implementasi {@code ObjectHelper}.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Bila data selalu berupa teks bukan ID numerik, {@code checkId=false}
	 * dapat mempercepat pencarian (lewati coba parse Long).</p>
	 *
	 * @param data      teks yang dibaca dari sel/form
	 * @param clazz     kelas entitas tujuan
	 * @param criterion pembatasan Hibernate tambahan (boleh null)
	 * @return entitas yang cocok, atau {@code null}
	 */
	@SuppressWarnings("rawtypes")
	public static Object getContentAsObject(String data, Class clazz, Criterion criterion) {
		return getContentAsObject(data, clazz, criterion, true);
	}

	/**
	 * Mencari entitas Hibernate berdasarkan nilai teks, kelas, dan criterion, dengan kontrol pemeriksaan ID.
	 *
	 * <p><b>Tujuan.</b> Mesin lookup umum untuk konversi teks→entitas yang dipakai oleh impor Excel,
	 * form web, dan berbagai pemanggil di seluruh aplikasi.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code ObjectHelper.getContentAsObject}: bila {@code checkId}
	 * aktif dan {@code data} adalah angka, mencoba {@code session.get(clazz, id)} dulu; bila gagal
	 * atau {@code checkId=false}, melakukan query berdasarkan field default kelas (nama/kode/nomor)
	 * dengan tambahan {@code criterion}.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Perubahan strategi pencarian dijaga di {@code ObjectHelper}; method ini
	 * murni delegasi. Pastikan field default kelas sudah diindeks DB untuk performa impor massal.</p>
	 *
	 * @param data      teks masukan (kode/nama/ID)
	 * @param clazz     kelas entitas tujuan
	 * @param criterion pembatasan Hibernate tambahan
	 * @param checkId   bila {@code true}, coba cocokkan sebagai ID numerik terlebih dahulu
	 * @return entitas yang cocok, atau {@code null}
	 */
	@SuppressWarnings("rawtypes")
	public static Object getContentAsObject(String data, Class clazz, Criterion criterion, boolean checkId) {
		return ObjectHelper.getContentAsObject(data, clazz, criterion, checkId);
	}

	/**
	 * Mengisi field-field entitas dari parameter HTTP request secara reflektif.
	 *
	 * <p><b>Tujuan.</b> Generik setter massal untuk impor/update dari form web: membaca nilai parameter
	 * request dan memetakannya ke properti entitas sesuai {@code properties} dan metadata Hibernate.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code ObjectHelper.setObjectValues(classMetadata, obj,
	 * properties, mulai, request)}, yang memakai refleksi Hibernate untuk set tipe yang tepat
	 * (String, Integer, Date, FK-entity, dll).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Indeks {@code mulai} menentukan offset kolom di {@code properties}.
	 * Return {@code Map} berisi pasangan properti→nilai (berguna untuk diagnostik/audit).</p>
	 *
	 * @param classMetadata metadata Hibernate kelas entitas
	 * @param obj           entitas yang akan diisi
	 * @param properties    daftar nama properti yang akan diisi
	 * @param mulai         indeks awal (offset) di array properties
	 * @param request       HTTP request sumber nilai
	 * @return map properti→nilai yang telah diisi
	 */
	@SuppressWarnings("rawtypes")
	public static Map setObjectValues(ClassMetadata classMetadata, Object obj, String[] properties, int mulai,
			HttpServletRequest request) {
		return ObjectHelper.setObjectValues(classMetadata, obj, properties, mulai, request);
	}

	/**
	 * Mengisi field-field entitas dari baris lembar Excel secara reflektif.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti {@link #setObjectValues(ClassMetadata, Object, String[], int, HttpServletRequest)}
	 * namun sumber nilainya baris Excel (POI). Mendelegasikan ke
	 * {@code ObjectHelper.setObjectValues(classMetadata, obj, properties, mulai, sheet, row)}.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pastikan urutan kolom Excel cocok dengan array {@code properties}; mismatch
	 * menyebabkan data tertukar diam-diam. Gunakan template import yang terdokumentasi.</p>
	 *
	 * @param classMetadata metadata Hibernate kelas entitas
	 * @param obj           entitas yang akan diisi
	 * @param properties    daftar nama properti yang akan diisi
	 * @param mulai         indeks awal (offset) di array properties
	 * @param sheet         lembar kerja Excel
	 * @param row           indeks baris Excel
	 * @return map properti→nilai yang telah diisi
	 */
	@SuppressWarnings("rawtypes")
	public static Map setObjectValues(ClassMetadata classMetadata, Object obj, String[] properties, int mulai,
			XSSFSheet sheet, int row) {
		return ObjectHelper.setObjectValues(classMetadata, obj, properties, mulai, sheet, row);
	}

	/**
	 * Membaca isi satu sel Excel dan mengonversinya ke entitas dengan pembatasan {@link Criterion}.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti {@link #getSheetContentAsObject(XSSFSheet, Integer, Integer, Class)}
	 * namun dengan {@code criterion} tambahan untuk mempersempit pencarian entitas (mis. hanya yang aktif,
	 * hanya milik institusi tertentu). Mendelegasikan ke
	 * {@code CommonExcelContentHelper.getSheetContentAsObject(sheet, col, row, clazz, criterion)}.</p>
	 *
	 * @param sheet     lembar kerja Excel
	 * @param col       indeks kolom
	 * @param row       indeks baris
	 * @param clazz     kelas entitas tujuan
	 * @param criterion pembatasan pencarian Hibernate
	 * @return entitas yang cocok, atau {@code null}
	 */
	@SuppressWarnings({ "rawtypes" })
	public static Object getSheetContentAsObject(XSSFSheet sheet, Integer col, Integer row, Class clazz,
			Criterion criterion) {
		return CommonExcelContentHelper.getSheetContentAsObject(sheet, col, row, clazz, criterion);
	}

	/**
	 * Mengambil sel ({@link XSSFCell}) dari lembar Excel pada posisi kolom dan baris tertentu.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Shortcut aman untuk mendapatkan objek sel POI tanpa memanggil
	 * {@code sheet.getRow(row).getCell(col)} berulang. Berguna bila pemanggil perlu akses langsung ke
	 * sel (mis. membaca style/format).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Baris/kolom harus valid; bila baris null (baris Excel kosong),
	 * {@code getRow} akan throw NPE — pastikan lembar sudah terisi.</p>
	 *
	 * @param sheet lembar kerja Excel
	 * @param col   indeks kolom (basis-0)
	 * @param row   indeks baris (basis-0)
	 * @return objek sel {@link XSSFCell}
	 */
	public static XSSFCell getCell(XSSFSheet sheet, Integer col, Integer row) {
		org.zkoss.poi.xssf.usermodel.XSSFRow xssfRow = sheet.getRow(row);
		if (xssfRow == null) return null;
		return xssfRow.getCell(col);
	}

	/**
	 * Mengambil semua lembar kerja ({@link XSSFSheet}) dari sebuah workbook Excel ke dalam {@link List}.
	 *
	 * <p><b>Tujuan.</b> Memudahkan iterasi semua sheet workbook tanpa harus memanggil
	 * {@code workbook.getNumberOfSheets()} dan {@code getSheetAt(i)} berulang di masing-masing pemanggil.</p>
	 *
	 * <p><b>Cara kerja.</b> Mengiterasi 0 hingga {@code numberOfSheets-1} dan mengumpulkan tiap
	 * {@link XSSFSheet} ke dalam daftar baru. Urutan sama dengan urutan tab di file Excel.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Daftar yang dikembalikan merupakan salinan referensi — modifikasi daftar
	 * tidak memengaruhi workbook, tapi sheet itu sendiri masih terhubung ke workbook.</p>
	 *
	 * @param workbook workbook Excel sumber
	 * @return daftar semua lembar kerja dalam workbook
	 */
	public static List<XSSFSheet> getAllXSSFSheet(XSSFWorkbook workbook) {
		List<XSSFSheet> sheets = new ArrayList<XSSFSheet>();
		for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
			XSSFSheet sheet = workbook.getSheetAt(i);
			sheets.add(sheet);
		}
		return sheets;
	}

	/**
	 * Menghitung tahun angkatan mahasiswa dari nomor semester dan jenis semester, menggunakan tahun akademik saat ini.
	 *
	 * <p><b>Tujuan.</b> Menentukan tahun angkatan (mis. 2022) dari konteks semester dan jenis semester,
	 * dengan referensi tahun diambil otomatis dari {@link #getCurrentTahunAkademik()}.</p>
	 *
	 * <p><b>Cara kerja.</b> Mengurai tahun dari tahun akademik aktif lalu memanggil overload
	 * {@link #getTahunAngkatan(Integer, String, Integer)}.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Bergantung pada konfigurasi tahun akademik aktif; pastikan
	 * {@code getCurrentTahunAkademik()} sudah dikonfigurasi dengan benar.</p>
	 *
	 * @param semester      nomor semester mahasiswa
	 * @param jenisSemester jenis semester ({@code Perkuliahan.GANJIL}/{@code GENAP})
	 * @return tahun angkatan mahasiswa
	 */
	public static Integer getTahunAngkatan(Integer semester, String jenisSemester) {
		// System.out.println("jenisSemester = " + jenisSemester);
		String ta = getCurrentTahunAkademik();
		Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
		return getTahunAngkatan(semester, jenisSemester, tahun);
	}

	/**
	 * Menghitung tahun angkatan mahasiswa dari nomor semester, jenis semester, dan tahun acuan eksplisit.
	 *
	 * <p><b>Cara kerja.</b> Untuk semester GANJIL: {@code tahun - (semester/2)};
	 * untuk GENAP: {@code tahun - ((semester-1)/2)}. Semester null/≤0 dinormalisasi ke 1.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Rumus ini menggunakan pembagian integer (floor). Pastikan konsistensi dengan
	 * {@link #getTahunAkademik} (fungsi invers) agar tidak terjadi inconsistency data angkatan.</p>
	 *
	 * @param semester      nomor semester mahasiswa
	 * @param jenisSemester jenis semester ({@code Perkuliahan.GANJIL}/{@code GENAP})
	 * @param tahun         tahun acuan (mis. tahun awal tahun akademik)
	 * @return tahun angkatan mahasiswa
	 */
	public static Integer getTahunAngkatan(Integer semester, String jenisSemester, Integer tahun) {
		semester = semester == null || semester <= 0 ? 1 : semester;
		// System.out.println("jenisSemester = " + jenisSemester);
		Integer angkatan = jenisSemester.equals(Perkuliahan.GANJIL) ? ((tahun - (semester / 2)))
				: ((tahun - ((semester - 1) / 2)));
		return angkatan;
	}

	/**
	 * Menghitung tahun akademik dari semester dan tahun angkatan (overload tanpa offset semester mulai).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Pintasan ke
	 * {@link #getTahunAkademik(Integer, Integer, Integer, String)} dengan {@code semesterMulai=0}.
	 * Mengembalikan tahun (angka) dari tahun akademik di mana mahasiswa berada pada semester tertentu.</p>
	 *
	 * @param semester            nomor semester mahasiswa
	 * @param tahunAngkatan       tahun angkatan mahasiswa
	 * @param awalMasukDiSemester semester awal masuk ({@code Perkuliahan.GANJIL}/{@code GENAP})
	 * @return tahun akademik (angka, mis. 2024)
	 */
	public static Integer getTahunAkademik(Integer semester, Integer tahunAngkatan, String awalMasukDiSemester) {
		return getTahunAkademik(semester, tahunAngkatan, 0, awalMasukDiSemester);
	}

	/**
	 * Menghitung tahun akademik dari semester, tahun angkatan, offset semester mulai, dan jenis masuk.
	 *
	 * <p><b>Cara kerja.</b> Fungsi invers dari {@link #getTahunAngkatan}:
	 * <ul>
	 *   <li>GANJIL: {@code tahunAngkatan + (semester-1)/2 - semesterMulai/2}</li>
	 *   <li>GENAP: {@code tahunAngkatan + semester/2 - semesterMulai/2}</li>
	 * </ul>
	 * Semester null/≤0 dinormalisasi ke 1.</p>
	 *
	 * <p><b>Pemeliharaan.</b> {@code semesterMulai} dipakai bila kurikulum memiliki offset semester
	 * (mis. mulai semester 3 bukan 1). Gunakan nilai 0 bila tidak ada offset.</p>
	 *
	 * @param semester            nomor semester mahasiswa
	 * @param tahunAngkatan       tahun angkatan mahasiswa
	 * @param semesterMulai       offset semester awal kurikulum
	 * @param awalMasukDiSemester semester awal masuk ({@code Perkuliahan.GANJIL}/{@code GENAP})
	 * @return tahun akademik (angka)
	 */
	public static Integer getTahunAkademik(Integer semester, Integer tahunAngkatan, Integer semesterMulai,
			String awalMasukDiSemester) {
		semester = semester == null || semester <= 0 ? 1 : semester;
		if (awalMasukDiSemester.equals(Perkuliahan.GANJIL)) {
			Integer tahunAkademik = (tahunAngkatan + ((semester - 1) / 2));
			return tahunAkademik - ((int) semesterMulai / 2);
		} else {
			Integer tahunAkademik = (tahunAngkatan + ((semester) / 2));
			return tahunAkademik - ((int) semesterMulai / 2);
		}
	}

	/**
	 * Menghitung tahun angkatan dari semester, jenis semester, dan string tahun akademik (mis. "2023/2024").
	 *
	 * <p><b>Cara kerja.</b> Mengurai tahun awal dari string tahun akademik (split "/", ambil elemen 0)
	 * lalu memanggil {@link #getTahunAngkatan(Integer, String, Integer)}.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Format string tahun akademik harus "YYYY/YYYY" — pisahkan dengan "/".</p>
	 *
	 * @param semester      nomor semester
	 * @param jenisSemester jenis semester
	 * @param tahunAkademik string tahun akademik (format "YYYY/YYYY")
	 * @return tahun angkatan mahasiswa
	 */
	public static Integer getTahunAngkatan(Integer semester, String jenisSemester, String tahunAkademik) {
		// System.out.println("jenisSemester = " + jenisSemester);
		Integer tahun = Integer.parseInt(tahunAkademik.split("/")[0].trim());
		return getTahunAngkatan(semester, jenisSemester, tahun);
	}

	/**
	 * Menyinkronkan (menghitung ulang) nilai huruf untuk semua mahasiswa, ditampilkan progresnya di label.
	 *
	 * <p><b>Tujuan.</b> Proses massal: iterasi semua detail perkuliahan yang belum/sudah mendapat nilai
	 * huruf dan menghitung ulang berdasarkan tabel {@link NilaiHuruf} aktif. Hasilnya ter-commit ke DB.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicKrsNilaiHelper.synNilaiHuruf(label, hanyaYangBelumdapatNilai)}. Label
	 * diperbarui selama proses untuk menampilkan progres.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Operasi berat — panggil di thread latar untuk menghindari timeout UI.
	 * {@code hanyaYangBelumdapatNilai=true} jauh lebih cepat untuk update inkremental.</p>
	 *
	 * @param label                   label ZK untuk menampilkan progres
	 * @param hanyaYangBelumdapatNilai bila {@code true}, hanya proses mahasiswa yang belum punya nilai huruf
	 */
	public static void synNilaiHuruf(Label label, boolean hanyaYangBelumdapatNilai) {
		CommonAcademicKrsNilaiHelper.synNilaiHuruf(label, hanyaYangBelumdapatNilai);
	}

	/**
	 * Menentukan {@link NilaiHuruf} yang sesuai untuk nilai angka mahasiswa berdasarkan konteks akademik.
	 *
	 * <p><b>Tujuan.</b> Mengonversi nilai angka (mis. 82.5) ke nilai huruf (mis. "A") sesuai tabel
	 * konversi yang dikonfigurasi per jurusan/fakultas/tahun angkatan/jenis penilaian.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicKrsNilaiHelper.getNilaiHuruf(...)}. Tabel dipilih berdasarkan
	 * {@code tahunAngkatan}/{@code jurusan}/{@code fakultas} dan filter {@code jenisNilaiHuruf}.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pastikan tabel {@link NilaiHuruf} terkonfigurasi untuk kombinasi
	 * jurusan/tahun yang dipakai; bila tidak ditemukan, helper biasanya mengembalikan default.</p>
	 *
	 * @param nilai           nilai angka mahasiswa
	 * @param tahunAngkatan   tahun angkatan mahasiswa
	 * @param jurusan         jurusan/prodi mahasiswa
	 * @param fakultas        fakultas mahasiswa
	 * @param tahunAkademik   tahun akademik (string)
	 * @param semester        semester (string)
	 * @param kodemk          kode matakuliah
	 * @param jenisNilaiHuruf jenis tabel nilai huruf
	 * @return {@link NilaiHuruf} yang cocok, atau default bila tidak ditemukan
	 */
	public static NilaiHuruf getNilaiHuruf(Double nilai, Integer tahunAngkatan, Jurusan jurusan, Fakultas fakultas,
			String tahunAkademik, String semester, String kodemk, JenisNilaiHurufMatakuliah jenisNilaiHuruf) {
		return CommonAcademicKrsNilaiHelper.getNilaiHuruf(nilai, tahunAngkatan, jurusan, fakultas, tahunAkademik,
				semester, kodemk, jenisNilaiHuruf);
	}

	/**
	 * Menentukan {@link NilaiHuruf} dengan kontrol mode percobaan dan cakupan kode matakuliah.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti
	 * {@link #getNilaiHuruf(Double, Integer, Jurusan, Fakultas, String, String, String, JenisNilaiHurufMatakuliah)}
	 * namun dengan dua flag tambahan:
	 * <ul>
	 *   <li>{@code coba}: bila {@code true}, tidak throw exception saat tabel tidak ditemukan (graceful).</li>
	 *   <li>{@code semuaKodeMk}: bila {@code true}, tidak memfilter per kode MK (pakai nilai huruf global).</li>
	 * </ul>
	 * Mendelegasikan ke {@code CommonAcademicKrsNilaiHelper.getNilaiHuruf(..., coba, semuaKodeMk, ...)}.</p>
	 *
	 * @param nilai           nilai angka mahasiswa
	 * @param tahunAngkatan   tahun angkatan mahasiswa
	 * @param jurusan         jurusan/prodi mahasiswa
	 * @param fakultas        fakultas mahasiswa
	 * @param tahunAkademik   tahun akademik (string)
	 * @param semester        semester (string)
	 * @param kodemk          kode matakuliah
	 * @param coba            bila {@code true}, gagal graceful (tanpa exception)
	 * @param semuaKodeMk     bila {@code true}, abaikan filter kode MK
	 * @param jenisNilaiHuruf jenis tabel nilai huruf
	 * @return {@link NilaiHuruf} yang cocok
	 */
	public static NilaiHuruf getNilaiHuruf(Double nilai, Integer tahunAngkatan, Jurusan jurusan, Fakultas fakultas,
			String tahunAkademik, String semester, String kodemk, boolean coba, boolean semuaKodeMk,
			JenisNilaiHurufMatakuliah jenisNilaiHuruf) {
		return CommonAcademicKrsNilaiHelper.getNilaiHuruf(nilai, tahunAngkatan, jurusan, fakultas, tahunAkademik,
				semester, kodemk, coba, semuaKodeMk, jenisNilaiHuruf);
	}

	/**
	 * Menentukan {@link NilaiHuruf} berdasarkan IP (Indeks Prestasi), bukan nilai angka matakuliah.
	 *
	 * <p><b>Tujuan.</b> Dipakai untuk menentukan predikat kelulusan (yudisium) atau kategorisasi
	 * mahasiswa berdasarkan IP, bukan nilai per-MK.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicKrsNilaiHelper.getNilaiHurufBerdasarkanIP(ip, tahunAngkatan, jurusan, fakultas)}.
	 * Menggunakan tabel nilai huruf khusus IP (bila dikonfigurasi) atau tabel default.</p>
	 *
	 * @param ip            nilai IP mahasiswa (0.0 – 4.0)
	 * @param tahunAngkatan tahun angkatan mahasiswa
	 * @param jurusan       jurusan/prodi mahasiswa
	 * @param fakultas      fakultas mahasiswa
	 * @return {@link NilaiHuruf} yang sesuai IP
	 */
	public static NilaiHuruf getNilaiHurufBerdasarkanIP(Double ip, Integer tahunAngkatan, Jurusan jurusan,
			Fakultas fakultas) {
		return CommonAcademicKrsNilaiHelper.getNilaiHurufBerdasarkanIP(ip, tahunAngkatan, jurusan, fakultas);
	}

	/**
	 * Menginisialisasi predikat judisium default di database bila belum ada.
	 *
	 * <p><b>Tujuan.</b> Menyediakan data master yudisium awal (mis. "Dengan Pujian", "Sangat Memuaskan",
	 * "Memuaskan", "Cukup") agar proses {@link #hitungJudisium} tidak gagal karena tabel kosong.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonAcademicSyncHelper.initDefaultJudisium()}
	 * yang memeriksa dan meng-insert predikat default bila belum ada.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Dipanggil sekali saat startup atau migrasi; idempoten (tidak duplikat
	 * bila sudah ada).</p>
	 */
	public static void initDefaultJudisium() {
		CommonAcademicSyncHelper.initDefaultJudisium();
	}

	/**
	 * Menghitung judisium (predikat kelulusan) mahasiswa berdasarkan KRS terakhir.
	 *
	 * <p><b>Tujuan.</b> Menentukan predikat wisuda mahasiswa (mis. "Dengan Pujian") dari IPK
	 * dan kondisi akademik lainnya sesuai peraturan institusi.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicKrsNilaiHelper.hitungJudisium(mahasiswa, krsMahasiswa)}.</p>
	 *
	 * @param mahasiswa    mahasiswa yang dihitung judisiumnya
	 * @param krsMahasiswa KRS terakhir mahasiswa
	 * @return {@link Judisium} yang sesuai, atau {@code null}
	 */
	public static Judisium hitungJudisium(Mahasiswa mahasiswa, KrsMahasiswa krsMahasiswa) {
		return CommonAcademicKrsNilaiHelper.hitungJudisium(mahasiswa, krsMahasiswa);
	}

	/**
	 * Menghitung judisium mahasiswa berdasarkan semester tertentu dan KRS-nya.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti {@link #hitungJudisium(Mahasiswa, KrsMahasiswa)} namun
	 * dengan parameter {@code smt} eksplisit untuk menghitung judisium pada semester tertentu
	 * (bukan selalu semester terakhir). Berguna untuk simulasi atau rekap historis.</p>
	 *
	 * @param mahasiswa    mahasiswa yang dihitung
	 * @param smt          nomor semester acuan
	 * @param krsMahasiswa KRS mahasiswa pada semester tersebut
	 * @return {@link Judisium} yang sesuai, atau {@code null}
	 */
	public static Judisium hitungJudisium(Mahasiswa mahasiswa, Integer smt, KrsMahasiswa krsMahasiswa) {
		return CommonAcademicKrsNilaiHelper.hitungJudisium(mahasiswa, smt, krsMahasiswa);
	}

	public static String subSqlHitungIp = " \nfrom detailperkuliahan a\n"
			+ "left join perkuliahan b on (a.perkuliahan = b.id)\nleft join\n(\n"
			+ "\tselect aa.id as id,\n\tmax(case when cc.id is null then aa.kode else cc.kode end) as kode,\n"
			+ "\tmax(case when cc.id is null then aa.nama else cc.nama end) as nama,\n"
			+ "\tmax(case when cc.id is null then aa.sks else cc.sks end) as sks,\n"
			+ "\tmax(kk.semester) as semester,\n\tmax(dd.tahun) as tahun\n\tfrom matakuliah aa\n"
			+ "\tleft join matakuliah_ekivalen bb on (aa.id = bb.matakuliah_ekivalen)\n"
			+ "\tleft join matakuliah cc on(bb.matakuliah = cc.id)\n"
			+ "\tleft join kurikulum_punya_matakuliah kk on ((case when cc.id is null then aa.id else cc.id end) = kk.matakuliah)\n"
			+ "\tleft join kurikulum dd on (dd.id = kk.kurikulum)\n"
			+ " \twhere (aa.extrakulikuler is null or aa.extrakulikuler = false)\n\tgroup by aa.id\n"
			+ ") f on ((f.id = b.matakuliah or a.matakuliah_konversi = f.id))\n ";

	/**
	 * Menghitung total angka kredit kegiatan kemahasiswaan mahasiswa yang telah disetujui.
	 *
	 * <p><b>Tujuan.</b> Mengakumulasi poin/kredit dari seluruh kegiatan kemahasiswaan (UKM, lomba,
	 * kepanitiaan, dll) yang berstatus "Disetujui" — dipakai untuk persyaratan kelulusan non-akademik.</p>
	 *
	 * <p><b>Cara kerja.</b> Menjalankan native SQL JOIN 6 tabel (kegiatan_kemahasiswaan_punya_mahasiswa,
	 * kegiatan_kemahasiswaan, kelompok_kegiatan, detail_kelompok, jabatan, skala, nilai_kegiatan) dan
	 * menjumlahkan {@code nilai_kegiatan_kemahasiswaan.nilai}. Hanya kegiatan dengan {@code status='Disetujui'}
	 * yang terhitung.</p>
	 *
	 * <p><b>Pemeliharaan.</b> SQL langsung (native), ID mahasiswa disubstitusikan inline — tidak rentan
	 * SQL injection karena ID selalu numerik. Bila skema berubah (tambah kolom/relasi), perbarui SQL ini.</p>
	 *
	 * @param mahasiswa mahasiswa yang dihitung angka kreditnya
	 * @return total angka kredit ({@link Double}), 0.0 bila belum ada kegiatan disetujui
	 */
	public static Double hitungAngkaKredit(Mahasiswa mahasiswa) {

		String sql = "select \n sum(h.nilai) as angka_kredit \nfrom kegiatan_kemahasiswaan_punya_mahasiswa a \n"
				+ "inner join kegiatan_kemahasiswaan b on (a.kegiatan_kemahasiswaan=b.id) \n"
				+ "inner join kelompok_kegiatan_kemahasiswaan d on (b.kelompok_kegiatan_kemahasiswaan=d.id) \n"
				+ "inner join detail_kelompok_kegiatan_kemahasiswaan e on (e.id=b.detail_kelompok_kegiatan_kemahasiswaan) \n"
				+ "left join jabatan_kegiatan_kemahasiswaan f on (f.id=a.jabatan_kegiatan_kemahasiswaan) \n"
				+ "inner join skala_kegiatan_kemahasiswaan g on (g.id=a.skala_kegiatan_kemahasiswaan) \n"
				+ "left join nilai_kegiatan_kemahasiswaan h on (h.skala_kegiatan_kemahasiswaan=a.skala_kegiatan_kemahasiswaan and (case when a.jabatan_kegiatan_kemahasiswaan is not null then h.jabatan_kegiatan_kemahasiswaan=a.jabatan_kegiatan_kemahasiswaan else a.jabatan_kegiatan_kemahasiswaan is null end) and h.detail_kelompok_kegiatan_kemahasiswaan=b.detail_kelompok_kegiatan_kemahasiswaan) \n"
				+

				"where a.mahasiswa=" + mahasiswa.getId() + " \n and b.status='Disetujui'";

		Number angkaKredit = ((Number) HibernateUtil.currentSession().createSQLQuery(sql).uniqueResult());

		// System.out.println("========== hitung angka kredit" + mahasiswa + " "
		// + sql + " ==> " + angkaKredit);
		return angkaKredit == null ? 0.0 : angkaKredit.doubleValue();
	}

	/**
	 * Mengambil data matakuliah dalam KRS mahasiswa dengan berbagai filter (semester, jenis, SP, konversi, dll).
	 *
	 * <p><b>Tujuan.</b> Sumber data utama untuk perhitungan IP, rekap SKS, dan tampilan transkrip:
	 * mengembalikan daftar baris matakuliah KRS beserta nilainya sesuai kombinasi filter yang diberikan.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicKrsNilaiHelper.dataMkKrs(mahasiswa, semester, genapGanjil, tahap, mk,
	 * ygSudahDisetujui, konversi, bukankonversi, sp)}. Tiap elemen array {@link Object[]} berisi
	 * field detailperkuliahan, perkuliahan, matakuliah, dan nilai.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Selalu tambahkan penjelasan filter baru di helper bila ada skema baru
	 * (mis. matakuliah modul, MK lintas prodi).</p>
	 *
	 * @param mahasiswa          mahasiswa yang dimintai datanya
	 * @param semester           semester tertentu ({@code null} = semua)
	 * @param genapGanjil        jenis semester (GANJIL/GENAP; {@code null} = semua)
	 * @param tahap              tahapan studi ({@code null} = semua)
	 * @param mk                 kode MK tertentu ({@code null} = semua)
	 * @param ygSudahDisetujui   bila {@code true}, hanya KRS yang sudah disetujui
	 * @param konversi           sertakan MK konversi
	 * @param bukankonversi      sertakan MK bukan konversi
	 * @param sp                 sertakan semester pendek
	 * @return daftar baris data MK-KRS sebagai {@code List<Object[]>}
	 */
	public static List<Object[]> dataMkKrs(Mahasiswa mahasiswa, Integer semester, String genapGanjil, Integer tahap,
			String mk, boolean ygSudahDisetujui, boolean konversi, boolean bukankonversi, boolean sp) {
		return CommonAcademicKrsNilaiHelper.dataMkKrs(mahasiswa, semester, genapGanjil, tahap, mk, ygSudahDisetujui,
				konversi, bukankonversi, sp);
	}

	/**
	 * Membuat {@link Criterion} Hibernate untuk filter rentang tanggal mulai–selesai perkuliahan.
	 *
	 * <p><b>Tujuan.</b> Standarisasi kondisi tumpang-tindih jadwal: dua slot waktu saling tumpang
	 * tindih bila {@code A.mulai < B.selesai AND A.selesai > B.mulai}. Berguna untuk validasi
	 * konflik jadwal perkuliahan.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonScheduleValidationHelper.getMulaiSampaiCriterion(perkuliahanDimulai, perkuliahanSampai)}
	 * yang menggunakan nama field default ("perkuliahanDimulai"/"perkuliahanSampai").</p>
	 *
	 * @param perkuliahanDimulai tanggal/waktu mulai yang akan dicek
	 * @param perkuliahanSampai  tanggal/waktu selesai yang akan dicek
	 * @return {@link Criterion} overlap untuk digunakan di query Hibernate
	 */
	public static Criterion getMulaiSampaiCriterion(Date perkuliahanDimulai, Date perkuliahanSampai) {
		return CommonScheduleValidationHelper.getMulaiSampaiCriterion(perkuliahanDimulai, perkuliahanSampai);
	}

	/**
	 * Membuat {@link Criterion} filter rentang tanggal dengan alias tabel Hibernate tertentu.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti {@link #getMulaiSampaiCriterion(Date, Date)} namun
	 * dengan {@code alias} eksplisit untuk query yang melibatkan join alias (mis. "p.perkuliahanDimulai").
	 * Mendelegasikan ke helper dengan parameter alias.</p>
	 *
	 * @param perkuliahanDimulai tanggal/waktu mulai
	 * @param perkuliahanSampai  tanggal/waktu selesai
	 * @param alias              alias tabel Hibernate (mis. "perkuliahan")
	 * @return {@link Criterion} overlap dengan alias
	 */
	public static Criterion getMulaiSampaiCriterion(Date perkuliahanDimulai, Date perkuliahanSampai, String alias) {
		return CommonScheduleValidationHelper.getMulaiSampaiCriterion(perkuliahanDimulai, perkuliahanSampai, alias);
	}

	/**
	 * Memeriksa apakah perkuliahan memiliki jadwal matakuliah yang sama (bukan paralel) yang bentrok.
	 *
	 * <p><b>Tujuan.</b> Validasi saat input/edit jadwal perkuliahan: mencegah dua slot mengajar
	 * matakuliah yang sama untuk kelas/prodi/semester yang sama pada rentang tanggal yang tumpang tindih.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonScheduleValidationHelper.checkMatakuliahKesamaanBukanParalel(...)}. Bila ada konflik
	 * dan {@code tampilWarning!=null}, menambahkan pesan ke Html; bila null, menampilkan Messagebox.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Perkuliahan paralel (mis. kelas A dan B di waktu bersamaan karena satu
	 * dosen) dikecualikan dari konflik—gunakan {@code hapusJugaParalel} di {@link #hapusPerkuliahan}
	 * untuk skenario tersebut.</p>
	 *
	 * @param perkuliahan        entitas jadwal yang sedang diperiksa (dikecualikan dari query)
	 * @param jurusan            jurusan/prodi
	 * @param kelas              kelas
	 * @param matakuliah         matakuliah
	 * @param semester           semester
	 * @param tahunAjaran        tahun ajaran
	 * @param program            program studi (reguler/RPL/dll)
	 * @param tampilWarning      Html untuk menampilkan pesan konflik (boleh null → Messagebox)
	 * @param semesterpendek     semester pendek (0 = bukan SP)
	 * @param minggu1..minggu5   flag minggu aktif
	 * @param perkuliahanDimulai tanggal mulai
	 * @param perkuliahanSampai  tanggal selesai
	 * @param merupakanRemedial  bila {@code true}, slot ini adalah remedial
	 * @return entitas {@link Perkuliahan} yang konflik, atau {@code null} bila aman
	 * @throws Exception bila terjadi error DB
	 */
	public static Perkuliahan checkMatakuliahKesamaanBukanParalel(Perkuliahan perkuliahan, Jurusan jurusan,
			String kelas, Matakuliah matakuliah, Integer semester, String tahunAjaran, String program,
			Html tampilWarning, Integer semesterpendek, Boolean minggu1, Boolean minggu2, Boolean minggu3,
			Boolean minggu4, Boolean minggu5, Date perkuliahanDimulai, Date perkuliahanSampai,
			Boolean merupakanRemedial) throws Exception {
		return CommonScheduleValidationHelper.checkMatakuliahKesamaanBukanParalel(perkuliahan, jurusan, kelas,
				matakuliah, semester, tahunAjaran, program, tampilWarning, semesterpendek, minggu1, minggu2, minggu3,
				minggu4, minggu5, perkuliahanDimulai, perkuliahanSampai, merupakanRemedial);
	}

	/**
	 * Membuat {@link Criterion} Hibernate untuk deteksi overlap slot waktu (jam mulai–selesai).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Menghasilkan kondisi OR ({@code A.mulai < selesai AND
	 * A.selesai > mulai}) untuk dua field waktu ({@code Double} = jam desimal, mis. 7.5 = 07:30).
	 * Dipakai oleh {@code checkJadwalDosen}/{@code checkJadwalRuangPerkuliahan} di {@code CommonScheduleValidationHelper}.</p>
	 *
	 * @param mulai   jam mulai yang akan dicek (Double, format jam desimal)
	 * @param selesai jam selesai yang akan dicek
	 * @return {@link Criterion} overlap waktu
	 */
	public static Criterion createOrJadwalMulaiSelesai(Double mulai, Double selesai) {
		return CommonScheduleValidationHelper.createOrJadwalMulaiSelesai(mulai, selesai);
	}

	/**
	 * Memeriksa konflik jadwal kelas perkuliahan (dua slot kelas/semester sama yang tumpang tindih).
	 *
	 * <p><b>Tujuan.</b> Validasi jadwal: mencegah satu kelas/prodi memiliki dua perkuliahan berbeda
	 * pada hari dan jam yang sama. Bila konflik ditemukan, menampilkan pesan peringatan.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicSyncHelper.checkKelasJadwalPerkuliahan(...)}. Query mengecualikan
	 * perkuliahan dengan ID {@code id} (entitas yang sedang diedit).</p>
	 *
	 * @param id              ID perkuliahan yang sedang diedit (dikecualikan; {@code null} untuk baru)
	 * @param jurusan         jurusan/prodi
	 * @param program         program studi
	 * @param hari            hari kuliah
	 * @param mulai           jam mulai (desimal)
	 * @param selesai         jam selesai (desimal)
	 * @param tahunAjaran     tahun ajaran
	 * @param jenisSemester   jenis semester
	 * @param kelas           kelas
	 * @param semester        semester
	 * @param tampilWarning   Html pesan peringatan (boleh null)
	 * @param semesterpendek  flag semester pendek
	 * @param minggu1..minggu5 flag minggu aktif
	 * @param perkuliahanDimulai tanggal mulai
	 * @param perkuliahanSampai  tanggal selesai
	 * @param matakuliah      matakuliah
	 * @return entitas {@link Perkuliahan} yang konflik, atau {@code null}
	 * @throws Exception bila terjadi error DB
	 */
	public static Perkuliahan checkKelasJadwalPerkuliahan(Long id, Jurusan jurusan, String program, String hari,
			Double mulai, Double selesai, String tahunAjaran, String jenisSemester, String kelas, Integer semester,
			Html tampilWarning, Integer semesterpendek, Boolean minggu1, Boolean minggu2, Boolean minggu3,
			Boolean minggu4, Boolean minggu5, Date perkuliahanDimulai, Date perkuliahanSampai, Matakuliah matakuliah)
			throws Exception {
		return CommonAcademicSyncHelper.checkKelasJadwalPerkuliahan(id, jurusan, program, hari, mulai, selesai,
				tahunAjaran, jenisSemester, kelas, semester, tampilWarning, semesterpendek, minggu1, minggu2, minggu3,
				minggu4, minggu5, perkuliahanDimulai, perkuliahanSampai, matakuliah);
	}

	/**
	 * Memeriksa konflik penggunaan ruang perkuliahan pada slot waktu yang sama.
	 *
	 * <p><b>Tujuan.</b> Memastikan tidak ada dua perkuliahan yang dijadwalkan di ruang yang sama
	 * pada hari dan jam yang tumpang tindih, untuk menghindari konflik penggunaan ruang.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonScheduleValidationHelper.checkJadwalRuangPerkuliahan(...)}. Bila konflik
	 * ditemukan, menampilkan peringatan ke {@code tampilWarning} atau Messagebox.</p>
	 *
	 * @param id              ID perkuliahan yang dikecualikan (yang sedang diedit)
	 * @param ruang           ruang yang akan dicek
	 * @param hari            hari
	 * @param mulai           jam mulai (desimal)
	 * @param selesai         jam selesai (desimal)
	 * @param tahunAjaran     tahun ajaran
	 * @param jenisSemester   jenis semester
	 * @param tampilWarning   Html pesan peringatan
	 * @param semesterpendek  flag semester pendek
	 * @param minggu1..minggu5 flag minggu aktif
	 * @param perkuliahanDimulai tanggal mulai
	 * @param perkuliahanSampai  tanggal selesai
	 * @return {@link Perkuliahan} yang konflik, atau {@code null}
	 * @throws Exception bila terjadi error DB
	 */
	public static Perkuliahan checkJadwalRuangPerkuliahan(Long id, Ruang ruang, String hari, Double mulai,
			Double selesai, String tahunAjaran, String jenisSemester, Html tampilWarning, Integer semesterpendek,
			Boolean minggu1, Boolean minggu2, Boolean minggu3, Boolean minggu4, Boolean minggu5,
			Date perkuliahanDimulai, Date perkuliahanSampai) throws Exception {
		return CommonScheduleValidationHelper.checkJadwalRuangPerkuliahan(id, ruang, hari, mulai, selesai, tahunAjaran,
				jenisSemester, tampilWarning, semesterpendek, minggu1, minggu2, minggu3, minggu4, minggu5,
				perkuliahanDimulai, perkuliahanSampai);
	}

	/**
	 * Memeriksa konflik jadwal mengajar dosen pada slot waktu perkuliahan tertentu.
	 *
	 * <p><b>Tujuan.</b> Mencegah satu dosen dijadwalkan mengajar dua matakuliah berbeda pada hari
	 * dan jam yang tumpang tindih dalam satu tahun ajaran/semester.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonScheduleValidationHelper.checkJadwalDosen(...)}. Seperti check ruang/kelas, query
	 * mengecualikan entitas yang sedang diedit ({@code id}).</p>
	 *
	 * @param id              ID perkuliahan yang dikecualikan
	 * @param hari            hari
	 * @param mulai           jam mulai (desimal)
	 * @param selesai         jam selesai (desimal)
	 * @param dosen           dosen yang diperiksa
	 * @param tahunAjaran     tahun ajaran
	 * @param jenisSemester   jenis semester
	 * @param jurusan         jurusan
	 * @param matakuliah      matakuliah
	 * @param kelas           kelas
	 * @param tampilWarning   Html pesan peringatan
	 * @param semesterpendek  flag semester pendek
	 * @param minggu1..minggu5 flag minggu aktif
	 * @param perkuliahanDimulai tanggal mulai
	 * @param perkuliahanSampai  tanggal selesai
	 * @return {@link Perkuliahan} yang konflik, atau {@code null}
	 * @throws Exception bila terjadi error DB
	 */
	public static Perkuliahan checkJadwalDosen(Long id, String hari, Double mulai, Double selesai, Dosen dosen,
			String tahunAjaran, String jenisSemester, Jurusan jurusan, Matakuliah matakuliah, String kelas,
			Html tampilWarning, Integer semesterpendek, Boolean minggu1, Boolean minggu2, Boolean minggu3,
			Boolean minggu4, Boolean minggu5, Date perkuliahanDimulai, Date perkuliahanSampai) throws Exception {
		return CommonScheduleValidationHelper.checkJadwalDosen(id, hari, mulai, selesai, dosen, tahunAjaran,
				jenisSemester, jurusan, matakuliah, kelas, tampilWarning, semesterpendek, minggu1, minggu2, minggu3,
				minggu4, minggu5, perkuliahanDimulai, perkuliahanSampai);
	}

	/**
	 * Memeriksa konflik matakuliah bukan-paralel pada template perkuliahan.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti
	 * {@link #checkMatakuliahKesamaanBukanParalel(Perkuliahan, Jurusan, String, Matakuliah, Integer, String, String, Html, Integer, Boolean, Boolean, Boolean, Boolean, Boolean, Date, Date, Boolean)}
	 * namun untuk entitas {@link TemplatePerkuliahan} (jadwal template, bukan jadwal aktif semester).
	 * Mendelegasikan ke {@code CommonScheduleValidationHelper}.</p>
	 *
	 * @param templatePerkuliahanDetail detail template yang sedang diperiksa
	 * @param templatePerkuliahan       template induk
	 * @param jurusan                   jurusan
	 * @param kelas                     kelas
	 * @param matakuliah                matakuliah
	 * @param semester                  semester
	 * @param program                   program studi
	 * @param tampilWarning             Html peringatan
	 * @return {@link TemplatePerkuliahanDetail} yang konflik, atau {@code null}
	 * @throws Exception bila terjadi error DB
	 */
	public static TemplatePerkuliahanDetail checkMatakuliahKesamaanBukanParalel(
			TemplatePerkuliahanDetail templatePerkuliahanDetail, TemplatePerkuliahan templatePerkuliahan,
			Jurusan jurusan, String kelas, Matakuliah matakuliah, Integer semester, String program, Html tampilWarning)
			throws Exception {
		return CommonScheduleValidationHelper.checkMatakuliahKesamaanBukanParalel(templatePerkuliahanDetail,
				templatePerkuliahan, jurusan, kelas, matakuliah, semester, program, tampilWarning);
	}

	/**
	 * Memeriksa konflik jadwal kelas pada detail template perkuliahan.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Varian {@link #checkKelasJadwalPerkuliahan} untuk template.
	 * Mendelegasikan ke {@code CommonAcademicSyncHelper.checkKelasJadwalTemplatePerkuliahanDetail(...)}.
	 * Validasi dijalankan saat admin menyiapkan template jadwal sebelum generate semester baru.</p>
	 *
	 * @param templatePerkuliahan template jadwal induk
	 * @param id                  ID detail template yang dikecualikan
	 * @param jurusan             jurusan
	 * @param program             program studi
	 * @param hari                hari
	 * @param mulai               jam mulai
	 * @param selesai             jam selesai
	 * @param jenisSemester       jenis semester
	 * @param kelas               kelas
	 * @param semester            semester
	 * @param matakuliah          matakuliah
	 * @param tampilWarning       Html peringatan
	 * @return {@link TemplatePerkuliahanDetail} yang konflik, atau {@code null}
	 * @throws Exception bila terjadi error DB
	 */
	public static TemplatePerkuliahanDetail checkKelasJadwalTemplatePerkuliahanDetail(
			TemplatePerkuliahan templatePerkuliahan, Long id, Jurusan jurusan, String program, String hari,
			Double mulai, Double selesai, String jenisSemester, String kelas, Integer semester, Matakuliah matakuliah,
			Html tampilWarning) throws Exception {
		return CommonAcademicSyncHelper.checkKelasJadwalTemplatePerkuliahanDetail(templatePerkuliahan, id, jurusan,
				program, hari, mulai, selesai, jenisSemester, kelas, semester, matakuliah, tampilWarning);
	}

	/**
	 * Memeriksa konflik penggunaan ruang pada detail template perkuliahan.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Varian {@link #checkJadwalRuangPerkuliahan} untuk template.
	 * Mendelegasikan ke {@code CommonScheduleValidationHelper.checkJadwalTemplateRuangPerkuliahanDetail(...)}.
	 * Berguna untuk memastikan konsistensi template sebelum dipakai untuk generate jadwal aktif.</p>
	 *
	 * @param templatePerkuliahan template jadwal induk
	 * @param id                  ID detail yang dikecualikan
	 * @param ruang               ruang yang diperiksa
	 * @param hari                hari
	 * @param mulai               jam mulai
	 * @param selesai             jam selesai
	 * @param jenisSemester       jenis semester
	 * @param jurusan             jurusan
	 * @param matakuliah          matakuliah
	 * @param kelas               kelas
	 * @param tampilWarning       Html peringatan
	 * @return {@link TemplatePerkuliahanDetail} yang konflik, atau {@code null}
	 * @throws Exception bila terjadi error DB
	 */
	public static TemplatePerkuliahanDetail checkJadwalTemplateRuangPerkuliahanDetail(
			TemplatePerkuliahan templatePerkuliahan, Long id, Ruang ruang, String hari, Double mulai, Double selesai,
			String jenisSemester, Jurusan jurusan, Matakuliah matakuliah, String kelas, Html tampilWarning)
			throws Exception {
		return CommonScheduleValidationHelper.checkJadwalTemplateRuangPerkuliahanDetail(templatePerkuliahan, id, ruang,
				hari, mulai, selesai, jenisSemester, jurusan, matakuliah, kelas, tampilWarning);
	}

	/**
	 * Memeriksa konflik jadwal dosen pada detail template perkuliahan.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Varian {@link #checkJadwalDosen} untuk konteks template jadwal.
	 * Mendelegasikan ke {@code CommonScheduleValidationHelper.checkJadwalDosen(templatePerkuliahan, ...)}.
	 * Template tidak memiliki rentang tanggal konkret, sehingga hanya hari dan jam yang diperiksa.</p>
	 *
	 * @param templatePerkuliahan template jadwal induk
	 * @param id                  ID detail yang dikecualikan
	 * @param hari                hari
	 * @param mulai               jam mulai
	 * @param selesai             jam selesai
	 * @param dosen               dosen yang diperiksa
	 * @param jenisSemester       jenis semester
	 * @param jurusan             jurusan
	 * @param matakuliah          matakuliah
	 * @param kelas               kelas
	 * @param tampilWarning       Html peringatan
	 * @return {@link TemplatePerkuliahanDetail} yang konflik, atau {@code null}
	 * @throws Exception bila terjadi error DB
	 */
	public static TemplatePerkuliahanDetail checkJadwalDosen(TemplatePerkuliahan templatePerkuliahan, Long id,
			String hari, Double mulai, Double selesai, Dosen dosen, String jenisSemester, Jurusan jurusan,
			Matakuliah matakuliah, String kelas, Html tampilWarning) throws Exception {
		return CommonScheduleValidationHelper.checkJadwalDosen(templatePerkuliahan, id, hari, mulai, selesai, dosen,
				jenisSemester, jurusan, matakuliah, kelas, tampilWarning);
	}

	/**
	 * Mengambil aturan pembatasan IPK untuk pengambilan KRS mahasiswa berdasarkan IPK dan semester.
	 *
	 * <p><b>Tujuan.</b> Setiap pengambilan KRS mempunyai batas maksimal SKS yang bergantung IPK semester
	 * sebelumnya. Method ini mencari aturan {@link PembatasanNilaiIPKUntukPengambilanKRS} yang berlaku
	 * untuk mahasiswa pada semester dan konteks akademik tertentu.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicSyncHelper.getIpkUntukPengambilanKRS(...)}. Aturan dicari berdasarkan
	 * kombinasi jurusan/fakultas/program/semester pendek.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Bila tidak ada aturan yang cocok, helper mengembalikan aturan default
	 * ({@link PembatasanNilaiIPKUntukPengambilanKRS#getDefaultPembatasanNilaiIpUntukAmbilKRS()}).</p>
	 *
	 * @param mahasiswa      mahasiswa yang dicek
	 * @param semester       semester yang akan diambil KRS-nya
	 * @param tahunAngkatan  tahun angkatan mahasiswa
	 * @param fakultas       fakultas mahasiswa
	 * @param jurusan        jurusan/prodi mahasiswa
	 * @param program        program studi
	 * @param semesterPendek flag semester pendek (0 = bukan SP)
	 * @return aturan pembatasan IPK yang berlaku, atau {@code null} bila tidak dikonfigurasi
	 */
	public static PembatasanNilaiIPKUntukPengambilanKRS getIpkUntukPengambilanKRS(Mahasiswa mahasiswa, Integer semester,
			Integer tahunAngkatan, Fakultas fakultas, Jurusan jurusan, String program, Integer semesterPendek) {
		return CommonAcademicSyncHelper.getIpkUntukPengambilanKRS(mahasiswa, semester, tahunAngkatan, fakultas, jurusan,
				program, semesterPendek);
	}

	/**
	 * Mengambil IP (Indeks Prestasi) semester terakhir yang diperoleh mahasiswa untuk referensi batas SKS KRS.
	 *
	 * <p><b>Tujuan.</b> Menentukan IP yang dipakai untuk menghitung batas SKS KRS semester berikutnya:
	 * mencari IP semester sebelumnya yang valid (bukan cuti, bukan semester tidak aktif).</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ul>
	 *   <li>Semester 1: langsung kembalikan 4.0 (tidak ada riwayat).</li>
	 *   <li>Bila konfigurasi {@code pembatasan_maksimal_sks...ip_semester_sebelum_nya} aktif: iterasi
	 *       mundur dari {@code semester-1} hingga 1, lewati semester cuti/non-aktif, ambil IPS pertama
	 *       yang valid.</li>
	 *   <li>Bila konfigurasi tidak aktif: gunakan IPK (kumulatif), bukan IPS.</li>
	 *   <li>Bila IP null/NaN/&lt;0.01: kembalikan 0.5 (aman, batas SKS minimum).</li>
	 * </ul></p>
	 *
	 * <p><b>Pemeliharaan.</b> Logika ini berimplikasi luas pada jumlah SKS yang bisa diambil mahasiswa.
	 * Setiap perubahan harus ditest di skenario cuti, mahasiswa baru, dan semester non-aktif.</p>
	 *
	 * @param mahasiswa mahasiswa yang dicari IP-nya
	 * @param semester  semester yang akan diambil KRS-nya (acuan untuk mencari semester sebelumnya)
	 * @return nilai IP terakhir yang valid sebagai acuan batas SKS
	 */
	public static Double ipTerakhir(Mahasiswa mahasiswa, Integer semester) {
		Double iplast = 4.0;
		if (!semester.equals(1)) {

			if (Common.bolehKonfigurasi("pembatasan_maksimal_sks_pada_pegambilan_krs_berdasarkan_ip_semester_sebelum_nya")) {
				// System.out.println("Hitung berdasarkan IP semester
				// sebelumnya");

				boolean ada = false;
				for (int i = semester - 1; i >= 1; i--) {
					PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa.ambilCuti(i, null, false);
					int countCuti = pendaftaranCutiMahasiswa != null && pendaftaranCutiMahasiswa.getPersetujuan() ? 1
							: 0;
					if (countCuti == 0) {

						KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, i, null, null);

						HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil
								.getHistoryStatusMahasiswa(krsMahasiswa);
						StatusMahasiswa statusHistory = historyStatusMahasiswa == null ? null
								: historyStatusMahasiswa.getStatusMahasiswa();
						if (historyStatusMahasiswa == null || statusHistory == null || statusHistory.getId() == null
								|| ConstantValues.AKTIF == null || ConstantValues.AKTIF.getId() == null
								|| statusHistory.getId().equals(ConstantValues.AKTIF.getId())) {

							iplast = krsMahasiswa.getIps();
							ada = true;
							break;
						}
					}
				}

				if (!ada) {
					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester - 1, null, null);
					iplast = krsMahasiswa.getIps();
				}

			} else {
				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester - 1, null, null);
				iplast = krsMahasiswa.getIpk();
			}
		}

		if (iplast == null || iplast.isNaN() || iplast < 0.01) {
			iplast = 0.5;
		}
		return iplast;
	}

	/**
	 * Mengambil aturan pembatasan IPK KRS beserta nilai IP terakhir dalam satu array.
	 *
	 * <p><b>Tujuan.</b> Menggabungkan hasil {@link #getIpkUntukPengambilanKRS} dan {@link #ipTerakhir}
	 * dalam satu panggilan—berguna untuk UI yang menampilkan aturan dan IP aktual secara bersamaan.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicSyncHelper.getIpkUntukPengambilanKRSDenganIPLast(...)}. Elemen array:
	 * {@code [0]} = {@link PembatasanNilaiIPKUntukPengambilanKRS}, {@code [1]} = IP terakhir ({@link Double}).</p>
	 *
	 * @param mahasiswa      mahasiswa
	 * @param semester       semester yang akan diambil
	 * @param tahunAngkatan  tahun angkatan
	 * @param fakultas       fakultas
	 * @param jurusan        jurusan
	 * @param program        program studi
	 * @param semesterPendek flag semester pendek
	 * @return array {@code [PembatasanNilaiIPK, Double ipTerakhir]}
	 */
	public static Object[] getIpkUntukPengambilanKRSDenganIPLast(Mahasiswa mahasiswa, Integer semester,
			Integer tahunAngkatan, Fakultas fakultas, Jurusan jurusan, String program, Integer semesterPendek) {
		return CommonAcademicSyncHelper.getIpkUntukPengambilanKRSDenganIPLast(mahasiswa, semester, tahunAngkatan,
				fakultas, jurusan, program, semesterPendek);
	}

	/**
	 * Memeriksa apakah jumlah SKS yang diambil mahasiswa melebihi batas maksimal berdasarkan IP-nya.
	 *
	 * <p><b>Tujuan.</b> Validasi saat pengambilan KRS: mencegah mahasiswa mengambil terlalu banyak SKS
	 * bila IPK/IPS-nya di bawah ambang yang dikonfigurasi.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Semester &lt; minimal ({@code minimal_smt_syarat_krs}, default 2): langsung {@code false}
	 *       (belum ada riwayat IP).</li>
	 *   <li>Semester 1: tidak ada aturan pembatasan ({@code null}).</li>
	 *   <li>Ambil {@code maxSks} dari {@link #getIpkUntukPengambilanKRS}; bila {@code null}, pakai
	 *       default {@link PembatasanNilaiIPKUntukPengambilanKRS#getDefaultPembatasanNilaiIpUntukAmbilKRS()}.</li>
	 *   <li>Bila {@code jumlahSksYangDiambil > maxSks}: tampilkan Messagebox peringatan dan return
	 *       {@code true}.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan.</b> Konfigurasi minimum semester bisa diubah via
	 * {@code getKonfigurasi("minimal_smt_syarat_krs")}. Pastikan tabel aturan terkonfigurasi untuk
	 * semua kombinasi fakultas/jurusan yang ada.</p>
	 *
	 * @param mahasiswa             mahasiswa yang mengambil KRS
	 * @param semester              semester yang akan diambil
	 * @param jumlahSksYangDiambil  jumlah SKS yang akan diambil
	 * @param semesterPendek        flag semester pendek
	 * @return {@code true} bila SKS melebihi batas (pengambilan DITOLAK), {@code false} bila aman
	 * @throws Exception bila terjadi error DB atau parsing
	 */
	public static boolean checkPembatasanSKSBerdasarkanIP(Mahasiswa mahasiswa, Integer semester,
			Integer jumlahSksYangDiambil, Integer semesterPendek) throws Exception {

		int minimalSmtSyaratKrs = 2;
		try {
			minimalSmtSyaratKrs = Integer.parseInt(Common.getKonfigurasi("minimal_smt_syarat_krs", "2").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:5756");
			// TODO: handle exception
		}

		if (semester != null && semester < minimalSmtSyaratKrs) {
			return false;
		}

		Fakultas fakultas = mahasiswa.getJurusan().getFakultas();
		Jurusan jurusan = mahasiswa.getJurusan();

		PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRS = semester.equals(1) ? null
				: getIpkUntukPengambilanKRS(mahasiswa, semester, mahasiswa.getTahunangkatan(), fakultas, jurusan,
						mahasiswa.getProgram(), semesterPendek);

		Integer maxsks = (Integer) (pembatasanNilaiIPKUntukPengambilanKRS == null
				? PembatasanNilaiIPKUntukPengambilanKRS.getDefaultPembatasanNilaiIpUntukAmbilKRS()
				: pembatasanNilaiIPKUntukPengambilanKRS.getBatasMaksimumIPKYangBolehDiambil());

		boolean result = jumlahSksYangDiambil > maxsks;
		if (result) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, pengambilan KRS belum dapat diproses. Pada semester {V1}, jumlah SKS yang diambil "
							+ "sebanyak {V2} SKS telah melebihi batas maksimal yang diperkenankan. Mahasiswa dengan NIM "
							+ "{V3} atas nama {V4} hanya diperkenankan mengambil paling banyak {V5} SKS. Langkah yang "
							+ "dapat dilakukan: (1) kurangi jumlah mata kuliah yang diambil sehingga total SKS tidak "
							+ "melebihi batas; (2) hubungi Dosen Pembimbing Akademik apabila memerlukan penambahan "
							+ "batas SKS; (3) simpan kembali pengambilan KRS Anda.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
					semester, jumlahSksYangDiambil, mahasiswa.getNim(), mahasiswa.getNama(), maxsks);
		}
		return result;
	}

	/**
	 * Menginisialisasi combobox Perguruan Tinggi dengan data aktif dan memilih item sesuai konteks pengguna.
	 *
	 * <p><b>Tujuan.</b> Menyiapkan dropdown pemilihan perguruan tinggi di form dengan perilaku konteks:
	 * administrator umum dapat memilih semua PT, pengguna terikat fakultas hanya melihat PT miliknya.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Mengisi combobox dengan semua {@link PerguruanTinggi} yang aktif
	 *       ({@code aktif=null OR aktif=true}).</li>
	 *   <li>Bila {@code perguruanTinggi!=null}: pilih PT tsb.</li>
	 *   <li>Combobox di-{@code setReadonly(true)} agar tidak bisa diketik bebas.</li>
	 *   <li>Bila pengguna saat ini terikat ke PT tertentu via fakultas: otomatis pilih PT tsb dan
	 *       disable combobox (multi-tenant isolation).</li>
	 *   <li>Bila tidak ada yang terpilih, pilih item pertama.</li>
	 *   <li>Lebar combobox diset 90%.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan.</b> Pastikan semua form yang memiliki field PT memanggil method ini agar
	 * isolasi tenant konsisten. Bila ada PT non-aktif yang perlu tampil di konteks tertentu,
	 * tambahkan overload dengan criterion eksplisit.</p>
	 *
	 * @param searchPerguruanTinggi combobox tujuan
	 * @param perguruanTinggi       PT yang akan dipilih awal (boleh {@code null})
	 */
	public static void initComboPerguruanTinggi(Combobox searchPerguruanTinggi, PerguruanTinggi perguruanTinggi) {
		Tbmuser tbmuser = Common.getCurrentUser();

		Common.insertCombo(searchPerguruanTinggi, "nama", PerguruanTinggi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (perguruanTinggi != null) {
			Common.selectComboItem(searchPerguruanTinggi, perguruanTinggi);
		}
		searchPerguruanTinggi.setReadonly(true);

		if (tbmuser != null && tbmuser.ambilFakultas() != null
				&& tbmuser.ambilFakultas().getPerguruanTinggi() != null) {
			Common.selectComboItem(searchPerguruanTinggi, tbmuser.ambilFakultas().getPerguruanTinggi());
			searchPerguruanTinggi.setDisabled(true);
		} else if (!searchPerguruanTinggi.getChildren().isEmpty() && searchPerguruanTinggi.getSelectedItem() == null) {
			searchPerguruanTinggi.setSelectedIndex(0);
		}
		searchPerguruanTinggi.setWidth("90%");
	}

	/**
	 * Mengonversi {@link Media} (upload ZK) menjadi {@link Blob} Hibernate untuk disimpan ke DB (overload tanpa session).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Pintasan ke {@link #getBlobFromMedia(Media, Session)} dengan
	 * {@code streamingSession=null} — menggunakan sesi default. Cocok untuk upload berkas kecil yang tidak
	 * memerlukan sesi streaming terpisah.</p>
	 *
	 * @param media berkas yang diupload via ZK
	 * @return {@link Blob} siap simpan ke kolom {@code bytea}/blob DB
	 * @throws Exception bila konversi gagal
	 */
	public static Blob getBlobFromMedia(Media media) throws Exception {
		return getBlobFromMedia(media, null);
	}

	/**
	 * Mengonversi {@link Media} (upload ZK) menjadi {@link Blob} Hibernate, dengan dukungan sesi streaming.
	 *
	 * <p><b>Tujuan.</b> Menyimpan berkas yang diupload pengguna ke dalam kolom BLOB di database.
	 * ZK {@link Media} bisa berupa stream, string, atau byte[] tergantung tipe berkas dan konfigurasi.</p>
	 *
	 * <p><b>Cara kerja.</b> Mencoba tiga pendekatan secara berurutan (try-catch berantai):
	 * <ol>
	 *   <li>{@code Hibernate.createBlob(media.getStreamData())} — stream langsung (paling hemat memori).</li>
	 *   <li>{@code Hibernate.createBlob(media.getStringData().getBytes())} — fallback teks.</li>
	 *   <li>{@code Hibernate.createBlob(media.getByteData())} — fallback byte array.</li>
	 * </ol>
	 * Menggunakan {@code @SuppressWarnings("deprecation")} karena {@code Hibernate.createBlob} deprecated
	 * di Hibernate 4+ namun masih dipakai untuk kompatibilitas PostgreSQL.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Parameter {@code streamingSession} saat ini tidak dipakai oleh implementasi
	 * (hanya untuk signature kompatibilitas); referensi ke factory streaming ada di
	 * {@code FileFoto.writeBlobFromDedicatedTransaction}.</p>
	 *
	 * @param media            berkas yang diupload via ZK
	 * @param streamingSession sesi streaming (untuk skenario factory khusus; boleh {@code null})
	 * @return {@link Blob} hasil konversi
	 * @throws Exception bila semua tiga cara konversi gagal
	 */
	@SuppressWarnings("deprecation")
	public static Blob getBlobFromMedia(Media media, Session streamingSession) throws Exception {

		Blob blob = null;
		try {
			blob = (Hibernate.createBlob(media.getStreamData()));
		} catch (Exception e) {
			try {
				blob = (Hibernate.createBlob(media.getStringData().getBytes()));
			} catch (Exception ee) {
				blob = (Hibernate.createBlob(media.getByteData()));
			}
		}
		return blob;
	}

	/**
	 * Memeriksa apakah baris form biodata dosen perlu ditampilkan, mengatur label otomatis.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Pintasan ke overload dengan {@code label=null} — label dibuat
	 * baru dari {@code BiodataDosenAction.MAPPING_DATA}. Baris ditampilkan bila status konfigurasi
	 * bukan {@code TIDAK_AKTIF}.</p>
	 *
	 * @param row ZK row form biodata
	 * @param key kunci field biodata dosen (sesuai MAPPING_DATA)
	 * @return status konfigurasi tampilan (AKTIF/READ_ONLY/dll)
	 */
	public static String checkApakahLabelDosenTampil(Row row, String key) {
		return checkApakahLabelDosenTampil(row, key, null);
	}

	/**
	 * Memeriksa apakah baris form biodata dosen perlu ditampilkan, dengan label kustom opsional.
	 *
	 * <p><b>Tujuan.</b> Mengontrol visibilitas field biodata dosen berdasarkan konfigurasi
	 * {@code KonfigurasiTampilanBiodataDosenAction}: field wajib, read-only, read-only-kecuali-admin,
	 * aktif-tidak-wajib, atau tidak tampil sama sekali.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Memanggil {@code KonfigurasiTampilanBiodataDosenAction.statusWajibIsi(key)}.</li>
	 *   <li>Set {@code row.setVisible()} berdasarkan status (tampil bila bukan {@code TIDAK_AKTIF}).</li>
	 *   <li>Buat atau isi {@code label}: teks dari {@code MAPPING_DATA.get(key)} + " (*)" bila wajib.</li>
	 *   <li>Tambahkan label ke row.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan.</b> MAPPING_DATA dan statusWajibIsi dijaga di {@code BiodataDosenAction}.
	 * Gunakan overload dengan {@code label=null} untuk kasus standar; overload ini bila label perlu
	 * konfigurasi tambahan (sclass, tooltip, dll) sebelum ditambahkan ke row.</p>
	 *
	 * @param row   ZK row form biodata
	 * @param key   kunci field biodata
	 * @param label label kustom (boleh {@code null}, dibuat otomatis bila null)
	 * @return status konfigurasi tampilan field
	 */
	public static String checkApakahLabelDosenTampil(Row row, String key, MyLabelConfig label) {
		String statusWajibIsi = KonfigurasiTampilanBiodataDosenAction.statusWajibIsi(key);
		row.setVisible(statusWajibIsi.equals(Konfigurasi.AKTIF)
				|| (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
				|| statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
				|| statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB));
		if (label == null) {
			row.appendChild(new ais.ui.util.MyLabelConfig((BiodataDosenAction.MAPPING_DATA.get(key) + " "
					+ (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")).trim()));
		} else {
			label.setValue((BiodataDosenAction.MAPPING_DATA.get(key) + " "
					+ (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")).trim());
			row.appendChild(label);
		}
		return statusWajibIsi;

	}

	/**
	 * Memeriksa apakah baris form biodata guru perlu ditampilkan (overload tanpa label kustom).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Pintasan ke {@link #checkApakahLabelGuruTampil(Row, String, MyLabelConfig)}
	 * dengan {@code label=null}. Identik dengan {@link #checkApakahLabelDosenTampil} namun menggunakan
	 * konfigurasi dan mapping data {@link GuruAction}.</p>
	 *
	 * @param row ZK row form biodata guru
	 * @param key kunci field biodata guru
	 * @return status konfigurasi tampilan
	 */
	public static String checkApakahLabelGuruTampil(Row row, String key) {
		return checkApakahLabelGuruTampil(row, key, null);
	}

	/**
	 * Memeriksa apakah baris form biodata guru perlu ditampilkan, dengan label kustom opsional.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti {@link #checkApakahLabelDosenTampil(Row, String, MyLabelConfig)}
	 * namun menggunakan konfigurasi {@code KonfigurasiTampilanGuruAction.statusWajibIsi(key)} dan
	 * {@code GuruAction.MAPPING_DATA} sebagai sumber teks label. Mengontrol visibilitas field biodata
	 * guru berdasarkan konfigurasi institusi.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Ketiga varian ({@code Dosen}/{@code Guru}/{@code Pegawai}) mengikuti
	 * pola yang sama — bila ada perubahan perilaku (mis. penambahan status baru), terapkan di ketiganya.</p>
	 *
	 * @param row   ZK row form biodata guru
	 * @param key   kunci field biodata guru
	 * @param label label kustom (boleh {@code null})
	 * @return status konfigurasi tampilan field
	 */
	public static String checkApakahLabelGuruTampil(Row row, String key, MyLabelConfig label) {
		String statusWajibIsi = KonfigurasiTampilanGuruAction.statusWajibIsi(key);
		row.setVisible(statusWajibIsi.equals(Konfigurasi.AKTIF)
				|| (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
						|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
				|| statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
				|| statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB));
		if (label == null) {
			row.appendChild(new ais.ui.util.MyLabelConfig(
					(GuruAction.MAPPING_DATA.get(key) + " " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : ""))
							.trim()));
		} else {
			label.setValue(
					(GuruAction.MAPPING_DATA.get(key) + " " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : ""))
							.trim());
			row.appendChild(label);
		}
		return statusWajibIsi;
	}

	/**
	 * Memeriksa apakah baris form biodata pegawai perlu ditampilkan (overload tanpa label kustom).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Pintasan ke
	 * {@link #checkApakahLabelPegawaiTampil(Row, String, MyLabelConfig)} dengan {@code label=null}.
	 * Menggunakan konfigurasi {@code KonfigurasiTampilanPegawaiAction} dan
	 * {@code FormBiodataPegawaiUtil.MAPPING_DATA}.</p>
	 *
	 * @param row ZK row form biodata pegawai
	 * @param key kunci field biodata pegawai
	 * @return status konfigurasi tampilan
	 */
	public static String checkApakahLabelPegawaiTampil(Row row, String key) {
		return checkApakahLabelPegawaiTampil(row, key, null);
	}

	/**
	 * Memeriksa apakah baris form biodata pegawai perlu ditampilkan, dengan label kustom opsional.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti varian Dosen dan Guru, namun untuk entitas Pegawai.
	 * Menggunakan {@code KonfigurasiTampilanPegawaiAction.statusWajibIsi(key)} dan
	 * {@code FormBiodataPegawaiUtil.MAPPING_DATA}. Exception ditangkap secara silent
	 * ({@link Common#tampilErrorJikaAdmin}) dan mengembalikan {@code ""} bila error — lebih defensif
	 * dibanding varian dosen/guru yang tidak punya try-catch.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Bila key tidak ada di MAPPING_DATA, label menampilkan "null (*)" atau
	 * sejenisnya — pastikan semua key field pegawai sudah didaftarkan di MAPPING_DATA.</p>
	 *
	 * @param row   ZK row form biodata pegawai
	 * @param key   kunci field biodata pegawai
	 * @param label label kustom (boleh {@code null})
	 * @return status konfigurasi tampilan field, atau {@code ""} bila terjadi exception
	 */
	public static String checkApakahLabelPegawaiTampil(Row row, String key, MyLabelConfig label) {
		try {
			String statusWajibIsi = KonfigurasiTampilanPegawaiAction.statusWajibIsi(key);
//			System.out.println(key + " " + statusWajibIsi);
			row.setVisible(statusWajibIsi.equals(Konfigurasi.AKTIF)
					|| (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
					|| statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
					|| statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB));
			if (label == null) {
				row.appendChild(new ais.ui.util.MyLabelConfig((FormBiodataPegawaiUtil.MAPPING_DATA.get(key) + " "
						+ (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")).trim()));
			} else {
				label.setValue((FormBiodataPegawaiUtil.MAPPING_DATA.get(key) + " "
						+ (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")).trim());
				row.appendChild(label);
			}
			return statusWajibIsi;
		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
		}
		return "";
	}

	/**
	 * Memeriksa apakah kombinasi username dan password valid untuk autentikasi.
	 *
	 * <p><b>Tujuan.</b> Verifikasi kredensial pengguna — dipakai di alur login, reset sandi,
	 * atau fitur yang memerlukan konfirmasi ulang kata sandi.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonSecurityLoginHelper.checkLogin(username, password)}
	 * yang mencari pengguna aktif dengan username tersebut dan memverifikasi hash password (biasanya
	 * BCrypt/MD5 sesuai konfigurasi). Tidak mempertimbangkan status sesi yang sedang aktif.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Algoritma hash password dijaga di helper. Bila ada perubahan
	 * mekanisme enkripsi, pastikan helper diperbarui; method ini tetap sebagai fasad.</p>
	 *
	 * @param username nama pengguna
	 * @param password kata sandi (plaintext; hash dilakukan di helper)
	 * @return {@code true} bila kredensial valid, {@code false} bila tidak
	 */
	public static boolean checkLogin(String username, String password) {
		return CommonSecurityLoginHelper.checkLogin(username, password);
	}


	/**
	 * Menghapus jadwal perkuliahan beserta data pertemuan yang terkait, dengan validasi integritas FK.
	 *
	 * <h4>Tujuan</h4>
	 * <p>Penghapusan perkuliahan memerlukan urutan validasi ketat: tidak boleh ada mahasiswa terdaftar,
	 * tidak boleh ada perkuliahan paralel (kecuali {@code hapusJugaParalel=true}), dan pertemuan harus
	 * dihapus dulu sebelum entitas induk.</p>
	 *
	 * <h4>Cara kerja</h4>
	 * <ol>
	 *   <li><b>Validasi paralel:</b> bila ada perkuliahan paralel dan {@code hapusJugaParalel=false}:
	 *       tampilkan peringatan dan return {@code false}. Bila {@code true}: rekursif hapus semua
	 *       perkuliahan paralel terlebih dahulu.</li>
	 *   <li><b>Validasi mahasiswa:</b> bila ada {@link Detailperkuliahan} yang bukan konversi (tanpa
	 *       {@code ikutiPerkuliahan}): tidak bisa dihapus, tampilkan peringatan, return {@code false}.</li>
	 *   <li><b>Hapus pertemuan:</b> {@code DELETE FROM pertemuan WHERE perkuliahan=id} via native SQL.</li>
	 *   <li><b>Hapus entitas:</b> {@link Common#refreshDelete(Session, Object)}.</li>
	 *   <li>Panggil {@code defaultListener.onSearchDefault(null)} bila ada.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan error.</b> Exception ditangkap dan ditampilkan via
	 * {@link Common#tampilErrorJikaAdmin(Exception)}; {@link HibernateUtil#closeSession()} dipanggil
	 * di branch error.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Native SQL hapus pertemuan menggunakan ID langsung — bila tabel
	 * pertemuan punya FK ke tabel lain (mis. absensi, jawaban), tambahkan DELETE bertahap.
	 * Pastikan mode rekursif {@code hapusJugaParalel} tidak menyebabkan stack overflow pada
	 * rantai paralel yang sangat panjang.</p>
	 *
	 * @param perkuliahan       jadwal perkuliahan yang akan dihapus
	 * @param defaultListener   listener untuk refresh grid setelah hapus (boleh {@code null})
	 * @param hapusJugaParalel  bila {@code true}, ikut menghapus semua perkuliahan paralel
	 * @param tampilWarning     Html untuk menampilkan pesan gagal (boleh {@code null} → Messagebox)
	 * @return {@code true} bila berhasil dihapus, {@code false} bila ada constraint yang mencegah
	 * @throws Exception bila terjadi error tak terduga
	 */
	@SuppressWarnings("unchecked")
	public static boolean hapusPerkuliahan(Perkuliahan perkuliahan, OnSearchDefaultListener defaultListener,
			boolean hapusJugaParalel, final Html tampilWarning) throws Exception {

		String content = "Gagal menghapus jadwal perkuliahan : <br>Matakuliah: " + perkuliahan.getMatakuliah().getNama()
				+ ", Dosen : " + (perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama())
				+ ", Hari : " + (perkuliahan.getHari() == null ? "" : perkuliahan.getHari()) + ", Jam : "
				+ (perkuliahan.getWaktuMulai() + "-" + perkuliahan.getWaktuSelesai()) + ", Ruang : "
				+ (perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama())
				+ (perkuliahan.getJurusan() == null ? ""
						: ", Prodi : " + (perkuliahan.getJurusan() + "-" + perkuliahan.getJurusan().getNama()));

		Session session = HibernateUtil.currentSession();
		try {

			// Integer count = ((Number)
			// session.createCriteria(Pertemuan.class).add(Restrictions.or(Restrictions.isNull("aktif"),
			// Restrictions.eq("aktif", true)))
			// .add(Restrictions.eq("perkuliahan",
			// perkuliahan)).setProjection(Projections.rowCount())
			// .uniqueResult()).intValue();
			//
			// if (!count.equals(0)) {
			// if (tampilWarning == null) {
			// MyMessageboxConfig.show(
			// "Perkuliahan ini mempunyai " + count
			// + " data pertemuan, anda tidak bisa menghapus perkuliahan ini. "
			// + content,
			// "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.EXCLAMATION);
			// } else {
			// String val = tampilWarning.getContent() + "<li><font
			// color='red'>";
			// val += "Perkuliahan ini mempunyai " + count
			// + " data pertemuan, anda tidak bisa menghapus perkuliahan ini. "
			// + content;
			//
			// val += "</font></li>";
			// tampilWarning.setContent(val);
			// }
			//
			// HibernateUtil.closeSession();
			// return false;
			// }

			Integer count = ((Number) session.createCriteria(Perkuliahan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("perkuliahan_paralel", perkuliahan)).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();

			if (!count.equals(0)) {

				if (hapusJugaParalel) {

					List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("perkuliahan_paralel", perkuliahan)).list();
					for (Perkuliahan myPerkuliahan : perkuliahans) {
						Common.hapusPerkuliahan(myPerkuliahan, defaultListener, hapusJugaParalel, tampilWarning);
					}

				} else {
					if (tampilWarning == null) {
						MyMessageboxConfig.show("Perkuliahan ini mempunyai " + count
								+ " data perkuliahan paralel, anda tidak bisa menghapus perkuliahan ini. " + content,
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					} else {
						String val = tampilWarning.getContent() + "<li><font color='red'>";
						val += "Perkuliahan ini mempunyai " + count
								+ " data perkuliahan paralel, anda tidak bisa menghapus perkuliahan ini. " + content;

						val += "</font></li>";
						tampilWarning.setContent(val);
					}

					HibernateUtil.closeSession();
					return false;
				}
			}

			count = ((Number) session.createCriteria(Detailperkuliahan.class)
					.add(Restrictions.isNull("ikutiPerkuliahan")).add(Restrictions.eq("perkuliahan", perkuliahan))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();

			if (!count.equals(0)) {
				if (tampilWarning == null) {
					MyMessageboxConfig.show(
							"Perkuliahan ini mempunyai " + count
									+ " data Mahasiswa, anda tidak bisa menghapus perkuliahan ini. " + content,
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				} else {
					String val = tampilWarning.getContent() + "<li><font color='red'>";
					val += "Perkuliahan ini mempunyai " + count
							+ " data Mahasiswa, anda tidak bisa menghapus perkuliahan ini. " + content;

					val += "</font></li>";
					tampilWarning.setContent(val);
				}

				HibernateUtil.closeSession();
				return false;
			}

			session.createSQLQuery("delete from pertemuan where perkuliahan=" + perkuliahan.getId()).executeUpdate();
			Common.refreshDelete(session, (perkuliahan));

			if (defaultListener != null) {
				defaultListener.onSearchDefault(null);
			}
			return true;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			if (tampilWarning == null) {
				PesanFormalHelper.tampilkanGagalException("penghapusan data " + content, e,
						new String[] {
								"Data ini kemungkinan masih berelasi/dipakai oleh data lain (mis. pertemuan, nilai, "
										+ "atau presensi) sehingga tidak dapat dihapus secara langsung.",
								"Hapus atau lepaskan terlebih dahulu data terkait yang masih berelasi, lalu ulangi "
										+ "penghapusan." });
			} else {
				String val = tampilWarning.getContent() + "<li><font color='red'>";
				val += content
						+ ". Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
						+ e.getMessage();

				val += "</font></li>";
				tampilWarning.setContent(val);
			}
		}
		return false;
	}

	/**
	 * Memeriksa apakah satu jadwal perkuliahan bertabrakan dengan salah satu jadwal dalam daftar.
	 *
	 * <p><b>Tujuan.</b> Deteksi konflik jam antar slot perkuliahan dalam daftar yang sudah terpilih:
	 * berguna saat input KRS mahasiswa untuk memperingatkan jadwal bentrok.</p>
	 *
	 * <p><b>Cara kerja.</b> Membandingkan {@code perkuliahan1} dengan setiap {@code perkuliahan}
	 * di {@code selectedperkuliahans}: hari harus sama, kemudian salah satu dari 5 kondisi overlap
	 * jam (mulai/selesai ±0.01 untuk toleransi floating-point, termasuk kondisi identik) harus terpenuhi.
	 * Exception per-perkuliahan diabaikan diam-diam.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Toleransi 0.01 jam = ~36 detik. Bila aturan bentrok perlu diperketat
	 * (mis. toleransi 0), ubah konstanta ini. Perhatikan bahwa {@code getAbaikanWaktuBentrokDenganJadwalLain}
	 * tidak diperiksa di sini — hanya di {@link #generateInformasiJamBentrok}.</p>
	 *
	 * @param selectedperkuliahans daftar jadwal yang sudah terpilih
	 * @param perkuliahan1         jadwal baru yang ingin diperiksa
	 * @return {@code true} bila ada bentrok, {@code false} bila aman
	 */
	public static Boolean checkJamBentrok(List<Perkuliahan> selectedperkuliahans, Perkuliahan perkuliahan1) {

		try {

			for (Perkuliahan perkuliahan : selectedperkuliahans) {
				try {
					if (((perkuliahan.getHari() == null ? "" : perkuliahan.getHari()).equals(perkuliahan1.getHari())) &&

							(

							(perkuliahan.getWaktuMulaiD() >= perkuliahan1.getWaktuMulaiD() + 0.01
									&& perkuliahan.getWaktuMulaiD() <= perkuliahan1.getWaktuSelesaiD() - 0.01)

									||

									(perkuliahan.getWaktuSelesaiD() >= perkuliahan1.getWaktuMulaiD() + 0.01
											&& perkuliahan.getWaktuSelesaiD() <= perkuliahan1.getWaktuSelesaiD() - 0.01)

									||

									(perkuliahan1.getWaktuMulaiD() >= perkuliahan.getWaktuMulaiD() + 0.01
											&& perkuliahan1.getWaktuMulaiD() <= perkuliahan.getWaktuSelesaiD() - 0.01)

									||

									(perkuliahan1.getWaktuSelesaiD() >= perkuliahan.getWaktuMulaiD() + 0.01
											&& perkuliahan1.getWaktuSelesaiD() <= perkuliahan.getWaktuSelesaiD() - 0.01)

									||

									(Common.numberFormat.get().format(perkuliahan1.getWaktuMulaiD())
											.equals(Common.numberFormat.get().format(perkuliahan.getWaktuMulaiD()))
											&& Common.numberFormat.get().format(perkuliahan1.getWaktuSelesaiD()).equals(
													Common.numberFormat.get().format(perkuliahan.getWaktuSelesaiD())))

							)

					) {
						return true;
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		return false;
	}

	/**
	 * Menghasilkan Html berisi daftar konflik jadwal antar perkuliahan dalam satu KRS mahasiswa.
	 *
	 * <p><b>Tujuan.</b> Menampilkan informasi bentrok jadwal secara terperinci kepada mahasiswa atau
	 * operator: MK apa, jam berapa, dengan MK mana yang bentrok.</p>
	 *
	 * <p><b>Cara kerja.</b> Iterasi semua pasangan {@link Detailperkuliahan} dalam list, memeriksa
	 * overlap hari dan jam (lima kondisi ±0.01 plus kondisi identik). Setiap pasangan bentrok dicatat
	 * satu kali (via {@code Set<String> sudahAda}). Perkuliahan dengan flag
	 * {@code abaikanWaktuBentrokDenganJadwalLain=true} atau {@code merupakan_tanpa_jadwal=true}
	 * dikecualikan. Hasil disimpan sebagai {@link Html} dengan atribut {@code "adabentrok"} (Boolean).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Verifikasi juga termasuk rentang tanggal ({@code perkuliahanDimulai/Sampai})
	 * bila keduanya non-null, dengan toleransi ±60ms (milidetik). Bila ada skema jadwal baru
	 * (mis. multi-minggu), perbarui logika overlap di sini.</p>
	 *
	 * @param detailperkuliahans daftar {@link Detailperkuliahan} dalam KRS mahasiswa
	 * @return {@link Html} berisi pesan bentrok; atribut "adabentrok" = true bila ada konflik
	 */
	public static Html generateInformasiJamBentrok(List<Detailperkuliahan> detailperkuliahans) {

		String bentrok = "<ol>";
		Set<String> sudahAda = new HashSet<String>();
		int jumlah = 0;
		boolean adabentrok = false;
		for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {
			if (detailperkuliahan != null) {
				Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan() == null
						? detailperkuliahan.getIkutiPerkuliahan()
						: detailperkuliahan.getPerkuliahan();
				if (perkuliahan == null || perkuliahan.getAbaikanWaktuBentrokDenganJadwalLain()) {
					continue;
				}
				if (perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan()) {
					continue;
				}

				for (Detailperkuliahan detailperkuliahan1 : detailperkuliahans) {

					if (detailperkuliahan1 != null) {
						try {
							Perkuliahan perkuliahan1 = detailperkuliahan1.getPerkuliahan();
							if (perkuliahan1 == null || perkuliahan.getId().equals(perkuliahan1.getId())
									|| perkuliahan1.getAbaikanWaktuBentrokDenganJadwalLain()) {
								continue;
							}
							if (perkuliahan1.getMerupakan_tanpa_jadwal_perkuliahan()) {
								continue;
							}
							String kode = perkuliahan.getId() + "_" + perkuliahan1.getId();
							String kode1 = perkuliahan1.getId() + "_" + perkuliahan.getId();

							Long start1 = perkuliahan.getPerkuliahanDimulai() == null ? null
									: perkuliahan.getPerkuliahanDimulai().getTime() + 60L;
							Long end1 = perkuliahan.getPerkuliahanSampai() == null ? null
									: perkuliahan.getPerkuliahanSampai().getTime() - 60L;

							Long start2 = perkuliahan1.getPerkuliahanDimulai() == null ? null
									: perkuliahan1.getPerkuliahanDimulai().getTime() + 60L;
							Long end2 = perkuliahan1.getPerkuliahanSampai() == null ? null
									: perkuliahan1.getPerkuliahanSampai().getTime() - 60L;

							boolean ada = true;
							if (start1 != null && end1 != null && start2 != null && end2 != null) {
								ada = (start1 >= start2 && start1 <= end2) || (end1 >= start2 && end1 <= end2);
							}

							if (ada && (!sudahAda.contains(kode) && !sudahAda.contains(kode1))
									&& (perkuliahan.getHari().equals(perkuliahan1.getHari())) && (

									(perkuliahan.getWaktuMulaiD() >= perkuliahan1.getWaktuMulaiD() + 0.01
											&& perkuliahan.getWaktuMulaiD() <= perkuliahan1.getWaktuSelesaiD() - 0.01)

											||

											(perkuliahan.getWaktuSelesaiD() >= perkuliahan1.getWaktuMulaiD() + 0.01
													&& perkuliahan.getWaktuSelesaiD() <= perkuliahan1.getWaktuSelesaiD()
															- 0.01)

											||

											(perkuliahan1.getWaktuMulaiD() >= perkuliahan.getWaktuMulaiD() + 0.01
													&& perkuliahan1.getWaktuMulaiD() <= perkuliahan.getWaktuSelesaiD()
															- 0.01)

											||

											(perkuliahan1.getWaktuSelesaiD() >= perkuliahan.getWaktuMulaiD() + 0.01
													&& perkuliahan1.getWaktuSelesaiD() <= perkuliahan.getWaktuSelesaiD()
															- 0.01)

											||

											(Common.numberFormat.get().format(perkuliahan1.getWaktuMulaiD()).equals(
													Common.numberFormat.get().format(perkuliahan.getWaktuMulaiD()))
													&& Common.numberFormat.get().format(perkuliahan1.getWaktuSelesaiD())
															.equals(Common.numberFormat.get()
																	.format(perkuliahan.getWaktuSelesaiD())))

									)) {
								adabentrok = true;
								sudahAda.add(kode);
								sudahAda.add(kode1);
								bentrok += "<li>Jadwal perkuliahan <font style='font-weight:bold;color:red;'>"
										+ perkuliahan.getMatakuliah().getNama() + " waktu "
										+ (perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai())
										+ " s.d "
										+ (perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai())
										+ " " + perkuliahan.getHari()
										+ "</font> bentrok dengan jadwal perkuliahan <font style='font-weight:bold;color:red;'>"
										+ perkuliahan1.getMatakuliah().getNama() + " waktu "
										+ perkuliahan1.getWaktuMulai() + " s.d " + perkuliahan1.getWaktuSelesai() + " "
										+ perkuliahan1.getHari() + "</font></li>";
								jumlah++;
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:6429");

						}
					}
				}
			}
		}
		bentrok += "</ol>";
		Html html;
		if (bentrok.equalsIgnoreCase("<ol></ol>")) {
			html = (new ais.ui.util.MyHtml("<font style='font-size:9px;'>--</font>"));
		} else {
			html = (new ais.ui.util.MyHtml(
					"<font style='font-size:9px;'>Terdapat <font style='font-weight:bold;color:red;'>" + jumlah
							+ "</font> jadwal perkuliahan yang bentrok, yaitu:<br>" + bentrok + "</font>"));
		}
		html.setAttribute("adabentrok", adabentrok);
		return html;
	}

	/**
	 * Menghasilkan Html berisi konflik jadwal antara perkuliahan KRS mahasiswa dan perkuliahan paralel.
	 *
	 * <p><b>Tujuan.</b> Deteksi dan pelaporan konflik silang antara jadwal dalam KRS mahasiswa
	 * ({@code detailperkuliahans}) dan jadwal paralel terkait ({@code perkuliahans}).
	 * Bentrok jenis ini terjadi bila kelas reguler mahasiswa tumpang tindih dengan kelas paralel
	 * yang dijadwalkan bersamaan.</p>
	 *
	 * <p><b>Cara kerja.</b> Seperti {@link #generateInformasiJamBentrok} namun pasangan yang diperiksa
	 * adalah kombinasi silang antara dua list berbeda (bukan antar-elemen dalam satu list). Menggunakan
	 * logika overlap yang sama (5 kondisi ±0.01 + rentang tanggal ±60ms) serta flag pengecualian.</p>
	 *
	 * @param detailperkuliahans KRS mahasiswa (list sumber)
	 * @param perkuliahans       jadwal paralel (list target)
	 * @return {@link Html} berisi pesan konflik; atribut "adabentrok" = true bila ada konflik
	 */
	public static Html generateInformasiJamBentrokParalel(List<Detailperkuliahan> detailperkuliahans,
			List<Perkuliahan> perkuliahans) {

		boolean adabentrok = false;
		String bentrok = "<ol>";
		Set<String> sudahAda = new HashSet<String>();
		int jumlah = 0;
		for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {

			if (detailperkuliahan != null) {
				Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan() == null
						? detailperkuliahan.getIkutiPerkuliahan()
						: detailperkuliahan.getPerkuliahan();
				if (perkuliahan == null || perkuliahan.getAbaikanWaktuBentrokDenganJadwalLain()) {
					continue;
				}
				if (perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan()) {
					continue;
				}

				for (Perkuliahan jadwalParalel : perkuliahans) {

					if (jadwalParalel == null || perkuliahan.getId().equals(jadwalParalel.getId())
							|| jadwalParalel.getAbaikanWaktuBentrokDenganJadwalLain()) {
						continue;
					}

					if (jadwalParalel.getMerupakan_tanpa_jadwal_perkuliahan()) {
						continue;
					}

					String kode = perkuliahan.getId() + "_" + jadwalParalel.getId();
					String kode1 = jadwalParalel.getId() + "_" + perkuliahan.getId();

					Long start1 = perkuliahan.getPerkuliahanDimulai() == null ? null
							: perkuliahan.getPerkuliahanDimulai().getTime() + 60L;
					Long end1 = perkuliahan.getPerkuliahanSampai() == null ? null
							: perkuliahan.getPerkuliahanSampai().getTime() - 60L;

					Long start2 = jadwalParalel.getPerkuliahanDimulai() == null ? null
							: jadwalParalel.getPerkuliahanDimulai().getTime() + 60;
					Long end2 = jadwalParalel.getPerkuliahanSampai() == null ? null
							: jadwalParalel.getPerkuliahanSampai().getTime() - 60L;

					boolean ada = true;
					if (start1 != null && end1 != null && start2 != null && end2 != null) {
						ada = (start1 >= start2 && start1 <= end2) || (end1 >= start2 && end1 <= end2);
					}

					if (ada && (!sudahAda.contains(kode) && !sudahAda.contains(kode1))
							&& ((perkuliahan.getHari() == null ? "" : perkuliahan.getHari())
									.equals(jadwalParalel.getHari()))
							&& (

							(perkuliahan.getWaktuMulaiD() >= jadwalParalel.getWaktuMulaiD() + 0.01
									&& perkuliahan.getWaktuMulaiD() <= jadwalParalel.getWaktuSelesaiD() - 0.01)

									||

									(perkuliahan.getWaktuSelesaiD() >= jadwalParalel.getWaktuMulaiD() + 0.01
											&& perkuliahan.getWaktuSelesaiD() <= jadwalParalel.getWaktuSelesaiD()
													- 0.01)

									||

									(jadwalParalel.getWaktuMulaiD() >= perkuliahan.getWaktuMulaiD() + 0.01
											&& jadwalParalel.getWaktuMulaiD() <= perkuliahan.getWaktuSelesaiD() - 0.01)

									||

									(jadwalParalel.getWaktuSelesaiD() >= perkuliahan.getWaktuMulaiD() + 0.01
											&& jadwalParalel.getWaktuSelesaiD() <= perkuliahan.getWaktuSelesaiD()
													- 0.01)

									||

									(Common.numberFormat.get().format(jadwalParalel.getWaktuMulaiD())
											.equals(Common.numberFormat.get().format(perkuliahan.getWaktuMulaiD()))
											&& Common.numberFormat.get().format(jadwalParalel.getWaktuSelesaiD())
													.equals(Common.numberFormat.get()
															.format(perkuliahan.getWaktuSelesaiD())))

							)) {
						adabentrok = true;
						sudahAda.add(kode);
						sudahAda.add(kode1);
						bentrok += "<li>Jadwal perkuliahan <font style='font-weight:bold;color:red;'>"
								+ perkuliahan.getMatakuliah().getNama() + " waktu "
								+ (perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()) + " s.d "
								+ (perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai()) + " "
								+ perkuliahan.getHari()
								+ "</font> bentrok dengan jadwal perkuliahan <font style='font-weight:bold;color:red;'>"
								+ jadwalParalel.getMatakuliah().getNama() + " waktu " + jadwalParalel.getWaktuMulai()
								+ " s.d " + jadwalParalel.getWaktuSelesai() + " " + jadwalParalel.getHari()
								+ "</font></li>";
						jumlah++;
					}
				}
			}
		}
		bentrok += "</ol>";
		Html html;
		if (bentrok.equalsIgnoreCase("<ol></ol>")) {
			html = (new ais.ui.util.MyHtml("<font style='font-size:9px;'>--</font>"));
		} else {
			html = (new ais.ui.util.MyHtml(
					"<font style='font-size:9px;'>Terdapat <font style='font-weight:bold;color:red;'>" + jumlah
							+ "</font> jadwal perkuliahan yang bentrok, yaitu:<br>" + bentrok + "</font>"));
		}
		html.setAttribute("adabentrok", adabentrok);
		return html;
	}

	/**
	 * Memeriksa konflik penggunaan ruang antara jadwal paralel dan jadwal reguler dalam tahun ajaran/semester.
	 *
	 * <p><b>Tujuan.</b> Saat jadwal perkuliahan paralel baru diinput atau diubah, memastikan ruang yang
	 * sama tidak sudah dipakai oleh jadwal reguler lain pada hari dan jam yang tumpang tindih.</p>
	 *
	 * <p><b>Cara kerja.</b> Query Hibernate untuk perkuliahan aktif dengan ruang, tahunAjaran, dan
	 * jenis semester yang sama, lalu bandingkan jam dengan kondisi overlap (mulai/selesai saling tumpang).
	 * Mengecualikan perkuliahan dengan ID {@code idperkuliahan}.</p>
	 *
	 * <p><b>Catatan nama.</b> Nama method diawali huruf kapital (konvensi lama) — tidak diubah untuk
	 * menghindari regresi pada pemanggil.</p>
	 *
	 * @param tahunAjaran       tahun ajaran yang diperiksa
	 * @param ganjilGenap       jenis semester (GANJIL/GENAP)
	 * @param ruang             ruang yang akan diperiksa
	 * @param hari              hari yang akan diperiksa
	 * @param getWaktuMulaiD    jam mulai slot baru (desimal)
	 * @param getWaktuSelesaiD  jam selesai slot baru (desimal)
	 * @param idperkuliahan     ID perkuliahan yang dikecualikan
	 * @return {@code true} bila ada konflik ruang, {@code false} bila aman
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	public static Boolean CekBentrokParalelRuang(String tahunAjaran, String ganjilGenap, Ruang ruang, String hari,
			Double getWaktuMulaiD, Double getWaktuSelesaiD, Long idperkuliahan) {
		String bentrok = "<ol>";
		int jumlah = 0;
		// System.out.println("Cek bentrok Paralel Ruang");

		List<Perkuliahan> perkuliahans = HibernateUtil.currentSession().createCriteria(Perkuliahan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.ilike("tahunAjaran", tahunAjaran, MatchMode.ANYWHERE))
				.add(ganjilGenap.equalsIgnoreCase(Perkuliahan.GENAP) ? Restrictions.in("semester", Common.genap)
						: Restrictions.in("semester", Common.ganjil))
				.add(Restrictions.eq("ruang", ruang)).add(Restrictions.ne("id", idperkuliahan))

				.list();
		// System.out.println("jumlah : " + perkuliahans.size());
		// System.out.println("tahun ajaran : " + tahunAjaran);
		// System.out.println("ganjilgenap : " + ganjilGenap.trim());
		// System.out.println("ruang : " + ruang.getNama());
		// System.out.println("idPerkuliahan : " + idperkuliahan);

		for (Perkuliahan p : perkuliahans) {

			if (idperkuliahan == p.getId() || p.getAbaikanWaktuBentrokDenganJadwalLain()) {
				continue;
			}
			// System.out.println(hari.equals(p.getHari()) + "/" + hari + "/" +
			// p.getHari());

			if ((hari.equals(p.getHari()))
					&& (getWaktuMulaiD >= p.getWaktuMulaiD() && getWaktuMulaiD <= p.getWaktuSelesaiD())
					|| (getWaktuSelesaiD >= p.getWaktuMulaiD() && getWaktuSelesaiD <= p.getWaktuSelesaiD())) {
				// System.out.println("Cek bentrok Paralel Ruang id perkuliahan
				// :" + p.getId());

				jumlah++;
			}

		}
		bentrok += "</ol>";
		Html html;
		if (bentrok.equalsIgnoreCase("<ol></ol>")) {
			html = (new ais.ui.util.MyHtml("<font style='font-size:9px;'>--</font>"));
		} else {
			html = (new ais.ui.util.MyHtml(
					"<font style='font-size:9px;'>Terdapat <font style='font-weight:bold;color:red;'>" + jumlah
							+ "</font> jadwal perkuliahan yang bentrok, yaitu:<br>" + bentrok + "</font>"));
		}
		// System.out.println("Jumlah Bentrok ruangan : " + jumlah);
		return jumlah > 0;
	}

	/**
	 * Memeriksa konflik ruang antara dua jadwal paralel (paralel vs paralel) dalam tahun ajaran/semester.
	 *
	 * <p><b>Tujuan.</b> Saat jadwal paralel baru disimpan, memastikan ruang tidak sudah dipakai oleh
	 * jadwal paralel lain (bukan jadwal reguler) pada hari dan jam yang sama. Berbeda dari
	 * {@link #CekBentrokParalelRuang} yang mengecek terhadap jadwal reguler—method ini
	 * mengecek terhadap sesama jadwal paralel.</p>
	 *
	 * <p><b>Cara kerja.</b> Query perkuliahan yang merupakan paralel ({@code createAlias("perkuliahan",...)})
	 * dengan ruang, tahunAjaran, dan semester yang sama, lalu periksa overlap jam. Mengecualikan
	 * {@code idperkuliahan} induk paralel.</p>
	 *
	 * @param tahunAjaran       tahun ajaran
	 * @param ganjilGenap       jenis semester
	 * @param ruang             ruang yang diperiksa
	 * @param hari              hari
	 * @param getWaktuMulaiD    jam mulai (desimal)
	 * @param getWaktuSelesaiD  jam selesai (desimal)
	 * @param idperkuliahan     ID perkuliahan induk paralel yang dikecualikan
	 * @return {@code true} bila ada konflik antar paralel
	 */
	// digunakan untuk mengecek ketika save jadwal paralel thd jadwal paralel
	// lainnya (ruang)
	@SuppressWarnings("unchecked")
	public static Boolean CekBentrokParalelParalelRuang(String tahunAjaran, String ganjilGenap, Ruang ruang,
			String hari, Double getWaktuMulaiD, Double getWaktuSelesaiD, Long idperkuliahan) {
		// String bentrok = "<ol>";
		int jumlah = 0;
		// System.out.println("Cek bentrok Paralel-Paralel Ruang");

		List<Perkuliahan> perkuliahans = HibernateUtil.currentSession().createCriteria(Perkuliahan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("ruang", ruang))

				.createAlias("perkuliahan", "perkuliahan").add(Restrictions.ne("perkuliahan.id", idperkuliahan))

				.add(Restrictions.ilike("perkuliahan.tahunAjaran", tahunAjaran, MatchMode.ANYWHERE))
				.add(ganjilGenap.equalsIgnoreCase(Perkuliahan.GENAP)
						? Restrictions.in("perkuliahan.semester", Common.genap)
						: Restrictions.in("perkuliahan.semester", Common.ganjil))

				.list();
		// System.out.println("jumlah : " + perkuliahans.size());
		// System.out.println("tahun ajaran : " + tahunAjaran);
		// System.out.println("ganjilgenap : " + ganjilGenap.trim());
		// System.out.println("ruang : " + ruang.getNama());

		for (Perkuliahan p : perkuliahans) {

			if (idperkuliahan == p.getId()) {
				continue;
			}

			if ((hari.equals(p.getHari()))
					&& (getWaktuMulaiD >= p.getWaktuMulaiD() && getWaktuMulaiD <= p.getWaktuSelesaiD())
					|| (getWaktuSelesaiD >= p.getWaktuMulaiD() && getWaktuSelesaiD <= p.getWaktuSelesaiD())) {
				// System.out.println("Cek bentrok Paralel-Paralel Ruang id
				// paralel bentrok :" + p.getId());

				jumlah++;
			}

		}
		// bentrok += "</ol>";
		// Html html;
		// if (bentrok.equalsIgnoreCase("<ol></ol>")) {
		// html = (new ais.ui.util.MyHtml(
		// "<font style='font-size:9px;'>--</font>"));
		// } else {
		// html = (new ais.ui.util.MyHtml(
		// "<font style='font-size:9px;'>Terdapat
		// <font style='font-weight:bold;color:red;'>"
		// + jumlah
		// + "</font> jadwal perkuliahan yang bentrok, yaitu:<br>"
		// + bentrok + "</font>"));
		// }
		// // System.out.println("Jumlah Bentrok ruangan : " + jumlah);
		return jumlah > 0;
	}

	/**
	 * Memeriksa konflik dosen antara dua jadwal paralel dalam tahun ajaran/semester yang sama.
	 *
	 * <p><b>Tujuan.</b> Memastikan satu dosen tidak dijadwalkan mengajar di dua kelas paralel berbeda
	 * pada hari dan jam yang sama. Melengkapi {@link #CekBentrokParalelParalelRuang} untuk dimensi dosen.</p>
	 *
	 * <p><b>Cara kerja.</b> Query perkuliahan paralel ({@code createAlias("perkuliahan")}) dengan
	 * {@code dosen1=dosen}, tahunAjaran, semester, dan hari yang sama, kecuali {@code idperkuliahan}.
	 * Periksa overlap jam.</p>
	 *
	 * @param tahunAjaran       tahun ajaran
	 * @param ganjilGenap       jenis semester
	 * @param dosen             dosen yang diperiksa
	 * @param hari              hari
	 * @param getWaktuMulaiD    jam mulai (desimal)
	 * @param getWaktuSelesaiD  jam selesai (desimal)
	 * @param idperkuliahan     ID perkuliahan induk yang dikecualikan
	 * @return {@code true} bila ada konflik jadwal dosen antar paralel
	 */
	// digunakan untuk mengecek ketika save jadwal paralel thd jadwal paralel
	// lainnya (ruang)
	@SuppressWarnings("unchecked")
	public static Boolean cekBentrokParalelParalelDosen(String tahunAjaran, String ganjilGenap, Dosen dosen,
			String hari, Double getWaktuMulaiD, Double getWaktuSelesaiD, Long idperkuliahan) {
		// String bentrok = "<ol>";
		int jumlah = 0;
		// System.out.println("Cek bentrok Paralel-Paralel Ruang");

		List<Perkuliahan> perkuliahans = HibernateUtil.currentSession().createCriteria(Perkuliahan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.createAlias("perkuliahan", "perkuliahan").add(Restrictions.eq("perkuliahan.dosen1", dosen))
				.add(Restrictions.ne("perkuliahan.id", idperkuliahan))

				.add(Restrictions.ilike("perkuliahan.tahunAjaran", tahunAjaran, MatchMode.ANYWHERE))
				.add(ganjilGenap.equalsIgnoreCase(Perkuliahan.GENAP)
						? Restrictions.in("perkuliahan.semester", Common.genap)
						: Restrictions.in("perkuliahan.semester", Common.ganjil))

				.list();
		// System.out.println("jumlah : " + perkuliahans.size());
		// System.out.println("tahun ajaran : " + tahunAjaran);
		// System.out.println("ganjilgenap : " + ganjilGenap.trim());
		// System.out.println("ruang : " + dosen.getNama());

		for (Perkuliahan p : perkuliahans) {

			if (idperkuliahan == p.getId()) {
				continue;
			}

			if ((hari.equals(p.getHari()))
					&& (getWaktuMulaiD >= p.getWaktuMulaiD() && getWaktuMulaiD <= p.getWaktuSelesaiD())
					|| (getWaktuSelesaiD >= p.getWaktuMulaiD() && getWaktuSelesaiD <= p.getWaktuSelesaiD())) {
				// System.out.println("Cek bentrok Paralel-Paralel Ruang id
				// paralel bentrok :" + p.getId());

				jumlah++;
			}

		}
		// bentrok += "</ol>";
		// Html html;
		// if (bentrok.equalsIgnoreCase("<ol></ol>")) {
		// html = (new ais.ui.util.MyHtml(
		// "<font style='font-size:9px;'>--</font>"));
		// } else {
		// html = (new ais.ui.util.MyHtml(
		// "<font style='font-size:9px;'>Terdapat
		// <font style='font-weight:bold;color:red;'>"
		// + jumlah
		// + "</font> jadwal perkuliahan yang bentrok, yaitu:<br>"
		// + bentrok + "</font>"));
		// }
		// // System.out.println("Jumlah Bentrok dosen : " + jumlah);
		return jumlah > 0;
	}

	/**
	 * Memeriksa konflik jadwal dosen antara jadwal paralel dan jadwal reguler.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti {@link #cekBentrokParalelParalelDosen} namun
	 * mengecek terhadap jadwal reguler (bukan join via paralel — langsung query {@code Perkuliahan}
	 * dengan {@code dosen1=dosen}). Berguna saat menyimpan jadwal paralel untuk memastikan
	 * dosen tidak sudah mengajar reguler di slot yang sama.</p>
	 *
	 * @param tahunAjaran       tahun ajaran
	 * @param ganjilGenap       jenis semester
	 * @param dosen             dosen yang diperiksa
	 * @param hari              hari
	 * @param getWaktuMulaiD    jam mulai (desimal)
	 * @param getWaktuSelesaiD  jam selesai (desimal)
	 * @param idperkuliahan     ID perkuliahan yang dikecualikan
	 * @return {@code true} bila ada konflik dosen jadwal paralel vs reguler
	 */
	@SuppressWarnings("unchecked")
	public static Boolean CekBentrokParalelDosen(String tahunAjaran, String ganjilGenap, Dosen dosen, String hari,
			Double getWaktuMulaiD, Double getWaktuSelesaiD, Long idperkuliahan) {
		// String bentrok = "<ol>";
		int jumlah = 0;
		// System.out.println("Cek bentrok Paralel Dosen");

		List<Perkuliahan> perkuliahans = HibernateUtil.currentSession().createCriteria(Perkuliahan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.ilike("tahunAjaran", tahunAjaran, MatchMode.ANYWHERE))
				.add(ganjilGenap.equalsIgnoreCase(Perkuliahan.GENAP) ? Restrictions.in("semester", Common.genap)
						: Restrictions.in("semester", Common.ganjil))
				.add(Restrictions.eq("dosen1", dosen)).add(Restrictions.ne("id", idperkuliahan))

				.list();
		// System.out.println("jumlah : " + perkuliahans.size());
		// System.out.println("tahun ajaran : " + tahunAjaran);
		// System.out.println("ganjilgenap : " + ganjilGenap.trim());
		// System.out.println("ruang : " + dosen.getNama());

		for (Perkuliahan p : perkuliahans) {

			if (idperkuliahan == p.getId()) {
				continue;
			}
			// System.out.println(hari.equals(p.getHari()) + "/" + hari + "/" +
			// p.getHari());

			if ((hari.equals(p.getHari()))
					&& (getWaktuMulaiD >= p.getWaktuMulaiD() && getWaktuMulaiD <= p.getWaktuSelesaiD())
					|| (getWaktuSelesaiD >= p.getWaktuMulaiD() && getWaktuSelesaiD <= p.getWaktuSelesaiD())) {
				// System.out.println("Cek bentrok Paralel dosen id perkuliahan
				// :" + idperkuliahan);

				jumlah++;
			}

		}
		// bentrok += "</ol>";
		// Html html;
		// if (bentrok.equalsIgnoreCase("<ol></ol>")) {
		// html = (new ais.ui.util.MyHtml(
		// "<font style='font-size:9px;'>--</font>"));
		// } else {
		// html = (new ais.ui.util.MyHtml(
		// "<font style='font-size:9px;'>Terdapat
		// <font style='font-weight:bold;color:red;'>"
		// + jumlah
		// + "</font> jadwal perkuliahan yang bentrok, yaitu:<br>"
		// + bentrok + "</font>"));
		// }
		// // System.out.println("Jumlah Bentrok dosen : " + jumlah);
		return jumlah > 0;
	}

	/**
	 * Menghasilkan Html berisi konflik jadwal antar sesama jadwal paralel dalam satu list.
	 *
	 * <p><b>Tujuan.</b> Melengkapi {@link #generateInformasiJamBentrok} dan
	 * {@link #generateInformasiJamBentrokParalel}: mendeteksi konflik internal di antara
	 * jadwal-jadwal paralel (bukan antara reguler dan paralel).</p>
	 *
	 * <p><b>Cara kerja.</b> Seperti {@link #generateInformasiJamBentrok} namun menerima list
	 * {@link Perkuliahan} (bukan {@link Detailperkuliahan}). Memeriksa semua pasangan unik dalam
	 * list dengan logika overlap yang sama (5 kondisi ±0.01 + rentang tanggal ±60ms), dengan
	 * deduplikasi via {@code Set<String>}.</p>
	 *
	 * @param perkuliahan daftar jadwal paralel yang akan diperiksa
	 * @return {@link Html} berisi daftar konflik; atribut "adabentrok" = true bila ada konflik
	 */
	public static Html generateInformasiJamBentrokParalelParalel(List<Perkuliahan> perkuliahan) {

		String bentrok = "<ol>";
		Set<String> sudahAda = new HashSet<String>();
		int jumlah = 0;
		boolean adabentrok = false;
		for (Perkuliahan perkuliahanParalel : perkuliahan) {

			if (perkuliahanParalel == null || perkuliahanParalel.getAbaikanWaktuBentrokDenganJadwalLain()) {
				continue;
			}
			if (perkuliahanParalel.getMerupakan_tanpa_jadwal_perkuliahan()) {
				continue;
			}

			for (Perkuliahan perkuliahanParalel1 : perkuliahan) {
				if (perkuliahanParalel1 == null || perkuliahanParalel1.getId().equals(perkuliahanParalel.getId())
						|| perkuliahanParalel1.getAbaikanWaktuBentrokDenganJadwalLain()) {
					continue;
				}

				if (perkuliahanParalel1.getMerupakan_tanpa_jadwal_perkuliahan()) {
					continue;
				}

				String kode = perkuliahanParalel.getId() + "_" + perkuliahanParalel1.getId();
				String kode1 = perkuliahanParalel1.getId() + "_" + perkuliahanParalel.getId();

				Long start1 = perkuliahanParalel.getPerkuliahanDimulai() == null ? null
						: perkuliahanParalel.getPerkuliahanDimulai().getTime() + 60L;
				Long end1 = perkuliahanParalel.getPerkuliahanSampai() == null ? null
						: perkuliahanParalel.getPerkuliahanSampai().getTime() - 60L;

				Long start2 = perkuliahanParalel1.getPerkuliahanDimulai() == null ? null
						: perkuliahanParalel1.getPerkuliahanDimulai().getTime() + 60L;
				Long end2 = perkuliahanParalel1.getPerkuliahanSampai() == null ? null
						: perkuliahanParalel1.getPerkuliahanSampai().getTime() - 60L;

				boolean ada = true;
				if (start1 != null && end1 != null && start2 != null && end2 != null) {
					ada = (start1 >= start2 && start1 <= end2) || (end1 >= start2 && end1 <= end2);
				}

				if (ada && (!sudahAda.contains(kode) && !sudahAda.contains(kode1))
						&& ((perkuliahanParalel.getHari() == null ? "" : perkuliahanParalel.getHari())
								.equals(perkuliahanParalel1.getHari()))
						&& ((perkuliahanParalel.getWaktuMulaiD() >= perkuliahanParalel1.getWaktuMulaiD() + 0.01
								&& perkuliahanParalel.getWaktuMulaiD() <= perkuliahanParalel1.getWaktuSelesaiD() - 0.01)
								|| (perkuliahanParalel.getWaktuSelesaiD() - 0.01 >= perkuliahanParalel1.getWaktuMulaiD()
										&& perkuliahanParalel.getWaktuSelesaiD() + 0.01 <= perkuliahanParalel1
												.getWaktuSelesaiD()))) {
					adabentrok = true;
					sudahAda.add(kode);
					sudahAda.add(kode1);
					bentrok += "<li>Jadwal perkuliahan <font style='font-weight:bold;color:red;'>"
							+ perkuliahanParalel.getMatakuliah().getNama() + " waktu "
							+ (perkuliahanParalel.getWaktuMulai() == null ? "" : perkuliahanParalel.getWaktuMulai())
							+ " s.d "
							+ (perkuliahanParalel.getWaktuSelesai() == null ? "" : perkuliahanParalel.getWaktuSelesai())
							+ " " + perkuliahanParalel.getHari()
							+ "</font> bentrok dengan jadwal perkuliahan <font style='font-weight:bold;color:red;'>"
							+ perkuliahanParalel1.getMatakuliah().getNama() + " waktu "
							+ perkuliahanParalel1.getWaktuMulai() + " s.d " + perkuliahanParalel1.getWaktuSelesai()
							+ " " + perkuliahanParalel1.getHari() + "</font></li>";
					jumlah++;
				}
			}
		}
		bentrok += "</ol>";
		Html html;
		if (bentrok.equalsIgnoreCase("<ol></ol>")) {
			html = (new ais.ui.util.MyHtml("<font style='font-size:9px;'>--</font>"));
		} else {
			html = (new ais.ui.util.MyHtml(
					"<font style='font-size:9px;'>Terdapat <font style='font-weight:bold;color:red;'>" + jumlah
							+ "</font> jadwal perkuliahan yang bentrok, yaitu:<br>" + bentrok + "</font>"));
		}
		html.setAttribute("adabentrok", adabentrok);
		return html;
	}

	/**
	 * Memeriksa seluruh konflik jadwal pada KRS mahasiswa (reguler, paralel, dan paralel-paralel) sekaligus.
	 *
	 * <p><b>Tujuan.</b> Entry point tunggal untuk validasi bentrok lengkap saat mahasiswa mengambil KRS:
	 * menangani semua tiga jenis konflik secara berurutan dan menampilkan popup modal untuk yang pertama
	 * ditemukan.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Cek bentrok antar perkuliahan reguler via {@link #generateInformasiJamBentrok}. Bila ada:
	 *       tampilkan popup, return {@code false}.</li>
	 *   <li>Kumpulkan semua jadwal paralel dari KRS (query per {@link Detailperkuliahan}).</li>
	 *   <li>Cek bentrok reguler vs paralel via {@link #generateInformasiJamBentrokParalel}. Bila ada:
	 *       popup, return {@code false}.</li>
	 *   <li>Cek bentrok antar paralel via {@link #generateInformasiJamBentrokParalelParalel}. Bila ada:
	 *       popup, return {@code false}.</li>
	 *   <li>Bila tidak ada konflik: return {@code true} (KRS aman untuk disimpan).</li>
	 * </ol>
	 * Popup MyWindow berukuran 300×600px dengan judul "Informasi Jam Bentrok".</p>
	 *
	 * @param detailperkuliahans daftar {@link Detailperkuliahan} KRS mahasiswa
	 * @return {@code true} bila tidak ada bentrok sama sekali, {@code false} bila ada konflik
	 * @throws Exception bila terjadi error saat query jadwal paralel
	 */
	public static boolean checkJamBentrok(List<Detailperkuliahan> detailperkuliahans) throws Exception {
		Html html = Common.generateInformasiJamBentrok(detailperkuliahans);
		if (html.getAttribute("adabentrok") != null && html.getAttribute("adabentrok").equals(true)) {
			MyWindow window = new MyWindow("Informasi Jam Bentrok", "none", true);
			window.setHeight("300px");
			window.setWidth("600px");
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			window.appendChild(html);
			window.onModal();
			return false;
		}

		List<Perkuliahan> jadwalPerkuliahanParalels = new ArrayList<Perkuliahan>();
		for (Detailperkuliahan d : detailperkuliahans) {

			if (d != null) {
				@SuppressWarnings("unchecked")
				List<Perkuliahan> jadwalparalels = HibernateUtil.currentSession().createCriteria(Perkuliahan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("perkuliahan_paralel", d.getPerkuliahan())).list();
				jadwalPerkuliahanParalels.addAll(jadwalparalels);
			}
		}

		html = Common.generateInformasiJamBentrokParalel(detailperkuliahans, jadwalPerkuliahanParalels);
		if (html.getAttribute("adabentrok") != null && html.getAttribute("adabentrok").equals(true)) {
			MyWindow window = new MyWindow("Informasi Jam Bentrok", "none", true);
			window.setHeight("300px");
			window.setWidth("600px");
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			window.appendChild(html);
			window.onModal();
			return false;
		}
		html = Common.generateInformasiJamBentrokParalelParalel(jadwalPerkuliahanParalels);
		if (html.getAttribute("adabentrok") != null && html.getAttribute("adabentrok").equals(true)) {
			MyWindow window = new MyWindow("Informasi Jam Bentrok", "none", true);
			window.setHeight("300px");
			window.setWidth("600px");
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			window.appendChild(html);
			window.onModal();
			return false;
		}

		return true;
	}

	/**
	 * Membuat tombol "Bersihkan" (clear/reset) untuk komponen {@link Bandbox} dengan {@link GetEventListener}.
	 *
	 * <p><b>Tujuan.</b> Bandbox (dropdown pencarian ZK) sering memerlukan tombol X untuk mereset
	 * pilihan. Method ini membuat tombol tersebut dengan event listener bertipe {@link GetEventListener}
	 * (lazy-loading). Mendelegasikan ke {@code CommonUiFactoryHelper.createCleanButton}.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Terdapat dua overload: satu dengan {@link GetEventListener} (untuk
	 * bandbox yang memuat data secara lazy), satu dengan {@link EventListener} standar. Pilih sesuai
	 * tipe listener yang digunakan di komponen terkait.</p>
	 *
	 * @param bandbox       komponen bandbox yang akan diberi tombol bersihkan
	 * @param eventListener listener yang dipanggil setelah bandbox dibersihkan
	 * @return konfigurasi toolbar button siap ditambahkan ke toolbar
	 */
	public static MyToolbarbuttonConfig createCleanButton(final Bandbox bandbox, final GetEventListener eventListener) {
		return CommonUiFactoryHelper.createCleanButton(bandbox, eventListener);
	}

	/**
	 * Membuat tombol "Bersihkan" untuk komponen {@link Bandbox} dengan {@link EventListener} standar ZK.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti
	 * {@link #createCleanButton(Bandbox, GetEventListener)} namun menerima {@link EventListener}
	 * standar ZK. Berguna bila reset bandbox tidak memerlukan lazy-loading data. Mendelegasikan ke
	 * {@code CommonUiFactoryHelper.createCleanButton}.</p>
	 *
	 * @param bandbox       komponen bandbox
	 * @param eventListener listener standar ZK
	 * @return konfigurasi toolbar button
	 */
	public static MyToolbarbuttonConfig createCleanButton(final Bandbox bandbox, final EventListener eventListener) {
		return CommonUiFactoryHelper.createCleanButton(bandbox, eventListener);
	}

	/**
	 * Mengambil {@link Staff} dengan jabatan Ketua Program Studi (Kaprodi) untuk jurusan tertentu.
	 *
	 * <p><b>Tujuan.</b> Menentukan siapa Kaprodi dari sebuah jurusan — dipakai untuk otorisasi,
	 * penandatanganan dokumen (mis. transkrip, KRS), atau tampilan profil akademik.</p>
	 *
	 * <p><b>Cara kerja.</b> Query {@link Staff} dengan {@code staff=Staff.KAPRODI} dan
	 * {@code jurusan=jurusan}, diurutkan desc by ID, ambil satu ({@code setMaxResults(1)}).
	 * Bila ada beberapa entri, yang terbaru (ID terbesar) yang diambil.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Bila institusi menggunakan jabatan berbeda (Kajur, Koordinator, dll),
	 * pastikan konstanta {@code Staff.KAPRODI} sesuai data master.</p>
	 *
	 * @param jurusan jurusan yang dicari Kaprodi-nya
	 * @return {@link Staff} Kaprodi, atau {@code null} bila tidak ada
	 */
	public static Staff getKaprodi(Jurusan jurusan) {
		Session session = HibernateUtil.currentSession();
		Staff staff = (Staff) session.createCriteria(Staff.class).add(Restrictions.eq("staff", Staff.KAPRODI))
				.setMaxResults(1).add(Restrictions.eq("jurusan", jurusan)).addOrder(Order.desc("id")).uniqueResult();
		return staff;
	}

	/**
	 * Membaca kunci API Google Maps dari file konfigurasi server ({@code /opt/google_map.key}).
	 *
	 * <p><b>Tujuan.</b> Menyediakan kunci API Google Maps untuk fitur peta di UI (mis. lokasi kampus,
	 * absensi berbasis GPS) tanpa menyimpannya di kode atau database—disimpan di file server.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ul>
	 *   <li>Bila file tidak ada: buat file baru berisi string kosong dan kembalikan {@code ""}.</li>
	 *   <li>Bila ada: baca char per char dan kembalikan isinya (di-{@code trim()}).</li>
	 * </ul>
	 * {@code @SuppressWarnings("resource")} karena FileReader/FileWriter tidak ditutup via
	 * try-with-resources (Java 1.7 kompatibel — perlu perbaikan bila ada leak).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Perbarui isi file {@code /opt/google_map.key} di server saat kunci
	 * API kedaluwarsa atau perlu diganti. Kunci yang kosong akan menonaktifkan fitur peta tanpa error.</p>
	 *
	 * @return kunci API Google Maps sebagai string, atau {@code ""} bila file kosong/tidak ada
	 */
	@SuppressWarnings("resource")
	public static String getGoogleMapKey() {

		String GOOGLE_KEY_MAP = "";
		File googleKey = new File("/opt/google_map.key");
		if (!googleKey.exists()) {
			try {
				googleKey.createNewFile();

				FileWriter fileWriter = new FileWriter(googleKey);
				fileWriter.write(GOOGLE_KEY_MAP);
				fileWriter.close();

			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}

		} else {
			try {
				FileReader fr = new FileReader(googleKey);
				// BufferedReader br = new BufferedReader(fr);
				int c;
				String s = "";
				while ((c = fr.read()) != -1) {
					// // System.out.println(c);
					s += ((char) c);
				}
				GOOGLE_KEY_MAP = s;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
		}

		// System.out.println("GOOGLE_KEY_MAP = " + GOOGLE_KEY_MAP);
		return GOOGLE_KEY_MAP.trim();
	}

	/**
	 * Mengambil entitas {@link Perpustakaan} yang terkait dengan sesi pengguna saat ini (tanpa refresh).
	 *
	 * <p><b>Tujuan.</b> Menyediakan konteks perpustakaan (multi-tenant) untuk fitur modul perpustakaan:
	 * memastikan data yang ditampilkan sesuai perpustakaan milik institusi pengguna yang login.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.getCurrentPerpustakaan()}.
	 * Biasanya mengambil dari cache sesi HTTP atau menghitung berdasarkan hierarki institusi pengguna.</p>
	 *
	 * @return {@link Perpustakaan} saat ini, atau {@code null} bila pengguna tidak memiliki perpustakaan
	 */
	public static Perpustakaan getCurrentPerpustakaan() {
		return CommonCurrentSessionHelper.getCurrentPerpustakaan();
	}

	/**
	 * Mengambil {@link Perpustakaan} terkait sesi pengguna dengan kontrol refresh cache.
	 *
	 * <p><b>Cara kerja.</b> Seperti {@link #getCurrentPerpustakaan()} namun bila {@code refresh=true},
	 * mengabaikan cache dan membaca ulang dari DB. Berguna setelah perubahan konfigurasi perpustakaan.</p>
	 *
	 * @param refresh bila {@code true}, paksa baca ulang dari DB
	 * @return {@link Perpustakaan} saat ini
	 */
	public static Perpustakaan getCurrentPerpustakaan(Boolean refresh) {
		return CommonCurrentSessionHelper.getCurrentPerpustakaan(refresh);
	}

	/**
	 * Mengambil entitas {@link Toko} yang terkait dengan sesi pengguna saat ini (tanpa refresh).
	 *
	 * <p><b>Tujuan.</b> Konteks toko untuk modul penjualan/kantin — memastikan transaksi dicatat
	 * ke toko yang tepat sesuai pengguna yang login.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.getCurrentToko()}.
	 * Data di-cache per sesi HTTP.</p>
	 *
	 * @return {@link Toko} saat ini, atau {@code null}
	 */
	public static Toko getCurrentToko() {
		return CommonCurrentSessionHelper.getCurrentToko();
	}

	/**
	 * Mengambil {@link Toko} terkait sesi pengguna dengan kontrol refresh cache.
	 *
	 * @param refresh bila {@code true}, paksa baca ulang dari DB
	 * @return {@link Toko} saat ini
	 */
	public static Toko getCurrentToko(Boolean refresh) {
		return CommonCurrentSessionHelper.getCurrentToko(refresh);
	}

	/**
	 * Mengambil entitas {@link Koperasi} yang terkait dengan sesi pengguna saat ini (tanpa refresh).
	 *
	 * <p><b>Tujuan.</b> Konteks koperasi untuk modul koperasi — memastikan data sesuai koperasi
	 * milik institusi pengguna yang login (multi-tenant).</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.getCurrentKoperasi()}.
	 * Data di-cache per sesi HTTP.</p>
	 *
	 * @return {@link Koperasi} saat ini, atau {@code null}
	 */
	public static Koperasi getCurrentKoperasi() {
		return CommonCurrentSessionHelper.getCurrentKoperasi();
	}

	/**
	 * Mengambil {@link Koperasi} terkait sesi pengguna dengan kontrol refresh cache.
	 *
	 * @param refresh bila {@code true}, paksa baca ulang dari DB
	 * @return {@link Koperasi} saat ini
	 */
	public static Koperasi getCurrentKoperasi(Boolean refresh) {
		return CommonCurrentSessionHelper.getCurrentKoperasi(refresh);
	}

	/**
	 * Mengambil daftar {@link Pejabat} yang terkait dengan pengguna saat ini (multi-jabatan).
	 *
	 * <p><b>Tujuan.</b> Pengguna bisa memiliki beberapa jabatan struktural ({@link Pejabat}) aktif —
	 * mis. Dekan sekaligus Kepala Unit. Method ini mengambil semua jabatan yang relevan untuk
	 * menentukan hak tanda tangan, otorisasi, dan tampilan profil pejabat.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ul>
	 *   <li>Bila sesi tidak ada atau user null: return {@code null}.</li>
	 *   <li>Bila cache "{@code CurrentPejabat}" di sesi belum ada atau {@code refresh=true}: query
	 *       DB dengan kondisi OR (pengguna berdasarkan role/username ATAU berdasarkan entitas
	 *       pegawai/dosen/guru).</li>
	 *   <li>Simpan hasil ke cache sesi. Bila null, simpan object Pejabat kosong untuk menghindari
	 *       query berulang.</li>
	 * </ul>
	 * Print debug ke {@code System.out} (bisa dinonaktifkan bila sudah stabil).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Query melibatkan ILIKE pada CSV role/username di kolom
	 * {@code jenisPengguna}/{@code usernamePengguna} — perlu perhatian khusus pada nilai yang
	 * mengandung koma. Bila ada jabatan yang tidak muncul, cek format nilai di tabel pejabat.</p>
	 *
	 * @param refresh bila {@code true}, paksa baca ulang dari DB (abaikan cache sesi)
	 * @return daftar {@link Pejabat} milik pengguna saat ini, atau {@code null}
	 */
	@SuppressWarnings("unchecked")
	public static List<Pejabat> getCurrentPejabat(Boolean refresh) {

		try {
			Tbmuser tbmuser = getCurrentUser();
			org.zkoss.zk.ui.Session httpSession = Sessions.getCurrent();
			if (httpSession == null || tbmuser == null) {
				return null;
			}

			List<Pejabat> pejabats = null;

			if (httpSession == null || httpSession.getAttribute("CurrentPejabat") == null || refresh) {

				pejabats = ConstantValues.simpleList(HibernateUtil.currentSession().createCriteria(Pejabat.class)

						.createAlias("jenisJabatan", "jenisJabatan").add(Restrictions.eq("jenisJabatan.aktif", true))

						.add(Restrictions.isNotNull("jenisJabatan"))
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

						.addOrder(Order.desc("id")), Pejabat.class);

				System.out.println(
						"======================= Init pejabat " + pejabats + "===============================");

				if (httpSession != null && pejabats != null) {
					httpSession.setAttribute("CurrentPejabat", pejabats);
				} else if (httpSession != null && pejabats == null) {
					httpSession.setAttribute("CurrentPejabat", new Pejabat());
				}
			} else {
				pejabats = (List<Pejabat>) httpSession.getAttribute("CurrentPejabat");
			}

			return pejabats;
		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
			return null;
		}
	}

	/**
	 * Mengambil satu {@link Pejabat} pengguna saat ini berdasarkan jenis jabatan tertentu.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Filter dari daftar jabatan pengguna: mengambil jabatan
	 * pertama yang cocok dengan {@code jenisJabatan}. Berguna untuk penandatanganan dokumen yang
	 * spesifik terhadap satu jenis jabatan (mis. hanya Dekan, hanya Rektor). Mendelegasikan ke
	 * {@code CommonCurrentSessionHelper.getCurrentPejabat(jenisJabatan)}.</p>
	 *
	 * @param jenisJabatan jenis jabatan yang dicari
	 * @return {@link Pejabat} pertama yang cocok, atau {@code null}
	 */
	public static Pejabat getCurrentPejabat(JenisJabatan jenisJabatan) {
		return CommonCurrentSessionHelper.getCurrentPejabat(jenisJabatan);
	}

	/**
	 * Mengambil semua {@link Pejabat} pengguna saat ini yang memiliki jenis jabatan tertentu.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti {@link #getCurrentPejabat(JenisJabatan)} namun
	 * mengembalikan semua yang cocok, bukan hanya satu. Berguna bila pengguna memiliki lebih dari
	 * satu jabatan dengan tipe yang sama (mis. menjabat di beberapa unit). Mendelegasikan ke
	 * {@code CommonCurrentSessionHelper.getCurrentPejabats(jenisJabatan)}.</p>
	 *
	 * @param jenisJabatan jenis jabatan yang dicari
	 * @return daftar {@link Pejabat} yang cocok (kosong bila tidak ada)
	 */
	public static List<Pejabat> getCurrentPejabats(JenisJabatan jenisJabatan) {
		return CommonCurrentSessionHelper.getCurrentPejabats(jenisJabatan);
	}

	// Parallel arrays used in the conversion process.
	private static final String[] RCODE = { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };
	private static final int[] BVAL = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };

	/**
	 * Mengonversi bilangan bulat positif menjadi representasi angka Romawi.
	 *
	 * <p><b>Tujuan.</b> Digunakan untuk penomoran bab, bagian, atau dokumen resmi yang menggunakan
	 * angka Romawi (mis. Bab I, II, III... atau nomor urut semester pertama I, kedua II, dll).</p>
	 *
	 * <p><b>Cara kerja.</b> Mengiterasi array paralel nilai ({@code BVAL}) dan simbol ({@code RCODE})
	 * dari terbesar ke terkecil; selama {@code binary >= BVAL[i]}, kurangi binary dan tambahkan
	 * simbol ke string hasil. Menangani nilai subtraktif (mis. 4 = IV, 9 = IX) via urutan array.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Jangkauan valid: 1 s.d. 3999. Nilai di luar range melempar
	 * {@link NumberFormatException}. Konstanta array {@code RCODE}/{@code BVAL} tidak perlu diubah
	 * kecuali ada kebutuhan angka Romawi khusus.</p>
	 *
	 * @param binary bilangan bulat positif yang akan dikonversi (1–3999)
	 * @return string angka Romawi
	 * @throws NumberFormatException bila nilai di luar jangkauan 1–3999
	 */
	// =========================================================== binaryToRoman
	public static String binaryToRoman(int binary) {
		if (binary <= 0 || binary >= 4000) {
			throw new NumberFormatException("Value outside roman numeral range.");
		}
		String roman = ""; // Roman notation will be accumualated here.

		// Loop from biggest value to smallest, successively subtracting,
		// from the binary value while adding to the roman representation.
		for (int i = 0; i < RCODE.length; i++) {
			while (binary >= BVAL[i]) {
				binary -= BVAL[i];
				roman += RCODE[i];
			}
		}
		return roman;
	}

	/**
	 * Menghapus data {@link Pegawai} yang merupakan dosen dari tabel pegawai (migrasi/sanitasi data).
	 *
	 * <p><b>Tujuan.</b> Utility satu kali untuk membersihkan entri ganda: pegawai yang memiliki
	 * relasi ke entitas {@link Dosen} (via {@code pegawai.dosen != null}) dihapus dari tabel Pegawai
	 * untuk menghindari duplikasi data antar dua entitas tersebut.</p>
	 *
	 * <p><b>Cara kerja.</b> Query semua Pegawai aktif yang memiliki dosen, lalu memanggil
	 * {@code PegawaiDao.delete(d)} untuk setiap entri. Tidak ada commit eksplisit — commit
	 * dilakukan oleh session induk.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Method ini dimaksudkan sebagai operasi migrasi, bukan operasi rutin.
	 * Panggil hanya bila ada masalah data duplikasi antara pegawai dan dosen. Pastikan ada backup DB
	 * sebelum dijalankan—operasi ini tidak dapat dibatalkan tanpa rollback.</p>
	 */
	@SuppressWarnings("unchecked")
	public static void prosesDosen() {
		Session session = HibernateUtil.currentSession();

		Integer count = ((Number) session.createCriteria(Pegawai.class)
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
				// .add(Restrictions.or(Restrictions.isNull("nama"),
				// Restrictions.eq("nama", "")))
				.add(Restrictions.isNotNull("dosen")).setProjection(Projections.rowCount()).uniqueResult()).intValue();

		if (!count.equals(0)) {
			List<Pegawai> dosen = session.createCriteria(Pegawai.class)
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					// .add(Restrictions.or(Restrictions.isNull("nama"),
					// Restrictions.eq("nama", "")))
					.add(Restrictions.isNotNull("dosen")).list();

			// // System.out.println("Engkrp semu " + tbmusers);
			for (Pegawai d : dosen) {
				PegawaiDao pegawaiDao = DaoFactory.getInstance().getPegawaiDao();

				// Pegawai pegawai = new Pegawai();
				// pegawai.setDosen(d);
				// d.setPegawai(true);
				// pegawaiDao.save(pegawai);
				pegawaiDao.delete(d);
				// Common.refreshUpdate(session, (dosen));
			}
		}

		// queryInit();
	}

	/**
	 * Membuat format penilaian default untuk sebuah perkuliahan bila belum ada.
	 *
	 * <p><b>Tujuan.</b> Menyiapkan komponen format nilai awal (mis. UTS/UAS dengan bobot default)
	 * agar dosen tidak perlu mengisi format dari nol — hanya perlu menyesuaikan bila perlu.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicSyncHelper.createDefaultFormatNilai(perkuliahan)}. Biasanya dipanggil
	 * saat perkuliahan baru dibuat atau diaktifkan.</p>
	 *
	 * @param perkuliahan perkuliahan yang akan diberi format nilai default
	 */
	public static void createDefaultFormatNilai(Perkuliahan perkuliahan) {
		CommonAcademicSyncHelper.createDefaultFormatNilai(perkuliahan);
	}

	/**
	 * Menyinkronkan status mahasiswa secara massal berdasarkan kriteria akademik dan pembayaran.
	 *
	 * <p><b>Tujuan.</b> Proses batch untuk memperbarui status akademik mahasiswa (aktif/cuti/DO/dll)
	 * berdasarkan KRS, pembayaran, dan kondisi perkuliahan—biasanya dijalankan di awal semester.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicSyncHelper.singkronisasiStatusMahasiswa(...)}. Flag boolean mengontrol
	 * komponen mana yang disinkronkan:
	 * <ul>
	 *   <li>{@code statusMhs}: perbarui status mahasiswa (aktif/dll).</li>
	 *   <li>{@code statusKrs}: perbarui status KRS mahasiswa.</li>
	 *   <li>{@code perkuliahanMhs}: periksa keikutsertaan perkuliahan.</li>
	 *   <li>{@code pembayaranMhs}: periksa status pembayaran.</li>
	 *   <li>{@code nonAktifkan}: bila {@code true}, non-aktifkan mahasiswa yang tidak memenuhi syarat.</li>
	 * </ul></p>
	 *
	 * <p><b>Pemeliharaan.</b> Operasi berat — jalankan di thread latar dengan progress label.
	 * {@code mulai}/{@code sampai} membatasi rentang semester yang diproses.</p>
	 *
	 * @param label           label ZK untuk menampilkan progres
	 * @param dataCriteria    kriteria tambahan untuk filter mahasiswa
	 * @param tahunAkademik   tahun akademik target
	 * @param jenisSemester   jenis semester (GANJIL/GENAP)
	 * @param mulai           semester mulai
	 * @param sampai          semester akhir
	 * @param statusMhs       sinkronkan status mahasiswa
	 * @param statusKrs       sinkronkan status KRS
	 * @param perkuliahanMhs  periksa perkuliahan
	 * @param pembayaranMhs   periksa pembayaran
	 * @param nonAktifkan     non-aktifkan mahasiswa tidak memenuhi syarat
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static ais.common.LaporanUpload singkronisasiStatusMahasiswa(Label label, DataCriteria dataCriteria, String tahunAkademik,
			String jenisSemester, Integer mulai, Integer sampai, boolean statusMhs, boolean statusKrs,
			boolean perkuliahanMhs, boolean pembayaranMhs, boolean nonAktifkan) {
		return CommonAcademicSyncHelper.singkronisasiStatusMahasiswa(label, dataCriteria, tahunAkademik, jenisSemester, mulai,
				sampai, statusMhs, statusKrs, perkuliahanMhs, pembayaranMhs, nonAktifkan);
	}

	/**
	 * Menyinkronkan (membuat/memperbarui) KRS semua mahasiswa aktif secara massal.
	 *
	 * <p><b>Tujuan.</b> Proses batch yang memastikan setiap mahasiswa aktif memiliki entitas
	 * {@link KrsMahasiswa} yang valid untuk semester aktif—berguna saat awal semester atau
	 * setelah reset data.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicSyncHelper.singkronisasiKRSMahasiswa(label)}, yang mengiterasi semua
	 * mahasiswa aktif dan memanggil {@code singkronkanKrsMahasiswa} untuk masing-masing.</p>
	 *
	 * @param label label ZK untuk menampilkan progres
	 */
	public static void singkronisasiKRSMahasiswa(Label label) {
		CommonAcademicSyncHelper.singkronisasiKRSMahasiswa(label);
	}

	/**
	 * Mengambil status saat ini mahasiswa pada semester pendek tertentu.
	 *
	 * <p><b>Tujuan.</b> Menentukan status akademik (aktif/cuti/dll) mahasiswa di semester pendek (SP)
	 * berdasarkan KRS yang tersinkronisasi.</p>
	 *
	 * <p><b>Cara kerja.</b> Membaca KRS tersimpan untuk semester aktif dan parameter SP
	 * tanpa menjalankan sinkronisasi, kemudian mengambil status via {@code HistoryStatusMahasiswaUtil}.</p>
	 *
	 * @param mahasiswa mahasiswa yang dicek statusnya
	 * @param sp        parameter semester pendek
	 * @return {@link HistoryStatusMahasiswa} status saat ini
	 */
	public static HistoryStatusMahasiswa currentStatusSp(Mahasiswa mahasiswa, Integer sp) {
		return ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatusSp(mahasiswa, sp);
	}

	/**
	 * Mengambil status akademik saat ini mahasiswa pada semester aktif.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicSyncHelper.currentStatus(mahasiswa)} — menentukan status berdasarkan
	 * KRS semester aktif mahasiswa. Dipakai untuk filter "mahasiswa aktif" di berbagai laporan.</p>
	 *
	 * @param mahasiswa mahasiswa yang dicek
	 * @return {@link HistoryStatusMahasiswa} status saat ini, atau {@code null}
	 */
	public static HistoryStatusMahasiswa currentStatus(Mahasiswa mahasiswa) {
		return CommonAcademicSyncHelper.currentStatus(mahasiswa);
	}

	/**
	 * Mengambil status akademik mahasiswa berdasarkan tahapan studi tertentu.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti {@link #currentStatus(Mahasiswa)} namun
	 * dengan {@code tahap} eksplisit untuk skenario multi-tahapan (mis. mahasiswa dengan
	 * kurikulum bertahap). Mendelegasikan ke
	 * {@code CommonAcademicSyncHelper.currentStatus(mahasiswa, tahap)}.</p>
	 *
	 * @param mahasiswa mahasiswa yang dicek
	 * @param tahap     nomor tahapan studi
	 * @return {@link HistoryStatusMahasiswa} pada tahapan tersebut
	 */
	public static HistoryStatusMahasiswa currentStatus(Mahasiswa mahasiswa, Integer tahap) {
		return CommonAcademicSyncHelper.currentStatus(mahasiswa, tahap);
	}

	/**
	 * Mengambil status mahasiswa di semester pendek pada tahun akademik dan semester tertentu.
	 *
	 * <p><b>Tujuan.</b> Varian {@link #currentStatusSp(Mahasiswa, Integer)} yang lebih eksplisit:
	 * memungkinkan cek status di semester pendek pada tahun akademik/semester tertentu, bukan
	 * selalu semester saat ini.</p>
	 *
	 * <p><b>Cara kerja.</b> Membaca KRS tersimpan dengan parameter semester+SP eksplisit
	 * tanpa menjalankan sinkronisasi, lalu mengambil status via {@code HistoryStatusMahasiswaUtil}.</p>
	 *
	 * @param mahasiswa     mahasiswa yang dicek
	 * @param tahunAkademik tahun akademik target (saat ini tidak dipakai langsung di implementasi)
	 * @param semester      semester target
	 * @param sp            parameter semester pendek
	 * @return {@link HistoryStatusMahasiswa} status mahasiswa di semester pendek tersebut
	 */
	public static HistoryStatusMahasiswa currentStatusSp(Mahasiswa mahasiswa, String tahunAkademik, Integer semester,
			Integer sp) {
		return ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatusSp(mahasiswa, tahunAkademik,
				semester, sp);
	}

	/**
	 * Mengambil status mahasiswa pada tahun akademik dan semester tertentu.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicSyncHelper.currentStatus(mahasiswa, tahunAkademik, semester)}.
	 * Berguna untuk melihat status historis mahasiswa (mis. laporan semester lalu).</p>
	 *
	 * @param mahasiswa     mahasiswa yang dicek
	 * @param tahunAkademik tahun akademik yang dimaksud
	 * @param semester      semester yang dimaksud
	 * @return {@link HistoryStatusMahasiswa} pada periode tersebut
	 */
	public static HistoryStatusMahasiswa currentStatus(Mahasiswa mahasiswa, String tahunAkademik, Integer semester) {
		return CommonAcademicSyncHelper.currentStatus(mahasiswa, tahunAkademik, semester);
	}

	/**
	 * Overload kompatibilitas: status terkini berdasarkan {@link KrsMahasiswa}.
	 *
	 * <p>Dipakai pemanggil lama (mis. {@code PembayaranUtil} versi lama). Mendelegasikan ke
	 * {@code HistoryStatusMahasiswaUtil.currentStatus(KrsMahasiswa)} — sumber kebenaran yang sama
	 * dengan {@link #getHistoryStatusMahasiswa(KrsMahasiswa)}. Disediakan agar pemanggil yang
	 * mengirim KrsMahasiswa tetap compile tanpa mengubah logikanya.</p>
	 */
	public static HistoryStatusMahasiswa currentStatus(KrsMahasiswa krsMahasiswa) {
		return HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa);
	}

	/**
	 * Menyisipkan semua properti entitas ke dalam {@code Map} parameter (tanpa depth/exclusion control).
	 *
	 * <p><b>Tujuan.</b> Mengonversi entitas Hibernate menjadi struktur {@code Map<String, Object>}
	 * untuk dipakai sebagai parameter laporan (JasperReports), JSON API, atau template dokumen.
	 * Semua properti entitas dimasukkan dengan kunci = nama properti Hibernate.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code ManajemenProperty.insertProperty(clazz,
	 * generalValueObject, parameters, nama)} — menggunakan refleksi Hibernate untuk mengakses
	 * nilai properti secara rekursif (kedalaman default).</p>
	 *
	 * @param clazz              kelas entitas
	 * @param generalValueObject objek entitas sumber
	 * @param parameters         map tujuan untuk menerima nilai properti
	 * @param nama               prefiks nama untuk kunci di map (mis. "mahasiswa.")
	 */
	@SuppressWarnings("rawtypes")
	public static void insertProperty(Class clazz, GeneralValueObject generalValueObject,
			Map<String, Object> parameters, String nama) {
		ManajemenProperty.insertProperty(clazz, generalValueObject, parameters, nama);
	}

	/**
	 * Menyisipkan properti entitas ke {@code Map} dengan kontrol kedalaman rekursi dan pengecualian.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti
	 * {@link #insertProperty(Class, GeneralValueObject, Map, String)} namun dengan:
	 * <ul>
	 *   <li>{@code deep}: kedalaman rekursi ke relasi (FK). Nilai 1 = hanya properti langsung;
	 *       nilai 2 = properti + relasi langsung.</li>
	 *   <li>{@code kecuali}: daftar nama properti yang dikecualikan dari ekstraksi.</li>
	 * </ul>
	 * Mendelegasikan ke {@code ManajemenProperty.insertProperty(..., deep, kecuali)}.</p>
	 *
	 * @param clazz              kelas entitas
	 * @param generalValueObject objek entitas sumber
	 * @param parameters         map tujuan
	 * @param nama               prefiks nama
	 * @param deep               kedalaman rekursi ke relasi FK
	 * @param kecuali            nama properti yang dikecualikan
	 */
	@SuppressWarnings({ "rawtypes" })
	public static void insertProperty(Class clazz, GeneralValueObject generalValueObject,
			Map<String, Object> parameters, String nama, int deep, String... kecuali) {
		ManajemenProperty.insertProperty(clazz, generalValueObject, parameters, nama, deep, kecuali);
	}

	/**
	 * Menyisipkan properti entitas ke {@link JSONObject} dengan kedalaman, batas, dan pengecualian.
	 *
	 * <p><b>Tujuan.</b> Varian serialisasi ke JSON: berguna untuk API response atau localStorage
	 * client-side yang menerima JSON. Parameter {@code max} membatasi jumlah properti per level
	 * untuk menghindari JSON terlalu besar.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code ManajemenProperty.insertProperty(clazz, generalValueObject, parameters, nama, deep, max, pengecualian)}.</p>
	 *
	 * @param clazz              kelas entitas
	 * @param generalValueObject objek entitas sumber
	 * @param parameters         JSONObject tujuan
	 * @param nama               prefiks nama
	 * @param deep               kedalaman rekursi
	 * @param max                jumlah maksimum properti per level
	 * @param pengecualian       nama properti yang dikecualikan
	 */
	@SuppressWarnings({ "rawtypes" })
	public static void insertProperty(Class clazz, GeneralValueObject generalValueObject, JSONObject parameters,
			String nama, int deep, int max, String... pengecualian) {
		ManajemenProperty.insertProperty(clazz, generalValueObject, parameters, nama, deep, max, pengecualian);
	}

	/**
	 * Menyisipkan properti entitas ke {@link JSONObject} dengan batas jumlah dan pengecualian (tanpa deep).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Versi ringkas dari
	 * {@link #insertProperty(Class, GeneralValueObject, JSONObject, String, int, int, String...)}:
	 * hanya menerima {@code max} tanpa parameter {@code deep} (menggunakan default kedalaman).
	 * Mendelegasikan ke {@code ManajemenProperty.insertProperty(..., max, pengecualian)}.</p>
	 *
	 * @param clazz              kelas entitas
	 * @param generalValueObject objek entitas sumber
	 * @param parameters         JSONObject tujuan
	 * @param nama               prefiks nama
	 * @param max                jumlah maksimum properti
	 * @param pengecualian       nama properti yang dikecualikan
	 */
	@SuppressWarnings("rawtypes")
	public static void insertProperty(Class clazz, GeneralValueObject generalValueObject, JSONObject parameters,
			String nama, int max, String... pengecualian) {
		ManajemenProperty.insertProperty(clazz, generalValueObject, parameters, nama, max, pengecualian);
	}

	/**
	 * Memperbarui (update) entitas ke sesi Hibernate saat ini menggunakan current session.
	 *
	 * <p><b>Tujuan.</b> Shortcut untuk update entitas tanpa harus memegang referensi session secara
	 * eksplisit—dipakai saat update sederhana dalam satu request.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonHibernateHelper.refreshUpdate(o)} yang menggunakan
	 * {@link HibernateUtil#currentSession()} secara internal. Jangan panggil close/commit setelahnya.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Jangan pakai untuk operasi write di thread latar — gunakan
	 * {@code openSession} + overload dengan session eksplisit.</p>
	 *
	 * @param o entitas yang akan diperbarui
	 */
	public static void refreshUpdate(GeneralValueObject o) {

		CommonHibernateHelper.refreshUpdate(o);

	}

	/**
	 * Memperbarui entitas ke sesi Hibernate yang ditentukan secara eksplisit.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti {@link #refreshUpdate(GeneralValueObject)} namun
	 * menggunakan session yang diberikan—berguna untuk operasi dalam transaksi eksplisit atau
	 * di thread latar yang menggunakan {@code openSession()}.</p>
	 *
	 * @param session sesi Hibernate yang aktif
	 * @param o       entitas yang akan diperbarui
	 */
	public static void refreshUpdate(Session session, GeneralValueObject o) {
		CommonHibernateHelper.refreshUpdate(session, o);
	}

	/**
	 * Memperbarui entitas ke sesi Hibernate dengan kontrol flush eksplisit.
	 *
	 * <p><b>Tujuan.</b> Varian dengan {@code flush=true} untuk memaksa sinkronisasi perubahan ke DB
	 * segera (tanpa menunggu akhir transaksi). Berguna bila ada operasi native SQL setelahnya yang
	 * harus melihat data terbaru.</p>
	 *
	 * @param session sesi Hibernate
	 * @param o       entitas yang akan diperbarui
	 * @param flush   bila {@code true}, langsung flush ke DB
	 */
	public static void refreshUpdate(Session session, GeneralValueObject o, boolean flush) {
		CommonHibernateHelper.refreshUpdate(session, o, flush);

	}

	/**
	 * Me-refresh entitas dari database (reload dari DB ke dalam objek yang sudah ada di memori).
	 *
	 * <p><b>Tujuan.</b> Memperbarui state objek entitas dengan data terbaru dari DB — berguna setelah
	 * operasi yang mungkin telah mengubah data via SQL native atau thread lain.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonHibernateHelper.refresh(o)}.
	 * Berbeda dari {@code refreshUpdate}: ini membaca DB → objek (bukan menulis objek → DB).</p>
	 *
	 * @param o entitas yang akan di-refresh dari DB
	 */
	public static void refresh(GeneralValueObject o) {

		CommonHibernateHelper.refresh(o);

	}

	/**
	 * Menyimpan atau memperbarui entitas ke sesi Hibernate saat ini (saveOrUpdate).
	 *
	 * <p><b>Tujuan.</b> Menyimpan entitas baru atau memperbarui yang sudah ada dalam satu panggilan.
	 * Hibernate menentukan apakah INSERT atau UPDATE berdasarkan state entitas (transient vs persistent).</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonHibernateHelper.refreshSaveOrUpdate(o)}
	 * yang menggunakan {@code currentSession()}.</p>
	 *
	 * @param o entitas yang akan disimpan/diperbarui
	 */
	public static void refreshSaveOrUpdate(GeneralValueObject o) {

		CommonHibernateHelper.refreshSaveOrUpdate(o);

	}

	/**
	 * Menyimpan atau memperbarui entitas ke sesi Hibernate yang ditentukan.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti {@link #refreshSaveOrUpdate(GeneralValueObject)}
	 * namun dengan session eksplisit — untuk operasi dalam transaksi terbuka atau di thread latar.</p>
	 *
	 * @param session sesi Hibernate aktif
	 * @param o       entitas yang akan disimpan/diperbarui
	 */
	public static void refreshSaveOrUpdate(Session session, GeneralValueObject o) {

		CommonHibernateHelper.refreshSaveOrUpdate(session, o);

	}

	/**
	 * Menyalin nilai properti dari satu entitas ke entitas lain, hanya untuk properti yang masih kosong/null.
	 *
	 * <p><b>Tujuan.</b> Mengisi data yang belum terisi di entitas tujuan dari entitas sumber — berguna
	 * untuk fitur "isi dari template" atau saat data mahasiswa/dosen diambil dari entitas induk yang
	 * berbeda kelas tapi propertinya kompatibel (mis. Pegawai → Dosen).</p>
	 *
	 * <p><b>Cara kerja.</b> Mengiterasi semua {@code classMetadataFrom.getPropertyNames()}, mengambil
	 * nilai dari {@code copyFrom} dan {@code copyTo}, kemudian mengisi ke {@code copyTo} hanya bila
	 * nilai saat ini null, string null, Double &lt; 0.01, atau Integer &lt; 1. Menggunakan EntityMode.POJO.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Jenis properti yang dianggap "kosong" terbatas pada null, String, Double,
	 * Integer — tipe lain (Boolean, Date, dll) yang null tidak akan disalin. Perbarui kondisi bila
	 * perlu menyalin tipe lain.</p>
	 *
	 * @param copyFrom   entitas sumber
	 * @param copyTo     entitas tujuan
	 * @param clazzFrom  kelas entitas sumber
	 * @param clazzTo    kelas entitas tujuan
	 */
	@SuppressWarnings("rawtypes")
	public static void copyDataJikaKosong(GeneralValueObject copyFrom, GeneralValueObject copyTo, Class clazzFrom,
			Class clazzTo) {
		ClassMetadata classMetadataFrom = HibernateUtil.getClassMetadata(clazzFrom);
		ClassMetadata classMetadataTo = HibernateUtil.getClassMetadata(clazzTo);
		String[] properties = classMetadataFrom.getPropertyNames();
		for (String p : properties) {
			Object dataLama = null;
			try {
				dataLama = classMetadataTo.getPropertyValue(copyTo, p, EntityMode.POJO);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:7840");

			}
			Object dataBaru = null;
			try {
				dataBaru = classMetadataFrom.getPropertyValue(copyFrom, p, EntityMode.POJO);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:7846");

			}
			try {
				if (dataLama == null || (dataLama instanceof String && Common.checkIsStringNull(dataLama))
						|| (dataLama instanceof Double && ((Double) dataLama) < 0.01)
						|| (dataLama instanceof Integer && ((Integer) dataLama) < 1)) {
					classMetadataTo.setPropertyValue(copyTo, p, dataBaru, EntityMode.POJO);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:7855");

			}
		}
	}

	/**
	 * Menghapus entitas dari sesi Hibernate saat ini dengan flush segera.
	 *
	 * <p><b>Tujuan.</b> Menghapus entitas dan langsung mem-flush perubahan ke DB (tanpa menunggu
	 * commit). Berguna bila ada operasi native SQL setelah delete yang bergantung pada state DB
	 * yang sudah bersih.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonHibernateHelper.refreshDeleteFlush(o)}.
	 * Flush tidak commit — transaksi masih harus di-commit secara eksplisit bila menggunakan
	 * {@code currentSession}.</p>
	 *
	 * @param o entitas yang akan dihapus
	 */
	public static void refreshDeleteFlush(GeneralValueObject o) {
		CommonHibernateHelper.refreshDeleteFlush(o);
	}

	/**
	 * Menghapus entitas dari sesi Hibernate saat ini (tanpa flush eksplisit).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonHibernateHelper.refreshDelete(o)}.
	 * Menggunakan {@code currentSession()} — jangan panggil close/commit secara manual setelahnya.</p>
	 *
	 * @param o entitas yang akan dihapus
	 */
	public static void refreshDelete(GeneralValueObject o) {
		CommonHibernateHelper.refreshDelete(o);
	}

	/**
	 * Menghapus entitas dari sesi Hibernate yang ditentukan secara eksplisit.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonHibernateHelper.refreshDelete(session, o)}.
	 * Untuk operasi hapus dalam transaksi eksplisit atau thread latar.</p>
	 *
	 * @param session sesi Hibernate aktif
	 * @param o       entitas yang akan dihapus
	 */
	public static void refreshDelete(Session session, GeneralValueObject o) {
		CommonHibernateHelper.refreshDelete(session, o);
	}

	/**
	 * Menampilkan halaman ZUL dalam popup {@link MyWindow} modal (lebar default).
	 *
	 * <p><b>Tujuan.</b> Entry point utama untuk membuka sub-halaman ZUL sebagai popup modal—dipakai
	 * di seluruh Action untuk membuka form detail, laporan, atau konfigurasi tanpa navigasi halaman.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonUiFactoryHelper.displayWindow(src, tampilToolbar)}.
	 * Ukuran dan judul window ditentukan oleh helper (default biasanya 600px lebar).</p>
	 *
	 * @param src            path ke file ZUL (mis. "/WEB-INF/zul/...")
	 * @param tampilToolbar  bila {@code true}, tampilkan toolbar bawaan window
	 * @return {@link MyWindow} yang dibuat
	 * @throws Exception bila ZUL tidak ditemukan atau gagal di-render
	 */
	public static MyWindow displayWindow(String src, boolean tampilToolbar) throws Exception {
		return CommonUiFactoryHelper.displayWindow(src, tampilToolbar);
	}

	/**
	 * Menampilkan halaman ZUL dalam popup modal dengan lebar tertentu.
	 *
	 * @param src           path ZUL
	 * @param tampilToolbar tampilkan toolbar
	 * @param lebar         lebar window (mis. "800px", "90%")
	 * @return {@link MyWindow} yang dibuat
	 * @throws Exception bila terjadi error render
	 */
	public static MyWindow displayWindow(String src, boolean tampilToolbar, String lebar) throws Exception {
		return CommonUiFactoryHelper.displayWindow(src, tampilToolbar, lebar);
	}

	/**
	 * Menampilkan halaman ZUL dalam popup modal dengan tinggi dan lebar tertentu.
	 *
	 * @param src           path ZUL
	 * @param tampilToolbar tampilkan toolbar
	 * @param tinggi        tinggi window (mis. "500px", "80vh")
	 * @param lebar         lebar window
	 * @return {@link MyWindow} yang dibuat
	 * @throws Exception bila terjadi error render
	 */
	public static MyWindow displayWindow(String src, boolean tampilToolbar, String tinggi, String lebar)
			throws Exception {
		return CommonUiFactoryHelper.displayWindow(src, tampilToolbar, tinggi, lebar);
	}

	/**
	 * Menampilkan halaman ZUL dalam popup modal dengan ukuran dan event listener penutup.
	 *
	 * <p><b>Tujuan.</b> Varian yang memungkinkan pemanggil mendaftarkan callback saat window ditutup
	 * (mis. untuk refresh grid setelah form detail disimpan).</p>
	 *
	 * @param src           path ZUL
	 * @param tampilToolbar tampilkan toolbar
	 * @param tinggi        tinggi window
	 * @param lebar         lebar window
	 * @param eventListener listener yang dipanggil saat window ditutup
	 * @return {@link MyWindow} yang dibuat
	 * @throws Exception bila terjadi error render
	 */
	public static MyWindow displayWindow(String src, boolean tampilToolbar, String tinggi, String lebar,
			final EventListener eventListener) throws Exception {
		return CommonUiFactoryHelper.displayWindow(src, tampilToolbar, tinggi, lebar, eventListener);
	}

	/**
	 * Menampilkan halaman ZUL dalam popup modal dengan ukuran, event listener, dan judul kustom.
	 *
	 * @param src           path ZUL
	 * @param tampilToolbar tampilkan toolbar
	 * @param tinggi        tinggi window
	 * @param lebar         lebar window
	 * @param eventListener listener penutup window
	 * @param judul         judul yang ditampilkan di title bar window
	 * @return {@link MyWindow} yang dibuat
	 * @throws Exception bila terjadi error render
	 */
	public static MyWindow displayWindow(String src, boolean tampilToolbar, String tinggi, String lebar,
			final EventListener eventListener, String judul) throws Exception {
		return CommonUiFactoryHelper.displayWindow(src, tampilToolbar, tinggi, lebar, eventListener, judul);
	}

	/**
	 * Menampilkan halaman ZUL dalam popup modal dengan semua opsi termasuk kontrol scroll.
	 *
	 * @param src           path ZUL
	 * @param tampilToolbar tampilkan toolbar
	 * @param tinggi        tinggi window
	 * @param lebar         lebar window
	 * @param eventListener listener penutup
	 * @param judul         judul window
	 * @param scroll        bila {@code true}, aktifkan autoscroll pada window
	 * @return {@link MyWindow} yang dibuat
	 * @throws Exception bila terjadi error render
	 */
	public static MyWindow displayWindow(String src, boolean tampilToolbar, String tinggi, String lebar,
			final EventListener eventListener, String judul, boolean scroll) throws Exception {
		return CommonUiFactoryHelper.displayWindow(src, tampilToolbar, tinggi, lebar, eventListener, judul, scroll);
	}

	/**
	 * Menampilkan berkas media ({@link FileFoto}) langsung di browser (download/tampil inline).
	 *
	 * <p><b>Tujuan.</b> Dipakai untuk menampilkan dokumen/gambar yang tersimpan sebagai BLOB di
	 * entitas {@link FileFoto} — mis. bukti pembayaran, foto profil, berkas lampiran.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonFileMediaHelper.display(alurFile)} yang
	 * membaca BLOB dari DB dan mengirimkannya sebagai {@code AMedia} ke browser.</p>
	 *
	 * @param alurFile entitas file yang akan ditampilkan
	 * @throws Exception bila BLOB tidak ditemukan atau gagal dibaca
	 */
	public static void display(FileFoto alurFile) throws Exception {
		CommonFileMediaHelper.display(alurFile);
	}

	/**
	 * Menampilkan berkas dalam popup modal dengan dukungan iframe dan berbagai mode tampil.
	 *
	 * <p><b>Tujuan.</b> Varian displayWindow untuk konten campuran: gambar langsung ({@code image=true}),
	 * ZUL include, atau iframe eksternal. Berguna untuk dokumen berformat beragam.</p>
	 *
	 * @param image         bila {@code true}, tampilkan sebagai gambar langsung
	 * @param src           URL/path sumber konten
	 * @param tampilToolbar tampilkan toolbar
	 * @param lebar         lebar window
	 * @param tinggi        tinggi window
	 * @param iframe        bila {@code true}, gunakan iframe
	 * @param ff            entitas FileFoto sumber (boleh {@code null})
	 * @return {@link MyWindow} yang dibuat
	 * @throws Exception bila terjadi error
	 */
	public static MyWindow displayWindow(Boolean image, String src, Boolean tampilToolbar, String lebar, String tinggi,
			Boolean iframe, FileFoto ff) throws Exception {
		return CommonUiFactoryHelper.displayWindow(image, src, tampilToolbar, lebar, tinggi, iframe, ff);
	}

	/**
	 * Membuat tombol "Simpan ke Drive" untuk mengunggah berkas ke Google Drive (satu folder).
	 *
	 * <p><b>Tujuan.</b> Menyediakan tombol toolbar untuk mengarsipkan berkas ke Google Drive
	 * dari dalam aplikasi — berguna untuk backup dokumen resmi (ijazah, transkrip, dll).</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonFileMediaHelper.simpanKeDrive(fileFoto, f, folderName, eventListener)}.
	 * Tombol yang dikembalikan perlu ditambahkan ke toolbar secara manual.</p>
	 *
	 * @param fileFoto      entitas file yang menyimpan metadata (nama, tipe, dll)
	 * @param f             berkas fisik yang akan diunggah
	 * @param folderName    nama folder tujuan di Google Drive
	 * @param eventListener listener yang dipanggil setelah upload selesai
	 * @return konfigurasi toolbar button
	 */
	public static MyToolbarbuttonConfig simpanKeDrive(FileFoto fileFoto, File f, String folderName,
			EventListener eventListener) {
		return CommonFileMediaHelper.simpanKeDrive(fileFoto, f, folderName, eventListener);
	}

	/**
	 * Membuat tombol "Simpan ke Drive" dengan struktur folder bertingkat (dua level).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti {@link #simpanKeDrive(FileFoto, File, String, EventListener)}
	 * namun dengan subfolder tambahan ({@code folderNameLagi}) untuk organisasi berkas lebih baik
	 * (mis. folder NIM di dalam folder Jurusan).</p>
	 *
	 * @param fileFoto        entitas file
	 * @param f               berkas fisik
	 * @param folderName      folder utama di Drive
	 * @param folderNameLagi  subfolder di dalam folder utama
	 * @param eventListener   listener setelah upload
	 * @return konfigurasi toolbar button
	 */
	public static MyToolbarbuttonConfig simpanKeDrive(FileFoto fileFoto, File f, String folderName,
			String folderNameLagi, EventListener eventListener) {
		return CommonFileMediaHelper.simpanKeDrive(fileFoto, f, folderName, folderNameLagi, eventListener);
	}

	/**
	 * Menampilkan halaman dalam popup modal menggunakan iframe (tanpa judul kustom).
	 *
	 * <p><b>Tujuan.</b> Embeds halaman eksternal atau halaman aplikasi lain dalam popup modal ZK
	 * via elemen {@code <iframe>} — berguna untuk tampilan laporan, halaman cetak, atau integrasi
	 * modul eksternal yang tidak bisa dirender sebagai komponen ZK langsung.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonUiFactoryHelper.displayWindowIframe(src, tampilToolbar, lebar, tinggi)}.</p>
	 *
	 * @param src           URL halaman yang akan di-embed
	 * @param tampilToolbar tampilkan toolbar
	 * @param lebar         lebar window
	 * @param tinggi        tinggi window
	 * @return {@link MyWindow} berisi iframe
	 * @throws Exception bila terjadi error
	 */
	public static MyWindow displayWindowIframe(String src, Boolean tampilToolbar, String lebar, String tinggi)
			throws Exception {
		return CommonUiFactoryHelper.displayWindowIframe(src, tampilToolbar, lebar, tinggi);
	}

	/**
	 * Menampilkan halaman dalam popup modal menggunakan iframe dengan judul kustom.
	 *
	 * @param src           URL halaman yang di-embed
	 * @param tampilToolbar tampilkan toolbar
	 * @param lebar         lebar window
	 * @param tinggi        tinggi window
	 * @param judul         judul title bar window
	 * @return {@link MyWindow} berisi iframe
	 * @throws Exception bila terjadi error
	 */
	public static MyWindow displayWindowIframe(String src, Boolean tampilToolbar, String lebar, String tinggi,
			String judul) throws Exception {
		return CommonUiFactoryHelper.displayWindowIframe(src, tampilToolbar, lebar, tinggi, judul);
	}

	/**
	 * Mengambil {@link JSONObject} dari URL HTTP eksternal.
	 *
	 * <p><b>Tujuan.</b> Memanggil API eksternal yang mengembalikan JSON object (mis. API pembayaran,
	 * API jadwal, API data pemerintah) dan langsung menguraikannya sebagai {@link JSONObject}.</p>
	 *
	 * <p><b>Cara kerja.</b> Memanggil {@link #getStringJson(String)} untuk mengambil teks JSON lalu
	 * mengurainya menjadi {@link JSONObject}. Timeout koneksi 5 detik (konfigurasi di getStringJson).</p>
	 *
	 * @param url URL endpoint API
	 * @return {@link JSONObject} hasil parsing
	 * @throws Exception bila URL tidak valid, timeout, atau respons bukan JSON object yang valid
	 */
	public static JSONObject getJsonObject(String url) throws Exception {
		JSONObject jsono = new JSONObject(getStringJson(url));
		return jsono;

	}

	/**
	 * Mengambil {@link JSONArray} dari URL HTTP eksternal secara defensif.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Seperti {@link #getJsonObject(String)} namun hasilnya
	 * {@link JSONArray} (daftar, bukan object). Exception ditangkap diam-diam dan mengembalikan
	 * {@code null} — pemanggil harus memeriksa null sebelum iterasi.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Bila API kadang mengembalikan JSON object alih-alih array (error response),
	 * tambahkan penanganan khusus di pemanggil karena method ini selalu mengembalikan null bila gagal.</p>
	 *
	 * @param url URL endpoint API
	 * @return {@link JSONArray} hasil parsing, atau {@code null} bila gagal/timeout/bukan array
	 */
	public static JSONArray getJsonArray(String url) {
		try {
			JSONArray jsono = new JSONArray(getStringJson(url));

			return jsono;
		} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/common/Common.java:8155");
			// Common.tampilErrorJikaAdmin(ee);
		}
		return null;
	}

	/**
	 * Mengambil isi respons HTTP dari URL sebagai string (UTF-8).
	 *
	 * <p><b>Tujuan.</b> Lapisan paling rendah dari komunikasi HTTP keluar aplikasi: dipakai oleh
	 * {@link #getJsonObject} dan {@link #getJsonArray} untuk mengambil teks respons.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Buka koneksi ke URL dengan {@link URLConnection}.</li>
	 *   <li>Set header {@code Content-Type: application/json} dan timeout koneksi+baca 5 detik.</li>
	 *   <li>Baca respons baris-demi-baris via {@link BufferedReader} dengan encoding UTF-8.</li>
	 *   <li>Tutup reader dan kembalikan konten respons.</li>
	 * </ol>
	 * Catatan: {@code in.close()} dipanggil dua kali (baris ganda di source) — tidak menyebabkan
	 * error karena {@code close()} idempoten, tapi sebaiknya dihapus satu.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Tidak ada retry. Untuk endpoint yang tidak stabil, pertimbangkan
	 * menambahkan retry di pemanggil. Gunakan HTTPS (bukan HTTP) untuk endpoint produksi.</p>
	 *
	 * @param urlString URL lengkap endpoint
	 * @return isi respons sebagai string
	 * @throws Exception bila koneksi gagal, timeout, atau URL tidak valid
	 */
	public static String getStringJson(String urlString) throws Exception {

		URL url = new URL(urlString);
		URLConnection connection = url.openConnection();
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setConnectTimeout(5000);
		connection.setReadTimeout(5000);

		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
		String json = "";
		String str = "";
		while ((json = in.readLine()) != null) {
			byte[] bytes = json.getBytes("UTF-8");
			str += new String(bytes, "UTF-8");
		}
		in.close();
		in.close();
		return str;

	}

	/**
	 * <h3>Pindai semua kelas Java dalam suatu paket beserta sub-paketnya</h3>
	 *
	 * <p><b>Tujuan.</b> Mengumpulkan semua kelas yang dapat diakses oleh class loader
	 * konteks thread saat ini yang berasal dari paket tertentu dan seluruh
	 * sub-paketnya, tanpa perlu mendaftar nama kelas secara manual.</p>
	 *
	 * <p><b>Cara kerja.</b> Nama paket dikonversi menjadi path direktori (titik → {@code /}),
	 * lalu class loader mencari semua resource yang cocok di seluruh classpath.
	 * Setiap direktori yang ditemukan diproses secara rekursif oleh
	 * {@link #findClasses(File, String)} untuk mengumpulkan file {@code .class}.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Dipanggil oleh {@link #populateDashboard()} untuk
	 * menemukan subkelas {@code MyWindow} di paket dashboard secara otomatis.
	 * Bila paket dipindahkan, cukup ubah string nama paket di pemanggil.</p>
	 *
	 * @param packageName nama paket dasar, misalnya {@code "ais.action.master.dashboard.admin"}.
	 * @return array semua {@link Class} yang ditemukan di paket dan sub-paketnya.
	 * @throws ClassNotFoundException bila nama kelas tidak dapat dimuat.
	 * @throws IOException            bila terjadi kegagalan akses resource classpath.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Class[] getClasses(String packageName) throws ClassNotFoundException, IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		assert classLoader != null;
		String path = packageName.replace('.', '/');
		Enumeration resources = classLoader.getResources(path);
		List<File> dirs = new ArrayList<File>();
		while (resources.hasMoreElements()) {
			URL resource = (URL) resources.nextElement();
			dirs.add(new File(resource.getFile()));
		}
		ArrayList classes = new ArrayList();
		for (File directory : dirs) {
			classes.addAll(findClasses(directory, packageName));
		}
		return (Class[]) classes.toArray(new Class[classes.size()]);
	}

	/**
	 * <h3>Rekursif: kumpulkan semua kelas dari direktori classpath</h3>
	 *
	 * <p><b>Tujuan.</b> Menelusuri direktori classpath secara rekursif dan mengumpulkan
	 * semua kelas ({@code .class}) yang ditemukan, termasuk di sub-direktori
	 * (yang merepresentasikan sub-paket).</p>
	 *
	 * <p><b>Cara kerja.</b> Untuk setiap entri dalam direktori: bila entri adalah
	 * sub-direktori, method memanggil dirinya sendiri dengan nama paket diperpanjang;
	 * bila entri berakhiran {@code .class}, kelas dimuat via {@link Class#forName(String)}
	 * dengan nama lengkap (paket + nama kelas tanpa sufiks).</p>
	 *
	 * @param directory   direktori yang akan dipindai.
	 * @param packageName nama paket Java yang bersesuaian dengan direktori ini.
	 * @return list kelas yang ditemukan.
	 * @throws ClassNotFoundException bila kelas tidak dapat dimuat dari classpath.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static List findClasses(File directory, String packageName) throws ClassNotFoundException {
		List classes = new ArrayList();
		if (!directory.exists()) {
			return classes;
		}
		File[] files = directory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				assert !file.getName().contains(".");
				classes.addAll(findClasses(file, packageName + "." + file.getName()));
			} else if (file.getName().endsWith(".class")) {
				classes.add(
						Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
			}
		}
		return classes;
	}

	/**
	 * <h3>Populasi otomatis entri Dashboard dari classpath via refleksi</h3>
	 *
	 * <p><b>Tujuan.</b> Memindai paket {@code ais.action.master.dashboard.admin},
	 * {@code .keuangan}, dan {@code .library} untuk menemukan semua subkelas
	 * {@link ais.ui.util.MyWindow}, lalu mendaftarkannya ke tabel {@code Dashboard}
	 * di database apabila belum terdaftar. Dengan cara ini, penambahan kelas dasbor
	 * baru otomatis tersedia di menu tanpa perlu mengisi data manual.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Untuk tiap sub-paket, {@link org.reflections.Reflections} mengumpulkan
	 *       semua subkelas {@code MyWindow}.</li>
	 *   <li>Setiap kelas diperiksa: bila belum ada entri {@link Dashboard} dengan
	 *       {@code clazz} = nama kelas lengkap, entri baru dibuat.</li>
	 *   <li>Nama tampilan dihasilkan dari nama kelas: kata "Dashboard" dihapus,
	 *       camelCase dipecah via {@link #splitByUppercase(String)}, dan label sub-paket
	 *       (misal "admin") ditambahkan dalam kurung.</li>
	 * </ol></p>
	 *
	 * <p><b>Threading.</b> Harus dipanggil dalam konteks ZK request yang memiliki
	 * {@code currentSession()} aktif karena memakai Hibernate {@code session.save()}.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Bila paket dasbor baru ditambahkan, tambahkan blok
	 * {@code reflections = new Reflections("paket.baru")} di sini. Kelas yang sudah
	 * terdaftar tidak akan diduplikasi karena ada pemeriksaan {@code rowCount == 0}.</p>
	 *
	 * @throws Exception bila terjadi kesalahan Hibernate atau refleksi classpath.
	 */
	@SuppressWarnings("rawtypes")
	public static void populateDashboard() throws Exception {

		// String data =
		// "ais.action.master.dashboard.admin.DashboardRekapKunjunganPengguna;Rekap
		// Kunjungan Pengguna"
		// +
		// "\nais.action.report.helper.keuangan.LaporanRekapMahasiswaSudahBayarWindow;Rekap
		// Mahasiswa Sudah Bayar"
		// +
		// "\nais.action.master.dashboard.admin.DashboardRekapMenuPengguna;Rekap
		// Menu Pengguna"
		// +
		// "\nais.action.master.dashboard.admin.DashboardStatistikStatusMahasiswa;Statistik
		// Status Mahasiswa"
		// +
		// "\nais.action.master.dashboard.admin.DashboardStatistikPengambilanKRSMahasiswa;Statistik
		// Pengambilan KRS"
		// + "\nais.action.report.helper.nilai.LaporanDaftarNilaiWindow;Nilai
		// Rata-Rata"
		// +
		// "\nais.action.master.dashboard.admin.DashboardRekapStatusMahasiswa;Rekap
		// Status Mahasiswa"
		// +
		// "\nais.action.master.dashboard.admin.DashboardStatistikJumlahMahasiswa;Statistik
		// Jumlah Mahasiswa"
		// +
		// "\nais.action.report.helper.keuangan.LaporanRekapHostToHostWindow;Rekapitulasi
		// Pembayaran"
		// +
		// "\nais.action.report.helper.akademik.LaporanRekapPenilaianMahasiswaWindow;Rekap
		// Penilaian Mahasiswa"
		// +
		// "\nais.action.master.dashboard.admin.DashboardStatistikPenilaian;Statistik
		// Penilaian"
		// +
		// "\nais.action.master.dashboard.admin.DashboardRekapJenisSekolahMahasiswaBaru;Rekap
		// Jenis Asal Sekolah Mahasiswa Baru"
		// +
		// "\nais.action.report.helper.nilai.LaporanDaftarPrestasiBelajarWindow;Prestasi
		// Belajar"
		// +
		// "\nais.action.master.dashboard.admin.DashboardStatistikPerkuliahan;Statistik
		// Perkuliahan"
		// +
		// "\nais.action.master.dashboard.keuangan.DashboardPerbandinganSudahMembayarDanBelum;Perbandingan
		// Sudah Dan Belum Membayar"
		// +
		// "\nais.action.master.dashboard.admin.DashboardLogAktifitasPengguna;Aktifitas
		// Pengguna"
		// +
		// "\nais.action.report.helper.keuangan.LaporanRekapHostToHostWindow;Rekap
		// Mahasiswa Belum Bayar"
		// +
		// "\nais.action.master.dashboard.admin.DashboardRekapPerkuliahan;Rekap
		// Perkuliahan"
		// +
		// "\nais.action.master.dashboard.admin.DashboardStatistikKunjunganPengguna;Statistik
		// Kunjungan Pengguna"
		// + "\nais.action.report.helper.akademik.LaporanDaftarHadirDosen;Jadwal
		// Hadir Dosen"
		// +
		// "\nais.action.master.dashboard.keuangan.DashboardRekapPembayaranMahasiswa;Rekap
		// Pembayaran Mahasiswa"
		// +
		// "\nais.action.master.dashboard.admin.DashboardStatistikJenisSekolahMahasiswaBaru;Statistik
		// Jenis Asal Sekolah Mahasiswa Baru"
		// +
		// "\nais.action.master.dashboard.admin.DashboardRekapPengambilanKRSMahasiswa;Rekap
		// Pengambilan KRS";
		// String[] ss = StringUtils.split(data, "\n");

		final Session session = HibernateUtil.currentSession();
		/**
		 * Tipe implementasi bersarang {@link CheckClass} milik {@link Common}. Kelas ini memberi nama pada state atau
		 * perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link Common} dan dapat mengakses state kelas induk.
		 * Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code check}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
		 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
		 * tambahkan perilaku lintas domain pada service bersama.</p>
		 *
		 * @see Common
		 */
		class CheckClass {
			public void check(Class s, String sub) {
				int jml = ((Number) session.createCriteria(Dashboard.class).add(Restrictions.eq("clazz", s.getName()))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();
				// System.out.println("check class " + s.getName() + " " + jml);
				if (jml == 0) {
					String nama = Common.splitByUppercase(s.getName().replaceAll("Dashboard", "")).replaceAll("_", " ");
					Dashboard dashboard = new Dashboard();
					dashboard.setNama(nama + " (" + sub + ")");
					dashboard.setClazz(s.getName());
					session.save(dashboard);
				}
			}
		}

		CheckClass checkClass = new CheckClass();

		org.reflections.Reflections reflections = new org.reflections.Reflections("ais.action.master.dashboard.admin");
		Set<Class<? extends MyWindow>> allClasses = reflections.getSubTypesOf(MyWindow.class);
		// System.out.println("allClasses => " + allClasses);
		for (Class s : allClasses) {
			checkClass.check(s, "admin");
		}

		reflections = new org.reflections.Reflections("ais.action.master.dashboard.keuangan");
		allClasses = reflections.getSubTypesOf(MyWindow.class);
		for (Class s : allClasses) {
			checkClass.check(s, "keuangan");
		}
		reflections = new org.reflections.Reflections("ais.action.master.dashboard.library");
		allClasses = reflections.getSubTypesOf(MyWindow.class);
		for (Class s : allClasses) {
			checkClass.check(s, "library");
		}
	}

	/**
	 * <h3>Pecah string camelCase berdasarkan huruf kapital (lookahead)</h3>
	 *
	 * <p><b>Tujuan.</b> Mengubah nama kelas Java (termasuk nama paket lengkap) menjadi
	 * kata-kata terpisah spasi agar bisa dipakai sebagai label tampilan yang mudah dibaca.
	 * Contoh: {@code "ais.action.DashboardRekapPembayaran"} → {@code "Rekap Pembayaran"}.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>String dipotong berdasarkan titik; bagian terakhir (nama kelas) diambil.</li>
	 *   <li>Nama kelas dipecah tepat sebelum setiap huruf kapital via regex
	 *       {@code "(?=\\p{Lu})"} (lookahead Unicode uppercase).</li>
	 *   <li>Bagian-bagian digabung dengan spasi.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan.</b> Dipanggil oleh {@link #populateDashboard()} untuk
	 * menghasilkan nama tampilan dasbor. Bila nama kelas mengandung singkatan semua
	 * kapital (misal {@code "IPK"}), setiap huruf akan dipisah menjadi spasi sendiri.</p>
	 *
	 * @param s string input; dapat berupa nama kelas lengkap dengan paket atau tanpa paket.
	 * @return string dengan kata-kata dipisah spasi berdasarkan batas huruf kapital.
	 */
	public static String splitByUppercase(String s) {
		String[] ss = s.split("\\.");
		s = ss[ss.length - 1];
		String[] r = s.split("(?=\\p{Lu})");
		String result = "";
		for (String it : r) {
			result += result.isEmpty() ? it : " " + it;
		}
		return result;
	}

	/**
	 * <h3>Konversi KalenderAkademik ke SimpleCalendarEvent ZK untuk tampilan kalender</h3>
	 *
	 * <p><b>Tujuan.</b> Mengubah entitas {@link ais.model.master.akademik.KalenderAkademik}
	 * (yang menyimpan rentang tanggal mulai–selesai kegiatan akademik) menjadi
	 * {@link org.zkoss.zul.SimpleCalendarEvent} yang dapat ditambahkan ke komponen
	 * {@code Calendar} ZK untuk ditampilkan sebagai event pada hari tertentu.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Jam/menit/detik dari tanggal mulai dan selesai KalenderAkademik diekstrak.</li>
	 *   <li>Waktu tersebut disematkan ke tanggal {@code current} (tanggal hari yang
	 *       sedang dirender di kalender) sehingga event muncul pada hari yang tepat.</li>
	 *   <li>Bila waktu mulai > waktu selesai (event melewati tengah malam), waktu selesai
	 *       diset ke 23:59:59 hari yang sama agar event tidak meluap ke hari berikutnya.</li>
	 *   <li>Judul event diisi dengan ID kalender akademik; konten diisi nama kegiatan,
	 *       deskripsi, dan rentang tanggal format Indonesia.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan.</b> Event dikunci ({@code locked=true}) agar pengguna tidak
	 * dapat memindahnya via drag-drop di UI. Konten label bergantung pada
	 * {@code dateFormat} thread-local {@link Common#dateFormat}.</p>
	 *
	 * @param myKalenderAkademik entitas kalender akademik sumber data.
	 * @param current            Calendar yang menunjukkan hari kalender yang sedang dirender.
	 * @return {@link org.zkoss.zul.SimpleCalendarEvent} siap ditambahkan ke komponen kalender ZK.
	 */
	public static SimpleCalendarEvent createSimpleCalendarEvent(KalenderAkademik myKalenderAkademik, Calendar current) {
		Calendar m = ais.ui.util.WaktuUtil.getCalendar();
		m.setTime(myKalenderAkademik.getTanggalMulai());
		Calendar s = ais.ui.util.WaktuUtil.getCalendar();
		s.setTime(myKalenderAkademik.getTanggalSelesai());

		Calendar dimulai = ais.ui.util.WaktuUtil.getCalendar();
		dimulai.setTime(current.getTime());
		dimulai.set(Calendar.HOUR_OF_DAY, m.get(Calendar.HOUR_OF_DAY));
		dimulai.set(Calendar.MINUTE, m.get(Calendar.MINUTE));
		dimulai.set(Calendar.SECOND, m.get(Calendar.SECOND));

		Calendar sampai = ais.ui.util.WaktuUtil.getCalendar();
		sampai.setTime(current.getTime());
		sampai.set(Calendar.HOUR_OF_DAY, s.get(Calendar.HOUR_OF_DAY));
		sampai.set(Calendar.MINUTE, s.get(Calendar.MINUTE));
		sampai.set(Calendar.SECOND, s.get(Calendar.SECOND));

		SimpleCalendarEvent sce = new SimpleCalendarEvent();
		sce.setLocked(true);
		sce.setTitle(myKalenderAkademik.getId() + "");

		if (dimulai.getTime().after(sampai.getTime())) {
			sce.setBeginDate(dimulai.getTime());
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(sampai.getTime());
			calendar.set(Calendar.HOUR_OF_DAY, 23);
			calendar.set(Calendar.MINUTE, 59);
			calendar.set(Calendar.SECOND, 59);
			sce.setEndDate(calendar.getTime());
		} else {
			sce.setBeginDate(dimulai.getTime());
			sce.setEndDate(sampai.getTime());
		}

		sce.setContent(myKalenderAkademik.getNamaKegiatanAkademik() + " "
				+ myKalenderAkademik.getDeskripsiKegiatanAkademik() + " "
				+ (myKalenderAkademik.getTanggalMulai() == null ? ""
						: " -> " + Common.dateFormat.get().format(myKalenderAkademik.getTanggalMulai()))

				+ (myKalenderAkademik.getTanggalSelesai() == null ? ""
						: " s.d " + Common.dateFormat.get().format(myKalenderAkademik.getTanggalSelesai()))

				+ "");
		return sce;
	}

	/**
	 * <h3>Periksa apakah mahasiswa memenuhi prasyarat matakuliah</h3>
	 *
	 * <p><b>Tujuan.</b> Memvalidasi bahwa mahasiswa telah memenuhi semua prasyarat
	 * (matakuliah yang harus sudah lulus terlebih dahulu) sebelum mengambil
	 * {@code matakuliah} tertentu pada semester yang ditunjuk.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi penuh ke
	 * {@link ais.common.CommonAcademicSyncHelper#checkMatakuliahPrasyarat}
	 * yang menelusuri relasi prasyarat matakuliah di database dan mencocokkan
	 * dengan riwayat nilai mahasiswa.</p>
	 *
	 * @param matakuliah matakuliah yang ingin diambil.
	 * @param mahasiswa  mahasiswa yang bersangkutan.
	 * @param semester   semester saat pengambilan KRS.
	 * @return {@code true} bila semua prasyarat terpenuhi; {@code false} bila belum.
	 * @throws Exception bila terjadi kesalahan akses database.
	 */
	public static Boolean checkMatakuliahPrasyarat(Matakuliah matakuliah, Mahasiswa mahasiswa, Integer semester)
			throws Exception {
		return CommonAcademicSyncHelper.checkMatakuliahPrasyarat(matakuliah, mahasiswa, semester);
	}

	/**
	 * <h3>Buat tombol "Upload Data" dari file Excel (overload minimal)</h3>
	 *
	 * <p><b>Tujuan.</b> Pabrik tombol unggah Excel ke entitas tertentu. Tombol ini
	 * memungkinkan pengguna mengimpor data massal dari file {@code .xlsx}. Overload
	 * ini menggunakan nilai default untuk semua listener dan kriteria.</p>
	 *
	 * <p><b>Cara kerja.</b> Meneruskan ke overload lengkap dengan semua parameter
	 * opsional bernilai {@code null}.</p>
	 *
	 * @param dataSearchDefault callback untuk me-refresh grid setelah import selesai.
	 * @param clazz             kelas entitas target import.
	 * @param columns           nama kolom yang akan diimpor dari Excel.
	 * @return konfigurasi tombol siap ditambahkan ke toolbar.
	 */
	public static MyToolbarbuttonConfig uploadData(DataSearchDefault dataSearchDefault,
			@SuppressWarnings("rawtypes") Class clazz, String... columns) {
		return uploadData(dataSearchDefault, clazz, null, null, null, columns);
	}

	/**
	 * <h3>Buat tombol "Upload Data" dengan listener setelah unggah</h3>
	 *
	 * <p>Overload dengan {@code uploadListener} untuk hook validasi/proses setelah
	 * file dipilih pengguna, sebelum data disimpan.</p>
	 *
	 * @param dataSearchDefault callback refresh grid.
	 * @param clazz             kelas entitas target.
	 * @param uploadListener    EventListener dipanggil setelah file dipilih.
	 * @param columns           kolom yang diimpor.
	 * @return konfigurasi tombol upload.
	 */
	public static MyToolbarbuttonConfig uploadData(DataSearchDefault dataSearchDefault,
			@SuppressWarnings("rawtypes") Class clazz, EventListener uploadListener, final String... columns) {
		return uploadData(dataSearchDefault, clazz, uploadListener, null, null, columns);
	}

	/**
	 * <h3>Buat tombol "Upload Data" dengan dua listener (upload dan pasca-simpan)</h3>
	 *
	 * <p>Overload dengan {@code uploadListener} (setelah file dipilih) dan
	 * {@code uploadListenerAfterSimpan} (setelah data berhasil disimpan ke DB).
	 * Berguna bila halaman perlu memperbarui UI tambahan setelah proses simpan.</p>
	 *
	 * @param dataSearchDefault          callback refresh grid.
	 * @param clazz                      kelas entitas target.
	 * @param uploadListener             listener setelah file dipilih.
	 * @param uploadListenerAfterSimpan  listener setelah data disimpan.
	 * @param columns                    kolom yang diimpor.
	 * @return konfigurasi tombol upload.
	 */
	public static MyToolbarbuttonConfig uploadData(DataSearchDefault dataSearchDefault,
			@SuppressWarnings("rawtypes") Class clazz, EventListener uploadListener,
			EventListener uploadListenerAfterSimpan, String... columns) {
		return uploadData(dataSearchDefault, clazz, uploadListener, uploadListenerAfterSimpan, null, null, columns);
	}

	/**
	 * <h3>Buat tombol "Upload Data" dengan kriteria ID dan nilai default kolom</h3>
	 *
	 * <p>Overload dengan {@code idCrit} (kriteria Hibernate untuk membatasi baris
	 * yang boleh diperbarui) dan {@code nilai} (nilai default yang disuntikkan ke
	 * setiap baris yang diimpor, misalnya {@code tahunAkademik=current}).</p>
	 *
	 * @param dataSearchDefault callback refresh grid.
	 * @param clazz             kelas entitas target.
	 * @param uploadListener    listener setelah file dipilih.
	 * @param idCrit            kriteria Hibernate pembatas baris yang boleh diupdate.
	 * @param nilai             map nilai default yang disuntikkan ke setiap baris impor.
	 * @param columns           kolom yang diimpor.
	 * @return konfigurasi tombol upload.
	 */
	public static MyToolbarbuttonConfig uploadData(DataSearchDefault dataSearchDefault,
			@SuppressWarnings("rawtypes") Class clazz, EventListener uploadListener, Criterion idCrit,
			final HashMap<String, Object> nilai, String... columns) {
		return uploadData(dataSearchDefault, clazz, uploadListener, null, idCrit, nilai, columns);
	}

	/**
	 * <h3>Buat tombol "Upload Data" — overload paling lengkap (delegasi ke CommonDownloadUpload)</h3>
	 *
	 * <p><b>Tujuan.</b> Titik akhir semua overload {@code uploadData}. Semua parameter
	 * import dikumpulkan di sini dan diteruskan ke
	 * {@link ais.common.CommonDownloadUpload#uploadData} yang bertanggung jawab atas
	 * logika membaca file Excel, memetakan kolom, dan menyimpan ke database.</p>
	 *
	 * <p><b>Cara kerja.</b> Tidak ada logika tambahan di sini — hanya delegasi 1 baris
	 * ke helper. Pabrik tombol akan menampilkan dialog file-picker saat diklik, membaca
	 * konten Excel, dan memanggil listener sesuai urutan: {@code uploadListener} →
	 * simpan ke DB → {@code uploadListenerAfterSimpan} → refresh grid.</p>
	 *
	 * @param dataSearchDefault         callback refresh grid setelah import selesai.
	 * @param clazz                     kelas entitas Hibernate target import.
	 * @param uploadListener            listener setelah file Excel dipilih pengguna.
	 * @param uploadListenerAfterSimpan listener setelah data berhasil disimpan ke DB.
	 * @param idCrit                    kriteria Hibernate untuk memfilter baris yang boleh diupdate.
	 * @param nilai                     nilai default yang disuntikkan ke setiap baris yang diimpor.
	 * @param columns                   nama properti entitas yang diimpor dari kolom Excel.
	 * @return konfigurasi tombol upload siap dipasang ke toolbar.
	 */
	@SuppressWarnings("rawtypes")
	public static MyToolbarbuttonConfig uploadData(DataSearchDefault dataSearchDefault, Class clazz,
			EventListener uploadListener, EventListener uploadListenerAfterSimpan, Criterion idCrit,
			HashMap<String, Object> nilai, String... columns) {
		return CommonDownloadUpload.uploadData(dataSearchDefault, clazz, uploadListener, uploadListenerAfterSimpan,
				idCrit, nilai, columns);
	}

	/**
	 * <h3>Tambahkan tombol "Download" Excel ke komponen induk</h3>
	 *
	 * <p><b>Tujuan.</b> Menyematkan tombol unduh data dalam format Excel ke komponen
	 * ZK ({@code anchor}) — biasanya toolbar atau toolbox — yang langsung mengunduh
	 * data sesuai {@code dataCriteria} aktif tanpa pratinjau.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonDownloadUpload#appendDownloadButton}. Tombol memanggil
	 * builder Excel internal (via {@link #cetakData}) saat diklik.</p>
	 *
	 * @param anchor        komponen ZK tempat tombol disematkan.
	 * @param clazz         kelas entitas yang diunduh.
	 * @param dataCriteria  kriteria pencarian yang menentukan data yang diunduh.
	 * @param columns       nama properti kolom yang disertakan dalam file Excel.
	 */
	@SuppressWarnings("rawtypes")
	public static void appendDownloadButton(Component anchor, Class clazz, DataCriteria dataCriteria,
			String... columns) {
		CommonDownloadUpload.appendDownloadButton(anchor, clazz, dataCriteria, columns);
	}

	/**
	 * <h3>Tambahkan tombol "Download" dan/atau "Upload" Excel ke komponen induk</h3>
	 *
	 * <p><b>Tujuan.</b> Versi gabungan yang menyematkan tombol unduh sekaligus tombol
	 * unggah ke komponen yang sama. Tombol upload dapat disembunyikan bila pengguna
	 * tidak memiliki hak impor.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonDownloadUpload#appendDownloadUploadButtons}. Tombol
	 * upload memanggil {@link #uploadData} saat diklik.</p>
	 *
	 * @param anchor            komponen ZK tempat tombol-tombol disematkan.
	 * @param clazz             kelas entitas.
	 * @param dataCriteria      kriteria pencarian untuk unduh.
	 * @param dataSearchDefault callback refresh grid setelah upload berhasil.
	 * @param uploadVisible     {@code true} = tombol upload ditampilkan.
	 * @param columns           kolom yang disertakan.
	 */
	@SuppressWarnings("rawtypes")
	public static void appendDownloadUploadButtons(Component anchor, Class clazz, DataCriteria dataCriteria,
			DataSearchDefault dataSearchDefault, boolean uploadVisible, String... columns) {
		CommonDownloadUpload.appendDownloadUploadButtons(anchor, clazz, dataCriteria, dataSearchDefault, uploadVisible,
				columns);
	}

	/**
	 * <h3>Terapkan gaya standar ke lembar kerja ZSS Worksheet</h3>
	 *
	 * <p><b>Tujuan.</b> Menerapkan format/gaya bawaan aplikasi (warna header, lebar kolom
	 * default, border, dll.) ke lembar kerja {@link org.zkoss.zss.model.Worksheet} ZK
	 * Spreadsheet (komponen pratinjau Excel berbasis ZSS).</p>
	 *
	 * <p>Delegasi ke {@link ais.common.CommonExcelStyleHelper#setStyled(Worksheet)}.</p>
	 *
	 * @param sheet lembar kerja ZSS yang akan diberi gaya.
	 */
	public static void setStyled(Worksheet sheet) {
		CommonExcelStyleHelper.setStyled(sheet);
	}

	/**
	 * <h3>Terapkan gaya standar ke XSSFSheet Apache POI</h3>
	 *
	 * <p><b>Tujuan.</b> Versi untuk {@link org.apache.poi.xssf.usermodel.XSSFSheet} —
	 * sheet POI yang dipakai saat membangun file {@code .xlsx} di server.
	 * Menerapkan format yang sama dengan overload {@link #setStyled(Worksheet)}
	 * sehingga file yang diunduh memiliki tampilan konsisten.</p>
	 *
	 * <p>Delegasi ke {@link ais.common.CommonExcelStyleHelper#setStyled(XSSFSheet)}.</p>
	 *
	 * @param sheet lembar kerja POI yang akan diberi gaya.
	 */
	public static void setStyled(XSSFSheet sheet) {
		CommonExcelStyleHelper.setStyled(sheet);
	}

	/**
	 * <h3>Ambil semua nama kolom (properti) suatu kelas entitas Hibernate</h3>
	 *
	 * <p><b>Tujuan.</b> Mengambil daftar nama properti suatu kelas entitas Hibernate
	 * secara dinamis (tanpa hardcode), mulai dari properti identifier (primary key)
	 * hingga semua properti lainnya. Biasanya dipakai untuk membangun header kolom
	 * Excel secara otomatis saat memanggil {@link #cetakData}.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Ambil {@link org.hibernate.metadata.ClassMetadata} via Hibernate.</li>
	 *   <li>Masukkan {@code identifierPropertyName} sebagai elemen pertama.</li>
	 *   <li>Tambahkan semua {@code propertyNames} (non-id) ke dalam list.</li>
	 *   <li>Konversi ke array string dan kembalikan.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan.</b> Urutan kolom mengikuti urutan definisi di mapping Hibernate,
	 * bukan urutan fisik kolom di database.</p>
	 *
	 * @param clazz kelas entitas Hibernate yang propertinya ingin diambil.
	 * @return array nama properti; elemen pertama selalu identifier (PK).
	 */
	@SuppressWarnings("rawtypes")
	public static String[] getColumns(Class clazz) {
		ClassMetadata meta = HibernateUtil.getClassMetadata(clazz);
		List<String> ss = new ArrayList<String>();
		ss.add(meta.getIdentifierPropertyName());
		for (String s : meta.getPropertyNames()) {
			ss.add(s);
		}
		return ss.toArray(new String[] {});
	}

	/**
	 * <h3>Buat tombol "Download" dengan label dan ikon standar (tanpa kelas)</h3>
	 *
	 * <p><b>Tujuan.</b> Pabrik tombol unduh data ke Excel dengan label "Download" dan
	 * ikon {@code /img/print.png} standar, tanpa perlu menetapkan kelas entitas secara
	 * eksplisit. Kelas entitas dapat disimpulkan dari {@code dataCriteria}.</p>
	 *
	 * <p><b>Cara kerja.</b> Meneruskan ke {@link #cetakDataCustomButton} dengan
	 * {@code clazz=null}.</p>
	 *
	 * @param dataCriteria kriteria pencarian yang menentukan data yang diunduh.
	 * @param columns      nama properti kolom yang disertakan dalam Excel.
	 * @return konfigurasi tombol unduh siap dipasang ke toolbar.
	 */
	public static MyToolbarbuttonConfig cetakData(DataCriteria dataCriteria, String... columns) {
		return cetakDataCustomButton(null, dataCriteria, "Download", "/img/print.png", columns);
	}

	/**
	 * <h3>Buat tombol "Download" dengan kelas entitas eksplisit</h3>
	 *
	 * <p><b>Tujuan.</b> Overload {@link #cetakData(DataCriteria, String[])} dengan
	 * kelas entitas Hibernate yang eksplisit. Diperlukan bila tipe data tidak dapat
	 * disimpulkan dari {@code dataCriteria} saja (misalnya criteria polimorfis).</p>
	 *
	 * <p><b>Cara kerja.</b> Meneruskan ke {@link #cetakDataCustomButton} dengan
	 * label "Download" dan ikon default.</p>
	 *
	 * @param clazz        kelas entitas Hibernate yang datanya diunduh.
	 * @param dataCriteria kriteria pencarian yang menentukan data yang diunduh.
	 * @param columns      nama properti kolom yang disertakan dalam Excel.
	 * @return konfigurasi tombol unduh siap dipasang ke toolbar.
	 */
	@SuppressWarnings("rawtypes")
	public static MyToolbarbuttonConfig cetakData(Class clazz, DataCriteria dataCriteria, String... columns) {
		return cetakDataCustomButton(clazz, dataCriteria, "Download", "/img/print.png", columns);
	}

	/**
	 * <h3>Buat tombol ekspor Excel dengan label/ikon kustom (overload dasar)</h3>
	 *
	 * <p><b>Tujuan.</b> Overload {@link #cetakDataCustomButton} paling dasar dengan
	 * label dan ikon tombol yang dapat dikustomisasi, namun tanpa kolom tambahan
	 * ({@code columnHeadersAdding=null}, {@code dataAdding=null}).</p>
	 *
	 * @param clazz        kelas entitas Hibernate.
	 * @param dataCriteria kriteria pencarian.
	 * @param buttonLabel  teks label tombol (misal "Cetak Laporan").
	 * @param buttonImage  path ikon tombol (misal {@code "/img/print.png"}).
	 * @param columns      kolom yang disertakan dalam Excel.
	 * @return konfigurasi tombol ekspor.
	 */
	@SuppressWarnings("rawtypes")
	public static MyToolbarbuttonConfig cetakDataCustomButton(Class clazz, DataCriteria dataCriteria,
			String buttonLabel, String buttonImage, String... columns) {
		return cetakDataCustomButton(clazz, dataCriteria, buttonLabel, buttonImage, null, null, columns);
	}

	/**
	 * <h3>Buat tombol ekspor Excel dengan kolom tambahan dan listener data kustom</h3>
	 *
	 * <p><b>Tujuan.</b> Overload dengan {@code columnHeadersAdding} (header kolom
	 * tambahan yang tidak berasal dari entitas) dan {@code dataAdding} (listener yang
	 * mengisi nilai kolom tambahan tersebut per-baris saat membangun file Excel).
	 * Berguna untuk menambahkan kolom komputasi seperti "Jumlah Tagihan" yang tidak
	 * ada sebagai properti entitas.</p>
	 *
	 * @param clazz               kelas entitas Hibernate.
	 * @param dataCriteria        kriteria pencarian.
	 * @param buttonLabel         teks label tombol.
	 * @param buttonImage         path ikon tombol.
	 * @param columnHeadersAdding header kolom tambahan yang disertakan di akhir Excel.
	 * @param dataAdding          listener yang mengisi data kolom tambahan per baris.
	 * @param columns             kolom entitas yang disertakan.
	 * @return konfigurasi tombol ekspor.
	 */
	@SuppressWarnings("rawtypes")
	public static MyToolbarbuttonConfig cetakDataCustomButton(Class clazz, DataCriteria dataCriteria,
			String buttonLabel, String buttonImage, List<String> columnHeadersAdding, EventListener dataAdding,
			String... columns) {
		return cetakDataCustomButton(clazz, dataCriteria, buttonLabel, buttonImage, columnHeadersAdding, dataAdding,
				false, null, columns);
	}

	/**
	 * <h3>Buat tombol ekspor Excel dengan dua set kolom tambahan (tab utama + tab tambahan)</h3>
	 *
	 * <p><b>Tujuan.</b> Overload dengan {@code adaTambahan=true} untuk menyertakan
	 * tab data tambahan (sheet kedua dalam file Excel) dengan header
	 * {@code columnHeadersAddingTambahan}. Nama tab tambahan default "DATA TAMBAHAN".</p>
	 *
	 * @param clazz                       kelas entitas.
	 * @param dataCriteria                kriteria pencarian.
	 * @param buttonLabel                 label tombol.
	 * @param buttonImage                 ikon tombol.
	 * @param columnHeadersAdding         header kolom tambahan pada sheet pertama.
	 * @param dataAdding                  listener pengisi kolom tambahan sheet pertama.
	 * @param adaTambahan                 {@code true} bila ada tab/sheet data tambahan.
	 * @param columnHeadersAddingTambahan header kolom pada sheet tambahan.
	 * @param columns                     kolom entitas pada sheet pertama.
	 * @return konfigurasi tombol ekspor.
	 */
	@SuppressWarnings("rawtypes")
	public static MyToolbarbuttonConfig cetakDataCustomButton(Class clazz, DataCriteria dataCriteria,
			String buttonLabel, String buttonImage, List<String> columnHeadersAdding, EventListener dataAdding,
			Boolean adaTambahan, List<String> columnHeadersAddingTambahan, String... columns) {
		return cetakDataCustomButton(clazz, dataCriteria, buttonLabel, buttonImage, columnHeadersAdding, dataAdding,
				adaTambahan, columnHeadersAddingTambahan, "DATA TAMBAHAN", columns);
	}

	/**
	 * <h3>Buat tombol ekspor Excel dengan tab tambahan bernama kustom</h3>
	 *
	 * <p>Overload dengan {@code namatabTambahan} untuk memberi nama kustom pada sheet
	 * data tambahan (misalnya "RINCIAN BIAYA"). Meneruskan ke overload yang menerima
	 * {@code dataParam} (data pra-komputasi) dengan nilai {@code null}.</p>
	 *
	 * @param clazz                       kelas entitas.
	 * @param dataCriteria                kriteria pencarian.
	 * @param buttonLabel                 label tombol.
	 * @param buttonImage                 ikon tombol.
	 * @param columnHeadersAdding         header kolom tambahan sheet pertama.
	 * @param dataAdding                  listener pengisi kolom tambahan.
	 * @param adaTambahan                 {@code true} bila ada tab data tambahan.
	 * @param columnHeadersAddingTambahan header kolom sheet tambahan.
	 * @param namatabTambahan             nama sheet/tab data tambahan.
	 * @param columns                     kolom entitas sheet pertama.
	 * @return konfigurasi tombol ekspor.
	 */
	@SuppressWarnings("rawtypes")
	public static MyToolbarbuttonConfig cetakDataCustomButton(Class clazz, DataCriteria dataCriteria,
			String buttonLabel, String buttonImage, List<String> columnHeadersAdding, EventListener dataAdding,
			Boolean adaTambahan, List<String> columnHeadersAddingTambahan, String namatabTambahan, String... columns) {
		return cetakDataCustomButton(clazz, dataCriteria, null, buttonLabel, buttonImage, columnHeadersAdding,
				dataAdding, adaTambahan, columnHeadersAddingTambahan, namatabTambahan, columns);
	}

	/**
	 * <h3>Buat tombol ekspor Excel dengan data pra-komputasi (dataParam)</h3>
	 *
	 * <p><b>Tujuan.</b> Overload yang menerima {@code dataParam} — list data yang sudah
	 * dihitung di luar (misal dari query agregasi atau perhitungan khusus) — untuk
	 * disertakan dalam file Excel tanpa perlu mengambil ulang via {@code dataCriteria}.
	 * Mengadaptasi {@code DataCriteria} menjadi {@link ais.common.DataCriteriaWithColumn}
	 * agar kompatibel dengan overload akhir.</p>
	 *
	 * @param clazz                       kelas entitas.
	 * @param dataCriteria                kriteria pencarian (dibungkus menjadi DataCriteriaWithColumn).
	 * @param dataParam                   list data pra-komputasi yang disertakan dalam Excel.
	 * @param buttonLabel                 label tombol.
	 * @param buttonImage                 ikon tombol.
	 * @param columnHeadersAdding         header kolom tambahan.
	 * @param dataAdding                  listener pengisi kolom tambahan.
	 * @param adaTambahan                 {@code true} bila ada tab data tambahan.
	 * @param columnHeadersAddingTambahan header sheet tambahan.
	 * @param namatabTambahan             nama sheet tambahan.
	 * @param columns                     kolom entitas.
	 * @return konfigurasi tombol ekspor.
	 */
	@SuppressWarnings("rawtypes")
	public static MyToolbarbuttonConfig cetakDataCustomButton(Class clazz, final DataCriteria dataCriteria,
			List dataParam, String buttonLabel, String buttonImage, List<String> columnHeadersAdding,
			final EventListener dataAdding, final Boolean adaTambahan, List<String> columnHeadersAddingTambahan,
			String namatabTambahan, String... columns) {
		return cetakDataCustomButton(clazz, new DataCriteriaWithColumn() {

			@Override
			public Object[] initCriteria(boolean order) {
				Object d = dataCriteria == null ? null : dataCriteria.initCriteria(order);
				return new Object[] { d };
			}
		}, dataParam, buttonLabel, buttonImage, columnHeadersAdding, dataAdding, adaTambahan,
				columnHeadersAddingTambahan, namatabTambahan, columns);
	}

	/**
	 * <h3>Buat tombol ekspor Excel — overload paling lengkap (delegasi ke CommonDownloadUpload)</h3>
	 *
	 * <p><b>Tujuan.</b> Titik akhir semua overload {@code cetakDataCustomButton}.
	 * Semua parameter ekspor dikumpulkan di sini dan diteruskan ke
	 * {@link ais.common.CommonDownloadUpload#cetakDataCustomButton} yang bertanggung
	 * jawab atas seluruh logika: membangun Excel di latar via thread terpisah, menampilkan
	 * pratinjau grid ringan (via {@link #displayXlsx}), dan menyediakan tombol unduh
	 * file asli.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Semua 187+ pemanggil di seluruh action akhirnya bermuara
	 * di sini. Perubahan perilaku ekspor (misal format cell, sheet name) cukup dilakukan
	 * di {@code CommonDownloadUpload}.</p>
	 *
	 * @param clazz                       kelas entitas Hibernate.
	 * @param dataCriteria                kriteria dengan kolom kustom (DataCriteriaWithColumn).
	 * @param dataParam                   data pra-komputasi tambahan; boleh {@code null}.
	 * @param buttonLabel                 teks label tombol.
	 * @param buttonImage                 path ikon tombol.
	 * @param columnHeadersAdding         header kolom tambahan di sheet utama; boleh {@code null}.
	 * @param dataAdding                  listener pengisi nilai kolom tambahan; boleh {@code null}.
	 * @param adaTambahan                 {@code true} bila ada sheet data tambahan.
	 * @param columnHeadersAddingTambahan header kolom sheet tambahan; boleh {@code null}.
	 * @param namatabTambahan             nama sheet tambahan.
	 * @param columnsTemporary            nama properti kolom entitas.
	 * @return konfigurasi tombol ekspor siap dipasang ke toolbar.
	 */
	@SuppressWarnings("rawtypes")
	public static MyToolbarbuttonConfig cetakDataCustomButton(Class clazz, DataCriteriaWithColumn dataCriteria,
			List dataParam, String buttonLabel, String buttonImage, List<String> columnHeadersAdding,
			EventListener dataAdding, Boolean adaTambahan, List<String> columnHeadersAddingTambahan,
			String namatabTambahan, String... columnsTemporary) {
		return CommonDownloadUpload.cetakDataCustomButton(clazz, dataCriteria, dataParam, buttonLabel, buttonImage,
				columnHeadersAdding, dataAdding, adaTambahan, columnHeadersAddingTambahan, namatabTambahan,
				columnsTemporary);
	}

	/**
	 * <h3>Pratinjau hasil ekspor: tabel/grid RINGAN dulu, Excel hanya saat Download</h3>
	 *
	 * <p>Titik tunggal tempat seluruh ekspor data (lihat {@code cetakData}/
	 * {@code cetakDataCustomButton} dan {@code CommonDownloadUpload.cetakDataCustomButton})
	 * menampilkan hasil setelah file .xlsx selesai dibangun di latar. Implementasi
	 * dipindahkan ke {@link ais.ui.util.PratinjauXlsxHelper#tampilkanXlsxRingan} agar dapat
	 * dipakai ulang dan konsisten dengan jalur laporan lain: pratinjau ditampilkan sebagai
	 * Grid RINGAN (dibaca via POI), Excel hanya muncul saat tombol Download diklik. Build
	 * Excel tidak disentuh.</p>
	 *
	 * @param fn           path absolut file .xlsx yang sudah dibangun (server-side temp).
	 * @param intbox       jumlah baris data (info/estimasi).
	 * @param noUrutColumn jumlah kolom (fallback bila sheet kosong).
	 */
	public static void displayXlsx(String fn, Intbox intbox, int noUrutColumn) throws Exception {
		ais.ui.util.PratinjauXlsxHelper.tampilkanXlsxRingan(fn, intbox, noUrutColumn);
	}

	/**
	 * <h3>Buat/perbarui entitas Nilai berdasarkan format penilaian dan jumlah skor</h3>
	 *
	 * <p><b>Tujuan.</b> Menyimpan nilai mahasiswa untuk suatu detail perkuliahan sesuai
	 * format penilaian yang berlaku (misal: format A-E, 0-100, atau bobot khusus program).
	 * Dipanggil setiap kali dosen mengentri atau memperbarui nilai.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonAcademicSyncHelper#createNilai} yang menentukan
	 * grade/huruf mutu berdasarkan {@code jumlah} dan aturan di {@code formatNilai},
	 * lalu menyimpan atau memperbarui entitas Nilai di database.</p>
	 *
	 * @param formatNilai        format penilaian yang berlaku (rentang, bobot, huruf mutu).
	 * @param detailperkuliahan  detail perkuliahan (mahasiswa × matakuliah × semester).
	 * @param jumlah             skor total yang diterima mahasiswa.
	 */
	public static void createNilai(FormatNilai formatNilai, Detailperkuliahan detailperkuliahan, Double jumlah) {
		CommonAcademicSyncHelper.createNilai(formatNilai, detailperkuliahan, jumlah);
	}

	/**
	 * <h3>Periksa dan paksa ganti password bila masih menggunakan password default</h3>
	 *
	 * <p><b>Tujuan.</b> Setelah login, memeriksa apakah pengguna masih menggunakan
	 * password default (= enkripsi DES dari User ID atau NIM). Bila ya, menampilkan
	 * dialog konfirmasi atau peringatan wajib ganti password, bergantung pada konfigurasi
	 * {@code boleh_skip_password_jika_belum_diganti}.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Password default dihitung sebagai {@code DES.encrypt(userId)} atau
	 *       {@code DES.encrypt(nim)} bila pengguna adalah mahasiswa.</li>
	 *   <li>Dibandingkan dengan password tersimpan di database.</li>
	 *   <li>Bila cocok (belum diganti) dan konfigurasi {@code boleh_skip} = AKTIF:
	 *       dialog konfirmasi muncul (OK/Cancel); bila tidak boleh skip: dialog paksa
	 *       (hanya OK) langsung membuka form ganti password.</li>
	 * </ol></p>
	 *
	 * <p><b>Threading.</b> Harus dipanggil di thread ZK event (UI thread) karena
	 * memanipulasi komponen ZK ({@link ais.ui.util.MyMessageboxConfig},
	 * {@link ais.ui.util.MyWindow}).</p>
	 *
	 * @param tbmuser pengguna yang baru saja berhasil login.
	 */
	public static void checkApakahPasswordSudahDiganti(Tbmuser tbmuser) {
		try {

			String lama1 = Common.desEncrypter.get().encrypt(tbmuser.getUserId());
			String lama2 = tbmuser.getUserPassword();

			if (tbmuser.getMahasiswa() != null) {
				lama1 = Common.desEncrypter.get().encrypt(tbmuser.getMahasiswa().getNim());
				lama2 = tbmuser.getMahasiswa().getPass();
			}

			// System.out.println("lama1 = " + lama1 + ", lama2 = " + lama2);

			if (lama1.equals(lama2)) {

				boolean boleh_skip_password_jika_belum_diganti = Common.bolehKonfigurasi("boleh_skip_password_jika_belum_diganti");

				if (boleh_skip_password_jika_belum_diganti) {
					MyMessageboxConfig.show("Password belum Anda ganti, apakah Anda ingin mengubah password sekarang ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											ChangePasswordWindow window = new ChangePasswordWindow(false, false);
											window.setVisible(true);
											window.setParent(
													ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
											window.setHeight("450px");
											window.setWidth("500px");
											window.onModal();

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);

										}

									}

								}
							});
				} else {
					MyMessageboxConfig.show("Password belum Anda ganti, Anda harus mengganti pasword sekarang !",
							"Pertanyaan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									ChangePasswordWindow window = new ChangePasswordWindow(false, false);
									window.setVisible(true);
									window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
									window.setHeight("300px");
									window.setWidth("500px");
									window.onModal();
								}
							});
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * <h3>Periksa dan tawarkan koneksi media sosial bila belum terhubung</h3>
	 *
	 * <p><b>Tujuan.</b> Setelah login, memeriksa apakah pengguna (atau mahasiswanya)
	 * sudah menghubungkan akun ke minimal satu media sosial (Facebook/Google/Twitter/LinkedIn).
	 * Bila belum, dan fitur ini diaktifkan via konfigurasi
	 * {@code tanya_media_sosial_jika_belum_punya}, muncul dialog tawaran.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Periksa konfigurasi {@code tanya_media_sosial_jika_belum_punya}; bila TIDAK
	 *       AKTIF, langsung return {@code true} (lewati pemeriksaan).</li>
	 *   <li>Periksa apakah role pengguna termasuk dalam daftar blokir
	 *       ({@code ConstantValues.grupPenggunaBlok}); bila ya, return {@code true}.</li>
	 *   <li>Periksa keempat field media sosial pada entitas Mahasiswa atau Tbmuser;
	 *       bila semuanya kosong, tampilkan dialog konfirmasi koneksi media sosial.</li>
	 * </ol></p>
	 *
	 * <p><b>Threading.</b> Harus dipanggil di thread ZK event karena menggunakan
	 * {@link ais.ui.util.MyMessageboxConfig} dan {@link ais.ui.util.MyWindow}.</p>
	 *
	 * @param tbmuser pengguna yang baru saja berhasil login.
	 * @return {@code true} bila sudah ada media sosial atau pemeriksaan tidak berlaku;
	 *         {@code false} bila dialog koneksi media sosial sedang ditampilkan.
	 */
	public static boolean checkApakahMediaSosialSudahAda(final Tbmuser tbmuser) {
		try {

			if ((Common.bolehKonfigurasi("tanya_media_sosial_jika_belum_punya"))) {

				try {
					String[] block = ConstantValues.grupPenggunaBlok.trim().split(",");
					Set<String> blockJenisPengguna = new HashSet<String>();
					for (String s : block) {
						if (!s.trim().isEmpty()) {
							blockJenisPengguna.add(s.trim().toLowerCase());
						}
					}

					if (blockJenisPengguna.contains(tbmuser.hakAkses().getRoleId().toLowerCase())) {
						return true;
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				final Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
				if ((mahasiswa != null && mahasiswa.getFacebookId().trim().isEmpty()
						&& mahasiswa.getGoogleId().trim().isEmpty() && mahasiswa.getTwitterId().trim().isEmpty()
						&& mahasiswa.getLinkedinId().trim().isEmpty())
						|| (mahasiswa == null && tbmuser != null && tbmuser.getFacebookId().trim().isEmpty()
								&& tbmuser.getGoogleId().trim().isEmpty() && tbmuser.getTwitterId().trim().isEmpty()
								&& tbmuser.getLinkedinId().trim().isEmpty())) {
					MyMessageboxConfig.show(
							"Anda belum terhubung ke media sosial, untuk mempermudah login ke sistem akademik, sebaiknya Anda memiliki minimal satu media sosial yang terhubung ke sistem akademik, sehingga jika suatu hari Anda lupa username dan password sistem akademik, Anda tetap bisa login ke sistem akademik menggunakan username dan password media sosial yang Anda pilih.\n\nApakah Anda ingin menghubungkan akun sistem akademik ke media sosial Anda sekarang ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										String username = mahasiswa != null ? mahasiswa.getNim() : tbmuser.getUserId();

										final MyWindow window = new MyWindow("Pilih Media Sosial", "none", false);
										window.setParent(
												ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
										window.setHeight("150px");
										window.setWidth("550px");

										Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
										borderlayout.setParent(window);
										Center center = new Center();
										center.setParent(borderlayout);
										ais.ui.util.ZkCompat.setFlex(center, true);

										Common.tampilTambahMediaSosial(center, username);

										South south = new South();
										ais.ui.util.ZkCompat.setFlex(south, true);
										south.setParent(borderlayout);

										Toolbar toolbar = new Toolbar();
										// toolbar.setHeight("25px");
										toolbar.setParent(south);
										MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal",
												"/img/cancel.gif");
										cancel.setTooltiptext("Tutup");
										cancel.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												window.detach();
											}
										});
										cancel.setParent(toolbar);

										window.onModal();

									}

								}
							});
					return false;
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		return true;
	}

	/**
	 * <h3>Lakukan pemeriksaan keamanan sesi dan hak akses halaman saat ini</h3>
	 *
	 * <p><b>Tujuan.</b> Memverifikasi bahwa pengguna yang sedang aktif memiliki hak akses
	 * yang valid untuk halaman/action yang sedang diakses. Bila tidak, diarahkan ke
	 * halaman login atau ditampilkan pesan error akses ditolak.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonSecurityLoginHelper#doCheckSecurity} yang memeriksa sesi
	 * HTTP, token keamanan, dan tabel hak akses role.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Dipanggil dari {@code doAfterCompose} tiap action
	 * yang memerlukan autentikasi. Hindari memanggil ulang bila sudah tercakup
	 * di superclass action.</p>
	 */
	public static void doCheckSecurity() {
		CommonSecurityLoginHelper.doCheckSecurity();
	}

	/**
	 * <h3>Simpan cicilan pembayaran kegiatan dengan nominal penuh (tanpa mencicil) via sesi baru</h3>
	 *
	 * <p><b>Tujuan.</b> Menyimpan satu catatan {@link ais.model.master.pembayaran.CicilanPembayaran}
	 * untuk {@link ais.model.master.kegiatan.Kegiatan} dengan nominal yang ditentukan,
	 * menggunakan sesi Hibernate baru yang dibuka dan ditutup secara internal oleh
	 * {@link ais.common.CommonPaymentHelper}. Kata "TanpaSession" merujuk pada fakta
	 * bahwa pemanggil tidak perlu menyediakan sesi — helper mengelola sesinya sendiri.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Dipakai oleh gateway pembayaran (H2H, virtual account)
	 * yang menerima notifikasi asinkron tanpa konteks sesi ZK aktif.</p>
	 *
	 * @param kegiatan         kegiatan akademik/non-akademik yang dibayar.
	 * @param nominal          jumlah nominal yang dibayarkan.
	 * @param tanggalValidasi  tanggal validasi pembayaran dari bank/gateway.
	 * @param keterangan       keterangan tambahan pembayaran.
	 * @param jenisPembayaran  jenis/metode pembayaran (transfer, VA, dll.).
	 * @param detailBiayas     koleksi rincian biaya yang dilunasi oleh pembayaran ini.
	 */
	public static void simpanCicilanDefaultTanpaSesseion(Kegiatan kegiatan, Double nominal, Date tanggalValidasi,
			String keterangan, JenisPembayaran jenisPembayaran, @SuppressWarnings("rawtypes") Collection detailBiayas) {
		CommonPaymentHelper.simpanCicilanDefaultTanpaSesseion(kegiatan, nominal, tanggalValidasi, keterangan,
				jenisPembayaran, detailBiayas);
	}

	/**
	 * <h3>Simpan cicilan pembayaran kegiatan (tanpa mencicil) menggunakan sesi yang diberikan</h3>
	 *
	 * <p><b>Tujuan.</b> Versi {@link #simpanCicilanDefaultTanpaSesseion(Kegiatan, Double, Date, String, JenisPembayaran, Collection)}
	 * yang menerima {@link org.hibernate.Session} dari luar. Cocok untuk konteks transaksional
	 * di mana pemanggil sudah memegang sesi dan perlu menyimpan cicilan dalam transaksi yang sama.</p>
	 *
	 * @param kegiatan        kegiatan yang dibayar.
	 * @param nominal         jumlah pembayaran.
	 * @param tanggalValidasi tanggal validasi.
	 * @param keterangan      keterangan pembayaran.
	 * @param jenisPembayaran metode pembayaran.
	 * @param detailBiayas    rincian biaya yang dilunasi.
	 * @param session         sesi Hibernate aktif dari pemanggil.
	 * @return entitas {@link ais.model.master.pembayaran.CicilanPembayaran} yang disimpan.
	 */
	public static CicilanPembayaran simpanCicilanTanpaMencicil(Kegiatan kegiatan, Double nominal, Date tanggalValidasi,
			String keterangan, JenisPembayaran jenisPembayaran, @SuppressWarnings("rawtypes") Collection detailBiayas,
			Session session) {
		return CommonPaymentHelper.simpanCicilanTanpaMencicil(kegiatan, nominal, tanggalValidasi, keterangan,
				jenisPembayaran, detailBiayas, session);
	}

	/**
	 * <h3>Simpan cicilan pembayaran kegiatan sementara (KegiatanTemporary) via sesi baru</h3>
	 *
	 * <p><b>Tujuan.</b> Sama dengan
	 * {@link #simpanCicilanDefaultTanpaSesseion(Kegiatan, Double, Date, String, JenisPembayaran, Collection)}
	 * tetapi untuk {@link ais.model.master.kegiatan.KegiatanTemporary} — kegiatan yang
	 * bersifat sementara (PPDB, pendaftaran calon mahasiswa) sebelum dikonfirmasi
	 * menjadi kegiatan resmi.</p>
	 *
	 * @param kegiatanTemporary kegiatan sementara yang dibayar.
	 * @param nominal           nominal pembayaran.
	 * @param tanggalValidasi   tanggal validasi dari bank/gateway.
	 * @param keterangan        keterangan pembayaran.
	 * @param jenisPembayaran   metode pembayaran.
	 * @param detailBiayas      rincian biaya yang dilunasi.
	 */
	public static void simpanCicilanDefaultTanpaSesseion(KegiatanTemporary kegiatanTemporary, Double nominal,
			Date tanggalValidasi, String keterangan, JenisPembayaran jenisPembayaran,
			@SuppressWarnings("rawtypes") Collection detailBiayas) {
		CommonPaymentHelper.simpanCicilanDefaultTanpaSesseion(kegiatanTemporary, nominal, tanggalValidasi, keterangan,
				jenisPembayaran, detailBiayas);
	}

	/**
	 * <h3>Simpan cicilan pembayaran kegiatan sementara menggunakan sesi yang diberikan</h3>
	 *
	 * <p><b>Tujuan.</b> Overload {@link #simpanCicilanTanpaMencicil(Kegiatan, Double, Date, String, JenisPembayaran, Collection, Session)}
	 * untuk {@link ais.model.master.kegiatan.KegiatanTemporary}. Menerima sesi dari luar
	 * agar penyimpanan dapat masuk dalam transaksi yang sama dengan operasi lain.</p>
	 *
	 * @param kegiatanTemporary kegiatan sementara yang dibayar.
	 * @param nominal           nominal pembayaran.
	 * @param tanggalValidasi   tanggal validasi.
	 * @param keterangan        keterangan pembayaran.
	 * @param jenisPembayaran   metode pembayaran.
	 * @param detailBiayas      rincian biaya.
	 * @param session           sesi Hibernate aktif dari pemanggil.
	 * @return entitas cicilan yang disimpan.
	 */
	public static CicilanPembayaran simpanCicilanTanpaMencicil(KegiatanTemporary kegiatanTemporary, Double nominal,
			Date tanggalValidasi, String keterangan, JenisPembayaran jenisPembayaran,
			@SuppressWarnings("rawtypes") Collection detailBiayas, Session session) {
		return CommonPaymentHelper.simpanCicilanTanpaMencicil(kegiatanTemporary, nominal, tanggalValidasi, keterangan,
				jenisPembayaran, detailBiayas, session);
	}

	/**
	 * <h3>Simpan otomatis data Kota baru saat registrasi bila belum ada di database</h3>
	 *
	 * <p><b>Tujuan.</b> Saat pengguna mengetik nama kota secara bebas di form registrasi
	 * (bukan memilih dari dropdown), method ini secara otomatis mencari kota yang cocok
	 * di database (fuzzy: hapus tanda baca, normalize spasi). Bila tidak ditemukan,
	 * kota baru dibuat dan dipilih di combobox sehingga data konsisten tanpa menghambat
	 * proses registrasi.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Cek bahwa propinsi sudah dipilih dan kota belum dipilih dari daftar.</li>
	 *   <li>Sanitasi nama kota: hapus {@code .}, {@code ,}, {@code "}, {@code '}, {@code =},
	 *       {@code ?}, normalkan spasi ganda.</li>
	 *   <li>Cari di tabel {@code Kota} dengan filter propinsi dan ILIKE nama kota.</li>
	 *   <li>Bila tidak ditemukan, buat entitas Kota baru dan simpan.</li>
	 *   <li>Reload combobox kota dan pilih kota yang ditemukan/dibuat.</li>
	 * </ol></p>
	 *
	 * <p><b>Catatan keamanan.</b> Nama kota disanitasi dari karakter berbahaya sebelum
	 * dipakai dalam query SQL literal. Tetap hindari memanggil ini dengan input
	 * yang belum divalidasi dari sumber eksternal.</p>
	 *
	 * @param comboboxKota     combobox kota di form registrasi (input + dropdown).
	 * @param comboboxPropinsi combobox propinsi yang harus sudah terpilih.
	 */
	public static void autoSaveDataKotaPadaSaatRegistrasi(Combobox comboboxKota, Combobox comboboxPropinsi) {
		Propinsi propinsi = (Propinsi) (comboboxPropinsi.getSelectedItem() == null ? null
				: comboboxPropinsi.getSelectedItem().getValue());
		if (propinsi != null && comboboxKota.getSelectedItem() == null && !comboboxKota.getValue().trim().isEmpty()) {
			Session session = HibernateUtil.currentSession();
			String namaKota = comboboxKota.getValue().trim();
			namaKota = org.apache.commons.lang3.StringUtils.replace(namaKota, ".", "");
			namaKota = org.apache.commons.lang3.StringUtils.replace(namaKota, ",", "");
			namaKota = org.apache.commons.lang3.StringUtils.replace(namaKota, "\"", "");
			namaKota = org.apache.commons.lang3.StringUtils.replace(namaKota, "'", "");
			namaKota = org.apache.commons.lang3.StringUtils.replace(namaKota, "=", "");
			namaKota = org.apache.commons.lang3.StringUtils.replace(namaKota, "?", "");
			namaKota = org.apache.commons.lang3.StringUtils.replace(namaKota, "  ", " ");
			namaKota = org.apache.commons.lang3.StringUtils.replace(namaKota, "  ", " ");
			namaKota = org.apache.commons.lang3.StringUtils.replace(namaKota, "  ", " ");
			namaKota = org.apache.commons.lang3.StringUtils.replace(namaKota, "  ", " ");
			Kota kota = (Kota) session.createCriteria(Kota.class)
					.add(Restrictions.sqlRestriction(
							"replace(replace(replace(replace(this_.nama,'.',''),',',''),'  ',' '),'  ',' ') ilike '"
									+ namaKota + "%'"))

					.add(Restrictions.eq("propinsi", propinsi)).setMaxResults(1).uniqueResult();
			if (kota == null) {
				kota = new Kota();
				kota.setNama(comboboxKota.getValue().trim());
				kota.setPropinsi(propinsi);
				session.save(kota);
			}
			// org.zkoss.zul.Comboitem comboitem = new
			// org.zkoss.zul.Comboitem();
			// comboitem.setValue(kota);
			// comboitem.setLabel(kota.getNama());
			// comboboxKota.appendChild(comboitem);

			Common.insertCombo(comboboxKota, "nama", Kota.class,
					Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
							Restrictions.eq("propinsi", propinsi)));
			Common.selectComboItem(comboboxKota, kota);
		}
	}

	/**
	 * <h3>Tambahkan baris keterangan polos ke grid form ZK</h3>
	 *
	 * <p><b>Tujuan.</b> Menyisipkan baris dua kolom ke {@link org.zkoss.zul.Rows} form ZK:
	 * kolom pertama kosong (untuk alignment dengan label field lain), kolom kedua berisi
	 * teks keterangan biasa ({@link org.zkoss.zul.Label} standar).</p>
	 *
	 * <p>Gaya baris (inline style) disalin dari baris pertama yang sudah ada agar konsisten
	 * dengan tampilan form yang sedang dibangun.</p>
	 *
	 * @param rows       container baris form ZK.
	 * @param keterangan teks keterangan yang ditampilkan.
	 * @return baris yang baru ditambahkan.
	 */
	public static Row initKeteranganBiasa(Rows rows, String keterangan) {
		if (rows == null) return null;
		String styled = ambilStyleBarisPertama(rows);
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		if (styled != null) {
			row.setStyle(styled);
		}
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(new Label(keterangan));
		return row;
	}

	/**
	 * <h3>Tambahkan baris keterangan bergaya (MyLabelConfig) ke grid form ZK</h3>
	 *
	 * <p><b>Tujuan.</b> Seperti {@link #initKeteranganBiasa} tetapi kolom kedua memakai
	 * {@link ais.ui.util.MyLabelConfig} yang menerapkan CSS styling khusus aplikasi
	 * (misalnya warna, ukuran font, atau class CSS tertentu). Berguna untuk keterangan
	 * yang perlu tampil lebih menonjol dari teks biasa.</p>
	 *
	 * @param rows       container baris form ZK.
	 * @param keterangan teks keterangan yang ditampilkan.
	 * @return baris yang baru ditambahkan.
	 */
	/**
	 * <h3>Tempelkan keterangan TEPAT DI BAWAH input, dalam sel yang sama</h3>
	 *
	 * <p><b>Masalah yang diselesaikan.</b> {@link #initKeterangan(Rows, String)} menambahkan
	 * keterangan sebagai BARIS TERSENDIRI (kolom pertama kosong). Bila beberapa keterangan
	 * berurutan, atau bila baris input-nya sendiri disembunyikan konfigurasi, keterangan itu
	 * tampil sebagai blok teks yang terlepas dari input yang dijelaskannya -- pengguna tidak
	 * tahu keterangan tersebut milik isian yang mana.</p>
	 *
	 * <p><b>Perbaikan.</b> Sel input pada baris dibungkus Vbox, lalu keterangan ditempel di
	 * bawahnya sehingga label + input + keterangan tampil sebagai SATU paket. Bila baris input
	 * TIDAK ditampilkan (parent null karena disembunyikan konfigurasi), keterangan ikut tidak
	 * ditampilkan -- mencegah keterangan yatim.</p>
	 *
	 * @param row        baris form yang sel terakhirnya berisi input; boleh null (diabaikan).
	 * @param keterangan teks keterangan yang ditempelkan di bawah input.
	 */
	public static void keteranganDalamSel(Row row, String keterangan) {
		try {
			if (row == null || row.getParent() == null || keterangan == null) {
				return;
			}
			java.util.List<?> anak = row.getChildren();
			if (anak == null || anak.isEmpty()) {
				return;
			}
			Object terakhir = anak.get(anak.size() - 1);
			if (!(terakhir instanceof org.zkoss.zk.ui.Component)) {
				return;
			}
			org.zkoss.zk.ui.Component selInput = (org.zkoss.zk.ui.Component) terakhir;
			org.zkoss.zul.Vbox bungkus = new org.zkoss.zul.Vbox();
			bungkus.setSpacing("0px");
			row.insertBefore(bungkus, selInput);
			selInput.setParent(bungkus);
			org.zkoss.zul.Label labelKeterangan = new org.zkoss.zul.Label(keterangan);
			labelKeterangan.setStyle("font-size:10px;color:#6b7280;line-height:1.3;");
			labelKeterangan.setParent(bungkus);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "Common.keteranganDalamSel");
		}
	}

	/**
	 * <h3>Baca nilai konfigurasi sebagai angka desimal secara TOLERAN</h3>
	 *
	 * <p><b>Masalah yang diselesaikan.</b> Nilai konfigurasi diketik manusia. Admin di
	 * Indonesia lazim menulis desimal memakai KOMA (mis. {@code 0,1}), sedangkan
	 * {@link Double#parseDouble(String)} hanya menerima TITIK. Akibatnya parsing melempar
	 * {@code NumberFormatException}; pemanggil yang membungkusnya dengan try/catch lalu
	 * diam-diam memakai nilai bawaan. Dua dampaknya: (1) log dibanjiri stack trace karena
	 * pembacaan konfigurasi terjadi pada tiap request, dan (2) LEBIH BERBAHAYA, nilai yang
	 * dimaksud admin diabaikan tanpa peringatan -- mis. admin mengetik {@code 2,5} tetapi
	 * sistem memakai bawaan {@code 0.1}.</p>
	 *
	 * <p><b>Aturan penerjemahan.</b>
	 * <ul>
	 *   <li>Ada KOMA dan TITIK sekaligus: pemisah desimal adalah yang paling KANAN, sisanya
	 *       dianggap pemisah ribuan. Contoh {@code 1.234,56} dan {@code 1,234.56} sama-sama
	 *       menjadi {@code 1234.56}.</li>
	 *   <li>Hanya KOMA: dianggap pemisah desimal gaya Indonesia ({@code 0,1} -> {@code 0.1}).</li>
	 *   <li>Hanya TITIK: dibiarkan apa adanya supaya perilaku konfigurasi lama yang sudah
	 *       benar TIDAK berubah.</li>
	 * </ul>
	 * Bila tetap gagal, nilai bawaan dikembalikan seperti perilaku lama (tanpa melempar).</p>
	 *
	 * @param nilai  teks nilai konfigurasi; boleh null/kosong.
	 * @param bawaan nilai yang dipakai bila teks kosong atau tidak bisa diartikan.
	 * @return angka hasil pembacaan, atau {@code bawaan}.
	 */
	public static double parseAngkaKonfigurasi(String nilai, double bawaan) {
		if (nilai == null) {
			return bawaan;
		}
		String teks = nilai.trim();
		if (teks.length() == 0) {
			return bawaan;
		}
		try {
			int posTitik = teks.indexOf(".") < 0 ? -1 : teks.lastIndexOf(".");
			int posKoma = teks.indexOf(",") < 0 ? -1 : teks.lastIndexOf(",");
			if (posKoma >= 0 && posTitik >= 0) {
				if (posKoma > posTitik) {
					teks = teks.replace(".", "").replace(",", ".");
				} else {
					teks = teks.replace(",", "");
				}
			} else if (posKoma >= 0) {
				teks = teks.replace(",", ".");
			}
			return Double.parseDouble(teks);
		} catch (Exception e) {
			return bawaan;
		}
	}

	/** Ambil style baris pertama tanpa menganggap Rows selalu sudah berisi Row. */
	private static String ambilStyleBarisPertama(Rows rows) {
		if (rows == null || rows.getChildren() == null || rows.getChildren().isEmpty()) {
			return null;
		}
		Object pertama = rows.getChildren().get(0);
		return pertama instanceof Row ? ((Row) pertama).getStyle() : null;
	}

	public static Row initKeterangan(Rows rows, String keterangan) {
		if (rows == null) return null;
		String styled = ambilStyleBarisPertama(rows);
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		if (styled != null) {
			row.setStyle(styled);
		}

		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(new ais.ui.util.MyLabelConfig(keterangan));
		return row;
	}

	/**
	 * <h3>Tambahkan baris keterangan teks merah tebal ke grid form ZK</h3>
	 *
	 * <p><b>Tujuan.</b> Seperti {@link #initKeterangan} tetapi kolom kedua memakai
	 * {@link ais.ui.util.MyLabelBoldMerahConfig} — label dengan warna merah dan huruf
	 * tebal. Dipakai untuk peringatan penting atau catatan yang harus diperhatikan
	 * pengguna (misalnya "Data tidak dapat diubah setelah disimpan").</p>
	 *
	 * @param rows       container baris form ZK.
	 * @param keterangan teks peringatan/keterangan merah tebal.
	 * @return baris yang baru ditambahkan.
	 */
	public static Row initKeteranganMerah(Rows rows, String keterangan) {
		if (rows == null) return null;
		String styled = ambilStyleBarisPertama(rows);
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		if (styled != null) {
			row.setStyle(styled);
		}

		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(new ais.ui.util.MyLabelBoldMerahConfig(keterangan));
		return row;
	}

	/**
	 * <h3>Tambahkan baris keterangan HTML ke grid form ZK</h3>
	 *
	 * <p><b>Tujuan.</b> Seperti {@link #initKeterangan} tetapi kolom kedua memakai
	 * {@link ais.ui.util.MyHtml} — komponen yang me-render konten HTML mentah.
	 * Berguna untuk keterangan yang memerlukan format seperti tautan ({@code <a>}),
	 * teks tebal ({@code <b>}), atau daftar ({@code <ul>/<li>}).</p>
	 *
	 * <p><b>Perhatian keamanan.</b> Pastikan {@code keterangan} sudah di-escape atau
	 * berasal dari sumber terpercaya — komponen ini me-render HTML apa adanya.</p>
	 *
	 * @param rows       container baris form ZK.
	 * @param keterangan konten HTML yang akan dirender.
	 * @return baris yang baru ditambahkan.
	 */
	public static Row initKeteranganHtml(Rows rows, String keterangan) {
		if (rows == null) return null;
		String styled = ambilStyleBarisPertama(rows);
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		if (styled != null) {
			row.setStyle(styled);
		}
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(new ais.ui.util.MyHtml(keterangan));
		return row;
	}

	/**
	 * <h3>Tambahkan baris keterangan satu kolom penuh ke grid form ZK</h3>
	 *
	 * <p><b>Tujuan.</b> Seperti {@link #initKeterangan} tetapi teks keterangan mengisi
	 * <em>satu</em> kolom penuh (bukan kolom kedua dari dua kolom). Berguna untuk
	 * keterangan umum di bagian atas atau bawah form yang tidak perlu sejajar dengan
	 * label field.</p>
	 *
	 * @param rows       container baris form ZK.
	 * @param keterangan teks keterangan yang ditampilkan.
	 * @return baris yang baru ditambahkan.
	 */
	public static Row initKeteranganSatuKolom(Rows rows, String keterangan) {
		if (rows == null) return null;
		String styled = ambilStyleBarisPertama(rows);
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		if (styled != null) {
			row.setStyle(styled);
		}

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(keterangan));
		return row;
	}

	/**
	 * <h3>Tambahkan separator horizontal, checkbox, dan garis penutup ke grid form ZK</h3>
	 *
	 * <p><b>Tujuan.</b> Menyisipkan tiga baris ke {@link org.zkoss.zul.Rows}:
	 * <ol>
	 *   <li>Baris dengan elemen {@code <hr>} sebagai garis pemisah atas.</li>
	 *   <li>Baris dengan {@link ais.ui.util.MyCheckboxConfig} berlabel {@code keterangan}
	 *       yang membentang dua kolom (colspan 2).</li>
	 *   <li>Baris dengan {@code <hr>} sebagai garis pemisah bawah.</li>
	 * </ol>
	 * Pola ini dipakai di form registrasi/persetujuan untuk menampilkan pernyataan
	 * yang harus dicentang pengguna (misalnya persetujuan syarat dan ketentuan).</p>
	 *
	 * @param rows       container baris form ZK.
	 * @param keterangan teks pernyataan untuk checkbox.
	 * @return komponen {@link ais.ui.util.MyCheckboxConfig} yang ditambahkan.
	 */
	@SuppressWarnings("deprecation")
	public static MyCheckboxConfig tambahKeteranganRowHtml(Rows rows, String keterangan) {
		if (rows == null) return null;
		String styled = ambilStyleBarisPertama(rows);
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		if (styled != null) {
			row.setStyle(styled);
		}

		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new ais.ui.util.MyHtml("<hr>"));

		MyCheckboxConfig checkbox = new MyCheckboxConfig(keterangan);

		row = new MyFormRow();
		if (styled != null) {
			row.setStyle(styled);
		}

		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(checkbox);

		row = new MyFormRow();
		if (styled != null) {
			row.setStyle(styled);
		}

		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new ais.ui.util.MyHtml("<hr>"));
		return checkbox;
	}

	/**
	 * <h3>Potong string panjang menjadi 25 karakter dengan elipsis</h3>
	 *
	 * <p><b>Tujuan.</b> Memotong teks panjang agar muat di label grid/tabel yang sempit.
	 * Teks yang melebihi 25 karakter dipotong dan diakhiri {@code "..."}.
	 * Memanggil {@link #simpleString(String, int)} dengan {@code size=25}.</p>
	 *
	 * @param nama string asli; {@code null} diperlakukan sebagai string kosong.
	 * @return string dipotong dengan elipsis bila perlu, atau string asli bila &le;25 karakter.
	 */
	public static String simpleString(String nama) {
		return simpleString(nama, 25);
	}

	/**
	 * <h3>Potong string panjang menjadi N karakter dengan elipsis</h3>
	 *
	 * <p><b>Tujuan.</b> Memotong teks panjang agar muat di ruang tampilan terbatas.
	 * Teks yang melebihi {@code size} karakter dipotong dan diakhiri {@code "..."}.
	 * {@code null} aman — diperlakukan sebagai string kosong.</p>
	 *
	 * @param nama string asli; {@code null} → dikonversi ke string kosong.
	 * @param size panjang maksimal sebelum dipotong (dalam jumlah karakter).
	 * @return string asli bila &le;{@code size} karakter, atau
	 *         substring 0..{@code size} + {@code "..."} bila lebih panjang.
	 */
	public static String simpleString(String nama, int size) {
		if (nama == null) {
			nama = "";
		}
		String nn = nama.length() > size ? nama.substring(0, size) + "..." : nama;
		return nn;
	}

	/** Cache statis daftar {@link ais.model.master.akademik.RencanaTahunAkademik}; diperbarui via {@link #reloadRencanaTahunAkademik}. */
	public static List<RencanaTahunAkademik> rencanaTahunAkademiks = new ArrayList<RencanaTahunAkademik>();

	/**
	 * <h3>Muat ulang cache rencana tahun akademik dari database</h3>
	 *
	 * <p><b>Tujuan.</b> Memperbarui field statis {@link #rencanaTahunAkademiks} dengan
	 * data terkini dari tabel {@code RencanaTahunAkademik}. Method ini dipanggil setelah
	 * ada perubahan data (tambah/ubah/hapus rencana TA) agar komponen yang membaca
	 * field statis mendapatkan data yang mutakhir.</p>
	 *
	 * <p><b>Threading.</b> Field statis tidak thread-safe — bila dipanggil dari thread
	 * bersamaan, bisa terjadi momen singkat di mana {@code rencanaTahunAkademiks=null}.
	 * Pada praktiknya, ini hanya dipanggil dari event UI ZK yang berjalan serial.</p>
	 *
	 * <p><b>Urutan.</b> Hasil diurutkan berdasarkan nama → fakultas → jurusan → semester.</p>
	 *
	 * @param session sesi Hibernate aktif untuk menjalankan query.
	 */
	@SuppressWarnings("unchecked")
	public static void reloadRencanaTahunAkademik(Session session) {
		rencanaTahunAkademiks = null;
		rencanaTahunAkademiks = session.createCriteria(RencanaTahunAkademik.class).addOrder(Order.asc("nama"))
				.addOrder(Order.asc("fakultas")).addOrder(Order.asc("jurusan")).addOrder(Order.asc("semester")).list();
	}

	/**
	 * <h3>Periksa apakah nilai dianggap "null string" (kosong/null/angka negatif)</h3>
	 *
	 * <p><b>Tujuan.</b> Menentukan apakah suatu nilai objek dianggap tidak terisi secara
	 * semantik, mencakup kasus: {@code null}, string kosong/blank, string literal
	 * {@code "null"}, angka negatif, dsb. Berguna dalam validasi form dan
	 * {@link #copyDataJikaKosong}.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonValidationHelper#checkIsStringNull}.</p>
	 *
	 * @param value nilai yang diperiksa; boleh berupa String, Number, atau tipe lain.
	 * @return {@code true} bila nilai dianggap "null/kosong"; {@code false} bila ada isi.
	 */
	public static boolean checkIsStringNull(Object value) {
		return CommonValidationHelper.checkIsStringNull(value);
	}

	/**
	 * <h3>Periksa apakah properti suatu entitas Hibernate bernilai null atau kosong</h3>
	 *
	 * <p><b>Tujuan.</b> Memeriksa nilai properti tertentu dari suatu objek entitas
	 * secara dinamis via metadata Hibernate, tanpa perlu memanggil getter secara eksplisit.
	 * Berguna untuk validasi generik pada field yang diketahui nama propertinya saja
	 * (misal dari konfigurasi atau refleksi).</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Ambil {@link org.hibernate.metadata.ClassMetadata} untuk {@code clazz}.</li>
	 *   <li>Baca nilai properti via {@code getPropertyValue} dalam mode POJO.</li>
	 *   <li>Return {@code true} bila nilai null atau string kosong.</li>
	 * </ol>
	 * Bila terjadi exception (properti tidak ada di metadata), return {@code false}
	 * secara diam-diam.</p>
	 *
	 * @param clazz    kelas entitas Hibernate.
	 * @param o        instance entitas yang diperiksa.
	 * @param property nama properti Hibernate yang ingin diperiksa.
	 * @return {@code true} bila properti null atau string kosong; {@code false} sebaliknya.
	 */
	@SuppressWarnings("rawtypes")
	public static boolean checkIsNull(Class clazz, Object o, String property) {
		if (clazz == null || o == null || property == null || property.trim().length() == 0) {
			return true;
		}
		if (!clazz.isInstance(o)) {
			return true;
		}
		try {
			ClassMetadata classMetadata = HibernateUtil.getClassMetadata(clazz);
			if (classMetadata == null) {
				return false;
			}
			Object value = classMetadata.getPropertyValue(o, property, EntityMode.POJO);
			if (value == null) {
				return true;
			}

			if ((value instanceof String) && Common.checkIsStringNull(value)) {
				return true;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:9733");
//			Common.tampilErrorJikaAdmin(e);
		}
		return false;
	}

	/**
	 * <h3>Validasi format alamat email</h3>
	 *
	 * <p><b>Tujuan.</b> Memeriksa apakah string yang diberikan merupakan alamat email
	 * yang valid secara sintaksis (format {@code user@domain.tld}). Dipakai di form
	 * registrasi, reset password, dan pengiriman notifikasi email.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonValidationHelper#isValidEmailAddress} yang menggunakan
	 * regex standar validasi email.</p>
	 *
	 * @param email string alamat email yang akan divalidasi; boleh {@code null}.
	 * @return {@code true} bila format email valid; {@code false} bila tidak valid atau null.
	 */
	public static boolean isValidEmailAddress(String email) {
		return CommonValidationHelper.isValidEmailAddress(email);
	}

	/**
	 * <h3>Simpan/perbarui UserAccess dan UserRole untuk SSO internal (Spring Security)</h3>
	 *
	 * <p><b>Tujuan.</b> Menyinkronkan data pengguna ke tabel {@code UserAccess} dan
	 * {@code UserRole} yang dipakai oleh sistem Spring Security internal (terpisah dari
	 * autentikasi utama ZK). Dipanggil saat pengguna baru dibuat atau password diubah
	 * agar sesi SSO juga diperbarui.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Cari {@code UserAccess} berdasarkan {@code username}; buat baru bila tidak ada.</li>
	 *   <li>Set email (hanya alamat pertama bila ada koma), aktifkan akun, hash password
	 *       dengan MD5.</li>
	 *   <li>Tentukan role: mahasiswa → {@code ROLE_MAHASISWA}; dosen aktif →
	 *       {@code ROLE_DOSEN}.</li>
	 *   <li>Pastikan {@code UserRole} (relasi UserAccess–RoleAccess) ada; buat bila belum.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan.</b> Bagian integrasi OJS (Journal) sudah di-comment-out dan
	 * tidak aktif. Bila OJS diaktifkan kembali, uncomment blok tersebut dan pastikan
	 * {@code OjsHibernateUtil} tersedia.</p>
	 *
	 * @param tbmuser   pengguna sistem yang diperbarui; boleh {@code null} bila hanya mahasiswa.
	 * @param mahasiswa entitas mahasiswa; boleh {@code null} bila bukan mahasiswa.
	 * @param username  username untuk UserAccess.
	 * @param password  password plaintext yang akan di-hash MD5.
	 * @param email     alamat email (diambil bagian pertama bila ada koma).
	 */
	public static void saveOrUpdateUserAccess(Tbmuser tbmuser, Mahasiswa mahasiswa, String username, String password,
			String email) {
		// openSession() DEDIKASI + transaksi eksplisit, lalu ditutup di finally (clear/disconnect/
		// close) agar koneksi TIDAK bocor DAN simpan benar-benar ter-commit (sesi native/ZK tanpa
		// transaksi tidak menuntaskan save). createCriteria pada sesi dedikasi tak butuh tx ThreadLocal.
		Session session = null;
		org.hibernate.Transaction txUA = null;
		try {
			session = HibernateUtil.openSession();
			txUA = session.beginTransaction();

			UserAccess userAccess = (UserAccess) session.createCriteria(UserAccess.class)
					.add(Restrictions.eq("username", username)).setMaxResults(1).uniqueResult();
			if (userAccess == null) {
				userAccess = new UserAccess();
				userAccess.setUsername(username);
			}

			userAccess.setEmail(email == null || email.trim().isEmpty() ? "" : email.trim().split(",")[0].trim());
			userAccess.setEnabled(true);
			userAccess.setFirstName(username);

			userAccess.setPassword(MD5.crypt(password.trim()));
			Common.refreshSaveOrUpdate(session, userAccess);

			RoleAccess roleAccess = null;

			if (mahasiswa != null) {
				roleAccess = (RoleAccess) session.createCriteria(RoleAccess.class)
						.add(Restrictions.eq("authority", "ROLE_MAHASISWA")).setMaxResults(1).uniqueResult();
				if (roleAccess == null) {
					roleAccess = new RoleAccess();
					roleAccess.setAuthority("ROLE_MAHASISWA");
					Common.refreshSaveOrUpdate(session, roleAccess);
				}
				UserRole userRole = (UserRole) session.createCriteria(UserRole.class)
						.add(Restrictions.eq("userAccess", userAccess)).add(Restrictions.eq("roleAccess", roleAccess))
						.setMaxResults(1).uniqueResult();
				if (userRole == null) {
					userRole = new UserRole();
					userRole.setUserAccess(userAccess);
					userRole.setRoleAccess(roleAccess);
					Common.refreshSaveOrUpdate(session, userRole);
				}
			} else if (tbmuser != null && tbmuser.ambilDosen() != null
					&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
				roleAccess = (RoleAccess) session.createCriteria(RoleAccess.class)
						.add(Restrictions.eq("authority", "ROLE_DOSEN")).setMaxResults(1).uniqueResult();
				if (roleAccess == null) {
					roleAccess = new RoleAccess();
					roleAccess.setAuthority("ROLE_DOSEN");
					Common.refreshSaveOrUpdate(session, roleAccess);
				}

			}

			if (roleAccess != null) {
				UserRole userRole = (UserRole) session.createCriteria(UserRole.class)
						.add(Restrictions.eq("userAccess", userAccess)).add(Restrictions.eq("roleAccess", roleAccess))
						.setMaxResults(1).uniqueResult();
				if (userRole == null) {
					userRole = new UserRole();
					userRole.setUserAccess(userAccess);
					userRole.setRoleAccess(roleAccess);
					Common.refreshSaveOrUpdate(session, userRole);
				}
			}

			txUA.commit();
		} catch (Exception e) {
			if (txUA != null) {
				try { txUA.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/Common.java:9856");}
			}
			tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/Common.java:9861");}
				try { session.disconnect(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/Common.java:9862");}
				try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/Common.java:9863");}
			}
		}
//		if (Common.getKonfigurasi("terhubung_ke_ojs", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF)) {
//			if (tbmuser != null && tbmuser.getUsernameOjs() != null) {
//				try {
//					Session ojSession = OjsHibernateUtil.getInstance().currentSession();
//					List<Journals> journals = ojSession.createCriteria(Journals.class).list();
//					TbmuserAction.updateUser(ojSession, tbmuser, journals);
//					OjsHibernateUtil.getInstance().closeSession();
//				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:9873");
//					tampilErrorJikaAdmin(e);
//				}
//			}
//
//			if (mahasiswa != null) {
//				try {
//					Session ojSession = OjsHibernateUtil.getInstance().currentSession();
//					List<Journals> journals = ojSession.createCriteria(Journals.class).list();
//					MahasiswaAction.updateUser(ojSession, mahasiswa, journals);
//					OjsHibernateUtil.getInstance().closeSession();
//				} catch (Exception e) {
//					tampilErrorJikaAdmin(e);
//				}
//			}
//		}

	}

	/**
	 * <h3>Buat baris field "Kota" di form registrasi/profil</h3>
	 *
	 * <p><b>Tujuan.</b> Menambahkan baris form yang menampilkan field kota dalam mode
	 * read-write: komponen {@link org.zkoss.zul.Label} editable yang menampilkan nama kota.
	 * Bila data kota sudah ada, nama ditampilkan dan entitas {@code Kota} disimpan sebagai
	 * atribut {@code "wilayah"} pada label untuk penggunaan selanjutnya.</p>
	 *
	 * <p><b>Cara kerja.</b> Baris dua kolom ditambahkan: kolom pertama label judul
	 * ({@code label}), kolom kedua adalah komponen kota ({@code Label editable})
	 * dengan lebar 90%. Nilai awal diisi dari {@code dataKota.getNama()}.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Blok kode yang menggunakan Combobox (mode dengan dropdown
	 * pilih kota) sudah di-comment-out. Saat ini field kota adalah Label editable
	 * (teks bebas dengan simpan otomatis via {@link #autoSaveDataKotaPadaSaatRegistrasi}).</p>
	 *
	 * @param rows     container baris form ZK.
	 * @param label    teks label judul field kota (misalnya "Kota/Kabupaten").
	 * @param kota     komponen Label ZK yang menjadi input nama kota.
	 * @param propinsi komponen Label ZK propinsi (untuk referensi konteks).
	 * @param dataKota entitas Kota default; boleh {@code null}.
	 * @param tampil   {@code true} bila baris ditampilkan; {@code false} bila disembunyikan.
	 * @return baris yang baru ditambahkan.
	 */
	public static Row createFieldKota(Rows rows, final String label, final Label kota, final Label propinsi,
			Kota dataKota, Boolean tampil) {
		if (rows == null || kota == null) return null;
		String styled = ambilStyleBarisPertama(rows);
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		if (styled != null) {
			row.setStyle(styled);
		}

//		if (Common.getKonfigurasi("pengguna_bisa_menambah_data_kota_kabupaten", Konfigurasi.AKTIF).getNilai()
//				.equals(Konfigurasi.AKTIF)) {
//			row.setVisible(tampil);
//			row.setParent(rows);
//			row.appendChild(new ais.ui.util.MyLabelConfig(label));
//			Common.selectComboItem(kota, dataKota);
//			row.appendChild(kota);
//			kota.setWidth("90%");
//			kota.addEventListener("onChange", new EventListener() {
//
//				@Override
//				public void onEvent(Event arg0) throws Exception {
//					Common.autoSaveDataKotaPadaSaatRegistrasi(kota, propinsi);
//				}
//			});
//			return row;
//		} else {
		row.setVisible(tampil);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(label));
//			Common.selectComboItem(kota, dataKota);
		kota.setValue(dataKota == null ? "" : dataKota.getNama());
		row.appendChild(kota);
		kota.setWidth("90%");
		if (dataKota != null) {
			kota.setAttribute("wilayah", dataKota);
		}
//		kota.setReadonly(true);
		return row;
//		}
	}

	/**
	 * <h3>Buat listener yang mengisi Kota dan Propinsi otomatis dari pilihan Kecamatan</h3>
	 *
	 * <p><b>Tujuan.</b> Saat pengguna memilih kecamatan di form registrasi/profil,
	 * method ini mengisi field kota dan propinsi secara otomatis berdasarkan hierarki
	 * wilayah (Kecamatan → Kota/Kabupaten → Propinsi). Mengurangi kemungkinan salah
	 * ketik dan mempercepat pengisian form.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Ambil entitas {@code Wilayah} kecamatan dari atribut komponen.</li>
	 *   <li>Naik hierarki: kecamatan → wilayah induk (kab/kota) → wilayah induk lagi (propinsi).</li>
	 *   <li>Cari propinsi di DB menggunakan Levenshtein distance ≤ 1 (fuzzy match, toleran
	 *       perbedaan ejaan seperti "Prop." di awal nama); bila tidak ada, buat propinsi baru.</li>
	 *   <li>Cari kota di DB dengan ILIKE exact; bila tidak ada, buat kota baru.</li>
	 *   <li>Isi label propinsi dan kota, serta simpan entitas terpilih sebagai atribut
	 *       {@code "wilayah"} pada masing-masing komponen.</li>
	 * </ol></p>
	 *
	 * <p><b>Pemeliharaan.</b> Listener juga langsung dipanggil satu kali ({@code onEvent(null)})
	 * setelah dibuat agar nilai terisi saat form pertama kali dibuka bila kecamatan sudah terpilih.</p>
	 *
	 * @param propinsi  komponen Label ZK untuk menampilkan nama propinsi yang terisi otomatis.
	 * @param kota      komponen Label ZK untuk menampilkan nama kota yang terisi otomatis.
	 * @param kecamatan komponen bandbox kecamatan yang memicu pengisian otomatis.
	 * @return {@link org.zkoss.zk.ui.event.EventListener} yang sudah terpasang ke {@code kecamatan}.
	 * @throws Exception bila terjadi kesalahan akses database.
	 */
	public static EventListener createKotaPropinsiListenerBerdasarkanKecamatan(final Label propinsi, final Label kota,
			final AmbilDataKecamatanBanbox kecamatan) throws Exception {
		EventListener kecamatanEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Wilayah wilayah = (Wilayah) kecamatan.getAttribute("wilayah");
				// System.out.println("wilayah kec. => " + wilayah);
				if (wilayah != null) {
					Session session = HibernateUtil.currentSession();
					Wilayah wilayahKab = wilayah.getWilayahInduk();
					// System.out.println("wilayah kab/kota => " + wilayahKab);
					if (wilayahKab != null) {
						Wilayah wilayahProp = wilayahKab.getWilayahInduk();
						// System.out.println("wilayah Prop => " + wilayahProp);
						String namaProp = wilayahProp == null ? "" : wilayahProp.getNama();
						Propinsi selectedPropinsi = null;

						if (namaProp != null && !namaProp.trim().isEmpty()) {
							namaProp = org.apache.commons.lang3.StringUtils.replace(namaProp, "Prop.", "");
							namaProp = namaProp.trim();
							List<Propinsi> propinsis = ConstantValues.simpleList(session.createCriteria(Propinsi.class)
									.add(Restrictions.ne("nama", "")).add(Restrictions.isNotNull("nama")),
									Propinsi.class);
							TreeMap<Integer, Propinsi> treeMap = new TreeMap<Integer, Propinsi>();
							for (Propinsi propinsi : propinsis) {
								String nama = propinsi.getNama();
								nama = org.apache.commons.lang3.StringUtils.replace(nama, "Prop.", "");
								nama = nama.trim();
								treeMap.put(
										StringUtils.getLevenshteinDistance(nama.toLowerCase(), namaProp.toLowerCase()),
										propinsi);
							}
							int firstKey = treeMap.isEmpty() ? 0 : treeMap.firstKey();
							if (!treeMap.isEmpty() && firstKey < 2) {
								selectedPropinsi = treeMap.get(firstKey);

								propinsi.setValue(selectedPropinsi == null ? "" : selectedPropinsi.getNama());

							} else {
								selectedPropinsi = new Propinsi();
								selectedPropinsi.setNama(namaProp);
								selectedPropinsi.setNegara(ConstantValues.INDONESIA);
								session.save(selectedPropinsi);

								propinsi.setValue(selectedPropinsi == null ? "" : selectedPropinsi.getNama());
							}
						}

						propinsi.setAttribute("wilayah", selectedPropinsi);

						if (selectedPropinsi != null) {
//							List<Kota> kotas = session.createCriteria(Kota.class)
//									.add(Restrictions.eq("propinsi", selectedPropinsi)).add(Restrictions.ne("nama", ""))
//									.add(Restrictions.isNotNull("nama")).list();
//							Common.insertComboItems(kota, "nama", kotas);
							String namaKab = wilayahKab.getNama().trim();

							Kota selectedKota = (Kota) ConstantValues.simpleObject(
									session.createCriteria(Kota.class)
											.add(Restrictions.eq("propinsi", selectedPropinsi))
											.add(Restrictions.ilike("nama", namaKab, MatchMode.EXACT)).setMaxResults(1),
									Kota.class);
							if (selectedKota == null) {
								selectedKota = new Kota();
								selectedKota.setNama(namaKab);
								selectedKota.setPropinsi(selectedPropinsi);
								session.save(selectedKota);
							}

							kota.setValue(selectedKota == null ? "" : selectedKota.getNama());
							kota.setAttribute("wilayah", selectedKota);

//							Common.selectComboItem(true, kota, selectedKota);

//							kota.setDisabled(true);
						}

					}
				}
			}
		};

		kecamatan.setEventListener(kecamatanEventListener);

		kecamatanEventListener.onEvent(null);
		return kecamatanEventListener;
	}

	/**
	 * <h3>Ambil ekstensi file dari nama file</h3>
	 *
	 * <p><b>Tujuan.</b> Mengekstrak bagian ekstensi (setelah titik terakhir) dari nama
	 * file, misalnya {@code ".pdf"}, {@code ".jpg"}, {@code ".xlsx"}. Berguna untuk
	 * menentukan tipe MIME dan handler yang sesuai saat memproses file unggahan.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonFileMediaHelper#getFileExtension(File)}.</p>
	 *
	 * @param file file yang ekstensinya ingin diambil.
	 * @return string ekstensi termasuk titik (misal {@code ".pdf"}), atau string kosong
	 *         bila file tidak memiliki ekstensi.
	 */
	public static String getFileExtension(File file) {
		return CommonFileMediaHelper.getFileExtension(file);
	}

	/**
	 * <h3>Tampilkan pratinjau gambar dalam popup modal ZK</h3>
	 *
	 * <p><b>Tujuan.</b> Membuka {@link ais.ui.util.MyWindow} modal yang menampilkan
	 * gambar dari URL tertentu. Lebar popup menyesuaikan mode perangkat:
	 * 90% lebar layar di mobile, 600px di desktop.</p>
	 *
	 * <p><b>Cara kerja.</b> Popup dibuat programatik dengan Borderlayout dan komponen
	 * {@link org.zkoss.zul.Image}; gambar discroll bila lebih besar dari window via
	 * {@link #tampilanScroll1(org.zkoss.zk.ui.Component)}.</p>
	 *
	 * @param url URL gambar yang akan ditampilkan (relatif atau absolut).
	 * @throws Exception bila terjadi kesalahan saat membuat popup ZK.
	 */
	public static void previewGambar(String url) throws Exception {
		final MyWindow window = new MyWindow("Preview Gambar", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("90%");
		window.setWidth(Common.isMobile() ? "90%" : "600px");

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Image image = new Image(url);
		image.setWidth("97%");
		Common.tampilanScroll1(center).appendChild(image);

		window.setVisible(true);
		window.onModal();
	}

	/**
	 * <h3>Tampilkan pratinjau file (PDF/gambar/dokumen) dalam popup modal ZK</h3>
	 *
	 * <p><b>Tujuan.</b> Membuka popup pratinjau untuk file yang diunggah pengguna.
	 * Mendukung berbagai tipe file (PDF via embed, gambar via img, dll.) tergantung
	 * implementasi di helper.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonFileMediaHelper#previewFile(String)}.</p>
	 *
	 * @param url URL file yang akan dipratinjau.
	 * @throws Exception bila terjadi kesalahan saat membuka popup.
	 */
	public static void previewFile(String url) throws Exception {
		CommonFileMediaHelper.previewFile(url);
	}

	/**
	 * <h3>Buat timer ZK tanpa busy indicator (overload minimal)</h3>
	 *
	 * <p><b>Tujuan.</b> Membuat timer ZK yang berjalan di latar tanpa menampilkan
	 * "busy" spinner kepada pengguna. Berguna untuk proses polling atau refresh data
	 * berkala yang tidak boleh mengganggu interaksi pengguna.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke {@link ais.common.CommonTimerHelper#createDefaultTimerNoBusy}.</p>
	 *
	 * @param eventListener listener yang dipanggil setiap timer fire.
	 */
	public static void createDefaultTimerNoBusy(EventListener eventListener) {
		CommonTimerHelper.createDefaultTimerNoBusy(eventListener);
	}

	/**
	 * <h3>Buat timer ZK default dengan busy indicator (overload minimal)</h3>
	 *
	 * <p><b>Tujuan.</b> Membuat timer ZK dengan konfigurasi default yang menampilkan
	 * spinner "busy" saat sedang memproses. Dipakai untuk operasi yang membutuhkan
	 * umpan balik visual kepada pengguna bahwa sistem sedang bekerja.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke {@link ais.common.CommonTimerHelper#createDefaultTimer}.</p>
	 *
	 * @param eventListener listener yang dipanggil saat timer fire.
	 */
	public static void createDefaultTimer(EventListener eventListener) {
		CommonTimerHelper.createDefaultTimer(eventListener);
	}

	/**
	 * <h3>Buat timer ZK default dengan pesan informasi kustom</h3>
	 *
	 * <p>Seperti {@link #createDefaultTimer(EventListener)} dengan tambahan {@code info}
	 * yang ditampilkan sebagai pesan busy.</p>
	 *
	 * @param eventListener listener yang dipanggil saat timer fire.
	 * @param info          pesan yang ditampilkan saat timer berjalan (busy message).
	 */
	public static void createDefaultTimer(EventListener eventListener, String info) {
		CommonTimerHelper.createDefaultTimer(eventListener, info);
	}

	/**
	 * <h3>Buat timer ZK dengan timeout dan busy indicator (overload minimal)</h3>
	 *
	 * <p><b>Tujuan.</b> Membuat timer ZK yang hanya berjalan sekali (timeout) dengan
	 * interval default. Cocok untuk operasi delay seperti auto-redirect atau menunggu
	 * sebelum menutup popup.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke {@link ais.common.CommonTimerHelper#createDefaultTimerTimeout}.</p>
	 *
	 * @param eventListener listener yang dipanggil setelah interval timeout.
	 */
	public static void createDefaultTimerTimeout(EventListener eventListener) {
		CommonTimerHelper.createDefaultTimerTimeout(eventListener);
	}

	/**
	 * <h3>Buat timer ZK timeout dengan pesan kustom</h3>
	 *
	 * <p>Seperti {@link #createDefaultTimerTimeout(EventListener)} dengan tambahan
	 * {@code info} sebagai pesan busy.</p>
	 *
	 * @param eventListener listener yang dipanggil setelah timeout.
	 * @param info          pesan busy yang ditampilkan.
	 */
	public static void createDefaultTimerTimeout(EventListener eventListener, String info) {
		CommonTimerHelper.createDefaultTimerTimeout(eventListener, info);
	}

	/**
	 * <h3>Buat timer ZK dengan konfigurasi penuh (repeat dan interval)</h3>
	 *
	 * <p><b>Tujuan.</b> Versi lengkap timer ZK dengan busy indicator. Memungkinkan
	 * kontrol penuh atas apakah timer berulang ({@code repeat=true}) dan interval
	 * waktu antar fire.</p>
	 *
	 * @param eventListener listener yang dipanggil saat timer fire.
	 * @param info          pesan busy; boleh {@code null} untuk pesan default.
	 * @param repeat        {@code true} = timer berulang; {@code false} = sekali jalan.
	 * @param interfal      interval waktu timer dalam milidetik.
	 */
	public static void createDefaultTimer(final EventListener eventListener, String info, final Boolean repeat,
			final Integer interfal) {
		CommonTimerHelper.createDefaultTimer(eventListener, info, repeat, interfal);
	}

	/**
	 * <h3>Buat timer ZK timeout dengan konfigurasi penuh (repeat dan interval)</h3>
	 *
	 * <p><b>Tujuan.</b> Versi lengkap timer timeout. Bila {@code repeat=false},
	 * timer hanya fire sekali setelah {@code interfal} milidetik.</p>
	 *
	 * @param eventListener listener yang dipanggil saat fire.
	 * @param info          pesan busy; boleh {@code null}.
	 * @param repeat        {@code true} = berulang; {@code false} = sekali.
	 * @param interfal      interval dalam milidetik.
	 */
	public static void createDefaultTimerTimeout(final EventListener eventListener, final String info, Boolean repeat,
			Integer interfal) {
		CommonTimerHelper.createDefaultTimerTimeout(eventListener, info, repeat, interfal);
	}

	/**
	 * <h3>Buat timer ZK tanpa busy indicator dengan konfigurasi penuh</h3>
	 *
	 * <p><b>Tujuan.</b> Versi lengkap timer tanpa busy indicator. Cocok untuk
	 * polling data di latar (misal refresh notifikasi) yang tidak boleh menampilkan
	 * spinner karena berjalan sering atau di latar belakang panel tertentu.</p>
	 *
	 * @param eventListener listener yang dipanggil saat fire.
	 * @param info          informasi internal; boleh {@code null}.
	 * @param repeat        {@code true} = berulang; {@code false} = sekali.
	 * @param interfal      interval dalam milidetik.
	 */
	public static void createDefaultTimerNoBusy(final EventListener eventListener, String info, final Boolean repeat,
			final Integer interfal) {
		CommonTimerHelper.createDefaultTimerNoBusy(eventListener, info, repeat, interfal);
	}

	/**
	 * <h3>Buat komponen unduh/unggah file lampiran mahasiswa di form (versi Rows)</h3>
	 *
	 * <p><b>Tujuan.</b> Menyisipkan baris unduh/unggah file lampiran (seperti scan KTP,
	 * ijazah, foto) ke dalam {@link org.zkoss.zul.Rows} form ZK. Menampilkan tautan
	 * unduh bila lampiran sudah ada, dan tombol unggah untuk menggantinya.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonFileMediaHelper#createDownloadUploadFileLampiran(Rows, Mahasiswa, String, String)}.</p>
	 *
	 * @param rows      container baris form ZK.
	 * @param mahasiswa mahasiswa pemilik lampiran.
	 * @param jenis     jenis lampiran (kode tipe file, misal "ktp", "ijazah").
	 * @param keterangan label yang ditampilkan di sebelah field.
	 */
	public static void createDownloadUploadFileLampiran(Rows rows, Mahasiswa mahasiswa, String jenis,
			String keterangan) {
		CommonFileMediaHelper.createDownloadUploadFileLampiran(rows, mahasiswa, jenis, keterangan);
	}

	/**
	 * <h3>Buat komponen unduh/unggah file lampiran generik di Row yang ada (versi Row+Hbox)</h3>
	 *
	 * <p><b>Tujuan.</b> Overload lebih fleksibel yang menerima {@link org.zkoss.zul.Row}
	 * yang sudah ada dan {@link org.zkoss.zul.Hbox} tempat preview ditampilkan.
	 * Mendukung entitas generik ({@link ais.model.GeneralValueObject}) bukan hanya
	 * Mahasiswa, sehingga bisa dipakai untuk guru, dosen, atau entitas lain.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonFileMediaHelper#createDownloadUploadFileLampiran(Row, Hbox, GeneralValueObject, String, String)}.</p>
	 *
	 * @param row           baris form yang sudah ada tempat komponen ditambahkan.
	 * @param parentPreview Hbox tempat pratinjau file ditampilkan.
	 * @param mahasiswa     entitas pemilik lampiran (Mahasiswa, Guru, Dosen, dll.).
	 * @param jenis         kode jenis lampiran.
	 * @param keterangan    label yang ditampilkan.
	 */
	public static void createDownloadUploadFileLampiran(Row row, Hbox parentPreview, GeneralValueObject mahasiswa,
			String jenis, String keterangan) {
		CommonFileMediaHelper.createDownloadUploadFileLampiran(row, parentPreview, mahasiswa, jenis, keterangan);
	}

	/**
	 * <h3>Buat komponen unduh/unggah foto entitas (versi ref Serializable)</h3>
	 *
	 * <p><b>Tujuan.</b> Menambahkan widget foto ke komponen ZK: menampilkan foto saat ini
	 * (bila ada) dan tombol unggah foto baru. Dipakai untuk foto profil mahasiswa, dosen,
	 * guru, dsb. Tipe foto ditentukan oleh parameter {@code fileFoto} (subkelas
	 * {@link ais.model.master.FileFotoLain}).</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonFileMediaHelper#createDownloadUploadFoto(Component, Serializable, Class, EventListener, boolean)}.</p>
	 *
	 * @param row           komponen induk tempat foto ditambahkan.
	 * @param ref           referensi ID entitas pemilik foto (primary key).
	 * @param fileFoto      kelas entitas foto yang dipakai (subkelas FileFotoLain).
	 * @param eventListener listener yang dipanggil setelah foto berhasil diunggah.
	 * @param tampilUpload  {@code true} bila tombol unggah ditampilkan.
	 * @throws Exception bila terjadi kesalahan saat membangun komponen.
	 */
	public static void createDownloadUploadFoto(Component row, Serializable ref, Class<? extends FileFotoLain> fileFoto,
			EventListener eventListener, boolean tampilUpload) throws Exception {
		CommonFileMediaHelper.createDownloadUploadFoto(row, ref, fileFoto, eventListener, tampilUpload);
	}

	/**
	 * <h3>Buat komponen unduh/unggah foto entitas (versi GeneralValueObject)</h3>
	 *
	 * <p><b>Tujuan.</b> Overload yang menerima {@link ais.model.GeneralValueObject}
	 * secara langsung sebagai pemilik foto, sehingga entitas tidak perlu dikonversi
	 * ke {@link java.io.Serializable} secara manual oleh pemanggil.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonFileMediaHelper#createDownloadUploadFoto(Component, GeneralValueObject, Class, EventListener, boolean)}.</p>
	 *
	 * @param parent                  komponen induk tempat foto ditambahkan.
	 * @param generalValueObject      entitas pemilik foto (Mahasiswa, Guru, Dosen, dll.).
	 * @param fileFoto                kelas entitas foto (subkelas FileFotoLain).
	 * @param eventListenerFotoUpload listener setelah upload berhasil.
	 * @param tampilUpload            {@code true} bila tombol upload ditampilkan.
	 * @throws Exception bila terjadi kesalahan saat membangun komponen.
	 */
	public static void createDownloadUploadFoto(Component parent, GeneralValueObject generalValueObject,
			Class<? extends FileFotoLain> fileFoto, final EventListener eventListenerFotoUpload, boolean tampilUpload)
			throws Exception {
		CommonFileMediaHelper.createDownloadUploadFoto(parent, generalValueObject, fileFoto, eventListenerFotoUpload,
				tampilUpload);
	}

	/**
	 * <h3>Buat komponen unduh/unggah lampiran persyaratan beasiswa</h3>
	 *
	 * <p><b>Tujuan.</b> Menambahkan widget unduh/unggah untuk lampiran persyaratan beasiswa
	 * ({@link ais.model.master.kemahasiswaan.MahasiswaBeasiswaPersyaratan}) ke dalam Hbox.
	 * Masing-masing persyaratan beasiswa bisa memiliki file lampiran berbeda.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonFileMediaHelper#createDownloadUploadFileLampiranBeasiswa}.</p>
	 *
	 * @param row                         Hbox tempat komponen ditambahkan.
	 * @param mahasiswaBeasiswaPersyaratan entitas persyaratan beasiswa mahasiswa.
	 * @param keterangan                  label keterangan lampiran.
	 */
	public static void createDownloadUploadFileLampiranBeasiswa(Hbox row,
			final MahasiswaBeasiswaPersyaratan mahasiswaBeasiswaPersyaratan, final String keterangan) {
		CommonFileMediaHelper.createDownloadUploadFileLampiranBeasiswa(row, mahasiswaBeasiswaPersyaratan, keterangan);
	}

	/**
	 * <h3>Buat komponen unduh/unggah lampiran persyaratan KKN</h3>
	 *
	 * <p><b>Tujuan.</b> Seperti {@link #createDownloadUploadFileLampiranBeasiswa} tetapi
	 * untuk persyaratan Kuliah Kerja Nyata (KKN). Setiap persyaratan KKN dapat dilampiri
	 * file bukti (misalnya surat keterangan sehat, asuransi).</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonFileMediaHelper#createDownloadUploadFileLampiranKkn}.</p>
	 *
	 * @param row                      komponen induk tempat widget ditambahkan.
	 * @param mahasiswaKknPersyaratan  entitas persyaratan KKN.
	 * @param keterangan               label keterangan.
	 */
	public static void createDownloadUploadFileLampiranKkn(Component row,
			final MahasiswaKknPersyaratan mahasiswaKknPersyaratan, final String keterangan) {
		CommonFileMediaHelper.createDownloadUploadFileLampiranKkn(row, mahasiswaKknPersyaratan, keterangan);
	}

	/**
	 * <h3>Buat komponen unduh/unggah lampiran persyaratan PKL</h3>
	 *
	 * <p><b>Tujuan.</b> Seperti {@link #createDownloadUploadFileLampiranKkn} tetapi
	 * untuk Praktik Kerja Lapangan (PKL). Mendukung unggah bukti dokumen persyaratan
	 * PKL (surat magang, laporan, dll.).</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonFileMediaHelper#createDownloadUploadFileLampiranPkl}.</p>
	 *
	 * @param row                     komponen induk tempat widget ditambahkan.
	 * @param mahasiswaPklPersyaratan entitas persyaratan PKL.
	 * @param keterangan              label keterangan.
	 */
	public static void createDownloadUploadFileLampiranPkl(Component row,
			final MahasiswaPklPersyaratan mahasiswaPklPersyaratan, final String keterangan) {
		CommonFileMediaHelper.createDownloadUploadFileLampiranPkl(row, mahasiswaPklPersyaratan, keterangan);
	}

	/**
	 * <h3>Ambil string ukuran maksimal file unggah dari konfigurasi (format label)</h3>
	 *
	 * <p><b>Tujuan.</b> Mengambil batas ukuran file unggah yang dikonfigurasi di server
	 * dalam format string siap tampil (misal {@code "5 MB"}). Dipakai untuk memberi tahu
	 * pengguna batas ukuran sebelum mengunggah.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonFileMediaHelper#ukuranFileUpload()}.</p>
	 *
	 * @return string ukuran file upload dalam format label, misal {@code "5MB"}.
	 */
	public static String ukuranFileUpload() {
		return CommonFileMediaHelper.ukuranFileUpload();
	}

	/**
	 * <h3>Ambil string ukuran maksimal file unggah dengan batas kustom</h3>
	 *
	 * <p>Overload dengan batas kustom untuk konteks tertentu (misalnya form foto profil
	 * hanya 2 MB, sedangkan lampiran skripsi 20 MB).</p>
	 *
	 * @param cutomUkuranUpload ukuran maksimal dalam MB; override konfigurasi default.
	 * @return string ukuran file upload.
	 */
	public static String ukuranFileUpload(Integer cutomUkuranUpload) {
		return CommonFileMediaHelper.ukuranFileUpload(cutomUkuranUpload);
	}

	/**
	 * <h3>Ambil string label ukuran file upload untuk tooltip/keterangan form</h3>
	 *
	 * <p><b>Tujuan.</b> Menghasilkan teks keterangan lengkap untuk ditampilkan di dekat
	 * tombol unggah, misal {@code "Ukuran file maksimal 5 MB"}.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonFileMediaHelper#ukuranLabelFileUpload()}.</p>
	 *
	 * @return string label keterangan ukuran file.
	 */
	public static String ukuranLabelFileUpload() {
		return CommonFileMediaHelper.ukuranLabelFileUpload();
	}

	/**
	 * <h3>Ambil string label ukuran file upload dengan batas kustom</h3>
	 *
	 * <p>Overload dengan batas kustom; berguna saat form yang sama perlu menampilkan
	 * batasan berbeda untuk jenis file yang berbeda.</p>
	 *
	 * @param cutomUkuranUpload ukuran maksimal kustom dalam MB.
	 * @return string label keterangan ukuran file.
	 */
	public static String ukuranLabelFileUpload(Integer cutomUkuranUpload) {
		return CommonFileMediaHelper.ukuranLabelFileUpload(cutomUkuranUpload);
	}

	/**
	 * <h3>Muat ulang nilai mahasiswa secara langsung (sinkron) untuk perkuliahan tertentu</h3>
	 *
	 * <p><b>Tujuan.</b> Menghitung ulang dan menyimpan nilai final mahasiswa untuk
	 * daftar detail perkuliahan yang ditentukan, kemudian memanggil listener untuk
	 * memperbarui tampilan grid nilai. Berbeda dengan {@link #realoadNilai} yang berjalan
	 * asinkron di latar, method ini menjalankan semua langkah secara sinkron
	 * di thread yang memanggil.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonAcademicKrsNilaiHelper#realoadNilaiLangsung}.</p>
	 *
	 * @param perkuliahan                          perkuliahan yang nilainya dimuat ulang.
	 * @param sembunyikanNilaiJikaBelumDiverifikasi {@code true} = sembunyikan nilai
	 *                                              yang belum diverifikasi koordinator.
	 * @param eventListener                        listener dipanggil setelah reload selesai.
	 * @param detailperkuliahans                   koleksi ID detail perkuliahan yang diproses.
	 * @throws Exception bila terjadi kesalahan database atau kalkulasi nilai.
	 */
	public static void realoadNilaiLangsung(final Perkuliahan perkuliahan,
			Boolean sembunyikanNilaiJikaBelumDiverifikasi, final EventListener eventListener,
			Collection<Long> detailperkuliahans) throws Exception {
		CommonAcademicKrsNilaiHelper.realoadNilaiLangsung(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi,
				eventListener, detailperkuliahans);
	}

	/**
	 * Versi PARALEL dari {@link #realoadNilaiLangsung}: hitung ulang nilai tiap mahasiswa di thread &amp;
	 * session Hibernate sendiri. Jumlah thread = sebanyak mahasiswa, maksimal {@code maxThread}.
	 */
	public static void realoadNilaiLangsungParalel(final Perkuliahan perkuliahan,
			Boolean sembunyikanNilaiJikaBelumDiverifikasi, final EventListener eventListener,
			Collection<Long> detailperkuliahans, int maxThread) throws Exception {
		CommonAcademicKrsNilaiHelper.realoadNilaiLangsungParalel(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi,
				eventListener, detailperkuliahans, maxThread);
	}

	/**
	 * <h3>Muat ulang nilai mahasiswa secara langsung dengan flag percobaan</h3>
	 *
	 * <p>Overload {@link #realoadNilaiLangsung(Perkuliahan, Boolean, EventListener, Collection)}
	 * dengan parameter {@code coba} yang mengontrol apakah proses berjalan dalam
	 * mode percobaan (misal untuk validasi tanpa menyimpan ke database).</p>
	 *
	 * @param perkuliahan                          perkuliahan target.
	 * @param sembunyikanNilaiJikaBelumDiverifikasi filter nilai terverifikasi.
	 * @param eventListener                        listener setelah selesai.
	 * @param detailperkuliahans                   koleksi ID detail perkuliahan.
	 * @param coba                                 {@code true} = mode percobaan tanpa simpan permanen.
	 * @throws Exception bila terjadi kesalahan.
	 */
	public static void realoadNilaiLangsung(final Perkuliahan perkuliahan,
			Boolean sembunyikanNilaiJikaBelumDiverifikasi, final EventListener eventListener,
			Collection<Long> detailperkuliahans, boolean coba) throws Exception {
		CommonAcademicKrsNilaiHelper.realoadNilaiLangsung(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi,
				eventListener, detailperkuliahans, coba);
	}

	// public static void realoadNilai(final Perkuliahan perkuliahan, final
	// Boolean sembunyikanNilaiJikaBelumDiverifikasi,
	// final EventListener eventListener) {
	// Collection<Long> detailperkuliahans =
	// perkuliahan.ambilDetailperkuliahan();
	// realoadNilai(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi,
	// eventListener, detailperkuliahans);
	// }

	/**
	 * <h3>Muat ulang nilai mahasiswa secara asinkron di thread latar</h3>
	 *
	 * <p><b>Tujuan.</b> Versi asinkron dari {@link #realoadNilaiLangsung}: proses
	 * hitung ulang nilai dijalankan di thread latar agar UI tidak memblokir. Berguna
	 * saat proses nilai melibatkan banyak mahasiswa dan memerlukan waktu signifikan.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonAcademicKrsNilaiHelper#realoadNilai} yang menggunakan
	 * executor thread terpisah. Listener dipanggil di thread ZK yang sesuai via
	 * {@code Executions.schedule} setelah proses selesai.</p>
	 *
	 * <p><b>Threading.</b> Hati-hati: listener dipanggil di thread latar — pastikan
	 * semua akses komponen ZK dilakukan via {@code Executions.schedule} atau
	 * {@code Clients.flushEvent}.</p>
	 *
	 * @param perkuliahan                          perkuliahan target.
	 * @param sembunyikanNilaiJikaBelumDiverifikasi filter nilai terverifikasi.
	 * @param eventListener                        listener dipanggil setelah proses selesai.
	 * @param detailperkuliahans                   koleksi ID detail perkuliahan.
	 */
	public static void realoadNilai(final Perkuliahan perkuliahan, final Boolean sembunyikanNilaiJikaBelumDiverifikasi,
			final EventListener eventListener, final Collection<Long> detailperkuliahans) {
		CommonAcademicKrsNilaiHelper.realoadNilai(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi, eventListener,
				detailperkuliahans);
	}

	/**
	 * <h3>Padding kiri dengan nol hingga panjang N karakter</h3>
	 *
	 * <p><b>Tujuan.</b> Memformat string angka dengan padding nol di kiri agar selalu
	 * tepat {@code max} karakter panjangnya. Dipakai untuk memformat nomor urut, kode
	 * rekening, dan nomor surat yang harus memiliki panjang tetap.</p>
	 *
	 * <p><b>Cara kerja.</b> Menambahkan string nol panjang di depan, lalu mengambil
	 * {@code max} karakter dari kanan. Null aman (diperlakukan sebagai string kosong).</p>
	 *
	 * <p><b>Contoh.</b> {@code maxPanjangNol("42", 5)} → {@code "00042"}.</p>
	 *
	 * @param str string masukan; {@code null} → string kosong.
	 * @param max panjang total karakter output.
	 * @return string dengan padding nol di kiri tepat {@code max} karakter.
	 */
	public static String maxPanjangNol(String str, Integer max) {
		str = str == null ? "" : str.trim();
		str = "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" + str;
		return str.substring(str.length() - max);
	}

	/**
	 * <h3>Padding kanan dengan spasi hingga panjang N karakter</h3>
	 *
	 * <p><b>Tujuan.</b> Memformat string teks dengan padding spasi di kanan agar selalu
	 * tepat {@code max} karakter. Berguna untuk output fixed-width seperti ekspor ke
	 * format teks lurus (flat file).</p>
	 *
	 * <p><b>Contoh.</b> {@code maxPanjangSpace("ABC", 6)} → {@code "ABC   "}.</p>
	 *
	 * @param str string masukan; {@code null} → string kosong.
	 * @param max panjang total karakter output.
	 * @return string dengan padding spasi di kanan tepat {@code max} karakter.
	 */
	public static String maxPanjangSpace(String str, Integer max) {
		str = str == null ? "" : str.trim();
		str = str
				+ "                                                                                                     ";
		return str.substring(0, max);
	}

	/**
	 * <h3>Potong string dari kiri hingga maksimal N karakter</h3>
	 *
	 * <p><b>Tujuan.</b> Memastikan string tidak melebihi panjang {@code max} karakter
	 * dengan memotong dari kanan bila perlu. Berguna untuk kolom database dengan panjang
	 * terbatas (misal VARCHAR(100)) agar tidak terjadi constraint violation.</p>
	 *
	 * <p><b>Contoh.</b> {@code maxPanjang("ABCDEFGH", 5)} → {@code "ABCDE"}.</p>
	 *
	 * @param str string masukan; {@code null} → string kosong.
	 * @param max panjang maksimal yang diizinkan.
	 * @return string dipotong dari kanan bila perlu, tanpa padding.
	 */
	public static String maxPanjang(String str, Integer max) {
		return str == null ? "" : str.trim().length() > max ? str.trim().substring(0, max) : str.trim();
	}

	/**
	 * <h3>Ambil N karakter terakhir dari string</h3>
	 *
	 * <p><b>Tujuan.</b> Mengambil bagian akhir string hingga {@code max} karakter.
	 * Kebalikan dari {@link #maxPanjang}: bila string lebih panjang dari {@code max},
	 * yang diambil adalah karakter dari kanan (akhir), bukan dari kiri (awal).</p>
	 *
	 * <p><b>Contoh.</b> {@code maxPanjangAkhir("ABCDEFGH", 5)} → {@code "DEFGH"}.</p>
	 *
	 * @param str string masukan; {@code null} → string kosong.
	 * @param max jumlah karakter yang diambil dari akhir string.
	 * @return {@code max} karakter terakhir dari string, atau string penuh bila &le;{@code max}.
	 */
	public static String maxPanjangAkhir(String str, Integer max) {

		return str == null ? ""
				: str.trim().length() > max ? str.trim().substring(str.trim().length() - max) : str.trim();
	}

	/**
	 * <h3>Bersihkan string agar hanya mengandung digit, lalu potong/pad ke panjang N</h3>
	 *
	 * <p><b>Tujuan.</b> Memformat field angka yang berasal dari input bebas menjadi string
	 * digit-only dengan panjang tepat {@code max} karakter. Berguna untuk nomor rekening,
	 * NPWP, NIK, atau field numerik lain yang harus dikirim ke bank/gateway sebagai
	 * string digit murni.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Hapus semua karakter bukan digit dan titik via regex {@code [^\\d.]}.</li>
	 *   <li>Hapus titik, koma, tanda minus, dan plus.</li>
	 *   <li>Bila kosong, isi dengan {@code "0"} sebanyak {@code max} karakter.</li>
	 *   <li>Potong dari kiri ke {@code max} karakter.</li>
	 * </ol></p>
	 *
	 * @param str string masukan yang mungkin mengandung tanda baca; boleh {@code null}.
	 * @param max panjang maksimal string digit yang dihasilkan.
	 * @return string digit-only panjang maksimal {@code max} karakter.
	 */
	public static String maxPanjangNumeric(String str, Integer max) {
		if (str != null) {
			str = str.replaceAll("[^\\d.]", "");
			str = org.apache.commons.lang3.StringUtils.replace(str, ".", "");
			str = org.apache.commons.lang3.StringUtils.replace(str, ",", "");
			str = org.apache.commons.lang3.StringUtils.replace(str, "-", "");
			str = org.apache.commons.lang3.StringUtils.replace(str, "+", "");
		} else {
			str = "";
		}

		if (str.trim().isEmpty()) {
			for (int i = 0; i < max; i++) {
				str += "0";
			}
		}

		return str == null ? "" : str.trim().length() > max ? str.trim().substring(0, max) : str.trim();
	}

	/**
	 * <h3>Periksa apakah mahasiswa memenuhi semua syarat pendaftaran beasiswa</h3>
	 *
	 * <p><b>Tujuan.</b> Memvalidasi apakah pendaftar beasiswa
	 * ({@link ais.model.master.kemahasiswaan.MahasiswaDaftarBeasiswa}) memenuhi
	 * semua persyaratan yang ditetapkan (IPK minimum, semester tertentu, status
	 * aktif, persyaratan berkas, dll.).</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonAcademicSyncHelper#checkApakahMemenuhiSyaratBeasiswa}.</p>
	 *
	 * @param mahasiswaDaftarBeasiswa data pendaftaran beasiswa mahasiswa.
	 * @return {@code true} bila semua syarat terpenuhi; {@code false} bila ada syarat yang gagal.
	 */
	public static boolean checkApakahMemenuhiSyaratBeasiswa(MahasiswaDaftarBeasiswa mahasiswaDaftarBeasiswa) {
		return CommonAcademicSyncHelper.checkApakahMemenuhiSyaratBeasiswa(mahasiswaDaftarBeasiswa);
	}

	/**
	 * <h3>Periksa apakah mahasiswa memenuhi syarat bergabung ke organisasi kemahasiswaan</h3>
	 *
	 * <p><b>Tujuan.</b> Memvalidasi apakah mahasiswa memenuhi kriteria keanggotaan
	 * {@link ais.model.master.kemahasiswaan.OrganisasiIntraKampus} tertentu
	 * (UKM, BEM, Himpunan, dll.) berdasarkan aturan yang dikonfigurasi per-organisasi.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonAcademicSyncHelper#checkApakahMemenuhiSyaratOrganisasiKemahasiswaan}.</p>
	 *
	 * @param mahasiswa            mahasiswa yang akan bergabung.
	 * @param organisasiIntraKampus organisasi kemahasiswaan yang dituju.
	 * @return {@code true} bila memenuhi syarat; {@code false} sebaliknya.
	 */
	public static boolean checkApakahMemenuhiSyaratOrganisasiKemahasiswaan(Mahasiswa mahasiswa,
			OrganisasiIntraKampus organisasiIntraKampus) {
		return CommonAcademicSyncHelper.checkApakahMemenuhiSyaratOrganisasiKemahasiswaan(mahasiswa,
				organisasiIntraKampus);
	}

	/**
	 * <h3>Hapus KRS matakuliah yang melebihi batas SKS yang diizinkan</h3>
	 *
	 * <p><b>Tujuan.</b> Membersihkan KRS mahasiswa dari matakuliah yang kelebihan
	 * setelah penghitungan ulang batas SKS. Dipanggil setelah perubahan IP/status
	 * atau validasi KRS massal.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonAcademicSyncHelper#hapusMatakuliahYangMelebihiKetentuan}.</p>
	 *
	 * @param mahasiswa      mahasiswa yang KRS-nya dibersihkan.
	 * @param semester       semester aktif.
	 * @param tahapan        tahapan KRS (1 = reguler, 2 = revisi, dll.).
	 * @param semesterPendek kode semester pendek (0 = tidak ada, 1 = ada).
	 * @param jumlah         jumlah SKS maksimal yang diizinkan.
	 */
	public static void hapusMatakuliahYangMelebihiKetentuan(Mahasiswa mahasiswa, Integer semester,
			final Integer tahapan, Integer semesterPendek, Integer jumlah) {
		CommonAcademicSyncHelper.hapusMatakuliahYangMelebihiKetentuan(mahasiswa, semester, tahapan, semesterPendek, jumlah);
	}

	/**
	 * <h3>Ambil atau buat entitas KRS mahasiswa untuk semester/tahapan tertentu</h3>
	 *
	 * <p><b>Tujuan.</b> Mengambil entitas {@link ais.model.master.akademik.KrsMahasiswa}
	 * yang sudah ada untuk mahasiswa pada semester dan tahapan tertentu. Bila belum ada,
	 * entitas baru dibuat secara otomatis.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonAcademicSyncHelper#ambilDataKrsMahasiswa}.</p>
	 *
	 * @param session   sesi Hibernate aktif.
	 * @param mahasiswa mahasiswa yang KRS-nya dicari.
	 * @param semester  semester aktif.
	 * @param tahapan   tahapan KRS.
	 * @return entitas KRS yang ditemukan atau yang baru dibuat.
	 */
	public static KrsMahasiswa ambilDataKrsMahasiswa(Session session, Mahasiswa mahasiswa, Integer semester,
			Integer tahapan) {
		return CommonAcademicSyncHelper.ambilDataKrsMahasiswa(session, mahasiswa, semester, tahapan);
	}

	/**
	 * <h3>Sinkronkan KRS mahasiswa dan paksa refresh cache</h3>
	 *
	 * <p><b>Tujuan.</b> Memperbarui KRS mahasiswa untuk semester aktif sekarang
	 * dengan me-refresh data dari database (tidak menggunakan cache). Berguna setelah
	 * perubahan data mahasiswa yang memengaruhi KRS (status, nilai, dll.).</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonAcademicSyncHelper#singkronkanKrsMahasiswaRefresh}.</p>
	 *
	 * @param mahasiswa mahasiswa yang KRS-nya disinkronkan.
	 * @return entitas KRS yang diperbarui.
	 */
	public static KrsMahasiswa singkronkanKrsMahasiswaRefresh(Mahasiswa mahasiswa) {
		return CommonAcademicSyncHelper.singkronkanKrsMahasiswaRefresh(mahasiswa);
	}

	/**
	 * <h3>Sinkronkan KRS mahasiswa untuk semester aktif saat ini</h3>
	 *
	 * <p><b>Tujuan.</b> Memastikan data KRS mahasiswa (daftar matakuliah diambil,
	 * total SKS, status KRS) konsisten dengan data perkuliahan aktif. Memanggil
	 * sinkronisasi untuk semester dan tahapan saat ini.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonAcademicSyncHelper#singkronkanKrsMahasiswa(Mahasiswa)}.</p>
	 *
	 * @param mahasiswa mahasiswa yang KRS-nya disinkronkan.
	 * @return entitas KRS yang telah disinkronkan.
	 */
	public static KrsMahasiswa singkronkanKrsMahasiswa(Mahasiswa mahasiswa) {
		return CommonAcademicSyncHelper.singkronkanKrsMahasiswa(mahasiswa);
	}

	public static KrsMahasiswa ambilKrsMahasiswaTanpaSinkronisasi(Mahasiswa mahasiswa) {
		return CommonAcademicSyncHelper.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa);
	}

	public static KrsMahasiswa ambilKrsMahasiswaTanpaSinkronisasi(Mahasiswa mahasiswa, Integer semester,
			Integer tahapan, Integer semesterPendek) {
		return CommonAcademicSyncHelper.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, semester, tahapan,
				semesterPendek);
	}

	/**
	 * <h3>Sinkronkan KRS mahasiswa untuk semester/tahapan/semesterPendek tertentu</h3>
	 *
	 * <p>Overload dengan parameter semester, tahapan, dan semesterPendek eksplisit.
	 * Berguna saat perlu mensinkronkan KRS untuk semester di luar semester aktif
	 * (misalnya validasi historis atau pengambilan KRS lintas semester).</p>
	 *
	 * @param mahasiswa      mahasiswa target.
	 * @param semester       semester yang disinkronkan.
	 * @param tahapan        tahapan KRS.
	 * @param semesterPendek kode semester pendek.
	 * @return entitas KRS yang telah disinkronkan.
	 */
	public static KrsMahasiswa singkronkanKrsMahasiswa(Mahasiswa mahasiswa, Integer semester, Integer tahapan,
			Integer semesterPendek) {
		return CommonAcademicSyncHelper.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek);
	}

	/**
	 * <h3>Periksa dan isi Dosen Pembimbing Akademik (PA) bila KRS belum memilikinya</h3>
	 *
	 * <p><b>Tujuan.</b> Memastikan setiap KRS mahasiswa memiliki dosen PA yang valid.
	 * Bila belum diisi, dosen PA default diambil dari jurusan/program studi dan
	 * ditetapkan ke KRS.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonAcademicSyncHelper#checkDosenPa}.</p>
	 *
	 * @param krsMahasiswa KRS yang diperiksa dan diperbarui dosen PA-nya.
	 */
	public static void checkDosenPa(KrsMahasiswa krsMahasiswa) {
		CommonAcademicSyncHelper.checkDosenPa(krsMahasiswa);
	}

	/**
	 * <h3>Periksa dan isi Kelas mahasiswa bila KRS belum memilikinya</h3>
	 *
	 * <p><b>Tujuan.</b> Memastikan setiap KRS mahasiswa terhubung ke kelas yang sesuai.
	 * Bila belum diisi, kelas ditentukan berdasarkan aturan pengelompokan otomatis
	 * (alfabet, kapasitas, dll.).</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonAcademicSyncHelper#checkKelas}.</p>
	 *
	 * @param krsMahasiswa KRS yang diperiksa dan diperbarui kelasnya.
	 */
	public static void checkKelas(KrsMahasiswa krsMahasiswa) {
		CommonAcademicSyncHelper.checkKelas(krsMahasiswa);
	}

	/**
	 * <h3>Ambil batas min-max SKS dan IP last mahasiswa untuk pengambilan KRS</h3>
	 *
	 * <p><b>Tujuan.</b> Mengembalikan array tiga elemen yang berisi informasi batasan
	 * pengambilan KRS berdasarkan IPK mahasiswa:</p>
	 * <ul>
	 *   <li>Index 0: batas maksimum SKS yang boleh diambil (default jika tidak ada ketentuan).</li>
	 *   <li>Index 1: batas minimum IPK yang disyaratkan untuk mengambil KRS.</li>
	 *   <li>Index 2: IP semester terakhir mahasiswa (IP last).</li>
	 * </ul>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Panggil {@link #getIpkUntukPengambilanKRSDenganIPLast} untuk mendapatkan
	 *       {@link ais.model.master.akademik.PembatasanNilaiIPKUntukPengambilanKRS}
	 *       yang berlaku dan IP last.</li>
	 *   <li>Ekstrak nilai dari entitas pembatasan; gunakan default bila entitas null.</li>
	 *   <li>Return sebagai array Double dengan null-safe (nol bila tidak ada data).</li>
	 * </ol></p>
	 *
	 * @param mahasiswa      mahasiswa yang batas KRS-nya dihitung.
	 * @param semester       semester aktif.
	 * @param semesterPendek kode semester pendek.
	 * @return array {@code [maxSKS, minIPK, ipLast]} dalam tipe Double.
	 */
	public static Double[] getMinDanMaxIPK(Mahasiswa mahasiswa, Integer semester, Integer semesterPendek) {
		Double iplast = 0.0;

		// Guard: mahasiswa null (mis. KRS tanpa mahasiswa terpasang / data belum lengkap)
		// -> jangan lanjut memanggil getTahunangkatan()/getProgram() yang akan NPE, cukup
		// kembalikan nilai default agar pemanggil (KrsHelper/KrsPaketHelper.display) tetap jalan.
		if (mahasiswa == null) {
			return new Double[] { 0.0, 0.0, 0.0 };
		}

		Jurusan jurusan = mahasiswa.getJurusan();
		Fakultas fakultas = jurusan == null ? null : jurusan.getFakultas();

		Object[] objects = null;
		try {
			objects = Common.getIpkUntukPengambilanKRSDenganIPLast(mahasiswa, semester,
					mahasiswa.getTahunangkatan(), fakultas, jurusan, mahasiswa.getProgram(), semesterPendek);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:10930");

		}
		try {
			iplast = objects == null ? 0.0 : (Double) objects[1];
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:10886");

		}
		PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRS = null;
		try {
			pembatasanNilaiIPKUntukPengambilanKRS = objects == null ? null
					: (PembatasanNilaiIPKUntukPengambilanKRS) objects[0];
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:10892");

		}
		Integer maxsks = (Integer) (pembatasanNilaiIPKUntukPengambilanKRS == null
				? PembatasanNilaiIPKUntukPengambilanKRS.getDefaultPembatasanNilaiIpUntukAmbilKRS()
				: pembatasanNilaiIPKUntukPengambilanKRS.getBatasMaksimumIPKYangBolehDiambil());

		Double minip = pembatasanNilaiIPKUntukPengambilanKRS == null ? 0.0
				: pembatasanNilaiIPKUntukPengambilanKRS.getBatasTerendahIPK();
		return new Double[] { maxsks == null ? 0.0 : maxsks.doubleValue(), minip == null ? 0.0 : minip,
				iplast == null ? 0.0 : iplast };
	}

	/**
	 * <h3>Sinkronkan KRS mahasiswa dengan kontrol penyimpanan ke database</h3>
	 *
	 * <p>Overload {@link #singkronkanKrsMahasiswa(Mahasiswa, Integer, Integer, Integer)}
	 * dengan parameter {@code keDatabase}: bila {@code false}, sinkronisasi hanya
	 * dilakukan di memori (untuk preview/kalkulasi) tanpa menyimpan ke DB.</p>
	 *
	 * @param mahasiswa      mahasiswa target.
	 * @param semester       semester yang disinkronkan.
	 * @param tahapan        tahapan KRS.
	 * @param semesterPendek kode semester pendek.
	 * @param keDatabase     {@code true} = simpan perubahan ke DB; {@code false} = memori saja.
	 * @return entitas KRS yang telah disinkronkan.
	 */
	public static KrsMahasiswa singkronkanKrsMahasiswa(Mahasiswa mahasiswa, Integer semester, Integer tahapan,
			Integer semesterPendek, boolean keDatabase) {
		return CommonAcademicSyncHelper.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek,
				keDatabase);
	}

	/**
	 * <h3>Sinkronkan KRS mahasiswa dengan kontrol penyimpanan dan dosen PA default</h3>
	 *
	 * <p>Overload dengan {@code dosenPaDefault}: bila {@code true}, bila tidak ada dosen PA
	 * terdaftar untuk mahasiswa ini, sistem mengisi dosen PA default dari konfigurasi
	 * program studi.</p>
	 *
	 * @param mahasiswa      mahasiswa target.
	 * @param semester       semester yang disinkronkan.
	 * @param tahapan        tahapan KRS.
	 * @param semesterPendek kode semester pendek.
	 * @param keDatabase     {@code true} = simpan ke DB.
	 * @param dosenPaDefault {@code true} = isi dosen PA default bila belum ada.
	 * @return entitas KRS yang telah disinkronkan.
	 */
	public static KrsMahasiswa singkronkanKrsMahasiswa(Mahasiswa mahasiswa, Integer semester, Integer tahapan,
			Integer semesterPendek, boolean keDatabase, boolean dosenPaDefault) {
		return CommonAcademicSyncHelper.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek,
				keDatabase, dosenPaDefault);
	}

	/**
	 * <h3>Sinkronkan KRS mahasiswa (overload paling lengkap)</h3>
	 *
	 * <p>Overload dengan {@code jikaTidakAdaKembali}: bila {@code true} dan KRS tidak
	 * ditemukan setelah sinkronisasi, method mengembalikan {@code null} alih-alih
	 * membuat entitas kosong baru. Berguna saat pemanggil perlu tahu apakah mahasiswa
	 * benar-benar punya KRS atau tidak.</p>
	 *
	 * @param mahasiswa          mahasiswa target.
	 * @param semester           semester yang disinkronkan.
	 * @param tahapan            tahapan KRS.
	 * @param semesterPendek     kode semester pendek.
	 * @param keDatabase         {@code true} = simpan ke DB.
	 * @param dosenPaDefault     {@code true} = isi dosen PA default.
	 * @param jikaTidakAdaKembali {@code true} = return {@code null} bila KRS tidak ada.
	 * @return entitas KRS yang disinkronkan, atau {@code null} bila tidak ada.
	 */
	public static KrsMahasiswa singkronkanKrsMahasiswa(Mahasiswa mahasiswa, Integer semester, Integer tahapan,
			Integer semesterPendek, boolean keDatabase, boolean dosenPaDefault, boolean jikaTidakAdaKembali) {
		return CommonAcademicSyncHelper.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek,
				keDatabase, dosenPaDefault, jikaTidakAdaKembali);
	}

	/**
	 * <h3>Cek apakah mahasiswa sudah mengambil KRS Seminar/Sidang Skripsi di semua semester</h3>
	 *
	 * <p><b>Tujuan.</b> Memeriksa apakah mahasiswa sudah pernah mengambil matakuliah
	 * seminar/skripsi (diidentifikasi oleh label konfigurasi) di semester mana pun.
	 * Berguna untuk memvalidasi prasyarat pengajuan sidang akhir.</p>
	 *
	 * <p><b>Cara kerja.</b> Delegasi ke
	 * {@link ais.common.CommonAcademicSyncHelper#checkApakahSudahMengambilKrsSeminarSkripsiDan}.</p>
	 *
	 * @param mahasiswa              mahasiswa yang diperiksa.
	 * @param label_seminar_skripsi  label/kode konfigurasi yang mengidentifikasi MK seminar/skripsi.
	 * @return {@link ais.model.master.akademik.Detailperkuliahan} yang ditemukan,
	 *         atau {@code null} bila belum pernah mengambil.
	 */
	public static Detailperkuliahan checkApakahSudahMengambilKrsSeminarSkripsiDan(Mahasiswa mahasiswa,
			String label_seminar_skripsi) {
		return CommonAcademicSyncHelper.checkApakahSudahMengambilKrsSeminarSkripsiDan(mahasiswa, label_seminar_skripsi);
	}

	/**
	 * <h3>Cek apakah mahasiswa sudah mengambil KRS Seminar/Skripsi pada semester tertentu</h3>
	 *
	 * <p>Overload dari {@link #checkApakahSudahMengambilKrsSeminarSkripsiDan} dengan
	 * filter semester: hanya memeriksa KRS pada semester yang ditentukan. Dipakai
	 * saat validasi harus spesifik per-semester (misal tidak boleh mengambil ulang
	 * di semester yang sama).</p>
	 *
	 * @param mahasiswa             mahasiswa yang diperiksa.
	 * @param semester              semester spesifik yang dicek.
	 * @param label_seminar_skripsi label/kode konfigurasi MK seminar/skripsi.
	 * @return {@link ais.model.master.akademik.Detailperkuliahan} yang ditemukan,
	 *         atau {@code null} bila belum pernah mengambil di semester ini.
	 */
	public static Detailperkuliahan checkApakahSudahMengambilKrsSeminarSkripsi(Mahasiswa mahasiswa, Integer semester,
			String label_seminar_skripsi) {
		return CommonAcademicSyncHelper.checkApakahSudahMengambilKrsSeminarSkripsi(mahasiswa, semester,
				label_seminar_skripsi);
	}

	/**
	 * <h3>Hitung jumlah hari kalender antara dua tanggal (inklusif)</h3>
	 *
	 * <p><b>Tujuan.</b> Menghitung selisih hari antara {@code startDate} dan
	 * {@code endDate} tanpa mempertimbangkan hari libur atau akhir pekan.
	 * Menghitung semua hari kalender, termasuk Sabtu dan Minggu.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Bila kedua tanggal sama, return 0.</li>
	 *   <li>Bila startDate > endDate, tukar agar selalu menghitung dari kecil ke besar.</li>
	 *   <li>Iterasi hari satu per satu (tidak termasuk startDate) hingga mencapai endDate,
	 *       hitung setiap hari sebagai +1.</li>
	 * </ol></p>
	 *
	 * <p><b>Catatan.</b> Implementasi iteratif (bukan selisih milidetik) agar
	 * konsisten meski ada perubahan DST (daylight saving time).</p>
	 *
	 * @param startDate tanggal mulai.
	 * @param endDate   tanggal selesai.
	 * @return jumlah hari kalender antara dua tanggal (inklusif kedua ujung).
	 */
	public static int getBetweenTwoDates(Date startDate, Date endDate) {
		Calendar startCal = ais.ui.util.WaktuUtil.getCalendar();
		startCal.setTime(startDate);

		Calendar endCal = ais.ui.util.WaktuUtil.getCalendar();
		endCal.setTime(endDate);

		int workDays = 0;

		// Return 0 if start and end are the same
		if (startCal.getTimeInMillis() == endCal.getTimeInMillis()) {
			return 0;
		}

		if (startCal.getTimeInMillis() > endCal.getTimeInMillis()) {
			startCal.setTime(endDate);
			endCal.setTime(startDate);
		}

		while (true) {
			// excluding start date
			startCal.add(Calendar.DATE, 1);

			if (startCal.getTimeInMillis() >= endCal.getTimeInMillis()) {
				break;
			}

			++workDays;

		}

		return workDays + 1;
	}

	/**
	 * <h3>Hitung jumlah hari kerja antara dua tanggal untuk pegawai tertentu</h3>
	 *
	 * <p><b>Tujuan.</b> Menghitung selisih hari kerja (non-libur) antara {@code startDate}
	 * dan {@code endDate} dengan mempertimbangkan kalender libur yang berlaku untuk
	 * pegawai tertentu (satuan kerja, lokasi, atau jadwal kerja individual).</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Seperti {@link #getBetweenTwoDates} tetapi setiap hari diperiksa via
	 *       {@link #isHoliday(Date, Pegawai)} sebelum dihitung.</li>
	 *   <li>Hari yang merupakan hari libur (libur nasional, cuti bersama, atau
	 *       jadwal pegawai non-aktif) tidak dihitung.</li>
	 * </ol></p>
	 *
	 * @param startDate tanggal mulai.
	 * @param endDate   tanggal selesai.
	 * @param pegawai   pegawai yang jadwal liburnya menjadi acuan.
	 * @return jumlah hari kerja antara dua tanggal.
	 */
	public static int getWorkingDaysBetweenTwoDates(Date startDate, Date endDate, Pegawai pegawai) {
		Calendar startCal = ais.ui.util.WaktuUtil.getCalendar();
		startCal.setTime(startDate);

		Calendar endCal = ais.ui.util.WaktuUtil.getCalendar();
		endCal.setTime(endDate);

		int workDays = 0;

		// Return 0 if start and end are the same
		if (startCal.getTimeInMillis() == endCal.getTimeInMillis()) {
			return 0;
		}

		if (startCal.getTimeInMillis() > endCal.getTimeInMillis()) {
			startCal.setTime(endDate);
			endCal.setTime(startDate);
		}

		while (true) {
			// excluding start date
			startCal.add(Calendar.DATE, 1);

			if (startCal.getTimeInMillis() >= endCal.getTimeInMillis()) {
				break;
			}

			boolean holiday = isHoliday(startCal.getTime(), pegawai);
			// // System.out.println("date "
			// + Common.dateFormat4.get().format(startCal.getTime())
			// + ", holiday " + holiday);
			if (!holiday) {
				++workDays;
			}
		}

		return workDays + 1;
	}

	/**
	 * <h3>Hitung jumlah hari kerja antara dua tanggal (kalender libur nasional)</h3>
	 *
	 * <p>Overload {@link #getWorkingDaysBetweenTwoDates(Date, Date, Pegawai)} tanpa
	 * konteks pegawai. Menggunakan kalender libur nasional/umum (via
	 * {@link #isHoliday(Date)}) tanpa memperhitungkan jadwal individu pegawai.</p>
	 *
	 * @param startDate tanggal mulai.
	 * @param endDate   tanggal selesai.
	 * @return jumlah hari kerja antara dua tanggal.
	 */
	public static int getWorkingDaysBetweenTwoDates(Date startDate, Date endDate) {
		Calendar startCal = ais.ui.util.WaktuUtil.getCalendar();
		startCal.setTime(startDate);

		Calendar endCal = ais.ui.util.WaktuUtil.getCalendar();
		endCal.setTime(endDate);

		int workDays = 0;

		// Return 0 if start and end are the same
		if (startCal.getTimeInMillis() == endCal.getTimeInMillis()) {
			return 0;
		}

		if (startCal.getTimeInMillis() > endCal.getTimeInMillis()) {
			startCal.setTime(endDate);
			endCal.setTime(startDate);
		}

		while (true) {
			// excluding start date
			startCal.add(Calendar.DATE, 1);

			if (startCal.getTimeInMillis() >= endCal.getTimeInMillis()) {
				break;
			}

			boolean holiday = isHoliday(startCal.getTime());
			// // System.out.println("date "
			// + Common.dateFormat4.get().format(startCal.getTime())
			// + ", holiday " + holiday);
			if (!holiday) {
				++workDays;
			}
		}

		return workDays + 1;
	}

	/**
	 * Memeriksa apakah tanggal {@code d} merupakan hari libur merah yang terdaftar
	 * di cache {@code hariLiburPerpustakaans} (koleksi tanggal khusus perpustakaan/institusi).
	 *
	 * <p><b>Tujuan.</b> Memberikan pengecekan hari libur khusus yang bersumber dari pengaturan
	 * internal institusi (berbeda dari LiburNasional pemerintah). Biasanya dipakai modul
	 * perpustakaan atau jadwal internal untuk menandai tanggal tutup operasional.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Bila {@code hariLiburPerpustakaans} kosong, langsung kembalikan {@code false}.</li>
	 *   <li>Iterasi semua kunci map; bandingkan format tanggal (dateFormat1 = dd/MM/yyyy) antara
	 *       kunci dan {@code d}. Jika cocok, kembalikan {@code true}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Threading.</b> {@code hariLiburPerpustakaans} adalah {@code static} field; perubahan
	 * dari thread lain saat iterasi dapat menyebabkan {@code ConcurrentModificationException}
	 * pada implementasi HashMap biasa — pastikan diisi saat startup dan tidak diubah saat runtime.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Metode ini HANYA mengecek {@code hariLiburPerpustakaans}; ia tidak
	 * mengecek LiburNasional atau flag ConstantValues. Gunakan {@link #isHoliday(java.util.Date)}
	 * untuk pengecekan lengkap. Lihat juga {@link #isHolidayMerahDanAtauHariLibur(java.util.Date)}
	 * yang saat ini merupakan alias {@code isHoliday}.</p>
	 *
	 * @param d tanggal yang akan diperiksa; tidak boleh null
	 * @return {@code true} jika {@code d} terdaftar di {@code hariLiburPerpustakaans},
	 *         {@code false} jika tidak atau bila map kosong
	 */
	public static boolean isHolidayMerah(java.util.Date d) {

		if (hariLiburPerpustakaans.isEmpty()) {
			return false;
		}

		for (Date date : hariLiburPerpustakaans.keySet()) {
			if (Common.dateFormat1.get().format(date).equals(Common.dateFormat1.get().format(d))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Alias semantis untuk {@link #isHoliday(java.util.Date)} — memeriksa apakah
	 * {@code d} adalah hari libur berdasarkan seluruh logika libur sistem.
	 *
	 * <p><b>Tujuan.</b> Menyediakan nama yang lebih alami dalam konteks Bahasa Indonesia
	 * ("hari libur") sebagai sinonim dari {@code isHoliday}. Pemanggil lama yang sudah
	 * memakai nama ini tidak perlu diubah.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan sepenuhnya ke {@code Common.isHoliday(d)}; tidak
	 * ada logika tambahan.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Selalu sesuai dengan perilaku {@link #isHoliday(java.util.Date)}.
	 * Bila logika libur berubah, cukup perbarui {@code isHoliday}.</p>
	 *
	 * @param d tanggal yang akan diperiksa
	 * @return {@code true} jika {@code d} termasuk hari libur menurut sistem
	 */
	public static boolean hariLibur(java.util.Date d) {
		return Common.isHoliday(d);
	}

	/**
	 * Alias untuk {@link #isHoliday(java.util.Date)} — memeriksa apakah {@code d} merupakan
	 * hari merah dan/atau hari libur berdasarkan konfigurasi sistem.
	 *
	 * <p><b>Tujuan.</b> Menyediakan nama deskriptif yang mencerminkan kedua sumber libur
	 * (libur merah pemerintah via {@code LiburNasional} dan hari libur operasional institusi
	 * via {@code hariLiburPerpustakaans}).</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@link #isHoliday(java.util.Date)} tanpa
	 * perubahan logika.</p>
	 *
	 * @param d tanggal yang akan diperiksa
	 * @return {@code true} jika {@code d} termasuk hari libur/merah menurut sistem
	 */
	public static boolean isHolidayMerahDanAtauHariLibur(java.util.Date d) {
		return isHoliday(d);
	}

	/**
	 * Memeriksa apakah tanggal {@code d} adalah hari libur berdasarkan tiga sumber:
	 * (1) LiburNasional pemerintah, (2) daftar libur internal institusi, (3) flag hari
	 * libur sabtu/minggu dari {@code ConstantValues}.
	 *
	 * <p><b>Tujuan.</b> Menjadi satu-satunya titik kebenaran pengecekan hari libur yang
	 * menggabungkan semua aturan libur dalam sistem. Dipakai oleh hitung hari kerja,
	 * validasi kehadiran, penjadwalan, dan lainnya.</p>
	 *
	 * <p><b>Cara kerja (urutan prioritas).</b>
	 * <ol>
	 *   <li><b>LiburNasional:</b> Cek apakah {@code d} jatuh di antara {@code tanggal}
	 *       dan {@code sampai} setiap LiburNasional dari cache master
	 *       ({@code LiburNasional.ambilLiburNasionalsMaster()}).</li>
	 *   <li><b>hariLiburPerpustakaans:</b> Cek apakah tanggal (format dd/MM/yyyy) cocok
	 *       dengan kunci di map libur institusi.</li>
	 *   <li><b>aktifkanHariLiburMingguSaja:</b> Bila flag aktif dan {@code d} adalah hari
	 *       Minggu, kembalikan {@code true}.</li>
	 *   <li><b>aktifkanHariLibur:</b> Bila flag tidak aktif, kembalikan {@code false} (hari
	 *       kerja normal).</li>
	 *   <li><b>aktifkanHariLiburSabtuDanMingguSaja:</b> Bila aktif, Sabtu atau Minggu dianggap
	 *       libur; hari lain dianggap kerja.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Threading.</b> Bergantung pada {@code hariLiburPerpustakaans} (static) dan cache
	 * LiburNasional. Keduanya perlu diisi saat startup; perubahan di runtime sebaiknya
	 * terproteksi dengan sinkronisasi yang sesuai.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Semua flag berasal dari {@code ConstantValues}; ubah nilai
	 * konfigurasi di admin untuk mengaktifkan/menonaktifkan libur Sabtu-Minggu atau hanya
	 * Minggu. Penambahan sumber libur baru harus ditambahkan di metode ini dan semua
	 * aliasnya ({@link #hariLibur}, {@link #isHolidayMerahDanAtauHariLibur}).</p>
	 *
	 * @param d tanggal yang akan diperiksa; tidak boleh null
	 * @return {@code true} jika {@code d} merupakan hari libur menurut salah satu dari
	 *         tiga sumber di atas, {@code false} jika hari kerja
	 */
	public static boolean isHoliday(java.util.Date d) {

		for (LiburNasional liburNasional : LiburNasional.ambilLiburNasionalsMaster()) {
			if (Common.isDateBetween(d, liburNasional.getTanggal(), liburNasional.getSampai())) {
				return true;
			}
		}

		for (Date date : hariLiburPerpustakaans.keySet()) {
			if (Common.dateFormat1.get().format(date).equals(Common.dateFormat1.get().format(d))) {
				return true;
			}
		}

		if (ConstantValues.aktifkanHariLiburMingguSaja) {
			Calendar c = new GregorianCalendar();
			c.setTime(d);

			if ((Calendar.SUNDAY == c.get(Calendar.DAY_OF_WEEK))) {
				return true;
			}
		}

		if (!ConstantValues.aktifkanHariLibur) {
			return false;
		}

		Calendar c = new GregorianCalendar();
		c.setTime(d);

		if (ConstantValues.aktifkanHariLiburSabtuDanMingguSaja && ((Calendar.SATURDAY == c.get(Calendar.DAY_OF_WEEK))
				|| (Calendar.SUNDAY == c.get(Calendar.DAY_OF_WEEK)))) {
			return (true);
		} else {
			return false;
		}
	}

	/**
	 * Alias kontekstual untuk {@link #isHoliday(java.util.Date, Pegawai)} — memeriksa
	 * apakah {@code d} merupakan hari merah dan/atau hari libur berdasarkan jadwal
	 * kerja spesifik pegawai.
	 *
	 * <p><b>Tujuan.</b> Memberi nama yang lebih ekspresif bagi pemanggil yang ingin
	 * menekankan bahwa pengecekan mempertimbangkan hari merah nasional sekaligus hari
	 * libur per jadwal pegawai.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code isHoliday(d, pegawai)}.</p>
	 *
	 * @param d       tanggal yang diperiksa
	 * @param pegawai pegawai pemilik jadwal kerja; boleh null (akan pakai logika global)
	 * @return {@code true} jika hari itu libur menurut jadwal pegawai atau libur sistem
	 */
	public static boolean isHolidayMerahDanAtauHariLibur(java.util.Date d, Pegawai pegawai) {
		return isHoliday(d, pegawai);
	}

	/**
	 * Memeriksa apakah tanggal {@code d} adalah hari libur untuk {@code pegawai} tertentu,
	 * dengan mempertimbangkan jadwal kerja mingguan sesuai {@code TipePegawai} pegawai tersebut.
	 *
	 * <p><b>Tujuan.</b> Memungkinkan sistem mengenali hari libur secara personal per pegawai —
	 * misalnya pegawai tipe tertentu mungkin masuk Sabtu tetapi tidak Minggu, atau sebaliknya.
	 * Dipakai dalam hitung lembur, absensi, dan penjadwalan per pegawai.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Cek {@code hariLiburPerpustakaans}: bila tanggal cocok, kembalikan {@code true}.</li>
	 *   <li>Bila {@code pegawai} dan {@code pegawai.getTipePegawai()} tidak null, cek hari
	 *       kalender ({@code Calendar.DAY_OF_WEEK}) lalu bandingkan dengan flag hari di
	 *       {@code TipePegawai}: {@code getSenin()}, {@code getSelasa()}, ..., {@code getMinggu()}.
	 *       Hari dianggap libur jika flag hari bersangkutan adalah {@code false} (tidak masuk kerja).</li>
	 *   <li>Bila {@code pegawai} null atau tidak punya {@code TipePegawai}, kembalikan
	 *       {@code false} (dianggap hari kerja).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Perbedaan dari {@link #isHoliday(java.util.Date)}.</b> Metode tanpa pegawai
	 * mengecek juga LiburNasional dan flag global ConstantValues. Metode ini HANYA mengecek
	 * hariLiburPerpustakaans dan jadwal TipePegawai — tidak ada pengecekan LiburNasional.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Bila tipe pegawai baru ditambah, pastikan {@code TipePegawai}
	 * memiliki flag hari yang lengkap. Untuk pegawai tanpa tipe, metode ini selalu
	 * mengembalikan {@code false}.</p>
	 *
	 * @param d       tanggal yang akan diperiksa; tidak boleh null
	 * @param pegawai pegawai yang jadwal kerjanya digunakan sebagai acuan; boleh null
	 * @return {@code true} jika {@code d} adalah hari libur bagi {@code pegawai},
	 *         {@code false} jika hari kerja atau pegawai/tipe null
	 */
	public static boolean isHoliday(java.util.Date d, Pegawai pegawai) {

		for (Date date : hariLiburPerpustakaans.keySet()) {
			if (Common.dateFormat1.get().format(date).equals(Common.dateFormat1.get().format(d))) {
				return true;
			}
		}

		if (pegawai != null && pegawai.getTipePegawai() != null) {
			Calendar c = new GregorianCalendar();
			c.setTime(d);

			if (Calendar.SATURDAY == c.get(Calendar.DAY_OF_WEEK)) {
				return !pegawai.getTipePegawai().getSabtu();
			} else if (Calendar.SUNDAY == c.get(Calendar.DAY_OF_WEEK)) {
				return !pegawai.getTipePegawai().getMinggu();
			} else if (Calendar.MONDAY == c.get(Calendar.DAY_OF_WEEK)) {
				return !pegawai.getTipePegawai().getSenin();
			} else if (Calendar.TUESDAY == c.get(Calendar.DAY_OF_WEEK)) {
				return !pegawai.getTipePegawai().getSelasa();
			} else if (Calendar.WEDNESDAY == c.get(Calendar.DAY_OF_WEEK)) {
				return !pegawai.getTipePegawai().getRabu();
			} else if (Calendar.THURSDAY == c.get(Calendar.DAY_OF_WEEK)) {
				return !pegawai.getTipePegawai().getKamis();
			} else if (Calendar.FRIDAY == c.get(Calendar.DAY_OF_WEEK)) {
				return !pegawai.getTipePegawai().getJumat();
			}
		}
		return false;
	}

	/**
	 * Menghitung tanggal kerja ke-{@code jumlahHari} setelah {@code startDate}, melewati
	 * hari libur berdasarkan {@link #isHoliday(java.util.Date)}.
	 *
	 * <p><b>Tujuan.</b> Memberikan tanggal batas waktu nyata (SLA, tenggat pengerjaan, dll.)
	 * dengan menghitung hari kalender maju tetapi hanya menghitung hari kerja (non-libur).</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Bila {@code jumlahHari} ≤ 0, kembalikan {@code startDate} itu sendiri.</li>
	 *   <li>Inisialisasi kalender dari {@code startDate}; set {@code workDays=0}.</li>
	 *   <li>Loop: maju 1 hari; bila hari tersebut bukan libur ({@code !isHoliday}), tambah
	 *       {@code workDays}. Berhenti saat {@code workDays == jumlahHari}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Perhatian.</b> Loop tidak memiliki batas iterasi eksplisit; bila konfigurasi libur
	 * sangat agresif (semua hari dianggap libur), loop bisa berjalan selamanya. Dalam praktik
	 * normal tidak terjadi karena minimal Senin-Jumat selalu dianggap hari kerja.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Terkait erat dengan {@link #isHoliday(java.util.Date)}.
	 * Lihat juga {@link #getWorkingDaysBetweenTwoDates} untuk menghitung selisih hari kerja
	 * antara dua tanggal. Untuk variasi yang menggunakan jadwal pegawai, buat overload
	 * menggunakan {@link #isHoliday(java.util.Date, Pegawai)}.</p>
	 *
	 * @param startDate  tanggal awal (tidak dihitung sebagai hari kerja)
	 * @param jumlahHari jumlah hari kerja yang ingin ditambahkan; bila ≤ 0 dikembalikan startDate
	 * @return tanggal kerja ke-{@code jumlahHari} setelah {@code startDate}
	 */
	public static Date getDateWorkingDays(Date startDate, int jumlahHari) {
		if (jumlahHari <= 0) {
			return startDate;
		}
		Calendar c = new GregorianCalendar();
		c.setTime(startDate);
		int workDays = 0;
		while (true) {

			c.add(Calendar.DAY_OF_MONTH, 1);
			// // System.out.println("Day = "
			// + Common.dateFormat6.get().format(c.getTime()) + ", workDays = "
			// + workDays + ", jumlahHari = " + jumlahHari);

			if (!isHoliday(c.getTime())) {
				++workDays;
				if (workDays == jumlahHari) {
					break;
				}
			}

		}
		return c.getTime();
	}

	/**
	 * Mengambil atau membuat entitas {@code PenugasanDosenMengajar} yang mencocokkan parameter
	 * jurusan, program, tahun, jenis semester, SKS, dan dosen dari database atau cache.
	 *
	 * <p><b>Tujuan.</b> Memastikan setiap mata kuliah yang ditugaskan ke dosen memiliki satu
	 * rekam penugasan yang konsisten. Dipanggil saat sinkronisasi KRS/perkuliahan.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicSyncHelper.getPenugasanDosenMengajar}.</p>
	 *
	 * @param idJurusan    ID jurusan/program studi penyelenggara
	 * @param program      nama program (S1, D3, dsb.)
	 * @param tahun        tahun akademik (misal "2024/2025")
	 * @param jenisSemester "Ganjil" atau "Genap"
	 * @param sks          jumlah SKS mata kuliah
	 * @param dosen        dosen pengampu; boleh null bila belum ditetapkan
	 * @return entitas {@code PenugasanDosenMengajar} yang sesuai, atau null bila tidak ditemukan
	 */
	public static PenugasanDosenMengajar getPenugasanDosenMengajar(Long idJurusan, String program, String tahun,
			String jenisSemester, Integer sks, Dosen dosen) {
		return CommonAcademicSyncHelper.getPenugasanDosenMengajar(idJurusan, program, tahun, jenisSemester, sks, dosen);
	}

	/**
	 * Mengambil atau membuat entitas {@code PenugasanGuruMengajar} yang mencocokkan parameter
	 * sekolah, program, tahun, jenis semester, dan guru dari database atau cache.
	 *
	 * <p><b>Tujuan.</b> Memastikan setiap jadwal pelajaran sekolah memiliki rekam penugasan
	 * guru yang konsisten, dipakai saat sinkronisasi absensi/jadwal pelajaran.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicSyncHelper.getPenugasanGuruMengajar}.</p>
	 *
	 * @param idSekolah    ID sekolah penyelenggara
	 * @param program      nama program/tingkat (SD, SMP, SMA, dsb.)
	 * @param tahun        tahun akademik
	 * @param jenisSemester "Ganjil" atau "Genap"
	 * @param guru         guru pengampu; boleh null
	 * @return entitas {@code PenugasanGuruMengajar} yang sesuai, atau null bila tidak ditemukan
	 */
	public static PenugasanGuruMengajar getPenugasanGuruMengajar(Long idSekolah, String program, String tahun,
			String jenisSemester, Guru guru) {
		return CommonAcademicSyncHelper.getPenugasanGuruMengajar(idSekolah, program, tahun, jenisSemester, guru);
	}

	/**
	 * Mengambil daftar {@code FormatNilai} untuk sekumpulan perkuliahan sekaligus.
	 *
	 * <p><b>Tujuan.</b> Memuat format penilaian (bobot komponen nilai: tugas, UTS, UAS, dll.)
	 * dari semua perkuliahan dalam daftar agar dapat digunakan dalam kalkulasi nilai akhir
	 * mahasiswa secara batch.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicSyncHelper.getFormatNilais(List)}.</p>
	 *
	 * @param perkuliahans daftar perkuliahan yang format nilainya dimuat; tidak boleh null
	 * @return daftar {@code FormatNilai} gabungan dari semua perkuliahan
	 */
	public static List<FormatNilai> getFormatNilais(List<Perkuliahan> perkuliahans) {
		return CommonAcademicSyncHelper.getFormatNilais(perkuliahans);
	}

	/**
	 * Mengambil daftar {@code FormatNilai} untuk satu perkuliahan dari cache atau database.
	 *
	 * <p><b>Tujuan.</b> Memuat komponen dan bobot penilaian (tugas, UTS, UAS, kuis, dsb.)
	 * untuk satu perkuliahan; dipakai saat menampilkan form input nilai atau menghitung
	 * nilai akhir mahasiswa.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicSyncHelper.getFormatNilais(Perkuliahan)}.</p>
	 *
	 * @param perkuliahan perkuliahan yang format nilainya dimuat; boleh null (hasilnya list kosong)
	 * @return daftar {@code FormatNilai} milik perkuliahan tersebut
	 */
	public static List<FormatNilai> getFormatNilais(Perkuliahan perkuliahan) {
		return CommonAcademicSyncHelper.getFormatNilais(perkuliahan);
	}

	/**
	 * Mengambil daftar {@code FormatNilai} untuk satu perkuliahan dengan opsi paksa-muat-ulang
	 * dari database (mengabaikan cache).
	 *
	 * <p><b>Tujuan.</b> Memastikan data format nilai yang digunakan selalu mutakhir, terutama
	 * setelah admin mengubah bobot komponen penilaian di tengah semester.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicSyncHelper.getFormatNilais(Perkuliahan, boolean)}.</p>
	 *
	 * @param perkuliahan perkuliahan yang format nilainya dimuat
	 * @param refresh     {@code true} untuk memuat ulang dari DB melewati cache;
	 *                    {@code false} untuk menggunakan cache bila tersedia
	 * @return daftar {@code FormatNilai} yang segar (atau dari cache bila {@code refresh=false})
	 */
	public static List<FormatNilai> getFormatNilais(Perkuliahan perkuliahan, boolean refresh) {
		return CommonAcademicSyncHelper.getFormatNilais(perkuliahan, refresh);
	}

	/**
	 * Mengambil daftar {@code FormatNilai} untuk satu perkuliahan menggunakan sesi Hibernate
	 * yang diberikan oleh pemanggil.
	 *
	 * <p><b>Tujuan.</b> Memungkinkan pemuatan format nilai dalam konteks transaksi yang sedang
	 * berjalan (tidak membuka sesi baru), sehingga entitas yang dihasilkan berada dalam sesi
	 * yang sama dan dapat dikaitkan dengan perubahan lain dalam satu transaksi.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicSyncHelper.getFormatNilais(Session, Perkuliahan)}.</p>
	 *
	 * @param session     sesi Hibernate aktif milik pemanggil; tidak boleh null atau tertutup
	 * @param perkuliahan perkuliahan yang format nilainya dimuat
	 * @return daftar {@code FormatNilai} yang dimuat menggunakan {@code session}
	 */
	public static List<FormatNilai> getFormatNilais(Session session, Perkuliahan perkuliahan) {
		return CommonAcademicSyncHelper.getFormatNilais(session, perkuliahan);
	}

	/**
	 * Menghasilkan angka acak {@code long} dalam rentang [0, 99_999_999] menggunakan
	 * {@code ThreadLocalRandom} yang aman untuk multi-thread.
	 *
	 * <p><b>Tujuan.</b> Menyediakan ID sementara, nonce, atau penanda unik ringan tanpa
	 * perlu instansiasi {@code Random} terpisah. Dipakai misalnya untuk ID transaksi
	 * sementara atau salt sederhana.</p>
	 *
	 * <p><b>Threading.</b> {@code ThreadLocalRandom} adalah thread-local sehingga tidak
	 * memiliki contention antar thread — aman dipakai dari pool thread atau servlet concurrently.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Nilai maksimum 99_999_999 (8 digit). Bukan kriptografi-aman;
	 * untuk kebutuhan keamanan tinggi gunakan {@code SecureRandom}.</p>
	 *
	 * @return angka acak long antara 0 (inklusif) dan 99_999_999 (eksklusif)
	 */
	public static long randLong() {
		return ThreadLocalRandom.current().nextLong(0, 99999999);
	}

	/**
	 * Menghasilkan acuan (ref) SEMENTARA untuk berkas yang diunggah sebelum entitas
	 * induknya punya id, mis. lampiran pada formulir pendaftaran yang belum tersimpan.
	 *
	 * <p><b>Selalu negatif.</b> Nilainya dipakai pada kolom acuan yang sama dengan id
	 * asli (lihat {@code FileFotoLain.ambil}: berkas dicari dengan
	 * {@code Restrictions.eq(refName, ref)}). Acuan sementara yang positif bisa menunjuk
	 * ke baris milik entitas lain yang benar-benar ada.</p>
	 *
	 * <p><b>Ruangnya lebar (~1e16), bukan ~1e8.</b> Kolom acuan ini dipakai BERSAMA oleh
	 * semua pengguna yang sedang mengunggah. Dengan ruang sesempit {@link #randLong()},
	 * dua pendaftar yang mengisi formulir bersamaan bisa mendapat acuan yang sama untuk
	 * jenis berkas yang sama, lalu saling melihat berkas satu sama lain. Dua undian
	 * digabung supaya bentrok praktis tidak terjadi.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Nilai terkecil secara magnitudo adalah -100_000_000, jadi
	 * hasilnya tidak pernah 0 dan tidak pernah muat di {@code int} - jangan simpan ke
	 * kolom integer. Bukan kriptografi-aman.</p>
	 *
	 * @return acuan sementara negatif, kira-kira antara -1e16 dan -1e8
	 */
	public static Long refSementara() {
		return Long.valueOf(-((randLong() + 1L) * 100000000L + randLong()));
	}

	/**
	 * Mencari daftar {@code CicilanPembayaran} yang cocok dengan parameter identifikasi
	 * pembayaran (kode tagihan, NIM mahasiswa, tanggal) menggunakan sesi dan log H2H sebagai
	 * konteks pencarian.
	 *
	 * <p><b>Tujuan.</b> Mengambil cicilan pembayaran aktif/sukses untuk keperluan rekonsiliasi
	 * pembayaran host-to-host dengan bank/payment gateway.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonPaymentHelper.ambilCicilanPembayarans}.</p>
	 *
	 * @param session       sesi Hibernate aktif
	 * @param logHostToHost log transaksi H2H yang menjadi konteks pencarian; boleh null
	 * @param kode          kode tagihan/jenis kegiatan
	 * @param nim           nomor induk mahasiswa
	 * @param tanggal       tanggal transaksi pembayaran
	 * @return daftar cicilan pembayaran yang cocok; list kosong bila tidak ditemukan
	 */
	public static List<CicilanPembayaran> ambilCicilanPembayarans(Session session, LogHostToHost logHostToHost,
			String kode, String nim, Date tanggal) {
		return CommonPaymentHelper.ambilCicilanPembayarans(session, logHostToHost, kode, nim, tanggal);
	}

	/**
	 * Mencari daftar {@code CicilanPembayaranGagal} yang cocok dengan parameter identifikasi
	 * pembayaran untuk keperluan rekonsiliasi atau retry pembayaran yang sebelumnya gagal.
	 *
	 * <p><b>Tujuan.</b> Mengidentifikasi cicilan yang tercatat sebagai gagal sehingga bisa
	 * diproses ulang (copy ke sukses) atau dilaporkan ke admin/gateway.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonPaymentHelper.ambilCicilanPembayaranGagals}.</p>
	 *
	 * @param session       sesi Hibernate aktif
	 * @param logHostToHost log transaksi H2H; boleh null
	 * @param kode          kode tagihan
	 * @param nim           nomor induk mahasiswa
	 * @param tanggal       tanggal transaksi
	 * @return daftar cicilan gagal yang cocok; list kosong bila tidak ditemukan
	 */
	public static List<CicilanPembayaranGagal> ambilCicilanPembayaranGagals(Session session,
			LogHostToHost logHostToHost, String kode, String nim, Date tanggal) {
		return CommonPaymentHelper.ambilCicilanPembayaranGagals(session, logHostToHost, kode, nim, tanggal);
	}

	/**
	 * Menyalin data dari {@code CicilanPembayaran} sukses ke entitas {@code CicilanPembayaranGagal}
	 * baru, sebagai audit trail ketika pembayaran perlu ditandai ulang sebagai gagal.
	 *
	 * <p><b>Tujuan.</b> Memindahkan rekam pembayaran sukses ke tabel gagal untuk keperluan
	 * rollback, dispute, atau audit log pembayaran yang dibatalkan/dikembalikan.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonPaymentHelper.copyCicilanPembayaranKeGagal}.</p>
	 *
	 * @param cicilanPembayaran cicilan sumber yang akan disalin; tidak boleh null
	 * @return entitas {@code CicilanPembayaranGagal} baru (belum disimpan ke DB)
	 */
	public static CicilanPembayaranGagal copyCicilanPembayaranKeGagal(CicilanPembayaran cicilanPembayaran) {
		return CommonPaymentHelper.copyCicilanPembayaranKeGagal(cicilanPembayaran);
	}

	/**
	 * Menyalin data dari {@code CicilanPembayaranGagal} ke entitas {@code CicilanPembayaran}
	 * baru, sebagai mekanisme retry atau konfirmasi ulang pembayaran yang semula gagal.
	 *
	 * <p><b>Tujuan.</b> Mengkonversi rekam cicilan gagal menjadi cicilan sukses setelah
	 * rekonsiliasi berhasil mengkonfirmasi pembayaran dengan bank/gateway.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonPaymentHelper.copyCicilanPembayaranKeSukses}.</p>
	 *
	 * @param cicilanPembayaranGagal cicilan gagal sumber; tidak boleh null
	 * @return entitas {@code CicilanPembayaran} baru (belum disimpan ke DB)
	 */
	public static CicilanPembayaran copyCicilanPembayaranKeSukses(CicilanPembayaranGagal cicilanPembayaranGagal) {
		return CommonPaymentHelper.copyCicilanPembayaranKeSukses(cicilanPembayaranGagal);
	}

	/**
	 * Memeriksa dan membuat entitas {@code OrangTua} secara otomatis dari data
	 * {@code BiodataMahasiswa} bila konfigurasi sistem mengaktifkan fitur ini.
	 *
	 * <p><b>Tujuan.</b> Memastikan setiap mahasiswa baru otomatis memiliki akun orang tua
	 * di sistem (untuk portal orang tua/wali) tanpa perlu pendaftaran manual.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonLibraryAutoHelper.checkApakahMahasiswaOtomatisMenjadiOrangTua}.</p>
	 *
	 * @param biodataMahasiswa biodata mahasiswa yang baru didaftarkan; tidak boleh null
	 * @return entitas {@code OrangTua} yang dibuat/ditemukan, atau null bila fitur tidak aktif
	 */
	public static OrangTua checkApakahMahasiswaOtomatisMenjadiOrangTua(BiodataMahasiswa biodataMahasiswa) {
		return CommonLibraryAutoHelper.checkApakahMahasiswaOtomatisMenjadiOrangTua(biodataMahasiswa);
	}

	/**
	 * Memeriksa dan membuat entitas {@code OrangTua} secara otomatis dari data {@code Siswa}
	 * bila konfigurasi sistem mengaktifkan fitur ini.
	 *
	 * <p><b>Tujuan.</b> Sama dengan {@link #checkApakahMahasiswaOtomatisMenjadiOrangTua} tetapi
	 * untuk konteks siswa sekolah (bukan mahasiswa perguruan tinggi).</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonLibraryAutoHelper.checkApakahSiswaOtomatisMenjadiOrangTua}.</p>
	 *
	 * @param siswa siswa yang baru didaftarkan; tidak boleh null
	 * @return entitas {@code OrangTua} yang dibuat/ditemukan, atau null bila fitur tidak aktif
	 */
	public static OrangTua checkApakahSiswaOtomatisMenjadiOrangTua(Siswa siswa) {
		return CommonLibraryAutoHelper.checkApakahSiswaOtomatisMenjadiOrangTua(siswa);
	}


	/**
	 * Mendaftarkan mahasiswa (berdasarkan NIM) secara otomatis sebagai anggota perpustakaan
	 * bila belum terdaftar, atau memperbarui data anggota yang sudah ada.
	 *
	 * <p><b>Tujuan.</b> Memastikan setiap mahasiswa aktif yang login ke sistem secara otomatis
	 * memiliki kartu anggota perpustakaan sehingga bisa meminjam buku tanpa registrasi manual.</p>
	 *
	 * <p><b>Cara kerja (inline — bukan delegasi).</b>
	 * <ol>
	 *   <li>Buka {@code currentNativeSession()} (sesi Hibernate thread-local).</li>
	 *   <li>Cari {@code Anggota} yang sudah terhubung ke mahasiswa dengan NIM tersebut, atau
	 *       yang kode anggotanya cocok dengan NIM (fuzzy match: hapus titik dan koma).</li>
	 *   <li>Cari {@code Mahasiswa} aktif dengan NIM yang sama.</li>
	 *   <li>Bila mahasiswa ditemukan: buat {@code Anggota} baru bila belum ada, lalu isi/perbarui
	 *       data (alamat, email, jenis identitas NIM, tipe MAHASISWA, kode=NIM), commit transaksi.</li>
	 *   <li>Panggil {@code HibernateUtil.closeSession()} di akhir untuk menutup sesi.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Manajemen sesi.</b> Menggunakan {@code currentNativeSession()} dan menutupnya
	 * sendiri dengan {@code HibernateUtil.closeSession()} — TIDAK menggunakan try-finally.
	 * Bila terjadi exception sebelum {@code closeSession()}, sesi bisa bocor. Ini adalah
	 * pola lama yang tidak ideal; pertimbangkan refaktor ke try-finally saat memelihara
	 * metode ini.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Jenis anggota diset ke {@code LibraryUtil.ANGGOTA_REGULER} dan
	 * tipe identitas ke {@code LibraryUtil.NIM}. Perpustakaan diset {@code null} (anggota berlaku
	 * di semua perpustakaan). Untuk jenis entitas lain (pegawai, dosen, siswa), lihat overload
	 * lain dari metode ini.</p>
	 *
	 * @param nim nomor induk mahasiswa yang akan didaftarkan; tidak boleh null
	 * @return entitas {@code Anggota} yang dibuat/diperbarui, atau null bila mahasiswa tidak ditemukan
	 */
	public static Anggota checkApakahMahasiswaOtomatisMenjadiAnggotaPerpustakaan(String nim) {
		if (nim == null || nim.trim().isEmpty()) {
			return null;
		}

		Session session = HibernateUtil.currentNativeSession();
		try {
			String nimAman = nim.replaceAll("'", "");
			Anggota anggota = ((Anggota) session.createCriteria(Anggota.class)
					.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
					.add(Restrictions.or(Restrictions.eq("mahasiswa.nim", nim),
							Restrictions.sqlRestriction(
									"replace(replace(trim(this_.kode),'.',''),',','') = replace(replace(trim('" + nimAman
											+ "'),'.',''),',','')")))

					.setMaxResults(1).uniqueResult());

			Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions
							.sqlRestriction("replace(replace(trim(this_.nim),'.',''),',','') = replace(replace(trim('" + nimAman
									+ "'),'.',''),',','')"))
					.setMaxResults(1).uniqueResult();
			if (mahasiswa != null) {
				if (anggota == null) {
					anggota = new Anggota();
					anggota.setAktif(true);
				}

				anggota.setAlamat(mahasiswa.getAlamat());
				anggota.setEmail(mahasiswa.getEmail());
				anggota.setJenisAnggota(LibraryUtil.ANGGOTA_REGULER);
				anggota.setJenisIdentitasAnggota(LibraryUtil.NIM);
				anggota.setJenisIdentitas("NIM");
				anggota.setKeterangan("Anggota ini mendaftar otomatis");
				anggota.setKodeIdentitas(mahasiswa.getNim());
				anggota.setKode(mahasiswa.getNim());
				anggota.setNama(mahasiswa.getNama());
				anggota.setPerpustakaan(null);
				anggota.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
				anggota.setMahasiswa(mahasiswa);
				anggota.setTipeAnggota(LibraryUtil.MAHASISWA);
				anggota.setTipe(LibraryUtil.MAHASISWA.getNama());

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, anggota);
				session.getTransaction().commit();
			}

			return anggota;
		} catch (Exception e) {
			try {
				if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/Common.java:checkMahasiswaPerpusRollback"); }
			ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/Common.java:checkApakahMahasiswaOtomatisMenjadiAnggotaPerpustakaan");
			return null;
		} finally {
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Mendaftarkan pegawai (berdasarkan kode/NIK) secara otomatis sebagai anggota perpustakaan
	 * bila belum terdaftar.
	 *
	 * <p><b>Tujuan.</b> Memastikan setiap pegawai aktif yang login ke sistem secara otomatis
	 * memiliki kartu anggota perpustakaan tanpa registrasi manual.</p>
	 *
	 * <p><b>Cara kerja (inline).</b>
	 * <ol>
	 *   <li>Buka {@code currentNativeSession()}.</li>
	 *   <li>Cari {@code Anggota} yang terhubung ke pegawai dengan kode {@code nidn}, atau
	 *       kode anggota cocok (fuzzy match hapus titik).</li>
	 *   <li>Bila {@code Anggota} belum ada, cari {@code Pegawai} dengan kode tersebut.</li>
	 *   <li>Bila ditemukan: buat {@code Anggota} baru, isi data (alamat, email, jenis identitas
	 *       NIK, tipe PEGAWAI, kode=mycode pegawai), simpan, commit.</li>
	 *   <li>Bila {@code Anggota} sudah ada, tidak ada pembaruan (hanya dikembalikan).</li>
	 *   <li>Tutup sesi dengan {@code HibernateUtil.closeSession()}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Manajemen sesi.</b> Sama dengan
	 * {@link #checkApakahMahasiswaOtomatisMenjadiAnggotaPerpustakaan} — gunakan
	 * try-finally bila merefaktor.</p>
	 *
	 * @param nidn kode/NIK pegawai yang akan didaftarkan; tidak boleh null
	 * @return entitas {@code Anggota} yang dibuat, atau null bila pegawai tidak ditemukan
	 */
	public static Anggota checkApakahPegawaiOtomatisMenjadiAnggotaPerpustakaan(String nidn) {

		Session session = HibernateUtil.currentNativeSession();
		Anggota anggota = ((Anggota) session.createCriteria(Anggota.class)
				.createAlias("pegawai", "pegawai", Criteria.LEFT_JOIN)
				.add(Restrictions.or(Restrictions.eq("pegawai.mycode", nidn), Restrictions.sqlRestriction(
						"replace(trim(this_.kode),'.','') = replace(trim('" + nidn.replaceAll("'", "") + "'),'.','')")))
				.setMaxResults(1).uniqueResult());
		if (anggota == null) {
			Pegawai pegawai = (Pegawai) session.createCriteria(Pegawai.class).add(Restrictions.eq("mycode", nidn))
					.setMaxResults(1).uniqueResult();
			if (pegawai != null) {
				anggota = new Anggota();
				anggota.setAktif(true);
				anggota.setAlamat(pegawai.getAlamat());
				anggota.setEmail(pegawai.getEmail());
				anggota.setJenisAnggota(LibraryUtil.ANGGOTA_REGULER);
				anggota.setJenisIdentitasAnggota(LibraryUtil.NIK);
				anggota.setJenisIdentitas("NIK");
				anggota.setKeterangan("Anggota ini mendaftar otomatis");
				anggota.setKodeIdentitas(pegawai.getMycode());
				anggota.setKode(pegawai.getMycode());
				anggota.setNama(pegawai.getNama());
				anggota.setPerpustakaan(null);
				anggota.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
				anggota.setPegawai(pegawai);
				anggota.setTipeAnggota(LibraryUtil.PEGAWAI);
				anggota.setTipe(LibraryUtil.PEGAWAI.getNama());
				session.getTransaction().begin();
				session.save(anggota);
				session.getTransaction().commit();
			}
			// System.out.println("=== Simpan anggota pegawai " + anggota + "
			// ===");
		}

		HibernateUtil.closeSession();
		return anggota;
	}

	/**
	 * Mendaftarkan dosen (berdasarkan NIDN) secara otomatis sebagai anggota perpustakaan
	 * bila belum terdaftar.
	 *
	 * <p><b>Tujuan.</b> Memastikan setiap dosen aktif yang login ke sistem secara otomatis
	 * memiliki kartu anggota perpustakaan tanpa registrasi manual.</p>
	 *
	 * <p><b>Cara kerja (inline).</b>
	 * <ol>
	 *   <li>Buka {@code currentNativeSession()}.</li>
	 *   <li>Cari {@code Anggota} yang terhubung ke dosen dengan NIDN tersebut, atau kode
	 *       anggota cocok (fuzzy match hapus titik).</li>
	 *   <li>Bila belum ada, cari {@code Dosen} dengan NIDN tersebut.</li>
	 *   <li>Bila ditemukan: buat {@code Anggota} baru dengan jenis identitas NIDN, tipe DOSEN,
	 *       simpan dan commit.</li>
	 *   <li>Tutup sesi.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan.</b> Pola sama dengan
	 * {@link #checkApakahPegawaiOtomatisMenjadiAnggotaPerpustakaan}; tipe anggota
	 * diset ke {@code LibraryUtil.DOSEN} dan jenis identitas ke {@code LibraryUtil.NIDN}.</p>
	 *
	 * @param nidn NIDN dosen yang akan didaftarkan; tidak boleh null
	 * @return entitas {@code Anggota} yang dibuat, atau null bila dosen tidak ditemukan
	 */
	public static Anggota checkApakahDosenOtomatisMenjadiAnggotaPerpustakaan(String nidn) {
		Session session = HibernateUtil.currentNativeSession();
		Anggota anggota = ((Anggota) session.createCriteria(Anggota.class)
				.createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
				.add(Restrictions.or(Restrictions.eq("dosen.nidn", nidn), Restrictions.sqlRestriction(
						"replace(trim(this_.kode),'.','') = replace(trim('" + nidn.replaceAll("'", "") + "'),'.','')")))
				.setMaxResults(1).uniqueResult());
		if (anggota == null) {
			Dosen dosen = (Dosen) session.createCriteria(Dosen.class).add(Restrictions.eq("nidn", nidn))
					.setMaxResults(1).uniqueResult();
			if (dosen != null) {
				anggota = new Anggota();
				anggota.setAktif(true);
				anggota.setAlamat(dosen.getAlamat());
				anggota.setEmail(dosen.getEmail());
				anggota.setJenisAnggota(LibraryUtil.ANGGOTA_REGULER);
				anggota.setJenisIdentitasAnggota(LibraryUtil.NIDN);
				anggota.setJenisIdentitas("NIDN");
				anggota.setKeterangan("Anggota ini mendaftar otomatis");
				anggota.setKodeIdentitas(dosen.getNidn());
				anggota.setKode(dosen.getNidn());
				anggota.setNama(dosen.getNama());
				anggota.setPerpustakaan(null);
				anggota.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
				anggota.setDosen(dosen);
				anggota.setTipeAnggota(LibraryUtil.DOSEN);
				anggota.setTipe(LibraryUtil.DOSEN.getNama());
				session.getTransaction().begin();
				session.save(anggota);
				session.getTransaction().commit();
			}
			// System.out.println("=== Simpan anggota dosen " + anggota + "
			// ===");
		}

		HibernateUtil.closeSession();
		return anggota;
	}

	/**
	 * Mendaftarkan siswa sekolah secara otomatis sebagai anggota perpustakaan, atau memperbarui
	 * data anggota yang sudah ada dengan relasi ke entitas {@code Siswa}.
	 *
	 * <p><b>Tujuan.</b> Memastikan setiap siswa sekolah yang login ke sistem otomatis dapat
	 * mengakses layanan perpustakaan (peminjaman buku, dll.) tanpa registrasi manual.</p>
	 *
	 * <p><b>Cara kerja (inline).</b>
	 * <ol>
	 *   <li>Ambil NIS (nomorInduk) dan NISN (nomorIndukNasional) dari {@code siswa}.</li>
	 *   <li>Buka {@code currentNativeSession()}.</li>
	 *   <li>Cari {@code Anggota} yang sudah terhubung ke NISN atau NIS siswa ini (multi-kriteria
	 *       via OR bersarang).</li>
	 *   <li>Bila belum ada: buat baru, isi semua field (jenis NIS, tipe SISWA, dsb.), simpan.</li>
	 *   <li>Bila sudah ada: update entitas yang ada (set relasi siswa, jenis identitas, keterangan),
	 *       update.</li>
	 *   <li>Commit dan tutup sesi.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Perbedaan dari versi mahasiswa/pegawai/dosen.</b> Versi ini menerima entitas
	 * {@code Siswa} langsung (bukan hanya kode), dan melakukan update bila anggota sudah ada
	 * (versi lain hanya membuat bila belum ada).</p>
	 *
	 * @param siswa entitas siswa yang akan didaftarkan; tidak boleh null
	 * @return entitas {@code Anggota} yang dibuat/diperbarui
	 */
	public static Anggota checkApakahSiswaOtomatisMenjadiAnggotaPerpustakaan(Siswa siswa) {

		String nim = siswa.getNomorInduk();
		String nim1 = siswa.getNomorIndukNasional();
		Session session = HibernateUtil.currentNativeSession();
		Anggota anggota = (Anggota) session.createCriteria(Anggota.class)
				.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
				.add(Restrictions.or(
						Restrictions.sqlRestriction(
								"replace(trim(this_.kode),'.','') = replace(trim('" + nim1 + "'),'.','')"),
						Restrictions.or(Restrictions.eq("siswa.nomorIndukNasional", nim1),
								Restrictions.or(Restrictions.eq("siswa.nomorInduk", nim), Restrictions.sqlRestriction(
										"replace(trim(this_.kode),'.','') = replace(trim('" + nim + "'),'.','')")))))
				.setMaxResults(1).uniqueResult();
		if (anggota == null) {
			anggota = new Anggota();
			anggota.setAktif(true);
			anggota.setAlamat(siswa.getAlamatSiswa());
			anggota.setEmail(siswa.getAlamatEmail());
			anggota.setJenisAnggota(LibraryUtil.ANGGOTA_REGULER);
			anggota.setJenisIdentitasAnggota(LibraryUtil.NIS);
			anggota.setJenisIdentitas("NIS");
			anggota.setKeterangan("Anggota ini mendaftar otomatis");
			anggota.setKodeIdentitas(siswa.getNomorInduk());
			anggota.setKode(siswa.getNomorInduk());
			anggota.setNama(siswa.getNama());
			anggota.setPerpustakaan(null);
			anggota.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
			anggota.setSiswa(siswa);
			anggota.setTipeAnggota(LibraryUtil.SISWA);
			anggota.setTipe(LibraryUtil.SISWA.getNama());
			session.getTransaction().begin();
			session.save(anggota);
			session.getTransaction().commit();

		} else {
			anggota.setSiswa(siswa);
			anggota.setJenisIdentitasAnggota(LibraryUtil.NIS);
			anggota.setJenisIdentitas("NIS");
			anggota.setKeterangan("Anggota ini mendaftar otomatis");
			session.getTransaction().begin();
			session.update(anggota);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return anggota;
	}

	/**
	 * Mendaftarkan mahasiswa (berdasarkan NIM) secara otomatis sebagai anggota koperasi
	 * yang diberikan, atau memperbarui data anggota yang sudah ada.
	 *
	 * <p><b>Tujuan.</b> Memastikan mahasiswa aktif otomatis menjadi anggota koperasi institusi
	 * (misal simpan-pinjam mahasiswa) saat login, sehingga bisa bertransaksi langsung.</p>
	 *
	 * <p><b>Cara kerja (inline).</b>
	 * <ol>
	 *   <li>Ambil {@code currentNativeSession()} (session bersama, TIDAK dibuka/ditutup di sini).</li>
	 *   <li>Cari {@code AnggotaKoperasi} yang terhubung ke NIM mahasiswa (fuzzy match hapus
	 *       titik dan koma; nilai NIM di-escape thd kutip-satu sebelum masuk sqlRestriction).</li>
	 *   <li>Cari {@code Mahasiswa} aktif dengan NIM yang sama; null bila tak ditemukan.</li>
	 *   <li>Buat atau perbarui {@code AnggotaKoperasi} — SEMUA field disegarkan baik saat membuat
	 *       maupun memperbarui (koperasi, tipe MAHASISWA, kode=NIM, alamat, email, dsb.), commit
	 *       per-record dengan rollback otomatis bila gagal.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Manajemen sesi (KONTRAK PENTING).</b> Method ini {@code TIDAK} menutup/disconnect
	 * session — dipanggil BERULANG dari loop batch sinkronisasi (satu panggilan per mahasiswa) oleh
	 * {@code AnggotaKoperasiAction} (ZK) dan endpoint JSP sinkronisasi; menutup session di sini akan
	 * memaksa {@code currentNativeSession()} membuka koneksi baru pada SETIAP baris batch (mahal,
	 * berisiko menghabiskan pool c3p0 pada batch besar — bug yang pernah terjadi). PEMANGGIL wajib
	 * membuka session sekali sebelum loop dan menutup sekali di {@code finally} setelah loop
	 * selesai.</p>
	 *
	 * @param nim      NIM mahasiswa yang akan didaftarkan; null/kosong → langsung return null
	 * @param koperasi koperasi tujuan pendaftaran; null → langsung return null
	 * @return entitas {@code AnggotaKoperasi} yang dibuat/diperbarui, atau null bila mahasiswa/NIM/koperasi tidak valid
	 */
	public static AnggotaKoperasi checkApakahMahasiswaOtomatisMenjadiAnggotaKoperasi(String nim, Koperasi koperasi) {

		if (nim == null || nim.trim().length() == 0 || koperasi == null) {
			return null;
		}
		// Escape kutip-satu sebelum disisipkan ke sqlRestriction -- NIM kotor yg mengandung ' tak
		// lagi merusak SQL & menghentikan seluruh batch sinkronisasi (lihat pemanggil di
		// AnggotaKoperasiAction / sinkron_anggota_koperasi_service.jsp).
		String nimEsc = nim.replace("'", "''");

		Session session = HibernateUtil.currentNativeSession();
		AnggotaKoperasi anggotaKoperasi = ((AnggotaKoperasi) ConstantValues.simpleObject(
				session.createCriteria(AnggotaKoperasi.class).createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
						.add(Restrictions.or(Restrictions.eq("mahasiswa.nim", nim),
								Restrictions.sqlRestriction(
										"replace(replace(trim(this_.kode),'.',''),',','') = replace(replace(trim('"
												+ nimEsc + "'),'.',''),',','')")))

						.setMaxResults(1),
				AnggotaKoperasi.class));

		Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.simpleObject(session.createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions
						.sqlRestriction("replace(replace(trim(this_.nim),'.',''),',','') = replace(replace(trim('" + nimEsc
								+ "'),'.',''),',','')"))
				.setMaxResults(1), Mahasiswa.class);
		if (mahasiswa == null) {
			return null;
		}
		if (anggotaKoperasi == null) {
			anggotaKoperasi = new AnggotaKoperasi();
			anggotaKoperasi.setAktif(true);
		}
		anggotaKoperasi.setMahasiswa(mahasiswa);
		anggotaKoperasi.setAlamat(mahasiswa.getAlamat());
		anggotaKoperasi.setEmail(mahasiswa.getEmail());
		anggotaKoperasi.setJenisIdentitas("NIM");
		anggotaKoperasi.setKeterangan("Anggota Koperasi ini mendaftar otomatis");
		anggotaKoperasi.setKodeIdentitas(mahasiswa.getNim());
		anggotaKoperasi.setKode(mahasiswa.getNim());
		anggotaKoperasi.setNama(mahasiswa.getNama());
		anggotaKoperasi.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
		anggotaKoperasi.setTipeAnggotaKoperasi(ConstantValues.MAHASISWA);
		anggotaKoperasi.setTipe(ConstantValues.MAHASISWA.getNama());
		anggotaKoperasi.setKoperasi(koperasi);
		session.getTransaction().begin();
		try {
			Common.refreshSaveOrUpdate(session, anggotaKoperasi);
			session.getTransaction().commit();
		} catch (RuntimeException ex) {
			try {
				session.getTransaction().rollback();
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/common/Common.java:12094");
			}
			throw ex;
		}
		// CATATAN KONTRAK SESSION: method ini SENGAJA TIDAK menutup session -- dipanggil BERULANG
		// dari loop batch sinkronisasi (satu kali per mahasiswa). Menutup di sini memaksa
		// currentNativeSession() membuka+menutup koneksi baru pada SETIAP baris (mahal & rawan
		// menghabiskan pool c3p0 pada batch besar). Pemanggil (ZK Action / JSP service) WAJIB
		// membuka session SEKALI sebelum loop dan menutup SEKALI di finally setelah loop selesai.
		return anggotaKoperasi;
	}

	/**
	 * Mendaftarkan pegawai (berdasarkan kode/NIK) secara otomatis sebagai anggota koperasi,
	 * atau memperbarui asosiasi koperasi pada rekam anggota yang sudah ada.
	 *
	 * <p><b>Tujuan.</b> Memastikan pegawai aktif bisa menjadi anggota koperasi (misal
	 * koperasi karyawan) tanpa pendaftaran manual.</p>
	 *
	 * <p><b>Cara kerja (inline).</b>
	 * <ol>
	 *   <li>Buka {@code currentNativeSession()}.</li>
	 *   <li>Cari {@code AnggotaKoperasi} yang terhubung ke pegawai dengan kode {@code nidn}.</li>
	 *   <li>Bila belum ada: cari {@code Pegawai}, buat entitas baru, simpan.</li>
	 *   <li>Bila sudah ada: perbarui asosiasi koperasi, simpan.</li>
	 *   <li>Tutup sesi (disconnect + close + closeSession).</li>
	 * </ol>
	 * </p>
	 *
	 * @param nidn     kode/NIK pegawai yang akan didaftarkan; tidak boleh null
	 * @param koperasi koperasi tujuan; tidak boleh null
	 * @return entitas {@code AnggotaKoperasi} yang dibuat/diperbarui, atau null bila pegawai tidak ditemukan
	 */
	public static AnggotaKoperasi checkApakahPegawaiOtomatisMenjadiAnggotaKoperasi(String nidn, Koperasi koperasi) {

		Session session = HibernateUtil.currentNativeSession();
		AnggotaKoperasi anggotaKoperasi = ((AnggotaKoperasi) ConstantValues.simpleObject(
				session.createCriteria(AnggotaKoperasi.class).createAlias("pegawai", "pegawai", Criteria.LEFT_JOIN)
						.add(Restrictions.or(Restrictions.eq("pegawai.mycode", nidn),
								Restrictions.sqlRestriction("replace(trim(this_.kode),'.','') = replace(trim('"
										+ nidn.replaceAll("'", "") + "'),'.','')")))
						.setMaxResults(1),
				AnggotaKoperasi.class));
		if (anggotaKoperasi == null) {
			Pegawai pegawai = (Pegawai) ConstantValues.simpleObject(
					session.createCriteria(Pegawai.class).add(Restrictions.eq("mycode", nidn)).setMaxResults(1),
					Pegawai.class);
			if (pegawai != null) {
				anggotaKoperasi = new AnggotaKoperasi();
				anggotaKoperasi.setAktif(true);
				anggotaKoperasi.setPegawai(pegawai);
				anggotaKoperasi.setAlamat(pegawai.getAlamat());
				anggotaKoperasi.setEmail(pegawai.getEmail());
				anggotaKoperasi.setJenisIdentitas("NIK");
				anggotaKoperasi.setKeterangan("Anggota Koperasi ini mendaftar otomatis");
				anggotaKoperasi.setKodeIdentitas(pegawai.getMycode());
				anggotaKoperasi.setKode(pegawai.getMycode());
				anggotaKoperasi.setNama(pegawai.getNama());
				anggotaKoperasi.setKoperasi(null);
				anggotaKoperasi.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
				anggotaKoperasi.setPegawai(pegawai);
				anggotaKoperasi.setTipeAnggotaKoperasi(ConstantValues.PEGAWAI);
				anggotaKoperasi.setTipe(ConstantValues.PEGAWAI.getNama());
				anggotaKoperasi.setKoperasi(koperasi);
				session.getTransaction().begin();
				session.save(anggotaKoperasi);
				session.getTransaction().commit();
			}
			// System.out.println("=== Simpan anggotaKoperasi pegawai " + anggotaKoperasi +
			// "
			// ===");
		} else {
			anggotaKoperasi.setKoperasi(koperasi);
			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, anggotaKoperasi);
			session.getTransaction().commit();
		}
		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
		return anggotaKoperasi;
	}

	/**
	 * Padanan {@link #checkApakahPegawaiOtomatisMenjadiAnggotaKoperasi(String, Koperasi)} utk
	 * pegawai TANPA kode ({@code mycode} null/kosong) -- sebelumnya pegawai semacam ini
	 * dikecualikan total dari kandidat sinkronisasi (permintaan eksplisit user 2026-08-12:
	 * "tetap ambil saja, buat kode otomatis random/acak"), krn overload berbasis-String di atas
	 * memakai {@code mycode} sbg KUNCI PENCARIAN AnggotaKoperasi yang sudah ada -- kunci
	 * kosong/null tak bisa dipakai (banyak pegawai berbeda akan tabrakan pada satu kunci sama).
	 *
	 * <p>Overload ini mencari/mencocokkan lewat FK {@code pegawai.id} langsung (stabil, tak
	 * bergantung mycode), dan men-generate {@code kode} otomatis via
	 * {@link BarcodeCommon#generateCode()} (pola sama dgn fallback bawaan
	 * {@link AnggotaKoperasi#getKode()}, dgn retry-loop cek tabrakan thd {@code kode unique})
	 * HANYA saat pertama kali dibuat -- re-sync berikutnya cocok lewat {@code pegawai.id}, bukan
	 * generate kode baru lagi.</p>
	 *
	 * @param idPegawai id {@code Pegawai} yang akan didaftarkan; tidak boleh null
	 * @param koperasi  koperasi tujuan; tidak boleh null
	 * @return entitas {@code AnggotaKoperasi} yang dibuat/diperbarui, atau null bila pegawai tidak ditemukan
	 */
	public static AnggotaKoperasi checkApakahPegawaiOtomatisMenjadiAnggotaKoperasi(Long idPegawai, Koperasi koperasi) {
		Session session = HibernateUtil.currentNativeSession();
		AnggotaKoperasi anggotaKoperasi = ((AnggotaKoperasi) ConstantValues.simpleObject(
				session.createCriteria(AnggotaKoperasi.class).createAlias("pegawai", "pegawai", Criteria.LEFT_JOIN)
						.add(Restrictions.eq("pegawai.id", idPegawai)).setMaxResults(1),
				AnggotaKoperasi.class));
		if (anggotaKoperasi == null) {
			Pegawai pegawai = (Pegawai) session.get(Pegawai.class, idPegawai);
			if (pegawai != null) {
				String kodeOtomatis;
				do {
					kodeOtomatis = ais.common.BarcodeCommon.generateCode();
				} while (session.createCriteria(AnggotaKoperasi.class).add(Restrictions.eq("kode", kodeOtomatis))
						.setMaxResults(1).uniqueResult() != null);

				anggotaKoperasi = new AnggotaKoperasi();
				anggotaKoperasi.setAktif(true);
				anggotaKoperasi.setPegawai(pegawai);
				anggotaKoperasi.setAlamat(pegawai.getAlamat());
				anggotaKoperasi.setEmail(pegawai.getEmail());
				anggotaKoperasi.setJenisIdentitas("NIK");
				anggotaKoperasi.setKeterangan(
						"Anggota Koperasi ini mendaftar otomatis (kode auto-generate, pegawai tanpa kode)");
				anggotaKoperasi.setKodeIdentitas(kodeOtomatis);
				anggotaKoperasi.setKode(kodeOtomatis);
				anggotaKoperasi.setNama(pegawai.getNama());
				anggotaKoperasi.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
				anggotaKoperasi.setTipeAnggotaKoperasi(ConstantValues.PEGAWAI);
				anggotaKoperasi.setTipe(ConstantValues.PEGAWAI.getNama());
				anggotaKoperasi.setKoperasi(koperasi);
				session.getTransaction().begin();
				session.save(anggotaKoperasi);
				session.getTransaction().commit();
			}
		} else {
			anggotaKoperasi.setKoperasi(koperasi);
			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, anggotaKoperasi);
			session.getTransaction().commit();
		}
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
		return anggotaKoperasi;
	}

	/**
	 * Mendaftarkan dosen (berdasarkan NIDN) secara otomatis sebagai anggota koperasi,
	 * atau memperbarui asosiasi koperasi pada rekam anggota yang sudah ada.
	 *
	 * <p><b>Tujuan.</b> Memastikan dosen aktif bisa mengakses layanan koperasi dosen/karyawan
	 * institusi tanpa pendaftaran manual.</p>
	 *
	 * <p><b>Cara kerja (inline).</b>
	 * <ol>
	 *   <li>Buka {@code currentNativeSession()}.</li>
	 *   <li>Cari {@code AnggotaKoperasi} yang terhubung ke dosen dengan NIDN tersebut.</li>
	 *   <li>Bila belum ada: cari {@code Dosen}, buat entitas baru dengan tipe DOSEN, simpan.</li>
	 *   <li>Bila sudah ada: perbarui asosiasi koperasi, simpan.</li>
	 *   <li>Tutup sesi (disconnect + close + closeSession).</li>
	 * </ol>
	 * </p>
	 *
	 * @param nidn     NIDN dosen yang akan didaftarkan; tidak boleh null
	 * @param koperasi koperasi tujuan; tidak boleh null
	 * @return entitas {@code AnggotaKoperasi} yang dibuat/diperbarui, atau null bila dosen tidak ditemukan
	 */
	public static AnggotaKoperasi checkApakahDosenOtomatisMenjadiAnggotaKoperasi(String nidn, Koperasi koperasi) {
		Session session = HibernateUtil.currentNativeSession();
		AnggotaKoperasi anggotaKoperasi = ((AnggotaKoperasi) ConstantValues.simpleObject(
				session.createCriteria(AnggotaKoperasi.class).createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
						.add(Restrictions.or(Restrictions.eq("dosen.nidn", nidn),
								Restrictions.sqlRestriction("replace(trim(this_.kode),'.','') = replace(trim('"
										+ nidn.replaceAll("'", "") + "'),'.','')")))
						.setMaxResults(1),
				AnggotaKoperasi.class));
		if (anggotaKoperasi == null) {
			Dosen dosen = (Dosen) ConstantValues.simpleObject(
					session.createCriteria(Dosen.class).add(Restrictions.eq("nidn", nidn)).setMaxResults(1),
					Dosen.class);
			if (dosen != null) {
				anggotaKoperasi = new AnggotaKoperasi();
				anggotaKoperasi.setAktif(true);
				anggotaKoperasi.setDosen(dosen);
				anggotaKoperasi.setAlamat(dosen.getAlamat());
				anggotaKoperasi.setEmail(dosen.getEmail());
				anggotaKoperasi.setJenisIdentitas("NIDN");
				anggotaKoperasi.setKeterangan("Anggota Koperasi ini mendaftar otomatis");
				anggotaKoperasi.setKodeIdentitas(dosen.getNidn());
				anggotaKoperasi.setKode(dosen.getNidn());
				anggotaKoperasi.setNama(dosen.getNama());
				anggotaKoperasi.setKoperasi(null);
				anggotaKoperasi.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
				anggotaKoperasi.setDosen(dosen);
				anggotaKoperasi.setTipeAnggotaKoperasi(ConstantValues.DOSEN);
				anggotaKoperasi.setTipe(ConstantValues.DOSEN.getNama());
				anggotaKoperasi.setKoperasi(koperasi);
				session.getTransaction().begin();
				session.save(anggotaKoperasi);
				session.getTransaction().commit();
			}
			// System.out.println("=== Simpan anggotaKoperasi dosen " + anggotaKoperasi + "
			// ===");
		} else {
			anggotaKoperasi.setKoperasi(koperasi);
			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, anggotaKoperasi);
			session.getTransaction().commit();
		}
		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
		return anggotaKoperasi;
	}

	/**
	 * Mendaftarkan guru (berdasarkan NIP) secara otomatis sebagai anggota koperasi,
	 * atau memperbarui asosiasi koperasi pada rekam anggota yang sudah ada.
	 *
	 * <p><b>Tujuan.</b> Padanan {@link #checkApakahDosenOtomatisMenjadiAnggotaKoperasi(String, Koperasi)}
	 * utk jenjang sekolah -- memastikan guru aktif bisa mengakses layanan koperasi sekolah tanpa
	 * pendaftaran manual.</p>
	 *
	 * <p><b>Cara kerja (inline).</b> SAMA PERSIS strukturnya dgn versi Dosen (lihat javadoc method
	 * tsb), kunci identitas NIP menggantikan NIDN, dan {@code AnggotaKoperasi.guru} menggantikan
	 * {@code AnggotaKoperasi.dosen}.</p>
	 *
	 * @param nip      NIP guru yang akan didaftarkan; tidak boleh null
	 * @param koperasi koperasi tujuan; tidak boleh null
	 * @return entitas {@code AnggotaKoperasi} yang dibuat/diperbarui, atau null bila guru tidak ditemukan
	 */
	public static AnggotaKoperasi checkApakahGuruOtomatisMenjadiAnggotaKoperasi(String nip, ais.database.model.koperasi.Koperasi koperasi) {
		Session session = HibernateUtil.currentNativeSession();
		AnggotaKoperasi anggotaKoperasi = ((AnggotaKoperasi) ConstantValues.simpleObject(
				session.createCriteria(AnggotaKoperasi.class).createAlias("guru", "guru", Criteria.LEFT_JOIN)
						.add(Restrictions.or(Restrictions.eq("guru.nip", nip),
								Restrictions.sqlRestriction("replace(trim(this_.kode),'.','') = replace(trim('"
										+ nip.replaceAll("'", "") + "'),'.','')")))
						.setMaxResults(1),
				AnggotaKoperasi.class));
		if (anggotaKoperasi == null) {
			ais.database.model.sekolah.Guru guru = (ais.database.model.sekolah.Guru) ConstantValues.simpleObject(
					session.createCriteria(ais.database.model.sekolah.Guru.class).add(Restrictions.eq("nip", nip)).setMaxResults(1),
					ais.database.model.sekolah.Guru.class);
			if (guru != null) {
				anggotaKoperasi = new AnggotaKoperasi();
				anggotaKoperasi.setAktif(true);
				anggotaKoperasi.setGuru(guru);
				anggotaKoperasi.setAlamat(guru.getAlamatGuru());
				anggotaKoperasi.setEmail(guru.getAlamatEmail());
				anggotaKoperasi.setJenisIdentitas("NIP");
				anggotaKoperasi.setKeterangan("Anggota Koperasi ini mendaftar otomatis");
				anggotaKoperasi.setKodeIdentitas(guru.getNip());
				anggotaKoperasi.setKode(guru.getNip());
				anggotaKoperasi.setNama(guru.getNama());
				anggotaKoperasi.setKoperasi(null);
				anggotaKoperasi.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
				anggotaKoperasi.setGuru(guru);
				anggotaKoperasi.setTipeAnggotaKoperasi(ConstantValues.GURU);
				anggotaKoperasi.setTipe(ConstantValues.GURU.getNama());
				anggotaKoperasi.setKoperasi(koperasi);
				session.getTransaction().begin();
				session.save(anggotaKoperasi);
				session.getTransaction().commit();
			}
		} else {
			anggotaKoperasi.setKoperasi(koperasi);
			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, anggotaKoperasi);
			session.getTransaction().commit();
		}
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
		return anggotaKoperasi;
	}

	/**
	 * Menghasilkan tanda tangan HMAC-SHA512 dari nilai dan kunci rahasia yang diberikan,
	 * dikembalikan sebagai string heksadesimal huruf kecil.
	 *
	 * <p><b>Tujuan.</b> Menyediakan mekanisme verifikasi integritas data untuk komunikasi
	 * dengan payment gateway, webhook, atau API eksternal yang memerlukan HMAC-SHA512
	 * sebagai tanda tangan keamanan.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Buat instance {@code Mac} dengan algoritma "HmacSHA512".</li>
	 *   <li>Inisialisasi dengan {@code SecretKeySpec} dari byte-byte kunci rahasia {@code secret}.</li>
	 *   <li>Hitung digest HMAC dari byte-byte {@code value}.</li>
	 *   <li>Konversi hasil ke {@code BigInteger} lalu ke string hex basis 16.</li>
	 *   <li>Bila panjang string ganjil, tambahkan "0" di depan agar setiap byte terwakili
	 *       tepat 2 karakter hex.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error.</b> Semua exception (NoSuchAlgorithmException,
	 * InvalidKeyException, dsb.) dibungkus dan dilempar ulang sebagai
	 * {@code RuntimeException} — pemanggil tidak perlu menangkap checked exception.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Menggunakan charset default JVM untuk konversi String → byte[]
	 * (via {@code getBytes()} tanpa charset eksplisit). Untuk portabilitas yang lebih baik,
	 * pertimbangkan menambahkan {@code "UTF-8"} secara eksplisit. Nilai {@code secret} tidak
	 * boleh null atau kosong — keduanya akan menghasilkan HMAC yang tidak valid atau exception.</p>
	 *
	 * @param value  data/pesan yang akan ditandatangani; tidak boleh null
	 * @param secret kunci rahasia HMAC; tidak boleh null atau kosong
	 * @return string heksadesimal HMAC-SHA512 (selalu genap, misal 128 karakter)
	 * @throws RuntimeException bila algoritma tidak didukung atau kunci tidak valid
	 */
	public static String buildHmacSignature(String value, String secret) {
		String result;
		try {
			Mac hmacSHA512 = Mac.getInstance("HmacSHA512");
			SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA512");
			hmacSHA512.init(secretKeySpec);

			byte[] digest = hmacSHA512.doFinal(value.getBytes());
			BigInteger hash = new BigInteger(1, digest);
			result = hash.toString(16);
			if ((result.length() % 2) != 0) {
				result = "0" + result;
			}
		} catch (Exception ex) {
			throw new RuntimeException("Problemas calculando HMAC", ex);
		}
		return result;
	}

	/**
	 * Mendaftarkan siswa sekolah secara otomatis sebagai anggota koperasi, atau memperbarui
	 * data anggota yang sudah ada dengan asosiasi koperasi terbaru.
	 *
	 * <p><b>Tujuan.</b> Memastikan setiap siswa aktif yang login ke sistem bisa mengakses
	 * layanan koperasi sekolah (kantin, perlengkapan, simpan-pinjam) tanpa registrasi manual.</p>
	 *
	 * <p><b>Cara kerja (inline).</b>
	 * <ol>
	 *   <li>Ambil NIS dan NISN dari {@code siswa}; bila keduanya kosong, return null (tak ada
	 *       kunci identitas utk dedup/sinkron).</li>
	 *   <li>Ambil {@code currentNativeSession()} (session bersama, TIDAK dibuka/ditutup di sini).</li>
	 *   <li>Cari {@code AnggotaKoperasi} yang cocok dengan NIS atau NISN siswa (multi-kriteria,
	 *       nilai di-escape thd kutip-satu; normalisasi SAMA dgn versi mahasiswa — strip '.' DAN ',').</li>
	 *   <li>Buat baru ATAU perbarui — SEMUA field disegarkan di kedua jalur (SIMETRIS dgn versi
	 *       mahasiswa; sebelumnya jalur update hanya menyentuh 4 field sehingga perubahan
	 *       nama/alamat/email siswa tak pernah ikut ter-sinkron ulang), commit per-record dengan
	 *       rollback otomatis bila gagal.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Manajemen sesi (KONTRAK PENTING).</b> SAMA seperti
	 * {@link #checkApakahMahasiswaOtomatisMenjadiAnggotaKoperasi(String, Koperasi)} — method ini
	 * {@code TIDAK} menutup/disconnect session; pemanggil (loop batch ZK/JSP) wajib membuka sekali
	 * sebelum loop, menutup sekali di {@code finally} setelah loop selesai.</p>
	 *
	 * @param siswa    entitas siswa yang akan didaftarkan; null → langsung return null
	 * @param koperasi koperasi tujuan; null → langsung return null
	 * @return entitas {@code AnggotaKoperasi} yang dibuat/diperbarui, atau null bila siswa/NIS-NISN/koperasi tidak valid
	 */
	public static synchronized AnggotaKoperasi checkApakahSiswaOtomatisMenjadiAnggotaKoperasi(Siswa siswa, Koperasi koperasi) {

		if (siswa == null || koperasi == null) {
			return null;
		}
		String nim = siswa.getNomorInduk();
		String nim1 = siswa.getNomorIndukNasional();
		if ((nim == null || nim.trim().length() == 0) && (nim1 == null || nim1.trim().length() == 0)) {
			return null; // tak ada NIS/NISN -- tak ada kunci utk dedup/sinkron
		}
		// Escape kutip-satu (lihat catatan di checkApakahMahasiswaOtomatisMenjadiAnggotaKoperasi) +
		// SAMAKAN normalisasi dgn versi mahasiswa (strip '.' DAN ',', sebelumnya cuma '.').
		String nimEsc = nim == null ? "" : nim.replace("'", "''");
		String nim1Esc = nim1 == null ? "" : nim1.replace("'", "''");

		Session session = HibernateUtil.currentNativeSession();
		AnggotaKoperasi anggotaKoperasi = ((AnggotaKoperasi) ConstantValues.simpleObject(session
				.createCriteria(AnggotaKoperasi.class).createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
				.add(Restrictions.or(
						Restrictions.sqlRestriction(
								"replace(replace(trim(this_.kode),'.',''),',','') = replace(replace(trim('" + nim1Esc
										+ "'),'.',''),',','')"),
						Restrictions.or(Restrictions.eq("siswa.nomorIndukNasional", nim1),
								Restrictions.or(Restrictions.eq("siswa.nomorInduk", nim), Restrictions.sqlRestriction(
										"replace(replace(trim(this_.kode),'.',''),',','') = replace(replace(trim('"
												+ nimEsc + "'),'.',''),',','')")))))
				.setMaxResults(1), AnggotaKoperasi.class));

		if (anggotaKoperasi == null) {
			anggotaKoperasi = new AnggotaKoperasi();
			anggotaKoperasi.setAktif(true);
		}
		// SIMETRIS dgn versi mahasiswa: SEMUA field disegarkan baik saat membuat baru MAUPUN saat
		// memperbarui data yg sudah ada -- sebelumnya jalur "update" hanya menyentuh 4 field
		// (koperasi/siswa/jenisIdentitas/keterangan), sehingga perubahan nama/alamat/email siswa
		// TIDAK PERNAH ikut ter-sinkron ulang meski tombol "Singkronkan Siswa" diklik lagi.
		anggotaKoperasi.setSiswa(siswa);
		anggotaKoperasi.setAlamat(siswa.getAlamatSiswa());
		anggotaKoperasi.setEmail(siswa.getAlamatEmail());
		anggotaKoperasi.setJenisIdentitas("NIS");
		anggotaKoperasi.setKeterangan("Anggota Koperasi ini mendaftar otomatis");
		anggotaKoperasi.setKodeIdentitas(siswa.getNomorInduk());
		anggotaKoperasi.setKode(siswa.getNomorInduk());
		anggotaKoperasi.setNama(siswa.getNama());
		anggotaKoperasi.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
		anggotaKoperasi.setTipeAnggotaKoperasi(ConstantValues.SISWA);
		anggotaKoperasi.setTipe(ConstantValues.SISWA.getNama());
		anggotaKoperasi.setKoperasi(koperasi);
		session.getTransaction().begin();
		try {
			// SAMAKAN dgn versi mahasiswa: refreshSaveOrUpdate (bukan session.save/update mentah) --
			// aman thd entitas yg sempat di-load/detach di session lain dlm batch yg sama.
			Common.refreshSaveOrUpdate(session, anggotaKoperasi);
			session.getTransaction().commit();
		} catch (RuntimeException ex) {
			try {
				session.getTransaction().rollback();
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/common/Common.java:12391");
			}
			throw ex;
		}
		// CATATAN KONTRAK SESSION: SAMA seperti versi mahasiswa -- TIDAK menutup session di sini.
		// Pemanggil (loop batch) WAJIB membuka session sekali sebelum loop, tutup sekali di finally.
		return anggotaKoperasi;
	}

	/**
	 * Mendaftarkan mahasiswa sebagai peminjam surat perpustakaan secara otomatis
	 * bila belum terdaftar, menggunakan NIM sebagai kunci identifikasi.
	 *
	 * <p><b>Tujuan.</b> Memungkinkan mahasiswa langsung meminjam surat keterangan atau
	 * dokumen dari perpustakaan tanpa registrasi awal sebagai peminjam surat.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonLibraryAutoHelper.checkApakahMahasiswaOtomatisMenjadiPeminjamSuratPerpustakaan}.</p>
	 *
	 * @param nim NIM mahasiswa; tidak boleh null
	 * @return entitas {@code PeminjamSurat} yang dibuat/ditemukan, atau null
	 */
	public static PeminjamSurat checkApakahMahasiswaOtomatisMenjadiPeminjamSuratPerpustakaan(String nim) {
		return CommonLibraryAutoHelper.checkApakahMahasiswaOtomatisMenjadiPeminjamSuratPerpustakaan(nim);
	}

	/**
	 * Mendaftarkan pegawai sebagai peminjam surat perpustakaan secara otomatis
	 * bila belum terdaftar, menggunakan kode pegawai sebagai kunci identifikasi.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonLibraryAutoHelper.checkApakahPegawaiOtomatisMenjadiPeminjamSuratPerpustakaan}.</p>
	 *
	 * @param nidn kode/NIK pegawai; tidak boleh null
	 * @return entitas {@code PeminjamSurat} yang dibuat/ditemukan, atau null
	 */
	public static PeminjamSurat checkApakahPegawaiOtomatisMenjadiPeminjamSuratPerpustakaan(String nidn) {
		return CommonLibraryAutoHelper.checkApakahPegawaiOtomatisMenjadiPeminjamSuratPerpustakaan(nidn);
	}

	/**
	 * Mendaftarkan dosen sebagai peminjam surat perpustakaan secara otomatis
	 * bila belum terdaftar, menggunakan NIDN sebagai kunci identifikasi.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonLibraryAutoHelper.checkApakahDosenOtomatisMenjadiPeminjamSuratPerpustakaan}.</p>
	 *
	 * @param nidn NIDN dosen; tidak boleh null
	 * @return entitas {@code PeminjamSurat} yang dibuat/ditemukan, atau null
	 */
	public static PeminjamSurat checkApakahDosenOtomatisMenjadiPeminjamSuratPerpustakaan(String nidn) {
		return CommonLibraryAutoHelper.checkApakahDosenOtomatisMenjadiPeminjamSuratPerpustakaan(nidn);
	}

	/**
	 * Mendaftarkan siswa sekolah sebagai peminjam surat perpustakaan secara otomatis
	 * bila belum terdaftar.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonLibraryAutoHelper.checkApakahSiswaOtomatisMenjadiPeminjamSuratPerpustakaan}.</p>
	 *
	 * @param siswa entitas siswa; tidak boleh null
	 * @return entitas {@code PeminjamSurat} yang dibuat/ditemukan, atau null
	 */
	public static PeminjamSurat checkApakahSiswaOtomatisMenjadiPeminjamSuratPerpustakaan(Siswa siswa) {
		return CommonLibraryAutoHelper.checkApakahSiswaOtomatisMenjadiPeminjamSuratPerpustakaan(siswa);
	}

	/**
	 * Mendaftarkan mahasiswa sebagai peserta kursus perpustakaan secara otomatis
	 * bila belum terdaftar.
	 *
	 * <p><b>Tujuan.</b> Memastikan mahasiswa dapat mengikuti program kursus/pelatihan
	 * yang diselenggarakan perpustakaan tanpa perlu registrasi terpisah.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonLibraryAutoHelper.checkApakahMahasiswaOtomatisMenjadiPesertaKursusPerpustakaan}.</p>
	 *
	 * @param nim NIM mahasiswa; tidak boleh null
	 * @return entitas {@code PesertaKursus} yang dibuat/ditemukan, atau null
	 */
	public static PesertaKursus checkApakahMahasiswaOtomatisMenjadiPesertaKursusPerpustakaan(String nim) {
		return CommonLibraryAutoHelper.checkApakahMahasiswaOtomatisMenjadiPesertaKursusPerpustakaan(nim);
	}

	/**
	 * Mendaftarkan pegawai sebagai peserta kursus perpustakaan secara otomatis
	 * bila belum terdaftar.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonLibraryAutoHelper.checkApakahPegawaiOtomatisMenjadiPesertaKursusPerpustakaan}.</p>
	 *
	 * @param nidn kode/NIK pegawai; tidak boleh null
	 * @return entitas {@code PesertaKursus} yang dibuat/ditemukan, atau null
	 */
	public static PesertaKursus checkApakahPegawaiOtomatisMenjadiPesertaKursusPerpustakaan(String nidn) {
		return CommonLibraryAutoHelper.checkApakahPegawaiOtomatisMenjadiPesertaKursusPerpustakaan(nidn);
	}

	/**
	 * Mendaftarkan dosen sebagai peserta kursus perpustakaan secara otomatis
	 * bila belum terdaftar.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonLibraryAutoHelper.checkApakahDosenOtomatisMenjadiPesertaKursusPerpustakaan}.</p>
	 *
	 * @param nidn NIDN dosen; tidak boleh null
	 * @return entitas {@code PesertaKursus} yang dibuat/ditemukan, atau null
	 */
	public static PesertaKursus checkApakahDosenOtomatisMenjadiPesertaKursusPerpustakaan(String nidn) {
		return CommonLibraryAutoHelper.checkApakahDosenOtomatisMenjadiPesertaKursusPerpustakaan(nidn);
	}

	/**
	 * Mendaftarkan siswa sekolah sebagai peserta kursus perpustakaan secara otomatis
	 * bila belum terdaftar.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonLibraryAutoHelper.checkApakahSiswaOtomatisMenjadiPesertaKursusPerpustakaan}.</p>
	 *
	 * @param siswa entitas siswa; tidak boleh null
	 * @return entitas {@code PesertaKursus} yang dibuat/ditemukan, atau null
	 */
	public static PesertaKursus checkApakahSiswaOtomatisMenjadiPesertaKursusPerpustakaan(Siswa siswa) {
		return CommonLibraryAutoHelper.checkApakahSiswaOtomatisMenjadiPesertaKursusPerpustakaan(siswa);
	}

	/**
	 * Menampilkan popup daftar tugas akhir (skripsi/thesis/disertasi) mahasiswa.
	 *
	 * <p><b>Tujuan.</b> Menyediakan shortcut UI dari berbagai halaman untuk membuka
	 * panel tugas akhir tanpa navigasi manual ke menu khusus.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonUiFactoryHelper.tampilkanTugasAkhir()}.</p>
	 *
	 * @throws Exception bila terjadi kesalahan saat membuka popup ZK
	 */
	public static void tampilkanTugasAkhir() throws Exception {
		CommonUiFactoryHelper.tampilkanTugasAkhir();
	}

	/**
	 * Menampilkan popup daftar skripsi mahasiswa (semua status: berjalan, selesai, dll.).
	 *
	 * <p><b>Tujuan.</b> Menyediakan akses cepat ke daftar skripsi dari berbagai konteks UI.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonUiFactoryHelper.tampilkanDaftarSkripsi()}.</p>
	 *
	 * @throws Exception bila terjadi kesalahan saat membuka popup ZK
	 */
	public static void tampilkanDaftarSkripsi() throws Exception {
		CommonUiFactoryHelper.tampilkanDaftarSkripsi();
	}

	/**
	 * Mengirimkan email lupa password kepada pengguna yang memiliki username/email yang diberikan.
	 *
	 * <p><b>Tujuan.</b> Memungkinkan pengguna mereset password mereka sendiri melalui email,
	 * tanpa perlu bantuan admin — fungsi self-service "Lupa Password".</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.kirimLupaPassword(username)}.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Implementasi di helper menentukan format email, konten, dan
	 * mekanisme reset (link token atau password sementara). Pastikan konfigurasi SMTP
	 * ({@code MailSender}) sudah benar sebelum memanggil metode ini.</p>
	 *
	 * @param username username atau email pengguna yang ingin mereset password
	 * @return pesan status pengiriman email (misal "Email berhasil dikirim" atau pesan error)
	 * @throws Exception bila terjadi kegagalan pengiriman email atau username tidak ditemukan
	 */
	public static String kirimLupaPassword(String username) throws Exception {
		return CommonSecurityLoginHelper.kirimLupaPassword(username);
	}


	/**
	 * Memeriksa apakah mahasiswa memenuhi syarat akademis (SKS dan IPK minimum) untuk
	 * mendaftar program KKN (Kuliah Kerja Nyata) tertentu.
	 *
	 * <p><b>Tujuan.</b> Memvalidasi kelayakan mahasiswa sebelum mendaftarkan diri ke KKN,
	 * sehingga hanya mahasiswa yang memenuhi prasyarat yang bisa mendaftar. Menampilkan
	 * pesan kesalahan via messagebox bila tidak memenuhi syarat.</p>
	 *
	 * <p><b>Cara kerja (inline — logika kompleks).</b>
	 * <ol>
	 *   <li>Cek apakah mahasiswa ada di daftar pengecualian ({@code PengecualianKknMahasiswa});
	 *       bila ya, langsung kembalikan {@code true}.</li>
	 *   <li>Cek kesesuaian jurusan: bila KKN dibatasi ke jurusan tertentu dan jurusan mahasiswa
	 *       berbeda, tampilkan pesan error dan kembalikan {@code false}.</li>
	 *   <li>Cek kesesuaian fakultas: sama seperti jurusan.</li>
	 *   <li>Hitung semester mahasiswa saat ini via {@code Common.getSemester()}.</li>
	 *   <li>Sinkronisasi KRS ({@code singkronkanKrsMahasiswa}) untuk mendapatkan total SKS
	 *       kumulatif ({@code sksk}) dan IPK.</li>
	 *   <li>Bandingkan SKS dan IPK dengan {@code kkn.getMinimalSksBolehIkutKkn()} dan
	 *       {@code kkn.getMinimalIpkBolehIkutKkn()}; bila terpenuhi kembalikan {@code true}.</li>
	 *   <li>Bila tidak, cek syarat alternatif ({@code aktifkanSyaratLain}) dengan batas minimum
	 *       yang berbeda. Bila tetap tidak terpenuhi, tampilkan pesan SKS/IPK kurang dan
	 *       kembalikan {@code false}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan.</b> Pesan error hardcode bahasa Indonesia. Syarat jurusan/fakultas
	 * diambil dari entitas {@code Kkn}; label "Jurusan"/"Fakultas" diambil dari
	 * {@code getBahasaConfig} untuk mendukung multi-bahasa.</p>
	 *
	 * @param mahasiswa mahasiswa yang akan didaftarkan; tidak boleh null
	 * @param kkn       program KKN yang ingin diikuti; tidak boleh null
	 * @return {@code true} bila memenuhi syarat atau ada pengecualian, {@code false} sebaliknya
	 * @throws Exception bila terjadi kesalahan sesi atau kalkulasi KRS
	 */
	public static Boolean checkSyaratKkn(Mahasiswa mahasiswa, Kkn kkn) throws Exception {

		Session session = HibernateUtil.currentSession();
		int kecuali = ((Number) session.createCriteria(PengecualianKknMahasiswa.class).add(Restrictions.eq("kkn", kkn))
				.add(Restrictions.eq("mahasiswa", mahasiswa)).setProjection(Projections.rowCount()).uniqueResult())
				.intValue();
		if (kecuali > 0) {
			return true;
		}

		if (kkn != null && kkn.getJurusan() != null && mahasiswa != null && mahasiswa.getJurusan() != null
				&& !mahasiswa.getJurusan().getId().equals(kkn.getJurusan().getId())) {
			MyMessageboxConfig.show(
					"Mahasiswa dengan NIM " + mahasiswa.getNim() + " dengan " + Common.getBahasaConfig("Jurusan") + " "
							+ mahasiswa.getJurusan().getNama() + " tidak bisa mendaftar di KKN di "
							+ Common.getBahasaConfig("Jurusan") + " " + kkn.getJurusan().getNama(),
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (kkn != null && kkn.getFakultas() != null && mahasiswa != null && mahasiswa.getJurusan() != null
				&& mahasiswa.getJurusan().getFakultas() != null
				&& !mahasiswa.getJurusan().getFakultas().getId().equals(kkn.getFakultas().getId())) {
			MyMessageboxConfig.show(
					"Mahasiswa dengan NIM " + mahasiswa.getNim() + " dengan " + Common.getBahasaConfig("Fakultas") + " "
							+ mahasiswa.getJurusan().getFakultas().getNama() + " tidak bisa mendaftar di KKN di "
							+ Common.getBahasaConfig("Fakultas") + " " + kkn.getFakultas().getNama(),
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		String ta = kkn.getTahunAkademik();
		Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
		Integer currentSemester = Common.getSemester(mahasiswa.getTahunangkatan(), kkn.getSemester(),
				mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, currentSemester, null, null);

		int sks = krsMahasiswa.getSksk();
		double ipk = krsMahasiswa.getIpk();

		if (sks >= kkn.getMinimalSksBolehIkutKkn() && ipk >= kkn.getMinimalIpkBolehIkutKkn()) {
			return true;
		} else {

			if (kkn.getAktifkanSyaratLain()) {
				if (sks >= kkn.getMinimalSksBolehIkutKkn2() && ipk >= kkn.getMinimalIpkBolehIkutKkn2()) {
					return true;
				}
			}

			MyMessageboxConfig.show(
					"Jumlah SKS atau IPK mahasiswa dengan NIM " + mahasiswa.getNim() + " dan nama "
							+ mahasiswa.getNama() + " belum memenuhi syarat.\n\nSKS Mahasiswa : " + sks
							+ "\nIPK Mahasiswa " + Common.numberFormat.get().format(ipk),
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		}

		return false;
	}

	/**
	 * Memeriksa apakah mahasiswa memenuhi syarat akademis (SKS dan IPK minimum) untuk
	 * mendaftar program PKL (Praktik Kerja Lapangan) tertentu.
	 *
	 * <p><b>Tujuan.</b> Identik dengan {@link #checkSyaratKkn} tetapi untuk PKL. Memvalidasi
	 * kelayakan sebelum pendaftaran dan menampilkan pesan error bila tidak memenuhi syarat.</p>
	 *
	 * <p><b>Cara kerja (inline — pola identik dengan checkSyaratKkn).</b>
	 * <ol>
	 *   <li>Cek pengecualian ({@code PengecualianPklMahasiswa}); bila ada, kembalikan {@code true}.</li>
	 *   <li>Validasi kesesuaian jurusan dan fakultas PKL vs mahasiswa.</li>
	 *   <li>Hitung semester dan sinkronisasi KRS mahasiswa.</li>
	 *   <li>Bandingkan SKS kumulatif dan IPK dengan syarat PKL ({@code getMinimalSksBolehIkutPkl},
	 *       {@code getMinimalIpkBolehIkutPkl}).</li>
	 *   <li>Bila tidak terpenuhi, cek syarat alternatif ({@code aktifkanSyaratLain}).</li>
	 *   <li>Tampilkan pesan error bila gagal semua syarat.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan.</b> Copy semantis dari {@code checkSyaratKkn}. Perubahan logika
	 * sinkronisasi KRS harus diterapkan di kedua metode ini.</p>
	 *
	 * @param mahasiswa mahasiswa yang akan mendaftar PKL; tidak boleh null
	 * @param pkl       program PKL yang ingin diikuti; tidak boleh null
	 * @return {@code true} bila memenuhi syarat atau ada pengecualian, {@code false} sebaliknya
	 * @throws Exception bila terjadi kesalahan sesi atau kalkulasi KRS
	 */
	public static Boolean checkSyaratPkl(Mahasiswa mahasiswa, Pkl pkl) throws Exception {

		Session session = HibernateUtil.currentSession();
		int kecuali = ((Number) session.createCriteria(PengecualianPklMahasiswa.class).add(Restrictions.eq("pkl", pkl))
				.add(Restrictions.eq("mahasiswa", mahasiswa)).setProjection(Projections.rowCount()).uniqueResult())
				.intValue();
		if (kecuali > 0) {
			return true;
		}

		if (pkl != null && pkl.getJurusan() != null && mahasiswa != null && mahasiswa.getJurusan() != null
				&& !mahasiswa.getJurusan().getId().equals(pkl.getJurusan().getId())) {
			MyMessageboxConfig.show(
					"Mahasiswa dengan NIM " + mahasiswa.getNim() + " dengan " + Common.getBahasaConfig("Jurusan") + " "
							+ mahasiswa.getJurusan().getNama() + " tidak bisa mendaftar di KKN di "
							+ Common.getBahasaConfig("Jurusan") + " " + pkl.getJurusan().getNama(),
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (pkl != null && pkl.getFakultas() != null && mahasiswa != null && mahasiswa.getJurusan() != null
				&& mahasiswa.getJurusan().getFakultas() != null
				&& !mahasiswa.getJurusan().getFakultas().getId().equals(pkl.getFakultas().getId())) {
			MyMessageboxConfig.show(
					"Mahasiswa dengan NIM " + mahasiswa.getNim() + " dengan " + Common.getBahasaConfig("Fakultas") + " "
							+ mahasiswa.getJurusan().getFakultas().getNama() + " tidak bisa mendaftar di KKN di "
							+ Common.getBahasaConfig("Fakultas") + " " + pkl.getFakultas().getNama(),
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		String ta = pkl.getTahunAkademik();
		Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
		Integer currentSemester = Common.getSemester(mahasiswa.getTahunangkatan(), pkl.getSemester(),
				mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, currentSemester, null, null);
		int sks = krsMahasiswa.getSksk();
		double ipk = krsMahasiswa.getIpk();
		if (sks >= pkl.getMinimalSksBolehIkutPkl() && ipk >= pkl.getMinimalIpkBolehIkutPkl()) {
			return true;
		} else {

			if (pkl.getAktifkanSyaratLain()) {
				if (sks >= pkl.getMinimalSksBolehIkutPkl2() && ipk >= pkl.getMinimalIpkBolehIkutPkl2()) {
					return true;
				}
			}

			MyMessageboxConfig.show(
					"Jumlah SKS atau IPK mahasiswa dengan NIM " + mahasiswa.getNim() + " dan nama "
							+ mahasiswa.getNama() + " belum memenuhi syarat.\n\nSKS Mahasiswa : " + sks
							+ "\nIPK Mahasiswa " + Common.numberFormat.get().format(ipk),
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		}

		return false;
	}

	/**
	 * Mengompres seluruh isi direktori {@code dir} ke dalam file ZIP {@code zipFileName}.
	 *
	 * <p><b>Tujuan.</b> Menyediakan utilitas kompresi direktori untuk keperluan ekspor
	 * data, backup, atau paket unduhan (misalnya: paket soal ujian, berkas laporan, dsb.).</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonFileMediaHelper.zipDir}.
	 * Implementasi di helper menggunakan {@link #addDir} untuk rekursi direktori.</p>
	 *
	 * @param zipFileName path file ZIP tujuan (akan dibuat bila belum ada)
	 * @param dir         path direktori yang akan dikompres
	 * @throws Exception bila gagal membuat/menulis file ZIP
	 */
	public static void zipDir(String zipFileName, String dir) throws Exception {
		CommonFileMediaHelper.zipDir(zipFileName, dir);
	}

	/**
	 * Menambahkan isi direktori {@code dirObj} ke dalam stream ZIP yang aktif secara rekursif.
	 *
	 * <p><b>Tujuan.</b> Helper rekursif yang dipakai oleh {@link #zipDir} untuk menambahkan
	 * semua file dan subdirektori ke dalam {@code ZipOutputStream}.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonFileMediaHelper.addDir}.</p>
	 *
	 * @param dirObj direktori yang isinya akan ditambahkan; tidak boleh null
	 * @param out    stream ZIP tujuan yang sedang terbuka; tidak boleh null
	 * @throws IOException bila terjadi kegagalan baca direktori atau tulis ke stream
	 */
	public static void addDir(File dirObj, ZipOutputStream out) throws IOException {
		CommonFileMediaHelper.addDir(dirObj, out);
	}

	/**
	 * Mengirimkan HTTP GET request ke URL yang diberikan dan mengembalikan respons sebagai String.
	 *
	 * <p><b>Tujuan.</b> Memungkinkan sistem memanggil API eksternal atau URL internal via HTTP GET
	 * — misalnya mengambil data dari layanan pihak ketiga, webhook, atau endpoint lokal.</p>
	 *
	 * <p><b>Cara kerja (inline).</b>
	 * <ol>
	 *   <li>Buka koneksi {@code HttpURLConnection} ke URL.</li>
	 *   <li>Set request method ke "GET" dan header {@code User-Agent} ke nilai browser standar.</li>
	 *   <li>Baca respons via {@code BufferedReader} baris per baris.</li>
	 *   <li>Kembalikan seluruh respons sebagai String.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error.</b> Exception dilempar ke pemanggil (throws Exception). Tidak ada
	 * timeout — koneksi yang lambat akan memblok thread tanpa batas.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Tidak ada pengelolaan SSL/TLS, autentikasi, atau redirect. Untuk
	 * kebutuhan lanjutan (HTTPS self-signed, Basic Auth, dsb.) gunakan implementasi yang lebih
	 * lengkap. Lihat {@link #excutePost} untuk versi POST.</p>
	 *
	 * @param url URL tujuan GET request (harus valid dan dapat dijangkau)
	 * @return respons HTTP sebagai String (semua baris digabung tanpa newline)
	 * @throws Exception bila URL tidak valid, koneksi gagal, atau terjadi IOException
	 */
	// HTTP GET request
	public static String sendGet(String url) throws Exception {

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		// add request header
		con.setRequestProperty("User-Agent", "Mozilla/5.0");

		// int responseCode = con.getResponseCode();
		// // System.out.println("\nSending 'GET' request to URL : " + url);
		// // System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		String result = response.toString();
		// print result
		// // System.out.println("url=>" + url + ", result=>" + result);
		return result;
	}

	/**
	 * Mengirimkan HTTP POST request ke URL dengan parameter yang dikodekan sebagai
	 * {@code application/x-www-form-urlencoded} dan mengembalikan respons sebagai String.
	 *
	 * <p><b>Tujuan.</b> Memungkinkan sistem mengirimkan data ke API eksternal atau endpoint
	 * internal via HTTP POST — misalnya notifikasi pembayaran, sinkronisasi data, atau
	 * callback ke layanan pihak ketiga.</p>
	 *
	 * <p><b>Cara kerja (inline).</b>
	 * <ol>
	 *   <li>Buka {@code HttpURLConnection} ke {@code targetURL}.</li>
	 *   <li>Set request method POST, Content-Type {@code application/x-www-form-urlencoded},
	 *       Content-Length, Content-Language "en-US", caches off, doOutput true.</li>
	 *   <li>Tulis {@code urlParameters} ke output stream ({@code DataOutputStream}).</li>
	 *   <li>Baca respons via {@code BufferedReader}; tiap baris diakhiri '\r'.</li>
	 *   <li>Kembalikan respons atau {@code null} bila terjadi exception.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error.</b> Exception ditangkap di blok catch, error ditampilkan
	 * via {@code tampilErrorJikaAdmin}, dan {@code null} dikembalikan. Blok finally
	 * memastikan koneksi selalu di-disconnect.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Parameter harus sudah dikodekan URL ({@code URLEncoder.encode}).
	 * Nama metode sengaja ejaan "excute" (bukan "execute") — jangan ganti agar tidak merusak
	 * referensi yang sudah ada. Lihat {@link #sendGet} untuk versi GET.</p>
	 *
	 * @param targetURL     URL endpoint POST tujuan
	 * @param urlParameters string parameter yang sudah dikodekan (misal "nama=Budi&nilai=90")
	 * @return respons server sebagai String, atau {@code null} bila terjadi kesalahan
	 */
	public static String excutePost(String targetURL, String urlParameters) {
		HttpURLConnection connection = null;
		try {
			// Create connection
			URL url = new URL(targetURL);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
			connection.setRequestProperty("Content-Language", "en-US");

			connection.setUseCaches(false);
			connection.setDoOutput(true);

			// Send request
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.close();

			// Get Response
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			StringBuilder response = new StringBuilder(); // or StringBuffer if
															// not Java 5+
			String line;
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			return response.toString();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	/**
	 * Menampilkan popup angket penilaian dosen (oleh dosen itu sendiri) bila status
	 * checklist penilaian untuk semester/tahun ajaran tertentu belum selesai.
	 *
	 * <p><b>Tujuan.</b> Memaksa dosen mengisi angket penilaian diri atau angket sejawat
	 * sebelum bisa melanjutkan akses sistem akademik untuk semester berjalan.</p>
	 *
	 * <p><b>Cara kerja (inline).</b>
	 * <ol>
	 *   <li>Bila {@code dosen} null atau belum punya ID, kembalikan {@code true} (tidak perlu
	 *       mengisi — mungkin bukan role dosen).</li>
	 *   <li>Panggil {@code ChecklistPenilaianHelper.checkStatusChecklist(dosen, ganjilgenap,
	 *       tahunAjaran)}; bila belum selesai ({@code true}):
	 *     <ul>
	 *       <li>Jadwalkan timer ZK untuk membuka popup modal berisi include ZUL
	 *           {@code checklist_penilaian_dosen_oleh_dosen.zul}.</li>
	 *       <li>Setelah popup dibuka, jadwalkan timer kedua untuk menampilkan pesan peringatan
	 *           agar dosen segera mengisi angket.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Kembalikan {@code false} bila belum selesai (akses dibatasi), {@code true} bila sudah.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Threading.</b> Menggunakan {@code createDefaultTimer} (ZK timer) untuk menunda
	 * pembukaan popup agar DOM siap. Aman dalam konteks ZK event thread.</p>
	 *
	 * @param dosen       dosen yang statusnya diperiksa; bila null langsung return {@code true}
	 * @param ganjilgenap semester (misal "Ganjil" atau "Genap")
	 * @param tahunAjaran tahun akademik (misal "2024/2025")
	 * @return {@code true} bila angket sudah selesai atau dosen null; {@code false} bila belum
	 * @throws Exception bila terjadi kesalahan inisialisasi popup ZK
	 */
	public static boolean displayPenilaianAngket(Dosen dosen, final String ganjilgenap, final String tahunAjaran)
			throws Exception {
		if (dosen == null || dosen.getId() == null) {
			return true;
		}

		if (ChecklistPenilaianHelper.checkStatusChecklist(dosen, ganjilgenap, tahunAjaran)) {

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					MyWindow window = new MyWindow("", "none", false);
					window.setHeight("97%");
					window.setWidth("97%");
					window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					MyInclude iframe = new MyInclude("/common/checklist_penilaian_dosen_oleh_dosen.zul");
					iframe.setParent(window);

					window.onModal();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							MyMessageboxConfig.show("Penilaian angket dosen untuk semester " + (ganjilgenap)
									+ ", tahun akademik " + tahunAjaran
									+ " sebagian atau semuanya belum Anda lakukan. Sebelum Anda bisa melanjutkan akses aplikasi akademik ini, mohon isilah terlebih dulu Angket Dosen untuk semester "
									+ ganjilgenap, "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						}
					});
				}
			});

			return false;
		} else {
			return true;
		}
	}

	/**
	 * Menampilkan popup angket penilaian guru (oleh siswa) bila status checklist penilaian
	 * untuk semester/tahun ajaran tertentu belum selesai.
	 *
	 * <p><b>Tujuan.</b> Memaksa siswa mengisi angket penilaian guru sebelum dapat melanjutkan
	 * akses sistem akademik untuk semester berjalan — memastikan respon penilaian terkumpul.</p>
	 *
	 * <p><b>Cara kerja (inline).</b>
	 * <ol>
	 *   <li>Bila {@code siswa} null atau belum punya ID, kembalikan {@code true}.</li>
	 *   <li>Panggil {@code ChecklistPenilaianGuruHelper.checkStatusChecklistGuru}; bila belum
	 *       selesai:
	 *     <ul>
	 *       <li>Ambil daftar ID jadwal pelajaran siswa via {@code getJadwalPelajaranSiswa}.</li>
	 *       <li>Jadwalkan timer untuk membuka {@code AngketGuruWindow} modal (window 97% × 97%,
	 *           dengan border-radius dan overflow:hidden).</li>
	 *       <li>Setelah popup, jadwalkan timer kedua untuk pesan peringatan pengisian angket.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Kembalikan {@code false} bila belum selesai, {@code true} bila sudah.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Perbedaan dari {@link #displayPenilaianAngket}.</b> Versi guru menggunakan
	 * {@code AngketGuruWindow} (komponen Java, bukan ZUL include) dan memerlukan daftar
	 * jadwal pelajaran secara eksplisit.</p>
	 *
	 * @param siswa       siswa yang statusnya diperiksa; bila null langsung return {@code true}
	 * @param ganjilgenap semester (misal "Ganjil" atau "Genap")
	 * @param tahunAjaran tahun akademik (misal "2024/2025")
	 * @return {@code true} bila angket sudah selesai atau siswa null; {@code false} bila belum
	 * @throws Exception bila terjadi kesalahan inisialisasi popup ZK
	 */
	public static boolean displayPenilaianAngketGuru(final ais.database.model.sekolah.Siswa siswa,
			final String ganjilgenap, final String tahunAjaran) throws Exception {
		if (siswa == null || siswa.getId() == null) {
			return true;
		}

		if (ChecklistPenilaianGuruHelper.checkStatusChecklistGuru(siswa, ganjilgenap, tahunAjaran)) {
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					List<Long> jadwalPelajaranIds = ChecklistPenilaianGuruHelper.getJadwalPelajaranSiswa(siswa,
							tahunAjaran, ganjilgenap);
					MyWindow window = new MyWindow("Angket Penilaian Guru", "none", false);
					window.setHeight("97%");
					window.setWidth("97%");
					window.setStyle("border-radius:10px; overflow:hidden;");
					window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					AngketGuruWindow angketGuruWindow = new AngketGuruWindow(tahunAjaran, ganjilgenap,
							jadwalPelajaranIds, siswa, window, true);
					angketGuruWindow.setParent(Common.tampilanScroll(window));
					window.onModal();

					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							MyMessageboxConfig.show("Penilaian angket guru untuk semester " + ganjilgenap
									+ ", tahun akademik " + tahunAjaran
									+ " sebagian atau semuanya belum Anda lakukan. Sebelum Anda bisa melanjutkan akses aplikasi ini, mohon isilah terlebih dulu Angket Guru.",
									"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						}
					});
				}
			});
			return false;
		}
		return true;
	}

	/**
	 * Memeriksa apakah berkas {@code file} adalah berkas gambar berdasarkan MIME type atau
	 * header byte-nya.
	 *
	 * <p><b>Tujuan.</b> Memvalidasi berkas yang diunggah pengguna apakah merupakan gambar
	 * (JPG, PNG, GIF, BMP, dsb.) sebelum diproses atau disimpan.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonFileMediaHelper.isImage}.</p>
	 *
	 * @param file berkas yang akan diperiksa; tidak boleh null
	 * @return {@code true} bila berkas adalah gambar; {@code false} sebaliknya
	 * @throws Exception bila terjadi kegagalan membaca header berkas
	 */
	public static boolean isImage(File file) throws Exception {
		return CommonFileMediaHelper.isImage(file);
	}

	/**
	 * Gerbang aman sebelum path berkas gambar dikirim sebagai parameter
	 * {@code <image>} ke JasperReports (ttd pejabat, kop/stempel/logo sekolah,
	 * yayasan, fakultas, jurusan, perguruan tinggi, dst). Berkas yang ADA tapi
	 * isinya bukan gambar valid (kosong/rusak/upload terpotong) membuat iText
	 * {@code Image.getInstance} melempar "byte array is not a recognized
	 * imageformat" saat export PDF dan membatalkan SELURUH laporan. Dengan
	 * gerbang ini berkas tak valid diperlakukan sama seperti "tidak ada"
	 * (parameter tak diisi) sehingga elemen gambar di jrxml kosong/di-skip,
	 * bukan meledakkan seluruh export.
	 *
	 * <p>Tidak pernah melempar; termasuk {@code catch(Throwable)} agar
	 * gambar bom (dimensi raksasa) yang bisa memicu {@code OutOfMemoryError}
	 * saat dekode header tetap aman ditangkap.</p>
	 */
	public static boolean isGambarLaporanValid(File file) {
		if (file == null || !file.exists() || file.length() == 0) {
			return false;
		}
		try {
			return isImage(file);
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * Memuat daftar {@code Komentar} (catatan dosen PA atau supervisor) milik seorang mahasiswa
	 * berdasarkan semester, tahapan, dan tahun akademik.
	 *
	 * <p><b>Tujuan.</b> Mengambil komentar/catatan bimbingan yang ditulis oleh dosen pembimbing
	 * akademik (PA) untuk seorang mahasiswa pada semester tertentu, dipakai di halaman
	 * konseling akademik atau laporan bimbingan.</p>
	 *
	 * <p><b>Cara kerja (inline).</b>
	 * <ol>
	 *   <li>Buka {@code currentSession()} (tidak membuka sesi baru).</li>
	 *   <li>Buat kriteria semester: bila {@code tahapan} null atau 0, filter by semester;
	 *       bila ada tahapan, gunakan {@code sqlRestriction("true")} (semua semester).</li>
	 *   <li>Buat kriteria tahapan: bila {@code tahapan} null atau 0, izinkan semua;
	 *       bila ada, cari yang tahapan cocok ATAU tahapan null.</li>
	 *   <li>Query ke {@code Komentar} dengan filter mahasiswa, semester, tahapan, tahun
	 *       akademik, dan semesterPendek (null-check).</li>
	 *   <li>Kembalikan hasilnya, atau list kosong bila exception.</li>
	 * </ol>
	 * </p>
	 *
	 * @param mahasiswa     mahasiswa pemilik komentar; tidak boleh null
	 * @param semester      nomor semester (1, 2, 3, ...) yang dicari
	 * @param tahapan       tahapan bimbingan (0 atau null = semua tahapan)
	 * @param tahunAjaran   tahun akademik (misal "2024/2025")
	 * @param semesterPendek nomor semester pendek; null bila bukan semester pendek
	 * @return daftar komentar diurutkan berdasarkan tanggal ascending; list kosong bila error
	 */
	@SuppressWarnings("unchecked")
	public static List<Komentar> loadKomentarData(Mahasiswa mahasiswa, Integer semester, final Integer tahapan,
			String tahunAjaran, Integer semesterPendek) {
		try {
			Session session = HibernateUtil.currentSession();
			Criterion criterionSemester = tahapan == null || tahapan.equals(0) ? Restrictions.eq("semester", semester)
					: Restrictions.sqlRestriction("true");

			Criterion criterionTahapan = tahapan == null || tahapan.equals(0) ? Restrictions.sqlRestriction("true")
					: Restrictions.or(Restrictions.eq("tahapan", tahapan), Restrictions.isNull("tahapan"));

			List<Komentar> komentars = session.createCriteria(Komentar.class).addOrder(Order.asc("tanggal"))
					.add(semesterPendek == null ? Restrictions.isNull("semesterPendek")
							: Restrictions.eq("semesterPendek", semesterPendek))
					.add(Restrictions.eq("mahasiswa", mahasiswa)).add(criterionSemester).add(criterionTahapan)
					.add(Restrictions.eq("tahunAkademik", tahunAjaran)).list();
			// myKomentars.addAll(komentars);
			return komentars;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return new ArrayList<Komentar>();
		}
	}

	/**
	 * Menghitung jumlah {@code Komentar} (catatan bimbingan PA) milik seorang mahasiswa
	 * untuk semester dan tahapan tertentu, tanpa memuat seluruh data.
	 *
	 * <p><b>Tujuan.</b> Memberikan jumlah komentar untuk keperluan badge, indikator, atau
	 * validasi kehadiran komentar tanpa overhead memuat seluruh objek.</p>
	 *
	 * <p><b>Cara kerja (inline).</b> Sama dengan {@link #loadKomentarData} tetapi menggunakan
	 * {@code Projections.rowCount()} di query, sehingga hanya mengambil satu angka dari
	 * database. Mengembalikan 0 bila exception.</p>
	 *
	 * @param mahasiswa      mahasiswa yang komentar-nya dihitung; tidak boleh null
	 * @param semester       nomor semester
	 * @param tahapan        tahapan bimbingan (0 atau null = semua)
	 * @param semesterPendek nomor semester pendek; null bila bukan semester pendek
	 * @return jumlah komentar yang sesuai filter; 0 bila tidak ada atau terjadi error
	 */
	public static int loadKomentarUkuran(Mahasiswa mahasiswa, Integer semester, final Integer tahapan,
			Integer semesterPendek) {
		Session session = null;
		try {
			// Query ini juga dipanggil dari thread latar. Gunakan session lokal agar tidak
			// meminjam currentNativeSession thread-local yang mungkin sudah terputus.
			session = HibernateUtil.openSession();
			Criterion criterionSemester = tahapan == null || tahapan.equals(0) ? Restrictions.eq("semester", semester)
					: Restrictions.sqlRestriction("true");

			Criterion criterionTahapan = tahapan == null || tahapan.equals(0) ? Restrictions.sqlRestriction("true")
					: Restrictions.or(Restrictions.eq("tahapan", tahapan), Restrictions.isNull("tahapan"));

			int size = ((Number) session.createCriteria(Komentar.class).setProjection(Projections.rowCount())
					.add(semesterPendek == null ? Restrictions.isNull("semesterPendek")
							: Restrictions.eq("semesterPendek", semesterPendek))
					.add(Restrictions.eq("mahasiswa", mahasiswa)).add(criterionSemester).add(criterionTahapan)
					.uniqueResult()).intValue();

			return size;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return 0;
		} finally {
			if (session != null && session.isOpen()) {
				try { session.clear(); } catch (Exception e) { }
				try { session.disconnect(); } catch (Exception e) { }
				try { session.close(); } catch (Exception e) { }
			}
		}
	}

	/**
	 * Renderer baris grid untuk menampilkan daftar {@code Komentar} (catatan bimbingan PA)
	 * dengan tombol hapus inline.
	 *
	 * <p><b>Tujuan.</b> Menyediakan tampilan baris komentar pada grid bimbingan PA, lengkap
	 * dengan isi komentar (HTML), nama pemberi komentar, tanggal, dan tombol hapus yang
	 * dikonfirmasi via messagebox.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Set {@code valign="top"} pada baris agar konten multi-baris rapi.</li>
	 *   <li>Tambahkan {@code MyHtml} untuk isi komentar (mendukung markup).</li>
	 *   <li>Tambahkan {@code Label} untuk nama pembuat ({@code getOleh()}) dan tanggal.</li>
	 *   <li>Tambahkan toolbar berisi tombol hapus (ikon trash SVG); onClick meminta
	 *       konfirmasi, bila OK panggil {@code Common.refreshDelete} lalu panggil
	 *       {@code eventListener.onEvent} untuk memperbarui UI pemanggil.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Threading.</b> Berjalan di ZK event thread. Event listener yang disuntikkan
	 * ({@code eventListener}) dipanggil setelah hapus berhasil.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Kelas ini adalah inner public static class di {@code Common}.
	 * Perubahan kolom (misal tambah kolom kategori) harus sejalan dengan definisi
	 * {@code Columns} pada ZUL yang menggunakan renderer ini.</p>
	 */
	public static class KomentarRenderer extends ais.ui.util.MyRowRenderer {

		private EventListener eventListener;

		/**
		 * Membuat renderer dengan event listener yang akan dipanggil setelah komentar dihapus.
		 *
		 * @param eventListener listener yang dipanggil untuk memperbarui UI setelah hapus;
		 *                      tidak boleh null
		 */
		public KomentarRenderer(EventListener eventListener) {
			this.eventListener = eventListener;
		}

		/**
		 * Merender satu baris {@code Komentar} ke dalam grid ZK.
		 *
		 * @param row  baris grid ZK yang akan diisi; tidak boleh null
		 * @param data objek {@code Komentar} yang dirender; harus instance Komentar
		 * @throws Exception bila terjadi kesalahan UI ZK
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final Komentar komentarBeans = (Komentar) data;

			new ais.ui.util.MyHtml(komentarBeans.getKomentar()).setParent(row);
			new Label(komentarBeans.getOleh()).setParent(row);
			new Label(Common.dateFormat.get().format(komentarBeans.getTanggal())).setParent(row);

			Hbox toolbar = new Hbox();
			toolbar.setParent(row);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setParent(toolbar);
			button.setTooltiptext("Hapus Data");
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
										Common.refreshDelete(komentarBeans);
										eventListener.onEvent(event);
									}

								}
							});
				}
			});

		}

	}

	private static Map<Integer, Integer> mappingTigaTahapanDenganSemesterGenap = new HashMap<Integer, Integer>();

	private static Map<Integer, Integer> mappingTigaTahapanDenganSemester = new HashMap<Integer, Integer>();
	private static Map<Integer, Integer> mappingEmpatTahapanDenganSemester = new HashMap<Integer, Integer>();

	static {
		mappingTigaTahapanDenganSemester.put(0, 0);
		mappingTigaTahapanDenganSemester.put(1, 0);
		mappingTigaTahapanDenganSemester.put(2, 0);
		mappingTigaTahapanDenganSemester.put(3, 3);
		mappingTigaTahapanDenganSemester.put(4, 3);
		mappingTigaTahapanDenganSemester.put(5, 6);
		mappingTigaTahapanDenganSemester.put(6, 6);
		mappingTigaTahapanDenganSemester.put(7, 9);
		mappingTigaTahapanDenganSemester.put(8, 9);
		mappingTigaTahapanDenganSemester.put(9, 12);
		mappingTigaTahapanDenganSemester.put(10, 12);
		mappingTigaTahapanDenganSemester.put(11, 15);
		mappingTigaTahapanDenganSemester.put(12, 15);
		mappingTigaTahapanDenganSemester.put(13, 18);
		mappingTigaTahapanDenganSemester.put(14, 18);
		mappingTigaTahapanDenganSemester.put(15, 21);
		mappingTigaTahapanDenganSemester.put(16, 21);
		mappingTigaTahapanDenganSemester.put(17, 24);
		mappingTigaTahapanDenganSemester.put(18, 24);

		mappingTigaTahapanDenganSemesterGenap.put(0, 0);
		mappingTigaTahapanDenganSemesterGenap.put(1, 0);
		mappingTigaTahapanDenganSemesterGenap.put(2, 3);
		mappingTigaTahapanDenganSemesterGenap.put(3, 3);
		mappingTigaTahapanDenganSemesterGenap.put(4, 6);
		mappingTigaTahapanDenganSemesterGenap.put(5, 6);
		mappingTigaTahapanDenganSemesterGenap.put(6, 9);
		mappingTigaTahapanDenganSemesterGenap.put(7, 9);
		mappingTigaTahapanDenganSemesterGenap.put(8, 12);
		mappingTigaTahapanDenganSemesterGenap.put(9, 12);
		mappingTigaTahapanDenganSemesterGenap.put(10, 15);
		mappingTigaTahapanDenganSemesterGenap.put(11, 15);
		mappingTigaTahapanDenganSemesterGenap.put(12, 18);
		mappingTigaTahapanDenganSemesterGenap.put(13, 18);
		mappingTigaTahapanDenganSemesterGenap.put(14, 21);
		mappingTigaTahapanDenganSemesterGenap.put(15, 21);
		mappingTigaTahapanDenganSemesterGenap.put(16, 24);
		mappingTigaTahapanDenganSemesterGenap.put(17, 24);
		mappingTigaTahapanDenganSemesterGenap.put(18, 25);

		mappingEmpatTahapanDenganSemester.put(0, 0);
		mappingEmpatTahapanDenganSemester.put(1, 0);
		mappingEmpatTahapanDenganSemester.put(2, 0);
		mappingEmpatTahapanDenganSemester.put(3, 0);
		mappingEmpatTahapanDenganSemester.put(4, 0);
		mappingEmpatTahapanDenganSemester.put(5, 4);
		mappingEmpatTahapanDenganSemester.put(6, 4);
		mappingEmpatTahapanDenganSemester.put(7, 4);
		mappingEmpatTahapanDenganSemester.put(8, 4);
		mappingEmpatTahapanDenganSemester.put(9, 8);
		mappingEmpatTahapanDenganSemester.put(10, 8);
		mappingEmpatTahapanDenganSemester.put(11, 8);
		mappingEmpatTahapanDenganSemester.put(12, 8);
		mappingEmpatTahapanDenganSemester.put(13, 12);
		mappingEmpatTahapanDenganSemester.put(14, 12);
		mappingEmpatTahapanDenganSemester.put(15, 12);
		mappingEmpatTahapanDenganSemester.put(16, 12);
	}

	/**
	 * Menghasilkan peta ({@code Map&lt;namaBulan, nomorTahapan&gt;}) yang memetakan setiap bulan
	 * dalam setahun ke nomor tahapan pembayaran cicilan, berdasarkan program studi, jurusan,
	 * semester, dan jenis semester.
	 *
	 * <p><b>Tujuan.</b> Menentukan pada bulan mana setiap cicilan pembayaran mahasiswa jatuh tempo,
	 * dengan memperhitungkan jumlah tahapan pembayaran yang dikonfigurasi per program (2, 3, atau 4
	 * tahapan) dan offset bulan mulai semester ganjil dari {@code ConstantValues}.</p>
	 *
	 * <p><b>Cara kerja (inline — logika kompleks).</b>
	 * <ol>
	 *   <li>Inisialisasi {@code ConstantValues.jumlahTahapan} bila masih kosong.</li>
	 *   <li>Tentukan jumlah tahapan (2, 3, atau 4) dari konfigurasi program/jurusan.</li>
	 *   <li>Untuk setiap 12 bulan, hitung bulan riil dengan offset
	 *       {@code pembayaranSemesterGanjilMulaiDiBulan}.</li>
	 *   <li><b>Jika 2 tahapan:</b> semua bulan langsung dipetakan ke {@code semester}.</li>
	 *   <li><b>Jika 3 tahapan:</b> 12 bulan dibagi 3 kelompok (1-4, 5-8, 9-12), lalu
	 *       ditambah offset mapping semester dari {@code mappingTigaTahapanDenganSemester}
	 *       (ganjil) atau {@code mappingTigaTahapanDenganSemesterGenap} (genap), dengan
	 *       koreksi khusus untuk semester genap awal (2, 4, 6, dsb.) agar melanjutkan
	 *       penomoran tahapan dari semester sebelumnya.</li>
	 *   <li><b>Jika 4 tahapan:</b> 12 bulan dibagi 4 kelompok (1-3, 4-6, 7-9, 10-12),
	 *       ditambah offset dari {@code mappingEmpatTahapanDenganSemester}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Tabel mapping tahapan</b> (static field, diinisialisasi di blok static):
	 * <ul>
	 *   <li>{@code mappingTigaTahapanDenganSemester} — semester ganjil</li>
	 *   <li>{@code mappingTigaTahapanDenganSemesterGenap} — semester genap</li>
	 *   <li>{@code mappingEmpatTahapanDenganSemester} — untuk 4 tahapan</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Pemeliharaan.</b> Logika pemetaan sangat spesifik domain pembayaran SPP; perubahan
	 * kebijakan jumlah tahapan harus diperbarui di tabel mapping. Nama metode mengandung typo
	 * "poulat" (seharusnya "populate") — jangan ganti agar tidak merusak referensi yang sudah ada.</p>
	 *
	 * @param program      nama program studi (S1, D3, dsb.)
	 * @param jurusan      jurusan mahasiswa; digunakan untuk menentukan jumlah tahapan
	 * @param semester     nomor semester mahasiswa saat ini
	 * @param jenisSemester "Ganjil" atau "Genap"
	 * @return peta nama bulan → nomor tahapan (12 entri, satu per bulan kalender)
	 */
	public static Map<String, Integer> poulateTahapan(String program, Jurusan jurusan, Integer semester,
			String jenisSemester) {
		if (ConstantValues.jumlahTahapan.isEmpty()) {
			ConstantValues.initJumlahTahapan();
		}
		Map<String, Integer> tahapanBulans = new HashMap<String, Integer>();
		for (int bulan = 1; bulan <= 12; bulan++) {
			int realBulan = ((ConstantValues.pembayaranSemesterGanjilMulaiDiBulan - 1) + bulan);
			if (realBulan > 12) {
				realBulan = realBulan % 12;
			}
			if (ConstantValues.getJumlahTahapan(program, jurusan) == 2) {
				tahapanBulans.put(Common.BULAN[realBulan - 1], semester);
			} else if (ConstantValues.getJumlahTahapan(program, jurusan) == 3) {
				if (jenisSemester.equals(Perkuliahan.GANJIL)) {

					int thp = (bulan >= 1 && bulan <= 4) ? 1 : (bulan >= 5 && bulan <= 8) ? 2 : 3;
					Integer mapping = null;
					if (mappingTigaTahapanDenganSemester.keySet().contains(semester)) {
						mapping = mappingTigaTahapanDenganSemester.get(semester);
					}

					if (thp == 1) {
						if (semester.equals(2) && (mapping == null || mapping.equals(0))) {
							mapping = 3;
						} else if (semester.equals(4) && (mapping == null || mapping.equals(3))) {
							mapping = 6;
						} else if (semester.equals(6) && (mapping == null || mapping.equals(6))) {
							mapping = 9;
						} else if (semester.equals(8) && (mapping == null || mapping.equals(9))) {
							mapping = 12;
						} else if (semester.equals(10) && (mapping == null || mapping.equals(12))) {
							mapping = 15;
						} else if (semester.equals(12) && (mapping == null || mapping.equals(15))) {
							mapping = 18;
						} else if (semester.equals(14) && (mapping == null || mapping.equals(18))) {
							mapping = 21;
						}
					}

					int newTahap = thp;
					if (mapping != null) {
						newTahap = thp + mapping;
					}

					tahapanBulans.put(Common.BULAN[realBulan - 1], newTahap);
				} else {
					int thp = (bulan >= 1 && bulan <= 4) ? 1 : (bulan >= 5 && bulan <= 8) ? 2 : 3;
					Integer mapping = null;
					if (mappingTigaTahapanDenganSemesterGenap.keySet().contains(semester)) {
						mapping = mappingTigaTahapanDenganSemesterGenap.get(semester);
					}

					if (thp == 1) {
						if (semester.equals(1) && (mapping == null || mapping.equals(0))) {
							mapping = 3;
						} else if (semester.equals(3) && (mapping == null || mapping.equals(3))) {
							mapping = 6;
						} else if (semester.equals(5) && (mapping == null || mapping.equals(6))) {
							mapping = 9;
						} else if (semester.equals(7) && (mapping == null || mapping.equals(9))) {
							mapping = 12;
						} else if (semester.equals(9) && (mapping == null || mapping.equals(12))) {
							mapping = 15;
						} else if (semester.equals(11) && (mapping == null || mapping.equals(15))) {
							mapping = 18;
						} else if (semester.equals(13) && (mapping == null || mapping.equals(18))) {
							mapping = 21;
						}
					}

					int newTahap = thp;
					if (mapping != null) {
						newTahap = thp + mapping;
					}

					tahapanBulans.put(Common.BULAN[realBulan - 1], newTahap);
				}
			} else if (ConstantValues.getJumlahTahapan(program, jurusan) == 4) {
				int thp = (bulan >= 1 && bulan <= 3) ? 1
						: (bulan >= 4 && bulan <= 6) ? 2 : (bulan >= 7 && bulan <= 9) ? 3 : 4;
				if (mappingEmpatTahapanDenganSemester.keySet().contains(semester)) {
					thp = thp + mappingEmpatTahapanDenganSemester.get(semester);
				}
				tahapanBulans.put(Common.BULAN[realBulan - 1], thp);
			}
		}

		// // System.out.println("tahapanBulans => " + tahapanBulans);
		return tahapanBulans;
	}

	/**
	 * Menulis daftar string {@code records} ke {@code writer} dan memflush/menutup writer
	 * setelah semua data tertulis.
	 *
	 * <p><b>Tujuan.</b> Menyediakan utilitas penulisan batch ke file/stream teks — misalnya
	 * mengekspor data CSV, log, atau teks lainnya ke file sistem.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Iterasi {@code records}; tulis setiap string ke {@code writer} apa adanya
	 *       (tanpa pemisah baris tambahan — format ditentukan oleh isi records).</li>
	 *   <li>Panggil {@code writer.flush()} untuk memastikan semua data tertulis ke buffer
	 *       dasar.</li>
	 *   <li>Panggil {@code writer.close()} untuk menutup dan melepas resource.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error.</b> {@code IOException} dilempar ke pemanggil. Tidak ada
	 * try-finally — bila {@code write} gagal, {@code close()} tidak akan dipanggil dan
	 * resource mungkin bocor. Pertimbangkan try-finally saat memelihara.</p>
	 *
	 * @param records daftar string yang akan ditulis; tidak boleh null
	 * @param writer  writer tujuan yang sudah terbuka; tidak boleh null
	 * @throws IOException bila terjadi kegagalan penulisan, flush, atau close
	 */
	public static void write(List<String> records, Writer writer) throws IOException {

		for (String record : records) {
			writer.write(record);
		}
		writer.flush();
		writer.close();
	}

	/**
	 * Melakukan login pengguna staff/admin ({@code Tbmuser}) ke sistem dan mengarahkan ke
	 * halaman profil atau callback URL.
	 *
	 * <p><b>Tujuan.</b> Memproses autentikasi programatis untuk pengguna bertipe {@code Tbmuser}
	 * (admin, dosen, pegawai, operator) — biasanya dipanggil setelah verifikasi OAuth/social login
	 * atau setelah proses registrasi otomatis.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.doLogin(tbmuser, linkProfile, callback_url)}.</p>
	 *
	 * @param tbmuser      pengguna yang akan di-login; tidak boleh null
	 * @param linkProfile  URL profil media sosial (dari OAuth callback); boleh null
	 * @param callback_url URL tujuan redirect setelah login berhasil; boleh null
	 * @throws Exception bila gagal membuat sesi atau mengirimkan redirect
	 */
	public static void doLogin(Tbmuser tbmuser, String linkProfile, String callback_url) throws Exception {
		CommonSecurityLoginHelper.doLogin(tbmuser, linkProfile, callback_url);
	}

	/**
	 * Melakukan login mahasiswa ke sistem dan mengarahkan ke halaman profil atau callback URL.
	 *
	 * <p><b>Tujuan.</b> Memproses autentikasi programatis untuk mahasiswa — biasanya dipanggil
	 * setelah verifikasi OAuth/social login berhasil mencocokkan akun social media dengan
	 * data mahasiswa.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.doLogin(mahasiswa, linkProfile, callback_url)}.</p>
	 *
	 * @param mahasiswa    mahasiswa yang akan di-login; tidak boleh null
	 * @param linkProfile  URL profil media sosial; boleh null
	 * @param callback_url URL redirect setelah login; boleh null
	 * @throws Exception bila gagal membuat sesi atau mengirimkan redirect
	 */
	public static void doLogin(Mahasiswa mahasiswa, String linkProfile, String callback_url) throws Exception {
		CommonSecurityLoginHelper.doLogin(mahasiswa, linkProfile, callback_url);
	}

	/**
	 * Melakukan login siswa sekolah ke sistem dan mengarahkan ke halaman profil atau callback URL.
	 *
	 * <p><b>Tujuan.</b> Memproses autentikasi programatis untuk siswa sekolah — dipakai di
	 * alur social login untuk pengguna dengan peran siswa.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.doLogin(siswa, linkProfile, callback_url)}.</p>
	 *
	 * @param siswa        siswa yang akan di-login; tidak boleh null
	 * @param linkProfile  URL profil media sosial; boleh null
	 * @param callback_url URL redirect setelah login; boleh null
	 * @throws Exception bila gagal membuat sesi atau mengirimkan redirect
	 */
	public static void doLogin(Siswa siswa, String linkProfile, String callback_url) throws Exception {
		CommonSecurityLoginHelper.doLogin(siswa, linkProfile, callback_url);
	}

	/**
	 * Melakukan login pengguna staff ({@code Tbmuser}) dengan parameter URL tambahan
	 * yang disertakan ke URL tujuan.
	 *
	 * <p><b>Tujuan.</b> Varian {@link #doLogin(Tbmuser, String, String)} untuk menambahkan
	 * parameter query string ke URL redirect — misalnya {@code ?tambah_akun=true} untuk
	 * membuka tab media sosial setelah login.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.doLogin(tbmuser, linkProfile, parameter, callback_url)}.</p>
	 *
	 * @param tbmuser      pengguna yang di-login; tidak boleh null
	 * @param linkProfile  URL profil media sosial; boleh null
	 * @param parameter    parameter query string tambahan (misal "?tambah_akun=true")
	 * @param callback_url URL redirect setelah login; boleh null
	 * @throws Exception bila gagal membuat sesi
	 */
	public static void doLogin(Tbmuser tbmuser, String linkProfile, String parameter, String callback_url)
			throws Exception {
		CommonSecurityLoginHelper.doLogin(tbmuser, linkProfile, parameter, callback_url);
	}

	/**
	 * Melakukan login mahasiswa dengan parameter URL tambahan yang disertakan ke URL tujuan.
	 *
	 * <p><b>Tujuan.</b> Varian {@link #doLogin(Mahasiswa, String, String)} untuk menyertakan
	 * parameter query string ke URL redirect pasca-login, misal untuk membuka tab tertentu.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.doLogin(mahasiswa, linkProfile, parameter, callback_url)}.</p>
	 *
	 * @param mahasiswa    mahasiswa yang di-login; tidak boleh null
	 * @param linkProfile  URL profil media sosial; boleh null
	 * @param parameter    parameter query string tambahan
	 * @param callback_url URL redirect setelah login; boleh null
	 * @throws Exception bila gagal membuat sesi
	 */
	public static void doLogin(Mahasiswa mahasiswa, String linkProfile, String parameter, String callback_url)
			throws Exception {
		CommonSecurityLoginHelper.doLogin(mahasiswa, linkProfile, parameter, callback_url);
	}

	/**
	 * Melakukan login siswa sekolah dengan parameter URL tambahan yang disertakan ke URL tujuan.
	 *
	 * <p><b>Tujuan.</b> Varian {@link #doLogin(Siswa, String, String)} untuk menyertakan
	 * parameter query string ke URL redirect pasca-login.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.doLogin(siswa, linkProfile, parameter, callback_url)}.</p>
	 *
	 * @param siswa        siswa yang di-login; tidak boleh null
	 * @param linkProfile  URL profil media sosial; boleh null
	 * @param parameter    parameter query string tambahan
	 * @param callback_url URL redirect setelah login; boleh null
	 * @throws Exception bila gagal membuat sesi
	 */
	public static void doLogin(Siswa siswa, String linkProfile, String parameter, String callback_url)
			throws Exception {
		CommonSecurityLoginHelper.doLogin(siswa, linkProfile, parameter, callback_url);
	}

	/**
	 * Menambahkan widget tombol penghubung akun media sosial (Facebook, Google, Twitter,
	 * LinkedIn) ke dalam komponen parent yang diberikan.
	 *
	 * <p><b>Tujuan.</b> Menyediakan UI untuk menghubungkan akun sistem dengan akun media sosial
	 * pengguna, sehingga pengguna bisa login di masa mendatang menggunakan akun sosial tersebut.</p>
	 *
	 * <p><b>Cara kerja (inline).</b>
	 * <ol>
	 *   <li>Buat {@code Vbox} utama di dalam {@code parent} dengan label instruksi.</li>
	 *   <li>Buat {@code Hbox} untuk menampung tombol-tombol media sosial secara horizontal.</li>
	 *   <li>Untuk setiap platform yang diaktifkan ({@code aktifkanIntegrasiFacebook},
	 *       {@code aktifkanIntegrasiGoogle}, {@code aktifkanIntegrasiTwitter},
	 *       {@code aktifkanIntegrasiLinkedin}): buat anchor ({@code A}) yang menuju URL OAuth
	 *       platform tersebut dengan parameter {@code tambah_akun=true&id=URLEncoded(username)}.
	 *       Sertakan logo platform (32px) dan label teks.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Keamanan.</b> Username di-encode dengan {@code URLEncoder.encode(username, "UTF-8")}
	 * sebelum dimasukkan ke URL — mencegah URL injection melalui username yang mengandung
	 * karakter khusus.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Menambah platform baru memerlukan: konstanta konfigurasi baru di
	 * {@code ConstantValues}, URL OAuth baru, dan blok if baru di sini. Target {@code _parent}
	 * digunakan agar OAuth callback tidak terjebak di iframe bila ada.</p>
	 *
	 * @param parent   komponen ZK induk yang akan menjadi wadah widget; tidak boleh null
	 * @param username username pengguna yang akan dihubungkan; dikodekan URL sebelum dipakai
	 * @throws Exception bila terjadi kesalahan encode URL atau inisialisasi komponen ZK
	 */
	public static void tampilTambahMediaSosial(Component parent, String username) throws Exception {

		Vbox vboxUtama = new Vbox();
		vboxUtama.setParent(parent);
		vboxUtama.appendChild(new Label(
				ais.common.Common.getBahasaConfig("Untuk menghubungkan akun media sosial ke sistem akademik, klik salah satu pilihan berikut :")));

		Hbox hbox = new Hbox();
		hbox.setParent(vboxUtama);
		if (ConstantValues.aktifkanIntegrasiFacebook) {
			A tambah = new A();
			hbox.appendChild(tambah);
			tambah.setTarget("_parent");
			tambah.setHref(Common.getRequestHostWithProtocol() + "/facebook.zul?tambah_akun=true&id="
					+ URLEncoder.encode(username, "UTF-8"));
			Image imTambah;
			tambah.appendChild(imTambah = new Image("/img/facebook_logo.png"));
			tambah.appendChild(new MyLabelKecil("Tambah Akun Facebook"));
			imTambah.setHeight("32px");
		}

		if (ConstantValues.aktifkanIntegrasiGoogle) {
			A tambah = new A();
			hbox.appendChild(tambah);
			tambah.setTarget("_parent");
			tambah.setHref(Common.getRequestHostWithProtocol() + "/google.zul?tambah_akun=true&id="
					+ URLEncoder.encode(username, "UTF-8"));
			Image imTambah;
			tambah.appendChild(imTambah = new Image("/img/google_logo.png"));
			tambah.appendChild(new MyLabelKecil("Tambah Akun Google"));
			imTambah.setHeight("32px");
		}

		if (ConstantValues.aktifkanIntegrasiTwitter) {
			A tambah = new A();
			hbox.appendChild(tambah);
			tambah.setTarget("_parent");
			tambah.setHref(Common.getRequestHostWithProtocol() + "/twitter.zul?tambah_akun=true&id="
					+ URLEncoder.encode(username, "UTF-8"));
			Image imTambah;
			tambah.appendChild(imTambah = new Image("/img/twitter_logo.png"));
			tambah.appendChild(new MyLabelKecil("Tambah Akun Twitter"));
			imTambah.setHeight("32px");
		}

		if (ConstantValues.aktifkanIntegrasiLinkedin) {
			A tambah = new A();
			hbox.appendChild(tambah);
			tambah.setTarget("_parent");
			tambah.setHref(Common.getRequestHostWithProtocol() + "/linkedin.zul?tambah_akun=true&id="
					+ URLEncoder.encode(username, "UTF-8"));
			Image imTambah;
			tambah.appendChild(imTambah = new Image("/img/linkedin_logo.png"));
			tambah.appendChild(new MyLabelKecil("Tambah Akun Linkedin"));
			imTambah.setHeight("32px");
		}
	}

	/**
	 * Menginisialisasi tab media sosial untuk pengguna yang dapat mewakili dirinya sendiri
	 * (tanpa konteks dosen, guru, atau pegawai tambahan).
	 *
	 * <p><b>Tujuan.</b> Shortcut untuk {@link #displaySocialMedia(MyTabConfig, Tabpanel,
	 * SocialMediaCommonModel, Dosen, Guru, Pegawai)} ketika pengguna yang ditampilkan adalah
	 * pengguna saat ini (bukan dosen/guru/pegawai tertentu).</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke overload lengkap dengan dosen/guru/pegawai null.</p>
	 *
	 * @param tabData  tab ZK yang jika diklik akan memuat daftar akun sosial; tidak boleh null
	 * @param tabpanel panel tab yang menjadi wadah konten; tidak boleh null
	 * @param a        model entitas yang punya data social media; boleh null
	 * @throws Exception bila terjadi kesalahan inisialisasi komponen ZK
	 */
	public static void displaySocialMedia(final MyTabConfig tabData, final org.zkoss.zk.ui.Component tabpanel,
			final SocialMediaCommonModel a) throws Exception {
		displaySocialMedia(tabData, tabpanel, a, null, null, null);
	}

	/**
	 * Menginisialisasi tab media sosial yang menampilkan daftar akun media sosial terhubung
	 * (Facebook, Google, Twitter, LinkedIn) beserta tombol hapus per akun, dengan lazy loading
	 * saat tab diklik untuk pertama kali.
	 *
	 * <p><b>Tujuan.</b> Menyediakan UI manajemen akun media sosial terhubung dalam panel tab —
	 * pengguna bisa melihat semua akun sosial yang terhubung ke akun sistem mereka dan menghapus
	 * koneksi yang tidak diinginkan.</p>
	 *
	 * <p><b>Cara kerja (inline — sangat kompleks).</b>
	 * <ol>
	 *   <li>Daftarkan {@code EventListener} pada {@code tabData.onClick}: listener ini baru
	 *       dieksekusi saat tab diklik pertama kali (lazy: cek {@code tabpanel.getChildren().isEmpty()}).</li>
	 *   <li>Bila konten belum dimuat: buat {@code Borderlayout} → North (berisi widget
	 *       {@link #tampilTambahMediaSosial}) + Center (berisi grid 4 kolom: Media, Profile, Link, Hapus).</li>
	 *   <li>Tentukan {@code tbmuserTemp}: bila {@code a} null, cari {@code Tbmuser} aktif yang
	 *       memiliki relasi ke {@code dosen}, {@code guru}, atau {@code pegawai} dari cache
	 *       {@code ConstantValues.ambilBerdasarClass(Tbmuser.class)}.</li>
	 *   <li>Untuk setiap platform yang diaktifkan dan memiliki ID terhubung, parse data profil
	 *       dari {@code socialMediaProfile} (format: {@code platform:id||link||picture||email||nama})
	 *       dan render baris grid dengan foto profil, nama, link, serta tombol hapus.</li>
	 *   <li>Tombol hapus setiap akun: konfirmasi, lalu update field ID platform di entitas
	 *       (hapus ID yang bersangkutan dari string comma-separated), simpan via session.</li>
	 *   <li>Setelah daftarkan listener: bila ada atribut sesi {@code tambah_akun=true}, jadwalkan
	 *       timer untuk memilih tab ini secara otomatis (menampilkan konten tanpa klik manual).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan.</b> Format {@code socialMediaProfile}: field dipisahkan {@code "||"},
	 * entri per-akun dipisahkan {@code ";"}. Penyimpanan ID menggunakan comma-separated string.
	 * Bila format berubah, logika parse di sini harus diperbarui.</p>
	 *
	 * @param tabData  tab ZK yang jika diklik akan memuat daftar akun; tidak boleh null
	 * @param tabpanel panel tab wadah konten; tidak boleh null
	 * @param a        model entitas dengan data social media (bisa Tbmuser/Mahasiswa/Siswa);
	 *                 bila null, sistem akan mencari via relasi dosen/guru/pegawai
	 * @param dosen    dosen pemilik akun sosial; boleh null
	 * @param guru     guru pemilik akun sosial; boleh null
	 * @param pegawai  pegawai pemilik akun sosial; boleh null
	 * @throws Exception bila terjadi kesalahan inisialisasi ZK
	 */
	public static void displaySocialMedia(final MyTabConfig tabData, final org.zkoss.zk.ui.Component tabpanel,
			final SocialMediaCommonModel a, final Dosen dosen, final Guru guru, final Pegawai pegawai)
			throws Exception {

		final EventListener eventListener = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				boolean child = tabpanel.getChildren().isEmpty();
				// System.out.println("child ==> " + child);
				if (child) {

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					tabpanel.appendChild(borderlayout);

					North north = new North();
					north.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("360px");
		north.setAutoscroll(true);
					String username = Common.getCurrentUser().getUserId();
					Common.tampilTambahMediaSosial(north, username);

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

					MyColumnConfig column = new MyColumnConfig("Media");
					column.setParent(columns);
					column.setWidth("62px");

					column = new MyColumnConfig("Profile");
					column.setParent(columns);
					column.setWidth("30%");

					column = new MyColumnConfig("Link");
					column.setParent(columns);

					column = new MyColumnConfig("Hapus");
					column.setParent(columns);
					column.setWidth("10%");

					SocialMediaCommonModel tbmuserTemp = a;

					if (tbmuserTemp == null && dosen != null) {
						Map<Serializable, Tbmuser> map = ConstantValues.ambilBerdasarClass(Tbmuser.class);
						for (Tbmuser tbmuser : map.values()) {
							if (tbmuser != null && tbmuser.getAktif() && tbmuser.ambilDosen() != null
									&& tbmuser.getDosen().getId() != null
									&& tbmuser.getDosen().getId().equals(dosen.getId())) {
								tbmuserTemp = tbmuser;
							}
						}

						if (tbmuserTemp == null) {
							for (Tbmuser tbmuser : map.values()) {
								if (tbmuser != null && tbmuser.ambilDosen() != null
										&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
										&& tbmuser.getDosen().getId() != null
										&& tbmuser.getDosen().getId().equals(dosen.getId())) {
									tbmuserTemp = tbmuser;
								}
							}
						}
					} else if (tbmuserTemp == null && guru != null) {
						Map<Serializable, Tbmuser> map = ConstantValues.ambilBerdasarClass(Tbmuser.class);
						for (Tbmuser tbmuser : map.values()) {
							if (tbmuser != null && tbmuser.getAktif() && tbmuser.ambilGuru() != null
									&& tbmuser.getGuru().getId() != null
									&& tbmuser.getGuru().getId().equals(guru.getId())) {
								tbmuserTemp = tbmuser;
							}
						}

						if (tbmuserTemp == null) {
							for (Tbmuser tbmuser : map.values()) {
								if (tbmuser != null && tbmuser.ambilGuru() != null && tbmuser.getGuru().getId() != null
										&& tbmuser.getGuru().getId().equals(guru.getId())) {
									tbmuserTemp = tbmuser;
								}
							}
						}
					} else if (tbmuserTemp == null && pegawai != null) {
						Map<Serializable, Tbmuser> map = ConstantValues.ambilBerdasarClass(Tbmuser.class);
						for (Tbmuser tbmuser : map.values()) {
							if (tbmuser != null && tbmuser.getAktif() && tbmuser.ambilPegawai() != null
									&& tbmuser.getPegawai().getId() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								tbmuserTemp = tbmuser;
							}
						}

						if (tbmuserTemp == null) {
							for (Tbmuser tbmuser : map.values()) {
								if (tbmuser != null && tbmuser.ambilPegawai() != null
										&& tbmuser.getPegawai().getId() != null
										&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
									tbmuserTemp = tbmuser;
								}
							}
						}
					}

					final SocialMediaCommonModel tbmuser = tbmuserTemp;

					Rows rows = new Rows();
					rows.setParent(grid);
					if (ConstantValues.aktifkanIntegrasiFacebook && tbmuser != null) {
						if (tbmuser.getFacebookId() != null && !tbmuser.getFacebookId().trim().isEmpty()) {

							for (final String id : tbmuser.getFacebookId().trim().split(",")) {
								if (!id.trim().isEmpty()) {
									try {
										final MyFormRow row = new MyFormRow();
										row.setValign("top");

										row.setParent(rows);

										String profile = "";
										for (String s : StringUtils.split(tbmuser.getSocialMediaProfile(), ";")) {
											if (s.contains("facebookId:" + id)) {
												profile = s;
											}
										}

										String[] data = StringUtils.split(profile, "||");

										final String linkProfile = data[1];
										String pictureUrl = data[2];
										String emailAddress = data[3];
										String formattedName = data[4];

										Image im;
										row.appendChild(im = new Image("/img/facebook_logo.png"));
										im.setHeight("32px");

										MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("",
												"/img/svg/trash.svg");

										toolbarbutton.addEventListener("onClick", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
														"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
														MyMessageboxConfig.QUESTION, new EventListener() {

															@Override
															public void onEvent(Event event) throws Exception {
																int i = Integer.parseInt(event.getData().toString());
																if (i == MyMessageboxConfig.OK) {
																	Session session = HibernateUtil.currentSession();
																	session.refresh(tbmuser);
																	String fid = tbmuser.getFacebookId().trim();
																	fid = org.apache.commons.lang3.StringUtils
																			.replace(fid, id, "");
																	tbmuser.setFacebookId(fid);
																	session.update(tbmuser);

																	row.setVisible(false);
																	row.detach();
																}

															}
														});
											}
										});
										Vbox vbox = new Vbox();
										row.appendChild(vbox);
										vbox.setWidth("100%");
										vbox.setPack("center");
										vbox.setAlign("center");
										Image img;
										vbox.appendChild(img = new Image(pictureUrl));
										img.setHeight("72px");
										vbox.appendChild(new Label(formattedName));

										A a = new A(emailAddress);
										a.setHref("mailto:" + emailAddress);
										a.setTarget("_top");
										vbox.appendChild(a);

										a = new A(linkProfile);
										a.setHref(linkProfile);
										a.setTarget("_blank");
										row.appendChild(a);
										row.appendChild(toolbarbutton);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:13954");
										// Common.tampilErrorJikaAdmin(e);
									}
								}
							}

						}
					}

					if (ConstantValues.aktifkanIntegrasiGoogle && tbmuser != null) {
						if (tbmuser.getGoogleId() != null && !tbmuser.getGoogleId().trim().isEmpty()) {

							for (final String id : tbmuser.getGoogleId().trim().split(",")) {
								if (!id.trim().isEmpty()) {
									try {
										final MyFormRow row = new MyFormRow();
										row.setValign("top");

										row.setParent(rows);

										String profile = "";
										for (String s : StringUtils.split(tbmuser.getSocialMediaProfile(), ";")) {
											if (s.contains("googleId:" + id)) {
												profile = s;
											}
										}

										String[] data = StringUtils.split(profile, "||");
										String linkProfile = "";
										if (data != null && data.length > 1) {
											linkProfile = data[1];
										}

										String pictureUrl = "";
										if (data != null && data.length > 2) {
											pictureUrl = data[2];
										}
										String emailAddress = "";
										if (data != null && data.length > 3) {
											emailAddress = data[3];
										}

										String formattedName = "";
										if (data != null && data.length > 4) {
											formattedName = data[4];
										}

										Image im;
										row.appendChild(im = new Image("/img/google_logo.png"));
										im.setHeight("32px");

										MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("",
												"/img/svg/trash.svg");
										toolbarbutton.addEventListener("onClick", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
														"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
														MyMessageboxConfig.QUESTION, new EventListener() {

															@Override
															public void onEvent(Event event) throws Exception {
																int i = Integer.parseInt(event.getData().toString());
																if (i == MyMessageboxConfig.OK) {
																	Session session = HibernateUtil.currentSession();
																	session.refresh(tbmuser);
																	String fid = tbmuser.getGoogleId().trim();
																	fid = org.apache.commons.lang3.StringUtils
																			.replace(fid, id, "");
																	tbmuser.setGoogleId(fid);
																	session.update(tbmuser);

																	row.setVisible(false);
																	row.detach();
																}

															}
														});
											}
										});

										Vbox vbox = new Vbox();
										row.appendChild(vbox);
										vbox.setWidth("100%");
										vbox.setPack("center");
										vbox.setAlign("center");
										Image img;
										vbox.appendChild(img = new Image(pictureUrl));
										img.setHeight("72px");
										vbox.appendChild(new Label(formattedName));

										A a = new A(emailAddress);
										a.setHref("mailto:" + emailAddress);
										a.setTarget("_top");
										vbox.appendChild(a);

										a = new A(linkProfile);
										a.setHref(linkProfile);
										a.setTarget("_blank");
										row.appendChild(a);
										row.appendChild(toolbarbutton);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:14066");
										// Common.tampilErrorJikaAdmin(e);
									}
								}
							}
						}
					}

					if (ConstantValues.aktifkanIntegrasiTwitter && tbmuser != null) {
						if (tbmuser.getTwitterId() != null && !tbmuser.getTwitterId().trim().isEmpty()) {

							for (final String id : tbmuser.getTwitterId().trim().split(",")) {
								if (!id.trim().isEmpty()) {

									try {

										final MyFormRow row = new MyFormRow();
										row.setValign("top");

										row.setParent(rows);

										String profile = "";
										for (String s : StringUtils.split(tbmuser.getSocialMediaProfile(), ";")) {
											if (s.contains("twitterId:" + id)) {
												profile = s;
											}
										}

										String[] data = StringUtils.split(profile, "||");

										final String linkProfile = data[1];
										String pictureUrl = data[2];
										String emailAddress = data[3];
										String formattedName = data[4];

										Image im;
										row.appendChild(im = new Image("/img/twitter_logo.png"));
										im.setHeight("32px");

										MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("",
												"/img/svg/trash.svg");
										toolbarbutton.addEventListener("onClick", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
														"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
														MyMessageboxConfig.QUESTION, new EventListener() {

															@Override
															public void onEvent(Event event) throws Exception {
																int i = Integer.parseInt(event.getData().toString());
																if (i == MyMessageboxConfig.OK) {

																	Session session = HibernateUtil.currentSession();
																	session.refresh(tbmuser);
																	String fid = tbmuser.getTwitterId().trim();
																	fid = org.apache.commons.lang3.StringUtils
																			.replace(fid, id, "");
																	tbmuser.setTwitterId(fid);
																	session.update(tbmuser);

																	row.setVisible(false);
																	row.detach();
																}

															}
														});
											}
										});

										Vbox vbox = new Vbox();
										row.appendChild(vbox);
										vbox.setWidth("100%");
										vbox.setPack("center");
										vbox.setAlign("center");
										Image img;
										vbox.appendChild(img = new Image(pictureUrl));
										img.setHeight("72px");
										vbox.appendChild(new Label(formattedName));

										A a = new A(emailAddress);
										a.setHref(linkProfile);
										a.setTarget("_blank");
										vbox.appendChild(a);

										a = new A(linkProfile);
										a.setHref(linkProfile);
										a.setTarget("_blank");
										row.appendChild(a);
										row.appendChild(toolbarbutton);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:14158");
										// Common.tampilErrorJikaAdmin(e);
									}
								}
							}
						}
					}

					if (ConstantValues.aktifkanIntegrasiLinkedin && tbmuser != null) {
						if (tbmuser.getLinkedinId() != null && !tbmuser.getLinkedinId().trim().isEmpty()) {

							for (final String id : tbmuser.getLinkedinId().trim().split(",")) {
								if (!id.trim().isEmpty()) {
									try {

										String profile = "";
										for (String s : StringUtils.split(tbmuser.getSocialMediaProfile(), ";")) {
											if (s.contains("linkedinId:" + id)) {
												profile = s;
											}
										}

										String[] data = StringUtils.split(profile, "||");

										final String linkProfile = data[1];
										String pictureUrl = data[2];
										String emailAddress = data[3];
										String formattedName = data[4];

										final MyFormRow row = new MyFormRow();
										row.setValign("top");

										row.setParent(rows);

										Image im;
										row.appendChild(im = new Image("/img/linkedin_logo.png"));
										im.setHeight("32px");

										MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("",
												"/img/svg/trash.svg");
										toolbarbutton.addEventListener("onClick", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
														"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
														MyMessageboxConfig.QUESTION, new EventListener() {

															@Override
															public void onEvent(Event event) throws Exception {
																int i = Integer.parseInt(event.getData().toString());
																if (i == MyMessageboxConfig.OK) {
																	Session session = HibernateUtil.currentSession();
																	session.refresh(tbmuser);
																	String fid = tbmuser.getLinkedinId().trim();
																	fid = org.apache.commons.lang3.StringUtils
																			.replace(fid, id, "");
																	tbmuser.setLinkedinId(fid);
																	session.update(tbmuser);

																	row.setVisible(false);
																	row.detach();
																}

															}
														});
											}
										});

										Vbox vbox = new Vbox();
										row.appendChild(vbox);
										vbox.setWidth("100%");
										vbox.setPack("center");
										vbox.setAlign("center");
										Image img;
										vbox.appendChild(img = new Image(pictureUrl));
										img.setHeight("72px");
										vbox.appendChild(new Label(formattedName));

										A a = new A(emailAddress);
										a.setHref("mailto:" + emailAddress);
										a.setTarget("_top");
										vbox.appendChild(a);

										a = new A(linkProfile);
										a.setHref(linkProfile);
										a.setTarget("_blank");
										row.appendChild(a);
										row.appendChild(toolbarbutton);

									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:14249");
										// Common.tampilErrorJikaAdmin(e);
									}
								}
							}
						}
					}

				}

			}
		};

		tabData.addEventListener("onClick", eventListener);

		if (Sessions.getCurrent().getAttribute("tambah_akun") != null
				&& Sessions.getCurrent().getAttribute("tambah_akun").equals("true")) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					tabData.setSelected(true);
					Sessions.getCurrent().removeAttribute("tambah_akun");
					eventListener.onEvent(arg0);
				}

			});
		}

	}

	/**
	 * Menghasilkan HTML marquee (teks berjalan) yang menggabungkan semua entitas
	 * {@code TextBerjalan} aktif yang relevan untuk pengguna yang sedang login.
	 *
	 * <p><b>Tujuan.</b> Menampilkan pengumuman berjalan di halaman utama sistem yang
	 * dapat dikonfigurasi per-yayasan, sekolah, fakultas, jurusan, atau program studi
	 * agar konten pengumuman tepat sasaran.</p>
	 *
	 * <p><b>Cara kerja (inline).</b>
	 * <ol>
	 *   <li>Inisialisasi {@code ConstantValues} dan ambil semua {@code TextBerjalan} dari cache.</li>
	 *   <li>Ambil konteks pengguna: fakultas, jurusan, sekolah, yayasan, program dari
	 *       {@code getCurrentUser()}.
	 *   <li>Bila konteks adalah sekolah (bukan PT): reset fakultas/jurusan/program, gunakan
	 *       konteks sekolah dan yayasan aktif dari {@code SekolahUtil}.</li>
	 *   <li>Untuk setiap {@code TextBerjalan} aktif: cek apakah cocok dengan semua filter
	 *       konteks (null = semua, tidak null = harus sama ID). Filter bersifat AND bertingkat.</li>
	 *   <li>Gabungkan teks yang lolos filter dengan pemisah " -------------- ".</li>
	 *   <li>Bungkus hasilnya dalam tag {@code &lt;marquee&gt;}, atau kembalikan string kosong
	 *       bila tidak ada teks yang lolos.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan.</b> Tag {@code &lt;marquee&gt;} sudah deprecated di HTML5 tetapi
	 * masih berfungsi di semua browser modern. Bila perlu animasi modern, ganti dengan CSS
	 * animation. Filter context-aware (sekolah vs PT) bergantung pada {@code chekPtAtauSekolah}.
	 * </p>
	 *
	 * @return string HTML marquee bila ada teks berjalan aktif yang relevan; string kosong bila tidak ada
	 */
	@SuppressWarnings("unchecked")
	public static String tampilanTextBerjalan() {
		ConstantValues.init();
		String text = "";
		Map<Long, GeneralValueObject> textBerjalans = ConstantValues.ambilBerdasarClass(TextBerjalan.class);

		if (textBerjalans != null) {

			Tbmuser tbmuser = Common.getCurrentUser();
			Fakultas fakultas = tbmuser == null ? null : tbmuser.getFakultas();
			Jurusan jurusan = tbmuser == null ? null : tbmuser.getJurusan();
			Sekolah sekolah = tbmuser == null ? null : tbmuser.getSekolah();
			Yayasan yayasan = tbmuser == null ? null : tbmuser.getYayasan();
			Program program = tbmuser == null ? null : tbmuser.getProgram();

			boolean[] a = Common.chekPtAtauSekolah(tbmuser);
			boolean ya = a[1];
			if (ya) {
				fakultas = null;
				jurusan = null;
				program = null;
				Sekolah sekolah2 = SekolahUtil.getSekolah();
				if (sekolah2 != null && sekolah2.getId() != null) {
					sekolah = sekolah2;
					yayasan = sekolah2.getYayasan();
				} else {
					Yayasan yayasan2 = SekolahUtil.getYayasan();
					if (yayasan2 != null && yayasan2.getId() != null) {
						yayasan = yayasan2;
					}
				}
			}
			for (Long generalValueObjectid : textBerjalans.keySet()) {
				TextBerjalan textBerjalan = (TextBerjalan) ConstantValues.ambil(TextBerjalan.class.getName(),
						generalValueObjectid);

				if ((fakultas == null) || (fakultas != null
						&& (textBerjalan.getFakultas() == null || (textBerjalan.getFakultas() != null
								&& fakultas.getId().equals(textBerjalan.getFakultas().getId()))))) {

					if ((jurusan == null) || (jurusan != null
							&& (textBerjalan.getJurusan() == null || (textBerjalan.getJurusan() != null
									&& jurusan.getId().equals(textBerjalan.getJurusan().getId()))))) {

						if ((yayasan == null) || (yayasan != null
								&& (textBerjalan.getYayasan() == null || (textBerjalan.getYayasan() != null
										&& yayasan.getId().equals(textBerjalan.getYayasan().getId()))))) {

							if ((sekolah == null) || (sekolah != null
									&& (textBerjalan.getSekolah() == null || (textBerjalan.getSekolah() != null
											&& sekolah.getId().equals(textBerjalan.getSekolah().getId()))))) {

								if ((program == null) || (program != null
										&& (textBerjalan.getProgram() == null || (textBerjalan.getProgram() != null
												&& program.getNama().equalsIgnoreCase(textBerjalan.getProgram()))))) {

									if (textBerjalan != null && textBerjalan.getAktif()) {
										text += text.isEmpty() ? textBerjalan.getNama()
												: " -------------- " + textBerjalan.getNama();
									}
								}
							}
						}
					}
				}
			}

		}
		String html = text.trim().isEmpty() ? ""
				: "<marquee behavior=\"scroll\" direction=\"left\">" + text + "</marquee>";

		return html;
	}

	/**
	 * Menghasilkan HTML tombol/link social login (Facebook, Google, Twitter, LinkedIn) untuk
	 * halaman login sistem tanpa konteks request HTTP.
	 *
	 * <p><b>Tujuan.</b> Menyediakan HTML fragment siap render yang berisi tombol "Login dengan
	 * Facebook/Google/Twitter/LinkedIn" untuk dimasukkan ke halaman login JSP/ZUL.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonUiFactoryHelper.tampilanSocialLogin()}.</p>
	 *
	 * @return string HTML berisi tombol social login; string kosong bila semua platform nonaktif
	 */
	public static String tampilanSocialLogin() {
		return CommonUiFactoryHelper.tampilanSocialLogin();
	}

	/**
	 * Memeriksa cookie "remember me" pada request HTTP dan bila valid, melakukan auto-login
	 * pengguna serta mengembalikan HTML redirect.
	 *
	 * <p><b>Tujuan.</b> Mengimplementasikan fitur "remember me" (login otomatis) pada halaman
	 * login — pengguna yang sebelumnya mencentang "ingat saya" tidak perlu login ulang.</p>
	 *
	 * <p><b>Cara kerja (inline).</b>
	 * <ol>
	 *   <li>Bila {@code req} atau {@code res} null, kembalikan string kosong.</li>
	 *   <li>Cari cookie bernama "userinfo" di request.</li>
	 *   <li>Decode URL encoding lalu dekripsi nilai cookie dengan {@code desEncrypter}
	 *       untuk mendapatkan username dan password (format: "user;pass").</li>
	 *   <li>Panggil {@code SecurityFilter.doAutoLogin} dengan kredensial tersebut.</li>
	 *   <li>Bila berhasil: kembalikan HTML dengan JavaScript {@code location.replace()} ke
	 *       halaman utama ({@code /main}) setelah 1500ms, dengan pesan "Anda telah login...".</li>
	 *   <li>Bila gagal atau tidak ada cookie: kembalikan string kosong.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Keamanan.</b> Cookie dienkripsi dengan DES ({@code desEncrypter}). DES tergolong
	 * lemah secara kriptografis — pertimbangkan migrasi ke AES-256. Auto-login tidak mengecek
	 * status akun (aktif/nonaktif) secara langsung; logika tersebut ada di {@code doAutoLogin}.</p>
	 *
	 * @param req request HTTP dari halaman login; boleh null (langsung return "")
	 * @param res response HTTP; boleh null (langsung return "")
	 * @return HTML string dengan redirect JavaScript bila auto-login berhasil; string kosong sebaliknya
	 */
	public static String remember(HttpServletRequest req, HttpServletResponse res) {
		String html = "";
		try {

			if (req != null && res != null) {
				Cookie[] cookies = req.getCookies();

				if (cookies != null) {
					String username = "";

					for (Cookie aCookie : cookies) {
						if (aCookie.getName().equals("userinfo")) {
							// Decode URL Encoded string dari JS kembali menjadi karakter asli
							username = java.net.URLDecoder.decode(aCookie.getValue(), "UTF-8");
						}
					}

					if (!username.trim().isEmpty()) {
						// Cookie legacy/rusak bisa gagal didekripsi (DesEncrypter.decrypt mengembalikan "")
						// atau hasil dekripsinya tidak mengandung delimiter ";" -- split() lalu menghasilkan
						// array 1 elemen. Cek panjang array dulu sebelum indexing agar tidak melempar
						// ArrayIndexOutOfBoundsException; kalau bentuknya tak sesuai, anggap saja "tidak
						// diingat" (no-op) tanpa mencoba auto-login.
						String s[] = Common.desEncrypter.get().decrypt(username).split(";");
						if (s.length >= 2) {
							String user = s[0];
							String pass = s[1];
							boolean mobile = false;
							if (SecurityFilter.doAutoLogin(user, pass, mobile, "Login via remeber me", req, res)) {

								String host = (req.isSecure() ? "https://" : "http://") + req.getServerName()
										+ (req.getServerPort() == 80 || req.getServerPort() == 443 ? ""
												: ":" + req.getServerPort())
										+ req.getContextPath();

								String redirect = host + "/main";
								html += "<h3 style=\"color:red;text-align:center;\">Anda telah login, karena browser Anda mengingat akun ini.. Harap tunggu, sedang di-arahkan ke halaman utama..</h3><br><script>\r\n"
										+ "function myFunction() {\r\n" + "  location.replace(\"" + redirect + "\")\r\n"
										+ "}\r\n" + "" + "setTimeout(myFunction(), 1500);" + "</script>";
							}
						}
					}
				}
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:14466");
		}

		return html;
	}

	/**
	 * Menghasilkan HTML tombol social login (varian 3) dengan konteks request/response HTTP
	 * untuk mendukung generasi URL OAuth yang mengandung host dinamis.
	 *
	 * <p><b>Tujuan.</b> Varian ketiga dari widget social login — dipakai di halaman login
	 * tertentu yang memerlukan format HTML atau tata letak yang berbeda dari varian 1 dan 2.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonUiFactoryHelper.tampilanSocialLogin3(req, res)}.</p>
	 *
	 * @param req request HTTP saat ini untuk menentukan host aplikasi; boleh null
	 * @param res response HTTP; boleh null
	 * @return string HTML tombol social login varian 3
	 */
	public static String tampilanSocialLogin3(HttpServletRequest req, HttpServletResponse res) {
		return CommonUiFactoryHelper.tampilanSocialLogin3(req, res);
	}

	/**
	 * Menghasilkan HTML tombol social login (varian 1 dengan request/response) untuk
	 * dimasukkan ke halaman login.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonUiFactoryHelper.tampilanSocialLogin(req, res)}.</p>
	 *
	 * @param req request HTTP; boleh null
	 * @param res response HTTP; boleh null
	 * @return string HTML tombol social login varian 1
	 */
	public static String tampilanSocialLogin(HttpServletRequest req, HttpServletResponse res) {
		return CommonUiFactoryHelper.tampilanSocialLogin(req, res);
	}

	/**
	 * Menghasilkan HTML tombol social login (varian 2 dengan request/response) untuk
	 * dimasukkan ke halaman login.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonUiFactoryHelper.tampilanSocialLogin2(req, res)}.</p>
	 *
	 * @param req request HTTP; boleh null
	 * @param res response HTTP; boleh null
	 * @return string HTML tombol social login varian 2
	 */
	public static String tampilanSocialLogin2(HttpServletRequest req, HttpServletResponse res) {
		return CommonUiFactoryHelper.tampilanSocialLogin2(req, res);
	}

	/**
	 * Melanjutkan proses social login setelah OAuth callback berhasil — mencocokkan akun
	 * sosial dengan akun sistem yang ada atau meminta pengguna menautkan akun secara manual.
	 *
	 * <p><b>Tujuan.</b> Menangani seluruh alur lanjutan social login: menambahkan ID sosial ke
	 * akun yang sudah login, atau menemukan akun yang cocok berdasarkan email/ID sosial, atau
	 * meminta pengguna memasukkan kredensial untuk menautkan akun baru.</p>
	 *
	 * <p><b>Cara kerja (inline — sangat kompleks, 3 alur utama).</b>
	 * <ol>
	 *   <li><b>Bila email kosong:</b> redirect ke halaman utama (email wajib dari OAuth).</li>
	 *   <li><b>Bila {@code currentUser} tidak null (sudah login):</b>
	 *     <ul>
	 *       <li>Bila mahasiswa ditemukan via relasi: tambahkan ID sosial ke entitas Mahasiswa,
	 *           perbarui profil sosial, commit, lalu doLogin ke halaman mahasiswa.</li>
	 *       <li>Bila Tbmuser: tambahkan ID sosial ke Tbmuser, perbarui profil, commit, doLogin.</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>Bila belum login ({@code currentUser} null):</b>
	 *     <ul>
	 *       <li>Cek apakah ada lebih dari satu akun dengan ID/email ini via
	 *           {@link #checkSocialMediaApakahLebihDariSatu}; bila ya, tampilkan picker user.</li>
	 *       <li>Cari di Tbmuser, Mahasiswa, atau Siswa yang cocok email atau ID sosialnya.</li>
	 *       <li>Bila ditemukan satu: tambahkan ID sosial (jika belum ada) dan doLogin.</li>
	 *       <li>Bila tidak ditemukan dan konfigurasi mengizinkan ({@code pengguna_bisa_memasukkan_sendiri_akun}):
	 *           tampilkan form input username/password untuk menautkan akun secara manual
	 *           (popup modal dengan validasi di onOK).</li>
	 *       <li>Bila tidak ditemukan dan tidak dikonfigurasi: tampilkan pesan "Email tidak terdaftar".</li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan.</b> Format profil sosial: {@code property:id||link||picture||email||nama}
	 * dipisahkan ";" per akun. Kolom DB yang diupdate tergantung {@code property} (facebookId,
	 * googleId, twitterId, linkedinId). Metode ini cukup panjang (~400 baris) — kandidat utama
	 * untuk refaktor ke helper class terpisah.</p>
	 *
	 * @param currentUser   pengguna yang saat ini sedang login (dari sesi ZK); null bila belum login
	 * @param kolom         nama kolom DB platform sosial (misal "facebook_id")
	 * @param id            ID unik akun sosial dari OAuth provider
	 * @param property      nama properti entitas (misal "facebookId")
	 * @param linkProfile   URL profil sosial pengguna
	 * @param pictureUrl    URL foto profil sosial
	 * @param emailAddress  alamat email dari akun sosial; wajib ada (bila null akan redirect)
	 * @param formattedName nama lengkap dari akun sosial
	 * @param img           path logo platform (untuk ditampilkan di form taut akun)
	 * @param callback_url  URL yang menjadi tujuan redirect setelah login berhasil
	 * @throws Exception bila terjadi kegagalan sesi Hibernate atau ZK
	 */
	@SuppressWarnings("deprecation")
	public static void lanjutProcessSocialMedia(final Tbmuser currentUser, String kolom, final String id,
			final String property, final String linkProfile, final String pictureUrl, final String emailAddress,
			final String formattedName, final String img, final String callback_url) throws Exception {

		if (emailAddress == null || emailAddress.trim().isEmpty()) {
			System.out.println("[SOCIAL-LOGIN] GAGAL (" + property + "): provider tidak mengembalikan alamat email "
					+ "(id=" + id + ") -- login dibatalkan, redirect ke home.");
			Executions.sendRedirect(Common.getRequestHostWithProtocol());
			return;
		}

		// System.out.println("kolom=>" + kolom + ", id=" + id + ", property=" +
		// property + ", linkProfile=" + linkProfile
		// + ", pictureUrl=" + pictureUrl + ", emailAddress=" + emailAddress +
		// ", formattedName=" + formattedName
		// + ", img=" + img + ", callback_url = " + callback_url);

		if (currentUser != null) {
			System.out.println("[SOCIAL-LOGIN] TAUTKAN AKUN (" + property + "=" + id + ", email=" + emailAddress
					+ "): pengguna sudah login sbg " + currentUser.getUserId() + " -- menautkan akun sosial ke akun ini.");

			String key = property + ":" + id + "||";
			Session session = HibernateUtil.currentSession();
			final Mahasiswa mahasiswa = currentUser.getMahasiswa();
			if (mahasiswa != null) {

				session.refresh(mahasiswa);
				// System.out.println("mhs facebookId => " +
				// mahasiswa.getFacebookId() + ", id => " + id + ", "
				// + mahasiswa.getFacebookId().contains(id));
				if (!StringUtils.contains(mahasiswa.getSocialMediaProfile(), key)
						|| ((property.equalsIgnoreCase("facebookId") && !mahasiswa.getFacebookId().contains(id))
								|| (property.equalsIgnoreCase("googleId") && !mahasiswa.getGoogleId().contains(id))
								|| (property.equalsIgnoreCase("twitterId") && !mahasiswa.getTwitterId().contains(id))
								|| (property.equalsIgnoreCase("linkedinId") && !mahasiswa.getLinkedinId().contains(id)))

				) {
					if (property.equalsIgnoreCase("facebookId")) {
						mahasiswa.appendFacebookId(id);
					} else if (property.equalsIgnoreCase("googleId")) {
						mahasiswa.appendGoogleId(id);
					} else if (property.equalsIgnoreCase("twitterId")) {
						mahasiswa.appendTwitterId(id);
					} else if (property.equalsIgnoreCase("linkedinId")) {
						mahasiswa.appendLinkedinId(id);
					}
					if (emailAddress != null && !emailAddress.trim().isEmpty()) {
						mahasiswa.appendEmail(emailAddress);
					}
					mahasiswa.setSocialMediaProfile(mahasiswa.getSocialMediaProfile() + ";" + property + ":" + id + "||"
							+ linkProfile + "||" + pictureUrl + "||" + emailAddress + "||" + formattedName);
					Common.refreshUpdate(session, mahasiswa);
				}
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.doLogin(mahasiswa, linkProfile, "?tambah_akun=true");
					}
				});
			} else {
				session.refresh(currentUser);
				if (!StringUtils.contains(currentUser.getSocialMediaProfile(), key)
						|| ((property.equalsIgnoreCase("facebookId") && !currentUser.getFacebookId().contains(id))
								|| (property.equalsIgnoreCase("googleId") && !currentUser.getGoogleId().contains(id))
								|| (property.equalsIgnoreCase("twitterId") && !currentUser.getTwitterId().contains(id))
								|| (property.equalsIgnoreCase("linkedinId")
										&& !currentUser.getLinkedinId().contains(id)))

				) {
					if (property.equalsIgnoreCase("facebookId")) {
						currentUser.appendFacebookId(id);
					} else if (property.equalsIgnoreCase("googleId")) {
						currentUser.appendGoogleId(id);
					} else if (property.equalsIgnoreCase("twitterId")) {
						currentUser.appendTwitterId(id);
					} else if (property.equalsIgnoreCase("linkedinId")) {
						currentUser.appendLinkedinId(id);
					}
					if (emailAddress != null && !emailAddress.trim().isEmpty()) {
						currentUser.appendEmail(emailAddress);
					}
					currentUser.setSocialMediaProfile(currentUser.getSocialMediaProfile() + ";" + property + ":" + id
							+ "||" + linkProfile + "||" + pictureUrl + "||" + emailAddress + "||" + formattedName);
					Common.refreshUpdate(session, currentUser);
				}
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.doLogin(currentUser, linkProfile, "?tambah_akun=true");
					}
				});

			}
		} else {

			if (Common.checkSocialMediaApakahLebihDariSatu(kolom, id, emailAddress, linkProfile, callback_url)) {
				return;
			}

			Session session = HibernateUtil.currentSession();

			String sql = kolom + " is not null and this_.aktif=true and '" + id + "' = ANY(string_to_array(" + kolom
					+ ",','))";

//			if (((Number) session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.sqlRestriction(sql))
//					.setProjection(Projections.rowCount()).uniqueResult()).intValue() > 1) {
//				return;
//			}

			Tbmuser tbmuser = (Tbmuser) ConstantValues.simpleObject(session.createCriteria(Tbmuser.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.or(Restrictions.ilike("email", emailAddress, MatchMode.START),
							Restrictions.sqlRestriction(sql)))
					.addOrder(Order.desc("tanggal_dirubah"))
					.setMaxResults(1), Tbmuser.class);
			if (tbmuser != null) {
				System.out.println("[SOCIAL-LOGIN] COCOK (" + property + "=" + id + ", email=" + emailAddress
						+ "): ditemukan sbg Tbmuser id=" + tbmuser.getId() + " userId=" + tbmuser.getUserId()
						+ " -- lanjut doLogin.");
				if (!tbmuser.getSocialMediaProfile().contains(property)) {
					if (emailAddress != null && !emailAddress.trim().isEmpty()) {
						tbmuser.appendEmail(emailAddress);
					}
					tbmuser.setSocialMediaProfile(tbmuser.getSocialMediaProfile() + ";" + property + ":" + id + "||"
							+ linkProfile + "||" + pictureUrl + "||" + emailAddress + "||" + formattedName);
					Common.refreshUpdate(session, tbmuser);
				}
				Common.doLogin(tbmuser, linkProfile, callback_url);
			} else {
				Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.simpleObject(session.createCriteria(Mahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.or(Restrictions.ilike("email", emailAddress, MatchMode.START),
								Restrictions.sqlRestriction(sql)))
						.setMaxResults(1), Mahasiswa.class);
				if (mahasiswa != null) {
					System.out.println("[SOCIAL-LOGIN] COCOK (" + property + "=" + id + ", email=" + emailAddress
							+ "): ditemukan sbg Mahasiswa id=" + mahasiswa.getId() + " nim=" + mahasiswa.getNim()
							+ " nama=" + mahasiswa.getNama() + " -- lanjut doLogin.");
					if (!mahasiswa.getSocialMediaProfile().contains(property)) {
						if (emailAddress != null && !emailAddress.trim().isEmpty()) {
							mahasiswa.appendEmail(emailAddress);
						}
						mahasiswa.setSocialMediaProfile(mahasiswa.getSocialMediaProfile() + ";" + property + ":" + id
								+ "||" + linkProfile + "||" + pictureUrl + "||" + emailAddress + "||" + formattedName);
						Common.refreshUpdate(session, mahasiswa);
					}
					Common.doLogin(mahasiswa, linkProfile, callback_url);
				} else {

					Siswa siswa = (Siswa) ConstantValues
							.simpleObject(session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
									.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
									.add(Restrictions.or(
											Restrictions.ilike("alamatEmail", emailAddress, MatchMode.START),
											Restrictions.sqlRestriction(sql)))
									.setMaxResults(1), Siswa.class);
					if (siswa != null) {
						System.out.println("[SOCIAL-LOGIN] COCOK (" + property + "=" + id + ", email=" + emailAddress
								+ "): ditemukan sbg Siswa id=" + siswa.getId() + " nomorInduk=" + siswa.getNomorInduk()
								+ " nama=" + siswa.getNamaSiswa() + " -- lanjut doLogin.");
						if (!siswa.getSocialMediaProfile().contains(property)) {
							if (emailAddress != null && !emailAddress.trim().isEmpty()) {
								siswa.appendEmail(emailAddress);
							}
							siswa.setSocialMediaProfile(siswa.getSocialMediaProfile() + ";" + property + ":" + id + "||"
									+ linkProfile + "||" + pictureUrl + "||" + emailAddress + "||" + formattedName);
							Common.refreshUpdate(session, siswa);
						}
						Common.doLogin(siswa, linkProfile, callback_url);
					} else {

						System.out.println("[SOCIAL-LOGIN] TIDAK DITEMUKAN (" + property + "=" + id + ", email="
								+ emailAddress + "): tidak ada Tbmuser/Mahasiswa/Siswa aktif yg cocok berdasarkan "
								+ "email maupun " + kolom + " -- kemungkinan akun belum terdaftar/tertaut.");

						if (Common.bolehKonfigurasi("pengguna_bisa_memasukkan_sendiri_akun_jika_email_belum_terdaftar", Konfigurasi.TIDAK_AKTIF)) {

							final MyWindow window = new MyWindow(
									"Login Via " + property.substring(0, property.length() - 2), "none", false);
							window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							window.setHeight("250px");
							window.setWidth("450px");

							Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
							borderlayout.setParent(window);
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

							final Textbox username = new Textbox();
							final Textbox password = new Textbox();

							EventListener eventListener = new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									if (username.getValue().trim().equals("")) {
										MyMessageboxConfig.show("Username harus diisi", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
										return;
									}
									if (password.getValue().trim().equals("")) {
										MyMessageboxConfig.show("Password tidak benar, coba ulangi lagi", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
										return;
									}

									String pass = Common.desEncrypter.get().encrypt(password.getValue().trim());
									Session session = HibernateUtil.currentSession();
									final Tbmuser tbmuser = (Tbmuser) ConstantValues.simpleObject(
											session.createCriteria(Tbmuser.class)
													.add(Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", true)))
													.add(Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", true)))
													.add(Restrictions.eq("userId", username.getValue().trim()))
													.add(Restrictions.eq("userPassword", pass)).setMaxResults(1),
											Tbmuser.class);
									if (tbmuser != null) {
										if (property.equalsIgnoreCase("facebookId")) {
											tbmuser.appendFacebookId(id);
										} else if (property.equalsIgnoreCase("googleId")) {
											tbmuser.appendGoogleId(id);
										} else if (property.equalsIgnoreCase("twitterId")) {
											tbmuser.appendTwitterId(id);
										} else if (property.equalsIgnoreCase("linkedinId")) {
											tbmuser.appendLinkedinId(id);
										}
										if (emailAddress != null && !emailAddress.trim().isEmpty()) {
											tbmuser.appendEmail(emailAddress);
										}
										tbmuser.setSocialMediaProfile(tbmuser.getSocialMediaProfile() + ";" + property
												+ ":" + id + "||" + linkProfile + "||" + pictureUrl + "||"
												+ emailAddress + "||" + formattedName);

										Common.refreshUpdate(session, tbmuser);
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												Common.doLogin(tbmuser, linkProfile, callback_url);
											}
										});

									} else {
										final Mahasiswa mahasiswa = (Mahasiswa) ConstantValues
												.simpleObject(
														session.createCriteria(Mahasiswa.class)
																.add(Restrictions.or(Restrictions.isNull("aktif"),
																		Restrictions.eq("aktif", true)))
																.add(Restrictions.or(Restrictions.isNull("aktif"),
																		Restrictions.eq("aktif", true)))
																.add(Restrictions.eq("nim", username.getValue().trim()))
																.add(Restrictions.eq("pass", pass)).setMaxResults(1),
														Mahasiswa.class);
										if (mahasiswa != null) {
											if (property.equalsIgnoreCase("facebookId")) {
												mahasiswa.appendFacebookId(id);
											} else if (property.equalsIgnoreCase("googleId")) {
												mahasiswa.appendGoogleId(id);
											} else if (property.equalsIgnoreCase("twitterId")) {
												mahasiswa.appendTwitterId(id);
											} else if (property.equalsIgnoreCase("linkedinId")) {
												mahasiswa.appendLinkedinId(id);
											}
											if (emailAddress != null && !emailAddress.trim().isEmpty()) {
												mahasiswa.appendEmail(emailAddress);
											}
											mahasiswa.setSocialMediaProfile(mahasiswa.getSocialMediaProfile() + ";"
													+ property + ":" + id + "||" + linkProfile + "||" + pictureUrl
													+ "||" + emailAddress + "||" + formattedName);
											Common.refreshUpdate(session, mahasiswa);
											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													Common.doLogin(mahasiswa, linkProfile, callback_url);
												}
											});
										} else {

											final Siswa siswa = (Siswa) ConstantValues.simpleObject(
													session.createCriteria(Siswa.class)
															.add(Restrictions.isNotNull("namaSiswa"))
															.add(Restrictions.ne("namaSiswa", ""))
															.add(Restrictions.isNotNull("sekolah"))
															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true)))
															.add(Restrictions.eq("nim", username.getValue().trim()))
															.add(Restrictions.eq("pass", pass)).setMaxResults(1),
													Siswa.class);
											if (siswa != null) {
												if (property.equalsIgnoreCase("facebookId")) {
													siswa.appendFacebookId(id);
												} else if (property.equalsIgnoreCase("googleId")) {
													siswa.appendGoogleId(id);
												} else if (property.equalsIgnoreCase("twitterId")) {
													siswa.appendTwitterId(id);
												} else if (property.equalsIgnoreCase("linkedinId")) {
													siswa.appendLinkedinId(id);
												}
												if (emailAddress != null && !emailAddress.trim().isEmpty()) {
													siswa.appendEmail(emailAddress);
												}
												siswa.setSocialMediaProfile(siswa.getSocialMediaProfile() + ";"
														+ property + ":" + id + "||" + linkProfile + "||" + pictureUrl
														+ "||" + emailAddress + "||" + formattedName);
												Common.refreshUpdate(session, siswa);
												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														Common.doLogin(siswa, linkProfile, callback_url);
													}
												});
											} else {

												MyMessageboxConfig.show("Username atau password tidak sesuai",
														"Peringatan", MyMessageboxConfig.OK,
														MyMessageboxConfig.EXCLAMATION);
												return;
											}
										}
									}
								}
							};

							MyFormRow row = new MyFormRow();
							row.setValign("top");

							row.setParent(rows);
							ais.ui.util.ZkCompat.setSpans(row, "2");
							row.appendChild(new ais.ui.util.MyHtml("Karena Anda baru pertama kali login via "
									+ property.substring(0, property.length() - 2)
									+ " menggunakan akun ini, username dan password Anda di sistem akademik ini harus diinput sekali lagi, selanjutnya, jika Anda kembali login ke sistem akademik ini, cukup dengan meng-klik tanda <img src='"
									+ Common.getRequestHostWithProtocol() + img
									+ "' width='110px'  height='22px'/> di halaman login.<br><hr>"));

							row = new MyFormRow();

							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig("Username"));
							row.appendChild(username);
							username.setWidth("90%");
							username.focus();
							username.addEventListener("onOK", eventListener);

							row = new MyFormRow();

							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig("Password"));
							row.appendChild(password);
							password.setType("password");
							password.setWidth("90%");
							password.setValue("");
							password.addEventListener("onOK", eventListener);

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
									Executions.sendRedirect(Common.getRequestHostWithProtocol());
								}
							});
							cancel.setParent(toolbar);
							MyToolbarbuttonConfig save = new MyToolbarbuttonConfig(
									"Aktifkan login via " + property.substring(0, property.length() - 2),
									"/img/save.gif");
							save.setTooltiptext("Login");
							save.addEventListener("onClick", eventListener);
							save.setParent(toolbar);

							window.onModal();
						} else {
							System.out.println("[SOCIAL-LOGIN] DITOLAK (" + property + "=" + id + ", email="
									+ emailAddress + "): akun belum terdaftar & konfigurasi "
									+ "\"pengguna_bisa_memasukkan_sendiri_akun_jika_email_belum_terdaftar\" nonaktif "
									+ "-- tampil pesan \"tidak terdaftar\" ke pengguna, redirect ke home.");
							MyMessageboxConfig.show(
									"Email yang Anda gunakan untuk masuk ke sistem tidak terdaftar, harap hubungi Admin untuk mendaftarakan akun email Anda",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
									new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											Executions.sendRedirect(Common.getRequestHostWithProtocol());
										}
									});
						}
					}
				}
			}
		}
	}

	/**
	 * Memeriksa apakah ada lebih dari satu akun pengguna yang cocok dengan ID atau email sosial
	 * tertentu, dan bila ya, menampilkan popup pilihan pengguna (user picker).
	 *
	 * <p><b>Tujuan.</b> Menangani kasus ambiguitas social login ketika satu akun media sosial
	 * bisa dikaitkan ke beberapa akun sistem (misal mahasiswa dan akun admin dengan email yang sama).
	 * Pengguna memilih sendiri akun mana yang ingin digunakan.</p>
	 *
	 * <p><b>Cara kerja (inline).</b>
	 * <ol>
	 *   <li>Jalankan SQL native yang menghitung jumlah akun cocok di 3 tabel (tbmuser, mahasiswa,
	 *       sekolah.siswa) berdasarkan email ilike dan ID sosial di kolom array PostgreSQL
	 *       ({@code ANY(string_to_array(kolom,','))}).</li>
	 *   <li>Bila jumlah ≤ 1, kembalikan {@code false} (tidak ada ambiguitas).</li>
	 *   <li>Bila &gt; 1: buat popup modal "Pilih Pengguna" (550px × 350px) dengan grid 4 kolom
	 *       (foto, nama, jenis pengguna, tombol Login).</li>
	 *   <li>Tampilkan semua Tbmuser, Mahasiswa, Siswa yang cocok sebagai baris grid; masing-masing
	 *       punya tombol "Login" yang memanggil {@code doLogin} untuk entitas tersebut.</li>
	 *   <li>Sertakan tombol Batal yang redirect ke halaman utama.</li>
	 *   <li>Kembalikan {@code true} untuk memberi tahu pemanggil bahwa popup sudah ditampilkan
	 *       dan proses normal harus dihentikan.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan.</b> Query SQL menggunakan PostgreSQL-specific syntax ({@code ANY},
	 * {@code string_to_array}) — tidak portabel ke database lain. Foto pengguna diambil via
	 * {@code CommonMedia.tampilkanGambarKecil}.</p>
	 *
	 * @param kolom        nama kolom DB platform sosial (misal "facebook_id")
	 * @param id           ID sosial yang dicari
	 * @param email        email akun sosial
	 * @param linkProfile  URL profil sosial (untuk doLogin setelah dipilih)
	 * @param callback_url URL redirect setelah login
	 * @return {@code true} bila popup picker sudah ditampilkan (ambiguitas ada);
	 *         {@code false} bila tidak ada ambiguitas (0 atau 1 akun cocok)
	 * @throws Exception bila terjadi kegagalan query atau inisialisasi ZK
	 */
	public static boolean checkSocialMediaApakahLebihDariSatu(String kolom, String id, String email,
			final String linkProfile, final String callback_url) throws Exception {
		Session session = HibernateUtil.currentSession();
		boolean transaksiLokal = false;
		String sql = "select (select count(*) from mahasiswa where " + kolom
				+ " is not null and aktif=true and (email ilike '" + email + "%' or '" + id + "' = ANY(string_to_array("
				+ kolom + ",',')))) +  (select count(*) from sekolah.siswa where " + kolom
				+ " is not null and aktif=true and (alamat_email ilike '" + email + "%' or '" + id
				+ "' = ANY(string_to_array(" + kolom + ",','))))  +  (select count(*) from tbmuser where " + kolom
				+ " is not null and aktif=true and (email ilike '" + email + "%' or '" + id + "' = ANY(string_to_array("
				+ kolom + ",',')))) as qty";
		if (session.getTransaction() == null || !session.getTransaction().isActive()) {
			session.beginTransaction();
			transaksiLokal = true;
		}
		int count = ((Number) session.createSQLQuery(sql).addScalar("qty", org.hibernate.Hibernate.INTEGER)
				.uniqueResult()).intValue();
		if (count > 1) {
			System.out.println("[SOCIAL-LOGIN] AMBIGU (" + kolom + "=" + id + ", email=" + email + "): " + count
					+ " akun cocok sekaligus -- menampilkan popup \"Pilih Pengguna\", menunggu pengguna memilih.");
			sql = kolom + " is not null and this_.aktif=true and '" + id + "' = ANY(string_to_array(" + kolom
					+ ",','))";

			final MyWindow window = new MyWindow("Pilih Pengguna", "none", false);
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			window.setHeight("350px");
			window.setWidth(Common.isMobile() ? "100%" : "550px");

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(window);
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

			MyColumnConfig column = new MyColumnConfig("");
			column.setParent(columns);
			column.setWidth("100px");

			column = new MyColumnConfig("Nama Pengguna");
			column.setParent(columns);
			column.setWidth("40%");

			column = new MyColumnConfig("Jenis Pengguna");
			column.setParent(columns);

			column = new MyColumnConfig("Login");
			column.setParent(columns);
			column.setWidth("15%");

			Rows rows = new Rows();
			rows.setParent(grid);

			List<Tbmuser> tbmusers = ConstantValues.simpleList(session.createCriteria(Tbmuser.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions
							.or(Restrictions.ilike("email", email, MatchMode.START), Restrictions.sqlRestriction(sql))),
					Tbmuser.class);
			for (final Tbmuser tbmuser : tbmusers) {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				CommonMedia.tampilkanGambarKecil(tbmuser).setParent(row);

				new Label(tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")").setParent(row);
				new Label(tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getRoleName()).setParent(row);
				MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Login", "/img/svg/check2.svg");
				toolbarbutton.setOrient("vertical");
				toolbarbutton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.doLogin(tbmuser, linkProfile, callback_url);
					}
				});
				toolbarbutton.setParent(row);
			}

			List<Mahasiswa> mahasiswas = ConstantValues.simpleList(session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions
							.or(Restrictions.ilike("email", email, MatchMode.START), Restrictions.sqlRestriction(sql))),
					Mahasiswa.class);
			for (final Mahasiswa mahasiswa : mahasiswas) {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(row);
				new Label(mahasiswa.getNama() + " (" + mahasiswa.getNim() + ")").setParent(row);
				new Label(ais.common.Common.getBahasaConfig("Mahasiswa")).setParent(row);
				MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Login", "/img/svg/check2.svg");
				toolbarbutton.setOrient("vertical");
				toolbarbutton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.doLogin(mahasiswa, linkProfile, callback_url);
					}
				});
				toolbarbutton.setParent(row);
			}

			List<Siswa> siswas = ConstantValues.simpleList(session.createCriteria(Siswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.or(Restrictions.ilike("alamatEmail", email, MatchMode.START),
							Restrictions.sqlRestriction(sql))),
					Siswa.class);
			for (final Siswa siswa : siswas) {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				CommonMedia.tampilkanGambarKecil(siswa).setParent(row);
				new Label(siswa.getNama() + " (" + siswa.getNomorInduk() + ")").setParent(row);
				new Label(ais.common.Common.getBahasaConfig("Siswa")).setParent(row);
				MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Login", "/img/svg/check2.svg");
				toolbarbutton.setOrient("vertical");
				toolbarbutton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.doLogin(siswa, linkProfile, callback_url);
					}
				});
				toolbarbutton.setParent(row);
			}

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
					Executions.sendRedirect(Common.getRequestHostWithProtocol());
				}
			});
			cancel.setParent(toolbar);

			if (transaksiLokal && session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().commit();
				transaksiLokal = false;
			}
			window.onModal();

			return true;
		} else {
			if (transaksiLokal && session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().commit();
				transaksiLokal = false;
			}
			return false;
		}
	}

	/**
	 * Menginisialisasi dan merender satu baris cicilan pembayaran dalam grid tagihan,
	 * lengkap dengan widget bukti bayar dan tombol hapus.
	 *
	 * <p><b>Tujuan.</b> Membangun tampilan baris cicilan di form pembayaran — menampilkan
	 * detail cicilan (nominal, tanggal, status) beserta file lampiran bukti bayar dan
	 * tombol untuk menghapus cicilan tersebut.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonPaymentHelper.initCicilan}.</p>
	 *
	 * @param buktiPembayarans map ID → LampiranLain untuk cache file bukti bayar
	 * @param rowsCicilan      container Rows ZK untuk baris-baris cicilan
	 * @param row              baris ZK tempat widget akan ditambahkan
	 * @param i                indeks urutan cicilan (1-based)
	 * @param cicilanPembayaran data cicilan yang akan dirender; tidak boleh null
	 * @param buttonHapus      tombol hapus cicilan yang sudah dikonfigurasi; boleh null
	 * @return Hbox berisi widget cicilan yang sudah dibuat
	 */
	public static Hbox initCicilan(final Map<Long, LampiranLain> buktiPembayarans, final Rows rowsCicilan,
			final Row row, int i, CicilanPembayaran cicilanPembayaran, MyToolbarbuttonConfig buttonHapus) {
		return CommonPaymentHelper.initCicilan(buktiPembayarans, rowsCicilan, row, i, cicilanPembayaran, buttonHapus);
	}

	/**
	 * Memfilter daftar {@code CicilanPembayaran} — menghapus entri yang tidak relevan atau
	 * tidak valid berdasarkan aturan bisnis yang didefinisikan di helper.
	 *
	 * <p><b>Tujuan.</b> Membersihkan daftar cicilan sebelum ditampilkan ke pengguna atau
	 * diproses — misalnya menghapus cicilan yang sudah terhapus lunak atau duplikat.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonPaymentHelper.filterCicilanPembayaran}.</p>
	 *
	 * @param cicilanPembayarans daftar cicilan mentah yang akan difilter; tidak boleh null
	 * @return daftar cicilan yang sudah difilter
	 */
	public static List<CicilanPembayaran> filterCicilanPembayaran(List<CicilanPembayaran> cicilanPembayarans) {
		return CommonPaymentHelper.filterCicilanPembayaran(cicilanPembayarans);
	}

	/**
	 * Mengkonversi string warna heksadesimal CSS (format "#RRGGBB") ke representasi
	 * array JSON {@code [R,G,B]}.
	 *
	 * <p><b>Tujuan.</b> Mengkonversi kode warna yang disimpan di database (misal warna
	 * kategori atau label) menjadi format array angka yang bisa dikonsumsi oleh library
	 * JavaScript untuk grafik atau Canvas rendering.</p>
	 *
	 * <p><b>Cara kerja.</b> Ekstrak 3 pasang karakter hex dari {@code colorStr} (posisi 1-2,
	 * 3-4, 5-6), parse ke Integer basis 16, lalu gabungkan dalam string "[R,G,B]".</p>
	 *
	 * <p><b>Pemeliharaan.</b> Hanya mendukung format 6-digit hex dengan prefix "#" (misal
	 * "#FFFFFF"). Format 3-digit atau tanpa "#" akan menghasilkan hasil salah atau exception.</p>
	 *
	 * @param colorStr kode warna CSS hex (misal "#FF8800"); tidak boleh null, harus 7 karakter
	 * @return string format "[R,G,B]" (misal "[255,136,0]")
	 */
	public static String hex2Rgb(String colorStr) {
		return "[" + Integer.valueOf(colorStr.substring(1, 3), 16) + "," + Integer.valueOf(colorStr.substring(3, 5), 16)
				+ "," + Integer.valueOf(colorStr.substring(5, 7), 16) + "]";
	}

	/**
	 * Mengkonversi nilai numerik desimal (representasi jam Excel/Google Sheets) ke objek
	 * {@code Date} yang mewakili waktu dalam hari yang sama.
	 *
	 * <p><b>Tujuan.</b> Mengubah nilai waktu yang tersimpan sebagai desimal (misal 8.5 =
	 * pukul 08:30) menjadi objek {@code Date} yang dapat digunakan di kalkulasi waktu atau
	 * ditampilkan dalam format waktu standar.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Ambil bagian jam ({@code Math.floor(nilai)}) dan menit ({@code (nilai-jam)*60}).</li>
	 *   <li>Hitung detik dari sisa desimal menit.</li>
	 *   <li>Set nilai ke Calendar dan tambahkan 1 detik (adjustment).</li>
	 *   <li>Kembalikan {@code calendar.getTime()}.</li>
	 * </ol>
	 * </p>
	 *
	 * @param nilai nilai desimal mewakili jam (misal 8.5 = 08:30:00)
	 * @return objek Date yang mewakili waktu tersebut pada hari ini
	 */
	public static Date convertNumericToTime(Double nilai) {
		double jam = Math.floor(nilai);
		double menit = ((nilai - jam) * 60.0);
		double second = (menit - Math.floor(menit)) * 60.0;
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		// calendar.set(Calendar.YEAR, calendar.getMinimum(Calendar.YEAR));
		// calendar.set(Calendar.MONTH, calendar.getMinimum(Calendar.MONTH));
		// calendar.set(Calendar.DATE, calendar.getMinimum(Calendar.DATE));
		calendar.set(Calendar.HOUR_OF_DAY, (int) jam);
		calendar.set(Calendar.MINUTE, (int) menit);
		calendar.set(Calendar.SECOND, (int) second + 1);
		calendar.set(Calendar.MILLISECOND, 0);
		Date waktu = calendar.getTime();
		return waktu;
	}

	/**
	 * Membuat widget {@code Hbox} berisi tombol aksi baris (Ubah dan Hapus) untuk grid CRUD.
	 *
	 * <p><b>Tujuan.</b> Menyediakan tombol aksi standar yang konsisten di seluruh halaman CRUD
	 * sistem — dipakai oleh ratusan Action class sehingga UI tombol baris dapat diperbarui
	 * secara global di satu tempat.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonUiFactoryHelper.copyEditDeleteButtons(edit, delete, obj, dataInitDefault)}.
	 * Versi ini hanya mendukung tombol Ubah dan Hapus (tanpa Copy).</p>
	 *
	 * @param edit            {@code true} untuk menampilkan tombol Ubah
	 * @param delete          {@code true} untuk menampilkan tombol Hapus
	 * @param obj             entitas baris yang akan diedit/dihapus; tidak boleh null
	 * @param dataInitDefault controller grid yang menangani aksi edit/delete
	 * @return Hbox berisi tombol-tombol aksi yang sudah terkonfigurasi
	 */
	public static Hbox copyEditDeleteButtons(boolean edit, boolean delete, GeneralValueObject obj,
			DataInitDefault dataInitDefault) {
		return CommonUiFactoryHelper.copyEditDeleteButtons(edit, delete, obj, dataInitDefault);
	}

	/**
	 * Membuat widget {@code Hbox} berisi tombol aksi baris (Copy, Ubah, dan/atau Hapus) untuk
	 * grid CRUD.
	 *
	 * <p><b>Tujuan.</b> Versi lengkap dari {@link #copyEditDeleteButtons(boolean, boolean, GeneralValueObject, DataInitDefault)}
	 * yang juga mendukung tombol Copy (duplikasi entitas).</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonUiFactoryHelper.copyEditDeleteButtons(edit, copy, delete, obj, dataInitDefault)}.</p>
	 *
	 * @param edit            {@code true} untuk tampilkan tombol Ubah
	 * @param copy            {@code true} untuk tampilkan tombol Copy/Duplikat
	 * @param delete          {@code true} untuk tampilkan tombol Hapus
	 * @param obj             entitas yang diwakili baris; tidak boleh null
	 * @param dataInitDefault controller yang menangani aksi
	 * @return Hbox berisi tombol-tombol aksi
	 */
	public static Hbox copyEditDeleteButtons(boolean edit, boolean copy, boolean delete, final GeneralValueObject obj,
			final DataInitDefault dataInitDefault) {
		return CommonUiFactoryHelper.copyEditDeleteButtons(edit, copy, delete, obj, dataInitDefault);
	}

	/**
	 * Membuat widget {@code Hbox} berisi tombol aksi baris (Copy, Ubah, Hapus) dengan opsi
	 * menampilkan label teks pada tombol selain ikon.
	 *
	 * <p><b>Tujuan.</b> Varian dari {@link #copyEditDeleteButtons(boolean, boolean, boolean, GeneralValueObject, DataInitDefault)}
	 * untuk konteks di mana tombol memerlukan teks label (bukan hanya ikon) — meningkatkan
	 * aksesibilitas pada layar sempit atau untuk pengguna baru.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonUiFactoryHelper.copyEditDeleteButtons(edit, copy, delete, obj, dataInitDefault, label)}.</p>
	 *
	 * @param edit            {@code true} untuk tampilkan tombol Ubah
	 * @param copy            {@code true} untuk tampilkan tombol Copy
	 * @param delete          {@code true} untuk tampilkan tombol Hapus
	 * @param obj             entitas yang diwakili baris
	 * @param dataInitDefault controller yang menangani aksi
	 * @param label           {@code true} untuk menampilkan teks label pada tombol
	 * @return Hbox berisi tombol-tombol aksi dengan atau tanpa label teks
	 */
	public static Hbox copyEditDeleteButtons(boolean edit, boolean copy, boolean delete, final GeneralValueObject obj,
			final DataInitDefault dataInitDefault, boolean label) {
		return CommonUiFactoryHelper.copyEditDeleteButtons(edit, copy, delete, obj, dataInitDefault, label);
	}

	/**
	 * Memeriksa apakah fitur login PMB via cookie diaktifkan di konfigurasi sistem.
	 *
	 * <p><b>Tujuan.</b> Menentukan apakah calon mahasiswa baru (PMB) dapat login ke portal
	 * PMB menggunakan cookie persisten tanpa harus memasukkan kredensial ulang.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.isPmbCookieLoginEnabled()}.</p>
	 *
	 * @return {@code true} bila login PMB via cookie diaktifkan; {@code false} sebaliknya
	 */
	public static boolean isPmbCookieLoginEnabled() {
		return CommonSecurityLoginHelper.isPmbCookieLoginEnabled();
	}

	/**
	 * Memeriksa apakah calon mahasiswa baru (PMB) sedang login di sesi ZK saat ini.
	 *
	 * <p><b>Tujuan.</b> Mengambil status dan data sesi login PMB tanpa perlu akses
	 * request HTTP eksplisit — dipakai dari komponen ZK/Action yang sudah punya sesi aktif.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonSecurityLoginHelper.isLogin()}.</p>
	 *
	 * @return entitas {@code BiodataCalonMahasiswa} bila login aktif; {@code null} bila tidak
	 */
	public static BiodataCalonMahasiswa isLogin() {
		return CommonSecurityLoginHelper.isLogin();
	}

	/**
	 * Memeriksa apakah calon mahasiswa baru (PMB) sedang login berdasarkan request HTTP
	 * yang diberikan (cookie atau session attribute).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.isLogin(request)}.</p>
	 *
	 * @param request request HTTP saat ini; boleh null
	 * @return entitas {@code BiodataCalonMahasiswa} bila login aktif; {@code null} bila tidak
	 */
	public static BiodataCalonMahasiswa isLogin(HttpServletRequest request) {
		return CommonSecurityLoginHelper.isLogin(request);
	}

	/**
	 * Melakukan logout calon mahasiswa PMB dari sesi ZK saat ini.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonSecurityLoginHelper.setLogout()}.</p>
	 */
	public static void setLogout() {
		CommonSecurityLoginHelper.setLogout();
	}

	/**
	 * Melakukan logout calon mahasiswa PMB dan membersihkan cookie serta sesi HTTP.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.setLogout(request, response)}.</p>
	 *
	 * @param request  request HTTP saat ini
	 * @param response response HTTP untuk penghapusan cookie
	 */
	public static void setLogout(HttpServletRequest request, HttpServletResponse response) {
		CommonSecurityLoginHelper.setLogout(request, response);
	}

	/**
	 * Membersihkan semua cookie terkait PMB ({@code COOKIE_PMB_BIODATA} dan
	 * {@code COOKIE_PMB_USERID}) dari response HTTP.
	 *
	 * <p><b>Tujuan.</b> Melakukan logout "bersih" di sisi client dengan menghapus cookie
	 * sesi PMB — dipakai setelah proses logout PMB selesai atau saat cookie sudah tidak valid.</p>
	 *
	 * <p><b>Cara kerja.</b> Memanggil {@link #deleteCookie} untuk setiap nama cookie PMB.
	 * Semua exception ditelan — operasi ini best-effort.</p>
	 *
	 * @param request  request HTTP untuk menentukan flag secure; tidak boleh null
	 * @param response response HTTP untuk menambahkan Set-Cookie header; tidak boleh null
	 */
	public static void clearPmbLoginCookies(HttpServletRequest request, HttpServletResponse response) {
		if (request == null || response == null) {
			return;
		}
		try {
			deleteCookie(response, COOKIE_PMB_BIODATA, request.isSecure());
			deleteCookie(response, COOKIE_PMB_USERID, request.isSecure());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:15412");
		}
	}

	/**
	 * Menghapus satu cookie dari response HTTP dengan mengeset nilainya ke string kosong
	 * dan Max-Age ke 0 (instruksi browser untuk menghapus cookie).
	 *
	 * <p><b>Cara kerja.</b> Buat objek {@code Cookie} baru dengan nama yang sama, nilai kosong,
	 * path "/", MaxAge 0 (expired), dan flag secure sesuai parameter. Exception ditelan.</p>
	 *
	 * @param response response HTTP untuk menambahkan Set-Cookie header penghapusan
	 * @param name     nama cookie yang akan dihapus
	 * @param secure   {@code true} bila cookie harus bertanda Secure (HTTPS only)
	 */
	private static void deleteCookie(HttpServletResponse response, String name, boolean secure) {
		try {
			Cookie cookie = new Cookie(name, "");
			cookie.setPath("/");
			cookie.setMaxAge(0);
			cookie.setSecure(secure);
			response.addCookie(cookie);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:15434");
		}
	}

	/**
	 * Menyimpan data login calon mahasiswa PMB ke sesi ZK saat ini.
	 *
	 * <p><b>Tujuan.</b> Menandai sesi sebagai "PMB sudah login" sehingga halaman portal PMB
	 * dapat menampilkan konten yang dipersonalisasi.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.setLogin(biodataCalonMahasiswa)}.</p>
	 *
	 * @param biodataCalonMahasiswa data calon mahasiswa yang sudah terverifikasi; tidak boleh null
	 */
	public static void setLogin(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		CommonSecurityLoginHelper.setLogin(biodataCalonMahasiswa);
	}

	/**
	 * Menyimpan data login calon mahasiswa PMB ke sesi HTTP dan cookie persisten.
	 *
	 * <p><b>Tujuan.</b> Varian {@link #setLogin(BiodataCalonMahasiswa)} dengan dukungan
	 * cookie "remember me" untuk login persisten antar sesi browser.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.setLogin(request, res, biodataCalonMahasiswa)}.</p>
	 *
	 * @param request                request HTTP saat ini
	 * @param res                    response HTTP untuk menulis cookie
	 * @param biodataCalonMahasiswa  data calon mahasiswa yang sudah terverifikasi
	 */
	public static void setLogin(HttpServletRequest request, HttpServletResponse res,
			BiodataCalonMahasiswa biodataCalonMahasiswa) {
		CommonSecurityLoginHelper.setLogin(request, res, biodataCalonMahasiswa);
	}

	// synchronizedMap (BUKAN ConcurrentHashMap): jalur lama mengizinkan key
	// requestedSessionId null dan value null — ConcurrentHashMap menolak keduanya.
	// Entri dibersihkan terpusat oleh SessionCounter.sessionDestroyed via
	// hapusSessionById() agar form PMB/PPDB yang ditinggalkan tidak bocor permanen.
	private static Map<String, GeneralValueObject> mapSession = java.util.Collections
			.synchronizedMap(new HashMap<String, GeneralValueObject>());

	/**
	 * Menghapus entri {@code mapSession} milik satu session ID. Dipanggil terpusat dari
	 * {@code SessionCounter.sessionDestroyed} (logout, timeout, invalidation) supaya data
	 * sesi yang ditinggalkan tidak tertahan selamanya di static map.
	 *
	 * @param sessionId ID sesi HTTP yang sudah dihancurkan; boleh null (diabaikan)
	 */
	public static void hapusSessionById(String sessionId) {
		if (sessionId != null) {
			mapSession.remove(sessionId);
		}
	}

	/**
	 * Menyimpan entitas {@code GeneralValueObject} ke dalam map sesi internal ({@code mapSession})
	 * yang diindeks berdasarkan ID sesi HTTP request saat ini.
	 *
	 * <p><b>Tujuan.</b> Menyediakan mekanisme "sesi aplikasi" yang sederhana untuk meneruskan
	 * data antar komponen ZK/Servlet tanpa menaruh data langsung di {@code HttpSession}
	 * (yang mungkin tidak tersedia atau memiliki masalah serialisasi).</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Ambil request HTTP dari {@code ExecutionsCtrl.getCurrent()} atau
	 *       {@code RequestContext.get()} sebagai fallback.</li>
	 *   <li>Simpan {@code generalValueObject} ke {@code mapSession} dengan kunci
	 *       {@code request.getRequestedSessionId()}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Threading/Pemeliharaan.</b> {@code mapSession} adalah static HashMap yang tidak
	 * thread-safe — bisa menyebabkan race condition di bawah beban tinggi. Pertimbangkan
	 * {@code ConcurrentHashMap} saat memelihara. Parameter {@code clazz} tidak digunakan
	 * dalam implementasi saat ini (kemungkinan untuk metadata type-safety di masa depan).</p>
	 *
	 * @param clazz               kelas entitas (tidak digunakan dalam implementasi); untuk dokumentasi
	 * @param generalValueObject  entitas yang akan disimpan di sesi; boleh null
	 */
	@SuppressWarnings({ "rawtypes" })
	public static void masukkanSession(Class clazz, GeneralValueObject generalValueObject) {
		HttpServletRequest request = null;
		if (ExecutionsCtrl.getCurrent() != null) {
			request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		}

		if (request == null) {
			request = RequestContext.get();
		}
		mapSession.put(request.getRequestedSessionId(), generalValueObject);
	}

	/**
	 * Mengambil entitas yang sebelumnya disimpan ke map sesi internal oleh
	 * {@link #masukkanSession} untuk sesi HTTP saat ini.
	 *
	 * <p><b>Tujuan.</b> Mengambil kembali data yang disimpan antar-request dalam satu sesi
	 * pengguna tanpa menggunakan HttpSession standar.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Ambil request HTTP dari {@code ExecutionsCtrl} atau {@code RequestContext}.</li>
	 *   <li>Kembalikan nilai dari {@code mapSession.get(sessionId)}; {@code null} bila belum ada.</li>
	 *   <li>Exception ditelan dan mengembalikan {@code null}.</li>
	 * </ol>
	 * </p>
	 *
	 * @param clazz kelas entitas (tidak digunakan dalam implementasi saat ini)
	 * @return entitas yang tersimpan, atau {@code null} bila tidak ada atau terjadi error
	 */
	@SuppressWarnings({ "rawtypes" })
	public static GeneralValueObject ambilSession(Class clazz) {

		try {

			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}

			if (request == null) {
				request = RequestContext.get();
			}
			return mapSession.get(request.getRequestedSessionId());

		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/common/Common.java:15545");
		}
		return null;
	}

	/**
	 * Menghapus dan mengembalikan entitas yang tersimpan di map sesi internal untuk
	 * sesi HTTP saat ini.
	 *
	 * <p><b>Tujuan.</b> Membersihkan data sesi setelah diproses — pola "ambil dan hapus"
	 * (remove) untuk menghindari data yang tertinggal di {@code mapSession} setelah sesi selesai.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Ambil request HTTP dari {@code ExecutionsCtrl} atau {@code RequestContext}.</li>
	 *   <li>Panggil {@code mapSession.remove(sessionId)}.</li>
	 *   <li>Exception ditelan dan mengembalikan {@code null}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan.</b> Sangat direkomendasikan memanggil metode ini setelah selesai
	 * memproses data dari {@link #ambilSession} untuk mencegah memory leak di {@code mapSession}
	 * (static map yang tidak pernah dibersihkan secara otomatis).</p>
	 *
	 * @param clazz kelas entitas (tidak digunakan dalam implementasi)
	 * @return entitas yang dihapus dari sesi, atau {@code null} bila tidak ada atau error
	 */
	@SuppressWarnings({ "rawtypes" })
	public static GeneralValueObject hapusSession(Class clazz) {

		try {

			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}

			if (request == null) {
				request = RequestContext.get();
			}
			return mapSession.remove(request.getRequestedSessionId());

		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/common/Common.java:15588");
		}
		return null;
	}

	/**
	 * Memeriksa apakah calon siswa (PPDB) sedang login di sesi ZK saat ini.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.isLoginCalonSiswa()}.</p>
	 *
	 * @return entitas {@code CalonSiswa} bila login aktif; {@code null} bila tidak
	 */
	public static CalonSiswa isLoginCalonSiswa() {
		return CommonSecurityLoginHelper.isLoginCalonSiswa();
	}

	/**
	 * Memeriksa apakah calon siswa (PPDB) sedang login berdasarkan request HTTP.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.isLoginCalonSiswa(request)}.</p>
	 *
	 * @param request request HTTP saat ini; boleh null
	 * @return entitas {@code CalonSiswa} bila login aktif; {@code null} bila tidak
	 */
	public static CalonSiswa isLoginCalonSiswa(HttpServletRequest request) {
		return CommonSecurityLoginHelper.isLoginCalonSiswa(request);
	}

	/**
	 * Melakukan logout calon siswa PPDB dari sesi ZK saat ini.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.setLogoutCalonSiswa()}.</p>
	 */
	public static void setLogoutCalonSiswa() {
		CommonSecurityLoginHelper.setLogoutCalonSiswa();
	}

	/**
	 * Melakukan logout calon siswa PPDB dan membersihkan cookie serta sesi HTTP.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.setLogoutCalonSiswa(request, response)}.</p>
	 *
	 * @param request  request HTTP
	 * @param response response HTTP untuk penghapusan cookie
	 */
	public static void setLogoutCalonSiswa(HttpServletRequest request, HttpServletResponse response) {
		CommonSecurityLoginHelper.setLogoutCalonSiswa(request, response);
	}

	/**
	 * Memeriksa apakah penyedia aset (vendor) sedang login di sesi saat ini.
	 *
	 * <p><b>Tujuan.</b> Mengidentifikasi akses dari portal vendor/penyedia aset eksternal
	 * yang login terpisah dari akun internal.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.isLoginPenyediaAsset()}.</p>
	 *
	 * @return entitas {@code PenyediaAsset} bila login aktif; {@code null} bila tidak
	 */
	public static PenyediaAsset isLoginPenyediaAsset() {
		return CommonSecurityLoginHelper.isLoginPenyediaAsset();
	}

	/**
	 * Melakukan logout penyedia aset dari sesi saat ini.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.setLogoutPenyediaAsset()}.</p>
	 */
	public static void setLogoutPenyediaAsset() {
		CommonSecurityLoginHelper.setLogoutPenyediaAsset();
	}

	/**
	 * Memeriksa apakah calon pegawai sedang login di sesi saat ini.
	 *
	 * <p><b>Tujuan.</b> Mengidentifikasi akses dari portal rekrutmen pegawai baru
	 * yang login terpisah dari akun internal.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.isLoginCalonPegawai()}.</p>
	 *
	 * @return entitas {@code CalonPegawai} bila login aktif; {@code null} bila tidak
	 */
	public static CalonPegawai isLoginCalonPegawai() {
		return CommonSecurityLoginHelper.isLoginCalonPegawai();
	}

	/**
	 * Melakukan logout calon pegawai dari sesi saat ini.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.setLogoutCalonPegawai()}.</p>
	 */
	public static void setLogoutCalonPegawai() {
		CommonSecurityLoginHelper.setLogoutCalonPegawai();
	}

	/**
	 * Menyimpan data login calon siswa PPDB ke sesi ZK saat ini.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.setLogin(calonSiswa)}.</p>
	 *
	 * @param calonSiswa data calon siswa yang terverifikasi; tidak boleh null
	 */
	public static void setLogin(CalonSiswa calonSiswa) {
		CommonSecurityLoginHelper.setLogin(calonSiswa);
	}

	/**
	 * Menyimpan data login calon siswa PPDB ke sesi HTTP yang diberikan.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.setLogin(request, calonSiswa)}.</p>
	 *
	 * @param request    request HTTP saat ini
	 * @param calonSiswa data calon siswa terverifikasi
	 */
	public static void setLogin(HttpServletRequest request, CalonSiswa calonSiswa) {
		CommonSecurityLoginHelper.setLogin(request, calonSiswa);
	}

	/**
	 * Menyimpan data login calon siswa PPDB ke sesi HTTP dan membuat cookie persisten.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.setLogin(request, res, calonSiswa)}.</p>
	 *
	 * @param request    request HTTP saat ini
	 * @param res        response HTTP untuk menulis cookie
	 * @param calonSiswa data calon siswa terverifikasi
	 */
	public static void setLogin(HttpServletRequest request, HttpServletResponse res, CalonSiswa calonSiswa) {
		CommonSecurityLoginHelper.setLogin(request, res, calonSiswa);
	}

	/**
	 * Memeriksa apakah pengguna saat ini mengakses sistem dari perangkat mobile (berdasarkan
	 * flag sesi atau User-Agent).
	 *
	 * <p><b>Tujuan.</b> Memungkinkan kode UI menyesuaikan tampilan atau perilaku berdasarkan
	 * tipe perangkat — misal ukuran popup, layout panel, atau navigasi menu.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.isMobile()}.
	 * Flag {@code is_mobile} bisa di-set via parameter URL atau sesi.</p>
	 *
	 * @return {@code true} bila pengguna di perangkat mobile; {@code false} bila desktop
	 */
	public static boolean isMobile() {
		return CommonCurrentSessionHelper.isMobile();
	}

	/**
	 * Memeriksa apakah request HTTP berasal dari perangkat mobile berdasarkan sesi atau
	 * User-Agent header.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonCurrentSessionHelper.isMobile(request)}.</p>
	 *
	 * @param request request HTTP yang diperiksa; boleh null
	 * @return {@code true} bila perangkat mobile; {@code false} bila desktop atau null
	 */
	public static boolean isMobile(HttpServletRequest request) {
		return CommonCurrentSessionHelper.isMobile(request);
	}

	/**
	 * Memeriksa apakah pengguna benar-benar mengakses dari perangkat mobile secara asli
	 * (berdasarkan User-Agent, bukan flag sesi yang mungkin sudah di-override ke desktop).
	 *
	 * <p><b>Tujuan.</b> Membedakan antara "pengguna yang mengakses dari HP namun memilih
	 * mode desktop" ({@code isMobile} mungkin false) dengan "pengguna benar-benar di HP"
	 * ({@code isAsliMobile} tetap true).</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonCurrentSessionHelper.isAsliMobile()}.</p>
	 *
	 * @return {@code true} bila User-Agent sesungguhnya adalah mobile; {@code false} bila desktop
	 */
	public static boolean isAsliMobile() {
		return CommonCurrentSessionHelper.isAsliMobile();
	}

	/**
	 * Memeriksa apakah request HTTP benar-benar dari perangkat mobile secara asli berdasarkan
	 * User-Agent request tersebut.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonCurrentSessionHelper.isAsliMobile(request)}.</p>
	 *
	 * @param request request HTTP yang diperiksa
	 * @return {@code true} bila User-Agent adalah mobile; {@code false} sebaliknya
	 */
	public static boolean isAsliMobile(HttpServletRequest request) {
		return CommonCurrentSessionHelper.isAsliMobile(request);
	}

	/**
	 * Mengambil daftar {@code DetailJenisPenilaian} yang berlaku untuk jadwal pelajaran tertentu.
	 *
	 * <p><b>Tujuan.</b> Menentukan komponen penilaian (ulangan harian, UTS, UAS, tugas, dsb.)
	 * yang harus diisi guru untuk jadwal pelajaran tersebut.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonUiFactoryHelper.getDetailJenisPenilaians}.</p>
	 *
	 * @param jadwalPelajaran jadwal pelajaran yang detail penilaiannya dimuat; tidak boleh null
	 * @return daftar detail jenis penilaian yang berlaku; list kosong bila tidak ada
	 */
	public static List<DetailJenisPenilaian> getDetailJenisPenilaians(JadwalPelajaran jadwalPelajaran) {
		return CommonUiFactoryHelper.getDetailJenisPenilaians(jadwalPelajaran);
	}

	/**
	 * Menghasilkan komponen HTML yang menampilkan hari, jam, dan ruangan dari jadwal pelajaran.
	 *
	 * <p><b>Tujuan.</b> Menyediakan widget ringkasan jadwal yang dapat disisipkan ke dalam
	 * baris grid atau komponen ZK lainnya.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code JadwalDisplayHelper.displayHariJamRuanganJadwalPelajaran(jadwal)}.</p>
	 *
	 * @param jadwal jadwal pelajaran yang informasinya ditampilkan; tidak boleh null
	 * @return {@code MyHtml} berisi teks/HTML hari-jam-ruangan
	 */
	public static ais.ui.util.MyHtml displayHariJamRuanganJadwalPelajaran(JadwalPelajaran jadwal) {
		return JadwalDisplayHelper.displayHariJamRuanganJadwalPelajaran(jadwal);
	}

	/**
	 * Menambahkan sel-sel hari, jam, dan ruangan jadwal pelajaran ke dalam baris grid ZK.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code JadwalDisplayHelper.displayHariJamRuanganJadwalPelajaran(row, jadwalPelajaran)}.</p>
	 *
	 * @param row             baris ZK yang akan diisi; tidak boleh null
	 * @param jadwalPelajaran jadwal pelajaran sumber data
	 */
	public static void displayHariJamRuanganJadwalPelajaran(Row row, JadwalPelajaran jadwalPelajaran) {
		JadwalDisplayHelper.displayHariJamRuanganJadwalPelajaran(row, jadwalPelajaran);
	}

	/**
	 * Menambahkan informasi hari, jam, dan ruangan jadwal pelajaran ke komponen ZK umum
	 * (bisa Row atau komponen lain yang mendukung appendChild).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code JadwalDisplayHelper.displayHariJamRuanganJadwalPelajaranUmum}.</p>
	 *
	 * @param row             komponen ZK induk (Row, Div, dsb.); tidak boleh null
	 * @param jadwalPelajaran jadwal pelajaran sumber data
	 */
	public static void displayHariJamRuanganJadwalPelajaranUmum(Component row, JadwalPelajaran jadwalPelajaran) {
		JadwalDisplayHelper.displayHariJamRuanganJadwalPelajaranUmum(row, jadwalPelajaran);
	}

	/**
	 * Menambahkan widget guru pengampu jadwal pelajaran ke komponen ZK, dengan opsi tampil
	 * asisten.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code JadwalDisplayHelper.displayGuruJadwalPelajaran(row, jadwal, tampilAsisten)}.</p>
	 *
	 * @param row           komponen induk untuk widget guru
	 * @param jadwal        jadwal pelajaran
	 * @param tampilAsisten {@code true} untuk menampilkan asisten guru
	 * @return Hbox berisi widget guru (foto, nama, dsb.)
	 */
	public static Hbox displayGuruJadwalPelajaran(Component row, JadwalPelajaran jadwal, Boolean tampilAsisten) {
		return JadwalDisplayHelper.displayGuruJadwalPelajaran(row, jadwal, tampilAsisten);
	}

	/**
	 * Menambahkan widget guru pengampu dengan opsi menampilkan nama dan asisten.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code JadwalDisplayHelper.displayGuruJadwalPelajaran(row, jadwal, displayName, tampilAsisten)}.</p>
	 *
	 * @param row           komponen induk
	 * @param jadwal        jadwal pelajaran
	 * @param displayName   {@code true} untuk menampilkan nama guru
	 * @param tampilAsisten {@code true} untuk menampilkan asisten
	 * @return Hbox berisi widget guru
	 */
	public static Hbox displayGuruJadwalPelajaran(Component row, JadwalPelajaran jadwal, Boolean displayName,
			Boolean tampilAsisten) {
		return JadwalDisplayHelper.displayGuruJadwalPelajaran(row, jadwal, displayName, tampilAsisten);
	}

	/**
	 * Menambahkan widget guru pengampu jadwal pelajaran ke komponen ZK umum (bukan hanya Row).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code JadwalDisplayHelper.displayGuruJadwalPelajaranUmum(row, jadwal, tampilAsisten)}.</p>
	 *
	 * @param row           komponen ZK umum induk
	 * @param jadwal        jadwal pelajaran
	 * @param tampilAsisten {@code true} untuk menampilkan asisten
	 * @return Hbox berisi widget guru
	 */
	public static Hbox displayGuruJadwalPelajaranUmum(Component row, JadwalPelajaran jadwal, Boolean tampilAsisten) {
		return JadwalDisplayHelper.displayGuruJadwalPelajaranUmum(row, jadwal, tampilAsisten);
	}

	/**
	 * Menambahkan widget guru dengan opsi nama dan asisten ke komponen ZK umum.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code JadwalDisplayHelper.displayGuruJadwalPelajaranUmum(row, jadwal, displayName, tampilAsisten)}.</p>
	 *
	 * @param row           komponen induk
	 * @param jadwal        jadwal pelajaran
	 * @param displayName   tampilkan nama guru
	 * @param tampilAsisten tampilkan asisten
	 * @return Hbox berisi widget guru
	 */
	public static Hbox displayGuruJadwalPelajaranUmum(Component row, JadwalPelajaran jadwal, Boolean displayName,
			Boolean tampilAsisten) {
		return JadwalDisplayHelper.displayGuruJadwalPelajaranUmum(row, jadwal, displayName, tampilAsisten);
	}

	/**
	 * Menambahkan widget guru dengan guru tambahan (team teaching) ke komponen ZK umum.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code JadwalDisplayHelper.displayGuruJadwalPelajaranUmum(row, jadwal, displayName, tampilAsisten, guruTambahan)}.</p>
	 *
	 * @param row           komponen induk
	 * @param jadwal        jadwal pelajaran
	 * @param displayName   tampilkan nama guru
	 * @param tampilAsisten tampilkan asisten
	 * @param guruTambahan  guru co-pengampu tambahan; boleh null
	 * @return Hbox berisi widget guru
	 */
	public static Hbox displayGuruJadwalPelajaranUmum(Component row, JadwalPelajaran jadwal, Boolean displayName,
			Boolean tampilAsisten, Guru guruTambahan) {
		return JadwalDisplayHelper.displayGuruJadwalPelajaranUmum(row, jadwal, displayName, tampilAsisten,
				guruTambahan);
	}

	/**
	 * Menambahkan widget guru dengan kontrol jumlah guru yang ditampilkan per baris ({@code tampilPerRow}).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code JadwalDisplayHelper.displayGuruJadwalPelajaranUmum(row, jadwal, displayName, tampilAsisten, guruTambahan, tampilPerRow)}.</p>
	 *
	 * @param row           komponen induk
	 * @param jadwal        jadwal pelajaran
	 * @param displayName   tampilkan nama guru
	 * @param tampilAsisten tampilkan asisten
	 * @param guruTambahan  guru tambahan; boleh null
	 * @param tampilPerRow  jumlah guru per baris widget
	 * @return Hbox berisi widget guru
	 */
	public static Hbox displayGuruJadwalPelajaranUmum(Component row, JadwalPelajaran jadwal, Boolean displayName,
			Boolean tampilAsisten, Guru guruTambahan, int tampilPerRow) {
		return JadwalDisplayHelper.displayGuruJadwalPelajaranUmum(row, jadwal, displayName, tampilAsisten, guruTambahan,
				tampilPerRow);
	}

	/**
	 * Menambahkan widget pegawai pengajar komponen data produk kursus ke komponen ZK.
	 *
	 * <p><b>Tujuan.</b> Menampilkan informasi instruktur kursus (bukan guru sekolah formal)
	 * untuk modul kursus/pelatihan perpustakaan.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code JadwalDisplayHelper.displayPegawaiKomponenDataProdukKursus}.</p>
	 *
	 * @param row           komponen induk
	 * @param data          komponen data produk kursus
	 * @param tampilAsisten tampilkan asisten
	 * @return Hbox berisi widget pegawai pengajar
	 */
	public static Hbox displayPegawaiKomponenDataProdukKursus(Component row, KomponenDataProdukKursus data,
			Boolean tampilAsisten) {
		return JadwalDisplayHelper.displayPegawaiKomponenDataProdukKursus(row, data, tampilAsisten);
	}

	/**
	 * Menambahkan widget pegawai pengajar kursus dengan opsi nama dan asisten.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code JadwalDisplayHelper.displayPegawaiKomponenDataProdukKursus(row, data, displayName, tampilAsisten)}.</p>
	 *
	 * @param row           komponen induk
	 * @param data          komponen kursus
	 * @param displayName   tampilkan nama pegawai
	 * @param tampilAsisten tampilkan asisten
	 * @return Hbox berisi widget pegawai
	 */
	public static Hbox displayPegawaiKomponenDataProdukKursus(Component row, KomponenDataProdukKursus data,
			Boolean displayName, Boolean tampilAsisten) {
		return JadwalDisplayHelper.displayPegawaiKomponenDataProdukKursus(row, data, displayName, tampilAsisten);
	}

	/**
	 * Menambahkan widget pegawai pengajar kursus ke komponen ZK umum (bukan hanya Row).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code JadwalDisplayHelper.displayPegawaiKomponenDataProdukKursusUmum(row, data, tampilAsisten)}.</p>
	 *
	 * @param row           komponen ZK umum induk
	 * @param data          komponen kursus
	 * @param tampilAsisten tampilkan asisten
	 * @return Hbox berisi widget pegawai
	 */
	public static Hbox displayPegawaiKomponenDataProdukKursusUmum(Component row, KomponenDataProdukKursus data,
			Boolean tampilAsisten) {
		return JadwalDisplayHelper.displayPegawaiKomponenDataProdukKursusUmum(row, data, tampilAsisten);
	}

	/**
	 * Menambahkan widget pegawai pengajar kursus (umum) dengan opsi nama dan asisten.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code JadwalDisplayHelper.displayPegawaiKomponenDataProdukKursusUmum(row, data, displayName, tampilAsisten)}.</p>
	 *
	 * @param row           komponen induk
	 * @param data          komponen kursus
	 * @param displayName   tampilkan nama
	 * @param tampilAsisten tampilkan asisten
	 * @return Hbox berisi widget pegawai
	 */
	public static Hbox displayPegawaiKomponenDataProdukKursusUmum(Component row, KomponenDataProdukKursus data,
			Boolean displayName, Boolean tampilAsisten) {
		return JadwalDisplayHelper.displayPegawaiKomponenDataProdukKursusUmum(row, data, displayName, tampilAsisten);
	}

	/**
	 * Menambahkan widget pegawai pengajar kursus (umum) dengan pegawai tambahan.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code JadwalDisplayHelper.displayPegawaiKomponenDataProdukKursusUmum(row, data, displayName, tampilAsisten, pegawaiTambahan)}.</p>
	 *
	 * @param row              komponen induk
	 * @param data             komponen kursus
	 * @param displayName      tampilkan nama
	 * @param tampilAsisten    tampilkan asisten
	 * @param pegawaiTambahan  pegawai co-instruktur tambahan; boleh null
	 * @return Hbox berisi widget pegawai
	 */
	public static Hbox displayPegawaiKomponenDataProdukKursusUmum(Component row, KomponenDataProdukKursus data,
			Boolean displayName, Boolean tampilAsisten, Pegawai pegawaiTambahan) {
		return JadwalDisplayHelper.displayPegawaiKomponenDataProdukKursusUmum(row, data, displayName, tampilAsisten,
				pegawaiTambahan);
	}

	/**
	 * Menambahkan widget guru yang bertanggung jawab pada absen piket ke komponen ZK.
	 *
	 * <p><b>Tujuan.</b> Menampilkan informasi guru piket harian (penjaga absensi) dalam
	 * tampilan rekap atau jadwal piket sekolah.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code JadwalDisplayHelper.displayGuruAbsenPiket(row, absen, tampilAsisten)}.</p>
	 *
	 * @param row           komponen induk
	 * @param absen         data absen piket
	 * @param tampilAsisten tampilkan asisten piket
	 * @return Hbox berisi widget guru piket
	 */
	public static Hbox displayGuruAbsenPiket(Component row, AbsenPiket absen, Boolean tampilAsisten) {
		return JadwalDisplayHelper.displayGuruAbsenPiket(row, absen, tampilAsisten);
	}

	/**
	 * Menambahkan widget guru piket dengan opsi nama dan asisten.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code JadwalDisplayHelper.displayGuruAbsenPiket(row, absen, displayName, tampilAsisten)}.</p>
	 *
	 * @param row           komponen induk
	 * @param absen         data absen piket
	 * @param displayName   tampilkan nama guru
	 * @param tampilAsisten tampilkan asisten
	 * @return Hbox berisi widget guru piket
	 */
	public static Hbox displayGuruAbsenPiket(Component row, AbsenPiket absen, Boolean displayName,
			Boolean tampilAsisten) {
		return JadwalDisplayHelper.displayGuruAbsenPiket(row, absen, displayName, tampilAsisten);
	}

	/**
	 * Menambahkan widget guru piket dengan guru tambahan (bila ada dua guru piket).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code JadwalDisplayHelper.displayGuruAbsenPiket(row, absen, displayName, tampilAsisten, guruTambahan)}.</p>
	 *
	 * @param row           komponen induk
	 * @param absen         data absen piket
	 * @param displayName   tampilkan nama guru
	 * @param tampilAsisten tampilkan asisten
	 * @param guruTambahan  guru piket tambahan; boleh null
	 * @return Hbox berisi widget guru piket
	 */
	public static Hbox displayGuruAbsenPiket(Component row, AbsenPiket absen, Boolean displayName,
			Boolean tampilAsisten, Guru guruTambahan) {
		return JadwalDisplayHelper.displayGuruAbsenPiket(row, absen, displayName, tampilAsisten, guruTambahan);
	}

	/**
	 * Mengkode string {@code s} menggunakan skema URL-encoding (percent-encoding) berdasarkan
	 * charset UTF-8.
	 *
	 * <p><b>Tujuan.</b> Menyiapkan nilai string yang aman untuk disisipkan sebagai parameter URL
	 * (query string atau path), sehingga karakter khusus seperti spasi, tanda seru, tanda persen,
	 * koma, dsb. diubah ke bentuk {@code %XX} sesuai standar RFC 3986.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Memanggil {@code URLEncoder.encode(s, "UTF-8")} yang mengembalikan string ter-encode.</li>
	 *   <li>Bila encoding gagal (misalnya charset tidak dikenal — praktis tidak mungkin untuk UTF-8),
	 *       membungkus {@code UnsupportedEncodingException} dalam {@code UnsupportedOperationException}
	 *       dan melemparnya; dengan demikian pemanggil tidak perlu mendeklarasikan checked exception.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error.</b> {@code UnsupportedEncodingException} diubah menjadi
	 * {@code UnsupportedOperationException} sehingga pemanggil tidak diwajibkan tangani
	 * checked exception — namun pada praktiknya UTF-8 selalu tersedia di semua JVM standar.</p>
	 *
	 * @param s string yang akan di-encode; boleh berisi karakter Unicode apa pun
	 * @return string hasil URL-encoding dalam charset UTF-8
	 * @throws UnsupportedOperationException bila JVM tidak mendukung UTF-8 (tidak akan terjadi di JVM standar)
	 */
	public static String urlEncodeUTF8(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (Exception e) {
			throw new UnsupportedOperationException(e);
		}
	}

	/**
	 * Mengonversi peta parameter {@code Map<String, Object>} menjadi query string URL
	 * (contoh: {@code key1=value1&key2=value2}).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonHelperClass.convertToQueryString(map)}
	 * yang mengiterasi entri peta, men-encode nilai masing-masing, lalu menggabungkannya
	 * dengan tanda {@code &}.</p>
	 *
	 * @param map peta parameter yang akan dikonversi; tidak boleh null
	 * @return query string yang siap ditambahkan ke URL
	 */
	public static String convertToQueryString(Map<String, Object> map) {
		return CommonHelperClass.convertToQueryString(map);

	}

	/**
	 * Memeriksa apakah pengecualian (throwable) disebabkan oleh koneksi klien yang terputus
	 * (client abort / broken pipe).
	 *
	 * <p><b>Tujuan.</b> Menghindari pencatatan error bising saat pengguna menutup browser atau
	 * koneksi terputus sebelum respons selesai dikirim.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code ErrorAuditUtil.isClientAbort(throwable)}
	 * yang memeriksa pesan exception maupun rantai penyebab (cause chain) untuk kata kunci
	 * "Broken pipe", "Connection reset", "ClientAbortException", dsb.</p>
	 *
	 * @param throwable exception yang diperiksa; tidak boleh null
	 * @return {@code true} bila error disebabkan client abort
	 */
	public static boolean isClientAbort(Throwable throwable) {
		return ErrorAuditUtil.isClientAbort(throwable);
	}

	/**
	 * Memeriksa apakah pengecualian disebabkan client abort yang boleh diabaikan (tidak perlu
	 * di-log atau ditampilkan).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code ErrorAuditUtil.isClientAbortIgnored(throwable)}. Berbeda dari
	 * {@link #isClientAbort(Throwable)} yang hanya mendeteksi, method ini juga mengindikasikan
	 * bahwa error tidak perlu ditindaklanjuti.</p>
	 *
	 * @param throwable exception yang diperiksa
	 * @return {@code true} bila error client abort yang bisa diabaikan
	 */
	public static boolean isClientAbortIgnored(Throwable throwable) {
		return ErrorAuditUtil.isClientAbortIgnored(throwable);
	}

	/**
	 * Menampilkan pesan error ke pengguna bila pengguna yang sedang login adalah administrator,
	 * atau mencatat error secara diam-diam bila pengguna biasa.
	 *
	 * <p><b>Tujuan.</b> Memberikan visibilitas error teknis kepada admin tanpa membingungkan
	 * pengguna akhir — pengguna biasa hanya melihat pesan generik atau tidak melihat apa pun,
	 * sedangkan admin melihat pesan exception lengkap untuk debugging.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.tampilErrorJikaAdmin(ex)}.</p>
	 *
	 * @param ex exception yang ditangkap; tidak boleh null
	 * @return pesan error yang ditampilkan (bila admin) atau string kosong (bila bukan admin)
	 */
	public static String tampilErrorJikaAdmin(Exception ex) {
		return CommonSecurityLoginHelper.tampilErrorJikaAdmin(ex);
	}

	/**
	 * Menampilkan kotak pesan informasi sederhana ke pengguna.
	 * Pembungkus MyMessageboxConfig.show yang menelan InterruptedException
	 * supaya pemanggil tidak perlu try/catch (dipakai mis. modul sapto).
	 */
	public static void showInfo(String pesan) {
		try {
			MyMessageboxConfig.show(pesan);
		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menampilkan pesan error ke pengguna bila admin, dengan informasi tambahan dan opsi
	 * menampilkan sebagai error unduhan (download error).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.tampilErrorJikaAdmin(ex, info, download)}.
	 * Parameter {@code info} menambahkan konteks (misal nama modul), dan {@code download}
	 * mengalihkan ke tampilan error ramah unduhan bila {@code true}.</p>
	 *
	 * @param ex       exception yang ditangkap
	 * @param info     informasi konteks tambahan yang ditampilkan bersama pesan error
	 * @param download {@code true} untuk format tampilan error unduhan
	 * @return pesan error (bila admin) atau string kosong
	 */
	public static String tampilErrorJikaAdmin(Exception ex, String info, boolean download) {
		return CommonSecurityLoginHelper.tampilErrorJikaAdmin(ex, info, download);
	}

	/**
	 * Membuat {@code EventListener} yang akan menampilkan error unduhan ketika dipicu,
	 * biasanya dipasang pada tombol atau timer setelah proses unduh gagal.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonSecurityLoginHelper.downloadError(ex)} yang membungkus exception dalam
	 * listener sehingga pesan error bisa ditampilkan secara asinkron di thread ZK.</p>
	 *
	 * @param ex exception yang akan dibungkus dalam listener
	 * @return {@code EventListener} yang menampilkan error saat dipicu
	 */
	public static EventListener downloadError(Exception ex) {
		return CommonSecurityLoginHelper.downloadError(ex);
	}

	/**
	 * Menghitung total tagihan mahasiswa yang menjadi syarat KRS untuk semester tertentu.
	 *
	 * <p><b>Tujuan.</b> Digunakan saat validasi KRS untuk memastikan mahasiswa tidak memiliki
	 * tunggakan yang melebihi batas toleransi sebelum diizinkan mengisi KRS.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicSyncHelper.hitungTagihanMahasiswaSebagaiSyaratKrs}.</p>
	 *
	 * @param session   sesi Hibernate yang aktif
	 * @param mahasiswa mahasiswa yang tagihannya dihitung
	 * @param semester  nomor semester yang diperiksa
	 * @return total tagihan (dalam rupiah) sebagai syarat KRS; 0.0 bila tidak ada tunggakan
	 */
	public static Double hitungTagihanMahasiswaSebagaiSyaratKrs(Session session, Mahasiswa mahasiswa,
			Integer semester) {
		return CommonAcademicSyncHelper.hitungTagihanMahasiswaSebagaiSyaratKrs(session, mahasiswa, semester);
	}

	/**
	 * Memeriksa apakah pembayaran mahasiswa sebelum pengisian KRS sudah memenuhi syarat minimal.
	 *
	 * <p><b>Tujuan.</b> Gerbang validasi KRS — bila metode ini mengembalikan {@code false},
	 * mahasiswa tidak boleh mengisi KRS karena belum memenuhi syarat pembayaran.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicSyncHelper.checkPembayaranSebelumKRSSudahMemenuhi}.</p>
	 *
	 * @param mahasiswa   mahasiswa yang diperiksa
	 * @param semester    nomor semester aktif
	 * @param tahap       tahap pembayaran yang diperiksa
	 * @param persetujuan {@code true} bila harus memeriksa persetujuan tambahan
	 * @return {@code true} bila syarat pembayaran sudah terpenuhi
	 */
	public static Boolean checkPembayaranSebelumKRSSudahMemenuhi(Mahasiswa mahasiswa, Integer semester, Integer tahap,
			boolean persetujuan) {
		return CommonAcademicSyncHelper.checkPembayaranSebelumKRSSudahMemenuhi(mahasiswa, semester, tahap, persetujuan);
	}

	/**
	 * Menampilkan indikator loading (load bar) yang dipicu oleh {@code EventListener}.
	 *
	 * <p><b>Tujuan.</b> Memberikan umpan balik visual kepada pengguna saat proses latar belakang
	 * sedang berjalan, mencegah aksi ganda (double-click) dan memberikan estimasi kemajuan.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonFileMediaHelper.displayLoadBar(eventListener)}.</p>
	 *
	 * @param eventListener listener yang dipicu saat proses selesai; tidak boleh null
	 * @return Label ZK yang digunakan sebagai indikator loading
	 */
	public static Label displayLoadBar(EventListener eventListener) {
		return CommonFileMediaHelper.displayLoadBar(eventListener);
	}

	/**
	 * Menampilkan indikator loading di dalam komponen induk yang diberikan.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonFileMediaHelper.displayLoadBar(parent)}. Indikator dipasang sebagai
	 * anak komponen {@code parent}.</p>
	 *
	 * @param parent komponen ZK yang akan menampung indikator loading; tidak boleh null
	 * @return Label ZK indikator loading
	 */
	public static Label displayLoadBar(Component parent) {
		return CommonFileMediaHelper.displayLoadBar(parent);
	}

	/**
	 * Menampilkan indikator loading di komponen induk dengan {@code EventListener} tambahan.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonFileMediaHelper.displayLoadBar(parent, eventListener)}.</p>
	 *
	 * @param parent        komponen induk tempat indikator dipasang
	 * @param eventListener listener yang dipicu setelah proses selesai
	 * @return Label ZK indikator loading
	 */
	public static Label displayLoadBar(Component parent, EventListener eventListener) {
		return CommonFileMediaHelper.displayLoadBar(parent, eventListener);
	}

	/**
	 * Menampilkan indikator loading yang tidak berhenti secara otomatis (tidak ada batas
	 * iterasi/timeout) — berguna untuk proses yang durasinya tidak dapat diprediksi.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code LoadBarUtils.displayLoadBarjanganBerhenti(eventListener)}.</p>
	 *
	 * @param eventListener listener yang mengelola penghentian load bar secara manual
	 * @return Label ZK indikator loading tanpa batas
	 */
	public static Label displayLoadBarjanganBerhenti(EventListener eventListener) {
		return LoadBarUtils.displayLoadBarjanganBerhenti(eventListener);
	}

	/**
	 * Menampilkan indikator loading terkait file tertentu di komponen induk.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonFileMediaHelper.displayLoadBar(parent, fileName)}. Load bar dapat
	 * menampilkan nama berkas yang sedang diproses.</p>
	 *
	 * @param parent   komponen induk
	 * @param fileName berkas yang sedang diproses; digunakan untuk label progres
	 * @return Label ZK indikator loading
	 */
	public static Label displayLoadBar(Component parent, File fileName) {
		return CommonFileMediaHelper.displayLoadBar(parent, fileName);
	}

	/**
	 * Menampilkan indikator loading dengan dukungan progres ukuran data (jumlah baris).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonFileMediaHelper.displayLoadBar(parent, file, center, sizedata)} di mana
	 * {@code sizedata} merupakan Intbox yang diperbarui secara berkala untuk melacak kemajuan.</p>
	 *
	 * @param parent   komponen induk
	 * @param file     berkas yang diproses
	 * @param center   komponen tengah tempat hasil akan ditampilkan
	 * @param sizedata Intbox penanda jumlah data yang telah diproses
	 * @return Label ZK indikator loading
	 */
	public static Label displayLoadBar(Component parent, File file, Component center, Intbox sizedata) {
		return CommonFileMediaHelper.displayLoadBar(parent, file, center, sizedata);
	}

	/**
	 * Menampilkan indikator loading dengan dukungan progres ukuran data dan jumlah kolom maksimal.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonFileMediaHelper.displayLoadBar(parent, file, center, sizedata, maxCol)}.
	 * Parameter {@code maxCol} digunakan untuk grid dengan banyak kolom agar load bar dapat
	 * menampilkan progress yang proporsional.</p>
	 *
	 * @param parent   komponen induk
	 * @param file     berkas yang diproses
	 * @param center   komponen tengah
	 * @param sizedata Intbox penanda jumlah data
	 * @param maxCol   Intbox batas kolom maksimal
	 * @return Label ZK indikator loading
	 */
	public static Label displayLoadBar(Component parent, File file, Component center, Intbox sizedata, Intbox maxCol) {
		return CommonFileMediaHelper.displayLoadBar(parent, file, center, sizedata, maxCol);
	}

	/**
	 * Mengonversi {@code JSONArray} menjadi {@code List} tanpa memetakan ke kelas entitas tertentu.
	 *
	 * <p><b>Cara kerja.</b> Memanggil {@link #convertToList(JSONArray, Class)} dengan {@code clazz = null},
	 * sehingga setiap elemen tetap berupa tipe JSON mentah (JSONObject, String, Integer, dsb.).</p>
	 *
	 * @param array JSONArray yang akan dikonversi; tidak boleh null
	 * @return List berisi elemen JSON mentah
	 */
	@SuppressWarnings({ "rawtypes" })
	public static List convertToList(JSONArray array) {
		return convertToList(array, null);
	}

	/**
	 * Mengonversi {@code JSONArray} menjadi {@code List}, dengan opsi pemetaan ke kelas entitas
	 * {@code clazz} menggunakan refleksi setter.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonJSONUtil.convertToList(array, clazz)}.
	 * Bila {@code clazz} null, elemen dikembalikan apa adanya; bila tidak null, tiap elemen
	 * JSONObject diinstansiasi sebagai objek {@code clazz} dengan nilai dari kunci JSON.</p>
	 *
	 * @param array JSONArray sumber
	 * @param clazz kelas tujuan pemetaan; boleh null untuk tipe mentah
	 * @return List hasil konversi
	 */
	@SuppressWarnings({ "rawtypes" })
	public static List convertToList(JSONArray array, Class clazz) {
		return CommonJSONUtil.convertToList(array, clazz);
	}

	/**
	 * Mengonversi {@code JSONArray} menjadi {@code Set} dan memetakan tiap elemen ke kelas
	 * entitas {@code clazz}.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonJSONUtil.convertToSet(array, clazz)}.
	 * Berguna saat koleksi unik diperlukan (tidak boleh duplikat), misalnya untuk daftar
	 * pilihan yang dipilih pengguna.</p>
	 *
	 * @param array JSONArray sumber
	 * @param clazz kelas tujuan pemetaan
	 * @return Set hasil konversi
	 */
	@SuppressWarnings({ "rawtypes" })
	public static Set convertToSet(JSONArray array, Class clazz) {
		return CommonJSONUtil.convertToSet(array, clazz);
	}

	/**
	 * Mengonversi {@code JSONObject} menjadi {@code GeneralValueObject} tanpa memetakan ke
	 * kelas entitas tertentu.
	 *
	 * <p><b>Cara kerja.</b> Memanggil {@link #convertToObject(JSONObject, Class)} dengan
	 * {@code clazz = null}.</p>
	 *
	 * @param json JSONObject yang akan dikonversi
	 * @return GeneralValueObject hasil konversi
	 */
	public static GeneralValueObject convertToObject(JSONObject json) {
		return convertToObject(json, null);
	}

	/**
	 * Mengonversi {@code JSONObject} menjadi {@code GeneralValueObject} dengan pemetaan ke
	 * kelas entitas {@code clazz} (percobaan pertama, {@code coba = 0}).
	 *
	 * <p><b>Cara kerja.</b> Memanggil {@link #convertToObject(JSONObject, Class, int)} dengan
	 * {@code coba = 0}.</p>
	 *
	 * @param json  JSONObject sumber
	 * @param clazz kelas entitas tujuan; boleh null
	 * @return GeneralValueObject hasil konversi
	 */
	@SuppressWarnings("rawtypes")
	public static GeneralValueObject convertToObject(JSONObject json, Class clazz) {
		return convertToObject(json, clazz, 0);
	}

	/**
	 * Mengonversi {@code JSONObject} menjadi {@code GeneralValueObject} dengan dukungan
	 * retry pada kelas entitas berbeda ({@code coba} menentukan varian percobaan).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonJSONUtil.convertToObject(json, clazz, coba)} yang menggunakan refleksi
	 * untuk mengisi field GeneralValueObject dari kunci JSONObject. Parameter {@code coba}
	 * memungkinkan strategi fallback bila pemetaan pertama gagal.</p>
	 *
	 * @param json  JSONObject sumber
	 * @param clazz kelas entitas tujuan; boleh null
	 * @param coba  indeks percobaan (0 = pertama kali, &gt;0 = strategi fallback)
	 * @return GeneralValueObject hasil konversi
	 */
	@SuppressWarnings("rawtypes")
	public static GeneralValueObject convertToObject(JSONObject json, Class clazz, int coba) {
		return CommonJSONUtil.convertToObject(json, clazz, coba);
	}

	/**
	 * Mengonversi {@code Set} menjadi {@code JSONArray} dengan opsi pengecualian kelas tertentu
	 * dari serialisasi (untuk menghindari rekursi tak terbatas pada relasi bidireksional).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonJSONUtil.convertToJson(obj, clazzPengecualian)}.</p>
	 *
	 * @param obj               Set yang akan dikonversi
	 * @param clazzPengecualian nama kelas yang dikecualikan dari serialisasi; varargs
	 * @return JSONArray hasil serialisasi
	 */
	@SuppressWarnings("rawtypes")
	public static JSONArray convertToJson(Set obj, String... clazzPengecualian) {
		return CommonJSONUtil.convertToJson(obj, clazzPengecualian);
	}

	/**
	 * Mengonversi {@code Collection} menjadi {@code JSONArray} dengan opsi pengecualian kelas.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonJSONUtil.convertToJson(obj, clazzPengecualian)}. Mendukung semua
	 * implementasi Collection termasuk List dan Set.</p>
	 *
	 * @param obj               koleksi yang akan dikonversi
	 * @param clazzPengecualian nama kelas yang dikecualikan dari serialisasi
	 * @return JSONArray hasil serialisasi
	 */
	@SuppressWarnings("rawtypes")
	public static JSONArray convertToJson(Collection obj, String... clazzPengecualian) {
		return CommonJSONUtil.convertToJson(obj, clazzPengecualian);
	}

	/**
	 * Memeriksa apakah nilai pada kunci {@code key} dalam {@code GeneralValueObject} adalah
	 * bertipe tanggal ({@code Date}) atau timestamp.
	 *
	 * <p><b>Tujuan.</b> Digunakan oleh serializer JSON untuk menentukan format output yang
	 * tepat — string tanggal ISO 8601 vs nilai numerik.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonJSONUtil.checkTanggalAtauTimeStamp(obj, key)}.</p>
	 *
	 * @param obj objek sumber yang diperiksa
	 * @param key kunci properti yang akan diperiksa tipenya
	 * @return {@code true} bila nilai adalah Date atau Timestamp
	 */
	public static boolean checkTanggalAtauTimeStamp(GeneralValueObject obj, String key) {
		return CommonJSONUtil.checkTanggalAtauTimeStamp(obj, key);
	}

	/**
	 * Mengonversi {@code GeneralValueObject} menjadi {@code JSONObject} dimulai dari
	 * indeks ke-0.
	 *
	 * <p><b>Cara kerja.</b> Menetapkan {@code indexke = 0} lalu mendelegasikan ke
	 * {@link #convertToJsonObject(Integer, GeneralValueObject, String...)}.</p>
	 *
	 * @param obj               objek yang akan dikonversi
	 * @param clazzPengecualian nama kelas yang dikecualikan dari serialisasi
	 * @return JSONObject hasil konversi
	 */
	public static JSONObject convertToJsonObject(GeneralValueObject obj, String... clazzPengecualian) {
		Integer indexke = 0;
		return convertToJsonObject(indexke, obj, clazzPengecualian);
	}

	/**
	 * Mengonversi {@code GeneralValueObject} menjadi {@code JSONObject} dengan nomor indeks
	 * yang digunakan untuk nama kunci saat ada beberapa objek dalam satu konteks serialisasi.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonJSONUtil.convertToJsonObject(indexke, obj, clazzPengecualian)}.</p>
	 *
	 * @param indexke           indeks urutan objek dalam konteks serialisasi
	 * @param obj               objek yang akan dikonversi
	 * @param clazzPengecualian nama kelas yang dikecualikan
	 * @return JSONObject hasil konversi
	 */
	public static JSONObject convertToJsonObject(Integer indexke, GeneralValueObject obj, String... clazzPengecualian) {
		return CommonJSONUtil.convertToJsonObject(indexke, obj, clazzPengecualian);
	}

	/**
	 * Mengonversi {@code GeneralValueObject} menjadi {@code JSONObject} secara sederhana
	 * dengan kedalaman ({@code dept}) tertentu untuk membatasi serialisasi rekursif.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonJSONUtil.convertToJsonObjectSimple(obj, dept)}. Parameter {@code dept}
	 * membatasi kedalaman rekursi sehingga relasi mendalam tidak menyebabkan stack overflow.</p>
	 *
	 * @param obj  objek sumber
	 * @param dept kedalaman serialisasi maksimal; 0 = hanya properti langsung
	 * @return JSONObject hasil konversi sederhana
	 */
	public static JSONObject convertToJsonObjectSimple(GeneralValueObject obj, int dept) {
		return CommonJSONUtil.convertToJsonObjectSimple(obj, dept);
	}

	/**
	 * Menentukan lokasi berkas (File) terkait properti {@code key} dari sebuah
	 * {@code GeneralValueObject}.
	 *
	 * <p><b>Tujuan.</b> Menyediakan akses ke path berkas yang disimpan dalam kolom JSON
	 * (biasanya kolom {@code lampiran} atau {@code berkas}) di entitas database.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonFileMediaHelper.getFileLocation(obj, key)}.</p>
	 *
	 * @param obj objek entitas sumber
	 * @param key kunci properti yang berisi informasi path berkas
	 * @return objek {@code File} berisi path berkas; boleh null bila tidak ditemukan
	 */
	public static File getFileLocation(GeneralValueObject obj, String key) {
		return CommonFileMediaHelper.getFileLocation(obj, key);
	}

	/**
	 * Menentukan lokasi berkas (File) untuk entitas dengan kelas dan primary key tertentu,
	 * di bawah kunci properti {@code key}.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonJSONUtil.getFileLocation(clazz, id, key)} yang memuat entitas dari
	 * database lalu membaca properti berkas dari data JSON-nya.</p>
	 *
	 * @param clazz kelas entitas
	 * @param id    primary key entitas
	 * @param key   kunci properti berkas dalam data JSON
	 * @return objek {@code File} berisi path berkas
	 */
	@SuppressWarnings("rawtypes")
	public static File getFileLocation(Class clazz, Serializable id, String key) {

		return CommonJSONUtil.getFileLocation(clazz, id, key);
	}

	/**
	 * Menyimpan {@code JSONObject} sementara ke dalam berkas temp yang dikaitkan dengan
	 * properti {@code key} dari entitas {@code obj}.
	 *
	 * <p><b>Tujuan.</b> Menyimpan data JSON sementara (misalnya draft atau perubahan belum
	 * tersimpan) tanpa langsung menulis ke database, aman untuk ditinjau sebelum dikomit.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonFileMediaHelper.setJSONTemporary(obj, key, jsonObject)} yang menulis
	 * JSON ke berkas sementara di direktori temp dan mengembalikan referensi berkas.</p>
	 *
	 * @param obj        entitas pemilik data
	 * @param key        kunci properti yang dikaitkan dengan berkas
	 * @param jsonObject data JSON yang akan disimpan sementara
	 * @return File berkas sementara yang dibuat
	 */
	public static File setJSONTemporary(GeneralValueObject obj, String key, JSONObject jsonObject) {
		return CommonFileMediaHelper.setJSONTemporary(obj, key, jsonObject);
	}

	/**
	 * Mengambil {@code JSONObject} sementara yang sebelumnya disimpan dengan
	 * {@link #setJSONTemporary} untuk entitas dan kunci tertentu.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonJSONUtil.getJSONTemporary(obj, key)}
	 * yang membaca dan mengurai berkas JSON sementara dari direktori temp.</p>
	 *
	 * @param obj entitas pemilik data sementara
	 * @param key kunci properti yang dikaitkan dengan berkas sementara
	 * @return JSONObject yang disimpan; null bila tidak ada berkas temp
	 */
	public static JSONObject getJSONTemporary(GeneralValueObject obj, String key) {
		return CommonJSONUtil.getJSONTemporary(obj, key);

	}

	/**
	 * Mengekstrak daftar URL dari string teks (misalnya dari kolom deskripsi atau komentar
	 * yang mengandung tautan).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonJSONUtil.getUrls(s)} yang
	 * menggunakan ekspresi reguler untuk mendeteksi URL http/https/ftp dalam teks bebas.</p>
	 *
	 * @param s teks yang akan dipindai untuk URL
	 * @return List URL yang ditemukan; list kosong bila tidak ada
	 */
	public static List<String> getUrls(String s) {
		return CommonJSONUtil.getUrls(s);
	}

	/**
	 * Menampilkan konten dari URL eksternal di dalam komponen ZK (Div atau serupa).
	 *
	 * <p><b>Tujuan.</b> Mengintegrasikan konten web eksternal (PDF, halaman web, video embed)
	 * ke dalam halaman ZK menggunakan iframe atau media player.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code UrlDisplayHelper.displayUrlContent(u, vboxa)}.</p>
	 *
	 * @param u     URL sumber konten yang akan ditampilkan
	 * @param vboxa komponen ZK induk tempat konten disisipkan
	 */
	public static void displayUrlContent(String u, Component vboxa) {
		UrlDisplayHelper.displayUrlContent(u, vboxa);
	}

	/**
	 * Membuka URL di tab/jendela baru browser dengan judul tertentu.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code UrlDisplayHelper.openUrl(url, title)}
	 * yang menggunakan JavaScript ZK {@code Clients.evalJavaScript("window.open(...)}")}
	 * untuk membuka tab baru di browser pengguna.</p>
	 *
	 * @param url   URL yang akan dibuka; tidak boleh null
	 * @param title judul tab/jendela baru
	 */
	public static void openUrl(String url, String title) {
		UrlDisplayHelper.openUrl(url, title);
	}

	/**
	 * Menghasilkan string style CSS inline dengan dimensi minimal tertentu.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code UrlDisplayHelper.getStyleString(minHeight, minWidth)} yang merangkai string
	 * seperti {@code "min-height:Xpx;min-width:Ypx;"}.</p>
	 *
	 * @param minHeight ketinggian minimal dalam piksel
	 * @param minWidth  lebar minimal dalam piksel
	 * @return string style CSS inline
	 */
	public static String getStyleString(int minHeight, int minWidth) {
		return UrlDisplayHelper.getStyleString(minHeight, minWidth);
	}

	/**
	 * Menampilkan tombol atau widget konferensi video online (Zoom, Google Meet, dsb.)
	 * di dalam komponen ZK sesuai konfigurasi pertemuan.
	 *
	 * <p><b>Tujuan.</b> Menyediakan antarmuka masuk ke ruang konferensi online langsung
	 * dari halaman pertemuan e-Learning atau jadwal kuliah.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonUiFactoryHelper.tampilOnline(pertemuan, vbox)}.</p>
	 *
	 * @param pertemuan objek pertemuan yang mengandung URL konferensi
	 * @param vbox      komponen ZK induk tempat widget disisipkan
	 */
	public static void tampilOnline(GeneralValueObject pertemuan, Component vbox) {
		CommonUiFactoryHelper.tampilOnline(pertemuan, vbox);
	}

	/**
	 * Membuat dan menambahkan tombol konferensi video ke komponen ZK, dengan berbagai
	 * opsi orientasi dan tampilan.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonUiFactoryHelper.createVideoConference(generalValueObject, hbox, vertical, isButton, externalListener)}.</p>
	 *
	 * @param generalValueObject entitas pertemuan atau kegiatan yang memiliki URL konferensi
	 * @param hbox               komponen ZK induk (Hbox, Vbox, dsb.)
	 * @param vertical           {@code true} untuk tata letak vertikal
	 * @param isButton           {@code true} untuk tampilan tombol; {@code false} untuk tautan
	 * @param externalListener   listener tambahan yang dipicu saat tombol diklik; boleh null
	 * @return Button ZK yang dibuat
	 * @throws Exception bila pembuatan komponen gagal
	 */
	public static Button createVideoConference(final GeneralValueObject generalValueObject, Component hbox,
			boolean vertical, boolean isButton, final EventListener externalListener) throws Exception {
		return CommonUiFactoryHelper.createVideoConference(generalValueObject, hbox, vertical, isButton,
				externalListener);
	}

	/**
	 * Memperbarui nilai konversi (misalnya konversi nilai ke skala 4.0 atau nilai huruf)
	 * untuk detail perkuliahan tertentu.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicSyncHelper.updateNilaiKonversi(detailperkuliahan, nilai, session)}
	 * yang mengambil format nilai berlaku dan menghitung konversinya lalu menyimpan hasilnya.</p>
	 *
	 * @param detailperkuliahan entitas detail perkuliahan yang nilainya diperbarui
	 * @param nilai             nilai mentah (angka 0–100 atau sesuai format)
	 * @param session           sesi Hibernate aktif untuk penyimpanan
	 */
	public static void updateNilaiKonversi(Detailperkuliahan detailperkuliahan, Double nilai, Session session) {
		CommonAcademicSyncHelper.updateNilaiKonversi(detailperkuliahan, nilai, session);
	}

	/**
	 * Menghasilkan string identifikasi "diupload oleh" berdasarkan pengguna ({@code Tbmuser}).
	 *
	 * <p><b>Tujuan.</b> Digunakan untuk men-tag berkas atau lampiran dengan informasi siapa
	 * yang mengunggah, dalam format ID unik yang dapat diverifikasi.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code UIClassHelper.generateOlehId(tbmuser)}.</p>
	 *
	 * @param tbmuser pengguna yang mengunggah; tidak boleh null
	 * @return string ID pengunggah
	 */
	public static String generateOlehId(Tbmuser tbmuser) {
		return UIClassHelper.generateOlehId(tbmuser);
	}

	/**
	 * Menampilkan informasi "diupload oleh" di dalam komponen ZK (tooltip atau label kecil
	 * di bawah berkas).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonFileMediaHelper.infoDiuploadOleh(olehId, oleh, parent)} yang membuat
	 * Label kecil berisi nama pengunggah dan waktu upload.</p>
	 *
	 * @param olehId string ID pengunggah (dari generateOlehId)
	 * @param oleh   nama pengunggah yang akan ditampilkan
	 * @param parent komponen ZK induk tempat informasi disisipkan
	 */
	public static void infoDiuploadOleh(String olehId, String oleh, Component parent) {
		CommonFileMediaHelper.infoDiuploadOleh(olehId, oleh, parent);
	}

	/**
	 * Membungkus komponen ZK dalam kontainer yang dapat di-scroll secara vertikal,
	 * berguna untuk konten yang mungkin melebihi tinggi area tampilan.
	 *
	 * <p><b>Tujuan.</b> Mencegah konten terpotong pada halaman dengan area tampilan terbatas,
	 * dengan menambahkan scrollbar otomatis bila diperlukan.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonUiFactoryHelper.tampilanScroll(component)}
	 * yang membungkus {@code component} dalam Grid dengan satu Row bertanda scroll.</p>
	 *
	 * @param component komponen ZK yang akan dibungkus
	 * @return Row ZK pembungkus scrollable
	 */
	public static Row tampilanScroll(Component component) {
		return CommonUiFactoryHelper.tampilanScroll(component);
	}

	/**
	 * Varian tampilanScroll untuk komponen pengisi tinggi (Tabbox/Iframe/
	 * Borderlayout ber-height 100%). Lihat catatan di UIClassHelper:
	 * tampilanScroll biasa membuat child ber-height 100% kolaps karena konten
	 * ditaruh di Row Grid yang tingginya auto.
	 */
	public static org.zkoss.zul.Center tampilanScrollTabbox(Component component) {
		return CommonUiFactoryHelper.tampilanScrollTabbox(component);
	}

	/**
	 * Konfigurasi Center (Borderlayout) yang SUDAH ADA agar bisa discroll — dipakai saat
	 * layar SUDAH punya Borderlayout+Center sendiri (mis. North=toolbar, Center=grid,
	 * South=total) dan tinggal butuh Center-nya scroll-ready.
	 *
	 * <p><b>WAJIB:</b> taruh Grid/komponen isi LANGSUNG sebagai anak {@code center} ini
	 * setelah memanggil method ini — JANGAN dibungkus Div tambahan ("scrollWrapper").
	 * Div pembungkus ber-overflow:auto+height:100% SERING TIDAK memunculkan scrollbar
	 * sama sekali pada versi ZK yang dipakai (height:100% di dalam &lt;td&gt; tidak
	 * ter-recompute otomatis setelah render ulang) — pola Div inilah yang GAGAL di grid
	 * "Akun Transaksi Jurnal Umum" (TransaksiJurnalUmumHelper) sebelum diganti ke pola
	 * Center-langsung ini. Contoh pakai:
	 * <pre>
	 * Center center = new Center();
	 * center.setParent(borderlayout);
	 * Common.jadikanCenterScrollable(center);
	 * MyGrid grid = new MyGrid();
	 * grid.setWidth("100%");
	 * grid.setHeight("100%");
	 * grid.setParent(center); // LANGSUNG, tanpa Div — Center-&gt;Grid-&gt;Rows-&gt;Row
	 * </pre>
	 *
	 * <p>Untuk Tab panel yang BELUM punya Borderlayout+Center sendiri, pakai
	 * {@link #tampilanScrollTabbox(Component)} — perilakunya sama, hanya method itu
	 * membuat Borderlayout+Center barunya sendiri dan MENGEMBALIKAN Center tsb.</p>
	 *
	 * @param center Center yang sudah di-parent-kan ke Borderlayout-nya sendiri.
	 */
	public static void jadikanCenterScrollable(org.zkoss.zul.Center center) {
		CommonUiFactoryHelper.jadikanCenterScrollable(center);
	}

	/**
	 * Varian tampilanScroll dengan tipe kembalian {@code MyFormRow} (varian 1) — digunakan
	 * dalam konteks form yang membutuhkan layout baris formulir scrollable.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonUiFactoryHelper.tampilanScroll1(component)}.</p>
	 *
	 * @param component komponen ZK yang akan dibungkus
	 * @return MyFormRow pembungkus scrollable untuk konteks form
	 */
	public static MyFormRow tampilanScroll1(Component component) {
		return CommonUiFactoryHelper.tampilanScroll1(component);
	}

	/**
	 * Varian tampilanScroll dengan tipe kembalian {@code MyFormRow} (varian 2).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonUiFactoryHelper.tampilanScroll2(component)}.</p>
	 *
	 * @param component komponen ZK yang akan dibungkus
	 * @return MyFormRow pembungkus scrollable (varian 2)
	 */
	public static MyFormRow tampilanScroll2(Component component) {
		return CommonUiFactoryHelper.tampilanScroll2(component);
	}

	/**
	 * Varian tampilanScroll dengan tipe kembalian {@code MyFormRow} (varian 3).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonUiFactoryHelper.tampilanScroll3(component)}.</p>
	 *
	 * @param component komponen ZK yang akan dibungkus
	 * @return MyFormRow pembungkus scrollable (varian 3)
	 */
	public static MyFormRow tampilanScroll3(Component component) {
		return CommonUiFactoryHelper.tampilanScroll3(component);
	}

	/**
	 * Varian tampilanScroll dengan tipe kembalian {@code MyFormRow} (varian 4).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonUiFactoryHelper.tampilanScroll4(component)}.</p>
	 *
	 * @param component komponen ZK yang akan dibungkus
	 * @return MyFormRow pembungkus scrollable (varian 4)
	 */
	public static MyFormRow tampilanScroll4(Component component) {
		return CommonUiFactoryHelper.tampilanScroll4(component);
	}

	/**
	 * Menghasilkan string CSS inline standar untuk menampilkan konten bertipe media
	 * (video, iframe, PDF) dengan dimensi default.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code UrlDisplayHelper.getStyleContent()}
	 * yang mengembalikan string CSS seperti {@code "width:100%;height:600px;"}.</p>
	 *
	 * @return string CSS inline untuk kontainer konten media
	 */
	public static String getStyleContent() {
		return UrlDisplayHelper.getStyleContent();
	}

	/**
	 * Menghasilkan string CSS inline alternatif (varian 1) untuk kontainer konten media.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code UrlDisplayHelper.getStyleContent1()}
	 * yang menggunakan dimensi berbeda dari {@link #getStyleContent()}.</p>
	 *
	 * @return string CSS inline varian 1 untuk kontainer konten media
	 */
	public static String getStyleContent1() {
		return UrlDisplayHelper.getStyleContent1();
	}

	/**
	 * Menghasilkan string CSS inline untuk kontainer konten audio.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code UrlDisplayHelper.getStyleContentAudio()}
	 * yang mengembalikan CSS yang sesuai untuk pemutar audio (tinggi lebih kecil dari video).</p>
	 *
	 * @return string CSS inline untuk kontainer audio
	 */
	public static String getStyleContentAudio() {
		return UrlDisplayHelper.getStyleContentAudio();
	}

	/**
	 * Membuat dan menambahkan tombol konferensi video ke komponen ZK.
	 *
	 * <p><b>Catatan.</b> Nama method ini mengandung typo "conrefrence" (seharusnya "conference")
	 * yang sengaja dipertahankan agar tidak memutus referensi dari seluruh codebase yang
	 * sudah memanggil method ini dengan nama tersebut.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonUiFactoryHelper.createVideoConrefrence}. Lihat
	 * {@link #createVideoConference} untuk dokumentasi parameter.</p>
	 *
	 * @param generalValueObject entitas yang memiliki URL konferensi
	 * @param hbox               komponen induk
	 * @param vertical           orientasi tata letak
	 * @param button             tampilan tombol vs tautan
	 * @param eventListener      listener tambahan; boleh null
	 * @return Button ZK yang dibuat
	 * @throws Exception bila pembuatan komponen gagal
	 */
	public static Button createVideoConrefrence(final GeneralValueObject generalValueObject, Component hbox,
			boolean vertical, boolean button, final EventListener eventListener) throws Exception {
		return CommonUiFactoryHelper.createVideoConrefrence(generalValueObject, hbox, vertical, button, eventListener);
	}

	/**
	 * Menghasilkan string CSS untuk kontainer konten dengan tinggi maksimal yang lebih kecil
	 * (versi compact/kecil).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code UrlDisplayHelper.getStyleContentHeighMaxKecil()} yang mengembalikan CSS dengan
	 * {@code max-height} yang lebih rendah, cocok untuk preview atau panel samping.</p>
	 *
	 * @return string CSS inline dengan tinggi maksimal versi kecil
	 */
	public static String getStyleContentHeighMaxKecil() {
		return UrlDisplayHelper.getStyleContentHeighMaxKecil();
	}

	/**
	 * Menghasilkan string CSS untuk kontainer konten dengan tinggi maksimal penuh.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code UrlDisplayHelper.getStyleContentHeighMax()} yang mengembalikan CSS dengan
	 * {@code max-height} sesuai layar penuh, cocok untuk tampilan modal utama.</p>
	 *
	 * @return string CSS inline dengan tinggi maksimal penuh
	 */
	public static String getStyleContentHeighMax() {
		return UrlDisplayHelper.getStyleContentHeighMax();
	}

	/**
	 * Mengonversi {@code JSONObject} yang merepresentasikan referensi bibliografi menjadi
	 * {@code CSLItemData} — format data yang digunakan oleh mesin sitasi CSL (Citation Style
	 * Language) untuk menghasilkan daftar pustaka atau kutipan ilmiah.
	 *
	 * <p><b>Tujuan.</b> Menjembatani antara format penyimpanan data referensi internal sistem
	 * (JSON dengan kunci Bahasa Indonesia: judul, pengarang, tanggal, isbn, issn, penerbit,
	 * sumber) dengan format standar {@code CSLItemData} yang diperlukan oleh pustaka
	 * {@code citeproc-java} untuk merender sitasi dalam berbagai gaya (APA, MLA, Chicago, dsb.).</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Membuat {@code CSLItemDataBuilder} bertipe {@code CSLType.BOOK} sebagai tipe default.</li>
	 *   <li>Menyimpan representasi JSON mentah via {@code builder.note(jsonObject.toString())}
	 *       sebagai fallback dan dokumentasi sumber.</li>
	 *   <li>Jika kunci {@code "judul"} tidak null, menetapkan judul item.</li>
	 *   <li>Jika kunci {@code "pengarang"} tidak null, memecah string dengan pemisah {@code ";"} dan
	 *       memproses tiap nama pengarang: memecah menjadi {@code given} (nama depan) dan
	 *       {@code family} (nama belakang) berdasarkan spasi pertama, lalu memanggil
	 *       {@code builder.author(given, family)}.</li>
	 *   <li>Jika kunci {@code "tanggal"} tidak null, mengurai string tanggal dengan format
	 *       {@code dateFormat1} ({@code dd-MM-yyyy}), mengekstrak tahun, bulan, dan hari,
	 *       lalu memanggil {@code builder.issued(...)}. Error penguraian ditangkap oleh
	 *       {@link #tampilErrorJikaAdmin(Exception)}.</li>
	 *   <li>Mengisi ISBN, ISSN, penerbit, dan URL sumber bila kunci-kunci tersebut tidak null.</li>
	 *   <li>Memanggil {@code builder.build()} dan mengembalikan hasilnya.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error.</b> Kegagalan penguraian tanggal ditangkap secara internal dan
	 * dilaporkan via {@link #tampilErrorJikaAdmin(Exception)} tanpa menghentikan proses;
	 * field tanggal akan absen dari item bila penguraian gagal.</p>
	 *
	 * @param jsonObject JSONObject berisi data referensi dengan kunci Bahasa Indonesia
	 *                   (judul, pengarang, tanggal, isbn, issn, penerbit, sumber)
	 * @return {@code CSLItemData} siap digunakan oleh mesin sitasi citeproc-java
	 * @throws Exception bila konstruksi item CSL gagal secara keseluruhan
	 */
	public static CSLItemData convertToCSLItemData(JSONObject jsonObject) throws Exception {
		CSLItemDataBuilder builder = new CSLItemDataBuilder().type(CSLType.BOOK);
		builder.note(jsonObject.toString());
		if (!jsonObject.isNull("judul"))
			builder.title(jsonObject.getString("judul"));

		if (!jsonObject.isNull("pengarang"))
			for (String pengarang : jsonObject.getString("pengarang").split(";")) {
				if (pengarang != null && !pengarang.trim().isEmpty()) {
					String[] pp = pengarang.split(" ", 1);
					String given = pp[0];
					String family = pp.length > 1 ? pp[1] : "";
					builder.author(given, family);
				}
			}

		if (!jsonObject.isNull("tanggal")) {
			Date tanggal = null;
			try {
				tanggal = Common.dateFormat1.get().parse(jsonObject.getString("tanggal"));
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(tanggal);
				builder.issued(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
						calendar.get(Calendar.DATE));
			} catch (Exception e) {
				tampilErrorJikaAdmin(e);
			}

		}

		if (!jsonObject.isNull("isbn")) {
			builder.ISBN(jsonObject.getString("isbn"));
		}
		if (!jsonObject.isNull("issn")) {
			builder.ISSN(jsonObject.getString("issn"));
		}
		if (!jsonObject.isNull("penerbit")) {
			builder.publisher(jsonObject.getString("penerbit"));
		}
		if (!jsonObject.isNull("sumber")) {
			builder.source(jsonObject.getString("sumber"));
			builder.URL(jsonObject.getString("sumber"));
		}

		CSLItemData item = builder.build();
		return item;
	}

	/**
	 * Menentukan path direktori ROOT webapp yang ter-deploy (folder di atas {@code WEB-INF/classes}).
	 *
	 * <p><b>Tujuan.</b> Memberi titik acuan filesystem untuk membaca/menulis berkas pendukung aplikasi
	 * (aset, berkas sementara, cache, dll.) secara relatif terhadap lokasi deploy—tanpa hardcode path
	 * absolut yang berbeda antar-server.</p>
	 *
	 * <p><b>Cara kerja.</b> Mengambil path resource root classloader, men-decode URL (UTF-8), lalu
	 * memotong pada {@code "/WEB-INF/classes/"} untuk memperoleh direktori webapp; hasilnya dinormalkan
	 * lewat {@code new File(fullPath).getPath()}. Bila terjadi kegagalan apa pun, mengembalikan path
	 * mentah classloader sebagai fallback.</p>
	 *
	 * <p><b>Return.</b> Path direktori root webapp (atau path mentah classloader bila gagal mem-parse).</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Dibungkus try/catch yang fail-safe (selalu
	 * mengembalikan sesuatu). Ada {@code System.out.println} diagnostik bawaan. Method ini sensitif
	 * terhadap struktur deploy standar ({@code WEB-INF/classes}); pada kemasan non-standar hasilnya
	 * bisa berbeda—pertimbangkan saat memindahkan ke container/struktur lain.</p>
	 *
	 * @return path direktori root webapp yang ter-deploy
	 */
	public static String getPath() {

		String path = Common.class.getClassLoader().getResource("").getPath();
		try {
			String fullPath = URLDecoder.decode(path, "UTF-8");
			String pathArr[] = fullPath.split("/WEB-INF/classes/");
			System.out.println(fullPath);
			System.out.println(pathArr[0]);
			fullPath = pathArr[0];

			String reponsePath = "";
			// to read a file from webcontent
			reponsePath = new File(fullPath).getPath();
			return reponsePath;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17089");
			// TODO: handle exception
		}
		return path;
	}

	/**
	 * Menentukan path direktori induk dari root webapp (direktori {@code webapps} Tomcat),
	 * yaitu satu level di atas direktori webapp yang ter-deploy.
	 *
	 * <p><b>Tujuan.</b> Berguna saat perlu mengakses direktori di luar webapp aktif, misalnya
	 * berkas konfigurasi global yang diletakkan di luar konteks web, atau untuk mengetahui
	 * lokasi instalasi Tomcat.</p>
	 *
	 * <p><b>Cara kerja.</b> Mengikuti langkah yang sama dengan {@link #getPath()}, namun
	 * menambahkan {@code .getParentFile().getPath()} pada hasil akhir sehingga menghasilkan
	 * direktori induk (satu level lebih atas dari root webapp). Fallback ke path mentah
	 * classloader bila terjadi error.</p>
	 *
	 * @return path direktori induk root webapp (direktori webapps); path mentah classloader bila gagal
	 */
	public static String getPathWebapps() {

		String path = Common.class.getClassLoader().getResource("").getPath();
		try {
			String fullPath = URLDecoder.decode(path, "UTF-8");
			String pathArr[] = fullPath.split("/WEB-INF/classes/");
			System.out.println(fullPath);
			System.out.println(pathArr[0]);
			fullPath = pathArr[0];

			String reponsePath = "";
			// to read a file from webcontent
			reponsePath = new File(fullPath).getParentFile().getPath();
			return reponsePath;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17124");
			// TODO: handle exception
		}
		return path;
	}

	/**
	 * Mengekstrak nama konteks (context path) webapp dari path deploy — yaitu segmen
	 * terakhir dari path root webapp setelah memotong suffix {@code /WEB-INF/classes/}.
	 *
	 * <p><b>Tujuan.</b> Mengetahui nama konteks aplikasi (misalnya {@code "ais"} atau
	 * {@code "ecampus"}) secara programatik tanpa bergantung pada {@code HttpServletRequest}
	 * — berguna untuk konstruksi URL relatif di luar konteks request HTTP.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Mengambil path resource root classloader.</li>
	 *   <li>Men-decode URL (UTF-8) dan memotong pada {@code "/WEB-INF/classes/"}.</li>
	 *   <li>Memecah path dengan {@code "/"} dan mengambil elemen terakhir sebagai nama konteks.</li>
	 * </ol>
	 * Fallback ke path mentah classloader bila terjadi error.</p>
	 *
	 * @return nama konteks aplikasi (misalnya {@code "ais"}); path mentah classloader bila gagal
	 */
	public static String getContextPath() {

		String path = Common.class.getClassLoader().getResource("").getPath();
		try {
			String fullPath = URLDecoder.decode(path, "UTF-8");
			String pathArr[] = fullPath.split("/WEB-INF/classes/");
			System.out.println(fullPath);
			System.out.println(pathArr[0]);
			fullPath = pathArr[0];

			String[] ss = fullPath.split("/");

			String contextPath = ss[ss.length - 1];
			return contextPath;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17162");
			// TODO: handle exception
		}
		return path;
	}

	/**
	 * Komparator statis untuk mengurutkan daftar {@code Kegiatan} berdasarkan semester
	 * secara menaik (ascending).
	 *
	 * <p><b>Tujuan.</b> Digunakan bersama {@code Collections.sort} atau pada
	 * {@code TreeSet}/{@code TreeMap} untuk menyortir kegiatan akademik berdasarkan
	 * urutan semester sehingga tampil secara kronologis.</p>
	 *
	 * <p><b>Cara kerja.</b> Implementasi anonymous class {@code Comparator<Kegiatan>} yang
	 * membandingkan {@code o1.getSemster().compareTo(o2.getSemster())}. Perhatikan: nama
	 * getter adalah {@code getSemster()} (typo, satu 'e') yang sengaja dibiarkan sesuai
	 * nama field di entitas. Error komparasi (misalnya nilai null) ditangkap dan mengembalikan
	 * 0 (dianggap sama urutan) agar sort tidak gagal.</p>
	 */
	public static Comparator<Kegiatan> compareBySmt = new Comparator<Kegiatan>() {
		@Override
		public int compare(Kegiatan o1, Kegiatan o2) {
			try {
				return o1.getSemster().compareTo(o2.getSemster());
			} catch (Exception e) {
				return 0;
			}
		}
	};

	/**
	 * Menginisialisasi serangkaian {@code Radiogroup} ZK secara massal dari larik entitas
	 * Hibernate, satu radiogroup per elemen larik.
	 *
	 * <p><b>Tujuan.</b> Menyederhanakan inisialisasi banyak filter radiogroup sekaligus
	 * pada halaman dengan beberapa panel kriteria berbasis radio button.</p>
	 *
	 * <p><b>Cara kerja.</b> Mengiterasi indeks {@code 0..radiogroups.length-1} dan memanggil
	 * {@code insertRadio(radiogroups[i], properties[i], classes[i])} untuk tiap pasangan.</p>
	 *
	 * @param radiogroups larik Radiogroup ZK yang akan diisi
	 * @param properties  larik nama properti label untuk masing-masing radiogroup
	 * @param classes     larik kelas entitas Hibernate sumber data radio
	 */
	@SuppressWarnings("rawtypes")
	public static void initRadios(Radiogroup[] radiogroups, String[] properties, Class[] classes) {

		for (int i = 0; i < radiogroups.length; i++) {
			insertRadio(radiogroups[i], properties[i], classes[i]);
		}
	}

	/**
	 * Menginisialisasi serangkaian {@code Radiogroup} ZK secara massal dengan filter
	 * {@code Criterion} Hibernate untuk masing-masing radiogroup.
	 *
	 * <p><b>Cara kerja.</b> Mengiterasi indeks larik dan memanggil
	 * {@code insertRadio(radiogroups[i], properties[i], classes[i], criterions[i])}.</p>
	 *
	 * @param radiogroups larik Radiogroup ZK yang akan diisi
	 * @param properties  larik nama properti label
	 * @param classes     larik kelas entitas sumber data
	 * @param criterions  larik Criterion Hibernate pemfilter; tiap elemen dapat null (tidak difilter)
	 */
	@SuppressWarnings({ "rawtypes" })
	public static void initRadios(Radiogroup[] radiogroups, String[] properties, Class[] classes,
			Criterion[] criterions) {
		for (int i = 0; i < radiogroups.length; i++) {
			insertRadio(radiogroups[i], properties[i], classes[i], criterions[i]);
		}
	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan daftar entitas Hibernate dari kelas {@code clazz},
	 * dengan opsi satu atau beberapa {@code Criterion} pemfilter.
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Membuat {@code Criteria} dari {@code currentSession()} untuk kelas {@code clazz}.</li>
	 *   <li>Menambahkan semua {@code Criterion} yang tidak null ke criteria.</li>
	 *   <li>Mengambil daftar via {@code ConstantValues.simpleList} dan mencoba mengurutkan.</li>
	 *   <li>Memanggil {@link #insertRadioItems(Radiogroup, String, List)} untuk merender Radio.</li>
	 * </ol>
	 * </p>
	 *
	 * @param radiogroup radiogroup yang akan diisi
	 * @param property   nama properti entitas yang digunakan sebagai label radio
	 * @param clazz      kelas entitas sumber data
	 * @param criterions filter Hibernate; varargs, boleh berisi null
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void insertRadio(Radiogroup radiogroup, String property, Class clazz, Criterion... criterions) {
		Criteria criteria = HibernateUtil.currentSession().createCriteria(clazz);
		if (criterions != null) {
			for (Criterion criterion : criterions) {
				if (criterion != null) {
					criteria.add(criterion);
				}
			}
		}
		List list = ConstantValues.simpleList(criteria, clazz);
		try {
			Collections.sort(list);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17266");

		}
		insertRadioItems(radiogroup, property, list);
	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan daftar entitas Hibernate dari kelas {@code clazz},
	 * difilter oleh satu {@code Criterion}.
	 *
	 * <p><b>Cara kerja.</b> Membuat Criteria, menambahkan criterion, mengambil list, lalu
	 * memanggil {@link #insertRadioItems(Radiogroup, String, List)}.</p>
	 *
	 * @param radiogroup radiogroup yang akan diisi
	 * @param property   properti label radio
	 * @param clazz      kelas entitas sumber
	 * @param criterion  filter Hibernate; tidak boleh null
	 */
	public static void insertRadio(Radiogroup radiogroup, String property, Class<?> clazz, Criterion criterion) {
		Criteria criteria = HibernateUtil.currentSession().createCriteria(clazz);
		criteria.add(criterion);
		List<?> list = ConstantValues.simpleList(criteria, clazz);
		insertRadioItems(radiogroup, property, list);
	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan deskripsi kustom dan daftar entitas terfilter.
	 *
	 * <p><b>Cara kerja.</b> Membuat Criteria dengan semua criterions, mengambil list, mengurutkan,
	 * lalu memanggil {@link #insertRadioItems(Radiogroup, String, String, List)} dengan
	 * {@code deskripsi} sebagai label tambahan (misalnya untuk header atau prefix).</p>
	 *
	 * @param radiogroup radiogroup yang akan diisi
	 * @param property   properti label radio
	 * @param deskripsi  deskripsi tambahan ditampilkan di radio
	 * @param clazz      kelas entitas sumber
	 * @param criterions filter Hibernate; varargs
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void insertRadio(Radiogroup radiogroup, String property, String deskripsi, Class clazz,
			Criterion... criterions) {
		Criteria criteria = HibernateUtil.currentSession().createCriteria(clazz);
		for (Criterion criterion : criterions) {
			criteria.add(criterion);
		}
		List list = ConstantValues.simpleList(criteria, clazz);
		try {
			Collections.sort(list);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17314");

		}
		insertRadioItems(radiogroup, property, deskripsi, list);
	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan seluruh data entitas Hibernate (tanpa filter),
	 * diurutkan secara alami.
	 *
	 * <p><b>Cara kerja.</b> Guard null pada radiogroup. Membuat Criteria tanpa filter,
	 * mengambil seluruh data, mengurutkan, lalu memanggil
	 * {@link #insertRadioItems(Radiogroup, String, List)}.</p>
	 *
	 * @param radiogroup radiogroup yang akan diisi; bila null, method tidak melakukan apa pun
	 * @param property   properti label radio
	 * @param clazz      kelas entitas sumber
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void insertRadio(Radiogroup radiogroup, String property, Class<?> clazz) {
		if (radiogroup == null)
			return;
		Criteria criteria = HibernateUtil.currentSession().createCriteria(clazz);

		List list = ConstantValues.simpleList(criteria, clazz);
		try {
			Collections.sort(list);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17341");

		}
		insertRadioItems(radiogroup, property, list);
	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan radio item bertipe {@code MyRadioConfig}
	 * (radio dengan gaya konfigurasi kustom sistem), dengan filter Criterion varargs.
	 *
	 * <p><b>Tujuan.</b> Varian dari {@link #insertRadio(Radiogroup, String, Class, Criterion...)}
	 * yang menggunakan {@code MyRadioConfig} (subkelas Radio dengan sclass kustom) sebagai
	 * elemen radio, sehingga bisa diberi gaya visual berbeda (misalnya warna atau ukuran font
	 * berbeda untuk panel konfigurasi).</p>
	 *
	 * <p><b>Cara kerja.</b> Guard null pada radiogroup, membuat Criteria, menambahkan criterion,
	 * mengambil list, mengurutkan, lalu memanggil
	 * {@link #insertRadioItemsMyConfig(Radiogroup, String, List)}.</p>
	 *
	 * @param radiogroup radiogroup yang akan diisi; bila null, tidak melakukan apa pun
	 * @param property   properti label radio
	 * @param clazz      kelas entitas sumber
	 * @param criterions filter Hibernate; varargs
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void insertRadioMyConfig(Radiogroup radiogroup, String property, Class<?> clazz,
			Criterion... criterions) {
		if (radiogroup == null)
			return;
		Criteria criteria = HibernateUtil.currentSession().createCriteria(clazz);
		for (Criterion criterion : criterions) {
			criteria.add(criterion);
		}
		List list = ConstantValues.simpleList(criteria, clazz);
		try {
			Collections.sort(list);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17377");

		}
		insertRadioItemsMyConfig(radiogroup, property, list);
	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan radio item bertipe {@code MyRadioConfig}
	 * (tanpa filter kriteria) — seluruh data entitas dimuat.
	 *
	 * <p><b>Cara kerja.</b> Guard null, Criteria tanpa filter, ambil list, urutkan, lalu
	 * {@link #insertRadioItemsMyConfig(Radiogroup, String, List)}.</p>
	 *
	 * @param radiogroup radiogroup yang akan diisi; bila null, tidak melakukan apa pun
	 * @param property   properti label radio
	 * @param clazz      kelas entitas sumber
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void insertRadioMyConfig(Radiogroup radiogroup, String property, Class<?> clazz) {
		if (radiogroup == null)
			return;
		Criteria criteria = HibernateUtil.currentSession().createCriteria(clazz);

		List list = ConstantValues.simpleList(criteria, clazz);
		try {
			Collections.sort(list);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17403");

		}
		insertRadioItemsMyConfig(radiogroup, property, list);
	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan seluruh entitas dan menambahkan satu item
	 * {@code null} di akhir sebagai pilihan "Semua", lalu memilihnya secara otomatis.
	 *
	 * <p><b>Tujuan.</b> Untuk panel filter di mana pengguna dapat memilih "Semua" sebagai
	 * nilai default sebelum mempersempit ke entitas tertentu.</p>
	 *
	 * <p><b>Cara kerja.</b> Guard null, ambil seluruh data, urutkan, tambahkan {@code null}
	 * ke list, panggil {@link #insertRadioItems(Radiogroup, String, List)}, lalu pilih
	 * item null via {@link #selectRadioItem(Radiogroup, Object)}.</p>
	 *
	 * @param radiogroup radiogroup yang akan diisi; bila null, tidak melakukan apa pun
	 * @param property   properti label radio
	 * @param clazz      kelas entitas sumber
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void insertRadioDanSemua(Radiogroup radiogroup, String property, Class<?> clazz) {
		if (radiogroup == null)
			return;
		Criteria criteria = HibernateUtil.currentSession().createCriteria(clazz);

		List list = ConstantValues.simpleList(criteria, clazz);
		try {
			Collections.sort(list);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17433");

		}
		list.add(null);
		insertRadioItems(radiogroup, property, list);

		Common.selectRadioItem(radiogroup, null);
	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan seluruh entitas, deskripsi kustom, dan item "Semua"
	 * yang dipilih otomatis.
	 *
	 * <p><b>Cara kerja.</b> Guard null, ambil data, urutkan, tambahkan {@code null}, panggil
	 * {@link #insertRadioItems(Radiogroup, String, String, List)}, lalu pilih null.</p>
	 *
	 * @param radiogroup  radiogroup yang akan diisi; bila null, tidak melakukan apa pun
	 * @param property    properti label radio
	 * @param keterangan  keterangan/deskripsi ditampilkan di radio
	 * @param clazz       kelas entitas sumber
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void insertRadioDanSemua(Radiogroup radiogroup, String property, String keterangan, Class<?> clazz) {
		if (radiogroup == null)
			return;
		Criteria criteria = HibernateUtil.currentSession().createCriteria(clazz);

		List list = ConstantValues.simpleList(criteria, clazz);
		try {
			Collections.sort(list);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17463");

		}
		list.add(null);
		insertRadioItems(radiogroup, property, keterangan, list);

		Common.selectRadioItem(radiogroup, null);
	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan entitas terfilter satu kriteria, deskripsi kustom,
	 * dan item "Semua" yang dipilih otomatis.
	 *
	 * <p><b>Cara kerja.</b> Guard null, Criteria + criterion, ambil data, urutkan, tambahkan
	 * {@code null}, {@link #insertRadioItems(Radiogroup, String, String, List)}, pilih null.</p>
	 *
	 * @param radiogroup  radiogroup yang akan diisi; bila null, tidak melakukan apa pun
	 * @param property    properti label radio
	 * @param keterangan  keterangan/deskripsi ditampilkan di radio
	 * @param clazz       kelas entitas sumber
	 * @param criterion   filter Hibernate; tidak boleh null
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void insertRadioDanSemua(Radiogroup radiogroup, String property, String keterangan, Class<?> clazz,
			Criterion criterion) {
		if (radiogroup == null)
			return;
		Criteria criteria = HibernateUtil.currentSession().createCriteria(clazz);
		criteria.add(criterion);

		List list = ConstantValues.simpleList(criteria, clazz);
		try {
			Collections.sort(list);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17496");

		}
		list.add(null);
		insertRadioItems(radiogroup, property, keterangan, list);

		Common.selectRadioItem(radiogroup, null);
	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan entitas terfilter satu kriteria dan menambahkan
	 * item "Semua" (null) yang dipilih otomatis.
	 *
	 * <p><b>Cara kerja.</b> Guard null, Criteria + criterion, ambil data, urutkan, tambahkan
	 * {@code null}, {@link #insertRadioItems(Radiogroup, String, List)}, pilih null.</p>
	 *
	 * @param radiogroup radiogroup yang akan diisi; bila null, tidak melakukan apa pun
	 * @param property   properti label radio
	 * @param clazz      kelas entitas sumber
	 * @param criterion  filter Hibernate; tidak boleh null
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void insertRadioDanSemua(Radiogroup radiogroup, String property, Class<?> clazz,
			Criterion criterion) {
		if (radiogroup == null)
			return;
		Criteria criteria = HibernateUtil.currentSession().createCriteria(clazz);
		criteria.add(criterion);

		List list = ConstantValues.simpleList(criteria, clazz);
		try {
			Collections.sort(list);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17528");

		}
		list.add(null);
		insertRadioItems(radiogroup, property, list);

		Common.selectRadioItem(radiogroup, null);
	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan seluruh entitas dan menerapkan style CSS inline
	 * kustom pada setiap {@code Radio} yang dibuat.
	 *
	 * <p><b>Cara kerja.</b> Guard null, Criteria tanpa filter, ambil data, urutkan, lalu
	 * {@link #insertRadioItems(Radiogroup, String, List, String)} dengan parameter style.</p>
	 *
	 * @param radiogroup radiogroup yang akan diisi; bila null, tidak melakukan apa pun
	 * @param property   properti label radio
	 * @param clazz      kelas entitas sumber
	 * @param style      string CSS inline yang diterapkan pada setiap Radio
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void insertRadio(Radiogroup radiogroup, String property, Class<?> clazz, String style) {
		if (radiogroup == null)
			return;
		Criteria criteria = HibernateUtil.currentSession().createCriteria(clazz);

		List list = ConstantValues.simpleList(criteria, clazz);
		try {
			Collections.sort(list);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17558");

		}
		insertRadioItems(radiogroup, property, list, style);
	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan seluruh entitas dan deskripsi tambahan, tanpa filter.
	 *
	 * <p><b>Cara kerja.</b> Guard null, ambil seluruh data, urutkan, lalu
	 * {@link #insertRadioItems(Radiogroup, String, String, List)}.</p>
	 *
	 * @param radiogroup radiogroup yang akan diisi; bila null, tidak melakukan apa pun
	 * @param property   properti label radio
	 * @param deskripsi  keterangan/deskripsi tambahan yang ditampilkan
	 * @param clazz      kelas entitas sumber
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void insertRadio(Radiogroup radiogroup, String property, String deskripsi, Class<?> clazz) {
		if (radiogroup == null)
			return;
		Criteria criteria = HibernateUtil.currentSession().createCriteria(clazz);

		List list = ConstantValues.simpleList(criteria, clazz);
		try {
			Collections.sort(list);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17584");

		}
		insertRadioItems(radiogroup, property, deskripsi, list);
	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan label multi-properti (beberapa nama properti
	 * digabung dengan " - "), difilter oleh Criterion varargs.
	 *
	 * <p><b>Tujuan.</b> Digunakan saat label radio perlu menggabungkan lebih dari satu
	 * properti, misalnya "Kode - Nama" untuk menampilkan representasi yang lebih informatif.</p>
	 *
	 * <p><b>Cara kerja.</b> Membuat Criteria, menambahkan criterions, mengambil list,
	 * mengurutkan, lalu memanggil
	 * {@link #insertRadioItems(Radiogroup, String[], List)}.</p>
	 *
	 * @param radiogroup radiogroup yang akan diisi
	 * @param properties larik nama properti yang digabung untuk label radio
	 * @param clazz      kelas entitas sumber
	 * @param criterions filter Hibernate; varargs, boleh berisi null
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void insertRadio(Radiogroup radiogroup, String[] properties, Class clazz, Criterion... criterions) {
		Criteria criteria = HibernateUtil.currentSession().createCriteria(clazz);
		if (criterions != null) {
			for (Criterion criterion : criterions) {
				if (criterion != null) {
					criteria.add(criterion);
				}
			}
		}
		List list = ConstantValues.simpleList(criteria, clazz);
		try {
			Collections.sort(list);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17619");

		}
		insertRadioItems(radiogroup, properties, list);
	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan label multi-properti, difilter satu {@code Criterion}.
	 *
	 * <p><b>Cara kerja.</b> Criteria + criterion, ambil list, urutkan, lalu
	 * {@link #insertRadioItems(Radiogroup, String[], List)}.</p>
	 *
	 * @param radiogroup radiogroup yang akan diisi
	 * @param properties larik nama properti yang digabung untuk label radio
	 * @param clazz      kelas entitas sumber
	 * @param criterion  filter Hibernate; tidak boleh null
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void insertRadio(Radiogroup radiogroup, String[] properties, Class<?> clazz, Criterion criterion) {
		Criteria criteria = HibernateUtil.currentSession().createCriteria(clazz);
		criteria.add(criterion);
		List list = ConstantValues.simpleList(criteria, clazz);
		try {
			Collections.sort(list);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17643");

		}
		insertRadioItems(radiogroup, properties, list);
	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan label multi-properti dan deskripsi kustom,
	 * difilter oleh Criterion varargs.
	 *
	 * <p><b>Cara kerja.</b> Criteria + criterions, ambil list, urutkan, lalu
	 * {@link #insertRadioItems(Radiogroup, String[], String, List)}.</p>
	 *
	 * @param radiogroup radiogroup yang akan diisi
	 * @param properties larik nama properti untuk label
	 * @param deskripsi  deskripsi kustom ditampilkan di radio
	 * @param clazz      kelas entitas sumber
	 * @param criterions filter Hibernate; varargs
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void insertRadio(Radiogroup radiogroup, String[] properties, String deskripsi, Class clazz,
			Criterion... criterions) {
		Criteria criteria = HibernateUtil.currentSession().createCriteria(clazz);
		for (Criterion criterion : criterions) {
			criteria.add(criterion);
		}
		List list = ConstantValues.simpleList(criteria, clazz);
		try {
			Collections.sort(list);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17672");

		}
		insertRadioItems(radiogroup, properties, deskripsi, list);
	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan label multi-properti, deskripsi kustom, item "Semua"
	 * berlabel default ("Semua"), dan pilihan otomatis ke "Semua".
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@link #insertRadioDanSemua(Radiogroup, String[], String, Class, String, Criterion...)}
	 * dengan {@code labelTidakDipilih = "Semua"}.</p>
	 *
	 * @param radiogroup radiogroup yang akan diisi
	 * @param properties larik nama properti untuk label
	 * @param deskripsi  deskripsi kustom
	 * @param clazz      kelas entitas sumber
	 * @param criterions filter Hibernate; varargs
	 */
	@SuppressWarnings({ "rawtypes" })
	public static void insertRadioDanSemua(Radiogroup radiogroup, String[] properties, String deskripsi, Class clazz,
			Criterion... criterions) {
		insertRadioDanSemua(radiogroup, properties, deskripsi, clazz, "Semua", criterions);
	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan label multi-properti, deskripsi kustom, item
	 * "tidak dipilih" dengan label yang dapat dikustomisasi, dan pilihan otomatis ke item tersebut.
	 *
	 * <p><b>Tujuan.</b> Varian paling lengkap dari keluarga insertRadioDanSemua — memungkinkan
	 * label item "tidak ada pilihan" diganti dari default "Semua" menjadi kata lain seperti
	 * "Semua Prodi", "Tidak Ada", dsb.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Membuat Criteria dan menambahkan semua criterions.</li>
	 *   <li>Mengambil list via {@code ConstantValues.simpleList}, mengurutkan.</li>
	 *   <li>Menambahkan {@code null} ke list untuk item "tidak dipilih".</li>
	 *   <li>Memanggil {@link #insertRadioItems(Radiogroup, String[], String, List, String)}
	 *       dengan {@code labelTidakDipilih}.</li>
	 *   <li>Memilih item null secara otomatis via {@link #selectRadioItem}.</li>
	 * </ol>
	 * </p>
	 *
	 * @param radiogroup          radiogroup yang akan diisi
	 * @param properties          larik nama properti untuk label radio
	 * @param deskripsi           deskripsi kustom ditampilkan di radio
	 * @param clazz               kelas entitas sumber
	 * @param labelTidakDipilih   label item null (misalnya "Semua", "Semua Prodi")
	 * @param criterions          filter Hibernate; varargs
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void insertRadioDanSemua(Radiogroup radiogroup, String[] properties, String deskripsi, Class clazz,
			String labelTidakDipilih, Criterion... criterions) {
		Criteria criteria = HibernateUtil.currentSession().createCriteria(clazz);
		for (Criterion criterion : criterions) {
			criteria.add(criterion);
		}

		List list = ConstantValues.simpleList(criteria, clazz);
		try {
			Collections.sort(list);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17735");

		}
		list.add(null);
		insertRadioItems(radiogroup, properties, deskripsi, list, labelTidakDipilih);

		Common.selectRadioItem(radiogroup, null);

	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan label multi-properti, seluruh data entitas (tanpa filter).
	 *
	 * <p><b>Cara kerja.</b> Guard null, Criteria tanpa filter, ambil data, urutkan, lalu
	 * {@link #insertRadioItems(Radiogroup, String[], List)}.</p>
	 *
	 * @param radiogroup radiogroup yang akan diisi; bila null, tidak melakukan apa pun
	 * @param properties larik nama properti untuk label radio
	 * @param clazz      kelas entitas sumber
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void insertRadio(Radiogroup radiogroup, String[] properties, Class<?> clazz) {
		if (radiogroup == null)
			return;
		Criteria criteria = HibernateUtil.currentSession().createCriteria(clazz);

		List list = ConstantValues.simpleList(criteria, clazz);
		try {
			Collections.sort(list);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17764");

		}
		insertRadioItems(radiogroup, properties, list);
	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan label multi-properti, seluruh data entitas, item "Semua"
	 * di akhir, dan pilihan otomatis ke "Semua".
	 *
	 * <p><b>Cara kerja.</b> Guard null, Criteria tanpa filter, ambil data, urutkan, tambahkan null,
	 * {@link #insertRadioItems(Radiogroup, String[], List)}, pilih null.</p>
	 *
	 * @param radiogroup radiogroup yang akan diisi; bila null, tidak melakukan apa pun
	 * @param properties larik nama properti untuk label radio
	 * @param clazz      kelas entitas sumber
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void insertRadioDanSemua(Radiogroup radiogroup, String[] properties, Class<?> clazz) {
		if (radiogroup == null)
			return;
		Criteria criteria = HibernateUtil.currentSession().createCriteria(clazz);

		List list = ConstantValues.simpleList(criteria, clazz);
		try {
			Collections.sort(list);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17790");

		}
		list.add(null);
		insertRadioItems(radiogroup, properties, list);
		Common.selectRadioItem(radiogroup, null);

	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan label multi-properti, deskripsi kustom, seluruh data
	 * entitas (tanpa filter).
	 *
	 * <p><b>Cara kerja.</b> Guard null, Criteria tanpa filter, ambil data, urutkan, lalu
	 * {@link #insertRadioItems(Radiogroup, String[], String, List)}.</p>
	 *
	 * @param radiogroup radiogroup yang akan diisi; bila null, tidak melakukan apa pun
	 * @param properties larik nama properti untuk label radio
	 * @param deskripsi  deskripsi kustom ditampilkan di radio
	 * @param clazz      kelas entitas sumber
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void insertRadio(Radiogroup radiogroup, String[] properties, String deskripsi, Class<?> clazz) {
		if (radiogroup == null)
			return;
		Criteria criteria = HibernateUtil.currentSession().createCriteria(clazz);

		List list = ConstantValues.simpleList(criteria, clazz);
		try {
			Collections.sort(list);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17820");

		}
		insertRadioItems(radiogroup, properties, deskripsi, list);
	}

	/**
	 * Metode inti pengisi {@code Radiogroup} ZK dari daftar objek Hibernate menggunakan
	 * satu nama properti sebagai label.
	 *
	 * <p><b>Tujuan.</b> Mengosongkan radiogroup yang ada, lalu membuat dan menambahkan
	 * item {@code Radio} satu per satu dari daftar objek yang diberikan. Metode ini adalah
	 * target akhir dari semua overload {@code insertRadio(...)} yang menggunakan satu properti.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Memanggil {@link Common#clear(Component)} untuk menghapus seluruh anak Radiogroup.</li>
	 *   <li>Bila list kosong, langsung kembali (tidak ada Radio yang dibuat).</li>
	 *   <li>Bila elemen pertama null, membuat satu Radio berlabel "Semua" dengan value null.</li>
	 *   <li>Bila tidak null, mengambil {@code ClassMetadata} Hibernate dari kelas elemen
	 *       pertama (kecuali {@code CommonVO}) untuk mengakses nilai properti via refleksi.</li>
	 *   <li>Untuk tiap objek dalam list:
	 *     <ul>
	 *       <li>Bila null → Radio berlabel "Semua", value null.</li>
	 *       <li>Bila {@code CommonVO} → label dari {@code getName()}, value dari {@code getId()}.</li>
	 *       <li>Selainnya → label dari {@code metadata.getPropertyValue(o, property)} dengan
	 *           fallback ke {@code metadata.getIdentifier(...)} bila properti tidak ditemukan.
	 *           Objek disimpan ke {@code radio.setAttribute("value", o)}.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Setiap Radio ditambahkan ke combo via {@code combo.appendChild(radio)}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Catatan pemeliharaan.</b> Menggunakan {@code @SuppressWarnings("deprecation")} karena
	 * {@code EntityMode.POJO} dan {@code ClassMetadata.getPropertyValue} sudah deprecated di
	 * Hibernate versi baru, namun masih digunakan karena codebase masih di Hibernate versi lama.</p>
	 *
	 * @param combo    Radiogroup ZK yang akan diisi; tidak boleh null
	 * @param property nama properti Hibernate yang nilainya digunakan sebagai label radio;
	 *                 string kosong ({@code ""}) menggunakan {@code toString()} objek
	 * @param items    daftar objek Hibernate (atau null untuk item "Semua") yang akan dirender
	 */
	@SuppressWarnings("deprecation")
	public static void insertRadioItems(Radiogroup combo, String property, List<?> items) {
		Common.clear(combo);
		if (items.size() == 0)
			return;
		if (items.get(0) == null) {
			Radio radio = new Radio();
			radio.setLabel("Semua");
			radio.setValue(null);
			combo.appendChild(radio);
		} else {
			Class<? extends Object> clazz = items.get(0).getClass();
			ClassMetadata metadata = null;
			if (!clazz.equals(CommonVO.class)) {
				metadata = HibernateUtil.getClassMetadata(clazz);
			}
			for (Object o : items) {
				Radio radio = new Radio();
				if (o == null) {
					radio.setLabel("Semua");
					radio.setValue(null);
				} else if (o instanceof CommonVO) {
					CommonVO oo = (CommonVO) o;
					radio.setLabel(oo.getName());
					radio.setValue(oo.getId());
				} else {
					try {
						try {
							radio.setLabel(property.equals("") ? o + ""
									: "" + metadata.getPropertyValue(o, property, EntityMode.POJO));
						} catch (Exception e) {
							radio.setLabel(
									property.equals("") ? o + "" : "" + metadata.getIdentifier(o, EntityMode.POJO));
						}
						radio.setAttribute("value", o);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:17898");

					}
				}
				combo.appendChild(radio);
			}
		}
	}

	/**
	 * Metode inti pengisi {@code Radiogroup} ZK menggunakan item {@code MyRadioConfig}
	 * (radio dengan sclass kustom) dari daftar objek Hibernate.
	 *
	 * <p><b>Perbedaan dari {@link #insertRadioItems(Radiogroup, String, List)}.</b>
	 * Setiap item yang dibuat adalah {@code MyRadioConfig} (bukan {@code Radio} standar),
	 * yang membawa sclass atau konfigurasi visual tambahan untuk panel konfigurasi sistem.</p>
	 *
	 * <p><b>Cara kerja.</b> Sama dengan {@link #insertRadioItems(Radiogroup, String, List)},
	 * namun menggunakan {@code new MyRadioConfig()} alih-alih {@code new Radio()}.
	 * Label dan value diisi dengan logika yang sama (null→Semua, CommonVO→getName/getId,
	 * Hibernate entity→getPropertyValue).</p>
	 *
	 * @param combo    Radiogroup ZK yang akan diisi; tidak boleh null
	 * @param property nama properti Hibernate untuk label; string kosong pakai toString()
	 * @param items    daftar objek yang akan dirender sebagai MyRadioConfig
	 */
	@SuppressWarnings("deprecation")
	public static void insertRadioItemsMyConfig(Radiogroup combo, String property, List<?> items) {
		Common.clear(combo);
		if (items.size() == 0)
			return;
		if (items.get(0) == null) {
			MyRadioConfig radio = new MyRadioConfig();
			radio.setLabel("Semua");
			radio.setValue(null);
			combo.appendChild(radio);
		} else {
			Class<? extends Object> clazz = items.get(0).getClass();
			ClassMetadata metadata = null;
			if (!clazz.equals(CommonVO.class)) {
				metadata = HibernateUtil.getClassMetadata(clazz);
			}
			for (Object o : items) {
				MyRadioConfig radio = new MyRadioConfig();
				if (o == null) {
					radio.setLabel("Semua");
					radio.setValue(null);
				} else if (o instanceof CommonVO) {
					CommonVO oo = (CommonVO) o;
					radio.setLabel(oo.getName());
					radio.setValue(oo.getId());
				} else {
					try {

						radio.setLabel(property.equals("") ? o + ""
								: "" + metadata.getPropertyValue(o, property, EntityMode.POJO));
					} catch (Exception e) {
						radio.setLabel(property.equals("") ? o + "" : "" + metadata.getIdentifier(o, EntityMode.POJO));
					}
					radio.setAttribute("value", o);
				}
				combo.appendChild(radio);
			}
		}
	}

	/**
	 * Mengisi {@code Radiogroup} ZK khusus untuk daftar {@code CommonVO} — objek Value Object
	 * yang memiliki pasangan {@code getName()}/{@code getId()} tanpa perlu Hibernate ClassMetadata.
	 *
	 * <p><b>Tujuan.</b> Digunakan saat sumber data bukan entitas Hibernate murni melainkan
	 * VO yang dibangun secara manual (misalnya dari native SQL atau logika bisnis), sehingga
	 * tidak perlu metadata refleksi Hibernate.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Guard null pada combo.</li>
	 *   <li>{@link Common#clear(Component)} membersihkan isi lama.</li>
	 *   <li>Mengiterasi items; bila elemen adalah {@code CommonVO}, membuat Radio dengan
	 *       label dari {@code getName()} dan value (via setAttribute) dari objek VO itu sendiri.</li>
	 *   <li>Menambahkan Radio ke combo.</li>
	 * </ol>
	 * </p>
	 *
	 * @param combo Radiogroup ZK yang akan diisi; bila null, tidak melakukan apa pun
	 * @param items daftar CommonVO yang akan dirender sebagai Radio
	 */
	public static void insertRadioItemsCommonVO(Radiogroup combo, List<?> items) {
		if (combo == null) {
			return;
		}
		Common.clear(combo);
		for (Object o : items) {
			Radio radio = new Radio();
			if (o instanceof CommonVO) {
				CommonVO oo = (CommonVO) o;
				radio.setLabel(oo.getName());
				radio.setAttribute("value", oo);
			}
			combo.appendChild(radio);
		}
	}

	/**
	 * Metode inti pengisi {@code Radiogroup} ZK dari daftar objek dengan style CSS kustom
	 * yang diterapkan pada setiap Radio.
	 *
	 * <p><b>Perbedaan dari {@link #insertRadioItems(Radiogroup, String, List)}.</b>
	 * Parameter {@code style} (string CSS inline) diterapkan ke setiap Radio melalui
	 * {@code radio.setStyle(style)}, memungkinkan penyesuaian visual per-radio.</p>
	 *
	 * <p><b>Cara kerja.</b> Sama dengan varian tanpa style, dengan tambahan pemanggilan
	 * {@code radio.setStyle(style)} setelah label diset. Untuk item null (Semua), style juga
	 * diterapkan.</p>
	 *
	 * @param combo    Radiogroup ZK yang akan diisi; tidak boleh null
	 * @param property nama properti Hibernate untuk label
	 * @param items    daftar objek yang akan dirender sebagai Radio
	 * @param style    string CSS inline yang diterapkan ke setiap Radio (misalnya {@code "color:red;"})
	 */
	@SuppressWarnings("deprecation")
	public static void insertRadioItems(Radiogroup combo, String property, List<?> items, String style) {
		Common.clear(combo);
		if (items.size() == 0)
			return;
		if (items.get(0) == null) {
			Radio radio = new Radio();
			radio.setLabel("Semua");
			radio.setValue(null);
			combo.appendChild(radio);
		} else {
			Class<? extends Object> clazz = items.get(0).getClass();
			ClassMetadata metadata = null;
			if (!clazz.equals(CommonVO.class)) {
				metadata = HibernateUtil.getClassMetadata(clazz);
			}
			for (Object o : items) {
				Radio radio = new Radio();
				if (o == null) {
					radio.setLabel("Semua");
					radio.setValue(null);
				} else if (o instanceof CommonVO) {
					CommonVO oo = (CommonVO) o;
					radio.setLabel(oo.getName());
					radio.setValue(oo.getId());
				} else {
					String label = "";
					try {
						label = (property.equals("") ? o + ""
								: "" + metadata.getPropertyValue(o, property, EntityMode.POJO));
					} catch (Exception e) {
						label = (property.equals("") ? o + "" : "" + metadata.getIdentifier(o, EntityMode.POJO));
					}
					radio.setLabel(label);
					radio.setAttribute("value", o);
				}
				combo.appendChild(radio);
			}
		}
	}

	/**
	 * Metode inti pengisi {@code Radiogroup} ZK dari daftar objek dengan deskripsi kustom
	 * sebagai tambahan label.
	 *
	 * <p><b>Tujuan.</b> Digunakan saat label radio perlu menyertakan keterangan tambahan
	 * (misalnya satuan, kode, atau konteks) di luar nilai properti tunggal.</p>
	 *
	 * <p><b>Cara kerja.</b> Guard null pada combo, clear, iterasi items. Untuk entitas Hibernate:
	 * label diambil dari {@code metadata.getPropertyValue(o, property)} (null diubah ke string
	 * kosong); objek disimpan ke {@code radio.setAttribute("value", o)}. Untuk {@code CommonVO}:
	 * label dari {@code getName()}, value dari {@code getId()} via setAttribute.
	 * Parameter {@code deskripsi} tidak digunakan langsung dalam implementasi saat ini
	 * (disediakan untuk kompatibilitas / ekstensi).</p>
	 *
	 * @param combo     Radiogroup ZK yang akan diisi; bila null, tidak melakukan apa pun
	 * @param property  nama properti Hibernate untuk label
	 * @param deskripsi deskripsi kustom (disediakan untuk kompatibilitas; tidak digunakan langsung)
	 * @param items     daftar objek yang akan dirender sebagai Radio
	 */
	public static void insertRadioItems(Radiogroup combo, String property, String deskripsi, List<?> items) {
		if (combo == null) {
			return;
		}
		Common.clear(combo);
		if (items.size() == 0)
			return;
		if (items.get(0) == null) {
			Radio radio = new Radio();
			radio.setLabel("Semua");
			radio.setValue(null);
			combo.appendChild(radio);
		} else {
			Class<? extends Object> clazz = items.get(0).getClass();
			ClassMetadata metadata = null;
			if (!clazz.equals(CommonVO.class)) {
				metadata = HibernateUtil.getClassMetadata(clazz);
			}
			for (Object o : items) {
				Radio radio = new Radio();
				if (o == null) {
					radio.setLabel("Semua");
					radio.setValue(null);
				} else if (o instanceof CommonVO) {
					CommonVO oo = (CommonVO) o;
					radio.setLabel(oo.getName());
					radio.setAttribute("value", oo.getId());
				} else {

					try {
						Object myproperty = metadata.getPropertyValue(o, property, EntityMode.POJO);

						radio.setLabel(property.equals("") ? (o == null ? "" : "") + ""
								: "" + (myproperty == null ? "" : myproperty));

						radio.setAttribute("value", o);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:18114");

					}
				}
				combo.appendChild(radio);
			}
		}
	}

	/**
	 * Metode inti pengisi {@code Radiogroup} ZK dari daftar objek dengan label yang
	 * menggabungkan beberapa properti Hibernate (multi-properti).
	 *
	 * <p><b>Tujuan.</b> Digunakan saat label radio harus merangkum lebih dari satu atribut,
	 * misalnya menampilkan "NIM - Nama Mahasiswa" atau "Kode - Nama Jurusan".</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Memanggil {@link Common#clear(Component)} untuk mengosongkan radiogroup.</li>
	 *   <li>Bila list kosong, langsung kembali.</li>
	 *   <li>Bila elemen pertama null, membuat Radio "Semua".</li>
	 *   <li>Mendapatkan {@code ClassMetadata} dari kelas elemen pertama (kecuali CommonVO).</li>
	 *   <li>Untuk tiap objek: membangun label dengan mengiterasi larik {@code properties};
	 *       setiap properti diambil via {@code metadata.getPropertyValue} atau
	 *       {@code metadata.getIdentifier} (bila properti adalah "id"), nilai null/kosong dilewati,
	 *       nilai digabungkan dengan {@code " - "}. Label kosong bila semua properti null.</li>
	 *   <li>Radio disimpan dengan {@code setAttribute("value", o)} ×2 (duplikat bawaan kode lama).</li>
	 * </ol>
	 * </p>
	 *
	 * @param combo      Radiogroup ZK yang akan diisi; tidak boleh null
	 * @param properties larik nama properti yang digabungkan untuk label ("id" diperlakukan khusus)
	 * @param items      daftar objek Hibernate yang akan dirender sebagai Radio
	 */
	@SuppressWarnings("deprecation")
	public static void insertRadioItems(Radiogroup combo, String properties[], List<?> items) {
		Common.clear(combo);
		if (items.size() == 0)
			return;
		if (items.get(0) == null) {
			Radio radio = new Radio();
			radio.setLabel("Semua");
			radio.setValue(null);
			combo.appendChild(radio);
		} else {
			Class<? extends Object> clazz = items.get(0).getClass();
			ClassMetadata metadata = null;
			if (!clazz.equals(CommonVO.class)) {
				metadata = HibernateUtil.getClassMetadata(clazz);
			}
			for (Object o : items) {
				Radio radio = new Radio();
				if (o == null) {
					radio.setLabel("Semua");
					radio.setValue(null);
				} else if (o instanceof CommonVO) {
					CommonVO oo = (CommonVO) o;
					radio.setLabel(oo.getName());
					radio.setValue(oo.getId());
				} else {
					String value = "";

					for (String property : properties) {

						Object val = property.trim().equals("id") ? metadata.getIdentifier(o, EntityMode.POJO)
								: metadata.getPropertyValue(o, property, EntityMode.POJO);
						if (val == null || val.toString().trim().equals("") || val.toString().trim().equals("null")) {
							continue;
						}

						value += value.equals("") ? val : " - " + val;
					}

					radio.setLabel(value);
					radio.setAttribute("value", o);
					radio.setAttribute("value", o);
				}
				combo.appendChild(radio);
			}
		}
	}

	/**
	 * Mengisi {@code Radiogroup} ZK dengan label multi-properti dan deskripsi kustom,
	 * dengan label item null default "Semua".
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@link #insertRadioItems(Radiogroup, String[], String, List, String)} dengan
	 * {@code labelTidakDipilih = "Semua"}.</p>
	 *
	 * @param combo     Radiogroup ZK yang akan diisi
	 * @param properties larik nama properti untuk label
	 * @param deskripsi  deskripsi kustom
	 * @param items     daftar objek yang akan dirender
	 */
	public static void insertRadioItems(Radiogroup combo, String properties[], String deskripsi, List<?> items) {
		insertRadioItems(combo, properties, deskripsi, items, "Semua");
	}

	/**
	 * Metode inti pengisi {@code Radiogroup} ZK dari daftar objek dengan label multi-properti,
	 * deskripsi kustom, dan label item null yang dapat dikustomisasi.
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Memanggil {@link Common#clear(Component)} untuk mengosongkan radiogroup.</li>
	 *   <li>Bila list kosong, langsung kembali.</li>
	 *   <li>Bila elemen pertama null, membuat Radio dengan {@code labelTidakDipilih}, value null.</li>
	 *   <li>Mendapatkan {@code ClassMetadata} dari kelas elemen pertama (kecuali CommonVO).</li>
	 *   <li>Setiap Radio mendapat {@code setAttribute("value", o)} di awal (sebelum kondisional).</li>
	 *   <li>Untuk tiap objek:
	 *     <ul>
	 *       <li>Null → label {@code labelTidakDipilih}, value null.</li>
	 *       <li>CommonVO → label dari {@code getName()}, value dari {@code getId()} via setAttribute.</li>
	 *       <li>Hibernate entity → membangun label dari larik properties (tiap properti diambil
	 *           via {@code metadata.getIdentifier} atau {@code metadata.getPropertyValue}, nilai
	 *           null/kosong dilewati, digabungkan dengan " - "). Error per-properti ditangkap
	 *           secara diam-diam.</li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 * </p>
	 *
	 * @param combo               Radiogroup ZK yang akan diisi; tidak boleh null
	 * @param properties          larik nama properti untuk label
	 * @param deskripsi           deskripsi kustom (disediakan untuk kompatibilitas)
	 * @param items               daftar objek yang akan dirender sebagai Radio
	 * @param labelTidakDipilih   label item null; misalnya "Semua", "Semua Jurusan"
	 */
	@SuppressWarnings("deprecation")
	public static void insertRadioItems(Radiogroup combo, String properties[], String deskripsi, List<?> items,
			String labelTidakDipilih) {
		Common.clear(combo);
		if (items.size() == 0)
			return;
		if (items.get(0) == null) {
			Radio radio = new Radio();
			radio.setLabel(labelTidakDipilih);
			radio.setValue(null);
			combo.appendChild(radio);
		} else {
			Class<? extends Object> clazz = items.get(0).getClass();
			ClassMetadata metadata = null;
			if (!clazz.equals(CommonVO.class)) {
				metadata = HibernateUtil.getClassMetadata(clazz);
			}
			for (Object o : items) {
				Radio radio = new Radio();
				radio.setAttribute("value", o);
				if (o == null) {
					radio.setLabel(labelTidakDipilih);
					radio.setValue(null);
				} else if (o instanceof CommonVO) {
					CommonVO oo = (CommonVO) o;
					radio.setLabel(oo.getName());
					radio.setAttribute("value", oo.getId());
				} else {

					try {
						String value = "";

						for (String property : properties) {

							try {
								Object val = property.trim().equals("id") ? metadata.getIdentifier(o, EntityMode.POJO)
										: metadata.getPropertyValue(o, property, EntityMode.POJO);
								if (val == null || val.toString().trim().equals("")) {
									continue;
								}

								value += value.equals("") ? val : " - " + val;
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:18285");
//								tampilErrorJikaAdmin(e);
							}
						}

						radio.setLabel(value);

					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:18292");
//						tampilErrorJikaAdmin(e);
					}
				}
				combo.appendChild(radio);
			}
		}
	}

	/**
	 * Memilih item {@code Radio} di dalam {@code Radiogroup} yang nilainya cocok dengan
	 * {@code value}, dengan opsi menambahkan data baru bila tidak ditemukan.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@link #selectRadioItem(Radiogroup, Object, boolean)} dengan urutan parameter yang
	 * dibalik — varian ini memungkinkan penulisan yang lebih natural saat flag datang
	 * sebelum objek combo.</p>
	 *
	 * @param jikaDataTidakDitemukanDitambahkan {@code true} untuk menambahkan item baru bila
	 *                                          value tidak ditemukan di radiogroup
	 * @param combo                            radiogroup yang dipilih; bila null, diabaikan
	 * @param value                            nilai yang dicari dan dipilih
	 */
	public static void selectRadioItem(boolean jikaDataTidakDitemukanDitambahkan, Radiogroup combo, Object value) {
		selectRadioItem(combo, value, jikaDataTidakDitemukanDitambahkan);
	}

	/**
	 * Memilih item {@code Radio} yang nilainya cocok dengan {@code value} tanpa menambahkan
	 * data baru bila tidak ditemukan.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@link #selectRadioItem(Radiogroup, Object, boolean)} dengan
	 * {@code jikaDataTidakDitemukanDitambahkan = false}.</p>
	 *
	 * @param combo radiogroup yang dipilih; bila null, diabaikan
	 * @param value nilai yang dicari dan dipilih; null untuk memilih item "Semua"
	 */
	public static void selectRadioItem(Radiogroup combo, Object value) {
		selectRadioItem(combo, value, false);
	}

	/**
	 * Memilih item {@code Radio} di dalam {@code Radiogroup} yang nilainya cocok dengan
	 * {@code value}, dengan opsi menambahkan Radio baru bila tidak ditemukan.
	 *
	 * <p><b>Tujuan.</b> Metode inti pemilihan Radio — digunakan saat nilai filter yang
	 * disimpan (misalnya dari sesi pengguna) perlu direstorasi ke radiogroup yang sudah
	 * diisi, agar pilihan terakhir pengguna tetap terjaga saat halaman di-refresh.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Guard null pada combo; bila null, langsung kembali.</li>
	 *   <li>Memanggil {@code combo.setSelectedIndex(-1)} untuk membatalkan pilihan aktif.</li>
	 *   <li>Bila {@code value == null}: mencari Radio dengan {@code getAttribute("value") == null}
	 *       dan memilihnya; bila tidak ditemukan, set selectedItem null (tidak ada pilihan).</li>
	 *   <li>Bila {@code value != null}: mengambil {@code ClassMetadata} dari kelas value untuk
	 *       mendapatkan ID identitas (kecuali String, Integer, Double, Date yang dibandingkan
	 *       langsung via toString).</li>
	 *   <li>Mengiterasi semua Radio dan membandingkan value:
	 *     <ul>
	 *       <li>String: perbandingan case-insensitive trim.</li>
	 *       <li>Integer: perbandingan string.</li>
	 *       <li>Hibernate entity: bandingkan ID identitas dengan ID objek di radio.</li>
	 *       <li>CommonVO: bandingkan getId().</li>
	 *     </ul>
	 *   </li>
	 *   <li>Bila tidak ditemukan dan {@code jikaDataTidakDitemukanDitambahkan == true},
	 *       membuat Radio baru dari value dan menambahkannya ke combo sebelum dipilih.</li>
	 * </ol>
	 * </p>
	 *
	 * @param combo                            radiogroup yang dipilih; bila null, tidak melakukan apa pun
	 * @param value                            nilai yang dicari; null untuk memilih item "Semua"
	 * @param jikaDataTidakDitemukanDitambahkan {@code true} untuk menambahkan Radio baru bila tidak ditemukan
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	public static void selectRadioItem(Radiogroup combo, Object value, boolean jikaDataTidakDitemukanDitambahkan) {

		try {

			if (combo == null) {
				return;
			}
			combo.setSelectedIndex(-1);
			if (value == null) {
				List<Radio> list = combo.getItems();
				for (Radio radio : list) {
					if (radio.getAttribute("value") == null) {
						combo.setSelectedItem(radio);
						return;
					}
				}
				combo.setSelectedItem(null);
				return;
			}

			List<Radio> list = combo.getItems();
			ClassMetadata metadata = HibernateUtil.getClassMetadata(value.getClass());
			Serializable identityvalue = null;
			if (metadata != null && !(value instanceof String) && !(value instanceof Integer)
					&& !(value instanceof Double) && !(value instanceof Date)) {
				identityvalue = metadata.getIdentifier(value, EntityMode.POJO);
			}

			for (Radio radio : list) {
				if ((value instanceof String)) {
					if (radio.getAttribute("value") != null && value.toString().trim()
							.equalsIgnoreCase(radio.getAttribute("value").toString().trim())) {
						combo.setSelectedItem(radio);
					}
				} else if ((value instanceof Boolean) || (value instanceof String) || (value instanceof Integer)
						|| (value instanceof Double) || (value instanceof Date)) {
					if (radio.getAttribute("value") != null && value.equals(radio.getAttribute("value"))) {
						combo.setSelectedItem(radio);
					}
				} else {
					try {
						Serializable id = metadata.getIdentifier(radio.getAttribute("value"), EntityMode.POJO);
						if (identityvalue.equals(id)) {
							combo.setSelectedItem(radio);
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:18414");
					}
				}
			}

			if (jikaDataTidakDitemukanDitambahkan) {
				if ((combo.getSelectedItem() == null || combo.getSelectedItem().getValue() == null) && value != null
						&& !(value instanceof String)) {
					Radio radio = new Radio(value.toString());
					radio.setAttribute("value", value);

					combo.appendChild(radio);
					combo.setSelectedItem(radio);
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menghitung jarak antara dua koordinat geografis (lintang/bujur) dalam satuan kilometer
	 * (default).
	 *
	 * <p><b>Tujuan.</b> Digunakan untuk fitur absensi berbasis lokasi GPS (geofencing),
	 * menentukan apakah perangkat mahasiswa/karyawan berada dalam radius lokasi yang ditentukan.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@link #getDistanceBetweenPointsNew(double, double, double, double, String)} dengan
	 * {@code unit = "kilometers"}.</p>
	 *
	 * @param latitude1  lintang titik pertama (derajat desimal)
	 * @param longitude1 bujur titik pertama (derajat desimal)
	 * @param latitude2  lintang titik kedua (derajat desimal)
	 * @param longitude2 bujur titik kedua (derajat desimal)
	 * @return jarak dalam kilometer antara dua koordinat
	 */
	public static double getDistanceBetweenPointsNew(double latitude1, double longitude1, double latitude2,
			double longitude2) {
		return getDistanceBetweenPointsNew(latitude1, longitude1, latitude2, longitude2, "kilometers");
	}

	/**
	 * Menghitung jarak antara dua koordinat geografis dalam satuan yang ditentukan
	 * (kilometers atau miles).
	 *
	 * <p><b>Cara kerja.</b> Menggunakan rumus trigonometri bola (spherical law of cosines):
	 * {@code theta = lon1 - lon2}, lalu:
	 * {@code dist = 60 * 1.1515 * (180/PI) * acos(sin(lat1)*sin(lat2) + cos(lat1)*cos(lat2)*cos(theta))}.
	 * Bila {@code unit = "miles"} dikembalikan apa adanya; bila {@code unit = "kilometers"}
	 * dikalikan 1.609344; selainnya mengembalikan 0.
	 * Koordinat dikonversi ke radian dalam tiap fungsi trigonometri.</p>
	 *
	 * @param latitude1  lintang titik pertama (derajat desimal)
	 * @param longitude1 bujur titik pertama (derajat desimal)
	 * @param latitude2  lintang titik kedua (derajat desimal)
	 * @param longitude2 bujur titik kedua (derajat desimal)
	 * @param unit       satuan jarak: {@code "kilometers"} atau {@code "miles"}
	 * @return jarak dalam satuan yang diminta; 0 bila unit tidak dikenal
	 */
	public static double getDistanceBetweenPointsNew(double latitude1, double longitude1, double latitude2,
			double longitude2, String unit) {
		double theta = longitude1 - longitude2;
		double distance = 60 * 1.1515 * (180 / Math.PI)
				* Math.acos(Math.sin(latitude1 * (Math.PI / 180)) * Math.sin(latitude2 * (Math.PI / 180))
						+ Math.cos(latitude1 * (Math.PI / 180)) * Math.cos(latitude2 * (Math.PI / 180))
								* Math.cos(theta * (Math.PI / 180)));

		if (unit.equals("miles")) {
			return distance;
		} else if (unit.equals("kilometers")) {
			return distance * 1.609344;
		} else {
			return 0;
		}
	}

	// Tambahkan 'final' karena nilainya tidak akan berubah
	public static final String pattern = "###########################";

	// Ubah menjadi ThreadLocal dan pindahkan setting dari blok static
	public static final ThreadLocal<DecimalFormat> decimalFormat = new ThreadLocal<DecimalFormat>() {
		@Override
		protected DecimalFormat initialValue() {
			DecimalFormat df = new DecimalFormat(pattern);
			df.setMaximumFractionDigits(8);
			df.setMinimumIntegerDigits(1);
			return df;
		}
	};

	/**
	 * Memecah string berisi angka yang dipisah koma menjadi larik {@code Long[]}.
	 *
	 * <p><b>Tujuan.</b> Digunakan saat ID entitas dikirim dalam satu string parameter HTTP
	 * (misalnya checkbox multi-pilih: {@code "1,2,3,45"}) dan perlu diubah ke larik Long
	 * untuk diproses dalam query Hibernate {@code Restrictions.in(...)}</p>
	 *
	 * <p><b>Cara kerja.</b> Memecah {@code string} dengan pemisah koma, membuat larik
	 * {@code Long[]} dengan ukuran yang sama, lalu mengisi tiap elemen dengan
	 * {@code Long.parseLong(str)}.</p>
	 *
	 * <p><b>Penanganan error.</b> Tidak ada guard null/format — bila string null atau
	 * mengandung nilai non-numerik, akan melempar {@code NullPointerException} atau
	 * {@code NumberFormatException}. Pastikan validasi dilakukan sebelum memanggil.</p>
	 *
	 * @param string string angka dipisah koma; tidak boleh null dan harus berisi angka valid
	 * @return larik Long hasil konversi
	 * @throws NumberFormatException bila salah satu token bukan angka valid
	 */
	public static Long[] splitToLongArray(String string) {

		String[] stringList = string.split(",");
		Long[] longArray = new Long[stringList.length];
		int i = 0;
		for (String str : stringList) {
			longArray[i++] = Long.parseLong(str);
		} /* from ww w . j a v a 2 s .c om */
		return longArray;
	}

	/**
	 * Menghapus kata-kata duplikat dari string masukan, mempertahankan urutan kemunculan
	 * pertama setiap kata.
	 *
	 * <p><b>Tujuan.</b> Digunakan untuk membersihkan string yang mungkin mengandung kata
	 * berulang akibat penggabungan beberapa sumber teks, misalnya tag, kategori, atau
	 * nama yang di-generate secara dinamis.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Memecah {@code inputString} dengan regex whitespace ({@code "\\s+"}) menjadi
	 *       larik kata.</li>
	 *   <li>Membuat {@code HashSet<String>} untuk melacak kata yang sudah ditambahkan.</li>
	 *   <li>Mengiterasi kata; bila kata belum ada di set, menambahkannya ke
	 *       {@code StringBuilder} dan ke set (via {@code add}).</li>
	 *   <li>Mengubah StringBuilder ke string, memangkas spasi di awal/akhir.</li>
	 * </ol>
	 * Catatan: urutan pertama dipertahankan karena HashSet menjaga keunikan tetapi
	 * iterasi dilakukan secara urut atas larik kata asli.</p>
	 *
	 * @param inputString string yang akan dibersihkan dari duplikat; tidak boleh null
	 * @return string dengan kata duplikat dihapus, dipisah spasi tunggal
	 */
	public static String removeDuplicateWords(String inputString) {
		// Splitting the string into words
		String[] words = inputString.split("\\s+");

		// Creating a set to store unique words
		Set<String> uniqueWords = new HashSet<String>();

		// Removing duplicate words
		StringBuilder resultBuilder = new StringBuilder();
		for (String word : words) {
			if (uniqueWords.add(word)) {
				resultBuilder.append(word).append(" ");
			}
		}

		// Converting the StringBuilder to a string
		String result = resultBuilder.toString().trim();

		return result;
	}

	/**
	 * Menambahkan sebuah komponen (mis. tombol toolbar kustom seperti
	 * "Singkronkan Nilai") ke TOOLBAR halaman CRUD secara AMAN.
	 *
	 * <p>Toolbar diambil dari parent tombol "add" bila tersedia. Bila "add" null
	 * (zul tanpa tombol Tambah / komponen belum ter-wire), toolbar dicari lewat
	 * fellow "find" lalu "add" dari komponen yang sudah ter-attach
	 * (anchorForFellow — mis. parameter {@code comp} pada doAfterCompose, yang
	 * merupakan space owner). Dengan begitu tombol kustom TETAP masuk ke toolbar
	 * walau "add" null, dan tidak pernah melempar NullPointerException.</p>
	 */
	public static void appendKeToolbar(org.zkoss.zk.ui.Component child,
			org.zkoss.zk.ui.Component addBtn, org.zkoss.zk.ui.Component anchorForFellow) {
		if (child == null) {
			return;
		}
		org.zkoss.zk.ui.Component toolbar = null;
		try {
			if (addBtn != null) {
				toolbar = addBtn.getParent();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:18601");
		}
		if (toolbar == null && anchorForFellow != null) {
			try {
				org.zkoss.zk.ui.Component f = anchorForFellow.getFellowIfAny("find");
				if (f == null) {
					f = anchorForFellow.getFellowIfAny("add");
				}
				if (f != null) {
					toolbar = f.getParent();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:18612");
			}
		}
		if (toolbar != null) {
			try {
				toolbar.appendChild(child);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:18618");
			}
		}
	}

	/**
	 * Deteksi error koneksi database yang bersifat TRANSIENT (mis. koneksi diputus
	 * administrator / maintenance tengah malam, koneksi sudah ditutup, reset, atau
	 * broken pipe). Dipakai agar log admin tidak dibanjiri error sementara yang
	 * sebenarnya sudah ditangani (rollback + hasil kosong). Menelusuri rantai cause.
	 */
	/**
	 * Membaca nilai id/angka dari payload JSON secara TOLERAN.
	 *
	 * <p><b>KE-FIX</b> ("java.lang.NumberFormatException: For input string: &quot;&quot;").
	 * Pola lama di seluruh helper API berbentuk:</p>
	 *
	 * <pre>request.isNull("x") ? null : Long.valueOf((request.get("x") + "").trim())</pre>
	 *
	 * <p>Formulir HTML dan klien Flutter mengirim field kosong sebagai string KOSONG
	 * ({@code "x": ""}), BUKAN JSON null. {@code JSONObject.isNull} bernilai false untuk string
	 * kosong sehingga {@code Long.valueOf("")} tetap dijalankan dan melempar
	 * NumberFormatException -- pengguna melihat layar galat "For input string:" pada aksi yang
	 * sebenarnya sah (mis. retur penjualan tanpa nota asal).</p>
	 *
	 * <p>Karena pola lamanya sendiri sudah membolehkan hasil {@code null} (cabang isNull),
	 * memulangkan {@code null} untuk nilai kosong/bukan angka TIDAK mengubah kontrak pemanggil:
	 * nilainya tetap salah satu dari "ada angkanya" atau "tidak diisi".</p>
	 *
	 * @param request payload JSON (boleh null)
	 * @param kunci   nama field
	 * @return nilai Long, atau null bila tidak ada / kosong / bukan angka
	 */
	/**
	 * Menyaring sebuah String agar aman dipakai sebagai <b>value</b> {@link javax.servlet.http.Cookie}.
	 *
	 * <p><b>KE-FIX</b> ("java.lang.IllegalArgumentException: An invalid character [32] was present
	 * in the Cookie value"). Tomcat 8+ memvalidasi value cookie menurut RFC 6265 saat header
	 * dibentuk, dan menolak spasi (32), koma (44), titik-koma (59), petik ganda, backslash, serta
	 * karakter kontrol. Beberapa cookie diisi LANGSUNG dari data pengguna -- mis. nomor registrasi
	 * calon mahasiswa/siswa yang boleh mengandung spasi -- sehingga login PMB gagal dengan layar
	 * galat, bukan karena kredensialnya salah.</p>
	 *
	 * <p>Karakter yang tidak sah DIBUANG, bukan diganti, supaya nilai yang selama ini sudah aman
	 * (hasil {@code URLEncoder.encode} atau Base64) tetap identik byte-per-byte -- cookie lama
	 * yang sudah tersimpan di peramban pengguna tetap terbaca seperti biasa.</p>
	 *
	 * @param value nilai mentah (boleh null)
	 * @return nilai yang hanya berisi cookie-octet RFC 6265; "" bila masukannya null
	 */
	/**
	 * Varian desimal dari {@link #angkaAtauNull(JSONObject, String)} untuk nilai uang.
	 *
	 * @return nilai Double, atau null bila tidak ada / kosong / bukan angka
	 */
	public static Double angkaDesimalAtauNull(JSONObject request, String kunci) {
		if (request == null || kunci == null) {
			return null;
		}
		Object mentah = request.opt(kunci);
		if (mentah == null || JSONObject.NULL.equals(mentah)) {
			return null;
		}
		if (mentah instanceof Number) {
			return Double.valueOf(((Number) mentah).doubleValue());
		}
		String teks = String.valueOf(mentah).trim();
		if (teks.length() == 0) {
			return null;
		}
		try {
			return Double.valueOf(teks);
		} catch (NumberFormatException bukanAngka) {
			return null;
		}
	}

	public static String nilaiCookieAman(String value) {
		if (value == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			// cookie-octet RFC6265: %x21, %x23-2B, %x2D-3A, %x3C-5B, %x5D-7E
			// (kecuali DQUOTE, koma, titik-koma, backslash, spasi, dan karakter kontrol).
			if (c == ',' || c == ';' || c == '\\' || c == '"' || c <= 0x20 || c >= 0x7F) {
				continue;
			}
			sb.append(c);
		}
		return sb.toString();
	}

	public static Long angkaAtauNull(JSONObject request, String kunci) {
		if (request == null || kunci == null) {
			return null;
		}
		Object mentah = request.opt(kunci);
		if (mentah == null || JSONObject.NULL.equals(mentah)) {
			return null;
		}
		if (mentah instanceof Number) {
			return Long.valueOf(((Number) mentah).longValue());
		}
		String teks = String.valueOf(mentah).trim();
		if (teks.length() == 0 || "null".equalsIgnoreCase(teks) || "undefined".equalsIgnoreCase(teks)) {
			return null;
		}
		try {
			return Long.valueOf(teks);
		} catch (NumberFormatException bukanBulat) {
			try {
				// Klien JavaScript kerap mengirim id sebagai desimal ("12.0"); ambil bagian bulatnya.
				return Long.valueOf(new java.math.BigDecimal(teks).longValue());
			} catch (NumberFormatException tetapBukanAngka) {
				return null;
			}
		}
	}

	/** Sama seperti {@link #angkaAtauNull(JSONObject, String)} untuk elemen JSONArray berisi objek. */
	public static Integer angkaBulatAtauNull(JSONObject request, String kunci) {
		Long nilai = angkaAtauNull(request, kunci);
		return nilai == null ? null : Integer.valueOf(nilai.intValue());
	}

	public static boolean isTransientKoneksiError(Throwable t) {
		Throwable cur = t;
		int guard = 0;
		while (cur != null && guard++ < 30) {
			if (cur instanceof org.hibernate.exception.JDBCConnectionException) {
				return true;
			}
			/* KE-FIX ("An I/O error occurred while sending to the backend" / EOFException dari
			 * PostgreSQL JDBC): kegagalan socket seperti ini TIDAK selalu dibungkus
			 * JDBCConnectionException dan kalimatnya tidak cocok dengan daftar teks di bawah,
			 * sehingga sebelumnya dianggap galat biasa -- pemanggil lalu mencoba rollback pada
			 * koneksi yang sudah mati dan menghasilkan exception sekunder yang membanjiri log.
			 * SQLState kelas "08" adalah kelas standar SQL untuk connection exception, jadi ini
			 * menutup seluruh ragamnya sekaligus (08000/08003/08006/08001/08004/08007). */
			if (cur instanceof java.sql.SQLException) {
				String state = ((java.sql.SQLException) cur).getSQLState();
				if (state != null && state.length() >= 2 && state.startsWith("08")) {
					return true;
				}
			}
			if (cur instanceof java.io.EOFException || cur instanceof java.net.SocketException
					|| cur instanceof java.net.SocketTimeoutException) {
				return true;
			}
			String m = cur.getMessage();
			if (m != null) {
				String lm = m.toLowerCase();
				if (lm.indexOf("connection has been closed") >= 0
						|| lm.indexOf("connection is closed") >= 0
						|| lm.indexOf("connection closed") >= 0
						|| lm.indexOf("terminating connection") >= 0
						|| lm.indexOf("cannot open connection") >= 0
						|| lm.indexOf("could not open connection") >= 0
						|| lm.indexOf("connection reset") >= 0
						|| lm.indexOf("broken pipe") >= 0
						|| lm.indexOf("administrator command") >= 0
						|| lm.indexOf("connection attempt failed") >= 0
						|| lm.indexOf("i/o error occurred while sending to the backend") >= 0
						|| lm.indexOf("an i/o error occurred") >= 0
						|| lm.indexOf("socket is closed") >= 0
						|| lm.indexOf("connection refused") >= 0) {
					return true;
				}
			}
			Throwable berikut = cur.getCause();
			if (berikut == null && cur instanceof java.sql.SQLException) {
				// PostgreSQL JDBC merantai penyebab aslinya di getNextException, bukan getCause.
				berikut = ((java.sql.SQLException) cur).getNextException();
			}
			if (berikut == cur) {
				break;
			}
			cur = berikut;
		}
		return false;
	}

	/**
	 * Mengeksekusi native SQL SELECT menggunakan {@code StreamingHibernateUtil} (session factory
	 * khusus streaming) dan mengembalikan hasilnya sebagai daftar larik objek.
	 *
	 * <p><b>Tujuan.</b> Digunakan untuk kueri besar atau kueri ke tabel yang khusus dikonfigurasi
	 * di SessionFactory streaming (terpisah dari SessionFactory utama), agar tidak membebani
	 * pool koneksi utama.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Memeriksa SQL via {@link #isMaliciousSql(String)} — bila terdeteksi anomali SQL
	 *       injection, melempar {@code SecurityException}.</li>
	 *   <li>Membuka sesi dari {@code StreamingHibernateUtil.getInstance().openSession()}.</li>
	 *   <li>Memulai transaksi eksplisit.</li>
	 *   <li>Membuat {@code SQLQuery} dan mengeksekusi {@code query.list()}.</li>
	 *   <li>Commit transaksi bila berhasil.</li>
	 *   <li>Pada error: rollback transaksi; bila error bersifat transient koneksi
	 *       ({@link #isTransientKoneksiError}), hanya mencetak log; selainnya memanggil
	 *       {@link #tampilErrorJikaAdmin(Exception)}.</li>
	 *   <li>Di blok finally: disconnect dan close session secara aman.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Keamanan.</b> Dilindungi oleh pemeriksaan SQL injection sebelum eksekusi.</p>
	 *
	 * @param sql native SQL SELECT yang akan dieksekusi; tidak boleh null atau kosong
	 * @return List of Object[] hasil kueri; list kosong bila tidak ada hasil atau terjadi error
	 * @throws SecurityException bila terdeteksi pola SQL injection pada {@code sql}
	 */
	@SuppressWarnings("unchecked")
	public static List<Object[]> ambilSqlStreaming(String sql) {
		// --- TAMBAHKAN PROTEKSI DI SINI ---
		if (isMaliciousSql(sql)) {
			// Lempar exception agar proses langsung dihentikan dan ditolak
			throw new SecurityException(
					"Akses ditolak: Terdeteksi aktivitas anomali pada kueri database (SQL Injection).");
		}
		List<Object[]> results = new ArrayList<Object[]>();
		Session session = null;
		Transaction transaction = null;
		try {
			session = StreamingHibernateUtil.getInstance().openSession();
			System.out.println(sql);
			// 1. Mulai transaksi secara eksplisit
			transaction = session.beginTransaction();

			if (sql != null && !sql.trim().isEmpty()) {
				SQLQuery query = session.createSQLQuery(sql);

				results = query.list();
			}

			// 2. Commit transaksi jika eksekusi berhasil
			transaction.commit();

		} catch (Exception e) {
			// 3. WAJIB ROLLBACK jika terjadi error
			// Ini akan mereset status koneksi PostgreSQL dari "aborted" menjadi normal
			// kembali
			if (transaction != null && transaction.isActive()) {
				try {
					transaction.rollback();
					System.err.println(
							"Transaction di-rollback untuk mencegah status 'aborted' pada koneksi.\nSQL = " + sql);
				} catch (Exception rollbackEx) {
					System.err.println("Gagal melakukan rollback: " + rollbackEx.getMessage());
				}
			}

			// Error koneksi transient (mis. koneksi diputus admin/maintenance tengah malam)
			// tidak perlu membanjiri log admin — sudah ditangani (rollback + hasil kosong).
			if (isTransientKoneksiError(e)) {
				System.err.println("Koneksi DB transient terputus; query dilewati setelah rollback. SQL = " + sql);
			} else if (e instanceof org.hibernate.exception.SQLGrammarException
					|| e instanceof org.hibernate.exception.GenericJDBCException
					|| e instanceof org.hibernate.QueryException || e instanceof org.hibernate.MappingException) {
				// ATURAN WAJIB NATIVE SQL: jangan pernah memakai cast PostgreSQL "::tipe".
				// Hibernate dapat membacanya sebagai named parameter ":tipe" dan menolak kueri.
				// Selalu gunakan bentuk standar CAST(ekspresi AS tipe), termasuk date/text/numeric/jsonb.
				// Error tingkat-KUERI (grammar/relasi tidak ada, tipe JDBC tak terpetakan mis. ARRAY,
				// parameter bernama palsu dari cast PostgreSQL "::tipe", dsb.) berasal dari TEKS SQL
				// yang dikirim pemanggil (data-explorer DaftarDataService), BUKAN bug aplikasi.
				// Rollback + hasil kosong sudah cukup; cukup catat ke System.err agar log admin tidak
				// dibanjiri kueri ad-hoc yang salah.
				System.err.println("Kueri native gagal (query-level, dilewati). SQL = " + sql + " ; err = "
						+ e.getMessage());
			} else {
				// Catat error aslinya (disarankan memakai Logger seperti log.error di production).
				tampilErrorJikaAdmin(e);
			}

		} finally {
			// 4. Tutup Session dengan bersih
			if (session != null && session.isOpen()) {
				try {
					// session.disconnect() tidak perlu dipanggil, session.close() sudah cukup
					// untuk mengembalikan koneksi ke dalam pool.
					session.disconnect();
					session.close();
				} catch (Exception e) {
					System.err.println("Gagal menutup session: " + e.getMessage());
				}
			}
		}

		return results;
	}

	/**
	 * Mengeksekusi native SQL SELECT menggunakan SessionFactory utama dan mengembalikan
	 * hasilnya sebagai daftar larik objek.
	 *
	 * <p><b>Tujuan.</b> Alternatif fleksibel dari Hibernate Criteria untuk kueri kompleks
	 * yang sulit diekspresikan lewat Criteria API — misalnya kueri dengan subquery,
	 * aggregasi bersarang, atau JOIN antar tabel yang tidak terhubung lewat relasi Hibernate.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Guard SQL injection via {@link #isMaliciousSql(String)}.</li>
	 *   <li>Membuka sesi dedicated via {@code HibernateUtil.getSessionFactory().openSession()}.</li>
	 *   <li>Memulai transaksi eksplisit, mengeksekusi {@code createSQLQuery(sql).list()},
	 *       commit bila berhasil.</li>
	 *   <li>Pada error: rollback; error transient → log saja; error lain → {@link #tampilErrorJikaAdmin}.</li>
	 *   <li>Finally: disconnect dan close session.</li>
	 * </ol>
	 * Berbeda dari {@link #ambilSqlStreaming}: menggunakan SessionFactory utama, bukan streaming.</p>
	 *
	 * @param sql native SQL SELECT; tidak boleh null atau kosong
	 * @return List of Object[] hasil kueri; list kosong bila tidak ada hasil atau error
	 * @throws SecurityException bila terdeteksi SQL injection
	 */
	@SuppressWarnings("unchecked")
	public static List<Object[]> ambilSql(String sql) {
		// --- TAMBAHKAN PROTEKSI DI SINI ---
		if (isMaliciousSql(sql)) {
			// Lempar exception agar proses langsung dihentikan dan ditolak
			throw new SecurityException(
					"Akses ditolak: Terdeteksi aktivitas anomali pada kueri database (SQL Injection).");
		}
		List<Object[]> results = new ArrayList<Object[]>();
		Session session = null;
		Transaction transaction = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			// 1. Mulai transaksi secara eksplisit
			transaction = session.beginTransaction();

			if (sql != null && !sql.trim().isEmpty()) {
				SQLQuery query = session.createSQLQuery(sql);

				results = query.list();
			}

			// 2. Commit transaksi jika eksekusi berhasil
			transaction.commit();

		} catch (Exception e) {
			// 3. WAJIB ROLLBACK jika terjadi error
			// Ini akan mereset status koneksi PostgreSQL dari "aborted" menjadi normal
			// kembali
			if (transaction != null && transaction.isActive()) {
				try {
					transaction.rollback();
					System.err.println(
							"Transaction di-rollback untuk mencegah status 'aborted' pada koneksi.\nSQL = " + sql);
				} catch (Exception rollbackEx) {
					System.err.println("Gagal melakukan rollback: " + rollbackEx.getMessage());
				}
			}

			// Error koneksi transient (mis. koneksi diputus admin/maintenance tengah malam)
			// tidak perlu membanjiri log admin — sudah ditangani (rollback + hasil kosong).
			if (isTransientKoneksiError(e)) {
				System.err.println("Koneksi DB transient terputus; query dilewati setelah rollback. SQL = " + sql);
			} else if (e instanceof org.hibernate.exception.SQLGrammarException
					|| e instanceof org.hibernate.exception.GenericJDBCException
					|| e instanceof org.hibernate.QueryException || e instanceof org.hibernate.MappingException) {
				// Error tingkat-KUERI (grammar/relasi tidak ada, tipe JDBC tak terpetakan mis. ARRAY,
				// parameter bernama palsu dari cast PostgreSQL "::tipe", dsb.) berasal dari TEKS SQL
				// yang dikirim pemanggil (data-explorer DaftarDataService), BUKAN bug aplikasi.
				// Rollback + hasil kosong sudah cukup; cukup catat ke System.err agar log admin tidak
				// dibanjiri kueri ad-hoc yang salah.
				System.err.println("Kueri native gagal (query-level, dilewati). SQL = " + sql + " ; err = "
						+ e.getMessage());
			} else {
				// Catat error aslinya (disarankan memakai Logger seperti log.error di production).
				tampilErrorJikaAdmin(e);
			}

		} finally {
			// 4. Tutup Session dengan bersih
			if (session != null && session.isOpen()) {
				try {
					// session.disconnect() tidak perlu dipanggil, session.close() sudah cukup
					// untuk mengembalikan koneksi ke dalam pool.
					session.disconnect();
					session.close();
				} catch (Exception e) {
					System.err.println("Gagal menutup session: " + e.getMessage());
				}
			}
		}

		return results;
	}

	/**
	 * Mengeksekusi native SQL SELECT dan mengembalikan hasilnya sebagai daftar {@code Map<String, Object>}
	 * (kolom → nilai), menggunakan {@code AliasToEntityMapResultTransformer}.
	 *
	 * <p><b>Tujuan.</b> Digunakan saat hasil kueri perlu diakses berdasarkan nama alias kolom
	 * (bukan indeks), terutama untuk kueri dinamis atau ekspor data ke JSON/Excel di mana
	 * nama kolom bervariasi.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Guard SQL injection.</li>
	 *   <li>Buka sesi dari SessionFactory utama, mulai transaksi.</li>
	 *   <li>Buat SQLQuery, pasang
	 *       {@code query.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE)}
	 *       agar setiap baris hasil diubah menjadi {@code Map<String, Object>}.</li>
	 *   <li>Eksekusi {@code query.list()}, commit, return results.</li>
	 *   <li>Error handling dan cleanup session sama seperti {@link #ambilSql(String)}.</li>
	 * </ol>
	 * </p>
	 *
	 * @param sql native SQL SELECT dengan alias kolom yang eksplisit untuk akses via Map
	 * @return List of Map<String, Object>; list kosong bila tidak ada hasil atau error
	 * @throws SecurityException bila terdeteksi SQL injection
	 */
	@SuppressWarnings({ "unchecked" })
	public static List<Map<String, Object>> ambilSqlMap(String sql) {
		// --- TAMBAHKAN PROTEKSI DI SINI ---
		if (isMaliciousSql(sql)) {
			// Lempar exception agar proses langsung dihentikan dan ditolak
			throw new SecurityException(
					"Akses ditolak: Terdeteksi aktivitas anomali pada kueri database (SQL Injection).");
		}
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		Session session = null;
		Transaction transaction = null;

		try {
			session = HibernateUtil.getSessionFactory().openSession();

			// 1. Mulai transaksi secara eksplisit
			transaction = session.beginTransaction();

			if (sql != null && !sql.trim().isEmpty()) {
				SQLQuery query = session.createSQLQuery(sql);

				// Set transformer agar hasil di-mapping ke Map
				query.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

				results = query.list();
			}

			// 2. Commit transaksi jika eksekusi berhasil
			transaction.commit();

		} catch (Exception e) {
			// 3. WAJIB ROLLBACK jika terjadi error
			// Ini akan mereset status koneksi PostgreSQL dari "aborted" menjadi normal
			// kembali
			if (transaction != null && transaction.isActive()) {
				try {
					transaction.rollback();
					System.err.println(
							"Transaction di-rollback untuk mencegah status 'aborted' pada koneksi.\nSQL = " + sql);
				} catch (Exception rollbackEx) {
					System.err.println("Gagal melakukan rollback: " + rollbackEx.getMessage());
				}
			}

			// Error koneksi transient (mis. koneksi diputus admin/maintenance tengah malam)
			// tidak perlu membanjiri log admin — sudah ditangani (rollback + hasil kosong).
			if (isTransientKoneksiError(e)) {
				System.err.println("Koneksi DB transient terputus; query dilewati setelah rollback. SQL = " + sql);
			} else if (e instanceof org.hibernate.exception.SQLGrammarException
					|| e instanceof org.hibernate.exception.GenericJDBCException
					|| e instanceof org.hibernate.QueryException || e instanceof org.hibernate.MappingException) {
				// Error tingkat-KUERI (grammar/relasi tidak ada, tipe JDBC tak terpetakan mis. ARRAY,
				// parameter bernama palsu dari cast PostgreSQL "::tipe", dsb.) berasal dari TEKS SQL
				// yang dikirim pemanggil (data-explorer DaftarDataService), BUKAN bug aplikasi.
				// Rollback + hasil kosong sudah cukup; cukup catat ke System.err agar log admin tidak
				// dibanjiri kueri ad-hoc yang salah.
				System.err.println("Kueri native gagal (query-level, dilewati). SQL = " + sql + " ; err = "
						+ e.getMessage());
			} else {
				// Catat error aslinya (disarankan memakai Logger seperti log.error di production).
				tampilErrorJikaAdmin(e);
			}

		} finally {
			// 4. Tutup Session dengan bersih
			if (session != null && session.isOpen()) {
				try {
					// session.disconnect() tidak perlu dipanggil, session.close() sudah cukup
					// untuk mengembalikan koneksi ke dalam pool.
					session.disconnect();
					session.close();
				} catch (Exception e) {
					System.err.println("Gagal menutup session: " + e.getMessage());
				}
			}
		}

		return results;
	}

	/**
	 * Mengeksekusi native SQL UPDATE/DELETE/INSERT dengan timeout default.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonSqlHelper.updateSql(sql)}
	 * yang membuka dedicated session, menjalankan {@code createSQLQuery(sql).executeUpdate()},
	 * commit, lalu menutup session. Dilindungi oleh pemeriksaan SQL injection.</p>
	 *
	 * @param sql native SQL UPDATE/DELETE/INSERT; tidak boleh null
	 * @return jumlah baris yang terpengaruh oleh eksekusi
	 * @throws SecurityException bila terdeteksi SQL injection
	 */
	public static int updateSql(String sql) {
		return CommonSqlHelper.updateSql(sql);
	}

	/**
	 * Mengeksekusi SQL Update/Delete dengan timeout bawaan 600 detik (10 menit)
	 * untuk mencegah error 'canceling statement due to lock timeout'.
	 */
	public static int updateSql10Menit(String sql) {
		return CommonSqlHelper.updateSql10Menit(sql);
	}

	/**
	 * Fungsi untuk mendeteksi indikasi SQL Injection
	 */
	public static boolean isMaliciousSql(String sql) {
		return CommonSqlHelper.isMaliciousSql(sql);
	}

	/**
	 * Mengeksekusi SQL Update/Delete dengan timeout bawaan 600 detik (60 menit)
	 * untuk mencegah error 'canceling statement due to lock timeout'.
	 */
	public static int updateSql60Menit(String sql) {
		return CommonSqlHelper.updateSql60Menit(sql);
	}

	public static int updateSql(String sql, int timeout) {
		return CommonSqlHelper.updateSql(sql, timeout);
	}

	/**
	 * Mengeksekusi SQL Update/Delete dengan custom timeout.
	 *
	 * @param sql     Query native SQL yang akan dieksekusi
	 * @param timeout Waktu maksimal eksekusi dan lock dalam detik. Jika 0, ikuti
	 *                default database.
	 */
	public static int updateSql(String sql, int timeout, boolean lewati) {
		return CommonSqlHelper.updateSql(sql, timeout, lewati);
	}

	/**
	 * Mengeksekusi native SQL UPDATE/DELETE/INSERT menggunakan SessionFactory streaming.
	 *
	 * <p><b>Tujuan.</b> Digunakan untuk operasi DML pada tabel yang dikonfigurasi di
	 * SessionFactory streaming (bukan SessionFactory utama), biasanya tabel besar atau tabel
	 * yang diakses lewat koneksi terpisah untuk menghindari konflik.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke {@code CommonSqlHelper.updateSqlStreaming(sql)}.</p>
	 *
	 * @param sql native SQL UPDATE/DELETE/INSERT; tidak boleh null
	 * @return jumlah baris yang terpengaruh
	 */
	public static int updateSqlStreaming(String sql) {
		return CommonSqlHelper.updateSqlStreaming(sql);
	}

	/**
	 * Menentukan apakah konteks pengguna saat ini adalah Perguruan Tinggi atau Sekolah/Pesantren,
	 * berdasarkan pengguna yang sedang login.
	 *
	 * <p><b>Tujuan.</b> Digunakan untuk menyesuaikan menu, label, dan modul yang tersedia
	 * sesuai jenis institusi pengguna saat ini — PT (perguruan tinggi) vs sekolah/pesantren.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@link #chekPtAtauSekolah(Tbmuser)} dengan pengguna dari {@link #getCurrentUser()}.</p>
	 *
	 * @return larik boolean 2 elemen: {@code [0] = isPT, [1] = isSekolah}
	 */
	public static boolean[] chekPtAtauSekolah() {
		Tbmuser tbmuser = Common.getCurrentUser();
		return chekPtAtauSekolah(tbmuser);
	}

	/**
	 * Menentukan apakah konteks pengguna tertentu adalah Perguruan Tinggi atau Sekolah/Pesantren
	 * berdasarkan relasi entitas pengguna dan konfigurasi sistem.
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Mengambil sekolah dari {@code SekolahUtil.getSekolah()} (konteks sesi).</li>
	 *   <li>Bila sekolah null tetapi pengguna memiliki relasi sekolah, gunakan sekolah dari pengguna.</li>
	 *   <li>Bila sekolah ada → {@code isPT=false, isSekolah=true}.</li>
	 *   <li>Bila pengguna adalah mahasiswa/dosen → {@code isPT=true, isSekolah=false}.</li>
	 *   <li>Bila pengguna adalah siswa/guru → {@code isPT=false, isSekolah=true}.</li>
	 *   <li>Bila tidak ada sekolah → baca konfigurasi
	 *       {@code apakah_aktifkan_modul_perguruan_tinggi} dan
	 *       {@code apakah_aktifkan_modul_sekolah/pesantren} dari tabel {@code Konfigurasi}.</li>
	 *   <li>Override: bila pengguna terikat PerguruanTinggi → isPT=true, isSekolah=false.</li>
	 *   <li>Override: bila yayasan memiliki flag merupakanSekolah → isPT=false, isSekolah=true.</li>
	 * </ol>
	 * </p>
	 *
	 * @param tbmuser pengguna yang diperiksa; boleh null (menggunakan konteks sesi)
	 * @return larik boolean 2 elemen: {@code [0] = isPT}, {@code [1] = isSekolah}
	 */
	public static boolean[] chekPtAtauSekolah(Tbmuser tbmuser) {
		Sekolah sekolah1 = SekolahUtil.getSekolah();

		boolean pt = true;
		boolean ya = true;

		if ((sekolah1 == null || sekolah1.getId() == null) && tbmuser != null && tbmuser.ambilSekolah() != null) {
			sekolah1 = tbmuser.ambilSekolah();
		}

		if (sekolah1 != null && sekolah1.getId() != null) {
			pt = false;
			ya = true;
		} else if (tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.getDosen() != null)) {
			pt = true;
			ya = false;
		} else if (tbmuser != null && (tbmuser.getSiswa() != null || tbmuser.getGuru() != null)) {
			pt = false;
			ya = true;
		} else if (sekolah1 == null || sekolah1.getId() == null) {
			pt = Common.bolehKonfigurasi("apakah_aktifkan_modul_perguruan_tinggi");
			ya = Common.bolehKonfigurasi("apakah_aktifkan_modul_sekolah", Konfigurasi.TIDAK_AKTIF)
					|| Common.bolehKonfigurasi("apakah_aktifkan_modul_pesantren", Konfigurasi.TIDAK_AKTIF);

			if (pt) {
				PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
				if (perguruanTinggi != null && perguruanTinggi.getFeeder() != null) {
					ya = false;
				}
			}
		}

		if (tbmuser != null && tbmuser.getPerguruanTinggi() != null && tbmuser.getPerguruanTinggi().getId() != null) {
			ya = false;
			pt = true;
		}

		// getMerupakanSekolah() bertipe Boolean -> bila NULL, pemakaian langsung dalam kondisi "&&"
		// meng-auto-unbox NULL = NullPointerException. Pakai Boolean.TRUE.equals agar aman.
		if (tbmuser != null && tbmuser.getYayasan() != null && tbmuser.getYayasan().getPendaftar() != null
				&& Boolean.TRUE.equals(tbmuser.getYayasan().getPendaftar().getMerupakanSekolah())) {
			ya = true;
			pt = false;
		}
		return new boolean[] { pt, ya };
	}

	/**
	 * Melakukan logout otomatis dengan redirect ke halaman logoff setelah timer singkat.
	 *
	 * <p><b>Tujuan.</b> Digunakan saat sistem mendeteksi sesi tidak valid atau pengguna
	 * perlu dikeluarkan paksa (misalnya timeout sesi, perubahan password oleh admin).</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Memanggil {@code Clients.confirmClose(null)} untuk mencegah browser memunculkan
	 *       dialog konfirmasi "Apakah Anda yakin ingin meninggalkan halaman ini?".</li>
	 *   <li>Membuat timer default via {@link #createDefaultTimer(EventListener)} dengan listener
	 *       yang memanggil {@code ExecutionsCtrl.getCurrent().sendRedirect("/logoff")}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Threading.</b> Redirect dilakukan dalam listener timer yang berjalan di thread
	 * event ZK, memastikan operasi UI aman.</p>
	 */
	public static void goLogoff() {
		try {
			if (ExecutionsCtrl.getCurrent() == null) {
				return;
			}
			Clients.confirmClose(null);
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (ExecutionsCtrl.getCurrent() != null) {
						ExecutionsCtrl.getCurrent().sendRedirect("/logoff");
					}
				}
			});
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit Common.goLogoff");
		}
	}

	/**
	 * Menggeser tanggal di {@code myCalendar} maju sesuai jenis interval (dari Radiogroup)
	 * hingga mendarat di hari yang BUKAN hari merah (libur merah saja — tidak termasuk
	 * hari libur perpustakaan).
	 *
	 * <p><b>Tujuan.</b> Digunakan dalam pengaturan jadwal peminjaman/pengembalian perpustakaan
	 * agar tanggal jatuh tempo tidak jatuh pada hari merah nasional.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Bila cache {@code hariLiburPerpustakaans} kosong, langsung kembalikan kalender
	 *       tanpa perubahan (tidak ada data libur yang dimuat).</li>
	 *   <li>Iterasi dengan batas 100 kali (guard tak-terbatas): bila tanggal saat ini adalah
	 *       hari merah ({@link #isHolidayMerah}), maju dengan {@link #curreDate(Radiogroup, Calendar)}.
	 *       Berhenti bila tidak lagi merah atau batas tercapai.</li>
	 * </ol>
	 * </p>
	 *
	 * @param jenis      Radiogroup berisi pilihan interval (Harian, Mingguan, Bulanan, dsb.)
	 * @param myCalendar kalender yang akan digeser; dimodifikasi secara in-place
	 * @return kalender yang sudah digeser ke hari non-merah
	 */
	public static Calendar tanggalMerahAja(Radiogroup jenis, Calendar myCalendar) {

		if (Common.hariLiburPerpustakaans.isEmpty()) {
			return myCalendar;
		}

		int iii = 0;
		while (Common.isHolidayMerah(myCalendar.getTime())) {
			iii++;
			if (iii > 100) {
				break;
			}

			myCalendar = curreDate(jenis, myCalendar);
		}
		return myCalendar;
	}

	/**
	 * Menggeser tanggal di {@code myCalendar} maju sesuai jenis interval (String) hingga
	 * mendarat di hari yang BUKAN hari merah.
	 *
	 * <p><b>Tujuan.</b> Varian dari {@link #tanggalMerahAja(Radiogroup, Calendar)} yang
	 * menerima nama interval sebagai String (misalnya {@code "Harian"}, {@code "Mingguan"})
	 * alih-alih Radiogroup, berguna saat interval sudah dikonversi ke String sebelumnya.</p>
	 *
	 * <p><b>Cara kerja.</b> Sama dengan varian Radiogroup, menggunakan
	 * {@link #curreDate(String, Calendar)} untuk menggeser.</p>
	 *
	 * @param jenis      nama interval sebagai String (misalnya "Harian", "Mingguan")
	 * @param myCalendar kalender yang akan digeser
	 * @return kalender yang sudah digeser ke hari non-merah
	 */
	public static Calendar tanggalMerahAja(String jenis, Calendar myCalendar) {

		if (Common.hariLiburPerpustakaans.isEmpty()) {
			return myCalendar;
		}

		int iii = 0;
		while (Common.isHolidayMerah(myCalendar.getTime())) {
			iii++;
			if (iii > 100) {
				break;
			}

			myCalendar = curreDate(jenis, myCalendar);
		}
		return myCalendar;
	}

	/**
	 * Menggeser tanggal di {@code myCalendar} maju sesuai jenis interval (Radiogroup) hingga
	 * mendarat di hari yang BUKAN hari libur (termasuk hari merah DAN hari libur perpustakaan).
	 *
	 * <p><b>Perbedaan dari {@link #tanggalMerahAja}.</b> Method ini menggunakan
	 * {@link #isHolidayMerahDanAtauHariLibur} yang memeriksa SEMUA jenis libur (hari merah
	 * nasional + hari libur yang dikonfigurasi perpustakaan), bukan hanya hari merah.</p>
	 *
	 * <p><b>Cara kerja.</b> Sama dengan {@link #tanggalMerahAja(Radiogroup, Calendar)}
	 * namun dengan pemeriksaan libur yang lebih komprehensif.</p>
	 *
	 * @param jenis      Radiogroup berisi pilihan interval
	 * @param myCalendar kalender yang akan digeser
	 * @return kalender yang sudah digeser ke hari non-libur
	 */
	public static Calendar tanggalMerah(Radiogroup jenis, Calendar myCalendar) {

		if (Common.hariLiburPerpustakaans.isEmpty()) {
			return myCalendar;
		}

		int iii = 0;
		while (Common.isHolidayMerahDanAtauHariLibur(myCalendar.getTime())) {
			iii++;
			if (iii > 100) {
				break;
			}

			myCalendar = curreDate(jenis, myCalendar);
		}
		return myCalendar;
	}

	/**
	 * Menggeser tanggal {@code myCalendar} satu langkah ke depan sesuai jenis interval
	 * yang dipilih di {@code Radiogroup}.
	 *
	 * <p><b>Tujuan.</b> Metode inti pergeseran tanggal yang digunakan oleh
	 * {@link #tanggalMerahAja} dan {@link #tanggalMerah} — menerapkan satu iterasi
	 * pergeseran berdasarkan jenis interval peminjaman perpustakaan.</p>
	 *
	 * <p><b>Cara kerja.</b> Membaca label item terpilih Radiogroup dan menggeser
	 * {@code Calendar.DATE} (atau {@code Calendar.MONTH}) sesuai pola berikut:
	 * <ul>
	 *   <li>{@code "Tgl Ganjil"} → maju 1 hari, lalu pastikan tanggal ganjil (geser lagi
	 *       bila genap, hingga 2 iterasi tambahan).</li>
	 *   <li>{@code "Tgl Genap"} → maju 1 hari, lalu pastikan tanggal genap.</li>
	 *   <li>{@code "Harian"} → +1 hari; {@code "2 Harian"} → +2 hari; dst. hingga 6 Harian.</li>
	 *   <li>{@code "Mingguan"} → +7 hari; {@code "2 Mingguan"} → +14 hari;
	 *       {@code "3 Mingguan"} → +21 hari; {@code "4 Mingguan"} → +28 hari.</li>
	 *   <li>{@code "Bulanan"} → {@code Calendar.MONTH + 1}.</li>
	 * </ul>
	 * </p>
	 *
	 * @param jenis      Radiogroup berisi item interval yang dipilih; tidak boleh null atau tanpa pilihan
	 * @param myCalendar kalender yang akan dimodifikasi; tidak boleh null
	 * @return kalender yang sudah digeser satu langkah interval
	 */
	public static Calendar curreDate(Radiogroup jenis, Calendar myCalendar) {

		if (jenis.getSelectedItem().getLabel().equals("Tgl Ganjil")) {
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);

			int date = myCalendar.get(Calendar.DATE);
			if (date % 2 == 0) {
				myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
				date = myCalendar.get(Calendar.DATE);
				if (date % 2 == 0) {
					myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
					date = myCalendar.get(Calendar.DATE);
				}
			}

		} else if (jenis.getSelectedItem().getLabel().equals("Tgl Genap")) {
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);

			int date = myCalendar.get(Calendar.DATE);
			if (date % 2 == 1) {
				myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
				date = myCalendar.get(Calendar.DATE);
				if (date % 2 == 1) {
					myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
					date = myCalendar.get(Calendar.DATE);
				}
			}

		} else if (jenis.getSelectedItem().getLabel().equals("Harian")) {
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
		} else if (jenis.getSelectedItem().getLabel().equals("2 Harian")) {
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 2);
		} else if (jenis.getSelectedItem().getLabel().equals("3 Harian")) {
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 3);
		} else if (jenis.getSelectedItem().getLabel().equals("4 Harian")) {
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 4);
		} else if (jenis.getSelectedItem().getLabel().equals("5 Harian")) {
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 5);
		} else if (jenis.getSelectedItem().getLabel().equals("6 Harian")) {
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 6);
		} else if (jenis.getSelectedItem().getLabel().equals("Mingguan")) {
			// myCalendar.set(Calendar.WEEK_OF_YEAR,
			// myCalendar.get(Calendar.WEEK_OF_YEAR) + 1);
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 7);
		} else if (jenis.getSelectedItem().getLabel().equals("2 Mingguan")) {
			// myCalendar.set(Calendar.WEEK_OF_YEAR,
			// myCalendar.get(Calendar.WEEK_OF_YEAR) + 2);
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 14);
		} else if (jenis.getSelectedItem().getLabel().equals("3 Mingguan")) {
			// myCalendar.set(Calendar.WEEK_OF_YEAR,
			// myCalendar.get(Calendar.WEEK_OF_YEAR) + 3);
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 21);
		} else if (jenis.getSelectedItem().getLabel().equals("4 Mingguan")) {
			// myCalendar.set(Calendar.WEEK_OF_YEAR,
			// myCalendar.get(Calendar.WEEK_OF_YEAR) + 4);
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 28);
		} else if (jenis.getSelectedItem().getLabel().equals("Bulanan")) {
			myCalendar.set(Calendar.MONTH, myCalendar.get(Calendar.MONTH) + 1);
		}
		return myCalendar;
	}

	/**
	 * Menggeser tanggal {@code myCalendar} satu langkah ke depan sesuai jenis interval
	 * yang dinyatakan sebagai String.
	 *
	 * <p><b>Perbedaan dari {@link #curreDate(Radiogroup, Calendar)}.</b> Menerima nama
	 * interval sebagai String langsung alih-alih Radiogroup. Logika pergeseran identik —
	 * membandingkan string {@code jenis} dengan nama interval yang dikenal.</p>
	 *
	 * @param jenis      nama interval; salah satu dari "Tgl Ganjil"/"Tgl Genap"/"Harian"/
	 *                   "2 Harian"/../"6 Harian"/"Mingguan"/"2 Mingguan"/"3 Mingguan"/
	 *                   "4 Mingguan"/"Bulanan"
	 * @param myCalendar kalender yang akan dimodifikasi; tidak boleh null
	 * @return kalender yang sudah digeser satu langkah
	 */
	public static Calendar curreDate(String jenis, Calendar myCalendar) {

		if (jenis.equals("Tgl Ganjil")) {
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);

			int date = myCalendar.get(Calendar.DATE);
			if (date % 2 == 0) {
				myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
				date = myCalendar.get(Calendar.DATE);
				if (date % 2 == 0) {
					myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
					date = myCalendar.get(Calendar.DATE);
				}
			}

		} else if (jenis.equals("Tgl Genap")) {
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);

			int date = myCalendar.get(Calendar.DATE);
			if (date % 2 == 1) {
				myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
				date = myCalendar.get(Calendar.DATE);
				if (date % 2 == 1) {
					myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
					date = myCalendar.get(Calendar.DATE);
				}
			}

		} else if (jenis.equals("Harian")) {
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
		} else if (jenis.equals("2 Harian")) {
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 2);
		} else if (jenis.equals("3 Harian")) {
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 3);
		} else if (jenis.equals("4 Harian")) {
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 4);
		} else if (jenis.equals("5 Harian")) {
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 5);
		} else if (jenis.equals("6 Harian")) {
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 6);
		} else if (jenis.equals("Mingguan")) {
			// myCalendar.set(Calendar.WEEK_OF_YEAR,
			// myCalendar.get(Calendar.WEEK_OF_YEAR) + 1);
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 7);
		} else if (jenis.equals("2 Mingguan")) {
			// myCalendar.set(Calendar.WEEK_OF_YEAR,
			// myCalendar.get(Calendar.WEEK_OF_YEAR) + 2);
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 14);
		} else if (jenis.equals("3 Mingguan")) {
			// myCalendar.set(Calendar.WEEK_OF_YEAR,
			// myCalendar.get(Calendar.WEEK_OF_YEAR) + 3);
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 21);
		} else if (jenis.equals("4 Mingguan")) {
			// myCalendar.set(Calendar.WEEK_OF_YEAR,
			// myCalendar.get(Calendar.WEEK_OF_YEAR) + 4);
			myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 28);
		} else if (jenis.equals("Bulanan")) {
			myCalendar.set(Calendar.MONTH, myCalendar.get(Calendar.MONTH) + 1);
		}
		return myCalendar;
	}

	/**
	 * Mengisi atau membuat {@code Combobox} ZK dengan daftar nama bulan (Januari–Desember),
	 * dan memilih bulan saat ini secara otomatis.
	 *
	 * <p><b>Tujuan.</b> Digunakan pada panel filter tanggal yang memerlukan pilihan bulan,
	 * misalnya laporan bulanan, rekap pembayaran, atau jadwal per-bulan.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Bila {@code combobox} null, membuat instance Combobox baru.</li>
	 *   <li>Mengambil bulan saat ini (0-based) dari {@code Calendar.getInstance()}.</li>
	 *   <li>Mengiterasi larik {@code BULAN} (konstanta statis berisi nama bulan Bahasa Indonesia),
	 *       membuat {@code Comboitem} dengan label nama bulan dan value (1-based, sesuai
	 *       {@code Calendar.MONTH + 1}).</li>
	 *   <li>Memilih item yang cocok dengan bulan saat ini.</li>
	 * </ol>
	 * </p>
	 *
	 * @param combobox Combobox ZK yang akan diisi; bila null, dibuat baru
	 * @return Combobox yang sudah terisi daftar bulan dengan pilihan bulan saat ini
	 */
	public static Combobox generateBulan(Combobox combobox) {
		if (combobox == null) {
			combobox = new Combobox();
		}
		int bulan = Calendar.getInstance().get(Calendar.MONTH);
		for (int i = 0; i < BULAN.length; i++) {
			Comboitem comboitem = new Comboitem(BULAN[i]);
			comboitem.setValue(i + 1);
			combobox.appendChild(comboitem);
			if (i == bulan) {
				combobox.setSelectedItem(comboitem);
			}
		}
		return combobox;
	}

	/**
	 * Mengisi atau membuat {@code Combobox} ZK dengan daftar tahun dari (tahun sekarang - 10)
	 * hingga (tahun sekarang + 10), dan memilih tahun saat ini secara otomatis.
	 *
	 * <p><b>Tujuan.</b> Digunakan pada panel filter rentang tahun akademik, laporan tahunan,
	 * atau pilihan tahun pada form umum.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Bila null, membuat Combobox baru.</li>
	 *   <li>Mengambil tahun saat ini dari {@code Calendar.getInstance()}.</li>
	 *   <li>Mengisi dari {@code (tahun-10)} hingga {@code (tahun+9)} (eksklusif), masing-masing
	 *       sebagai Comboitem dengan label dan value bertipe Integer.</li>
	 *   <li>Memilih item yang cocok dengan tahun saat ini.</li>
	 * </ol>
	 * </p>
	 *
	 * @param combobox Combobox ZK yang akan diisi; bila null, dibuat baru
	 * @return Combobox yang sudah terisi daftar tahun dengan pilihan tahun saat ini
	 */
	public static Combobox generateTahun(Combobox combobox) {
		if (combobox == null) {
			combobox = new Combobox();
		}
		int tahun = Calendar.getInstance().get(Calendar.YEAR);
		for (int i = (tahun - 10); i < (tahun + 10); i++) {
			Comboitem comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			combobox.appendChild(comboitem);
			if (i == tahun) {
				combobox.setSelectedItem(comboitem);
			}
		}
		return combobox;
	}

	/**
	 * Mengambil lokasi ({@code Lokasi}) dari sesi pengguna yang sedang aktif.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonCurrentSessionHelper.getCurrentLokasi()} yang membaca atribut lokasi
	 * dari sesi HTTP aktif.</p>
	 *
	 * @return objek {@code Lokasi} pengguna saat ini; null bila tidak ada lokasi dalam sesi
	 */
	public static Lokasi getCurrentLokasi() {
		return CommonCurrentSessionHelper.getCurrentLokasi();
	}

	/**
	 * Menghasilkan kode unik (misalnya nomor urut) untuk entitas dari kelas tertentu
	 * dengan panjang digit yang ditentukan.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonGenerateHelper.generateCode(class1, panjang)} yang menghitung nilai max
	 * dari kolom kode entitas lalu mem-format angka berikutnya dengan zero-padding.</p>
	 *
	 * @param class1  kelas entitas yang kodenya di-generate
	 * @param panjang panjang kode dalam digit (misalnya 6 untuk "000001")
	 * @return string kode unik yang di-format
	 */
	public static String generateCode(Class<?> class1, int panjang) {
		return CommonGenerateHelper.generateCode(class1, panjang);
	}

	/**
	 * Menghasilkan kode unik dengan awalan dan panjang digit tertentu.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonGenerateHelper.generateCode(class1, panjang, awalan)} — awalan
	 * ditambahkan sebagai prefix di depan angka urut.</p>
	 *
	 * @param class1  kelas entitas yang kodenya di-generate
	 * @param panjang panjang bagian numerik kode
	 * @param awalan  prefix yang ditambahkan sebelum angka (misalnya "INV-")
	 * @return string kode unik dengan awalan
	 */
	public static String generateCode(Class<?> class1, int panjang, String awalan) {
		return CommonGenerateHelper.generateCode(class1, panjang, awalan);
	}

	/**
	 * Menghasilkan kode unik dengan awalan, panjang digit, dan lokasi tertentu.
	 *
	 * <p><b>Tujuan.</b> Mendukung penomoran per-lokasi/satuan kerja — misalnya kode surat
	 * per-kantor atau kode item per-gudang yang harus unik hanya dalam konteks lokasi
	 * tersebut.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonGenerateHelper.generateCode(class1, panjang, awalan, lokasi)}.</p>
	 *
	 * @param class1  kelas entitas
	 * @param panjang panjang bagian numerik
	 * @param awalan  prefix kode
	 * @param lokasi  lokasi/satuan kerja yang digunakan sebagai namespace kode
	 * @return string kode unik per-lokasi
	 */
	public static String generateCode(Class<?> class1, int panjang, String awalan, Lokasi lokasi) {
		return CommonGenerateHelper.generateCode(class1, panjang, awalan, lokasi);
	}

	/**
	 * Menghasilkan kode unik dengan awalan, panjang digit, lokasi, dan penambahan offset
	 * untuk menghindari konflik numbering.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonGenerateHelper.generateCode(class1, panjang, awalan, lokasi, penambahan)}.
	 * Parameter {@code penambahan} memungkinkan offset angka awal untuk menghindari duplikasi
	 * saat ada batching atau pre-alokasi kode.</p>
	 *
	 * @param class1     kelas entitas
	 * @param panjang    panjang bagian numerik
	 * @param awalan     prefix kode
	 * @param lokasi     lokasi/satuan kerja
	 * @param penambahan offset penambahan dari nilai max saat ini
	 * @return string kode unik dengan offset
	 */
	public static String generateCode(Class<?> class1, int panjang, String awalan, Lokasi lokasi, Long penambahan) {
		return CommonGenerateHelper.generateCode(class1, panjang, awalan, lokasi, penambahan);
	}

	/**
	 * Mengambil nilai MAX dari kode numerik untuk entitas di lokasi tertentu.
	 *
	 * <p><b>Tujuan.</b> Mendukung pembuatan kode berurutan — mengetahui kode terakhir yang
	 * terpakai agar kode berikutnya bisa di-generate tanpa duplikasi.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonGenerateHelper.generateMaxByLokasi(class1, lokasi)} yang menjalankan
	 * query {@code max(id)} atau {@code max(kode)} untuk entitas dan lokasi tersebut.</p>
	 *
	 * @param class1 kelas entitas yang nilai maxnya dicari
	 * @param lokasi lokasi/satuan kerja sebagai konteks pencarian
	 * @return nilai max numerik dari kode yang sudah ada; null bila belum ada data
	 */
	public static Long generateMaxByLokasi(Class<?> class1, Lokasi lokasi) {
		return CommonGenerateHelper.generateMaxByLokasi(class1, lokasi);
	}


	/**
	 * Mengisi atau membuat {@code Combobox} ZK dengan daftar jenis pekerjaan standar
	 * untuk data orang tua/wali mahasiswa atau data kepegawaian.
	 *
	 * <p><b>Tujuan.</b> Menyediakan daftar pilihan pekerjaan yang konsisten di seluruh
	 * form yang memerlukan informasi pekerjaan (mis. biodata mahasiswa, form PPDB, data wali)
	 * tanpa harus mengulang inisialisasi item di setiap tempat.</p>
	 *
	 * <p><b>Cara kerja.</b> Bila {@code pekerjaan} null, membuat Combobox baru. Menambahkan
	 * 11 item pekerjaan statis: BUMN, BURUH, PNS, PROFESI, PURNABAKTI, SWASTA, TGG KELUARGA,
	 * TNI, POLRI, WIRASWASTA, IBU RUMAH TANGGA. Label dan value identik (string pekerjaan).
	 * Tidak ada pilihan default yang dipilih otomatis.</p>
	 *
	 * @param pekerjaan Combobox ZK yang akan diisi; bila null, dibuat baru
	 * @return Combobox yang sudah terisi daftar pekerjaan statis
	 */
	public static Combobox initPekerjaan(Combobox pekerjaan) {
		if (pekerjaan == null) {
			pekerjaan = new Combobox();
		}
		Comboitem comboitem = new Comboitem("BUMN");
		comboitem.setValue("BUMN");
		pekerjaan.appendChild(comboitem);
		comboitem = new Comboitem("BURUH");
		comboitem.setValue("BURUH");
		pekerjaan.appendChild(comboitem);
		comboitem = new Comboitem("PNS");
		comboitem.setValue("PNS");
		pekerjaan.appendChild(comboitem);
		comboitem = new Comboitem("PROFESI");
		comboitem.setValue("PROFESI");
		pekerjaan.appendChild(comboitem);
		comboitem = new Comboitem("PURNABAKTI");
		comboitem.setValue("PURNABAKTI");
		pekerjaan.appendChild(comboitem);
		comboitem = new Comboitem("SWASTA");
		comboitem.setValue("SWASTA");
		pekerjaan.appendChild(comboitem);
		comboitem = new Comboitem("TGG KELUARGA");
		comboitem.setValue("TGG KELUARGA");
		pekerjaan.appendChild(comboitem);
		comboitem = new Comboitem("TNI");
		comboitem.setValue("TNI");
		pekerjaan.appendChild(comboitem);
		comboitem = new Comboitem("POLRI");
		comboitem.setValue("POLRI");
		pekerjaan.appendChild(comboitem);
		comboitem = new Comboitem("WIRASWASTA");
		comboitem.setValue("WIRASWASTA");
		pekerjaan.appendChild(comboitem);
		comboitem = new Comboitem("IBU RUMAH TANGGA");
		comboitem.setValue("IBU RUMAH TANGGA");
		pekerjaan.appendChild(comboitem);
		return pekerjaan;
	}

	/**
	 * Mengonversi string ke huruf kapital di awal setiap kata (Title Case) dalam
	 * Bahasa Indonesia.
	 *
	 * <p><b>Tujuan.</b> Menormalkan nama orang, nama kota, nama jabatan, atau teks lain
	 * yang seharusnya diformat dengan huruf kapital di setiap awal kata — misalnya mengubah
	 * "JOHN DOE" atau "john doe" menjadi "John Doe".</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Guard null/kosong: kembalikan {@code teks} apa adanya.</li>
	 *   <li>Memecah {@code teks} dengan whitespace ({@code "\\s+"}) menjadi larik kata.</li>
	 *   <li>Untuk tiap kata: mengambil karakter pertama lalu {@code Character.toUpperCase},
	 *       sisanya {@code substring(1).toLowerCase()}.</li>
	 *   <li>Menggabungkan kembali dengan spasi dan memangkas spasi di akhir.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Catatan.</b> Kata kosong (dari beberapa spasi berturutan) diabaikan karena
	 * {@code split("\\s+")} tidak menghasilkan token kosong di awal bila tidak ada spasi
	 * awal. Huruf kapital dari karakter Unicode non-ASCII didukung lewat
	 * {@code Character.toUpperCase}.</p>
	 *
	 * @param teks teks yang akan dikonversi; boleh null (dikembalikan apa adanya)
	 * @return string dengan huruf kapital di awal setiap kata; null bila input null
	 */
	public static String kapitalAwalKata(String teks) {
		/** Mengonversi string ke huruf kapital hanya di depan setiap kata. */
		if (teks == null || teks.isEmpty()) {
			return teks;
		}

		StringBuilder hasil = new StringBuilder();
		String[] kataKata = teks.split("\\s+");

		for (String kata : kataKata) {
			if (!kata.isEmpty()) {
				hasil.append(Character.toUpperCase(kata.charAt(0))).append(kata.substring(1).toLowerCase()).append(" ");
			}
		}

		return hasil.toString().trim();
	}

	/**
	 * Mengekstrak semua URL gambar (atribut {@code src} dari tag {@code <img>}) yang ada
	 * dalam string HTML.
	 *
	 * <p><b>Tujuan.</b> Digunakan untuk mendeteksi dan mengumpulkan referensi gambar dalam
	 * konten HTML (misalnya deskripsi kursus, pengumuman, atau konten e-Learning yang
	 * disimpan sebagai HTML) — berguna untuk pra-unduh, validasi tautan rusak, atau
	 * migrasi aset.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Mem-parse string {@code html} menggunakan {@code Jsoup.parse(html)}.</li>
	 *   <li>Menyeleksi semua elemen {@code img} via {@code doc.select("img")}.</li>
	 *   <li>Untuk tiap elemen {@code img}, mengambil nilai atribut {@code src}; bila tidak
	 *       kosong, menambahkannya ke list.</li>
	 * </ol>
	 * </p>
	 *
	 * @param html string HTML yang akan diparsing; boleh null (mengembalikan list kosong)
	 * @return List URL gambar; list kosong bila tidak ada tag img atau src kosong
	 */
	public static List<String> ambilUrlGambarDariHtml(String html) {
		List<String> urlGambar = new ArrayList<String>();
		Document doc = Jsoup.parse(html);
		Elements gambar = doc.select("img");

		for (Element img : gambar) {
			String url = img.attr("src");
			if (!url.isEmpty()) {
				urlGambar.add(url);
			}
		}

		return urlGambar;
	}

	/**
	 * Menginisialisasi komponen {@code Paging} ZK untuk menampilkan hasil audit Hibernate Envers
	 * dengan jumlah halaman yang dihitung dari total baris {@code AuditQuery}.
	 *
	 * <p><b>Tujuan.</b> Mendukung tampilan riwayat perubahan data (audit trail) dengan navigasi
	 * halaman, agar tidak semua revisi dimuat sekaligus dan performa tetap terjaga.</p>
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Menetapkan ukuran halaman via {@code paging.setPageSize(Common.ROWS_COUNT_ON_PAGE)}.</li>
	 *   <li>Menetapkan increment halaman: 5 pada mobile, 10 pada desktop.</li>
	 *   <li>Menetapkan mold "os" (paging style).</li>
	 *   <li>Menjalankan {@code auditQuery.addProjection(AuditEntity.revisionNumber().count()).getSingleResult()}
	 *       untuk menghitung total revisi; error ditangani oleh {@link #tampilErrorJikaAdmin(Exception)}.</li>
	 *   <li>Menetapkan {@code detailed(true)}, total size, dan visibility paging (disembunyikan
	 *       bila data tidak melebihi satu halaman).</li>
	 *   <li>Menyesuaikan tinggi komponen induk ({@code South} atau {@code Row}) menjadi
	 *       "30px" (bila perlu) atau "0px" (bila tidak perlu paging).</li>
	 * </ol>
	 * </p>
	 *
	 * @param paging     komponen Paging ZK yang akan diinisialisasi; tidak boleh null
	 * @param auditQuery AuditQuery Hibernate Envers yang sudah difilter dengan kriteria audit
	 */
	public static void initPaging(Paging paging, AuditQuery auditQuery) {
		try {
			paging.setPageSize(Common.ROWS_COUNT_ON_PAGE);
			paging.setPageIncrement(Common.isMobile() ? 5 : 10);
			paging.setMold("os");
			int size = Common.ROWS_COUNT_ON_PAGE;

			try {
				size = ((Number) auditQuery.addProjection(AuditEntity.revisionNumber().count()).getSingleResult())
						.intValue();
			} catch (Exception e) {
				tampilErrorJikaAdmin(e);
			}
			// // System.out.println("size = " + size);
			paging.setDetailed(true);
			paging.setTotalSize(size);
			paging.setVisible(size > Common.ROWS_COUNT_ON_PAGE);
			try {
				if (paging.getParent() instanceof South)
					((South) paging.getParent()).setHeight(size > Common.ROWS_COUNT_ON_PAGE ? "30px" : "0px");
				if (paging.getParent() instanceof Row)
					((Row) paging.getParent()).setHeight(size > Common.ROWS_COUNT_ON_PAGE ? "30px" : "0px");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:19777");
				// Common.tampilErrorJikaAdmin(e);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Common.java:19780");
			// TODO: handle exception
		}
	}

	/**
	 * Memeriksa apakah tanggal {@code dateToCheck} berada dalam rentang
	 * [{@code startDate}, {@code endDate}] (inklusif di kedua ujung).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonValidationHelper.isDateBetween(dateToCheck, startDate, endDate)}.</p>
	 *
	 * @param dateToCheck tanggal yang diperiksa
	 * @param startDate   tanggal awal rentang (inklusif)
	 * @param endDate     tanggal akhir rentang (inklusif)
	 * @return {@code true} bila {@code dateToCheck} berada dalam rentang
	 */
	public static boolean isDateBetween(Date dateToCheck, Date startDate, Date endDate) {
		return CommonValidationHelper.isDateBetween(dateToCheck, startDate, endDate);
	}

	/**
	 * Method untuk mengecek apakah field ada di dalam class. Menggunakan
	 * getDeclaredField agar bisa mendeteksi field private sekalipun.
	 */
	@SuppressWarnings("rawtypes")
	public static boolean isFieldExist(Class clazz, String fieldName) {
		try {
			// getDeclaredField akan melempar NoSuchFieldException jika tidak ketemu
			clazz.getDeclaredField(fieldName);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Menghitung selisih durasi dalam menit antara dua waktu (String). Format waktu
	 * input harus "HH.mm" (contoh: "09.00").
	 *
	 * @param waktuMulai   Waktu awal (contoh: "09.00")
	 * @param waktuSelesai Waktu akhir (contoh: "11.30")
	 * @return Durasi dalam menit (long)
	 */
	public static long hitungDurasiMenit(String waktuMulai, String waktuSelesai) {
		// Pola "HH.mm" berarti Jam (00-23) dan Menit.
		SimpleDateFormat format = new SimpleDateFormat("HH.mm");

		long durasiMenit = 0;

		try {
			// Konversi String ke Object Date
			Date dateMulai = format.parse(waktuMulai);
			Date dateSelesai = format.parse(waktuSelesai);

			// Dapatkan waktu dalam milidetik
			long millisMulai = dateMulai.getTime();
			long millisSelesai = dateSelesai.getTime();

			// Hitung selisih milidetik
			long selisihMillis = millisSelesai - millisMulai;

			// Konversi milidetik ke menit (1 menit = 60.000 ms)
			durasiMenit = selisihMillis / (60 * 1000);

		} catch (Exception e) {
			System.err.println("Format waktu salah. Gunakan format HH.mm");
			tampilErrorJikaAdmin(e);
		}

		return durasiMenit;
	}

	/**
	 * Memeriksa apakah string {@code json} merupakan JSON object yang valid.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonValidationHelper.isValidJsonObject(json)} yang mencoba mem-parse string
	 * sebagai JSONObject dan mengembalikan {@code true} bila berhasil.</p>
	 *
	 * @param json string yang diperiksa; boleh null
	 * @return {@code true} bila string adalah JSON object yang valid
	 */
	public static boolean isValidJsonObject(String json) {
		return CommonValidationHelper.isValidJsonObject(json);
	}

	/**
	 * Mem-parse string menjadi {@code JSONObject} secara aman, mengembalikan {@code null}
	 * bila string null, kosong, atau bukan JSON object yang valid.
	 *
	 * <p><b>Cara kerja.</b> Guard null/kosong, lalu {@code new JSONObject(json)}; exception
	 * ditangkap dan mengembalikan null tanpa melempar. Pasangan dari {@link #validJsonArray}.</p>
	 *
	 * @param json string yang akan di-parse sebagai JSONObject; boleh null
	 * @return JSONObject bila valid; null bila null/kosong/tidak valid
	 */
	public static JSONObject validJsonObject(String json) {
		try {
			if (json == null || json.trim().isEmpty()) {
				return null;
			}
			return new JSONObject(json);
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/Common.java:19883");
		}
		return null;
	}

	/**
	 * Mem-parse string menjadi {@link JSONArray} secara aman, mengembalikan {@code null} bila tidak valid.
	 *
	 * <p><b>Tujuan.</b> Menyederhanakan penanganan input JSON dari sumber yang tak terjamin (payload
	 * API, kolom teks, parameter) tanpa membebani pemanggil dengan try/catch berulang. Bila string
	 * bukan array JSON yang sah, pemanggil cukup memeriksa hasil {@code null}.</p>
	 *
	 * <p><b>Cara kerja.</b> Mengembalikan {@code null} bila {@code json} null/kosong; selain itu
	 * mencoba {@code new JSONArray(json)}. Setiap kegagalan parsing ditangkap dan menghasilkan
	 * {@code null} (tanpa melempar).</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code json} = string yang diharapkan berupa array JSON.
	 * Mengembalikan {@link JSONArray} bila valid, atau {@code null} bila null/kosong/tidak valid.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pasangan {@code validJsonObject} tersedia untuk objek JSON. Karena
	 * exception sengaja ditelan, jangan memakai method ini bila perlu membedakan "kosong" dari
	 * "format salah"—keduanya menghasilkan {@code null}.</p>
	 *
	 * @param json string yang diharapkan berupa array JSON
	 * @return {@link JSONArray} valid, atau {@code null}
	 */
	public static JSONArray validJsonArray(String json) {
		try {
			if (json == null || json.trim().isEmpty()) {
				return null;
			}
			return new JSONArray(json);
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/Common.java:19915");
		}
		return null;
	}

	/**
	 * Mengambil entitas {@link Konfigurasi} bernama {@code nama}, atau membuat/memakai default bila
	 * belum ada (overload paling umum).
	 *
	 * <p><b>Tujuan.</b> Titik akses utama untuk parameter sistem yang dapat diubah operator tanpa
	 * mengganti kode (mis. flag fitur, batas waktu, alamat email pengirim). Memusatkan pembacaan
	 * konfigurasi memastikan perilaku seragam dan ber-cache.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke overload lengkap
	 * {@link #getKonfigurasi(String, String, String, String, String)} dengan {@code info1/info2/info3}
	 * kosong—yakni konfigurasi global (tanpa konteks tambahan). Overload lengkap itulah yang membaca
	 * dari DB/cache dan menerapkan {@code defaultValue} bila entri belum ada.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code nama} = kunci konfigurasi; {@code defaultValue} = nilai
	 * dipakai bila konfigurasi belum ada. Mengembalikan entitas {@link Konfigurasi} (gunakan
	 * {@code getNilai()} untuk nilainya)—idealnya tidak null karena default diterapkan.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Untuk konfigurasi yang bergantung konteks (tahun akademik, jurusan,
	 * dll.) pakai overload yang menerima parameter konteks. Hindari memetakan nama konfigurasi secara
	 * hardcode di banyak tempat—definisikan konstanta.</p>
	 *
	 * @param nama         kunci konfigurasi
	 * @param defaultValue nilai default bila belum ada
	 * @return entitas {@link Konfigurasi} terkait
	 */
	public static Konfigurasi getKonfigurasi(String nama, String defaultValue) {
		return getKonfigurasi(nama, defaultValue, "", "", "");
	}

	/**
	 * <h3>Apakah pengguna aktif berhak menekan tombol "Upload data" pada suatu modul</h3>
	 *
	 * <p><b>Tujuan.</b> Menyatukan (sentralisasi) logika pengecekan hak akses untuk tombol
	 * <i>bulk import</i> / "Upload (maks &hellip; Mb)" yang tersebar di beberapa halaman master
	 * data — antara lain daftar Siswa ({@code SiswaAction}), Calon Siswa
	 * ({@code CalonSiswaAction}), Mahasiswa ({@code MahasiswaAction}), dan Registrasi PMB
	 * ({@code CetakRegistrasiAction}). Sebelum helper ini ada, kelayakan tombol upload
	 * ditentukan secara terpisah oleh kombinasi flag {@code edit}/{@code delete},
	 * {@code getApakahAdmin()}, atau konfigurasi aktif/non-aktif yang berbeda-beda; akibatnya
	 * administrator sulit membatasi siapa yang boleh melakukan impor massal secara konsisten.
	 * Helper ini menjadikan penentu utamanya satu <b>konfigurasi berbasis daftar role</b>
	 * sehingga admin cukup mengatur satu tempat (menu Konfigurasi) untuk membuka/menutup akses
	 * upload per modul.</p>
	 *
	 * <p><b>Nilai konfigurasi &amp; default.</b> Parameter {@code konfigurasiKey} adalah nama
	 * kunci konfigurasi (mis. {@code hak_akses_upload_data_siswa}). Nilainya berupa <b>daftar
	 * roleId dipisah koma</b>, contoh {@code "am,operator"}. Bila baris konfigurasi belum
	 * pernah dibuat, {@link #getKonfigurasi(String, String)} akan mengembalikan nilai default
	 * yang di-hardcode di sini, yaitu {@link ais.database.model.Tbmrole#ADMINISTRATOR}
	 * (kode {@code "am"}). Dengan demikian <b>secara bawaan hanya Administrator</b> yang melihat
	 * dan dapat memakai tombol upload; role lain harus ditambahkan administrator ke dalam
	 * daftar melalui halaman Konfigurasi. Nilai kosong/whitespace diperlakukan sama seperti
	 * "belum diisi" dan otomatis jatuh kembali ke default {@code "am"}, mencegah kondisi di mana
	 * nilai kosong tak sengaja mengunci semua orang termasuk admin.</p>
	 *
	 * <p><b>Token khusus.</b> Selain roleId eksplisit, daftar mendukung token wildcard yang
	 * membuka akses untuk <i>semua</i> role: {@code "*"}, {@code "semua"}, dan {@code "all"}
	 * (tidak <i>case-sensitive</i>). Ini memudahkan skenario "izinkan semua pengguna yang
	 * berwenang mengelola master data" tanpa harus menuliskan setiap roleId. Pencocokan roleId
	 * pengguna terhadap daftar juga dilakukan <i>case-insensitive</i> memakai
	 * {@code equalsIgnoreCase} agar toleran terhadap perbedaan huruf besar/kecil pada data role.</p>
	 *
	 * <p><b>Cara kerja (langkah demi langkah).</b> (1) Validasi {@code konfigurasiKey}; bila null
	 * atau kosong, kembalikan {@code false} (fail-closed) karena pemanggilan tidak jelas
	 * modulnya. (2) Ambil pengguna aktif via {@link #getCurrentUser()} lalu turunkan
	 * {@code roleId} dari {@code hakAkses().getRoleId()} dengan penjagaan null berlapis. (3) Baca
	 * nilai konfigurasi via {@link #getKonfigurasi(String, String)} dengan default {@code "am"};
	 * bila pembacaan gagal atau menghasilkan nilai kosong, gunakan default. (4) Pecah daftar
	 * dengan pemisah koma, lalu iterasi tiap potongan: lewati potongan kosong, dan kembalikan
	 * {@code true} begitu ditemukan token wildcard atau roleId yang cocok. (5) Bila tak ada yang
	 * cocok, kembalikan {@code false}.</p>
	 *
	 * <p><b>Strategi kegagalan (fail-closed).</b> Seluruh badan dibungkus {@code try/catch}. Bila
	 * terjadi kesalahan tak terduga (mis. konteks eksekusi ZK tidak tersedia atau sesi terputus),
	 * method mengembalikan {@code false} sehingga tombol upload disembunyikan. Pilihan ini
	 * disengaja: fitur impor massal bersifat sensitif (dapat menimpa/menambah ratusan baris data),
	 * sehingga lebih aman menyembunyikan tombol saat status hak akses tak dapat dipastikan
	 * daripada menampilkannya secara keliru. Karena {@code getKonfigurasi} selalu punya jalur
	 * default {@code "am"}, administrator yang login normal tetap lolos pada kondisi wajar dan
	 * tidak akan terkunci oleh gangguan sesaat pada baris konfigurasi.</p>
	 *
	 * <p><b>Cara pakai di pemanggil.</b> Helper ini dirancang untuk di-<i>AND</i>-kan ke dalam
	 * ekspresi {@code setVisible(...)} tombol upload yang sudah ada — bukan menggantikannya —
	 * agar semua syarat lama (mis. {@code add.isVisible()}, {@code edit && delete},
	 * konfigurasi aktif/non-aktif) tetap dihormati dan hanya <i>ditambah</i> lapisan pembatasan
	 * role. Contoh: {@code upload.setVisible(syaratLama && Common.bolehUploadDataKonfigurasi(
	 * "hak_akses_upload_data_siswa"));}. Karena hanya membaca konfigurasi dan pengguna aktif,
	 * method aman dipanggil berkali-kali serta tidak membuka sesi Hibernate sendiri di luar yang
	 * sudah dilakukan {@code getKonfigurasi}/{@code getCurrentUser}.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Setiap kunci baru yang dipakai di sini sebaiknya ikut didaftarkan di
	 * {@code KonfigurasiNewAction} (bagian "Hak Akses Tombol Upload Data") agar administrator dapat
	 * menyuntingnya lewat UI. Kompatibel Java 1.7 (tanpa lambda/stream). Bila format daftar role di
	 * masa depan berubah (mis. memakai pemisah selain koma), cukup ubah logika pemecahan di sini.</p>
	 *
	 * @param konfigurasiKey nama kunci konfigurasi yang menyimpan daftar roleId berhak (dipisah koma)
	 * @return {@code true} bila role pengguna aktif termasuk dalam daftar (atau daftar berisi
	 *         wildcard); {@code false} bila tidak berhak, tidak login, atau terjadi kesalahan
	 */
	public static boolean bolehUploadDataKonfigurasi(String konfigurasiKey) {
		// Default bawaan "am" (hanya Administrator). Gunakan overload berdefault untuk modul yang
		// dioperasikan peran lain (mis. Admin Sekolah) agar tidak terkunci oleh default "am".
		return bolehUploadDataKonfigurasi(konfigurasiKey, ais.database.model.Tbmrole.ADMINISTRATOR);
	}

	/**
	 * Varian {@link #bolehUploadDataKonfigurasi(String)} dengan nilai default daftar-role yang dapat
	 * ditentukan pemanggil. Berguna untuk halaman yang dioperasikan peran non-"am" — mis. daftar
	 * Siswa yang dikelola <b>Admin Sekolah</b> (yang roleId-nya bukan {@code "am"} dan bersifat
	 * kustom per instalasi), sehingga default {@code "*"} (semua role yang sudah lolos syarat
	 * edit/hapus) lebih tepat daripada mengunci ke Administrator saja. Lihat dokumentasi lengkap
	 * pada {@link #bolehUploadDataKonfigurasi(String)}.
	 *
	 * @param konfigurasiKey nama kunci konfigurasi (daftar roleId dipisah koma; {@code "*"} = semua)
	 * @param defaultRole    nilai default bila baris konfigurasi belum ada / gagal dibaca
	 * @return {@code true} bila role pengguna aktif termasuk dalam daftar (atau daftar mengandung
	 *         wildcard {@code "*"}/{@code "semua"}/{@code "all"})
	 */
	public static boolean bolehUploadDataKonfigurasi(String konfigurasiKey, String defaultRole) {
		if (defaultRole == null || defaultRole.trim().length() == 0) {
			defaultRole = ais.database.model.Tbmrole.ADMINISTRATOR;
		}
		try {
			if (konfigurasiKey == null || konfigurasiKey.trim().length() == 0) {
				return false;
			}
			ais.database.model.Tbmuser pengguna = getCurrentUser();
			String roleId = "";
			String kodeRole = "";
			if (pengguna != null && pengguna.hakAkses() != null && pengguna.hakAkses().getRoleId() != null) {
				roleId = pengguna.hakAkses().getRoleId().trim();
			}
			if (pengguna != null && pengguna.hakAkses() != null && pengguna.hakAkses().getKode() != null) {
				kodeRole = pengguna.hakAkses().getKode().trim();
			}
			String daftarRole = defaultRole;
			try {
				Konfigurasi konfig = getKonfigurasi(konfigurasiKey, defaultRole);
				if (konfig != null && konfig.getNilai() != null && konfig.getNilai().trim().length() > 0) {
					daftarRole = konfig.getNilai();
				}
			} catch (Exception e) {
				daftarRole = defaultRole;
			}
			String[] bagian = daftarRole.split(",");
			for (int i = 0; i < bagian.length; i++) {
				String r = bagian[i] == null ? "" : bagian[i].trim();
				if (r.length() == 0) {
					continue;
				}
				if ("*".equals(r) || "semua".equalsIgnoreCase(r) || "all".equalsIgnoreCase(r)
						|| r.equalsIgnoreCase(roleId) || r.equalsIgnoreCase(kodeRole)) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			// fail-closed: sembunyikan tombol upload bila status hak akses tak dapat dipastikan
			return false;
		}
	}

	/**
	 * <h3>Apakah sebuah konfigurasi bergaya aktif/non-aktif bernilai "aktif"</h3>
	 *
	 * <p><b>Tujuan.</b> Menyederhanakan dan menyeragamkan pola pembacaan konfigurasi biner
	 * (hidup/mati) yang paling sering dipakai di seluruh aplikasi. Sebelum ada helper ini,
	 * pemeriksaan sebuah sakelar konfigurasi ditulis panjang dan berulang, mis.
	 * {@code Common.bolehKonfigurasi("kunci")}.
	 * Pola verbose seperti itu tersebar ribuan kali sehingga rawan salah ketik (mis. lupa
	 * {@code .getNilai()}, keliru membandingkan ke {@code TIDAK_AKTIF}, atau memakai default yang
	 * tidak konsisten), sulit dibaca, dan menyulitkan perubahan logika terpusat di kemudian hari.
	 * Dengan helper ini pemanggil cukup menulis {@code Common.bolehKonfigurasi("kunci")} — jauh
	 * lebih ringkas, konsisten, dan mudah dipelihara.</p>
	 *
	 * <p><b>Semantik &amp; default.</b> Overload satu argumen ini menganggap nilai <b>default
	 * "aktif"</b> ({@link Konfigurasi#AKTIF}); artinya bila baris konfigurasi belum pernah dibuat,
	 * fitur dianggap <i>menyala</i>. Ini menggantikan idiom lama
	 * {@code bolehKonfigurasi(kunci)} secara
	 * setara. Untuk konfigurasi yang secara bawaan harus <i>mati</i>, gunakan overload dua argumen
	 * {@link #bolehKonfigurasi(String, String)} dengan default {@link Konfigurasi#TIDAK_AKTIF} agar
	 * setara dengan {@code getKonfigurasi(kunci, Konfigurasi.TIDAK_AKTIF).getNilai().equals(
	 * Konfigurasi.AKTIF)}.</p>
	 *
	 * <p><b>Cara kerja.</b> Method mendelegasikan ke {@link #bolehKonfigurasi(String, String)}
	 * dengan default {@link Konfigurasi#AKTIF}. Overload dua argumen: (1) memvalidasi {@code kunci}
	 * (null/kosong &rarr; {@code false}); (2) membaca nilai melalui {@link #getKonfigurasi(String,
	 * String)} memakai {@code defaultNilai} sebagai cadangan; (3) bila pembacaan gagal atau nilai
	 * {@code null}, gunakan {@code defaultNilai}; (4) mengembalikan hasil perbandingan
	 * <i>case-insensitive</i> terhadap {@link Konfigurasi#AKTIF} setelah di-{@code trim}.</p>
	 *
	 * <p><b>Kenapa case-insensitive.</b> Perbandingan memakai {@code equalsIgnoreCase} (bukan
	 * {@code equals}) agar toleran terhadap variasi huruf besar/kecil dan spasi pinggir pada nilai
	 * yang tersimpan (mis. {@code "AKTIF"}, {@code " aktif "}). Ini strictly lebih longgar daripada
	 * {@code equals} sehingga aman menggantikan kedua bentuk lama ({@code equals} maupun
	 * {@code equalsIgnoreCase}) tanpa mengubah hasil pada data yang normal (nilai baku "aktif"/
	 * "tidak_aktif" huruf kecil), sekaligus mencegah "false" tak sengaja akibat perbedaan kapital.</p>
	 *
	 * <p><b>Strategi kegagalan (fail-closed).</b> Seluruh badan dibungkus {@code try/catch}; bila
	 * terjadi kesalahan tak terduga, method mengembalikan {@code false} (fitur dianggap mati)
	 * sehingga tidak pernah melempar exception ke pemanggil. Karena {@code getKonfigurasi} selalu
	 * memiliki jalur default, kondisi wajar (baris belum ada) tetap menghasilkan nilai default yang
	 * benar dan tidak terpengaruh oleh gangguan sesaat.</p>
	 *
	 * <p><b>Cara pakai &amp; pemeliharaan.</b> Gantikan langsung idiom lama, termasuk pada ekspresi
	 * bernegasi: {@code !getKonfigurasi(k, AKTIF).getNilai().equals(AKTIF)} menjadi
	 * {@code !Common.bolehKonfigurasi(k)}. Method hanya membaca konfigurasi sehingga aman dipanggil
	 * berkali-kali dan tidak membuka sesi Hibernate sendiri di luar yang dilakukan
	 * {@code getKonfigurasi}. Kompatibel Java 1.7 (tanpa lambda/stream). Bila kelak semantik
	 * "aktif" perlu diubah terpusat (mis. menambah token "1"/"true"), cukup sesuaikan overload dua
	 * argumen di sini tanpa menyentuh ribuan pemanggil.</p>
	 *
	 * @param kunci nama kunci konfigurasi yang diperiksa
	 * @return {@code true} bila nilai konfigurasi (atau default "aktif") sama dengan
	 *         {@link Konfigurasi#AKTIF} secara case-insensitive; {@code false} bila mati, kunci
	 *         kosong, atau terjadi kesalahan
	 */
	public static boolean bolehKonfigurasi(String kunci) {
		return bolehKonfigurasi(kunci, Konfigurasi.AKTIF);
	}

	/**
	 * Varian {@link #bolehKonfigurasi(String)} dengan nilai default yang dapat ditentukan pemanggil.
	 *
	 * <p>Gunakan {@link Konfigurasi#TIDAK_AKTIF} sebagai {@code defaultNilai} untuk konfigurasi yang
	 * secara bawaan harus mati — setara dengan idiom lama {@code getKonfigurasi(kunci,
	 * Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF)}. Lihat dokumentasi lengkap pada
	 * {@link #bolehKonfigurasi(String)} untuk perilaku, perbandingan case-insensitive, dan strategi
	 * fail-closed.</p>
	 *
	 * @param kunci        nama kunci konfigurasi yang diperiksa
	 * @param defaultNilai nilai yang dipakai bila baris konfigurasi belum ada / gagal dibaca
	 * @return {@code true} bila nilai efektif sama dengan {@link Konfigurasi#AKTIF} (case-insensitive)
	 */
	public static boolean bolehKonfigurasi(String kunci, String defaultNilai) {
		try {
			if (kunci == null || kunci.trim().length() == 0) {
				return false;
			}
			String nilai = defaultNilai;
			try {
				Konfigurasi konfig = getKonfigurasi(kunci, defaultNilai);
				if (konfig != null && konfig.getNilai() != null) {
					nilai = konfig.getNilai();
				}
			} catch (Exception e) {
				nilai = defaultNilai;
			}
			return nilai != null && nilai.trim().equalsIgnoreCase(Konfigurasi.AKTIF);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Mengambil {@link Konfigurasi} bernama {@code nama} memakai {@link Session} yang diberikan
	 * pemanggil.
	 *
	 * <p><b>Tujuan.</b> Sama seperti {@link #getKonfigurasi(String, String)}, tetapi memakai session
	 * Hibernate yang sudah dimiliki pemanggil—berguna di dalam transaksi/thread yang ingin konsisten
	 * dengan session-nya sendiri (mis. proses latar, batch) alih-alih session konteks default.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke overload lengkap berbasis session
	 * {@link #getKonfigurasi(Session, String, String, String, String, String)} dengan info kosong
	 * (konfigurasi global).</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code session} = session yang dipakai membaca; {@code nama} =
	 * kunci; {@code defaultValue} = nilai default. Mengembalikan entitas {@link Konfigurasi}.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Pakai varian ber-session pada konteks non-ZK/transaksional agar tidak
	 * tercampur session lain. Pastikan siklus hidup {@code session} dikelola pemanggil sesuai aturan
	 * {@code HibernateUtil}.</p>
	 *
	 * @param session      session Hibernate milik pemanggil
	 * @param nama         kunci konfigurasi
	 * @param defaultValue nilai default bila belum ada
	 * @return entitas {@link Konfigurasi} terkait
	 */
	public static Konfigurasi getKonfigurasi(Session session, String nama, String defaultValue) {
		return getKonfigurasi(session, nama, defaultValue, "", "", "");
	}

	/**
	 * Mengambil {@link Konfigurasi} bernama {@code nama} dengan konteks info1/info2/info3
	 * (misalnya kode satker, kode lokasi, dll.) dari cache/DB.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code KonfigurasiManager.getKonfigurasi(nama, defaultValue, info1, info2, info3)}.</p>
	 *
	 * @param nama         kunci konfigurasi
	 * @param defaultValue nilai default bila belum ada
	 * @param info1        konteks tambahan 1 (misalnya kode satker)
	 * @param info2        konteks tambahan 2
	 * @param info3        konteks tambahan 3
	 * @return entitas Konfigurasi
	 */
	public static Konfigurasi getKonfigurasi(String nama, String defaultValue, String info1, String info2,
			String info3) {
		return KonfigurasiManager.getKonfigurasi(nama, defaultValue, info1, info2, info3);
	}

	/**
	 * Mengambil {@link Konfigurasi} menggunakan sesi Hibernate yang diberikan, dengan
	 * konteks info1/info2/info3.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code KonfigurasiManager.getKonfigurasi(session, nama, defaultValue, info1, info2, info3)}.</p>
	 *
	 * @param session      sesi Hibernate milik pemanggil
	 * @param nama         kunci konfigurasi
	 * @param defaultValue nilai default
	 * @param info1        konteks tambahan 1
	 * @param info2        konteks tambahan 2
	 * @param info3        konteks tambahan 3
	 * @return entitas Konfigurasi
	 */
	public static Konfigurasi getKonfigurasi(Session session, String nama, String defaultValue, String info1,
			String info2, String info3) {
		return KonfigurasiManager.getKonfigurasi(session, nama, defaultValue, info1, info2, info3);
	}

	/**
	 * Memproses dan menyimpan konfigurasi akademik berbasis semester, angkatan, jurusan, dan
	 * program studi; membuat entri baru bila belum ada.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code KonfigurasiManager.prosesKonfigurasi(nama, defaultValue, semester, angkatan, jurusan, pro, stsAwl)}.</p>
	 *
	 * @param nama         kunci konfigurasi
	 * @param defaultValue nilai default
	 * @param semester     semester akademik
	 * @param angkatan     tahun angkatan
	 * @param jurusan      jurusan terkait; boleh null untuk semua jurusan
	 * @param pro          kode program studi
	 * @param stsAwl       status awal mahasiswa
	 * @return entitas Konfigurasi yang sudah diproses/dibuat
	 */
	public static Konfigurasi prosesKonfigurasi(String nama, String defaultValue, Integer semester, Integer angkatan,
			Jurusan jurusan, String pro, String stsAwl) {
		return KonfigurasiManager.prosesKonfigurasi(nama, defaultValue, semester, angkatan, jurusan, pro, stsAwl);
	}

	/**
	 * Mengambil konfigurasi akademik yang spesifik untuk semester, angkatan, jurusan, program,
	 * dan status awal mahasiswa.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code KonfigurasiManager.getKonfigurasi(nama, defaultValue, semester, angkatan, jurusan, program, statusAwalMahasiswa)}.</p>
	 *
	 * @param nama                   kunci konfigurasi
	 * @param defaultValue           nilai default
	 * @param semester               semester akademik
	 * @param angkatan               tahun angkatan mahasiswa
	 * @param jurusan                jurusan; boleh null
	 * @param program                kode program studi
	 * @param statusAwalMahasiswa    status awal mahasiswa (reguler, transfer, dsb.)
	 * @return entitas Konfigurasi
	 */
	public static Konfigurasi getKonfigurasi(String nama, String defaultValue, Integer semester, Integer angkatan,
			Jurusan jurusan, String program, StatusAwalMahasiswa statusAwalMahasiswa) {
		return KonfigurasiManager.getKonfigurasi(nama, defaultValue, semester, angkatan, jurusan, program,
				statusAwalMahasiswa);
	}

	/**
	 * Mengambil konfigurasi akademik untuk program studi, jurusan, dan nilai kustom tertentu.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code KonfigurasiManager.getKonfigurasi(nama, defaultValue, program, jurusan, custom)}.</p>
	 *
	 * @param nama         kunci konfigurasi
	 * @param defaultValue nilai default
	 * @param program      kode program studi
	 * @param jurusan      jurusan; boleh null
	 * @param custom       nilai kustom tambahan untuk namespace konfigurasi
	 * @return entitas Konfigurasi
	 */
	public static Konfigurasi getKonfigurasi(String nama, String defaultValue, String program, Jurusan jurusan,
			String custom) {
		return KonfigurasiManager.getKonfigurasi(nama, defaultValue, program, jurusan, custom);
	}

	/**
	 * Mengambil konfigurasi berdasarkan jenis, tahun akademik, dan satu info tambahan,
	 * dengan default {@code TIDAK_AKTIF} dan tanpa konteks jurusan/fakultas.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@link #getKonfigurasi(String, String, String, String, Fakultas, Jurusan, String)}
	 * dengan semua parameter konteks null.</p>
	 *
	 * @param jenisKonfigurasi jenis/nama konfigurasi
	 * @param tahunAkademik    tahun akademik aktif (misalnya "2024/2025")
	 * @param info1            informasi tambahan 1 (misalnya semester)
	 * @return entitas Konfigurasi
	 */
	public static Konfigurasi getKonfigurasi(String jenisKonfigurasi, String tahunAkademik, String info1) {
		return getKonfigurasi(jenisKonfigurasi, tahunAkademik, info1, null, null, null, null);
	}

	/**
	 * Memeriksa dan mengambil konfigurasi yang dikaitkan dengan kalender akademik untuk
	 * jurusan/fakultas tertentu.
	 *
	 * <p><b>Tujuan.</b> Digunakan saat konfigurasi berlaku per-kalender akademik (tahun+semester)
	 * dan per-jurusan — misalnya batas SKS, jadwal pengisian KRS, atau pengaturan sidang.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code KonfigurasiManager.checkKonfigurasiDenganKalenderAkademik}.</p>
	 *
	 * @param session          sesi Hibernate aktif
	 * @param jenisKonfigurasi jenis konfigurasi
	 * @param tahunAkademik    tahun akademik
	 * @param smt              semester aktif
	 * @param masukDiSmt       semester masuk mahasiswa
	 * @param fakultas         fakultas; boleh null
	 * @param jurusan          jurusan; boleh null untuk semua jurusan
	 * @param program          kode program studi
	 * @return entitas Konfigurasi yang cocok
	 */
	public static Konfigurasi checkKonfigurasiDenganKalenderAkademik(Session session, String jenisKonfigurasi,
			String tahunAkademik, String smt, String masukDiSmt, Fakultas fakultas, Jurusan jurusan, String program) {
		return KonfigurasiManager.checkKonfigurasiDenganKalenderAkademik(session, jenisKonfigurasi, tahunAkademik, smt,
				masukDiSmt, fakultas, jurusan, program);
	}

	/**
	 * Memeriksa konfigurasi yang dikaitkan dengan kalender akademik AKTIF saat ini
	 * (tahun akademik tidak perlu ditentukan secara eksplisit).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code KonfigurasiManager.checkKonfigurasiDenganKalenderAkademikAktif} yang secara
	 * otomatis menggunakan tahun akademik dan semester aktif dari konfigurasi sistem.</p>
	 *
	 * @param session          sesi Hibernate aktif
	 * @param jenisKonfigurasi jenis konfigurasi
	 * @param masukDiSmt       semester masuk mahasiswa
	 * @param fakultas         fakultas; boleh null
	 * @param jurusan          jurusan; boleh null
	 * @param program          kode program studi
	 * @return entitas Konfigurasi dari kalender akademik aktif
	 */
	public static Konfigurasi checkKonfigurasiDenganKalenderAkademikAktif(Session session, String jenisKonfigurasi,
			String masukDiSmt, Fakultas fakultas, Jurusan jurusan, String program) {
		return KonfigurasiManager.checkKonfigurasiDenganKalenderAkademikAktif(session, jenisKonfigurasi, masukDiSmt,
				fakultas, jurusan, program);
	}

	/**
	 * Memeriksa konfigurasi yang dikaitkan dengan kalender akademik untuk yayasan/sekolah
	 * (digunakan di konteks sekolah, bukan perguruan tinggi).
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code KonfigurasiManager.checkKonfigurasiDenganKalenderAkademik} dengan parameter
	 * yayasan dan sekolah alih-alih fakultas dan jurusan.</p>
	 *
	 * @param session          sesi Hibernate aktif
	 * @param jenisKonfigurasi jenis konfigurasi
	 * @param tahunAkademik    tahun akademik
	 * @param smt              semester aktif
	 * @param masukDiSmt       semester masuk siswa
	 * @param yayasan          yayasan pemilik sekolah; boleh null
	 * @param sekolah          sekolah; boleh null
	 * @param program          kode program
	 * @return entitas Konfigurasi yang cocok
	 */
	public static Konfigurasi checkKonfigurasiDenganKalenderAkademik(Session session, String jenisKonfigurasi,
			String tahunAkademik, String smt, String masukDiSmt, Yayasan yayasan, Sekolah sekolah, String program) {
		return KonfigurasiManager.checkKonfigurasiDenganKalenderAkademik(session, jenisKonfigurasi, tahunAkademik, smt,
				masukDiSmt, yayasan, sekolah, program);
	}

	/**
	 * Mengambil konfigurasi berdasarkan jenis, tahun akademik, info1, semester masuk,
	 * fakultas, jurusan, dan program studi; dengan nilai default {@code TIDAK_AKTIF}.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@link #getKonfigurasi(String, String, String, String, Fakultas, Jurusan, String, String)}
	 * dengan {@code defaultNilai = Konfigurasi.TIDAK_AKTIF}.</p>
	 *
	 * @param jenisKonfigurasi jenis konfigurasi
	 * @param tahunAkademik    tahun akademik
	 * @param info1            informasi tambahan 1
	 * @param masukDiSmt       semester masuk mahasiswa
	 * @param fakultas         fakultas; boleh null
	 * @param jurusan          jurusan; boleh null
	 * @param program          program studi
	 * @return entitas Konfigurasi
	 */
	public static Konfigurasi getKonfigurasi(String jenisKonfigurasi, String tahunAkademik, String info1,
			String masukDiSmt, Fakultas fakultas, Jurusan jurusan, String program) {
		return getKonfigurasi(jenisKonfigurasi, tahunAkademik, info1, masukDiSmt, fakultas, jurusan, program,
				Konfigurasi.TIDAK_AKTIF);
	}

	/**
	 * Mengambil konfigurasi paling spesifik berdasarkan jenis, tahun akademik, info1, semester
	 * masuk, fakultas, jurusan, program, dan nilai default kustom.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code KonfigurasiManager.getKonfigurasi} dengan semua parameter konteks akademik.</p>
	 *
	 * @param jenisKonfigurasi jenis konfigurasi
	 * @param tahunAkademik    tahun akademik
	 * @param info1            informasi tambahan 1
	 * @param masukDiSmt       semester masuk
	 * @param fakultas         fakultas; boleh null
	 * @param jurusan          jurusan; boleh null
	 * @param program          program studi
	 * @param defaultNilai     nilai default kustom
	 * @return entitas Konfigurasi
	 */
	public static Konfigurasi getKonfigurasi(String jenisKonfigurasi, String tahunAkademik, String info1,
			String masukDiSmt, Fakultas fakultas, Jurusan jurusan, String program, String defaultNilai) {
		return KonfigurasiManager.getKonfigurasi(jenisKonfigurasi, tahunAkademik, info1, masukDiSmt, fakultas, jurusan,
				program, defaultNilai);
	}

	/**
	 * Mengambil konfigurasi paling spesifik untuk konteks sekolah/yayasan berdasarkan jenis,
	 * tahun akademik, info1, semester masuk, yayasan, sekolah, program, dan nilai default kustom.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code KonfigurasiManager.getKonfigurasi} dengan konteks yayasan dan sekolah.</p>
	 *
	 * @param jenisKonfigurasi jenis konfigurasi
	 * @param tahunAkademik    tahun akademik
	 * @param info1            informasi tambahan 1
	 * @param masukDiSmt       semester masuk
	 * @param yayasan          yayasan; boleh null
	 * @param sekolah          sekolah; boleh null
	 * @param program          program
	 * @param defaultNilai     nilai default kustom
	 * @return entitas Konfigurasi
	 */
	public static Konfigurasi getKonfigurasi(String jenisKonfigurasi, String tahunAkademik, String info1,
			String masukDiSmt, Yayasan yayasan, Sekolah sekolah, String program, String defaultNilai) {
		return KonfigurasiManager.getKonfigurasi(jenisKonfigurasi, tahunAkademik, info1, masukDiSmt, yayasan, sekolah,
				program, defaultNilai);
	}

	/**
	 * Pengganti fungsi CURL ProcessBuilder yang jauh lebih efisien & aman
	 * menggunakan Native HTTP.
	 */
	public static String executeHttp(String strURL, String method, String payload, Map<String, String> headers,
			String contentType) throws Exception {
		HttpURLConnection con = null;
		DataOutputStream output = null;
		DataInputStream input = null;
		try {
			URL url = new URL(strURL);
			con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod(method);
			con.setInstanceFollowRedirects(true);

			if (contentType != null) {
				con.setRequestProperty("Content-Type", contentType);
			}
			if (headers != null) {
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					con.setRequestProperty(entry.getKey(), entry.getValue());
				}
			}

			if (payload != null && !payload.isEmpty()) {
				con.setDoOutput(true);
				output = new DataOutputStream(con.getOutputStream());
				output.writeBytes(payload);
				output.flush();
			}

			System.out.println("HTTP Response Code: " + con.getResponseCode());

			// Membaca input stream response API
			input = new DataInputStream(con.getResponseCode() < 400 ? con.getInputStream() : con.getErrorStream());
			int c;
			StringBuilder resultBuf = new StringBuilder();
			while ((c = input.read()) != -1) {
				resultBuf.append((char) c);
			}
			String response = resultBuf.toString();
			System.out.println("response: " + response);
			return response;

		} finally {
			// Safe stream closing (Java 1.6 compatible)
			if (output != null)
				try {
					output.close();
				} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/common/Common.java:20505");
				}
			if (input != null)
				try {
					input.close();
				} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/common/Common.java:20510");
				}
			if (con != null)
				try {
					con.disconnect();
				} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/common/Common.java:20515");
				}
		}
	}

	/**
	 * Menghitung umur dalam tahun berdasarkan tanggal lahir, dibandingkan dengan hari ini.
	 *
	 * <p><b>Cara kerja.</b>
	 * <ol>
	 *   <li>Guard null: mengembalikan 0 bila {@code tanggalLahir} null.</li>
	 *   <li>Menghitung selisih tahun antara hari ini dan tanggal lahir.</li>
	 *   <li>Mengurangi 1 tahun bila ulang tahun tahun ini belum terlewat (bulan/hari
	 *       hari ini masih lebih awal dari bulan/hari lahir).</li>
	 *   <li>Mengembalikan 0 bila hasil negatif (tanggal lahir di masa depan).</li>
	 * </ol>
	 * </p>
	 *
	 * @param tanggalLahir tanggal lahir yang dihitung umurnya; boleh null (mengembalikan 0)
	 * @return umur dalam tahun penuh; 0 bila null atau tanggal di masa depan
	 */
	public static int hitungUmur(Date tanggalLahir) {
		if (tanggalLahir == null) {
			return 0;
		}

		Calendar lahir = Calendar.getInstance();
		lahir.setTime(tanggalLahir);

		Calendar hariIni = Calendar.getInstance();
		int umur = hariIni.get(Calendar.YEAR) - lahir.get(Calendar.YEAR);

		int bulanHariIni = hariIni.get(Calendar.MONTH);
		int bulanLahir = lahir.get(Calendar.MONTH);
		int tanggalHariIni = hariIni.get(Calendar.DAY_OF_MONTH);
		int tanggalLahirInt = lahir.get(Calendar.DAY_OF_MONTH);

		if (bulanHariIni < bulanLahir || (bulanHariIni == bulanLahir && tanggalHariIni < tanggalLahirInt)) {
			umur--;
		}

		return umur < 0 ? 0 : umur;
	}

	/**
	 * Menggabungkan elemen-elemen dari sebuah koleksi (List, Set, dll) menjadi satu
	 * String tunggal yang dipisahkan oleh karakter pemisah (delimiter). Sangat
	 * efisien untuk memori karena menggunakan StringBuilder. Kompatibel dengan Java
	 * 1.6 / 1.7.
	 *
	 * @param elements  Koleksi elemen yang akan digabungkan (contoh: Set<String>,
	 *                  List<String>)
	 * @param delimiter Karakter/teks pemisah antar elemen (contoh: ", " atau "-")
	 * @return String hasil gabungan, atau string kosong ("") jika koleksi
	 *         null/kosong.
	 */
	public static String join(Iterable<?> elements, String delimiter) {
		if (elements == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		boolean isFirst = true;

		for (Object element : elements) {
			if (!isFirst) {
				sb.append(delimiter != null ? delimiter : "");
			}
			sb.append(element != null ? element.toString() : "");
			isFirst = false;
		}

		return sb.toString();
	}

	/**
	 * Overload method join untuk mendukung penggabungan dari Array (Object[]).
	 */
	public static String join(Object[] elements, String delimiter) {
		if (elements == null || elements.length == 0) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		boolean isFirst = true;

		for (Object element : elements) {
			if (!isFirst) {
				sb.append(delimiter != null ? delimiter : "");
			}
			sb.append(element != null ? element.toString() : "");
			isFirst = false;
		}

		return sb.toString();
	}

	/**
	 * Mengambil riwayat status mahasiswa ({@code HistoryStatusMahasiswa}) yang terkait
	 * dengan data KRS ({@code KrsMahasiswa}) tertentu.
	 *
	 * <p><b>Tujuan.</b> Digunakan untuk memeriksa status aktif/cuti/keluar mahasiswa pada
	 * semester yang terkait dengan KRS tersebut, misalnya untuk validasi atau tampilan
	 * informasi status akademik.</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(krsMahasiswa)}.</p>
	 *
	 * @param krsMahasiswa data KRS mahasiswa yang riwayat statusnya dicari; tidak boleh null
	 * @return entitas HistoryStatusMahasiswa yang sesuai; null bila tidak ditemukan
	 */
	public static HistoryStatusMahasiswa getHistoryStatusMahasiswa(KrsMahasiswa krsMahasiswa) {
		return HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(krsMahasiswa);
	}

	/**
	 * Overload kompatibilitas (dipakai pemanggil lama, mis. {@code PembayaranUtil} versi lama):
	 * mengambil riwayat status dengan opsi memaksa proses ulang walau datanya sudah ada
	 * ({@code reload} lama = {@code tetapDiprosesWalaupunSudahAda}). Mendelegasikan ke
	 * {@code HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(KrsMahasiswa, boolean)}.
	 */
	public static HistoryStatusMahasiswa getHistoryStatusMahasiswa(KrsMahasiswa krsMahasiswa, boolean reload) {
		return HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(krsMahasiswa, reload);
	}

	/**
	 * Mensinkronisasi status mahasiswa (aktif/cuti/keluar/dsb.) untuk tahun akademik dan
	 * semester tertentu, dengan opsi menonaktifkan mahasiswa yang tidak memenuhi syarat.
	 *
	 * <p><b>Tujuan.</b> Digunakan oleh proses batch administratif untuk memastikan status
	 * mahasiswa di database sesuai dengan kondisi aktual (misalnya setelah proses evaluasi
	 * akhir semester atau deteksi mahasiswa tidak daftar ulang).</p>
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonAcademicSyncHelper.singkronisasiStatusMahasiswa}. Label ZK digunakan
	 * untuk menampilkan progress kepada operator.</p>
	 *
	 * @param label             Label ZK untuk menampilkan progress; boleh null
	 * @param mahasiswa         mahasiswa yang statusnya disinkronisasi
	 * @param tahunAkademikParam tahun akademik yang diperiksa
	 * @param jenisSemester     jenis semester (GANJIL/GENAP)
	 * @param nonAktifkan       {@code true} untuk menonaktifkan mahasiswa yang tidak memenuhi syarat
	 */
	public static void singkronisasiStatusMahasiswa(Label label, Mahasiswa mahasiswa, String tahunAkademikParam,
			String jenisSemester, boolean nonAktifkan) {
		CommonAcademicSyncHelper.singkronisasiStatusMahasiswa(label, mahasiswa, tahunAkademikParam, jenisSemester,
				nonAktifkan);
	}

	/**
	 * Menginisialisasi combobox Fakultas dan Jurusan secara berpasangan — memilih item
	 * yang sesuai dengan {@code currentFakultas}/{@code currentJurusan} dan memastikan
	 * pilihan jurusan difilter berdasarkan fakultas yang dipilih.
	 *
	 * <p><b>Cara kerja.</b> Mendelegasikan ke
	 * {@code CommonComboLanguageHelper.initFakultasDanJurusanData} yang mengisi kedua
	 * combobox, menerapkan filter jurusan berdasarkan fakultas, dan memilih item awal.</p>
	 *
	 * @param currentFakultas fakultas yang saat ini dipilih (nilai awal); boleh null
	 * @param currentJurusan  jurusan yang saat ini dipilih (nilai awal); boleh null
	 * @param searchfakultas  Combobox ZK untuk pilihan fakultas
	 * @param searchjurusan   Combobox ZK untuk pilihan jurusan (difilter per fakultas)
	 */
	public static void initFakultasDanJurusanData(Fakultas currentFakultas, Jurusan currentJurusan,
			Combobox searchfakultas, Combobox searchjurusan) {
		CommonComboLanguageHelper.initFakultasDanJurusanData(currentFakultas, currentJurusan, searchfakultas,
				searchjurusan);
	}

	/**
	 * Sertakan (include) satu fragmen JSP <b>hanya bila berkasnya benar-benar ada</b>, dan
	 * jangan pernah melempar exception ke halaman pemanggil.
	 *
	 * <h3>Kenapa diperlukan</h3>
	 * <p>Dasbor beranda ({@code /WEB-INF/baru/modul/home/index.jsp}) disusun dari banyak
	 * fragmen kecil per peran: info mahasiswa, tombol dosen, profil admin, pengumuman,
	 * kalender akademik, dan seterusnya. Tidak semua instalasi memiliki seluruh fragmen itu —
	 * instalasi tanpa modul sekolah tidak punya fragmen guru/siswa, misalnya. Dengan
	 * {@code <jsp:include>} biasa, satu fragmen yang tidak ada langsung membuat SELURUH
	 * dasbor gagal dirender. Method ini membuat fragmen bersifat opsional: yang ada
	 * ditampilkan, yang tidak ada dilewati diam-diam.</p>
	 *
	 * <h3>Kenapa kegagalan render pun ditelan</h3>
	 * <p>Bila fragmennya ADA tetapi gagal dijalankan (mis. datanya belum lengkap), kegagalan
	 * itu dicatat ke audit lalu dilewati. Alasannya sama: satu kartu dasbor yang bermasalah
	 * tidak boleh membuat pengguna kehilangan seluruh halaman beranda. Jejaknya tetap ada di
	 * audit sehingga penyebabnya tetap bisa ditelusuri pengembang.</p>
	 *
	 * <h3>Catatan teknis</h3>
	 * <p>Keberadaan berkas diperiksa lewat {@code getRealPath} lebih dulu (murah, tanpa I/O
	 * jaringan); bila aplikasi dijalankan dari WAR yang tidak diekstrak, {@code getRealPath}
	 * mengembalikan null sehingga dicoba ulang lewat {@code getResource}.</p>
	 *
	 * <p>Penyertaan memakai {@code PageContext.include(String)}, BUKAN varian dua argumen
	 * yang dapat menahan flush: {@code servlet_.jar} yang dipaketkan aplikasi ini masih
	 * API JSP 1.2 dan hanya memiliki bentuk satu argumen. Konsekuensinya buffer
	 * {@code JspWriter} induk ikut di-flush sebelum tiap fragmen disertakan — persis
	 * seperti {@code <jsp:include flush="true">} yang sudah dipakai di seluruh JSP
	 * aplikasi ini — sehingga respons ter-commit sejak fragmen pertama dan header tidak
	 * lagi dapat diubah setelah titik itu. Untuk dasbor beranda yang memang dirender di
	 * akhir alur, perilakunya sama dengan halaman-halaman lain yang sudah ada.</p>
	 *
	 * @param pageContext konteks halaman JSP pemanggil; diabaikan bila null
	 * @param path        path absolut fragmen relatif terhadap context root, mis.
	 *                    {@code "/WEB-INF/baru/modul/home/pengumuman.jsp"}
	 */
	public static void sertakanJikaAda(javax.servlet.jsp.PageContext pageContext, String path) {
		if (pageContext == null || path == null || path.trim().length() == 0) {
			return;
		}
		String berkas = path.trim();
		try {
			javax.servlet.ServletContext konteks = pageContext.getServletContext();
			if (konteks == null) {
				return;
			}
			if (!berkasAda(konteks, berkas)) {
				return;
			}
			/* FIX 21-08-2026: overload include(String, boolean) baru ada pada JSP 2.0+, sedangkan
			 * jsp-api pada classpath proyek ini belum memilikinya -- Common.java jadi GAGAL
			 * dikompilasi, sehingga Common.class yang ter-deploy tidak memuat method ini dan
			 * seluruh halaman beranda gagal dengan JasperException "method sertakanJikaAda
			 * is undefined for the type Common". Dipakai overload satu-argumen yang tersedia. */
			pageContext.include(berkas);
		} catch (Throwable gagal) {
			try {
				ErrorAuditUtil.record(gagal, "Common.sertakanJikaAda: fragmen dilewati -- " + berkas);
			} catch (Throwable abaikan) {
				// pencatatan audit tidak boleh ikut menggagalkan render halaman
			}
		}
	}

	/** Apakah sebuah resource web benar-benar ada. Lihat {@link #sertakanJikaAda}. */
	private static boolean berkasAda(javax.servlet.ServletContext konteks, String path) {
		try {
			String nyata = konteks.getRealPath(path);
			if (nyata != null) {
				return new java.io.File(nyata).isFile();
			}
		} catch (Throwable abaikan) {
			// lanjut ke pemeriksaan berbasis getResource di bawah
		}
		try {
			// WAR yang tidak diekstrak: getRealPath null, resource tetap dapat diperiksa.
			return konteks.getResource(path) != null;
		} catch (Throwable abaikan) {
			return false;
		}
	}

}
