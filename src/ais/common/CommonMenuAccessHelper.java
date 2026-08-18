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
 * Helper menu, tab, akses pengguna, footer, dan validasi umum navigasi aplikasi.
 * Dipisahkan dari Common agar Common tetap ringan sebagai facade/wrapper.
 */
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class CommonMenuAccessHelper extends Common {

	private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CommonMenuAccessHelper.class);

	private CommonMenuAccessHelper() {
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
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonMenuAccessHelper.java:410");
		}
	}

	public static void insertToTab(West navigasi, MyToolbarbuttonConfig menuService, final Tabbox iframe,
				final Menu menu, final LogLogin login) {

			try {

				if (menu.getBukaHalamanBaru()) {
					Tbmuser tbmuser = Common.getCurrentUser();
					Executions.getCurrent()
							.sendRedirect(
									(menu.getUrl().startsWith("http")
											? menu.getUrl()
											: (Common.getRequestHostWithProtocol() + menu.getUrl()))
											+ (tbmuser == null ? ""
													: "?uid=" + URLEncoder.encode(
															Common.desEncrypter.get().encrypt(tbmuser.getUserId()),
															"UTF-8")),
									"_blank");
					return;
				}

				Common.pilihMenu(navigasi, menuService);

				if (menu.getUrl() != null) {
					String menuUrlTrim = menu.getUrl().trim();
					// FIX noise ErrorLog: ClassNotFoundException RUTIN saat menu.getUrl()
					// berisi path ZUL/URL (kasus paling umum, mis. "/pages/master/log_login.zul"),
					// bukan nama class Java -- Class.forName memang didesain gagal utk
					// kasus ini & jatuh ke penanganan ZUL/URL di bawah. Saring dulu pola
					// yg JELAS BUKAN nama class agar exception jadi genuinely langka.
					boolean kemungkinanNamaClass = !menuUrlTrim.isEmpty() && !menuUrlTrim.startsWith("/")
							&& !menuUrlTrim.toLowerCase().startsWith("http")
							&& !menuUrlTrim.toLowerCase().endsWith(".zul")
							&& !menuUrlTrim.toLowerCase().endsWith(".jsp")
							&& !menuUrlTrim.toLowerCase().endsWith(".html");
					if (kemungkinanNamaClass) {
						try {
							MyWindow window = (MyWindow) Class.forName(menuUrlTrim).newInstance();
							insertToTab(navigasi, menuService, iframe, menu, window, login);
							return;
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonMenuAccessHelper.java:441");

						}
					}
				}

				if (menu.getUrl() != null && menu.getUrl().toLowerCase().trim().startsWith("http")) {
					try {

						String url = menu.getUrl().trim();
						Tbmuser tbmuser = Common.getCurrentUser();

						if (tbmuser != null && tbmuser.getUserId() != null) {
							url = org.apache.commons.lang3.StringUtils.replace(url, "{username}",
									URLEncoder.encode(tbmuser.getUserId(), "UTF-8"));
						}

						if (Common.bolehKonfigurasi("pakai_include_saat_menu")) {
							MyWindow window = new MyWindow();
							MyInclude include = new MyInclude(url);
							include.setWidth("100%");
							include.setHeight("100%");
							window.appendChild(include);
							window.setWidth("100%");
							window.setHeight("100%");
							insertToTab(navigasi, menuService, iframe, menu, window, login);
						} else {

							MyWindow window = new MyWindow();
							MyIframe include = new MyIframe(url);
							include.setWidth("100%");
							include.setHeight("100%");
							window.appendChild(include);
							window.setWidth("100%");
							window.setHeight("100%");
							insertToTab(navigasi, menuService, iframe, menu, window, login);
						}

						return;
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonMenuAccessHelper.java:479");

					}
				}

				List<Tab> tabs = iframe.getTabs().getChildren();
				boolean ada = false;
				for (Tab tab : tabs) {
					// // System.out.println("menu => " + tab.getAttribute("menu"));
					if (tab.getAttribute("menu") != null && (tab.getAttribute("menu") instanceof Menu)) {
						Menu m = (Menu) tab.getAttribute("menu");
						if (m.getId().equals(menu.getId())) {
							// System.out.println("ada menu => " + menu);
							ada = true;
							tab.setSelected(true);
							break;
						}
					}

					else if (tab.getLabel().equalsIgnoreCase(menu.getLabel())) {
						ada = true;
						tab.setSelected(true);
						break;
					}
				}
				if (!ada) {
					MyTabConfig tab = new MyTabConfig(
							login == null || login.getTbmuser() == null || login.getTbmuser().hakAkses() == null ? ""
									: login.getTbmuser().hakAkses().getRoleId(),
							menu.getLabel(), "");

					if (menu.getBigIcon() != null && menu.getBigIcon().endsWith("svg")) {
						tab.setImage(menu.getBigIcon());
					} else {
						File fileAsli = new File(Sessions.getCurrent().getWebApp()
								.getRealPath(menu.getBigIcon() == null || menu.getBigIcon().trim().equals("")
										? "/img/none-icon.png"
										: menu.getBigIcon().trim()));

						// Ikon menu (kolom bigIcon) bisa menunjuk ke berkas yang sudah dihapus/dipindah dari
						// disk; ImageIO.read akan gagal (IIOException "Can't read input file!") pada berkas
						// yang tak ada. resizeImage(...) sendiri sudah aman (try/catch internal, tak pernah
						// melempar), tapi tanpa cek ini <img> di browser tetap menunjuk ke berkas kecil yang
						// TAK PERNAH terbentuk -> ikon patah/blank bagi pengguna. Cek dulu spy langsung
						// jatuh ke ikon default bila sumbernya memang tak ada, tanpa mencoba resize yang
						// pasti gagal.
						if (!fileAsli.exists() || !Common.isImage(fileAsli)) {
							fileAsli = new File(
									Sessions.getCurrent().getWebApp().getRealPath("/img/none-icon.png"));
						}

						File filekecil = new File(fileAsli.getAbsolutePath() + ".png");

						if (!filekecil.exists()) {
							try {
								CommonMedia.resizeImage(fileAsli, 16, 16, filekecil);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonMenuAccessHelper.java:523");
								// TODO: handle exception
							}
						}
						if (filekecil.exists()) {
							tab.setImage("/img/" + filekecil.getName());
						}
					}
					tab.setAttribute("menu", menu);

					tab.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							// System.out.println(
							// "============================= " + tab.getLabel() + "
							// ================ clicked");
							DetailLogLogin detailLogLogin = new DetailLogLogin();
							detailLogLogin.setKeterangan(menu.getLabel());
							detailLogLogin.setWaktu(ais.ui.util.WaktuUtil.getDate());
							detailLogLogin.setLogLogin(login);
							detailLogLogin.setHalaman(menu.getUrl());
							Session session = null;
							Transaction tx = null;
							try {
								session = HibernateUtil.openSession();
								tx = session.beginTransaction();
								session.save(detailLogLogin);
								tx.commit();
								Sessions.getCurrent().setAttribute("detailLogLogin", detailLogLogin);
							} catch (Exception e) {
								rollbackQuietly(tx);
								tampilErrorJikaAdmin(e);
							} finally {
								HibernateUtil.closeSessionQuietly(session);
							}
						}
					});
					tab.setClosable(true);
					tab.setSelected(true);
					iframe.getTabs().appendChild(tab);
					Tabpanel tabpanel = new ais.ui.util.MyTabpanel();

					if (Common.bolehKonfigurasi("pakai_include_saat_menu")) {
						MyInclude include = new MyInclude(menu.getUrl().trim());
						include.setWidth("100%");
						include.setHeight("100%");
						tabpanel.appendChild(include);
					} else {
						MyIframe include = new MyIframe(menu.getUrl().trim());
						include.setWidth("100%");
						include.setHeight("100%");
						tabpanel.appendChild(include);
					}

					iframe.getTabpanels().appendChild(tabpanel);
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				try {
					PesanFormalHelper.tampilkanGagalException("pembukaan menu " + (menu == null ? "" : menu.getLabel()),
							e, new String[] {
									"Muat ulang (refresh) halaman browser Bapak/Ibu, kemudian buka kembali menu ini.",
									"Bila menu masih belum bisa dibuka, coba keluar (logout) lalu masuk (login) kembali." });
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/common/CommonMenuAccessHelper.java:587");
				}

			}
		}

	public static void insertToTab(final West navigasi, final MyToolbarbuttonConfig menuService, Tabbox iframe,
				final Menu menu, MyWindow window, final LogLogin login) {

			Common.pilihMenu(navigasi, menuService);

			window.setHeight("100%");
			window.setWidth("100%");
			window.setTitle("");
			window.setClosable(false);

			List<MyTabConfig> tabs = iframe.getTabs().getChildren();
			boolean ada = false;
			for (Tab tab : tabs) {
				// // System.out.println("menu => " + tab.getAttribute("menu"));
				if (tab.getAttribute("menu") != null && (tab.getAttribute("menu") instanceof Menu)) {
					Menu m = (Menu) tab.getAttribute("menu");
					if (m.getId().equals(menu.getId())) {
						// System.out.println("ada menu => " + menu);
						ada = true;
						tab.setSelected(true);
						break;
					}
				}

				else if (tab.getLabel().equalsIgnoreCase(menu.getLabel())) {
					ada = true;
					tab.setSelected(true);
					break;
				}
			}
			if (!ada) {
				MyTabConfig tab = new MyTabConfig(
						login == null || login.getTbmuser() == null || login.getTbmuser().hakAkses() == null ? ""
								: login.getTbmuser().hakAkses().getRoleId(),
						menu.getLabel(), "");

				File fileAsli = new File(Sessions.getCurrent().getWebApp()
						.getRealPath(menu.getBigIcon() == null || menu.getBigIcon().trim().equals("") ? "/img/none-icon.png"
								: menu.getBigIcon().trim()));

				File filekecil = new File(fileAsli.getAbsolutePath() + ".png");

				if (!filekecil.exists()) {
					CommonMedia.resizeImage(fileAsli, 16, 16, filekecil);
				}

				// AImage aImage = new

				tab.setImage("/img/" + filekecil.getName());
				tab.setAttribute("menu", menu);

				tab.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// // System.out.println("============================= " +
						// tab.getLabel() + " ================ clicked");
						DetailLogLogin detailLogLogin = new DetailLogLogin();
						detailLogLogin.setKeterangan(menu.getLabel());
						detailLogLogin.setWaktu(ais.ui.util.WaktuUtil.getDate());
						detailLogLogin.setLogLogin(login);
						detailLogLogin.setHalaman(menu.getUrl());
							Session session = null;
							Transaction tx = null;
							try {
								session = HibernateUtil.openSession();
								tx = session.beginTransaction();
								session.save(detailLogLogin);
								tx.commit();
								Sessions.getCurrent().setAttribute("detailLogLogin", detailLogLogin);
							} catch (Exception e) {
								rollbackQuietly(tx);
								tampilErrorJikaAdmin(e);
							} finally {
								HibernateUtil.closeSessionQuietly(session);
							}
					}
				});
				tab.setClosable(true);
				tab.setSelected(true);
				iframe.getTabs().appendChild(tab);
				Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
				tabpanel.appendChild(window);
				iframe.getTabpanels().appendChild(tabpanel);
			}
		}

	public static AccessedUsers setUserAccess(HttpServletRequest request) {
		/* Jalur ini tidak mempunyai entity caller yang perlu di-attach. Membuka session
		 * tambahan hanya untuk session.get() menggandakan peluang mendapat koneksi pool
		 * yang sudah EOF. ensureAccessedUsersExists sudah memakai session terisolasi. */
		return setUserAccess(null, request);
	}

	/**
	 * Menyimpan/mengambil data AccessedUsers secara aman.
	 *
	 * Masalah lama:
	 * - Dua request dengan JSESSIONID yang sama dapat sama-sama melihat data belum ada.
	 * - Keduanya mencoba insert ke accessed_users.nama yang merupakan primary key.
	 * - Insert kedua memicu duplicate key, transaction PostgreSQL menjadi aborted.
	 * - Session yang sama kemudian dipakai query lain sehingga muncul: current transaction is aborted.
	 *
	 * Solusi:
	 * - Proses create AccessedUsers dilakukan di session lokal terpisah.
	 * - Jika duplicate key terjadi, transaction di-rollback, session dibersihkan, lalu data dibaca ulang.
	 * - Entity kemudian di-attach ulang ke session caller agar relasi OnlineUsers tetap aman.
	 */
	public static AccessedUsers setUserAccess(Session mySession, HttpServletRequest request) {
		if (request == null) {
			return null;
		}

		HttpSession httpSession = request.getSession();
		String sessionId = httpSession == null ? null : httpSession.getId();
		if (sessionId == null || sessionId.trim().length() == 0) {
			return null;
		}
		sessionId = sessionId.trim();

		AccessedUsers accessedUsers = null;
		try {
			accessedUsers = ensureAccessedUsersExists(request, sessionId);
		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
		}

		try {
			if (HibernateUtil.isSessionUsable(mySession)) {
				AccessedUsers managed = (AccessedUsers) mySession.get(AccessedUsers.class, sessionId);
				if (managed != null) {
					accessedUsers = managed;
				}
			}
		} catch (Exception e) {
			// Jangan membuat session caller menjadi aborted hanya karena gagal attach.
			tampilErrorJikaAdmin(e);
		}

		if (accessedUsers == null) {
			accessedUsers = new AccessedUsers();
			accessedUsers.setNama(sessionId);
			accessedUsers.setHost(Common.getRequestHost(request));
		}

		/* HttpSession bisa sudah di-invalidate (logout/timeout) di tengah request lain →
		 * setAttribute melempar IllegalStateException. Itu bukan bug aplikasi; abaikan dengan
		 * aman dan tetap kembalikan data agar request berjalan. */
		try {
			httpSession.setAttribute("accessedUsers", accessedUsers);
		} catch (IllegalStateException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonMenuAccessHelper.java:746");
			/* sesi sudah invalid — tidak perlu menyimpan, lanjutkan saja */
		}
		return accessedUsers;
	}

	private static AccessedUsers ensureAccessedUsersExists(HttpServletRequest request, String sessionId) {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			return ensureAccessedUsersExists(session, request, sessionId);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static AccessedUsers ensureAccessedUsersExists(Session session, HttpServletRequest request, String sessionId) {
		AccessedUsers accessedUsers = findAccessedUsers(session, sessionId);
		if (accessedUsers != null) {
			return accessedUsers;
		}

		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			accessedUsers = new AccessedUsers();
			accessedUsers.setNama(sessionId);
			accessedUsers.setHost(Common.getRequestHost(request));
			session.save(accessedUsers);
			session.flush();
			tx.commit();
			return accessedUsers;
		} catch (Exception e) {
			rollbackQuietly(tx);
			clearSessionQuietly(session);
			if (isDuplicateAccessedUsersException(e)) {
				return findAccessedUsersWithNewSession(sessionId);
			}
			tampilErrorJikaAdmin(e);
			return findAccessedUsersWithNewSession(sessionId);
		}
	}

	private static AccessedUsers findAccessedUsersWithNewSession(String sessionId) {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			return findAccessedUsers(session, sessionId);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static AccessedUsers findAccessedUsers(Session session, String sessionId) {
		if (!HibernateUtil.isSessionUsable(session) || sessionId == null) {
			return null;
		}
		try {
			return (AccessedUsers) session.get(AccessedUsers.class, sessionId);
		} catch (Exception e) {
			return null;
		}
	}

	public static void rollbackQuietly(Transaction tx) {
		try {
			if (tx != null && tx.isActive()) {
				tx.rollback();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonMenuAccessHelper.java:815");
		}
	}

	private static void clearSessionQuietly(Session session) {
		try {
			if (session != null && session.isOpen()) {
				session.clear();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonMenuAccessHelper.java:824");
		}
	}

	private static boolean isDuplicateAccessedUsersException(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			String message = current.getMessage();
			if (message != null) {
				String lower = message.toLowerCase();
				if (lower.indexOf("duplicate key value") >= 0
						&& (lower.indexOf("accessed_users_pkey") >= 0 || lower.indexOf("accessed_users") >= 0)) {
					return true;
				}
			}
			current = current.getCause();
		}
		return false;
	}

	public static void setFooterText(HttpServletRequest request, A a) {
			String req = request.getRequestURL().toString();
			req = req.replaceAll("http://", "");
			req = req.split(":")[0];
			req = req.split("/")[0];

			String content_path = request.getContextPath().replace("/", "");
			if (content_path.contains("contoh")) {
				a.setLabel("\u00a9 " + ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + " Universitas Anda");
				a.setHref("http://universitas.anda.com");
			} else if (req.contains("fst")) {
				a.setLabel("\u00a9 " + ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
						+ " Fakultas Sains dan Teknologi, Universitas Islam Negeri Syarif Hidayatullah Jakarta");
				a.setHref("http://fst.uinjkt.ac.id/fstportal");
			} else if (req.contains("ais")) {
				a.setLabel("\u00a9 " + ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
						+ " Universitas Islam Negeri Syarif Hidayatullah Jakarta");
			} else if (req.equals("114.134.75.206") || req.contains("smartkampus")) {
				a.setLabel("\u00a9 " + ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + "Smart Kampus");
			}
		}

	public static Menu getCurrentMenu() {

			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}

			if (request == null) {
				request = RequestContext.get();
			}

			List<Menu> menus = (List<Menu>) request.getSession().getAttribute("currentMenus");
			if (menus == null) {
				return null;
			}
	//		String url = ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getRequestPath();

			Menu menu = (Menu) request.getSession().getAttribute("currentMenu");
			if (menu == null) {
				String url = request.getServletPath();
				for (Menu m : menus) {
					if (m.getUrl() != null && url != null && m.getUrl().equalsIgnoreCase(url)) {
						menu = m;
						break;
					}
				}
				System.out.println("url = " + url + ", menu = " + menu + ", menus " + menus.size());
				if (menu == null) {
					menu = (Menu) request.getSession().getAttribute("currentMenu");
				} else {
					request.getSession().setAttribute("currentMenu", menu);
				}
			}
			return menu;
		}

	public static Boolean isMenuExist(Tree tree, Menu menu) {
			List child = tree.getChildren();
			for (Object o : child) {
				// System.out.println(o.getClass().getName());
				if (o instanceof Treechildren) {
					Treechildren treechildren = (Treechildren) o;
					List<MyTreeitemConfig> treeitems = treechildren.getChildren();
					for (MyTreeitemConfig treeitem : treeitems) {
						if (menu.getUrl() != null && treeitem.getValue() != null
								&& menu.getUrl().trim().equals(treeitem.getValue())) {
							return true;
						}
					}
				}
			}
			return false;
		}

	public static Boolean isMenuExist(Collection<Menu> menus, Menu menu) {
			return CommonCurrentSessionHelper.isMenuExist(menus, menu);
		}

	public static Boolean checkValidasi(Object... properties) throws Exception {
			if (properties == null || properties.length == 0)
				return true;

			for (Object o : properties) {
				if (o == null) {
					MyMessageboxConfig.show("Field harus dilengkapi");
					return false;
				}

				if (o instanceof String) {
					if (((String) o).trim().isEmpty()) {
						MyMessageboxConfig.show("Field harus dilengkapi");
						return false;
					}
				} else if (o instanceof Textbox) {
					Textbox txt = (Textbox) o;
					if (txt.getValue() == null || txt.getValue().trim().isEmpty()) {
						MyMessageboxConfig.show("Field ini harus diisi");
						txt.focus();
						return false;
					}
				} else if (o instanceof Bandbox) {
					Bandbox bnd = (Bandbox) o;
					if (bnd.getValue() == null || bnd.getValue().trim().isEmpty()) {
						MyMessageboxConfig.show("Field ini harus diisi");
						bnd.focus();
						return false;
					}
				} else if (o instanceof Combobox) {
					Combobox cmb = (Combobox) o;
					if (cmb.getSelectedItem() == null || cmb.getSelectedItem().getValue() == null) {
						MyMessageboxConfig.show("Field ini harus diisi");
						cmb.focus();
						return false;
					}
				} else if (o instanceof Decimalbox) {
					Decimalbox dec = (Decimalbox) o;
					// Decimalbox getValue() biasanya me-return BigDecimal/Double, bukan String.
					if (dec.getValue() == null || String.valueOf(dec.getValue()).trim().isEmpty()) {
						MyMessageboxConfig.show("Field ini harus diisi");
						dec.focus();
						return false;
					}
				}
			}
			return true;
		}
}
