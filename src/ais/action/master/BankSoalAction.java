package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.master.helper.AmbilDataPenjelasanBankSoalBanbox;
import ais.action.master.helper.DetailUjianHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.helper.AmbilDataMatapelajaranBanbox;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.AIGenerator;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.listener.DataLoader;
import ais.database.dao.BankSoalDao;
import ais.database.dao.DaoFactory;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.BankSoalDetail;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswaDetail;
import ais.database.model.Jurusan;
import ais.database.model.KategoriBankSoal;
import ais.database.model.Matakuliah;
import ais.database.model.PenjelasanBankSoal;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk bank soal. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code Grid grid}, {@code Textbox searchsoal}, {@code Combobox searchfakultas}, {@code Combobox
 * searchjurusan}, {@code Combobox searchyayasan}, {@code Combobox searchsekolah}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code initMain()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code tampilkanLampiran()}, {@code tampilkanLampiran()}, {@code onSearchDefault()});
 * mutasi data ({@code onSave()}); operasi domain lain ({@code onKategori()}, {@code onAddExternal()}, {@code
 * onAddExternal()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
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
public class BankSoalAction extends GenericAutowireComposer {

	/**
	 *
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private Grid grid;

	private Textbox searchsoal;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Combobox searchjenis;
	private Combobox searchkategoriBankSoal;
	private Combobox searchjenisPilihanGanda;
	private AmbilDataMatakuliahBanbox searchmatakuliah;
	private AmbilDataMatapelajaranBanbox searchMatapelajaran;
	private AmbilDataDosenBanbox searchdosen;
	private AmbilDataGuruBanbox searchguru;

	private MyCkEditor soal;
	private Textbox keterangan;
	private Combobox fakultas;
	private Combobox jurusan;

	private Combobox yayasan;
	private Combobox sekolah;
	private Combobox kategoriBankSoal;
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	private AmbilDataDosenBanbox dosen;
	private AmbilDataGuruBanbox guru;
	private AmbilDataMatakuliahBanbox matakuliah;
	private Combobox matapelajaran;

	private boolean edit = false;
	private boolean delete = false;

	private BankSoal bankSoal;
	private MyToolbarbuttonConfig add;

	private Textbox essay;
	private BankSoalDetail bankSoalDetail;
	private EventListener eventListener;
	private BankSoal bankSoalOriginal;
	protected LampiranLain lainSoal1;
	protected LampiranLain lainSoal2;
	protected LampiranLain lainSoal3;
	protected LampiranLain lainSoal4;
	protected LampiranLain lainSoal5;
	private MyCheckboxConfig jumlahLampiran;
	private Combobox jenisPilihanGanda;
	private MyDoublebox skor;
	private MyDoublebox skorSalah;
	private MyDoublebox skorDefault;

	private MyLabelConfig label_dosen;
	private MyLabelConfig label_fakultas;
	private MyLabelConfig label_prodi;

	private Column col_dosen;
	private Column col_mat;
	private boolean pt = false;
	private boolean ya = false;
	private Row hbFakultasLabel;
	private Row hbYayasan;

	private Tabpanel kategoriTab;
	private ais.ui.util.MyButtonTabbox btnTabBankSoal;
	private Combobox jenisKoreksi;
	private AmbilDataPenjelasanBankSoalBanbox penjelasanBankSoalBanbox;
	private MyCheckboxConfig jumlahJawabanDibatasi;
	private MyIntbox minimalJumlahJawaban;
	private MyIntbox maksimalJumlahJawaban;

	public void onKategori(Event event) {
		if (kategoriTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kategoriTab);
			MyInclude iframe = new MyInclude("/pages/master/kategori_bank_soal.zul");
			iframe.setParent(window);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// Obtain the container BEFORE super call so autowiring finds sub-ZUL children
		org.zkoss.zul.Div mainContainer = (org.zkoss.zul.Div) comp.getFellow("mainContainer");

		int[] tabAktif = {0};
		ais.ui.util.MyButtonTabbox btabs = ais.ui.util.MyButtonTabbox.buat(mainContainer, "100%", tabAktif);

		// Tab 0: Manajemen Soal — was inline, extracted to sub-ZUL (eager)
		org.zkoss.zul.Div panel0 = btabs.tambahTab(0, "Manajemen Soal");
		ais.ui.util.MyButtonTabbox.muatZulEager(panel0,
				"/WEB-INF/z/x/y/pages/master/bank_soal_tab_0.zul");

		// Tab 1: Grup Soal — was <include src="penjelasan_bank_soal.zul"> (lazy)
		final String src1 = "/WEB-INF/z/x/y/pages/master/penjelasan_bank_soal.zul";
		btabs.tambahTabLazy(1, "Grup Soal", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(org.zkoss.zul.Div panel) throws Exception {
				ais.ui.util.MyButtonTabbox.muatZul(panel, src1);
			}
		});

		// Wire fields from component tree (incl. eager sub-ZUL content in panel0)
		super.doAfterCompose(comp);

		// Tab 2: Kategori — admin-only, lazy (replaces empty tabpanel + onKategori pattern)
		int totalTabs = 2;
		if (Common.getApakahAdmin()) {
			btabs.tambahTabLazy(2, "Kategori", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override
				public void muat(org.zkoss.zul.Div panel) throws Exception {
					MyWindow window = new MyWindow("", "none", false);
					window.setHeight("100%");
					window.setWidth("100%");
					window.setParent(panel);
					MyInclude iframe = new MyInclude("/pages/master/kategori_bank_soal.zul");
					iframe.setParent(window);
				}
			});
			totalTabs = 3;
		}
		btabs.pulihkanSeleksi(totalTabs);

		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (kategoriTab != null && !Common.getApakahAdmin()) {
			kategoriTab.getLinkedTab().setVisible(false);
			kategoriTab.setVisible(false);
		}

		searchguru.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		searchmatakuliah.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		searchMatapelajaran.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);
		if (hbFakultasLabel != null)
			if (hbFakultasLabel != null) { hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1); }

		if (hbYayasan != null)
			if (hbYayasan != null) { hbYayasan.setVisible(ya); }

		if (label_dosen != null) {
			label_dosen.getParent().setVisible(pt);
		}
		if (label_fakultas != null) {
			label_fakultas.setVisible(pt);
		}
		if (label_prodi != null) {
			label_prodi.setVisible(pt);
		}

		if (searchfakultas != null) {
			searchfakultas.setVisible(pt);
		}
		if (searchjurusan != null) {
			searchjurusan.setVisible(pt);
		}
		if (col_dosen != null && pt) {
			col_dosen.setWidth("0%");
		}
		if (col_mat != null && pt) {
			col_mat.setWidth("0%");
		}

		searchmatakuliah.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});
		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		Common.insertComboDanSemua(searchkategoriBankSoal, "nama", "keterangan", KategoriBankSoal.class,
				Restrictions.eq("aktif", true));
		MyComboitemConfig comboitem = new MyComboitemConfig(PenjelasanBankSoal.KOREKSI_OTOMATIS);
		if (comboitem != null) { comboitem.setValue(PenjelasanBankSoal.KOREKSI_OTOMATIS); }
		searchjenis.appendChild(comboitem);
		comboitem = new MyComboitemConfig(PenjelasanBankSoal.KOREKSI_MANUAL);
		if (comboitem != null) { comboitem.setValue(PenjelasanBankSoal.KOREKSI_MANUAL); }
		searchjenis.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchjenis.appendChild(comboitem);

		if (searchjenis != null) { searchjenis.setSelectedItem(comboitem); }
		if (searchjenis != null) { searchjenis.setReadonly(true); }

		comboitem = new MyComboitemConfig(BankSoal.MULTIPLE_COICE);
		if (comboitem != null) { comboitem.setValue(BankSoal.MULTIPLE_COICE); }
		searchjenisPilihanGanda.appendChild(comboitem);
		comboitem = new MyComboitemConfig(BankSoal.COMBINATION_CHOICE);
		if (comboitem != null) { comboitem.setValue(BankSoal.COMBINATION_CHOICE); }
		searchjenisPilihanGanda.appendChild(comboitem);
		comboitem = new MyComboitemConfig(BankSoal.BENAR_SALAH);
		if (comboitem != null) { comboitem.setValue(BankSoal.BENAR_SALAH); }
		searchjenisPilihanGanda.appendChild(comboitem);
		comboitem = new MyComboitemConfig(BankSoal.JAWABAN_SINGKAT);
		if (comboitem != null) { comboitem.setValue(BankSoal.JAWABAN_SINGKAT); }
		searchjenisPilihanGanda.appendChild(comboitem);
		comboitem = new MyComboitemConfig(BankSoal.RUMPANG);
		if (comboitem != null) { comboitem.setValue(BankSoal.RUMPANG); }
		searchjenisPilihanGanda.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchjenisPilihanGanda.appendChild(comboitem);

		if (searchjenisPilihanGanda != null) { searchjenisPilihanGanda.setSelectedItem(comboitem); }
		if (searchjenisPilihanGanda != null) { searchjenisPilihanGanda.setReadonly(true); }

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Download", "/img/excel.png");
		if (button != null) { button.setVisible((add != null && add.isVisible()) && edit); }
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				DetailUjianHelper.doDownload(initCriteria(true), null);
			}
		});
		Common.appendKeToolbar(button, add, comp);

		button = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(), "/img/excel.png");
		if (button != null) { button.setVisible((add != null && add.isVisible()) && edit); }
		if (button != null) { button.setUpload(Common.ukuranFileUpload()); }
		button.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;
				DetailUjianHelper.doUpload(media, new DataLoader() {

					@Override
					public void loadData(Object value) {
						onSearchDefault(null);
					}
				}, null);
			}
		});

		Common.appendKeToolbar(button, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link BankSoalAction}. Kelas ini menerjemahkan satu item data menjadi
	 * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link BankSoalAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Tbmuser tbmuser}, {@code
	 * EventListener ubahEventListener}; operasi lokal: {@code render}(). Aturan bisnis bersama tetap berada pada
	 * kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see BankSoalAction
	 */
	class BankSoalRenderer extends ais.ui.util.MyRowRenderer {

		private Tbmuser tbmuser = Common.getCurrentUser();

		private EventListener ubahEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		};

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			BankSoal bankSoal = (BankSoal) arg1;

			DetailUjianHelper.tampilSoalDanJawaban(arg0, bankSoal, null, tbmuser, true, true, true, ubahEventListener,
					edit, delete);

		}

	}

	public static void onAddExternal(Event event, EventListener eventListener, BankSoal bankSoal, String jenisKoreksi)
			throws Exception {
		onAddExternal(event, eventListener, bankSoal, null, jenisKoreksi);
	}

	public static void onAddExternal(Event event, EventListener eventListener, BankSoal bankSoal,
			BankSoal bankSoalOriginal, String jenisKoreksi) throws Exception {
		if (bankSoal == null) {
			PesanFormalHelper.tampilkanGagal("pembukaan form ubah soal",
					"Data soal yang akan diubah tidak ditemukan.",
					new String[] { "Klik Refresh pada daftar soal, lalu coba ubah kembali.",
							"Jika soal masih tidak dapat dibuka, periksa apakah data soal masih terhubung ke ujian ini." });
			return;
		}
		if (bankSoal.getId() != null) {
			try {
				Object fresh = HibernateUtil.currentSession().get(BankSoal.class, bankSoal.getId());
				if (fresh instanceof BankSoal) {
					bankSoal = (BankSoal) fresh;
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit src/ais/action/master/BankSoalAction.java:onAddExternal-refresh-bankSoal");
			}
		}
		if (jenisKoreksi != null && !jenisKoreksi.trim().isEmpty()) {
			bankSoal.setJenisKoreksi(jenisKoreksi);
		} else {
			bankSoal.setJenisKoreksi(bankSoal.getJenisKoreksi());
		}
		bankSoal.setJenisPilihanGanda(bankSoal.getJenisPilihanGanda());

		BankSoalAction bankSoalAction = new BankSoalAction();
		bankSoalAction.eventListener = eventListener;
		bankSoalAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(bankSoalAction.addWindow);
		bankSoalAction.addWindow.setHeight("95%");
		bankSoalAction.addWindow.setWidth("90%");
		bankSoalAction.bankSoalOriginal = bankSoalOriginal;
		bankSoalAction.init(bankSoal);

		if (jenisKoreksi != null) {
			Common.selectComboItem(true, bankSoalAction.jenisKoreksi, jenisKoreksi);
//			bankSoalAction.jenisKoreksi.setDisabled(true);
		}

		bankSoalAction.addWindow.setVisible(true);
		bankSoalAction.addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new BankSoal());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public static void tampilkanLampiran(final BankSoalAction bankSoalAction, BankSoal bankSoal, Component c,
			boolean tampilUpload, Component parentPreview1, Component parentPreview2, Component parentPreview3,
			Component parentPreview4, Component parentPreview5) {

		Vbox vbox = new Vbox();
		vbox.setParent(c);

		Hbox hbox = new Hbox();
		hbox.setParent(vbox);

		LampiranLain.createDownloadUploadFileLain(hbox, bankSoal.getId(), "Soal I", "Lampiran Soal I", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (bankSoalAction != null) {
							bankSoalAction.lainSoal1 = (LampiranLain) arg0.getData();
						}
					}
				}, null, false, false, false, tampilUpload, null, false, false, parentPreview1);

		hbox = new Hbox();
		hbox.setParent(vbox);

		LampiranLain.createDownloadUploadFileLain(hbox, bankSoal.getId(), "Soal II", "Lampiran Soal II", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (bankSoalAction != null) {
							bankSoalAction.lainSoal2 = (LampiranLain) arg0.getData();
						}
					}
				}, null, false, false, false, tampilUpload, null, false, false, parentPreview2);

		hbox = new Hbox();
		hbox.setParent(vbox);

		LampiranLain.createDownloadUploadFileLain(hbox, bankSoal.getId(), "Soal III", "Lampiran Soal III", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (bankSoalAction != null) {
							bankSoalAction.lainSoal3 = (LampiranLain) arg0.getData();
						}
					}
				}, null, false, false, false, tampilUpload, null, false, false, parentPreview3);

		hbox = new Hbox();
		hbox.setParent(vbox);

		LampiranLain.createDownloadUploadFileLain(hbox, bankSoal.getId(), "Soal IV", "Lampiran Soal IV", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (bankSoalAction != null) {
							bankSoalAction.lainSoal4 = (LampiranLain) arg0.getData();
						}
					}
				}, null, false, false, false, tampilUpload, null, false, false, parentPreview4);

		hbox = new Hbox();
		hbox.setParent(vbox);

		LampiranLain.createDownloadUploadFileLain(hbox, bankSoal.getId(), "Soal V", "Lampiran Soal V", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (bankSoalAction != null) {
							bankSoalAction.lainSoal5 = (LampiranLain) arg0.getData();
						}
					}
				}, null, false, false, false, tampilUpload, null, false, false, parentPreview5);

	}

	public static void tampilkanLampiran(HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail, Component component,
			boolean tampilUpload, int jumlah, final EventListener myEventListener) {
		System.out.println("jumlah => " + jumlah + ", "
				+ (hasilUjianMahasiswaDetail == null ? "" : hasilUjianMahasiswaDetail.getId()));
		if (jumlah > 0) {
			MyGrid gridmy = new MyGrid();
			gridmy.setWidth("100%");
			gridmy.setParent(component);
			gridmy.setStyle("border:0px;background: transparent;");

			Rows rows = new Rows();
			rows.setParent(gridmy);

			for (int i = 0; i < jumlah; i++) {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				MyFormRow rowlampiran = new MyFormRow();
				rowlampiran.setParent(rows);

				Hbox hbox = new Hbox();
				hbox.setParent(row);
				LampiranLain.createDownloadUploadFileLain(hbox, hasilUjianMahasiswaDetail.getId(),
						"Jawaban ke-" + (i + 1), "Lampiran Jawaban Anda", false, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (myEventListener != null) {
									myEventListener.onEvent(new Event("", null, arg0.getData()));
								}
							}
						}, null, false, false, false, tampilUpload, null, false, false, rowlampiran);
			}
		}

	}

	private Borderlayout initMain(final BankSoal bankSoal) throws Exception {
		Sekolah sekolah1 = SekolahUtil.getSekolah();

		Tbmuser tbmuser = Common.getCurrentUser();
		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, searchfakultas, searchjurusan);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		West west = new West();
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("65%");

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// column.setWidth("90%");

		Borderlayout myborderlayout = new ais.ui.util.MyBorderlayout();
		myborderlayout.setParent(west);

		Center mycenter = new Center();
		mycenter.setParent(myborderlayout);
		ais.ui.util.ZkCompat.setFlex(mycenter, true);
		mycenter.setBorder("none");

		Borderlayout myborderlayout22 = new ais.ui.util.MyBorderlayout();
		myborderlayout22.setParent(mycenter);

		mycenter = new Center();
		mycenter.setParent(myborderlayout22);
		ais.ui.util.ZkCompat.setFlex(mycenter, true);
		mycenter.setBorder("none");

		Grid gridSoal = new MyGrid();
		gridSoal.setWidth("100%");
		gridSoal.setParent(mycenter);
		gridSoal.setWidth("100%");
		gridSoal.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(gridSoal);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		if (bankSoal != null && bankSoal.getId() != null) {
			RevisiHelper.createNewRevisi(BankSoal.class, bankSoal, "Soal").setParent(row);
		} else {
			row.appendChild(new MyLabelBoldConfig("Soal"));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(soal = new MyCkEditor());

		soal.setValue(bankSoal.getSoal());
		soal.setWidth("99%");
		soal.setHeight("300px");

		MyToolbarbutton fileupload = new MyToolbarbutton("fa-cog", "Generate Soal");
		soal.hbox.appendChild(fileupload);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelBoldConfig("Lampiran Soal"));

		MyFormRow rowlampiran1 = new MyFormRow();
		MyFormRow rowlampiran2 = new MyFormRow();
		MyFormRow rowlampiran3 = new MyFormRow();
		MyFormRow rowlampiran4 = new MyFormRow();
		MyFormRow rowlampiran5 = new MyFormRow();

		row = new MyFormRow();
		row.setParent(rows);
		BankSoalAction.tampilkanLampiran(this, bankSoal, row, true, rowlampiran1, rowlampiran2, rowlampiran3,
				rowlampiran4, rowlampiran5);

		rowlampiran1.setParent(rows);
		rowlampiran2.setParent(rows);
		rowlampiran3.setParent(rows);
		rowlampiran4.setParent(rows);
		rowlampiran5.setParent(rows);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);

		rows = new Rows();
		rows.setParent(grid);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Koreksi *"));
		row.appendChild(jenisKoreksi = new Combobox());

		MyComboitemConfig comboitem = new MyComboitemConfig(PenjelasanBankSoal.KOREKSI_OTOMATIS);
		comboitem.setValue(PenjelasanBankSoal.KOREKSI_OTOMATIS);
		jenisKoreksi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(PenjelasanBankSoal.KOREKSI_MANUAL);
		comboitem.setValue(PenjelasanBankSoal.KOREKSI_MANUAL);
		jenisKoreksi.appendChild(comboitem);

		Common.selectComboItem(true, jenisKoreksi, bankSoal.getJenisKoreksi());
		jenisKoreksi.setWidth("90%");
		jenisKoreksi.setReadonly(true);

		final MyFormRow rowJenisPilihanGanda = new MyFormRow();
		rowJenisPilihanGanda.setStyle("border:0px;background: transparent;");
		rowJenisPilihanGanda.setParent(rows);
		rowJenisPilihanGanda.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Soal *")));
		rowJenisPilihanGanda.appendChild(jenisPilihanGanda = new Combobox());
		comboitem = new MyComboitemConfig(BankSoal.MULTIPLE_COICE);
		comboitem.setValue(BankSoal.MULTIPLE_COICE);
		jenisPilihanGanda.appendChild(comboitem);
		comboitem = new MyComboitemConfig(BankSoal.COMBINATION_CHOICE);
		comboitem.setValue(BankSoal.COMBINATION_CHOICE);
		jenisPilihanGanda.appendChild(comboitem);

		comboitem = new MyComboitemConfig(BankSoal.MENGURUTKAN);
		comboitem.setValue(BankSoal.MENGURUTKAN);
		jenisPilihanGanda.appendChild(comboitem);

		comboitem = new MyComboitemConfig(BankSoal.MENJODOHKAN);
		comboitem.setValue(BankSoal.MENJODOHKAN);
		jenisPilihanGanda.appendChild(comboitem);

		comboitem = new MyComboitemConfig(BankSoal.BENAR_SALAH);
		comboitem.setValue(BankSoal.BENAR_SALAH);
		jenisPilihanGanda.appendChild(comboitem);

		comboitem = new MyComboitemConfig(BankSoal.JAWABAN_SINGKAT);
		comboitem.setValue(BankSoal.JAWABAN_SINGKAT);
		jenisPilihanGanda.appendChild(comboitem);

		comboitem = new MyComboitemConfig(BankSoal.RUMPANG);
		comboitem.setValue(BankSoal.RUMPANG);
		jenisPilihanGanda.appendChild(comboitem);

		Common.selectComboItem(jenisPilihanGanda, bankSoal.getJenisPilihanGanda());
		jenisPilihanGanda.setWidth("90%");
		jenisPilihanGanda.setReadonly(true);

		EventListener eventListenerKor = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

					if (jenisKoreksi == null || jenisKoreksi.getSelectedItem() == null) {
						return;
					}
					String kor = (String) jenisKoreksi.getSelectedItem().getValue();

				bankSoal.setJenisKoreksi(kor);

				if (kor.equals(PenjelasanBankSoal.KOREKSI_MANUAL)) {
					rowJenisPilihanGanda.setVisible(false);
					if (btnTabBankSoal != null) btnTabBankSoal.setVisibleTombol(1, false);
				} else if (kor.equals(PenjelasanBankSoal.KOREKSI_OTOMATIS)) {

					rowJenisPilihanGanda.setVisible(true);
					if (btnTabBankSoal != null) btnTabBankSoal.setVisibleTombol(1, true);

				}

			}
		};

		jenisKoreksi.addEventListener("onChange", eventListenerKor);
		eventListenerKor.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(jumlahJawabanDibatasi = new MyCheckboxConfig("Jumlah jawaban peserta dibatasi"));
		jumlahJawabanDibatasi.setChecked(bankSoal.getJumlahJawabanDibatasi());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal jumlah jawaban"));
		row.appendChild(minimalJumlahJawaban = new MyIntbox(bankSoal.getMinimalJumlahJawaban()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Maksimal jumlah jawaban"));
		row.appendChild(maksimalJumlahJawaban = new MyIntbox(bankSoal.getMaksimalJumlahJawaban()));

		Common.insertComboDanSemua(kategoriBankSoal = new Combobox(), "nama", "keterangan", KategoriBankSoal.class,
				Restrictions.eq("aktif", true));
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kategori"));
		row.appendChild(kategoriBankSoal);
		kategoriBankSoal.setWidth("90%");
		Common.selectComboItem(kategoriBankSoal, bankSoal.getKategoriBankSoal());
		kategoriBankSoal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(bankSoal.getSatuanKerja() == null ? "" : bankSoal.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", bankSoal.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");
		satuanKerja.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Skor Benar"));
		row.appendChild(skor = new MyDoublebox(bankSoal.getSkor()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Skor Salah"));
		row.appendChild(skorSalah = new MyDoublebox(bankSoal.getSkorSalah()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Skor Default"));
		row.appendChild(skorDefault = new MyDoublebox(bankSoal.getSkorDefault()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(
				jumlahLampiran = new MyCheckboxConfig("Peserta ujian bisa meng-upload file jawaban di soal ini"));
		jumlahLampiran.setChecked(bankSoal.getJumlahLampiran() > 0);

		EventListener eventListenerJumlahJawabanDibatasi = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				String jenis = (String) (jenisPilihanGanda.getSelectedItem() == null ? null
						: jenisPilihanGanda.getSelectedItem().getValue());

				jumlahJawabanDibatasi.getParent()
						.setVisible(jenis != null && jenis.equalsIgnoreCase(BankSoal.COMBINATION_CHOICE));
				minimalJumlahJawaban.getParent().setVisible(jumlahJawabanDibatasi.isChecked());
				maksimalJumlahJawaban.getParent().setVisible(jumlahJawabanDibatasi.isChecked());

			}
		};

		jumlahJawabanDibatasi.addEventListener("onClick", eventListenerJumlahJawabanDibatasi);
		eventListenerJumlahJawabanDibatasi.onEvent(null);

		jenisPilihanGanda.addEventListener("onChange", eventListenerJumlahJawabanDibatasi);

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.insertCombo(fakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(fakultas,
				bankSoal.getFakultas() == null || bankSoal.getFakultas() == null
						? Common.getCurrentUser().ambilFakultas()
						: bankSoal.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan,
				bankSoal.getJurusan() == null ? Common.getCurrentUser().ambilJurusan() : bankSoal.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(null, null, yayasan, sekolah);

		if (sekolah1 != null && sekolah1.getId() != null) {
			bankSoal.setSekolah(sekolah1);
		}

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		Common.selectComboItem(yayasan,
				bankSoal.getYayasan() == null || bankSoal.getYayasan() == null ? Common.getCurrentUser().ambilYayasan()
						: bankSoal.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		Common.pilihSekolah(sekolah,
				bankSoal.getSekolah() == null ? Common.getCurrentUser().ambilSekolah() : bankSoal.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembuat"));
		row.appendChild(dosen = new AmbilDataDosenBanbox());
		dosen.setValue(bankSoal.getDosen() == null ? "" : bankSoal.getDosen().getNama());
		dosen.setAttribute("myValue", bankSoal.getDosen());
		dosen.setWidth("90%");

		if (tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen mydosen = tbmuser.ambilDosen();
			dosen.setValue(mydosen.getNama());
			dosen.setAttribute("myValue", mydosen);
			dosen.setDisabled(true);
		}

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru Pembuat"));
		row.appendChild(guru = new AmbilDataGuruBanbox());
		guru.setValue(bankSoal.getGuru() == null ? "" : bankSoal.getGuru().getNama());
		guru.setAttribute("myValue", bankSoal.getGuru());
		guru.setAttribute("guru", bankSoal.getGuru());
		guru.setWidth("90%");

		if (tbmuser != null && tbmuser.ambilGuru() != null) {
			Guru mydosen = tbmuser.ambilGuru();
			guru.setValue(mydosen.getNama());
			guru.setAttribute("myValue", mydosen);
			guru.setAttribute("guru", mydosen);
			guru.setDisabled(true);
		}

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah terkait"));
		row.appendChild(matakuliah = new AmbilDataMatakuliahBanbox());
		matakuliah.setValue(bankSoal.getMatakuliah() == null ? "" : bankSoal.getMatakuliah().getNama());
		matakuliah.setAttribute("matakuliah", bankSoal.getMatakuliah());
		matakuliah.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matapelajaran terkait"));
		row.appendChild(matapelajaran = new Combobox());
		matapelajaran.setWidth("90%");

		EventListener mkListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				System.out.println("s => " + s);

				Common.insertCombo(matapelajaran, new String[] { "nama", "jenisPenilaian" }, Matapelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				matapelajaran.setReadonly(true);

				Common.selectComboItem(matapelajaran, bankSoal.getMatapelajaran());
			}
		};

		sekolah.addEventListener("onChange", mkListener);
		Common.createDefaultTimer(mkListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Deskipsi / Penjelasan Soal"));
		penjelasanBankSoalBanbox = new AmbilDataPenjelasanBankSoalBanbox();
		penjelasanBankSoalBanbox
				.setValue(bankSoal.getPenjelasanBankSoal() == null ? "" : bankSoal.getPenjelasanBankSoal().getNama());
		penjelasanBankSoalBanbox.setWidth("90%");
		penjelasanBankSoalBanbox.setAttribute("penjelasanBankSoal", bankSoal.getPenjelasanBankSoal());
		row.appendChild(penjelasanBankSoalBanbox);
		penjelasanBankSoalBanbox.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(bankSoal.getKeterangan() == null ? "" : bankSoal.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		Matakuliah mk = (Matakuliah) (matakuliah == null ? null : matakuliah.getAttribute("matakuliah"));
		Matapelajaran matpel = (Matapelajaran) (matapelajaran == null || matapelajaran.getSelectedItem() == null ? null
				: matapelajaran.getSelectedItem().getValue());
		String tanyaAkhiran = "";
		String tanyaMengajar = " apa saja";
		if (mk != null) {
			tanyaMengajar = " matakuliah " + mk.getNama();
			tanyaAkhiran = " pada matakuliah \"" + mk.getNama() + "\"";
		} else if (matpel != null) {
			tanyaMengajar = " matapelajaran " + matpel.getNama();
			tanyaAkhiran = " pada matapelajaran \"" + matpel.getNama() + "\"";
		}

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

			}
		};

		final Textbox tentangText = new Textbox(
				"Buatkan satu soal " + jenisPilihanGanda.getSelectedItem().getValue() + " tentang");

		jenisPilihanGanda.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				tentangText
						.setValue("Buatkan satu soal " + jenisPilihanGanda.getSelectedItem().getValue() + " tentang");
			}
		});

		fileupload.addEventListener("onClick",
				AIGenerator.generateApa("Generate Soal", "Soal tentang apa ?", tentangText, "", false, tanyaAkhiran,
						Common.getKonfigurasi("llama_system_buat_soal", "Kamu adalah Pengajar atau Dosen atau Guru ")
								.getNilai().trim(),
						soal, eventListener, tanyaMengajar, eventListener));

		return borderlayout;
	}

	private void init(final BankSoal bankSoal) throws Exception {

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		this.bankSoal = bankSoal;
		addWindow.setTitle(bankSoal.getId() == null ? "Tambah Bank Soal" : "Ubah Bank Soal");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		btnTabBankSoal = ais.ui.util.MyButtonTabbox.buat(Common.tampilanScrollTabbox(center), "100%", new int[] { 0 });

		{ org.zkoss.zul.Div panel = btnTabBankSoal.tambahTab(0, "Soal", "/img/svg/question-square.svg");
		  panel.appendChild(initMain(bankSoal)); }

		final org.zkoss.zul.Div panelJawaban = btnTabBankSoal.tambahTab(1, "Jawaban", "/img/svg/check2-circle.svg");
		panelJawaban.setStyle("min-height: 5470px;");
		btnTabBankSoal.onSetiapPilih(1, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(panelJawaban);
				if (!onSave(arg0)) {
					btnTabBankSoal.pilih(0);
					return;
				}

				BankSoalDao bankSoalDao = DaoFactory.getInstance().getBankSoalDao();
				if (BankSoalAction.this.bankSoal.getId() != null) {
					BankSoalAction.this.bankSoal = bankSoalDao.load(BankSoalAction.this.bankSoal.getId());

				}
				BankSoalDetailDetailAction bankSoalDetailDetailAction = new BankSoalDetailDetailAction(
						BankSoalAction.this.bankSoal);

				if (bankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA)
						&& (bankSoal.getJenisPilihanGanda().equals(BankSoal.MULTIPLE_COICE)
								|| bankSoal.getJenisPilihanGanda().equals(BankSoal.BENAR_SALAH))) {
					Radiogroup radiogroup = new Radiogroup();
					panelJawaban.appendChild(radiogroup);
					radiogroup.appendChild(bankSoalDetailDetailAction.display());
				} else {
					panelJawaban.appendChild(bankSoalDetailDetailAction.display());
				}

			}
		});

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		/*
		 * FIX "Soal harus diisi" padahal soal SUDAH diketik:
		 * CKEditor hanya mengirim isinya ke server saat kehilangan fokus (onChange/blur).
		 * Bila pengguna mengetik soal lalu LANGSUNG klik Simpan tanpa editor kehilangan
		 * fokus, nilai di server (soal.getValue()) masih kosong → validasi keliru menolak.
		 * Solusi: saat tombol Simpan diklik di CLIENT, paksa SEMUA instance CKEditor
		 * menyinkronkan isinya (updateElement + fire blur/change) LEBIH DAHULU — event ini
		 * dieksekusi sebelum perintah onClick dikirim ke server, sehingga saat onSave membaca
		 * soal.getValue() teks terbaru sudah masuk.
		 */
		save.setWidgetListener("onClick",
				"try{if(window.CKEDITOR){for(var k in CKEDITOR.instances){var e=CKEDITOR.instances[k];"
						+ "try{e.updateElement();}catch(x1){}try{e.fire('blur');}catch(x2){}try{e.fire('change');}catch(x3){}}}}catch(ex){}");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);

					if (eventListener != null) {
						eventListener.onEvent(new Event("", addWindow, BankSoalAction.this.bankSoal));
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		String isiSoal = soal == null || soal.getValue() == null ? "" : soal.getValue().trim();
		if (isiSoal.equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Soal",
					"Kolom Soal belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Soal.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (bankSoal.getId() != null) {
			bankSoal = (BankSoal) session.load(BankSoal.class, bankSoal.getId());

		}
		bankSoal.setMatapelajaran((Matapelajaran) (matapelajaran.getSelectedItem() == null ? null
				: matapelajaran.getSelectedItem().getValue()));
		bankSoal.setPenjelasanBankSoal(
				(PenjelasanBankSoal) penjelasanBankSoalBanbox.getAttribute("penjelasanBankSoal"));
		if (jenisKoreksi == null || jenisKoreksi.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Soal",
					"Jenis koreksi belum dipilih, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] { "Pilih jenis koreksi terlebih dahulu.",
							"Ulangi proses penyimpanan setelah jenis koreksi terisi." });
			return false;
		}
		bankSoal.setJenisKoreksi((String) jenisKoreksi.getSelectedItem().getValue());
		bankSoal.setSkorDefault(skorDefault.getValue());
		bankSoal.setSkorSalah(skorSalah.getValue());
		bankSoal.setGuru((Guru) guru.getAttribute("myValue"));
		bankSoal.setDosen((Dosen) dosen.getAttribute("myValue"));
		bankSoal.setMatakuliah((Matakuliah) matakuliah.getAttribute("matakuliah"));

		bankSoal.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		bankSoal.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		bankSoal.setSoal(soal.getValue());
		bankSoal.setKeterangan(keterangan.getValue());
		bankSoal.setJumlahLampiran(jumlahLampiran.isChecked() ? 1 : 0);
		bankSoal.setJenisPilihanGanda((String) (jenisPilihanGanda.getSelectedItem() == null ? null
				: jenisPilihanGanda.getSelectedItem().getValue()));
		bankSoal.setSkor(skor.getValue());

		bankSoal.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null ? null
						: yayasan.getSelectedItem().getValue()));
		bankSoal.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null ? null
						: sekolah.getSelectedItem().getValue()));

		bankSoal.setKategoriBankSoal((KategoriBankSoal) (kategoriBankSoal.getSelectedItem() == null
				|| kategoriBankSoal.getSelectedItem().getValue() == null ? null
						: kategoriBankSoal.getSelectedItem().getValue()));

		bankSoal.setJumlahJawabanDibatasi(jumlahJawabanDibatasi.isChecked());
		bankSoal.setMinimalJumlahJawaban(minimalJumlahJawaban.getValue());
		bankSoal.setMaksimalJumlahJawaban(maksimalJumlahJawaban.getValue());

		if (bankSoal.getId() != null) {
			Common.refreshUpdate(session, bankSoal);
		} else {
			session.save(bankSoal);
			session.flush();
		}

		if (bankSoal.getJenisPilihanGanda().equals(BankSoal.BENAR_SALAH)) {

			String huruf = "A";
			BankSoalDetail bankSoalDetail = (BankSoalDetail) session.createCriteria(BankSoalDetail.class)
					.add(Restrictions.eq("bankSoal", bankSoal)).add(Restrictions.ilike("huruf", huruf, MatchMode.EXACT))
					.setMaxResults(1).uniqueResult();
			if (bankSoalDetail == null) {
				bankSoalDetail = new BankSoalDetail();
				bankSoalDetail.setBetul(true);
			}
			bankSoalDetail.setHuruf(huruf);
			bankSoalDetail.setJawaban("Benar");
			bankSoalDetail.setBankSoal(bankSoal);
			Common.refreshSaveOrUpdate(session, bankSoalDetail);

			huruf = "B";
			bankSoalDetail = (BankSoalDetail) session.createCriteria(BankSoalDetail.class)
					.add(Restrictions.eq("bankSoal", bankSoal)).add(Restrictions.ilike("huruf", huruf, MatchMode.EXACT))
					.setMaxResults(1).uniqueResult();
			if (bankSoalDetail == null) {
				bankSoalDetail = new BankSoalDetail();
				bankSoalDetail.setBetul(false);
			}
			bankSoalDetail.setHuruf(huruf);
			bankSoalDetail.setJawaban("Salah");
			bankSoalDetail.setBankSoal(bankSoal);
			Common.refreshSaveOrUpdate(session, bankSoalDetail);

		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Session session = HibernateUtil.currentSession();
				if (bankSoalOriginal != null && bankSoalOriginal.getId() != null) {
					List<Long> bankSoalDetails = bankSoalOriginal.ambilBankSoalDetail(true);

					for (Long oriBankSoalDetailid : bankSoalDetails) {
						BankSoalDetail oriBankSoalDetail = (BankSoalDetail) GeneralValueObject
								.ambilData(BankSoalDetail.class, oriBankSoalDetailid.toString());
						if (oriBankSoalDetail != null) {

							BankSoalDetail newBankSoalDetail = (BankSoalDetail) oriBankSoalDetail.clone();
							newBankSoalDetail.setBankSoal(bankSoal);
							newBankSoalDetail.setId(null);

							String kodeUnik = newBankSoalDetail.getKodeUnik();
							if (((Number) session.createCriteria(BankSoalDetail.class)
									.add(Restrictions.eq("kodeUnik", kodeUnik)).setProjection(Projections.rowCount())
									.uniqueResult()).intValue() == 0) {
								session.save(newBankSoalDetail);
							}
						}
					}

				}

				if (bankSoalDetail != null && essay != null) {
					bankSoalDetail.setBankSoal(bankSoal);
					bankSoalDetail.setEssay(essay.getValue().trim());
					session.saveOrUpdate(bankSoalDetail);
				}

				try {
					session = StreamingHibernateUtil.getInstance().currentSession();

					if (lainSoal1 != null && lainSoal1.getId() != null) {
						session.refresh(lainSoal1);
						lainSoal1.setRef(bankSoal.getId());

						session.getTransaction().begin();
						session.update(lainSoal1);
						session.getTransaction().commit();
					}

					if (lainSoal2 != null && lainSoal2.getId() != null) {
						session.refresh(lainSoal2);
						lainSoal2.setRef(bankSoal.getId());

						session.getTransaction().begin();
						session.update(lainSoal2);
						session.getTransaction().commit();
					}

					if (lainSoal3 != null && lainSoal3.getId() != null) {
						session.refresh(lainSoal3);
						lainSoal3.setRef(bankSoal.getId());

						session.getTransaction().begin();
						session.update(lainSoal3);
						session.getTransaction().commit();
					}

					if (lainSoal4 != null && lainSoal4.getId() != null) {
						session.refresh(lainSoal4);
						lainSoal4.setRef(bankSoal.getId());

						session.getTransaction().begin();
						session.update(lainSoal4);
						session.getTransaction().commit();
					}

					if (lainSoal5 != null && lainSoal5.getId() != null) {
						session.refresh(lainSoal5);
						lainSoal5.setRef(bankSoal.getId());

						session.getTransaction().begin();
						session.update(lainSoal5);
						session.getTransaction().commit();
					}

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(BankSoal.class);
		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria

				.add((searchdosen == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchdosen.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("dosen", searchdosen.getAttribute("myValue"))))

				.add((searchguru == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchguru.getAttribute("guru") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("guru", searchguru.getAttribute("guru"))))

				.add((searchMatapelajaran == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchMatapelajaran.getAttribute("matapelajaran") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("matapelajaran", searchMatapelajaran.getAttribute("matapelajaran"))))

				.add((searchmatakuliah == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmatakuliah.getAttribute("matakuliah") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("matakuliah", searchmatakuliah.getAttribute("matakuliah"))))

				.add(searchkategoriBankSoal.getSelectedItem() == null
						|| searchkategoriBankSoal.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kategoriBankSoal",
										searchkategoriBankSoal.getSelectedItem().getValue()))

				.add(searchjenisPilihanGanda.getSelectedItem() == null
						|| searchjenisPilihanGanda.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisPilihanGanda",
										searchjenisPilihanGanda.getSelectedItem().getValue()))

				.add(searchjenis.getSelectedItem() == null || searchjenis.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisKoreksi", searchjenis.getSelectedItem().getValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))

				.add(searchsoal.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("soal", searchsoal.getValue().trim(), MatchMode.ANYWHERE));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchdosen == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);
		List<BankSoal> bankSoal = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(bankSoal);
		grid.setRowRenderer(new BankSoalRenderer());
		grid.setModel(strset);

	}

	/**
	 * Tipe implementasi bersarang {@link BankSoalDetailDetailAction} milik {@link BankSoalAction}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link BankSoalAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code BankSoal bankSoal}; operasi lokal:
	 * {@code display()}, {@code reloadDataFormula()}, {@code reloadFormula()}, {@code loadDetail}(). Aturan bisnis
	 * bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see BankSoalAction
	 */
	public class BankSoalDetailDetailAction {

		private BankSoal bankSoal;

		public BankSoalDetailDetailAction(BankSoal bankSoal) throws Exception {
			this.bankSoal = bankSoal;
		}

		public Borderlayout display() {

			Grid myGrid = new Grid();
			Rows myRows = new Rows();
			myRows.setParent(myGrid);

			MyFormRow myRow = new MyFormRow();
			myRow.setParent(myRows);

			Div div = new Div();
			div.setParent(myRow);

			div.appendChild(new ais.ui.util.MyHtml(bankSoal.getSoal()));
			BankSoalAction.tampilkanLampiran(null, bankSoal, div, false, div, div, div, div, div);

			final MyCheckboxConfig myCheckboxConfig = new MyCheckboxConfig(
					"Skor nilai berada di masing-masing jawaban");

			if (bankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA) && !bankSoal.getSoalMenjodohkan()
					&& !bankSoal.getJenisPilihanGanda().equals(BankSoal.COMBINATION_CHOICE)
					&& !bankSoal.getSoalMenjodohkan() && !bankSoal.getJenisPilihanGanda().equals(BankSoal.BENAR_SALAH)
					&& !bankSoal.getSoalMengurutkan()) {
				myRow = new MyFormRow();
				myRow.setParent(myRows);

				myRow.appendChild(myCheckboxConfig);
				myCheckboxConfig.setChecked(bankSoal.getSkorJawabanBerbeda());

			}

			final MyFormRow myRowOpsi = new MyFormRow();
			final MyFormRow myRowOpsiTabel = new MyFormRow();

			if (bankSoal.getSoalMenjodohkan()) {
				myRowOpsi.setParent(myRows);
				myRowOpsiTabel.setParent(myRows);
			}

			final MyFormRow myRowData = new MyFormRow();
			myRowData.setParent(myRows);
			loadDetail(myRowData, myRowOpsi, myRowOpsiTabel);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					bankSoal.setSkorJawabanBerbeda(myCheckboxConfig.isChecked());

					Common.refreshUpdate(bankSoal);

					loadDetail(myRowData, myRowOpsi, myRowOpsiTabel);
				}
			};

			myCheckboxConfig.addEventListener("onClick", eventListener);

			Borderlayout borderlayout = new Borderlayout();
			borderlayout.setStyle("min-height: 5470px;");
			Center center = new Center();
			center.setBorder("none");
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);
			center.appendChild(myGrid);

			return borderlayout;
		}

		public void reloadDataFormula(final Row rowU, final JSONArray array, final BankSoal bankSoal) throws Exception {
			Common.clear(rowU);

			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setWidth("100%");
			grid.setParent(rowU);
			grid.setWidth("100%");
			grid.setStyle("min-height:350px");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig("Opsi Jawaban");
			column.setParent(columns);
			column.setWidth("70%");

			column = new MyColumnConfig("Kode Opsi (A,B,C..)");
			column.setParent(columns);
			column.setWidth("20%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("10%");

			Rows rows = new Rows();
			rows.setParent(grid);

			for (int i = 0; i < array.length(); i++) {
				final int index = i;
				final JSONObject jsonObject = array.getJSONObject(i);

				if (jsonObject.isNull("key")) {
					continue;
				}

				String nama = "";

				if (!jsonObject.isNull("nama")) {
					nama = jsonObject.get("nama") + "";
				}

				String nomor = "";

				if (!jsonObject.isNull("nomor")) {
					nomor = jsonObject.get("nomor") + "";
				}

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				final MyTextbox targetText = new MyTextbox(nama);

				targetText.setWidth("95%");

				final MyTextbox nomorText = new MyTextbox(nomor);

				row.appendChild(targetText);
				row.appendChild(nomorText);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						jsonObject.put("nama", targetText.getValue());
						jsonObject.put("nomor", nomorText.getValue());

						if (bankSoal.getId() != null) {
							Session session = HibernateUtil.currentSession();
							session.refresh(bankSoal);
							bankSoal.setOpsiSoal(array.toString());
							Common.refreshUpdate(session, bankSoal);
							session.flush();
						}

					}
				};

				targetText.addEventListener("onChange", eventListener);

				nomorText.addEventListener("onChange", eventListener);

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

												if (bankSoal.getId() != null) {
													Session session = HibernateUtil.currentSession();
													session.refresh(bankSoal);
													bankSoal.setOpsiSoal(array.toString());
													Common.refreshUpdate(session, bankSoal);
													session.flush();
												}

												reloadDataFormula(rowU, array, bankSoal);

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

		public void reloadFormula(final Row rowFormula, final Row rowU, final JSONArray array, final BankSoal bankSoal)
				throws Exception {

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Opsi Jawaban", "/img/svg/addthis.svg");
			button.setTooltiptext("Hapus Data");

			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("nama", "");

					Long key = Math.abs(Common.randLong());
					jsonObject.put("key", key);
					array.put(jsonObject);

					reloadDataFormula(rowU, array, bankSoal);
				}
			});
			button.setParent(rowFormula);

			reloadDataFormula(rowU, array, bankSoal);

		}

		private void loadDetail(final Row myRow, final Row myRowOpsi, final Row myRowOpsiTabel) {
			Common.clear(myRow);
			Common.clear(myRowOpsi);

			if (bankSoal.getSoalMenjodohkan()) {

				try {
					reloadFormula(myRowOpsi, myRowOpsiTabel, new JSONArray(bankSoal.getOpsiSoal()), bankSoal);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					ais.common.Common.tampilErrorJikaAdmin(e);
				}

			}

			if (bankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA)) {

				Grid grid = new Grid();
				grid.setSclass("dgrid");
				grid.setWidth("100%");
				grid.setParent(myRow);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				List<String> jawaban = new ArrayList<String>();

				if (bankSoal.getJenisPilihanGanda().equals(BankSoal.BENAR_SALAH)) {
					MyColumnConfig column = new MyColumnConfig("No.");
					column.setParent(columns);

					column = new MyColumnConfig("Jawaban");
					column.setParent(columns);
					column.setWidth("75%");

					column = new MyColumnConfig("Benar/Salah");
					column.setParent(columns);

					jawaban.add("A");
					jawaban.add("B");
				}

				else if (bankSoal.getJenisPilihanGanda().equals(BankSoal.JAWABAN_SINGKAT)
						|| bankSoal.getJenisPilihanGanda().equals(BankSoal.RUMPANG)) {

					MyColumnConfig column = new MyColumnConfig("No.");
					column.setParent(columns);
					column.setWidth(bankSoal.getJenisPilihanGanda().equals(BankSoal.JAWABAN_SINGKAT) ? "0px" : "50px");

					column = new MyColumnConfig("Jawaban");
					column.setParent(columns);
					column.setWidth(bankSoal.getSkorJawabanBerbeda() ? "85%" : "95%");

					for (int i = 1; i <= (bankSoal.getJenisPilihanGanda().equals(BankSoal.RUMPANG) ? 20 : 1); i++) {
						jawaban.add(i + "");
					}

					if (bankSoal.getSkorJawabanBerbeda()) {
						column = new MyColumnConfig("Skor");
						column.setParent(columns);
						column.setWidth("10%");
					}

				} else {

					MyColumnConfig column = new MyColumnConfig("Huruf");
					column.setParent(columns);
					column.setWidth("60px");

					if (bankSoal.getSkorJawabanBerbeda()) {
						column = new MyColumnConfig("Jawaban");
						column.setParent(columns);
						column.setWidth("60%");

						if (bankSoal.getSoalMengurutkan()) {
							column = new MyColumnConfig("Urutan benar");
							column.setParent(columns);
							column = new MyColumnConfig("Urutan saat diujikan");
							column.setParent(columns);
						} else if (!bankSoal.getSoalMenjodohkan()) {
							column = new MyColumnConfig("Benar/Salah");
							column.setParent(columns);
						} else {
							column.setWidth("90%");
						}

						column = new MyColumnConfig("Skor");
						column.setParent(columns);
						column.setWidth("10%");
					} else {
						column = new MyColumnConfig("Jawaban");
						column.setParent(columns);
						column.setWidth("70%");

						if (bankSoal.getSoalMengurutkan()) {
							column = new MyColumnConfig("Urutan benar");
							column.setParent(columns);
							column = new MyColumnConfig("Urutan saat diujikan");
							column.setParent(columns);
						} else if (!bankSoal.getSoalMenjodohkan()) {
							column = new MyColumnConfig("Benar/Salah");
							column.setParent(columns);
						} else {
							column.setWidth("100%");
						}
					}

					jawaban.add("A");
					jawaban.add("B");
					jawaban.add("C");
					jawaban.add("D");
					jawaban.add("E");
					jawaban.add("F");
					jawaban.add("G");
					jawaban.add("H");
					jawaban.add("I");
					jawaban.add("J");
				}

				Session session = HibernateUtil.currentSession();
				Rows rows = grid.getRows() == null ? new Rows() : grid.getRows();
				grid.appendChild(rows);
				for (final String huruf : jawaban) {
					final BankSoalDetail bankSoalDetail = (BankSoalDetail) session.createCriteria(BankSoalDetail.class)
							.add(Restrictions.eq("bankSoal", bankSoal))
							.add(Restrictions.ilike("huruf", huruf, MatchMode.EXACT)).setMaxResults(1).uniqueResult();

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);

					if (bankSoalDetail != null) {
						try {
							RevisiHelper.createNewRevisi(BankSoalDetail.class, bankSoalDetail, huruf).setParent(row);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
					} else {
						row.appendChild(new ais.ui.util.MyLabelConfig(huruf));
					}

					final Label benarSalah = bankSoal.getJenisPilihanGanda().equals(BankSoal.BENAR_SALAH)
							? (new Label(huruf.equals("A") ? "Benar" : "Salah"))
							: null;

					final Textbox textbox = new Textbox(bankSoalDetail == null ? "" : bankSoalDetail.getJawaban());

					final Checkbox benar = bankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA)
							&& (bankSoal.getJenisPilihanGanda().equals(BankSoal.MULTIPLE_COICE)
									|| bankSoal.getJenisPilihanGanda().equals(BankSoal.BENAR_SALAH))
											? new MyRadioConfig("Benar")
											: new MyCheckboxConfig("Benar");
					benar.setChecked(bankSoalDetail == null ? false : bankSoalDetail.getBetul());

					final MyIntbox urutanBenar = new MyIntbox(
							bankSoalDetail == null ? 1 : bankSoalDetail.getUrutanBenar());
					final MyIntbox urutanDiujikan = new MyIntbox(
							bankSoalDetail == null ? 1 : bankSoalDetail.getUrutanDiujikan());

					final MyDoublebox intbox = new MyDoublebox(bankSoalDetail == null ? 1.0 : bankSoalDetail.getSkor());

					final EventListener eventListener = new EventListener() {

						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event arg0) throws Exception {

							LampiranLain lampiranLain = LampiranLain.ambil(bankSoal.getId(), "Gambar_Jawaban_" + huruf);

							Session session = HibernateUtil.currentSession();
							BankSoalDetail bankSoalDetail = (BankSoalDetail) session
									.createCriteria(BankSoalDetail.class).add(Restrictions.eq("bankSoal", bankSoal))
									.add(Restrictions.ilike("huruf", huruf, MatchMode.EXACT)).setMaxResults(1)
									.uniqueResult();
							if (benarSalah == null && textbox.getValue().trim().isEmpty() && lampiranLain == null) {
								if (bankSoalDetail != null) {
									Common.refreshDelete(session, bankSoalDetail);
								}
							} else {
								if (bankSoalDetail == null) {
									bankSoalDetail = new BankSoalDetail();
								}
								bankSoalDetail.setHuruf(huruf);
								bankSoalDetail.setSkor(intbox.getValue());
								bankSoalDetail.setUrutanBenar(urutanBenar.getValue());
								bankSoalDetail.setUrutanDiujikan(urutanDiujikan.getValue());
								bankSoalDetail.setJawaban(benarSalah != null ? benarSalah.getValue()
										: textbox == null ? null : textbox.getValue().trim());
								bankSoalDetail.setBetul(benar.isChecked());
								bankSoalDetail.setBankSoal(bankSoal);
								Common.refreshSaveOrUpdate(session, bankSoalDetail);
							}

							if (bankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA)
									&& (bankSoal.getJenisPilihanGanda().equals(BankSoal.MULTIPLE_COICE)
											|| bankSoal.getJenisPilihanGanda().equals(BankSoal.BENAR_SALAH))) {
								List<BankSoalDetail> bankSoalDetails = session.createCriteria(BankSoalDetail.class)
										.add(Restrictions.eq("bankSoal", bankSoal))
										.add(Restrictions.not(Restrictions.ilike("huruf", huruf, MatchMode.EXACT)))
										.list();
								for (BankSoalDetail b : bankSoalDetails) {
									b.setBetul(false);
									Common.refreshUpdate(session, b);
								}
							}
						}
					};

					textbox.addEventListener("onChange", eventListener);

					if (bankSoal.getJenisPilihanGanda().equals(BankSoal.BENAR_SALAH)) {
						row.appendChild(benarSalah);
						benar.addEventListener("onClick", eventListener);
						row.appendChild(benar);
					}

					else if (bankSoal.getJenisPilihanGanda().equals(BankSoal.JAWABAN_SINGKAT)) {
						row.appendChild(textbox);
						textbox.setWidth("95%");

					} else {

						Vbox vbox = new Vbox();
						vbox.setWidth("100%");
						vbox.setParent(row);

						vbox.appendChild(textbox);

						Hbox hbox = new Hbox();
						hbox.setWidth("100%");
						hbox.setParent(vbox);

						LampiranLain.createDownloadUploadFileLain(hbox, bankSoal.getId(), "Gambar_Jawaban_" + huruf,
								"Gambar Jawaban", false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										eventListener.onEvent(arg0);
										loadDetail(myRow, myRowOpsi, myRowOpsiTabel);
									}
								}, null, false, false, false, true, null, false, false);

						benar.addEventListener("onClick", eventListener);
						textbox.setWidth("95%");
						textbox.setRows(2);

						if (bankSoal.getSoalMengurutkan()) {

							urutanBenar.setWidth("90%");
							urutanBenar.setParent(row);

							urutanDiujikan.setWidth("90%");
							urutanDiujikan.setParent(row);

							urutanBenar.addEventListener("onChange", eventListener);
							urutanDiujikan.addEventListener("onChange", eventListener);

						} else {

							row.appendChild(benar);
						}

					}

					if (bankSoal.getSkorJawabanBerbeda()) {

						intbox.setWidth("90%");
						intbox.setParent(row);
						intbox.addEventListener("onChange", eventListener);

					}
				}

			} else if (bankSoal.getJenis().equals(BankSoal.ESAY)) {

				Session session = HibernateUtil.currentSession();
				bankSoalDetail = (BankSoalDetail) session.createCriteria(BankSoalDetail.class)
						.add(Restrictions.isNotNull("essay")).add(Restrictions.eq("bankSoal", bankSoal))
						.setMaxResults(1).uniqueResult();

				if (bankSoalDetail == null) {
					bankSoalDetail = new BankSoalDetail();
				}

				myRow.appendChild(essay = new Textbox());
				essay.setValue(bankSoalDetail.getEssay());
				essay.setWidth("90%");
				essay.setRows(bankSoal.getJenis().equals(BankSoal.ESAY) ? 10 : 1);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						BankSoalDetail bankSoalDetail = (BankSoalDetail) session.createCriteria(BankSoalDetail.class)
								.add(Restrictions.eq("bankSoal", bankSoal)).setMaxResults(1).uniqueResult();
						if (bankSoalDetail == null) {
							bankSoalDetail = new BankSoalDetail();
						}
						bankSoalDetail.setEssay(essay.getValue().trim());
						bankSoalDetail.setBankSoal(bankSoal);
						Common.refreshSaveOrUpdate(session, bankSoalDetail);
					}
				};
				essay.addEventListener("onChange", eventListener);
			}
		}

	}

}
