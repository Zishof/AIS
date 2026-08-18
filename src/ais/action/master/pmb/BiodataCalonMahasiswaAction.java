package ais.action.master.pmb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.joda.time.Years;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.maintenance.PMBAction;
import ais.action.master.DiskonCalonMahasiswaAction;
import ais.action.master.KonfigurasiTampilanBiodataCalonMahasiswaAction;
import ais.action.master.KonfigurasiTampilanLoginCalonMahasiswaAction;
import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.AmbilDataAfiliasiCalonMahasiswaBanbox;
import ais.action.master.helper.AmbilDataKecamatanBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.AmbilDataNamaSekolahBanbox;
import ais.action.master.helper.AmbilDataPerguruanTinggiLainBanbox;
import ais.action.master.helper.HasilUjianHelper;
import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.helper.virtualaccount.DownloadNoRegistrasiCalonMahasiswaBankNtt;
import ais.action.master.helper.virtualaccount.DownloadNoRegistrasiCalonMahasiswaBankOnline;
import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.action.ws.util.PembayaranUtil;
import ais.common.BarcodeCommon;
import ais.common.BniCommon;
import ais.common.Common;
import ais.common.CommonEmail;
import ais.common.CommonPMB;
import ais.common.ConstantValues;
import ais.common.EnglishNumberToWords;
import ais.common.IndonesianNumberToWords;
import ais.common.PmbArkatama;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.AfiliasiCalonMahasiswa;
import ais.database.model.Agama;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.GeneralValueObject;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKartuIdentitasMahasiswaBaru;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisSekolahMahasiswaBaru;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jurusan;
import ais.database.model.JurusanSekolahMahasiswaBaru;
import ais.database.model.Kegiatan;
import ais.database.model.KelompokJenisSeleksi;
import ais.database.model.Konfigurasi;
import ais.database.model.Konsentrasi;
import ais.database.model.Kota;
import ais.database.model.Mahasiswa;
import ais.database.model.NamaSekolahAsal;
import ais.database.model.Negara;
import ais.database.model.Paket;
import ais.database.model.PaketJurusanPmb;
import ais.database.model.PaketPunyaGelombangPendaftaran;
import ais.database.model.PaketPunyaProgram;
import ais.database.model.Pegawai;
import ais.database.model.PekerjaanOrangTua;
import ais.database.model.PendapatanOrangTua;
import ais.database.model.PendidikanOrangTua;
import ais.database.model.PerguruanTinggi;
import ais.database.model.PerguruanTinggiLain;
import ais.database.model.Perkuliahan;
import ais.database.model.PersyaratanPilihanPaket;
import ais.database.model.PilihanPaketPerJurusanMhsBaru;
import ais.database.model.Program;
import ais.database.model.Propinsi;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.VirtualAccountBank;
import ais.database.model.Wilayah;
import ais.database.model.bni.BniRequest;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoBiodataCalonMahasiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.LampiranLainBiodataCalonMahasiswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import org.zkoss.zul.Grid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfigTitikDua;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyRowStyled;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class BiodataCalonMahasiswaAction extends MyWindow {

	@SuppressWarnings("unchecked")
	public static String infoMahasiswaBaru(Box infoKampusDariMana) {
		String info = "";

		List<Component> c = infoKampusDariMana == null ? null : infoKampusDariMana.getChildren();
		if (c != null) {
			for (Component ccc : c) {
				try {
					Checkbox cc = (Checkbox) ccc;
					if (cc.isChecked()) {
						info += ";" + cc.getLabel().toLowerCase() + ";";
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:169");
					// TODO: handle exception
				}
			}
		}

		return info;
	}

	public final static String[] DEFAULT_TIDAK_AKTIF = new String[] { "kk", "gelar", "afiliasiCalonMahasiswa" };

	public final static String[] DEFAULT_TIDAK_WAJIB = new String[] { "asalSma", "alamatAsalSma", "kodePosSekolah",
			"jurusanSekolahLain", "kecamatanSekolah", "namaAyah", "namaWali", "alamatOrtu", "rtOrtu", "rwOrtu",
			"kodePosOrtu", "kelurahanOrtu", "kecamatanOrtu", "noTelpOrtu", "pendidikanOrtu", "pekerjaanAyah",
			"pendapatanOrtu",

			"pendidikanOrtuIbu", "pekerjaanAyahIbu", "pendapatanOrtuIbu",

			"pendidikanOrtuWali", "pekerjaanAyahWali", "pendapatanOrtuWali", "nisn", "afiliasiCalonMahasiswa" };

	public final static String[] DATA = new String[] { "gelar", "nama", "jenisKartuIdentitas", "noIdentitas", "kk",
			"tempatLahir", "tanggalLahir", "jenisKelamin", "statusNikah", "agama", "kewarganegaraan", "asalNegara",

			"alamat", "dusunCalon", "rt", "rw", "kodePos", "kelurahanCalon", "kecamatanCalon", "propinsiCalon",
			"kotaCalon", "teleponRumah", "jenisSekolah", "jurusanSekolah", "jurusanSekolahLain", "akreditasiSekolah",
			"asalSma", "alamatAsalSma", "kodePosSekolah", "kecamatanSekolah", "propinsiSekolah", "kotaSekolah",
			"noTelpSekolah", "tahunKelulusan", "namaAyah", "namaIbu", "namaWali", "alamatOrtu", "rtOrtu", "rwOrtu",
			"kodePosOrtu", "kelurahanOrtu", "kecamatanOrtu", "propinsiOrtu", "kotaOrtu", "noTelpOrtu",

			"pendidikanOrtu", "pekerjaanAyah", "pendapatanOrtu",

			"pendidikanOrtuIbu", "pekerjaanAyahIbu", "pendapatanOrtuIbu",

			"pendidikanOrtuWali", "pekerjaanAyahWali", "pendapatanOrtuWali", "nisn", "afiliasiCalonMahasiswa" };

	public final static String[] DATA_DESC = new String[] { "Gelar", "Nama Lengkap", "Jenis Kartu Identitas",
			"No Kartu Identitas", "No. Kartu Keluarga", "Tempat Lahir", "Tanggal Lahir", "Jenis Kelamin",
			"Status Perkawinan", "Agama", "Kewarganegaraan", "Asal Negara",

			"Alamat / Jalan/ Gang Rumah", "Dusun / Kampung", "RT", "RW", "Kode Pos", "Kelurahan / Desa", "Kecamatan",
			"Propinsi", "Kota/Kabupaten", "Telepon (atau HP) / No. WA", "Jenis Pendidikan Asal",
			"Jurusan Pendidikan Asal", "Deskripsi Jurusan Pendidikan Asal", "Akreditasi Pendidikan Asal",
			"Nama Pendidikan Asal", "Alamat Pendidikan Asal", "Kode Pos Pendidikan Asal", "Kecamatan Pendidikan Asal",
			"Propinsi Pendidikan Asal", "Kota/Kabupaten Pendidikan Asal", "No. Telpon Pendidikan Asal",
			"Tahun Kelulusan", "Nama Ayah", "Nama Ibu", "Nama Wali", "Alamat Orang Tua/Wali", "RT Orang Tua/Wali",
			"RW Orang Tua/Wali", "Kode Pos Orang Tua/Wali", "Desa/Kelurahan Orang Tua/Wali", "Kecamatan Orang Tua/Wali",
			"Propinsi Orang Tua/Wali", "Kabupaten/Kota Orang Tua/Wali", "Telepon (atau HP) / No. WA Orang Tua/Wali",
			"Pendidikan Ayah", "Pekerjaan Ayah", "Pendapatan Ayah", "Pendidikan Ibu", "Pekerjaan Ibu", "Pendapatan Ibu",
			"Pendidikan Wali", "Pekerjaan Wali", "Pendapatan Wali", "Nomor Induk Siswa Nasional (NISN)", "Afiliasi" };

	/**
	 * 
	 */
	private static final long serialVersionUID = 1155733365712985677L;


	private static String pesanKonflikDariException(org.hibernate.exception.ConstraintViolationException cve) {
		String gabung = "";
		if (cve.getMessage() != null) gabung += cve.getMessage().toLowerCase();
		if (cve.getCause() != null && cve.getCause().getMessage() != null)
			gabung += " " + cve.getCause().getMessage().toLowerCase();
		if (gabung.contains("no_registrasi") || gabung.contains("noregistrasi"))
			return "Nomor registrasi yang digenerate sistem telah terpakai oleh data lain. "
					+ "Mohon muat ulang halaman, kemudian coba simpan kembali. "
					+ "Apabila kendala tetap terjadi, silakan hubungi petugas administrasi penerimaan mahasiswa baru.";
		if (gabung.contains("nik"))
			return "Nomor Induk Kependudukan (NIK) yang Anda masukkan telah terdaftar dalam sistem. "
					+ "Kemungkinan Anda pernah mendaftar sebelumnya atau terdapat kesalahan pengetikan. "
					+ "Mohon periksa kembali NIK Anda. Apabila merasa belum pernah mendaftar, "
					+ "silakan hubungi petugas administrasi untuk pengecekan lebih lanjut.";
		if (gabung.contains("email"))
			return "Alamat surel (email) yang Anda masukkan telah terdaftar dalam sistem. "
					+ "Mohon gunakan alamat surel lain, atau hubungi petugas administrasi "
					+ "apabila Anda merasa belum pernah melakukan pendaftaran sebelumnya.";
		if (gabung.contains("no_hp") || gabung.contains("nohp") || gabung.contains("telepon") || gabung.contains("phone"))
			return "Nomor telepon/HP yang Anda masukkan telah terdaftar dalam sistem. "
					+ "Mohon periksa kembali nomor telepon Anda. Apabila terdapat kekeliruan, "
					+ "silakan hubungi petugas administrasi penerimaan mahasiswa baru.";
		if (gabung.contains("no_ujian") || gabung.contains("noujian"))
			return "Nomor ujian yang digenerate sistem telah terpakai. "
					+ "Mohon muat ulang halaman dan coba simpan kembali. "
					+ "Apabila kendala tetap berlanjut, silakan hubungi petugas administrasi.";
		return "Data tidak dapat disimpan karena terdapat data yang sama atau bertabrakan dengan data yang telah ada di sistem. "
				+ "Mohon periksa kembali isian formulir Anda, terutama pada kolom NIK, surel, nomor telepon, "
				+ "dan nomor registrasi. Apabila kendala tetap terjadi, silakan hubungi petugas administrasi "
				+ "penerimaan mahasiswa baru untuk mendapatkan bantuan.";
	}

	private static void closeOpenedNativeSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen()) {
				session.clear();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:265");
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:269");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:275");
		}
		try {
			HibernateUtil.closeSession();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:279");
		}
	}


	private Textbox noRegistrasi;
	private Textbox nama;
	private Textbox tempatLahir;
	private MyDatebox tanggalLahir;
	private Combobox jenisKelamin;
	private Combobox kewarganegaraan;
	private Combobox asalNegara;
	private Combobox agama;
	private Combobox statusNikah;

	private Textbox alamat;
	private Textbox rt;
	private Textbox rw;
	private Textbox kelurahanCalon;
	private AmbilDataKecamatanBanbox kecamatanCalon;
	private Label propinsiCalon;
	private Label kotaCalon;
	private Textbox kodePos;
	private Textbox teleponRumah;
	private MyCheckboxConfig alamatSama;

	private Combobox jenisKartuIdentitas;
	private Textbox noIdentitas;
	private Boolean emailWajibDiisi = false;
	private Textbox email;

	private Textbox namaAyah;
	private Combobox pekerjaanAyah;
	private Textbox namaIbu;
	private Textbox namaWali;
	private Textbox noTelpOrtu;
	private Combobox pendapatanOrtu;
	private Combobox pendidikanOrtu;

	private Combobox pendidikanOrtuIbu;
	private Combobox pekerjaanAyahIbu;
	private Combobox pendapatanOrtuIbu;

	private Combobox pendidikanOrtuWali;
	private Combobox pekerjaanAyahWali;
	private Combobox pendapatanOrtuWali;

	private Textbox alamatOrtu;
	private Textbox rtOrtu;
	private Textbox rwOrtu;
	private Textbox kodePosOrtu;
	private AmbilDataKecamatanBanbox kecamatanOrtu;
	private Textbox kelurahanOrtu;
	private Label propinsiOrtu;
	private Label kotaOrtu;

	private AmbilDataNamaSekolahBanbox asalSma;
	private Textbox alamatAsalSma;
	private Combobox jenisSekolah;
	private Radiogroup akreditasiSekolah;
	private Box infoKampusDariMana;
	private Textbox keteranganInfoKampusDariMana;
	private Textbox namaTemanInfoKampusDariMana;
	private Textbox kodePosSekolah;
	private Textbox noTelpSekolah;
	private AmbilDataKecamatanBanbox kecamatanSekolah;
	private Label propinsiSekolah;
	private Label kotaSekolah;
	private Combobox tahunKelulusan;
	private Combobox jurusanSekolah;
	private Textbox jurusanSekolahLain;
	// private Combobox jenjang;
	// private Combobox jenisSeleksi;
	private Combobox paket;
	private Combobox prodi1;
	private Combobox prodi2;
	private Combobox prodi3;
	private Combobox prodi4;
	private Combobox prodi5;

	private Combobox program;

	private BiodataCalonMahasiswa biodataCalonMahasiswa;

	private Combobox tahunAkademik;

	private Combobox gelombangPendaftaran;

	private MyDatebox tanggalPendaftaran;

	private EventListener calonMahasiswaListener;

	private String tahunAkademikPenerimaanMahasiswaBaru;

	private MyCheckboxConfig merupakanPindahan;
	private AmbilDataPerguruanTinggiLainBanbox pindahanDari;
	private Textbox pindahanDariKampus;
	private Textbox pindahanDariProdi;
	private Textbox nimPindahan;
	private Intbox pindahDariKampusLamaDiSemester;
	private Textbox keteranganPindah;
	private Map<String, LampiranLain> lampiranLains = new HashMap<String, LampiranLain>();

	private PerguruanTinggi selectedPerguruanTinggi;

	private GelombangPendaftaran mygelombangPendaftaran = null;
	private JenisSeleksi myjenisSeleksi = null;

	private AfiliasiCalonMahasiswa afiliasiCalonMahasiswaData = null;

	private Paket myPaket = null;

	private Mahasiswa mahasiswaAlumni = null;

	public static void initBg(Center center, GelombangPendaftaran gelombangPendaftaran) {

		center.setStyle("background:url('" + Common.ROOT
				+ "/img/bg_default.jpg') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");

		try {
			LampiranLain kop = null;
			if ((gelombangPendaftaran != null && gelombangPendaftaran.getId() != null)) {
				kop = LampiranLain.ambil(gelombangPendaftaran.getId(), LampiranLain.BG_PMB_GELOMBANG);
			}

			if (kop == null) {
				PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
				if ((perguruanTinggi != null && perguruanTinggi.getId() != null)) {
					kop = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.BG_PT);
				}
			}

			if (kop != null) {
				center.setStyle("background:url('" + kop.createLinkUri(true, true)
						+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:416");

		}

	}

	@SuppressWarnings("unchecked")
	public BiodataCalonMahasiswaAction(GelombangPendaftaran gelombangPendaftaran, JenisSeleksi jenisSeleksi,
			AfiliasiCalonMahasiswa afiliasiCalonMahasiswaData, Mahasiswa mahasiswaAlumni, EventListener eventListener)
			throws Exception {

		super();
		mygelombangPendaftaran = gelombangPendaftaran;
		myjenisSeleksi = jenisSeleksi;
		this.mahasiswaAlumni = mahasiswaAlumni;

		if (mygelombangPendaftaran != null && mygelombangPendaftaran.getId() != null) {
			Session session = HibernateUtil.currentSession();
			List<PaketPunyaGelombangPendaftaran> paketPunyaGelombangPendaftarans = ConstantValues.simpleList(
					session.createCriteria(PaketPunyaGelombangPendaftaran.class).add(Restrictions.isNotNull("paket"))
							.add(Restrictions.eq("gelombangPendaftaran", mygelombangPendaftaran)),
					PaketPunyaGelombangPendaftaran.class);
			if (paketPunyaGelombangPendaftarans.size() == 1) {
				myPaket = paketPunyaGelombangPendaftarans.get(0).getPaket();
			} else {
				myPaket = null;
			}
		}

		this.afiliasiCalonMahasiswaData = afiliasiCalonMahasiswaData;
		this.eventListener = eventListener;
		try {
			init(new BiodataCalonMahasiswa());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

	}

	public BiodataCalonMahasiswaAction() throws Exception {

		super();
		this.mahasiswaAlumni = null;
		try {
			init(new BiodataCalonMahasiswa());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

	}

	private EventListener eventListener;

	public BiodataCalonMahasiswaAction(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		this.mahasiswaAlumni = null;
		try {
			init(new BiodataCalonMahasiswa());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

	}

	public BiodataCalonMahasiswaAction(String title, String border, boolean closable, EventListener eventListener)
			throws Exception {
		super(title, border, closable);
		this.mahasiswaAlumni = null;
		this.eventListener = eventListener;
		try {
			init(new BiodataCalonMahasiswa());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

	}

	public BiodataCalonMahasiswaAction(String title, String border, boolean closable, EventListener eventListener,
			BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		super(title, border, closable);
		this.eventListener = eventListener;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		this.mahasiswaAlumni = null;
		try {
			init(biodataCalonMahasiswa);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

	}

	public void loadBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		Session session = HibernateUtil.currentSession();
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;

		biodataCalonMahasiswa = biodataCalonMahasiswa == null || biodataCalonMahasiswa.getId() == null ? null
				: (BiodataCalonMahasiswa) session.createCriteria(BiodataCalonMahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.idEq(biodataCalonMahasiswa.getId())).setMaxResults(1).uniqueResult();

		if (biodataCalonMahasiswa == null)
			init(new BiodataCalonMahasiswa());
		else
			init(biodataCalonMahasiswa);

	}

	private class CheckKesamaan implements EventListener {
		private Long biodataCalonMahasiswa;

		public CheckKesamaan(Long biodataCalonMahasiswa) {
			this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {

			final String ta = (String) tahunAkademik.getSelectedItem().getValue();

			if (ta != null && tanggalLahir.getValue() != null && !nama.getValue().trim().isEmpty()
					&& !namaIbu.getValue().trim().isEmpty()) {
				Session session = HibernateUtil.currentSession();
				int count = ((Number) session.createCriteria(BiodataCalonMahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(Restrictions.eq("tahunAkademik", ta)).setProjection(Projections.rowCount())

						.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.EXACT))

						.add(Restrictions.ilike("namaIbu", namaIbu.getValue().trim(), MatchMode.EXACT))

						.add(biodataCalonMahasiswa != null ? Restrictions.ne("id", biodataCalonMahasiswa)
								: Restrictions.sqlRestriction("true"))

						.add(Restrictions.eq("tanggalLahir", tanggalLahir.getValue()))

						.uniqueResult()).intValue();

				System.out.println(
						"count = " + count + ", nama = " + nama.getValue() + ", namaIbu = " + namaIbu.getValue()
								+ ", tanggalLahir = " + Common.dateFormat4.get().format(tanggalLahir.getValue()));

				if (count > 0) {

					count = ((Number) session.createCriteria(BiodataCalonMahasiswa.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

							.add(Restrictions.eq("ditolak", true)).add(Restrictions.eq("tahunAkademik", ta))
							.setProjection(Projections.rowCount())

							.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.EXACT))

							.add(Restrictions.ilike("namaIbu", namaIbu.getValue().trim(), MatchMode.EXACT))

							.add(biodataCalonMahasiswa != null ? Restrictions.ne("id", biodataCalonMahasiswa)
									: Restrictions.sqlRestriction("true"))

							.add(Restrictions.eq("tanggalLahir", tanggalLahir.getValue()))

							.uniqueResult()).intValue();
					if (count > 0) {
						MyMessageboxConfig.show("Anda dinyatakan tidak diterima di tahun akademik ini", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					} else {

						MyMessageboxConfig.show(
								"Data pendaftaran sebagai berikut :\n" + "Nama : " + nama.getValue() + "\n"
										+ "Tanggal Lahir : " + Common.dateFormat2.get().format(tanggalLahir.getValue()) + "\n"
										+ "Nama Ibu : " + namaIbu.getValue() + "telah terdaftar sebelumnya.\n"
										+ "Apakah yakin ingin mengubah data ini ?",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											Common.clear(BiodataCalonMahasiswaAction.this);

											Session session = HibernateUtil.currentSession();
											BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) session
													.createCriteria(BiodataCalonMahasiswa.class)
													.add(Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", true)))

													.add(Restrictions.eq("tahunAkademik", ta))

													.add(Restrictions.ilike("nama", nama.getValue().trim(),
															MatchMode.EXACT))

													.add(Restrictions.ilike("namaIbu", namaIbu.getValue().trim(),
															MatchMode.EXACT))

													.add(Restrictions.eq("tanggalLahir", tanggalLahir.getValue()))

													.add(biodataCalonMahasiswa != null
															? Restrictions.ne("id", biodataCalonMahasiswa)
															: Restrictions.sqlRestriction("true"))

													.setMaxResults(1).uniqueResult();

											init(calonMahasiswa);

											paket.setDisabled(true);
											prodi1.setDisabled(true);
											prodi2.setDisabled(true);
											prodi3.setDisabled(true);
											prodi4.setDisabled(true);
											prodi5.setDisabled(true);
											program.setDisabled(true);

											gelombangPendaftaran.setDisabled(true);

										} else {
											detach();
										}

									}
								});
					}
				}

			}
		}
	}

	private Textbox dusunCalon;

	private Row rowprodi1;

	private Row rowprodi2;

	private Row rowprodi3;

	private Row rowprodi4;

	private Row rowprodi5;

	private MyCheckboxConfig pernyataan;
	private Row rowpindahanDari;
	private Row rowpindahanDariKampus;

	private Row rowketeranganPindah;

	private Row rownimPindahan;

	private Row rowpindahDariKampusLamaDiSemester;

	private Row rownamaTemanInfoKampusDariMana;

	private Row rowketeranganInfoKampusDariMana;

	private Row rowpindahanDariProdi;

	private List<Row> parameterRows;

	private Textbox pinPassword;

	private ParameterTambahanListener parameterTambahanListener;

	private boolean tampilSederhana = false;

	private Combobox jenisSeleksi;

	private Combobox gelar;

	private boolean tampilkanUsernameDanPasswordPadaFormPMB = false;

	private Textbox username;

	private Textbox password;

	private boolean usernameHarusMenggunakanFormatEmail;

	private Textbox kk;

	private Rows subRowsVerifikasiKelengkapanCalonMahasiswa;

	private Rows subRowsVerifikasiNilaiRapor;

	private List<Rows> subRowsVerifikasiNilaiParameter;

	private Row rowPindahan1;

	private Row rowPindahan2;

	private Textbox keterangan;

	protected FotoBiodataCalonMahasiswa fotoBiodataCalonMahasiswa = null;

	private Textbox nisn;

	private AmbilDataKecamatanBanbox kotaInstansi;

	private Textbox instansiAsal;

	private Textbox jabatanDiInstansiAsal;

	private AmbilDataAfiliasiCalonMahasiswaBanbox afiliasiCalonMahasiswa;

	private Tbmuser tbmuser;

	private MyRowStyled rowPaket;

	private EventListener eventListenerPerubahan = null;

	private EventListener masukkanPerubahan = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			if (eventListenerPerubahan != null) {

				try {
					setdata();
					eventListenerPerubahan.onEvent(new Event("", null, biodataCalonMahasiswa));
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:732");
				}

			}

		}
	};

	private MyRowStyled rowJalurPenerimaan;

	private Combobox kelompokJenisSeleksi;

	private Textbox noUjianData;

	private MyRowStyled rowFoto;

	private MyRowStyled rowUmum;

	private AmbilDataPegawaiBanbox afiliasiPegawai;

	private MyRowStyled rowPegawai;

	private MyRowStyled rowMahasiswa;

	private AmbilDataMahasiswaBanbox afiliasiMahasiswa;

	private Radiogroup jenisAfiliasi;

	private MyRowStyled rowkonsentrasi;

	private Combobox konsentrasi;

	private Textbox dariNamaDosenKaryawan;

	private MyRowStyled rowdariNamaDosenKaryawan;

	private Combobox bahasa;

	private boolean isCalonMahasiswaLogin() {
		try {
			Tbmuser current = Common.getCurrentUser();
			return current != null && current.getBiodataCalonMahasiswa() != null;
		} catch (Exception e) {
			return false;
		}
	}

	private boolean isCalonMahasiswaSudahDiterima() {
		try {
			if (biodataCalonMahasiswa == null) {
				return false;
			}
			if (biodataCalonMahasiswa.getProdiLulus() != null) {
				return true;
			}
			if (biodataCalonMahasiswa.getStatusLulus() != null
					&& BiodataCalonMahasiswa.LULUS.equals(biodataCalonMahasiswa.getStatusLulus())) {
				return true;
			}
			if (biodataCalonMahasiswa.getGelombangPendaftaranDiterima() != null) {
				return true;
			}
			if (biodataCalonMahasiswa.getTanggalDiterima() != null) {
				return true;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:797");
		}
		return false;
	}

	private boolean bolehCalonMahasiswaMengubahGelombangPendaftaran() {
		try {
			if (!isCalonMahasiswaLogin()) {
				return false;
			}
			if (biodataCalonMahasiswa == null || biodataCalonMahasiswa.getId() == null) {
				return false;
			}
			if (isCalonMahasiswaSudahDiterima()) {
				return false;
			}
			String aktif = Common.getKonfigurasi(
					"calon_mahasiswa_boleh_mengubah_gelombang_sebelum_diterima", Konfigurasi.AKTIF).getNilai();
			return Konfigurasi.AKTIF.equalsIgnoreCase(aktif);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return false;
	}

	private GelombangPendaftaran refreshSelectedGelombangPendaftaranComboValue() {
		GelombangPendaftaran selected = null;
		try {
			if (gelombangPendaftaran != null && gelombangPendaftaran.getSelectedItem() != null
					&& gelombangPendaftaran.getSelectedItem().getValue() instanceof GelombangPendaftaran) {
				selected = (GelombangPendaftaran) gelombangPendaftaran.getSelectedItem().getValue();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:829");
		}

		if ((selected == null || selected.getId() == null) && mygelombangPendaftaran != null
				&& mygelombangPendaftaran.getId() != null) {
			selected = mygelombangPendaftaran;
		}

		if ((selected == null || selected.getId() == null) && biodataCalonMahasiswa != null
				&& biodataCalonMahasiswa.getGelombangPendaftaran() != null
				&& biodataCalonMahasiswa.getGelombangPendaftaran().getId() != null) {
			selected = biodataCalonMahasiswa.getGelombangPendaftaran();
		}

		if (selected == null || selected.getId() == null) {
			return selected;
		}

		try {
			Session session = HibernateUtil.currentSession();
			GelombangPendaftaran fresh = (GelombangPendaftaran) session.get(GelombangPendaftaran.class, selected.getId());
			if (fresh != null) {
				try {
					fresh.getKelompokParameterTambahanCalonMahasiswas().size();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:853");
				}
				try {
					fresh.getVerifikasiKelengkapanCalonMahasiswas().size();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:857");
				}
				try {
					if (gelombangPendaftaran != null && gelombangPendaftaran.getSelectedItem() != null) {
						gelombangPendaftaran.getSelectedItem().setValue(fresh);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:863");
				}
				try {
					mygelombangPendaftaran = fresh;
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:867");
				}
				try {
					if (biodataCalonMahasiswa != null) {
						biodataCalonMahasiswa.setGelombangPendaftaran(fresh);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:873");
				}
				return fresh;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return selected;
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	public void init(BiodataCalonMahasiswa b) throws Exception {

		try {

			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null) {
				afiliasiCalonMahasiswaData = null;
				try {
					for (Object aa : ConstantValues.ambilBerdasarClass(AfiliasiCalonMahasiswa.class).values()) {
						AfiliasiCalonMahasiswa afiliasiCalonMahasiswa = (AfiliasiCalonMahasiswa) aa;
						if (afiliasiCalonMahasiswa.getAktif()) {
							String khususUser = afiliasiCalonMahasiswa.getKhususUsername();
							for (String username : khususUser.split(",")) {
								if (username.equalsIgnoreCase(tbmuser.getUserId())) {
									afiliasiCalonMahasiswaData = afiliasiCalonMahasiswa;
								}
							}
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:903");
					// TODO: handle exception
				}

			}

			if (afiliasiCalonMahasiswaData != null && afiliasiCalonMahasiswaData.getId() != null) {
				Session session1 = HibernateUtil.currentNativeSession();
				Number s = ((Number) (session1.createCriteria(BiodataCalonMahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(b.getId() == null ? Restrictions.sqlRestriction("true") : Restrictions.ne("id", b.getId()))
						.add(Restrictions.eq("afiliasiCalonMahasiswa", afiliasiCalonMahasiswaData))
						.setProjection(Projections.rowCount()).uniqueResult()));

				Integer jmlAfiliasi = s == null ? 0 : s.intValue();

				System.out.println(
						"jmlAfiliasi = " + jmlAfiliasi + " afiliasiCalonMahasiswaData = " + afiliasiCalonMahasiswaData);

				if (jmlAfiliasi >= afiliasiCalonMahasiswaData.getKuotaDaftar()) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, kuota pendaftaran untuk afiliasi \"{V1}\" telah penuh, sehingga pendaftaran tidak dapat dilanjutkan. Langkah yang dapat dilakukan: (1) Periksa kembali pilihan afiliasi yang dituju; (2) Pilih afiliasi lain yang kuotanya masih tersedia; (3) Hubungi petugas administrasi penerimaan mahasiswa baru apabila memerlukan bantuan.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, afiliasiCalonMahasiswaData.getNama());
					closeOpenedNativeSession(session1);
					return;
				}
				closeOpenedNativeSession(session1);
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:933");
		}

		try {
			if (b.getId() == null) {

				eventListenerPerubahan = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) arg0.getData();
						Common.masukkanSession(BiodataCalonMahasiswa.class, biodataCalonMahasiswa);
					}
				};

				BiodataCalonMahasiswa tempCookie = (BiodataCalonMahasiswa) Common
						.ambilSession(BiodataCalonMahasiswa.class);
				System.out.println("tempCookie -> " + tempCookie);

				if (tempCookie != null) {
					b = tempCookie;
					b.setId(null);
				}
			} else {
				eventListenerPerubahan = null;
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:960");
		}
		final BiodataCalonMahasiswa biodataCalonMahasiswa = b;

		if (biodataCalonMahasiswa.getId() == null && mahasiswaAlumni != null && mahasiswaAlumni.getId() != null) {

			try {
				BeanUtilsBean.getInstance().copyProperties(biodataCalonMahasiswa, mahasiswaAlumni);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:969");
			}
			biodataCalonMahasiswa.setId(null);
		}

		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		emailWajibDiisi = Common.bolehKonfigurasi("saat_pendaftaran_pmb_email_wajib_diisi");
		tampilSederhana = Common.bolehKonfigurasi("tampil_formulir_sederhana", Konfigurasi.TIDAK_AKTIF);

		selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		tbmuser = Common.getCurrentUser();

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		final Center centerUtama = new Center();
		centerUtama.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(centerUtama, true);
		centerUtama.setBorder("none");

		Grid grid = new Grid();
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setStyle("border:none;");

		if (biodataCalonMahasiswa.getId() != null && tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null) {
			Tabbox tabbox = new Tabbox();
			tabbox.setParent(centerUtama);
			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			final MyTabConfig tabData = new MyTabConfig("Data Calon Mahasiswa");
			tabData.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			tabpanel.setParent(tabpanels);

			Borderlayout myborderlayout = new ais.ui.util.MyBorderlayout();
			myborderlayout.setParent(tabpanel);
			myborderlayout.setWidth("100%");
			myborderlayout.setHeight("100%");

			Center mycenter = new Center();
			mycenter.setParent(myborderlayout);
			ais.ui.util.ZkCompat.setFlex(mycenter, true);
			mycenter.setBorder("none");

			Row rowUtama = Common.tampilanScroll1(mycenter);
			rowUtama.setValign("top");

			Row rowUtama1;
			try {
				if ((mygelombangPendaftaran != null && mygelombangPendaftaran.getId() != null)
						|| (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getGelombangPendaftaran() != null)) {
					GelombangPendaftaran gel = (mygelombangPendaftaran != null
							&& mygelombangPendaftaran.getId() != null) ? mygelombangPendaftaran
									: biodataCalonMahasiswa.getGelombangPendaftaran();
					LampiranLain kop = LampiranLain.ambil(gel.getId(), LampiranLain.KOP_GELOMBANG_PMB);

					if (kop != null && kop.getId() != null) {

						ais.ui.util.ZkCompat.setSpans(rowUtama, "2");
						Image image = new Image(kop.createLinkUri());
						image.setWidth("100%");
						rowUtama.appendChild(image);

						rowUtama1 = new MyFormRow();
						ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
						rowUtama1.setValign("top");
						rowUtama1.setParent(rowUtama.getParent());
					} else {
						Hbox hbox = PMBAction.headerBox(false);

						hbox.setSclass("headerHbox");
						ais.ui.util.ZkCompat.setSpans(rowUtama, "2");
						rowUtama.appendChild(hbox);

						rowUtama1 = new MyFormRow();
						ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
						rowUtama1.setValign("top");
						rowUtama1.setParent(rowUtama.getParent());
					}
				} else {
					Hbox hbox = PMBAction.headerBox(false);

					hbox.setSclass("headerHbox");
					ais.ui.util.ZkCompat.setSpans(rowUtama, "2");
					rowUtama.appendChild(hbox);

					rowUtama1 = new MyFormRow();
					ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
					rowUtama1.setValign("top");
					rowUtama1.setParent(rowUtama.getParent());
				}
			} catch (Exception e) {
				Hbox hbox = PMBAction.headerBox(false);

				hbox.setSclass("headerHbox");
				ais.ui.util.ZkCompat.setSpans(rowUtama, "2");
				rowUtama.appendChild(hbox);

				rowUtama1 = new MyFormRow();
				ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
				rowUtama1.setValign("top");
				rowUtama1.setParent(rowUtama.getParent());
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:1080");
			}

			MyGroupboxStyled myGroupboxUtama = new MyGroupboxStyled();
			myGroupboxUtama.appendChild(new MyCaptionStyled("FORMULIR PENDAFTARAN CALON MAHASISWA"));
			myGroupboxUtama.setParent(rowUtama1);

			myGroupboxUtama.appendChild(grid);
//			grid.setHeight("1500px");

			int jumlah = biodataCalonMahasiswa.ambilHasilUjianMahasiswa(HibernateUtil.currentSession(), false).size();

			final MyTabConfig tabHasilUjian = new MyTabConfig(Common.getBahasaConfig("Hasil Ujian") + " "
					+ (jumlah == 0 ? "" : "(" + jumlah + " " + Common.getBahasaConfig("ujian") + ")"));
			tabHasilUjian.setParent(tabs);
			final Tabpanel tabpanelHasilUjian = new ais.ui.util.MyTabpanel();
			tabpanelHasilUjian.setParent(tabpanels);

			tabHasilUjian.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelHasilUjian.getChildren().isEmpty()) {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								new HasilUjianHelper(null, biodataCalonMahasiswa, null).display(tabpanelHasilUjian);
							}
						});

					}
				}
			});

			MyTabConfig tabUjianTugasMateri = new MyTabConfig("Tugas, Ujian, Materi");
			tabUjianTugasMateri.setParent(tabs);
			final Tabpanel tabpanelUjianTugasMateri = new ais.ui.util.MyTabpanel();
			tabpanelUjianTugasMateri.setParent(tabpanels);

			tabUjianTugasMateri.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelUjianTugasMateri.getChildren().isEmpty()) {

						TampilanUjianCalonMahasiswa tampilanUjianCalonMahasiswa = new TampilanUjianCalonMahasiswa(
								false);
						tampilanUjianCalonMahasiswa.init(biodataCalonMahasiswa);
						tampilanUjianCalonMahasiswa.setParent(tabpanelUjianTugasMateri);
						tampilanUjianCalonMahasiswa.setHeight("100%");
						tampilanUjianCalonMahasiswa.setWidth("100%");

					}
				}
			});

		} else {

			Row rowUtama = Common.tampilanScroll1(centerUtama);
			rowUtama.setValign("top");

			Row rowUtama1;
			try {
				if ((mygelombangPendaftaran != null && mygelombangPendaftaran.getId() != null)
						|| (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getGelombangPendaftaran() != null)) {
					GelombangPendaftaran gel = (mygelombangPendaftaran != null
							&& mygelombangPendaftaran.getId() != null) ? mygelombangPendaftaran
									: biodataCalonMahasiswa.getGelombangPendaftaran();
					LampiranLain kop = LampiranLain.ambil(gel.getId(), LampiranLain.KOP_GELOMBANG_PMB);

					if (kop != null && kop.getId() != null) {

						ais.ui.util.ZkCompat.setSpans(rowUtama, "2");
						Image image = new Image(kop.createLinkUri());
						image.setWidth("100%");
						rowUtama.appendChild(image);

						rowUtama1 = new MyFormRow();
						ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
						rowUtama1.setValign("top");
						rowUtama1.setParent(rowUtama.getParent());
					} else {

						Hbox hbox = PMBAction.headerBox(false);
						hbox.setSclass("headerHbox");
						ais.ui.util.ZkCompat.setSpans(rowUtama, "2");
						rowUtama.appendChild(hbox);

						rowUtama1 = new MyFormRow();
						ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
						rowUtama1.setValign("top");
						rowUtama1.setParent(rowUtama.getParent());
					}
				} else {
					Hbox hbox = PMBAction.headerBox(false);
					hbox.setSclass("headerHbox");
					ais.ui.util.ZkCompat.setSpans(rowUtama, "2");
					rowUtama.appendChild(hbox);

					rowUtama1 = new MyFormRow();
					ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
					rowUtama1.setValign("top");
					rowUtama1.setParent(rowUtama.getParent());
				}
			} catch (Exception e) {
				Hbox hbox = PMBAction.headerBox(false);
				hbox.setSclass("headerHbox");
				ais.ui.util.ZkCompat.setSpans(rowUtama, "2");
				rowUtama.appendChild(hbox);

				rowUtama1 = new MyFormRow();
				ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
				rowUtama1.setValign("top");
				rowUtama1.setParent(rowUtama.getParent());
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:1196");
			}

			MyGroupboxStyled myGroupboxUtama = new MyGroupboxStyled();
			myGroupboxUtama.appendChild(new MyCaptionStyled("FORMULIR PENDAFTARAN CALON MAHASISWA"));
			myGroupboxUtama.setParent(rowUtama1);
			myGroupboxUtama.appendChild(grid);
		}

		if (mygelombangPendaftaran != null && mygelombangPendaftaran.getId() != null) {
			BiodataCalonMahasiswaAction.initBg(centerUtama, mygelombangPendaftaran);
		} else {
			BiodataCalonMahasiswaAction.initBg(centerUtama, biodataCalonMahasiswa.getGelombangPendaftaran());
		}
		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth(Common.isMobile() ? "40%" : "30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		final Rows rows = new Rows();
		rows.setParent(grid);

		final boolean berbintang = biodataCalonMahasiswa.getGelombangPendaftaran() == null
				|| biodataCalonMahasiswa.getGelombangPendaftaran().getDokumenHarusDiverivikasiSebelumBisaSimpan();

		if (berbintang) {
			Row row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.appendChild(new Label("Tanda \"* \" merupakan isian yang wajib diisi"));
		}
		Row row = new MyRowStyled();
		row.setVisible(biodataCalonMahasiswa.getId() != null);
		row.setParent(rows);
		row.appendChild(new MyLabelConfigTitikDua("Nomor Pendaftaran"));

		noRegistrasi = new Textbox(biodataCalonMahasiswa.getNoRegistrasi());

		if (tbmuser == null || tbmuser.getBiodataCalonMahasiswa() != null) {
			row.appendChild(new MyLabelBoldAja(biodataCalonMahasiswa.getNoRegistrasi()));
		} else {
			row.appendChild(noRegistrasi);
		}

		noRegistrasi.setWidth("90%");
		noRegistrasi.setStyle("font-weight: bold; font-size: large;");

		tampilkanUsernameDanPasswordPadaFormPMB = Common.bolehKonfigurasi("tampilkan_username_dan_password_form_PMB", Konfigurasi.TIDAK_AKTIF);
		usernameHarusMenggunakanFormatEmail = Common.bolehKonfigurasi("username_pmb_harus_menggunakan_format_email", Konfigurasi.TIDAK_AKTIF);

		noUjianData = new Textbox(biodataCalonMahasiswa.getNoUjian());
		noUjianData.setWidth("90%");
		noUjianData.setStyle("font-weight: bold; font-size: large;");

		if (biodataCalonMahasiswa.getNoUjian() != null) {
			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new MyLabelConfigTitikDua("No. Ujian : "));

			if (tbmuser == null || tbmuser.getBiodataCalonMahasiswa() != null) {
				row.appendChild(new MyLabelBoldAja(biodataCalonMahasiswa.getNoUjian()));
			} else {
				row.appendChild(noUjianData);
			}

		}

		if (biodataCalonMahasiswa.getMahasiswa() != null) {
			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new MyLabelConfigTitikDua("NIM Mahasiswa : "));
			row.appendChild(new MyLabelBoldAja(biodataCalonMahasiswa.getMahasiswa().getNim()));
		}

		// Hanya tampilkan username/password untuk ADMIN (bukan calon sendiri) saat config aktif
		boolean tampilRowLogin = tampilkanUsernameDanPasswordPadaFormPMB
				&& tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null;

		if (tampilRowLogin) {
			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new MyLabelConfigTitikDua(usernameHarusMenggunakanFormatEmail ? "Email *" : "Username Login"));
			Vbox usernameVbox = new Vbox();
			usernameVbox.appendChild(username = new Textbox(biodataCalonMahasiswa.getUsername()));
			username.setWidth("90%");
			Label usernameHint = new Label(usernameHarusMenggunakanFormatEmail
					? "Masukkan alamat email yang valid sebagai username login calon"
					: "Kosongkan agar otomatis menggunakan Nomor Pendaftaran sebagai username");
			usernameHint.setStyle("font-size:10px;color:gray;");
			usernameVbox.appendChild(usernameHint);
			row.appendChild(usernameVbox);

			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new MyLabelConfigTitikDua("Password Login"));
			Vbox passwordVbox = new Vbox();
			passwordVbox.appendChild(password = new Textbox(
					biodataCalonMahasiswa.getPassword() == null || biodataCalonMahasiswa.getPassword().trim().equals("")
							? ""
							: Common.desEncrypter.get().decrypt(biodataCalonMahasiswa.getPassword())));
			password.setWidth("90%");
			password.setType("password");
			Label passwordHint = new Label(ais.common.Common.getBahasaConfig("Kosongkan agar otomatis menggunakan password default sistem"));
			passwordHint.setStyle("font-size:10px;color:gray;");
			passwordVbox.appendChild(passwordHint);
			row.appendChild(passwordVbox);
		}

		row = new MyRowStyled();
		tanggalPendaftaran = new MyDatebox(biodataCalonMahasiswa.getTanggalPendaftaran());
		row.setParent(rows);
		row.appendChild(new MyLabelConfigTitikDua("Tanggal Pendaftaran"));
		if (tbmuser == null || tbmuser.getBiodataCalonMahasiswa() != null) {
			row.appendChild(new Label(Common.dateFormat51.get().format(biodataCalonMahasiswa.getTanggalPendaftaran())));
		} else {
			row.appendChild(tanggalPendaftaran);
		}
		tanggalPendaftaran.setWidth("90%");
		tanggalPendaftaran.setDisabled(tbmuser == null);

		tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		row = new MyRowStyled();
		row.setVisible(!tampilSederhana);

		row.setParent(rows);
		row.appendChild(new MyLabelConfigTitikDua("Tahun Akademik *"));
		tahunAkademik = new Combobox();
		tahunAkademik.setReadonly(true);

		Map<Long, GeneralValueObject> gelombangsAktif = ConstantValues.ambilBerdasarClass(GelombangPendaftaran.class);
		TreeSet<String> tas = new TreeSet<String>();
		Date sekarang = WaktuUtil.getDate();

		for (Long gelId : gelombangsAktif.keySet()) {
			GelombangPendaftaran gelombangPendaftaran = (GelombangPendaftaran) ConstantValues
					.ambil(GelombangPendaftaran.class.getName(), gelId);
			if (gelombangPendaftaran != null && gelombangPendaftaran.getAktif()
					&& (gelombangPendaftaran.getMulai().before(sekarang) || Common.dateFormat8.get()
							.format(gelombangPendaftaran.getMulai()).equals(Common.dateFormat8.get().format(sekarang)))
					&& (gelombangPendaftaran.getSampai().after(sekarang) || Common.dateFormat8.get()
							.format(gelombangPendaftaran.getSampai()).equals(Common.dateFormat8.get().format(sekarang)))) {
				tas.add(gelombangPendaftaran.getTahunAkademik());
			}
		}

		for (String ta : tas) {
			Comboitem comboitem = new Comboitem(ta);
			comboitem.setValue(ta);
			tahunAkademik.appendChild(comboitem);
		}

		if (tahunAkademik.getChildren().isEmpty() && tahunAkademikPenerimaanMahasiswaBaru != null) {
			Comboitem comboitem = new Comboitem(tahunAkademikPenerimaanMahasiswaBaru);
			comboitem.setValue(tahunAkademikPenerimaanMahasiswaBaru);
			tahunAkademik.appendChild(comboitem);
		}

		if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null
				&& biodataCalonMahasiswa.getTahunAkademik() != null
				&& !tas.contains(biodataCalonMahasiswa.getTahunAkademik())) {
			Comboitem comboitem = new Comboitem(biodataCalonMahasiswa.getTahunAkademik());
			comboitem.setValue(biodataCalonMahasiswa.getTahunAkademik());
			tahunAkademik.appendChild(comboitem);
		}

		Common.selectComboItem(true, tahunAkademik,
				biodataCalonMahasiswa.getTahunAkademik() == null ? tahunAkademikPenerimaanMahasiswaBaru
						: biodataCalonMahasiswa.getTahunAkademik());

		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
			row.appendChild(new Label(biodataCalonMahasiswa.getTahunAkademik()));
		} else if (mygelombangPendaftaran != null) {
			Common.selectComboItem(true, tahunAkademik, mygelombangPendaftaran.getTahunAkademik());
			tahunAkademik.setDisabled(true);
			row.appendChild(new Label(mygelombangPendaftaran.getTahunAkademik()));
		} else {
			row.appendChild(tahunAkademik);
		}

		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row = new MyRowStyled();
		row.setVisible(!tampilSederhana);

		row.setParent(rows);
		row.appendChild(new MyLabelConfigTitikDua("Gelombang Pendaftaran *"));
		gelombangPendaftaran = new Combobox();
		gelombangPendaftaran.setReadonly(true);
		EventListener gelombangEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(gelombangPendaftaran);
				gelombangPendaftaran.setSelectedItem(null);

				Tbmuser tbmuser = Common.getCurrentUser();

				Criterion criterion = Restrictions.and(
						tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("tahunAkademik", tahunAkademik.getSelectedItem().getValue()),
						tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getMahasiswa() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.and(Restrictions.le("mulai", ais.ui.util.WaktuUtil.getDate()),
										Restrictions.ge("sampai", ais.ui.util.WaktuUtil.getDate())));

				criterion = Restrictions.and(criterion,
						tbmuser == null || tbmuser.getBiodataCalonMahasiswa() != null
								? Restrictions.or(Restrictions.eq("bisaDipilihPendaftarOnline", true),
										Restrictions.isNull("bisaDipilihPendaftarOnline"))
								: Restrictions.sqlRestriction("true"));

				Common.insertComboDanSemua(gelombangPendaftaran, new String[] { "nama" }, "tahunAkademik",
						GelombangPendaftaran.class, "== Klik disini untuk pilih ==",
						selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.eq("perguruanTinggi", selectedPerguruanTinggi),
										Restrictions.isNull("perguruanTinggi")),
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								criterion));

				if (biodataCalonMahasiswa.getGelombangPendaftaran() != null) {
					Common.selectComboItem(true, gelombangPendaftaran, biodataCalonMahasiswa.getGelombangPendaftaran());
					refreshSelectedGelombangPendaftaranComboValue();
				}
			}
		};

		final boolean bolehUbahGelombangCalon = bolehCalonMahasiswaMengubahGelombangPendaftaran();
		if (isCalonMahasiswaSudahDiterima()) {
			row.appendChild(new Label(biodataCalonMahasiswa.getGelombangPendaftaranDiterima() != null
					? biodataCalonMahasiswa.getGelombangPendaftaranDiterima().getNama()
					: biodataCalonMahasiswa.getGelombangPendaftaran() == null ? ""
							: biodataCalonMahasiswa.getGelombangPendaftaran().getNama()));
		} else if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null && !bolehUbahGelombangCalon) {
			row.appendChild(new Label(biodataCalonMahasiswa.getGelombangPendaftaran() == null ? ""
					: biodataCalonMahasiswa.getGelombangPendaftaran().getNama()));
		} else if (mygelombangPendaftaran != null && !bolehUbahGelombangCalon) {
			biodataCalonMahasiswa.setGelombangPendaftaran(mygelombangPendaftaran);
			Common.selectComboItem(true, gelombangPendaftaran, mygelombangPendaftaran);
			gelombangPendaftaran.setDisabled(true);
			row.appendChild(new Label(mygelombangPendaftaran.getNama()));
		} else {
			row.appendChild(gelombangPendaftaran);
		}

		gelombangPendaftaran.setWidth("90%");

		gelombangEventListener.onEvent(null);
		refreshSelectedGelombangPendaftaranComboValue();
		tahunAkademik.addEventListener("onChange", gelombangEventListener);

		if (biodataCalonMahasiswa.getGelombangPendaftaran() != null) {
			Common.selectComboItem(true, gelombangPendaftaran, biodataCalonMahasiswa.getGelombangPendaftaran());
			refreshSelectedGelombangPendaftaranComboValue();
		}

		rowJalurPenerimaan = new MyRowStyled();
		rowJalurPenerimaan.setVisible(!tampilSederhana);
		kelompokJenisSeleksi = new Combobox();
		rowJalurPenerimaan.setParent(rows);
		rowJalurPenerimaan.appendChild(new MyLabelConfigTitikDua("Jalur Penerimaan"));
		rowJalurPenerimaan.appendChild(kelompokJenisSeleksi);
		kelompokJenisSeleksi.setReadonly(true);
		kelompokJenisSeleksi.setWidth("90%");

		row = new MyRowStyled();
		row.setVisible(!tampilSederhana);
		jenisSeleksi = new Combobox();
		row.setParent(rows);
		row.appendChild(new MyLabelConfigTitikDua("Jenis Seleksi *"));

		if (biodataCalonMahasiswa.getJenisSeleksiDipilih() != null) {
			row.appendChild(new Label(biodataCalonMahasiswa.getJenisSeleksiDipilih().getNama()));
		} else if (mygelombangPendaftaran != null && mygelombangPendaftaran.getJenisSeleksiDipilihDiFormPendaftaran()) {
			row.appendChild(jenisSeleksi);
		} else if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
			row.appendChild(new Label(biodataCalonMahasiswa.getJenisSeleksi() == null ? ""
					: biodataCalonMahasiswa.getJenisSeleksi().getNama()));
		} else if (myjenisSeleksi != null) {
			biodataCalonMahasiswa.setJenisSeleksi(myjenisSeleksi);
			Common.selectComboItem(true, jenisSeleksi, myjenisSeleksi);
			gelombangPendaftaran.setDisabled(true);
			row.appendChild(new Label(myjenisSeleksi.getNama()));
		} else {
			row.appendChild(jenisSeleksi);
		}

		jenisSeleksi.setReadonly(true);
		jenisSeleksi.setWidth("90%");

		if (Common.bolehKonfigurasi("tampilkan_link_login_oleh_admin_di_data_calon_mahasiswa")) {
			if (biodataCalonMahasiswa.getId() != null) {

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Link"));

				final A a = new A("Tampilkan Link");
				a.setHref("");
				row.appendChild(a);

				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String code = biodataCalonMahasiswa.urlLogin();
						a.setLabel(code);
						a.setHref(Common.getRequestHostWithProtocol() + "/logoff?param="
								+ URLEncoder.encode(code, "UTF-8"));
					}
				});

			}
		}
		boolean integrasi_pmb_arkatama = Common.bolehKonfigurasi("integrasi_pmb_arkatama", Konfigurasi.TIDAK_AKTIF);

		row = new MyRowStyled();
		row.setVisible(integrasi_pmb_arkatama && Common.getApakahAdmin());
		pinPassword = new Textbox(biodataCalonMahasiswa.getPinPassword());
		row.setParent(rows);
		row.appendChild(new MyLabelConfigTitikDua("Kode Feeder"));
		row.appendChild(pinPassword);
		pinPassword.setWidth("90%");

		String statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser,
				"afiliasiCalonMahasiswa");

		row = new MyRowStyled();
		row.setVisible(
				afiliasiCalonMahasiswaData == null && (tbmuser == null || tbmuser.getBiodataCalonMahasiswa() == null)
						&& ((statusWajibIsi.equals(Konfigurasi.AKTIF))
								|| statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)));
		row.setParent(rows);
		row.appendChild(new MyLabelConfigTitikDua(
				"Jenis Afiliasi " + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		jenisAfiliasi = new Radiogroup();
		jenisAfiliasi.setParent(row);

		int pilih = 0;
		if (biodataCalonMahasiswa.getAfiliasiPegawai() != null) {
			pilih = 1;
		} else if (biodataCalonMahasiswa.getAfiliasiMahasiswa() != null) {
			pilih = 2;
		}

		MyRadioConfig radioConfig = new MyRadioConfig("Umum");
		radioConfig.setValue("Umum");
		radioConfig.setParent(jenisAfiliasi);
		radioConfig.setChecked(pilih == 0);

		radioConfig = new MyRadioConfig("Pegawai/Dosen");
		radioConfig.setValue("Pegawai/Dosen");
		radioConfig.setParent(jenisAfiliasi);
		radioConfig.setChecked(pilih == 1);

		radioConfig = new MyRadioConfig("Mahasiswa");
		radioConfig.setValue("Mahasiswa");
		radioConfig.setParent(jenisAfiliasi);
		radioConfig.setChecked(pilih == 2);

		rowUmum = new MyRowStyled();
		rowUmum.setVisible(
				(pilih == 0 || afiliasiCalonMahasiswaData != null) && ((statusWajibIsi.equals(Konfigurasi.AKTIF))
						|| statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)));
		rowUmum.setParent(rows);
		rowUmum.appendChild(new MyLabelConfigTitikDua(
				"Afiliasi " + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		afiliasiCalonMahasiswa = new AmbilDataAfiliasiCalonMahasiswaBanbox();

		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
			rowUmum.appendChild(new Label(biodataCalonMahasiswa.getAfiliasiCalonMahasiswa() == null ? ""
					: biodataCalonMahasiswa.getAfiliasiCalonMahasiswa().getNama()));
		} else if (afiliasiCalonMahasiswaData == null) {
			rowUmum.appendChild(afiliasiCalonMahasiswa);
		} else {
			rowUmum.appendChild(new Label(afiliasiCalonMahasiswaData.getNama()));
		}

		afiliasiCalonMahasiswa.setWidth("90%");

		afiliasiCalonMahasiswa.setAttribute("afiliasiCalonMahasiswa",
				biodataCalonMahasiswa.getAfiliasiCalonMahasiswa());
		afiliasiCalonMahasiswa.setValue(biodataCalonMahasiswa.getAfiliasiCalonMahasiswa() == null ? ""
				: biodataCalonMahasiswa.getAfiliasiCalonMahasiswa().getNama());

		afiliasiCalonMahasiswa.setReadonly(true);
		if (afiliasiCalonMahasiswaData != null) {
			afiliasiCalonMahasiswa.setAttribute("afiliasiCalonMahasiswa", afiliasiCalonMahasiswaData);
			afiliasiCalonMahasiswa.setValue(afiliasiCalonMahasiswaData.getNama());
			afiliasiCalonMahasiswa.setDisabled(true);
		}

		rowPegawai = new MyRowStyled();
		rowPegawai.setVisible(
				afiliasiCalonMahasiswaData == null && pilih == 1 && ((statusWajibIsi.equals(Konfigurasi.AKTIF))
						|| statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)));
		rowPegawai.setParent(rows);
		rowPegawai.appendChild(new MyLabelConfigTitikDua(
				"Afiliasi Pegawai/Dosen " + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		afiliasiPegawai = new AmbilDataPegawaiBanbox(false);

		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
			rowPegawai.appendChild(new Label(biodataCalonMahasiswa.getAfiliasiPegawai() == null ? ""
					: biodataCalonMahasiswa.getAfiliasiPegawai().getNama()));
		} else {
			rowPegawai.appendChild(afiliasiPegawai);
		}

		afiliasiPegawai.setWidth("90%");

		afiliasiPegawai.setAttribute("pegawai", biodataCalonMahasiswa.getAfiliasiPegawai());
		afiliasiPegawai.setValue(biodataCalonMahasiswa.getAfiliasiPegawai() == null ? ""
				: biodataCalonMahasiswa.getAfiliasiPegawai().getNama());

		rowMahasiswa = new MyRowStyled();
		rowMahasiswa.setVisible(
				afiliasiCalonMahasiswaData == null && pilih == 2 && ((statusWajibIsi.equals(Konfigurasi.AKTIF))
						|| statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)));
		rowMahasiswa.setParent(rows);
		rowMahasiswa.appendChild(new MyLabelConfigTitikDua(
				"Afiliasi Mahasiswa " + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		afiliasiMahasiswa = new AmbilDataMahasiswaBanbox();

		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
			rowMahasiswa.appendChild(new Label(biodataCalonMahasiswa.getAfiliasiMahasiswa() == null ? ""
					: biodataCalonMahasiswa.getAfiliasiMahasiswa().getNama()));
		} else {
			rowMahasiswa.appendChild(afiliasiMahasiswa);
		}

		afiliasiMahasiswa.setWidth("90%");

		afiliasiMahasiswa.setAttribute("mahasiswa", biodataCalonMahasiswa.getAfiliasiMahasiswa());
		afiliasiMahasiswa.setValue(biodataCalonMahasiswa.getAfiliasiMahasiswa() == null ? ""
				: biodataCalonMahasiswa.getAfiliasiMahasiswa().getNama());

		jenisAfiliasi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser,
						"afiliasiCalonMahasiswa");

				String val = jenisAfiliasi.getSelectedItem() == null
						|| jenisAfiliasi.getSelectedItem().getValue() == null ? "Umum"
								: jenisAfiliasi.getSelectedItem().getValue().toString();

				rowUmum.setVisible(val.equals("Umum") && ((statusWajibIsi.equals(Konfigurasi.AKTIF))
						|| statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)));
				rowPegawai.setVisible(val.equals("Pegawai/Dosen") && ((statusWajibIsi.equals(Konfigurasi.AKTIF))
						|| statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)));
				rowMahasiswa.setVisible(val.equals("Mahasiswa") && ((statusWajibIsi.equals(Konfigurasi.AKTIF))
						|| statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)));

			}
		});

		String currentLang = null;
		try {
			currentLang = (String) Sessions.getCurrent(true).getAttribute("current_lang");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:1665");

		}

		if (currentLang == null) {
			currentLang = tbmuser == null ? null : tbmuser.getBahasa();
		}
		if (currentLang == null) {
			currentLang = Tbmuser.INDONESIA;
		}

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("tampilkan_label_bahasa_form_PMB", Konfigurasi.TIDAK_AKTIF));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bahasa"));
		row.appendChild(bahasa = new Combobox());
		Comboitem comboitem = new Comboitem();
		comboitem.setLabel(Tbmuser.INDONESIA);
		comboitem.setValue(Tbmuser.INDONESIA);
		bahasa.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Tbmuser.ENGLISH);
		comboitem.setValue(Tbmuser.ENGLISH);
		bahasa.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Tbmuser.ARAB);
		comboitem.setValue(Tbmuser.ARAB);
		bahasa.appendChild(comboitem);
		bahasa.setWidth("90%");
		bahasa.setReadonly(true);

		Common.selectComboItem(true, bahasa,
				biodataCalonMahasiswa.getBahasa() == null || biodataCalonMahasiswa.getBahasa().trim().isEmpty()
						? currentLang
						: biodataCalonMahasiswa.getBahasa());

		boolean tampilkanLabelBesarPadaFormPMB = Common.bolehKonfigurasi("tampilkan_label_besar_pada_form_PMB");

		row = new MyRowStyled();
		row.setVisible(tampilkanLabelBesarPadaFormPMB);

		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("I. Data Diri Pendaftar"));

		row = new MyRowStyled();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "gelar");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Gelar " + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(gelar = new Combobox());
		gelar.setWidth("90%");

		MyComboitemConfig comboitemConfig = new MyComboitemConfig("Tuan");
		comboitemConfig.setValue(Common.getBahasaConfig("Tuan"));
		gelar.appendChild(comboitemConfig);

		comboitemConfig = new MyComboitemConfig("Nyonya");
		comboitemConfig.setValue(Common.getBahasaConfig("Nyonya"));
		gelar.appendChild(comboitemConfig);

		comboitemConfig = new MyComboitemConfig("Nona");
		comboitemConfig.setValue(Common.getBahasaConfig("Nona"));
		gelar.appendChild(comboitemConfig);

		Common.selectComboItem(true, gelar, biodataCalonMahasiswa.getGelar());

		gelar.setReadonly(true);

		CheckKesamaan checkKesamaan = new CheckKesamaan(biodataCalonMahasiswa.getId());

		if (mahasiswaAlumni != null && mahasiswaAlumni.getNim() != null) {
			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new MyLabelConfigTitikDua("NIM Alumni"));
			row.appendChild(new Label(mahasiswaAlumni.getNim()));
		}

		row = new MyRowStyled();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "nama");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Nama Lengkap " + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(
				nama = new Textbox(biodataCalonMahasiswa.getNama() == null ? "" : biodataCalonMahasiswa.getNama()));
		nama.setWidth("90%");
		nama.addEventListener("onChange", checkKesamaan);
		nama.setDisabled(biodataCalonMahasiswa.getMahasiswa() != null || mahasiswaAlumni != null);

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "jenisKartuIdentitas");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Jenis Kartu Identitas " + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));

		Common.insertComboDanSemua(jenisKartuIdentitas = new Combobox(), new String[] { "nama" }, "keterangan",
				JenisKartuIdentitasMahasiswaBaru.class, "== Klik disini untuk pilih ==",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		jenisKartuIdentitas.setReadonly(true);
		Common.selectComboItem(true, jenisKartuIdentitas, biodataCalonMahasiswa.getJenisKartuIdentitas());
		row.appendChild(jenisKartuIdentitas);
		jenisKartuIdentitas.setWidth("90%");

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "noIdentitas");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"No Kartu Identitas " + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(noIdentitas = new Textbox(
				biodataCalonMahasiswa.getNoIdentitas() == null ? "" : biodataCalonMahasiswa.getNoIdentitas()));
		noIdentitas.setWidth("90%");
		// noIdentitas.//setConstraint("no empty");

		if (Common.bolehKonfigurasi("integrasi_pmb_arkatama", Konfigurasi.TIDAK_AKTIF)) {
			noIdentitas.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					JenisSeleksi js = (JenisSeleksi) (jenisSeleksi.getSelectedItem() == null ? null
							: jenisSeleksi.getSelectedItem().getValue());

					if (js != null && !noIdentitas.getValue().trim().isEmpty()
							&& (biodataCalonMahasiswa.getId() == null || !biodataCalonMahasiswa.getNoIdentitas()
									.equalsIgnoreCase(noIdentitas.getValue().trim()))) {

						if (PmbArkatama.token == null || PmbArkatama.token.trim().isEmpty()) {
							PmbArkatama.login();
						}

						String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id")
								.getNilai() + "/api/Registrasi/isNIKExist");
						JSONObject data = new JSONObject();
						data.put("nik", noIdentitas.getValue().trim());
						data.put("id_jalur_masuk", js.getKode());

						JSONObject jsonObject = PmbArkatama.prosesPost(strURL, data.toString());
						if (!jsonObject.isNull("status")
								&& !jsonObject.get("status").toString().equalsIgnoreCase("200")) {

							MyMessageboxConfig.show(
									jsonObject.isNull("error")
											? "Nomor Identitas / NIK " + noIdentitas.getValue().trim()
													+ " sudah terdaftar"
											: jsonObject.getString("error") + "\nNIK " + noIdentitas.getValue().trim(),
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							noIdentitas.focus();
							noIdentitas.setValue("");
						}
					}

				}
			});
		}

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "nisn");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua("Nomor Induk Siswa Nasional (NISN) "
				+ ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(
				nisn = new Textbox(biodataCalonMahasiswa.getNisn() == null ? "" : biodataCalonMahasiswa.getNisn()));
		nisn.setWidth("90%");

		String ketNisn = Common.getKonfigurasi("keterangan_nisn_di_biodata_calon_mhs", "").getNilai().trim();
		if (!ketNisn.isEmpty()) {
			Common.initKeterangan(rows, ketNisn).setStyle(row.getStyle());
		}

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "kk");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"No Kartu Keluarga " + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(kk = new Textbox(biodataCalonMahasiswa.getKk()));
		kk.setWidth("90%");

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "tempatLahir");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Tempat Lahir " + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(tempatLahir = new Textbox(
				biodataCalonMahasiswa.getTempatLahir() == null ? "" : biodataCalonMahasiswa.getTempatLahir()));
		tempatLahir.setWidth("90%");

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "tanggalLahir");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Tanggal Lahir " + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(tanggalLahir = new MyDatebox(biodataCalonMahasiswa.getTanggalLahir()));
		tanggalLahir.addEventListener("onChange", checkKesamaan);

		row = new MyRowStyled();
		if (usernameHarusMenggunakanFormatEmail) {
			row.setVisible(false);
		}
		row.setParent(rows);
		row.appendChild(new MyLabelConfigTitikDua("Email " + (emailWajibDiisi ? "*" : "")));
		row.appendChild(
				email = new Textbox(biodataCalonMahasiswa.getEmail() == null ? "" : biodataCalonMahasiswa.getEmail()));
		email.setWidth("90%");
		if (!usernameHarusMenggunakanFormatEmail) {
			Common.initKeterangan(rows,
					"Jika email lebih dari satu, gunakan tanda koma (,) sebagai pemisah, misal-nya :  anda@mail.com,anda1@oke.com,anda3@mail.com. Sedangkan untuk email utama, tempatkan di urutan paling depan.")
					.setStyle(row.getStyle());
		}
		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "jenisKelamin");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);

		row.appendChild(new MyLabelConfigTitikDua(
				"Jenis Kelamin " + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));

		jenisKelamin = new Combobox();
		jenisKelamin.setReadonly(true);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Laki-laki");
		comboitem.setValue("Laki-laki");
		jenisKelamin.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Perempuan");
		comboitem.setValue("Perempuan");
		jenisKelamin.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("== Klik disini untuk pilih ==");
		comboitem.setValue(null);
		jenisKelamin.appendChild(comboitem);
		Common.selectComboItem(true, jenisKelamin, biodataCalonMahasiswa.getJenisKelamin());
		row.appendChild(jenisKelamin);
		jenisKelamin.setWidth("90%");

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "statusNikah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);

		row.appendChild(new MyLabelConfigTitikDua(
				"Status Perkawinan" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		statusNikah = new Combobox();
		statusNikah.setReadonly(true);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Belum Nikah");
		comboitem.setValue(0);
		statusNikah.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Nikah");
		comboitem.setValue(1);
		statusNikah.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Janda");
		comboitem.setValue(2);
		statusNikah.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Duda");
		comboitem.setValue(3);
		statusNikah.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("== Klik disini untuk pilih ==");
		comboitem.setValue(null);
		statusNikah.appendChild(comboitem);

		Common.selectComboItem(true, statusNikah,
				biodataCalonMahasiswa.getId() == null ? null : biodataCalonMahasiswa.getStatusNikah());
		row.appendChild(statusNikah);
		statusNikah.setWidth("90%");
		// statusNikah.//setConstraint("no empty");

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "agama");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Agama" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));

		Common.insertComboDanSemua(agama = new Combobox(), new String[] { "nama" }, "keterangan", Agama.class,
				"== Klik disini untuk pilih ==",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		agama.setReadonly(true);
		Common.selectComboItem(true, agama, biodataCalonMahasiswa.getAgama());
		row.appendChild(agama);
		agama.setWidth("90%");

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "kewarganegaraan");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Kewarganegaraan" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		kewarganegaraan = new Combobox();
		kewarganegaraan.setReadonly(true);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(ais.database.model.Mahasiswa.WNI);
		comboitem.setValue(ais.database.model.Mahasiswa.WNI);
		kewarganegaraan.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(ais.database.model.Mahasiswa.WNA);
		comboitem.setValue(ais.database.model.Mahasiswa.WNA);
		kewarganegaraan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("== Klik disini untuk pilih ==");
		comboitem.setValue(null);
		kewarganegaraan.appendChild(comboitem);

		Common.selectComboItem(true, kewarganegaraan, biodataCalonMahasiswa.getKewarganegaraan());
		row.appendChild(kewarganegaraan);
		kewarganegaraan.setWidth("90%");

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "asalNegara");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Asal Negara" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		asalNegara = new Combobox();
		asalNegara.setReadonly(true);
		row.appendChild(asalNegara);
		asalNegara.setWidth("90%");
		// asalNegara.//setConstraint("no empty");

		class KewarganegaraanEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(asalNegara);
				asalNegara.setSelectedItem(null);
				if (kewarganegaraan.getSelectedItem() == null || kewarganegaraan.getSelectedItem().getValue() == null) {
					return;
				}
				if (kewarganegaraan.getSelectedItem().getValue().equals(ais.database.model.Mahasiswa.WNI)) {

					Common.insertComboDanSemua(asalNegara, new String[] { "namaNegara" }, "kode", Negara.class,
							"== Klik disini untuk pilih ==", Restrictions.eq("namaNegara", "Indonesia"));

				} else {
					Common.insertComboDanSemua(asalNegara, new String[] { "namaNegara" }, "kode", Negara.class,
							"== Klik disini untuk pilih ==", Restrictions.ne("namaNegara", "Indonesia"));
				}

				Common.selectComboItem(true, asalNegara, biodataCalonMahasiswa.getAsalNegara());

			}

		}

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "alamat");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua("Alamat / Jalan / Gang Rumah"
				+ ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(alamat = new Textbox(
				biodataCalonMahasiswa.getAlamat() == null ? "" : biodataCalonMahasiswa.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(3);
		// alamat.//setConstraint("no empty");

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "dusunCalon");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Dusun / Kampung" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(dusunCalon = new Textbox(biodataCalonMahasiswa.getDusunCalon()));
		dusunCalon.setWidth("90%");

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "rt");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"RT" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(rt = new Textbox(biodataCalonMahasiswa.getRt() == null ? "" : biodataCalonMahasiswa.getRt()));
		rt.setWidth("90%");
		rt.setMaxlength(3);
		rt.setCols(3);

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "rw");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"RW" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(rw = new Textbox(biodataCalonMahasiswa.getRw() == null ? "" : biodataCalonMahasiswa.getRw()));
		rw.setWidth("90%");
		rw.setMaxlength(3);
		rw.setCols(3);

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "kodePos");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Kode Pos" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(kodePos = new Textbox(
				biodataCalonMahasiswa.getKodePos() == null ? "" : biodataCalonMahasiswa.getKodePos()));
		kodePos.setWidth("90%");
		kodePos.setMaxlength(8);
		kodePos.setCols(8);

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "kelurahanCalon");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Kelurahan / Desa" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(kelurahanCalon = new Textbox(
				biodataCalonMahasiswa.getKelurahanCalon() == null ? "" : biodataCalonMahasiswa.getKelurahanCalon()));
		kelurahanCalon.setWidth("90%");
		// kelurahanCalon.//setConstraint("no empty");

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "kecamatanCalon");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Kecamatan" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(kecamatanCalon = new AmbilDataKecamatanBanbox());
		kecamatanCalon.setValue(biodataCalonMahasiswa.getKecamatanCalon() == null ? "== Klik disini untuk pilih =="
				: biodataCalonMahasiswa.getKecamatanCalon().getNama());
		kecamatanCalon.setAttribute("wilayah", biodataCalonMahasiswa.getKecamatanCalon());
		kecamatanCalon.setWidth("90%");
		// kecamatanCalon.//setConstraint("no empty");
		if (row.isVisible()) {
			Common.initKeterangan(rows, "* Jika tidak ada Kecamatan yang ditemukan, pilihlah Kecamatan terdekat");
		}

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "propinsiCalon");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Propinsi" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		propinsiCalon = new Label();
		row.appendChild(propinsiCalon);
		propinsiCalon.setWidth("90%");
		propinsiCalon.setAttribute("wilayah", biodataCalonMahasiswa.getPropinsiCalon());

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "kotaCalon");
		Common.createFieldKota(rows,
				"Kota/Kabupaten" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : ""),
				kotaCalon = new Label(), propinsiCalon, biodataCalonMahasiswa.getKotaCalon(),
				(statusWajibIsi.equals(Konfigurasi.AKTIF)) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB))
				.setStyle(row.getStyle());

		kotaCalon.setAttribute("wilayah", biodataCalonMahasiswa.getKotaCalon());

		Common.createKotaPropinsiListenerBerdasarkanKecamatan(propinsiCalon, kotaCalon, kecamatanCalon);

		KewarganegaraanEventListener kewarganegaraanEventListener = new KewarganegaraanEventListener();

		kewarganegaraan.addEventListener("onChange", kewarganegaraanEventListener);
		kewarganegaraanEventListener.onEvent(null);

		row = new MyRowStyled();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "teleponRumah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Telepon (atau HP) / No. WA" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(teleponRumah = new Textbox(
				biodataCalonMahasiswa.getTeleponRumah() == null ? "" : biodataCalonMahasiswa.getTeleponRumah()));
		teleponRumah.setWidth("90%");

		row = new MyRowStyled();
		row.setVisible(tampilkanLabelBesarPadaFormPMB);

		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("II. Data Pendidikan Asal"));

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "jenisSekolah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Jenis Pendidikan Asal" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));

		Common.insertComboDanSemua(jenisSekolah = new Combobox(), new String[] { "nama" }, "keterangan",
				JenisSekolahMahasiswaBaru.class, "== Klik disini untuk pilih ==",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		jenisSekolah.setReadonly(true);
		Common.selectComboItem(true, jenisSekolah, biodataCalonMahasiswa.getJenisSekolah());

		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null
				&& biodataCalonMahasiswa.getJenisSekolah() != null) {
			row.appendChild(new Label(biodataCalonMahasiswa.getJenisSekolah().getNama()));
		} else {
			row.appendChild(jenisSekolah);
		}

		jenisSekolah.setWidth("90%");

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "jurusanSekolah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Jurusan Pendidikan Asal" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		Common.insertComboDanSemua(jurusanSekolah = new Combobox(), new String[] { "nama" }, "keterangan",
				JurusanSekolahMahasiswaBaru.class, "== Klik disini untuk pilih ==",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("jenisSekolahMahasiswaBaru", biodataCalonMahasiswa.getJenisSekolah()));

		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null
				&& biodataCalonMahasiswa.getJurusanSekolah() != null) {
			row.appendChild(new Label(biodataCalonMahasiswa.getJurusanSekolah().getNama()));
		} else {
			row.appendChild(jurusanSekolah);
		}

		jurusanSekolah.setWidth("90%");
		jurusanSekolah.setReadonly(true);

		Common.initKeterangan(rows,
				"Jurusan Pendidikan Asal harus dipilih, pilihan ini untuk menentukan pilihan paket atau prodi yang akan dipilih di bawah.")
				.setStyle(row.getStyle());

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "jurusanSekolahLain");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua("Jika jurusan pendidikan asal tidak ditemukan, ketik disini "
				+ ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));

		row.appendChild(jurusanSekolahLain = new Textbox(biodataCalonMahasiswa.getJurusanSekolahLain()));
		jurusanSekolahLain.setWidth("90%");

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "akreditasiSekolah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Akreditasi Pendidikan Asal" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		akreditasiSekolah = new Radiogroup();
		MyRadioConfig radio = new MyRadioConfig();
		radio.setLabel("A");
		radio.setValue("A");
		akreditasiSekolah.appendChild(radio);
		if (biodataCalonMahasiswa.getAkreditasiSekolah().equalsIgnoreCase("A")) {
			radio.setSelected(true);
		}
		radio = new MyRadioConfig();
		radio.setLabel("B");
		radio.setValue("B");
		akreditasiSekolah.appendChild(radio);
		if (biodataCalonMahasiswa.getAkreditasiSekolah().equalsIgnoreCase("B")) {
			radio.setSelected(true);
		}
		radio = new MyRadioConfig();
		radio.setLabel("C");
		radio.setValue("C");
		akreditasiSekolah.appendChild(radio);
		if (biodataCalonMahasiswa.getAkreditasiSekolah().equalsIgnoreCase("C")) {
			radio.setSelected(true);
		}
		radio = new MyRadioConfig();
		radio.setLabel("D");
		radio.setValue("D");
		akreditasiSekolah.appendChild(radio);
		if (biodataCalonMahasiswa.getAkreditasiSekolah().equalsIgnoreCase("D")) {
			radio.setSelected(true);
		}
		radio = new MyRadioConfig();
		radio.setLabel("E");
		radio.setValue("E");
		akreditasiSekolah.appendChild(radio);
		if (biodataCalonMahasiswa.getAkreditasiSekolah().equalsIgnoreCase("E")) {
			radio.setSelected(true);
		}
		row.appendChild(akreditasiSekolah);

		row = new MyRowStyled();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "asalSma");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Nama Pendidikan Asal" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(asalSma = new AmbilDataNamaSekolahBanbox(biodataCalonMahasiswa.getNamaSekolahAsal() == null
				|| biodataCalonMahasiswa.getNamaSekolahAsal().getNama().trim().isEmpty()
						? "== Klik disini untuk pilih =="
						: biodataCalonMahasiswa.getNamaSekolahAsal().getNama()));
		asalSma.setAttribute("namaSekolahAsal", biodataCalonMahasiswa.getNamaSekolahAsal());
		asalSma.setWidth("90%");

		if (row.isVisible()) {
			Common.initKeterangan(rows,
					"Jika Asal pendidikan sebelumnya tidak ditemukan, buat baru dan masukkan nama instansi pendikan secara lengkap");
		}

		row = new MyRowStyled();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "alamatAsalSma");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Alamat Pendidikan Asal" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(alamatAsalSma = new Textbox(
				biodataCalonMahasiswa.getAlamatAsalSma() == null ? "" : biodataCalonMahasiswa.getAlamatAsalSma()));
		alamatAsalSma.setWidth("90%");
		alamatAsalSma.setRows(2);

		row = new MyRowStyled();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "kodePosSekolah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Kode Pos Pendidikan Asal" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(kodePosSekolah = new Textbox(
				biodataCalonMahasiswa.getKodePosSekolah() == null ? "" : biodataCalonMahasiswa.getKodePosSekolah()));
		kodePosSekolah.setWidth("90%");
		kodePosSekolah.setMaxlength(8);
		kodePosSekolah.setCols(8);
		// kodePosSekolah//.setConstraint("none");

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "kecamatanSekolah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Kecamatan Pendidikan Asal" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(kecamatanSekolah = new AmbilDataKecamatanBanbox());
		kecamatanSekolah.setValue(biodataCalonMahasiswa.getKecamatanSekolah() == null ? "== Klik disini untuk pilih =="
				: biodataCalonMahasiswa.getKecamatanSekolah().getNama());
		kecamatanSekolah.setAttribute("wilayah", biodataCalonMahasiswa.getKecamatanSekolah());
		kecamatanSekolah.setWidth("90%");

		if (row.isVisible()) {
			Common.initKeterangan(rows, "* Jika tidak ada Kecamatan yang ditemukan, pilihlah Kecamatan terdekat");
		}

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "propinsiSekolah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Propinsi Pendidikan Asal" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		propinsiSekolah = new Label();
		row.appendChild(propinsiSekolah);
		propinsiSekolah.setWidth("90%");

		propinsiSekolah.setAttribute("wilayah", biodataCalonMahasiswa.getPropinsiSekolah());

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "kotaSekolah");
		Common.createFieldKota(rows,
				"Kota/Kabupaten Pendidikan Asal"
						+ ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : ""),
				kotaSekolah = new Label(), propinsiSekolah, biodataCalonMahasiswa.getKotaSekolah(),
				(statusWajibIsi.equals(Konfigurasi.AKTIF)) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB));

		kotaSekolah.setAttribute("wilayah", biodataCalonMahasiswa.getKotaSekolah());

		Common.createKotaPropinsiListenerBerdasarkanKecamatan(propinsiSekolah, kotaSekolah, kecamatanSekolah);

		row = new MyRowStyled();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "noTelpSekolah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"No. Telpon Pendidikan Asal" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(noTelpSekolah = new Textbox(biodataCalonMahasiswa.getNoTelpSekolah()));
		noTelpSekolah.setWidth("90%");

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "tahunKelulusan");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Tahun Kelulusan" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		tahunKelulusan = new Combobox();
		tahunKelulusan.setReadonly(true);
		row.appendChild(tahunKelulusan);
		tahunKelulusan.setWidth("90%");

		EventListener gelombangPendaftaranEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(tahunKelulusan);

				GelombangPendaftaran myGelombangPendaftaran = (GelombangPendaftaran) (gelombangPendaftaran
						.getSelectedItem() == null ? null : gelombangPendaftaran.getSelectedItem().getValue());

				Common.clear(jenisSeleksi);
				List<JenisSeleksi> temp = myGelombangPendaftaran == null ? new ArrayList<JenisSeleksi>()
						: myGelombangPendaftaran.ambilJenisSeleksi();
				List<JenisSeleksi> jenisSeleksis;
				if (arg0 != null && arg0.getTarget() == kelompokJenisSeleksi) {

					KelompokJenisSeleksi pilih = (KelompokJenisSeleksi) (kelompokJenisSeleksi.getSelectedItem() == null
							? null
							: kelompokJenisSeleksi.getSelectedItem().getValue());

					if (pilih == null) {
						jenisSeleksis = temp;
					} else {
						jenisSeleksis = new ArrayList<JenisSeleksi>();
						for (JenisSeleksi jenisSeleksi : temp) {
							if (jenisSeleksi.getKelompokJenisSeleksi() != null
									&& jenisSeleksi.getKelompokJenisSeleksi().getId().equals(pilih.getId())) {
								jenisSeleksis.add(jenisSeleksi);
							}
						}
					}
					Collections.sort(jenisSeleksis);
				} else {
					jenisSeleksis = temp;
					Collections.sort(jenisSeleksis);
					List<KelompokJenisSeleksi> kelompokJenisSeleksis = new ArrayList<KelompokJenisSeleksi>();
					for (JenisSeleksi jenisSeleksi : jenisSeleksis) {
						if (jenisSeleksi != null && jenisSeleksi.getKelompokJenisSeleksi() != null
								&& !kelompokJenisSeleksis.contains(jenisSeleksi.getKelompokJenisSeleksi())) {
							kelompokJenisSeleksis.add(jenisSeleksi.getKelompokJenisSeleksi());
						}
					}

					Collections.sort(kelompokJenisSeleksis);

					Common.clear(kelompokJenisSeleksi);
					kelompokJenisSeleksi.getParent().setVisible(!kelompokJenisSeleksis.isEmpty());
					kelompokJenisSeleksi.setVisible(!kelompokJenisSeleksis.isEmpty());

					Common.insertComboItems(kelompokJenisSeleksi, "nama", "keterangan", kelompokJenisSeleksis);

					MyComboitemConfig comboitem = new MyComboitemConfig();
					comboitem.setLabel("== Klik disini untuk pilih ==");
					comboitem.setValue(null);
					kelompokJenisSeleksi.appendChild(comboitem);

					Common.selectComboItem(true, kelompokJenisSeleksi, biodataCalonMahasiswa.getKelompokJenisSeleksi());

				}

				Common.insertComboItems(jenisSeleksi, "nama", "deskripsi", jenisSeleksis);

				MyComboitemConfig comboitem = new MyComboitemConfig();
				comboitem.setLabel("== Klik disini untuk pilih ==");
				comboitem.setValue(null);
				jenisSeleksi.appendChild(comboitem);

				Common.selectComboItem(true, jenisSeleksi,
						biodataCalonMahasiswa.getJenisSeleksi() == null
								? (myGelombangPendaftaran == null ? null : myGelombangPendaftaran.getJenisSeleksi())
								: biodataCalonMahasiswa.getJenisSeleksi());

				if (myGelombangPendaftaran == null) {

					int jumlahMaksTahunKelulusanKebelakang = 50;
					try {
						jumlahMaksTahunKelulusanKebelakang = Integer.parseInt(Common
								.getKonfigurasi("jumlah_maks_tahun_kelulusan_kebelakang", "50").getNilai().trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:2422");
					}
					Integer tahunCurrent = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
					for (int i = tahunCurrent - jumlahMaksTahunKelulusanKebelakang; i <= (tahunCurrent + 1); i++) {
						comboitem = new MyComboitemConfig();
						comboitem.setLabel(i + "");
						comboitem.setValue(i + "");
						tahunKelulusan.appendChild(comboitem);
					}
				} else {

					List<Paket> paketsIds = ConstantValues.simpleList(
							HibernateUtil.currentSession().createCriteria(PaketPunyaGelombangPendaftaran.class)
									.add(Restrictions.eq("gelombangPendaftaran", myGelombangPendaftaran))
									.setProjection(Projections.groupProperty("paket.id")),
							Paket.class, false);

					System.out.println("paketsIds -> " + paketsIds);

					if (paketsIds.isEmpty()) {
						Common.insertComboDanSemua(jenisSekolah, new String[] { "nama" }, "keterangan",
								JenisSekolahMahasiswaBaru.class, "== Klik disini untuk pilih ==",
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
						Common.selectComboItem(true, jenisSekolah, biodataCalonMahasiswa.getJenisSekolah());

					} else {

						Map<Long, PilihanPaketPerJurusanMhsBaru> jenisSekolahMahasiswaBarus = ConstantValues
								.ambilBerdasarClass(PilihanPaketPerJurusanMhsBaru.class);

						TreeMap<Long, JenisSekolahMahasiswaBaru> a = new TreeMap<Long, JenisSekolahMahasiswaBaru>();
						for (Paket paket : paketsIds) {
							for (PilihanPaketPerJurusanMhsBaru baru : jenisSekolahMahasiswaBarus.values()) {

								if (baru != null && baru.getPaket() != null && paket != null
										&& paket.getId().equals(baru.getPaket().getId())) {
									a.put(baru.getJurusanSekolahMahasiswaBaru().getJenisSekolahMahasiswaBaru().getId(),
											baru.getJurusanSekolahMahasiswaBaru().getJenisSekolahMahasiswaBaru());
								}

							}
						}
						List<JenisSekolahMahasiswaBaru> jenisSekolahs = new ArrayList<JenisSekolahMahasiswaBaru>(
								a.values());

						System.out.println("jenisSekolahs -> " + jenisSekolahs);

						jenisSekolahs.add(null);
						Common.insertComboItems(jenisSekolah, new String[] { "nama" }, "keterangan", jenisSekolahs,
								"== Klik disini untuk pilih ==");
						jenisSekolah.setReadonly(true);
						Common.selectComboItem(true, jenisSekolah, biodataCalonMahasiswa.getJenisSekolah());

						a = null;
						jenisSekolahs = null;
					}

					if (biodataCalonMahasiswa.getJenisSekolah() == null && jenisSekolah.getChildren().size() == 2) {
						jenisSekolah.setSelectedIndex(0);
					}

					paketsIds.clear();
					paketsIds = null;

					for (int i = myGelombangPendaftaran.getTahunAngkatanMinimal(); i <= myGelombangPendaftaran
							.getTahunAngkatanMaksimal(); i++) {
						comboitem = new MyComboitemConfig();
						comboitem.setLabel(i + "");
						comboitem.setValue(i + "");
						tahunKelulusan.appendChild(comboitem);
					}
				}

				Common.selectComboItem(true, tahunKelulusan, biodataCalonMahasiswa.getTahunKelulusan());

				if (myGelombangPendaftaran != null) {
					if (biodataCalonMahasiswa.getId() == null && myGelombangPendaftaran.getProgram() != null) {
						Common.selectComboItem(true, program, myGelombangPendaftaran.getProgram());
					}

					rowFoto.setVisible(myGelombangPendaftaran.getTampilkanUploadFoto());

					program.setDisabled(myGelombangPendaftaran.getTidakBolehMemilihProgramLain());

					rowPindahan1.setVisible(myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar());
					rowPindahan2.setVisible(myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar());
					if (!myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar()) {
						merupakanPindahan.setChecked(myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar());

					}
					rowpindahanDari.setVisible(myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar()
							&& merupakanPindahan.isChecked());
					rowpindahanDariKampus.setVisible(myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar()
							&& merupakanPindahan.isChecked());
					rownimPindahan.setVisible(myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar()
							&& merupakanPindahan.isChecked());
					rowpindahDariKampusLamaDiSemester
							.setVisible(myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar()
									&& merupakanPindahan.isChecked());
					rowketeranganPindah.setVisible(myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar()
							&& merupakanPindahan.isChecked());
					rowpindahanDariProdi.setVisible(myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar()
							&& merupakanPindahan.isChecked());
				}
			}
		};

		gelombangPendaftaran.addEventListener("onChange", gelombangPendaftaranEventListener);
		tahunAkademik.addEventListener("onChange", gelombangPendaftaranEventListener);
		kelompokJenisSeleksi.addEventListener("onChange", gelombangPendaftaranEventListener);

		row = new MyRowStyled();
		row.setVisible(tampilkanLabelBesarPadaFormPMB);

		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("III. Data Orang Tua/Wali"));

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "namaAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Nama Ayah Kandung" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(namaAyah = new Textbox(
				biodataCalonMahasiswa.getNamaAyah() == null ? "" : biodataCalonMahasiswa.getNamaAyah()));
		namaAyah.setWidth("90%");

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "namaIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Nama Ibu Kandung" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(namaIbu = new Textbox(
				biodataCalonMahasiswa.getNamaIbu() == null ? "" : biodataCalonMahasiswa.getNamaIbu()));
		namaIbu.setWidth("90%");
		namaIbu.addEventListener("onChange", checkKesamaan);
		// namaIbu.setConstraint("no empty");

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "namaWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Nama Wali" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(namaWali = new Textbox(
				biodataCalonMahasiswa.getNamaWali() == null ? "" : biodataCalonMahasiswa.getNamaWali()));
		namaWali.setWidth("90%");
		// namaWali//.setConstraint("none");

		row = new MyRowStyled();
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(""));
		row.appendChild(new MyLabelConfigTitikDua("* Jika tidak ada, isi nama ayah"));

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "alamatOrtu");
		alamatSama = new MyCheckboxConfig();
		alamatSama.setChecked(biodataCalonMahasiswa.getAlamatSama());
		alamatOrtu = new Textbox(biodataCalonMahasiswa.getAlamatOrtu() == null ? "" : biodataCalonMahasiswa.getAlamatOrtu());
		alamatOrtu.setWidth("90%");
		alamatOrtu.setRows(3);
		if (statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) {
			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new MyLabelConfigTitikDua("Sama dengan alamat pendaftar"));
			row.appendChild(alamatSama);

			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new MyLabelConfigTitikDua(
					"Alamat Orang Tua/Wali" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
			row.appendChild(alamatOrtu);
		}

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "rtOrtu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"RT Orang Tua/Wali" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(rtOrtu = new Textbox(
				biodataCalonMahasiswa.getRtOrtu() == null ? "" : biodataCalonMahasiswa.getRtOrtu()));
		rtOrtu.setWidth("90%");
		rtOrtu.setMaxlength(3);
		rtOrtu.setCols(3);

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "rwOrtu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"RW Orang Tua/Wali" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(rwOrtu = new Textbox(
				biodataCalonMahasiswa.getRwOrtu() == null ? "" : biodataCalonMahasiswa.getRwOrtu()));
		rwOrtu.setWidth("90%");
		rwOrtu.setMaxlength(3);
		rwOrtu.setCols(3);

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "kodePosOrtu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Kode Pos Orang Tua/Wali" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(kodePosOrtu = new Textbox(
				biodataCalonMahasiswa.getKodePosOrtu() == null ? "" : biodataCalonMahasiswa.getKodePosOrtu()));
		kodePosOrtu.setWidth("90%");
		kodePosOrtu.setMaxlength(8);
		kodePosOrtu.setCols(8);

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "kelurahanOrtu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Kelurahan Orang Tua/Wali" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(kelurahanOrtu = new Textbox(
				biodataCalonMahasiswa.getKelurahanOrtu() == null ? "" : biodataCalonMahasiswa.getKelurahanOrtu()));
		kelurahanOrtu.setWidth("90%");

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "kecamatanOrtu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Kecamatan Orang Tua/Wali" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(kecamatanOrtu = new AmbilDataKecamatanBanbox());
		kecamatanOrtu.setValue(biodataCalonMahasiswa.getKecamatanOrtu() == null ? "== Klik disini untuk pilih =="
				: biodataCalonMahasiswa.getKecamatanOrtu().getNama());
		kecamatanOrtu.setAttribute("wilayah", biodataCalonMahasiswa.getKecamatanOrtu());
		kecamatanOrtu.setWidth("90%");

		if (row.isVisible()) {
			Common.initKeterangan(rows, "* Jika tidak ada Kecamatan yang ditemukan, pilihlah Kecamatan terdekat");
		}

		row = new MyRowStyled();

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "propinsiOrtu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Propinsi Orang Tua/Wali" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(propinsiOrtu = new Label());
		propinsiOrtu.setWidth("90%");

		propinsiOrtu.setAttribute("wilayah", biodataCalonMahasiswa.getPropinsiOrtu());

		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "kotaOrtu");
		Common.createFieldKota(rows,
				"Kabupaten/Kota Orang Tua/Wali"
						+ ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : ""),
				kotaOrtu = new Label(), propinsiOrtu, biodataCalonMahasiswa.getKotaOrtu(),
				(statusWajibIsi.equals(Konfigurasi.AKTIF)) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB));

		kotaOrtu.setAttribute("wilayah", biodataCalonMahasiswa.getKotaOrtu());

		Common.createKotaPropinsiListenerBerdasarkanKecamatan(propinsiOrtu, kotaOrtu, kecamatanOrtu);

		row = new MyRowStyled();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "noTelpOrtu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua("Telepon (atau HP) / No. WA Orang Tua/Wali"
				+ ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		row.appendChild(noTelpOrtu = new Textbox(
				biodataCalonMahasiswa.getNoTelpOrtu() == null ? "" : biodataCalonMahasiswa.getNoTelpOrtu()));
		noTelpOrtu.setWidth("90%");
		noTelpOrtu.setMaxlength(12);
		noTelpOrtu.setCols(12);

		row = new MyRowStyled();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "pendidikanOrtu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Pendidikan Ayah" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		Common.insertComboDanSemua(pendidikanOrtu = new Combobox(), new String[] { "nama" }, "keterangan",
				PendidikanOrangTua.class, "== Klik disini untuk pilih ==");
		Common.selectComboItem(true, pendidikanOrtu, biodataCalonMahasiswa.getPendidikanOrtu());
		row.appendChild(pendidikanOrtu);
		pendidikanOrtu.setWidth("90%");
		pendidikanOrtu.setReadonly(true);

		row = new MyRowStyled();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "pekerjaanAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Pekerjaan Ayah" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		Common.insertComboDanSemua(pekerjaanAyah = new Combobox(), new String[] { "nama" }, "keterangan",
				PekerjaanOrangTua.class, "== Klik disini untuk pilih ==",
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(true, pekerjaanAyah, biodataCalonMahasiswa.getPekerjaanAyah());
		row.appendChild(pekerjaanAyah);
		pekerjaanAyah.setWidth("90%");
		pekerjaanAyah.setReadonly(true);

		row = new MyRowStyled();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "pendapatanOrtu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Pendapatan Ayah" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		pendapatanOrtu = new Combobox();
		pendapatanOrtu.setReadonly(true);

		List<PendapatanOrangTua> pendapatanOrangTuas = ConstantValues
				.simpleList(
						HibernateUtil.currentSession().createCriteria(PendapatanOrangTua.class)
								.addOrder(Order.asc("mulaiDari")).addOrder(Order.asc("sampai")),
						PendapatanOrangTua.class);

		for (PendapatanOrangTua o : pendapatanOrangTuas) {
			MyComboitemConfig comboitems = new MyComboitemConfig();
			comboitems.setLabel("Rp. " + Common.numberFormat.get().format(o.getMulaiDari()) + " - Rp. "
					+ Common.numberFormat.get().format(o.getSampai()));
			comboitems.setValue(o);
			pendapatanOrtu.appendChild(comboitems);
		}
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("== Klik disini untuk pilih ==");
		comboitem.setValue(null);
		pendapatanOrtu.appendChild(comboitem);
		Common.selectComboItem(true, pendapatanOrtu, biodataCalonMahasiswa.getPendapatanOrtu());
		row.appendChild(pendapatanOrtu);
		pendapatanOrtu.setWidth("90%");

		row = new MyRowStyled();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "pendidikanOrtuIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Pendidikan Ibu" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		Common.insertComboDanSemua(pendidikanOrtuIbu = new Combobox(), new String[] { "nama" }, "keterangan",
				PendidikanOrangTua.class, "== Klik disini untuk pilih ==");
		Common.selectComboItem(true, pendidikanOrtuIbu, biodataCalonMahasiswa.getPendidikanOrtuIbu());
		row.appendChild(pendidikanOrtuIbu);
		pendidikanOrtuIbu.setWidth("90%");
		pendidikanOrtuIbu.setReadonly(true);

		row = new MyRowStyled();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "pekerjaanAyahIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Pekerjaan Ibu" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		Common.insertComboDanSemua(pekerjaanAyahIbu = new Combobox(), new String[] { "nama" }, "keterangan",
				PekerjaanOrangTua.class, "== Klik disini untuk pilih ==",
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(true, pekerjaanAyahIbu, biodataCalonMahasiswa.getPekerjaanAyahIbu());
		row.appendChild(pekerjaanAyahIbu);
		pekerjaanAyahIbu.setWidth("90%");
		pekerjaanAyahIbu.setReadonly(true);

		row = new MyRowStyled();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "pendapatanOrtuIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Pendapatan Ibu" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		pendapatanOrtuIbu = new Combobox();
		pendapatanOrtuIbu.setReadonly(true);
		for (PendapatanOrangTua o : pendapatanOrangTuas) {
			MyComboitemConfig comboitems = new MyComboitemConfig();
			comboitems.setLabel("Rp. " + Common.numberFormat.get().format(o.getMulaiDari()) + " - Rp. "
					+ Common.numberFormat.get().format(o.getSampai()));
			comboitems.setValue(o);
			pendapatanOrtuIbu.appendChild(comboitems);
		}
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("== Klik disini untuk pilih ==");
		comboitem.setValue(null);
		pendapatanOrtuIbu.appendChild(comboitem);
		Common.selectComboItem(true, pendapatanOrtuIbu, biodataCalonMahasiswa.getPendapatanOrtuIbu());
		row.appendChild(pendapatanOrtuIbu);
		pendapatanOrtuIbu.setWidth("90%");

		row = new MyRowStyled();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "pendidikanOrtuWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Pendidikan Wali" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		Common.insertComboDanSemua(pendidikanOrtuWali = new Combobox(), new String[] { "nama" }, "keterangan",
				PendidikanOrangTua.class, "== Klik disini untuk pilih ==");
		Common.selectComboItem(true, pendidikanOrtuWali, biodataCalonMahasiswa.getPendidikanOrtuWali());
		row.appendChild(pendidikanOrtuWali);
		pendidikanOrtuWali.setWidth("90%");
		pendidikanOrtuWali.setReadonly(true);

		row = new MyRowStyled();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "pekerjaanAyahWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Pekerjaan Wali" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		Common.insertComboDanSemua(pekerjaanAyahWali = new Combobox(), new String[] { "nama" }, "keterangan",
				PekerjaanOrangTua.class, "== Klik disini untuk pilih ==",
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(true, pekerjaanAyahWali, biodataCalonMahasiswa.getPekerjaanAyahWali());
		row.appendChild(pekerjaanAyahWali);
		pekerjaanAyahWali.setWidth("90%");
		pekerjaanAyahWali.setReadonly(true);

		row = new MyRowStyled();
		statusWajibIsi = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(tbmuser, "pendapatanOrtuWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new MyLabelConfigTitikDua(
				"Pendapatan Wali" + ((statusWajibIsi.equals(Konfigurasi.AKTIF) && berbintang) ? " *" : "")));
		pendapatanOrtuWali = new Combobox();
		pendapatanOrtuWali.setReadonly(true);
		for (PendapatanOrangTua o : pendapatanOrangTuas) {
			MyComboitemConfig comboitems = new MyComboitemConfig();
			comboitems.setLabel("Rp. " + Common.numberFormat.get().format(o.getMulaiDari()) + " - Rp. "
					+ Common.numberFormat.get().format(o.getSampai()));
			comboitems.setValue(o);
			pendapatanOrtuWali.appendChild(comboitems);
		}
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("== Klik disini untuk pilih ==");
		comboitem.setValue(null);
		pendapatanOrtuWali.appendChild(comboitem);
		Common.selectComboItem(true, pendapatanOrtuWali, biodataCalonMahasiswa.getPendapatanOrtuWali());
		row.appendChild(pendapatanOrtuWali);
		pendapatanOrtuWali.setWidth("90%");

		class AlamatSamaEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub

				if (alamatSama.isChecked()) {
					alamatOrtu.setValue(alamat.getValue());
					rtOrtu.setValue(rt.getValue());
					rwOrtu.setValue(rw.getValue());
					kodePosOrtu.setValue(kodePos.getValue());
					kelurahanOrtu.setValue(kelurahanCalon.getValue());
					kecamatanOrtu.setValue(kecamatanCalon.getValue());
					kecamatanOrtu.setAttribute("wilayah", kecamatanCalon.getAttribute("wilayah"));

					propinsiOrtu.setValue(propinsiCalon.getValue());

					Kota kota = (Kota) kotaCalon.getAttribute("wilayah");
					kotaOrtu.setValue(kota == null ? "" : kota.getNama());
					kotaOrtu.setAttribute("wilayah", kota);

					Propinsi propinsi = (Propinsi) propinsiCalon.getAttribute("wilayah");
					propinsiOrtu.setValue(propinsi == null ? "" : propinsi.getNama());
					propinsiOrtu.setAttribute("wilayah", propinsi);

					alamatOrtu.setDisabled(true);
					rtOrtu.setDisabled(true);
					rwOrtu.setDisabled(true);
					kodePosOrtu.setDisabled(true);
					kelurahanOrtu.setDisabled(true);
					kecamatanOrtu.setDisabled(true);
//					propinsiOrtu.setDisabled(true);
//					kotaOrtu.setDisabled(true);
				} else {
					alamatOrtu.setDisabled(false);
					rtOrtu.setDisabled(false);
					rwOrtu.setDisabled(false);
					kodePosOrtu.setDisabled(false);
					kelurahanOrtu.setDisabled(false);
					kecamatanOrtu.setDisabled(false);
//					propinsiOrtu.setDisabled(false);
//					kotaOrtu.setDisabled(false);
				}
			}

		}

		AlamatSamaEventListener alamatSamaEventListener = new AlamatSamaEventListener();
		alamatSama.addEventListener("onCheck", alamatSamaEventListener);
		alamatSamaEventListener.onEvent(null);

		boolean tampilkan_pilihan_paket = Common.bolehKonfigurasi("tampilkan_pilihan_paket");

		rowPaket = new MyRowStyled();
		rowPaket.setVisible(tampilkanLabelBesarPadaFormPMB && tampilkan_pilihan_paket);

		rowPaket.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(rowPaket, "2");
		rowPaket.appendChild(new MyLabelStyled("IV. Pilihan Paket"));

		row = new MyRowStyled();
		row.setVisible(!tampilSederhana && tampilkan_pilihan_paket);

		row.setParent(rows);
		row.appendChild(new MyLabelConfigTitikDua("Pilihan Paket  *"));
		paket = new Combobox();
		paket.setReadonly(true);

		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null && biodataCalonMahasiswa.getPaket() != null) {
			row.appendChild(new Label(biodataCalonMahasiswa.getPaket().getNama()));
		} else {
			row.appendChild(paket);
		}
		paket.setWidth("90%");

		final EventListener eventListenerProdi1 = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (Common.getKonfigurasi("tampil_konsentrasi_calon_mahasiswa", Konfigurasi.TIDAK_AKTIF).getNilai()
						.trim().equalsIgnoreCase(Konfigurasi.AKTIF)) {
					Jurusan jurusan = (Jurusan) (prodi1.getSelectedItem() == null ? null
							: prodi1.getSelectedItem().getValue());

					Common.insertCombo(konsentrasi, new String[] { "nama" }, "namaEnglish", Konsentrasi.class,
							Restrictions.eq("jurusan", jurusan), Restrictions.sqlRestriction("true"));

					rowkonsentrasi.setVisible(!konsentrasi.getChildren().isEmpty());

					MyComboitemConfig comboitem = new MyComboitemConfig();
					comboitem.setLabel("== Klik disini untuk pilih ==");
					comboitem.setValue(null);
					konsentrasi.appendChild(comboitem);

					Common.selectComboItem(konsentrasi, biodataCalonMahasiswa.getKonsentrasi());
					konsentrasi.setWidth("90%");
					konsentrasi.setReadonly(true);
				}
				Paket _ppP1 = myPaket != null && myPaket.getId() != null ? myPaket
						: (Paket) (paket.getSelectedItem() == null ? null : paket.getSelectedItem().getValue());
				if (_ppP1 != null && !tampilSederhana && _ppP1.getJumlahProdiYgBolehDiambil() > 1) {
					boolean p1Filled = prodi1.getSelectedItem() != null && prodi1.getSelectedItem().getValue() != null;
					rowprodi2.setVisible(p1Filled);
					if (!p1Filled) {
						prodi2.setSelectedItem(null);
						rowprodi3.setVisible(false); prodi3.setSelectedItem(null);
						rowprodi4.setVisible(false); prodi4.setSelectedItem(null);
						rowprodi5.setVisible(false); prodi5.setSelectedItem(null);
						rowkonsentrasi.setVisible(false);
					}
				}
			}
		};

		final MyLabelStyled dPilhanProdi = new MyLabelStyled("V. Pilihan Program Studi (Prodi)");
		final MyLabelStyled pilihanPindahan = new MyLabelStyled("VI. Pilihan Pindahan (Untuk Mahasiswa Pindahan)");

		class Prodi1EventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {

				try {

					// TODO Auto-generated method stub
					Common.clear(prodi1);
					Common.clear(prodi2);
					Common.clear(prodi3);
					Common.clear(prodi4);
					Common.clear(prodi5);

					Common.clear(program);

					prodi1.setSelectedItem(null);
					prodi2.setSelectedItem(null);
					prodi3.setSelectedItem(null);
					prodi4.setSelectedItem(null);
					prodi5.setSelectedItem(null);

					Paket pp = myPaket != null && myPaket.getId() != null ? myPaket
							: (Paket) (paket.getSelectedItem() == null ? null : paket.getSelectedItem().getValue());

					System.out.println("Pilihan paket -> " + pp);

					if (pp == null) {
						return;
					}

					Session session = HibernateUtil.currentSession();
					List<Program> programs = session.createCriteria(PaketPunyaProgram.class)
							.setProjection(Projections.groupProperty("program")).add(Restrictions.eq("paket", pp))
							.list();
					if (programs.isEmpty()) {
						programs = session.createCriteria(Program.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.list();
						for (Program program : programs) {
							PaketPunyaProgram paketPunyaProgram = new PaketPunyaProgram();
							paketPunyaProgram.setProgram(program);
							paketPunyaProgram.setPaket(pp);
							session.save(paketPunyaProgram);
						}
					}

					program.setReadonly(true);
					program.setSelectedItem(null);
					for (Program strProgram : programs) {
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(strProgram.getNamaBaru());
						comboitem.setValue(strProgram.getNama());
						program.appendChild(comboitem);
						if (strProgram.getNama().equals("Reguler")) {
							program.setSelectedItem(comboitem);
						}
					}

					MyComboitemConfig comboitem = new MyComboitemConfig();
					comboitem.setLabel("== Klik disini untuk pilih ==");
					comboitem.setValue(null);
					program.appendChild(comboitem);
					Common.selectComboItem(true, program,
							biodataCalonMahasiswa.getId() == null ? null : biodataCalonMahasiswa.getProgram());

					GelombangPendaftaran myGelombangPendaftaran = mygelombangPendaftaran != null
							? mygelombangPendaftaran
							: (GelombangPendaftaran) (gelombangPendaftaran.getSelectedItem() == null ? null
									: gelombangPendaftaran.getSelectedItem().getValue());
					if (myGelombangPendaftaran != null) {
						if (biodataCalonMahasiswa.getId() == null && myGelombangPendaftaran.getProgram() != null) {
							Common.selectComboItem(true, program, myGelombangPendaftaran.getProgram());
						}
						program.setDisabled(myGelombangPendaftaran.getTidakBolehMemilihProgramLain());

						rowPindahan1.setVisible(myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar());
						rowPindahan2.setVisible(myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar());
						if (!myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar()) {
							merupakanPindahan.setChecked(myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar());

						}
						rowpindahanDari.setVisible(myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar()
								&& merupakanPindahan.isChecked());
						rowpindahanDariKampus.setVisible(myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar()
								&& merupakanPindahan.isChecked());
						rownimPindahan.setVisible(myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar()
								&& merupakanPindahan.isChecked());
						rowpindahDariKampusLamaDiSemester
								.setVisible(myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar()
										&& merupakanPindahan.isChecked());
						rowketeranganPindah.setVisible(myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar()
								&& merupakanPindahan.isChecked());
						rowpindahanDariProdi.setVisible(myGelombangPendaftaran.getMahasiswaPindahanBolehMendaftar()
								&& merupakanPindahan.isChecked());
					}

					rowprodi1.setVisible(!tampilSederhana && pp.getJumlahProdiYgBolehDiambil() > 0);
					rowprodi2.setVisible(false);
					rowprodi3.setVisible(false);
					rowprodi4.setVisible(false);
					rowprodi5.setVisible(false);

					if (biodataCalonMahasiswa.getProdi1() != null && tbmuser != null
							&& tbmuser.getBiodataCalonMahasiswa() != null) {
						rowprodi1.setVisible(true);
					}
					if (biodataCalonMahasiswa.getProdi2() != null && tbmuser != null
							&& tbmuser.getBiodataCalonMahasiswa() != null) {
						rowprodi2.setVisible(true);
					}
					if (biodataCalonMahasiswa.getProdi3() != null && tbmuser != null
							&& tbmuser.getBiodataCalonMahasiswa() != null) {
						rowprodi3.setVisible(true);
					}
					if (biodataCalonMahasiswa.getProdi4() != null && tbmuser != null
							&& tbmuser.getBiodataCalonMahasiswa() != null) {
						rowprodi4.setVisible(true);
					}
					if (biodataCalonMahasiswa.getProdi5() != null && tbmuser != null
							&& tbmuser.getBiodataCalonMahasiswa() != null) {
						rowprodi5.setVisible(true);
					}
					if (!tampilSederhana && pp.getJumlahProdiYgBolehDiambil() > 1 && biodataCalonMahasiswa.getProdi1() != null) {
						rowprodi2.setVisible(true);
					}
					if (!tampilSederhana && pp.getJumlahProdiYgBolehDiambil() > 2 && biodataCalonMahasiswa.getProdi2() != null) {
						rowprodi3.setVisible(true);
					}
					if (!tampilSederhana && pp.getJumlahProdiYgBolehDiambil() > 3 && biodataCalonMahasiswa.getProdi3() != null) {
						rowprodi4.setVisible(true);
					}
					if (!tampilSederhana && pp.getJumlahProdiYgBolehDiambil() > 4 && biodataCalonMahasiswa.getProdi4() != null) {
						rowprodi5.setVisible(true);
					}

					List<PaketJurusanPmb> paketJurusanPmb = ConstantValues.simpleList(session
							.createCriteria(PaketJurusanPmb.class).add(Restrictions.eq("paket", pp))
							.createAlias("jurusan", "jurusan").add(Restrictions.or(Restrictions.isNull("jurusan.aktif"),
									Restrictions.eq("jurusan.aktif", true))),
							PaketJurusanPmb.class);

					System.out.println("Pilihan paketJurusanPmb -> " + paketJurusanPmb.size());

					if (paketJurusanPmb.isEmpty()) {
						MyMessageboxConfig.show(
								"Prodi untuk paket " + pp
										+ " tidak ditemuan.\nHarap menghubungi panitian penerimaan mahasiswa baru.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					String jk = (String) (jenisKelamin.getSelectedItem() == null ? "Semua"
							: jenisKelamin.getSelectedItem().getValue());

					comboitem = new MyComboitemConfig();
					comboitem.setLabel("== Klik disini untuk pilih ==");
					comboitem.setValue(null);
					prodi1.appendChild(comboitem);

					comboitem = new MyComboitemConfig();
					comboitem.setLabel("== Klik disini untuk pilih ==");
					comboitem.setValue(null);
					prodi2.appendChild(comboitem);

					comboitem = new MyComboitemConfig();
					comboitem.setLabel("== Klik disini untuk pilih ==");
					comboitem.setValue(null);
					prodi3.appendChild(comboitem);

					comboitem = new MyComboitemConfig();
					comboitem.setLabel("== Klik disini untuk pilih ==");
					comboitem.setValue(null);
					prodi4.appendChild(comboitem);

					comboitem = new MyComboitemConfig();
					comboitem.setLabel("== Klik disini untuk pilih ==");
					comboitem.setValue(null);
					prodi5.appendChild(comboitem);

					for (PaketJurusanPmb p : paketJurusanPmb) {
						if (p.getPilihan1()
								&& (p.getKelamin().equalsIgnoreCase("Semua") || p.getKelamin().equalsIgnoreCase(jk))) {
							comboitem = new MyComboitemConfig();
							comboitem.setValue(p.getJurusan());
							comboitem.setLabel(p.getJurusan().getNama());
							comboitem.setDescription(
									p.getJurusan().getFakultas() + " - " + p.getJurusan().getJenjang().getNama());
							prodi1.appendChild(comboitem);
						}

						if (p.getPilihan2()
								&& (p.getKelamin().equalsIgnoreCase("Semua") || p.getKelamin().equalsIgnoreCase(jk))) {
							comboitem = new MyComboitemConfig();
							comboitem.setValue(p.getJurusan());
							comboitem.setLabel(p.getJurusan().getNama());
							comboitem.setDescription(
									p.getJurusan().getFakultas() + " - " + p.getJurusan().getJenjang().getNama());
							prodi2.appendChild(comboitem);
						}

						if (p.getPilihan3()
								&& (p.getKelamin().equalsIgnoreCase("Semua") || p.getKelamin().equalsIgnoreCase(jk))) {
							comboitem = new MyComboitemConfig();
							comboitem.setValue(p.getJurusan());
							comboitem.setLabel(p.getJurusan().getNama());
							comboitem.setDescription(
									p.getJurusan().getFakultas() + " - " + p.getJurusan().getJenjang().getNama());
							prodi3.appendChild(comboitem);
						}

						if (p.getPilihan4()
								&& (p.getKelamin().equalsIgnoreCase("Semua") || p.getKelamin().equalsIgnoreCase(jk))) {
							comboitem = new MyComboitemConfig();
							comboitem.setValue(p.getJurusan());
							comboitem.setLabel(p.getJurusan().getNama());
							comboitem.setDescription(
									p.getJurusan().getFakultas() + " - " + p.getJurusan().getJenjang().getNama());
							prodi4.appendChild(comboitem);
						}

						if (p.getPilihan5()
								&& (p.getKelamin().equalsIgnoreCase("Semua") || p.getKelamin().equalsIgnoreCase(jk))) {
							comboitem = new MyComboitemConfig();
							comboitem.setValue(p.getJurusan());
							comboitem.setLabel(p.getJurusan().getNama());
							comboitem.setDescription(
									p.getJurusan().getFakultas() + " - " + p.getJurusan().getJenjang().getNama());
							prodi5.appendChild(comboitem);
						}
					}

					// Pilihan prodi PMB harus benar-benar berasal dari PaketJurusanPmb paket yang sedang
					// dipilih. Jangan memakai flag "tambahkan jika tidak ditemukan", karena itu dapat
					// memunculkan kembali prodi lama/di luar paket saat membuka data edit.
					Common.selectComboItem(false, prodi1, biodataCalonMahasiswa.getProdi1());
					Common.selectComboItem(false, prodi2, biodataCalonMahasiswa.getProdi2());
					Common.selectComboItem(false, prodi3, biodataCalonMahasiswa.getProdi3());
					Common.selectComboItem(false, prodi4, biodataCalonMahasiswa.getProdi4());
					Common.selectComboItem(false, prodi5, biodataCalonMahasiswa.getProdi5());

					if (rowprodi1.isVisible()) {
						if (biodataCalonMahasiswa.getProdi1() == null && prodi1.getChildren().size() == 2) {
							prodi1.setSelectedIndex(1);
						}
					}
					if (rowprodi2.isVisible()) {
						if (biodataCalonMahasiswa.getProdi2() == null && prodi2.getChildren().size() == 2) {
							prodi2.setSelectedIndex(1);
						}
					}

					if (rowprodi3.isVisible()) {
						if (biodataCalonMahasiswa.getProdi3() == null && prodi3.getChildren().size() == 2) {
							prodi3.setSelectedIndex(1);
						}
					}

					if (rowprodi4.isVisible()) {
						if (biodataCalonMahasiswa.getProdi4() == null && prodi4.getChildren().size() == 2) {
							prodi4.setSelectedIndex(1);
						}
					}

					if (rowprodi5.isVisible()) {
						if (biodataCalonMahasiswa.getProdi5() == null && prodi5.getChildren().size() == 2) {
							prodi5.setSelectedIndex(1);
						}
					}

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:3225");
				}

				eventListenerProdi1.onEvent(null);
			}

		}

		final Prodi1EventListener prodi1EventListener = new Prodi1EventListener();

		class PilihanPaketEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				try {
					Common.clear(paket);
					if (gelombangPendaftaran.getSelectedItem() != null
							&& gelombangPendaftaran.getSelectedItem().getValue() != null) {
						mygelombangPendaftaran = (GelombangPendaftaran) gelombangPendaftaran.getSelectedItem()
								.getValue();
					}

					if (mygelombangPendaftaran != null && mygelombangPendaftaran.getId() != null) {

						BiodataCalonMahasiswaAction.initBg(centerUtama, mygelombangPendaftaran);

						Session session = HibernateUtil.currentSession();
						List<PaketPunyaGelombangPendaftaran> paketPunyaGelombangPendaftarans = ConstantValues
								.simpleList(
										session.createCriteria(PaketPunyaGelombangPendaftaran.class)
												.add(Restrictions.isNotNull("paket"))
												.add(Restrictions.eq("gelombangPendaftaran", mygelombangPendaftaran)),
										PaketPunyaGelombangPendaftaran.class);
						if (paketPunyaGelombangPendaftarans.size() == 1) {
							myPaket = paketPunyaGelombangPendaftarans.get(0).getPaket();
						} else {
							myPaket = null;
						}
					}

					if (myPaket != null && myPaket.getId() != null) {
						Common.selectComboItem(true, paket, myPaket);
						if (paket.getParent() != null) {
							paket.getParent().setVisible(false);
						}
						rowPaket.setVisible(false);
						dPilhanProdi.setContent("<h2>IV. Pilihan Program Studi (Prodi)</h2>");
						pilihanPindahan.setContent("<h2>V. Pilihan Pindahan (Untuk Mahasiswa Pindahan)</h2>");
						prodi1EventListener.onEvent(null);

					} else {
						if (paket.getParent() != null) {
							paket.getParent().setVisible(true);
						}

						rowPaket.setVisible(true);
						paket.setSelectedItem(null);
						dPilhanProdi.setContent("<h2>V. Pilihan Program Studi (Prodi)</h2>");
						pilihanPindahan.setContent("<h2>VI. Pilihan Pindahan (Untuk Mahasiswa Pindahan)</h2>");

						Session session = HibernateUtil.currentSession();
						JurusanSekolahMahasiswaBaru jurusanSekolahMahasiswaBaru = (JurusanSekolahMahasiswaBaru) (jurusanSekolah
								.getSelectedItem() == null ? biodataCalonMahasiswa.getJurusanSekolah()
										: jurusanSekolah.getSelectedItem().getValue());

						if (mygelombangPendaftaran != null) {

							List<Paket> paketsSemua = jurusanSekolahMahasiswaBaru == null ? null
									: ConstantValues.simpleList(session
											.createCriteria(PilihanPaketPerJurusanMhsBaru.class)
											.setProjection(Projections.groupProperty("paket.id"))

											.createAlias("paket", "paket")
											.add(selectedPerguruanTinggi == null
													|| selectedPerguruanTinggi.getId() == null
															? Restrictions.sqlRestriction("true")
															: Restrictions.or(
																	Restrictions.eq("paket.perguruanTinggi",
																			selectedPerguruanTinggi),
																	Restrictions.isNull("paket.perguruanTinggi")))
											.add(Restrictions.or(Restrictions.isNull("paket.aktif"),
													Restrictions.eq("paket.aktif", true)))
											.add(Restrictions.isNotNull("paket")).add(Restrictions
													.eq("jurusanSekolahMahasiswaBaru", jurusanSekolahMahasiswaBaru)),
											Paket.class, false);

							List<Paket> paketsGelombang = ConstantValues
									.simpleList(session.createCriteria(PaketPunyaGelombangPendaftaran.class)
											.add(paketsSemua == null || paketsSemua.isEmpty()
													? Restrictions.sqlRestriction("true")
													: Restrictions.in("paket", paketsSemua))
											.createAlias("paket", "paket")
											.add(selectedPerguruanTinggi == null
													|| selectedPerguruanTinggi.getId() == null
															? Restrictions.sqlRestriction("true")
															: Restrictions.or(
																	Restrictions.eq("paket.perguruanTinggi",
																			selectedPerguruanTinggi),
																	Restrictions.isNull("paket.perguruanTinggi")))
											.add(Restrictions.or(Restrictions.isNull("paket.aktif"),
													Restrictions.eq("paket.aktif", true)))
											.add(Restrictions.eq("gelombangPendaftaran", mygelombangPendaftaran))
											.setProjection(Projections.groupProperty("paket.id")), Paket.class, false);

							System.out.println("paketsGelombang -> " + paketsGelombang.size());

							if (!paketsGelombang.isEmpty()) {
								Common.clear(paket);

								try {
									Collections.sort(paketsGelombang);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:3337");
									// TODO: handle exception
								}

								for (Paket p : paketsGelombang) {
									MyComboitemConfig comboitem = new MyComboitemConfig();
									comboitem.setValue(p);
									comboitem.setLabel(p.getNama());
									comboitem.setDescription(p.getKeterangan());
									paket.appendChild(comboitem);
								}
								Common.selectComboItem(true, paket, biodataCalonMahasiswa.getPaket());
								if (paket.getSelectedItem() == null && paket.getChildren().size() == 1) {
									paket.setSelectedIndex(0);
//									paket.setDisabled(true);
								}

//								if (paket.getSelectedItem() != null && paket.getChildren().size() == 1) {
//									paket.setDisabled(true);
//								} else {
//									paket.setDisabled(false);
//								}
								prodi1EventListener.onEvent(null);
								return;
							}
						}

						if (biodataCalonMahasiswa.getPaket() != null) {
							Common.selectComboItem(true, paket, biodataCalonMahasiswa.getPaket());
						}
						if (jurusanSekolahMahasiswaBaru == null) {
							return;
						}

						List<Paket> paketsSemua = ConstantValues.simpleList(
								session.createCriteria(PilihanPaketPerJurusanMhsBaru.class)
										.setProjection(Projections.groupProperty("paket.id"))

										.createAlias("paket", "paket")
										.add(selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.or(
														Restrictions.eq("paket.perguruanTinggi",
																selectedPerguruanTinggi),
														Restrictions.isNull("paket.perguruanTinggi")))
										.add(Restrictions.or(Restrictions.isNull("paket.aktif"),
												Restrictions.eq("paket.aktif", true)))
										.add(Restrictions.isNotNull("paket")).add(Restrictions
												.eq("jurusanSekolahMahasiswaBaru", jurusanSekolahMahasiswaBaru)),
								Paket.class, false);

						List<Paket> paketsGelombang = ConstantValues
								.simpleList(
										session.createCriteria(PaketPunyaGelombangPendaftaran.class)
												.add(paketsSemua.isEmpty() ? Restrictions.sqlRestriction("true")
														: Restrictions.in("paket", paketsSemua))
												.createAlias("paket", "paket")
												.add(selectedPerguruanTinggi == null
														|| selectedPerguruanTinggi.getId() == null
																? Restrictions.sqlRestriction("true")
																: Restrictions.or(
																		Restrictions.eq("paket.perguruanTinggi",
																				selectedPerguruanTinggi),
																		Restrictions.isNull("paket.perguruanTinggi")))
												.add(Restrictions.or(Restrictions.isNull("paket.aktif"),
														Restrictions.eq("paket.aktif", true)))
												.add(Restrictions.eq("paket.bisaDipilihSemuaGelombang", false))
												.add(gelombangPendaftaran.getSelectedItem() == null
														? Restrictions.sqlRestriction("false")
														: Restrictions.eq("gelombangPendaftaran",
																gelombangPendaftaran.getSelectedItem().getValue()))
												.setProjection(Projections.groupProperty("paket.id")),
										Paket.class, false);

						if (!paketsGelombang.isEmpty()) {
							Common.clear(paket);

							try {
								Collections.sort(paketsGelombang);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:3416");
								// TODO: handle exception
							}

							for (Paket p : paketsGelombang) {
								MyComboitemConfig comboitem = new MyComboitemConfig();
								comboitem.setValue(p);
								comboitem.setLabel(p.getNama());
								comboitem.setDescription(p.getKeterangan());
								paket.appendChild(comboitem);
							}
						} else {

							List<Paket> pakets = ConstantValues.simpleList(
									session.createCriteria(PilihanPaketPerJurusanMhsBaru.class)
											.createAlias("paket", "paket")
											.add(selectedPerguruanTinggi == null
													|| selectedPerguruanTinggi.getId() == null
															? Restrictions.sqlRestriction("true")
															: Restrictions.or(
																	Restrictions.eq("paket.perguruanTinggi",
																			selectedPerguruanTinggi),
																	Restrictions.isNull("paket.perguruanTinggi")))
											.add(Restrictions.or(Restrictions.isNull("paket.aktif"),
													Restrictions.eq("paket.aktif", true)))
											.add(Restrictions.isNotNull("paket"))
											.add(Restrictions.or(
													Restrictions.eq("paket.bisaDipilihSemuaGelombang", true),
													Restrictions.isNull("paket.bisaDipilihSemuaGelombang")))
											.setProjection(Projections.groupProperty("paket.id")).add(Restrictions
													.eq("jurusanSekolahMahasiswaBaru", jurusanSekolahMahasiswaBaru)),
									Paket.class, false);

							try {
								Collections.sort(pakets);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:3451");
								// TODO: handle exception
							}

							Common.clear(paket);
							for (Paket p : pakets) {
								MyComboitemConfig comboitem = new MyComboitemConfig();
								comboitem.setValue(p);
								comboitem.setLabel(p.getNama());
								comboitem.setDescription(p.getKeterangan());
								paket.appendChild(comboitem);
							}

						}

						Common.selectComboItem(paket, biodataCalonMahasiswa.getPaket());
						if (paket.getSelectedItem() == null && paket.getChildren().size() == 1) {
							paket.setSelectedIndex(0);
						}

//						if (paket.getSelectedItem() != null && paket.getChildren().size() == 1) {
//							paket.setDisabled(true);
//						} else {
//							paket.setDisabled(false);
//						}

						prodi1EventListener.onEvent(null);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:3480");
				}

			}

		}

		final PilihanPaketEventListener pilihanPaketEventListener = new PilihanPaketEventListener();
		jurusanSekolah.addEventListener("onChange", pilihanPaketEventListener);
		gelombangPendaftaran.addEventListener("onChange", pilihanPaketEventListener);

		row = new MyRowStyled();
		row.setVisible(tampilkanLabelBesarPadaFormPMB);

		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(dPilhanProdi);

		rowprodi1 = new MyRowStyled();
		rowprodi1.setVisible(false);
		rowprodi1.setParent(rows);
		rowprodi1.appendChild(new MyLabelConfigTitikDua("Prodi  *"));

		prodi1 = new Combobox();

		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
			rowprodi1.appendChild(new Label(
					biodataCalonMahasiswa.getProdi1() == null ? "" : biodataCalonMahasiswa.getProdi1().getNama()));
		} else {
			rowprodi1.appendChild(prodi1);
		}

		prodi1.setWidth("90%");
		prodi1.setReadonly(true);

		rowprodi2 = new MyRowStyled();
		rowprodi2.setVisible(false);
		rowprodi2.setParent(rows);
		rowprodi2.appendChild(new MyLabelConfigTitikDua("Prodi II   *"));

		prodi2 = new Combobox();

		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
			rowprodi2.appendChild(new Label(
					biodataCalonMahasiswa.getProdi2() == null ? "" : biodataCalonMahasiswa.getProdi2().getNama()));
		} else {
			rowprodi2.appendChild(prodi2);
		}

		prodi2.setWidth("90%");
		prodi2.setReadonly(true);

		rowprodi3 = new MyRowStyled();
		rowprodi3.setVisible(false);
		rowprodi3.setParent(rows);
		rowprodi3.appendChild(new MyLabelConfigTitikDua("Prodi III   *"));

		prodi3 = new Combobox();

		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
			rowprodi3.appendChild(new Label(
					biodataCalonMahasiswa.getProdi3() == null ? "" : biodataCalonMahasiswa.getProdi3().getNama()));
		} else {
			rowprodi3.appendChild(prodi3);
		}

		prodi3.setWidth("90%");
		prodi3.setReadonly(true);

		rowprodi4 = new MyRowStyled();
		rowprodi4.setVisible(false);
		rowprodi4.setParent(rows);
		rowprodi4.appendChild(new MyLabelConfigTitikDua("Prodi IV   *"));

		prodi4 = new Combobox();

		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
			rowprodi4.appendChild(new Label(
					biodataCalonMahasiswa.getProdi4() == null ? "" : biodataCalonMahasiswa.getProdi4().getNama()));
		} else {
			rowprodi4.appendChild(prodi4);
		}

		prodi4.setWidth("90%");
		prodi4.setReadonly(true);

		rowprodi5 = new MyRowStyled();
		rowprodi5.setVisible(false);
		rowprodi5.setParent(rows);
		rowprodi5.appendChild(new MyLabelConfigTitikDua("Prodi V   *"));

		prodi5 = new Combobox();

		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
			rowprodi5.appendChild(new Label(
					biodataCalonMahasiswa.getProdi5() == null ? "" : biodataCalonMahasiswa.getProdi5().getNama()));
		} else {
			rowprodi5.appendChild(prodi5);
		}

		prodi5.setWidth("90%");
		prodi5.setReadonly(true);

		rowkonsentrasi = new MyRowStyled();
		rowkonsentrasi.setVisible(false);
		rowkonsentrasi.setParent(rows);
		rowkonsentrasi.appendChild(new MyLabelConfigTitikDua("Konsentrasi   *"));

		konsentrasi = new Combobox();
		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
			rowkonsentrasi.appendChild(new Label(biodataCalonMahasiswa.getKonsentrasi() == null ? ""
					: biodataCalonMahasiswa.getKonsentrasi().getNama()));
		} else {
			rowkonsentrasi.appendChild(konsentrasi);
		}
		konsentrasi.setWidth("90%");
		konsentrasi.setReadonly(true);

		prodi1.addEventListener("onChange", eventListenerProdi1);

		prodi2.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				Paket _pp = myPaket != null && myPaket.getId() != null ? myPaket
						: (Paket) (paket.getSelectedItem() == null ? null : paket.getSelectedItem().getValue());
				if (_pp != null && !tampilSederhana && _pp.getJumlahProdiYgBolehDiambil() > 2) {
					boolean filled = prodi2.getSelectedItem() != null && prodi2.getSelectedItem().getValue() != null;
					rowprodi3.setVisible(filled);
					if (!filled) {
						prodi3.setSelectedItem(null);
						rowprodi4.setVisible(false); prodi4.setSelectedItem(null);
						rowprodi5.setVisible(false); prodi5.setSelectedItem(null);
					}
				}
			}
		});
		prodi3.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				Paket _pp = myPaket != null && myPaket.getId() != null ? myPaket
						: (Paket) (paket.getSelectedItem() == null ? null : paket.getSelectedItem().getValue());
				if (_pp != null && !tampilSederhana && _pp.getJumlahProdiYgBolehDiambil() > 3) {
					boolean filled = prodi3.getSelectedItem() != null && prodi3.getSelectedItem().getValue() != null;
					rowprodi4.setVisible(filled);
					if (!filled) {
						prodi4.setSelectedItem(null);
						rowprodi5.setVisible(false); prodi5.setSelectedItem(null);
					}
				}
			}
		});
		prodi4.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				Paket _pp = myPaket != null && myPaket.getId() != null ? myPaket
						: (Paket) (paket.getSelectedItem() == null ? null : paket.getSelectedItem().getValue());
				if (_pp != null && !tampilSederhana && _pp.getJumlahProdiYgBolehDiambil() > 4) {
					boolean filled = prodi4.getSelectedItem() != null && prodi4.getSelectedItem().getValue() != null;
					rowprodi5.setVisible(filled);
					if (!filled) {
						prodi5.setSelectedItem(null);
					}
				}
			}
		});

		rowprodi1.setVisible(false);
		rowprodi2.setVisible(false);
		rowprodi3.setVisible(false);
		rowprodi4.setVisible(false);
		rowprodi5.setVisible(false);

		row = new MyRowStyled();
		row.setVisible(!tampilSederhana);

		row.setParent(rows);
		row.appendChild(new MyLabelConfigTitikDua("Program *"));

		program = new Combobox();
		program.setReadonly(true);
		Common.initPrograms(program);
		if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null) {
			Common.selectComboItem(true, program, biodataCalonMahasiswa.getProgram());
		}

		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
			row.appendChild(new Label(biodataCalonMahasiswa.getProgram()));
		} else {
			row.appendChild(program);
		}

		program.setWidth("90%");
		program.setDisabled(Common.getKonfigurasi("program_di_formulir_pmb_bisa_dipilih", Konfigurasi.AKTIF).getNilai()
				.equals(Konfigurasi.TIDAK_AKTIF));

		rowPindahan1 = new MyRowStyled();
		rowPindahan1.setVisible(tampilkanLabelBesarPadaFormPMB);
		//		rowPindahan1.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(rowPindahan1, "2");
		rowPindahan1.appendChild(pilihanPindahan);

		rowPindahan2 = new MyRowStyled();
		row.setVisible(!tampilSederhana);
		rowPindahan2.setParent(rows);
		rowPindahan2.appendChild(new MyLabelConfigTitikDua("Merupakan Pendaftaran Mahasiswa Pindahan"));
		rowPindahan2.appendChild(merupakanPindahan = new MyCheckboxConfig());
		merupakanPindahan.setChecked(biodataCalonMahasiswa.getMerupakanPindahan());

		rowpindahanDari = new MyRowStyled();
		rowpindahanDari.setParent(rows);
		rowpindahanDari.appendChild(new ais.ui.util.MyLabelConfigTitikDua("Pindahan Dari Kampus"));
		rowpindahanDari.appendChild(pindahanDari = new AmbilDataPerguruanTinggiLainBanbox());
		pindahanDari.setWidth("90%");
		pindahanDari.setValue(biodataCalonMahasiswa.getPindahanDari() == null ? ""
				: biodataCalonMahasiswa.getPindahanDari().getNama());
		pindahanDari.setAttribute("perguruanTinggiLain", biodataCalonMahasiswa.getPindahanDari());
		pindahanDari.setAttribute("myValue", biodataCalonMahasiswa.getPindahanDari());

		rowpindahanDariKampus = new MyRowStyled();
		rowpindahanDariKampus.setParent(rows);
		rowpindahanDariKampus.appendChild(new MyLabelConfigTitikDua("Atau Nama Kampus Sebelum Pindah"));
		rowpindahanDariKampus
				.appendChild(pindahanDariKampus = new Textbox(biodataCalonMahasiswa.getPindahanDariKampus()));
		pindahanDariKampus.setWidth("90%");

		rowpindahanDariProdi = new MyRowStyled();
		rowpindahanDariProdi.setParent(rows);
		rowpindahanDariProdi.appendChild(new MyLabelConfigTitikDua("Nama Program Studi Sebelum Pindah"));
		rowpindahanDariProdi.appendChild(pindahanDariProdi = new Textbox(biodataCalonMahasiswa.getPindahanDariProdi()));
		pindahanDariProdi.setWidth("90%");

		rownimPindahan = new MyRowStyled();
		rownimPindahan.setParent(rows);
		rownimPindahan.appendChild(new MyLabelConfigTitikDua("NIM/NPM Lama Sebelum Pindah"));
		rownimPindahan.appendChild(nimPindahan = new Textbox(biodataCalonMahasiswa.getNimLamaSebelumPindah()));
		nimPindahan.setWidth("90%");

		rowpindahDariKampusLamaDiSemester = new MyRowStyled();
		rowpindahDariKampusLamaDiSemester.setParent(rows);
		rowpindahDariKampusLamaDiSemester.appendChild(new MyLabelConfigTitikDua("Pindah Dari Kampus Lama Di Semester"));
		rowpindahDariKampusLamaDiSemester.appendChild(
				pindahDariKampusLamaDiSemester = new Intbox(biodataCalonMahasiswa.getPindahDariKampusLamaDiSemester()));
		pindahDariKampusLamaDiSemester.setWidth("90%");

		rowketeranganPindah = new MyRowStyled();
		rowketeranganPindah.setParent(rows);
		rowketeranganPindah.appendChild(new MyLabelConfigTitikDua("Keterangan / Alasan Pindah"));
		rowketeranganPindah.appendChild(keteranganPindah = new Textbox(biodataCalonMahasiswa.getKeteranganPindah()));
		keteranganPindah.setWidth("90%");
		keteranganPindah.setRows(3);

		EventListener pindahanEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowpindahanDari.setVisible(merupakanPindahan.isChecked());
				rowpindahanDariKampus.setVisible(merupakanPindahan.isChecked());
				rownimPindahan.setVisible(merupakanPindahan.isChecked());
				rowpindahDariKampusLamaDiSemester.setVisible(merupakanPindahan.isChecked());
				rowketeranganPindah.setVisible(merupakanPindahan.isChecked());
				rowpindahanDariProdi.setVisible(merupakanPindahan.isChecked());

			}
		};

		merupakanPindahan.addEventListener("onClick", pindahanEventListener);

		nama.setMaxlength(255);
		noIdentitas.setMaxlength(255);
		tempatLahir.setMaxlength(255);
		email.setMaxlength(255);
		alamat.setMaxlength(255);

		kelurahanCalon.setMaxlength(255);
		kecamatanCalon.setMaxlength(255);
		teleponRumah.setMaxlength(20);
		asalSma.setMaxlength(255);
		alamatAsalSma.setMaxlength(255);

		kecamatanSekolah.setMaxlength(255);
		namaAyah.setMaxlength(255);
		namaIbu.setMaxlength(255);
		namaWali.setMaxlength(255);
		alamatOrtu.setMaxlength(255);
		kelurahanOrtu.setMaxlength(255);
		kecamatanOrtu.setMaxlength(255);
		noTelpOrtu.setMaxlength(20);

		class JurusanSekolahEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				try {
					// TODO Auto-generated method stub
					Common.selectComboItem(true, jurusanSekolah, biodataCalonMahasiswa.getJurusanSekolah());

					if (myPaket == null || myPaket.getId() == null) {
						Common.clear(prodi1);
						Common.clear(prodi2);
						Common.clear(prodi3);
						Common.clear(prodi4);
						Common.clear(prodi5);
						prodi1.setSelectedItem(null);
						prodi2.setSelectedItem(null);
						prodi3.setSelectedItem(null);
						prodi4.setSelectedItem(null);
						prodi5.setSelectedItem(null);
						Common.clear(paket);
						paket.setSelectedItem(null);
					}

					if (jenisSekolah.getSelectedItem() == null || jenisSekolah.getSelectedItem().getValue() == null) {
						return;
					}

					GelombangPendaftaran myGelombangPendaftaran = (GelombangPendaftaran) (gelombangPendaftaran
							.getSelectedItem() == null ? null : gelombangPendaftaran.getSelectedItem().getValue());

					System.out.println("myGelombangPendaftaran -> " + myGelombangPendaftaran);

					if (myGelombangPendaftaran == null) {
						Common.insertComboDanSemua(jurusanSekolah, new String[] { "nama" }, "keterangan",
								JurusanSekolahMahasiswaBaru.class, "== Klik disini untuk pilih ==",
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								Restrictions.eq("jenisSekolahMahasiswaBaru",
										jenisSekolah.getSelectedItem().getValue()));
					} else {
						List<Long> pakets = HibernateUtil.currentSession()
								.createCriteria(PaketPunyaGelombangPendaftaran.class)
								.add(Restrictions.eq("gelombangPendaftaran", myGelombangPendaftaran))
								.add(Restrictions.isNotNull("paket"))
								.setProjection(Projections.groupProperty("paket.id")).list();

						System.out.println("pakets -> " + pakets);

						if (pakets.isEmpty()) {
							Common.insertComboDanSemua(jurusanSekolah, new String[] { "nama" }, "keterangan",
									JurusanSekolahMahasiswaBaru.class, "== Klik disini untuk pilih ==",
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
									Restrictions.eq("jenisSekolahMahasiswaBaru",
											jenisSekolah.getSelectedItem().getValue()));
						} else {
							List<JurusanSekolahMahasiswaBaru> jenisSekolahMahasiswaBarus = ConstantValues.simpleList(
									HibernateUtil.currentSession().createCriteria(PilihanPaketPerJurusanMhsBaru.class)
											.add(Restrictions.in("paket.id", pakets))
											.setProjection(Projections.groupProperty("jurusanSekolahMahasiswaBaru.id"))

											.createAlias("jurusanSekolahMahasiswaBaru", "jurusanSekolahMahasiswaBaru")
											.add(Restrictions.eq(
													"jurusanSekolahMahasiswaBaru.jenisSekolahMahasiswaBaru",
													jenisSekolah.getSelectedItem().getValue())),

									JurusanSekolahMahasiswaBaru.class, false);

							System.out.println("jenisSekolahMahasiswaBarus -> " + jenisSekolahMahasiswaBarus.size());

							jenisSekolahMahasiswaBarus.add(null);
							Common.insertComboItems(jurusanSekolah, new String[] { "nama" }, "keterangan",
									jenisSekolahMahasiswaBarus, "== Klik disini untuk pilih ==");
							jurusanSekolah.setReadonly(true);
						}
						pakets.clear();
						pakets = null;
					}

					Common.selectComboItem(jurusanSekolah, biodataCalonMahasiswa.getJurusanSekolah());

					if (biodataCalonMahasiswa.getJurusanSekolah() == null && jurusanSekolah.getChildren().size() == 2) {
						jurusanSekolah.setSelectedIndex(0);
					}

					pilihanPaketEventListener.onEvent(null);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:3853");
				}
			}

		}

		JurusanSekolahEventListener jurusanSekolahEventListener = new JurusanSekolahEventListener();
		jenisSekolah.addEventListener("onChange", jurusanSekolahEventListener);

		Common.createDefaultTimer(jurusanSekolahEventListener);

		paket.addEventListener("onChange", prodi1EventListener);
		jenisKelamin.addEventListener("onChange", prodi1EventListener);

		parameterRows = new ArrayList<Row>();
		lampiranLains = new HashMap<String, LampiranLain>();
		parameterTambahanListener = new ParameterTambahanListener(biodataCalonMahasiswa, parameterRows, lampiranLains,
				paket, gelombangPendaftaran, true, rows);

		EventListener refreshGelombangParameterTambahanListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				refreshSelectedGelombangPendaftaranComboValue();
			}
		};
		paket.addEventListener("onChange", refreshGelombangParameterTambahanListener);
		gelombangPendaftaran.addEventListener("onChange", refreshGelombangParameterTambahanListener);
		paket.addEventListener("onChange", parameterTambahanListener);
		gelombangPendaftaran.addEventListener("onChange", parameterTambahanListener);
		refreshSelectedGelombangPendaftaranComboValue();
		parameterTambahanListener.onEvent(null);

		if (Common.bolehKonfigurasi("tampilkan_info_asal_instansi", Konfigurasi.TIDAK_AKTIF)) {

			MyRowStyled rowPindahan1 = new MyRowStyled();
			rowPindahan1.setVisible(tampilkanLabelBesarPadaFormPMB);
			//			rowPindahan1.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowPindahan1, "2");
			rowPindahan1.appendChild(new MyLabelStyled("Informasi Asal Instansi (jika berasal dari suatu instansi)"));

			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new MyLabelConfigTitikDua("Nama Instansi Asal"));
			row.appendChild(instansiAsal = new Textbox(biodataCalonMahasiswa.getInstansiAsal()));
			instansiAsal.setWidth("90%");

			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new MyLabelConfigTitikDua("Kota/Kab. Instansi Asal"));
			row.appendChild(kotaInstansi = new AmbilDataKecamatanBanbox("2"));
			kotaInstansi.setValue(biodataCalonMahasiswa.getKotaInstansi() == null ? "== Klik disini untuk pilih =="
					: biodataCalonMahasiswa.getKotaInstansi().getNama());
			kotaInstansi.setAttribute("wilayah", biodataCalonMahasiswa.getKotaInstansi());
			kotaInstansi.setWidth("90%");

			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new MyLabelConfigTitikDua("Jabatan di Instansi Asal"));
			row.appendChild(jabatanDiInstansiAsal = new Textbox(biodataCalonMahasiswa.getJabatanDiInstansiAsal()));
			jabatanDiInstansiAsal.setWidth("90%");
		}

		fotoBiodataCalonMahasiswa = null;

		rowFoto = new MyRowStyled();
		rowFoto.setParent(rows);
		rowFoto.setAttribute("jenis", true);
		ais.ui.util.ZkCompat.setSpans(rowFoto, "2");
		Common.createDownloadUploadFoto(rowFoto, biodataCalonMahasiswa, FotoBiodataCalonMahasiswa.class,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// Callback upload dapat membawa LampiranLain (bukan FotoBiodataCalonMahasiswa)
						// bila file disimpan sebagai lampiran generik → guard instanceof agar tidak
						// ClassCastException.
						Object dataFoto = arg0.getData();
						if (dataFoto instanceof FotoBiodataCalonMahasiswa) {
							fotoBiodataCalonMahasiswa = (FotoBiodataCalonMahasiswa) dataFoto;
						}
					}
				}, true);

		pindahanEventListener.onEvent(null);

		gelombangPendaftaranEventListener.onEvent(null);

		pilihanPaketEventListener.onEvent(null);

		if (tbmuser != null && biodataCalonMahasiswa.getId() != null) {
			if (Common.getKonfigurasi("calon_mahasiswa_tidak_perlu_upload_ijazah", Konfigurasi.AKTIF).getNilai()
					.equals(Konfigurasi.TIDAK_AKTIF)) {
				CommonPMB.createDownloadUploadFileLampiran(rows, biodataCalonMahasiswa,
						LampiranLainBiodataCalonMahasiswa.IJAZAH,
						"Ijazah / Surat Keterangan Lulus (pendidikan sebelumnya)", row.getStyle());
			}

			if (Common.getKonfigurasi("calon_mahasiswa_tidak_perlu_upload_nilai", Konfigurasi.AKTIF).getNilai()
					.equals(Konfigurasi.TIDAK_AKTIF)) {
				CommonPMB.createDownloadUploadFileLampiran(rows, biodataCalonMahasiswa,
						LampiranLainBiodataCalonMahasiswa.TRANSKRIP_NILAI,
						"Transkrip Nilai Lulus (pendidikan sebelumnya)", row.getStyle());
			}

			if (Common.getKonfigurasi("calon_mahasiswa_tidak_perlu_upload_ktp", Konfigurasi.AKTIF).getNilai()
					.equals(Konfigurasi.TIDAK_AKTIF)) {
				CommonPMB.createDownloadUploadFileLampiran(rows, biodataCalonMahasiswa,
						LampiranLainBiodataCalonMahasiswa.KTP,
						"KTP / Kartu Pelajar (pendidikan sebelumnya) / Kartu Identitas lain", row.getStyle());
			}

			Konfigurasi konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_1",
					Konfigurasi.TIDAK_AKTIF);
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				CommonPMB.createDownloadUploadFileLampiran(rows, biodataCalonMahasiswa,
						LampiranLainBiodataCalonMahasiswa.LAMPIRAN_1, konfigurasiUpload.getInfo1(), row.getStyle());
			}

			konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_2", Konfigurasi.TIDAK_AKTIF);
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				CommonPMB.createDownloadUploadFileLampiran(rows, biodataCalonMahasiswa,
						LampiranLainBiodataCalonMahasiswa.LAMPIRAN_2, konfigurasiUpload.getInfo1(), row.getStyle());
			}

			konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_3", Konfigurasi.TIDAK_AKTIF);
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				CommonPMB.createDownloadUploadFileLampiran(rows, biodataCalonMahasiswa,
						LampiranLainBiodataCalonMahasiswa.LAMPIRAN_3, konfigurasiUpload.getInfo1(), row.getStyle());
			}

			konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_4", Konfigurasi.TIDAK_AKTIF);
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				CommonPMB.createDownloadUploadFileLampiran(rows, biodataCalonMahasiswa,
						LampiranLainBiodataCalonMahasiswa.LAMPIRAN_4, konfigurasiUpload.getInfo1(), row.getStyle());
			}

			konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_5", Konfigurasi.TIDAK_AKTIF);
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				CommonPMB.createDownloadUploadFileLampiran(rows, biodataCalonMahasiswa,
						LampiranLainBiodataCalonMahasiswa.LAMPIRAN_5, konfigurasiUpload.getInfo1(), row.getStyle());
			}

			konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_6", Konfigurasi.TIDAK_AKTIF);
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				CommonPMB.createDownloadUploadFileLampiran(rows, biodataCalonMahasiswa,
						LampiranLainBiodataCalonMahasiswa.LAMPIRAN_6, konfigurasiUpload.getInfo1(), row.getStyle());
			}

			konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_7", Konfigurasi.TIDAK_AKTIF);
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				CommonPMB.createDownloadUploadFileLampiran(rows, biodataCalonMahasiswa,
						LampiranLainBiodataCalonMahasiswa.LAMPIRAN_7, konfigurasiUpload.getInfo1(), row.getStyle());
			}

			konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_8", Konfigurasi.TIDAK_AKTIF);
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				CommonPMB.createDownloadUploadFileLampiran(rows, biodataCalonMahasiswa,
						LampiranLainBiodataCalonMahasiswa.LAMPIRAN_8, konfigurasiUpload.getInfo1(), row.getStyle());
			}

			konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_9", Konfigurasi.TIDAK_AKTIF);
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				CommonPMB.createDownloadUploadFileLampiran(rows, biodataCalonMahasiswa,
						LampiranLainBiodataCalonMahasiswa.LAMPIRAN_9, konfigurasiUpload.getInfo1(), row.getStyle());
			}

			konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_10", Konfigurasi.TIDAK_AKTIF);
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				CommonPMB.createDownloadUploadFileLampiran(rows, biodataCalonMahasiswa,
						LampiranLainBiodataCalonMahasiswa.LAMPIRAN_10, konfigurasiUpload.getInfo1(), row.getStyle());
			}
		}

		if (Common.bolehKonfigurasi("tampilkan_info_sekolah_dari_mana_pada_pmb", Konfigurasi.TIDAK_AKTIF)) {

			row = new MyRowStyled();

			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.appendChild(new ais.ui.util.MyHtml("<hr>"));

			String infoDariManaPmb = Common.getKonfigurasi("info_dari_mana_pmb", "Website,Teman,Radio,Koran,Lain-lain")
					.getNilai();

			row = new MyRowStyled();
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.setParent(rows);
			row.appendChild(new MyLabelConfigTitikDua(
					"Anda mendapatkan informasi Pendaftaran Mahasiswa Baru ini dari mana ? *"));

			boolean pilihSalahSatuInfoPmbDariMana = Common.bolehKonfigurasi("pilih_salah_satu_info_pmb_dari_mana", Konfigurasi.TIDAK_AKTIF);

			row = new MyRowStyled();
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.setParent(rows);
			infoKampusDariMana = Common.isMobile() ? new Vbox() : new Hbox();

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
						if (biodataCalonMahasiswa.getInfoKampusDariMana().contains(";Teman;".toLowerCase())
								|| biodataCalonMahasiswa.getInfoKampusDariMana().contains(";Kawan;".toLowerCase())) {
							radioTeman.setChecked(true);
						}
					} else if (r.toLowerCase().equalsIgnoreCase("Siswa".toLowerCase())
							|| r.toLowerCase().equalsIgnoreCase("Mahasiswa".toLowerCase())
							|| r.toLowerCase().equalsIgnoreCase("mahasiswa/alumni kampus".toLowerCase())) {
						radioMahasiswa.setLabel(r);
						radioMahasiswa.setValue(r);
						infoKampusDariMana.appendChild(radioMahasiswa);
						if (biodataCalonMahasiswa.getInfoKampusDariMana().toLowerCase()
								.contains(";mahasiswa/alumni kampus;".toLowerCase())
								|| biodataCalonMahasiswa.getInfoKampusDariMana().toLowerCase()
										.contains(";Mahasiswa;".toLowerCase())
								|| biodataCalonMahasiswa.getInfoKampusDariMana().toLowerCase()
										.contains(";Siswa;".toLowerCase())) {
							radioMahasiswa.setChecked(true);
						}
					} else if (r.equalsIgnoreCase("Lain-lain".toLowerCase()) || r.equalsIgnoreCase("Lain".toLowerCase())
							|| r.equalsIgnoreCase("Lainnya".toLowerCase())) {

						radioLainlain.setLabel(r);
						radioLainlain.setValue(r);
						infoKampusDariMana.appendChild(radioLainlain);
						if (biodataCalonMahasiswa.getInfoKampusDariMana().contains(";Lain-lain;".toLowerCase())
								|| biodataCalonMahasiswa.getInfoKampusDariMana().contains(";lain;".toLowerCase())
								|| biodataCalonMahasiswa.getInfoKampusDariMana().contains(";lain-lain;".toLowerCase())
								|| biodataCalonMahasiswa.getInfoKampusDariMana().contains(";Lainnya;".toLowerCase())) {
							radioLainlain.setChecked(true);
						}
					} else if (r.contains("dosen".toLowerCase())) {

						radioDosen.setLabel(r);
						radioDosen.setValue(r);
						infoKampusDariMana.appendChild(radioDosen);
						if (biodataCalonMahasiswa.getInfoKampusDariMana().contains(";Dosen;".toLowerCase())) {
							radioDosen.setChecked(true);
						}
					} else {
						Checkbox check = pilihSalahSatuInfoPmbDariMana ? new Radio() : new Checkbox();
						check.setLabel(r);
						check.setValue(r);
						infoKampusDariMana.appendChild(check);
						if (biodataCalonMahasiswa.getInfoKampusDariMana().contains(";" + r + ";")) {
							check.setChecked(true);
						}
					}
				}
			}

			rownamaTemanInfoKampusDariMana = new MyRowStyled();
			rownamaTemanInfoKampusDariMana.setParent(rows);
			rownamaTemanInfoKampusDariMana
					.appendChild(new MyLabelConfigTitikDua("Sebutkan Nama dan NIM Teman/Mahasiswa *"));
			rownamaTemanInfoKampusDariMana.appendChild(
					namaTemanInfoKampusDariMana = new Textbox(biodataCalonMahasiswa.getNamaTemanInfoKampusDariMana()));
			rownamaTemanInfoKampusDariMana.setWidth("90%");

			rowdariNamaDosenKaryawan = new MyRowStyled();
			rowdariNamaDosenKaryawan.setParent(rows);
			rowdariNamaDosenKaryawan.appendChild(new MyLabelConfigTitikDua("Sebutkan Nama Dosen / Karyawan *"));
			rowdariNamaDosenKaryawan
					.appendChild(dariNamaDosenKaryawan = new Textbox(biodataCalonMahasiswa.getDariNamaDosenKaryawan()));
			rowdariNamaDosenKaryawan.setWidth("90%");

			rowketeranganInfoKampusDariMana = new MyRowStyled();
			rowketeranganInfoKampusDariMana.setParent(rows);
			rowketeranganInfoKampusDariMana.appendChild(new MyLabelConfigTitikDua("Sebutkan dari mana *"));
			rowketeranganInfoKampusDariMana.appendChild(keteranganInfoKampusDariMana = new Textbox(
					biodataCalonMahasiswa.getKeteranganInfoKampusDariMana()));
			rowketeranganInfoKampusDariMana.setWidth("90%");

			EventListener keteranganEventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					try {
						rowketeranganInfoKampusDariMana.setVisible(radioLainlain.isChecked());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:4149");

					}

					try {
						rownamaTemanInfoKampusDariMana.setVisible(radioTeman.isChecked() || radioMahasiswa.isChecked());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:4155");

					}

					try {
						rowdariNamaDosenKaryawan.setVisible(radioDosen.isChecked());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:4161");

					}
				}
			};
			keteranganEventListener.onEvent(null);
			infoKampusDariMana.addEventListener("onClick", keteranganEventListener);

		}

		if (biodataCalonMahasiswa.getId() != null) {
			/*
			 * Jangan kirim object GelombangPendaftaran fixed ke helper verifikasi.
			 * Jika dikirim sebagai parameter fixed, saat combobox gelombang diganti, helper
			 * tetap membaca gelombang lama. Karena itu cukup refresh value combobox, lalu
			 * helper membaca gelombang aktif langsung dari combobox saat onChange.
			 */
			refreshSelectedGelombangPendaftaranComboValue();
			subRowsVerifikasiKelengkapanCalonMahasiswa = VerifikasiPMBHelper.tampilkanVerifikasi(biodataCalonMahasiswa,
					rows, gelombangPendaftaran, jenisSeleksi, null);

			/*
			 * Nilai rapor dan parameter verifikasi bergantung pada paket. Pada saat gelombang
			 * berubah, daftar paket juga berubah. Helper versi ini diberi trigger combobox
			 * gelombang agar section ikut di-render ulang setelah paket aktif diperbarui.
			 */
			subRowsVerifikasiNilaiRapor = VerifikasiMatapelajaranPMBHelper.tampilkanVerifikasi(biodataCalonMahasiswa,
					rows, paket, null, gelombangPendaftaran);

			subRowsVerifikasiNilaiParameter = VerifikasiParameterPMBHelper.tampilkanVerifikasi(biodataCalonMahasiswa,
					rows, paket, null, gelombangPendaftaran);
		}

		pernyataan = Common.tambahKeteranganRowHtml(rows,
				"Dengan ini saya menyatakan bahwa data yang saya masukkan benar adanya, dan jika ternyata dikemudian hari ditemukan kesalahan pada data ini baik yang disengaja ataupun tidak disengaja maka saya bersedia menerima sanksi dan resiko yang ditimbulkan karenanya");

		pernyataan.setChecked(biodataCalonMahasiswa.getPernyataan());

		if (biodataCalonMahasiswa.getId() != null) {
			pernyataan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					session.refresh(biodataCalonMahasiswa);

					biodataCalonMahasiswa.setPernyataan(pernyataan.isChecked());

					Common.refreshUpdate(session, biodataCalonMahasiswa);

					session.flush();

				}
			});
		}

		if (Common.getKonfigurasi("jika_sudah_ada_data_mahasiswa_data_calon_mahasiswa_tidak_bisa_diubah_baru",
				Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.TIDAK_AKTIF)) {
			try {
				if (biodataCalonMahasiswa.getMahasiswa() != null) {
					Common.freeze(borderlayout, true);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:4223");

			}
		}
		keterangan = new Textbox(biodataCalonMahasiswa.getKeterangan());
		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null) {
			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new MyLabelConfigTitikDua("Keterangan / Catatan Admin"));
			row.appendChild(keterangan);
			keterangan.setWidth("90%");
			keterangan.setRows(3);
		} else if (!biodataCalonMahasiswa.getKeterangan().isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new MyLabelConfigTitikDua("Keterangan / Catatan Admin"));
			row.appendChild(new Label(biodataCalonMahasiswa.getKeterangan()));
		}

		Common.selectComboItem(true, jurusanSekolah, biodataCalonMahasiswa.getJurusanSekolah());

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		toolbar.setAlign("center");

		if (biodataCalonMahasiswa.getId() == null) {

			final MyButtonConfig save = new MyButtonConfig("  D A F T A R  ", "/img/save.gif");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (onSave(event)) {
						save.setDisabled(true);
						noRegistrasi.focus();

						String informasi = Common.getKonfigurasi("informasi_registrasi_pmb_berhasil_login",
								"Proses pendaftaran berhasil dilakukan. Nomor pendaftaran : [no_reg]. Nomor pendaftaran ini digunakan untuk proses login. Silahkan catat nomor pendaftaran tersebut dan selanjutnya klik tombol Login.")
								.getNilai();
						informasi = org.apache.commons.lang3.StringUtils.replace(informasi, "[no_reg]",
								noRegistrasi.getValue());
						MyMessageboxConfig.show(informasi, "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										if (calonMahasiswaListener != null) {
											calonMahasiswaListener.onEvent(arg0);
										}

										if (eventListener != null) {
											detach();
										}

										if (BiodataCalonMahasiswaAction.this.biodataCalonMahasiswa
												.getGelombangPendaftaran().getOtomatisLoginSetelahDaftar()) {
											BiodataCalonMahasiswaAction.this.biodataCalonMahasiswa.setTelahLogin(true);
											BiodataCalonMahasiswaAction.this.biodataCalonMahasiswa
													.setWaktuLogin(ais.ui.util.WaktuUtil.getDate());
											Common.refreshUpdate(
													BiodataCalonMahasiswaAction.this.biodataCalonMahasiswa);

											Common.setLogin(BiodataCalonMahasiswaAction.this.biodataCalonMahasiswa);
											Sessions.getCurrent(true).setAttribute("cetak", true);
											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													Executions.getCurrent().sendRedirect("");
												}
											});
										} else {

											BiodataCalonMahasiswaAction.onCetakKartu(
													BiodataCalonMahasiswaAction.this.biodataCalonMahasiswa, true);
										}

									}
								});

					}

				}
			});
			save.setParent(toolbar);

			if (eventListener != null) {
				MyButtonConfig batal = new MyButtonConfig("  BATAL  ", "/img/svg/trash.svg");
				batal.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						detach();

					}
				});
				batal.setParent(toolbar);
			}

		} else {
			MyButtonConfig save = new MyButtonConfig("  SIMPAN  ", "/img/save.gif");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (onSave(event)) {

						if (eventListener != null) {
							eventListener.onEvent(event);
						}

						MyMessageboxConfig.show("Data berhasil tersimpan", "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										BiodataCalonMahasiswaAction.onCetakKartu(
												BiodataCalonMahasiswaAction.this.biodataCalonMahasiswa, false);
									}
								});

					}

				}
			});
			save.setParent(toolbar);

			if (BiodataCalonMahasiswaAction.this.biodataCalonMahasiswa.getDikunci() != null) {
				save.setVisible(false);
			}

			if (biodataCalonMahasiswa.getPernyataan() && tbmuser != null
					&& tbmuser.getBiodataCalonMahasiswa() != null) {

				boolean a = Common.bolehKonfigurasi("jika_calon_mhs_sudah_terdaftar_dan_mensetujui_maka_tidak_boleh_simpan_ulang", Konfigurasi.TIDAK_AKTIF);

				save.setVisible(!a);

				if (a) {
					Common.freeze(borderlayout, true);
				}
			}

			save = new MyButtonConfig("  SELESAI  ", "/img/cancel.gif");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					detach();

				}
			});
			save.setParent(toolbar);
		}

		Common.masukkanListener(rows, masukkanPerubahan);

		if (BiodataCalonMahasiswaAction.this.biodataCalonMahasiswa.getDikunci() != null) {
			Common.freezeGanti(centerUtama, true);
		}
	}

	private Paket getPaketDipilih() {
		try {
			if (myPaket != null && myPaket.getId() != null) {
				return myPaket;
			}
			return paket == null || paket.getSelectedItem() == null ? null : (Paket) paket.getSelectedItem().getValue();
		} catch (Exception e) {
			return null;
		}
	}

	private Object getSelectedComboValue(Combobox combo) {
		try {
			return combo == null || combo.getSelectedItem() == null ? null : combo.getSelectedItem().getValue();
		} catch (Exception e) {
			return null;
		}
	}

	private String getSelectedStringValue(Combobox combo, String fallback) {
		Object value = getSelectedComboValue(combo);
		return value == null ? fallback : value.toString();
	}

	private Paket getPaketDipilihDenganFallback(Paket fallback) {
		Paket dipilih = getPaketDipilih();
		return dipilih == null ? fallback : dipilih;
	}

	private Jurusan getJurusanDipilihDenganFallback(Combobox combo, Jurusan fallback) {
		Object value = getSelectedComboValue(combo);
		return value instanceof Jurusan ? (Jurusan) value : fallback;
	}

	private JenisSeleksi getJenisSeleksiDipilihDenganFallback(JenisSeleksi fallback) {
		Object value = getSelectedComboValue(jenisSeleksi);
		return value instanceof JenisSeleksi ? (JenisSeleksi) value : fallback;
	}

	private GelombangPendaftaran getGelombangDipilihDenganFallback(GelombangPendaftaran fallback) {
		Object value = getSelectedComboValue(gelombangPendaftaran);
		return value instanceof GelombangPendaftaran ? (GelombangPendaftaran) value : fallback;
	}

	private Jurusan getJurusanDipilih(Combobox combo) {
		try {
			return combo == null || combo.getSelectedItem() == null ? null : (Jurusan) combo.getSelectedItem().getValue();
		} catch (Exception e) {
			return null;
		}
	}

	private PaketJurusanPmb getPaketJurusanPmb(Session session, Paket paket, Jurusan jurusan) {
		if (session == null || paket == null || jurusan == null) {
			return null;
		}
		try {
			return (PaketJurusanPmb) session.createCriteria(PaketJurusanPmb.class).add(Restrictions.eq("paket", paket))
					.add(Restrictions.eq("jurusan", jurusan)).setMaxResults(1).uniqueResult();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return null;
		}
	}

	private boolean validasiJenjangProdiSesuaiPaket() {
		try {
			Paket paketDipilih = getPaketDipilihDenganFallback(
					biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getPaket());
			if (paketDipilih == null) {
				return true;
			}
			Session session = HibernateUtil.currentSession();

			if (!validasiPilihanTermasukPaket(session, paketDipilih,
					getJurusanDipilihDenganFallback(prodi1, biodataCalonMahasiswa.getProdi1()), prodi1,
					"Prodi pilihan pertama", true, false, false, false, false)) {
				return false;
			}
			if (!validasiPilihanTermasukPaket(session, paketDipilih,
					getJurusanDipilihDenganFallback(prodi2, biodataCalonMahasiswa.getProdi2()), prodi2,
					"Prodi pilihan kedua", false, true, false, false, false)) {
				return false;
			}
			if (!validasiPilihanTermasukPaket(session, paketDipilih,
					getJurusanDipilihDenganFallback(prodi3, biodataCalonMahasiswa.getProdi3()), prodi3,
					"Prodi pilihan ketiga", false, false, true, false, false)) {
				return false;
			}
			if (!validasiPilihanTermasukPaket(session, paketDipilih,
					getJurusanDipilihDenganFallback(prodi4, biodataCalonMahasiswa.getProdi4()), prodi4,
					"Prodi pilihan keempat", false, false, false, true, false)) {
				return false;
			}
			if (!validasiPilihanTermasukPaket(session, paketDipilih,
					getJurusanDipilihDenganFallback(prodi5, biodataCalonMahasiswa.getProdi5()), prodi5,
					"Prodi pilihan kelima", false, false, false, false, true)) {
				return false;
			}
			return true;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return true;
		}
	}

	private boolean validasiPilihanTermasukPaket(Session session, Paket paketDipilih, Jurusan jurusan, Combobox combo,
			String labelPilihan, boolean cekPilihan1, boolean cekPilihan2, boolean cekPilihan3, boolean cekPilihan4,
			boolean cekPilihan5) {
		if (jurusan == null) {
			return true;
		}
		PaketJurusanPmb paketJurusanPmb = getPaketJurusanPmb(session, paketDipilih, jurusan);
		boolean cocok = paketJurusanPmb != null
				&& ((cekPilihan1 && paketJurusanPmb.getPilihan1()) || (cekPilihan2 && paketJurusanPmb.getPilihan2())
						|| (cekPilihan3 && paketJurusanPmb.getPilihan3())
						|| (cekPilihan4 && paketJurusanPmb.getPilihan4())
						|| (cekPilihan5 && paketJurusanPmb.getPilihan5()));
		if (!cocok) {
			String namaJenjang = jurusan.getJenjang() == null || jurusan.getJenjang().getNama() == null ? ""
					: jurusan.getJenjang().getNama();
			try {
				MyMessageboxConfig.show(
						"Mohon maaf, " + labelPilihan + " (" + jurusan.getNama() + " - " + namaJenjang
								+ ") tidak sesuai dengan Paket Pendaftaran (" + paketDipilih.getNama() + ") yang dipilih."
								+ " Langkah yang dapat dilakukan: (1) buka kembali dropdown " + labelPilihan
								+ " dan pilih Program Studi dari daftar yang tersedia untuk paket ini; (2) pastikan jenjang"
								+ " Program Studi sesuai dengan jenjang paket pendaftaran; (3) jangan memilih Program Studi"
								+ " hasil ketik manual/di luar daftar dropdown. Jika masih mengalami kendala, hubungi"
								+ " Administrator atau tim teknis.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				ais.common.ErrorAuditUtil.record(e,
						"validasiPilihanTermasukPaket: gagal menampilkan peringatan prodi di luar paket");
			}
			try {
				if (combo != null) {
					combo.focus();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:validasiPilihanTermasukPaket");
			}
			return false;
		}
		return true;
	}

	private boolean validasiKuotaPaketJurusanPmb() {
		Session session = null;
		try {
			Paket paketDipilih = getPaketDipilihDenganFallback(
					biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getPaket());
			if (paketDipilih == null) {
				return true;
			}
			String ta = getSelectedStringValue(tahunAkademik,
					biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getTahunAkademik());
			GelombangPendaftaran gel = getGelombangDipilihDenganFallback(
					biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getGelombangPendaftaran());
			Long currentId = biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getId();
			session = HibernateUtil.currentSession();

			if (!validasiKuotaPerPilihan(session, paketDipilih,
					getJurusanDipilihDenganFallback(prodi1, biodataCalonMahasiswa.getProdi1()), gel, ta, currentId,
					prodi1, "Prodi pilihan pertama")) {
				return false;
			}
			if (!validasiKuotaPerPilihan(session, paketDipilih,
					getJurusanDipilihDenganFallback(prodi2, biodataCalonMahasiswa.getProdi2()), gel, ta, currentId,
					prodi2, "Prodi pilihan kedua")) {
				return false;
			}
			if (!validasiKuotaPerPilihan(session, paketDipilih,
					getJurusanDipilihDenganFallback(prodi3, biodataCalonMahasiswa.getProdi3()), gel, ta, currentId,
					prodi3, "Prodi pilihan ketiga")) {
				return false;
			}
			if (!validasiKuotaPerPilihan(session, paketDipilih,
					getJurusanDipilihDenganFallback(prodi4, biodataCalonMahasiswa.getProdi4()), gel, ta, currentId,
					prodi4, "Prodi pilihan keempat")) {
				return false;
			}
			if (!validasiKuotaPerPilihan(session, paketDipilih,
					getJurusanDipilihDenganFallback(prodi5, biodataCalonMahasiswa.getProdi5()), gel, ta, currentId,
					prodi5, "Prodi pilihan kelima")) {
				return false;
			}
			return true;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return true;
		}
	}

	private boolean validasiKuotaPerPilihan(Session session, Paket paketDipilih, Jurusan jurusan,
			GelombangPendaftaran gelombang, String tahunAkademik, Long currentId, Combobox combo, String labelPilihan) {
		if (jurusan == null) {
			return true;
		}
		PaketJurusanPmb paketJurusanPmb = getPaketJurusanPmb(session, paketDipilih, jurusan);
		if (paketJurusanPmb == null) {
			return true;
		}
		Integer kuota = paketJurusanPmb.getKuota();
		if (kuota == null || kuota.intValue() <= 0) {
			return true;
		}
		int terpakai = BiodataCalonMahasiswa.hitungJumlahPendaftarKuota(session, paketJurusanPmb, gelombang,
				tahunAkademik, currentId);
		if (terpakai >= kuota.intValue()) {
			String lingkup = paketJurusanPmb.getKuotaBerlakuPerGelombang() ? "gelombang ini"
					: "tahun akademik " + (tahunAkademik == null ? "ini" : tahunAkademik);

			tampilkanPeringatanKuotaPenuh(labelPilihan, jurusan, lingkup, kuota, terpakai);

			try {
				if (combo != null) {
					combo.focus();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:4491");
			}
			return false;
		}
		return true;
	}

	private void tampilkanPeringatanKuotaPenuh(String labelPilihan, Jurusan jurusan, String lingkup, Integer kuota,
			int terpakai) {
		String namaJurusan = jurusan == null || jurusan.getNama() == null ? "" : jurusan.getNama();
		String pesan = labelPilihan + " (" + namaJurusan + ") sudah memenuhi kuota pada " + lingkup
				+ ".\nKuota: " + kuota + ", sudah terisi: " + terpakai
				+ ".\nSilakan pilih prodi lain atau hubungi panitia PMB.";
		try {
			MyMessageboxConfig.show(pesan, "Kuota Prodi Penuh", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
		} catch (InterruptedException e) {
			try {
				Thread.currentThread().interrupt();
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:4510");
			}
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:4515");
			}
		}
	}


	/**
	 * Guard anti-spam untuk pesan error tanggal lahir. {@code setdata()} dipanggil ulang tiap kali
	 * field APAPUN pada form berubah (lihat {@code masukkanPerubahan}/{@code masukkanListener}
	 * baris ~4384), jadi bila {@link #tanggalLahir} berisi teks tidak valid, pesan akan berpotensi
	 * muncul berulang-ulang setiap kali user mengisi field lain yang TIDAK terkait. Flag ini
	 * memastikan pesan hanya tampil SEKALI sampai isian tanggal lahir diperbaiki (parse berhasil).
	 */
	private boolean errorTanggalLahirSudahDitampilkan = false;

	private void setdata() {

		try {

			String info = BiodataCalonMahasiswaAction.infoMahasiswaBaru(infoKampusDariMana);
			Paket pkt = getPaketDipilihDenganFallback(biodataCalonMahasiswa.getPaket());
			if (tampilkanUsernameDanPasswordPadaFormPMB && username != null) {
				biodataCalonMahasiswa.setUsername(username.getValue().trim());
				biodataCalonMahasiswa.setPassword(Common.desEncrypter.get().encrypt(password.getValue().trim()));
			}

			biodataCalonMahasiswa.setNisn(nisn.getValue().trim());
			biodataCalonMahasiswa
					.setGelar((String) (gelar.getSelectedItem() == null ? "" : gelar.getSelectedItem().getValue()));
			biodataCalonMahasiswa.setKk(kk.getValue());
			biodataCalonMahasiswa.setPindahanDariProdi(pindahanDariProdi.getValue().trim());
			biodataCalonMahasiswa.setPernyataan(pernyataan.isChecked());
			biodataCalonMahasiswa.setDusunCalon(dusunCalon.getValue());
			biodataCalonMahasiswa.setAlamatSama(alamatSama.isChecked());
			if (!isCalonMahasiswaLogin() || bolehCalonMahasiswaMengubahGelombangPendaftaran()
					|| biodataCalonMahasiswa.getGelombangPendaftaran() == null) {
				GelombangPendaftaran gelombangDipilih = getGelombangDipilihDenganFallback(
						isCalonMahasiswaLogin() ? biodataCalonMahasiswa.getGelombangPendaftaran() : null);
				if (gelombangDipilih != null || !isCalonMahasiswaLogin()
						|| biodataCalonMahasiswa.getGelombangPendaftaran() == null) {
					biodataCalonMahasiswa.setGelombangPendaftaran(gelombangDipilih);
				}
			}
			biodataCalonMahasiswa.setTahunAkademik(
					getSelectedStringValue(tahunAkademik, biodataCalonMahasiswa.getTahunAkademik()));
			try {
				// FIX WrongValueException "You must specify a date": sama seperti tanggalLahir di
				// bawah -- Datebox.getValue() melempar exception bila user mengetik teks tanggal
				// tidak valid. setdata() dipanggil live per-field (onChange), jangan sampai field
				// lain gagal tersimpan gara-gara input tanggalPendaftaran yang belum valid.
				biodataCalonMahasiswa.setTanggalPendaftaran(tanggalPendaftaran.getValue());
			} catch (WrongValueException e) {
				try { tanggalPendaftaran.clearErrorMessage(); } catch (Exception ce) { ais.common.ErrorAuditUtil.record(ce, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:tanggalPendaftaran"); }
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
			biodataCalonMahasiswa.setNama(nama.getValue());
			biodataCalonMahasiswa.setRt(rt.getValue());
			biodataCalonMahasiswa.setRw(rw.getValue());
			biodataCalonMahasiswa.setKodePos(kodePos.getValue());

			biodataCalonMahasiswa.setKonsentrasi((Konsentrasi) (konsentrasi.getSelectedItem() == null ? null
					: konsentrasi.getSelectedItem().getValue()));

			biodataCalonMahasiswa.setTempatLahir(tempatLahir.getValue());
			try {
				biodataCalonMahasiswa.setTanggalLahir(tanggalLahir.getValue());
				// Parse berhasil -> reset guard supaya pesan bisa tampil lagi bila nanti user
				// mengetik ulang isian yang tidak valid.
				errorTanggalLahirSudahDitampilkan = false;
			} catch (WrongValueException e) {
				// Datebox.getValue() melempar WrongValueException "You must specify a date.
				// Format: dd-MM-yyyy" bila user mengetik teks tanggal lahir manual yang tidak
				// valid. setdata() ini dipanggil live oleh listener onChange SETIAP field pada
				// form (bukan hanya tanggalLahir), jadi JANGAN lempar ulang (akan menggagalkan
				// alur pengisian field lain yang tidak terkait) -- cukup lewati field ini saja,
				// field lain tetap tersimpan normal ke biodataCalonMahasiswa di bawah.
				try { tanggalLahir.clearErrorMessage(); } catch (Exception ce) { ais.common.ErrorAuditUtil.record(ce, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:tanggalLahir"); }
				ais.common.Common.tampilErrorJikaAdmin(e);
				if (!errorTanggalLahirSudahDitampilkan) {
					errorTanggalLahirSudahDitampilkan = true;
					MyMessageboxConfig.show(
							"Format tanggal lahir tidak valid, gunakan dd-MM-yyyy. (" + e.getMessage() + ")",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:4562");
				// TODO: handle exception
			}

			biodataCalonMahasiswa.setAlamat(alamat.getValue());
			biodataCalonMahasiswa.setNamaAyah(namaAyah.getValue());
			biodataCalonMahasiswa.setNamaIbu(namaIbu.getValue());

			biodataCalonMahasiswa.setTeleponRumah(teleponRumah.getValue());

			biodataCalonMahasiswa.setNoTelpSekolah(noTelpSekolah.getValue());

			biodataCalonMahasiswa.setAsalSma(asalSma.getAttribute("namaSekolahAsal") == null ? "" : asalSma.getValue());
			biodataCalonMahasiswa.setNamaSekolahAsal((NamaSekolahAsal) asalSma.getAttribute("namaSekolahAsal"));
			biodataCalonMahasiswa.setAlamatAsalSma(alamatAsalSma.getValue());
			if (dariNamaDosenKaryawan != null)
				biodataCalonMahasiswa.setDariNamaDosenKaryawan(dariNamaDosenKaryawan.getValue());
			biodataCalonMahasiswa.setStatusNikah((Integer) (statusNikah.getSelectedItem() == null ? null
					: statusNikah.getSelectedItem().getValue()));
			biodataCalonMahasiswa.setKewarganegaraan((String) (kewarganegaraan.getSelectedItem() == null ? null
					: kewarganegaraan.getSelectedItem().getValue()));
			biodataCalonMahasiswa
					.setAgama(agama.getSelectedItem() == null ? null : (Agama) agama.getSelectedItem().getValue());

			biodataCalonMahasiswa.setJenisKelamin(
					jenisKelamin.getSelectedItem() == null ? null : (String) jenisKelamin.getSelectedItem().getValue());

			biodataCalonMahasiswa.setAsalNegara(
					(Negara) (asalNegara.getSelectedItem() == null ? null : asalNegara.getSelectedItem().getValue()));

			biodataCalonMahasiswa.setJenisKartuIdentitas(jenisKartuIdentitas.getSelectedItem() == null ? null
					: (JenisKartuIdentitasMahasiswaBaru) jenisKartuIdentitas.getSelectedItem().getValue());
			biodataCalonMahasiswa.setNoIdentitas(noIdentitas.getValue());
			biodataCalonMahasiswa.setEmail(email.getValue());
			biodataCalonMahasiswa.setKelurahanCalon(kelurahanCalon.getValue());
			biodataCalonMahasiswa.setKecamatanCalon((Wilayah) kecamatanCalon.getAttribute("wilayah"));
			biodataCalonMahasiswa.setPropinsiCalon((Propinsi) (propinsiCalon.getAttribute("wilayah")));
			biodataCalonMahasiswa.setKotaCalon((Kota) (kotaCalon.getAttribute("wilayah")));
			biodataCalonMahasiswa.setJenisSekolah(jenisSekolah.getSelectedItem() == null ? null
					: (JenisSekolahMahasiswaBaru) jenisSekolah.getSelectedItem().getValue());
			biodataCalonMahasiswa.setAkreditasiSekolah(akreditasiSekolah.getSelectedItem() == null
					|| akreditasiSekolah.getSelectedItem().getValue() == null ? null
							: akreditasiSekolah.getSelectedItem().getValue().toString());

			biodataCalonMahasiswa.setInfoKampusDariMana(info);
			biodataCalonMahasiswa.setKeteranganInfoKampusDariMana(
					keteranganInfoKampusDariMana == null ? "" : keteranganInfoKampusDariMana.getValue());
			biodataCalonMahasiswa.setNamaTemanInfoKampusDariMana(
					namaTemanInfoKampusDariMana == null ? "" : namaTemanInfoKampusDariMana.getValue());

			biodataCalonMahasiswa.setKodePosSekolah(kodePosSekolah.getValue());
			biodataCalonMahasiswa.setKecamatanSekolah((Wilayah) kecamatanSekolah.getAttribute("wilayah"));
			biodataCalonMahasiswa.setPropinsiSekolah((Propinsi) (propinsiSekolah.getAttribute("wilayah")));
			biodataCalonMahasiswa.setKotaSekolah((Kota) (kotaSekolah.getAttribute("wilayah")));
			biodataCalonMahasiswa.setTahunKelulusan(
					tahunKelulusan.getValue() == null ? biodataCalonMahasiswa.getTahunKelulusan()
							: tahunKelulusan.getValue().toString());
			biodataCalonMahasiswa.setJurusanSekolah(jurusanSekolah.getSelectedItem() == null ? null
					: (JurusanSekolahMahasiswaBaru) jurusanSekolah.getSelectedItem().getValue());
			biodataCalonMahasiswa.setAlamatOrtu(alamatOrtu.getValue());
			biodataCalonMahasiswa.setNamaWali(namaWali.getValue());
			biodataCalonMahasiswa.setNoTelpOrtu(noTelpOrtu.getValue());

			biodataCalonMahasiswa.setRtOrtu(rtOrtu.getValue());
			biodataCalonMahasiswa.setRwOrtu(rwOrtu.getValue());
			biodataCalonMahasiswa.setKodePosOrtu(kodePosOrtu.getValue());
			biodataCalonMahasiswa.setKecamatanOrtu((Wilayah) kecamatanOrtu.getAttribute("wilayah"));
			biodataCalonMahasiswa.setKelurahanOrtu(kelurahanOrtu.getValue());
			biodataCalonMahasiswa.setPropinsiOrtu((Propinsi) (propinsiOrtu.getAttribute("wilayah")));
			biodataCalonMahasiswa.setKotaOrtu((Kota) (kotaOrtu.getAttribute("wilayah")));

			biodataCalonMahasiswa.setPaket(pkt);

			biodataCalonMahasiswa.setProdi1(
					getJurusanDipilihDenganFallback(prodi1, biodataCalonMahasiswa.getProdi1()));
			biodataCalonMahasiswa.setProdi2(
					getJurusanDipilihDenganFallback(prodi2, biodataCalonMahasiswa.getProdi2()));

			biodataCalonMahasiswa.setProdi3(
					getJurusanDipilihDenganFallback(prodi3, biodataCalonMahasiswa.getProdi3()));

			biodataCalonMahasiswa.setProdi4(
					getJurusanDipilihDenganFallback(prodi4, biodataCalonMahasiswa.getProdi4()));

			biodataCalonMahasiswa.setProdi5(
					getJurusanDipilihDenganFallback(prodi5, biodataCalonMahasiswa.getProdi5()));

			Jurusan prodiLulusUntukJenjang = biodataCalonMahasiswa.getProdiLulus();
			if (prodiLulusUntukJenjang != null && prodiLulusUntukJenjang.getJenjang() != null) {
				biodataCalonMahasiswa.setJenjang(prodiLulusUntukJenjang.getJenjang());
			} else if (biodataCalonMahasiswa.getProdi1() != null
					&& biodataCalonMahasiswa.getProdi1().getJenjang() != null) {
				biodataCalonMahasiswa.setJenjang(biodataCalonMahasiswa.getProdi1().getJenjang());
			} else {
				biodataCalonMahasiswa.setJenjang(ConstantValues.s1);
			}

			biodataCalonMahasiswa.setProgram(getSelectedStringValue(program, biodataCalonMahasiswa.getProgram()));

			biodataCalonMahasiswa.setJurusanSekolahLain(jurusanSekolahLain.getValue());

			biodataCalonMahasiswa
					.setPindahanDari((PerguruanTinggiLain) pindahanDari.getAttribute("perguruanTinggiLain"));

			biodataCalonMahasiswa.setPekerjaanAyah(pekerjaanAyah.getSelectedItem() == null ? null
					: (PekerjaanOrangTua) pekerjaanAyah.getSelectedItem().getValue());
			biodataCalonMahasiswa.setPendapatanOrtu(pendapatanOrtu.getSelectedItem() == null ? null
					: (PendapatanOrangTua) pendapatanOrtu.getSelectedItem().getValue());
			biodataCalonMahasiswa.setPendidikanOrtu(pendidikanOrtu.getSelectedItem() == null ? null
					: (PendidikanOrangTua) pendidikanOrtu.getSelectedItem().getValue());

			biodataCalonMahasiswa.setPekerjaanAyahIbu(pekerjaanAyahIbu.getSelectedItem() == null ? null
					: (PekerjaanOrangTua) pekerjaanAyahIbu.getSelectedItem().getValue());
			biodataCalonMahasiswa.setPendapatanOrtuIbu(pendapatanOrtuIbu.getSelectedItem() == null ? null
					: (PendapatanOrangTua) pendapatanOrtuIbu.getSelectedItem().getValue());
			biodataCalonMahasiswa.setPendidikanOrtuIbu(pendidikanOrtuIbu.getSelectedItem() == null ? null
					: (PendidikanOrangTua) pendidikanOrtuIbu.getSelectedItem().getValue());

			biodataCalonMahasiswa.setPekerjaanAyahWali(pekerjaanAyahWali.getSelectedItem() == null ? null
					: (PekerjaanOrangTua) pekerjaanAyahWali.getSelectedItem().getValue());
			biodataCalonMahasiswa.setPendapatanOrtuWali(pendapatanOrtuWali.getSelectedItem() == null ? null
					: (PendapatanOrangTua) pendapatanOrtuWali.getSelectedItem().getValue());
			biodataCalonMahasiswa.setPendidikanOrtuWali(pendidikanOrtuWali.getSelectedItem() == null ? null
					: (PendidikanOrangTua) pendidikanOrtuWali.getSelectedItem().getValue());

			biodataCalonMahasiswa.setPinPassword(pinPassword.getValue());

			Random random = new Random();
			Integer pin = random.nextInt(99999);
			biodataCalonMahasiswa.setPin(pin);

			Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			Date dateWithoutTime = cal.getTime();

			biodataCalonMahasiswa.setTanggalDaftar(dateWithoutTime);

			biodataCalonMahasiswa.setMerupakanPindahan(merupakanPindahan.isChecked());
			biodataCalonMahasiswa.setPindahanDariKampus(pindahanDariKampus.getValue());
			biodataCalonMahasiswa.setPindahDariKampusLamaDiSemester(pindahDariKampusLamaDiSemester.getValue());
			biodataCalonMahasiswa.setKeteranganPindah(keteranganPindah.getValue());
			biodataCalonMahasiswa.setNimLamaSebelumPindah(nimPindahan.getValue());
			biodataCalonMahasiswa.setPinPassword(pinPassword.getValue().trim());

			biodataCalonMahasiswa
					.setJenisSeleksi(getJenisSeleksiDipilihDenganFallback(biodataCalonMahasiswa.getJenisSeleksi()));

			biodataCalonMahasiswa.setKeterangan(keterangan.getValue());

			if (mahasiswaAlumni != null) {
				biodataCalonMahasiswa.setMahasiswaAlumni(mahasiswaAlumni);
			}

			biodataCalonMahasiswa.setInstansiAsal(instansiAsal == null ? "" : instansiAsal.getValue());
			biodataCalonMahasiswa
					.setKotaInstansi((Wilayah) (kotaInstansi == null ? null : kotaInstansi.getAttribute("wilayah")));
			biodataCalonMahasiswa
					.setJabatanDiInstansiAsal(jabatanDiInstansiAsal == null ? "" : jabatanDiInstansiAsal.getValue());

			if (biodataCalonMahasiswa.getNoRegistrasi() == null
					|| biodataCalonMahasiswa.getNoRegistrasi().trim().isEmpty()) {
				biodataCalonMahasiswa.setNoRegistrasi(CommonPMB.generateNoRegistrasi(biodataCalonMahasiswa));
			} else {
				biodataCalonMahasiswa.setNoRegistrasi(noRegistrasi.getValue());
			}

			biodataCalonMahasiswa.setNoUjian(noUjianData.getValue().trim());
			biodataCalonMahasiswa.setBahasa(
					(String) (bahasa.getSelectedItem() == null ? null : bahasa.getSelectedItem().getValue()));

			if (afiliasiCalonMahasiswaData != null) {
				biodataCalonMahasiswa.setAfiliasiCalonMahasiswa(afiliasiCalonMahasiswaData);
			} else {

				biodataCalonMahasiswa.setAfiliasiCalonMahasiswa(null);
				biodataCalonMahasiswa.setAfiliasiPegawai(null);
				biodataCalonMahasiswa.setAfiliasiMahasiswa(null);

				String val = jenisAfiliasi.getSelectedItem() == null
						|| jenisAfiliasi.getSelectedItem().getValue() == null ? "Umum"
								: jenisAfiliasi.getSelectedItem().getValue().toString();

				if (val.equals("Umum")) {
					biodataCalonMahasiswa.setAfiliasiCalonMahasiswa(
							(AfiliasiCalonMahasiswa) afiliasiCalonMahasiswa.getAttribute("afiliasiCalonMahasiswa"));
				} else if (val.equals("Pegawai/Dosen")) {
					biodataCalonMahasiswa.setAfiliasiPegawai((Pegawai) afiliasiPegawai.getAttribute("pegawai"));
				} else if (val.equals("Mahasiswa")) {
					biodataCalonMahasiswa.setAfiliasiMahasiswa((Mahasiswa) afiliasiMahasiswa.getAttribute("mahasiswa"));
				}

			}

			parameterTambahanListener.onSave(biodataCalonMahasiswa);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:4753");
		}

	}

	public boolean onSave(Event event) throws InterruptedException {

		try {
			Tbmuser tbmuser = Common.getCurrentUser();
			String tahunAkademikDipilih = getSelectedStringValue(tahunAkademik,
					biodataCalonMahasiswa.getTahunAkademik());
			if (tahunAkademikDipilih == null || tahunAkademikDipilih.trim().isEmpty()) {
				MyMessageboxConfig.show("Mohon maaf, Tahun Akademik belum dipilih. Langkah yang dapat dilakukan: (1) pilih Tahun Akademik dari daftar dropdown yang tersedia; (2) pastikan data tahun akademik sudah terdaftar di sistem; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				tahunAkademik.focus();
				return false;
			}

			if (getGelombangDipilihDenganFallback(biodataCalonMahasiswa.getGelombangPendaftaran()) == null) {
				MyMessageboxConfig.show("Mohon maaf, Gelombang Pendaftaran belum dipilih. Langkah yang dapat dilakukan: (1) pilih Gelombang Pendaftaran dari daftar dropdown yang tersedia; (2) pastikan gelombang pendaftaran sudah aktif dan tersedia; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				tahunAkademik.focus();
				return false;
			}

			if (getJenisSeleksiDipilihDenganFallback(biodataCalonMahasiswa.getJenisSeleksi()) == null) {
				MyMessageboxConfig.show("Mohon maaf, Jenis Seleksi belum dipilih. Langkah yang dapat dilakukan: (1) pilih Jenis Seleksi dari daftar dropdown yang tersedia; (2) pastikan jenis seleksi sudah dikonfigurasi di sistem; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				jenisSeleksi.focus();
				return false;
			}

			if (!nisn.getValue().isEmpty() && nisn.getValue().trim().length() != 10) {
				MyMessageboxConfig.show("Mohon maaf, NISN yang dimasukkan tidak valid karena harus 10 digit. Langkah yang dapat dilakukan: (1) periksa kembali NISN di ijazah atau kartu identitas sekolah; (2) pastikan NISN terdiri dari tepat 10 angka tanpa spasi atau tanda baca; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				nisn.focus();
				return false;
			}

			if (jenisKartuIdentitas.getValue() != null
					&& jenisKartuIdentitas.getValue().toLowerCase().contains("ktp")) {
				if (!noIdentitas.getValue().isEmpty() && noIdentitas.getValue().trim().length() != 16) {
					MyMessageboxConfig.show("Mohon maaf, NIK (Nomor Induk Kependudukan) yang dimasukkan tidak valid karena harus 16 digit. Langkah yang dapat dilakukan: (1) periksa kembali NIK di KTP atau Kartu Keluarga; (2) pastikan NIK terdiri dari tepat 16 angka tanpa spasi; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					noIdentitas.focus();
					return false;
				}
			}

			if (KonfigurasiTampilanBiodataCalonMahasiswaAction.wajibIsi(tbmuser, "kewarganegaraan")) {
				if (kewarganegaraan.getSelectedItem() == null || kewarganegaraan.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, Kewarganegaraan belum dipilih. Langkah yang dapat dilakukan: (1) pilih Kewarganegaraan dari daftar dropdown yang tersedia; (2) jika WNA, pastikan pilihan kewarganegaraan asing tersedia di sistem; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					kewarganegaraan.focus();
					return false;
				}
			}

			if (KonfigurasiTampilanBiodataCalonMahasiswaAction.wajibIsi(tbmuser, "asalNegara")) {
				if (asalNegara.getSelectedItem() == null || asalNegara.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, Asal Negara belum dipilih. Langkah yang dapat dilakukan: (1) pilih Asal Negara dari daftar dropdown yang tersedia; (2) pastikan daftar negara sudah tersedia di sistem; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					asalNegara.focus();
					return false;
				}
			}

			Date myTanggalLahirVal = null;
			try {
				myTanggalLahirVal = tanggalLahir.getValue();
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:4822");}
			if (myTanggalLahirVal != null) {
				GelombangPendaftaran myGelombangPendaftaran = getGelombangDipilihDenganFallback(
						biodataCalonMahasiswa.getGelombangPendaftaran());
				if (myGelombangPendaftaran == null || myGelombangPendaftaran.getDibatasiUmur() == null
						|| myGelombangPendaftaran.getUmurmaksimal() == null) {
					if (Common.bolehKonfigurasi("umur_calon_mahasiswa_dibatasi", Konfigurasi.TIDAK_AKTIF)) {
						try {
							int umur = Integer.parseInt(Common
									.getKonfigurasi("nilai_umur_calon_mahasiswa_dibatasi", "27").getNilai().trim());

							int umurCalonMahasiwa = Years
									.yearsBetween(new org.joda.time.DateTime(myTanggalLahirVal),
											new org.joda.time.DateTime(ais.ui.util.WaktuUtil.getDate()))
									.getYears();
							System.out.println("umur => " + umur + ", umurCalonMahasiwa =>" + umurCalonMahasiwa);
							if (umurCalonMahasiwa > umur) {
								MyMessageboxConfig.show(
										"Umur calon mahasiswa yang diperbolehkan untuk mendaftar adalah " + umur
												+ " tahun, sedangkan umur Anda adalah " + umurCalonMahasiwa + " tahun",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								return false;
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				} else {
					if (myGelombangPendaftaran.getDibatasiUmur()) {
						try {
							int umur = myGelombangPendaftaran.getUmurmaksimal();
							int umurMin = myGelombangPendaftaran.getUmurminimal();

							int umurCalonMahasiwa = Years
									.yearsBetween(new org.joda.time.DateTime(myTanggalLahirVal),
											new org.joda.time.DateTime(ais.ui.util.WaktuUtil.getDate()))
									.getYears();
							System.out.println("umur => " + umur + ", umurCalonMahasiwa =>" + umurCalonMahasiwa);
							if (umurCalonMahasiwa > umur) {
								MyMessageboxConfig.show(
										"Umur maksimal calon mahasiswa yang diperbolehkan untuk mendaftar adalah "
												+ umur + " tahun, sedangkan umur Anda adalah " + umurCalonMahasiwa
												+ " tahun",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								return false;
							}
							if (umurCalonMahasiwa < umurMin) {
								MyMessageboxConfig.show(
										"Umur minimal calon mahasiswa yang diperbolehkan untuk mendaftar adalah "
												+ umurMin + " tahun, sedangkan umur Anda adalah " + umurCalonMahasiwa
												+ " tahun",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								return false;
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				}
			}

			if (tampilkanUsernameDanPasswordPadaFormPMB && username != null && username.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Mohon maaf, Username belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Username dengan nama pengguna yang unik; (2) gunakan format email atau kombinasi huruf dan angka tanpa spasi; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				username.focus();
				return false;
			}

			if (tampilkanUsernameDanPasswordPadaFormPMB && password != null && password.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Mohon maaf, Password belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Password dengan kata sandi yang kuat minimal 6 karakter; (2) pastikan password tidak mengandung karakter yang tidak diijinkan; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				username.focus();
				return false;
			}

			if (usernameHarusMenggunakanFormatEmail && username != null) {
				email.setValue(username.getValue().trim());
			}

			if (emailWajibDiisi && email.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Mohon maaf, Email belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Email dengan alamat email yang aktif dan valid; (2) pastikan format email menggunakan tanda @ dan domain yang benar; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				email.focus();
				return false;
			}

			if (email != null && !email.getValue().trim().isEmpty()
					&& !Common.isValidEmailAddress(email.getValue().trim())) {
				MyMessageboxConfig.show("Mohon maaf, format Email yang dimasukkan tidak valid. Langkah yang dapat dilakukan: (1) periksa kembali alamat email; pastikan mengandung tanda @ dan domain yang valid (contoh: nama@email.com); (2) hapus spasi atau karakter khusus yang tidak diijinkan; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}

			if (prodi1.getSelectedItem() == null || prodi1.getSelectedItem().getValue() == null) {
				if (biodataCalonMahasiswa.getProdi1() != null) {
					Common.selectComboItem(true, prodi1, biodataCalonMahasiswa.getProdi1());
				}
			}
			if (prodi2.getSelectedItem() == null || prodi2.getSelectedItem().getValue() == null) {
				if (biodataCalonMahasiswa.getProdi2() != null) {
					Common.selectComboItem(true, prodi2, biodataCalonMahasiswa.getProdi2());
				}
			}
			if (prodi3.getSelectedItem() == null || prodi3.getSelectedItem().getValue() == null) {
				if (biodataCalonMahasiswa.getProdi3() != null) {
					Common.selectComboItem(true, prodi3, biodataCalonMahasiswa.getProdi3());
				}
			}
			if (prodi4.getSelectedItem() == null || prodi4.getSelectedItem().getValue() == null) {
				if (biodataCalonMahasiswa.getProdi4() != null) {
					Common.selectComboItem(true, prodi4, biodataCalonMahasiswa.getProdi4());
				}
			}
			if (prodi5.getSelectedItem() == null || prodi5.getSelectedItem().getValue() == null) {
				if (biodataCalonMahasiswa.getProdi5() != null) {
					Common.selectComboItem(true, prodi5, biodataCalonMahasiswa.getProdi5());
				}
			}

			Paket pkt = null;
			if (paket != null || (myPaket != null && myPaket.getId() != null)) {
				pkt = getPaketDipilihDenganFallback(biodataCalonMahasiswa.getPaket());
				if (pkt == null) {
					MyMessageboxConfig.show("Mohon maaf, Paket Pendaftaran belum dipilih. Langkah yang dapat dilakukan: (1) pilih Paket Pendaftaran yang sesuai dari daftar dropdown; (2) pastikan paket pendaftaran sudah tersedia dan aktif; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					paket.focus();
					return false;
				}

				if (pkt != null && !tampilSederhana) {

					if (getJurusanDipilihDenganFallback(prodi1, biodataCalonMahasiswa.getProdi1()) == null) {
						MyMessageboxConfig.show("Mohon maaf, Program Studi pilihan pertama belum dipilih. Langkah yang dapat dilakukan: (1) pilih Program Studi pilihan pertama dari daftar dropdown; (2) pastikan prodi yang dipilih sesuai dengan paket pendaftaran; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						prodi1.focus();
						return false;
					}

					if (getJurusanDipilihDenganFallback(prodi2, biodataCalonMahasiswa.getProdi2()) == null
							&& pkt.getJumlahProdiYgBolehDiambil() > 1) {
						MyMessageboxConfig.show("Mohon maaf, Program Studi pilihan ke-2 belum dipilih. Langkah yang dapat dilakukan: (1) pilih Program Studi pilihan ke-2 dari daftar dropdown; (2) pilih prodi yang berbeda dari pilihan pertama; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						prodi2.focus();
						return false;
					}
					if (getJurusanDipilihDenganFallback(prodi3, biodataCalonMahasiswa.getProdi3()) == null
							&& pkt.getJumlahProdiYgBolehDiambil() > 2) {
						MyMessageboxConfig.show("Mohon maaf, Program Studi pilihan ke-3 belum dipilih. Langkah yang dapat dilakukan: (1) pilih Program Studi pilihan ke-3 dari daftar dropdown; (2) pilih prodi yang berbeda dari pilihan sebelumnya; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						prodi3.focus();
						return false;
					}

					if (getJurusanDipilihDenganFallback(prodi4, biodataCalonMahasiswa.getProdi4()) == null
							&& pkt.getJumlahProdiYgBolehDiambil() > 3) {
						MyMessageboxConfig.show("Mohon maaf, Program Studi pilihan ke-4 belum dipilih. Langkah yang dapat dilakukan: (1) pilih Program Studi pilihan ke-4 dari daftar dropdown; (2) pilih prodi yang berbeda dari pilihan sebelumnya; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						prodi4.focus();
						return false;
					}

					if (getJurusanDipilihDenganFallback(prodi5, biodataCalonMahasiswa.getProdi5()) == null
							&& pkt.getJumlahProdiYgBolehDiambil() > 4) {
						MyMessageboxConfig.show("Mohon maaf, Program Studi pilihan ke-5 belum dipilih. Langkah yang dapat dilakukan: (1) pilih Program Studi pilihan ke-5 dari daftar dropdown; (2) pilih prodi yang berbeda dari pilihan sebelumnya; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						prodi5.focus();
						return false;
					}

					Jurusan jurusan = null;
					if (pkt != null && !pkt.getBisaMemilihPilihanYangSama()) {

						List<Long> indJur = new ArrayList<Long>();
						jurusan = getJurusanDipilihDenganFallback(prodi1, biodataCalonMahasiswa.getProdi1());
						if (jurusan != null) {
							indJur.add(jurusan.getId());
						}
						jurusan = getJurusanDipilihDenganFallback(prodi2, biodataCalonMahasiswa.getProdi2());
						if (jurusan != null) {
							if (indJur.contains(jurusan.getId())) {
								MyMessageboxConfig.show(
										"Prodi pilihan " + jurusan.getNama()
												+ " tidak bisa dipilih lebih dari satu kali",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								prodi2.focus();
								return false;
							}
							indJur.add(jurusan.getId());
						}
						jurusan = getJurusanDipilihDenganFallback(prodi3, biodataCalonMahasiswa.getProdi3());
						if (jurusan != null) {
							if (indJur.contains(jurusan.getId())) {
								MyMessageboxConfig.show(
										"Prodi pilihan " + jurusan.getNama()
												+ " tidak bisa dipilih lebih dari satu kali",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								prodi3.focus();
								return false;
							}
							indJur.add(jurusan.getId());
						}
						jurusan = getJurusanDipilihDenganFallback(prodi4, biodataCalonMahasiswa.getProdi4());
						if (jurusan != null) {
							if (indJur.contains(jurusan.getId())) {
								MyMessageboxConfig.show(
										"Prodi pilihan " + jurusan.getNama()
												+ " tidak bisa dipilih lebih dari satu kali",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								prodi4.focus();
								return false;
							}
							indJur.add(jurusan.getId());
						}
						jurusan = getJurusanDipilihDenganFallback(prodi5, biodataCalonMahasiswa.getProdi5());
						if (jurusan != null) {
							if (indJur.contains(jurusan.getId())) {
								MyMessageboxConfig.show(
										"Prodi pilihan " + jurusan.getNama()
												+ " tidak bisa dipilih lebih dari satu kali",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								prodi5.focus();
								return false;
							}
							indJur.add(jurusan.getId());
						}
					}

					if (jenisSekolah.getSelectedItem() == null) {
						MyMessageboxConfig.show("Mohon maaf, Jenis Pendidikan Asal belum dipilih. Langkah yang dapat dilakukan: (1) pilih Jenis Pendidikan Asal dari daftar dropdown yang tersedia; (2) pastikan jenis sekolah asal tersedia di sistem; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						asalNegara.focus();
						return false;
					}

					if (jurusanSekolah.getSelectedItem() == null) {
						MyMessageboxConfig.show(Common.getBahasaConfig("Jurusan") + " Pendidikan Asal harus diisi",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						asalNegara.focus();
						return false;
					}

					if (getSelectedStringValue(program, biodataCalonMahasiswa.getProgram()) == null) {
						MyMessageboxConfig.show("Mohon maaf, Program belum dipilih. Langkah yang dapat dilakukan: (1) pilih Program dari daftar dropdown yang tersedia; (2) pastikan program sudah terdaftar di sistem; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						program.focus();
						return false;
					}

					List<Jurusan> jurusans = new ArrayList<Jurusan>();
					jurusan = getJurusanDipilihDenganFallback(prodi1, biodataCalonMahasiswa.getProdi1());
					if (jurusan != null) {
						jurusans.add(jurusan);
					}
					jurusan = getJurusanDipilihDenganFallback(prodi2, biodataCalonMahasiswa.getProdi2());
					if (jurusan != null) {
						jurusans.add(jurusan);
					}
					jurusan = getJurusanDipilihDenganFallback(prodi3, biodataCalonMahasiswa.getProdi3());
					if (jurusan != null) {
						jurusans.add(jurusan);
					}
					jurusan = getJurusanDipilihDenganFallback(prodi4, biodataCalonMahasiswa.getProdi4());
					if (jurusan != null) {
						jurusans.add(jurusan);
					}
					jurusan = getJurusanDipilihDenganFallback(prodi5, biodataCalonMahasiswa.getProdi5());
					if (jurusan != null) {
						jurusans.add(jurusan);
					}
					if (pkt != null) {
						if (jurusans.size() > 1 && !PersyaratanPilihanPaket.checkKombinasiPaket(pkt, jurusans)) {
							MyMessageboxConfig.show("untuk pilihan paket \"" + pkt.getNama()
									+ "\", kombinasi pilihan program studi tidak sesuai, coba pilih pilihan kembali !",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							this.paket.focus();
							return false;
						}
					}

					if (merupakanPindahan.isChecked()) {
						if (pindahanDariKampus.getValue().trim().isEmpty()) {
							MyMessageboxConfig.show(
									"Untuk calon mahasiswa pindahan, nama kampus sebelum pindah harus diisi",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							pindahanDariKampus.focus();
							return false;
						}

						if (pindahanDariProdi.getValue().trim().isEmpty()) {
							MyMessageboxConfig.show(
									"Untuk calon mahasiswa pindahan, nama program studi sebelum pindah harus diisi",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							pindahanDariProdi.focus();
							return false;
						}

						if (nimPindahan.getValue().trim().isEmpty()) {
							MyMessageboxConfig.show(
									"Untuk calon mahasiswa pindahan, NIM/NPM kampus sebelum pindah harus diisi",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							pindahanDariKampus.focus();
							return false;
						}
						if (pindahDariKampusLamaDiSemester.getValue() == null
								|| pindahDariKampusLamaDiSemester.getValue() <= 0) {
							MyMessageboxConfig.show(
									"Untuk calon mahasiswa pindahan, semester sebelum pindah harus diisi", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							pindahDariKampusLamaDiSemester.focus();
							return false;
						}
						if (keteranganPindah.getValue().trim().isEmpty()) {
							MyMessageboxConfig.show(
									"Untuk calon mahasiswa pindahan, keterangan / alasan pindah harus diisi",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							keteranganPindah.focus();
							return false;
						}
					}
				}
			}

			if (!validasiJenjangProdiSesuaiPaket()) {
				return false;
			}

			if (!validasiKuotaPaketJurusanPmb()) {
				return false;
			}

			if (rowkonsentrasi.isVisible()
					&& (konsentrasi.getSelectedItem() == null || konsentrasi.getSelectedItem().getValue() == null)) {
				MyMessageboxConfig.show("Mohon maaf, Konsentrasi belum dipilih. Langkah yang dapat dilakukan: (1) pilih Konsentrasi dari daftar dropdown yang tersedia; (2) pastikan konsentrasi sudah dikonfigurasi untuk program studi yang dipilih; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}

			String info = BiodataCalonMahasiswaAction.infoMahasiswaBaru(infoKampusDariMana);
			if (Common.bolehKonfigurasi("tampilkan_info_sekolah_dari_mana_pada_pmb", Konfigurasi.TIDAK_AKTIF)) {

				if (info.trim().isEmpty()) {
					MyMessageboxConfig.show(
							"\"Anda mendapatkan informasi Pendaftaran Mahasiswa Baru ini dari mana ?\" harus diisi",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					infoKampusDariMana.focus();
					return false;
				}

				if (rownamaTemanInfoKampusDariMana != null && rownamaTemanInfoKampusDariMana.isVisible()
						&& namaTemanInfoKampusDariMana != null
						&& namaTemanInfoKampusDariMana.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Mohon maaf, Nama Teman yang memberi informasi belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Teman dengan nama lengkap teman yang menginformasikan tentang pendaftaran ini; (2) pastikan kolom tidak kosong; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					namaTemanInfoKampusDariMana.focus();
					return false;
				}

				if (rowdariNamaDosenKaryawan != null && rowdariNamaDosenKaryawan.isVisible()
						&& dariNamaDosenKaryawan != null && dariNamaDosenKaryawan.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Mohon maaf, Nama Dosen atau Karyawan yang memberi informasi belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Dosen/Karyawan dengan nama lengkapnya; (2) pastikan kolom tidak kosong; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					dariNamaDosenKaryawan.focus();
					return false;
				}

				if (rowketeranganInfoKampusDariMana != null && rowketeranganInfoKampusDariMana.isVisible()
						&& keteranganInfoKampusDariMana != null
						&& keteranganInfoKampusDariMana.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Mohon maaf, keterangan sumber informasi pendaftaran belum diisi. Langkah yang dapat dilakukan: (1) isi kolom keterangan dengan sumber informasi yang Anda gunakan untuk mengetahui pendaftaran ini; (2) pastikan kolom tidak kosong; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					namaTemanInfoKampusDariMana.focus();
					return false;
				}
			}

			if (!pernyataan.isChecked()) {
				MyMessageboxConfig.show("Mohon maaf, Pernyataan Persetujuan belum dicentang. Langkah yang dapat dilakukan: (1) baca pernyataan persetujuan yang tertera pada formulir; (2) centang kotak persetujuan jika Anda menyetujui semua ketentuan yang berlaku; (3) ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				pernyataan.focus();
				return false;
			}

			if (!parameterTambahanListener.validate()) {
				return false;
			}

			Session session1 = HibernateUtil.currentNativeSession();

			AfiliasiCalonMahasiswa afil = (AfiliasiCalonMahasiswa) this.afiliasiCalonMahasiswa
					.getAttribute("afiliasiCalonMahasiswa");
			if (afil != null && afil.getId() != null) {
				Number s = ((Number) (session1.createCriteria(BiodataCalonMahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(biodataCalonMahasiswa.getId() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.ne("id", biodataCalonMahasiswa.getId()))
						.add(Restrictions.eq("afiliasiCalonMahasiswa", afil)).setProjection(Projections.rowCount())
						.uniqueResult()));

				Integer jmlAfiliasi = s == null ? 0 : s.intValue();

				System.out.println("jmlAfiliasi = " + jmlAfiliasi + " afil = " + afil);

				if (jmlAfiliasi >= afil.getKuotaDaftar()) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, kuota pendaftaran untuk afiliasi \"{V1}\" telah penuh, sehingga pendaftaran tidak dapat dilanjutkan. Langkah yang dapat dilakukan: (1) Periksa kembali pilihan afiliasi yang dituju; (2) Pilih afiliasi lain yang kuotanya masih tersedia; (3) Hubungi petugas administrasi penerimaan mahasiswa baru apabila memerlukan bantuan.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, afil.getNama());
					closeOpenedNativeSession(session1);
					return false;
				}
			}

			String ta = getSelectedStringValue(tahunAkademik, biodataCalonMahasiswa.getTahunAkademik());
			JenisSeleksi js = getJenisSeleksiDipilihDenganFallback(biodataCalonMahasiswa.getJenisSeleksi());

			GelombangPendaftaran gel = getGelombangDipilihDenganFallback(
					biodataCalonMahasiswa.getGelombangPendaftaran());
			// Baca tanggal lahir secara AMAN: Datebox.getValue() melempar WrongValueException
			// bila isian kosong/format salah. Bila tidak valid, lewati kriteria tanggal pada
			// pengecekan duplikat agar query tidak gagal (validasi wajib-isi ditangani terpisah).
			java.util.Date tglLahirAman = null;
			try {
				tglLahirAman = tanggalLahir.getValue();
			} catch (org.zkoss.zk.ui.WrongValueException wve) {
				tglLahirAman = null;
			}
			int count = ((Number) session1.createCriteria(BiodataCalonMahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

					.add(js != null && gel != null && !gel.getTidakBolehMendaftarMhsYgSama()
							? Restrictions.eq("jenisSeleksi", js)
							: Restrictions.sqlRestriction("true"))

					.add(gel != null && !gel.getTidakBolehMendaftarMhsYgSama()
							? Restrictions.eq("gelombangPendaftaran", gel)
							: Restrictions.sqlRestriction("true"))

					.add(Restrictions.eq("tahunAkademik", ta)).setProjection(Projections.rowCount())

					.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.EXACT))

					.add(Restrictions.ilike("namaIbu", namaIbu.getValue().trim(), MatchMode.EXACT))

					.add(biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null
							? Restrictions.ne("id", biodataCalonMahasiswa.getId())
							: Restrictions.sqlRestriction("true"))

					.add(tglLahirAman == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("tanggalLahir", tglLahirAman))

					.uniqueResult()).intValue();

//			System.out.println("count = " + count + ", nama = " + nama.getValue() + ", namaIbu = " + namaIbu.getValue()
//					+ ", tanggalLahir = " + Common.dateFormat4.get().format(tanggalLahir.getValue()));

			if (count > 0) {
				// Reuse tglLahirAman (sudah aman dr WrongValueException, lihat baris ~5236)
				// alih-alih tanggalLahir.getValue() mentah lagi di sini, supaya format pesan
				// duplikat ini juga tidak ikut melempar WrongValueException.
				String tglLahirStr = tglLahirAman == null ? "-" : Common.dateFormat2.get().format(tglLahirAman);
				String seleksiStr = js == null ? "" : "\nJenis Seleksi : " + js.getNama();
				MyMessageboxConfig.showFormat(
						"Mohon maaf, data pendaftaran dengan rincian berikut telah terdaftar sebelumnya.\nNama : {V1}\nTanggal Lahir : {V2}\nNama Ibu : {V3}{V4}\n\nLangkah yang dapat dilakukan: (1) Pastikan calon mahasiswa yang bersangkutan belum pernah mendaftar sebelumnya; (2) Apabila ini merupakan pendaftaran yang berbeda, sesuaikan data pembeda seperti gelombang atau jenis seleksi; (3) Hubungi petugas administrasi penerimaan mahasiswa baru apabila memerlukan bantuan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						nama.getValue(), tglLahirStr, namaIbu.getValue(), seleksiStr);
				closeOpenedNativeSession(session1);
				return false;
			}

			// Cek duplikat NIK hanya jika gelombang mengaktifkan opsi "NIK tidak boleh sama".
			if (gel != null && gel.getTidakBolehNikSama()
					&& noIdentitas != null && !noIdentitas.getValue().trim().isEmpty()) {
				String nikCek = noIdentitas.getValue().trim();
				int countNik = ((Number) session1.createCriteria(BiodataCalonMahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.ilike("noIdentitas", nikCek, MatchMode.EXACT))
						.add(biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null
								? Restrictions.ne("id", biodataCalonMahasiswa.getId())
								: Restrictions.sqlRestriction("true"))
						.setProjection(Projections.rowCount())
						.uniqueResult()).intValue();

				if (countNik > 0) {
					MyMessageboxConfig.show(
							"Nomor Induk Kependudukan (NIK) " + nikCek + " sudah terdaftar dalam sistem.\n"
							+ "Calon mahasiswa ini kemungkinan telah mendaftar sebelumnya pada gelombang atau program lain.\n"
							+ "Mohon periksa data yang sudah ada terlebih dahulu.\n"
							+ "Apabila membutuhkan bantuan, silakan hubungi petugas administrasi penerimaan mahasiswa baru.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					closeOpenedNativeSession(session1);
					noIdentitas.focus();
					return false;
				}
			}

			if (gel != null && gel.getFotoWajibDiuplad()) {
				FileFotoLain fileFotoLain = biodataCalonMahasiswa.getId() == null ? fotoBiodataCalonMahasiswa
						: FileFotoLain.ambil(false, biodataCalonMahasiswa.getId(),
								FotoBiodataCalonMahasiswa.DEFAULT_JENIS, FotoBiodataCalonMahasiswa.class, true);
				if (fileFotoLain == null || fileFotoLain.getId() == null) {
					MyMessageboxConfig.show(
							"Mohon maaf, foto calon mahasiswa wajib diunggah terlebih dahulu sebelum data dapat disimpan. Langkah yang dapat dilakukan: (1) Siapkan berkas pas foto sesuai ketentuan; (2) Klik tombol unggah foto lalu pilih berkas yang dimaksud; (3) Setelah foto berhasil terunggah, ulangi proses penyimpanan.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					closeOpenedNativeSession(session1);
					return false;
				}
			}

			final boolean update;
			if (biodataCalonMahasiswa.getId() != null) {
				update = true;
				biodataCalonMahasiswa = (BiodataCalonMahasiswa) session1.load(BiodataCalonMahasiswa.class,
						biodataCalonMahasiswa.getId());
			} else {
				update = false;
				biodataCalonMahasiswa = new BiodataCalonMahasiswa();
			}

			StatusAwalMahasiswa awalMahasiswa = biodataCalonMahasiswa.getStatusAwalMahasiswa();

			if (tampilkanUsernameDanPasswordPadaFormPMB && username != null && !username.getValue().trim().isEmpty()) {
				boolean i = Common.checkUsername(username.getValue().trim(), biodataCalonMahasiswa.getUsername(),
						biodataCalonMahasiswa.getId());
				if (i) {
					MyMessageboxConfig.show(
							"Username yang Anda masukkan telah terpakai, silahkan pilih username yang lain",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					username.focus();
					closeOpenedNativeSession(session1);
					return false;
				}
			}

			setdata();

			if (biodataCalonMahasiswa.getGelombangPendaftaran() != null
					&& biodataCalonMahasiswa.getGelombangPendaftaran().getDokumenHarusDiverivikasiSebelumBisaSimpan()) {
				if (!BiodataCalonMahasiswaAction.lengkap(biodataCalonMahasiswa)) {
					closeOpenedNativeSession(session1);
					return false;
				}
			}

			try {
				session1.getTransaction().begin();
				if (biodataCalonMahasiswa.getId() == null) {
					session1.save(biodataCalonMahasiswa);
				} else {
					Common.refreshUpdate(session1, biodataCalonMahasiswa);
				}
				session1.getTransaction().commit();
			} catch (org.hibernate.exception.ConstraintViolationException cve) {
				try { session1.getTransaction().rollback(); } catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:5367"); /* ignore */ }
				// Deteksi race condition no_registrasi: dua pendaftar bersamaan dapat noreg sama.
				// Hanya untuk entitas baru (id==null) → coba ulang hingga 3 kali dengan noreg baru.
				boolean berhasilRetry = false;
				if (biodataCalonMahasiswa.getId() == null) {
					String causeMsg = (cve.getCause() != null && cve.getCause().getMessage() != null)
							? cve.getCause().getMessage().toLowerCase() : "";
					if (causeMsg.contains("no_registrasi") || causeMsg.contains("noregistrasi")) {
						for (int attempt = 1; attempt <= 3; attempt++) {
							try {
								session1.clear();
								biodataCalonMahasiswa.setNoRegistrasi(
										CommonPMB.generateNoRegistrasi(biodataCalonMahasiswa));
								session1.getTransaction().begin();
								biodataCalonMahasiswa = (BiodataCalonMahasiswa) session1
										.merge(biodataCalonMahasiswa);
								session1.getTransaction().commit();
								berhasilRetry = true;
								break;
							} catch (Exception retryEx) {
								try { session1.getTransaction().rollback(); } catch (Exception ex3) { ais.common.ErrorAuditUtil.record(ex3, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:5387"); /* ignore */ }
							}
						}
					}
				}
				if (!berhasilRetry) {
					closeOpenedNativeSession(session1);
					String pesanKonflik = pesanKonflikDariException(cve);
					Common.tampilErrorJikaAdmin(cve);
					MyMessageboxConfig.show(pesanKonflik, "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} catch (org.hibernate.exception.DataException de) {
				try { session1.getTransaction().rollback(); } catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:5401"); /* ignore */ }
				closeOpenedNativeSession(session1);
				Common.tampilErrorJikaAdmin(de);
				MyMessageboxConfig.show(
						"Data tidak dapat disimpan karena terdapat isian yang melebihi batas panjang yang diizinkan. "
								+ "Mohon periksa kembali seluruh isian formulir Anda, terutama pada kolom alamat, "
								+ "nama lengkap, surel, nomor telepon, atau keterangan lainnya. "
								+ "Persingkat isian yang terlalu panjang, kemudian coba simpan kembali.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			} catch (org.hibernate.StaleObjectStateException sose) {
				try { session1.getTransaction().rollback(); } catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:5412"); /* ignore */ }
				closeOpenedNativeSession(session1);
				Common.tampilErrorJikaAdmin(sose);
				MyMessageboxConfig.show(
						"Data formulir yang Anda isi telah mengalami pembaruan oleh sistem secara bersamaan. "
								+ "Mohon muat ulang halaman ini, kemudian isi kembali formulir pendaftaran Anda dari awal. "
								+ "Apabila kendala tetap terjadi, silakan hubungi petugas administrasi penerimaan mahasiswa baru.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			} catch (Exception ex) {
				try { session1.getTransaction().rollback(); } catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:5422"); /* ignore */ }
				closeOpenedNativeSession(session1);
				Common.tampilErrorJikaAdmin(ex);
				MyMessageboxConfig.show(
						"Terjadi kendala pada sistem saat menyimpan data Anda. "
								+ "Mohon coba beberapa saat lagi. Apabila kendala tetap berlanjut, "
								+ "silakan hubungi petugas administrasi penerimaan mahasiswa baru "
								+ "dan sampaikan bahwa terdapat gangguan teknis saat proses penyimpanan data.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}

			session1.disconnect();
			session1.close();
			HibernateUtil.closeSession();

			if (update) {
				if (awalMahasiswa != null && biodataCalonMahasiswa.getStatusAwalMahasiswa() != null
						&& !biodataCalonMahasiswa.getStatusAwalMahasiswa().getId().equals(awalMahasiswa.getId())) {
					sinkronkanPembayaranSetelahStatusAwalBerubah(biodataCalonMahasiswa.getId());
				}
			} else {
				sinkronkanPembayaranSetelahPendaftaranBaru(biodataCalonMahasiswa.getId());
			}

			noRegistrasi.setValue(biodataCalonMahasiswa.getNoRegistrasi());

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null
							&& fotoBiodataCalonMahasiswa != null && fotoBiodataCalonMahasiswa.getId() != null) {
						try {
							Session session = StreamingHibernateUtil.getInstance().currentSession();

							session.refresh(fotoBiodataCalonMahasiswa);
							fotoBiodataCalonMahasiswa.setBiodataCalonMahasiswa(biodataCalonMahasiswa.getId());

							session.getTransaction().begin();
							session.update(fotoBiodataCalonMahasiswa);
							session.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}

					VerifikasiPMBHelper.simpanVerifikasi(biodataCalonMahasiswa,
							subRowsVerifikasiKelengkapanCalonMahasiswa);
					VerifikasiMatapelajaranPMBHelper.simpanVerifikasi(biodataCalonMahasiswa,
							subRowsVerifikasiNilaiRapor);
					VerifikasiParameterPMBHelper.simpanVerifikasi(biodataCalonMahasiswa,
							subRowsVerifikasiNilaiParameter);

					if (!lampiranLains.isEmpty()) {
						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
						streamingSession.getTransaction().begin();
						for (LampiranLain lampiranLain : lampiranLains.values()) {
							streamingSession.refresh(lampiranLain);
							lampiranLain.setRef(biodataCalonMahasiswa.getId());
							streamingSession.update(lampiranLain);
						}
						streamingSession.getTransaction().commit();
						StreamingHibernateUtil.getInstance().closeSession();
					}

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							parameterTambahanListener.onSave(biodataCalonMahasiswa);

							System.out.println(
									"biodataCalonMahasiswa => " + biodataCalonMahasiswa.getParameterTambahanInds());
							Common.refreshSaveOrUpdate(biodataCalonMahasiswa);

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									try {
										DiskonCalonMahasiswaAction.ambilDiskon(biodataCalonMahasiswa);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:5509");
										// TODO: handle exception
									}

									// PROSES TAGIHAN otomatis setelah simpan: buat/segarkan Kegiatan
									// pembayaran Registrasi & Daftar Ulang calon (pola sama dengan tombol
									// "Proses Tagihan") agar kolom "Pemb. Registrasi" dan "Pemb. Daftar
									// Ulang" langsung terisi di tabel tanpa menunggu proses manual.
									// chekPembayaran* membuka session sendiri dan aman dipanggil berulang
									// (idempoten: kegiatan yang sudah ada tidak dibuat ganda).
									try {
										biodataCalonMahasiswa.chekPembayaranRegistrasi();
									} catch (Exception eTagihan) {
										ais.common.ErrorAuditUtil.record(eTagihan,
												"onSave: proses tagihan registrasi calon");
									}
									try {
										biodataCalonMahasiswa.chekPembayaranDaftarUlang();
									} catch (Exception eTagihan) {
										ais.common.ErrorAuditUtil.record(eTagihan,
												"onSave: proses tagihan daftar ulang calon");
									}

									if (Common.bolehKonfigurasi("integrasi_pmb_arkatama", Konfigurasi.TIDAK_AKTIF) && !update) {
										PmbArkatama.doPost(biodataCalonMahasiswa, new ArrayList<String>());

										Session session = null;
										try {
											session = HibernateUtil.currentNativeSession();
											session.getTransaction().begin();
											session.update(biodataCalonMahasiswa);
											session.getTransaction().commit();
										} catch (Exception e) {
											try {
												if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
													session.getTransaction().rollback();
												}
											} catch (Exception rollbackError) { ais.common.ErrorAuditUtil.record(rollbackError, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:5527");
											}
											throw e;
										} finally {
											closeOpenedNativeSession(session);
										}
									}
								}
							});
						}
					});
				}
			});

			Common.hapusSession(BiodataCalonMahasiswa.class);

		} catch (WrongValueException e) {
			Common.tampilErrorJikaAdmin(e);
			MyMessageboxConfig.show("Data tidak lengkap. " + e.getMessage(), "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.ERROR);
			return false;

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			MyMessageboxConfig.show("Terjadi kesalahan, coba ulangi lagi!", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.ERROR);
			return false;
		}
		return true;
	}

	public static boolean lengkap(BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser == null || tbmuser.getUserId() == null) {
			if (!KonfigurasiTampilanBiodataCalonMahasiswaAction.check(tbmuser, biodataCalonMahasiswa)) {
				return false;
			}
		} else {
			if (!KonfigurasiTampilanLoginCalonMahasiswaAction.check(biodataCalonMahasiswa)) {
				return false;
			}

			if (!VerifikasiPMBHelper.checkVerifikasi(biodataCalonMahasiswa)) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakKartu(final BiodataCalonMahasiswa biodataCalonMahasiswa, final boolean kirimEmail)
			throws Exception {
		if (Common.bolehKonfigurasi("setelah_daftar_pmb_langsung_cetak_kartu")) {
			Session session = HibernateUtil.currentSession();
			session.refresh(biodataCalonMahasiswa);

			if (biodataCalonMahasiswa.getNoRegistrasi() == null
					|| biodataCalonMahasiswa.getNoRegistrasi().trim().isEmpty()) {
				biodataCalonMahasiswa.setNoRegistrasi(CommonPMB.generateNoRegistrasi(biodataCalonMahasiswa));
				try {
					Common.refreshUpdate(session, biodataCalonMahasiswa);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:5589");
				}
			}

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					final boolean bni = Common.bolehKonfigurasi("generate_nomor_pembayaran_saat_formulir_mahasiswa_baru", Konfigurasi.TIDAK_AKTIF);

					final boolean online = Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online", Konfigurasi.TIDAK_AKTIF);

					final boolean aktifkan_pembayaran_via_bank_ntt = Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_ntt", Konfigurasi.TIDAK_AKTIF);

					String info = null;
					if (!bni && !online && !aktifkan_pembayaran_via_bank_ntt) {
						TampilanPaymentGateway.tampilPembayaranRegistrasi(biodataCalonMahasiswa);
					}

//					Map parameters = ais.common.HashMapGenerator.getRand();
					Map parameters = CommonReportHelper.genSklMap(biodataCalonMahasiswa);

					Common.insertProperty(BiodataCalonMahasiswa.class, biodataCalonMahasiswa, parameters, "calon");
					parameters.put("biodata_id", biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getId());
					parameters.put("tahun_akademik",
							Common.getTahunAkademik(1, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR),
									biodataCalonMahasiswa.getSemesterMulai()) + "");

					parameters.put("tagihan", "Rp. " + Common.numberFormat.get().format(CommonPMB
							.getTotalTagihan(biodataCalonMahasiswa, ConstantValues.PENDAFTARAN_CALON_MAHASISWA)));

					parameters.put("pilihan", CommonPMB.getProdiPilihan(biodataCalonMahasiswa));

					File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
							+ biodataCalonMahasiswa.getNoRegistrasi() + ".png");

					BarcodeCommon
							.generateCRCode(
									biodataCalonMahasiswa.getNoRegistrasi() + "\n" + biodataCalonMahasiswa.getNama()
											+ "\n" + biodataCalonMahasiswa.getGelombangPendaftaran().getNama(),
									myfilebarcode);
					parameters.put("cr_code", myfilebarcode.getAbsolutePath());
					parameters.put("qr_code", Common.desEncrypter.get()
							.encrypt(BiodataCalonMahasiswa.class.getName() + ":" + biodataCalonMahasiswa.getId()));
					String code = parameters.get("qr_code") + "";
					File myfilebarcode1 = new File(
							Common.ambilREAL_PATH_REPORT() + "/crcode_" + Common.randLong() + ".png");
					BarcodeCommon.generateCRCode(code, myfilebarcode1);
					parameters.put("qr_code_img", myfilebarcode1.getAbsolutePath());

					if (Common.bolehKonfigurasi("generate_va_langsung_saat_daftar")) {
						if (biodataCalonMahasiswa != null && (biodataCalonMahasiswa.getPembayaranRegistrasi() == null
								|| !biodataCalonMahasiswa.getPembayaranRegistrasi().getLunas())) {
							JenisKegiatan jenisKegiatan = ConstantValues.PENDAFTARAN_CALON_MAHASISWA;

							if (bni) {

								BniRequest bniRequest = BniCommon.bayarCalonMahasiswa(biodataCalonMahasiswa,
										jenisKegiatan, false);
								System.out.println("bniRequest => " + bniRequest);
								if (bniRequest != null) {

									Double amn = bniRequest.getAmount();
									Double biayaAdministrasi = 0.0;
									try {
										biayaAdministrasi = Double.parseDouble(
												Common.getKonfigurasi("bni_biaya_administrasi", "0.0").getNilai());
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:5657");

									}

									info = "Kode Pembayaran\t\t: " + bniRequest.getVa() + "\n";
									info += "Kode invoice\t\t\t: " + bniRequest.getBillNo() + "\n";
									info += "Tagihan \t\t\t: " + Common.numberFormat.get().format(amn) + "\n";
									if (biayaAdministrasi > 0.1) {
										info += "Biaya admin \t\t\t: " + Common.numberFormat.get().format(biayaAdministrasi)
												+ "\n";
										info += "Total tagihan\t\t\t: "
												+ Common.numberFormat.get().format(amn + biayaAdministrasi) + "\n";
									}

									String terbilang = IndonesianNumberToWords
											.convert((long) (amn + biayaAdministrasi));

									String terbilangEn = EnglishNumberToWords.convert((long) (amn + biayaAdministrasi));

									info += "Terbilang \t\t\t: " + terbilang + "\n";
									if (bniRequest.getMahasiswa() != null) {
										info += "NIM \t\t\t\t: " + bniRequest.getMahasiswa().getNim() + "\n";
										info += "Nama \t\t\t\t: " + bniRequest.getMahasiswa().getNama() + "\n";
									} else if (bniRequest.getBiodataCalonMahasiswa() != null) {
										info += "No. Reg \t\t\t: "
												+ bniRequest.getBiodataCalonMahasiswa().getNoRegistrasi() + "\n";
										if (bniRequest.getBiodataCalonMahasiswa().getNoUjian() != null) {
											info += "No. Ujian \t\t\t: "
													+ bniRequest.getBiodataCalonMahasiswa().getNoUjian() + "\n";
										}
										info += "Nama\t\t\t\t: " + bniRequest.getBiodataCalonMahasiswa().getNama()
												+ "\n";
									}

									if (bniRequest.getBillExpired() != null) {
										info += "Waktu Kadaluarsa \t\t: "
												+ Common.dateFormat5.get().format(bniRequest.getBillExpired()) + "\n";
									}

									parameters.put("kode_va", bniRequest.getVa());
									parameters.put("biaya_va", amn);
									parameters.put("admin_va", biayaAdministrasi);
									parameters.put("terbilang_va", terbilang);
									parameters.put("total_va", amn + biayaAdministrasi);
									parameters.put("kadaluarsa", bniRequest.getBillExpired());

									parameters.put("terbilang_en_va", terbilangEn);

									parameters.put("kode_pembayaran", bniRequest.getVa());
									parameters.put("kode_invoice", bniRequest.getBillNo());
									parameters.put("tagihan", amn);
									parameters.put("tagihan_format", Common.numberFormat.get().format(amn));
									parameters.put("biaya_administrasi_format",
											Common.numberFormat.get().format(biayaAdministrasi));
									parameters.put("biaya_administrasi", biayaAdministrasi);
									parameters.put("total_tagihan", amn + biayaAdministrasi);
									parameters.put("total_tagihan_format",
											Common.numberFormat.get().format(amn + biayaAdministrasi));

								}
							} else if (online) {

								Jurusan prodiLulus = biodataCalonMahasiswa.getProdiLulus();
								final List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
								if (prodiLulus == null || prodiLulus.getId() == null) {
									Jurusan myjurusan1 = biodataCalonMahasiswa.getProdi1() == null
											? biodataCalonMahasiswa.getProdi2()
											: biodataCalonMahasiswa.getProdi1();

									java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper
											.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan,
													myjurusan1, true);
									detailBiayas.addAll(detailBiayas1);
								} else {
									java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper
											.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan,
													prodiLulus, true);
									detailBiayas.addAll(detailBiayas1);
								}

								Double nilaiBiayaHarusDiBayars = 0.0;

								for (DetailBiaya detailBiaya : detailBiayas) {
									nilaiBiayaHarusDiBayars += (detailBiaya.getNilaiBiayaBaru() == null
											? detailBiaya.getNilaiBiaya()
											: detailBiaya.getNilaiBiayaBaru());
								}

								if (nilaiBiayaHarusDiBayars > 0.1) {
									Serializable[] serializables = PembayaranUtil.getInstance()
											.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
													biodataCalonMahasiswa.getTanggalDaftar(), jenisKegiatan,
													biodataCalonMahasiswa.getJenjang(),
													biodataCalonMahasiswa.getTahunAkademik(),
													biodataCalonMahasiswa.getGelombangPendaftaran().getJenisSemester()
															.equalsIgnoreCase(Perkuliahan.GANJIL),
													biodataCalonMahasiswa.getJenisSeleksi(),
													biodataCalonMahasiswa.getProgram(),
													biodataCalonMahasiswa.getNoRegistrasi(),
													biodataCalonMahasiswa.getGelombangPendaftaran());
									JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
									System.out.println("jadwalPembayaran => " + jadwalPembayaran);

									if (jadwalPembayaran != null) {

										Double biayaAdmin = 0.0;
										try {
											biayaAdmin = Double.parseDouble(Common
													.getKonfigurasi("online_biaya_administrasi", "0.0").getNilai());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:5766");

										}

										BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
												Common.getKonfigurasi("online_bank_host_ip", "").getNilai(),
												"Bank Host");
										Map param = new HashMap();
										VirtualAccountBank virtualAccountBank = DownloadNoRegistrasiCalonMahasiswaBankOnline
												.downloadData(biodataCalonMahasiswa, jadwalPembayaran, detailBiayas,
														param, biayaAdmin, bankHost);

										if (virtualAccountBank != null) {

											Double amn = virtualAccountBank.getTotal();
											Double biayaAdministrasi = virtualAccountBank.getBiayaAdmin();

											info = "Kode Pembayaran\t\t: " + virtualAccountBank.getKode() + "\n";
											info += "Tagihan \t\t\t: " + Common.numberFormat.get().format(amn) + "\n";
											if (biayaAdministrasi > 0.1) {
												info += "Biaya admin \t\t\t: "
														+ Common.numberFormat.get().format(biayaAdministrasi) + "\n";
												info += "Total tagihan \t\t\t: "
														+ Common.numberFormat.get().format(amn + biayaAdministrasi) + "\n";
											}

											String terbilang = IndonesianNumberToWords
													.convert((long) (amn + biayaAdministrasi));

											String terbilangEn = EnglishNumberToWords
													.convert((long) (amn + biayaAdministrasi));

											info += "Terbilang \t\t\t: " + terbilang + "\n";
											if (virtualAccountBank.getMahasiswa() != null) {
												info += "NIM \t\t\t\t: " + virtualAccountBank.getMahasiswa().getNim()
														+ "\n";
												info += "Nama \t\t\t\t: " + virtualAccountBank.getMahasiswa().getNama()
														+ "\n";
											} else if (virtualAccountBank.getBiodataCalonMahasiswa() != null) {
												info += "No. Reg \t\t\t: " + virtualAccountBank
														.getBiodataCalonMahasiswa().getNoRegistrasi() + "\n";
												if (virtualAccountBank.getBiodataCalonMahasiswa()
														.getNoUjian() != null) {
													info += "No. Ujian \t\t\t: "
															+ virtualAccountBank.getBiodataCalonMahasiswa().getNoUjian()
															+ "\n";
												}
												info += "Nama \t\t\t\t: "
														+ virtualAccountBank.getBiodataCalonMahasiswa().getNama()
														+ "\n";
											}

											if (virtualAccountBank.getKadaluarsaWaktu() != null) {
												info += "Tgl Kadaluarsa \t\t\t: " + Common.dateFormat51.get()
														.format(virtualAccountBank.getKadaluarsaWaktu()) + "\n";
											}

											parameters.put("kode_va", virtualAccountBank.getKode());
											parameters.put("biaya_va", virtualAccountBank.getTotal());
											parameters.put("admin_va", biayaAdministrasi);
											parameters.put("terbilang_va", terbilang);
											parameters.put("total_va",
													virtualAccountBank.getTotal() + biayaAdministrasi);

											parameters.put("terbilang_en_va", terbilangEn);

											parameters.put("kode_pembayaran", virtualAccountBank.getKode());
											parameters.put("kode_invoice", virtualAccountBank.getKode());
											parameters.put("tagihan", amn);
											parameters.put("tagihan_format", Common.numberFormat.get().format(amn));
											parameters.put("biaya_administrasi_format",
													Common.numberFormat.get().format(biayaAdministrasi));
											parameters.put("biaya_administrasi", biayaAdministrasi);
											parameters.put("total_tagihan", amn + biayaAdministrasi);
											parameters.put("total_tagihan_format",
													Common.numberFormat.get().format(amn + biayaAdministrasi));
										}

									}
								}

							}

							else if (aktifkan_pembayaran_via_bank_ntt) {

								Kegiatan kegiatan = biodataCalonMahasiswa.ambilKegiatans(null,
										ConstantValues.PENDAFTARAN_CALON_MAHASISWA);

								System.out.println("data keg " + kegiatan);
								if (kegiatan == null || kegiatan.getId() == null) {

									Serializable[] serializables = PembayaranUtil.getInstance()
											.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
													biodataCalonMahasiswa.getTanggalDaftar(), jenisKegiatan,
													biodataCalonMahasiswa.getJenjang(),
													biodataCalonMahasiswa.getTahunAkademik(),
													biodataCalonMahasiswa.getGelombangPendaftaran().getJenisSemester()
															.equalsIgnoreCase(Perkuliahan.GANJIL),
													biodataCalonMahasiswa.getJenisSeleksi(),
													biodataCalonMahasiswa.getProgram(),
													biodataCalonMahasiswa.getNoRegistrasi(),
													biodataCalonMahasiswa.getGelombangPendaftaran());
									JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
									System.out.println("jadwalPembayaran " + jadwalPembayaran);
									if (jadwalPembayaran != null) {
										Jurusan prodiLulus = biodataCalonMahasiswa.getProdiLulus();
										ArrayList<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
										if (prodiLulus == null || prodiLulus.getId() == null) {
											Jurusan myjurusan1 = biodataCalonMahasiswa.getProdi1() == null
													? biodataCalonMahasiswa.getProdi2()
													: biodataCalonMahasiswa.getProdi1();
				
											java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa,
															jenisKegiatan, myjurusan1, false);
											detailBiayas.addAll(detailBiayas1);
										} else {
											java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa,
															jenisKegiatan, prodiLulus, false);
											detailBiayas.addAll(detailBiayas1);
										}
										System.out.println("detailBiayas " + detailBiayas);
										if (!detailBiayas.isEmpty()) {

											VirtualAccountBank virtualAccountBank = DownloadNoRegistrasiCalonMahasiswaBankNtt
													.downloadData(biodataCalonMahasiswa, jadwalPembayaran,
															detailBiayas);
											if (virtualAccountBank != null) {
												code = virtualAccountBank.getKode();

												myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
														+ virtualAccountBank.getId() + ".png");

												BarcodeCommon.generateCRCode(code, myfilebarcode1);

												Double biayaAdministrasi = 0.0;

												info = "Pembayaran dapat dilakukan di Bank NTT dengan informasi sbb :\nKode Pembayaran\t\t: "
														+ virtualAccountBank.getKode() + "\n";
												info += "Tagihan \t\t\t: "
														+ Common.numberFormat.get().format(virtualAccountBank.getTotal())
														+ "\n";
												if (biayaAdministrasi > 0.1) {
													info += "Biaya admin \t\t\t: "
															+ Common.numberFormat.get().format(biayaAdministrasi) + "\n";
													info += "Total tagihan \t\t: "
															+ Common.numberFormat.get().format(
																	virtualAccountBank.getTotal() + biayaAdministrasi)
															+ "\n";
												}

												String terbilang = IndonesianNumberToWords.convert(
														(long) (virtualAccountBank.getTotal() + biayaAdministrasi));

												String terbilangEn = EnglishNumberToWords.convert(
														(long) (virtualAccountBank.getTotal() + biayaAdministrasi));

												info += "Terbilang \t\t\t: " + terbilang + "\n";
												if (virtualAccountBank.getMahasiswa() != null) {
													info += "NIM \t\t\t\t: "
															+ virtualAccountBank.getMahasiswa().getNim() + "\n";
													info += "Nama \t\t\t\t: "
															+ virtualAccountBank.getMahasiswa().getNama() + "\n";
												} else if (virtualAccountBank.getBiodataCalonMahasiswa() != null) {
													info += "No. Reg \t\t\t: " + virtualAccountBank
															.getBiodataCalonMahasiswa().getNoRegistrasi() + "\n";
													if (virtualAccountBank.getBiodataCalonMahasiswa()
															.getNoUjian() != null) {
														info += "No. Ujian \t\t\t: " + virtualAccountBank
																.getBiodataCalonMahasiswa().getNoUjian() + "\n";
													}
													info += "Nama \t\t\t: "
															+ virtualAccountBank.getBiodataCalonMahasiswa().getNama()
															+ "\n";
												}
												System.out.println("info " + info);

												parameters.put("barcode_file", myfilebarcode1.getAbsolutePath());
												parameters.put("kode_va", virtualAccountBank.getKode());
												parameters.put("biaya_va", virtualAccountBank.getTotal());
												parameters.put("admin_va", biayaAdministrasi);
												parameters.put("terbilang_va", terbilang);
												parameters.put("total_va",
														virtualAccountBank.getTotal() + biayaAdministrasi);

												parameters.put("terbilang_en_va", terbilangEn);

												parameters.put("kode_pembayaran", virtualAccountBank.getKode());
												parameters.put("kode_invoice", virtualAccountBank.getKode());
												parameters.put("tagihan", virtualAccountBank.getTotal());
												parameters.put("tagihan_format",
														Common.numberFormat.get().format(virtualAccountBank.getTotal()));
												parameters.put("biaya_administrasi_format",
														Common.numberFormat.get().format(biayaAdministrasi));
												parameters.put("biaya_administrasi", biayaAdministrasi);
												parameters.put("total_tagihan",
														virtualAccountBank.getTotal() + biayaAdministrasi);
												parameters.put("total_tagihan_format", Common.numberFormat.get()
														.format(virtualAccountBank.getTotal() + biayaAdministrasi));

											}
										}
									}

								}
							}

							if (info != null && !info.trim().isEmpty()) {
								info = "INFORMASI VIRTUAL ACCOUNT PEMBAYARAN "
										+ (ConstantValues.PENDAFTARAN_CALON_MAHASISWA == null ? ""
												: ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getNamaKegiatan()
														.toUpperCase())
										+ "\n\n" + info;
							}
						}
					}

					parameters.put("info_data", info);

					Common.insertProperty(BiodataCalonMahasiswa.class, biodataCalonMahasiswa, parameters, "pmb");

					File file = Report.generateDownloadReport(Report.PDF, parameters, "KartuBayarSpmbMandiri", null,
							ais.ui.util.WaktuUtil.getDate(), Common.locale, false);

					PDFMergerUtility ut = new PDFMergerUtility();
					ut.addSource(file);

					if (info != null && !info.trim().isEmpty()) {

						File fileinfo = Report.generateDownloadReport(Report.PDF, parameters, "info", null,
								ais.ui.util.WaktuUtil.getDate(), Common.locale, false);
						ut.addSource(fileinfo);
					}

					if (biodataCalonMahasiswa.getNoUjian() != null) {
						File bio = CommonReportHelper.onCetakBiodataCalonMahasiswa(biodataCalonMahasiswa, false);
						ut.addSource(bio);
					}
					final File filePdfBaru = new File(
							file.getParentFile().getAbsolutePath() + "/" + Common.getGeneratedBarCode() + ".pdf");
					FileOutputStream fileOutputStreamPdf = null;
					try {
						fileOutputStreamPdf = new FileOutputStream(filePdfBaru);
						ut.setDestinationStream(fileOutputStreamPdf);
						ut.mergeDocuments();
					} finally {
						if (fileOutputStreamPdf != null) {
							try {
								fileOutputStreamPdf.close();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:6014");
							}
						}
					}

					Tbmuser tbmuser = Common.getCurrentUser();
					if (tbmuser == null || tbmuser.getBiodataCalonMahasiswa() != null
							|| tbmuser.getMahasiswa() != null) {
						Report.tampil(filePdfBaru, parameters);
					} else {
						Report.tampil(filePdfBaru, parameters, "KartuBayarSpmbMandiri", "info");
					}

					if (kirimEmail) {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								CommonEmail.infoDaftarMahasiswaBanyakFile(biodataCalonMahasiswa,
										new File[] { filePdfBaru });
							}
						});
					}
				}

			});
		}
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}


	private void sinkronkanPembayaranSetelahStatusAwalBerubah(final Long biodataCalonMahasiswaId) {
		if (biodataCalonMahasiswaId == null) {
			return;
		}
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					CetakRegistrasiAction.singkronkanDenganPembayaran(biodataCalonMahasiswaId, null, 1, 1);
				} catch (Exception e) {
					try {
						Common.tampilErrorJikaAdmin(e);
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:6065");
					}
				}
			}
		}, "ais-pmb-sync-pembayaran-" + biodataCalonMahasiswaId);
		try {
			thread.setDaemon(true);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:6072");
		}
		thread.start();
	}

	private void sinkronkanPembayaranSetelahPendaftaranBaru(Long biodataCalonMahasiswaId) {
		if (biodataCalonMahasiswaId == null) {
			return;
		}
		try {
			CetakRegistrasiAction.singkronkanDenganPembayaran(biodataCalonMahasiswaId, null, 1, 1);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(pmb-auto-sync-pembayaran-setelah-daftar) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java");
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ex) {
				ais.common.ErrorAuditUtil.record(ex,
						"auto-audit(empty-catch) src/ais/action/master/pmb/BiodataCalonMahasiswaAction.java:auto-sync-pembayaran");
			}
		}
	}

}
