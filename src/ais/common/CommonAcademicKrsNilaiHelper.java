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
 * Helper akademik untuk KRS, semester, tahapan, reload nilai, nilai huruf, judisium, dan data MK KRS.
 * Dipisahkan dari Common agar Common tetap ringan sebagai facade/wrapper.
 */
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class CommonAcademicKrsNilaiHelper extends Common {

	private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CommonAcademicKrsNilaiHelper.class);

	private CommonAcademicKrsNilaiHelper() {
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
			MyMessageboxConfig.show(pesan + detail);
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonAcademicKrsNilaiHelper.java:410");
		}
	}

	public static List<Long> getIkutDetailperkuliahans(Mahasiswa mahasiswa, Integer semester, final Integer tahapan,
				Integer persetujuan, Integer semesterPendek, Boolean hitungSemua) {

			Criterion criterionSemester = tahapan == null || tahapan.equals(0) ? Restrictions.eq("semester", semester)
					: Restrictions.sqlRestriction("true");

			Criterion criterionTahapan = tahapan.equals(0) ? Restrictions.sqlRestriction("true")
					: Restrictions.eq("tahap", tahapan);

			List<Long> detailperkuliahans = HibernateUtil.currentSession().createCriteria(Detailperkuliahan.class)
					.setProjection(Projections.property("id")).add(Restrictions.isNotNull("ikutiPerkuliahan"))
					.add(semester == null ? Restrictions.sqlRestriction("1=1") : criterionSemester)
					.add(Restrictions.eq("mahasiswa", mahasiswa))

					.addOrder(Order.desc("id"))
					.add(persetujuan == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("persetujuan", persetujuan))
					.createAlias("ikutiPerkuliahan", "ikutiPerkuliahan", Criteria.LEFT_JOIN)
					.createAlias("perkuliahan.kurikulumPunyaMatakuliah", "kurikulumPunyaMatakuliah", Criteria.LEFT_JOIN)

					.add(criterionTahapan)

					.add(hitungSemua ? Restrictions.sqlRestriction("1=1")
							: semesterPendek == null ? Restrictions.isNull("ikutiPerkuliahan.statusSemesterPendek")
									: Restrictions.eq("ikutiPerkuliahan.statusSemesterPendek", semesterPendek))

					.list();

			return detailperkuliahans;
		}

	public static List<Long> saringMatakuliahyangPalingBesarNilainya(List<Long> detailperkuliahans, String nim) {
			Map<String, Long> myDetailperkuliahans = new HashMap<String, Long>();
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan == null
						|| (detailperkuliahan.getMatakuliahKonversi() == null && detailperkuliahan.getPerkuliahan() == null)
								&& detailperkuliahan.getPerkuliahan().getMatakuliah() == null) {
					continue;
				}
				Matakuliah matakuliah = detailperkuliahan.getMatakuliahKonversi() == null
						? detailperkuliahan.getPerkuliahan().getMatakuliah()
						: detailperkuliahan.getMatakuliahKonversi();
				matakuliah = Common.getMatakuliahApakahEkivalen(matakuliah, nim, false)[0];

				Long detailperkuliahanYangSudahAdaid = myDetailperkuliahans.get(matakuliah.getKode().toUpperCase());
				if (detailperkuliahanYangSudahAdaid != null) {
					Detailperkuliahan detailperkuliahanYangSudahAda = (Detailperkuliahan) GeneralValueObject
							.ambilData(Detailperkuliahan.class, detailperkuliahanYangSudahAdaid.toString());
					if (detailperkuliahanYangSudahAda != null && detailperkuliahanYangSudahAda.getTotalNilai() != null
							&& detailperkuliahan.getTotalNilai() != null) {
						if (detailperkuliahanYangSudahAda.getTotalNilai() < detailperkuliahan.getTotalNilai()) {
							myDetailperkuliahans.put(matakuliah.getKode().toUpperCase(), detailperkuliahan.getId());
						}
					} else {
						myDetailperkuliahans.put(matakuliah.getKode().toUpperCase(), detailperkuliahan.getId());
					}
				} else {
					myDetailperkuliahans.put(matakuliah.getKode().toUpperCase(), detailperkuliahan.getId());
				}
			}
			detailperkuliahans = null;

			List<Long> myDetailperkuliahans2 = new ArrayList<Long>();
			myDetailperkuliahans2.addAll(myDetailperkuliahans.values());
			return myDetailperkuliahans2;
		}

	public static void reloadNilaiCurrentNilai(Mahasiswa mahasiswa, final Boolean reload) {
			final Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
			String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

			int smt = Common.getSemester(tahunAngkatanMhs, semesterMulai, mahasiswa.getPindahKeKampusIniMasukSemester(),
					mahasiswa.getSemesterMulai()) - 1;
			reloadNilai(mahasiswa, smt, reload);
		}

	public static void reloadNilai(final Mahasiswa mahasiswa, final Integer semester, final Boolean reload) {

			if (Common.bolehKonfigurasi("nilai_mahasiswa_otomatis_terkoreksi", Konfigurasi.TIDAK_AKTIF)) {

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						List<Long> detailperkuliahans = Common.getDetailperkuliahans(mahasiswa, semester,
								Detailperkuliahan.DISETUJUI, null, false, false, true, reload);

						Session session = HibernateUtil.currentSession();
						for (Long detailperkuliahanid : detailperkuliahans) {
							Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
									.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
							if (detailperkuliahan != null) {
								Boolean sembunyikanNilaiJikaBelumDiverifikasi = false;
								if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null) {
									sembunyikanNilaiJikaBelumDiverifikasi = detailperkuliahan.getPerkuliahan()
											.getSembunyikanNilaiJikaBelumDiverifikasi();
								}
								List<FormatNilai> formatNilais = detailperkuliahan.getPerkuliahan() == null ? null
										: detailperkuliahan.getPerkuliahan().ambilFormatNilai(session);
								detailperkuliahan.reloadFormatNilai(formatNilais, sembunyikanNilaiJikaBelumDiverifikasi);
								Double totalNilai = detailperkuliahan.hitungTotalNilai(true, formatNilais);
								if (totalNilai != null && totalNilai > 0.1) {

									Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() != null
											? detailperkuliahan.getPerkuliahan().getMatakuliah()
											: detailperkuliahan.getMatakuliahKonversi();

									NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(totalNilai,
											detailperkuliahan.getMahasiswa().getTahunangkatan(),
											detailperkuliahan.getMahasiswa().getJurusan(),
											detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
											detailperkuliahan.getTahunAkademik(),
											detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
													: Perkuliahan.GANJIL,
											matakuliah == null ? "" : matakuliah.getKode(),
											matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
									String nh = nilaiHuruf == null ? "-" : nilaiHuruf.getNilaiHuruf();
									Double nil = nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK();

									if ((totalNilai != null
											&& totalNilai.intValue() != detailperkuliahan.getTotalNilai().intValue())
											|| !nh.equalsIgnoreCase(detailperkuliahan.getNilaiHuruf())
											|| !Common.numberFormat.get().format(nil).equalsIgnoreCase(
													Common.numberFormat.get().format(detailperkuliahan.getTotalIP()))) {
										// System.out.println("==updatelah-> " +
										// detailperkuliahan + ", " + nh + ", " +
										// nil);
										detailperkuliahan.setTotalNilai(totalNilai);
										detailperkuliahan.setNilaiHuruf(nh);
										detailperkuliahan.setTotalIP(nil);
										detailperkuliahan.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());

										Double totalSementara = totalNilai;
										nilaiHuruf = Common.getNilaiHuruf(totalSementara,
												detailperkuliahan.getMahasiswa().getTahunangkatan(),
												detailperkuliahan.getMahasiswa().getJurusan(),
												detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
												detailperkuliahan.getTahunAkademik(),
												detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
														: Perkuliahan.GANJIL,
												matakuliah == null ? "" : matakuliah.getKode(),
												matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

										detailperkuliahan.setTotalNilaiSementara(totalSementara);
										detailperkuliahan.setNilaiHurufSementara(
												nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
										detailperkuliahan
												.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

										Common.refreshSaveOrUpdate(detailperkuliahan);
									}
								}
							}
						}
					}
				}, "Sedang menyiapkan data..");
			}
		}

	public static Integer getSemester(Integer tahunAngkatanMhs, String tahunAkademik, String jenisSemester,
				Integer mulaiSemester, String masukDiSemester) {
			if (tahunAkademik == null) {
				tahunAkademik = getCurrentTahunAkademik();
			}
			// Menggunakan split bawaan Java agar hemat memori tanpa dependensi eksternal
			Integer tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
			return getSemester(tahunAngkatanMhs, jenisSemester, mulaiSemester, tahun, masukDiSemester);
		}

	public static Integer getSemester(Integer tahunAngkatanMhs, String jenisSemester, Integer mulaiSemester,
				String masukDiSemester) {
			String ta = getCurrentTahunAkademik();
			Integer tahun = Integer.parseInt(ta.split("/")[0]);
			return getSemester(tahunAngkatanMhs, jenisSemester, mulaiSemester, tahun, masukDiSemester);
		}

	public static Integer getSemester(Integer tahunAngkatanMhs, String jenisSemester, Integer mulaiSemester,
				Integer tahun, String masukDiSemester) {

			if (tahunAngkatanMhs == null) {
				return 0;
			}

			// 1. Normalisasi parameter untuk mencegah NullPointerException
			if (jenisSemester == null)
				jenisSemester = Perkuliahan.GANJIL;
			if (masukDiSemester == null)
				masukDiSemester = Perkuliahan.GANJIL;
			if (mulaiSemester == null || mulaiSemester <= 0)
				mulaiSemester = 1;

			// 2. Konversi Ganjil/Genap menjadi koefisien angka (Ganjil = 0, Genap = 1)
			int valCurrentSeason = jenisSemester.equalsIgnoreCase(Perkuliahan.GENAP) ? 1 : 0;
			int valStartSeason = masukDiSemester.equalsIgnoreCase(Perkuliahan.GENAP) ? 1 : 0;

			// 3. Menghitung jumlah semester absolut sejak "tahun 0"
			// Rumus: (Tahun * 2) + Musim Semester
			int absoluteCurrent = (tahun * 2) + valCurrentSeason;
			int absoluteStart = (tahunAngkatanMhs * 2) + valStartSeason;

			// 4. Hitung selisih (berapa semester yang sudah berlalu)
			int elapsedSemesters = absoluteCurrent - absoluteStart;

			// 5. Semester saat ini = Semester awal (mulaiSemester) + Semester yang berlalu
			int currentSemesterNumber = mulaiSemester + elapsedSemesters;

			// 6. Return (handle batas bawah agar tidak pernah minus jika tahun salah input)
			return currentSemesterNumber > 0 ? currentSemesterNumber : 0;
		}

	public static Integer getTahapan(Integer tahunAngkatanMhs, String jenisSemester, Integer mulaiSemester,
				Integer tahun, String masukDiSemester) {
			if (tahunAngkatanMhs == null) {
				return 0;
			}

			if (jenisSemester == null) {
				jenisSemester = Perkuliahan.GANJIL;
			}

			if (masukDiSemester.equalsIgnoreCase(Perkuliahan.GANJIL)) {
				Integer mysemester = jenisSemester.equalsIgnoreCase(Perkuliahan.GENAP)
						? (((tahun + 1) - tahunAngkatanMhs) * 2)
						: (((tahun - tahunAngkatanMhs) * 2) + 1);
				return mysemester + mulaiSemester;
			} else {
				Integer mysemester = jenisSemester.equalsIgnoreCase(Perkuliahan.GANJIL) ? (((tahun) - tahunAngkatanMhs) * 2)
						: (((tahun - tahunAngkatanMhs) * 2) + 1);
				return mysemester + mulaiSemester;
			}
		}

	public static void synNilaiHuruf(Label label, boolean hanyaYangBelumdapatNilai) {
			Session session = HibernateUtil.currentNativeSession();
			int c = ((Number) session.createCriteria(NilaiHuruf.class).add(Restrictions.isNull("ta"))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (c > 0) {
				List<NilaiHuruf> nilaiHurufs = session.createCriteria(NilaiHuruf.class).add(Restrictions.isNull("ta"))
						.list();
				session.getTransaction().begin();
				for (NilaiHuruf huruf : nilaiHurufs) {
					session.update(huruf);
				}
				session.getTransaction().commit();
			}
			ProjectionList projectionList = Projections.projectionList();
			projectionList.add(Projections.property("totalNilai"));
			projectionList.add(Projections.property("mahasiswa.id"));
			projectionList.add(Projections.property("totalIP"));
			projectionList.add(Projections.property("id"));
			projectionList.add(Projections.property("tahunAkademik"));
			projectionList.add(Projections.property("semester"));
			List<Object[]> objects = session.createCriteria(Detailperkuliahan.class).add(Restrictions.gt("totalNilai", 2.0))
					.add(hanyaYangBelumdapatNilai
							? Restrictions.or(Restrictions.eq("nilaiHuruf", ""), Restrictions.isNull("nilaiHuruf"))
							: Restrictions.sqlRestriction("true"))
					.setProjection(projectionList).list();

			HibernateUtil.closeSession();

			// System.out.println("objects = " + objects.size());
			int size = objects.size();
			int i = 0;
			for (Object[] o : objects) {
				i++;
				try {
					Double nilai = (Double) o[0];
					Long mhsid = (Long) o[1];
					if (mhsid != null) {
						Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), mhsid);
						Jurusan jurusan = mahasiswa.getJurusan();
						Integer tahunAngkatan = mahasiswa.getTahunangkatan();
						Long id = (Long) o[3];

						Integer smt = (Integer) o[5];
						String tahunAkademik = (String) o[4];
						String semester = smt == null ? null : (smt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);

						if (nilai != null && jurusan != null && tahunAngkatan != null && id != null) {

							session = HibernateUtil.currentNativeSession();
							Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
									.createCriteria(Detailperkuliahan.class).add(Restrictions.idEq(id)).uniqueResult();
							if (detailperkuliahan != null) {
								Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() != null
										? detailperkuliahan.getPerkuliahan().getMatakuliah()
										: detailperkuliahan.getMatakuliahKonversi();
								NilaiHuruf nilaiHuruf = getNilaiHuruf(nilai, tahunAngkatan, jurusan, jurusan.getFakultas(),
										tahunAkademik, semester, matakuliah == null ? "" : matakuliah.getKode(),
										matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
								if (nilaiHuruf != null) {
									detailperkuliahan.setNilaiHuruf(nilaiHuruf.getNilaiHuruf());
									detailperkuliahan.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());
									detailperkuliahan.setTotalIP(nilaiHuruf.getNilaiDiIPK());
									session.getTransaction().begin();
									Common.refreshSaveOrUpdate(session, detailperkuliahan);
									session.getTransaction().commit();
								}

								if (label != null) {
									label.setValue(Common.numberFormat.get().format((i * 100.0 / size))
											+ "% .. Singkronisasi " + detailperkuliahan.getMahasiswa() + " nilai = " + nilai
											+ ", jurusan = " + jurusan + ", tahunAngkatan = " + tahunAngkatan + ", id = "
											+ id + ", nilaiHuruf = " + nilaiHuruf.getNilaiHuruf() + ", IP = "
											+ nilaiHuruf.getNilaiDiIPK());
								}
							}

							// session.disconnect();
							if (session.isOpen()) {
								session.disconnect();
								session.close();
							}
							HibernateUtil.closeSession();
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (label != null)

			{
				label.setValue("");
			}
		}

	public static NilaiHuruf getNilaiHuruf(Double nilai, Integer tahunAngkatan, Jurusan jurusan, Fakultas fakultas,
				String tahunAkademik, String semester, String kodemk, JenisNilaiHurufMatakuliah jenisNilaiHuruf) {
			return getNilaiHuruf(nilai, tahunAngkatan, jurusan, fakultas, tahunAkademik, semester, kodemk, true, false,
					jenisNilaiHuruf);
		}

	private static boolean cocokKodeMkNilaiHuruf(NilaiHuruf nilaiHuruf, String kodemk) {
			if (nilaiHuruf == null) {
				return false;
			}
			String kodeDicari = kodemk == null ? "" : kodemk.trim().toLowerCase();
			String kodeSetup = nilaiHuruf.getKodeMk() == null ? "" : nilaiHuruf.getKodeMk().trim().toLowerCase();
			if (kodeDicari.isEmpty()) {
				return kodeSetup.isEmpty();
			}
			if (kodeSetup.isEmpty()) {
				return true;
			}
			String daftarKode = "," + kodeSetup.replace(" ", "") + ",";
			String kode = kodeDicari.replace(" ", "");
			return daftarKode.contains("," + kode + ",") || kodeSetup.equals(kodeDicari);
		}

	public static NilaiHuruf getNilaiHuruf(Double nilai, Integer tahunAngkatan, Jurusan jurusan, Fakultas fakultas,
				String tahunAkademik, String semester, String kodemk, boolean coba, boolean semuaKodeMk,
				JenisNilaiHurufMatakuliah jenisNilaiHuruf) {
			nilai = nilai == null ? 0.0 : nilai;
			// // System.out.println("Hitung sebelum nilai " + nilai + "");
			try {
				nilai = nilai == null ? 0.0
						: Common.numberFormat.get().parse(Common.numberFormat.get().format(nilai)).doubleValue();
			} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/common/CommonAcademicKrsNilaiHelper.java:759");
				// TODO Auto-generated catch block
				// e1.printStackTrace();
			}

			String id_smt = (tahunAkademik == null || tahunAkademik.trim().isEmpty() ? "0" : tahunAkademik.split("/")[0])
					+ (semester == null || semester.trim().isEmpty() ? "0"
							: semester.equals(Perkuliahan.GENAP) ? "2" : "1");
			Integer ta = 0;
			try {
				ta = Integer.parseInt(id_smt.trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonAcademicKrsNilaiHelper.java:770");

			}

			NilaiHuruf huruf = null;
			try {

				for (NilaiHuruf nilaiHuruf : ConstantValues.nilaiHurufs) {
					try {

						if (nilaiHuruf != null
								&& (nilaiHuruf.getTa() == null || nilaiHuruf.getTa() <= ta || tahunAkademik == null)
								&& nilaiHuruf.getJurusan() != null && jurusan != null
								&& nilaiHuruf.getJurusan().getId().equals(jurusan.getId())

								&& ((nilaiHuruf.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
										|| (nilaiHuruf.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
												&& jenisNilaiHuruf.getId() != null
												&& nilaiHuruf.getJenisNilaiHuruf().getId().equals(jenisNilaiHuruf.getId())))

								&& nilaiHuruf.getFakultas() != null && fakultas != null
								&& nilaiHuruf.getFakultas().getId().equals(fakultas.getId())

								&& nilai >= nilaiHuruf.getMulai()

								&& nilai <= nilaiHuruf.getSampai()

								&& tahunAngkatan.equals(nilaiHuruf.getTahunAngkatan())

								&& cocokKodeMkNilaiHuruf(nilaiHuruf, kodemk)) {
							huruf = nilaiHuruf;
							// System.out.println("Step 1 : nilai huruf " +
							// huruf.toString());
							break;
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (huruf == null) {
					for (NilaiHuruf nilaiHuruf : ConstantValues.nilaiHurufs) {
						try {
							if (nilaiHuruf != null
									&& (nilaiHuruf.getTa() == null || nilaiHuruf.getTa() <= ta || tahunAkademik == null)
									&& nilaiHuruf.getJurusan() != null && jurusan != null
									&& nilaiHuruf.getJurusan().getId().equals(jurusan.getId())

									&& ((nilaiHuruf.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
											|| (nilaiHuruf.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
													&& jenisNilaiHuruf.getId() != null
													&& nilaiHuruf.getJenisNilaiHuruf().getId()
															.equals(jenisNilaiHuruf.getId())))

									&& nilaiHuruf.getFakultas() != null && fakultas != null
									&& nilaiHuruf.getFakultas().getId().equals(fakultas.getId())

									&& nilai >= nilaiHuruf.getMulai()

									&& nilai <= nilaiHuruf.getSampai()

									&& tahunAngkatan >= nilaiHuruf.getTahunAngkatan()

									&& cocokKodeMkNilaiHuruf(nilaiHuruf, kodemk)) {
								huruf = nilaiHuruf;
								// System.out.println("Step 1.1 : nilai huruf " +
								// huruf.toString());
								break;
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				}

				if (huruf == null) {
					for (NilaiHuruf nilaiHuruf : ConstantValues.nilaiHurufs) {
						try {
							if (nilaiHuruf != null
									&& (nilaiHuruf.getTa() == null || nilaiHuruf.getTa() <= ta || tahunAkademik == null)

									&& nilaiHuruf.getJurusan() == null

									&& ((nilaiHuruf.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
											|| (nilaiHuruf.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
													&& jenisNilaiHuruf.getId() != null
													&& nilaiHuruf.getJenisNilaiHuruf().getId()
															.equals(jenisNilaiHuruf.getId())))

									&& nilaiHuruf.getFakultas() != null && fakultas != null
									&& nilaiHuruf.getFakultas().getId().equals(fakultas.getId())

									&& nilai >= nilaiHuruf.getMulai()

									&& nilai <= nilaiHuruf.getSampai()

									&& tahunAngkatan.equals(nilaiHuruf.getTahunAngkatan())

									&& cocokKodeMkNilaiHuruf(nilaiHuruf, kodemk)) {
								huruf = nilaiHuruf;
								// System.out.println("Step 2 : nilai huruf " +
								// huruf.toString());
								break;
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}

				}

				if (huruf == null) {
					for (NilaiHuruf nilaiHuruf : ConstantValues.nilaiHurufs) {
						try {
							if (nilaiHuruf != null
									&& (nilaiHuruf.getTa() == null || nilaiHuruf.getTa() <= ta || tahunAkademik == null)

									&& nilaiHuruf.getJurusan() == null

									&& ((nilaiHuruf.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
											|| (nilaiHuruf.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
													&& jenisNilaiHuruf.getId() != null
													&& nilaiHuruf.getJenisNilaiHuruf().getId()
															.equals(jenisNilaiHuruf.getId())))

									&& nilaiHuruf.getFakultas() != null && fakultas != null
									&& nilaiHuruf.getFakultas().getId().equals(fakultas.getId())

									&& nilai >= nilaiHuruf.getMulai()

									&& nilai <= nilaiHuruf.getSampai()

									&& tahunAngkatan >= nilaiHuruf.getTahunAngkatan()

									&& cocokKodeMkNilaiHuruf(nilaiHuruf, kodemk)) {
								huruf = nilaiHuruf;
								// System.out.println("Step 2.1 : nilai huruf " +
								// huruf.toString());
								break;
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}

				}

				if (huruf == null) {
					for (NilaiHuruf nilaiHuruf : ConstantValues.nilaiHurufs) {
						try {
							if (nilaiHuruf != null
									&& (nilaiHuruf.getTa() == null || nilaiHuruf.getTa() <= ta || tahunAkademik == null)

									&& ((nilaiHuruf.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
											|| (nilaiHuruf.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
													&& jenisNilaiHuruf.getId() != null
													&& nilaiHuruf.getJenisNilaiHuruf().getId()
															.equals(jenisNilaiHuruf.getId())))

									&& nilaiHuruf.getJurusan() == null

									&& nilaiHuruf.getFakultas() == null

									&& nilai >= nilaiHuruf.getMulai()

									&& nilai <= nilaiHuruf.getSampai()

									&& tahunAngkatan.equals(nilaiHuruf.getTahunAngkatan())

									&& cocokKodeMkNilaiHuruf(nilaiHuruf, kodemk)) {
								huruf = nilaiHuruf;
								// System.out.println("Step 3 : nilai huruf " +
								// huruf.toString());
								break;
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}

				}

				if (huruf == null) {
					for (NilaiHuruf nilaiHuruf : ConstantValues.nilaiHurufs) {
						try {
							if (nilaiHuruf != null
									&& (nilaiHuruf.getTa() == null || nilaiHuruf.getTa() <= ta || tahunAkademik == null)

									&& ((nilaiHuruf.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
											|| (nilaiHuruf.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
													&& jenisNilaiHuruf.getId() != null
													&& nilaiHuruf.getJenisNilaiHuruf().getId()
															.equals(jenisNilaiHuruf.getId())))

									&& nilaiHuruf.getJurusan() == null

									&& nilaiHuruf.getFakultas() == null

									&& nilai >= nilaiHuruf.getMulai()

									&& nilai <= nilaiHuruf.getSampai()

									&& tahunAngkatan >= nilaiHuruf.getTahunAngkatan()

									&& cocokKodeMkNilaiHuruf(nilaiHuruf, kodemk)) {
								huruf = nilaiHuruf;
								// System.out.println("Step 3.1 : nilai huruf " +
								// huruf.toString());
								break;
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}

					}
				}

				if (huruf == null) {
					for (NilaiHuruf nilaiHuruf : ConstantValues.nilaiHurufs) {
						try {
							if (nilaiHuruf != null
									&& (nilaiHuruf.getTa() == null || nilaiHuruf.getTa() <= ta || tahunAkademik == null)

									&& ((nilaiHuruf.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
											|| (nilaiHuruf.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
													&& jenisNilaiHuruf.getId() != null
													&& nilaiHuruf.getJenisNilaiHuruf().getId()
															.equals(jenisNilaiHuruf.getId())))

									&& nilaiHuruf.getJurusan() == null

									&& nilaiHuruf.getFakultas() == null

									&& nilai >= nilaiHuruf.getMulai()

									&& nilai <= nilaiHuruf.getSampai()

									&& cocokKodeMkNilaiHuruf(nilaiHuruf, kodemk)) {
								huruf = nilaiHuruf;
								// System.out.println("Step 4 : nilai huruf " +
								// huruf.toString());
								break;
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}

				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			if (huruf == null && jenisNilaiHuruf != null) {
				huruf = getNilaiHuruf(nilai, tahunAngkatan, jurusan, fakultas, tahunAkademik, semester, kodemk, false,
						semuaKodeMk, null);
			}

			if (huruf == null & !semuaKodeMk) {
				huruf = getNilaiHuruf(nilai, tahunAngkatan, jurusan, fakultas, tahunAkademik, semester, kodemk, coba, true,
						jenisNilaiHuruf);
			}

			if (huruf == null && coba) {

				Session session = HibernateUtil.currentNativeSession();
				ConstantValues.realoadNilaiHuruf(session);
				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
				HibernateUtil.closeSession();
				huruf = getNilaiHuruf(nilai, tahunAngkatan, jurusan, fakultas, tahunAkademik, semester, kodemk, false, true,
						jenisNilaiHuruf);
			}

			return huruf;
		}

	public static NilaiHuruf getNilaiHurufBerdasarkanIP(Double ip, Integer tahunAngkatan, Jurusan jurusan,
				Fakultas fakultas) {
			ip = ip == null ? 0.0 : ip;
			// System.out.println("Hitung sebelum ip " + ip + "");
			try {
				ip = ip == null ? 0.0 : Common.numberFormat.get().parse(Common.numberFormat.get().format(ip)).doubleValue();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/common/CommonAcademicKrsNilaiHelper.java:1061");
			}
			// System.out.println("Hitung setelah ip " + ip + "");
			Session session = HibernateUtil.currentSession();
			NilaiHuruf huruf = null;
			try {

				huruf = (NilaiHuruf) session.createCriteria(NilaiHuruf.class).add(Restrictions.eq("jurusan", jurusan))
						.add(Restrictions.eq("fakultas", fakultas)).add(Restrictions.le("nilaiDiIPK", ip))
						.add(Restrictions.le("tahunAngkatan", tahunAngkatan)).addOrder(Order.desc("nilaiDiIPK"))
						.addOrder(Order.desc("tahunAngkatan")).setMaxResults(1).uniqueResult();

				if (huruf == null) {
					huruf = (NilaiHuruf) session.createCriteria(NilaiHuruf.class).add(Restrictions.eq("fakultas", fakultas))
							.add(Restrictions.isNull("jurusan")).add(Restrictions.le("nilaiDiIPK", ip))
							.add(Restrictions.le("tahunAngkatan", tahunAngkatan)).addOrder(Order.desc("nilaiDiIPK"))
							.addOrder(Order.desc("tahunAngkatan")).setMaxResults(1).uniqueResult();
				}
				if (huruf == null) {
					huruf = (NilaiHuruf) session.createCriteria(NilaiHuruf.class).add(Restrictions.isNull("jurusan"))
							.add(Restrictions.isNull("fakultas")).add(Restrictions.le("nilaiDiIPK", ip))
							.add(Restrictions.le("tahunAngkatan", tahunAngkatan)).addOrder(Order.desc("nilaiDiIPK"))
							.addOrder(Order.desc("tahunAngkatan")).setMaxResults(1).uniqueResult();
				}

				if (huruf == null) {
					huruf = (NilaiHuruf) session.createCriteria(NilaiHuruf.class).add(Restrictions.isNull("jurusan"))
							.add(Restrictions.isNull("fakultas")).add(Restrictions.le("nilaiDiIPK", ip))
							.addOrder(Order.desc("nilaiDiIPK")).addOrder(Order.desc("tahunAngkatan")).setMaxResults(1)
							.uniqueResult();
				}

				// //---------------------------///

				if (huruf == null) {
					huruf = (NilaiHuruf) session.createCriteria(NilaiHuruf.class).add(Restrictions.eq("jurusan", jurusan))
							.add(Restrictions.eq("fakultas", fakultas)).add(Restrictions.le("nilaiDiIPK", ip))
							.add(Restrictions.le("tahunAngkatan", tahunAngkatan)).addOrder(Order.desc("nilaiDiIPK"))
							.addOrder(Order.desc("tahunAngkatan")).setMaxResults(1).uniqueResult();
				}

				if (huruf == null) {
					huruf = (NilaiHuruf) session.createCriteria(NilaiHuruf.class).add(Restrictions.eq("fakultas", fakultas))
							.add(Restrictions.isNull("jurusan")).add(Restrictions.le("nilaiDiIPK", ip))
							.add(Restrictions.le("tahunAngkatan", tahunAngkatan)).addOrder(Order.desc("nilaiDiIPK"))
							.addOrder(Order.desc("tahunAngkatan")).setMaxResults(1).uniqueResult();
				}
				if (huruf == null) {
					huruf = (NilaiHuruf) session.createCriteria(NilaiHuruf.class).add(Restrictions.isNull("jurusan"))
							.add(Restrictions.isNull("fakultas")).add(Restrictions.le("nilaiDiIPK", ip))
							.add(Restrictions.le("tahunAngkatan", tahunAngkatan)).addOrder(Order.desc("nilaiDiIPK"))
							.addOrder(Order.desc("tahunAngkatan")).setMaxResults(1).uniqueResult();
				}

				if (huruf == null) {
					huruf = (NilaiHuruf) session.createCriteria(NilaiHuruf.class).add(Restrictions.isNull("jurusan"))
							.add(Restrictions.isNull("fakultas")).add(Restrictions.le("nilaiDiIPK", ip))
							.addOrder(Order.desc("nilaiDiIPK")).addOrder(Order.desc("tahunAngkatan")).setMaxResults(1)
							.uniqueResult();
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			return huruf;
		}

	public static Judisium hitungJudisium(Mahasiswa mahasiswa, KrsMahasiswa krsMahasiswa) {
			return hitungJudisium(mahasiswa, null, krsMahasiswa);
		}

	public static Judisium hitungJudisium(Mahasiswa mahasiswa, Integer smt, KrsMahasiswa krsMahasiswa) {

			if (mahasiswa.getPredikatKelulusan() != null) {
				return mahasiswa.getPredikatKelulusan();
			}

			int maxSmt = 0;
			List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan();
			Set<String> lowerCase = new HashSet<String>();
			for (Long detailperkuliahanid : saringMatakuliahyangPalingBesarNilainya(detailperkuliahans, null)) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (!detailperkuliahan.getNilaiHuruf().trim().isEmpty()) {
						lowerCase.add(detailperkuliahan.getNilaiHuruf().trim().toLowerCase());
						if (maxSmt < detailperkuliahan.getSemester()) {
							maxSmt = detailperkuliahan.getSemester();
						}
					}
				}
			}

			if (krsMahasiswa == null) {
				krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, smt == null ? maxSmt : smt, null, null);
			}
			Double ipk = smt == null ? krsMahasiswa.getIpk() : krsMahasiswa.getIps();

			Integer sks = smt == null ? krsMahasiswa.getSksk() : krsMahasiswa.getSksYangDiambil();
			Session session = HibernateUtil.currentSession();

			List<Judisium> judisiums = ConstantValues
					.simpleList(
							session.createCriteria(Judisium.class).add(Restrictions.eq("jenjang", mahasiswa.getJenjang()))

									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.or(Restrictions.isNull("minimalIpkYangTelahDitempuh"),
											Restrictions.le("minimalIpkYangTelahDitempuh", ipk)))
									.add(Restrictions.or(Restrictions.isNull("minimalSksYangTelahDitempuh"),
											Restrictions.le("minimalSksYangTelahDitempuh", sks)))

									.add(Restrictions.eq("statusAwalMahasiswa", mahasiswa.getStatusAwalMahasiswa()))
									.add(Restrictions.ge("masaStudiMaksimal", maxSmt))
									.add(Restrictions.sqlRestriction(ipk + " between nilai_mulai and nilai_sampai"))
									.addOrder(Order.asc("masaStudiMaksimal")).addOrder(Order.desc("nilaiMulai")),
							Judisium.class);

			if (judisiums.isEmpty()) {
				judisiums = ConstantValues.simpleList(
						session.createCriteria(Judisium.class).add(Restrictions.isNull("jenjang"))

								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.or(Restrictions.isNull("minimalIpkYangTelahDitempuh"),
										Restrictions.le("minimalIpkYangTelahDitempuh", ipk)))
								.add(Restrictions.or(Restrictions.isNull("minimalSksYangTelahDitempuh"),
										Restrictions.le("minimalSksYangTelahDitempuh", sks)))

								.add(Restrictions.eq("statusAwalMahasiswa", mahasiswa.getStatusAwalMahasiswa()))
								.add(Restrictions.ge("masaStudiMaksimal", maxSmt))
								.add(Restrictions.sqlRestriction(ipk + " between nilai_mulai and nilai_sampai"))
								.addOrder(Order.asc("masaStudiMaksimal")).addOrder(Order.desc("nilaiMulai")),
						Judisium.class);
			}

			if (judisiums.isEmpty()) {
				judisiums = ConstantValues.simpleList(
						session.createCriteria(Judisium.class).add(Restrictions.eq("jenjang", mahasiswa.getJenjang()))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.isNull("statusAwalMahasiswa"))
								.add(Restrictions.ge("masaStudiMaksimal", maxSmt))
								.add(Restrictions.sqlRestriction(ipk + " between nilai_mulai and nilai_sampai"))
								.addOrder(Order.asc("masaStudiMaksimal")).addOrder(Order.desc("nilaiMulai")),
						Judisium.class);
			}

			if (judisiums.isEmpty()) {
				judisiums = ConstantValues.simpleList(
						session.createCriteria(Judisium.class).add(Restrictions.isNull("jenjang"))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.isNull("statusAwalMahasiswa"))
								.add(Restrictions.ge("masaStudiMaksimal", maxSmt))
								.add(Restrictions.sqlRestriction(ipk + " between nilai_mulai and nilai_sampai"))
								.addOrder(Order.asc("masaStudiMaksimal")).addOrder(Order.desc("nilaiMulai")),
						Judisium.class);
			}

			System.out.print("mahasiswa " + mahasiswa + ", maxSmt = " + maxSmt + ", ipk : " + ipk + ", nilaiHurufs : "
					+ lowerCase + ", judisiums = " + judisiums);
			if (judisiums.isEmpty()) {
				detailperkuliahans = null;
				return new Judisium();
			} else {

				List<Judisium> judisiumsBener = new ArrayList<Judisium>();
				for (Judisium j : judisiums) {

					if (j.getTermasukMengulang()) {
						lowerCase = new HashSet<String>();
						for (Long detailperkuliahanid : detailperkuliahans) {
							Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
									.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
							if (detailperkuliahan != null) {
								if (!detailperkuliahan.getNilaiHuruf().trim().isEmpty()) {

									Matakuliah matakuliah = detailperkuliahan.getMatakuliahKonversi() != null
											? detailperkuliahan.getMatakuliahKonversi()
											: detailperkuliahan.getPerkuliahan().getMatakuliah();

									if (matakuliah != null && matakuliah.getNama() != null) {
										String mk = j.getKecualiMk().trim().toLowerCase();
										if (mk.isEmpty() || !(mk.contains(";" + matakuliah.getNama().toLowerCase() + ";")
												|| mk.contains(";" + matakuliah.getKode().toLowerCase() + ";"))) {
											lowerCase.add(detailperkuliahan.getNilaiHuruf().trim().toLowerCase());
										}
									}
								}
							}
						}
					}

					Boolean benerlahSemua = true;
					if (!j.getNilaiHurufYangHarusAda().trim().isEmpty()) {
						boolean adaSemua = true;
						for (String s : j.getNilaiHurufYangHarusAda().trim().toLowerCase().split(",")) {
							adaSemua &= lowerCase.contains(s);
						}
						benerlahSemua &= adaSemua;
					}

					if (!j.getNilaiHurufYangTidakBolehAda().trim().isEmpty()) {
						boolean adaSemua = true;
						for (String s : j.getNilaiHurufYangTidakBolehAda().trim().toLowerCase().split(",")) {
							adaSemua &= !lowerCase.contains(s.trim());
						}
						benerlahSemua &= adaSemua;
					}

					if (benerlahSemua) {
						judisiumsBener.add(j);
					}
				}
				detailperkuliahans = null;
				// System.out.println(", judisiumsBener = " + judisiumsBener);
				return judisiumsBener.isEmpty() ? new Judisium() : judisiumsBener.get(0);
			}
		}

	public static List<Object[]> dataMkKrs(Mahasiswa mahasiswa, Integer semester, String genapGanjil, Integer tahap,
				String mk, boolean ygSudahDisetujui, boolean konversi, boolean bukankonversi, boolean sp) {

			if (mahasiswa == null) {
				return null;
			}

			String sql = "select f.kode,f.nama,a.total_nilai,a.nilai_ip,a.nilai_huruf,a.semester,f.sks as jumlahMk,a.perkuliahan "
					+ subSqlHitungIp + " where a.ikuti_perkuliahan is null\n "
					+ (sp ? " " : " and (b.status_semesterpendek is null or a.semester<" + semester + ") ")
					+ (konversi ? " and a.matakuliah_konversi is not null " : "")
					+ (bukankonversi ? " and a.perkuliahan is not null " : "")
					+ (mk == null || mk.trim().isEmpty() ? ""
							: " and (f.kode ilike '%" + mk + "%' or f.nama ilike '%" + mk + "%' )")
					+ (genapGanjil == null || genapGanjil.trim().isEmpty() ? ""
							: " and a.semester " + (genapGanjil.equals(Perkuliahan.GENAP) ? " % 2 = 0" : " % 2 = 1 "))
					+ " and a.mahasiswa = " + mahasiswa.getId() + "\n"
					+ (ygSudahDisetujui ? " and a.persetujuan=1 \n " : " and a.persetujuan=0\n ")
					+ (tahap == null || tahap.equals(0) ? (semester == null ? "" : " and a.semester = " + semester + " ")
							: (" and a.tahap = " + tahap + ""));

			List<Object[]> data = HibernateUtil.currentSession().createSQLQuery(sql).list();

			return data;
		}

	/**
	 * HITUNG ULANG PARALEL: sama seperti {@link #realoadNilaiLangsung} namun tiap mahasiswa diproses
	 * di THREAD & SESSION Hibernate SENDIRI (aman paralel). Jumlah thread = sebanyak mahasiswa, TAPI
	 * maksimal {@code maxThread} (default 50), lalu dikecilkan lagi oleh {@code DbThreadPool.safe} agar
	 * tak melampaui kapasitas pool DB. Instance TERKELOLA diambil ulang via {@code session.get(id)} pada
	 * session milik masing-masing thread (tidak berbagi objek grid). Setelah commit, nilai SKALAR hasil
	 * (total/nilaiHuruf/detailNilai) disalin ke instance CACHE per-id (tak memicu lazy-load, satu id per
	 * thread jadi aman) supaya tampilan tak basi. eventListener dijalankan SETELAH semua selesai (di
	 * thread pemanggil / ZK) untuk render ulang.
	 */
	public static void realoadNilaiLangsungParalel(final Perkuliahan perkuliahan,
			final Boolean sembunyikanNilaiJikaBelumDiverifikasi, final EventListener eventListener,
			Collection<Long> detailperkuliahans, int maxThread) throws Exception {
		realoadNilaiLangsungParalel(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi, eventListener,
				detailperkuliahans, maxThread, null);
	}

	/**
	 * Varian dengan PELAPORAN PROGRES: bila {@code diproses} non-null, di-increment setiap 1 mahasiswa
	 * selesai dihitung, sehingga pemanggil dapat menampilkan progress bar (X dari total). Total = jumlah
	 * detailperkuliahan yang diproses.
	 */
	public static void realoadNilaiLangsungParalel(final Perkuliahan perkuliahan,
			final Boolean sembunyikanNilaiJikaBelumDiverifikasi, final EventListener eventListener,
			Collection<Long> detailperkuliahans, int maxThread,
			final java.util.concurrent.atomic.AtomicInteger diproses) throws Exception {
		if (detailperkuliahans == null) {
			detailperkuliahans = perkuliahan.ambilDetailperkuliahan();
		}
		final List<FormatNilai> formatNilais = Common.getFormatNilais(perkuliahan);
		final List<Long> ids = new ArrayList<Long>(detailperkuliahans);
		int jml = ids.size();
		if (jml <= 1 || maxThread <= 1) {
			// 1 mahasiswa / tak perlu paralel -> pakai jalur sekuensial yang sudah ada.
			realoadNilaiLangsung(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi, eventListener, detailperkuliahans);
			if (diproses != null && jml > 0) {
				diproses.addAndGet(jml);
			}
			return;
		}
		int nThread = ais.common.DbThreadPool.safe(Math.min(jml, Math.max(1, maxThread)));
		java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(nThread);
		for (final Long id : ids) {
			if (id == null) {
				continue;
			}
			executor.submit(new Runnable() {
				@Override
				public void run() {
					org.hibernate.Session session = null;
					org.hibernate.Transaction tx = null;
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session.get(Detailperkuliahan.class, id);
						if (detailperkuliahan == null) {
							return;
						}
						detailperkuliahan.reloadFormatNilai(formatNilais, sembunyikanNilaiJikaBelumDiverifikasi);
						Double total = detailperkuliahan.hitungTotalNilai(true, formatNilais);
						Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() != null
								? detailperkuliahan.getPerkuliahan().getMatakuliah()
								: detailperkuliahan.getMatakuliahKonversi();
						NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total,
								detailperkuliahan.getMahasiswa().getTahunangkatan(),
								detailperkuliahan.getMahasiswa().getJurusan(),
								detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
								detailperkuliahan.getTahunAkademik(),
								detailperkuliahan.getPerkuliahan() == null ? null
										: detailperkuliahan.getPerkuliahan().getGanjilGenap(),
								matakuliah == null ? "" : matakuliah.getKode(),
								matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
						detailperkuliahan.setTotalIP(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
						detailperkuliahan.setTotalNilai(total);
						detailperkuliahan.setNilaiHuruf(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
						detailperkuliahan.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());
						Double totalSementara = detailperkuliahan.hitungTotalNilaiSementara(true, formatNilais);
						NilaiHuruf nilaiHurufSm = Common.getNilaiHuruf(totalSementara,
								detailperkuliahan.getMahasiswa().getTahunangkatan(),
								detailperkuliahan.getMahasiswa().getJurusan(),
								detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
								detailperkuliahan.getTahunAkademik(),
								detailperkuliahan.getPerkuliahan() == null ? null
										: detailperkuliahan.getPerkuliahan().getGanjilGenap(),
								matakuliah == null ? "" : matakuliah.getKode(),
								matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
						detailperkuliahan.setTotalNilaiSementara(totalSementara);
						detailperkuliahan.setNilaiHurufSementara(nilaiHurufSm == null ? "" : nilaiHurufSm.getNilaiHuruf());
						detailperkuliahan.setTotalIPSementara(nilaiHurufSm == null ? 0.0 : nilaiHurufSm.getNilaiDiIPK());
						tx = session.beginTransaction();
						session.update(detailperkuliahan);
						tx.commit();
						tx = null;
						try {
							Detailperkuliahan cached = (Detailperkuliahan) GeneralValueObject
									.ambilData(Detailperkuliahan.class, id.toString());
							if (cached != null && cached != detailperkuliahan) {
								cached.setDetailNilai(detailperkuliahan.getDetailNilai());
								cached.setDetailNilaiKunci(detailperkuliahan.getDetailNilaiKunci());
								cached.setTotalNilai(detailperkuliahan.getTotalNilai());
								cached.setTotalIP(detailperkuliahan.getTotalIP());
								cached.setNilaiHuruf(detailperkuliahan.getNilaiHuruf());
								cached.setLulus(detailperkuliahan.getLulus());
								cached.setTotalNilaiSementara(detailperkuliahan.getTotalNilaiSementara());
								cached.setNilaiHurufSementara(detailperkuliahan.getNilaiHurufSementara());
								cached.setTotalIPSementara(detailperkuliahan.getTotalIPSementara());
							}
						} catch (Exception eCache) { ais.common.ErrorAuditUtil.record(eCache, "auto-audit(empty-catch) src/ais/common/CommonAcademicKrsNilaiHelper.java:1411");
						}
					} catch (Exception e) {
						if (tx != null) {
							try { tx.rollback(); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/common/CommonAcademicKrsNilaiHelper.java:1415");}
						}
						tampilErrorJikaAdmin(e);
					} finally {
						if (session != null) {
							try { if (session.isOpen()) session.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonAcademicKrsNilaiHelper.java:1420");}
							try { session.disconnect(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonAcademicKrsNilaiHelper.java:1421");}
							try { if (session.isOpen()) session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonAcademicKrsNilaiHelper.java:1422");}
						}
						if (diproses != null) {
							diproses.incrementAndGet();
						}
					}
				}
			});
		}
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		if (eventListener != null) {
			eventListener.onEvent(null);
		}
	}

	public static void realoadNilaiLangsung(final Perkuliahan perkuliahan,
				Boolean sembunyikanNilaiJikaBelumDiverifikasi, final EventListener eventListener,
				Collection<Long> detailperkuliahans) throws Exception {
			if (detailperkuliahans == null) {
				detailperkuliahans = perkuliahan.ambilDetailperkuliahan();
			}
			realoadNilaiLangsung(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi, eventListener, detailperkuliahans,
					true);
		}

	public static void realoadNilaiLangsung(final Perkuliahan perkuliahan,
				Boolean sembunyikanNilaiJikaBelumDiverifikasi, final EventListener eventListener,
				Collection<Long> detailperkuliahans, boolean coba) throws Exception {

			try {
				List<FormatNilai> formatNilais = Common.getFormatNilais(perkuliahan);
				for (Long detailperkuliahanid : detailperkuliahans) {
					Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
							.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
					if (detailperkuliahan != null) {
						detailperkuliahan.reloadFormatNilai(formatNilais, sembunyikanNilaiJikaBelumDiverifikasi);
						Double total = detailperkuliahan.hitungTotalNilai(true, formatNilais);

						Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() != null
								? detailperkuliahan.getPerkuliahan().getMatakuliah()
								: detailperkuliahan.getMatakuliahKonversi();

	//					if(total)

						System.out.println("total -> " + total);

						NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total,
								detailperkuliahan.getMahasiswa().getTahunangkatan(),
								detailperkuliahan.getMahasiswa().getJurusan(),
								detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
								detailperkuliahan.getTahunAkademik(),
								detailperkuliahan.getPerkuliahan() == null ? null
										: detailperkuliahan.getPerkuliahan().getGanjilGenap(),
								matakuliah == null ? "" : matakuliah.getKode(),
								matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
						detailperkuliahan.setTotalIP(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
						detailperkuliahan.setTotalNilai(total);
						detailperkuliahan.setNilaiHuruf(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
						detailperkuliahan.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());

						Double totalSementara = detailperkuliahan.hitungTotalNilaiSementara(true, formatNilais);
						nilaiHuruf = Common.getNilaiHuruf(totalSementara,
								detailperkuliahan.getMahasiswa().getTahunangkatan(),
								detailperkuliahan.getMahasiswa().getJurusan(),
								detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
								detailperkuliahan.getTahunAkademik(),
								detailperkuliahan.getPerkuliahan() == null ? null
										: detailperkuliahan.getPerkuliahan().getGanjilGenap(),
								matakuliah == null ? "" : matakuliah.getKode(),
								matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

						detailperkuliahan.setTotalNilaiSementara(totalSementara);
						detailperkuliahan.setNilaiHurufSementara(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
						detailperkuliahan.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

						Session s = HibernateUtil.currentNativeSession();
						org.hibernate.Transaction tx = null;
						try {
							tx = s.beginTransaction();
							Common.refreshUpdate(s, detailperkuliahan);
							tx.commit();
						} catch (Exception hibEx) {
							if (tx != null) {
								try { tx.rollback(); } catch (Exception rb) { ais.common.ErrorAuditUtil.record(rb, "auto-audit(empty-catch) src/ais/common/CommonAcademicKrsNilaiHelper.java:1510");}
							}
							throw hibEx;
						} finally {
							try { s.clear(); } catch (Exception c) { ais.common.ErrorAuditUtil.record(c, "auto-audit(empty-catch) src/ais/common/CommonAcademicKrsNilaiHelper.java:1514");}
							try { s.disconnect(); } catch (Exception d) { ais.common.ErrorAuditUtil.record(d, "auto-audit(empty-catch) src/ais/common/CommonAcademicKrsNilaiHelper.java:1515");}
							try { HibernateUtil.closeSession(); } catch (Exception cl) { ais.common.ErrorAuditUtil.record(cl, "auto-audit(empty-catch) src/ais/common/CommonAcademicKrsNilaiHelper.java:1516");}
						}
					}
				}
			} catch (Exception e) {
				tampilErrorJikaAdmin(e);
				try {
					if (coba) {
						Session session = HibernateUtil.currentSession();
						perkuliahan.singkronkan(session);
						Collection<Long> detailperkuliahansbaru = perkuliahan.ambilDetailperkuliahan();
						realoadNilaiLangsung(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi, eventListener,
								detailperkuliahansbaru, false);
					}
				} catch (Exception ee) {
					tampilErrorJikaAdmin(ee);
				}
			}

			if (eventListener != null) {
				eventListener.onEvent(null);
			}

		}

	public static void realoadNilai(final Perkuliahan perkuliahan, final Boolean sembunyikanNilaiJikaBelumDiverifikasi,
				final EventListener eventListener, final Collection<Long> detailperkuliahans) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					realoadNilaiLangsung(perkuliahan, sembunyikanNilaiJikaBelumDiverifikasi, eventListener,
							detailperkuliahans);
				}
			}, "Sedang memproses data penilaian.. harap tunggu..");

		}
}
