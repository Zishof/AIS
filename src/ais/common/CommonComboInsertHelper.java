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
 * Helper terpusat untuk pengisian Combobox.
 * Dipisahkan dari Common agar method Common tetap pendek sebagai wrapper,
 * sementara seluruh logika query, sorting, dan pembentukan Comboitem dikelola di sini.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CommonComboInsertHelper {

	@SuppressWarnings({ "rawtypes", "unchecked" })
		public static void insertCombo(Combobox combobox, String property, Class clazz, Criterion... criterions) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				if (criterions != null) {
					for (Criterion criterion : criterions) {
						if (criterion != null)
							criteria.add(criterion);
					}
				}
				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:410");
					/* abaikan error sort */ }
				insertComboItems(combobox, property, list);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings({ "rawtypes", "unchecked" })
		public static void insertCombo(Combobox combobox, String property, Class<?> clazz, Criterion criterion) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				if (criterion != null)
					criteria.add(criterion);

				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:438");
				}
				insertComboItems(combobox, property, list);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings({ "rawtypes" })
		public static void insertCombo(Combobox combobox, String property, Class<?> clazz, Order order,
				Criterion criterion) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				if (order != null)
					criteria.addOrder(order);
				if (criterion != null)
					criteria.add(criterion);

				List list = ConstantValues.simpleList(criteria, clazz);
				insertComboItems(combobox, property, list);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings({ "rawtypes", "unchecked" })
		public static void insertCombo(Combobox combobox, String property, String deskripsi, Class clazz,
				Criterion... criterions) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				if (criterions != null) {
					for (Criterion criterion : criterions) {
						if (criterion != null)
							criteria.add(criterion);
					}
				}
				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:497");
				}
				insertComboItems(combobox, property, deskripsi, list);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings({ "unchecked", "rawtypes" })
		public static void insertCombo(Combobox combobox, String property, Class<?> clazz, Criteria criteria) {
			if (combobox == null || criteria == null)
				return;
			try {
				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:519");
				}
				insertComboItems(combobox, property, list);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

	@SuppressWarnings({ "unchecked", "rawtypes" })
		public static void insertCombo(Combobox combobox, String property, Class<?> clazz) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:538");
				}
				insertComboItems(combobox, property, list);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings({ "rawtypes", "unchecked" })
		public static void insertComboMyConfig(Combobox combobox, String property, Class<?> clazz,
				Criterion... criterions) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				if (criterions != null) {
					for (Criterion criterion : criterions) {
						if (criterion != null)
							criteria.add(criterion);
					}
				}
				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:570");
				}
				insertComboItemsMyConfig(combobox, property, list);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings({ "rawtypes", "unchecked" })
		public static void insertComboMyConfig(Combobox combobox, String property, Class<?> clazz) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:595");
				}
				insertComboItemsMyConfig(combobox, property, list);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings({ "unchecked", "rawtypes" })
		public static void insertComboDanSemua(Combobox combobox, String property, Class<?> clazz, Criteria criteria) {
			if (combobox == null || criteria == null)
				return;
			try {
				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:617");
				}
				list.add(null);
				insertComboItems(combobox, property, list);
				combobox.setReadonly(true);
				Common.selectComboItem(combobox, null);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

	@SuppressWarnings({ "unchecked", "rawtypes" })
		public static void insertComboDanSemua(Combobox combobox, String property, Class<?> clazz) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:639");
				}
				list.add(null);
				insertComboItems(combobox, property, list);
				combobox.setReadonly(true);
				Common.selectComboItem(combobox, null);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings({ "unchecked", "rawtypes" })
		public static void insertComboDanSemua(Combobox combobox, String property, String keterangan, Class<?> clazz) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:667");
				}
				list.add(null);
				insertComboItems(combobox, property, keterangan, list);
				combobox.setReadonly(true);
				Common.selectComboItem(combobox, null);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings({ "unchecked", "rawtypes" })
		public static void insertComboDanSemua(Combobox combobox, String property, String keterangan, Class<?> clazz,
				Criterion criterion) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				if (criterion != null)
					criteria.add(criterion);

				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:699");
				}
				list.add(null);
				insertComboItems(combobox, property, keterangan, list);
				combobox.setReadonly(true);
				Common.selectComboItem(combobox, null);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings({ "unchecked", "rawtypes" })
		public static void insertComboDanSemua(Combobox combobox, String property, Class<?> clazz, Criterion criterion) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				if (criterion != null)
					criteria.add(criterion);

				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:730");
				}
				list.add(null);
				insertComboItems(combobox, property, list);
				combobox.setReadonly(true);
				Common.selectComboItem(combobox, null);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings({ "unchecked", "rawtypes" })
		public static void insertCombo(Combobox combobox, String property, Class<?> clazz, String style) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:758");
				}
				insertComboItems(combobox, property, list, style);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings({ "unchecked", "rawtypes" })
		public static void insertCombo(Combobox combobox, String property, String deskripsi, Class<?> clazz) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:783");
				}
				insertComboItems(combobox, property, deskripsi, list);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings({ "rawtypes", "unchecked" })
		public static void insertCombo(Combobox combobox, String[] properties, Class clazz, Criterion... criterions) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				if (criterions != null) {
					for (Criterion criterion : criterions) {
						if (criterion != null)
							criteria.add(criterion);
					}
				}
				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:814");
				}
				insertComboItems(combobox, properties, list);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings({ "unchecked", "rawtypes" })
		public static void insertCombo(Combobox combobox, String[] properties, Class<?> clazz, Criterion criterion) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				if (criterion != null)
					criteria.add(criterion);
				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:841");
				}
				insertComboItems(combobox, properties, list);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings({ "rawtypes", "unchecked" })
		public static void insertCombo(Combobox combobox, String[] properties, String deskripsi, Class clazz,
				Criterion... criterions) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				if (criterions != null) {
					for (Criterion criterion : criterions) {
						if (criterion != null)
							criteria.add(criterion);
					}
				}
				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:873");
				}
				insertComboItems(combobox, properties, deskripsi, list);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings({ "rawtypes" })
		public static void insertComboDanSemua(Combobox combobox, String[] properties, String deskripsi, Class clazz,
				Criterion... criterions) {
			insertComboDanSemua(combobox, properties, deskripsi, clazz, "Semua", criterions);
		}

	@SuppressWarnings({ "rawtypes", "unchecked" })
		public static void insertComboDanSemua(Combobox combobox, String[] properties, String deskripsi, Class clazz,
				String labelTidakDipilih, Criterion... criterions) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				if (criterions != null) {
					for (Criterion criterion : criterions) {
						if (criterion != null)
							criteria.add(criterion);
					}
				}
				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:911");
				}
				list.add(null);
				insertComboItems(combobox, properties, deskripsi, list, labelTidakDipilih);
				combobox.setReadonly(true);
				Common.selectComboItem(combobox, null);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings({ "rawtypes", "unchecked" })
		public static void insertComboDanSemua(Combobox combobox, String[] properties, String deskripsi, Class clazz,
				String labelTidakDipilih, Criteria criteria) {
			if (combobox == null || criteria == null)
				return;
			try {
				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:937");
				}
				list.add(null);
				insertComboItems(combobox, properties, deskripsi, list, labelTidakDipilih);
				combobox.setReadonly(true);
				Common.selectComboItem(combobox, null);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

	@SuppressWarnings({ "unchecked", "rawtypes" })
		public static void insertCombo(Combobox combobox, String[] properties, Class<?> clazz) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:959");
				}
				insertComboItems(combobox, properties, list);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings({ "unchecked", "rawtypes" })
		public static void insertComboDanSemua(Combobox combobox, String[] properties, Class<?> clazz) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:984");
				}
				list.add(null);
				insertComboItems(combobox, properties, list);
				Common.selectComboItem(combobox, null);
				combobox.setReadonly(true);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings({ "unchecked", "rawtypes" })
		public static void insertCombo(Combobox combobox, String[] properties, String deskripsi, Class<?> clazz) {
			if (combobox == null)
				return;
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(clazz);
				List list = ConstantValues.simpleList(criteria, clazz);
				try {
					Collections.sort(list);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:1012");
				}
				insertComboItems(combobox, properties, deskripsi, list);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

	@SuppressWarnings("deprecation")
		public static void insertComboItems(Combobox combo, String property, List<?> items) {
			Common.clear(combo);
			if (items.size() == 0)
				return;
			if (items.get(0) == null) {
				Comboitem comboitem = new Comboitem();
				comboitem.setLabel("Semua");
				comboitem.setValue(null);
				combo.appendChild(comboitem);
			} else {
				Class<? extends Object> clazz = items.get(0).getClass();
				ClassMetadata metadata = null;
				if (!clazz.equals(CommonVO.class)) {
					metadata = HibernateUtil.getClassMetadata(clazz);
				}
				for (Object o : items) {
					Comboitem comboitem = new Comboitem();
					if (o == null) {
						comboitem.setLabel("Semua");
						comboitem.setValue(null);
					} else if (o instanceof CommonVO) {
						CommonVO oo = (CommonVO) o;
						comboitem.setLabel(oo.getName());
						comboitem.setValue(oo.getId());
					} else {
						// FIX NPE: property yg diminta bisa tidak ter-mapping Hibernate
						// utk clazz item ini (mis. list heterogen / nama properti salah
						// dari pemanggil), dan fallback metadata.getIdentifier(o,...)
						// sendiri bisa ikut NPE kalau item tsb tidak lengkap / metadata
						// null (lihat CommonComboInsertHelper.java lama baris 1058).
						// Verifikasi dulu properti benar2 ada di metadata (pola sama
						// dgn adaProperti() di bawah) sblm akses reflektif, dan bungkus
						// fallback identifier dgn try/catch terpisah; kalau keduanya
						// tetap gagal, skip item ini drpd combo crash -- tidak mengubah
						// hasil utk item yg datanya lengkap.
						boolean berhasil = false;
						if (metadata != null) {
							boolean propertiTerpetakan = property.equals("")
									|| adaProperti(metadata.getPropertyNames(), property);
							try {
								if (propertiTerpetakan) {
									comboitem.setLabel(property.equals("") ? o + ""
											: "" + metadata.getPropertyValue(o, property, EntityMode.POJO));
								} else {
									comboitem.setLabel("" + metadata.getIdentifier(o, EntityMode.POJO));
								}
								comboitem.setValue(o);
								berhasil = true;
							} catch (Exception e) {
								try {
									comboitem.setLabel("" + metadata.getIdentifier(o, EntityMode.POJO));
									comboitem.setValue(o);
									berhasil = true;
								} catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:1058");
								}
							}
						} else {
							comboitem.setLabel(o + "");
							comboitem.setValue(o);
							berhasil = true;
						}
						if (!berhasil) {
							continue;
						}
					}
					combo.appendChild(comboitem);
				}
			}
		}

	@SuppressWarnings("deprecation")
		public static void insertComboItemsMyConfig(Combobox combo, String property, List<?> items) {
			Common.clear(combo);
			if (items.size() == 0)
				return;
			if (items.get(0) == null) {
				MyComboitemConfig comboitem = new MyComboitemConfig();
				comboitem.setLabel("Semua");
				comboitem.setValue(null);
				combo.appendChild(comboitem);
			} else {
				Class<? extends Object> clazz = items.get(0).getClass();
				ClassMetadata metadata = null;
				if (!clazz.equals(CommonVO.class)) {
					metadata = HibernateUtil.getClassMetadata(clazz);
				}
				for (Object o : items) {
					MyComboitemConfig comboitem = new MyComboitemConfig();
					if (o == null) {
						comboitem.setLabel("Semua");
						comboitem.setValue(null);
					} else if (o instanceof CommonVO) {
						CommonVO oo = (CommonVO) o;
						comboitem.setLabel(oo.getName());
						comboitem.setValue(oo.getId());
					} else {
						try {

							comboitem.setLabel(property.equals("") ? o + ""
									: "" + metadata.getPropertyValue(o, property, EntityMode.POJO));
						} catch (Exception e) {
							comboitem.setLabel(
									property.equals("") ? o + "" : "" + metadata.getIdentifier(o, EntityMode.POJO));
						}
						comboitem.setValue(o);
					}
					combo.appendChild(comboitem);
				}
			}
		}

	public static void insertComboItemsCommonVO(Combobox combo, List<?> items) {
			if (combo == null) {
				return;
			}
			Common.clear(combo);
			for (Object o : items) {
				Comboitem comboitem = new Comboitem();
				if (o instanceof CommonVO) {
					CommonVO oo = (CommonVO) o;
					comboitem.setLabel(oo.getName());
					comboitem.setDescription(oo.getName3());
					comboitem.setValue(oo);
				}
				combo.appendChild(comboitem);
			}
		}

	@SuppressWarnings("deprecation")
		public static void insertComboItems(Combobox combo, String property, List<?> items, String style) {
			Common.clear(combo);
			if (items.size() == 0)
				return;
			if (items.get(0) == null) {
				Comboitem comboitem = new Comboitem();
				comboitem.setLabel("Semua");
				comboitem.setValue(null);
				combo.appendChild(comboitem);
			} else {
				Class<? extends Object> clazz = items.get(0).getClass();
				ClassMetadata metadata = null;
				if (!clazz.equals(CommonVO.class)) {
					metadata = HibernateUtil.getClassMetadata(clazz);
				}
				for (Object o : items) {
					Comboitem comboitem = new Comboitem();
					if (o == null) {
						comboitem.setLabel("Semua");
						comboitem.setValue(null);
					} else if (o instanceof CommonVO) {
						CommonVO oo = (CommonVO) o;
						comboitem.setLabel(oo.getName());
						comboitem.setValue(oo.getId());
					} else {
						String label = "";
						try {
							label = (property.equals("") ? o + ""
									: "" + metadata.getPropertyValue(o, property, EntityMode.POJO));
						} catch (Exception e) {
							label = (property.equals("") ? o + "" : "" + metadata.getIdentifier(o, EntityMode.POJO));
						}
						comboitem.setLabel(label);
						comboitem.setContent("<div style=\"font-size: x-large;\">" + label + "</div>");
						comboitem.setValue(o);
					}
					combo.appendChild(comboitem);
				}
			}
		}

	public static void insertComboItems(Combobox combo, String property, String deskripsi, List<?> items) {
			if (combo == null) {
				return;
			}
			Common.clear(combo);
			if (items.size() == 0)
				return;
			if (items.get(0) == null) {
				Comboitem comboitem = new Comboitem();
				comboitem.setLabel("Semua");
				comboitem.setValue(null);
				combo.appendChild(comboitem);
			} else {
				Class<? extends Object> clazz = items.get(0).getClass();
				ClassMetadata metadata = null;
				if (!clazz.equals(CommonVO.class)) {
					metadata = HibernateUtil.getClassMetadata(clazz);
				}
				for (Object o : items) {
					Comboitem comboitem = new Comboitem();
					if (o == null) {
						comboitem.setLabel("Semua");
						comboitem.setValue(null);
					} else if (o instanceof CommonVO) {
						CommonVO oo = (CommonVO) o;
						comboitem.setLabel(oo.getName());
						comboitem.setDescription(oo.getName1());
						comboitem.setValue(oo.getId());
					} else {

						try {
							Object myproperty = metadata.getPropertyValue(o, property, EntityMode.POJO);
							Object mydeskripsi = metadata.getPropertyValue(o, deskripsi, EntityMode.POJO);

							comboitem.setLabel(property.equals("") ? (o == null ? "" : "") + ""
									: "" + (myproperty == null ? "" : myproperty));
							// Jangan memanggil toString() entity/proxy saat deskripsi tidak diminta.
							// Proxy Akun yang sudah detached akan mencoba lazy-load dari session tertutup.
							comboitem.setDescription(deskripsi.equals("") ? ""
									: "" + (mydeskripsi == null ? "" : mydeskripsi));
							comboitem.setValue(o);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:1209");

						}
					}
					combo.appendChild(comboitem);
				}
			}
		}

	@SuppressWarnings("deprecation")
		public static void insertComboItems(Combobox combo, String properties[], List<?> items) {
			Common.clear(combo);
			if (items.size() == 0)
				return;
			if (items.get(0) == null) {
				Comboitem comboitem = new Comboitem();
				comboitem.setLabel("Semua");
				comboitem.setValue(null);
				combo.appendChild(comboitem);
			} else {
				Class<? extends Object> clazz = items.get(0).getClass();
				ClassMetadata metadata = null;
				if (!clazz.equals(CommonVO.class)) {
					// FIX NPE: getClassMetadata bisa null (mis. clazz adalah proxy
					// Hibernate CGLIB yg tak terdaftar persis di metadata registry) atau
					// bisa saja melempar exception pada implementasi tertentu -- bungkus
					// defensif drpd langsung crash sebelum sempat masuk loop item.
					try {
						metadata = HibernateUtil.getClassMetadata(clazz);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:1246-metadata");
					}
				}
				for (Object o : items) {
					Comboitem comboitem = new Comboitem();
					if (o == null) {
						comboitem.setLabel("Semua");
						comboitem.setValue(null);
					} else if (o instanceof CommonVO) {
						CommonVO oo = (CommonVO) o;
						comboitem.setLabel(oo.getName());
						comboitem.setValue(oo.getId());
					} else {
						// FIX NPE: metadata bisa null (lihat catatan di atas) dan properti
						// individual bisa tidak ter-mapping utk item ini (list heterogen /
						// data lama) -- skip properti/item bermasalah drpd menggagalkan
						// seluruh pembuatan combo (bandingkan pola berhasil/adaProperti pd
						// insertComboItems(combo, property, items) di atas).
						String value = "";
						boolean adaNilai = false;

						if (metadata != null) {
							for (String property : properties) {
								if (property == null) {
									continue;
								}
								try {
									boolean propertiTerpetakan = property.trim().equals("id")
											|| adaProperti(metadata.getPropertyNames(), property);
									if (!propertiTerpetakan) {
										continue;
									}
									Object val = property.trim().equals("id") ? metadata.getIdentifier(o, EntityMode.POJO)
											: metadata.getPropertyValue(o, property, EntityMode.POJO);
									if (val == null || val.toString().trim().equals("")
											|| val.toString().trim().equals("null")) {
										continue;
									}

									value += value.equals("") ? val : " - " + val;
									adaNilai = true;
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:1246-property");
								}
							}
						}

						comboitem.setLabel(adaNilai ? value : o + "");
						comboitem.setValue(o);
					}
					combo.appendChild(comboitem);
				}
			}
		}

	public static void insertComboItems(Combobox combo, String properties[], String deskripsi, List<?> items) {
			insertComboItems(combo, properties, deskripsi, items, "Semua");
		}

	@SuppressWarnings("deprecation")
		public static void insertComboItems(Combobox combo, String properties[], String deskripsi, List<?> items,
				String labelTidakDipilih) {

			if (combo == null) {
				return;
			}

			Common.clear(combo);
			if (items.size() == 0)
				return;
			if (items.get(0) == null) {
				Comboitem comboitem = new Comboitem();
				comboitem.setLabel(labelTidakDipilih);
				comboitem.setValue(null);
				combo.appendChild(comboitem);
			} else {
				Class<? extends Object> clazz = items.get(0).getClass();
				ClassMetadata metadata = null;
				if (!clazz.equals(CommonVO.class)) {
					// FIX NPE: getClassMetadata bisa null (mis. clazz proxy Hibernate
					// CGLIB yg tak terdaftar persis di metadata registry, atau entity
					// legacy yg belum/tak lagi ter-mapping) -- kalau dibiarkan null
					// tanpa guard, reflective getIdentifier/getPropertyValue di bawah
					// (dipanggil dari LaporanPengajuan via Common.insertCombo utk combo
					// Jenis Pengajuan) melempar NPE. Bungkus akuisisi metadata + cek
					// eksplisit metadata!=null sblm dipakai, drpd mengandalkan
					// try/catch semata -- item bermasalah cukup di-skip labelnya,
					// tidak menggagalkan seluruh combo/laporan.
					try {
						metadata = HibernateUtil.getClassMetadata(clazz);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:1341-metadata");
					}
				}
				for (Object o : items) {
					Comboitem comboitem = new Comboitem();
					comboitem.setValue(o);
					if (o == null) {
						comboitem.setLabel(labelTidakDipilih);
						comboitem.setValue(null);
					} else if (o instanceof CommonVO) {
						CommonVO oo = (CommonVO) o;
						comboitem.setLabel(oo.getName());
						comboitem.setDescription(oo.getName1());
						comboitem.setValue(oo.getId());
					} else {

						try {
							String value = "";

							for (String property : properties) {

								if (property == null || metadata == null) {
									continue;
								}
								try {
									boolean propertiTerpetakan = property.trim().equals("id")
											|| adaProperti(metadata.getPropertyNames(), property);
									if (!propertiTerpetakan) {
										continue;
									}
									Object val = property.trim().equals("id") ? metadata.getIdentifier(o, EntityMode.POJO)
											: metadata.getPropertyValue(o, property, EntityMode.POJO);
									if (val == null || val.toString().trim().equals("")) {
										continue;
									}

									value += value.equals("") ? val : " - " + val;
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:1317");
	//								Common.tampilErrorJikaAdmin(e);
								}
							}

							if (value.equals("") && metadata == null) {
								// metadata tak tersedia sama sekali utk clazz ini (unmapped) --
								// fallback ke toString() spy label tidak kosong, drpd combo
								// item tanpa label yg membingungkan user.
								value = o + "";
							}

							comboitem.setLabel(value);

							// FIX NPE: sebelum ini deskripsi langsung dipassing ke
							// metadata.getPropertyValue() walau properti tsb TIDAK
							// ter-mapping Hibernate utk clazz ini (mis. caller
							// copy-paste nama properti dari entity lain) -- reflective
							// getter Hibernate melempar NPE/exception yg tak konsisten
							// tertangkap catch di bawah pada sebagian versi deploy.
							// Verifikasi dulu properti benar2 ada di metadata sblm
							// akses reflektif; kalau tidak ada, treat spt deskripsi
							// kosong (fallback toString()) -- tidak mengubah hasil utk
							// caller yg sudah benar.
							boolean deskripsiTerpetakan = !deskripsi.equals("") && metadata != null
									&& adaProperti(metadata.getPropertyNames(), deskripsi);
							if (!deskripsiTerpetakan) {
								comboitem.setDescription(o + "");
							} else {
								Object des = metadata.getPropertyValue(o, deskripsi, EntityMode.POJO);
								comboitem.setDescription(des == null ? "" : des.toString());
							}

						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonComboInsertHelper.java:1327");
	//						Common.tampilErrorJikaAdmin(e);
						}
					}
					combo.appendChild(comboitem);
				}
			}
		}

	// Cek apakah nama properti ada di daftar properti ter-mapping Hibernate (dipakai
	// insertComboItems utk guard NPE deskripsi yg tak ter-mapping, lihat catatan di atas).
	private static boolean adaProperti(String[] namaProperti, String target) {
		if (namaProperti == null || target == null) {
			return false;
		}
		for (String p : namaProperti) {
			if (target.equals(p)) {
				return true;
			}
		}
		return false;
	}
}
