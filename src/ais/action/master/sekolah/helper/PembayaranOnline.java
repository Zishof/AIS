package ais.action.master.sekolah.helper;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.East;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankBtn;
import ais.action.master.helper.virtualaccount.DownloadTagihanSiswaBankOnline;
import ais.action.master.sekolah.util.PembayaranSiswaUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.report.CommonReportHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.BarcodeCommon;
import ais.common.BniCommon;
import ais.common.BriCommon;
import ais.common.BsiCommon;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.IndonesianNumberToWords;
import ais.common.OnlineBmtUtil;
import ais.common.TunaiSiswaCommon;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.VirtualAccountBank;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.DepositSiswa;
import ais.database.model.sekolah.DiskonSiswa;
import ais.database.model.sekolah.GrupItemBiayaSekolah;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.NominalBiaya;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.PengaturanBiayaItemBiaya;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupConfig;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakBesar;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.KeuanganDashboardEnhanceUtil;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Tipe khusus untuk pembayaran online. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code int MAKS_WORKER_TAGIHAN}, {@code
 * AmbilDataSiswaBanbox siswa}, {@code AmbilDataCalonSiswaBanbox calonSiswa}, {@code MyWindow window}, {@code
 * Siswa selectedSiswa}, {@code CalonSiswa selectedCalonSiswa}, {@code MyDoublebox deposit}, {@code Component
 * east}; inisialisasi/lifecycle ({@code doAfterCompose()}, {@code setupInitialStudentVisibility()}, {@code
 * initTabAndPanel()}, {@code init()}); pembacaan/pencarian ({@code refreshClientScrollableLayout()}, {@code
 * getSiswaLokal()}, {@code getCalonSiswaLokal()}, {@code reloadRiwayatPembayaran()}, {@code
 * refreshPanelJurnalPembayaranOnline()}, {@code reloadTagihan()}); validasi/perhitungan ({@code
 * hitungJumlahWorkerTagihan()}, {@code checkUlangHarusBayar()}, {@code checkKondisiSebelumbayar()}, {@code
 * hitungTotalTagihanTerpilihUntukBayar()}, {@code hitungUlangTagihan()}); pelaporan/ekspor ({@code
 * renderPanelJurnalPembayaranOnline()}, {@code renderRiwayatPage()}, {@code renderUIHasilPembayaran()}, {@code
 * renderDasborPanel()}, {@code renderModernPaymentHistoryCharts()}); operasi domain lain ({@code
 * closeSessionAndDisconnect()}, {@code appendStyle()}, {@code applyFullSizeScrollable()}, {@code
 * applyFullSizeHidden()}, {@code bukaRevisiPembayaranSiswaDetail()}, {@code merupakanAdmin()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class PembayaranOnline extends GenericAutowireComposer {

	private static final long serialVersionUID = 7381263733011550603L;
	private static final int MAKS_WORKER_TAGIHAN = 16;

	private static int hitungJumlahWorkerTagihan(int totalData) {
		if (totalData <= 0) {
			return 1;
		}
		int berbasisCpu = Runtime.getRuntime().availableProcessors() * 2;
		int jumlahWorker = Math.min(MAKS_WORKER_TAGIHAN, berbasisCpu);
		jumlahWorker = Math.max(2, jumlahWorker);
		return Math.min(jumlahWorker, totalData);
	}

	private AmbilDataSiswaBanbox siswa;
	private AmbilDataCalonSiswaBanbox calonSiswa;
	private MyWindow window;
	private Siswa selectedSiswa = null;
	private CalonSiswa selectedCalonSiswa = null;

	private MyDoublebox deposit;
	private Component east;
	private HashMap<Long, Tagihan> tagihans = new HashMap<Long, Tagihan>();
	private HashSet<Long> tagihansPilih;
	private Combobox bulan;
	private Combobox tahun;

	private MyLabelBold totalTagihan;
	private double t;

	private MyLabelBoldAja lblSiswa;
	private MyLabelBoldAja lblCalonSiswa;

	private MyTabConfig tabBri, tabBni, tabOnline, tabBsi;
	private Tabpanel tabpanelBri, tabpanelBni, tabpanelOnline, tabpanelBsi, tabpanel2, tabpanel3;

	private Center center;
	private Component colKiri;
	// Kolom portal full-width di bawah dua kolom (kiri grid + kanan riwayat).
	// Menampung "Dasbor Riwayat Pembayaran" agar membentang penuh ke kiri.
	private Component colBawah;
	private Rows rowsDetailBiaya;
	private MyLabelBold terbilang;
	private boolean tampilSiswa = true;
	private boolean tampilCalonSiswa = true;
	private MyDatebox tanggalTransaski;
	private boolean langsungBayar = false;
	/**
	 * Mode "Wizard Siswa" (param URL {@code wizardsiswa=1}): tampil ringkas di dalam
	 * popup — sembunyikan pemilih siswa/bulan/tahun (sudah pra-isi dari param), riwayat
	 * (east), dasbor (colBawah), dan tab lain; sisakan daftar tagihan + total + tombol
	 * pembayaran (colKiri) yang dilebarkan penuh. Catatan: popup wizard lama
	 * (SiswaPembayaranWizardHelper) sudah digantikan {@link WizardPembayaranSiswaHelper}
	 * yang mandiri; mode ini dipertahankan untuk kompatibilitas URL lama.
	 */
	private boolean modeWizardSiswa = false;
	/** Mode topup-only yang dibuka dari menu Tabungan. */
	private boolean modeTopupSiswa = false;

	private List<MyCheckboxConfig> pilihan = new ArrayList<MyCheckboxConfig>();
	private MyCheckboxConfig pilihCustom;
	private MyCheckboxConfig pilihBukanTagihan;

	private Integer pilihBulan = null;
	private Integer pilihTahun = null;
	private Double tabungan = 0.0;

	private MyLabelAgakBesar labelTabungan;
	private MyCheckboxConfig sisaTabungan = new MyCheckboxConfig("Sertakan sisa tabungan / Ambil dari tabungan");
	private Vbox panelJurnalPembayaranOnline;
	private AkunPembayaranSiswa akunPembayaranPreview;

	/**
	 * Kontrak callback/strategi bersarang milik {@link PembayaranOnline}. Tipe ini memisahkan satu variasi
	 * perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PembayaranOnline} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini merupakan detail
	 * implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code execute}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see PembayaranOnline
	 */
	private interface PaymentAction {
		void execute() throws Exception;
	}

	private void closeSessionAndDisconnect(Session session) {
		if (session != null) {
			try {
				session.clear();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PembayaranOnline.java:179");
			}
			try {
				session.disconnect();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PembayaranOnline.java:183");
			}
			try {
				session.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PembayaranOnline.java:187");
			}
		}
	}

	private void appendStyle(HtmlBasedComponent component, String style) {
		if (component == null || style == null || style.trim().length() == 0) {
			return;
		}
		String current = component.getStyle();
		current = current == null ? "" : current.trim();
		if (current.length() > 0 && !current.endsWith(";")) {
			current = current + ";";
		}
		component.setStyle(current + style);
	}

	private void applyFullSizeScrollable(HtmlBasedComponent component) {
		if (component == null) {
			return;
		}
		component.setWidth("100%");
		component.setHeight("100%");
		appendStyle(component, "border:0; margin:0; padding:0; box-sizing:border-box; overflow:auto;");
	}

	private void applyFullSizeHidden(HtmlBasedComponent component) {
		if (component == null) {
			return;
		}
		component.setWidth("100%");
		component.setHeight("100%");
		appendStyle(component, "border:0; margin:0; padding:0; box-sizing:border-box; overflow:hidden;");
	}

	private void refreshClientScrollableLayout() {
		try {
			Clients.evalJavaScript("setTimeout(function(){try{"
					+ "jq('.z-tabpanel,.z-tabpanel-cnt,.z-window-embedded-cnt').css({'overflow':'auto'});"
					// Paksa SEMUA konten (dasbor, CRUD, panel) RATA ATAS: bila kontainer flex/tabel
					// sempat ter-set vertical-align/justify center, kembalikan ke flex-start/top.
					+ "jq('.z-center-body').css({'overflow-y':'auto','overflow-x':'hidden','vertical-align':'top','align-items':'flex-start','align-content':'flex-start','justify-content':'flex-start'});"
					+ "jq('.z-tabpanel,.z-tabpanel-cnt,.z-window-embedded-cnt').css({'vertical-align':'top'});"
					+ "jq('.ais-ce-portallayout').css({'align-items':'flex-start','align-content':'flex-start','justify-content':'flex-start'});"
					+ "jq('.ais-ce-portalchildren').css({'justify-content':'flex-start','align-self':'flex-start','vertical-align':'top'});"
					+ "jq('.z-east-body').css({'overflow-y':'auto','overflow-x':'hidden','height':'100%','vertical-align':'top'});"
					+ "jq('.z-grid,.z-grid-body,.z-rows,.z-row').css({'max-width':'100%'});"
					+ "jq(window).trigger('resize');"
					+ "}catch(e){}},150);");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PembayaranOnline.java:236");
		}
	}

	private Siswa getSiswaLokal() {
		return (Siswa) siswa.getAttribute("siswa");
	}

	private CalonSiswa getCalonSiswaLokal() {
		return (CalonSiswa) calonSiswa.getAttribute("calonSiswa");
	}

	/**
	 * Buka jendela audit/restore PembayaranSiswaDetail (Envers) untuk siswa/calon
	 * siswa terpilih. fokusRestore=true membuka langsung tab "Seluruh Data Revisi"
	 * yang memuat tombol Restore per-revisi dan "Restore Terbaru mulai tanggal".
	 * Setelah jendela ditutup, daftar tagihan dimuat ulang agar hasil restore
	 * langsung terlihat.
	 */
	private void bukaRevisiPembayaranSiswaDetail(boolean fokusRestore) throws Exception {
		// PENJAGA SISI-SERVER (lapis kedua selain visibilitas tombol): akun siswa,
		// calon siswa, dan orang tua TIDAK BOLEH mengakses audit/restore pembayaran.
		Tbmuser penggunaSaatIni = Common.getCurrentUser();
		if (penggunaSaatIni == null || penggunaSaatIni.getSiswa() != null
				|| penggunaSaatIni.getCalonSiswa() != null || penggunaSaatIni.getOrangTua() != null) {
			MyMessageboxConfig.show(
					"Mohon maaf, fitur History/Restore pembayaran hanya tersedia untuk petugas/administrator.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		Siswa s = getSiswaLokal();
		CalonSiswa cs = getCalonSiswaLokal();
		if (s == null && cs != null && cs.getSiswa() != null) {
			s = cs.getSiswa();
		}
		if (s == null && cs == null) {
			MyMessageboxConfig.show(
					"Siswa harus dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar siswa pada halaman ini; (2) pilih siswa yang akan diproses; (3) ulangi tindakan setelah siswa terpilih.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		RevisiPembayaranSiswaDetailHelper revisiHelper = new RevisiPembayaranSiswaDetailHelper(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event a) throws Exception {
						reloadTagihan(true);
					}
				});
			}
		}, s, cs);
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
		revisiHelper.setVisible(true);
		if (fokusRestore) {
			revisiHelper.bukaTabSeluruhData();
		}
		revisiHelper.onModal();
	}

	/**
	 * Apakah pengguna yang sedang login adalah ADMIN/PETUGAS (bukan akun Siswa
	 * maupun Calon Siswa sendiri). Dipakai untuk mengecualikan admin dari penguncian
	 * checkbox tagihan "Wajib Dipilih" — item wajib tetap TERCENTANG untuk siapa pun
	 * (aturan bisnisnya tidak berubah), namun HANYA siswa/orang tua yang tidak boleh
	 * membatalkan centangnya; admin tetap boleh menyesuaikan (mis. mengecualikan satu
	 * item wajib untuk kasus tertentu tanpa perlu masuk ke Setting Biaya).
	 * <p>
	 * Fail-closed: bila deteksi gagal/pengguna tidak diketahui, dianggap BUKAN admin
	 * sehingga checkbox tetap terkunci seperti perilaku lama.
	 */
	private boolean merupakanAdmin() {
		try {
			Tbmuser u = Common.getCurrentUser();
			if (u == null) return false;
			if (u.getSiswa() != null) return false;
			if (u.getCalonSiswa() != null) return false;
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private EventListener eventListenerData = new EventListener() {
		@Override
		public void onEvent(Event arg0) throws Exception {
			try {
				MyCheckboxConfig check = (MyCheckboxConfig) arg0.getTarget();
				Tagihan tagihan = (Tagihan) check.getAttribute("tagihan");

				if (!pilihCustom.isChecked()) {
					handleDependencies(check, tagihan);
				}

				boolean adminSaatIni = merupakanAdmin();
				for (MyCheckboxConfig checkboxConfig : pilihan) {
					Tagihan tag = (Tagihan) checkboxConfig.getAttribute("tagihan");
					if (tag != null && tag.getItemBiayaSekolah() != null && tag.getItemBiayaSekolah().getWajibPilih()) {
						if (adminSaatIni) {
							// Admin: HORMATI centang saat ini (boleh di-uncheck oleh admin) —
							// JANGAN dipaksa tercentang lagi, sinkronkan peta tagihan mengikuti
							// status centang yang sebenarnya. Tanpa ini, klik uncheck admin akan
							// langsung "dilawan" oleh listener ini sendiri di event yang sama.
							checkboxConfig.setDisabled(false);
							if (checkboxConfig.isChecked()) {
								tagihans.put(tag.getId(), tag);
							} else {
								tagihans.remove(tag.getId());
							}
						} else {
							// Siswa/orang tua: aturan lama — wajib tercentang & terkunci.
							checkboxConfig.setChecked(true);
							tagihans.put(tag.getId(), tag);
							checkboxConfig.setDisabled(true);
						}
					}
				}
				hitungUlangTagihan();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:269");
			}
		}
	};

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		applyFullSizeHidden(window);

		lblSiswa = new MyLabelBoldAja("Siswa :");
		siswa = new AmbilDataSiswaBanbox();
		lblCalonSiswa = new MyLabelBoldAja("Calon Siswa :");
		calonSiswa = new AmbilDataCalonSiswaBanbox();

		if (ExecutionsCtrl.getCurrent().getParameter("lbl_siswa") != null)
			tampilCalonSiswa = false;
		else if (ExecutionsCtrl.getCurrent().getParameter("lbl_calon_siswa") != null)
			tampilSiswa = false;

		String pLangsungBayar = ExecutionsCtrl.getCurrent().getParameter("langsungBayar");
		if (pLangsungBayar != null)
			langsungBayar = Boolean.parseBoolean(pLangsungBayar);

		modeWizardSiswa = "1".equals(ExecutionsCtrl.getCurrent().getParameter("wizardsiswa"));
		modeTopupSiswa = "1".equals(ExecutionsCtrl.getCurrent().getParameter("modetopup"));

		String pSiswa = ExecutionsCtrl.getCurrent().getParameter("siswa");
		String pCalonSiswa = ExecutionsCtrl.getCurrent().getParameter("calon_siswa");

		Session session = null;
		try {
			session = HibernateUtil.openSession();
			if (pSiswa != null) {
				selectedSiswa = (Siswa) ConstantValues.simpleObject(
						session.createCriteria(Siswa.class).add(Restrictions.idEq(Long.parseLong(pSiswa))),
						Siswa.class);
				setupInitialStudentVisibility(true);
			} else if (pCalonSiswa != null) {
				selectedCalonSiswa = (CalonSiswa) ConstantValues.simpleObject(
						session.createCriteria(CalonSiswa.class).add(Restrictions.idEq(Long.parseLong(pCalonSiswa))),
						CalonSiswa.class);
				setupInitialStudentVisibility(false);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:313");
		} finally {
			closeSessionAndDisconnect(session);
		}

		if (ExecutionsCtrl.getCurrent().getParameter("tagihans") != null) {
			tagihansPilih = new HashSet<Long>();
			for (String s : ExecutionsCtrl.getCurrent().getParameter("tagihans").split(",")) {
				try {
					if (!s.trim().isEmpty())
						tagihansPilih.add(Long.parseLong(s.trim()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PembayaranOnline.java:324");
				}
			}
		}

		String pBulan = ExecutionsCtrl.getCurrent().getParameter("pilihBulan");
		if (pBulan != null && !pBulan.isEmpty() && !pBulan.equalsIgnoreCase("null")) {
			try {
				pilihBulan = Integer.parseInt(pBulan);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PembayaranOnline.java:333");
			}
		}

		String pTahun = ExecutionsCtrl.getCurrent().getParameter("pilihTahun");
		if (pTahun != null && !pTahun.isEmpty() && !pTahun.equalsIgnoreCase("null")) {
			try {
				pilihTahun = Integer.parseInt(pTahun);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PembayaranOnline.java:341");
			}
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null) {
			if (tbmuser.getSiswa() != null) {
				selectedSiswa = tbmuser.getSiswa();
				setupInitialStudentVisibility(true);
			} else if (tbmuser.getCalonSiswa() != null) {
				selectedCalonSiswa = tbmuser.getCalonSiswa();
				setupInitialStudentVisibility(false);
			}
		}

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				init();
			}
		});
	}

	private void setupInitialStudentVisibility(boolean isSiswa) {
		lblSiswa.setVisible(isSiswa);
		siswa.setVisible(isSiswa);
		lblCalonSiswa.setVisible(!isSiswa);
		calonSiswa.setVisible(!isSiswa);
		if (isSiswa) {
			siswa.setAttribute("siswa", selectedSiswa);
			siswa.setValue(selectedSiswa == null ? "" : selectedSiswa.getNama());
			siswa.setDisabled(true);
		} else {
			calonSiswa.setAttribute("calonSiswa", selectedCalonSiswa);
			calonSiswa.setValue(selectedCalonSiswa == null ? "" : selectedCalonSiswa.getNama());
			calonSiswa.setDisabled(true);
		}
	}

	private void checkUlangHarusBayar(List<Long> harusBayars, Tagihan tagihan) {
		for (MyCheckboxConfig checkboxConfig : pilihan) {
			Tagihan tag = (Tagihan) checkboxConfig.getAttribute("tagihan");
			if (tag != null && tagihan.getItemBiayaSekolah().getHarusBayar() != null && tag.getItemBiayaSekolah()
					.getId().equals(tagihan.getItemBiayaSekolah().getHarusBayar().getId())) {
				if (tag.getItemBiayaSekolah().getHarusBayar() != null) {
					harusBayars.add(tag.getItemBiayaSekolah().getHarusBayar().getId());
					checkUlangHarusBayar(harusBayars, tag);
				}
			}
		}
	}

	private void handleDependencies(MyCheckboxConfig check, Tagihan tagihan) {
		boolean pilih = check.isChecked();

		if (tagihan.getPengaturanBiaya() != null && tagihan.getPengaturanBiaya().getWajibDibayarSebelumnya() != null
				&& !tagihan.getPengaturanBiaya().getWajibDibayarSebelumnya().isEmpty()) {
			tagihans.remove(tagihan.getId());
			for (MyCheckboxConfig checkboxConfig : pilihan) {
				Tagihan tag = (Tagihan) checkboxConfig.getAttribute("tagihan");
				if (tag != null
						&& tagihan.getPengaturanBiaya().getWajibDibayarSebelumnya().contains("," + tag.getId() + ",")) {
					checkboxConfig.setDisabled(false);
					checkboxConfig.setChecked(false);
					tagihans.remove(tag.getId());
				}
			}
			if (pilih) {
				for (MyCheckboxConfig checkboxConfig : pilihan) {
					Tagihan tag = (Tagihan) checkboxConfig.getAttribute("tagihan");
					if (tag != null && tagihan.getPengaturanBiaya().getWajibDibayarSebelumnya()
							.contains("," + tag.getId() + ",")) {
						checkboxConfig.setChecked(true);
						tagihans.put(tag.getId(), tag);
						checkboxConfig.setDisabled(check != checkboxConfig);
					}
				}
			}
		}

		if (tagihan.getNominalBiaya() != null && tagihan.getNominalBiaya().getDibayarSebayak() != null
				&& tagihan.getNominalBiaya().getDibayarSebayak() > 1) {
			tagihans.remove(tagihan.getId());
			for (MyCheckboxConfig checkboxConfig : pilihan) {
				Tagihan tag = (Tagihan) checkboxConfig.getAttribute("tagihan");
				if (tag != null && checkboxConfig.isChecked() && checkboxConfig.isDisabled()
						&& (tag.getPengaturanBiaya().getJenisBiayaSekolah()
								.getPilihanItemBiayaTerakumulasiBulanan()
								|| tag.getItemBiayaSekolah().getId().equals(tagihan.getItemBiayaSekolah().getId()))) {
					checkboxConfig.setDisabled(false);
					checkboxConfig.setChecked(false);
					tagihans.remove(tag.getId());
				}
			}
			if (pilih) {
				for (MyCheckboxConfig checkboxConfig : pilihan) {
					Tagihan tag = (Tagihan) checkboxConfig.getAttribute("tagihan");
					if (tag != null
							&& tag.getItemBiayaSekolah().getId().equals(tagihan.getItemBiayaSekolah().getId())) {
						String ta1 = tag.getTahunAjaran() != null ? tag.getTahunAjaran().split("/")[0] : "";
						String val1 = ta1 + tag.getBayarKe();
						String digitEmpat1 = ("000000000000" + val1);
						String lastFour1 = digitEmpat1.substring(digitEmpat1.length() - 4);
						Long tag1 = Long.parseLong(
								(tag.getTahunbulan() == null ? "" : tag.getTahunbulan().toString()) + lastFour1);

						String ta2 = tagihan.getTahunAjaran() != null ? tagihan.getTahunAjaran().split("/")[0] : "";
						String val2 = ta2 + tagihan.getBayarKe();
						String digitEmpat2 = ("000000000000" + val2);
						String lastFour2 = digitEmpat2.substring(digitEmpat2.length() - 4);
						Long tag2 = Long
								.parseLong((tagihan.getTahunbulan() == null ? "" : tagihan.getTahunbulan().toString())
										+ lastFour2);

						if (tag1 <= tag2) {
							checkboxConfig.setChecked(true);
							tagihans.put(tag.getId(), tag);
							checkboxConfig.setDisabled(check != checkboxConfig);
						}
					}
				}
			}
		} else if (tagihan.getNominalBiaya() != null && tagihan.getPengaturanBiaya()
				.getJenisBiayaSekolah().getPeriode().equalsIgnoreCase("Bulanan")) {
			tagihans.remove(tagihan.getId());
			for (MyCheckboxConfig checkboxConfig : pilihan) {
				Tagihan tag = (Tagihan) checkboxConfig.getAttribute("tagihan");
				if (tag != null && checkboxConfig.isChecked() && checkboxConfig.isDisabled()
						&& (tag.getPengaturanBiaya().getJenisBiayaSekolah()
								.getPilihanItemBiayaTerakumulasiBulanan()
								|| tag.getItemBiayaSekolah().getId().equals(tagihan.getItemBiayaSekolah().getId()))) {
					checkboxConfig.setDisabled(false);
					checkboxConfig.setChecked(false);
					tagihans.remove(tag.getId());
				}
			}
			if (pilih) {
				for (MyCheckboxConfig checkboxConfig : pilihan) {
					Tagihan tag = (Tagihan) checkboxConfig.getAttribute("tagihan");
					if (tag != null) {
						boolean accumulatedCondition = tagihan.getPengaturanBiaya()
								.getJenisBiayaSekolah().getPilihanItemBiayaTerakumulasiBulanan()
								&& tag.getPengaturanBiaya().getJenisBiayaSekolah()
										.getPilihanItemBiayaTerakumulasiBulanan()
								&& tagihan.getTahunbulan() != null && tag.getTahunbulan() != null
								&& tagihan.getTahunbulan().equals(tag.getTahunbulan());

						if (accumulatedCondition
								|| tag.getItemBiayaSekolah().getId().equals(tagihan.getItemBiayaSekolah().getId())) {
							Integer tag1 = tag.getTahunbulan() == null ? 0 : tag.getTahunbulan();
							Integer tag2 = tagihan.getTahunbulan() == null ? 0 : tagihan.getTahunbulan();

							if (tag1 <= tag2 || accumulatedCondition) {
								checkboxConfig.setChecked(true);
								tagihans.put(tag.getId(), tag);
								checkboxConfig.setDisabled(check != checkboxConfig);
								for (MyCheckboxConfig checkboxConfigdata : pilihan) {
									Tagihan tagdata = (Tagihan) checkboxConfigdata.getAttribute("tagihan");
									if (tagdata != null && (tagdata.getPengaturanBiaya()
											.getJenisBiayaSekolah().getPilihanItemBiayaTerakumulasiBulanan()
											&& tag.getPengaturanBiaya().getJenisBiayaSekolah()
													.getPilihanItemBiayaTerakumulasiBulanan()
											&& tagdata.getTahunbulan() != null && tag.getTahunbulan() != null
											&& tagdata.getTahunbulan().equals(tag.getTahunbulan()))) {
										checkboxConfigdata.setChecked(true);
										tagihans.put(tagdata.getId(), tagdata);
										checkboxConfigdata.setDisabled(check != checkboxConfigdata);
									}
								}
							}
						}
					}
				}
			}
		} else {
			boolean ada = tagihan.getItemBiayaSekolah().getHarusBayar() != null;
			if (ada) {
				List<Long> harusBayars = new ArrayList<Long>();
				harusBayars.add(tagihan.getItemBiayaSekolah().getHarusBayar().getId());
				checkUlangHarusBayar(harusBayars, tagihan);
				tagihans.remove(tagihan.getId());
				for (MyCheckboxConfig checkboxConfig : pilihan) {
					Tagihan tag = (Tagihan) checkboxConfig.getAttribute("tagihan");
					if (tag != null && checkboxConfig.isChecked() && checkboxConfig.isDisabled()
							&& harusBayars.contains(tag.getItemBiayaSekolah().getId())) {
						checkboxConfig.setDisabled(false);
						checkboxConfig.setChecked(false);
						tagihans.remove(tag.getId());
					}
				}
				if (pilih) {
					tagihans.put(tagihan.getId(), tagihan);
					for (MyCheckboxConfig checkboxConfig : pilihan) {
						Tagihan tag = (Tagihan) checkboxConfig.getAttribute("tagihan");
						if (tag != null && harusBayars.contains(tag.getItemBiayaSekolah().getId())) {
							checkboxConfig.setChecked(true);
							tagihans.put(tag.getId(), tag);
							checkboxConfig.setDisabled(check != checkboxConfig);
						}
					}
				}
			} else {
				if (pilih)
					tagihans.put(tagihan.getId(), tagihan);
				else
					tagihans.remove(tagihan.getId());
			}
		}
	}

	private void initTabAndPanel(Tabs tabs, Tabpanels panels, MyTabConfig tab, final Tabpanel panel, final String url,
			boolean isMobileVariant) {
		tab.setParent(tabs);
		panel.setParent(panels);
		applyFullSizeScrollable(panel);
		if (url != null && !url.isEmpty()) {
			tab.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (panel.getChildren().size() == 0) {
						MyWindow windowLocal = new MyWindow("", "none", false);
						applyFullSizeHidden(windowLocal);
						windowLocal.setParent(panel);
						String finalUrl = url
								+ (selectedCalonSiswa != null
										? (url.contains("?") ? "&" : "?") + "calon_siswa=" + selectedCalonSiswa.getId()
										: "")
								+ (selectedSiswa != null
										? (url.contains("?") ? "&" : "?") + "siswa=" + selectedSiswa.getId()
										: "");
						MyInclude include = new MyInclude(finalUrl);
						include.setHeight("100%");
						include.setWidth("100%");
						include.setParent(windowLocal);
						refreshClientScrollableLayout();
					}
				}
			});
		}
	}

	private void init() {
		Tbmuser tbmuser = Common.getCurrentUser();
		Tabbox tabbox = new Tabbox();
		if (tbmuser != null && tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null)
			tabbox.setParent(window);
		applyFullSizeHidden(tabbox);

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);
		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);
		applyFullSizeHidden(tabpanels);

		MyTabConfig tab1 = new MyTabConfig("Proses Pembayaran");
		tab1.setParent(tabs);
		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);
		applyFullSizeScrollable(tabpanel1);

		tabpanel3 = new ais.ui.util.MyTabpanel();
		MyTabConfig tab3 = new MyTabConfig("Sejarah Pemb.Siswa");
		tab3.setVisible(siswa.isVisible());
		initTabAndPanel(tabs, tabpanels, tab3, tabpanel3, "/pages/master/sekolah/pembayaran_siswa.zul?1=1", false);

		tabpanel2 = new ais.ui.util.MyTabpanel();
		MyTabConfig tab2 = new MyTabConfig("Sejarah Pemb.Calon Siswa");
		tab2.setVisible(calonSiswa.isVisible());
		initTabAndPanel(tabs, tabpanels, tab2, tabpanel2, "/pages/master/sekolah/pembayaran_calon_siswa.zul?1=1",
				false);

		tabBri = new MyTabConfig("Pembayaran Via BRI");
		tabpanelBri = new ais.ui.util.MyTabpanel();
		initTabAndPanel(tabs, tabpanels, tabBri, tabpanelBri, "/pages/master/bri/bri_request.zul?1=1", false);

		tabBni = new MyTabConfig("Pembayaran Via BNI");
		tabpanelBni = new ais.ui.util.MyTabpanel();
		initTabAndPanel(tabs, tabpanels, tabBni, tabpanelBni, "/pages/master/bni/bni_request.zul?1=1", false);

		tabOnline = new MyTabConfig("Pembayaran Online");
		tabpanelOnline = new ais.ui.util.MyTabpanel();
		initTabAndPanel(tabs, tabpanels, tabOnline, tabpanelOnline, "/pages/master/virtual_account_bank.zul?1=1",
				false);

		tabBsi = new MyTabConfig("Pembayaran Via BSI");
		tabpanelBsi = new ais.ui.util.MyTabpanel();
		initTabAndPanel(tabs, tabpanels, tabBsi, tabpanelBsi, "/pages/master/bsi/bsi_request.zul?1=1", false);

		MyTabConfig tagihan = new MyTabConfig("Tagihan");
		Tabpanel tabpanelTagihan = new ais.ui.util.MyTabpanel();
		initTabAndPanel(tabs, tabpanels, tagihan, tabpanelTagihan,
				Common.isMobile() ? "/common/mobile/tagihan.zul?1=1" : "/pages/master/sekolah/tagihan.zul?1=1",
				Common.isMobile());

		MyTabConfig tabunganTab = new MyTabConfig("Tabungan");
		Tabpanel tabpanelTabungan = new ais.ui.util.MyTabpanel();
		initTabAndPanel(tabs, tabpanels, tabunganTab, tabpanelTabungan, "/pages/master/deposit.zul?1=1", false);

		MyTabConfig pembelianTab = new MyTabConfig("Pembelian");
		Tabpanel tabpanelPembelian = new ais.ui.util.MyTabpanel();
		initTabAndPanel(tabs, tabpanels, pembelianTab, tabpanelPembelian,
				"/pages/master/koperasi/pem_online.zul?langsungBayar=true&sumberPembayaran=siswa&modePelanggan=siswa&jenisPelanggan=siswa&hanyaSiswa=true&dariPembayaranOnline=true", false);

		MyTabConfig pengeluaran = new MyTabConfig("Pengeluaran");
		Tabpanel tabpanelPengeluaran = new ais.ui.util.MyTabpanel();
		initTabAndPanel(tabs, tabpanels, pengeluaran, tabpanelPengeluaran,
				"/pages/master/pengeluaran_mahasiswa.zul?1=1", false);

		Borderlayout borderlayout = new Borderlayout();
		applyFullSizeHidden(borderlayout);
		borderlayout.setParent(
				(tbmuser != null && tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null) ? tabpanel1
						: window);

		North north = new North();
		north.setParent(borderlayout);
		north.setHeight(Common.isMobile() ? "170px" : "118px");
		north.setStyle("border:0; margin:0; padding:0; overflow:auto; background:#f8fafc;");
		Vbox vboxUtama = new Vbox();
		vboxUtama.setWidth("100%");
		vboxUtama.setStyle("padding:8px 10px; box-sizing:border-box;");
		vboxUtama.setParent(north);

		Box hbox = Common.isMobile() ? new Vbox() : new Hbox();
		if (Common.isMobile())
			hbox.setWidth("100%");
		else {
			hbox.setHeight("70px");
			hbox.setPack("center");
			hbox.setAlign("center");
		}
		hbox.setParent(vboxUtama);

		hbox.appendChild(lblSiswa);
		hbox.appendChild(siswa);
		siswa.setWidth("100px");
		hbox.appendChild(lblCalonSiswa);
		hbox.appendChild(calonSiswa);
		calonSiswa.setWidth("100px");

		bulan = new Combobox();
		tahun = new Combobox();
		bulan.setReadonly(true);
		tahun.setReadonly(true);
		if (lblSiswa.isVisible()) {
			hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("sd Bulan :")));
			hbox.appendChild(bulan);
			hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("sd Tahun :")));
			hbox.appendChild(tahun);
		}

		for (int i = 0; i < 12; i++) {
			Comboitem ci = new Comboitem(Common.BULAN[i]);
			ci.setValue(i + 1);
			bulan.appendChild(ci);
		}
		Common.selectComboItem(bulan,
				pilihBulan == null ? (ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1) : pilihBulan);

		Integer currTahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = currTahun - 10; i < currTahun + 10; i++) {
			Comboitem ci = new Comboitem(i + "");
			ci.setValue(i);
			tahun.appendChild(ci);
		}
		Common.selectComboItem(tahun, pilihTahun == null ? currTahun : pilihTahun);

		Sekolah sekolah = SekolahUtil.getSekolah();
		if (sekolah != null && sekolah.getId() == null)
			sekolah = null;

		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null && sekolah != null) {
			tanggalTransaski = new MyDatebox(WaktuUtil.getDate());
			tanggalTransaski.setReadonly(false);
			hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu Transaksi :")));
			hbox.appendChild(tanggalTransaski);
		}

		labelTabungan = new MyLabelAgakBesar();
		labelTabungan.setParent(hbox);
		sisaTabungan.setParent(hbox);
		sisaTabungan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) {
				hitungUlangTagihan();
			}
		});

		Hbox toolbar = new Hbox();
		toolbar.setParent(vboxUtama);
		MyToolbarbuttonConfig btnRefresh = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh-cw.svg");
		toolbar.appendChild(btnRefresh);
		btnRefresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {

				reloadTagihan(true);
			}
		});

		MyToolbarbuttonConfig btnAnalisis = new MyToolbarbuttonConfig("Analisis Data", "/img/svg/search.svg");
		btnAnalisis.setTooltiptext("Telusuri Pengaturan Biaya satu per satu untuk menemukan penyebab tagihan tidak tampil");
		btnAnalisis.setVisible(tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
				&& tbmuser.getOrangTua() == null);
		btnAnalisis.setParent(toolbar);
		btnAnalisis.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Integer bln = bulan == null || bulan.getSelectedItem() == null ? null
						: (Integer) bulan.getSelectedItem().getValue();
				Integer thn = tahun == null || tahun.getSelectedItem() == null ? null
						: (Integer) tahun.getSelectedItem().getValue();
				AnalisisTagihanSekolahHelper.buka(getSiswaLokal(), getCalonSiswaLokal(), null, bln, thn,
						pilihan == null ? 0 : pilihan.size());
			}
		});

		MyToolbarbuttonConfig btnTagihan = new MyToolbarbuttonConfig("Surat Tagihan", "/img/svg/money-bills.svg");
		btnTagihan.setVisible(tbmuser != null && tbmuser.getCalonSiswa() == null);
		btnTagihan.setParent(toolbar);
		btnTagihan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Siswa s = getSiswaLokal();
				CalonSiswa cs = getCalonSiswaLokal();
				if (cs != null && cs.getSiswa() != null)
					s = cs.getSiswa();
				if (s != null)
					CommonReportHelper.prosesSuratTagihan(s, (Integer) bulan.getSelectedItem().getValue(),
							(Integer) tahun.getSelectedItem().getValue());
				else
					MyMessageboxConfig.show(
							"Siswa harus dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar siswa pada halaman ini; (2) pilih siswa yang akan diproses; (3) ulangi tindakan setelah siswa terpilih.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			}
		});

		MyToolbarbuttonConfig btnTandaTerima = new MyToolbarbuttonConfig("Tanda Terima", "/img/svg/printer.svg");
		btnTandaTerima.setVisible(tbmuser != null && tbmuser.getCalonSiswa() == null);
		btnTandaTerima.setParent(toolbar);
		btnTandaTerima.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Siswa s = getSiswaLokal();
				CalonSiswa cs = getCalonSiswaLokal();
				if (cs != null && cs.getSiswa() != null)
					s = cs.getSiswa();
				if (s != null)
					CommonReportHelper.prosesSuratTandaTerima(s, (Integer) bulan.getSelectedItem().getValue(),
							(Integer) tahun.getSelectedItem().getValue());
				else
					MyMessageboxConfig.show(
							"Siswa harus dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar siswa pada halaman ini; (2) pilih siswa yang akan diproses; (3) ulangi tindakan setelah siswa terpilih.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			}
		});

		// Tombol Wizard: buka Wizard Pembayaran Siswa/Calon Siswa 5-langkah mandiri
		// (WizardPembayaranSiswaHelper — periode → tagihan → nominal → cara bayar →
		// selesai). Entitas di-resolve SAAT KLIK (siswa dipilih setelah toolbar dibangun);
		// daftar tagihan dimuat ulang saat wizard ditutup.
		MyToolbarbuttonConfig btnWizardBayar = new MyToolbarbuttonConfig("Wizard Pembayaran",
				"/img/svg/payments.svg");
		btnWizardBayar.setParent(toolbar);
		btnWizardBayar.setTooltiptext("Buka Wizard Pembayaran - bayar tagihan langkah demi langkah");
		btnWizardBayar.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Siswa s = getSiswaLokal();
				CalonSiswa cs = getCalonSiswaLokal();
				EventListener setelahTutup = new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event a) throws Exception {
								reloadTagihan();
							}
						});
					}
				};
				if (s != null) {
					WizardPembayaranSiswaHelper.buka(s, setelahTutup);
				} else if (cs != null) {
					WizardPembayaranSiswaHelper.bukaCalon(cs, setelahTutup);
				} else {
					MyMessageboxConfig.show(
							"Siswa harus dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar siswa pada halaman ini; (2) pilih siswa yang akan diproses; (3) ulangi tindakan setelah siswa terpilih.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				}
			}
		});

		pilihCustom = new MyCheckboxConfig("Boleh pilih kustom per item biaya");
		pilihBukanTagihan = new MyCheckboxConfig("Tampilkan pilihan bukan tagihan");
		if (tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
				&& tbmuser.getOrangTua() == null) {
			pilihCustom.setParent(toolbar);
			pilihCustom.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) {
					reloadTagihan();
				}
			});
			pilihBukanTagihan.setParent(toolbar);
			pilihBukanTagihan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) {
					reloadTagihan();
				}
			});

			// Tombol History: tampilkan data audit (Envers) PembayaranSiswaDetail milik
			// siswa/calon siswa terpilih -- pola sama dgn tombol History di layar
			// Pembayaran Mahasiswa (RevisiCicilanPembayaranHelper).
			MyToolbarbuttonConfig btnHistory = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
			btnHistory.setTooltiptext("Riwayat perubahan (audit) detail pembayaran siswa ini");
			btnHistory.setParent(toolbar);
			btnHistory.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					bukaRevisiPembayaranSiswaDetail(false);
				}
			});

			MyToolbarbuttonConfig btnRestore = new MyToolbarbuttonConfig("Restore", "/img/refresh.gif");
			btnRestore.setTooltiptext(
					"Kembalikan (restore) detail pembayaran siswa ini dari riwayat revisi -- tersedia Restore per-revisi dan Restore Terbaru mulai tanggal");
			btnRestore.setParent(toolbar);
			btnRestore.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					bukaRevisiPembayaranSiswaDetail(true);
				}
			});
		}

		center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);
		center.setTitle("Pembayaran Online");
		center.setBorder("none");
		center.setStyle("border:0; padding:0; margin:0; overflow:hidden; background:#f8fafc; box-sizing:border-box;");

		ais.ui.util.MyPortallayout portalUtama = new ais.ui.util.MyPortallayout();
		portalUtama.setParent(center);
		portalUtama.setWidth("100%");
		// align-items/align-content/justify-content flex-start → semua kolom & barisnya
		// RATA ATAS (cegah konten ter-tengah-kan secara vertikal saat tinggi portal > isi).
		// TANPA min-height besar agar tak ada area kosong yang mereservasi tinggi (dulu 600px).
		portalUtama.setStyle(
				"background:#f8fafc;align-items:flex-start;align-content:flex-start;justify-content:flex-start;");

		ais.ui.util.MyPortalchildren kolKiriPC = new ais.ui.util.MyPortalchildren();
		kolKiriPC.setWidth("55%");
		kolKiriPC.setSclass("ais-portal-col");
		kolKiriPC.setParent(portalUtama);
		colKiri = kolKiriPC;

		ais.ui.util.MyPortalchildren eastPC = new ais.ui.util.MyPortalchildren();
		eastPC.setWidth("45%");
		eastPC.setSclass("ais-portal-col");
		eastPC.setParent(portalUtama);
		east = eastPC;

		// Kolom ketiga ber-lebar 100%: karena MyPortallayout memakai flex-wrap,
		// kolom ini turun ke baris baru di bawah kedua kolom (kiri 55% + kanan 45%)
		// dan membentang penuh. Dipakai untuk Dasbor Riwayat Pembayaran agar full
		// sampai ke sebelah kiri, tetap responsif (turun/menyusun ulang di layar sempit).
		ais.ui.util.MyPortalchildren bawahPC = new ais.ui.util.MyPortalchildren();
		bawahPC.setWidth("100%");
		bawahPC.setSclass("ais-portal-col");
		bawahPC.setParent(portalUtama);
		colBawah = bawahPC;

		// PENTING: colKiri (daftar tagihan + tombol Bayar) TIDAK dibatasi tingginya. Pembatasan
		// max-height 520px sebelumnya MEMOTONG daftar item & tombol Bayar di bawah ("terpotong")
		// pada siswa dengan banyak item (mis. KOMITE bulanan). Biarkan colKiri setinggi isinya;
		// seluruh item & tombol Bayar tetap terjangkau (halaman bergulir di center-body), dan
		// dasbor (colBawah) berada tepat di bawahnya tanpa ruang kosong (min-height portal sudah
		// dilepas) — jadi tidak "turun terlalu jauh" maupun "terpotong".

		siswa.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event arg0) {
				reloadTagihan();
			}
		});
		calonSiswa.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event arg0) {
				reloadTagihan();
			}
		});
		bulan.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) {
				reloadTagihan();
			}
		});
		tahun.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) {
				reloadTagihan();
			}
		});

		// Mode Wizard Siswa: sisakan HANYA daftar tagihan + total + tombol bayar (colKiri).
		// Pemilih siswa/bulan/tahun (north) sudah pra-isi dari param → sembunyikan beserta
		// riwayat (east), dasbor (colBawah), dan strip tab. colKiri dilebarkan penuh.
		if (modeWizardSiswa) {
			try {
				// Pemilih siswa/bulan/tahun (north) sudah pra-isi dari param → sembunyikan.
				// PENTING: di borderlayout, region north yg di-setVisible(false) kadang TETAP
				// memesan tingginya (mis. 170px di HP) → muncul RUANG KOSONG di atas konten
				// karena center ter-offset sebanyak tinggi north. Set tinggi north 0 (server-side,
				// gerbang modeWizardSiswa — ANDAL, tak bergantung flag isMobile) agar center
				// menempel ke atas tanpa ruang kosong.
				if (north != null) {
					north.setVisible(false);
					north.setHeight("0px");
				}
				if (tabs != null)
					tabs.setVisible(false);
				// Kolom portal (sclass ais-portal-col) MENGABAIKAN setVisible (di-override CSS),
				// jadi diatur lewat LEBAR. Lebarkan SEMUA kolom jadi 100% agar menumpuk penuh:
				// tagihan (colKiri), riwayat pembayaran (east), dan dasbor (colBawah) — sehingga
				// tabel riwayat tidak lagi ramping 45% & tak ada ruang kosong di kanan.
				if (colKiri instanceof HtmlBasedComponent)
					((HtmlBasedComponent) colKiri).setWidth("100%");
				if (east instanceof HtmlBasedComponent)
					((HtmlBasedComponent) east).setWidth("100%");
				if (colBawah instanceof HtmlBasedComponent)
					((HtmlBasedComponent) colBawah).setWidth("100%");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PembayaranOnline.java:935");
				// non-fatal: bila gagal, layar penuh tetap dapat dipakai
			}
		}

		// Mode Topup: pemilih siswa tetap tampil, tetapi daftar riwayat/dasbor dan tab
		// lain disembunyikan. Kolom kiri menjadi layar pembayaran topup penuh.
		if (modeTopupSiswa) {
			try {
				if (tabs != null)
					tabs.setVisible(false);
				if (colKiri instanceof HtmlBasedComponent)
					((HtmlBasedComponent) colKiri).setWidth("100%");
				if (east instanceof HtmlBasedComponent) {
					((HtmlBasedComponent) east).setWidth("0px");
					((HtmlBasedComponent) east).setVisible(false);
					appendStyle((HtmlBasedComponent) east, "display:none !important;");
				}
				if (colBawah instanceof HtmlBasedComponent) {
					((HtmlBasedComponent) colBawah).setWidth("0px");
					((HtmlBasedComponent) colBawah).setVisible(false);
					appendStyle((HtmlBasedComponent) colBawah, "display:none !important;");
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PembayaranOnline.java:modeTopup");
			}
		}

		reloadTagihan();
		refreshClientScrollableLayout();
	}

	private void confirmAndExecute(String actionName, final double biayaAdministrasi, final PaymentAction action)
			throws Exception {
		String msg = "Apakah yakin ingin melakukan " + Common.getBahasaConfig(actionName).trim()
				+ " untuk:\nSiswa : "
				+ (getSiswaLokal() != null ? getSiswaLokal().getNama() : getCalonSiswaLokal().getNama())
				+ "\nTotal tagihan : " + totalTagihan.getValue()
				+ (biayaAdministrasi > 0.1
						? "\nBiaya administrasi : " + Common.numberFormat.get().format(biayaAdministrasi)
								+ "\nTotal yang akan dibayar : "
								+ Common.numberFormat.get().format(t + biayaAdministrasi)
						: " ")
				+ "\nTerbilang : " + IndonesianNumberToWords.convert((long) (t + biayaAdministrasi));

		MyMessageboxConfig.show(msg, "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
				MyMessageboxConfig.QUESTION, new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						if (Integer.parseInt(ev.getData().toString()) == MyMessageboxConfig.OK) {
							Common.createDefaultTimer(new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									action.execute();
								}
							}, "Proses pembayaran ..");
						}
					}
				});
	}

	private boolean checkKondisiSebelumbayar() throws Exception {
		if (siswa.getAttribute("siswa") == null && getCalonSiswaLokal() == null) {
			MyMessageboxConfig.show(
					"Siswa harus dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar siswa pada halaman ini; (2) pilih siswa yang akan diproses; (3) ulangi tindakan setelah siswa terpilih.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		boolean adaDepositTambahan = deposit != null && deposit.getValue() != null && deposit.getValue() > 0.1
				&& !deposit.isDisabled();
		if (tagihans.isEmpty() && !adaDepositTambahan) {
			MyMessageboxConfig.show(
					"Belum ada tagihan yang dipilih untuk dibayar. Langkah yang dapat dilakukan: (1) periksa daftar tagihan yang tersedia; (2) tandai tagihan yang akan dibayar; (3) ulangi proses pembayaran setelah tagihan dipilih.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		double totalAkanDibayar = hitungTotalTagihanTerpilihUntukBayar();
		boolean tolakTotalNolAtauMinus = true;
		try {
			tolakTotalNolAtauMinus = Common.bolehKonfigurasi("payment_gateway_tolak_total_nol_atau_minus");
		} catch (Exception e) {
			tolakTotalNolAtauMinus = true;
		}
		if (tolakTotalNolAtauMinus && totalAkanDibayar <= 0.0 && !adaDepositTambahan) {
			MyMessageboxConfig.show(
					"Total tagihan yang dipilih bernilai nol atau minus. Nilai minus biasanya merupakan bantuan, beasiswa, potongan, atau koreksi sehingga tidak dapat dibuatkan transaksi pembayaran/VA tersendiri. Langkah yang dapat dilakukan: (1) periksa kembali tagihan yang dipilih; (2) hapus tagihan bernilai nol atau minus dari pilihan; (3) hubungi administrator apabila memerlukan penyesuaian data.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		return true;
	}

	private double hitungTotalTagihanTerpilihUntukBayar() {
		double total = 0.0;
		try {
			for (Tagihan tagihan : tagihans.values()) {
				if (tagihan == null) {
					continue;
				}
				Double denda = tagihan.getDenda() != null ? tagihan.getDenda() : 0.0;
				Double diskon = tagihan.getDiskon() != null ? tagihan.getDiskon() : 0.0;
				Double nominal = tagihan.getNominal() != null ? tagihan.getNominal() : 0.0;
				total += (nominal.doubleValue() + denda.doubleValue()) - diskon.doubleValue();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return total;
	}

	private void hitungUlangTagihan() {
		if (!pilihCustom.isChecked()) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			Integer tahunbulansekarang = PembayaranSiswa.convert(calendar.get(Calendar.YEAR),
					calendar.get(Calendar.MONTH) + 1);
			boolean adminSaatIni = merupakanAdmin();
			for (MyCheckboxConfig cb : pilihan) {
				Tagihan tagdata = (Tagihan) cb.getAttribute("tagihan");
				if ((tagdata != null && tagdata.getItemBiayaSekolah() != null
						&& tagdata.getItemBiayaSekolah().getWajibPilihJikaBulanDipilih()
						&& tagdata.getTahunbulan() != null && tagdata.getTahunbulan() <= tahunbulansekarang)) {
					if (adminSaatIni) {
						// Admin: HORMATI centang saat ini (boleh di-uncheck), jangan dipaksa
						// tercentang lagi — sinkronkan peta tagihan mengikuti status centang
						// yang sebenarnya.
						cb.setDisabled(false);
						if (cb.isChecked()) {
							tagihans.put(tagdata.getId(), tagdata);
						} else {
							tagihans.remove(tagdata.getId());
						}
					} else {
						// Siswa/orang tua: aturan lama — wajib tercentang & terkunci.
						cb.setChecked(true);
						cb.setDisabled(true);
						tagihans.put(tagdata.getId(), tagdata);
					}
				}
			}
		}
		t = 0.0;
		for (Tagihan tagihan : tagihans.values()) {
			Double denda = tagihan.getDenda() != null ? tagihan.getDenda() : 0.0;
			Double diskon = tagihan.getDiskon() != null ? tagihan.getDiskon() : 0.0;
			t += (tagihan.getNominal() + denda) - diskon;
		}
		if (deposit != null && deposit.getValue() != null && !deposit.isDisabled()) {
			t += deposit.getValue();
		}
		if (sisaTabungan.isChecked()) {
			if (tabungan != null && tabungan > t) {
				t = 0.0;
			} else if (tabungan != null) {
				t = t - tabungan;
			}
		}
		totalTagihan.setValue(Common.numberFormat.get().format(t));
		terbilang.setValue(IndonesianNumberToWords.convert((long) t));
		refreshPanelJurnalPembayaranOnline();
	}

	private MyColumnConfig addCol(Columns cols, String title, String width, String align) {
		MyColumnConfig c = new MyColumnConfig(title);
		c.setParent(cols);
		if (width != null)
			c.setWidth(width);
		if (align != null)
			c.setAlign(align);
		return c;
	}

	private void reloadRiwayatPembayaran(final Siswa s, final CalonSiswa cs, final Row sub) {
		Common.clear(east);

		// Container utama vertikal
		Vbox vMain = new Vbox();
		vMain.setParent(east);
		vMain.setWidth("100%");
		vMain.setHeight("100%");
		// Riwayat dibatasi tingginya (di-scroll internal) agar baris atas tidak terlalu
		// tinggi → dasbor di bawahnya tidak turun terlalu jauh.
		vMain.setStyle("padding:10px; background:#fdfdfd; box-sizing:border-box; overflow-y:auto; overflow-x:hidden; max-height:480px;");

		// =========================
		// HEADER RIWAYAT PEMBAYARAN
		// =========================
		vMain.appendChild(new MyLabelBold("Riwayat Transaksi Terakhir"));
		vMain.appendChild(new ais.ui.util.MyHtml("<div style='font-size:11px;color:#64748b;margin:4px 0 8px 0;line-height:1.45;'>Pembayaran terakhir ditampilkan agar transaksi baru mudah dicek dan riwayat singkat tetap terbaca.</div>"));

		/*
		 * Riwayat pembayaran berada di panel samping. Pada data yang cukup banyak,
		 * tinggi grid sering melebihi area East/Row sehingga baris bawah terlihat
		 * seperti terpotong. Grid dibungkus container scroll agar data tetap bisa
		 * dibaca tanpa mengubah paging dan tanpa membebani memory.
		 */
		Vbox riwayatScroll = new Vbox();
		riwayatScroll.setParent(vMain);
		riwayatScroll.setWidth("100%");
		riwayatScroll.setHeight(Common.isMobile() ? "300px" : "340px");
		riwayatScroll.setStyle("overflow:auto;border:1px solid #dbe4f0;border-radius:8px;background:#ffffff;box-sizing:border-box;");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(riwayatScroll);
		grid.setStyle("border:0;background:#ffffff;");
		//grid.setVflex("min");

		Columns columns = new Columns();
		columns.setParent(grid);

		addCol(columns, "Waktu", "16%", null);
		addCol(columns, "Bln", "16%", null);
		addCol(columns, "Item", null, null);
		addCol(columns, "Via", "15%", null);
		addCol(columns, "Nominal", "15%", "right");

		final Rows rows = new Rows();
		rows.setParent(grid);

		// Paging melekat di bawah grid
		final org.zkoss.zul.Paging paging = new org.zkoss.zul.Paging();
		paging.setPageSize(10);
		paging.setParent(vMain);
		paging.setDetailed(true);
		paging.setStyle("border: 1px solid #ddd; border-top: none; background: #fafafa; margin-bottom: 30px;");

		renderRiwayatPage(0, s, cs, rows, paging, sub);

		paging.addEventListener("onPaging", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				org.zkoss.zul.event.PagingEvent pe = (org.zkoss.zul.event.PagingEvent) event;
				renderRiwayatPage(pe.getActivePage(), s, cs, rows, paging, sub);
			}
		});

		// =========================
		// BAGIAN DASBOR (FULL-WIDTH)
		// =========================
		// Dirender ke kolom portal full-width (colBawah) yang berada di baris bawah
		// kedua kolom, sehingga dasbor membentang penuh sampai ke sebelah kiri.
		// Tetap responsif: pada layar sempit kolom portal menumpuk otomatis.
		Component dasborHost = colBawah != null ? colBawah : vMain;
		if (colBawah != null) {
			Common.clear(colBawah);
		}

		Vbox vDasbor = new Vbox();
		vDasbor.setParent(dasborHost);
		vDasbor.setWidth("100%");
		vDasbor.setStyle("padding:10px; background:#fdfdfd; box-sizing:border-box;");

		Hbox hTitleDasbor = new Hbox();
		hTitleDasbor.setParent(vDasbor);
		hTitleDasbor.setAlign("center");
		hTitleDasbor.appendChild(new MyLabelBold("Dasbor Riwayat Pembayaran"));
		hTitleDasbor.setStyle(
				"border-bottom: 2px solid #0d6efd; width:100%; padding-bottom: 5px; margin-bottom: 15px;");

		vDasbor.appendChild(new ais.ui.util.MyHtml("<div style='font-size:11px;color:#64748b;margin:-6px 0 10px 0;line-height:1.45;'>Riwayat uang masuk, item biaya, dan pola pembayaran diringkas agar kondisi pembayaran cepat dipahami.</div>"));

		Vbox vDasborContainer = new Vbox();
		vDasborContainer.setParent(vDasbor);
		vDasborContainer.setWidth("100%");

		Integer bln = null;
		Integer thn = null;

		try {
			if (bulan != null && bulan.getSelectedItem() != null) {
				bln = (Integer) bulan.getSelectedItem().getValue();
			}
			if (tahun != null && tahun.getSelectedItem() != null) {
				thn = (Integer) tahun.getSelectedItem().getValue();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:1174");
		}

		renderDasborPanel(s, cs, vDasborContainer, bln, thn);
		renderPanelJurnalPembayaranOnline(vDasborContainer);
	}
	
	private void renderPanelJurnalPembayaranOnline(Vbox parent) {
		if (parent == null) {
			return;
		}

		Vbox box = new Vbox();
		box.setParent(parent);
		box.setWidth("100%");
		box.setStyle("margin-top:12px;border:1px solid #dbe4f0;border-radius:12px;background:#ffffff;box-sizing:border-box;padding:10px;box-shadow:0 6px 18px rgba(15,23,42,0.06);");

		box.appendChild(new MyLabelBold("Preview Jurnal Pembayaran"));
		box.appendChild(new ais.ui.util.MyHtml("<div style=\'font-size:11px;color:#64748b;line-height:1.45;margin:4px 0 8px 0;\'>Akun yang terlibat ditampilkan sebelum pembayaran diproses agar kas, piutang, denda, diskon, dan tabungan mudah diperiksa.</div>"));

		panelJurnalPembayaranOnline = new Vbox();
		panelJurnalPembayaranOnline.setParent(box);
		panelJurnalPembayaranOnline.setWidth("100%");
		panelJurnalPembayaranOnline.setHeight(Common.isMobile() ? "320px" : "360px");
		panelJurnalPembayaranOnline.setStyle("overflow:auto;border:1px solid #e2e8f0;border-radius:10px;background:#ffffff;box-sizing:border-box;");

		refreshPanelJurnalPembayaranOnline();
	}

	private void refreshPanelJurnalPembayaranOnline() {
		if (panelJurnalPembayaranOnline == null) {
			return;
		}
		try {
			Common.clear(panelJurnalPembayaranOnline);

			double tambahanDeposit = 0.0;
			try {
				tambahanDeposit = deposit == null || deposit.isDisabled() || deposit.getValue() == null ? 0.0
						: deposit.getValue().doubleValue();
			} catch (Exception e) {
				tambahanDeposit = 0.0;
			}

			boolean gunakanAkunDeposit = false;
			try {
				gunakanAkunDeposit = sisaTabungan != null && sisaTabungan.isChecked();
			} catch (Exception e) {
				gunakanAkunDeposit = false;
			}
			try {
				gunakanAkunDeposit = gunakanAkunDeposit || (akunPembayaranPreview != null
						&& akunPembayaranPreview.getDariTabungan());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PembayaranOnline.java:1227");
			}

			panelJurnalPembayaranOnline.appendChild(new ais.ui.util.MyHtml(buildDashboardTagihanTerpilihHtml(
					tambahanDeposit, gunakanAkunDeposit)));

			Grid gridJurnal = GrupTransaksi.tampilkanJurnalPembayaranSiswa(
					tagihans == null ? null : new ArrayList<Tagihan>(tagihans.values()), akunPembayaranPreview,
					gunakanAkunDeposit, Double.valueOf(tambahanDeposit));
			gridJurnal.setParent(panelJurnalPembayaranOnline);
			gridJurnal.setHeight("auto");
			gridJurnal.setWidth("100%");
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:1240");
			Common.clear(panelJurnalPembayaranOnline);
			panelJurnalPembayaranOnline.appendChild(new ais.ui.util.MyHtml("<div style=\'padding:10px;color:#991b1b;background:#fef2f2;border-radius:8px;font-size:11px;line-height:1.45;\'>Jurnal pembayaran belum dapat ditampilkan. Periksa kembali konfigurasi akun pembayaran dan akun item biaya.</div>"));
		}
	}

	private String buildDashboardTagihanTerpilihHtml(double tambahanDeposit, boolean gunakanAkunDeposit) {
		double nominal = 0.0;
		double denda = 0.0;
		double diskon = 0.0;
		int jumlahTagihan = 0;
		if (tagihans != null) {
			for (Tagihan tagihan : tagihans.values()) {
				if (tagihan == null) {
					continue;
				}
				jumlahTagihan++;
				nominal += safeDouble(tagihan.getNominal());
				denda += safeDouble(tagihan.getDenda());
				diskon += safeDouble(tagihan.getDiskon());
			}
		}
		double bruto = nominal + denda;
		double nettoSebelumTabungan = bruto - diskon + tambahanDeposit;
		double pemakaianTabungan = 0.0;
		if (gunakanAkunDeposit && tabungan != null && tabungan.doubleValue() > 0.0) {
			pemakaianTabungan = Math.min(tabungan.doubleValue(), Math.max(0.0, nettoSebelumTabungan));
		}
		double nettoBayar = nettoSebelumTabungan - pemakaianTabungan;
		if (nettoBayar < 0.0) {
			nettoBayar = 0.0;
		}
		int persenDiskon = bruto <= 0.0 ? 0 : (int) Math.round((diskon * 100.0) / bruto);
		int persenDenda = bruto <= 0.0 ? 0 : (int) Math.round((denda * 100.0) / bruto);
		if (persenDiskon > 100) {
			persenDiskon = 100;
		}
		if (persenDenda > 100) {
			persenDenda = 100;
		}

		StringBuilder html = new StringBuilder();
		html.append("<div style='font-family:Arial,sans-serif;background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:10px;margin-bottom:10px;'>");
		html.append("<div style='font-size:13px;font-weight:bold;color:#0f172a;margin-bottom:3px;'>Dashboard Tagihan Terpilih</div>");
		html.append("<div style='font-size:11px;color:#64748b;line-height:1.45;margin-bottom:10px;'>Ringkasan nilai yang akan diproses sebelum pembayaran atau VA dibuat.</div>");
		html.append("<div style='display:grid;grid-template-columns:repeat(4,minmax(115px,1fr));gap:8px;margin-bottom:10px;'>");
		appendInfoTile(html, "Dipilih", jumlahTagihan + " Tagihan", "#ffffff", "#334155");
		appendInfoTile(html, "Bruto", "Rp " + formatNumber(bruto), "#eff6ff", "#1d4ed8");
		appendInfoTile(html, "Diskon", "Rp " + formatNumber(diskon), "#f0fdf4", "#15803d");
		appendInfoTile(html, "Bayar Net", "Rp " + formatNumber(nettoBayar), "#fff7ed", "#c2410c");
		html.append("</div>");
		html.append("<div style='display:grid;grid-template-columns:repeat(2,minmax(180px,1fr));gap:8px;'>");
		html.append(buildMiniProgressHtml("Porsi denda terhadap bruto", persenDenda, "#f97316"));
		html.append(buildMiniProgressHtml("Porsi diskon terhadap bruto", persenDiskon, "#16a34a"));
		html.append("</div>");
		html.append("<div style='font-size:11px;color:#475569;margin-top:8px;line-height:1.5;'>Tambahan deposit: <b>Rp ")
				.append(formatNumber(tambahanDeposit)).append("</b>. Tabungan dipakai: <b>Rp ")
				.append(formatNumber(pemakaianTabungan)).append("</b>.</div>");
		html.append("</div>");
		return html.toString();
	}

	private String buildMiniProgressHtml(String label, int percent, String color) {
		if (percent < 0) {
			percent = 0;
		}
		if (percent > 100) {
			percent = 100;
		}
		StringBuilder html = new StringBuilder();
		html.append("<div style='background:#ffffff;border:1px solid #e2e8f0;border-radius:10px;padding:8px;'>");
		html.append("<div style='display:flex;justify-content:space-between;gap:8px;font-size:11px;color:#334155;font-weight:bold;margin-bottom:4px;'>")
				.append("<span>").append(escapeHtml(label)).append("</span><span>").append(percent).append("%</span></div>");
		html.append("<div style='height:10px;background:#e5e7eb;border-radius:999px;overflow:hidden;'><div style='height:10px;width:")
				.append(percent).append("%;background:").append(color).append(";'></div></div>");
		html.append("</div>");
		return html.toString();
	}
	
	@SuppressWarnings("deprecation")
	private void renderRiwayatPage(int activePage, final Siswa s, final CalonSiswa cs, final Rows rows,
			final org.zkoss.zul.Paging paging, final Row sub) {

		rows.getChildren().clear();

		Session session = null;
		List<PembayaranSiswaDetail> listPembayaranSiswaDetail = null;

		try {
			session = HibernateUtil.openSession();

			// ======================================================
			// FILTER SISWA / CALON SISWA
			// Penting:
			// Jangan pakai Restrictions.eq(field, null), karena bisa
			// membuat data lain ikut terhitung atau query jadi tidak akurat.
			// ======================================================
			Criterion filterPemilik = null;

			if (s != null && cs != null) {
				filterPemilik = Restrictions.or(
						Restrictions.eq("pembayaranSiswa.siswa", s),
						Restrictions.eq("pembayaranSiswa.calonSiswa", cs));
			} else if (s != null) {
				filterPemilik = Restrictions.eq("pembayaranSiswa.siswa", s);
			} else if (cs != null) {
				filterPemilik = Restrictions.eq("pembayaranSiswa.calonSiswa", cs);
			}

			if (filterPemilik == null) {
				paging.setTotalSize(0);

				MyFormRow rowKosong = new MyFormRow();
				rowKosong.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(rowKosong, "5");
				rowKosong.appendChild(new Label("Data siswa/calon siswa tidak ditemukan."));

				return;
			}

			// ======================================================
			// COUNT QUERY
			// Dibuat sama struktur join/filter-nya dengan query list.
			// Ini mencegah paging menampilkan total ada, tetapi isi grid kosong.
			// ======================================================
			Criteria countCriteria = session.createCriteria(PembayaranSiswaDetail.class)
					.createAlias("pembayaranSiswa", "pembayaranSiswa")
					.createAlias("itemBiayaSekolah", "itemBiayaSekolah", Criteria.LEFT_JOIN)
					.createAlias("tagihan", "tagihan", Criteria.LEFT_JOIN)
					.createAlias("tagihan.pengaturanBiaya", "pengaturanBiaya", Criteria.LEFT_JOIN)
					.add(filterPemilik)
					.setProjection(Projections.rowCount());

			Number totalData = (Number) countCriteria.uniqueResult();
			int totalSize = totalData == null ? 0 : totalData.intValue();

			paging.setTotalSize(totalSize);

			if (totalSize <= 0) {
				MyFormRow rowKosong = new MyFormRow();
				rowKosong.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(rowKosong, "5");
				rowKosong.appendChild(new Label(ais.common.Common.getBahasaConfig("Belum ada riwayat pembayaran.")));

				return;
			}

			// Pastikan activePage tidak melewati halaman terakhir
			int pageSize = paging.getPageSize();
			int maxPage = (int) Math.ceil((double) totalSize / (double) pageSize) - 1;

			if (maxPage < 0) {
				maxPage = 0;
			}

			if (activePage > maxPage) {
				activePage = maxPage;
				paging.setActivePage(activePage);
			}

			if (activePage < 0) {
				activePage = 0;
				paging.setActivePage(activePage);
			}

			// ======================================================
			// LIST QUERY
			// Struktur join/filter disamakan dengan countCriteria.
			// itemBiayaSekolah dibuat LEFT_JOIN agar detail yang tidak punya
			// item langsung tetap bisa tampil.
			// ======================================================
			Criteria crit = session.createCriteria(PembayaranSiswaDetail.class)
					.createAlias("pembayaranSiswa", "pembayaranSiswa")
					.createAlias("itemBiayaSekolah", "itemBiayaSekolah", Criteria.LEFT_JOIN)
					.createAlias("tagihan", "tagihan", Criteria.LEFT_JOIN)
					.createAlias("tagihan.pengaturanBiaya", "pengaturanBiaya", Criteria.LEFT_JOIN)
					.add(filterPemilik)
					.addOrder(Order.desc("pengaturanBiaya.id"))
					.addOrder(Order.asc("itemBiayaSekolah.nama"))
					.addOrder(Order.asc("tagihan.tahunbulan"))
					.addOrder(Order.asc("tagihan.bayarKe"))
					.addOrder(Order.desc("id"));

			crit.setFirstResult(activePage * pageSize);
			crit.setMaxResults(pageSize);

			listPembayaranSiswaDetail = ConstantValues.simpleList(crit, PembayaranSiswaDetail.class);

			if (listPembayaranSiswaDetail == null || listPembayaranSiswaDetail.isEmpty()) {
				MyFormRow rowKosong = new MyFormRow();
				rowKosong.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(rowKosong, "5");
				rowKosong.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak ada riwayat pembayaran pada halaman ini.")));

				return;
			}

			Tbmuser tbmuser = Common.getCurrentUser();

			boolean bolehEdit = (tbmuser != null
					&& tbmuser.getMahasiswa() == null
					&& tbmuser.getSiswa() == null
					&& tbmuser.getCalonSiswa() == null)
					|| Common.getApakahAdmin();

			PengaturanBiaya pengaturanBiaya = null;

			for (final PembayaranSiswaDetail pDetail : listPembayaranSiswaDetail) {
				try {
					if (pDetail == null) {
						continue;
					}

					final PembayaranSiswa pSiswa = pDetail.getPembayaranSiswa();
					Tagihan tagihan = pDetail.getTagihan();

					PengaturanBiaya pbAktif = null;

					if (tagihan != null) {
						try {
							pbAktif = tagihan.getPengaturanBiaya();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:1462");
						}
					}

					if (pbAktif != null
							&& (pengaturanBiaya == null || !pengaturanBiaya.getId().equals(pbAktif.getId()))) {
						new Group(pbAktif.toString()).setParent(rows);
						pengaturanBiaya = pbAktif;
					}

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);

					// =====================
					// KOLOM 1: WAKTU BAYAR
					// =====================
					try {
						String waktuBayar = "";

						if (pSiswa != null && pSiswa.getTanggal() != null) {
							waktuBayar = Common.dateFormat3.get().format(pSiswa.getTanggal());
						}

						Vbox vWaktu = RevisiHelper.createNewRevisi(PembayaranSiswaDetail.class, pDetail, waktuBayar);
						vWaktu.setParent(row);

						if (pSiswa != null && pSiswa.getVirtualAccountBank() == null && bolehEdit) {
							MyToolbarbuttonConfig btnEdit = new MyToolbarbuttonConfig("",
									"/img/svg/pencil-square.svg");
							btnEdit.setParent(vWaktu);
							btnEdit.setTooltiptext("Ubah data pembayaran");

							btnEdit.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									MyMessageboxConfig.show("Apakah Anda yakin ingin mengubah data pembayaran ini? Perubahan yang disimpan akan menggantikan data pembayaran sebelumnya.",
											"Pertanyaan",
											MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
											MyMessageboxConfig.QUESTION,
											new EventListener() {
												@Override
												public void onEvent(Event event) throws Exception {
													if (Integer.parseInt(event.getData().toString()) == MyMessageboxConfig.OK) {
														showEditPembayaranWindow(pSiswa, pDetail, s, cs, sub);
													}
												}
											});
								}
							});
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:1514");
						row.appendChild(new Label(""));
					}

					// =====================
					// KOLOM 2: BULAN/TAHUN
					// =====================
					String bulanTahun = "-";

					if (tagihan != null) {
						String namaBulan = "";

						if (tagihan.getBulan() != null
								&& tagihan.getBulan() >= 1
								&& tagihan.getBulan() <= Common.BULAN.length) {
							namaBulan = Common.BULAN[tagihan.getBulan() - 1] + " ";
						}

						String tahunText = tagihan.getTahun() == null ? "" : String.valueOf(tagihan.getTahun());

						bulanTahun = (namaBulan + tahunText).trim();

						if (bulanTahun.length() == 0) {
							bulanTahun = "-";
						}
					}

					org.zkoss.zul.Vbox vBulanId = new org.zkoss.zul.Vbox();
					vBulanId.setParent(row);
					vBulanId.appendChild(new Label(bulanTahun));
					if (tagihan != null && tagihan.getId() != null) {
						Label idTagihanLabel = new Label("ID: " + tagihan.getId());
						idTagihanLabel.setStyle("font-size:9px;color:#888;");
						vBulanId.appendChild(idTagihanLabel);
					}

					// =====================
					// KOLOM 3: ITEM
					// =====================
					StringBuilder ketB = new StringBuilder();

					if (tagihan != null) {
						try {
							if (tagihan.getNominalBiaya() != null
									&& tagihan.getNominalBiaya().getItemBiayaSekolah() != null
									&& tagihan.getNominalBiaya().getItemBiayaSekolah().getNama() != null) {
								ketB.append(tagihan.getNominalBiaya().getItemBiayaSekolah().getNama());
							} else if (pDetail.getItemBiayaSekolah() != null
									&& pDetail.getItemBiayaSekolah().getNama() != null) {
								ketB.append(pDetail.getItemBiayaSekolah().getNama());
							} else {
								ketB.append("Pembayaran");
							}

							if (tagihan.getNominalBiaya() != null
									&& tagihan.getNominalBiaya().getDibayarSebayak() != null
									&& tagihan.getNominalBiaya().getDibayarSebayak() > 1) {
								ketB.append(" (ke ").append(tagihan.getBayarKe()).append(")");
							}

							if (tagihan.getDenda() != null && tagihan.getDenda() > 0.01) {
								ketB.append(", Denda ").append(Common.numberFormat.get().format(tagihan.getDenda()));
							}

							if (tagihan.getTanggalDeadline() != null) {
								ketB.append(", Deadline ")
										.append(Common.dateFormat4.get().format(tagihan.getTanggalDeadline()));
							}

							DiskonSiswa diskonSiswaTagihan = null;
							try {
								diskonSiswaTagihan = tagihan.getDiskonSiswa();
							} catch (Exception e) {
								diskonSiswaTagihan = null;
								try {
									Common.tampilErrorJikaAdmin(e);
								} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PembayaranOnline.java:1590");
								}
							}
							if (diskonSiswaTagihan != null && diskonSiswaTagihan.getMemotongTagihan()) {
								ketB.append(" - ").append(diskonSiswaTagihan.getNama());
							}

							try {
								if (tagihan.getNominalBiaya() != null) {
									PengaturanBiayaItemBiaya pbItem = tagihan.getNominalBiaya()
											.getPengaturanBiayaItemBiaya();

									double diskonBiaya = 0.0;

									if (!(pbItem == null
											|| (tagihan.getNominalBiaya().getDibayarSebayak() != null
													&& tagihan.getNominalBiaya().getDibayarSebayak() > 1))) {
										diskonBiaya = pbItem.getDiskonBiaya();
									}

									if (diskonBiaya > 0.1) {
										ketB.append(", Diskon dibayar lunas 1x");
									}
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:1615");
							}

						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:1619");

							if (ketB.length() == 0) {
								ketB.append("Pembayaran");
							}
						}
					} else {
						// Data detail tanpa tagihan tetap ditampilkan
						if (pDetail.getItemBiayaSekolah() != null
								&& pDetail.getItemBiayaSekolah().getNama() != null) {
							ketB.append(pDetail.getItemBiayaSekolah().getNama());
						} else {
							ketB.append("Pembayaran / Deposit");
						}
					}

					row.appendChild(new Label(ketB.toString()));

					// =====================
					// KOLOM 4: VIA
					// =====================
					String via = "";

					try {
						if (pSiswa != null && pSiswa.getAkunPembayaranSiswa() != null) {
							via = pSiswa.getAkunPembayaranSiswa().getNama();
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:1647");
					}

					row.appendChild(new Label(via));

					// =====================
					// KOLOM 5: NOMINAL
					// =====================
					try {
						Double n = pDetail.ambilNominal();

						if (n == null) {
							n = 0.0;
						}

						if (tagihan != null) {
							Double diskon = tagihan.getDiskon() != null ? tagihan.getDiskon() : 0.0;
							Double denda = tagihan.getDenda() != null ? tagihan.getDenda() : 0.0;
							Double n1 = n + diskon + denda;

							if (diskon > 0.01 && n1 > n) {
								Vbox vbox = new Vbox();
								row.appendChild(vbox);

								RevisiHelper.createNewRevisi(Tagihan.class, tagihan,
										Common.numberFormat.get().format(n)).setParent(vbox);

								Label tagDiskon = new Label(Common.numberFormat.get().format(n1));
								tagDiskon.setParent(vbox);
								tagDiskon.setWidth("100%");
								tagDiskon.setStyle("text-decoration: line-through;");
							} else {
								RevisiHelper.createNewRevisi(Tagihan.class, tagihan,
										Common.numberFormat.get().format(n)).setParent(row);
							}
						} else {
							Label lblNominal = new Label(Common.numberFormat.get().format(n));
							lblNominal.setStyle("text-align:right; display:block; width:100%;");
							row.appendChild(lblNominal);
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:1688");
						row.appendChild(new Label("0"));
					}

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:1693");

					MyFormRow rowError = new MyFormRow();
					rowError.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(rowError, "5");
					rowError.appendChild(new Label(ais.common.Common.getBahasaConfig("Gagal menampilkan salah satu data pembayaran.")));
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:1703");

			MyFormRow rowError = new MyFormRow();
			rowError.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowError, "5");
			rowError.appendChild(new Label(ais.common.Common.getBahasaConfig("Terjadi kesalahan saat mengambil riwayat pembayaran.")));
		} finally {
			closeSessionAndDisconnect(session);
		}
	}
	
	
	
	private void showEditPembayaranWindow(final PembayaranSiswa pSiswa, final PembayaranSiswaDetail pDetail,
			final Siswa s, final CalonSiswa cs, final Row sub) {
		try {
			if (pSiswa == null || pDetail == null) {
				MyMessageboxConfig.show(
						"Data pembayaran tidak valid. Langkah yang dapat dilakukan: (1) tutup jendela ini lalu buka kembali data pembayaran; (2) pastikan data pembayaran dan rinciannya telah tersimpan dengan benar; (3) hubungi administrator apabila masalah masih berlanjut.",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			List<AkunPembayaranSiswa> akunList = null;
			Session ss = null;

			try {
				ss = HibernateUtil.openSession();

				akunList = ConstantValues.simpleList(
						ss.createCriteria(AkunPembayaranSiswa.class)
								.add(Restrictions.eq("sekolah", pSiswa.getSekolah()))
								.add(Restrictions.or(
										Restrictions.isNull("aktif"),
										Restrictions.eq("aktif", true)))
								.addOrder(Order.asc("nama")),
						AkunPembayaranSiswa.class);
			} finally {
				closeSessionAndDisconnect(ss);
			}

			final MyWindow win = new MyWindow("Ubah Data", "none", true);
			win.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			win.setHeight("250px");
			win.setWidth("500px");

			Borderlayout borderlayoutInner = new ais.ui.util.MyBorderlayout();
			borderlayoutInner.setParent(win);

			Center centerInner = new Center();
			centerInner.setParent(borderlayoutInner);
			ais.ui.util.ZkCompat.setFlex(centerInner, true);

			MyGrid gridInner = new MyGrid();
			gridInner.setWidth("100%");
			gridInner.setHeight("100%");
			gridInner.setParent(centerInner);

			Columns columnsInner = new Columns();
			columnsInner.setParent(gridInner);

			addCol(columnsInner, "", "30%", null);
			addCol(columnsInner, "", null, null);

			Rows rowsInner = new Rows();
			rowsInner.setParent(gridInner);

			final MyDatebox d = new MyDatebox(pSiswa.getTanggal());
			final MyDoublebox nominal = new MyDoublebox(pDetail.getNominal());
			final Combobox cbBayar = new Combobox();

			EventListener eventListenerSimpan = new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Session sUpdate = null;

					try {
						sUpdate = HibernateUtil.openSession();
						sUpdate.getTransaction().begin();

						PembayaranSiswa pSiswaUpdate = (PembayaranSiswa) sUpdate.get(PembayaranSiswa.class,
								pSiswa.getId());
						PembayaranSiswaDetail pDetailUpdate = (PembayaranSiswaDetail) sUpdate.get(
								PembayaranSiswaDetail.class, pDetail.getId());

						if (pSiswaUpdate == null || pDetailUpdate == null) {
							throw new Exception("Data pembayaran tidak ditemukan saat proses update.");
						}

						Double valNominal = nominal.getValue();
						pDetailUpdate.setNominalManual(valNominal != null ? valNominal : 0.0);

						Common.refreshUpdate(sUpdate, pDetailUpdate);

						Number total = (Number) sUpdate.createCriteria(PembayaranSiswaDetail.class)
								.add(Restrictions.eq("pembayaranSiswa", pSiswaUpdate))
								.setProjection(Projections.sum("nominal"))
								.uniqueResult();

						double totalValue = total == null ? 0.0 : total.doubleValue();

						pSiswaUpdate.setNominal(totalValue);
						pSiswaUpdate.setTambahanDeposit(totalValue);

						AkunPembayaranSiswa akunTerpilih = null;

						if (cbBayar.getSelectedItem() != null) {
							akunTerpilih = (AkunPembayaranSiswa) cbBayar.getSelectedItem().getValue();
						}

						pSiswaUpdate.setAkunPembayaranSiswa(akunTerpilih);
						pSiswaUpdate.setTanggal(d.getValue());

						Common.refreshUpdate(sUpdate, pSiswaUpdate);

						sUpdate.getTransaction().commit();

					} catch (Exception e) {
						if (sUpdate != null
								&& sUpdate.getTransaction() != null
								&& sUpdate.getTransaction().isActive()) {
							sUpdate.getTransaction().rollback();
						}

						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:1827");
						Common.tampilErrorJikaAdmin(e);
					} finally {
						closeSessionAndDisconnect(sUpdate);
					}
				}
			};

			MyFormRow rInner = new MyFormRow();
			rInner.setValign("top");
			rInner.setParent(rowsInner);
			rInner.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Bayar")));

			d.setReadonly(true);
			d.setFormat(Common.dateFormat3.get().toPattern());
			d.setWidth("95%");
			rInner.appendChild(d);
			d.addEventListener("onChange", eventListenerSimpan);

			rInner = new MyFormRow();
			rInner.setValign("top");
			rInner.setParent(rowsInner);
			rInner.appendChild(new Label(ais.common.Common.getBahasaConfig("Nominal Bayar")));

			nominal.setWidth("95%");
			rInner.appendChild(nominal);
			nominal.addEventListener("onChange", eventListenerSimpan);

			rInner = new MyFormRow();
			rInner.setValign("top");
			rInner.setParent(rowsInner);
			rInner.appendChild(new Label(ais.common.Common.getBahasaConfig("Cara Bayar")));

			cbBayar.setParent(rInner);
			cbBayar.setWidth("95%");

			if (akunList != null) {
				Common.insertComboItems(cbBayar, "nama", "keterangan", akunList);
			}

			cbBayar.setReadonly(true);
			Common.selectComboItem(true, cbBayar, pSiswa.getAkunPembayaranSiswa());
			cbBayar.addEventListener("onChange", eventListenerSimpan);

			South southInner = new South();
			ais.ui.util.ZkCompat.setFlex(southInner, true);
			southInner.setParent(borderlayoutInner);

			Toolbar tb = new Toolbar();
			tb.setParent(southInner);

			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					win.detach();
					reloadRiwayatPembayaran(s, cs, sub);
				}
			});
			cancel.setParent(tb);

			win.setVisible(true);
			win.onModal();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void reloadTagihan() {
		reloadTagihan(false);
	}

	/**
	 * Event listener lokal milik {@link PembayaranOnline}. Kelas ini menangani event untuk komponen induk dan
	 * meneruskan pekerjaan domain ke method/service yang sudah tersedia.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PembayaranOnline} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini merupakan detail
	 * implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String ipConfigKey}, {@code String
	 * prefixBankLainKey}, {@code String labelBtn}, {@code Map param}, {@code boolean isBankLainOnline}; operasi
	 * lokal: {@code onEvent()}, {@code getBiayaAdministrasi()}, {@code executePayment()}, {@code
	 * executeDownload()}, {@code handleSuccess}(). Aturan bisnis bersama tetap berada pada kelas induk atau
	 * service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PembayaranOnline
	 */
	private abstract class BaseOnlinePaymentListener implements EventListener {
		protected String ipConfigKey, prefixBankLainKey, labelBtn;
		protected Map<String, Object> param;
		protected boolean isBankLainOnline;

		public BaseOnlinePaymentListener(String labelBtn, String ipConfigKey, Map<String, Object> param,
				boolean isBankLainOnline, String prefixBankLainKey) {
			this.labelBtn = labelBtn;
			this.ipConfigKey = ipConfigKey;
			this.param = param;
			this.isBankLainOnline = isBankLainOnline;
			this.prefixBankLainKey = prefixBankLainKey;
		}

		@Override
		public void onEvent(Event event) throws Exception {
			if (!checkKondisiSebelumbayar())
				return;
			final double adminFee = getBiayaAdministrasi();
			confirmAndExecute(labelBtn, adminFee, new PaymentAction() {
				@Override
				public void execute() throws Exception {
					executePayment(getSiswaLokal(), getCalonSiswaLokal(), adminFee);
				}
			});
		}

		protected abstract double getBiayaAdministrasi();

		protected void executePayment(Siswa s, CalonSiswa cs, double biayaAdministrasi) throws Exception {
			BankHost bankHost = PembayaranUtil.getInstance()
					.getBankHost(Common.getKonfigurasi(ipConfigKey, "").getNilai(), "Bank Host");
			Sekolah sek = s != null ? s.getSekolah() : cs.getSekolah();
			Session session = null;
			AkunPembayaranSiswa akun = null;
			try {
				session = HibernateUtil.openSession();
				akun = (AkunPembayaranSiswa) ConstantValues
						.simpleObject(
								session.createCriteria(AkunPembayaranSiswa.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("manual", false)).add(Restrictions.eq("sekolah", sek))
										.setMaxResults(1),
								AkunPembayaranSiswa.class);
			} finally {
				closeSessionAndDisconnect(session);
			}

			VirtualAccountBank va = executeDownload(s, cs, biayaAdministrasi, bankHost, akun, sek);
			handleSuccess(va, s, cs, biayaAdministrasi, sek);
			reloadTagihan();
		}

		protected VirtualAccountBank executeDownload(Siswa s, CalonSiswa cs, double biayaAdministrasi,
				BankHost bankHost, AkunPembayaranSiswa akun, Sekolah sek) throws Exception {
			return DownloadTagihanSiswaBankOnline.downloadData(s, cs, tagihans.values(), param, biayaAdministrasi,
					sisaTabungan.isChecked() ? tabungan : null, getNilaiTopupAktif(), bankHost, akun, sek);
		}

		protected abstract void handleSuccess(VirtualAccountBank va, Siswa s, CalonSiswa cs, double biayaAdministrasi,
				Sekolah sek) throws Exception;
	}

	private void createStandardPopupGatewayAction(Hbox spaceBayar, String label, final String ipKey,
			final Map<String, Object> param, final boolean isBankLainOnline, final String prefixKey,
			final String urlPattern, final String adminFeeConfig) {
		MyButtonConfig btn = new MyButtonConfig(label);
		spaceBayar.appendChild(btn);
		btn.addEventListener("onClick",
				new BaseOnlinePaymentListener(label, ipKey, param, isBankLainOnline, prefixKey) {
					@Override
					protected double getBiayaAdministrasi() {
						Object onlineBmtFee = param.get("onlineBmtAdministrationFee");
						if (Boolean.TRUE.equals(param.get(OnlineBmtUtil.PARAM_KEY)) && onlineBmtFee instanceof Number) {
							return ((Number) onlineBmtFee).doubleValue();
						}
						try {
							return Double.parseDouble(Common.getKonfigurasi(adminFeeConfig, "0.0").getNilai());
						} catch (Exception e) {
							return 0.0;
						}
					}

					@Override
					protected void handleSuccess(VirtualAccountBank va, Siswa s, CalonSiswa cs,
							double biayaAdministrasi, Sekolah sek) throws Exception {
						if (va != null && va.getId() != null) {
							String code = va.getKode(), nama = s != null ? s.getNama() : cs.getNama(),
									kodebankLainOnline = "";
							File fBarcode = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + va.getId() + ".png");
							if (isBankLainOnline) {
								kodebankLainOnline = Common.getKonfigurasi(prefixKey, "").getNilai();
								if (!kodebankLainOnline.trim().isEmpty()) {
									if (va.getKanalPembayaran() != null
											&& va.getKanalPembayaran().getBsiUsername() != null
											&& !va.getKanalPembayaran().getBsiUsername().isEmpty())
										code = va.getKanalPembayaran().getBsiUsername() + code;
									else if (param.containsKey("maja") && sek.getBsiUsername() != null)
										code = sek.getBsiUsername() + code;
									kodebankLainOnline += code;
								}
							}
							BarcodeCommon.generateCRCode(code, fBarcode);
							String myUrl = urlPattern + "?va=" + URLEncoder.encode(code, "UTF-8") + "&nominal="
									+ URLEncoder
											.encode("Rp. " + Common.numberFormat.get().format(va.getTotal()), "UTF-8")
									+ "&biayaAdministrasi="
									+ URLEncoder.encode(
											"Rp. " + Common.numberFormat.get().format(biayaAdministrasi), "UTF-8")
									+ "&nama=" + URLEncoder.encode(nama, "UTF-8") + "&kadalurasa="
									+ (va.getKadaluarsa() == null ? ""
											: URLEncoder.encode(
													Common.dateFormat.get().format(va.getKadaluarsa()), "UTF-8"))
									+ "&biayaTotal="
									+ URLEncoder.encode("Rp. "
											+ Common.numberFormat.get().format(va.getTotal() + biayaAdministrasi),
											"UTF-8")
									+ "&qr="
									+ URLEncoder.encode(
											Common.getRequestHostWithProtocol() + "/report/" + fBarcode.getName(),
											"UTF-8")
									+ "&terbilang="
									+ URLEncoder.encode(
											IndonesianNumberToWords.convert((long) (va.getTotal() + biayaAdministrasi)),
											"UTF-8")
									+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1)
									+ (kodebankLainOnline.trim().isEmpty() ? ""
											: "&kodeBankLain=" + URLEncoder.encode(kodebankLainOnline, "UTF-8"));
							Common.displayWindow(myUrl, true, "75%");
						} else
							MyMessageboxConfig.show("Transaksi gagal dilakukan. Langkah yang dapat dilakukan: (1) periksa kembali koneksi jaringan Anda; (2) ulangi proses pembayaran beberapa saat lagi; (3) hubungi administrator apabila transaksi tetap gagal.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
					}
				});
	}

	private void createDirectBankButton(Hbox spaceBayar, final int type, final String btnLabel, String cfgPrefix) {
		boolean aktif = Common.bolehKonfigurasi("aktifkan_pembayaran_via_" + cfgPrefix, Konfigurasi.TIDAK_AKTIF);
		Sekolah sek = getSiswaLokal() != null ? getSiswaLokal().getSekolah()
				: (getCalonSiswaLokal() != null ? getCalonSiswaLokal().getSekolah() : SekolahUtil.getSekolah());
		if (type == 2 || type == 3 || type == 4)
			aktif = aktif
					&& Common
							.getKonfigurasi("aktifkan_pembayaran_via_" + cfgPrefix + "_sekolah_"
									+ (sek == null ? "" : sek.getId()), Konfigurasi.AKTIF)
							.getNilai().equals(Konfigurasi.AKTIF);

		if (aktif) {
			if (type == 1 && tabBri != null) {
				tabBri.setVisible(true);
				tabpanelBri.setVisible(true);
			} else if (type == 2 && tabBni != null) {
				tabBni.setVisible(true);
				tabpanelBni.setVisible(true);
			} else if (type == 3 && tabBsi != null) {
				tabBsi.setVisible(true);
				tabpanelBsi.setVisible(true);
			} else if (type == 4 && tabOnline != null) {
				tabOnline.setVisible(true);
				tabpanelOnline.setVisible(true);
			}

			MyButtonConfig btn = type == 1 ? BriCommon.createButton()
					: type == 2 ? BniCommon.createButton()
							: type == 3 ? BsiCommon.createButton() : new MyButtonConfig(btnLabel);
			spaceBayar.appendChild(btn);

			if (type != 4) {
				final String fCfgPrefix = cfgPrefix;
				btn.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						if (!checkKondisiSebelumbayar())
							return;
						double adminFeeLocal = 0.0;
						try {
							adminFeeLocal = Double.parseDouble(
									Common.getKonfigurasi(fCfgPrefix + "_biaya_administrasi", "0.0").getNilai());
						} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PembayaranOnline.java:2076");
						}

						final double adminFee = adminFeeLocal;
						confirmAndExecute(btnLabel, adminFee, new PaymentAction() {
							@Override
							public void execute() throws Exception {
								double dep = deposit == null || deposit.isDisabled() || deposit.getValue() == null ? 0.0
										: deposit.getValue();
								if (type == 1)
									BriCommon.onSaveBri(getSiswaLokal(), getCalonSiswaLokal(), tagihans.values(), t,
											true, dep);
								else if (type == 2)
									BniCommon.onSaveBni(getSiswaLokal(), getCalonSiswaLokal(), tagihans.values(), t,
											true, dep);
								else if (type == 3)
									BsiCommon.onSaveBsi(getSiswaLokal(), getCalonSiswaLokal(), tagihans.values(), t,
											true, dep);
								reloadTagihan(false);
							}
						});
					}
				});
			} else {
				btn.addEventListener("onClick", new BaseOnlinePaymentListener(btnLabel, "online_bank_host_ip",
						new HashMap<String, Object>(), false, "") {
					@Override
					protected double getBiayaAdministrasi() {
						try {
							return Double
									.parseDouble(Common.getKonfigurasi("btn_biaya_administrasi", "0.0").getNilai());
						} catch (Exception e) {
							return 0.0;
						}
					}

					@Override
					protected VirtualAccountBank executeDownload(Siswa s, CalonSiswa cs, double admin, BankHost bh,
							AkunPembayaranSiswa akun, Sekolah sek) throws Exception {
						return DownloadTagihanMahasiswaBankBtn.downloadData(s, cs, tagihans.values(), false, admin, bh,
								akun, sek, getNilaiTopupAktif());
					}

					@Override
					protected void handleSuccess(VirtualAccountBank va, Siswa s, CalonSiswa cs, double admin,
							Sekolah sek) throws Exception {
						if (va != null && va.getId() != null) {
							File fBarcode = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + va.getId() + ".png");
							BarcodeCommon.generateCRCode(va.getKode(), fBarcode);
							String myUrl = "/common/btn/no_va.zul?va=" + URLEncoder.encode(va.getKode(), "UTF-8")
									+ "&nominal="
									+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(va.getTotal()),
											"UTF-8")
									+ "&biayaAdministrasi="
									+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(admin), "UTF-8")
									+ "&nama=" + URLEncoder.encode(s != null ? s.getNama() : cs.getNama(), "UTF-8")
									+ "&kadalurasa="
									+ (va.getKadaluarsa() == null ? ""
											: URLEncoder.encode(Common.dateFormat.get().format(va.getKadaluarsa()),
													"UTF-8"))
									+ "&biayaTotal="
									+ URLEncoder.encode(
											"Rp. " + Common.numberFormat.get().format(va.getTotal() + admin), "UTF-8")
									+ "&qr="
									+ URLEncoder.encode(
											Common.getRequestHostWithProtocol() + "/report/" + fBarcode.getName(),
											"UTF-8")
									+ "&terbilang="
									+ URLEncoder.encode(IndonesianNumberToWords.convert((long) (va.getTotal() + admin)),
											"UTF-8")
									+ "&tampilBiayaAdministrasi=" + (admin > 0.1);
							Common.displayWindow(myUrl, true, "75%");
						} else
							MyMessageboxConfig.show("Transaksi gagal dilakukan. Langkah yang dapat dilakukan: (1) periksa kembali koneksi jaringan Anda; (2) ulangi proses pembayaran beberapa saat lagi; (3) hubungi administrator apabila transaksi tetap gagal.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
					}
				});
			}
		}
	}

	private void registerOnlineGateway(Hbox spaceBayar, String btnLabel, final String paramKey, final double adminFee,
			final Sekolah sek, final int popupType, final boolean isSmartlinkCheck) {
		MyButtonConfig btn = new MyButtonConfig(btnLabel);
		spaceBayar.appendChild(btn);
		final Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put(paramKey, true);

		if (langsungBayar) {
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Siswa ls = getSiswaLokal();
					CalonSiswa lcs = getCalonSiswaLokal();
					if ((ls == null && lcs == null) || (tagihans.isEmpty() && !(deposit != null
							&& deposit.getValue() != null && deposit.getValue() > 0.1 && !deposit.isDisabled())))
						return;
					BankHost bankHost = PembayaranUtil.getInstance()
							.getBankHost(Common.getKonfigurasi("online_bank_host_ip", "").getNilai(), "Bank Host");
					Sekolah sekL = ls != null ? ls.getSekolah() : lcs.getSekolah();
					Session sess = null;
					AkunPembayaranSiswa akun = null;
					try {
						sess = HibernateUtil.openSession();
						akun = (AkunPembayaranSiswa) ConstantValues.simpleObject(
								sess.createCriteria(AkunPembayaranSiswa.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("manual", false)).add(Restrictions.eq("sekolah", sekL))
										.setMaxResults(1),
								AkunPembayaranSiswa.class);
					} finally {
						closeSessionAndDisconnect(sess);
					}
					VirtualAccountBank va = DownloadTagihanSiswaBankOnline.downloadData(ls, lcs, tagihans.values(),
							paramMap, adminFee, sisaTabungan.isChecked() ? tabungan : null, getNilaiTopupAktif(),
							bankHost, akun, sekL);
					showGatewayPopup(va, popupType);
				}
			}, "", false, 1000);
		}

		btn.addEventListener("onClick",
				new BaseOnlinePaymentListener(btnLabel, "online_bank_host_ip", paramMap, false, "") {
					@Override
					protected double getBiayaAdministrasi() {
						return adminFee;
					}

					@Override
					protected void handleSuccess(VirtualAccountBank va, Siswa s, CalonSiswa cs, double admin,
							Sekolah sekL) throws Exception {
						if (va != null && !va.getLink().isEmpty())
							showGatewayPopup(va, popupType);
						else if (!isSmartlinkCheck || sekL.getVariableBiayaAdminEsmartlink().isEmpty())
							MyMessageboxConfig.show("Transaksi gagal dilakukan. Langkah yang dapat dilakukan: (1) periksa kembali koneksi jaringan Anda; (2) ulangi proses pembayaran beberapa saat lagi; (3) hubungi administrator apabila transaksi tetap gagal.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
					}

					@Override
					public void onEvent(Event event) throws Exception {
						if (isSmartlinkCheck) {
							if (!checkKondisiSebelumbayar())
								return;
							Sekolah sekL = getSiswaLokal() != null ? getSiswaLokal().getSekolah()
									: getCalonSiswaLokal().getSekolah();
							if (!sekL.getVariableBiayaAdminEsmartlink().isEmpty()) {
								Common.createDefaultTimer(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										Map<String, Object> pUpd = new HashMap<String, Object>();
										pUpd.put(paramKey, true);
										pUpd.put("update", true);
										Siswa ls = getSiswaLokal();
										CalonSiswa lcs = getCalonSiswaLokal();
										Sekolah sk = ls != null ? ls.getSekolah() : lcs.getSekolah();
										BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
												Common.getKonfigurasi("online_bank_host_ip", "").getNilai(),
												"Bank Host");
										Session sess = null;
										AkunPembayaranSiswa akun = null;
										try {
											sess = HibernateUtil.openSession();
											akun = (AkunPembayaranSiswa) ConstantValues.simpleObject(
													sess.createCriteria(AkunPembayaranSiswa.class)
															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true)))
															.add(Restrictions.eq("manual", false))
															.add(Restrictions.eq("sekolah", sk)).setMaxResults(1),
													AkunPembayaranSiswa.class);
										} finally {
											closeSessionAndDisconnect(sess);
										}
										VirtualAccountBank va = DownloadTagihanSiswaBankOnline.downloadData(ls, lcs,
												tagihans.values(), pUpd, adminFee,
												sisaTabungan.isChecked() ? tabungan : null, getNilaiTopupAktif(), bankHost,
												akun, sk);
										showGatewayPopup(va, popupType);
									}
								}, "Proses pembayaran ..");
								return;
							}
						}
						super.onEvent(event);
					}
				});
	}

	private void showGatewayPopup(VirtualAccountBank va, int popupType) throws Exception {
		if (va == null || va.getLink().isEmpty())
			return;
		if (popupType == 1)
			Clients.evalJavaScript("popupCenter({url: '" + Common.jsEscape(va.getLink()) + "', title: 'Book', w: 1200, h: 600});");
		else if (popupType == 2)
			Common.displayWindowIframe(va.getLink(), true, "600px", "95%", "Pembayaran Online");
		else
			ExecutionsCtrl.getCurrent().sendRedirect(va.getLink(), "_blank");
	}

	private void reloadTagihan(final boolean refersh) {
		tagihans.clear();
		akunPembayaranPreview = null;
		pilihan.clear();
		deposit = null;
		Common.clear(colKiri);

		final Row sub = Common.tampilanScroll1(colKiri);
		sub.setStyle("border:0; padding:0; margin:0; overflow-y:auto; overflow-x:hidden; max-width:100%; box-sizing:border-box;");
		if (sub.getParent() instanceof HtmlBasedComponent) {
			appendStyle((HtmlBasedComponent) sub.getParent(), "overflow-y:auto; overflow-x:hidden; max-width:100%; box-sizing:border-box;");
		}
		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(sub);
		grid.setHeight("100%");
		grid.setStyle("border:0; margin:0; padding:0; overflow-y:auto; overflow-x:hidden; max-width:100%; box-sizing:border-box;");
		Columns columns = new Columns();
		columns.setParent(grid);
		addCol(columns, "Item Pembayaran", null, null);
		addCol(columns, "Bulan/Tahun", "17%", null);
		addCol(columns, "Tagihan", "35%", "right");
		addCol(columns, "Angsur", "5%", null);

		rowsDetailBiaya = new Rows();
		rowsDetailBiaya.setParent(grid);
		Foot foot = new Foot();
		foot.setParent(grid);
		Footer footer;
		foot.appendChild(footer = new Footer());
		footer.appendChild(new MyLabelBold("Total Transaksi"));
		footer = new Footer();
		footer.setAlign("right");
		foot.appendChild(footer);
		footer = new Footer();
		footer.setAlign("right");
		foot.appendChild(footer);

		Vbox vboxData = new Vbox();
		vboxData.setAlign("end");
		vboxData.setPack("end");
		vboxData.setWidth("100%");
		footer.appendChild(vboxData);
		vboxData.appendChild(totalTagihan = new MyLabelBold("0"));
		vboxData.appendChild(terbilang = new MyLabelBold(""));
		totalTagihan.setStyle("text-align:right;");

		// Tombol bayar di-CENTER: bungkus dalam Div lebar penuh, Hbox dibiarkan
		// menyusut sesuai isi lalu di-tengah-kan dengan margin auto. (pack center
		// pada Hbox saja kurang andal di dalam grid sehingga tombol tampak kiri.)
		// Rows HANYA menerima Row — Div tidak boleh di-parent langsung ke Rows
		// (sebelumnya centerBayar di-set ke sub.getParent() = Rows → ClassCastException).
		// Jadi tombol bayar ditaruh di Row tersendiri di bawah konten.
		Row rowBayar = new Row();
		rowBayar.setStyle("border:0; padding:0; margin:0;");
		rowBayar.setParent(sub.getParent());
		org.zkoss.zul.Div centerBayar = new org.zkoss.zul.Div();
		centerBayar.setWidth("100%");
		centerBayar.setStyle("text-align:center; padding:6px 0;");
		centerBayar.setParent(rowBayar);
		Hbox spaceBayar = new Hbox();
		spaceBayar.setParent(centerBayar);
		spaceBayar.setAlign("center");
		spaceBayar.setPack("center");
		spaceBayar.setSclass("ais-bayar-gateway-area");
		spaceBayar.setStyle("margin-left:auto; margin-right:auto;");

		Tbmuser tbmuser = Common.getCurrentUser();
		final Siswa s_lokal = getSiswaLokal();
		final CalonSiswa cs_lokal = getCalonSiswaLokal();

		try {
			if (cs_lokal != null && cs_lokal.getPenjurusanSekolah() == null && cs_lokal.getSekolah() != null
					&& cs_lokal.getSekolah().getPenjurusanWajibDipilih()) {
				MyMessageboxConfig.showFormat(
						"Penjurusan calon siswa atas nama {V1} belum ditentukan. Langkah yang dapat dilakukan: (1) buka menu Pendataan Calon Siswa; (2) tentukan penjurusan untuk siswa yang bersangkutan; (3) ulangi proses pembayaran setelah penjurusan tersimpan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, cs_lokal.getNama());
				return;
			}
			if (s_lokal != null && s_lokal.getPenjurusanSekolah() == null && s_lokal.getSekolah() != null
					&& s_lokal.getSekolah().getPenjurusanWajibDipilih()) {
				MyMessageboxConfig.showFormat(
						"Penjurusan siswa atas nama {V1} belum ditentukan. Langkah yang dapat dilakukan: (1) buka menu Pendataan Siswa; (2) tentukan penjurusan untuk siswa yang bersangkutan; (3) ulangi proses pembayaran setelah penjurusan tersimpan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, s_lokal.getNama());
				return;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PembayaranOnline.java:2359");
		}

		Sekolah sekolah_lokal = s_lokal != null && s_lokal.getSekolah() != null ? s_lokal.getSekolah()
				: cs_lokal != null && cs_lokal.getSekolah() != null ? cs_lokal.getSekolah() : SekolahUtil.getSekolah();
		if (sekolah_lokal != null && sekolah_lokal.getId() == null)
			sekolah_lokal = null;

		tabungan = ais.action.master.sekolah.util.DepositHelper.hitungDeposit(s_lokal, cs_lokal);
		labelTabungan.setVisible(tabungan > 0.1);
		labelTabungan.setValue("Tabungan : " + Common.numberFormat.get().format(tabungan));
		sisaTabungan.setVisible(tabungan > 0.1);
		if (modeTopupSiswa) {
			sisaTabungan.setChecked(false);
			sisaTabungan.setVisible(false);
		}

		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getOrangTua() == null && tbmuser.getCalonSiswa() == null && sekolah_lokal != null) {
			Session sTunai = null;
			List<AkunPembayaranSiswa> akunList = null;
			try {
				sTunai = HibernateUtil.openSession();
				akunList = ConstantValues
						.simpleList(
								sTunai.createCriteria(AkunPembayaranSiswa.class)
										.add(Restrictions.or(Restrictions.eq("dariTabungan", true),
												Restrictions.eq("manual", true)))
										.add(Restrictions.eq("sekolah", sekolah_lokal))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.addOrder(Order.asc("nama")),
								AkunPembayaranSiswa.class);
			} finally {
				closeSessionAndDisconnect(sTunai);
			}

			if (akunList != null) {
				for (final AkunPembayaranSiswa akun : akunList) {
					if (!akun.getDariTabungan() || (tabungan > 0.1 && akun.getDariTabungan())) {
						MyButtonConfig btnTunai = new MyButtonConfig("Bayar via " + akun.getNama(),
								"/img/svg/payments.svg");
						spaceBayar.appendChild(btnTunai);
						btnTunai.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								if (!checkKondisiSebelumbayar())
									return;
								double biayaAdministrasi = 0.0;
								try {
									biayaAdministrasi = Double.parseDouble(Common
											.getKonfigurasi(akun.getId() + "_biaya_administrasi", "0.0").getNilai());
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PembayaranOnline.java:2407");
								}
								if (akun.getDariTabungan() && ((t + biayaAdministrasi) > tabungan)) {
									MyMessageboxConfig.showFormat(
											"Nilai tabungan tidak mencukupi untuk melakukan pembayaran. Pembayaran via tabungan sebesar {V1}, sementara sisa deposit hanya {V2}. Langkah yang dapat dilakukan: (1) periksa kembali nominal pembayaran yang dipilih; (2) lakukan penambahan saldo tabungan terlebih dahulu; (3) gunakan metode pembayaran lain apabila diperlukan.",
											"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
											Common.numberFormat.get().format((t + biayaAdministrasi)),
											Common.numberFormat.get().format(tabungan));
									return;
								}
								akunPembayaranPreview = akun;
								refreshPanelJurnalPembayaranOnline();
								confirmAndExecute(((MyButtonConfig) event.getTarget()).getLabel(), biayaAdministrasi,
										new PaymentAction() {
											@Override
											public void execute() throws Exception {
												final Double nilaiTopup = getNilaiTopupAktif();
												if (modeTopupSiswa && tagihans.isEmpty()) {
													final DepositSiswa topup = TunaiSiswaCommon.onSaveTopup(s_lokal,
															cs_lokal, nilaiTopup, Common.getCurrentUser().getUserNama(), akun,
															tanggalTransaski == null ? WaktuUtil.getDate()
																	: tanggalTransaski.getValue());
													if (topup == null) {
														return;
													}
													Common.createDefaultTimer(new EventListener() {
														@Override
														public void onEvent(Event arg0) throws Exception {
															PembayaranSiswaUtil.cetakDeposit(topup);
															reloadTagihan();
														}
													});
													return;
												}
												final PembayaranSiswa pemb = TunaiSiswaCommon.onSave(s_lokal, cs_lokal,
														tagihans.values(),
														nilaiTopup,
														sisaTabungan.isChecked() ? tabungan : null,
														Common.getCurrentUser().getUserNama(), akun, rowsDetailBiaya,
														tanggalTransaski == null ? WaktuUtil.getDate()
																: tanggalTransaski.getValue());
												if (pemb == null) {
													return;
												}
												Common.createDefaultTimer(new EventListener() {
													@Override
													public void onEvent(Event arg0) throws Exception {
														hitungUlangTagihan();
														Common.createDefaultTimer(new EventListener() {
															@Override
															public void onEvent(Event arg0) throws Exception {
																PembayaranSiswaUtil.cetakStruk(pemb);
																reloadTagihan();
															}
														});
													}
												});
											}
										});
							}
						});
					}
				}
			}
		}

		createDirectBankButton(spaceBayar, 1, "BAYAR VIA BRI", "bri");
		createDirectBankButton(spaceBayar, 2, "BAYAR VIA BNI", "bni");
		createDirectBankButton(spaceBayar, 3, "BAYAR VIA BSI", "bsi");
		createDirectBankButton(spaceBayar, 4, "BAYAR VIA BTN", "bank_btn");

		if (sekolah_lokal != null) {
			if (OnlineBmtUtil.isSekolahReady(sekolah_lokal, sekolah_lokal.getKanalPembayaran())) {
				Map<String, Object> paramOnlineBmt = new HashMap<String, Object>();
				paramOnlineBmt.put(OnlineBmtUtil.PARAM_KEY, true);
				paramOnlineBmt.put("onlineBmtAdministrationFee", OnlineBmtUtil
						.resolveSettings(sekolah_lokal, sekolah_lokal.getKanalPembayaran()).getAdministrationFee());
				createStandardPopupGatewayAction(spaceBayar, "BAYAR VIA ONLINE BMT", "online_bank_host_ip",
						paramOnlineBmt, false, null, "/common/online/no_va.zul",
						Konfigurasi.ONLINE_BMT_BIAYA_ADMINISTRASI);
				if (tabOnline != null) {
					tabOnline.setVisible(true);
					tabpanelOnline.setVisible(true);
				}
			}
			if (sekolah_lokal.getAktfkanPembayaranViaFlip()) {
				if (tabOnline != null) {
					tabOnline.setVisible(true);
					tabpanelOnline.setVisible(true);
				}
				registerOnlineGateway(spaceBayar, "BAYAR VIA FLIP", "flip", sekolah_lokal.getBiayaAdminFlip(),
						sekolah_lokal, 1, false);
			}
			if (sekolah_lokal.getAktfkanPembayaranViaEsmartlink())
				registerOnlineGateway(spaceBayar, "BAYAR VIA SMART LINK", "esmartlink",
						sekolah_lokal.getBiayaAdminEsmartlink(), sekolah_lokal, 2, true);
			if (sekolah_lokal.getAktfkanPembayaranViaFinpay())
				registerOnlineGateway(spaceBayar, "BAYAR VIA FINPAY", "finpay", sekolah_lokal.getBiayaAdminFinpay(),
						sekolah_lokal, 3, false);
			if (sekolah_lokal.getAktfkanBjbSyariah())
				registerOnlineGateway(spaceBayar, "BAYAR VIA BJB SYARIAH", "bjb_langsung",
						sekolah_lokal.getBiayaAdminBjbSyariah(), sekolah_lokal, 2, false);
		}

		if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_maja", Konfigurasi.TIDAK_AKTIF)
				&& Common
						.getKonfigurasi("aktifkan_pembayaran_via_maja_sekolah_"
								+ (sekolah_lokal == null ? "" : sekolah_lokal.getId()), Konfigurasi.AKTIF)
						.getNilai().equals(Konfigurasi.AKTIF)) {
			Map<String, Object> paramMaja = new HashMap<String, Object>();
			paramMaja.put("maja", true);
			createStandardPopupGatewayAction(spaceBayar, "BAYAR VIA BSI", "online_bank_host_ip", paramMaja, true,
					"prefix_kode_bank_lain_online", "/common/online/no_va.zul", "online_biaya_maja");
		}

		if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online", Konfigurasi.TIDAK_AKTIF)
				&& Common
						.getKonfigurasi("aktifkan_pembayaran_via_bank_online_sekolah_"
								+ (sekolah_lokal == null ? "" : sekolah_lokal.getId()), Konfigurasi.AKTIF)
						.getNilai().equals(Konfigurasi.AKTIF)) {
			createStandardPopupGatewayAction(spaceBayar, "BAYAR ONLINE SISWA", "online_bank_host_ip",
					new HashMap<String, Object>(), true, "prefix_kode_bank_lain_online", "/common/online/no_va.zul",
					"online_biaya_administrasi");
			if (tabOnline != null) {
				tabOnline.setVisible(true);
				tabpanelOnline.setVisible(true);
			}
		}

		if (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online_2", Konfigurasi.TIDAK_AKTIF)
				&& Common
						.getKonfigurasi("aktifkan_pembayaran_via_bank_online_sekolah_2_"
								+ (sekolah_lokal == null ? "" : sekolah_lokal.getId()), Konfigurasi.AKTIF)
						.getNilai().equals(Konfigurasi.AKTIF)) {
			createStandardPopupGatewayAction(spaceBayar, "BAYAR ONLINE 2 SISWA", "online_2_bank_host_ip",
					new HashMap<String, Object>(), true, "prefix_kode_bank_lain_online_2", "/common/online_2/no_va.zul",
					"online_2_biaya_administrasi");
			if (tabOnline != null) {
				tabOnline.setVisible(true);
				tabpanelOnline.setVisible(true);
			}
		}

		Common.clear(rowsDetailBiaya);

		if (s_lokal != null) {
			calonSiswa.setVisible(false);
			lblCalonSiswa.setVisible(false);
		} else if (cs_lokal != null) {
			siswa.setVisible(false);
			lblSiswa.setVisible(false);
		} else {
			calonSiswa.setVisible(true);
			lblCalonSiswa.setVisible(true);
			siswa.setVisible(true);
			lblSiswa.setVisible(true);
		}

		if (!tampilSiswa) {
			tabpanel3.setVisible(false);
			tabpanel3.getLinkedTab().setVisible(false);
			siswa.setVisible(false);
			lblSiswa.setVisible(false);
		}
		if (!tampilCalonSiswa) {
			tabpanel2.setVisible(false);
			tabpanel2.getLinkedTab().setVisible(false);
			calonSiswa.setVisible(false);
			lblCalonSiswa.setVisible(false);
		}

		Integer bln = (Integer) bulan.getSelectedItem().getValue();
		Integer thn = (Integer) tahun.getSelectedItem().getValue();

		prosesTampilPembayaranParalel(s_lokal, cs_lokal, bln, thn, refersh, sub);
	}

	private void prosesTampilPembayaranParalel(final Siswa s_lokal, final CalonSiswa cs_lokal, final Integer bln,
			final Integer thn, final boolean refresh, final Row sub) {
		if (s_lokal == null && cs_lokal == null) {
			hitungUlangTagihan();
			return;
		}
		if (modeTopupSiswa) {
			appendFormTopupSiswa(s_lokal, true);
			hitungUlangTagihan();
			return;
		}

		final List<PengaturanBiaya> allPBiayas = new ArrayList<PengaturanBiaya>();
		final List<Object[]> targetList = new ArrayList<Object[]>();

		Session session = null;
		try {
			session = HibernateUtil.openSession();

			CalonSiswa csDataToUse = cs_lokal;
			if (cs_lokal == null && s_lokal != null) {
				if (s_lokal.getCalonSiswa() != null) {
					csDataToUse = (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(),
							s_lokal.getCalonSiswa());
				} else {
					Long calSis = (Long) session.createCriteria(CalonSiswa.class)
							.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
							.setProjection(Projections.property("id"))
							.add(Restrictions.ilike("namaSiswa", s_lokal.getNamaSiswa()))
							.add(Restrictions.eq("tanggalLahir", s_lokal.getTanggalLahir())).setMaxResults(1)
							.addOrder(Order.desc("id")).addOrder(Order.desc("tahunMasuk")).uniqueResult();
					if (calSis != null) {
						// FIX duplicate-key (constraint siswa_calonsiswa_key): kriteria
						// pencarian (nama ilike + tanggal lahir) bisa cocok ke CalonSiswa
						// yang SUDAH terpaut ke Siswa lain (nama sama/mirip antar siswa
						// berbeda). Cek dulu apakah calSis sudah dipakai siswa lain
						// sebelum set+commit, agar tak melanggar unique constraint.
						Long pemilikLain = (Long) session.createCriteria(Siswa.class)
								.add(Restrictions.eq("calonSiswa", calSis))
								.add(Restrictions.ne("id", s_lokal.getId()))
								.setProjection(Projections.property("id")).setMaxResults(1).uniqueResult();

						if (pemilikLain == null) {
							session.refresh(s_lokal);
							s_lokal.setCalonSiswa(calSis);
							session.getTransaction().begin();
							session.update(s_lokal);
							session.getTransaction().commit();
							csDataToUse = (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(),
									s_lokal.getCalonSiswa());
						}
					}
				}
			}

			if (csDataToUse != null && s_lokal != null && cs_lokal == null) {
				List<PengaturanBiaya> pbCs = ConstantValues.simpleList(PengaturanBiaya
						.terapkanFilterPembayaran(session.createCriteria(PengaturanBiaya.class), null, csDataToUse)
						.addOrder(Order.desc("id")).addOrder(Order.desc("jenisBiayaSekolah.periode"))
						.addOrder(Order.asc("jenisBiayaSekolah.nama")), PengaturanBiaya.class);
				if (pbCs != null) {
					for (PengaturanBiaya pb : pbCs) {
						allPBiayas.add(pb);
						targetList.add(new Object[] { null, csDataToUse });
					}
				}
			}

			List<PengaturanBiaya> pbMain = ConstantValues.simpleList(PengaturanBiaya
					.terapkanFilterPembayaran(session.createCriteria(PengaturanBiaya.class), s_lokal, cs_lokal)
					.addOrder(Order.desc("id")).addOrder(Order.desc("jenisBiayaSekolah.periode"))
					.addOrder(Order.asc("jenisBiayaSekolah.nama")), PengaturanBiaya.class);
			if (pbMain != null) {
				for (PengaturanBiaya pb : pbMain) {
					allPBiayas.add(pb);
					targetList.add(new Object[] { s_lokal, cs_lokal });
				}
			}

			// FIX konkurensi (ConcurrentModificationException di BatchFetchQueue / "ResultSet is
			// closed" / "Session is closed!" pada TagihanUtil.kumpulkanSlotSudahLunas dipanggil dari
			// Runnable thread-pool di bawah): jenisBiayaSekolah adalah relasi @ManyToOne LAZY pada
			// PengaturanBiaya. Bila TIDAK diinisialisasi di sini (selagi "session" milik method ini
			// masih terbuka), setiap PengaturanBiaya membawa proxy JenisBiayaSekolah yang MASIH TERTAUT
			// ke Session ini. Karena banyak baris PengaturanBiaya sering menunjuk JenisBiayaSekolah yang
			// SAMA, Hibernate mengembalikan proxy instance yang SAMA PERSIS untuk semua baris tsb
			// (session-level cache). Setelah session ini ditutup di finally, puluhan Runnable paralel
			// (lihat executor.submit di bawah) memanggil pb.getJenisBiayaSekolah().getGunakanCalonSiswa()
			// secara BERSAMAAN dari banyak thread pool — bila proxy yang sama itu belum terinisialisasi,
			// beberapa thread akan memicu inisialisasi lazy proxy SECARA BERSAMAAN lewat Session yang
			// sama (walau sudah closed), merusak state internal Session tsb. Dengan memaksa inisialisasi
			// di sini (masih single-threaded, session masih hidup), setiap Runnable nanti hanya membaca
			// objek yang sudah lengkap tanpa pernah menyentuh Session ini lagi.
			for (PengaturanBiaya pbInit : allPBiayas) {
				try {
					if (pbInit != null) {
						org.hibernate.Hibernate.initialize(pbInit.getJenisBiayaSekolah());
					}
				} catch (Exception exInit) {
					ais.common.ErrorAuditUtil.record(exInit,
							"auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:init-jbs");
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:2615");
		} finally {
			closeSessionAndDisconnect(session);
		}

		if (allPBiayas.isEmpty()) {
			reloadRiwayatPembayaran(s_lokal, cs_lokal, sub);
			hitungUlangTagihan();
			return;
		}

		final int totalData = allPBiayas.size();
		final AtomicInteger completedCount = new AtomicInteger(0);
		final Map<Integer, List<Tagihan>> resultMap = new ConcurrentHashMap<Integer, List<Tagihan>>();

		final String PB_KEY = "tagihan_siswa_pb";
		KeuanganDashboardEnhanceUtil.showFloatingProgress(PB_KEY, "Memuat Tagihan",
				"Sistem sedang membaca pengaturan biaya, menghitung tagihan, bantuan/potongan, dan menyiapkan tombol pembayaran.",
				10);

		final ExecutorService executor = Executors.newFixedThreadPool(hitungJumlahWorkerTagihan(totalData));

		for (int i = 0; i < totalData; i++) {
			final int index = i;
			final PengaturanBiaya pb = allPBiayas.get(i);
			final Siswa targetSiswa = (Siswa) targetList.get(i)[0];
			final CalonSiswa targetCalonSiswa = (CalonSiswa) targetList.get(i)[1];
			final Long pbId = pb == null ? null : pb.getId();
			final Long targetSiswaId = targetSiswa == null ? null : targetSiswa.getId();
			final Long targetCalonSiswaId = targetCalonSiswa == null ? null : targetCalonSiswa.getId();

			executor.submit(new Runnable() {
				@Override
				public void run() {
					Session workerSession = null;
					try {
						/* Entity dari request/ZK tidak boleh dibawa ke worker. Muat ulang seluruh
						 * akar object pada native session khusus thread ini agar setiap proxy lazy
						 * mempunyai sesi yang masih hidup selama penghitungan tagihan. */
						workerSession = HibernateUtil.currentNativeSession();
						PengaturanBiaya pbWorker = pbId == null ? null
								: (PengaturanBiaya) workerSession.get(PengaturanBiaya.class, pbId);
						Siswa siswaWorker = targetSiswaId == null ? null
								: (Siswa) workerSession.get(Siswa.class, targetSiswaId);
						CalonSiswa calonWorker = targetCalonSiswaId == null ? null
								: (CalonSiswa) workerSession.get(CalonSiswa.class, targetCalonSiswaId);
						if (pbWorker == null) {
							return;
						}
						JenisBiayaSekolah jbs = pbWorker.getJenisBiayaSekolah();
						boolean isValid = false;
						if (pbWorker.getAktif()) {
							if (siswaWorker != null && !jbs.getGunakanCalonSiswa()
									&& DetailTagihanSiswaHelper.apakahAda(workerSession, pbWorker, siswaWorker)) {
								isValid = true;
							} else if (calonWorker != null && jbs.getGunakanCalonSiswa()
									&& DetailTagihanCalonSiswaHelper.apakahAda(workerSession, pbWorker, calonWorker)) {
								isValid = true;
							}
						}

						if (isValid) {
							List<Tagihan> listTagihanLokal = jbs.getGunakanCalonSiswa()
									? TagihanUtilCalonSiswa.getTagihan(jbs, pbWorker, calonWorker,
											bln, thn, refresh)
									: TagihanUtil.getTagihan(jbs, pbWorker, siswaWorker, bln, thn,
											refresh);

							if (listTagihanLokal != null && !listTagihanLokal.isEmpty()) {
								resultMap.put(index, listTagihanLokal);
							}
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:2671");
					} finally {
						/* currentNativeSession disimpan ThreadLocal: wajib unbind sekaligus
						 * clear/disconnect/close sebelum thread pool dipakai ulang. */
						HibernateUtil.closeSession();
						completedCount.incrementAndGet();
					}
				}
			});
		}
		executor.shutdown();

		final Timer timer = new Timer();
		timer.setDelay(300);
		timer.setRepeats(true);
		timer.setParent(window);
		timer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				int current = completedCount.get();
				int percent = (int) ((current * 100.0) / totalData);
				int remaining = totalData - current;

				KeuanganDashboardEnhanceUtil.updateFloatingProgress(PB_KEY, percent,
						"Memproses " + current + " dari " + totalData + " data (" + percent + "%). Sisa: " + remaining);

				if (current >= totalData) {
					timer.stop();
					timer.detach();
					KeuanganDashboardEnhanceUtil.hideFloatingProgress(PB_KEY);

					renderUIHasilPembayaran(allPBiayas, resultMap);
					reloadRiwayatPembayaran(s_lokal, cs_lokal, sub);
					hitungUlangTagihan();
				}
			}
		});
	}

	private void renderUIHasilPembayaran(List<PengaturanBiaya> allPBiayas, Map<Integer, List<Tagihan>> resultMap) {
		Tbmuser tbmuser = Common.getCurrentUser();

		for (int i = 0; i < allPBiayas.size(); i++) {
			PengaturanBiaya pb = allPBiayas.get(i);
			List<Tagihan> listTagihanLokal = resultMap.get(i);

			if (listTagihanLokal != null && !listTagihanLokal.isEmpty()) {
				boolean ada = false;
				for (Tagihan tagihan : listTagihanLokal) {
					if (((tagihan.getAktif() && (pilihBukanTagihan.isChecked() || !tagihan.ambilBukanTagihanData()))
							&& !tagihan.getNominalBiaya().getBukanTagihan())
							&& tagihan.getPembayaranSiswaDetail() == null) {
						if (tagihan.getNominalBiaya().getItemBiayaSekolah().getNilaiBiayaBisaDiubahSaatPembayaran()
								|| tagihan.getNominal() > 0.1) {
							ada = true;
							break;
						}
					}
				}

				if (ada) {
					final PengaturanBiaya pbGrp = pb;
					final java.util.List<Tagihan> tagihanGrp = listTagihanLokal;
					final ais.ui.util.MyGroupConfig grpPb = new ais.ui.util.MyGroupConfig(pb.toString());
					grpPb.setParent(rowsDetailBiaya);
					if (pilihBukanTagihan.isChecked()) {
						final org.zkoss.zul.Hbox grpCtrlBox = new org.zkoss.zul.Hbox();
						grpCtrlBox.setStyle("float:right; margin-right:6px; gap:6px;");
						grpCtrlBox.setParent(grpPb);
						boolean allBukanTagihan = true; int cntAktif = 0;
						for (Tagihan tGrp : listTagihanLokal) {
							if (tGrp.getAktif() && tGrp.getPembayaranSiswaDetail() == null) {
								cntAktif++;
								if (!tGrp.ambilBukanTagihanData()) allBukanTagihan = false;
							}
						}
						final MyCheckboxConfig grpBukanTagihan = new MyCheckboxConfig("Bukan Tagihan");
						grpBukanTagihan.setChecked(cntAktif > 0 && allBukanTagihan);
						grpBukanTagihan.setStyle("font-size:8px; white-space:nowrap;");
						grpCtrlBox.appendChild(grpBukanTagihan);
						grpBukanTagihan.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event eBT) throws Exception {
								MyMessageboxConfig.show(
									"Apakah Anda yakin ingin menandai semua item pada grup ini sebagai "
										+ (grpBukanTagihan.isChecked() ? "\"Bukan Tagihan\"" : "tagihan aktif")
										+ "? Perubahan ini akan diterapkan pada seluruh item dalam grup tersebut.",
									"Konfirmasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
									MyMessageboxConfig.QUESTION, new EventListener() {
										@Override
										public void onEvent(Event evOk) throws Exception {
											if (Integer.parseInt(evOk.getData().toString()) != MyMessageboxConfig.OK) {
												grpBukanTagihan.setChecked(!grpBukanTagihan.isChecked()); return;
											}
											for (Tagihan tGrp2 : tagihanGrp) {
												tGrp2.setBukanTagihan(grpBukanTagihan.isChecked());
												Common.refreshUpdate(tGrp2);
											}
											Common.createDefaultTimer(new EventListener() {
												@Override
												public void onEvent(Event arg0) throws Exception { reloadTagihan(); }
											});
										}
									});
								}
							});
						final MyToolbarbuttonConfig btnResetGrp = new MyToolbarbuttonConfig("Reset");
						btnResetGrp.setStyle("font-size:8px; color:red; font-weight:bold;");
						btnResetGrp.setTooltiptext("Hapus semua NominalBiaya + Tagihan belum dibayar di grup ini, lalu buat ulang");
						grpCtrlBox.appendChild(btnResetGrp);
						btnResetGrp.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event eRst) throws Exception {
								MyMessageboxConfig.show(
									"Apakah Anda yakin ingin mereset semua tagihan \"" + pbGrp.toString()
										+ "\" yang belum dibayar? Status tagihan tersebut akan dikembalikan ke kondisi semula.",
									"Konfirmasi Reset", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
									MyMessageboxConfig.QUESTION, new EventListener() {
										@Override
										public void onEvent(Event evOk) throws Exception {
											if (Integer.parseInt(evOk.getData().toString()) != MyMessageboxConfig.OK) return;
											TagihanUtil.resetSemuaTagihanDalamPB(pbGrp, getSiswaLokal(), getCalonSiswaLokal());
											PengaturanBiaya.reloadTagihan(pbGrp, true);
											Common.createDefaultTimer(new EventListener() {
												@Override
												public void onEvent(Event arg0) throws Exception { reloadTagihan(); }
											});
										}
									});
								}
							});
					}
					Calendar cal = null;
					JenisBiayaSekolah jbs = pb.getJenisBiayaSekolah();
					List<Tagihan> listTagihanTampil = new ArrayList<Tagihan>(listTagihanLokal);
					Collections.sort(listTagihanTampil, new Comparator<Tagihan>() {
						@Override public int compare(Tagihan kiri, Tagihan kanan) {
							GrupItemBiayaSekolah gKiri = kiri == null || kiri.getItemBiayaSekolah() == null
									? null : kiri.getItemBiayaSekolah().getGrupItemBiayaSekolah();
							GrupItemBiayaSekolah gKanan = kanan == null || kanan.getItemBiayaSekolah() == null
									? null : kanan.getItemBiayaSekolah().getGrupItemBiayaSekolah();
							String kKiri = gKiri == null ? "\uffff" : gKiri.getLabelTampilan().toLowerCase();
							String kKanan = gKanan == null ? "\uffff" : gKanan.getLabelTampilan().toLowerCase();
							return kKiri.compareTo(kKanan);
						}
					});
					Long grupItemTerakhir = null;

					for (final Tagihan tagihan : listTagihanTampil) {
						if (((tagihan.getAktif() && (pilihBukanTagihan.isChecked() || !tagihan.ambilBukanTagihanData()))
								&& !tagihan.getNominalBiaya().getBukanTagihan())
								&& tagihan.getPembayaranSiswaDetail() == null) {

							try {
								if (jbs != null && jbs.getPeriode().equalsIgnoreCase("Bulanan")) {
									if (tagihan.getTahunbulan() == null) {
										continue;
									}
									if (tagihan.getPengaturanBiaya().getBulanMulai() != null
											&& tagihan.getTahunbulan() < tagihan.getPengaturanBiaya()
													.getBulanMulai())
										continue;
									if (tagihan.getPengaturanBiaya().getBulanSampai() != null
											&& tagihan.getTahunbulan() > tagihan.getPengaturanBiaya()
													.getBulanSampai())
										break;
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PembayaranOnline.java:2822");
								// TODO: handle exception
							}

							if (tagihan.getPembayaranSiswaDetail() == null) {
								GrupItemBiayaSekolah grupItem = tagihan.getItemBiayaSekolah() == null ? null
										: tagihan.getItemBiayaSekolah().getGrupItemBiayaSekolah();
								if (grupItem != null && grupItem.getId() != null
										&& !grupItem.getId().equals(grupItemTerakhir)) {
									MyGroupConfig kepalaGrupItem = new MyGroupConfig(grupItem.getLabelTampilan());
									kepalaGrupItem.setStyle("background:#0f4c5c;color:white;font-weight:bold;");
									kepalaGrupItem.setParent(rowsDetailBiaya);
									grupItemTerakhir = grupItem.getId();
								}
								MyFormRow row = new MyFormRow();
								row.setValign("top");
								row.setParent(rowsDetailBiaya);
								row.setAttribute("tagihan", tagihan);

								StringBuilder ketBuilder = new StringBuilder();
								ketBuilder.append(tagihan.getNominalBiaya().getItemBiayaSekolah().getNama());
								if (tagihan.getNominalBiaya().getDibayarSebayak() != null
										&& tagihan.getNominalBiaya().getDibayarSebayak() > 1) {
									ketBuilder.append(" (ke ").append(tagihan.getBayarKe()).append(")");
								}

								if (tagihan.getPengaturanBiaya().getJenisBiayaSekolah().getPeriode()
										.equals("Harian")) {
									if (cal == null)
										cal = ais.ui.util.WaktuUtil.getCalendar();
									cal.set(Calendar.DAY_OF_MONTH, tagihan.getBayarKe());
									if (tagihan.getTahunbulan() != null) {
										String tbStr = String.valueOf(tagihan.getTahunbulan());
										if (tbStr.length() >= 6) {
											cal.set(Calendar.MONTH, Integer.parseInt(tbStr.substring(4, 6)) - 1);
											cal.set(Calendar.YEAR, Integer.parseInt(tbStr.substring(0, 4)));
										}
									}
									ketBuilder.setLength(0);
									ketBuilder.append(tagihan.getNominalBiaya().getItemBiayaSekolah().getNama())
											.append(" (").append(Common.dateFormat41.get().format(cal.getTime()))
											.append(")");
								}

								if (tagihan.getDenda() != null && tagihan.getDenda() > 0.01) {
									ketBuilder.append(", Denda ")
											.append(Common.numberFormat.get().format(tagihan.getDenda()));
								}
								if (tagihan.getTanggalDeadline() != null) {
									ketBuilder.append(", Deadline ")
											.append(Common.dateFormat4.get().format(tagihan.getTanggalDeadline()));
								}
								DiskonSiswa diskonSiswaTagihan = null;
								try {
									diskonSiswaTagihan = tagihan.getDiskonSiswa();
								} catch (Exception e) {
									diskonSiswaTagihan = null;
									try {
										Common.tampilErrorJikaAdmin(e);
									} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PembayaranOnline.java:2872");
									}
								}
								if (diskonSiswaTagihan != null && diskonSiswaTagihan.getMemotongTagihan()) {
									ketBuilder.append(" - ").append(diskonSiswaTagihan.getNama());
								}

								try {
									PengaturanBiayaItemBiaya pbItem = tagihan.getNominalBiaya()
											.getPengaturanBiayaItemBiaya();
									if ((pbItem == null || (tagihan.getNominalBiaya().getDibayarSebayak() != null
											&& tagihan.getNominalBiaya().getDibayarSebayak() > 1) ? 0.0
													: pbItem.getDiskonBiaya()) > 0.1) {
										ketBuilder.append(", Diskon dibayar lunas 1x");
									}
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:2888");
								}

								String ket = ketBuilder.toString();
								final MyCheckboxConfig check = new MyCheckboxConfig(ket);
								check.setAttribute("tagihan", tagihan);
								pilihan.add(check);

								if (tagihan.getItemBiayaSekolah().getWajibPilih()
										|| (tagihansPilih != null && tagihansPilih.contains(tagihan.getId()))) {
									check.setChecked(true);
									// Admin boleh menyesuaikan meski item ditandai wajib; siswa/orang
									// tua tetap terkunci (lihat javadoc merupakanAdmin()).
									check.setDisabled(!merupakanAdmin());
									Common.createDefaultTimer(new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											try {
												eventListenerData.onEvent(new Event("", check));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:2906");
											}
										}
									});
								}
								row.setValign("top");
								row.setAttribute("pilih", check);

								boolean hasLink = tagihan.getLink() != null && !tagihan.getLink().isEmpty()
										&& (tagihan.getExpired() == null
												|| tagihan.getExpired().after(WaktuUtil.getDate()));
								boolean hasVa = tagihan.getVa() != null && (tagihan.getExpired() == null
										|| tagihan.getExpired().after(WaktuUtil.getDate()));

								if (hasLink) {
									Vbox vbox = new Vbox();
									vbox.setWidth("95%");
									vbox.setParent(row);
									vbox.appendChild(check);
									vbox.appendChild(new ais.ui.util.MyHtml(
											"<div style='font-size:9px;color:red;font-weight: bolder;'>bisa dibayar di link <a style='font-size:7px;color:blue;font-weight: normal;' onclick=\"popupCenter({url: '"
													+ tagihan.getLink()
													+ "', title: 'Pembayaran', w: 600, h: 600});\" href=\"#\">"
													+ tagihan.getLink() + "</a> "
													+ (tagihan.getExpired() == null ? ""
															: " sampai dengan " + Common.dateFormat.get()
																	.format(tagihan.getExpired()))
													+ "</div>"));
								} else if ((tbmuser == null || tbmuser.getSiswa() != null
										|| tbmuser.getCalonSiswa() != null) && hasVa) {
									row.appendChild(new ais.ui.util.MyHtml("<div style='font-size:10px;'>"
											+ tagihan.getNominalBiaya().getItemBiayaSekolah().getNama()
											+ (tagihan.getNominalBiaya().getDibayarSebayak() != null
													&& tagihan.getNominalBiaya().getDibayarSebayak() > 1
															? " (ke " + tagihan.getBayarKe() + ")"
															: "")
											+ "</div><div style='font-size:9px;color:red;font-weight: bolder;'>bisa dibayar di nomor VA : "
											+ tagihan.getVa()
											+ (tagihan.getExpired() == null ? ""
													: " sampai dengan "
															+ Common.dateFormat.get().format(tagihan.getExpired()))
											+ "</div>"));
								} else if (hasVa) {
									Vbox vbox = new Vbox();
									vbox.setWidth("95%");
									vbox.setParent(row);
									vbox.appendChild(check);
									vbox.appendChild(new ais.ui.util.MyHtml(
											"<div style='font-size:9px;color:red;font-weight: bolder;'>bisa dibayar di nomor VA : "
													+ tagihan.getVa()
													+ (tagihan.getExpired() == null ? ""
															: " sampai dengan " + Common.dateFormat.get()
																	.format(tagihan.getExpired()))
													+ "</div>"));
								} else {
									row.appendChild(check);
								}

								check.addEventListener("onClick", eventListenerData);
								try {
									org.zkoss.zul.Vbox vBulanTahun = RevisiHelper
											.createNewRevisi(Tagihan.class, tagihan,
													(tagihan.getBulan() == null ? ""
															: Common.BULAN[tagihan.getBulan() - 1] + " ")
															+ (tagihan.getTahun() == null ? "-" : tagihan.getTahun()));
									vBulanTahun.setParent(row);
									if (tagihan.getId() != null) {
										Label idTagihanLabel = new Label("ID: " + tagihan.getId());
										idTagihanLabel.setStyle("font-size:9px;color:#888;");
										vBulanTahun.appendChild(idTagihanLabel);
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PembayaranOnline.java:2977");
								}

								Vbox vboxRight = new Vbox();
								vboxRight.setPack("end");
								vboxRight.setAlign("end");
								vboxRight.setWidth("100%");
								if (pilihBukanTagihan.isChecked()) {
									row.appendChild(vboxRight);
								}

								if (tagihan.getNominalBiaya().getItemBiayaSekolah()
										.getNilaiBiayaBisaDiubahSaatPembayaran()) {
									final MyDoublebox nominalBox = new MyDoublebox(tagihan.getNominal());
									nominalBox.setWidth("90%");
									if (pilihBukanTagihan.isChecked())
										vboxRight.appendChild(nominalBox);
									else
										row.appendChild(nominalBox);
									row.setAttribute("nominal", nominalBox);
									nominalBox.addEventListener("onChange", new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											Session sDB = null;
											try {
												sDB = HibernateUtil.openSession();
												sDB.refresh(tagihan);
												NominalBiaya nb = tagihan.getNominalBiaya();
												nb.setNominal(nominalBox.getValue());
												sDB.getTransaction().begin();
												Common.refreshUpdate(sDB, nb);
												sDB.getTransaction().commit();
												tagihan.setNominalBiaya(nb);
												if (check.isChecked())
													tagihans.put(tagihan.getId(), tagihan);
												else
													tagihans.remove(tagihan.getId());
												hitungUlangTagihan();
											} catch (Exception e) {
												if (sDB != null && sDB.getTransaction() != null
														&& sDB.getTransaction().isActive())
													sDB.getTransaction().rollback();
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:3019");
											} finally {
												closeSessionAndDisconnect(sDB);
											}
										}
									});
								} else {
									Double diskon = tagihan.getDiskon() != null ? tagihan.getDiskon() : 0.0;
									Double denda = tagihan.getDenda() != null ? tagihan.getDenda() : 0.0;
									Double n = (tagihan.getNominal() - diskon) + denda;
									Double n1 = (n + diskon + denda);

									if (diskon > 0.01 && n1 > n) {
										if (!pilihBukanTagihan.isChecked())
											row.appendChild(vboxRight);
										vboxRight.appendChild(new Label(Common.numberFormat.get().format((n))));
										Label tagDiskon = new Label(Common.numberFormat.get().format(n1));
										tagDiskon.setParent(vboxRight);
										tagDiskon.setWidth("100%");
										tagDiskon.setStyle("text-decoration: line-through;");
									} else {
										if (pilihBukanTagihan.isChecked())
											vboxRight.appendChild(new Label(Common.numberFormat.get().format((n))));
										else
											row.appendChild(new Label(Common.numberFormat.get().format((n))));
									}
								}

								// ==========================================================
								// BLOK LOGIKA ANGSURAN (TAMBAH / HAPUS ANGSURAN)
								// ==========================================================
								if (tagihan.getNominalBiaya().getItemBiayaSekolah().getBolehDiangsur()
										&& tagihan.getPengaturanBiaya().getJenisBiayaSekolah()
												.getBolehAngsurBerapapun()
										&& !tagihan.getNominalBiaya().getItemBiayaSekolah().getWajibPilih()) {

									if (tagihan.getNominalBiaya().getDibayarSebayak() != null
											&& tagihan.getNominalBiaya().getDibayarSebayak().intValue() == tagihan
													.getBayarKe().intValue()) {

										if (!tagihan.getNominalBiaya().getItemBiayaSekolah().getAngsuranSeragam()
												&& tagihan.getKunci() == null
												&& tagihan.getPengaturanBiaya().getKunci() == null
												&& (pilihCustom.isChecked() || !tagihan.getItemBiayaSekolah()
														.getWajibPilihJikaBulanDipilih())) {
											MyToolbarbuttonConfig btnAddAngsuran = new MyToolbarbuttonConfig("",
													"/img/svg/addthis.svg");
											btnAddAngsuran.setParent(row);
											btnAddAngsuran.addEventListener("onClick", new EventListener() {
												@Override
												public void onEvent(Event arg0) throws Exception {
													tampilWindowAngsuran(tagihan);
												}
											});
										}
									} else if (tagihan.getNominalBiaya().getDibayarSebayak() != null
											&& tagihan.getNominalBiaya().getDibayarSebayak().intValue() > 1
											&& !tagihan.getNominalBiaya().getItemBiayaSekolah().getWajibPilih()
											&& !tagihan.getNominalBiaya().getItemBiayaSekolah().getAngsuranSeragam()
											&& tagihan.getKunci() == null
											&& tagihan.getPengaturanBiaya().getKunci() == null) {

										MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("",
												"/img/svg/trash.svg");
										button.setTooltiptext("Hapus Data");
										button.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												MyMessageboxConfig.show("Apakah Anda yakin ingin membatalkan angsuran ini? Angsuran yang telah dibatalkan tidak dapat dikembalikan secara otomatis.",
														"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
														MyMessageboxConfig.QUESTION, new EventListener() {
															@SuppressWarnings("unchecked")
															@Override
															public void onEvent(Event event) throws Exception {
																if (Integer.parseInt(event.getData()
																		.toString()) == MyMessageboxConfig.OK) {
																	Session sessionHapus = null;
																	Session mySession = null;
																	try {
																		Double tag = tagihan.getNominal();
																		NominalBiaya nominalBiaya = tagihan
																				.getNominalBiaya();
																		sessionHapus = HibernateUtil.currentSession();
																		Tagihan tagihanTerakhir = ((Tagihan) sessionHapus
																				.createCriteria(Tagihan.class)
																				.add(Restrictions.eq("nominalBiaya",
																						tagihan.getNominalBiaya()))
																				.add(Restrictions.gt("bayarKe", 1))
																				.addOrder(Order.desc("bayarKe"))
																				.addOrder(Order.desc("id"))
																				.setMaxResults(1).uniqueResult());
																		if (tagihanTerakhir != null) {
																			Double sisaYgBelum = tagihanTerakhir
																					.getNominal() + tag;
																			tagihanTerakhir.setNominal(sisaYgBelum);
																			tagihanTerakhir
																					.setNominalManual(sisaYgBelum);
																			Common.refreshUpdate(sessionHapus,
																					tagihanTerakhir);
																			sessionHapus.flush();
																		}
																		Common.refreshDelete(sessionHapus, tagihan);

																		List<Tagihan> tList = sessionHapus
																				.createCriteria(Tagihan.class)
																				.add(Restrictions.gt("nominal", 0.1))
																				.add(Restrictions.eq("nominalBiaya",
																						nominalBiaya))
																				.addOrder(Order.asc("id")).list();
																		int index = 1;
																		for (Tagihan t : tList) {
																			t.setBayarKe(index);
																			Common.refreshUpdate(sessionHapus, t);
																			sessionHapus.flush();
																			index++;
																		}

																		if (nominalBiaya != null) {
																			try {
																				mySession = HibernateUtil
																						.currentNativeSession();
																				mySession.refresh(nominalBiaya);
																				nominalBiaya.setDibayarSebayakManual(
																						index - 1);
																				nominalBiaya
																						.setDibayarSebayak(index - 1);
																				mySession.getTransaction().begin();
																				Common.refreshUpdate(mySession,
																						nominalBiaya);
																				mySession.getTransaction().commit();
																			} catch (Exception eInner) {
																				if (mySession != null && mySession
																						.getTransaction() != null
																						&& mySession.getTransaction()
																								.isActive())
																					mySession.getTransaction()
																							.rollback();
																				throw eInner;
																																						} finally {
																				/* currentNativeSession dikelola HibernateUtil; tidak ditutup manual di sini. */
																			}
																		}
																		Common.createDefaultTimer(new EventListener() {
																			@Override
																			public void onEvent(Event arg0)
																					throws Exception {
																				reloadTagihan();
																			}
																		});
																	} catch (Exception e) {
																		Common.tampilErrorJikaAdmin(e);
																		MyMessageboxConfig.show(
																				"Data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan: "
																						+ e.getMessage());
																	}
																}
															}
														});
											}
										});
										button.setParent(row);

//										MyToolbarbuttonConfig btnHapus = new MyToolbarbuttonConfig("",
//												"/img/svg/trash.svg");
//										btnHapus.setTooltiptext("Hapus Data");
//										btnHapus.setParent(row);
//										btnHapus.addEventListener("onClick", new EventListener() {
//											@Override
//											public void onEvent(Event event) throws Exception {
//												MyMessageboxConfig.show("Apakah yakin ingin membatalkan angsuran ini ?",
//														"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
//														MyMessageboxConfig.QUESTION, new EventListener() {
//															@Override
//															public void onEvent(Event event) throws Exception {
//																if (Integer.parseInt(event.getData()
//																		.toString()) == MyMessageboxConfig.OK) {
//																	Session sessHapus = null;
//																	try {
//																		sessHapus = HibernateUtil.openSession();
//																		sessHapus.getTransaction().begin();
//
//																		// PENTING: Reload entities in NEW session to
//																		// avoid "different object" error
//																		Tagihan freshTag = (Tagihan) sessHapus
//																				.get(Tagihan.class, tagihan.getId());
//																		NominalBiaya freshNb = (NominalBiaya) sessHapus
//																				.get(NominalBiaya.class, tagihan
//																						.getNominalBiaya().getId());
//
//																		Double tagVal = freshTag.getNominal();
//
//																		Tagihan tAkhir = (Tagihan) sessHapus
//																				.createCriteria(Tagihan.class)
//																				.add(Restrictions.eq("nominalBiaya",
//																						freshNb))
//																				.add(Restrictions.gt("bayarKe", 1))
//																				.addOrder(Order.desc("bayarKe"))
//																				.addOrder(Order.desc("id"))
//																				.setMaxResults(1).uniqueResult();
//
//																		if (tAkhir != null && !tAkhir.getId()
//																				.equals(freshTag.getId())) {
//																			tAkhir.setNominal(
//																					tAkhir.getNominal() + tagVal);
//																			sessHapus.update(tAkhir);
//																		}
//
//																		sessHapus.delete(freshTag);
//
//																		// Flush to apply deletion before checking list
//																		// again
//																		sessHapus.flush();
//
//
//																		List<Tagihan> tList = ConstantValues.simpleList(
//																				sessHapus.createCriteria(Tagihan.class)
//																						.add(Restrictions.gt("nominal", 0.1))
//																						.add(Restrictions.eq("nominalBiaya", freshNb))
//																						.addOrder(Order.asc("id")),
//																				Tagihan.class);
//
//																		if (tList != null && !tList.isEmpty()) {
//																			// TAHAP 1: Reset bayarKe ke nilai temporary yang unik
//																			// Kita gunakan offset 100000 + index agar kode unik selalu berbeda
//																			// dan menghindari kondisi 'bayarKe < 2' yang menyebabkan suffix kosong
//																			int tempIndex = 400;
//																			for (Tagihan tData : tList) {
//																				tData.setBayarKe(tempIndex++); 
//																				sessHapus.update(tData);
//																			}
//																			sessHapus.flush(); 
//
//																			// TAHAP 2: Re-index ke urutan yang benar (1, 2, 3...)
//																			int index = 1;
//																			for (Tagihan tData : tList) {
//																				tData.setBayarKe(index++);
//																				sessHapus.update(tData);
//																			} 
//																			
//																			// Update informasi jumlah tagihan di header
//																			if (freshNb != null) {
//																				freshNb.setDibayarSebayakManual(index - 1);
//																				freshNb.setDibayarSebayak(index - 1);
//																				sessHapus.update(freshNb);
//																			}
//																		} else if (freshNb != null) {
//                                                                            // Handle jika list kosong setelah hapus
//                                                                            freshNb.setDibayarSebayakManual(0); 
//                                                                            freshNb.setDibayarSebayak(0);
//                                                                            sessHapus.update(freshNb);
//                                                                        }
//
//																		sessHapus.getTransaction().commit();
//
//																		Common.createDefaultTimer(new EventListener() {
//																			@Override
//																			public void onEvent(Event arg0)
//																					throws Exception {
//																				reloadTagihan();
//																			}
//																		});
//																	} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PembayaranOnline.java:3280");
//																		if (sessHapus != null
//																				&& sessHapus.getTransaction() != null
//																				&& sessHapus.getTransaction()
//																						.isActive()) {
//																			sessHapus.getTransaction().rollback();
//																		}
//																		Common.tampilErrorJikaAdmin(e);
//																		MyMessageboxConfig.show(
//																				"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error: "
//																						+ e.getMessage());
//																	} finally {
//																		closeSessionAndDisconnect(sessHapus);
//																	}
//																}
//															}
//														});
//											}
//										});
									}
								}
							}
						}
					}
				}
			}
		}

		appendFormTopupSiswa(getSiswaLokal(), false);
	}

	/** Menambahkan input topup yang sama untuk mode pembayaran normal dan topup-only. */
	private void appendFormTopupSiswa(Siswa s_lokal, boolean otomatisAktif) {
		if (s_lokal != null) {
			boolean showTabungan = Common.bolehKonfigurasi("tampilkan_tabungan_siswa");
			MyGroupConfig grp = new ais.ui.util.MyGroupConfig();
			grp.setParent(rowsDetailBiaya);
			grp.appendChild(new Label(ais.common.Common.getBahasaConfig("Tambah Tabungan Siswa")));
			grp.setVisible(showTabungan);
			MyFormRow rTab = new MyFormRow();
			rTab.setValign("top");
			rTab.setParent(rowsDetailBiaya);
			rTab.setVisible(showTabungan);
			final MyCheckboxConfig cbTabungan = new MyCheckboxConfig("Nilai Tabungan Siswa");
			rTab.appendChild(cbTabungan);
			rTab.setAttribute("pilih", cbTabungan);
			rTab.appendChild(new MyLabelBoldAja(""));
			deposit = new MyDoublebox(0.0);
			deposit.setWidth("90%");
			deposit.setDisabled(!otomatisAktif);
			rTab.appendChild(deposit);
			rTab.setAttribute("nominal", deposit);
			cbTabungan.setChecked(otomatisAktif);
			cbTabungan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) {
					deposit.setDisabled(!cbTabungan.isChecked());
					hitungUlangTagihan();
				}
			});
			deposit.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event arg0) {
					hitungUlangTagihan();
				}
			});
		}
	}

	/** Nilai topup aktif untuk diteruskan ke seluruh kanal pembayaran siswa. */
	private Double getNilaiTopupAktif() {
		if (deposit == null || deposit.isDisabled() || deposit.getValue() == null
				|| deposit.getValue().doubleValue() <= 0.1) {
			return null;
		}
		return deposit.getValue();
	}

	private void tampilWindowAngsuran(final Tagihan tagihan) throws Exception {
		final MyWindow addWindow = new MyWindow("Tambah Angsuran", "none", false);
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setHeight("400px");
		addWindow.setWidth("450px");

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
		column.setWidth("40%");
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow r = new MyFormRow();
		r.setValign("top");
		r.setParent(rows);
		r.appendChild(new ais.ui.util.MyLabelConfig("Biaya"));
		r.appendChild(new MyLabelBoldAja(tagihan.getItemBiayaSekolah().getNama()));

		Session sLocal = null;
		Tagihan maksTagihan = null;
		try {
			sLocal = HibernateUtil.currentSession();
			maksTagihan = (Tagihan) sLocal.createCriteria(Tagihan.class)
					.add(Restrictions.eq("nominalBiaya", tagihan.getNominalBiaya())).addOrder(Order.desc("bayarKe"))
					.setMaxResults(1).uniqueResult();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PembayaranOnline.java:3383");
		}

		r = new MyFormRow();
		r.setParent(rows);
		r.appendChild(new ais.ui.util.MyLabelConfig("Tagihan"));

		final Double diskon = maksTagihan == null ? tagihan.ambilDiskonTanpaDikonBayarSatuKali()
				: maksTagihan.ambilDiskonTanpaDikonBayarSatuKali();
		final Double tag = maksTagihan == null
				? (tagihan.getDenda() + tagihan.getNominal()) - tagihan.ambilDiskonTanpaDikonBayarSatuKali()
				: (maksTagihan.getDenda() + tagihan.getNominal()) - maksTagihan.ambilDiskonTanpaDikonBayarSatuKali();

		r.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(tag)));

		if (diskon > 0.1) {
			r = new MyFormRow();
			r.setParent(rows);
			r.appendChild(new ais.ui.util.MyLabelConfig("Diskon"));
			r.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(diskon)));
		}

		r = new MyFormRow();
		r.setParent(rows);
		r.appendChild(new ais.ui.util.MyLabelConfig("Nominal yang akan dibayar *"));
		final MyDoublebox dibayar = new MyDoublebox(0.0);
		r.appendChild(dibayar);
		dibayar.setWidth("90%");

		r = new MyFormRow();
		r.setParent(rows);
		r.appendChild(new ais.ui.util.MyLabelConfig("Catatan / Informasi"));
		final MyTextbox informasi = new MyTextbox();
		r.appendChild(informasi);
		informasi.setWidth("90%");
		informasi.setRows(5);

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

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (dibayar.getValue() == null || dibayar.getValue() < 0.01 || dibayar.getValue() >= tag) {
					MyMessageboxConfig.show(
							"Nominal biaya belum diisi dengan benar. Langkah yang dapat dilakukan: (1) isi nominal biaya dengan angka yang lebih besar dari nol; (2) pastikan nominal tidak melebihi sisa tagihan; (3) ulangi penyimpanan setelah nominal sesuai.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}
				addWindow.detach();

				Session sDB = null;
				try {
					sDB = HibernateUtil.currentSession();
					NominalBiaya nominalBiaya = tagihan.getNominalBiaya();
					sDB.refresh(nominalBiaya);

					Number maks = (Number) sDB.createCriteria(Tagihan.class)
							.add(Restrictions.eq("nominalBiaya", nominalBiaya)).setProjection(Projections.rowCount())
							.add(Restrictions.gt("nominal", 0.1)).uniqueResult();
					int jml = (maks == null ? 1 : maks.intValue());
					nominalBiaya.setDibayarSebayakManual(jml + 1);
					nominalBiaya.setDibayarSebayak(jml + 1);
					Common.refreshUpdate(sDB, nominalBiaya);
					// FIX "Session is closed!": session ThreadLocal bisa ditutup helper lain di tengah alur.
					// Re-acquire bila sudah tertutup agar flush tidak melempar (jangan tutup manual sesi ini).
					if (sDB == null || !sDB.isOpen()) { sDB = HibernateUtil.currentNativeSession(); }
					sDB.flush();

					tagihan.setNominalBiaya(nominalBiaya);
					tagihan.setDiskonManual(diskon);
					tagihan.setNominal(dibayar.getValue() + diskon);
					tagihan.setNominalManual(dibayar.getValue() + diskon);
					tagihan.setDibayarManual(dibayar.getValue() + diskon);
					tagihan.setAktifkanmanual(true);
					Common.refreshUpdate(sDB, tagihan);
					// FIX "Session is closed!": session ThreadLocal bisa ditutup helper lain di tengah alur.
					// Re-acquire bila sudah tertutup agar flush tidak melempar (jangan tutup manual sesi ini).
					if (sDB == null || !sDB.isOpen()) { sDB = HibernateUtil.currentNativeSession(); }
					sDB.flush();

					Double sisaYgBelum = tag - dibayar.getValue();
					if (sisaYgBelum > 0.1) {
						Tagihan tagihan1 = ((Tagihan) sDB.createCriteria(Tagihan.class)
								.add(Restrictions.eq("nominalBiaya", nominalBiaya))
								.add(Restrictions.eq("bayarKe", jml + 1)).uniqueResult());

						if (tagihan1 == null) {
							Tagihan tagihanBaru = new Tagihan();
							tagihanBaru.setNominalBiaya(nominalBiaya);
							tagihanBaru
									.setBulan(nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah().getUntukBulan());
							tagihanBaru
									.setTahun(nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah().getUntukTahun());
							tagihanBaru.setSiswa(nominalBiaya.getSiswa());
							tagihanBaru.setCalonSiswa(nominalBiaya.getCalonSiswa());
							tagihanBaru.setItemBiayaSekolah(tagihan.getItemBiayaSekolah());
							tagihanBaru.setBayarKe(jml + 1);
							tagihanBaru.setNominal(sisaYgBelum);
							tagihanBaru.setNominalManual(sisaYgBelum);
							tagihanBaru.setAktifkanmanual(true);
							tagihanBaru.setInformasi(informasi.getValue());
							// Cegah duplicate key "tagihan_kode_unik_key": bila Tagihan dengan kode_unik
							// yang sama SUDAH ada (proses ganda / bayarKe sama), UPDATE yang ada — jangan
							// INSERT baru (yang memicu unique violation & rollback).
							Tagihan tagihanKodeSama = Tagihan.findByKodeUnik(tagihanBaru.getKodeUnik(), sDB);
							if (tagihanKodeSama != null) {
								tagihanKodeSama.setInformasi(informasi.getValue());
								tagihanKodeSama.setAktif(true);
								tagihanKodeSama.setAktifkanmanual(true);
								tagihanKodeSama.setNominal(sisaYgBelum);
								tagihanKodeSama.setNominalManual(sisaYgBelum);
								Common.refreshUpdate(sDB, tagihanKodeSama);
								// FIX "Session is closed!": session ThreadLocal bisa ditutup helper lain di tengah alur.
					// Re-acquire bila sudah tertutup agar flush tidak melempar (jangan tutup manual sesi ini).
					if (sDB == null || !sDB.isOpen()) { sDB = HibernateUtil.currentNativeSession(); }
					sDB.flush();
							} else {
								sDB.save(tagihanBaru);
								// FIX "Session is closed!": session ThreadLocal bisa ditutup helper lain di tengah alur.
					// Re-acquire bila sudah tertutup agar flush tidak melempar (jangan tutup manual sesi ini).
					if (sDB == null || !sDB.isOpen()) { sDB = HibernateUtil.currentNativeSession(); }
					sDB.flush();
							}
						} else {
							tagihan1.setInformasi(informasi.getValue());
							tagihan1.setAktif(true);
							tagihan1.setAktifkanmanual(true);
							tagihan1.setNominal(sisaYgBelum);
							tagihan1.setNominalManual(sisaYgBelum);
							Common.refreshUpdate(sDB, tagihan1);
							// FIX "Session is closed!": session ThreadLocal bisa ditutup helper lain di tengah alur.
					// Re-acquire bila sudah tertutup agar flush tidak melempar (jangan tutup manual sesi ini).
					if (sDB == null || !sDB.isOpen()) { sDB = HibernateUtil.currentNativeSession(); }
					sDB.flush();
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						reloadTagihan();
					}
				});
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
		addWindow.onModal();
	}

	private void renderDasborPanel(Siswa s, CalonSiswa cs, Component container, Integer bln, Integer thn) {
		container.getChildren().clear();
		if (s == null && cs == null) {
			container.appendChild(new ais.ui.util.MyHtml(
					"<div style='padding:20px;text-align:center;color:#999;'>Silakan pilih pelanggan terlebih dahulu.</div>"));
			return;
		}

		Session session = null;
		double totalTagihanKumulatif = 0.0;
		double totalBayarKumulatif = 0.0;
		double sisaDeposit = ais.action.master.sekolah.util.DepositHelper.hitungDeposit(s, cs);

		if (bln == null || bln.intValue() < 1 || bln.intValue() > 12) {
			bln = Integer.valueOf(WaktuUtil.getCalendar().get(Calendar.MONTH) + 1);
		}
		if (thn == null || thn.intValue() < 1900) {
			thn = Integer.valueOf(WaktuUtil.getCalendar().get(Calendar.YEAR));
		}
		Integer targetTahunBulan = Integer.valueOf((thn.intValue() * 100) + bln.intValue());

		try {
			session = HibernateUtil.openSession();

			for (MyCheckboxConfig cb : pilihan) {
				Tagihan tg = (Tagihan) cb.getAttribute("tagihan");

				String periode = "";
				if (tg.getNominalBiaya() != null && tg.getPengaturanBiaya() != null
						&& tg.getPengaturanBiaya().getJenisBiayaSekolah() != null) {
					periode = tg.getPengaturanBiaya().getJenisBiayaSekolah().getPeriode();
				}

				boolean isIncluded = false;
				if ("Bulanan".equalsIgnoreCase(periode)) {
					if (tg.getTahunbulan() != null && tg.getTahunbulan() <= targetTahunBulan) {
						isIncluded = true;
					}
				} else if ("Tahunan".equalsIgnoreCase(periode)) {
					if (tg.getTahun() != null && tg.getTahun() <= thn) {
						isIncluded = true;
					}
				} else {
					// Insidentil atau periode lainnya (tidak terikat bulan/tahun jatuh tempo)
					isIncluded = true;
				}

				if (isIncluded) {
					double n = tg.getNominal() != null ? tg.getNominal() : 0.0;
					double d = tg.getDenda() != null ? tg.getDenda() : 0.0;
					double ds = tg.getDiskon() != null ? tg.getDiskon() : 0.0;
					totalTagihanKumulatif += (n + d) - ds;

				}
			}

			// --- DATA GRAFIK HISTORY ---
			Criteria historyCriteria = session.createCriteria(PembayaranSiswaDetail.class)
					.createAlias("pembayaranSiswa", "pembayaranSiswa");
			if (s != null && cs != null) {
				historyCriteria.add(Restrictions.or(Restrictions.eq("pembayaranSiswa.siswa", s),
						Restrictions.eq("pembayaranSiswa.calonSiswa", cs)));
			} else if (s != null) {
				historyCriteria.add(Restrictions.eq("pembayaranSiswa.siswa", s));
			} else if (cs != null) {
				historyCriteria.add(Restrictions.eq("pembayaranSiswa.calonSiswa", cs));
			}
			historyCriteria.addOrder(Order.asc("pembayaranSiswa.tanggal"));
			List<PembayaranSiswaDetail> allHistoryForChart = ConstantValues.simpleList(historyCriteria,
					PembayaranSiswaDetail.class);
			for (PembayaranSiswaDetail psd : allHistoryForChart) {
				Tagihan tg = psd.getTagihan();
				if (tg == null)
					continue;

				String periode = "";
				if (tg.getNominalBiaya() != null && tg.getPengaturanBiaya() != null
						&& tg.getPengaturanBiaya().getJenisBiayaSekolah() != null) {
					periode = tg.getPengaturanBiaya().getJenisBiayaSekolah().getPeriode();
				}

				boolean isIncluded = false;
				if ("Bulanan".equalsIgnoreCase(periode)) {
					if (tg.getTahunbulan() != null && tg.getTahunbulan() <= targetTahunBulan) {
						isIncluded = true;
					}
				} else if ("Tahunan".equalsIgnoreCase(periode)) {
					if (tg.getTahun() != null && tg.getTahun() <= thn) {
						isIncluded = true;
					}
				} else {
					isIncluded = true;
				}

				if (isIncluded) {
					double n = psd.getNominal() != null ? psd.getNominal() : 0.0;
					totalBayarKumulatif += n;
					totalTagihanKumulatif += n;
				}
			}

			double sisaTagihanKumulatif = totalTagihanKumulatif - totalBayarKumulatif;
			if (sisaTagihanKumulatif < 0)
				sisaTagihanKumulatif = 0;

			// --- RENDER SUMMARY CARDS ---
			String labelPeriode = "S/D " + Common.BULAN[bln - 1].toUpperCase() + " " + thn;

			Vbox vContainer = new Vbox();
			vContainer.setParent(container);
			vContainer.setWidth("100%");

			vContainer.appendChild(new ais.ui.util.MyHtml(
					"<div style='display:grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap:10px; margin-bottom:20px;'>"
							+ "<div style='background:#f1f8ff; border:1px solid #c8e1ff; border-radius:8px; padding:12px; border-top:4px solid #0366d6;'>"
							+ "<div style='font-size:10px; color:#586069; font-weight:bold;'>TOTAL TAGIHAN ("
							+ labelPeriode + ")</div>"
							+ "<div style='font-size:16px; font-weight:bold; color:#0366d6;'>Rp "
							+ Common.numberFormat.get().format(totalTagihanKumulatif) + "</div>" + "</div>"
							+ "<div style='background:#f6ffed; border:1px solid #b7eb8f; border-radius:8px; padding:12px; border-top:4px solid #52c41a;'>"
							+ "<div style='font-size:10px; color:#586069; font-weight:bold;'>TOTAL TERBAYAR</div>"
							+ "<div style='font-size:16px; font-weight:bold; color:#52c41a;'>Rp "
							+ Common.numberFormat.get().format(totalBayarKumulatif) + "</div>" + "</div>"
							+ "<div style='background:#fff1f0; border:1px solid #ffa39e; border-radius:8px; padding:12px; border-top:4px solid #f5222d;'>"
							+ "<div style='font-size:10px; color:#586069; font-weight:bold;'>SISA TAGIHAN</div>"
							+ "<div style='font-size:16px; font-weight:bold; color:#f5222d;'>Rp "
							+ Common.numberFormat.get().format(sisaTagihanKumulatif) + "</div>" + "</div>"
							+ "<div style='background:#feffe6; border:1px solid #fffb8f; border-radius:8px; padding:12px; border-top:4px solid #fadb14;'>"
							+ "<div style='font-size:10px; color:#586069; font-weight:bold;'>SALDO TABUNGAN</div>"
							+ "<div style='font-size:16px; font-weight:bold; color:#856404;'>Rp "
							+ Common.numberFormat.get().format(sisaDeposit) + "</div>" + "</div>" + "</div>"));

			if (allHistoryForChart == null || allHistoryForChart.isEmpty()) {
				vContainer.appendChild(new Label(ais.common.Common.getBahasaConfig("Data history belum tersedia untuk grafik.")));
				return;
			}

			// --- GRAFIK TREND & KOMPOSISI ---
			Map<String, Double> itemMap = new HashMap<String, Double>();
			Map<String, Double> trendMap = new java.util.LinkedHashMap<String, Double>();
			java.text.SimpleDateFormat sdfMonth = new java.text.SimpleDateFormat("MMM yyyy");
			final List<Object[]> rekapTableData = new ArrayList<Object[]>();

			for (PembayaranSiswaDetail pDetail : allHistoryForChart) {
				double nom = pDetail.getNominal() != null ? pDetail.getNominal() : 0.0;
				String itemName = pDetail.getItemBiayaSekolah() != null ? pDetail.getItemBiayaSekolah().getNama()
						: "Lainnya";
				if (nom < 0.0) {
					itemName = "Bantuan/Potongan - " + itemName;
				}
				itemMap.put(itemName, itemMap.containsKey(itemName) ? itemMap.get(itemName) + nom : nom);
				if (pDetail.getPembayaranSiswa().getTanggal() != null) {
					String mY = sdfMonth.format(pDetail.getPembayaranSiswa().getTanggal());
					trendMap.put(mY, trendMap.containsKey(mY) ? trendMap.get(mY) + nom : nom);
					rekapTableData.add(new Object[] { new java.text.SimpleDateFormat("dd-MM-yyyy")
							.format(pDetail.getPembayaranSiswa().getTanggal()), itemName, nom });
				}
			}

			renderModernPaymentHistoryCharts(vContainer, trendMap, itemMap);

			MyGrid gRekap = new MyGrid();
			gRekap.setParent(vContainer);
			gRekap.setMold("paging");
			gRekap.setPageSize(5);
			Columns cRekap = new Columns();
			cRekap.setParent(gRekap);
			addCol(cRekap, "Tanggal", "100px", null);
			addCol(cRekap, "Item", null, null);
			addCol(cRekap, "Nominal", "120px", "right");
			Rows rRekap = new Rows();
			rRekap.setParent(gRekap);
			for (Object[] rd : rekapTableData) {
				MyFormRow rr = new MyFormRow();
				rr.setParent(rRekap);
				rr.appendChild(new Label((String) rd[0]));
				rr.appendChild(new Label((String) rd[1]));
				rr.appendChild(new Label(Common.numberFormat.get().format((Double) rd[2])));
			}

			allHistoryForChart.clear();
			allHistoryForChart = null;

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/PembayaranOnline.java:3736");
		} finally {
			closeSessionAndDisconnect(session);
		}
	}

	private void renderModernPaymentHistoryCharts(Vbox parent, Map<String, Double> trendMap, Map<String, Double> itemMap) {
		if (parent == null) {
			return;
		}
		parent.appendChild(new ais.ui.util.MyHtml(buildModernPaymentHistoryChartHtml(trendMap, itemMap)));
	}

	private String buildModernPaymentHistoryChartHtml(Map<String, Double> trendMap, Map<String, Double> itemMap) {
		StringBuilder html = new StringBuilder();
		double maxTrend = maxValue(trendMap);
		double maxItem = maxValue(itemMap);
		double totalTrend = sumValue(trendMap);
		double totalItem = sumValue(itemMap);
		int jumlahPeriode = trendMap == null ? 0 : trendMap.size();
		int jumlahItem = itemMap == null ? 0 : itemMap.size();
		double rataPeriode = jumlahPeriode <= 0 ? 0.0 : totalTrend / jumlahPeriode;
		String itemTerbesar = maxKey(itemMap);

		html.append("<div style='background:#ffffff; border:1px solid #e2e8f0; border-radius:14px; padding:14px; margin:10px 0 16px 0; box-shadow:0 1px 4px rgba(15,23,42,0.08);'>");
		html.append("<div style='display:flex; justify-content:space-between; gap:10px; align-items:flex-start; margin-bottom:12px;'>");
		html.append("<div><div style='font-weight:bold; color:#0f172a; font-size:14px;'>Riwayat Pembayaran</div>");
		html.append("<div style='font-size:11px; color:#64748b;'>Tren uang masuk, item biaya, dan ringkasan pembayaran ditampilkan dengan grafik ringan.</div></div>");
		html.append("<div style='font-size:11px; color:#0f766e; background:#ecfdf5; border:1px solid #bbf7d0; border-radius:999px; padding:5px 10px;'>HTML/CSS</div>");
		html.append("</div>");

		html.append("<div style='display:grid; grid-template-columns: repeat(4, minmax(130px, 1fr)); gap:10px; margin-bottom:14px;'>");
		appendInfoTile(html, "Total Riwayat", "Rp " + formatNumber(totalTrend), "#eff6ff", "#1d4ed8");
		appendInfoTile(html, "Rata-rata / Periode", "Rp " + formatNumber(rataPeriode), "#f0fdf4", "#15803d");
		appendInfoTile(html, "Jumlah Periode", String.valueOf(jumlahPeriode), "#f8fafc", "#334155");
		appendInfoTile(html, "Item Terbesar", itemTerbesar.length() == 0 ? "-" : itemTerbesar, "#fffbeb", "#92400e");
		html.append("</div>");

		html.append("<div style='display:grid; grid-template-columns: repeat(2, minmax(280px, 1fr)); gap:14px;'>");
		html.append("<div style='background:#f8fbff; border:1px solid #dbeafe; border-radius:12px; padding:14px;'>");
		html.append("<div style='font-weight:bold; color:#1e3a8a; margin-bottom:4px;'>Tren Pembayaran per Periode</div>");
		html.append("<div style='font-size:11px; color:#64748b; margin-bottom:12px;'>Uang masuk per bulan atau periode.</div>");
		appendVerticalBars(html, trendMap, maxTrend);
		html.append("</div>");

		html.append("<div style='background:#f7fff9; border:1px solid #dcfce7; border-radius:12px; padding:14px;'>");
		html.append("<div style='font-weight:bold; color:#166534; margin-bottom:4px;'>Komposisi Pembayaran per Item</div>");
		html.append("<div style='font-size:11px; color:#64748b; margin-bottom:12px;'>Uang masuk per item biaya.</div>");
		appendHorizontalBars(html, itemMap, maxItem);
		html.append("</div>");
		html.append("</div>");

		html.append("<div style='display:grid; grid-template-columns: repeat(2, minmax(280px, 1fr)); gap:14px; margin-top:14px;'>");
		html.append("<div style='background:#ffffff; border:1px solid #e2e8f0; border-radius:12px; padding:14px;'>");
		html.append("<div style='font-weight:bold; color:#0f172a; margin-bottom:8px;'>Porsi Item Pembayaran</div>");
		appendShareBars(html, itemMap, totalItem);
		html.append("</div>");
		html.append("<div style='background:#ffffff; border:1px solid #e2e8f0; border-radius:12px; padding:14px;'>");
		html.append("<div style='font-weight:bold; color:#0f172a; margin-bottom:8px;'>Catatan Riwayat</div>");
		html.append("<div style='font-size:11px; color:#475569; line-height:1.7;'>");
		html.append("<b>Periode transaksi:</b> ").append(jumlahPeriode).append("<br/>");
		html.append("<b>Jumlah item biaya:</b> ").append(jumlahItem).append("<br/>");
		html.append("<b>Total nominal item:</b> Rp ").append(formatNumber(totalItem)).append("<br/>");
		html.append("<b>Item nominal terbesar:</b> ").append(escapeHtml(itemTerbesar.length() == 0 ? "-" : itemTerbesar));
		html.append("</div>");
		html.append(ais.ui.util.PembayaranDashboardHtmlUtil.buildSpiderWebOnly("Spider Riwayat", "Bandingkan total riwayat, item, periode, dan rata-rata transaksi.", totalItem, totalTrend, Math.max(0.0, totalItem - totalTrend), totalTrend, jumlahPeriode, rataPeriode));
		html.append("</div>");
		html.append("</div>");
		html.append("</div>");
		return html.toString();
	}

	private void appendVerticalBars(StringBuilder html, Map<String, Double> data, double maxValue) {
		if (data == null || data.isEmpty()) {
			html.append("<div style='padding:18px; color:#64748b; background:#f8fafc; border-radius:8px;'>Belum ada data tren.</div>");
			return;
		}
		html.append("<div style='display:flex; align-items:flex-end; gap:8px; min-height:190px; overflow:auto; padding:8px 0;'>");
		for (Map.Entry<String, Double> entry : data.entrySet()) {
			double value = safeDouble(entry.getValue());
			int height = maxValue <= 0.0 ? 4 : (int) Math.round((value / maxValue) * 145.0);
			if (height < 4 && value > 0.0) {
				height = 4;
			}
			html.append("<div style='min-width:58px; display:flex; flex-direction:column; justify-content:flex-end; align-items:center;'>");
			html.append("<div title='Rp ").append(formatNumber(value)).append("' style='width:34px; height:").append(height)
					.append("px; border-radius:8px 8px 3px 3px; background:linear-gradient(180deg,#3b82f6,#1d4ed8);'></div>");
			html.append("<div style='font-size:10px; color:#334155; margin-top:6px; text-align:center; white-space:normal;'>")
					.append(escapeHtml(entry.getKey())).append("</div>");
			html.append("<div style='font-size:10px; color:#0f172a; font-weight:bold;'>Rp ").append(formatCompact(value)).append("</div>");
			html.append("</div>");
		}
		html.append("</div>");
	}

	private void appendHorizontalBars(StringBuilder html, Map<String, Double> data, double maxValue) {
		if (data == null || data.isEmpty()) {
			html.append("<div style='padding:18px; color:#64748b; background:#f8fafc; border-radius:8px;'>Belum ada data komposisi.</div>");
			return;
		}
		html.append("<div style='display:block;'>");
		int index = 0;
		for (Map.Entry<String, Double> entry : data.entrySet()) {
			if (index >= 10) {
				html.append("<div style='font-size:11px; color:#64748b; margin-top:8px;'>Item lain tetap ditampilkan pada tabel rekap.</div>");
				break;
			}
			double value = safeDouble(entry.getValue());
			int width = maxValue <= 0.0 ? 0 : (int) Math.round((Math.abs(value) / maxValue) * 100.0);
			html.append("<div style='margin-bottom:10px;'>");
			html.append("<div style='display:flex; justify-content:space-between; gap:8px; font-size:11px; color:#334155;'>");
			html.append("<span style='font-weight:bold;'>").append(escapeHtml(entry.getKey())).append("</span>");
			html.append("<span>Rp ").append(formatNumber(value)).append("</span>");
			html.append("</div>");
			html.append("<div style='height:10px; background:#f1f5f9; border-radius:999px; overflow:hidden; margin-top:4px;'>");
			html.append("<div style='height:10px; width:").append(width)
					.append("%; background:linear-gradient(90deg,#22c55e,#15803d); border-radius:999px;'></div>");
			html.append("</div>");
			html.append("</div>");
			index++;
		}
		html.append("</div>");
	}

	private void appendInfoTile(StringBuilder html, String title, String value, String background, String color) {
		html.append("<div style='background:").append(background)
				.append("; border:1px solid rgba(15,23,42,0.06); border-radius:12px; padding:10px;'>");
		html.append("<div style='font-size:10px; color:#64748b; font-weight:bold;'>").append(escapeHtml(title)).append("</div>");
		html.append("<div style='font-size:14px; color:").append(color)
				.append("; font-weight:bold; margin-top:4px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;'>")
				.append(escapeHtml(value)).append("</div>");
		html.append("</div>");
	}

	private void appendShareBars(StringBuilder html, Map<String, Double> data, double total) {
		if (data == null || data.isEmpty() || total <= 0.0) {
			html.append("<div style='padding:18px; color:#64748b; background:#f8fafc; border-radius:8px;'>Belum ada data rasio item.</div>");
			return;
		}
		int index = 0;
		for (Map.Entry<String, Double> entry : data.entrySet()) {
			if (index >= 8) {
				html.append("<div style='font-size:11px; color:#64748b; margin-top:8px;'>Item lain tetap tersedia pada tabel rekap.</div>");
				break;
			}
			double value = safeDouble(entry.getValue());
			int percent = total <= 0.0 ? 0 : (int) Math.round((Math.abs(value) / Math.abs(total)) * 100.0);
			if (percent < 0) {
				percent = 0;
			}
			if (percent > 100) {
				percent = 100;
			}
			html.append("<div style='margin-bottom:9px;'>");
			html.append("<div style='display:flex; justify-content:space-between; gap:8px; font-size:11px; color:#334155;'>");
			html.append("<span style='font-weight:bold;'>").append(escapeHtml(entry.getKey())).append("</span>");
			html.append("<span>").append(percent).append("%</span>");
			html.append("</div>");
			html.append("<div style='height:8px; background:#f1f5f9; border-radius:999px; overflow:hidden; margin-top:4px;'>");
			html.append("<div style='height:8px; width:").append(percent)
					.append("%; background:linear-gradient(90deg,#6366f1,#2563eb); border-radius:999px;'></div>");
			html.append("</div></div>");
			index++;
		}
	}

	private double sumValue(Map<String, Double> data) {
		double total = 0.0;
		if (data == null) {
			return total;
		}
		for (Double value : data.values()) {
			if (value != null) {
				total += value.doubleValue();
			}
		}
		return total;
	}

	private String maxKey(Map<String, Double> data) {
		String key = "";
		double max = 0.0;
		if (data == null) {
			return key;
		}
		for (Map.Entry<String, Double> entry : data.entrySet()) {
			double value = safeDouble(entry.getValue());
			if (value <= 0.0) {
				continue;
			}
			if (key.length() == 0 || value > max) {
				max = value;
				key = entry.getKey() == null ? "" : entry.getKey();
			}
		}
		return key;
	}

	private double maxValue(Map<String, Double> data) {
		double max = 0.0;
		if (data == null) {
			return max;
		}
		for (Double value : data.values()) {
			if (value != null && Math.abs(value.doubleValue()) > max) {
				max = Math.abs(value.doubleValue());
			}
		}
		return max;
	}

	private double safeDouble(Double value) {
		return value == null ? 0.0 : value.doubleValue();
	}

	private String formatNumber(double value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String formatCompact(double value) {
		if (value >= 1000000000.0) {
			return formatNumber(value / 1000000000.0) + "M";
		}
		if (value >= 1000000.0) {
			return formatNumber(value / 1000000.0) + "Jt";
		}
		if (value >= 1000.0) {
			return formatNumber(value / 1000.0) + "Rb";
		}
		return formatNumber(value);
	}

	private String escapeHtml(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

}
