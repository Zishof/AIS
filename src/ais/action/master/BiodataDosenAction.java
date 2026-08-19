package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.net.URLEncoder;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
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
import org.zkoss.zul.West;

import ais.action.master.dashboard.admin.DashboardKegiatanKedosenan;
import ais.action.master.epsbed.RiwayatPendidikanDosenHelper;
import ais.action.master.helper.AktifitasSkripsiHelper;
import ais.action.master.helper.AktifitasTugasAkhirHelper;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataKecamatanBanbox;
import ais.action.master.helper.AmbilDataNegaraBanbox;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.AmbilDataTbmuserBanbox;
import ais.action.master.helper.DetailArtikelHelper;
import ais.action.master.helper.DosenMengajarHelper;
import ais.action.master.helper.MainHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.penelitiandanpengabdian.helper.PengajuanPenelitianDanPengabdianHelper;
import ais.action.report.format1.akademik.LaporanAngketDosenPerDosenWindow;
import ais.action.report.format1.akademik.LaporanSKDosen;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonOnSearchdefault;
import ais.common.ConstantValues;
import ais.database.dao.BiodataDosenDao;
import ais.database.dao.DaoFactory;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Agama;
import ais.database.model.Asesor;
import ais.database.model.AsesorPegawai;
import ais.database.model.AsesorPenunjangKinerjaDosen;
import ais.database.model.BiodataDosen;
import ais.database.model.Dosen;
import ais.database.model.DspaceInformation;
import ais.database.model.Fakultas;
import ais.database.model.GolonganPns;
import ais.database.model.IkatanKerjaDosen;
import ais.database.model.Jabatan;
import ais.database.model.JabatanFungsionalDosen;
import ais.database.model.JenisPendidikDanTenagaKependidikan;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Kota;
import ais.database.model.LembagaPengangkat;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Negara;
import ais.database.model.Pegawai;
import ais.database.model.PekerjaanOrangTua;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PenugasanDosenMengajar;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Propinsi;
import ais.database.model.Ruang;
import ais.database.model.Skripsi;
import ais.database.model.StatusKepegawaian;
import ais.database.model.StatusPegawai;
import ais.database.model.SumberGaji;
import ais.database.model.Tbmuser;
import ais.database.model.Wilayah;
import ais.database.model.employ.Pendidikan;
import ais.database.model.file.FotoDosen;
import ais.database.model.file.FotoPegawai;
import ais.database.model.file.LampiranLain;
import ais.database.model.kkn.KelompokKkn;
import ais.ui.dspace.DspaceCommon;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class BiodataDosenAction extends MyWindow {

	public final static String[] DEFAULT_TIDAK_WAJIB = new String[] { "code", "mycode", "nidn", "niyNigk", "nuptk",
			"skCpns", "tglSkCpns", "skAngkat", "tmtSkAngkat", "tmtPns",

			"gelarDepan",

			"gelarBelakang", "email", "telp", "pangkat",

			"spesifikasiJabatan",

			"jabatan", "spesialisasi1", "spesialisasi2", "spesialisasi3",

			"tetap", "perguruanTinggi", "fakultas", "jurusan",

			"statusPegawai", "milikUniversitas",

			"ikatanKerjaDosen", "statusKepegawaian",

			"Golongan",

			"sumberGaji", "aLisensiKepsek",

			"jmlSekolahBinaan", "aDiklatAwas", "aktaIjinAjar", "nira", "aBraille",

			"aBhsIsyarat",

			"npwp", "jenisPendidikDanTenagaKependidikan", "lembagaPengangkat", "jabatanFungsionalDosen", "cbJabatan",
			"cbJabatanPtLain", "pendidikan", "pendidikans1", "pendidikans2", "pendidikans3", "sertifikasi",
			"nomorSertifikasi", "sesuaiBidangKeilmuan", "idfinger", "atasanlangsung" };

	public final static String[] DATA = new String[] { "code", "mycode", "nidn", "niyNigk", "nuptk", "skCpns",
			"tglSkCpns", "skAngkat", "tmtSkAngkat", "tmtPns",

			"nama", "gelarDepan",

			"gelarBelakang", "email", "telp", "kelamin", "tempatlahir", "tanggallahir", "pangkat",

			"spesifikasiJabatan",

			"jabatan", "spesialisasi1", "spesialisasi2", "spesialisasi3",

			"tetap", "perguruanTinggi", "fakultas", "jurusan",

			"statusPegawai", "milikUniversitas",

			"ikatanKerjaDosen", "statusKepegawaian",

			"Golongan",

			"sumberGaji", "aLisensiKepsek",

			"jmlSekolahBinaan", "aDiklatAwas", "aktaIjinAjar", "nira", "aBraille",

			"aBhsIsyarat",

			"npwp", "jenisPendidikDanTenagaKependidikan", "lembagaPengangkat", "jabatanFungsionalDosen", "cbJabatan",
			"cbJabatanPtLain", "pendidikan", "pendidikans1", "pendidikans2", "pendidikans3", "sertifikasi",
			"nomorSertifikasi", "sesuaiBidangKeilmuan", "idfinger", "atasanlangsung" };

	public final static String[] DATA_DESC = new String[] { "NIP", "NIP (PNS)", "NIDN/NUPN",
			"NIGB (Nomor induk yayasan, nomor induk guru kontrak, termasuk nomor induk guru bantu)",
			"Nomor urut pendidik dan tenaga kependidikan", "Nomor SK CPNS", "Tanggal dikeluarkannya SK CPNS",
			"SK Pengangkatan Dosen", "Terhitung mulai tanggal pengangkatan bagi yang bukan berstatus PNS",
			"Terhitung mulai tanggal menjadi PNS",

			"Nama Dosen", "Gelar Depan",

			"Gelar Belakang", "Email", "Telp./HP", "Jenis Kelamin", "Tempat Lahir", "Tanggal Lahir", "Pangkat",

			"Jabatan",

			"Deskripsi Jabatan",

			"Bidang Ilmu I", "Bidang Ilmu II", "Bidang Ilmu III",

			"Merupakan Dosen Tetap",

			"Perguruan Tinggi", "Fakultas", Common.getBahasaConfig("Jurusan") + " (Home Base)",

			"Status Keaktifan Pegawai", "Dosen Milik Universitas (Boleh mengajar di Prodi Lain)",

			"Jenis Ikatan Kerja", "Status Kepegawaian",

			"Golongan",

			"Sumber Gaji", "Apakah memiliki lisensi kepala sekolah",

			"Jumlah sekolah binaan yang diawasi oleh seorang pengawas",
			"Apakah seorang pengawas telah mengikuti diklat kepengawasan",
			"Nomor akta dan ijin mengajar yang dikeluarkan oleh universitas",
			"Nomor induk registrasi asesor, nomor induk yang dimiliki oleh seorang dosen jika berperan sebagai asesor",
			"Apakah memiliki kemampuan membaca huruf braille",

			"Apakah memiliki kemampuan bahasa isyarat",

			"NPWP (Nomor pokok wajib pajak)", "Jenis Pendidik dan Tenaga Kependidikan", "Lembaga Pengangkat",
			"Jabatan Fungsional", "Jabatan", "Jabatan PT Lain", "Pendidikan Terakhir", "Bidang Ilmu Pendidikan S1",
			"Bidang Ilmu Pendidikan S2", "Bidang Ilmu Pendidikan S3", "Sertifikasi", "Nomor Sertifikasi",
			"Sesuai Bidang Keilmuan", "Kode Finger / RFID", "Atasan langsung" };

	public final static TreeMap<String, String> MAPPING_DATA = new TreeMap<String, String>();

	public final static AktifitasTugasAkhirHelper aktifitasTugasAkhirHelper = new AktifitasTugasAkhirHelper();
	public final static AktifitasSkripsiHelper aktifitasSkripsiHelper = new AktifitasSkripsiHelper();

	static {
		for (int i = 0; i < DATA.length; i++) {
			try {
				MAPPING_DATA.put(DATA[i], DATA_DESC[i]);
			} catch (Exception e) {
				System.out.println("error key " + DATA[i]);
			}
		}

		System.out.println("MAPPING_DATA => " + MAPPING_DATA);
	}

	public static void displayBiodataWindow() throws Exception {
		final MyWindow window = new MyWindow();
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("95%");
		window.setWidth("850px");
		window.setClosable(false);

		MyInclude include = new MyInclude("/pages/master/biodata_dosen.zul?refresh=true");
		include.setParent(window);
		include.setWidth("100%");
		include.setHeight("100%");
		window.onModal();
	}

	public static boolean checkEmailDosen(Dosen dosen) throws Exception {
		if (dosen.getEmail() == null || dosen.getEmail().trim().isEmpty()
				|| !Common.isValidEmailAddress(dosen.getEmail().trim())) {

			displayBiodataWindow();

			MyMessageboxConfig.show("Email harus diisi dan format email harus benar", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							// TODO Auto-generated method stub

						}
					});
			return false;
		}
		return true;
	}

	public static boolean checkBiodataDosen(Dosen dosen) throws Exception {

		BiodataDosen biodataDosen = dosen.ambilBiodata();
		if (biodataDosen == null) {
			displayBiodataWindow();
			MyMessageboxConfig.show("Biodata Anda harus dilengkapi", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							// TODO Auto-generated method stub

						}
					});

			return false;
		}

		List<String> daftarWajibDiisi = KonfigurasiTampilanBiodataDosenAction.dataYangWajibDiisi();
		for (String key : daftarWajibDiisi) {
			if (Common.checkIsNull(Dosen.class, dosen, key)) {

				displayBiodataWindow();

				MyMessageboxConfig.show(
						"Biodata Anda harus dilengkapi. Data \"" + KonfigurasiTampilanBiodataDosenAction.keyDesc(key)
								+ "\" masih belum terisi dengan benar",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								// TODO Auto-generated method stub

							}
						});

				return false;
			}
		}

		String[] DATA = new String[] { "statusNikah", "alamat", "rt", "rw", "kelurahan", "kecamatan", "propinsi",
				"kota", "noIdentitas", "dusun" };

		String[] DATA_DESC = new String[] { "Rincian data -> Keluarga -> Status Menikah",
				"Rincian data -> Alamat -> Alamat", "Rincian data -> Alamat -> rt", "Rincian data -> Alamat -> rw",
				"Rincian data -> Alamat -> desa / kelurahan", "Rincian data -> Alamat -> kecamatan",
				"Rincian data -> Alamat -> propinsi", "Rincian data -> Alamat -> kota",
				"Rincian data -> Alamat -> No Identitas / KTP", "Rincian data -> Alamat -> Nama Dusun / Kampung" };

		String d = "";
		for (int i = 0; i < DATA_DESC.length; i++) {
			d += "\n" + DATA_DESC[i];
		}

		for (int i = 0; i < DATA.length; i++) {
			if (Common.checkIsNull(BiodataDosen.class, biodataDosen, DATA[i])) {
				displayBiodataWindow();
				MyMessageboxConfig.show(
						"Biodata Anda harus dilengkapi. Data \"" + DATA_DESC[i]
								+ "\" masih belum terisi..\n\nData yang wajib diis antara lain :" + d,
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								// TODO Auto-generated method stub

							}
						});

				return false;
			}
		}

		try {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

			int fotoDosen = ((Number) streamingSession.createCriteria(FotoDosen.class)
					.add(Restrictions.eq("dosen", dosen.getId())).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();
			if (fotoDosen == 0) {
				MyMessageboxConfig.show("Foto Anda harus dilengkapi.", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								// TODO Auto-generated method stub

							}
						});
				displayBiodataWindow();
				return false;
			}
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		return true;
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 72558191307949087L;

	private CommonOnSearchdefault commonOnSearchdefault;

	private ManagingBiodataDosen managingBiodataDosen = new ManagingBiodataDosen();
	private ManagingDosen managingDosen = new ManagingDosen();
	private ManagingProdiYangDiajar managingProdiYangDiajar = new ManagingProdiYangDiajar();

	private RiwayatPendidikanDosenHelper riwayatPendidikanDosenHelper = new RiwayatPendidikanDosenHelper();

	private EventListener fotoEventListener;

	private Boolean refresh = null;

	protected BiodataPegawaiAction biodataPegawaiAction = null;

	private boolean tampilPegawai = true;
	private EventListener eventListener = null;

	private boolean simpanPegawai = true;

	public BiodataDosenAction() throws Exception {
		super();
		init(Common.getCurrentUser() == null ? null : Common.getCurrentUser().getDosen());
	}

	public BiodataDosenAction(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init(Common.getCurrentUser() == null ? null : Common.getCurrentUser().getDosen());
	}

	public BiodataDosenAction(Dosen dosen) throws Exception {
		super();

		if (dosen == null) {
			init(Common.getCurrentUser().getDosen());
		} else {
			init(dosen);
		}

	}

	public BiodataDosenAction(Dosen dosen, EventListener eventListener, boolean tampilPegawai) throws Exception {
		super();
		this.tampilPegawai = tampilPegawai;
		this.eventListener = eventListener;
		if (dosen == null) {
			init(Common.getCurrentUser().getDosen());
		} else {
			init(dosen);
		}
	}

	public BiodataDosenAction(Dosen dosen, EventListener eventListener, boolean tampilPegawai, boolean simpanPegawai)
			throws Exception {
		super();
		this.tampilPegawai = tampilPegawai;
		this.simpanPegawai = simpanPegawai;
		this.eventListener = eventListener;
		if (dosen == null) {
			init(Common.getCurrentUser().getDosen());
		} else {
			init(dosen);
		}
	}

	public BiodataDosenAction(Dosen dosen, String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		if (dosen == null) {
			init(Common.getCurrentUser().getDosen());
		} else {
			init(dosen);
		}

	}

	private void init(final Dosen dosen) throws Exception {
		setTitle("Biodata Dosen");
		if (ExecutionsCtrl.getCurrent().getParameter("refresh") != null) {
			refresh = Boolean.parseBoolean(ExecutionsCtrl.getCurrent().getParameter("refresh"));
		}

		if (dosen == null) {
			MyMessageboxConfig.show("Anda harus login sebagai dosen", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (Sessions.getCurrent().getAttribute("fotoEventListener") != null) {
			fotoEventListener = (EventListener) Sessions.getCurrent().getAttribute("fotoEventListener");
			Sessions.getCurrent().removeAttribute("fotoEventListener");
		}

		Tabpanel tabpanelDosen = managingDosen.init(dosen, simpanPegawai);
		Tabpanel tabpanelBiodataDosen = managingBiodataDosen.preInit(dosen);
		Tabpanel tabpanelJurusans = managingProdiYangDiajar.init(dosen, null);

		final Tabpanel tabpanelPublikasi = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelPengabdian = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelPublikasiIlmiah = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelRiwayatPendidikan = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelMengajarPtlain = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelTugasBelajar = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelKegiatanKedosenan = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelKepegawian = new ais.ui.util.MyTabpanel();

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		borderlayout.setStyle("border:0px;");

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);
		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);
		final MyTabConfig tab1;
		final MyTabConfig tab2;
		final MyTabConfig tab3;
		final MyTabConfig tab31;
		final MyTabConfig tab32;
		final MyTabConfig tab33;

		final MyTabConfig tab5;
		final MyTabConfig tab50;
		final MyTabConfig tab6;
		tabs.appendChild(tab1 = new MyTabConfig("Data Dosen"));
		tabs.appendChild(tab2 = new MyTabConfig("Rincian Data"));

		tabs.appendChild(tab3 = new MyTabConfig("Penugasan"));
		tabs.appendChild(tab31 = new MyTabConfig("Mengajar di PT Lain"));
		tabs.appendChild(tab32 = new MyTabConfig("Tugas Belajar"));
		tabs.appendChild(tab33 = new MyTabConfig("Kegiatan Dosen"));

		final MyTabConfig tab34;
		tabs.appendChild(tab34 = new MyTabConfig("Dokumen Dosen"));

		tabs.appendChild(tab5 = new MyTabConfig("Penelitian"));
		tabs.appendChild(tab50 = new MyTabConfig("Pengabdian"));
		final MyTabConfig tab51;
		tabs.appendChild(tab51 = new MyTabConfig("Publikasi Ilmiah"));
		tabs.appendChild(tab6 = new MyTabConfig("Riwayat Pendidikan"));
		MyTabConfig tab7 = new MyTabConfig("Kepegawaian");
		if (tampilPegawai) {
			tabs.appendChild(tab7);
		}
		// boolean terhubung_ke_ojs = Common
		// .getKonfigurasi("tampilkan_penelitian_dan_pengabdian_di_dosen",
		// Konfigurasi.AKTIF).getNilai()
		// .equals(Konfigurasi.AKTIF);
		// tab5.setVisible(terhubung_ke_ojs);
		// tab50.setVisible(terhubung_ke_ojs);
		// tab51.setVisible(terhubung_ke_ojs);

		tab3.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Dosen dsn = dosen == null || dosen.getId() == null ? managingDosen.onSave(event) : dosen;
				if (dsn == null || dsn.getId() == null) {
					tab1.setSelected(true);
				} else {
					managingBiodataDosen.dosen = dsn;
				}
			}
		});

		tab31.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Dosen dsn = dosen == null || dosen.getId() == null ? managingDosen.onSave(event) : dosen;
				if (dsn == null || dsn.getId() == null) {
					tab1.setSelected(true);
				} else {
					Common.clear(tabpanelMengajarPtlain);
					MyWindow window = new MyWindow("", "none", false);
					window.setHeight("97%");
					window.setWidth("97%");
					window.setParent(tabpanelMengajarPtlain);
					MyInclude iframe = new MyInclude(
							"/pages/master/mengajar_di_perguruan_tinggi_lain.zul?dosen=" + dsn.getId());
					iframe.setParent(window);
				}
			}
		});

		tab32.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Dosen dsn = dosen == null || dosen.getId() == null ? managingDosen.onSave(event) : dosen;
				if (dsn == null || dsn.getId() == null) {
					tab1.setSelected(true);
				} else {

					Common.clear(tabpanelTugasBelajar);
					MyWindow window = new MyWindow("", "none", false);
					window.setHeight("97%");
					window.setWidth("97%");
					window.setParent(tabpanelTugasBelajar);
					MyInclude iframe = new MyInclude("/pages/master/tugas_belajar_dosen.zul?dosen=" + dsn.getId());
					iframe.setParent(window);
				}
			}
		});

		tab33.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Dosen dsn = dosen == null || dosen.getId() == null ? managingDosen.onSave(event) : dosen;
				if (dsn == null || dsn.getId() == null) {
					tab1.setSelected(true);
				} else {

					Common.clear(tabpanelKegiatanKedosenan);
					DashboardKegiatanKedosenan window = new DashboardKegiatanKedosenan(dosen);
					ais.ui.util.BaseDasbordPortal.mountWrapped(window, tabpanelKegiatanKedosenan,
						"Kegiatan Kedosenan", "Rekap kegiatan pengajaran, penelitian, dan pengabdian dosen ini.");
				}
			}
		});

		tab5.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Dosen dsn = dosen == null || dosen.getId() == null ? managingDosen.onSave(event) : dosen;
				if (dsn == null || dsn.getId() == null) {
					tab1.setSelected(true);
				} else {
					if (tabpanelPublikasi.getChildren().isEmpty()) {
						Tbmuser tbmuser = (Tbmuser) HibernateUtil.currentSession().createCriteria(Tbmuser.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("dosen", dosen))
								.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
								.addOrder(Order.desc("tanggal_dirubah")).setMaxResults(1).uniqueResult();
						if (tbmuser == null || tbmuser.getUserId() == null) {
							tbmuser = (Tbmuser) HibernateUtil.currentSession().createCriteria(Tbmuser.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("dosen", dosen)).addOrder(Order.desc("tanggal_dirubah"))
									.setMaxResults(1).uniqueResult();
						}

						if (tbmuser != null) {
							PengajuanPenelitianDanPengabdianHelper pengajuanPenelitianDanPengabdianHelper = new PengajuanPenelitianDanPengabdianHelper(
									PengumumanAkademis.UNTUK_DOSEN);
							MyWindow addWindowPengajuan = new MyWindow();
							addWindowPengajuan
									.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							pengajuanPenelitianDanPengabdianHelper.displayPengajuan(false, tbmuser.getUserId(),
									PengumumanAkademis.UNTUK_DOSEN, null, tabpanelPublikasi, addWindowPengajuan,
									ConstantValues.PENELITIAN, "600px");
						} else {
							MyMessageboxConfig.show("Dosen ini tidak memiliki akun / login pengguna", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					}
				}
			}
		});

		tab50.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Dosen dsn = dosen == null || dosen.getId() == null ? managingDosen.onSave(event) : dosen;
				if (dsn == null || dsn.getId() == null) {
					tab1.setSelected(true);
				} else {
					if (tabpanelPengabdian.getChildren().isEmpty()) {
						Tbmuser tbmuser = (Tbmuser) HibernateUtil.currentSession().createCriteria(Tbmuser.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("dosen", dosen)).addOrder(Order.desc("tanggal_dirubah"))
								.setMaxResults(1).uniqueResult();

						if (tbmuser != null) {
							PengajuanPenelitianDanPengabdianHelper pengajuanPenelitianDanPengabdianHelper = new PengajuanPenelitianDanPengabdianHelper(
									PengumumanAkademis.UNTUK_DOSEN);
							MyWindow addWindowPengajuan = new MyWindow();
							addWindowPengajuan
									.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							pengajuanPenelitianDanPengabdianHelper.displayPengajuan(false, tbmuser.getUserId(),
									PengumumanAkademis.UNTUK_DOSEN, null, tabpanelPengabdian, addWindowPengajuan,
									ConstantValues.PENGABDIAN, "600px");
						} else {
							MyMessageboxConfig.show("Dosen ini tidak memiliki akun / login pengguna", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					}
				}
			}
		});

		tab51.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Dosen dsn = dosen == null || dosen.getId() == null ? managingDosen.onSave(event) : dosen;
				if (dsn == null || dsn.getId() == null) {
					tab1.setSelected(true);
				} else {
					if (tabpanelPublikasiIlmiah.getChildren().isEmpty()) {

						// if (Common.getKonfigurasi("terhubung_ke_ojs",
						// Konfigurasi.TIDAK_AKTIF).getNilai()
						// .equals(Konfigurasi.AKTIF)) {

						Tbmuser tbmuser = (Tbmuser) HibernateUtil.currentSession().createCriteria(Tbmuser.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("dosen", dosen)).addOrder(Order.desc("tanggal_dirubah"))
								.setMaxResults(1).uniqueResult();

						if (tbmuser != null) {
							DetailArtikelHelper detailArtikelHelper = new DetailArtikelHelper(dosen);
							MyWindow addWindowPengajuan = new MyWindow();
							addWindowPengajuan
									.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							detailArtikelHelper.displayPengajuan(false, tbmuser.getUserId(),
									PengumumanAkademis.UNTUK_DOSEN, null, tabpanelPublikasiIlmiah, addWindowPengajuan,
									"600px");
						} else {
							MyMessageboxConfig.show("Dosen ini tidak memiliki akun / login pengguna", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
						// } else {
						// tabpanelPublikasiIlmiah.appendChild(publikasiIlmiahDosenHelper.display(dosen));
						// }
					}

				}
			}
		});

		tab6.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Dosen dsn = dosen == null || dosen.getId() == null ? managingDosen.onSave(event) : dosen;
				if (dsn == null || dsn.getId() == null) {
					tab1.setSelected(true);
				} else {
					if (tabpanelRiwayatPendidikan.getChildren().isEmpty()) {
						tabpanelRiwayatPendidikan.appendChild(riwayatPendidikanDosenHelper.display(dosen));
					}
				}
			}
		});

		tab7.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Dosen dsn = dosen == null || dosen.getId() == null ? managingDosen.onSave(event) : dosen;
				if (dsn == null || dsn.getId() == null) {
					tab1.setSelected(true);
				} else {
					if (tabpanelKepegawian.getChildren().isEmpty()) {

						Session session = HibernateUtil.currentNativeSession();
						Pegawai pegawai = Pegawai.createDataPegawaiDariDosen(session, dosen);
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
						HibernateUtil.closeSession();

						biodataPegawaiAction = new BiodataPegawaiAction(pegawai, false);
						biodataPegawaiAction.setHeight("100%");
						biodataPegawaiAction.setWidth("100%");
						biodataPegawaiAction.setBorder("none");
						biodataPegawaiAction.setTitle("");
						tabpanelKepegawian.appendChild(biodataPegawaiAction);
					}
				}
			}
		});

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);
		tabpanels.appendChild(tabpanelDosen);
		tabpanels.appendChild(tabpanelBiodataDosen);

		tabpanels.appendChild(tabpanelJurusans);
		tabpanels.appendChild(tabpanelMengajarPtlain);
		tabpanels.appendChild(tabpanelTugasBelajar);
		tabpanels.appendChild(tabpanelKegiatanKedosenan);

		final Tabpanel tabpanelDokumen = new ais.ui.util.MyTabpanel();
		tabpanelDokumen.setParent(tabpanels);

		tab34.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (tabpanelDokumen.getChildren().isEmpty()) {
					MyWindow window = new MyWindow("", "none", false);
					window.setHeight("100%");
					window.setWidth("100%");
					window.setParent(tabpanelDokumen);
					MyInclude iframe = new MyInclude("/pages/master/akreditasi.zul?dosen=" + dosen.getId());
					iframe.setParent(window);
				}
			}
		});

		tabpanels.appendChild(tabpanelPublikasi);
		tabpanels.appendChild(tabpanelPengabdian);
		tabpanels.appendChild(tabpanelPublikasiIlmiah);
		tabpanels.appendChild(tabpanelRiwayatPendidikan);

		if (tampilPegawai) {
			tabpanels.appendChild(tabpanelKepegawian);
		}

		final MyTabConfig tabAngket = new MyTabConfig("Penulis Buku");
		tabAngket.setParent(tabs);

		final Tabpanel tabpanelAngket = new ais.ui.util.MyTabpanel();
		tabpanelAngket.setParent(tabpanels);

		tabAngket.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (tabpanelAngket.getChildren().isEmpty()) {

					Dosen dsn = dosen == null || dosen.getId() == null ? managingDosen.onSave(event) : dosen;
					if (dsn == null || dsn.getId() == null) {
						tab1.setSelected(true);
					} else {
						MyWindow window = new MyWindow("", "none", false);
						window.setHeight("97%");
						window.setWidth("97%");
						window.setParent(tabpanelAngket);
						MyInclude iframe = new MyInclude("/pages/master/buku_bahan_ajar.zul?dosen=" + dsn.getId());
						iframe.setParent(window);
					}
				}
			}
		});

		final MyTabConfig tabIpd = new MyTabConfig("Indeks Prestasi Dosen");
		tabIpd.setParent(tabs);

		final Tabpanel tabpanelIpd = new ais.ui.util.MyTabpanel();
		tabpanelIpd.setParent(tabpanels);

		tabIpd.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (tabpanelIpd.getChildren().isEmpty()) {

					Dosen dsn = dosen == null || dosen.getId() == null ? managingDosen.onSave(event) : dosen;
					if (dsn == null || dsn.getId() == null) {
						tab1.setSelected(true);
					} else {

						LaporanAngketDosenPerDosenWindow dosenWindow = new LaporanAngketDosenPerDosenWindow(dsn);
						dosenWindow.setHeight("100%");
						dosenWindow.setWidth("100%");
						dosenWindow.setParent(tabpanelIpd);

					}
				}
			}
		});

		final MyTabConfig tabAngketUmum = new MyTabConfig("Angket Umum");
		tabAngketUmum.setParent(tabs);

		final Tabpanel tabpanelAngketUmum = new ais.ui.util.MyTabpanel();
		tabpanelAngketUmum.setParent(tabpanels);

		tabAngketUmum.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (tabpanelAngketUmum.getChildren().isEmpty()) {
					MyWindow window = new MyWindow("", "none", false);
					window.setHeight("97%");
					window.setWidth("97%");
					window.setParent(tabpanelAngketUmum);
					MyInclude iframe = new MyInclude("/common/checklist_penilaian_umum.zul?dosen=" + dosen.getId());
					iframe.setParent(window);
				}
			}
		});

		MyTabConfig tabMobile = new MyTabConfig("Mobile");
		tabMobile.setParent(tabs);

		tabMobile.setVisible(
				Common.getApakahAdmin() && Common.bolehKonfigurasi("tampilkan_mobile_di_profile_dosen"));

		tab2.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Dosen dsn = dosen == null || dosen.getId() == null ? managingDosen.onSave(event) : dosen;
				if (dsn == null || dsn.getId() == null) {
					tab1.setSelected(true);
				} else {
					managingBiodataDosen.dosen = dsn;

				}
			}
		});

		final Tabpanel tabpanelMobile = new ais.ui.util.MyTabpanel();
		tabpanelMobile.setParent(tabpanels);
		tabpanelMobile.setVisible(tabMobile.isVisible());
		tabMobile.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelMobile.getChildren().isEmpty()) {
					Dosen dsn = dosen == null || dosen.getId() == null ? managingDosen.onSave(arg0) : dosen;
					if (dsn == null || dsn.getId() == null) {
						tab1.setSelected(true);
					} else {
						Tbmuser userId = (Tbmuser) HibernateUtil.currentSession().createCriteria(Tbmuser.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("dosen", dosen)).addOrder(Order.desc("tanggal_dirubah"))
								.setMaxResults(1).uniqueResult();
						if (userId == null) {
							MyMessageboxConfig.show("Data / akun pengguna untuk dosen ini tidak ditemukan",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}
						MainHelper.onDapatkanKode(userId, tabpanelMobile, false);
					}
				}
			}

		});

//
		boolean tampilRiwayatMediaSosial = Common.bolehKonfigurasi("tampilRiwayatMediaSosial");
		if (tampilRiwayatMediaSosial) {
			final MyTabConfig tabSosial = new MyTabConfig("Media Sosial");
			tabSosial.setParent(tabs);
			final Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			tabpanel.setParent(tabpanels);

			Common.displaySocialMedia(tabSosial, tabpanel, null, dosen, null, null);
		}

		ais.ui.util.MyButtonTabbox.gantiTabboxNative(tabbox, new int[] { 1 });

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		if (refresh != null && refresh) {
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Keluar", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.goLogoff();
				}
			});
			cancel.setParent(toolbar);
		} else {

			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					BiodataDosenAction.this.setVisible(false);
					if (fotoEventListener != null) {
						fotoEventListener.onEvent(event);
					}

					try {
						BiodataDosenAction.this.detach();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BiodataDosenAction.java:966");
						// TODO: handle exception
					}
				}
			});
			cancel.setParent(toolbar);
		}

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(final Event event) throws Exception {

				final Dosen dosen = managingDosen.onSave(event);
				if (dosen != null && dosen.getId() != null) {

					if (Common.getCurrentUser().getDosen() != null && Common.getCurrentUser().getDosen().getId() != null
							&& dosen.getId().equals(Common.getCurrentUser().getDosen().getId())) {
						Common.getCurrentUser().setDosen(dosen);
					}

					managingBiodataDosen.dosen = dosen;

					boolean result = managingBiodataDosen.onSave(event);

					if (fotoEventListener != null) {
						fotoEventListener.onEvent(event);
					}

					if (biodataPegawaiAction != null) {
						Common.createDefaultTimerNoBusy(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								biodataPegawaiAction.save(event);
							}
						}, "", false, 2000);
					}

					Tbmuser tbmuser = Common.getCurrentUser();
					if (tbmuser != null && tbmuser.ambilDosen() != null
							&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
							&& tbmuser.getDosen().getId().equals(managingBiodataDosen.dosen.getId())) {
						tbmuser.setDosen(managingBiodataDosen.dosen);
					}

					if (result) {

						if (refresh != null && refresh) {

							Clients.confirmClose(null);
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Executions.sendRedirect("/main");
								}
							});

						}
						if (commonOnSearchdefault != null)
							commonOnSearchdefault.onSearchDefault(event);
					}

					if (eventListener != null) {
						eventListener.onEvent(new Event("", event.getTarget(), dosen));
					}

					try {
						BiodataDosenAction.this.detach();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BiodataDosenAction.java:1037");
						// TODO: handle exception
					}

				}
			}
		});
		save.setParent(toolbar);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Daftar Riwayat Hidup Dosen", "/img/print.png");
		button.setTooltiptext("Cetak Daftar Riwayat Hidup");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings({})
			@Override
			public void onEvent(Event event) throws Exception {
				DosenAction.cetakDRHDosen(dosen);
			}

		});
		button.setParent(toolbar);

	}

	// @SuppressWarnings("unused")
	// private Borderlayout initPublikasi() throws Exception {
	// if (dosenPub == null || dosenPub.getId() == null) {
	//
	// return new ais.ui.util.MyBorderlayout();
	// }
	//
	// Borderlayout borderlayout = publikasiDosenHelper.display(dosenPub);
	//
	// return borderlayout;
	// }

	public void setCommonOnSearchdefault(CommonOnSearchdefault commonOnSearchdefault) {
		this.commonOnSearchdefault = commonOnSearchdefault;
	}

	public CommonOnSearchdefault getCommonOnSearchdefault() {
		return commonOnSearchdefault;
	}

	public static class ManagingProdiYangDiajar {

		public ManagingProdiYangDiajar() {

		}

		public Tabpanel init(final Dosen dosen, final Tabpanel bidangPendidikan) throws Exception {
			final Tabpanel panel = bidangPendidikan == null ? new ais.ui.util.MyTabpanel() : bidangPendidikan;
			panel.setWidth("100%");
			panel.setHeight("100%");

			Borderlayout borderlayout1 = new ais.ui.util.MyBorderlayout();
			borderlayout1.setParent(panel);
			borderlayout1.setStyle("border:0px;");

			Center center = new Center();
			center.setParent(borderlayout1);
			ais.ui.util.ZkCompat.setFlex(center, true);

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(center);
			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			tabs.appendChild(new MyTabConfig("Penugasan Mengajar"));

			MyTabConfig membimbing;
			tabs.appendChild(membimbing = new MyTabConfig("Penugasan Membimbing"));

			MyTabConfig menguji;
			tabs.appendChild(menguji = new MyTabConfig("Penugasan Menguji"));

			MyTabConfig kkn;
			tabs.appendChild(kkn = new MyTabConfig("Membimbing KKN"));

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			tabpanel.setParent(tabpanels);

			penugasanMengajar(tabpanel, dosen);

			final Tabpanel tabpanelMembimbing = new ais.ui.util.MyTabpanel();
			tabpanelMembimbing.setParent(tabpanels);

			membimbing.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelMembimbing.getChildren().isEmpty()) {
						penugasanMembimbing(tabpanelMembimbing, dosen);
					}
				}
			});

			final Tabpanel tabpanelMenguji = new ais.ui.util.MyTabpanel();
			tabpanelMenguji.setParent(tabpanels);

			menguji.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelMenguji.getChildren().isEmpty()) {
						penugasanPenguji(tabpanelMenguji, dosen);
					}
				}
			});

			final Tabpanel tabpanelKkn = new ais.ui.util.MyTabpanel();
			tabpanelKkn.setParent(tabpanels);

			kkn.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelKkn.getChildren().isEmpty()) {
						penugasanKkn(tabpanelKkn, dosen);
					}
				}
			});

			ais.ui.util.MyButtonTabbox.gantiTabboxNative(tabbox, new int[] { 1 });

			return panel;
		}

		private void penugasanMengajar(Tabpanel tabpanel, final Dosen dosen) throws Exception {
			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(tabpanel);
			borderlayout.setStyle("border:0px;");
			borderlayout.setHeight("100%");
			borderlayout.setWidth("100%");

			North north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, true);

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(north);

			toolbar.appendChild(new MyLabelConfig("Tahun Akademik"));
			final Combobox ta = Common.generateTahunAjaranDanSemua(null);
			Common.selectComboItem(ta, Common.getCurrentTahunAkademik());
			toolbar.appendChild(ta);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("SK", "/img/svg/printer.svg");
			button.setTooltiptext("Cetak SK");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					LaporanSKDosen buktiPengeluaranKas = new LaporanSKDosen(dosen);
					buktiPengeluaranKas.setTitle("Laporan");
					buktiPengeluaranKas.setClosable(true);
					buktiPengeluaranKas.setHeight("90%");
					buktiPengeluaranKas.setWidth("1200px");
					buktiPengeluaranKas.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					buktiPengeluaranKas.onModal();
				}
			});
			button.setParent(toolbar);

			final Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(center);
					BiodataDosenAction.reloadDosen(center, dosen,
							(String) (ta.getSelectedItem() == null ? null : ta.getSelectedItem().getValue()), null);
				}
			};

			ta.addEventListener("onChange", eventListener);

			eventListener.onEvent(null);
		}

		private void penugasanMembimbing(Tabpanel tabpanel, final Dosen dosen) throws Exception {
			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(tabpanel);
			borderlayout.setStyle("border:0px;");
			borderlayout.setHeight("100%");
			borderlayout.setWidth("100%");

			North north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, true);

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(north);

			toolbar.appendChild(new MyLabelConfig("Tahun Akademik"));
			final Combobox ta = Common.generateTahunAjaranDanSemua(null);
			Common.selectComboItem(ta, Common.getCurrentTahunAkademik());
			toolbar.appendChild(ta);

			final Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(center);
					BiodataDosenAction.reloadDosenBimbingan(center, dosen,
							(String) (ta.getSelectedItem() == null ? null : ta.getSelectedItem().getValue()), null);
				}
			};

			ta.addEventListener("onChange", eventListener);

			eventListener.onEvent(null);
		}

		private void penugasanPenguji(Tabpanel tabpanel, final Dosen dosen) throws Exception {
			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(tabpanel);
			borderlayout.setStyle("border:0px;");
			borderlayout.setHeight("100%");
			borderlayout.setWidth("100%");

			North north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, true);

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(north);

			toolbar.appendChild(new MyLabelConfig("Tahun Akademik"));
			final Combobox ta = Common.generateTahunAjaranDanSemua(null);
			Common.selectComboItem(ta, Common.getCurrentTahunAkademik());
			toolbar.appendChild(ta);

			final Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(center);
					BiodataDosenAction.reloadDosenPenguji(center, dosen,
							(String) (ta.getSelectedItem() == null ? null : ta.getSelectedItem().getValue()), null);
				}
			};

			ta.addEventListener("onChange", eventListener);

			eventListener.onEvent(null);
		}

		private void penugasanKkn(Tabpanel tabpanel, final Dosen dosen) throws Exception {
			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(tabpanel);
			borderlayout.setStyle("border:0px;");
			borderlayout.setHeight("100%");
			borderlayout.setWidth("100%");

			North north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, true);

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(north);

			toolbar.appendChild(new MyLabelConfig("Tahun Akademik"));
			final Combobox ta = Common.generateTahunAjaranDanSemua(null);
			Common.selectComboItem(ta, Common.getCurrentTahunAkademik());
			toolbar.appendChild(ta);

			final Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(center);
					BiodataDosenAction.reloadDosenKkn(center, dosen,
							(String) (ta.getSelectedItem() == null ? null : ta.getSelectedItem().getValue()), null);
				}
			};

			ta.addEventListener("onChange", eventListener);

			eventListener.onEvent(null);
		}
	}

	public static class ManagingDosen {

		private Textbox code;
		private Textbox mycode;
		private Textbox nidn;

		private Textbox niyNigk;
		private Textbox nuptk;

		private Textbox skCpns;
		private MyDatebox tglSkCpns;
		private Textbox skAngkat;
		private MyDatebox tmtSkAngkat;
		private MyDatebox tmtPns;

		// private Textbox ktp;
		private Textbox nama;
		// private Textbox golongan;
		// private Textbox alamat;

		private Textbox gelarDepan;
		private Textbox gelarBelakang;

		private Textbox email;
		private Textbox telp;
		private Combobox kelamin;
		private Textbox tempatlahir;
		private MyDatebox tanggallahir;
		private Textbox pangkat;
		private Combobox cbJabatan;
		private Combobox cbJabatanPtLain;
		private Textbox jabatan;
		private Textbox spesialisasi1;
		private Textbox spesialisasi2;
		private Textbox spesialisasi3;
		private MyCheckboxConfig tetap;

		private Dosen dosen;
		private Combobox perguruanTinggi;
		private Combobox fakultas;
		private Combobox jurusan;

		private Combobox statusPegawai;
		private MyCheckboxConfig milikUniversitas;
		private Combobox ikatanKerjaDosen;
		private Combobox statusKepegawaian;
		private Combobox golonganPegawai;
		private Combobox sumberGaji;

		private MyCheckboxConfig aLisensiKepsek;
		private Intbox jmlSekolahBinaan;
		private MyCheckboxConfig aDiklatAwas;
		private Textbox aktaIjinAjar;
		private Textbox nira;
		private MyCheckboxConfig aBraille;
		private MyCheckboxConfig aBhsIsyarat;
		private Textbox npwp;

		private Combobox jenisPendidikDanTenagaKependidikan;
		private Combobox lembagaPengangkat;
		private Combobox jabatanFungsionalDosen;
		private Combobox bahasa;
		private Combobox pendidikan;
		private Textbox pendidikans1;
		private Textbox pendidikans2;
		private Textbox pendidikans3;
		private Textbox feeder;
		private Textbox idRegPtk;
		private MyCheckboxConfig sertifikasi;
		protected LampiranLain lainMahasiswa;
		private MyCheckboxConfig sesuaiBidangKeilmuan;
		private AmbilDataRuangBanbox ruang;
		protected LampiranLain ttd;
		private Textbox nomorSertifikasi;
		protected AmbilDataDosenBanbox atasanlangsung;
		private Textbox idfinger;
		protected FotoDosen fotoDosen;
		private Textbox googleScholar;
		private Combobox onlineMenggunakan;
		private Row rowMeetKeterangan;
		private Textbox onlineLink;
		private Row rowLinkZoomKeterangan;
		private Row rowLinkZoomLink;
		private Row rowLinkBbbKeterangan;
		private Row rowLinkBbbLink;
		private Row rowLinkSkypeKeterangan;
		private Row rowLinkSkypeLink;
		private Row rowLinkWaKeterangan;
		private Row rowLinkLainKeterangan;
		private Row rowOnlineLink;
		private Textbox kodeSinta;
		private MyLabelConfig labelCode;
		private MyLabelConfig labelMycode;
		private MyLabelConfig labelNidn;
		private boolean simpanPegawai = true;

		public ManagingDosen() {
			perguruanTinggi = new Combobox();
			fakultas = new Combobox();
			jurusan = new Combobox();

			PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();

			Common.insertCombo(perguruanTinggi, "nama", PerguruanTinggi.class,
					pt == null || pt.getId() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.idEq(pt.getId()));

			Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

			if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
				Fakultas fak = (Fakultas) fakultas.getSelectedItem().getValue();
				if (fak.getPerguruanTinggi() != null) {
					Common.selectComboItem(true, perguruanTinggi, fak.getPerguruanTinggi());
					perguruanTinggi.setDisabled(fakultas.isDisabled());
				}
			}
			perguruanTinggi.setDisabled(true);
			// Common.insertCombo(fakultas, new String[] { "nama", "kode" },
			// Fakultas.class);
			// tanpaHomebaseFakultas = new MyComboitemConfig("Tanpa Homebase");
			// tanpaHomebaseFakultas.setValue(null);
			// fakultas.appendChild(tanpaHomebaseFakultas);
			// class FakultasEventListener implements EventListener {
			//
			// @Override
			// public void onEvent(Event event) throws Exception {
			// // TODO Auto-generated method stub
			// Common.clear(jurusan);
			// jurusan.setSelectedItem(null);
			// if (fakultas.getSelectedItem() == null ||
			// fakultas.getSelectedItem().getValue() == null) {
			// return;
			// }
			// Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed"
			// }, "jenjang", Jurusan.class,
			// Restrictions.or(Restrictions.isNull("aktif"),
			// Restrictions.eq("aktif", true)),
			// Restrictions.eq("fakultas",
			// fakultas.getSelectedItem().getValue()));
			// MyComboitemConfig comboitem = new MyComboitemConfig("Tanpa
			// Homebase");
			// comboitem.setValue(null);
			// jurusan.appendChild(comboitem);
			//
			// if (jurusan.getSelectedItem() == null ||
			// jurusan.getSelectedItem().getValue() == null) {
			// jurusan.setSelectedItem(comboitem);
			// }
			//
			// }
			//
			// }
			//
			// fakultas.addEventListener("onChange", new
			// FakultasEventListener());

			kelamin = new Combobox();
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel("Laki-laki");
			comboitem.setValue("Laki-laki");
			kelamin.appendChild(comboitem);
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Perempuan");
			comboitem.setValue("Perempuan");
			kelamin.appendChild(comboitem);

		}

		public Tabpanel init(final Dosen dosen, boolean simpanPegawai) throws Exception {
			this.dosen = dosen;
			this.simpanPegawai = simpanPegawai;
			Tabpanel panel = new ais.ui.util.MyTabpanel();
			panel.setWidth("100%");
			panel.setHeight("100%");
			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(panel);
			borderlayout.setStyle("border:0px;");

			West west = new West();
			west.setStyle("border:0px;");
			ais.ui.util.ZkCompat.setFlex(west, true);
			west.setWidth("270px");
			west.setParent(borderlayout);

			Tbmuser tbmuser = Common.getCurrentUser();
			fotoDosen = null;

			boolean bolehgantiFoto = true;
			if (tbmuser != null && tbmuser.ambilDosen() != null
					&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
				bolehgantiFoto = Common.bolehKonfigurasi("dosen_boleh_mengubah_foto_profile");
			}

			Common.createDownloadUploadFoto(west, dosen, FotoDosen.class, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					fotoDosen = (FotoDosen) arg0.getData();
				}
			}, bolehgantiFoto);

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);
			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
			columns.setParent(grid);
			MyColumnConfig column = new MyColumnConfig();
			column.setWidth("40%");
			columns.appendChild(column);
			column = new MyColumnConfig();
			column.setWidth("60%");
			columns.appendChild(column);

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");

			boolean admin = Common.getApakahAdmin();

			row.setParent(rows);
			String statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "code", labelCode = new MyLabelConfig());
			code = new Textbox(dosen.getCode());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? code
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(dosen.getCode())
									: code);
			code.setWidth("90%");

			System.out.println("code -> " + dosen.getCode());

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "mycode", labelMycode = new MyLabelConfig());
			mycode = new Textbox(dosen.getMycode());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? mycode
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getMycode())
									: mycode);
			mycode.setWidth("90%");

			System.out.println("mycode -> " + dosen.getMycode());

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "nidn", labelNidn = new MyLabelConfig());
			nidn = new Textbox(dosen.getNidn());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? nidn
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(dosen.getNidn())
									: nidn);
			nidn.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "npwp");
			npwp = new Textbox(dosen.getNpwp());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? npwp
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(dosen.getNpwp())
									: npwp);
			npwp.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "niyNigk");
			niyNigk = new Textbox(dosen.getNiyNigk());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? niyNigk
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getNiyNigk())
									: niyNigk);
			niyNigk.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "nuptk");
			nuptk = new Textbox(dosen.getNuptk());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? nuptk
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getNuptk())
									: nuptk);
			nuptk.setWidth("90%");
			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "idfinger");
			idfinger = new Textbox(dosen.getIdfinger());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? idfinger
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getIdfinger())
									: idfinger);
			idfinger.setWidth("90%");
			idfinger.setDisabled(tbmuser == null || tbmuser.ambilDosen() != null || tbmuser.getMahasiswa() != null);
			if (Common.bolehKonfigurasi("tampilkan_link_login_oleh_admin_di_data_dosen")) {
				String adminLain = Common.getKonfigurasi("admin_yg_boleh_buka_link_login_dosen", "").getNilai();
				if (dosen.getId() != null && (Common.getApakahAdmin() || Common.getApakahAdmin(adminLain))) {

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Link"));

					final A a = new A("Tampilkan Link");
					a.setHref("");
					row.appendChild(a);

					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							String userId = (String) HibernateUtil.currentSession().createCriteria(Tbmuser.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.setProjection(Projections.property("userId"))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("dosen", dosen)).addOrder(Order.desc("tanggal_dirubah"))
									.setMaxResults(1).uniqueResult();
							if (userId == null) {
								MyMessageboxConfig.show("Data / akun pengguna untuk dosen ini tidak ditemukan",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								return;
							}
							String code = Common.getRequestHostWithProtocol() + "/m?q=" + URLEncoder.encode(
									Common.desEncrypter.get().encrypt(userId + "-user-abcdefghijklmnopqrstuvwxyz"), "UTF-8");
							a.setLabel(code);
							a.setHref(Common.getRequestHostWithProtocol() + "/logoff?param="
									+ URLEncoder.encode(code, "UTF-8"));
						}
					});

					Common.initKeterangan(rows,
							"Link ini bisa digunakan untuk login tanpa menggunakan password, Misal: bisa digunakan untuk Review SPADA");
				}
			}
			row = new MyFormRow();
			row.setVisible(false);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode Sinta"));
			row.appendChild(kodeSinta = new Textbox(dosen.getKodeSinta()));
			kodeSinta.setWidth("90%");

//			Common.initKeterangan(rows,
//					"Jika link sinta dosen https://sinta.kemdikbud.go.id/authors/detail?id=xxxx&view=overview., maka kode sinta dosen adalah xxxx");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode Google Scholar"));
			row.appendChild(googleScholar = new Textbox(dosen.getGoogleScholar()));
			googleScholar.setWidth("90%");

			Common.initKeterangan(rows,
					"Anda bisa mandapatkan kode ini dari https://scholar.google.com, Jika link google scholar Anda adalah https://scholar.google.com/citations?user=Z8ZcJboAAAAJ, maka \"Kode Google Scholar\" anda adalah Z8ZcJboAAAAJ");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "nama");
			nama = new Textbox(dosen.getNama() == null ? "" : dosen.getNama());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? nama
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(dosen.getNama())
									: nama);
			nama.setWidth("90%");

			ttd = null;
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tanda Tangan (PNG) "));
			Hbox hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, dosen.getId(), LampiranLain.TTD_DOSEN, "Tanda Tangan",
					false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							ttd = (LampiranLain) arg0.getData();
						}
					}, null, false, false, false, true, null, false, false);

			hbox.setParent(row);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "kelamin");
			Common.selectComboItem(kelamin, dosen.getKelamin());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? kelamin
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getKelamin())
									: kelamin);
			kelamin.setWidth("90%");
			kelamin.setReadonly(true);

			row = new MyFormRow();
			row.setVisible(ConstantValues.penggunaanLabelBahasa);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Bahasa"));
			row.appendChild(bahasa = new Combobox());
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
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

			Common.selectComboItem(bahasa, dosen.getBahasa());

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "tempatlahir");
			tempatlahir = new Textbox(dosen.getTempatlahir() == null ? "" : dosen.getTempatlahir());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? tempatlahir
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getTempatlahir())
									: tempatlahir);
			tempatlahir.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "tanggallahir");
			tanggallahir = new MyDatebox(
					dosen.getTanggallahir() == null ? ais.ui.util.WaktuUtil.getDate() : dosen.getTanggallahir());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? tanggallahir
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getTanggallahir() == null ? ""
											: Common.dateFormat2.get().format(dosen.getTanggallahir()))
									: tanggallahir);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "telp");
			telp = new Textbox(dosen.getTelp() == null ? "" : dosen.getTelp());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? telp
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(dosen.getTelp())
									: telp);
			telp.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "email");
			email = new Textbox(dosen.getEmail() == null ? "" : dosen.getEmail());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? email
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getEmail())
									: email);
			email.setWidth("90%");
			Common.initKeterangan(rows,
					"Jika email lebih dari satu, gunakan tanda koma (,) sebagai pemisah, misal-nya :  anda@mail.com,anda1@oke.com,anda3@mail.com. Sedangkan untuk email utama, tempatkan di urutan paling depan.");

			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Media Online"));
			onlineMenggunakan = new Combobox();

			Comboitem mediaOnline = new Comboitem("Jitsi", "/img/jitsi.png");
			mediaOnline.setValue(Dosen.JITSI);
			onlineMenggunakan.appendChild(mediaOnline);

			mediaOnline = new Comboitem("Google Meet", "/img/meet-google.png");
			mediaOnline.setValue(Dosen.GOOGLE_MEET);
			onlineMenggunakan.appendChild(mediaOnline);

			mediaOnline = new Comboitem("Zoom", "/img/zoom.png");
			mediaOnline.setValue(Dosen.ZOOM);
			onlineMenggunakan.appendChild(mediaOnline);

			mediaOnline = new Comboitem("Big Blue Button", "/img/bbb.png");
			mediaOnline.setValue(Dosen.BBB);
			onlineMenggunakan.appendChild(mediaOnline);

			mediaOnline = new Comboitem("Skype", "/img/Skype-icon.png");
			mediaOnline.setValue(Dosen.SKYPE);
			onlineMenggunakan.appendChild(mediaOnline);

			mediaOnline = new Comboitem("Grup Whatsapp", "/img/svg/whats.svg");
			mediaOnline.setValue(Pertemuan.WA);
			onlineMenggunakan.appendChild(mediaOnline);

			mediaOnline = new Comboitem("Lain-Lain", "/img/online-red-icon.png");
			mediaOnline.setValue(Pertemuan.LAIN);
			onlineMenggunakan.appendChild(mediaOnline);

			mediaOnline = new Comboitem("Tidak Ada Link Online", "/img/svg/trash.svg");
			mediaOnline.setValue(Dosen.TIDAK_AKTIF);
			onlineMenggunakan.appendChild(mediaOnline);

			Common.selectComboItem(onlineMenggunakan, dosen.getOnlineMenggunakan());
			onlineMenggunakan.setCols(7);

			Hbox myonlineMenggunakan = new Hbox();
			row.appendChild(myonlineMenggunakan);
			myonlineMenggunakan.appendChild(onlineMenggunakan);

			final MyToolbarbuttonConfig testButton = new MyToolbarbuttonConfig("Tes Online Sekarang");
			myonlineMenggunakan.appendChild(testButton);
			testButton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String url = dosen.getOnlineLink();
					if (url == null || url.trim().isEmpty()) {
						MyMessageboxConfig.show("Untuk tatap muka online, harap masukkan link online secara benar.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + url + "', title: 'Video Conference', w: 1200, h: 600});");

					}
				}
			});

			onlineMenggunakan.setReadonly(true);

			Common.initKeterangan(rows,
					"jika dosen mengajar selalu menggunakan link online yang sama, pilihlah salah satu link online, kemudian masukkan link nya di bawah ini secara benar.");

			rowMeetKeterangan = Common.initKeterangan(rows,
					"Untuk tatap muka online menggunakan Google Meet, masukkan link berikut.");

			rowLinkZoomKeterangan = Common.initKeterangan(rows,
					"Untuk tatap muka online menggunakan Zoom, harap memasukkan link zoom di bawah ini. Contoh link zoom : https://us04web.zoom.us/j/4445712881?pwd=ZnNReHRJYXVRem8zRkc5OFpPd3I3QT09");

			rowLinkZoomLink = new MyFormRow();
			rowLinkZoomLink.setValign("top");
			rowLinkZoomLink.setParent(rows);
			rowLinkZoomLink.appendChild(new ais.ui.util.MyLabelConfig(""));
			A linkZoomSignup;
			rowLinkZoomLink.appendChild(linkZoomSignup = new A(
					"Klik disini dan login untuk mendapatkan link zoom yang baru, https://zoom.us/signin"));
			linkZoomSignup.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String server = "https://zoom.us/signin";

					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");

					}
				}
			});

			rowLinkBbbKeterangan = Common.initKeterangan(rows,
					"Untuk tatap muka online menggunakan Big Blue Button, harap memasukkan link Big Blue Button di bawah ini. Contoh link bbb : https://demo.bigbluebutton.org/gl/muh-jjn-72p");

			rowLinkBbbLink = new MyFormRow();
			rowLinkBbbLink.setValign("top");
			rowLinkBbbLink.setParent(rows);
			rowLinkBbbLink.appendChild(new ais.ui.util.MyLabelConfig(""));
			A linkBbbSignup;
			rowLinkBbbLink.appendChild(linkBbbSignup = new A(
					"Klik disini dan login untuk mendapatkan link Big Blue Button yang baru, https://demo.bigbluebutton.org/gl/signin"));

			linkBbbSignup.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String server = "https://demo.bigbluebutton.org/gl/signin";

					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");

					}
				}
			});

			rowLinkSkypeKeterangan = Common.initKeterangan(rows,
					"Untuk tatap muka online menggunakan Skype, harap memasukkan link Skype di bawah ini. Contoh link skype : https://join.skype.com/Ut2b1onFnJnD");

			rowLinkSkypeLink = new MyFormRow();
			rowLinkSkypeLink.setValign("top");
			rowLinkSkypeLink.setParent(rows);
			rowLinkSkypeLink.appendChild(new ais.ui.util.MyLabelConfig(""));
			A linkSkypeSignup;
			rowLinkSkypeLink.appendChild(linkSkypeSignup = new A(
					"Klik disini dan login untuk mendapatkan link Skype yang baru, https://web.skype.com"));

			linkSkypeSignup.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String server = "https://web.skype.com";

					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");

					}
				}
			});

			rowLinkWaKeterangan = Common.initKeterangan(rows,
					"Untuk tatap muka online menggunakan Grup WA, harap memasukkan link WA di bawah ini. Untuk membuat link Grup WA, buka aplikasi WA Grup Anda (harus sebagai admin) atau buat grup WA baru, pilih Grup Info, dan pilih undang via link.. Contoh link : https://chat.whatsapp.com/Djx0r98Z30YTmFmEZGJ3");

			rowLinkLainKeterangan = Common.initKeterangan(rows,
					"Untuk tatap muka online menggunakan media onlien lain, harap memasukkan link media tersebut di bawah ini.");

			rowOnlineLink = new MyFormRow();
			rowOnlineLink.setValign("top");
			rowOnlineLink.setParent(rows);
			rowOnlineLink.appendChild(new ais.ui.util.MyLabelConfig("Link Online *"));
			rowOnlineLink.appendChild(onlineLink = new Textbox(dosen.getOnlineLink()));
			onlineLink.setWidth("90%");
			onlineLink.setRows(2);

			onlineLink.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					dosen.setOnlineLink(onlineLink.getValue().trim());
				}
			});

			EventListener eventListenerOl = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Integer ol = (Integer) onlineMenggunakan.getSelectedItem().getValue();

					rowOnlineLink.setVisible(!ol.equals(Dosen.TIDAK_AKTIF));

					rowMeetKeterangan.setVisible(ol.equals(Dosen.GOOGLE_MEET));

					rowLinkZoomKeterangan.setVisible(ol.equals(Dosen.ZOOM));
					rowLinkZoomLink.setVisible(ol.equals(Dosen.ZOOM));

					rowLinkBbbKeterangan.setVisible(ol.equals(Dosen.BBB));
					rowLinkBbbLink.setVisible(ol.equals(Dosen.BBB));

					rowLinkSkypeKeterangan.setVisible(ol.equals(Dosen.SKYPE));
					rowLinkSkypeLink.setVisible(ol.equals(Dosen.SKYPE));

					rowLinkWaKeterangan.setVisible(ol.equals(Dosen.WA));

					rowLinkLainKeterangan.setVisible(ol.equals(Dosen.LAIN));

					testButton.setVisible(true);
					if (ol.equals(Dosen.GOOGLE_MEET)) {
						testButton.setImage("/img/meet-google.png");
					} else if (ol.equals(Dosen.JITSI)) {
						testButton.setImage("/img/jitsi.png");
					} else if (ol.equals(Dosen.ZOOM)) {
						testButton.setImage("/img/zoom.png");
					} else if (ol.equals(Dosen.BBB)) {
						testButton.setImage("/img/bbb.png");
					} else if (ol.equals(Dosen.SKYPE)) {
						testButton.setImage("/img/Skype-icon.png");
					} else if (ol.equals(Dosen.WA)) {
						testButton.setImage("/img/svg/whats.svg");
					} else if (ol.equals(Dosen.LAIN)) {
						testButton.setImage("/img/online-red-icon.png");
					} else {
						testButton.setVisible(false);
					}

				}
			};

			onlineMenggunakan.addEventListener("onChange", eventListenerOl);
			eventListenerOl.onEvent(null);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "gelarDepan");
			gelarDepan = new Textbox(dosen.getGelarDepan());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? gelarDepan
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getGelarDepan())
									: gelarDepan);
			gelarDepan.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "gelarBelakang");
			gelarBelakang = new Textbox(dosen.getGelarBelakang());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? gelarBelakang
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getGelarBelakang())
									: gelarBelakang);
			gelarBelakang.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "ikatanKerjaDosen");
			ikatanKerjaDosen = new Combobox();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? ikatanKerjaDosen
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(
									dosen.getIkatanKerjaDosen() == null ? "" : dosen.getIkatanKerjaDosen().getNama())
									: ikatanKerjaDosen);
			Common.insertComboDanSemua(ikatanKerjaDosen, new String[] { "nama", "feeder" }, "keterangan",
					IkatanKerjaDosen.class, "Ikatan Kerja Belum Ditentulan", Restrictions.ne("nama", ""),
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.selectComboItem(ikatanKerjaDosen, dosen.getIkatanKerjaDosen());
			ikatanKerjaDosen.setWidth("90%");
			ikatanKerjaDosen.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "statusKepegawaian");
			statusKepegawaian = new Combobox();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? statusKepegawaian
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(
									dosen.getStatusKepegawaian() == null ? "" : dosen.getStatusKepegawaian().getNama())
									: statusKepegawaian);
			Common.insertComboDanSemua(statusKepegawaian, new String[] { "nama" }, "keterangan",
					StatusKepegawaian.class, "Status Kepegawaian Belum Ditentulan", Restrictions.ne("nama", ""),
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.selectComboItem(statusKepegawaian, dosen.getStatusKepegawaian());
			statusKepegawaian.setWidth("90%");
			statusKepegawaian.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "jenisPendidikDanTenagaKependidikan");
			jenisPendidikDanTenagaKependidikan = new Combobox();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
					? jenisPendidikDanTenagaKependidikan
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(
											dosen.getJenisPendidikDanTenagaKependidikan() == null ? ""
													: dosen.getJenisPendidikDanTenagaKependidikan().getNama())
									: jenisPendidikDanTenagaKependidikan);
			Common.insertComboDanSemua(jenisPendidikDanTenagaKependidikan, new String[] { "nama" }, "keterangan",
					JenisPendidikDanTenagaKependidikan.class, "Belum Ditentulan", Restrictions.ne("nama", ""),
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.selectComboItem(jenisPendidikDanTenagaKependidikan, dosen.getJenisPendidikDanTenagaKependidikan());
			jenisPendidikDanTenagaKependidikan.setWidth("90%");
			jenisPendidikDanTenagaKependidikan.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "jabatanFungsionalDosen");
			jabatanFungsionalDosen = new Combobox();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? jabatanFungsionalDosen
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getJabatanFungsionalDosen() == null ? ""
											: dosen.getJabatanFungsionalDosen().getNama())
									: jabatanFungsionalDosen);
			Common.insertComboDanSemua(jabatanFungsionalDosen, new String[] { "nama" }, "keterangan",
					JabatanFungsionalDosen.class, "Belum Ditentulan",
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.ne("nama", ""));
			Common.selectComboItem(jabatanFungsionalDosen, dosen.getJabatanFungsionalDosen());
			jabatanFungsionalDosen.setWidth("90%");
			jabatanFungsionalDosen.setReadonly(true);

			row = new MyFormRow();
			row.setVisible(false);
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "tetap");
			row.appendChild(tetap = new MyCheckboxConfig());
			tetap.setChecked(dosen.getTetap() != null && dosen.getTetap().equals(1));

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "sertifikasi");
			sertifikasi = new MyCheckboxConfig();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? sertifikasi
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getSertifikasi() ? "Ya" : "Yidak")
									: sertifikasi);
			sertifikasi.setChecked(dosen.getSertifikasi());

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "nomorSertifikasi");
			nomorSertifikasi = new Textbox(dosen.getNomorSertifikasi());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? nomorSertifikasi
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getNomorSertifikasi())
									: nomorSertifikasi);
			nomorSertifikasi.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new Label());
			Vbox myVbox = new Vbox();
			myVbox.setParent(row);

			lainMahasiswa = null;

			hbox = new Hbox();
			hbox.setParent(myVbox);
			LampiranLain.createDownloadUploadFileLain(hbox, dosen.getId(), LampiranLain.SERTIFIKASI_DOSEN,
					LampiranLain.SERTIFIKASI_DOSEN, false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							lainMahasiswa = (LampiranLain) arg0.getData();
						}
					}, null, false, false, false, true);

			row = new MyFormRow();

			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "statusPegawai");
			statusPegawai = new Combobox();
			Common.insertCombo(statusPegawai, "nama", StatusPegawai.class, Restrictions.eq("aktif", true),
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.ne("nama", ""));
			Common.selectComboItem(statusPegawai, dosen.getStatusPegawai());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? statusPegawai
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(
											dosen.getStatusPegawai() == null ? "" : dosen.getStatusPegawai().getNama())
									: statusPegawai);
			statusPegawai.setWidth("90%");
			statusPegawai.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "skCpns");
			skCpns = new Textbox(dosen.getSkCpns());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? skCpns
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getSkCpns())
									: skCpns);
			skCpns.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "tglSkCpns");
			tglSkCpns = new MyDatebox(dosen.getTglSkCpns());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? tglSkCpns
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(
									dosen.getTglSkCpns() == null ? "" : Common.dateFormat2.get().format(dosen.getTglSkCpns()))
									: tglSkCpns);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "tmtPns");
			tmtPns = new MyDatebox(dosen.getTmtPns());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? tmtPns
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(
									dosen.getTmtPns() == null ? "" : Common.dateFormat2.get().format(dosen.getTmtPns()))
									: tmtPns);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "Golongan");
			golonganPegawai = new Combobox();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? golonganPegawai
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(
											dosen.getGolonganPns() == null ? "" : dosen.getGolonganPns().getNama())
									: golonganPegawai);
			Common.insertComboDanSemua(golonganPegawai, new String[] { "kode", "nama" }, "keterangan",
					GolonganPns.class, "=Tanpa Golongan=", Restrictions.eq("aktif", true));
			Common.selectComboItem(golonganPegawai, dosen.getGolonganPns());
			golonganPegawai.setWidth("90%");
			golonganPegawai.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "skAngkat");
			skAngkat = new Textbox(dosen.getSkAngkat());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? skAngkat
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getSkAngkat())
									: skAngkat);
			skAngkat.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "tmtSkAngkat");
			tmtSkAngkat = new MyDatebox(dosen.getTmtSkAngkat());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? tmtSkAngkat
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getTmtSkAngkat() == null ? ""
											: Common.dateFormat2.get().format(dosen.getTmtSkAngkat()))
									: tmtSkAngkat);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "lembagaPengangkat");
			lembagaPengangkat = new Combobox();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? lembagaPengangkat
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(
									dosen.getLembagaPengangkat() == null ? "" : dosen.getLembagaPengangkat().getNama())
									: lembagaPengangkat);
			Common.insertCombo(lembagaPengangkat, new String[] { "nama" }, LembagaPengangkat.class,
					Restrictions.ne("nama", ""),
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.selectComboItem(lembagaPengangkat, dosen.getLembagaPengangkat());
			lembagaPengangkat.setWidth("90%");
			lembagaPengangkat.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "sumberGaji");
			sumberGaji = new Combobox();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? sumberGaji
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(
											dosen.getSumberGaji() == null ? "" : dosen.getSumberGaji().getNama())
									: sumberGaji);
			Common.insertCombo(sumberGaji, new String[] { "nama" }, SumberGaji.class, Restrictions.ne("nama", ""),
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.selectComboItem(sumberGaji, dosen.getSumberGaji());
			sumberGaji.setWidth("90%");
			sumberGaji.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "aLisensiKepsek");
			aLisensiKepsek = new MyCheckboxConfig();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? aLisensiKepsek
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getaLisensiKepsek() ? "Ya" : "Yidak")
									: aLisensiKepsek);
			aLisensiKepsek.setChecked(dosen.getaLisensiKepsek());

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "jmlSekolahBinaan");
			jmlSekolahBinaan = new Intbox(dosen.getJmlSekolahBinaan());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? jmlSekolahBinaan
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getJmlSekolahBinaan() == null ? ""
											: Common.numberFormat.get().format(dosen.getJmlSekolahBinaan()))
									: jmlSekolahBinaan);
			jmlSekolahBinaan.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "aDiklatAwas");
			aDiklatAwas = new MyCheckboxConfig();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? aDiklatAwas
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getaDiklatAwas() ? "Ya" : "Yidak")
									: aDiklatAwas);
			aDiklatAwas.setChecked(dosen.getaDiklatAwas());

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "aktaIjinAjar");
			aktaIjinAjar = new Textbox(dosen.getAktaIjinAjar());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? aktaIjinAjar
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getAktaIjinAjar())
									: aktaIjinAjar);
			aktaIjinAjar.setRows(2);
			aktaIjinAjar.setMaxlength(1);

			row = new MyFormRow();
			row.setVisible(false);
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "nira");
			nira = new Textbox(dosen.getNira());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? nira
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(dosen.getNira())
									: nira);
			nira.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "aBraille");
			aBraille = new MyCheckboxConfig();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? aBraille
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getaBraille() ? "Ya" : "Yidak")
									: aBraille);
			aBraille.setChecked(dosen.getaBraille());

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "aBhsIsyarat");
			aBhsIsyarat = new MyCheckboxConfig();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? aBhsIsyarat
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getaBhsIsyarat() ? "Ya" : "Yidak")
									: aBhsIsyarat);
			aBhsIsyarat.setChecked(dosen.getaBhsIsyarat());

			row = new MyFormRow();
			row.setVisible(false);
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "pangkat");
			pangkat = new Textbox(dosen.getPangkat() == null ? "" : dosen.getPangkat());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? pangkat
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getPangkat())
									: pangkat);
			pangkat.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "cbJabatan");
			Common.insertCombo(cbJabatan = new Combobox(), "nama", "keterangan", Jabatan.class,
					Restrictions.ne("nama", ""),
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.or(Restrictions.isNull("ptSendiri"), Restrictions.eq("ptSendiri", true)));
			Common.selectComboItem(cbJabatan,
					dosen.getSpesifikasiJabatan() == null ? null : dosen.getSpesifikasiJabatan());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? cbJabatan
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getSpesifikasiJabatan() == null ? ""
											: dosen.getSpesifikasiJabatan().getNama())
									: cbJabatan);
			cbJabatan.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "cbJabatanPtLain");
			Common.insertCombo(cbJabatanPtLain = new Combobox(), "nama", "keterangan", Jabatan.class,
					Restrictions.ne("nama", ""),
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.and(Restrictions.isNotNull("ptSendiri"), Restrictions.eq("ptSendiri", false)));
			Common.selectComboItem(cbJabatanPtLain,
					dosen.getSpesifikasiJabatan() == null ? null : dosen.getSpesifikasiJabatan());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? cbJabatanPtLain
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getSpesifikasiJabatan() == null ? ""
											: dosen.getSpesifikasiJabatan().getNama())
									: cbJabatanPtLain);
			cbJabatanPtLain.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "jabatan");
			jabatan = new Textbox(dosen.getJabatan() == null ? "" : dosen.getJabatan());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? jabatan
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getJabatan())
									: jabatan);
			jabatan.setRows(2);
			jabatan.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "pendidikan");
			pendidikan = new Combobox();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? pendidikan
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(
											dosen.getPendidikan() == null ? "" : dosen.getPendidikan().getNama())
									: pendidikan);
			Common.insertCombo(pendidikan, "nama", Pendidikan.class);
			Common.selectComboItem(pendidikan, dosen.getPendidikan());
			pendidikan.setWidth("90%");
			pendidikan.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "pendidikans1");
			pendidikans1 = new Textbox(dosen.getPendidikans1());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? pendidikans1
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getPendidikans1())
									: pendidikans1);
			pendidikans1.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "pendidikans2");
			pendidikans2 = new Textbox(dosen.getPendidikans2());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? pendidikans2
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getPendidikans2())
									: pendidikans2);
			pendidikans2.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "pendidikans3");
			pendidikans3 = new Textbox(dosen.getPendidikans3());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? pendidikans3
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getPendidikans3())
									: pendidikans3);
			pendidikans3.setWidth("90%");

			row = new MyFormRow();
			row.setVisible(false);
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "spesialisasi1");
			spesialisasi1 = new Textbox(dosen.getSpesialisasi1() == null ? "" : dosen.getSpesialisasi1());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? spesialisasi1
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getSpesialisasi1())
									: spesialisasi1);
			spesialisasi1.setWidth("90%");
			spesialisasi1.setRows(2);

			row = new MyFormRow();
			row.setVisible(false);

			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "spesialisasi2");
			spesialisasi2 = new Textbox(dosen.getSpesialisasi2() == null ? "" : dosen.getSpesialisasi2());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? spesialisasi2
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getSpesialisasi2())
									: spesialisasi2);
			spesialisasi2.setWidth("90%");
			spesialisasi2.setRows(2);

			row = new MyFormRow();
			row.setVisible(false);

			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "spesialisasi3");
			spesialisasi3 = new Textbox(dosen.getSpesialisasi3() == null ? "" : dosen.getSpesialisasi3());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? spesialisasi3
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getSpesialisasi3())
									: spesialisasi3);
			spesialisasi3.setWidth("90%");
			spesialisasi3.setRows(2);

			MyFormRow rowAtasan = new MyFormRow();
			rowAtasan.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(rowAtasan, "atasanlangsung");

			Dosen atasan = dosen.getAtasanlangsung() == null ? null
					: (Dosen) ConstantValues.ambil(Dosen.class.getName(), dosen.getAtasanlangsung());
			atasanlangsung = new AmbilDataDosenBanbox(true);
			rowAtasan.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? atasanlangsung
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(atasan == null ? "" : atasan.getNama())
									: atasanlangsung);

			atasanlangsung.setAttribute("dosen", atasan);
			atasanlangsung.setValue(atasan == null ? "" : atasan.getNama());
			atasanlangsung.setWidth("90%");
			atasanlangsung.setReadonly(true);

			boolean tampilkanAsesor = (Common.bolehKonfigurasi("tampilkan_asesor"));

			if (dosen != null && dosen.getId() != null && dosen.getPegawaiId() != null && tampilkanAsesor) {
				Session session = HibernateUtil.currentSession();
				@SuppressWarnings("unchecked")
				List<AsesorPenunjangKinerjaDosen> asesorPenunjangKinerjaDosens = session
						.createCriteria(AsesorPenunjangKinerjaDosen.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
				for (final AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen : asesorPenunjangKinerjaDosens) {
					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new Label(asesorPenunjangKinerjaDosen.getNama()));

					@SuppressWarnings("unchecked")
					List<String> usernames = session.createCriteria(Asesor.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("asesorPenunjangKinerjaDosen", asesorPenunjangKinerjaDosen))
							.createAlias("tbmuser", "tbmuser").setProjection(Projections.property("tbmuser.userId"))
							.add(Restrictions.eq("tbmuser.aktif", true)).list();

					AsesorPegawai asesorPegawai = (AsesorPegawai) session.createCriteria(AsesorPegawai.class)
							.add(Restrictions.eq("pegawai.id", dosen.getPegawaiId())).createAlias("asesor", "asesor")
							.add(Restrictions.or(Restrictions.isNull("asesor.aktif"),
									Restrictions.eq("asesor.aktif", true)))
							.add(Restrictions.eq("asesor.asesorPenunjangKinerjaDosen", asesorPenunjangKinerjaDosen))
							.setMaxResults(1).uniqueResult();

					final AmbilDataTbmuserBanbox ambilAsesor = new AmbilDataTbmuserBanbox(usernames);

					row.appendChild(ambilAsesor);
					ambilAsesor.setAttribute("tbmuser",
							asesorPegawai == null || asesorPegawai.getAsesor() == null ? null
									: asesorPegawai.getAsesor().getTbmuser());
					ambilAsesor.setValue(asesorPegawai == null || asesorPegawai.getAsesor() == null
							|| asesorPegawai.getAsesor().getTbmuser() == null ? ""
									: asesorPegawai.getAsesor().getTbmuser().getUserNama());
					ambilAsesor.setWidth("90%");
					ambilAsesor.setReadonly(true);

					ambilAsesor.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Tbmuser tbmuser = (Tbmuser) ambilAsesor.getAttribute("tbmuser");
							if (tbmuser != null) {
								Session session = HibernateUtil.currentSession();
								Asesor asesor = (Asesor) session.createCriteria(Asesor.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("tbmuser", tbmuser)).add(Restrictions
												.eq("asesorPenunjangKinerjaDosen", asesorPenunjangKinerjaDosen))
										.setMaxResults(1).uniqueResult();
								if (asesor != null) {
									AsesorPegawai asesorPegawai = (AsesorPegawai) session
											.createCriteria(AsesorPegawai.class)
											.add(Restrictions.eq("pegawai.id", dosen.getPegawaiId()))
											.add(Restrictions.eq("asesor", asesor)).setMaxResults(1).uniqueResult();
									if (asesorPegawai == null) {
										asesorPegawai = new AsesorPegawai();
									}
									asesorPegawai.setAsesor(asesor);
									asesorPegawai.setPegawai(new Pegawai(dosen));
									Common.refreshSaveOrUpdate(session, asesorPegawai);
								}
							}
						}
					});
				}
			}

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Ruang kerja dosen")));
			row.appendChild(ruang = new AmbilDataRuangBanbox());
			ruang.setAttribute("ruang", dosen.getRuang());
			ruang.setValue(dosen.getRuang() == null ? "" : dosen.getRuang().getNama());
			ruang.setWidth("90%");
			ruang.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "perguruanTinggi");
			Common.selectComboItem(true, perguruanTinggi,
					dosen.getPerguruanTinggi() == null
							? (tbmuser.ambilFakultas() != null ? tbmuser.ambilFakultas().getPerguruanTinggi() : null)
							: dosen.getPerguruanTinggi());

			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? perguruanTinggi
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(
									dosen.getPerguruanTinggi() == null ? "" : dosen.getPerguruanTinggi().getNama())
									: perguruanTinggi);
			perguruanTinggi.setWidth("90%");
			perguruanTinggi.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "fakultas");
			Common.selectComboItem(true, fakultas,
					dosen.getFakultas() == null ? tbmuser.ambilFakultas() : dosen.getFakultas());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? fakultas
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(
											dosen.getFakultas() == null ? "" : dosen.getFakultas().getNama())
									: fakultas);
			fakultas.setWidth("90%");
			fakultas.setReadonly(true);

			if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
				Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						"Tanpa Home Base Prodi",
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
			}

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "jurusan");
			Common.selectComboItem(true, jurusan,
					dosen.getJurusan() == null ? tbmuser.ambilJurusan() : dosen.getJurusan());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? jurusan
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(
											dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama())
									: jurusan);
			jurusan.setWidth("90%");
			jurusan.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "sesuaiBidangKeilmuan");
			sesuaiBidangKeilmuan = new MyCheckboxConfig();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? sesuaiBidangKeilmuan
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(dosen.getSesuaiBidangKeilmuan() ? "Ya" : "Yidak")
									: sesuaiBidangKeilmuan);
			sesuaiBidangKeilmuan.setChecked(dosen.getSesuaiBidangKeilmuan());

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "milikUniversitas");
			milikUniversitas = new MyCheckboxConfig();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? milikUniversitas
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(
									dosen.getMilikUniversitas() != null && dosen.getMilikUniversitas() ? "Ya" : "Yidak")
									: milikUniversitas);
			milikUniversitas.setChecked(dosen.getMilikUniversitas() != null && dosen.getMilikUniversitas() == true);
			row.setVisible(tbmuser.ambilDosen() == null);

			row = new MyFormRow();
			row.setVisible(Common.getApakahAdminBolehAksesFeeder());
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode Feeder"));
			row.appendChild(feeder = new Textbox(dosen.getFeeder()));
			feeder.setWidth("90%");

			row = new MyFormRow();
			row.setVisible(Common.getApakahAdmin());

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode ID Reg Ptk"));
			row.appendChild(idRegPtk = new Textbox(dosen.getIdRegPtk()));
			idRegPtk.setWidth("90%");

			return panel;
		}

		public Dosen onSave(Event event) throws Exception {

			if (!email.getValue().trim().isEmpty() && !Common.isValidEmailAddress(email.getValue().trim())) {
				MyMessageboxConfig.show("Email harus diisi dan format email harus benar", "Informasi",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return null;
			}

			if (nama.getValue().trim().equals("")) {
				PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
						"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
						new String[] {
								"Isi/pilih terlebih dahulu Nama.",
								"Ulangi proses penyimpanan setelah kolom tersebut terisi."
						});
				return null;
			}

			if (!code.getValue().trim().equals("")) {

				Integer kotaCount = null;
				Session session = HibernateUtil.currentSession();
				kotaCount = ((Number) session.createCriteria(Dosen.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setProjection(Projections.rowCount()).add(Restrictions.eq("code", code.getValue().trim()))
						.add(this.dosen.getId() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.ne("id", this.dosen.getId()))
						.uniqueResult()).intValue();
				if (kotaCount > 0) {
					MyMessageboxConfig.show(labelCode.getValue() + " dosen tidak boleh sama", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return null;
				}
			}

			if (!mycode.getValue().trim().equals("")) {

				Integer kotaCount = null;
				Session session = HibernateUtil.currentSession();
				kotaCount = ((Number) session.createCriteria(Dosen.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setProjection(Projections.rowCount()).add(Restrictions.eq("mycode", mycode.getValue().trim()))
						.add(this.dosen.getId() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.ne("id", this.dosen.getId()))
						.uniqueResult()).intValue();
				if (kotaCount > 0) {
					MyMessageboxConfig.show(labelMycode.getValue() + " dosen tidak boleh sama", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return null;
				}
			}

			if (!nidn.getValue().trim().equals("")) {

				Integer kotaCount = null;
				Session session = HibernateUtil.currentSession();
				kotaCount = ((Number) session.createCriteria(Dosen.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setProjection(Projections.rowCount()).add(Restrictions.eq("nidn", nidn.getValue().trim()))
						.add(this.dosen.getId() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.ne("id", this.dosen.getId()))
						.uniqueResult()).intValue();
				if (kotaCount > 0) {
					MyMessageboxConfig.show(labelNidn.getValue() + " dosen tidak boleh sama", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return null;
				}
			}

			Integer ol = (Integer) (onlineMenggunakan.getSelectedItem() == null ? null
					: onlineMenggunakan.getSelectedItem().getValue());
			if (ol != null && !ol.equals(Dosen.TIDAK_AKTIF) && onlineLink.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Jika media online aktif, maka link online nya harus diisi secara benar",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return null;
			}

			Session session = HibernateUtil.currentNativeSession();
			if (dosen.getId() != null) {
				dosen = (Dosen) session.load(Dosen.class, dosen.getId());
			}
			dosen.setOnlineMenggunakan(ol);
			dosen.setOnlineLink(onlineLink.getValue().trim());
			dosen.setSesuaiBidangKeilmuan(sesuaiBidangKeilmuan.isChecked());
			dosen.setSertifikasi(sertifikasi.isChecked());
			dosen.setNomorSertifikasi(nomorSertifikasi.getValue());
			dosen.setBahasa((String) bahasa.getSelectedItem().getValue());
			dosen.setPerguruanTinggi((PerguruanTinggi) (perguruanTinggi.getSelectedItem() == null ? null
					: perguruanTinggi.getSelectedItem().getValue()));

			dosen.setGelarBelakang(gelarBelakang.getValue());
			dosen.setGelarDepan(gelarDepan.getValue());

			dosen.setaLisensiKepsek(aLisensiKepsek.isChecked());
			dosen.setJmlSekolahBinaan(jmlSekolahBinaan.getValue());
			dosen.setaDiklatAwas(aDiklatAwas.isChecked());
			dosen.setAktaIjinAjar(aktaIjinAjar.getValue());
			dosen.setNira(nira.getValue());
			dosen.setaBraille(aBraille.isChecked());
			dosen.setaBhsIsyarat(aBhsIsyarat.isChecked());
			dosen.setNpwp(npwp.getValue());
			dosen.setJabatanFungsionalDosen(
					(JabatanFungsionalDosen) (jabatanFungsionalDosen.getSelectedItem() == null ? null
							: jabatanFungsionalDosen.getSelectedItem().getValue()));

			dosen.setIkatanKerjaDosen((IkatanKerjaDosen) (ikatanKerjaDosen.getSelectedItem() == null ? null
					: ikatanKerjaDosen.getSelectedItem().getValue()));

			dosen.setSumberGaji((SumberGaji) (sumberGaji.getSelectedItem() == null ? null
					: sumberGaji.getSelectedItem().getValue()));

			dosen.setStatusKepegawaian((StatusKepegawaian) (statusKepegawaian.getSelectedItem() == null ? null
					: statusKepegawaian.getSelectedItem().getValue()));

			dosen.setJenisPendidikDanTenagaKependidikan(
					(JenisPendidikDanTenagaKependidikan) (jenisPendidikDanTenagaKependidikan.getSelectedItem() == null
							? null
							: jenisPendidikDanTenagaKependidikan.getSelectedItem().getValue()));

			dosen.setLembagaPengangkat((LembagaPengangkat) (lembagaPengangkat.getSelectedItem() == null ? null
					: lembagaPengangkat.getSelectedItem().getValue()));

			dosen.setNuptk(nuptk.getValue());
			dosen.setNiyNigk(niyNigk.getValue());

			dosen.setTmtPns(tmtPns.getValue());

			dosen.setSkAngkat(skAngkat.getValue());
			dosen.setSkCpns(skCpns.getValue());
			dosen.setTglSkCpns(tglSkCpns.getValue());
			dosen.setTmtSkAngkat(tmtSkAngkat.getValue());

			dosen.setGolonganPns((GolonganPns) (golonganPegawai.getSelectedItem() == null ? null
					: golonganPegawai.getSelectedItem().getValue()));
			dosen.setSpesialisasi1(spesialisasi1.getValue());
			dosen.setSpesialisasi2(spesialisasi2.getValue());
			dosen.setSpesialisasi3(spesialisasi3.getValue());

			Dosen atasan = (Dosen) atasanlangsung.getAttribute("dosen");
			dosen.setAtasanlangsung(atasan == null ? null : atasan.getId());

			dosen.setJabatan(jabatan.getValue());
			dosen.setMycode(mycode.getValue());
			dosen.setNidn(nidn.getValue());
			dosen.setCode(code.getValue().trim());
			// dosen.setAlamat(alamat.getValue());
			dosen.setEmail(email.getValue());
			dosen.setKelamin(
					kelamin.getSelectedItem() == null ? null : kelamin.getSelectedItem().getValue().toString());
			dosen.setNama(nama.getValue());
			dosen.setTanggallahir(tanggallahir.getValue());
			dosen.setTelp(telp.getValue());
			dosen.setTempatlahir(tempatlahir.getValue());
			dosen.setPangkat(pangkat.getValue().trim());
			dosen.setSpesifikasiJabatan(
					cbJabatan.getSelectedItem() == null ? null : (Jabatan) cbJabatan.getSelectedItem().getValue());

			dosen.setSpesifikasiJabatanPtLain(cbJabatanPtLain.getSelectedItem() == null ? null
					: (Jabatan) cbJabatanPtLain.getSelectedItem().getValue());

			dosen.setStatusPegawai((StatusPegawai) (statusPegawai.getSelectedItem() == null ? null
					: statusPegawai.getSelectedItem().getValue()));

			dosen.setFakultas(
					(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
							? null
							: fakultas.getSelectedItem().getValue()));
			dosen.setJurusan(
					(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
							: jurusan.getSelectedItem().getValue()));
			dosen.setPendidikan((Pendidikan) (pendidikan.getSelectedItem() == null ? null
					: pendidikan.getSelectedItem().getValue()));

			dosen.setTetap(tetap.isChecked() ? 1 : 0);
			dosen.setMilikUniversitas(milikUniversitas.isChecked() ? true : false);

			dosen.setPendidikans1(pendidikans1.getValue().trim());
			dosen.setPendidikans2(pendidikans2.getValue().trim());
			dosen.setPendidikans3(pendidikans3.getValue().trim());
			dosen.setFeeder(feeder.getValue().trim());
			dosen.setIdRegPtk(idRegPtk.getValue().trim());

			dosen.setRuang((Ruang) ruang.getAttribute("ruang"));
			dosen.setIdfinger(idfinger.getValue().trim());
			dosen.setGoogleScholar(googleScholar.getValue().trim());
			dosen.setKodeSinta(kodeSinta.getValue());

			session.getTransaction().begin();
			if (dosen.getId() != null) {
				session.update(dosen);
			} else {
				session.save(dosen);
			}
			session.getTransaction().commit();

			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser.ambilDosen() != null && tbmuser.getDosen().getId().equals(dosen.getId())) {
				tbmuser.setDosen(dosen);

				Sessions.getCurrent().setAttribute("mytbmuser", tbmuser);
				Sessions.getCurrent().setAttribute("usersTemp", tbmuser);
			}

			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();

			if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
				try {
					session = StreamingHibernateUtil.getInstance().currentSession();

					session.refresh(lainMahasiswa);
					lainMahasiswa.setRef(dosen.getId());

					session.getTransaction().begin();
					session.update(lainMahasiswa);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

			}

			if (fotoDosen != null && fotoDosen.getId() != null) {
				try {
					Session sessionS = StreamingHibernateUtil.getInstance().currentSession();

					sessionS.refresh(fotoDosen);
					fotoDosen.setDosen(dosen.getId());

					sessionS.getTransaction().begin();
					sessionS.update(fotoDosen);
					sessionS.getTransaction().commit();

				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}
				StreamingHibernateUtil.getInstance().closeSession();
			}

			if (ttd != null && ttd.getId() != null) {
				try {
					session = StreamingHibernateUtil.getInstance().currentSession();

					session.refresh(ttd);
					ttd.setRef(dosen.getId());

					session.getTransaction().begin();
					session.update(ttd);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

			}

			try {
				if (dosen != null && dosen.getId() != null && simpanPegawai) {
					session = HibernateUtil.currentNativeSession();
					Pegawai pegawai = (Pegawai) ConstantValues.simpleObject(
							session.createCriteria(Pegawai.class).add(Restrictions.eq("dosen", dosen)).setMaxResults(1),
							Pegawai.class);
					if (pegawai == null) {
						pegawai = new Pegawai();
						pegawai.setDosen(dosen);

						session.getTransaction().begin();
						session.save(pegawai);
						session.getTransaction().commit();

						dosen.setPegawaiId(pegawai.getId());
						session.getTransaction().begin();
						Common.refreshUpdate(session, dosen);
						session.getTransaction().commit();

						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
						FotoDosen fotoDosen = (FotoDosen) streamingSession.createCriteria(FotoDosen.class)
								.add(Restrictions.eq("dosen", dosen.getId())).setMaxResults(1).uniqueResult();
						if (fotoDosen != null) {
							FotoPegawai fotoPegawai = new FotoPegawai();
							fotoPegawai.setNama(fotoDosen.getNama());
							fotoPegawai.setKeterangan(fotoDosen.getKeterangan());
							fotoPegawai.setPegawai(pegawai.getId());
							fotoPegawai.setFoto(fotoDosen.getFoto());

							streamingSession.getTransaction().begin();
							streamingSession.save(fotoPegawai);
							streamingSession.getTransaction().commit();
						}
						StreamingHibernateUtil.getInstance().closeSession();
					}

					// session.disconnect();
					if (session.isOpen()) {session.disconnect();session.close();}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/BiodataDosenAction.java:3001");
			}

			HibernateUtil.closeSession();

			return dosen;
		}

	}

	private static class ManagingBiodataDosen {
		private Textbox alamat;
		private Textbox namaAyah;
		private Combobox pekerjaanAyah;
		private Textbox namaIbu;
		private Combobox pekerjaanIbu;

		private Combobox pernahMenetapDiLuarNegeri;
		private Intbox tinggiBadan;
		private Intbox beratBadan;
		private Textbox teleponRumah;
		private Textbox hp;
		private Textbox suratIzinMengemudi;
		private Textbox kendaraanKuliah;
		private Combobox pernahMemimpinOrganisasi;
		private Textbox namaOrganisasi;
		private Textbox hobi;
		private Textbox minatSeni;
		private Textbox kemampuanBahasa1;
		private Textbox kemampuanBahasa2;
		private Textbox kemampuanBahasa3;
		// private Textbox asalS1;
		// private Textbox alamatAsalS1;
		// private Textbox asalS2;
		// private Textbox alamatAsalS2;
		// private Textbox asalS3;
		// private Textbox alamatAsalS3;
		private Textbox keahlian1;
		private Textbox keahlian2;
		private Textbox keahlian3;
		private Textbox keahlian4;
		private Textbox keahlian5;
		// private AmbilDataNamaSekolahBanbox asalSma;
		// private Textbox alamatAsalSma;
		// private AmbilDataNamaSekolahBanbox asalSmp;
		// private Textbox alamatAsalSmp;
		// private AmbilDataNamaSekolahBanbox asalSd;
		// private Textbox alamatAsalSd;
		private Textbox golonganDarah;
		private Combobox statusNikah;
		private Combobox kewarganegaraan;
		private Combobox agama;

		// private MyButtonConfig foto;

		private BiodataDosen biodataDosen;

		private Dosen dosen;
		private Textbox gelar;

		private Textbox rt;
		private Textbox rw;
		private Textbox kodepos;
		private Textbox kelurahan;
		private AmbilDataKecamatanBanbox kecamatan;
		private Label propinsi;
		private Label kota;

		private Textbox noIdentitas;
		private Textbox dusun;
		private Textbox namaSuamiIstri;
		private Textbox nipSuamiIstri;
		private Combobox pekerjaanSuamiIstri;
		private AmbilDataNegaraBanbox negara;

		private Tabpanel preInit(Dosen dosen) throws Exception {
			this.dosen = dosen;

			pernahMenetapDiLuarNegeri = new Combobox();
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel("Ya");
			comboitem.setValue(1);
			pernahMenetapDiLuarNegeri.appendChild(comboitem);
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Tidak");
			comboitem.setValue(0);
			pernahMenetapDiLuarNegeri.appendChild(comboitem);

			pernahMemimpinOrganisasi = new Combobox();
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Ya");
			comboitem.setValue(1);
			pernahMemimpinOrganisasi.appendChild(comboitem);
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Tidak");
			comboitem.setValue(0);
			pernahMemimpinOrganisasi.appendChild(comboitem);

			statusNikah = new Combobox();
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

			return loadDataDosen();
		}

		public Tabpanel loadDataDosen() throws Exception {
			if (dosen != null && dosen.getId() != null) {
				biodataDosen = dosen.ambilBiodata();
			}

			Tabpanel tabpanelBiodata;
			if (biodataDosen == null || biodataDosen.getId() == null) {
				tabpanelBiodata = initBiodataDosen(new BiodataDosen());
			} else {
				HibernateUtil.currentSession().refresh(biodataDosen);
				tabpanelBiodata = initBiodataDosen(biodataDosen);
			}
			return tabpanelBiodata;
		}

		public Borderlayout initData(final BiodataDosen biodataDosen) throws Exception {
			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setWidth("100%");
			borderlayout.setHeight("100%");
			borderlayout.setStyle("border:0px;");

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");
			// grid.setOddRowSclass("non-odd");

			org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
			columns.setParent(grid);
			MyColumnConfig column = new MyColumnConfig();
			column.setWidth("25%");
			columns.appendChild(column);
			column = new MyColumnConfig();
			column.setWidth("75%");
			columns.appendChild(column);

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
			RevisiHelper.createNewRevisi(BiodataDosen.class, biodataDosen, dosen.getNama()).setParent(row);

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode/NIP"));
			row.appendChild(new ais.ui.util.MyLabelConfig(dosen.getCode()));

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Gelar Akademik / Professional"));
			row.appendChild(gelar = new Textbox(
					biodataDosen.getGelarAkademikProf() == null ? "" : biodataDosen.getGelarAkademikProf()));
			gelar.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Telepon Rumah"));
			row.appendChild(teleponRumah = new Textbox(
					biodataDosen.getTeleponRumah() == null ? "" : biodataDosen.getTeleponRumah()));
			teleponRumah.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("HP"));
			row.appendChild(hp = new Textbox(biodataDosen.getHp() == null ? "" : biodataDosen.getHp()));
			hp.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Surat Ijin Mengemudi"));
			row.appendChild(suratIzinMengemudi = new Textbox(
					biodataDosen.getSuratIzinMengemudi() == null ? "" : biodataDosen.getSuratIzinMengemudi()));
			suratIzinMengemudi.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kendaraan"));
			row.appendChild(kendaraanKuliah = new Textbox(
					biodataDosen.getKendaraanKuliah() == null ? "" : biodataDosen.getKendaraanKuliah()));
			kendaraanKuliah.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pernah Menetap di Luar Negeri"));
			Common.selectComboItem(pernahMenetapDiLuarNegeri, biodataDosen.getPernahMenetapDiLuarNegeri());
			row.appendChild(pernahMenetapDiLuarNegeri);
			pernahMenetapDiLuarNegeri.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pernah Memimpin Organisasi"));
			Common.selectComboItem(pernahMemimpinOrganisasi, biodataDosen.getPernahMemimpinOrganisasi());
			row.appendChild(pernahMemimpinOrganisasi);
			pernahMemimpinOrganisasi.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama Organisasi"));
			row.appendChild(namaOrganisasi = new Textbox(
					biodataDosen.getNamaOrganisasi() == null ? "" : biodataDosen.getNamaOrganisasi()));
			namaOrganisasi.setWidth("90%");
			namaOrganisasi.setMaxlength(49);

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Hobi"));
			row.appendChild(hobi = new Textbox(biodataDosen.getHobi() == null ? "" : biodataDosen.getHobi()));
			hobi.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Minat Seni"));
			row.appendChild(
					minatSeni = new Textbox(biodataDosen.getMinatSeni() == null ? "" : biodataDosen.getMinatSeni()));
			minatSeni.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kemampuan Bahasa 1"));
			row.appendChild(kemampuanBahasa1 = new Textbox(
					biodataDosen.getKemampuanBahasa1() == null ? "" : biodataDosen.getKemampuanBahasa1()));
			kemampuanBahasa1.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kemampuan Bahasa 2"));
			row.appendChild(kemampuanBahasa2 = new Textbox(
					biodataDosen.getKemampuanBahasa2() == null ? "" : biodataDosen.getKemampuanBahasa2()));
			kemampuanBahasa2.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kemampuan Bahasa 3"));
			row.appendChild(kemampuanBahasa3 = new Textbox(
					biodataDosen.getKemampuanBahasa3() == null ? "" : biodataDosen.getKemampuanBahasa3()));
			kemampuanBahasa3.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Keahlian 1"));
			row.appendChild(
					keahlian1 = new Textbox(biodataDosen.getKeahliah1() == null ? "" : biodataDosen.getKeahliah1()));
			keahlian1.setWidth("90%");
			keahlian1.setRows(4);

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Keahlian 2"));
			row.appendChild(
					keahlian2 = new Textbox(biodataDosen.getKeahlian2() == null ? "" : biodataDosen.getKeahlian2()));
			keahlian2.setWidth("90%");
			keahlian2.setRows(4);

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Keahlian 3"));
			row.appendChild(
					keahlian3 = new Textbox(biodataDosen.getKeahlian3() == null ? "" : biodataDosen.getKeahlian3()));
			keahlian3.setWidth("90%");
			keahlian3.setRows(4);

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Keahlian 4"));
			row.appendChild(
					keahlian4 = new Textbox(biodataDosen.getKeahlian4() == null ? "" : biodataDosen.getKeahlian4()));
			keahlian4.setWidth("90%");
			keahlian4.setRows(4);

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Keahlian 5"));
			row.appendChild(
					keahlian5 = new Textbox(biodataDosen.getKeahlian5() == null ? "" : biodataDosen.getKeahlian5()));
			keahlian5.setWidth("90%");
			keahlian5.setRows(4);

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Golongan Darah"));
			row.appendChild(golonganDarah = new Textbox(
					biodataDosen.getGolonganDarah() == null ? "" : biodataDosen.getGolonganDarah()));
			golonganDarah.setWidth("90%");

			kewarganegaraan = new Combobox();
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(ais.database.model.Mahasiswa.WNI);
			comboitem.setValue(ais.database.model.Mahasiswa.WNI);
			kewarganegaraan.appendChild(comboitem);
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(ais.database.model.Mahasiswa.WNA);
			comboitem.setValue(ais.database.model.Mahasiswa.WNA);
			kewarganegaraan.appendChild(comboitem);

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kewarganegaraan"));
			row.appendChild(kewarganegaraan);
			Common.selectComboItem(kewarganegaraan, biodataDosen.getKewarganegaraan());
			kewarganegaraan.setWidth("90%");
			kewarganegaraan.setReadonly(true);

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Asal Negara"));
			row.appendChild(negara = new AmbilDataNegaraBanbox());

			try {
				negara.setAttribute("negara",
						biodataDosen.getNegara() == null ? ConstantValues.INDONESIA : biodataDosen.getNegara());
				negara.setValue((biodataDosen.getNegara() == null ? ConstantValues.INDONESIA : biodataDosen.getNegara())
						.getNamaNegara());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BiodataDosenAction.java:3365");
				// TODO: handle exception
			}

			negara.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Agama"));
			row.appendChild(agama = new Combobox());
			Common.insertCombo(agama, "nama", Agama.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.selectComboItem(agama, biodataDosen.getAgama());
			agama.setWidth("90%");
			agama.setReadonly(true);

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tinggi Badan"));
			row.appendChild(tinggiBadan = new Intbox(biodataDosen.getTinggiBadan()));
			tinggiBadan.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Berat Badan"));
			row.appendChild(beratBadan = new Intbox(biodataDosen.getBeratBadan()));
			beratBadan.setWidth("90%");

			return borderlayout;
		}

		public Borderlayout initKeluarga(final BiodataDosen biodataDosen) throws Exception {
			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setWidth("100%");
			borderlayout.setHeight("100%");
			borderlayout.setStyle("border:0px;");

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
			columns.setParent(grid);
			MyColumnConfig column = new MyColumnConfig();
			column.setWidth("30%");
			columns.appendChild(column);
			column = new MyColumnConfig();
			column.setWidth("70%");
			columns.appendChild(column);

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama Ayah"));
			row.appendChild(
					namaAyah = new Textbox(biodataDosen.getNamaAyah() == null ? "" : biodataDosen.getNamaAyah()));
			namaAyah.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pekerjaan Ayah"));
			row.appendChild(pekerjaanAyah = new Combobox());
			Common.insertCombo(pekerjaanAyah, "nama", PekerjaanOrangTua.class,
					Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
			Common.selectComboItem(pekerjaanAyah, biodataDosen.getPekerjaanAyah());
			pekerjaanAyah.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama Ibu"));
			row.appendChild(namaIbu = new Textbox(biodataDosen.getNamaIbu() == null ? "" : biodataDosen.getNamaIbu()));
			namaIbu.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pekerjaan Ibu"));
			row.appendChild(pekerjaanIbu = new Combobox());
			Common.insertCombo(pekerjaanIbu, "nama", PekerjaanOrangTua.class,
					Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
			Common.selectComboItem(pekerjaanIbu, biodataDosen.getPekerjaanIbu());
			pekerjaanIbu.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Status Nikah *"));
			Common.selectComboItem(statusNikah, biodataDosen.getStatusNikah());
			row.appendChild(statusNikah);
			statusNikah.setWidth("90%");
			statusNikah.setReadonly(true);

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new MyLabelConfig(
					"Nama lengkap suami atau istri PTK tanpa gelar akademik, keagamaan, atau kebangsawanan"));
			row.appendChild(namaSuamiIstri = new Textbox(biodataDosen.getNamaSuamiIstri()));
			namaSuamiIstri.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("NIP suami atau istri (jika PNS)"));
			row.appendChild(nipSuamiIstri = new Textbox(biodataDosen.getNipSuamiIstri()));
			nipSuamiIstri.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pekerjaan Suami / Istri"));
			row.appendChild(pekerjaanSuamiIstri = new Combobox());
			Common.insertCombo(pekerjaanSuamiIstri, "nama", PekerjaanOrangTua.class,
					Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
			Common.selectComboItem(pekerjaanSuamiIstri, biodataDosen.getPekerjaanSuamiIstri());
			pekerjaanAyah.setWidth("90%");

			return borderlayout;
		}

		public Borderlayout initAlamat(final BiodataDosen biodataDosen) throws Exception {
			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setWidth("100%");
			borderlayout.setHeight("100%");
			borderlayout.setStyle("border:0px;");

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
			columns.setParent(grid);
			MyColumnConfig column = new MyColumnConfig();
			column.setWidth("30%");
			columns.appendChild(column);
			column = new MyColumnConfig();
			column.setWidth("70%");
			columns.appendChild(column);

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nomor KTP tanpa tanda baca *"));
			row.appendChild(noIdentitas = new Textbox(dosen.getKtp()));
			noIdentitas.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Alamat *"));
			row.appendChild(alamat = new Textbox(biodataDosen.getAlamat() == null ? "" : biodataDosen.getAlamat()));
			alamat.setWidth("90%");
			alamat.setRows(3);

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Dusun / Kampung *"));
			row.appendChild(dusun = new Textbox(biodataDosen.getDusun()));
			dusun.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("RT *"));
			row.appendChild(rt = new Textbox(biodataDosen.getRt() == null ? "" : biodataDosen.getRt()));
			rt.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("RW *"));
			row.appendChild(rw = new Textbox(biodataDosen.getRw() == null ? "" : biodataDosen.getRw()));
			rw.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode Pos"));
			row.appendChild(kodepos = new Textbox(biodataDosen.getKodepos() == null ? "" : biodataDosen.getKodepos()));
			kodepos.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kelurahan *"));
			row.appendChild(
					kelurahan = new Textbox(biodataDosen.getKelurahan() == null ? "" : biodataDosen.getKelurahan()));
			kelurahan.setWidth("90%");

			propinsi = new Label();
			kota = new Label();

//			Common.insertCombo(propinsi, "nama", Propinsi.class, Restrictions.eq("negara", ConstantValues.INDONESIA));
//			Common.insertCombo(kota, "nama", Kota.class,
//					biodataDosen.getPropinsi() != null ? Restrictions.eq("propinsi", biodataDosen.getPropinsi())
//							: Restrictions.sqlRestriction("1=1"));
//			class PropinsiCalonEventListener implements EventListener {
//
//				@Override
//				public void onEvent(Event event) throws Exception {
//					// TODO Auto-generated method stub
//					Common.clear(kota);
//					kota.setSelectedItem(null);
//					if (propinsi.getSelectedItem() == null) {
//						return;
//					}
//					Common.insertCombo(kota, "nama", Kota.class,
//							Restrictions.eq("propinsi", propinsi.getSelectedItem().getValue()));
//					
//					
//					
//					
//
//				}
//			}
//
//			propinsi.addEventListener("onChange", new PropinsiCalonEventListener());

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kecamatan *"));
			row.appendChild(kecamatan = new AmbilDataKecamatanBanbox());
			kecamatan.setValue(biodataDosen.getKecamatan() == null ? "" : biodataDosen.getKecamatan().getNama());
			kecamatan.setAttribute("wilayah", biodataDosen.getKecamatan());
			kecamatan.setWidth("90%");

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Propinsi *"));
//			Common.selectComboItem(propinsi, biodataDosen.getPropinsi());

			propinsi.setAttribute("wilayah", biodataDosen.getPropinsi());

			row.appendChild(propinsi);
			propinsi.setWidth("90%");

			Common.createFieldKota(rows, "Kabupaten/Kota *", kota, propinsi, biodataDosen.getKota(), true);

			Common.createKotaPropinsiListenerBerdasarkanKecamatan(propinsi, kota, kecamatan);

			return borderlayout;
		}

		private Tabpanel initBiodataDosen(final BiodataDosen biodataDosen) throws Exception {
			this.biodataDosen = biodataDosen;
			Tabpanel panel = new ais.ui.util.MyTabpanel();
			panel.setWidth("100%");
			panel.setHeight("100%");
			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(panel);
			borderlayout.setStyle("border:0px;");

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(center);
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			final MyTabConfig tabData = new MyTabConfig("Biodata");
			tabData.setParent(tabs);

			MyTabConfig tabAlamat = new MyTabConfig("Alamat");
			tabAlamat.setParent(tabs);

			MyTabConfig tabOrangTua = new MyTabConfig("Keluarga");
			tabOrangTua.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanelData = new ais.ui.util.MyTabpanel();
			tabpanelData.appendChild(initData(biodataDosen));
			tabpanelData.setParent(tabpanels);

			tabpanelData = new ais.ui.util.MyTabpanel();
			tabpanelData.appendChild(initAlamat(biodataDosen));
			tabpanelData.setParent(tabpanels);

			tabpanelData = new ais.ui.util.MyTabpanel();
			tabpanelData.appendChild(initKeluarga(biodataDosen));
			tabpanelData.setParent(tabpanels);

			ais.ui.util.MyButtonTabbox.gantiTabboxNative(tabbox, new int[] { 1 });

			return panel;
		}

		public boolean onSave(Event event) throws Exception {
			try {

				BiodataDosenDao biodataDosenDao = DaoFactory.getInstance().getBiodataDosenDao();

				if (biodataDosen.getId() != null) {
					System.out.println("Load");
					biodataDosen = biodataDosenDao.load(biodataDosen.getId());
				}

				biodataDosen.setDosen(dosen);
				biodataDosen.setAlamat(alamat.getValue());
				biodataDosen.setGelarAkademikProf(gelar.getValue());
				biodataDosen.setNoKtp(noIdentitas.getValue());
				biodataDosen.setNamaAyah(namaAyah.getValue());
				biodataDosen.setPekerjaanAyah((PekerjaanOrangTua) (pekerjaanAyah.getSelectedItem() == null ? null
						: pekerjaanAyah.getSelectedItem().getValue()));
				biodataDosen.setNamaIbu(namaIbu.getValue());
				biodataDosen.setPekerjaanIbu((PekerjaanOrangTua) (pekerjaanIbu.getSelectedItem() == null ? null
						: pekerjaanIbu.getSelectedItem().getValue()));
				biodataDosen.setTinggiBadan(tinggiBadan.getValue());
				biodataDosen.setPernahMenetapDiLuarNegeri(
						(Integer) (pernahMenetapDiLuarNegeri.getSelectedItem() == null ? null
								: pernahMenetapDiLuarNegeri.getSelectedItem().getValue()));
				biodataDosen.setBeratBadan(beratBadan.getValue());
				biodataDosen.setTeleponRumah(teleponRumah.getValue());
				biodataDosen.setHp(hp.getValue());
				biodataDosen.setSuratIzinMengemudi(suratIzinMengemudi.getValue());
				biodataDosen.setKendaraanKuliah(kendaraanKuliah.getValue());
				biodataDosen.setPernahMemimpinOrganisasi(
						(Integer) (pernahMemimpinOrganisasi.getSelectedItem() == null ? null
								: pernahMemimpinOrganisasi.getSelectedItem().getValue()));
				biodataDosen.setNamaOrganisasi(namaOrganisasi.getValue());
				biodataDosen.setHobi(hobi.getValue());
				biodataDosen.setMinatSeni(minatSeni.getValue());
				biodataDosen.setKemampuanBahasa1(kemampuanBahasa1.getValue());
				biodataDosen.setKemampuanBahasa2(kemampuanBahasa2.getValue());
				biodataDosen.setKemampuanBahasa3(kemampuanBahasa3.getValue());
				// biodataDosen.setAsalS1(asalS1.getValue());
				// biodataDosen.setAlamatAsalS1(alamatAsalS1.getValue());
				// biodataDosen.setAsalS2(asalS2.getValue());
				// biodataDosen.setAlamatAsalS2(alamatAsalS2.getValue());
				// biodataDosen.setAsalS3(asalS3.getValue());
				// biodataDosen.setAlamatAsalS3(alamatAsalS3.getValue());
				biodataDosen.setKeahliah1(keahlian1.getValue());
				biodataDosen.setKeahlian2(keahlian2.getValue());
				biodataDosen.setKeahlian3(keahlian3.getValue());
				biodataDosen.setKeahlian4(keahlian4.getValue());
				biodataDosen.setKeahlian5(keahlian5.getValue());
				// biodataDosen.setAsalSma(asalSma.getValue());
				// biodataDosen.setAlamatAsalSma(alamatAsalSma.getValue());
				// biodataDosen.setAsalSmp(asalSmp.getValue());
				// biodataDosen.setAlamatAsalSmp(alamatAsalSmp.getValue());
				// biodataDosen.setAsalSd(asalSd.getValue());
				// biodataDosen.setAlamatAsalSd(alamatAsalSd.getValue());
				biodataDosen.setGolonganDarah(golonganDarah.getValue());
				biodataDosen.setStatusNikah((Integer) (statusNikah.getSelectedItem() == null ? null
						: statusNikah.getSelectedItem().getValue()));
				biodataDosen.setKewarganegaraan((String) (kewarganegaraan.getSelectedItem() == null ? null
						: kewarganegaraan.getSelectedItem().getValue()));
				biodataDosen.setNegara((Negara) negara.getAttribute("negara"));
				biodataDosen.setAgama(
						(Agama) (agama.getSelectedItem() == null ? null : agama.getSelectedItem().getValue()));

				biodataDosen.setRt(rt.getValue());
				biodataDosen.setRw(rw.getValue());
				biodataDosen.setKodepos(kodepos.getValue());
				biodataDosen.setKelurahan(kelurahan.getValue());
				biodataDosen.setKecamatan((Wilayah) kecamatan.getAttribute("wilayah"));
				biodataDosen.setPropinsi((Propinsi) (propinsi.getAttribute("wilayah")));
				biodataDosen.setKota((Kota) (kota.getAttribute("wilayah")));
				biodataDosen.setNoIdentitas(noIdentitas.getValue());
				biodataDosen.setDusun(dusun.getValue());
				biodataDosen.setNamaSuamiIstri(namaSuamiIstri.getValue());
				biodataDosen.setNipSuamiIstri(nipSuamiIstri.getValue());
				biodataDosen.setPekerjaanSuamiIstri(
						(PekerjaanOrangTua) (pekerjaanSuamiIstri.getSelectedItem() == null ? null
								: pekerjaanSuamiIstri.getSelectedItem().getValue()));

				if (biodataDosen.getId() != null) {
					biodataDosenDao.update(biodataDosen);
				} else {
					biodataDosenDao.save(biodataDosen);
				}

				dosen.biodataDosen = biodataDosen;

				Session session = HibernateUtil.currentSession();
				session.refresh(dosen);
				dosen.setAlamat(alamat.getValue());
				dosen.setKtp(noIdentitas.getValue());
				dosen.setHp(hp.getValue());

				Common.refreshUpdate(session, dosen);
				session.flush();

				return true;
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				return false;
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	public static void reloadDosen(final Center center, final Dosen dosen, final String ta, final String smt) {

		Tbmuser tbmuser = Common.getCurrentUser();
		boolean upload_SK_oleh_admin = Common.bolehKonfigurasi("upload_SK_oleh_admin", Konfigurasi.TIDAK_AKTIF);

		Common.clear(center);
		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("45px");
		column.setLabel("");
		columns.appendChild(column);
		column = new MyColumnConfig();
		column.setLabel("Tahun Akademik");
		columns.appendChild(column);
		column = new MyColumnConfig();
		column.setWidth("30%");
		column.setLabel("Jenis Semester");
		columns.appendChild(column);

		column = new MyColumnConfig();
		column.setVisible(!upload_SK_oleh_admin || (tbmuser != null && tbmuser.ambilDosen() == null));
		column.setWidth("30%");
		column.setLabel("Upload SK Gabungan");
		columns.appendChild(column);

		if (dosen == null || dosen.getId() == null) {
			return;
		}

		Session session = HibernateUtil.currentSession();
		Object[] objects = dosen.ambilPerkuliahanDanParalel(session, null, null, null, "", "", false, null, true, false,
				false,

				true, true, true, true, true, true,

				true, true, true,

				TampilanELearningAction.PERKULIAHAN,

				0, 10000);
		List<Perkuliahan> perkuliahans = (List<Perkuliahan>) objects[0];
		TreeSet<String> tahunAkademiks = new TreeSet<String>();
		for (Perkuliahan perkuliahan : perkuliahans) {
			String d = perkuliahan.getTahunAjaran() + ";"
					+ (perkuliahan.getStatusSemesterPendek() != null
							&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)
									? "3;" + Perkuliahan.SP
									: (perkuliahan.getGanjilGenap().equals(Perkuliahan.GENAP) ? "2;" + Perkuliahan.GENAP
											: "1;" + Perkuliahan.GANJIL));
			tahunAkademiks.add(d);
		}

		Rows rows = new Rows();
		rows.setParent(grid);

		for (String s : tahunAkademiks) {
			try {
				String[] objs = s.split(";");
				final String tahun = objs[0];
				String jenisSemesterNumber = objs[1];
				final String jenisSemester = objs[2];
				if ((ta == null || (tahun != null && ta.equalsIgnoreCase(tahun)))
						&& (smt == null || (jenisSemester != null && smt.equalsIgnoreCase(jenisSemester)))) {

					final MyDetail detail = new MyDetail();
					detail.addEventListener("onOpen", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Tbmuser tbmuser = Common.getCurrentUser();
							boolean upload_SK_oleh_admin = Common.bolehKonfigurasi("upload_SK_oleh_admin", Konfigurasi.TIDAK_AKTIF);

							Common.clear(detail);
							if (detail.isOpen()) {
								ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
								groupbox.setStyle("min-height: 1200px;");
								groupbox.setParent(detail);
								groupbox.appendChild(new MyCaptionStyled("Penugasan Dosen Mengajar"));
								MyGrid grid = new MyGrid();
								grid.setWidth("100%");
								grid.setParent(groupbox);
								grid.setWidth("100%");
								grid.setHeight("100%");
								grid.setStyle("min-height: 1200px;");
								grid.setSclass("dgrid");

								org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
								columns.setParent(grid);
								MyColumnConfig column = new MyColumnConfig();
								column.setWidth("40px");
								column.setLabel("");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setWidth("15%");
								column.setLabel("Fakultas");
								columns.appendChild(column);
								column = new MyColumnConfig();
								column.setWidth("15%");
								column.setLabel("Jurusan");
								columns.appendChild(column);
								column = new MyColumnConfig();
								column.setWidth("10%");
								column.setLabel("Program");
								columns.appendChild(column);
								column = new MyColumnConfig();
								column.setWidth("10%");
								column.setLabel("SKS");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setWidth("20%");
								column.setLabel("No Surat Tugas");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setWidth("15%");
								column.setLabel("Tgl. Surat Tugas");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setWidth("15%");
								column.setLabel("TMT Surat Tugas");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setVisible(
										!upload_SK_oleh_admin || (tbmuser != null && tbmuser.ambilDosen() == null));
								column.setWidth("10%");
								column.setLabel("Upload SK");
								columns.appendChild(column);

								String sql = "select \n" + "max(c.nama) as fakultas, \n" + "max(b.nama) as jurusan, \n"
										+ "a.program, \n" + "sum(d.sks) as jumlah_sks,\n" + "max(b.id) as jurusanId \n"
										+ "from (\n"
										+ "\tselect max(bb.jurusan) jurusan, max(bb.matakuliah) matakuliah, max(bb.program) as program \n"
										+ "\tfrom perkuliahan bb  \n" + "\twhere (bb.dosen1 = "
										+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen2 = "
										+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen3 = "
										+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen4 = "
										+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen5 = "
										+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen6 = "
										+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen7 = "
										+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen8 = "
										+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen9 = "
										+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen10 = "
										+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + ") \n" + "\tand bb.tahun_ajaran = '"
										+ tahun + "' \n" + "\tand "
										+ (jenisSemester.equals(Perkuliahan.SP)
												? "bb.status_semesterpendek=" + Perkuliahan.SEMESTER_PENDEK
														: " bb.ganjil_genap = '" + jenisSemester.replace("'", "''") + "'")
										+ "\n" + "\tgroup by bb.id\n" + ") a\n"
										+ "left join jurusan b on (a.jurusan = b.id) \n"
										+ "left join fakultas c on (b.fakultas = c.id) \n"
										+ "left join matakuliah d on (a.matakuliah = d.id)   \n"
										+ "group by b.fakultas, a.jurusan, a.program";

								System.out.println("sql => " + sql);

								Session session = HibernateUtil.currentSession();
								List<Object[]> hasils = session.createSQLQuery(sql).list();

								Rows rows = new Rows();
								rows.setParent(grid);
								Integer total = 0;
								for (Object[] objects : hasils) {

									Long idJurusan = ((Number) objects[4]).longValue();

									String program = (String) objects[2];
									Integer sks = ((Number) (objects[3] == null ? 0 : objects[3])).intValue();
									final PenugasanDosenMengajar penugasanDosenMengajar = Common
											.getPenugasanDosenMengajar(idJurusan, program, tahun, jenisSemester, sks,
													dosen);

									if (penugasanDosenMengajar.getDosen() == null) {
										penugasanDosenMengajar.setDosen(dosen);
										Common.refreshUpdate(session, penugasanDosenMengajar);
									}

									MyFormRow row = new MyFormRow();
									row.setValign("top");
									row.setParent(rows);

									final MyDetail detail = new MyDetail();
									detail.setParent(row);
									detail.addEventListener("onOpen", new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											Common.clear(detail);
											if (detail.isOpen()) {

												DosenMengajarHelper dosenMengajarHelper = new DosenMengajarHelper();
												dosenMengajarHelper.display(false, penugasanDosenMengajar, detail);
											}
										}
									});

									Vbox a = RevisiHelper.createNewRevisi(PenugasanDosenMengajar.class,
											penugasanDosenMengajar, objects[0] == null ? "-" : objects[0].toString());
									a.setParent(row);

									Vbox vbox = new Vbox();
									vbox.setParent(a);
									if (penugasanDosenMengajar.getFeeder() != null
											&& !penugasanDosenMengajar.getFeeder().trim().isEmpty()) {
										new MyLabelKecilSekali("Feeder: " + penugasanDosenMengajar.getFeeder())
												.setParent(vbox);
									}

									row.appendChild(new ais.ui.util.MyLabelConfig(
											objects[1] == null ? "" : objects[1].toString()));
									row.appendChild(new ais.ui.util.MyLabelConfig(
											objects[2] == null ? "" : objects[2].toString()));
									row.appendChild(new ais.ui.util.MyLabelConfig(
											Common.numberFormat.get().format(penugasanDosenMengajar.getSks())));

									final Textbox kode = new Textbox(penugasanDosenMengajar.getKode());
									kode.setWidth("90%");
									row.appendChild(kode);

									final MyDatebox tanggalSuratTugas = new MyDatebox(
											penugasanDosenMengajar.getTanggalSuratTugas());
									tanggalSuratTugas.setWidth("90%");
									row.appendChild(tanggalSuratTugas);

									final MyDatebox tmtSuratTugas = new MyDatebox(
											penugasanDosenMengajar.getTmtSuratTugas());
									tmtSuratTugas.setWidth("90%");
									row.appendChild(tmtSuratTugas);

									final PenugasanDosenMengajar tempPenugasanDosenMengajar = penugasanDosenMengajar;

									Hbox hbox = new Hbox();
									hbox.setVisible(
											!upload_SK_oleh_admin || (tbmuser != null && tbmuser.ambilDosen() == null));
									LampiranLain.createDownloadUploadFileLain(hbox, penugasanDosenMengajar.getId(),
											"sk_penugasan_pengajaran_dosen", "SK", false, new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {

												}
											}, null, false, false, false, true);
									hbox.setParent(row);

									EventListener eventListener = new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											tempPenugasanDosenMengajar.setKode(kode.getValue());
											tempPenugasanDosenMengajar.setTmtSuratTugas(tmtSuratTugas.getValue());
											tempPenugasanDosenMengajar
													.setTanggalSuratTugas(tanggalSuratTugas.getValue());
											tempPenugasanDosenMengajar.setDosen(dosen);
											Common.refreshUpdate(tempPenugasanDosenMengajar);
										}
									};

									kode.addEventListener("onChange", eventListener);
									tanggalSuratTugas.addEventListener("onChange", eventListener);
									tmtSuratTugas.addEventListener("onChange", eventListener);

									total += sks;
								}

								MyFormRow row = new MyFormRow();
								row.setValign("top");
								row.setStyle("background-color: rgba(224, 224, 235,0.4);");
								row.setParent(rows);
								row.appendChild(new Label());
								row.appendChild(new Label());
								row.appendChild(new Label());
								row.appendChild(new MyLabelBoldConfig("Total SKS"));
								row.appendChild(new MyLabelBold(Common.numberFormat.get().format(total)));
								row.appendChild(new Label());
								row.appendChild(new Label());
								row.appendChild(new Label());
								row.appendChild(new Label());

							}

						}

					});
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					detail.setParent(row);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(tahun));
					row.appendChild(new ais.ui.util.MyLabelConfig(jenisSemester));

					String kode = Common.maxPanjangAkhir("000000000000000000000" + dosen.getId() + "00"
							+ StringUtils.split(tahun, "/")[0] + jenisSemesterNumber, 14);
					kode = "1" + kode;
					Long l = Long.parseLong(kode);

					System.out.println("l -> " + l);

					Vbox myVbox = new Vbox();
					myVbox.setParent(row);
					Hbox hbox = new Hbox();
					hbox.setVisible(!upload_SK_oleh_admin || (tbmuser != null && tbmuser.ambilDosen() == null));
					LampiranLain.createDownloadUploadFileLain(hbox, l, "sk_penugasan_pengajaran_dosen_gabungan",
							"SK Gabungan", false, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

								}
							}, null, false, false, false, true);
					hbox.setParent(myVbox);

					if (Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)) {

						// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
						final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
								new java.util.ArrayList<org.zkoss.zk.ui.Component>();

						final MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Ekspor",
								"/img/corner.gif");
						exportKeOjs.setStyle("font-size:9px;");
						aksiButtons.add(exportKeOjs);
						exportKeOjs.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
								&& Common.bolehKonfigurasi("biodata_dosen_terhubung_ke_dspace"));

						exportKeOjs.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								final Label label = Common.displayLoadBar(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										reloadDosen(center, dosen, ta, smt);
									}
								});

								new Thread(new Runnable() {

									@Override
									public void run() {
										try {
											String cookie = DspaceCommon.login();
											String sql = "select \n" + "max(c.nama) as fakultas, \n"
													+ "max(b.nama) as jurusan, \n" + "a.program, \n"
													+ "sum(d.sks) as jumlah_sks,\n" + "max(b.id) as jurusanId \n"
													+ "from (\n"
													+ "\tselect max(bb.jurusan) jurusan, max(bb.matakuliah) matakuliah, max(bb.program) as program \n"
													+ "\tfrom perkuliahan bb  \n" + "\twhere (bb.dosen1 = "
													+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen2 = "
													+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen3 = "
													+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen4 = "
													+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen5 = "
													+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen6 = "
													+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen7 = "
													+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen8 = "
													+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen9 = "
													+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen10 = "
													+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + ") \n"
													+ "\tand bb.tahun_ajaran = '" + tahun + "' \n" + "\tand "
													+ (jenisSemester.equals(Perkuliahan.SP)
															? "bb.status_semesterpendek=" + Perkuliahan.SEMESTER_PENDEK
																	: " bb.ganjil_genap = '" + jenisSemester.replace("'", "''") + "'")
													+ "\n" + "\tgroup by bb.id\n" + ") a\n"
													+ "left join jurusan b on (a.jurusan = b.id) \n"
													+ "left join fakultas c on (b.fakultas = c.id) \n"
													+ "left join matakuliah d on (a.matakuliah = d.id)   \n"
													+ "group by b.fakultas, a.jurusan, a.program";

											System.out.println("sql => " + sql);

											Session session = HibernateUtil.currentSession();
											List<Object[]> hasils = session.createSQLQuery(sql).list();

											int rowIndex = 1;
											for (Object[] objects : hasils) {

												Long idJurusan = ((Number) objects[4]).longValue();

												String program = (String) objects[2];
												Integer sks = ((Number) (objects[3] == null ? 0 : objects[3]))
														.intValue();
												PenugasanDosenMengajar penugasanDosenMengajar = Common
														.getPenugasanDosenMengajar(idJurusan, program, tahun,
																jenisSemester, sks, dosen);

												if (penugasanDosenMengajar.getDosen() == null) {
													penugasanDosenMengajar.setDosen(dosen);
													Common.refreshUpdate(session, penugasanDosenMengajar);
												}
												label.setValue("Sedang memproses data "
														+ penugasanDosenMengajar.toString() + " (" + Common.numberFormat.get()
																.format((rowIndex++) * 100.0 / hasils.size())
														+ " %)");
												getDspaceTugasMengajar(cookie, penugasanDosenMengajar, true);
											}
										} catch (Exception e) {
											// TODO Auto-generated catch block
											Common.tampilErrorJikaAdmin(e);
										}
										label.setValue("");
									}
								}).start();
							}
						});

						MyToolbarbuttonConfig batalExport = new MyToolbarbuttonConfig("Batalkan Ekspor",
								"/img/svg/trash.svg");
						aksiButtons.add(batalExport);
						batalExport.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
								&& Common.bolehKonfigurasi("biodata_dosen_terhubung_ke_dspace"));
						batalExport.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								MyMessageboxConfig.show("Apakah yakin ingin membatalkan ekspor data ini ?",
										"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
										MyMessageboxConfig.QUESTION, new EventListener() {

											@Override
											public void onEvent(Event event) throws Exception {
												int i = Integer.parseInt(event.getData().toString());
												if (i == MyMessageboxConfig.OK) {

													final Label label = Common.displayLoadBar(new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															reloadDosen(center, dosen, ta, smt);
															LogLoginAction.tampilDpsaceLog();
														}
													});

													new Thread(new Runnable() {

														@Override
														public void run() {
															try {
															try {
																String cookie = DspaceCommon.login();
																String sql = "select \n" + "max(c.nama) as fakultas, \n"
																		+ "max(b.nama) as jurusan, \n" + "a.program, \n"
																		+ "sum(d.sks) as jumlah_sks,\n"
																		+ "max(b.id) as jurusanId \n" + "from (\n"
																		+ "\tselect max(bb.jurusan) jurusan, max(bb.matakuliah) matakuliah, max(bb.program) as program \n"
																		+ "\tfrom perkuliahan bb  \n"
																		+ "\twhere (bb.dosen1 = "
																		+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
																		+ " or bb.dosen2 = "
																		+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
																		+ " or bb.dosen3 = "
																		+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
																		+ " or bb.dosen4 = "
																		+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
																		+ " or bb.dosen5 = "
																		+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
																		+ " or bb.dosen6 = "
																		+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
																		+ " or bb.dosen7 = "
																		+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
																		+ " or bb.dosen8 = "
																		+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
																		+ " or bb.dosen9 = "
																		+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
																		+ " or bb.dosen10 = "
																		+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + ") \n"
																		+ "\tand bb.tahun_ajaran = '" + tahun + "' \n"
																		+ "\tand "
																		+ (jenisSemester.equals(Perkuliahan.SP)
																				? "bb.status_semesterpendek="
																						+ Perkuliahan.SEMESTER_PENDEK
																						: " bb.ganjil_genap = '" + jenisSemester.replace("'", "''") + "'")
																		+ "\n" + "\tgroup by bb.id\n" + ") a\n"
																		+ "left join jurusan b on (a.jurusan = b.id) \n"
																		+ "left join fakultas c on (b.fakultas = c.id) \n"
																		+ "left join matakuliah d on (a.matakuliah = d.id)   \n"
																		+ "group by b.fakultas, a.jurusan, a.program";

																System.out.println("sql => " + sql);

																Session session = HibernateUtil.currentSession();
																List<Object[]> hasils = session.createSQLQuery(sql)
																		.list();

																int rowIndex = 1;
																for (Object[] objects : hasils) {

																	Long idJurusan = ((Number) objects[4]).longValue();

																	String program = (String) objects[2];
																	Integer sks = ((Number) (objects[3] == null ? 0
																			: objects[3])).intValue();
																	PenugasanDosenMengajar penugasanDosenMengajar = Common
																			.getPenugasanDosenMengajar(idJurusan,
																					program, tahun, jenisSemester, sks,
																					dosen);

																	if (penugasanDosenMengajar.getDosen() == null) {
																		penugasanDosenMengajar.setDosen(dosen);
																		Common.refreshUpdate(session,
																				penugasanDosenMengajar);
																	}
																	label.setValue("Sedang memproses data "
																			+ penugasanDosenMengajar.toString() + " ("
																			+ Common.numberFormat.get().format((rowIndex++)
																					* 100.0 / hasils.size())
																			+ " %)");
																	DspaceInformation dspaceInformation = DspaceInformation
																			.getDspaceInformation(
																					PenugasanDosenMengajar.class
																							.getName(),
																					penugasanDosenMengajar.getId());
																	if (dspaceInformation != null) {
																		int i = DspaceInformation.delete(cookie,
																				"items/" + dspaceInformation.getUuid(),
																				dspaceInformation.getPostInfo());
																		if (i == 200) {

																			session = HibernateUtil
																					.currentNativeSession();
																			session.getTransaction().begin();
																			session.delete(dspaceInformation);
																			session.getTransaction().commit();
																			HibernateUtil.closeSession();
																		}
																	}
																}
															} catch (Exception e) {
																// TODO Auto-generated catch
																// block
																Common.tampilErrorJikaAdmin(e);
															}
															label.setValue("");
																													} finally {
																ais.database.hibernate.HibernateUtil.closeSession();
															}
														}
													}).start();

												}

											}
										});
							}
						});

						ais.ui.util.UIHelper.buatBarisAksi(myVbox, 3, aksiButtons);

					}

				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BiodataDosenAction.java:4354");
				// TODO: handle exception
			}
		}

	}

	@SuppressWarnings({ "unchecked" })
	public static void reloadDosenBimbingan(final Center center, final Dosen dosen, final String ta, final String smt) {

		Common.clear(center);
		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("45px");
		column.setLabel("");
		columns.appendChild(column);
		column = new MyColumnConfig();
		column.setLabel("Tahun Akademik");
		columns.appendChild(column);
		column = new MyColumnConfig();
		column.setWidth("30%");
		column.setLabel("Jenis Semester");
		columns.appendChild(column);

		if (dosen == null || dosen.getId() == null) {
			return;
		}

		Session session = HibernateUtil.currentSession();
		Object[] objects = dosen.ambilPerkuliahanDanParalel(session, null, null, null, "", "", false, null, true, false,
				false,

				true, true, true, true, true, true,

				true, true, true,

				TampilanELearningAction.BIMBINGAN,

				0, 10000);
		List<MahasiswaRequestTugasAkhir> mahasiswaRequestTugasAkhirs = (List<MahasiswaRequestTugasAkhir>) objects[0];
		TreeSet<String> tahunAkademiks = new TreeSet<String>();
		for (MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir : mahasiswaRequestTugasAkhirs) {
			String d = mahasiswaRequestTugasAkhir.getTahunAkademik() + ";"
					+ (mahasiswaRequestTugasAkhir.getSemester() % 2 == 0 ? "2;" + Perkuliahan.GENAP
							: "1;" + Perkuliahan.GANJIL);
			tahunAkademiks.add(d);
		}

		Rows rows = new Rows();
		rows.setParent(grid);

		for (String s : tahunAkademiks) {
			try {
				String[] objs = s.split(";");
				final String tahun = objs[0];
				final String jenisSemester = objs[2];
				if ((ta == null || (tahun != null && ta.equalsIgnoreCase(tahun)))
						&& (smt == null || (jenisSemester != null && smt.equalsIgnoreCase(jenisSemester)))) {

					final MyDetail detail = new MyDetail();
					detail.addEventListener("onOpen", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Common.clear(detail);
							if (detail.isOpen()) {
								ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
								groupbox.setStyle("min-height: 1200px;");
								groupbox.setParent(detail);
								groupbox.appendChild(new MyCaptionStyled("Penugasan Dosen Membimbing"));
								MyGrid grid = new MyGrid();
								grid.setWidth("100%");
								grid.setParent(groupbox);
								grid.setWidth("100%");
								grid.setHeight("100%");
								grid.setStyle("min-height: 1200px;");
								grid.setSclass("dgrid");

								org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
								columns.setParent(grid);
								MyColumnConfig column = new MyColumnConfig();
								column.setWidth("40px");
								column.setLabel("");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setLabel("Fakultas");
								columns.appendChild(column);
								column = new MyColumnConfig();
								column.setLabel("Jurusan");
								columns.appendChild(column);
								column = new MyColumnConfig();
								column.setLabel("Program");
								columns.appendChild(column);
								column = new MyColumnConfig();
								column.setWidth("10%");
								column.setLabel("Jumlah");
								columns.appendChild(column);

								String sql = "select \n" + "max(c.nama) as fakultas, \n" + "max(b.nama) as jurusan, \n"
										+ "a.program, \n" + "count(*) as jumlah_sks,\n" + "max(b.id) as jurusanId \n"
										+ "from (\n" + "\tselect max(b1.jurusan) jurusan, max(b1.program) as program \n"
										+ " from mahasiswa_request_tugas_akhir bb "
										+ " left join mahasiswa b1 on (bb.mahasiswa = b1.id)" + " where (bb.dosen1 = "
										+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen2 = "
										+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen3 = "
										+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen4 = "
										+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen5 = "
										+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " or bb.dosen6 = "
										+ (dosen == null || dosen.getId() == null ? -1L : dosen.getId()) + " )  and bb.tahun_akademik = '"
										+ tahun + "'  and  bb.semester % 2 = "
										+ (jenisSemester.equals(Perkuliahan.GENAP) ? "0" : "1") + "\n"
										+ " group by bb.id ) a\n" + "left join jurusan b on (a.jurusan = b.id) \n"
										+ "left join fakultas c on (b.fakultas = c.id) \n"
										+ "group by b.fakultas, a.jurusan, a.program";

								System.out.println("sql => " + sql);

								Session session = HibernateUtil.currentSession();
								List<Object[]> hasils = session.createSQLQuery(sql).list();

								Rows rows = new Rows();
								rows.setParent(grid);
								Integer total = 0;
								for (Object[] objects : hasils) {

									String program = (String) objects[2];
									Integer jumlah = ((Number) (objects[3] == null ? 0 : objects[3])).intValue();

									MyFormRow row = new MyFormRow();
									row.setValign("top");
									row.setParent(rows);

									final MyDetail detail = new MyDetail();
									detail.setParent(row);
									detail.addEventListener("onOpen", new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											Common.clear(detail);
											if (detail.isOpen()) {

												Session session = HibernateUtil.currentSession();
												Object[] objects = dosen.ambilPerkuliahanDanParalel(session, tahun,
														jenisSemester, null, "", "", false, null, true, false, false,

														true, true, true, true, true, true,

														true, true, true,

														TampilanELearningAction.BIMBINGAN,

														0, 10000);

												List<MahasiswaRequestTugasAkhir> mahasiswaRequestTugasAkhirs = (List<MahasiswaRequestTugasAkhir>) objects[0];

												ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
												groupbox.setStyle("min-height: 1200px;");
												groupbox.setParent(detail);
												MyGrid grid = new MyGrid();
												grid.setWidth("100%");
												grid.setParent(groupbox);
												grid.setWidth("100%");
												grid.setHeight("100%");
												grid.setStyle("min-height: 1200px;");
												grid.setSclass("dgrid");

												org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
												columns.setParent(grid);
												MyColumnConfig column = new MyColumnConfig();
												column.setWidth("40px");
												column.setLabel("");
												columns.appendChild(column);

												column = new MyColumnConfig();
												column.setLabel("Mahasiswa");
												column.setWidth("30%");
												columns.appendChild(column);
												column = new MyColumnConfig();
												column.setLabel("Judul / Pembimbing");
												columns.appendChild(column);

												Rows rows = new Rows();
												rows.setParent(grid);
												for (final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir : mahasiswaRequestTugasAkhirs) {
													MyFormRow row = new MyFormRow();
													row.setValign("top");
													row.setParent(rows);

													final MyDetail detail = new MyDetail();
													detail.setParent(row);
													detail.addEventListener("onOpen", new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															Common.clear(detail);
															if (detail.isOpen()) {
																ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
																groupbox.setStyle("min-height: 200px;");
																aktifitasTugasAkhirHelper.initDetail(
																		mahasiswaRequestTugasAkhir, groupbox);
																detail.appendChild(groupbox);
															}
														}
													});

													MahasiswaRequestTugasAkhirAction
															.tampilkanInfoMahasiswa(mahasiswaRequestTugasAkhir, null)
															.setParent(row);
													MahasiswaRequestTugasAkhirAction
															.tampilkanInfoDosen(mahasiswaRequestTugasAkhir, true)
															.setParent(row);

												}
											}
										}
									});

									row.appendChild(new Label(objects[0] == null ? "-" : objects[0].toString()));
									row.appendChild(new Label(objects[1] == null ? "" : objects[1].toString()));
									row.appendChild(new Label(program));
									row.appendChild(new Label(Common.numberFormat.get().format(jumlah)));

									total += jumlah;
								}

								MyFormRow row = new MyFormRow();
								row.setValign("top");
								row.setStyle("background-color: rgba(224, 224, 235,0.4);");
								row.setParent(rows);
								row.appendChild(new Label());
								row.appendChild(new Label());
								row.appendChild(new Label());
								row.appendChild(new MyLabelBoldConfig("Total Bimbingan"));
								row.appendChild(new MyLabelBold(Common.numberFormat.get().format(total)));

							}

						}

					});
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					detail.setParent(row);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(tahun));
					row.appendChild(new ais.ui.util.MyLabelConfig(jenisSemester));

				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BiodataDosenAction.java:4613");
				// TODO: handle exception
			}
		}

	}

	@SuppressWarnings({ "unchecked" })
	public static void reloadDosenPenguji(final Center center, final Dosen dosen, final String ta, final String smt) {

		Common.clear(center);
		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("45px");
		column.setLabel("");
		columns.appendChild(column);
		column = new MyColumnConfig();
		column.setLabel("Tahun Akademik");
		columns.appendChild(column);
		column = new MyColumnConfig();
		column.setWidth("30%");
		column.setLabel("Jenis Semester");
		columns.appendChild(column);

		if (dosen == null || dosen.getId() == null) {
			return;
		}

		Session session = HibernateUtil.currentSession();
		Object[] objects = dosen.ambilPerkuliahanDanParalel(session, null, null, null, "", "", false, null, true, false,
				false,

				true, true, true, true, true, true,

				true, true, true,

				TampilanELearningAction.SKRIPSI,

				0, 10000);
		List<Skripsi> skripsis = (List<Skripsi>) objects[0];
		TreeSet<String> tahunAkademiks = new TreeSet<String>();
		for (Skripsi skripsi : skripsis) {
			String d = skripsi.getTahunAkademik() + ";"
					+ (skripsi.getSemester() % 2 == 0 ? "2;" + Perkuliahan.GENAP : "1;" + Perkuliahan.GANJIL);
			tahunAkademiks.add(d);
		}

		Rows rows = new Rows();
		rows.setParent(grid);

		for (String s : tahunAkademiks) {
			try {
				String[] objs = s.split(";");
				final String tahun = objs[0];
				final String jenisSemester = objs[2];
				if ((ta == null || (tahun != null && ta.equalsIgnoreCase(tahun)))
						&& (smt == null || (jenisSemester != null && smt.equalsIgnoreCase(jenisSemester)))) {

					final MyDetail detail = new MyDetail();
					detail.addEventListener("onOpen", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Common.clear(detail);
							if (detail.isOpen()) {
								ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
								groupbox.setStyle("min-height: 1200px;");
								groupbox.setParent(detail);
								groupbox.appendChild(new MyCaptionStyled("Penugasan Dosen Menguji"));
								MyGrid grid = new MyGrid();
								grid.setWidth("100%");
								grid.setParent(groupbox);
								grid.setWidth("100%");
								grid.setHeight("100%");
								grid.setStyle("min-height: 1200px;");
								grid.setSclass("dgrid");

								org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
								columns.setParent(grid);
								MyColumnConfig column = new MyColumnConfig();
								column.setWidth("40px");
								column.setLabel("");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setLabel("Fakultas");
								columns.appendChild(column);
								column = new MyColumnConfig();
								column.setLabel("Jurusan");
								columns.appendChild(column);
								column = new MyColumnConfig();
								column.setLabel("Program");
								columns.appendChild(column);
								column = new MyColumnConfig();
								column.setWidth("10%");
								column.setLabel("Jumlah");
								columns.appendChild(column);

								String sql = "select \n" + "max(c.nama) as fakultas, \n" + "max(b.nama) as jurusan, \n"
										+ "a.program, \n" + "count(*) as jumlah_sks,\n" + "max(b.id) as jurusanId \n"
										+ "from (\n" + "\tselect max(b1.jurusan) jurusan, max(b1.program) as program \n"
										+ " from skripsi bb " + " left join mahasiswa b1 on (bb.mahasiswa = b1.id)"
										+ " where (bb.dosen1 = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
										+ " or bb.penguji1 = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
										+ " or bb.pembimbing = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
										+ " or bb.ketua_sidang = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
										+ " or bb.penguji2 = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
										+ " or bb.penguji3 = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
										+ " or bb.penguji4 = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
										+ " or bb.penguji5 = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
										+ " or bb.pembimbing3 = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
										+ " )  and bb.tahun_akademik = '" + tahun + "'  and  bb.semester % 2 = "
										+ (jenisSemester.equals(Perkuliahan.GENAP) ? "0" : "1") + "\n"
										+ " group by bb.id ) a\n" + "left join jurusan b on (a.jurusan = b.id) \n"
										+ "left join fakultas c on (b.fakultas = c.id) \n"
										+ "group by b.fakultas, a.jurusan, a.program";

								System.out.println("sql => " + sql);

								Session session = HibernateUtil.currentSession();
								List<Object[]> hasils = session.createSQLQuery(sql).list();

								Rows rows = new Rows();
								rows.setParent(grid);
								Integer total = 0;
								for (Object[] objects : hasils) {

									String program = (String) objects[2];
									Integer jumlah = ((Number) (objects[3] == null ? 0 : objects[3])).intValue();

									MyFormRow row = new MyFormRow();
									row.setValign("top");
									row.setParent(rows);

									final MyDetail detail = new MyDetail();
									detail.setParent(row);
									detail.addEventListener("onOpen", new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											Common.clear(detail);
											if (detail.isOpen()) {

												Session session = HibernateUtil.currentSession();
												Object[] objects = dosen.ambilPerkuliahanDanParalel(session, tahun,
														jenisSemester, null, "", "", false, null, true, false, false,

														true, true, true, true, true, true,

														true, true, true,

														TampilanELearningAction.SKRIPSI,

														0, 10000);

												List<Skripsi> skripsis = (List<Skripsi>) objects[0];

												ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
												groupbox.setStyle("min-height: 1200px;");
												groupbox.setParent(detail);
												MyGrid grid = new MyGrid();
												grid.setWidth("100%");
												grid.setParent(groupbox);
												grid.setWidth("100%");
												grid.setHeight("100%");
												grid.setStyle("min-height: 1200px;");
												grid.setSclass("dgrid");

												org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
												columns.setParent(grid);
												MyColumnConfig column = new MyColumnConfig();
												column.setWidth("40px");
												column.setLabel("");
												columns.appendChild(column);

												column = new MyColumnConfig();
												column.setLabel("Mahasiswa");
												column.setWidth("30%");
												columns.appendChild(column);
												column = new MyColumnConfig();
												column.setLabel("Judul / Penguji");
												columns.appendChild(column);

												Rows rows = new Rows();
												rows.setParent(grid);
												for (final Skripsi skripsi : skripsis) {
													MyFormRow row = new MyFormRow();
													row.setValign("top");
													row.setParent(rows);

													final MyDetail detail = new MyDetail();
													detail.setParent(row);
													detail.addEventListener("onOpen", new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															Common.clear(detail);
															if (detail.isOpen()) {
																ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
																groupbox.setStyle("min-height: 200px;");
																aktifitasSkripsiHelper.initDetail(skripsi, groupbox);
																detail.appendChild(groupbox);
															}
														}
													});

													SkripsiAction.tampilkanInfoMahasiswa(skripsi, null).setParent(row);
													SkripsiAction.tampilkanInfoDosen(skripsi, true, true)
															.setParent(row);

												}
											}
										}
									});

									row.appendChild(new Label(objects[0] == null ? "-" : objects[0].toString()));
									row.appendChild(new Label(objects[1] == null ? "" : objects[1].toString()));
									row.appendChild(new Label(program));
									row.appendChild(new Label(Common.numberFormat.get().format(jumlah)));

									total += jumlah;
								}

								MyFormRow row = new MyFormRow();
								row.setValign("top");
								row.setStyle("background-color: rgba(224, 224, 235,0.4);");
								row.setParent(rows);
								row.appendChild(new Label());
								row.appendChild(new Label());
								row.appendChild(new Label());
								row.appendChild(new MyLabelBoldConfig("Total Bimbingan"));
								row.appendChild(new MyLabelBold(Common.numberFormat.get().format(total)));

							}

						}

					});
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					detail.setParent(row);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(tahun));
					row.appendChild(new ais.ui.util.MyLabelConfig(jenisSemester));

				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BiodataDosenAction.java:4869");
				// TODO: handle exception
			}
		}

	}

	@SuppressWarnings({ "unchecked" })
	public static void reloadDosenKkn(final Center center, final Dosen dosen, final String ta, final String smt) {

		Common.clear(center);
		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("45px");
		column.setLabel("");
		columns.appendChild(column);
		column = new MyColumnConfig();
		column.setLabel("Tahun Akademik");
		columns.appendChild(column);
		column = new MyColumnConfig();
		column.setWidth("30%");
		column.setLabel("Jenis Semester");
		columns.appendChild(column);

		if (dosen == null || dosen.getId() == null) {
			return;
		}

		Session session = HibernateUtil.currentSession();
		Object[] objects = dosen.ambilPerkuliahanDanParalel(session, null, null, null, "", "", false, null, true, false,
				false,

				true, true, true, true, true, true,

				true, true, true,

				TampilanELearningAction.KKN,

				0, 10000);
		List<KelompokKkn> kelompokKkns = (List<KelompokKkn>) objects[0];
		TreeSet<String> tahunAkademiks = new TreeSet<String>();
		for (KelompokKkn kelompokKkn : kelompokKkns) {
			String d = kelompokKkn.getKkn().getTahunAkademik() + ";"
					+ (kelompokKkn.getKkn().getSemester().equalsIgnoreCase(Perkuliahan.GENAP) ? "2;" + Perkuliahan.GENAP
							: "1;" + Perkuliahan.GANJIL);
			tahunAkademiks.add(d);
		}

		Rows rows = new Rows();
		rows.setParent(grid);

		for (String s : tahunAkademiks) {
			try {
				String[] objs = s.split(";");
				final String tahun = objs[0];
				final String jenisSemester = objs[2];
				if ((ta == null || (tahun != null && ta.equalsIgnoreCase(tahun)))
						&& (smt == null || (jenisSemester != null && smt.equalsIgnoreCase(jenisSemester)))) {

					final MyDetail detail = new MyDetail();
					detail.addEventListener("onOpen", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Common.clear(detail);
							if (detail.isOpen()) {
								ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
								groupbox.setStyle("min-height: 1200px;");
								groupbox.setParent(detail);
								groupbox.appendChild(new MyCaptionStyled("Penugasan Dosen Pembimbing KKN"));
								MyGrid grid = new MyGrid();
								grid.setWidth("100%");
								grid.setParent(groupbox);
								grid.setWidth("100%");
								grid.setHeight("100%");
								grid.setStyle("min-height: 1200px;");
								grid.setSclass("dgrid");

								org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
								columns.setParent(grid);
								MyColumnConfig column = new MyColumnConfig();
								column.setWidth("40px");
								column.setLabel("");
								columns.appendChild(column);

								column = new MyColumnConfig();
								column.setLabel("Fakultas");
								columns.appendChild(column);
								column = new MyColumnConfig();
								column.setLabel("Jurusan");
								columns.appendChild(column);
								column = new MyColumnConfig();
								column.setLabel("Program");
								columns.appendChild(column);
								column = new MyColumnConfig();
								column.setWidth("10%");
								column.setLabel("Jumlah Mhs");
								columns.appendChild(column);

								String sql = "select \n" + "max(c.nama) as fakultas, \n" + "max(b.nama) as jurusan, \n"
										+ "a.program, \n" + "count(*) as jumlah_sks,\n" + "max(b.id) as jurusanId \n"
										+ "from (\n" + "\tselect max(b1.jurusan) jurusan, max(b1.program) as program \n"
										+ " from kelompok_kkn bb inner join mahasiswa_dapat_kelompok_kelompok_kkn cc (bb.id = cc.kelompok_kkn) inner join kkn dd (bb.kkn = dd.id) left join mahasiswa b1 on (cc.mahasiswa = b1.id)"
										+ " where (bb.dosen_pembimbing1 = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
										+ " or bb.dosen_pembimbing2 = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
										+ " or bb.dosen_pembimbing3 = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
										+ " or bb.dosen_pembimbing4 = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
										+ " or bb.dosen_pembimbing5 = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
										+ " or bb.dosen_pembimbing6 = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
										+ " or bb.dosen_pembimbing7 = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
										+ " or bb.dosen_pembimbing8 = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
										+ " or bb.dosen_pembimbing9 = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
										+ " or bb.dosen_pembimbing10 = " + (dosen == null || dosen.getId() == null ? -1L : dosen.getId())
										+ " )  and dd.tahunakademik = '" + tahun + "'  and  bb.semester % 2 = '"
										+ jenisSemester + "' \n" + " group by bb.id ) a\n"
										+ "left join jurusan b on (a.jurusan = b.id) \n"
										+ "left join fakultas c on (b.fakultas = c.id) \n"
										+ "group by b.fakultas, a.jurusan, a.program";

								System.out.println("sql => " + sql);

								Session session = HibernateUtil.currentSession();
								List<Object[]> hasils = session.createSQLQuery(sql).list();

								Rows rows = new Rows();
								rows.setParent(grid);
								Integer total = 0;
								for (Object[] objects : hasils) {

									String program = (String) objects[2];
									Integer jumlah = ((Number) (objects[3] == null ? 0 : objects[3])).intValue();

									MyFormRow row = new MyFormRow();
									row.setValign("top");
									row.setParent(rows);

									final MyDetail detail = new MyDetail();
									detail.setParent(row);
									detail.addEventListener("onOpen", new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											Common.clear(detail);
											if (detail.isOpen()) {

												Session session = HibernateUtil.currentSession();
												Object[] objects = dosen.ambilPerkuliahanDanParalel(session, tahun,
														jenisSemester, null, "", "", false, null, true, false, false,

														true, true, true, true, true, true,

														true, true, true,

														TampilanELearningAction.SKRIPSI,

														0, 10000);

												List<Skripsi> skripsis = (List<Skripsi>) objects[0];

												ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
												groupbox.setStyle("min-height: 1200px;");
												groupbox.setParent(detail);
												MyGrid grid = new MyGrid();
												grid.setWidth("100%");
												grid.setParent(groupbox);
												grid.setWidth("100%");
												grid.setHeight("100%");
												grid.setStyle("min-height: 1200px;");
												grid.setSclass("dgrid");

												org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
												columns.setParent(grid);
												MyColumnConfig column = new MyColumnConfig();
												column.setWidth("40px");
												column.setLabel("");
												columns.appendChild(column);

												column = new MyColumnConfig();
												column.setLabel("Mahasiswa");
												column.setWidth("30%");
												columns.appendChild(column);
												column = new MyColumnConfig();
												column.setLabel("Judul / Penguji");
												columns.appendChild(column);

												Rows rows = new Rows();
												rows.setParent(grid);
												for (final Skripsi skripsi : skripsis) {
													MyFormRow row = new MyFormRow();
													row.setValign("top");
													row.setParent(rows);

													final MyDetail detail = new MyDetail();
													detail.setParent(row);
													detail.addEventListener("onOpen", new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															Common.clear(detail);
															if (detail.isOpen()) {
																ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
																groupbox.setStyle("min-height: 200px;");
																aktifitasSkripsiHelper.initDetail(skripsi, groupbox);
																detail.appendChild(groupbox);
															}
														}
													});

													SkripsiAction.tampilkanInfoMahasiswa(skripsi, null).setParent(row);
													SkripsiAction.tampilkanInfoDosen(skripsi, true, true)
															.setParent(row);

												}
											}
										}
									});

									row.appendChild(new Label(objects[0] == null ? "-" : objects[0].toString()));
									row.appendChild(new Label(objects[1] == null ? "" : objects[1].toString()));
									row.appendChild(new Label(program));
									row.appendChild(new Label(Common.numberFormat.get().format(jumlah)));

									total += jumlah;
								}

								MyFormRow row = new MyFormRow();
								row.setValign("top");
								row.setStyle("background-color: rgba(224, 224, 235,0.4);");
								row.setParent(rows);
								row.appendChild(new Label());
								row.appendChild(new Label());
								row.appendChild(new Label());
								row.appendChild(new MyLabelBoldConfig("Total Bimbingan"));
								row.appendChild(new MyLabelBold(Common.numberFormat.get().format(total)));

							}

						}

					});
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					detail.setParent(row);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(tahun));
					row.appendChild(new ais.ui.util.MyLabelConfig(jenisSemester));

				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BiodataDosenAction.java:5127");
				// TODO: handle exception
			}
		}

	}

	public static DspaceInformation getDspaceSK(String cookie, PenugasanDosenMengajar penugasanDosenMengajar)
			throws Exception {
		Jurusan jurusan = penugasanDosenMengajar.getJurusan();

		String description = "SK Mengajar untuk " + Common.getBahasaConfig("Jurusan") + " " + jurusan.getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", "SK Mengajar");
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription", "SK Mengajar " + jurusan.getJenjang().getNama() + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common.getKonfigurasi("dspace_label_collection_sk_mengajar_" + jurusan.getId(),
				"");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "collections",
				"communities/" + JurusanAction.getDspace(cookie, jurusan, false) + "/collections");

	}

	public static DspaceInformation getDspaceTugasMengajar(String cookie, PenugasanDosenMengajar penugasanDosenMengajar,
			boolean update) throws Exception {

		JSONArray jsonArray = new JSONArray();
		String penulis = "dc.contributor.author";

		String nama = penugasanDosenMengajar.getDosen().getNama();
		JSONObject jsonMetadata = new JSONObject();
		jsonMetadata.put("key", penulis);
		jsonMetadata.put("value", nama);
		jsonArray.put(jsonMetadata);

		String tahun = (String) penugasanDosenMengajar.getTahunAkademik();
		String thn = StringUtils.split(tahun, "/")[0];

		String sb = "Penugasan mengajar " + nama + " di prodi " + penugasanDosenMengajar.getJurusan().getNama() + " TA "
				+ thn + " smt " + penugasanDosenMengajar.getSemester() + " sebanyak " + penugasanDosenMengajar.getSks()
				+ " sks";

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.description.abstract");
		jsonMetadata.put("value", sb);
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.title");
		jsonMetadata.put("value",
				penugasanDosenMengajar.getKode() == null || penugasanDosenMengajar.getKode().trim().isEmpty()
						? "Tidak ada no SK"
						: penugasanDosenMengajar.getKode());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.date.copyright");
		jsonMetadata.put("value",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonArray.put(jsonMetadata);

		if (penugasanDosenMengajar.getTanggalSuratTugas() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.date.issued");
			jsonMetadata.put("value", Common.databaseDateFormat.get().format(penugasanDosenMengajar.getTanggalSuratTugas()));
			jsonArray.put(jsonMetadata);
		}

		LampiranLain lam = LampiranLain.ambil(penugasanDosenMengajar.getId(), "sk_penugasan_pengajaran_dosen");
		if (lam != null) {
			String uri = lam.createLinkUri();
			if (uri != null && !uri.trim().isEmpty()) {
				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.uri");
				jsonMetadata.put("value", uri);
				jsonArray.put(jsonMetadata);
			}
		}

		String jenisSemesterNumber = penugasanDosenMengajar.getSemester().equals(Perkuliahan.SP) ? "3"
				: penugasanDosenMengajar.getSemester().equals(Perkuliahan.GANJIL) ? "1" : "2";
		Long idDosen = penugasanDosenMengajar.getDosen().getId();
		String kode = Common.maxPanjangAkhir("000000000000000000000" + idDosen + "00" + thn + jenisSemesterNumber, 14);
		kode = "1" + kode;
		Long l = Long.parseLong(kode);

		LampiranLain lampiranLain = LampiranLain.ambil(l, "sk_penugasan_pengajaran_dosen_gabungan");
		if (lampiranLain != null) {
			String uri = lampiranLain.createLinkUri(false);
			if (uri != null && !uri.trim().isEmpty()) {
				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.uri");
				jsonMetadata.put("value", uri);
				jsonArray.put(jsonMetadata);
			}
		}

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("metadata", jsonArray);

		DspaceInformation dspaceInformation = DspaceInformation.dspaceProcess(cookie, penugasanDosenMengajar,
				jsonPost.toString(), jsonArray.toString(), update, "items",
				"collections/" + getDspaceSK(cookie, penugasanDosenMengajar) + "/items", "items/{uuid}/metadata");

		if (lam != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lam,
					"SK Mengajar " + nama + " pada prodi " + penugasanDosenMengajar.getJurusan().getNama() + " TA "
							+ penugasanDosenMengajar.getTahunAkademik() + " semester "
							+ penugasanDosenMengajar.getSemester());
		}

		if (lampiranLain != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain,
					"SK Mengajar Gabungan " + nama + " pada prodi " + penugasanDosenMengajar.getJurusan().getNama()
							+ " TA " + penugasanDosenMengajar.getTahunAkademik() + " semester "
							+ penugasanDosenMengajar.getSemester());
		}

		return dspaceInformation;

	}

}
