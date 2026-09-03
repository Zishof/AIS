package ais.action.master;

import java.io.File;
import java.io.Serializable;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.East;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.DaftarUlangPembayaranHelper;
import ais.action.master.helper.DaftarUlangTagihanAnalisisHelper;
import ais.action.master.helper.KegiatanHelper;
import ais.action.master.helper.KegiatanPersistenceHelper;
import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.master.helper.PengecualianTagihanList;
import ais.action.master.helper.PengecualianJadwalPengisianKRSMahasiswaHelper;
import ais.action.master.helper.RevisiCicilanPembayaranHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.TunggakanMahasiswaHelper;
import ais.action.master.helper.keuangan.DownloadCicilanMahasiswa;
import ais.action.master.helper.keuangan.UploadCicilanMahasiswa;
import ais.action.master.helper.util.PenilaianUtil;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankBankaltimtara;
import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankBjb;
import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankBtn;
import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankNtt;
import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankOnline;
import ais.action.master.helper.virtualaccount.MahasiswaVirtualAccountHelper;
import ais.action.master.pmb.TampilanPaymentGateway;
import ais.action.report.CommonReportHelper;
import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.BarcodeCommon;
import ais.common.BniCommon;
import ais.common.BriCommon;
import ais.common.BsiCommon;
import ais.common.CicilanPembayaranRecoveryHelper;
import ais.common.CimbCommon;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.DokuCommon;
import ais.common.FaspayCommon;
import ais.common.FinpayCommon;
import ais.common.IndonesianNumberToWords;
import ais.common.IpaymuCommon;
import ais.common.JatelindoCommon;
import ais.common.OnlineBmtUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.BuktiPembayaran;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.DetailSettingBiaya;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.Jenjang;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisPembayaran;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.KegiatanTemporary;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.LogPembayaran;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranCutiMahasiswa;
import ais.database.model.Paket;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.database.model.SettingBiaya;
import ais.database.model.SettingBiayaDetail;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.TunggakanMahasiswa;
import ais.database.model.VirtualAccountBank;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.ui.render.DetailPembayaranMahasiswaRenderer;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyPortallayout;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyDoubleboxMin;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk daftar ulang mahasiswa lama. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * AbstractDaftarUlangMahasiswaAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk
 * variasi ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak
 * bercabang atau tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Label semester}, {@code Combobox akun},
 * {@code HistoryStatusMahasiswa tempHistoryStatusMahasiswa}, {@code Label tanggalValidasi}, {@code boolean
 * edit}, {@code boolean delete}, {@code Row rowInfoTunggakan}, {@code Row rowNim}; inisialisasi/lifecycle
 * ({@code doAfterCompose()}, {@code buatPlaceholderPanel()}, {@code buatSettingBiayaDariMahasiswa()}, {@code
 * setupPaymentGateway()}, {@code setupBankOnlineGateway()}); pembacaan/pencarian ({@code getGridCicilan()},
 * {@code getGridBiaya()}, {@code getCicilanPembayarans()}, {@code getSemuaItemBiaya()}, {@code
 * getFooterDibayar()}, {@code getFooterDibayarTerbilang()}); validasi/perhitungan ({@code
 * checkKondisiSebelumbayarBaru()}, {@code hitungSettingBiaya()}, {@code hitungUjiKriteriaDilewati()}, {@code
 * hitungDetailBiayaTahapanLama()}, {@code validasiPembayaran()}, {@code validasiPembayaran()}); mutasi data
 * ({@code adaTagihanTerpilihUntukProses()}, {@code reset()}, {@code onSave()}, {@code onSave()}); operasi domain
 * lain ({@code onTagihanMahasiswaBaru()}, {@code onDiskonMahasiswa()}, {@code onKeranjangPembayaran()}, {@code
 * onBuktiPembayaran()}, {@code onBlokirMahasiswa()}, {@code onPengeluaranMahasiswa()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see AbstractDaftarUlangMahasiswaAction
 */
public class DaftarUlangMahasiswaLamaAction extends AbstractDaftarUlangMahasiswaAction {

	private static final long serialVersionUID = -4681108885695239730L;

	private Label semester;
	private Combobox akun;
	private HistoryStatusMahasiswa tempHistoryStatusMahasiswa = null;
	private Label tanggalValidasi;

	private boolean edit = true;
	private boolean delete = true;

	private Row rowInfoTunggakan;
	private Row rowNim;
	private Row rowJenisKuliah;
	private Row rowJenjang;
	private Row rowProdi;
	private Row rowSemester;
	private Row rowTahunMasuk;
	private Row rowTahunAkademik;
	private Row rowTanggalValidasi;
	private Row rowValidator;
	private Row rowPengurangan;
	private Row rowKeterangan;
	private Row rowListBiaya;

	private AmbilDataMahasiswaBanbox nim;
	private Vbox jenisKuliah;
	private Label labelJenjang;
	private Label prodi;
	private Vbox labelFotoMahasiswa;
	private Label labelNimMahasiswa;
	private Label labelNamaMahasiswa;
	private MyLabelBoldAja labelTabungan;
	private Label labelTahunMasuk;
	private Label labelTahunAkademik;
	private Label validator;
	private List<MyDoubleboxMin> pengurangan;
	private Textbox keterangan;
	private Grid gridss;

	private Button tombolRefresh;
	private Hbox informasi;

	private MyLabelBoldAja labelFooterItemBiaya;
	private MyLabelBoldAja labelFooterTagihan;
	private MyLabelBoldAja labelFooterDibayarAja;

	private Combobox jenisPembayaran;
	private Combobox semesterPilihan;

	private Mahasiswa mahasiswa;
	private Kegiatan kegiatan;
	private JadwalPembayaran jadwalPembayaran;
	private PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	private Double nilaiBiayaHarusDiBayars = 0.0;
	private List<PengaturanPembayaranBulanan> pengaturanPembayaranBulanans = new ArrayList<PengaturanPembayaranBulanan>();
	private Map<Long, LampiranLain> buktiPembayarans = new HashMap<Long, LampiranLain>();

	private Vbox center;
	private Borderlayout borderlayoutUtama;
	private org.zkoss.zul.Div portalHost;
	private Component panelMencicil;
	private Component panelAnalisis;
	private Row rowMobile;
	private boolean bolehmencicilBaru = true;

	private JenisKegiatan jk = null;
	private Mahasiswa mahasiswaAktif = null;
	private Tbmuser tbmuser = null;

	private Tabpanel tabpanelTagihanMahasiswaBaru;
	private Tabpanel tabpanelBuktiPembayaran;
	private Tabpanel tabpanelDiskonMahasiswa;
	private MyTabConfig keranjang;
	private Tabpanel tabpanelKeranjangPembayaran;
	private Tabpanel tabpanelBlokirMahasiswa;
	private Tabpanel tabpanelPengeluaranMahasiswa;
	private Tabpanel tabpanelDepositMahasiswa;
	private Tabpanel tabpanelPembelianMahasiswa;
	protected Tabpanel tabpanelUploadDanDownloadCicilanMahasiswa;

	private MyTabConfig tabFinpay;
	private Tabpanel tabpanelFinpay;
	private MyTabConfig tabIpaymu;
	private Tabpanel tabpanelIpaymu;
	private MyTabConfig tabFaspay;
	private Tabpanel tabpanelFaspay;
	private MyTabConfig tabJatelindo;
	private Tabpanel tabpanelJatelindo;
	private MyTabConfig tabCimb;
	private Tabpanel tabpanelCimb;
	private MyTabConfig tabBni;
	private Tabpanel tabpanelBni;
	private MyTabConfig tabBsi;
	private Tabpanel tabpanelBsi;
	private MyTabConfig tabBri;
	private Tabpanel tabpanelBri;
	private MyTabConfig tabDoku;
	private Tabpanel tabpanelDoku;

	private double jumlahYangAkanDibayar;
	private Integer selectedSemester;

	private boolean tampilkanTanggalKwitansi = false;
	private BuktiPembayaran buktiPembayaran = null;
	private Double tabungan = 0.0;
	// Batas saldo saat isi cicilan otomatis "Dari Tabungan" (0 = normal/tanpa batas).
	private double capSaldoIsiCicilan = 0.0;
//	private List<CicilanPembayaran> cicilanPembayaransTemp = new ArrayList<CicilanPembayaran>();
	protected StatusMahasiswa statusmahasiswa;

	private Grid gridCicilan = new Grid();
	private MyLabelBoldAja footerTotal;
	private MyLabelBoldAja footerTotalTerbilang;
	private MyLabelBoldAja footerDibayar;
	private MyLabelBoldAja footerDibayarTerbilang;
	private MyCheckboxConfig mencicil;
	private Hbox hboxJenisPembayaran;
	private Rows rowsCicilan;
	private EventListener jumlahCicilahEventListener;
	private HashMap<Long, DetailBiaya> itemBiayas;
	private boolean bolehMerubahCicilan;
	private int countPengaturanBulanan = 0;
	private List<CicilanPembayaran> cicilanPembayarans = new ArrayList<CicilanPembayaran>();
	@SuppressWarnings("rawtypes")
	protected Collection detailBiayas = new ArrayList();
	private JenisKegiatan jenisKegiatan;
	private DetailPembayaranMahasiswaRenderer detailPembayaranMahasiswaRenderer;
	private Label terbilang;
	private MyLabelBoldAja labelFooterKekurangan;
	private HashMap<Long, Double> dataTagihan;
	private boolean refresh = false;
	private Label terbilangTagihan;
	private Label terbilangSisa;
	private Label terbilangSisaPersen;
	private Button sesuaikanDenganTagihan;
	private Button sesuaikanDenganTagihanBulanan;
	private MyToolbarbuttonConfig buttonReset;
	@SuppressWarnings("rawtypes")
	private ArrayList dataTagihanData = null;
	private Box myspaceBayar = null;
	private boolean starat = true;

	// Mode ringkas untuk embed di Wizard Pembayaran (param URL wizard=1): sembunyikan
	// tab, kombo filter, profil mahasiswa, dan dasbor analisis — sisakan tabel item
	// biaya (beserta checkbox "Proses Bayar") + panel pembayaran. Logika tidak diubah.
	private boolean modeWizardRingkas = false;
	// Wadah kolom "Pilih Cara Pembayaran" (langkah 4 Wizard) — tombol bayar (menuBayar)
	// dipindah ke sini saat mode wizard agar bisa jadi langkah terpisah.
	private org.zkoss.zk.ui.Component caraBayarHost = null;
	// Ringkasan item "yang akan dibayar" (langkah 4 Wizard) — di-update tiap hitung ulang.
	private org.zkoss.zul.Html ringkasanItemWizard = null;

	// === Accessor hook untuk AbstractDaftarUlangMahasiswaAction (state spesifik subclass) ===
	@Override
	protected Grid getGridCicilan() {
		return gridCicilan;
	}

	@Override
	protected Grid getGridBiaya() {
		return gridss;
	}

	@Override
	protected List<CicilanPembayaran> getCicilanPembayarans() {
		return cicilanPembayarans;
	}

	@Override
	protected java.util.Collection<DetailBiaya> getSemuaItemBiaya() {
		return itemBiayas.values();
	}

	@Override
	protected MyLabelBoldAja getFooterDibayar() {
		return footerDibayar;
	}

	@Override
	protected MyLabelBoldAja getFooterDibayarTerbilang() {
		return footerDibayarTerbilang;
	}

	@Override
	protected MyLabelBoldAja getFooterTotal() {
		return footerTotal;
	}

	@Override
	protected MyLabelBoldAja getFooterTotalTerbilang() {
		return footerTotalTerbilang;
	}

	public void onTagihanMahasiswaBaru(Event event) throws Exception {
		Tbmuser tbmuser = Common.getCurrentUser();
		mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		if (mahasiswa == null || mahasiswa.getBiodataCalonMahasiswaData() == null) {
			MyMessageboxConfig.show("Mohon maaf, data Calon Mahasiswa tidak ditemukan pada data mahasiswa ini. Langkah yang dapat dilakukan: (1) pastikan mahasiswa ini memiliki data calon mahasiswa yang terhubung; (2) lengkapi data calon mahasiswa terlebih dahulu; (3) apabila memerlukan bantuan, mohon hubungi Administrator sistem.", "Pemberitahuan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		} else {
			loadIframeToTabpanel(tabpanelTagihanMahasiswaBaru,
					"/common/daftarulang_mahasiswa_baru.zul?biodataCalonMahasiswa="
							+ mahasiswa.getBiodataCalonMahasiswa());
		}
	}

	public void onDiskonMahasiswa(Event event) {
		loadIframeToTabpanel(tabpanelDiskonMahasiswa, "/pages/master/diskon_mahasiswa.zul");
	}

	public void onKeranjangPembayaran(Event event) {
		loadIframeToTabpanel(tabpanelKeranjangPembayaran, "/pages/master/kegiatan_temporary.zul");
	}

	public void onBuktiPembayaran(Event event) {
		loadIframeToTabpanel(tabpanelBuktiPembayaran, "/pages/master/bukti_pembayaran.zul");
	}

	public void onBlokirMahasiswa(Event event) {
		loadIframeToTabpanel(tabpanelBlokirMahasiswa, "/pages/master/blokir_mahasiswa.zul");
	}

	public void onPengeluaranMahasiswa(Event event) {
		loadIframeToTabpanel(tabpanelPengeluaranMahasiswa, "/pages/master/pengeluaran_mahasiswa.zul");
	}

	public void onDepositMahasiswa(Event event) {
		loadIframeToTabpanel(tabpanelDepositMahasiswa, "/pages/master/deposit.zul");
	}

	public void onPembelianMahasiswa(Event event) {
		loadIframeToTabpanel(tabpanelPembelianMahasiswa,
				"/pages/master/koperasi/pem_online.zul?langsungBayar=true&sumberPembayaran=mahasiswa&modePelanggan=mahasiswa&jenisPelanggan=mahasiswa&hanyaMahasiswa=true&dariDaftarUlangMahasiswa=true");
	}

	public void onFinpay(Event event) {
		loadIframeToTabpanel(tabpanelFinpay, "/pages/master/finpay/finpay_request.zul");
	}

	public void onIpaymu(Event event) {
		loadIframeToTabpanel(tabpanelIpaymu, "/pages/master/ipaymu/ipaymu_request.zul");
	}

	public void onFaspay(Event event) {
		loadIframeToTabpanel(tabpanelFaspay, "/pages/master/faspay/faspay_request.zul");
	}

	public void onJatelindo(Event event) {
		loadIframeToTabpanel(tabpanelJatelindo, "/pages/master/jatelindo/jatelindo_request.zul");
	}

	public void onCimb(Event event) {
		loadIframeToTabpanel(tabpanelCimb, "/pages/master/cimb/cimb_request.zul");
	}

	public void onBni(Event event) {
		loadIframeToTabpanel(tabpanelBni, "/pages/master/bni/bni_request.zul");
	}

	public void onBsi(Event event) {
		loadIframeToTabpanel(tabpanelBsi, "/pages/master/bsi/bsi_request.zul");
	}

	public void onBri(Event event) {
		loadIframeToTabpanel(tabpanelBri, "/pages/master/bri/bri_request.zul");
	}

	public void onUploadDanDownloadCicilanMahasiswa(Event event) {
		if (tabpanelUploadDanDownloadCicilanMahasiswa.getChildren().size() == 0) {
			ais.ui.util.MyButtonTabbox btnTab = ais.ui.util.MyButtonTabbox.buat(tabpanelUploadDanDownloadCicilanMahasiswa, "100%", new int[] { 0 });

			{ org.zkoss.zul.Div panel = btnTab.tambahTab(0, "Download Pembayaran", "/img/svg/download.svg");
			  DownloadCicilanMahasiswa laporan = new DownloadCicilanMahasiswa();
			  laporan.setHeight("100%"); laporan.setWidth("100%"); laporan.setParent(panel); }

			{ org.zkoss.zul.Div panel = btnTab.tambahTab(1, "Upload Pembayaran", "/img/svg/upload.svg");
			  UploadCicilanMahasiswa upload = new UploadCicilanMahasiswa();
			  upload.setHeight("100%"); upload.setWidth("100%"); upload.setParent(panel); }
		}
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		tbmuser = Common.getCurrentUser();

		if (rowTanggalValidasi != null)
			if (rowTanggalValidasi != null) { rowTanggalValidasi.setVisible(false); }

		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getMahasiswa() == null) {
			edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}

		Session session = null;
		try {
			session = HibernateUtil.openSession();
			if (execution.getParameter("buktiPembayaran") != null) {
				buktiPembayaran = (BuktiPembayaran) session.createCriteria(BuktiPembayaran.class)
						.add(Restrictions.idEq(Long.parseLong(execution.getParameter("buktiPembayaran"))))
						.uniqueResult();
			}

			if (tabpanelDepositMahasiswa != null) {
				tabpanelDepositMahasiswa.setVisible(true);
				tabpanelDepositMahasiswa.getLinkedTab().setVisible(true);
			}

			if (tombolRefresh != null)
				tombolRefresh.setAttribute("janganDisabled", true);

			if (keranjang != null) {
				boolean aktifkan_keranjang_pembayaran = Common.bolehKonfigurasi("aktifkan_keranjang_pembayaran", Konfigurasi.TIDAK_AKTIF);
				keranjang.setVisible(aktifkan_keranjang_pembayaran);
				tabpanelKeranjangPembayaran.setVisible(aktifkan_keranjang_pembayaran);
			}

			String idMahasiswa = execution.getParameter("mahasiswa");
			if (idMahasiswa != null && !idMahasiswa.trim().isEmpty()) {
				mahasiswaAktif = (Mahasiswa) session.createCriteria(Mahasiswa.class)
						.add(Restrictions.idEq(Long.parseLong(idMahasiswa))).uniqueResult();
			}

			String paramJk = execution.getParameter("jk");
			if (paramJk != null && !paramJk.trim().isEmpty()) {
				this.jk = (JenisKegiatan) session.createCriteria(JenisKegiatan.class)
						.add(Restrictions.idEq(Long.parseLong(paramJk))).uniqueResult();
			}

			String smt = execution.getParameter("smt");

			modeWizardRingkas = "1".equals(execution.getParameter("wizard"));

			if (tbmuser != null && tbmuser.getMahasiswa() != null)
				mahasiswaAktif = tbmuser.getMahasiswa();

			if (mahasiswaAktif == null) {
				if (this.session.getAttribute("usersTemp") == null
						|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
					this.session.removeAttribute("usersTemp");
					Common.goLogoff();
					return;
				}
			}

			jenisPembayaran.setReadonly(true);

			nim.setEventListener(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onCariMahasiswa(arg0);
						}
					});
				}
			});

			/*
			 * Tata letak PORTAL responsif (reuse ais.ui.util.PortalUiHelper —
			 * komponen yang sama dengan halaman e-Learning): data/tagihan di kiri dan
			 * daftar pembayaran di kanan pada desktop. CSS portal menumpuk keduanya
			 * menjadi satu kolom pada layar mobile.
			 */
			// TINGGI PENUH: #portalHost secara bawaan dibatasi .ais-portal-host (max-height 72vh)
			// sebagai pengaman konteks POPUP. Di halaman penuh batas itu MEMOTONG daftar tagihan
			// padahal ruang di bawahnya masih kosong. Kelas tambahan ini melepas max-height dan
			// memakai height:100% -> tinggi MENGIKUTI PARENT (center borderlayout). Bila rantai
			// height:100% tak resolve (include berlapis di popup), jatuh ke auto sehingga isi
			// mengalir penuh & wadah induk (overflow:auto) yang men-scroll -> tetap tak terpotong.
			tambahSclass(portalHost, "ais-portal-host-tinggi-penuh");

			MyPortallayout portal = ais.ui.util.PortalUiHelper.portal(portalHost);
			portal.setSclass("ais-pembayaran-mahasiswa-layout");
			MyPortalchildren kolMahasiswa = ais.ui.util.PortalUiHelper.kolom(portal, "50%");
			MyPortalchildren kolPembayaran = ais.ui.util.PortalUiHelper.kolom(portal, "50%");
			MyPortalchildren kolAnalisis = ais.ui.util.PortalUiHelper.kolom(portal, "100%");

			org.zkoss.zk.ui.Component bodyMahasiswa = ais.ui.util.PortalUiHelper.panel(kolMahasiswa,
					"Data Mahasiswa & Tagihan",
					"Identitas mahasiswa beserta rincian biaya yang harus dibayar.");
			if (center != null) {
				center.setParent(bodyMahasiswa);
				center.setVisible(true);
			}

			panelMencicil = ais.ui.util.PortalUiHelper.panel(kolPembayaran,
					"Daftar Pembayaran / Angsuran",
					"Catatan tiap pembayaran/cicilan dan tombol untuk membayar.");
			buatPlaceholderPanel(panelMencicil, "💳", "Belum ada data pembayaran");

			panelAnalisis = ais.ui.util.PortalUiHelper.panel(kolAnalisis,
					"Analisis & Dasbor Pembayaran",
					"Ringkasan mudah: sudah dibayar berapa, sisanya berapa, dan tren pembayarannya.");
			buatPlaceholderPanel(panelAnalisis, "📊", "Belum ada analisis");

			ais.ui.util.MyPortalchildren kolCaraBayar = null;
			if (modeWizardRingkas) {
				kolCaraBayar = ais.ui.util.PortalUiHelper.kolom(portal, "100%");
				caraBayarHost = ais.ui.util.PortalUiHelper.panel(kolCaraBayar, "Pilih Cara Pembayaran",
						"Pilih metode pembayaran: Tunai / Virtual Account / Online, lalu ikuti instruksinya.");
				terapkanModeWizardRingkas(comp, kolMahasiswa, kolPembayaran, kolAnalisis, kolCaraBayar);
			}

			jenisPembayaran = Common.initJenisPembayaranMahasiswa(jenisPembayaran);

			int maxSemesterPilihan = 25;
			try {
				maxSemesterPilihan = Integer
						.parseInt(Common.getKonfigurasi("max_semester_pilihan", "25").getNilai().trim());
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel("Smt saat ini");
			comboitem.setValue(null);
			semesterPilihan.appendChild(comboitem);
			semesterPilihan.setSelectedItem(comboitem);

			for (int i = 1; i < maxSemesterPilihan; i++) {
				comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel(i + "");
				comboitem.setValue(i);
				semesterPilihan.appendChild(comboitem);
			}

			semesterPilihan.setReadonly(true);
			semesterPilihan.setAttribute("janganDisabled", true);
			tanggalValidasi.setAttribute("janganDisabled", true);
			jenisPembayaran.setAttribute("janganDisabled", true);
			semester.setAttribute("janganDisabled", true);

			if (this.jk != null && this.jk.getId() != null && smt != null && !smt.trim().isEmpty()) {
				Common.selectComboItem(true, jenisPembayaran, this.jk);
				Common.selectComboItem(true, semesterPilihan, Integer.parseInt(smt));
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						jenisPembayaran.setDisabled(true);
						semesterPilihan.setDisabled(true);
					}
				});
			}

			if (buktiPembayaran != null && buktiPembayaran.getMahasiswa() != null) {
				Mahasiswa mhsAktif = buktiPembayaran.getMahasiswa();
				nim.setVisible(false);
				nim.setAttribute("mahasiswa", mhsAktif);
				nim.setAttribute("calonMahasiswa", mhsAktif);
				nim.setValue(mhsAktif.getNim() + " - " + mhsAktif.getNama());
				nim.setId("calonmhs_" + mhsAktif.getId());
				nim.setDisabled(true);

				Common.selectComboItem(true, jenisPembayaran, buktiPembayaran.getJenisKegiatan());
				jenisPembayaran.setDisabled(true);
				Common.selectComboItem(true, semesterPilihan, buktiPembayaran.getSemester());
				semesterPilihan.setDisabled(true);

				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						onCariMahasiswa(arg0);
						jenisPembayaran.setDisabled(true);
						semesterPilihan.setDisabled(true);
					}
				});
			} else if (mahasiswaAktif != null) {
				nim.setVisible(false);
				nim.setAttribute("mahasiswa", mahasiswaAktif);
				nim.setAttribute("calonMahasiswa", mahasiswaAktif);
				nim.setValue(mahasiswaAktif.getNim() + " - " + mahasiswaAktif.getNama());
				nim.setId("calonmhs_" + mahasiswaAktif.getId());
				nim.setDisabled(true);

				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						onCariMahasiswa(arg0);
					}
				});
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try {
					session.clear();
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
				try {
					session.disconnect();
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
				try {
					session.close();
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private boolean checkKondisiSebelumbayarBaru() throws Exception {
		selectedSemester = (semester.getValue() == null || semester.getValue().trim().isEmpty()) ? null
				: Integer.parseInt(semester.getValue());
		if (selectedSemester == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu memilih semester terlebih dahulu. Langkah yang dapat dilakukan: (1) buka kolom pilihan semester; (2) pilih semester yang sesuai; (3) lanjutkan kembali proses yang sedang dijalankan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
				: jenisPembayaran.getSelectedItem().getValue());

		if (countPengaturanBulanan > 0 && cicilanPembayarans.isEmpty()) {
			inputSesuaiTagihanBulanan(null);
		} else if (gridCicilan != null && countPengaturanBulanan == 0) {
			boolean ada = false;
			List<Row> rows = gridCicilan.getRows().getChildren();
			for (Row row : rows) {
				try {
					MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
					CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");
					cicilanPembayaran.setNilai(jumlahCicilan.getValue());
					row.setValign("top");
					row.setAttribute("cicilanPembayaran", cicilanPembayaran);
					ada |= (jumlahCicilan != null && row.isVisible());
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
			if (!ada)
				inputSesuaiTagihan();
		}

		Session session = null;
		try {
			session = HibernateUtil.openSession();
			JenisKegiatan kegiatanDaftarUlangMahasiswaBaru = pembayaranUtil
					.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);

			if (Common.bolehKonfigurasi("chek_tunggakan_sebelum_bayar", Konfigurasi.TIDAK_AKTIF)) {
				List<TunggakanMahasiswa> tunggakanMahasiswas = pembayaranUtil.getTunggakanMahasiswa(
						new JenisKegiatan[] { kegiatanDaftarUlangMahasiswaBaru, jenisKegiatan }, mahasiswa, session);
				for (TunggakanMahasiswa tunggakanMahasiswa : tunggakanMahasiswas) {
					if (tunggakanMahasiswa.getSemester() < selectedSemester) {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, mahasiswa ini masih memiliki tunggakan pada semester {V1} sehingga pembayaran belum dapat dilanjutkan. Langkah yang dapat dilakukan: (1) mohon lunasi terlebih dahulu tunggakan pada semester tersebut; (2) periksa rincian tunggakan pada menu informasi pembayaran; (3) apabila memerlukan bantuan, mohon hubungi bagian keuangan.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
								tunggakanMahasiswa.getSemester());
						return false;
					}
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try {
					session.clear();
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
				try {
					session.disconnect();
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
				try {
					session.close();
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
		}

		return true;
	}

	public boolean apakah0(boolean chek) throws Exception {
		jumlahYangAkanDibayar = hitungJumlahYangAkanDibayarDariTampilan();

		if (chek && Math.abs(jumlahYangAkanDibayar) < 0.01) {
			MyMessageboxConfig.show(
					"Belum ada nilai pembayaran baru yang dapat dikirim ke bank atau payment gateway. Silakan klik Pilih Semua, pilih tagihan yang akan dibayar, atau isi nilai cicilan terlebih dahulu.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		return true;
	}

	/** Tagihan bulanan dapat valid walau sumber DetailBiaya nonbulanan kosong. */
	private boolean adaTagihanTerpilihUntukProses() {
		return (detailBiayas != null && !detailBiayas.isEmpty())
				|| Math.abs(jumlahYangAkanDibayar) >= 0.01;
	}

	private static void buatPlaceholderPanel(org.zkoss.zk.ui.Component host, String ikon, String pesan) {
		if (host == null) return;
		org.zkoss.zul.Html ph = new org.zkoss.zul.Html();
		ph.setContent("<div style='text-align:center;padding:24px 12px 16px;color:#94a3b8;font-size:12px;line-height:1.6;'>"
			+ "<div style='font-size:22px;margin-bottom:8px;color:#e2e8f0;'>" + ikon + "</div>"
			+ "<div style='font-weight:600;color:#475569;font-size:12px;margin-bottom:4px;'>" + pesan + "</div>"
			+ "Pilih mahasiswa terlebih dahulu.</div>");
		ph.setParent(host);
	}

	private void reset() {
		if (rowNim != null)
			rowNim.setVisible(false);
		if (rowJenisKuliah != null)
			rowJenisKuliah.setVisible(false);
		if (rowJenjang != null)
			rowJenjang.setVisible(false);
		if (rowProdi != null)
			rowProdi.setVisible(false);
		if (rowSemester != null)
			rowSemester.setVisible(false);
		if (rowTahunMasuk != null)
			rowTahunMasuk.setVisible(false);
		if (rowTahunAkademik != null)
			rowTahunAkademik.setVisible(false);
		if (rowTanggalValidasi != null)
			rowTanggalValidasi.setVisible(false);
		if (rowValidator != null)
			rowValidator.setVisible(false);
		if (rowPengurangan != null)
			rowPengurangan.setVisible(false);
		if (rowKeterangan != null)
			rowKeterangan.setVisible(false);
		if (rowListBiaya != null)
			rowListBiaya.setVisible(false);
		if (rowMobile != null)
			rowMobile.setVisible(true);
	}

	private StatusMahasiswa statusMahasiswaPembayaranEfektif(StatusMahasiswa status) {
		return PembayaranUtilHelper.statusMahasiswaPembayaranEfektif(status);
	}

	/**
	 * Merender identitas akademik dan link analisis status. Ringkasan selalu muncul untuk status
	 * apa pun, sedangkan klik pada teks dalam tanda kurung membuka popup dari helper reusable.
	 * Snapshot analisis dibuat sebelum method ini dipanggil sehingga teks dan popup memakai bukti
	 * yang persis sama dalam satu render layar.
	 */
	private void tampilkanIdentitasJenisKuliah(String program, String status, String statusAwal,
			String jenisSeleksi, String gelombang, String semesterMasuk, String kelas,
			final ais.action.master.helper.HistoryStatusMahasiswaUtil.AnalisisStatusMahasiswa analisis) {
		Common.clear(jenisKuliah);
		jenisKuliah.setWidth("100%");
		jenisKuliah.setSpacing("2px");
		new Label("Jenis Kuliah : " + nilaiTampil(program)).setParent(jenisKuliah);

		org.zkoss.zul.Div barisStatus = new org.zkoss.zul.Div();
		barisStatus.setWidth("100%");
		barisStatus.setStyle("white-space:normal;line-height:1.45;");
		barisStatus.setParent(jenisKuliah);
		new Label("Status : " + nilaiTampil(status) + " ").setParent(barisStatus);
		A linkAnalisis = new A();
		linkAnalisis.setParent(barisStatus);
		ais.ui.util.StatusMahasiswaAnalisisPopupHelper.pasangLink(linkAnalisis, analisis);

		new Label("Status Awal : " + nilaiTampil(statusAwal)).setParent(jenisKuliah);
		new Label("Jenis Seleksi : " + nilaiTampil(jenisSeleksi)).setParent(jenisKuliah);
		new Label("Gelombang : " + nilaiTampil(gelombang)).setParent(jenisKuliah);
		new Label("Semester Masuk : " + nilaiTampil(semesterMasuk)).setParent(jenisKuliah);
		new Label("Kelas : " + nilaiTampil(kelas)).setParent(jenisKuliah);
	}

	private String nilaiTampil(String value) {
		return value == null || value.trim().isEmpty() ? "-" : value.trim();
	}

	private EventListener eventListener = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			Common.clear(rowListBiaya);
			Integer smt = (semester.getValue() == null || semester.getValue().isEmpty()) ? null
					: Integer.parseInt(semester.getValue());
			if (smt == null) {
				MyMessageboxConfig.show("Mohon Bapak/Ibu memilih semester terlebih dahulu. Langkah yang dapat dilakukan: (1) buka kolom pilihan semester; (2) pilih semester yang sesuai; (3) lanjutkan kembali proses yang sedang dijalankan.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return;
			}

			Date tanggal = WaktuUtil.getDate();
			final JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
					: jenisPembayaran.getSelectedItem().getValue());

			if (jenisKegiatan == null) {
				MyMessageboxConfig.show("Mohon Bapak/Ibu memilih jenis pembayaran terlebih dahulu. Langkah yang dapat dilakukan: (1) buka kolom pilihan jenis pembayaran; (2) pilih jenis pembayaran yang sesuai; (3) lanjutkan kembali proses yang sedang dijalankan.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return;
			}

			Integer tahap = PengaturanPembayaranBulanan.hitungTahap(mahasiswa, smt,
					Common.BULAN[ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH)]);
			Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
			Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
			Integer tahunAkademikMulai = Common.getTahunAkademik(Integer.parseInt(semester.getValue()),
					tahunAngkatanMhs, semesterMulai, mahasiswa.getSemesterMulai());
			String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
			labelTahunAkademik.setValue(tahunAkademik);

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, smt, tahap,
					jenisKegiatan != null && jenisKegiatan.getUntukBayarSP() ? Perkuliahan.SEMESTER_PENDEK : null,
					refresh);
			tempHistoryStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil
					.currentStatus(krsMahasiswa, refresh);
			statusmahasiswa = tempHistoryStatusMahasiswa.getStatusMahasiswa();
			PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa.ambilCuti(smt, tahap, false);
			if (pendaftaranCutiMahasiswa != null && pendaftaranCutiMahasiswa.getPersetujuan() != null
					&& pendaftaranCutiMahasiswa.getPersetujuan())
				statusmahasiswa = ConstantValues.CUTI;
			StatusMahasiswa statusMahasiswaTampil = statusmahasiswa;
			statusmahasiswa = statusMahasiswaPembayaranEfektif(statusmahasiswa);

			String statusNama = statusMahasiswaTampil != null ? statusMahasiswaTampil.getNama() : "-";
			final ais.action.master.helper.HistoryStatusMahasiswaUtil.AnalisisStatusMahasiswa analisisStatus =
					ais.action.master.helper.HistoryStatusMahasiswaUtil.analisisStatus(krsMahasiswa,
							tempHistoryStatusMahasiswa, statusMahasiswaTampil);
			String statusAwalNama = (tempHistoryStatusMahasiswa != null
					&& tempHistoryStatusMahasiswa.getStatusAwalMahasiswa() != null)
							? tempHistoryStatusMahasiswa.getStatusAwalMahasiswa().getNama()
							: "-";
			String progNama = tempHistoryStatusMahasiswa != null ? tempHistoryStatusMahasiswa.getProgram() : "-";

			// Tampilkan tiap data akademik dengan LABEL-nya sendiri (multi-baris) agar mudah dikenali.
			String jenisSeleksiNama = mahasiswa.getJenisSeleksi() == null ? "-"
					: mahasiswa.getJenisSeleksi().getNama();
			String gelombangNama = mahasiswa.getGelombangPendaftaran() == null ? "-"
					: mahasiswa.getGelombangPendaftaran().getNama();
			String semesterMasukNama = mahasiswa.getSemesterMulai() == null ? "-"
					: (mahasiswa.getSemesterMulai() + "");
			String kelasNama = mahasiswa.getKelas() == null || mahasiswa.getKelas().trim().isEmpty() ? "-"
					: mahasiswa.getKelas();
			tampilkanIdentitasJenisKuliah(progNama, statusNama, statusAwalNama, jenisSeleksiNama,
					gelombangNama, semesterMasukNama, kelasNama, analisisStatus);

			Serializable[] serializables = pembayaranUtil.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(tanggal,
					jenisKegiatan, mahasiswa.getJenjang(), tahunAkademik, smt.intValue() % 2 != 0,
					mahasiswa.getJenisSeleksi(), progNama, mahasiswa.getNim(), null);

			jadwalPembayaran = null;
			if (serializables != null && serializables.length > 0) {
				jadwalPembayaran = (JadwalPembayaran) serializables[0];
			}

			if (jadwalPembayaran == null) {
				// BLOK LANGSUNG: jadwal tidak ditemukan utk konteks ini (belum dibuat, sudah
				// selesai, atau belum dimulai) -> JANGAN fallback ke kegiatan.getJadwalPembayaran()
				// (jadwal lama yang menempel di Kegiatan bisa milik TA lain / sudah kedaluwarsa).
				return;
			}

			rowListBiaya.setVisible(true);
			if (semester.getValue() == null || !Common.isNumber(semester.getValue())) {
				MyMessageboxConfig.show("Mohon maaf, data semester mahasiswa belum tersedia. Langkah yang dapat dilakukan: (1) pastikan semester mahasiswa telah terisi dengan benar; (2) muat ulang data mahasiswa; (3) apabila memerlukan bantuan, mohon hubungi Administrator sistem.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return;
			}
			listBiaya(rowListBiaya, mahasiswa, kegiatan);
		}
	};

	private EventListener rubahTanggal = new EventListener() {
		@Override
		public void onEvent(Event arg0) throws Exception {
			starat = true;
			Date tanggal = WaktuUtil.getDate();
			final JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
					: jenisPembayaran.getSelectedItem().getValue());

			if (jenisKegiatan == null) {
				MyMessageboxConfig.show("Mohon Bapak/Ibu memilih jenis pembayaran terlebih dahulu. Langkah yang dapat dilakukan: (1) buka kolom pilihan jenis pembayaran; (2) pilih jenis pembayaran yang sesuai; (3) lanjutkan kembali proses yang sedang dijalankan.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return;
			}

			Integer smt = (semester.getValue() == null || semester.getValue().isEmpty()) ? null
					: Integer.parseInt(semester.getValue());
			if (smt == null) {
				MyMessageboxConfig.show("Mohon Bapak/Ibu mengisi semester terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan atau pilih semester pada kolom yang tersedia; (2) periksa kembali kelengkapan data; (3) lanjutkan kembali proses yang sedang dijalankan.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return;
			}

			eventListener.onEvent(arg0);

			Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
			Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
			Integer tahunAkademikMulai = Common.getTahunAkademik(Integer.parseInt(semester.getValue()),
					tahunAngkatanMhs, semesterMulai, mahasiswa.getSemesterMulai());
			String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
			labelTahunAkademik.setValue(tahunAkademik);

			String progNama = tempHistoryStatusMahasiswa != null ? tempHistoryStatusMahasiswa.getProgram() : "-";

			Serializable[] serializables = pembayaranUtil.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(tanggal,
					jenisKegiatan, mahasiswa.getJenjang(), tahunAkademik, smt.intValue() % 2 != 0,
					mahasiswa.getJenisSeleksi(), progNama, mahasiswa.getNim(), null);

			jadwalPembayaran = null;
			if (serializables != null && serializables.length > 0) {
				jadwalPembayaran = (JadwalPembayaran) serializables[0];
			}

			if (jadwalPembayaran == null) {
				// BLOK LANGSUNG: jadwal tidak ditemukan utk konteks ini (belum dibuat, sudah
				// selesai, atau belum dimulai) -> JANGAN fallback ke kegiatan.getJadwalPembayaran()
				// (jadwal lama yang menempel di Kegiatan bisa milik TA lain / sudah kedaluwarsa).
				MyMessageboxConfig.show("Mohon maaf, jadwal pembayaran belum tersedia, telah terlewat, atau belum dimulai. Langkah yang dapat dilakukan: (1) periksa kembali periode jadwal pembayaran yang berlaku; (2) pastikan tanggal saat ini berada dalam rentang jadwal pembayaran; (3) apabila memerlukan bantuan, mohon hubungi bagian keuangan atau Administrator sistem.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}

			Integer s = (semester.getValue() == null || semester.getValue().isEmpty()) ? null
					: Integer.parseInt(semester.getValue());
			starat = JenisKegiatanPrasyaratAction.checkSyarat(mahasiswa, jenisKegiatan, s,
					labelTahunAkademik.getValue(),
					s == null ? null : (s % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL));
			if (!starat) {
				Common.freeze(gridss, true);
				nim.setDisabled(false);
				return;
			}
		}
	};

	public void onCariMahasiswa(final Event event) throws Exception {
		tabungan = 0.0;
//		cicilanPembayaransTemp.clear();
		Session sessionObj = null;

		boolean refreshDiminta = refresh || (event != null
				&& (event.getTarget() instanceof Button || event.getTarget() instanceof Toolbarbutton));
		if (refreshDiminta) {
			try {
				sessionObj = HibernateUtil.openSession();
				if (mahasiswa != null && mahasiswa.getId() != null) {
					mahasiswa.reInitKegiatan(sessionObj);
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			} finally {
				if (sessionObj != null) {
					try {
						sessionObj.clear();
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
					try {
						sessionObj.disconnect();
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
					try {
						sessionObj.close();
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}
			}
			refresh = true;
		} else {
			refresh = false;
		}

		Common.clear(panelMencicil);
		kegiatan = null;
		if (jenisPembayaran.getSelectedItem() == null) {
			reset();
			return;
		}
		jenisPembayaran.setReadonly(true);
		JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
				: jenisPembayaran.getSelectedItem().getValue());

		Session session = null;
		try {
			session = HibernateUtil.openSession();

			mahasiswa = (Mahasiswa) nim.getAttribute("mahasiswa");
			if (mahasiswa == null || mahasiswa.getId() == null) {
				reset();
				return;
			}

//			cicilanPembayaransTemp = mahasiswa.ambilCicilan(refresh);
			tabungan = ais.action.master.sekolah.util.DepositHelper.hitungDeposit(mahasiswa);
			session.refresh(mahasiswa);

			JenisKegiatan kegiatanDaftarUlangMahasiswaBaru = pembayaranUtil
					.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);

			if (Common.bolehKonfigurasi("chek_tunggakan_sebelum_bayar", Konfigurasi.TIDAK_AKTIF)) {
				List<TunggakanMahasiswa> tunggakanMahasiswas = pembayaranUtil.getTunggakanMahasiswa(
						new JenisKegiatan[] { kegiatanDaftarUlangMahasiswaBaru, jenisKegiatan }, mahasiswa, session);
				Common.clear(rowInfoTunggakan);
				if (tunggakanMahasiswas.size() != 0)
					new TunggakanMahasiswaHelper().display(rowInfoTunggakan, tunggakanMahasiswas);
			}

			Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
			String semesterMulaiStr = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

			Integer smt = (Integer) (semesterPilihan.getSelectedItem() == null
					|| semesterPilihan.getSelectedItem().getValue() == null
							? (Common.getSemester(tahunAngkatanMhs, semesterMulaiStr,
									mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai()))
							: semesterPilihan.getSelectedItem().getValue());
			if (smt.equals(0))
				smt = 1;

			if (jenisKegiatan != null && (jenisKegiatan.getMinSmt() > smt || jenisKegiatan.getMaxSmt() < smt)) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, jenis pembayaran \"{V1}\" tidak tersedia untuk semester {V2}. Langkah yang dapat dilakukan: (1) periksa kembali pilihan semester pada kolom yang tersedia; (2) pilih semester yang sesuai dengan ketentuan jenis pembayaran ini; (3) apabila memerlukan bantuan, mohon hubungi Administrator sistem.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						jenisKegiatan.getNamaKegiatan(), smt);
				return;
			}

			kegiatan = mahasiswa.ambilKegiatansRefresh(smt, jenisKegiatan);

			if (rowMobile != null)
				rowMobile.setVisible(false);
			rowNim.setVisible(true);
			labelNimMahasiswa.setValue(mahasiswa.getNim());
			if (labelFotoMahasiswa != null) {
				Common.clear(labelFotoMahasiswa);
				CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(labelFotoMahasiswa);
			}
			labelNamaMahasiswa.setValue(mahasiswa.getNama());

			if (mahasiswa != null) {
				BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
				Object[] hp = new Object[] { biodataMahasiswa == null ? null : biodataMahasiswa.getHp(),
						biodataMahasiswa == null ? null : biodataMahasiswa.getTeleponRumah() };
				String hps = mahasiswa.getTelp();
				try {
					hps = (hp[0] == null || hp[0].toString().trim().equals("08100000000000000000")
							|| hp[0].toString().trim().equals("0000000000") ? "" : hp[0])
							+ (hp[1] == null || hp[1].toString().trim().isEmpty()
									|| hp[1].toString().trim().equals("00000000000000000000")
									|| hp[1].toString().trim().equals("000000000")
											? ""
											: (hp[0] == null || hp[0].toString().trim().isEmpty()
													|| hp[0].toString().trim().equals("08100000000000000000")
													|| hp[0].toString().trim().equals("0000000000") ? "" : " / ")
													+ hp[1]);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				// Tiap data identitas diberi LABEL sendiri (multi-baris) agar mudah dikenali.
				String namaAyahMhs = biodataMahasiswa == null || biodataMahasiswa.getNamaAyah() == null
						|| biodataMahasiswa.getNamaAyah().trim().isEmpty() ? null : biodataMahasiswa.getNamaAyah().trim();
				StringBuilder sbIdentitas = new StringBuilder();
				sbIdentitas.append("Nama : ").append(mahasiswa.getNama() == null ? "-" : mahasiswa.getNama());
				if (hps != null && !hps.trim().isEmpty()) {
					sbIdentitas.append("\nNo. HP : ").append(hps);
				}
				if (mahasiswa.getEmail() != null && !mahasiswa.getEmail().trim().isEmpty()) {
					sbIdentitas.append("\nEmail : ").append(mahasiswa.getEmail());
				}
				if (mahasiswa.getAlamat() != null && !mahasiswa.getAlamat().trim().isEmpty()) {
					sbIdentitas.append("\nAlamat : ").append(mahasiswa.getAlamat());
				}
				if (namaAyahMhs != null) {
					sbIdentitas.append("\nNama Ayah : ").append(namaAyahMhs);
				}
				sbIdentitas.append("\nKewarganegaraan : ")
						.append(mahasiswa.getWarganegara() == null ? "-" : mahasiswa.getWarganegara());
				labelNamaMahasiswa.setMultiline(true);
				labelNamaMahasiswa.setValue(sbIdentitas.toString());

				if (labelTabungan != null) {
					labelTabungan.setValue("Tabungan : " + Common.numberFormat.get().format(tabungan));
					labelTabungan.setVisible(tabungan > 0.1 && !(Common.bolehKonfigurasi("sembunyikan_nominal_tabungan_ke_mahasiswa", ais.database.model.Konfigurasi.AKTIF) && Common.getCurrentUser() != null && (Common.getCurrentUser().getMahasiswa() != null || Common.getCurrentUser().getBiodataCalonMahasiswa() != null)));
				}
			}

			rowJenisKuliah.setVisible(true);
			rowJenjang.setVisible(true);
			Jenjang jenjangProfil = mahasiswa.getJurusan() != null
					? mahasiswa.getJurusan().getJenjang() : mahasiswa.getJenjang();
			labelJenjang.setValue(jenjangProfil == null ? "-" : namaObjekAnalisis(jenjangProfil));
			rowProdi.setVisible(true);
			prodi.setValue(mahasiswa.getJurusan() != null ? mahasiswa.getJurusan().getNama() : "-");
			rowSemester.setVisible(true);
			semester.setValue(smt == null ? "0" : smt.toString());
			rowTahunMasuk.setVisible(true);
			labelTahunMasuk
					.setValue(mahasiswa.getTahunangkatan() != null ? mahasiswa.getTahunangkatan().toString() : "-");
			rowTahunAkademik.setVisible(true);
			rowTanggalValidasi.setVisible(false);
			rowValidator.setVisible(true);
			rowPengurangan.setVisible(true);
			rowKeterangan.setVisible(true);

			Common.clear(rowListBiaya);
			rowListBiaya.setVisible(true);

			if (kegiatan != null && kegiatan.getAmount() > 0.1) {
				tanggalValidasi
						.setValue(kegiatan.getTanggal() != null ? Common.dateFormat6.get().format(kegiatan.getTanggal())
								: Common.dateFormat6.get().format(WaktuUtil.getDate()));
				validator.setValue(kegiatan.getValidator() == null ? "" : kegiatan.getValidator());
				// Mode Wizard: kosongkan keterangan (mhs fokus mengisi nilai bayar);
				// di layar DaftarUlang biasa tetap dipertahankan.
				keterangan.setValue(modeWizardRingkas ? "" : (kegiatan.getKeterangan() == null ? "" : kegiatan.getKeterangan()));

				if (!bolehmencicilBaru) {
					MyMessageboxConfig.show(
							MyMessageboxConfig.format(
									"Perlu diketahui, mahasiswa dengan NIM {V1} telah melakukan pembayaran pada semester {V2}. Silakan periksa kembali data pembayaran mahasiswa yang bersangkutan sebelum melanjutkan.",
									mahasiswa.getNim(), smt),
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									Common.freeze(center, true);
								}
							});
				}
			} else {
				tanggalValidasi.setValue(Common.dateFormat6.get().format(ais.ui.util.WaktuUtil.getDate()));
			}

			tanggalValidasi.addEventListener("onChange", rubahTanggal);
			rubahTanggal.onEvent(null);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try {
					session.clear();
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
				try {
					session.disconnect();
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
				try {
					session.close();
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	/**
	 * Mode ringkas Wizard: sembunyikan kromiun yang tak relevan saat layar ini
	 * di-embed di dalam Wizard Pembayaran, sehingga fokus ke tabel item biaya
	 * (dengan checkbox "Proses Bayar") + panel pembayaran. Memakai trik kelas CSS
	 * ({@code display:none !important}) agar tetap tersembunyi walau baris di-set
	 * visible lagi oleh proses pemuatan tagihan. Logika pembayaran tidak diubah.
	 */
	private void terapkanModeWizardRingkas(Component windowComp, org.zkoss.zk.ui.Component kolMahasiswa,
			org.zkoss.zk.ui.Component kolPembayaran, org.zkoss.zk.ui.Component kolAnalisis,
			org.zkoss.zk.ui.Component kolCaraBayar) {
		try {
			windowComp.appendChild(new Html(
					"<style>.wz-ringkas-hide{display:none !important;}</style>"));

			// Tandai kolom agar Wizard dapat menampilkan TAGIHAN (kiri), PEMBAYARAN/
			// ANGSURAN (kanan), dan CARA BAYAR (tombol bayar) sebagai langkah terpisah.
			tambahSclass(kolMahasiswa, "wz-col-tagihan");
			tambahSclass(kolPembayaran, "wz-col-bayar");
			tambahSclass(kolCaraBayar, "wz-col-carabayar");

			// Kolom dasbor analisis (membentang penuh di bawah) — sembunyikan.
			tambahSclass(kolAnalisis, "wz-ringkas-hide");

			// Tab strip + baris kombo filter (Jenis/Mahasiswa/Semester sudah dipilih di Wizard).
			tambahSclass(windowComp.getFellowIfAny("tabsUtama"), "wz-ringkas-hide");
			tambahSclass(windowComp.getFellowIfAny("grid"), "wz-ringkas-hide");

			// Kombo filter berada di region <north>. MyGrid.init() mengubah north pembungkus
			// MyGrid menjadi KOTAK "Menu" (judul "Menu" + min-height 250px) — di Wizard kombo
			// sudah terisi & disembunyikan, sehingga north menyisakan kotak "Menu" + RUANG
			// KOSONG besar (terlihat jelas di HP). Sembunyikan SELURUH region north (level ZK,
			// bukan CSS) agar borderlayout merebut kembali ruangnya & center mengisi penuh.
			org.zkoss.zk.ui.Component gridKombo = windowComp.getFellowIfAny("grid");
			if (gridKombo != null && gridKombo.getParent() instanceof org.zkoss.zul.North) {
				((org.zkoss.zul.North) gridKombo.getParent()).setVisible(false);
			}

			// Baris-baris profil mahasiswa (biarkan rowListBiaya = tabel item biaya tampil).
			// + rowKeterangan: di Wizard fokus pemilihan item → sembunyikan field Keterangan.
			String[] rowProfil = { "rowNim", "rowJenisKuliah", "rowJenjang", "rowProdi", "rowSemester", "rowTahunMasuk",
					"rowTahunAkademik", "rowTanggalValidasi", "rowValidator", "rowKeterangan" };
			for (String rid : rowProfil) {
				tambahSclass(windowComp.getFellowIfAny(rid), "wz-ringkas-hide");
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Wizard: sembunyikan baris item tagihan yang sudah LUNAS (Kekurangan &lt;= 0). Membaca
	 * nilai dari label "kurang" (Kekurangan yang DITAMPILKAN) — andal karena dipanggil via
	 * timer setelah render &amp; hitung-ulang selesai. Gagal-parse / kosong dianggap masih ada
	 * kekurangan (tetap ditampilkan) agar tak salah menyembunyikan.
	 */
	private void sembunyikanItemLunasWizard() {
		try {
			if (gridss == null || gridss.getRows() == null) {
				return;
			}
			for (Object o : gridss.getRows().getChildren()) {
				if (!(o instanceof Row)) {
					continue;
				}
				Row row = (Row) o;
				Object kurangObj = row.getAttribute("kurang");
				if (!(kurangObj instanceof org.zkoss.zul.Label)) {
					continue;
				}
				String s = ((org.zkoss.zul.Label) kurangObj).getValue();
				double kurang;
				try {
					kurang = (s == null || s.trim().isEmpty()) ? 1.0
							: Common.numberFormat.get().parse(s.trim()).doubleValue();
				} catch (Exception ex) {
					kurang = 1.0; // gagal baca → anggap masih ada kekurangan (tetap tampil)
				}
				if (kurang <= 0.0) {
					row.setVisible(false);
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Tambahkan kelas CSS tanpa menimpa sclass yang sudah ada. */
	private void tambahSclass(org.zkoss.zk.ui.Component c, String cls) {
		if (!(c instanceof org.zkoss.zk.ui.HtmlBasedComponent)) {
			return;
		}
		org.zkoss.zk.ui.HtmlBasedComponent h = (org.zkoss.zk.ui.HtmlBasedComponent) c;
		String s = h.getSclass();
		h.setSclass((s == null || s.trim().isEmpty()) ? cls : s + " " + cls);
	}

	/** Escape teks untuk ringkasan HTML Wizard. */
	private static String escHtmlWizard(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	public void listBiaya(final Component comp, final Mahasiswa mahasiswa, final Kegiatan kegiatan) throws Exception {
		SatuanKerja satuanKerja = Common.getSatuanKerja();
		jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
				: jenisPembayaran.getSelectedItem().getValue());

		gridCicilan = new MyGrid();
		this.mahasiswa = mahasiswa;

		if (kegiatan != null && kegiatan.getValidator() != null)
			validator.setValue(kegiatan.getValidator());

		final ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		// Wizard: groupbox (induk LANGSUNG tabel) jadi wadah SCROLL MENDATAR. Di HP grid
		// (min-width 520) lebih lebar dari groupbox → groupbox menggulir mendatar, sehingga
		// tabel TAK terpotong oleh sel baris grid induk (yang overflow:hidden).
		groupbox.setStyle("min-height: 200px;"
				+ (modeWizardRingkas ? "overflow-x:auto;-webkit-overflow-scrolling:touch;" : ""));
		groupbox.appendChild(new MyCaptionStyled("Daftar Biaya"));
		groupbox.setWidth("100%");
		groupbox.setParent(comp);

		Hbox btn = new Hbox();
		btn.setParent(groupbox);
		if (modeWizardRingkas) {
			btn.setSclass("wz-ringkas-hide");
		}
		// Wadah info "mode tagihan" (BULANAN vs BUKAN BULANAN) — sengaja dibuat KOSONG di
		// sini agar posisinya tepat di bawah baris tombol; isinya diisi belakangan oleh
		// isiInfoModeTagihan() setelah countPengaturanBulanan & dataTagihanData termuat.
		final ais.ui.util.MyDiv infoModeTagihan = new ais.ui.util.MyDiv();
		infoModeTagihan.setParent(groupbox);
		Button button = new MyToolbarbuttonConfig("Lihat Tagihan", "/img/Finance-Invoice-icon.png");
		button.setParent(btn);
		button.setAttribute("janganDisabled", true);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				// PERBAIKAN (data mahasiswa tertukar saat "Lihat Tagihan"): sebelumnya dibuka via
				// IFRAME + URL query-string (?mahasiswa=<id>), dengan alasan awal: konten
				// InformasiPembayaran yang BERAT (chart spider SVG + dasbor + riwayat) perlu dimuat
				// sebagai halaman TERPISAH agar respons AJAX dari KLIK ini tetap kecil (versi inline
				// lama sempat memasukkan seluruh render berat ke SATU respons AU yang bisa terpotong
				// proxy -> popup "server out of service"). Sekarang dipanggil LANGSUNG sebagai method
				// static (InformasiPembayaranMahasiswaAction.onViewExternal, pola sama dgn
				// SetingBiayaAction.onAddExternal) yang mengoper objek mahasiswa APA ADANYA (bukan
				// di-serialisasi ke ID di URL lalu di-parse ulang) -- menghilangkan celah ID salah
				// ter-embed di URL yang dicurigai sbg penyebab data mahasiswa tertukar. Render
				// beratnya TETAP di-defer lewat AsyncTaskManager (timer terpisah, sama seperti alur
				// .zul lama) sehingga respons klik tombol ini tetap kecil -- risiko "response terlalu
				// besar" yang jadi alasan iframe dulu TIDAK kembali muncul.
				ais.action.master.InformasiPembayaranMahasiswaAction.onViewExternal(mahasiswa, null,
					jenisKegiatan);
			}
		});

		if (kegiatan == null || kegiatan.getId() == null) {
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(final Event arg0) throws Exception {
					try {
						detailPembayaranMahasiswaRenderer.buatBaruJikaBelumAda();
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event a) throws Exception {
							refresh = true;
							onCariMahasiswa(arg0);
						}
					});
				}
			}, "Loading tagihan..", false, 2000);
		}

		button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		button.setParent(btn);
		button.setAttribute("janganDisabled", true);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(final Event arg0) throws Exception {
				try {
					detailPembayaranMahasiswaRenderer.buatBaruJikaBelumAda();
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event a) throws Exception {
						refresh = true;
						onCariMahasiswa(arg0);
					}
				});
			}
		});

		buttonReset = new MyToolbarbuttonConfig("Reset", "/img/Business-Process-icon.png");
		buttonReset.setParent(btn);
		buttonReset.setAttribute("janganDisabled", true);
		buttonReset.setVisible(kegiatan != null && kegiatan.getId() != null && tbmuser != null
				&& tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		buttonReset.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(final Event arg0) throws Exception {
				MyMessageboxConfig.show(
						"Apakah Bapak/Ibu yakin ingin mengembalikan tagihan ini ke tagihan awal (default) sesuai billing pembayaran? Nilai tagihan akan disesuaikan kembali ke ketentuan awal. Silakan tekan OK untuk melanjutkan, atau Batal untuk membatalkan.",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {
										detailPembayaranMahasiswaRenderer.buatBaruJikaBelumAda();
									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
									}
									Common.createDefaultTimer(new EventListener() {
										@Override
										public void onEvent(Event arg01) throws Exception {
											Session session = null;
											try {
												session = HibernateUtil.openSession();
											Transaction tx = session.beginTransaction();
												KegiatanHelper.checkKegiatanMahasiswa(kegiatan,
														kegiatan.getJenisKegiatan(), mahasiswa, kegiatan.getSemster(),
														kegiatan.getTahunAkademik(), true,
														kegiatan.getJadwalPembayaran(), true, false, null, session);
											if (tx != null && tx.isActive() && !tx.wasCommitted() && !tx.wasRolledBack()) {
												tx.commit();
											}
											} catch (Exception e) {
												ais.common.Common.tampilErrorJikaAdmin(e);
											} finally {
											ais.database.hibernate.HibernateUtil.closeSessionQuietly(session);
											}
											refresh = true;
											onCariMahasiswa(arg0);
										}
									});
								}
							}
						});
			}
		});

		sesuaikanDenganTagihan = new MyToolbarbuttonConfig("Pilih Semua", "/img/svg/check2.svg");
		sesuaikanDenganTagihan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				inputSesuaiTagihan();
			}
		});
		btn.appendChild(sesuaikanDenganTagihan);

		sesuaikanDenganTagihanBulanan = new MyToolbarbuttonConfig("Pilih Pemua", "/img/svg/check2.svg");
		sesuaikanDenganTagihanBulanan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				inputSesuaiTagihanBulanan(null);
			}
		});
		btn.appendChild(sesuaikanDenganTagihanBulanan);

		if (kegiatan != null && kegiatan.getId() != null && tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getSiswa() == null) {
			RevisiHelper.createNewRevisi(Kegiatan.class, kegiatan, "History").setParent(btn);
		}

		// Tombol Wizard: buka Wizard Pembayaran 5-langkah mandiri (WizardPembayaranMhsHelper)
		// untuk mahasiswa yang sedang dipilih; tagihan dimuat ulang saat wizard ditutup.
		// Gerbang ON/OFF: Konfigurasi > Pembayaran Mahasiswa > "Wizard Pembayaran Mahasiswa".
		if (mahasiswa != null && mahasiswa.getId() != null
				&& ais.action.master.helper.WizardPembayaranMhsHelper.aktif()) {
			MyToolbarbuttonConfig btnWizardBayar = new MyToolbarbuttonConfig("Wizard", "/img/Finance-Invoice-icon.png");
			btnWizardBayar.setAttribute("janganDisabled", true);
			btnWizardBayar.setTooltiptext("Buka Wizard Pembayaran - bayar tagihan langkah demi langkah");
			btnWizardBayar.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					ais.action.master.helper.WizardPembayaranMhsHelper.buka(mahasiswa, new EventListener() {
						@Override
						public void onEvent(Event ev) throws Exception {
							Common.createDefaultTimer(new EventListener() {
								@Override
								public void onEvent(Event a) throws Exception {
									refresh = true;
									onCariMahasiswa(a);
								}
							});
						}
					});
				}
			});
			btnWizardBayar.setParent(btn);
		}


		// Wizard (langkah 2): toolbar lengkap di atas disembunyikan (wz-ringkas-hide), tapi
		// sediakan tombol "Refresh" yang TERLIHAT — fungsinya identik dengan tombol Refresh di
		// DaftarUlangMahasiswa*Action (buatBaruJikaBelumAda + muat ulang tagihan dgn refresh=true).
		if (modeWizardRingkas) {
			Hbox btnWizard = new Hbox();
			btnWizard.setStyle("margin:2px 0 8px 2px;");
			btnWizard.setParent(groupbox);
			MyToolbarbuttonConfig refreshWizard = new MyToolbarbuttonConfig("Refresh",
					"/img/Button-Refresh-icon.png");
			refreshWizard.setAttribute("janganDisabled", true);
			refreshWizard.setParent(btnWizard);
			refreshWizard.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(final Event arg0) throws Exception {
					try {
						detailPembayaranMahasiswaRenderer.buatBaruJikaBelumAda();
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event a) throws Exception {
							refresh = true;
							onCariMahasiswa(arg0);
						}
					});
				}
			});
		}

		Common.insertCombo(akun = new Combobox(), "nama", "akun", JenisPembayaran.class,
				Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		hboxJenisPembayaran = new Hbox(new Component[] { new Label(ais.common.Common.getBahasaConfig("Cara Pembayaran : ")), akun });
		hboxJenisPembayaran.setVisible(false);
		akun.setCols(50);
		hboxJenisPembayaran.setParent(groupbox);

		gridss = new MyGrid();
		gridss.setMold("paging");
		gridss.setPageSize(1000);
		gridss.setParent(groupbox);
		gridss.setWidth("100%");
		// HP (Wizard): tandai grid agar tabel dikonversi jadi 1 KOLOM (kartu vertikal) via CSS.
		// Gerbang pakai isMobile ATAU isAsliMobile (UA perangkat) supaya tetap aktif walau
		// flag sesi sedang "mode desktop" di HP.
		// du-nowarna: matikan pewarnaan status (belang) → tampil default (lihat css_utama).
		gridss.setSclass(modeWizardRingkas && (Common.isMobile() || Common.isAsliMobile())
				? "dgrid du-nowarna wz-stack-hp" : "dgrid du-nowarna");

		Columns columns = new Columns();
		columns.setParent(gridss);
		MyColumnConfig column;

		// Kolom toggle "Bayar?" mode Wizard: PALING KIRI agar mahasiswa langsung melihat
		// pilihan bayar tanpa menggulir ke kanan (di HP kolom kanan kerap di luar layar).
		if (modeWizardRingkas) {
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Bayar?");
			column.setWidth("64px");
			column.setAlign("center");
		}

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Item Biaya");
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tagihan");
		column.setWidth("22%");
		column.setAlign("right");
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dibayar");
		column.setWidth("18%");
		column.setAlign("right");
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kekurangan");
		column.setWidth("18%");
		column.setAlign("right");

		Double sumBiaya = 0.0;
		Foot foot = new Foot();
		foot.setParent(gridss);

		// Footer kolom toggle (kiri) — kosong, agar selaras dgn kolom "Bayar?" Wizard.
		Footer footer;
		if (modeWizardRingkas) {
			footer = new Footer();
			footer.setParent(foot);
		}

		footer = new Footer();
		footer.setParent(foot);
		labelFooterItemBiaya = new MyLabelBoldAja();
		labelFooterItemBiaya.setParent(footer);
		labelFooterItemBiaya.setValue("Total");

		footer = new Footer();
		footer.setParent(foot);
		labelFooterTagihan = new MyLabelBoldAja();
		labelFooterTagihan.setParent(footer);
		labelFooterTagihan.setStyle("font-weight: bold;text-align: right;");
		labelFooterTagihan.setWidth("100%");

		footer = new Footer();
		footer.setParent(foot);
		labelFooterDibayarAja = new MyLabelBoldAja();
		labelFooterDibayarAja.setParent(footer);
		labelFooterDibayarAja.setValue(sumBiaya.toString());
		labelFooterDibayarAja.setStyle("font-weight: bold;text-align: right;");
		labelFooterDibayarAja.setWidth("100%");

		footer = new Footer();
		footer.setParent(foot);
		labelFooterKekurangan = new MyLabelBoldAja();
		labelFooterKekurangan.setParent(footer);
		labelFooterKekurangan.setStyle("font-weight: bold;text-align: right;");
		labelFooterKekurangan.setWidth("100%");

		Vbox vbox = new Vbox();
		vbox.setParent(groupbox);
		terbilang = new Label();
		terbilang.setParent(vbox);
		terbilang.setWidth("100%");
		terbilang.setVisible(false);
		terbilangTagihan = new Label();
		terbilangTagihan.setParent(vbox);
		terbilangTagihan.setWidth("100%");
		terbilangTagihan.setVisible(false);
		terbilangSisa = new Label();
		terbilangSisa.setParent(vbox);
		terbilangSisa.setWidth("100%");
		terbilangSisa.setVisible(false);
		terbilangSisaPersen = new Label();
		terbilangSisaPersen.setParent(vbox);
		terbilangSisaPersen.setWidth("100%");
		terbilangSisaPersen.setVisible(false);

		if (jenisKegiatan != null) {
			itemBiayas = new HashMap<Long, DetailBiaya>();
			// Tagihan mahasiswa harus selalu mengikuti pilihan Item Biaya terbaru.
			// Cache lama dapat masih memuat item yang sudah dilepas dari Setting Biaya.
			final boolean muatDariSettingBiayaTerbaru = true;
			detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa,
					Integer.parseInt(semester.getValue()), jenisKegiatan, muatDariSettingBiayaTerbaru);
			boolean nimDikecualikan = PengecualianTagihanList.adalah(detailBiayas);

			for (Object o : detailBiayas) {
				if (o instanceof DetailBiaya) {
					DetailBiaya detailBiaya = (DetailBiaya) o;
					if (detailBiaya != null && detailBiaya.getItemBiaya() != null)
						itemBiayas.put(detailBiaya.getId(), detailBiaya);
				}
			}

			Session session = null;
			try {
				session = HibernateUtil.openSession();
				countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, mahasiswa, jenisKegiatan,
						Integer.parseInt(semester.getValue()), detailBiayas, muatDariSettingBiayaTerbaru, false);
				pengurangan = new ArrayList<MyDoubleboxMin>();

				Collection biayaBulanan = null;
				if (countPengaturanBulanan > 0) {
					biayaBulanan = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa,
							Integer.parseInt(semester.getValue()), jenisKegiatan, "-1", true,
							muatDariSettingBiayaTerbaru);
					serapBiayaBulanan(biayaBulanan);
				}

				// PENYEMBUHAN-DIRI: muatan pertama membaca cache (refresh=false); cache basi
				// dapat membuat KEDUA varian (default & bulanan) kosong padahal billing ada
				// di tabel — kasus nyata: S2 smt 1-3 bermode bulanan, layar tampil kosong.
				// Bila hasil kosong, hitung ulang SEKALI langsung dari database (reload=true)
				// sebelum menyerah, sehingga admin tidak perlu menekan Refresh manual.
				if (!nimDikecualikan && !refresh && (detailBiayas == null || detailBiayas.isEmpty())
						&& (biayaBulanan == null || biayaBulanan.isEmpty())) {
					detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa,
							Integer.parseInt(semester.getValue()), jenisKegiatan, true);
					itemBiayas.clear();
					for (Object o : detailBiayas) {
						if (o instanceof DetailBiaya) {
							DetailBiaya detailBiaya = (DetailBiaya) o;
							if (detailBiaya.getItemBiaya() != null)
								itemBiayas.put(detailBiaya.getId(), detailBiaya);
						}
					}
					countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, mahasiswa, jenisKegiatan,
							Integer.parseInt(semester.getValue()), detailBiayas, true, false);
					if (countPengaturanBulanan > 0) {
						biayaBulanan = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa,
								Integer.parseInt(semester.getValue()), jenisKegiatan, "-1", true, true);
						serapBiayaBulanan(biayaBulanan);
					}
				}

				ListModel strset = null;
				Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
						: kegiatan.ambilDetailKegiatan(refresh);
				Collection ooo = (biayaBulanan != null && !biayaBulanan.isEmpty() ? biayaBulanan : detailBiayas);
				dataTagihanData = new ArrayList(ooo);
				// Jangan menghidupkan kembali item dari riwayat cicilan ketika item tersebut
				// sudah tidak dipilih pada Setting Biaya. Riwayat pembayaran tetap tersimpan.
				Collections.sort(dataTagihanData);
				isiInfoModeTagihan(infoModeTagihan, Integer.parseInt(semester.getValue()));
				strset = new SimpleListModel(dataTagihanData);
				if (kegiatan != null)
					kegiatan.resetTagihans();

				gridss.setRowRenderer(detailPembayaranMahasiswaRenderer = new ais.ui.render.ProsesBayarCheckboxRenderer(
						kegiatan, jadwalPembayaran, labelFooterTagihan, labelFooterDibayarAja, labelFooterKekurangan,
						terbilang, terbilangTagihan, terbilangSisa, terbilangSisaPersen, pengurangan,
						new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (arg0 != null && arg0.getData() != null && arg0.getData() instanceof Kegiatan)
									DaftarUlangMahasiswaLamaAction.this.kegiatan = (Kegiatan) arg0.getData();
								hitungJumlahBiayaSeharusnya();
							}
						}, gridCicilan, mahasiswa, null, Integer.parseInt(semester.getValue()),
						labelTahunAkademik.getValue(), dataTagihan = new HashMap<Long, Double>(), gridss,
						detailKegiatans, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.createDefaultTimer(new EventListener() {
									@Override
									public void onEvent(Event a) throws Exception {
										refresh = true;
										onCariMahasiswa(a);
									}
								});
							}
						}, edit, modeWizardRingkas));
				gridss.setModel(strset);
				for (MyDoubleboxMin kurang : pengurangan) {
					kurang.addEventListener("onChange", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							hitungJumlahBiayaSeharusnya();
						}
					});
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					try {
						session.clear();
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
					try {
						session.disconnect();
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
					try {
						session.close();
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}
			}

			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					listCicilan(kegiatan, refresh);
					if (mahasiswaAktif != null)
						Common.freeze(gridss, true);
					hitungJumlahBiayaSeharusnya();

					if (informasi != null) {
						int smt = Integer.parseInt(semester.getValue());
						if (smt > 0) {
							Common.clear(informasi);
							Integer tahap = PengaturanPembayaranBulanan.hitungTahap(mahasiswa, smt,
									Common.BULAN[ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH)]);
							final KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, smt, tahap,
									jenisKegiatan != null && jenisKegiatan.getUntukBayarSP()
											? Perkuliahan.SEMESTER_PENDEK
											: null,
									refresh);

							A sks = new A(
									"/" + Common.numberFormat.get().format(krsMahasiswa.getSksYangDiambil()) + " SKS");
							informasi.appendChild(sks);
							sks.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									PenilaianUtil.downloadSemuaKRS(krsMahasiswa.getSksYangDiambilS(),
											krsMahasiswa.getMahasiswa());
								}
							});

							sks = new A("/" + Common.numberFormat.get().format(krsMahasiswa.getSksk()) + " SKSK");
							informasi.appendChild(sks);
							sks.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									PenilaianUtil.downloadSemuaKRS(krsMahasiswa.getSkskS(),
											krsMahasiswa.getMahasiswa());
								}
							});

							if (krsMahasiswa.getSksKonversi() > 0)
								informasi.appendChild(new Label("(Bukan Konversi : "
										+ Common.numberFormat.get().format(krsMahasiswa.getSksBukanKonversi())
										+ " SKS, Konversi "
										+ Common.numberFormat.get().format(krsMahasiswa.getSksKonversi()) + " SKS)"));

							A ip = new A();
							ip.setLabel("/IP " + Common.numberFormat.get().format(krsMahasiswa.getIps()) + "/IPK "
									+ Common.numberFormat.get().format(krsMahasiswa.getIpk()) + "");
							informasi.appendChild(ip);
							ip.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									PenilaianUtil.downloadSemuaKRS(mahasiswa);
								}
							});
						}
					}
				}
			});
		}

		sesuaikanDenganTagihan.setVisible(countPengaturanBulanan == 0);
		sesuaikanDenganTagihanBulanan.setVisible(countPengaturanBulanan > 0);
	}

	/**
	 * Menyerap hasil muatan varian BULANAN ({@code getDetailBiayaMahasiswa(bulan="-1")})
	 * ke dalam struktur kerja layar: peta {@code itemBiayas} (kunci id {@link DetailBiaya})
	 * dan koleksi {@code detailBiayas}. Koleksi sumber berisi campuran dua tipe objek —
	 * {@link PengaturanPembayaranBulanan} (satu baris per bulan/angsuran, memegang relasi
	 * ke {@code DetailBiaya} induknya) dan {@link DetailBiaya} biasa (item non-bulanan yang
	 * tetap ikut ditagih pada semester yang sama). Keduanya dipetakan seragam agar seluruh
	 * logika hilir (renderer grid, hitung total, tombol bayar) tidak perlu peduli dari
	 * varian mana data berasal.
	 * <p>
	 * Method ini <b>menimpa</b> isi {@code itemBiayas}/{@code detailBiayas} sebelumnya
	 * (clear dahulu) — sesuai kontrak lama blok inline yang digantikannya — namun hanya
	 * bila koleksi sumber TIDAK kosong; sumber kosong dibiarkan lewat tanpa efek supaya
	 * data default yang sudah termuat tidak ikut terhapus. Kesalahan per-elemen ditelan
	 * per baris (tampilkan ke admin saja) agar satu baris rusak tidak menggagalkan seluruh
	 * daftar tagihan. Diekstrak menjadi method tersendiri karena kini dipanggil dari dua
	 * titik: muatan normal dan jalur penyembuhan-diri saat cache basi menghasilkan layar
	 * kosong.
	 *
	 * @param biayaBulanan hasil {@code PembayaranUtilHelper.getDetailBiayaMahasiswa(...,
	 *                     "-1", true, reload)}; boleh {@code null}/kosong (tanpa efek)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void serapBiayaBulanan(Collection biayaBulanan) {
		if (biayaBulanan == null || biayaBulanan.isEmpty())
			return;
		itemBiayas.clear();
		detailBiayas.clear();
		for (Object o : biayaBulanan) {
			try {
				if (o instanceof PengaturanPembayaranBulanan) {
					PengaturanPembayaranBulanan detailBiaya = (PengaturanPembayaranBulanan) o;
					if (detailBiaya.getDetailBiaya() != null
							&& detailBiaya.getDetailBiaya().getItemBiaya() != null) {
						itemBiayas.put(detailBiaya.getDetailBiaya().getId(), detailBiaya.getDetailBiaya());
						detailBiayas.add(detailBiaya.getDetailBiaya());
					}
				} else if (o instanceof DetailBiaya) {
					DetailBiaya detailBiaya = (DetailBiaya) o;
					if (detailBiaya != null && detailBiaya.getItemBiaya() != null) {
						itemBiayas.put(detailBiaya.getId(), detailBiaya);
						detailBiayas.add(detailBiaya);
					}
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	/**
	 * Mengisi lencana informasi <b>mode tagihan</b> pada panel "Daftar Biaya" — jawaban
	 * langsung atas kebingungan klasik pengguna: <i>"mengapa tagihan tidak tampil / mengapa
	 * bentuknya berbeda dari semester lain?"</i>. Konfigurasi billing di Setting Biaya
	 * berlaku PER SEMESTER (contoh nyata: program S2 semester 1-3 ditagih bulanan, semester
	 * 4 ditagih sekaligus), tetapi layar selama ini tidak pernah memberi tahu mode mana
	 * yang sedang berlaku, sehingga layar bulanan yang kosong atau berbentuk lain mudah
	 * disangka error. Lencana ini menampilkan mode tersebut secara eksplisit dengan bahasa
	 * sehari-hari (tanpa istilah teknis) sehingga petugas keuangan maupun orang awam
	 * langsung paham konteks tagihan yang dilihatnya.
	 * <p>
	 * Tiga keadaan yang ditampilkan:
	 * <ol>
	 *   <li><b>BULANAN / ANGSURAN</b> (chip biru) — {@code countPengaturanBulanan > 0}:
	 *       tagihan semester ini dipecah per bulan/angsuran; jumlah baris tagihan ikut
	 *       ditampilkan supaya pengguna tahu berapa bulan yang tersedia.</li>
	 *   <li><b>BUKAN BULANAN</b> (chip hijau) — tagihan terbit satu kali untuk satu
	 *       semester; nominal tetap dapat dibayar bertahap saat transaksi.</li>
	 *   <li><b>BELUM ADA TAGIHAN</b> (chip kuning) — kedua varian kosong meski sudah
	 *       melalui penyembuhan-diri; pengguna diarahkan memeriksa Setting Biaya, bukan
	 *       dibiarkan menatap tabel kosong tanpa penjelasan.</li>
	 * </ol>
	 * Gaya visual memakai HTML+CSS murni (chip membulat, tipografi ringkas, lebar mengikuti
	 * konten dengan {@code max-width:100%}) sehingga rapi di desktop maupun layar sempit
	 * mobile, dan tidak menambah beban library apa pun. Seluruh kegagalan render ditelan
	 * dengan pencatatan ke ErrorLog — lencana bersifat informatif dan tidak boleh
	 * menggagalkan pemuatan daftar biaya.
	 *
	 * @param wadah wadah kosong yang sudah diposisikan tepat di bawah baris tombol
	 *              (Lihat Tagihan / Ubah Tagihan / Refresh / Wizard) oleh {@code listBiaya}
	 * @param smt   semester yang sedang dipilih pada combo semester
	 */
	private void isiInfoModeTagihan(org.zkoss.zk.ui.Component wadah, int smt) {
		try {
			if (wadah == null)
				return;
			Common.clear(wadah);
			boolean bulanan = countPengaturanBulanan > 0;
			int jumlahBaris = dataTagihanData == null ? 0 : dataTagihanData.size();
			int jumlahBulan = 0;
			if (dataTagihanData != null) {
				for (Object o : dataTagihanData) {
					if (o instanceof PengaturanPembayaranBulanan)
						jumlahBulan++;
				}
			}

			String warnaLatar, warnaTeks, ikon, judul, keterangan;
			String diagnosa = diagnosaTagihanTidakMuncul(smt, jumlahBaris);
			if (jumlahBaris == 0) {
				warnaLatar = "#fef9c3";
				warnaTeks = "#854d0e";
				ikon = "&#9888;"; // segitiga peringatan
				judul = "Belum ada tagihan untuk semester " + smt;
				keterangan = "Tagihan belum dibuat di menu Setting Biaya untuk jenis pembayaran dan semester ini,"
						+ " atau seluruh tagihan sudah lunas. Tekan tombol Refresh untuk memuat ulang,"
						+ " atau periksa pengaturan billing-nya.";
			} else if (bulanan) {
				warnaLatar = "#e0f2fe";
				warnaTeks = "#075985";
				ikon = "&#128197;"; // kalender
				judul = "Tagihan semester " + smt + ": BULANAN / ANGSURAN";
				keterangan = "Tagihan semester ini dibagi menjadi "
						+ (jumlahBulan > 0 ? jumlahBulan + " bulan/angsuran" : "beberapa bulan/angsuran")
						+ ". Nominal Setting Biaya adalah total satu semester, sedangkan nominal pada daftar di bawah adalah bagian per bulan sesuai Rencana Angsuran."
						+ " Denda, diskon, dan penyesuaian lain dihitung terpisah dari pokok angsuran.";
			} else {
				warnaLatar = "#dcfce7";
				warnaTeks = "#166534";
				ikon = "&#128181;"; // uang
				judul = "Tagihan semester " + smt + ": BUKAN BULANAN (sekali tagih per semester)";
				keterangan = "Tagihan semester ini ditagih satu kali (tidak dipecah per bulan)."
						+ " Nominalnya tetap boleh dibayar bertahap saat melakukan pembayaran.";
				if (diagnosa.length() > 0) {
					warnaLatar = "#fef9c3";
					warnaTeks = "#854d0e";
					ikon = "&#9888;";
					judul = "Tagihan semester " + smt + " perlu diperiksa";
				}
			}

			Html chip = new Html("<div style=\"display:inline-block;max-width:100%;box-sizing:border-box;"
					+ "margin:4px 0 6px 0;padding:8px 14px;border-radius:10px;background:" + warnaLatar + ";"
					+ "color:" + warnaTeks + ";font-family:'Segoe UI',Arial,sans-serif;line-height:1.5;\">"
					+ "<span style=\"font-weight:700;font-size:12px;letter-spacing:.2px;\">" + ikon + " "
					+ judul + "</span>"
					+ "<div style=\"font-size:11.5px;opacity:.92;margin-top:2px;\">" + keterangan + "</div>"
					+ diagnosa
					+ "</div>");
			chip.setParent(wadah);
			if (penggunaAdalahPetugasKeuangan()) {
				// Gaya diterapkan pada Div pembungkus (BUKAN pada Toolbarbutton langsung) --
				// pada ZK 5, style inline di Toolbarbutton ikut diduplikasi ke elemen internal
				// .z-toolbarbutton-cnt (lihat javadoc MyToolbarbuttonConfig), yang bisa membuat
				// area klik sebenarnya bergeser dari yang terlihat sehingga tombol tampak diam
				// saat diklik. Pola sama seperti PanelToolHelper.pasangBantuanDanLayarPenuh.
				org.zkoss.zul.Div analisisWrap = new org.zkoss.zul.Div();
				analisisWrap.setStyle("display:inline-block;margin:4px 0 6px 8px;vertical-align:top;");
				analisisWrap.setParent(wadah);
				Button analisis = new MyToolbarbuttonConfig("Analisis Data", "/img/svg/search.svg");
				analisis.setTooltiptext("Telusuri kriteria Setting Biaya satu per satu untuk menemukan penyebab tagihan tidak tampil");
				analisis.setParent(analisisWrap);
				final int semesterAnalisis = smt;
				analisis.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						tampilkanAnalisisTagihan(semesterAnalisis);
					}
				});
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "DaftarUlangMahasiswaLamaAction: gagal render info mode tagihan;"
					+ " mahasiswa=" + (mahasiswa == null ? null : mahasiswa.getNim()) + ", smt=" + smt);
		}
	}

	private boolean penggunaAdalahPetugasKeuangan() {
		return tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null;
	}

	/** Satu baris hasil audit query Setting Biaya. */
	private static class TahapAnalisisTagihan {
		private final String nama;
		private final String nilai;
		private final int jumlah;
		private final boolean gagalPertama;
		private final String nilaiTersedia;
		private final String tindakan;
		private final Criterion criterion;
		private int jumlahJikaDilewati = -1;

		private TahapAnalisisTagihan(String nama, String nilai, int jumlah, boolean gagalPertama,
				String nilaiTersedia, String tindakan, Criterion criterion) {
			this.nama = nama;
			this.nilai = nilai;
			this.jumlah = jumlah;
			this.gagalPertama = gagalPertama;
			this.nilaiTersedia = nilaiTersedia;
			this.tindakan = tindakan;
			this.criterion = criterion;
		}
	}

	/** Audit lapis kedua pada template DetailBiaya yang benar-benar dibaca mesin tagihan. */
	private static class TahapAnalisisDetailLama {
		private final String nama;
		private final String nilai;
		private final Criterion criterion;
		private final boolean parameterDinamis;
		private int jumlah;
		private int jumlahJikaDilewati = -1;
		private boolean gagalPertama;

		private TahapAnalisisDetailLama(String nama, String nilai, Criterion criterion, boolean parameterDinamis) {
			this.nama = nama;
			this.nilai = nilai;
			this.criterion = criterion;
			this.parameterDinamis = parameterDinamis;
		}
	}

	/** Ringkasan rantai sumber tagihan sampai transaksi dan baris yang terlihat. */
	private static class AnalisisHilirTagihanLama {
		private List<TahapAnalisisDetailLama> tahap = new ArrayList<TahapAnalisisDetailLama>();
		private int templateAkhir;
		private int hasilQueryProduksi;
		private int kegiatan;
		private int cicilan;
		private double nilaiDibayar;
		private int barisLayar;
		private String sumberItem = "-";
		private String mode = "NONBULANAN";
	}

	/**
	 * Audit baca-saja untuk menjawab secara pasti mengapa tagihan mahasiswa tidak muncul.
	 * Query dimulai tanpa filter, kemudian kriteria produksi ditambahkan satu per satu.
	 * Dengan demikian baris pertama yang berubah menjadi nol adalah titik kegagalannya;
	 * bukan lagi sekadar dugaan gabungan beberapa kriteria.
	 */
	@SuppressWarnings("unchecked")
	private void tampilkanAnalisisTagihan(int smt) throws Exception {
		Session sessionAnalisis = null;
		try {
			if (mahasiswa == null || jenisKegiatan == null) {
				MyMessageboxConfig.show("Mahasiswa atau jenis pembayaran belum dipilih.", "Analisis Data",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}

			sessionAnalisis = HibernateUtil.openSession();
			List<Criterion> filter = new ArrayList<Criterion>();
			List<TahapAnalisisTagihan> tahap = new ArrayList<TahapAnalisisTagihan>();
			int sebelum = hitungSettingBiaya(sessionAnalisis, filter);
			tahap.add(new TahapAnalisisTagihan("Semua data Setting Biaya", "Tanpa kriteria", sebelum, sebelum == 0,
					sebelum == 0 ? "Database Setting Biaya masih kosong" : sebelum + " baris ditemukan",
					sebelum == 0 ? tindakanUntukKriteriaAnalisis("Semua data Setting Biaya", "-", "-") : "", null));
			boolean sudahGagal = sebelum == 0;

			sudahGagal = tambahTahapAnalisis(sessionAnalisis, filter, tahap, "Jenis pembayaran",
					namaObjekAnalisis(jenisKegiatan), Restrictions.eq("jenisKegiatan", jenisKegiatan), sudahGagal);

			Integer tahunAkademikMulai = Common.getTahunAkademik(smt, mahasiswa.getTahunangkatan(),
					mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
			int ta = Integer.parseInt(String.valueOf(tahunAkademikMulai)
					+ (smt % 2 == 0 ? "2" : "1"));
			sudahGagal = tambahTahapAnalisis(sessionAnalisis, filter, tahap, "Periode setting",
					"TA <= " + ta, Restrictions.le("ta", ta), sudahGagal);
			sudahGagal = tambahTahapAnalisis(sessionAnalisis, filter, tahap, "Cakupan mahasiswa",
					"Setting umum (bukan khusus satu mahasiswa)", Restrictions.or(
							Restrictions.isNull("khususBuatMahasiswaTertentu"),
							Restrictions.eq("khususBuatMahasiswaTertentu", false)), sudahGagal);

			int minJenis = jenisKegiatan.getMinSmt() == null ? 0 : jenisKegiatan.getMinSmt().intValue();
			int maxJenis = jenisKegiatan.getMaxSmt() == null ? 30 : jenisKegiatan.getMaxSmt().intValue();
			Criterion rentangSetting = Restrictions.sqlRestriction(smt + " between minsmt and maxsmt");
			Criterion semesterCocok = Restrictions.or(
					Restrictions.and(Restrictions.eq("smtIkutiSettinganDisini", Boolean.TRUE), rentangSetting),
					Restrictions.and(Restrictions.or(Restrictions.isNull("smtIkutiSettinganDisini"),
							Restrictions.eq("smtIkutiSettinganDisini", Boolean.FALSE)),
							Restrictions.and(rentangSetting, smt >= minJenis && smt <= maxJenis
									? Restrictions.sqlRestriction("1=1") : Restrictions.sqlRestriction("1=0"))));
			sudahGagal = tambahTahapAnalisis(sessionAnalisis, filter, tahap, "Semester",
					"Semester " + smt + " berada dalam rentang setting", semesterCocok, sudahGagal);

			Object jenjang = mahasiswa.getJurusan() == null ? mahasiswa.getJenjang() : mahasiswa.getJurusan().getJenjang();
			Object statusAwal = tempHistoryStatusMahasiswa == null ? mahasiswa.getStatusAwalMahasiswa()
					: tempHistoryStatusMahasiswa.getStatusAwalMahasiswa();
			Object status = statusmahasiswa;
			String program = tempHistoryStatusMahasiswa == null ? mahasiswa.getProgram()
					: tempHistoryStatusMahasiswa.getProgram();
			BiodataCalonMahasiswa biodata = null;
			try { biodata = mahasiswa.getBiodataCalonMahasiswaData(); } catch (Exception ignored) {
				ais.common.ErrorAuditUtil.record(ignored, "AnalisisTagihan: biodata calon mahasiswa");
			}

			sudahGagal = tambahTahapAnalisis(sessionAnalisis, filter, tahap, "Angkatan",
					String.valueOf(mahasiswa.getTahunangkatan()), kriteriaWildcardAnalisis("angkatan",
							mahasiswa.getTahunangkatan()), sudahGagal);
			sudahGagal = tambahTahapAnalisis(sessionAnalisis, filter, tahap, "Jenjang",
					namaObjekAnalisis(jenjang), kriteriaWildcardAnalisis("jenjang", jenjang), sudahGagal);
			sudahGagal = tambahTahapAnalisis(sessionAnalisis, filter, tahap, "Status awal",
					namaObjekAnalisis(statusAwal), kriteriaWildcardAnalisis("statusAwalMahasiswa", statusAwal), sudahGagal);
			sudahGagal = tambahTahapAnalisis(sessionAnalisis, filter, tahap, "Status mahasiswa",
					namaObjekAnalisis(status), kriteriaWildcardAnalisis("statusMahasiswa", status), sudahGagal);
			sudahGagal = tambahTahapAnalisis(sessionAnalisis, filter, tahap, "Jenis seleksi",
					namaObjekAnalisis(mahasiswa.getJenisSeleksi()),
					kriteriaWildcardAnalisis("jenisSeleksi", mahasiswa.getJenisSeleksi()), sudahGagal);
			sudahGagal = tambahTahapAnalisis(sessionAnalisis, filter, tahap, "Gelombang",
					namaObjekAnalisis(mahasiswa.getGelombangPendaftaran()),
					kriteriaWildcardAnalisis("gelombangPendaftaran", mahasiswa.getGelombangPendaftaran()), sudahGagal);
			sudahGagal = tambahTahapAnalisis(sessionAnalisis, filter, tahap, "Paket",
					namaObjekAnalisis(biodata == null ? null : biodata.getPaket()),
					kriteriaWildcardAnalisis("paket", biodata == null ? null : biodata.getPaket()), sudahGagal);
			sudahGagal = tambahTahapAnalisis(sessionAnalisis, filter, tahap, "Program",
					program, kriteriaWildcardStringAnalisis("program", program), sudahGagal);
			sudahGagal = tambahTahapAnalisis(sessionAnalisis, filter, tahap, "Prodi/Jurusan",
					namaObjekAnalisis(mahasiswa.getJurusan()),
					kriteriaWildcardAnalisis("jurusan", mahasiswa.getJurusan()), sudahGagal);
			sudahGagal = tambahTahapAnalisis(sessionAnalisis, filter, tahap, "Jenis kelamin",
					mahasiswa.getKelamin(), kriteriaWildcardStringAnalisis("kelamin", mahasiswa.getKelamin()), sudahGagal);
			sudahGagal = tambahTahapAnalisis(sessionAnalisis, filter, tahap, "Afiliasi",
					namaObjekAnalisis(biodata == null ? null : biodata.getAfiliasiCalonMahasiswa()),
					kriteriaWildcardAnalisis("afiliasiCalonMahasiswa",
							biodata == null ? null : biodata.getAfiliasiCalonMahasiswa()), sudahGagal);
			hitungUjiKriteriaDilewati(sessionAnalisis, tahap);

			Criteria criteriaKandidatAkhir = sessionAnalisis.createCriteria(SettingBiaya.class);
			for (Criterion criterion : filter) criteriaKandidatAkhir.add(criterion);
			List<SettingBiaya> kandidatAkhir = criteriaKandidatAkhir.addOrder(Order.desc("ta")).list();
			int detailSetting = 0;
			int pengaturanBulanan = 0;
			if (!kandidatAkhir.isEmpty()) {
				detailSetting = ((Number) sessionAnalisis.createCriteria(DetailSettingBiaya.class)
						.createAlias("itemBiaya", "itemBiaya")
						.add(Restrictions.in("settingBiaya", kandidatAkhir))
						.add(Restrictions.or(Restrictions.isNull("itemBiaya.aktif"), Restrictions.eq("itemBiaya.aktif", true)))
						.add(Restrictions.or(Restrictions.isNull("itemBiaya.minSmt"), Restrictions.le("itemBiaya.minSmt", smt)))
						.add(Restrictions.or(Restrictions.isNull("itemBiaya.maxSmt"), Restrictions.ge("itemBiaya.maxSmt", smt)))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();
				try {
					pengaturanBulanan = ((Number) sessionAnalisis.createCriteria(PengaturanPembayaranBulanan.class)
							.createAlias("detailBiaya", "detailBiaya")
							.add(Restrictions.in("detailBiaya.settingBiaya", kandidatAkhir))
							.add(Restrictions.eq("detailBiaya.semester", smt))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "AnalisisTagihan: hitung pengaturan bulanan");
				}
			}

			int settingKhususAngkatan = ((Number) sessionAnalisis.createCriteria(SettingBiaya.class)
					.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
					.add(Restrictions.eq("angkatan", mahasiswa.getTahunangkatan()))
					.add(Restrictions.or(Restrictions.isNull("khususBuatMahasiswaTertentu"),
							Restrictions.eq("khususBuatMahasiswaTertentu", false)))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			List<SettingBiaya> settingKhususData = sessionAnalisis.createCriteria(SettingBiayaDetail.class)
					.createAlias("settingBiaya", "settingBiaya")
					.add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.eq("settingBiaya.jenisKegiatan", jenisKegiatan))
					.add(Restrictions.eq("settingBiaya.khususBuatMahasiswaTertentu", true))
					.add(Restrictions.le("settingBiaya.ta", ta))
					.add(Restrictions.or(Restrictions.isNull("settingBiaya.minSmt"), Restrictions.le("settingBiaya.minSmt", smt)))
					.add(Restrictions.or(Restrictions.isNull("settingBiaya.maxSmt"), Restrictions.ge("settingBiaya.maxSmt", smt)))
					.add(Restrictions.or(Restrictions.isNull("minSmt"), Restrictions.le("minSmt", smt)))
					.add(Restrictions.or(Restrictions.isNull("maxSmt"), Restrictions.ge("maxSmt", smt)))
					.setProjection(Projections.distinct(Projections.property("settingBiaya"))).list();
			int settingKhususMahasiswa = settingKhususData.size();
			List<SettingBiaya> kandidatSumber = new ArrayList<SettingBiaya>(kandidatAkhir);
			for (SettingBiaya khusus : settingKhususData)
				if (khusus != null && !kandidatSumber.contains(khusus)) kandidatSumber.add(khusus);
			if (!settingKhususData.isEmpty()) {
				detailSetting = ((Number) sessionAnalisis.createCriteria(DetailSettingBiaya.class)
						.createAlias("itemBiaya", "itemBiayaKhusus")
						.add(Restrictions.in("settingBiaya", kandidatSumber))
						.add(Restrictions.or(Restrictions.isNull("itemBiayaKhusus.aktif"), Restrictions.eq("itemBiayaKhusus.aktif", true)))
						.add(Restrictions.or(Restrictions.isNull("itemBiayaKhusus.minSmt"), Restrictions.le("itemBiayaKhusus.minSmt", smt)))
						.add(Restrictions.or(Restrictions.isNull("itemBiayaKhusus.maxSmt"), Restrictions.ge("itemBiayaKhusus.maxSmt", smt)))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();
				pengaturanBulanan = ((Number) sessionAnalisis.createCriteria(PengaturanPembayaranBulanan.class)
						.createAlias("detailBiaya", "detailBiayaKhusus")
						.add(Restrictions.in("detailBiayaKhusus.settingBiaya", kandidatSumber))
						.add(Restrictions.eq("detailBiayaKhusus.semester", smt))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			}
			/* Samakan keputusan analisis dengan penghitung produksi yang dipakai panel
			 * pembayaran. PPB berbasis aturan JenisKegiatan/jenjang atau relasi setting
			 * lama tetap harus dikenali sebagai bulanan meski query kandidat audit nol. */
			pengaturanBulanan = Math.max(pengaturanBulanan, countPengaturanBulanan);
			AnalisisHilirTagihanLama hilir = analisisHilirTagihanLama(sessionAnalisis, kandidatSumber, smt,
					pengaturanBulanan, settingKhususMahasiswa > 0, tahunAkademikMulai);

			String rekomendasi = rekomendasiAnalisisTagihan(tahap, settingKhususAngkatan,
					settingKhususMahasiswa, kandidatAkhir.size(), detailSetting, pengaturanBulanan, smt, hilir);
			tampilkanJendelaAnalisisTagihan(tahap, rekomendasi, settingKhususMahasiswa,
					kandidatAkhir.size(), detailSetting, pengaturanBulanan, smt, hilir, kandidatSumber);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "DaftarUlangMahasiswaLamaAction: analisis data tagihan");
			MyMessageboxConfig.show("Analisis belum dapat diselesaikan: " + e.getMessage(), "Analisis Data",
					MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
		} finally {
			HibernateUtil.closeSessionQuietly(sessionAnalisis);
		}
	}

	private boolean tambahTahapAnalisis(Session session, List<Criterion> filter,
			List<TahapAnalisisTagihan> tahap, String nama, String nilai, Criterion criterion, boolean sudahGagal) {
		filter.add(criterion);
		int jumlah = hitungSettingBiaya(session, filter);
		boolean gagalPertama = !sudahGagal && jumlah == 0;
		String tersedia = gagalPertama ? nilaiTersediaSebelumKriteria(session, filter, nama) : "";
		String tindakan = gagalPertama ? tindakanUntukKriteriaAnalisis(nama, nilai, tersedia) : "";
		tahap.add(new TahapAnalisisTagihan(nama, nilai == null || nilai.trim().isEmpty() ? "-" : nilai,
				jumlah, gagalPertama, tersedia, tindakan, criterion));
		return sudahGagal || gagalPertama;
	}

	@SuppressWarnings("unchecked")
	private String nilaiTersediaSebelumKriteria(Session session, List<Criterion> filter, String namaTahap) {
		try {
			Criteria criteria = session.createCriteria(SettingBiaya.class);
			for (int i = 0; i < filter.size() - 1; i++) criteria.add(filter.get(i));
			if ("Semester".equals(namaTahap)) {
				List<SettingBiaya> data = criteria.addOrder(Order.desc("ta")).setMaxResults(20).list();
				LinkedHashSet<String> nilai = new LinkedHashSet<String>();
				for (SettingBiaya sb : data) {
					nilai.add("semester " + sb.getMinSmt() + " s.d. " + sb.getMaxSmt());
				}
				return gabungkanNilaiAnalisis(nilai);
			}
			String properti = propertiUntukTahapAnalisis(namaTahap);
			if (properti == null) return "Tidak dapat diringkas otomatis";
			List data = criteria.setProjection(Projections.distinct(Projections.property(properti)))
					.setMaxResults(15).list();
			LinkedHashSet<String> nilai = new LinkedHashSet<String>();
			for (Object o : data) {
				if ("khususBuatMahasiswaTertentu".equals(properti)) {
					nilai.add(Boolean.TRUE.equals(o) ? "Khusus satu mahasiswa" : "Umum");
				} else {
					nilai.add(o == null ? "Semua" : namaObjekAnalisis(o));
				}
			}
			return gabungkanNilaiAnalisis(nilai);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "AnalisisTagihan: nilai tersedia " + namaTahap);
			return "Gagal membaca pilihan yang tersedia";
		}
	}

	private String propertiUntukTahapAnalisis(String namaTahap) {
		if ("Jenis pembayaran".equals(namaTahap)) return "jenisKegiatan";
		if ("Periode setting".equals(namaTahap)) return "ta";
		if ("Cakupan mahasiswa".equals(namaTahap)) return "khususBuatMahasiswaTertentu";
		if ("Angkatan".equals(namaTahap)) return "angkatan";
		if ("Jenjang".equals(namaTahap)) return "jenjang";
		if ("Status awal".equals(namaTahap)) return "statusAwalMahasiswa";
		if ("Status mahasiswa".equals(namaTahap)) return "statusMahasiswa";
		if ("Jenis seleksi".equals(namaTahap)) return "jenisSeleksi";
		if ("Gelombang".equals(namaTahap)) return "gelombangPendaftaran";
		if ("Paket".equals(namaTahap)) return "paket";
		if ("Program".equals(namaTahap)) return "program";
		if ("Prodi/Jurusan".equals(namaTahap)) return "jurusan";
		if ("Jenis kelamin".equals(namaTahap)) return "kelamin";
		if ("Afiliasi".equals(namaTahap)) return "afiliasiCalonMahasiswa";
		return null;
	}

	private String gabungkanNilaiAnalisis(LinkedHashSet<String> nilai) {
		if (nilai == null || nilai.isEmpty()) return "Tidak ada nilai yang tersedia";
		StringBuffer sb = new StringBuffer();
		for (String s : nilai) {
			if (sb.length() > 0) sb.append(", ");
			sb.append(s == null || s.trim().isEmpty() ? "Kosong/Semua" : s);
		}
		return sb.toString();
	}

	private String tindakanUntukKriteriaAnalisis(String nama, String nilaiDicari, String nilaiTersedia) {
		String buka = "Buka menu Pembayaran > Pengaturan Billing Pembayaran > Setting Biaya.";
		if ("Semua data Setting Biaya".equals(nama)) {
			return buka + "\nKlik Tambah dan buat konfigurasi biaya pertama.\nPilih Jenis Pembayaran, rentang semester, lalu masukkan Item Biaya dan nominalnya.";
		}
		if ("Jenis pembayaran".equals(nama)) {
			return buka + "\nKlik Tambah, kemudian pilih Jenis Pembayaran: " + nilaiDicari
					+ ".\nAtur kriteria mahasiswa dan tambahkan Item Biaya beserta nominal tagihannya.";
		}
		if ("Periode setting".equals(nama)) {
			return buka + "\nBuat atau duplikasi setting untuk periode " + nilaiDicari
					+ ".\nJangan mengubah periode lama apabila masih dipakai mahasiswa lain.";
		}
		if ("Cakupan mahasiswa".equals(nama)) {
			return buka + "\nBuat setting umum, atau tambahkan mahasiswa ini pada setting khusus mahasiswa."
					+ "\nPastikan pilihan 'Khusus Mahasiswa Tertentu' sesuai tujuan tagihan.";
		}
		if ("Semester".equals(nama)) {
			return buka + "\nUbah Min Semester dan Max Semester agar mencakup " + nilaiDicari
					+ ".\nPeriksa juga pilihan apakah rentang semester mengikuti Jenis Pembayaran atau diatur di Setting Biaya.";
		}
		if ("Angkatan".equals(nama)) {
			return buka + "\nDuplikasi setting angkatan terdekat, lalu ubah kolom Angkatan menjadi " + nilaiDicari
					+ ".\nPeriksa kembali Item Biaya dan nominal sebelum menyimpan; jangan hanya mengganti setting angkatan lama.";
		}
		if ("Jenjang".equals(nama)) return buka + "\nBuat varian setting dengan Jenjang " + nilaiDicari + ", atau pilih Semua bila tarif memang sama untuk seluruh jenjang.";
		if ("Status awal".equals(nama)) return buka + "\nBuat varian Status Awal " + nilaiDicari + ", atau pilih Semua bila tidak perlu dibedakan.";
		if ("Status mahasiswa".equals(nama)) return buka + "\nBuat varian Status Mahasiswa " + nilaiDicari + ".\nUbah status mahasiswa hanya jika data akademiknya memang salah; jangan mengubahnya sekadar agar tagihan muncul.";
		if ("Jenis seleksi".equals(nama)) return buka + "\nBuat varian Jenis Seleksi " + nilaiDicari + ", atau pilih Semua bila tarifnya sama.";
		if ("Gelombang".equals(nama)) return buka + "\nBuat varian Gelombang " + nilaiDicari + ", atau pilih Semua bila tarifnya sama.";
		if ("Paket".equals(nama)) return buka + "\nBuat varian Paket " + nilaiDicari + ", atau pilih Semua bila paket tidak memengaruhi tarif.";
		if ("Program".equals(nama)) return buka + "\nBuat varian Program/Jenis Kuliah " + nilaiDicari + ", atau pilih Semua bila tarifnya sama.";
		if ("Prodi/Jurusan".equals(nama)) return buka + "\nBuat varian Prodi/Jurusan " + nilaiDicari + ", atau pilih Semua bila tarif berlaku lintas prodi.";
		if ("Jenis kelamin".equals(nama)) return buka + "\nPilih Jenis Kelamin " + nilaiDicari + ", atau Semua bila tidak memengaruhi tarif.";
		if ("Afiliasi".equals(nama)) return buka + "\nBuat varian Afiliasi " + nilaiDicari + ", atau pilih Semua bila afiliasi tidak memengaruhi tarif.";
		return buka + "\nSamakan kriteria setting dengan data mahasiswa yang dicari: " + nilaiDicari + ".";
	}

	private int hitungSettingBiaya(Session session, List<Criterion> filter) {
		Criteria criteria = session.createCriteria(SettingBiaya.class);
		if (filter != null && !filter.isEmpty()) {
			for (Criterion criterion : filter) criteria.add(criterion);
		}
		Number n = (Number) criteria.setProjection(Projections.rowCount()).uniqueResult();
		return n == null ? 0 : n.intValue();
	}

	/**
	 * Uji sensitivitas: setiap kriteria dilepas satu per satu sementara seluruh kriteria
	 * lain tetap aktif. Jika hasil berubah dari nol menjadi lebih dari nol, kriteria itu
	 * terbukti sebagai penghambat independen, bukan sekadar dugaan karena urutan filter.
	 */
	private void hitungUjiKriteriaDilewati(Session session, List<TahapAnalisisTagihan> tahap) {
		for (TahapAnalisisTagihan yangDilewati : tahap) {
			if (yangDilewati.criterion == null) continue;
			Criteria criteria = session.createCriteria(SettingBiaya.class);
			for (TahapAnalisisTagihan t : tahap) {
				if (t.criterion != null && t != yangDilewati) criteria.add(t.criterion);
			}
			Number n = (Number) criteria.setProjection(Projections.rowCount()).uniqueResult();
			yangDilewati.jumlahJikaDilewati = n == null ? 0 : n.intValue();
		}
	}

	private Criterion kriteriaWildcardAnalisis(String properti, Object nilai) {
		return nilai == null ? Restrictions.isNull(properti)
				: Restrictions.or(Restrictions.isNull(properti), Restrictions.eq(properti, nilai));
	}

	private Criterion kriteriaWildcardStringAnalisis(String properti, String nilai) {
		if (nilai == null || nilai.trim().isEmpty()) {
			return Restrictions.or(Restrictions.isNull(properti), Restrictions.eq(properti, ""));
		}
		return Restrictions.or(Restrictions.or(Restrictions.isNull(properti), Restrictions.eq(properti, "")),
				Restrictions.ilike(properti, nilai.trim(), org.hibernate.criterion.MatchMode.EXACT));
	}

	private String namaObjekAnalisis(Object o) {
		if (o == null) return "Semua / tidak diisi";
		try {
			java.lang.reflect.Method m = o.getClass().getMethod("getNama");
			Object nama = m.invoke(o);
			if (nama != null) return nama.toString();
		} catch (Exception ignored) { /* fallback toString */ }
		return o.toString();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private AnalisisHilirTagihanLama analisisHilirTagihanLama(Session session, List<SettingBiaya> setting,
			int smt, int pengaturanBulanan, boolean settingKhusus, Integer tahunAkademikMulai) {
		AnalisisHilirTagihanLama hasil = new AnalisisHilirTagihanLama();
		hasil.barisLayar = dataTagihanData == null ? 0 : dataTagihanData.size();
		hasil.mode = pengaturanBulanan > 0 ? "BULANAN / ANGSURAN" : "NONBULANAN / SEKALI TAGIH";
		if (settingKhusus) hasil.mode += " (SETTING KHUSUS MAHASISWA)";

		List<ItemBiaya> itemSetting = new ArrayList<ItemBiaya>();
		if (setting != null && !setting.isEmpty()) {
			itemSetting = session.createCriteria(DetailSettingBiaya.class)
					.createAlias("itemBiaya", "itemBiayaAnalisis")
					.add(Restrictions.in("settingBiaya", setting))
					.add(Restrictions.or(Restrictions.isNull("itemBiayaAnalisis.aktif"),
							Restrictions.eq("itemBiayaAnalisis.aktif", true)))
					.setProjection(Projections.distinct(Projections.property("itemBiaya"))).list();
		}
		Object jenjang = mahasiswa.getJurusan() == null ? mahasiswa.getJenjang() : mahasiswa.getJurusan().getJenjang();
		Object statusAwal = tempHistoryStatusMahasiswa == null ? mahasiswa.getStatusAwalMahasiswa()
				: tempHistoryStatusMahasiswa.getStatusAwalMahasiswa();
		Object status = statusmahasiswa;
		String program = tempHistoryStatusMahasiswa == null ? mahasiswa.getProgram() : tempHistoryStatusMahasiswa.getProgram();
		String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);

		List<TahapAnalisisDetailLama> tahap = hasil.tahap;
		tahap.add(new TahapAnalisisDetailLama("Semua template Detail Biaya", "Tanpa kriteria", null, false));
		tahap.add(new TahapAnalisisDetailLama("Template aktif", "Aktif atau belum ditentukan",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)), false));
		tahap.add(new TahapAnalisisDetailLama("Item dari Setting Biaya", itemSetting.size() + " item aktif",
				itemSetting.isEmpty() ? Restrictions.sqlRestriction("1=0") : Restrictions.in("itemBiaya", itemSetting), false));
		tahap.add(new TahapAnalisisDetailLama("Bukan catatan pembayaran", "Template tagihan",
				Restrictions.or(Restrictions.isNull("merupakanPembayaran"), Restrictions.eq("merupakanPembayaran", false)), false));
		tahap.add(new TahapAnalisisDetailLama("Tahun akademik template", tahunAkademik,
				kriteriaCabangKhususLama(settingKhusus, Restrictions.eq("tahunAkademik", tahunAkademik)), false));
		tahap.add(new TahapAnalisisDetailLama("Semester template", "Semester " + smt,
				Restrictions.eq("semester", smt), false));
		tahap.add(new TahapAnalisisDetailLama("Status awal template", namaObjekAnalisis(statusAwal),
				kriteriaCabangKhususLama(settingKhusus, kriteriaTepatDetailLama("statusAwalMahasiswa", statusAwal)), false));
		tahap.add(new TahapAnalisisDetailLama("Status mahasiswa template", namaObjekAnalisis(status),
				kriteriaCabangKhususLama(settingKhusus, kriteriaTepatDetailLama("statusMahasiswa", status)), false));
		tahap.add(new TahapAnalisisDetailLama("Semester mulai", mahasiswa.getSemesterMulai(),
				kriteriaCabangKhususLama(settingKhusus, kriteriaTepatStringDetailLama("mulaiBelajarDiSemester", mahasiswa.getSemesterMulai())), false));
		tahap.add(new TahapAnalisisDetailLama("Jenis pembayaran template", namaObjekAnalisis(jenisKegiatan),
				Restrictions.eq("jenisKegiatan", jenisKegiatan), false));
		tahap.add(new TahapAnalisisDetailLama("Kewarganegaraan", mahasiswa.getWarganegara(),
				kriteriaCabangKhususLama(settingKhusus, kriteriaTepatStringDetailLama("wnaAtauWni", mahasiswa.getWarganegara())), false));
		tahap.add(new TahapAnalisisDetailLama("Jenjang template", namaObjekAnalisis(jenjang),
				kriteriaCabangKhususLama(settingKhusus,
						PembayaranUtilHelper.kriteriaJenjangDetailBiaya((Jenjang) jenjang)), false));
		tahap.add(new TahapAnalisisDetailLama("Prodi/Jurusan template", namaObjekAnalisis(mahasiswa.getJurusan()),
				kriteriaCabangKhususLama(settingKhusus, kriteriaTepatDetailLama("jurusan", mahasiswa.getJurusan())), false));
		tahap.add(new TahapAnalisisDetailLama("Program template", program,
				kriteriaCabangKhususLama(settingKhusus, kriteriaTepatStringDetailLama("program", program)), false));
		tahap.add(new TahapAnalisisDetailLama("Angkatan template", String.valueOf(mahasiswa.getTahunangkatan()),
				kriteriaCabangKhususLama(settingKhusus, Restrictions.eq("angkatan", mahasiswa.getTahunangkatan())), false));
		tahap.add(new TahapAnalisisDetailLama("Parameter tambahan dinamis", "Parameter mahasiswa ikut diperiksa",
				null, !settingKhusus));

		boolean gagal = false;
		for (int i = 0; i < tahap.size(); i++) {
			TahapAnalisisDetailLama t = tahap.get(i);
			t.jumlah = hitungDetailBiayaTahapanLama(session, tahap, i, -1);
			t.gagalPertama = !gagal && t.jumlah == 0;
			if (t.gagalPertama) gagal = true;
		}
		for (int i = 0; i < tahap.size(); i++)
			tahap.get(i).jumlahJikaDilewati = hitungDetailBiayaTahapanLama(session, tahap, tahap.size() - 1, i);

		List<DetailBiaya> detailAkhir = criteriaDetailBiayaTahapanLama(session, tahap, tahap.size() - 1, -1)
				.addOrder(Order.desc("id")).list();
		hasil.templateAkhir = detailAkhir.size();
		LinkedHashSet<String> sumber = new LinkedHashSet<String>();
		for (DetailBiaya db : detailAkhir) {
			if (db == null || sumber.size() >= 12) continue;
			String item = db.getItemBiaya() == null ? "Item tanpa nama" : db.getItemBiaya().getNama();
			String asal = db.getSettingBiaya() == null || db.getSettingBiaya().getId() == null ? "template langsung"
					: "SettingBiaya #" + db.getSettingBiaya().getId();
			sumber.add(item + " (DetailBiaya #" + db.getId() + ", " + asal + ")");
		}
		hasil.sumberItem = gabungkanNilaiAnalisis(sumber);
		try {
			Collection produksi = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, smt, jenisKegiatan, true);
			hasil.hasilQueryProduksi = produksi == null ? 0 : produksi.size();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "AnalisisTagihanLama: query produksi");
		}
		Number kegiatanN = (Number) session.createCriteria(Kegiatan.class)
				.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
				.add(Restrictions.eq("semster", smt))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount()).uniqueResult();
		hasil.kegiatan = kegiatanN == null ? 0 : kegiatanN.intValue();
		Criteria cicilan = session.createCriteria(CicilanPembayaran.class).createAlias("kegiatan", "kegiatanAnalisis")
				.add(Restrictions.eq("kegiatanAnalisis.mahasiswa", mahasiswa))
				.add(Restrictions.eq("kegiatanAnalisis.jenisKegiatan", jenisKegiatan))
				.add(Restrictions.eq("kegiatanAnalisis.semster", smt));
		Number cicilanN = (Number) cicilan.setProjection(Projections.rowCount()).uniqueResult();
		hasil.cicilan = cicilanN == null ? 0 : cicilanN.intValue();
		Number nilai = (Number) session.createCriteria(CicilanPembayaran.class).createAlias("kegiatan", "kegiatanNilai")
				.add(Restrictions.eq("kegiatanNilai.mahasiswa", mahasiswa))
				.add(Restrictions.eq("kegiatanNilai.jenisKegiatan", jenisKegiatan))
				.add(Restrictions.eq("kegiatanNilai.semster", smt))
				.setProjection(Projections.sum("nilai")).uniqueResult();
		hasil.nilaiDibayar = nilai == null ? 0.0 : nilai.doubleValue();
		return hasil;
	}

	private Criterion kriteriaTepatDetailLama(String properti, Object nilai) {
		return nilai == null ? Restrictions.isNull(properti) : Restrictions.eq(properti, nilai);
	}

	private Criterion kriteriaCabangKhususLama(boolean khusus, Criterion normal) {
		return khusus ? Restrictions.sqlRestriction("1=1") : normal;
	}

	private Criterion kriteriaTepatStringDetailLama(String properti, String nilai) {
		return nilai == null || nilai.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike(properti, nilai.trim(), org.hibernate.criterion.MatchMode.EXACT);
	}

	private int hitungDetailBiayaTahapanLama(Session session, List<TahapAnalisisDetailLama> tahap,
			int batas, int dilewati) {
		Number n = (Number) criteriaDetailBiayaTahapanLama(session, tahap, batas, dilewati)
				.setProjection(Projections.rowCount()).uniqueResult();
		return n == null ? 0 : n.intValue();
	}

	private Criteria criteriaDetailBiayaTahapanLama(Session session, List<TahapAnalisisDetailLama> tahap,
			int batas, int dilewati) {
		Criteria criteria = session.createCriteria(DetailBiaya.class);
		boolean dinamis = false;
		for (int i = 0; i <= batas && i < tahap.size(); i++) {
			if (i == dilewati) continue;
			TahapAnalisisDetailLama t = tahap.get(i);
			if (t.criterion != null) criteria.add(t.criterion);
			if (t.parameterDinamis) dinamis = true;
		}
		if (dinamis) PembayaranUtilHelper.filterCriteriaDenganNilaiTambahan(criteria, session, mahasiswa, null);
		return criteria;
	}

	private String rekomendasiAnalisisTagihan(List<TahapAnalisisTagihan> tahap, int settingKhususAngkatan,
			int settingKhususMahasiswa, int kandidatAkhir, int detailSetting, int pengaturanBulanan, int smt,
			AnalisisHilirTagihanLama hilir) {
		List<String> penghambatTerbukti = new ArrayList<String>();
		for (TahapAnalisisTagihan t : tahap) {
			if (kandidatAkhir == 0 && settingKhususMahasiswa == 0 && t.jumlahJikaDilewati > 0) penghambatTerbukti.add(t.nama + " harus cocok dengan " + t.nilai);
		}
		if (!penghambatTerbukti.isEmpty()) {
			StringBuffer sb = new StringBuffer("Uji pengecualian membuktikan kriteria penghambat: ");
			for (int i = 0; i < penghambatTerbukti.size(); i++) {
				if (i > 0) sb.append("; ");
				sb.append(penghambatTerbukti.get(i));
			}
			return sb.append(". Isi atau buat varian Setting Biaya sesuai nilai tersebut.").toString();
		}
		for (TahapAnalisisTagihan t : tahap) {
			if (settingKhususMahasiswa > 0) break;
			if (t.gagalPertama) {
				if ("Angkatan".equals(t.nama)) {
					return "Setting Biaya untuk angkatan " + mahasiswa.getTahunangkatan()
							+ " belum dibuat dan tidak ada baris 'Semua' yang dapat dipakai. "
							+ "Data angkatan yang tersedia sebelum kriteria ini: " + t.nilaiTersedia + ".";
				}
				return "Kandidat menjadi kosong saat kriteria '" + t.nama + "' diterapkan. "
						+ "Sistem mencari '" + t.nilai + "', sedangkan data yang tersedia sebelum kriteria ini: "
						+ t.nilaiTersedia + "."
						+ (settingKhususAngkatan == 0 && !"Jenis pembayaran".equals(t.nama)
								? " Catatan: setting khusus angkatan " + mahasiswa.getTahunangkatan()
										+ " juga belum tersedia; kandidat sebelumnya hanya mengandalkan baris 'Semua'."
								: "");
			}
		}
		if (kandidatAkhir == 0 && settingKhususMahasiswa == 0) return "Tidak ada Setting Biaya yang cocok untuk kombinasi data mahasiswa ini.";
		if (detailSetting == 0) return "Setting Biaya ditemukan, tetapi Item Biaya aktif untuk semester " + smt
				+ " belum diisi atau berada di luar rentang semester.";
		if (hilir != null && hilir.templateAkhir == 0) {
			for (TahapAnalisisDetailLama t : hilir.tahap)
				if (t.jumlahJikaDilewati > 0) return "Setting dan Item Biaya ditemukan, tetapi template DetailBiaya ditolak oleh kriteria '"
						+ t.nama + "' (data mahasiswa: " + t.nilai + ").";
			for (TahapAnalisisDetailLama t : hilir.tahap)
				if (t.gagalPertama) return "Rantai sumber tagihan berhenti pada '" + t.nama + "' (" + t.nilai + ").";
		}
		if (pengaturanBulanan == 0 && countPengaturanBulanan > 0) return "Setting dan item biaya ditemukan, tetapi Pengaturan Tagihan Bulanan belum terbentuk untuk semester ini.";
		if (hilir != null && hilir.hasilQueryProduksi > 0 && hilir.barisLayar == 0 && hilir.cicilan > 0)
			return "Sumber tagihan ditemukan, tetapi tidak tampil karena sudah ada " + hilir.cicilan
					+ " cicilan/pembayaran senilai " + Common.numberFormat.get().format(hilir.nilaiDibayar)
					+ ". Periksa History untuk memastikan status lunas atau sisa tagihan.";
		if (hilir != null && hilir.templateAkhir > 0 && hilir.hasilQueryProduksi == 0)
			return "Template DetailBiaya cocok, tetapi query produksi tidak mengembalikan baris. Periksa status lulus/keluar, semester pindahan, kelas/tempat tinggal, mode angsuran, dan parameter tambahan mahasiswa.";
		return "Setting dan item biaya ditemukan. Jika layar tetap kosong, tagihan mungkin sudah lunas atau Detail Biaya belum tergenerasi; tekan Refresh/Proses Tagihan.";
	}

	private String tindakanUtamaAnalisisTagihan(List<TahapAnalisisTagihan> tahap, int settingKhususMahasiswa,
			int kandidatAkhir, int detailSetting, int pengaturanBulanan, int smt) {
		StringBuffer penghambat = new StringBuffer();
		for (TahapAnalisisTagihan t : tahap) {
			if (kandidatAkhir == 0 && t.jumlahJikaDilewati > 0 && t.tindakan != null && !t.tindakan.trim().isEmpty()) {
				if (penghambat.length() > 0) penghambat.append("\n");
				penghambat.append("Prioritas ").append(t.nama).append(": ").append(t.nilai).append(".")
						.append("\n").append(t.tindakan);
			}
		}
		if (penghambat.length() > 0) return penghambat.toString();
		for (TahapAnalisisTagihan t : tahap) {
			if (t.gagalPertama && t.tindakan != null && !t.tindakan.trim().isEmpty()) return t.tindakan;
		}
		if (settingKhususMahasiswa > 0) {
			return "Buka Setting Biaya khusus mahasiswa ini.\nPastikan rentang semester mencakup semester " + smt
					+ ".\nPastikan Item Biaya aktif dan nominalnya sudah diisi.";
		}
		if (kandidatAkhir == 0) {
			return "Buka menu Pembayaran > Pengaturan Billing Pembayaran > Setting Biaya."
					+ "\nBuat konfigurasi baru yang sama dengan seluruh data mahasiswa pada tabel analisis."
					+ "\nMasukkan Item Biaya dan nominal tagihan.";
		}
		if (detailSetting == 0) {
			return "Buka Setting Biaya yang cocok, lalu klik Ubah."
					+ "\nTambahkan Item Biaya yang akan ditagihkan dan isi nominalnya."
					+ "\nAktifkan item serta atur Min/Max Semester agar mencakup semester " + smt + ".";
		}
		if (pengaturanBulanan == 0 && countPengaturanBulanan > 0) {
			return "Buka Pengaturan Tagihan/Pembayaran Bulanan untuk Setting Biaya tersebut."
					+ "\nBuat baris bulan atau tahap pembayaran semester " + smt + " dan isi nominalnya."
					+ "\nPastikan total nominal bulanan sama dengan ketentuan Item Biaya.";
		}
		return "Klik Refresh untuk membuang cache tagihan lama."
				+ "\nJika masih kosong, jalankan Proses Tagihan agar Detail Biaya dibuat."
				+ "\nPeriksa riwayat pembayaran karena tagihan yang sudah lunas memang tidak ditampilkan sebagai tunggakan.";
	}

	/** Penjelasan kontekstual per baris agar petugas memahami arti dan tindak lanjut audit. */
	private String keteranganPintarTahapLama(TahapAnalisisTagihan t, int kandidatAkhir) {
		String arti = artiKriteriaTagihanLama(t.nama);
		if (t.criterion == null) {
			return arti + (t.jumlah == 0
					? " Tidak ada data dasar; buat Setting Biaya sebelum memeriksa kriteria lain."
					: " Ditemukan " + t.jumlah + " konfigurasi sebagai titik awal analisis.");
		}
		if (kandidatAkhir == 0 && t.jumlahJikaDilewati > 0) {
			return arti + " Kriteria ini terbukti menjadi penghambat: tanpa kriteria ini ditemukan "
					+ t.jumlahJikaDilewati + " kandidat. Data yang tersedia: " + t.nilaiTersedia
					+ ". " + t.tindakan;
		}
		if (t.gagalPertama) {
			return arti + " Rantai pencarian pertama kali menjadi kosong di sini. Sistem membutuhkan '"
					+ t.nilai + "'. Data sebelum tahap ini: " + t.nilaiTersedia + ". " + t.tindakan;
		}
		if (t.jumlah > 0 && t.jumlahJikaDilewati > t.jumlah) {
			return arti + " Cocok. Kriteria ini menyaring kandidat dari " + t.jumlahJikaDilewati
					+ " menjadi " + t.jumlah + ", sehingga nilainya berpengaruh tetapi valid.";
		}
		if (t.jumlah > 0) {
			return arti + " Cocok. Tersisa " + t.jumlah
					+ " kandidat; kriteria lain juga sudah cukup mengarahkan ke konfigurasi yang sama.";
		}
		return arti + " Belum ada kandidat tersisa karena kegagalan telah terjadi pada baris sebelumnya.";
	}

	private String artiKriteriaTagihanLama(String nama) {
		if ("Semua data Setting Biaya".equals(nama)) return "Basis seluruh konfigurasi biaya tanpa filter.";
		if ("Jenis pembayaran".equals(nama)) return "Memastikan konfigurasi memakai Jenis Kegiatan pembayaran yang sedang dibuka.";
		if ("Periode setting".equals(nama)) return "Memastikan tahun/periode konfigurasi sudah berlaku untuk transaksi ini.";
		if (nama != null && nama.startsWith("Cakupan")) return "Menentukan apakah biaya berlaku umum atau khusus untuk mahasiswa ini.";
		if ("Semester".equals(nama)) return "Memastikan semester berada dalam rentang minimum dan maksimum biaya.";
		if ("Angkatan".equals(nama)) return "Mencocokkan tahun masuk mahasiswa dengan angkatan pada Setting Biaya.";
		if ("Jenjang".equals(nama)) return "Mencocokkan jenjang pendidikan; nilai Semua/null berarti lintas jenjang.";
		if ("Status awal".equals(nama)) return "Membedakan mahasiswa baru, pindahan, beasiswa, atau status awal lainnya.";
		if ("Status mahasiswa".equals(nama)) return "Memastikan status akademik mahasiswa memenuhi aturan biaya.";
		if ("Jenis seleksi".equals(nama)) return "Mencocokkan jalur penerimaan yang dapat mempunyai tarif berbeda.";
		if ("Gelombang".equals(nama)) return "Mencocokkan gelombang pendaftaran mahasiswa.";
		if ("Paket".equals(nama)) return "Mencocokkan paket PMB asal mahasiswa yang menentukan kelompok biaya.";
		if ("Program".equals(nama)) return "Mencocokkan program atau jenis kuliah, misalnya reguler/karyawan.";
		if ("Prodi/Jurusan".equals(nama)) return "Mencocokkan program studi aktif mahasiswa.";
		if ("Jenis kelamin".equals(nama)) return "Menerapkan tarif khusus jenis kelamin bila memang dikonfigurasi.";
		if ("Afiliasi".equals(nama)) return "Mencocokkan afiliasi mahasiswa; kosong berarti tidak dibatasi.";
		return "Kriteria produksi yang ikut menentukan sumber tagihan.";
	}

	private void tampilkanJendelaAnalisisTagihan(List<TahapAnalisisTagihan> tahap, String rekomendasi,
			int settingKhususMahasiswa, int kandidatAkhir, int detailSetting, int pengaturanBulanan, final int smt,
			AnalisisHilirTagihanLama hilir, List<SettingBiaya> kandidatSumber) throws InterruptedException {
		MyWindow window = new MyWindow("Analisis Data Tagihan", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setWidth("95%");
		window.setHeight("82%");
		window.setSizable(true);
		final Jurusan jurusan = mahasiswa.getJurusan();
		final boolean arahBulanan = pengaturanBulanan > 0 || (hilir != null && hilir.mode != null
				&& hilir.mode.toUpperCase().startsWith("BULANAN"));
		final Long settingTujuanId = kandidatSumber == null || kandidatSumber.isEmpty()
				|| kandidatSumber.get(0) == null ? null : kandidatSumber.get(0).getId();

		org.zkoss.zul.Div barTindakan = new org.zkoss.zul.Div();
		barTindakan.setStyle("padding:4px 12px;background:#f8fafc;border-bottom:1px solid #cbd5e1;"
				+ "box-sizing:border-box;white-space:normal;");
		barTindakan.setParent(window);
		MyButtonTabbox navigasiPengaturan = MyButtonTabbox.buat(barTindakan, "52px", new int[] { 1 });
		org.zkoss.zul.Div panelNavigasi = navigasiPengaturan.tambahTab(1, arahBulanan
				? "Buka Pengaturan Tagihan Bulanan" : "Buka Setting Biaya", "/img/svg/cash.svg");
		panelNavigasi.setStyle("padding:3px 2px;overflow:hidden;box-sizing:border-box;");
		new Label(arahBulanan ? "Sistem mendeteksi sumber tagihan bulanan/angsuran."
				: "Sistem mendeteksi sumber Setting Biaya bukan bulanan.").setParent(panelNavigasi);
		navigasiPengaturan.setTooltipTombol(1,
				"Buka sumber pengaturan yang dipilih otomatis dari hasil analisis");
		navigasiPengaturan.onSetiapKlikPanel(panelNavigasi, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (arahBulanan) {
					/* Full-screen dan tanpa wrapper scroll Grid generik. Wrapper lama dapat
					 * menyusutkan Include menjadi setengah layar pada ZK lama. Halaman tujuan
					 * sudah memiliki scroll internal sendiri. */
					Common.displayWindow(urlPengaturanBulananAnalisisLama(smt, jurusan), true,
							"100%", "100%", null, "Pengaturan Tagihan Bulanan", false);
				} else {
					SettingBiaya tujuan = settingTujuanId == null ? buatSettingBiayaDariMahasiswa(smt, jurusan)
							: (SettingBiaya) HibernateUtil.currentSession().get(SettingBiaya.class, settingTujuanId);
					if (tujuan == null) tujuan = buatSettingBiayaDariMahasiswa(smt, jurusan);
					SetingBiayaAction.onAddExternal(null, tujuan);
				}
			}
		});

		/* Pada ZK lama Vbox dirender seperti table sehingga overflow tidak membentuk
		 * scrollbar. Div blok dengan tinggi maksimum membuat seluruh hasil terjangkau. */
		org.zkoss.zul.Div isi = new org.zkoss.zul.Div();
		isi.setWidth("100%");
		isi.setStyle("height:calc(82vh - 145px);max-height:calc(82vh - 145px);min-height:260px;"
				+ "overflow-y:scroll;overflow-x:auto;padding:12px 12px 64px 12px;box-sizing:border-box;position:relative;");
		isi.setParent(window);
		StringBuffer html = new StringBuffer();
		String tindakanUtama = tindakanUtamaAnalisisTagihan(tahap, settingKhususMahasiswa,
				kandidatAkhir, detailSetting, pengaturanBulanan, smt);
		DaftarUlangTagihanAnalisisHelper.Data ringkasan = buatDataRingkasanAnalisisTagihan(
				rekomendasi, tindakanUtama, settingKhususMahasiswa, kandidatAkhir, detailSetting,
				pengaturanBulanan, smt, hilir, kandidatSumber);
		html.append("<div style='font-family:Segoe UI,Arial,sans-serif;color:#1f2937'>")
				.append("<div style='padding:10px 12px;background:#eff6ff;border-left:4px solid #2563eb;margin-bottom:10px'>")
				.append("<b>").append(escHtmlTagihan(mahasiswa.getNim())).append(" - ")
				.append(escHtmlTagihan(mahasiswa.getNama())).append("</b><br>")
				.append("Jenis pembayaran: ").append(escHtmlTagihan(namaObjekAnalisis(jenisKegiatan)))
				.append(" &nbsp;|&nbsp; Semester: ").append(smt)
				.append(" &nbsp;|&nbsp; Angkatan: ").append(mahasiswa.getTahunangkatan()).append("</div>")
				.append(DaftarUlangTagihanAnalisisHelper.htmlRingkasan(ringkasan))
				.append("<p>Query diuji dua arah: dimasukkan berurutan dan dilewati satu per satu. Nilai pada kolom <b>Jika dilewati</b> yang berubah menjadi lebih dari nol membuktikan kriteria tersebut sebagai penyebab utama.</p>")
				.append("<table style='width:100%;min-width:1240px;border-collapse:collapse;font-size:12px'>")
				.append("<tr style='background:#e5e7eb'><th title='Urutan pemeriksaan query' style='padding:7px;border:1px solid #d1d5db'>No.</th>")
				.append("<th title='Syarat Setting Biaya yang sedang diuji' style='padding:7px;border:1px solid #d1d5db;text-align:left'>Kriteria yang ditambahkan</th>")
				.append("<th title='Nilai nyata milik mahasiswa' style='padding:7px;border:1px solid #d1d5db;text-align:left'>Nilai mahasiswa</th>")
				.append("<th title='Jumlah Setting Biaya yang masih cocok setelah syarat diterapkan' style='padding:7px;border:1px solid #d1d5db'>Kandidat tersisa</th>")
				.append("<th title='Jumlah kandidat jika hanya syarat pada baris ini diabaikan' style='padding:7px;border:1px solid #d1d5db'>Jika dilewati</th>")
				.append("<th title='Pilihan nilai yang ditemukan sebelum query menjadi kosong' style='padding:7px;border:1px solid #d1d5db;text-align:left'>Data tersedia saat gagal</th>")
				.append("<th title='Kesimpulan kecocokan atau titik penyebab' style='padding:7px;border:1px solid #d1d5db'>Status</th>")
				.append("<th style='padding:7px;border:1px solid #d1d5db;text-align:left;min-width:320px'>Keterangan Pintar</th></tr>");
		for (int i = 0; i < tahap.size(); i++) {
			TahapAnalisisTagihan t = tahap.get(i);
			String bg = t.gagalPertama ? "#fee2e2" : (t.jumlah > 0 ? "#f0fdf4" : "#f9fafb");
			boolean terbukti = kandidatAkhir == 0 && t.jumlahJikaDilewati > 0;
			String status = terbukti ? "&#9888; PENYEBAB TERBUKTI"
					: (t.gagalPertama ? "&#10060; TITIK GAGAL" : (t.jumlah > 0 ? "&#10004; Cocok" : "- Tetap kosong"));
			String keteranganPintar = keteranganPintarTahapLama(t, kandidatAkhir);
			html.append("<tr style='background:").append(bg).append("'><td style='padding:6px;border:1px solid #d1d5db;text-align:center'>")
					.append(i + 1).append("</td><td style='padding:6px;border:1px solid #d1d5db'><b>")
					.append(escHtmlTagihan(t.nama)).append("</b></td><td style='padding:6px;border:1px solid #d1d5db'>")
					.append(escHtmlTagihan(t.nilai)).append("</td><td style='padding:6px;border:1px solid #d1d5db;text-align:center'>")
					.append(t.jumlah).append("</td><td style='padding:6px;border:1px solid #d1d5db;text-align:center'>")
					.append(t.criterion == null ? "-" : String.valueOf(t.jumlahJikaDilewati))
					.append("</td><td style='padding:6px;border:1px solid #d1d5db'>")
					.append(escHtmlTagihan(t.nilaiTersedia)).append("</td><td style='padding:6px;border:1px solid #d1d5db;text-align:center'>")
					.append(status).append("</td><td style='padding:6px;border:1px solid #d1d5db;line-height:1.45'>")
					.append(escHtmlTagihan(keteranganPintar)).append("</td></tr>");
		}
		html.append("</table>")
				.append(htmlAnalisisHilirTagihanLama(hilir))
				.append("<div style='margin-top:10px;padding:9px;background:#f8fafc;border:1px solid #cbd5e1'>")
				.append("Setting khusus mahasiswa: <b>").append(settingKhususMahasiswa).append("</b> &nbsp;|&nbsp; ")
				.append("Setting akhir cocok: <b>").append(kandidatAkhir).append("</b> &nbsp;|&nbsp; ")
				.append("Item biaya aktif: <b>").append(detailSetting).append("</b> &nbsp;|&nbsp; ")
				.append("Pengaturan bulanan: <b>").append(pengaturanBulanan).append("</b></div>")
				.append("<div style='margin-top:10px;padding:11px;background:#f0fdf4;border-left:4px solid #16a34a'><b>Setelah diperbaiki, lakukan verifikasi:</b>")
				.append("<ol style='margin:6px 0 0 20px;padding:0'><li>Simpan konfigurasi dan pastikan tidak ada kolom wajib yang kosong.</li>")
				.append("<li>Kembali ke Pembayaran Mahasiswa, lalu klik Refresh atau Proses Tagihan.</li>")
				.append("<li>Jalankan Analisis Data kembali; seluruh tahap seharusnya tetap memiliki kandidat.</li>")
				.append("<li>Periksa nama item, nominal, semester, dan sisa tagihan sebelum memproses pembayaran.</li></ol></div>")
				.append("<div style='margin-top:8px;color:#7c2d12;font-size:11px'><b>Penting:</b> jangan mengubah data akademik mahasiswa hanya agar cocok dengan setting. Ubah data mahasiswa hanya bila datanya memang salah; selain itu buat varian Setting Biaya yang benar.</div></div>");
		new Html(html.toString()).setParent(isi);
		/* FIX 19-08-2026: jendela hasil analisis DIBUAT dan diisi, tetapi tidak pernah
		 * DITAMPILKAN -- tidak ada onModal()/setVisible sehingga pengguna menekan tombol
		 * "Analisis Data" dan tidak terjadi apa-apa. Varian sekolah
		 * (AnalisisTagihanSekolahHelper) sudah benar karena diakhiri onModal(). */
		window.setVisible(true);
		window.onModal();
	}

	/**
	 * Menormalisasi hasil audit khusus mahasiswa lama ke kontrak keputusan bersama. Nilai
	 * pembayaran sengaja memakai agregat CicilanPembayaran dari database pada {@code hilir},
	 * sedangkan nominal sumber dihitung dari baris yang benar-benar sedang ditampilkan. Dengan
	 * pemisahan ini popup dapat menjelaskan pembayaran sebagian, lunas, atau selisih tanpa
	 * menganggap nilai yang baru diketik tetapi belum disimpan sebagai transaksi sah.
	 */
	private DaftarUlangTagihanAnalisisHelper.Data buatDataRingkasanAnalisisTagihan(
			String rekomendasi, String tindakan, int khusus, int kandidat, int detail, int bulanan,
			int smt, AnalisisHilirTagihanLama hilir, List<SettingBiaya> kandidatSumber) {
		DaftarUlangTagihanAnalisisHelper.Data data = new DaftarUlangTagihanAnalisisHelper.Data();
		data.identitas = mahasiswa == null ? "-" : mahasiswa.getNim() + " - " + mahasiswa.getNama();
		data.jenisPembayaran = namaObjekAnalisis(jenisKegiatan);
		data.statusAkademik = namaObjekAnalisis(statusmahasiswa);
		data.semester = smt;
		data.settingKhusus = khusus;
		data.kandidatSetting = kandidat;
		data.itemBiayaAktif = detail;
		data.pengaturanBulanan = bulanan;
		data.kesimpulanTeknis = rekomendasi;
		data.tindakanTeknis = tindakan;
		data.nominalTagihanTampil = DaftarUlangTagihanAnalisisHelper
				.hitungNominalTagihanTampil(dataTagihanData);
		if (hilir != null) {
			data.mode = hilir.mode;
			data.templateAkhir = hilir.templateAkhir;
			data.hasilProduksi = hilir.hasilQueryProduksi;
			data.kegiatan = hilir.kegiatan;
			data.cicilan = hilir.cicilan;
			data.barisLayar = hilir.barisLayar;
			data.nilaiDibayarCommitted = hilir.nilaiDibayar;
		}
		DaftarUlangTagihanAnalisisHelper.hitungModeSetting(data, kandidatSumber);
		return data;
	}

	/** Membentuk Setting Biaya baru yang sudah dipraisi sesuai mahasiswa hasil analisis. */
	private SettingBiaya buatSettingBiayaDariMahasiswa(int smt, Jurusan jurusan) {
		SettingBiaya setting = new SettingBiaya();
		BiodataCalonMahasiswa biodata = biodataCalonUntukAnalisisLama();
		setting.setJenisKegiatan(jenisKegiatan);
		setting.setAngkatan(mahasiswa.getTahunangkatan());
		setting.setJenjang(jurusan != null ? jurusan.getJenjang() : mahasiswa.getJenjang());
		setting.setJurusan(jurusan);
		setting.setStatusMahasiswa(statusmahasiswa);
		setting.setStatusAwalMahasiswa(tempHistoryStatusMahasiswa == null
				? mahasiswa.getStatusAwalMahasiswa() : tempHistoryStatusMahasiswa.getStatusAwalMahasiswa());
		setting.setJenisSeleksi(mahasiswa.getJenisSeleksi());
		setting.setGelombangPendaftaran(mahasiswa.getGelombangPendaftaran());
		setting.setPaket(biodata == null ? null : biodata.getPaket());
		setting.setProgram(tempHistoryStatusMahasiswa == null
				? mahasiswa.getProgram() : tempHistoryStatusMahasiswa.getProgram());
		setting.setMinSmt(Integer.valueOf(smt));
		setting.setMaxSmt(Integer.valueOf(smt));
		return setting;
	}

	private BiodataCalonMahasiswa biodataCalonUntukAnalisisLama() {
		try {
			return mahasiswa == null ? null : mahasiswa.getBiodataCalonMahasiswaData();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "AnalisisTagihan: biodata mahasiswa lama");
			return null;
		}
	}

	/** URL filter cerdas menuju sumber tagihan bulanan mahasiswa yang sedang dibuka. */
	private String nilaiParameterTambahanAnalisisLama(BiodataMahasiswa biodata, int urutan) {
		String[] keys = new String[] { "tambah_dan_aktifkan_filter_ke_1_paramater_tambahan",
				"tambah_dan_aktifkan_filter_ke_2_paramater_tambahan",
				"tambah_dan_aktifkan_filter_ke_3_paramater_tambahan" };
		if (biodata == null || urutan < 1 || urutan > keys.length) return "";
		Konfigurasi konfigurasi = Common.getKonfigurasi(keys[urutan - 1], Konfigurasi.TIDAK_AKTIF, "-1", "", "");
		if (konfigurasi == null || !Konfigurasi.AKTIF.equals(konfigurasi.getNilai())
				|| konfigurasi.getInfo1() == null) return "";
		String idParameter = konfigurasi.getInfo1().trim();
		String data = biodata.getParameterTambahanInds();
		if (data == null || data.trim().isEmpty()) return "";
		for (String baris : data.split("\\n")) {
			String[] bagian = baris.split("<=>");
			String[] identitas = bagian.length > 0 ? bagian[0].trim().split("->") : new String[0];
			String nilai = bagian.length > 1 ? bagian[1].trim() : "";
			if (identitas.length > 1 && idParameter.equals(identitas[1].trim()) && !nilai.isEmpty()) {
				return idParameter + "<=>" + nilai;
			}
		}
		return "";
	}

	private String urlPengaturanBulananAnalisisLama(int smt, Jurusan jurusan) throws Exception {
		Jenjang jenjang = jurusan != null && jurusan.getJenjang() != null ? jurusan.getJenjang()
				: mahasiswa.getJenjang();
		BiodataCalonMahasiswa biodata = biodataCalonUntukAnalisisLama();
		Paket paket = biodata == null ? null : biodata.getPaket();
		Integer tahunAkademikMulai = Common.getTahunAkademik(smt, mahasiswa.getTahunangkatan(),
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
		String tahunAjaran = tahunAkademikMulai == null ? ""
				: tahunAkademikMulai + "/" + (tahunAkademikMulai.intValue() + 1);
		String program = tempHistoryStatusMahasiswa == null
				? mahasiswa.getProgram() : tempHistoryStatusMahasiswa.getProgram();
		BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata(false);
		Object statusAwal = tempHistoryStatusMahasiswa == null
				? mahasiswa.getStatusAwalMahasiswa() : tempHistoryStatusMahasiswa.getStatusAwalMahasiswa();
		return "/pages/master/detail_biaya_excel.zul?searchSemester=" + smt
				+ "&searchTahunAjaran=" + URLEncoder.encode(tahunAjaran, "UTF-8")
				+ "&labelAngkatan=" + (mahasiswa.getTahunangkatan() == null ? -1 : mahasiswa.getTahunangkatan())
				+ "&searchMulaiBelajarDiSemester=" + URLEncoder.encode(mahasiswa.getSemesterMulai() == null
						? "" : mahasiswa.getSemesterMulai(), "UTF-8")
				+ "&searchProgram=" + URLEncoder.encode(program == null ? "" : program, "UTF-8")
				+ "&searchWargaNegara=" + URLEncoder.encode(mahasiswa.getWarganegara() == null
						? "" : mahasiswa.getWarganegara(), "UTF-8")
				+ "&searchJenjang=" + (jenjang == null || jenjang.getId() == null ? -1 : jenjang.getId())
				+ "&searchJurusan=" + (jurusan == null || jurusan.getId() == null ? -1 : jurusan.getId())
				+ "&searchStatusMahasiswa=" + (statusmahasiswa == null || statusmahasiswa.getId() == null
						? -1 : statusmahasiswa.getId())
				+ "&searchStatusAwalMahasiswa=" + (statusAwal == null ? -1
						: ((ais.database.model.StatusAwalMahasiswa) statusAwal).getId())
				+ "&searchJenisKegiatan=" + (jenisKegiatan == null || jenisKegiatan.getId() == null
						? -1 : jenisKegiatan.getId())
				+ "&searchPaket=" + (paket == null || paket.getId() == null ? -1 : paket.getId())
				+ "&searchJenisSeleksi=" + (mahasiswa.getJenisSeleksi() == null ? -1
						: mahasiswa.getJenisSeleksi().getId())
				+ "&searchGelombangPendaftaran=" + (mahasiswa.getGelombangPendaftaran() == null ? -1
						: mahasiswa.getGelombangPendaftaran().getId())
				+ "&searchKelas=" + (mahasiswa.getKelasPmb() == null
						|| mahasiswa.getKelasPmb().getKelas() == null ? -1
						: mahasiswa.getKelasPmb().getKelas().getId())
				+ "&searchJenisTempatTinggalMahasiswa=" + (biodataMahasiswa == null
						|| biodataMahasiswa.getJenisTinggalMahasiswa() == null ? -1
						: biodataMahasiswa.getJenisTinggalMahasiswa().getId())
				+ "&searchTambahan1=" + URLEncoder.encode(
						nilaiParameterTambahanAnalisisLama(biodataMahasiswa, 1), "UTF-8")
				+ "&searchTambahan2=" + URLEncoder.encode(
						nilaiParameterTambahanAnalisisLama(biodataMahasiswa, 2), "UTF-8")
				+ "&searchTambahan3=" + URLEncoder.encode(
						nilaiParameterTambahanAnalisisLama(biodataMahasiswa, 3), "UTF-8")
				+ "&kunciFilterAnalisis=1"
				+ "&autoBukaRencanaAngsuran=1";
	}

	private String htmlAnalisisHilirTagihanLama(AnalisisHilirTagihanLama hilir) {
		if (hilir == null) return "";
		StringBuffer html = new StringBuffer();
		html.append("<div style='margin-top:14px;padding:9px;background:#ecfeff;border-left:4px solid #0891b2'><b>Asal-usul tagihan yang diperiksa</b><br>")
				.append("SettingBiaya &rarr; DetailSettingBiaya/ItemBiaya &rarr; DetailBiaya &rarr; ")
				.append(escHtmlTagihan(hilir.mode)).append(" &rarr; Kegiatan/Cicilan Pembayaran &rarr; layar pembayaran.</div>")
				.append("<h4 style='margin:12px 0 6px'>Audit template DetailBiaya (kriteria produksi)</h4>")
				.append("<table style='width:100%;border-collapse:collapse;font-size:12px'><tr style='background:#e5e7eb'>")
				.append("<th style='padding:6px;border:1px solid #d1d5db'>No.</th><th style='padding:6px;border:1px solid #d1d5db;text-align:left'>Kriteria template</th>")
				.append("<th style='padding:6px;border:1px solid #d1d5db;text-align:left'>Nilai mahasiswa</th><th style='padding:6px;border:1px solid #d1d5db'>Tersisa</th>")
				.append("<th style='padding:6px;border:1px solid #d1d5db'>Jika dilewati</th><th style='padding:6px;border:1px solid #d1d5db'>Status</th></tr>");
		for (int i = 0; i < hilir.tahap.size(); i++) {
			TahapAnalisisDetailLama t = hilir.tahap.get(i);
			boolean terbukti = hilir.templateAkhir == 0 && t.jumlahJikaDilewati > 0;
			String status = terbukti ? "&#9888; PENYEBAB TERBUKTI" : t.gagalPertama ? "&#10060; TITIK GAGAL"
					: t.jumlah > 0 ? "&#10004; Cocok" : "- Tetap kosong";
			html.append("<tr style='background:").append(terbukti || t.gagalPertama ? "#fee2e2" : t.jumlah > 0 ? "#f0fdf4" : "#f9fafb")
					.append("'><td style='padding:6px;border:1px solid #d1d5db;text-align:center'>").append(i + 1)
					.append("</td><td style='padding:6px;border:1px solid #d1d5db'><b>").append(escHtmlTagihan(t.nama))
					.append("</b></td><td style='padding:6px;border:1px solid #d1d5db'>").append(escHtmlTagihan(t.nilai))
					.append("</td><td style='padding:6px;border:1px solid #d1d5db;text-align:center'>").append(t.jumlah)
					.append("</td><td style='padding:6px;border:1px solid #d1d5db;text-align:center'>").append(i == 0 ? "-" : String.valueOf(t.jumlahJikaDilewati))
					.append("</td><td style='padding:6px;border:1px solid #d1d5db;text-align:center'>").append(status).append("</td></tr>");
		}
		html.append("</table><div style='margin-top:9px;padding:9px;background:#f8fafc;border:1px solid #cbd5e1'>Mode: <b>")
				.append(escHtmlTagihan(hilir.mode)).append("</b> &nbsp;|&nbsp; Template akhir: <b>").append(hilir.templateAkhir)
				.append("</b> &nbsp;|&nbsp; Hasil query produksi: <b>").append(hilir.hasilQueryProduksi)
				.append("</b> &nbsp;|&nbsp; Kegiatan: <b>").append(hilir.kegiatan).append("</b> &nbsp;|&nbsp; Cicilan/transaksi: <b>")
				.append(hilir.cicilan).append("</b> &nbsp;|&nbsp; Nilai dibayar: <b>").append(Common.numberFormat.get().format(hilir.nilaiDibayar))
				.append("</b> &nbsp;|&nbsp; Baris layar: <b>").append(hilir.barisLayar).append("</b></div>")
				.append("<div style='margin-top:8px;padding:9px;background:#fafafa;border:1px solid #e5e7eb'><b>Sumber item yang ditemukan:</b><br>")
				.append(escHtmlTagihan(hilir.sumberItem)).append("</div>");
		return html.toString();
	}

	private String diagnosaTagihanTidakMuncul(int smt, int jumlahBaris) {
		try {
			if (jumlahBaris != 0 && !totalTagihanTampilNol()) {
				return "";
			}
			List<String> alasan = new ArrayList<String>();
			if (jenisKegiatan != null && diLuarRangeTagihan(smt, jenisKegiatan.getMinSmt(), jenisKegiatan.getMaxSmt())) {
				alasan.add("Jenis kegiatan \"" + escHtmlTagihan(jenisKegiatan.getNamaKegiatan())
						+ "\" hanya berlaku semester " + rangeTextTagihan(jenisKegiatan.getMinSmt(), jenisKegiatan.getMaxSmt()) + ".");
			}
			List<DetailBiaya> kandidat = kumpulkanDetailBiayaDiagnosa();
			for (DetailBiaya db : kandidat) {
				if (db == null) continue;
				String nama = namaDetailBiayaDiagnosa(db);
				try {
					if (db.getItemBiaya() != null
							&& diLuarRangeTagihan(smt, db.getItemBiaya().getMinSmt(), db.getItemBiaya().getMaxSmt())) {
						alasan.add("Item biaya \"" + escHtmlTagihan(nama) + "\" hanya berlaku semester "
								+ rangeTextTagihan(db.getItemBiaya().getMinSmt(), db.getItemBiaya().getMaxSmt()) + ".");
					}
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "DaftarUlangLama:diagnosa item biaya"); }
				try {
					if (db.getSettingBiaya() != null
							&& diLuarRangeTagihan(smt, db.getSettingBiaya().getMinSmt(), db.getSettingBiaya().getMaxSmt())) {
						alasan.add("Setting biaya untuk \"" + escHtmlTagihan(nama) + "\" hanya berlaku semester "
								+ rangeTextTagihan(db.getSettingBiaya().getMinSmt(), db.getSettingBiaya().getMaxSmt()) + ".");
					}
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "DaftarUlangLama:diagnosa setting biaya"); }
				try {
					if (db.getSettingBiayaDetail() != null
							&& diLuarRangeTagihan(smt, db.getSettingBiayaDetail().getMinSmt(), db.getSettingBiayaDetail().getMaxSmt())) {
						alasan.add("Detail setting biaya untuk \"" + escHtmlTagihan(nama) + "\" hanya berlaku semester "
								+ rangeTextTagihan(db.getSettingBiayaDetail().getMinSmt(), db.getSettingBiayaDetail().getMaxSmt()) + ".");
					}
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "DaftarUlangLama:diagnosa detail setting biaya"); }
			}
			if (alasan.isEmpty()) {
				alasan.add("Tidak ada baris tagihan aktif yang cocok dengan kombinasi semester, jenis pembayaran, prodi/jenjang, angkatan, status mahasiswa, program, paket/gelombang, atau tagihan sudah lunas.");
			}
			StringBuffer sb = new StringBuffer();
			sb.append("<div style=\"margin-top:6px;font-size:11.5px;opacity:.96;\"><b>Kemungkinan penyebab:</b><ul style=\"margin:3px 0 0 18px;padding:0;\">");
			for (int i = 0; i < alasan.size() && i < 5; i++) {
				sb.append("<li>").append(alasan.get(i)).append("</li>");
			}
			sb.append("</ul></div>");
			return sb.toString();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "DaftarUlangMahasiswaLamaAction: diagnosa tagihan gagal");
			return "";
		}
	}

	private boolean totalTagihanTampilNol() {
		double total = 0.0;
		if (dataTagihanData != null) {
			for (Object o : dataTagihanData) {
				try {
					if (o instanceof PengaturanPembayaranBulanan) {
						PengaturanPembayaranBulanan p = (PengaturanPembayaranBulanan) o;
						total += p.getNominal() == null ? 0.0 : p.getNominal().doubleValue();
					} else if (o instanceof DetailBiaya) {
						DetailBiaya db = (DetailBiaya) o;
						Double n = db.getNilaiBiayaBaru() == null ? db.getNilaiBiaya() : db.getNilaiBiayaBaru();
						total += n == null ? 0.0 : n.doubleValue();
					}
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "DaftarUlangLama:total tagihan tampil"); }
			}
		}
		return total <= 0.01;
	}

	@SuppressWarnings("unchecked")
	private List<DetailBiaya> kumpulkanDetailBiayaDiagnosa() {
		List<DetailBiaya> hasil = new ArrayList<DetailBiaya>();
		if (dataTagihanData != null) {
			for (Object o : dataTagihanData) {
				try {
					if (o instanceof PengaturanPembayaranBulanan) {
						DetailBiaya db = ((PengaturanPembayaranBulanan) o).getDetailBiaya();
						if (db != null && !hasil.contains(db)) hasil.add(db);
					} else if (o instanceof DetailBiaya && !hasil.contains(o)) {
						hasil.add((DetailBiaya) o);
					}
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "DaftarUlangLama:kumpul detail diagnosa"); }
			}
		}
		if (!hasil.isEmpty() || jenisKegiatan == null || jenisKegiatan.getId() == null) {
			return hasil;
		}
		Session s = null;
		try {
			s = HibernateUtil.openSession();
			hasil.addAll(s.createCriteria(DetailBiaya.class)
					.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(Order.desc("id"))
					.setMaxResults(20).list());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "DaftarUlangMahasiswaLamaAction: query diagnosa tagihan");
		} finally {
			HibernateUtil.closeSessionQuietly(s);
		}
		return hasil;
	}

	private boolean diLuarRangeTagihan(int smt, Integer min, Integer max) {
		int mi = min == null ? 0 : min.intValue();
		int ma = max == null ? 30 : max.intValue();
		return smt < mi || smt > ma;
	}

	private String rangeTextTagihan(Integer min, Integer max) {
		return (min == null ? "0" : min.toString()) + " s.d. " + (max == null ? "30" : max.toString());
	}

	private String namaDetailBiayaDiagnosa(DetailBiaya db) {
		try {
			return db.getItemBiaya() == null ? "Tanpa nama" : db.getItemBiaya().getNama();
		} catch (Exception e) {
			return "Tanpa nama";
		}
	}

	private String escHtmlTagihan(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void inputSesuaiTagihan() throws Exception {
		JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
				: jenisPembayaran.getSelectedItem().getValue());
		if (jenisKegiatan != null) {
			Common.clear(rowsCicilan);
			int i = 0;
			double sisaSaldoIsi = capSaldoIsiCicilan; // mode "Dari Tabungan" (cap saldo)
			List<Row> rows = gridss.getRows().getChildren();
			for (Row rowData : rows) {
				ItemBiaya itemBiaya = null;
				DetailBiaya detailBiaya = null;
				if (rowData.getAttribute("pengaturanPembayaranBulanan") != null) {
					PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) rowData
							.getAttribute("pengaturanPembayaranBulanan");
					detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
					itemBiaya = detailBiaya.getItemBiaya();
				} else if (rowData.getAttribute("myValue") != null) {
					detailBiaya = (DetailBiaya) rowData.getAttribute("myValue");
					itemBiaya = detailBiaya.getItemBiaya();
				}

				if (itemBiaya == null)
					continue;

				Double kekurangan = 0.0;
				try {
					kekurangan = Common.numberFormat.get()
							.parse(((MyLabelAgakKecil) rowData.getAttribute("kurang")).getValue()).doubleValue();
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}

				CicilanPembayaran cicilanPembayaran = new CicilanPembayaran(detailBiaya);
				for (CicilanPembayaran c : cicilanPembayarans) {
					if (c.getItemBiaya().getId().equals(itemBiaya.getId())) {
						cicilanPembayaran = c;
						break;
					}
				}

				cicilanPembayaran.setBayarKe(detailBiaya.getBayarKe());
				cicilanPembayaran.setItemBiaya(itemBiaya);
				cicilanPembayaran.setKe((i + 1));
				cicilanPembayaran.setKegiatan(kegiatan);
				// Mode "Dari Tabungan" (capSaldoIsiCicilan>0): tiap tagihan dibatasi sisa saldo →
				// otomatis terangsur sesuai saldo, tak melebihi saldo (habis → 0, baris tak tampil).
				double nilaiIsiCicilan = kekurangan;
				if (capSaldoIsiCicilan > 0.1) {
					if (kekurangan <= 0.1 || sisaSaldoIsi <= 0.1) {
						nilaiIsiCicilan = 0.0;
					} else {
						nilaiIsiCicilan = Math.min(kekurangan, sisaSaldoIsi);
						sisaSaldoIsi -= nilaiIsiCicilan;
					}
				}
				cicilanPembayaran.setNilai(nilaiIsiCicilan);
				cicilanPembayaran.setNilaiAsli(nilaiIsiCicilan);
				cicilanPembayaran.setValidator(tbmuser == null ? "" : tbmuser.toString());

				boolean tidakBolehUbah = cicilanPembayaran.getId() != null
						|| (tbmuser != null
								&& (tbmuser.getBiodataCalonMahasiswa() != null || tbmuser.getMahasiswa() != null)
								&& cicilanPembayaran.getItemBiaya() != null
								&& !cicilanPembayaran.getItemBiaya().getMahasiswaBolehMencicilkan())
						|| (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null
								&& tbmuser.getMahasiswa() == null && cicilanPembayaran.getItemBiaya() != null
								&& !cicilanPembayaran.getItemBiaya().getAdminBolehMencicilkan());

				if (cicilanPembayaran.getNilai() > 0.1 || cicilanPembayaran.getNilai() < -0.1) {
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					Hbox hboxLampiran = Common.initCicilan(buktiPembayarans, rowsCicilan, row, i, cicilanPembayaran,
							null);
					Common.freeze(hboxLampiran, false);
					hboxLampiran.setVisible(true);
					i++;

					final Textbox keterangan = new Textbox(
							cicilanPembayaran == null ? "" : cicilanPembayaran.getKeterangan());
					keterangan.setRows(2);
					final Combobox myCaraBayar = new Combobox();
					myCaraBayar.setReadonly(true);
					myCaraBayar.setAttribute("janganDisabled", true);
					final MyDoublebox jumlahCicilan = new MyDoublebox(
							cicilanPembayaran == null ? 0.0 : cicilanPembayaran.getNilai());

					if (ItemBiaya.DIKALI_NILAI_MINUS.equals(itemBiaya.getPenghitungan())) {
						for (MyDoubleboxMin kurang : pengurangan) {
							DetailBiaya penguranganItemBiaya = (DetailBiaya) kurang.getAttribute("itemBiaya");
							if (penguranganItemBiaya != null
									&& penguranganItemBiaya.getId().equals(detailBiaya.getId())) {
								Double nom = kurang.getValue() == null ? 0.0 : kurang.getValue();
								jumlahCicilan.setValue(nom);
								break;
							}
						}
					}

					jumlahCicilan.setWidth("90%");
					if (tidakBolehUbah) {
						jumlahCicilan.disabledPaksa(tidakBolehUbah);
						row.appendChild(new Label(Common.numberFormat.get()
								.format(cicilanPembayaran == null ? 0.0 : cicilanPembayaran.getNilai())));
					} else {
						row.appendChild(jumlahCicilan);
					}
					jumlahCicilan.addEventListener("onChange", jumlahCicilahEventListener);
					jumlahCicilan.setAttribute("jumlahCicilanEventListener", jumlahCicilahEventListener);

					final MyDatebox tanggal = new MyDatebox(cicilanPembayaran == null ? ais.ui.util.WaktuUtil.getDate()
							: cicilanPembayaran.getTanggal());
					tanggal.setFormat(Common.dateFormat31.get().toPattern());
					tanggal.setWidth("90%");
					final MyDatebox tanggalKwitansi = new MyDatebox(
							cicilanPembayaran == null ? ais.ui.util.WaktuUtil.getDate()
									: cicilanPembayaran.getTanggalKwitansi());
					tanggalKwitansi.setFormat(Common.dateFormat31.get().toPattern());
					tanggalKwitansi.setWidth("90%");

					if (tampilkanTanggalKwitansi) {
						Vbox v = new Vbox();
						v.appendChild(tanggal);
						v.appendChild(tanggalKwitansi);
						row.appendChild(v);
					} else {
						row.appendChild(tanggal);
					}

					final Combobox myItemBiaya = new Combobox();
					myItemBiaya.setReadonly(true);
					myItemBiaya.setWidth("90%");
					row.appendChild(myItemBiaya);
					myItemBiaya.setDisabled(true);
					Common.insertComboItems(myItemBiaya, "", new ArrayList(itemBiayas.values()));
					if (cicilanPembayaran.getItemBiaya() != null)
						Common.selectComboItem(myItemBiaya, detailBiaya);

					myCaraBayar.setWidth("90%");
					// Wizard: cara bayar dipilih di langkah 4 → sembunyikan combo per-baris.
					if (modeWizardRingkas) {
						myCaraBayar.setVisible(false);
					}
					row.appendChild(myCaraBayar);

					Session sessionCombo = null;
					try {
						sessionCombo = HibernateUtil.openSession();
						if (cicilanPembayaran != null && cicilanPembayaran.getJenisPembayaran() == null) {
							JenisPembayaran jenisPembayaranDefault = (JenisPembayaran) sessionCombo
									.createCriteria(JenisPembayaran.class)
									.add(Restrictions.eq("defaultPembayaran", true))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.setMaxResults(1).uniqueResult();
							cicilanPembayaran.setJenisPembayaran(jenisPembayaranDefault);
						}

						Common.insertCombo(myCaraBayar, "nama", "akun", JenisPembayaran.class,
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					} finally {
						if (sessionCombo != null) {
							try {
								sessionCombo.clear();
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}
							try {
								sessionCombo.disconnect();
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}
							try {
								sessionCombo.close();
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}
						}
					}

					Common.selectComboItem(myCaraBayar,
							cicilanPembayaran == null || cicilanPembayaran.getJenisPembayaran() == null
									? ConstantValues.TUNAI
									: cicilanPembayaran.getJenisPembayaran());

					keterangan.setWidth("90%");
					row.appendChild(keterangan);

					MyToolbarbuttonConfig buttonHapus = (MyToolbarbuttonConfig) row.getAttribute("buttonHapus");
					if (buttonHapus != null)
						buttonHapus.setVisible(!jadwalPembayaran.getJenisKegiatan().getTidakBolehMengangsur());

					row.setValign("top");
					row.setAttribute("jumlahCicilan", jumlahCicilan);
					row.setValign("top");
					row.setAttribute("tanggal", tanggal);
					row.setValign("top");
					row.setAttribute("tanggalKwitansi", tanggalKwitansi);
					row.setValign("top");
					row.setAttribute("itemBiaya", myItemBiaya);
					row.setValign("top");
					row.setAttribute("caraBayar", myCaraBayar);
					row.setValign("top");
					row.setAttribute("keterangan", keterangan);

					if (tidakBolehUbah)
						Common.freeze(row, true);
				}
			}
		}
		jumlahCicilahEventListener.onEvent(null);
	}

	@SuppressWarnings({ "unchecked" })
	public void inputSesuaiTagihanBulanan(Integer bulan) throws Exception {
		List<Long> telahDibayar = new ArrayList<Long>();
		for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
			if (cicilanPembayaran.getPengaturanPembayaranBulanan() != null)
				telahDibayar.add(cicilanPembayaran.getPengaturanPembayaranBulanan().getId());
		}
		List<PengaturanPembayaranBulanan> yangBelumDibayar = new ArrayList<PengaturanPembayaranBulanan>();
		for (PengaturanPembayaranBulanan pengaturanPembayaranBulanan : pengaturanPembayaranBulanans) {
			if (bulan == null || pengaturanPembayaranBulanan.getRealBulan().equals(bulan)) {
				Double nom = pengaturanPembayaranBulanan.getNominal();
				if (nom > 0.1 && !telahDibayar.contains(pengaturanPembayaranBulanan.getId()))
					yangBelumDibayar.add(pengaturanPembayaranBulanan);
			}
		}

		for (PengaturanPembayaranBulanan pengaturanPembayaranBulanan : pengaturanPembayaranBulanans) {
			if (bulan == null || pengaturanPembayaranBulanan.getRealBulan().equals(bulan)) {
				for (MyDoubleboxMin kurang : pengurangan) {
					DetailBiaya penguranganItemBiaya = (DetailBiaya) kurang.getAttribute("itemBiaya");
					if (penguranganItemBiaya != null && penguranganItemBiaya.getId()
							.equals(pengaturanPembayaranBulanan.getDetailBiaya().getId())) {
						Double nom = kurang.getValue() == null ? 0.0 : kurang.getValue();
						if (nom < -0.01) {
							yangBelumDibayar.add(pengaturanPembayaranBulanan);
							break;
						}
					}
				}
			}
		}

		List<Row> mycicilanrows = (gridCicilan == null || gridCicilan.getRows() == null)
				? new java.util.ArrayList<Row>()
				: gridCicilan.getRows().getChildren();
		int i = 0;
		Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
				: kegiatan.ambilDetailKegiatan(refresh);

		for (final Row row : mycicilanrows) {
			CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");
			if (cicilanPembayaran == null)
				cicilanPembayaran = new CicilanPembayaran(null);

			boolean tidakBolehUbah = cicilanPembayaran.getId() != null
					|| (tbmuser != null
							&& (tbmuser.getBiodataCalonMahasiswa() != null || tbmuser.getMahasiswa() != null)
							&& cicilanPembayaran.getItemBiaya() != null
							&& !cicilanPembayaran.getItemBiaya().getMahasiswaBolehMencicilkan())
					|| (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getMahasiswa() == null
							&& cicilanPembayaran.getItemBiaya() != null
							&& !cicilanPembayaran.getItemBiaya().getAdminBolehMencicilkan());

			if (cicilanPembayaran.getPengaturanPembayaranBulanan() == null) {
				final PengaturanPembayaranBulanan pembayaranBulanan = i >= yangBelumDibayar.size() ? null
						: yangBelumDibayar.get(i);
				if (pembayaranBulanan != null && pembayaranBulanan.getPersentase() > 0.1) {
					Hbox hboxLampiran = (Hbox) row.getAttribute("hboxLampiran");
					hboxLampiran.setVisible(true);

					Double nilai = Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans, mahasiswa,
							Integer.parseInt(semester.getValue()), pembayaranBulanan);
					JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
							&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
									? jadwalPembayaran
									: null;
					nilai = pembayaranBulanan.checkDenda(nilai, ais.ui.util.WaktuUtil.getDate(), jdw, jenisKegiatan);

					MyToolbarbuttonConfig buttonHapus = (MyToolbarbuttonConfig) row.getAttribute("buttonHapus");
					if (buttonHapus != null)
						buttonHapus.setVisible(!jadwalPembayaran.getJenisKegiatan().getTidakBolehMengangsur());

					cicilanPembayaran.setPengaturanPembayaranBulanan(pembayaranBulanan);
					cicilanPembayaran.setValidator(tbmuser.getUserId());
					final MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
					jumlahCicilan.setValue(nilai);
					cicilanPembayaran.setNilai(nilai);
					cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
					final MyDatebox tanggal = (MyDatebox) row.getAttribute("tanggal");
					final MyDatebox tanggalKwitansi = (MyDatebox) row.getAttribute("tanggalKwitansi");
					final Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
							&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan")
									: null);

					tanggal.setValue(ais.ui.util.WaktuUtil.getDate());
					tanggal.setDisabled(false);
					tanggalKwitansi.setValue(ais.ui.util.WaktuUtil.getDate());
					tanggalKwitansi.setDisabled(false);
					final CicilanPembayaran tempCicilanPembayaran = cicilanPembayaran;

					tanggal.addEventListener("onChange", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							Double nom = pembayaranBulanan.ambilNominalModifikasi(mahasiswa,
									Integer.parseInt(semester.getValue()));
							Double denda = 0.0;
							if (ItemBiaya.DIKALI_NILAI_MINUS
									.equals(pembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan())) {
								for (MyDoubleboxMin kurang : pengurangan) {
									DetailBiaya penguranganItemBiaya = (DetailBiaya) kurang.getAttribute("itemBiaya");
									if (penguranganItemBiaya != null && penguranganItemBiaya.getId()
											.equals(pembayaranBulanan.getDetailBiaya().getId())) {
										nom = kurang.getValue() == null ? 0.0 : kurang.getValue();
										break;
									}
								}
							} else {
								JadwalPembayaran jdw = jadwalPembayaran != null
										&& jadwalPembayaran.getKhususUntukNim() != null
										&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
												? jadwalPembayaran
												: null;
								denda = pembayaranBulanan.checkDenda(nom, tanggal.getValue(), jdw, jenisKegiatan) - nom;
							}
							tempCicilanPembayaran.setValidator(tbmuser.getUserId());
							tempCicilanPembayaran.setDenda(denda);
							tempCicilanPembayaran.setPengaturanPembayaranBulanan(pembayaranBulanan);
							tempCicilanPembayaran.setTanggal(tanggal.getValue());
							tempCicilanPembayaran.setTanggalKwitansi(tanggalKwitansi.getValue());
							keterangan.setValue(tempCicilanPembayaran.getKeterangan());
							keterangan.setDisabled(false);
							jumlahCicilan.setValue(nom + denda);
							row.setValign("top");
							row.setAttribute("cicilanPembayaran", tempCicilanPembayaran);
						}
					});

					Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
					Common.selectComboItem(myItemBiaya, pembayaranBulanan);
					Combobox myCaraBayar = (Combobox) row.getAttribute("caraBayar");
					keterangan.setValue(cicilanPembayaran.getKeterangan());
					keterangan.setDisabled(false);

					String val = cicilanPembayaran == null ? null : cicilanPembayaran.getValidator();
					if (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null"))
						val = (tbmuser == null ? "" : tbmuser.toString());

					cicilanPembayaran.setValidator(val);
					cicilanPembayaran.setKegiatan(kegiatan);
					cicilanPembayaran.setKeterangan(keterangan.getValue());
					cicilanPembayaran.setItemBiaya(pembayaranBulanan.getDetailBiaya().getItemBiaya());
					cicilanPembayaran.setPengaturanPembayaranBulanan(pembayaranBulanan);
					cicilanPembayaran.setNilai(jumlahCicilan.getValue());
					cicilanPembayaran.setTanggal(tanggal.getValue());
					cicilanPembayaran.setTanggalKwitansi(tanggalKwitansi.getValue());
					cicilanPembayaran.setJenisPembayaran(
							(JenisPembayaran) (myCaraBayar.getSelectedItem() == null ? ConstantValues.TUNAI
									: myCaraBayar.getSelectedItem().getValue()));
					row.setValign("top");
					row.setAttribute("cicilanPembayaran", cicilanPembayaran);
					cicilanPembayarans.add(cicilanPembayaran);
					i++;

					row.setValign("top");
					row.setAttribute("jumlahCicilan", jumlahCicilan);
					row.setValign("top");
					row.setAttribute("tanggal", tanggal);
					row.setValign("top");
					row.setAttribute("tanggalKwitansi", tanggalKwitansi);
					row.setValign("top");
					row.setAttribute("itemBiaya", myItemBiaya);
					row.setValign("top");
					row.setAttribute("caraBayar", myCaraBayar);
					row.setValign("top");
					row.setAttribute("keterangan", keterangan);
					row.setVisible(true);
					if (tidakBolehUbah)
						Common.freeze(row, true);
				}
			}
		}
		jumlahCicilahEventListener.onEvent(null);
	}

	private boolean validasiPembayaran(final Mahasiswa mahasiswa, boolean checkBukti) throws Exception {
		return validasiPembayaran(mahasiswa, checkBukti, null);
	}

	@SuppressWarnings("unchecked")
	private boolean validasiPembayaran(final Mahasiswa mahasiswa, boolean checkBukti, JenisPembayaran tabungan)
			throws Exception {
		if (gridss != null && gridss.getRows() != null && gridss.getRows().getChildren().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, data tagihan tidak ditemukan. Langkah yang dapat dilakukan: (1) muat ulang data mahasiswa; (2) pastikan mahasiswa telah memiliki tagihan pada semester yang dipilih; (3) apabila memerlukan bantuan, mohon hubungi Administrator sistem.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (gridCicilan != null) {
			Double jumlah = 0.0;
			List<Row> mycicilanrows = (gridCicilan == null || gridCicilan.getRows() == null)
				? new java.util.ArrayList<Row>()
				: gridCicilan.getRows().getChildren();
			Map<Long, Object[]> pengaturanPembayaranBulanansMap = new HashMap<Long, Object[]>();
			double totalDeposit = 0.0;

			for (Row row : mycicilanrows) {
				try {
					MyDatebox tanggal = (MyDatebox) row.getAttribute("tanggal");
					Combobox myJenisPembayaran = (Combobox) row.getAttribute("caraBayar");
					JenisPembayaran jenisPembayaran = (JenisPembayaran) (myJenisPembayaran.getSelectedItem() == null
							? ConstantValues.TUNAI
							: myJenisPembayaran.getSelectedItem().getValue());

					if (tabungan != null)
						jenisPembayaran = tabungan;

					MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
					Double c = (jumlahCicilan.getValue() == null ? 0.0 : jumlahCicilan.getValue());

					CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");
					if (cicilanPembayaran != null && cicilanPembayaran.getId() == null) {
						if (jenisPembayaran != null && jenisPembayaran.getJenisTabungan() != null)
							totalDeposit += c;
					}

					if (cicilanPembayaran != null && cicilanPembayaran.getId() == null
							&& cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
						PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
								.getPengaturanPembayaranBulanan();
						JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
								&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
										? jadwalPembayaran
										: null;

						if (pengaturanPembayaranBulanansMap
								.containsKey(cicilanPembayaran.getPengaturanPembayaranBulanan().getId())) {
							Double nom = ((Double) pengaturanPembayaranBulanansMap
									.get(cicilanPembayaran.getPengaturanPembayaranBulanan().getId())[0]) + c;
							Double denda = pengaturanPembayaranBulanan.checkDenda(nom, tanggal.getValue(), jdw,
									jenisKegiatan) - nom;
							pengaturanPembayaranBulanansMap.put(
									cicilanPembayaran.getPengaturanPembayaranBulanan().getId(),
									new Object[] { nom, denda, cicilanPembayaran.getPengaturanPembayaranBulanan() });
						} else {
							Double denda = pengaturanPembayaranBulanan.checkDenda(c, tanggal.getValue(), jdw,
									jenisKegiatan) - c;
							pengaturanPembayaranBulanansMap.put(
									cicilanPembayaran.getPengaturanPembayaranBulanan().getId(),
									new Object[] { c, denda, cicilanPembayaran.getPengaturanPembayaranBulanan() });
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (tabungan != null && totalDeposit > 0.1 && mahasiswa != null) {
				if (this.tabungan < totalDeposit) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, nilai deposit tidak mencukupi untuk melakukan pembayaran ini. Nilai pembayaran melalui deposit adalah {V1}, sedangkan sisa deposit yang tersedia hanya {V2}. Langkah yang dapat dilakukan: (1) kurangi nilai pembayaran yang mengambil dari deposit; (2) tambahkan saldo deposit terlebih dahulu; (3) gunakan metode pembayaran lain yang tersedia.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
							Common.numberFormat.get().format(totalDeposit),
							Common.numberFormat.get().format(this.tabungan));
					return false;
				}
			}

			if (Konfigurasi.AKTIF.equals(
					Common.getKonfigurasi("check_apakah_melebihi_tagihan", Konfigurasi.TIDAK_AKTIF).getNilai())) {
				for (Long k : pengaturanPembayaranBulanansMap.keySet()) {
					PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) pengaturanPembayaranBulanansMap
							.get(k)[2];
					Double nom = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa,
							Integer.parseInt(semester.getValue()));
					Double denda = (Double) pengaturanPembayaranBulanansMap.get(k)[1];
					Double n = (Double) pengaturanPembayaranBulanansMap.get(k)[0];
					if ((nom + denda) < n) {
						String namaIBMsg = "";
						try {
							if (pengaturanPembayaranBulanan.getDetailBiaya() != null
									&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null)
								namaIBMsg = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/DaftarUlangMahasiswaLamaAction.java:2321");}
						MyMessageboxConfig.showFormat(
								"Mohon maaf, nilai pembayaran untuk item biaya \"{V1}\" pada bulan {V2} tidak boleh melebihi nilai tagihan. Nilai tagihan adalah {V3}, sedangkan nominal pembayaran yang dimasukkan adalah {V4}. Langkah yang dapat dilakukan: (1) periksa kembali nominal pembayaran yang dimasukkan; (2) sesuaikan agar tidak melebihi nilai tagihan; (3) apabila memerlukan bantuan, mohon hubungi bagian keuangan.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
								namaIBMsg, pengaturanPembayaranBulanan.getNamaBulan(),
								Common.numberFormat.get().format(nom),
								Common.numberFormat.get().format(n));
						return false;
					}
				}
			}

			for (Row row : mycicilanrows) {
				if (Konfigurasi.AKTIF
						.equals(Common.getKonfigurasi("integrasi_modul_akuntansi", Konfigurasi.TIDAK_AKTIF).getNilai())
						&& tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) {
					MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
					if (jumlahCicilan.getValue() != null && jumlahCicilan.getValue() > 0.1) {
						Combobox myJenisPembayaran = (Combobox) row.getAttribute("caraBayar");
						if (!myJenisPembayaran.isDisabled() && myJenisPembayaran.getSelectedItem() == null) {
							MyMessageboxConfig.show("Mohon Bapak/Ibu memilih Cara Bayar terlebih dahulu. Langkah yang dapat dilakukan: (1) buka kolom pilihan Cara Bayar; (2) pilih cara pembayaran yang sesuai; (3) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
							myJenisPembayaran.focus();
							return false;
						}
					}
				}

				if (checkBukti && Konfigurasi.AKTIF.equals(Common
						.getKonfigurasi("harus_menyertakan_bukti_pembayaran", Konfigurasi.TIDAK_AKTIF).getNilai())) {
					MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
					if (jumlahCicilan.getValue() != null
							&& (jumlahCicilan.getValue() > 0.01 || jumlahCicilan.getValue() < -0.01)) {
						CicilanPembayaran cicilanPembayaranSebelumnya = (CicilanPembayaran) row
								.getAttribute("cicilanPembayaran");
						if (cicilanPembayaranSebelumnya != null
								&& cicilanPembayaranSebelumnya.getIdLampiran() == null) {
							MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi bukti pembayaran terlebih dahulu. Langkah yang dapat dilakukan: (1) siapkan berkas bukti pembayaran dalam bentuk gambar atau PDF; (2) tekan tombol unggah dan pilih berkas tersebut; (3) lanjutkan kembali proses penyimpanan.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							return false;
						}
					}
				}

				Combobox itemBiaya = (Combobox) row.getAttribute("itemBiaya");
				MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");

				if ((jumlahCicilan.getValue() == null ? 0.0 : jumlahCicilan.getValue()) > 0.1 && !itemBiaya.isDisabled()
						&& itemBiaya.getSelectedItem() == null) {
					MyMessageboxConfig.show("Mohon Bapak/Ibu memilih Item Biaya terlebih dahulu. Langkah yang dapat dilakukan: (1) buka kolom pilihan Item Biaya; (2) pilih item biaya yang akan dibayar; (3) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					itemBiaya.focus();
					return false;
				}
				jumlah += (jumlahCicilan.getValue() == null ? 0.0 : jumlahCicilan.getValue());
			}
		}

		JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
				: jenisPembayaran.getSelectedItem().getValue());
		if (jenisKegiatan == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu memilih Jenis Pembayaran terlebih dahulu. Langkah yang dapat dilakukan: (1) buka kolom pilihan Jenis Pembayaran; (2) pilih jenis pembayaran yang sesuai; (3) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (hboxJenisPembayaran.isVisible() && akun.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu memilih Cara Pembayaran terlebih dahulu. Langkah yang dapat dilakukan: (1) buka kolom pilihan Cara Pembayaran; (2) pilih cara pembayaran yang sesuai; (3) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			akun.focus();
			return false;
		}

		return true;
	}

	/*
	 * ===== PENGAMAN ANTI-PEMBAYARAN GANDA (double submit) =====
	 * Semua tombol BAYAR bermuara ke onSave(...). Tanpa pengaman, menekan BAYAR
	 * dua kali (bahkan berselang beberapa menit) untuk item & nominal yang sama
	 * membuat record CicilanPembayaran GANDA (gejala yang dilaporkan: dua batch
	 * 07:52 & 07:56 dengan item/nominal identik). Guard di bawah menolak
	 * pembayaran ber-SIGNATURE sama (kegiatan + item + nominal) bila baru saja
	 * sukses diproses dalam rentang cooldown, plus flag in-flight.
	 */
	private volatile boolean bayarSedangDiproses = false;
	private String lastBayarSignature = null;
	private long lastBayarTime = 0L;
	/** Pelewat sekali-pakai guard ganda: disetel true bila admin menegaskan lewat konfirmasi. */
	private boolean lewatiGuardGanda = false;

	@SuppressWarnings({ "unchecked" })
	public boolean onSave(Kegiatan keg, final Mahasiswa mahasiswa, Event event, JenisPembayaran tabungan)
			throws Exception {
		if (tbmuser == null
				|| (tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null)))
			return false;

		kegiatan = keg;
		if (!validasiPembayaran(mahasiswa, true, tabungan))
			return false;

		// Pengaman anti-pembayaran ganda (lihat field & komentar di atas onSave).
		final String bayarSignature = buildBayarSignature(keg);
		final long bayarSekarang = System.currentTimeMillis();
		if (bayarSedangDiproses) {
			MyMessageboxConfig.show(
					"Pembayaran sedang diproses. Mohon tunggu sampai selesai dan jangan menekan tombol bayar berulang kali.",
					"Mohon Tunggu", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (bayarSignature != null && bayarSignature.equals(lastBayarSignature)
				&& (bayarSekarang - lastBayarTime) < getBayarCooldownMs() && !lewatiGuardGanda) {
			// BUKAN blokir keras: pembayaran sah (mis. angsuran berikutnya dgn nominal sama)
			// tak boleh terkunci sampai 5 menit. Tampilkan KONFIRMASI; bila admin menegaskan
			// OK, ulangi onSave dgn melewati guard sekali-pakai. Klik ganda cepat tetap
			// dicegah oleh flag in-flight bayarSedangDiproses di atas.
			final Kegiatan kegKonfirmasi = keg;
			final Mahasiswa mhsKonfirmasi = mahasiswa;
			final Event eventKonfirmasi = event;
			final JenisPembayaran tabunganKonfirmasi = tabungan;
			MyMessageboxConfig.show(
					"Pembayaran dengan rincian (item & nominal) yang sama baru saja diproses.\n\n"
							+ "Bila ini BENAR-BENAR pembayaran berbeda (mis. angsuran berikutnya), tekan OK untuk tetap menyimpan.\n"
							+ "Bila ragu, tekan Batal untuk mencegah pembayaran ganda.",
					"Konfirmasi Kemungkinan Pembayaran Ganda",
					MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.EXCLAMATION,
					new EventListener() {
						@Override
						public void onEvent(Event ev) throws Exception {
							int i = Integer.parseInt(ev.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								lewatiGuardGanda = true;
								onSave(kegKonfirmasi, mhsKonfirmasi, eventKonfirmasi, tabunganKonfirmasi);
							}
						}
					});
			return false;
		}
		lewatiGuardGanda = false;
		bayarSedangDiproses = true;

		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.openSession();
			tx = session.beginTransaction();

			if (kegiatan != null && kegiatan.getId() != null)
				kegiatan = (Kegiatan) session.load(Kegiatan.class, kegiatan.getId());
			else
				kegiatan = new Kegiatan();

			this.mahasiswa = mahasiswa;
			JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
					: jenisPembayaran.getSelectedItem().getValue());
			kegiatan.setJenisKegiatan(jenisKegiatan);
			kegiatan.setJadwalPembayaran(jadwalPembayaran);
			kegiatan.setMahasiswa(mahasiswa);
			kegiatan.setSemster(semester.getValue().isEmpty() ? 1 : Integer.parseInt(semester.getValue()));
			kegiatan.setStatusMahasiswa(statusmahasiswa);
			kegiatan.setTahunAkademik(labelTahunAkademik.getValue() == null ? "" : labelTahunAkademik.getValue());
			kegiatan.setTanggal(WaktuUtil.getDate());
			kegiatan.setValidated(1);
			kegiatan.setValidator(tbmuser == null ? null : tbmuser.getUserNama());

			Double totalpengurangan = 0.0;
			for (MyDoubleboxMin kurang : pengurangan)
				totalpengurangan += kurang.getValue() == null ? 0.0 : kurang.getValue();
			kegiatan.setPengurangan(totalpengurangan);
			kegiatan.setKeterangan(keterangan.getValue().trim());
			kegiatan.setAmount(nilaiBiayaHarusDiBayars);

			validator.setValue(kegiatan.getValidator());
			keterangan.setValue(kegiatan.getKeterangan() == null ? "" : kegiatan.getKeterangan());
			Common.refreshSaveOrUpdate(session, kegiatan);

			if (jumlahYangAkanDibayar > 0.1) {
				LogPembayaran logPembayaran = new LogPembayaran();
				logPembayaran.setKegiatan(kegiatan);
				logPembayaran.setNominal(jumlahYangAkanDibayar);
				logPembayaran.setKeterangan("Pembayaran manual");
				logPembayaran.setValidator(tbmuser == null ? null : tbmuser.getUserNama());
				Common.refreshSaveOrUpdate(session, logPembayaran);
			}

			if (gridCicilan != null && kegiatan.getId() != null) {
				Double check = 0.0;
				List<Row> mycicilanrows = (gridCicilan == null || gridCicilan.getRows() == null)
				? new java.util.ArrayList<Row>()
				: gridCicilan.getRows().getChildren();

				for (Row row : mycicilanrows) {
					MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
					check += Math.abs(jumlahCicilan.getValue() == null ? 0.0 : jumlahCicilan.getValue());
				}

				if (check >= 1.0) {
					int i = 1;
					for (Row row : mycicilanrows) {
						MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
						MyDatebox tanggal = (MyDatebox) row.getAttribute("tanggal");
						MyDatebox tanggalKwitansi = (MyDatebox) row.getAttribute("tanggalKwitansi");
						Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
						Combobox myJenisPembayaran = (Combobox) row.getAttribute("caraBayar");
						Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
								&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan")
										: null);

						if (jumlahCicilan.getValue() != null && jumlahCicilan.getValue().intValue() != 0) {
							CicilanPembayaran cicilanPembayaranSebelumnya = (CicilanPembayaran) row
									.getAttribute("cicilanPembayaran");
							Long idLampiran = (Long) row.getAttribute("idLampiran");
							BuktiPembayaran buktiPembayaran = (BuktiPembayaran) row.getAttribute("buktiPembayaran");

							JenisPembayaran jenisPembayaran = (JenisPembayaran) (myJenisPembayaran
									.getSelectedItem() == null ? ConstantValues.TUNAI
											: myJenisPembayaran.getSelectedItem().getValue());
							if (tabungan != null)
								jenisPembayaran = tabungan;

							String val = cicilanPembayaranSebelumnya == null ? null
									: cicilanPembayaranSebelumnya.getValidator();
							if (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null"))
								val = (tbmuser == null ? "" : tbmuser.toString());

							Object jenisBiaya = myItemBiaya.getSelectedItem() == null ? null
									: myItemBiaya.getSelectedItem().getValue();
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaranSebelumnya != null
									? cicilanPembayaranSebelumnya.getPengaturanPembayaranBulanan()
									: null;
							ItemBiaya itemBiaya = cicilanPembayaranSebelumnya != null
									? cicilanPembayaranSebelumnya.getItemBiaya()
									: null;
							DetailBiaya detailBiaya = null;

							if (jenisBiaya != null && jenisBiaya instanceof PengaturanPembayaranBulanan) {
								pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) jenisBiaya;
								detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
								itemBiaya = detailBiaya.getItemBiaya();
							} else if (jenisBiaya != null && jenisBiaya instanceof DetailBiaya) {
								detailBiaya = (DetailBiaya) jenisBiaya;
								itemBiaya = detailBiaya.getItemBiaya();
							}

							try {
								CicilanPembayaran cicilanPembayaran = cicilanPembayaranSebelumnya == null
										? new CicilanPembayaran(detailBiaya)
										: cicilanPembayaranSebelumnya;
								// CATATAN: idempotency adaKembarDiDb DIHAPUS. Atas permintaan, pembayaran
								// dengan nominal SAMA dengan cicilan yang sudah ada HARUS tetap bisa
								// disimpan (mis. beberapa angsuran bernominal sama). Proteksi klik-ganda
								// cepat tetap oleh flag in-flight bayarSedangDiproses; kemungkinan ganda
								// dikonfirmasi via dialog OK/Batal di onSave (lewatiGuardGanda).
								if (cicilanPembayaran.getId() == null) {
									cicilanPembayaran.setDetailBiaya(detailBiaya);
									cicilanPembayaran.setBuktiPembayaran(buktiPembayaran);
									cicilanPembayaran.setIdLampiran(cicilanPembayaranSebelumnya == null
											|| cicilanPembayaranSebelumnya.getId() == null ? null
													: cicilanPembayaranSebelumnya.getIdLampiran());
									cicilanPembayaran.setValidator(val);
									cicilanPembayaran.setTanggalKwitansi(tanggalKwitansi.getValue());
									cicilanPembayaran.setKe(i);
									cicilanPembayaran.setKegiatan(kegiatan);
									cicilanPembayaran.setKeterangan(keterangan.getValue());
									cicilanPembayaran.setItemBiaya(itemBiaya);
									cicilanPembayaran.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
									cicilanPembayaran.setNilai(jumlahCicilan.getValue());
									cicilanPembayaran.setTanggal(tanggal.getValue());
									cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
									if (tabungan != null)
										cicilanPembayaran.setDeposit(jumlahCicilan.getValue());
									cicilanPembayaran.setJenisTabungan(tabungan);
									cicilanPembayaran.setCicilanSebelumnya(cicilanPembayaranSebelumnya == null ? null
											: cicilanPembayaranSebelumnya.getId());
									cicilanPembayaran.setDenda(cicilanPembayaranSebelumnya == null
											|| cicilanPembayaranSebelumnya.getId() == null ? null
													: cicilanPembayaranSebelumnya.getDenda());

									if (pengaturanPembayaranBulanan != null) {
										Double nilaiAsli = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa,
												Integer.parseInt(semester.getValue()));
										cicilanPembayaran.setNilaiAsli(nilaiAsli);
									}

									LampiranLain lainMahasiswa = buktiPembayarans.get(idLampiran);
									if (lainMahasiswa != null)
										cicilanPembayaran.setIdLampiran(lainMahasiswa.getId());

									if (cicilanPembayaran.getId() == null)
										session.save(cicilanPembayaran);
									else
										Common.refreshUpdate(session, cicilanPembayaran);

									if (buktiPembayaran != null) {
										buktiPembayaran.setCicilanPembayaran(cicilanPembayaran);
										Common.refreshUpdate(session, buktiPembayaran);
									}

									JenisPembayaran j = (JenisPembayaran) (myJenisPembayaran.getSelectedItem() == null
											? null
											: myJenisPembayaran.getSelectedItem().getValue());
									if (j != null && cicilanPembayaran.getJenisPembayaran() != null
											&& !j.getId().equals(cicilanPembayaran.getJenisPembayaran().getId())) {
										cicilanPembayaran.setJenisPembayaran(j);
										Common.refreshUpdate(session, cicilanPembayaran);
									}
								}
								row.setValign("top");
								row.setAttribute("cicilanPembayaran", cicilanPembayaran);
								session.flush();
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}
							i++;
						}
					}
				} else {
					Common.simpanCicilanDefaultTanpaSesseion(kegiatan, nilaiBiayaHarusDiBayars, WaktuUtil.getDate(),
							keterangan.getValue(), (JenisPembayaran) (akun.getSelectedItem() == null ? null
									: akun.getSelectedItem().getValue()),
							detailBiayas);
				}
			} else {
				Common.simpanCicilanDefaultTanpaSesseion(kegiatan, nilaiBiayaHarusDiBayars, WaktuUtil.getDate(),
						keterangan.getValue(),
						(JenisPembayaran) (akun.getSelectedItem() == null ? null : akun.getSelectedItem().getValue()),
						detailBiayas);
			}

			Common.freeze(panelMencicil, true);
			pembayaranUtil.updateTunggakan(kegiatan, session);
			JenisKegiatan kegiatanDaftarUlangMahasiswaBaru = pembayaranUtil
					.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);

			if (Konfigurasi.AKTIF.equals(
					Common.getKonfigurasi("chek_tunggakan_sebelum_bayar", Konfigurasi.TIDAK_AKTIF).getNilai())) {
				List<TunggakanMahasiswa> tunggakanMahasiswas = pembayaranUtil.getTunggakanMahasiswa(
						new JenisKegiatan[] { kegiatanDaftarUlangMahasiswaBaru, jenisKegiatan },
						kegiatan.getMahasiswa(), session);
				Common.clear(rowInfoTunggakan);
				if (tunggakanMahasiswas.size() != 0)
					new TunggakanMahasiswaHelper().display(rowInfoTunggakan, tunggakanMahasiswas);
			}

			session.flush();
			Double[] d = kegiatan.hitungTotalDanDendaFromCicilan();
			Double jumlah = d[0];
			Double denda = d[1];
			kegiatan.setDenda(denda.doubleValue());
			kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));
			kegiatan.setAmount(jumlah.doubleValue());

			Common.refreshUpdate(session, kegiatan);
			session.flush();
			tx.commit();

			// Catat signature pembayaran sukses agar submit identik berikutnya (dalam
			// rentang cooldown) ditolak -> mencegah CicilanPembayaran ganda.
			lastBayarSignature = bayarSignature;
			lastBayarTime = bayarSekarang;

			if (Konfigurasi.AKTIF
					.equals(Common.getKonfigurasi("cetak_bukti_pembayaran_setelah_proses_pembayaran", Konfigurasi.AKTIF)
							.getNilai())) {
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, true);
						buktiPembayaran = null;
						onCariMahasiswa(new Event("", new MyToolbarbuttonConfig(), null));
					}
				});
			} else {
				MyMessageboxConfig.show("Alhamdulillah, pembayaran telah berhasil dilakukan dan tercatat pada sistem. Terima kasih atas pembayaran yang telah Bapak/Ibu lakukan.", "Pemberitahuan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.createDefaultTimerNoBusy(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										buktiPembayaran = null;
										onCariMahasiswa(new Event("", new MyToolbarbuttonConfig(), null));
									}
								});
							}
						});
			}
			return true;
		} catch (Exception e) {
			if (tx != null) {
				try {
					if (tx.isActive()) {
						tx.rollback();
					}
				} catch (Exception rollEx) {
					Common.tampilErrorJikaAdmin(rollEx);
				}
			}
			MyMessageboxConfig.show("Mohon maaf, proses pembayaran tidak berhasil dilakukan. Langkah yang dapat dilakukan: (1) periksa kembali kelengkapan data pembayaran; (2) ulangi proses pembayaran beberapa saat lagi; (3) apabila masih berlanjut, mohon hubungi Administrator sistem.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			Common.tampilErrorJikaAdmin(e);
			return false;
		} finally {
			// Lepas flag in-flight agar pembayaran berikutnya (mis. setelah gagal/retry)
			// tetap bisa diproses.
			bayarSedangDiproses = false;
			if (session != null) {
				try {
					session.clear();
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
				try {
					session.disconnect();
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
				try {
					session.close();
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	public boolean onSave(final Mahasiswa mahasiswa, Event event) throws Exception {
		KegiatanTemporary kegiatanTemporary = null;
		if (!validasiPembayaran(mahasiswa, true))
			return false;

		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.openSession();
			tx = session.beginTransaction();

			this.mahasiswa = mahasiswa;
			JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
					: jenisPembayaran.getSelectedItem().getValue());

			kegiatanTemporary = (KegiatanTemporary) session.createCriteria(KegiatanTemporary.class)
					.add(Restrictions.eq("jenisKegiatan", jenisKegiatan)).add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.eq("semster",
							semester.getValue().isEmpty() ? 1 : Integer.parseInt(semester.getValue())))
					.add(Restrictions.isNull("kegiatan")).setMaxResults(1).uniqueResult();

			if (kegiatanTemporary == null)
				kegiatanTemporary = new KegiatanTemporary();
			else {
				tx.rollback();
				MyMessageboxConfig.showFormat(
						"Mohon maaf, keranjang belanja Bapak/Ibu untuk jenis pembayaran \"{V1}\" pada semester {V2} sudah ada. Silakan periksa terlebih dahulu pada menu Keranjang Belanja. Bapak/Ibu perlu menyelesaikan pembayaran transaksi tersebut terlebih dahulu, atau menghapusnya terlebih dahulu.\n\nCatatan penting: Apabila Bapak/Ibu menghapus data pembayaran di keranjang belanja sebelum melakukan pembayaran, mohon pastikan untuk MENDAPATKAN KODE PEMBAYARAN ULANG yang baru dengan menekan tombol Bayar Via Bank. Mohon tidak menggunakan KODE PEMBAYARAN yang lama.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						jenisKegiatan.getNamaKegiatan(), semester.getValue());
				return false;
			}

			kegiatanTemporary.setJenisKegiatan(jenisKegiatan);
			kegiatanTemporary.setJadwalPembayaran(jadwalPembayaran);
			kegiatanTemporary.setMahasiswa(mahasiswa);
			kegiatanTemporary.setSemster(semester.getValue().isEmpty() ? 1 : Integer.parseInt(semester.getValue()));
			kegiatanTemporary.setStatusMahasiswa(statusmahasiswa);
			kegiatanTemporary
					.setTahunAkademik(labelTahunAkademik.getValue() == null ? "" : labelTahunAkademik.getValue());
			kegiatanTemporary.setTanggal(WaktuUtil.getDate());
			kegiatanTemporary.setValidated(1);
			kegiatanTemporary.setValidator(tbmuser == null ? null : tbmuser.getUserNama());
			kegiatanTemporary.setKeterangan(keterangan.getValue().trim());
			kegiatanTemporary.setAmount(nilaiBiayaHarusDiBayars);

			validator.setValue(kegiatanTemporary.getValidator());
			keterangan.setValue(kegiatanTemporary.getKeterangan() == null ? "" : kegiatanTemporary.getKeterangan());
			Common.refreshSaveOrUpdate(session, kegiatanTemporary);

			if (kegiatanTemporary.getId() != null) {
				session.createSQLQuery("delete from detail_kegiatan where kegiatan_temporary = "
						+ kegiatanTemporary.getId() + " and posting_history is null").executeUpdate();
			}

			Rows rows = (Rows) gridss.getRows();
			if (rows != null && rows.getChildren() != null) {
				List<Row> myRows = rows.getChildren();
				for (Row row : myRows) {
					if (!row.isVisible())
						continue;

					DetailBiaya detailBiaya = (DetailBiaya) row.getAttribute("myValue");
					DetailKegiatan detailKegiatanTemporary = new DetailKegiatan();
					Double biaya = detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
							: detailBiaya.getNilaiBiayaBaru();

					try {
						Component component = (Component) row.getAttribute("tag");
						if (component instanceof MyDoublebox && detailBiaya.getItemBiaya().getNilaiBisaDiubah()) {
							MyDoublebox jumlah = (MyDoublebox) component;
							biaya = jumlah.getValue() == null ? 0.0 : jumlah.getValue();
						} else if (component instanceof Label) {
							Label myLabel = (Label) component;
							biaya = Common.numberFormat.get().parse(myLabel.getValue()).doubleValue();
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}

					if (ItemBiaya.DIKALI_NILAI_MINUS.equals(detailBiaya.getItemBiaya().getPenghitungan())) {
						for (MyDoubleboxMin kurang : pengurangan) {
							DetailBiaya penguranganItemBiaya = (DetailBiaya) kurang.getAttribute("itemBiaya");
							if (penguranganItemBiaya != null
									&& penguranganItemBiaya.getId().equals(detailBiaya.getId())) {
								Double nom = kurang.getValue() == null ? 0.0 : kurang.getValue();
								detailKegiatanTemporary.setBiaya(nom);
								break;
							}
						}
					} else {
						detailKegiatanTemporary.setBiaya(biaya);
					}
					detailKegiatanTemporary.setDetailBiaya(detailBiaya);
					detailKegiatanTemporary.setKeterangan(detailBiaya.getKeterangan());
					detailKegiatanTemporary.setKegiatanTemporary(kegiatanTemporary);
					session.save(detailKegiatanTemporary);
				}
			}

			if (gridCicilan != null && kegiatanTemporary.getId() != null) {
				Double jumlahYgDibayar = 0.0;
				List<Row> mycicilanrows = (gridCicilan == null || gridCicilan.getRows() == null)
				? new java.util.ArrayList<Row>()
				: gridCicilan.getRows().getChildren();

				for (Row row : mycicilanrows) {
					CicilanPembayaran cicilanPembayaranSebelumnya = (CicilanPembayaran) row
							.getAttribute("cicilanPembayaran");
					if (cicilanPembayaranSebelumnya == null || cicilanPembayaranSebelumnya.getId() == null) {
						MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
						jumlahYgDibayar += Math.abs(jumlahCicilan.getValue() == null ? 0.0 : jumlahCicilan.getValue());
					}
				}

				if (jumlahYgDibayar >= 1.0) {
					kegiatanTemporary.setAmount(jumlahYgDibayar);
					Common.refreshUpdate(session, (kegiatanTemporary));
					session.createSQLQuery(
							"delete from cicilan_pembayaran where kegiatan_temporary = " + kegiatanTemporary.getId())
							.executeUpdate();

					int i = 1;
					for (Row row : mycicilanrows) {
						MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
						MyDatebox tanggal = (MyDatebox) row.getAttribute("tanggal");
						MyDatebox tanggalKwitansi = (MyDatebox) row.getAttribute("tanggalKwitansi");
						Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
						Combobox myJenisPembayaran = (Combobox) row.getAttribute("caraBayar");
						Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
								&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan")
										: null);

						if (jumlahCicilan.getValue() != null && jumlahCicilan.getValue().intValue() != 0) {
							CicilanPembayaran cicilanPembayaranSebelumnya = (CicilanPembayaran) row
									.getAttribute("cicilanPembayaran");
							if (cicilanPembayaranSebelumnya == null || cicilanPembayaranSebelumnya.getId() == null) {
								Long idLampiran = (Long) row.getAttribute("idLampiran");
								BuktiPembayaran buktiPembayaran = (BuktiPembayaran) row.getAttribute("buktiPembayaran");

								String val = cicilanPembayaranSebelumnya == null ? null
										: cicilanPembayaranSebelumnya.getValidator();
								if (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null"))
									val = (tbmuser == null ? "" : tbmuser.toString());

								Object jenisBiaya = myItemBiaya.getSelectedItem() == null ? null
										: myItemBiaya.getSelectedItem().getValue();
								PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaranSebelumnya != null
										? cicilanPembayaranSebelumnya.getPengaturanPembayaranBulanan()
										: null;

								ItemBiaya itemBiaya = cicilanPembayaranSebelumnya != null
										? cicilanPembayaranSebelumnya.getItemBiaya()
										: null;
								DetailBiaya detailBiaya = null;
								if (jenisBiaya != null && jenisBiaya instanceof PengaturanPembayaranBulanan) {
									pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) jenisBiaya;
									detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
									itemBiaya = detailBiaya.getItemBiaya();
								} else if (jenisBiaya != null && jenisBiaya instanceof DetailBiaya) {
									detailBiaya = (DetailBiaya) jenisBiaya;
									itemBiaya = detailBiaya.getItemBiaya();
								}

								CicilanPembayaran cicilanPembayaran = new CicilanPembayaran(detailBiaya);
								cicilanPembayaran.setTanggalKwitansi(tanggalKwitansi.getValue());
								cicilanPembayaran.setBuktiPembayaran(buktiPembayaran);
								cicilanPembayaran.setIdLampiran(cicilanPembayaranSebelumnya == null
										|| cicilanPembayaranSebelumnya.getId() == null ? null
												: cicilanPembayaranSebelumnya.getIdLampiran());
								cicilanPembayaran.setValidator(val);
								cicilanPembayaran.setKe(i);
								cicilanPembayaran.setKegiatanTemporary(kegiatanTemporary);
								cicilanPembayaran.setKeterangan(keterangan.getValue());
								cicilanPembayaran.setItemBiaya(itemBiaya);
								cicilanPembayaran.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
								cicilanPembayaran.setNilai(jumlahCicilan.getValue());
								cicilanPembayaran.setTanggal(tanggal.getValue());
								cicilanPembayaran.setJenisPembayaran(
										(JenisPembayaran) (myJenisPembayaran.getSelectedItem() == null
												? ConstantValues.TUNAI
												: myJenisPembayaran.getSelectedItem().getValue()));
								cicilanPembayaran.setCicilanSebelumnya(cicilanPembayaranSebelumnya == null ? null
										: cicilanPembayaranSebelumnya.getId());
								cicilanPembayaran.setDenda(cicilanPembayaranSebelumnya == null
										|| cicilanPembayaranSebelumnya.getId() == null ? null
												: cicilanPembayaranSebelumnya.getDenda());
								cicilanPembayaran.setNilaiAsli(cicilanPembayaranSebelumnya == null
										|| cicilanPembayaranSebelumnya.getId() == null ? null
												: cicilanPembayaranSebelumnya.getNilaiAsli());

								LampiranLain lainMahasiswa = buktiPembayarans.get(idLampiran);
								if (lainMahasiswa != null)
									cicilanPembayaran.setIdLampiran(lainMahasiswa.getId());

								if (cicilanPembayaran.getId() == null)
									session.save(cicilanPembayaran);
								else
									Common.refreshUpdate(session, cicilanPembayaran);
								session.flush();

								if (buktiPembayaran != null) {
									buktiPembayaran.setCicilanPembayaran(cicilanPembayaran);
									Common.refreshUpdate(session, buktiPembayaran);
								}
								i++;
							}
						}
					}
				} else {
					Common.simpanCicilanDefaultTanpaSesseion(kegiatanTemporary, nilaiBiayaHarusDiBayars,
							WaktuUtil.getDate(), keterangan.getValue(),
							(JenisPembayaran) (akun.getSelectedItem() == null ? null
									: akun.getSelectedItem().getValue()),
							detailBiayas);
				}
			} else {
				Common.simpanCicilanDefaultTanpaSesseion(kegiatanTemporary, nilaiBiayaHarusDiBayars,
						WaktuUtil.getDate(), keterangan.getValue(),
						(JenisPembayaran) (akun.getSelectedItem() == null ? null : akun.getSelectedItem().getValue()),
						detailBiayas);
			}

			Common.freeze(panelMencicil, true);
			buktiPembayaran = null;

			tx.commit();
			MyMessageboxConfig.show("Alhamdulillah, pembayaran telah berhasil dimasukkan ke keranjang pembayaran. Silakan lanjutkan proses pembayaran melalui menu Keranjang Belanja. Terima kasih.", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return true;
		} catch (Exception e) {
			if (tx != null)
				tx.rollback();
			MyMessageboxConfig.show("Mohon maaf, pembayaran tidak berhasil dimasukkan ke keranjang pembayaran. Langkah yang dapat dilakukan: (1) periksa kembali kelengkapan data pembayaran; (2) ulangi proses beberapa saat lagi; (3) apabila masih berlanjut, mohon hubungi Administrator sistem.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			Common.tampilErrorJikaAdmin(e);
			return false;
		} finally {
			if (session != null) {
				try {
					session.clear();
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
				try {
					session.disconnect();
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
				try {
					session.close();
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
		}
	}

	public void hitungJumlahBiayaSeharusnya() throws ParseException {
		nilaiBiayaHarusDiBayars = detailPembayaranMahasiswaRenderer.hitungUlang();
		if (myspaceBayar != null && (tbmuser == null || tbmuser.getMahasiswa() != null)) {
			Common.freeze(myspaceBayar, nilaiBiayaHarusDiBayars < 0.01 && nilaiBiayaHarusDiBayars > -0.01);
		}
	}

	@SuppressWarnings({ "unchecked" })
	public void listCicilan(final Kegiatan kegiatan, final boolean refresh) throws Exception {
		Common.clear(panelMencicil);
		if (panelMencicil instanceof East)
			((East) panelMencicil).setTitle("Daftar Pembayaran / Angsuran");

		Integer jumlah = 40;
		cicilanPembayarans = new ArrayList<CicilanPembayaran>();

		if (kegiatan != null && kegiatan.getId() != null)
			cicilanPembayarans = KegiatanPersistenceHelper.ambilCicilan(kegiatan, refresh);
		if (detailPembayaranMahasiswaRenderer != null)
			pengaturanPembayaranBulanans = detailPembayaranMahasiswaRenderer.ubahWarnaStatus(cicilanPembayarans);

		// PERINGATAN "item hilang dari tagihan": bila sebuah Item Biaya PERNAH dibayar
		// (ada di riwayat cicilan) untuk kegiatan ini, tapi TIDAK ADA lagi di tagihan yang
		// SEDANG tampil saat ini (mis. tagihan ter-generate ulang dari Setting Biaya yang
		// beda, contoh kasus: item beasiswa "Gratis Pol" hilang berganti item SPP reguler),
		// tampilkan keterangan mencolok/merah di panel riwayat pembayaran agar staf langsung
		// sadar sebelum melanjutkan proses entry pembayaran/pelunasan.
		if (kegiatan != null && kegiatan.getId() != null && itemBiayas != null) {
			java.util.Map<Long, String> namaItemHilang = new java.util.LinkedHashMap<Long, String>();
			java.util.Set<Long> itemBiayaIdSaatIni = new java.util.HashSet<Long>();
			for (DetailBiaya db : itemBiayas.values()) {
				if (db != null && db.getItemBiaya() != null && db.getItemBiaya().getId() != null) {
					itemBiayaIdSaatIni.add(db.getItemBiaya().getId());
				}
			}
			for (CicilanPembayaran riwayat : cicilanPembayarans) {
				if (riwayat == null || riwayat.getNilai() == null || Math.abs(riwayat.getNilai()) < 0.1
						|| riwayat.getItemBiaya() == null || riwayat.getItemBiaya().getId() == null) {
					continue;
				}
				Long idItem = riwayat.getItemBiaya().getId();
				if (!itemBiayaIdSaatIni.contains(idItem) && !namaItemHilang.containsKey(idItem)) {
					namaItemHilang.put(idItem, riwayat.getItemBiaya().getKode() + " - " + riwayat.getItemBiaya().getNama());
				}
			}
			if (!namaItemHilang.isEmpty()) {
				ais.ui.util.MyDiv peringatanItemHilang = new ais.ui.util.MyDiv();
				peringatanItemHilang.setStyle(
						"background:#fee2e2;border:1px solid #dc2626;color:#991b1b;font-weight:700;"
								+ "padding:8px 10px;margin:4px 0 8px 0;border-radius:4px;");
				StringBuilder pesanItemHilang = new StringBuilder(
						"⚠ Perhatian: item biaya berikut PERNAH dibayar/ditagih sebelumnya untuk kegiatan ini, "
								+ "tetapi TIDAK muncul lagi di tagihan yang sedang tampil saat ini. Ini bisa menandakan "
								+ "tagihan ter-generate ulang dari Setting Biaya yang berbeda (mis. beasiswa berubah "
								+ "menjadi reguler). Mohon periksa kembali Paket/Jenis Seleksi/Setting Biaya mahasiswa "
								+ "ini sebelum melanjutkan entry pembayaran:");
				for (String namaItem : namaItemHilang.values()) {
					pesanItemHilang.append("\n- ").append(namaItem);
				}
				new Label(pesanItemHilang.toString()).setParent(peringatanItemHilang);
				peringatanItemHilang.setParent(panelMencicil);
			}
		}

		// Wizard: setelah seluruh nilai dihitung & dirender, sembunyikan item yang SUDAH LUNAS
		// (Kekurangan <= 0) agar mahasiswa fokus pada yang masih perlu dibayar. Timer dipakai
		// supaya nilai Kekurangan sudah final (dibaca dari label, bukan dari status warna yang
		// kadang tak ter-set). Hanya berlaku di Wizard.
		if (modeWizardRingkas) {
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					sembunyikanItemLunasWizard();
				}
			});
		}

		if (kegiatan != null && kegiatan.getId() != null) {
			CicilanPembayaran cicilanPembayaran = cicilanPembayarans.isEmpty() ? null
					: cicilanPembayarans.get(cicilanPembayarans.size() - 1);
			if (cicilanPembayaran != null)
				Common.selectComboItem(akun, cicilanPembayaran.getJenisPembayaran());
			else
				Common.selectComboItem(akun, ConstantValues.TUNAI);
		} else {
			Common.selectComboItem(akun, ConstantValues.TUNAI);
		}

		if (akun != null && akun.getSelectedItem() == null) {
			if (JenisPembayaran.DEFAULT_JENIS_PEMBAYARAN == null)
				JenisPembayaran.reloadDefault();
			Common.selectComboItem(akun, JenisPembayaran.DEFAULT_JENIS_PEMBAYARAN);
		}

		final Row rowUtama = (panelMencicil instanceof East) ? Common.tampilanScroll(panelMencicil)
				: Common.tampilanScroll1(panelMencicil);

		if (detailPembayaranMahasiswaRenderer != null) {
			// Dasbor Analisis dipindah ke panel sendiri yang MEMBENTANG penuh di bawah
			// (kolom Analisis portal). Bila portal belum tersedia (mode lama/mobile
			// fallback), tetap dipasang di rowUtama agar tidak ada logika hilang.
			if (panelAnalisis != null) {
				Common.clear(panelAnalisis);
				Row analisisRow = Common.tampilanScroll1(panelAnalisis);
				detailPembayaranMahasiswaRenderer.pasangPanelAnalisisPembayaran(cicilanPembayarans, analisisRow);
			} else {
				MyFormRow rowAnalisisFallback = new MyFormRow();
				rowAnalisisFallback.setParent(rowUtama.getParent());
				detailPembayaranMahasiswaRenderer.pasangPanelAnalisisPembayaran(cicilanPembayarans, rowAnalisisFallback);
			}
		}

		// Alat penanganan data pembayaran GANDA: peringatan + checkbox "Tampilkan
		// data ganda" + tombol "Bersihkan Data Ganda". Hanya muncul bila terdeteksi.
		// Ditaruh di BARIS sendiri (1 kolom) agar tata letak panel tetap 1 kolom
		// (toolbar/tombol di atas, lalu peringatan ganda, lalu grid).
		MyFormRow rowDuplikat = new MyFormRow();
		rowDuplikat.setParent(rowUtama.getParent());
		ais.action.master.helper.CicilanDuplikatHelper.pasangAlatDuplikat(kegiatan, cicilanPembayarans, rowDuplikat,
				new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						listCicilan(kegiatan, true);
					}
				});

		// Deteksi PER ITEM: pembayaran lebih dari sekali (dibayar > tagihan akibat baris berulang) +
		// tombol "Terdeteksi pembayaran lebih dari sekali" → bersihkan kelebihan sampai pas tagihan.
		ais.action.master.helper.CicilanDuplikatHelper.pasangAlatPembayaranBerulang(kegiatan, cicilanPembayarans,
				rowDuplikat, new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						listCicilan(kegiatan, true);
					}
				});

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(rowUtama);
		// Mode Wizard (langkah 3): sembunyikan deretan tombol (Download/Surat Tagihan/
		// Bukti/History/Recovery) agar mahasiswa fokus mengisi nilai bayar; lalu beri
		// catatan singkat tentang angsuran (hanya bila item BOLEH diangsur).
		if (modeWizardRingkas) {
			toolbar.setSclass("wz-ringkas-hide");
			boolean bolehAngsur = jadwalPembayaran != null && jadwalPembayaran.getJenisKegiatan() != null
					&& !jadwalPembayaran.getJenisKegiatan().getTidakBolehMengangsur();
			Html catatanAngsur = new Html(bolehAngsur
					? "<div style='font-size:12px;line-height:1.5;color:#166534;background:#f0fdf4;border:1px solid #bbf7d0;"
							+ "border-radius:10px;padding:9px 12px;margin:4px 0;'>&#128178; <b>Boleh diangsur.</b> "
							+ "Isi <b>Nilai bayar</b> pada baris paling bawah sesuai jumlah yang ingin dibayar sekarang "
							+ "(boleh lebih kecil dari total = cicilan), atau bayar penuh. Lalu tekan "
							+ "<b>Pilih Cara Bayar &rarr;</b>.</div>"
					: "<div style='font-size:12px;line-height:1.5;color:#1e40af;background:#eff6ff;border:1px solid #bfdbfe;"
							+ "border-radius:10px;padding:9px 12px;margin:4px 0;'>&#128178; Isi <b>Nilai bayar</b> sesuai "
							+ "tagihan pada baris paling bawah, lalu tekan <b>Pilih Cara Bayar &rarr;</b>.</div>");
			// Catatan dipasang pada baris TERSENDIRI yang MEMBENTANG penuh (span 2),
			// bukan di sel kanan rowUtama (yang membuatnya tampak separuh + ruang kosong).
			MyFormRow rowCatatan = new MyFormRow();
			ais.ui.util.ZkCompat.setSpans(rowCatatan, "2");
			rowCatatan.setParent(rowUtama.getParent());
			catatanAngsur.setParent(rowCatatan);
		}
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(CicilanPembayaran.class, new DataCriteria() {
			@Override
			public Criteria initCriteria(boolean order) {
				try {
					// FIX bocor: pakai session TERKELOLA request ZK (ditutup OpenSessionInView/FilterJSP).
					// Criteria dikonsumsi Common.cetakData dalam event yang sama; openSession() dulu ->
					// sess (Criteria terikat) TAK PERNAH ditutup = bocor koneksi.
					return HibernateUtil.currentSession().createCriteria(CicilanPembayaran.class).add(Restrictions.eq("kegiatan", kegiatan))
							.addOrder(Order.asc("tanggal")).addOrder(Order.asc("ke"));
				} catch (Exception e) {
					return null;
				}
			}
		}, "kegiatan", "jenisPembayaran", "tanggal", "nilai", "itemBiaya", "keterangan");

		toolbar.appendChild(cetakToolbarbutton);
		cetakToolbarbutton.setVisible(!cicilanPembayarans.isEmpty());

		MyToolbarbuttonConfig pengecualian = new MyToolbarbuttonConfig("Pengecualian KRS Mahasiswa",
				"/img/svg/edit-box-line.svg");
		toolbar.appendChild(pengecualian);
		pengecualian.setVisible(tbmuser.getMahasiswa() == null && Konfigurasi.AKTIF.equals(
				Common.getKonfigurasi("tampilkan_pengecualian_krs_mahasiswa_di_pembayaran", Konfigurasi.TIDAK_AKTIF)
						.getNilai()));
		pengecualian.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				new PengecualianJadwalPengisianKRSMahasiswaHelper().display();
			}
		});

		BuktiPembayaranAction
				.ambilBukti(mahasiswa, Integer.parseInt(semester.getValue()), null,
						(JenisKegiatan) jenisPembayaran.getSelectedItem().getValue(), gridCicilan, buktiPembayaran)
				.setParent(toolbar);

		mencicil = new MyCheckboxConfig("Pembayaran dengan cara bertahap");
		mencicil.setParent(toolbar);
		mencicil.setVisible(false);
		mencicil.setChecked(cicilanPembayarans.size() > 0 || bolehmencicilBaru);

		mencicil.addEventListener("onCheck", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				gridCicilan.setVisible(mencicil.isChecked());
				hboxJenisPembayaran.setVisible(!mencicil.isChecked()
						&& Konfigurasi.AKTIF.equals(
								Common.getKonfigurasi("integrasi_modul_akuntansi", Konfigurasi.TIDAK_AKTIF).getNilai())
						&& tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
			}
		});

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				gridCicilan.setVisible(mencicil.isChecked());
				hboxJenisPembayaran.setVisible(!mencicil.isChecked()
						&& Konfigurasi.AKTIF.equals(
								Common.getKonfigurasi("integrasi_modul_akuntansi", Konfigurasi.TIDAK_AKTIF).getNilai())
						&& tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Surat Tagihan", "/img/invoice-icon_surat.png");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				CommonReportHelper.prosesSuratTagihan(mahasiswa,
						labelTahunAkademik.getValue() == null ? "" : labelTahunAkademik.getValue(),
						Integer.parseInt(semester.getValue()), jadwalPembayaran);
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Bukti Pembayaran", "/img/invoice-icon_surat.png");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, false);
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				RevisiCicilanPembayaranHelper revisiHelper = new RevisiCicilanPembayaranHelper(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								listCicilan(kegiatan, true);
							}
						});
					}
				}, kegiatan);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();
			}
		});
		button.setParent(toolbar);

		if (kegiatan != null && kegiatan.getId() != null) {
			CicilanPembayaranRecoveryHelper.createRecoveryButton(kegiatan, new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							// Panggil onCariMahasiswa agar dataTagihanData (PPB list) di-rebuild
							// sehingga PPB yang baru di-restore muncul di combobox "Item Biaya".
							// Hanya listCicilan tidak cukup karena dataTagihanData tidak ikut di-refresh.
							try {
								onCariMahasiswa(null);
							} catch (Exception ex) {
								listCicilan(kegiatan, true);
							}
						}
					});
				}
			}).setParent(toolbar);
		}

		MyFormRow myrow = new MyFormRow();
		myrow.setParent(rowUtama.getParent());

		gridCicilan.setMold("paging");
		gridCicilan.setPageSize(10000);
		// Wizard: bungkus grid cicilan dalam Div wadah SCROLL MENDATAR (HP) supaya tabel
		// (min-width 520) tak terpotong oleh sel baris induk; grid menggulir di dalam Div.
		if (modeWizardRingkas) {
			org.zkoss.zul.Div wadahCicilan = new org.zkoss.zul.Div();
			wadahCicilan.setParent(myrow);
			wadahCicilan.setWidth("100%");
			wadahCicilan.setStyle("overflow-x:auto;-webkit-overflow-scrolling:touch;");
			gridCicilan.setParent(wadahCicilan);
		} else {
			gridCicilan.setParent(myrow);
		}
		gridCicilan.setVisible(cicilanPembayarans.size() > 0);
		// HP (Wizard): konversi tabel cicilan jadi 1 kolom (kartu vertikal) via CSS — sama spt gridss.
		gridCicilan.setSclass(modeWizardRingkas && (Common.isMobile() || Common.isAsliMobile())
				? "dgrid du-nowarna wz-stack-hp" : "dgrid du-nowarna");
		// Mode Wizard: baris cicilan membentang penuh (span 2) + grid 100% agar tak
		// ada ruang kosong di kanan pada langkah 3.
		if (modeWizardRingkas) {
			ais.ui.util.ZkCompat.setSpans(myrow, "2");
			gridCicilan.setWidth("100%");
		}

		Columns columns = gridCicilan.getColumns() == null ? new Columns() : gridCicilan.getColumns();
		Common.clear(columns);
		columns.setParent(gridCicilan);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Bayar ke");
		column.setWidth("12%");
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai bayar");
		column.setWidth("15%");
		column.setAlign("right");
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel(tampilkanTanggalKwitansi ? "Tgl Byr/Tgl Kwitansi" : "Tanggal Bayar");
		column.setWidth("15%");
		column.setAlign("right");
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Item Biaya");
		column.setWidth("15%");
		column.setAlign("right");
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Cara bayar");
		column.setWidth("15%");
		column.setAlign("right");
		// Wizard: cara bayar dipilih di langkah 4 (Pilih Cara Pembayaran) → sembunyikan
		// kolom/combo cara bayar per-baris di langkah 3 agar tak membingungkan.
		column.setVisible(!modeWizardRingkas && tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getSiswa() == null);
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		rowsCicilan = gridCicilan.getRows() == null ? new Rows() : gridCicilan.getRows();
		Common.clear(rowsCicilan);
		rowsCicilan.setParent(gridCicilan);

		jumlahCicilahEventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							Double jumlah = 0.0;
							Double jumlahDibayar = 0.0;
							org.zkoss.zul.Rows gridRows = gridCicilan.getRows();
							List<Row> rows = gridRows != null ? gridRows.getChildren()
									: new java.util.ArrayList();
							for (Row row : rows) {
								MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
								if (jumlahCicilan == null) continue;
								Double nilaiCicilan;
								try {
									nilaiCicilan = jumlahCicilan.getValue();
								} catch (org.zkoss.zk.ui.WrongValueException inputBelumValid) {
									continue;
								}
								jumlah += nilaiCicilan == null ? 0.0 : nilaiCicilan;

								CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) row
										.getAttribute("cicilanPembayaran");
								if (cicilanPembayaran == null) continue;
								cicilanPembayaran.setNilai(nilaiCicilan);
								row.setValign("top");
								row.setAttribute("cicilanPembayaran", cicilanPembayaran);

								if (cicilanPembayaran.getId() == null)
									jumlahDibayar += nilaiCicilan == null ? 0.0 : nilaiCicilan;
							}

							footerTotal.setValue(Common.numberFormat.get().format(jumlah));
							footerTotalTerbilang.setValue(Common
									.kapitalAwalKata(IndonesianNumberToWords.convert(jumlah.longValue()) + " rupiah"));
							footerDibayar.setValue(Common.numberFormat.get().format(jumlahDibayar));
							footerDibayarTerbilang.setValue(Common.kapitalAwalKata(
									IndonesianNumberToWords.convert(jumlahDibayar.longValue()) + " rupiah"));

							// Mode Wizard: perbarui ringkasan "yang akan dibayar" (item + nominal).
							if (modeWizardRingkas && ringkasanItemWizard != null) {
								StringBuilder rin = new StringBuilder();
								boolean ada = false;
								for (Row r2 : rows) {
									MyDoublebox jc = (MyDoublebox) r2.getAttribute("jumlahCicilan");
									Double nilaiRingkasan = null;
									try {
										nilaiRingkasan = jc == null ? null : jc.getValue();
									} catch (org.zkoss.zk.ui.WrongValueException inputBelumValid) {
										continue;
									}
									if (nilaiRingkasan == null || nilaiRingkasan <= 0.0) {
										continue;
									}
									// HANYA yang AKAN dibayar sekarang = cicilan baru (getId()==null),
									// selaras dengan footer "Dibayar" (jumlahDibayar). Baris ber-id
									// adalah cicilan tersimpan/historis → bukan bagian pembayaran ini.
									CicilanPembayaran cp2 = (CicilanPembayaran) r2.getAttribute("cicilanPembayaran");
									if (cp2 == null || cp2.getId() != null) {
										continue;
									}
									Object icObj = r2.getAttribute("itemBiaya");
									String nm = "Item Biaya";
									if (icObj instanceof Combobox && ((Combobox) icObj).getSelectedItem() != null) {
										String lbl = ((Combobox) icObj).getSelectedItem().getLabel();
										if (lbl != null && !lbl.trim().isEmpty()) {
											nm = lbl;
										}
									}
									rin.append("<div style='display:flex;justify-content:space-between;gap:10px;"
											+ "padding:4px 0;border-bottom:1px dashed #e2e8f0;'><span style='color:#334155;'>")
											.append(escHtmlWizard(nm)).append("</span><b style='color:#0f172a;white-space:nowrap;'>Rp ")
											.append(Common.numberFormat.get().format(nilaiRingkasan)).append("</b></div>");
									ada = true;
								}
								String konten = "<div style='font-size:12px;background:#ffffff;border:1px solid #e2e8f0;"
										+ "border-radius:10px;padding:10px 12px;margin:0 0 10px 0;'>"
										+ "<div style='font-weight:800;color:#1e3a8a;margin-bottom:6px;'>Yang akan dibayar</div>"
										+ (ada ? rin.toString()
												: "<div style='color:#94a3b8;'>Belum ada nominal yang diisi pada langkah Bayar.</div>")
										+ "</div>";
								ringkasanItemWizard.setContent(konten);
							}
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
					}
				});
			}
		};

		bolehMerubahCicilan = tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null
				&& tbmuser.hakAkses().getRoleId().trim().equalsIgnoreCase(Tbmrole.ADMINISTRATOR);

		if (mahasiswaAktif != null) {
			bolehMerubahCicilan = false;
		} else {
			String admLain = Common.getKonfigurasi("admin_yang_bisa_menghapus_data_pembayaran_mahasiswa", "am")
					.getNilai();
			String[] aa = admLain.split(";");
			for (String a : aa) {
				try {
					if (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null) {
						bolehMerubahCicilan = a.trim().equalsIgnoreCase(tbmuser.hakAkses().getRoleId());
						if (bolehMerubahCicilan)
							break;
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (!bolehMerubahCicilan) {
				admLain = Common.getKonfigurasi("admin_lain_bisa_menghapus_pembayaran_mahasiswa", "").getNilai();
				aa = admLain.split(";");
				for (String a : aa) {
					try {
						bolehMerubahCicilan = a.trim().equalsIgnoreCase(tbmuser.getUserId());
						if (bolehMerubahCicilan)
							break;
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}
		}

		List<DetailBiaya> yangSudahDibayar = new ArrayList<DetailBiaya>();
		if (countPengaturanBulanan > 0) {
			List<Integer> bulans = new ArrayList<Integer>();
			for (final PengaturanPembayaranBulanan pengaturanPembayaranBulanan : pengaturanPembayaranBulanans) {
				Double n = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa,
						Integer.parseInt(semester.getValue()));
				if (!bulans.contains(pengaturanPembayaranBulanan.getRealBulan()) && n > 0.1) {
					bulans.add(pengaturanPembayaranBulanan.getRealBulan());
					MyToolbarbuttonConfig sesuaikanBulanan = new MyToolbarbuttonConfig(
							pengaturanPembayaranBulanan.getNamaBulan(), "/img/svg/check2.svg");
					sesuaikanBulanan.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							inputSesuaiTagihanBulanan(pengaturanPembayaranBulanan.getRealBulan());
						}
					});
					toolbar.appendChild(sesuaikanBulanan);
				}
			}
		} else {
			yangSudahDibayar = updateDetalBiayaUntukDibayar();
		}

		List<Object[]> objects = new ArrayList<Object[]>();
		for (PengaturanPembayaranBulanan pengaturanPembayaranBulanan : pengaturanPembayaranBulanans) {
			Double nominalModifikasi = dataTagihan.containsKey(pengaturanPembayaranBulanan.getId())
					? dataTagihan.get(pengaturanPembayaranBulanan.getId())
					: pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa,
							Integer.parseInt(semester.getValue()));
			objects.add(new Object[] { pengaturanPembayaranBulanan, nominalModifikasi });
		}

		ArrayList<Long> yangSudahDibayarBulanans = new ArrayList<Long>();
		final ArrayList<Combobox> comboboxsItemBiaya = new ArrayList<Combobox>();
		for (int i = 0; i < jumlah; i++) {
			final MyToolbarbuttonConfig buttonHapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			CicilanPembayaran cicilanPembayaran = null;
			try {
				cicilanPembayaran = cicilanPembayarans.get(i);
			} catch (Exception e) {
				cicilanPembayaran = null;
			}

			if (cicilanPembayaran != null && cicilanPembayaran.getId() != null && cicilanPembayaran.getNilai() > 0.1) {
				sesuaikanDenganTagihan.setVisible(false);
				sesuaikanDenganTagihanBulanan.setVisible(false);
			}

			// Wizard: JANGAN buat baris untuk cicilan HISTORIS (getId()!=null) dari
			// kegiatan.ambilCicilan(). Di langkah 3 hanya boleh muncul cicilan BARU yang
			// dipilih pada langkah 2. (cicilanPembayarans tetap dimuat utuh untuk analisis,
			// total, & visibilitas grid — hanya pembuatan BARIS historis yang dilewati.)
			if (modeWizardRingkas && cicilanPembayaran != null && cicilanPembayaran.getId() != null) {
				continue;
			}

			if (cicilanPembayaran == null) {
				cicilanPembayaran = new CicilanPembayaran(null);
				cicilanPembayaran.setValidator(tbmuser.getUserId());
			}

			final MyFormRow row = new MyFormRow();
			row.setValign("top");
			// Wizard (desktop & HP): JANGAN tampilkan cicilan historis (getId()!=null).
			// Semua baris mulai tersembunyi; baris BARU (getId()==null) baru muncul saat
			// item dipilih di langkah 2 (mekanisme lama) → di langkah 3 hanya tampak yang
			// baru dipilih. Halaman DaftarUlang biasa (non-wizard): seperti semula.
			if (modeWizardRingkas) {
				row.setVisible(false);
			} else {
				row.setVisible(cicilanPembayaran.getId() != null);
			}
			final Hbox hboxLampiran = Common.initCicilan(buktiPembayarans, rowsCicilan, row, i, cicilanPembayaran,
					buttonHapus);

			final Textbox keterangan = new Textbox(cicilanPembayaran == null ? "" : cicilanPembayaran.getKeterangan());
			keterangan.setRows(2);
			final Combobox myCaraBayar = new Combobox();
			myCaraBayar.setReadonly(true);
			myCaraBayar.setAttribute("janganDisabled", true);
			final MyDoublebox jumlahCicilan = new MyDoublebox(
					cicilanPembayaran == null ? 0.0 : cicilanPembayaran.getNilai());
			jumlahCicilan.setWidth("90%");

			boolean tidakBolehUbah = cicilanPembayaran.getId() != null
					|| (tbmuser != null
							&& (tbmuser.getBiodataCalonMahasiswa() != null || tbmuser.getMahasiswa() != null)
							&& cicilanPembayaran.getItemBiaya() != null
							&& !cicilanPembayaran.getItemBiaya().getMahasiswaBolehMencicilkan())
					|| (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getMahasiswa() == null
							&& cicilanPembayaran.getItemBiaya() != null
							&& !cicilanPembayaran.getItemBiaya().getAdminBolehMencicilkan());

			if (tidakBolehUbah) {
				if (cicilanPembayaran != null && cicilanPembayaran.getDenda() > 0.1) {
					Vbox vbox = new Vbox();
					row.appendChild(vbox);
					vbox.appendChild(new Label(Common.numberFormat.get()
							.format(cicilanPembayaran == null ? 0.0 : cicilanPembayaran.getNilai())));
					vbox.appendChild(new Label("Denda:" + Common.numberFormat.get()
							.format(cicilanPembayaran == null ? 0.0 : cicilanPembayaran.getDenda())));
				} else {
					row.appendChild(new Label(Common.numberFormat.get()
							.format(cicilanPembayaran == null ? 0.0 : cicilanPembayaran.getNilai())));
				}
			} else {
				row.appendChild(jumlahCicilan);
			}
			jumlahCicilan.addEventListener("onChange", jumlahCicilahEventListener);
			jumlahCicilan.setAttribute("jumlahCicilanEventListener", jumlahCicilahEventListener);

			final MyDatebox tanggal = new MyDatebox(
					cicilanPembayaran == null ? ais.ui.util.WaktuUtil.getDate() : cicilanPembayaran.getTanggal());
			tanggal.setFormat(Common.dateFormat31.get().toPattern());
			tanggal.setWidth("90%");
			tanggal.setDisabled(true);
			tanggal.setReadonly(false);

			final MyDatebox tanggalKwitansi = new MyDatebox(cicilanPembayaran == null ? ais.ui.util.WaktuUtil.getDate()
					: cicilanPembayaran.getTanggalKwitansi());
			tanggalKwitansi.setFormat(Common.dateFormat31.get().toPattern());
			tanggalKwitansi.setWidth("90%");
			tanggalKwitansi.setDisabled(true);
			tanggalKwitansi.setReadonly(false);

			if (tidakBolehUbah) {
				Vbox v;
				(v = RevisiHelper.createNewRevisi(CicilanPembayaran.class, cicilanPembayaran,
						Common.dateFormat.get().format(cicilanPembayaran.getTanggal()))).setParent(row);
				if (tampilkanTanggalKwitansi)
					new Label(Common.dateFormat.get().format(cicilanPembayaran.getTanggalKwitansi())).setParent(v);
			} else {
				if (tampilkanTanggalKwitansi) {
					Vbox v = new Vbox();
					v.appendChild(tanggal);
					v.appendChild(tanggalKwitansi);
					row.appendChild(v);
				} else {
					row.appendChild(tanggal);
				}
			}

			final Combobox myItemBiaya = new Combobox();
			myItemBiaya.setReadonly(true);
			comboboxsItemBiaya.add(myItemBiaya);
			myItemBiaya.setWidth("90%");

			if (tidakBolehUbah) {
				if (cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
					PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
							.getPengaturanPembayaranBulanan();
					DetailBiaya dbLabel = null;
					try { dbLabel = pengaturanPembayaranBulanan.getDetailBiaya(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/DaftarUlangMahasiswaLamaAction.java:3591");}
					String desc = pengaturanPembayaranBulanan.getKeterangan();
					if (desc.isEmpty() && dbLabel != null && dbLabel.getItemBiaya() != null) {
						try { desc = dbLabel.getItemBiaya().getNama(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/DaftarUlangMahasiswaLamaAction.java:3594");}
					}
					desc = desc + ",  " + pengaturanPembayaranBulanan.getNamaBulan()
							+ (dbLabel != null && dbLabel.getSettingBiayaDetail() != null
									&& dbLabel.getDetailSettingBiaya() != null
									&& dbLabel.getDetailSettingBiaya().getSettingBiaya() != null
									&& dbLabel.getDetailSettingBiaya().getSettingBiaya().getJumlahPembayaran() > 1
											? ", ke-" + dbLabel.getBayarKe()
											: "");
					row.appendChild(new Label(desc));
				} else {
					row.appendChild(
							new Label(cicilanPembayaran.getItemBiaya() == null ? ""
									: cicilanPembayaran.getItemBiaya().getNama() + (cicilanPembayaran.getBayarKe() > 1
											? " ke-" + cicilanPembayaran.getBayarKe()
											: "")));
				}
			} else {
				row.appendChild(myItemBiaya);
			}

			row.setValign("top");
			row.setAttribute("jumlahCicilan", jumlahCicilan);
			row.setValign("top");
			row.setAttribute("tanggal", tanggal);
			row.setValign("top");
			row.setAttribute("tanggalKwitansi", tanggalKwitansi);
			row.setValign("top");
			row.setAttribute("itemBiaya", myItemBiaya);
			row.setValign("top");
			row.setAttribute("caraBayar", myCaraBayar);
			row.setValign("top");
			row.setAttribute("keterangan", keterangan);

			if (countPengaturanBulanan > 0) {
				tanggal.addEventListener("onChange", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Comboitem selectedComboitem = myItemBiaya.getSelectedItem();
						PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) (selectedComboitem == null
								? null
								: selectedComboitem.getValue());
						if (pengaturanPembayaranBulanan != null
								&& !pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNilaiBisaDiubah()) {
							Double nom = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa,
									Integer.parseInt(semester.getValue()));
							Double denda = 0.0;
							if (ItemBiaya.DIKALI_NILAI_MINUS.equals(
									pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan())) {
								for (MyDoubleboxMin kurang : pengurangan) {
									DetailBiaya penguranganItemBiaya = (DetailBiaya) kurang.getAttribute("itemBiaya");
									if (penguranganItemBiaya != null && penguranganItemBiaya.getId()
											.equals(pengaturanPembayaranBulanan.getDetailBiaya().getId())) {
										nom = kurang.getValue() == null ? 0.0 : kurang.getValue();
										break;
									}
								}
							} else {
								JadwalPembayaran jdw = jadwalPembayaran != null
										&& jadwalPembayaran.getKhususUntukNim() != null
										&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
												? jadwalPembayaran
												: null;
								denda = pengaturanPembayaranBulanan.checkDenda(nom, tanggal.getValue(), jdw,
										jenisKegiatan) - nom;
							}

							CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) row
									.getAttribute("cicilanPembayaran");
							cicilanPembayaran.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
							cicilanPembayaran.setTanggal(tanggal.getValue());

							if (denda > 0.1) {
								cicilanPembayaran.setNilai(nom + denda);
								cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
								cicilanPembayaran.setDenda(denda);
								keterangan.setValue(cicilanPembayaran.getKeterangan());
								keterangan.setDisabled(false);
								jumlahCicilan.setValue(nom + denda);
							}
							row.setValign("top");
							row.setAttribute("cicilanPembayaran", cicilanPembayaran);
							try {
								jumlahCicilahEventListener.onEvent(null);
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}
						}
					}
				});

				if (cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
					PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
							.getPengaturanPembayaranBulanan();
					String namaItemCombo = "";
					try {
						if (pengaturanPembayaranBulanan.getDetailBiaya() != null
								&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null) {
							namaItemCombo = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama();
						} else if (cicilanPembayaran.getItemBiaya() != null) {
							namaItemCombo = cicilanPembayaran.getItemBiaya().getNama();
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
					MyComboitemConfig comboitem = new MyComboitemConfig(
							namaItemCombo + ", Bulan " + pengaturanPembayaranBulanan.getNamaBulan());
					comboitem.setDescription(cicilanPembayaran.getKeterangan());
					comboitem.setValue(pengaturanPembayaranBulanan);
					myItemBiaya.appendChild(comboitem);
					myItemBiaya.setSelectedItem(comboitem);
					myItemBiaya.setTooltiptext(comboitem.getDescription());
				} else {
					JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
							&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
									? jadwalPembayaran
									: null;
					for (Object[] obj : objects) {
						PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) obj[0];
						Double nominalModifikasi = (Double) obj[1];
						if (nominalModifikasi >= 0.1 || nominalModifikasi <= -0.1) {
							int tahapan = 0;
							if (ConstantValues.aktifkanTahapanTerhubungKeKeuangan) {
								try {
									String bln = Common.BULAN[pengaturanPembayaranBulanan.getRealBulan() - 1];
									tahapan = Common.poulateTahapan(tempHistoryStatusMahasiswa.getProgram(),
											mahasiswa.getJurusan(), Integer.parseInt(semester.getValue()),
											mahasiswa.getSemesterMulai()).get(bln);
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
							}
							Double hasilDenda = pengaturanPembayaranBulanan.checkDenda(nominalModifikasi,
									tanggal.getValue(), jdw, jenisKegiatan);

							String namaIBLoop = "";
							String kodeIBLoop = "";
							try {
								if (pengaturanPembayaranBulanan.getDetailBiaya() != null
										&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null) {
									namaIBLoop = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama();
									kodeIBLoop = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode();
								}
							} catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
							MyComboitemConfig comboitem = new MyComboitemConfig(
									namaIBLoop + ", Bulan " + pengaturanPembayaranBulanan.getNamaBulan());
							comboitem.setDescription(
									kodeIBLoop + "-" + namaIBLoop
											+ ",  " + pengaturanPembayaranBulanan.getNamaBulan() + " "
											+ ", nominal Rp. " + Common.numberFormat.get().format(nominalModifikasi)
											+ (hasilDenda.intValue() > nominalModifikasi.intValue()
													? pengaturanPembayaranBulanan.getInfoDenda()
													: "")
											+ (ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahapan > 0
													? ", tahap " + tahapan
													: ""));
							comboitem.setValue(pengaturanPembayaranBulanan);
							myItemBiaya.appendChild(comboitem);
						}
					}
				}

				EventListener itemBiayaEventListener = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Comboitem selectedComboitem = myItemBiaya.getSelectedItem();
						Object val = (selectedComboitem == null ? null : selectedComboitem.getValue());
						if (val != null || (arg0 != null && arg0.getData() != null)) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (val != null
									&& val instanceof PengaturanPembayaranBulanan) ? (PengaturanPembayaranBulanan) (val)
											: null;
							DetailBiaya detailBiaya = (val != null && val instanceof DetailBiaya) ? (DetailBiaya) (val)
									: null;

							if (arg0 != null && arg0.getData() != null
									&& (arg0.getData() instanceof PengaturanPembayaranBulanan))
								pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) arg0.getData();
							if (arg0 != null && arg0.getData() != null && (arg0.getData() instanceof DetailBiaya))
								pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) arg0.getData();

							if (pengaturanPembayaranBulanan != null) {
								Double nominalModifikasi = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa,
										Integer.parseInt(semester.getValue()));

								if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNilaiBisaDiubah()
										&& (nominalModifikasi == null || nominalModifikasi.intValue() == 0)) {
									Rows rows = (Rows) gridss.getRows();
									if (rows != null && rows.getChildren() != null) {
										List<Row> myRows = rows.getChildren();
										for (Row r : myRows) {
											detailBiaya = (DetailBiaya) r.getAttribute("myValue");
											if (detailBiaya != null && detailBiaya.getItemBiaya() != null
													&& detailBiaya.getItemBiaya().getId()
															.equals(pengaturanPembayaranBulanan.getDetailBiaya()
																	.getItemBiaya().getId())) {
												Double biaya = detailBiaya.getNilaiBiayaBaru() == null
														? detailBiaya.getNilaiBiaya()
														: detailBiaya.getNilaiBiayaBaru();
												try {
													Component component = (Component) r.getAttribute("tag");
													if (component instanceof MyDoublebox
															&& detailBiaya.getItemBiaya().getNilaiBisaDiubah()) {
														MyDoublebox jumlah = (MyDoublebox) component;
														biaya = jumlah.getValue() == null ? 0.0 : jumlah.getValue();
													} else if (component instanceof Label) {
														Label myLabel = (Label) component;
														biaya = Common.numberFormat.get().parse(myLabel.getValue())
																.doubleValue();
													}
												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
												}
												nominalModifikasi = biaya;
												break;
											}
										}
									}
								}

								JadwalPembayaran jdw = jadwalPembayaran != null
										&& jadwalPembayaran.getKhususUntukNim() != null
										&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
												? jadwalPembayaran
												: null;
								Double hasilDenda = pengaturanPembayaranBulanan.checkDenda(nominalModifikasi,
										tanggal.getValue(), jdw, jenisKegiatan);

								jumlahCicilan.setValue(hasilDenda);
								keterangan.setValue(myItemBiaya.getSelectedItem().getDescription());
								tanggalKwitansi.setDisabled(
										!(jumlahCicilan.getValue() != null && jumlahCicilan.getValue() > 0.0));
								tanggal.setDisabled(
										!(jumlahCicilan.getValue() != null && jumlahCicilan.getValue() > 0.0));
								keterangan.setDisabled(
										!(jumlahCicilan.getValue() != null && jumlahCicilan.getValue() > 0.0));
								myCaraBayar.setDisabled(
										!(jumlahCicilan.getValue() != null && jumlahCicilan.getValue() > 0.0));

								if (!tanggal.isDisabled() && tanggal.getValue() == null)
									tanggal.setValue(ais.ui.util.WaktuUtil.getDate());
								if (!tanggalKwitansi.isDisabled() && tanggalKwitansi.getValue() == null)
									tanggalKwitansi.setValue(ais.ui.util.WaktuUtil.getDate());

								if (ItemBiaya.DIKALI_NILAI_MINUS.equals(pengaturanPembayaranBulanan.getDetailBiaya()
										.getItemBiaya().getPenghitungan())) {
									for (MyDoubleboxMin kurang : pengurangan) {
										DetailBiaya penguranganItemBiaya = (DetailBiaya) kurang
												.getAttribute("itemBiaya");
										if (penguranganItemBiaya != null && penguranganItemBiaya.getId()
												.equals(pengaturanPembayaranBulanan.getDetailBiaya().getId())) {
											Double nom = kurang.getValue() == null ? 0.0 : kurang.getValue();
											jumlahCicilan.setValue(nom);
											break;
										}
									}
								}

								CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) row
										.getAttribute("cicilanPembayaran");
								cicilanPembayaran.setNilai(jumlahCicilan.getValue());
								cicilanPembayaran.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
								row.setValign("top");
								row.setAttribute("cicilanPembayaran", cicilanPembayaran);
								cicilanPembayarans.add(cicilanPembayaran);
								jumlahCicilahEventListener.onEvent(null);
								if (jumlahCicilan.getValue() != null)
									buttonHapus.setVisible(
											jumlahCicilan.getValue() > 0.01 || jumlahCicilan.getValue() < -0.01);

								for (Combobox combobox : comboboxsItemBiaya) {
									if (combobox != myItemBiaya && !combobox.isDisabled()) {
										List<MyComboitemConfig> comboitems = combobox.getChildren();
										for (MyComboitemConfig comboitem : comboitems) {
											if (comboitem.getValue() instanceof PengaturanPembayaranBulanan) {
												PengaturanPembayaranBulanan pb = (PengaturanPembayaranBulanan) comboitem
														.getValue();
												if (pb.getId().equals(pengaturanPembayaranBulanan.getId())) {
													comboitem.detach();
													break;
												}
											}
										}
									}
								}

								row.setStyle("background-color: rgba(255,255,51,0.4)");
								Common.freeze(row, true);
								Common.freeze(hboxLampiran, false);
								hboxLampiran.setVisible(true);
								myCaraBayar.setDisabled(false);
								keterangan.setDisabled(false);

								if (cicilanPembayaran.getJenisPembayaran() != null)
									Common.selectComboItem(myCaraBayar, cicilanPembayaran.getJenisPembayaran());
								if (cicilanPembayaran.getBuktiPembayaran() != null)
									myCaraBayar.setDisabled(true);

								if (tbmuser.getMahasiswa() == null) {
									tanggal.setDisabled(false);
									tanggalKwitansi.setDisabled(false);
									jumlahCicilan.setDisabled(
											(hasilDenda - nominalModifikasi) > 0.1 || !pengaturanPembayaranBulanan
													.getDetailBiaya().getItemBiaya().getNilaiBisaDiubah());
									jumlahCicilan.setReadonly(jumlahCicilan.isDisabled());
								} else {
									myCaraBayar.setDisabled(true);
								}

								try {
									boolean tidakBolehUbah = (tbmuser != null
											&& (tbmuser.getBiodataCalonMahasiswa() != null
													|| tbmuser.getMahasiswa() != null)
											&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
											&& !pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
													.getMahasiswaBolehMencicilkan())
											|| (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null
													&& tbmuser.getMahasiswa() == null
													&& pengaturanPembayaranBulanan.getDetailBiaya()
															.getItemBiaya() != null
													&& !pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
															.getAdminBolehMencicilkan());
									jumlahCicilan.disabledPaksa(tidakBolehUbah);
									jumlahCicilan.setReadonly(jumlahCicilan.isDisabled());
								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
								}

							} else if (detailBiaya != null) {
								CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) row
										.getAttribute("cicilanPembayaran");
								cicilanPembayaran.setNilai(jumlahCicilan.getValue());
								cicilanPembayaran.setDetailBiaya(detailBiaya);
								row.setValign("top");
								row.setAttribute("cicilanPembayaran", cicilanPembayaran);
								cicilanPembayarans.add(cicilanPembayaran);
								row.setStyle("background-color: rgba(255,255,51,0.4)");
							}
						}
						try {
							jumlahCicilahEventListener.onEvent(null);
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
					}
				};

				myItemBiaya.addEventListener("onChange", itemBiayaEventListener);
				myItemBiaya.setAttribute("itemBiayaEventListener", itemBiayaEventListener);
				myItemBiaya.setDisabled(cicilanPembayaran.getId() != null
						&& (cicilanPembayaran.getNilai() > 0.01 || cicilanPembayaran.getNilai() < -0.01));

				if (cicilanPembayaran.getId() != null && cicilanPembayaran.getPengaturanPembayaranBulanan() != null)
					yangSudahDibayarBulanans.add(cicilanPembayaran.getPengaturanPembayaranBulanan().getId());

				PengaturanPembayaranBulanan nil = (PengaturanPembayaranBulanan) (myItemBiaya.getSelectedItem() == null
						? null
						: myItemBiaya.getSelectedItem().getValue());
				// Rantai nil.getDetailBiaya().getItemBiaya().getNilaiBisaDiubah() rawan NPE:
				// getDetailBiaya()/getItemBiaya() bisa null, atau getNilaiBisaDiubah() (Boolean) null
				// ter-unbox pada ternary. Hitung aman -> semantik lama dipertahankan (default disable).
				boolean bisaDiubah = nil != null && nil.getDetailBiaya() != null
						&& nil.getDetailBiaya().getItemBiaya() != null
						&& Boolean.TRUE.equals(nil.getDetailBiaya().getItemBiaya().getNilaiBisaDiubah());
				jumlahCicilan.setDisabled((tbmuser != null
						&& (tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null))
						|| !bisaDiubah);

			} else {
				myItemBiaya.setDisabled(true);
				Common.insertComboItems(myItemBiaya, "nama", cicilanPembayaran.getId() == null ? yangSudahDibayar
						: new ArrayList<DetailBiaya>(itemBiayas.values()));
				if (cicilanPembayaran.getItemBiaya() != null) {
					List<Component> components = myItemBiaya.getChildren();
					for (Component c : components) {
						if (c instanceof Comboitem) {
							DetailBiaya detailBiaya = (DetailBiaya) ((Comboitem) c).getValue();
							if (detailBiaya != null && detailBiaya.getItemBiaya() != null
									&& detailBiaya.getBayarKe().equals(cicilanPembayaran.getBayarKe()) && detailBiaya
											.getItemBiaya().getId().equals(cicilanPembayaran.getItemBiaya().getId())) {
								myItemBiaya.setSelectedItem(((Comboitem) c));
								break;
							}
						}
					}
				}

				EventListener itemBiayaEventListener = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						DetailBiaya selectedItemBiaya = (DetailBiaya) myItemBiaya.getSelectedItem().getValue();
						if (selectedItemBiaya != null) {
							if (jumlahCicilan.getValue() != null)
								buttonHapus.setVisible(
										jumlahCicilan.getValue() > 0.01 || jumlahCicilan.getValue() < -0.01);
							if (buttonHapus.isVisible()) {
								CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) row
										.getAttribute("cicilanPembayaran");
								cicilanPembayaran.setItemBiaya(selectedItemBiaya.getItemBiaya());
								cicilanPembayaran.setBayarKe(selectedItemBiaya.getBayarKe());
								cicilanPembayaran.setNilai(jumlahCicilan.getValue());
								row.setValign("top");
								row.setAttribute("cicilanPembayaran", cicilanPembayaran);
								cicilanPembayarans.add(cicilanPembayaran);

								List<DetailBiaya> yangSudahDibayar = updateDetalBiayaUntukDibayar();
								for (Combobox combobox : comboboxsItemBiaya) {
									if (combobox != myItemBiaya && combobox.getSelectedItem() == null)
										Common.insertComboItems(combobox, "nama", yangSudahDibayar);
								}

								row.setStyle("background-color: rgba(255,255,51,0.4)");
								Common.freeze(row, true);
								Common.freeze(hboxLampiran, false);
								hboxLampiran.setVisible(true);
								myCaraBayar.setDisabled(false);
								keterangan.setDisabled(false);

								if (cicilanPembayaran.getJenisPembayaran() != null)
									Common.selectComboItem(myCaraBayar, cicilanPembayaran.getJenisPembayaran());
								if (cicilanPembayaran.getBuktiPembayaran() != null)
									myCaraBayar.setDisabled(true);
							} else {
								if (selectedItemBiaya != null
										&& selectedItemBiaya.getItemBiaya().getJenisPembayaran() != null)
									Common.selectComboItem(myCaraBayar,
											selectedItemBiaya.getItemBiaya().getJenisPembayaran());
							}

							try {
								boolean tidakBolehUbah = (tbmuser != null
										&& (tbmuser.getBiodataCalonMahasiswa() != null
												|| tbmuser.getMahasiswa() != null)
										&& selectedItemBiaya != null
										&& !selectedItemBiaya.getItemBiaya().getMahasiswaBolehMencicilkan())
										|| (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null
												&& tbmuser.getMahasiswa() == null && selectedItemBiaya != null
												&& !selectedItemBiaya.getItemBiaya().getAdminBolehMencicilkan());
								jumlahCicilan.disabledPaksa(tidakBolehUbah);
								jumlahCicilan.setReadonly(jumlahCicilan.isDisabled());
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}
						}
					}
				};

				myItemBiaya.addEventListener("onChange", itemBiayaEventListener);
				myItemBiaya.setAttribute("itemBiayaEventListener", itemBiayaEventListener);
				jumlahCicilan.setDisabled(tbmuser != null
						&& (tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null));
			}

			myCaraBayar.setDisabled(true);
			myCaraBayar.setWidth("90%");

			if (tidakBolehUbah) {
				try {
					Label lblCaraBayar = new Label(
							(cicilanPembayaran == null || cicilanPembayaran.getJenisPembayaran() == null
									? ConstantValues.TUNAI
									: cicilanPembayaran.getJenisPembayaran()).getNama());
					// Wizard: cara bayar dipilih di langkah 4 → sembunyikan teks/combo per-baris.
					if (modeWizardRingkas) {
						lblCaraBayar.setVisible(false);
					}
					row.appendChild(lblCaraBayar);
				} catch (Exception e) {
					row.appendChild(new Label());
				}
			} else {
				if (modeWizardRingkas) {
					myCaraBayar.setVisible(false);
				}
				row.appendChild(myCaraBayar);
			}

			Session sessionCombo = null;
			try {
				sessionCombo = HibernateUtil.openSession();
				if (cicilanPembayaran != null && cicilanPembayaran.getJenisPembayaran() == null) {
					JenisPembayaran jenisPembayaranDefault = (JenisPembayaran) sessionCombo
							.createCriteria(JenisPembayaran.class).add(Restrictions.eq("defaultPembayaran", true))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setMaxResults(1).uniqueResult();
					cicilanPembayaran.setJenisPembayaran(jenisPembayaranDefault);
				}

				Common.insertCombo(myCaraBayar, "nama", "akun", JenisPembayaran.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			} finally {
				if (sessionCombo != null) {
					try {
						sessionCombo.clear();
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
					try {
						sessionCombo.disconnect();
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
					try {
						sessionCombo.close();
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}
			}

			try {
				Common.selectComboItem(myCaraBayar,
						cicilanPembayaran == null || cicilanPembayaran.getJenisPembayaran() == null
								? ConstantValues.TUNAI
								: cicilanPembayaran.getJenisPembayaran());
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

			keterangan.setWidth("90%");
			keterangan.setDisabled(true);
			if (tidakBolehUbah)
				row.appendChild(new Label(cicilanPembayaran.getKeterangan()));
			else
				row.appendChild(keterangan);

			jumlahCicilan.setDisabled((tbmuser != null
					&& (tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null))
					|| (countPengaturanBulanan > 0 || (cicilanPembayaran != null && cicilanPembayaran.getId() != null
							&& (cicilanPembayaran.getNilai() > 0.01 || cicilanPembayaran.getNilai() < -0.01))));

			EventListener jumlahCicilanEventListener = new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					tanggal.setDisabled(!(jumlahCicilan.getValue() != null && jumlahCicilan.getValue() > 0.0));
					tanggalKwitansi.setDisabled(!(jumlahCicilan.getValue() != null && jumlahCicilan.getValue() > 0.0));
					keterangan.setDisabled(!(jumlahCicilan.getValue() != null && jumlahCicilan.getValue() > 0.0));
					myItemBiaya.setDisabled(!(jumlahCicilan.getValue() != null && jumlahCicilan.getValue() > 0.0));

					if (!tanggal.isDisabled() && tanggal.getValue() == null)
						tanggal.setValue(ais.ui.util.WaktuUtil.getDate());
					if (!tanggalKwitansi.isDisabled() && tanggalKwitansi.getValue() == null)
						tanggalKwitansi.setValue(ais.ui.util.WaktuUtil.getDate());
					if (jumlahCicilan.getValue() != null)
						buttonHapus.setVisible(jumlahCicilan.getValue() > 0.01 || jumlahCicilan.getValue() < -0.01);

					Common.freeze(hboxLampiran, false);
					hboxLampiran.setVisible(true);
				}
			};

			jumlahCicilan.addEventListener("onChange", jumlahCicilanEventListener);
			jumlahCicilan.setAttribute("jumlahCicilanEventListener", jumlahCicilahEventListener);

			final CicilanPembayaran temCicilanPembayaran = cicilanPembayaran;

			Vbox hbox = new Vbox();
			hbox.setParent(row);

			button = new MyToolbarbuttonConfig("", "/img/svg/pencil-square.svg");
			button.setParent(hbox);
			button.setVisible(delete && temCicilanPembayaran != null
					&& bolehMerubahCicilan && cicilanPembayaran != null && cicilanPembayaran.getId() != null);
			button.setAttribute("janganDisabled", true);
			button.setTooltiptext("Ubah data pembayaran");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin mengubah data pembayaran ini? Perubahan yang dilakukan akan menggantikan data pembayaran sebelumnya. Silakan tekan OK untuk melanjutkan, atau Batal untuk membatalkan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
											Center center = new Center();
											center.setParent(borderlayout);
											ais.ui.util.ZkCompat.setFlex(center, true);
											MyGrid grid = new MyGrid();
											grid.setWidth("100%");
											grid.setParent(center);
											grid.setHeight("100%");

											Columns columns = new Columns();
											columns.setParent(grid);
											MyColumnConfig column = new MyColumnConfig();
											column.setParent(columns);
											column.setWidth("30%");
											column = new MyColumnConfig();
											column.setParent(columns);

											Rows rows = new Rows();
											rows.setParent(grid);

											final MyDatebox d = new MyDatebox(temCicilanPembayaran.getTanggal());
											final MyDoublebox comboboxBayar = new MyDoublebox(
													temCicilanPembayaran.getNilai());
											final MyDoublebox comboboxDenda = new MyDoublebox(
													temCicilanPembayaran.getDenda());
											final Combobox myCaraBayar = new Combobox();
											final Combobox myItemBayar = new Combobox();
											final Textbox keterangan = new Textbox(
													temCicilanPembayaran.getKeterangan());

											EventListener eventListenerSimpan = new EventListener() {
												@Override
												public void onEvent(Event arg0) throws Exception {
													Session session = null;
													Transaction tx = null;
													try {
														session = HibernateUtil.openSession();
														tx = session.beginTransaction();
														session.refresh(temCicilanPembayaran);
														temCicilanPembayaran.setNilai(comboboxBayar.getValue());
														temCicilanPembayaran.setNilaiDiubah(comboboxBayar.getValue());
														temCicilanPembayaran.setDenda(comboboxDenda.getValue());
														temCicilanPembayaran.setTanggal(d.getValue());
														temCicilanPembayaran.setJenisPembayaran(
																(JenisPembayaran) (myCaraBayar.getSelectedItem() == null
																		? ConstantValues.TUNAI
																		: myCaraBayar.getSelectedItem().getValue()));

														DetailBiaya tempdetailBiaya = (DetailBiaya) (myItemBayar
																.getSelectedItem() == null ? null
																		: myItemBayar.getSelectedItem()
																				.getAttribute("detailBiaya"));
														PengaturanPembayaranBulanan temppengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) (myItemBayar
																.getSelectedItem() == null ? null
																		: myItemBayar.getSelectedItem().getAttribute(
																				"pengaturanPembayaranBulanan"));

														if (tempdetailBiaya != null) {
															temCicilanPembayaran.setDetailBiaya(tempdetailBiaya);
															temCicilanPembayaran
																	.setItemBiaya(tempdetailBiaya.getItemBiaya());
														}
														if (temppengaturanPembayaranBulanan != null) {
															temCicilanPembayaran.setPengaturanPembayaranBulanan(
																	temppengaturanPembayaranBulanan);
															temCicilanPembayaran.setDetailBiaya(
																	temppengaturanPembayaranBulanan.getDetailBiaya());
															temCicilanPembayaran
																	.setItemBiaya(temppengaturanPembayaranBulanan
																			.getDetailBiaya().getItemBiaya());
														}

														temCicilanPembayaran.setKeterangan(keterangan.getValue());
														Common.refreshUpdate(session, temCicilanPembayaran);
														session.flush();
														tx.commit();
													} catch (Exception e) {
														if (tx != null)
															tx.rollback();
														ais.common.Common.tampilErrorJikaAdmin(e);
													} finally {
														if (session != null) {
															try {
																session.clear();
															} catch (Exception e) {
																ais.common.Common.tampilErrorJikaAdmin(e);
															}
															try {
																session.disconnect();
															} catch (Exception e) {
																ais.common.Common.tampilErrorJikaAdmin(e);
															}
															try {
																session.close();
															} catch (Exception e) {
																ais.common.Common.tampilErrorJikaAdmin(e);
															}
														}
													}
												}
											};

											MyFormRow row = new MyFormRow();
											row.setValign("top");
											row.setParent(rows);
											row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Bayar")));
											d.setReadonly(true);
											d.setFormat(Common.dateFormat3.get().toPattern());
											d.setWidth("95%");
											row.appendChild(d);
											d.addEventListener("onChange", eventListenerSimpan);

											row = new MyFormRow();
											row.setValign("top");
											row.setParent(rows);
											row.appendChild(new Label(ais.common.Common.getBahasaConfig("Item Biaya")));
											row.appendChild(myItemBayar);

											boolean bulanan = false;
											for (Object oo : dataTagihanData)
												if (oo instanceof PengaturanPembayaranBulanan)
													bulanan = true;

											for (Object oo : dataTagihanData) {
												DetailBiaya tempdetailBiaya = null;
												PengaturanPembayaranBulanan temppengaturanPembayaranBulanan = null;
												if (oo instanceof DetailBiaya)
													tempdetailBiaya = (DetailBiaya) oo;
												else if (oo instanceof PengaturanPembayaranBulanan) {
													temppengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) oo;
													if (temppengaturanPembayaranBulanan != null)
														tempdetailBiaya = temppengaturanPembayaranBulanan
																.getDetailBiaya();
												}
												String desc = "";
												if (temppengaturanPembayaranBulanan != null) {
													Double jml = temppengaturanPembayaranBulanan.getNominal();
													desc = temppengaturanPembayaranBulanan.getKeterangan();
													desc = (desc.isEmpty()
															? (temppengaturanPembayaranBulanan.getDetailBiaya()
																	.getItemBiaya().getNama())
															: desc) + ",  "
															+ temppengaturanPembayaranBulanan.getNamaBulan() + " "
															+ ", nominal Rp. " + Common.numberFormat.get().format(jml);
												} else if (tempdetailBiaya != null) {
													desc = tempdetailBiaya.getKeterangan();
													desc = (desc.isEmpty() ? (tempdetailBiaya.getItemBiaya().getNama())
															: desc) + ", nominal Rp. "
															+ Common.numberFormat.get()
																	.format(tempdetailBiaya.getNilaiBiaya());
												}

												Comboitem comboitem = new Comboitem(desc);
												comboitem.setAttribute("detailBiaya", tempdetailBiaya);
												comboitem.setAttribute("pengaturanPembayaranBulanan",
														temppengaturanPembayaranBulanan);
												myItemBayar.appendChild(comboitem);
												if (bulanan) {
													if (temppengaturanPembayaranBulanan != null
															&& temCicilanPembayaran
																	.getPengaturanPembayaranBulanan() != null
															&& temppengaturanPembayaranBulanan.getId()
																	.equals(temCicilanPembayaran
																			.getPengaturanPembayaranBulanan().getId()))
														myItemBayar.setSelectedItem(comboitem);
												} else {
													if (tempdetailBiaya != null
															&& temCicilanPembayaran.getDetailBiaya() != null
															&& tempdetailBiaya.getId().equals(
																	temCicilanPembayaran.getDetailBiaya().getId()))
														myItemBayar.setSelectedItem(comboitem);
												}
											}
											myItemBayar.setReadonly(true);
											myItemBayar.addEventListener("onChange", eventListenerSimpan);
											myItemBayar.setWidth("95%");

											row = new MyFormRow();
											row.setValign("top");
											row.setParent(rows);
											row.appendChild(new Label(ais.common.Common.getBahasaConfig("Cara Bayar")));
											row.appendChild(myCaraBayar);
											// Wizard: cara bayar dipilih di langkah 4 → sembunyikan baris ini.
											if (modeWizardRingkas) {
												row.setVisible(false);
											}
											myCaraBayar.setReadonly(true);
											myCaraBayar.setAttribute("janganDisabled", true);

											Session sessionCombo = null;
											try {
												sessionCombo = HibernateUtil.openSession();
												Common.insertCombo(myCaraBayar, "nama", "akun", JenisPembayaran.class,
														Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true)));
											} catch (Exception e) {
												ais.common.Common.tampilErrorJikaAdmin(e);
											} finally {
												if (sessionCombo != null) {
													try {
														sessionCombo.clear();
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}
													try {
														sessionCombo.disconnect();
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}
													try {
														sessionCombo.close();
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}
												}
											}

											Common.selectComboItem(myCaraBayar,
													temCicilanPembayaran == null
															|| temCicilanPembayaran.getJenisPembayaran() == null
																	? ConstantValues.TUNAI
																	: temCicilanPembayaran.getJenisPembayaran());
											myCaraBayar.addEventListener("onChange", eventListenerSimpan);
											myCaraBayar.setWidth("95%");

											row = new MyFormRow();
											row.setValign("top");
											row.setParent(rows);
											row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nilai Bayar")));
											comboboxBayar.setParent(row);
											comboboxBayar.setWidth("95%");
											comboboxBayar.addEventListener("onChange", eventListenerSimpan);

											row = new MyFormRow();
											row.setValign("top");
											row.setParent(rows);
											row.appendChild(new Label(ais.common.Common.getBahasaConfig("Denda")));
											comboboxDenda.setParent(row);
											comboboxDenda.setWidth("95%");
											comboboxDenda.addEventListener("onChange", eventListenerSimpan);

											row = new MyFormRow();
											row.setValign("top");
											row.setParent(rows);
											row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
											keterangan.setParent(row);
											keterangan.setWidth("95%");
											keterangan.setRows(5);
											keterangan.addEventListener("onChange", eventListenerSimpan);

											final MyWindow window = new MyWindow("Ubah Data", "none", true);
											window.setParent(
													ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
											window.setHeight("90%");
											window.setWidth("500px");

											South south = new South();
											ais.ui.util.ZkCompat.setFlex(south, true);
											south.setParent(borderlayout);

											Toolbar toolbar = new Toolbar();
											toolbar.setParent(south);
											MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai",
													"/img/cancel.gif");
											cancel.setTooltiptext("Tutup");
											cancel.addEventListener("onClick", new EventListener() {
												@Override
												public void onEvent(Event event) throws Exception {
													window.detach();
													Common.createDefaultTimer(new EventListener() {
														@Override
														public void onEvent(Event arg0) throws Exception {
															onCariMahasiswa(
																	new Event("", new MyToolbarbuttonConfig(), null));
														}
													});
												}
											});
											cancel.setParent(toolbar);
											borderlayout.setParent(window);
											window.setVisible(true);
											window.onModal();
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}
									}
								}
							});
				}
			});

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && temCicilanPembayaran != null && temCicilanPembayaran.getPostingHistory() == null
					&& bolehMerubahCicilan && cicilanPembayaran != null && cicilanPembayaran.getId() != null);
			button.setAttribute("janganDisabled", true);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus pembayaran ini? Data yang telah dihapus tidak dapat dikembalikan lagi. Silakan tekan OK untuk melanjutkan penghapusan, atau Batal untuk membatalkan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										if (event == null || event.getData() == null) {
											return;
										}
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											Session session = null;
											Transaction tx = null;
											boolean lockTimeoutTerakhir = false;
											try {
												if (temCicilanPembayaran == null || temCicilanPembayaran.getId() == null) {
													return;
												}
												// FIX akar masalah GenericJDBCException lock-timeout saat hapus:
												// sebelumnya HANYA 1x percobaan -- begitu baris cicilanPembayaran
												// sedang terkunci proses lain (mis. posting bersamaan), langsung
												// gagal & pesan "berelasi dengan data lain" yang MENYESATKAN
												// (padahal cuma lock sementara, bukan FK violation). Retry
												// singkat dgn sesi baru tiap percobaan, KHUSUS utk lock-timeout.
												int maxRetry = 3;
												boolean berhasil = false;
												Exception exTerakhir = null;
												for (int percobaan = 1; percobaan <= maxRetry && !berhasil; percobaan++) {
													session = HibernateUtil.openSession();
													tx = session.beginTransaction();
													try {
														CicilanPembayaran cicilanDb = (CicilanPembayaran) session.get(
																CicilanPembayaran.class, temCicilanPembayaran.getId());
														if (cicilanDb != null) {
															session.delete(cicilanDb);
														}
														tx.commit();
														berhasil = true;
													} catch (Exception eDelete) {
														try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception eRb) { ais.common.ErrorAuditUtil.record(eRb, "auto-audit(empty-catch) src/ais/action/master/DaftarUlangMahasiswaLamaAction.java:hapusCicilan-rollback"); }
														try { if (session.isOpen()) session.clear(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/DaftarUlangMahasiswaLamaAction.java:hapusCicilan-clear"); }
														try { session.disconnect(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/DaftarUlangMahasiswaLamaAction.java:hapusCicilan-disconnect"); }
														try { if (session.isOpen()) session.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/DaftarUlangMahasiswaLamaAction.java:hapusCicilan-close"); }
														session = null;
														tx = null;
														String pesanLower = eDelete.getMessage() == null ? "" : eDelete.getMessage().toLowerCase();
														lockTimeoutTerakhir = pesanLower.indexOf("lock timeout") >= 0
																|| pesanLower.indexOf("55p03") >= 0
																|| pesanLower.indexOf("canceling statement due to lock") >= 0;
														exTerakhir = eDelete;
														if (lockTimeoutTerakhir && percobaan < maxRetry) {
															try { Thread.sleep(300L * percobaan); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
															continue;
														}
														throw eDelete;
													}
												}
												if (!berhasil && exTerakhir != null) {
													throw exTerakhir;
												}
											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onCariMahasiswa(new Event("", new MyToolbarbuttonConfig(), null));
												}
											});
										} catch (Exception e) {
											if (tx != null)
												try { if (tx.isActive()) tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/DaftarUlangMahasiswaLamaAction.java:4756"); }
											Common.tampilErrorJikaAdmin(e);
											if (lockTimeoutTerakhir) {
												MyMessageboxConfig.showFormat(
														"Mohon maaf, data ini sedang diproses oleh transaksi lain (terkunci sementara) sehingga belum bisa dihapus setelah beberapa kali percobaan. Rincian teknis: {V1}. Silakan coba lagi beberapa saat lagi.",
														"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
														e.getMessage());
											} else {
											MyMessageboxConfig.showFormat(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lain. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) pastikan tidak ada data lain yang masih menggunakan data ini; (2) hapus terlebih dahulu data yang berkaitan; (3) apabila masih berlanjut, mohon hubungi Administrator sistem.",
													"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
													e.getMessage());
											}
										} finally {
											if (session != null) {
												try {
													session.clear();
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
												}
												try {
													session.disconnect();
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
												}
												try {
													session.close();
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
												}
											}
										}
									}
								}
							});
				}
			});
			button.setParent(hbox);

			row.setValign("top");
			row.setAttribute("buttonHapus", buttonHapus);
			buttonHapus.setTooltiptext("Hapus Data");
			buttonHapus.setVisible(false);
			buttonHapus.setAttribute("janganDisabled", true);
			buttonHapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin membatalkan data ini? Pembatalan akan mengubah status data terkait. Silakan tekan OK untuk melanjutkan pembatalan, atau Batal untuk mengurungkan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) row
													.getAttribute("cicilanPembayaran");
											if (cicilanPembayaran != null) {
												if (cicilanPembayaran.getPengaturanPembayaranBulanan() != null
														&& cicilanPembayaran.getPengaturanPembayaranBulanan()
																.getId() != null) {
													detailPembayaranMahasiswaRenderer.bul.remove(
															cicilanPembayaran.getPengaturanPembayaranBulanan().getId());
												} else if (cicilanPembayaran.getItemBiaya() != null
														&& cicilanPembayaran.getItemBiaya().getId() != null) {
													detailPembayaranMahasiswaRenderer.det
															.remove(cicilanPembayaran.getItemBiaya().getId());
												}
											}

											cicilanPembayaran.setPengaturanPembayaranBulanan(null);
											row.setValign("top");
											row.setAttribute("cicilanPembayaran", cicilanPembayaran);
											cicilanPembayarans.remove(cicilanPembayaran);

											myCaraBayar.setSelectedItem(null);
											myItemBiaya.setSelectedItem(null);
											jumlahCicilan.setValue(0.0);
											keterangan.setValue("");

											jumlahCicilan.setDisabled(countPengaturanBulanan > 0
													|| (cicilanPembayaran != null && cicilanPembayaran.getId() != null
															&& cicilanPembayaran.getNilai() > 0.0));
											tanggal.setDisabled(false);
											tanggal.setReadonly(false);
											tanggalKwitansi.setDisabled(false);
											tanggalKwitansi.setReadonly(false);
											keterangan.setReadonly(false);
											keterangan.setDisabled(false);
											myItemBiaya.setReadonly(false);
											myItemBiaya.setDisabled(false);

											if (jumlahCicilan.getValue() != null)
												buttonHapus.setVisible(jumlahCicilan.getValue() > 0.01
														|| jumlahCicilan.getValue() < -0.01);
											row.setVisible(false);
											row.detach();
											jumlahCicilahEventListener.onEvent(null);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.showFormat(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lain. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) pastikan tidak ada data lain yang masih menggunakan data ini; (2) hapus terlebih dahulu data yang berkaitan; (3) apabila masih berlanjut, mohon hubungi Administrator sistem.",
													"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
													e.getMessage());
										}
									}
								}
							});
				}
			});
			buttonHapus.setParent(hbox);

			if (mahasiswaAktif != null)
				hbox.setVisible(false);
		}

		MyFormRow foot = new MyFormRow();
		foot.setStyle("border-bottom: 1px dashed;border-bottom-color: gray;");
		foot.setParent(rowUtama.getParent());
		Hbox hbox = new Hbox();
		hbox.setWidth("99%");
		hbox.setParent(foot);

		Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("Tambah Baru", "/img/add_item.png");
		// Wizard: sembunyikan "Tambah Baru" (baris cicilan baru dibuat otomatis dari pilihan
		// langkah 2; mahasiswa tak perlu menambah baris manual).
		/* KE-FIX NullPointerException di listCicilan: jadwalPembayaran.getJenisKegiatan() bisa
		 * null (jadwal lama / data belum lengkap), dan getTidakBolehMengangsur() sendiri bertipe
		 * Boolean sehingga "!nilai" meledak saat auto-unboxing bila isinya null. Akibatnya
		 * SELURUH panel cicilan gagal dirender dan layar daftar ulang kosong. Bila datanya tidak
		 * lengkap, perlakukan sebagai "boleh mengangsur" -- yaitu perilaku lama untuk jadwal
		 * normal -- sehingga tombol tetap tampil dan fungsinya tidak hilang. */
		boolean tidakBolehMengangsur = false;
		if (jadwalPembayaran != null && jadwalPembayaran.getJenisKegiatan() != null
				&& Boolean.TRUE.equals(jadwalPembayaran.getJenisKegiatan().getTidakBolehMengangsur())) {
			tidakBolehMengangsur = true;
		}
		toolbarbutton.setVisible(!modeWizardRingkas && !tidakBolehMengangsur);
		toolbarbutton.setParent(hbox);
		hbox.appendChild(new Space());
		hbox.appendChild(new Space());

		toolbarbutton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Rows rows = gridCicilan.getRows();
				List<Row> listRow = rows.getChildren();
				for (Row row : listRow) {
					if (!row.isVisible()) {
						row.setVisible(true);
						MyToolbarbuttonConfig buttonHapus = (MyToolbarbuttonConfig) row.getAttribute("buttonHapus");
						buttonHapus.setVisible(true);
						Clients.scrollIntoView(row);
						break;
					}
				}
				if (countPengaturanBulanan > 0) {
					inputSesuaiTagihanBulanan(null);
				}
			}
		});

		// === Ringkasan pembayaran + tombol bayar (tata letak via helper bersama, dipakai Lama & Baru) ===
		footerTotal = new MyLabelBoldAja();
		footerTotalTerbilang = new MyLabelBoldAja();
		footerDibayar = new MyLabelBoldAja();
		footerDibayarTerbilang = new MyLabelBoldAja();

		foot = new MyFormRow();
		foot.setParent(rowUtama.getParent());

		// Mode Wizard langkah 4 "Cara Bayar": panduan singkat + ringkasan item yang akan
		// dibayar + kartu ringkasan total/terbilang + tombol bayar — semua di caraBayarHost.
		Component ringkasanParent = (modeWizardRingkas && caraBayarHost != null) ? caraBayarHost : foot;
		if (modeWizardRingkas && caraBayarHost != null) {
			Html panduanCara = new Html(
					"<div style='font-size:12px;line-height:1.55;color:#475569;background:#eff6ff;border:1px solid #bfdbfe;"
							+ "border-radius:10px;padding:10px 12px;margin:0 0 10px 0;'>"
							+ "<b>Panduan:</b> ① periksa <b>ringkasan</b> di bawah (item &amp; total) &rarr; "
							+ "② pilih <b>cara bayar</b> (Tunai / Virtual Account / Online) &rarr; "
							+ "③ ikuti instruksi pembayaran &rarr; ④ tekan <b>Selesai</b>.</div>");
			panduanCara.setParent(caraBayarHost);

			ringkasanItemWizard = new Html("");
			ringkasanItemWizard.setParent(caraBayarHost);
		}

		Box box = pasangRingkasanBayar(ringkasanParent);
		menuBayar(box);

		// Pastikan tombol bayar berada di bagian bawah caraBayarHost (setelah ringkasan).
		if (modeWizardRingkas && caraBayarHost != null) {
			box.setParent(caraBayarHost);
		}

		jumlahCicilahEventListener.onEvent(null);

		try {
			if (mahasiswaAktif != null) {
				if (!TampilanPaymentGateway.adaPaymentGatewayYangAktif())
					Common.freeze(panelMencicil, true);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		if (!jenisKegiatan.getPenjelasanPembayaran().isEmpty()) {
			foot = new MyFormRow();
			foot.setParent(rowUtama.getParent());
			foot.appendChild(new Html(jenisKegiatan.getPenjelasanPembayaran()));
		}
	}

	/**
	 * Kontrak callback/strategi bersarang milik {@link DaftarUlangMahasiswaLamaAction}. Tipe ini memisahkan satu
	 * variasi perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DaftarUlangMahasiswaLamaAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code execute}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see DaftarUlangMahasiswaLamaAction
	 */
	private interface GatewayAction {
		void execute(Double nilaiYgAkanDibayar, Double totalPengurangan, Event originalEvent) throws Exception;
	}

	private void setupPaymentGateway(Box spaceBayar, final String gatewayName, String configKey, String ptConfigSuffix,
			Tab tab, Tabpanel tabpanel, String btnLabel, String iconPath, final Double defaultAdminFee,
			final boolean calcCicilanVirtual, final GatewayAction action) {
		boolean isActive = Konfigurasi.AKTIF
				.equals(Common.getKonfigurasi(configKey, Konfigurasi.TIDAK_AKTIF).getNilai());
		if (ptConfigSuffix != null && isActive) {
			PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();
			isActive = Konfigurasi.AKTIF.equals(
					Common.getKonfigurasi(configKey + ptConfigSuffix + pt.getId(), Konfigurasi.AKTIF).getNilai());
		}

		if (tab != null && tabpanel != null) {
			tab.setVisible(isActive);
			tabpanel.setVisible(isActive);
		}

		if (isActive) {
			final MyButtonConfig btn = iconPath != null ? new MyButtonConfig(btnLabel, iconPath)
					: new MyButtonConfig(btnLabel);

			if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
					|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";" + gatewayName + ";"))) {
				spaceBayar.appendChild(btn);
			}

			btn.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(final Event event) throws Exception {
					if (!checkKondisiSebelumbayarBaru())
						return;
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (!apakah0(true))
								return;

							Double fee = 0.0;
							if (defaultAdminFee != null) {
								try {
									String feeStr = Common.getKonfigurasi(gatewayName + "_biaya_administrasi", "0.0")
											.getNilai();
									if (feeStr != null && !feeStr.trim().isEmpty())
										fee = Double.parseDouble(feeStr);
								} catch (Exception e) {
									fee = 0.0;
								}
							}
							final Double finalFee = fee;

							if (calcCicilanVirtual && jumlahYangAkanDibayar < 0.01) {
								jumlahYangAkanDibayar = hitungJumlahYangAkanDibayarDariTampilan();
							}

							String message = "Mohon Bapak/Ibu memeriksa kembali rincian " + btn.getLabel() + " berikut:\nNama Mahasiswa : "
									+ mahasiswa.getNama() + "\nJumlah total tagihan : " + labelFooterTagihan.getValue()
									+ "\nJumlah yang akan dibayar : "
									+ Common.numberFormat.get().format(jumlahYangAkanDibayar)
									+ (finalFee > 0.1
											? "\nBiaya administrasi : " + Common.numberFormat.get().format(finalFee)
													+ "\nTotal yang akan dibayar : "
													+ Common.numberFormat.get().format(jumlahYangAkanDibayar + finalFee)
											: " ")
									+ "\nTerbilang : "
									+ IndonesianNumberToWords.convert((long) (jumlahYangAkanDibayar + finalFee));

							MyMessageboxConfig.show(message, "Pertanyaan",
									MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
									new EventListener() {
										@Override
										public void onEvent(final Event boxEvent) throws Exception {
											int i = Integer.parseInt(boxEvent.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												if (!validasiPembayaran(mahasiswa, false))
													return;
												Common.createDefaultTimer(new EventListener() {
													@Override
													public void onEvent(Event arg0) throws Exception {
														Double totalPengurangan = 0.0;
														for (MyDoubleboxMin kurang : pengurangan)
															totalPengurangan += kurang.getValue() == null ? 0.0
																	: kurang.getValue();
														Double valAkanDibayar = Common.numberFormat.get().parse(
																Common.numberFormat.get().format(jumlahYangAkanDibayar))
																.doubleValue();

														action.execute(valAkanDibayar, totalPengurangan, event);

														Common.freeze(center, true);
														Common.freeze(panelMencicil, true);
														btn.setDisabled(true);
													}
												}, "Proses pembayaran ..");
											}
										}
									});
						}
					}, "Harap tunggu", false, 1500);
				}
			});
		}
	}

	private void tampilkanJendelaVA(VirtualAccountBank va, Double biayaAdministrasi, String fileZulPrefix,
			String prefixBankLainKode) throws Exception {
		MahasiswaVirtualAccountHelper.tampilkanHasilVirtualAccount(va, mahasiswa, null, biayaAdministrasi,
				fileZulPrefix, prefixBankLainKode);
	}

	private void setupBankOnlineGateway(Box spaceBayar, String configKey, String ptConfigSuffix, String btnLabel,
			final String bankGatewayId, final String bankHostConfig, final String adminFeeConfig,
			final String popupUrlPrefix, final String prefixKodeLainConfig) {
		boolean isActive = Konfigurasi.AKTIF
				.equals(Common.getKonfigurasi(configKey, Konfigurasi.TIDAK_AKTIF).getNilai());
		if (ptConfigSuffix != null && isActive) {
			PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();
			String tenantDefault = OnlineBmtUtil.PARAM_KEY.equals(bankGatewayId)
					? Konfigurasi.TIDAK_AKTIF : Konfigurasi.AKTIF;
			isActive = Konfigurasi.AKTIF.equals(
					Common.getKonfigurasi(configKey + ptConfigSuffix + pt.getId(), tenantDefault).getNilai());
			if (isActive && OnlineBmtUtil.PARAM_KEY.equals(bankGatewayId)) {
				isActive = OnlineBmtUtil.isPerguruanTinggiReady(pt.getId());
			}
		}

		if (isActive) {
			final MyButtonConfig bayarBtn = new MyButtonConfig(btnLabel);

			if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
					|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";" + bankGatewayId + ";"))) {
				spaceBayar.appendChild(bayarBtn);
			}

			bayarBtn.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (!checkKondisiSebelumbayarBaru())
						return;

					Common.createDefaultTimer(new EventListener() {
						@SuppressWarnings({ "rawtypes", "unchecked" })
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (!apakah0(true))
								return;

							if (adaTagihanTerpilihUntukProses()) {
								Double biayaAdministrasi = 0.0;
								try {
									biayaAdministrasi = Double
											.parseDouble(Common.getKonfigurasi(adminFeeConfig, "0.0").getNilai());
								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
								}

								BankHost bankHost = pembayaranUtil
										.getBankHost(Common.getKonfigurasi(bankHostConfig, "").getNilai(), "Bank Host");

								Map param = new HashMap();
								param.put("tahunAkademik",
										labelTahunAkademik.getValue() == null ? "" : labelTahunAkademik.getValue());
								if ("smartlink".equals(bankGatewayId))
									param.put("smartlink", true);
								else if (OnlineBmtUtil.PARAM_KEY.equals(bankGatewayId))
									param.put(OnlineBmtUtil.PARAM_KEY, true);
								else if ("maja".equals(bankGatewayId))
									param.put("maja", true);
								else if ("qris".equals(bankGatewayId))
									param.put("qris", true);
								else if ("finpay".equals(bankGatewayId))
									param.put("finpay", true);
								else if ("flip".equals(bankGatewayId))
									param.put("flip", true);
								else if ("otto".equals(bankGatewayId))
									param.put("otto", true);
								else if ("briva".equals(bankGatewayId))
									param.put("briva", true);

								VirtualAccountBank va;
								try {
									va = DownloadTagihanMahasiswaBankOnline.downloadData(mahasiswa,
											Integer.parseInt(semester.getValue()), jadwalPembayaran, detailBiayas,
											gridCicilan, param, biayaAdministrasi, null, null, bankHost);
								} catch (MahasiswaVirtualAccountHelper.TagihanSudahDibayarException e) {
									MyMessageboxConfig.show(e.getMessage(), "Tagihan Sudah Dibayar",
											MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
									return;
								}

								if (param.get("jangan_notif") != null && (Boolean) param.get("jangan_notif"))
									return;

								if (va != null && va.getLink() != null && !va.getLink().isEmpty()) {
									if ("finpay".equals(bankGatewayId) || "otto".equals(bankGatewayId)) {
										ExecutionsCtrl.getCurrent().sendRedirect(va.getLink(), "_blank");
									} else {
										Clients.evalJavaScript("popupCenter({url: '" + va.getLink()
												+ "', title: 'Book', w: 1200, h: 600});");
									}
									return;
								}

								if (popupUrlPrefix != null) {
									String prefixKode = prefixKodeLainConfig != null
											? Common.getKonfigurasi(prefixKodeLainConfig, "").getNilai()
											: "";
									tampilkanJendelaVA(va, biayaAdministrasi, popupUrlPrefix, prefixKode);
								} else if (va == null) {
									MyMessageboxConfig.show("Mohon maaf, transaksi tidak berhasil dilakukan. Langkah yang dapat dilakukan: (1) periksa kembali koneksi jaringan Bapak/Ibu; (2) ulangi proses transaksi beberapa saat lagi; (3) apabila masih berlanjut, mohon hubungi Administrator sistem.", "Peringatan",
											MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								}
							} else {
								MyMessageboxConfig.show("Mohon Bapak/Ibu memasukkan nilai tagihan yang akan dibayarkan terlebih dahulu. Langkah yang dapat dilakukan: (1) tekan Pilih Semua atau centang tagihan yang akan dibayar; (2) isikan nilai pembayaran pada kolom yang tersedia; (3) lanjutkan kembali proses pembayaran.", "Peringatan",
										MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							}
						}
					}, "Proses pembayaran ..");
				}
			});
		}
	}

	private void menuBayar(Box spaceBayar) {
		if (!edit || !starat)
			return;
		if (keterangan != null)
			keterangan.setDisabled(
					tbmuser == null || tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null);

		spaceBayar.setPack("center");
		spaceBayar.setAlign("center");
		this.myspaceBayar = spaceBayar;
		// Tombol gateway pembayaran default ZK = abu-abu muda -> teks putih nyaris tak terbaca.
		// Beri kelas agar CSS memberi WARNA KONTRAS (lihat .ais-bayar-gateway-area di css_utama.css).
		spaceBayar.setSclass(("ais-bayar-gateway-area "
				+ (spaceBayar.getSclass() == null ? "" : spaceBayar.getSclass())).trim());
		final String thnAkademikVal = labelTahunAkademik.getValue() == null ? "" : labelTahunAkademik.getValue();

		setupPaymentGateway(spaceBayar, "doku", "aktifkan_pembayaran_via_doku", null, tabDoku, tabpanelDoku,
				"BAYAR VIA DOKU", "/img/msc-logo.png", null, false, new GatewayAction() {
					@Override
					public void execute(Double nilaiYgAkanDibayar, Double totalPengurangan, Event evt)
							throws Exception {
						JenisKegiatan jk = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
								: jenisPembayaran.getSelectedItem().getValue());
						DokuCommon.onSaveDoku(jumlahYangAkanDibayar, mahasiswa, null, jk, jadwalPembayaran,
								selectedSemester, thnAkademikVal, keterangan.getValue().trim(), totalPengurangan,
								nilaiBiayaHarusDiBayars,
								DokuCommon.populateDokuRequestDetail(gridCicilan, mahasiswa,
										Integer.parseInt(semester.getValue()), jadwalPembayaran),
								DokuCommon.populateDetailBiaya(gridss, pengurangan), evt);
					}
				});

		setupPaymentGateway(spaceBayar, "ipaymu", "aktifkan_pembayaran_via_ipaymu", null, tabIpaymu, tabpanelIpaymu,
				"BAYAR VIA IPaymu", "/img/logo_ipaymu.png", null, false, new GatewayAction() {
					@Override
					public void execute(Double nilaiYgAkanDibayar, Double totalPengurangan, Event evt)
							throws Exception {
						JenisKegiatan jk = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
								: jenisPembayaran.getSelectedItem().getValue());
						IpaymuCommon.onSaveIpaymu(jumlahYangAkanDibayar, mahasiswa, null, jk, jadwalPembayaran,
								selectedSemester, thnAkademikVal, keterangan.getValue().trim(), totalPengurangan,
								nilaiBiayaHarusDiBayars,
								IpaymuCommon.populateIpaymuRequestDetail(gridCicilan, mahasiswa,
										Integer.parseInt(semester.getValue()), jadwalPembayaran),
								IpaymuCommon.populateDetailBiaya(gridss, pengurangan), evt);
					}
				});

		setupPaymentGateway(spaceBayar, "faspay", "aktifkan_pembayaran_via_faspay", null, tabFaspay, tabpanelFaspay,
				"BAYAR VIA FASPAY", null, 0.0, false, new GatewayAction() {
					@Override
					public void execute(Double nilaiYgAkanDibayar, Double totalPengurangan, Event evt)
							throws Exception {
						JenisKegiatan jk = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
								: jenisPembayaran.getSelectedItem().getValue());
						FaspayCommon.onSaveFaspay(nilaiYgAkanDibayar, mahasiswa, null, jk, jadwalPembayaran,
								selectedSemester, thnAkademikVal, keterangan.getValue().trim(), totalPengurangan,
								nilaiBiayaHarusDiBayars,
								FaspayCommon.populateFaspayRequestDetail(gridCicilan, mahasiswa,
										Integer.parseInt(semester.getValue()), jadwalPembayaran),
								FaspayCommon.populateDetailBiaya(gridss, pengurangan), evt);
					}
				});

		setupPaymentGateway(spaceBayar, "jatelindo", "aktifkan_pembayaran_via_jatelindo", "_pt_", tabJatelindo,
				tabpanelJatelindo, "BAYAR VIA JATELINDO", null, 0.0, false, new GatewayAction() {
					@Override
					public void execute(Double nilaiYgAkanDibayar, Double totalPengurangan, Event evt)
							throws Exception {
						JenisKegiatan jk = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
								: jenisPembayaran.getSelectedItem().getValue());
						JatelindoCommon.onSaveJatelindo(nilaiYgAkanDibayar, mahasiswa, null, jk, jadwalPembayaran,
								selectedSemester, thnAkademikVal, keterangan.getValue().trim(), totalPengurangan,
								nilaiBiayaHarusDiBayars,
								JatelindoCommon.populateJatelindoRequestDetail(gridCicilan, mahasiswa,
										Integer.parseInt(semester.getValue()), jadwalPembayaran),
								JatelindoCommon.populateDetailBiaya(gridss, pengurangan), evt);
					}
				});

		setupPaymentGateway(spaceBayar, "cimb", "aktifkan_pembayaran_via_cimb", null, tabCimb, tabpanelCimb,
				"BAYAR VIA CIMB", null, null, false, new GatewayAction() {
					@Override
					public void execute(Double nilaiYgAkanDibayar, Double totalPengurangan, Event evt)
							throws Exception {
						JenisKegiatan jk = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
								: jenisPembayaran.getSelectedItem().getValue());
						CimbCommon.onSaveCimb(nilaiYgAkanDibayar, mahasiswa, null, jk, jadwalPembayaran,
								selectedSemester, thnAkademikVal, keterangan.getValue().trim(), totalPengurangan,
								nilaiBiayaHarusDiBayars,
								CimbCommon.populateCimbRequestDetail(gridCicilan, mahasiswa,
										Integer.parseInt(semester.getValue()), jadwalPembayaran),
								CimbCommon.populateDetailBiaya(gridss, pengurangan), evt);
					}
				});

		setupPaymentGateway(spaceBayar, "bni", "aktifkan_pembayaran_via_bni", "_pt_", tabBni, tabpanelBni,
				"BAYAR VIA BNI", null, 0.0, true, new GatewayAction() {
					@Override
					public void execute(Double nilaiYgAkanDibayar, Double totalPengurangan, Event evt)
							throws Exception {
						JenisKegiatan jk = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
								: jenisPembayaran.getSelectedItem().getValue());
						BniCommon.onSaveBni(nilaiYgAkanDibayar, mahasiswa, null, jk, jadwalPembayaran, selectedSemester,
								thnAkademikVal, keterangan.getValue().trim(), totalPengurangan, nilaiBiayaHarusDiBayars,
								BniCommon.populateBniRequestDetail(gridCicilan, mahasiswa,
										Integer.parseInt(semester.getValue()), jadwalPembayaran),
								BniCommon.populateDetailBiaya(gridss, pengurangan), true, evt,
								VirtualAccountBank.populateCicilan(gridCicilan));
					}
				});

		setupPaymentGateway(spaceBayar, "bsi_lama", "aktifkan_pembayaran_via_bsi", "_pt_", tabBsi, tabpanelBsi,
				"BAYAR VIA BSI", null, 0.0, true, new GatewayAction() {
					@Override
					public void execute(Double nilaiYgAkanDibayar, Double totalPengurangan, Event evt)
							throws Exception {
						JenisKegiatan jk = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
								: jenisPembayaran.getSelectedItem().getValue());
						BsiCommon.onSaveBsi(nilaiYgAkanDibayar, mahasiswa, null, jk, jadwalPembayaran, selectedSemester,
								thnAkademikVal, keterangan.getValue().trim(), totalPengurangan, nilaiBiayaHarusDiBayars,
								BsiCommon.populateBsiRequestDetail(gridCicilan, mahasiswa,
										Integer.parseInt(semester.getValue()), jadwalPembayaran),
								BsiCommon.populateDetailBiaya(gridss, pengurangan), true, evt,
								VirtualAccountBank.populateCicilan(gridCicilan));
					}
				});

		setupPaymentGateway(spaceBayar, "bri", "aktifkan_pembayaran_via_bri", "_pt_", tabBri, tabpanelBri,
				"BAYAR VIA BRI", null, 0.0, true, new GatewayAction() {
					@Override
					public void execute(Double nilaiYgAkanDibayar, Double totalPengurangan, Event evt)
							throws Exception {
						JenisKegiatan jk = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
								: jenisPembayaran.getSelectedItem().getValue());
						BriCommon.onSaveBri(nilaiYgAkanDibayar, mahasiswa, null, jk, jadwalPembayaran, selectedSemester,
								thnAkademikVal, keterangan.getValue().trim(), totalPengurangan, nilaiBiayaHarusDiBayars,
								BriCommon.populateBriRequestDetail(gridCicilan, mahasiswa,
										Integer.parseInt(semester.getValue()), jadwalPembayaran),
								BriCommon.populateDetailBiaya(gridss, pengurangan), true, evt);
					}
				});

		setupPaymentGateway(spaceBayar, "finpay", "aktifkan_pembayaran_via_finpay", null, tabFinpay, tabpanelFinpay,
				"BAYAR VIA FINPAY", "/img/spi-finpay.png", null, false, new GatewayAction() {
					@Override
					public void execute(Double nilaiYgAkanDibayar, Double totalPengurangan, Event evt)
							throws Exception {
						JenisKegiatan jk = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
								: jenisPembayaran.getSelectedItem().getValue());
						FinpayCommon.onSaveFinpay(jumlahYangAkanDibayar, mahasiswa, null, jk, jadwalPembayaran,
								selectedSemester, thnAkademikVal, keterangan.getValue().trim(), totalPengurangan,
								nilaiBiayaHarusDiBayars,
								FinpayCommon.populateFinpayRequestDetail(gridCicilan, mahasiswa,
										Integer.parseInt(semester.getValue()), jadwalPembayaran),
								FinpayCommon.populateDetailBiaya(gridss, pengurangan), evt);
					}
				});

		boolean isNtt = Konfigurasi.AKTIF
				.equals(Common.getKonfigurasi("aktifkan_pembayaran_via_bank_ntt", Konfigurasi.TIDAK_AKTIF).getNilai());
		if (isNtt) {
			final MyButtonConfig bayarBankNTT = new MyButtonConfig("BAYAR VIA BANK NTT");
			bayarBankNTT.setWidth("130px");
			bayarBankNTT.setHeight("55px");
			if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
					|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";ntt;")))
				spaceBayar.appendChild(bayarBankNTT);
			bayarBankNTT.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					if (!checkKondisiSebelumbayarBaru())
						return;
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (!apakah0(true))
								return;
							if (adaTagihanTerpilihUntukProses()) {
								VirtualAccountBank va = DownloadTagihanMahasiswaBankNtt.downloadData(mahasiswa,
										Integer.parseInt(semester.getValue()), jadwalPembayaran, detailBiayas,
										gridCicilan);
								tampilkanJendelaVA(va, 0.0, "/common/ntt/no_va.zul", null);
							} else
								MyMessageboxConfig.show("Mohon Bapak/Ibu memasukkan nilai tagihan yang akan dibayarkan terlebih dahulu. Langkah yang dapat dilakukan: (1) tekan Pilih Semua atau centang tagihan yang akan dibayar; (2) isikan nilai pembayaran pada kolom yang tersedia; (3) lanjutkan kembali proses pembayaran.", "Peringatan",
										MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					}, "Proses pembayaran ..");
				}
			});
		}

		PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();
		boolean isBtn = Konfigurasi.AKTIF
				.equals(Common.getKonfigurasi("aktifkan_pembayaran_via_bank_btn", Konfigurasi.TIDAK_AKTIF).getNilai())
				&& Konfigurasi.AKTIF.equals(
						Common.getKonfigurasi("aktifkan_pembayaran_via_bank_btn_pt_" + pt.getId(), Konfigurasi.AKTIF)
								.getNilai());
		if (isBtn) {
			final MyButtonConfig bayarBankBTN = new MyButtonConfig("BAYAR VIA BANK BTN");
			bayarBankBTN.setWidth("130px");
			bayarBankBTN.setHeight("55px");
			if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
					|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";btn;")))
				spaceBayar.appendChild(bayarBankBTN);
			bayarBankBTN.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					if (!checkKondisiSebelumbayarBaru())
						return;
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (!apakah0(true))
								return;
							if (adaTagihanTerpilihUntukProses()) {
								VirtualAccountBank va = DownloadTagihanMahasiswaBankBtn.downloadData(mahasiswa,
										Integer.parseInt(semester.getValue()), jadwalPembayaran, detailBiayas,
										gridCicilan);
								tampilkanJendelaVA(va, 0.0, "/common/btn/no_va.zul", null);
							} else
								MyMessageboxConfig.show("Mohon Bapak/Ibu memasukkan nilai tagihan yang akan dibayarkan terlebih dahulu. Langkah yang dapat dilakukan: (1) tekan Pilih Semua atau centang tagihan yang akan dibayar; (2) isikan nilai pembayaran pada kolom yang tersedia; (3) lanjutkan kembali proses pembayaran.", "Peringatan",
										MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					}, "Proses pembayaran ..");
				}
			});
		}

		boolean isBjb = Konfigurasi.AKTIF
				.equals(Common.getKonfigurasi("aktifkan_pembayaran_via_bank_bjb", Konfigurasi.TIDAK_AKTIF).getNilai());
		if (isBjb) {
			final MyButtonConfig bayarBankBJB = new MyButtonConfig("BAYAR VIA BANK BJB");
			bayarBankBJB.setWidth("130px");
			bayarBankBJB.setHeight("55px");
			if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
					|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";bjb;")))
				spaceBayar.appendChild(bayarBankBJB);
			bayarBankBJB.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (!checkKondisiSebelumbayarBaru())
						return;
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (!apakah0(true))
								return;
							if (adaTagihanTerpilihUntukProses()) {
								VirtualAccountBank va = DownloadTagihanMahasiswaBankBjb.downloadData(mahasiswa,
										Integer.parseInt(semester.getValue()), jadwalPembayaran, detailBiayas,
										gridCicilan);
								tampilkanJendelaVA(va, 0.0, "/common/bjb/no_va.zul", null);
							} else
								MyMessageboxConfig.show("Mohon Bapak/Ibu memasukkan nilai tagihan yang akan dibayarkan terlebih dahulu. Langkah yang dapat dilakukan: (1) tekan Pilih Semua atau centang tagihan yang akan dibayar; (2) isikan nilai pembayaran pada kolom yang tersedia; (3) lanjutkan kembali proses pembayaran.", "Peringatan",
										MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					}, "Proses pembayaran ..");
				}
			});
		}

		boolean isAltimtara = Konfigurasi.AKTIF.equals(Common
				.getKonfigurasi("aktifkan_pembayaran_via_bank_bankaltimtara", Konfigurasi.TIDAK_AKTIF).getNilai());
		if (isAltimtara) {
			final MyButtonConfig btnAltimtara = new MyButtonConfig("BAYAR VIA Bankaltimtara");
			btnAltimtara.setWidth("130px");
			btnAltimtara.setHeight("55px");
			if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
					|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";bankaltimtara;")))
				spaceBayar.appendChild(btnAltimtara);

			btnAltimtara.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (!checkKondisiSebelumbayarBaru())
						return;
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (!apakah0(true))
								return;
							if (adaTagihanTerpilihUntukProses()) {
								final MyWindow window = new MyWindow("Pilihlah Bayar Via", "none", false);
								window.setHeight("150px");
								window.setWidth("400px");
								Radiogroup radiogroup = new Radiogroup();
								radiogroup.setParent(window);
								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(radiogroup);
								Center center = new Center();
								center.setParent(borderlayout);
								ais.ui.util.ZkCompat.setFlex(center, true);
								MyGrid grid = new MyGrid();
								grid.setWidth("100%");
								grid.setParent(center);
								grid.setHeight("100%");
								Rows rows = new Rows();
								rows.setParent(grid);

								for (final String kode : new String[] { "Virtual Account", "QRIS" }) {
									MyFormRow row = new MyFormRow();
									row.setValign("top");
									row.setParent(rows);
									MyRadioConfig radio = new MyRadioConfig(kode);
									radio.setParent(row);
									radio.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											Common.createDefaultTimer(new EventListener() {
												@Override
												public void onEvent(Event arg0) throws Exception {
													Double biayaAdm = 0.0;
													try {
														biayaAdm = Double.parseDouble(Common.getKonfigurasi(
																"bankaltimtara_biaya_administrasi", "0.0").getNilai());
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}
													VirtualAccountBank va = DownloadTagihanMahasiswaBankBankaltimtara
															.downloadData(mahasiswa,
																	Integer.parseInt(semester.getValue()),
																	jadwalPembayaran, detailBiayas, gridCicilan,
																	biayaAdm, "Virtual Account".equalsIgnoreCase(kode));
													if (va != null) {
														File fileBarcode = new File(Common.ambilREAL_PATH_REPORT()
																+ "/crcode_" + va.getId() + ".png");
														BarcodeCommon.generateCRCode(va.getBarcode(), fileBarcode, 600,
																600);
														String myUrl = "/common/bankaltimtara/no_va.zul?pakaiva="
																+ va.getPakaiva() + "&va="
																+ URLEncoder.encode(
																		va.getKode() != null ? va.getKode() : "",
																		"UTF-8")
																+ "&nominal="
																+ URLEncoder.encode("Rp. " + Common.numberFormat.get()
																		.format(va.getTotal()), "UTF-8")
																+ "&biayaAdministrasi="
																+ URLEncoder.encode("Rp. "
																		+ Common.numberFormat.get().format(biayaAdm),
																		"UTF-8")
																+ "&nama="
																+ URLEncoder.encode(mahasiswa.getNama() != null
																		? mahasiswa.getNama()
																		: "", "UTF-8")
																+ "&kadalurasa="
																+ URLEncoder.encode(va.getKadaluarsaWaktu() != null
																		? Common.dateFormat.get()
																				.format(va.getKadaluarsaWaktu())
																		: "", "UTF-8")
																+ (va.getKadaluarsaBarcode() == null ? ""
																		: "&kadalurasa_barcode=" + URLEncoder.encode(
																				Common.dateFormat5.get().format(
																						va.getKadaluarsaBarcode()),
																				"UTF-8"))
																+ "&biayaTotal="
																+ URLEncoder.encode("Rp. " + Common.numberFormat.get()
																		.format(va.getTotal() + biayaAdm), "UTF-8")
																+ "&qr="
																+ URLEncoder.encode(Common.getRequestHostWithProtocol()
																		+ "/report/" + fileBarcode.getName(), "UTF-8")
																+ "&terbilang="
																+ URLEncoder.encode(
																		IndonesianNumberToWords.convert(
																				(long) (va.getTotal() + biayaAdm)),
																		"UTF-8")
																+ (va.getHtmlTemporaryData() == null
																		|| va.getHtmlTemporaryData().isEmpty()
																				? ""
																				: "&html=" + URLEncoder.encode(
																						va.getHtmlTemporaryData(),
																						"UTF-8"))
																+ "&tampilBiayaAdministrasi=" + (biayaAdm > 0.1);
														Common.displayWindow(myUrl, true, "75%");
													} else
														MyMessageboxConfig.show("Mohon maaf, transaksi tidak berhasil dilakukan. Langkah yang dapat dilakukan: (1) periksa kembali koneksi jaringan Bapak/Ibu; (2) ulangi proses transaksi beberapa saat lagi; (3) apabila masih berlanjut, mohon hubungi Administrator sistem.",
																"Peringatan", MyMessageboxConfig.OK,
																MyMessageboxConfig.EXCLAMATION);
												}
											});
											window.detach();
										}
									});
								}
								ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
								window.onModal();
							} else
								MyMessageboxConfig.show("Mohon Bapak/Ibu memasukkan nilai tagihan yang akan dibayarkan terlebih dahulu. Langkah yang dapat dilakukan: (1) tekan Pilih Semua atau centang tagihan yang akan dibayar; (2) isikan nilai pembayaran pada kolom yang tersedia; (3) lanjutkan kembali proses pembayaran.", "Peringatan",
										MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					}, "Proses pembayaran ..");
				}
			});
		}

		setupBankOnlineGateway(spaceBayar, "aktifkan_pembayaran_via_bank_online", "_pt_", "BAYAR ONLINE", "online",
				"online_bank_host_ip", "online_biaya_administrasi", "/common/online/no_va.zul",
				"prefix_kode_bank_lain_online");
		setupBankOnlineGateway(spaceBayar, "aktifkan_pembayaran_via_bank_online_2", "_pt_", "BAYAR ONLINE 2",
				"online_2", "online_2_bank_host_ip", "online_biaya_administrasi_2", "/common/online/no_va.zul",
				"prefix_kode_bank_lain_online_2");
		setupBankOnlineGateway(spaceBayar, "aktifkan_pembayaran_via_bank_online_smartlink", "_pt_", "BAYAR VIA ONLINE",
				"smartlink", "online_bank_host_ip", "online_smartlink_biaya_administrasi", "/common/online/no_va.zul",
				"prefix_kode_bank_lain_online");
		setupBankOnlineGateway(spaceBayar, Konfigurasi.ONLINE_BMT_AKTIF, "_pt_", "BAYAR VIA ONLINE BMT",
				OnlineBmtUtil.PARAM_KEY, "online_bank_host_ip", Konfigurasi.ONLINE_BMT_BIAYA_ADMINISTRASI,
				"/common/online/no_va.zul", null);
		setupBankOnlineGateway(spaceBayar, "aktifkan_pembayaran_via_bank_maja", "_pt_", "BAYAR VIA BSI", "maja",
				"maja_bank_host_ip", "maja_biaya_administrasi", "/common/online/no_va.zul",
				"prefix_kode_bank_lain_maja");
		setupBankOnlineGateway(spaceBayar, "aktifkan_pembayaran_via_bank_qris", null, "BAYAR QRIS", "qris",
				"qris_bank_host_ip", "qris_biaya_administrasi", "/common/qris/no_va.zul", null);
		setupBankOnlineGateway(spaceBayar, "aktifkan_pembayaran_via_bank_finpay", null, "BAYAR FINPAY", "finpay",
				"finpay_bank_host_ip", "finpay_biaya_administrasi", null, null);
		setupBankOnlineGateway(spaceBayar, "aktifkan_pembayaran_via_bank_flip", null, "BAYAR VIA FLIP", "flip",
				"flip_bank_host_ip", "flip_biaya_administrasi", null, null);
		setupBankOnlineGateway(spaceBayar, "aktifkan_pembayaran_via_bank_otto", null, "BAYAR OTTO", "otto",
				"otto_bank_host_ip", "otto_biaya_administrasi", null, null);
		setupBankOnlineGateway(spaceBayar, "aktifkan_pembayaran_via_bank_briva", null, "BAYAR VIA BRIVA", "briva",
				"briva_bank_host_ip", "briva_biaya_administrasi", "/common/bri/no_va.zul", null);

		final MyButtonConfig save = new MyButtonConfig("Bayar", "/img/Money-icon_kecil.png");
		if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
				|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";tunai;"))) {
			spaceBayar.appendChild(save);
		}
		save.setHeight("55px");
		save.setWidth("130px");

		String admLain = Common.getKonfigurasi("admin_lain_yang_tidak_bisa_membayar_langsung", "").getNilai();
		String[] aa = admLain.split(";");
		boolean adminLainGaBisaLgsg = false;
		for (String a : aa) {
			try {
				if (tbmuser != null) {
					adminLainGaBisaLgsg = a.trim().equalsIgnoreCase(tbmuser.getUserId());
					if (adminLainGaBisaLgsg)
						break;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		save.setVisible(Konfigurasi.AKTIF
				.equals(Common.getKonfigurasi("aktifkan_pembayaran_manual", Konfigurasi.AKTIF).getNilai())
				&& !adminLainGaBisaLgsg);
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (!checkKondisiSebelumbayarBaru())
					return;
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (!apakah0(false))
							return;
						double biayaAdministrasi = 0.0;
						try {
							String _cfgVal = Common.getKonfigurasi("manual_biaya_administrasi", "0.0").getNilai();
							if (_cfgVal != null && !_cfgVal.trim().isEmpty()) {
								biayaAdministrasi = Double.parseDouble(_cfgVal.trim());
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/DaftarUlangMahasiswaLamaAction.java:5361");
							// config kosong atau non-numerik — gunakan default 0
						}

						MyMessageboxConfig.show(
								"Mohon Bapak/Ibu memeriksa kembali rincian pembayaran berikut sebelum melanjutkan:\nNama Mahasiswa : " + mahasiswa.getNama()
										+ "\nJumlah total tagihan : " + labelFooterTagihan.getValue()
										+ "\nJumlah yang akan dibayar : "
										+ Common.numberFormat.get().format(jumlahYangAkanDibayar)
										+ (biayaAdministrasi > 0.1
												? "\nBiaya administrasi : "
														+ Common.numberFormat.get().format(biayaAdministrasi)
														+ "\nTotal yang akan dibayar : "
														+ Common.numberFormat.get()
																.format(jumlahYangAkanDibayar + biayaAdministrasi)
												: " ")
										+ "\nTerbilang : "
										+ IndonesianNumberToWords
												.convert((long) (jumlahYangAkanDibayar + biayaAdministrasi)),
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {
									@Override
									public void onEvent(final Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											if (onSave(kegiatan, mahasiswa, event, null)) {
												Common.freeze(center, true);
												Common.freeze(panelMencicil, true);
											}
										}
									}
								});
					}
				});
			}
		});

		boolean aktifkan_keranjang_pembayaran = Konfigurasi.AKTIF
				.equals(Common.getKonfigurasi("aktifkan_keranjang_pembayaran", Konfigurasi.TIDAK_AKTIF).getNilai());
		if (aktifkan_keranjang_pembayaran) {
			final MyButtonConfig keranjangPembayaran = new MyButtonConfig("KERANJANG", "/img/shop-cart-icon.png");
			keranjangPembayaran.setWidth("130px");
			keranjangPembayaran.setHeight("55px");
			if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
					|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";keranjang;"))) {
				spaceBayar.appendChild(keranjangPembayaran);
			}

			keranjangPembayaran.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (!checkKondisiSebelumbayarBaru())
						return;
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (!apakah0(true))
								return;
							onSave(mahasiswa, arg0);
							keranjangPembayaran.setDisabled(true);
							save.setDisabled(true);
						}
					});
				}
			});
		}

		MyButtonConfig saveTabungan = new MyButtonConfig("Dari Tabungan", "/img/payments-icon.png");
		if (jenisKegiatan != null && (jenisKegiatan.getNamaBankPembayaran().isEmpty()
				|| jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";tabungan;"))) {
			spaceBayar.appendChild(saveTabungan);
		}
		saveTabungan.setHeight("55px");
		saveTabungan.setVisible(tabungan > 0.1);
		saveTabungan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				// Auto-isi DARI TABUNGAN: terangsur sesuai saldo. Bila belum diisi (0) ATAU
				// melebihi saldo → isi otomatis dibatasi saldo (saldo>=total → penuh).
				double saldoTbg = tabungan == null ? 0.0 : tabungan;
				double totalDiisi = hitungJumlahYangAkanDibayarDariTampilan();
				if (saldoTbg > 0.1 && (Math.abs(totalDiisi) < 0.01 || totalDiisi > saldoTbg + 0.01)) {
					try { capSaldoIsiCicilan = saldoTbg; inputSesuaiTagihan(); } finally { capSaldoIsiCicilan = 0.0; }
				}
				if (!checkKondisiSebelumbayarBaru())
					return;
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (!apakah0(true))
							return;
						double biayaAdministrasi = 0.0;
						try {
							String _cfgVal = Common.getKonfigurasi("manual_biaya_administrasi", "0.0").getNilai();
							if (_cfgVal != null && !_cfgVal.trim().isEmpty()) {
								biayaAdministrasi = Double.parseDouble(_cfgVal.trim());
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/DaftarUlangMahasiswaLamaAction.java:5458");
							// config kosong atau non-numerik — gunakan default 0
						}

						MyMessageboxConfig.show(
								"Mohon Bapak/Ibu memeriksa kembali rincian pembayaran dari tabungan berikut sebelum melanjutkan:\nNama Mahasiswa : "
										+ mahasiswa.getNama() + "\nJumlah total tagihan : "
										+ labelFooterTagihan.getValue() + "\nJumlah yang akan dibayar : "
										+ Common.numberFormat.get().format(jumlahYangAkanDibayar)
										+ (biayaAdministrasi > 0.1
												? "\nBiaya administrasi : "
														+ Common.numberFormat.get().format(biayaAdministrasi)
														+ "\nTotal yang akan dibayar : "
														+ Common.numberFormat.get()
																.format(jumlahYangAkanDibayar + biayaAdministrasi)
												: " ")
										+ "\nTerbilang : "
										+ IndonesianNumberToWords
												.convert((long) (jumlahYangAkanDibayar + biayaAdministrasi))
										+ "\nNominal Tabungan : " + Common.numberFormat.get().format(tabungan)
										+ "\nTerbilang : " + IndonesianNumberToWords.convert(tabungan.longValue()),
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {
									@Override
									public void onEvent(final Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											Session session = null;
											List<JenisPembayaran> jenisPembayarans = new ArrayList<JenisPembayaran>();
											try {
												session = HibernateUtil.openSession();
												jenisPembayarans = ConstantValues.simpleList(
														session.createCriteria(JenisPembayaran.class)
																.add(Restrictions.or(Restrictions.isNull("aktif"),
																		Restrictions.eq("aktif", true)))
																.add(Restrictions.isNotNull("jenisTabungan")),
														JenisPembayaran.class);
											} catch (Exception e) {
												ais.common.Common.tampilErrorJikaAdmin(e);
											} finally {
												if (session != null) {
													try {
														session.clear();
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}
													try {
														session.disconnect();
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}
													try {
														session.close();
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}
												}
											}

											if (jenisPembayarans.size() == 1) {
												if (onSave(kegiatan, mahasiswa, event, jenisPembayarans.get(0))) {
													Common.freeze(DaftarUlangMahasiswaLamaAction.this.center, true);
													Common.freeze(panelMencicil, true);
												}
											} else if (!jenisPembayarans.isEmpty()) {
												final MyWindow window = new MyWindow("Pilihlah Jenis Tabungan", "none",
														false);
												window.setHeight("200px");
												window.setWidth("500px");
												Radiogroup radiogroup = new Radiogroup();
												radiogroup.setParent(window);
												Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
												borderlayout.setParent(radiogroup);
												Center center = new Center();
												center.setParent(borderlayout);
												ais.ui.util.ZkCompat.setFlex(center, true);

												MyGrid grid = new MyGrid();
												grid.setWidth("100%");
												grid.setParent(center);
												grid.setHeight("100%");
												Rows rows = new Rows();
												rows.setParent(grid);
												for (final JenisPembayaran jenisPembayaran : jenisPembayarans) {
													MyFormRow row = new MyFormRow();
													row.setValign("top");
													row.setParent(rows);
													MyRadioConfig radio = new MyRadioConfig(jenisPembayaran.getNama()
															+ " (" + (jenisPembayaran.getJenisTabungan() + ")"));
													radio.setParent(row);
													radio.addEventListener("onClick", new EventListener() {
														@Override
														public void onEvent(Event arg0) throws Exception {
															if (onSave(kegiatan, mahasiswa, event, jenisPembayaran)) {
																Common.freeze(
																		DaftarUlangMahasiswaLamaAction.this.center,
																		true);
																Common.freeze(panelMencicil, true);
															}
															window.detach();
														}
													});
												}
												ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
														.appendChild(window);
												window.onModal();
											} else {
												MyMessageboxConfig.show("Mohon maaf, jenis pembayaran melalui tabungan tidak ditemukan pada pengaturan sistem. Langkah yang dapat dilakukan: (1) pastikan jenis pembayaran tabungan telah dikonfigurasikan; (2) gunakan metode pembayaran lain yang tersedia; (3) apabila memerlukan bantuan, mohon hubungi Administrator sistem.",
														"Peringatan", MyMessageboxConfig.OK,
														MyMessageboxConfig.EXCLAMATION);
											}
										}
									}
								});
					}
				});
			}
		});

		if (jadwalPembayaran != null && (jadwalPembayaran.getStartDate().after(WaktuUtil.getDate())
				|| jadwalPembayaran.getEndDate().before(WaktuUtil.getDate()))) {
			if (!(jadwalPembayaran.getAdminBolehMembayarkanDiluarjadwal() && tbmuser != null
					&& tbmuser.getMahasiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null)) {
				spaceBayar.setVisible(false);
				spaceBayar.setHeight("0px");
				try {
					MyMessageboxConfig.show("Mohon maaf, tidak ada jadwal pembayaran yang berlaku, atau pembayaran telah terlambat, atau periode pembayaran belum dimulai. Langkah yang dapat dilakukan: (1) periksa kembali periode jadwal pembayaran yang berlaku; (2) pastikan tanggal saat ini berada dalam rentang jadwal pembayaran; (3) apabila memerlukan bantuan, mohon hubungi bagian keuangan atau Administrator sistem.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
		} else if (mahasiswaAktif != null) {
			save.setVisible(false);
			if (!TampilanPaymentGateway.adaPaymentGatewayYangAktif()) {
				spaceBayar.setVisible(false);
				spaceBayar.setHeight("0px");
			}
		}
	}

}
