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
 * Helper pemilihan combobox, fakultas, dan jurusan.
 * Dipisahkan dari Common agar Common tetap ringan sebagai facade/wrapper.
 */
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class CommonComboSelectionHelper extends Common {

	private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CommonComboSelectionHelper.class);

	private CommonComboSelectionHelper() {
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
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonComboSelectionHelper.java:410");
		}
	}

	public static void selectFakultasAndJurusan(Combobox combobox, Combobox jurusan, Combobox fakultas) {

			if (combobox.getSelectedItem() != null) {
				Dosen dosen = (Dosen) combobox.getSelectedItem().getValue();
				if (dosen.getFakultas() != null) {
					Common.selectComboItem(fakultas, dosen.getFakultas());
					Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
							Restrictions.eq("fakultas", dosen.getFakultas()));
					fakultas.setDisabled(true);
					if (dosen.getJurusan() != null) {
						Common.pilihJurusan(jurusan, dosen.getJurusan());
						jurusan.setDisabled(true);
					} else {
						jurusan.setDisabled(false);
					}
				} else {
					fakultas.setDisabled(false);
					jurusan.setDisabled(false);
				}

			}

		}

	public static void selectComboItem(boolean jikaDataTidakDitemukanDitambahkan, Combobox combo, Object value) {
			selectComboItem(combo, value, jikaDataTidakDitemukanDitambahkan);
		}

	public static void selectComboItem(Combobox combo, Object value) {
			selectComboItem(combo, value, false);
		}

	public static void selectComboItem(Combobox combo, Object value, boolean jikaDataTidakDitemukanDitambahkan) {

			try {

				if (combo == null) {
					return;
				}
				combo.setSelectedIndex(-1);
				if (!combo.isDisabled() && value == null) {
					List<Comboitem> list = combo.getItems();
					for (Comboitem comboitem : list) {
						if (comboitem.getValue() == null) {
							combo.setSelectedItem(comboitem);
							return;
						}
					}
					combo.setSelectedItem(null);
					return;
				} else if (combo.isDisabled() && value == null) {
					return;
				} else if (value == null) {
					combo.setSelectedItem(null);
					return;
				}

				List<Comboitem> list = combo.getItems();
				ClassMetadata metadata = HibernateUtil.getClassMetadata(value.getClass());
				Serializable identityvalue = null;
				if (value != null && value instanceof Tbmuser) {
					identityvalue = ((Tbmuser) value).getUserId();
				} else if (value != null && value instanceof Tbmrole) {
					identityvalue = ((Tbmrole) value).getRoleId();
				} else if (value != null && value instanceof Program) {
					identityvalue = ((Program) value).getNama();
				} else if (value != null && value instanceof GeneralValueObject) {
					identityvalue = ((GeneralValueObject) value).getId();
				} else if (metadata != null && !(value instanceof String) && !(value instanceof Integer)
						&& !(value instanceof Long) && !(value instanceof Double) && !(value instanceof Date)) {
					try {
						identityvalue = metadata.getIdentifier(value, EntityMode.POJO);
					} catch (Exception e) {
						// FIX NPE berulang: metadata.getIdentifier(value, ...) memanggil getter
						// property id milik "value" secara reflektif (Hibernate BasicPropertyAccessor)
						// -- bila value adalah entity baru/transient/proxy dengan property
						// underlying null, getter itu sendiri bisa melempar NPE SEBELUM
						// identityvalue sempat diisi, jadi guard "identityvalue == null" di
						// bawah belum sempat menyelamatkan. Tangkap di sini, biarkan
						// identityvalue tetap null supaya guard tsb yang menangani lanjutannya.
						ais.common.ErrorAuditUtil.record(e, "auto-audit(npe-getidentifier) src/ais/common/CommonComboSelectionHelper.java:485");
						identityvalue = null;
					}
				}

				boolean ada = false;
				for (Comboitem comboitem : list) {
					if ((value instanceof String)) {
						if (comboitem.getValue() != null
								&& value.toString().trim().equalsIgnoreCase(comboitem.getValue().toString().trim())) {
							combo.setSelectedItem(comboitem);
							ada = true;
							break;
						}
					} else if ((value instanceof Boolean) || (value instanceof String) || (value instanceof Integer)
							|| (value instanceof Long) || (value instanceof Double) || (value instanceof Date)) {
						if (comboitem.getValue() != null && value.equals(comboitem.getValue())) {
							combo.setSelectedItem(comboitem);
							ada = true;
							break;
						}
					} else if (identityvalue == null) {
						// FIX NPE berulang: identityvalue bisa null (mis. value adalah
						// GeneralValueObject baru/belum tersimpan tanpa id) -- tanpa guard
						// ini, identityvalue.equals(id) di bawah melempar NPE utk SETIAP
						// comboitem dlm daftar (bukan cuma satu kali). Tak ada identityvalue
						// = tak mungkin cocok dgn item manapun, lanjut ke item berikutnya.
						continue;
					} else {
						try {

							if (comboitem.getValue() != null && comboitem.getValue() instanceof Tbmuser) {
								Serializable id = ((Tbmuser) comboitem.getValue()).getUserId();
								if (identityvalue.equals(id)) {
									combo.setSelectedItem(comboitem);
									ada = true;
									break;
								}
							} else if (comboitem.getValue() != null && comboitem.getValue() instanceof Tbmrole) {
								Serializable id = ((Tbmrole) comboitem.getValue()).getRoleId();
								if (identityvalue.equals(id)) {
									combo.setSelectedItem(comboitem);
									ada = true;
									break;
								}
							} else if (comboitem.getValue() != null && comboitem.getValue() instanceof Program) {
								Serializable id = ((Program) comboitem.getValue()).getNama();
								if (identityvalue.equals(id)) {
									combo.setSelectedItem(comboitem);
									ada = true;
									break;
								}
							} else if (comboitem.getValue() != null && comboitem.getValue() instanceof GeneralValueObject) {
								Serializable id = ((GeneralValueObject) comboitem.getValue()).getId();
								if (identityvalue.equals(id)) {
									combo.setSelectedItem(comboitem);
									ada = true;
									break;
								}
							} else if (comboitem.getValue() != null) {
								// FIX NPE berulang (simetris dgn dua guard di atas): comboitem.getValue()
								// bisa null (mis. item "Semua"/"kosong" tanpa entitas terpilih yang
								// sengaja ditambahkan di awal daftar combobox) -- metadata.getIdentifier(null, ...)
								// memanggil getter id secara reflektif dgn target null -> NPE. Guard
								// comboitem.getValue() != null di sini menutup celah yang belum tertangani
								// oleh dua fix NPE berulang sebelumnya di method ini.
								Class mappedClass = metadata.getMappedClass(EntityMode.POJO);
								if (mappedClass != null && !mappedClass.isInstance(comboitem.getValue())) {
									continue;
								}
								Serializable id = metadata.getIdentifier(comboitem.getValue(), EntityMode.POJO);
								if (identityvalue.equals(id)) {
									combo.setSelectedItem(comboitem);
									ada = true;
									break;
								}
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboSelectionHelper.java:543");
	//						tampilErrorJikaAdmin(e);
						}
					}
				}

				if (!ada) {
					if (jikaDataTidakDitemukanDitambahkan && value != null) {
						if ((combo.getSelectedItem() == null || combo.getSelectedItem().getValue() == null)
								&& value != null) {

							if (value != null && value instanceof GeneralValueObject) {
								GeneralValueObject generalValueObject = (GeneralValueObject) value;
								Comboitem comboitem = new Comboitem(generalValueObject.getNama());
								comboitem.setValue(generalValueObject);
								comboitem.setDescription(generalValueObject.getKeterangan());
								combo.appendChild(comboitem);
								combo.setSelectedItem(comboitem);
							} else {
								Comboitem comboitem = new Comboitem(value.toString());
								comboitem.setValue(value);
								combo.appendChild(comboitem);
								combo.setSelectedItem(comboitem);
							}

						}
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
}
