package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
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

import ais.action.maintenance.TbmuserAction;
import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.employ.helper.KedinasanPegawaiHelper;
import ais.action.master.employ.helper.KeluargaPegawaiHelper;
import ais.action.master.employ.helper.KenaikanPangkatHelper;
import ais.action.master.employ.helper.PenghargaanPegawaiHelper;
import ais.action.master.employ.helper.RiwayatKartuIdentitasPegawaiHelper;
import ais.action.master.employ.helper.RiwayatKeluarNegeriPegawaiHelper;
import ais.action.master.employ.helper.RiwayatKerjaPegawaiHelper;
import ais.action.master.employ.helper.RiwayatKeteranganLainPegawaiHelper;
import ais.action.master.employ.helper.RiwayatOrganisasiKampusPegawaiHelper;
import ais.action.master.employ.helper.RiwayatOrganisasiLainPegawaiHelper;
import ais.action.master.employ.helper.RiwayatOrganisasiSekolahPegawaiHelper;
import ais.action.master.employ.helper.RiwayatPelatihanPegawaiHelper;
import ais.action.master.employ.helper.RiwayatPendidikanPegawaiHelper;
import ais.action.master.employ.helper.RiwayatSeminarPegawaiHelper;
import ais.action.master.employ.helper.RiwayatTandaJasaPegawaiHelper;
import ais.action.master.employ.helper.SangsiPegawaiHelper;
import ais.action.master.employ.util.FormBiodataPegawaiUtil;
import ais.action.master.helper.AbsensiKehadiranPegawaiHarianHelper;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataNamaSekolahBanbox;
import ais.action.master.helper.DetailArtikelHelper;
import ais.action.master.helper.MainHelper;
import ais.action.master.penelitiandanpengabdian.helper.PengajuanPenelitianDanPengabdianHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.sekolah.GuruAction;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.report.format1.akademik.LaporanAbsensiPegawaiPerOrang;
import ais.action.report.format1.payroll.LaporanSlipGajiPegawaiPerOrang;
import ais.action.report.format1.payroll.LaporanSlipGajiRealPegawaiPerOrang;
import ais.common.Common;
import ais.common.CommonOnSearchdefault;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Agama;
import ais.database.model.Bank;
import ais.database.model.BiodataDosen;
import ais.database.model.BiodataPegawai;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.IkatanKerjaDosen;
import ais.database.model.JenisTenagaKependidikan;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.PengumumanAkademis;
import ais.database.model.SatuanKerjaPegawai;
import ais.database.model.StatusKepegawaian;
import ais.database.model.StatusPegawai;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.employ.GajiPokok;
import ais.database.model.employ.Insentif;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.employ.Makan;
import ais.database.model.employ.MasaKerja;
import ais.database.model.employ.Pendidikan;
import ais.database.model.employ.TipeMasaKerja;
import ais.database.model.employ.TipePegawai;
import ais.database.model.employ.Transport;
import ais.database.model.employ.UnitKerja;
import ais.database.model.file.FotoDosen;
import ais.database.model.file.FotoGuru;
import ais.database.model.file.FotoPegawai;
import ais.database.model.file.LampiranLain;
import ais.database.model.kpi.PenilaianKpi;
import ais.database.model.payroll.AsuransiPegawai;
import ais.database.model.payroll.JatahCuti;
import ais.database.model.payroll.PtkpPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class BiodataPegawaiAction extends MyWindow {

	private static final long serialVersionUID = 72558191307949087L;

	private CommonOnSearchdefault commonOnSearchdefault;

	private ManagingBiodataPegawai managingBiodataPegawai = new ManagingBiodataPegawai();
	private ManagingPegawai managingPegawai = new ManagingPegawai();

	private RiwayatPendidikanPegawaiHelper riwayatPendidikanPegawaiHelper;
	private KeluargaPegawaiHelper keluargaPegawaiHelper;
	private RiwayatPelatihanPegawaiHelper riwayatPelatihanPegawaiHelper;
	private RiwayatTandaJasaPegawaiHelper riwayatTandaJasaPegawaiHelper;
	private RiwayatKartuIdentitasPegawaiHelper riwayatKartuIdentitasPegawaiHelper;
	private RiwayatKeluarNegeriPegawaiHelper riwayatKeluarNegeriPegawaiHelper;
	private RiwayatOrganisasiSekolahPegawaiHelper riwayatOrganisasiSekolahPegawaiHelper;
	private RiwayatOrganisasiKampusPegawaiHelper riwayatOrganisasiKampusPegawaiHelper;
	private RiwayatOrganisasiLainPegawaiHelper riwayatOrganisasiLainPegawaiHelper;
	private RiwayatKeteranganLainPegawaiHelper riwayatKeteranganLainPegawaiHelper;
	private KenaikanPangkatHelper kenaikanPangkatHelper;
	private RiwayatKerjaPegawaiHelper riwayatKerjaPegawai;
	private RiwayatSeminarPegawaiHelper riwayatSeminarPegawaiHelper;

	private Boolean tampilBatal = true;
	private Boolean tampilSave = true;

	public MyCheckboxConfig sertifikasi;
	private Combobox ptkpPegawai;
	private Combobox asuransiPegawai1;
	private Combobox asuransiPegawai2;
	private Combobox asuransiPegawai3;
	public MyDoublebox tunjanganKinerja;

	public Decimalbox jatahCutiTahunan;

	public MyDatebox tanggalkeluarHonorer;

	public Textbox lintang;
	public Textbox bujur;

	public Combobox ikatanKerjaDosen;
	public MyCheckboxConfig checkboxConfigDosen;

	private Textbox nomorAsuransiPegawai1;
	private Textbox nomorAsuransiPegawai2;
	private Textbox nomorAsuransiPegawai3;
	private Textbox nomorAsuransiPegawai4;

	private Pegawai pegawaiData = null;

	// ===================================================================================
	// HELPER: MANAJEMEN SESSION ANTI LEAK
	// ===================================================================================
	private void cleanupSession(Session session) {
		if (session != null) {
			try {
				if (session.isOpen())
					session.clear();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BiodataPegawaiAction.java:196");
			}
			try {
				session.disconnect();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BiodataPegawaiAction.java:200");
			}
			try {
				if (session.isOpen())
					session.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BiodataPegawaiAction.java:205");
			}
		}
	}

	public BiodataPegawaiAction() throws Exception {
		super();
		init(null);
	}

	public BiodataPegawaiAction(SatuanKerja satuanKerjaOnSession) throws Exception {
		super();
		init(null);
	}

	public BiodataPegawaiAction(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init(null);
	}

	public BiodataPegawaiAction(String title, String border, boolean closable, SatuanKerja satuanKerjaOnSession)
			throws Exception {
		super(title, border, closable);
		init(null);
	}

	public BiodataPegawaiAction(Pegawai pegawai) throws Exception {
		super();
		init(pegawai);
	}

	public BiodataPegawaiAction(Pegawai pegawai, Boolean tampilSave) throws Exception {
		super();
		this.tampilSave = tampilSave;
		init(pegawai);
	}

	public BiodataPegawaiAction(Pegawai pegawai, SatuanKerja satuanKerjaOnSession) throws Exception {
		super();
		init(pegawai);
	}

	public BiodataPegawaiAction(Pegawai pegawai, SatuanKerja satuanKerjaOnSession, Boolean tampilLogin,
			Boolean tampilBatal) throws Exception {
		super();
		this.tampilBatal = tampilBatal;
		init(pegawai);
	}

	public BiodataPegawaiAction(Pegawai pegawai, String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init(pegawai);
	}

	private void init(Pegawai pegawai) throws Exception {
		setTitle("Biodata Pegawai");
		this.pegawaiData = pegawai;
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			if (pegawai == null && Common.getCurrentUser() != null) {
				pegawai = Common.getCurrentUser().getPegawai();
			}
			if (pegawai == null && Common.getCurrentUser() != null) {
				Dosen dosen = Common.getCurrentUser().getDosen();
				if (dosen != null) {
					pegawai = (Pegawai) ConstantValues.simpleObject(
							session.createCriteria(Pegawai.class).add(Restrictions.eq("dosen", dosen)).setMaxResults(1),
							Pegawai.class);
				}
			}
			if (pegawai == null && Common.getCurrentUser() != null) {
				Guru guru = Common.getCurrentUser().getGuru();
				if (guru != null) {
					pegawai = (Pegawai) ConstantValues.simpleObject(
							session.createCriteria(Pegawai.class).add(Restrictions.eq("guru", guru)).setMaxResults(1),
							Pegawai.class);
				}
			}
		} finally {
			cleanupSession(session);
		}

		if (pegawai == null) {
			MyMessageboxConfig.show("Mohon maaf, halaman Biodata Pegawai hanya dapat diakses oleh pengguna yang terhubung dengan data kepegawaian. Langkah yang dapat dilakukan: (1) pastikan Anda masuk (login) menggunakan akun yang tertaut sebagai pegawai; (2) hubungi administrator untuk mengaitkan akun Anda dengan data pegawai; (3) ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		boolean tampilSangsi = true;
		String konfiguarsiSangsi = Common.getKonfigurasi("hak_akses_yg_boleh_akses_sk_sangsi", "").getNilai().trim();
		Tbmuser tbmuser = Common.getCurrentUser();
		Tbmrole tbmrole = tbmuser == null ? null : tbmuser.hakAkses();
		if (tbmrole != null && tbmrole.getRoleId() != null && !konfiguarsiSangsi.trim().isEmpty()) {
			tampilSangsi = false;
			for (String ss : konfiguarsiSangsi.split(",")) {
				if (ss.trim().equalsIgnoreCase(tbmrole.getRoleId().trim())) {
					tampilSangsi = true;
					break;
				}
			}
		}

		Tabpanel tabpanelPegawai = managingPegawai.init(pegawai);
		Tabpanel tabpanelBiodataPegawai = managingBiodataPegawai.preInit(pegawai);

		final Tabpanel tabpanelRiwayatPendidikan = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelRiwayatPelatihan = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelRiwayatSeminar = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelRiwayatTandaJasa = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelRiwayatKartuIdentitas = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelRiwayatKedinasan = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelRiwayatPenghargaan = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelRiwayatSangsi = new ais.ui.util.MyTabpanel();
		tabpanelRiwayatSangsi.setVisible(tampilSangsi);
		final Tabpanel tabpanelRiwayatOrganisasiSekolah = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelRiwayatOrganisasiKampus = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelRiwayatOrganisasiLain = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelRiwayatKeluarNegeri = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelRiwayatKepangkatan = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelRiwayatKerja = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelKeluarga = new ais.ui.util.MyTabpanel();
		final Tabpanel tabpanelRiwayatKeteranganLain = new ais.ui.util.MyTabpanel();

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
		final MyTabConfig tab4;
		final MyTabConfig tab5;
		final MyTabConfig tab51;
		final MyTabConfig tab52;
		final MyTabConfig tab53;
		final MyTabConfig tab54;
		final MyTabConfig tab55;
		final MyTabConfig tab56;
		final MyTabConfig tab57;
		final MyTabConfig tab58;
		final MyTabConfig tab59;
		final MyTabConfig tab512;
		final MyTabConfig tab5211;
		final MyTabConfig tab6;

		tabs.appendChild(tab1 = new MyTabConfig("Data"));

		boolean tampilRincianDataPegawai = Common.bolehKonfigurasi("tampilRincianDataPegawai");

		tabs.appendChild(tab2 = new MyTabConfig("Rincian Data"));
		tab2.setVisible(tampilRincianDataPegawai);
		tabpanelBiodataPegawai.setVisible(tampilRincianDataPegawai);

		MyTabConfig tabPrestasi = new MyTabConfig("Prestasi");
		tabPrestasi.setParent(tabs);

		boolean terhubung_ke_ojs = Common.bolehKonfigurasi("tampilkan_penelitian_dan_pengabdian_di_pegawai", Konfigurasi.TIDAK_AKTIF);

		final MyTabConfig tabPenelitian = new MyTabConfig("Penelitian dan Pengabdian");
		tabPenelitian.setVisible(terhubung_ke_ojs);
		tabPenelitian.setParent(tabs);

		final MyTabConfig tabPublikasi = new MyTabConfig("Publikasi Ilmiah");
		tabPublikasi.setVisible(terhubung_ke_ojs);
		tabPublikasi.setParent(tabs);

		boolean tampilRiwayatAbsen = Common.bolehKonfigurasi("tampilRiwayatAbsen");

		tabs.appendChild(tab4 = new MyTabConfig("Absensi"));
		tab4.setVisible(tampilRiwayatAbsen);

		tabs.appendChild(tab59 = new MyTabConfig("Kartu Identitas"));
		MyTabConfig tab591;
		tabs.appendChild(tab591 = new MyTabConfig("SK Kedinasan"));
		MyTabConfig tab592;
		tabs.appendChild(tab592 = new MyTabConfig("SK Penghargaan"));
		MyTabConfig tab593;
		tabs.appendChild(tab593 = new MyTabConfig("SK Sangsi"));
		tab593.setVisible(tampilSangsi);
		tabs.appendChild(tab6 = new MyTabConfig("Keluarga Pegawai"));
		tabs.appendChild(tab5 = new MyTabConfig("Riwayat Pendidikan"));
		tabs.appendChild(tab51 = new MyTabConfig("Riwayat Pelatihan / Kursus"));
		tabs.appendChild(tab512 = new MyTabConfig("Riwayat Seminar"));

		boolean tampilRiwayatSeminar = Common.bolehKonfigurasi("tampilRiwayatSeminar");
		tab512.setVisible(tampilRiwayatSeminar);
		tabpanelRiwayatSeminar.setVisible(tampilRiwayatSeminar);

		boolean tampilKepangkatanKesana = Common.bolehKonfigurasi("tampilKepangkatanKesana");
		boolean tampilRiwayatBekerja = Common.bolehKonfigurasi("tampilRiwayatBekerja");

		tabs.appendChild(tab5211 = new MyTabConfig("Riwayat Bekerja"));
		tabs.appendChild(tab52 = new MyTabConfig("Riwayat Kepangkatan"));
		tabs.appendChild(tab53 = new MyTabConfig("Riwayat Tanda Jasa / Penghargaan"));
		tabs.appendChild(tab54 = new MyTabConfig("Riwayat Keluar Negeri"));
		tabs.appendChild(tab55 = new MyTabConfig("Riwayat Organisasi Sekolah"));
		tabs.appendChild(tab56 = new MyTabConfig("Riwayat Organisasi Kampus"));
		tabs.appendChild(tab58 = new MyTabConfig("Riwayat Organisasi Lain"));
		tabs.appendChild(tab57 = new MyTabConfig("Keterangan Lain"));

		tab5211.setVisible(tampilRiwayatBekerja);
		tab52.setVisible(tampilKepangkatanKesana);
		tab53.setVisible(tampilKepangkatanKesana);
		tab54.setVisible(tampilKepangkatanKesana);
		tab55.setVisible(tampilKepangkatanKesana);
		tab56.setVisible(tampilKepangkatanKesana);
		tab57.setVisible(tampilKepangkatanKesana);

		tabpanelRiwayatTandaJasa.setVisible(tampilKepangkatanKesana);
		tabpanelRiwayatOrganisasiSekolah.setVisible(tampilKepangkatanKesana);
		tabpanelRiwayatOrganisasiKampus.setVisible(tampilKepangkatanKesana);
		tabpanelRiwayatKeluarNegeri.setVisible(tampilKepangkatanKesana);
		tabpanelRiwayatKepangkatan.setVisible(tampilKepangkatanKesana);
		tabpanelRiwayatKerja.setVisible(tampilRiwayatBekerja);

		tab2.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else {
					managingBiodataPegawai.pegawai = pegawai;
				}
			}
		});

		final Tabpanel tabpanelAccount = new ais.ui.util.MyTabpanel();
		tabpanelAccount.setHeight("560px");
		tabpanelAccount.setVisible(tampilRiwayatAbsen);

		tab4.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				final Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else {
					managingBiodataPegawai.pegawai = pegawai;
					if (tabpanelAccount.getChildren().isEmpty()) {
						Tabbox tabbox = new Tabbox();
						tabbox.setWidth("100%");
						tabbox.setHeight("555px");
						tabbox.setParent(tabpanelAccount);
						Tabs tabs = new Tabs();
						tabs.setParent(tabbox);

						final MyTabConfig tab62;
						tabs.appendChild(new MyTabConfig("Sejarah Absensi"));
						tabs.appendChild(tab62 = new MyTabConfig("Laporan Absensi"));

						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
						final Tabpanel tabpanelSlipGaji = new ais.ui.util.MyTabpanel();

						AbsensiKehadiranPegawaiHarianHelper absensiKehadiranPegawaiHarianHelper = new AbsensiKehadiranPegawaiHarianHelper(
								pegawai);
						Row r = Common.tampilanScroll(tabpanelUtama);
						r.appendChild(absensiKehadiranPegawaiHarianHelper.display());

						tabpanels.appendChild(tabpanelUtama);
						tabpanels.appendChild(tabpanelSlipGaji);

						tab62.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (tabpanelSlipGaji.getChildren().isEmpty()) {
									LaporanAbsensiPegawaiPerOrang laporan = new LaporanAbsensiPegawaiPerOrang(pegawai);
									laporan.setHeight("100%");
									laporan.setWidth("100%");
									laporan.setParent(tabpanelSlipGaji);
								}
							}
						});
						Common.freeze(r, true);
					}
				}
			}
		});

		tab5.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else {
					riwayatPendidikanPegawaiHelper = new RiwayatPendidikanPegawaiHelper(pegawai, true);
					tabpanelRiwayatPendidikan.appendChild(riwayatPendidikanPegawaiHelper.display());
				}
			}
		});

		tab51.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else {
					riwayatPelatihanPegawaiHelper = new RiwayatPelatihanPegawaiHelper(pegawai);
					tabpanelRiwayatPelatihan.appendChild(riwayatPelatihanPegawaiHelper.display());
				}
			}
		});

		tab512.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else {
					riwayatSeminarPegawaiHelper = new RiwayatSeminarPegawaiHelper(pegawai);
					tabpanelRiwayatSeminar.appendChild(riwayatSeminarPegawaiHelper.display());
				}
			}
		});

		tab5211.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else {
					riwayatKerjaPegawai = new RiwayatKerjaPegawaiHelper(pegawai);
					tabpanelRiwayatKerja.appendChild(riwayatKerjaPegawai.display());
				}
			}
		});

		tab52.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else {
					kenaikanPangkatHelper = new KenaikanPangkatHelper(pegawai);
					tabpanelRiwayatKepangkatan.appendChild(kenaikanPangkatHelper.display());
				}
			}
		});

		tab53.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else {
					riwayatTandaJasaPegawaiHelper = new RiwayatTandaJasaPegawaiHelper(pegawai);
					tabpanelRiwayatTandaJasa.appendChild(riwayatTandaJasaPegawaiHelper.display());
				}
			}
		});

		tab59.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else {
					riwayatKartuIdentitasPegawaiHelper = new RiwayatKartuIdentitasPegawaiHelper(pegawai);
					tabpanelRiwayatKartuIdentitas.appendChild(riwayatKartuIdentitasPegawaiHelper.display());
				}
			}
		});

		tab591.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else if (tabpanelRiwayatKedinasan.getChildren().isEmpty()) {
					KedinasanPegawaiHelper kedinasanPegawaiHelper = new KedinasanPegawaiHelper(pegawai);
					tabpanelRiwayatKedinasan.appendChild(kedinasanPegawaiHelper.display());
				}
			}
		});

		tab592.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else if (tabpanelRiwayatPenghargaan.getChildren().isEmpty()) {
					PenghargaanPegawaiHelper penghargaanPegawaiHelper = new PenghargaanPegawaiHelper(pegawai);
					tabpanelRiwayatPenghargaan.appendChild(penghargaanPegawaiHelper.display());
				}
			}
		});

		tab593.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else if (tabpanelRiwayatSangsi.getChildren().isEmpty()) {
					SangsiPegawaiHelper sangsiPegawaiHelper = new SangsiPegawaiHelper(pegawai);
					tabpanelRiwayatSangsi.appendChild(sangsiPegawaiHelper.display());
				}
			}
		});

		tab55.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else {
					riwayatOrganisasiSekolahPegawaiHelper = new RiwayatOrganisasiSekolahPegawaiHelper(pegawai);
					tabpanelRiwayatOrganisasiSekolah.appendChild(riwayatOrganisasiSekolahPegawaiHelper.display());
				}
			}
		});

		tab56.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else {
					riwayatOrganisasiKampusPegawaiHelper = new RiwayatOrganisasiKampusPegawaiHelper(pegawai);
					tabpanelRiwayatOrganisasiKampus.appendChild(riwayatOrganisasiKampusPegawaiHelper.display());
				}
			}
		});

		tab58.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else {
					riwayatOrganisasiLainPegawaiHelper = new RiwayatOrganisasiLainPegawaiHelper(pegawai);
					tabpanelRiwayatOrganisasiLain.appendChild(riwayatOrganisasiLainPegawaiHelper.display());
				}
			}
		});

		tab57.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else {
					riwayatKeteranganLainPegawaiHelper = new RiwayatKeteranganLainPegawaiHelper(pegawai);
					tabpanelRiwayatKeteranganLain.appendChild(riwayatKeteranganLainPegawaiHelper.display());
				}
			}
		});

		tab54.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else {
					riwayatKeluarNegeriPegawaiHelper = new RiwayatKeluarNegeriPegawaiHelper(pegawai);
					tabpanelRiwayatKeluarNegeri.appendChild(riwayatKeluarNegeriPegawaiHelper.display());
				}
			}
		});

		tab6.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else {
					keluargaPegawaiHelper = new KeluargaPegawaiHelper(pegawai);
					tabpanelKeluarga.appendChild(keluargaPegawaiHelper.display());
				}
			}
		});

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);
		tabpanels.appendChild(tabpanelPegawai);
		tabpanels.appendChild(tabpanelBiodataPegawai);

		final Pegawai thisPegawai = pegawai;

		final Tabpanel tabpanelPrestasi = new ais.ui.util.MyTabpanel();
		tabpanelPrestasi.setParent(tabpanels);
		tabPrestasi.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPrestasi.getChildren().isEmpty()) {
					MyInclude iframe = new MyInclude(
							"/pages/master/prestasi_pegawai.zul?pegawai=" + thisPegawai.getId());
					iframe.setParent(tabpanelPrestasi);
				}
			}
		});

		final Tabpanel tabpanelPublikasi = new ais.ui.util.MyTabpanel();
		tabpanelPublikasi.setParent(tabpanels);
		tabpanelPublikasi.setVisible(terhubung_ke_ojs);

		tabPenelitian.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (tabpanelPublikasi.getChildren().isEmpty()) {
					Session sessionObj = null;
					try {
						sessionObj = HibernateUtil.getSessionFactory().openSession();
						Tbmuser tbmuser = (Tbmuser) ConstantValues.simpleObject(
								sessionObj.createCriteria(Tbmuser.class).add(Restrictions.eq("pegawai", thisPegawai))
										.addOrder(Order.desc("tanggal_dirubah")).setMaxResults(1),
								Tbmuser.class);

						if (tbmuser != null) {
							PengajuanPenelitianDanPengabdianHelper pengajuanHelper = new PengajuanPenelitianDanPengabdianHelper();
							MyWindow addWindowPengajuan = new MyWindow();
							addWindowPengajuan
									.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							pengajuanHelper.displayPengajuan(false, tbmuser.getUserId(),
									PengumumanAkademis.UNTUK_PEGAWAI, null, tabpanelPublikasi, addWindowPengajuan,
									ConstantValues.PENELITIAN_LAINNYA, "500px");
						} else {
							MyMessageboxConfig.show(
									"Mohon maaf, pegawai ini belum memiliki akun / login pengguna sehingga data terkait belum dapat ditampilkan. Langkah yang dapat dilakukan: (1) buatkan akun pengguna untuk pegawai yang bersangkutan melalui menu Pengguna; (2) pastikan akun telah dikaitkan dengan pegawai ini; (3) ulangi kembali proses ini.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					} finally {
						cleanupSession(sessionObj);
					}
				}
			}
		});

		final Tabpanel tabpanelPublikasiIlmiah = new ais.ui.util.MyTabpanel();
		tabpanelPublikasiIlmiah.setParent(tabpanels);
		tabpanelPublikasiIlmiah.setVisible(terhubung_ke_ojs);

		tabPublikasi.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (tabpanelPublikasiIlmiah.getChildren().isEmpty()) {
					Session sessionPub = null;
					try {
						sessionPub = HibernateUtil.getSessionFactory().openSession();
						Tbmuser tbmuser = (Tbmuser) ConstantValues.simpleObject(
								sessionPub.createCriteria(Tbmuser.class).add(Restrictions.eq("pegawai", thisPegawai))
										.addOrder(Order.desc("tanggal_dirubah")).setMaxResults(1),
								Tbmuser.class);

						if (tbmuser != null) {
							DetailArtikelHelper detailArtikelHelper = new DetailArtikelHelper(null);
							MyWindow addWindowPengajuan = new MyWindow();
							addWindowPengajuan
									.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							detailArtikelHelper.displayPengajuan(false, tbmuser.getUserId(),
									PengumumanAkademis.UNTUK_PEGAWAI, null, tabpanelPublikasiIlmiah, addWindowPengajuan,
									"500px");
						} else {
							MyMessageboxConfig.show(
									"Mohon maaf, pegawai ini belum memiliki akun / login pengguna sehingga data terkait belum dapat ditampilkan. Langkah yang dapat dilakukan: (1) buatkan akun pengguna untuk pegawai yang bersangkutan melalui menu Pengguna; (2) pastikan akun telah dikaitkan dengan pegawai ini; (3) ulangi kembali proses ini.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					} finally {
						cleanupSession(sessionPub);
					}
				}
			}
		});

		tabpanels.appendChild(tabpanelAccount);
		tabpanels.appendChild(tabpanelRiwayatKartuIdentitas);
		tabpanels.appendChild(tabpanelRiwayatKedinasan);
		tabpanels.appendChild(tabpanelRiwayatPenghargaan);
		tabpanels.appendChild(tabpanelRiwayatSangsi);
		tabpanels.appendChild(tabpanelKeluarga);
		tabpanels.appendChild(tabpanelRiwayatPendidikan);
		tabpanels.appendChild(tabpanelRiwayatPelatihan);
		tabpanels.appendChild(tabpanelRiwayatSeminar);
		tabpanels.appendChild(tabpanelRiwayatKerja);
		tabpanels.appendChild(tabpanelRiwayatKepangkatan);
		tabpanels.appendChild(tabpanelRiwayatTandaJasa);
		tabpanels.appendChild(tabpanelRiwayatKeluarNegeri);
		tabpanels.appendChild(tabpanelRiwayatOrganisasiSekolah);
		tabpanels.appendChild(tabpanelRiwayatOrganisasiKampus);
		tabpanels.appendChild(tabpanelRiwayatOrganisasiLain);
		tabpanels.appendChild(tabpanelRiwayatKeteranganLain);

		boolean tampilRiwayatMediaSosial = Common.bolehKonfigurasi("tampilRiwayatMediaSosial");
		if (tampilRiwayatMediaSosial) {
			final MyTabConfig tabSosial = new MyTabConfig("Media Sosial");
			tabSosial.setParent(tabs);
			final Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			tabpanel.setParent(tabpanels);
			Common.displaySocialMedia(tabSosial, tabpanel, null, null, null, pegawai);
		}

		ais.ui.util.MyButtonTabbox.gantiTabboxNative(tabbox, new int[] { 1 });

		South south = new South();
		south.setVisible(tampilSave);
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.setVisible(tampilBatal);
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				BiodataPegawaiAction.this.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				save(event);
			}
		});
		save.setParent(toolbar);
	}

	public void save(Event event) throws Exception {
		Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
		if (pegawai != null && pegawai.getId() != null) {
			managingBiodataPegawai.pegawai = pegawai;

			boolean result = managingBiodataPegawai.onSave(event);

			if (result) {
				if (commonOnSearchdefault != null)
					commonOnSearchdefault.onSearchDefault(new Event("", null, pegawai));
			} else {
				MyMessageboxConfig.show("Mohon maaf, data tidak berhasil disimpan. Langkah yang dapat dilakukan: (1) periksa kembali kelengkapan dan kebenaran isian data; (2) pastikan koneksi ke sistem stabil; (3) ulangi kembali proses penyimpanan; (4) apabila masih gagal, mohon hubungi administrator sistem.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
			}
		}
	}

	public void setCommonOnSearchdefault(CommonOnSearchdefault commonOnSearchdefault) {
		this.commonOnSearchdefault = commonOnSearchdefault;
	}

	public CommonOnSearchdefault getCommonOnSearchdefault() {
		return commonOnSearchdefault;
	}

	private class ManagingPegawai {
		private Textbox code;
		private Textbox mycode;
		private Textbox nama;
		private Textbox ktp;
		private Textbox alamat;
		private Textbox email;
		private Intbox usiaPensiun;
		private Textbox telp;
		private Textbox telpDarurat;
		private Textbox namaDarurat;
		private Textbox statusDarurat;
		private Textbox golonganDarah;
		private Textbox nomorKartuKeluarga;
		private Textbox namaIbuKandung;
		private Combobox kelamin;
		private Textbox tempatlahir;
		private MyDatebox tanggallahir;
		private Textbox pangkat;
		private Textbox jabatan;
		private Textbox spesialisasi1;
		private Textbox spesialisasi2;
		private Textbox spesialisasi3;

		private Combobox agama;
		private Combobox statusPerkawinan;
		private Textbox alamatJalan;
		private Textbox alamatKelurahan;
		private Textbox alamatKecamatan;
		private Textbox alamatKabupaten;
		private Textbox alamatPropinsi;

		private Combobox bank;
		private Textbox norek;
		private Textbox karis;
		private Textbox askes;
		private Textbox taspen;
		private Textbox npwp;

		private Textbox keteranganBadanTinggi;
		private Textbox keteranganBadanBerat;
		private Textbox keteranganBadanRambut;
		private Textbox keteranganBadanBentukMuka;
		private Textbox keteranganBadanWarnaKulit;
		private Textbox keteranganBadanCiriKhas;
		private Textbox keteranganBadanCacat;
		private MyDatebox tanggalmasuk;
		public AmbilDataDosenBanbox dosen;
		private Textbox hobi;

		private Pegawai pegawai;
		private Combobox statusPegawai;
		private Combobox jenisTenagaKependidikan;
		private Combobox pendidikan;
		private AmbilDataSatuanKerjaBanbox satuanKerja;
		private Textbox keterangan;
		private Combobox bahasa;
		protected LampiranLain lainMahasiswa;
		private Textbox idfinger;
		protected LampiranLain ttd;
		private Combobox statusKepegawaian;
		protected FotoPegawai fotoPegawai;
		private AmbilDataPegawaiBanbox atasanlangsung;
		private Combobox atasanJbt;
		private Combobox atasanPendukung;
		private Combobox atasanPendukungCadangan;
		private AmbilDataPegawaiBanbox atasanlangsung2;
		private AmbilDataPegawaiBanbox atasanlangsung3;
		private MyDatebox tanggalkeluar;

		private Label masaKerjaTahunPengalamanKerja;
		private Label masaKerjaBulanPengalamanKerja;

		private Label masaKerjaTahunHonorer;
		private Label masaKerjaBulanHonorer;

		private Label masaKerjaTahunSemiTetap;
		private Label masaKerjaBulanSemiTetap;

		private Label masaKerjaTahun;
		private Label masaKerjaBulan;
		private Label GAPOK;
		private Label INSENTIF;
		private Label MAKAN;
		private Label TRANSPORT;
		private Label LAIN;
		private MyDatebox tanggalmasukHonorer;
		private MyDatebox tanggalmasukSemiTetap;
		private MyDatebox tanggalkeluarSemiTetap;
		private MyDatebox tanggalMulaiPengalanKerja;
		private MyDatebox tanggalSampaiPengalanKerja;
		private Combobox tipePegawai;
		private Combobox masaKerja;
		private Combobox tipeMasaKerja;
		private Combobox unitKerja;

		private Tbmuser tbmuser = Common.getCurrentUser();
		private AmbilDataGuruBanbox guru;
		private Combobox asuransiPegawai4;
		private MyTabConfig tab1d;
		private Combobox tendikFakultas;
		private Combobox tendikJurusan;
		private Combobox tendikSekolah;
		private MyCheckboxConfig checkboxConfigGuru;
		private MyTabConfig tab1e;
		private MyLabelConfig labelCode;
		private MyLabelConfig labelMycode;
		private Combobox bank2;
		private Textbox norek2;
		private Combobox bank3;
		private Textbox norek3;

		public ManagingPegawai() {
			kelamin = new Combobox();
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel("Laki-laki");
			comboitem.setValue("Laki-laki");
			kelamin.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Perempuan");
			comboitem.setValue("Perempuan");
			kelamin.appendChild(comboitem);

			statusPerkawinan = new Combobox();
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Belum kawin");
			comboitem.setValue("Belum kawin");
			statusPerkawinan.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Kawin");
			comboitem.setValue("Kawin");
			statusPerkawinan.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Janda");
			comboitem.setValue("Janda");
			statusPerkawinan.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Duda");
			comboitem.setValue("Duda");
			statusPerkawinan.appendChild(comboitem);
		}

		public Tabpanel init(final Pegawai pegawai) throws Exception {
			this.pegawai = pegawai;
			Tabpanel panel = new ais.ui.util.MyTabpanel();
			panel.setWidth("100%");
			panel.setHeight("100%");

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setStyle("border:0px;");

			Borderlayout borderlayoutUtama = new ais.ui.util.MyBorderlayout();
			borderlayoutUtama.setParent(panel);
			borderlayoutUtama.setStyle("border:0px;");

			Center centerUtama = new Center();
			centerUtama.setParent(borderlayoutUtama);

			Tabbox tabbox = new Tabbox();
			tabbox.setWidth("100%");
			tabbox.setHeight("100%");
			tabbox.setParent(centerUtama);
			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			final MyTabConfig tab60;
			final MyTabConfig tab61;
			final MyTabConfig tab62;
			final MyTabConfig tab1a;
			final MyTabConfig tab1b;
			final MyTabConfig tab1c;
			final MyTabConfig tab3;

			tabs.appendChild(tab60 = new MyTabConfig("Data Pegawai"));
			tabs.appendChild(tab1a = new MyTabConfig("Masa Kerja"));
			tabs.appendChild(tab1b = new MyTabConfig("Riwayat Gol & Jab"));
			tabs.appendChild(tab1c = new MyTabConfig("Asuransi"));
			tabs.appendChild(tab1d = new MyTabConfig("Catatan"));
			tabs.appendChild(tab1e = new MyTabConfig("Dokumen dan Lampiran"));

			tabs.appendChild(tab3 = new MyTabConfig("Mobile"));
			tab3.setVisible(Common.getApakahAdmin()
					&& Common.bolehKonfigurasi("tampilkan_mobile_di_profile_pegawai"));

			tabs.appendChild(tab61 = new MyTabConfig("Slip Gaji"));
			tabs.appendChild(tab62 = new MyTabConfig("Slip Gaji Perkiraan"));
			tab61.setVisible(false);
			tab62.setVisible(false);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			final Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
			final Tabpanel tabpanelUtamaA = new ais.ui.util.MyTabpanel();
			final Tabpanel tabpanelUtamaB = new ais.ui.util.MyTabpanel();
			final Tabpanel tabpanelUtamaC = new ais.ui.util.MyTabpanel();
			final Tabpanel tabpanelUtamaD = new ais.ui.util.MyTabpanel();
			final Tabpanel tabpanelUtamaE = new ais.ui.util.MyTabpanel();
			final Tabpanel tabpanelSlipGaji = new ais.ui.util.MyTabpanel();
			final Tabpanel tabpanelSlipPerkiraan = new ais.ui.util.MyTabpanel();
			tabpanelSlipPerkiraan.setVisible(false);
			tabpanelSlipGaji.setVisible(false);

			tabpanels.appendChild(tabpanelUtama);
			tabpanels.appendChild(tabpanelUtamaA);
			tabpanels.appendChild(tabpanelUtamaB);
			tabpanels.appendChild(tabpanelUtamaC);
			tabpanels.appendChild(tabpanelUtamaD);
			tabpanels.appendChild(tabpanelUtamaE);

			final Tabpanel tabpanelMobile = new ais.ui.util.MyTabpanel();
			tabpanelMobile.setParent(tabpanels);
			tabpanelMobile.setVisible(tab3.isVisible());

			tab3.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelMobile.getChildren().isEmpty()) {
						if (pegawai == null || pegawai.getId() == null) {
							tab60.setSelected(true);
						} else {
							Session sessionMobile = null;
							try {
								sessionMobile = HibernateUtil.getSessionFactory().openSession();
								Tbmuser userId = (Tbmuser) sessionMobile.createCriteria(Tbmuser.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("pegawai", pegawai))
										.addOrder(Order.desc("tanggal_dirubah")).setMaxResults(1).uniqueResult();
								if (userId == null) {
									MyMessageboxConfig.show("Mohon maaf, data / akun pengguna untuk pegawai ini tidak ditemukan sehingga proses tidak dapat dilanjutkan. Langkah yang dapat dilakukan: (1) pastikan pegawai yang bersangkutan sudah memiliki akun pengguna; (2) buatkan atau kaitkan akun pengguna melalui menu Pengguna; (3) ulangi kembali proses ini.",
											"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
									return;
								}
								MainHelper.onDapatkanKode(userId, tabpanelMobile, false);
							} finally {
								cleanupSession(sessionMobile);
							}
						}
					}
				}
			});

			tabpanels.appendChild(tabpanelSlipGaji);
			tabpanels.appendChild(tabpanelSlipPerkiraan);

			borderlayout.setParent(tabpanelUtama);

			tanggalMulaiPengalanKerja = new MyDatebox(pegawai.getTanggalMulaiPengalanKerja());
			tanggalSampaiPengalanKerja = new MyDatebox(pegawai.getTanggalSampaiPengalanKerja());
			tanggalmasukHonorer = new MyDatebox(pegawai.getTanggalmasukHonorer());
			tanggalkeluarHonorer = new MyDatebox(pegawai.getTanggalkeluarHonorer());
			tanggalmasukSemiTetap = new MyDatebox(pegawai.getTanggalmasukSemiTetap());
			tanggalkeluarSemiTetap = new MyDatebox(pegawai.getTanggalkeluarSemiTetap());
			tanggalmasuk = new MyDatebox(pegawai.getTanggalmasuk());
			tanggalkeluar = new MyDatebox(pegawai.getTanggalkeluar());
			masaKerjaTahun = new Label(pegawai.ambilMasaKerjaTahun() + " tahun");
			masaKerjaBulan = new Label(pegawai.ambilMasaKerjaBulan() + " bulan");

			masaKerjaTahunPengalamanKerja = new Label(pegawai.ambilMasaKerjaTahunPengalamanKerja() + " tahun");
			masaKerjaBulanPengalamanKerja = new Label(pegawai.ambilMasaKerjaBulanPengalamanKerja() + " bulan");

			masaKerjaTahunHonorer = new Label(pegawai.ambilMasaKerjaTahunHonorer() + " tahun");
			masaKerjaBulanHonorer = new Label(pegawai.ambilMasaKerjaBulanHonorer() + " bulan");

			masaKerjaTahunSemiTetap = new Label(pegawai.ambilMasaKerjaTahunSemiTetap() + " tahun");
			masaKerjaBulanSemiTetap = new Label(pegawai.ambilMasaKerjaBulanSemiTetap() + " bulan");

			Date sekarang = WaktuUtil.getDate();
			GajiPokok gajiPokok = pegawai.ambilGajiPokok(sekarang);
			Insentif insentif = pegawai.ambilInsentif(sekarang);
			Makan makan = pegawai.ambilMakan(sekarang);
			Transport transport = pegawai.ambilTransport(sekarang);
			GAPOK = new Label(gajiPokok == null ? "" : Common.numberFormat.get().format(gajiPokok.getGaji()));
			INSENTIF = new Label(insentif == null ? "" : Common.numberFormat.get().format(insentif.getInsentif()));
			MAKAN = new Label(makan == null ? "" : Common.numberFormat.get().format(makan.getMakan()));
			TRANSPORT = new Label(transport == null ? "" : Common.numberFormat.get().format(transport.getTransport()));
			LAIN = new Label(gajiPokok == null ? "" : Common.numberFormat.get().format(gajiPokok.getLain()));
			tunjanganKinerja = new MyDoublebox(pegawai.getTunjanganKinerja());

			ptkpPegawai = new Combobox();
			Common.insertComboDanSemua(ptkpPegawai, new String[] { "nama" }, "keterangan", PtkpPegawai.class,
					"=Belum Ditentukan=", Restrictions.eq("aktif", true));
			Common.selectComboItem(ptkpPegawai, pegawai.getPtkpPegawai());

			asuransiPegawai1 = new Combobox();
			Common.insertComboDanSemua(asuransiPegawai1, new String[] { "nama" }, "keterangan", AsuransiPegawai.class,
					"=Tidak Ada Asuransi=",
					Restrictions.and(Restrictions.or(Restrictions.isNull("jenis"),
							Restrictions.or(Restrictions.eq("jenis", AsuransiPegawai.JENIS_KHUSUS_UNTUK_PEGAWAI),
									Restrictions.eq("jenis", AsuransiPegawai.JENIS_UNTUK_KEDUANYA))),
							Restrictions.eq("aktif", true)));
			Common.selectComboItem(asuransiPegawai1, pegawai.getAsuransiPegawai1());

			asuransiPegawai2 = new Combobox();
			Common.insertComboDanSemua(asuransiPegawai2, new String[] { "nama" }, "keterangan", AsuransiPegawai.class,
					"=Tidak Ada Asuransi=",
					Restrictions.and(Restrictions.or(Restrictions.isNull("jenis"),
							Restrictions.or(Restrictions.eq("jenis", AsuransiPegawai.JENIS_KHUSUS_UNTUK_PEGAWAI),
									Restrictions.eq("jenis", AsuransiPegawai.JENIS_UNTUK_KEDUANYA))),
							Restrictions.eq("aktif", true)));
			Common.selectComboItem(asuransiPegawai2, pegawai.getAsuransiPegawai2());

			asuransiPegawai3 = new Combobox();
			Common.insertComboDanSemua(asuransiPegawai3, new String[] { "nama" }, "keterangan", AsuransiPegawai.class,
					"=Tidak Ada Asuransi=",
					Restrictions.and(Restrictions.or(Restrictions.isNull("jenis"),
							Restrictions.or(Restrictions.eq("jenis", AsuransiPegawai.JENIS_KHUSUS_UNTUK_PEGAWAI),
									Restrictions.eq("jenis", AsuransiPegawai.JENIS_UNTUK_KEDUANYA))),
							Restrictions.eq("aktif", true)));
			Common.selectComboItem(asuransiPegawai3, pegawai.getAsuransiPegawai3());

			asuransiPegawai4 = new Combobox();
			Common.insertComboDanSemua(asuransiPegawai4, new String[] { "nama" }, "keterangan", AsuransiPegawai.class,
					"=Tidak Ada Asuransi=",
					Restrictions.and(Restrictions.or(Restrictions.isNull("jenis"),
							Restrictions.or(Restrictions.eq("jenis", AsuransiPegawai.JENIS_KHUSUS_UNTUK_PEGAWAI),
									Restrictions.eq("jenis", AsuransiPegawai.JENIS_UNTUK_KEDUANYA))),
							Restrictions.eq("aktif", true)));
			Common.selectComboItem(asuransiPegawai4, pegawai.getAsuransiPegawai4());

			nomorAsuransiPegawai1 = new Textbox(pegawai.getNomorAsuransiPegawai1());
			nomorAsuransiPegawai2 = new Textbox(pegawai.getNomorAsuransiPegawai2());
			nomorAsuransiPegawai3 = new Textbox(pegawai.getNomorAsuransiPegawai3());
			nomorAsuransiPegawai4 = new Textbox(pegawai.getNomorAsuransiPegawai4());

			pangkat = new Textbox(pegawai.getPangkat() == null ? "" : pegawai.getPangkat());
			jabatan = new Textbox(pegawai.getJabatan() == null ? "" : pegawai.getJabatan());
			satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
			satuanKerja.setValue(pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama());
			satuanKerja.setAttribute("satuanKerja", pegawai.getSatuanKerja());

			jatahCutiTahunan = new Decimalbox(new BigDecimal(pegawai.getJatahCutiTahunan()));
			usiaPensiun = new Intbox(pegawai.getUsiaPensiun());

			atasanJbt = new Combobox();
			Common.insertComboDanSemua(atasanJbt, "nama", JenisJabatan.class, Restrictions.eq("aktif", true));
			Common.selectComboItem(atasanJbt, pegawai.getAtasan());
			atasanJbt.setReadonly(true);

			atasanPendukung = new Combobox();
			Common.insertComboDanSemua(atasanPendukung, "nama", JenisJabatan.class, Restrictions.eq("aktif", true));
			Common.selectComboItem(atasanPendukung, pegawai.getAtasanPendukung());
			atasanPendukung.setReadonly(true);

			atasanPendukungCadangan = new Combobox();
			Common.insertComboDanSemua(atasanPendukungCadangan, "nama", JenisJabatan.class,
					Restrictions.eq("aktif", true));
			Common.selectComboItem(atasanPendukungCadangan, pegawai.getAtasanPendukungCadangan());
			atasanPendukungCadangan.setReadonly(true);

			atasanlangsung = new AmbilDataPegawaiBanbox(false);
			Pegawai atasan = pegawai.getAtasanlangsung();
			Pegawai atasan2 = pegawai.getAtasanlangsung2();
			Pegawai atasan3 = pegawai.getAtasanlangsung3();

			atasanlangsung.setAttribute("pegawai", atasan);
			atasanlangsung.setValue(atasan == null ? "" : atasan.getNama());

			atasanlangsung2 = new AmbilDataPegawaiBanbox(false);
			atasanlangsung2.setAttribute("pegawai", atasan2);
			atasanlangsung2.setValue(atasan2 == null ? "" : atasan2.getNama());

			atasanlangsung3 = new AmbilDataPegawaiBanbox(false);
			atasanlangsung3.setAttribute("pegawai", atasan3);
			atasanlangsung3.setValue(atasan3 == null ? "" : atasan3.getNama());

			tipePegawai = new Combobox();
			Common.insertCombo(tipePegawai, "nama", "keterangan", TipePegawai.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			tipePegawai.setReadonly(true);
			Common.selectComboItem(tipePegawai, pegawai.getTipePegawai());

			masaKerja = new Combobox();
			Common.insertCombo(masaKerja, "nama", "keterangan", MasaKerja.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			masaKerja.setReadonly(true);
			Common.selectComboItem(masaKerja, pegawai.getMasaKerja());

			unitKerja = new Combobox();
			Common.insertCombo(unitKerja, "nama", "keterangan", UnitKerja.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			unitKerja.setReadonly(true);
			Common.selectComboItem(unitKerja, pegawai.getUnitKerja());

			tipeMasaKerja = new Combobox();
			Common.insertCombo(tipeMasaKerja, "nama", "keterangan", TipeMasaKerja.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			tipeMasaKerja.setReadonly(true);
			Common.selectComboItem(tipeMasaKerja, pegawai.getTipeMasaKerja());

			final EventListener eventListenerGaji = new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {

					pegawai.setTipeMasaKerja((TipeMasaKerja) (tipeMasaKerja.getSelectedItem() == null ? null
							: tipeMasaKerja.getSelectedItem().getValue()));
					pegawai.setTipePegawai((TipePegawai) (tipePegawai.getSelectedItem() == null ? null
							: tipePegawai.getSelectedItem().getValue()));
					pegawai.setUnitKerja((UnitKerja) (unitKerja.getSelectedItem() == null ? null
							: unitKerja.getSelectedItem().getValue()));
					pegawai.setMasaKerja((MasaKerja) (masaKerja.getSelectedItem() == null ? null
							: masaKerja.getSelectedItem().getValue()));

					pegawai.setTanggalmasuk(tanggalmasuk.getValue());
					pegawai.setTanggalkeluar(tanggalkeluar.getValue());
					pegawai.setTanggalMulaiPengalanKerja(tanggalMulaiPengalanKerja.getValue());
					pegawai.setTanggalSampaiPengalanKerja(tanggalSampaiPengalanKerja.getValue());
					pegawai.setTanggalmasukHonorer(tanggalmasukHonorer.getValue());
					pegawai.setTanggalkeluarHonorer(tanggalkeluarHonorer.getValue());
					pegawai.setTanggalmasukSemiTetap(tanggalmasukSemiTetap.getValue());
					pegawai.setTanggalkeluarSemiTetap(tanggalkeluarSemiTetap.getValue());

					masaKerjaTahun.setValue(pegawai.ambilMasaKerjaTahun() + " tahun");
					masaKerjaBulan.setValue(pegawai.ambilMasaKerjaBulan() + " bulan");

					masaKerjaTahunPengalamanKerja.setValue(pegawai.ambilMasaKerjaTahunPengalamanKerja() + " tahun");
					masaKerjaBulanPengalamanKerja.setValue(pegawai.ambilMasaKerjaBulanPengalamanKerja() + " bulan");

					masaKerjaTahunHonorer.setValue(pegawai.ambilMasaKerjaTahunHonorer() + " tahun");
					masaKerjaBulanHonorer.setValue(pegawai.ambilMasaKerjaBulanHonorer() + " bulan");

					masaKerjaTahunSemiTetap.setValue(pegawai.ambilMasaKerjaTahunSemiTetap() + " tahun");
					masaKerjaBulanSemiTetap.setValue(pegawai.ambilMasaKerjaBulanSemiTetap() + " bulan");

					Date sekarang = WaktuUtil.getDate();
					GajiPokok gajiPokok = pegawai.ambilGajiPokok(sekarang);
					Insentif insentif = pegawai.ambilInsentif(sekarang);
					Makan makan = pegawai.ambilMakan(sekarang);
					Transport transport = pegawai.ambilTransport(sekarang);

					GAPOK.setValue(gajiPokok == null ? "" : Common.numberFormat.get().format(gajiPokok.getGaji()));
					INSENTIF.setValue(insentif == null ? "" : Common.numberFormat.get().format(insentif.getInsentif()));
					MAKAN.setValue(makan == null ? "" : Common.numberFormat.get().format(makan.getMakan()));
					TRANSPORT.setValue(transport == null ? "" : Common.numberFormat.get().format(transport.getTransport()));
					LAIN.setValue(gajiPokok == null ? "" : Common.numberFormat.get().format(gajiPokok.getLain()));

					if (GAPOK.getParent() != null) GAPOK.getParent().setVisible(false);
					if (INSENTIF.getParent() != null) INSENTIF.getParent().setVisible(false);
					if (MAKAN.getParent() != null) MAKAN.getParent().setVisible(false);
					if (TRANSPORT.getParent() != null) TRANSPORT.getParent().setVisible(false);
					if (LAIN.getParent() != null) LAIN.getParent().setVisible(false);
				}
			};

			tanggalmasuk.addEventListener("onChange", eventListenerGaji);
			tanggalkeluar.addEventListener("onChange", eventListenerGaji);
			tipeMasaKerja.addEventListener("onChange", eventListenerGaji);
			tipePegawai.addEventListener("onChange", eventListenerGaji);
			unitKerja.addEventListener("onChange", eventListenerGaji);
			tanggalkeluarHonorer.addEventListener("onChange", eventListenerGaji);
			tanggalmasukHonorer.addEventListener("onChange", eventListenerGaji);
			tanggalmasukSemiTetap.addEventListener("onChange", eventListenerGaji);
			tanggalkeluarSemiTetap.addEventListener("onChange", eventListenerGaji);
			tanggalMulaiPengalanKerja.addEventListener("onChange", eventListenerGaji);
			tanggalSampaiPengalanKerja.addEventListener("onChange", eventListenerGaji);

			tab1a.addEventListener(Events.ON_CLICK, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
					if (pegawai == null || pegawai.getId() == null) {
						tab60.setSelected(true);
					} else {
						if (tabpanelUtamaA.getChildren().isEmpty()) {
							boolean admin = Common.getApakahAdmin();
							MyGrid grid = new MyGrid();
							grid.setWidth("100%");
							grid.setParent(Common.tampilanScroll(tabpanelUtamaA));
							grid.setWidth("100%");
							grid.setHeight("100%");

							Columns columns = new Columns();
							columns.setParent(grid);
							MyColumnConfig column = new MyColumnConfig();
							column.setWidth("40%");
							columns.appendChild(column);
							column = new MyColumnConfig();
							columns.appendChild(column);

							Rows rows = new Rows();
							rows.setParent(grid);

							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);

							String statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "tipeMasaKerja");

							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(new Label(pegawai.getTipeMasaKerja() == null ? ""
										: pegawai.getTipeMasaKerja().getNama()));
							} else {
								row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
										? tipeMasaKerja
										: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
														? new MyLabelAgakKecilBold(
																pegawai.getTipeMasaKerja() == null ? ""
																		: pegawai.getTipeMasaKerja().getNama())
														: tipeMasaKerja);
								tipeMasaKerja.setWidth("90%");
							}

							row = new MyFormRow();
							row.setParent(rows);

							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "tanggalMulaiPengalanKerja");
							boolean tampil = row.isVisible();
							Hbox hbox2 = new Hbox();

							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								hbox2 = new Hbox();
								row.appendChild(hbox2);
								hbox2.appendChild(new Label(pegawai.getTanggalMulaiPengalanKerja() == null ? ""
										: Common.dateFormat1.get().format(pegawai.getTanggalMulaiPengalanKerja())));
								hbox2.appendChild(new MyLabelConfig("s.d"));
								hbox2.appendChild(new Label(pegawai.getTanggalSampaiPengalanKerja() == null ? ""
										: Common.dateFormat1.get().format(pegawai.getTanggalSampaiPengalanKerja())));
							} else {
								hbox2 = new Hbox();
								row.appendChild(hbox2);
								hbox2.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
										? tanggalMulaiPengalanKerja
										: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
														? new MyLabelAgakKecilBold(
																pegawai.getTanggalMulaiPengalanKerja() == null ? ""
																		: Common.dateFormat1.get().format(
																				pegawai.getTanggalMulaiPengalanKerja()))
														: tanggalMulaiPengalanKerja);
								tanggalMulaiPengalanKerja.setReadonly(false);
								hbox2.appendChild(new MyLabelConfig("s.d"));
								hbox2.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
										? tanggalSampaiPengalanKerja
										: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
														? new MyLabelAgakKecilBold(
																pegawai.getTanggalSampaiPengalanKerja() == null ? ""
																		: Common.dateFormat1.get().format(pegawai
																				.getTanggalSampaiPengalanKerja()))
														: tanggalSampaiPengalanKerja);
								tanggalSampaiPengalanKerja.setReadonly(false);
							}

							row = new MyFormRow();
							row.setVisible(tampil);
							row.setParent(rows);
							row.appendChild(
									new ais.ui.util.MyLabelConfig("Masa kerja sebagai pengalaman bekerja / paklaring"));
							Hbox hbox = new Hbox();
							row.appendChild(hbox);
							hbox.appendChild(masaKerjaTahunPengalamanKerja);
							hbox.appendChild(new MyLabelConfig(", "));
							hbox.appendChild(masaKerjaBulanPengalamanKerja);

							row = new MyFormRow();
							row.setParent(rows);
							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "tanggalmasukHonorer");
							tampil = row.isVisible();

							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								hbox2 = new Hbox();
								row.appendChild(hbox2);
								hbox2.appendChild(new Label(pegawai.getTanggalmasukHonorer() == null ? ""
										: Common.dateFormat1.get().format(pegawai.getTanggalmasukHonorer())));
								hbox2.appendChild(new MyLabelConfig("s.d"));
								hbox2.appendChild(new Label(pegawai.getTanggalkeluarHonorer() == null ? ""
										: Common.dateFormat1.get().format(pegawai.getTanggalkeluarHonorer())));
							} else {
								hbox2 = new Hbox();
								row.appendChild(hbox2);
								hbox2.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
										? tanggalmasukHonorer
										: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
														? new MyLabelAgakKecilBold(
																pegawai.getTanggalmasukHonorer() == null ? ""
																		: Common.dateFormat1.get().format(
																				pegawai.getTanggalmasukHonorer()))
														: tanggalmasukHonorer);
								tanggalmasukHonorer.setReadonly(false);
								hbox2.appendChild(new MyLabelConfig("s.d"));
								hbox2.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
										? tanggalkeluarHonorer
										: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
														? new MyLabelAgakKecilBold(
																pegawai.getTanggalkeluarHonorer() == null ? ""
																		: Common.dateFormat1.get().format(
																				pegawai.getTanggalkeluarHonorer()))
														: tanggalkeluarHonorer);
								tanggalkeluarHonorer.setReadonly(false);
							}

							row = new MyFormRow();
							row.setVisible(tampil);
							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig("Masa kerja sebagai pegawai honor"));
							hbox = new Hbox();
							row.appendChild(hbox);
							hbox.appendChild(masaKerjaTahunHonorer);
							hbox.appendChild(new MyLabelConfig(", "));
							hbox.appendChild(masaKerjaBulanHonorer);

							row = new MyFormRow();
							row.setParent(rows);
							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "tanggalmasukSemiTetap");
							tampil = row.isVisible();

							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								hbox2 = new Hbox();
								row.appendChild(hbox2);
								hbox2.appendChild(new Label(pegawai.getTanggalmasukSemiTetap() == null ? ""
										: Common.dateFormat1.get().format(pegawai.getTanggalmasukSemiTetap())));
								hbox2.appendChild(new MyLabelConfig("s.d"));
								hbox2.appendChild(new Label(pegawai.getTanggalkeluarSemiTetap() == null ? ""
										: Common.dateFormat1.get().format(pegawai.getTanggalkeluarSemiTetap())));
							} else {
								hbox2 = new Hbox();
								row.appendChild(hbox2);
								hbox2.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
										? tanggalmasukSemiTetap
										: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
														? new MyLabelAgakKecilBold(
																pegawai.getTanggalmasukSemiTetap() == null ? ""
																		: Common.dateFormat1.get().format(
																				pegawai.getTanggalmasukSemiTetap()))
														: tanggalmasukSemiTetap);
								tanggalmasukSemiTetap.setReadonly(false);
								hbox2.appendChild(new MyLabelConfig("s.d"));
								hbox2.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
										? tanggalkeluarSemiTetap
										: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
														? new MyLabelAgakKecilBold(
																pegawai.getTanggalkeluarSemiTetap() == null ? ""
																		: Common.dateFormat1.get().format(
																				pegawai.getTanggalkeluarSemiTetap()))
														: tanggalkeluarSemiTetap);
								tanggalkeluarSemiTetap.setReadonly(false);
							}

							row = new MyFormRow();
							row.setVisible(tampil);
							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig("Masa kerja sebagai pegawai semi tetap"));
							hbox = new Hbox();
							row.appendChild(hbox);
							hbox.appendChild(masaKerjaTahunSemiTetap);
							hbox.appendChild(new MyLabelConfig(", "));
							hbox.appendChild(masaKerjaBulanSemiTetap);

							row = new MyFormRow();
							row.setParent(rows);
							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "tanggalmasuk");
							tampil = row.isVisible();

							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								hbox2 = new Hbox();
								row.appendChild(hbox2);
								hbox2.appendChild(new Label(pegawai.getTanggalmasuk() == null ? ""
										: Common.dateFormat1.get().format(pegawai.getTanggalmasuk())));
								hbox2.appendChild(new MyLabelConfig("s.d"));
								hbox2.appendChild(new Label(pegawai.getTanggalkeluar() == null ? ""
										: Common.dateFormat1.get().format(pegawai.getTanggalkeluar())));
							} else {
								hbox2 = new Hbox();
								row.appendChild(hbox2);
								hbox2.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
										? tanggalmasuk
										: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
														? new MyLabelAgakKecilBold(
																pegawai.getTanggalmasuk() == null ? ""
																		: Common.dateFormat1.get()
																				.format(pegawai.getTanggalmasuk()))
														: tanggalmasuk);
								tanggalmasuk.setReadonly(false);
								hbox2.appendChild(new MyLabelConfig("s.d"));
								hbox2.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
										? tanggalkeluar
										: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
														? new MyLabelAgakKecilBold(
																pegawai.getTanggalkeluar() == null ? ""
																		: Common.dateFormat1.get()
																				.format(pegawai.getTanggalkeluar()))
														: tanggalkeluar);
								tanggalkeluar.setReadonly(false);
							}

							row = new MyFormRow();
							row.setVisible(tampil);
							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig("Masa kerja sebagai pegawai tetap"));
							hbox = new Hbox();
							row.appendChild(hbox);
							hbox.appendChild(masaKerjaTahun);
							hbox.appendChild(new MyLabelConfig(", "));
							hbox.appendChild(masaKerjaBulan);

							EventListener eventListenerJenis = new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {

									TipeMasaKerja masaKerja = (TipeMasaKerja) (tipeMasaKerja.getSelectedItem() == null
											? null
											: tipeMasaKerja.getSelectedItem().getValue());

									tanggalmasuk.setDisabled(!(masaKerja != null && TipeMasaKerja.Tetap != null
											&& masaKerja.getId().equals(TipeMasaKerja.Tetap.getId())));
									tanggalkeluar.setDisabled(!(masaKerja != null && TipeMasaKerja.Tetap != null
											&& masaKerja.getId().equals(TipeMasaKerja.Tetap.getId())));

									tanggalmasukSemiTetap.setDisabled(!(masaKerja != null && TipeMasaKerja.Tetap != null
											&& TipeMasaKerja.Semi_Tetap != null
											&& (masaKerja.getId().equals(TipeMasaKerja.Tetap.getId())
													|| masaKerja.getId().equals(TipeMasaKerja.Semi_Tetap.getId()))));

									tanggalkeluarSemiTetap.setDisabled(!(masaKerja != null
											&& TipeMasaKerja.Tetap != null && TipeMasaKerja.Semi_Tetap != null
											&& (masaKerja.getId().equals(TipeMasaKerja.Tetap.getId())
													|| masaKerja.getId().equals(TipeMasaKerja.Semi_Tetap.getId()))));

									tanggalmasukHonorer.setDisabled(!(masaKerja != null && TipeMasaKerja.Tetap != null
											&& TipeMasaKerja.Honorer != null && TipeMasaKerja.Semi_Tetap != null
											&& (masaKerja.getId().equals(TipeMasaKerja.Tetap.getId())
													|| masaKerja.getId().equals(TipeMasaKerja.Honorer.getId())
													|| masaKerja.getId().equals(TipeMasaKerja.Semi_Tetap.getId()))));

									tanggalkeluarHonorer.setDisabled(!(masaKerja != null && TipeMasaKerja.Tetap != null
											&& TipeMasaKerja.Honorer != null && TipeMasaKerja.Semi_Tetap != null
											&& (masaKerja.getId().equals(TipeMasaKerja.Tetap.getId())
													|| masaKerja.getId().equals(TipeMasaKerja.Honorer.getId())
													|| masaKerja.getId().equals(TipeMasaKerja.Semi_Tetap.getId()))));

								}
							};

							eventListenerJenis.onEvent(null);
							tipeMasaKerja.addEventListener("onChange", eventListenerJenis);
						}

					}
				}
			});

			tab1c.addEventListener(Events.ON_CLICK, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
					if (pegawai == null || pegawai.getId() == null) {
						tab60.setSelected(true);
					} else {
						if (tabpanelUtamaC.getChildren().isEmpty()) {
							boolean admin = Common.getApakahAdmin();
							MyGrid grid = new MyGrid();
							grid.setWidth("100%");
							grid.setParent(Common.tampilanScroll(tabpanelUtamaC));
							grid.setWidth("100%");
							grid.setHeight("100%");

							Columns columns = new Columns();
							columns.setParent(grid);
							MyColumnConfig column = new MyColumnConfig();
							column.setWidth("40%");
							columns.appendChild(column);
							column = new MyColumnConfig();
							columns.appendChild(column);

							Rows rows = new Rows();
							rows.setParent(grid);

							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);

							String statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "asuransiPegawai1");
							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(new Label(pegawai.getAsuransiPegawai1() == null ? ""
										: pegawai.getAsuransiPegawai1().getNama()));
							} else {
								row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
										? asuransiPegawai1
										: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
														? new MyLabelAgakKecilBold(
																pegawai.getAsuransiPegawai1() == null ? ""
																		: pegawai.getAsuransiPegawai1().getNama())
														: asuransiPegawai1);
							}
							asuransiPegawai1.setWidth("90%");

							row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);

							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "nomorAsuransiPegawai1");
							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(new Label(pegawai.getNomorAsuransiPegawai1()));
							} else {
								row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
										? nomorAsuransiPegawai1
										: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
														? new MyLabelAgakKecilBold(pegawai.getNomorAsuransiPegawai1())
														: nomorAsuransiPegawai1);
							}
							nomorAsuransiPegawai1.setWidth("90%");

							row = new MyFormRow();
							row.setParent(rows);

							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "asuransiPegawai2");
							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(new Label(pegawai.getAsuransiPegawai2() == null ? ""
										: pegawai.getAsuransiPegawai2().getNama()));
							} else {
								row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
										? asuransiPegawai2
										: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
														? new MyLabelAgakKecilBold(
																pegawai.getAsuransiPegawai2() == null ? ""
																		: pegawai.getAsuransiPegawai2().getNama())
														: asuransiPegawai2);
							}
							asuransiPegawai2.setWidth("90%");

							row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);

							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "nomorAsuransiPegawai2");
							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(new Label(pegawai.getNomorAsuransiPegawai2()));
							} else {
								row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
										? nomorAsuransiPegawai2
										: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
														? new MyLabelAgakKecilBold(pegawai.getNomorAsuransiPegawai2())
														: nomorAsuransiPegawai2);
							}
							nomorAsuransiPegawai2.setWidth("90%");

							row = new MyFormRow();
							row.setParent(rows);

							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "asuransiPegawai3");
							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(new Label(pegawai.getAsuransiPegawai3() == null ? ""
										: pegawai.getAsuransiPegawai3().getNama()));
							} else {
								row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
										? asuransiPegawai3
										: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
														? new MyLabelAgakKecilBold(
																pegawai.getAsuransiPegawai3() == null ? ""
																		: pegawai.getAsuransiPegawai3().getNama())
														: asuransiPegawai3);
							}
							asuransiPegawai3.setWidth("90%");

							row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);

							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "nomorAsuransiPegawai3");
							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(new Label(pegawai.getNomorAsuransiPegawai3()));
							} else {
								row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
										? nomorAsuransiPegawai3
										: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
														? new MyLabelAgakKecilBold(pegawai.getNomorAsuransiPegawai3())
														: nomorAsuransiPegawai3);
							}
							nomorAsuransiPegawai3.setWidth("90%");

							row = new MyFormRow();
							row.setParent(rows);

							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "asuransiPegawai4");
							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(new Label(pegawai.getAsuransiPegawai4() == null ? ""
										: pegawai.getAsuransiPegawai4().getNama()));
							} else {
								row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
										? asuransiPegawai4
										: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
														? new MyLabelAgakKecilBold(
																pegawai.getAsuransiPegawai4() == null ? ""
																		: pegawai.getAsuransiPegawai4().getNama())
														: asuransiPegawai4);
							}
							asuransiPegawai4.setWidth("90%");

							row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);

							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "nomorAsuransiPegawai4");
							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(new Label(pegawai.getNomorAsuransiPegawai4()));
							} else {
								row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
										? nomorAsuransiPegawai4
										: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
														? new MyLabelAgakKecilBold(pegawai.getNomorAsuransiPegawai4())
														: nomorAsuransiPegawai4);
							}
							nomorAsuransiPegawai4.setWidth("90%");

						}
					}
				}
			});

			tab1d.addEventListener(Events.ON_CLICK, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
					if (pegawai == null || pegawai.getId() == null) {
						tab60.setSelected(true);
					} else {
						if (tabpanelUtamaD.getChildren().isEmpty()) {
							MyInclude iframe = new MyInclude(
									"/pages/master/catatan_pegawai.zul?currPegawai=" + pegawai.getId());
							iframe.setParent(tabpanelUtamaD);
						}
					}
				}
			});

			tab1e.addEventListener(Events.ON_CLICK, new EventListener() {
				@SuppressWarnings({ "deprecation", "unchecked" })
				private void reload() {
					Common.clear(tabpanelUtamaE);

					MyGrid grid = new MyGrid();
					grid.setWidth("100%");
					grid.setParent(Common.tampilanScroll(tabpanelUtamaE));
					grid.setWidth("100%");
					grid.setHeight("100%");

					Columns columns = new Columns();
					columns.setParent(grid);
					MyColumnConfig column = new MyColumnConfig();
					column.setWidth("60%");
					columns.appendChild(column);
					column = new MyColumnConfig();
					columns.appendChild(column);

					Rows rows = new Rows();
					rows.setParent(grid);

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");

					MyToolbarbuttonConfig tambahDokumen;
					row.appendChild(
							tambahDokumen = new MyToolbarbuttonConfig("Tambah Dokumen", "/img/svg/addthis.svg"));
					tambahDokumen.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							final MyWindow addWindow = new MyWindow("Tambah Dokumen", "none", false);
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
							addWindow.setHeight("300px");
							addWindow.setWidth("450px");

							Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
							borderlayout.setParent(addWindow);
							Center center = new Center();
							center.setParent(borderlayout);
							ais.ui.util.ZkCompat.setFlex(center, true);
							MyGrid grid = new MyGrid();
							grid.setWidth("100%");
							grid.setParent(center);
							grid.setWidth("100%");
							grid.setHeight("100%");

							Columns columns = new Columns();
							columns.setParent(grid);

							MyColumnConfig column = new MyColumnConfig();
							column.setParent(columns);
							column.setWidth("40%");

							column = new MyColumnConfig();
							column.setParent(columns);

							Rows rowsTambah = new Rows();
							rowsTambah.setParent(grid);

							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setParent(rowsTambah);
							row.appendChild(new ais.ui.util.MyLabelConfig("Nama Dokumen"));
							String[] lampiran_Pegawai = Common.getKonfigurasi("lampiran_pegawai",
									"Akta Kelahiran;BPJS;Kartu Keluarga;KTP;NPWP;Ijazah S1;Ijazah S2;Ijazah S3;Prestasi")
									.getNilai().split(";");

							final Combobox dokumen = new Combobox();
							for (String s : lampiran_Pegawai) {
								Comboitem comboitem = new Comboitem(s);
								dokumen.appendChild(comboitem);
							}
							row.appendChild(dokumen);
							dokumen.setWidth("95%");

							Common.initKeterangan(rowsTambah,
									"Ketikkan nama dokumen jika tidak tercantum dalam pilihan");

							final MyFormRow rowDokumen = new MyFormRow();
							rowDokumen.setVisible(false);
							rowDokumen.setValign("top");
							rowDokumen.setParent(rowsTambah);
							rowDokumen.appendChild(new ais.ui.util.MyLabelConfig("File Dokumen"));

							dokumen.addEventListener("onOK", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									rowDokumen.setVisible(!dokumen.getValue().trim().isEmpty());
								}
							});

							dokumen.addEventListener("onChange", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									rowDokumen.setVisible(!dokumen.getValue().trim().isEmpty());
								}
							});

							Hbox myHbox = new Hbox();
							myHbox.setParent(rowDokumen);
							myHbox.setHeight("30px");

							Hbox hboxGambar = new Hbox();
							hboxGambar.setParent(myHbox);

							LampiranLain.createDownloadUploadFileLain(hboxGambar, pegawai.getId(),
									"Dokumen_Pegawai_" + Common.getGeneratedBarCode(), "Dokumen", false,
									new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											LampiranLain lainMahasiswaCover = (LampiranLain) arg0.getData();

											Session streamingSession = null;
											Transaction tx = null;
											try {
												streamingSession = StreamingHibernateUtil.getInstance()
														.getSessionFactory().openSession();
												tx = streamingSession.beginTransaction();

												streamingSession.refresh(lainMahasiswaCover);
												lainMahasiswaCover.setRef(pegawai.getId());
												lainMahasiswaCover
														.setJenis("Dokumen_Pegawai_" + dokumen.getValue().trim());

												streamingSession.update(lainMahasiswaCover);
												tx.commit();
											} catch (Exception e) {
												if (tx != null)
													tx.rollback();
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/BiodataPegawaiAction.java:1970");
											} finally {
												cleanupSession(streamingSession);
											}

											Common.createDefaultTimer(new EventListener() {
												@Override
												public void onEvent(Event arg0) throws Exception {
													addWindow.detach();
													reload();
												}
											});
										}
									});

							South south = new South();
							ais.ui.util.ZkCompat.setFlex(south, true);
							south.setParent(borderlayout);

							Toolbar toolbar = new Toolbar();
							toolbar.setParent(south);
							MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
							cancel.setTooltiptext("Tutup");
							cancel.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									addWindow.detach();
								}
							});
							cancel.setParent(toolbar);

							addWindow.setVisible(true);
							addWindow.onModal();
						}
					});

					Session streamingSession = null;
					List<LampiranLain> lampiranLains = new java.util.ArrayList<LampiranLain>();
					try {
						streamingSession = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
						lampiranLains = streamingSession.createCriteria(LampiranLain.class).addOrder(Order.asc("id"))
								.add(Restrictions.eq("ref", pegawai.getId()))
								.add(Restrictions.ilike("jenis", "Dokumen_Pegawai_", MatchMode.START)).list();
					} finally {
						cleanupSession(streamingSession);
					}

					String doksId = "";
					for (final LampiranLain lampiranLain : lampiranLains) {

						doksId += doksId.isEmpty() ? lampiranLain.getId().toString() : "," + lampiranLain.getId();

						row = new MyFormRow();
						row.setValign("top");
						row.setParent(rows);
						row.appendChild(new Label(lampiranLain.getJenis().replaceAll("Dokumen_Pegawai_", "")));

						Hbox hbox = new Hbox();
						row.appendChild(hbox);

						tambahDokumen = new MyToolbarbuttonConfig("Lihat Dokumen", "/img/svg/eye.svg");
						hbox.appendChild(tambahDokumen);

						tambahDokumen.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.display(lampiranLain);
							}
						});

						MyToolbarbuttonConfig hapusDokumen;
						hapusDokumen = new MyToolbarbuttonConfig("Hapus Dokumen", "/img/svg/trash.svg");
						hbox.appendChild(hapusDokumen);

						hapusDokumen.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus dokumen ini? Mohon diperhatikan, dokumen yang telah dihapus tidak dapat dikembalikan. Silakan tekan OK untuk melanjutkan penghapusan atau Batal untuk membatalkan.", "Pertanyaan",
										MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
										new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												int i = Integer.parseInt(event.getData().toString());
												if (i == MyMessageboxConfig.OK) {
													try {
														Session session = null;
														Transaction tx = null;
														try {
															session = StreamingHibernateUtil.getInstance()
																	.getSessionFactory().openSession();
															tx = session.beginTransaction();
															session.delete(lampiranLain);
															tx.commit();
														} catch (Exception e) {
															if (tx != null)
																tx.rollback();
															Common.tampilErrorJikaAdmin(e);
														} finally {
															cleanupSession(session);
														}
														reload();
													} catch (Exception e) {
														Common.tampilErrorJikaAdmin(e);
														MyMessageboxConfig.show(Common.pesan(
																"Mohon maaf, dokumen ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu seluruh data lain yang terkait dengan dokumen ini; (2) pastikan dokumen tidak sedang digunakan; (3) ulangi kembali proses penghapusan.",
																e.getMessage()));
													}
												}
											}
										});
							}
						});
					}

					if (!pegawai.getKarpeg().equalsIgnoreCase(doksId)) {
						pegawai.setKarpeg(doksId);

						Session pSession = null;
						Transaction tx = null;
						try {
							pSession = HibernateUtil.getSessionFactory().openSession();
							tx = pSession.beginTransaction();
							pSession.update(pegawai);
							tx.commit();
						} catch (Exception e) {
							if (tx != null)
								tx.rollback();
						} finally {
							cleanupSession(pSession);
						}
					}

					lampiranLains.clear(); // Optimasi memory
				}

				@Override
				public void onEvent(Event event) throws Exception {
					Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
					if (pegawai == null || pegawai.getId() == null) {
						tab60.setSelected(true);
					} else {
						if (tabpanelUtamaE.getChildren().isEmpty()) {
							reload();
						}
					}
				}
			});

			tab1b.addEventListener(Events.ON_CLICK, new EventListener() {
				@SuppressWarnings("unused")
				@Override
				public void onEvent(Event event) throws Exception {
					Pegawai pegawai = managingPegawai.onSave(event, pegawaiData);
					if (pegawai == null || pegawai.getId() == null) {
						tab60.setSelected(true);
					} else {
						if (tabpanelUtamaB.getChildren().isEmpty()) {
							MyGrid grid = new MyGrid();
							grid.setWidth("100%");
							grid.setParent(Common.tampilanScroll(tabpanelUtamaB));
							grid.setWidth("100%");
							grid.setHeight("100%");

							Columns columns = new Columns();
							columns.setParent(grid);
							MyColumnConfig column = new MyColumnConfig();
							column.setWidth("40%");
							columns.appendChild(column);
							column = new MyColumnConfig();
							columns.appendChild(column);

							Rows rows = new Rows();
							rows.setParent(grid);

							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);

							String statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "tipePegawai");
							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(new Label(
										pegawai.getTipePegawai() == null ? "" : pegawai.getTipePegawai().getNama()));
							} else {
								row.appendChild(tipePegawai);
								tipePegawai.setWidth("90%");
							}

							row = new MyFormRow();
							row.setVisible(false);
							row.setParent(rows);

							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "unitKerja");
							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(new Label(
										pegawai.getUnitKerja() == null ? "" : pegawai.getUnitKerja().getNama()));
							} else {
								row.appendChild(unitKerja);
								unitKerja.setWidth("90%");
							}

							if (unitKerja.getChildren().isEmpty()) {
								row.setVisible(false);
							}

							row = new MyFormRow();
							row.setParent(rows);
							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "masaKerja");
							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(new Label(
										pegawai.getMasaKerja() == null ? "" : pegawai.getMasaKerja().getNama()));
							} else {
								row.appendChild(masaKerja);
								masaKerja.setWidth("90%");
							}

							if (masaKerja.getChildren().isEmpty()) {
								row.setVisible(false);
							}

							Date sekarang = WaktuUtil.getDate();
							List<KenaikanPangkat> kenaikanPangkats = pegawai.ambilKenaikanPangkatData(sekarang);

							for (KenaikanPangkat kenaikanPangkat : kenaikanPangkats) {
								if (kenaikanPangkat.getKenaikanPangkatFungsional()) {
									MyFormRow jabatanfungsionalrow = new MyFormRow();
									jabatanfungsionalrow.setParent(rows);
									jabatanfungsionalrow.appendChild(new MyLabelConfig("Jabatan Fungsional"));

									Label jabatanFungsional;
									jabatanFungsional = new Label();
									jabatanfungsionalrow.appendChild(jabatanFungsional);
									jabatanFungsional.setValue(
											kenaikanPangkat == null || kenaikanPangkat.getJabatanFungsional() == null
													? ""
													: kenaikanPangkat.getJabatanFungsional().getNama());

									MyFormRow jabatanfungsionalrowtgl = new MyFormRow();
									jabatanfungsionalrowtgl.setParent(rows);
									jabatanfungsionalrowtgl.appendChild(new MyLabelConfig("Mulai Menjabat Fungsional"));
									jabatanfungsionalrowtgl.appendChild(
											new Label(kenaikanPangkat == null || kenaikanPangkat.getMulai() == null ? ""
													: Common.dateFormat6.get().format(kenaikanPangkat.getMulai())));
								}
							}

							for (KenaikanPangkat kenaikanPangkat : kenaikanPangkats) {
								if (kenaikanPangkat.getKenaikanPangkatGolongan()) {
									final MyFormRow jabatanstrukturalrow = new MyFormRow();
									jabatanstrukturalrow.setParent(rows);
									jabatanstrukturalrow.appendChild(new MyLabelConfig("Jabatan Struktural"));

									Label jabatanStruktural;
									jabatanStruktural = new Label();
									jabatanstrukturalrow.appendChild(jabatanStruktural);
									jabatanStruktural.setValue(
											kenaikanPangkat == null || kenaikanPangkat.getJabatanStruktural() == null
													? ""
													: kenaikanPangkat.getJabatanStruktural().getNama());
									jabatanStruktural.setWidth("90%");

									MyFormRow jabatanstrukturalrowtgl = new MyFormRow();
									jabatanstrukturalrowtgl.setParent(rows);
									jabatanstrukturalrowtgl.appendChild(new MyLabelConfig("Mulai Menjabat Struktural"));
									jabatanstrukturalrowtgl.appendChild(
											new Label(kenaikanPangkat == null || kenaikanPangkat.getMulai() == null ? ""
													: Common.dateFormat6.get().format(kenaikanPangkat.getMulai())));
								}
							}

							for (KenaikanPangkat kenaikanPangkat : kenaikanPangkats) {
								if (kenaikanPangkat.getJabatan() != null) {
									MyFormRow jabatanrow = new MyFormRow();
									jabatanrow.setParent(rows);
									jabatanrow.appendChild(new MyLabelConfig("Jabatan Lain"));
									jabatanrow.appendChild(new Label(
											kenaikanPangkat == null || kenaikanPangkat.getJabatan() == null ? ""
													: kenaikanPangkat.getJabatan().getNama()));
								}
							}

							KenaikanPangkat kenaikanPangkat = kenaikanPangkats.isEmpty() ? null
									: kenaikanPangkats.get(0);

							row = new MyFormRow();
							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig("Golongan"));
							row.appendChild(
									new Label((kenaikanPangkat == null || kenaikanPangkat.getGolongan() == null ? ""
											: kenaikanPangkat.getGolongan().getNama())));

							row = new MyFormRow();
							row.setVisible(false);
							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig("Gaji Pokok"));
							row.appendChild(GAPOK);

							row = new MyFormRow();
							row.setVisible(false);
							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig("Insentif"));
							row.appendChild(INSENTIF);

							row = new MyFormRow();
							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig("Persen insentif"));

							Session sessionKpi = null;
							try {
								sessionKpi = HibernateUtil.getSessionFactory().openSession();
								PenilaianKpi penilaianKpiData = PenilaianKpi.hitungKpi(sessionKpi, pegawai, sekarang);
								row.appendChild(new MyLabelBold(Common.numberFormat.get()
										.format(penilaianKpiData == null ? 0.0 : penilaianKpiData.getPersen()) + "%"
										+ (penilaianKpiData == null ? " (Belum dinilai/belum disetujui)"
												: " Berlaku " + penilaianKpiData.getTa())));
							} finally {
								cleanupSession(sessionKpi);
							}

							row = new MyFormRow();
							row.setVisible(false);
							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig("Makan"));
							row.appendChild(MAKAN);

							row = new MyFormRow();
							row.setVisible(false);
							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig("Transportasi"));
							row.appendChild(TRANSPORT);

							row = new MyFormRow();
							row.setVisible(false);
							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig("Lain-lain"));
							row.appendChild(LAIN);

							if (false) {
								row = new MyFormRow();
								row.setParent(rows);
								row.appendChild(new ais.ui.util.MyLabelConfig("Tunjangan Kinerja"));
								if (tbmuser != null && tbmuser.getPegawai() != null
										&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
									row.appendChild(
											new Label(Common.numberFormat.get().format(pegawai.getTunjanganKinerja())));
								} else {
									row.appendChild(tunjanganKinerja);
								}
								tunjanganKinerja.setWidth("90%");
							}

							row = new MyFormRow();
							row.setParent(rows);

							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "ptkpPegawai");
							boolean tampil = row.isVisible();
							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(new Label(
										pegawai.getPtkpPegawai() == null ? "" : pegawai.getPtkpPegawai().getNama()));
							} else {
								row.appendChild(ptkpPegawai);
							}
							ptkpPegawai.setWidth("90%");

							row = new MyFormRow();
							row.setParent(rows);
							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "pangkat");
							tampil = row.isVisible();

							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(new Label(pegawai.getPangkat()));
							} else {
								row.appendChild(pangkat);
								pangkat.setWidth("90%");
							}

							row = new MyFormRow();
							row.setParent(rows);

							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "jabatan");
							tampil = row.isVisible();

							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(new Label(pegawai.getJabatan()));
							} else {
								row.appendChild(jabatan);
								jabatan.setRows(2);
								jabatan.setWidth("90%");
							}

							row = new MyFormRow();
							row.setParent(rows);

							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "satuanKerja");
							tampil = row.isVisible();

							SatuanKerja s = SatuanKerjaPegawai.ambilSatuanKerja(pegawai);
							if (s != null && s.getId() != null) {
								row.appendChild(new Label(s.getNama()));
							} else if (pegawai != null && pegawai.getDosen() != null
									&& pegawai.getDosen().getJurusan() != null
									&& pegawai.getDosen().getJurusan().getSatuanKerja() != null
									&& pegawai.getDosen().getJurusan().getDosenHarusPakaiSatuanKerja()) {
								row.appendChild(new Label(
										pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama()));
							} else if (pegawai != null && pegawai.getDosen() != null
									&& pegawai.getDosen().getFakultas() != null
									&& pegawai.getDosen().getFakultas().getSatuanKerja() != null
									&& pegawai.getDosen().getFakultas().getDosenHarusPakaiSatuanKerja()) {
								row.appendChild(new Label(
										pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama()));
							} else if (pegawai != null && pegawai.getDosen() != null
									&& pegawai.getDosen().getPerguruanTinggi() != null
									&& pegawai.getDosen().getPerguruanTinggi().getSatuanKerja() != null
									&& pegawai.getDosen().getPerguruanTinggi().getDosenHarusPakaiSatuanKerja()) {
								row.appendChild(new Label(
										pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama()));
							} else if (pegawai != null && pegawai.getGuru() != null
									&& pegawai.getGuru().getSekolah() != null
									&& pegawai.getGuru().getSekolah().getSatuanKerja() != null
									&& pegawai.getGuru().getSekolah().getGuruHarusPakaiSatuanKerja()) {
								row.appendChild(new Label(
										pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama()));
							} else if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(new Label(
										pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama()));
							} else {
								row.appendChild(satuanKerja);
								satuanKerja.setWidth("90%");
							}

							row = new MyFormRow();
							row.setParent(rows);

							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "jatahCutiTahunan");
							tampil = row.isVisible();

							JatahCuti jatahCuti = pegawai.ambilJatahCuti();

							if (jatahCuti != null || (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId()))) {
								row.appendChild(new Label(pegawai.getJatahCutiTahunan() == null ? ""
										: pegawai.getJatahCutiTahunan() + " hari"));
							} else {
								row.appendChild(jatahCutiTahunan);
								jatahCutiTahunan.setWidth("90%");
							}

							row = new MyFormRow();
							row.setParent(rows);

							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "usiaPensiun");
							tampil = row.isVisible();

							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(new Label(
										pegawai.getUsiaPensiun() == null ? "" : pegawai.getUsiaPensiun() + " tahun"));
							} else {
								row.appendChild(usiaPensiun);
								usiaPensiun.setWidth("90%");
							}

							Sekolah sk = SekolahUtil.getSekolah();
							Tbmuser tbmuser = Common.getCurrentUser();
							boolean[] ptYa = Common.chekPtAtauSekolah();
							boolean pt = ptYa[0];
							boolean ya = ptYa[1];

							tendikSekolah = null;
							tendikJurusan = null;
							tendikFakultas = null;

							if (pegawai.getDosen() == null) {
								row = new MyFormRow();
								row.setParent(rows);

								statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "tendikFakultas");

								if (ya) {
									row.setVisible(false);
								}
								tampil = row.isVisible();

								if (tbmuser != null && tbmuser.getPegawai() != null
										&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
									row.appendChild(new Label(pegawai.getTendikFakultas() == null ? ""
											: pegawai.getTendikFakultas().getNama()));
								} else {
									row.appendChild(tendikFakultas = new Combobox());
									tendikFakultas.setWidth("90%");
								}

								row = new MyFormRow();
								row.setParent(rows);

								statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "tendikJurusan");

								if (ya) {
									row.setVisible(false);
								}
								tampil = row.isVisible();

								if (tbmuser != null && tbmuser.getPegawai() != null
										&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
									row.appendChild(new Label(pegawai.getTendikJurusan() == null ? ""
											: pegawai.getTendikJurusan().getNama()));
								} else {
									row.appendChild(tendikJurusan = new Combobox());
									tendikJurusan.setWidth("90%");
								}

								if (tendikFakultas != null && tendikJurusan != null) {
									Common.initFakultasDanJurusan(tendikFakultas, tendikJurusan, null, null);

									if (tendikFakultas.getSelectedItem() != null
											&& tendikFakultas.getSelectedItem().getValue() != null) {
										Common.insertCombo(tendikJurusan, new String[] { "nama", "kodeEpsbed" },
												"jenjang", Jurusan.class,
												Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)),
												CommonSearchFilterHelper.eqSelectedWithId("fakultas", tendikFakultas, false));
									}

									Common.selectComboItem(tendikFakultas, pegawai.getTendikFakultas());
									Common.selectComboItem(tendikJurusan, pegawai.getTendikJurusan());
								}
							}

							if (pegawai.getGuru() == null) {
								row = new MyFormRow();
								row.setParent(rows);

								statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "tendikSekolah");

								if (ya) {
									row.setVisible(false);
								}
								tampil = row.isVisible();

								if (tbmuser != null && tbmuser.getPegawai() != null
										&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
									row.appendChild(new Label(pegawai.getTendikSekolah() == null ? ""
											: pegawai.getTendikSekolah().getNama()));
								} else {
									row.appendChild(tendikSekolah = new Combobox());
									tendikSekolah.setWidth("90%");
								}
								if (tendikSekolah != null) {
									Common.insertCombo(tendikSekolah, new String[] { "nama" }, Sekolah.class,
											Restrictions.and(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)),
											sk != null && sk.getId() != null ? Restrictions.eq("id", sk.getId())
													: Restrictions.sqlRestriction("true"));
									Common.selectComboItem(tendikSekolah, pegawai.getTendikSekolah());
								}
							}

							row = new MyFormRow();
							row.setParent(rows);

							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "atasan");

							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(
										new Label(pegawai.getAtasan() == null ? "" : pegawai.getAtasan().getNama()));
							} else {
								row.appendChild(Common.getApakahAdmin()
										&& statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												? atasanJbt
												: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
														|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
																? new MyLabelAgakKecilBold(
																		pegawai.getAtasan() == null ? ""
																				: pegawai.getAtasan().getNama())
																: atasanJbt);
								atasanJbt.setWidth("90%");
							}

							row = new MyFormRow();
							row.setParent(rows);

							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "atasanPendukung");

							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(new Label(pegawai.getAtasanPendukung() == null ? ""
										: pegawai.getAtasanPendukung().getNama()));
							} else {
								row.appendChild(Common.getApakahAdmin()
										&& statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												? atasanPendukung
												: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
														|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
																? new MyLabelAgakKecilBold(
																		pegawai.getAtasanPendukung() == null ? ""
																				: pegawai.getAtasanPendukung()
																						.getNama())
																: atasanPendukung);
								atasanPendukung.setWidth("90%");
							}

							row = new MyFormRow();
							row.setParent(rows);

							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "atasanPendukungCadangan");

							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
								row.appendChild(new Label(pegawai.getAtasanPendukungCadangan() == null ? ""
										: pegawai.getAtasanPendukungCadangan().getNama()));
							} else {
								row.appendChild(Common.getApakahAdmin()
										&& statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												? atasanPendukungCadangan
												: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
														|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
																? new MyLabelAgakKecilBold(
																		pegawai.getAtasanPendukungCadangan() == null
																				? ""
																				: pegawai.getAtasanPendukungCadangan()
																						.getNama())
																: atasanPendukungCadangan);
								atasanPendukungCadangan.setWidth("90%");
							}

							row = new MyFormRow();
							row.setParent(rows);
							statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "atasanlangsung");

							Vbox atasanVbox = new Vbox();
							atasanVbox.setWidth("90%");
							atasanVbox.setParent(row);

							if (tbmuser != null && tbmuser.getPegawai() != null
									&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {

								atasanVbox.appendChild(new Label(pegawai.getAtasanlangsung() == null ? ""
										: pegawai.getAtasanlangsung().getNama()));
								atasanVbox.appendChild(new Label(pegawai.getAtasanlangsung2() == null ? ""
										: pegawai.getAtasanlangsung2().getNama()));
								atasanVbox.appendChild(new Label(pegawai.getAtasanlangsung3() == null ? ""
										: pegawai.getAtasanlangsung3().getNama()));

							} else {

								atasanVbox.appendChild(Common.getApakahAdmin()
										&& statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												? atasanlangsung
												: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
														|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
																? new MyLabelAgakKecilBold(
																		pegawai.getAtasanlangsung() == null ? ""
																				: pegawai.getAtasanlangsung().getNama())
																: atasanlangsung);

								atasanlangsung.setWidth("90%");
								atasanlangsung.setReadonly(true);

								atasanVbox.appendChild(Common.getApakahAdmin()
										&& statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												? atasanlangsung2
												: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
														|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
																? new MyLabelAgakKecilBold(
																		pegawai.getAtasanlangsung2() == null ? ""
																				: pegawai.getAtasanlangsung2()
																						.getNama())
																: atasanlangsung2);
								atasanlangsung2.setWidth("90%");
								atasanlangsung2.setReadonly(true);

								atasanVbox.appendChild(Common.getApakahAdmin()
										&& statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
												? atasanlangsung3
												: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
														|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
																? new MyLabelAgakKecilBold(
																		pegawai.getAtasanlangsung2() == null ? ""
																				: pegawai.getAtasanlangsung2()
																						.getNama())
																: atasanlangsung3);
								atasanlangsung3.setWidth("90%");
								atasanlangsung3.setReadonly(true);
							}

							kenaikanPangkats.clear(); // Optimasi memori
							eventListenerGaji.onEvent(null);
						}
					}
				}
			});

			tab61.addEventListener(Events.ON_CLICK, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (tabpanelSlipGaji.getChildren().isEmpty()) {
						if (pegawai == null || pegawai.getId() == null) {
							tab60.setSelected(true);
						} else {
							LaporanSlipGajiRealPegawaiPerOrang gajiRealPegawaiPerOrang = new LaporanSlipGajiRealPegawaiPerOrang(
									pegawai, null, null);
							gajiRealPegawaiPerOrang.setHeight("100%");
							gajiRealPegawaiPerOrang.setWidth("100%");
							tabpanelSlipGaji.appendChild(gajiRealPegawaiPerOrang);
						}
					}
				}
			});

			tab62.addEventListener(Events.ON_CLICK, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (tabpanelSlipPerkiraan.getChildren().isEmpty()) {
						if (pegawai == null || pegawai.getId() == null) {
							tab60.setSelected(true);
						} else {
							LaporanSlipGajiPegawaiPerOrang gajiRealPegawaiPerOrang = new LaporanSlipGajiPegawaiPerOrang(
									pegawai);
							gajiRealPegawaiPerOrang.setHeight("100%");
							gajiRealPegawaiPerOrang.setWidth("100%");
							tabpanelSlipPerkiraan.appendChild(gajiRealPegawaiPerOrang);
						}
					}
				}
			});

			ais.ui.util.MyButtonTabbox.gantiTabboxNative(tabbox, new int[] { 1 });

			West west = new West();
			west.setStyle("border:0px;");
			ais.ui.util.ZkCompat.setFlex(west, true);
			west.setWidth(tampilSave ? "270px" : "0px");
			west.setParent(borderlayout);

			fotoPegawai = null;
			if (pegawai.getDosen() != null && pegawai.getDosen().getAktif()) {
				Common.createDownloadUploadFoto(west, pegawai.getDosen(), FotoDosen.class, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
					}
				}, false);
			} else if (pegawai.getGuru() != null && pegawai.getGuru().getAktif()) {
				Common.createDownloadUploadFoto(west, pegawai.getGuru(), FotoGuru.class, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
					}
				}, false);
			} else {
				Common.createDownloadUploadFoto(west, pegawai, FotoPegawai.class, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						fotoPegawai = (FotoPegawai) arg0.getData();
					}
				}, true);
			}

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);
			MyColumnConfig column = new MyColumnConfig();
			column.setWidth("35%");
			columns.appendChild(column);
			column = new MyColumnConfig();
			columns.appendChild(column);

			Rows rows = new Rows();
			rows.setParent(grid);

			boolean admin = Common.getApakahAdmin();

			MyFormRow row = new MyFormRow();
			row.setParent(rows);
			String statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "mycode",
					labelMycode = new MyLabelConfig(pegawai.getMycode()));
			mycode = new Textbox(pegawai.getMycode() == null ? "" : pegawai.getMycode());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? mycode
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getMycode())
									: mycode);
			mycode.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "code",
					labelCode = new MyLabelConfig(pegawai.getCode()));
			code = new Textbox(pegawai.getCode());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? code
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getCode())
									: code);
			code.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "idfinger");
			idfinger = new Textbox(pegawai.getIdfinger());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? idfinger
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getIdfinger())
									: idfinger);
			idfinger.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "ktp");
			ktp = new Textbox(pegawai.getKtp());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? ktp
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getKtp())
									: ktp);
			ktp.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "nama");
			nama = new Textbox(pegawai.getNama() == null ? "" : pegawai.getNama());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? nama
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getNama())
									: nama);
			nama.setWidth("90%");

			ttd = null;
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tanda Tangan (PNG) "));
			Hbox hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, pegawai.getId(), LampiranLain.TTD_PEGAWAI, "Tanda Tangan",
					false, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							ttd = (LampiranLain) arg0.getData();
						}
					}, null, false, false, false, true, null, false, false);
			hbox.setParent(row);

			if (Common.bolehKonfigurasi("tampilkan_link_login_oleh_admin_di_data_pegawai")) {
				if (pegawai.getId() != null && Common.getApakahAdmin()) {
					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Link"));

					final A a = new A("Tampilkan Link");
					a.setHref("");
					row.appendChild(a);

					a.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							Session sessionLink = null;
							try {
								sessionLink = HibernateUtil.getSessionFactory().openSession();
								String userId = (String) sessionLink.createCriteria(Tbmuser.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.setProjection(Projections.property("userId"))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("pegawai", pegawai))
										.addOrder(Order.desc("tanggal_dirubah")).setMaxResults(1).uniqueResult();

								if (userId == null) {
									MyMessageboxConfig.show("Mohon maaf, data / akun pengguna untuk pegawai ini tidak ditemukan sehingga proses tidak dapat dilanjutkan. Langkah yang dapat dilakukan: (1) pastikan pegawai yang bersangkutan sudah memiliki akun pengguna; (2) buatkan atau kaitkan akun pengguna melalui menu Pengguna; (3) ulangi kembali proses ini.",
											"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
									return;
								}
								String code = Common.getRequestHostWithProtocol() + "/m?q=" + URLEncoder.encode(
										Common.desEncrypter.get().encrypt(userId + "-user-abcdefghijklmnopqrstuvwxyz"),
										"UTF-8");
								a.setLabel(code);
								a.setHref(Common.getRequestHostWithProtocol() + "/logoff?param="
										+ URLEncoder.encode(code, "UTF-8"));
							} finally {
								cleanupSession(sessionLink);
							}
						}
					});

					Common.initKeterangan(rows, "Link ini bisa digunakan untuk login tanpa menggunakan password");
				}
			}

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "statusPegawai");
			statusPegawai = new Combobox();
			Common.insertCombo(statusPegawai, "nama", StatusPegawai.class, Restrictions.eq("aktif", true));
			Common.selectComboItem(statusPegawai, pegawai.getStatusPegawai());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? statusPegawai
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY)) ? new MyLabelAgakKecilBold(
									pegawai.getStatusPegawai() == null ? "" : pegawai.getStatusPegawai().getNama())
									: statusPegawai);
			statusPegawai.setWidth("90%");
			statusPegawai.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "statusKepegawaian");
			statusKepegawaian = new Combobox();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? statusKepegawaian
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getStatusKepegawaian() == null ? ""
											: pegawai.getStatusKepegawaian().getNama())
									: statusKepegawaian);
			Common.insertCombo(statusKepegawaian, new String[] { "nama" }, StatusKepegawaian.class,
					Restrictions.ne("nama", ""),
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.selectComboItem(statusKepegawaian, pegawai.getStatusKepegawaian());
			statusKepegawaian.setWidth("90%");
			statusKepegawaian.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelDosenTampil(row, "ikatanKerjaDosen");
			ikatanKerjaDosen = new Combobox();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? ikatanKerjaDosen
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getIkatanKerjaDosen() == null ? ""
											: pegawai.getIkatanKerjaDosen().getNama())
									: ikatanKerjaDosen);
			Common.insertComboDanSemua(ikatanKerjaDosen, new String[] { "nama", "feeder" }, "keterangan",
					IkatanKerjaDosen.class, "Ikatan Kerja Belum Ditentulan", Restrictions.ne("nama", ""),
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.selectComboItem(ikatanKerjaDosen, pegawai.getIkatanKerjaDosen());
			ikatanKerjaDosen.setWidth("90%");
			ikatanKerjaDosen.setReadonly(true);

			row = new MyFormRow();
			row.setVisible(false);
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "sertifikasi");
			sertifikasi = new MyCheckboxConfig();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? sertifikasi
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getSertifikasi() ? "Ya" : "Yidak")
									: sertifikasi);
			sertifikasi.setChecked(pegawai.getSertifikasi());

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new Label());
			Vbox myVbox = new Vbox();
			myVbox.setParent(row);

			lainMahasiswa = null;

			hbox = new Hbox();
			hbox.setParent(myVbox);
			LampiranLain.createDownloadUploadFileLain(hbox, pegawai.getId(), LampiranLain.SERTIFIKASI_PEGAWAI,
					LampiranLain.SERTIFIKASI_PEGAWAI, false, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							lainMahasiswa = (LampiranLain) arg0.getData();
						}
					}, null, false, false, false, true);

			row = new MyFormRow();
			row.setVisible(Common.bolehKonfigurasi("tampil_pilihan_dosen_pada_data_pegawai"));
			row.setParent(rows);

			dosen = new AmbilDataDosenBanbox();
			checkboxConfigDosen = new MyCheckboxConfig("Pilih jika pegawai ini sebagai dosen");
			checkboxConfigDosen.setChecked(pegawai.getDosen() != null);

			if (tbmuser != null && tbmuser.ambilDosen() != null && pegawai.getDosen() != null
					&& tbmuser.getDosen().getId().equals(pegawai.getDosen().getId())) {
				row.appendChild(new ais.ui.util.MyLabelConfig("Apakah sebagai dosen?"));
				row.appendChild(new MyLabelConfig("Ya"));
			} else {
				row.appendChild(checkboxConfigDosen);
				final MyToolbarbuttonConfig myToolbarbuttonConfig = new MyToolbarbuttonConfig("Data Dosen",
						"/img/svg/user-circle.svg");
				myToolbarbuttonConfig.setDisabled(!checkboxConfigDosen.isChecked());
				checkboxConfigDosen.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						myToolbarbuttonConfig.setDisabled(!checkboxConfigDosen.isChecked());
					}
				});
				row.appendChild(myToolbarbuttonConfig);
				myToolbarbuttonConfig.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {

						if (nama.getValue().trim().isEmpty()) {
							MyMessageboxConfig.show(
									"Mohon Bapak/Ibu melengkapi isian Nama terlebih dahulu. Langkah yang dapat dilakukan: (1) isi kolom Nama; (2) pastikan nama tidak dikosongkan; (3) simpan kembali data.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}
						if (tanggallahir.getValue() == null) {
							MyMessageboxConfig.show(
									"Mohon Bapak/Ibu melengkapi isian Tanggal Lahir terlebih dahulu. Langkah yang dapat dilakukan: (1) isi kolom Tanggal Lahir; (2) pastikan tanggal lahir telah dipilih; (3) simpan kembali data.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						Dosen dosen = pegawai.getDosen();

						if (dosen == null || dosen.getId() == null) {
							Session sessionDosen = null;
							try {
								sessionDosen = HibernateUtil.getSessionFactory().openSession();
								dosen = (Dosen) ConstantValues.simpleObject(sessionDosen.createCriteria(Dosen.class)
										.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
										.add(Restrictions.eq("tanggallahir", tanggallahir.getValue())).setMaxResults(1),
										Dosen.class);
							} finally {
								cleanupSession(sessionDosen);
							}
						}

						if (dosen == null || dosen.getId() == null) {
							dosen = new Dosen();
							try {
								BeanUtilsBean.getInstance().copyProperties(dosen, pegawai);
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/BiodataPegawaiAction.java:3011");
							}
							dosen.setId(null);
							dosen.setMycode(mycode.getValue().trim());
							dosen.setCode(code.getValue().trim());
							dosen.setNama(nama.getValue().trim());
							dosen.setTempatlahir(tempatlahir.getValue().trim());
							dosen.setTanggallahir(tanggallahir.getValue());
						}

						if (!mycode.getValue().trim().isEmpty()) {
							dosen.setMycode(mycode.getValue().trim());
						}
						if (!code.getValue().trim().isEmpty()) {
							dosen.setCode(code.getValue().trim());
						}
						if (!nama.getValue().trim().isEmpty()) {
							dosen.setNama(nama.getValue().trim());
						}

						BiodataDosenAction biodataDosenAction = new BiodataDosenAction(dosen, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								Dosen d = (Dosen) arg0.getData();
								if (d != null && d.getId() != null) {
									if (d.getPegawaiId() != null) {
										pegawai.setId(d.getPegawaiId());
										ManagingPegawai.this.pegawai.setId(d.getPegawaiId());
									}
									pegawai.setDosen(d);
									ManagingPegawai.this.dosen.setAttribute("dosen", d);
									ManagingPegawai.this.dosen.setAttribute("myValue", d);
									ManagingPegawai.this.dosen.setValue(d.getNama());
								}
							}
						}, false, false);
						biodataDosenAction.setHeight("99%");
						biodataDosenAction.setWidth("90%");
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(biodataDosenAction);
						biodataDosenAction.setVisible(true);
						biodataDosenAction.onModal();
					}
				});
			}

			dosen.setValue(pegawai.getDosen() == null ? "" : pegawai.getDosen().getNama());
			dosen.setAttribute("myValue", pegawai.getDosen());
			dosen.setAttribute("dosen", pegawai.getDosen());
			dosen.setWidth("90%");
			dosen.setReadonly(true);

			row = new MyFormRow();
			row.setVisible(Common.bolehKonfigurasi("tampil_pilihan_guru_pada_data_pegawai"));
			row.setParent(rows);

			guru = new AmbilDataGuruBanbox();
			checkboxConfigGuru = new MyCheckboxConfig("Pilih jika pegawai ini sebagai guru");
			checkboxConfigGuru.setChecked(pegawai.getGuru() != null);

			if (tbmuser != null && tbmuser.ambilGuru() != null && pegawai.getGuru() != null
					&& tbmuser.getGuru().getId().equals(pegawai.getGuru().getId())) {
				row.appendChild(new ais.ui.util.MyLabelConfig("Apakah sebagai guru?"));
				row.appendChild(new MyLabelConfig("Ya"));
			} else {
				row.appendChild(checkboxConfigGuru);
				final MyToolbarbuttonConfig myToolbarbuttonConfig = new MyToolbarbuttonConfig("Data Guru",
						"/img/svg/user-circle.svg");
				myToolbarbuttonConfig.setDisabled(!checkboxConfigGuru.isChecked());
				checkboxConfigGuru.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						myToolbarbuttonConfig.setDisabled(!checkboxConfigGuru.isChecked());
					}
				});
				row.appendChild(myToolbarbuttonConfig);
				myToolbarbuttonConfig.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {

						if (nama.getValue().trim().isEmpty()) {
							MyMessageboxConfig.show(
									"Mohon Bapak/Ibu melengkapi isian Nama terlebih dahulu. Langkah yang dapat dilakukan: (1) isi kolom Nama; (2) pastikan nama tidak dikosongkan; (3) simpan kembali data.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}
						if (tanggallahir.getValue() == null) {
							MyMessageboxConfig.show(
									"Mohon Bapak/Ibu melengkapi isian Tanggal Lahir terlebih dahulu. Langkah yang dapat dilakukan: (1) isi kolom Tanggal Lahir; (2) pastikan tanggal lahir telah dipilih; (3) simpan kembali data.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						Guru guru = pegawai.getGuru();

						if (guru == null || guru.getId() == null) {
							Session sessionGuru = null;
							try {
								sessionGuru = HibernateUtil.getSessionFactory().openSession();
								guru = (Guru) ConstantValues.simpleObject(sessionGuru.createCriteria(Guru.class)
										.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
										.add(Restrictions.eq("tanggalLahir", tanggallahir.getValue())).setMaxResults(1),
										Guru.class);
							} finally {
								cleanupSession(sessionGuru);
							}
						}

						if (guru == null || guru.getId() == null) {
							guru = new Guru();
							try {
								BeanUtilsBean.getInstance().copyProperties(guru, pegawai);
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/BiodataPegawaiAction.java:3123");
							}
							guru.setKode(code.getValue().trim());
							guru.setNamaGuru(nama.getValue().trim());
							guru.setTempatLahir(tempatlahir.getValue().trim());
							guru.setTanggalLahir(tanggallahir.getValue());
							guru.setId(null);
						}

						GuruAction.onAddExternal(arg0, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								Guru guru = (Guru) arg0.getData();
								if (guru != null && guru.getId() != null) {
									pegawai.setGuru(guru);
									if (guru.getPegawaiId() != null) {
										pegawai.setId(guru.getPegawaiId());
										ManagingPegawai.this.pegawai.setId(guru.getPegawaiId());
									}
									ManagingPegawai.this.guru.setAttribute("guru", guru);
									ManagingPegawai.this.guru.setAttribute("myValue", guru);
									ManagingPegawai.this.guru.setValue(guru.getNama());
								}
							}
						}, guru, false);
					}
				});
			}

			guru.setValue(pegawai.getGuru() == null ? "" : pegawai.getGuru().getNama());
			guru.setAttribute("myValue", pegawai.getGuru());
			guru.setAttribute("guru", pegawai.getGuru());
			guru.setWidth("90%");
			guru.setReadonly(true);

			row = new MyFormRow();
			row.setVisible(pegawai.getId() != null
					&& Common.bolehKonfigurasi("tampil_pilihan_user_pada_data_pegawai"));
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Akun pegawai"));

			MyToolbarbuttonConfig myToolbarbuttonConfig = new MyToolbarbuttonConfig("Akun/Login Pengguna",
					"/img/svg/user-circle.svg");
			row.appendChild(myToolbarbuttonConfig);
			myToolbarbuttonConfig.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Session sessionUser = null;
					try {
						sessionUser = HibernateUtil.getSessionFactory().openSession();
						Tbmuser tbmuser = (Tbmuser) ConstantValues.simpleObject(
								sessionUser.createCriteria(Tbmuser.class).add(Restrictions.eq("pegawai", pegawai))
										.addOrder(Order.desc("tanggal_dirubah")).setMaxResults(1),
								Tbmuser.class);
						if (tbmuser == null || tbmuser.getUserId() == null) {
							tbmuser = new Tbmuser();
						}
						tbmuser.setPegawai(pegawai);
						TbmuserAction.onAddExternal(null, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
							}
						}, tbmuser, pegawai);
					} finally {
						cleanupSession(sessionUser);
					}
				}
			});

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "pendidikan");
			pendidikan = new Combobox();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? pendidikan
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(
											pegawai.getPendidikan() == null ? "" : pegawai.getPendidikan().getNama())
									: pendidikan);
			Common.insertCombo(pendidikan, "nama", Pendidikan.class);
			Common.selectComboItem(pendidikan, pegawai.getPendidikan());
			pendidikan.setWidth("90%");
			pendidikan.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "jenisTenagaKependidikan");
			jenisTenagaKependidikan = new Combobox();
			Common.insertCombo(jenisTenagaKependidikan, "nama", JenisTenagaKependidikan.class);
			Common.selectComboItem(jenisTenagaKependidikan, pegawai.getJenisTenagaKependidikan());
			row.appendChild(
					admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? jenisTenagaKependidikan
							: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
									|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
											? new MyLabelAgakKecilBold(pegawai.getJenisTenagaKependidikan() == null ? ""
													: pegawai.getJenisTenagaKependidikan().getNama())
											: jenisTenagaKependidikan);
			jenisTenagaKependidikan.setWidth("90%");
			jenisTenagaKependidikan.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "statusPerkawinan");
			Common.selectComboItem(statusPerkawinan, pegawai.getStatusPerkawinan());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? statusPerkawinan
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getStatusPerkawinan())
									: statusPerkawinan);
			statusPerkawinan.setWidth("90%");
			statusPerkawinan.setDisabled(pegawai.getDosen() != null || pegawai.getGuru() != null);
			statusPerkawinan.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "kelamin");
			Common.selectComboItem(kelamin, pegawai.getKelamin());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? kelamin
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getKelamin())
									: kelamin);
			kelamin.setWidth("90%");
			kelamin.setDisabled(pegawai.getDosen() != null || pegawai.getGuru() != null);
			kelamin.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "tempatlahir");
			tempatlahir = new Textbox(pegawai.getTempatlahir() == null ? "" : pegawai.getTempatlahir());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? tempatlahir
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getTempatlahir())
									: tempatlahir);
			tempatlahir.setWidth("90%");
			tempatlahir.setDisabled(pegawai.getDosen() != null || pegawai.getGuru() != null);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "tanggallahir");
			tanggallahir = new MyDatebox(
					pegawai.getTanggallahir() == null ? ais.ui.util.WaktuUtil.getDate() : pegawai.getTanggallahir());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? tanggallahir
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getTanggallahir() == null ? ""
											: Common.dateFormat2.get().format(pegawai.getTanggallahir()))
									: tanggallahir);
			tanggallahir.setDisabled(pegawai.getDosen() != null || pegawai.getGuru() != null);

			row = new MyFormRow();
			row.setVisible(ConstantValues.penggunaanLabelBahasa);
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "bahasa");
			bahasa = new Combobox();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? bahasa
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getBahasa())
									: bahasa);

			Comboitem comboitemBahasa = new Comboitem();
			comboitemBahasa.setLabel(Tbmuser.INDONESIA);
			comboitemBahasa.setValue(Tbmuser.INDONESIA);
			bahasa.appendChild(comboitemBahasa);

			comboitemBahasa = new MyComboitemConfig();
			comboitemBahasa.setLabel(Tbmuser.ENGLISH);
			comboitemBahasa.setValue(Tbmuser.ENGLISH);
			bahasa.appendChild(comboitemBahasa);

			comboitemBahasa = new MyComboitemConfig();
			comboitemBahasa.setLabel(Tbmuser.ARAB);
			comboitemBahasa.setValue(Tbmuser.ARAB);
			bahasa.appendChild(comboitemBahasa);
			bahasa.setWidth("90%");
			bahasa.setReadonly(true);

			Common.selectComboItem(bahasa, pegawai.getBahasa());

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "telp");
			telp = new Textbox(pegawai.getTelp() == null ? "" : pegawai.getTelp());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? telp
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getTelp())
									: telp);
			telp.setWidth("90%");
			telp.setDisabled(pegawai.getDosen() != null || pegawai.getGuru() != null);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "telpDarurat");
			telpDarurat = new Textbox(pegawai.getTelpDarurat());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? telpDarurat
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getTelpDarurat())
									: telpDarurat);
			telpDarurat.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "namaDarurat");
			namaDarurat = new Textbox(pegawai.getNamaDarurat());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? namaDarurat
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getNamaDarurat())
									: namaDarurat);
			namaDarurat.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "statusDarurat");
			statusDarurat = new Textbox(pegawai.getStatusDarurat());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? statusDarurat
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getStatusDarurat())
									: statusDarurat);
			statusDarurat.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "email");
			email = new Textbox(pegawai.getEmail() == null ? "" : pegawai.getEmail());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? email
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getEmail())
									: email);
			email.setWidth("90%");
			email.setDisabled(pegawai.getDosen() != null || pegawai.getGuru() != null);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "golonganDarah");
			golonganDarah = new Textbox(pegawai.getGolonganDarah());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? golonganDarah
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getGolonganDarah())
									: golonganDarah);
			golonganDarah.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "nomorKartuKeluarga");
			nomorKartuKeluarga = new Textbox(pegawai.getNomorKartuKeluarga());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? nomorKartuKeluarga
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getNomorKartuKeluarga())
									: nomorKartuKeluarga);
			nomorKartuKeluarga.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "namaIbuKandung");
			namaIbuKandung = new Textbox(pegawai.getNamaIbuKandung());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? namaIbuKandung
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getNamaIbuKandung())
									: namaIbuKandung);
			namaIbuKandung.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "agama");
			agama = new Combobox();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? agama
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(
											pegawai.getAgama() == null ? "" : pegawai.getAgama().getNama())
									: agama);
			Common.insertCombo(agama, "nama", Agama.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.selectComboItem(agama, pegawai.getAgama());
			agama.setWidth("90%");
			agama.setDisabled(pegawai.getDosen() != null || pegawai.getGuru() != null);
			agama.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "bank");
			bank = new Combobox();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? bank
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(
											pegawai.getBank() == null ? "" : pegawai.getBank().getNama())
									: bank);
			Common.insertComboDanSemua(bank, new String[] { "nama" }, "keterangan", Bank.class, "Belum Ditentukan",
					Restrictions.eq("aktif", true));
			Common.selectComboItem(bank, pegawai.getBank());
			bank.setWidth("90%");
			bank.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "norek");
			norek = new Textbox(pegawai.getNorek());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? norek
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getNorek())
									: norek);
			norek.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "bank2");
			bank2 = new Combobox();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? bank2
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(
											pegawai.getBank2() == null ? "" : pegawai.getBank2().getNama())
									: bank2);
			Common.insertComboDanSemua(bank2, new String[] { "nama" }, "keterangan", Bank.class, "Belum Ditentukan",
					Restrictions.eq("aktif", true));
			Common.selectComboItem(bank2, pegawai.getBank2());
			bank2.setWidth("90%");
			bank2.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "norek2");
			norek2 = new Textbox(pegawai.getNorek2());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? norek2
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getNorek2())
									: norek2);
			norek2.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "bank3");
			bank3 = new Combobox();
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? bank3
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(
											pegawai.getBank3() == null ? "" : pegawai.getBank3().getNama())
									: bank3);
			Common.insertComboDanSemua(bank3, new String[] { "nama" }, "keterangan", Bank.class, "Belum Ditentukan",
					Restrictions.eq("aktif", true));
			Common.selectComboItem(bank3, pegawai.getBank3());
			bank3.setWidth("90%");
			bank3.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "norek3");
			norek3 = new Textbox(pegawai.getNorek3());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? norek3
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getNorek3())
									: norek3);
			norek3.setWidth("90%");

			row = new MyFormRow();
			row.setVisible(false);
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "karis");
			karis = new Textbox(pegawai.getKaris());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? karis
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getKaris())
									: karis);
			karis.setWidth("90%");

			row = new MyFormRow();
			row.setVisible(false);
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "askes");
			askes = new Textbox(pegawai.getAskes());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? askes
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getAskes())
									: askes);
			askes.setWidth("90%");

			row = new MyFormRow();
			row.setVisible(false);
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "taspen");
			taspen = new Textbox(pegawai.getTaspen());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? taspen
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getTaspen())
									: taspen);
			taspen.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "npwp");
			npwp = new Textbox(pegawai.getNpwp());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? npwp
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getNpwp())
									: npwp);
			npwp.setWidth("90%");
			npwp.setDisabled(pegawai.getDosen() != null || pegawai.getGuru() != null);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "alamat");

			MyGrid alamatGrid = new MyGrid();
			alamatGrid.setStyle("border:0px;background: transparent;");
			row.appendChild(alamatGrid);

			Columns alamatcolumns = new Columns();
			alamatcolumns.setParent(alamatGrid);
			MyColumnConfig alamatcolumn = new MyColumnConfig();
			alamatcolumn.setWidth("40%");
			alamatcolumns.appendChild(alamatcolumn);
			alamatcolumn = new MyColumnConfig();
			alamatcolumns.appendChild(alamatcolumn);

			Rows alamatrows = new Rows();
			alamatrows.setParent(alamatGrid);

			MyFormRow alamatrow = new MyFormRow();
			alamatrow.setParent(alamatrows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(alamatrow, "alamatJalan");
			alamatJalan = new Textbox(pegawai.getAlamatJalan());
			alamatrow.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? alamatJalan
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getAlamatJalan())
									: alamatJalan);
			alamatJalan.setWidth("90%");
			alamatJalan.setDisabled(pegawai.getDosen() != null || pegawai.getGuru() != null);

			alamatrow = new MyFormRow();
			alamatrow.setParent(alamatrows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(alamatrow, "alamatKelurahan");
			alamatKelurahan = new Textbox(pegawai.getAlamatKelurahan());
			alamatrow.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? alamatKelurahan
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getAlamatKelurahan())
									: alamatKelurahan);
			alamatKelurahan.setWidth("90%");
			alamatKelurahan.setDisabled(pegawai.getDosen() != null || pegawai.getGuru() != null);

			alamatrow = new MyFormRow();
			alamatrow.setParent(alamatrows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(alamatrow, "alamatKecamatan");
			alamatKecamatan = new Textbox(pegawai.getAlamatKecamatan());
			alamatrow.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? alamatKecamatan
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getAlamatKecamatan())
									: alamatKecamatan);
			alamatKecamatan.setWidth("90%");
			alamatKecamatan.setDisabled(pegawai.getDosen() != null || pegawai.getGuru() != null);

			alamatrow = new MyFormRow();
			alamatrow.setParent(alamatrows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(alamatrow, "alamatKabupaten");
			alamatKabupaten = new Textbox(pegawai.getAlamatKabupaten());
			alamatrow.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? alamatKabupaten
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getAlamatKabupaten())
									: alamatKabupaten);
			alamatKabupaten.setWidth("90%");
			alamatKabupaten.setDisabled(pegawai.getDosen() != null || pegawai.getGuru() != null);

			alamatrow = new MyFormRow();
			alamatrow.setParent(alamatrows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(alamatrow, "alamatPropinsi");
			alamatPropinsi = new Textbox(pegawai.getAlamatPropinsi());
			alamatrow.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? alamatPropinsi
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getAlamatPropinsi())
									: alamatPropinsi);
			alamatPropinsi.setWidth("90%");
			alamatPropinsi.setDisabled(pegawai.getDosen() != null || pegawai.getGuru() != null);

			alamat = new Textbox(pegawai.getAlamat() == null ? "" : pegawai.getAlamat());
			alamat.setWidth("90%");
			alamat.setRows(5);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "lintang");
			lintang = new Textbox(pegawai.getLintang());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? lintang
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getLintang())
									: lintang);
			lintang.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "bujur");
			bujur = new Textbox(pegawai.getBujur());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? bujur
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getBujur())
									: bujur);
			bujur.setWidth("90%");

			EventListener listenerDosen = new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Dosen dsn = (Dosen) dosen.getAttribute("myValue");
					Guru gr = (Guru) guru.getAttribute("guru");
					if (dsn != null) {
						BiodataDosen biodataDosen = (BiodataDosen) (dsn == null ? null : dsn.ambilBiodata());

						nama.setDisabled(dsn != null);
						nama.setValue(dsn == null ? "" : dsn.getNama());

						mycode.setDisabled(dsn != null);
						mycode.setValue(dsn == null ? "" : dsn.getMycode());

						if (dsn != null && !dsn.getIdfinger().trim().isEmpty()) {
							idfinger.setDisabled(dsn != null);
							idfinger.setValue(dsn == null ? "" : dsn.getIdfinger());
						}

						pendidikan.setDisabled(dsn != null);
						Common.selectComboItem(pendidikan, dsn == null ? null : dsn.getPendidikan());

						ktp.setDisabled(dsn != null);
						ktp.setValue(dsn == null ? "" : dsn.getKtp());

						npwp.setDisabled(dsn != null);
						npwp.setValue(dsn == null ? "" : dsn.getNpwp());

						telp.setDisabled(dsn != null);
						telp.setValue(dsn == null ? "" : dsn.getTelp());

						email.setDisabled(dsn != null);
						email.setValue(dsn == null ? "" : dsn.getEmail());

						statusKepegawaian.setDisabled(dsn != null);
						Common.selectComboItem(statusKepegawaian, dsn == null ? null : dsn.getStatusKepegawaian());

						ikatanKerjaDosen.setDisabled(dsn != null);
						Common.selectComboItem(statusKepegawaian, dsn == null ? null : dsn.getIkatanKerjaDosen());

						tempatlahir.setDisabled(dsn != null);
						tempatlahir.setValue(dsn == null ? "" : dsn.getTempatlahir());

						tanggallahir.setDisabled(dsn != null);
						tanggallahir.setValue(dsn == null ? null : dsn.getTanggallahir());

						alamat.setDisabled(dsn != null);
						alamat.setValue(dsn == null ? "" : dsn.getAlamat());

						alamatKelurahan.setDisabled(biodataDosen != null);
						alamatKelurahan.setValue(biodataDosen == null || biodataDosen.getKelurahan() == null ? ""
								: biodataDosen.getKelurahan());

						alamatKecamatan.setDisabled(biodataDosen != null);
						alamatKecamatan.setValue(biodataDosen == null || biodataDosen.getKecamatan() == null ? ""
								: biodataDosen.getKecamatan().getNama());

						alamatKabupaten.setDisabled(biodataDosen != null);
						alamatKabupaten.setValue(biodataDosen == null || biodataDosen.getKota() == null ? ""
								: biodataDosen.getKota().getNama());

						alamatPropinsi.setDisabled(biodataDosen != null);
						alamatPropinsi.setValue(biodataDosen == null || biodataDosen.getPropinsi() == null ? ""
								: biodataDosen.getPropinsi().getNama());

						agama.setDisabled(biodataDosen != null);
						Common.selectComboItem(agama, biodataDosen == null ? null : biodataDosen.getAgama());

						statusPegawai.setDisabled(dsn != null);
						Common.selectComboItem(statusPegawai, dsn == null ? null : dsn.getStatusPegawai());

						String statusNikah = biodataDosen == null || biodataDosen.getStatusNikah().equals(0)
								? "Belum kawin"
								: biodataDosen.getStatusNikah().equals(1) ? "Kawin"
										: biodataDosen.getStatusNikah().equals(2) ? "Janda" : "Duda";

						statusPerkawinan.setDisabled(biodataDosen != null);
						Common.selectComboItem(statusPerkawinan, statusNikah);

						kelamin.setDisabled(dsn != null);
						Common.selectComboItem(kelamin, dsn == null ? null : dsn.getKelamin());

						Common.freezeGanti(nama, mycode, pendidikan, ktp, npwp, telp, email, statusKepegawaian,
								ikatanKerjaDosen, tempatlahir, tanggallahir, alamat, alamatKelurahan, alamatKecamatan,
								alamatKabupaten, alamatPropinsi, agama, statusPegawai, statusPerkawinan, kelamin);

					} else if (gr != null) {

						nama.setDisabled(gr != null);
						nama.setValue(gr == null ? "" : gr.getNama());

						mycode.setDisabled(gr != null);
						mycode.setValue(gr == null ? "" : gr.getKode());

						pendidikan.setDisabled(gr != null);
						Common.selectComboItem(pendidikan, gr == null ? null : gr.getPendidikan());

						ktp.setDisabled(gr != null);
						ktp.setValue(gr == null ? "" : gr.getNik());

						npwp.setDisabled(gr != null);
						npwp.setValue(gr == null ? "" : gr.getNpwp());

						telp.setDisabled(gr != null);
						telp.setValue(gr == null ? "" : gr.getTeleponGuru());

						email.setDisabled(gr != null);
						email.setValue(gr == null ? "" : gr.getAlamatEmail());

						statusKepegawaian.setDisabled(gr != null);
						Common.selectComboItem(statusKepegawaian, gr == null ? null : gr.getStatusKepegawaian());

						tempatlahir.setDisabled(gr != null);
						tempatlahir.setValue(gr == null ? "" : gr.getTempatLahir());

						tanggallahir.setDisabled(gr != null);
						tanggallahir.setValue(gr == null ? null : gr.getTanggalLahir());

						alamat.setDisabled(gr != null);
						alamat.setValue(gr == null ? "" : gr.getAlamatGuru());

						alamatKelurahan.setDisabled(gr != null);
						alamatKelurahan.setValue(gr == null || gr.getKelurahan() == null ? "" : gr.getKelurahan());

						alamatKecamatan.setDisabled(gr != null);
						alamatKecamatan
								.setValue(gr == null || gr.getKecamatan() == null ? "" : gr.getKecamatan().getNama());

						alamatKabupaten.setDisabled(gr != null);
						alamatKabupaten.setValue(
								gr == null || gr.getKecamatan() == null || gr.getKecamatan().getWilayahInduk() == null
										? ""
										: gr.getKecamatan().getWilayahInduk().getNama());

						alamatPropinsi.setDisabled(gr != null);
						alamatPropinsi.setValue(
								gr == null || gr.getKecamatan() == null || gr.getKecamatan().getWilayahInduk() == null
										|| gr.getKecamatan().getWilayahInduk().getWilayahInduk() == null ? ""
												: gr.getKecamatan().getWilayahInduk().getWilayahInduk().getNama());

						agama.setDisabled(gr != null);
						Common.selectComboItem(agama, gr == null ? null : gr.getAgama());

						statusPegawai.setDisabled(gr != null);
						Common.selectComboItem(statusPegawai, gr == null ? null : gr.getStatusPegawai());

						String statusNikah = gr == null ? "" : gr.getStatusNikah();

						statusPerkawinan.setDisabled(gr != null);
						Common.selectComboItem(statusPerkawinan, statusNikah);

						kelamin.setDisabled(gr != null);
						Common.selectComboItem(kelamin, gr == null ? null : gr.getJenisKelamin());

						Common.freezeGanti(nama, mycode, pendidikan, ktp, npwp, telp, email, statusKepegawaian,
								tempatlahir, tanggallahir, alamat, alamatKelurahan, alamatKecamatan, alamatKabupaten,
								alamatPropinsi, agama, statusPegawai, statusPerkawinan, kelamin);
					}
				}
			};

			dosen.setEventListener(listenerDosen);
			guru.setEventListener(listenerDosen);

			boolean tampilKeteranganBadan = Common.bolehKonfigurasi("tampilKeteranganBadan");

			row = new MyFormRow();
			row.setVisible(tampilKeteranganBadan);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan Badan"));

			MyGrid keteranganBadanGrid = new MyGrid();
			keteranganBadanGrid.setStyle("border:0px;background: transparent;");
			row.appendChild(keteranganBadanGrid);

			Columns keteranganBadancolumns = new Columns();
			keteranganBadancolumns.setParent(keteranganBadanGrid);
			MyColumnConfig keteranganBadancolumn = new MyColumnConfig();
			keteranganBadancolumn.setWidth("40%");
			keteranganBadancolumns.appendChild(keteranganBadancolumn);
			keteranganBadancolumn = new MyColumnConfig();
			keteranganBadancolumns.appendChild(keteranganBadancolumn);

			Rows keteranganBadanrows = new Rows();
			keteranganBadanrows.setParent(keteranganBadanGrid);

			MyFormRow keteranganBadanrow = new MyFormRow();
			keteranganBadanrow.setParent(keteranganBadanrows);

			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(keteranganBadanrow, "keteranganBadanTinggi");
			keteranganBadanTinggi = new Textbox(pegawai.getKeteranganBadanTinggi());
			keteranganBadanrow.appendChild(
					admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? keteranganBadanTinggi
							: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
									|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
											? new MyLabelAgakKecilBold(pegawai.getKeteranganBadanTinggi())
											: keteranganBadanTinggi);
			keteranganBadanTinggi.setWidth("90%");

			keteranganBadanrow = new MyFormRow();
			keteranganBadanrow.setParent(keteranganBadanrows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(keteranganBadanrow, "keteranganBadanBerat");
			keteranganBadanBerat = new Textbox(pegawai.getKeteranganBadanBerat());
			keteranganBadanrow.appendChild(
					admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? keteranganBadanBerat
							: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
									|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
											? new MyLabelAgakKecilBold(pegawai.getKeteranganBadanBerat())
											: keteranganBadanBerat);
			keteranganBadanBerat.setWidth("90%");

			keteranganBadanrow = new MyFormRow();
			keteranganBadanrow.setParent(keteranganBadanrows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(keteranganBadanrow, "keteranganBadanRambut");
			keteranganBadanRambut = new Textbox(pegawai.getKeteranganBadanRambut());
			keteranganBadanrow.appendChild(
					admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? keteranganBadanRambut
							: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
									|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
											? new MyLabelAgakKecilBold(pegawai.getKeteranganBadanRambut())
											: keteranganBadanRambut);
			keteranganBadanRambut.setWidth("90%");

			keteranganBadanrow = new MyFormRow();
			keteranganBadanrow.setParent(keteranganBadanrows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(keteranganBadanrow, "keteranganBadanBentukMuka");
			keteranganBadanBentukMuka = new Textbox(pegawai.getKeteranganBadanBentukMuka());
			keteranganBadanrow.appendChild(
					admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? keteranganBadanBentukMuka
							: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
									|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
											? new MyLabelAgakKecilBold(pegawai.getKeteranganBadanBentukMuka())
											: keteranganBadanBentukMuka);
			keteranganBadanBentukMuka.setWidth("90%");

			keteranganBadanrow = new MyFormRow();
			keteranganBadanrow.setParent(keteranganBadanrows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(keteranganBadanrow, "keteranganBadanWarnaKulit");
			keteranganBadanWarnaKulit = new Textbox(pegawai.getKeteranganBadanWarnaKulit());
			keteranganBadanrow.appendChild(
					admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? keteranganBadanWarnaKulit
							: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
									|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
											? new MyLabelAgakKecilBold(pegawai.getKeteranganBadanWarnaKulit())
											: keteranganBadanWarnaKulit);
			keteranganBadanWarnaKulit.setWidth("90%");

			keteranganBadanrow = new MyFormRow();
			keteranganBadanrow.setParent(keteranganBadanrows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(keteranganBadanrow, "keteranganBadanCiriKhas");
			keteranganBadanCiriKhas = new Textbox(pegawai.getKeteranganBadanCiriKhas());
			keteranganBadanrow.appendChild(
					admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? keteranganBadanCiriKhas
							: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
									|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
											? new MyLabelAgakKecilBold(pegawai.getKeteranganBadanCiriKhas())
											: keteranganBadanCiriKhas);
			keteranganBadanCiriKhas.setWidth("90%");

			keteranganBadanrow = new MyFormRow();
			keteranganBadanrow.setParent(keteranganBadanrows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(keteranganBadanrow, "keteranganBadanCacat");
			keteranganBadanCacat = new Textbox(pegawai.getKeteranganBadanCacat());
			keteranganBadanrow.appendChild(
					admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? keteranganBadanCacat
							: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
									|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
											? new MyLabelAgakKecilBold(pegawai.getKeteranganBadanCacat())
											: keteranganBadanCacat);
			keteranganBadanCacat.setWidth("90%");

			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "hobi");
			hobi = new Textbox(pegawai.getHobi());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? hobi
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getHobi())
									: hobi);
			hobi.setWidth("90%");
			hobi.setRows(5);

			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "keterangan");
			keterangan = new Textbox(pegawai.getKeterangan());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? keterangan
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getKeterangan())
									: keterangan);
			keterangan.setWidth("90%");
			keterangan.setRows(3);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "spesialisasi1");
			spesialisasi1 = new Textbox(pegawai.getSpesialisasi1() == null ? "" : pegawai.getSpesialisasi1());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? spesialisasi1
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getSpesialisasi1())
									: spesialisasi1);
			spesialisasi1.setWidth("90%");
			spesialisasi1.setRows(2);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "spesialisasi2");
			spesialisasi2 = new Textbox(pegawai.getSpesialisasi2() == null ? "" : pegawai.getSpesialisasi2());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? spesialisasi2
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getSpesialisasi2())
									: spesialisasi2);
			spesialisasi2.setWidth("90%");
			spesialisasi2.setRows(2);

			row = new MyFormRow();
			row.setParent(rows);
			statusWajibIsi = Common.checkApakahLabelPegawaiTampil(row, "spesialisasi3");
			spesialisasi3 = new Textbox(pegawai.getSpesialisasi3() == null ? "" : pegawai.getSpesialisasi3());
			row.appendChild(admin && statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN) ? spesialisasi3
					: (statusWajibIsi.equals(Konfigurasi.READ_ONLY_KECUALI_ADMIN)
							|| statusWajibIsi.equals(Konfigurasi.READ_ONLY))
									? new MyLabelAgakKecilBold(pegawai.getSpesialisasi3())
									: spesialisasi3);
			spesialisasi3.setWidth("90%");
			spesialisasi3.setRows(2);

			Common.createDefaultTimer(listenerDosen);

			return panel;
		}

		public Pegawai onSave(Event event, Pegawai pegawai) throws Exception {

			if (this.pegawai != null && this.pegawai.getId() != null && (pegawai == null || pegawai.getId() == null)) {
				pegawai = this.pegawai;
			}

			String statusWajibIsi = KonfigurasiTampilanPegawaiAction.statusWajibIsi("mycode");
			if (statusWajibIsi.equals(Konfigurasi.AKTIF) && mycode.getValue().trim().isEmpty()) {
				MyMessageboxConfig.showFormat(
						"Mohon Bapak/Ibu melengkapi isian \"{V1}\" terlebih dahulu. Langkah yang dapat dilakukan: (1) isi kolom yang dimaksud; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data pegawai.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						FormBiodataPegawaiUtil.MAPPING_DATA.get("mycode"));
				return null;
			}

			statusWajibIsi = KonfigurasiTampilanPegawaiAction.statusWajibIsi("code");
			if (statusWajibIsi.equals(Konfigurasi.AKTIF) && code.getValue().trim().isEmpty()) {
				MyMessageboxConfig.showFormat(
						"Mohon Bapak/Ibu melengkapi isian \"{V1}\" terlebih dahulu. Langkah yang dapat dilakukan: (1) isi kolom yang dimaksud; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data pegawai.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						FormBiodataPegawaiUtil.MAPPING_DATA.get("code"));
				return null;
			}

			if (nama.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi isian Nama terlebih dahulu. Langkah yang dapat dilakukan: (1) isi kolom Nama; (2) pastikan nama tidak dikosongkan; (3) simpan kembali data pegawai.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return null;
			}

			Session sessionCheck = null;
			try {
				sessionCheck = HibernateUtil.getSessionFactory().openSession();

				// Pengecekan untuk mycode
				if (!mycode.getValue().trim().isEmpty()) {
					Pegawai pegawaiBentrokMycode = (Pegawai) sessionCheck.createCriteria(Pegawai.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("mycode", mycode.getValue().trim()))
							.add(pegawai.getId() == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.ne("id", pegawai.getId()))
							.setMaxResults(1) // Optimasi: Cukup cari 1 saja yang bentrok
							.uniqueResult();

					if (pegawaiBentrokMycode != null) {
						String namaBentrok = pegawaiBentrokMycode.getNama() != null ? pegawaiBentrokMycode.getNama()
								: "Tanpa Nama";
						MyMessageboxConfig.showFormat(
								"Mohon maaf, nilai \"{V1}\" tidak boleh sama dengan data pegawai lain. Data ini bentrok dengan pegawai atas nama \"{V2}\" (ID: {V3}). Langkah yang dapat dilakukan: (1) gunakan nilai yang berbeda dan unik; (2) periksa kembali data pegawai yang bersangkutan; (3) simpan kembali data pegawai.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								labelMycode.getValue(), namaBentrok, pegawaiBentrokMycode.getId());
						return null;
					}
				}

				// Pengecekan untuk code
				if (!code.getValue().trim().isEmpty()) {
					Pegawai pegawaiBentrokCode = (Pegawai) sessionCheck.createCriteria(Pegawai.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("code", code.getValue().trim()))
							.add(pegawai.getId() == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.ne("id", pegawai.getId()))
							.setMaxResults(1) // Optimasi: Cukup cari 1 saja yang bentrok
							.uniqueResult();

					if (pegawaiBentrokCode != null) {
						String namaBentrok = pegawaiBentrokCode.getNama() != null ? pegawaiBentrokCode.getNama()
								: "Tanpa Nama";
						MyMessageboxConfig.showFormat(
								"Mohon maaf, nilai \"{V1}\" tidak boleh sama dengan data pegawai lain. Data ini bentrok dengan pegawai atas nama \"{V2}\" (ID: {V3}). Langkah yang dapat dilakukan: (1) gunakan nilai yang berbeda dan unik; (2) periksa kembali data pegawai yang bersangkutan; (3) simpan kembali data pegawai.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								labelCode.getValue(), namaBentrok, pegawaiBentrokCode.getId());
						return null;
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/BiodataPegawaiAction.java:4027");
			} finally {
				cleanupSession(sessionCheck);
			}

			if (checkboxConfigGuru != null && checkboxConfigGuru.isChecked() && guru.getAttribute("guru") == null) {
				MyMessageboxConfig.show("Mohon maaf, apabila opsi Guru dipilih, maka data Guru wajib diisi. Langkah yang dapat dilakukan: (1) isi atau pilih data Guru pada kolom yang tersedia; (2) atau batalkan pilihan opsi Guru; (3) simpan kembali data pegawai.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return null;
			}

			if (checkboxConfigDosen != null && checkboxConfigDosen.isChecked() && dosen.getAttribute("dosen") == null) {
				MyMessageboxConfig.show("Mohon maaf, apabila opsi Dosen dipilih, maka data Dosen wajib diisi. Langkah yang dapat dilakukan: (1) isi atau pilih data Dosen pada kolom yang tersedia; (2) atau batalkan pilihan opsi Dosen; (3) simpan kembali data pegawai.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return null;
			}

			Session sessionUpdate = null;
			Transaction tx = null;

			try {
				sessionUpdate = HibernateUtil.getSessionFactory().openSession();
				tx = sessionUpdate.beginTransaction();

				if (pegawai.getId() != null) {
					pegawai = (Pegawai) sessionUpdate.load(Pegawai.class, pegawai.getId());
				}

				pegawai.setTanggalmasuk(tanggalmasuk.getValue());
				pegawai.setHobi(hobi.getValue());
				pegawai.setStatusPerkawinan((String) (statusPerkawinan.getSelectedItem() == null ? null
						: statusPerkawinan.getSelectedItem().getValue()));
				pegawai.setAgama((Agama) (agama.getSelectedItem() == null ? null : agama.getSelectedItem().getValue()));

				String almt = Common.removeDuplicateWords(
						"Jln. " + alamatJalan.getValue() + ", Kelurahan / Desa " + alamatKelurahan.getValue()
								+ ", Kecamatan " + alamatKecamatan.getValue() + ", Kabupaten / Kota "
								+ alamatKabupaten.getValue() + ", Provinsi " + alamatPropinsi.getValue());

				alamat.setValue(almt);
				pegawai.setTunjanganKinerja(tunjanganKinerja.getValue());
				pegawai.setSertifikasi(sertifikasi.isChecked());
				pegawai.setBahasa(
						(String) (bahasa.getSelectedItem() == null ? null : bahasa.getSelectedItem().getValue()));
				pegawai.setUsiaPensiun(usiaPensiun.getValue());
				pegawai.setJatahCutiTahunan(jatahCutiTahunan == null || jatahCutiTahunan.getValue() == null ? null
						: jatahCutiTahunan.getValue().intValue());
				pegawai.setAlamatJalan(alamatJalan.getValue());
				pegawai.setAlamatKecamatan(alamatKecamatan.getValue());
				pegawai.setAlamatKabupaten(alamatKabupaten.getValue());
				pegawai.setAlamatKelurahan(alamatKelurahan.getValue());
				pegawai.setAlamatPropinsi(alamatPropinsi.getValue());
				pegawai.setKeteranganBadanBentukMuka(keteranganBadanBentukMuka.getValue());
				pegawai.setKeteranganBadanBerat(keteranganBadanBerat.getValue());
				pegawai.setKeteranganBadanCacat(keteranganBadanCacat.getValue());
				pegawai.setKeteranganBadanCiriKhas(keteranganBadanCiriKhas.getValue());
				pegawai.setKeteranganBadanRambut(keteranganBadanRambut.getValue());
				pegawai.setKeteranganBadanTinggi(keteranganBadanTinggi.getValue());
				pegawai.setKeteranganBadanWarnaKulit(keteranganBadanWarnaKulit.getValue());

				pegawai.setIdfinger(idfinger.getValue());
				pegawai.setKtp(ktp.getValue());
				pegawai.setSpesialisasi1(spesialisasi1.getValue());
				pegawai.setSpesialisasi2(spesialisasi2.getValue());
				pegawai.setSpesialisasi3(spesialisasi3.getValue());
				pegawai.setJabatan(jabatan.getValue());
				pegawai.setMycode(mycode.getValue());
				pegawai.setCode(code.getValue().trim());
				pegawai.setAlamat(alamat.getValue());
				pegawai.setEmail(email.getValue());
				pegawai.setKelamin(
						kelamin.getSelectedItem() == null ? null : kelamin.getSelectedItem().getValue().toString());
				pegawai.setNama(nama.getValue());
				pegawai.setTanggallahir(tanggallahir.getValue());
				pegawai.setTelp(telp.getValue());
				pegawai.setTelpDarurat(telpDarurat.getValue());
				pegawai.setNamaDarurat(namaDarurat.getValue());
				pegawai.setStatusDarurat(statusDarurat.getValue());
				pegawai.setGolonganDarah(golonganDarah.getValue());
				pegawai.setNomorKartuKeluarga(nomorKartuKeluarga.getValue());
				pegawai.setNamaIbuKandung(namaIbuKandung.getValue());
				pegawai.setTempatlahir(tempatlahir.getValue());
				pegawai.setPangkat(pangkat.getValue().trim());
				pegawai.setStatusPegawai((StatusPegawai) (statusPegawai.getSelectedItem() == null ? null
						: statusPegawai.getSelectedItem().getValue()));
				pegawai.setJenisTenagaKependidikan(
						(JenisTenagaKependidikan) (jenisTenagaKependidikan.getSelectedItem() == null ? null
								: jenisTenagaKependidikan.getSelectedItem().getValue()));
				pegawai.setPendidikan((Pendidikan) (pendidikan.getSelectedItem() == null ? null
						: pendidikan.getSelectedItem().getValue()));
				pegawai.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

				pegawai.setTetap(1);
				pegawai.setKeterangan(keterangan.getValue());
				pegawai.setNorek(norek.getValue());
				pegawai.setNorek2(norek2.getValue());
				pegawai.setNorek3(norek3.getValue());
				pegawai.setKaris(karis.getValue());
				pegawai.setAskes(askes.getValue());
				pegawai.setTaspen(taspen.getValue());
				pegawai.setNpwp(npwp.getValue());

				pegawai.setDosen(checkboxConfigDosen.isChecked() ? (Dosen) dosen.getAttribute("myValue") : null);
				pegawai.setStatusKepegawaian((StatusKepegawaian) (statusKepegawaian.getSelectedItem() == null ? null
						: statusKepegawaian.getSelectedItem().getValue()));
				pegawai.setIkatanKerjaDosen((IkatanKerjaDosen) (ikatanKerjaDosen.getSelectedItem() == null ? null
						: ikatanKerjaDosen.getSelectedItem().getValue()));

				Pegawai atasanBaru = (Pegawai) atasanlangsung.getAttribute("pegawai");
				pegawai.setAtasanlangsung(atasanBaru);

				Pegawai atasanBaru2 = (Pegawai) atasanlangsung2.getAttribute("pegawai");
				pegawai.setAtasanlangsung2(atasanBaru2);

				Pegawai atasanBaru3 = (Pegawai) atasanlangsung3.getAttribute("pegawai");
				pegawai.setAtasanlangsung3(atasanBaru3);

				if (atasanJbt != null)
					pegawai.setAtasan((JenisJabatan) (atasanJbt.getSelectedItem() == null ? null
							: atasanJbt.getSelectedItem().getValue()));

				if (atasanPendukung != null)
					pegawai.setAtasanPendukung((JenisJabatan) (atasanPendukung.getSelectedItem() == null ? null
							: atasanPendukung.getSelectedItem().getValue()));

				if (atasanPendukungCadangan != null)
					pegawai.setAtasanPendukungCadangan(
							(JenisJabatan) (atasanPendukungCadangan.getSelectedItem() == null ? null
									: atasanPendukungCadangan.getSelectedItem().getValue()));

				pegawai.setTanggalkeluar(tanggalkeluar.getValue());
				pegawai.setTanggalmasuk(tanggalmasuk.getValue());
				pegawai.setTanggalkeluar(tanggalkeluar.getValue());
				pegawai.setTanggalMulaiPengalanKerja(tanggalMulaiPengalanKerja.getValue());
				pegawai.setTanggalSampaiPengalanKerja(tanggalSampaiPengalanKerja.getValue());
				pegawai.setTanggalmasukHonorer(tanggalmasukHonorer.getValue());
				pegawai.setTanggalkeluarHonorer(tanggalkeluarHonorer.getValue());
				pegawai.setTanggalmasukSemiTetap(tanggalmasukSemiTetap.getValue());
				pegawai.setTanggalkeluarSemiTetap(tanggalkeluarSemiTetap.getValue());
				pegawai.setUnitKerja((UnitKerja) (unitKerja.getSelectedItem() == null ? null
						: unitKerja.getSelectedItem().getValue()));
				pegawai.setMasaKerja((MasaKerja) (masaKerja.getSelectedItem() == null ? null
						: masaKerja.getSelectedItem().getValue()));

				pegawai.setTipePegawai((TipePegawai) (tipePegawai.getSelectedItem() == null ? null
						: tipePegawai.getSelectedItem().getValue()));
				pegawai.setTipeMasaKerja((TipeMasaKerja) (tipeMasaKerja.getSelectedItem() == null ? null
						: tipeMasaKerja.getSelectedItem().getValue()));

				pegawai.setLintang(lintang.getValue());
				pegawai.setBujur(bujur.getValue());

				pegawai.setGuru(checkboxConfigGuru.isChecked() ? (Guru) guru.getAttribute("guru") : null);

				pegawai.setPtkpPegawai((PtkpPegawai) (ptkpPegawai.getSelectedItem() == null ? null
						: ptkpPegawai.getSelectedItem().getValue()));
				pegawai.setAsuransiPegawai1((AsuransiPegawai) (asuransiPegawai1.getSelectedItem() == null ? null
						: asuransiPegawai1.getSelectedItem().getValue()));
				pegawai.setAsuransiPegawai2((AsuransiPegawai) (asuransiPegawai2.getSelectedItem() == null ? null
						: asuransiPegawai2.getSelectedItem().getValue()));
				pegawai.setAsuransiPegawai3((AsuransiPegawai) (asuransiPegawai3.getSelectedItem() == null ? null
						: asuransiPegawai3.getSelectedItem().getValue()));
				pegawai.setAsuransiPegawai4((AsuransiPegawai) (asuransiPegawai4.getSelectedItem() == null ? null
						: asuransiPegawai4.getSelectedItem().getValue()));

				pegawai.setNomorAsuransiPegawai1(nomorAsuransiPegawai1.getValue().trim());
				pegawai.setNomorAsuransiPegawai2(nomorAsuransiPegawai2.getValue().trim());
				pegawai.setNomorAsuransiPegawai3(nomorAsuransiPegawai3.getValue().trim());
				pegawai.setNomorAsuransiPegawai4(nomorAsuransiPegawai4.getValue().trim());

				if (tendikSekolah != null) {
					pegawai.setTendikSekolah((Sekolah) (tendikSekolah.getSelectedItem() == null ? null
							: tendikSekolah.getSelectedItem().getValue()));
				}
				if (tendikJurusan != null) {
					pegawai.setTendikJurusan((Jurusan) (tendikJurusan.getSelectedItem() == null ? null
							: tendikJurusan.getSelectedItem().getValue()));
				}
				if (tendikFakultas != null) {
					pegawai.setTendikFakultas((Fakultas) (tendikFakultas.getSelectedItem() == null ? null
							: tendikFakultas.getSelectedItem().getValue()));
				}

				pegawai.setBank((Bank) (bank == null || bank.getSelectedItem() == null ? null
						: bank.getSelectedItem().getValue()));
				pegawai.setBank2((Bank) (bank2 == null || bank2.getSelectedItem() == null ? null
						: bank2.getSelectedItem().getValue()));
				pegawai.setBank3((Bank) (bank3 == null || bank3.getSelectedItem() == null ? null
						: bank3.getSelectedItem().getValue()));

				pegawai.setKtp(ktp.getValue());
				List<String> daftarWajibDiisi = KonfigurasiTampilanPegawaiAction.dataYangWajibDiisi();
				for (String key : daftarWajibDiisi) {
					if (Common.checkIsNull(Pegawai.class, pegawai, key)) {
						MyMessageboxConfig.showFormat(
								"Mohon Bapak/Ibu melengkapi biodata pegawai. Data \"{V1}\" masih belum terisi dengan benar. Langkah yang dapat dilakukan: (1) lengkapi kolom yang dimaksud; (2) pastikan seluruh data wajib terisi dengan benar; (3) simpan kembali data pegawai.",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								KonfigurasiTampilanPegawaiAction.keyDesc(key));
						if (tx != null)
							tx.rollback();
						return pegawai;
					}
				}

				if (pegawai.getSatuanKerja() == null) {
					Yayasan yayasan = SekolahUtil.getYayasan();
					SatuanKerja satuanKerja = (SatuanKerja) ConstantValues
							.simpleObject(
									sessionUpdate.createCriteria(SatuanKerja.class).add(Restrictions.isNull("parent"))
											.add(yayasan == null || yayasan.getId() == null
													? Restrictions.sqlRestriction("true")
													: Restrictions.eq("yayasan", yayasan))
											.setMaxResults(1),
									SatuanKerja.class);
					pegawai.setSatuanKerja(satuanKerja);
				}

				Common.refreshSaveOrUpdate(sessionUpdate, pegawai);
				tx.commit();

			} catch (Exception e) {
				if (tx != null)
					tx.rollback();
				throw e;
			} finally {
				cleanupSession(sessionUpdate);
			}

			// Penanganan File Lintas-Server (StreamingHibernateUtil)
			if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
				Session streamSession = null;
				Transaction streamTx = null;
				try {
					streamSession = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
					streamTx = streamSession.beginTransaction();

					streamSession.refresh(lainMahasiswa);
					lainMahasiswa.setRef(pegawai.getId());
					streamSession.update(lainMahasiswa);

					streamTx.commit();
				} catch (Exception e) {
					if (streamTx != null)
						streamTx.rollback();
					Common.tampilErrorJikaAdmin(e);
				} finally {
					cleanupSession(streamSession);
				}
			}

			if (fotoPegawai != null && fotoPegawai.getId() != null) {
				Session streamSession = null;
				Transaction streamTx = null;
				try {
					streamSession = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
					streamTx = streamSession.beginTransaction();

					streamSession.refresh(fotoPegawai);
					fotoPegawai.setPegawai(pegawai.getId());
					streamSession.update(fotoPegawai);

					streamTx.commit();
				} catch (Exception e) {
					if (streamTx != null)
						streamTx.rollback();
					Common.tampilErrorJikaAdmin(e);
				} finally {
					cleanupSession(streamSession);
				}
			}

			if (ttd != null && ttd.getId() != null) {
				Session streamSession = null;
				Transaction streamTx = null;
				try {
					streamSession = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
					streamTx = streamSession.beginTransaction();

					streamSession.refresh(ttd);
					ttd.setRef(pegawai.getId());
					streamSession.update(ttd);

					streamTx.commit();
				} catch (Exception e) {
					if (streamTx != null)
						streamTx.rollback();
					Common.tampilErrorJikaAdmin(e);
				} finally {
					cleanupSession(streamSession);
				}
			}

			return pegawai;
		}
	}

	private class ManagingBiodataPegawai {
		private Textbox alamat;
		private Textbox namaAyah;
		private Textbox pekerjaanAyah;
		private Textbox namaIbu;
		private Textbox pekerjaanIbu;

		private Combobox pernahMenetapDiLuarNegeri;
		private Textbox tinggiBadan;
		private Textbox beratBadan;
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
		private Textbox asalS1;
		private Textbox alamatAsalS1;
		private Textbox asalS2;
		private Textbox alamatAsalS2;
		private Textbox asalS3;
		private Textbox alamatAsalS3;
		private Textbox keahlian1;
		private Textbox keahlian2;
		private Textbox keahlian3;
		private Textbox keahlian4;
		private Textbox keahlian5;
		private AmbilDataNamaSekolahBanbox asalSma;
		private Textbox alamatAsalSma;
		private AmbilDataNamaSekolahBanbox asalSmp;
		private Textbox alamatAsalSmp;
		private AmbilDataNamaSekolahBanbox asalSd;
		private Textbox alamatAsalSd;
		private Textbox golonganDarah;
		private Combobox statusNikah;
		private Textbox kewarganegaraan;
		private Textbox agama;

		private BiodataPegawai biodataPegawai;
		private Pegawai pegawai;

		private Tabpanel preInit(Pegawai pegawai) throws Exception {
			this.pegawai = pegawai;

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

			return loadDataPegawai();
		}

		public Tabpanel loadDataPegawai() throws Exception {
			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				if (pegawai != null && pegawai.getId() != null) {
					biodataPegawai = (BiodataPegawai) session.createCriteria(BiodataPegawai.class)
							.add(Restrictions.eq("pegawai", pegawai)).addOrder(Order.desc("id")).setMaxResults(1)
							.uniqueResult();
				}
			} finally {
				cleanupSession(session);
			}

			Tabpanel tabpanelBiodata;
			if (biodataPegawai == null) {
				tabpanelBiodata = initBiodataPegawai(new BiodataPegawai());
			} else {
				tabpanelBiodata = initBiodataPegawai(biodataPegawai);
			}
			return tabpanelBiodata;
		}

		private Tabpanel initBiodataPegawai(final BiodataPegawai biodataPegawai) throws Exception {
			this.biodataPegawai = biodataPegawai;
			Tabpanel panel = new ais.ui.util.MyTabpanel();
			panel.setWidth("100%");
			panel.setHeight("100%");
			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(panel);
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
			row.appendChild(new ais.ui.util.MyLabelConfig(pegawai.getNama()));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode/NIP"));
			row.appendChild(new ais.ui.util.MyLabelConfig(pegawai.getCode()));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Alamat"));
			row.appendChild(alamat = new Textbox(biodataPegawai.getAlamat() == null ? "" : biodataPegawai.getAlamat()));
			alamat.setWidth("90%");
			alamat.setRows(3);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama Ayah"));
			row.appendChild(
					namaAyah = new Textbox(biodataPegawai.getNamaAyah() == null ? "" : biodataPegawai.getNamaAyah()));
			namaAyah.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pekerjaan Ayah"));
			row.appendChild(pekerjaanAyah = new Textbox(
					biodataPegawai.getPekerjaanAyah() == null ? "" : biodataPegawai.getPekerjaanAyah()));
			pekerjaanAyah.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama Ibu"));
			row.appendChild(
					namaIbu = new Textbox(biodataPegawai.getNamaIbu() == null ? "" : biodataPegawai.getNamaIbu()));
			namaIbu.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pekerjaan Ibu"));
			row.appendChild(pekerjaanIbu = new Textbox(
					biodataPegawai.getPekerjaanIbu() == null ? "" : biodataPegawai.getPekerjaanIbu()));
			pekerjaanIbu.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pernah Menetap di Luar Negeri"));
			Common.selectComboItem(pernahMenetapDiLuarNegeri, biodataPegawai.getPernahMenetapDiLuarNegeri());
			row.appendChild(pernahMenetapDiLuarNegeri);
			pernahMenetapDiLuarNegeri.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tinggi Badan"));
			row.appendChild(tinggiBadan = new Textbox(
					biodataPegawai.getTinggiBadan() == null ? "" : biodataPegawai.getTinggiBadan().toString()));
			tinggiBadan.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Berat Badan"));
			row.appendChild(beratBadan = new Textbox(
					biodataPegawai.getBeratBadan() == null ? "" : biodataPegawai.getBeratBadan().toString()));
			beratBadan.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Telepon Rumah"));
			row.appendChild(teleponRumah = new Textbox(
					biodataPegawai.getTeleponRumah() == null ? "" : biodataPegawai.getTeleponRumah()));
			teleponRumah.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("HP"));
			row.appendChild(hp = new Textbox(biodataPegawai.getHp() == null ? "" : biodataPegawai.getHp()));
			hp.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Surat Ijin Mengemudi"));
			row.appendChild(suratIzinMengemudi = new Textbox(
					biodataPegawai.getSuratIzinMengemudi() == null ? "" : biodataPegawai.getSuratIzinMengemudi()));
			suratIzinMengemudi.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kendaraan Kuliah"));
			row.appendChild(kendaraanKuliah = new Textbox(
					biodataPegawai.getKendaraanKuliah() == null ? "" : biodataPegawai.getKendaraanKuliah()));
			kendaraanKuliah.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pernah Memimpin Organisasi"));
			Common.selectComboItem(pernahMemimpinOrganisasi, biodataPegawai.getPernahMemimpinOrganisasi());
			row.appendChild(pernahMemimpinOrganisasi);
			pernahMemimpinOrganisasi.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama Organisasi"));
			row.appendChild(namaOrganisasi = new Textbox(
					biodataPegawai.getNamaOrganisasi() == null ? "" : biodataPegawai.getNamaOrganisasi()));
			namaOrganisasi.setWidth("90%");
			namaOrganisasi.setMaxlength(49);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Hobi"));
			row.appendChild(hobi = new Textbox(biodataPegawai.getHobi() == null ? "" : biodataPegawai.getHobi()));
			hobi.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Minat Seni"));
			row.appendChild(minatSeni = new Textbox(
					biodataPegawai.getMinatSeni() == null ? "" : biodataPegawai.getMinatSeni()));
			minatSeni.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kemampuan Bahasa 1"));
			row.appendChild(kemampuanBahasa1 = new Textbox(
					biodataPegawai.getKemampuanBahasa1() == null ? "" : biodataPegawai.getKemampuanBahasa1()));
			kemampuanBahasa1.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kemampuan Bahasa 2"));
			row.appendChild(kemampuanBahasa2 = new Textbox(
					biodataPegawai.getKemampuanBahasa2() == null ? "" : biodataPegawai.getKemampuanBahasa2()));
			kemampuanBahasa2.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kemampuan Bahasa 3"));
			row.appendChild(kemampuanBahasa3 = new Textbox(
					biodataPegawai.getKemampuanBahasa3() == null ? "" : biodataPegawai.getKemampuanBahasa3()));
			kemampuanBahasa3.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Asal S1"));
			row.appendChild(asalS1 = new Textbox(biodataPegawai.getAsalS1() == null ? "" : biodataPegawai.getAsalS1()));
			asalS1.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Alamat Asal S1"));
			row.appendChild(alamatAsalS1 = new Textbox(
					biodataPegawai.getAlamatAsalS1() == null ? "" : biodataPegawai.getAlamatAsalS1()));
			alamatAsalS1.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Asal S2"));
			row.appendChild(asalS2 = new Textbox(biodataPegawai.getAsalS2() == null ? "" : biodataPegawai.getAsalS2()));
			asalS2.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Alamat Asal S2"));
			row.appendChild(alamatAsalS2 = new Textbox(
					biodataPegawai.getAlamatAsalS2() == null ? "" : biodataPegawai.getAlamatAsalS2()));
			alamatAsalS2.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Asal S3"));
			row.appendChild(asalS3 = new Textbox(biodataPegawai.getAsalS3() == null ? "" : biodataPegawai.getAsalS3()));
			asalS3.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Alamat Asal S3"));
			row.appendChild(alamatAsalS3 = new Textbox(
					biodataPegawai.getAlamatAsalS3() == null ? "" : biodataPegawai.getAlamatAsalS3()));
			alamatAsalS3.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Keahlian 1"));
			row.appendChild(keahlian1 = new Textbox(
					biodataPegawai.getKeahliah1() == null ? "" : biodataPegawai.getKeahliah1()));
			keahlian1.setWidth("90%");
			keahlian1.setRows(4);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Keahlian 2"));
			row.appendChild(keahlian2 = new Textbox(
					biodataPegawai.getKeahlian2() == null ? "" : biodataPegawai.getKeahlian2()));
			keahlian2.setWidth("90%");
			keahlian2.setRows(4);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Keahlian 3"));
			row.appendChild(keahlian3 = new Textbox(
					biodataPegawai.getKeahlian3() == null ? "" : biodataPegawai.getKeahlian3()));
			keahlian3.setWidth("90%");
			keahlian3.setRows(4);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Keahlian 4"));
			row.appendChild(keahlian4 = new Textbox(
					biodataPegawai.getKeahlian4() == null ? "" : biodataPegawai.getKeahlian4()));
			keahlian4.setWidth("90%");
			keahlian4.setRows(4);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Keahlian 5"));
			row.appendChild(keahlian5 = new Textbox(
					biodataPegawai.getKeahlian5() == null ? "" : biodataPegawai.getKeahlian5()));
			keahlian5.setWidth("90%");
			keahlian5.setRows(4);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Asal SMA"));
			row.appendChild(asalSma = new AmbilDataNamaSekolahBanbox(
					biodataPegawai.getAsalSma() == null ? "" : biodataPegawai.getAsalSma()));
			asalSma.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Alamat Asal SMA"));
			row.appendChild(alamatAsalSma = new Textbox(
					biodataPegawai.getAlamatAsalSma() == null ? "" : biodataPegawai.getAlamatAsalSma()));
			alamatAsalSma.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Asal SMP"));
			row.appendChild(asalSmp = new AmbilDataNamaSekolahBanbox(
					biodataPegawai.getAsalSmp() == null ? "" : biodataPegawai.getAsalSmp()));
			asalSmp.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Alamat Asal SMP"));
			row.appendChild(alamatAsalSmp = new Textbox(
					biodataPegawai.getAlamatAsalSmp() == null ? "" : biodataPegawai.getAlamatAsalSmp()));
			alamatAsalSmp.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Asal SD"));
			row.appendChild(asalSd = new AmbilDataNamaSekolahBanbox(
					biodataPegawai.getAsalSd() == null ? "" : biodataPegawai.getAsalSd()));
			asalSd.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Alamat Asal SD"));
			row.appendChild(alamatAsalSd = new Textbox(
					biodataPegawai.getAlamatAsalSd() == null ? "" : biodataPegawai.getAlamatAsalSd()));
			alamatAsalSd.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Golongan Darah"));
			row.appendChild(golonganDarah = new Textbox(
					biodataPegawai.getGolonganDarah() == null ? "" : biodataPegawai.getGolonganDarah()));
			golonganDarah.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Status Nikah"));
			Common.selectComboItem(statusNikah, biodataPegawai.getStatusNikah());
			row.appendChild(statusNikah);
			statusNikah.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kewarganegaraan"));
			row.appendChild(kewarganegaraan = new Textbox(
					biodataPegawai.getKewarganegaraan() == null ? "" : biodataPegawai.getKewarganegaraan()));
			kewarganegaraan.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Agama"));
			row.appendChild(agama = new Textbox(biodataPegawai.getAgama() == null ? "" : biodataPegawai.getAgama()));
			agama.setWidth("90%");

			if (pegawai != null && pegawai.getDosen() != null) {
				Common.freeze(rows, true);
			}

			return panel;
		}

		public boolean onSave(Event event) throws Exception {
			Session sessionObj = null;
			Transaction tx = null;
			try {
				sessionObj = HibernateUtil.getSessionFactory().openSession();
				tx = sessionObj.beginTransaction();

				if (biodataPegawai.getId() != null) {
					biodataPegawai = (BiodataPegawai) sessionObj.load(BiodataPegawai.class, biodataPegawai.getId());
				}
				biodataPegawai.setPegawai(pegawai);
				biodataPegawai.setAlamat(alamat.getValue());
				biodataPegawai.setNamaAyah(namaAyah.getValue());
				biodataPegawai.setPekerjaanAyah(pekerjaanAyah.getValue());
				biodataPegawai.setNamaIbu(namaIbu.getValue());
				biodataPegawai.setPekerjaanIbu(pekerjaanIbu.getValue());
				try {
					biodataPegawai.setTinggiBadan(tinggiBadan.getValue().trim().isEmpty() ? null
							: Integer.parseInt(tinggiBadan.getValue().trim()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BiodataPegawaiAction.java:4786");
				}

				biodataPegawai.setPernahMenetapDiLuarNegeri(
						(Integer) (pernahMenetapDiLuarNegeri.getSelectedItem() == null ? null
								: pernahMenetapDiLuarNegeri.getSelectedItem().getValue()));

				try {
					biodataPegawai.setBeratBadan(beratBadan.getValue().trim().isEmpty() ? null
							: Integer.parseInt(beratBadan.getValue().trim()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BiodataPegawaiAction.java:4796");
				}

				biodataPegawai.setTeleponRumah(teleponRumah.getValue());
				biodataPegawai.setHp(hp.getValue());
				biodataPegawai.setSuratIzinMengemudi(suratIzinMengemudi.getValue());
				biodataPegawai.setKendaraanKuliah(kendaraanKuliah.getValue());
				biodataPegawai.setPernahMemimpinOrganisasi(
						(Integer) (pernahMemimpinOrganisasi.getSelectedItem() == null ? null
								: pernahMemimpinOrganisasi.getSelectedItem().getValue()));
				biodataPegawai.setNamaOrganisasi(namaOrganisasi.getValue());
				biodataPegawai.setHobi(hobi.getValue());
				biodataPegawai.setMinatSeni(minatSeni.getValue());
				biodataPegawai.setKemampuanBahasa1(kemampuanBahasa1.getValue());
				biodataPegawai.setKemampuanBahasa2(kemampuanBahasa2.getValue());
				biodataPegawai.setKemampuanBahasa3(kemampuanBahasa3.getValue());
				biodataPegawai.setAsalS1(asalS1.getValue());
				biodataPegawai.setAlamatAsalS1(alamatAsalS1.getValue());
				biodataPegawai.setAsalS2(asalS2.getValue());
				biodataPegawai.setAlamatAsalS2(alamatAsalS2.getValue());
				biodataPegawai.setAsalS3(asalS3.getValue());
				biodataPegawai.setAlamatAsalS3(alamatAsalS3.getValue());
				biodataPegawai.setKeahliah1(keahlian1.getValue());
				biodataPegawai.setKeahlian2(keahlian2.getValue());
				biodataPegawai.setKeahlian3(keahlian3.getValue());
				biodataPegawai.setKeahlian4(keahlian4.getValue());
				biodataPegawai.setKeahlian5(keahlian5.getValue());
				biodataPegawai.setAsalSma(asalSma.getValue());
				biodataPegawai.setAlamatAsalSma(alamatAsalSma.getValue());
				biodataPegawai.setAsalSmp(asalSmp.getValue());
				biodataPegawai.setAlamatAsalSmp(alamatAsalSmp.getValue());
				biodataPegawai.setAsalSd(asalSd.getValue());
				biodataPegawai.setAlamatAsalSd(alamatAsalSd.getValue());
				biodataPegawai.setGolonganDarah(golonganDarah.getValue());
				biodataPegawai.setStatusNikah((Integer) (statusNikah.getSelectedItem() == null ? null
						: statusNikah.getSelectedItem().getValue()));
				biodataPegawai.setKewarganegaraan(kewarganegaraan.getValue());
				biodataPegawai.setAgama(agama.getValue());

				if (biodataPegawai.getId() != null) {
					sessionObj.update(biodataPegawai);
				} else {
					sessionObj.save(biodataPegawai);
				}
				tx.commit();
				return true;
			} catch (Exception e) {
				if (tx != null)
					tx.rollback();
				Common.tampilErrorJikaAdmin(e);
				return false;
			} finally {
				cleanupSession(sessionObj);
			}
		}
	}
}
