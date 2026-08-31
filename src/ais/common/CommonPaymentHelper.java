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
 * Helper generik lintas payment gateway/lintas modul untuk logika pembayaran mahasiswa di AIS —
 * bagian dari hierarki {@link Common} (di-extend, bukan dikomposisi, mengikuti pola pewarisan
 * "helper besar" yang dipakai beberapa kelas utilitas AIS lain) yang khusus menangani DUA area
 * besar: (1) <b>pengecekan status pembayaran</b> sebagai syarat berbagai aksi akademik (isi KRS,
 * persetujuan KRS, pengajuan proposal/sidang skripsi, wisuda, penilaian), dan (2) <b>pencatatan
 * cicilan pembayaran</b> (baik untuk mahasiswa aktif lewat {@link Kegiatan} maupun pendaftar/calon
 * mahasiswa lewat {@link KegiatanTemporary}) beserta rekonsiliasi Host-to-Host (H2H) dengan bank.
 * Kelas ini TIDAK memanggil satu payment gateway spesifik pun secara langsung (bandingkan dengan
 * {@link CimbCommon}/{@link OttoUtil} yang masing-masing mengintegrasikan satu gateway) — perannya
 * murni sebagai logika bisnis "apakah mahasiswa sudah cukup bayar" dan "bagaimana mencatat
 * cicilan", tidak bergantung pada gateway pembayaran mana pun yang dipakai untuk membayar.
 *
 * <h2>Area 1 — Pengecekan status pembayaran (gerbang syarat aksi akademik)</h2>
 * <p>
 * Method {@code checkStatusPembayaran*} dipanggil sebagai GERBANG sebelum mahasiswa diizinkan
 * melakukan aksi akademik tertentu. Polanya konsisten: (a) periksa pengecualian eksplisit lebih
 * dulu — baik lewat konfigurasi {@code checkApakahMahasiswaBolehAmbilKrsLewatPengecualian}, maupun
 * lewat mekanisme "bypass" terpusat {@link #checkBaypassStatusPembayaranMahasiswa} (baris {@link
 * BaypassPembayaranMahasiswa} yang berlaku untuk kombinasi mahasiswa+jenis kegiatan+semester+
 * rentang tanggal tertentu, mis. pembebasan biaya untuk kasus khusus); (b) bila saklar konfigurasi
 * terkait dimatikan (mis. {@code mahasiswa_harus_bayar_sebelum_isi_krs}), gerbang otomatis
 * diloloskan; (c) bila tidak ada pengecualian, hitung apakah mahasiswa sudah membayar minimal
 * ambang tertentu (mis. {@code hitungPersentaseLunasAktual() >= 0.1}, atau memiliki cicilan untuk
 * kode item biaya syarat tertentu yang dikonfigurasi per semester/angkatan/jurusan/program/status
 * awal mahasiswa lewat {@code Common.getKonfigurasi(...)} dengan variasi parameter). Method-method
 * ini secara historis besar dan bercabang banyak karena mengakumulasi aturan bisnis bertahun-tahun
 * (mis. penanganan khusus mahasiswa semester 1, mahasiswa pindahan, konversi ke data
 * {@link BiodataCalonMahasiswa} untuk mahasiswa baru yang belum lengkap datanya di
 * {@link Kegiatan}) — sebagian besar hanya mendelegasikan ke {@link CommonHelperClass} sebagai
 * implementasi kanonik sesungguhnya (mis. {@code checkStatusPembayaranMahasiswaSebelumnya},
 * {@code checkStatusPembayaranKegiatanMahasiswa}, dan seluruh varian {@code ...PengajuanSkripsi}/
 * {@code ...PengajuanSidang}/{@code ...PengajuanWisuda}), sedangkan
 * {@link #checkStatusPembayaranMahasiswa} sendiri berisi logika penuh di kelas ini.
 * </p>
 *
 * <h2>Area 2 — Pencatatan cicilan pembayaran</h2>
 * <p>
 * Method {@code simpanCicilan*}/{@code ambilCicilan*}/{@code copyCicilanPembayaranKe*} menangani
 * siklus hidup baris {@link CicilanPembayaran} (cicilan sukses) dan {@link CicilanPembayaranGagal}
 * (cicilan gagal, mis. hasil rekonsiliasi Host-to-Host yang tidak cocok): {@link
 * #simpanCicilanTanpaMencicil} membuat satu ATAU BANYAK baris cicilan sekaligus (satu per
 * {@link DetailBiaya}/{@link PengaturanPembayaranBulanan} dalam koleksi yang diberikan, atau satu
 * baris "generik" tanpa rincian item bila koleksi kosong/{@code null}) tanpa proses cicil-mencicil
 * bertahap (nilai dan tanggal validasi ditentukan langsung oleh pemanggil, bukan dihitung ulang);
 * {@link #ambilCicilanPembayarans}/{@link #ambilCicilanPembayaranGagals} mencari baris cicilan yang
 * cocok dengan satu baris {@link LogHostToHost} (parsing kolom {@code item} berformat
 * {@code kodeItem\<pemisah>\<...>\namaBulan} dipisah {@code |}, dicocokkan berdasarkan kode item
 * biaya + bulan + (nomor registrasi/nomor ujian calon mahasiswa ATAU NIM mahasiswa) + tanggal); dan
 * {@link #copyCicilanPembayaranKeGagal}/{@link #copyCicilanPembayaranKeSukses} adalah konversi dua
 * arah antara kedua entitas tersebut (dipakai saat rekonsiliasi H2H mengoreksi status sukses/gagal
 * suatu transaksi setelah pencatatan awal).
 * </p>
 * <p>
 * {@link #initCicilan} adalah satu-satunya method di kelas ini yang membangun komponen UI ZKoss
 * (baris {@link Hbox}/{@link Vbox}/{@link Label} untuk satu baris cicilan pada grid ZKoss, lengkap
 * dengan area unggah bukti pembayaran lewat {@link LampiranLain#createDownloadUploadFileLain});
 * {@link #filterCicilanPembayaran} melakukan deduplikasi daftar cicilan berdasarkan kunci komposit
 * yang berbeda tergantung apakah cicilan terkait pembayaran bulanan dengan nilai variabel/tetap
 * atau pembayaran biaya biasa.
 * </p>
 *
 * <h2>Catatan keamanan — konstruksi SQL native via string concatenation</h2>
 * <p>
 * Beberapa query di kelas ini memakai {@link Restrictions#sqlRestriction(String)} dengan nilai yang
 * disisipkan lewat concatenation string langsung ke dalam SQL, alih-alih parameter terikat
 * (bind parameter). Contoh: {@link #checkStatusPembayaranMahasiswa} menyisipkan
 * {@code mahasiswa.getNim().trim()} langsung ke klausa {@code WHERE} SQL native saat mencari
 * {@link BiodataCalonMahasiswa} yang cocok, dan beberapa method lain menyisipkan tanggal terformat
 * ke {@code sqlRestriction}. NIM berasal dari data mahasiswa yang sudah tervalidasi di database
 * (bukan input request langsung), sehingga risiko praktisnya rendah pada jalur ini, namun pola
 * penyisipan string mentah ke SQL tetap merupakan praktik yang rawan bila suatu saat nilai yang
 * disisipkan berasal dari input pengguna yang belum divalidasi. Sesuai lingkup pekerjaan
 * dokumentasi ini, pola query tersebut TIDAK diubah di sini — dicatat sebagai catatan tinjauan
 * keamanan untuk perbaikan terpisah bila diperlukan (migrasi ke parameter terikat/Criteria API
 * murni).
 * </p>
 */
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class CommonPaymentHelper extends Common {


	private static final Logger log = Logger.getLogger(CommonPaymentHelper.class);
	private static final String COOKIE_PMB_BIODATA = "biodataCalonMahasiswa";
	private static final String COOKIE_PMB_USERID = "userid";

	/** Mengembalikan {@code value} setelah di-trim, atau string kosong bila {@code value} bernilai {@code null}. */
	private static String safeTrim(String value) {
		return value == null ? "" : value.trim();
	}

	/** Mengecek apakah {@code value} bernilai {@code null} atau kosong setelah di-trim. */
	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}

	/** Memastikan {@code directory} ada sebagai direktori, membuatnya (beserta direktori antara) bila belum ada. */
	private static boolean ensureDirectory(File directory) {
		if (directory == null) {
			return false;
		}
		if (directory.exists()) {
			return directory.isDirectory();
		}
		return directory.mkdirs();
	}

	/** Menampilkan pesan galat CRUD ke admin (lewat {@link Common#tampilErrorJikaAdmin}) dan sebagai alert ke pengguna ({@code pesan} plus detail {@code e.getMessage()} bila ada), dengan kegagalan menampilkan alert itu sendiri diredam. */
	private static void tampilCrudError(Exception e, String pesan) {
		Common.tampilErrorJikaAdmin(e);
		String detail = e == null || e.getMessage() == null ? "" : "\n" + e.getMessage();
		try {
			MyMessageboxConfig.show(pesan + detail);
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonPaymentHelper.java:461");
		}
	}




	/**
	 * Seperti {@link #checkBaypassStatusPembayaranMahasiswa(Integer, Integer, Mahasiswa, Collection)},
	 * untuk SATU {@link JenisKegiatan}. Sebelum mendelegasikan, melakukan pengecekan cepat tambahan:
	 * bila {@code jenisKegiatan} dan {@code semester} sama-sama diberikan dan semester berada DI LUAR
	 * rentang {@code minSmt}/{@code maxSmt} milik {@code jenisKegiatan}, method langsung mengembalikan
	 * {@code true} (dianggap bypass berlaku) tanpa query database — logikanya: bila jenis kegiatan
	 * tersebut memang tidak berlaku untuk semester mahasiswa saat ini, tidak relevan untuk menagih
	 * syarat pembayaran terkait kegiatan tersebut sama sekali.
	 *
	 * @param semester      semester berjalan mahasiswa, boleh {@code null}
	 * @param tahap         tahap pembayaran (0/{@code null} berarti tidak membatasi berdasarkan
	 *                      tahap, hanya berdasarkan semester)
	 * @param mahasiswa     mahasiswa yang diperiksa
	 * @param jenisKegiatan jenis kegiatan terkait syarat pembayaran, boleh {@code null}
	 * @return {@code true} bila mahasiswa memiliki pengecualian bypass yang berlaku (baik dari
	 *         pengecekan rentang semester cepat di atas, maupun dari baris
	 *         {@link BaypassPembayaranMahasiswa} yang cocok); {@code false} bila tidak ada bypass
	 */
	public static boolean checkBaypassStatusPembayaranMahasiswa(Integer semester, Integer tahap, Mahasiswa mahasiswa,
			JenisKegiatan jenisKegiatan) {

		if (jenisKegiatan != null && semester != null) {
			if (jenisKegiatan.getMinSmt() > semester || jenisKegiatan.getMaxSmt() < semester) {
				return true;
			}
		}

		List<JenisKegiatan> jenisKegiatans = new ArrayList<JenisKegiatan>();
		if (jenisKegiatan != null) {
			jenisKegiatans.add(jenisKegiatan);
		}
		return checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, jenisKegiatans);
	}



	/**
	 * Implementasi kanonik pengecekan bypass syarat pembayaran: mencari baris
	 * {@link BaypassPembayaranMahasiswa} yang cocok dengan {@code mahasiswa}, salah satu dari
	 * {@code jenisKegiatans} (atau berlaku untuk SEMUA jenis kegiatan bila kolom
	 * {@code jenisKegiatan} pada baris bypass bernilai {@code null}), kombinasi
	 * semester/tahap (dilonggarkan menjadi "selalu cocok" bila {@code tahap} bernilai
	 * {@code null}/{@code 0}), DAN berada dalam rentang tanggal berlaku (
	 * {@code berlaku_mulai}/{@code berlaku_sampai}, boleh {@code null} berarti tidak terbatas pada
	 * ujung tersebut). Perbandingan tanggal SENGAJA dibungkus fungsi SQL {@code DATE(...)} pada kedua
	 * sisi (bukan perbandingan timestamp mentah) — dijelaskan pada komentar inline di badan method —
	 * agar pengecualian tetap aktif sepanjang HARI mulai/sampai berapa pun komponen jamnya, termasuk
	 * data impor lama yang jam-nya kebetulan bukan tengah malam.
	 *
	 * <p>
	 * Dua jenis kegagalan koneksi/protokol JDBC tertentu ({@link java.util.NoSuchElementException}
	 * yang mengindikasikan korupsi state protokol JDBC, dan {@link IllegalStateException} dengan
	 * pesan resultset tanpa struktur field) ditangani secara KHUSUS: keduanya dianggap sebagai
	 * "tidak ada bypass" (hitungan diperlakukan sebagai 0) alih-alih dilempar sebagai galat ke
	 * pemanggil — pola defensif untuk mencegah kegagalan koneksi database yang jarang terjadi
	 * menghalangi mahasiswa yang seharusnya tidak diblokir bypass sama sekali.
	 * </p>
	 *
	 * @param semester     semester berjalan mahasiswa, boleh {@code null}
	 * @param tahap        tahap pembayaran, {@code null}/{@code 0} berarti tidak membatasi tahap
	 * @param mahasiswa    mahasiswa yang diperiksa
	 * @param jenisKegiatans kumpulan jenis kegiatan yang relevan; {@code null}/kosong berarti tidak
	 *                       membatasi berdasarkan jenis kegiatan (cocok dengan bypass jenis apa pun)
	 * @return {@code true} bila ditemukan minimal satu baris {@link BaypassPembayaranMahasiswa} yang
	 *         cocok dan sedang berlaku; {@code false} bila tidak ada, atau bila terjadi kegagalan
	 *         koneksi JDBC tertentu yang ditangani sebagai "tidak ada bypass"
	 */
	public static boolean checkBaypassStatusPembayaranMahasiswa(Integer semester, Integer tahap, Mahasiswa mahasiswa,
			Collection<JenisKegiatan> jenisKegiatans) {

		Session session = HibernateUtil.currentNativeSession();

		Criterion criterionSemester = tahap == null || tahap.equals(0) ? Restrictions.eq("semester", semester)
				: Restrictions.sqlRestriction("true");

		Criterion criterionTahapan = tahap == null || tahap.equals(0) ? Restrictions.sqlRestriction("true")
				: Restrictions.eq("tahap", tahap);

		Date now = WaktuUtil.getDate();

		int count = 0;
		try {
			count = ((Number) session.createCriteria(BaypassPembayaranMahasiswa.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa))
					// Bandingkan HANYA bagian TANGGAL (bungkus kolom dgn DATE()) supaya pengecualian AKTIF PADA
					// hari mulai (berlaku_mulai) apa pun komponen JAM tersimpan. Tanpa DATE() pada kolom, bila
					// berlaku_mulai kebetulan tersimpan sbg timestamp berjam >00:00 (mis. impor lama), maka
					// "berlaku_mulai <= DATE(now)" bernilai FALSE di hari mulai → mahasiswa gagal cetak di hari
					// pertama pengecualian. DATE(kolom) menjamin inklusif di kedua ujung (mulai & sampai).
					.add(Restrictions.sqlRestriction("(berlaku_mulai is null or DATE(berlaku_mulai) <= DATE('"
							+ Common.databaseDateFormat.get().format(now)
							+ "'))  and  (berlaku_sampai is null or DATE(berlaku_sampai) >= DATE('"
							+ Common.databaseDateFormat.get().format(now) + "'))"))
					.add(jenisKegiatans == null || jenisKegiatans.isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.in("jenisKegiatan", jenisKegiatans),
									Restrictions.isNull("jenisKegiatan")))
					.add(criterionSemester).add(criterionTahapan).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();
		} catch (java.util.NoSuchElementException nse) {
			// JDBC protocol state corruption (ArrayDeque kosong) — asumsikan tidak ada bypass
			count = 0;
		} catch (IllegalStateException ise) {
			// "Received resultset tuples, but no field structure" — koneksi korup
			count = 0;
		}

		// System.out.println("checkBaypassStatusPembayaranMahasiswa ->
		// jenisKegiatan => " + jenisKegiatans + ", count => "
		// + count + ", mahasiswa => " + mahasiswa + ", semester => " +
		// semester);

		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
		return count != 0;

	}



	/** Seperti {@link #checkStatusPembayaranMahasiswa(Integer, Integer, Mahasiswa, boolean, boolean, boolean)} dengan {@code check=true} (saklar konfigurasi gerbang pembayaran selalu diperiksa, tidak dilewati). */
	public static boolean checkStatusPembayaranMahasiswa(Integer semester, Integer tahap, Mahasiswa mahasiswa,
			boolean persetujuan, boolean sp) {
		return checkStatusPembayaranMahasiswa(semester, tahap, mahasiswa, true, persetujuan, sp);
	}



	/**
	 * Implementasi kanonik gerbang syarat pembayaran sebelum mahasiswa boleh mengisi/menyetujui KRS
	 * (Kartu Rencana Studi) reguler atau semester pendek (SP). Urutan pemeriksaan:
	 * <ol>
	 * <li>Bila mahasiswa memenuhi pengecualian eksplisit lewat
	 * {@code Common.checkApakahMahasiswaBolehAmbilKrsLewatPengecualian}, langsung {@code true}.</li>
	 * <li>Semester {@code null}/{@code <= 0}, atau semester 1 dan konfigurasi
	 * {@code mahasiswa_baru_mengikuti_persyaratan_krs_spt_mahasiswa} tidak aktif, langsung
	 * {@code true} (mahasiswa baru tidak dikenai syarat pembayaran KRS).</li>
	 * <li>Untuk jalur SEMESTER PENDEK ({@code sp=true}): bila {@code check} dan saklar
	 * {@code mahasiswa_harus_bayar_sebelum_isi_krs_sp} tidak aktif, langsung {@code true}. Jika
	 * tidak, kode item biaya syarat (dikonfigurasi per semester/angkatan/jurusan/program/status awal
	 * mahasiswa lewat {@code kode_item_biaya_mahasiswa_harus_bayar_sebelum_isi_krs_sp}) diperiksa:
	 * bila kosong, langsung {@code true}; jika ada, hasil ditentukan dari jumlah cicilan yang sudah
	 * dibayar untuk kode tersebut, dengan bypass ({@link #checkBaypassStatusPembayaranMahasiswa})
	 * sebagai jalan pintas tambahan bila belum lunas.</li>
	 * <li>Untuk jalur KRS REGULER ({@code sp=false}): bila {@code check} dan saklar terkait
	 * ({@code mahasiswa_harus_bayar_sebelum_persetujuan_krs} atau
	 * {@code mahasiswa_harus_bayar_sebelum_isi_krs}, dipilih berdasarkan {@code persetujuan}) tidak
	 * aktif, langsung {@code true}. Untuk pengisian awal (bukan persetujuan): total tagihan syarat
	 * KRS dihitung lewat {@code hitungTagihanMahasiswaSebagaiSyaratKrs} — bila kurang dari
	 * {@code 0.01} (dianggap lunas/tidak ada tagihan), langsung {@code true}; jika tidak, kode item
	 * biaya syarat diperiksa satu per satu (dipisah {@code ;}) dengan logika cicilan+bypass yang
	 * sama seperti jalur SP.</li>
	 * <li>Terakhir, bila belum ada keputusan dari langkah di atas, method memeriksa daftar
	 * {@link Kegiatan} yang sudah dibayar mahasiswa untuk jenis-jenis kegiatan KRS
	 * ({@link CommonHelperClass#jenisKegiatansUntukKrs}, dimuat ulang sekali lewat
	 * {@code reloadJenisKegiatans()} bila belum ada) — dengan penanganan KHUSUS untuk mahasiswa yang
	 * baru pindah masuk kampus ini (semester 1, atau semester sama dengan/tepat setelah
	 * {@code pindahKeKampusIniMasukSemester}) yang belum punya {@link Kegiatan}: kegiatan yang sudah
	 * dibayar dicari dari data {@link BiodataCalonMahasiswa} yang NIM-nya cocok (dicari lewat SQL
	 * native — lihat catatan keamanan pada javadoc kelas {@link CommonPaymentHelper}), digabung
	 * dengan jenis kegiatan "Pendaftaran Ulang Mahasiswa Baru". Hasil akhir bernilai {@code true}
	 * bila ADA minimal satu {@link Kegiatan} dengan persentase lunas aktual {@code >= 0.1} (10%).</li>
	 * </ol>
	 *
	 * @param semester    semester berjalan yang akan diisi/disetujui KRS-nya
	 * @param tahap       tahap pembayaran, diteruskan ke {@link #checkBaypassStatusPembayaranMahasiswa}
	 * @param mahasiswa   mahasiswa yang diperiksa
	 * @param check       bila {@code false}, saklar konfigurasi gerbang ({@code
	 *                    mahasiswa_harus_bayar_sebelum_...}) DILEWATI sepenuhnya (pemeriksaan
	 *                    tagihan/cicilan tetap dijalankan) — dipakai saat pemanggil ingin hasil
	 *                    "apakah sudah bayar" murni tanpa peduli status saklar fitur
	 * @param persetujuan bila {@code true}, dianggap sebagai gerbang PERSETUJUAN KRS (bukan
	 *                    pengisian awal) — memilih kunci konfigurasi berbeda dan melewati pengecekan
	 *                    tagihan minimum
	 * @param sp          bila {@code true}, dianggap sebagai gerbang KRS SEMESTER PENDEK (jalur kode
	 *                    item biaya dan konfigurasi yang berbeda dari KRS reguler)
	 * @return {@code true} bila mahasiswa boleh melanjutkan aksi (syarat terpenuhi, ada
	 *         pengecualian/bypass, atau saklar terkait tidak aktif); {@code false} bila syarat
	 *         pembayaran belum terpenuhi
	 */
	public static boolean checkStatusPembayaranMahasiswa(Integer semester, Integer tahap, Mahasiswa mahasiswa,
			boolean check, boolean persetujuan, boolean sp) {

		Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
		Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
		Integer tahunAkademikMulai = Common.getTahunAkademik(semester, tahunAngkatanMhs, semesterMulai,
				mahasiswa.getSemesterMulai());

		String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
		if (Common.checkApakahMahasiswaBolehAmbilKrsLewatPengecualian(mahasiswa, tahunAkademik,
				semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL)) {
			return true;
		}

		boolean mahasiswabaruMengikutipersyaratanKrsSptMahasiswa = Common.bolehKonfigurasi("mahasiswa_baru_mengikuti_persyaratan_krs_spt_mahasiswa", Konfigurasi.TIDAK_AKTIF);

		if (semester == null || (!mahasiswabaruMengikutipersyaratanKrsSptMahasiswa && semester.intValue() == 1)
				|| semester.intValue() <= 0) {
			return true;
		}

		if (sp) {

			if (check) {
				if (!Common.bolehKonfigurasi("mahasiswa_harus_bayar_sebelum_isi_krs_sp")) {
					return true;
				}
			}

			String kodeItemBiaya = Common.getKonfigurasi("kode_item_biaya_mahasiswa_harus_bayar_sebelum_isi_krs_sp", "",
					semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(), mahasiswa.getProgram(),
					mahasiswa.getStatusAwalMahasiswa()).getNilai();
			if (kodeItemBiaya.trim().isEmpty()) {
				return true;
			}

			int kegiatan = mahasiswa.ambilJumlahCicilanPembayaran(kodeItemBiaya, semester);
			boolean hasil = kegiatan > 0;

			if (!hasil) {
				Collection<JenisKegiatan> jenisKegiatan = mahasiswa.ambilJenisKegiatans(semester, kodeItemBiaya);
				if (Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, jenisKegiatan)) {
					return true;
				}
			}

			return hasil;

		} else {

			if (check) {
				if (!Common
						.getKonfigurasi(persetujuan ? "mahasiswa_harus_bayar_sebelum_persetujuan_krs"
								: "mahasiswa_harus_bayar_sebelum_isi_krs", Konfigurasi.AKTIF)
						.getNilai().equals(Konfigurasi.AKTIF)) {
					return true;
				}
			}

			if (!persetujuan) {
				Session session = null;
				try {
					session = HibernateUtil.currentNativeSession();
					Double tagihanSyaratKrs = hitungTagihanMahasiswaSebagaiSyaratKrs(session, mahasiswa, semester);
					if (tagihanSyaratKrs < 0.01) {
						return true;
					}
				} finally {
					HibernateUtil.closeSession();
				}

				String kodeItemBiaya = Common.getKonfigurasi("kode_item_biaya_mahasiswa_harus_bayar_sebelum_isi_krs",
						"", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(), mahasiswa.getProgram(),
						mahasiswa.getStatusAwalMahasiswa()).getNilai();
				// System.out.println("kodeItemBiaya -> " + kodeItemBiaya);
				if (!kodeItemBiaya.trim().isEmpty()) {

					List<String> kodes = new ArrayList<String>();
					for (String s : kodeItemBiaya.split(";")) {
						if (!s.trim().isEmpty()) {
							kodes.add(s.trim());
						}
					}

					// System.out.println("kodeItemBiaya kodes -> " +
					// kodes.size());

					if (!kodes.isEmpty()) {
						int kegiatan = mahasiswa.ambilJumlahCicilanPembayaran(kodeItemBiaya, semester);
						boolean hasil = kegiatan > 0;

						if (!hasil) {
							Collection<JenisKegiatan> jenisKegiatan = mahasiswa.ambilJenisKegiatans(semester,
									kodeItemBiaya);
							if (Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa,
									jenisKegiatan)) {
								return true;
							}
						}

						if (!hasil) {
							return false;
						}
					}

				}

			}

			if (CommonHelperClass.jenisKegiatansUntukKrs == null) {
				reloadJenisKegiatans();
			}

			List<Kegiatan> kegiatanDibayars = mahasiswa.ambilKegiatans(semester,
					CommonHelperClass.jenisKegiatansUntukKrs, true);

			if (!mahasiswabaruMengikutipersyaratanKrsSptMahasiswa && (semester != null
					&& (semester.equals(1) || semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 1)
							|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester()))
					&& kegiatanDibayars.isEmpty() && mahasiswa != null && mahasiswa.getNim() != null)) {

				if (Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa,
						ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU)) {
					return true;
				}

				Session session = null;
				BiodataCalonMahasiswa biodataCalonMahasiswa = null;
				try {
					session = HibernateUtil.currentNativeSession();
					biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues
							.simpleObject(session.createCriteria(BiodataCalonMahasiswa.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.sqlRestriction(
											"upper(trim(this_.nim)) = upper(trim('" + mahasiswa.getNim().trim() + "'))"))
									.setMaxResults(1), BiodataCalonMahasiswa.class);
				} finally {
					HibernateUtil.closeSession();
				}
				if (biodataCalonMahasiswa != null) {
					TreeSet<JenisKegiatan> jenisKegiatanCalonMahasiswa = new TreeSet<JenisKegiatan>(
							CommonHelperClass.jenisKegiatansUntukKrs);
					jenisKegiatanCalonMahasiswa.add(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
					kegiatanDibayars = biodataCalonMahasiswa.ambilKegiatans(semester,
							jenisKegiatanCalonMahasiswa, true);
				}

			} else {
				if (Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa,
						ConstantValues.PENDAFTARAN_MAHASISWA_LAMA)) {
					return true;
				}
			}

			boolean hasil = false;
			for (Kegiatan kegiatanDibayar : kegiatanDibayars) {
				if (kegiatanDibayar != null && kegiatanDibayar.hitungPersentaseLunasAktual() >= 0.1) {
					hasil = true;
					break;
				}
			}
			return hasil;
		}
	}



	/** Seperti {@link #checkStatusPembayaranMahasiswaSebelumnya(Integer, Integer, Mahasiswa, boolean)} dengan {@code persetujuan=false}. */
	public static boolean checkStatusPembayaranMahasiswaSebelumnya(Integer semester, Integer tahap,
			Mahasiswa mahasiswa) {
		return checkStatusPembayaranMahasiswaSebelumnya(semester, tahap, mahasiswa, false);
	}



	/**
	 * Memeriksa syarat pembayaran SEMESTER-SEMESTER SEBELUMNYA (bukan semester berjalan) — mis.
	 * dipakai sebagai syarat tambahan sebelum mengizinkan aksi yang mensyaratkan seluruh tunggakan
	 * lampau lunas. Murni mendelegasikan ke implementasi kanonik di {@link CommonHelperClass}.
	 *
	 * @param semester    semester acuan (pemeriksaan mencakup semester-semester sebelum ini)
	 * @param tahap       tahap pembayaran
	 * @param mahasiswa   mahasiswa yang diperiksa
	 * @param persetujuan bila {@code true}, memakai jalur/konfigurasi gerbang persetujuan
	 * @return hasil pemeriksaan dari {@link CommonHelperClass}
	 */
	public static boolean checkStatusPembayaranMahasiswaSebelumnya(Integer semester, Integer tahap, Mahasiswa mahasiswa,
			boolean persetujuan) {
		return CommonHelperClass.checkStatusPembayaranMahasiswaSebelumnya(semester, tahap, mahasiswa, persetujuan);
	}



	/**
	 * Memeriksa syarat pembayaran untuk satu {@link FormulirKegiatan} (kegiatan non-KRS yang
	 * mensyaratkan pembayaran, mis. kegiatan kemahasiswaan berbayar). Murni mendelegasikan ke
	 * implementasi kanonik di {@link CommonHelperClass}.
	 *
	 * @param formulirKegiatan kegiatan yang syarat pembayarannya diperiksa
	 * @param mahasiswa        mahasiswa yang diperiksa
	 * @return hasil pemeriksaan dari {@link CommonHelperClass}
	 */
	public static boolean checkStatusPembayaranKegiatanMahasiswa(FormulirKegiatan formulirKegiatan,
			Mahasiswa mahasiswa) {
		return CommonHelperClass.checkStatusPembayaranKegiatanMahasiswa(formulirKegiatan, mahasiswa);
	}



	/**
	 * Memeriksa syarat pembayaran semester-semester sebelumnya sebagai gerbang PENILAIAN (mis.
	 * dosen tidak dapat menginput nilai bila mahasiswa punya tunggakan). Murni mendelegasikan ke
	 * implementasi kanonik di {@link CommonHelperClass}.
	 *
	 * @param semester     semester acuan
	 * @param tahap        tahap pembayaran
	 * @param mahasiswa    mahasiswa yang diperiksa
	 * @param harusLunas   ambang batas persentase/nilai yang harus lunas agar gerbang dilewati
	 * @param termasukSmt1 bila {@code true}, semester 1 turut diperhitungkan dalam pemeriksaan
	 * @return hasil pemeriksaan dari {@link CommonHelperClass}
	 */
	public static boolean checkStatusPembayaranMahasiswaSebelumnyaUntukPenilaian(Integer semester, Integer tahap,
			Mahasiswa mahasiswa, Double harusLunas, boolean termasukSmt1) {
		return CommonHelperClass.checkStatusPembayaranMahasiswaSebelumnyaUntukPenilaian(semester, tahap, mahasiswa,
				harusLunas, termasukSmt1);
	}



	/**
	 * Memeriksa syarat pembayaran sebagai gerbang PENGAJUAN PROPOSAL SKRIPSI untuk satu format
	 * penilaian proposal tertentu. Murni mendelegasikan ke implementasi kanonik di
	 * {@link CommonHelperClass}.
	 *
	 * @param formatNilaiProposalSkripsi format nilai proposal terkait
	 * @param semester                   semester berjalan
	 * @param mahasiswa                  mahasiswa yang mengajukan
	 * @return hasil pemeriksaan dari {@link CommonHelperClass}
	 */
	public static boolean checkStatusPembayaranMahasiswaPengajuanSkripsi(
			FormatNilaiProposalSkripsi formatNilaiProposalSkripsi, Integer semester, Mahasiswa mahasiswa) {
		return CommonHelperClass.checkStatusPembayaranMahasiswaPengajuanSkripsi(formatNilaiProposalSkripsi, semester,
				mahasiswa);
	}



	/**
	 * Memeriksa syarat pembayaran sebagai gerbang PENGAJUAN SIDANG SKRIPSI untuk satu format
	 * penilaian sidang tertentu. Murni mendelegasikan ke implementasi kanonik di
	 * {@link CommonHelperClass}.
	 *
	 * @param formatNilaiSkripsi format nilai sidang terkait
	 * @param semester           semester berjalan
	 * @param mahasiswa          mahasiswa yang mengajukan
	 * @return hasil pemeriksaan dari {@link CommonHelperClass}
	 */
	public static boolean checkStatusPembayaranMahasiswaPengajuanSidang(FormatNilaiSkripsi formatNilaiSkripsi,
			Integer semester, Mahasiswa mahasiswa) {
		return CommonHelperClass.checkStatusPembayaranMahasiswaPengajuanSidang(formatNilaiSkripsi, semester, mahasiswa);
	}



	/**
	 * Memeriksa syarat pembayaran sebagai gerbang PENGAJUAN WISUDA. Murni mendelegasikan ke
	 * implementasi kanonik di {@link CommonHelperClass}.
	 *
	 * @param semester  semester berjalan
	 * @param mahasiswa mahasiswa yang mengajukan wisuda
	 * @return hasil pemeriksaan dari {@link CommonHelperClass}
	 */
	public static boolean checkStatusPembayaranMahasiswaPengajuanWisuda(Integer semester, Mahasiswa mahasiswa) {
		return CommonHelperClass.checkStatusPembayaranMahasiswaPengajuanWisuda(semester, mahasiswa);
	}



	/**
	 * Pembungkus transaksional untuk {@link #simpanCicilanTanpaMencicil(Kegiatan, Double, Date,
	 * String, JenisPembayaran, Collection, Session)}: membuka session Hibernate baru, memulai
	 * transaksi, mendelegasikan pencatatan cicilan, lalu commit dan menutup session — dipakai saat
	 * pemanggil TIDAK memiliki session/transaksi Hibernate yang sedang berjalan (nama method
	 * "TanpaSesseion" merujuk pada tidak adanya session yang perlu disediakan pemanggil, bukan
	 * "tanpa session sama sekali"). Kegagalan ditampilkan ke admin lewat
	 * {@link Common#tampilErrorJikaAdmin} tanpa dilempar ke pemanggil.
	 *
	 * @param kegiatan       kegiatan (mahasiswa aktif) yang dicatat pembayarannya
	 * @param nominal        nominal cicilan, dipakai hanya untuk jalur tanpa rincian item
	 *                       ({@code detailBiayas} kosong)
	 * @param tanggalValidasi tanggal validasi pembayaran
	 * @param keterangan     keterangan cicilan
	 * @param jenisPembayaran jenis/metode pembayaran
	 * @param detailBiayas   koleksi {@link DetailBiaya}/{@link PengaturanPembayaranBulanan} rincian
	 *                       item yang dibayar; kosong/{@code null} berarti satu baris cicilan
	 *                       generik tanpa rincian item
	 */
	public static void simpanCicilanDefaultTanpaSesseion(Kegiatan kegiatan, Double nominal, Date tanggalValidasi,
			String keterangan, JenisPembayaran jenisPembayaran, @SuppressWarnings("rawtypes") Collection detailBiayas) {
		Session session = HibernateUtil.currentNativeSession();

		try {
			session.getTransaction().begin();

			simpanCicilanTanpaMencicil(kegiatan, nominal, tanggalValidasi, keterangan, jenisPembayaran, detailBiayas,
					session);

			session.getTransaction().commit();
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
		}

		HibernateUtil.closeSession();
	}



	/**
	 * Mencatat cicilan pembayaran untuk satu {@link Kegiatan} (mahasiswa aktif) TANPA proses
	 * cicil-mencicil bertahap (satu kali pencatatan penuh, bukan menghitung sisa cicilan berikutnya)
	 * — dipakai saat mahasiswa membayar langsung dari luar mekanisme cicilan biasa (mis. hasil
	 * rekonsiliasi Host-to-Host, penyesuaian manual admin). Lebih dulu memanggil
	 * {@link PembayaranUtil#getResetCicilanOld} untuk membersihkan cicilan lama yang mungkin perlu
	 * disegarkan/dihapus sebelum baris baru dicatat.
	 *
	 * <p>
	 * Bila {@code detailBiayas} kosong/{@code null}: membuat SATU baris {@link CicilanPembayaran}
	 * generik (tanpa item biaya spesifik) dengan {@code nominal} yang diberikan langsung. Bila
	 * {@code detailBiayas} berisi elemen: membuat SATU baris per elemen (bertipe
	 * {@link DetailBiaya} langsung, atau {@link PengaturanPembayaranBulanan} yang detail biayanya
	 * diturunkan darinya), dengan nilai per baris dihitung dari
	 * {@link PengaturanPembayaranBulanan#ambilNominalModifikasi(Mahasiswa, Integer)} (bila terkait
	 * pembayaran bulanan dan kegiatan punya {@link Mahasiswa}) atau nilai nominal/biaya baru pada
	 * {@link DetailBiaya} itu sendiri, diberi nomor urut ({@code ke}) mulai dari 1. Setiap baris
	 * disimpan lewat {@link Common#refreshSaveOrUpdate}, dan bila baris tersebut sudah memiliki
	 * {@link BuktiPembayaran} terkait, relasi baliknya turut diperbarui lewat
	 * {@link Common#refreshUpdate}.
	 * </p>
	 *
	 * @param kegiatan        kegiatan (mahasiswa aktif) yang dicatat pembayarannya
	 * @param nominal         nominal cicilan, dipakai hanya untuk jalur tanpa rincian item
	 * @param tanggalValidasi tanggal validasi pembayaran, diisikan ke setiap baris
	 * @param keterangan      keterangan cicilan, diisikan ke setiap baris
	 * @param jenisPembayaran jenis/metode pembayaran, diisikan ke setiap baris
	 * @param detailBiayas    koleksi {@link DetailBiaya}/{@link PengaturanPembayaranBulanan} rincian
	 *                        item; kosong/{@code null} berarti satu baris generik
	 * @param session         session Hibernate aktif dengan transaksi yang sudah dimulai oleh
	 *                        pemanggil (method ini TIDAK membuka/menutup session/transaksi sendiri)
	 * @return baris {@link CicilanPembayaran} TERAKHIR yang dibuat/disimpan (bukan seluruh daftar —
	 *         bila {@code detailBiayas} berisi banyak elemen, hanya baris paling akhir yang
	 *         dikembalikan; baris-baris sebelumnya tetap tersimpan ke database namun referensinya
	 *         tidak dikembalikan ke pemanggil), atau {@code null} bila terjadi kegagalan yang
	 *         tertangkap secara internal (ditampilkan ke admin, tidak dilempar ke pemanggil)
	 */
	public static CicilanPembayaran simpanCicilanTanpaMencicil(Kegiatan kegiatan, Double nominal, Date tanggalValidasi,
			String keterangan, JenisPembayaran jenisPembayaran, @SuppressWarnings("rawtypes") Collection detailBiayas,
			Session session) {
		CicilanPembayaran cicilanPembayaran = null;
		try {
			PembayaranUtil.getInstance().getResetCicilanOld(session, kegiatan.getCalonMahasiswa(),
					kegiatan.getMahasiswa(), kegiatan.getSemster(), kegiatan.getJenisKegiatan(), kegiatan);

			if (detailBiayas == null || detailBiayas.isEmpty()) {
				cicilanPembayaran = new CicilanPembayaran(null);
				cicilanPembayaran.setKe(1);
				cicilanPembayaran.setKegiatan(kegiatan);
				cicilanPembayaran.setKeterangan(keterangan);
				cicilanPembayaran.setItemBiaya(null);
				cicilanPembayaran.setNilai(nominal);
				cicilanPembayaran.setTanggal(tanggalValidasi);
				cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
				Common.refreshSaveOrUpdate(session, cicilanPembayaran);

				if (cicilanPembayaran.getBuktiPembayaran() != null) {
					BuktiPembayaran buktiPembayaran = cicilanPembayaran.getBuktiPembayaran();
					buktiPembayaran.setCicilanPembayaran(cicilanPembayaran);
					Common.refreshUpdate(session, buktiPembayaran);
				}
			} else {
				int i = 1;
				for (Object o : detailBiayas) {
					DetailBiaya detailBiaya = null;
					PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;
					if (o instanceof DetailBiaya) {
						detailBiaya = (DetailBiaya) o;
					} else if (o instanceof PengaturanPembayaranBulanan) {
						pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
						detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();

					}

					cicilanPembayaran = new CicilanPembayaran(detailBiaya);

					cicilanPembayaran.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
					cicilanPembayaran.setItemBiaya(detailBiaya.getItemBiaya());

					if (pengaturanPembayaranBulanan != null) {
						if (kegiatan.getMahasiswa() != null) {
							cicilanPembayaran.setNilai(pengaturanPembayaranBulanan
									.ambilNominalModifikasi(kegiatan.getMahasiswa(), kegiatan.getSemster()));
						} else {
							cicilanPembayaran.setNilai(pengaturanPembayaranBulanan.getNominal());
						}
					} else {
						cicilanPembayaran.setNilai(detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
								: detailBiaya.getNilaiBiayaBaru());
					}

					cicilanPembayaran.setKe(i);
					cicilanPembayaran.setKegiatan(kegiatan);
					cicilanPembayaran.setKeterangan(keterangan);
					cicilanPembayaran.setTanggal(tanggalValidasi);
					cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
					Common.refreshSaveOrUpdate(session, cicilanPembayaran);

					if (cicilanPembayaran.getBuktiPembayaran() != null) {
						BuktiPembayaran buktiPembayaran = cicilanPembayaran.getBuktiPembayaran();
						buktiPembayaran.setCicilanPembayaran(cicilanPembayaran);
						Common.refreshUpdate(session, buktiPembayaran);
					}
					i++;
				}
			}
		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
		}

		return cicilanPembayaran;

	}



	/**
	 * Seperti {@link #simpanCicilanDefaultTanpaSesseion(Kegiatan, Double, Date, String,
	 * JenisPembayaran, Collection)}, untuk {@link KegiatanTemporary} (kegiatan pendaftaran/calon
	 * mahasiswa yang belum menjadi {@link Kegiatan} permanen). Berbeda dari varian
	 * {@link Kegiatan}, method ini TIDAK membungkus proses dalam {@code try/catch} — kegagalan akan
	 * diteruskan sebagai exception tak tertangani ke pemanggil, bukan ditampilkan sebagai alert.
	 *
	 * @param kegiatanTemporary kegiatan sementara (pendaftaran/calon mahasiswa) yang dicatat
	 *                          pembayarannya
	 * @param nominal           nominal cicilan, dipakai hanya untuk jalur tanpa rincian item
	 * @param tanggalValidasi   tanggal validasi pembayaran
	 * @param keterangan        keterangan cicilan
	 * @param jenisPembayaran   jenis/metode pembayaran
	 * @param detailBiayas      koleksi rincian item; kosong/{@code null} berarti satu baris generik
	 */
	public static void simpanCicilanDefaultTanpaSesseion(KegiatanTemporary kegiatanTemporary, Double nominal,
			Date tanggalValidasi, String keterangan, JenisPembayaran jenisPembayaran,
			@SuppressWarnings("rawtypes") Collection detailBiayas) {
		Session session = HibernateUtil.currentNativeSession();

		session.getTransaction().begin();

		simpanCicilanTanpaMencicil(kegiatanTemporary, nominal, tanggalValidasi, keterangan, jenisPembayaran,
				detailBiayas, session);

		session.getTransaction().commit();
		HibernateUtil.closeSession();
	}



	/**
	 * Seperti {@link #simpanCicilanTanpaMencicil(Kegiatan, Double, Date, String, JenisPembayaran,
	 * Collection, Session)}, untuk {@link KegiatanTemporary}. Perbedaan penting: method ini SELALU
	 * menghapus lebih dulu SELURUH baris {@link CicilanPembayaran} yang sudah ada untuk
	 * {@code kegiatanTemporary} ini (lewat SQL native {@code DELETE}) sebelum mencatat baris baru —
	 * berbeda dari varian {@link Kegiatan} yang memakai
	 * {@link PembayaranUtil#getResetCicilanOld} (reset yang lebih selektif). Pola "hapus semua lalu
	 * tulis ulang" ini masuk akal untuk kegiatan sementara karena datanya belum final/permanen.
	 *
	 * @param kegiatanTemporary kegiatan sementara yang dicatat pembayarannya; cicilan lamanya
	 *                          (bila ada) dihapus seluruhnya lebih dulu
	 * @param nominal           nominal cicilan, dipakai hanya untuk jalur tanpa rincian item
	 * @param tanggalValidasi   tanggal validasi pembayaran
	 * @param keterangan        keterangan cicilan
	 * @param jenisPembayaran   jenis/metode pembayaran
	 * @param detailBiayas      koleksi rincian item; kosong/{@code null} berarti satu baris generik
	 * @param session           session Hibernate aktif dengan transaksi yang sudah dimulai oleh
	 *                          pemanggil
	 * @return baris {@link CicilanPembayaran} TERAKHIR yang dibuat/disimpan (lihat catatan yang sama
	 *         pada {@link #simpanCicilanTanpaMencicil(Kegiatan, Double, Date, String,
	 *         JenisPembayaran, Collection, Session)} mengenai hanya baris terakhir yang
	 *         dikembalikan)
	 */
	public static CicilanPembayaran simpanCicilanTanpaMencicil(KegiatanTemporary kegiatanTemporary, Double nominal,
			Date tanggalValidasi, String keterangan, JenisPembayaran jenisPembayaran,
			@SuppressWarnings("rawtypes") Collection detailBiayas, Session session) {

		session.createSQLQuery("delete from cicilan_pembayaran where kegiatan_temporary = " + kegiatanTemporary.getId())
				.executeUpdate();
		CicilanPembayaran cicilanPembayaran = null;
		if (detailBiayas == null || detailBiayas.isEmpty()) {
			cicilanPembayaran = new CicilanPembayaran(null);
			cicilanPembayaran.setKe(1);
			cicilanPembayaran.setKegiatanTemporary(kegiatanTemporary);
			cicilanPembayaran.setKeterangan(keterangan);
			cicilanPembayaran.setItemBiaya(null);
			cicilanPembayaran.setNilai(nominal);
			cicilanPembayaran.setTanggal(tanggalValidasi);
			cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
			Common.refreshSaveOrUpdate(session, cicilanPembayaran);

			if (cicilanPembayaran.getBuktiPembayaran() != null) {
				BuktiPembayaran buktiPembayaran = cicilanPembayaran.getBuktiPembayaran();
				buktiPembayaran.setCicilanPembayaran(cicilanPembayaran);
				Common.refreshUpdate(session, buktiPembayaran);
			}
		} else {
			int i = 1;
			for (Object o : detailBiayas) {
				DetailBiaya detailBiaya = null;
				PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;
				if (o instanceof DetailBiaya) {
					detailBiaya = (DetailBiaya) o;
				} else if (o instanceof PengaturanPembayaranBulanan) {
					pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
					detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();

				}
				cicilanPembayaran = new CicilanPembayaran(detailBiaya);

				if (pengaturanPembayaranBulanan != null) {
					if (kegiatanTemporary.getMahasiswa() != null) {
						cicilanPembayaran.setNilai(pengaturanPembayaranBulanan.ambilNominalModifikasi(
								kegiatanTemporary.getMahasiswa(), kegiatanTemporary.getSemster()));
					} else {
						cicilanPembayaran.setNilai(pengaturanPembayaranBulanan.getNominal());
					}
				} else {
					cicilanPembayaran.setNilai(detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
							: detailBiaya.getNilaiBiayaBaru());
				}

				cicilanPembayaran.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
				cicilanPembayaran.setItemBiaya(detailBiaya.getItemBiaya());

				cicilanPembayaran.setKe(i);
				cicilanPembayaran.setKegiatanTemporary(kegiatanTemporary);
				cicilanPembayaran.setKeterangan(keterangan);
				cicilanPembayaran.setTanggal(tanggalValidasi);
				cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
				Common.refreshSaveOrUpdate(session, cicilanPembayaran);

				if (cicilanPembayaran.getBuktiPembayaran() != null) {
					BuktiPembayaran buktiPembayaran = cicilanPembayaran.getBuktiPembayaran();
					buktiPembayaran.setCicilanPembayaran(cicilanPembayaran);
					Common.refreshUpdate(session, buktiPembayaran);
				}
				i++;
			}
		}
		return cicilanPembayaran;

	}



	/**
	 * Mencari baris {@link CicilanPembayaran} yang cocok dengan satu baris rekonsiliasi
	 * {@link LogHostToHost} (log transaksi Host-to-Host dari bank). Kolom
	 * {@code logHostToHost.getItem()} berisi daftar item transaksi berformat
	 * {@code kodeItem\<pemisah>\<...>\namaBulan} (elemen dipisah {@code \}, mengambil indeks 0
	 * sebagai kode item biaya dan indeks 2 sebagai nama bulan), dengan beberapa item dipisah
	 * {@code |}. Kegagalan parsing satu elemen item dicatat sebagai error admin dan elemen tersebut
	 * dilewati, tidak menggagalkan parsing elemen lain. Hasil parsing dipakai untuk mencari cicilan
	 * yang: terkait {@code logHostToHost.getKegiatan()}, kode item biaya-nya termasuk dalam daftar
	 * yang diparsing, nama bulan pembayaran bulanannya termasuk dalam daftar yang diparsing (bila
	 * daftar kosong, kondisi ini sengaja dibuat SELALU GAGAL lewat {@code sqlRestriction("false")} —
	 * bukan diabaikan — sehingga log tanpa item yang valid tidak mencocokkan cicilan mana pun),
	 * pemilik kegiatan cocok dengan {@code kode} (nomor registrasi ATAU nomor ujian calon
	 * mahasiswa) ATAU {@code nim} (NIM mahasiswa aktif), dan tanggal cicilan (dibandingkan hanya
	 * bagian TANGGAL lewat {@code DATE(...)}) sama dengan {@code tanggal}.
	 *
	 * @param session       session Hibernate aktif untuk query
	 * @param logHostToHost log transaksi H2H sumber daftar item yang dicari kecocokannya
	 * @param kode          nomor registrasi/nomor ujian calon mahasiswa yang dicocokkan
	 * @param nim           NIM mahasiswa aktif yang dicocokkan
	 * @param tanggal       tanggal transaksi yang dicocokkan (hanya bagian tanggal, bukan waktu)
	 * @return daftar {@link CicilanPembayaran} yang cocok, bisa kosong bila tidak ada kecocokan atau
	 *         {@code item} pada log tidak berisi kode/bulan yang valid
	 */
	public static List<CicilanPembayaran> ambilCicilanPembayarans(Session session, LogHostToHost logHostToHost,
			String kode, String nim, Date tanggal) {
		List<String> bulans = new ArrayList<String>();
		List<String> kodeItems = new ArrayList<String>();
		String item = logHostToHost.getItem();
		if (item != null && !item.trim().isEmpty()) {
			String[] ss = item.split("\\|");
			for (String sss : ss) {
				try {
					if (sss != null && !sss.trim().isEmpty()) {
						// System.out.println("sss ==> " + sss);
						String[] i = StringUtils.split(sss, '\\');
						bulans.add(i[2]);
						kodeItems.add(i[0]);
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}

		@SuppressWarnings("unchecked")
		List<CicilanPembayaran> cicilanPembayarans = session.createCriteria(CicilanPembayaran.class)
				.add(Restrictions.eq("kegiatan", logHostToHost.getKegiatan()))

				.createAlias("itemBiaya", "itemBiaya")

				.add(kodeItems.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.in("itemBiaya.kode", kodeItems))

				.createAlias("pengaturanPembayaranBulanan", "pengaturanPembayaranBulanan")
				.add(bulans.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.in("pengaturanPembayaranBulanan.namaBulan", bulans))
				.createAlias("kegiatan", "kegiatan").createAlias("kegiatan.mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("kegiatan.calonMahasiswa", "calonMahasiswa", Criteria.LEFT_JOIN)
				.add(Restrictions.or(
						Restrictions.or(Restrictions.eq("calonMahasiswa.noRegistrasi", kode),
								Restrictions.eq("calonMahasiswa.noUjian", kode)),
						Restrictions.eq("mahasiswa.nim", nim)))

				.add(Restrictions.sqlRestriction(
						"DATE(this_.tanggal) = DATE('" + Common.databaseDateFormat.get().format(tanggal) + "')"))
				.list();

		// System.out.println("nim = " + nim + ", kode = " + kode + ", bulans =
		// " + bulans + ", cicilanPembayarans = "
		// + cicilanPembayarans);

		return cicilanPembayarans;
	}



	/**
	 * Seperti {@link #ambilCicilanPembayarans(Session, LogHostToHost, String, String, Date)}, tetapi
	 * mencari pada entitas {@link CicilanPembayaranGagal} (cicilan yang GAGAL tervalidasi/gagal
	 * rekonsiliasi) alih-alih {@link CicilanPembayaran} yang sukses. Logika parsing item dan kriteria
	 * pencarian identik.
	 *
	 * @param session       session Hibernate aktif untuk query
	 * @param logHostToHost log transaksi H2H sumber daftar item yang dicari kecocokannya
	 * @param kode          nomor registrasi/nomor ujian calon mahasiswa yang dicocokkan
	 * @param nim           NIM mahasiswa aktif yang dicocokkan
	 * @param tanggal       tanggal transaksi yang dicocokkan
	 * @return daftar {@link CicilanPembayaranGagal} yang cocok, bisa kosong
	 */
	public static List<CicilanPembayaranGagal> ambilCicilanPembayaranGagals(Session session,
			LogHostToHost logHostToHost, String kode, String nim, Date tanggal) {
		List<String> bulans = new ArrayList<String>();
		List<String> kodeItems = new ArrayList<String>();
		String item = logHostToHost.getItem();
		if (item != null && !item.trim().isEmpty()) {
			String[] ss = item.split("\\|");
			for (String sss : ss) {
				try {
					if (sss != null && !sss.trim().isEmpty()) {
						// System.out.println("sss ==> " + sss);
						String[] i = StringUtils.split(sss, '\\');
						bulans.add(i[2]);
						kodeItems.add(i[0]);
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}

		@SuppressWarnings("unchecked")
		List<CicilanPembayaranGagal> cicilanPembayaranGagals = session.createCriteria(CicilanPembayaranGagal.class)
				.add(Restrictions.eq("kegiatan", logHostToHost.getKegiatan()))

				.createAlias("itemBiaya", "itemBiaya")

				.add(kodeItems.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.in("itemBiaya.kode", kodeItems))

				.createAlias("pengaturanPembayaranBulanan", "pengaturanPembayaranBulanan")
				.add(bulans.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.in("pengaturanPembayaranBulanan.namaBulan", bulans))
				.createAlias("kegiatan", "kegiatan").createAlias("kegiatan.mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("kegiatan.calonMahasiswa", "calonMahasiswa", Criteria.LEFT_JOIN)
				.add(Restrictions.or(
						Restrictions.or(Restrictions.eq("calonMahasiswa.noRegistrasi", kode),
								Restrictions.eq("calonMahasiswa.noUjian", kode)),
						Restrictions.eq("mahasiswa.nim", nim)))

				.add(Restrictions.sqlRestriction(
						"DATE(this_.tanggal) = DATE('" + Common.databaseDateFormat.get().format(tanggal) + "')"))
				.list();

		// System.out.println("nim = " + nim + ", kode = " + kode + ", bulans =
		// " + bulans + ", cicilanPembayaranGagals = "
		// + cicilanPembayaranGagals);
		return cicilanPembayaranGagals;
	}



	/**
	 * Membuat objek {@link CicilanPembayaranGagal} BARU (belum disimpan) dengan seluruh field
	 * disalin dari {@code cicilanPembayaran} yang sukses — dipakai saat rekonsiliasi H2H
	 * menyimpulkan bahwa suatu cicilan yang tadinya tercatat sukses ternyata harus dipindahkan ke
	 * status gagal. Penyimpanan ke database (termasuk penghapusan baris sukses asal, bila perlu)
	 * adalah tanggung jawab pemanggil, bukan method ini.
	 *
	 * @param cicilanPembayaran cicilan sukses sumber data yang disalin
	 * @return objek {@link CicilanPembayaranGagal} baru dengan field yang sama, belum tersimpan
	 */
	public static CicilanPembayaranGagal copyCicilanPembayaranKeGagal(CicilanPembayaran cicilanPembayaran) {
		CicilanPembayaranGagal cicilanPembayaranGagal = new CicilanPembayaranGagal(cicilanPembayaran.getDetailBiaya());
		cicilanPembayaranGagal.setItemBiaya(cicilanPembayaran.getItemBiaya());
		cicilanPembayaranGagal.setJenisPembayaran(cicilanPembayaran.getJenisPembayaran());
		cicilanPembayaranGagal.setKe(cicilanPembayaran.getKe());
		cicilanPembayaranGagal.setKegiatan(cicilanPembayaran.getKegiatan());
		cicilanPembayaranGagal.setKeterangan(cicilanPembayaran.getKeterangan());
		cicilanPembayaranGagal.setNilai(cicilanPembayaran.getNilai());
		cicilanPembayaranGagal.setPengaturanPembayaranBulanan(cicilanPembayaran.getPengaturanPembayaranBulanan());
		cicilanPembayaranGagal.setPostingHistory(cicilanPembayaran.getPostingHistory());
		cicilanPembayaranGagal.setRekonsiliasiHostToHost(cicilanPembayaran.getRekonsiliasiHostToHost());
		cicilanPembayaranGagal.setValidator(cicilanPembayaran.getValidator());
		cicilanPembayaranGagal.setTanggal(cicilanPembayaran.getTanggal());
		return cicilanPembayaranGagal;
	}



	/**
	 * Kebalikan dari {@link #copyCicilanPembayaranKeGagal(CicilanPembayaran)}: membuat objek
	 * {@link CicilanPembayaran} BARU (belum disimpan) dengan seluruh field disalin dari
	 * {@code cicilanPembayaranGagal} — dipakai saat rekonsiliasi H2H menyimpulkan bahwa suatu
	 * cicilan yang tadinya tercatat gagal ternyata harus dipindahkan ke status sukses. Penyimpanan
	 * ke database adalah tanggung jawab pemanggil.
	 *
	 * @param cicilanPembayaranGagal cicilan gagal sumber data yang disalin
	 * @return objek {@link CicilanPembayaran} baru dengan field yang sama, belum tersimpan
	 */
	public static CicilanPembayaran copyCicilanPembayaranKeSukses(CicilanPembayaranGagal cicilanPembayaranGagal) {
		CicilanPembayaran cicilanPembayaran = new CicilanPembayaran(cicilanPembayaranGagal.getDetailBiaya());
		cicilanPembayaran.setItemBiaya(cicilanPembayaranGagal.getItemBiaya());
		cicilanPembayaran.setJenisPembayaran(cicilanPembayaranGagal.getJenisPembayaran());
		cicilanPembayaran.setKe(cicilanPembayaranGagal.getKe());
		cicilanPembayaran.setKegiatan(cicilanPembayaranGagal.getKegiatan());
		cicilanPembayaran.setKeterangan(cicilanPembayaranGagal.getKeterangan());
		cicilanPembayaran.setNilai(cicilanPembayaranGagal.getNilai());
		cicilanPembayaran.setPengaturanPembayaranBulanan(cicilanPembayaranGagal.getPengaturanPembayaranBulanan());
		cicilanPembayaran.setPostingHistory(cicilanPembayaranGagal.getPostingHistory());
		cicilanPembayaran.setRekonsiliasiHostToHost(cicilanPembayaranGagal.getRekonsiliasiHostToHost());
		cicilanPembayaran.setTanggal(cicilanPembayaranGagal.getTanggal());
		cicilanPembayaran.setValidator(cicilanPembayaranGagal.getValidator());
		return cicilanPembayaran;
	}



	/**
	 * Membangun satu BARIS ZKoss ({@code row}, sudah dibuat oleh pemanggil, ditambahkan sebagai
	 * anak {@code rowsCicilan} di sini) untuk menampilkan satu {@link CicilanPembayaran} pada grid
	 * cicilan pembayaran, lengkap dengan label "Ke-N" dan area unggah/unduh bukti pembayaran.
	 * Berbeda tampilannya tergantung apakah cicilan SUDAH punya {@link BuktiPembayaran}
	 * ({@code cicilanPembayaran.getBuktiPembayaran()} atau atribut {@code "buktiPembayaran"} yang
	 * sudah ada pada {@code row}, yang pertama diprioritaskan): bila BELUM ada, ditampilkan
	 * komponen unggah aktif lewat {@link LampiranLain#createDownloadUploadFileLain} yang saat
	 * lampiran berhasil diunggah akan mendaftarkannya ke {@code buktiPembayarans} (map berbagi milik
	 * pemanggil, dikunci berdasarkan {@code idLampiran} sementara/permanen) dan memperbarui
	 * {@code idLampiran} pada {@code cicilanPembayaran}; bila SUDAH ada, ditampilkan sebagai
	 * tautan unduh saja (read-only, tidak bisa diganti dari sini). Sejumlah atribut ZKoss disimpan
	 * pada {@code row} untuk dibaca kembali oleh kode pemanggil saat menyusun ulang data dari grid
	 * (mis. {@code "cicilanPembayaran"}, {@code "buttonHapus"}, {@code "idLampiran"},
	 * {@code "hboxLampiran"}).
	 *
	 * @param buktiPembayarans map berbagi (dimiliki pemanggil) tempat lampiran bukti pembayaran yang
	 *                         baru diunggah didaftarkan, dikunci berdasarkan id lampiran
	 *                         sementara/permanen
	 * @param rowsCicilan      kontainer {@link Rows} grid ZKoss tempat {@code row} ditambahkan
	 * @param row              baris ZKoss yang akan diisi (sudah dibuat pemanggil, belum punya
	 *                         parent)
	 * @param i                indeks urutan baris (dipakai untuk label "Ke-(i+1)")
	 * @param cicilanPembayaran data cicilan yang direpresentasikan baris ini
	 * @param buttonHapus      konfigurasi tombol hapus baris, disimpan sebagai atribut {@code row}
	 *                         untuk dipakai kode lain (tidak dirender langsung oleh method ini)
	 * @return {@link Hbox} kontainer area lampiran yang baru dibuat, sudah ditambahkan ke struktur
	 *         baris
	 */
	public static Hbox initCicilan(final Map<Long, LampiranLain> buktiPembayarans, final Rows rowsCicilan,
			final Row row, int i, CicilanPembayaran cicilanPembayaran, MyToolbarbuttonConfig buttonHapus) {
		BuktiPembayaran buktiPembayaran = (BuktiPembayaran) row.getAttribute("buktiPembayaran");

		if (cicilanPembayaran.getBuktiPembayaran() != null) {
			buktiPembayaran = cicilanPembayaran.getBuktiPembayaran();
		}

		row.setValign("top");
		row.setAttribute("buktiPembayaran", buktiPembayaran);

		row.setParent(rowsCicilan);
		Vbox vbox = new Vbox();
		row.appendChild(vbox);
		vbox.appendChild(new Label("Ke-" + (i + 1) + ""));
		row.setValign("top");
		row.setAttribute("cicilanPembayaran", cicilanPembayaran);
		row.setValign("top");
		row.setAttribute("buttonHapus", buttonHapus);

		final Long idLampiran = cicilanPembayaran.getIdLampiran() == null ? Common.refSementara()
				: cicilanPembayaran.getIdLampiran();
		row.setValign("top");
		row.setAttribute("idLampiran", idLampiran);
		Hbox hboxLampiran = new Hbox();
		if (buktiPembayaran == null) {
			hboxLampiran.setVisible(cicilanPembayaran.getId() != null);
			final CicilanPembayaran tempCicilanPembayaran = cicilanPembayaran;
			LampiranLain.createDownloadUploadFileLain(hboxLampiran, idLampiran, "cicilanPembayaran", "Bukti Pembayaran",
					false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lainMahasiswa = (LampiranLain) arg0.getData();
							buktiPembayarans.put(idLampiran, lainMahasiswa);

							tempCicilanPembayaran.setIdLampiran(lainMahasiswa.getId());
							row.setValign("top");
							row.setAttribute("cicilanPembayaran", tempCicilanPembayaran);
						}
					}, null, false, true, true, true, null);
		} else {
			Hbox hbox = new Hbox();
			hbox.setParent(hboxLampiran);
			LampiranLain.createDownloadUploadFileLain(hbox, buktiPembayaran.getId(), BuktiPembayaran.class.getName(),
					"Bukti Pembayaran", true, null, null, false, false, false, false);
			// LampiranLain.createDownloadUploadFileLain(hboxLampiran,
			// buktiPembayaran.getId(),
			// BuktiPembayaran.class.getName(), "Bukti Pembayaran", true, null,
			// null, false, true, true, false,
			// null);
		}
		hboxLampiran.setParent(vbox);
		row.setValign("top");
		row.setAttribute("hboxLampiran", hboxLampiran);
		return hboxLampiran;

	}



	/**
	 * Menghilangkan duplikat dari {@code cicilanPembayarans} berdasarkan kunci komposit yang dipilih
	 * tergantung jenis cicilan (dipakai sebelum menampilkan/menyimpan ulang daftar cicilan agar
	 * tidak ada baris yang secara efektif merepresentasikan pembayaran yang sama muncul dobel):
	 * untuk cicilan {@link PengaturanPembayaranBulanan} dengan nilai yang BOLEH DIUBAH pengguna
	 * ({@code getNilaiBisaDiubah()}), kunci mencakup id cicilan itu sendiri (sehingga setiap baris
	 * tetap dianggap unik walau bulan+item-nya sama, karena nilainya bisa berbeda-beda per baris);
	 * untuk cicilan bulanan dengan nilai TETAP, kunci hanya bulan+item biaya (baris dengan
	 * bulan+item sama dianggap duplikat, yang terakhir diproses akan menang menggantikan yang
	 * sebelumnya di map); untuk cicilan biaya biasa (bukan bulanan), kunci mencakup item biaya +
	 * nilai terformat + tanggal terformat.
	 *
	 * @param cicilanPembayarans daftar sumber yang mungkin mengandung duplikat
	 * @return daftar baru berisi satu {@link CicilanPembayaran} per kunci unik (urutan tidak
	 *         dijamin sama dengan input, mengikuti urutan iterasi {@link HashMap})
	 */
	public static List<CicilanPembayaran> filterCicilanPembayaran(List<CicilanPembayaran> cicilanPembayarans) {
		Map<String, CicilanPembayaran> mapCicilanPembayaran = new HashMap<String, CicilanPembayaran>();
		for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
					.getPengaturanPembayaranBulanan();
			if (pengaturanPembayaranBulanan != null) {
				if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNilaiBisaDiubah()) {
					mapCicilanPembayaran.put(
							cicilanPembayaran.getId() + "-" + pengaturanPembayaranBulanan.getBulan() + "-"
									+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId(),
							cicilanPembayaran);
				} else {
					mapCicilanPembayaran.put(
							pengaturanPembayaranBulanan.getBulan() + "-"
									+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId(),
							cicilanPembayaran);
				}
			} else {
				mapCicilanPembayaran.put(cicilanPembayaran.getItemBiaya().getId() + "-"
						+ Common.numberFormat.get().format(cicilanPembayaran.getNilai()) + "-"
						+ Common.dateFormat.get().format(cicilanPembayaran.getTanggal()), cicilanPembayaran);
			}
		}
		// System.out.println("cicilanPembayarans=>" +
		// mapCicilanPembayaran.keySet());
		return new ArrayList<CicilanPembayaran>(mapCicilanPembayaran.values());
	}

}
