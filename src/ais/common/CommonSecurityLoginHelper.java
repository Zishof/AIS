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



/**
 * Kumpulan helper statis untuk seluruh mekanisme login/logout dan sesi pengguna di AIS, mencakup
 * banyak "jenis" identitas yang dapat login secara terpisah (login sosial untuk {@link Tbmuser}/
 * {@link Mahasiswa}/{@link Siswa}, login cookie PMB untuk {@link BiodataCalonMahasiswa}, login
 * {@link CalonSiswa} PPDB, login {@link PenyediaAsset}, login {@link CalonPegawai} rekrutmen),
 * ditambah verifikasi kredensial ({@link #checkLogin(String, String)}) dan alur lupa password
 * ({@link #kirimLupaPassword(String)}). Kelas ini mewarisi {@link Common} (mengekspos ulang
 * beberapa method seperti {@code tampilErrorJikaAdmin}) dan seluruh anggotanya bersifat statis.
 *
 * <h2>Pola sesi &amp; cookie</h2>
 * <p>
 * Untuk setiap jenis identitas, kelas ini umumnya menyediakan tiga method berpasangan:
 * {@code isLogin*()} (memeriksa status login, kadang membaca dari {@link HttpSession} lalu
 * memulihkannya dari cookie bila sesi kosong), {@code setLogin*(...)} (menandai login: menulis ke
 * {@link HttpSession} dan — untuk sebagian jenis — juga menulis cookie persisten), dan
 * {@code setLogout*(...)} (membersihkan session attribute terkait, dan untuk sebagian jenis juga
 * menghapus cookie). Session attribute standar yang SELALU ikut diisi/dibersihkan berdampingan
 * dengan attribute spesifik jenis adalah {@code mytbmuser}, {@code usersTemp}, dan {@code user} —
 * ini adalah representasi {@link Tbmuser} generik dari identitas yang sedang login, dipakai luas
 * oleh lapisan otorisasi/tampilan lain di aplikasi tanpa perlu tahu jenis identitas aslinya.
 * </p>
 * <p>
 * Login cookie PMB (Penerimaan Mahasiswa Baru) untuk {@link BiodataCalonMahasiswa} bersifat
 * OPSIONAL, dikendalikan oleh saklar konfigurasi {@link #isPmbCookieLoginEnabled()}
 * ({@code KONFIG_PMB_LOGIN_COOKIE}) — bila tidak aktif, login PMB murni berbasis session tanpa
 * cookie "ingat saya". Nilai cookie yang menyimpan id entitas dienkripsi lebih dulu lewat
 * {@code Common.desEncrypter.get().encrypt(...)} sebelum ditulis (dan didekripsi saat dibaca
 * kembali via {@link #getCookieValue(HttpServletRequest, String)}), dibungkus lagi lewat
 * {@code Common.nilaiCookieAman(...)}, dengan umur cookie 15.552.000 detik (180 hari) dan flag
 * {@code Secure} mengikuti {@code request.isSecure()}.
 * </p>
 *
 * <h2>PERINGATAN KEAMANAN — penyimpanan &amp; verifikasi kata sandi</h2>
 * <p>
 * <b>1. Kata sandi pengguna disimpan sebagai ENKRIPSI SIMETRIS YANG DAPAT DIBALIK (reversible),
 * BUKAN sebagai hash satu-arah.</b> Seluruh alur di kelas ini — {@link #checkLogin(String,
 * String)}, {@link #kirimLupaPassword(String)}, dan ketiga varian lengkap {@code doLogin(...)} —
 * memanggil {@code Common.desEncrypter.get().decrypt(...)} untuk memperoleh kata sandi asli dalam
 * bentuk plain text dari kolom tersimpan ({@code Tbmuser#getUserPassword()},
 * {@code Mahasiswa#getPass()}, {@code Siswa#getPass()}). Ini adalah pola kriptografi yang lemah
 * dibandingkan praktik standar (hash satu-arah bergaram seperti bcrypt/PBKDF2/Argon2): siapa pun
 * yang memperoleh kunci enkripsi ({@code desEncrypter}, lokasi definisinya di luar file ini) dapat
 * membalik SELURUH kata sandi pengguna yang tersimpan di database, bukan hanya kata sandi yang
 * sedang diverifikasi. Sesuai batasan tugas dokumentasi ini, pola ini TIDAK diubah di sini — lihat
 * laporan dokumentasi terkait untuk detail lokasi persis dan rekomendasi migrasi ke hashing
 * satu-arah.
 * </p>
 * <p>
 * <b>2. {@link #kirimLupaPassword(String)} mengirim kata sandi ASLI (hasil dekripsi) dalam bentuk
 * plain text lewat email</b> ({@code "... Kata sandi : " + passwordDecript}) — pola yang HANYA
 * mungkin dilakukan karena kata sandi disimpan reversibel (temuan #1 di atas); pada sistem yang
 * memakai hash satu-arah, pola semacam ini secara desain tidak mungkin dilakukan (yang dikirim
 * seharusnya tautan reset, bukan kata sandi lama). Ini bukan bug terpisah, melainkan konsekuensi
 * langsung dari temuan #1.
 * </p>
 * <p>
 * <b>3. {@link #checkLogin(String, String)} membandingkan kata sandi dengan {@code
 * password.equals(pwd)}</b> — perbandingan string standar Java yang TIDAK constant-time (waktu
 * eksekusinya dapat bervariasi tergantung di karakter mana ketidakcocokan pertama terjadi),
 * membuka celah teoretis serangan timing untuk menebak kata sandi karakter demi karakter pada
 * lingkungan dengan pengukuran waktu jaringan yang sangat presisi. Risiko praktis pola ini
 * umumnya lebih rendah dibanding temuan #1-2, namun tetap dicatat sebagai penyimpangan dari
 * praktik terbaik (perbandingan token/secret idealnya memakai fungsi constant-time seperti
 * {@code MessageDigest.isEqual}).
 * </p>
 * <p>
 * Sesuai instruksi tugas dokumentasi ini, KETIGA pola di atas TIDAK diubah — hanya didokumentasikan
 * apa adanya berdasarkan pembacaan kode yang teliti.
 * </p>
 */
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class CommonSecurityLoginHelper extends Common {

	/**
	 * Mencari nilai satu cookie HTTP berdasarkan namanya di antara seluruh cookie pada
	 * {@code request}. Kegagalan (mis. {@code request.getCookies()} melempar) ditangkap dan
	 * dicatat ke {@link ErrorAuditUtil}, mengembalikan {@code null} alih-alih melempar ulang.
	 *
	 * @param request    HTTP request sumber cookie; {@code null} menghasilkan {@code null}
	 * @param cookieName nama cookie yang dicari; {@code null}/kosong menghasilkan {@code null}
	 * @return nilai cookie yang cocok, atau {@code null} bila tidak ditemukan/terjadi kegagalan
	 */
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

	/** @return {@code value} setelah di-trim, atau string kosong bila {@code value} {@code null} */
	private static String safeTrim(String value) {
		return value == null ? "" : value.trim();
	}

	/** @return {@code true} bila {@code value} {@code null}, kosong, atau hanya whitespace */
	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}

	/**
	 * Memastikan {@code directory} ada sebagai direktori, membuatnya (beserta direktori induk
	 * yang belum ada) bila belum ada.
	 *
	 * @param directory direktori yang dipastikan tersedia; {@code null} menghasilkan {@code false}
	 * @return {@code true} bila direktori sudah ada (dan memang berupa direktori) atau berhasil
	 *         dibuat; {@code false} bila path sudah ada tapi bukan direktori, atau pembuatan gagal
	 */
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
	 * Menampilkan pesan error CRUD generik ke pengguna lewat {@link MyMessageboxConfig}, dengan
	 * detail pesan exception (bila ada) ditambahkan setelah {@code pesan}, sekaligus mencatat
	 * exception lewat {@link Common#tampilErrorJikaAdmin(Exception)}.
	 *
	 * @param e     exception penyebab, boleh {@code null}
	 * @param pesan pesan utama yang ditampilkan ke pengguna
	 */
	private static void tampilCrudError(Exception e, String pesan) {
		Common.tampilErrorJikaAdmin(e);
		String detail = e == null || e.getMessage() == null ? "" : "\n" + e.getMessage();
		try {
			MyMessageboxConfig.show(pesan + detail);
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonSecurityLoginHelper.java:483");
		}
	}




	/**
	 * Memicu pemeriksaan hak akses baca standar untuk permintaan saat ini. Sekadar pembungkus
	 * tipis di atas {@link CommonPrivilages#doCheckPrevilagesRead()}.
	 */
	public static void doCheckSecurity() {
		CommonPrivilages.doCheckPrevilagesRead();
	}



	/**
	 * Varian ringkas {@link #doLogin(Tbmuser, String, String, String)} tanpa parameter tambahan
	 * ({@code parameter=null}).
	 */
	public static void doLogin(Tbmuser tbmuser, String linkProfile, String callback_url) throws Exception {
		doLogin(tbmuser, linkProfile, null, callback_url);
	}



	/**
	 * Varian ringkas {@link #doLogin(Mahasiswa, String, String, String)} tanpa parameter
	 * tambahan ({@code parameter=null}).
	 */
	public static void doLogin(Mahasiswa mahasiswa, String linkProfile, String callback_url) throws Exception {
		doLogin(mahasiswa, linkProfile, null, callback_url);
	}



	/**
	 * Varian ringkas {@link #doLogin(Siswa, String, String, String)} tanpa parameter tambahan
	 * ({@code parameter=null}).
	 */
	public static void doLogin(Siswa siswa, String linkProfile, String callback_url) throws Exception {
		doLogin(siswa, linkProfile, null, callback_url);
	}



	/**
	 * Implementasi kanonik login sosial (mis. via Google/Facebook) untuk pengguna {@link Tbmuser}
	 * — dipanggil setelah proses otentikasi sosial di lapisan lain berhasil mengidentifikasi
	 * entitas {@code tbmuser} yang bersangkutan.
	 *
	 * <p>
	 * Langkah kerja: (1) menutup dialog konfirmasi tutup halaman ZK ({@link
	 * Clients#confirmClose(String)}); (2) memeriksa apakah role pengguna termasuk dalam daftar
	 * yang diblokir dari login sosial (konfigurasi {@code ConstantValues#grupPenggunaBlok},
	 * daftar role dipisah koma, dibandingkan case-insensitive) — bila diblokir, menampilkan
	 * peringatan dan memanggil {@code Common.goLogoff()}, lalu method berhenti; (3) bila lolos,
	 * memanggil {@code SecurityFilter.doAutoLogin(...)} dengan userId dan kata sandi HASIL
	 * DEKRIPSI ({@code Common.desEncrypter.get().decrypt(tbmuser.getUserPassword())} — lihat
	 * peringatan keamanan pada javadoc kelas terkait penyimpanan kata sandi reversibel) untuk
	 * benar-benar membangun sesi login; (4) mengarahkan ulang (redirect) browser ke
	 * {@code callback_url} atau {@code "main" + parameter} sebagai default.
	 * </p>
	 *
	 * <p>
	 * <b>Catatan perilaku:</b> redirect SELALU dilakukan di akhir method — bahkan bila
	 * {@code SecurityFilter.doAutoLogin(...)} mengembalikan {@code false} (gagal), method tetap
	 * memanggil {@link ExecutionsCtrl#sendRedirect(String)} seolah login berhasil (dicatat
	 * eksplisit dalam log konsol sebagai peringatan). Seluruh langkah pengecekan blokir dibungkus
	 * {@code try/catch} yang menelan exception (dicatat via {@code System.out}/{@code
	 * Common.tampilErrorJikaAdmin}), sehingga kegagalan pengecekan blokir TIDAK menghentikan
	 * proses login — hanya dicatat sebagai log.
	 * </p>
	 *
	 * @param tbmuser      entitas pengguna yang login
	 * @param linkProfile  tautan profil, diteruskan ke {@code SecurityFilter.doAutoLogin}
	 * @param parameter    parameter tambahan yang disisipkan ke URL redirect default
	 *                     ({@code "main" + parameter}) bila {@code callback_url} {@code null}
	 * @param callback_url URL tujuan redirect setelah login; bila {@code null}, dipakai
	 *                     {@code "main" + parameter}
	 * @throws Exception diteruskan dari kegagalan {@code SecurityFilter.doAutoLogin} atau proses
	 *                    redirect
	 */
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



	/**
	 * Sama seperti {@link #doLogin(Tbmuser, String, String, String)} namun untuk pengguna
	 * {@link Mahasiswa} — role yang diblokir dari login sosial dicek dengan literal tetap
	 * {@code "mhs"} (bukan role dinamis seperti pada varian {@link Tbmuser}), kata sandi untuk
	 * {@code SecurityFilter.doAutoLogin} didekripsi dari {@code mahasiswa.getPass()}. Lihat
	 * javadoc {@link #doLogin(Tbmuser, String, String, String)} untuk penjelasan lengkap alur dan
	 * catatan perilaku redirect-selalu-jalan.
	 *
	 * @param mahasiswa    entitas mahasiswa yang login
	 * @param linkProfile  tautan profil
	 * @param parameter    parameter tambahan URL redirect default
	 * @param callback_url URL tujuan redirect
	 * @throws Exception diteruskan dari kegagalan proses login/redirect
	 */
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



	/**
	 * Sama seperti {@link #doLogin(Tbmuser, String, String, String)} namun untuk pengguna
	 * {@link Siswa} — role diblokir dicek dengan literal {@code "mhs"} (sama seperti varian
	 * {@link Mahasiswa}), otentikasi dilakukan dengan {@code siswa.getNomorIndukNasional()}
	 * sebagai username dan kata sandi hasil dekripsi {@code siswa.getPass()}. Lihat javadoc
	 * {@link #doLogin(Tbmuser, String, String, String)} untuk penjelasan lengkap.
	 *
	 * @param siswa        entitas siswa yang login
	 * @param linkProfile  tautan profil
	 * @param parameter    parameter tambahan URL redirect default
	 * @param callback_url URL tujuan redirect
	 * @throws Exception diteruskan dari kegagalan proses login/redirect
	 */
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



	/**
	 * Memeriksa apakah fitur "ingat saya" berbasis cookie untuk login PMB (calon mahasiswa baru)
	 * aktif, dibaca dari konfigurasi {@code KONFIG_PMB_LOGIN_COOKIE}. Kegagalan pembacaan
	 * konfigurasi (mis. exception apa pun) diperlakukan sebagai tidak aktif ({@code false}) demi
	 * keamanan (fail-closed).
	 *
	 * @return {@code true} hanya bila konfigurasi ditemukan dan nilainya sama dengan
	 *         {@link Konfigurasi#AKTIF} (case-insensitive)
	 */
	public static boolean isPmbCookieLoginEnabled() {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi(KONFIG_PMB_LOGIN_COOKIE, Konfigurasi.TIDAK_AKTIF);
			return konfigurasi != null && konfigurasi.getNilai() != null
					&& Konfigurasi.AKTIF.equalsIgnoreCase(konfigurasi.getNilai().trim());
		} catch (Exception e) {
			return false;
		}
	}



	/**
	 * Varian tanpa parameter {@link #isLogin(HttpServletRequest)}: menentukan
	 * {@link HttpServletRequest} aktif dari konteks eksekusi ZK saat ini
	 * ({@link ExecutionsCtrl#getCurrent()}), atau dari {@code RequestContext.get()} sebagai
	 * fallback bila di luar konteks ZK.
	 *
	 * @return entitas {@link BiodataCalonMahasiswa} yang sedang login, atau {@code null} bila
	 *         tidak ada request aktif atau tidak sedang login
	 */
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



	/**
	 * Implementasi kanonik pemeriksaan status login PMB (calon mahasiswa baru), dengan
	 * pemulihan otomatis dari cookie "ingat saya" bila sesi HTTP belum/tidak memilikinya.
	 *
	 * <p>
	 * Urutan pengecekan: (1) bila {@link HttpSession} yang ada sudah memiliki attribute
	 * {@code "BiodataCalonMahasiswa"}, kembalikan langsung tanpa akses database; (2) bila tidak,
	 * dan fitur cookie PMB tidak aktif ({@link #isPmbCookieLoginEnabled()} bernilai
	 * {@code false}), kembalikan {@code null} (tidak login); (3) bila aktif, baca cookie
	 * {@code COOKIE_PMB_BIODATA}, DEKRIPSI nilainya untuk memperoleh id entitas, dan muat entitas
	 * {@link BiodataCalonMahasiswa} dari cache/database lewat {@code ConstantValues.ambil(...)};
	 * (4) bila entitas ditemukan, PULIHKAN sesi HTTP dengan menuliskan kembali seluruh attribute
	 * standar ({@code BiodataCalonMahasiswa}, {@code mytbmuser}, {@code usersTemp}, {@code user})
	 * sehingga pemanggilan berikutnya dalam sesi yang sama tidak perlu lagi membaca cookie/database.
	 * </p>
	 *
	 * <p>
	 * Nilai id {@code "-1"} pada hasil dekripsi cookie diperlakukan khusus sebagai "tidak ada
	 * login" (bukan error) — kemungkinan penanda eksplisit dari kode yang menulis cookie untuk
	 * merepresentasikan status logout tanpa harus menghapus cookie. Seluruh exception (termasuk
	 * kegagalan dekripsi cookie yang rusak/dipalsukan) ditangkap dan menghasilkan {@code null},
	 * bukan dilempar ulang.
	 * </p>
	 *
	 * @param request HTTP request sumber sesi/cookie; {@code null} menghasilkan {@code null}
	 * @return entitas {@link BiodataCalonMahasiswa} yang sedang login (dari sesi atau dipulihkan
	 *         dari cookie), atau {@code null} bila tidak login/gagal
	 */
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



	/**
	 * Varian tanpa parameter {@link #setLogout(HttpServletRequest, HttpServletResponse)}:
	 * menentukan request/response aktif dari konteks eksekusi ZK saat ini, atau dari
	 * {@code RequestContext}/{@code ResponseContext} sebagai fallback. Seluruh exception
	 * ditangkap dan dicatat ke {@link ErrorAuditUtil}, tidak dilempar ulang.
	 */
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



	/**
	 * Implementasi kanonik logout PMB: menghapus attribute sesi standar
	 * ({@code BiodataCalonMahasiswa}, {@code mytbmuser}, {@code usersTemp}, {@code user}) dan
	 * meng-invalidate seluruh {@link HttpSession}, lalu membersihkan cookie login PMB lewat
	 * {@code clearPmbLoginCookies(request, response)} (dipanggil tanpa syarat, terlepas dari
	 * apakah invalidasi sesi berhasil).
	 *
	 * @param request  HTTP request sumber sesi/cookie yang akan dibersihkan
	 * @param response HTTP response tempat penghapusan cookie ditulis
	 */
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



	/**
	 * Varian tanpa parameter {@link #setLogin(HttpServletRequest, HttpServletResponse,
	 * BiodataCalonMahasiswa)}: menentukan request/response aktif dari konteks ZK saat ini, atau
	 * dari {@code RequestContext}/{@code ResponseContext} sebagai fallback. Kegagalan ditangkap
	 * dan dicatat ke {@link ErrorAuditUtil}, tidak dilempar ulang.
	 *
	 * @param biodataCalonMahasiswa entitas calon mahasiswa yang akan ditandai login
	 */
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



	/**
	 * Implementasi kanonik login PMB: menulis attribute sesi standar
	 * ({@code BiodataCalonMahasiswa}, {@code mytbmuser}, {@code usersTemp}, {@code user} — yang
	 * terakhir tiga berupa objek {@link Tbmuser} yang dibungkus dari {@code biodataCalonMahasiswa}
	 * lewat konstruktor {@code new Tbmuser(BiodataCalonMahasiswa)}), lalu — hanya bila
	 * {@code res} tidak {@code null} DAN {@link #isPmbCookieLoginEnabled()} aktif — menulis dua
	 * cookie persisten (180 hari, {@code Secure} mengikuti {@code request.isSecure()}):
	 * {@code COOKIE_PMB_BIODATA} (id entitas terenkripsi) dan {@code COOKIE_PMB_USERID} (nomor
	 * registrasi, tidak dienkripsi, hanya dibungkus {@code Common.nilaiCookieAman(...)}).
	 *
	 * <p>
	 * Bila {@code request} atau {@code biodataCalonMahasiswa} {@code null}, method berhenti
	 * lebih awal tanpa efek apa pun. Kegagalan penulisan sesi maupun cookie ditangani terpisah
	 * (masing-masing dalam blok {@code try/catch} sendiri) sehingga kegagalan menulis cookie
	 * tidak menggagalkan penulisan sesi, dan sebaliknya.
	 * </p>
	 *
	 * @param request                HTTP request sumber sesi
	 * @param res                    HTTP response tempat cookie ditulis; {@code null} berarti
	 *                               cookie tidak ditulis (hanya sesi)
	 * @param biodataCalonMahasiswa  entitas calon mahasiswa yang ditandai login
	 */
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
			Cookie cookieUsername = new Cookie(COOKIE_PMB_BIODATA, Common.nilaiCookieAman(
					Common.desEncrypter.get().encrypt(biodataCalonMahasiswa.getId().toString())));
			cookieUsername.setMaxAge(15552000);
			cookieUsername.setPath("/");
			cookieUsername.setSecure(request.isSecure());
			res.addCookie(cookieUsername);

			String noRegistrasi = biodataCalonMahasiswa.getNoRegistrasi() == null ? ""
					: biodataCalonMahasiswa.getNoRegistrasi();
			cookieUsername = new Cookie(COOKIE_PMB_USERID, Common.nilaiCookieAman(noRegistrasi));
			cookieUsername.setMaxAge(15552000);
			cookieUsername.setPath("/");
			cookieUsername.setSecure(request.isSecure());
			res.addCookie(cookieUsername);

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonSecurityLoginHelper.java:792");
//			tampilErrorJikaAdmin(e);
		}
	}



	/**
	 * Varian tanpa parameter {@link #isLoginCalonSiswa(HttpServletRequest)}: menentukan request
	 * aktif dari konteks ZK saat ini atau {@code RequestContext} sebagai fallback.
	 *
	 * @return entitas {@link CalonSiswa} yang tersimpan di sesi ({@code request.getSession(true)}
	 *         — SELALU membuat sesi baru bila belum ada), atau {@code null} bila tidak login
	 *         atau terjadi kegagalan (ditangani lewat {@link Common#tampilErrorJikaAdmin(Exception)})
	 */
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



	/**
	 * Memeriksa status login {@link CalonSiswa} (calon siswa PPDB) murni dari attribute sesi
	 * HTTP — TIDAK ada mekanisme pemulihan dari cookie seperti pada
	 * {@link #isLogin(HttpServletRequest)} untuk PMB.
	 *
	 * @param request HTTP request sumber sesi
	 * @return entitas {@link CalonSiswa} dari sesi, atau {@code null} bila tidak login/gagal
	 */
	public static CalonSiswa isLoginCalonSiswa(HttpServletRequest request) {
		try {
			return (CalonSiswa) request.getSession(true).getAttribute("CalonSiswa");
		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
			return null;
		}
	}



	/**
	 * Varian tanpa parameter {@link #setLogoutCalonSiswa(HttpServletRequest,
	 * HttpServletResponse)}: menentukan request/response aktif dari konteks ZK saat ini, atau
	 * dari {@code RequestContext}/{@code ResponseContext} sebagai fallback.
	 */
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



	/**
	 * Implementasi kanonik logout {@link CalonSiswa}: mengosongkan (bukan menghapus) attribute
	 * sesi {@code CalonSiswa}/{@code mytbmuser}/{@code usersTemp}/{@code user} (di-set ke
	 * {@code null}, session tidak di-invalidate — berbeda dari {@link #setLogout(HttpServletRequest,
	 * HttpServletResponse)} versi PMB yang meng-invalidate seluruh sesi).
	 *
	 * <p>
	 * <b>Catatan perilaku cookie:</b> berbeda dari {@code clearPmbLoginCookies} yang hanya
	 * menghapus cookie tertentu, method ini mengiterasi SELURUH cookie pada {@code request}
	 * (bukan hanya cookie terkait login CalonSiswa) dan menghapus semuanya (nilai dikosongkan,
	 * {@code maxAge=0}) — efek sampingnya adalah SEMUA cookie yang dikirim browser untuk domain
	 * ini akan dihapus oleh operasi logout ini, bukan hanya cookie login.
	 * </p>
	 *
	 * @param request  HTTP request sumber sesi/cookie
	 * @param response HTTP response tempat penghapusan cookie ditulis
	 */
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



	/**
	 * Memeriksa status login {@link PenyediaAsset} (penyedia/vendor aset) murni dari attribute
	 * sesi HTTP ({@code request.getSession(true)}), tanpa mekanisme cookie.
	 *
	 * @return entitas {@link PenyediaAsset} dari sesi, atau {@code null} bila tidak login/gagal
	 */
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



	/**
	 * Logout {@link PenyediaAsset}: mengosongkan attribute sesi
	 * {@code PenyediaAsset}/{@code mytbmuser}/{@code usersTemp}/{@code user} (di-set
	 * {@code null}, tanpa invalidate sesi maupun penghapusan cookie).
	 */
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



	/**
	 * Memeriksa status login {@link CalonPegawai} (calon pegawai rekrutmen) murni dari attribute
	 * sesi HTTP ({@code request.getSession(true)}), tanpa mekanisme cookie.
	 *
	 * @return entitas {@link CalonPegawai} dari sesi, atau {@code null} bila tidak login/gagal
	 */
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



	/**
	 * Logout {@link CalonPegawai}: mengosongkan attribute sesi
	 * {@code CalonPegawai}/{@code mytbmuser}/{@code usersTemp}/{@code user} (di-set
	 * {@code null}, tanpa invalidate sesi maupun penghapusan cookie).
	 */
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



	/**
	 * Varian tanpa parameter {@link #setLogin(HttpServletRequest, CalonSiswa)}: menentukan
	 * request aktif dari konteks ZK saat ini atau {@code RequestContext} sebagai fallback.
	 * Kegagalan ditangani lewat {@link Common#tampilErrorJikaAdmin(Exception)}.
	 *
	 * @param calonSiswa entitas calon siswa yang ditandai login
	 */
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



	/**
	 * Login {@link CalonSiswa} pada sesi saja (tanpa cookie persisten) — menulis attribute sesi
	 * {@code CalonSiswa} dan {@code mytbmuser}/{@code usersTemp}/{@code user} (dibungkus dari
	 * {@code new Tbmuser(CalonSiswa)}). Lihat {@link #setLogin(HttpServletRequest,
	 * HttpServletResponse, CalonSiswa)} untuk varian yang juga menulis cookie.
	 *
	 * @param request    HTTP request sumber sesi
	 * @param calonSiswa entitas calon siswa yang ditandai login
	 */
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



	/**
	 * Implementasi kanonik login {@link CalonSiswa} dengan cookie persisten: menulis attribute
	 * sesi (sama seperti {@link #setLogin(HttpServletRequest, CalonSiswa)}) DAN dua cookie
	 * ({@code "calonSiswa"} berisi id terenkripsi, {@code "userid"} berisi nomor registrasi),
	 * masing-masing berumur 180 hari.
	 *
	 * <p>
	 * Berbeda dari {@link #setLogin(HttpServletRequest, HttpServletResponse,
	 * BiodataCalonMahasiswa)} versi PMB, method ini TIDAK dikondisikan oleh saklar konfigurasi
	 * apa pun (mis. {@link #isPmbCookieLoginEnabled()}) — cookie login {@link CalonSiswa} SELALU
	 * ditulis tanpa syarat setiap kali method ini dipanggil, dan flag {@code Secure} pada cookie
	 * TIDAK diset eksplisit (berbeda dari cookie PMB yang mengikuti {@code request.isSecure()}).
	 * </p>
	 *
	 * @param request    HTTP request sumber sesi
	 * @param res        HTTP response tempat cookie ditulis
	 * @param calonSiswa entitas calon siswa yang ditandai login
	 */
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
			Cookie cookieUsername = new Cookie("calonSiswa", Common.nilaiCookieAman(
					Common.desEncrypter.get().encrypt(calonSiswa.getId().toString())));
			cookieUsername.setMaxAge(15552000);

			res.addCookie(cookieUsername);

			cookieUsername = new Cookie("userid", Common.nilaiCookieAman(calonSiswa.getNoRegistrasi()));
			cookieUsername.setMaxAge(15552000);

			res.addCookie(cookieUsername);

		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
		}
	}



	/**
	 * Varian ringkas {@link #tampilErrorJikaAdmin(Exception, String, boolean)} tanpa info
	 * tambahan dan tanpa opsi unduh ({@code info=""}, {@code download=false}).
	 */
	public static String tampilErrorJikaAdmin(Exception ex) {
		// ex.printStackTrace();
		return tampilErrorJikaAdmin(ex, "", false);
	}



	/**
	 * Menampilkan detail exception ke pengguna HANYA bila pengguna saat ini adalah admin
	 * (logika penentuan admin dan penampilan didelegasikan sepenuhnya ke
	 * {@link CommonHelperClass#tampilErrorJikaAdmin(Exception, String, boolean)}). Sekadar
	 * pembungkus tipis untuk kompatibilitas nama method pada kelas ini.
	 *
	 * @param ex       exception yang akan ditampilkan/dicatat
	 * @param info     informasi tambahan yang disisipkan ke tampilan error
	 * @param download bila {@code true}, sediakan opsi mengunduh detail error
	 * @return pesan/hasil sebagaimana dikembalikan {@link CommonHelperClass#tampilErrorJikaAdmin}
	 */
	public static String tampilErrorJikaAdmin(Exception ex, String info, boolean download) {

		return CommonHelperClass.tampilErrorJikaAdmin(ex, info, download);

	}



	/**
	 * Membuat {@link EventListener} yang, saat dipicu, memulai proses unduh detail
	 * {@code ex}. Sekadar pembungkus tipis di atas
	 * {@link CommonHelperClass#downloadError(Exception)}.
	 *
	 * @param ex exception yang detailnya akan diunduh
	 * @return listener siap dipasang ke komponen UI (mis. tombol unduh)
	 */
	public static EventListener downloadError(Exception ex) {
		return CommonHelperClass.downloadError(ex);
	}



	/**
	 * Memverifikasi kombinasi username/password terhadap data {@link Tbmuser} atau
	 * {@link Mahasiswa} tersimpan.
	 *
	 * <p>
	 * Pertama mencari {@link Tbmuser} aktif dengan {@code userId} sama dengan {@code username}.
	 * Bila TIDAK ditemukan, jalur verifikasi BERBEDA dipakai untuk {@link Mahasiswa}: kata sandi
	 * masukan dienkripsi lebih dulu ({@code Common.desEncrypter.get().encrypt(password)}) lalu
	 * dibandingkan LANGSUNG di query SQL terhadap kolom {@code pass} tersimpan (perbandingan
	 * ciphertext-ke-ciphertext) — berbeda dari jalur {@link Tbmuser} yang mendekripsi kata sandi
	 * tersimpan lebih dulu baru membandingkan plaintext-ke-plaintext dengan
	 * {@code password.equals(pwd)} (lihat peringatan keamanan pada javadoc kelas ini terkait
	 * penyimpanan kata sandi reversibel dan perbandingan non-constant-time).
	 * </p>
	 *
	 * @param username userId ({@link Tbmuser}) atau NIM ({@link Mahasiswa}) yang diverifikasi
	 * @param password kata sandi masukan yang diverifikasi (plain text)
	 * @return {@code true} bila kombinasi username/password cocok dengan salah satu entitas
	 *         (Tbmuser atau Mahasiswa) yang aktif; {@code false} bila {@code username}/{@code
	 *         password} {@code null}, tidak cocok, atau terjadi kegagalan yang tertangkap
	 *         secara internal
	 */
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



    /**
     * Implementasi alur "lupa password": mencari akun pengguna berdasarkan {@code username} di
     * antara lima kemungkinan jenis entitas ({@link Tbmuser}, dan turunannya {@link Dosen}/
     * {@link Pegawai} lewat relasi {@code Tbmuser}, {@link Mahasiswa}, {@link Siswa}), lalu — bila
     * ditemukan tepat satu akun yang cocok dan email tujuannya valid — MENDEKRIPSI kata sandi
     * tersimpan dan MENGIRIMKANNYA APA ADANYA (plain text) lewat email ke alamat terdaftar.
     *
     * <p>
     * <b>Lihat peringatan keamanan pada javadoc kelas ini (poin #1 dan #2)</b> — method ini
     * adalah SATU-SATUNYA tempat kata sandi asli (hasil dekripsi) dikirim keluar sistem (via
     * email), yang HANYA mungkin dilakukan karena kata sandi disimpan sebagai enkripsi
     * reversibel, bukan hash satu-arah. Ini bukan tautan reset password sekali pakai — pengguna
     * menerima kata sandi lama mereka apa adanya.
     * </p>
     *
     * <p>
     * Urutan prioritas pencocokan akun: {@link Dosen} (via relasi {@code Tbmuser#getDosen()}) →
     * {@link Pegawai} (via {@code Tbmuser#getPegawai()}) → {@link Tbmuser} generik →
     * {@link Mahasiswa} → {@link Siswa}. Bila ditemukan LEBIH DARI SATU akun yang cocok pada
     * salah satu jenis entitas ({@code daftarUser}/{@code daftarMahasiswa}/{@code daftarSiswa}
     * masing-masing dibatasi maksimum 2 hasil untuk deteksi duplikasi), method menolak mengirim
     * apa pun dan mengembalikan pesan yang meminta pengguna menghubungi admin — demi keamanan,
     * kata sandi tidak dikirim bila identitas ambigu. Bila email tujuan kosong/tidak valid,
     * dikembalikan pesan yang meminta pengguna melengkapi data email lewat admin (tanpa mengirim
     * email apa pun).
     * </p>
     *
     * @param username userId ({@link Tbmuser}/{@link Dosen}/{@link Pegawai}), NIM ({@link
     *                 Mahasiswa}), atau nomor induk nasional ({@link Siswa}) yang kata sandinya
     *                 ingin dipulihkan
     * @return pesan hasil dalam Bahasa Indonesia untuk ditampilkan ke pengguna — bisa berupa
     *         konfirmasi pengiriman sukses, pesan penolakan (akun ganda/ambigu), pesan email
     *         belum terdaftar, pesan akun tidak ditemukan, atau pesan kegagalan teknis
     * @throws Exception dideklarasikan pada tanda tangan namun pada praktiknya sebagian besar
     *                    kegagalan (query database, dekripsi, pengiriman email) ditangkap secara
     *                    internal dan dikembalikan sebagai pesan teks, bukan dilempar ulang
     */
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
            List<Tbmuser> daftarUser = session.createCriteria(Tbmuser.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                    .add(Restrictions.eq("userId", username)).setMaxResults(2).list();
            List<Mahasiswa> daftarMahasiswa = session.createCriteria(Mahasiswa.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                    .add(Restrictions.eq("nim", username)).addOrder(Order.desc("id")).setMaxResults(2).list();
            List<Siswa> daftarSiswa = session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
                    .add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
                    .add(Restrictions.eq("nomorIndukNasional", username)).addOrder(Order.desc("id"))
                    .setMaxResults(2).list();
            if (daftarUser.size() > 1 || daftarMahasiswa.size() > 1 || daftarSiswa.size() > 1) {
                return "ID pengguna terdaftar lebih dari satu. Demi keamanan, kata sandi tidak dikirim. Silakan hubungi admin untuk memperbaiki data pengguna.";
            }
            user = daftarUser.isEmpty() ? null : daftarUser.get(0);
            mahasiswa = daftarMahasiswa.isEmpty() ? null : daftarMahasiswa.get(0);
            siswa = daftarSiswa.isEmpty() ? null : daftarSiswa.get(0);
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
