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
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
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
import ais.action.master.SkripsiAction;
import ais.action.master.TampilanELearningAction;
import ais.action.master.employ.util.FormBiodataPegawaiUtil;
import ais.action.master.helper.AmbilDataKecamatanBanbox;
import ais.action.master.helper.BukuBahanAjarHelper;
import ais.action.master.helper.CetakAlbumWisudaAdminWindow;
import ais.action.master.helper.CetakAlbumWisudaMahasiswaHelper;
import ais.action.master.helper.ChangePasswordWindow;
import ais.action.master.helper.GenerateNoKursiDanNoRegistrasiWindow;
import ais.action.master.helper.GenerateNoKursiWindow;
import ais.action.master.helper.GenerateUndanganWisudaWindow;
import ais.action.master.helper.HistoryStatusMahasiswaUtil;
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
import ais.action.master.helper.generic.AngketGuruWindow;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.kursus.helper.KursusUtil;
import ais.action.master.library.util.LibraryUtil;
import ais.action.master.pmb.statistik.LaporanDaftarUlangMahasiswaBaru;
import ais.action.master.pmb.statistik.LaporanLulusMahasiswaBaru;
import ais.action.master.pmb.statistik.LaporanPendaftarMahasiswaBaru;
import ais.action.master.pmb.statistik.LaporanRekapJenisSeleksiMahasiswaBaru;
import ais.action.master.sekolah.GuruAction;
import ais.action.master.sekolah.helper.BukuBahanAjarMatapelajaranHelper;
import ais.action.master.sekolah.helper.JadwalPelajaranPunyaItemHelper;
import ais.action.master.sekolah.util.SekolahUtil;
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
import ais.database.model.MatakuliahPunyaBukuBahanAjar;
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
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.kkn.MahasiswaKknPersyaratan;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.Koperasi;
import ais.database.model.kursus.KomponenDataProdukKursus;
import ais.database.model.kursus.PesertaKursus;
import ais.database.model.library.Anggota;
import ais.database.model.library.HariLiburPerpustakaan;
import ais.database.model.library.Perpustakaan;
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
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyTreeitemConfig;
import ais.ui.util.MyVboxStyled;
import ais.ui.util.MyWindow;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLType;


/**
 * Helper validasi jadwal perkuliahan dan template jadwal.
 * Dipisahkan dari Common agar Common tetap ringan sebagai facade/wrapper.
 */
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class CommonScheduleValidationHelper extends Common {

	private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CommonScheduleValidationHelper.class);

	private CommonScheduleValidationHelper() {
	}

	private static String safeTrim(String value) {
		return value == null ? "" : value.trim();
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}

	private static void tampilCrudError(Exception e, String pesan) {
		Common.tampilErrorJikaAdmin(e);
		String detail = e == null || e.getMessage() == null ? "" : "\n" + e.getMessage();
		try {
			MyMessageboxConfig.showFormat("{V1}{V2}", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, pesan, detail);
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonScheduleValidationHelper.java:411");
		}
	}

	public static Criterion getMulaiSampaiCriterion(Date perkuliahanDimulai, Date perkuliahanSampai) {
			return getMulaiSampaiCriterion(perkuliahanDimulai, perkuliahanSampai, "");
		}

	public static Criterion getMulaiSampaiCriterion(Date perkuliahanDimulai, Date perkuliahanSampai, String alias) {
			Criterion criterion = Restrictions.sqlRestriction("1=1");
			if (perkuliahanDimulai != null && perkuliahanSampai != null) {

				criterion = Restrictions.or(
						Restrictions.between(alias + "perkuliahanDimulai", perkuliahanDimulai, perkuliahanSampai),
						Restrictions.between(alias + "perkuliahanSampai", perkuliahanDimulai, perkuliahanSampai));

				criterion = Restrictions.or(criterion,
						Restrictions.sqlRestriction("date('" + Common.databaseDateFormat.get().format(perkuliahanDimulai)
								+ "') between perkuliahandimulai and perkuliahansampai"));

				criterion = Restrictions.or(criterion,
						Restrictions.sqlRestriction("date('" + Common.databaseDateFormat.get().format(perkuliahanSampai)
								+ "') between perkuliahandimulai and perkuliahansampai"));

			} else if (perkuliahanDimulai != null) {
				criterion = Restrictions.and(Restrictions.ge(alias + "perkuliahanDimulai", perkuliahanDimulai),
						Restrictions.le(alias + "perkuliahanSampai", perkuliahanDimulai));
			} else if (perkuliahanSampai != null) {
				criterion = Restrictions.and(Restrictions.le(alias + "perkuliahanDimulai", perkuliahanSampai),
						Restrictions.ge(alias + "perkuliahanSampai", perkuliahanSampai));
			}

			criterion = Restrictions.or(criterion, Restrictions.isNull(alias + "perkuliahanDimulai"));
			criterion = Restrictions.or(criterion, Restrictions.isNull(alias + "perkuliahanSampai"));

			return criterion;

		}

	public static Perkuliahan checkMatakuliahKesamaanBukanParalel(Perkuliahan perkuliahan, Jurusan jurusan,
				String kelas, Matakuliah matakuliah, Integer semester, String tahunAjaran, String program,
				Html tampilWarning, Integer semesterpendek, Boolean minggu1, Boolean minggu2, Boolean minggu3,
				Boolean minggu4, Boolean minggu5, Date perkuliahanDimulai, Date perkuliahanSampai,
				Boolean merupakanRemedial) throws Exception {

			if (kelas == null) {
				return null;
			}

			Session session = HibernateUtil.currentNativeSession();
			Perkuliahan count = (Perkuliahan) (session.createCriteria(Perkuliahan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("merupakanRemedial", merupakanRemedial))

					.add(Restrictions.or(Restrictions.eq("merupakan_paralel", false),
							Restrictions.isNull("merupakan_paralel")))
					.add(perkuliahan == null || perkuliahan.getId() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ne("id", perkuliahan.getId()))
					.add(Restrictions.eq("tahunAjaran", tahunAjaran))
					.add(semesterpendek == null ? Restrictions.isNull("statusSemesterPendek")
							: Restrictions.eq("statusSemesterPendek", semesterpendek))
					.add(Restrictions.eq("program", program)).add(Restrictions.eq("jurusan", jurusan))
					.add(Restrictions.eq("semester", semester)).add(Restrictions.eq("matakuliah", matakuliah))
					.add(Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT)).setMaxResults(1).uniqueResult());
			if (count != null) {

				if (count.getRuang() == null && count.getHari() == null && count.getWaktuMulai() == null
						&& count.getDosen1() == null) {
					return null;
				}

				if (tampilWarning == null) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, Bapak/Ibu. Jadwal perkuliahan dengan matakuliah {V1} ({V2} SKS), semester {V3}, kelas {V4}, program {V5}, program studi {V6} pada tahun akademik {V7} sudah ada. Apabila Bapak/Ibu ingin membuat jadwal perkuliahan paralel, mohon centang pilihan Paralel. Langkah yang dapat dilakukan: (1) periksa kembali jadwal yang sudah ada; (2) centang pilihan Paralel bila memang menghendaki jadwal paralel; (3) simpan ulang jadwal.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, matakuliah.getNama(),
							matakuliah.getSks(), semester, kelas, program, jurusan.getNama(), tahunAjaran);
				} else {
					String val = tampilWarning.getContent() + "<li><font color='red'>";
					val += "Jadwal perkuliahan dengan matakuliah " + matakuliah.getNama() + " " + matakuliah.getSks()
							+ " SKS, semester " + semester + ", kelas " + kelas + ", program " + program + ", prodi "
							+ jurusan.getNama() + " di tahun akademik " + tahunAjaran + " sudah ada. </font></li>";

					tampilWarning.setContent(val);
				}
			}

			HibernateUtil.closeSession();

			return count;
		}

	public static Criterion createOrJadwalMulaiSelesai(Double mulai, Double selesai) {
			return mulai == null && selesai == null
					? Restrictions.and(
							(Restrictions.and(Restrictions.isNull("waktuMulai"), Restrictions.sqlRestriction("true"))),
							Restrictions.and(Restrictions.isNull("waktuSelesai"), Restrictions.sqlRestriction("true")))
					: mulai == null
							? Restrictions.and(Restrictions.isNull("waktuMulai"), Restrictions.sqlRestriction("true"))
							: selesai == null
									? Restrictions.and(Restrictions.isNull("waktuSelesai"),
											Restrictions.sqlRestriction("true"))
									: Restrictions.sqlRestriction("(waktu_mulai_d between " + mulai + " and " + selesai
											+ " or  waktu_selesai_d between " + mulai + " and " + selesai + "   or  "
											+ mulai + " between waktu_mulai_d and waktu_selesai_d " + " or  " + selesai
											+ " between waktu_mulai_d and waktu_selesai_d )");
		}

	public static Perkuliahan checkJadwalRuangPerkuliahan(Long id, Ruang ruang, String hari, Double mulai,
				Double selesai, String tahunAjaran, String jenisSemester, Html tampilWarning, Integer semesterpendek,
				Boolean minggu1, Boolean minggu2, Boolean minggu3, Boolean minggu4, Boolean minggu5,
				Date perkuliahanDimulai, Date perkuliahanSampai) throws Exception {

			if (ruang == null || hari == null || hari.isEmpty() || mulai == null || selesai == null) {
				return null;
			}

			Session session = HibernateUtil.currentNativeSession();
			List<Perkuliahan> counts = null;
			if (id == null) {
				counts = ConstantValues
						.simpleList(
								session.createCriteria(Perkuliahan.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

										.add(CommonScheduleValidationHelper.getMulaiSampaiCriterion(perkuliahanDimulai, perkuliahanSampai))

										.add(Restrictions.sqlRestriction("(minggu1 = " + minggu1 + " or minggu2 = "
												+ minggu2 + " or minggu3 = " + minggu3 + " or minggu4 = " + minggu4
												+ " or minggu5 = " + minggu5 + ")"))

										.add(semesterpendek == null ? Restrictions.isNull("statusSemesterPendek")
												: Restrictions.eq("statusSemesterPendek", semesterpendek))

										.add(Restrictions.eq("ganjilGenap", jenisSemester.toString()))
										.add(Restrictions.eq("tahunAjaran", tahunAjaran))

										.add(ruang == null ? Restrictions.isNull("ruang") : Restrictions.eq("ruang", ruang))

										.add(hari == null || hari.trim().equals("") ? Restrictions.isNull("hari")
												: Restrictions.eq("hari", hari))

										.add(CommonScheduleValidationHelper.createOrJadwalMulaiSelesai(mulai, selesai)),
								Perkuliahan.class);
			} else {
				counts = ConstantValues
						.simpleList(
								session.createCriteria(Perkuliahan.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

										.add(CommonScheduleValidationHelper.getMulaiSampaiCriterion(perkuliahanDimulai, perkuliahanSampai))

										.add(Restrictions.sqlRestriction("(minggu1 = " + minggu1 + " or minggu2 = "
												+ minggu2 + " or minggu3 = " + minggu3 + " or minggu4 = " + minggu4
												+ " or minggu5 = " + minggu5 + ")"))

										.add(semesterpendek == null ? Restrictions.isNull("statusSemesterPendek")
												: Restrictions.eq("statusSemesterPendek", semesterpendek))

										.add(Restrictions.ne("id", id))
										.add(Restrictions.eq("ganjilGenap", jenisSemester.toString()))
										.add(Restrictions.eq("tahunAjaran", tahunAjaran))

										.add(ruang == null ? Restrictions.isNull("ruang") : Restrictions.eq("ruang", ruang))

										.add(hari == null || hari.trim().equals("") ? Restrictions.isNull("hari")
												: Restrictions.eq("hari", hari))
										.add(CommonScheduleValidationHelper.createOrJadwalMulaiSelesai(mulai, selesai)),
								Perkuliahan.class);

			}

			System.out.println("checkJadwalRuangPerkuliahan -> " + counts);
			if (counts != null) {
				for (Perkuliahan count : counts) {
					if (tampilWarning == null) {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, Bapak/Ibu. Terjadi bentrok jadwal di Ruang Perkuliahan. Jadwal sudah terpakai di Program Studi {V1}, ruang {V2}, pada hari {V3} dari pukul {V4} sampai pukul {V5} oleh Dosen {V6}, matakuliah {V7}, kelas {V8} {V9}. Langkah yang dapat dilakukan: (1) periksa kembali ruang dan waktu yang mengalami bentrok; (2) pilih ruang atau jam yang masih tersedia; (3) simpan ulang jadwal setelah tidak ada bentrok.",
								"Peringatan bentrok di Ruang Perkuliahan", 1, MyMessageboxConfig.EXCLAMATION,
								count.getJurusan().getNama(),
								(count.getRuang() == null ? "" : count.getRuang().getNama()), count.getHari(),
								count.getWaktuMulai(), count.getWaktuSelesai(),
								(count.getDosen1() == null ? "" : count.getDosen1().getNama()),
								(count.getMatakuliah() == null ? "" : count.getMatakuliah().getNama()),
								count.getSemester(), count.getKelas());
					} else {
						String val = tampilWarning.getContent() + "<li><font color='red'>";
						val += "Peringatan bentrok di Ruang Perkuliahan..<br>Jadwal sudah terpakai di Prodi "
								+ count.getJurusan().getNama() + ", ruang '"
								+ (count.getRuang() == null ? "" : count.getRuang().getNama()) + "' dan hari '"
								+ count.getHari() + "' dari jam '" + count.getWaktuMulai() + "' sampai jam "
								+ count.getWaktuSelesai() + "' oleh Dosen "
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

	public static Perkuliahan checkJadwalDosen(Long id, String hari, Double mulai, Double selesai, Dosen dosen,
				String tahunAjaran, String jenisSemester, Jurusan jurusan, Matakuliah matakuliah, String kelas,
				Html tampilWarning, Integer semesterpendek, Boolean minggu1, Boolean minggu2, Boolean minggu3,
				Boolean minggu4, Boolean minggu5, Date perkuliahanDimulai, Date perkuliahanSampai) throws Exception {

			if (dosen == null || hari == null || hari.isEmpty() || mulai == null || selesai == null) {
				return null;
			}

			Session session = HibernateUtil.currentNativeSession();
			List<Perkuliahan> counts = null;
			if (id == null) {
				counts = ConstantValues
						.simpleList(
								session.createCriteria(Perkuliahan.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

										.add(CommonScheduleValidationHelper.getMulaiSampaiCriterion(perkuliahanDimulai, perkuliahanSampai))

										.add(Restrictions.sqlRestriction("(minggu1 = " + minggu1 + " or minggu2 = "
												+ minggu2 + " or minggu3 = " + minggu3 + " or minggu4 = " + minggu4
												+ " or minggu5 = " + minggu5 + ")"))

										.add(semesterpendek == null ? Restrictions.isNull("statusSemesterPendek")
												: Restrictions.eq("statusSemesterPendek", semesterpendek))

										.add(Restrictions.eq("ganjilGenap", jenisSemester.toString()))
										.add(Restrictions.eq("tahunAjaran", tahunAjaran))

										.add(dosen == null
												? Restrictions.and(
														Restrictions.and(
																Restrictions.and(Restrictions.isNull("dosen1"),
																		Restrictions.eq("jurusan", jurusan)),
																Restrictions.eq("matakuliah", matakuliah)),
														(Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT)))
												: Restrictions.eq("dosen1", dosen))

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
												? Restrictions.and((Restrictions.and(
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
														: selesai == null ? Restrictions.and(
																Restrictions.and(Restrictions.isNull("waktuSelesai"),
																		Restrictions.eq("matakuliah", matakuliah)),
																Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT))
																: CommonScheduleValidationHelper.createOrJadwalMulaiSelesai(mulai, selesai)),
								Perkuliahan.class);
			} else {
				counts = ConstantValues
						.simpleList(
								session.createCriteria(Perkuliahan.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

										.add(CommonScheduleValidationHelper.getMulaiSampaiCriterion(perkuliahanDimulai, perkuliahanSampai))

										.add(Restrictions.sqlRestriction("(minggu1 = " + minggu1 + " or minggu2 = "
												+ minggu2 + " or minggu3 = " + minggu3 + " or minggu4 = " + minggu4
												+ " or minggu5 = " + minggu5 + ")"))

										.add(semesterpendek == null ? Restrictions.isNull("statusSemesterPendek")
												: Restrictions.eq("statusSemesterPendek", semesterpendek))

										.add(Restrictions.ne("id", id))
										.add(Restrictions.eq("ganjilGenap", jenisSemester.toString()))
										.add(Restrictions.eq("tahunAjaran", tahunAjaran))

										.add(dosen == null
												? Restrictions.and(
														Restrictions.and(
																Restrictions.and(Restrictions.isNull("dosen1"),
																		Restrictions.eq("jurusan", jurusan)),
																Restrictions.eq("matakuliah", matakuliah)),
														(Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT)))
												: Restrictions.eq("dosen1", dosen))

										.add(hari == null || hari.trim().equals("")
												? Restrictions.or(
														Restrictions.and(
																Restrictions.and(Restrictions.isNull("hari"),
																		Restrictions.eq("matakuliah", matakuliah)),
																Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT)),
														Restrictions.and(Restrictions.and(Restrictions.eq("hari", ""),
																Restrictions.eq("matakuliah", matakuliah)),
																Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT)))
												: Restrictions.eq("hari", hari))
										.add(mulai == null && selesai == null
												? Restrictions.and((Restrictions.and(
														Restrictions.and(Restrictions.isNull("waktuMulai"),
																Restrictions.eq("matakuliah", matakuliah)),
														Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT))),
														Restrictions.and(
																Restrictions.and(Restrictions.isNull("waktuSelesai"),
																		Restrictions.eq("matakuliah", matakuliah)),
																Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT)))
												: mulai == null
														? Restrictions.and(Restrictions.and(
																Restrictions.isNull("waktuMulai"),
																Restrictions.eq("matakuliah", matakuliah)),
																Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT))
														: selesai == null ? Restrictions.and(
																Restrictions.and(Restrictions.isNull("waktuSelesai"),
																		Restrictions.eq("matakuliah", matakuliah)),
																Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT))
																: CommonScheduleValidationHelper.createOrJadwalMulaiSelesai(mulai, selesai)),
								Perkuliahan.class);
			}
			System.out.println("checkJadwalDosen -> " + counts);
			if (counts != null) {
				for (Perkuliahan count : counts) {
					if (tampilWarning == null) {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, Bapak/Ibu. Terjadi bentrok jadwal di Jadwal Dosen Utama. Jadwal sudah terpakai di Program Studi {V1}, ruang {V2}, pada hari {V3} dari pukul {V4} sampai pukul {V5} oleh Dosen {V6}, matakuliah {V7}, kelas {V8} {V9}. Langkah yang dapat dilakukan: (1) periksa kembali jadwal dosen yang mengalami bentrok; (2) sesuaikan hari, jam, atau dosen pengampu; (3) simpan ulang jadwal setelah tidak ada bentrok.",
								"Peringatan bentrok di Jadwal Dosen Utama", 1, MyMessageboxConfig.EXCLAMATION,
								jurusan.getNama(), (count.getRuang() == null ? "" : count.getRuang().getNama()),
								count.getHari(), count.getWaktuMulai(), count.getWaktuSelesai(),
								(count.getDosen1() == null ? "" : count.getDosen1().getNama()),
								(count.getMatakuliah() == null ? "" : count.getMatakuliah().getNama()),
								count.getSemester(), count.getKelas());

					} else {
						String val = tampilWarning.getContent() + "<li><font color='red'>";
						val += "Peringatan bentrok di Jadwal Dosen Utama..<br>Jadwal sudah terpakai di Prodi "
								+ jurusan.getNama() + ", ruang '"
								+ (count.getRuang() == null ? "" : count.getRuang().getNama()) + "' dan hari '"
								+ count.getHari() + "' dari jam '" + count.getWaktuMulai() + "' sampai jam "
								+ count.getWaktuSelesai() + "' oleh Dosen "
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

	public static TemplatePerkuliahanDetail checkMatakuliahKesamaanBukanParalel(
				TemplatePerkuliahanDetail templatePerkuliahanDetail, TemplatePerkuliahan templatePerkuliahan,
				Jurusan jurusan, String kelas, Matakuliah matakuliah, Integer semester, String program, Html tampilWarning)
				throws Exception {

			if (kelas == null) {
				return null;
			}

			Session session = HibernateUtil.currentNativeSession();
			TemplatePerkuliahanDetail count = (TemplatePerkuliahanDetail) (session
					.createCriteria(TemplatePerkuliahanDetail.class)
					.add(Restrictions.or(Restrictions.eq("merupakan_paralel", false),
							Restrictions.isNull("merupakan_paralel")))
					.add(templatePerkuliahanDetail == null || templatePerkuliahanDetail.getId() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.ne("id", templatePerkuliahanDetail.getId()))

					.add(Restrictions.eq("templatePerkuliahan", templatePerkuliahan))
					.add(Restrictions.eq("program", program)).add(Restrictions.eq("jurusan", jurusan))
					.add(Restrictions.eq("semester", semester)).add(Restrictions.eq("matakuliah", matakuliah))
					.add(Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT)).setMaxResults(1).uniqueResult());

			HibernateUtil.closeSession();

			if (count != null) {
				if (count.getRuang() == null && count.getHari() == null && count.getWaktuMulai() == null
						&& count.getDosen1() == null) {
					return null;
				}
				if (tampilWarning == null) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, Bapak/Ibu. Jadwal Template Perkuliahan dengan matakuliah {V1} ({V2} SKS), semester {V3}, kelas {V4}, program {V5}, program studi {V6} sudah ada. Apabila Bapak/Ibu ingin membuat jadwal template perkuliahan paralel, mohon centang pilihan Paralel. Langkah yang dapat dilakukan: (1) periksa kembali jadwal template yang sudah ada; (2) centang pilihan Paralel bila memang menghendaki jadwal paralel; (3) simpan ulang jadwal.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, matakuliah.getNama(),
							matakuliah.getSks(), semester, kelas, program, jurusan.getNama());
				} else {
					String val = tampilWarning.getContent() + "<li><font color='red'>";
					val += "Jadwal Template Perkuliahan dengan matakuliah " + matakuliah.getNama() + " "
							+ matakuliah.getSks() + " SKS, semester " + semester + ", kelas " + kelas + ", program "
							+ program + ", prodi " + jurusan.getNama() + ". </font></li>";

					tampilWarning.setContent(val);
				}
				return count;
			}
			return count;
		}

	public static TemplatePerkuliahanDetail checkJadwalTemplateRuangPerkuliahanDetail(
				TemplatePerkuliahan templatePerkuliahan, Long id, Ruang ruang, String hari, Double mulai, Double selesai,
				String jenisSemester, Jurusan jurusan, Matakuliah matakuliah, String kelas, Html tampilWarning)
				throws Exception {
			Session session = HibernateUtil.currentNativeSession();
			TemplatePerkuliahanDetail count = null;

			if (id == null) {

				count = (TemplatePerkuliahanDetail) (session.createCriteria(TemplatePerkuliahanDetail.class)

						.add(jenisSemester.toString()
								.equalsIgnoreCase(Perkuliahan.GENAP) ? Restrictions.in("semester", Common.genap)
										: Restrictions.in("semester", Common.ganjil))
						.add(Restrictions.eq("templatePerkuliahan", templatePerkuliahan)).add(
								ruang == null
										? Restrictions.and(
												Restrictions.and(
														Restrictions.and(Restrictions.isNull("ruang"),
																Restrictions.eq("jurusan", jurusan)),
														Restrictions.eq("matakuliah", matakuliah)),
												(Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT)))
										: Restrictions.eq("ruang", ruang))
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
												: CommonScheduleValidationHelper.createOrJadwalMulaiSelesai(mulai, selesai))
						.setMaxResults(1).uniqueResult());

			} else {
				count = (TemplatePerkuliahanDetail) (session.createCriteria(TemplatePerkuliahanDetail.class)
						.add(Restrictions.ne("id", id))
						.add(jenisSemester.toString()
								.equalsIgnoreCase(Perkuliahan.GENAP) ? Restrictions.in("semester", Common.genap)
										: Restrictions.in("semester", Common.ganjil))
						.add(Restrictions.eq("templatePerkuliahan", templatePerkuliahan))
						.add(ruang == null
								? Restrictions.and(
										Restrictions.and(
												Restrictions.and(Restrictions.isNull("ruang"),
														Restrictions.eq("jurusan", jurusan)),
												Restrictions.eq("matakuliah", matakuliah)),
										(Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT)))
								: Restrictions.eq("ruang", ruang))
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
												: CommonScheduleValidationHelper.createOrJadwalMulaiSelesai(mulai, selesai))
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
							"Mohon maaf, Bapak/Ibu. Jadwal sudah terpakai di Program Studi {V1}, ruang {V2}, pada hari {V3} dari pukul {V4} sampai pukul {V5} oleh Dosen {V6}, matakuliah {V7}, kelas {V8} {V9}. Catatan: apabila waktu selesai suatu jadwal template perkuliahan sama dengan waktu mulai jadwal lain atau sebaliknya, mohon tambahkan 1 menit pada waktu mulai atau kurangkan 1 menit pada waktu selesai. Sebagai contoh, apabila waktu selesai suatu jadwal adalah pukul 09.10, maka waktu mulai jadwal berikutnya dibuat pukul 09.11. Langkah yang dapat dilakukan: (1) periksa kembali ruang dan waktu yang mengalami bentrok; (2) sesuaikan waktu mulai atau waktu selesai sesuai catatan di atas; (3) simpan ulang jadwal setelah tidak ada bentrok.",
							"Peringatan check Ruang Jadwal TemplatePerkuliahanDetail", 1, MyMessageboxConfig.EXCLAMATION,
							jurusan.getNama(), (count.getRuang() == null ? "" : count.getRuang().getNama()),
							count.getHari(), count.getWaktuMulai(), count.getWaktuSelesai(),
							(count.getDosen1() == null ? "" : count.getDosen1().getNama()),
							(count.getMatakuliah() == null ? "" : count.getMatakuliah().getNama()),
							count.getSemester(), count.getKelas());
				} else {
					String val = tampilWarning.getContent() + "<li><font color='red'>";
					val += "Jadwal sudah terpakai di Prodi " + jurusan.getNama() + ", ruang '"
							+ (count.getRuang() == null ? "" : count.getRuang().getNama()) + "' dan hari '"
							+ count.getHari() + "' dari jam '" + count.getWaktuMulai() + "' sampai jam "
							+ count.getWaktuSelesai() + "' oleh Dosen "
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

	public static TemplatePerkuliahanDetail checkJadwalDosen(TemplatePerkuliahan templatePerkuliahan, Long id,
				String hari, Double mulai, Double selesai, Dosen dosen, String jenisSemester, Jurusan jurusan,
				Matakuliah matakuliah, String kelas, Html tampilWarning) throws Exception {
			Session session = HibernateUtil.currentNativeSession();
			TemplatePerkuliahanDetail count = null;

			if (id == null) {
				count = ((TemplatePerkuliahanDetail) session.createCriteria(TemplatePerkuliahanDetail.class)
						.add(jenisSemester.toString().equalsIgnoreCase(Perkuliahan.GENAP)
								? Restrictions.in("semester", Common.genap)
								: Restrictions.in("semester", Common.ganjil))
						.add(Restrictions.eq("templatePerkuliahan", templatePerkuliahan))

						.add(dosen == null
								? Restrictions.and(
										Restrictions.and(
												Restrictions.and(Restrictions.isNull("dosen1"),
														Restrictions.eq("jurusan", jurusan)),
												Restrictions.eq("matakuliah", matakuliah)),
										(Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT)))
								: Restrictions.eq("dosen1", dosen))

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
												: CommonScheduleValidationHelper.createOrJadwalMulaiSelesai(mulai, selesai))
						.setMaxResults(1).uniqueResult());
			} else {
				count = ((TemplatePerkuliahanDetail) session.createCriteria(TemplatePerkuliahanDetail.class)
						.add(Restrictions.ne("id", id))
						.add(jenisSemester.toString().equalsIgnoreCase(Perkuliahan.GENAP)
								? Restrictions.in("semester", Common.genap)
								: Restrictions.in("semester", Common.ganjil))
						.add(Restrictions.eq("templatePerkuliahan", templatePerkuliahan))

						.add(dosen == null
								? Restrictions.and(Restrictions.isNull("dosen1"), Restrictions.eq("jurusan", jurusan))
								: Restrictions.eq("dosen1", dosen))

						.add(dosen == null
								? Restrictions.and(
										Restrictions.and(
												Restrictions.and(Restrictions.isNull("dosen1"),
														Restrictions.eq("jurusan", jurusan)),
												Restrictions.eq("matakuliah", matakuliah)),
										(Restrictions.ilike("kelas", kelas.trim(), MatchMode.EXACT)))
								: Restrictions.eq("dosen1", dosen))

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
												: CommonScheduleValidationHelper.createOrJadwalMulaiSelesai(mulai, selesai))
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
							"Mohon maaf, Bapak/Ibu. Jadwal sudah terpakai di Program Studi {V1}, ruang {V2}, pada hari {V3} dari pukul {V4} sampai pukul {V5} oleh Dosen {V6}, matakuliah {V7}, kelas {V8} {V9}. Catatan: apabila waktu selesai suatu jadwal template perkuliahan sama dengan waktu mulai jadwal lain atau sebaliknya, mohon tambahkan 1 menit pada waktu mulai atau kurangkan 1 menit pada waktu selesai. Sebagai contoh, apabila waktu selesai suatu jadwal adalah pukul 09.10, maka waktu mulai jadwal berikutnya dibuat pukul 09.11. Langkah yang dapat dilakukan: (1) periksa kembali ruang dan waktu yang mengalami bentrok; (2) sesuaikan waktu mulai atau waktu selesai sesuai catatan di atas; (3) simpan ulang jadwal setelah tidak ada bentrok.",
							"Peringatan check Jadwal Dosen", 1, MyMessageboxConfig.EXCLAMATION,
							jurusan.getNama(), (count.getRuang() == null ? "" : count.getRuang().getNama()),
							count.getHari(), count.getWaktuMulai(), count.getWaktuSelesai(),
							(count.getDosen1() == null ? "" : count.getDosen1().getNama()),
							(count.getMatakuliah() == null ? "" : count.getMatakuliah().getNama()),
							count.getSemester(), count.getKelas());
				} else {
					String val = tampilWarning.getContent() + "<li><font color='red'>";
					val += "Jadwal sudah terpakai di Prodi " + jurusan.getNama() + ", ruang '"
							+ (count.getRuang() == null ? "" : count.getRuang().getNama()) + "' dan hari '"
							+ count.getHari() + "' dari jam '" + count.getWaktuMulai() + "' sampai jam "
							+ count.getWaktuSelesai() + "' oleh Dosen "
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
}
