package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Calendar;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
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
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Include;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Vbox;

import ais.action.maintenance.PSBAction;
import ais.action.master.KonfigurasiTampilanBiodataCalonSiswaAction;
import ais.action.master.SertifikatAction;
import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.AmbilDataKecamatanBanbox;
import ais.action.master.helper.AmbilDataNegaraBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.pmb.BiodataCalonMahasiswaAction;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.master.sekolah.helper.RevisiCalonSiswaHelper;
import ais.action.master.sekolah.helper.TagihanUtilCalonSiswa;
import ais.action.master.sekolah.psb.CommonReportPsb;
import ais.action.master.sekolah.psb.ParameterTambahanPsbListener;
import ais.action.master.sekolah.psb.TampilanUjianCalonSiswa;
import ais.action.master.sekolah.psb.VerifikasiMatapelajaranPSBHelper;
import ais.action.master.sekolah.psb.VerifikasiPSBHelper;
import ais.action.master.sekolah.psb.VerifikasiParameterPSBHelper;
import ais.action.master.sekolah.psb.form.PPDB;
import ais.action.master.sekolah.psb.nis.DefaultNisGenerator;
import ais.action.master.sekolah.psb.nis.NisGenerator;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.common.BarcodeCommon;
import ais.common.BniCommon;
import ais.common.BriCommon;
import ais.common.Common;
import ais.common.CommonEmail;
import ais.common.CommonMedia;
import ais.common.CommonPSB;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Agama;
import ais.database.model.AlatTransportasiMahasiswa;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisTinggalMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.Kota;
import ais.database.model.MatapelajaranSekolah;
import ais.database.model.Negara;
import ais.database.model.Pegawai;
import ais.database.model.Pekerjaan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Propinsi;
import ais.database.model.Tbmuser;
import ais.database.model.Wilayah;
import ais.database.model.bni.BniRequest;
import ais.database.model.bri.BriRequest;
import ais.database.model.employ.Keluarga;
import ais.database.model.employ.Pendidikan;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoCalonSiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.CalonSiswaPunyaVerifikasiBerkas;
import ais.database.model.sekolah.CalonSiswaPunyaVerifikasiMatapelajaran;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.GelombangPendaftaranPsbPunyaMatapelajaran;
import ais.database.model.sekolah.JadwalPertemuanPSB;
import ais.database.model.sekolah.JadwalUjianPSB;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.KelasLesSiswaPunyaSiswa;
import ais.database.model.sekolah.KelasSiswaPSB;
import ais.database.model.sekolah.PaketPsb;
import ais.database.model.sekolah.PaketPsbPunyaGelombangPendaftaranPsb;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.PenghasilanOrangTuaSiswa;
import ais.database.model.sekolah.PenjurusanSekolah;
import ais.database.model.sekolah.RuangGelombangPendaftaranPsbPSB;
import ais.database.model.sekolah.RuangPSB;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.StatusAwalSiswa;
import ais.database.model.sekolah.Tagihan;
import ais.database.model.sekolah.VerifikasiKelengkapanCalonSiswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyCombobox;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBoldHijau;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyLabelConfigTitikDua;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowStyled;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

public class CalonSiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	public final static String[] DEFAULT_TIDAK_AKTIF = new String[] { "noRegistrasi", "panggilan", "agama", "anakKe",
			"dariAnakKe", "tahunMasuk", "nomorInduk", "nomorIndukNasional", "jumlahSaudaraKandung", "jumlahSaudaraTiri",
			"kewarganegaraan", "negara", "padaTanggal", "alamatEmail", "teleponSiswa", "pekerjaanAyah",
			"pendidikanAyah", "penghasilanAyah", "tempatLahirAyah", "tanggalLahirAyah", "hp1ayah", "hp2ayah", "hp3ayah",
			"pekerjaanIbu", "pendidikanIbu", "penghasilanIbu", "tempatLahirIbu", "tanggalLahirIbu", "hp1ibu", "hp2ibu",
			"hp3ibu", "namaWali", "pekerjaanWali", "pendidikanWali", "penghasilanWali", "tempatLahirWali",
			"tanggalLahirWali", "hp1wali", "hp2wali", "hp3wali", "statusDalamKeluarga", "bahasa", "berat", "tinggi",
			"riwayatPenyakit", "golonganDarah", "hobby", "keterangan", "statusSiswa", "alamatAyah", "alamatIbu",
			"prestasiSiswa1", "prestasiSiswa2", "prestasiSiswa3", "nik", "kk", "waAyah", "waIbu", "waWali",
			"desaKelurahanSekolahAsal", "kecamatanSekolahAsal", "kotaSekolahAsal", "propinsiSekolahAsal",
			"orangTuaPegawai", "riwayatPembayaranPendaftaran", "riwayatPembayaranDaftarUlang", "paketPsb",
			"merupakanPindahan", "tanggalPindah", "keteranganPindah", "pindahanDariSekolah", "alamatSekolahPindahan",
			"kelasSekolahPindahan", "orangTuaPegawai", "riwayatPembayaranPendaftaran", "riwayatPembayaranDaftarUlang" };

	public final static String[] DEFAULT_TIDAK_WAJIB = new String[] { "noRegistrasi", "noAktaKelahiran", "panggilan",
			"statusAwalSiswa", "gelombangPendaftaranPsb", "kelas", "sekolah", "penjurusanSekolah", "agama",
			"tahunMasuk", "anakKe", "dariAnakKe", "nomorInduk", "nomorIndukNasional", "jumlahSaudaraKandung",
			"jumlahSaudaraTiri", "kewarganegaraan", "negara", "padaTanggal", "alamatEmail", "teleponSiswa",
			"pekerjaanAyah", "pendidikanAyah", "penghasilanAyah", "tempatLahirAyah", "tanggalLahirAyah", "hp1ayah",
			"hp2ayah", "hp3ayah", "pekerjaanIbu", "pendidikanIbu", "penghasilanIbu", "tempatLahirIbu",
			"tanggalLahirIbu", "hp1ibu", "hp2ibu", "hp3ibu", "namaWali", "pekerjaanWali", "pendidikanWali",
			"penghasilanWali", "tempatLahirWali", "tanggalLahirWali", "hp1wali", "hp2wali", "hp3wali", "alamatSiswa",
			"dusunCalon", "rt", "rw", "kodePos", "kelurahanCalon", "kecamatanCalon", "propinsiCalon", "kotaCalon",
			"alamatWali", "teleponWali", "sekolahAsal", "alamatSekolahAsal", "statusDalamKeluarga", "bahasa", "berat",
			"tinggi", "riwayatPenyakit", "golonganDarah", "hobby", "keterangan", "statusSiswa", "alamatAyah",
			"alamatIbu", "prestasiSiswa1", "prestasiSiswa2", "prestasiSiswa3", "nik", "kk", "waAyah", "waIbu", "waWali",
			"desaKelurahanSekolahAsal", "kecamatanSekolahAsal", "kotaSekolahAsal", "propinsiSekolahAsal",
			"jenisTinggalMahasiswa", "alatTransportasiMahasiswa", "tahunLulus", "noIjazah", "statusSekolah",
			"jadwalPertemuanPSB",

			"noAktaKelahiran", "mempunyaiWali", "nomorPokokSekolahNasional", "nomorUjianNasional", "nomorSeriSkhun",
			"nomorSeriIjazah", "nis", "nikAyah", "nikIbu", "nikWali", "tidakLayakPip", "penerimaBantuan", "layakPip",
			"noKip", "alatTransportasi", "jenisTinggal", "koordinat", "kebutuhanKhusus", "infoMempunyaiSaudaraKandung",
			"apakahMempunyaiSaudaraKandung", "infoKampusDariMana", "namaTemanInfoKampusDariMana",
			"keteranganInfoKampusDariMana" };

	public final static String[] contents = new String[] { "id", "noRegistrasi", "namaSiswa", "gelombangPendaftaranPsb",
			"panggilan", "statusAwalSiswa", "nomorInduk", "nomorIndukNasional", "tahunMasuk", "tempatLahir",
			"tanggalLahir", "kelas", "sekolah", "penjurusanSekolah", "jenisKelamin", "agama", "anakKe", "dariAnakKe",
			"jumlahSaudaraKandung", "jumlahSaudaraTiri", "kewarganegaraan", "negara", "padaTanggal", "alamatEmail",
			"teleponSiswa", "namaAyah", "pekerjaanAyah", "pendidikanAyah", "penghasilanAyah", "tempatLahirAyah",
			"tanggalLahirAyah", "hp1ayah", "hp2ayah", "hp3ayah", "namaIbu", "pekerjaanIbu", "pendidikanIbu",
			"penghasilanIbu", "tempatLahirIbu", "tanggalLahirIbu", "hp1ibu", "hp2ibu", "hp3ibu", "namaWali",
			"pekerjaanWali", "pendidikanWali", "penghasilanWali", "tempatLahirWali", "tanggalLahirWali", "hp1wali",
			"hp2wali", "hp3wali", "alamatOrangTua", "teleponOrangTua", "alamatSiswa", "dusunCalon", "rt", "rw",
			"kodePos", "kelurahanCalon", "kecamatanCalon", "propinsiCalon", "kotaCalon", "alamatWali", "teleponWali",
			"sekolahAsal", "alamatSekolahAsal", "statusDalamKeluarga", "bahasa", "berat", "tinggi", "riwayatPenyakit",
			"golonganDarah", "hobby", "keterangan", "statusSiswa", "alamatAyah", "alamatIbu", "prestasiSiswa1",
			"prestasiSiswa2", "prestasiSiswa3", "nik", "kk", "waAyah", "waIbu", "waWali", "desaKelurahanSekolahAsal",
			"kecamatanSekolahAsal", "kotaSekolahAsal", "propinsiSekolahAsal", "jenisTinggalMahasiswa",
			"alatTransportasiMahasiswa", "tahunLulus", "noIjazah", "statusSekolah", "jadwalPertemuanPSB",
			"noAktaKelahiran", "mempunyaiWali", "nomorPokokSekolahNasional", "nomorUjianNasional", "nomorSeriSkhun",
			"nomorSeriIjazah", "nis", "nikAyah", "nikIbu", "nikWali", "tidakLayakPip", "layakPip", "penerimaBantuan",
			"noKip", "alatTransportasi", "jenisTinggal", "koordinat", "kebutuhanKhusus", "infoMempunyaiSaudaraKandung",
			"apakahMempunyaiSaudaraKandung", "infoKampusDariMana", "namaTemanInfoKampusDariMana",
			"keteranganInfoKampusDariMana", "paketPsb", "merupakanPindahan", "tanggalPindah", "keteranganPindah",
			"pindahanDariSekolah", "alamatSekolahPindahan", "kelasSekolahPindahan", "orangTuaPegawai",
			"riwayatPembayaranPendaftaran", "riwayatPembayaranDaftarUlang" };

	public final static String[] DATA = contents;

	public final static String[] DATA_DESC = new String[] { "id", "No Registrasi", "Nama Lengkap", "Gelombang",
			"Nama Panggilan", "Status Awal Siswa", "Nomor Induk", "Nomor Induk Siswa Nasional", "Tahun Masuk",
			"Tempat Lahir", "Tanggal Lahir", "Kelas", "Sekolah", "Penjurusan Sekolah", "Jenis Kelamin", "Agama",

			"Anak Ke", "Dari Ke", "Jumlah Saudara Kandung", "Jumlah Saudara Tiri", "Kewarganegaraan", "Negara",
			"Pada Tanggal", "Alamat Email", "Telepon (atau HP) / No. WA", "Nama Ayah", "Pekerjaan Ayah",

			"Pendidikan Ayah", "Penghasilan Ayah",

			"Tempat Lahir Ayah", "Tanggal Lahir Ayah", "HP Ayah", "HP Ayah", "HP Ayah",

			"Nama Ibu", "Pekerjaan Ibu",

			"Pendidikan Ibu", "Penghasilan Ibu",

			"Tempat Lahir Ibu", "Tanggal Lahir Ibu", "HP Ibu", "HP Ibu", "HP Ibu",

			"Nama Wali", "Pekerjaan Wali",

			"Pendidikan Wali", "Penghasilan Wali", "Tempat Lahir Wali", "Tanggal Lahir Wali", "HP Wali", "HP Wali",
			"HP Wali",

			"Alamat Orang Tua", "Telepon Orang Tua", "Alamat Siswa", "Dusun /Kampung Calon", "RT", "RW", "Kode Pos",
			"Kelurahan Calon", "Kecamatan Calon", "Propinsi Calon", "Kota Calon",

			"Alamat Wali", "Telepon Wali", "Nama Pendidikan/Sekolah Sebelumnya", "Alamat Pendidikan/Sekolah Sebelumnya",
			"Status Dalam Keluarga", "Bahasa", "Berat", "Tinggi", "Riwayat Penyakit", "Golongan Darah", "Hobby",
			"Keterangan", "Status Siswa", "Alamat Ayah", "Alamat Ibu", "Prestasi Siswa I", "Prestasi Siswa II",
			"Prestasi Siswa III", "NIK", "No. Kartu Keluarga", "No. WA Ayah", "No. WA Ibu", "No. WA Wali",
			"Desa atau Kelurahan Sekolah Asal", "Kecamatan Sekolah Asal", "Kota Sekolah Asal", "Propinsi Sekolah Asal",
			"Jenis Tinggal Siswa", "Alat Transportasi Siswa", "Tahun Lulus", "No. Ijazah", "Status Sekolah",
			"Jadwal Pertemuan", "Nomor Akta Kelahiran",

			"Mempunyai Wali", "Nomor Pokok Sekolah Nasional", "Nomor Ujian Nasional", "Nomor Seri SKHUN",
			"Nomor Seri Ijazah", "NIS", "NIK Ayah", "NIK Ibu", "NIK Wali", "Tidak Layak Pip ?", "Layak Pip ?",
			"Penerima Bantuan", "No. KIP", "Alat Transportasi", "Jenis Tinggal", "Koordinat", "Kebutuhan Khusus",
			"Info Mempunyai Saudara Kandung", "Apakah Mempunyai Saudara Kandung", "Informasi PPDB dari mana",
			"Nama teman info PPDB", "keterangan info PPDB", "Paket Pilihan", "Merupakan Pindahan", "Tanggal Pindah",
			"Keterangan Pindah", "Pindahan Dari Sekolah", "Alamat Sekolah Pindahan", "Kelas Sekolah Pindahan",
			"Orang Tua Pegawai", "Riwayat Pembayaran Pendaftaran", "Riwayat Pembayaran Daftar Ulang"

	};

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Tabbox mainTabbox;
	private Paging paging;
	private MyGrid grid;

	private North filter;

	private MyButtonConfig ujian;
	private Combobox searchSatusAwal;
	private Combobox searchta;
	private Combobox searchgel;
	private Textbox searchno;
	private Textbox searchnama;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Combobox searchPenjurusan;

	private Textbox namaSiswa;
	private Label nomorInduk;
	private Textbox nomorIndukNasional;
	private Combobox agama;
	private Textbox alamatEmail;
	private Textbox alamatOrangTua;
	private Textbox alamatSiswa;
	private Textbox alamatWali;
	private Intbox anakKe;
	private Intbox dariAnakKe;
	private Combobox jenisKelamin;
	private Textbox namaAyah;
	private Textbox namaIbu;
	private Textbox namaWali;
	private Combobox pekerjaanAyah;
	private Combobox pekerjaanIbu;
	private Combobox pekerjaanWali;
	private Textbox sekolahAsal;
	private Combobox statusDalamKeluarga;
	private MyDatebox tanggalLahir;
	private Textbox teleponOrangTua;
	private Textbox teleponSiswa;
	private Textbox teleponWali;
	private Textbox tempatLahir;
	private Textbox bahasa;
	private MyDoublebox berat;
	private Textbox golonganDarah;
	private Textbox hobby;
	private Textbox hp1ayah;
	private Textbox hp1ibu;
	private Textbox hp2ayah;
	private Textbox hp2ibu;
	private Textbox hp3ayah;
	private Textbox hp3ibu;
	private Intbox jumlahSaudaraKandung;
	private Intbox jumlahSaudaraTiri;
	private Combobox kewarganegaraan;
	private Textbox panggilan;
	private Combobox pendidikanAyah;
	private Combobox pendidikanIbu;
	private Combobox penghasilanAyah;
	private Combobox penghasilanIbu;
	private Textbox riwayatPenyakit;
	private MyDatebox tanggalLahirAyah;
	private MyDatebox tanggalLahirIbu;
	private Textbox tempatLahirAyah;
	private Textbox tempatLahirIbu;
	private MyDoublebox tinggi;
	private Combobox pendidikanWali;
	private Combobox penghasilanWali;
	private Textbox keterangan;
	private Box infoKampusDariMana;
	private Textbox keteranganInfoKampusDariMana;
	private Textbox namaTemanInfoKampusDariMana;
	private Textbox alamatAyah, alamatIbu, nik, kk, waAyah, waIbu, waWali, desaKelurahanSekolahAsal;

	private boolean edit = false;
	private boolean delete = false;

	private CalonSiswa calonSiswa;
	private MyToolbarbuttonConfig add;
	private AmbilDataNegaraBanbox negara;
	private Textbox tempatLahirWali;
	private MyDatebox tanggalLahirWali;
	private Textbox hp1wali;
	private Textbox hp2wali;
	private Textbox hp3wali;
	private Combobox gelombangPendaftaran;
	protected FotoCalonSiswa fotoCalonSiswa;
	private EventListener eventListener;
	private Combobox sekolah;
	private Combobox yayasan;

	private MyCheckboxConfig pernyataan;

	private List<Rows> subRowsVerifikasiNilaiParameter;

	private Rows subRowsVerifikasiKelengkapanCalonSiswa;

	private Row rowParameterTambahan;

	private ArrayList<Row> parameterRows;

	private ParameterTambahanPsbListener parameterTambahanListener;

	private Map<String, LampiranLain> lampiranLains = new HashMap<String, LampiranLain>();

	private Tbmuser tbmuser;

	private Textbox dusunCalon;

	private Textbox rt;

	private Textbox rw;

	private Textbox kodePos;

	private Textbox kelurahanCalon;

	private AmbilDataKecamatanBanbox kecamatanCalon;

	private Label propinsiCalon;

	private Label kotaCalon;

	private Textbox alamatSekolahAsal;

	private AmbilDataKecamatanBanbox kecamatanSekolahAsal;

	private Label kotaSekolahAsal;

	private Label propinsiSekolahAsal;

	private Combobox searchstatus;

	private Textbox searchnamaayah;
	private Textbox searchnamaibu;

	private GelombangPendaftaranPsb myGelombangPendaftaranPsb = null;

	private MyCheckboxConfig tampilkanYgSudahBayarDaftarUlang;
	private MyCheckboxConfig tampilkanYgBelumBayarDaftarUlang;

	private MyCheckboxConfig tampilkanYgSudahdapatNIM;
	private MyCheckboxConfig tampilkanYgBelumdapatNIM;

	private MyCheckboxConfig tampilkanYgSudahBayar;
	private MyCheckboxConfig tampilkanYgBelumBayar;

	private MyCheckboxConfig diterima;
	private MyCheckboxConfig terverifikasi;
	private MyCheckboxConfig mundur;
	private MyCheckboxConfig ditolak;
	private MyCheckboxConfig belum;

	public CalonSiswaAction() {

	}

	public CalonSiswaAction(GelombangPendaftaranPsb myGelombangPendaftaranPsb) {
		this.myGelombangPendaftaranPsb = myGelombangPendaftaranPsb;
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		if (execution.getParameter("jadwalPertemuanPSB") != null) {
			try {
				jadwalPertemuanPSBData = Long.parseLong(execution.getParameter("jadwalPertemuanPSB").trim());

				if (jadwalPertemuanPSBData != null) {
					filter.setVisible(false);
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		Comboitem diterima = new Comboitem("Diterima");
		Comboitem terverifikasi = new Comboitem("Terverifikasi");
		Comboitem ditolak = new Comboitem("Ditolak");
		Comboitem undur = new Comboitem("Mengundurkan Diri");
		Comboitem belum = new Comboitem("Blm Ditentukan");
		Comboitem semua = new Comboitem("Semua");

		searchstatus.appendChild(diterima);
		searchstatus.appendChild(terverifikasi);
		searchstatus.appendChild(ditolak);
		searchstatus.appendChild(undur);
		searchstatus.appendChild(belum);
		searchstatus.appendChild(semua);

		if (searchstatus != null) { searchstatus.setSelectedItem(semua); }
		if (searchstatus != null) { searchstatus.setReadonly(true); }

		Common.generateTahunAjaranDanSemua(searchta);

		Sekolah sekolah = SekolahUtil.getSekolah();

		if (searchgel != null) { searchgel.setWidth("90%"); }
		Common.insertComboDanSemua(searchgel, new String[] { "nama", "tahunAjaran" }, "keterangan",
				GelombangPendaftaranPsb.class,

				Restrictions.and(
						sekolah != null && sekolah.getId() != null ? Restrictions.eq("sekolah", sekolah)
								: Restrictions.sqlRestriction("true"),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

		);
		if (searchgel != null) { searchgel.setReadonly(true); }

		// FIX NPE: StatusAwalSiswa TIDAK punya properti Hibernate-mapped
		// "keterangan" (field keterangan di GeneralValueObject tak ter-mapping
		// krn bukan @MappedSuperclass, dan StatusAwalSiswa sendiri tak
		// redeclare kolom itu) -- metadata.getPropertyValue(o,"keterangan",...)
		// di CommonComboInsertHelper melempar NPE reflektif. Deskripsi "" aman
		// (fallback ke toString() entity, lihat insertComboItems).
		Common.insertComboDanSemua(searchSatusAwal, new String[] { "nama" }, "", StatusAwalSiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (searchSatusAwal != null) { searchSatusAwal.setReadonly(true); }

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		if (Common.bolehKonfigurasi("aktifkan_download_upload_calon_siswa", Konfigurasi.TIDAK_AKTIF)) {
			MyToolbarbuttonConfig upload = Common.uploadData(this, CalonSiswa.class, contents);
			if ((add != null && add.isVisible()) && edit && delete
					&& Common.bolehUploadDataKonfigurasi("hak_akses_upload_data_calon_siswa")) {
				Common.appendKeToolbar(upload, add, comp);
			}
		}

		cetakToolbarbutton = Common.cetakDataCustomButton(CalonSiswa.class, this, "Download Gen. NIS", "/img/excel.png",
				new String[] { "id", "noRegistrasi", "noUjian", "siswa.nomorInduk", "namaSiswa", "sekolah",
						"telahDiterima" });
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload Gen. NIS", "/img/excel.png");
		if (upload != null) { upload.setUpload(Common.ukuranFileUpload()); }
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							CommonPSB.uploadKelulusan(file, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
									Clients.clearBusy();
								}
							});
						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		Common.appendKeToolbar(upload, add, comp);
		upload.setVisible(Common.bolehKonfigurasi("aktifkan_tombol_upload_data_calon_siswa"));

		MyToolbarbuttonConfig downloadLampiran = new MyToolbarbuttonConfig("Lampiran", "/img/attachment-icon.png");
		downloadLampiran.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onDownloadLampiran(arg0);
			}
		});
		Common.appendKeToolbar(downloadLampiran, add, comp);

		EventListener eventListenerSekolah = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (searchPenjurusan != null) {
					Common.clear(searchPenjurusan);
					Sekolah s = (Sekolah) (searchsekolah.getSelectedItem() == null ? null
							: searchsekolah.getSelectedItem().getValue());
					System.out.println("s => " + s);
					searchPenjurusan.setReadonly(true);
					if (s != null && s.getId() != null) {
						try {
							HibernateUtil.currentSession().refresh(s);
							Set<PenjurusanSekolah> selectedPenjurusanSekolah = s.getPenjurusanSekolahs();
							for (PenjurusanSekolah o : selectedPenjurusanSekolah) {
								if (o.getAktif() && o.getTampilkanDiPpdb()) {
									Comboitem comboitem = new Comboitem();
									comboitem.setLabel(o.getNama());
									comboitem.setDescription(o.getKeterangan());
									comboitem.setValue(o);
									searchPenjurusan.appendChild(comboitem);
								}
							}

							Comboitem comboitem = new Comboitem();
							comboitem.setLabel("Semua Penjurusan");
							comboitem.setValue(null);
							searchPenjurusan.appendChild(comboitem);
							searchPenjurusan.setSelectedItem(comboitem);

							comboitem = new Comboitem();
							comboitem.setLabel("Belum Ditentukan Penjurusan");
							comboitem.setValue(new PenjurusanSekolah());
							searchPenjurusan.appendChild(comboitem);

							comboitem = new Comboitem();
							comboitem.setLabel("Sudah Ditentukan Penjurusan");
							comboitem.setValue(new PenjurusanSekolah(-1L, ""));
							searchPenjurusan.appendChild(comboitem);
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
					}
				}

			}
		};

		MyToolbarbuttonConfig singkronDenganMhs = new MyToolbarbuttonConfig("Semua diterima", "/img/svg/check2.svg");
		singkronDenganMhs.setVisible(Common.bolehKonfigurasi("tampilkan_tombol_semua_diterima"));
		singkronDenganMhs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin menerima semua siswa ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Semua Calon Siswa Diterima");

									final Label label = Common.displayLoadBar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											laporan.selesaikan(new EventListener() {
												@Override
												public void onEvent(Event event2) throws Exception {
													onSearchDefault(null);
												}
											});
										}
									});

									new Thread(new Runnable() {

										@SuppressWarnings("unchecked")
										@Override
										public void run() {
											try {

											List<Long> longs = initCriteria(true)
													.setProjection(Projections.property("id")).list();
											int size = longs.size();
											int index = 0;
											for (Long id : longs) {
												index++;
												try {
													CalonSiswa biodataCalonMahasiswa = (CalonSiswa) ConstantValues
															.ambil(CalonSiswa.class.getName(), id);

													String kunci = biodataCalonMahasiswa.getNoRegistrasi() + "-"
															+ biodataCalonMahasiswa.getNama();

													label.setValue("Singkronkan Calon Siswa "
															+ biodataCalonMahasiswa.getNoRegistrasi() + "-"
															+ biodataCalonMahasiswa.getNama() + " ("
															+ Common.numberFormat.get().format((index * 100.0) / size)
															+ "%)");
													try {
														Session session = HibernateUtil.currentNativeSession();
														session.refresh(biodataCalonMahasiswa);
														biodataCalonMahasiswa.setTelahDiterima(true);

														session.getTransaction().begin();
														session.update(biodataCalonMahasiswa);
														session.getTransaction().commit();

														// session.disconnect();
														if (session.isOpen()) {
															session.disconnect();
															session.close();
														}
														laporan.catatBerhasil(index - 1, kunci, "Sinkronisasi berhasil");
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/CalonSiswaAction.java:722");
														laporan.catatGagalDetail(index - 1, kunci, e);
													}

													HibernateUtil.closeSession();

												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
													laporan.catatGagalDetail(index - 1, "id=" + id, e);
												}

											}
																					} finally {
												label.setValue("");
												ais.database.hibernate.HibernateUtil.closeSession();
											}
										}
									}).start();

								}

							}
						});

			}
		});
		Common.appendKeToolbar(singkronDenganMhs, add, comp);

		singkronDenganMhs = new MyToolbarbuttonConfig("Singkron dg siswa", "/img/svg/check2.svg");
		singkronDenganMhs
				.setVisible(Common.bolehKonfigurasi("tampilkan_tombol_singkronkan_calon_dengan_siswa"));
		singkronDenganMhs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null) {
							MyMessageboxConfig.show("Tahun ajaran harus dipilih", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						String tahun = searchta.getSelectedItem().getValue().toString().split("/")[0];
						// FIX SQLGrammarException: kolom fisik pada tabel sekolah.calon_siswa
						// untuk relasi ke Siswa adalah "siswa_id" (lihat CalonSiswa.java
						// field "siswa" -> @JoinColumn(name = "siswa_id")), BUKAN "siswa".
						// Native SQL ini masih memakai nama kolom lama sebelum entity
						// direvisi memakai @JoinColumn eksplisit.
						String sql = "update sekolah.calon_siswa aa set siswa_id=\r\n"
								+ " (select max(bb.id) from sekolah.siswa bb where bb.tahun_masuk=aa.tahun_masuk and bb.nama_siswa=aa.nama_siswa and date(bb.tanggal_lahir)=date(aa.tanggal_lahir) \r\n"
								+ " and tahun_masuk=" + tahun + " and bb.sekolah_id=aa.sekolah_id) where aa.tahun_masuk="
								+ tahun + " and aa.sekolah_id is not null and tanggal_lahir is not null;";

						System.out.println("sql => " + sql);

						Session session = HibernateUtil.currentSession();
						session.createSQLQuery(sql).executeUpdate();

						sql = "update sekolah.siswa aa set calonsiswa=\r\n"
								+ " (select max(bb.id) from sekolah.calon_siswa bb where bb.tahun_masuk=aa.tahun_masuk and bb.nama_siswa=aa.nama_siswa and date(bb.tanggal_lahir)=date(aa.tanggal_lahir) \r\n"
								+ " and tahun_masuk=" + tahun + " and bb.sekolah_id=aa.sekolah_id) where aa.tahun_masuk="
								+ tahun + " and aa.sekolah_id is not null and tanggal_lahir is not null;";

						System.out.println("sql => " + sql);

						session.createSQLQuery(sql).executeUpdate();

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						});

					}
				});
			}
		});
		Common.appendKeToolbar(singkronDenganMhs, add, comp);

		singkronDenganMhs = new MyToolbarbuttonConfig("Singkron dg pemb.", "/img/svg/check2.svg");
		singkronDenganMhs.setVisible(
				Common.bolehKonfigurasi("tampilkan_tombol_singkronkan_calon_dengan_pembayaran"));
		singkronDenganMhs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Pembayaran Calon Siswa");

				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						laporan.selesaikan(new EventListener() {
							@Override
							public void onEvent(Event event2) throws Exception {
								onSearchDefault(null);
							}
						});
					}
				});

				new Thread(new Runnable() {

					@SuppressWarnings("unchecked")
					@Override
					public void run() {
						try {
						List<Long> longs = initCriteria(true).setProjection(Projections.property("id")).list();
						int size = longs.size();
						int index = 0;
						for (Long id : longs) {
							index++;

							CalonSiswa calonSiswa = (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(), id);
							if (calonSiswa != null && calonSiswa.getId() != null) {
								String kunci = calonSiswa.getNoRegistrasi() + "-" + calonSiswa.getNama();
								label.setValue("Singkronkan pembayaran " + calonSiswa.getNoRegistrasi() + "-"
										+ calonSiswa.getNama() + " ("
										+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");
								try {
									CalonSiswa.populate(calonSiswa);
									laporan.catatBerhasil(index - 1, kunci, "Sinkronisasi berhasil");
								} catch (Exception ePerItem) {
									Common.tampilErrorJikaAdmin(ePerItem);
									laporan.catatGagalDetail(index - 1, kunci, ePerItem);
								}
							}
						}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							laporan.tambahCatatan("Proses sinkronisasi terhenti total (di luar per-calon siswa): "
									+ ais.common.LaporanUpload.detailTeknisException(e));
						} finally {
							label.setValue("");
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();

			}
		});
		Common.appendKeToolbar(singkronDenganMhs, add, comp);

		searchsekolah.addEventListener("onChange", eventListenerSekolah);
		Common.createDefaultTimer(eventListenerSekolah);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		downloadLampiran = new MyToolbarbuttonConfig("Nilai Raport", "/img/attachment-icon.png");
		downloadLampiran.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onDownloadNilaiRaport(arg0);
			}
		});
		Common.appendKeToolbar(downloadLampiran, add, comp);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiCalonSiswaHelper revisiHelper = new RevisiCalonSiswaHelper(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(null);
					}
				});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		if (button != null) { button.setParent(add.getParent()); }
	        FilterLanjutHelper.setup(comp);
		if (mainTabbox != null) {
			mainTabbox.addEventListener("onSelect", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					Tab sel = mainTabbox.getSelectedTab();
					if (sel != null && "tabDasborKeuangan".equals(sel.getId())) {
						Tabpanel panel = (Tabpanel) sel.getLinkedPanel();
						if (panel != null && panel.getChildren().isEmpty()) {
							Include inc = new Include();
							inc.setSrc("/WEB-INF/z/x/y/pages/master/sekolah/dasbor_keuangan_siswa.zul");
							inc.setParent(panel);
						}
					}
				}
			});
		}
}

	@SuppressWarnings("unchecked")
	public void onDownloadLampiran(Event event) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				List<CalonSiswa> calonSiswas = initCriteria(true).list();
				File fileFolderLampiran = new File(
						"/opt/ecampus/lampiran_" + ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis());
				fileFolderLampiran.mkdirs();
				System.out.println("fileFolderLampiran => " + fileFolderLampiran.getAbsolutePath());

				for (CalonSiswa calonSiswa : calonSiswas) {
					File fileFolderCalon = new File(fileFolderLampiran.getAbsolutePath() + "/"
							+ URLEncoder.encode(calonSiswa.getNoRegistrasi() + "_" + calonSiswa.getNama(), "UTF-8"));
					fileFolderCalon.mkdirs();
					System.out.println("fileFolderCalon => " + fileFolderCalon.getAbsolutePath());

					try {
						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

						FotoCalonSiswa fotocalonSiswa = (FotoCalonSiswa) streamingSession
								.createCriteria(FotoCalonSiswa.class)
								.add(Restrictions.eq("calonSiswa", calonSiswa.getId())).setMaxResults(1).uniqueResult();
						if (fotocalonSiswa != null && fotocalonSiswa.getGdrive() != null) {
							File fileCopy = new File(fileFolderLampiran.getAbsolutePath() + "/"
									+ calonSiswa.getNoRegistrasi() + "_" + calonSiswa.getNama() + "_FOTO_.txt");
							ais.common.BacaTulisUtil.tulis(fileCopy, fotocalonSiswa.forwardGDriveUrl());
						} else if (fotocalonSiswa != null) {
							File fileFoto = fotocalonSiswa.ambilFile();
							File fileCopy = new File(fileFolderCalon.getAbsolutePath() + "/FOTO_" + fileFoto.getName());
							System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
							FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
							FileInputStream fileInputStream = new FileInputStream(fileFoto);
							IOUtils.copyLarge(fileInputStream, fileOutputStream);
							fileInputStream.close();
							fileOutputStream.close();
						}

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e1) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/CalonSiswaAction.java:949");
					}

					String params = calonSiswa.getParameterTambahanInds();
					System.out.println("params => " + params);
					String[] spl = params.split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						String jenis = value.length > 0 ? value[0].trim() : "";
						String val = value.length > 1 ? value[1].trim() : "";
						String url = value.length > 2 ? value[2].trim() : "";

						System.out.println("jenis => " + jenis + ", val => " + val + ", url => " + url);

						if (!url.trim().isEmpty()) {

							File fileCopy = null;

							LampiranLain lam = LampiranLain.ambil(calonSiswa.getId(), jenis);

							System.out.println("jenis => " + jenis + ", lam => " + lam);

							if (lam != null && lam.getGdrive() != null) {
								fileCopy = new File(fileFolderCalon.getAbsolutePath() + "/" + lam.getJenis() + "_"
										+ calonSiswa.getNoRegistrasi() + "_" + calonSiswa.getNama() + ".txt");
								ais.common.BacaTulisUtil.tulis(fileCopy, lam.forwardGDriveUrl());
							} else if (lam != null) {

								File file = lam.ambilFile();
								fileCopy = new File(fileFolderCalon.getAbsolutePath() + "/"
										+ URLEncoder.encode(val, "UTF-8") + "_" + file.getName());
								System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
								FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
								FileInputStream fileInputStream = new FileInputStream(file);
								IOUtils.copyLarge(fileInputStream, fileOutputStream);
								fileInputStream.close();
								fileOutputStream.close();
							}

						}
					}

					Session session = HibernateUtil.currentSession();
					GelombangPendaftaranPsb gel = calonSiswa.getGelombangPendaftaranPsb();
					if (gel != null) {
						session.refresh(gel);
					}
					Set<VerifikasiKelengkapanCalonSiswa> verifikasiKelengkapanCalonSiswas = gel == null
							? new TreeSet<VerifikasiKelengkapanCalonSiswa>()
							: new TreeSet<VerifikasiKelengkapanCalonSiswa>(gel.getVerifikasiKelengkapanCalonSiswas());
					for (final VerifikasiKelengkapanCalonSiswa verifikasiKelengkapanCalonSiswa : verifikasiKelengkapanCalonSiswas) {

						CalonSiswaPunyaVerifikasiBerkas calonSiswaPunyaVerifikasiBerkas = (CalonSiswaPunyaVerifikasiBerkas) session
								.createCriteria(CalonSiswaPunyaVerifikasiBerkas.class)
								.add(Restrictions.eq("verifikasiKelengkapanCalonSiswa",
										verifikasiKelengkapanCalonSiswa))
								.add(Restrictions.eq("calonSiswa", calonSiswa)).setMaxResults(1).uniqueResult();

						if (calonSiswaPunyaVerifikasiBerkas == null) {
							calonSiswaPunyaVerifikasiBerkas = new CalonSiswaPunyaVerifikasiBerkas();
							calonSiswaPunyaVerifikasiBerkas.setCalonSiswa(calonSiswa);
							calonSiswaPunyaVerifikasiBerkas
									.setVerifikasiKelengkapanCalonSiswa(verifikasiKelengkapanCalonSiswa);
							Common.refreshSaveOrUpdate(session, calonSiswaPunyaVerifikasiBerkas);
						}

						String jenis = CalonSiswaPunyaVerifikasiBerkas.class.getName();

						LampiranLain lam = LampiranLain.ambil(calonSiswaPunyaVerifikasiBerkas.getId(), jenis);

						System.out.println("jenis => " + jenis + ", lam => " + lam);

						if (lam != null) {

							File file = lam.ambilFile();
							File fileCopy = new File(
									fileFolderCalon.getAbsolutePath() + "/"
											+ URLEncoder.encode(
													calonSiswa.getNoRegistrasi() + "_" + calonSiswa.getNama(), "UTF-8")
											+ "_" + file.getName());
							System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
							FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
							FileInputStream fileInputStream = new FileInputStream(file);
							IOUtils.copyLarge(fileInputStream, fileOutputStream);
							fileInputStream.close();
							fileOutputStream.close();
						}

					}

				}

				File fileFolderLampiranZip = new File(fileFolderLampiran.getAbsolutePath() + ".zip");
				Common.zipDir(fileFolderLampiranZip.getAbsolutePath(), fileFolderLampiran.getAbsolutePath());
				Filedownload.save(fileFolderLampiranZip, "application/zip");

			}
		}, "Harap tunggu.. sedang melakukan proses download lampiran..");

	}

	@SuppressWarnings("unchecked")
	public void onDownloadNilaiRaport(Event event) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				List<CalonSiswa> calonSiswasTemp = initCriteria(true).list();

				Map<GelombangPendaftaranPsb, List<CalonSiswa>> map = new HashMap<GelombangPendaftaranPsb, List<CalonSiswa>>();

				for (CalonSiswa calonSiswa : calonSiswasTemp) {
					if (calonSiswa.getGelombangPendaftaranPsb() != null) {
						List<CalonSiswa> s = map.get(calonSiswa.getGelombangPendaftaranPsb());
						if (s == null) {
							s = new ArrayList<CalonSiswa>();
							map.put(calonSiswa.getGelombangPendaftaranPsb(), s);
						}
						s.add(calonSiswa);
					}
				}

				File fileFolderLampiran = new File(
						"/opt/ecampus/lampiran_raport_" + ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis());
				fileFolderLampiran.mkdirs();
				System.out.println("fileFolderLampiran => " + fileFolderLampiran.getAbsolutePath());

				Session session = HibernateUtil.currentSession();

				for (GelombangPendaftaranPsb gel : map.keySet()) {

					File filename = new File(fileFolderLampiran.getAbsolutePath() + "/"
							+ URLEncoder.encode(gel.getNama() + "_" + gel.getId(), "UTF-8") + ".xlsx");
					filename.createNewFile();
					List<CalonSiswa> calonSiswas = map.get(gel);

					XSSFWorkbook workbook = new XSSFWorkbook();
					XSSFSheet sheet = workbook.createSheet(Common.getBahasaConfig("Nilai Raport"));
					XSSFRow rowhead = sheet.createRow((short) 0);

					rowhead.createCell(0).setCellValue(Common.getBahasaConfig("No.Reg"));
					rowhead.createCell(1).setCellValue(Common.getBahasaConfig("Nama"));
					rowhead.createCell(2).setCellValue(Common.getBahasaConfig("Matapelajaran"));

					int indexData = 3;
					for (String nilaikelas : gel.getKelasVerifikasiRapor().split(";")) {
						if (!nilaikelas.trim().isEmpty()) {

							rowhead.createCell(indexData).setCellValue(Common.getBahasaConfig("KKM"));
							indexData++;

							String[] ca = StringUtils.split(nilaikelas, ":");
							String kel = ca.length > 0 ? ca[0] : "";
							String sem = ca.length > 1 ? ca[1] : "";
							rowhead.createCell(indexData)
									.setCellValue("Kls:" + kel + (sem.isEmpty() ? "" : ", Smt:" + sem));

							indexData++;

						}
					}

					rowhead.createCell(indexData).setCellValue(Common.getBahasaConfig("Rata-Rata Nilai"));
					indexData++;

					List<MatapelajaranSekolah> matapelajaranSekolahs = ConstantValues
							.simpleList(
									session.createCriteria(GelombangPendaftaranPsbPunyaMatapelajaran.class)
											.setProjection(Projections.property("matapelajaranSekolah.id"))
											.createAlias("matapelajaranSekolah", "matapelajaranSekolah")
											.add(Restrictions.eq("gelombangPendaftaranPsb", gel))
											.add(Restrictions.eq("matapelajaranSekolah.aktif", true))
											.addOrder(Order.asc("matapelajaranSekolah.nama")),
									MatapelajaranSekolah.class, false);

					int col = 1;
					for (CalonSiswa calonSiswa : calonSiswas) {
						Double nilaiSemua = 0.0;
						Double jumlahSemua = 0.0;
						for (MatapelajaranSekolah matapelajaranSekolah : matapelajaranSekolahs) {

							XSSFRow row = sheet.createRow((short) col);
							col++;

							row.createCell(0).setCellValue(calonSiswa.getNoRegistrasi());
							row.createCell(1).setCellValue(calonSiswa.getNama());

							row.createCell(2).setCellValue(matapelajaranSekolah.getNama());

							CalonSiswaPunyaVerifikasiMatapelajaran calonSiswaPunyaVerifikasiMatapelajaranTemp = (CalonSiswaPunyaVerifikasiMatapelajaran) session
									.createCriteria(CalonSiswaPunyaVerifikasiMatapelajaran.class)
									.add(Restrictions.eq("matapelajaranSekolah", matapelajaranSekolah))
									.add(Restrictions.eq("calonSiswa", calonSiswa)).setMaxResults(1).uniqueResult();

							if (calonSiswaPunyaVerifikasiMatapelajaranTemp == null) {
								calonSiswaPunyaVerifikasiMatapelajaranTemp = new CalonSiswaPunyaVerifikasiMatapelajaran();
								calonSiswaPunyaVerifikasiMatapelajaranTemp.setCalonSiswa(calonSiswa);
								calonSiswaPunyaVerifikasiMatapelajaranTemp
										.setMatapelajaranSekolah(matapelajaranSekolah);
								Common.refreshSaveOrUpdate(session, calonSiswaPunyaVerifikasiMatapelajaranTemp);
							}

							Double nilaiSemuaData = 0.0;
							Double jumlahSemuaData = 0.0;

							indexData = 3;
							CalonSiswaPunyaVerifikasiMatapelajaran calonSiswaPunyaVerifikasiMatapelajaran = calonSiswaPunyaVerifikasiMatapelajaranTemp;
							for (String nilaikelas : gel.getKelasVerifikasiRapor().split(";")) {
								if (!nilaikelas.trim().isEmpty()) {
									Double kkm = calonSiswaPunyaVerifikasiMatapelajaran.ambilKkm(nilaikelas.trim());
									Double nilai = calonSiswaPunyaVerifikasiMatapelajaran.ambilNilai(nilaikelas.trim());

									nilaiSemua += nilai;
									jumlahSemua++;

									nilaiSemuaData += nilai;
									jumlahSemuaData++;

									row.createCell(indexData).setCellValue(kkm);
									indexData++;

									row.createCell(indexData).setCellValue(nilai);
									indexData++;
								}
							}

							row.createCell(indexData).setCellValue(nilaiSemuaData / jumlahSemuaData);
							indexData++;

						}

						XSSFRow row = sheet.createRow((short) col);
						col++;

						row.createCell(0).setCellValue("");
						row.createCell(1).setCellValue("Rata-Rata " + calonSiswa.getNama());
						row.createCell(2).setCellValue("");

						indexData = 3;
						for (String nilaikelas : gel.getKelasVerifikasiRapor().split(";")) {
							if (!nilaikelas.trim().isEmpty()) {
								row.createCell(indexData).setCellValue("");
								indexData++;

								row.createCell(indexData).setCellValue("");
								indexData++;
							}
						}

						row.createCell(indexData).setCellValue(nilaiSemua / jumlahSemua);
						indexData++;
					}

					try {
						FileOutputStream fileOut = new FileOutputStream(filename);
						workbook.write(fileOut);
						fileOut.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						Common.tampilErrorJikaAdmin(e);
					}
				}

				File fileFolderLampiranZip = new File(fileFolderLampiran.getAbsolutePath() + ".zip");
				Common.zipDir(fileFolderLampiranZip.getAbsolutePath(), fileFolderLampiran.getAbsolutePath());
				Filedownload.save(fileFolderLampiranZip, "application/zip");
			}
		}, "Harap tunggu.. sedang melakukan proses download lampiran..");

	}

	class CalonSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final CalonSiswa calonSiswa = (CalonSiswa) arg1;

			MyDetail detail = new MyDetail();
			detail.setOpen(true);
			detail.setParent(arg0);
			final Hbox hbox = new Hbox();
			hbox.setParent(detail);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					GelombangPendaftaranPsb gel = calonSiswa.getGelombangPendaftaranPsb();

					Session session = HibernateUtil.currentSession();
					session.refresh(gel);
					Set<VerifikasiKelengkapanCalonSiswa> verifikasiKelengkapanCalonSiswasTemp = gel
							.getVerifikasiKelengkapanCalonSiswas();

					List<VerifikasiKelengkapanCalonSiswa> verifikasiKelengkapanCalonSiswas = new ArrayList<VerifikasiKelengkapanCalonSiswa>(
							verifikasiKelengkapanCalonSiswasTemp);

					try {
						Collections.sort(verifikasiKelengkapanCalonSiswas);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/CalonSiswaAction.java:1246");
						// TODO: handle exception
					}

					final List<VerifikasiKelengkapanCalonSiswa> upload = new ArrayList<VerifikasiKelengkapanCalonSiswa>();
					final List<VerifikasiKelengkapanCalonSiswa> belumupload = new ArrayList<VerifikasiKelengkapanCalonSiswa>();
					final List<VerifikasiKelengkapanCalonSiswa> lolos = new ArrayList<VerifikasiKelengkapanCalonSiswa>();
					final List<VerifikasiKelengkapanCalonSiswa> belum = new ArrayList<VerifikasiKelengkapanCalonSiswa>();

					for (VerifikasiKelengkapanCalonSiswa verifikasiKelengkapanCalonSiswa : verifikasiKelengkapanCalonSiswas) {
						if (verifikasiKelengkapanCalonSiswa.getAktif()) {
							CalonSiswaPunyaVerifikasiBerkas calonSiswaPunyaVerifikasiBerkas = (CalonSiswaPunyaVerifikasiBerkas) session
									.createCriteria(CalonSiswaPunyaVerifikasiBerkas.class)
									.add(Restrictions.eq("verifikasiKelengkapanCalonSiswa",
											verifikasiKelengkapanCalonSiswa))
									.add(Restrictions.eq("calonSiswa", calonSiswa)).setMaxResults(1).uniqueResult();

							if (calonSiswaPunyaVerifikasiBerkas == null) {
								calonSiswaPunyaVerifikasiBerkas = new CalonSiswaPunyaVerifikasiBerkas();
								calonSiswaPunyaVerifikasiBerkas.setCalonSiswa(calonSiswa);
								calonSiswaPunyaVerifikasiBerkas
										.setVerifikasiKelengkapanCalonSiswa(verifikasiKelengkapanCalonSiswa);
								Common.refreshSaveOrUpdate(session, calonSiswaPunyaVerifikasiBerkas);
							}

							FileFotoLain lampiranLain = FileFotoLain.ambil(calonSiswaPunyaVerifikasiBerkas.getId(),
									CalonSiswaPunyaVerifikasiBerkas.class.getName(), LampiranLain.class);
							if (lampiranLain != null) {
								upload.add(verifikasiKelengkapanCalonSiswa);
							} else {
								belumupload.add(verifikasiKelengkapanCalonSiswa);
							}

							if (calonSiswaPunyaVerifikasiBerkas.getVerified()) {
								lolos.add(verifikasiKelengkapanCalonSiswa);
							} else {
								belum.add(verifikasiKelengkapanCalonSiswa);
							}
						}
					}

					Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig(belumupload.size() + " belum upload",
							"/img/Record-Normal-icon.png");
					toolbarbutton.setTooltiptext("Belum upload : " + belumupload);
					toolbarbutton.setStyle("font-size:9px;");
					toolbarbutton.setDisabled(belumupload.isEmpty());
					toolbarbutton.setParent(hbox);
					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							displayVerifikasi(calonSiswa, belumupload, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
								}
							});
						}
					});

					toolbarbutton = new MyToolbarbuttonConfig(upload.size() + " telah upload",
							"/img/attachment-icon.png");
					toolbarbutton.setTooltiptext("Telah upload : " + upload);
					toolbarbutton.setDisabled(upload.isEmpty());
					toolbarbutton.setStyle("font-size:9px;");
					toolbarbutton.setParent(hbox);
					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							displayVerifikasi(calonSiswa, upload, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
								}
							});
						}
					});

					toolbarbutton = new MyToolbarbuttonConfig(belum.size() + " belum verifikasi",
							"/img/Check-icon.png");
					toolbarbutton.setTooltiptext("Belum verifikasi : " + belum);
					toolbarbutton.setDisabled(belum.isEmpty());
					toolbarbutton.setStyle("font-size:9px;");
					toolbarbutton.setParent(hbox);
					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							displayVerifikasi(calonSiswa, belum, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
								}
							});
						}
					});

					toolbarbutton = new MyToolbarbuttonConfig(lolos.size() + " telah verifikasi",
							"/img/Cute-Ball-Go-icon.png");
					toolbarbutton.setTooltiptext("Telah verifikasi : " + lolos);
					toolbarbutton.setStyle("font-size:9px;");
					toolbarbutton.setDisabled(lolos.isEmpty());
					toolbarbutton.setParent(hbox);
					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							displayVerifikasi(calonSiswa, lolos, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
								}
							});
						}
					});

				}
			});

			CommonMedia.tampilkanGambarKecil(calonSiswa).setParent(arg0);
			if (calonSiswa.getId() == null) {
				calonSiswa.setNoRegistrasi(CommonPSB.generateNoRegistrasi(calonSiswa));
				Common.refreshSaveOrUpdate(calonSiswa);
			}
			new Label(calonSiswa.getNomorInduk()).setParent(arg0);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(CalonSiswa.class, calonSiswa, calonSiswa.getNama())).setParent(arg0);
			a.appendChild(new MyLabelAgakKecil(Common.dateFormat.get().format(calonSiswa.getTanggalPendaftaran())));

			new Label(calonSiswa.getTempatLahir() + ", " + (calonSiswa.getTanggalLahir() == null ? ""
					: Common.dateFormat1.get().format(calonSiswa.getTanggalLahir()))).setParent(arg0);

			a = new Vbox();
			a.setParent(arg0);
			new Label(calonSiswa.getGelombangPendaftaranPsb() == null ? ""
					: calonSiswa.getGelombangPendaftaranPsb().getNama()
							+ (calonSiswa.getPenjurusanSekolah() == null ? ""
									: " (" + calonSiswa.getPenjurusanSekolah().getNama() + ")"))
					.setParent(a);
			new Label(calonSiswa.getSekolah() == null ? "" : calonSiswa.getSekolah().getNama()).setParent(a);

			if (calonSiswa.getOrangTuaPegawai() != null) {
				new Label(calonSiswa.getOrangTuaPegawai().getNama()).setParent(arg0);
			} else {
				a = new Vbox();
				a.setParent(arg0);
				new Label(calonSiswa.getNamaAyah()).setParent(a);
				new Label(calonSiswa.getNamaIbu()).setParent(a);
			}

			a = new Vbox();
			a.setParent(arg0);
			new Label(calonSiswa.getStatusDalamKeluarga()).setParent(a);
			new Label(calonSiswa.getStatusAwalSiswa() == null ? "" : calonSiswa.getStatusAwalSiswa().getNama())
					.setParent(a);
			new Label(calonSiswa.getSiswa() == null ? "" : calonSiswa.getSiswa().getNomorInduk()).setParent(a);
			new Label(calonSiswa.getSiswa() == null ? "" : calonSiswa.getSiswa().getNomorIndukNasional()).setParent(a);

			if (!calonSiswa.getRiwayatPembayaranInfo().isEmpty()) {

				String _riwayat = calonSiswa.getRiwayatPembayaranInfo();
				String[] _parts = _riwayat.split(",");
				int _nItem = Math.max(1, _parts.length / 3);
				A aa = new A("Riwayat pembayaran (" + _nItem + " item)");
				aa.setStyle("font-size:9px;color:#1d4ed8;text-decoration:underline;cursor:pointer;white-space:nowrap;");
				aa.setTooltiptext(_riwayat.length() > 400 ? _riwayat.substring(0, 397) + "..." : _riwayat);
				aa.setParent(a);
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						EventListener eventListener = (EventListener) Common
								.cetakDataCustomButton(PembayaranSiswaDetail.class, new DataCriteriaWithColumn() {

									@Override
									public Object[] initCriteria(boolean order) {

										try {

											Session session = HibernateUtil.currentSession();
											Criteria criteria = session.createCriteria(PembayaranSiswaDetail.class)
													.createAlias("pembayaranSiswa", "pembayaranSiswa")
													.add(Restrictions.eq("pembayaranSiswa.calonSiswa", calonSiswa))
													.addOrder(Order.asc("id"));

											return new Object[] { criteria,
													new String[] { "pembayaranSiswa.tanggal", "tagihan.pengaturanBiaya",
															"pembayaranSiswa.validator", "itemBiayaSekolah.kode",
															"itemBiayaSekolah.nama", "tagihan.tahunAjaran",
															"tagihan.tahun", "tagihan.bulan", "tagihan.kelasSiswa",
															"tagihan.calonSiswa", "nominal" } };

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}
										return null;
									}

								}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
										new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "" })
								.getAttribute("eventListener");

						eventListener.onEvent(null);

					}
				});

			}

			a = new Vbox();
			a.setParent(arg0);
			new Label(calonSiswa.getKeterangan()).setParent(a);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			if (calonSiswa.getGelombangPendaftaranPsb() != null
					&& calonSiswa.getGelombangPendaftaranPsb().getOtomatisDiterimaKetikaSudahBayarReg()
					&& !calonSiswa.getRiwayatPembayaranPendaftaran().isEmpty()) {
				new MyLabelAgakKecil("Otomatis diterima karena telah melakukan pembayaran").setParent(vbox);

				final MyCheckboxConfig mengundurkanDiri = new MyCheckboxConfig("Mengundurkan diri");
				mengundurkanDiri.setChecked(calonSiswa.getMengundurkanDiri());
				mengundurkanDiri.setParent(vbox);
				mengundurkanDiri.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						calonSiswa.setMengundurkanDiri(mengundurkanDiri.isChecked());
						Common.refreshSaveOrUpdate(calonSiswa);
					}
				});

			} else {

				Radio diterima = new Radio("Diterima");
				Radio terverifikasi = new Radio("Terverifikasi");
				Radio ditolak = new Radio("Ditolak");
				Radio undur = new Radio("Mengundurkan Diri");
				Radio belum = new Radio("Blm Ditentukan");

				diterima.setStyle("font-size:8px;");
				ditolak.setStyle("font-size:8px;");
				undur.setStyle("font-size:8px;");
				belum.setStyle("font-size:8px;");
				terverifikasi.setStyle("font-size:8px;");

				final Radiogroup status = new Radiogroup();
				status.setOrient("vertical");

				status.appendChild(diterima);
				status.appendChild(terverifikasi);
				status.appendChild(ditolak);
				status.appendChild(undur);
				status.appendChild(belum);

				belum.setDisabled(!edit);
				diterima.setDisabled(!edit);
				terverifikasi.setDisabled(!edit);
				undur.setDisabled(!edit);
				ditolak.setDisabled(!edit);
				status.setParent(vbox);

				if (calonSiswa.getTerverifikasi()) {
					status.setSelectedItem(terverifikasi);
				} else if (calonSiswa.getTelahDiterima()) {
					status.setSelectedItem(diterima);
				} else if (calonSiswa.getDitolak()) {
					status.setSelectedItem(ditolak);
				} else if (calonSiswa.getMengundurkanDiri()) {
					status.setSelectedItem(undur);
				} else {
					status.setSelectedItem(belum);
				}

				EventListener e = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (status.getSelectedItem().getLabel().equalsIgnoreCase("Diterima")) {

							Session session = HibernateUtil.currentSession();

							int count = ((Number) session.createCriteria(CalonSiswa.class)
									.add(Restrictions.eq("gelombangPendaftaranPsb",
											calonSiswa.getGelombangPendaftaranPsb()))
									.add(Restrictions.eq("telahDiterima", true)).setProjection(Projections.rowCount())
									.uniqueResult()).intValue();

							if (count >= calonSiswa.getGelombangPendaftaranPsb().getKuotaDiterima()) {

								MyMessageboxConfig.show("Jumlah kuota siswa yang diterima telah penuh", "Peringatan",
										MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												onSearchDefault(arg0);
											}
										});

								return;
							}

						}

						if (status.getSelectedItem().getLabel().equalsIgnoreCase("Diterima")) {
							calonSiswa.setTelahDiterima(true);
							calonSiswa.setDitolak(false);
							calonSiswa.setMengundurkanDiri(false);
							calonSiswa.setTerverifikasi(false);
						} else if (status.getSelectedItem().getLabel().equalsIgnoreCase("Terverifikasi")) {
							calonSiswa.setTelahDiterima(false);
							calonSiswa.setDitolak(false);
							calonSiswa.setMengundurkanDiri(false);
							calonSiswa.setTerverifikasi(true);
						} else if (status.getSelectedItem().getLabel().equalsIgnoreCase("Ditolak")) {
							calonSiswa.setTelahDiterima(false);
							calonSiswa.setDitolak(true);
							calonSiswa.setMengundurkanDiri(false);
							calonSiswa.setTerverifikasi(false);
						} else if (status.getSelectedItem().getLabel().equalsIgnoreCase("Mengundurkan Diri")) {
							calonSiswa.setTelahDiterima(false);
							calonSiswa.setDitolak(false);
							calonSiswa.setMengundurkanDiri(true);
							calonSiswa.setTerverifikasi(false);
						} else if (status.getSelectedItem().getLabel().equalsIgnoreCase("Blm Ditentukan")) {
							calonSiswa.setTelahDiterima(false);
							calonSiswa.setDitolak(false);
							calonSiswa.setMengundurkanDiri(false);
							calonSiswa.setTerverifikasi(false);
						}
						Common.refreshSaveOrUpdate(calonSiswa);

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						});
					}
				};
				diterima.addEventListener("onClick", e);
				terverifikasi.addEventListener("onClick", e);
				ditolak.addEventListener("onClick", e);
				undur.addEventListener("onClick", e);
				belum.addEventListener("onClick", e);
			}

			new MyLabelAgakKecil("Keterangan:").setParent(vbox);
			final Textbox keterangan = new Textbox(calonSiswa.getKeterangan());
			keterangan.setCols(14);
			keterangan.setRows(2);
			keterangan.setParent(vbox);

			keterangan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					calonSiswa.setKeterangan(keterangan.getValue());
					Common.refreshSaveOrUpdate(calonSiswa);
				}
			});

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			Hbox tempEditDelete = Common.copyEditDeleteButtons(edit, edit, delete, calonSiswa, CalonSiswaAction.this,
					true);
			aksiButtons.addAll(ais.ui.util.UIHelper.ambilItemAksi(tempEditDelete));

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("No Reg.", "/img/print.png");
			button.setOrient("vertical");
			button.setStyle("font-size:9px;");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					onCetakKartu(calonSiswa, false);
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Notif.", "/img/print.png");
			button.setOrient("vertical");
			button.setStyle("font-size:9px;");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					onCetakKartu(calonSiswa, true);
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Print", "/img/print.png");
			button.setOrient("vertical");
			button.setStyle("font-size:9px;");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportPsb.onCetakCalonSiswa(calonSiswa);
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Ujian", "/img/print.png");
			button.setOrient("vertical");
			button.setStyle("font-size:9px;");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (calonSiswa.getNoUjian() != null && !calonSiswa.getNoUjian().trim().isEmpty()) {
						CommonReportPsb.onCetakKartuUjianPSB(calonSiswa, calonSiswa.getNoUjian());
						return;
					}

					String noUjianGenerated = CommonPSB.generateNoUjian(calonSiswa);

					if (!noUjianGenerated.trim().isEmpty()) {
						CommonReportPsb.onCetakKartuUjianPSB(calonSiswa, noUjianGenerated);
						onSearchDefault(null);
					}

				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("NIS", "/img/svg/user-circle.svg");
			button.setOrient("vertical");
			button.setStyle("font-size:9px;");
			button.setVisible(edit && calonSiswa.getTelahDiterima());
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub
					NisGenerator nisGenerator = (NisGenerator) Class.forName(
							Common.getKonfigurasi("class_untuk_generate_nis", DefaultNisGenerator.class.getName())
									.getNilai().trim())
							.newInstance();

					CommonPSB.onGenerateNis(calonSiswa, nisGenerator);

				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Ket. Lulus", "/img/svg/check-circled-outline.svg");
			button.setVisible(calonSiswa.getTelahDiterima());
			button.setOrient("vertical");
			button.setStyle("font-size:9px;");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportHelper.onCetakSuratKeteranganLulus(calonSiswa);

				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Perny. Ortu.", "/img/print.png");
			button.setOrient("vertical");
			button.setStyle("font-size:9px;");
			button.setVisible(calonSiswa.getTelahDiterima());
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportPsb.onCetakPernyataanOrtu(calonSiswa);
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Perny. Siswa.", "/img/print.png");
			button.setOrient("vertical");
			button.setStyle("font-size:9px;");
			button.setVisible(calonSiswa.getTelahDiterima());
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportPsb.onCetakPernyataanSiswa(calonSiswa);
				}
			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	public static class CheckKesamaan implements EventListener {

		private GelombangPendaftaranPsb gelombangPendaftaranPsb;
		private MyDatebox tanggalLahir;
		private Textbox namaSiswa;
		private CalonSiswa calonSiswa;
		private Textbox namaIbu;
		private MyWindow addWindow;
		private EventListener eventListener;

		public CheckKesamaan(CalonSiswa calonSiswa, GelombangPendaftaranPsb gelombangPendaftaranPsb,
				MyDatebox tanggalLahir, Textbox namaSiswa, Textbox namaIbu, MyWindow addWindow,
				EventListener eventListener) {
			this.gelombangPendaftaranPsb = gelombangPendaftaranPsb;
			this.tanggalLahir = tanggalLahir;
			this.namaSiswa = namaSiswa;
			this.calonSiswa = calonSiswa;
			this.namaIbu = namaIbu;
			this.addWindow = addWindow;
			this.eventListener = eventListener;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {

			if (gelombangPendaftaranPsb != null && tanggalLahir.getValue() != null
					&& !namaSiswa.getValue().trim().isEmpty()
					&& (namaIbu == null || !namaIbu.getValue().trim().isEmpty())) {
				Session session = HibernateUtil.currentSession();
				int count = ((Number) session.createCriteria(CalonSiswa.class)
						.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
						.add(Restrictions.eq("gelombangPendaftaranPsb", gelombangPendaftaranPsb))
						.setProjection(Projections.rowCount())

						.add(Restrictions.ilike("namaSiswa", namaSiswa.getValue().trim(), MatchMode.EXACT))

						.add(namaIbu == null ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("namaIbu", namaIbu.getValue().trim(), MatchMode.EXACT))

						.add(calonSiswa.getId() != null ? Restrictions.ne("id", calonSiswa.getId())
								: Restrictions.sqlRestriction("true"))

						.add(Restrictions.eq("tanggalLahir", tanggalLahir.getValue()))

						.uniqueResult()).intValue();

				System.out.println("count = " + count + ", namaSiswa = " + namaSiswa.getValue() + ", namaIbu = "
						+ (namaIbu == null ? "" : namaIbu.getValue()) + ", tanggalLahir = "
						+ Common.dateFormat4.get().format(tanggalLahir.getValue()));

				if (count > 0) {

					MyMessageboxConfig.show(
							"Data pendaftaran sebagai berikut :\n" + "Nama : " + namaSiswa.getValue() + "\n"
									+ "Tanggal Lahir : " + Common.dateFormat2.get().format(tanggalLahir.getValue()) + "\n"
									+ (namaIbu == null ? "" : "Nama Ibu : " + namaIbu.getValue()) + "\n"
									+ "telah terdaftar sebelumnya.\n" + "Apakah yakin ingin mengubah data ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {

									addWindow.setVisible(false);

									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												Session session = HibernateUtil.currentSession();
												CalonSiswa calonSiswa1 = (CalonSiswa) session
														.createCriteria(CalonSiswa.class)
														.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))

														.add(Restrictions.eq("gelombangPendaftaranPsb",
																gelombangPendaftaranPsb))

														.add(Restrictions.ilike("namaSiswa",
																namaSiswa.getValue().trim(), MatchMode.EXACT))

														.add(namaIbu == null ? Restrictions.sqlRestriction("true")
																: Restrictions.ilike("namaIbu",
																		namaIbu.getValue().trim(), MatchMode.EXACT))

														.add(calonSiswa.getId() != null
																? Restrictions.ne("id", calonSiswa.getId())
																: Restrictions.sqlRestriction("true"))

														.add(Restrictions.eq("tanggalLahir", tanggalLahir.getValue()))

														.setMaxResults(1).uniqueResult();

												eventListener.onEvent(new Event("", addWindow, calonSiswa1));

												addWindow.setVisible(true);
												addWindow.onModal();

											}
										});

									}

								}
							});

				}

			}
		}
	}

	private EventListener checkKesamaan = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {

			GelombangPendaftaranPsb gelombangPendaftaranPsb = (GelombangPendaftaranPsb) (CalonSiswaAction.this.gelombangPendaftaran
					.getSelectedItem() == null ? null
							: CalonSiswaAction.this.gelombangPendaftaran.getSelectedItem().getValue());

			CalonSiswaAction.CheckKesamaan checkKesamaan = new CalonSiswaAction.CheckKesamaan(calonSiswa,
					gelombangPendaftaranPsb, tanggalLahir, namaSiswa, namaIbu, addWindow, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							CalonSiswa calonSiswa1 = (CalonSiswa) arg0.getData();
							init(calonSiswa1);

							gelombangPendaftaran.setDisabled(true);
						}
					});
			checkKesamaan.onEvent(arg0);
		}

	};

	private Combobox penjurusanSekolah;

	private Combobox jenisTinggalMahasiswa;

	private Combobox alatTransportasiMahasiswa;

	private Intbox tahunLulus;

	private Textbox noIjazah;

	private JSONArray array;

	private Row rowFormula;

	private Radiogroup statusSekolah;

	private Rows subRowsVerifikasiNilaiRapor;

	private Long jadwalPertemuanPSBData = null;

	private Row rowjadwalPertemuanPSB;

	private EventListener eventListenerPerubahan = null;

	public static void initBg(Center center, GelombangPendaftaranPsb gelombangPendaftaranPsb) {

		center.setStyle("background:url('" + Common.ROOT
				+ "/img/bg_default.jpg') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");

		try {

			if ((gelombangPendaftaranPsb != null && gelombangPendaftaranPsb.getId() != null)) {
				LampiranLain kop = LampiranLain.ambil(gelombangPendaftaranPsb.getId(), LampiranLain.BG_PPDB_GELOMBANG);
				if (kop != null && kop.getId() != null) {

					center.setStyle("background:url('" + kop.createLinkUri(true, true)
							+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");

				} else {
					Sekolah sekolah = SekolahUtil.getSekolah();

					if ((sekolah != null && sekolah.getId() != null)) {
						kop = LampiranLain.ambil(sekolah.getId(), LampiranLain.BG_PPDB_SEKOLAH);
						if (kop != null && kop.getId() != null) {

							center.setStyle("background:url('" + kop.createLinkUri(true, true)
									+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");

						} else {
							kop = LampiranLain.ambil(sekolah.getYayasan().getId(), LampiranLain.BG_PPDB_YAYASAN);
							if (kop != null && kop.getId() != null) {
								center.setStyle("background:url('" + kop.createLinkUri(true, true)
										+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");

							}
						}
					} else {
						Yayasan yayasan = SekolahUtil.getYayasan();
						if ((yayasan != null && yayasan.getId() != null)) {
							kop = LampiranLain.ambil(yayasan.getId(), LampiranLain.BG_PPDB_YAYASAN);
							if (kop != null && kop.getId() != null) {
								center.setStyle("background:url('" + kop.createLinkUri(true, true)
										+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");

							}
						}
					}
				}
			} else {
				Sekolah sekolah = SekolahUtil.getSekolah();

				if ((sekolah != null && sekolah.getId() != null)) {
					LampiranLain kop = LampiranLain.ambil(sekolah.getId(), LampiranLain.BG_PPDB_SEKOLAH);
					if (kop != null && kop.getId() != null) {

						center.setStyle("background:url('" + kop.createLinkUri(true, true)
								+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");

					} else {
						kop = LampiranLain.ambil(sekolah.getYayasan().getId(), LampiranLain.BG_PPDB_YAYASAN);
						if (kop != null && kop.getId() != null) {
							center.setStyle("background:url('" + kop.createLinkUri(true, true)
									+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");

						}
					}
				} else {
					Yayasan yayasan = SekolahUtil.getYayasan();
					if ((yayasan != null && yayasan.getId() != null)) {
						LampiranLain kop = LampiranLain.ambil(yayasan.getId(), LampiranLain.BG_PPDB_YAYASAN);
						if (kop != null && kop.getId() != null) {
							center.setStyle("background:url('" + kop.createLinkUri(true, true)
									+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");

						}
					}
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

	}

	/**
	 * Unggah FOTO MASSAL calon siswa: pilih banyak berkas foto (nama berkas = No. Registrasi/NIS)
	 * ATAU satu berkas ZIP; setiap foto langsung dipasang ke calon siswa yang cocok, lalu ringkasan
	 * (berhasil / tak ditemukan / gagal) ditampilkan. Dipicu tombol toolbar "Upload Foto".
	 */
	/** Unduh SEMUA foto calon siswa sebagai satu ZIP (baca BLOB paralel maks 50 thread). Tombol "Download Foto Massal". */
	public void onDownloadFotoMassal(Event event) throws Exception {
		ais.common.helper.DownloadFotoMassalHelper.downloadFotoCalonSiswaMassal();
	}

	public void onUploadFotoMassal(Event event) throws Exception {
		org.zkoss.zk.ui.event.ForwardEvent forwardEvent = (org.zkoss.zk.ui.event.ForwardEvent) event;
		org.zkoss.zk.ui.event.UploadEvent uploadEvent = (org.zkoss.zk.ui.event.UploadEvent) forwardEvent.getOrigin();
		org.zkoss.util.media.Media[] medias = uploadEvent.getMedias();
		java.util.List<org.zkoss.util.media.Media> daftar = new java.util.ArrayList<org.zkoss.util.media.Media>();
		if (medias != null) {
			for (org.zkoss.util.media.Media m : medias) {
				if (m != null) {
					daftar.add(m);
				}
			}
		} else if (uploadEvent.getMedia() != null) {
			daftar.add(uploadEvent.getMedia());
		}
		if (daftar.isEmpty()) {
			ais.ui.util.MyMessageboxConfig.show("Tidak ada berkas foto yang dipilih.", "Informasi",
					ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
			return;
		}
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Foto Massal Calon Siswa");
		for (int i = 0; i < daftar.size(); i++) {
			org.zkoss.util.media.Media m = daftar.get(i);
			String nama = (m != null && m.getName() != null && !m.getName().trim().isEmpty()) ? m.getName() : ("berkas-" + (i + 1));
			if (m != null && m.isBinary() && m.getName() != null && !m.getName().trim().isEmpty()) {
				report.sukses(i + 1, nama, "diajukan untuk upload");
			} else {
				report.gagal(i + 1, nama, "Berkas tidak valid (bukan biner atau nama berkas kosong)", "Pastikan berkas berupa gambar jpg/png dengan nama = No. Registrasi calon siswa");
			}
		}
		int[] hasil = ais.common.helper.UploadFotoMassalHelper.uploadFotoCalonSiswaByNim(daftar);
		try {
			Filedownload.save(report.simpanLaporan(), "text/plain");
		} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) CalonSiswaAction laporan foto massal"); }
		ais.ui.util.MyMessageboxConfig.show(ais.common.helper.UploadFotoMassalHelper.ringkasan(hasil),
				"Upload Foto Massal", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
	}

	public void onAdd(Event event) throws Exception {

		init(new CalonSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();

	}

	public static void onAddExternal(Event event, EventListener eventListener, EventListener eventListenerPerubahan,
			CalonSiswa calonSiswa, Tbmuser tbmuser, Integer desktopWidth, Integer desktopHeight,
			GelombangPendaftaranPsb myGelombangPendaftaranPsb) throws Exception {

		try {
			if (calonSiswa.getId() == null) {
				CalonSiswa tempCookie = (CalonSiswa) Common.ambilSession(CalonSiswa.class);
				System.out.println("tempCookie -> " + tempCookie);

				if (tempCookie != null) {
					calonSiswa = tempCookie;
					calonSiswa.setId(null);
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		try {
			if (myGelombangPendaftaranPsb != null && (calonSiswa == null || calonSiswa.getId() == null)) {

				Session session = HibernateUtil.currentSession();

				Long idmin = (Long) session.createCriteria(RuangPSB.class).createAlias("ujianPSB", "ujianPSB")
						.add(Restrictions.eq("gelombangPendaftaranPsb", calonSiswa.getGelombangPendaftaranPsb()))
						.add(Restrictions.eq("penuh", 0))
						.add(Restrictions.eq("ujianPSB.gelombangPendaftaranPsb",
								calonSiswa.getGelombangPendaftaranPsb()))
						.setProjection(Projections.min("id")).uniqueResult();

				if (idmin == null) {
					MyMessageboxConfig.show(
							"Kuota / ruangan penerimaan calon siswa telah penuh, harap hubungi petugas penerimaan siswa baru..",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				RuangPSB ruangSelected = (RuangPSB) session.createCriteria(RuangPSB.class).add(Restrictions.idEq(idmin))
						.uniqueResult();

				Number t = ((Number) (session.createCriteria(RuangGelombangPendaftaranPsbPSB.class)
						.createAlias("calonSiswa", "calonSiswa").add(Restrictions.ne("calonSiswa.noUjian", ""))
						.add(Restrictions.isNotNull("calonSiswa.noUjian"))
						.add(Restrictions.eq("ruangPSB", ruangSelected)).setProjection(Projections.rowCount())
						.uniqueResult()));
				Integer isiRuang = t == null ? 0 : t.intValue();

				if (ruangSelected.getKapasitasRuangan() <= isiRuang) {
					MyMessageboxConfig.show(
							"Kuota / ruangan penerimaan calon siswa telah penuh, harap hubungi petugas penerimaan siswa baru..",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		calonSiswa.setGelombangPendaftaranPsb(myGelombangPendaftaranPsb);

		String classWindow = calonSiswa.getId() == null
				? (calonSiswa != null && calonSiswa.getGelombangPendaftaranPsb() != null
						&& calonSiswa.getGelombangPendaftaranPsb().getClassUntukPendaftaran() != null
								? calonSiswa.getGelombangPendaftaranPsb().getClassUntukPendaftaran()
								: null)
				: (calonSiswa != null && calonSiswa.getGelombangPendaftaranPsb() != null
						&& calonSiswa.getGelombangPendaftaranPsb().getClassUntukMelengkapiBerkas() != null
								? calonSiswa.getGelombangPendaftaranPsb().getClassUntukMelengkapiBerkas()
								: null);

		if (classWindow != null) {
			PPDB window = (PPDB) Class.forName(classWindow).newInstance();
			window.setCalonSiswa(calonSiswa);
			window.setGelombangPendaftaranPsb(myGelombangPendaftaranPsb);
			window.setEventListener(eventListener);
			window.init();
			ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
			window.setHeight(Common.isMobile() ? "100%" : desktopHeight == null ? "95%" : desktopHeight + "px");
			window.setWidth(Common.isMobile() ? "100%" : desktopWidth == null ? "750px" : desktopWidth + "px");
			window.setVisible(true);
			window.onModal();
		} else {
			CalonSiswaAction calonSiswaAction = new CalonSiswaAction();
			calonSiswaAction.myGelombangPendaftaranPsb = myGelombangPendaftaranPsb;
			calonSiswaAction.eventListener = eventListener;
			calonSiswaAction.eventListenerPerubahan = eventListenerPerubahan;
			calonSiswaAction.addWindow = new MyWindow();
			calonSiswaAction.tbmuser = tbmuser;

			ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(calonSiswaAction.addWindow);
			calonSiswaAction.addWindow
					.setHeight(Common.isMobile() ? "100%" : desktopHeight == null ? "95%" : desktopHeight + "px");
			calonSiswaAction.addWindow
					.setWidth(Common.isMobile() ? "100%" : desktopWidth == null ? "750px" : desktopWidth + "px");

			calonSiswaAction.init(calonSiswa);

			calonSiswaAction.addWindow.setVisible(true);
			calonSiswaAction.addWindow.onModal();
		}
	}

	private EventListener masukkanPerubahan = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			if (eventListenerPerubahan != null) {

				try {
					setdata();
					eventListenerPerubahan.onEvent(new Event("", null, calonSiswa));
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}

			}

		}
	};

	private Textbox noAktaKelahiran;

	protected Set<Long> selectedKelasLesSiswa = null;

	private boolean baru;

	protected TreeSet<Long> hapusKelasLesSiswa;

	private Combobox paketPsb;

	private MyCheckboxConfig merupakanPindahan;

	private Textbox pindahanDariSekolah;

	private Textbox alamatSekolahPindahan;

	private MyDatebox tanggalPindah;

	private Textbox keteranganPindah;

	private Textbox kelasSekolahPindahan;

	private AmbilDataPegawaiBanbox orangTuaPegawai;

	private Row rowAlumni;

	protected AmbilDataSiswaBanbox siswaAlumni;

	private Row rowAnak;

	protected Combobox keluarga = null;

	private Combobox jadwalPertemuanPSB;

	private Tabpanel dasborStatistik;
	private Tabpanel dasborStatus;
	private Tabpanel dasborSekolah;
	private Tabpanel dasborRegistrasi;
	private Tabpanel dasborHarian;
	private Tabpanel dasborRekapMultiTahun;
	private DashboardStatistikSiswa      dashboardStatistik;
	private DashboardStatusSiswa         dashboardStatus;
	private DashboardAsalSekolahSiswa    dashboardSekolah;
	private DashboardRegistrasiSiswa     dashboardRegistrasi;
	private DashboardHarianSiswa         dashboardHarian;
	private RekapJalurMasukMultiTahunPsb dashboardRekapMultiTahun;

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		calonSiswa = (CalonSiswa) obj;

		String classWindow = calonSiswa.getId() == null
				? (calonSiswa != null && calonSiswa.getGelombangPendaftaranPsb() != null
						&& calonSiswa.getGelombangPendaftaranPsb().getClassUntukPendaftaran() != null
								? calonSiswa.getGelombangPendaftaranPsb().getClassUntukPendaftaran()
								: null)
				: (calonSiswa != null && calonSiswa.getGelombangPendaftaranPsb() != null
						&& calonSiswa.getGelombangPendaftaranPsb().getClassUntukMelengkapiBerkas() != null
								? calonSiswa.getGelombangPendaftaranPsb().getClassUntukMelengkapiBerkas()
								: null);

		if (classWindow != null) {

			PPDB window = (PPDB) Class.forName(classWindow).newInstance();
			window.setCalonSiswa(calonSiswa);
			window.setGelombangPendaftaranPsb(calonSiswa.getGelombangPendaftaranPsb());
			window.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});
			window.init();
			ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
			window.setHeight("95%");
			window.setWidth(Common.isMobile() ? "100%" : "750px");
			window.setVisible(true);
			window.onModal();

		} else {

			init(calonSiswa);
			addWindow.setVisible(true);
			addWindow.onModal();
		}
	}

	@SuppressWarnings("unchecked")
	public static void initKelasLes(Row row, GelombangPendaftaranPsb gelombangPendaftaranPsb,
			final Set<Long> selectedKelasLesSiswa, final Set<Long> hapusKelasLesSiswa, final CalonSiswa calonSiswa) {
		Common.clear(row);

		List<KelasLesSiswa> kelasLesSiswas = gelombangPendaftaranPsb == null || gelombangPendaftaranPsb.getId() == null
				? null
				: ConstantValues.simpleList(
						HibernateUtil.currentSession().createCriteria(KelasSiswaPSB.class)
								.setProjection(Projections.property("kelasLesSiswa.id"))
								.add(Restrictions.or(Restrictions.isNull("penuh"), Restrictions.eq("penuh", 0)))
								.createAlias("kelasLesSiswa", "kelasLesSiswa")
								.addOrder(Order.asc("kelasLesSiswa.tingkat")).addOrder(Order.asc("kelasLesSiswa.nama"))
								.add(Restrictions.or(Restrictions.isNull("kelasLesSiswa.aktif"),
										Restrictions.eq("kelasLesSiswa.aktif", true)))
								.add(Restrictions.eq("gelombangPendaftaranPsb", gelombangPendaftaranPsb)),
						KelasLesSiswa.class, false);

		if (kelasLesSiswas != null && !kelasLesSiswas.isEmpty()) {
			row.setVisible(true);
			final MyGrid subGridKelasLesSiswa = new MyGrid();
			row.appendChild(subGridKelasLesSiswa);

			Columns subColumns = new Columns();
			subColumns.setParent(subGridKelasLesSiswa);
			Column c = new Column("Pilih Kelas");
			c.setWidth("70%");
			subColumns.appendChild(c);

			c = new Column("Sertifikat");
			subColumns.appendChild(c);

			Rows subRows = new Rows();
			subRows.setParent(subGridKelasLesSiswa);

			selectedKelasLesSiswa.addAll(calonSiswa.ambilKelasLesSiswaId());

			Set<Long> ids = new HashSet<Long>();
			for (Long v : selectedKelasLesSiswa) {
				ids.add(v);
			}

			System.out.println("ids ->" + ids);

			Session session = HibernateUtil.currentSession();
			List<Long> idsKelasMasuk = calonSiswa == null || calonSiswa.getId() == null ? new ArrayList<Long>()
					: session.createCriteria(KelasLesSiswaPunyaSiswa.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setProjection(Projections.groupProperty("kelasLesSiswa.id"))
							.add(Restrictions.eq("calonSiswa", calonSiswa)).list();

			for (final KelasLesSiswa kelasLesSiswa : kelasLesSiswas) {

				MyFormRow subRow = new MyFormRow();
				subRow.setParent(subRows);
				subRow.setValign("top");

				if (idsKelasMasuk.contains(kelasLesSiswa.getId())) {
					new MyLabelAgakKecilBoldHijau(kelasLesSiswa.getNama()).setParent(subRow);
					final KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa = (KelasLesSiswaPunyaSiswa) session
							.createCriteria(KelasLesSiswaPunyaSiswa.class)
							.add(Restrictions.eq("calonSiswa", calonSiswa))
							.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa)).setMaxResults(1).uniqueResult();

					if (kelasLesSiswaPunyaSiswa != null && kelasLesSiswaPunyaSiswa.getAcc()
							&& kelasLesSiswa.getSertifikat() != null) {
						MyToolbarbuttonConfig cetakToolbarbuttonSertifikat = new MyToolbarbuttonConfig("Sertifikat",
								"/img/certificate-icon.png");
						cetakToolbarbuttonSertifikat.setParent(subRow);
						cetakToolbarbuttonSertifikat.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								SertifikatAction.cetakSertifikat(kelasLesSiswaPunyaSiswa);
							}
						});
					} else {
						new Label().setParent(subRow);
					}
				} else {

					if (calonSiswa.getSiswa() != null) {
						new Label(kelasLesSiswa.getNama()).setParent(subRow);
						new Label().setParent(subRow);
					} else {

						final Checkbox checkbox = new Checkbox(kelasLesSiswa.getNama());
						checkbox.setParent(subRow);
						checkbox.setChecked(ids.contains(kelasLesSiswa.getId()));
						checkbox.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (checkbox.isChecked()) {
									selectedKelasLesSiswa.add(kelasLesSiswa.getId());
									hapusKelasLesSiswa.remove(kelasLesSiswa.getId());
								} else {
									for (Long a : selectedKelasLesSiswa) {
										if (a.equals(kelasLesSiswa.getId())) {
											selectedKelasLesSiswa.remove(a);
											hapusKelasLesSiswa.add(a);
											break;
										}
									}
								}

								String jenisS = "";
								for (Long kelasLesSiswa : selectedKelasLesSiswa) {
									jenisS += jenisS.isEmpty() ? kelasLesSiswa.toString() : "," + kelasLesSiswa;
								}
								calonSiswa.setKelasLesDipilih(jenisS);

								System.out.println(
										"selectedKelasLesSiswa => " + selectedKelasLesSiswa + ", jenisS " + jenisS);
							}
						});

						new Label().setParent(subRow);
					}
				}

			}

		} else {
			row.setVisible(false);
		}
	}

	@SuppressWarnings("deprecation")
	private void init(final CalonSiswa calonSiswa) throws Exception {
		this.calonSiswa = calonSiswa;
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		Tbmuser tbmuser = this.tbmuser != null ? this.tbmuser : Common.getCurrentUser();

		final Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		CalonSiswaAction.initBg(center, calonSiswa.getGelombangPendaftaranPsb());

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

		final Rows rows = new Rows();
		rows.setParent(grid);

		try {
			if ((calonSiswa != null && calonSiswa.getGelombangPendaftaranPsb() != null)) {
				GelombangPendaftaranPsb gel = calonSiswa.getGelombangPendaftaranPsb();
				LampiranLain kop = LampiranLain.ambil(gel.getId(), LampiranLain.KOP_GELOMBANG_PSB);

				if (kop != null && kop.getId() != null) {

					Image image = new Image(kop.createLinkUri());
					image.setWidth("100%");

					MyFormRow rowUtama1 = new MyFormRow();
					rowUtama1.setSclass("headerHbox");
					rowUtama1.appendChild(image);
					ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
					rowUtama1.setValign("top");
					rowUtama1.setParent(rows);
				} else {
					Hbox hbox = PSBAction.headerBox();

					MyFormRow rowUtama1 = new MyFormRow();
					rowUtama1.setSclass("headerHbox");
					rowUtama1.appendChild(hbox);
					ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
					rowUtama1.setValign("top");
					rowUtama1.setParent(rows);
				}
			} else {
				Hbox hbox = PSBAction.headerBox();

				MyFormRow rowUtama1 = new MyFormRow();
				rowUtama1.setSclass("headerHbox");
				rowUtama1.appendChild(hbox);
				ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
				rowUtama1.setValign("top");
				rowUtama1.setParent(rows);
			}
		} catch (Exception e) {
			Hbox hbox = PSBAction.headerBox();

			MyFormRow rowUtama1 = new MyFormRow();
			rowUtama1.setSclass("headerHbox");
			rowUtama1.appendChild(hbox);
			ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
			rowUtama1.setValign("top");
			rowUtama1.setParent(rows);
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("I. Data Pendaftaran"));

		row = new MyFormRow();
		row.setVisible(calonSiswa.getNomorInduk() != null && !calonSiswa.getNomorInduk().isEmpty());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Registrasi"));
		row.appendChild(nomorInduk = new Label(calonSiswa.getNomorInduk()));
		nomorInduk.setWidth("90%");

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));

		if (myGelombangPendaftaranPsb != null) {
			calonSiswa.setGelombangPendaftaranPsb(myGelombangPendaftaranPsb);
			row.appendChild(new Label(myGelombangPendaftaranPsb.getYayasan().getNama()));
		} else {
			row.appendChild(yayasan);
		}

		Common.selectComboItem(yayasan, calonSiswa.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));

		if (myGelombangPendaftaranPsb != null) {
			row.appendChild(new Label(myGelombangPendaftaranPsb.getSekolah().getNama()));
		} else {
			row.appendChild(sekolah);
		}

		// selectComboItem(yayasan,...) TIDAK memicu onChange, jadi daftar sekolah harus
		// dimuat eksplisit untuk yayasan terpilih agar combo Sekolah tidak kosong.
		Yayasan yayasanTerpilih = (yayasan.getSelectedItem() != null
				&& yayasan.getSelectedItem().getValue() instanceof Yayasan)
						? (Yayasan) yayasan.getSelectedItem().getValue()
						: calonSiswa.getYayasan();
		Common.muatSekolahMilikYayasan(sekolah, yayasanTerpilih);

		Common.pilihSekolah(sekolah, calonSiswa.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penjurusan *"));
		row.appendChild(penjurusanSekolah = new Combobox());
		penjurusanSekolah.setWidth("90%");
		penjurusanSekolah.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Paket *"));
		row.appendChild(paketPsb = new Combobox());
		paketPsb.setWidth("90%");
		paketPsb.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang *"));
		gelombangPendaftaran = new Combobox();
		if (myGelombangPendaftaranPsb != null) {
			row.appendChild(new Label(myGelombangPendaftaranPsb.getNama()));
		} else {
			row.appendChild(gelombangPendaftaran);
		}
		gelombangPendaftaran.setWidth("90%");
		gelombangPendaftaran.setReadonly(true);
		gelombangPendaftaran.addEventListener("onChange", checkKesamaan);
		if (Common.bolehKonfigurasi("tampilkan_link_login_oleh_admin_di_data_calon_siswa")) {
			if (calonSiswa.getId() != null) {

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Link"));

				final A a = new A("Tampilkan Link");
				a.setHref("");
				row.appendChild(a);

				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String code = calonSiswa.urlLogin();
						a.setLabel(code);
						a.setHref(Common.getRequestHostWithProtocol() + "/logoff?param="
								+ URLEncoder.encode(code, "UTF-8"));
					}
				});

			}
		}

		rowAlumni = new MyFormRow();
		rowAlumni.setVisible(false);
		rowAlumni.setParent(rows);

		rowAnak = new MyFormRow();
		rowAnak.setVisible(false);
		rowAnak.setParent(rows);

		EventListener gelombangALumni = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rowAlumni);
				Common.clear(rowAnak);

				GelombangPendaftaranPsb gel = (GelombangPendaftaranPsb) (gelombangPendaftaran.getSelectedItem() == null
						? null
						: gelombangPendaftaran.getSelectedItem().getValue());

				if (myGelombangPendaftaranPsb != null) {
					gel = myGelombangPendaftaranPsb;
				}

				if (gel != null) {
					siswaAlumni = PPDB.alumni(calonSiswa, gel, rows, rowAlumni, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Siswa s = (Siswa) arg0.getData();
							if (namaSiswa != null) {
								namaSiswa.setValue(s.getNama());
								namaSiswa.setDisabled(true);
							}

							if (jenisKelamin != null) {
								Common.selectComboItem(jenisKelamin, s.getJenisKelamin());
							}

							if (tempatLahir != null && tempatLahir.getValue().trim().isEmpty()) {
								tempatLahir.setValue(s.getTempatLahir());
							}
							if (tanggalLahir != null && tanggalLahir.getValue() == null) {
								tanggalLahir.setValue(s.getTanggalLahir());
							}
							if (namaAyah != null && namaAyah.getValue().trim().isEmpty()) {
								namaAyah.setValue(s.getNamaAyah());
							}
							if (namaIbu != null && namaIbu.getValue().trim().isEmpty()) {
								namaIbu.setValue(s.getNamaIbu());
							}
							if (hp1ayah != null && hp1ayah.getValue().trim().isEmpty()) {
								hp1ayah.setValue(s.getHp1ayah());
							}
							if (hp1ibu != null && hp1ibu.getValue().trim().isEmpty()) {
								hp1ibu.setValue(s.getHp1ibu());
							}
							if (teleponSiswa != null && teleponSiswa.getValue().trim().isEmpty()) {
								teleponSiswa.setValue(s.getTeleponSiswa());
							}
							if (alamatSiswa != null && alamatSiswa.getValue().trim().isEmpty()) {
								alamatSiswa.setValue(s.getAlamatSiswa());
							}
						}
					});
					rowAlumni.setVisible(gel.getHarusSebagaiAlumni());

					Tbmuser tbmuser = Common.getCurrentUser();
					Pegawai pegawai = tbmuser == null ? null : tbmuser.getPegawai();
					keluarga = PPDB.anakPegawai(calonSiswa, gel, pegawai, rows, rowAnak, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Keluarga s = (Keluarga) arg0.getData();
							if (namaSiswa != null) {
								namaSiswa.setValue(s.getNama());
								namaSiswa.setDisabled(true);
							}
							Pegawai pegawai = s.getPegawai();
							if (jenisKelamin != null) {
								Common.selectComboItem(jenisKelamin, s.getJenisKelamin());
							}

							if (tempatLahir != null && tempatLahir.getValue().trim().isEmpty()) {
								tempatLahir.setValue(s.getTempatLahir());
							}
							if (tanggalLahir != null && tanggalLahir.getValue() == null) {
								tanggalLahir.setValue(s.getTanggalLahir());
							}
							if (namaAyah != null && namaAyah.getValue().trim().isEmpty()
									&& pegawai.getKelamin().equalsIgnoreCase("Laki-laki")) {
								namaAyah.setValue(pegawai.getNama());
							}
							if (namaIbu != null && namaIbu.getValue().trim().isEmpty()
									&& pegawai.getKelamin().equalsIgnoreCase("Perempuan")) {
								namaIbu.setValue(pegawai.getNama());
							}
							if (hp1ayah != null && hp1ayah.getValue().trim().isEmpty()
									&& pegawai.getKelamin().equalsIgnoreCase("Laki-laki")) {
								hp1ayah.setValue(pegawai.getHp());
							}
							if (hp1ibu != null && hp1ibu.getValue().trim().isEmpty()
									&& pegawai.getKelamin().equalsIgnoreCase("Perempuan")) {
								hp1ibu.setValue(pegawai.getHp());
							}

							if (alamatSiswa != null && alamatSiswa.getValue().trim().isEmpty()) {
								alamatSiswa.setValue(s.getAlamat());
							}
						}
					});
				}
			}
		};

		gelombangPendaftaran.addEventListener("onChange", gelombangALumni);
		try {
			gelombangALumni.onEvent(null);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("II. Data Diri Pendaftar"));

		row = new MyFormRow();
		String statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "nik");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nomor Induk Kependudukan (NIK) " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(nik = new Textbox(calonSiswa.getNik()));
		nik.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "kk");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nomor Kartu Keluarga (KK) " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(kk = new Textbox(calonSiswa.getKk()));
		kk.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "nomorIndukNasional");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);

		row.appendChild(
				new ais.ui.util.MyLabelConfig("NISN" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(nomorIndukNasional = new Textbox(calonSiswa.getNomorIndukNasional()));
		nomorIndukNasional.setWidth("90%");

		nomorIndukNasional.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				GelombangPendaftaranPsb gel = gelombangPendaftaran.getSelectedItem() == null ? null
						: (GelombangPendaftaranPsb) gelombangPendaftaran.getSelectedItem().getValue();

				if (gel != null) {

					if (!nomorIndukNasional.getValue().trim().isEmpty()) {

						if (!Common.isNumber(nomorIndukNasional.getValue().trim())) {
							MyMessageboxConfig.showFormat(
									"Mohon maaf, NISN \"{V1}\" tidak valid karena harus berupa angka. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan NISN dan pastikan hanya terdiri dari angka; (2) Perbaiki isian NISN tersebut; (3) Apabila masih mengalami kendala, silakan hubungi: {V2}",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
									nomorIndukNasional.getValue().trim(),
									Common.getKonfigurasi("hubungi_admin_calon_mhs",
											"Silahkan hubungi admin di Nomor \r\n" + "WA : ...... / ..... \r\n"
													+ "atau email : .....\r\n" + "")
											.getNilai());
							nomorIndukNasional.focus();
							nomorIndukNasional.select();
							return;
						}

						int jml = ((Number) HibernateUtil.currentSession().createCriteria(CalonSiswa.class)
								.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
								.add(Restrictions.eq("tahunMasuk", gel.getTahunMasuk()))
								.setProjection(Projections.rowCount())
								.add(calonSiswa.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", calonSiswa.getId()))
								.add(Restrictions.ilike("nomorIndukNasional", nomorIndukNasional.getValue().trim(),
										MatchMode.EXACT))
								.uniqueResult()).intValue();

						if (jml > 0) {

							MyMessageboxConfig.showFormat(
									"Mohon maaf, NISN \"{V1}\" sudah terdaftar sebelumnya, sehingga tidak dapat digunakan kembali. Langkah yang dapat dilakukan: (1) Pastikan Ananda belum pernah didaftarkan sebelumnya; (2) Periksa kembali penulisan NISN; (3) Apabila masih mengalami kendala, silakan hubungi: {V2}",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
									nomorIndukNasional.getValue().trim(),
									Common.getKonfigurasi("hubungi_admin_calon_mhs",
											"Silahkan hubungi admin di Nomor \r\n" + "WA : ...... / ..... \r\n"
													+ "atau email : .....\r\n" + "")
											.getNilai());
							nomorIndukNasional.focus();
							nomorIndukNasional.select();
						}
					}

				}
			}
		});

		// Keterangan + tautan NISN hanya tampil bila baris NISN memang ditampilkan.
		// row.isVisible() BUKAN penanda yang tepat: baris yang disembunyikan konfigurasi
		// di-set parent null (visible-nya tetap true), sehingga keterangan tampil yatim.
		if (row.getParent() != null) {
			Common.initKeterangan(rows,
					"* Nomor Induk Siswa Nasional (NISN) pendidikan yang sebelumnya ditempuh, untuk mencari NISN, bisa di lihat di link berikut :");

			String l = Common.getKonfigurasi("link_mencari_nisn_data",
					"https://nisn.data.kemdikbud.go.id/index.php/Cindex/formcaribynama").getNilai();

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new Label());
			A link = new A(l);
			link.setHref(l);
			link.setTarget("_blank");
			row.appendChild(link);
		}

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "noAktaKelahiran");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nomor Akta Kelahiran " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(noAktaKelahiran = new Textbox(calonSiswa.getNoAktaKelahiran()));
		noAktaKelahiran.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "namaSiswa");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nama Lengkap " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(namaSiswa = new Textbox(calonSiswa.getNamaSiswa()));
		namaSiswa.setWidth("90%");
		namaSiswa.addEventListener("onChange", checkKesamaan);

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "panggilan");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Panggilan " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(panggilan = new Textbox(calonSiswa.getPanggilan()));
		panggilan.setWidth("90%");

		jenisKelamin = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig();
		comboitem.setLabel("Laki-laki");
		comboitem.setValue("Laki-laki");
		jenisKelamin.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Perempuan");
		comboitem.setValue("Perempuan");
		jenisKelamin.appendChild(comboitem);

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "jenisKelamin");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Jenis Kelamin " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		Common.selectComboItem(jenisKelamin, calonSiswa.getJenisKelamin());
		row.appendChild(jenisKelamin);
		jenisKelamin.setWidth("90%");
		jenisKelamin.setReadonly(true);

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "agama");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);

		row.appendChild(
				new ais.ui.util.MyLabelConfig("Agama " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(agama = new Combobox());
		Common.insertCombo(agama, "nama", "keterangan", Agama.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(agama, calonSiswa.getAgama());
		agama.setWidth("90%");
		agama.setReadonly(true);

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "tempatLahir");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Tempat / Tanggal Lahir " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		Box hbox = Common.isMobile() ? new Vbox() : new Hbox();
		hbox.appendChild(
				tempatLahir = new Textbox(calonSiswa.getTempatLahir() == null ? "" : calonSiswa.getTempatLahir()));
		hbox.appendChild(tanggalLahir = new MyDatebox(calonSiswa.getTanggalLahir()));
		row.appendChild(hbox);
		tempatLahir.setCols(15);
		tanggalLahir.addEventListener("onChange", checkKesamaan);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "statusAwalSiswa");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Status Awal Siswa " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(
				new Label(calonSiswa.getStatusAwalSiswa() == null ? "" : calonSiswa.getStatusAwalSiswa().getNama()));

		rowjadwalPertemuanPSB = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "jadwalPertemuanPSB");
		rowjadwalPertemuanPSB.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		rowjadwalPertemuanPSB.appendChild(new Label(
				"Jadwal Pertemuan Siswa / Orang Tua" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		final boolean daftar = tbmuser == null;

		final MyButtonConfig save = new MyButtonConfig(daftar ? "D A F T A R" : "S I M P A N", "/img/save.gif");

		jadwalPertemuanPSB = new Combobox();

		if (calonSiswa.getJadwalPertemuanPSB() != null && tbmuser.getCalonSiswa() != null) {
			rowjadwalPertemuanPSB.appendChild(new Label(calonSiswa.getJadwalPertemuanPSB().getNama()));

			if (!calonSiswa.getJadwalPertemuanPSB().getKeterangan().isEmpty()) {
				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new Label());
				row.appendChild(new Label(calonSiswa.getJadwalPertemuanPSB().getKeterangan()));
			}
		} else {
			rowjadwalPertemuanPSB.appendChild(jadwalPertemuanPSB);

			jadwalPertemuanPSB.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					JadwalPertemuanPSB d = (JadwalPertemuanPSB) (jadwalPertemuanPSB.getSelectedItem() == null ? null
							: jadwalPertemuanPSB.getSelectedItem().getValue());
					int jml = ((Number) HibernateUtil.currentSession().createCriteria(CalonSiswa.class)
							.setProjection(Projections.rowCount())
							.add(calonSiswa.getId() == null ? Restrictions.sqlRestriction("true")
									: Restrictions.ne("id", calonSiswa.getId()))
							.add(Restrictions.eq("jadwalPertemuanPSB", d)).uniqueResult()).intValue();
					save.setVisible(jml <= d.getKuota());
					if (jml >= d.getKuota()) {

						MyMessageboxConfig.showFormatCb(
								"Mohon maaf, kuota untuk jadwal pertemuan \"{V1}\" telah penuh. Langkah yang dapat dilakukan: (1) Tutup pesan ini; (2) Pilih jadwal pertemuan lain yang kuotanya masih tersedia; (3) Lanjutkan proses pendaftaran.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										jadwalPertemuanPSB.focus();
										jadwalPertemuanPSB.select();
										Clients.scrollIntoView(jadwalPertemuanPSB);
									}
								}, d.getNama());
					}
				}
			});

		}

		jadwalPertemuanPSB.setWidth("90%");
		jadwalPertemuanPSB.setReadonly(true);

		final EventListener jadwalPertemuanEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				GelombangPendaftaranPsb gel = myGelombangPendaftaranPsb != null ? myGelombangPendaftaranPsb
						: (GelombangPendaftaranPsb) (gelombangPendaftaran == null
								|| gelombangPendaftaran.getSelectedItem() == null ? null
										: gelombangPendaftaran.getSelectedItem().getValue());

				if (gel == null && calonSiswa.getGelombangPendaftaranPsb() != null) {
					gel = calonSiswa.getGelombangPendaftaranPsb();
				}

				Common.clear(jadwalPertemuanPSB);
				Session session = HibernateUtil.currentSession();
				List<JadwalPertemuanPSB> jadwalPertemuanPSBs = ConstantValues.simpleList(
						session.createCriteria(JadwalPertemuanPSB.class).addOrder(Order.asc("waktuMulai"))
								.add(Restrictions.gt("waktuSampai", WaktuUtil.getDate()))
								.add(Restrictions.and(
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
										Restrictions.and(Restrictions.eq("bolehDipilihSendiriOlehCalonSiswa", true),
												Restrictions.eq("gelombangPendaftaranPsb", gel)))),
						JadwalPertemuanPSB.class);

				for (JadwalPertemuanPSB pertemuanPSB : jadwalPertemuanPSBs) {
					Comboitem comboitem = new Comboitem(pertemuanPSB.getNama());
					comboitem.setDescription("Jadwal " + Common.dateFormat51.get().format(pertemuanPSB.getWaktuMulai())
							+ " sd " + Common.dateFormat51.get().format(pertemuanPSB.getWaktuSampai()));
					comboitem.setValue(pertemuanPSB);
					jadwalPertemuanPSB.appendChild(comboitem);
				}

				Comboitem comboitem = new Comboitem("Belum Menentukan");
				comboitem.setValue(null);
				jadwalPertemuanPSB.appendChild(comboitem);

				if (jadwalPertemuanPSB.getChildren().size() > 1) {
					jadwalPertemuanPSB.setVisible(true);
					if (jadwalPertemuanPSB.getParent() != null) {
						jadwalPertemuanPSB.getParent().setVisible(true);
					}
				} else {
					jadwalPertemuanPSB.setVisible(false);
					if (jadwalPertemuanPSB.getParent() != null) {
						jadwalPertemuanPSB.getParent().setVisible(false);
					}
				}

				Common.selectComboItem(jadwalPertemuanPSB, calonSiswa.getJadwalPertemuanPSB());
			}
		};

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "alamatSiswa");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Alamat Siswa" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(alamatSiswa = new Textbox(calonSiswa.getAlamatSiswa()));
		alamatSiswa.setWidth("90%");
		alamatSiswa.setRows(2);

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "rt");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig("RT" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(rt = new Textbox(calonSiswa.getRt() == null ? "" : calonSiswa.getRt()));
		rt.setWidth("90%");
		rt.setMaxlength(3);
		rt.setCols(3);

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "rw");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig("RW" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(rw = new Textbox(calonSiswa.getRw() == null ? "" : calonSiswa.getRw()));
		rw.setWidth("90%");
		rw.setMaxlength(3);
		rw.setCols(3);

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "dusunCalon");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Dusun / Kampung" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(dusunCalon = new Textbox(calonSiswa.getDusunCalon()));
		dusunCalon.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "kelurahanCalon");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Kelurahan / Desa" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(kelurahanCalon = new Textbox(
				calonSiswa.getKelurahanCalon() == null ? "" : calonSiswa.getKelurahanCalon()));
		kelurahanCalon.setWidth("90%");
		// kelurahanCalon.//setConstraint("no empty");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "kecamatanCalon");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Kecamatan" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(kecamatanCalon = new AmbilDataKecamatanBanbox());
		kecamatanCalon.setValue(calonSiswa.getKecamatanCalon() == null ? "" : calonSiswa.getKecamatanCalon().getNama());
		kecamatanCalon.setAttribute("wilayah", calonSiswa.getKecamatanCalon());
		kecamatanCalon.setWidth("90%");
		// kecamatanCalon.//setConstraint("no empty");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "propinsiCalon");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Propinsi" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		propinsiCalon = new Label();
		row.appendChild(propinsiCalon);
		propinsiCalon.setWidth("90%");

		propinsiCalon.setAttribute("wilayah", calonSiswa.getPropinsiCalon());

		// propinsiCalon.//setConstraint("no empty");

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "kotaCalon");
		Common.createFieldKota(rows, "Kota/Kabupaten" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : ""),
				kotaCalon = new Label(), propinsiCalon, calonSiswa.getKotaCalon(),
				statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB));

		Common.createKotaPropinsiListenerBerdasarkanKecamatan(propinsiCalon, kotaCalon, kecamatanCalon);

		kotaCalon.setAttribute("wilayah", calonSiswa.getKotaCalon());

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "kodePos");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Kode Pos" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(kodePos = new Textbox(calonSiswa.getKodePos() == null ? "" : calonSiswa.getKodePos()));
		kodePos.setWidth("90%");
		kodePos.setMaxlength(8);
		kodePos.setCols(8);

		kewarganegaraan = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Siswa.WNI);
		comboitem.setValue(Siswa.WNI);
		kewarganegaraan.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Siswa.WNA);
		comboitem.setValue(Siswa.WNA);
		kewarganegaraan.appendChild(comboitem);
		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "kewarganegaraan");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Kewarganegaraan" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		Common.selectComboItem(kewarganegaraan, calonSiswa.getKewarganegaraan());
		row.appendChild(kewarganegaraan);
		kewarganegaraan.setWidth("90%");
		kewarganegaraan.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "negara");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Asal Negara" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(negara = new AmbilDataNegaraBanbox());
		negara.setAttribute("negara",
				calonSiswa.getNegara() == null ? ConstantValues.INDONESIA : calonSiswa.getNegara());
		try {
			negara.setValue((calonSiswa.getNegara() == null ? ConstantValues.INDONESIA : calonSiswa.getNegara())
					.getNamaNegara());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/CalonSiswaAction.java:3108");
			// TODO: handle exception
		}
		negara.setReadonly(true);
		negara.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "jenisTinggalMahasiswa");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("Tempat Tinggal Saat Sekolah" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(jenisTinggalMahasiswa = new Combobox());
		Common.insertCombo(jenisTinggalMahasiswa, "nama", JenisTinggalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jenisTinggalMahasiswa, calonSiswa.getJenisTinggalMahasiswa());
		jenisTinggalMahasiswa.setWidth("90%");
		jenisTinggalMahasiswa.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser,
				"alatTransportasiMahasiswa");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Transportasi Siswa Saat Sekolah" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(alatTransportasiMahasiswa = new Combobox());
		Common.insertCombo(alatTransportasiMahasiswa, "nama", AlatTransportasiMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(alatTransportasiMahasiswa, calonSiswa.getAlatTransportasiMahasiswa());
		alatTransportasiMahasiswa.setWidth("90%");
		alatTransportasiMahasiswa.setReadonly(true);

		statusDalamKeluarga = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Anak Kandung");
		comboitem.setValue("Anak Kandung");
		statusDalamKeluarga.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Anak Angkat");
		comboitem.setValue("Anak Angkat");
		statusDalamKeluarga.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Anak Tiri");
		comboitem.setValue("Anak Tiri");
		statusDalamKeluarga.appendChild(comboitem);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "statusDalamKeluarga");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Status Anak" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		Common.selectComboItem(statusDalamKeluarga, calonSiswa.getStatusDalamKeluarga());
		row.appendChild(statusDalamKeluarga);
		statusDalamKeluarga.setWidth("90%");
		statusDalamKeluarga.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "bahasa");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Bahasa Di Rumah" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(bahasa = new Textbox(calonSiswa.getBahasa()));
		bahasa.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "anakKe");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);

		row.appendChild(
				new ais.ui.util.MyLabelConfig("Anak ke " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		hbox = new Hbox();
		hbox.appendChild(anakKe = new Intbox(calonSiswa.getAnakKe()));
		hbox.appendChild(new ais.ui.util.MyLabelConfig(" dari "));
		hbox.appendChild(dariAnakKe = new Intbox(calonSiswa.getDariAnakKe()));
		hbox.appendChild(new ais.ui.util.MyLabelConfig(" bersaudara "));
		row.appendChild(hbox);
		anakKe.setCols(2);
		dariAnakKe.setCols(2);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "jumlahSaudaraKandung");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);

		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Jml. saudara kandung " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(jumlahSaudaraKandung = new Intbox(calonSiswa.getJumlahSaudaraKandung()));

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "jumlahSaudaraTiri");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Jml. saudara tiri" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(jumlahSaudaraTiri = new Intbox(calonSiswa.getJumlahSaudaraTiri()));

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "golonganDarah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Golongan Darah" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(golonganDarah = new Textbox(calonSiswa.getGolonganDarah()));
		golonganDarah.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "tinggi");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Tinggi Badan" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(tinggi = new MyDoublebox(calonSiswa.getTinggi()));

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "berat");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Berat Badan" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(berat = new MyDoublebox(calonSiswa.getBerat()));

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "hobby");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Hobbi Siswa" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(hobby = new Textbox(calonSiswa.getHobby()));
		hobby.setWidth("90%");
		hobby.setRows(2);

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "riwayatPenyakit");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Riwayat Penyakit Siswa (jika ada)" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(riwayatPenyakit = new Textbox(calonSiswa.getRiwayatPenyakit()));
		riwayatPenyakit.setWidth("90%");
		riwayatPenyakit.setRows(2);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "teleponSiswa");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"No. Handphone (WA Aktif)" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(teleponSiswa = new Textbox(calonSiswa.getTeleponSiswa()));
		teleponSiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("III. Data Orang Tua"));

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Anak dari pegawai"));
		row.appendChild(orangTuaPegawai = new AmbilDataPegawaiBanbox(true));
		if (calonSiswa.getOrangTuaPegawai() != null) {
			orangTuaPegawai.setAttribute("pegawai", calonSiswa.getOrangTuaPegawai());
			orangTuaPegawai.setAttribute("myValue", calonSiswa.getOrangTuaPegawai());
			orangTuaPegawai
					.setName(calonSiswa.getOrangTuaPegawai() == null ? "" : calonSiswa.getOrangTuaPegawai().getNama());
		}
		orangTuaPegawai.setWidth("90%");
		orangTuaPegawai.setReadonly(true);

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "namaAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Nama Ayah" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(namaAyah = new Textbox(calonSiswa.getNamaAyah()));
		namaAyah.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "alamatAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Alamat Ayah" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(alamatAyah = new Textbox(calonSiswa.getAlamatAyah()));
		alamatAyah.setWidth("90%");
		alamatAyah.setRows(2);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "tempatLahirAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Tempat / Tanggal Lahir Ayah" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		Box hboxa = Common.isMobile() ? new Vbox() : new Hbox();
		hboxa.appendChild(tempatLahirAyah = new Textbox(calonSiswa.getTempatLahirAyah()));
		hboxa.appendChild(tanggalLahirAyah = new MyDatebox(calonSiswa.getTanggalLahirAyah()));
		row.appendChild(hboxa);
		tempatLahirAyah.setCols(15);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "hp1ayah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"No. HP Ayah" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		hbox = new Hbox();
		hbox.appendChild(hp1ayah = new Textbox(calonSiswa.getHp1ayah()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" atau ")));
		hbox.appendChild(hp2ayah = new Textbox(calonSiswa.getHp2ayah()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" atau ")));
		hbox.appendChild(hp3ayah = new Textbox(calonSiswa.getHp3ayah()));
		row.appendChild(hbox);
		hp1ayah.setCols(10);
		hp2ayah.setCols(10);
		hp3ayah.setCols(10);

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "waAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"No. HP Ayah (WA Aktif)" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(waAyah = new Textbox(calonSiswa.getWaAyah()));
		waAyah.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "pendidikanAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Pendidikan Ayah" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(pendidikanAyah = new Combobox());
		Common.insertCombo(pendidikanAyah, "nama", Pendidikan.class);
		Common.selectComboItem(pendidikanAyah, calonSiswa.getPendidikanAyah());
		pendidikanAyah.setWidth("90%");
		pendidikanAyah.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "pekerjaanAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Pekerjaan Ayah" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(pekerjaanAyah = new Combobox());
		Common.insertCombo(pekerjaanAyah, "nama", Pekerjaan.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(pekerjaanAyah, calonSiswa.getPekerjaanAyah());
		pekerjaanAyah.setWidth("90%");
		pekerjaanAyah.setReadonly(true);

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "penghasilanAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("Rata-rata penghasilan Ayah" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(penghasilanAyah = new Combobox());
		Common.insertCombo(penghasilanAyah, "nama", PenghasilanOrangTuaSiswa.class);
		Common.selectComboItem(penghasilanAyah, calonSiswa.getPenghasilanAyah());
		penghasilanAyah.setWidth("90%");
		penghasilanAyah.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "namaIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Nama Ibu" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(namaIbu = new Textbox(calonSiswa.getNamaIbu()));
		namaIbu.setWidth("90%");
		namaIbu.addEventListener("onChange", checkKesamaan);

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "alamatIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Alamat Ibu" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(alamatIbu = new Textbox(calonSiswa.getAlamatIbu()));
		alamatIbu.setWidth("90%");
		alamatIbu.setRows(2);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "tempatLahirIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Tempat / Tanggal Lahir Ibu" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		hboxa = Common.isMobile() ? new Vbox() : new Hbox();
		hboxa.appendChild(tempatLahirIbu = new Textbox(calonSiswa.getTempatLahirIbu()));
		hboxa.appendChild(tanggalLahirIbu = new MyDatebox(calonSiswa.getTanggalLahirIbu()));
		row.appendChild(hboxa);
		tempatLahirIbu.setCols(15);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "hp1ibu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("No. HP Ibu" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		hbox = new Hbox();
		hbox.appendChild(hp1ibu = new Textbox(calonSiswa.getHp1ibu()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" atau ")));
		hbox.appendChild(hp2ibu = new Textbox(calonSiswa.getHp2ibu()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" atau ")));
		hbox.appendChild(hp3ibu = new Textbox(calonSiswa.getHp3ibu()));
		row.appendChild(hbox);
		hp1ibu.setCols(10);
		hp2ibu.setCols(10);
		hp3ibu.setCols(10);

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "waIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"No. HP Ibu (WA Aktif)" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(waIbu = new Textbox(calonSiswa.getWaIbu()));
		waIbu.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "pendidikanIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Pendidikan Ibu" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(pendidikanIbu = new Combobox());
		Common.insertCombo(pendidikanIbu, "nama", Pendidikan.class);
		Common.selectComboItem(pendidikanIbu, calonSiswa.getPendidikanIbu());
		pendidikanIbu.setWidth("90%");
		pendidikanIbu.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "pekerjaanIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Pekerjaan Ibu" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(pekerjaanIbu = new Combobox());
		Common.insertCombo(pekerjaanIbu, "nama", Pekerjaan.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(pekerjaanIbu, calonSiswa.getPekerjaanIbu());
		pekerjaanIbu.setWidth("90%");
		pekerjaanIbu.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "penghasilanIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("Rata-rata penghasilan Ibu" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(penghasilanIbu = new Combobox());
		Common.insertCombo(penghasilanIbu, "nama", PenghasilanOrangTuaSiswa.class);
		Common.selectComboItem(penghasilanIbu, calonSiswa.getPenghasilanIbu());
		penghasilanIbu.setWidth("90%");
		penghasilanIbu.setReadonly(true);

		orangTuaPegawai.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Pegawai pegawai = (Pegawai) orangTuaPegawai.getAttribute("pegawai");
				if (pegawai != null && pegawai.getKelamin() != null
						&& pegawai.getKelamin().equalsIgnoreCase("Laki-laki")) {
					namaAyah.setValue(pegawai.getNama());
					if (pegawai.getTempatlahir() != null && !pegawai.getTempatlahir().trim().isEmpty()) {
						tempatLahirAyah.setValue(pegawai.getTempatlahir());
					}
					if (pegawai.getTanggallahir() != null) {
						tanggalLahirAyah.setValue(pegawai.getTanggallahir());
					}
				} else if (pegawai != null && pegawai.getKelamin() != null
						&& !pegawai.getKelamin().equalsIgnoreCase("Laki-laki")) {
					namaIbu.setValue(pegawai.getNama());
					if (pegawai.getTempatlahir() != null && !pegawai.getTempatlahir().trim().isEmpty()) {
						tempatLahirIbu.setValue(pegawai.getTempatlahir());
					}
					if (pegawai.getTanggallahir() != null) {
						tanggalLahirIbu.setValue(pegawai.getTanggallahir());
					}
				}
			}
		});

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "namaWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Nama Wali" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(namaWali = new Textbox(calonSiswa.getNamaWali()));
		namaWali.setWidth("90%");

		// Keterangan ditempel MENYATU di bawah input (bukan baris terpisah) dan
		// otomatis tidak tampil bila baris input disembunyikan konfigurasi.
		Common.keteranganDalamSel(row, "* Kosongkan nama Wali jika Wali nya adalah Ayah");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "alamatWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Alamat Wali" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(alamatWali = new Textbox(calonSiswa.getAlamatWali()));
		alamatWali.setWidth("90%");
		alamatWali.setRows(2);

		// Keterangan ditempel MENYATU di bawah input (bukan baris terpisah) dan
		// otomatis tidak tampil bila baris input disembunyikan konfigurasi.
		Common.keteranganDalamSel(row, "* Kosongkan alamat Wali jika Wali nya adalah Ayah");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "tempatLahirWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Tempat / Tanggal Lahir Wali" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		hboxa = Common.isMobile() ? new Vbox() : new Hbox();
		hboxa.appendChild(tempatLahirWali = new Textbox(calonSiswa.getTempatLahirWali()));
		hboxa.appendChild(tanggalLahirWali = new MyDatebox(calonSiswa.getTanggalLahirWali()));
		row.appendChild(hboxa);
		tempatLahirWali.setCols(15);

		// Keterangan ditempel MENYATU di bawah input (bukan baris terpisah) dan
		// otomatis tidak tampil bila baris input disembunyikan konfigurasi.
		Common.keteranganDalamSel(row, "* Kosongkan Tempat / Tanggal Lahir Wali jika Wali nya adalah Ayah");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "hp1wali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"No. HP Wali" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		hbox = new Hbox();
		hbox.appendChild(hp1wali = new Textbox(calonSiswa.getHp1wali()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" atau ")));
		hbox.appendChild(hp2wali = new Textbox(calonSiswa.getHp2wali()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" atau ")));
		hbox.appendChild(hp3wali = new Textbox(calonSiswa.getHp3wali()));
		row.appendChild(hbox);
		hp1wali.setCols(10);
		hp2wali.setCols(10);
		hp3wali.setCols(10);

		// Keterangan ditempel MENYATU di bawah input (bukan baris terpisah) dan
		// otomatis tidak tampil bila baris input disembunyikan konfigurasi.
		Common.keteranganDalamSel(row, "* Kosongkan nomor HP Wali jika Wali nya adalah Ayah");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "waWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"No. HP Wali (WA Aktif)" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(waWali = new Textbox(calonSiswa.getWaWali()));
		waWali.setWidth("90%");

		// Keterangan ditempel MENYATU di bawah input (bukan baris terpisah) dan
		// otomatis tidak tampil bila baris input disembunyikan konfigurasi.
		Common.keteranganDalamSel(row, "* Kosongkan nomor WA Wali jika Wali nya adalah Ayah");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "pendidikanWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Pendidikan Wali" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(pendidikanWali = new Combobox());
		Common.insertCombo(pendidikanWali, "nama", Pendidikan.class);
		Common.selectComboItem(pendidikanWali, calonSiswa.getPendidikanWali());
		pendidikanWali.setWidth("90%");
		pendidikanWali.setReadonly(true);

		// Keterangan ditempel MENYATU di bawah input (bukan baris terpisah) dan
		// otomatis tidak tampil bila baris input disembunyikan konfigurasi.
		Common.keteranganDalamSel(row, "* Kosongkan Pendidikan Wali jika Wali nya adalah Ayah");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "pekerjaanWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Pekerjaan Wali" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(pekerjaanWali = new Combobox());
		Common.insertCombo(pekerjaanWali, "nama", Pekerjaan.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(pekerjaanWali, calonSiswa.getPekerjaanWali());
		pekerjaanWali.setWidth("90%");
		pekerjaanWali.setReadonly(true);

		// Keterangan ditempel MENYATU di bawah input (bukan baris terpisah) dan
		// otomatis tidak tampil bila baris input disembunyikan konfigurasi.
		Common.keteranganDalamSel(row, "* Kosongkan Pekerjaan Wali jika Wali nya adalah Ayah");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "penghasilanWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("Rata-rata penghasilan Wali" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(penghasilanWali = new Combobox());
		Common.insertCombo(penghasilanWali, "nama", PenghasilanOrangTuaSiswa.class);
		Common.selectComboItem(penghasilanWali, calonSiswa.getPenghasilanWali());
		penghasilanWali.setWidth("90%");
		penghasilanWali.setReadonly(true);

		// Keterangan ditempel MENYATU di bawah input (bukan baris terpisah) dan
		// otomatis tidak tampil bila baris input disembunyikan konfigurasi.
		Common.keteranganDalamSel(row, "* Kosongkan Rata-rata penghasilan Wali jika Wali nya adalah Ayah");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "alamatEmail");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Alamat e-mail" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(alamatEmail = new Textbox(calonSiswa.getAlamatEmail()));
		alamatEmail.setWidth("90%");

		// PINDAH BAGIAN: Telepon Wali, Alamat & Telepon Orang Tua sebelumnya berada SETELAH
		// header "IV. Sekolah Asal" sehingga tampil seolah bagian dari data sekolah asal.
		// Ketiganya adalah data ORANG TUA/WALI, jadi dikembalikan ke bagian III.
		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "teleponWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Telepon Wali" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(teleponWali = new Textbox(calonSiswa.getTeleponWali()));
		teleponWali.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "alamatOrangTua");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Alamat Orang Tua" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(alamatOrangTua = new Textbox(calonSiswa.getAlamatOrangTua()));
		alamatOrangTua.setWidth("90%");
		alamatOrangTua.setRows(2);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "teleponOrangTua");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Telepon Orang Tua" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(teleponOrangTua = new Textbox(calonSiswa.getTeleponOrangTua()));
		teleponOrangTua.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("IV. Sekolah Asal"));

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "sekolahAsal");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nama Sekolah Asal " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		sekolahAsal = new Textbox(calonSiswa.getSekolahAsal());

		if (myGelombangPendaftaranPsb != null && myGelombangPendaftaranPsb.getAlumniDari() != null) {
			row.appendChild(new Label(myGelombangPendaftaranPsb.getAlumniDari().getNama()));
		} else {
			row.appendChild(sekolahAsal);
		}

		sekolahAsal.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "alamatSekolahAsal");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Alamat Sekolah Asal " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		alamatSekolahAsal = new Textbox(calonSiswa.getAlamatSekolahAsal());
		if (myGelombangPendaftaranPsb != null && myGelombangPendaftaranPsb.getAlumniDari() != null) {
			row.appendChild(new Label(myGelombangPendaftaranPsb.getAlumniDari().getAlamat()));
		} else {
			row.appendChild(alamatSekolahAsal);
		}

		alamatSekolahAsal.setWidth("90%");
		alamatSekolahAsal.setRows(2);

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "tahunLulus");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Tahun Lulus " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(tahunLulus = new Intbox(calonSiswa.getTahunLulus()));

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "noIjazah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"No. Ijazah " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(noIjazah = new Textbox(calonSiswa.getNoIjazah()));
		noIjazah.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "statusSekolah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Status Sekolah " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(statusSekolah = new Radiogroup());

		Radio radio1 = new Radio(CalonSiswa.NEGERI);
		radio1.setValue(CalonSiswa.NEGERI);
		statusSekolah.appendChild(radio1);

		Radio radio2 = new Radio(CalonSiswa.SWASTA);
		radio2.setValue(CalonSiswa.SWASTA);
		statusSekolah.appendChild(radio2);

		if (calonSiswa.getStatusSekolah() != null && calonSiswa.getStatusSekolah().equals(CalonSiswa.NEGERI)) {
			radio1.setSelected(true);
		} else if (calonSiswa.getStatusSekolah() != null && calonSiswa.getStatusSekolah().equals(CalonSiswa.SWASTA)) {
			radio2.setSelected(true);
		}

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "desaKelurahanSekolahAsal");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Kelurahan / Desa" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(desaKelurahanSekolahAsal = new Textbox(
				calonSiswa.getDesaKelurahanSekolahAsal() == null ? "" : calonSiswa.getDesaKelurahanSekolahAsal()));
		desaKelurahanSekolahAsal.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "kecamatanSekolahAsal");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Kecamatan" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(kecamatanSekolahAsal = new AmbilDataKecamatanBanbox());
		kecamatanSekolahAsal.setValue(
				calonSiswa.getKecamatanSekolahAsal() == null ? "" : calonSiswa.getKecamatanSekolahAsal().getNama());
		kecamatanSekolahAsal.setAttribute("wilayah", calonSiswa.getKecamatanSekolahAsal());
		kecamatanSekolahAsal.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "propinsiSekolahAsal");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Propinsi" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		propinsiSekolahAsal = new Label();
//		propinsiSekolahAsal.setReadonly(true);
		row.appendChild(propinsiSekolahAsal);
		propinsiSekolahAsal.setWidth("90%");

		propinsiSekolahAsal.setAttribute("wilayah", calonSiswa.getPropinsiSekolahAsal());

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "kotaSekolahAsal");
		Common.createFieldKota(rows, "Kota/Kabupaten" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : ""),
				kotaSekolahAsal = new Label(), propinsiSekolahAsal, calonSiswa.getKotaSekolahAsal(),
				statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB));

		Common.createKotaPropinsiListenerBerdasarkanKecamatan(propinsiSekolahAsal, kotaSekolahAsal,
				kecamatanSekolahAsal);

		kotaSekolahAsal.setAttribute("wilayah", calonSiswa.getKotaSekolahAsal());


		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonSiswaAction.statusWajibIsi(tbmuser, "keterangan");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Keterangan" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(keterangan = new Textbox(calonSiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("V. Prestasi Yang Pernah Diraih"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		array = new JSONArray(calonSiswa.getFormulaPrestasi());
		rowFormula = Common.tampilanScroll1(row);
		reloadFormula(rowFormula, array);

		final MyLabelStyled myRowStyledKelas = new MyLabelStyled("VI. Kelas yang dipilih");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(myRowStyledKelas);

		final MyFormRow rowDataG = new MyFormRow();
		rowDataG.setVisible(false);
		rowDataG.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(rowDataG, "2");

		row = new MyFormRow();
		row.setVisible(tbmuser != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Foto Siswa"));

		fotoCalonSiswa = null;

		Vbox vbox = new Vbox();
		vbox.setHeight("100%");
		vbox.setWidth("100%");
		vbox.setParent(row);

		Common.createDownloadUploadFoto(vbox, calonSiswa, FotoCalonSiswa.class, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				fotoCalonSiswa = (FotoCalonSiswa) arg0.getData();
			}
		}, true);

		rowParameterTambahan = new MyFormRow();
		rowParameterTambahan.setVisible(false);
		rowParameterTambahan.setStyle("border:0px;background: transparent;");
		rowParameterTambahan.setParent(rows);
		rowParameterTambahan.appendChild(new MyLabelBolder("VII. Form Tambahan"));

		parameterRows = new ArrayList<Row>();

		parameterTambahanListener = new ParameterTambahanPsbListener(calonSiswa, parameterRows, lampiranLains,
				gelombangPendaftaran, false, rows);

		if (calonSiswa.getId() != null) {
			subRowsVerifikasiKelengkapanCalonSiswa = VerifikasiPSBHelper.tampilkanVerifikasi(calonSiswa, rows, null,
					calonSiswa.getGelombangPendaftaranPsb());

			subRowsVerifikasiNilaiRapor = VerifikasiMatapelajaranPSBHelper.tampilkanVerifikasi(calonSiswa, rows,
					gelombangPendaftaran);

			subRowsVerifikasiNilaiParameter = VerifikasiParameterPSBHelper.tampilkanVerifikasi(calonSiswa, rows, null,
					calonSiswa.getGelombangPendaftaranPsb());
		}

		try {
			Component[] c = CalonSiswaAction.infoPindahan(rows, calonSiswa);
			merupakanPindahan = (MyCheckboxConfig) c[0];
			pindahanDariSekolah = (Textbox) c[1];
			alamatSekolahPindahan = (Textbox) c[2];
			keteranganPindah = (Textbox) c[3];
			tanggalPindah = (MyDatebox) c[4];
			kelasSekolahPindahan = (Textbox) c[5];
		} catch (Exception e) {
			// TODO Auto-generated catch block
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		Component[] c = CalonSiswaAction.infoDariMana(rows, calonSiswa);
		infoKampusDariMana = (Box) c[0];
		namaTemanInfoKampusDariMana = (Textbox) c[1];
		keteranganInfoKampusDariMana = (Textbox) c[2];

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setAlign("center");
		toolbar.setParent(south);

		if (tbmuser != null && Common.getCurrentUser() == null) {

			ujian = new MyButtonConfig(" I K U T   U J I A N  ", "/img/stock_data_edit_table.png");
			ujian.setVisible(false);
			ujian.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					TampilanUjianCalonSiswa tampilanUjianCalonSiswa = new TampilanUjianCalonSiswa();
					tampilanUjianCalonSiswa.init(CalonSiswaAction.this.calonSiswa);
					tampilanUjianCalonSiswa.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					tampilanUjianCalonSiswa.setHeight("95%");
					tampilanUjianCalonSiswa.setWidth("90%");
					tampilanUjianCalonSiswa.onModal();
				}
			});
			ujian.setParent(toolbar);

			checkApakahAdaUjian(calonSiswa);

			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (onSave(event)) {

						if (CalonSiswaAction.this.eventListener != null) {
							CalonSiswaAction.this.eventListener
									.onEvent(new Event("", save, CalonSiswaAction.this.calonSiswa));
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								if (CalonSiswaAction.this.calonSiswa.getNoUjian() != null
										&& !CalonSiswaAction.this.calonSiswa.getNoUjian().trim().isEmpty()) {
									CommonReportPsb.onCetakKartuUjianPSB(CalonSiswaAction.this.calonSiswa,
											CalonSiswaAction.this.calonSiswa.getNoUjian());
									return;
								}

								String noUjianGenerated = CommonPSB.generateNoUjian(CalonSiswaAction.this.calonSiswa);

								if (!noUjianGenerated.trim().isEmpty()) {
									CommonReportPsb.onCetakKartuUjianPSB(CalonSiswaAction.this.calonSiswa,
											noUjianGenerated);
									onSearchDefault(null);
								}

							}
						});
					}
				}
			});
			save.setParent(toolbar);
		} else {

			MyButtonConfig cancel = new MyButtonConfig("B A T A L", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					addWindow.setVisible(false);
				}
			});
			cancel.setParent(toolbar);

			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					baru = false;
					if (onSave(event)) {
						onSearchDefault(null);
						addWindow.setVisible(false);

						if (CalonSiswaAction.this.eventListener != null) {
							CalonSiswaAction.this.eventListener
									.onEvent(new Event("", save, CalonSiswaAction.this.calonSiswa));
						}
						if (baru) {
							String informasi = Common.getKonfigurasi("informasi_registrasi_psb_berhasil_login",
									"Proses pendaftaran peserta didik baru berhasil dilakukan dengan nomor pendaftaran : [no_reg]. Silahkan catat nomor pendaftaran tersebut dan selanjutnya akan diarahkan ke Login.")
									.getNilai();
							informasi = org.apache.commons.lang3.StringUtils.replace(informasi, "[no_reg]",
									CalonSiswaAction.this.calonSiswa.getNoRegistrasi());
							MyMessageboxConfig.show(informasi, "Informasi", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION, new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {

													if (baru && CalonSiswaAction.this.calonSiswa
															.getGelombangPendaftaranPsb()
															.getOtomatisLoginSetelahDaftar()) {
														CalonSiswaAction.this.calonSiswa.setTelahLogin(true);
														CalonSiswaAction.this.calonSiswa
																.setWaktuLogin(ais.ui.util.WaktuUtil.getDate());
														Common.refreshUpdate(CalonSiswaAction.this.calonSiswa);

														Common.setLogin(CalonSiswaAction.this.calonSiswa);
														Sessions.getCurrent(true).setAttribute("cetak", true);
														Common.createDefaultTimer(new EventListener() {

															@Override
															public void onEvent(Event arg0) throws Exception {
																Executions.getCurrent().sendRedirect("");
															}
														});
													} else {
														onCetakKartu(CalonSiswaAction.this.calonSiswa, daftar);
													}
												}
											});

										}
									});
						}
					}
				}
			});
			save.setParent(toolbar);
		}
		borderlayout.setParent(addWindow);

		final EventListener eventListenerKelasLesSiswa = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				GelombangPendaftaranPsb gelombangPendaftaranPsb = (GelombangPendaftaranPsb) (gelombangPendaftaran
						.getSelectedItem() == null ? null : gelombangPendaftaran.getSelectedItem().getValue());

				orangTuaPegawai.getParent().setVisible(
						gelombangPendaftaranPsb != null && gelombangPendaftaranPsb.getHanyaUntukAnakPegawai());

				merupakanPindahan.getParent().setVisible(
						gelombangPendaftaranPsb != null && gelombangPendaftaranPsb.getSiswaPindahanBolehMendaftar());
				pindahanDariSekolah.getParent().setVisible(merupakanPindahan.isChecked()
						&& gelombangPendaftaranPsb != null && gelombangPendaftaranPsb.getSiswaPindahanBolehMendaftar()
						&& Common.bolehKonfigurasi("tampil_nama_sekolah_calon_siswa_pindah_dari"));
				alamatSekolahPindahan.getParent().setVisible(merupakanPindahan.isChecked()
						&& gelombangPendaftaranPsb != null && gelombangPendaftaranPsb.getSiswaPindahanBolehMendaftar()
						&& Common.bolehKonfigurasi("tampil_alamat_sekolah_calon_siswa_pindah_dari"));
				keteranganPindah.getParent()
						.setVisible(merupakanPindahan.isChecked() && gelombangPendaftaranPsb != null
								&& gelombangPendaftaranPsb.getSiswaPindahanBolehMendaftar()
								&& Common.bolehKonfigurasi("tampil_keterangan_calon_siswa_pindah_dari"));
				tanggalPindah.getParent().setVisible(merupakanPindahan.isChecked() && gelombangPendaftaranPsb != null
						&& gelombangPendaftaranPsb.getSiswaPindahanBolehMendaftar()
						&& Common.bolehKonfigurasi("tampil_tanggal_pindah_calon_siswa_pindah_dari"));

				kelasSekolahPindahan.getParent()
						.setVisible(merupakanPindahan.isChecked() && gelombangPendaftaranPsb != null
								&& gelombangPendaftaranPsb.getSiswaPindahanBolehMendaftar()
								&& Common.bolehKonfigurasi("tampil_kelas_sebelum_pindah_calon_siswa_pindah_dari"));

				CalonSiswaAction.initBg(center, gelombangPendaftaranPsb);

				rowDataG.setVisible(false);

				selectedKelasLesSiswa = new HashSet<Long>();
				hapusKelasLesSiswa = new TreeSet<Long>();
				CalonSiswaAction.initKelasLes(rowDataG, gelombangPendaftaranPsb, selectedKelasLesSiswa,
						hapusKelasLesSiswa, calonSiswa);

				myRowStyledKelas.getParent().setVisible(rowDataG.isVisible());

				parameterTambahanListener.onEvent(null);

				jadwalPertemuanEventListener.onEvent(null);
			}

		};

		EventListener eventListenerSekolah = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				penjurusanSekolah.getParent().setVisible(false);
				Common.clear(penjurusanSekolah);

				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				System.out.println("s => " + s);

				if (s != null && s.getPenjurusanBolehDipilihSaatPsb()) {
					HibernateUtil.currentSession().refresh(s);
					Set<PenjurusanSekolah> selectedPenjurusanSekolah = s.getPenjurusanSekolahs();
					for (PenjurusanSekolah o : selectedPenjurusanSekolah) {
						if (o.getAktif() && o.getTampilkanDiPpdb()) {
							Comboitem comboitem = new Comboitem();
							comboitem.setLabel(o.getNama());
							comboitem.setDescription(o.getKeterangan());
							comboitem.setValue(o);
							penjurusanSekolah.appendChild(comboitem);
						}
					}
					penjurusanSekolah.getParent().setVisible(!selectedPenjurusanSekolah.isEmpty());
					Common.selectComboItem(penjurusanSekolah, CalonSiswaAction.this.calonSiswa.getPenjurusanSekolah());
				}

				if (myGelombangPendaftaranPsb != null) {
					CalonSiswaAction.this.calonSiswa.setGelombangPendaftaranPsb(myGelombangPendaftaranPsb);
				}

				Common.insertComboDanSemua(gelombangPendaftaran, new String[] { "nama", "tahunAjaran" }, "informasi",
						GelombangPendaftaranPsb.class, "Pilih Gelombang Pendaftaran",
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.and(
										Restrictions.and(Restrictions.le("mulai", ais.ui.util.WaktuUtil.getDate()),
												Restrictions.ge("sampai", ais.ui.util.WaktuUtil.getDate())),
										Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))));
				Common.selectComboItem(true, gelombangPendaftaran,
						CalonSiswaAction.this.calonSiswa.getGelombangPendaftaranPsb());

				GelombangPendaftaranPsb psb = (GelombangPendaftaranPsb) (gelombangPendaftaran.getSelectedItem() == null
						? null
						: gelombangPendaftaran.getSelectedItem().getValue());
				if (psb != null && psb.getPenjurusanSekolah() != null) {
					Common.selectComboItem(true, penjurusanSekolah, psb.getPenjurusanSekolah());
					penjurusanSekolah.setDisabled(true);
				} else {
					penjurusanSekolah.setDisabled(false);
				}

				List<Long> paketPsbPunyaGelombangPendaftaranPsbs = HibernateUtil.currentSession()
						.createCriteria(PaketPsbPunyaGelombangPendaftaranPsb.class)
						.setProjection(Projections.groupProperty("paketPsb.id"))
						.add(Restrictions.eq("gelombangPendaftaranPsb", psb)).list();

				Criterion criterion = Restrictions.sqlRestriction("false");
				if (!paketPsbPunyaGelombangPendaftaranPsbs.isEmpty()) {
					criterion = Restrictions.in("id", paketPsbPunyaGelombangPendaftaranPsbs);
				}

				paketPsb.getParent().setVisible(!paketPsbPunyaGelombangPendaftaranPsbs.isEmpty());
				Common.clear(paketPsb);
				Common.insertComboDanSemua(paketPsb, new String[] { "nama" }, "keterangan", PaketPsb.class,
						"Pilih Paket", criterion);
				Common.selectComboItem(true, paketPsb, CalonSiswaAction.this.calonSiswa.getPaketPsb());

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (gelombangPendaftaran.getSelectedItem() == null
								|| gelombangPendaftaran.getSelectedItem().getValue() == null) {
							gelombangPendaftaran.setSelectedIndex(0);
						}

						eventListenerKelasLesSiswa.onEvent(null);
					}
				});
			}
		};

		sekolah.addEventListener("onChange", eventListenerSekolah);
		Common.createDefaultTimer(eventListenerSekolah);

		if (gelombangPendaftaran != null)
			gelombangPendaftaran.addEventListener("onChange", eventListenerKelasLesSiswa);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pernyataan = Common.tambahKeteranganRowHtml(rows,
						"Dengan ini saya menyatakan bahwa data yang saya masukkan benar adanya, dan jika ternyata dikemudian hari ditemukan kesalahan pada data ini baik yang disengaja ataupun tidak disengaja maka saya bersedia menerima sanksi dan resiko yang ditimbulkan karenanya");

				pernyataan.setChecked(calonSiswa.getPernyataan());

				if (calonSiswa.getId() != null) {
					pernyataan.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Session session = HibernateUtil.currentSession();
							session.refresh(calonSiswa);

							calonSiswa.setPernyataan(pernyataan.isChecked());

							Common.refreshUpdate(session, calonSiswa);

							session.flush();

						}
					});
				}
			}
		});

		Common.masukkanListener(rows, masukkanPerubahan);

	}

	public static void reloadDataFormula(final Row rowU, final JSONArray array) throws Exception {
		Common.clear(rowU);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(rowU);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Prestasi");
		column.setParent(columns);
		column.setWidth("70%");

		column = new MyColumnConfig("Level");
		column.setParent(columns);
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		Rows rows = new Rows();
		rows.setParent(grid);

		for (int i = 0; i < array.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = array.getJSONObject(i);

			String prestasi = "";
			String level = "";

			if (!jsonObject.isNull("prestasi")) {
				prestasi = jsonObject.get("prestasi") + "";
			}

			if (!prestasi.isEmpty()) {

				if (!jsonObject.isNull("level")) {
					level = jsonObject.get("level") + "";
				}

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				final MyTextbox targetText = new MyTextbox(prestasi);
				final MyCombobox levelbox = new MyCombobox();
				targetText.setWidth("90%");
				row.appendChild(targetText);
				levelbox.setWidth("90%");
				row.appendChild(levelbox);

				for (String s : new String[] { "Sekolah", "Regional", "Nasional", "Internasional" }) {
					MyComboitemConfig comboitemConfig = new MyComboitemConfig(s);
					comboitemConfig.setValue(s);
					levelbox.appendChild(comboitemConfig);
				}

				levelbox.setReadonly(true);
				Common.selectComboItem(levelbox, level);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						jsonObject.put("prestasi", targetText.getValue());

						jsonObject.put("level",
								levelbox.getSelectedItem() == null ? "" : levelbox.getSelectedItem().getValue());

					}
				};

				levelbox.addEventListener("onChange", eventListener);
				targetText.addEventListener("onChange", eventListener);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
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
											try {
												array.put(index, new JSONObject());

												reloadDataFormula(rowU, array);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(
														"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
																+ e.getMessage());
											}

										}

									}
								});

					}
				});
				button.setParent(row);
			}
		}
	}

	public static void reloadFormula(final Row rowFormula, final JSONArray array) throws Exception {
		final MyFormRow rowU = new MyFormRow();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Prestasi", "/img/svg/addthis.svg");
		button.setTooltiptext("Hapus Data");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("prestasi", "Prestasi ...");
				array.put(jsonObject);

				reloadDataFormula(rowU, array);
			}
		});
		button.setParent(rowFormula);

		rowU.setParent(rowFormula.getParent());

		reloadDataFormula(rowU, array);

	}

	public boolean onSave(Event event) throws Exception {
		if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Yayasan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Sekolah harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (nik != null && nik.isVisible() && nik.getParent() != null && nik.getParent().isVisible()) {
			if (!nik.getValue().trim().isEmpty() && nik.getValue().trim().length() != 16) {
				MyMessageboxConfig.show("NIK harus 16 digit", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								nik.focus();
								Clients.scrollIntoView(nik);
							}
						});
				return false;
			}
		}

		if (keluarga != null && keluarga.getParent() != null && keluarga.getParent().isVisible()) {
			Keluarga k = (Keluarga) (keluarga.getSelectedItem() == null ? null : keluarga.getSelectedItem().getValue());
			if (k == null) {
				MyMessageboxConfig.show("Anak pegawai harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								keluarga.focus();
								keluarga.select();
								Clients.scrollIntoView(keluarga);
							}
						});
				return false;
			}
		}

		Sekolah s = (Sekolah) sekolah.getSelectedItem().getValue();
		if (s.getPenjurusanBolehDipilihSaatPsb()) {
			if (penjurusanSekolah.getParent() != null && penjurusanSekolah.getParent().isVisible()
					&& (penjurusanSekolah.getSelectedItem() == null
							|| penjurusanSekolah.getSelectedItem().getValue() == null)) {
				MyMessageboxConfig.show("Penjurusan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								penjurusanSekolah.focus();
								Clients.scrollIntoView(penjurusanSekolah);
							}
						});
				return false;
			}
		}

		if (paketPsb.getParent() != null && paketPsb.getParent().isVisible()
				&& (paketPsb.getSelectedItem() == null || paketPsb.getSelectedItem().getValue() == null)) {
			MyMessageboxConfig.show("Paket harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							paketPsb.focus();
							Clients.scrollIntoView(paketPsb);
						}
					});
			return false;
		}

		if (gelombangPendaftaran.getSelectedItem() == null
				|| gelombangPendaftaran.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Gelombang Pendaftaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							gelombangPendaftaran.focus();
							Clients.scrollIntoView(gelombangPendaftaran);
						}
					});
			return false;
		}
		Siswa alumni = (Siswa) (siswaAlumni == null ? null : siswaAlumni.getAttribute("siswa"));
		GelombangPendaftaranPsb gelombangPendaftaranPsb = (GelombangPendaftaranPsb) gelombangPendaftaran
				.getSelectedItem().getValue();
		if (gelombangPendaftaranPsb != null && gelombangPendaftaranPsb.getHarusSebagaiAlumni() && siswaAlumni != null) {

			if (alumni == null) {
				MyMessageboxConfig.show("Siswa alumni harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								siswaAlumni.focus();
								siswaAlumni.select();
								Clients.scrollIntoView(siswaAlumni);
							}
						});
				return false;
			}
		}

		if (namaSiswa.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Calon Siswa harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							namaSiswa.focus();
							Clients.scrollIntoView(namaSiswa);
						}
					});
			return false;
		}

		if (jenisKelamin.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis kelamin harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							jenisKelamin.focus();
							Clients.scrollIntoView(jenisKelamin);
						}
					});
			return false;
		}

		if (alamatEmail != null && !alamatEmail.getValue().trim().isEmpty()
				&& !Common.isValidEmailAddress(alamatEmail.getValue().trim())) {
			MyMessageboxConfig.show("Format email harus benar", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							alamatEmail.focus();
							alamatEmail.select();
							Clients.scrollIntoView(alamatEmail);
						}
					});
			return false;
		}

		if (!parameterTambahanListener.validate()) {
			return false;
		}

		if (!pernyataan.isChecked()) {
			MyMessageboxConfig.show("Pernyataan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							pernyataan.focus();
							Clients.scrollIntoView(pernyataan);
						}
					});
			return false;
		}

		if (!nomorIndukNasional.getValue().trim().isEmpty()) {

			GelombangPendaftaranPsb gel = gelombangPendaftaran.getSelectedItem() == null ? null
					: (GelombangPendaftaranPsb) gelombangPendaftaran.getSelectedItem().getValue();

			if (gel != null) {

				if (!Common.isNumber(nomorIndukNasional.getValue().trim())) {
					MyMessageboxConfig.showFormatCb(
							"Mohon maaf, NISN Pendidikan Sebelumnya \"{V1}\" tidak valid karena harus berupa angka. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan NISN dan pastikan hanya terdiri dari angka; (2) Perbaiki isian NISN tersebut; (3) Apabila masih mengalami kendala, silakan hubungi: {V2}",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									nomorIndukNasional.focus();
									nomorIndukNasional.select();
									Clients.scrollIntoView(nomorIndukNasional);
								}
							}, nomorIndukNasional.getValue().trim(),
							Common.getKonfigurasi("hubungi_admin_calon_mhs",
									"Silahkan hubungi admin di Nomor \r\n" + "WA : ...... / ..... \r\n"
											+ "atau email : .....\r\n" + "")
									.getNilai());

					return false;
				}

				int jml = ((Number) HibernateUtil.currentSession().createCriteria(CalonSiswa.class)
						.add(Restrictions.eq("tahunMasuk", gel.getTahunMasuk()))
						.add(Restrictions.isNotNull("gelombangPendaftaranPsb")).setProjection(Projections.rowCount())
						.add(calonSiswa.getId() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.ne("id", calonSiswa.getId()))
						.add(Restrictions.ilike("nomorIndukNasional", nomorIndukNasional.getValue().trim(),
								MatchMode.EXACT))
						.uniqueResult()).intValue();

				if (jml > 0) {

					MyMessageboxConfig.showFormatCb(
							"Mohon maaf, NISN Pendidikan Sebelumnya \"{V1}\" sudah terdaftar sebelumnya, sehingga tidak dapat digunakan kembali. Langkah yang dapat dilakukan: (1) Pastikan Ananda belum pernah didaftarkan sebelumnya; (2) Periksa kembali penulisan NISN; (3) Apabila masih mengalami kendala, silakan hubungi: {V2}",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									nomorIndukNasional.focus();
									nomorIndukNasional.select();
									Clients.scrollIntoView(nomorIndukNasional);
								}
							}, nomorIndukNasional.getValue().trim(),
							Common.getKonfigurasi("hubungi_admin_calon_mhs",
									"Silahkan hubungi admin di Nomor \r\n" + "WA : ...... / ..... \r\n"
											+ "atau email : .....\r\n" + "")
									.getNilai());

					return false;
				}
			}
		}

		JadwalPertemuanPSB d = (JadwalPertemuanPSB) (jadwalPertemuanPSB.getSelectedItem() == null ? null
				: jadwalPertemuanPSB.getSelectedItem().getValue());
		if (d != null) {

			int jml = ((Number) HibernateUtil.currentSession().createCriteria(CalonSiswa.class)
					.setProjection(Projections.rowCount())
					.add(calonSiswa.getId() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.ne("id", calonSiswa.getId()))
					.add(Restrictions.eq("jadwalPertemuanPSB", d)).uniqueResult()).intValue();

			if (jml >= d.getKuota()) {

				MyMessageboxConfig.showFormatCb(
						"Mohon maaf, kuota untuk jadwal pertemuan \"{V1}\" telah penuh. Langkah yang dapat dilakukan: (1) Tutup pesan ini; (2) Pilih jadwal pertemuan lain yang kuotanya masih tersedia; (3) Lanjutkan proses pendaftaran.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								jadwalPertemuanPSB.focus();
								jadwalPertemuanPSB.select();
								Clients.scrollIntoView(jadwalPertemuanPSB);
							}
						}, d.getNama());

				return false;
			}
		}

		if (rowjadwalPertemuanPSB.isVisible()
				&& (jadwalPertemuanPSB.getSelectedItem() == null
						|| jadwalPertemuanPSB.getSelectedItem().getValue() == null)
				&& jadwalPertemuanPSB.getChildren().size() > 1) {
			MyMessageboxConfig.show("Jadwal pertemuan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							jadwalPertemuanPSB.focus();
							jadwalPertemuanPSB.select();
							Clients.scrollIntoView(jadwalPertemuanPSB);
						}
					});
			return false;
		}

		if (orangTuaPegawai.getParent() != null && orangTuaPegawai.getParent().isVisible()
				&& orangTuaPegawai.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, data Orang Tua Pegawai belum diisi. Langkah yang dapat dilakukan: (1) Klik kolom Orang Tua Pegawai; (2) Pilih data pegawai yang bersangkutan dari daftar; (3) Ulangi proses penyimpanan.",
					"Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							orangTuaPegawai.focus();
							orangTuaPegawai.select();
							Clients.scrollIntoView(orangTuaPegawai);
						}
					});

			return false;
		}

		if (merupakanPindahan.isChecked()) {
			if (pindahanDariSekolah != null && pindahanDariSekolah.getParent() != null
					&& pindahanDariSekolah.getParent().isVisible()
					&& pindahanDariSekolah.getValue().trim().equals("")) {
				MyMessageboxConfig.show("Asal sekolah sebelumnya harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								pindahanDariSekolah.focus();
								pindahanDariSekolah.select();
								Clients.scrollIntoView(pindahanDariSekolah);
							}
						});
				return false;
			}
			if (kelasSekolahPindahan != null && kelasSekolahPindahan.getParent() != null
					&& kelasSekolahPindahan.getParent().isVisible()
					&& kelasSekolahPindahan.getValue().trim().equals("")) {
				MyMessageboxConfig.show("Kelas terakhir sekolah sebelumnya harus diisi", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								kelasSekolahPindahan.focus();
								kelasSekolahPindahan.select();
								Clients.scrollIntoView(kelasSekolahPindahan);
							}
						});
				return false;
			}
			if (alamatSekolahPindahan != null && alamatSekolahPindahan.getParent() != null
					&& alamatSekolahPindahan.getParent().isVisible()
					&& alamatSekolahPindahan.getValue().trim().equals("")) {
				MyMessageboxConfig.show("Alamat sekolah sebelumnya harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								alamatSekolahPindahan.focus();
								alamatSekolahPindahan.select();
								Clients.scrollIntoView(alamatSekolahPindahan);
							}
						});
				return false;
			}
			if (tanggalPindah != null && tanggalPindah.getParent() != null && tanggalPindah.getParent().isVisible()
					&& tanggalPindah.getValue() == null) {
				MyMessageboxConfig.show("Tanggal pindah harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								tanggalPindah.focus();
								tanggalPindah.select();
								Clients.scrollIntoView(tanggalPindah);
							}
						});
				return false;
			}
		}

		if (gelombangPendaftaran.getSelectedItem() != null && gelombangPendaftaran.getSelectedItem().getValue() != null
				&& !namaSiswa.getValue().trim().isEmpty() && tanggalLahir.getValue() != null) {

			GelombangPendaftaranPsb gel = gelombangPendaftaran.getSelectedItem() == null ? null
					: (GelombangPendaftaranPsb) gelombangPendaftaran.getSelectedItem().getValue();

			if (gel != null) {

				int jml = ((Number) HibernateUtil.currentSession().createCriteria(CalonSiswa.class)
						.add(Restrictions.eq("tahunMasuk", gel.getTahunMasuk()))
						.add(Restrictions.isNotNull("gelombangPendaftaranPsb")).setProjection(Projections.rowCount())
						.add(calonSiswa.getId() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.ne("id", calonSiswa.getId()))
						.add(Restrictions.ilike("namaSiswa", namaSiswa.getValue().trim(), MatchMode.EXACT))
						.add(Restrictions.eq("tanggalLahir", tanggalLahir.getValue()))
						.createAlias("gelombangPendaftaranPsb", "gelombangPendaftaranPsb").add(Restrictions
								.eq("gelombangPendaftaranPsb.tahunAjaran", gelombangPendaftaranPsb.getTahunAjaran()))
						.uniqueResult()).intValue();

				if (jml > 0) {

					MyMessageboxConfig.showFormatCb(
							"Mohon maaf, Nama Siswa \"{V1}\" dengan Tanggal Lahir \"{V2}\" sudah terdaftar pada tahun pelajaran \"{V3}\", sehingga tidak dapat didaftarkan kembali. Langkah yang dapat dilakukan: (1) Pastikan Ananda belum pernah didaftarkan sebelumnya; (2) Periksa kembali Nama Siswa dan Tanggal Lahir yang dimasukkan; (3) Apabila masih mengalami kendala, silakan hubungi: {V4}",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									tanggalLahir.focus();
									tanggalLahir.select();
									Clients.scrollIntoView(tanggalLahir);
								}
							}, namaSiswa.getValue().trim(),
							Common.dateFormat2.get().format(tanggalLahir.getValue()),
							gelombangPendaftaranPsb.getTahunAjaran(),
							Common.getKonfigurasi("hubungi_admin_calon_mhs",
									"Silahkan hubungi admin di Nomor \r\n" + "WA : ...... / ..... \r\n"
											+ "atau email : .....\r\n" + "")
									.getNilai());

					return false;
				}
			}
		}

		if (!CalonSiswaAction.checkInfoDariMana(infoKampusDariMana, namaTemanInfoKampusDariMana,
				keteranganInfoKampusDariMana)) {
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (calonSiswa.getId() != null && calonSiswa.getId() > 0L) {
			calonSiswa = (CalonSiswa) session.load(CalonSiswa.class, calonSiswa.getId());

		}

		if (calonSiswa.getId() != null && calonSiswa.getId() < 0L) {
			calonSiswa.setId(null);
		}

		setdata();

		List<String> daftarWajibDiisi = KonfigurasiTampilanBiodataCalonSiswaAction.dataYangWajibDiisi(tbmuser);
		for (String key : daftarWajibDiisi) {
			if (Common.checkIsNull(CalonSiswa.class, calonSiswa, key)) {

				MyMessageboxConfig.show(
						"Biodata Anda harus dilengkapi. Data \""
								+ KonfigurasiTampilanBiodataCalonSiswaAction.keyDesc(key)
								+ "\" masih belum terisi dengan benar",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		// Guard: jika session ditutup oleh side-effect (misalnya getter entity memanggil
		// HibernateUtil.closeSession() di thread yang sama), ambil session baru yang valid.
		if (!ais.database.hibernate.HibernateUtil.isSessionUsable(session)) {
			session = ais.database.hibernate.HibernateUtil.currentSession();
		}
		baru = false;
		if (calonSiswa.getId() == null) {
			session.save(calonSiswa);
			baru = true;
		} else {
			Common.refreshSaveOrUpdate(session, calonSiswa);
		}

		if (fotoCalonSiswa != null) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.refresh(fotoCalonSiswa);
			fotoCalonSiswa.setCalonSiswa(calonSiswa.getId());
			streamingSession.getTransaction().begin();
			streamingSession.update(fotoCalonSiswa);
			streamingSession.getTransaction().commit();

			StreamingHibernateUtil.getInstance().closeSession();
		}

		VerifikasiParameterPSBHelper.simpanVerifikasi(calonSiswa, subRowsVerifikasiNilaiParameter);
		VerifikasiMatapelajaranPSBHelper.simpanVerifikasi(calonSiswa, subRowsVerifikasiNilaiRapor);
		VerifikasiPSBHelper.simpanVerifikasi(calonSiswa, subRowsVerifikasiKelengkapanCalonSiswa);

		if (!lampiranLains.isEmpty()) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
					streamingSession.getTransaction().begin();
					for (LampiranLain lampiranLain : lampiranLains.values()) {
						streamingSession.refresh(lampiranLain);
						lampiranLain.setRef(calonSiswa.getId());
						streamingSession.update(lampiranLain);
					}
					streamingSession.getTransaction().commit();
					StreamingHibernateUtil.getInstance().closeSession();
				}
			});
		}

		if (this.hapusKelasLesSiswa != null) {
			for (Long kelasLesSiswa : this.hapusKelasLesSiswa) {
				session.createSQLQuery("delete from sekolah.kelas_les_punya_siswa where kelas_id=" + kelasLesSiswa
						+ " and calon_siswa=" + calonSiswa.getId()).executeUpdate();
			}
		}

		Common.hapusSession(CalonSiswa.class);

		if (baru && calonSiswa.getGelombangPendaftaranPsb().getMunculkanTagihanSetelahDaftar()
				&& !calonSiswa.getGelombangPendaftaranPsb().getOtomatisLoginSetelahDaftar()) {

			calonSiswa.munculkanFormPembayaran(eventListener);
		}

		try {
			if (calonSiswa.getGelombangPendaftaranPsb().getLangsungDapatNisSaatDaftar()
					&& calonSiswa.getSiswa() == null) {
				NisGenerator nisGenerator = (NisGenerator) Class
						.forName(Common.getKonfigurasi("class_untuk_generate_nis", DefaultNisGenerator.class.getName())
								.getNilai().trim())
						.newInstance();
				calonSiswa.setTelahDiterima(true);
				CommonPSB.onGenerateNis(calonSiswa, nisGenerator, false);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		return true;
	}

	private void setdata() {

		try {
			String info = BiodataCalonMahasiswaAction.infoMahasiswaBaru(infoKampusDariMana);
			if (jadwalPertemuanPSB.getSelectedItem() != null) {
				JadwalPertemuanPSB d = (JadwalPertemuanPSB) jadwalPertemuanPSB.getSelectedItem().getValue();
				calonSiswa.setJadwalPertemuanPSB(d);
			}
			calonSiswa.setOrangTuaPegawai((Pegawai) orangTuaPegawai.getAttribute("pegawai"));
			calonSiswa.setMerupakanPindahan(merupakanPindahan.isChecked());
			calonSiswa.setPindahanDariSekolah(pindahanDariSekolah.getValue());
			calonSiswa.setAlamatSekolahPindahan(alamatSekolahPindahan.getValue());
			calonSiswa.setKeteranganPindah(keteranganPindah.getValue());
			calonSiswa.setTanggalPindah(tanggalPindah.getValue());
			calonSiswa.setInfoKampusDariMana(info);
			calonSiswa.setKeteranganInfoKampusDariMana(
					keteranganInfoKampusDariMana == null ? "" : keteranganInfoKampusDariMana.getValue());
			calonSiswa.setNamaTemanInfoKampusDariMana(
					namaTemanInfoKampusDariMana == null ? "" : namaTemanInfoKampusDariMana.getValue());
			calonSiswa.setNamaSiswa(namaSiswa.getValue().trim());
			calonSiswa.setNoAktaKelahiran(noAktaKelahiran.getValue().trim());
			calonSiswa.setGelombangPendaftaranPsb(gelombangPendaftaran.getSelectedItem() == null ? null
					: (GelombangPendaftaranPsb) gelombangPendaftaran.getSelectedItem().getValue());
			calonSiswa.setNamaSiswa(namaSiswa.getValue());
			calonSiswa.setPanggilan(panggilan.getValue());
			calonSiswa.setNoRegistrasi(nomorInduk.getValue());
			calonSiswa.setNomorInduk(nomorInduk.getValue());

			calonSiswa.setJenisKelamin(jenisKelamin.getValue());
			calonSiswa.setTempatLahir(tempatLahir.getValue());
			calonSiswa.setTanggalLahir(tanggalLahir.getValue());
			calonSiswa.setAgama((Agama) (agama.getSelectedItem() == null ? null : agama.getSelectedItem().getValue()));
			calonSiswa.setAnakKe(anakKe.getValue());
			calonSiswa.setDariAnakKe(dariAnakKe.getValue());
			calonSiswa.setJumlahSaudaraKandung(jumlahSaudaraKandung.getValue());
			calonSiswa.setJumlahSaudaraTiri(jumlahSaudaraTiri.getValue());
			calonSiswa.setKewarganegaraan(kewarganegaraan.getValue());
			calonSiswa.setNegara((Negara) negara.getAttribute("negara"));
			calonSiswa.setTeleponSiswa(teleponSiswa.getValue());
			calonSiswa.setAlamatEmail(alamatEmail.getValue());
			calonSiswa.setNamaAyah(namaAyah.getValue());
			calonSiswa.setNamaIbu(namaIbu.getValue());
			calonSiswa.setNamaWali(namaWali.getValue());
			calonSiswa.setKeterangan(keterangan.getValue());
			calonSiswa.setPekerjaanAyah((Pekerjaan) (pekerjaanAyah.getSelectedItem() == null ? null
					: pekerjaanAyah.getSelectedItem().getValue()));
			calonSiswa.setPekerjaanIbu((Pekerjaan) (pekerjaanIbu.getSelectedItem() == null ? null
					: pekerjaanIbu.getSelectedItem().getValue()));
			calonSiswa.setPekerjaanWali((Pekerjaan) (pekerjaanWali.getSelectedItem() == null ? null
					: pekerjaanWali.getSelectedItem().getValue()));
			calonSiswa.setKelasSekolahPindahan(kelasSekolahPindahan.getValue().trim());
			calonSiswa.setPendidikanAyah((Pendidikan) (pendidikanAyah.getSelectedItem() == null ? null
					: pendidikanAyah.getSelectedItem().getValue()));
			calonSiswa.setPendidikanIbu((Pendidikan) (pendidikanIbu.getSelectedItem() == null ? null
					: pendidikanIbu.getSelectedItem().getValue()));
			calonSiswa.setPendidikanWali((Pendidikan) (pendidikanWali.getSelectedItem() == null ? null
					: pendidikanWali.getSelectedItem().getValue()));

			calonSiswa.setPenghasilanAyah((PenghasilanOrangTuaSiswa) (penghasilanAyah.getSelectedItem() == null ? null
					: penghasilanAyah.getSelectedItem().getValue()));
			calonSiswa.setPenghasilanIbu((PenghasilanOrangTuaSiswa) (penghasilanIbu.getSelectedItem() == null ? null
					: penghasilanIbu.getSelectedItem().getValue()));
			calonSiswa.setPenghasilanWali((PenghasilanOrangTuaSiswa) (penghasilanWali.getSelectedItem() == null ? null
					: penghasilanWali.getSelectedItem().getValue()));

			calonSiswa.setAlamatOrangTua(alamatOrangTua.getValue());
			calonSiswa.setAlamatSiswa(alamatSiswa.getValue());
			calonSiswa.setAlamatWali(alamatWali.getValue());
			calonSiswa.setTeleponOrangTua(teleponOrangTua.getValue());
			calonSiswa.setTeleponWali(teleponWali.getValue());
			calonSiswa.setSekolahAsal(sekolahAsal.getValue());
			calonSiswa.setBerat(berat.getValue());
			calonSiswa.setTinggi(tinggi.getValue());
			calonSiswa.setGolonganDarah(golonganDarah.getValue());
			calonSiswa.setBahasa(bahasa.getValue());
			calonSiswa.setStatusDalamKeluarga((String) (statusDalamKeluarga.getSelectedItem() == null ? null
					: statusDalamKeluarga.getSelectedItem().getValue()));

			calonSiswa.setHobby(hobby.getValue());
			calonSiswa.setTempatLahirAyah(tempatLahirAyah.getValue());
			calonSiswa.setTempatLahirIbu(tempatLahirIbu.getValue());
			calonSiswa.setTanggalLahirAyah(tanggalLahirAyah.getValue());
			calonSiswa.setTanggalLahirIbu(tanggalLahirIbu.getValue());

			calonSiswa.setTempatLahirWali(tempatLahirWali.getValue());
			calonSiswa.setTanggalLahirWali(tanggalLahirWali.getValue());

			calonSiswa.setRiwayatPenyakit(riwayatPenyakit.getValue());

			calonSiswa.setHp1ayah(hp1ayah.getValue());
			calonSiswa.setHp2ayah(hp2ayah.getValue());
			calonSiswa.setHp3ayah(hp3ayah.getValue());

			calonSiswa.setHp1ibu(hp1ibu.getValue());
			calonSiswa.setHp2ibu(hp2ibu.getValue());
			calonSiswa.setHp3ibu(hp3ibu.getValue());

			calonSiswa.setHp1wali(hp1wali.getValue());
			calonSiswa.setHp2wali(hp2wali.getValue());
			calonSiswa.setHp3wali(hp3wali.getValue());
			calonSiswa.setPernyataan(pernyataan.isChecked());
			calonSiswa.setDusunCalon(dusunCalon.getValue());
			calonSiswa.setRt(rt.getValue());
			calonSiswa.setRw(rw.getValue());
			calonSiswa.setKodePos(kodePos.getValue());
			calonSiswa.setKelurahanCalon(kelurahanCalon.getValue());
			calonSiswa.setKecamatanCalon((Wilayah) kecamatanCalon.getAttribute("wilayah"));
			calonSiswa.setPropinsiCalon((Propinsi) (propinsiCalon.getAttribute("wilayah")));
			calonSiswa.setKotaCalon((Kota) (kotaCalon.getAttribute("wilayah")));

			calonSiswa.setAlamatSekolahAsal(alamatSekolahAsal.getValue());
			calonSiswa.setPenjurusanSekolah((PenjurusanSekolah) (penjurusanSekolah.getSelectedItem() == null ? null
					: penjurusanSekolah.getSelectedItem().getValue()));

			calonSiswa.setPaketPsb(
					(PaketPsb) (paketPsb.getSelectedItem() == null ? null : paketPsb.getSelectedItem().getValue()));

			if (calonSiswa.getId() == null) {
				calonSiswa.setNoRegistrasi(CommonPSB.generateNoRegistrasi(calonSiswa));
			}
			calonSiswa.setNomorIndukNasional(nomorIndukNasional.getValue());

			calonSiswa.setAlamatAyah(alamatAyah.getValue());
			calonSiswa.setAlamatIbu(alamatIbu.getValue());
			calonSiswa.setNik(nik.getValue());
			calonSiswa.setKk(kk.getValue());
			calonSiswa.setWaAyah(waAyah.getValue());
			calonSiswa.setWaIbu(waIbu.getValue());
			calonSiswa.setWaWali(waWali.getValue());

			/* Input bisa berisi teks tidak valid (mis. "2020L"): jangan
			 * lempar WrongValueException, ambil digit-nya saja. */
			Integer nilaiTahunLulus = null;
			try {
				nilaiTahunLulus = tahunLulus.getValue();
			} catch (Exception eTahun) {
				try {
					String teks = String.valueOf(tahunLulus.getText()).replaceAll("[^0-9]", "");
					nilaiTahunLulus = teks.length() == 0 ? null : Integer.valueOf(teks);
				} catch (Exception eTahun2) {
					nilaiTahunLulus = null;
				}
			}
			calonSiswa.setTahunLulus(nilaiTahunLulus);

			calonSiswa.setAlatTransportasiMahasiswa(
					(AlatTransportasiMahasiswa) (alatTransportasiMahasiswa.getSelectedItem() == null ? null
							: alatTransportasiMahasiswa.getSelectedItem().getValue()));
			calonSiswa.setJenisTinggalMahasiswa(
					(JenisTinggalMahasiswa) (jenisTinggalMahasiswa.getSelectedItem() == null ? null
							: jenisTinggalMahasiswa.getSelectedItem().getValue()));

			calonSiswa.setDesaKelurahanSekolahAsal(desaKelurahanSekolahAsal.getValue());
			calonSiswa.setKecamatanSekolahAsal((Wilayah) kecamatanSekolahAsal.getAttribute("wilayah"));
			calonSiswa.setKotaSekolahAsal((Kota) (kotaSekolahAsal.getAttribute("wilayah")));
			calonSiswa.setPropinsiSekolahAsal((Propinsi) (propinsiSekolahAsal.getAttribute("wilayah")));

			calonSiswa.setFormulaPrestasi(array.toString());

			calonSiswa.setStatusSekolah(
					statusSekolah.getSelectedItem() == null || statusSekolah.getSelectedItem().getValue() == null ? null
							: statusSekolah.getSelectedItem().getValue().toString());

			String jenisS = "";
			if (this.selectedKelasLesSiswa != null) {
				for (Long kelasLesSiswa : this.selectedKelasLesSiswa) {
					jenisS += jenisS.isEmpty() ? kelasLesSiswa.toString() : "," + kelasLesSiswa;
				}
			}
			calonSiswa.setKelasLesDipilih(jenisS);
			calonSiswa.setPernyataan(pernyataan.isChecked());

			parameterTambahanListener.onSave(calonSiswa);
			Siswa alumni = (Siswa) (siswaAlumni == null ? null : siswaAlumni.getAttribute("siswa"));
			calonSiswa.setSiswaAlumni(alumni);
			if (alumni != null && alumni.getId() != null) {
				Common.copyDataJikaKosong(alumni, calonSiswa, Siswa.class, CalonSiswa.class);
			}

			if (keluarga != null && keluarga.getParent() != null && keluarga.getParent().isVisible()) {
				Keluarga k = (Keluarga) (keluarga.getSelectedItem() == null ? null
						: keluarga.getSelectedItem().getValue());
				calonSiswa.setKeluarga(k);
			}

		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		if (jadwalPertemuanPSBData != null) {

			Criteria criteria = session.createCriteria(CalonSiswa.class)
					.add(Restrictions.eq("jadwalPertemuanPSB.id", jadwalPertemuanPSBData))
					.add(Restrictions.isNotNull("gelombangPendaftaranPsb"));

			if (order)
				criteria.addOrder(Order.desc("id"));

			return criteria;
		} else {

			String s = searchstatus.getSelectedItem().getLabel();

			Criterion criterion;
			if (s.equalsIgnoreCase("Diterima")) {
				criterion = Restrictions.eq("telahDiterima", true);
			} else if (s.equalsIgnoreCase("Terverifikasi")) {
				criterion = Restrictions.eq("terverifikasi", true);
			} else if (s.equalsIgnoreCase("Ditolak")) {
				criterion = Restrictions.eq("ditolak", true);
			} else if (s.equalsIgnoreCase("Mengundurkan Diri")) {
				criterion = Restrictions.eq("mengundurkanDiri", true);
			} else if (s.equalsIgnoreCase("Blm Ditentukan")) {
				criterion = Restrictions.and(Restrictions.eq("telahDiterima", false), Restrictions
						.and(Restrictions.eq("mengundurkanDiri", false), Restrictions.eq("ditolak", false)));
			} else {
				criterion = Restrictions.sqlRestriction("true");
			}

			Criterion criterion2 = Restrictions.sqlRestriction("false");

			if ((mundur != null && mundur.isChecked()) && (ditolak != null && ditolak.isChecked())
					&& (diterima != null && diterima.isChecked()) && (belum != null && belum.isChecked())) {
				criterion2 = Restrictions.sqlRestriction("true");
			} else {

				if (mundur != null && mundur.isChecked()) {
					criterion2 = Restrictions.or(criterion2, Restrictions.eq("mengundurkanDiri", true));
				}
				if (ditolak != null && ditolak.isChecked()) {
					criterion2 = Restrictions.or(criterion2, Restrictions.eq("ditolak", true));
				}
				if (diterima != null && diterima.isChecked()) {
					criterion2 = Restrictions.or(criterion2, Restrictions.eq("telahDiterima", true));
				}
				if (terverifikasi != null && terverifikasi.isChecked()) {
					criterion2 = Restrictions.or(criterion2, Restrictions.eq("terverifikasi", true));
				}
				if (belum != null && belum.isChecked()) {
					criterion2 = Restrictions.or(criterion2,
							Restrictions.and(Restrictions.eq("telahDiterima", false),
									Restrictions.and(Restrictions.eq("terverifikasi", false),
											Restrictions.and(Restrictions.eq("mengundurkanDiri", false),
													Restrictions.eq("ditolak", false)))));
				}
			}
			PenjurusanSekolah penjurusanSekolah = (PenjurusanSekolah) (searchPenjurusan == null
					|| searchPenjurusan.getSelectedItem() == null ? null
							: searchPenjurusan.getSelectedItem().getValue());

			Criteria criteria = session.createCriteria(CalonSiswa.class)
					.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))

					.add(criterion2)

					.add(tampilkanYgBelumBayarDaftarUlang != null && tampilkanYgBelumBayarDaftarUlang.isChecked()
							? Restrictions.eq("riwayatPembayaranDaftarUlang", "")
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgSudahBayarDaftarUlang != null && tampilkanYgSudahBayarDaftarUlang.isChecked()
							? Restrictions.ne("riwayatPembayaranDaftarUlang", "")
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgBelumBayar != null && tampilkanYgBelumBayar.isChecked()
							? Restrictions.eq("riwayatPembayaranDaftarUlang", "")
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgSudahBayar != null && tampilkanYgSudahBayar.isChecked()
							? Restrictions.ne("riwayatPembayaranPendaftaran", "")
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgBelumBayar != null && tampilkanYgBelumBayar.isChecked()
							? Restrictions.eq("riwayatPembayaranPendaftaran", "")
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgSudahdapatNIM != null && tampilkanYgSudahdapatNIM.isChecked()
							? Restrictions.isNotNull("siswa")
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgBelumdapatNIM != null && tampilkanYgBelumdapatNIM.isChecked()
							? Restrictions.isNull("siswa")
							: Restrictions.sqlRestriction("true"))

					.add(penjurusanSekolah == null ? Restrictions.sqlRestriction("true")
							: penjurusanSekolah.getId() == null ? Restrictions.isNull("penjurusanSekolah")
									: penjurusanSekolah.getId().equals(-1L)
											? Restrictions.isNotNull("penjurusanSekolah")
											: Restrictions.eq("penjurusanSekolah", penjurusanSekolah))

					.add(criterion).createAlias("gelombangPendaftaranPsb", "gelombangPendaftaranPsb");

			if (order)
				criteria.addOrder(Order.desc("id"));

			criteria

					.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("namaSiswa", searchnama.getValue().trim(), MatchMode.ANYWHERE))

					.add(searchnamaayah.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("namaAyah", searchnamaayah.getValue().trim(), MatchMode.ANYWHERE))

					.add(searchnamaibu.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("namaIbu", searchnamaibu.getValue().trim(), MatchMode.ANYWHERE))

					.add(searchno.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("nomorInduk", searchno.getValue().trim(), MatchMode.ANYWHERE))

					.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("gelombangPendaftaranPsb.tahunAjaran",
									searchta.getSelectedItem().getValue()))

					.add(searchgel.getSelectedItem() == null || searchgel.getSelectedItem().getValue() == null
							|| searchgel.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("gelombangPendaftaranPsb",
											searchgel.getSelectedItem().getValue()))

					.add(searchSatusAwal.getSelectedItem() == null
							|| searchSatusAwal.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("statusAwalSiswa", searchSatusAwal.getSelectedItem().getValue()))

					.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
							|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

					.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
							|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
			return criteria;
		}
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		if (searchnama == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);

		List<CalonSiswa> calonSiswa = ConstantValues
				.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE).setFirstResult(
								Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
						CalonSiswa.class);
		ListModel strset = new SimpleListModel(calonSiswa);
		grid.setRowRenderer(new CalonSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakKartu(final CalonSiswa calonSiswa, final boolean kirim) throws Exception {

		Session session = HibernateUtil.currentSession();
		session.refresh(calonSiswa);

		if (Common.bolehKonfigurasi("setelah_daftar_psb_langsung_generate_nomor_ujian")) {
			String noUjian = CommonPSB.generateNoUjian(calonSiswa);
			System.out.println("noUjian => " + noUjian);

			RuangGelombangPendaftaranPsbPSB ruangGelombangPendaftaranPsbPSB = (RuangGelombangPendaftaranPsbPSB) session
					.createCriteria(RuangGelombangPendaftaranPsbPSB.class)
					.add(Restrictions.eq("calonSiswa", calonSiswa)).setMaxResults(1).uniqueResult();

			if (ruangGelombangPendaftaranPsbPSB == null && calonSiswa.getNoUjian() != null
					&& !calonSiswa.getNoUjian().trim().isEmpty()) {
				ruangGelombangPendaftaranPsbPSB = CommonPSB.dapatkanRuangUjian(calonSiswa);
			}

			if (ruangGelombangPendaftaranPsbPSB == null) {
				MyMessageboxConfig.show(
						"Kuota / ruangan penerimaan calon siswa telah penuh, harap hubungi petugas penerimaan siswa baru..",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}
		}

		if (Common.bolehKonfigurasi("setelah_daftar_psb_langsung_cetak_kartu")) {

			Map parameters = ais.common.HashMapGenerator.getRand();

			GelombangPendaftaranPsb gel = calonSiswa.getGelombangPendaftaranPsb();
			LampiranLain kop = LampiranLain.ambil(gel.getId(), LampiranLain.KOP_GELOMBANG_PSB);
			if (kop != null) {
				try {
					parameters.put("kop_file", kop.ambilFile().getAbsolutePath());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/CalonSiswaAction.java:5200");
					// TODO: handle exception
				}
			}

			parameters.put("biodata_id", calonSiswa == null ? "" : calonSiswa.getId());
			parameters.put("tahun_akademik", calonSiswa.getGelombangPendaftaranPsb().getTahunAjaran());

			Common.insertProperty(CalonSiswa.class, calonSiswa, parameters, "calon");

			File myfilebarcode = new File(
					Common.ambilREAL_PATH_REPORT() + "/crcode_" + calonSiswa.getNoRegistrasi() + ".png");

			BarcodeCommon.generateCRCode(calonSiswa.getNoRegistrasi() + "\n" + calonSiswa.getNamaSiswa() + "\n"
					+ calonSiswa.getSekolah().getNama() + "\n" + calonSiswa.getGelombangPendaftaranPsb().getNama(),
					myfilebarcode);
			parameters.put("cr_code", myfilebarcode.getAbsolutePath());
			parameters.put("qr_code", Common.desEncrypter.get().encrypt(CalonSiswa.class.getName() + ":" + calonSiswa.getId()));
			String code = parameters.get("qr_code")+"";
			File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + Common.randLong() + ".png");
			BarcodeCommon.generateCRCode(code, myfilebarcode1);
			parameters.put("qr_code_img", myfilebarcode1.getAbsolutePath());

			JenisBiayaSekolah jenisBiayaSekolah = calonSiswa.getTerverifikasi()
					? calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahTerverifikasi()
					: !calonSiswa.getTelahDiterima() ? calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolah()
							: calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahLulus();

			if (jenisBiayaSekolah != null) {

				boolean bri = Common.bolehKonfigurasi("generate_nomor_pembayaran_bri_saat_formulir_siswa_baru", Konfigurasi.TIDAK_AKTIF);
				if (bri) {

					List<Tagihan> tagihans = TagihanUtilCalonSiswa.getTagihan(jenisBiayaSekolah, null, calonSiswa, null,
							null, true);
					System.out.println("tagihans => " + tagihans);
					if (!tagihans.isEmpty()) {
						Double amn = 0.0;
						for (Tagihan tagihan : tagihans) {
							amn += (tagihan.getNominal() + tagihan.getDenda());
						}

						BriRequest briRequest = BriCommon.onSaveBri(null, calonSiswa, tagihans, amn, false, 0.0);
						System.out.println("briRequest => " + briRequest);
						if (briRequest != null) {
							Double biayaAdministrasi = 0.0;
							try {
								biayaAdministrasi = Double
										.parseDouble(Common.getKonfigurasi("bri_biaya_administrasi", "0.0").getNilai());
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							String brivaNo = Common.getKonfigurasi("bri_briva_no", "77777").getNilai();

							String info = "Kode Pembayaran\t\t: " + brivaNo + briRequest.getVa() + "\n";
							info += "Tagihan \t\t\t: " + Common.numberFormat.get().format(amn) + "\n";
							if (biayaAdministrasi > 0.1) {
								info += "Biaya admin \t\t\t: " + Common.numberFormat.get().format(biayaAdministrasi) + "\n";
								info += "Total tagihan \t\t: " + Common.numberFormat.get().format(amn + biayaAdministrasi)
										+ "\n";
							}
							info += "Terbilang \t\t\t: "
									+ IndonesianNumberToWords.convert((long) (amn + biayaAdministrasi)) + "\n";
							if (briRequest.getSiswa() != null) {
								info += "NIS  \t\t\t\t: " + briRequest.getSiswa().getNomorInduk() + "\n";
								info += "Nama \t\t\t\t: " + briRequest.getSiswa().getNama() + "\n";
							} else if (briRequest.getCalonSiswa() != null) {
								info += "No. Reg \t\t\t: " + briRequest.getCalonSiswa().getNoRegistrasi() + "\n";
								if (briRequest.getCalonSiswa().getNoUjian() != null) {
									info += "No. Ujian \t\t\t: " + briRequest.getCalonSiswa().getNoUjian() + "\n";
								}
								info += "Nama \t\t\t\t: " + briRequest.getCalonSiswa().getNama() + "\n";
							}

							if (briRequest.getBill_expired() != null) {
								info += "Wkt. Kadalurasa\t\t: "
										+ Common.dateFormat3.get().format(briRequest.getBill_expired()) + "\n";
							}

							info += "\nTata Cara Pembayaran bisa dilihat di menu pengumuman cara pembayaran di Sistem Penerimaan Peserta Didik Baru (PPDB)\n";

							parameters.put("info_bayar", info);
						}
					}
				}

				boolean bni = Common.bolehKonfigurasi("generate_nomor_pembayaran_bni_saat_formulir_siswa_baru", Konfigurasi.TIDAK_AKTIF);
				if (bni) {

					List<Tagihan> tagihans = TagihanUtilCalonSiswa.getTagihan(jenisBiayaSekolah, null, calonSiswa, null,
							null, true);
					System.out.println("tagihans => " + tagihans);
					if (!tagihans.isEmpty()) {
						Double amn = 0.0;
						for (Tagihan tagihan : tagihans) {
							amn += (tagihan.getNominal() + tagihan.getDenda());
						}

						BniRequest bniRequest = BniCommon.onSaveBni(null, calonSiswa, tagihans, amn, false, 0.0);
						System.out.println("bniRequest => " + bniRequest);
						if (bniRequest != null) {
							Double biayaAdministrasi = 0.0;
							try {
								biayaAdministrasi = Double
										.parseDouble(Common.getKonfigurasi("bni_biaya_administrasi", "0.0").getNilai());
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

							String info = "Kode Pembayaran\t\t: " + bniRequest.getVa() + "\n";
							info += "Tagihan \t\t\t: " + Common.numberFormat.get().format(amn) + "\n";
							if (biayaAdministrasi > 0.1) {
								info += "Biaya admin \t\t\t: " + Common.numberFormat.get().format(biayaAdministrasi) + "\n";
								info += "Total tagihan \t\t: " + Common.numberFormat.get().format(amn + biayaAdministrasi)
										+ "\n";
							}
							info += "Terbilang \t\t\t: "
									+ IndonesianNumberToWords.convert((long) (amn + biayaAdministrasi)) + "\n";
							if (bniRequest.getSiswa() != null) {
								info += "NIS  \t\t\t\t: " + bniRequest.getSiswa().getNomorInduk() + "\n";
								info += "Nama \t\t\t\t: " + bniRequest.getSiswa().getNama() + "\n";
							} else if (bniRequest.getCalonSiswa() != null) {
								info += "No. Reg \t\t\t: " + bniRequest.getCalonSiswa().getNoRegistrasi() + "\n";
								if (bniRequest.getCalonSiswa().getNoUjian() != null) {
									info += "No. Ujian \t\t\t: " + bniRequest.getCalonSiswa().getNoUjian() + "\n";
								}
								info += "Nama \t\t\t\t: " + bniRequest.getCalonSiswa().getNama() + "\n";
							}

							if (bniRequest.getBillExpired() != null) {
								info += "Wkt. Kadalurasa\t\t: " + Common.dateFormat3.get().format(bniRequest.getBillExpired())
										+ "\n";
							}

							info += "\nTata Cara Pembayaran bisa dilihat di menu pengumuman cara pembayaran di Sistem Penerimaan Peserta Didik Baru (PPDB)\n";

							parameters.put("info_bayar", info);
						}
					}
				}

				if (!Common.isMobile()) {
					Report.generatePDFReport(Report.PDF, parameters, "sekolah/KartuBayarPsbMandiri",
							ais.ui.util.WaktuUtil.getDate(), Common.locale);
				}

				File file = Report.generateDownloadReport(Report.PDF, parameters, "sekolah/KartuBayarPsbMandiri", null,
						ais.ui.util.WaktuUtil.getDate());

				if (kirim) {
					CommonEmail.infoDaftarSiswaBanyakFile(calonSiswa, new File[] { file });
				}

			} else {
				if (!Common.isMobile()) {
					Report.generatePDFReport(Report.PDF, parameters, "sekolah/KartuBayarPsbMandiri",
							ais.ui.util.WaktuUtil.getDate(), Common.locale);
				}

				File file = Report.generateDownloadReport(Report.PDF, parameters, "sekolah/KartuBayarPsbMandiri", null,
						ais.ui.util.WaktuUtil.getDate());
				if (kirim) {
					CommonEmail.infoDaftarSiswaBanyakFile(calonSiswa, new File[] { file });
				}
			}

		}
	}

	@SuppressWarnings("unchecked")
	private void checkApakahAdaUjian(CalonSiswa calonSiswa) {

		Session session = HibernateUtil.currentSession();
		RuangGelombangPendaftaranPsbPSB ruangGelombangPendaftaranPsbPSB = (RuangGelombangPendaftaranPsbPSB) (calonSiswa == null
				|| calonSiswa.getId() == null
						? null
						: session.createCriteria(RuangGelombangPendaftaranPsbPSB.class)
								.add(Restrictions.eq("calonSiswa", calonSiswa)).setMaxResults(1)
								.addOrder(Order.desc("id")).uniqueResult());
		if (ruangGelombangPendaftaranPsbPSB != null) {
			List<JadwalUjianPSB> jadwalUjianPSBs = session.createCriteria(JadwalUjianPSB.class)
					.add(Restrictions.eq("ujianPSB", ruangGelombangPendaftaranPsbPSB.getRuangPSB().getUjianPSB()))
					.add(Restrictions.or(
							Restrictions.eq("gelombangPendaftaranPsb", calonSiswa.getGelombangPendaftaranPsb()),
							Restrictions.isNull("gelombangPendaftaranPsb")))
					.list();

			if (!jadwalUjianPSBs.isEmpty()) {
				List<Pertemuan> pertemuans = session.createCriteria(PertemuanPunyaUjian.class)
						.setProjection(Projections.groupProperty("pertemuan")).createAlias("pertemuan", "pertemuan")

						.add(Restrictions.in("pertemuan.jadwalUjianPSB", jadwalUjianPSBs)).list();
				ujian.setVisible(!pertemuans.isEmpty());
			} else {
				ujian.setVisible(false);
			}

		} else {
			ujian.setVisible(false);
		}
	}

	public static boolean checkInfoDariMana(final Box infoKampusDariMana, final Textbox namaTemanInfoKampusDariMana,
			final Textbox keteranganInfoKampusDariMana) throws Exception {
		String info = BiodataCalonMahasiswaAction.infoMahasiswaBaru(infoKampusDariMana);
		if (Common.bolehKonfigurasi("tampilkan_info_sekolah_dari_mana_pada_ppdb", Konfigurasi.TIDAK_AKTIF)) {

			if (info.trim().isEmpty()) {
				MyMessageboxConfig.show(
						"\"Anda mendapatkan informasi informasi penerimaan peserta didik baru dari mana ?\" harus diisi",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								infoKampusDariMana.focus();
								Clients.scrollIntoView(infoKampusDariMana);
							}
						});
				return false;
			} else if ((info.toLowerCase().contains(";teman;") || info.toLowerCase().contains(";kawan"))
					&& namaTemanInfoKampusDariMana != null && namaTemanInfoKampusDariMana.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Nama teman harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								namaTemanInfoKampusDariMana.focus();
								namaTemanInfoKampusDariMana.select();
								Clients.scrollIntoView(namaTemanInfoKampusDariMana);
							}
						});
				return false;
			} else if ((info.toLowerCase().contains(";lain-lain;") || info.toLowerCase().contains(";lain;")
					|| info.toLowerCase().contains(";dosen;") || info.toLowerCase().contains(";lainnya"))
					&& keteranganInfoKampusDariMana != null
					&& keteranganInfoKampusDariMana.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Sebutkan dari mana harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								keteranganInfoKampusDariMana.focus();
								keteranganInfoKampusDariMana.select();
								Clients.scrollIntoView(keteranganInfoKampusDariMana);
							}
						});
				return false;
			}
		}

		return true;
	}

	@SuppressWarnings("deprecation")
	public static Component[] infoDariMana(Rows rows, CalonSiswa calonSiswa) throws Exception {
		Textbox namaTemanInfoKampusDariMana = new Textbox(calonSiswa.getNamaTemanInfoKampusDariMana());
		Textbox keteranganInfoKampusDariMana = new Textbox(calonSiswa.getKeteranganInfoKampusDariMana());
		Box infoKampusDariMana = Common.isMobile() ? new Vbox() : new Hbox();

		if (Common.bolehKonfigurasi("tampilkan_info_sekolah_dari_mana_pada_ppdb")) {

			MyRowStyled row = new MyRowStyled();

			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.appendChild(new ais.ui.util.MyHtml("<hr>"));

			String infoDariManaPmb = Common.getKonfigurasi("info_dari_mana_ppdb", "Website,Teman,Radio,Koran,Lain-lain")
					.getNilai();

			row = new MyRowStyled();
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.setParent(rows);
			row.appendChild(new MyLabelConfigTitikDua(
					"Anda mendapatkan informasi penerimaan peserta didik baru ini dari mana ? *"));

			boolean pilihSalahSatuInfoPmbDariMana = Common.bolehKonfigurasi("pilih_salah_satu_info_ppdb_dari_mana", Konfigurasi.TIDAK_AKTIF);

			row = new MyRowStyled();
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.setParent(rows);

			if (pilihSalahSatuInfoPmbDariMana) {
				Radiogroup radiogroup = new Radiogroup();
				radiogroup.appendChild(infoKampusDariMana);
				row.appendChild(radiogroup);
			} else {
				row.appendChild(infoKampusDariMana);
			}

			final Checkbox radioTeman = pilihSalahSatuInfoPmbDariMana ? new Radio() : new Checkbox();
			final Checkbox radioMahasiswa = pilihSalahSatuInfoPmbDariMana ? new Radio() : new Checkbox();
			final Checkbox radioLainlain = pilihSalahSatuInfoPmbDariMana ? new Radio() : new Checkbox();
			final Checkbox radioDosen = pilihSalahSatuInfoPmbDariMana ? new Radio() : new Checkbox();

			for (String r : StringUtils.split(infoDariManaPmb, ",")) {
				r = r.trim().toLowerCase();
				if (!r.isEmpty()) {
					if (r.equalsIgnoreCase("Teman".toLowerCase()) || r.equalsIgnoreCase("Kawan".toLowerCase())) {
						radioTeman.setLabel(r);
						radioTeman.setValue(r);
						infoKampusDariMana.appendChild(radioTeman);
						if (calonSiswa.getInfoKampusDariMana().contains(";Teman;".toLowerCase())
								|| calonSiswa.getInfoKampusDariMana().contains(";Kawan;".toLowerCase())) {
							radioTeman.setChecked(true);
						}
					} else if (r.equalsIgnoreCase("Siswa".toLowerCase()) || r.equalsIgnoreCase("Siswa".toLowerCase())) {
						radioMahasiswa.setLabel(r);
						radioMahasiswa.setValue(r);
						infoKampusDariMana.appendChild(radioMahasiswa);
						if (calonSiswa.getInfoKampusDariMana().contains(";Siswa;".toLowerCase())
								|| calonSiswa.getInfoKampusDariMana().contains(";Siswa;".toLowerCase())) {
							radioMahasiswa.setChecked(true);
						}
					} else if (r.equalsIgnoreCase("Lain-lain".toLowerCase()) || r.equalsIgnoreCase("Lain".toLowerCase())
							|| r.equalsIgnoreCase("Lainnya".toLowerCase())) {

						radioLainlain.setLabel(r);
						radioLainlain.setValue(r);
						infoKampusDariMana.appendChild(radioLainlain);
						if (calonSiswa.getInfoKampusDariMana().contains(";Lain-lain;".toLowerCase())
								|| calonSiswa.getInfoKampusDariMana().contains(";lain;".toLowerCase())
								|| calonSiswa.getInfoKampusDariMana().contains(";lain-lain;".toLowerCase())
								|| calonSiswa.getInfoKampusDariMana().contains(";Lainnya;".toLowerCase())) {
							radioLainlain.setChecked(true);
						}
					} else if (r.equalsIgnoreCase("Dosen".toLowerCase())) {

						radioDosen.setLabel(r);
						radioDosen.setValue(r);
						infoKampusDariMana.appendChild(radioDosen);
						if (calonSiswa.getInfoKampusDariMana().contains(";Dosen;".toLowerCase())) {
							radioDosen.setChecked(true);
						}
					} else {
						Checkbox check = pilihSalahSatuInfoPmbDariMana ? new Radio() : new Checkbox();
						check.setLabel(r);
						check.setValue(r);
						infoKampusDariMana.appendChild(check);
						if (calonSiswa.getInfoKampusDariMana().contains(";" + r + ";")) {
							check.setChecked(true);
						}
					}
				}
			}

			final MyRowStyled rownamaTemanInfoKampusDariMana = new MyRowStyled();
			rownamaTemanInfoKampusDariMana.setParent(rows);
			rownamaTemanInfoKampusDariMana.appendChild(new MyLabelConfigTitikDua("Sebutkan Nama Teman/Siswa"));

			rownamaTemanInfoKampusDariMana.appendChild(namaTemanInfoKampusDariMana);
			rownamaTemanInfoKampusDariMana.setWidth("90%");

			final MyRowStyled rowketeranganInfoKampusDariMana = new MyRowStyled();
			rowketeranganInfoKampusDariMana.setParent(rows);
			rowketeranganInfoKampusDariMana.appendChild(new MyLabelConfigTitikDua("Sebutkan dari mana"));

			rowketeranganInfoKampusDariMana.appendChild(keteranganInfoKampusDariMana);
			rowketeranganInfoKampusDariMana.setWidth("90%");

			EventListener keteranganEventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					try {
						rowketeranganInfoKampusDariMana.setVisible(radioLainlain.isChecked() || radioDosen.isChecked());
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

					try {
						rownamaTemanInfoKampusDariMana.setVisible(radioTeman.isChecked() || radioMahasiswa.isChecked());
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				}
			};
			keteranganEventListener.onEvent(null);
			infoKampusDariMana.addEventListener("onClick", keteranganEventListener);

		}

		return new Component[] { infoKampusDariMana, namaTemanInfoKampusDariMana, keteranganInfoKampusDariMana };

	}

	@SuppressWarnings("deprecation")
	public static Component[] infoPindahan(Rows rows, final CalonSiswa calonSiswa) throws Exception {

		final MyRowStyled row = new MyRowStyled();

		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new ais.ui.util.MyHtml("<hr>"));

		final MyRowStyled rowPindahan2 = new MyRowStyled();
		rowPindahan2.setParent(rows);
		rowPindahan2.appendChild(new MyLabelConfigTitikDua("Merupakan Siswa Pindahan"));
		final MyCheckboxConfig merupakanPindahan;
		rowPindahan2.appendChild(merupakanPindahan = new MyCheckboxConfig("Ya, Saya siswa pindahan"));
		merupakanPindahan.setChecked(calonSiswa.getMerupakanPindahan());

		final MyRowStyled rowpindahanDari = new MyRowStyled();
		rowpindahanDari.setParent(rows);
		rowpindahanDari.appendChild(new ais.ui.util.MyLabelConfigTitikDua("Nama Sekolah Sebelumnya *"));
		Textbox pindahanDariSekolah;
		rowpindahanDari.appendChild(pindahanDariSekolah = new Textbox(calonSiswa.getPindahanDariSekolah()));
		pindahanDariSekolah.setWidth("90%");

		final MyRowStyled rowpindahanDariKelas = new MyRowStyled();
		rowpindahanDariKelas.setParent(rows);
		rowpindahanDariKelas.appendChild(new ais.ui.util.MyLabelConfigTitikDua("Kelas Terakhir Sekolah Sebelumnya *"));
		Textbox kelasSekolahPindahan;
		rowpindahanDariKelas.appendChild(kelasSekolahPindahan = new Textbox(calonSiswa.getKelasSekolahPindahan()));
		kelasSekolahPindahan.setWidth("90%");

		final MyRowStyled rowpindahanDariAlamat = new MyRowStyled();
		rowpindahanDariAlamat.setParent(rows);
		rowpindahanDariAlamat.appendChild(new ais.ui.util.MyLabelConfigTitikDua("Alamat Sekolah Sebelumnya *"));
		Textbox alamatSekolahPindahan;
		rowpindahanDariAlamat.appendChild(alamatSekolahPindahan = new Textbox(calonSiswa.getAlamatSekolahPindahan()));
		alamatSekolahPindahan.setWidth("90%");
		alamatSekolahPindahan.setRows(2);

		final MyRowStyled rowketeranganPindah = new MyRowStyled();
		rowketeranganPindah.setParent(rows);
		rowketeranganPindah.appendChild(new MyLabelConfigTitikDua("Keterangan / Alasan Pindah"));
		Textbox keteranganPindah;
		rowketeranganPindah.appendChild(keteranganPindah = new Textbox(calonSiswa.getKeteranganPindah()));
		keteranganPindah.setWidth("90%");
		keteranganPindah.setRows(3);

		final MyRowStyled rowTanggalPindah = new MyRowStyled();
		rowTanggalPindah.setParent(rows);
		rowTanggalPindah.appendChild(new MyLabelConfigTitikDua("Tanggal Pindah *"));
		final MyDatebox tanggalPindah;
		rowTanggalPindah.appendChild(tanggalPindah = new MyDatebox(calonSiswa.getTanggalPindah()));
		tanggalPindah.setReadonly(true);
		EventListener pindahanEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				row.setVisible(calonSiswa.getGelombangPendaftaranPsb() != null
						&& calonSiswa.getGelombangPendaftaranPsb().getSiswaPindahanBolehMendaftar());
				rowPindahan2.setVisible(calonSiswa.getGelombangPendaftaranPsb() != null
						&& calonSiswa.getGelombangPendaftaranPsb().getSiswaPindahanBolehMendaftar());

				rowpindahanDari.setVisible(merupakanPindahan.isChecked()
						&& calonSiswa.getGelombangPendaftaranPsb() != null
						&& calonSiswa.getGelombangPendaftaranPsb().getSiswaPindahanBolehMendaftar()
						&& Common.bolehKonfigurasi("tampil_nama_sekolah_calon_siswa_pindah_dari"));
				rowpindahanDariAlamat.setVisible(merupakanPindahan.isChecked()
						&& calonSiswa.getGelombangPendaftaranPsb() != null
						&& calonSiswa.getGelombangPendaftaranPsb().getSiswaPindahanBolehMendaftar()
						&& Common.bolehKonfigurasi("tampil_alamat_sekolah_calon_siswa_pindah_dari"));
				rowTanggalPindah.setVisible(merupakanPindahan.isChecked()
						&& calonSiswa.getGelombangPendaftaranPsb() != null
						&& calonSiswa.getGelombangPendaftaranPsb().getSiswaPindahanBolehMendaftar()
						&& Common.bolehKonfigurasi("tampil_tanggal_pindah_calon_siswa_pindah_dari"));
				rowketeranganPindah
						.setVisible(merupakanPindahan.isChecked() && calonSiswa.getGelombangPendaftaranPsb() != null
								&& calonSiswa.getGelombangPendaftaranPsb().getSiswaPindahanBolehMendaftar()
								&& Common.bolehKonfigurasi("tampil_keterangan_calon_siswa_pindah_dari"));
				rowpindahanDariKelas
						.setVisible(merupakanPindahan.isChecked() && calonSiswa.getGelombangPendaftaranPsb() != null
								&& calonSiswa.getGelombangPendaftaranPsb().getSiswaPindahanBolehMendaftar()
								&& Common.bolehKonfigurasi("tampil_kelas_sebelum_pindah_calon_siswa_pindah_dari"));
			}
		};

		merupakanPindahan.addEventListener("onClick", pindahanEventListener);
		pindahanEventListener.onEvent(null);

		return new Component[] { merupakanPindahan, pindahanDariSekolah, alamatSekolahPindahan, keteranganPindah,
				tanggalPindah, kelasSekolahPindahan };

	}

	private void displayVerifikasi(CalonSiswa calonSiswa, List<VerifikasiKelengkapanCalonSiswa> data,
			final EventListener eventListener) throws Exception {
		final MyWindow window = new MyWindow("Verifikasi Berkas", "none", false);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("90%");
		window.setWidth("900px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);

		Rows rows = new Rows();
		rows.setParent(grid);

		VerifikasiPSBHelper.tampilkanVerifikasi(calonSiswa, rows, null, calonSiswa.getGelombangPendaftaranPsb(), data);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
				Common.createDefaultTimer(eventListener);
			}
		});
		cancel.setParent(toolbar);

		window.onModal();
	}

	// ════════════════════════════════════════════════════════════════════
	// Dasbor Siswa — helper + event handlers (lazy-load per tab klik)
	// ════════════════════════════════════════════════════════════════════

	/** Tahun masuk dari combo filter utama halaman ini. */
	private int getCurrentTahunMasuk() {
		if (searchta != null && searchta.getSelectedItem() != null) {
			String label = searchta.getSelectedItem().getLabel();
			if (label != null && !label.trim().isEmpty()) {
				String clean = label.trim();
				int slash = clean.indexOf('/');
				if (slash > 0) { clean = clean.substring(0, slash); }
				try { return Integer.parseInt(clean.trim()); }
				catch (NumberFormatException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/CalonSiswaAction.java:5735");}
			}
		}
		return Calendar.getInstance().get(Calendar.YEAR);
	}

	/** Dasbor Utama: statistik keseluruhan penerimaan calon siswa. */
	public void onDasborStatistik() throws Exception {
		if (dasborStatistik == null || !dasborStatistik.getChildren().isEmpty()) { return; }
		ais.database.model.PerguruanTinggi pt = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
		dashboardStatistik = new DashboardStatistikSiswa(pt);
		dashboardStatistik.build(dasborStatistik, String.valueOf(getCurrentTahunMasuk()), null);
	}

	/** Dasbor Status Seleksi: distribusi diterima/ditolak/mundur per jurusan. */
	public void onDasborStatus() throws Exception {
		if (dasborStatus == null || !dasborStatus.getChildren().isEmpty()) { return; }
		ais.database.model.PerguruanTinggi pt = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
		dashboardStatus = new DashboardStatusSiswa(pt);
		dashboardStatus.build(dasborStatus, String.valueOf(getCurrentTahunMasuk()), null);
	}

	/** Dasbor Asal Sekolah: top sekolah pengirim siswa + sebaran propinsi. */
	public void onDasborSekolah() throws Exception {
		if (dasborSekolah == null || !dasborSekolah.getChildren().isEmpty()) { return; }
		ais.database.model.PerguruanTinggi pt = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
		dashboardSekolah = new DashboardAsalSekolahSiswa(pt);
		dashboardSekolah.build(dasborSekolah, String.valueOf(getCurrentTahunMasuk()), null);
	}

	/** Dasbor Registrasi: status pembayaran pendaftaran dan daftar ulang. */
	public void onDasborRegistrasi() throws Exception {
		if (dasborRegistrasi == null || !dasborRegistrasi.getChildren().isEmpty()) { return; }
		ais.database.model.PerguruanTinggi pt = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
		dashboardRegistrasi = new DashboardRegistrasiSiswa(pt);
		dashboardRegistrasi.build(dasborRegistrasi, String.valueOf(getCurrentTahunMasuk()), null);
	}

	/** Dasbor Harian: tren pendaftaran per hari dan perbandingan mingguan. */
	public void onDasborHarian() throws Exception {
		if (dasborHarian == null || !dasborHarian.getChildren().isEmpty()) { return; }
		ais.database.model.PerguruanTinggi pt = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
		dashboardHarian = new DashboardHarianSiswa(pt);
		dashboardHarian.build(dasborHarian, String.valueOf(getCurrentTahunMasuk()), null);
	}

	/** Rekap Multi-Tahun: tabel jalur masuk 4 tahun per gelombang. */
	public void onDasborRekapMultiTahun() throws Exception {
		if (dasborRekapMultiTahun == null || !dasborRekapMultiTahun.getChildren().isEmpty()) { return; }
		ais.database.model.PerguruanTinggi pt = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
		dashboardRekapMultiTahun = new RekapJalurMasukMultiTahunPsb(pt);
		dashboardRekapMultiTahun.build(dasborRekapMultiTahun, String.valueOf(getCurrentTahunMasuk()), null);
	}
}
