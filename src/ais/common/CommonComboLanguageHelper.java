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
public class CommonComboLanguageHelper extends Common {

	private static final String TAB_CARACTER = " ";
	private static final Object BAHASA_CONFIG_LOCK = new Object();

	private static String buildBahasaKey(String prefix, String defaultBahasa) {
		if (defaultBahasa == null) {
			return "";
		}
		String key = org.apache.commons.lang3.StringUtils.replace(defaultBahasa, " ", "_").toLowerCase().trim();
		key = org.apache.commons.lang3.StringUtils.replace(key, "/", "").toLowerCase().trim();
		key = org.apache.commons.lang3.StringUtils.replace(key, "&", "_").toLowerCase().trim();
		key = org.apache.commons.lang3.StringUtils.replace(key, "<", "").toLowerCase().trim();
		key = org.apache.commons.lang3.StringUtils.replace(key, ">", "").toLowerCase().trim();
		return (prefix == null || prefix.trim().length() == 0 ? "" : prefix.trim() + "_") + key;
	}

	private static String ambilBahasaDariMemory(String key, String currentLang) {
		try {
			if (currentLang == null) {
				currentLang = Tbmuser.INDONESIA;
			}
			if (currentLang.equalsIgnoreCase(Tbmuser.INDONESIA)) {
				return MemoryDbUtil.getBahasaIndonesias().containsKey(key) ? MemoryDbUtil.getBahasaIndonesias().get(key) : null;
			}
			if (currentLang.equalsIgnoreCase(Tbmuser.ARAB)) {
				return MemoryDbUtil.getBahasaArabs().containsKey(key) ? MemoryDbUtil.getBahasaArabs().get(key) : null;
			}
			if (currentLang.equalsIgnoreCase(Tbmuser.MANDARIN)) {
				return MemoryDbUtil.getBahasaMandarins().containsKey(key) ? MemoryDbUtil.getBahasaMandarins().get(key) : null;
			}
			return MemoryDbUtil.getBahasaEnglishs().containsKey(key) ? MemoryDbUtil.getBahasaEnglishs().get(key) : null;
		} catch (Exception e) {
			return null;
		}
	}

	private static String ambilNilaiBahasa(LabelBahasa labelBahasa, String currentLang, String defaultBahasa) {
		if (labelBahasa == null) {
			return defaultBahasa;
		}
		if (currentLang == null) {
			currentLang = Tbmuser.INDONESIA;
		}
		String hasil = currentLang.equalsIgnoreCase(Tbmuser.INDONESIA) ? labelBahasa.getIndonesia()
				: currentLang.equalsIgnoreCase(Tbmuser.ARAB) ? labelBahasa.getArab()
				: currentLang.equalsIgnoreCase(Tbmuser.MANDARIN) ? labelBahasa.getMandarin() : labelBahasa.getEnglish();
		return hasil == null || hasil.trim().length() == 0 ? defaultBahasa : hasil;
	}

	// ============ AUTO-TERJEMAH label yang belum diterjemahkan (English/Arab) ============
	private static final java.util.concurrent.BlockingQueue<String[]> ANTRIAN_TERJEMAH =
			new java.util.concurrent.LinkedBlockingQueue<String[]>();
	private static final java.util.Set<String> SUDAH_DIANTRI =
			java.util.Collections.synchronizedSet(new java.util.HashSet<String>());
	private static volatile boolean workerTerjemahJalan = false;
	private static final Object WORKER_TERJEMAH_LOCK = new Object();

	/**
	 * Bila bahasa aktif bukan Indonesia dan {@code hasil} masih SAMA dengan teks Indonesia
	 * ({@code indonesia}) — artinya label belum diterjemahkan — maka terjemahkan otomatis via
	 * {@link KamusBahasaInternal}. Bila kamus mengenali (hasil berbeda dari Indonesia), perbarui cache
	 * memori &amp; jadwalkan penyimpanan permanen ke tabel {@code label_bahasa} (asinkron), lalu
	 * kembalikan terjemahannya. Bila kamus tak mengenali, kembalikan apa adanya (bisa diisi manual).
	 */
	private static String autoTerjemahBilaPerlu(String key, String currentLang, String indonesia, String hasil) {
		try {
			if (currentLang == null || currentLang.equalsIgnoreCase(Tbmuser.INDONESIA)) {
				return hasil;
			}
			if (key == null || hasil == null || indonesia == null || indonesia.trim().length() == 0) {
				return hasil;
			}
			if (!hasil.trim().equalsIgnoreCase(indonesia.trim())) {
				return hasil; // sudah diterjemahkan (berbeda dari Indonesia)
			}
			String langKode = currentLang.equalsIgnoreCase(Tbmuser.ARAB) ? "arab"
					: currentLang.equalsIgnoreCase(Tbmuser.MANDARIN) ? "mandarin" : "english";
			String terjemah = KamusBahasaInternal.terjemah(indonesia, langKode);
			if (terjemah == null || terjemah.trim().length() == 0
					|| terjemah.trim().equalsIgnoreCase(indonesia.trim())) {
				return hasil; // kamus tak mengenali → biarkan untuk pelengkapan manual
			}
			try {
				if ("arab".equals(langKode)) {
					MemoryDbUtil.getBahasaArabs().put(key, terjemah);
				} else if ("mandarin".equals(langKode)) {
					MemoryDbUtil.getBahasaMandarins().put(key, terjemah);
				} else {
					MemoryDbUtil.getBahasaEnglishs().put(key, terjemah);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:522");
			}
			// Tampilkan hasil kamus internal SEKARANG (instan, tak memblokir render). Worker latar akan
			// meng-UPGRADE ke terjemahan Ollama (bila server AI siap) lalu menyimpannya ke DB + memori.
			enqueueSimpanTerjemah(key, langKode, terjemah, indonesia.trim());
			return terjemah;
		} catch (Exception e) {
			return hasil;
		}
	}

	private static void enqueueSimpanTerjemah(String key, String langKode, String terjemahInternal, String indonesia) {
		try {
			String tandaDedup = langKode + "::" + key;
			if (!SUDAH_DIANTRI.add(tandaDedup)) {
				return;
			}
			ANTRIAN_TERJEMAH.offer(new String[] { key, langKode, terjemahInternal, indonesia });
			pastikanWorkerTerjemahJalan();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:541");
		}
	}

	private static void pastikanWorkerTerjemahJalan() {
		if (workerTerjemahJalan) {
			return;
		}
		synchronized (WORKER_TERJEMAH_LOCK) {
			if (workerTerjemahJalan) {
				return;
			}
			workerTerjemahJalan = true;
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					jalankanWorkerTerjemah();
				}
			}, "auto-terjemah-label");
			t.setDaemon(true);
			t.start();
		}
	}

	private static void jalankanWorkerTerjemah() {
		while (true) {
			String[] tugas = null;
			try {
				tugas = ANTRIAN_TERJEMAH.poll(30, java.util.concurrent.TimeUnit.SECONDS);
			} catch (InterruptedException ie) { ais.common.ErrorAuditUtil.record(ie, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:570");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:571");
			}
			if (tugas == null) {
				synchronized (WORKER_TERJEMAH_LOCK) {
					if (ANTRIAN_TERJEMAH.isEmpty()) {
						workerTerjemahJalan = false;
						return;
					}
				}
				continue;
			}
			simpanTerjemahKeDatabase(tugas[0], tugas[1], tugas[2], tugas.length > 3 ? tugas[3] : null);
		}
	}

	private static void simpanTerjemahKeDatabase(String key, String langKode, String terjemahInternal, String indonesia) {
		org.hibernate.Session session = null;
		org.hibernate.Transaction tx = null;
		try {
			// Di thread LATAR (aman memblokir): coba UPGRADE ke Ollama bila server AI siap; bila tidak/gagal,
			// AiTerjemah otomatis kembali ke kamus internal. Fallback akhir = hasil internal yg sudah dihitung.
			String terjemah = terjemahInternal;
			if (indonesia != null && indonesia.trim().length() > 0) {
				try {
					String upg = ais.common.AiTerjemah.terjemah(indonesia, langKode);
					if (upg != null && upg.trim().length() > 0 && !upg.trim().equalsIgnoreCase(indonesia.trim())) {
						terjemah = upg;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:599");
				}
			}
			if (terjemah == null || terjemah.trim().length() == 0) {
				return;
			}
			// Selaraskan cache memori dgn hasil final (agar render berikutnya menampilkan versi terbaik).
			try {
				if ("arab".equalsIgnoreCase(langKode)) {
					MemoryDbUtil.getBahasaArabs().put(key, terjemah);
				} else if ("mandarin".equalsIgnoreCase(langKode)) {
					MemoryDbUtil.getBahasaMandarins().put(key, terjemah);
				} else {
					MemoryDbUtil.getBahasaEnglishs().put(key, terjemah);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:614");
			}
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			String kolom = "arab".equalsIgnoreCase(langKode) ? "arab"
					: "mandarin".equalsIgnoreCase(langKode) ? "mandarin" : "english";
			// Hanya perbarui bila kolom masih kosong / sama dengan Indonesia (belum diisi manual).
			session.createQuery("update ais.database.model.LabelBahasa set " + kolom + " = :v where nama = :n"
					+ " and (" + kolom + " is null or " + kolom + " = '' or " + kolom + " = indonesia)")
					.setString("v", terjemah).setString("n", key).executeUpdate();
			tx.commit();
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:629");
				}
			}
		} finally {
			if (session != null) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:636");
				}
			}
		}
	}

	private static void simpanBahasaKeMemory(String key, LabelBahasa labelBahasa, String defaultBahasa) {
		if (key == null || key.trim().length() == 0) {
			return;
		}
		String indonesia = labelBahasa == null || labelBahasa.getIndonesia() == null ? defaultBahasa : labelBahasa.getIndonesia();
		String english = labelBahasa == null || labelBahasa.getEnglish() == null ? defaultBahasa : labelBahasa.getEnglish();
		String arab = labelBahasa == null || labelBahasa.getArab() == null ? defaultBahasa : labelBahasa.getArab();
		String mandarin = labelBahasa == null || labelBahasa.getMandarin() == null ? defaultBahasa : labelBahasa.getMandarin();
		try {
			MemoryDbUtil.getBahasaIndonesias().put(key, indonesia);
			MemoryDbUtil.getBahasaEnglishs().put(key, english);
			MemoryDbUtil.getBahasaArabs().put(key, arab);
			MemoryDbUtil.getBahasaMandarins().put(key, mandarin);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:655");
		}
	}

	private static LabelBahasa cariLabelBahasa(Session session, String key) {
		if (session == null || key == null || key.trim().length() == 0) {
			return null;
		}
		return (LabelBahasa) session.createCriteria(LabelBahasa.class).add(Restrictions.eq("nama", key))
				.setMaxResults(1).uniqueResult();
	}

	private static String ambilAtauBuatLabelBahasa(String key, String defaultBahasa, String currentLang) {
		LabelBahasa labelBahasa = null;
		Session session = null;

		try {
			session = HibernateUtil.currentNativeSession();
			labelBahasa = cariLabelBahasa(session, key);
			if (labelBahasa != null) {
				simpanBahasaKeMemory(key, labelBahasa, defaultBahasa);
				return ambilNilaiBahasa(labelBahasa, currentLang, defaultBahasa);
			}
		} catch (Exception e) {
			return defaultBahasa;
		} finally {
			try {
				HibernateUtil.closeSession();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:683");
			}
		}

		synchronized (BAHASA_CONFIG_LOCK) {
			Transaction transaction = null;
			try {
				session = HibernateUtil.currentNativeSession();
				labelBahasa = cariLabelBahasa(session, key);
				if (labelBahasa != null) {
					simpanBahasaKeMemory(key, labelBahasa, defaultBahasa);
					return ambilNilaiBahasa(labelBahasa, currentLang, defaultBahasa);
				}

				labelBahasa = new LabelBahasa();
				labelBahasa.setNama(key);
				// Seed default 3 bahasa dari file konfigurasi (WEB-INF/DEFAULT_BAHASA_INDONESIA.conf,
				// DEFAULT_ENGLISH.conf, DEFAULT_ARABIC.conf). Bila kunci tak ada di seed → fallback ke teks
				// Indonesia asli. Setelah baris ini masuk DB, DB menjadi acuan (dapat disunting).
				String seedId = DefaultBahasaSeed.getIndonesia(key);
				String seedEn = DefaultBahasaSeed.getEnglish(key);
				String seedAr = DefaultBahasaSeed.getArab(key);
				String baseIndonesia = seedId != null && seedId.trim().length() > 0 ? seedId : defaultBahasa;
				labelBahasa.setIndonesia(baseIndonesia);
				// English/Arab: pakai nilai seed dari file .conf bila tersedia; jika tidak, TERJEMAHKAN OTOMATIS
				// dari teks Indonesia memakai kamus lokal (KamusBahasaInternal, manual-assisted). Kata/istilah yang
				// belum dikenal kamus dibiarkan apa adanya untuk dilengkapi manual lewat layar Label Bahasa.
				labelBahasa.setEnglish(seedEn != null && seedEn.trim().length() > 0 ? seedEn
						: KamusBahasaInternal.terjemah(baseIndonesia, "english"));
				labelBahasa.setArab(seedAr != null && seedAr.trim().length() > 0 ? seedAr
						: KamusBahasaInternal.terjemah(baseIndonesia, "arab"));
				labelBahasa.setMandarin(KamusBahasaInternal.terjemah(baseIndonesia, "mandarin"));

				transaction = session.getTransaction();
				if (transaction == null || !transaction.isActive()) {
					transaction = session.beginTransaction();
				}
				session.save(labelBahasa);
				transaction.commit();

				simpanBahasaKeMemory(key, labelBahasa, defaultBahasa);
				// PENTING: baris English/Arab/Mandarin BARU SAJA diisi via KamusBahasaInternal di atas —
				// kembalikan sesuai bahasa aktif SEKARANG (bukan selalu Indonesia). Sebelumnya method ini
				// selalu `return defaultBahasa`, sehingga string yang baru PERTAMA KALI dirender (row belum
				// ada di DB) selalu tampil Indonesia walau kamus internal sudah punya terjemahannya —
				// terjemahan baru "muncul" pada kunjungan BERIKUTNYA setelah row ada (bug ini tidak terlihat
				// pada label lama yang sudah pernah dirender sebelumnya, karena row-nya sudah ada duluan).
				return ambilNilaiBahasa(labelBahasa, currentLang, defaultBahasa);
			} catch (Exception insertException) {
				rollbackBahasaTransaction(transaction, session);
				return ambilLabelBahasaSetelahGagalInsert(key, defaultBahasa, currentLang);
			} finally {
				try {
					HibernateUtil.closeSession();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:731");
				}
			}
		}
	}

	/**
	 * Pilih nilai bahasa sesuai bahasa aktif dari tiga argumen (fallback ke Indonesia bila kosong).
	 */
	private static String pilihBahasa(String indonesia, String english, String arab, String currentLang) {
		if (currentLang == null) {
			currentLang = Tbmuser.INDONESIA;
		}
		String hasil;
		if (currentLang.equalsIgnoreCase(Tbmuser.ARAB)) {
			hasil = arab;
		} else if (currentLang.equalsIgnoreCase(Tbmuser.INDONESIA)) {
			hasil = indonesia;
		} else {
			hasil = english;
		}
		return hasil == null || hasil.trim().length() == 0 ? indonesia : hasil;
	}

	/**
	 * Ambil terjemahan untuk {@code key}; bila BELUM ADA di tabel LabelBahasa, buat satu baris berisi KETIGA
	 * bahasa (Indonesia/English/Arab) sekaligus dari argumen. Mirip {@code ambilAtauBuatLabelBahasa} namun
	 * kolom English &amp; Arab diisi nilai NYATA (bukan salinan teks Indonesia).
	 */
	private static String ambilAtauBuatLabelBahasaTiga(String key, String indonesia, String english, String arab,
			String currentLang) {
		LabelBahasa labelBahasa = null;
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			labelBahasa = cariLabelBahasa(session, key);
			if (labelBahasa != null) {
				simpanBahasaKeMemory(key, labelBahasa, indonesia);
				return ambilNilaiBahasa(labelBahasa, currentLang, indonesia);
			}
		} catch (Exception e) {
			return pilihBahasa(indonesia, english, arab, currentLang);
		} finally {
			try {
				HibernateUtil.closeSession();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:776");
			}
		}

		synchronized (BAHASA_CONFIG_LOCK) {
			Transaction transaction = null;
			try {
				session = HibernateUtil.currentNativeSession();
				labelBahasa = cariLabelBahasa(session, key);
				if (labelBahasa != null) {
					simpanBahasaKeMemory(key, labelBahasa, indonesia);
					return ambilNilaiBahasa(labelBahasa, currentLang, indonesia);
				}

				labelBahasa = new LabelBahasa();
				labelBahasa.setNama(key);
				labelBahasa.setIndonesia(indonesia);
				labelBahasa.setEnglish(english == null || english.trim().length() == 0 ? indonesia : english);
				labelBahasa.setArab(arab == null || arab.trim().length() == 0 ? indonesia : arab);

				transaction = session.getTransaction();
				if (transaction == null || !transaction.isActive()) {
					transaction = session.beginTransaction();
				}
				session.save(labelBahasa);
				transaction.commit();

				simpanBahasaKeMemory(key, labelBahasa, indonesia);
				return pilihBahasa(indonesia, english, arab, currentLang);
			} catch (Exception insertException) {
				rollbackBahasaTransaction(transaction, session);
				return ambilLabelBahasaSetelahGagalInsert(key, indonesia, currentLang);
			} finally {
				try {
					HibernateUtil.closeSession();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:811");
				}
			}
		}
	}

	/**
	 * <h3>Terjemahan MULTI-BAHASA inline: Indonesia / English / Arab</h3>
	 *
	 * <p>Mengembalikan teks sesuai bahasa aktif pengguna. Bila kunci (dibentuk dari teks Indonesia) BELUM
	 * ADA di tabel {@code LabelBahasa}, method ini otomatis meng-INSERT satu baris berisi KETIGA bahasa
	 * sekaligus dari argumen yang diberikan &mdash; sehingga developer dapat menuliskan terjemahan lengkap
	 * langsung di kode. Setelah tersimpan, terjemahan dapat disunting per-kampus lewat data DB.</p>
	 *
	 * <p>Contoh: {@code Common.bahasa("Data berhasil disimpan.", "Data saved successfully.", "تم حفظ البيانات بنجاح.")}</p>
	 *
	 * @param indonesia teks Bahasa Indonesia (WAJIB &mdash; juga menjadi kunci &amp; fallback)
	 * @param english   teks Bahasa Inggris (boleh kosong &rarr; fallback ke Indonesia)
	 * @param arab      teks Bahasa Arab (boleh kosong &rarr; fallback ke Indonesia)
	 * @return teks sesuai bahasa aktif
	 */
	public static String bahasa(String indonesia, String english, String arab) {
		try {
			if (indonesia == null || indonesia.trim().length() == 0) {
				return indonesia == null ? "" : indonesia;
			}
			String currentLang = Common.currentLang();
			String hasil;
			if (!ConstantValues.penggunaanLabelBahasa) {
				hasil = pilihBahasa(indonesia, english, arab, currentLang);
			} else {
				String key = buildBahasaKey(null, indonesia.trim());
				if (key == null || key.trim().length() == 0) {
					hasil = pilihBahasa(indonesia, english, arab, currentLang);
				} else {
					String memo = ambilBahasaDariMemory(key, currentLang);
					hasil = memo != null ? memo
							: ambilAtauBuatLabelBahasaTiga(key, indonesia.trim(), english, arab, currentLang);
				}
			}
			if (hasil == null) {
				hasil = indonesia;
			}
			// Tandai "sudah diterjemahkan" (sentinel) agar TIDAK diterjemahkan ulang oleh
			// getBahasaConfig()/MyMessageboxConfig.show() bila hasilnya diteruskan ke sana.
			return hasil.endsWith(TAB_CARACTER) ? hasil : hasil + TAB_CARACTER;
		} catch (Exception e) {
			return indonesia;
		}
	}

	/**
	 * Sama seperti {@link #bahasa(String, String, String)} namun mendukung placeholder berparameter
	 * {@code {V1}},{@code {V2}},... yang disubstitusi dengan {@code args} SETELAH pemilihan bahasa. Template
	 * tiap bahasa boleh memuat placeholder yang sama. Contoh:
	 * {@code Common.bahasaFormat("Data \"{V1}\" tersimpan.", "Data \"{V1}\" saved.", "...", nama)}.
	 */
	public static String bahasaFormat(String indonesia, String english, String arab, Object... args) {
		String hasil = bahasa(indonesia, english, arab);
		if (hasil != null && args != null) {
			for (int i = 0; i < args.length; i++) {
				hasil = hasil.replace("{V" + (i + 1) + "}", args[i] == null ? "" : String.valueOf(args[i]));
			}
		}
		return hasil == null ? "" : hasil;
	}

	private static void rollbackBahasaTransaction(Transaction transaction, Session session) {
		try {
			if (transaction != null && transaction.isActive()) {
				transaction.rollback();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:883");
		}
		try {
			if (session != null) {
				session.clear();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:889");
		}
		try {
			HibernateUtil.closeSession();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:893");
		}
	}

	private static String ambilLabelBahasaSetelahGagalInsert(String key, String defaultBahasa, String currentLang) {
		Session retrySession = null;
		try {
			retrySession = HibernateUtil.currentNativeSession();
			LabelBahasa labelBahasa = cariLabelBahasa(retrySession, key);
			if (labelBahasa != null) {
				simpanBahasaKeMemory(key, labelBahasa, defaultBahasa);
				return ambilNilaiBahasa(labelBahasa, currentLang, defaultBahasa);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:906");
		} finally {
			try {
				HibernateUtil.closeSession();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:910");
			}
		}
		MemoryDbUtil.getBahasaIndonesias().put(key, defaultBahasa);
		MemoryDbUtil.getBahasaEnglishs().put(key, defaultBahasa);
		MemoryDbUtil.getBahasaArabs().put(key, defaultBahasa);
		return defaultBahasa;
	}


	private static final Logger log = Logger.getLogger(CommonComboLanguageHelper.class);
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
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:947");
		}
	}




	public static Combobox initJenisPembayaranMahasiswa(Combobox jenisPembayaranMahasiswa) {
		return CommonHelperClass.initJenisPembayaranMahasiswa(jenisPembayaranMahasiswa);
	}



	public static Combobox initJenisPembayaranBiodataCalonMahasiswa(Combobox jenisPembayaranMahasiswa) {
		return CommonHelperClass.initJenisPembayaranBiodataCalonMahasiswa(jenisPembayaranMahasiswa);
	}



	public static Combobox initJenisPembayaranMahasiswaDanBiodataCalonMahasiswa(Combobox jenisPembayaranMahasiswa) {
		return CommonHelperClass.initJenisPembayaranMahasiswaDanBiodataCalonMahasiswa(jenisPembayaranMahasiswa);
	}



	public static Combobox initJenisSemester(Combobox jenisSemester) {
		return CommonHelperClass.initJenisSemester(jenisSemester);
	}



	public static Combobox initJenisSemester(Combobox jenisSemester, boolean sp) {
		return CommonHelperClass.initJenisSemester(jenisSemester, sp);
	}



	public static TreeMap<Integer, String[]> generateStatusSemester(Mahasiswa mahasiswa) {
		return CommonHelperClass.generateStatusSemester(mahasiswa);
	}



	public static List<String[]> generateSemestersForGrid(Mahasiswa mahasiswa, int mulai, int sampai,
			Integer semesterPendek) {
		return CommonHelperClass.generateSemestersForGrid(mahasiswa, mulai, sampai, semesterPendek);
	}



	public static List<String[]> generateSemestersForGridTahapan(Mahasiswa mahasiswa, int jumlahTahapan) {
		return CommonHelperClass.generateSemestersForGridTahapan(mahasiswa, jumlahTahapan);
	}



	public static Combobox createComboKonfigurasi(Combobox conf) {
		return CommonHelperClass.createComboKonfigurasi(conf);
	}



	public static void initFakultasDanJurusan(final Combobox fakultas, final Combobox jurusan,
			final Combobox searchfakultas, final Combobox searchjurusan) {
		initFakultasDanJurusanDanSemua(fakultas, jurusan, searchfakultas, searchjurusan);
	}



	public static void initFakultasDanJurusanDanSemua(Combobox fakultas, Combobox jurusan) {
		initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);
	}



	public static void initFakultasDanJurusanDanSemua(Combobox fakultas, Combobox jurusan, Combobox searchfakultas,
			Combobox searchjurusan) {
		initFakultasDanJurusanDanSemua(fakultas, jurusan, searchfakultas, searchjurusan, true);
	}



	public static void initFakultasDanJurusanDanSemua(Combobox fakultas, Combobox jurusan, Combobox searchfakultas,
			Combobox searchjurusan, boolean pilih) {
		InitComboUtil.initFakultasDanJurusanDanSemua(fakultas, jurusan, searchfakultas, searchjurusan, pilih);
	}



	public static void initYayasanDanSekolahDanSemua(Combobox yayasan, Combobox sekolah, Combobox searchyayasan,
			Combobox searchsekolah) {
		initYayasanDanSekolahDanSemua(yayasan, sekolah, searchyayasan, searchsekolah, true, false);
	}



	public static void initYayasanDanSekolahDanSemua(Combobox yayasan, Combobox sekolah, Combobox searchyayasan,
			Combobox searchsekolah, boolean pilih, boolean otomatisPilih) {
		InitComboUtil.initYayasanDanSekolahDanSemua(yayasan, sekolah, searchyayasan, searchsekolah, pilih,
				otomatisPilih);
	}



	public static void initJurusanDanSemua(Combobox jurusan, Jenjang jenjang) {
		initJurusanDanSemua(jurusan, jenjang, "== Klik disini untuk pilih ==");
	}



	public static void initJurusanDanSemua(Combobox jurusan, Jenjang jenjang, String label) {
		InitComboUtil.initJurusanDanSemua(jurusan, jenjang, label);
	}



	public static Combobox createComboJenisPembayaran(Combobox jenisPembayaran) {
		return CommonHelperClass.createComboJenisPembayaran(jenisPembayaran);
	}



	public static Combobox createComboJenisPembayaranDanSemua(Combobox jenisPembayaran) {
		return CommonHelperClass.createComboJenisPembayaranDanSemua(jenisPembayaran);
	}



	public static LangUtil initLaguage() {
		try {
			//
			// Component component =
			// ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();
			// Common.ubahBahasa(component);

			if (Sessions.getCurrent() != null && Common.getCurrentUser() == null) {
				return null;
			}

			LangUtil langUtil = (LangUtil) Sessions.getCurrent().getAttribute("langUtil");
			if (langUtil == null) {
				langUtil = new LangUtil("ais", Common.locale);
				Sessions.getCurrent().setAttribute("langUtil", langUtil);
			}
			Component component = ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();
			langUtil.setLanguage(component);
			return langUtil;
		} catch (Exception e) {
			return null;
		}
	}



	public static String getBahasaConfig(String defaultBahasa) {
		return getBahasaConfig("", defaultBahasa);
	}



	public static String getBahasaConfig(String prefix, String defaultBahasa) {

		String originalBahasa = defaultBahasa;
		boolean containBintangKurungDanTangan = false;
		boolean containBintangKurung = false;
		boolean containBintang = false;

		try {

			if (defaultBahasa != null && defaultBahasa.endsWith(TAB_CARACTER)) {
				return defaultBahasa;
			}

			if (defaultBahasa == null || defaultBahasa.trim().isEmpty()) {
				return "";
			}

			defaultBahasa = defaultBahasa.trim();
			originalBahasa = defaultBahasa;

			if (!ConstantValues.penggunaanLabelBahasa) {
				return defaultBahasa;
			}

			try {
				String lower = defaultBahasa.toLowerCase();
				if (lower.endsWith(".jpeg") || lower.endsWith(".jpg") || lower.endsWith(".png")
						|| lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx")
						|| lower.endsWith(".ppt") || lower.endsWith(".pptx") || lower.endsWith(".xlsx")
						|| lower.endsWith("..")) {
					return defaultBahasa;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:1139");
			}
			try {
				if (defaultBahasa.contains("..")) {
					return defaultBahasa;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:1145");
			}

			try {
				if (Common.isNumber(defaultBahasa.split(" ")[0].trim())) {
					return defaultBahasa;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:1152");
			}

			try {
				// PENTING: "." di String.split(regex) adalah REGEX (cocok SEMUA karakter), bukan
				// titik literal -- split(".") pada string apa pun menghasilkan array KOSONG (semua
				// segmen kosong lalu dibuang sbg trailing-empty oleh Java) -> [0] SELALU melempar
				// ArrayIndexOutOfBoundsException. Titik literal WAJIB di-escape ("\\.").
				String[] _splitDotParts = defaultBahasa.split("\\.");
				if (_splitDotParts.length > 0 && Common.isNumber(_splitDotParts[0].trim())) {
					return defaultBahasa;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:1159");
			}

			try {
				if (Common.isNumber(defaultBahasa.split(",")[0].trim())) {
					return defaultBahasa;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:1166");
			}

			try {
				if (defaultBahasa.charAt(0) >= 48 && defaultBahasa.charAt(0) <= 57) {
					return defaultBahasa;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:1173");
			}

			try {
				if (Common.isNumber(org.apache.commons.lang3.StringUtils.replace(defaultBahasa, ".", ""))) {
					return defaultBahasa;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:1180");
			}

			try {
				if (Common.isNumber(org.apache.commons.lang3.StringUtils.replace(defaultBahasa, ",", ""))) {
					return defaultBahasa;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:1187");
			}

			if (defaultBahasa.toLowerCase().contains("logged in")) {
				return defaultBahasa;
			}

			try {
				String[] slashParts = StringUtils.split(defaultBahasa, "/");
				if (slashParts != null && slashParts.length > 0 && slashParts[0] != null
						&& Common.isNumber(slashParts[0].trim())) {
					return defaultBahasa;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:1198");
			}

			try {
				int digits = 0;
				for (int i = 0; i < defaultBahasa.length(); i++) {
					if (defaultBahasa.charAt(i) >= 48 && defaultBahasa.charAt(i) <= 57) {
						digits++;
					}
				}
				if (digits > 2) {
					return defaultBahasa;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:1211");
			}

			if (defaultBahasa.length() > 300) {
				return defaultBahasa;
			}

			containBintangKurungDanTangan = StringUtils.contains(defaultBahasa, "(*) 👉");
			if (containBintangKurungDanTangan) {
				defaultBahasa = org.apache.commons.lang3.StringUtils.replace(defaultBahasa, "(*) 👉", "").trim();
			}
			containBintangKurung = StringUtils.contains(defaultBahasa, "(*)");
			if (containBintangKurung) {
				defaultBahasa = org.apache.commons.lang3.StringUtils.replace(defaultBahasa, "(*)", "").trim();
			}

			containBintang = StringUtils.contains(defaultBahasa, "*");
			if (containBintang) {
				defaultBahasa = org.apache.commons.lang3.StringUtils.replace(defaultBahasa, "*", "").trim();
			}

			String key = buildBahasaKey(prefix, defaultBahasa);

			if (key == null || key.trim().isEmpty()) {
				return defaultBahasa;
			}

			if (Common.isNumber(key)) {
				return defaultBahasa;
			}

			try {
				// FIX AIOOBE: StringUtils.split(key, "-") menghasilkan array KOSONG bila key
				// hanya terdiri dari karakter "-" (mis. key == "-"), karena commons-lang
				// membuang token kosong di sekitar separator -- akses [0] tanpa cek panjang
				// melempar ArrayIndexOutOfBoundsException: 0. Cek panjang dulu, jika kosong
				// lewati saja pengecekan ini (lanjut ke logika berikutnya).
				String[] keyParts = StringUtils.split(key, "-");
				if (keyParts != null && keyParts.length > 0 && Common.isNumber(keyParts[0].trim())) {
					return defaultBahasa;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:1246");
			}

			String currentLang = Common.currentLang();
			String hasil = ambilBahasaDariMemory(key, currentLang);
			if (hasil == null) {
				hasil = ambilAtauBuatLabelBahasa(key, defaultBahasa, currentLang);
			}

			if (hasil == null || hasil.trim().isEmpty()) {
				hasil = defaultBahasa;
			}

			// AUTO-TERJEMAH: bila bahasa aktif English/Arab tetapi nilai masih SAMA dengan teks Indonesia
			// (belum diterjemahkan), terjemahkan otomatis via kamus lokal, tampilkan hasilnya, dan simpan
			// permanen ke tabel label_bahasa (asinkron) agar tak perlu diterjemah ulang.
			hasil = autoTerjemahBilaPerlu(key, currentLang, defaultBahasa, hasil);

			return hasil + (containBintangKurungDanTangan ? " (*) 👉" : "") + (containBintangKurung ? " (*)" : "")
					+ (containBintang ? " *" : "") + TAB_CARACTER;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:1266");
			// Jangan menampilkan error bahasa ke user, karena method ini sering dipanggil
			// dari proses render UI. Jika terjadi error, cukup fallback ke teks asal.
		}
		return originalBahasa == null ? "" : originalBahasa;
	}



	/**
	 * Terapkan pilihan bahasa. Menerima kode {@code "id"/"en"/"ar"} maupun nilai penuh
	 * ({@code Tbmuser.INDONESIA/ENGLISH/ARAB}). Selalu menyetel bahasa aktif ke SESSION
	 * (dipakai {@link Common#currentLang()} &amp; halaman JSP untuk RTL). Bila pengguna SEDANG LOGIN
	 * ({@code Common.getCurrentUser() != null}), pilihan juga DISIMPAN PERMANEN ke kolom
	 * {@code bahasa} pada tabel pengguna (Tbmuser) dan entitas terkait yang ada
	 * (Mahasiswa/Siswa/Dosen/Pegawai/Guru).
	 */
	public static void initBahasaParameter(String lang) {
		try {
			if (lang == null || lang.trim().isEmpty()) {
				return;
			}
			String kode = lang.trim();
			String currentLang = null;
			if (kode.equalsIgnoreCase("id") || kode.equalsIgnoreCase(Tbmuser.INDONESIA)) {
				currentLang = Tbmuser.INDONESIA;
			} else if (kode.equalsIgnoreCase("en") || kode.equalsIgnoreCase(Tbmuser.ENGLISH)) {
				currentLang = Tbmuser.ENGLISH;
			} else if (kode.equalsIgnoreCase("ar") || kode.equalsIgnoreCase(Tbmuser.ARAB)) {
				currentLang = Tbmuser.ARAB;
			} else if (kode.equalsIgnoreCase("zh") || kode.equalsIgnoreCase(Tbmuser.MANDARIN)
					|| kode.equalsIgnoreCase("mandarin") || kode.equalsIgnoreCase("china")) {
				currentLang = Tbmuser.MANDARIN;
			}
			if (currentLang == null) {
				return;
			}

			// 1) Simpan ke session ZK (bahasa aktif untuk render UI ZK).
			// PENTING: halaman JSP MURNI publik (mis. landing PMB via header.jsp) diakses TANPA
			// ZK Execution/Desktop aktif (visitor anonymous, belum ada session ZK). Sessions.getCurrent(true)
			// bergantung pada Execution ZK yang sedang berjalan; bila tidak ada, panggilan ini bisa
			// menghasilkan objek session null (atau NPE internal ZK) sehingga .setAttribute(...) NPE.
			// Guard dengan cek Executions.getCurrent() != null dulu supaya jalur JSP murni tidak
			// bergantung pada exception+catch untuk kontrol alur normal (bahasa tetap tersimpan via
			// HttpSession pada blok 1b di bawah untuk kasus ini).
			try {
				if (org.zkoss.zk.ui.Executions.getCurrent() != null) {
					org.zkoss.zk.ui.Session zkSession = Sessions.getCurrent(true);
					if (zkSession != null) {
						zkSession.setAttribute("current_lang", currentLang);
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:1307");
			}

			// 1b) Simpan ke HttpSession (bahasa aktif untuk JSP MURNI tanpa ZK Execution, mis. PMB/PSB).
			//     Common.currentLang() membacanya kembali via RequestContext agar bendera benar-benar berganti.
			try {
				javax.servlet.http.HttpServletRequest req = RequestContext.get();
				if (req != null) {
					req.getSession(true).setAttribute("current_lang", currentLang);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:initBahasa-httpsession");
			}

			// 2) Bila sedang login, simpan permanen ke DB (kolom bahasa) tiap entitas terkait.
			try {
				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.getId() != null) {
					simpanBahasaPenggunaKeDatabase(tbmuser, currentLang);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:1316");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:1318");
		}
	}

	/**
	 * Simpan bahasa terpilih secara PERMANEN ke kolom {@code bahasa} pada Tbmuser + entitas terkait
	 * (Mahasiswa/Siswa/Dosen/Pegawai/Guru) memakai sesi Hibernate dedicated (buka + tutup di finally)
	 * dengan HQL bulk-update by id — aman terhadap kontrak sesi ZK/JSP/thread &amp; entitas yang mungkin
	 * detached. Nilai in-memory ikut disetel agar UI langsung mencerminkan pilihan.
	 */
	private static void simpanBahasaPenggunaKeDatabase(Tbmuser tbmuser, String currentLang) {
		org.hibernate.Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			// PENTING: setiap update dalam TRANSAKSI TERPISAH. Di PostgreSQL, satu statement gagal
			// (mis. tabel 'dosen' belum punya kolom 'bahasa' → 42703) akan MERACUNI transaksi sehingga
			// commit gagal & SEMUA rollback — termasuk Tbmuser.bahasa. Dengan tx per-entitas, kegagalan
			// pada entitas terkait tidak membatalkan penyimpanan Tbmuser (yang paling penting).
			// Tbmuser diutamakan lebih dulu karena itulah sumber otoritatif yang dibaca getBahasa().
			updateBahasaEntitas(session, "ais.database.model.Tbmuser", tbmuser.getId(), currentLang);
			if (tbmuser.getMahasiswa() != null && tbmuser.getMahasiswa().getId() != null) {
				updateBahasaEntitas(session, "ais.database.model.Mahasiswa", tbmuser.getMahasiswa().getId(), currentLang);
			}
			if (tbmuser.getSiswa() != null && tbmuser.getSiswa().getId() != null) {
				updateBahasaEntitas(session, "ais.database.model.sekolah.Siswa", tbmuser.getSiswa().getId(), currentLang);
			}
			if (tbmuser.ambilDosen() != null && tbmuser.ambilDosen().getId() != null) {
				updateBahasaEntitas(session, "ais.database.model.Dosen", tbmuser.ambilDosen().getId(), currentLang);
			}
			if (tbmuser.ambilPegawai() != null && tbmuser.ambilPegawai().getId() != null) {
				updateBahasaEntitas(session, "ais.database.model.Pegawai", tbmuser.ambilPegawai().getId(), currentLang);
			}
			if (tbmuser.ambilGuru() != null && tbmuser.ambilGuru().getId() != null) {
				updateBahasaEntitas(session, "ais.database.model.sekolah.Guru", tbmuser.ambilGuru().getId(), currentLang);
			}

			// Sinkronkan objek in-memory (agar tampilan pada request yang sama langsung benar).
			try {
				tbmuser.setBahasa(currentLang);
				if (tbmuser.getMahasiswa() != null) {
					tbmuser.getMahasiswa().setBahasa(currentLang);
				}
				if (tbmuser.getSiswa() != null) {
					tbmuser.getSiswa().setBahasa(currentLang);
				}
				if (tbmuser.ambilDosen() != null) {
					tbmuser.ambilDosen().setBahasa(currentLang);
				}
				if (tbmuser.ambilPegawai() != null) {
					tbmuser.ambilPegawai().setBahasa(currentLang);
				}
				if (tbmuser.ambilGuru() != null) {
					tbmuser.ambilGuru().setBahasa(currentLang);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:1373");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:1375");
		} finally {
			if (session != null) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:1380");
				}
			}
		}
	}

	/**
	 * Update kolom bahasa satu entitas dalam TRANSAKSI SENDIRI. Bila gagal (mis. kolom belum ada →
	 * transaksi ter-poison di PostgreSQL), rollback HANYA transaksi ini; entitas lain tetap tersimpan.
	 */
	private static void updateBahasaEntitas(org.hibernate.Session session, String namaEntitas, Long id,
			String currentLang) {
		org.hibernate.Transaction tx = null;
		try {
			tx = session.beginTransaction();
			session.createQuery("update " + namaEntitas + " set bahasa = :bahasa where id = :id")
					.setString("bahasa", currentLang).setLong("id", id).executeUpdate();
			tx.commit();
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonComboLanguageHelper.java:1402");
				}
			}
		}
	}



	public static String getBahasa(String key) {
		// Konfigurasi nilai = Common.getKonfigurasi(key,
		// key.toLowerCase().replaceAll("label_", "").replaceAll("_", " "));
		return key.toLowerCase().replaceAll("label_", "").replaceAll("_", " ");
	}



	public static String getReq() {
		HttpServletRequest request = null;
		if (ExecutionsCtrl.getCurrent() != null) {
			request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		}
		if (request == null) {
			request = RequestContext.get();
		}
		String req = request.getRequestURL().toString();
		req = req.replaceAll("http://", "");
		req = req.split(":")[0];
		req = req.split("/")[0];
		return req;
	}



	public static Combobox createComboBulan(Combobox conf) {
		if (conf == null) {
			conf = new Combobox();
		}
		int i = 0;
		for (String h : BULAN) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(h);
			comboitem.setValue(i++);
			conf.appendChild(comboitem);
		}

		Common.selectComboItem(conf, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH));
		conf.setReadonly(true);
		return conf;
	}



	public static Combobox createComboHari() {
		Combobox hari = new Combobox();
		for (String h : Common.haris) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari.appendChild(comboitem);
		}
		return hari;
	}



	public static Combobox createComboHariDanSemua() {
		Combobox hari = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		hari.appendChild(comboitem);
		for (String h : Common.haris) {
			comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari.appendChild(comboitem);
		}
		hari.setReadonly(true);
		return hari;
	}



	public static void initFakultasDanJurusanData(Fakultas currentFakultas, Jurusan currentJurusan,
			Combobox searchfakultas, Combobox searchjurusan) {

		Common.initFakultasDanJurusanDanSemua(searchfakultas, searchjurusan, null, null);

		Common.selectComboItem(searchfakultas, currentFakultas, true);
		Common.selectComboItem(searchjurusan, currentJurusan, true);
	}

}
