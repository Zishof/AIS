package ais.action.master;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.jsoup.Jsoup;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.ClientInfoEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Div;
import org.zkoss.zul.East;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;
import org.zkoss.zul.Window;

import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.dashboard.admin.DasborInfoDanMateri;
import ais.action.master.dashboard.admin.DashboardTrenAktivitasPerkuliahan;
import ais.action.master.helper.BukuBahanAjarHelper;
import ais.action.master.helper.CalendarPerkuliahanMingguIniComposer;
import ais.action.master.helper.DataPunyaArtikelHelper;
import ais.action.master.helper.PerkuliahanPunyaItemHelper;
import ais.action.master.helper.PertemuanHelper;
import ais.action.master.helper.PertemuanPunyaDiskusiHelper;
import ais.action.master.helper.PertemuanPunyaUjianHelper;
import ais.action.master.helper.ProsesUjianHelper;
import ais.action.master.helper.RekapitulasiAudioHelper;
import ais.action.master.helper.RekapitulasiMateriHelper;
import ais.action.master.helper.RekapitulasiPerkuliahanHelper;
import ais.action.master.helper.RekapitulasiTugasHelper;
import ais.action.master.helper.RekapitulasiTugasKelompokHelper;
import ais.action.master.helper.RekapitulasiUjianHelper;
import ais.action.master.helper.RekapitulasiVideoHelper;
import ais.action.master.helper.profile.ProfileUtil;
import ais.action.master.sekolah.helper.RekapitulasiJadwalPelajaranHelper;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.maintenance.MainProgressHelper;
import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanPendidikanLingkunganKampus;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.FormulirKegiatan;
import ais.database.model.FormulirKegiatanPeserta;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.HasilUjianMahasiswaDetail;
import ais.database.model.JenisFormulirKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaDiskusi;
import ais.database.model.PertemuanPunyaGrupPertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.RuangPaketPMB;
import ais.database.model.Skripsi;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.VOPembelajaran;
import ais.database.model.Wisuda;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.file.TugasFileContent;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfigKecil;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyLabelKecilBold;
import ais.ui.util.MyMenuitem;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.SmartDateTimeUtil;
import ais.ui.util.UIUtil;
import ais.ui.util.WaktuUtil;

import org.zkoss.zul.Html;
public class TampilanELearningAction extends GenericAutowireComposer {

	/**
	 *
	 */
	private static final long serialVersionUID = -8483617001982354763L;
	private Tbmuser tbmuser;
	private Mahasiswa mahasiswa;
	private LayoutRegion menu = null;
	private LayoutRegion menuKanan = null;
	private ais.ui.util.MyButtonTabbox btnAktivitas;
	private ais.ui.util.MyButtonTabbox btnKanan;

	private Integer jumlahDataDalamSatuHalamanElearning = 15;
	private boolean tampilkanPilihanTaDiPerkuliakan = false;

	public static final Integer PERKULIAHAN = 1;
	public static final Integer SKRIPSI = 2;
	public static final Integer BIMBINGAN = 3;
	public static final Integer KKN = 4;
	public static final Integer PKL = 5;
	public static final Integer KRS = 6;
	public static final Integer KONSULTASI = 7;
	public static final Integer PELAJARAN = 8;
	public static final Integer KEGIATAN = 9;
	public static final Integer WISUDA = 10;

	public static final Integer MATERI = 100;
	public static final Integer KOMENTAR = 101;

	@SuppressWarnings("rawtypes")
	private static final java.util.concurrent.ConcurrentHashMap<String, java.util.List> TELA_CACHE
			= new java.util.concurrent.ConcurrentHashMap<String, java.util.List>();
	private static final java.util.concurrent.ConcurrentHashMap<String, Long> TELA_EXPIRY
			= new java.util.concurrent.ConcurrentHashMap<String, Long>();
	private static final long TELA_TTL_MS = 5L * 60 * 1000;

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
		tbmuser = Common.getCurrentUser();
		if (tbmuser == null || tbmuser.getUserId() == null) {
			return;
		}
		mahasiswa = tbmuser.getMahasiswa();

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];
		tampilkanPilihanTaDiPerkuliakan = Common.bolehKonfigurasi("tampilkanPilihanTaDiPerkuliakan", Konfigurasi.TIDAK_AKTIF);

		/* init() dulu ditunda ke onInfo (ClientInfoEvent) untuk klien mobile.
		 * Saat halaman ini di-include ke tab MainAction (desktop ZK yang sama
		 * dan sudah ter-render), onClientInfo tidak pernah dikirim ulang oleh
		 * client sehingga dasbor tampil kosong di tampilan mobile. init()
		 * sekarang selalu dijalankan di sini; onInfo tetap dipakai halaman
		 * yang berdiri sendiri dan aman dari double-init (guard menu != null). */
		init();
	}

	private Integer desktopHeight = null;
	private Integer desktopWidth = null;

	private org.zkoss.zk.ui.Component tabpanelTimeline;
	private MyTabConfig tabPerkuliahan;
	private MyTabConfig tabPelajaran;
	private MyTabConfig tabSidang;
	private MyTabConfig tabBimbingan;
	private MyTabConfig tabKkn;
	private MyTabConfig tabPkl;
	private MyTabConfig tabPA;
	private MyTabConfig tabKegiatan;
	private List<MyTabConfig> tabKegiatans;
	private MyTabConfig tabKonsultasi;
	private MyTabConfig tabWisuda;

	// private MyTabConfig tabMateri;
	private EventListener eventListenerMateri = null;
	private boolean reloadBlnSd = false;
	private boolean mobileTampilan;
	// private Tabpanel tabpanelUtamaKanan;
	// private Tabpanel tabpanelUtamaPerkuliahan;
	private Div centerMateri;
	private ais.ui.util.MyHtml loadingAwalTengah;
	private ais.ui.util.MyHtml loadingAwalKanan;
	private Div southKomentar;
	private Tabpanel tabpanelUtamaPerkuliahan;
	private boolean pt = false;
	private boolean ya = false;

	private Sekolah sekolah = null;

	public void onInfo(ClientInfoEvent evt) throws Exception {

		if (menu != null || !Common.isMobile()) {
			return;
		}

		desktopHeight = evt.getDesktopHeight();
		desktopWidth = evt.getDesktopWidth();

		System.out.println("desktopHeight => " + desktopHeight + ", desktopWidth => " + desktopWidth);

		init();
	}

	private void init() throws Exception {
		mobileTampilan = isMobileView();
		if (mobileTampilan) {
			/* Mobile: panel dikonversi menjadi tab (Agenda, Linimasa,
			 * Tgs/Ujian/Materi) agar tidak tampak kecil-kecil. */
			initRootLayout();
			initMobileMenuPanel();
		} else {
			/* Desktop: 3 panel ditata memakai MyPortallayout modern
			 * (mengikuti pola ais.action.master.sop.helper.DasboardSop). */
			initPortalRootLayout();
		}
		initLearningTabs();
		initInfoMateriTabs();
		initKalenderTabs();
		initDasborTabs();
		// initObeTabs() DIGABUNG: "Dasbor OBE" kini sub-tab di dalam tab "Dasbor" (lihat initDasborTabs).
		initBimbinganTab();
		loadMenu();
	}

	private Borderlayout initRootLayout() throws Exception {
		sekolah = SekolahUtil.getSekolah();

		/* Mobile: tanpa tinggi eksplisit, Borderlayout flex di dalam rantai
		 * height:auto (mode satu scroll) kolaps menjadi 0px sehingga seluruh
		 * konten e-learning tidak terlihat. Pakai pola yang sama dengan
		 * desktop: shell div + tinggi panel dari konfigurasi frame. */
		int tinggiMinimal = ais.action.maintenance.MainAction.getConfiguredFrameMinimumHeight(Common.isMobile()) - 190;
		if (tinggiMinimal < 480) {
			tinggiMinimal = 480;
		}
		tinggiKanvasMobile = tinggiMinimal;

		Div shell = new Div();
		shell.setSclass("elearning-portal-shell elearning-mobile-shell");
		shell.setWidth("100%");
		shell.setParent(page.getFirstRoot());

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight(tinggiMinimal + "px");
		borderlayout.setParent(shell);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		btnAktivitas = ais.ui.util.MyButtonTabbox.buat(center, tinggiMinimal + "px", null);

		pasangAutoFitTinggiPortal(tinggiMinimal);
		return borderlayout;
	}

	/**
	 * Layout desktop berbasis MyPortallayout: tiga kolom (Perkuliahan | Aktivitas
	 * Pembelajaran | Tugas-Ujian-Materi). Panel dapat di-drag antar kolom dan
	 * di-maximize; styling terpusat di css_utama.css blok "elearning-portal".
	 */
	private void initPortalRootLayout() throws Exception {
		sekolah = SekolahUtil.getSekolah();

		/* Tinggi minimal panel mengikuti konfigurasi tinggi frame aplikasi
		 * (sama dengan dashboard utama), dikurangi perkiraan tinggi header. */
		int tinggiMinimal = ais.action.maintenance.MainAction.getConfiguredFrameMinimumHeight(Common.isMobile()) - 190;
		if (tinggiMinimal < 480) {
			tinggiMinimal = 480;
		}
		String tinggiPanel = tinggiMinimal + "px";

		Div shell = new Div();
		shell.setSclass("elearning-portal-shell");
		shell.setParent(page.getFirstRoot());

		MyPortallayout portal = new MyPortallayout();
		portal.setWidth("100%");
		portal.setSclass("elearning-portal");
		portal.setParent(shell);

		MyPortalchildren kolomKiri = buatKolomPortal(portal, "25%");
		MyPortalchildren kolomTengah = buatKolomPortal(portal, "50%");
		MyPortalchildren kolomKanan = buatKolomPortal(portal, "25%");

		/* Kolom kiri: daftar perkuliahan/pelajaran/sidang/dst. */
		Panelchildren isiKiri = buatPanelPortal(kolomKiri, "Perkuliahan & Kelas");
		Borderlayout layoutKiri = new Borderlayout();
		layoutKiri.setSclass("elearning-portal-body");
		layoutKiri.setWidth("100%");
		layoutKiri.setHeight(tinggiPanel);
		layoutKiri.setParent(isiKiri);

		// RESTRUKTURISASI ANDAL: gunakan region CENTER (bukan West) untuk isi panel kiri. Borderlayout
		// yang HANYA berisi satu region SISI (West/East) tanpa Center adalah bentuk MALFORMED di ZK —
		// region sisi tersebut kerap KOLAPS (lebar/tinggi 0) sehingga panel kiri/kanan KOSONG, terutama
		// saat HP membuka Mode Desktop (viewport lebar-scaled). Region CENTER SELALU mengisi penuh
		// panel (pola yang sama dg panel tengah yang tak pernah gagal). Pemuatan data tetap lewat
		// jalankanLoadAwalElearning()/Timer (tidak bergantung ON_OPEN region).
		Center menuKiri = new Center();
		menuKiri.setBorder("none");
		ais.ui.util.ZkCompat.setFlex(menuKiri, true);
		menuKiri.setParent(layoutKiri);
		menu = menuKiri;

		/* Kolom tengah: tab Linimasa/Ringkasan/Info & Materi/Kalender/Dasbor. */
		Panelchildren isiTengah = buatPanelPortal(kolomTengah, "Aktivitas Pembelajaran");
		Div bodyTengah = new Div();
		bodyTengah.setSclass("elearning-portal-body");
		bodyTengah.setWidth("100%");
		bodyTengah.setHeight(tinggiPanel);
		bodyTengah.setParent(isiTengah);

		btnAktivitas = ais.ui.util.MyButtonTabbox.buat(bodyTengah, tinggiPanel, null);

		/* Kolom kanan: pencarian + tugas/ujian/materi + diskusi. */
		Panelchildren isiKanan = buatPanelPortal(kolomKanan, "Tugas, Ujian, Materi & Diskusi");
		Borderlayout layoutKanan = new Borderlayout();
		layoutKanan.setSclass("elearning-portal-body");
		layoutKanan.setWidth("100%");
		layoutKanan.setHeight(tinggiPanel);
		layoutKanan.setParent(isiKanan);

		// Sama seperti panel kiri: region CENTER agar isi panel kanan ("Tugas, Ujian, Materi & Diskusi")
		// SELALU mengisi penuh & tidak kolaps pada mode mobile-versi-desktop. Center.isOpen() selalu
		// true → kondisi pemuatan panel kanan (eventListenerMateri via picuKontenPanelKanan) makin andal.
		Center panelKanan = new Center();
		panelKanan.setBorder("none");
		ais.ui.util.ZkCompat.setFlex(panelKanan, true);
		panelKanan.setParent(layoutKanan);
		menuKanan = panelKanan;

		pasangAutoFitTinggiPortal(tinggiMinimal);
	}

	/**
	 * Mode satu scroll: scroll milik html/body halaman e-learning dimatikan,
	 * lalu .elearning-portal-shell dipaskan setinggi viewport dan menjadi
	 * SATU-SATUNYA scroller vertikal. Panel-panel di dalamnya tingginya
	 * sudah dipatok dari Java (tinggiMinimal) sehingga konten panjang
	 * di-scroll lewat shell, bukan scrollbar kecil per panel.
	 */
	private void pasangAutoFitTinggiPortal(int tinggiMinimal) {
		try {
			StringBuilder js = new StringBuilder();
			js.append("(function(){");
			js.append("function fit(){try{");
			js.append("var shell=document.querySelector('.elearning-portal-shell');if(!shell){return;}");
			/* Cari panel parent utama dari MainAction (createDashboardFrame). */
			js.append("var host=null,n=shell.parentNode;");
			js.append("while(n&&n!==document){var cn=' '+(n.className||'')+' ';");
			js.append("if(cn.indexOf(' main-dashboard-content ')>=0||cn.indexOf(' main-dashboard-frame ')>=0){host=n;break;}");
			js.append("n=n.parentNode;}");
			js.append("if(host){");
			/* Embedded di MainAction: scroll disatukan ke container root.
			   Shell dan semua ancestor sampai main-dashboard-frame dibuat
			   tinggi otomatis tanpa scrollbar sendiri sehingga satu-satunya
			   scroll adalah milik area tab utama MainAction. */
			js.append("shell.style.height='auto';");
			js.append("shell.style.overflowY='visible';");
			js.append("shell.style.overflowX='hidden';");
			js.append("var up=shell.parentNode;");
			js.append("while(up&&up!==document){");
			js.append("try{up.style.height='auto';up.style.maxHeight='none';up.style.minHeight='0';up.style.overflowY='visible';}catch(ex){}");
			js.append("var cu=' '+(up.className||'')+' ';");
			js.append("if(cu.indexOf(' main-dashboard-frame ')>=0){break;}");
			js.append("up=up.parentNode;}");
			js.append("}else{");
			/* Berdiri sendiri (halaman penuh): pakai viewport dan matikan
			   scroll dokumen agar hanya shell yang scroll. */
			js.append("document.documentElement.style.overflow='hidden';");
			js.append("if(document.body){document.body.style.overflow='hidden';}");
			js.append("var r=shell.getBoundingClientRect();");
			js.append("var vh=window.innerHeight||document.documentElement.clientHeight;");
			js.append("var h=Math.floor(vh-r.top-2);");
			js.append("if(h<300){h=300;}");
			js.append("shell.style.height=h+'px';");
			js.append("shell.style.overflowY='auto';");
			js.append("shell.style.overflowX='hidden';");
			js.append("}");
			js.append("try{if(window.zUtl){zUtl.fireSized();}}catch(e){}");
			js.append("}catch(e){}}");
			js.append("fit();setTimeout(fit,250);setTimeout(fit,900);setTimeout(fit,2000);");
			js.append("try{if(!window.ecampusElearningSingleScroll){window.ecampusElearningSingleScroll=true;");
			js.append("window.addEventListener('resize',fit);");
			js.append("}}catch(e){}");
			js.append("})();");
			Clients.evalJavaScript(js.toString());
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	private MyPortalchildren buatKolomPortal(MyPortallayout portal, String lebar) {
		MyPortalchildren kolom = new MyPortalchildren();
		kolom.setWidth(lebar);
		kolom.setSclass("elearning-portal-col");
		kolom.setParent(portal);
		return kolom;
	}

	private Panelchildren buatPanelPortal(MyPortalchildren kolom, String judul) {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMinimizable(false);
		panel.setSclass("elearning-portal-panel");
		panel.setParent(kolom);

		/*
		 * Header panel: judul + tombol Layar Penuh (maximize bawaan ZK) +
		 * tombol Bantuan (panduan khusus panel). Dipasang SEBELUM Panelchildren
		 * karena Caption wajib menjadi anak pertama Panel. Dengan ini SEMUA
		 * panel portal punya kontrol seragam: perbesar ke layar penuh & buka
		 * panduan kapan saja.
		 */
		ais.ui.util.PanelToolHelper.pasangBantuanDanLayarPenuh(panel, judul, bantuanUntukPanel(judul));

		Panelchildren isi = new Panelchildren();
		isi.setSclass("elearning-portal-panel-body");
		isi.setParent(panel);
		return isi;
	}

	/**
	 * Mengembalikan konten HTML panduan (Bantuan) untuk tiap panel portal,
	 * dicocokkan berdasarkan judul panel. Bahasa sederhana untuk pengguna akhir,
	 * minimal 500 kata per panel, mengikuti template Bantuan pada AgamaAction.
	 */
	private String bantuanUntukPanel(String judul) {
		String j = judul == null ? "" : judul.trim();
		if (j.startsWith("Perkuliahan")) {
			return bantuanPanelPerkuliahan();
		}
		if (j.startsWith("Aktivitas")) {
			return bantuanPanelAktivitas();
		}
		if (j.startsWith("Tugas")) {
			return bantuanPanelTugas();
		}
		return bantuanPanelUmum(j);
	}

	private static final String HELP_WRAP_BUKA =
			"<div style='font-family:sans-serif;font-size:13px;line-height:1.8;color:#1e293b;'>";
	private static final String HELP_H3_BUKA =
			"<h3 style='margin-top:0;color:#1d4ed8;border-bottom:2px solid #dbeafe;padding-bottom:6px;'>";
	private static final String HELP_H4 = "<h4 style='color:#0f172a;margin-bottom:4px;'>";
	private static final String HELP_TR_HEAD = "<tr style='background:#f1f5f9;'>";
	private static final String HELP_TH =
			"<th style='border:1px solid #cbd5e1;padding:5px 8px;text-align:left;'>";
	private static final String HELP_TD = "<td style='border:1px solid #cbd5e1;padding:5px 8px;'>";

	/** Penjelasan dua tombol baru pada header panel; dipakai ulang di semua panel. */
	private String bantuanBlokTombolHeader() {
		return HELP_H4 + "&#128208; Dua Tombol di Pojok Kanan Atas Panel</h4>"
				+ "<ul style='margin-top:0;padding-left:18px;'>"
				+ "<li><b>Layar Penuh</b> (ikon kotak): klik untuk memperbesar panel ini "
				+ "memenuhi layar agar isinya lebih jelas dan mudah dibaca. Klik sekali lagi "
				+ "untuk mengembalikan ke ukuran semula.</li>"
				+ "<li><b>Bantuan</b> (ikon tanda tanya): klik untuk membuka kembali panduan "
				+ "ini kapan saja. Tutup panduan dengan tombol <b>Tutup</b> di bawah.</li>"
				+ "</ul>";
	}

	private String bantuanPanelPerkuliahan() {
		StringBuilder sb = new StringBuilder(4096);
		sb.append(HELP_WRAP_BUKA);
		sb.append(HELP_H3_BUKA).append("&#127979; Panduan &mdash; Perkuliahan &amp; Kelas</h3>");

		sb.append("<p>Panel ini adalah <b>daftar kelas Anda</b> pada semester yang sedang "
				+ "berjalan (contoh: 2025/2026 Ganjil). Bagi <b>dosen</b>, di sini tampil semua "
				+ "mata kuliah yang Anda ampu. Bagi <b>mahasiswa</b>, di sini tampil mata kuliah "
				+ "yang Anda ikuti. Setiap kelas ditampilkan sebagai satu kartu yang memuat nama "
				+ "mata kuliah, jumlah SKS, nama kelas, nama dosen pengampu, serta hari dan jam "
				+ "kuliah. Dari panel ini Anda bisa langsung melihat sejauh mana perkuliahan "
				+ "sudah berjalan tanpa perlu membuka menu lain.</p>");

		sb.append("<p>Di sisi kiri panel terdapat <b>menu tegak</b> berisi kelompok kegiatan "
				+ "akademik. Klik salah satu menu untuk mengganti isi daftar di sebelahnya. "
				+ "Berikut arti masing-masing menu:</p>");

		sb.append("<table style='border-collapse:collapse;width:100%;margin-bottom:10px;'>");
		sb.append(HELP_TR_HEAD).append(HELP_TH).append("Menu</th>").append(HELP_TH)
				.append("Untuk apa</th></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Perkuliahan</b></td>").append(HELP_TD)
				.append("Daftar mata kuliah perguruan tinggi pada semester aktif.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Pelajaran</b></td>").append(HELP_TD)
				.append("Daftar mata pelajaran (untuk jenjang sekolah).</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Sidang</b></td>").append(HELP_TD)
				.append("Jadwal dan kelas terkait sidang/ujian akhir.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Bimbingan</b></td>").append(HELP_TD)
				.append("Kegiatan bimbingan, misalnya bimbingan tugas akhir/skripsi.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>KKN</b></td>").append(HELP_TD)
				.append("Kuliah Kerja Nyata yang Anda ikuti atau bimbing.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>PKL</b></td>").append(HELP_TD)
				.append("Praktik Kerja Lapangan/magang.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Pembimbing Akademik</b></td>").append(HELP_TD)
				.append("Daftar mahasiswa bimbingan akademik (untuk dosen wali).</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Kegiatan Lain</b></td>").append(HELP_TD)
				.append("Kegiatan akademik lain di luar perkuliahan reguler.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Konsultasi</b></td>").append(HELP_TD)
				.append("Sesi konsultasi mahasiswa dengan dosen.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Wisuda</b></td>").append(HELP_TD)
				.append("Informasi terkait kelulusan dan wisuda.</td></tr>");
		sb.append("</table>");

		sb.append(HELP_H4).append("&#128202; Membaca Kartu Kelas</h4>");
		sb.append("<p>Pada setiap kartu kelas terdapat angka kemajuan yang membantu Anda memantau "
				+ "perkuliahan secara cepat:</p>");
		sb.append("<ul style='margin-top:0;padding-left:18px;'>");
		sb.append("<li><b>Tuntas</b> &mdash; berapa pertemuan yang sudah terlaksana dibanding "
				+ "target, contoh <i>16/16 (100%)</i> berarti seluruh 16 pertemuan sudah "
				+ "dilaksanakan.</li>");
		sb.append("<li><b>Hdr mhs</b> &mdash; ringkasan kehadiran mahasiswa di kelas tersebut.</li>");
		sb.append("<li><b>Hdr dosen</b> &mdash; ringkasan kehadiran dosen pengampu.</li>");
		sb.append("<li><b>Mahasiswa : M=...</b> &mdash; jumlah mahasiswa yang tercatat hadir "
				+ "(M = Masuk/Hadir) pada kelas itu.</li>");
		sb.append("</ul>");

		sb.append(HELP_H4).append("&#128269; Mencari dan Menyegarkan Data</h4>");
		sb.append("<ul style='margin-top:0;padding-left:18px;'>");
		sb.append("<li>Gunakan kolom <b>Pencarian</b> di atas daftar untuk menyaring kelas "
				+ "berdasarkan nama mata kuliah atau kelas. Tidak perlu mengetik lengkap, "
				+ "sebagian kata sudah cukup, dan tidak membedakan huruf besar/kecil.</li>");
		sb.append("<li>Klik <b>Refresh</b> bila data terasa belum berubah setelah Anda "
				+ "melakukan presensi atau perubahan jadwal di menu lain.</li>");
		sb.append("<li>Klik kartu kelas untuk membuka rincian dan masuk ke aktivitas kelas "
				+ "tersebut (presensi, materi, tugas, dan nilai).</li>");
		sb.append("</ul>");

		sb.append(HELP_H4).append("&#128100; Untuk Siapa Panel Ini</h4>");
		sb.append("<p>Panel ini berguna untuk <b>dosen</b> maupun <b>mahasiswa</b>. Dosen memakai "
				+ "panel ini sebagai titik awal mengajar: memilih kelas, mengisi presensi, "
				+ "membagikan materi, memberi tugas, dan memasukkan nilai. Mahasiswa memakai panel "
				+ "ini untuk melihat kelas yang sedang diikuti, memantau berapa pertemuan yang "
				+ "sudah berjalan, dan masuk ke ruang kelas untuk membaca materi atau mengerjakan "
				+ "tugas. Karena semua kelas semester berjalan dikumpulkan di satu tempat, Anda "
				+ "tidak perlu menghafal jadwal atau membuka banyak halaman.</p>");

		sb.append(HELP_H4).append("&#128073; Langkah Cepat</h4>");
		sb.append("<ol style='margin-top:0;padding-left:20px;'>");
		sb.append("<li>Pilih kelompok kegiatan pada <b>menu tegak</b> di sebelah kiri "
				+ "(misalnya Perkuliahan).</li>");
		sb.append("<li>Cari kelas yang dituju lewat kolom <b>Pencarian</b> bila daftarnya "
				+ "panjang.</li>");
		sb.append("<li>Perhatikan angka <b>Tuntas</b> untuk tahu sejauh mana perkuliahan sudah "
				+ "berjalan.</li>");
		sb.append("<li>Klik kartu kelas untuk masuk ke rincian dan mulai mengisi presensi, "
				+ "materi, atau tugas.</li>");
		sb.append("<li>Klik <b>Refresh</b> bila ada perubahan yang belum tampil.</li>");
		sb.append("</ol>");

		sb.append(HELP_H4).append("&#10067; Tanya Jawab Singkat</h4>");
		sb.append("<ul style='margin-top:0;padding-left:18px;'>");
		sb.append("<li><b>Kelas saya tidak muncul?</b> Pastikan semester yang dipilih benar dan "
				+ "kelas sudah dibuka oleh bagian akademik, lalu klik Refresh.</li>");
		sb.append("<li><b>Angka Tuntas tidak bertambah?</b> Pertemuan baru dihitung tuntas "
				+ "setelah presensi pertemuan tersebut tersimpan.</li>");
		sb.append("<li><b>Apa beda Hdr mhs dan Hdr dosen?</b> Hdr mhs adalah kehadiran mahasiswa, "
				+ "Hdr dosen adalah kehadiran pengajar pada kelas itu.</li>");
		sb.append("</ul>");

		sb.append(bantuanBlokTombolHeader());

		sb.append("<div style='background:#fff7ed;border:1px solid #fed7aa;border-radius:6px;"
				+ "padding:10px 14px;margin-top:10px;'>&#9888;&#65039; <b>Tips:</b> Jika daftar "
				+ "kelas kosong, pastikan Anda sudah memilih <b>semester yang benar</b> dan kelas "
				+ "sudah dibuka oleh bagian akademik. Bila kelas seharusnya ada tetapi tidak "
				+ "muncul, klik <b>Refresh</b> terlebih dahulu, lalu hubungi admin akademik bila "
				+ "tetap tidak tampil.</div>");
		sb.append("</div>");
		return sb.toString();
	}

	private String bantuanPanelAktivitas() {
		StringBuilder sb = new StringBuilder(4096);
		sb.append(HELP_WRAP_BUKA);
		sb.append(HELP_H3_BUKA).append("&#128218; Panduan &mdash; Aktivitas Pembelajaran</h3>");

		sb.append("<p>Panel tengah ini adalah <b>pusat pemantauan pembelajaran digital</b>. "
				+ "Di sinilah Anda melihat siapa yang sudah hadir, siapa yang membaca materi, "
				+ "siapa yang mengumpulkan tugas, mengikuti ujian, membuka video/audio, dan "
				+ "melengkapi dokumen pembelajaran. Semua ringkasan ditampilkan dalam bentuk "
				+ "angka yang mudah dibaca sehingga Anda cepat tahu kondisi kelas hari ini.</p>");

		sb.append(HELP_H4).append("&#128450;&#65039; Tab di Bagian Atas</h4>");
		sb.append("<table style='border-collapse:collapse;width:100%;margin-bottom:10px;'>");
		sb.append(HELP_TR_HEAD).append(HELP_TH).append("Tab</th>").append(HELP_TH)
				.append("Isi</th></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Linimasa</b></td>").append(HELP_TD)
				.append("Aliran aktivitas terbaru beserta ringkasan dasbor (angka pertemuan dan "
						+ "kehadiran).</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Ringkasan</b></td>").append(HELP_TD)
				.append("Rangkuman singkat capaian pembelajaran.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Info &amp; Materi</b></td>").append(HELP_TD)
				.append("Pengumuman kelas dan bahan ajar yang dibagikan.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Kalender</b></td>").append(HELP_TD)
				.append("Agenda kegiatan dalam tampilan kalender.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Dasbor</b></td>").append(HELP_TD)
				.append("Grafik dan angka capaian pembelajaran. Kini memuat <b>dua sub-tab</b> dalam satu "
						+ "tab: <b>Dasbor</b> dan <b>OBE</b> (Outcome Based Education).</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Bimbingan</b></td>").append(HELP_TD)
				.append("Pusat kegiatan bimbingan mahasiswa. Berisi <b>lima sub-tab</b>: "
						+ "<b>Tugas Akhir</b>, <b>Sidang</b>, <b>KKN</b>, <b>PKL</b>, dan <b>PA</b> "
						+ "(Pembimbing Akademik).</td></tr>");
		sb.append("</table>");

		sb.append("<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-radius:6px;"
				+ "padding:8px 12px;margin:0 0 10px;'>&#128260; <b>Perubahan tampilan terbaru:</b> "
				+ "tab <b>Dasbor OBE</b> yang dulu terpisah kini menjadi sub-tab di dalam tab "
				+ "<b>Dasbor</b>. Selain itu, kegiatan Tugas Akhir, Sidang, KKN, PKL, dan Pembimbing "
				+ "Akademik yang sebelumnya berupa tab-tab terpisah kini disatukan ke dalam satu tab "
				+ "<b>Bimbingan</b> beserta sub-tabnya.</div>");

		sb.append(HELP_H4).append("&#127891; Tab Bimbingan dan Sub-Tabnya</h4>");
		sb.append("<p>Tab <b>Bimbingan</b> menyatukan seluruh kegiatan pembimbingan mahasiswa. Pilih "
				+ "salah satu sub-tab sesuai jenis kegiatan:</p>");
		sb.append("<table style='border-collapse:collapse;width:100%;margin-bottom:10px;'>");
		sb.append(HELP_TR_HEAD).append(HELP_TH).append("Sub-tab</th>").append(HELP_TH)
				.append("Isi</th></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Tugas Akhir</b></td>").append(HELP_TD)
				.append("Daftar tugas akhir/skripsi mahasiswa bimbingan beserta judulnya.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Sidang</b></td>").append(HELP_TD)
				.append("Kegiatan dan penilaian sidang/ujian tugas akhir.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>KKN</b></td>").append(HELP_TD)
				.append("Bimbingan Kuliah Kerja Nyata.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>PKL</b></td>").append(HELP_TD)
				.append("Bimbingan Praktik Kerja Lapangan/magang.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>PA</b></td>").append(HELP_TD)
				.append("Pembimbing Akademik &mdash; daftar mahasiswa perwalian Anda.</td></tr>");
		sb.append("</table>");

		sb.append("<p>Setiap sub-tab menampilkan <b>penyaring</b> di bagian atas: <b>Tahun Akademik</b>, "
				+ "<b>Semester</b>, <b>Fakultas</b>, <b>Program Studi</b>, kotak <b>Cari NIM/NIS/Nama/Judul</b>, "
				+ "dan <b>Dosen</b>. Isi penyaring seperlunya lalu klik <b>Tampilkan</b> untuk memuat "
				+ "daftar. Bagi mahasiswa, daftar otomatis menampilkan bimbingannya sendiri; bagi dosen, "
				+ "menampilkan mahasiswa yang dibimbing.</p>");
		sb.append("<ul style='margin-top:0;padding-left:18px;'>");
		sb.append("<li><b>Pengajuan Baru</b> &mdash; pada sub-tab <b>Tugas Akhir</b> dan <b>Sidang</b>, "
				+ "tombol ini digunakan untuk mengajukan tugas akhir/sidang baru. Tombol ini "
				+ "disembunyikan bagi dosen karena pengajuan dilakukan oleh mahasiswa.</li>");
		sb.append("<li>Kolom daftar: <b>Foto</b>, <b>NIM/NIS</b>, <b>Nama</b>, <b>Judul</b>, "
				+ "<b>TA/Smt</b> (tahun akademik &amp; semester), dan <b>Aksi</b>.</li>");
		sb.append("<li>Pada kolom <b>Aksi</b>: <b>Agenda</b> membuka catatan/agenda bimbingan mahasiswa "
				+ "tersebut, dan <b>Nilai</b> membuka lembar penilaian bimbingan.</li>");
		sb.append("</ul>");

		sb.append(HELP_H4).append("&#128203; Kotak Ringkasan Dasbor</h4>");
		sb.append("<ul style='margin-top:0;padding-left:18px;'>");
		sb.append("<li><b>Pertemuan</b> &mdash; berapa pertemuan yang sudah diakses dibanding "
				+ "target, contoh <i>73/106 (68,8%)</i>. <i>Telah Diakses</i> = sudah dibuka, "
				+ "<i>Belum Diakses</i> = belum dibuka, <i>Target</i> = total rencana.</li>");
		sb.append("<li><b>Kehadiran Mahasiswa</b> dan <b>Kehadiran Siswa</b> &mdash; rekap "
				+ "kehadiran. Klik angka untuk melihat rincian nama pesertanya.</li>");
		sb.append("</ul>");

		sb.append(HELP_H4).append("&#128221; Arti Kode Kehadiran</h4>");
		sb.append("<table style='border-collapse:collapse;width:100%;margin-bottom:10px;'>");
		sb.append(HELP_TR_HEAD).append(HELP_TH).append("Kode</th>").append(HELP_TH)
				.append("Arti</th></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>M</b></td>").append(HELP_TD)
				.append("Hadir (Masuk).</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>A</b></td>").append(HELP_TD)
				.append("Mangkir / tidak ada alasan (alpa).</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>S</b></td>").append(HELP_TD)
				.append("Sakit.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>I</b></td>").append(HELP_TD)
				.append("Izin.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>&ndash;</b></td>").append(HELP_TD)
				.append("Belum ada keterangan / belum diabsen.</td></tr>");
		sb.append("</table>");

		sb.append(HELP_H4).append("&#9881;&#65039; Alat Bantu di Atas Daftar</h4>");
		sb.append("<ul style='margin-top:0;padding-left:18px;'>");
		sb.append("<li><b>Agenda</b> / <b>Kalender</b> &mdash; berpindah antara tampilan daftar "
				+ "dan tampilan kalender.</li>");
		sb.append("<li><b>Pencarian</b> &mdash; mencari kegiatan atau pertemuan tertentu.</li>");
		sb.append("<li><b>Rentang waktu</b> (misalnya <i>3 bulan</i>) &mdash; mengatur seberapa "
				+ "jauh ke belakang data yang ditampilkan.</li>");
		sb.append("<li><b>Refresh</b> / <b>Refresh Ringkasan Dasbor</b> &mdash; memuat ulang "
				+ "angka terbaru setelah ada presensi atau pengumpulan tugas.</li>");
		sb.append("</ul>");

		sb.append(HELP_H4).append("&#128100; Untuk Siapa Panel Ini</h4>");
		sb.append("<p>Panel ini paling sering dipakai <b>dosen</b>, <b>operator</b>, dan "
				+ "<b>pimpinan</b> untuk memantau jalannya pembelajaran tanpa harus membuka satu "
				+ "per satu kelas. Dalam sekejap Anda bisa tahu: berapa pertemuan yang sudah "
				+ "diakses, berapa mahasiswa yang hadir hari ini, dan siapa saja yang belum "
				+ "menyentuh materi. Mahasiswa juga bisa melihat ringkasan aktivitasnya sendiri. "
				+ "Semua disajikan sebagai angka besar yang mudah dibaca, lengkap dengan rincian "
				+ "yang bisa dibuka bila diperlukan.</p>");

		sb.append(HELP_H4).append("&#128073; Langkah Memantau Kehadiran</h4>");
		sb.append("<ol style='margin-top:0;padding-left:20px;'>");
		sb.append("<li>Atur <b>rentang waktu</b> (misalnya 3 bulan) agar data yang ditampilkan "
				+ "sesuai kebutuhan.</li>");
		sb.append("<li>Lihat kotak <b>Pertemuan</b> untuk tahu berapa banyak pertemuan yang "
				+ "sudah diakses dibanding target.</li>");
		sb.append("<li>Pada kotak <b>Kehadiran</b>, baca angka di tiap kode (M, A, S, I).</li>");
		sb.append("<li><b>Klik angka</b> tersebut untuk melihat daftar nama peserta pada "
				+ "kategori itu.</li>");
		sb.append("<li>Setelah mengisi presensi di kelas, kembali ke sini dan klik "
				+ "<b>Refresh Ringkasan Dasbor</b> agar angka diperbarui.</li>");
		sb.append("</ol>");

		sb.append("<p>Gunakan tab <b>Linimasa</b> untuk melihat aktivitas terbaru secara "
				+ "berurutan, tab <b>Info &amp; Materi</b> untuk membagikan pengumuman dan bahan "
				+ "ajar, serta tab <b>Kalender</b> bila Anda lebih suka melihat agenda dalam "
				+ "bentuk penanggalan. Tab <b>Dasbor</b> menyajikan capaian pembelajaran dalam bentuk "
				+ "grafik (termasuk sub-tab <b>OBE</b>) untuk kebutuhan evaluasi dan akreditasi, "
				+ "sedangkan tab <b>Bimbingan</b> memusatkan seluruh kegiatan Tugas Akhir, Sidang, "
				+ "KKN, PKL, dan Pembimbing Akademik dalam satu tempat.</p>");

		sb.append(HELP_H4).append("&#10067; Tanya Jawab Singkat</h4>");
		sb.append("<ul style='margin-top:0;padding-left:18px;'>");
		sb.append("<li><b>Kenapa angka kehadiran nol?</b> Bisa jadi presensi pertemuan tersebut "
				+ "belum diisi, atau rentang waktunya tidak mencakup tanggal pertemuan.</li>");
		sb.append("<li><b>Angka tidak berubah padahal sudah absen?</b> Klik Refresh Ringkasan "
				+ "Dasbor; angka dihitung ulang saat dimuat.</li>");
		sb.append("<li><b>Apa arti tanda strip (&ndash;)?</b> Peserta itu belum punya keterangan "
				+ "kehadiran alias belum diabsen.</li>");
		sb.append("</ul>");

		sb.append(bantuanBlokTombolHeader());

		sb.append("<div style='background:#eff6ff;border:1px solid #bfdbfe;border-radius:6px;"
				+ "padding:10px 14px;margin-top:10px;'>&#128161; <b>Tips:</b> Angka pada kotak "
				+ "ringkasan bisa diklik untuk melihat daftar nama. Jika angka belum berubah "
				+ "setelah Anda mengisi presensi, klik <b>Refresh Ringkasan Dasbor</b>. Untuk "
				+ "melihat angka lebih jelas, gunakan tombol <b>Layar Penuh</b> di pojok kanan "
				+ "atas panel.</div>");
		sb.append("</div>");
		return sb.toString();
	}

	private String bantuanPanelTugas() {
		StringBuilder sb = new StringBuilder(4096);
		sb.append(HELP_WRAP_BUKA);
		sb.append(HELP_H3_BUKA).append("&#128221; Panduan &mdash; Tugas, Ujian, Materi &amp; Diskusi</h3>");

		sb.append("<p>Panel kanan ini mengumpulkan <b>semua bahan dan kegiatan belajar</b> dalam "
				+ "satu tempat: tugas yang harus dikerjakan, ujian yang dijadwalkan, materi/bahan "
				+ "ajar yang dibagikan dosen, serta ruang diskusi kelas. Tujuannya agar Anda "
				+ "tidak perlu berpindah-pindah halaman untuk tahu apa yang harus dikerjakan dan "
				+ "bahan apa yang tersedia.</p>");

		sb.append(HELP_H4).append("&#128206; Dua Tab Utama</h4>");
		sb.append("<ul style='margin-top:0;padding-left:18px;'>");
		sb.append("<li><b>Tugas, Ujian, Materi</b> &mdash; daftar tugas, ujian, dan materi "
				+ "pembelajaran.</li>");
		sb.append("<li><b>Diskusi</b> &mdash; ruang tanya-jawab dan diskusi antara dosen dan "
				+ "mahasiswa.</li>");
		sb.append("</ul>");

		sb.append(HELP_H4).append("&#9989; Saringan Cepat (Kotak Centang)</h4>");
		sb.append("<p>Di bawah kolom pencarian tersedia tiga kotak centang: <b>Materi</b>, "
				+ "<b>Ujian</b>, dan <b>Tugas</b>. Hilangkan centang pada jenis yang tidak ingin "
				+ "Anda lihat, lalu daftar akan menyaring otomatis. Gunakan kolom <b>Cari</b> "
				+ "untuk menemukan judul tertentu (cukup sebagian kata), lalu klik "
				+ "<b>Refresh</b> bila perlu memuat ulang.</p>");

		sb.append(HELP_H4).append("&#128196; Membaca Kartu pada Daftar</h4>");
		sb.append("<table style='border-collapse:collapse;width:100%;margin-bottom:10px;'>");
		sb.append(HELP_TR_HEAD).append(HELP_TH).append("Yang terlihat</th>").append(HELP_TH)
				.append("Artinya</th></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Tugas</b></td>").append(HELP_TD)
				.append("Judul tugas beserta pertemuan dan mata kuliahnya, lengkap dengan batas "
						+ "waktu pengumpulan.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Upload : N peserta</b></td>").append(HELP_TD)
				.append("Berapa peserta yang sudah mengunggah jawaban tugas.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>N Akses</b></td>").append(HELP_TD)
				.append("Berapa kali materi/tugas/ujian itu sudah dibuka peserta. "
						+ "<i>0 Akses</i> berarti belum ada yang membuka.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Materi</b></td>").append(HELP_TD)
				.append("Bahan ajar (dokumen, tautan buku, video). Klik untuk membuka.</td></tr>");
		sb.append("<tr>").append(HELP_TD).append("<b>Ujian</b></td>").append(HELP_TD)
				.append("Ujian yang dijadwalkan beserta waktunya.</td></tr>");
		sb.append("</table>");

		sb.append(HELP_H4).append("&#128172; Diskusi</h4>");
		sb.append("<ul style='margin-top:0;padding-left:18px;'>");
		sb.append("<li>Buka tab <b>Diskusi</b> untuk mengirim pertanyaan atau menjawab "
				+ "diskusi kelas.</li>");
		sb.append("<li>Dosen dapat memulai topik, mahasiswa dapat membalas; semua tercatat "
				+ "sehingga bisa dibaca ulang.</li>");
		sb.append("</ul>");

		sb.append(HELP_H4).append("&#128100; Untuk Dosen dan Mahasiswa</h4>");
		sb.append("<p>Bagi <b>dosen</b>, panel ini adalah tempat membagikan materi, membuat "
				+ "tugas, menjadwalkan ujian, serta memantau berapa peserta yang sudah membuka "
				+ "atau mengumpulkan. Bagi <b>mahasiswa</b>, panel ini adalah daftar pekerjaan: "
				+ "apa yang harus dibaca, apa yang harus dikerjakan, dan kapan batas waktunya. "
				+ "Karena tugas, ujian, materi, dan diskusi dikumpulkan dalam satu panel, Anda "
				+ "tidak akan ketinggalan informasi penting.</p>");

		sb.append(HELP_H4).append("&#128073; Langkah Mengumpulkan Tugas (Mahasiswa)</h4>");
		sb.append("<ol style='margin-top:0;padding-left:20px;'>");
		sb.append("<li>Temukan kartu tugas yang dituju (gunakan kotak <b>Cari</b> bila perlu).</li>");
		sb.append("<li>Periksa <b>batas waktu</b> pengumpulan yang tertera pada kartu.</li>");
		sb.append("<li>Klik kartu untuk membukanya, lalu <b>unggah berkas</b> jawaban Anda.</li>");
		sb.append("<li>Pastikan status berubah menjadi sudah mengunggah, lalu klik "
				+ "<b>Refresh</b>.</li>");
		sb.append("</ol>");

		sb.append(HELP_H4).append("&#128073; Langkah Membuat Tugas/Materi (Dosen)</h4>");
		sb.append("<ol style='margin-top:0;padding-left:20px;'>");
		sb.append("<li>Buka kelas dan pertemuan yang sesuai dari panel kiri.</li>");
		sb.append("<li>Tambahkan materi (dokumen, tautan, atau video) atau buat tugas/ujian baru "
				+ "beserta batas waktunya.</li>");
		sb.append("<li>Pantau angka <b>Upload</b> dan <b>Akses</b> untuk melihat partisipasi "
				+ "peserta.</li>");
		sb.append("<li>Beri nilai pada tugas yang telah dikumpulkan, lalu nilai akan masuk ke "
				+ "rekap kelas.</li>");
		sb.append("</ol>");

		sb.append(HELP_H4).append("&#10067; Tanya Jawab Singkat</h4>");
		sb.append("<ul style='margin-top:0;padding-left:18px;'>");
		sb.append("<li><b>Tugas tidak muncul?</b> Periksa kotak centang Materi/Ujian/Tugas di "
				+ "atas, lalu klik Refresh. Bisa jadi jenisnya sedang tersaring.</li>");
		sb.append("<li><b>Apa arti 0 Akses?</b> Belum ada peserta yang membuka materi/tugas "
				+ "tersebut.</li>");
		sb.append("<li><b>Sudah unggah tapi tidak terhitung?</b> Klik Refresh; jumlah peserta "
				+ "yang mengunggah diperbarui saat dimuat ulang.</li>");
		sb.append("</ul>");

		sb.append(HELP_H4).append("&#128218; Istilah Penting</h4>");
		sb.append("<ul style='margin-top:0;padding-left:18px;'>");
		sb.append("<li><b>Materi</b> &mdash; bahan ajar yang dibagikan dosen, bisa berupa "
				+ "berkas dokumen, tautan buku, atau video untuk dipelajari.</li>");
		sb.append("<li><b>Tugas</b> &mdash; pekerjaan yang harus dikumpulkan mahasiswa sebelum "
				+ "batas waktu, biasanya dengan cara mengunggah berkas jawaban.</li>");
		sb.append("<li><b>Ujian</b> &mdash; penilaian terjadwal (misalnya UTS atau UAS) yang "
				+ "dikerjakan pada waktu yang ditentukan.</li>");
		sb.append("<li><b>Akses</b> &mdash; jumlah pembukaan oleh peserta; menandakan seberapa "
				+ "banyak yang sudah melihat materi, tugas, atau ujian itu.</li>");
		sb.append("</ul>");

		sb.append(bantuanBlokTombolHeader());

		sb.append("<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-radius:6px;"
				+ "padding:10px 14px;margin-top:10px;'>&#9888;&#65039; <b>Perhatian:</b> Selalu "
				+ "cek <b>batas waktu</b> pada kartu tugas/ujian sebelum mengerjakan. Bila daftar "
				+ "terlalu panjang, gunakan kotak centang untuk menyaring dan tombol "
				+ "<b>Layar Penuh</b> agar lebih leluasa membaca. Klik <b>Refresh</b> setelah "
				+ "mengumpulkan tugas untuk memastikan status terbaru tampil.</div>");
		sb.append("</div>");
		return sb.toString();
	}

	private String bantuanPanelUmum(String judul) {
		StringBuilder sb = new StringBuilder(1024);
		sb.append(HELP_WRAP_BUKA);
		sb.append(HELP_H3_BUKA).append("&#128218; Panduan &mdash; ")
				.append(judul == null || judul.isEmpty() ? "Panel" : judul).append("</h3>");
		sb.append("<p>Panel ini menampilkan informasi pembelajaran. Gunakan tombol di pojok "
				+ "kanan atas untuk memperbesar tampilan atau membuka panduan.</p>");
		sb.append(bantuanBlokTombolHeader());
		sb.append("</div>");
		return sb.toString();
	}

	private boolean isMobileView() {
		return Common.isMobile() || (desktopWidth != null && desktopHeight != null
				&& desktopWidth < ConstantValues.UKURAN_BATAS_MOBILE);
	}

	private int tinggiKanvasMobile = 480;

	private void initMobileMenuPanel() {
		Div panelAgenda = btnAktivitas.tambahTab("Agenda", "/img/Folder-Scheduled-Tasks-icon.png");

		menu = new Center();
		ais.ui.util.ZkCompat.setFlex(menu, true);

		/* Tanpa tinggi eksplisit, Borderlayout di dalam panel kolaps 0px
		 * pada rantai height:auto (mode satu scroll) sehingga isi e-learning
		 * mobile tampak kosong. Tinggi mengikuti kanvas dikurangi perkiraan
		 * tinggi baris tab. */
		Borderlayout borderlayoutSub = new Borderlayout();
		borderlayoutSub.setWidth("100%");
		int tinggiIsi = tinggiKanvasMobile - 96;
		if (tinggiIsi < 420) {
			tinggiIsi = 420;
		}
		borderlayoutSub.setHeight(tinggiIsi + "px");
		borderlayoutSub.setParent(panelAgenda);
		borderlayoutSub.appendChild(menu);
	}

	private void initLearningTabs() {
		if (mobileTampilan) {
			// Mobile: tab Linimasa + tab Tgs,Ujian,Materi (indeks otomatis setelah Agenda)
			tabpanelTimeline = btnAktivitas.tambahTab("Linimasa", "/img/Time-Sandglass-icon.png");
			Div panelTugasUjianMateri = btnAktivitas.tambahTab("Tgs,Ujian,Materi",
					"/img/folder-documents-icon.png");
			initMobileJadwalUjianPanel(panelTugasUjianMateri);
		} else {
			// Desktop: tab 1=Linimasa, tab 2=Ringkasan
			tabpanelTimeline = btnAktivitas.tambahTab(1, "Linimasa", "/img/Time-Sandglass-icon.png");
			btnAktivitas.tambahTabLazy(2, "Ringkasan", "/img/svg/table-report.svg",
					new ais.ui.util.MyButtonTabbox.PemuatTab() {
						@Override
						public void muat(Div panel) throws Exception {
							if (sekolah != null && sekolah.getId() != null) {
								RekapitulasiJadwalPelajaranHelper.display(panel, tbmuser, false);
							} else {
								RekapitulasiPerkuliahanHelper.display(panel, tbmuser, false);
							}
						}
					});
		}
	}

	private void initMobileJadwalUjianPanel(final Div panelTugasUjianMateri) {
		Borderlayout borderlayoutSub = new Borderlayout();
		borderlayoutSub.setParent(panelTugasUjianMateri);

		menuKanan = new Center();
		menuKanan.setParent(borderlayoutSub);
		ais.ui.util.ZkCompat.setFlex(menuKanan, true);

		btnAktivitas.onSetiapKlikPanel(panelTugasUjianMateri, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadMobileJadwalUjianContent(arg0);
			}
		});
	}

	private void loadMobileJadwalUjianContent(Event event) throws Exception {
		if (tabpanelUtamaPerkuliahan != null && hanyaPlaceholderLoading(tabpanelUtamaPerkuliahan)) {
			loadMenu(tabpanelUtamaPerkuliahan, TampilanELearningAction.PERKULIAHAN);
		}

		if (tabpanelTimelineSiapDiisi()) {
			bersihkanLoadingTengah();
			tabpanelTimeline.appendChild(dashboardTimelinePertemuan = new DashboardTimelinePertemuan(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					reloadBlnSd = true;
					if (eventListenerMateri != null) {
						eventListenerMateri.onEvent(arg0);
					}
				}
			}));
		}

		if (centerMateri != null && centerMateri.getChildren().isEmpty()) {
			final Timer timer = new Timer(500);
			timer.setRepeats(true);
			timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			final int[] percobaanKanan = { 0 };
			timer.addEventListener("onTimer", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					percobaanKanan[0]++;
					boolean pertemuanSiap = dashboardTimelinePertemuan != null
							&& dashboardTimelinePertemuan.pertemuansa != null;
					// FALLBACK: sebelumnya timer ini menunggu pertemuansa center TANPA batas — bila center
					// gagal/lambat memuat, panel kanan "Ambil data..." berputar SELAMANYA. Kini setelah
					// ~20 detik (40 x 500ms) panel kanan TETAP dipicu walau data center belum siap, agar
					// tidak menggantung (loadDataMateri sudah aman terhadap pertemuansa kosong/null).
					if (pertemuanSiap || percobaanKanan[0] >= 40) {
						if (eventListenerMateri != null) {
							eventListenerMateri.onEvent(null);
						}
						timer.stop();
						timer.detach();
					}
				}
			});
			timer.start();
		}
	}

	private void initInfoMateriTabs() throws Exception {
		btnAktivitas.tambahTabLazy("Info & Materi", "/img/svg/filter-square.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						buildInfoMateriContent(panel);
					}
				});
	}

	private void buildInfoMateriContent(org.zkoss.zk.ui.Component panel) throws Exception {
		ais.ui.util.MyButtonTabbox btnInfoMateri = ais.ui.util.MyButtonTabbox.buat(panel, "100%", null);

		btnInfoMateri.tambahTabLazy(1, "Dasbor", "/img/svg/dashboard-speed.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div p) throws Exception {
						DasborInfoDanMateri.display(p, tbmuser);
					}
				});

		if (sekolah == null || sekolah.getId() == null) {
			btnInfoMateri.tambahTabLazy(2, "Info", "/img/svg/info.svg",
					new ais.ui.util.MyButtonTabbox.PemuatTab() {
						@Override
						public void muat(Div p) throws Exception {
							MyWindow window = new MyWindow("", "none", false);
							window.setHeight("100%");
							window.setWidth("100%");
							window.setParent(p);
							MyInclude iframe = new MyInclude(
									"/pages/master/tampilan_pengumuman_perkuliahan.zul?sederhana=true");
							iframe.setParent(window);
						}
					});
		}

		btnInfoMateri.tambahTabLazy(3, "Ujian", "/img/svg/user-edit.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div p) throws Exception {
						RekapitulasiUjianHelper.display(p, tbmuser);
					}
				});

		btnInfoMateri.tambahTabLazy(4, "Tgs", "/img/svg/task-line.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div p) throws Exception {
						RekapitulasiTugasHelper.display(p, tbmuser);
					}
				});

		btnInfoMateri.tambahTabLazy(5, "Tgs.Kel", "/img/svg/list-task.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div p) throws Exception {
						RekapitulasiTugasKelompokHelper.display(p, tbmuser);
					}
				});

		btnInfoMateri.tambahTabLazy(6, "Materi", "/img/svg/books-thin.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div p) throws Exception {
						renderMateriReferensiSubTabs(p);
					}
				});
	}

	private void renderMateriReferensiSubTabs(Div host) throws Exception {
		ais.ui.util.MyButtonTabbox btnMateri = ais.ui.util.MyButtonTabbox.buat(host, "100%", null);

		// Tab 1: Materi — auto-load sebagai tab pertama
		btnMateri.tambahTabLazy(1, "Materi", "/img/svg/file-lines.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						RekapitulasiMateriHelper.display(panel, tbmuser);
					}
				});

		btnMateri.tambahTabLazy(2, "Video", "/img/svg/camera-video.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						RekapitulasiVideoHelper.display(panel, tbmuser);
					}
				});

		btnMateri.tambahTabLazy(3, "Audio", "/img/svg/file-audio-thin.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						RekapitulasiAudioHelper.display(panel, tbmuser);
					}
				});

		btnMateri.tambahTabLazy(4, "Buku", "/img/svg/book.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						PerkuliahanPunyaItemHelper perkuliahanPunyaItemHelper = new PerkuliahanPunyaItemHelper();
						perkuliahanPunyaItemHelper.display(createSqlTambahan(tbmuser), panel);
					}
				});

		btnMateri.tambahTabLazy(5, "Diktat", "/img/svg/file-earmark-text.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						Borderlayout borderlayout = new Borderlayout();
						borderlayout.setParent(panel);
						Center center = new Center();
						center.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(center, true);
						BukuBahanAjarHelper bukuBahanAjarHelper = new BukuBahanAjarHelper();
						bukuBahanAjarHelper.display(createSqlTambahanMatakuliah(), center);
					}
				});

		btnMateri.tambahTabLazy(6, "Artikel", "/img/svg/journal-bookmark.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						Borderlayout borderlayout = new Borderlayout();
						borderlayout.setParent(panel);
						Center center = new Center();
						center.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(center, true);
						DataPunyaArtikelHelper dataPunyaArtikelHelper = new DataPunyaArtikelHelper();
						dataPunyaArtikelHelper.display(createSqlTambahan(tbmuser), center);
					}
				});
	}

	private void initKalenderTabs() {
		btnAktivitas.tambahTabLazy("Kalender", "/img/Time-Today-icon.png",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						buildKalenderContent(panel);
					}
				});
	}

	private void buildKalenderContent(org.zkoss.zk.ui.Component panel) throws Exception {
		ais.ui.util.MyButtonTabbox btnKalender = ais.ui.util.MyButtonTabbox.buat(panel, "100%", null);

		btnKalender.tambahTabLazy(1, "Hari Ini", "/img/Time-Today-icon.png",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div p) throws Exception {
						String src = sekolah != null && sekolah.getId() != null
								? "/pages/master/kalender/jadwal_pelajaran/penjadwalan_hari_ini.zul"
								: "/pages/master/kalender/perkuliahan/penjadwalan_hari_ini.zul";
						ais.ui.util.MyButtonTabbox.muatZul(p, src);
					}
				});

		btnKalender.tambahTabLazy(2, "Minggu Ini", "/img/calendar-view-week-icon.png",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div p) throws Exception {
						ais.ui.util.MyButtonTabbox.muatZul(p,
								"/pages/master/kalender/perkuliahan/penjadwalan_minggu_ini.zul");
					}
				});

		btnKalender.tambahTabLazy(3, "Bulan Ini", "/img/calendar-view-month-icon.png",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div p) throws Exception {
						ais.ui.util.MyButtonTabbox.muatZul(p,
								"/pages/master/kalender/perkuliahan/penjadwalan_bulan_ini.zul");
					}
				});
	}

	/**
	 * Tab "Dasbor" pada panel tengah kini berisi DUA sub-tab (nested Tabbox): "Dasbor" (Tren Aktivitas
	 * Perkuliahan) dan "Dasbor OBE". Struktur nested dibangun LAZY sekali saat tab induk dibuka; sub-tab
	 * pertama (Tren) dibangun langsung karena default terpilih (onClick sub-tab tak terpicu otomatis),
	 * sub-tab kedua (OBE) dibangun saat diklik. Semua memakai guard {@code getChildren().isEmpty()}.
	 */
	@SuppressWarnings("deprecation")
	private void initDasborTabs() {
		btnAktivitas.tambahTabLazy("Dasbor", "/img/svg/dashboard-speed.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						buildDasborContent(panel);
					}
				});
	}

	@SuppressWarnings("deprecation")
	private void buildDasborContent(org.zkoss.zk.ui.Component panel) throws Exception {
		ais.ui.util.MyButtonTabbox btnDasbor = ais.ui.util.MyButtonTabbox.buat(panel, "100%", null);

		btnDasbor.tambahTabLazy(1, "Dasbor", "/img/svg/dashboard-speed.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div p) throws Exception {
						DashboardTrenAktivitasPerkuliahan window = new DashboardTrenAktivitasPerkuliahan();
						ais.ui.util.BaseDasbordPortal.mountWrappedTinggiPenuh(window, p,
								"Tren Aktivitas Perkuliahan",
								"Pola kehadiran dan aktivitas belajar mahasiswa dari waktu ke waktu.");
					}
				});

		btnDasbor.tambahTabLazy(2, "Dasbor OBE", "/img/svg/chart-line.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div p) throws Exception {
						ais.action.master.dashboard.admin.DasboardObeElearningHelper obeHelper =
								new ais.action.master.dashboard.admin.DasboardObeElearningHelper();
						ais.ui.util.BaseDasbordPortal.mountWrapped(obeHelper, p,
								"Dasbor OBE E-Learning",
								"Ringkasan capaian OBE dan aktivitas e-learning mahasiswa.");
					}
				});

		btnDasbor.tambahTabLazy(3, "Dasbor Bimbingan", "/img/svg/chalkboard-user.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div p) throws Exception {
						ais.action.master.dashboard.admin.DashboardBimbinganTaSkripsi dbBimbingan =
								new ais.action.master.dashboard.admin.DashboardBimbinganTaSkripsi();
						ais.ui.util.BaseDasbordPortal.mountWrapped(dbBimbingan, p,
								"Dasbor Bimbingan (Tugas Akhir & Skripsi)",
								"Ringkasan jumlah, status, dan tren bimbingan tugas akhir dan skripsi.");
					}
				});
	}

	/**
	 * Membangun tab <b>"Bimbingan"</b> pada panel TENGAH ("Aktivitas Pembelajaran"), tepat setelah
	 * tab "Dasbor". Tab ini adalah wadah "Bimbingan Saya" berisi 5 sub-tab (Tugas Akhir, Sidang, KKN,
	 * PKL, PA); tiap sub-tab menampilkan daftar mahasiswa/siswa bimbingan, dan klik satu baris membuka
	 * Agenda pertemuan. Konten dibangun MALAS (lazy) saat tab diklik agar halaman tetap ringan.
	 *
	 * <p>Isi sub-tab dan cakupan data disesuaikan peran oleh {@link #bangunTabBimbingan(Component)}
	 * (guru/siswa &rarr; hanya PKL; lainnya &rarr; 5 sub-tab) dan {@code terapkanScopingBimbingan()}
	 * pada {@code initStaticCriteria} (dosen &rarr; bimbingannya, mahasiswa &rarr; miliknya, guru
	 * &rarr; PKL sekolahnya, siswa &rarr; PKL-nya, admin &rarr; semua). Tab tampil untuk konteks PT
	 * maupun sekolah, untuk pemegang peran bimbingan, atau admin. Pemasangan tab+tabpanel sebagai satu
	 * pasangan menjaga pasangan indeks Tabbox tetap seimbang.</p>
	 */
	private void initBimbinganTab() {
		Tbmuser tbmuserKini = Common.getCurrentUser();
		boolean adaPeranBimbingan = tbmuserKini != null && (tbmuserKini.ambilDosen() != null
				|| tbmuserKini.getMahasiswa() != null || tbmuserKini.ambilGuru() != null
				|| tbmuserKini.getSiswa() != null);
		final boolean tampil = pt || ya || adaPeranBimbingan || Common.getApakahAdmin();

		Div panelBimbingan = btnAktivitas.tambahTabLazy("Bimbingan", "/img/svg/chalkboard-user.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						bangunTabBimbingan(panel);
					}
				});
		btnAktivitas.setVisiblePanel(panelBimbingan, tampil);
	}

	/**
	 * Mengambil daftar kelompok PKL (unik) yang diikuti seorang siswa, untuk ditampilkan sebagai
	 * "kelas PKL" di e-learning siswa. Hanya kelompok ber-scope sekolah (PKL siswa). Diurutkan
	 * terbaru dulu; difilter kata kunci pada nama kelompok bila ada. Memakai session yang diberikan
	 * (currentNativeSession) dan tidak menutupnya di sini (penutupan dilakukan pemanggil di finally).
	 */
	@SuppressWarnings("unchecked")
	private List<KelompokPkl> ambilKelompokPklMilikSiswa(org.hibernate.Session session,
			ais.database.model.sekolah.Siswa siswa, String keyword) {
		java.util.LinkedHashMap<Long, KelompokPkl> map = new java.util.LinkedHashMap<Long, KelompokPkl>();
		if (session == null || siswa == null || siswa.getId() == null) {
			return new ArrayList<KelompokPkl>();
		}
		List<ais.database.model.SiswaDapatKelompokPkl> rels = session
				.createCriteria(ais.database.model.SiswaDapatKelompokPkl.class).createAlias("kelompokPkl", "kp")
				.add(Restrictions.eq("siswa", siswa)).add(Restrictions.isNotNull("kp.sekolah"))
				.addOrder(Order.desc("kp.id")).list();
		String kw = keyword == null ? "" : keyword.trim().toLowerCase();
		for (ais.database.model.SiswaDapatKelompokPkl r : rels) {
			if (r == null) {
				continue;
			}
			KelompokPkl kp = r.getKelompokPkl();
			if (kp == null || kp.getId() == null) {
				continue;
			}
			if (kw.length() > 0) {
				String nm = kp.getNama_kelompok() == null ? "" : kp.getNama_kelompok().toLowerCase();
				if (nm.indexOf(kw) < 0) {
					continue;
				}
			}
			map.put(kp.getId(), kp);
		}
		return new ArrayList<KelompokPkl>(map.values());
	}

	public static String createSqlTambahan(Tbmuser tbmuser) {
		String sqltambahan = "";

		Sekolah sk = SekolahUtil.getSekolah();
		Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();
		Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();

		if (guru != null) {
			sk = guru.getSekolah();
		}
		if (siswa != null) {
			sk = siswa.getSekolah();
		}

		if (siswa != null) {
			sqltambahan = "this_.jadwal_pelajaran in (select id from sekolah.jadwal_pelajaran where kelas_id in (select a.kelas_id from sekolah.kelas_punya_siswa a where 1=1 and a.siswa_id="
					+ siswa.getId() + " group by a.kelas_id))";
		} else if (guru != null) {
			String sqlGuru = "guru_id=" + guru.getId();

			sqlGuru += " or guru2_id=" + guru.getId();
			sqlGuru += " or guru3_id=" + guru.getId();
			sqlGuru += " or guru4_id=" + guru.getId();
			sqlGuru += " or guru5_id=" + guru.getId();
			sqlGuru += " or guru6_id=" + guru.getId();
			sqlGuru += " or guru7_id=" + guru.getId();
			sqlGuru += " or guru8_id=" + guru.getId();
			sqlGuru += " or guru9_id=" + guru.getId();
			sqlGuru += " or guru10_id=" + guru.getId();
			sqlGuru += " or guru11_id=" + guru.getId();
			sqlGuru += " or guru12_id=" + guru.getId();

			sqltambahan = "this_.jadwal_pelajaran in (select id from sekolah.jadwal_pelajaran where " + sqlGuru + " )";
		} else if (sk != null && sk.getId() != null) {
			String sqlGuru = "sekolah_id=" + sk.getId();
			sqltambahan = "this_.jadwal_pelajaran in (select id from sekolah.jadwal_pelajaran where " + sqlGuru + " )";
		} else {

			Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
			if (mahasiswa == null) {
				Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();

				String sqlDosen = "";
				if (dosen != null) {
					sqlDosen += " and (";
					sqlDosen += " dosen1=" + dosen.getId();
					sqlDosen += " or dosen2=" + dosen.getId();
					sqlDosen += " or dosen3=" + dosen.getId();
					sqlDosen += " or dosen4=" + dosen.getId();
					sqlDosen += " or dosen5=" + dosen.getId();
					sqlDosen += " or dosen6=" + dosen.getId();
					sqlDosen += " or dosen7=" + dosen.getId();
					sqlDosen += " or dosen8=" + dosen.getId();
					sqlDosen += " or dosen9=" + dosen.getId();
					sqlDosen += " or dosen10=" + dosen.getId();
					sqlDosen += " ) ";
				}

				sqltambahan = "this_.perkuliahan in (select id from perkuliahan where 1=1 " + sqlDosen + ")";
			} else {
				sqltambahan = "this_.perkuliahan in (select a.perkuliahan from detailperkuliahan a where 1=1 and a.mahasiswa="
						+ mahasiswa.getId() + " group by a.perkuliahan)";
			}

		}

		System.out.println("sqltambahan => " + sqltambahan);
		return sqltambahan;
	}

	public String createSqlTambahanMatakuliah() {
		String sqltambahan = "";
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		if (mahasiswa == null) {
			Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();

			String sqlDosen = "";
			if (dosen != null) {
				sqlDosen += " and (";
				sqlDosen += " dosen1=" + dosen.getId();
				sqlDosen += " or dosen2=" + dosen.getId();
				sqlDosen += " or dosen3=" + dosen.getId();
				sqlDosen += " or dosen4=" + dosen.getId();
				sqlDosen += " or dosen5=" + dosen.getId();
				sqlDosen += " or dosen6=" + dosen.getId();
				sqlDosen += " or dosen7=" + dosen.getId();
				sqlDosen += " or dosen8=" + dosen.getId();
				sqlDosen += " or dosen9=" + dosen.getId();
				sqlDosen += " or dosen10=" + dosen.getId();
				sqlDosen += " ) ";
			}

			sqltambahan = "this_.matakuliah in (select matakuliah from perkuliahan where tahun_ajaran='"
					+ Common.getCurrentTahunAkademik() + "' and ganjil_genap='"
					+ (Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP) + "' " + sqlDosen + ")";
		} else {
			sqltambahan = "this_.matakuliah in (select b.matakuliah from detailperkuliahan a inner join perkuliahan b on (a.perkuliahan=b.id) where a.tahunakademik='"
					+ Common.getCurrentTahunAkademik() + "' and b.ganjil_genap='"
					+ (Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP) + "' and a.mahasiswa=" + mahasiswa.getId()
					+ " group by b.matakuliah)";
		}
		System.out.println("sqltambahan => " + sqltambahan);
		return sqltambahan;
	}

	/**
	 * Kotak isian filter <b>"Pengajar"</b> pada popup Pencarian e-Learning. Menyaring kelas/jadwal
	 * berdasarkan NAMA <b>dosen</b> (untuk Perkuliahan) atau NAMA <b>guru</b> (untuk Pelajaran).
	 * Dibuat ulang setiap popup Pencarian dirender; dibaca oleh {@link #terapkanFilterPengajar}.
	 */
	private Textbox pengajar;

	/**
	 * <h3>Menerapkan filter "Pengajar" (dosen/guru) pada kriteria daftar e-Learning</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> bila pengguna mengetik nama pengajar di kotak
	 * "Pengajar" pada popup Pencarian, daftar kelas disaring agar hanya menampilkan kelas yang diampu
	 * pengajar tersebut &mdash; <b>dosen</b> untuk Perkuliahan, atau <b>guru</b> untuk Pelajaran.</p>
	 *
	 * <p><b>Cara kerja teknis.</b> Memakai subkueri {@code EXISTS} berparameter (aman dari SQL
	 * injection lewat {@code StandardBasicTypes.STRING}) yang mencocokkan salah satu dari kolom
	 * pengajar pada baris ini (Perkuliahan: {@code dosen1..dosen10}; JadwalPelajaran:
	 * {@code guru_id, guru2_id..guru12_id}) dengan data pada tabel {@code dosen}/{@code sekolah.guru}
	 * yang namanya mengandung teks pencarian (tidak peka huruf besar/kecil). Pendekatan subkueri
	 * dipilih agar TIDAK bergantung pada &mdash; dan tidak menimbulkan bentrok dengan &mdash; alias
	 * join yang mungkin sudah dibuat untuk pencarian "Kata Kunci". Aman dipanggil untuk jenis apa pun;
	 * hanya berpengaruh pada Perkuliahan &amp; Pelajaran, dan hanya jika kotak "Pengajar" terisi.</p>
	 *
	 * @param criteria kriteria daftar yang akan ditambahi penyaring (boleh {@code null} &rarr; diabaikan)
	 * @param jenis    jenis tampilan e-Learning aktif ({@code PERKULIAHAN} / {@code PELAJARAN} / dll.)
	 */
	/**
	 * Nilai filter "Pengajar" untuk thread/request ini. Di-set oleh {@code initCriteria} sebelum
	 * memanggil {@code initStaticCriteria}, lalu dibaca {@link #terapkanFilterPengajar} DARI DALAM
	 * {@code initStaticCriteria} (metode statis) &mdash; penting: harus ditambahkan SEBELUM klausa
	 * ORDER BY (yang disisipkan sebagai {@code sqlRestriction("1=1 order by ...")}), agar tidak
	 * menghasilkan SQL rusak seperti {@code ... order by ... waktu_mulai_d AND exists(...)}.
	 */
	private static final ThreadLocal<String> FILTER_PENGAJAR_NAMA = new ThreadLocal<String>();

	private static void terapkanFilterPengajar(Criteria criteria, Integer jenis) {
		if (criteria == null) {
			return;
		}
		String p = FILTER_PENGAJAR_NAMA.get();
		if (p == null || p.trim().isEmpty()) {
			return;
		}
		String like = "%" + p.trim().toLowerCase() + "%";
		if (PERKULIAHAN.equals(jenis)) {
			criteria.add(Restrictions.sqlRestriction(
					"exists (select 1 from dosen d where lower(coalesce(d.nama,'')) like ? and d.id in ("
							+ "this_.dosen1,this_.dosen2,this_.dosen3,this_.dosen4,this_.dosen5,"
							+ "this_.dosen6,this_.dosen7,this_.dosen8,this_.dosen9,this_.dosen10))",
					like, org.hibernate.type.StandardBasicTypes.STRING));
		} else if (PELAJARAN.equals(jenis)) {
			criteria.add(Restrictions.sqlRestriction(
					"exists (select 1 from sekolah.guru g where lower(coalesce(g.nama,'')) like ? and g.id in ("
							+ "this_.guru_id,this_.guru2_id,this_.guru3_id,this_.guru4_id,this_.guru5_id,this_.guru6_id,"
							+ "this_.guru7_id,this_.guru8_id,this_.guru9_id,this_.guru10_id,this_.guru11_id,this_.guru12_id))",
					like, org.hibernate.type.StandardBasicTypes.STRING));
		}
	}

	/**
	 * KEAMANAN — membatasi daftar bimbingan ke "milik" pengguna yang login: dosen -> mahasiswa
	 * bimbingannya, mahasiswa -> miliknya, siswa -> kelompok PKL-nya, guru -> seluruh PKL siswa di
	 * sekolahnya. Untuk admin (bukan salah satu peran) tanpa pembatasan (memakai filter admin terpisah).
	 * WAJIB dipanggil SETELAH alias entitas dibuat dan SEBELUM ORDER BY (agar cabang yang memakai
	 * sqlRestriction "order by" tetap valid). Memakai Criteria/DetachedCriteria; tak membuka session.
	 */
	@SuppressWarnings("deprecation")
	private static void terapkanScopingBimbingan(Criteria criteria, Integer jenis, Tbmuser tbmuser) {
		if (criteria == null || tbmuser == null) {
			return;
		}
		Dosen dosen = tbmuser.ambilDosen();
		ais.database.model.Mahasiswa mhs = tbmuser.getMahasiswa();
		ais.database.model.sekolah.Guru guru = tbmuser.ambilGuru();
		ais.database.model.sekolah.Siswa siswa = tbmuser.getSiswa();

		if (SKRIPSI.equals(jenis)) {
			if (dosen != null) {
				criteria.add(orEqProps(new String[] { "pembimbing", "ketuaSidang", "pembimbing3", "penguji1",
						"penguji2", "penguji3", "penguji4", "penguji5" }, dosen));
			} else if (mhs != null) {
				criteria.add(Restrictions.eq("mahasiswa", mhs));
			}
		} else if (BIMBINGAN.equals(jenis)) {
			if (dosen != null) {
				criteria.add(orEqProps(new String[] { "dosen1", "dosen2", "dosen3", "dosen4", "dosen5", "dosen6" }, dosen));
			} else if (mhs != null) {
				criteria.add(Restrictions.eq("mahasiswa", mhs));
			}
		} else if (KKN.equals(jenis)) {
			if (dosen != null) {
				criteria.add(orEqProps(dosenPembimbingProps(), dosen));
			} else if (mhs != null) {
				criteria.add(org.hibernate.criterion.Subqueries.propertyIn("id",
						org.hibernate.criterion.DetachedCriteria.forClass(ais.database.model.MahasiswaDapatKelompokKkn.class)
								.add(Restrictions.eq("mahasiswa", mhs)).setProjection(Projections.property("kelompokKkn"))));
			}
		} else if (PKL.equals(jenis)) {
			if (dosen != null) {
				criteria.add(orEqProps(dosenPembimbingProps(), dosen));
			} else if (mhs != null) {
				criteria.add(org.hibernate.criterion.Subqueries.propertyIn("id",
						org.hibernate.criterion.DetachedCriteria.forClass(ais.database.model.MahasiswaDapatKelompokPkl.class)
								.add(Restrictions.eq("mahasiswa", mhs)).setProjection(Projections.property("kelompokPkl"))));
			} else if (siswa != null) {
				criteria.add(org.hibernate.criterion.Subqueries.propertyIn("id",
						org.hibernate.criterion.DetachedCriteria.forClass(ais.database.model.SiswaDapatKelompokPkl.class)
								.add(Restrictions.eq("siswa", siswa)).setProjection(Projections.property("kelompokPkl"))));
			} else if (guru != null) {
				ais.database.model.sekolah.Sekolah sk = guru.getSekolah();
				criteria.add(sk == null ? Restrictions.sqlRestriction("1=0") : Restrictions.eq("sekolah", sk));
			}
		} else if (KRS.equals(jenis)) {
			if (dosen != null) {
				criteria.add(Restrictions.or(Restrictions.eq("dosenPa", dosen),
						Restrictions.eq("mahasiswa.dosen", dosen.getId())));
			} else if (mhs != null) {
				criteria.add(Restrictions.eq("mahasiswa", mhs));
			}
		}
	}

	private static Criterion orEqProps(String[] props, Object val) {
		Criterion c = Restrictions.sqlRestriction("1=0");
		for (String p : props) {
			c = Restrictions.or(c, Restrictions.eq(p, val));
		}
		return c;
	}

	private static String[] dosenPembimbingProps() {
		return new String[] { "dosen_pembimbing1", "dosen_pembimbing2", "dosen_pembimbing3", "dosen_pembimbing4",
				"dosen_pembimbing5", "dosen_pembimbing6", "dosen_pembimbing7", "dosen_pembimbing8",
				"dosen_pembimbing9", "dosen_pembimbing10" };
	}


	public Criteria initCriteria(boolean order, Integer jenis, String keyword, Fakultas fak, Jurusan jur, String prog,
			Yayasan yay, Sekolah sek, Combobox ta, Combobox smt, Combobox hari, MyCheckboxConfig remedial,
			MyCheckboxConfig pra, MyCheckboxConfig ekstra, MyCheckboxConfig paralel, MyCheckboxConfig requestStatus,
			MyCheckboxConfig aktifStatus, MyCheckboxConfig seminarStatus, MyCheckboxConfig lulusStatus,
			MyCheckboxConfig gagalStatus, MyCheckboxConfig belumStatus, MyCheckboxConfig setujuStatus,
			MyCheckboxConfig mengulangStatus, MyCheckboxConfig sidangStatus, String kelas, Session session) {
		JenisFormulirKegiatan jenisFormulirKegiatan = null;
		return initCriteria(order, jenis, keyword, fak, jur, prog, yay, sek, ta, smt, hari, remedial, pra, ekstra,
				paralel, requestStatus, aktifStatus, seminarStatus, lulusStatus, gagalStatus, belumStatus, setujuStatus,
				mengulangStatus, sidangStatus, kelas, jenisFormulirKegiatan, session);
	}

	public Criteria initCriteria(boolean order, Integer jenis, String keyword, Fakultas fak, Jurusan jur, String prog,
			Yayasan yay, Sekolah sek, Combobox ta, Combobox smt, Combobox hari, MyCheckboxConfig remedial,
			MyCheckboxConfig pra, MyCheckboxConfig ekstra, MyCheckboxConfig paralel, MyCheckboxConfig requestStatus,
			MyCheckboxConfig aktifStatus, MyCheckboxConfig seminarStatus, MyCheckboxConfig lulusStatus,
			MyCheckboxConfig gagalStatus, MyCheckboxConfig belumStatus, MyCheckboxConfig setujuStatus,
			MyCheckboxConfig mengulangStatus, MyCheckboxConfig sidangStatus, String kelas,
			JenisFormulirKegiatan jenisFormulirKegiatan, Session session) {

		String tahunAjaran = (String) (ta.getSelectedItem() == null || ta.getSelectedItem().getValue() == null ? null
				: ta.getSelectedItem().getValue());
		String semester = (String) (smt.getSelectedItem() == null || smt.getSelectedItem().getValue() == null ? null
				: smt.getSelectedItem().getValue());
		String hr = (String) (hari.getSelectedItem() == null || hari.getSelectedItem().getValue() == null ? null
				: hari.getSelectedItem().getValue());

		boolean remedialb = remedial.isChecked();
		boolean prab = pra.isChecked();
		boolean ekstrab = ekstra.isChecked();
		boolean paralelb = paralel.isChecked();
		boolean requestStatusb = requestStatus.isChecked();
		boolean aktifStatusb = aktifStatus.isChecked();
		boolean seminarStatusb = seminarStatus.isChecked();
		boolean lulusStatusb = lulusStatus.isChecked();
		boolean gagalStatusb = gagalStatus.isChecked();
		boolean belumStatusb = belumStatus.isChecked();
		boolean setujuStatusb = setujuStatus.isChecked();
		boolean mengulangStatusb = mengulangStatus.isChecked();
		boolean sidangStatusb = sidangStatus.isChecked();

		// Sediakan nilai filter "Pengajar" agar dibaca initStaticCriteria SEBELUM ORDER BY disisipkan,
		// lalu bersihkan ThreadLocal di finally supaya tidak bocor ke pemanggil lain di thread ini.
		FILTER_PENGAJAR_NAMA.set(pengajar == null || pengajar.getValue() == null ? null : pengajar.getValue().trim());
		try {
			return initStaticCriteria(order, jenis, keyword, fak, jur, prog, yay, sek, tahunAjaran, semester, hr,
					remedialb, prab, ekstrab, paralelb, requestStatusb, aktifStatusb, seminarStatusb, lulusStatusb,
					gagalStatusb, belumStatusb, setujuStatusb, mengulangStatusb, sidangStatusb, kelas, tbmuser,
					jenisFormulirKegiatan, session);
		} finally {
			FILTER_PENGAJAR_NAMA.remove();
		}

	}

	public static Criteria initStaticCriteria(boolean order, Integer jenis, String keyword, Fakultas fak, Jurusan jur,
			String prog, Yayasan yay, Sekolah sek, String tahunAjaran, String semester, String hari, boolean remedial,
			boolean pra, boolean ekstra, boolean paralel, boolean requestStatus, boolean aktifStatus,
			boolean seminarStatus, boolean lulusStatus, boolean mengulangStatus, boolean gagalStatus,
			boolean belumStatus, boolean setujuStatus, boolean sidangStatus, String kelas, Tbmuser tbmuser,
			Session session) {
		JenisFormulirKegiatan jenisFormulirKegiatan = null;
		return initStaticCriteria(order, jenis, keyword, fak, jur, prog, yay, sek, tahunAjaran, semester, hari,
				remedial, pra, ekstra, paralel, requestStatus, aktifStatus, seminarStatus, lulusStatus, mengulangStatus,
				gagalStatus, belumStatus, setujuStatus, sidangStatus, kelas, tbmuser, jenisFormulirKegiatan, session);
	}

	public static Criteria initStaticCriteria(boolean order, Integer jenis, String keyword, Fakultas fak, Jurusan jur,
			String prog, Yayasan yay, Sekolah sek, String tahunAjaran, String semester, String hari, boolean remedial,
			boolean pra, boolean ekstra, boolean paralel, boolean requestStatus, boolean aktifStatus,
			boolean seminarStatus, boolean lulusStatus, boolean mengulangStatus, boolean gagalStatus,
			boolean belumStatus, boolean setujuStatus, boolean sidangStatus, String kelas, Tbmuser tbmuser,
			JenisFormulirKegiatan jenisFormulirKegiatan, Session session) {

		Fakultas fakultas = fak == null ? tbmuser.ambilFakultas() : fak;
		Jurusan jurusan = jur == null ? tbmuser.ambilJurusan() : jur;
		String program = prog == null || prog.trim().isEmpty()
				? (tbmuser.ambilProgram() == null ? null : tbmuser.ambilProgram().getNama())
				: prog;

		if (jenis.equals(PERKULIAHAN)) {

			Criteria criteria = session.createCriteria(Perkuliahan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

					.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)
					.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jurusan.fakultas", fakultas))
					.add(jurusan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", jurusan))
					.add(program == null || program.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("program", program));

			if (!kelas.trim().isEmpty()) {
				Criterion criterion = Restrictions.ilike("kelas", kelas.trim(), MatchMode.ANYWHERE);
				criteria.add(criterion);
			}

			if (!keyword.trim().isEmpty()) {
				criteria.createAlias("dosen1", "dosen1", Criteria.LEFT_JOIN)
						.createAlias("dosen2", "dosen2", Criteria.LEFT_JOIN)
						.createAlias("dosen3", "dosen3", Criteria.LEFT_JOIN)
						.createAlias("dosen4", "dosen4", Criteria.LEFT_JOIN)
						.createAlias("dosen5", "dosen5", Criteria.LEFT_JOIN)
						.createAlias("dosen6", "dosen6", Criteria.LEFT_JOIN)
						.createAlias("dosen7", "dosen7", Criteria.LEFT_JOIN)
						.createAlias("dosen8", "dosen8", Criteria.LEFT_JOIN)
						.createAlias("dosen9", "dosen9", Criteria.LEFT_JOIN)
						.createAlias("dosen10", "dosen10", Criteria.LEFT_JOIN).createAlias("matakuliah", "matakuliah");

				Criterion criterion = Restrictions.or(
						Restrictions.ilike("dosen1.nama", keyword.trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("dosen2.nama", keyword.trim(), MatchMode.ANYWHERE));

				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen3.nama", keyword.trim(), MatchMode.ANYWHERE));
				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen4.nama", keyword.trim(), MatchMode.ANYWHERE));
				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen5.nama", keyword.trim(), MatchMode.ANYWHERE));
				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen6.nama", keyword.trim(), MatchMode.ANYWHERE));
				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen7.nama", keyword.trim(), MatchMode.ANYWHERE));
				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen8.nama", keyword.trim(), MatchMode.ANYWHERE));
				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen9.nama", keyword.trim(), MatchMode.ANYWHERE));
				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen10.nama", keyword.trim(), MatchMode.ANYWHERE));

				criterion = Restrictions.or(criterion,
						Restrictions.or(Restrictions.ilike("matakuliah.kode", keyword.trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("matakuliah.nama", keyword.trim(), MatchMode.ANYWHERE)));

				criteria.add(criterion);

				if (ekstra) {
					criteria.add(Restrictions.eq("matakuliah.extraKulikuler", true));
				} else {
					criteria.add(Restrictions.or(Restrictions.isNull("matakuliah.extraKulikuler"),
							Restrictions.eq("matakuliah.extraKulikuler", false)));
				}
			} else if (ekstra) {
				criteria.createAlias("matakuliah", "matakuliah")
						.add(Restrictions.eq("matakuliah.extraKulikuler", true));
			} else {
				criteria.createAlias("matakuliah", "matakuliah")
						.add(Restrictions.or(Restrictions.isNull("matakuliah.extraKulikuler"),
								Restrictions.eq("matakuliah.extraKulikuler", false)));
			}

			criteria.add(paralel ? Restrictions.or(Restrictions.sqlRestriction(
					"this_.id in (select perkuliahan_paralel from perkuliahan where perkuliahan_paralel is not null)"),
					Restrictions.eq("merupakan_paralel", true)) : Restrictions.sqlRestriction("1=1"))

					.add(pra ? Restrictions.eq("merupakanPraPerkuliahan", true)
							: Restrictions.or(Restrictions.eq("merupakanPraPerkuliahan", false),
									Restrictions.isNull("merupakanPraPerkuliahan")))

					.add(remedial ? Restrictions.eq("merupakanRemedial", true)
							: Restrictions.or(Restrictions.isNull("merupakanRemedial"),
									Restrictions.eq("merupakanRemedial", false)))

					.add(Restrictions.isNotNull("tahunAjaran"))

					.add(tahunAjaran == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("tahunAjaran", tahunAjaran))

					.add(semester == null ? Restrictions.sqlRestriction("1=1")
							: semester.equals(Perkuliahan.SP)
									? Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
									: Restrictions.and(Restrictions.isNull("statusSemesterPendek"),
											Restrictions.eq("ganjilGenap", semester)))

					.add(hari == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("hari", hari));

			// Filter "Pengajar" (dosen) — WAJIB ditambahkan SEBELUM ORDER BY di bawah agar SQL valid.
			terapkanFilterPengajar(criteria, jenis);

			if (order) {
				criteria.add(Restrictions.sqlRestriction(
						"1=1 order by tahun_ajaran desc,status_semesterpendek asc, semester_perkuliahan desc, ganjil_genap desc,case hari when 'Senin' then 1 when 'Selasa' then 2 when 'Rabu' then 3 when 'Kamis' then 4 when 'Jumat' then 5  when 'Sabtu' then 6 when 'Minggu' then 7 else 5 end, waktu_mulai_d"));
			}

			return criteria;
		} else if (jenis.equals(SKRIPSI)) {

			Criterion criterion = Restrictions.sqlRestriction("false");
			if (belumStatus) {
				criterion = Restrictions.or(criterion, Restrictions.eq("setujuiSidang", false));
			}
			if (setujuStatus) {
				criterion = Restrictions.or(criterion, Restrictions.eq("setujuiSidang", true));
			}
			if (sidangStatus) {
				criterion = Restrictions.or(criterion, Restrictions.eq("telahSidang", 1));
			}

			Criteria criteria = session.createCriteria(Skripsi.class).add(criterion)
					.createAlias("mahasiswa", "mahasiswa")
					.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)
					.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jurusan.fakultas", fakultas))
					.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("mahasiswa.jurusan", jurusan))
					.add(program == null || program.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("mahasiswa.program", program));

			// KEAMANAN: batasi ke bimbingan milik pengguna login (dosen/mahasiswa/guru/siswa).
			terapkanScopingBimbingan(criteria, SKRIPSI, tbmuser);

			if (!kelas.trim().isEmpty()) {
				Criterion criterion1 = Restrictions.ilike("mahasiswa.kelas", kelas.trim(), MatchMode.ANYWHERE);
				criteria.add(criterion1);
			}

			if (!keyword.trim().isEmpty()) {
				criteria.createAlias("pembimbing", "dosen1", Criteria.LEFT_JOIN)
						.createAlias("ketuaSidang", "dosen2", Criteria.LEFT_JOIN)
						.createAlias("penguji1", "dosen3", Criteria.LEFT_JOIN)
						.createAlias("penguji2", "dosen4", Criteria.LEFT_JOIN)
						.createAlias("penguji3", "dosen5", Criteria.LEFT_JOIN)
						.createAlias("pembimbing3", "dosen6", Criteria.LEFT_JOIN)
						.createAlias("penguji4", "dosen7", Criteria.LEFT_JOIN);

				criterion = Restrictions.or(Restrictions.ilike("dosen1.nama", keyword.trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("dosen2.nama", keyword.trim(), MatchMode.ANYWHERE));

				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen3.nama", keyword.trim(), MatchMode.ANYWHERE));
				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen4.nama", keyword.trim(), MatchMode.ANYWHERE));
				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen5.nama", keyword.trim(), MatchMode.ANYWHERE));
				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen6.nama", keyword.trim(), MatchMode.ANYWHERE));
				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen7.nama", keyword.trim(), MatchMode.ANYWHERE));

				criterion = Restrictions.or(criterion,
						Restrictions.or(Restrictions.ilike("judul", keyword.trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("keyword", keyword.trim(), MatchMode.ANYWHERE)));

				criterion = Restrictions.or(criterion,
						Restrictions.or(Restrictions.ilike("mahasiswa.nim", keyword.trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("mahasiswa.nama", keyword.trim(), MatchMode.ANYWHERE)));

				criteria.add(criterion);

			}

			criteria.add(Restrictions.isNotNull("tahunAkademik"))

					.add(tahunAjaran == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("tahunAkademik", tahunAjaran))

					.add(semester == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.sqlRestriction(
									"this_.semester % 2 = " + (semester.equals(Perkuliahan.GANJIL) ? "1" : "0")));

			if (order) {
				criteria.add(Restrictions.sqlRestriction(
						"1=1 order by tahun_akademik desc,(case when semester % 2 = 0 then 'Genap' else 'Ganjil' end) desc, nim"));
			}

			return criteria;
		} else if (jenis.equals(BIMBINGAN)) {

			List<String> statuses = new ArrayList<String>();
			if (requestStatus) {
				statuses.add(MahasiswaRequestTugasAkhir.REQUEST_STATUS);
			}
			if (aktifStatus) {
				statuses.add(MahasiswaRequestTugasAkhir.AKTIF_STATUS);
			}
			if (seminarStatus) {
				statuses.add(MahasiswaRequestTugasAkhir.SEMINAR_STATUS);
			}
			if (mengulangStatus) {
				statuses.add(MahasiswaRequestTugasAkhir.MENGULANG_STATUS);
			}
			if (lulusStatus) {
				statuses.add(MahasiswaRequestTugasAkhir.LULUS_STATUS);
			}
			if (gagalStatus) {
				statuses.add(MahasiswaRequestTugasAkhir.GAGAL_STATUS);
			}

			Criteria criteria = session.createCriteria(MahasiswaRequestTugasAkhir.class)

					.add(statuses.isEmpty() ? Restrictions.sqlRestriction("false")
							: Restrictions.in("status", statuses))

					.createAlias("mahasiswa", "mahasiswa")
					.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)
					.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jurusan.fakultas", fakultas))
					.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("mahasiswa.jurusan", jurusan))
					.add(program == null || program.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("mahasiswa.program", program));

			// KEAMANAN: batasi ke bimbingan milik pengguna login (dosen/mahasiswa/guru/siswa).
			terapkanScopingBimbingan(criteria, BIMBINGAN, tbmuser);

			if (!kelas.trim().isEmpty()) {
				Criterion criterion1 = Restrictions.ilike("mahasiswa.kelas", kelas.trim(), MatchMode.ANYWHERE);
				criteria.add(criterion1);
			}

			if (!keyword.trim().isEmpty()) {
				criteria.createAlias("dosen1", "dosen1", Criteria.LEFT_JOIN)
						.createAlias("dosen2", "dosen2", Criteria.LEFT_JOIN)
						.createAlias("dosen3", "dosen3", Criteria.LEFT_JOIN)
						.createAlias("dosen4", "dosen4", Criteria.LEFT_JOIN)
						.createAlias("dosen5", "dosen5", Criteria.LEFT_JOIN)
						.createAlias("dosen6", "dosen6", Criteria.LEFT_JOIN);

				Criterion criterion = Restrictions.or(
						Restrictions.ilike("dosen1.nama", keyword.trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("dosen2.nama", keyword.trim(), MatchMode.ANYWHERE));

				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen3.nama", keyword.trim(), MatchMode.ANYWHERE));
				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen4.nama", keyword.trim(), MatchMode.ANYWHERE));
				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen5.nama", keyword.trim(), MatchMode.ANYWHERE));
				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen6.nama", keyword.trim(), MatchMode.ANYWHERE));

				criterion = Restrictions.or(criterion, Restrictions.ilike("judul", keyword.trim(), MatchMode.ANYWHERE));

				criterion = Restrictions.or(criterion,
						Restrictions.or(Restrictions.ilike("mahasiswa.nim", keyword.trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("mahasiswa.nama", keyword.trim(), MatchMode.ANYWHERE)));

				criteria.add(criterion);

			}

			criteria.add(Restrictions.isNotNull("tahunAkademik"))

					.add(tahunAjaran == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("tahunAkademik", tahunAjaran))

					// "semester" di sini adalah label Ganjil/Genap (String), sedangkan kolom
					// MahasiswaRequestTugasAkhir.semester bertipe Integer (nomor semester) — samakan
					// lewat modulo genap/ganjil, bukan Restrictions.eq langsung (ClassCastException
					// String->Integer saat binding).
					.add(semester == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.sqlRestriction(
									"this_.semester % 2 = " + (semester.equals(Perkuliahan.GANJIL) ? "1" : "0")));

			if (order) {
				criteria.add(Restrictions.sqlRestriction(
						"1=1 order by tahun_akademik desc,(case when semester % 2 = 0 then 'Genap' else 'Ganjil' end) desc, nim"));
			}

			return criteria;
		} else if (jenis.equals(KKN)) {

			Criteria criteria = session.createCriteria(KelompokKkn.class).createAlias("kkn", "kkn")
					.createAlias("kkn.jurusan", "jurusan", Criteria.LEFT_JOIN)
					.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("kkn.fakultas", fakultas))
					.add(jurusan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("kkn.jurusan", jurusan))
					.add(program == null || program.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("kkn.program", program));

			// KEAMANAN: batasi ke bimbingan milik pengguna login (dosen/mahasiswa/guru/siswa).
			terapkanScopingBimbingan(criteria, KKN, tbmuser);

			if (!keyword.trim().isEmpty()) {
				criteria.createAlias("dosen_pembimbing1", "dosen1", Criteria.LEFT_JOIN)
						.createAlias("dosen_pembimbing2", "dosen2", Criteria.LEFT_JOIN)
						.createAlias("dosen_pembimbing3", "dosen3", Criteria.LEFT_JOIN)
						.createAlias("dosen_pembimbing4", "dosen4", Criteria.LEFT_JOIN)
						.createAlias("dosen_pembimbing5", "dosen5", Criteria.LEFT_JOIN);

				Criterion criterion = Restrictions.or(
						Restrictions.ilike("dosen1.nama", keyword.trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("dosen2.nama", keyword.trim(), MatchMode.ANYWHERE));

				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen3.nama", keyword.trim(), MatchMode.ANYWHERE));
				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen4.nama", keyword.trim(), MatchMode.ANYWHERE));
				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen5.nama", keyword.trim(), MatchMode.ANYWHERE));

				criterion = Restrictions.or(criterion,
						Restrictions.or(Restrictions.ilike("kkn.nama", keyword.trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nama_kelompok", keyword.trim(), MatchMode.ANYWHERE)));

				criteria.add(criterion);

			}

			criteria.add(Restrictions.isNotNull("kkn.tahunAkademik"))

					.add(tahunAjaran == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("kkn.tahunAkademik", tahunAjaran))

					// KelompokKkn tidak punya properti "semester" sendiri (hanya lewat relasi ke
					// Kkn.semester via alias "kkn") — bare "semester" gagal diresolusi Hibernate.
					.add(semester == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("kkn.semester", semester));

			if (order) {
				criteria.addOrder(Order.desc("kkn.tahunAkademik")).addOrder(Order.desc("kkn.semester"))
						.addOrder(Order.asc("nama_kelompok"));
			}

			return criteria;
		} else if (jenis.equals(PKL)) {

			Criteria criteria = session.createCriteria(KelompokPkl.class).createAlias("pkl", "pkl")
					.createAlias("pkl.jurusan", "jurusan", Criteria.LEFT_JOIN)
					.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("pkl.fakultas", fakultas))
					.add(jurusan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("pkl.jurusan", jurusan))
					.add(program == null || program.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("pkl.program", program));

			// KEAMANAN: batasi ke bimbingan milik pengguna login (dosen/mahasiswa/guru/siswa).
			terapkanScopingBimbingan(criteria, PKL, tbmuser);

			if (!keyword.trim().isEmpty()) {
				criteria.createAlias("dosen_pembimbing1", "dosen1", Criteria.LEFT_JOIN)
						.createAlias("dosen_pembimbing2", "dosen2", Criteria.LEFT_JOIN)
						.createAlias("dosen_pembimbing3", "dosen3", Criteria.LEFT_JOIN)
						.createAlias("dosen_pembimbing4", "dosen4", Criteria.LEFT_JOIN)
						.createAlias("dosen_pembimbing5", "dosen5", Criteria.LEFT_JOIN);

				Criterion criterion = Restrictions.or(
						Restrictions.ilike("dosen1.nama", keyword.trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("dosen2.nama", keyword.trim(), MatchMode.ANYWHERE));

				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen3.nama", keyword.trim(), MatchMode.ANYWHERE));
				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen4.nama", keyword.trim(), MatchMode.ANYWHERE));
				criterion = Restrictions.or(criterion,
						Restrictions.ilike("dosen5.nama", keyword.trim(), MatchMode.ANYWHERE));

				criterion = Restrictions.or(criterion,
						Restrictions.or(Restrictions.ilike("pkl.nama", keyword.trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nama_kelompok", keyword.trim(), MatchMode.ANYWHERE)));

				criteria.add(criterion);

			}

			criteria.add(Restrictions.isNotNull("pkl.tahunAkademik"))

					.add(tahunAjaran == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("pkl.tahunAkademik", tahunAjaran))

					.add(semester == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.sqlRestriction(
									"semester % 2 = " + (semester.equals(Perkuliahan.GANJIL) ? "1" : "0")));

			if (order) {
				criteria.addOrder(Order.desc("pkl.tahunAkademik")).addOrder(Order.desc("pkl.semester"))
						.addOrder(Order.asc("nama_kelompok"));
			}

			return criteria;
		} else if (jenis.equals(KRS)) {

			Criteria criteria = session.createCriteria(KrsMahasiswa.class).createAlias("mahasiswa", "mahasiswa")

					.add(Restrictions.isNull("mahasiswa.statusKeluar"))

					.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)

					.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jurusan.fakultas", fakultas))
					.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("mahasiswa.jurusan", jurusan))
					.add(program == null || program.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("mahasiswa.program", program));

			// KEAMANAN: batasi ke bimbingan milik pengguna login (dosen/mahasiswa/guru/siswa).
			terapkanScopingBimbingan(criteria, KRS, tbmuser);

			if (!kelas.trim().isEmpty()) {
				Criterion criterion = Restrictions.ilike("mahasiswa.kelas", kelas.trim(), MatchMode.ANYWHERE);
				criteria.add(criterion);
			}

			if (!keyword.trim().isEmpty()) {
				criteria.createAlias("dosenPa", "dosen1", Criteria.LEFT_JOIN);

				Criterion criterion = Restrictions.or(
						Restrictions.ilike("dosen1.nama", keyword.trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("mahasiswa.nama", keyword.trim(), MatchMode.ANYWHERE));

				criterion = Restrictions.or(criterion,
						Restrictions.ilike("mahasiswa.nim", keyword.trim(), MatchMode.ANYWHERE));

				criteria.add(criterion);

			}

			criteria.add(Restrictions.isNull("mahasiswa.statusKeluar")).add(Restrictions.le("semester", 14))
					.add(Restrictions.isNotNull("tahunAkademik"))

					.add(tahunAjaran == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("tahunAkademik", tahunAjaran))

					.add(semester == null ? Restrictions.sqlRestriction("1=1")
							: semester.equals(Perkuliahan.SP)
									? Restrictions.eq("semesterPendek", Perkuliahan.SEMESTER_PENDEK)
									: Restrictions.and(Restrictions.isNull("semesterPendek"),
											Restrictions.sqlRestriction("this_.semester % 2 = "
													+ (semester.equals(Perkuliahan.GANJIL) ? "1" : "0"))));

			if (order) {
				criteria.add(Restrictions.sqlRestriction(
						"1=1 order by tahunakademik desc,semesterpendek asc,(case when semester % 2 = 0 then 'Genap' else 'Ganjil' end) desc, nim"));
			}

			return criteria;
		} else if (jenis.equals(PELAJARAN)) {

			Yayasan yayasan = yay == null || yay.getId() == null ? tbmuser.ambilYayasan() : yay;
			Sekolah sekolah = sek == null || sek.getId() == null ? tbmuser.ambilSekolah() : sek;
			Guru guru = tbmuser.ambilGuru();
			Siswa siswa = tbmuser.getSiswa();

			Criteria criteria;

			boolean guruDanSiswaHanyaBolehMelihatMatpelSatuSekolah = Common.bolehKonfigurasi("guru_dan_siswa_hanya_boleh_melihat_matpel_satu_sekolah");

			Criterion criterion = Restrictions.or(Restrictions.eq("guru", guru), Restrictions.eq("guru2", guru));

			criterion = Restrictions.or(criterion, Restrictions.eq("guru3", guru));
			criterion = Restrictions.or(criterion, Restrictions.eq("guru4", guru));
			criterion = Restrictions.or(criterion, Restrictions.eq("guru5", guru));
			criterion = Restrictions.or(criterion, Restrictions.eq("guru6", guru));
			criterion = Restrictions.or(criterion, Restrictions.eq("guru7", guru));
			criterion = Restrictions.or(criterion, Restrictions.eq("guru8", guru));
			criterion = Restrictions.or(criterion, Restrictions.eq("guru9", guru));
			criterion = Restrictions.or(criterion, Restrictions.eq("guru10", guru));
			criterion = Restrictions.or(criterion, Restrictions.eq("guru11", guru));
			criterion = Restrictions.or(criterion, Restrictions.eq("guru12", guru));

			criteria = session.createCriteria(JadwalPelajaran.class)
					.add(guru == null ? Restrictions.sqlRestriction("1=1") : criterion)
					.add(yayasan == null || yayasan.getId() == null
							|| (!guruDanSiswaHanyaBolehMelihatMatpelSatuSekolah && (guru != null || siswa != null))
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("yayasan", yayasan))
					.add(sekolah == null || sekolah.getId() == null
							|| (!guruDanSiswaHanyaBolehMelihatMatpelSatuSekolah && (guru != null || siswa != null))
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("sekolah", sekolah));

			try {
				Sekolah sekolah2Data = SekolahUtil.getSekolah();
				if (sekolah2Data != null && sekolah2Data.getId() != null) {
					criteria.add(Restrictions.eq("sekolah", sekolah2Data));
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:2493");
				// TODO: handle exception
			}

			if (siswa != null) {
				String sql = "this_.kelas_id in (select kelas_id from sekolah.kelas_punya_siswa where siswa_id="
						+ siswa.getId() + " and kelas_id is not null and aktif=true group by kelas_id)";
				Criterion criterionMhs = Restrictions.sqlRestriction(sql);

				sql = "this_.kelas_les_siswa in (select kelas_id from sekolah.kelas_les_punya_siswa where siswa_id="
						+ siswa.getId() + " and kelas_id is not null and aktif=true group by kelas_id)";
				Criterion criterionLes = Restrictions.sqlRestriction(sql);

				criteria.add(Restrictions.or(criterionMhs, criterionLes));
			}

			if (!kelas.trim().isEmpty()) {
				criteria.createAlias("kelas", "kelas", Criteria.LEFT_JOIN).createAlias("kelasLesSiswa", "kelasLesSiswa",
						Criteria.LEFT_JOIN);
				Criterion criterion1 = Restrictions.or(
						Restrictions.ilike("kelas.nama", kelas.trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("kelasLesSiswa.nama", kelas.trim(), MatchMode.ANYWHERE));
				criteria.add(criterion1);
			}

			criteria.add(Restrictions.isNotNull("tahunAjaran"))

					.add(tahunAjaran == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("tahunAjaran", tahunAjaran))

					.add(semester == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.sqlRestriction(
									"this_.semester % 2 = " + (semester.equals(Perkuliahan.GANJIL) ? "1" : "0")))

					.add(hari == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions
									.or(Restrictions.eq("hari5", hari),
											Restrictions.or(Restrictions.eq("hari4", hari),
													Restrictions.or(Restrictions.eq("hari3", hari),
															Restrictions.or(Restrictions.eq("hari", hari),
																	Restrictions.eq("hari2", hari))))));

			if (!keyword.trim().isEmpty()) {
				criteria.createAlias("matapelajaran", "matapelajaran")
						.add(Restrictions.or(
								Restrictions.ilike("matapelajaran.kode", keyword.trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("matapelajaran.nama", keyword.trim(), MatchMode.ANYWHERE)));
			}

			// Filter "Pengajar" (guru) — WAJIB ditambahkan SEBELUM ORDER BY di bawah agar SQL valid.
			terapkanFilterPengajar(criteria, jenis);

			if (order) {
				criteria.add(Restrictions.sqlRestriction(
						"1=1 order by tahun_ajaran desc, (this_.semester%2) asc,case hari when 'Senin' then 1 when 'Selasa' then 2 when 'Rabu' then 3 when 'Kamis' then 4 when 'Jumat' then 5  when 'Sabtu' then 6 when 'Minggu' then 7 else 5 end, waktumulai"));
			}

			return criteria;
		}

		else if (jenis.equals(KEGIATAN)) {

			Criteria criteria = session.createCriteria(FormulirKegiatan.class)

					.add(jenisFormulirKegiatan == null ? Restrictions.isNull("jenisFormulirKegiatan")
							: Restrictions.eq("jenisFormulirKegiatan", jenisFormulirKegiatan))

					.add(fakultas == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("fakultas", fakultas))
					.add(jurusan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", jurusan))
					.add(program == null || program.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("program", program));

			try {
				Sekolah sekolah2Data = SekolahUtil.getSekolah();
				if (sekolah2Data != null && sekolah2Data.getId() != null) {
					criteria.add(Restrictions.eq("sekolah", sekolah2Data));
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:2570");
				// TODO: handle exception
			}

			if (!keyword.trim().isEmpty()) {

				Criterion criterion = Restrictions.or(Restrictions.ilike("nama", keyword.trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("keterangan", keyword.trim(), MatchMode.ANYWHERE));

				criteria.add(criterion);

			}

			criteria.add(Restrictions.isNotNull("tahunAkademik"))

					.add(tahunAjaran == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("tahunAkademik", tahunAjaran))

					.add(semester == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("semester", semester));

			if (order) {
				criteria.addOrder(Order.desc("tahunAkademik")).addOrder(Order.desc("semester"))
						.addOrder(Order.desc("id"));
			}

			return criteria;
		}

		else if (jenis.equals(WISUDA)) {

			Criteria criteria = session.createCriteria(Wisuda.class);

			if (!keyword.trim().isEmpty()) {

				Criterion criterion = Restrictions.or(Restrictions.ilike("moto", keyword.trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("keterangan", keyword.trim(), MatchMode.ANYWHERE));

				criteria.add(criterion);
			}

			criteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

			if (order) {
				criteria.addOrder(Order.desc("wisudaKe")).addOrder(Order.desc("id"));
			}

			return criteria;
		}

		else {
			return null;
		}
	}

	private North pencarianKanan(final Textbox cari, final Rows rows1, final Rows rows2, final Paging paging1,
			final Paging paging2) {

		North subNorth = new North();
		subNorth.setBorder("none");

		Toolbar hbox = new Toolbar();
		hbox.setAlign("center");
		hbox.setParent(subNorth);
		hbox.appendChild(new Space());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Cari:")));
		hbox.appendChild(new Space());
		cari.setParent(hbox);
		cari.setCols(mobileTampilan ? 10 : 15);

		cari.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (rows1 != null) {

					Common.clear(rows1);

				}
				final Label label1 = createProgressLabel(
						createRowsProgressPanel(rows1, "Memuat Tugas, Ujian, Materi",
								"Mengambil daftar materi dan aktivitas belajar.", 12),
						"Ambil data ...", 12);

				if (rows2 != null) {

					Common.clear(rows2);

				}
				final Label label2 = createProgressLabel(
						createRowsProgressPanel(rows2, "Memuat Diskusi",
								"Mengambil daftar komentar dan diskusi pembelajaran.", 12),
						"Ambil data ...", 12);

				Common.createDefaultTimerNoBusy(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (btnKanan == null || btnKanan.getTabAktif() == 1) {
							loadDataKanan(cari, rows1, paging1, false, MATERI, label1);
						} else {
							loadDataKanan(cari, rows2, paging2, false, KOMENTAR, label2);
						}
					}
				});
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (rows1 != null) {

					Common.clear(rows1);

				}
				final Label label1 = createProgressLabel(
						createRowsProgressPanel(rows1, "Memuat Tugas, Ujian, Materi",
								"Mengambil ulang daftar materi dan aktivitas belajar.", 12),
						"Ambil data ...", 12);

				if (rows2 != null) {

					Common.clear(rows2);

				}
				final Label label2 = createProgressLabel(
						createRowsProgressPanel(rows2, "Memuat Diskusi",
								"Mengambil ulang daftar komentar dan diskusi pembelajaran.", 12),
						"Ambil data ...", 12);

				Common.createDefaultTimerNoBusy(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadDataKanan(cari, rows1, paging1, true, MATERI, label1);
						loadDataKanan(cari, rows2, paging2, true, KOMENTAR, label2);
					}
				});

			}
		});

		button.setParent(hbox);

		return subNorth;
	}

	private void loadMenu() throws Exception {

		if (menu == null) {
			return;
		}

		if (menuKanan != null) {

			final Textbox cari = new Textbox();

			final Rows rows1 = new Rows();
			final Rows rows2 = new Rows();

			final Paging paging1 = new Paging();
			final Paging paging2 = new Paging();

			Borderlayout borderlayout = new Borderlayout();
			borderlayout.setParent(menuKanan);
			borderlayout.appendChild(pencarianKanan(cari, rows1, rows2, paging1, paging2));

			Center centerbaru = new Center();
			centerbaru.setBorder("none");
//			centerbaru.setTitle("Tugas, Ujian, dan Materi");
			borderlayout.appendChild(centerbaru);

			btnKanan = ais.ui.util.MyButtonTabbox.buat(centerbaru, "100%", null);
			centerMateri = btnKanan.tambahTab(1, "Tugas, Ujian, Materi", "/img/svg/task-line.svg");
			southKomentar = btnKanan.tambahTab(2, "Diskusi", "/img/svg/comment-2-text-line.svg");

			eventListenerMateri = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (menuKanan.isOpen() && (reloadBlnSd || centerMateri.getChildren().isEmpty()
							|| southKomentar.getChildren().isEmpty())) {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								if (btnKanan == null || btnKanan.getTabAktif() == 1) {
									loadMenuKanan(centerMateri, cari, rows1, paging1, TampilanELearningAction.MATERI,
											reloadBlnSd);
								} else {
									loadMenuKanan(southKomentar, cari, rows2, paging2, TampilanELearningAction.KOMENTAR,
											reloadBlnSd);
								}

								reloadBlnSd = false;

								// SELF-HEAL panel KIRI "Perkuliahan & Kelas": bila DATA daftarnya belum termuat,
								// picu ulang DI DALAM Timer ANDAL INI — yaitu Timer yang SAMA yang memuat panel
								// KANAN (TERBUKTI selalu tampil). Timer ini dibuat di konteks menuKanan ON_OPEN
								// (post-render) sehingga inner timer loadData yang dijadwalkan reloadDataKiri IKUT
								// fire → daftar Perkuliahan/Kelas muncul tanpa perlu memilih combo dulu. Idempoten:
								// dataKiriTampil jadi true begitu loadData benar-benar jalan → tak dipicu berulang.
								if (!dataKiriTampil && reloadDataKiri != null) {
									try {
										reloadDataKiri.onEvent(null);
									} catch (Exception eHealKiri) {
										ais.common.Common.tampilErrorJikaAdmin(eHealKiri);
									}
								}

								// SELF-HEAL panel TENGAH "Linimasa": bila komponen timeline SUDAH ada TAPI
								// daftar pertemuan (pertemuansa) belum termuat — mis. pemuatan awal
								// initSpreadsheet yang dijadwalkan saat konstruksi/include TAK ter-fire —
								// picu ULANG di konteks ANDAL ini (menuKanan ON_OPEN, post-render, jalur
								// yang sama yang membuat panel KANAN selalu tampil). Saat timeline selesai
								// memuat, callback-nya (eventRefresh) memanggil eventListenerMateri lagi
								// sehingga panel KANAN ikut terisi datanya (self-heal berantai). Idempoten:
								// hanya dipicu selama pertemuansa masih null.
								if (dashboardTimelinePertemuan != null
										&& dashboardTimelinePertemuan.pertemuansa == null) {
									try {
										dashboardTimelinePertemuan.initSpreadsheet(false, true);
									} catch (Exception eHealTengah) {
										ais.common.Common.tampilErrorJikaAdmin(eHealTengah);
									}
								}
							}
						});
					}

				}
			};

			menuKanan.addEventListener(Events.ON_OPEN, eventListenerMateri);
			btnKanan.onSetiapPilih(2, eventListenerMateri);
		}

		Vbox menuShell = new Vbox();
		menuShell.setParent(menu);
		menuShell.setWidth("100%");
		menuShell.setHeight("100%");
		menuShell.setSclass("elearning-menu-combo-shell");

		final Combobox comboMenu = new Combobox();
		comboMenu.setParent(menuShell);
		comboMenu.setWidth("100%");
		comboMenu.setReadonly(true);
		comboMenu.setSclass("elearning-menu-combo");

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(menuShell);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");
		tabbox.setSclass("elearning-menu-combo-tabbox");
//		tabbox.setMold("accordion");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		tabPerkuliahan = new MyTabConfig("Perkuliahan", "/img/svg/chalkboard-teacher-light.svg");
		tabPerkuliahan.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
		tabPerkuliahan.setParent(tabs);

		tabPelajaran = new MyTabConfig("Pelajaran", "/img/svg/book.svg");
		tabPelajaran.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
		tabPelajaran.setParent(tabs);

		tabSidang = new MyTabConfig("Sidang", "/img/svg/journal-check.svg");
		tabSidang.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
		tabSidang.setParent(tabs);

		tabBimbingan = new MyTabConfig("Bimbingan", "/img/svg/chalkboard-user.svg");
		tabBimbingan.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
		tabBimbingan.setParent(tabs);

		tabKkn = new MyTabConfig("KKN", "/img/svg/users.svg");
		tabKkn.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
		tabKkn.setParent(tabs);

		tabPkl = new MyTabConfig("PKL", "/img/svg/user-business.svg");
		tabPkl.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
		tabPkl.setParent(tabs);

		tabPA = new MyTabConfig("Pembimbing Akademik", "/img/svg/user-tie.svg");
		tabPA.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
		tabPA.setParent(tabs);

		List<JenisFormulirKegiatan> jenisFormulirKegiatans = new ArrayList<JenisFormulirKegiatan>();
		int adakosong = 0;
		try {
			Session session = HibernateUtil.currentNativeSession();
			jenisFormulirKegiatans = ConstantValues.simpleList(session.createCriteria(FormulirKegiatan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.isNotNull("jenisFormulirKegiatan"))
					.createAlias("jenisFormulirKegiatan", "jenisFormulirKegiatan")
					.setProjection(Projections.groupProperty("jenisFormulirKegiatan.id"))
					.add(Restrictions.eq("jenisFormulirKegiatan.aktif", true))
					.addOrder(Order.asc("jenisFormulirKegiatan.id")), JenisFormulirKegiatan.class, false);
			adakosong = ((Number) session.createCriteria(FormulirKegiatan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.isNull("jenisFormulirKegiatan")).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				closeHibernateSessionQuietly(session);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		HibernateUtil.closeSession();

		tabKegiatans = new ArrayList<MyTabConfig>();
		for (JenisFormulirKegiatan jenisFormulirKegiatan : jenisFormulirKegiatans) {
			MyTabConfig tabKegiatan = new MyTabConfig(jenisFormulirKegiatan.getNama(), "/img/svg/calendar-check.svg");
			tabKegiatan.setAttribute("jenisFormulirKegiatan", jenisFormulirKegiatan);
			tabKegiatan.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
			tabKegiatan.setParent(tabs);
			tabKegiatans.add(tabKegiatan);
		}
		if (adakosong > 0) {
			tabKegiatan = new MyTabConfig("Kegiatan Lain", "/img/svg/calendar-check.svg");
			tabKegiatan.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
			tabKegiatan.setParent(tabs);
		}
		tabKonsultasi = new MyTabConfig("Konsultasi", "/img/svg/comment-2-text-line.svg");
		tabKonsultasi.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
		tabKonsultasi.setParent(tabs);
		tabKonsultasi.setVisible(false);

		tabWisuda = new MyTabConfig("Wisuda", "/img/svg/graduation-cap-light.svg");
		tabWisuda.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
		tabWisuda.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		tabpanelUtamaPerkuliahan = new ais.ui.util.MyTabpanel();
		tabpanelUtamaPerkuliahan.setParent(tabpanels);

		EventListener eventListenerPerkuliahan = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (hanyaPlaceholderLoading(tabpanelUtamaPerkuliahan)) {
					loadMenu(tabpanelUtamaPerkuliahan, TampilanELearningAction.PERKULIAHAN);
				}
			}
		};

		tabPerkuliahan.addEventListener("onClick", eventListenerPerkuliahan);
		final Comboitem itemPerkuliahan = tambahPilihanMenuElearning(comboMenu, "Perkuliahan",
				"/img/svg/chalkboard-teacher-light.svg", tabPerkuliahan, eventListenerPerkuliahan);

		final Tabpanel tabpanelUtamaKedua = new ais.ui.util.MyTabpanel();
		tabpanelUtamaKedua.setParent(tabpanels);

		EventListener eventListenerPelajaran = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (hanyaPlaceholderLoading(tabpanelUtamaKedua)) {
					loadMenu(tabpanelUtamaKedua, TampilanELearningAction.PELAJARAN);
				}
			}
		};

		tabPelajaran.addEventListener("onClick", eventListenerPelajaran);
		final Comboitem itemPelajaran = tambahPilihanMenuElearning(comboMenu, "Pelajaran", "/img/svg/book.svg",
				tabPelajaran, eventListenerPelajaran);

		final Tabpanel tabpanelSidang = new ais.ui.util.MyTabpanel();
		tabpanelSidang.setParent(tabpanels);
		EventListener eventListenerSidang = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelSidang.getChildren().isEmpty()) {
					loadMenu(tabpanelSidang, TampilanELearningAction.SKRIPSI);
				}
			}
		};
		tabSidang.addEventListener("onClick", eventListenerSidang);
		final Comboitem itemSidang = tambahPilihanMenuElearning(comboMenu, "Sidang", "/img/svg/journal-check.svg",
				tabSidang, eventListenerSidang);

		final Tabpanel tabpanelBimbingan = new ais.ui.util.MyTabpanel();
		tabpanelBimbingan.setParent(tabpanels);
		EventListener eventListenerBimbingan = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// Tab "Bimbingan" KONSOLIDASI: nested Tabbox berisi sub-tab Tugas Akhir/Sidang/KKN/PKL/PA.
				if (tabpanelBimbingan.getChildren().isEmpty()) {
					bangunTabBimbingan(tabpanelBimbingan);
				}
			}
		};
		tabBimbingan.addEventListener("onClick", eventListenerBimbingan);
		final Comboitem itemBimbingan = tambahPilihanMenuElearning(comboMenu, "Bimbingan",
				"/img/svg/chalkboard-user.svg", tabBimbingan, eventListenerBimbingan);

		final Tabpanel tabpanelKkn = new ais.ui.util.MyTabpanel();
		tabpanelKkn.setParent(tabpanels);
		EventListener eventListenerKkn = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelKkn.getChildren().isEmpty()) {
					loadMenu(tabpanelKkn, TampilanELearningAction.KKN);
				}
			}
		};
		tabKkn.addEventListener("onClick", eventListenerKkn);
		final Comboitem itemKkn = tambahPilihanMenuElearning(comboMenu, "KKN", "/img/svg/users.svg", tabKkn,
				eventListenerKkn);

		final Tabpanel tabpanelPkl = new ais.ui.util.MyTabpanel();
		tabpanelPkl.setParent(tabpanels);
		EventListener eventListenerPkl = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPkl.getChildren().isEmpty()) {
					loadMenu(tabpanelPkl, TampilanELearningAction.PKL);
				}
			}
		};
		tabPkl.addEventListener("onClick", eventListenerPkl);
		final Comboitem itemPkl = tambahPilihanMenuElearning(comboMenu, "PKL", "/img/svg/user-business.svg",
				tabPkl, eventListenerPkl);

		final Tabpanel tabpanelPA = new ais.ui.util.MyTabpanel();
		tabpanelPA.setParent(tabpanels);
		EventListener eventListenerPA = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPA.getChildren().isEmpty()) {
					loadMenu(tabpanelPA, TampilanELearningAction.KRS);
				}
			}
		};
		tabPA.addEventListener("onClick", eventListenerPA);
		final Comboitem itemPA = tambahPilihanMenuElearning(comboMenu, "Pembimbing Akademik",
				"/img/svg/user-tie.svg", tabPA, eventListenerPA);

		final List<Comboitem> itemKegiatans = new ArrayList<Comboitem>();
		for (MyTabConfig myTabConfig : tabKegiatans) {
			final JenisFormulirKegiatan jenisFormulirKegiatan = (JenisFormulirKegiatan) myTabConfig
					.getAttribute("jenisFormulirKegiatan");
			final Tabpanel tabpanelKegiatan = new ais.ui.util.MyTabpanel();
			tabpanelKegiatan.setParent(tabpanels);
			EventListener eventListenerKegiatan = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelKegiatan.getChildren().isEmpty()) {
						loadMenu(tabpanelKegiatan, TampilanELearningAction.KEGIATAN, jenisFormulirKegiatan);
					}
				}
			};
			myTabConfig.addEventListener("onClick", eventListenerKegiatan);
			itemKegiatans.add(tambahPilihanMenuElearning(comboMenu, myTabConfig.getLabel(),
					"/img/svg/calendar-check.svg", myTabConfig, eventListenerKegiatan));
		}

		final Comboitem itemKegiatanLain;
		if (adakosong > 0) {
			final Tabpanel tabpanelKegiatan = new ais.ui.util.MyTabpanel();
			tabpanelKegiatan.setParent(tabpanels);
			EventListener eventListenerKegiatanLain = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelKegiatan.getChildren().isEmpty()) {
						loadMenu(tabpanelKegiatan, TampilanELearningAction.KEGIATAN);
					}
				}
			};
			tabKegiatan.addEventListener("onClick", eventListenerKegiatanLain);
			itemKegiatanLain = tambahPilihanMenuElearning(comboMenu, "Kegiatan Lain",
					"/img/svg/calendar-check.svg", tabKegiatan, eventListenerKegiatanLain);
		} else {
			itemKegiatanLain = null;
		}

		final Tabpanel tabpanelKonsultasi = new ais.ui.util.MyTabpanel();
		tabpanelKonsultasi.setParent(tabpanels);
		EventListener eventListenerKonsultasi = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelKonsultasi.getChildren().isEmpty()) {
					loadMenu(tabpanelKonsultasi, TampilanELearningAction.KONSULTASI);
				}
			}
		};
		tabKonsultasi.addEventListener("onClick", eventListenerKonsultasi);
		final Comboitem itemKonsultasi = tambahPilihanMenuElearning(comboMenu, "Konsultasi",
				"/img/svg/comment-2-text-line.svg", tabKonsultasi, eventListenerKonsultasi);

		final Tabpanel tabpanelWisuda = new ais.ui.util.MyTabpanel();
		tabpanelWisuda.setParent(tabpanels);
		EventListener eventListenerWisuda = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelWisuda.getChildren().isEmpty()) {
					loadMenu(tabpanelWisuda, TampilanELearningAction.WISUDA);
				}
			}
		};
		tabWisuda.addEventListener("onClick", eventListenerWisuda);
		final Comboitem itemWisuda = tambahPilihanMenuElearning(comboMenu, "Wisuda",
				"/img/svg/graduation-cap-light.svg", tabWisuda, eventListenerWisuda);

		// Panel KIRI dikembalikan ke daftar SEMULA (dropdown/tab): Perkuliahan, Pelajaran (sekolah/yayasan),
		// Sidang, Bimbingan, KKN, PKL (PT & sekolah), Pembimbing Akademik, Wisuda. Fitur "Bimbingan Saya"
		// versi konsolidasi (5 sub-tab) TETAP tersedia sebagai tab tersendiri di panel TENGAH
		// (initBimbinganTab()), jadi navigasi kiri tidak berkurang. Kondisi mengikuti pola tab lain:
		// pt=konteks Perguruan Tinggi, ya=konteks sekolah/yayasan. PKL tampil di kedua konteks.
		tabSidang.setVisible(pt);
		tabBimbingan.setVisible(pt);
		tabKkn.setVisible(pt);
		tabPkl.setVisible(pt || ya);
		tabPA.setVisible(pt);
		tabKonsultasi.setVisible(false);
		tabPerkuliahan.setVisible(pt);

		tabpanelWisuda.setVisible(pt && tbmuser != null && tbmuser.ambilDosen() == null);
		tabWisuda.setVisible(pt && tbmuser != null && tbmuser.ambilDosen() == null);
		tabPelajaran
				.setVisible(ya && tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);

		itemSidang.setVisible(tabSidang.isVisible());
		itemBimbingan.setVisible(tabBimbingan.isVisible());
		itemKkn.setVisible(tabKkn.isVisible());
		itemPkl.setVisible(tabPkl.isVisible());
		itemPA.setVisible(tabPA.isVisible());
		itemKonsultasi.setVisible(tabKonsultasi.isVisible());
		itemPerkuliahan.setVisible(tabPerkuliahan.isVisible());
		itemWisuda.setVisible(tabWisuda.isVisible());
		itemPelajaran.setVisible(tabPelajaran.isVisible());
		for (Comboitem itemKegiatan : itemKegiatans) {
			itemKegiatan.setVisible(true);
		}
		if (itemKegiatanLain != null) {
			itemKegiatanLain.setVisible(true);
		}

		comboMenu.addEventListener(Events.ON_SELECT, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				pilihMenuElearning(comboMenu, comboMenu.getSelectedItem());
			}
		});

		/*
		 * Muat pilihan menu AWAL (Perkuliahan / Pelajaran) LANGSUNG & SINKRON di sini — PERSIS pola
		 * versi LAMA (vertical tab) yang TERBUKTI JALAN: dulu di akhir loadMenu() cukup memanggil
		 * `eventListenerPerkuliahan.onEvent(null)` secara langsung, tanpa penundaan apa pun.
		 *
		 * TIDAK memakai echoEvent / Timer / server-push / JavaScript sama sekali, untuk menghindari:
		 *  (a) flaky: echo/timer kerap TAK ter-fire saat halaman di-include -> panel kosong;
		 *  (b) error ZK client "Cannot read properties of null (reading 'set')" akibat Timer di halaman
		 *      utama memanipulasi komponen halaman include (lintas-page);
		 *  (c) bentrok JavaScript (sesuai permintaan: JANGAN pakai JavaScript).
		 *
		 * pilihMenuElearning(combo, item) memilih item + memanggil listener tab -> loadMenu(3-arg),
		 * identik dengan versi lama yang me-render daftar Perkuliahan pada akses pertama.
		 */
		this.comboMenuAwalElearning = comboMenu;
		this.itemMenuAwalElearning = pt ? itemPerkuliahan : (ya ? itemPelajaran : null);
		this.tabpanelAwalElearning = pt ? tabpanelUtamaPerkuliahan : (ya ? tabpanelUtamaKedua : null);
		if (this.itemMenuAwalElearning != null) {
			// GAMBAR LOADING DULUAN: tampilkan indikator loading di panel default (tabpanel pertama,
			// terlihat by default) SEBELUM Timer/ON_OPEN memicu load — jadi saat pertama diakses
			// pengguna langsung melihat loading, bukan panel kosong. Placeholder ini DIBERSIHKAN di
			// jalankanLoadAwalElearning() tepat sebelum data dimuat (agar listener menu yang meng-cek
			// getChildren().isEmpty() tetap menjalankan loadMenu).
			if (this.tabpanelAwalElearning != null && this.tabpanelAwalElearning.getChildren().isEmpty()) {
				// PROGRESS BAR panel KIRI (seperti panel tengah/kanan): beri umpan balik visual saat
				// data awal disiapkan, bukan kotak kosong. Tetap MyHtml agar kontenAwalElearningSudahTampil()
				// masih menganggap ini "belum tampil" (self-heal tetap memuat ulang bila perlu). Bar
				// indeterminate (aisIndet) + spinner (aisSpin). Begitu daftar termuat, placeholder ini
				// diganti oleh Grid data (loadData juga punya progress bar-nya sendiri via createRowsProgressPanel).
				ais.ui.util.MyHtml loadingAwal = new ais.ui.util.MyHtml(
						"<div style='padding:16px 14px;'>"
								+ "<div style='font-size:10px;font-weight:800;letter-spacing:.09em;color:#94a3b8;text-transform:uppercase;margin-bottom:5px;'>Memproses panel</div>"
								+ "<div style='display:flex;align-items:center;gap:9px;margin-bottom:3px;'>"
								+ "<div style='flex:none;width:22px;height:22px;border:3px solid #e2e8f0;border-top-color:#7b2ff7;border-radius:50%;animation:aisSpin .8s linear infinite;'></div>"
								+ "<div style='font-weight:800;color:#1e293b;font-size:15px;line-height:1.2;'>Memuat Perkuliahan &amp; Kelas</div>"
								+ "</div>"
								+ "<div style='font-size:11.5px;color:#64748b;margin:2px 0 12px 0;'>Menyiapkan daftar perkuliahan/pelajaran, kelas, dan status...</div>"
								+ "<div style='height:10px;border-radius:8px;background:#eef2f7;overflow:hidden;position:relative;'>"
								+ "<div style='position:absolute;top:0;bottom:0;left:-45%;width:45%;border-radius:8px;background:linear-gradient(90deg,#3b0a63,#7b2ff7,#e11d48);animation:aisIndet 1.15s ease-in-out infinite;'></div>"
								+ "</div>"
								+ "<style>@keyframes aisIndet{0%{left:-45%}100%{left:100%}}</style>"
								+ "</div>");
				loadingAwal.setParent(this.tabpanelAwalElearning);
			}
			// SEMUA PANEL diberi indikator loading saat akses pertama (bukan hanya panel kiri):
			// panel TENGAH "Aktivitas Pembelajaran" (tabpanel Linimasa) & panel KANAN "Tugas, Ujian,
			// Materi & Diskusi" (centerMateri). Placeholder DI-DETACH di jalankanLoadAwalElearning()
			// SEBELUM data dimuat, agar cek getChildren().isEmpty() pada listener load tetap lolos
			// sehingga konten asli tetap termuat. Gaya spinner sama dgn panel kiri (animasi aisSpin).
			if (tabpanelTimeline != null && tabpanelTimeline.getChildren().isEmpty()) {
				// PROGRESS BAR panel TENGAH (samakan dgn panel kiri): tampilkan bar progres beranimasi
				// (aisIndet) + spinner, bukan sekadar spinner — agar SEMUA panel "menampilkan progress"
				// saat data disiapkan. Konten asli tetap menggantikan placeholder saat termuat.
				loadingAwalTengah = new ais.ui.util.MyHtml(
						"<div style='padding:16px 14px;'>"
								+ "<div style='font-size:10px;font-weight:800;letter-spacing:.09em;color:#94a3b8;text-transform:uppercase;margin-bottom:5px;'>Memproses panel</div>"
								+ "<div style='display:flex;align-items:center;gap:9px;margin-bottom:3px;'>"
								+ "<div style='flex:none;width:22px;height:22px;border:3px solid #e2e8f0;border-top-color:#2563eb;border-radius:50%;animation:aisSpin .8s linear infinite;'></div>"
								+ "<div style='font-weight:800;color:#1e293b;font-size:15px;line-height:1.2;'>Memuat Aktivitas Pembelajaran</div>"
								+ "</div>"
								+ "<div style='font-size:11.5px;color:#64748b;margin:2px 0 12px 0;'>Menyiapkan linimasa &amp; aktivitas pembelajaran...</div>"
								+ "<div style='height:10px;border-radius:8px;background:#eef2f7;overflow:hidden;position:relative;'>"
								+ "<div style='position:absolute;top:0;bottom:0;left:-45%;width:45%;border-radius:8px;background:linear-gradient(90deg,#0c4a6e,#2563eb,#06b6d4);animation:aisIndet 1.15s ease-in-out infinite;'></div>"
								+ "</div>"
								+ "<style>@keyframes aisIndet{0%{left:-45%}100%{left:100%}}</style>"
								+ "</div>");
				loadingAwalTengah.setParent(tabpanelTimeline);
			}
			if (centerMateri != null && centerMateri.getChildren().isEmpty()) {
				// PROGRESS BAR panel KANAN (samakan dgn panel kiri/tengah): bar progres beranimasi.
				loadingAwalKanan = new ais.ui.util.MyHtml(
						"<div style='padding:16px 14px;'>"
								+ "<div style='font-size:10px;font-weight:800;letter-spacing:.09em;color:#94a3b8;text-transform:uppercase;margin-bottom:5px;'>Memproses panel</div>"
								+ "<div style='display:flex;align-items:center;gap:9px;margin-bottom:3px;'>"
								+ "<div style='flex:none;width:22px;height:22px;border:3px solid #e2e8f0;border-top-color:#059669;border-radius:50%;animation:aisSpin .8s linear infinite;'></div>"
								+ "<div style='font-weight:800;color:#1e293b;font-size:15px;line-height:1.2;'>Memuat Tugas, Ujian, Materi</div>"
								+ "</div>"
								+ "<div style='font-size:11.5px;color:#64748b;margin:2px 0 12px 0;'>Menyiapkan daftar tugas, ujian, materi &amp; diskusi...</div>"
								+ "<div style='height:10px;border-radius:8px;background:#eef2f7;overflow:hidden;position:relative;'>"
								+ "<div style='position:absolute;top:0;bottom:0;left:-45%;width:45%;border-radius:8px;background:linear-gradient(90deg,#064e3b,#059669,#34d399);animation:aisIndet 1.15s ease-in-out infinite;'></div>"
								+ "</div>"
								+ "<style>@keyframes aisIndet{0%{left:-45%}100%{left:100%}}</style>"
								+ "</div>");
				loadingAwalKanan.setParent(centerMateri);
			}
			// PEMICU UTAMA (andal) — MENIRU panel kanan "Tugas, Ujian, Materi & Diskusi" yang PASTI
			// keluar datanya: panel kanan memuat pada event region Events.ON_OPEN (menuKanan). ON_OPEN
			// adalah event AU NORMAL yang dikirim klien SAAT region ter-render (sama konteks dengan
			// user mengubah combo), sehingga render loadData berjalan andal — tidak seperti Timer yang
			// dibuat saat halaman masih di-INCLUDE (kerap tak fire → panel kiri kosong akses pertama).
			// ON_OPEN dipasang pada region KIRI ("menu") sendiri = same-page (aman dari error setAttr
			// null lintas-page). Idempoten via flag loadAwalElearningDijalankan.
			if (menu != null) {
				menu.addEventListener(Events.ON_OPEN, new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						jalankanLoadAwalElearning();
					}
				});
			}
			// PEMICU PALING ANDAL — PIGGYBACK ke region KANAN (menuKanan). Panel kanan "Tugas, Ujian,
			// Materi & Diskusi" TERBUKTI SELALU muncul karena memuat pada Events.ON_OPEN region-nya
			// (menuKanan) yang PASTI ter-fire klien saat borderlayout ter-render. Region KIRI ("menu")
			// kadang TAK mengirim ON_OPEN pada akses pertama → panel kiri kosong. Karena SAAT menuKanan
			// ter-render seluruh borderlayout (termasuk panel kiri) sudah ada di klien, memicu pemuatan
			// panel KIRI dari ON_OPEN menuKanan itu AMAN & ANDAL — "meniru cara panel kanan" persis
			// permintaan pengguna. Idempoten via kontenAwalElearningSudahTampil() (tak dobel-load).
			if (menuKanan != null) {
				menuKanan.addEventListener(Events.ON_OPEN, new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						jalankanLoadAwalElearning();
					}
				});
			}
			// Muat menu awal lewat Common.createDefaultTimerNoBusy: memicu logika yg SAMA dengan saat
			// combo dipilih user (pilihMenuElearning -> loadMenu(3-arg) -> loadData). Timer ini men-DEFER
			// ke event AU PASCA-RENDER (mekanisme ZK Timer standar yg dipakai di banyak panel lain &
			// terbukti fire). SEBAB perlu di-defer: bila dipanggil LANGSUNG saat doAfterCompose (halaman
			// e-Learning masih disusun sebagai include), render server-push loadData kerap TAK sampai ke
			// klien -> panel "Perkuliahan & Kelas" kosong sampai user mengubah combo. TANPA JavaScript.
			// Delay 1 detik (1000ms) sebelum load — memberi waktu halaman/include ter-render di klien
			// lebih dulu, baru daftar Perkuliahan dimuat (overload: info="", repeat=false, interval=1000).
			Common.createDefaultTimerNoBusy(new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					jalankanLoadAwalElearning();
				}
			}, "", false, 1000);

			// PEMICU CADANGAN (self-healing) untuk panel KIRI "Perkuliahan & Kelas": kadang panel ini
			// belum selesai ter-render di klien saat Timer 1 detik pertama fire (tergantung kecepatan
			// render include), sehingga daftar perkuliahan/kelas KOSONG dan tidak diperbaiki lagi.
			// Timer kedua (2,5 detik) memanggil ulang jalankanLoadAwalElearning() — AMAN karena
			// idempoten: hanya memuat bila konten kiri masih kosong (kontenAwalElearningSudahTampil()
			// -> false). Meniru keandalan pemuatan panel tengah "Aktivitas Pembelajaran".
			Common.createDefaultTimerNoBusy(new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					jalankanLoadAwalElearning();
				}
			}, "", false, 2500);

			// ── PEMICU UTAMA: EAGER + SINKRON saat compose — MENYAMAKAN dengan panel TENGAH "Aktivitas
			// Pembelajaran" yang TIDAK PERNAH gagal tampil. Panel tengah andal karena komponen kontennya
			// di-append LANGSUNG ke tab saat init() → ikut ter-compose & DIKIRIM BERSAMA halaman pada render
			// pertama, tidak menunggu Timer/ON_OPEN (yang dibuat saat include masih dirender & KERAP tak
			// fire → panel kiri "kadang muncul kadang tidak"). Panel KIRI "Perkuliahan & Kelas" kini dibangun
			// dengan cara yang sama: jalankanLoadAwalElearning() dipanggil LANGSUNG di sini (masih di dalam
			// doAfterCompose → init → loadMenu) sehingga FORM FILTER + TOOLBAR (tombol Pencarian & Refresh) +
			// GRID ikut ter-compose = PASTI tampil pada render pertama — sekaligus memperbaiki keluhan tombol
			// Pencarian/Refresh yang tak muncul (selama ini panel kiri tak pernah termuat). Pengambilan DATA
			// daftar tetap di-defer loadData() ke createDefaultTimerNoBusy internal (jalur AU andal). Timer
			// 1 dtk/2,5 dtk & ON_OPEN di atas dibiarkan sebagai penambal self-healing (idempoten via
			// kontenAwalElearningSudahTampil()). DIBATASI ke layout portal desktop (!mobileTampilan) — layout
			// mobile punya alurnya sendiri, JANGAN diubah dari sini. try/catch agar kegagalan eager tak
			// menggagalkan render halaman (penambal akan memuat ulang).
			if (!mobileTampilan) {
				try {
					jalankanLoadAwalElearning();
				} catch (Exception eEagerKiri) {
					ais.common.Common.tampilErrorJikaAdmin(eEagerKiri);
				}
			}
		}
	}

	/** Combo & item menu awal e-Learning (diisi di loadMenu) untuk pemicu load awal. */
	private Combobox comboMenuAwalElearning = null;
	private Comboitem itemMenuAwalElearning = null;
	/** Tabpanel default (Perkuliahan/Pelajaran) tempat loading placeholder awal ditampilkan. */
	private Tabpanel tabpanelAwalElearning = null;
	/** Penjaga agar pemuatan awal hanya berjalan sekali (idempoten). */
	private boolean loadAwalElearningDijalankan = false;
	/**
	 * Listener pemuat-ulang DATA daftar panel kiri (di-set di {@link #loadMenu(Component, Integer, JenisFormulirKegiatan)}
	 * saat membangun tab awal). Dipakai sebagai penambal SELF-HEALING: jika form panel kiri sudah
	 * terbangun (eager saat compose) tetapi inner timer {@code loadData} yang dibuat saat compose
	 * TIDAK fire sehingga daftar kosong, listener ini dipicu ulang dari jalur ANDAL (menuKanan
	 * ON_OPEN) di konteks post-render sehingga fetch data berjalan &amp; daftar muncul.
	 */
	private EventListener reloadDataKiri = null;
	/**
	 * Menjadi {@code true} begitu pemuatan DATA daftar panel kiri BENAR-BENAR berjalan (yakni
	 * {@code loadDoneListener} di dalam {@code loadData} tereksekusi = inner timer fetch fire).
	 * Bila masih {@code false} padahal form sudah tampil, artinya data belum termuat → picu ulang.
	 */
	private boolean dataKiriTampil = false;

	/**
	 * Menjalankan pemilihan menu e-Learning AWAL (Perkuliahan/Pelajaran) satu kali saja, LANGSUNG &
	 * SINKRON (dipanggil dari akhir loadMenu()) — pola versi lama yg terbukti jalan; tanpa
	 * echoEvent/Timer/server-push/JavaScript. Idempoten via flag.
	 */
	private void jalankanLoadAwalElearning() throws Exception {
		// Idempoten BERBASIS KONTEN — MENIRU panel kanan yang andal: panel kanan
		// (eventListenerMateri) memuat ulang SELAMA kontennya masih kosong
		// (centerMateri.getChildren().isEmpty()) setiap kali ON_OPEN, sehingga satu
		// percobaan yang gagal akan sembuh sendiri pada pemicu berikutnya.
		//
		// Sebelumnya panel KIRI memakai flag sekali-jalan: bila Timer yang dibuat saat
		// halaman masih di-INCLUDE sempat fire lebih dulu, meng-set flag, TAPI render-nya
		// tak sampai ke klien, maka pemicu andal (ON_OPEN region kiri) IKUT terblokir
		// selamanya → daftar "kadang muncul kadang tidak". Kini: bila daftar sudah benar-
		// benar ter-render, berhenti; bila flag sudah true TAPI konten masih kosong
		// (percobaan sebelumnya gagal), IZINKAN dijalankan ulang agar data tetap muncul.
		if (loadAwalElearningDijalankan && kontenAwalElearningSudahTampil()) {
			// FORM panel kiri sudah terbangun (toolbar Pencarian/Refresh + grid). Namun bisa jadi
			// DATA daftarnya BELUM termuat: pembangunan form dilakukan EAGER saat compose supaya
			// toolbar pasti tampil, tetapi inner timer loadData yang dibuat saat compose KERAP tak
			// fire (sama sebabnya dg panel kiri dulu) → daftar kosong sampai user memilih combo.
			// Karena pemicu INI berjalan di konteks post-render yang ANDAL (menuKanan ON_OPEN /
			// Timer), picu ulang pemuatan DATA lewat reloadDataKiri di sini → inner timer loadData
			// kini dibuat post-render → fire → daftar muncul. Meniru pola self-healing panel KANAN
			// (eventListenerMateri memuat ulang selama centerMateri masih kosong). Idempoten:
			// dataKiriTampil menjadi true begitu loadData benar-benar jalan → tak dipicu berulang.
			if (!dataKiriTampil && reloadDataKiri != null) {
				try {
					reloadDataKiri.onEvent(null);
				} catch (Exception eReloadKiri) {
					ais.common.Common.tampilErrorJikaAdmin(eReloadKiri);
				}
			}
			// Tetap PASTIKAN panel KANAN ("Tugas, Ujian, Materi & Diskusi") ikut terisi. Pada mode
			// "mobile versi desktop" (HP membuka Mode Desktop → viewport sempit TETAP merender
			// layout portal 3 kolom), event region East ON_OPEN (menuKanan) kerap TAK ter-fire
			// klien sehingga eventListenerMateri tak jalan → panel ke-3 kosong. Pemicu andal ini
			// menambalnya (self-healing).
			picuKontenPanelKanan();
			return;
		}
		loadAwalElearningDijalankan = true;
		// PERBAIKAN "panel kiri/tengah kosong saat pertama tampil": indikator loading awal panel KIRI
		// ("Perkuliahan & Kelas") dan TENGAH ("Aktivitas Pembelajaran") SENGAJA TIDAK dilepas di sini.
		//   • KIRI: placeholder dilepas oleh loadMenu() (Common.clear(parent)) TEPAT sebelum form/daftar
		//     dibangun. Guard listener tab kini memakai hanyaPlaceholderLoading() (bukan getChildren()
		//     .isEmpty()) sehingga loadMenu tetap terpicu walau placeholder masih terpasang. Hasilnya
		//     indikator loading tetap terlihat sampai daftar benar-benar dirender (tanpa kedip kosong).
		//   • TENGAH: placeholder dilepas oleh bersihkanLoadingTengah() TEPAT sebelum timeline di-append
		//     (di loadDoneListener panel kiri / path mobile). Karena timeline baru dibangun SETELAH data
		//     panel kiri selesai diambil, mempertahankan placeholder membuat panel tengah tetap
		//     menampilkan loading selama proses (bukan kosong berdetik-detik) — meniru panel KANAN.
		// Panel KANAN tetap dilepas di sini karena konten kanan (eventListenerMateri via
		// picuKontenPanelKanan) memasang indikator loading-nya SENDIRI secara sinkron setelahnya
		// (peralihan mulus), dan guard konten kanan (centerMateri.isEmpty()) harus lolos.
		if (loadingAwalKanan != null) {
			loadingAwalKanan.detach();
			loadingAwalKanan = null;
		}
		if (comboMenuAwalElearning != null && itemMenuAwalElearning != null) {
			pilihMenuElearning(comboMenuAwalElearning, itemMenuAwalElearning);
		}
		// Placeholder loading panel KANAN sudah di-detach di atas → centerMateri kosong. Picu
		// pemuatan panel ke-3 ("Tugas, Ujian, Materi & Diskusi") lewat jalur ANDAL yang sama
		// dengan panel kiri/tengah — TIDAK hanya mengandalkan East ON_OPEN (menuKanan) yang
		// flaky pada mobile-versi-desktop. Idempoten & aman (lihat picuKontenPanelKanan()).
		picuKontenPanelKanan();
	}

	/**
	 * Memicu pemuatan konten panel KANAN e-Learning ("Tugas, Ujian, Materi &amp; Diskusi") lewat
	 * jalur ANDAL yang sama dengan panel kiri/tengah, sebagai penambal kasus <b>mobile versi
	 * desktop</b>: ketika perangkat HP membuka Mode Desktop, viewport sempit TETAP merender
	 * layout portal 3 kolom ({@link #initPortalRootLayout()}), namun event region {@code East}
	 * {@code ON_OPEN} (menuKanan) — satu-satunya pemicu {@link #eventListenerMateri} — kerap TAK
	 * ter-fire oleh klien pada render sempit tersebut, sehingga {@link #centerMateri} tak pernah
	 * terisi dan panel ke-3 tampak kosong padahal panel kiri &amp; tengah sudah tampil (keduanya
	 * dimuat oleh Timer 1 detik yang andal di {@link #jalankanLoadAwalElearning()}).
	 *
	 * <p>Metode ini memanggil {@link #eventListenerMateri} langsung dari jalur andal tersebut
	 * (dipicu Timer 1 detik / ON_OPEN region kiri), meniru pola versi mobile
	 * ({@code loadMobileJadwalUjianContent}). Aman dipanggil berulang (idempoten):
	 * {@code eventListenerMateri} hanya benar-benar memuat bila {@code menuKanan.isOpen()} dan
	 * {@link #centerMateri}/{@link #southKomentar} masih kosong, dan {@code loadMenuKanan()}
	 * memanggil {@code Common.clear(parent)} sebelum merender sehingga tak menggandakan isi.
	 *
	 * <p>DIBATASI hanya untuk tampilan portal desktop ({@code !mobileTampilan}); pada layout
	 * mobile ({@link #initRootLayout()}) panel kanan sudah punya pemicunya sendiri (via
	 * {@code loadMobileJadwalUjianContent}) sehingga tak perlu — dan tak boleh — diubah dari sini.
	 *
	 * @throws Exception bila {@code eventListenerMateri.onEvent} melempar (mis. saat menjadwalkan
	 *                   Timer pemuatan panel kanan); dibiarkan naik agar konsisten dg pemicu lain.
	 */
	private void picuKontenPanelKanan() throws Exception {
		if (mobileTampilan) {
			return;
		}
		if (eventListenerMateri != null && menuKanan != null && centerMateri != null
				&& centerMateri.getChildren().isEmpty()) {
			eventListenerMateri.onEvent(null);
		}
	}

	/**
	 * Apakah daftar Perkuliahan/Pelajaran pada panel kiri sudah BENAR-BENAR ter-render,
	 * bukan sekadar placeholder loading. Dipakai {@link #jalankanLoadAwalElearning()} untuk
	 * idempotensi berbasis konten (meniru panel kanan yang memuat ulang selama masih
	 * kosong): mengembalikan {@code true} bila tabpanel awal memiliki minimal satu komponen
	 * konten nyata (filter/daftar), dan {@code false} bila kosong atau hanya berisi
	 * indikator loading ({@link ais.ui.util.MyHtml}) — sehingga percobaan berikutnya
	 * (mis. dari event region ON_OPEN) diizinkan memuat ulang bila percobaan sebelumnya
	 * gagal menampilkan data.
	 *
	 * @return {@code true} jika konten daftar sudah tampil; {@code false} bila masih kosong/placeholder.
	 */
	private boolean kontenAwalElearningSudahTampil() {
		if (tabpanelAwalElearning == null) {
			return false;
		}
		for (Object anak : tabpanelAwalElearning.getChildren()) {
			if (!(anak instanceof ais.ui.util.MyHtml)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Apakah panel TENGAH ({@link #tabpanelTimeline} "Aktivitas Pembelajaran") siap diisi konten
	 * timeline sebenarnya — yaitu KOSONG atau HANYA berisi indikator loading awal
	 * ({@link #loadingAwalTengah}, sebuah {@link ais.ui.util.MyHtml}). Dipakai agar placeholder
	 * loading panel tengah TETAP terlihat sampai timeline benar-benar dirender (bukan dilepas dini
	 * saat compose), sehingga panel tengah menampilkan indikator loading sejak render pertama —
	 * meniru keandalan panel KANAN ("Tugas, Ujian, Materi &amp; Diskusi").
	 *
	 * @return {@code true} bila boleh membangun timeline (kosong / hanya placeholder); {@code false}
	 *         bila sudah berisi konten nyata (idempotensi build tetap terjaga).
	 */
	private boolean tabpanelTimelineSiapDiisi() {
		if (tabpanelTimeline == null) {
			return false;
		}
		return hanyaPlaceholderLoading(tabpanelTimeline);
	}

	/**
	 * Melepas indikator loading awal panel TENGAH ({@link #loadingAwalTengah}) bila masih terpasang.
	 * Idempoten &amp; defensif (dibungkus try/catch) — dipanggil tepat sebelum konten timeline
	 * di-append agar peralihan placeholder&rarr;konten mulus tanpa jeda kosong.
	 */
	private void bersihkanLoadingTengah() {
		if (loadingAwalTengah != null) {
			try {
				loadingAwalTengah.detach();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:3517");
				// abaikan: pelepasan indikator loading hanya kosmetik, jangan ganggu render konten.
			}
			loadingAwalTengah = null;
		}
	}

	/**
	 * Apakah komponen KOSONG atau HANYA berisi indikator loading awal (satu/lebih
	 * {@link ais.ui.util.MyHtml}), bukan konten nyata (form/daftar). Dipakai panel KIRI agar guard
	 * pemuatan tab (yang semula {@code getChildren().isEmpty()}) tetap memicu {@code loadMenu}
	 * meski placeholder loading masih terpasang — {@code loadMenu} sendiri melepas placeholder
	 * ({@code Common.clear(parent)}) tepat sebelum membangun form. Dengan begitu indikator loading
	 * panel kiri tetap tampak sampai daftar dirender (tidak ada kedip kosong), sekaligus menjaga
	 * idempotensi: bila konten nyata sudah ada, mengembalikan {@code false}.
	 *
	 * @param c komponen kontainer tab (boleh {@code null}).
	 * @return {@code true} bila kosong atau hanya placeholder; {@code false} bila ada konten nyata.
	 */
	private boolean hanyaPlaceholderLoading(Component c) {
		if (c == null) {
			return true;
		}
		java.util.List anak = c.getChildren();
		if (anak == null || anak.isEmpty()) {
			return true;
		}
		for (int i = 0; i < anak.size(); i++) {
			if (!(anak.get(i) instanceof ais.ui.util.MyHtml)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * <h3>Memicu perhitungan ulang tata letak (event 'resize') dari sisi klien</h3>
	 *
	 * <p>Region Borderlayout di dalam layout e-Learning kadang belum terukur (tinggi 0 / kolaps)
	 * sampai jendela browser di-resize manual — sehingga indikator loading maupun daftar data
	 * panel kiri "tidak muncul" pada akses pertama. Method ini mengirim event {@code 'resize'}
	 * ke window klien (dijeda 60 &amp; 300 ms) sehingga ZK menghitung ulang ukuran region tanpa
	 * pengguna perlu me-resize. Dipakai berulang (loadData saat progress bar dipasang &amp; saat
	 * data selesai) — idempoten, hanya memerintahkan reflow. Fallback {@code createEvent} untuk
	 * browser lama; seluruhnya dibungkus try/catch agar koreksi tampilan ini tak pernah
	 * mengganggu proses utama.</p>
	 */
	private void picuResizeTataLetakElearning() {
		try {
			Clients.evalJavaScript(
					"try{var _r=function(){try{var e;if(typeof(Event)==='function'){e=new Event('resize');}"
							+ "else{e=document.createEvent('Event');e.initEvent('resize',true,true);}"
							+ "window.dispatchEvent(e);}catch(x){}};setTimeout(_r,60);setTimeout(_r,300);}catch(x){}");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:3570");
		}
	}

	private Comboitem tambahPilihanMenuElearning(Combobox combo, String label, String icon, Tab tab,
			EventListener listener) {
		Comboitem item = new Comboitem(label);
		item.setImage(icon);
		item.setValue(tab);
		item.setAttribute("listener", listener);
		item.setParent(combo);
		return item;
	}

	private void pilihMenuElearning(Combobox combo, Comboitem item) throws Exception {
		if (combo == null || item == null || !item.isVisible()) {
			return;
		}
		combo.setSelectedItem(item);
		Object tab = item.getValue();
		if (tab instanceof Tab) {
			((Tab) tab).setSelected(true);
		}
		Object listener = item.getAttribute("listener");
		if (listener instanceof EventListener) {
			((EventListener) listener).onEvent(null);
		}
	}

	private void loadMenuKanan(Component parent, final Textbox cari, final Rows rows, final Paging paging,
			final Integer jenis, final boolean refresh) {
		/* Panel tujuan bisa sudah detach (user berpindah halaman/menutup tab
		 * saat timer masih berjalan); setParent pada induk tanpa page melempar
		 * NPE di addMoved. Tidak ada lagi yang perlu dirender. */
		if (parent == null || parent.getDesktop() == null || parent.getPage() == null) {
			return;
		}
		Common.clear(parent);
		int jumlahDataDalamSatuHalamanElearning = 25;
		Borderlayout subBorderlayout = new Borderlayout();
		subBorderlayout.setParent(parent);

		Center subcenter = new Center();
		subcenter.setParent(subBorderlayout);
		ais.ui.util.ZkCompat.setFlex(subcenter, true);
		subcenter.setBorder("none");

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setParent(subcenter);

		grid.appendChild(rows);

		final Label label1 = new Label(ais.common.Common.getBahasaConfig("Memuat tugas, ujian & materi..."));

		paging.setHeight("30px");
		Common.initPagingCustom(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataKanan(cari, rows, paging, false, jenis, label1);
			}
		}, jumlahDataDalamSatuHalamanElearning);

		if (rows != null) {

			Common.clear(rows);

		}
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setStyle("border:0px;background: transparent;font-size: x-small;");
		row.setParent(rows);
		Vbox vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");

		vbox.appendChild(label1);
		vbox.appendChild(new ais.ui.util.MyHtml("<div style='margin:6px auto;width:34px;height:34px;border:3px solid #e2e8f0;border-top-color:#2563eb;border-radius:50%;animation:aisSpin .8s linear infinite;'></div>"));

		Common.createDefaultTimerNoBusy(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataKanan(cari, rows, paging, refresh, jenis, label1);
			}
		});

	}

	/**
	 * FASE 2 &mdash; lazy panel kanan TANPA menyembunyikan data. Menyaring peta pertemuan sejendela ke HANYA
	 * pertemuan yang BENAR-BENAR punya konten e-Learning (materi/audio/video/tugas/tugas kelompok/ujian) atau
	 * judul tugas langsung. Pertemuan kosong tak menghasilkan baris apa pun di panel &rarr; hasil TAMPIL
	 * IDENTIK, namun fan-out {@code ambilMateri} (~230 thread &times; 6 koleksi anak/pertemuan) jauh lebih
	 * kecil. Semua kolom {@code pertemuan} pada tabel anak sudah ter-index (EXISTS = index-only lookup).
	 * Query bersifat LIVE sehingga konten yang BARU ditambah pasti ikut. Bila query GAGAL, kembalikan set
	 * PENUH agar TIDAK ADA data yang tersembunyi (safety net atas pelajaran regresi windowing-by-date dulu).
	 */
	private static TreeMap<String, Long> saringPertemuanBerkonten(TreeMap<String, Long> pertemuansa) {
		if (pertemuansa == null || pertemuansa.isEmpty()) {
			return pertemuansa;
		}
		// Kumpulkan id unik jadi klausa IN literal (aman dari injeksi karena bertipe Long).
		StringBuilder inClause = new StringBuilder();
		for (Long id : pertemuansa.values()) {
			if (id == null)
				continue;
			if (inClause.length() > 0)
				inClause.append(',');
			inClause.append(id.longValue());
		}
		if (inClause.length() == 0) {
			return pertemuansa;
		}
		java.util.HashSet<Long> berkonten = new java.util.HashSet<Long>();

		// (1) DB UTAMA: judultugas (kolom pertemuan) + tugas_pertemuan + tugas_kelompok + pertemuan_punya_ujian.
		//     openSession() + closeSessionQuietly di finally (clear/disconnect/close). Aman di event-thread ZK
		//     maupun background thread; tidak menyentuh session ThreadLocal request.
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			String sqlUtama = "SELECT p.id FROM pertemuan p WHERE p.id IN (" + inClause + ") AND ("
					+ " (p.judultugas IS NOT NULL AND btrim(p.judultugas) <> '')"
					+ " OR EXISTS (SELECT 1 FROM tugas_pertemuan t WHERE t.pertemuan = p.id)"
					+ " OR EXISTS (SELECT 1 FROM tugas_kelompok tk WHERE tk.pertemuan = p.id)"
					+ " OR EXISTS (SELECT 1 FROM pertemuan_punya_ujian u WHERE u.pertemuan = p.id))";
			List<?> hasilUtama = session.createSQLQuery(sqlUtama).list();
			for (Object r : hasilUtama) {
				if (r instanceof Number) {
					berkonten.add(((Number) r).longValue());
				}
			}
		} catch (Exception ex) {
			// SAFETY NET: apa pun yang gagal -> pakai set PENUH, JANGAN pernah menyembunyikan konten.
			ais.common.Common.tampilErrorJikaAdmin(ex);
			return pertemuansa;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}

		// (2) DB STREAMING: audio_pertemuan / video_pertemuan / pertemuan_file_content adalah tabel FILE/BLOB
		//     yang berada di DATABASE STREAMING (${url_streaming}), BUKAN di DB utama — makanya query gabungan
		//     lama gagal "relation audio_pertemuan does not exist" (join lintas-DB tak mungkin). Query tabel
		//     FILE WAJIB lewat StreamingHibernateUtil. openSession() dari factory streaming, ditutup di finally
		//     via closeSessionQuietly (clear/disconnect/close). Bila DB streaming gagal -> set PENUH (safety net).
		Session sessionStreaming = null;
		try {
			sessionStreaming = ais.database.hibernate.StreamingHibernateUtil.getInstance().openSession();
			String sqlStreaming = "SELECT DISTINCT x.pertemuan FROM ("
					+ " SELECT pertemuan FROM audio_pertemuan WHERE pertemuan IN (" + inClause + ")"
					+ " UNION SELECT pertemuan FROM video_pertemuan WHERE pertemuan IN (" + inClause + ")"
					+ " UNION SELECT pertemuan FROM pertemuan_file_content WHERE pertemuan IN (" + inClause + ")"
					+ " ) x WHERE x.pertemuan IS NOT NULL";
			List<?> hasilStreaming = sessionStreaming.createSQLQuery(sqlStreaming).list();
			for (Object r : hasilStreaming) {
				if (r instanceof Number) {
					berkonten.add(((Number) r).longValue());
				}
			}
		} catch (Exception ex) {
			ais.common.Common.tampilErrorJikaAdmin(ex);
			return pertemuansa;
		} finally {
			HibernateUtil.closeSessionQuietly(sessionStreaming);
		}

		// Susun ulang peta HANYA berisi pertemuan berkonten (gabungan DB utama + streaming); pertahankan
		// kunci & urutan aslinya.
		TreeMap<String, Long> hasil = new TreeMap<String, Long>(pertemuansa.comparator());
		for (java.util.Map.Entry<String, Long> e : pertemuansa.entrySet()) {
			if (e.getValue() != null && berkonten.contains(e.getValue().longValue())) {
				hasil.put(e.getKey(), e.getValue());
			}
		}
		return hasil;
	}

	/**
	 * FASE 3 &mdash; kalkulasi MURNI DATA panel kanan (saring pertemuan berkonten + fan-out {@code ambilMateri}).
	 * TIDAK menyentuh komponen ZK sama sekali (label tak dipakai di dalam ambilMateri, tak ada akses Executions/
	 * current-user &mdash; worker DB kelola sesi Hibernate-nya sendiri), sehingga AMAN dijalankan di background
	 * thread. Dipakai oleh background thread Fase 3 dan juga jalur fallback sinkron.
	 */
	private static TreeMap<String, Object[]> hitungDataMateri(Tbmuser tbmuser, TreeMap<String, Long> pertemuansa,
			boolean refresh, Label label, boolean urutBerdasarkanNama) {
		Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		TreeMap<String, Long> pertemuanKonten = saringPertemuanBerkonten(pertemuansa);
		if (mahasiswa != null) {
			return mahasiswa.ambilMateri(pertemuanKonten, refresh, label, urutBerdasarkanNama, tbmuser);
		} else if (dosen != null) {
			return dosen.ambilMateri(pertemuanKonten, refresh, label, urutBerdasarkanNama, tbmuser);
		}
		return PertemuanFileContent.ambilMateri(pertemuanKonten, refresh, label, urutBerdasarkanNama, tbmuser);
	}

	private DashboardTimelinePertemuan dashboardTimelinePertemuan = null;
	private TreeMap<String, Object[]> dataMateriKomentar = null;
	private Collection<Object[]> dataMateriValuesKomentar = null;

	private MyCheckboxConfig dosenPil = null;
	private MyCheckboxConfig mahasiswaPil = null;
	private MyCheckboxConfig guruPil = null;
	private MyCheckboxConfig siswaPil = null;
	private MyCheckboxConfig adminPil = null;
	private MyCheckboxConfig materiPil;
	private MyCheckboxConfig ujianPil;
	private MyCheckboxConfig tugasPil;

	private void loadDataKanan(Textbox cari, Rows rows, Paging paging, boolean refresh, Integer jenis, Label label) {
		loadDataKanan(cari, rows, paging, refresh, jenis, materiPil == null ? true : materiPil.isChecked(),
				ujianPil == null ? true : ujianPil.isChecked(), tugasPil == null ? true : tugasPil.isChecked(),
				dosenPil == null ? true : dosenPil.isChecked(), mahasiswaPil == null ? true : mahasiswaPil.isChecked(),
				guruPil == null ? true : guruPil.isChecked(), siswaPil == null ? true : siswaPil.isChecked(),
				adminPil == null ? true : adminPil.isChecked(), label);
	}

	private String cariSebelumnya = "";
	private int mulaiParam;
	private int sampaiParam;

	/**
	 * FIX WrongValueException "Unable to set active page to N since only M pages": halaman
	 * hasil hitung (index / jumlahPerHalaman) bisa melebihi jumlah halaman Paging yang
	 * sebenarnya (mis. data berkurang sejak grid terakhir dimuat). Jepit ke rentang valid
	 * [0, getPageCount()-1] sekali, bukan tebak-tebak "halaman - 1" via try-catch berlapis.
	 */
	private static void setActivePageAman(Paging paging, int halaman) {
		if (paging == null) {
			return;
		}
		try {
			int maxHalaman = paging.getPageCount() - 1;
			if (halaman < 0) {
				halaman = 0;
			} else if (halaman > maxHalaman) {
				halaman = maxHalaman;
			}
			if (halaman >= 0) {
				paging.setActivePage(halaman);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:setActivePageAman");
		}
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	public static void loadDataMateri(final Textbox cari, final Rows rows, final Paging paging, final boolean refresh,
			final MyCheckboxConfig materiPil, final MyCheckboxConfig ujianPil, final MyCheckboxConfig tugasPil,
			final Label label, final Tbmuser tbmuser, final TreeMap<String, Long> pertemuansa, final boolean edit,
			final boolean urutBerdasarkanNama) {

		final int jumlahDataDalamSatuHalamanElearning = 25;
		paging.setPageSize(jumlahDataDalamSatuHalamanElearning);
		// awal (loncat ke halaman "hari ini") dibawa via atribut supaya render pass hasil background (re-invoke
		// refresh=false) tetap memposisikan halaman dengan benar.
		boolean awal = Boolean.TRUE.equals(paging.getAttribute("dataMateriAwalFlag"));
		if (awal) {
			paging.setAttribute("dataMateriAwalFlag", null); // konsumsi sekali
		}
		TreeMap<String, Object[]> dataMateri = (TreeMap<String, Object[]>) paging.getAttribute("dataMateri");
		Collection<Object[]> dataMateriValues = (Collection<Object[]>) paging.getAttribute("dataMateriValues");
		if (refresh || dataMateri == null) {
			// Cegah dua background berjalan sekaligus (mis. callback timeline + reload checkbox saat bg jalan).
			if (Boolean.TRUE.equals(paging.getAttribute("dataMateriBgRunning"))) {
				return; // bg yang sedang jalan akan me-render saat siap
			}

			boolean adaKerja = pertemuansa != null && !pertemuansa.isEmpty();
			if (adaKerja) {
				// ===== FASE 3 (backend streaming): kalkulasi BERAT (Fase 2 saring pertemuan berkonten +
				// fan-out ambilMateri) dipindah ke BACKGROUND THREAD; render menyusul via poll timer saat
				// siap. Event-thread TIDAK lagi tertahan menunggu await fan-out → akses pertama INSTAN
				// (kerangka + checkbox filter langsung tampil), detail "mengalir" belakangan. ambilMateri
				// murni data (tak sentuh komponen ZK) → aman di thread latar. Fallback sinkron bila bg
				// gagal/timeout agar data TETAP tampil. =====
				paging.setAttribute("dataMateriBgRunning", Boolean.TRUE);

				// Indikator "memuat konten" ringan (di bawah baris checkbox yang sudah dipasang loadDataKanan).
				final MyFormRow rowMuat = new MyFormRow();
				rowMuat.setValign("top");
				rowMuat.setStyle("border:0px;background:transparent;font-size:x-small;");
				rowMuat.setParent(rows);
				Vbox vboxMuat = new Vbox();
				vboxMuat.setParent(rowMuat);
				vboxMuat.setWidth("100%");
				vboxMuat.appendChild(new Label(ais.common.Common.getBahasaConfig("Memuat konten...")));
				vboxMuat.appendChild(new ais.ui.util.MyHtml(
						"<div style='margin:6px auto;width:28px;height:28px;border:3px solid #e2e8f0;border-top-color:#2563eb;border-radius:50%;animation:aisSpin .8s linear infinite;'></div>"));

				final TreeMap<String, Object[]>[] holder = new TreeMap[] { null };
				final boolean[] siap = { false };
				final Tbmuser tbmuserF = tbmuser;
				final TreeMap<String, Long> pertemuansaF = pertemuansa;
				final boolean refreshF = refresh;
				final Label labelF = label;
				final boolean urutF = urutBerdasarkanNama;

				Thread bg = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							holder[0] = hitungDataMateri(tbmuserF, pertemuansaF, refreshF, labelF, urutF);
						} catch (Throwable t) {
							holder[0] = null; // fallback ditangani poll (hitung sinkron)
						} finally {
							siap[0] = true;
						}
					}
				}, "elearning-materi-bg");
				bg.setDaemon(true);
				bg.start();

				final Timer timerMateri = new Timer(400);
				timerMateri.setRepeats(true);
				timerMateri.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				final int[] tick = { 0 };
				timerMateri.addEventListener("onTimer", new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						tick[0]++;
						if (!siap[0] && tick[0] < 90) {
							return; // tunggu maksimum ~36 dtk (90 x 400ms) sebelum fallback
						}
						timerMateri.stop();
						timerMateri.detach();

						TreeMap<String, Object[]> hasil = holder[0];
						if (hasil == null) {
							// FALLBACK: bg gagal/timeout → hitung SINKRON (jarang) agar data TETAP tampil.
							hasil = hitungDataMateri(tbmuserF, pertemuansaF, refreshF, labelF, urutF);
						}

						paging.setAttribute("dataMateri", hasil);
						paging.setAttribute("dataMateriValues", hasil.values());
						paging.setAttribute("dataMateriBgRunning", null);

						try {
							rowMuat.detach();
						} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:3886");
						}
						// Bawa kembali makna refresh (reset posisi) & awal (loncat "hari ini") untuk render pass.
						if (refreshF) {
							paging.setAttribute("mulaiParam", null);
							paging.setAttribute("sampaiParam", null);
						}
						paging.setAttribute("dataMateriAwalFlag", Boolean.TRUE);
						// Re-invoke: refresh=false → lewati compute → langsung render data yang sudah tersimpan.
						loadDataMateri(cari, rows, paging, false, materiPil, ujianPil, tugasPil, label, tbmuser,
								pertemuansa, edit, urutBerdasarkanNama);
					}
				});
				timerMateri.start();
				return; // event-thread lepas; detail menyusul saat siap
			}

			// ===== pertemuansa kosong/null (mis. fallback timeline belum siap): kerja trivial → tetap SINKRON
			// (instan, tak perlu thread latar). =====
			dataMateri = hitungDataMateri(tbmuser, pertemuansa, refresh, label, urutBerdasarkanNama);
			awal = true;
			paging.setAttribute("dataMateri", dataMateri);
			dataMateriValues = dataMateri.values();
			paging.setAttribute("dataMateriValues", dataMateriValues);
		}

		if (dataMateriValues != null) {

			List<Object[]> pertemuanFileContents = new ArrayList<Object[]>();
			for (Object[] objects : dataMateriValues) {

				if (objects[0] instanceof PertemuanFileContent && materiPil.isChecked()) {
					PertemuanFileContent pertemuanFileContent = (PertemuanFileContent) objects[0];

					String n = pertemuanFileContent.getNama() != null
							&& pertemuanFileContent.getNama().trim().equalsIgnoreCase("link")
									? pertemuanFileContent.getLink()
									: pertemuanFileContent.getNama();

					if (!pertemuanFileContent.getGoogleBook().isEmpty()) {
						n = pertemuanFileContent.getNama();
					}
					if (n == null)
						n = "";
					if (cari.getValue().trim().isEmpty()
							|| n.trim().toLowerCase().contains(cari.getValue().trim().toLowerCase())) {
						pertemuanFileContents.add(objects);
					}
				} else if (objects[0] instanceof Tugas && tugasPil.isChecked()) {
					Tugas tugas = (Tugas) objects[0];

					String n = tugas.getJudultugas();
					if (n == null)
						n = "";
					if (cari.getValue().trim().isEmpty()
							|| n.trim().toLowerCase().contains(cari.getValue().trim().toLowerCase())) {
						pertemuanFileContents.add(objects);
					}
				} else if (objects[0] instanceof PertemuanPunyaUjian && ujianPil.isChecked()) {
					PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) objects[0];

					String n = pertemuanPunyaUjian.getNama();
					if (n == null)
						n = "";
					if (pertemuanPunyaUjian.getUjian() != null && pertemuanPunyaUjian.getUjian().getAktif()
							&& (cari.getValue().trim().isEmpty()
									|| n.trim().toLowerCase().contains(cari.getValue().trim().toLowerCase()))) {
						pertemuanFileContents.add(objects);
					}
				} else if (objects[0] instanceof AudioPertemuan && materiPil.isChecked()) {
					AudioPertemuan audioPertemuan = (AudioPertemuan) objects[0];

					String n = audioPertemuan == null ? ""
							: (audioPertemuan.getLink() == null || audioPertemuan.getLink().isEmpty()
									? audioPertemuan.getNama()
									: audioPertemuan.getLink());
					if (n == null)
						n = "";
					String isi = audioPertemuan.getKeteranganTambahan();

					if (cari.getValue().trim().isEmpty()
							|| n.trim().toLowerCase().contains(cari.getValue().trim().toLowerCase())
							|| isi.trim().toLowerCase().contains(cari.getValue().trim().toLowerCase())) {
						pertemuanFileContents.add(objects);
					}
				} else if (objects[0] instanceof VideoPertemuan && materiPil.isChecked()) {
					VideoPertemuan videoPertemuan = (VideoPertemuan) objects[0];

					String n = videoPertemuan == null ? ""
							: (videoPertemuan.getLink() == null || videoPertemuan.getLink().isEmpty()
									? videoPertemuan.getNama()
									: videoPertemuan.getLink());
					if (n == null)
						n = "";
					String isi = videoPertemuan.getKeteranganTambahan();

					if (cari.getValue().trim().isEmpty()
							|| n.trim().toLowerCase().contains(cari.getValue().trim().toLowerCase())
							|| isi.trim().toLowerCase().contains(cari.getValue().trim().toLowerCase())) {
						pertemuanFileContents.add(objects);
					}
				}
			}

			int size = pertemuanFileContents.size();
			paging.setTotalSize(size);
			paging.setVisible(size > jumlahDataDalamSatuHalamanElearning);

			if (awal) {
				int index = 0;
				boolean ada = false;
				Date tgl = WaktuUtil.getDate();
				for (Object[] objects : dataMateriValues) {
					Date tglKey = (Date) objects[2];
					if (Common.dateFormat8.get().format(tglKey).equals(Common.dateFormat8.get().format(tgl))
							|| tglKey.after(tgl)) {
						setActivePageAman(paging, index / jumlahDataDalamSatuHalamanElearning);
						ada = true;
						break;
					}
					index++;
				}

				if (!ada) {
					setActivePageAman(paging, index / jumlahDataDalamSatuHalamanElearning);
				}
			}

			Toolbarbutton back = new MyToolbarbuttonConfig("Data sebelumnya.. ", "/img/Go-back-icon.png");

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			rows.appendChild(row);

			if (edit) {
				ais.ui.util.ZkCompat.setSpans(row, "2");
			}

			if (refresh) {
				paging.setAttribute("mulaiParam", null);
				paging.setAttribute("sampaiParam", null);
			}

			int index = 0;
			int mulaiParamMateriTemp = paging.getAttribute("mulaiParam") != null
					? ((Number) paging.getAttribute("mulaiParam")).intValue()
					: jumlahDataDalamSatuHalamanElearning * (paging == null ? 0 : paging.getActivePage());

			if ((mulaiParamMateriTemp + jumlahDataDalamSatuHalamanElearning) > pertemuanFileContents.size()) {
				int mulaiParamMateriBaru = pertemuanFileContents.size() - jumlahDataDalamSatuHalamanElearning;
				if (mulaiParamMateriBaru < 0)
					mulaiParamMateriTemp = 0;
				paging.setAttribute("mulaiParam", mulaiParamMateriTemp);
			}

			final int mulaiParamMateri = mulaiParamMateriTemp;

			final int sampaiParamMateri = paging.getAttribute("sampaiParam") != null
					? ((Number) paging.getAttribute("sampaiParam")).intValue()
					: jumlahDataDalamSatuHalamanElearning;

			back.setStyle(
					"font-size:12px; font-weight: 500; color: #2563eb; background: #eff6ff; border-radius: 6px; padding: 4px 10px; margin-bottom: 10px;");
			row.appendChild(back);
			back.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					int mulaiParamMateriBaru = mulaiParamMateri - jumlahDataDalamSatuHalamanElearning;
					int sampaiParamMateriBaru = sampaiParamMateri + jumlahDataDalamSatuHalamanElearning;
					paging.setAttribute("mulaiParam", mulaiParamMateriBaru);
					paging.setAttribute("sampaiParam", sampaiParamMateriBaru);

					loadDataMateri(cari, rows, paging, false, materiPil, ujianPil, tugasPil, label, tbmuser,
							pertemuansa, edit, urutBerdasarkanNama);
				}
			});

			back.getParent().setVisible(mulaiParamMateri > 0);

			int jmlMateri = 0;
			for (Object[] objects : pertemuanFileContents) {
				if (index < mulaiParamMateri) {
					jmlMateri++;
					back.setLabel("Data sebelumnya.. (" + jmlMateri + " data)");
				}
				if (index >= mulaiParamMateri && index < (mulaiParamMateri + sampaiParamMateri)) {
					row = new MyFormRow();
					row.setStyle("border:0px; background: transparent;");
					row.setParent(rows);
					final Pertemuan pertemuan = (Pertemuan) objects[1];

					if (objects[0] instanceof PertemuanFileContent) {
						final PertemuanFileContent pertemuanFileContent = (PertemuanFileContent) objects[0];

						String n = pertemuanFileContent.getNama() != null
								&& pertemuanFileContent.getNama().trim().equalsIgnoreCase("link")
										? pertemuanFileContent.getLink()
										: pertemuanFileContent.getNama();

						if (!pertemuanFileContent.getGoogleBook().isEmpty())
							n = pertemuanFileContent.getNama();
						if (n == null)
							n = "";

						String isi = pertemuanFileContent.getKeterangan();
						if (isi != null && !isi.trim().isEmpty())
							n = isi;

						Groupbox vbox = new ais.ui.util.MyGroupboxStyled();
						vbox.setWidth("95%");
						vbox.setStyle(
								"background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; margin: 6px 0px 12px 10px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05); transition: transform 0.2s;");
						vbox.setParent(row);

						String icon = pertemuanFileContent.getLokasiFisik() != null ? "/img/svg/desktop-light.svg"
								: MyMenuitem.svgIcon(pertemuanFileContent.getNama(),
										FileFoto.icon(pertemuanFileContent.getNama()));

						Toolbarbutton downloadButton = new ais.ui.util.MyToolbarbuttonConfig(
								n.length() > 150 ? n.substring(0, 150) + "..." : n,
								!pertemuanFileContent.getGoogleBook().isEmpty() ? "/img/Apps-Google-Play-Books-icon.png"
										: icon);
						downloadButton.setTooltiptext("Download \"" + pertemuanFileContent.getNama() + "\"");
						downloadButton.setAttribute("janganDisabled", true);
						downloadButton.setStyle(
								"font-size: 14px; font-weight: 600; color: #1e40af; text-decoration: none; padding-bottom: 8px; display: block; line-height: 1.4;");
						vbox.appendChild(downloadButton);

						TampilanELearningAction.dilihat(pertemuan, "bahan_perkulaiahan_" + pertemuanFileContent.getId(),
								"Akses", false).setParent(vbox);

						EventListener eventListener = new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (ProfileUtil.chekSyarat(pertemuan.ambilVOPembelajaran(),
										pertemuanFileContent.getSyaratAkses()))
									return;
								pertemuan.masukkanData("bahan_perkulaiahan_" + pertemuanFileContent.getId());

								if (!pertemuanFileContent.getGoogleBook().isEmpty()) {
									if (Common.isMobile())
										ExecutionsCtrl.getCurrent().sendRedirect(pertemuanFileContent.getLink(),
												"_blank");
									else
										Clients.evalJavaScript("popupCenter({url: '" + pertemuanFileContent.getLink()
												+ "', title: 'Book', w: 1200, h: 600});");
								} else if (pertemuanFileContent.getGdrive() != null) {
									pertemuanFileContent.tampilGDrive(null);
								} else {
									String link = pertemuanFileContent == null ? null
											: (pertemuanFileContent.getLink() == null
													|| pertemuanFileContent.getLink().isEmpty() ? null
															: pertemuanFileContent.getLink());
									if (pertemuanFileContent != null
											&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
										link = pertemuanFileContent.createLinkUri();
									}
									if (pertemuanFileContent != null && link != null && !link.trim().isEmpty()) {
										if (pertemuanFileContent.bisaPreview())
											Common.displayWindow(pertemuanFileContent.merupakanGambar(), link, true,
													"95%", "95%", true, pertemuanFileContent);
										else {
											if (Common.isMobile())
												ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
											else
												Clients.evalJavaScript("popupCenter({url: '" + link
														+ "', title: 'data', w: 1200, h: 600});");
										}
									} else {
										MyMessageboxConfig.show("Mohon maaf, berkas yang Bapak/Ibu akses saat ini tidak dapat ditemukan pada sistem. Langkah yang dapat dilakukan: (1) mohon menunggu beberapa saat dan mencoba kembali karena berkas kemungkinan masih dalam proses pengunggahan; (2) mohon memastikan koneksi internet Bapak/Ibu dalam keadaan stabil; (3) apabila berkas tetap tidak dapat dibuka, mohon menghubungi pengajar atau administrator sistem agar berkas tersebut dapat diunggah ulang.", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}
								}
							}
						};
						downloadButton.addEventListener("onClick", eventListener);

						A a;
						vbox.appendChild(a = new A(
								"Materi pertemuan ke: " + pertemuan.getPertemuanKe() + ", " + pertemuan.info() + ", "
										+ ((pertemuan.getTanggal() == null ? "-"
												: (SmartDateTimeUtil.getDayString(pertemuan.getTanggal(),
														pertemuan.getWaktuMulai())
														+ Common.dateFormat6.get().format(pertemuan.getTanggal()))
														+ " "
														+ (pertemuan.getWaktuMulai() == null
																&& pertemuan.getWaktuSelesai() == null ? ""
																		: pertemuan.getWaktuMulai() + "-"
																				+ pertemuan.getWaktuSelesai())))));
						a.setStyle(
								"font-size: 11px; color: #475569; background-color: #f1f5f9; padding: 4px 10px; border-radius: 12px; display: inline-block; margin-top: 10px; text-decoration: none; border: 1px solid #cbd5e1;");

						a.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								new PertemuanHelper(tbmuser == null ? null : tbmuser.getMahasiswa(),
										tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
										.display(pertemuan, new DataLoader() {
											@Override
											public void loadData(Object value) {
											}
										}, 2, pertemuanFileContent);
							}
						});

						if (edit) {
							MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Lihat", "/img/svg/eye.svg");
							button.setStyle(
									"background-color: #eff6ff; color: #1d4ed8; padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: 600; text-decoration: none; margin-left: 10px; border: 1px solid #bfdbfe;");
							button.setParent(row);
							button.addEventListener("onClick", eventListener);
						}

					} else if (objects[0] instanceof Tugas) {

						final Tugas tugas = (Tugas) objects[0];
						if (tugas.getJudultugas() != null && !tugas.getJudultugas().trim().isEmpty()) {
							String n = tugas.getJudultugas();
							if (n == null)
								n = "";
							Groupbox vbox = new ais.ui.util.MyGroupboxStyled();
							vbox.setWidth("95%");
							vbox.setStyle(
									"background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; margin: 6px 0px 12px 10px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);");
							vbox.setParent(row);

							String icon = "/img/svg/list-task.svg";
							if (tugas instanceof TugasKelompok)
								icon = "/img/svg/user-group.svg";

							Toolbarbutton downloadButton = new ais.ui.util.MyToolbarbuttonConfig(
									n.length() > 150 ? n.substring(0, 150) + "..." : n, icon);
							downloadButton.setAttribute("janganDisabled", true);
							downloadButton.setStyle(
									"font-size: 14px; font-weight: 600; color: #1e40af; text-decoration: none; padding-bottom: 8px; display: block;");
							vbox.appendChild(downloadButton);
							downloadButton.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									if (tugas instanceof Pertemuan) {
										new PertemuanHelper(tbmuser == null ? null : tbmuser.getMahasiswa(),
												tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
												.display(pertemuan, new DataLoader() {
													@Override
													public void loadData(Object value) {
													}
												}, 3);
									} else if (tugas instanceof TugasKelompok) {
										TugasKelompok tugasKelompok = (TugasKelompok) tugas;
										new PertemuanHelper(tbmuser == null ? null : tbmuser.getMahasiswa(),
												tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
												.display(pertemuan, new DataLoader() {
													@Override
													public void loadData(Object value) {
													}
												}, 3, null, tugasKelompok, null, null, null);
									} else {
										TugasPertemuan tugasPertemuan = (TugasPertemuan) tugas;
										new PertemuanHelper(tbmuser == null ? null : tbmuser.getMahasiswa(),
												tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
												.display(pertemuan, new DataLoader() {
													@Override
													public void loadData(Object value) {
													}
												}, 3, tugasPertemuan, null, null, null, null);
									}
								}
							});

							Date tgl = tugas.getMulai() == null ? pertemuan.getTanggal() : tugas.getMulai();
							Hbox myHbox = new Hbox();
							myHbox.setAlign("center");
							myHbox.setParent(vbox);

							if (!(tugas instanceof TugasKelompok) && tugas.getJudultugas() != null
									&& !tugas.getJudultugas().trim().equals("")) {
								Number tg = tugas.ambilJumlahTugasFileContent();
								MyLabelKecil labelKecil = new MyLabelKecil(
										"Upload : " + Common.numberFormat.get().format(tg.intValue()) + " peserta");
								labelKecil.setStyle(
										"font-size: 10px; font-weight: 700; color: #ffffff; background-color: #3b82f6; padding: 3px 8px; border-radius: 12px; margin-right: 10px; box-shadow: 0 2px 4px rgba(59,130,246,0.3);");
								myHbox.appendChild(labelKecil);
							}
							TampilanELearningAction.dilihat(tugas, "tugas", "Akses", false).setParent(myHbox);

							A a;
							vbox.appendChild(a = new A("Tugas pertemuan ke: " + pertemuan.getPertemuanKe() + ", "
									+ pertemuan.info() + ", " + (SmartDateTimeUtil.getDayString(tgl, null)
											+ Common.dateFormat51.get().format(tgl))));
							a.setStyle(
									"font-size: 11px; color: #475569; background-color: #f1f5f9; padding: 4px 10px; border-radius: 12px; display: inline-block; margin-top: 10px; text-decoration: none; border: 1px solid #cbd5e1;");

							EventListener eventListener = new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									if (tugas instanceof Pertemuan) {
										new PertemuanHelper(tbmuser == null ? null : tbmuser.getMahasiswa(),
												tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
												.display(pertemuan, new DataLoader() {
													@Override
													public void loadData(Object value) {
													}
												}, 3);
									} else if (tugas instanceof TugasKelompok) {
										TugasKelompok tugasKelompok = (TugasKelompok) tugas;
										new PertemuanHelper(tbmuser == null ? null : tbmuser.getMahasiswa(),
												tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
												.display(pertemuan, new DataLoader() {
													@Override
													public void loadData(Object value) {
													}
												}, 3, null, tugasKelompok, null, null, null);
									} else {
										TugasPertemuan tugasPertemuan = (TugasPertemuan) tugas;
										new PertemuanHelper(tbmuser == null ? null : tbmuser.getMahasiswa(),
												tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
												.display(pertemuan, new DataLoader() {
													@Override
													public void loadData(Object value) {
													}
												}, 3, tugasPertemuan, null, null, null, null);
									}
								}
							};
							a.addEventListener("onClick", eventListener);

							if (edit) {
								MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Lihat", "/img/svg/eye.svg");
								button.setStyle(
										"background-color: #eff6ff; color: #1d4ed8; padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: 600; text-decoration: none; margin-left: 10px; border: 1px solid #bfdbfe;");
								button.setParent(row);
								button.addEventListener("onClick", eventListener);
							}
						}

					} else if (objects[0] instanceof PertemuanPunyaUjian) {
						PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) objects[0];

						String n = pertemuanPunyaUjian.getNama();
						if (n == null)
							n = "";
						Groupbox vbox = new ais.ui.util.MyGroupboxStyled();
						vbox.setWidth("95%");
						vbox.setStyle(
								"background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; margin: 6px 0px 12px 10px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);");
						vbox.setParent(row);

						Toolbarbutton downloadButton = new ais.ui.util.MyToolbarbuttonConfig(
								n.length() > 150 ? n.substring(0, 150) + "..." : n, "/img/svg/card-checklist.svg");
						downloadButton.setAttribute("janganDisabled", true);
						downloadButton.setStyle(
								"font-size: 14px; font-weight: 600; color: #1e40af; text-decoration: none; padding-bottom: 8px; display: block;");
						vbox.appendChild(downloadButton);

						Hbox myHbox = new Hbox();
						myHbox.setAlign("center");
						myHbox.setParent(vbox);
						Number tg = pertemuanPunyaUjian.ambilJumlahHasilUjianMahasiswaTelahIkut(false);
						MyLabelKecil labelKecil = new MyLabelKecil(
								"Ikut Ujian : " + Common.numberFormat.get().format(tg.intValue()) + " peserta");
						labelKecil.setStyle(
								"font-size: 10px; font-weight: 700; color: #ffffff; background-color: #ef4444; padding: 3px 8px; border-radius: 12px; margin-right: 10px; box-shadow: 0 2px 4px rgba(239,68,68,0.3);");

						myHbox.appendChild(labelKecil);
						TampilanELearningAction
								.dilihat(pertemuan, "ujian_" + pertemuanPunyaUjian.getId(), "Akses", false)
								.setParent(myHbox);

						Date tgl = pertemuanPunyaUjian.getMulaiUjian();
						A a;
						vbox.appendChild(a = new A(
								"Ujian pertemuan ke: " + pertemuan.getPertemuanKe() + ", " + pertemuan.info() + ", "
										+ (tgl == null ? ""
												: (SmartDateTimeUtil.getDayString(tgl, null)
														+ Common.dateFormat51.get().format(tgl)))));
						a.setStyle(
								"font-size: 11px; color: #475569; background-color: #f1f5f9; padding: 4px 10px; border-radius: 12px; display: inline-block; margin-top: 10px; text-decoration: none; border: 1px solid #cbd5e1;");

						List<EventListener> eventListeners = new ArrayList<EventListener>();
						if (edit) {
							HasilUjianMahasiswa hasilUjianMahasiswa = HasilUjianMahasiswa.ambilByKey(
									pertemuanPunyaUjian, tbmuser == null ? null : tbmuser.getMahasiswa(),
									tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa(), null, null);
							if (hasilUjianMahasiswa != null) {
								if (pertemuanPunyaUjian.getUjian() == null && pertemuanPunyaUjian.getId() != null) {
									HibernateUtil.currentSession().refresh(pertemuanPunyaUjian);
									ProsesUjianHelper.kuotaUjian.remove(hasilUjianMahasiswa.getKeyhasil());
								}
							}

							PertemuanPunyaUjianHelper.tampilBolekIkutUjianAtauTidak(row, pertemuanPunyaUjian,
									tbmuser == null ? null : tbmuser.getMahasiswa(),
									tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa(), hasilUjianMahasiswa,
									new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											Common.createDefaultTimer(new EventListener() {
												@Override
												public void onEvent(Event arg0) throws Exception {
													loadDataMateri(cari, rows, paging, true, materiPil, ujianPil,
															tugasPil, label, tbmuser, pertemuansa, edit,
															urutBerdasarkanNama);
												}
											});
										}
									}, eventListeners);
						}

						if (!eventListeners.isEmpty()) {
							a.addEventListener("onClick", eventListeners.get(0));
							downloadButton.addEventListener("onClick", eventListeners.get(0));
						} else {
							downloadButton.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									new PertemuanHelper(tbmuser == null ? null : tbmuser.getMahasiswa(),
											tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
											.display(pertemuan, new DataLoader() {
												@Override
												public void loadData(Object value) {
												}
											}, 6);
								}
							});
							a.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									new PertemuanHelper(tbmuser == null ? null : tbmuser.getMahasiswa(),
											tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
											.display(pertemuan, new DataLoader() {
												@Override
												public void loadData(Object value) {
												}
											}, 6);
								}
							});
						}
					} else if (objects[0] instanceof AudioPertemuan) {
						final AudioPertemuan audioPertemuan = (AudioPertemuan) objects[0];

						String n = audioPertemuan == null ? ""
								: (audioPertemuan.getLink() == null || audioPertemuan.getLink().isEmpty()
										? audioPertemuan.getNama()
										: audioPertemuan.getLink());
						if (n == null)
							n = "";
						String isi = audioPertemuan.getKeteranganTambahan();
						if (isi != null && !isi.trim().isEmpty())
							n = isi;

						Groupbox vbox = new ais.ui.util.MyGroupboxStyled();
						vbox.setWidth("95%");
						vbox.setStyle(
								"background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; margin: 6px 0px 12px 10px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);");
						vbox.setParent(row);

						Toolbarbutton downloadButton = new ais.ui.util.MyToolbarbuttonConfig(
								n.length() > 150 ? n.substring(0, 150) + "..." : n, "/img/svg/sound-on.svg");
						downloadButton.setTooltiptext("Download \"" + audioPertemuan.getNama() + "\"");
						downloadButton.setAttribute("janganDisabled", true);
						downloadButton.setStyle(
								"font-size: 14px; font-weight: 600; color: #1e40af; text-decoration: none; padding-bottom: 8px; display: block;");
						vbox.appendChild(downloadButton);

						TampilanELearningAction.dilihat(pertemuan, "audio_" + audioPertemuan.getId(), "Akses", false)
								.setParent(vbox);

						downloadButton.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (ProfileUtil.chekSyarat(pertemuan.ambilVOPembelajaran(),
										audioPertemuan.getSyaratAkses()))
									return;
								pertemuan.masukkanData("audio_" + audioPertemuan.getId());
								if (audioPertemuan.getGdrive() != null) {
									audioPertemuan.tampilGDrive(null);
								} else {
									String link = audioPertemuan == null ? null
											: (audioPertemuan.getLink() == null || audioPertemuan.getLink().isEmpty()
													? null
													: audioPertemuan.getLink());
									if (audioPertemuan != null
											&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
										link = audioPertemuan.createLinkUri();
									}
									if (audioPertemuan != null && link != null && !link.trim().isEmpty()) {
										if (Common.isMobile())
											ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
										else
											Clients.evalJavaScript("popupCenter({url: '" + link
													+ "', title: 'data', w: 1200, h: 600});");
									} else {
										MyMessageboxConfig.show("Mohon maaf, berkas yang Bapak/Ibu akses saat ini tidak dapat ditemukan pada sistem. Langkah yang dapat dilakukan: (1) mohon menunggu beberapa saat dan mencoba kembali karena berkas kemungkinan masih dalam proses pengunggahan; (2) mohon memastikan koneksi internet Bapak/Ibu dalam keadaan stabil; (3) apabila berkas tetap tidak dapat dibuka, mohon menghubungi pengajar atau administrator sistem agar berkas tersebut dapat diunggah ulang.", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}
								}
							}
						}); 

						A a;
						vbox.appendChild(a = new A(
								"Audio pertemuan ke: " + pertemuan.getPertemuanKe() + ", " + pertemuan.info() + ", "
										+ ((pertemuan.getTanggal() == null ? "-"
												: (SmartDateTimeUtil.getDayString(pertemuan.getTanggal(),
														pertemuan.getWaktuMulai())
														+ Common.dateFormat6.get().format(pertemuan.getTanggal()))
														+ " "
														+ (pertemuan.getWaktuMulai() == null
																&& pertemuan.getWaktuSelesai() == null ? ""
																		: pertemuan.getWaktuMulai() + "-"
																				+ pertemuan.getWaktuSelesai())))));
						a.setStyle(
								"font-size: 11px; color: #475569; background-color: #f1f5f9; padding: 4px 10px; border-radius: 12px; display: inline-block; margin-top: 10px; text-decoration: none; border: 1px solid #cbd5e1;");

						a.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								new PertemuanHelper(tbmuser == null ? null : tbmuser.getMahasiswa(),
										tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
										.display(pertemuan, new DataLoader() {
											@Override
											public void loadData(Object value) {
											}
										}, 4, audioPertemuan);
							}
						});

						if (edit) {
							MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Lihat", "/img/svg/eye.svg");
							button.setStyle(
									"background-color: #eff6ff; color: #1d4ed8; padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: 600; text-decoration: none; margin-left: 10px; border: 1px solid #bfdbfe;");
							button.setParent(row);
							button.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									new PertemuanHelper(tbmuser == null ? null : tbmuser.getMahasiswa(),
											tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
											.display(pertemuan, new DataLoader() {
												@Override
												public void loadData(Object value) {
												}
											}, 4, audioPertemuan);
								}
							});
						}

					} else if (objects[0] instanceof VideoPertemuan) {

						final VideoPertemuan videoPertemuan = (VideoPertemuan) objects[0];

						String n = videoPertemuan == null ? ""
								: (videoPertemuan.getLink() == null || videoPertemuan.getLink().isEmpty()
										? videoPertemuan.getNama()
										: videoPertemuan.getLink());
						if (n == null)
							n = "";
						String isi = videoPertemuan.getKeteranganTambahan();
						if (isi != null && !isi.trim().isEmpty())
							n = isi;

						Groupbox vbox = new ais.ui.util.MyGroupboxStyled();
						vbox.setWidth("95%");
						vbox.setStyle(
								"background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; margin: 6px 0px 12px 10px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);");
						vbox.setParent(row);

						Toolbarbutton downloadButton = new ais.ui.util.MyToolbarbuttonConfig(
								n.length() > 150 ? n.substring(0, 150) + "..." : n, "/img/svg/camera-video.svg");
						downloadButton.setTooltiptext("Download \"" + videoPertemuan.getNama() + "\"");
						downloadButton.setAttribute("janganDisabled", true);
						downloadButton.setStyle(
								"font-size: 14px; font-weight: 600; color: #1e40af; text-decoration: none; padding-bottom: 8px; display: block;");
						vbox.appendChild(downloadButton);

						TampilanELearningAction.dilihat(pertemuan, "video_" + videoPertemuan.getId(), "Akses", false)
								.setParent(vbox);

						downloadButton.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (ProfileUtil.chekSyarat(pertemuan.ambilVOPembelajaran(),
										videoPertemuan.getSyaratAkses()))
									return;
								pertemuan.masukkanData("video_" + videoPertemuan.getId());
								if (videoPertemuan.getGdrive() != null) {
									videoPertemuan.tampilGDrive(null);
								} else {
									String link = videoPertemuan == null ? null
											: (videoPertemuan.getLink() == null || videoPertemuan.getLink().isEmpty()
													? null
													: videoPertemuan.getLink());
									if (videoPertemuan != null
											&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
										link = videoPertemuan.createLinkUri();
									}
									if (videoPertemuan != null && link != null && !link.trim().isEmpty()) {
										if (Common.isMobile())
											ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
										else
											Clients.evalJavaScript("popupCenter({url: '" + link
													+ "', title: 'data', w: 1200, h: 600});");
									} else {
										MyMessageboxConfig.show("Mohon maaf, berkas yang Bapak/Ibu akses saat ini tidak dapat ditemukan pada sistem. Langkah yang dapat dilakukan: (1) mohon menunggu beberapa saat dan mencoba kembali karena berkas kemungkinan masih dalam proses pengunggahan; (2) mohon memastikan koneksi internet Bapak/Ibu dalam keadaan stabil; (3) apabila berkas tetap tidak dapat dibuka, mohon menghubungi pengajar atau administrator sistem agar berkas tersebut dapat diunggah ulang.", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}
								}
							}
						});

						A a;
						vbox.appendChild(a = new A(
								"Video pertemuan ke: " + pertemuan.getPertemuanKe() + ", " + pertemuan.info() + ", "
										+ ((pertemuan.getTanggal() == null ? "-"
												: (SmartDateTimeUtil.getDayString(pertemuan.getTanggal(),
														pertemuan.getWaktuMulai())
														+ Common.dateFormat6.get().format(pertemuan.getTanggal()))
														+ " "
														+ (pertemuan.getWaktuMulai() == null
																&& pertemuan.getWaktuSelesai() == null ? ""
																		: pertemuan.getWaktuMulai() + "-"
																				+ pertemuan.getWaktuSelesai())))));
						a.setStyle(
								"font-size: 11px; color: #475569; background-color: #f1f5f9; padding: 4px 10px; border-radius: 12px; display: inline-block; margin-top: 10px; text-decoration: none; border: 1px solid #cbd5e1;");

						a.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								new PertemuanHelper(tbmuser == null ? null : tbmuser.getMahasiswa(),
										tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
										.display(pertemuan, new DataLoader() {
											@Override
											public void loadData(Object value) {
											}
										}, 5, videoPertemuan);
							}
						});

						if (edit) {
							MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Lihat", "/img/svg/eye.svg");
							button.setStyle(
									"background-color: #eff6ff; color: #1d4ed8; padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: 600; text-decoration: none; margin-left: 10px; border: 1px solid #bfdbfe;");
							button.setParent(row);
							button.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									new PertemuanHelper(tbmuser == null ? null : tbmuser.getMahasiswa(),
											tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
											.display(pertemuan, new DataLoader() {
												@Override
												public void loadData(Object value) {
												}
											}, 5, videoPertemuan);
								}
							});
						}
					}
				}
				index++;
			}

			if (paging != null && paging.getTotalSize() > (mulaiParamMateri + sampaiParamMateri)) {
				row = new MyFormRow();
				rows.appendChild(row);

				if (edit)
					ais.ui.util.ZkCompat.setSpans(row, "2");

				MyToolbarbuttonConfig a = new MyToolbarbuttonConfig("Data selanjutnya.. ("
						+ (paging.getTotalSize() - (mulaiParamMateri + sampaiParamMateri)) + " data)",
						"/img/Button-Next-icon.png");
				a.setStyle(
						"font-size:12px; font-weight: 500; color: #2563eb; background: #eff6ff; border-radius: 6px; padding: 4px 10px; margin-top: 10px;");
				row.appendChild(a);
				a.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						int sampaiParamMateriBaru = sampaiParamMateri + jumlahDataDalamSatuHalamanElearning;
						paging.setAttribute("mulaiParam", mulaiParamMateri);
						paging.setAttribute("sampaiParam", sampaiParamMateriBaru);
						loadDataMateri(cari, rows, paging, false, materiPil, ujianPil, tugasPil, label, tbmuser,
								pertemuansa, edit, urutBerdasarkanNama);
					}
				});
			}

			pertemuanFileContents.clear();
			pertemuanFileContents = null;
		}
	}

	private void loadDataKomentar(final Textbox cari, final Rows rows, final Paging paging, final boolean refresh,
			final Integer jenis, final boolean materiBol, final boolean ujianBol, final boolean tugasBol,
			final boolean dosenBol, final boolean mahasiswaBol, final boolean guruBol, final boolean siswaBol,
			final boolean adminBol, final Label label, final EventListener eventListenerReload) {

		final Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		final Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		final Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();
		final CalonSiswa calonSiswa = tbmuser == null ? null : tbmuser.getCalonSiswa();
		final BiodataCalonMahasiswa biodataCalonMahasiswa = tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa();

		dosenPil = new MyCheckboxConfig("Dosen");
		mahasiswaPil = new MyCheckboxConfig("Mahasiswa");
		guruPil = new MyCheckboxConfig("Guru");
		siswaPil = new MyCheckboxConfig("Siswa");
		adminPil = new MyCheckboxConfig("Admin");

		dosenPil.addEventListener("onClick", eventListenerReload);
		mahasiswaPil.addEventListener("onClick", eventListenerReload);
		guruPil.addEventListener("onClick", eventListenerReload);
		siswaPil.addEventListener("onClick", eventListenerReload);
		adminPil.addEventListener("onClick", eventListenerReload);

		dosenPil.setChecked(dosenBol);
		mahasiswaPil.setChecked(mahasiswaBol);
		adminPil.setChecked(adminBol);
		guruPil.setChecked(guruBol);
		siswaPil.setChecked(siswaBol);

		dosenPil.setVisible(pt);
		mahasiswaPil.setVisible(pt);
		guruPil.setVisible(ya);
		siswaPil.setVisible(ya);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setStyle("border:0px;background: transparent;font-size: x-small;");
		row.setParent(rows);

		Hbox hbox = new Hbox(new Component[] { dosenPil, mahasiswaPil, guruPil, siswaPil, adminPil });
		hbox.setStyle(
				"background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 10px 15px; box-shadow: inset 0 2px 4px rgba(0,0,0,0.02); width: 100%; margin-bottom: 12px;");
		hbox.setAlign("center");
		hbox.setSpacing("20px");
		row.appendChild(hbox);

		paging.setPageSize(jumlahDataDalamSatuHalamanElearning);
		boolean awal = false;
		if (refresh || dataMateriKomentar == null || !cari.getValue().trim().equalsIgnoreCase(cariSebelumnya)) {
			dataMateriKomentar = PertemuanFileContent.ambilKomentar(dashboardTimelinePertemuan.pertemuansa, refresh,
					dosenBol, mahasiswaBol, guruBol, siswaBol, adminBol, cari.getValue().trim(), label);
			awal = true;
			dataMateriValuesKomentar = dataMateriKomentar.values();
		}

		cariSebelumnya = cari.getValue().trim();

		if (dataMateriValuesKomentar != null) {

			int size = dataMateriValuesKomentar.size();
			paging.setTotalSize(size);
			paging.setVisible(size > jumlahDataDalamSatuHalamanElearning);

			if (awal) {
				try {
					int halaman = (size / jumlahDataDalamSatuHalamanElearning);
					paging.setActivePage(halaman);
				} catch (Exception e) {
					try {
						int halaman = (size / jumlahDataDalamSatuHalamanElearning);
						paging.setActivePage(halaman - 1);
					} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:4765");
					}
				}
			}

			int index = 0;
			mulaiParam = dataMateriValuesKomentar.size() - jumlahDataDalamSatuHalamanElearning;
			if (mulaiParam < 0)
				mulaiParam = 0;
			sampaiParam = jumlahDataDalamSatuHalamanElearning;

			Toolbarbutton back = new MyToolbarbuttonConfig("Diskusi sebelumnya.. ", "/img/Go-back-icon.png");

			row = new MyFormRow();
			rows.appendChild(row);

			if (refresh) {
				paging.setAttribute("mulaiParam", null);
				paging.setAttribute("sampaiParam", null);
			}

			back.setStyle(
					"font-size:12px; font-weight: 500; color: #2563eb; background: #eff6ff; border-radius: 6px; padding: 4px 10px; margin-bottom: 10px;");
			row.appendChild(back);
			back.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					mulaiParam = mulaiParam - jumlahDataDalamSatuHalamanElearning;
					sampaiParam = sampaiParam + jumlahDataDalamSatuHalamanElearning;
					paging.setAttribute("mulaiParam", mulaiParam);
					paging.setAttribute("sampaiParam", sampaiParam);

					loadDataKanan(cari, rows, paging, false, jenis, materiBol, ujianBol, tugasBol, dosenBol,
							mahasiswaBol, guruBol, siswaBol, adminBol, label);
				}
			});

			if (paging.getAttribute("mulaiParam") != null)
				mulaiParam = (Integer) paging.getAttribute("mulaiParam");
			if (paging.getAttribute("sampaiParam") != null)
				sampaiParam = (Integer) paging.getAttribute("sampaiParam");

			back.getParent().setVisible(mulaiParam > 0);

			int jmlMateri = 0;
			for (Object[] objects : dataMateriValuesKomentar) {
				if (index < mulaiParam) {
					jmlMateri++;
					back.setLabel("Diskusi sebelumnya.. (" + jmlMateri + " dsk)");
				}

				if (index >= mulaiParam && index < (mulaiParam + sampaiParam)) {
					row = new MyFormRow();
					row.setStyle("border:0px;background: transparent;");
					row.setParent(rows);
					final Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
							objects[1].toString());

					final Long diskusiId = (Long) objects[0];
					PertemuanPunyaDiskusi pertemuanPunyaDiskusi = (PertemuanPunyaDiskusi) GeneralValueObject
							.ambilData(PertemuanPunyaDiskusi.class, diskusiId.toString());

					Tbmuser usrkomentar = pertemuanPunyaDiskusi.getTbmuser();
					Groupbox vbox = new ais.ui.util.MyGroupboxStyled();
					vbox.setStyle(
							"background: transparent; border: none; padding: 0; margin-bottom: 15px; width: 100%; position: relative;");
					vbox.setParent(row);

					try {
						if (pertemuanPunyaDiskusi.getMahasiswa() != null)
							CommonMedia.tampilkanGambarKecil(pertemuanPunyaDiskusi.getMahasiswa(), "42px", "right")
									.setParent(vbox);
						else if (pertemuanPunyaDiskusi.getSiswa() != null)
							CommonMedia.tampilkanGambarKecil(pertemuanPunyaDiskusi.getSiswa(), "42px", "right")
									.setParent(vbox);
						else if (pertemuanPunyaDiskusi.getBiodataCalonMahasiswa() != null)
							CommonMedia.tampilkanGambarKecil(pertemuanPunyaDiskusi.getBiodataCalonMahasiswa(), "42px",
									"right").setParent(vbox);
						else if (pertemuanPunyaDiskusi.getCalonSiswa() != null)
							CommonMedia.tampilkanGambarKecil(pertemuanPunyaDiskusi.getCalonSiswa(), "42px", "right")
									.setParent(vbox);
						else if (pertemuanPunyaDiskusi.getDosen() != null)
							CommonMedia.tampilkanGambarKecil(pertemuanPunyaDiskusi.getDosen(), "42px", "right")
									.setParent(vbox);
						else if (usrkomentar != null)
							CommonMedia.tampilkanGambarKecil(usrkomentar, "42px", "right").setParent(vbox);
						else
							new Label().setParent(vbox);

						String oleh = pertemuanPunyaDiskusi.getBiodataCalonMahasiswa() != null
								? (pertemuanPunyaDiskusi.getBiodataCalonMahasiswa().getNama() + " (Calon Mahasiswa)")
								: (pertemuanPunyaDiskusi.getMahasiswa() != null
										? pertemuanPunyaDiskusi.getMahasiswa().getNama() + " (Mahasiswa)"
										: (pertemuanPunyaDiskusi.getSiswa() != null
												? pertemuanPunyaDiskusi.getSiswa().getNama() + " (Siswa)"
												: (pertemuanPunyaDiskusi.getCalonSiswa() != null
														? pertemuanPunyaDiskusi.getCalonSiswa().getNama()
																+ " (Calon Siswa)"
														: "")));

						try {
							if (oleh.trim().equals(""))
								oleh = pertemuanPunyaDiskusi.getDosen() != null
										? pertemuanPunyaDiskusi.getDosen().getNama() + " (Dosen)"
										: "";
							if (oleh.trim().equals(""))
								oleh = pertemuanPunyaDiskusi.getTbmuser() != null
										? pertemuanPunyaDiskusi.getTbmuser().getUserNama() + " ("
												+ pertemuanPunyaDiskusi.getTbmuser().hakAkses().getRoleName() + ")"
										: "";
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

						String waktu = pertemuanPunyaDiskusi.getTanggal_dirubah() == null ? ""
								: SmartDateTimeUtil.getDayString(pertemuanPunyaDiskusi.getTanggal_dirubah(), null)
										+ Common.dateFormat5.get().format(pertemuanPunyaDiskusi.getTanggal_dirubah());
						String isi = pertemuanPunyaDiskusi.getIsi();
						final List<String> urls = Common.getUrls(isi);
						for (String u : urls) {
							isi = org.apache.commons.lang3.StringUtils.replace(isi, u,
									"<a href='" + u + "' target='_blank'>" + u + "</a>");
						}

						try {
							isi = Jsoup.parse(isi.replaceAll("\n", "<br>")).text();
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}

						new ais.ui.util.MyHtml(
								"<div style='margin-right: 55px; background: #ffffff; padding: 12px 16px; border-radius: 12px 0 12px 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); border: 1px solid #edf2f7; position: relative;'>"
										+ "<div style='display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;'>"
										+ "<strong style='color:#2563eb; font-size:12px;'>" + oleh + "</strong>"
										+ "<span style='color:#94a3b8; font-size:10px; font-weight: 500;'>" + waktu
										+ "</span>"
										+ "</div> <div style='font-size:13px; color:#334155; line-height: 1.5; word-wrap: break-word;'>"
										+ isi + "</div></div>")
								.setParent(vbox);

						if (!pertemuanPunyaDiskusi.getPertemuan().getKomentarDitutup()) {
							Hbox tombol = new Hbox();
							tombol.setStyle("margin-top: 5px;");
							tombol.setParent(vbox);
							Toolbarbutton toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig(
									pertemuanPunyaDiskusi.getParent() == null ? "Balas" : "Tanggapi", "");
							toolbarbutton.setVisible(!toolbarbutton.getLabel().isEmpty());
							toolbarbutton.setStyle(
									"font-size:10px; font-weight:600; color:#475569; background:#f8fafc; border:1px solid #e2e8f0; padding:4px 12px; border-radius:15px; margin-right:6px; margin-top:6px; text-decoration:none; cursor:pointer;");

							toolbarbutton.setParent(tombol);
							toolbarbutton.setVisible(pertemuanPunyaDiskusi.getParent() == null
									|| pertemuanPunyaDiskusi.getParent().getParent() == null);

							toolbarbutton.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									PertemuanPunyaDiskusi baru = new PertemuanPunyaDiskusi();
									baru.setDosen(dosen);
									baru.setPertemuan(pertemuan);
									baru.setMahasiswa(mahasiswa);
									baru.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
									baru.setSiswa(siswa);
									baru.setCalonSiswa(calonSiswa);

									PertemuanPunyaDiskusi pertemuanPunyaDiskusi = (PertemuanPunyaDiskusi) GeneralValueObject
											.ambilData(PertemuanPunyaDiskusi.class, diskusiId.toString());

									PertemuanPunyaDiskusiHelper.onAddKomentar(pertemuanPunyaDiskusi, baru, null,
											mahasiswa, dosen, biodataCalonMahasiswa, siswa, calonSiswa,
											new EventListener() {
												@Override
												public void onEvent(Event arg0) throws Exception {
													PertemuanPunyaDiskusi a = (PertemuanPunyaDiskusi) arg0.getData();
													if (a != null && a.getId() != null) {
														PertemuanHelper pertemuanHelper = new PertemuanHelper(mahasiswa,
																biodataCalonMahasiswa);
														pertemuanHelper.selectedDiskusi = a.getId();
														pertemuanHelper.display(pertemuan, new DataLoader() {
															@Override
															public void loadData(Object value) {
															}
														}, 7);
													}
												}
											});
								}
							});

							boolean boleh = (mahasiswa != null && mahasiswa.getId() != null
									&& pertemuanPunyaDiskusi.getMahasiswa() != null
									&& pertemuanPunyaDiskusi.getMahasiswa().getId() != null
									&& mahasiswa.getId().equals(pertemuanPunyaDiskusi.getMahasiswa().getId()));
							boleh = boleh || (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null
									&& pertemuanPunyaDiskusi.getBiodataCalonMahasiswa() != null
									&& pertemuanPunyaDiskusi.getBiodataCalonMahasiswa().getId() != null
									&& biodataCalonMahasiswa.getId()
											.equals(pertemuanPunyaDiskusi.getBiodataCalonMahasiswa().getId()));
							boleh = boleh || (siswa != null && siswa.getId() != null
									&& pertemuanPunyaDiskusi.getSiswa() != null
									&& pertemuanPunyaDiskusi.getSiswa().getId() != null
									&& siswa.getId().equals(pertemuanPunyaDiskusi.getSiswa().getId()));
							boleh = boleh || (calonSiswa != null && calonSiswa.getId() != null
									&& pertemuanPunyaDiskusi.getCalonSiswa() != null
									&& pertemuanPunyaDiskusi.getCalonSiswa().getId() != null
									&& calonSiswa.getId().equals(pertemuanPunyaDiskusi.getCalonSiswa().getId()));
							boleh = boleh || (dosen != null && dosen.getId() != null
									&& pertemuanPunyaDiskusi.getDosen() != null
									&& pertemuanPunyaDiskusi.getDosen().getId() != null
									&& dosen.getId().equals(pertemuanPunyaDiskusi.getDosen().getId()));
							boleh = boleh || (tbmuser != null && pertemuanPunyaDiskusi.getTbmuser() != null
									&& pertemuanPunyaDiskusi.getTbmuser().getUserId() != null
									&& tbmuser.getUserId() != null
									&& tbmuser.getUserId().equals(pertemuanPunyaDiskusi.getTbmuser().getUserId()));

							boolean bolehHapus = boleh || Common.getApakahAdmin()
									|| (dosen != null && dosen.getId() != null);

							Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ubah");
							button.setStyle(
									"font-size:10px; font-weight:600; color:#475569; background:#f8fafc; border:1px solid #e2e8f0; padding:4px 12px; border-radius:15px; margin-right:6px; margin-top:6px; text-decoration:none; cursor:pointer;");
							button.setVisible(boleh);
							button.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									PertemuanPunyaDiskusi pertemuanPunyaDiskusi = (PertemuanPunyaDiskusi) GeneralValueObject
											.ambilData(PertemuanPunyaDiskusi.class, diskusiId.toString());
									PertemuanPunyaDiskusiHelper.onAddKomentar(pertemuanPunyaDiskusi.getParent(),
											pertemuanPunyaDiskusi, null, mahasiswa, dosen, biodataCalonMahasiswa, siswa,
											calonSiswa, new EventListener() {
												@Override
												public void onEvent(Event arg0) throws Exception {
													Common.createDefaultTimer(new EventListener() {
														@Override
														public void onEvent(Event arg0) throws Exception {
															loadDataKanan(cari, rows, paging, refresh, jenis, materiBol,
																	ujianBol, tugasBol, dosenBol, mahasiswaBol, guruBol,
																	siswaBol, adminBol, null);
														}
													});
												}
											});
								}
							});
							button.setParent(tombol);

							button = new ais.ui.util.MyToolbarbuttonConfig("Hapus");
							button.setStyle(
									"font-size:10px; font-weight:600; color:#475569; background:#f8fafc; border:1px solid #e2e8f0; padding:4px 12px; border-radius:15px; margin-right:6px; margin-top:6px; text-decoration:none; cursor:pointer;");
							button.setVisible(bolehHapus);
							button.setTooltiptext("Hapus Data");
							button.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Mohon diperhatikan bahwa data yang telah dihapus tidak dapat dikembalikan lagi. Silakan pilih OK untuk melanjutkan penghapusan, atau Batal untuk membatalkan proses ini.", "Pertanyaan",
											MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
											MyMessageboxConfig.QUESTION, new EventListener() {
												@Override
												public void onEvent(Event event) throws Exception {
													int i = Integer.parseInt(event.getData().toString());
													if (i == MyMessageboxConfig.OK) {
														try {
															PertemuanPunyaDiskusi pertemuanPunyaDiskusi = (PertemuanPunyaDiskusi) GeneralValueObject
																	.ambilData(PertemuanPunyaDiskusi.class,
																			diskusiId.toString());
															Session session = HibernateUtil.currentSession();
															session.createSQLQuery(
																	"delete from pertemuan_punya_diskusi where parent in (select id from pertemuan_punya_diskusi where parent="
																			+ pertemuanPunyaDiskusi.getId() + ")")
																	.executeUpdate();
															session.createSQLQuery(
																	"delete from pertemuan_punya_diskusi where parent="
																			+ pertemuanPunyaDiskusi.getId())
																	.executeUpdate();
															Common.refreshDelete(pertemuanPunyaDiskusi);
															Common.createDefaultTimer(new EventListener() {
																@Override
																public void onEvent(Event arg0) throws Exception {
																	loadDataKanan(cari, rows, paging, refresh, jenis,
																			materiBol, ujianBol, tugasBol, dosenBol,
																			mahasiswaBol, guruBol, siswaBol, adminBol,
																			null);
																}
															});
														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);
															MyMessageboxConfig.show(
																	MyMessageboxConfig.format("Mohon maaf, data ini tidak dapat dihapus karena masih memiliki keterkaitan (relasi) dengan data lain di dalam sistem. Adapun rincian teknis kesalahan yang terjadi adalah sebagai berikut: {V1}. Langkah yang dapat dilakukan: (1) mohon terlebih dahulu menghapus atau melepaskan data lain yang masih terkait dengan data ini; (2) setelah tidak terdapat lagi keterkaitan, silakan mengulangi proses penghapusan; (3) apabila kendala masih berlanjut, mohon menghubungi administrator sistem dengan menyertakan rincian kesalahan tersebut."
																			, e.getMessage()));
														}
													}
												}
											});
								}
							});
							button.setParent(tombol);

							if (pertemuanPunyaDiskusi.getPertemuan().getIzinkanUploadLampiranDiGrive() && boleh) {
								EventListener uploadeventListener = new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										PertemuanPunyaDiskusi pertemuanPunyaDiskusi = (PertemuanPunyaDiskusi) GeneralValueObject
												.ambilData(PertemuanPunyaDiskusi.class, diskusiId.toString());
										pertemuanPunyaDiskusi.setDosen(dosen);
										pertemuanPunyaDiskusi.setMahasiswa(mahasiswa);
										pertemuanPunyaDiskusi.setBiodataCalonMahasiswa(biodataCalonMahasiswa);

										Tbmuser tbmuser = Common.getCurrentUser();
										if (tbmuser != null && tbmuser.getUserPassword() != null
												&& !tbmuser.getUserPassword().trim().equals("")) {
											pertemuanPunyaDiskusi.setTbmuser(tbmuser);
										}
										Common.refreshSaveOrUpdate(pertemuanPunyaDiskusi);

										LampiranLain copy = (LampiranLain) arg0.getData();
										if (copy != null) {
											Session session = StreamingHibernateUtil.getInstance().currentSession();
											try {
												copy.setRef(pertemuanPunyaDiskusi.getId());
												session.getTransaction().begin();
												session.update(copy);
												session.getTransaction().commit();
											} catch (Exception e) {
												ais.common.Common.tampilErrorJikaAdmin(e);
											}
											StreamingHibernateUtil.getInstance().closeSession();
										}

										PertemuanHelper pertemuanHelper = new PertemuanHelper(mahasiswa,
												biodataCalonMahasiswa);
										pertemuanHelper.selectedDiskusi = pertemuanPunyaDiskusi.getId();
										pertemuanHelper.display(pertemuan, new DataLoader() {
											@Override
											public void loadData(Object value) {
											}
										}, 7);
									}
								};

								Toolbarbutton upload = FileFotoLain.tampilkanTombolUploadGdrive(null, null,
										uploadeventListener, null, null, LampiranLain.DISKUSI, false, null, "Lampiran",
										null, false, Common.refSementara(), false, LampiranLain.class);
								upload.setImage("");
								upload.setLabel("Lampiran");
								upload.setOrient("vertical");
								upload.setStyle(
										"font-size:10px; font-weight:600; color:#475569; background:#f8fafc; border:1px solid #e2e8f0; padding:4px 12px; border-radius:15px; margin-right:6px; margin-top:6px; text-decoration:none; cursor:pointer;");
								tombol.appendChild(upload);
							}

							if (pertemuanPunyaDiskusi.getParent() == null
									|| pertemuanPunyaDiskusi.getParent().getParent() == null) {
								if (pertemuanPunyaDiskusi != null && pertemuanPunyaDiskusi.getId() != null) {
									int balasan = 0;
									TreeSet<Long> pertemuanPunyaDiskusisa = pertemuan
											.ambilPertemuanPunyaDiskusiTotal(false);
									for (Long l : pertemuanPunyaDiskusisa) {
										PertemuanPunyaDiskusi da = (PertemuanPunyaDiskusi) GeneralValueObject
												.ambilData(PertemuanPunyaDiskusi.class, l.toString());
										if (da != null && da.getParent() != null && da.getParent().getId() != null
												&& da.getParent().getId().equals(pertemuanPunyaDiskusi.getId())) {
											balasan++;
										}
									}
									pertemuanPunyaDiskusisa = null;

									String b = pertemuanPunyaDiskusi.getParent() == null ? "balasan" : "tanggapan";
									String balasanA = Common.numberFormat.get().format(balasan) + " " + b;
									Toolbarbutton balasana = new MyToolbarbuttonConfig(balasanA);
									balasana.setStyle("font-size:11px; font-weight:600; color: "
											+ (balasan > 0 ? "#ef4444" : "#3b82f6")
											+ "; background: #fef2f2; padding: 4px 10px; border-radius: 12px; margin-top:6px; margin-left: 6px; text-decoration: none; display: inline-block;");
									balasana.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											PertemuanHelper pertemuanHelper = new PertemuanHelper(mahasiswa,
													biodataCalonMahasiswa);
											pertemuanHelper.selectedDiskusi = diskusiId;
											pertemuanHelper.display(pertemuan, new DataLoader() {
												@Override
												public void loadData(Object value) {
												}
											}, 7);
										}
									});
									balasana.setParent(vbox);
								}
							}
						}

						A a = new A(pertemuan.info());
						a.setStyle(
								"font-size:11px; color:#64748b; margin-top: 8px; display: inline-block; text-decoration:none; font-style: italic;");
						a.setHref("");
						a.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								PertemuanHelper pertemuanHelper = new PertemuanHelper(mahasiswa, biodataCalonMahasiswa);
								pertemuanHelper.selectedDiskusi = diskusiId;
								pertemuanHelper.display(pertemuan, new DataLoader() {
									@Override
									public void loadData(Object value) {
									}
								}, 7);
							}
						});
						a.setParent(vbox);

						Vbox myVbox = new Vbox();
						myVbox.setParent(vbox);

						hbox = new Hbox();
						hbox.setParent(myVbox);
						LampiranLain.createDownloadUploadFileLain(hbox, pertemuanPunyaDiskusi.getId(),
								LampiranLain.DISKUSI, LampiranLain.DISKUSI, false, new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
									}
								}, null, false, false, false, false, null, false, true);

					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}
				index++;
			}
		}
	}

	private void loadDataKanan(final Textbox cari, final Rows rows, final Paging paging, final boolean refresh,
			final Integer jenis, final boolean materiBol, final boolean ujianBol, final boolean tugasBol,
			final boolean dosenBol, final boolean mahasiswaBol, final boolean guruBol, final boolean siswaBol,
			final boolean adminBol, final Label label) {

		// JANGAN early-return saat timeline belum siap. Pola lama `if(dashboardTimelinePertemuan==null) return;`
		// MENINGGALKAN placeholder "Ambil data..." BERPUTAR SELAMANYA (panel kanan menggantung) untuk kelas
		// yang timeline-nya null/lambat (mis. kelas tanpa pertemuan / gagal muat). Kini render tetap jalan
		// dengan daftar pertemuan KOSONG bila belum siap; ketika timeline selesai memuat, callback-nya
		// memanggil eventListenerMateri (reload) sehingga panel kanan terisi ulang otomatis (self-heal).
		final TreeMap<String, Long> pertemuansaAman = (dashboardTimelinePertemuan != null
				&& dashboardTimelinePertemuan.pertemuansa != null) ? dashboardTimelinePertemuan.pertemuansa
						: new TreeMap<String, Long>();

		EventListener eventListenerReload = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				// Perilaku eksklusif (seperti tab): klik checkbox yang baru di-centang →
				// uncheck yang lain; klik satu-satunya yang tercek → reset semua ke checked.
				if (arg0 != null && arg0.getTarget() instanceof MyCheckboxConfig
						&& (arg0.getTarget() == materiPil || arg0.getTarget() == ujianPil || arg0.getTarget() == tugasPil)) {
					MyCheckboxConfig klik = (MyCheckboxConfig) arg0.getTarget();
					if (klik.isChecked()) {
						// baru di-centang → jadikan satu-satunya
						if (klik != materiPil && materiPil != null) materiPil.setChecked(false);
						if (klik != ujianPil  && ujianPil  != null) ujianPil .setChecked(false);
						if (klik != tugasPil  && tugasPil  != null) tugasPil .setChecked(false);
					} else {
						// di-un-centang → jika kini semua kosong, reset ke semua
						boolean adaYgChecked = (materiPil != null && materiPil.isChecked())
								|| (ujianPil != null && ujianPil.isChecked())
								|| (tugasPil != null && tugasPil.isChecked());
						if (!adaYgChecked) {
							if (materiPil != null) materiPil.setChecked(true);
							if (ujianPil  != null) ujianPil .setChecked(true);
							if (tugasPil  != null) tugasPil .setChecked(true);
						}
					}
				}

				if (rows != null) {
					Common.clear(rows);
				}
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setStyle("border:0px;background: transparent;font-size: x-small;");
				row.setParent(rows);

				Vbox vbox = new Vbox();
				vbox.setParent(row);
				vbox.setWidth("100%");
				final Label label;
				vbox.appendChild(label = new Label(ais.common.Common.getBahasaConfig("Memuat tugas, ujian & materi...")));
				vbox.appendChild(new ais.ui.util.MyHtml("<div style='margin:6px auto;width:34px;height:34px;border:3px solid #e2e8f0;border-top-color:#2563eb;border-radius:50%;animation:aisSpin .8s linear infinite;'></div>"));
				final boolean ref;
				if (arg0 != null && arg0.getTarget() != null && (arg0.getTarget() == dosenPil || arg0.getTarget() == mahasiswaPil
						|| arg0.getTarget() == adminPil)) {
					ref = true;
				} else {
					ref = false;
				}
				Common.createDefaultTimerNoBusy(new EventListener() {
					@Override
					public void onEvent(Event a) throws Exception {
						loadDataKanan(cari, rows, paging, ref, jenis, label);
					}
				});
			}
		};

		if (rows != null) {

			Common.clear(rows);

		}
		paging.setVisible(false);
		paging.setPageIncrement(3);
		paging.setDetailed(false);

		if (jenis.equals(KOMENTAR)) {
			loadDataKomentar(cari, rows, paging, refresh, jenis, materiBol, ujianBol, tugasBol, dosenBol, mahasiswaBol,
					guruBol, siswaBol, adminBol, label, eventListenerReload);
		} else if (jenis.equals(MATERI)) {
			materiPil = new MyCheckboxConfig("Materi");
			ujianPil = new MyCheckboxConfig("Ujian");
			tugasPil = new MyCheckboxConfig("Tugas");

			materiPil.addEventListener("onClick", eventListenerReload);
			ujianPil.addEventListener("onClick", eventListenerReload);
			tugasPil.addEventListener("onClick", eventListenerReload);

			materiPil.setChecked(materiBol);
			ujianPil.setChecked(ujianBol);
			tugasPil.setChecked(tugasBol);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setStyle("border:0px;background: transparent;font-size: x-small;");
			row.setParent(rows);

			Hbox hbox = new Hbox(new Component[] { materiPil, ujianPil, tugasPil });
			hbox.setStyle(
					"background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 10px 15px; box-shadow: inset 0 2px 4px rgba(0,0,0,0.02); width: 100%; margin-bottom: 12px;");
			hbox.setSpacing("20px");
			hbox.setAlign("center");
			row.appendChild(hbox);

			boolean urutBerdasarkanNama = false;
			loadDataMateri(cari, rows, paging, refresh, materiPil, ujianPil, tugasPil, label, tbmuser,
					pertemuansaAman, false, urutBerdasarkanNama);
		}
	}

	private void loadMenu(Component parent, Integer jenis) {
		loadMenu(parent, jenis, null);
	}

	/**
	 * Membangun isi tab "Bimbingan": sebuah {@link ais.ui.util.MyButtonTabbox} berisi sub-tab
	 * (Tugas Akhir, Sidang, KKN, PKL, PA). Memakai MyButtonTabbox agar konten tab tampil
	 * dengan benar di dalam Tabpanel bersarang — native ZK Tabbox rentan kolaps atau gagal
	 * memuat konten saat di-nested di dalam Tabpanel yang dimuat secara lazy. Sub-tab yang
	 * ditampilkan mengikuti peran login: PT (dosen/mahasiswa/admin) → kelima sub-tab;
	 * sekolah (guru/siswa) → hanya PKL. Konten sub-tab dimuat LAZY oleh framework.
	 */
	private void bangunTabBimbingan(Component host) {
		boolean adaMhs = tbmuser != null && tbmuser.getMahasiswa() != null;
		boolean adaDosen = tbmuser != null && tbmuser.ambilDosen() != null;
		boolean adaSiswa = tbmuser != null && tbmuser.getSiswa() != null;
		boolean adaGuru = tbmuser != null && tbmuser.ambilGuru() != null;
		boolean sekolahSaja = (adaSiswa || adaGuru) && !adaMhs && !adaDosen;

		ais.ui.util.MyButtonTabbox btnBimbingan = ais.ui.util.MyButtonTabbox.buat(host, "100%", null);
		int idx = 1;
		if (!sekolahSaja) {
			tambahSubBimbinganBtn(btnBimbingan, idx++, "Tugas Akhir", "/img/svg/chalkboard-user.svg", TampilanELearningAction.BIMBINGAN);
			tambahSubBimbinganBtn(btnBimbingan, idx++, "Sidang", "/img/svg/journal-check.svg", TampilanELearningAction.SKRIPSI);
			tambahSubBimbinganBtn(btnBimbingan, idx++, "KKN", "/img/svg/users.svg", TampilanELearningAction.KKN);
		}
		tambahSubBimbinganBtn(btnBimbingan, idx++, "PKL", "/img/svg/user-business.svg", TampilanELearningAction.PKL);
		if (!sekolahSaja) {
			tambahSubBimbinganBtn(btnBimbingan, idx++, "PA", "/img/svg/user-tie.svg", TampilanELearningAction.KRS);
		}
		// Tab pertama dimuat otomatis oleh MyButtonTabbox saat tambahTabLazy dipanggil pertama kali.
	}

	/** Menambah satu sub-tab Bimbingan ke MyButtonTabbox; konten dimuat LAZY oleh framework. */
	private void tambahSubBimbinganBtn(ais.ui.util.MyButtonTabbox btnTabbox, int index, String label,
			String icon, final Integer jenis) {
		btnTabbox.tambahTabLazy(index, label, icon, new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(Div panel) throws Exception {
				renderTabelBimbingan(panel, jenis);
			}
		});
	}

	/**
	 * Merender satu sub-tab Bimbingan: BAR FILTER (gaya "Dasbor OBE") di atas + TABEL di bawah. Filter:
	 * Tahun Akademik, Semester, Fakultas, Program Studi, "Cari NIM/NIS/Nama", dan "Cari Judul"; tombol
	 * "Tampilkan" memuat ulang tabel sesuai filter.
	 *
	 * <p><b>Bar filter HANYA untuk peran yang melihat data lintas-orang</b> (dosen/guru/admin). Untuk
	 * <b>mahasiswa/siswa</b> bar filter DISEMBUNYIKAN dan langsung ditampilkan tabelnya, karena mereka
	 * hanya boleh melihat datanya sendiri &mdash; sudah dibatasi {@code terapkanScopingBimbingan} &mdash;
	 * sehingga tak ada gunanya menyaring/mencari lintas orang. Kolom tabel: Foto, NIM/NIS, Nama, Judul,
	 * TA/Smt, Aksi (Agenda, Nilai).</p>
	 */
	private void renderTabelBimbingan(final Component host, final Integer jenis) {
		org.zkoss.zul.Vlayout wrap = new org.zkoss.zul.Vlayout();
		wrap.setParent(host);
		wrap.setWidth("100%");
		wrap.setStyle("padding:8px; box-sizing:border-box;");

		final org.zkoss.zul.Div container = new org.zkoss.zul.Div();
		container.setWidth("100%");

		Tbmuser pengguna = Common.getCurrentUser();
		boolean bolehFilter = pengguna == null || (pengguna.getMahasiswa() == null && pengguna.getSiswa() == null);

		// Tombol "Pengajuan Baru" HANYA muncul di sub-tab Tugas Akhir & Sidang, dan TIDAK untuk dosen
		// (dosen berperan membimbing/menilai, bukan mengajukan). Mahasiswa -> mengajukan untuk dirinya;
		// admin -> memilih mahasiswanya lewat form. Lihat buatTombolPengajuan()/bukaPengajuanBimbingan().
		// TA & Sidang: non-dosen (mahasiswa ATAU admin) boleh mengajukan.
		boolean jenisPengajuanTaSidang = BIMBINGAN.equals(jenis) || SKRIPSI.equals(jenis);
		// KKN & PKL: PENGAJUAN MAHASISWA -> hanya utk mahasiswa yang login (halaman pendaftaran
		// bersifat student-centric); dosen TIDAK ditampilkan.
		boolean jenisPengajuanKknPkl = KKN.equals(jenis) || PKL.equals(jenis);
		final boolean bolehPengajuan = pengguna != null && pengguna.ambilDosen() == null
				&& (jenisPengajuanTaSidang || (jenisPengajuanKknPkl && pengguna.getMahasiswa() != null));

		if (!bolehFilter) {
			// Mahasiswa/siswa: hanya boleh melihat data sendiri -> tanpa bar filter, langsung tabel.
			if (bolehPengajuan) {
				org.zkoss.zul.Div barPengajuan = new org.zkoss.zul.Div();
				barPengajuan.setStyle("text-align:right;margin-bottom:8px;");
				barPengajuan.setParent(wrap);
				barPengajuan.appendChild(buatTombolPengajuan(host, jenis));
			}
			container.setParent(wrap);
			muatIsiTabelBimbingan(container, jenis, null, null, null, null, null, null);
			return;
		}

		org.zkoss.zul.Div filterCard = new org.zkoss.zul.Div();
		filterCard.setParent(wrap);
		filterCard.setStyle(
				"background:#ffffff;border:1px solid #e2e8f0;border-radius:10px;padding:10px 12px;margin-bottom:8px;");

		Grid fgrid = new Grid();
		fgrid.setParent(filterCard);
		fgrid.setWidth("100%");
		fgrid.setStyle("border:none;background:transparent;");
		Columns fcols = new Columns();
		fcols.setParent(fgrid);
		// 2 field per baris (label + kontrol) x2 = 4 kolom; fixed-layout agar semua kontrol pas 100%
		// dan kolom Fakultas/Cari Judul tak terpotong.
		for (int i = 0; i < 4; i++) {
			org.zkoss.zul.Column c = new org.zkoss.zul.Column();
			if (i % 2 == 0) {
				c.setWidth("150px");
			}
			c.setParent(fcols);
		}
		Rows frows = new Rows();
		frows.setParent(fgrid);

		final Combobox ta = new Combobox();
		ta.setWidth("100%");
		ta.setReadonly(true);
		Common.generateTahunAjaranDanSemua(ta);

		final Combobox smt = new Combobox();
		smt.setWidth("100%");
		smt.setReadonly(true);
		ais.ui.util.MyComboitemConfig ci = new ais.ui.util.MyComboitemConfig("Semua");
		ci.setValue(null);
		smt.appendChild(ci);
		ci = new ais.ui.util.MyComboitemConfig(Perkuliahan.GANJIL);
		ci.setValue(Perkuliahan.GANJIL);
		smt.appendChild(ci);
		ci = new ais.ui.util.MyComboitemConfig(Perkuliahan.GENAP);
		ci.setValue(Perkuliahan.GENAP);
		smt.appendChild(ci);
		smt.setSelectedIndex(0);

		final Combobox fak = new Combobox();
		fak.setWidth("100%");
		final Combobox jur = new Combobox();
		jur.setWidth("100%");
		try {
			Common.initFakultasDanJurusanDanSemua(fak, jur);
		} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:5456");
		}

		final ais.ui.util.MyTextbox cariMhs = new ais.ui.util.MyTextbox();
		cariMhs.setWidth("100%");
		// Filter DOSEN (pengganti "Cari Judul"): pemilih dosen (banbox). "Judul" kini DIGABUNG ke kotak
		// "Cari NIM/NIS/Nama / Judul" di bawah. Untuk dosen yang login, banbox terisi otomatis dirinya.
		final ais.action.master.helper.AmbilDataDosenBanbox dosenBox = new ais.action.master.helper.AmbilDataDosenBanbox();
		dosenBox.setWidth("100%");

		MyFormRow r1 = new MyFormRow();
		r1.setValign("middle");
		r1.setParent(frows);
		r1.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		r1.appendChild(ta);
		r1.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		r1.appendChild(smt);

		MyFormRow r2 = new MyFormRow();
		r2.setValign("middle");
		r2.setParent(frows);
		r2.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		r2.appendChild(fak);
		r2.appendChild(new ais.ui.util.MyLabelConfig("Program Studi"));
		r2.appendChild(jur);

		MyFormRow rCari = new MyFormRow();
		rCari.setValign("middle");
		rCari.setParent(frows);
		rCari.appendChild(new ais.ui.util.MyLabelConfig("Cari NIM/NIS/Nama / Judul"));
		rCari.appendChild(cariMhs);
		rCari.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		rCari.appendChild(dosenBox);

		container.setParent(wrap);

		final EventListener tampilListener = new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				Common.clear(container);
				muatIsiTabelBimbingan(container, jenis, ta, smt, fak, jur, cariMhs, dosenBox);
			}
		};

		MyFormRow r3 = new MyFormRow();
		r3.setParent(frows);
		r3.appendChild(new ais.ui.util.MyLabelConfig(""));
		MyToolbarbuttonConfig btnTampil = new MyToolbarbuttonConfig("Tampilkan", "/img/svg/search.svg");
		btnTampil.setTooltiptext("Muat ulang tabel sesuai filter");
		btnTampil.addEventListener("onClick", tampilListener);
		r3.appendChild(btnTampil);
		if (bolehPengajuan) {
			// Sel ke-3 kosong (perata), tombol "Pengajuan Baru" di sel ke-4 -> sejajar kolom kanan bar filter.
			r3.appendChild(new ais.ui.util.MyLabelConfig(""));
			r3.appendChild(buatTombolPengajuan(host, jenis));
		}

		cariMhs.addEventListener("onOK", tampilListener);
		dosenBox.addEventListener("onChange", tampilListener);

		muatIsiTabelBimbingan(container, jenis, ta, smt, fak, jur, cariMhs, dosenBox);
	}

	/**
	 * Membangun tombol <b>"Pengajuan Baru"</b> untuk sub-tab Tugas Akhir/Sidang. Saat diklik, form pengajuan
	 * bawaan modul dibuka lewat {@link #bukaPengajuanBimbingan}. Hanya dipanggil bila pengajuan diizinkan
	 * (jenis Tugas Akhir/Sidang &amp; pengguna BUKAN dosen) &mdash; lihat {@link #renderTabelBimbingan}.
	 */
	private MyToolbarbuttonConfig buatTombolPengajuan(final Component host, final Integer jenis) {
		MyToolbarbuttonConfig b = new MyToolbarbuttonConfig("Pengajuan Baru", "/img/svg/plus-circle.svg");
		String tip;
		if (SKRIPSI.equals(jenis)) {
			tip = "Ajukan pendaftaran sidang baru";
		} else if (KKN.equals(jenis)) {
			tip = "Ajukan / daftar KKN";
		} else if (PKL.equals(jenis)) {
			tip = "Ajukan / daftar PKL";
		} else {
			tip = "Ajukan judul tugas akhir baru";
		}
		b.setTooltiptext(tip);
		b.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				bukaPengajuanBimbingan(host, jenis);
			}
		});
		return b;
	}

	/**
	 * Membuka form <b>"Pengajuan Baru"</b> dengan <b>MENGGUNAKAN KEMBALI</b> form pengajuan bawaan tiap modul
	 * ({@code MahasiswaRequestTugasAkhirAction.onAddExternal} untuk Tugas Akhir; {@code SkripsiAction.onAddExternal}
	 * untuk Sidang) agar aturan &amp; tampilannya identik dengan menu aslinya.
	 * <ul>
	 * <li><b>Mahasiswa</b>: pemilik otomatis dirinya sendiri (di-<i>lock</i> oleh form). Bila sudah punya
	 * pengajuan/skripsi aktif, data itulah yang dibuka untuk dilengkapi &mdash; bukan membuat duplikat.</li>
	 * <li><b>Admin</b> (bukan dosen): entitas baru kosong; pemilih mahasiswa (banbox) AKTIF sehingga admin
	 * memilih mahasiswanya sendiri di dalam form.</li>
	 * </ul>
	 * Untuk <b>dosen</b> tombol tak pernah dibuat. Sesudah tersimpan, tabel sub-tab dimuat ulang agar baris
	 * baru langsung tampak. Memakai {@code HibernateUtil.currentSession()} (sesi ZK/event-thread &mdash; TIDAK
	 * ditutup di sini, sama seperti {@code Common.tampilkanTugasAkhir()}).
	 */
	private void bukaPengajuanBimbingan(final Component host, final Integer jenis) throws Exception {
		// KKN & PKL: buka HALAMAN PENDAFTARAN/pengajuan mahasiswa (KknUntukMahasiswaAction /
		// PklUntukMahasiswaAction) di jendela modal -> mahasiswa memilih periode & mendaftar seperti menu
		// aslinya. Sesudah ditutup, tabel sub-tab dimuat ulang. (TA/Sidang tetap pakai form onAddExternal.)
		if (KKN.equals(jenis)) {
			bukaHalamanPengajuanMhs(host, jenis, "Pengajuan KKN", "/pages/master/kkn/kkn_utk_mhs.zul");
			return;
		}
		if (PKL.equals(jenis)) {
			bukaHalamanPengajuanMhs(host, jenis, "Pengajuan PKL", "/pages/master/pkl/pkl_utk_mhs.zul");
			return;
		}
		Tbmuser pengguna = Common.getCurrentUser();
		final Mahasiswa mhs = pengguna == null ? null : pengguna.getMahasiswa();

		// Callback sesudah tersimpan: notifikasi ringkas + muat ulang tabel sub-tab.
		final EventListener setelahSimpan = new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				Object data = ev == null ? null : ev.getData();
				String judul = null;
				if (data instanceof MahasiswaRequestTugasAkhir) {
					judul = ((MahasiswaRequestTugasAkhir) data).getJudul();
				} else if (data instanceof Skripsi) {
					judul = ((Skripsi) data).getJudul();
				}
				MyMessageboxConfig.show(
						MyMessageboxConfig.format(
								"Pengajuan yang Bapak/Ibu lakukan telah berhasil disimpan ke dalam sistem{V1}",
								(judul == null || judul.trim().length() == 0 ? "." : " dengan judul:\n\n" + judul)),
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				try {
					Common.clear(host);
					renderTabelBimbingan(host, jenis);
				} catch (Exception ex) {
					Common.tampilErrorJikaAdmin(ex);
				}
			}
		};

		Session session = HibernateUtil.currentSession();
		if (SKRIPSI.equals(jenis)) {
			Skripsi skripsi = null;
			if (mhs != null) {
				skripsi = (Skripsi) session.createCriteria(Skripsi.class).add(Restrictions.eq("mahasiswa", mhs))
						.setMaxResults(1).uniqueResult();
			}
			if (skripsi == null) {
				skripsi = new Skripsi();
			}
			ais.action.master.SkripsiAction.onAddExternal(setelahSimpan, skripsi, mhs);
		} else { // BIMBINGAN = Tugas Akhir
			MahasiswaRequestTugasAkhir req = null;
			if (mhs != null) {
				req = (MahasiswaRequestTugasAkhir) session.createCriteria(MahasiswaRequestTugasAkhir.class)
						.add(Restrictions.ne("status", MahasiswaRequestTugasAkhir.GAGAL_STATUS))
						.add(Restrictions.eq("mahasiswa", mhs)).setMaxResults(1).uniqueResult();
			}
			if (req == null) {
				req = new MahasiswaRequestTugasAkhir();
			}
			if (mhs != null) {
				req.setMahasiswa(mhs);
			}
			ais.action.master.MahasiswaRequestTugasAkhirAction.onAddExternal(setelahSimpan, req);
		}
	}

	/**
	 * Membuka halaman pendaftaran/pengajuan mahasiswa ({@code zulSrc}) dalam jendela MODAL, lalu memuat
	 * ulang tabel sub-tab Bimbingan saat modal ditutup agar pengajuan baru langsung tampak. Dipakai untuk
	 * KKN &amp; PKL yang alur pendaftarannya student-centric (mahasiswa memilih periode di dalam halaman).
	 */
	private void bukaHalamanPengajuanMhs(final Component host, final Integer jenis, String judulWindow,
			String zulSrc) {
		final MyWindow window = new MyWindow();
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setTitle(judulWindow);
		window.setBorder("normal");
		window.setClosable(true);
		window.setWidth("92%");
		window.setHeight("92%");
		window.setVisible(true);
		MyInclude include = new MyInclude();
		include.setWidth("100%");
		include.setHeight("100%");
		include.setSrc(zulSrc);
		include.setParent(window);
		window.addEventListener("onClose", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				try {
					Common.clear(host);
					renderTabelBimbingan(host, jenis);
				} catch (Exception ex) {
					Common.tampilErrorJikaAdmin(ex);
				}
			}
		});
		try {
			window.onModal();
		} catch (InterruptedException ie) { ais.common.ErrorAuditUtil.record(ie, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:5661");
			// Normal ketika modal ditutup pengguna; abaikan.
		}
	}

	/**
	 * Membangun Criteria bimbingan SENDIRI (tanpa {@code initStaticCriteria}) per jenis + pembatasan
	 * per-peran + SEMUA filter di sisi DB (Fakultas/Prodi/Cari-NIM-Nama/TA/Semester/Cari-Judul) supaya
	 * paging LIMIT/OFFSET akurat. Pemetaan: Tugas Akhir&rarr;MahasiswaRequestTugasAkhir(dosen1..6);
	 * Sidang&rarr;Skripsi(pembimbing/ketuaSidang/pembimbing3/penguji1..5); KKN&rarr;MahasiswaDapatKelompokKkn
	 * (dosen lewat kelompokKkn.dosen_pembimbing1..10); PKL&rarr;MahasiswaDapatKelompokPkl; PA&rarr;KrsMahasiswa
	 * (dosenPa/mahasiswa.dosen). Catatan field: TA (tahunAkademik) hanya ada di Skripsi &amp; KrsMahasiswa;
	 * semester (Integer) ada di TA/Sidang/PA (difilter Ganjil/Genap via daftar ganjil/genap); Judul =
	 * "judul" (TA/Sidang) atau nama kelompok (KKN/PKL). Filter yang field-nya tak ada = dilewati.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Criteria kriteriaBimbinganSendiri(Session session, Integer jenis, Tbmuser tbmuser, Fakultas fakultas,
			Jurusan jurusan, String keyword, String tahunAjaran, String semester, Dosen dosenFilter) {
		Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		Mahasiswa mhs = tbmuser == null ? null : tbmuser.getMahasiswa();
		String kw = (keyword == null || keyword.trim().length() == 0) ? null : keyword.trim();

		Criteria c;
		// Properti "Judul"/nama-kelompok untuk pencarian GABUNGAN NIM/Nama/Judul (null = jenis tanpa judul, mis. PA).
		String judulField = null;
		// Properti dosen pembimbing/penguji untuk filter Dosen (null = KRS/PA yang ditangani khusus).
		String[] dosenFields = null;

		if (SKRIPSI.equals(jenis)) {
			c = session.createCriteria(Skripsi.class).createAlias("mahasiswa", "m").createAlias("m.jurusan", "jur",
					Criteria.LEFT_JOIN);
			dosenFields = new String[] { "pembimbing", "ketuaSidang", "pembimbing3", "penguji1", "penguji2", "penguji3",
					"penguji4", "penguji5" };
			judulField = "judul";
			if (tahunAjaran != null) {
				c.add(Restrictions.eq("tahunAkademik", tahunAjaran));
			}
			tambahFilterSemester(c, semester);
		} else if (KKN.equals(jenis)) {
			c = session.createCriteria(ais.database.model.MahasiswaDapatKelompokKkn.class)
					.createAlias("mahasiswa", "m").createAlias("m.jurusan", "jur", Criteria.LEFT_JOIN)
					.createAlias("kelompokKkn", "kk", Criteria.LEFT_JOIN);
			dosenFields = new String[] { "kk.dosen_pembimbing1", "kk.dosen_pembimbing2", "kk.dosen_pembimbing3",
					"kk.dosen_pembimbing4", "kk.dosen_pembimbing5", "kk.dosen_pembimbing6", "kk.dosen_pembimbing7",
					"kk.dosen_pembimbing8", "kk.dosen_pembimbing9", "kk.dosen_pembimbing10" };
			judulField = "kk.nama";
		} else if (PKL.equals(jenis)) {
			c = session.createCriteria(ais.database.model.MahasiswaDapatKelompokPkl.class)
					.createAlias("mahasiswa", "m").createAlias("m.jurusan", "jur", Criteria.LEFT_JOIN)
					.createAlias("kelompokPkl", "kp", Criteria.LEFT_JOIN);
			dosenFields = new String[] { "kp.dosen_pembimbing1", "kp.dosen_pembimbing2", "kp.dosen_pembimbing3",
					"kp.dosen_pembimbing4", "kp.dosen_pembimbing5", "kp.dosen_pembimbing6", "kp.dosen_pembimbing7",
					"kp.dosen_pembimbing8", "kp.dosen_pembimbing9", "kp.dosen_pembimbing10" };
			judulField = "kp.nama";
		} else if (KRS.equals(jenis)) {
			c = session.createCriteria(KrsMahasiswa.class).createAlias("mahasiswa", "m").createAlias("m.jurusan", "jur",
					Criteria.LEFT_JOIN);
			if (tahunAjaran != null) {
				c.add(Restrictions.eq("tahunAkademik", tahunAjaran));
			}
			tambahFilterSemester(c, semester);
		} else { // BIMBINGAN = Tugas Akhir
			c = session.createCriteria(MahasiswaRequestTugasAkhir.class).createAlias("mahasiswa", "m")
					.createAlias("m.jurusan", "jur", Criteria.LEFT_JOIN);
			dosenFields = new String[] { "dosen1", "dosen2", "dosen3", "dosen4", "dosen5", "dosen6" };
			judulField = "judul";
			tambahFilterSemester(c, semester);
		}

		// Auto-scope peran: dosen yang login hanya melihat bimbingannya; mahasiswa hanya miliknya.
		if (dosen != null) {
			tambahScopeDosen(c, jenis, dosenFields, dosen);
		} else if (mhs != null) {
			c.add(Restrictions.eq("mahasiswa", mhs));
		}

		// Filter "Dosen" (dipilih admin di bar filter): batasi ke bimbingan/penilaian dosen tersebut.
		if (dosenFilter != null) {
			tambahScopeDosen(c, jenis, dosenFields, dosenFilter);
		}

		// MAHASISWA: hanya boleh melihat TA sekarang atau yang lalu (sembunyikan TA yang akan datang).
		if (mhs != null && (SKRIPSI.equals(jenis) || KRS.equals(jenis))) {
			String taSekarang = Common.getCurrentTahunAkademik();
			if (taSekarang != null && taSekarang.trim().length() > 0) {
				c.add(Restrictions.le("tahunAkademik", taSekarang));
			}
		}
		if (fakultas != null) {
			c.add(Restrictions.eq("jur.fakultas", fakultas));
		}
		if (jurusan != null) {
			c.add(Restrictions.eq("m.jurusan", jurusan));
		}
		// Pencarian GABUNGAN: satu kotak mencocokkan NIM/NIS, Nama, ATAU Judul (bila jenis punya judul).
		if (kw != null) {
			// Hibernate lama: Restrictions.or hanya 2-arg -> pakai Disjunction agar bisa 2-3 kondisi.
			org.hibernate.criterion.Disjunction or = Restrictions.disjunction();
			or.add(Restrictions.ilike("m.nim", kw, MatchMode.ANYWHERE));
			or.add(Restrictions.ilike("m.nama", kw, MatchMode.ANYWHERE));
			if (judulField != null) {
				or.add(Restrictions.ilike(judulField, kw, MatchMode.ANYWHERE));
			}
			c.add(or);
		}
		return c;
	}

	/**
	 * Menambahkan pembatasan Criteria ke bimbingan/penilaian yang melibatkan {@code d} sesuai jenis:
	 * KRS/PA &rarr; dosenPa ATAU dosen wali (m.dosen); jenis lain &rarr; OR pada daftar {@code dosenFields}
	 * (pembimbing/penguji). Dipakai untuk auto-scope dosen yang login MAUPUN filter "Dosen" pilihan admin.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void tambahScopeDosen(Criteria c, Integer jenis, String[] dosenFields, Dosen d) {
		if (d == null) {
			return;
		}
		if (KRS.equals(jenis)) {
			c.add(Restrictions.or(Restrictions.eq("dosenPa", d), Restrictions.eq("m.dosen", d.getId())));
		} else if (dosenFields != null) {
			c.add(orEqProps(dosenFields, d));
		}
	}

	/**
	 * Menambahkan filter Semester (Ganjil/Genap) pada properti {@code semester} (Integer) entitas akar
	 * memakai daftar semester ganjil/genap (1..30) supaya tidak perlu modulo SQL. Untuk "Semua"/null tak
	 * menambah apa pun. Hanya dipakai pada entitas yang punya kolom semester (TA/Sidang/PA).
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void tambahFilterSemester(Criteria c, String semester) {
		if (semester == null) {
			return;
		}
		boolean ganjil = Perkuliahan.GANJIL.equalsIgnoreCase(semester);
		boolean genap = Perkuliahan.GENAP.equalsIgnoreCase(semester);
		if (!ganjil && !genap) {
			return;
		}
		java.util.List<Integer> sems = new java.util.ArrayList<Integer>();
		for (int i = 1; i <= 30; i++) {
			if ((i % 2 == 1) == ganjil) {
				sems.add(Integer.valueOf(i));
			}
		}
		c.add(Restrictions.in("semester", sems));
	}

	/**
	 * Memuat isi TABEL bimbingan ke {@code container}: bar tabel LEBAR 100% + PAGING 10 data/halaman yang
	 * meng-query DB per halaman (LIMIT/OFFSET), BUKAN sekadar paging grid. Membangun grid + kolom + komponen
	 * {@link Paging}, menghitung total baris via {@code rowCount}, lalu memuat halaman aktif lewat
	 * {@link #muatHalamanBimbingan}. Semua argumen combo/textbox boleh {@code null} (mahasiswa/siswa: tanpa
	 * filter). Query & filter di {@link #kriteriaBimbinganSendiri}. Sesi ditutup di {@code finally}.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void muatIsiTabelBimbingan(final Component container, final Integer jenis, Combobox taCombo,
			Combobox smtCombo, Combobox fakCombo, Combobox jurCombo, ais.ui.util.MyTextbox cariMhsBox,
			ais.action.master.helper.AmbilDataDosenBanbox dosenBox) {

		final String tahunAjaran = (taCombo == null || taCombo.getSelectedItem() == null
				|| taCombo.getSelectedItem().getValue() == null) ? null : (String) taCombo.getSelectedItem().getValue();
		final String semester = (smtCombo == null || smtCombo.getSelectedItem() == null
				|| smtCombo.getSelectedItem().getValue() == null) ? null
						: (String) smtCombo.getSelectedItem().getValue();
		final Fakultas fakultas = (fakCombo == null || fakCombo.getSelectedItem() == null
				|| fakCombo.getSelectedItem().getValue() == null) ? null
						: (Fakultas) fakCombo.getSelectedItem().getValue();
		final Jurusan jurusan = (jurCombo == null || jurCombo.getSelectedItem() == null
				|| jurCombo.getSelectedItem().getValue() == null) ? null
						: (Jurusan) jurCombo.getSelectedItem().getValue();
		String kwTmp = (cariMhsBox == null || cariMhsBox.getValue() == null) ? null : cariMhsBox.getValue().trim();
		if (kwTmp != null && kwTmp.isEmpty()) {
			kwTmp = null;
		}
		final String keyword = kwTmp;
		// Dosen terpilih di banbox (null bila kosong / "=Dosen="): filter tambahan ke bimbingan dosen tsb.
		Dosen dosenTmp = null;
		if (dosenBox != null) {
			String dv = dosenBox.getValue();
			Object da = dosenBox.getAttribute("dosen");
			if (da instanceof Dosen && dv != null && dv.trim().length() > 0 && !"=Dosen=".equals(dv.trim())) {
				dosenTmp = (Dosen) da;
			}
		}
		final Dosen dosenFilter = dosenTmp;

		Grid grid = new Grid();
		grid.setParent(container);
		grid.setWidth("100%");
		grid.setStyle("background:#ffffff; border:1px solid #e2e8f0; border-radius:10px;");

		Columns columns = new Columns();
		columns.setParent(grid);
		// Foto/NIM/TA/Aksi lebar tetap; Nama & Judul TANPA lebar agar melebar mengisi 100% (fixed layout).
		String[] heads = { "Foto", "NIM/NIS", "Nama", "Judul", "TA/Smt", "" };
		String[] widths = { "64px", "130px", null, null, "150px", "205px" };
		String[] aligns = { "center", "left", "left", "left", "center", "center" };
		for (int i = 0; i < heads.length; i++) {
			org.zkoss.zul.Column col = new org.zkoss.zul.Column(heads[i]);
			if (widths[i] != null) {
				col.setWidth(widths[i]);
			}
			col.setAlign(aligns[i]);
			col.setStyle("font-weight:bold; background:#f8fafc; color:#334155; padding:9px 10px;");
			col.setParent(columns);
		}

		final Rows rows = new Rows();
		rows.setParent(grid);

		final Paging paging = new Paging();
		paging.setPageSize(10);
		paging.setMold("os");
		paging.setDetailed(true);
		paging.setStyle("margin-top:8px;");
		paging.setParent(container);

		int totalCnt = 0;
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			Tbmuser tbmuser = Common.getCurrentUser();
			Number n = (Number) kriteriaBimbinganSendiri(session, jenis, tbmuser, fakultas, jurusan, keyword,
					tahunAjaran, semester, dosenFilter).setProjection(org.hibernate.criterion.Projections.rowCount())
							.uniqueResult();
			totalCnt = n == null ? 0 : n.intValue();
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.disconnect();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:5895");
				}
				closeHibernateSessionQuietly(session);
			}
			HibernateUtil.closeSession();
		}

		paging.setTotalSize(totalCnt);
		paging.setVisible(totalCnt > 10);

		paging.addEventListener("onPaging", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				muatHalamanBimbingan(rows, jenis, fakultas, jurusan, keyword, tahunAjaran, semester, dosenFilter,
						paging.getActivePage());
			}
		});

		muatHalamanBimbingan(rows, jenis, fakultas, jurusan, keyword, tahunAjaran, semester, dosenFilter, 0);

		if (totalCnt == 0) {
			Label kosong = new Label(ais.common.Common.getBahasaConfig("Belum ada data bimbingan untuk ditampilkan."));
			kosong.setStyle("color:#64748b; padding:12px; display:block;");
			kosong.setParent(container);
		}
	}

	/**
	 * Memuat SATU halaman (10 baris) tabel bimbingan ke {@code rows} langsung dari DB via LIMIT/OFFSET
	 * ({@code setFirstResult(page*10).setMaxResults(10)}). Membersihkan baris lama lebih dulu. Baris =
	 * per-mahasiswa; untuk KKN/PKL objek aksi (Agenda/Nilai) = kelompoknya. Sesi ditutup di {@code finally}.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void muatHalamanBimbingan(final Rows rows, final Integer jenis, Fakultas fakultas, Jurusan jurusan,
			String keyword, String tahunAjaran, String semester, Dosen dosenFilter, int page) {
		Common.clear(rows);
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			Tbmuser tbmuser = Common.getCurrentUser();
			Criteria criteria = kriteriaBimbinganSendiri(session, jenis, tbmuser, fakultas, jurusan, keyword,
					tahunAjaran, semester, dosenFilter);
			criteria.setFirstResult(page * 10);
			criteria.setMaxResults(10);
			List list = criteria.list();
			for (Object o : list) {
				Mahasiswa mhs = null;
				String judul = "";
				VOPembelajaran voAksi = null;

				if (o instanceof MahasiswaRequestTugasAkhir) {
					MahasiswaRequestTugasAkhir x = (MahasiswaRequestTugasAkhir) o;
					mhs = x.getMahasiswa();
					judul = x.getJudul();
					voAksi = x;
				} else if (o instanceof Skripsi) {
					Skripsi x = (Skripsi) o;
					mhs = x.getMahasiswa();
					judul = x.getJudul();
					voAksi = x;
				} else if (o instanceof KrsMahasiswa) {
					mhs = ((KrsMahasiswa) o).getMahasiswa();
					voAksi = (KrsMahasiswa) o;
				} else if (o instanceof ais.database.model.MahasiswaDapatKelompokKkn) {
					ais.database.model.MahasiswaDapatKelompokKkn x = (ais.database.model.MahasiswaDapatKelompokKkn) o;
					mhs = x.getMahasiswa();
					KelompokKkn kk = x.getKelompokKkn();
					if (kk != null) {
						judul = kk.getNama();
						voAksi = kk;
					}
				} else if (o instanceof ais.database.model.MahasiswaDapatKelompokPkl) {
					ais.database.model.MahasiswaDapatKelompokPkl x = (ais.database.model.MahasiswaDapatKelompokPkl) o;
					mhs = x.getMahasiswa();
					KelompokPkl kp = x.getKelompokPkl();
					if (kp != null) {
						judul = kp.getNama();
						voAksi = kp;
					}
				} else if (o instanceof VOPembelajaran) {
					voAksi = (VOPembelajaran) o;
				}

				String taSmt = "";
				if (voAksi != null) {
					String taNilai = "";
					String smtNilai = "";
					try {
						taNilai = voAksi.ambilTahunAjaran();
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:5984");
					}
					try {
						smtNilai = voAksi.ambilJenisSemester();
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:5988");
					}
					taSmt = ((taNilai == null ? "" : taNilai) + " / " + (smtNilai == null ? "" : smtNilai)).trim();
					if (taSmt.equals("/")) {
						taSmt = "";
					}
				}

				String nimTampil = "-";
				String namaTampil = "";
				if (mhs != null) {
					try {
						nimTampil = mhs.getNim();
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:6001");
					}
					try {
						namaTampil = mhs.getNama();
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:6005");
					}
				}

				Row row = new Row();
				row.setValign("middle");
				row.setStyle("border-bottom:1px solid #eef2f7;");
				row.setParent(rows);

				try {
					if (mhs != null) {
						row.appendChild(ais.common.ProfileImageUtil.tampilkanGambarKecil(mhs, "40px", "center"));
					} else {
						Label ikon = new Label("");
						ikon.setStyle("display:inline-block;width:40px;height:40px;border-radius:50%;background:#e2e8f0;");
						row.appendChild(ikon);
					}
				} catch (Exception ig) {
					row.appendChild(new Label(""));
				}

				Label labNim = new Label(nimTampil == null ? "-" : nimTampil);
				labNim.setStyle("white-space:nowrap; padding:9px 10px; color:#475569;");
				row.appendChild(labNim);

				Label labNama = new Label(namaTampil == null ? "" : namaTampil);
				labNama.setStyle("font-weight:600; padding:9px 10px; color:#0f172a;");
				row.appendChild(labNama);

				Label labJudul = new Label(judul == null ? "" : judul);
				labJudul.setStyle("padding:9px 10px; color:#475569;");
				row.appendChild(labJudul);

				Label labTa = new Label(taSmt);
				labTa.setStyle("white-space:nowrap; padding:9px 10px; color:#475569;");
				row.appendChild(labTa);

				final VOPembelajaran voKlik = voAksi;
				org.zkoss.zul.Hlayout aksi = new org.zkoss.zul.Hlayout();
				aksi.setSpacing("8px");
				aksi.setStyle("padding:6px 8px;");
				MyToolbarbuttonConfig btnAgenda = new MyToolbarbuttonConfig("Agenda", "/img/svg/calendar-check.svg");
				btnAgenda.setStyle("font-size:12px; white-space:nowrap;");
				btnAgenda.setTooltiptext("Lihat agenda pertemuan/konsultasi");
				btnAgenda.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						if (voKlik != null) {
							prosess(voKlik, false);
						}
					}
				});
				aksi.appendChild(btnAgenda);
				// Label tombol: khusus PA (KRS) = "Transkrip" (buka transkrip akademik); jenis lain = "Nilai".
				String labelAksiNilai = KRS.equals(jenis) ? "Transkrip" : "Nilai";
				MyToolbarbuttonConfig btnNilai = new MyToolbarbuttonConfig(labelAksiNilai,
						"/img/svg/journal-check.svg");
				btnNilai.setStyle("font-size:12px; white-space:nowrap;");
				btnNilai.setTooltiptext(KRS.equals(jenis) ? "Lihat transkrip akademik" : "Input / lihat penilaian");
				btnNilai.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						if (voKlik != null) {
							bukaNilaiBimbingan(voKlik, jenis);
						}
					}
				});
				aksi.appendChild(btnNilai);
				row.appendChild(aksi);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.disconnect();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:6081");
				}
				closeHibernateSessionQuietly(session);
			}
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Membuka form PENILAIAN sesuai jenis bimbingan dalam jendela modal, memakai helper penilaian yang
	 * sudah ada: Sidang&rarr;{@code PenilaianSkripsiHelper}, Tugas Akhir&rarr;{@code PenilaianProposalSkripsiHelper},
	 * KKN&rarr;{@code PenilaianKknHelper}, PKL&rarr;{@code PenilaianPklHelper}. Untuk PA (KRS) yang tidak
	 * memiliki form penilaian tersendiri, dibuka rincian pertemuan lewat {@link #prosess(VOPembelajaran, boolean)}.
	 */
	private void bukaNilaiBimbingan(VOPembelajaran vo, Integer jenis) throws Exception {
		if (vo == null) {
			return;
		}
		if (vo instanceof KrsMahasiswa) {
			// PA (Pembimbing Akademik): tampilkan TRANSKRIP AKADEMIK mahasiswa langsung dalam modal.
			ais.database.model.Mahasiswa mhsPa = ((KrsMahasiswa) vo).getMahasiswa();
			if (mhsPa == null) {
				prosess(vo, false);
				return;
			}
			Window wTr = new Window("Transkrip Akademik", "normal", true);
			wTr.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			wTr.setWidth("95%");
			wTr.setHeight("95%");
			wTr.setStyle("background:#f8fafc;");
			Borderlayout blTr = new Borderlayout();
			blTr.setHeight("100%");
			blTr.setParent(wTr);
			Center cTr = new Center();
			cTr.setAutoscroll(true);
			cTr.setStyle("padding:8px; background:transparent; border:none;");
			ais.ui.util.ZkCompat.setFlex(cTr, true);
			cTr.setParent(blTr);
			ais.action.report.format1.akademik.LaporanTranskipAkademik transkrip = new ais.action.report.format1.akademik.LaporanTranskipAkademik(
					mhsPa);
			transkrip.setWidth("100%");
			transkrip.setHeight("100%");
			transkrip.setParent(cTr);
			wTr.onModal();
			return;
		}
		if (!(vo instanceof Skripsi || vo instanceof MahasiswaRequestTugasAkhir || vo instanceof KelompokKkn
				|| vo instanceof KelompokPkl)) {
			// Jenis lain tanpa form penilaian khusus: buka rincian pertemuan.
			prosess(vo, false);
			return;
		}

		Window window = new Window("Penilaian", "normal", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setWidth("95%");
		window.setHeight("95%");
		window.setStyle("background:#f8fafc;");

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setAutoscroll(true);
		center.setStyle("background:transparent;border:none;padding:10px;");
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		if (vo instanceof Skripsi) {
			new ais.action.master.helper.PenilaianSkripsiHelper().display((Skripsi) vo, center, null);
		} else if (vo instanceof MahasiswaRequestTugasAkhir) {
			// Samakan dengan Penilaian di menu Skripsi: bila Tugas Akhir sudah punya Skripsi terkait,
			// pakai PenilaianSkripsiHelper (tampilan identik menu Skripsi). Bila belum ada Skripsi, jatuh ke
			// penilaian proposal/seminar (PenilaianProposalSkripsiHelper).
			Skripsi skTa = ((MahasiswaRequestTugasAkhir) vo).ambilSkripsi();
			if (skTa != null) {
				new ais.action.master.helper.PenilaianSkripsiHelper().display(skTa, center, null);
			} else {
				new ais.action.master.helper.PenilaianProposalSkripsiHelper().display((MahasiswaRequestTugasAkhir) vo,
						center);
			}
		} else if (vo instanceof KelompokKkn) {
			new ais.action.master.helper.PenilaianKknHelper().display((KelompokKkn) vo, center);
		} else if (vo instanceof KelompokPkl) {
			new ais.action.master.helper.PenilaianPklHelper().display((KelompokPkl) vo, center);
		}

		window.onModal();
	}

	private void loadMenu(Component parent, final Integer jenis, final JenisFormulirKegiatan jenisFormulirKegiatan) {

		// Lepas indikator loading awal panel kiri (bila masih terpasang) TEPAT sebelum membangun
		// form/daftar — agar placeholder loading tergantikan MULUS oleh konten (tidak menumpuk di
		// atasnya). Aman & idempoten: pemanggil hanya memicu loadMenu saat tab kosong/hanya
		// placeholder (guard hanyaPlaceholderLoading()), jadi ini tak menghapus konten nyata.
		if (parent != null) {
			Common.clear(parent);
		}

		Borderlayout subBorderlayoutUtama = new Borderlayout();
		subBorderlayoutUtama.setParent(parent);

		final Combobox yay = new Combobox();
		final Combobox sek = new Combobox();

		final Combobox fak = new Combobox();
		final Combobox jur = new Combobox();
		final Combobox prog = new Combobox();

		final Combobox ta = new Combobox();
		final Combobox smt = new Combobox();
		final Combobox hari = new Combobox();

		final MyCheckboxConfig remedial = new MyCheckboxConfig("Remedial");
		final MyCheckboxConfig paralel = new MyCheckboxConfig("Paralel");
		final MyCheckboxConfig pra = new MyCheckboxConfig("Pra.Perkuliahan");
		final MyCheckboxConfig ekstra = new MyCheckboxConfig("Ekstrakurikuler");

		final MyCheckboxConfig requestStatus = new MyCheckboxConfig(MahasiswaRequestTugasAkhir.REQUEST_STATUS);
		final MyCheckboxConfig aktifStatus = new MyCheckboxConfig(MahasiswaRequestTugasAkhir.AKTIF_STATUS);
		final MyCheckboxConfig seminarStatus = new MyCheckboxConfig(MahasiswaRequestTugasAkhir.SEMINAR_STATUS);
		final MyCheckboxConfig lulusStatus = new MyCheckboxConfig(MahasiswaRequestTugasAkhir.LULUS_STATUS);
		final MyCheckboxConfig mengulangStatus = new MyCheckboxConfig(MahasiswaRequestTugasAkhir.MENGULANG_STATUS);
		final MyCheckboxConfig gagalStatus = new MyCheckboxConfig(MahasiswaRequestTugasAkhir.GAGAL_STATUS);

		requestStatus.setChecked(true);
		aktifStatus.setChecked(true);
		seminarStatus.setChecked(true);
		lulusStatus.setChecked(true);
		mengulangStatus.setChecked(true);
		gagalStatus.setChecked(true);

		final MyCheckboxConfig belumStatus = new MyCheckboxConfig("Blm disetujui");
		final MyCheckboxConfig setujuStatus = new MyCheckboxConfig("Disetujui");
		final MyCheckboxConfig sidangStatus = new MyCheckboxConfig("Telah Sidang");

		belumStatus.setChecked(true);
		setujuStatus.setChecked(true);
		sidangStatus.setChecked(true);

		for (String h : Common.haris) {
			MyComboitemConfigKecil comboitem = new MyComboitemConfigKecil();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari.appendChild(comboitem);
		}
		MyComboitemConfigKecil comboitem = new MyComboitemConfigKecil();
		comboitem.setLabel("=hari=");
		comboitem.setValue(null);
		hari.appendChild(comboitem);
		hari.setReadonly(true);
		hari.setSelectedItem(comboitem);

		comboitem = new MyComboitemConfigKecil();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		smt.appendChild(comboitem);

		comboitem = new MyComboitemConfigKecil();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		smt.appendChild(comboitem);

		if (jenis.equals(PERKULIAHAN) || jenis.equals(KRS)) {
			comboitem = new MyComboitemConfigKecil();
			comboitem.setLabel(Perkuliahan.SP);
			comboitem.setValue(Perkuliahan.SP);
			smt.appendChild(comboitem);
		}

		comboitem = new MyComboitemConfigKecil();
		comboitem.setLabel("=smt=");
		comboitem.setValue(null);
		smt.appendChild(comboitem);
		smt.setReadonly(true);

		if (jenis.equals(KRS)) {
			Common.selectComboItem(smt, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		} else {
			smt.setSelectedItem(comboitem);
		}
		Common.generateTahunAjaranDanSemua(ta);
		Common.selectComboItem(ta, jenis.equals(KRS) ? Common.getCurrentTahunAkademik() : null);

		// ===== PERCEPAT LOADING e-Learning: DEFAULT Tahun Akademik = TA BERJALAN =====
		// Panel "Perkuliahan & Kelas" (PERKULIAHAN) & "Pelajaran" (PELAJARAN) SELALU default ke Tahun
		// Akademik BERJALAN — bukan "Semua"/null — supaya query pemuatan awal hanya memindai 1 tahun
		// akademik (jauh lebih cepat untuk data yang banyak). Sebelumnya bergantung konfigurasi
		// "tampilkanPilihanTaDiPerkuliakan" (default TIDAK AKTIF) sehingga default = "Semua" = lambat.
		// Pengguna tetap bisa memilih "Semua" secara manual dari combo TA yang kini ada di TOOLBAR.
		final boolean taDiToolbar = jenis.equals(PERKULIAHAN) || jenis.equals(PELAJARAN);
		if (taDiToolbar) {
			String taBerjalanElearning = Common.getCurrentTahunAkademik();
			if (taBerjalanElearning != null && taBerjalanElearning.trim().length() > 0) {
				Common.selectComboItem(ta, taBerjalanElearning);
			}
		}

		fak.setVisible(jenis.equals(PERKULIAHAN) || jenis.equals(SKRIPSI) || jenis.equals(BIMBINGAN)
				|| jenis.equals(KKN) || jenis.equals(PKL) || jenis.equals(KRS) || jenis.equals(KEGIATAN)
				|| jenis.equals(KONSULTASI));
		jur.setVisible(fak.isVisible());
		prog.setVisible(fak.isVisible());

		yay.setVisible(!fak.isVisible());
		sek.setVisible(!fak.isVisible());

		final Rows rows = new Rows();
		final Textbox cari = new Textbox();
		final Textbox kelas = new Textbox();
		// Kotak filter "Pengajar" (dosen untuk Perkuliahan / guru untuk Pelajaran) — dibuat baru tiap
		// popup Pencarian dirender, dibaca oleh terapkanFilterPengajar() saat menyusun kriteria.
		pengajar = new Textbox();
		final Toolbar hboxPencarian = new Toolbar();

		// Penanda Tahun Akademik & Semester yang SEDANG ditampilkan di panel kiri "Perkuliahan & Kelas".
		// Daftar perkuliahan/kelas tersaring oleh TA & Semester yang dipilih pada dialog Pencarian
		// (inilah sebab "sebagian data tidak muncul"). Label ini ditaruh di toolbar Pencarian/Refresh dan
		// diperbarui setiap kali data dimuat / dicari / di-refresh, membaca nilai terbaru dari combo
		// ta (Tahun Akademik) & smt (Semester), agar pengguna sadar sedang menampilkan TA/Semester berapa.
		final ais.ui.util.MyLabelConfig labelTaAktif = new ais.ui.util.MyLabelConfig("");
		labelTaAktif.setStyle("margin-left:10px;font-weight:bold;color:#3a3a7a;white-space:nowrap;");
		final EventListener refreshLabelTaAktif = new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				String taVal = ta.getSelectedItem() == null || ta.getSelectedItem().getValue() == null ? null
						: ta.getSelectedItem().getValue().toString();
				String smtVal = smt.getSelectedItem() == null || smt.getSelectedItem().getValue() == null ? null
						: smt.getSelectedItem().getValue().toString();
				labelTaAktif.setValue("TA: " + (taVal == null || taVal.trim().isEmpty() ? "Semua" : taVal.trim())
						+ (smtVal == null || smtVal.trim().isEmpty() ? "" : " / " + smtVal.trim()));
			}
		};

		final Paging paging = new Paging();
		paging.setHeight("30px");

		final EventListener pagingEvent = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(cari.getValue().trim(), jenis, rows, paging, fak, jur, prog, yay, sek, ta, smt, hari, remedial,
						pra, ekstra, paralel, requestStatus, aktifStatus, seminarStatus, lulusStatus, gagalStatus,
						belumStatus, setujuStatus, mengulangStatus, sidangStatus, this, kelas.getValue().trim(),
						!(arg0 != null && arg0.getName().equalsIgnoreCase("paging")), jenisFormulirKegiatan);
			}
		};

		Common.initPagingCustom(paging, pagingEvent, jumlahDataDalamSatuHalamanElearning);

		final MyWindow myWindowPencarian = new MyWindow("Filter Lanjutan", "none", true);
		myWindowPencarian.setHeight("90%");
		myWindowPencarian.setWidth(Common.isMobile() ? "99%" : "400px");

		Borderlayout borderlayoutencarian = new Borderlayout();
		borderlayoutencarian.setParent(myWindowPencarian);

		Center center = new Center();
		center.setParent(borderlayoutencarian);
		ais.ui.util.ZkCompat.setFlex(center, true);
		Grid gridcari = new Grid();
		gridcari.setWidth("100%");
		gridcari.setParent(center);
		gridcari.setWidth("100%");
		gridcari.setHeight("100%");

		Columns columnscari = new Columns();
		columnscari.setParent(gridcari);

		MyColumnConfig columncari = new MyColumnConfig();
		columncari.setParent(columnscari);

		Rows rowscari = new Rows();

		rowscari.setParent(gridcari);

		MyFormRow rowcari = new MyFormRow();
		// Untuk Perkuliahan/Pelajaran, kotak "Kata Kunci" DIPINDAH ke TOOLBAR panel kiri (lihat blok
		// "CARI MATA KULIAH LANGSUNG DI TOOLBAR") supaya pencarian mata kuliah tidak perlu membuka popup
		// Pencarian. Karena satu komponen hanya boleh punya satu induk, di sini tidak ditambahkan lagi.
		if (!taDiToolbar) {
			rowcari.setParent(rowscari);
			rowcari.appendChild(new MyLabelBoldConfig("Kata Kunci"));

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(cari);
			cari.setWidth("90%");
		}

		if (jenis.equals(PERKULIAHAN) && !taDiToolbar) {
			Common.initKeteranganSatuKolom(rowscari, "* Kata kunci bisa berupa kode/nama matakuliah atau nama dosen");
		} else if (jenis.equals(SKRIPSI)) {
			Common.initKeteranganSatuKolom(rowscari,
					"* Kata kunci bisa berupa nim/nama mahasiswa atau judul/keyword skripsi/tugas akhir");
		} else if (jenis.equals(BIMBINGAN)) {
			Common.initKeteranganSatuKolom(rowscari,
					"* Kata kunci bisa berupa nim/nama mahasiswa atau judul/keyword skripsi/tugas akhir");
		} else if (jenis.equals(KKN)) {
			Common.initKeteranganSatuKolom(rowscari,
					"* Kata kunci bisa berupa nama dosen pembimbing atau nama kelompok KKN");
		} else if (jenis.equals(PKL)) {
			Common.initKeteranganSatuKolom(rowscari,
					"* Kata kunci bisa berupa nama dosen pembimbing atau nama kelompok PKL");
		} else if (jenis.equals(KRS)) {
			Common.initKeteranganSatuKolom(rowscari,
					"* Kata kunci bisa berupa nim/nama mahasiswa, nama dosen pembimbing, atau catatan KRS/KHS");
		} else if (jenis.equals(KEGIATAN)) {
			Common.initKeteranganSatuKolom(rowscari, "* Kata kunci bisa berupa nama kegiatan");
		} else if (jenis.equals(PELAJARAN) && !taDiToolbar) {
			Common.initKeteranganSatuKolom(rowscari, "* Kata kunci bisa berupa nis/nama siswa");
		} else if (jenis.equals(KONSULTASI)) {
			Common.initKeteranganSatuKolom(rowscari,
					"* Kata kunci bisa berupa nama dosen pembimbing atau nama konsultasi");
		} else if (jenis.equals(WISUDA)) {
			Common.initKeteranganSatuKolom(rowscari, "* Kata kunci bisa berupa motto atau keterangan wisuda");
		}
		if (jenis.equals(PERKULIAHAN) || jenis.equals(PELAJARAN) || jenis.equals(BIMBINGAN) || jenis.equals(SKRIPSI)
				|| jenis.equals(KRS)) {
			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(new MyLabelBoldConfig("Kelas"));

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(kelas);
			kelas.setWidth("90%");
		}

		// Filter "Pengajar": mencari dosen (Perkuliahan) atau guru (Pelajaran) berdasarkan nama.
		if (jenis.equals(PERKULIAHAN) || jenis.equals(PELAJARAN)) {
			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(new MyLabelBoldConfig("Pengajar"));

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(pengajar);
			pengajar.setWidth("90%");

			Common.initKeteranganSatuKolom(rowscari, jenis.equals(PELAJARAN)
					? "* Ketik nama guru pengajar untuk menyaring jadwal pelajaran"
					: "* Ketik nama dosen pengampu untuk menyaring kelas perkuliahan");
		}

		if (fak.isVisible()) {
			Common.initPrograms(prog);
			Common.initFakultasDanJurusanDanSemua(fak, jur, null, null);

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(new MyLabelBoldConfig("Fakultas"));

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(fak);
			fak.setWidth("90%");

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(new MyLabelBoldConfig("Jurusan"));

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(jur);
			jur.setWidth("90%");

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(new MyLabelBoldConfig("Program"));

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(prog);
			prog.setWidth("90%");
		}

		if (!jenis.equals(WISUDA)) {
			// TA (Tahun Akademik) untuk Perkuliahan/Pelajaran DIPINDAH ke toolbar panel kiri (lihat taDiToolbar)
			// → tidak ditampilkan lagi di dalam popup Pencarian agar tidak ganda. Semester tetap di popup.
			if (!taDiToolbar) {
				rowcari = new MyFormRow();
				rowcari.setParent(rowscari);
				rowcari.appendChild(new MyLabelBoldConfig("Tahun Akademik"));

				rowcari = new MyFormRow();
				rowcari.setParent(rowscari);
				rowcari.appendChild(ta);
				ta.setWidth("90%");
			}

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(new MyLabelBoldConfig("Semester"));

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(smt);
			smt.setWidth("90%");
		}

		// Filter "Hari" DIHAPUS dari popup Pencarian (permintaan STIKSAM) — digantikan kotak pencarian
		// "Mata Kuliah" yang kini ada LANGSUNG di toolbar panel kiri. Combo `hari` tetap dibuat (tidak
		// di-parent) agar loadData() tetap menerima nilainya = default "Semua" (tanpa penyaringan hari).

		if (!fak.isVisible() && !jenis.equals(WISUDA)) {
			Common.initYayasanDanSekolahDanSemua(yay, sek, null, null);

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(new MyLabelBoldConfig("Yayasan"));

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(yay);
			yay.setWidth("90%");

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(new MyLabelBoldConfig("Sekolah"));

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(sek);
			sek.setWidth("90%");

		} else if (jenis.equals(PERKULIAHAN)) {

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(remedial);

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(paralel);

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(ekstra);

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(pra);

		}

		else if (jenis.equals(BIMBINGAN) && mahasiswa == null) {

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(requestStatus);

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(aktifStatus);

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(seminarStatus);

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(lulusStatus);

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(gagalStatus);

		}

		else if (jenis.equals(SKRIPSI) && mahasiswa == null) {

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(belumStatus);

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(setujuStatus);

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(sidangStatus);

		}

		final EventListener eventListenerLoadData = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(cari.getValue().trim(), jenis, rows, paging, fak, jur, prog, yay, sek, ta, smt, hari, remedial,
						pra, ekstra, paralel, requestStatus, aktifStatus, seminarStatus, lulusStatus, gagalStatus,
						belumStatus, setujuStatus, mengulangStatus, sidangStatus, pagingEvent, kelas.getValue().trim(),
						true, jenisFormulirKegiatan);
				refreshLabelTaAktif.onEvent(event); // perbarui penanda TA/Semester yang sedang ditampilkan
			}
		};

		// Simpan listener pemuat-ulang DATA panel kiri (tab yang sedang dibangun) agar bisa dipicu
		// ULANG (self-heal) dari jalur ANDAL (menuKanan ON_OPEN) bila inner timer loadData dari
		// pemuatan EAGER saat compose tak fire. Lihat jalankanLoadAwalElearning() cabang early-return.
		reloadDataKiri = eventListenerLoadData;

		South southPencarian = new South();
		ais.ui.util.ZkCompat.setFlex(southPencarian, true);
		southPencarian.setParent(borderlayoutencarian);

		// Tombol Batal & Cari dirapikan seperti button group Bootstrap: seukuran, sejajar, dan
		// flex-wrap (kelas .ais-btn-group sudah ada di css_utama.css). Pakai Hbox + sclass.
		Hbox toolbarPencarian = new Hbox();
		toolbarPencarian.setSclass("ais-btn-group");
		toolbarPencarian.setWidth("100%");
		toolbarPencarian.setParent(southPencarian);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				myWindowPencarian.setVisible(false);
			}
		});
		cancel.setParent(toolbarPencarian);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Cari", "/img/search.png");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				myWindowPencarian.setVisible(false);
				eventListenerLoadData.onEvent(event);
			}
		});
		save.setParent(toolbarPencarian);

		cari.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				myWindowPencarian.setVisible(false);
				eventListenerLoadData.onEvent(event);
			}
		});

		kelas.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				myWindowPencarian.setVisible(false);
				eventListenerLoadData.onEvent(event);
			}
		});

		// Label "Lanjut" (bukan "Pencarian"): pencarian utama kini sudah ada di toolbar (kotak "Mata
		// Kuliah" + tombol Cari), tombol ini hanya untuk MENYARING LEBIH LANJUT lewat popup.
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Lanjut", "/img/search.png");
		toolbarbutton.setTooltiptext("Filter lanjutan (Tahun Akademik, Semester, Prodi, dll)");
		toolbarbutton.setParent(hboxPencarian);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				/*
				 * Buka ulang panel Pencarian dengan render BERSIH.
				 * Masalah: MyWindow.detach() hanya meng-hide (pakaiClose=true) dan
				 * window ini dibuat sekali lalu dipakai ulang. Pada klik ke-2,
				 * toggle setVisible(false)->true + onModal() pada widget yang sama
				 * membuat modal + Borderlayout(flex) gagal mengukur ulang sehingga
				 * lebar window menciut jadi separuh.
				 * Solusi: detach sungguhan via setParent(null) (mem-bypass override
				 * detach yang cuma hide) lalu attach ulang -> ZK merender window dari
				 * awal seperti klik pertama. Nilai filter tetap karena state komponen
				 * anak dipertahankan saat re-attach.
				 */
				if (myWindowPencarian.getParent() != null) {
					myWindowPencarian.setParent(null);
				}
				myWindowPencarian.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				myWindowPencarian.setWidth(Common.isMobile() ? "99%" : "400px");
				myWindowPencarian.setHeight("90%");
				myWindowPencarian.setVisible(true);
				myWindowPencarian.onModal();

			}
		});

		if (jenis.equals(BIMBINGAN) && tbmuser != null && tbmuser.ambilDosen() == null) {

			toolbarbutton = new MyToolbarbuttonConfig("Ajukan", "/img/Document-Write-icon.png");
			toolbarbutton.setVisible(tbmuser != null && tbmuser.ambilDosen() == null);
			hboxPencarian.appendChild(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = new MahasiswaRequestTugasAkhir();
					MahasiswaRequestTugasAkhirAction.onAddExternal(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) arg0
									.getData();
							Mahasiswa mahasiswa = mahasiswaRequestTugasAkhir.getMahasiswa();
							MyMessageboxConfig.show(
									MyMessageboxConfig.format(
											"Mahasiswa dengan NIM {V1} atas nama {V2} telah berhasil mengajukan Tugas Akhir dengan judul:\n\n{V3}",
											mahasiswa.getNim(), mahasiswa.getNama(),
											(mahasiswaRequestTugasAkhir.getJudul().isEmpty()
													? mahasiswaRequestTugasAkhir.getJudul1()
													: mahasiswaRequestTugasAkhir.getJudul())),
									"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
									new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											loadData(cari.getValue().trim(), jenis, rows, paging, fak, jur, prog, yay,
													sek, ta, smt, hari, remedial, pra, ekstra, paralel, requestStatus,
													aktifStatus, seminarStatus, lulusStatus, gagalStatus, belumStatus,
													setujuStatus, mengulangStatus, sidangStatus, pagingEvent,
													kelas.getValue().trim(), true, jenisFormulirKegiatan);
										}
									});

						}
					}, mahasiswaRequestTugasAkhir);

				}
			});
		}

		if (jenis.equals(SKRIPSI) && tbmuser != null && tbmuser.ambilDosen() == null) {

			toolbarbutton = new MyToolbarbuttonConfig("Ajukan", "/img/Document-Write-icon.png");
			toolbarbutton.setVisible(tbmuser != null && tbmuser.ambilDosen() == null);
			hboxPencarian.appendChild(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Skripsi skripsi = new Skripsi();
					Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
					skripsi.setMahasiswa(mahasiswa);
					SkripsiAction.onAddExternal(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Skripsi skripsi = (Skripsi) arg0.getData();
							Mahasiswa mahasiswa = skripsi.getMahasiswa();
							MyMessageboxConfig.show(
									MyMessageboxConfig.format(
											"Mahasiswa dengan NIM {V1} atas nama {V2} telah berhasil mengajukan sidang dengan judul:\n\n{V3}",
											mahasiswa.getNim(), mahasiswa.getNama(), skripsi.getJudul()),
									"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
									new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											loadData(cari.getValue().trim(), jenis, rows, paging, fak, jur, prog, yay,
													sek, ta, smt, hari, remedial, pra, ekstra, paralel, requestStatus,
													aktifStatus, seminarStatus, lulusStatus, gagalStatus, belumStatus,
													setujuStatus, mengulangStatus, sidangStatus, pagingEvent,
													kelas.getValue().trim(), true, jenisFormulirKegiatan);
										}
									});

						}
					}, skripsi, mahasiswa);

				}
			});
		}

		if (jenis.equals(WISUDA) && tbmuser != null) {

			toolbarbutton = new MyToolbarbuttonConfig("Ajukan", "/img/Document-Write-icon.png");
			hboxPencarian.appendChild(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyWindow laporan = new MyWindow();
					laporan.setTitle("Pengajuan Kegiatan");
					laporan.setClosable(true);
					laporan.setHeight("95%");
					laporan.setWidth("90%");

					Borderlayout borderlayout = new Borderlayout();
					borderlayout.setParent(laporan);
					Center center = new Center();
					center.setParent(borderlayout);
					if (tbmuser.getMahasiswa() != null) {
						center.appendChild(new MyInclude("/pages/master/pendaftaran_wisuda_mahasiswa.zul?mahasiswa="
								+ (tbmuser.getMahasiswa() == null ? "-1" : tbmuser.getMahasiswa().getId())));
					} else {
						center.appendChild(new MyInclude("/pages/master/pendaftaran_wisuda_mahasiswa.zul"));
					}

					laporan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					laporan.onModal();

					laporan.addEventListener("onClose", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(cari.getValue().trim(), jenis, rows, paging, fak, jur, prog, yay, sek, ta, smt,
									hari, remedial, pra, ekstra, paralel, requestStatus, aktifStatus, seminarStatus,
									lulusStatus, gagalStatus, belumStatus, setujuStatus, mengulangStatus, sidangStatus,
									pagingEvent, kelas.getValue().trim(), true, jenisFormulirKegiatan);
						}
					});

				}
			});
		}

		if (jenis.equals(KEGIATAN) && tbmuser != null) {

			toolbarbutton = new MyToolbarbuttonConfig("Ajukan", "/img/Document-Write-icon.png");
			hboxPencarian.appendChild(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					LaporanPendidikanLingkunganKampus laporan = new LaporanPendidikanLingkunganKampus(
							jenisFormulirKegiatan);
					laporan.setTitle("Pengajuan Kegiatan");
					laporan.setClosable(true);
					laporan.setHeight("95%");
					laporan.setWidth("90%");
					laporan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					laporan.onModal();

					laporan.addEventListener("onClose", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(cari.getValue().trim(), jenis, rows, paging, fak, jur, prog, yay, sek, ta, smt,
									hari, remedial, pra, ekstra, paralel, requestStatus, aktifStatus, seminarStatus,
									lulusStatus, gagalStatus, belumStatus, setujuStatus, mengulangStatus, sidangStatus,
									pagingEvent, kelas.getValue().trim(), true, jenisFormulirKegiatan);
						}
					});

				}
			});
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(cari.getValue().trim(), jenis, rows, paging, fak, jur, prog, yay, sek, ta, smt, hari, remedial,
						pra, ekstra, paralel, true, requestStatus, aktifStatus, seminarStatus, lulusStatus, gagalStatus,
						belumStatus, setujuStatus, mengulangStatus, sidangStatus, pagingEvent, kelas.getValue().trim(),
						true, jenisFormulirKegiatan);
				refreshLabelTaAktif.onEvent(arg0); // perbarui penanda TA/Semester yang sedang ditampilkan
			}
		});

		button.setParent(hboxPencarian);

		// ===== TA DIPINDAH KE TOOLBAR (Perkuliahan/Pelajaran) =====
		// Combo Tahun Akademik tampil langsung di toolbar panel kiri (samping tombol Pencarian & Refresh),
		// tidak lagi tersembunyi di dalam popup Pencarian. Default = TA berjalan (lihat blok "PERCEPAT
		// LOADING" di atas). Mengubah pilihan langsung MEMUAT ULANG daftar sesuai TA terpilih.
		if (taDiToolbar) {
			// ===== BARIS FILTER (TA + Cari Mata Kuliah) =====
			// Dibungkus SATU wadah flex agar RAPI: menempati barisnya sendiri di bawah tombol
			// Pencarian/Refresh, jarak antar-kontrol seragam, semua rata tengah secara vertikal, dan
			// membungkus dengan rapi saat panel menyempit (sebelumnya kontrol ditempel langsung ke
			// Toolbar sehingga berdesakan & turun baris tidak beraturan).
			// Tata letak GRID 2 KOLOM (label | kontrol): panel kiri sempit, sehingga tata letak mengalir
			// (flex-wrap) membuat label & kontrol patah ke baris masing-masing = terlihat menumpuk/berantakan.
			// Dengan grid, label selalu di kolom-1 dan kontrol MEMENUHI kolom-2, jadi sejajar rapi dan
			// otomatis menyesuaikan lebar panel berapa pun.
			org.zkoss.zul.Div barisFilter = new org.zkoss.zul.Div();
			barisFilter.setStyle("display:grid;grid-template-columns:auto 1fr;gap:5px 7px;align-items:center;"
					+ "width:100%;box-sizing:border-box;padding:6px 7px 4px 7px;margin-top:3px;"
					+ "border-top:1px solid #eef2f7;");
			barisFilter.setParent(hboxPencarian);

			final String gayaLabelFilter = "font-weight:600;font-size:11px;color:#334155;white-space:nowrap;";
			final String gayaKotakFilter = "border:1px solid #cbd5e1;border-radius:6px;font-size:11px;";

			// — Tahun Akademik —
			ais.ui.util.MyLabelConfig lblTaToolbar = new ais.ui.util.MyLabelConfig("TA:");
			lblTaToolbar.setStyle(gayaLabelFilter);
			lblTaToolbar.setParent(barisFilter);
			ta.setWidth("100%");
			ta.setParent(barisFilter);
			ta.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event evTa) throws Exception {
					// muat ulang dari halaman pertama sesuai filter terkini (termasuk TA baru)
					pagingEvent.onEvent(evTa);
					refreshLabelTaAktif.onEvent(evTa);
				}
			});

			// — Cari Mata Kuliah (pengganti filter "Hari") —
			// Permintaan STIKSAM: pengguna TIDAK perlu membuka popup "Pencarian"/"Lanjutan" hanya untuk
			// mencari mata kuliah. Kotak kata kunci (kode/nama matakuliah atau nama dosen — dipakai
			// loadData lewat cari.getValue()) ditaruh di sini. Tekan ENTER atau pindah fokus = mencari.
			final boolean pelajaranMode = jenis.equals(PELAJARAN);
			ais.ui.util.MyLabelConfig lblCariMk = new ais.ui.util.MyLabelConfig(
					pelajaranMode ? "Mata Pelajaran:" : "Mata Kuliah:");
			lblCariMk.setStyle(gayaLabelFilter);
			lblCariMk.setParent(barisFilter);
			// Kotak cari + tombol Cari menempati SATU sel grid (kolom-2) agar tetap SEBARIS: kotak
			// melar mengisi sisa ruang, tombol menempel di kanannya.
			org.zkoss.zul.Div selCari = new org.zkoss.zul.Div();
			selCari.setStyle("display:flex;align-items:center;gap:5px;width:100%;min-width:0;");
			selCari.setParent(barisFilter);
			cari.setWidth("100%");
			cari.setStyle(gayaKotakFilter + "padding:3px 7px;flex:1;min-width:0;box-sizing:border-box;");
			cari.setTooltiptext(pelajaranMode ? "Ketik nama mata pelajaran / guru lalu tekan Enter"
					: "Ketik nama atau kode mata kuliah / nama dosen lalu tekan Enter");
			cari.setParent(selCari);
			EventListener cariMkEvent = new EventListener() {
				@Override
				public void onEvent(Event evCari) throws Exception {
					pagingEvent.onEvent(evCari);
					refreshLabelTaAktif.onEvent(evCari);
				}
			};
			cari.addEventListener("onOK", cariMkEvent);
			cari.addEventListener("onChange", cariMkEvent);

			MyToolbarbuttonConfig btnCariMk = new MyToolbarbuttonConfig("Cari", "/img/search.png");
			btnCariMk.setStyle(gayaKotakFilter + "background:#f8fafc;color:#1d4ed8;font-weight:600;"
					+ "padding:2px 10px;cursor:pointer;");
			btnCariMk.setTooltiptext("Cari sesuai kata kunci mata kuliah di sebelah kiri");
			btnCariMk.addEventListener("onClick", cariMkEvent);
			btnCariMk.setParent(selCari);
		}

		// Penanda TA/Semester aktif di toolbar. Bila combo TA sudah dipindah ke toolbar (taDiToolbar),
		// chip ini REDUNDAN (TA sudah tampil pada combo) → JANGAN ditampilkan. Untuk jenis lain tetap tampil.
		if (!taDiToolbar) {
			labelTaAktif.setParent(hboxPencarian);
			try {
				refreshLabelTaAktif.onEvent(null);
			} catch (Exception eLabel) { ais.common.ErrorAuditUtil.record(eLabel, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:6869");
			}
		}

		North subnorth = new North();
		subnorth.setParent(subBorderlayoutUtama);
		ais.ui.util.ZkCompat.setFlex(subnorth, true);
		subnorth.setBorder("none");
		subnorth.appendChild(hboxPencarian);

		Center subcenter = new Center();
		subcenter.setParent(subBorderlayoutUtama);
		ais.ui.util.ZkCompat.setFlex(subcenter, true);
		subcenter.setBorder("none");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(subcenter);
		grid.setWidth("100%");
		grid.setHeight("100%");

		rows.setParent(grid);

		/*
		 * Muat data PERTAMA dengan memanggil loadData SECARA LANGSUNG (BUKAN lewat Timer).
		 *
		 * MASALAH SEBELUMNYA: pemuatan awal dibungkus Common.createDefaultTimerNoBusy(...). Halaman
		 * e-Learning ini di-INCLUDE ke tab MainAction; Timer yang DIBUAT SAAT panel disusun dari
		 * trigger awal (include masih dirender) KERAP TIDAK TER-FIRE di klien -> daftar "Perkuliahan
		 * & Kelas" tampil KOSONG dan baru terisi setelah user MENGUBAH combo (yang menjalankan
		 * loadMenu di konteks AU normal sehingga Timer-nya fire). Bandingkan: panel kanan (menuKanan)
		 * membuat Timer-nya pada event ON_OPEN (setelah render) -> fire normal.
		 *
		 * SOLUSI: loadData sendiri SUDAH asinkron (membuat progress panel + background thread +
		 * server-push untuk render hasil) -> tidak memblokir walau dipanggil langsung. Dengan
		 * memanggilnya langsung (dikombinasikan echoEvent pada pemilihan menu awal, lihat loadMenu()),
		 * pemuatan tidak lagi bergantung pada Timer bersarang yang tak selalu fire. refresh=true
		 * (identik tombol Refresh) agar koleksi lazy Mahasiswa/Dosen di-reInit lebih dulu.
		 */
		loadData(cari.getValue().trim(), jenis, rows, paging, fak, jur, prog, yay, sek, ta, smt, hari, remedial,
				pra, ekstra, paralel, true, requestStatus, aktifStatus, seminarStatus, lulusStatus, gagalStatus,
				belumStatus, setujuStatus, mengulangStatus, sidangStatus, pagingEvent, kelas.getValue().trim(),
				true, jenisFormulirKegiatan);
	}

	@SuppressWarnings({})
	public void loadData(String keyword, Integer jenis, Rows rows, Paging paging, Combobox fak, Combobox jur,
			Combobox prog, Combobox yay, Combobox sek, Combobox ta, Combobox smt, Combobox hari,
			MyCheckboxConfig remedial, MyCheckboxConfig pra, MyCheckboxConfig ekstra, MyCheckboxConfig paralel,
			MyCheckboxConfig requestStatus, MyCheckboxConfig aktifStatus, MyCheckboxConfig seminarStatus,
			MyCheckboxConfig lulusStatus, MyCheckboxConfig gagalStatus, MyCheckboxConfig belumStatus,
			MyCheckboxConfig setujuStatus, MyCheckboxConfig mengulangStatus, MyCheckboxConfig sidangStatus,
			EventListener pagingEvent, String kelas, boolean pagingLoad, JenisFormulirKegiatan jenisFormulirKegiatan) {
		loadData(keyword, jenis, rows, paging, fak, jur, prog, yay, sek, ta, smt, hari, remedial, pra, ekstra, paralel,
				false, requestStatus, aktifStatus, seminarStatus, lulusStatus, gagalStatus, belumStatus, setujuStatus,
				mengulangStatus, sidangStatus, pagingEvent, kelas, pagingLoad, jenisFormulirKegiatan);
	}

	private Integer size = 0;
	private String tAlama = "";

	private Div createRowsProgressPanel(Rows rows, String title, String description, int percent) {
		if (rows == null) {
			return null;
		}
		MyFormRow progressRow = new MyFormRow();
		progressRow.setValign("top");
		ais.ui.util.ZkCompat.setSpans(progressRow, "20");
		progressRow.setStyle("border:0;background:transparent;");
		progressRow.setParent(rows);
		Div progress = MainProgressHelper.createProgressPanel(title, description, percent);
		progress.setParent(progressRow);
		progress.setAttribute("progressRow", progressRow);
		return progress;
	}

	private Label createProgressLabel(final Div progress, String defaultMessage, final int defaultPercent) {
		MainProgressHelper.updateProgressPanel(progress, defaultMessage, defaultPercent);
		return new Label(defaultMessage == null ? "" : defaultMessage) {
			private static final long serialVersionUID = 1L;

			public void setValue(String value) {
				super.setValue(value);
				if (value == null || value.trim().isEmpty() || value.toLowerCase().indexOf("selesai") >= 0) {
					MainProgressHelper.finishProgressPanel(progress, "Selesai", true);
					detachRowsProgressPanel(progress);
				} else {
					MainProgressHelper.updateProgressPanel(progress, value, extractPercent(value, defaultPercent));
				}
			}
		};
	}

	private int extractPercent(String value, int fallback) {
		if (value == null) {
			return fallback;
		}
		try {
			int percentIndex = value.indexOf("%");
			if (percentIndex < 0) {
				return fallback;
			}
			int start = percentIndex - 1;
			while (start >= 0) {
				char c = value.charAt(start);
				if ((c >= '0' && c <= '9') || c == '.' || c == ',') {
					start--;
				} else {
					break;
				}
			}
			String number = value.substring(start + 1, percentIndex).replace(',', '.').trim();
			return (int) Math.round(Double.parseDouble(number));
		} catch (Exception e) {
			return fallback;
		}
	}

	private void detachRowsProgressPanel(Div progress) {
		if (progress == null) {
			return;
		}
		try {
			Object row = progress.getAttribute("progressRow");
			if (row instanceof Component) {
				((Component) row).detach();
			} else {
				progress.detach();
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	private String getJenisTitle(Integer jenis) {
		if (PERKULIAHAN.equals(jenis)) {
			return "Perkuliahan";
		} else if (SKRIPSI.equals(jenis)) {
			return "Skripsi";
		} else if (BIMBINGAN.equals(jenis)) {
			return "Bimbingan";
		} else if (KKN.equals(jenis)) {
			return "KKN";
		} else if (PKL.equals(jenis)) {
			return "PKL";
		} else if (KRS.equals(jenis)) {
			return "KRS";
		} else if (KONSULTASI.equals(jenis)) {
			return "Konsultasi";
		} else if (PELAJARAN.equals(jenis)) {
			return "Pelajaran";
		} else if (KEGIATAN.equals(jenis)) {
			return "Kegiatan";
		} else if (WISUDA.equals(jenis)) {
			return "Wisuda";
		}
		return "Pembelajaran";
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void loadData(final String keyword, final Integer jenis, final Rows rows, final Paging paging,
			final Combobox fak, final Combobox jur, final Combobox prog, final Combobox yay, final Combobox sek,
			final Combobox ta, final Combobox smt, final Combobox hari, final MyCheckboxConfig remedial,
			final MyCheckboxConfig pra, final MyCheckboxConfig ekstra, final MyCheckboxConfig paralel,
			final boolean refresh, final MyCheckboxConfig requestStatus, final MyCheckboxConfig aktifStatus,
			final MyCheckboxConfig seminarStatus, final MyCheckboxConfig lulusStatus,
			final MyCheckboxConfig gagalStatus, final MyCheckboxConfig belumStatus, final MyCheckboxConfig setujuStatus,
			final MyCheckboxConfig mengulangStatus, final MyCheckboxConfig sidangStatus,
			final EventListener pagingEvent, final String kelas, boolean pagingLoad,
			final JenisFormulirKegiatan jenisFormulirKegiatan) {

		if (refresh || pagingLoad) {
			tAlama = "";
		}

		final Fakultas fakultas = (Fakultas) (fak.getSelectedItem() == null ? null : fak.getSelectedItem().getValue());
		final Jurusan jurusan = (Jurusan) (jur.getSelectedItem() == null ? null : jur.getSelectedItem().getValue());
		final String program = (String) (prog.getSelectedItem() == null ? null : prog.getSelectedItem().getValue());

		final Yayasan yayasan = (Yayasan) (yay.getSelectedItem() == null ? null : yay.getSelectedItem().getValue());
		final Sekolah sekolah = (Sekolah) (sek.getSelectedItem() == null ? null : sek.getSelectedItem().getValue());

		final String tahunAkademik = (String) (ta.getSelectedItem() == null || ta.getSelectedItem().getValue() == null
				? null
				: ta.getSelectedItem().getValue());
		final String jenisSemester = smt.getSelectedItem() == null || smt.getSelectedItem().getValue() == null ? null
				: smt.getSelectedItem().getValue().toString();

		final String hr = hari.getSelectedItem() == null || hari.getSelectedItem().getValue() == null ? null
				: hari.getSelectedItem().getValue().toString();

		final Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		final Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		// Siswa: dipakai untuk menampilkan PKL siswa di e-learning (seperti PKL mahasiswa).
		final ais.database.model.sekolah.Siswa siswaLogin = tbmuser == null ? null : tbmuser.getSiswa();

		if (pagingLoad) {
			if (rows != null) {
				Common.clear(rows);
			}
		}
		paging.setVisible(false);
		final List<Map> data = new ArrayList<Map>();
		final Div progressPanel = createRowsProgressPanel(rows, "Memuat " + getJenisTitle(jenis),
				"Menyiapkan filter dan daftar pembelajaran", 8);
		// Gambar loading (SAMA seperti panel kanan "Tugas, Ujian, Materi & Diskusi") — ditampilkan
		// LEBIH DULU sebelum data selesai dimuat, agar panel tidak terlihat kosong saat proses.
		if (progressPanel != null) {
			try {
				ais.ui.util.MyHtml loadingImg = new ais.ui.util.MyHtml("<div style='margin:6px auto;width:34px;height:34px;border:3px solid #e2e8f0;border-top-color:#2563eb;border-radius:50%;animation:aisSpin .8s linear infinite;'></div>");
				loadingImg.setParent(progressPanel);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:7080");
			}
		}
		// Pastikan progress bar panel kiri LANGSUNG TERLIHAT (bukan di container yang kolaps hingga
		// window di-resize): picu event 'resize' agar tata letak ZK dihitung ulang begitu indikator
		// loading dipasang — bukan hanya setelah data selesai (loadDoneListener). Aman & idempoten.
		picuResizeTataLetakElearning();
		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses mengambil data... 0%"));
		final EventListener loadDoneListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				// Tandai bahwa pemuatan DATA panel kiri BENAR-BENAR berjalan (inner timer loadData
				// fire & fetch selesai, kini sedang me-render hasil). Dipakai self-heal di
				// jalankanLoadAwalElearning(): selama flag ini masih false padahal form kiri sudah
				// tampil, pemuatan data akan dipicu ulang dari jalur andal (menuKanan ON_OPEN).
				dataKiriTampil = true;

				// ── PERBAIKAN "muncul setelah di-resize" ──
				// Gejala: data panel kiri sudah ada di DOM, tetapi region West / tabbox-nya masih
				// KOLAPS (tinggi 0) sampai jendela browser di-resize manual, baru tampil. Sebab:
				// tata letak ZK (Borderlayout/region) dihitung ulang saat render awal & saat event
				// 'resize'. Skrip pas-tinggi (pasangAutoFitTinggiPortal) menjalankan fit() pada
				// 250/900/2000 ms; padahal DATA daftar baru selesai dimuat SETELAH itu (rantai
				// timer) sehingga tak ada lagi pemicu hitung-ulang → panel tampak kosong. Solusi:
				// setelah DATA benar-benar ter-render, PICU event 'resize' dari klien (fit() sudah
				// terikat ke window.resize + ZK ikut menghitung ulang region) sehingga tinggi
				// terkoreksi otomatis TANPA pengguna perlu me-resize. Dijeda 80/450 ms agar baris
				// selesai dilukis dulu; dibungkus try/catch + fallback createEvent utk browser lama.
				try {
					org.zkoss.zk.ui.util.Clients.evalJavaScript(
							"try{var _r=function(){try{var e;if(typeof(Event)==='function'){e=new Event('resize');}"
									+ "else{e=document.createEvent('Event');e.initEvent('resize',true,true);}"
									+ "window.dispatchEvent(e);if(window.zk&&zk.Widget){}}catch(x){}};"
									+ "setTimeout(_r,80);setTimeout(_r,450);}catch(x){}");
				} catch (Exception eResize) { ais.common.ErrorAuditUtil.record(eResize, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:7116");
					// abaikan: koreksi tinggi hanya optimasi tampilan, jangan ganggu render data.
				}

				// loadDoneListener bisa dipanggil dari fallback thread tanpa desktop push aktif.
				// Bila komponen paging sudah detach (desktop null), paging.setPageSize() memicu
				// Events.postEvent NPE. Sentuh properti paging hanya saat masih terpasang di desktop.
				if (paging != null && paging.getDesktop() != null) {
					paging.setPageSize(jumlahDataDalamSatuHalamanElearning);
					paging.setPageIncrement(Common.isMobile() ? 4 : 6);
					paging.setMold("os");
					paging.setTotalSize(size);
					paging.setVisible(size > jumlahDataDalamSatuHalamanElearning);
					paging.setDetailed(false);
				}

				Row row;
				for (Map map : data) {

					VOPembelajaran voPembelajaran = (VOPembelajaran) map.get("perkuliahan");
					Object[] jml = (Object[]) map.get("jml");
					Integer mhsSize = (Integer) map.get("mhsSize");
					String tabName = (String) map.get("tabName");
					final String a = (String) map.get("ta");
					if (!tAlama.equalsIgnoreCase(a)) {
						tAlama = a;
						if (!(voPembelajaran instanceof Wisuda)) {
							Group group = new ais.ui.util.MyGroupConfig(a);
							group.setParent(rows);
						}
					}

					row = new MyFormRow();
					row.setStyle("border:0px;background: transparent;font-size: x-small;");
					row.setParent(rows);
					TampilanELearningAction.tampilkanStatistik(voPembelajaran, jml, mhsSize, tabName, row);
				}

				data.clear();

				if (tabpanelTimelineSiapDiisi()) {
					// REGRESI: dulu memakai `menu instanceof Center` untuk membedakan MOBILE (buat timeline
					// timeline saat tab Linimasa diklik) vs DESKTOP (buat EAGER). Sejak layout portal desktop
					// diubah agar region KIRI juga `Center` (hindari Borderlayout 1-region malformed/kolaps),
					// `menu` DESKTOP pun ber-tipe Center -> cabang mobile SELALU terpilih -> timeline DITUNDA
					// ke klik tab Linimasa yang (karena terpilih default) TAK PERNAH terjadi -> panel TENGAH
					// & KANAN blank sampai Refresh. Pakai flag sebenarnya `mobileTampilan` (di-set di init
					// dari isMobileView()) agar desktop kembali membuat timeline EAGER seperti versi lama.
					if (mobileTampilan) {
						btnAktivitas.onSetiapKlikPanel((Div) tabpanelTimeline, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (tabpanelTimelineSiapDiisi()) {
									bersihkanLoadingTengah();
									tabpanelTimeline
											.appendChild(dashboardTimelinePertemuan = new DashboardTimelinePertemuan(
													new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															reloadBlnSd = true;
															eventListenerMateri.onEvent(arg0);
														}
													}));
								}
							}
						});

					} else {

						// FASE 1 (kerangka instan): timeline TENGAH dibangun di AU EVENT TERPISAH (defer via
						// createDefaultTimerNoBusy) — BUKAN sinkron di dalam loadDoneListener panel KIRI. Tujuan:
						// event-thread TIDAK tertahan membangun timeline (initSpreadsheet) tepat saat kerangka baru
						// dirender, sehingga progress "Menyiapkan tampilan..." tidak membeku; panel tengah terisi
						// sesaat kemudian di event-nya sendiri. Poll 500ms di bawah tetap menunggu pertemuansa siap
						// (aman thd dashboardTimelinePertemuan yang sementara null) + fallback 20 dtk. Idempoten
						// via getChildren().isEmpty().
						Common.createDefaultTimerNoBusy(new EventListener() {
							@Override
							public void onEvent(Event evTimeline) throws Exception {
								if (tabpanelTimeline != null && tabpanelTimelineSiapDiisi()) {
									bersihkanLoadingTengah();
									tabpanelTimeline.appendChild(
											dashboardTimelinePertemuan = new DashboardTimelinePertemuan(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													reloadBlnSd = true;
													eventListenerMateri.onEvent(arg0);
												}
											}));
								}
							}
						});

						System.out.println("init dashboard");

						if (eventListenerMateri != null) {

							final Timer timer = new Timer(500);
							timer.setRepeats(true);
							timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

							final int[] percobaanKanan = { 0 };
							timer.addEventListener("onTimer", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									percobaanKanan[0]++;
									boolean pertemuanSiap = dashboardTimelinePertemuan != null
											&& dashboardTimelinePertemuan.pertemuansa != null;
									// FALLBACK anti-gantung (samakan dgn jalur mobile ~1035): setelah ~20 detik
									// (40 x 500ms) picu panel kanan WALAU pertemuansa center belum siap, agar
									// "Ambil data..." tidak berputar selamanya bila timeline lambat/gagal muat.
									// loadDataKanan kini aman terhadap pertemuansa kosong/null, dan callback
									// timeline tetap me-reload panel kanan saat data benar-benar siap.
									if (pertemuanSiap || percobaanKanan[0] >= 40) {
										if (eventListenerMateri != null) {
											eventListenerMateri.onEvent(null);
										}
										timer.stop();
										timer.detach();
									}

								}
							});
							timer.start();
						}

					}
				}

				if (paging != null && paging.isVisible()
						&& paging.getTotalSize() > (paging.getActivePage() * jumlahDataDalamSatuHalamanElearning)) {
					row = new MyFormRow();
					rows.appendChild(row);

					MyToolbarbuttonConfig a = new MyToolbarbuttonConfig("Tampilkan data selanjutnya.. ("
							+ (paging.getTotalSize() - (paging.getActivePage() * jumlahDataDalamSatuHalamanElearning))
							+ " data)", "/img/Button-Next-icon.png");
					a.setStyle("font-size:11px;");
					row.appendChild(a);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							arg0.getTarget().getParent().setVisible(false);

							try {
								paging.setActivePage(paging.getActivePage() + 1);
								pagingEvent.onEvent(new Event("paging"));
							} catch (Exception e) {
								MyMessageboxConfig.show("Mohon maaf, tidak terdapat data selanjutnya yang dapat ditampilkan. Bapak/Ibu telah berada pada halaman terakhir dari data yang tersedia saat ini.", "Informasi",
										MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
						}
					});
				}
			}
		};

		// 1. Ambil desktop SEBELUM thread dimulai (di main event thread)
		final org.zkoss.zk.ui.Desktop desktop = org.zkoss.zk.ui.Executions.getCurrent().getDesktop();

		// 2. WAJIB: Aktifkan Server Push jika belum aktif agar UI bisa di-update dari
		// background thread
		boolean pushBaruDinyalakan = false;
		if (desktop != null && !desktop.isServerPushEnabled()) {
			desktop.enableServerPush(true);
			pushBaruDinyalakan = true;
		}
		final boolean pushDinyalakanDiSini = pushBaruDinyalakan;

		// 3. Set label awal DI LUAR background thread agar aman dari NullPointerException.
		//    Pesan DESKRIPTIF (biar pengguna tahu SEDANG memuat apa saat data banyak): fase ini memuat daftar
		//    perkuliahan/kelas milik pengguna. Panel tengah (linimasa) & kanan (tugas/ujian/materi) punya
		//    indikator "Memuat ..." sendiri yang muncul saat masing-masing di-render.
		label.setValue("Memuat daftar perkuliahan & kelas...");
		MainProgressHelper.updateProgressPanel(progressPanel, "Memuat daftar perkuliahan & kelas...", 20);

		Common.createDefaultTimerNoBusy(new EventListener() {

			@Override
			public void onEvent(Event evLoadSync) throws Exception {
				try {
				List<? extends VOPembelajaran> voPembelajarans = null;
				// int size = 0;

				if (mahasiswa != null) {
					if (refresh) {
						try {
							Session session = HibernateUtil.currentNativeSession();

							if (jenis.equals(PERKULIAHAN)) {
								mahasiswa.reInitDetailperkuliahan(session);
							} else if (jenis.equals(SKRIPSI)) {
								mahasiswa.reInitSkripsi(session);
							} else if (jenis.equals(BIMBINGAN)) {
								mahasiswa.reInitBimbingan(session);
							} else if (jenis.equals(KKN)) {
								mahasiswa.reInitKkn(session);
							} else if (jenis.equals(PKL)) {
								mahasiswa.reInitPkl(session);
							} else if (jenis.equals(KRS)) {
								mahasiswa.reInitKrs(session);
							} else if (jenis.equals(KEGIATAN)) {
								mahasiswa.reInitFormulirKegiatanPeserta(session);
							} else if (jenis.equals(WISUDA)) {
								mahasiswa.reInitPendaftaranWisuda(session);
							} else if (jenis.equals(KONSULTASI)) {
								mahasiswa.reInitKonsultasi(session);
							}

							if (session.isOpen()) {
								session.disconnect();
								closeHibernateSessionQuietly(session);
							}
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
						HibernateUtil.closeSession();
					}

					Object[] objects = mahasiswa.ambilPerkuliahanDanParalel(tahunAkademik, jenisSemester, hr, keyword,
							kelas, dicek(pra), dicek(ekstra) ? Perkuliahan.EKSTRA : null, true,
							dicek(remedial), dicek(paralel), jenis,
							jumlahDataDalamSatuHalamanElearning * (paging == null ? 0 : paging.getActivePage()),
							jumlahDataDalamSatuHalamanElearning, jenisFormulirKegiatan);
					voPembelajarans = (List<VOPembelajaran>) objects[0];
					size = (Integer) objects[1];

				} else if (dosen != null) {

					if (refresh) {
						try {
							Session session = HibernateUtil.currentNativeSession();
							if (jenis.equals(PERKULIAHAN)) {
								dosen.reInitPerkuliahan(session);
							} else if (jenis.equals(SKRIPSI)) {
								dosen.reInitSkripsi(session);
							} else if (jenis.equals(BIMBINGAN)) {
								dosen.reInitBimbingan(session);
							} else if (jenis.equals(KKN)) {
								dosen.reInitKkn(session);
							} else if (jenis.equals(PKL)) {
								dosen.reInitPkl(session);
							} else if (jenis.equals(KRS)) {
								dosen.reInitKrs(session);
							} else if (jenis.equals(KEGIATAN)) {
								dosen.reInitFormulirKegiatanPeserta(session);
							} else if (jenis.equals(KONSULTASI)) {
								dosen.reInitKonsultasi(session);
							}

							if (session.isOpen()) {
								session.disconnect();
								closeHibernateSessionQuietly(session);
							}
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
						HibernateUtil.closeSession();
					}

					Session session = HibernateUtil.currentNativeSession();

					Object[] objects = dosen.ambilPerkuliahanDanParalel(session, tahunAkademik, jenisSemester, hr,
							keyword, kelas, dicek(pra), dicek(ekstra) ? Perkuliahan.EKSTRA : null, true,
							dicek(remedial), dicek(paralel),

							dicek(requestStatus), dicek(aktifStatus), dicek(seminarStatus),
							dicek(mengulangStatus), dicek(lulusStatus), dicek(gagalStatus),
							dicek(belumStatus), dicek(setujuStatus), dicek(sidangStatus),

							jenis,

							jumlahDataDalamSatuHalamanElearning * (paging == null ? 0 : paging.getActivePage()),
							jumlahDataDalamSatuHalamanElearning, jenisFormulirKegiatan);
					voPembelajarans = (List<VOPembelajaran>) objects[0];
					size = (Integer) objects[1];

					if (session.isOpen()) {
						session.disconnect();
						closeHibernateSessionQuietly(session);
					}
					HibernateUtil.closeSession();
				} else if (siswaLogin != null && PKL.equals(jenis)) {
					// === SISWA + PKL: tampilkan HANYA kelompok PKL yang diikuti siswa ini ===
					// (memakai ulang KelompokPkl sebagai VOPembelajaran, seperti PKL mahasiswa).
					Session sessionPkl = HibernateUtil.currentNativeSession();
					try {
						List<KelompokPkl> pkls = ambilKelompokPklMilikSiswa(sessionPkl, siswaLogin, keyword);
						size = pkls.size();
						int off = jumlahDataDalamSatuHalamanElearning * (paging == null ? 0 : paging.getActivePage());
						int end = Math.min(off + jumlahDataDalamSatuHalamanElearning, pkls.size());
						voPembelajarans = off >= pkls.size() ? new ArrayList<KelompokPkl>()
								: new ArrayList<KelompokPkl>(pkls.subList(off, end));
					} catch (Exception ePkl) {
						ais.common.Common.tampilErrorJikaAdmin(ePkl);
						voPembelajarans = new ArrayList<KelompokPkl>();
						size = 0;
					} finally {
						if (sessionPkl != null && sessionPkl.isOpen()) {
							sessionPkl.disconnect();
							closeHibernateSessionQuietly(sessionPkl);
						}
						HibernateUtil.closeSession();
					}
				} else {
					// ── L1 cache admin path: load full list once, paginate in-memory ────────
					java.util.List _full = null;
					String _tk = (tbmuser == null ? "0" : String.valueOf(tbmuser.getId()))
							+ "|" + jenis
							+ "|" + (keyword == null ? "" : keyword.trim())
							+ "|" + (kelas == null ? "" : kelas.trim())
							+ "|" + (tahunAkademik == null ? "" : tahunAkademik)
							+ "|" + (jenisSemester == null ? "" : jenisSemester)
							+ "|" + (hr == null ? "" : hr)
							+ "|" + (fakultas == null ? "" : fakultas.getId())
							+ "|" + (jurusan == null ? "" : jurusan.getId())
							+ "|" + (program == null ? "" : program)
							+ "|" + (yayasan == null ? "" : yayasan.getId())
							+ "|" + (sekolah == null ? "" : sekolah.getId())
							+ "|" + dicek(remedial) + "|" + dicek(pra) + "|" + dicek(ekstra)
							+ "|" + dicek(paralel) + "|" + dicek(requestStatus) + "|" + dicek(aktifStatus)
							+ "|" + dicek(seminarStatus) + "|" + dicek(lulusStatus) + "|" + dicek(gagalStatus)
							+ "|" + dicek(belumStatus) + "|" + dicek(setujuStatus) + "|" + dicek(mengulangStatus)
							+ "|" + dicek(sidangStatus)
							+ "|" + (jenisFormulirKegiatan == null ? "" : jenisFormulirKegiatan.getId())
							// Sertakan filter "Pengajar" pada kunci cache agar hasil tidak tertukar
							// antar pencarian dengan nama pengajar berbeda.
							+ "|" + (pengajar == null || pengajar.getValue() == null ? "" : pengajar.getValue().trim());
					Long _te = TELA_EXPIRY.get(_tk);
					if (!refresh && _te != null && _te > System.currentTimeMillis()) {
						_full = TELA_CACHE.get(_tk);
					}
					if (_full == null) {
						Session session = null;
						try {
						session = HibernateUtil.openSession();
						if (jenis.equals(PERKULIAHAN)) {
							_full = ConstantValues.simpleList(
									initCriteria(true, jenis, keyword, fakultas, jurusan, program, yayasan, sekolah,
											ta, smt, hari, remedial, pra, ekstra, paralel, requestStatus,
											aktifStatus, seminarStatus, lulusStatus, gagalStatus, belumStatus,
											setujuStatus, mengulangStatus, sidangStatus, kelas, session),
									Perkuliahan.class);
						} else if (jenis.equals(SKRIPSI)) {
							_full = ConstantValues.simpleList(
									initCriteria(true, jenis, keyword, fakultas, jurusan, program, yayasan, sekolah,
											ta, smt, hari, remedial, pra, ekstra, paralel, requestStatus,
											aktifStatus, seminarStatus, lulusStatus, gagalStatus, belumStatus,
											setujuStatus, mengulangStatus, sidangStatus, kelas, session),
									Skripsi.class);
						} else if (jenis.equals(BIMBINGAN)) {
							_full = ConstantValues.simpleList(
									initCriteria(true, jenis, keyword, fakultas, jurusan, program, yayasan, sekolah, ta,
											smt, hari, remedial, pra, ekstra, paralel, requestStatus, aktifStatus,
											seminarStatus, lulusStatus, gagalStatus, belumStatus, setujuStatus,
											mengulangStatus, sidangStatus, kelas, session),
									MahasiswaRequestTugasAkhir.class);
						} else if (jenis.equals(KKN)) {
							_full = ConstantValues.simpleList(
									initCriteria(true, jenis, keyword, fakultas, jurusan, program, yayasan, sekolah,
											ta, smt, hari, remedial, pra, ekstra, paralel, requestStatus,
											aktifStatus, seminarStatus, lulusStatus, gagalStatus, belumStatus,
											setujuStatus, mengulangStatus, sidangStatus, kelas, session),
									KelompokKkn.class);
						} else if (jenis.equals(PKL)) {
							_full = ConstantValues.simpleList(
									initCriteria(true, jenis, keyword, fakultas, jurusan, program, yayasan, sekolah,
											ta, smt, hari, remedial, pra, ekstra, paralel, requestStatus,
											aktifStatus, seminarStatus, lulusStatus, gagalStatus, belumStatus,
											setujuStatus, mengulangStatus, sidangStatus, kelas, session),
									KelompokPkl.class);
						} else if (jenis.equals(KEGIATAN)) {
							_full = ConstantValues.simpleList(
									initCriteria(true, jenis, keyword, fakultas, jurusan, program, yayasan, sekolah, ta,
											smt, hari, remedial, pra, ekstra, paralel, requestStatus, aktifStatus,
											seminarStatus, lulusStatus, gagalStatus, belumStatus, setujuStatus,
											mengulangStatus, sidangStatus, kelas, jenisFormulirKegiatan, session),
									FormulirKegiatan.class);
						} else if (jenis.equals(WISUDA)) {
							_full = ConstantValues.simpleList(
									initCriteria(true, jenis, keyword, fakultas, jurusan, program, yayasan, sekolah, ta,
											smt, hari, remedial, pra, ekstra, paralel, requestStatus, aktifStatus,
											seminarStatus, lulusStatus, gagalStatus, belumStatus, setujuStatus,
											mengulangStatus, sidangStatus, kelas, session),
									Wisuda.class);
						} else if (jenis.equals(KRS)) {
							_full = ConstantValues.simpleList(
									initCriteria(true, jenis, keyword, fakultas, jurusan, program, yayasan, sekolah, ta,
											smt, hari, remedial, pra, ekstra, paralel, requestStatus, aktifStatus,
											seminarStatus, lulusStatus, gagalStatus, belumStatus, setujuStatus,
											mengulangStatus, sidangStatus, kelas, session)
											.setProjection(Projections.property("id")),
									KrsMahasiswa.class, false);
						} else if (jenis.equals(PELAJARAN)) {
							_full = initCriteria(true, jenis, keyword, fakultas, jurusan, program, yayasan,
									sekolah, ta, smt, hari, remedial, pra, ekstra, paralel, requestStatus,
									aktifStatus, seminarStatus, lulusStatus, gagalStatus, belumStatus,
									setujuStatus, mengulangStatus, sidangStatus, kelas, session).list();
						} else {
							_full = new ArrayList<VOPembelajaran>();
						}
						TELA_CACHE.put(_tk, new ArrayList(_full));
						TELA_EXPIRY.put(_tk, System.currentTimeMillis() + TELA_TTL_MS);
						} finally {
							if (session != null && session.isOpen()) {
								session.clear();
								session.disconnect();
								closeHibernateSessionQuietly(session);
							}
						}
					}
					size = _full.size();
					int _off = jumlahDataDalamSatuHalamanElearning * (paging == null ? 0 : paging.getActivePage());
					voPembelajarans = _full.subList(
							Math.min(_off, _full.size()),
							Math.min(_off + jumlahDataDalamSatuHalamanElearning, _full.size()));
				}

				// 4. MENGOSONGKAN DATA UI DENGAN AMAN MENGGUNAKAN LOCK DESKTOP
				data.clear(); // SINKRON: kosongkan buffer tanpa Executions.activate

				// Buffer untuk menampung hasil eksekusi multi-threading sementara
				final List<Map<String, Object>> synchronizedDataList = java.util.Collections
						.synchronizedList(new ArrayList<Map<String, Object>>());

				if (voPembelajarans != null) {
					final int s = voPembelajarans.size();

					if (s > 0) {
						// 5. BATASAN MULTITHREADING: tiap task bisa memicu VOPembelajaran.reInitPertemuan
						// (DELETE + commit berat saat cache dingin). 30 paralel = lonjakan tulis & koneksi
						// (pool c3p0 max 80; beberapa user serentak bisa menghabiskannya) + dulu antri di
						// write-lock UpdateTimestampsCache. Dibatasi 8 agar tak membanjiri DB/koneksi;
						// pemuatan pertama sedikit lebih lama, tapi hasilnya di-cache (akses berikut cepat).
						int maxThreads = Math.min(8, s);
						java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors
								.newFixedThreadPool(maxThreads);

						// Counter aman dari bentrokan antar thread
						final java.util.concurrent.atomic.AtomicInteger inx = new java.util.concurrent.atomic.AtomicInteger(
								0);

						List<java.util.concurrent.Callable<Map<String, Object>>> tasks = new ArrayList<java.util.concurrent.Callable<Map<String, Object>>>();

						for (final VOPembelajaran voPembelajaran : voPembelajarans) {
							tasks.add(new java.util.concurrent.Callable<Map<String, Object>>() {
								@Override
								public Map<String, Object> call() throws Exception {
									KrsMahasiswa krsMahasiswa = null;
									if (voPembelajaran instanceof KrsMahasiswa) {
										krsMahasiswa = (KrsMahasiswa) voPembelajaran;
									}

									if (krsMahasiswa == null || dosen == null
											|| (dosen != null && krsMahasiswa != null
													&& krsMahasiswa.getDosenPa() != null
													&& krsMahasiswa.getDosenPa().getId().equals(dosen.getId()))) {

										// (progress bar live dinonaktifkan pada mode render sinkron)

										String subta = "";
										String tabName = "";
										Object[] jml = null;
										int mhsSize = 0;

										try {
											if (voPembelajaran instanceof JadwalPelajaran) {
												JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) voPembelajaran;
												subta = jadwalPelajaran.getTahunAjaran() + "/"
														+ (jadwalPelajaran.getSemester() == null ? "Semua"
																: jadwalPelajaran.getSemester() % 2 == 0
																		? Perkuliahan.GENAP
																		: Perkuliahan.GANJIL);

												tabName = jadwalPelajaran.infoSimple();
											} else {
												subta = voPembelajaran.ambilTahunAjaran() + "/"
														+ (voPembelajaran.ambilMerupakanPraPerkuliahan()
																? "Pra Perkuliahan"
																: (!voPembelajaran.ambilMerupakanSP()
																		? voPembelajaran.ambilJenisSemester()
																		: Common.getBahasaConfig("Semester Pendek")));

												tabName = voPembelajaran.infoSimple();
												jml = voPembelajaran.ambilJumlahPertemuanStatistik(false, false);
												mhsSize = voPembelajaran.ambilJumlahDetailperkuliahanLangsung();
											}

											Map<String, Object> map = new java.util.HashMap<String, Object>();
											map.put("perkuliahan", voPembelajaran);
											map.put("jml", jml);
											map.put("mhsSize", mhsSize);
											map.put("tabName", tabName);
											map.put("subta", subta);
											return map;

										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
										}
									}
									return null;
								}
							});
						}

						try {
							// 7. EKSEKUSI SELURUH THREAD SECARA PARALEL DAN TUNGGU HINGGA SELESAI
							List<java.util.concurrent.Future<Map<String, Object>>> results = executor.invokeAll(tasks);

							// 8. KELOLA URUTAN 'ta' SECARA SEKUENSIAL DI LUAR THREAD
							String ta = "";
							for (java.util.concurrent.Future<Map<String, Object>> future : results) {
								Map<String, Object> map = future.get();
								if (map != null) {
									String currentSubta = (String) map.get("subta");
									boolean ganti = false;
									if (!currentSubta.equalsIgnoreCase(ta)) {
										ta = currentSubta;
										ganti = true;
									}
									map.put("ta", ta);
									map.put("ganti", ganti);

									// Masukkan ke buffer data
									synchronizedDataList.add(map);
								}
							}
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						} finally {
							// WAJIB: Tutup Thread Pool agar memori tidak bocor
							executor.shutdown();
						}
					}
				}

				// 9. INSERT HASIL AKHIR KE ZK UI MODEL SECARA SINKRON DAN AMAN
				// SINKRON (event AU): render LANGSUNG tanpa Executions.activate/deactivate — kita sudah
				// memegang desktop lock di event listener. Ini yang membuat data PASTI muncul (mirip panel
				// kanan loadDataKanan yang render sinkron).
				data.addAll(synchronizedDataList);
				try {
					label.setValue("");
					MainProgressHelper.finishProgressPanel(progressPanel, "Data pembelajaran selesai dimuat", true);
					detachRowsProgressPanel(progressPanel);
					loadDoneListener.onEvent(null);
				} catch (Exception e) {
					MainProgressHelper.errorProgressPanel(progressPanel, "Data pembelajaran belum dapat dimuat.");
					ais.common.Common.tampilErrorJikaAdmin(e);
				}

				voPembelajarans = null;
				} finally {
					/* OPTIMASI FASE 5: push dulu dinyalakan tapi tidak pernah dimatikan sehingga browser
					 * terus polling dan menahan thread Tomcat selama tab terbuka. Matikan HANYA bila kita
					 * yang menyalakannya, dan hanya bila desktop masih hidup. */
					if (pushDinyalakanDiSini && desktop != null && desktop.isAlive() && desktop.isServerPushEnabled()) {
						try { desktop.enableServerPush(false); } catch (Exception e) {
							ais.common.ErrorAuditUtil.record(e, "Fase5.lepasServerPush");
						}
					}
				}
			}
		}, "", false, 60);
	}

	@SuppressWarnings({})
	public static void tampilkanStatistikAktifitasMahasiswa(final Perkuliahan perkuliahan, final Component rowMahasiswa,
			final Component rowDosen, final Component toolbarButtonMahasiswa) throws Exception {
		List<Perkuliahan> per = new ArrayList<Perkuliahan>();
		per.add(perkuliahan);
		tampilkanStatistikAktifitasMahasiswa(per, rowMahasiswa, rowDosen, toolbarButtonMahasiswa);
	}

	@SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
	public static void tampilkanStatistikAktifitasMahasiswa(final List<Perkuliahan> per, final Component rowMahasiswa,
			final Component rowDosen, final Component toolbarButtonMahasiswa) throws Exception {
		Tbmuser tbmuser = Common.getCurrentUser();

		if (per == null || per.isEmpty()) {
			return;
		}

		Rows rowsBaruMahasiswa = null;
		if (per.size() > 1) {
			Grid gridBaruMahasiswa = new Grid();
			gridBaruMahasiswa.setSclass("fgrid");
			gridBaruMahasiswa.setOddRowSclass("non-odd");
			gridBaruMahasiswa.setParent(rowMahasiswa);

			rowsBaruMahasiswa = new Rows();
			rowsBaruMahasiswa.setParent(gridBaruMahasiswa);
		}

		Rows rowsBaruDosen = null;
		if (per.size() > 1) {
			Grid gridBaruDosen = new Grid();
			gridBaruDosen.setSclass("fgrid");
			gridBaruDosen.setOddRowSclass("non-odd");
			gridBaruDosen.setParent(rowDosen);

			rowsBaruDosen = new Rows();
			rowsBaruDosen.setParent(gridBaruDosen);
		}

		final List<Map> maps = new ArrayList<Map>();
		for (final Perkuliahan perkuliahan : per) {
			// Elemen per bisa null (mis. entri cache/ambilData gagal) -> lewati agar
			// perkuliahan.getId()/ambilPertemuan() di bawah tidak NPE.
			if (perkuliahan == null) {
				continue;
			}

			Dosen kaprodi = perkuliahan.getJurusan() == null ? null
					: perkuliahan.getJurusan().getKaprodi();
			final Map parameters = ais.common.HashMapGenerator.getRand();

			parameters.put("perkuliahan", perkuliahan.getId());
			parameters.put("tampil_nilai", "1");
			parameters.put("kaprodi",
					kaprodi == null ? "(                                          )" : kaprodi.getNama());
			parameters.put("nip", kaprodi == null ? "" : kaprodi.getCode());
			parameters.put("tanggal", Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));

			parameters.put("nama_kaprodi",
					kaprodi == null ? "(                                          )" : kaprodi.getNama());
			parameters.put("nip_kaprodi", kaprodi == null || kaprodi.getCode() == null ? "" : kaprodi.getCode().trim());

			parameters.put("nidn_kaprodi", kaprodi == null || kaprodi.getNidn() == null ? "" : kaprodi.getNidn());

			TreeMap<String, Long> pertemuanss = perkuliahan.ambilPertemuan();
			List<Pertemuan> pertemuans = new ArrayList<Pertemuan>();
			for (Long pertemuanid : pertemuanss.values()) {
				Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
				if (pertemuan != null) {
					pertemuans.add(pertemuan);
				}
			}
			pertemuanss.clear();
			pertemuanss = null;

			Collection<Long> collection = tbmuser != null && tbmuser.getMahasiswa() != null ? new ArrayList<Long>()
					: perkuliahan.ambilDetailperkuliahan();

			if (perkuliahan.getPerkuliahan_paralel() != null) {
				collection = tbmuser != null && tbmuser.getMahasiswa() != null ? new ArrayList<Long>()
						: perkuliahan.getPerkuliahan_paralel().ambilDetailperkuliahan();
			}

			if (tbmuser != null && tbmuser.getMahasiswa() != null) {
				collection.add(perkuliahan.ambilDetailperkuliahan(tbmuser.getMahasiswa()));

				if (perkuliahan.getPerkuliahan_paralel() != null) {
					collection.add(perkuliahan.getPerkuliahan_paralel().ambilDetailperkuliahan(tbmuser.getMahasiswa()));
				}
			}

			if (true) {
				MyGrid grid = new MyGrid();
				grid.setSclass("fgrid");
				grid.setWidth("100%");
				ais.ui.util.ZkCompat.setFixedLayout(grid, true);
				if (rowsBaruMahasiswa == null) {
					grid.setParent(rowMahasiswa);
				} else {
					MyFormRow rowBaru = new MyFormRow();
					rowBaru.setParent(rowsBaruMahasiswa);
					grid.setParent(rowBaru);
				}

				Columns columns = new Columns();
				columns.setParent(grid);

				Column column = new Column("No.");
				column.setWidth("40px");
				column.setParent(columns);

				column = new Column("Mahasiswa");
				column.setParent(columns);

				column = new Column("Hdr");
				column.setWidth("10%");
				column.setParent(columns);

				column = new Column("Ikt.Ujian");
				column.setWidth("5%");
				column.setParent(columns);

				column = new Column("Ikt.Disk.");
				column.setWidth("5%");
				column.setParent(columns);

				column = new Column("Upl.Tgs.");
				column.setWidth("5%");
				column.setParent(columns);

				column = new Column("Lht.Tgs");
				column.setWidth("5%");
				column.setParent(columns);

				column = new Column("Vid.Conf.");
				column.setWidth("5%");
				column.setParent(columns);

				column = new Column("Akses");
				column.setWidth("5%");
				column.setParent(columns);

				column = new Column("Lht.Video");
				column.setWidth("5%");
				column.setParent(columns);

				column = new Column("Lht.Audio");
				column.setWidth("5%");
				column.setParent(columns);

				column = new Column("Lht.Materi");
				column.setWidth("5%");
				column.setParent(columns);

				Rows rows = new Rows();

				rows.setParent(grid);

				if (per.size() > 1) {
					Group myRow = new ais.ui.util.MyGroupConfig();
					myRow.setParent(rows);

					Hbox v = ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahanUmum(myRow,
							perkuliahan, false, false, null);
					v.appendChild(new Space());
					v.appendChild(new MyLabelAgakKecilBold(perkuliahan.info()));
					ais.ui.util.ZkCompat.setSpans(myRow, "12");
				}

				int nomor = 1;
				int ujian = 0;
				int diskusi = 0;
				int uplTugas = 0;
				int tugas = 0;
				int online = 0;
				int akses = 0;
				int video = 0;
				int audio = 0;
				int bahan_perkulaiahan = 0;

				for (Long detailperkuliahanid : collection) {
					// 'collection' bisa memuat elemen null -> detailperkuliahanid.toString() NPE. Lewati.
					if (detailperkuliahanid == null) {
						continue;
					}
					final Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
							.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());

					if (detailperkuliahan != null && detailperkuliahan.getMahasiswa() != null) {
						Map<String, Object> map = new java.util.HashMap<String, Object>();
						Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
						MyFormRow myRow = new MyFormRow();
						myRow.setParent(rows);

						myRow.appendChild(new Label(Common.numberFormat.get().format(nomor)));

						Hbox hbox = new Hbox();
						hbox.setParent(myRow);
						try {
							CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(hbox);

							Vbox a = new Vbox();
							a.setParent(hbox);
							new MyLabelAgakKecil(mahasiswa == null ? "" : mahasiswa.getNim()).setParent(a);
							new MyLabelAgakKecil(mahasiswa == null ? "" : mahasiswa.getNama()).setParent(a);
							mahasiswa.tampilkanHp(a);
							mahasiswa.tampilkanEmail(a);
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
						map.put("jurusan", perkuliahan.infoSimple());
						map.put("jenis", "Mahasiswa");
						map.put("nim", mahasiswa == null ? "" : mahasiswa.getNim());
						map.put("nama", mahasiswa == null ? "" : mahasiswa.getNama());
						map.put("telp", mahasiswa == null ? "" : mahasiswa.getTelp());
						map.put("email", mahasiswa == null ? "" : mahasiswa.getEmail());

						Object[] jml = perkuliahan.ambilJumlahPertemuanStatistik(pertemuans, mahasiswa, null, true,
								true);
						int jumlahUjianTotal = jml == null || jml[8] == null ? 0 : Integer.parseInt(jml[8].toString());
						int jumlahDiskusiTotal = jml == null || jml[9] == null ? 0
								: Integer.parseInt(jml[9].toString());

						ujian += jumlahUjianTotal;
						diskusi += jumlahDiskusiTotal;

						Map<String, Integer> statuses = (Map<String, Integer>) (jml == null || jml[4] == null ? null
								: jml[4]);
						String abs = statuses == null ? ""
								: statuses.toString().replaceAll("\\{", "").replaceAll("\\}", "").trim();
						A a;
						myRow.appendChild(a = new A(abs));

						map.put("abs", abs);
						a.setStyle("font-size:12px;");
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Map parametersBaru = new HashMap(parameters);
								List<Map<String, Serializable>> maps = CommonReportHelper
										.generateParameterMapAbsensiRinci(perkuliahan, detailperkuliahan, null, null,
												true, false);
								parametersBaru.put("maps", maps);
								Report.generatePDFReport("pdf", parametersBaru, "LaporanAbsensiRinci",
										ais.ui.util.WaktuUtil.getDate(), Common.locale, null, null);
							}
						});

						myRow.appendChild(new Label(Common.numberFormat.get().format(jumlahUjianTotal)));
						myRow.appendChild(new Label(Common.numberFormat.get().format(jumlahDiskusiTotal)));

						map.put("jumlahUjianTotal", jumlahUjianTotal);
						map.put("jumlahDiskusiTotal", jumlahDiskusiTotal);

						int jumlahUploadTugas = 0;
						for (Pertemuan pertemuan : pertemuans) {
							if (pertemuan.getAktif()) {
								jumlahUploadTugas += pertemuan.ambilJumlahTugasFileContent(mahasiswa);
								Collection<TugasPertemuan> tugasPertemuans = pertemuan.ambilTugasPertemuanTotal()
										.values();
								for (TugasPertemuan tugasPertemuan : tugasPertemuans) {
									jumlahUploadTugas += tugasPertemuan.ambilJumlahTugasFileContent(mahasiswa);
								}
							}
						}
						myRow.appendChild(new Label(Common.numberFormat.get().format(jumlahUploadTugas)));

						uplTugas += jumlahUploadTugas;
						map.put("file_tugas", jumlahUploadTugas);

						int pert = 0;
						a = new A();
						for (Pertemuan pertemuan : pertemuans) {
							if (pertemuan.getAktif()) {
								TreeMap<String, String> d = pertemuan.ambilData("tugas", mahasiswa.getId().toString(),
										pertemuan.getJudultugas());
								if (a.getAttribute("d") == null) {
									a.setAttribute("d", d);
								} else {
									((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
								}
								if (!d.isEmpty()) {
									pert += d.size();
								}

								Collection<TugasPertemuan> tugasPertemuans = pertemuan.ambilTugasPertemuanTotal()
										.values();
								for (TugasPertemuan tugasPertemuan : tugasPertemuans) {
									d = tugasPertemuan.ambilData("tugas", mahasiswa.getId().toString(),
											tugasPertemuan.getJudultugas());
									if (a.getAttribute("d") == null) {
										a.setAttribute("d", d);
									} else {
										((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
									}
									if (!d.isEmpty()) {
										pert += d.size();
									}
								}
							}
						}
						a.setLabel(Common.numberFormat.get().format(pert));
						myRow.appendChild(a);
						a.setStyle("font-size:12px;");
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								A a = (A) arg0.getTarget();
								TreeMap<String, String> d = (TreeMap<String, String>) a.getAttribute("d");
								if (d != null && !d.isEmpty()) {
									displayTreeMap(d);
								}
							}
						});
						map.put("tugas", pert);

						tugas += pert;

						pert = 0;
						a = new A();
						for (Pertemuan pertemuan : pertemuans) {
							if (pertemuan.getAktif()) {
								TreeMap<String, String> d = pertemuan.ambilData("online", mahasiswa.getId().toString(),
										pertemuan.getPertemuanKe() + "-"
												+ Common.dateFormat4.get().format(pertemuan.getTanggal()));
								if (a.getAttribute("d") == null) {
									a.setAttribute("d", d);
								} else {
									((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
								}
								if (!d.isEmpty()) {
									pert += d.size();
								}
							}
						}
						a.setLabel(Common.numberFormat.get().format(pert));
						myRow.appendChild(a);
						a.setStyle("font-size:12px;");
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								A a = (A) arg0.getTarget();
								TreeMap<String, String> d = (TreeMap<String, String>) a.getAttribute("d");
								if (d != null && !d.isEmpty()) {
									displayTreeMap(d);
								}
							}
						});
						map.put("online", pert);

						online += pert;

						pert = 0;
						a = new A();
						for (Pertemuan pertemuan : pertemuans) {
							if (pertemuan.getAktif()) {
								TreeMap<String, String> d = pertemuan.ambilData("akses", mahasiswa.getId().toString(),
										pertemuan.getPertemuanKe() + "-"
												+ Common.dateFormat4.get().format(pertemuan.getTanggal()));
								if (a.getAttribute("d") == null) {
									a.setAttribute("d", d);
								} else {
									((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
								}
								if (!d.isEmpty()) {
									pert += d.size();
								}
							}
						}
						a.setLabel(Common.numberFormat.get().format(pert));
						myRow.appendChild(a);
						a.setStyle("font-size:12px;");
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								A a = (A) arg0.getTarget();
								TreeMap<String, String> d = (TreeMap<String, String>) a.getAttribute("d");
								if (d != null && !d.isEmpty()) {
									displayTreeMap(d);
								}
							}
						});
						map.put("akses", pert);

						akses += pert;

						pert = 0;
						a = new A();
						for (Pertemuan pertemuan : pertemuans) {
							if (pertemuan.getAktif()) {
								TreeMap<String, String> d = pertemuan.ambilData("video", mahasiswa.getId().toString(),
										pertemuan.getPertemuanKe() + "-"
												+ Common.dateFormat4.get().format(pertemuan.getTanggal()));
								if (a.getAttribute("d") == null) {
									a.setAttribute("d", d);
								} else {
									((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
								}
								if (!d.isEmpty()) {
									pert += d.size();
								}
							}
						}
						a.setLabel(Common.numberFormat.get().format(pert));
						myRow.appendChild(a);
						a.setStyle("font-size:12px;");
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								A a = (A) arg0.getTarget();
								TreeMap<String, String> d = (TreeMap<String, String>) a.getAttribute("d");
								if (d != null && !d.isEmpty()) {
									displayTreeMap(d);
								}
							}
						});
						map.put("video", pert);

						video += pert;

						pert = 0;
						a = new A();
						for (Pertemuan pertemuan : pertemuans) {
							if (pertemuan.getAktif()) {
								TreeMap<String, String> d = pertemuan.ambilData("audio", mahasiswa.getId().toString(),
										pertemuan.getPertemuanKe() + "-"
												+ Common.dateFormat4.get().format(pertemuan.getTanggal()));
								if (a.getAttribute("d") == null) {
									a.setAttribute("d", d);
								} else {
									((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
								}
								if (!d.isEmpty()) {
									pert += d.size();
								}
							}
						}
						a.setLabel(Common.numberFormat.get().format(pert));
						myRow.appendChild(a);
						a.setStyle("font-size:12px;");
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								A a = (A) arg0.getTarget();
								TreeMap<String, String> d = (TreeMap<String, String>) a.getAttribute("d");
								if (d != null && !d.isEmpty()) {
									displayTreeMap(d);
								}
							}
						});
						map.put("audio", pert);

						audio += pert;

						pert = 0;
						a = new A();
						for (Pertemuan pertemuan : pertemuans) {
							if (pertemuan.getAktif()) {
								TreeMap<String, String> d = pertemuan.ambilData("bahan_perkulaiahan",
										mahasiswa.getId().toString(), pertemuan.getPertemuanKe() + "-"
												+ Common.dateFormat4.get().format(pertemuan.getTanggal()));
								if (a.getAttribute("d") == null) {
									a.setAttribute("d", d);
								} else {
									((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
								}
								if (!d.isEmpty()) {
									pert += d.size();
								}
							}
						}
						a.setLabel(Common.numberFormat.get().format(pert));
						myRow.appendChild(a);
						a.setStyle("font-size:12px;");
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								A a = (A) arg0.getTarget();
								TreeMap<String, String> d = (TreeMap<String, String>) a.getAttribute("d");
								if (d != null && !d.isEmpty()) {
									displayTreeMap(d);
								}
							}
						});
						map.put("bahan_perkulaiahan", pert);
						jml = null;
						nomor++;
						bahan_perkulaiahan += pert;

						maps.add(map);
					}
				}
					MyFormRow myRow = new MyFormRow();
					myRow.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(myRow, "12");
					myRow.appendChild(buildElearningHtmlMetricChart("Ringkasan Aktivitas Peserta", "Angka ini membantu melihat bagian belajar yang paling sering digunakan peserta.",
							new String[] { "Ujian", "Diskusi", "Upload Tugas", "Lihat Tugas", "Video Conference", "Akses", "Video", "Audio", "Materi" },
							new int[] { ujian, diskusi, uplTugas, tugas, online, akses, video, audio, bahan_perkulaiahan }));

				Foot foot = new Foot();
				foot.setParent(grid);
				Footer footer = new Footer();
				footer.setParent(foot);
				footer = new Footer("Total");
				footer.setParent(foot);
				footer = new Footer();
				footer.setParent(foot);

				footer = new Footer(Common.numberFormat.get().format(ujian));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(diskusi));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(uplTugas));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(tugas));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(online));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(akses));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(video));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(audio));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(bahan_perkulaiahan));
				footer.setParent(foot);

				UIUtil.checkGrigMobile(grid);

			}

			if (tbmuser == null || tbmuser.getMahasiswa() == null) {
				MyGrid grid = new MyGrid();
				ais.ui.util.ZkCompat.setFixedLayout(grid, true);
				grid.setWidth("100%");
				grid.setSclass("fgrid");

				if (rowsBaruDosen == null) {
					grid.setParent(rowDosen);
				} else {
					MyFormRow rowBaru = new MyFormRow();
					rowBaru.setParent(rowsBaruDosen);
					grid.setParent(rowBaru);
				}

				Columns columns = new Columns();
				columns.setParent(grid);

				Column column = new Column("No.");
				column.setWidth("40px");
				column.setParent(columns);

				column = new Column("Dosen");
				column.setParent(columns);

				column = new Column("Hdr");
				column.setWidth("10%");
				column.setParent(columns);

				column = new Column("Ikt.Diskusi");
				column.setWidth("5%");
				column.setParent(columns);

				column = new Column("Buat Tgs");
				column.setWidth("5%");
				column.setParent(columns);

				column = new Column("Vid.Conf.");
				column.setWidth("5%");
				column.setParent(columns);

				column = new Column("Lht.Akses");
				column.setWidth("5%");
				column.setParent(columns);

				column = new Column("Lht.Video");
				column.setWidth("5%");
				column.setParent(columns);

				column = new Column("Lht.Audio");
				column.setWidth("5%");
				column.setParent(columns);

				column = new Column("Lht.Materi");
				column.setWidth("5%");
				column.setParent(columns);

				Rows rows = new Rows();

				rows.setParent(grid);

				if (per.size() > 1) {
					Group myRow = new ais.ui.util.MyGroupConfig();
					myRow.setParent(rows);
					myRow.appendChild(new MyLabelBold(perkuliahan.infoSimple()));
				}

				int nomor = 1;
				int diskusi = 0;
				int tugas = 0;
				int online = 0;
				int akses = 0;
				int video = 0;
				int audio = 0;
				int bahan_perkulaiahan = 0;
				List<Dosen> ds = perkuliahan.populateDosenBuNama();
				for (final Dosen dosen : ds) {
					Map<String, Object> map = new java.util.HashMap<String, Object>();
					MyFormRow myRow = new MyFormRow();
					myRow.setParent(rows);
					map.put("jurusan", perkuliahan.infoSimple());
					map.put("jenis", "Dosen");
					map.put("nim", dosen == null ? "" : dosen.getNidn());
					map.put("nama", dosen == null ? "" : dosen.getNama());
					map.put("telp", dosen == null ? "" : dosen.getTelp());
					map.put("email", dosen == null ? "" : dosen.getEmail());

					myRow.appendChild(new Label(Common.numberFormat.get().format(nomor)));

					Hbox hbox = new Hbox();
					hbox.setParent(myRow);
					try {
						CommonMedia.tampilkanGambarKecil(dosen).setParent(hbox);

						Vbox a = new Vbox();
						a.setParent(hbox);
						new MyLabelAgakKecil(dosen == null ? "" : dosen.getNidn()).setParent(a);
						new MyLabelAgakKecil(dosen == null ? "" : dosen.getNama()).setParent(a);
						new MyLabelAgakKecil(dosen == null ? "" : dosen.getTelp()).setParent(a);
						new MyLabelAgakKecil(dosen == null ? "" : dosen.getEmail()).setParent(a);
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}

					Object[] jml = perkuliahan.ambilJumlahPertemuanStatistik(pertemuans, null, dosen, true, false);
					int jumlahDiskusiTotal = jml == null || jml[9] == null ? 0 : Integer.parseInt(jml[9].toString());

					Map<String, Integer> statusesDosen = (Map<String, Integer>) (jml == null || jml[6] == null ? null
							: jml[6]);
					String absDosen = statusesDosen == null ? ""
							: statusesDosen.toString().replaceAll("\\{", "").replaceAll("\\}", "").trim();
					A a;
					myRow.appendChild(a = new A(absDosen));
					a.setStyle("font-size:12px;");
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Map parametersBaru = new HashMap(parameters);
							List<Map<String, Serializable>> maps = CommonReportHelper
									.generateParameterMapAbsensiRinci(perkuliahan, null, null, dosen, false, true);
							parametersBaru.put("maps", maps);
							Report.generatePDFReport("pdf", parametersBaru, "LaporanAbsensiRinci",
									ais.ui.util.WaktuUtil.getDate(), Common.locale, null, null);
						}
					});

					map.put("abs_dsn", absDosen);

					myRow.appendChild(new Label(Common.numberFormat.get().format(jumlahDiskusiTotal)));

					map.put("jumlahUjianTotal", null);
					map.put("jumlahDiskusiTotal", jumlahDiskusiTotal);

					diskusi += jumlahDiskusiTotal;

					int pert = 0;
					a = new A();
					for (Pertemuan pertemuan : pertemuans) {
						if (pertemuan.getAktif()) {
							TreeMap<String, String> d = pertemuan.ambilData("tugas", dosen.getId().toString(),
									pertemuan.getPertemuanKe() + "-"
											+ Common.dateFormat4.get().format(pertemuan.getTanggal()));
							if (a.getAttribute("d") == null) {
								a.setAttribute("d", d);
							} else {
								((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
							}
							if (!d.isEmpty()) {
								pert += d.size();
							}
						}
					}
					a.setLabel(Common.numberFormat.get().format(pert));
					myRow.appendChild(a);
					a.setStyle("font-size:12px;");
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							A a = (A) arg0.getTarget();
							TreeMap<String, String> d = (TreeMap<String, String>) a.getAttribute("d");
							if (d != null && !d.isEmpty()) {
								displayTreeMap(d);
							}
						}
					});
					map.put("tugas", pert);

					tugas += pert;

					pert = 0;
					a = new A();
					for (Pertemuan pertemuan : pertemuans) {
						if (pertemuan.getAktif()) {
							TreeMap<String, String> d = pertemuan.ambilData("online", dosen.getId().toString(),
									pertemuan.getPertemuanKe() + "-"
											+ Common.dateFormat4.get().format(pertemuan.getTanggal()));
							if (a.getAttribute("d") == null) {
								a.setAttribute("d", d);
							} else {
								((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
							}
							if (!d.isEmpty()) {
								pert += d.size();
							}
						}
					}
					a.setLabel(Common.numberFormat.get().format(pert));
					myRow.appendChild(a);
					a.setStyle("font-size:12px;");
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							A a = (A) arg0.getTarget();
							TreeMap<String, String> d = (TreeMap<String, String>) a.getAttribute("d");
							if (d != null && !d.isEmpty()) {
								displayTreeMap(d);
							}
						}
					});
					map.put("online", pert);

					online += pert;

					pert = 0;
					a = new A();
					for (Pertemuan pertemuan : pertemuans) {
						if (pertemuan.getAktif()) {
							TreeMap<String, String> d = pertemuan.ambilData("akses", dosen.getId().toString(),
									pertemuan.getPertemuanKe() + "-"
											+ Common.dateFormat4.get().format(pertemuan.getTanggal()));
							if (a.getAttribute("d") == null) {
								a.setAttribute("d", d);
							} else {
								((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
							}
							if (!d.isEmpty()) {
								pert += d.size();
							}
						}
					}
					a.setLabel(Common.numberFormat.get().format(pert));
					myRow.appendChild(a);
					a.setStyle("font-size:12px;");
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							A a = (A) arg0.getTarget();
							TreeMap<String, String> d = (TreeMap<String, String>) a.getAttribute("d");
							if (d != null && !d.isEmpty()) {
								displayTreeMap(d);
							}
						}
					});
					map.put("akses", pert);

					akses += pert;

					pert = 0;
					a = new A();
					for (Pertemuan pertemuan : pertemuans) {
						if (pertemuan.getAktif()) {
							TreeMap<String, String> d = pertemuan.ambilData("video", dosen.getId().toString(),
									pertemuan.getPertemuanKe() + "-"
											+ Common.dateFormat4.get().format(pertemuan.getTanggal()));
							if (a.getAttribute("d") == null) {
								a.setAttribute("d", d);
							} else {
								((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
							}
							if (!d.isEmpty()) {
								pert += d.size();
							}
						}
					}
					a.setLabel(Common.numberFormat.get().format(pert));
					myRow.appendChild(a);
					a.setStyle("font-size:12px;");
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							A a = (A) arg0.getTarget();
							TreeMap<String, String> d = (TreeMap<String, String>) a.getAttribute("d");
							if (d != null && !d.isEmpty()) {
								displayTreeMap(d);
							}
						}
					});
					map.put("video", pert);

					video += pert;

					pert = 0;
					a = new A();
					for (Pertemuan pertemuan : pertemuans) {
						if (pertemuan.getAktif()) {
							TreeMap<String, String> d = pertemuan.ambilData("audio", dosen.getId().toString(),
									pertemuan.getPertemuanKe() + "-"
											+ Common.dateFormat4.get().format(pertemuan.getTanggal()));
							if (a.getAttribute("d") == null) {
								a.setAttribute("d", d);
							} else {
								((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
							}
							if (!d.isEmpty()) {
								pert += d.size();
							}
						}
					}
					a.setLabel(Common.numberFormat.get().format(pert));
					myRow.appendChild(a);
					a.setStyle("font-size:12px;");
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							A a = (A) arg0.getTarget();
							TreeMap<String, String> d = (TreeMap<String, String>) a.getAttribute("d");
							if (d != null && !d.isEmpty()) {
								displayTreeMap(d);
							}
						}
					});
					map.put("audio", pert);

					audio += pert;

					pert = 0;
					a = new A();
					for (Pertemuan pertemuan : pertemuans) {
						if (pertemuan.getAktif()) {
							TreeMap<String, String> d = pertemuan.ambilData("bahan_perkulaiahan",
									dosen.getId().toString(), pertemuan.getPertemuanKe() + "-"
											+ Common.dateFormat4.get().format(pertemuan.getTanggal()));
							if (a.getAttribute("d") == null) {
								a.setAttribute("d", d);
							} else {
								((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
							}
							if (!d.isEmpty()) {
								pert += d.size();
							}
						}
					}
					a.setLabel(Common.numberFormat.get().format(pert));
					myRow.appendChild(a);
					a.setStyle("font-size:12px;");
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							A a = (A) arg0.getTarget();
							TreeMap<String, String> d = (TreeMap<String, String>) a.getAttribute("d");
							if (d != null && !d.isEmpty()) {
								displayTreeMap(d);
							}
						}
					});
					map.put("bahan_perkulaiahan", pert);

					bahan_perkulaiahan += pert;

					maps.add(map);
					jml = null;
					nomor++;

				}
					MyFormRow myRow = new MyFormRow();
					myRow.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(myRow, "10");
					myRow.appendChild(buildElearningHtmlMetricChart("Ringkasan Aktivitas Dosen", "Angka ini membantu melihat bahan, tugas, diskusi, dan aktivitas yang sudah berjalan.",
							new String[] { "Diskusi", "Buat Tugas", "Video Conference", "Akses", "Video", "Audio", "Materi" },
							new int[] { diskusi, tugas, online, akses, video, audio, bahan_perkulaiahan }));

				Foot foot = new Foot();
				foot.setParent(grid);
				Footer footer = new Footer();
				footer.setParent(foot);
				footer = new Footer("Total");
				footer.setParent(foot);
				footer = new Footer();
				footer.setParent(foot);

				footer = new Footer(Common.numberFormat.get().format(diskusi));
				footer.setParent(foot);

				footer = new Footer(Common.numberFormat.get().format(tugas));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(online));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(akses));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(video));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(audio));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(bahan_perkulaiahan));
				footer.setParent(foot);

				UIUtil.checkGrigMobile(grid);
			}
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {

				Map parameters = ais.common.HashMapGenerator.getRand();
				parameters.put("maps", maps);
				@SuppressWarnings("unused")
				Tabbox tabbox = Report.generatePDFReportKembaliTab(Report.PDF, new Map[] { parameters },
						new String[] { "Kegiatan_Belajar_Mengajar" }, new String[] { "Kegiatan Belajar Mengajar" },
						ais.ui.util.WaktuUtil.getDate());

			}
		});
		button.setParent(toolbarButtonMahasiswa);
	}

	@SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
	public static void tampilkanStatistikKeaktifanPeserta(final VOPembelajaran vopemblajaran, Jurusan jurusan,
			final Component rowMahasiswa, final Component rowDosen, final Component toolbarButtonMahasiswa)
			throws Exception {

		final Tbmuser tbmuser = Common.getCurrentUser();
		final List<Map> maps = new ArrayList<Map>();

		Dosen kaprodi = jurusan == null ? null : jurusan.getKaprodi();
		final Map parameters = ais.common.HashMapGenerator.getRand();

		parameters.put("perkuliahan", vopemblajaran.getId());
		parameters.put("tampil_nilai", "1");
		parameters.put("kaprodi", kaprodi == null ? "(                                          )" : kaprodi.getNama());
		parameters.put("nip", kaprodi == null || kaprodi.getCode() == null ? "" : kaprodi.getCode());
		parameters.put("tanggal", Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));
		parameters.put("nama_kaprodi",
				kaprodi == null ? "(                                          )" : kaprodi.getNama());
		parameters.put("nip_kaprodi", kaprodi == null || kaprodi.getCode() == null ? "" : kaprodi.getCode().trim());
		parameters.put("nidn_kaprodi", kaprodi == null || kaprodi.getNidn() == null ? "" : kaprodi.getNidn());

		// 1. PENGUMPULAN DATA PESERTA DIDIK (MAHASISWA & SISWA)
		List<Long> collectionMahasiswa = tbmuser != null && tbmuser.getMahasiswa() != null ? new ArrayList<Long>()
				: vopemblajaran.ambilMahasiswaById();
		List<Long> collectionSiswa = tbmuser != null && tbmuser.getSiswa() != null ? new ArrayList<Long>()
				: vopemblajaran.ambilSiswaById();

		List<Object> daftarPesertaDidik = new ArrayList<Object>();
		for (Long idMhs : collectionMahasiswa) {
			Mahasiswa mhs = (Mahasiswa) GeneralValueObject.ambilData(Mahasiswa.class, idMhs.toString());
			if (mhs != null)
				daftarPesertaDidik.add(mhs);
		}
		for (Long idSiswa : collectionSiswa) {
			Siswa sw = (Siswa) GeneralValueObject.ambilData(Siswa.class, idSiswa.toString());
			if (sw != null)
				daftarPesertaDidik.add(sw);
		}

		if (tbmuser.getMahasiswa() != null) {
			daftarPesertaDidik.add(tbmuser.getMahasiswa());
		}
		if (tbmuser.getSiswa() != null) {
			daftarPesertaDidik.add(tbmuser.getSiswa());
		}

		// 2. PENGUMPULAN DATA PENGAJAR (DOSEN & GURU)
		List<Object> daftarPengajar = new ArrayList<Object>();
		List<Dosen> dosens = vopemblajaran.populateDosenBuNama();
		List<Guru> gurus = vopemblajaran.populateGuruBuNama();
		if (dosens != null)
			daftarPengajar.addAll(dosens);
		if (gurus != null)
			daftarPengajar.addAll(gurus);

		TreeMap<String, Object[]> materis = PertemuanFileContent.ambilMateri(vopemblajaran.ambilPertemuan(), false,
				null, tbmuser);

		// =============================================================================================
		// BAGIAN 1: RENDER GRID PESERTA DIDIK (MAHASISWA & SISWA)
		// =============================================================================================
		if (!daftarPesertaDidik.isEmpty() && materis != null) {
			int totalMateri = materis.size();
			boolean fix = materis.size() <= 5;

			Grid grid = new Grid();
			grid.setSclass("dgrid fgrid");
			grid.setWidth(fix ? "100%" : Common.isMobile() ? "300px" : "1200px");
			ais.ui.util.ZkCompat.setFixedLayout(grid, fix);
			grid.setParent(rowMahasiswa);
			grid.setStyle(
					"background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.05);");

			Columns columns = new Columns();
			columns.setParent(grid);

			Column column = new Column(Common.getBahasaConfig("No."));
			column.setWidth("50px");
			column.setStyle("font-weight:bold; background-color:#e9ecef; text-align:center;");
			column.setParent(columns);

			column = new Column(Common.getBahasaConfig("Peserta Didik"));
			column.setParent(columns);
			column.setWidth(fix ? "25%" : "280px");
			column.setStyle("font-weight:bold; background-color:#e9ecef;");

			ProfileUtil.tampilkanMateriColum(columns, materis);

			column = new Column(Common.getBahasaConfig("Total Statistik"));
			column.setWidth("140px");
			column.setStyle("font-weight:bold; background-color:#e9ecef; text-align:center;");
			column.setParent(columns);

			Rows rows = new Rows();

			rows.setParent(grid);

			Paging paging = new Paging();
			int nomor = 1;
			TreeMap<String, Integer> jmls = new TreeMap<String, Integer>();
			TreeMap<Long, Integer> jmlsMhs = new TreeMap<Long, Integer>();
			Map<String, TreeMap<Long, TugasFileContent>> tugases = new HashMap<String, TreeMap<Long, TugasFileContent>>();
			Map<String, List<Long>> ujians = new HashMap<String, List<Long>>();

			Integer totTgs = 0;
			Integer totUjian = 0;

			for (String key : materis.keySet()) {
				Object[] data = materis.get(key);
				if (data[0] instanceof Tugas) {
					totTgs++;
					Tugas tugas = (Tugas) data[0];
					TreeMap<Long, TugasFileContent> treemapData = new TreeMap<Long, TugasFileContent>();
					TreeMap<Long, TugasFileContent> tugasFileContentsa = tugas.ambilTugasFileContentTotal(treemapData,
							"", paging, 5000);
					tugases.put(key, tugasFileContentsa);
				} else if (data[0] instanceof PertemuanPunyaUjian) {
					PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) data[0];
					if (pertemuanPunyaUjian.getUjian() != null && pertemuanPunyaUjian.getUjian().getAktif()) {
						totUjian++;
						ujians.put(key, pertemuanPunyaUjian.ambilHasilUjianMahasiswa());
					}
				}
			}

			TreeMap<Long, Integer> jmlsTgsMhs = new TreeMap<Long, Integer>();
			TreeMap<Long, Integer> jmlsUjianMhs = new TreeMap<Long, Integer>();

			for (Object objPeserta : daftarPesertaDidik) {
				Map<String, Object> map = new java.util.HashMap<String, Object>();
				MyFormRow myRow = new MyFormRow();
				myRow.setParent(rows);

				Label lblNomor = new Label(Common.numberFormat.get().format(nomor++));
				lblNomor.setStyle("font-weight:bold; text-align:center; display:block;");
				myRow.appendChild(lblNomor);

				// Ekstraksi Identitas Fleksibel (Mahasiswa atau Siswa)
				Long idPeserta = null;
				String noInduk = "";
				String namaPeserta = "";
				String telpPeserta = "";
				String emailPeserta = "";
				String jenisPeserta = "";

				Hbox hboxProfile = new Hbox();
				hboxProfile.setSpacing("8px");
				hboxProfile.setParent(myRow);

				Vbox vboxDataProfile = new Vbox();

				try {
					if (objPeserta instanceof Mahasiswa) {
						Mahasiswa m = (Mahasiswa) objPeserta;
						idPeserta = m.getId();
						noInduk = m.getNim();
						namaPeserta = m.getNama();
						telpPeserta = m.getTelp();
						emailPeserta = m.getEmail();
						jenisPeserta = "Mahasiswa";
						CommonMedia.tampilkanGambarKecil(m).setParent(hboxProfile);
						m.tampilkanHp(vboxDataProfile);
						m.tampilkanEmail(vboxDataProfile);
					} else if (objPeserta instanceof Siswa) {
						Siswa s = (Siswa) objPeserta;
						idPeserta = s.getId();
						noInduk = s.getNis();
						namaPeserta = s.getNama();
						telpPeserta = s.getTeleponSiswa();
						emailPeserta = s.getAlamatEmail();
						jenisPeserta = "Siswa";
						CommonMedia.tampilkanGambarKecil(s).setParent(hboxProfile);
						s.tampilkanHp(vboxDataProfile);
						s.tampilkanEmail(vboxDataProfile);
					}

					vboxDataProfile.setParent(hboxProfile);
					MyLabelAgakKecil lblNoInduk = new MyLabelAgakKecil(noInduk);
					lblNoInduk.setStyle("font-weight:bold; color:#0d6efd;");
					lblNoInduk.setParent(vboxDataProfile);
					new MyLabelAgakKecil(namaPeserta).setParent(vboxDataProfile);

				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}

				map.put("jurusan", vopemblajaran.infoSimple());
				map.put("jenis", jenisPeserta);
				map.put("nim", noInduk);
				map.put("nama", namaPeserta);
				map.put("telp", telpPeserta);
				map.put("email", emailPeserta);

				StringBuilder absAkses = new StringBuilder();
				StringBuilder absTugas = new StringBuilder();
				StringBuilder absUjian = new StringBuilder();

				for (String key : materis.keySet()) {
					Object[] data = materis.get(key);
					Pertemuan pertemuan = (Pertemuan) data[1];
					TreeMap<String, String> d = null;

					TugasFileContent fileContent = null;
					HasilUjianMahasiswa hasilUjianMahasiswa = null;

					String akses = "";
					String idStr = idPeserta.toString();

					if (data[0] instanceof PertemuanFileContent) {
						PertemuanFileContent pertemuanFileContent = (PertemuanFileContent) data[0];
						d = pertemuan.ambilData("bahan_perkulaiahan_" + pertemuanFileContent.getId(), idStr);
						akses = pertemuanFileContent.getNama();

					} else if (data[0] instanceof Tugas) {
						Tugas tugas = (Tugas) data[0];
						d = tugas.ambilData("tugas", idStr);
						akses = tugas.getJudultugas();

						TreeMap<Long, TugasFileContent> tugasFileContentsa = tugases.get(key);
						if (tugasFileContentsa != null) {
							for (TugasFileContent tfc : tugasFileContentsa.values()) {
								if ((objPeserta instanceof Mahasiswa && tfc.getMahasiswa() != null
										&& tfc.getMahasiswa().equals(idPeserta))
										|| (objPeserta instanceof Siswa && tfc.getSiswa() != null
												&& tfc.getSiswa().equals(idPeserta))) {
									fileContent = tfc;
									break;
								}
							}
						}
					} else if (data[0] instanceof PertemuanPunyaUjian) {
						PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) data[0];
						if (pertemuanPunyaUjian.getUjian() != null && pertemuanPunyaUjian.getUjian().getAktif()) {
							akses = pertemuanPunyaUjian.getNama();
							d = pertemuan.ambilData("ujian_" + pertemuanPunyaUjian.getId(), idStr);

							List<Long> listHasilUjian = ujians.get(key);
							if (listHasilUjian != null) {
								for (Long ujianId : listHasilUjian) {
									HasilUjianMahasiswa hsl = (HasilUjianMahasiswa) GeneralValueObject
											.ambilData(HasilUjianMahasiswa.class, ujianId.toString());
									if (hsl != null) {
										if ((objPeserta instanceof Mahasiswa && hsl.getMahasiswa() != null
												&& hsl.getMahasiswa().getId().equals(idPeserta))
												|| (objPeserta instanceof Siswa && hsl.getSiswa() != null
														&& hsl.getSiswa().getId().equals(idPeserta))) {
											hasilUjianMahasiswa = hsl;
											break;
										}
									}
								}
							}
						}
					} else if (data[0] instanceof AudioPertemuan) {
						AudioPertemuan audioPertemuan = (AudioPertemuan) data[0];
						akses = audioPertemuan.getNama();
						d = pertemuan.ambilData("audio_" + audioPertemuan.getId(), idStr);
					} else if (data[0] instanceof VideoPertemuan) {
						VideoPertemuan videoPertemuan = (VideoPertemuan) data[0];
						akses = videoPertemuan.getNama();
						d = pertemuan.ambilData("video_" + videoPertemuan.getId(), idStr);
					}

					Integer qt = jmls.get(key) == null ? 0 : jmls.get(key);
					Integer qtMhs = jmlsMhs.get(idPeserta) == null ? 0 : jmlsMhs.get(idPeserta);
					Integer qtTgsMhs = jmlsTgsMhs.get(idPeserta) == null ? 0 : jmlsTgsMhs.get(idPeserta);
					Integer qtUjianMhs = jmlsUjianMhs.get(idPeserta) == null ? 0 : jmlsUjianMhs.get(idPeserta);

					Vbox vboxData = new Vbox();
					vboxData.setAlign("center");
					myRow.appendChild(vboxData);

					if ((d == null || d.isEmpty())
							&& (hasilUjianMahasiswa == null || hasilUjianMahasiswa.getMulaiPada() == null)
							&& (fileContent == null)) {
						Label lblNotAccess = new MyLabelKecil(Common.getBahasaConfig("Tidak Akses"));
						lblNotAccess.setStyle(
								"color:#dc3545; font-weight:bold; background-color:#f8d7da; padding:2px 6px; border-radius:4px;");
						vboxData.appendChild(lblNotAccess);
						map.put(key, Common.getBahasaConfig("Tidak Akses"));
					} else {
						if (akses != null && !akses.trim().isEmpty()) {
							if (absAkses.length() > 0)
								absAkses.append(", ");
							absAkses.append(akses);
						}
						qt++;
						qtMhs++;

						String jam = "";
						if ((d == null || d.isEmpty()) && hasilUjianMahasiswa != null
								&& hasilUjianMahasiswa.getMulaiPada() != null) {
							jam = Common.dateFormat3.get().format(hasilUjianMahasiswa.getMulaiPada());
						} else if ((d == null || d.isEmpty()) && fileContent != null) {
							jam = Common.dateFormat3.get().format(fileContent.getTanggal_dirubah());
						} else {
							jam = d.firstEntry().getValue();
						}

						map.put(key, jam);
						try {
							Date j = Common.dateFormat3.get().parse(jam);
							Label lblAccess = new MyLabelKecil(
									SmartDateTimeUtil.getDayString(j, null) + Common.dateFormat3.get().format(j));
							lblAccess.setStyle("color:#198754; font-size:10px;");
							vboxData.appendChild(lblAccess);
						} catch (Exception e) {
							vboxData.appendChild(new MyLabelKecil(jam));
						}
					}

					if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getMulaiPada() != null) {
						if (d == null || d.isEmpty()) {
							if (akses != null && !akses.trim().isEmpty()) {
								if (absAkses.length() > 0)
									absAkses.append(", ");
								absAkses.append(akses);
							}
							qt++;
							qtMhs++;
						}

						qtUjianMhs++;
						String strLama = hasilUjianMahasiswa.getLamaPengerjaan() == null ? ""
								: Common.getBahasaConfig(" selama ")
										+ Common.timeFormat1.get().format(hasilUjianMahasiswa.getLamaPengerjaan());
						String tglUjian = Common.getBahasaConfig("Mulai ")
								+ SmartDateTimeUtil.getDayString(hasilUjianMahasiswa.getMulaiPada(), null)
								+ Common.dateFormat.get().format(hasilUjianMahasiswa.getMulaiPada()) + strLama;

						map.put("ikut_ujian", tglUjian);
						if (absUjian.length() > 0)
							absUjian.append(", ");
						absUjian.append(hasilUjianMahasiswa.getPertemuanPunyaUjian().getNama());

						Label lblIkutUjian = new MyLabelKecil(Common.getBahasaConfig("Ikut Ujian: ") + tglUjian);
						lblIkutUjian.setStyle("color:#0d6efd; font-weight:bold; font-size:9px;");
						vboxData.appendChild(lblIkutUjian);

						String[] contents = new String[] {
								hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getJadwalUjianPMB() == null
										? "hasilUjianMahasiswa.mahasiswa.nim"
										: "hasilUjianMahasiswa.biodataCalonMahasiswa.noRegistrasi",
								hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getJadwalUjianPMB() == null
										? "hasilUjianMahasiswa.mahasiswa.nama"
										: "hasilUjianMahasiswa.biodataCalonMahasiswa.nama",
								"bankSoal.soal-text", "bankSoalDetail.huruf", "bankSoalDetail.jawaban",
								"bankSoalDetail.betul", "nilai", "jawaban", "koreksi", "waktuJawab",
								"hasilUjianMahasiswa" };

						final HasilUjianMahasiswa hslFinal = hasilUjianMahasiswa;
						MyToolbarbuttonConfig downloadButton = (MyToolbarbuttonConfig) Common
								.cetakData(new DataCriteria() {
									@Override
									public Object initCriteria(boolean order) {
										Session session = HibernateUtil.currentSession();
										Criteria criteria = session.createCriteria(HasilUjianMahasiswaDetail.class)
												.add(Restrictions.eq("hasilUjianMahasiswa", hslFinal));
										if (order)
											criteria.addOrder(Order.asc("id"));
										return criteria;
									}
								}, contents);

						downloadButton.setSclass("fas fa-file-excel");
						downloadButton.setLabel(Common.getBahasaConfig("Unduh Hasil Ujian"));
						downloadButton.setStyle("font-size:9px; margin-top:5px;");

						boolean bolehUnduhUjian = false;
						if (tbmuser != null) {
							if (tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
									&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null) {
								bolehUnduhUjian = true;
							} else if (objPeserta instanceof Mahasiswa && tbmuser.getMahasiswa() != null
									&& tbmuser.getMahasiswa().getId().equals(hslFinal.getMahasiswa().getId())) {
								bolehUnduhUjian = true;
							} else if (objPeserta instanceof Siswa && tbmuser.getSiswa() != null
									&& tbmuser.getSiswa().getId().equals(hslFinal.getSiswa().getId())) {
								bolehUnduhUjian = true;
							}
						}
						downloadButton.setVisible(bolehUnduhUjian);
						vboxData.appendChild(downloadButton);
					}

					if (fileContent != null) {
						qtTgsMhs++;
						String tglTugas = fileContent.getUploadDate() == null ? ""
								: SmartDateTimeUtil.getDayString(fileContent.getUploadDate(), null)
										+ Common.dateFormat.get().format(fileContent.getUploadDate());
						map.put("upload_tugas", tglTugas);

						String namaFile = "";
						if (objPeserta instanceof Mahasiswa)
							namaFile = fileContent.ambilRealNameSesuaiDenganNIM((Mahasiswa) objPeserta);
						else if (objPeserta instanceof Siswa)
							namaFile = fileContent.getNama(); // Fallback untuk siswa

						if (absTugas.length() > 0)
							absTugas.append(", ");
						absTugas.append(namaFile);

						Label lblTugas = new MyLabelKecil(Common.getBahasaConfig("Unggah Tugas: ") + tglTugas);
						lblTugas.setStyle("color:#0d6efd; font-weight:bold; font-size:9px;");
						vboxData.appendChild(lblTugas);

						final TugasFileContent tugasFileContent = fileContent;
						MyToolbarbutton btnTugasDownload = new MyToolbarbutton("fas fa-download", namaFile);
						btnTugasDownload.setStyle("font-size:9px; margin-top:5px;");

						boolean bolehUnduhTugas = false;
						if (tbmuser != null) {
							if (tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
									&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null) {
								bolehUnduhTugas = true;
							} else if (objPeserta instanceof Mahasiswa && tbmuser.getMahasiswa() != null
									&& tbmuser.getMahasiswa().getId().equals(tugasFileContent.getMahasiswa())) {
								bolehUnduhTugas = true;
							} else if (objPeserta instanceof Siswa && tbmuser.getSiswa() != null
									&& tbmuser.getSiswa().getId().equals(tugasFileContent.getSiswa())) {
								bolehUnduhTugas = true;
							}
						}

						btnTugasDownload.setVisible(bolehUnduhTugas);
						btnTugasDownload.setTooltiptext(
								Common.getBahasaConfig("Lihat / Unduh") + " \"" + tugasFileContent.getNama() + "\"");
						vboxData.appendChild(btnTugasDownload);

						btnTugasDownload.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (tugasFileContent.getGdrive() != null) {
									tugasFileContent.tampilGDrive(null);
								} else {
									String link = tugasFileContent.getLink();
									if (link == null || link.trim().isEmpty() || !link.startsWith("http")) {
										link = tugasFileContent.createLinkUri();
									}
									if (link != null && !link.trim().isEmpty()) {
										if (tugasFileContent.bisaPreview()) {
											Common.displayWindow(tugasFileContent.merupakanGambar(), link, true, "95%",
													"95%", true, tugasFileContent);
										} else {
											org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrent().sendRedirect(link,
													"_blank");
										}
									} else {
										MyMessageboxConfig.show(
												"Mohon maaf, berkas tugas yang Bapak/Ibu akses saat ini tidak dapat ditemukan pada sistem. Langkah yang dapat dilakukan: (1) mohon menunggu beberapa saat dan mencoba kembali karena berkas kemungkinan masih dalam proses pengunggahan; (2) mohon memastikan koneksi internet Bapak/Ibu dalam keadaan stabil; (3) apabila berkas tetap tidak dapat dibuka, mohon menghubungi pengajar atau administrator sistem agar berkas tersebut dapat diunggah ulang.",
												"Peringatan", MyMessageboxConfig.OK,
												MyMessageboxConfig.EXCLAMATION);
									}
								}
							}
						});
					} else if (data[0] instanceof Tugas) {
						Label lblNoTugas = new MyLabelKecil(Common.getBahasaConfig("Tidak Unggah Tugas"));
						lblNoTugas.setStyle("color:#dc3545; font-size:9px;");
						vboxData.appendChild(lblNoTugas);
					}

					jmls.put(key, qt);
					jmlsMhs.put(idPeserta, qtMhs);
					jmlsTgsMhs.put(idPeserta, qtTgsMhs);
					jmlsUjianMhs.put(idPeserta, qtUjianMhs);
				}

				map.put("abs",
						Common.getBahasaConfig("Akses: ") + absAkses.toString() + "\n"
								+ Common.getBahasaConfig("Tugas: ") + absTugas.toString() + "\n"
								+ Common.getBahasaConfig("Ujian: ") + absUjian.toString());

				Integer totalDiakses = jmlsMhs.get(idPeserta) == null ? 0 : jmlsMhs.get(idPeserta);
				Integer qtTgsMhs = jmlsTgsMhs.get(idPeserta) == null ? 0 : jmlsTgsMhs.get(idPeserta);
				Integer qtUjianMhs = jmlsUjianMhs.get(idPeserta) == null ? 0 : jmlsUjianMhs.get(idPeserta);

				jmls.put("totalDiakses", totalDiakses);
				jmls.put("totTgs", totTgs);
				jmls.put("totUjian", totUjian);
				jmls.put("qtTgsMhs", qtTgsMhs);
				jmls.put("qtUjianMhs", qtUjianMhs);

				String hsl = Common.getBahasaConfig("Akses: ") + Common.numberFormat.get().format(totalDiakses) + "/"
						+ Common.numberFormat.get().format(totalMateri) + " (" + Common.numberFormat.get()
								.format(totalMateri == 0 ? 0 : ((totalDiakses * 100.0) / totalMateri))
						+ "%)";
				String hslTugas = Common.getBahasaConfig("Tugas: ") + Common.numberFormat.get().format(qtTgsMhs) + "/"
						+ Common.numberFormat.get().format(totTgs) + " ("
						+ Common.numberFormat.get().format(totTgs == 0 ? 0 : ((qtTgsMhs * 100.0) / totTgs)) + "%)";
				String hslUjian = Common.getBahasaConfig("Ujian: ") + Common.numberFormat.get().format(qtUjianMhs) + "/"
						+ Common.numberFormat.get().format(totUjian) + " ("
						+ Common.numberFormat.get().format(totUjian == 0 ? 0 : ((qtUjianMhs * 100.0) / totUjian))
						+ "%)";

				map.put("video", totalMateri == 0 ? 0 : ((totalDiakses * 100.0) / totalMateri));
				map.put("audio", totTgs == 0 ? 0 : ((qtTgsMhs * 100.0) / totTgs));
				map.put("bahan_perkulaiahan", totUjian == 0 ? 0 : ((qtUjianMhs * 100.0) / totUjian));

				Vbox vboxStat = new Vbox();
				vboxStat.setAlign("start");
				myRow.appendChild(vboxStat);

				Label l1 = new MyLabelKecil(hsl);
				l1.setStyle("font-weight:bold; color:#0d6efd;");
				vboxStat.appendChild(l1);
				Label l2 = new MyLabelKecil(hslTugas);
				l2.setStyle("font-weight:bold; color:#198754;");
				vboxStat.appendChild(l2);
				Label l3 = new MyLabelKecil(hslUjian);
				l3.setStyle("font-weight:bold; color:#ffc107;");
				vboxStat.appendChild(l3);

				map.put("hsl", hsl);
				map.put("hslTugas", hslTugas);
				map.put("hslUjian", hslUjian);
				maps.add(map);
			}

			tugases = null;
			ujians = null;

			org.zkoss.zul.Foot foot = new org.zkoss.zul.Foot();
			foot.setParent(grid);
			org.zkoss.zul.Footer footer = new org.zkoss.zul.Footer();
			footer.setParent(foot);
			footer = new org.zkoss.zul.Footer(Common.getBahasaConfig("Total Keseluruhan"));
			footer.setStyle("font-weight:bold; text-align:right;");
			footer.setParent(foot);

			for (String key : materis.keySet()) {
				try {
					Integer j = jmls.get(key) == null ? 0 : jmls.get(key);
					footer = new org.zkoss.zul.Footer(Common.numberFormat.get().format(j));
					footer.setStyle("font-weight:bold; text-align:center;");
					footer.setParent(foot);
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}

			try {
				Integer totalDiakses = jmls.get("totalDiakses") == null ? 0 : jmls.get("totalDiakses");
				footer = new org.zkoss.zul.Footer(Common.numberFormat.get().format(totalDiakses));
				footer.setStyle("font-weight:bold; text-align:center;");
				footer.setParent(foot);
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			ais.ui.util.UIUtil.checkGrigMobile(grid);
		}

		// =============================================================================================
		// BAGIAN 2: RENDER GRID PENGAJAR (DOSEN & GURU)
		// =============================================================================================
		if (!daftarPengajar.isEmpty() && materis != null
				&& (tbmuser == null || (tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null))) {
			int totalMateri = materis.size();
			boolean fix = materis.size() <= 5;

			Grid grid = new Grid();
			grid.setSclass("dgrid fgrid");
			grid.setWidth(fix ? "100%" : Common.isMobile() ? "300px" : "1200px");
			ais.ui.util.ZkCompat.setFixedLayout(grid, fix);
			grid.setParent(rowDosen);
			grid.setStyle(
					"background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); margin-top: 15px;");

			Columns columns = new Columns();
			columns.setParent(grid);

			Column column = new Column(Common.getBahasaConfig("No."));
			column.setWidth("50px");
			column.setStyle("font-weight:bold; background-color:#e9ecef; text-align:center;");
			column.setParent(columns);

			column = new Column(Common.getBahasaConfig("Tenaga Pengajar"));
			column.setParent(columns);
			column.setWidth(fix ? "25%" : "280px");
			column.setStyle("font-weight:bold; background-color:#e9ecef;");

			ProfileUtil.tampilkanMateriColum(columns, materis);

			column = new Column(Common.getBahasaConfig("Total Statistik"));
			column.setWidth("140px");
			column.setStyle("font-weight:bold; background-color:#e9ecef; text-align:center;");
			column.setParent(columns);

			Rows rows = new Rows();

			rows.setParent(grid);

			int nomor = 1;
			TreeMap<String, Integer> jmls = new TreeMap<String, Integer>();
			TreeMap<Long, Integer> jmlsPengajar = new TreeMap<Long, Integer>();

			for (Object objPengajar : daftarPengajar) {
				Map<String, Object> map = new java.util.HashMap<String, Object>();
				MyFormRow myRow = new MyFormRow();
				myRow.setParent(rows);

				Label lblNomor = new Label(Common.numberFormat.get().format(nomor++));
				lblNomor.setStyle("font-weight:bold; text-align:center; display:block;");
				myRow.appendChild(lblNomor);

				Long idPengajar = null;
				String noInduk = "";
				String namaPengajar = "";
				String telpPengajar = "";
				String emailPengajar = "";
				String jenisPengajar = "";

				Hbox hboxProfile = new Hbox();
				hboxProfile.setSpacing("8px");
				hboxProfile.setParent(myRow);

				Vbox vboxDataProfile = new Vbox();

				try {
					if (objPengajar instanceof Dosen) {
						Dosen dsn = (Dosen) objPengajar;
						idPengajar = dsn.getId();
						noInduk = dsn.getNidn() != null ? dsn.getNidn() : "";
						namaPengajar = dsn.getNama();
						telpPengajar = dsn.getTelp();
						emailPengajar = dsn.getEmail();
						jenisPengajar = "Dosen";
						CommonMedia.tampilkanGambarKecil(dsn).setParent(hboxProfile);
						dsn.tampilkanHp(vboxDataProfile);
						dsn.tampilkanEmail(vboxDataProfile);
					} else if (objPengajar instanceof Guru) {
						Guru gr = (Guru) objPengajar;
						idPengajar = gr.getId();
						noInduk = gr.getNip() != null ? gr.getNip() : "";
						namaPengajar = gr.getNama();
						telpPengajar = gr.getTeleponGuru();
						emailPengajar = gr.getAlamatEmail();
						jenisPengajar = "Guru";
						CommonMedia.tampilkanGambarKecil(gr).setParent(hboxProfile);
						gr.tampilkanHp(vboxDataProfile);
						gr.tampilkanEmail(vboxDataProfile);
					}

					vboxDataProfile.setParent(hboxProfile);
					MyLabelAgakKecil lblNoInduk = new MyLabelAgakKecil(noInduk);
					lblNoInduk.setStyle("font-weight:bold; color:#0d6efd;");
					lblNoInduk.setParent(vboxDataProfile);
					new MyLabelAgakKecil(namaPengajar).setParent(vboxDataProfile);
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}

				map.put("jurusan", vopemblajaran.infoSimple());
				map.put("jenis", jenisPengajar);
				map.put("nim", noInduk);
				map.put("nama", namaPengajar);
				map.put("telp", telpPengajar);
				map.put("email", emailPengajar);

				StringBuilder absAkses = new StringBuilder();

				for (String key : materis.keySet()) {
					Object[] data = materis.get(key);
					Pertemuan pertemuan = (Pertemuan) data[1];
					TreeMap<String, String> d = null;
					String akses = "";
					String idStr = idPengajar.toString();

					if (data[0] instanceof PertemuanFileContent) {
						PertemuanFileContent pertemuanFileContent = (PertemuanFileContent) data[0];
						d = pertemuan.ambilData("bahan_perkulaiahan_" + pertemuanFileContent.getId(), idStr);
						akses = pertemuanFileContent.getNama();
					} else if (data[0] instanceof Tugas) {
						Tugas tugas = (Tugas) data[0];
						d = tugas.ambilData("tugas", idStr);
						akses = tugas.getJudultugas();
					} else if (data[0] instanceof PertemuanPunyaUjian) {
						PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) data[0];
						d = pertemuan.ambilData("ujian_" + pertemuanPunyaUjian.getId(), idStr);
						akses = pertemuanPunyaUjian.getNama();
					} else if (data[0] instanceof AudioPertemuan) {
						AudioPertemuan audioPertemuan = (AudioPertemuan) data[0];
						akses = audioPertemuan.getNama();
						d = pertemuan.ambilData("audio_" + audioPertemuan.getId(), idStr);
					} else if (data[0] instanceof VideoPertemuan) {
						VideoPertemuan videoPertemuan = (VideoPertemuan) data[0];
						akses = videoPertemuan.getNama();
						d = pertemuan.ambilData("video_" + videoPertemuan.getId(), idStr);
					}

					Integer qt = jmls.get(key) == null ? 0 : jmls.get(key);
					Integer qtPengajar = jmlsPengajar.get(idPengajar) == null ? 0 : jmlsPengajar.get(idPengajar);

					Vbox vboxData = new Vbox();
					vboxData.setAlign("center");
					myRow.appendChild(vboxData);

					if (d == null || d.isEmpty()) {
						Label lblNotAccess = new MyLabelKecil(Common.getBahasaConfig("Tidak Akses"));
						lblNotAccess.setStyle(
								"color:#dc3545; font-weight:bold; background-color:#f8d7da; padding:2px 6px; border-radius:4px;");
						vboxData.appendChild(lblNotAccess);
						map.put(key, Common.getBahasaConfig("Tidak Akses"));
					} else {
						if (akses != null && !akses.trim().isEmpty()) {
							if (absAkses.length() > 0)
								absAkses.append(", ");
							absAkses.append(akses);
						}

						qt++;
						qtPengajar++;
						String jam = d.firstEntry().getValue();
						map.put(key, jam);
						try {
							Date j = Common.dateFormat3.get().parse(jam);
							Label lblAccess = new MyLabelKecil(
									SmartDateTimeUtil.getDayString(j, null) + Common.dateFormat3.get().format(j));
							lblAccess.setStyle("color:#198754; font-size:10px;");
							vboxData.appendChild(lblAccess);
						} catch (Exception e) {
							vboxData.appendChild(new MyLabelKecil(jam));
						}
					}
					jmls.put(key, qt);
					jmlsPengajar.put(idPengajar, qtPengajar);
				}

				Integer totalDiakses = jmlsPengajar.get(idPengajar) == null ? 0 : jmlsPengajar.get(idPengajar);
				jmls.put("totalDiakses", totalDiakses);

				String hsl = Common.getBahasaConfig("Akses: ") + Common.numberFormat.get().format(totalDiakses) + "/"
						+ Common.numberFormat.get().format(totalMateri) + " (" + Common.numberFormat.get()
								.format(totalMateri == 0 ? 0 : ((totalDiakses * 100.0) / totalMateri))
						+ "%)";

				Vbox vboxStat = new Vbox();
				vboxStat.setAlign("center");
				myRow.appendChild(vboxStat);

				Label l1 = new MyLabelKecil(hsl);
				l1.setStyle("font-weight:bold; color:#0d6efd;");
				vboxStat.appendChild(l1);

				map.put("hsl", hsl);
				map.put("abs", Common.getBahasaConfig("Akses: ") + absAkses.toString());
				map.put("video", totalMateri == 0 ? 0 : ((totalDiakses * 100.0) / totalMateri));

				maps.add(map);
			}

			org.zkoss.zul.Foot foot = new org.zkoss.zul.Foot();
			foot.setParent(grid);
			org.zkoss.zul.Footer footer = new org.zkoss.zul.Footer();
			footer.setParent(foot);
			footer = new org.zkoss.zul.Footer(Common.getBahasaConfig("Total Keseluruhan"));
			footer.setStyle("font-weight:bold; text-align:right;");
			footer.setParent(foot);

			for (String key : materis.keySet()) {
				Integer j = jmls.get(key) == null ? 0 : jmls.get(key);
				footer = new org.zkoss.zul.Footer(Common.numberFormat.get().format(j));
				footer.setStyle("font-weight:bold; text-align:center;");
				footer.setParent(foot);
			}

			Integer totalDiakses = jmls.get("totalDiakses") == null ? 0 : jmls.get("totalDiakses");
			footer = new org.zkoss.zul.Footer(Common.numberFormat.get().format(totalDiakses));
			footer.setStyle("font-weight:bold; text-align:center;");
			footer.setParent(foot);

			ais.ui.util.UIUtil.checkGrigMobile(grid);
		}

		// =============================================================================================
		// BAGIAN 3: TOMBOL EKSPOR KE PDF (JASPER REPORT)
		// =============================================================================================
		MyToolbarbutton btnCetak = new MyToolbarbutton("fas fa-print", Common.getBahasaConfig("Cetak PDF"));
		btnCetak.setStyle("font-weight:bold; margin-top:10px;");
		btnCetak.addEventListener("onClick", new EventListener() {
			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {
				Map parameters = ais.common.HashMapGenerator.getRand();
				parameters.put("maps", maps);
				@SuppressWarnings("unused")
				org.zkoss.zul.Tabbox tabbox = ais.action.report.Report.generatePDFReportKembaliTab(
						ais.action.report.Report.PDF, new Map[] { parameters },
						new String[] { "Keaktifan_Peserta_Perkuliahan" },
						new String[] { Common.getBahasaConfig("Keaktifan Peserta Pembelajaran") },
						ais.ui.util.WaktuUtil.getDate());
			}
		});
		btnCetak.setParent(toolbarButtonMahasiswa);
		materis = null;
	}

	public static void displayTreeMap(TreeMap<String, String> d) throws Exception {
		displayTreeMap(d, null, null, null);
	}

	@SuppressWarnings("deprecation")
	public static void displayTreeMap(TreeMap<String, String> d, final List<String> ygBelumAkses, GeneralValueObject vo,
			final String jns) throws Exception {

		final List<Map.Entry<String, String>> elements = new LinkedList<Map.Entry<String, String>>(d.entrySet());
		Collections.sort(elements, new Comparator<Map.Entry<String, String>>() {

			public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
				try {
					Date b1 = Common.dateFormat3.get().parse(o2.getValue());
					Date b2 = Common.dateFormat3.get().parse(o1.getValue());
					return b1.compareTo(b2);
				} catch (Exception e) {
					return o2.getValue().compareTo(o1.getValue());
				}
			}

		});

		final MyWindow window = new MyWindow("", "none", false);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight(!Common.isMobile() ? "450px" : "98%");
		window.setWidth(!Common.isMobile() ? "700px" : "98%");

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(window);

		List<String> adaData = new ArrayList<String>();

		Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);
		if (ygBelumAkses != null) {

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(center);

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			int jumlah = ygBelumAkses.size();
			int akses = d.size();
			int belumAkses = (jumlah - d.size());

			Tab tab = new Tab(
					"Daftar yang mengakses (" + akses + " / "
							+ Common.numberFormat.get().format((akses * 100.0) / jumlah) + "%)",
					"/img/online-icon_access.png");
			tab.setParent(tabs);

			Tab tabBelumMengakses = new Tab(
					"Daftar yang belum mengakses (" + belumAkses + " / "
							+ Common.numberFormat.get().format((belumAkses * 100.0) / jumlah) + "%)",
					"/img/offline-icon.png");
			tabBelumMengakses.setParent(tabs);

			Tab tabProsentase = new Tab("Prosentase Akses", "/img/chart-pie-icon.png");
			tabProsentase.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			tabpanel.setParent(tabpanels);

			Borderlayout myborderlayout = new Borderlayout();
			myborderlayout.setParent(tabpanel);

			center = new Center();
			center.setParent(myborderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			Tabpanel tabpanelBelumAkses = new ais.ui.util.MyTabpanel();
			tabpanelBelumAkses.setParent(tabpanels);

			Borderlayout myborderlayoutBelumAkses = new Borderlayout();
			myborderlayoutBelumAkses.setParent(tabpanelBelumAkses);

			Center centerBelumAkses = new Center();
			centerBelumAkses.setParent(myborderlayoutBelumAkses);
			ais.ui.util.ZkCompat.setFlex(centerBelumAkses, true);

			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setWidth("100%");
			ais.ui.util.ZkCompat.setFixedLayout(grid, true);
			grid.setParent(centerBelumAkses);

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig("No.");
			column.setParent(columns);
			column.setWidth("40px");

			column = new MyColumnConfig("Belum mengakses");
			column.setParent(columns);

			Rows rows = new Rows();

			rows.setParent(grid);
			int no = 1;
			for (String key : ygBelumAkses) {
				String[] u = key.split("-", 3);

				try {
					adaData.add(u[2] + "-" + u[1]);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:9526");
					// TODO: handle exception
				}

				String jenis = u[2];
				try {
					boolean ada = false;
					for (Map.Entry<String, String> ds : elements) {
						String key1 = ds.getKey();
						String[] u1 = key1.split("-", 3);
						String jenis1 = u1[2];

						if (jenis1.equals(jenis) && u1[1].equals(u[1])) {
							ada = true;
							break;
						}
					}
					if (ada) {
						continue;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:9546");
//					ais.common.Common.tampilErrorJikaAdmin(e);
				}

				Hbox hbox = new Hbox();
				A image = new A();

				if (u[2].startsWith("CalonSiswa")) {
					image = CommonMedia.tampilkanGambarKecil(new CalonSiswa(Long.parseLong(u[1])));
					jenis = "Calon Siswa";
				} else if (u[2].startsWith("CalonMahasiswa")) {
					image = CommonMedia.tampilkanGambarKecil(new BiodataCalonMahasiswa(Long.parseLong(u[1])));
					jenis = "Calon Mahasiswa";
				} else if (u[2].startsWith("Mahasiswa")) {
					image = CommonMedia.tampilkanGambarKecil(new Mahasiswa(Long.parseLong(u[1])));
					jenis = "Mahasiswa";
				} else if (u[2].startsWith("Dosen")) {
					image = CommonMedia.tampilkanGambarKecil(new Dosen(Long.parseLong(u[1])));
					jenis = "Dosen";
				} else if (u[2].startsWith("Guru")) {
					image = CommonMedia.tampilkanGambarKecil(new Guru(Long.parseLong(u[1])));
					jenis = "Guru";
				} else if (u[2].startsWith("Siswa")) {
					image = CommonMedia.tampilkanGambarKecil(new Siswa(Long.parseLong(u[1])));
					jenis = "Siswa";
				}
				image.setHeight("62px");
				image.setParent(hbox);
				String sisa = u[2].replaceFirst(jenis, "");

				Vbox vbox = new Vbox();
				vbox.setParent(hbox);
				new Label(u[0]).setParent(vbox);
				new Label(jenis).setParent(vbox);

				new MyLabelAgakKecil(sisa).setParent(vbox);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				new Label(Common.numberFormat.get().format(no++)).setParent(row);
				row.setParent(rows);
				row.appendChild(hbox);

			}

			Tabpanel tabpanelProsentase = new ais.ui.util.MyTabpanel();
			tabpanelProsentase.setParent(tabpanels);

			Borderlayout myborderlayoutProsentase = new Borderlayout();
			myborderlayoutProsentase.setParent(tabpanelProsentase);

			Center centerProsentase = new Center();
			centerProsentase.setParent(myborderlayoutProsentase);
			ais.ui.util.ZkCompat.setFlex(centerProsentase, true);
				centerProsentase.appendChild(buildElearningHtmlPie("Persentase Akses Materi", "Menunjukkan jumlah peserta yang sudah membuka materi dibandingkan seluruh peserta.", akses, jumlah));
		}

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		ais.ui.util.ZkCompat.setFixedLayout(grid, true);
		grid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("No.");
		column.setParent(columns);
		column.setWidth("40px");

		column = new MyColumnConfig("Diakses oleh");
		column.setParent(columns);

		column = new MyColumnConfig("Waktu akses");
		column.setParent(columns);
		column.setWidth("40%");

		Rows rows = new Rows();

		rows.setParent(grid);

		int no = 1;
		for (Map.Entry<String, String> ds : elements) {

			try {
				String key = ds.getKey();
				String jam = ds.getValue();
				String[] u = key == null ? new String[0] : key.split("-", 3);
				if (u.length < 3) {
					continue;
				}

				Hbox hbox = new Hbox();
				A image = new A();
				String jenis = u[2];

				if (adaData.isEmpty() || adaData.contains(u[2] + "-" + u[1])) {

					if (u[2].startsWith("CalonSiswa")) {
						image = CommonMedia.tampilkanGambarKecil(new CalonSiswa(Long.parseLong(u[1])));
						jenis = "Calon Siswa";
					} else if (u[2].startsWith("CalonMahasiswa")) {
						image = CommonMedia.tampilkanGambarKecil(new BiodataCalonMahasiswa(Long.parseLong(u[1])));
						jenis = "Calon Mahasiswa";
					} else if (u[2].startsWith("Mahasiswa")) {
						image = CommonMedia.tampilkanGambarKecil(new Mahasiswa(Long.parseLong(u[1])));
						jenis = "Mahasiswa";
					} else if (u[2].startsWith("Dosen")) {
						image = CommonMedia.tampilkanGambarKecil(new Dosen(Long.parseLong(u[1])));
						jenis = "Dosen";
					} else if (u[2].startsWith("Guru")) {
						image = CommonMedia.tampilkanGambarKecil(new Guru(Long.parseLong(u[1])));
						jenis = "Guru";
					} else if (u[2].startsWith("Siswa")) {
						image = CommonMedia.tampilkanGambarKecil(new Siswa(Long.parseLong(u[1])));
						jenis = "Siswa";
					}
					image.setHeight("62px");
					image.setParent(hbox);

					String sisa = u[2].replaceFirst(jenis, "");

					Vbox vbox = new Vbox();
					vbox.setParent(hbox);
					new Label(u[0]).setParent(vbox);
					new Label(jenis).setParent(vbox);

					new MyLabelAgakKecil(sisa).setParent(vbox);

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					new Label(Common.numberFormat.get().format(no++)).setParent(row);
					row.setParent(rows);
					row.appendChild(hbox);

					try {
						Date j = Common.dateFormat3.get().parse(jam);
						row.appendChild(new Label(
								(SmartDateTimeUtil.getDayString(j, null) + Common.dateFormat5.get().format(j))));
					} catch (Exception e) {
						row.appendChild(new Label(jam));
					}
				}

			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		}

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		Tbmuser tbmuser = Common.getCurrentUser();

		if (vo != null && vo instanceof Pertemuan && jns != null) {
			final Pertemuan pertemuan = (Pertemuan) vo;
			Toolbarbutton masuk = new MyToolbarbuttonConfig(jns + " dianggap hadir", "/img/svg/check2.svg");
			masuk.setStyle("font-size:11px;");
			masuk.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
					&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null
					&& pertemuan.getJadwalUjianPMB() == null && pertemuan.getJadwalUjianPSB() == null);
			masuk.setTooltiptext("Mahasiswa dan dosen yang akses");
			masuk.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					PertemuanPunyaDiskusiHelper.aksesDianggapHadir(pertemuan, jns, jns + " Pertemuan", null, null,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									new PertemuanHelper(null, null).display(pertemuan, new DataLoader() {

										@Override
										public void loadData(Object value) {
											window.detach();
										}
									}, 0);
								}
							});

				}
			});
			masuk.setParent(toolbar);

			masuk = new MyToolbarbuttonConfig("tidak " + jns + " dianggap alpa", "/img/Button-Delete-icon.png");
			masuk.setStyle("font-size:11px;");
			masuk.setParent(toolbar);
			masuk.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
					&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null
					&& pertemuan.getJadwalUjianPMB() == null && pertemuan.getJadwalUjianPSB() == null);
			masuk.setTooltiptext("Semua mahasiswa yang belum absen dianggap Alpa");
			masuk.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin akan menetapkan seluruh mahasiswa dan dosen yang belum melakukan absensi sebagai Alpa? Mohon diperhatikan bahwa tindakan ini akan langsung memengaruhi rekapitulasi kehadiran yang bersangkutan. Silakan pilih OK untuk melanjutkan, atau Batal untuk membatalkan proses ini.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												if (pertemuan.getId() != null) {
													HibernateUtil.currentSession().refresh(pertemuan);
												}

												for (String key : ygBelumAkses) {
													try {
														String[] u = key.split("-", 3);
														String jenis = u[2];
														try {
															boolean ada = false;
															for (Map.Entry<String, String> ds : elements) {
																String key1 = ds.getKey();
																String[] u1 = key1.split("-", 3);
																String jenis1 = u1[2];

																if (jenis1.equals(jenis) && u1[1].equals(u[1])) {
																	ada = true;
																	break;
																}
															}
															if (ada) {
																continue;
															}
														} catch (Exception e) {
															ais.common.Common.tampilErrorJikaAdmin(e);
														}

														Long id = Long.parseLong(u[1]);
														Statusabsensi statusabsensi = ConstantValues.TIDAK_ADA_ALASAN;
														pertemuan.populate(id, statusabsensi,
																"Otomatis dijadikan alpa karena tidak " + jns, null,
																pertemuan.getWaktuMulai(), pertemuan.getWaktuSelesai(),
																"Mahasiswa");

													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}
												}

												Common.refreshUpdate(pertemuan);

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														new PertemuanHelper(null, null).display(pertemuan,
																new DataLoader() {

																	@Override
																	public void loadData(Object value) {
																		window.detach();
																	}
																}, 0);
													}
												});
											}
										});

									}

								}
							});

				}
			});

		}

		window.onModal();
		d = null;
	}

	@SuppressWarnings({})
	public static void tampilkanRekapPerkuliahan(final Perkuliahan perkuliahan, final Component finalRowPertemuan,
			final Component toolbarButtonMahasiswa) throws Exception {
		List<Perkuliahan> per = new ArrayList<Perkuliahan>();
		per.add(perkuliahan);
		tampilkanRekapPerkuliahan(per, finalRowPertemuan, toolbarButtonMahasiswa);
	}

	@SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
	public static void tampilkanRekapPerkuliahan(final List<Perkuliahan> per, final Component finalRowPertemuan,
			final Component toolbarButtonMahasiswa) throws Exception {
		final MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
		final DataLoader dataLoader = new DataLoader() {

			@Override
			public void loadData(Object value) {
				button.detach();
				if (finalRowPertemuan != null) {
					Common.clear(finalRowPertemuan);
				}
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						tampilkanRekapPerkuliahan(per, finalRowPertemuan, toolbarButtonMahasiswa);
					}
				});
			}
		};

		Rows rowsBaruPertemuan = null;
		if (per.size() > 1) {
			Grid gridBaruMahasiswa = new Grid();
			gridBaruMahasiswa.setSclass("fgrid");
			gridBaruMahasiswa.setOddRowSclass("non-odd");
			gridBaruMahasiswa.setParent(finalRowPertemuan);

			rowsBaruPertemuan = new Rows();
			rowsBaruPertemuan.setParent(gridBaruMahasiswa);
		}

		final List<Map> maps = new ArrayList<Map>();
		for (final Perkuliahan perkuliahan : per) {
			// Elemen per bisa null (mis. entri cache/ambilData gagal) -> lewati agar
			// perkuliahan.getId()/ambilPertemuan() di bawah tidak NPE.
			if (perkuliahan == null) {
				continue;
			}

			Dosen kaprodi = perkuliahan.getJurusan() == null ? null
					: perkuliahan.getJurusan().getKaprodi();
			final Map parameters = ais.common.HashMapGenerator.getRand();

			parameters.put("perkuliahan", perkuliahan.getId());
			parameters.put("tampil_nilai", "1");
			parameters.put("kaprodi",
					kaprodi == null ? "(                                          )" : kaprodi.getNama());
			parameters.put("nip", kaprodi == null ? "" : kaprodi.getCode());
			parameters.put("tanggal", Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));

			parameters.put("nama_kaprodi",
					kaprodi == null ? "(                                          )" : kaprodi.getNama());
			parameters.put("nip_kaprodi", kaprodi == null || kaprodi.getCode() == null ? "" : kaprodi.getCode().trim());

			parameters.put("nidn_kaprodi", kaprodi == null || kaprodi.getNidn() == null ? "" : kaprodi.getNidn());

			TreeMap<String, Long> pertemuanss = perkuliahan.ambilPertemuan();
			List<Pertemuan> pertemuans = new ArrayList<Pertemuan>();
			for (Long pertemuanid : pertemuanss.values()) {
				Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
				if (pertemuan != null) {
					pertemuans.add(pertemuan);
				}
			}
			pertemuanss.clear();
			pertemuanss = null;

			Collection<Long> collection = perkuliahan.ambilDetailperkuliahan();

			if (perkuliahan.getPerkuliahan_paralel() != null) {
				collection = perkuliahan.getPerkuliahan_paralel().ambilDetailperkuliahan();
			}

			if (true) {
				MyGrid grid = new MyGrid();
				grid.setSclass("fgrid");
				grid.setWidth("100%");
				ais.ui.util.ZkCompat.setFixedLayout(grid, true);
				if (rowsBaruPertemuan == null) {
					grid.setParent(finalRowPertemuan);
				} else {
					MyFormRow rowBaru = new MyFormRow();
					rowBaru.setParent(rowsBaruPertemuan);
					grid.setParent(rowBaru);
				}

				Columns columns = new Columns();
				columns.setParent(grid);

				Column column = new Column("No.");
				column.setWidth("40px");
				column.setParent(columns);

				column = new Column("Pertemuan");
				column.setParent(columns);

				column = new Column("Hdr.Dsn");
				column.setWidth("5%");
				column.setParent(columns);

				column = new Column("Hdr.Mhs");
				column.setWidth("5%");
				column.setParent(columns);

				column = new Column("Tgs");
				column.setWidth("4%");
				column.setParent(columns);

				column = new Column("Mat.");
				column.setWidth("4%");
				column.setParent(columns);

				column = new Column("Ujian");
				column.setWidth("4%");
				column.setParent(columns);

				column = new Column("Aud.");
				column.setWidth("4%");
				column.setParent(columns);

				column = new Column("Vid.");
				column.setWidth("4%");
				column.setParent(columns);

				column = new Column("Ikt.Ujn");
				column.setWidth("4%");
				column.setParent(columns);

				column = new Column("Ikt.Disk.");
				column.setWidth("4%");
				column.setParent(columns);

				column = new Column("Upld.Tgs.");
				column.setWidth("4%");
				column.setParent(columns);

				column = new Column("Lht.Tgs");
				column.setWidth("4%");
				column.setParent(columns);

				column = new Column("Vd.Conf.");
				column.setWidth("4%");
				column.setParent(columns);

				column = new Column("Aks.");
				column.setWidth("4%");
				column.setParent(columns);

				column = new Column("Lht.Vid");
				column.setWidth("4%");
				column.setParent(columns);

				column = new Column("Lht.Aud.");
				column.setWidth("4%");
				column.setParent(columns);

				column = new Column("Lht.Materi");
				column.setWidth("4%");
				column.setParent(columns);

				Rows rows = new Rows();

				rows.setParent(grid);

				if (per.size() > 1) {

					Group myRow = new ais.ui.util.MyGroupConfig();
					myRow.setParent(rows);

					Hbox v = ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahanUmum(myRow,
							perkuliahan, false, false, null);
					v.appendChild(new Space());
					v.appendChild(new MyLabelAgakKecilBold(perkuliahan.info()));
				}

				int qtyTugs = 0;
				int qtyMat = 0;
				int qtyUjian = 0;
				int qtyAud = 0;
				int qtyVid = 0;

				int ujian = 0;
				int diskusi = 0;
				int uplTugas = 0;
				int tugas = 0;
				int online = 0;
				int akses = 0;
				int video = 0;
				int audio = 0;
				int bahan_perkulaiahan = 0;

				for (final Pertemuan pertemuan : pertemuans) {
					if (pertemuan != null && pertemuan.getPerkuliahan() != null) {
						// Day day = new Day(pertemuan.getTanggal());
						Map<String, Object> map = new java.util.HashMap<String, Object>();
						MyFormRow myRow = new MyFormRow();
						myRow.setParent(rows);

						myRow.appendChild(new Label(Common.numberFormat.get().format(pertemuan.getPertemuanKe())));

						Hbox hbox = new Hbox();
						ais.ui.util.MenuAksiBaris.pasang(hbox);
						ais.ui.util.MenuAksiBaris.pasang(hbox);
						hbox.setParent(myRow);
						try {

							Vbox a = new Vbox();
							a.setParent(hbox);
							new MyLabelAgakKecil(pertemuan.getTopik()).setParent(a);
							new MyLabelAgakKecil(pertemuan.getCatatan()).setParent(a);
							new MyLabelAgakKecil((pertemuan.getTanggal() == null ? ""
									: Common.dateFormat4.get().format(pertemuan.getTanggal()))
									+ " "
									+ ((pertemuan.getWaktuMulai() == null ? "" : pertemuan.getWaktuMulai())
											+ (pertemuan.getWaktuSelesai() == null ? ""
													: " s.d " + pertemuan.getWaktuSelesai())))
									.setParent(a);
							new MyLabelAgakKecil(
									pertemuan.getJudultugas().isEmpty() ? "" : "Tgs:" + pertemuan.getJudultugas())
									.setParent(a);
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
						map.put("jurusan", perkuliahan.infoSimple());
						map.put("jenis", "Pertemuan");
						map.put("ke", pertemuan.getPertemuanKe());
						map.put("topik", pertemuan.getTopik());
						map.put("nama", pertemuan.getCatatan());
						map.put("telp",
								(pertemuan.getTanggal() == null ? ""
										: Common.dateFormat4.get().format(pertemuan.getTanggal()))
										+ " "
										+ ((pertemuan.getWaktuMulai() == null ? "" : pertemuan.getWaktuMulai())
												+ (pertemuan.getWaktuSelesai() == null ? ""
														: " s.d " + pertemuan.getWaktuSelesai())));

						Collection<TugasPertemuan> tugasPertemuans = pertemuan.ambilTugasPertemuanTotal().values();
						String jdl = pertemuan.getJudultugas().isEmpty() ? "" : pertemuan.getJudultugas();
						for (TugasPertemuan tugasPertemuan : tugasPertemuans) {
							jdl += jdl.isEmpty()
									? (tugasPertemuan.getJudultugas().isEmpty() ? "" : tugasPertemuan.getJudultugas())
									: "; " + (tugasPertemuan.getJudultugas().isEmpty() ? ""
											: tugasPertemuan.getJudultugas());
						}
						map.put("email", jdl);

						Object[] jml = perkuliahan.ambilJumlahPertemuanStatistik(pertemuan, collection, null);
						int jumlahUjianTotal = jml == null || jml[8] == null ? 0 : Integer.parseInt(jml[8].toString());
						int jumlahDiskusiTotal = jml == null || jml[9] == null ? 0
								: Integer.parseInt(jml[9].toString());

						ujian += jumlahUjianTotal;
						diskusi += jumlahDiskusiTotal;

						Map<String, Integer> statusesDosen = (Map<String, Integer>) (jml == null || jml[6] == null
								? null
								: jml[6]);
						String absDosen = statusesDosen == null ? ""
								: statusesDosen.toString().replaceAll("\\{", "").replaceAll("\\}", "").trim();
						A a;
						myRow.appendChild(a = new A(absDosen));
						a.setStyle("font-size:12px;");
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Map parametersBaru = new HashMap(parameters);
								List<Map<String, Serializable>> maps = CommonReportHelper
										.generateParameterMapAbsensiRinci(perkuliahan, null, pertemuan, null, false,
												true);
								parametersBaru.put("maps", maps);
								Report.generatePDFReport("pdf", parametersBaru, "LaporanAbsensiRinci",
										ais.ui.util.WaktuUtil.getDate(), Common.locale, null, null);
							}
						});

						map.put("abs_dsn", absDosen);

						Map<String, Integer> statuses = (Map<String, Integer>) (jml == null || jml[4] == null ? null
								: jml[4]);
						String abs = statuses == null ? ""
								: statuses.toString().replaceAll("\\{", "").replaceAll("\\}", "").trim();
						myRow.appendChild(a = new A(abs));

						map.put("abs", abs);
						a.setStyle("font-size:12px;");
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Map parametersBaru = new HashMap(parameters);
								List<Map<String, Serializable>> maps = CommonReportHelper
										.generateParameterMapAbsensiRinci(perkuliahan, null, pertemuan, null, true,
												false);
								parametersBaru.put("maps", maps);
								Report.generatePDFReport("pdf", parametersBaru, "LaporanAbsensiRinci",
										ais.ui.util.WaktuUtil.getDate(), Common.locale, null, null);
							}
						});

						int jmlTugs = pertemuan.getJudultugas().isEmpty() ? 0 : 1;
						jmlTugs += tugasPertemuans.size();

						myRow.appendChild(a = new A(Common.numberFormat.get().format(jmlTugs)));
						a.setStyle("font-size:12px;");
						map.put("tgs", jmlTugs);
						qtyTugs += jmlTugs;
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								new PertemuanHelper().display(pertemuan, dataLoader, 3);
							}
						});

						int mat = pertemuan.ambilJumlahPertemuanFileContent();
						myRow.appendChild(a = new A(Common.numberFormat.get().format(mat)));
						a.setStyle("font-size:12px;");
						map.put("mat", mat);
						qtyMat += mat;
						// tMat.addOrUpdate(day, mat);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								new PertemuanHelper().display(pertemuan, dataLoader, 2);
							}
						});

						int uj = pertemuan.ambilJumlahPertemuanPunyaUjian();
						myRow.appendChild(a = new A(Common.numberFormat.get().format(uj)));
						a.setStyle("font-size:12px;");
						map.put("uj", uj);
						qtyUjian += uj;
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								new PertemuanHelper().display(pertemuan, dataLoader, 6);
							}
						});

						int aud = pertemuan.ambilJumlahAudioPertemuan();
						myRow.appendChild(a = new A(Common.numberFormat.get().format(aud)));
						a.setStyle("font-size:12px;");
						map.put("aud", aud);
						qtyAud += aud;
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								new PertemuanHelper().display(pertemuan, dataLoader, 4);
							}
						});

						int vid = pertemuan.ambilJumlahVideoPertemuan();
						myRow.appendChild(a = new A(Common.numberFormat.get().format(vid)));
						a.setStyle("font-size:12px;");
						map.put("vid", vid);
						qtyVid += vid;
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								new PertemuanHelper().display(pertemuan, dataLoader, 5);
							}
						});

						myRow.appendChild(a = new A(Common.numberFormat.get().format(jumlahUjianTotal)));
						a.setStyle("font-size:12px;");
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								new PertemuanHelper().display(pertemuan, dataLoader, 6);
							}
						});

						myRow.appendChild(a = new A(Common.numberFormat.get().format(jumlahDiskusiTotal)));
						a.setStyle("font-size:12px;");
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								new PertemuanHelper().display(pertemuan, dataLoader, 7);
							}
						});
						// tIktUjn.addOrUpdate(day, jumlahUjianTotal);
						map.put("jumlahUjianTotal", jumlahUjianTotal);
						// tIktDisk.addOrUpdate(day, jumlahDiskusiTotal);
						map.put("jumlahDiskusiTotal", jumlahDiskusiTotal);

						int uploadTugas = pertemuan.ambilJumlahTugasFileContent();
						for (TugasPertemuan tugasPertemuan : tugasPertemuans) {
							uploadTugas += tugasPertemuan.ambilJumlahTugasFileContent();
						}
						myRow.appendChild(a = new A(Common.numberFormat.get().format(uploadTugas)));
						a.setStyle("font-size:12px;");
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								new PertemuanHelper().display(pertemuan, dataLoader, 3);
							}
						});

						uplTugas += uploadTugas;
						map.put("file_tugas", uploadTugas);

						int pert = 0;
						a = new A();
						for (Long detailperkuliahanid : collection) {
							Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
									.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
							if (detailperkuliahan != null) {
								TreeMap<String, String> d = pertemuan.ambilData("tugas",
										detailperkuliahan.getMahasiswa().getId().toString(), pertemuan.getJudultugas());
								if (a.getAttribute("d") == null) {
									a.setAttribute("d", d);
								} else {
									((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
								}
								if (!d.isEmpty()) {
									pert += d.size();
								}
								for (TugasPertemuan tugasPertemuan : tugasPertemuans) {
									d = tugasPertemuan.ambilData("tugas",
											detailperkuliahan.getMahasiswa().getId().toString(),
											tugasPertemuan.getJudultugas());
									if (a.getAttribute("d") == null) {
										a.setAttribute("d", d);
									} else {
										((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
									}
									if (!d.isEmpty()) {
										pert += d.size();
									}
								}
							}
						}
						for (Dosen dosen : perkuliahan.populateDosenBuNama()) {
							TreeMap<String, String> d = pertemuan.ambilData("tugas", dosen.getId().toString(),
									pertemuan.getJudultugas());
							if (a.getAttribute("d") == null) {
								a.setAttribute("d", d);
							} else {
								((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
							}
							if (!d.isEmpty()) {
								pert += d.size();
							}
							for (TugasPertemuan tugasPertemuan : tugasPertemuans) {
								d = tugasPertemuan.ambilData("tugas", dosen.getId().toString(),
										tugasPertemuan.getJudultugas());
								if (a.getAttribute("d") == null) {
									a.setAttribute("d", d);
								} else {
									((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
								}
								if (!d.isEmpty()) {
									pert += d.size();
								}
							}
						}
						a.setLabel(Common.numberFormat.get().format(pert));
						myRow.appendChild(a);
						a.setStyle("font-size:12px;");
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								A a = (A) arg0.getTarget();
								TreeMap<String, String> d = (TreeMap<String, String>) a.getAttribute("d");
								if (d != null && !d.isEmpty()) {
									displayTreeMap(d);
								}
							}
						});
						map.put("tugas", pert);

						tugas += pert;

						pert = 0;
						a = new A();
						for (Long detailperkuliahanid : collection) {
							Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
									.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
							if (detailperkuliahan != null) {
								TreeMap<String, String> d = pertemuan.ambilData("online",
										detailperkuliahan.getMahasiswa().getId().toString());
								if (a.getAttribute("d") == null) {
									a.setAttribute("d", d);
								} else {
									((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
								}
								if (!d.isEmpty()) {
									pert += d.size();
								}
							}
						}
						for (Dosen dosen : perkuliahan.populateDosenBuNama()) {
							TreeMap<String, String> d = pertemuan.ambilData("online", dosen.getId().toString());
							if (a.getAttribute("d") == null) {
								a.setAttribute("d", d);
							} else {
								((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
							}
							if (!d.isEmpty()) {
								pert += d.size();
							}
						}
						// tVdConf.addOrUpdate(day, pert);
						a.setLabel(Common.numberFormat.get().format(pert));
						myRow.appendChild(a);
						a.setStyle("font-size:12px;");
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								A a = (A) arg0.getTarget();
								TreeMap<String, String> d = (TreeMap<String, String>) a.getAttribute("d");
								if (d != null && !d.isEmpty()) {
									displayTreeMap(d);
								}
							}
						});
						map.put("online", pert);

						online += pert;

						pert = 0;
						a = new A();
						for (Long detailperkuliahanid : collection) {
							Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
									.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
							if (detailperkuliahan != null) {
								TreeMap<String, String> d = pertemuan.ambilData("akses",
										detailperkuliahan.getMahasiswa().getId().toString());
								if (a.getAttribute("d") == null) {
									a.setAttribute("d", d);
								} else {
									((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
								}
								if (!d.isEmpty()) {
									pert += d.size();
								}
							}
						}
						for (Dosen dosen : perkuliahan.populateDosenBuNama()) {
							TreeMap<String, String> d = pertemuan.ambilData("akses", dosen.getId().toString());
							if (a.getAttribute("d") == null) {
								a.setAttribute("d", d);
							} else {
								((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
							}
							if (!d.isEmpty()) {
								pert += d.size();
							}
						}
						// tAks.addOrUpdate(day, pert);
						a.setLabel(Common.numberFormat.get().format(pert));
						myRow.appendChild(a);
						a.setStyle("font-size:12px;");
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								A a = (A) arg0.getTarget();
								TreeMap<String, String> d = (TreeMap<String, String>) a.getAttribute("d");
								if (d != null && !d.isEmpty()) {
									displayTreeMap(d);
								}
							}
						});
						map.put("akses", pert);

						akses += pert;

						pert = 0;
						a = new A();
						for (Long detailperkuliahanid : collection) {
							Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
									.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
							if (detailperkuliahan != null) {
								TreeMap<String, String> d = pertemuan.ambilData("video",
										detailperkuliahan.getMahasiswa().getId().toString());
								if (a.getAttribute("d") == null) {
									a.setAttribute("d", d);
								} else {
									((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
								}
								if (!d.isEmpty()) {
									pert += d.size();
								}
							}
						}
						for (Dosen dosen : perkuliahan.populateDosenBuNama()) {
							TreeMap<String, String> d = pertemuan.ambilData("video", dosen.getId().toString());
							if (a.getAttribute("d") == null) {
								a.setAttribute("d", d);
							} else {
								((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
							}
							if (!d.isEmpty()) {
								pert += d.size();
							}
						}
						// tLhtVid.addOrUpdate(day, pert);
						a.setLabel(Common.numberFormat.get().format(pert));
						myRow.appendChild(a);
						a.setStyle("font-size:12px;");
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								A a = (A) arg0.getTarget();
								TreeMap<String, String> d = (TreeMap<String, String>) a.getAttribute("d");
								if (d != null && !d.isEmpty()) {
									displayTreeMap(d);
								}
							}
						});
						map.put("video", pert);

						video += pert;

						pert = 0;
						a = new A();
						for (Long detailperkuliahanid : collection) {
							Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
									.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
							if (detailperkuliahan != null) {
								TreeMap<String, String> d = pertemuan.ambilData("audio",
										detailperkuliahan.getMahasiswa().getId().toString());
								if (a.getAttribute("d") == null) {
									a.setAttribute("d", d);
								} else {
									((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
								}
								if (!d.isEmpty()) {
									pert += d.size();
								}
							}
						}
						for (Dosen dosen : perkuliahan.populateDosenBuNama()) {
							TreeMap<String, String> d = pertemuan.ambilData("audio", dosen.getId().toString());
							if (a.getAttribute("d") == null) {
								a.setAttribute("d", d);
							} else {
								((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
							}
							if (!d.isEmpty()) {
								pert += d.size();
							}
						}
						// tLhtAud.addOrUpdate(day, pert);
						a.setLabel(Common.numberFormat.get().format(pert));
						myRow.appendChild(a);
						a.setStyle("font-size:12px;");
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								A a = (A) arg0.getTarget();
								TreeMap<String, String> d = (TreeMap<String, String>) a.getAttribute("d");
								if (d != null && !d.isEmpty()) {
									displayTreeMap(d);
								}
							}
						});
						map.put("audio", pert);

						audio += pert;

						pert = 0;
						a = new A();
						for (Long detailperkuliahanid : collection) {
							Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
									.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
							if (detailperkuliahan != null) {
								TreeMap<String, String> d = pertemuan.ambilData("bahan_perkulaiahan",
										detailperkuliahan.getMahasiswa().getId().toString());
								if (a.getAttribute("d") == null) {
									a.setAttribute("d", d);
								} else {
									((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
								}
								if (!d.isEmpty()) {
									pert += d.size();
								}
							}
						}
						for (Dosen dosen : perkuliahan.populateDosenBuNama()) {
							TreeMap<String, String> d = pertemuan.ambilData("bahan_perkulaiahan",
									dosen.getId().toString());
							if (a.getAttribute("d") == null) {
								a.setAttribute("d", d);
							} else {
								((TreeMap<String, String>) a.getAttribute("d")).putAll(d);
							}
							if (!d.isEmpty()) {
								pert += d.size();
							}
						}
						// tLhtMateri.addOrUpdate(day, pert);
						a.setLabel(Common.numberFormat.get().format(pert));
						myRow.appendChild(a);
						a.setStyle("font-size:12px;");
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								A a = (A) arg0.getTarget();
								TreeMap<String, String> d = (TreeMap<String, String>) a.getAttribute("d");
								if (d != null && !d.isEmpty()) {
									displayTreeMap(d);
								}
							}
						});
						map.put("bahan_perkulaiahan", pert);
						jml = null;
						bahan_perkulaiahan += pert;

						maps.add(map);
					}
				}
				StreamingHibernateUtil.getInstance().closeSession();
					MyFormRow myRow = new MyFormRow();
					myRow.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(myRow, "18");
					myRow.appendChild(buildElearningHtmlMetricChart("Ringkasan Bahan dan Aktivitas Pertemuan", "Angka ini membantu melihat isi pertemuan dan aktivitas akses peserta secara sederhana.",
							new String[] { "Tugas", "Materi", "Ujian", "Audio", "Video", "Diskusi", "Upload Tugas", "Lihat Tugas", "Video Conference", "Akses Materi" },
							new int[] { qtyTugs, qtyMat, qtyUjian, qtyAud, qtyVid, diskusi, uplTugas, tugas, online, bahan_perkulaiahan }));

					myRow = new MyFormRow();
					myRow.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(myRow, "18");
					myRow.appendChild(buildElearningHtmlMetricChart("Ringkasan Akses Media", "Menunjukkan seberapa sering peserta membuka halaman pertemuan, video, audio, dan materi.",
							new String[] { "Akses Pertemuan", "Akses Video", "Akses Audio", "Akses Materi" },
							new int[] { akses, video, audio, bahan_perkulaiahan }));

				Foot foot = new Foot();
				foot.setParent(grid);
				Footer footer = new Footer();
				footer.setParent(foot);
				footer = new Footer("Total");
				footer.setParent(foot);
				footer = new Footer();
				footer.setParent(foot);
				footer = new Footer();
				footer.setParent(foot);

				footer = new Footer(Common.numberFormat.get().format(qtyTugs));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(qtyMat));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(qtyUjian));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(qtyAud));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(qtyVid));
				footer.setParent(foot);

				footer = new Footer(Common.numberFormat.get().format(ujian));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(diskusi));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(uplTugas));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(tugas));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(online));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(akses));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(video));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(audio));
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(bahan_perkulaiahan));
				footer.setParent(foot);

				UIUtil.checkGrigMobile(grid);

			}

		}

		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {

				Map parameters = ais.common.HashMapGenerator.getRand();
				parameters.put("maps", maps);
				@SuppressWarnings("unused")
				Tabbox tabbox = Report.generatePDFReportKembaliTab(Report.PDF, new Map[] { parameters },
						new String[] { "Rekap_Pembelajaran" }, new String[] { "Rekap Pembelajaran" },
						ais.ui.util.WaktuUtil.getDate());

			}
		});
		button.setParent(toolbarButtonMahasiswa);
	}

	public static Vbox dilihat(GeneralValueObject vo, String jenis, String nama) {
		return dilihat(vo, jenis, nama, true);
	}

	@SuppressWarnings("unchecked")
	public static List<String> belumAkses(Pertemuan pertemuan) {
		List<String> ygBelumAkses = null;
		ygBelumAkses = new ArrayList<String>();

		if (pertemuan != null && pertemuan.getJadwalUjianPMB() != null) {
			List<BiodataCalonMahasiswa> biodataCalonMahasiswas = HibernateUtil.currentSession()
					.createCriteria(RuangPaketPMB.class).setProjection(Projections.property("biodataCalonMahasiswa"))
					.createAlias("ruangPMB", "ruangPMB").createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
					.add(Restrictions.eq("ruangPMB.ujianPMB", pertemuan.getJadwalUjianPMB().getUjianPMB()))
					.add(pertemuan.getJadwalUjianPMB().getPaket() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("biodataCalonMahasiswa.paket", pertemuan.getJadwalUjianPMB().getPaket()))
					.addOrder(Common.bolehKonfigurasi("absensi_urut_berdasarkan_nim") ? Order.asc("biodataCalonMahasiswa.noRegistrasi")
									: Order.asc("biodataCalonMahasiswa.nama"))
					.list();
			for (BiodataCalonMahasiswa biodataCalonMahasiswa : biodataCalonMahasiswas) {
				String s = biodataCalonMahasiswa.getNama() + "-" + biodataCalonMahasiswa.getId() + "-CalonMahasiswa";
				ygBelumAkses.add(s);
			}
			biodataCalonMahasiswas = null;
		} else if (pertemuan != null && pertemuan.getPerkuliahan() != null) {
			for (Long detailperkuliahanid : pertemuan.getPerkuliahan().ambilDetailperkuliahan()) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					String s = detailperkuliahan.getMahasiswa().getNama() + "-"
							+ detailperkuliahan.getMahasiswa().getId() + "-Mahasiswa";
					ygBelumAkses.add(s);
				}
			}
		} else if (pertemuan != null && pertemuan.getMahasiswaRequestTugasAkhir() != null) {
			String s = pertemuan.getMahasiswaRequestTugasAkhir().getMahasiswa().getNama() + "-"
					+ pertemuan.getMahasiswaRequestTugasAkhir().getMahasiswa().getId() + "-Mahasiswa";
			ygBelumAkses.add(s);
		} else if (pertemuan != null && pertemuan.getSkripsi() != null) {
			String s = pertemuan.getSkripsi().getMahasiswa().getNama() + "-"
					+ pertemuan.getSkripsi().getMahasiswa().getId() + "-Mahasiswa";
			ygBelumAkses.add(s);
		} else if (pertemuan != null && pertemuan.getKrsMahasiswa() != null) {
			String s = pertemuan.getKrsMahasiswa().getMahasiswa().getNama() + "-"
					+ pertemuan.getKrsMahasiswa().getMahasiswa().getId() + "-Mahasiswa";
			ygBelumAkses.add(s);
		} else if (pertemuan != null && pertemuan.getKelompokKkn() != null) {
			for (MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn : pertemuan.getKelompokKkn()
					.ambilMahasiswaDapatKelompokKkn(false)) {
				String s = mahasiswaDapatKelompokKkn.getMahasiswa().getNama() + "-"
						+ mahasiswaDapatKelompokKkn.getMahasiswa().getId() + "-Mahasiswa";
				ygBelumAkses.add(s);
			}
		} else if (pertemuan != null && pertemuan.getKelompokPkl() != null) {
			for (MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl : pertemuan.getKelompokPkl()
					.ambilMahasiswaDapatKelompokPkl(false)) {
				String s = mahasiswaDapatKelompokPkl.getMahasiswa().getNama() + "-"
						+ mahasiswaDapatKelompokPkl.getMahasiswa().getId() + "-Mahasiswa";
				ygBelumAkses.add(s);
			}
		} else if (pertemuan != null && pertemuan.getFormulirKegiatan() != null) {
			List<Object[]> pesertas = HibernateUtil.currentSession().createCriteria(FormulirKegiatanPeserta.class)
					.add(Restrictions.eq("formulirKegiatan", pertemuan.getFormulirKegiatan())).setProjection(Projections
							.projectionList().add(Projections.property("dosen")).add(Projections.property("mahasiswa")))
					.list();
			for (Object[] peserta : pesertas) {
				if (peserta[0] != null) {
					Dosen dosen = (Dosen) peserta[0];
					String s = dosen.getNama() + "-" + dosen.getId() + "-Dosen";
					ygBelumAkses.add(s);
				} else if (peserta[1] != null) {
					Mahasiswa mahasiswa = (Mahasiswa) peserta[1];
					String s = mahasiswa.getNama() + "-" + mahasiswa.getId() + "-Mahasiswa";
					ygBelumAkses.add(s);
				}
			}
		}

		VOPembelajaran voPembelajaran = pertemuan.ambilVOPembelajaran();
		for (Dosen dosen : voPembelajaran.populateDosenBuNama()) {
			String s = dosen.getNama() + "-" + dosen.getId() + "-Dosen";
			ygBelumAkses.add(s);
		}
		return ygBelumAkses;
	}

	public static Vbox dilihat(final GeneralValueObject vo, final String jenis, String nama, boolean icon) {

		if (vo == null) {
			A a = icon ? new A(nama, "/img/12123-eyes-icon.png") : new A(nama);
			a.setVisible(false);
			Vbox vb = new Vbox();
			vb.appendChild(a);
			return vb;
		}
		TreeMap<String, String> d = vo.ambilData(jenis, null);
		Component a;

		Vbox vb = new Vbox();

		if (icon) {
			a = new MyToolbarbutton("fa-eye", nama);
			vb.appendChild(a);
			if (d.size() > 0) {
				MyLabelKecil labelKecil = new MyLabelKecil(Common.numberFormat.get().format(d.size()) + " lihat");
				labelKecil.setStyle("font-size:8px;color:blue;");
				vb.appendChild(labelKecil);
			}

			d.clear();
			d = null;
		} else {
			a = new MyToolbarbuttonConfig(d.size() + " " + nama);
			((Toolbarbutton) a).setStyle("font-size:8px;color:blue;");
			vb.appendChild(a);
		}

		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				TreeMap<String, String> d = vo.ambilData(jenis, null);
				if (d != null) {
					Pertemuan p = (vo instanceof TugasKelompok) ? ((TugasKelompok) vo).ambilPertemuan()
							: (vo instanceof TugasPertemuan) ? ((TugasPertemuan) vo).ambilPertemuan()
									: ((Pertemuan) vo);

					List<String> ygBelumAkses = TampilanELearningAction.belumAkses(p);
					displayTreeMap(d, ygBelumAkses, vo, jenis);
					ygBelumAkses = null;
					d.clear();
					d = null;
				}
			}
		});

		return vb;
	}

	@SuppressWarnings("unchecked")
	public static void tampilkanStatistik(final VOPembelajaran voPembelajaran, Object[] jml, int mhsSize,
			String tabName, final Component row) {
		if (voPembelajaran == null || row == null) {
			return;
		}
		try {
			int total = jml == null || jml[0] == null ? 0 : Integer.parseInt(jml[0].toString());
			int jumlah = jml == null || jml[1] == null ? 0 : Integer.parseInt(jml[1].toString());
			int absen = jml == null || jml[3] == null ? 0 : Integer.parseInt(jml[3].toString());
			Map<String, Integer> statuses = (Map<String, Integer>) (jml == null || jml[4] == null ? null : jml[4]);
			String abs = statuses == null ? "" : statuses.toString().replaceAll("\\{", "").replaceAll("\\}", "").trim();

			int absenDosen = jml == null || jml[5] == null ? 0 : Integer.parseInt(jml[5].toString());
			Map<String, Integer> statusesDosen = (Map<String, Integer>) (jml == null || jml[6] == null ? null : jml[6]);
			String absDosen = statusesDosen == null ? ""
					: statusesDosen.toString().replaceAll("\\{", "").replaceAll("\\}", "").trim();
			tabName = tabName.replaceAll("<br>", "");
			Integer persen = total == 0 ? 0 : total == jumlah ? 100 : ((jumlah * 100) / total);

			// UI Upgrade: Styling untuk Judul Card (Title)
			final A toolbarbutton = new A(tabName);
			toolbarbutton.setStyle(
					"font-size: 14px; font-weight: 700; color: #1e40af; text-decoration: none; display: block; margin-bottom: 12px; border-bottom: 1px solid #e2e8f0; padding-bottom: 8px;");
			toolbarbutton.setVisible(tabName != null && !tabName.trim().isEmpty());

			// UI Upgrade: Styling Groupbox menjadi Modern Card
			Groupbox vbox = new ais.ui.util.MyGroupboxStyled();
			vbox.setWidth("95%");
			vbox.setStyle(
					"background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 15px; margin: 10px 0; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03); transition: transform 0.2s ease-in-out;");
			row.appendChild(vbox);
			vbox.appendChild(toolbarbutton);

			if (voPembelajaran instanceof Perkuliahan) {
				Perkuliahan perkuliahan = (Perkuliahan) voPembelajaran;
				List<Perkuliahan> jadwalParalels = perkuliahan.ambilParalelPerkuliahan();
				for (Perkuliahan jadwal : jadwalParalels) {
					org.zkoss.zul.Label lblParalel = new MyLabelKecilBold("; Paralel dengan : " + jadwal.infoSimple());
					lblParalel.setStyle("color: #64748b; font-style: italic; display: block; margin-bottom: 5px;");
					lblParalel.setParent(vbox);
				}
				jadwalParalels = null;
			}

			// UI Upgrade: Merapikan layout Progressmeter Tuntas
			Hbox hbox = new Hbox();
			hbox.setWidth("100%");
			hbox.setAlign("center");
			hbox.setStyle("margin-bottom: 8px;");
			vbox.appendChild(hbox);

			org.zkoss.zul.Label lblTuntas = new MyLabelAgakKecilBold(
					"Tuntas " + jumlah + "/" + total + " (" + persen + "%)");
			lblTuntas.setStyle("color: #475569; width: 140px; display: inline-block;");
			hbox.appendChild(lblTuntas);

			Progressmeter progressmeter = new Progressmeter(persen);
			progressmeter.setHeight("8px");
			progressmeter.setWidth("180px");
			hbox.appendChild(progressmeter);

			int totalMhs = mhsSize * total;

			try {
				persen = totalMhs == 0 ? 0 : (totalMhs == absen ? 100 : ((absen * 100) / totalMhs));

				// UI Upgrade: Merapikan layout Progressmeter Kehadiran Mhs/Siswa
				hbox = new Hbox();
				hbox.setWidth("100%");
				hbox.setAlign("center");
				hbox.setStyle("margin-bottom: 4px;");
				vbox.appendChild(hbox);

				org.zkoss.zul.Label lblHadirSiswaMhs;
				if (voPembelajaran instanceof JadwalPelajaran) {
					lblHadirSiswaMhs = new MyLabelAgakKecilBold(
							"Hdr siswa " + absen + "/" + totalMhs + " (" + persen + "%)");
				} else {
					lblHadirSiswaMhs = new MyLabelAgakKecilBold(
							"Hdr mhs " + absen + "/" + totalMhs + " (" + persen + "%)");
				}
				lblHadirSiswaMhs.setStyle("color: #475569; width: 140px; display: inline-block;");
				hbox.appendChild(lblHadirSiswaMhs);

				progressmeter = new Progressmeter(persen > 100 ? 100 : persen);
				progressmeter.setHeight("8px");
				progressmeter.setWidth("180px");
				hbox.appendChild(progressmeter);

				A absenssi;
				if (voPembelajaran instanceof JadwalPelajaran) {
					(absenssi = new A("Siswa : " + abs)).setParent(vbox);
				} else {
					(absenssi = new A("Mahasiswa : " + abs)).setParent(vbox);
				}
				// UI Upgrade: Mengubah link detail absensi menjadi badge modern yang clickable
				absenssi.setStyle(
						"font-size: 10px; font-weight: 600; color: #b91c1c; background-color: #fef2f2; padding: 3px 10px; border-radius: 12px; text-decoration: none; display: inline-block; margin-bottom: 12px; border: 1px solid #fca5a5; cursor: pointer;");
				absenssi.setVisible(voPembelajaran != null);
				absenssi.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Clients.scrollIntoView(row);
						prosess(voPembelajaran, false);
					}
				});

			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/TampilanELearningAction.java:10903");
				// TODO: handle exception
			}

			if (!(voPembelajaran instanceof Wisuda)) {
				try {
					int totalDosen = total == 0 || voPembelajaran == null ? 0
							: (voPembelajaran.getJumlahDosen() * total);
					persen = totalDosen == 0 ? 0 : (totalDosen == absenDosen ? 100 : ((absenDosen * 100) / totalDosen));

					// UI Upgrade: Merapikan layout Progressmeter Kehadiran Dosen/Guru
					hbox = new Hbox();
					hbox.setWidth("100%");
					hbox.setAlign("center");
					hbox.setStyle("margin-bottom: 4px;");
					vbox.appendChild(hbox);

					org.zkoss.zul.Label lblHadirDosenGuru;
					if (voPembelajaran instanceof JadwalPelajaran) {
						lblHadirDosenGuru = new MyLabelAgakKecilBold(
								"Hdr guru " + absenDosen + "/" + totalDosen + " (" + persen + "%)");
					} else {
						lblHadirDosenGuru = new MyLabelAgakKecilBold(
								"Hdr dosen " + absenDosen + "/" + totalDosen + " (" + persen + "%)");
					}
					lblHadirDosenGuru.setStyle("color: #475569; width: 140px; display: inline-block;");
					hbox.appendChild(lblHadirDosenGuru);

					progressmeter = new Progressmeter(persen > 100 ? 100 : persen);
					progressmeter.setHeight("8px");
					progressmeter.setWidth("180px");
					hbox.appendChild(progressmeter);

					A absenssi;
					if (voPembelajaran instanceof JadwalPelajaran) {
						(absenssi = new A("Guru : " + absDosen)).setParent(vbox);
					} else {
						(absenssi = new A("Dosen : " + absDosen)).setParent(vbox);
					}
					// UI Upgrade: Badge modern untuk detail absensi pengajar
					absenssi.setStyle(
							"font-size: 10px; font-weight: 600; color: #047857; background-color: #f0fdf4; padding: 3px 10px; border-radius: 12px; text-decoration: none; display: inline-block; margin-bottom: 4px; border: 1px solid #86efac; cursor: pointer;");
					absenssi.setVisible(voPembelajaran != null);
					absenssi.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							Clients.scrollIntoView(row);
							prosess(voPembelajaran, false);
						}
					});

				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Clients.scrollIntoView(row);
					prosess(voPembelajaran, false);
				}
			});

		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void prosess(VOPembelajaran vo, boolean tampilLangsungRinci) throws Exception {
		Window window = new Window("", "none", false);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("99%");
		window.setWidth("99%");
		// UI Upgrade: Memberikan warna latar yang lebih modern (slate/abu-abu terang)
		// pada window
		window.setStyle("background-color: #f8fafc;");

		prosess(vo, tampilLangsungRinci, window, true);
	}

	public static void prosess(final VOPembelajaran vo, final boolean tampilLangsungRinci, final Component window,
			final boolean tampilTutup) throws Exception {

		if (vo != null) {

			final Pertemuan pertemuan = new Pertemuan();

			if (vo instanceof Perkuliahan) {
				Perkuliahan perkuliahan = (Perkuliahan) vo;
				pertemuan.setPerkuliahan(perkuliahan);
			} else if (vo instanceof KelompokKkn) {
				KelompokKkn kelompokKkn = (KelompokKkn) vo;
				pertemuan.setKelompokKkn(kelompokKkn);
			} else if (vo instanceof KelompokPkl) {
				KelompokPkl kelompokPkl = (KelompokPkl) vo;
				pertemuan.setKelompokPkl(kelompokPkl);
			} else if (vo instanceof Skripsi) {
				Skripsi skripsi = (Skripsi) vo;
				pertemuan.setSkripsi(skripsi);
			} else if (vo instanceof MahasiswaRequestTugasAkhir) {
				MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) vo;
				pertemuan.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
			} else if (vo instanceof KrsMahasiswa) {
				KrsMahasiswa krsMahasiswa = (KrsMahasiswa) vo;
				pertemuan.setKrsMahasiswa(krsMahasiswa);
			} else if (vo instanceof PertemuanPunyaGrupPertemuan) {
				PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) vo;
				pertemuan.setPertemuanPunyaGrupPertemuan(pertemuanPunyaGrupPertemuan);
			} else if (vo instanceof JadwalPelajaran) {
				JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) vo;
				pertemuan.setJadwalPelajaran(jadwalPelajaran);
			} else if (vo instanceof FormulirKegiatan) {
				FormulirKegiatan formulirKegiatan = (FormulirKegiatan) vo;
				pertemuan.setFormulirKegiatan(formulirKegiatan);
			} else if (vo instanceof Wisuda) {
				Wisuda wisuda = (Wisuda) vo;
				pertemuan.setWisuda(wisuda);
			}

			Borderlayout borderlayout = new Borderlayout();
			borderlayout.setStyle("background: transparent; border: none;");
			borderlayout.setParent(window);

			Center center = new Center();
			center.setStyle("background: transparent; border: none; padding: 15px;");
			center.setAutoscroll(true);
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			MyGrid grid = new MyGrid();
			grid.setStyle(
					"border: 1px solid #e2e8f0; border-radius: 8px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); background: #ffffff;");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("0%");

			column = new MyColumnConfig();
			column.setParent(columns);

			Rows rows = new Rows();

			rows.setParent(grid);

			final MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setStyle("border: none; background: transparent;");
			row.setParent(rows);

			final MyDetail detail = new MyDetail();
			detail.setParent(row);

			final org.zkoss.zul.Div groupbox = CalendarPerkuliahanMingguIniComposer.displayRinci(row, pertemuan,
					new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (window instanceof Window) {
								window.detach();
							}
							TampilanELearningAction.prosess(vo, tampilLangsungRinci);
						}
					});
			detail.appendChild(groupbox);
			detail.setOpen(true);

			if (tampilTutup) {
				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setStyle("background: #ffffff; border-top: 1px solid #e2e8f0; padding: 12px;");
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				toolbar.setStyle("background: transparent; border: none; float: right; padding-right: 15px;");

				// UI Upgrade: Mengubah tombol Tutup menjadi solid button yang modern
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
				button.setStyle(
						"font-size: 13px; font-weight: bold; color: #ffffff; background-color: #ef4444; border-radius: 6px; padding: 6px 20px; text-decoration: none; cursor: pointer; box-shadow: 0 2px 4px rgba(239, 68, 68, 0.3); transition: background-color 0.2s;");
				button.setTooltiptext("Tutup");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						SkripsiAction skripsiAction = (SkripsiAction) groupbox.getAttribute("skripsiAction");
						if (skripsiAction != null) {
							skripsiAction.onSave(event);
						}
						window.detach();
					}
				});
				button.setParent(toolbar);
				toolbar.setParent(south);

				if (window instanceof Window) {
					((Window) window).onModal();
				}
			}
		}

	}

	/**
	 * Baca status checkbox secara aman; beberapa pemanggil mengoper null
	 * untuk checkbox status sehingga thread latar bisa NPE.
	 */
	private static boolean dicek(org.zkoss.zul.Checkbox checkbox) {
		return checkbox != null && checkbox.isChecked();
	}

	private static void closeHibernateSessionQuietly(Session session) {
		ais.common.ElearningSessionUtil.closeQuietly(session);
	}

    private static Component buildElearningHtmlMetricChart(String title, String description, String[] labels, int[] values) {
        int max = 0;
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] > max) {
                    max = values[i];
                }
            }
        }
        if (max <= 0) {
            max = 1;
        }
        StringBuffer sb = new StringBuffer();
        sb.append("<div class='el-html-chart'>");
        sb.append("<div class='el-html-chart-title'>").append(escapeHtmlSimple(title)).append("</div>");
        sb.append("<div class='el-html-chart-desc'>").append(escapeHtmlSimple(description)).append("</div>");
        if (labels != null && values != null) {
            for (int i = 0; i < labels.length && i < values.length; i++) {
                int value = values[i];
                int width = (int) Math.round((value * 100.0) / max);
                if (value > 0 && width < 3) {
                    width = 3;
                }
                sb.append("<div class='el-html-chart-row'>");
                sb.append("<div class='el-html-chart-label'>").append(escapeHtmlSimple(labels[i])).append("</div>");
                sb.append("<div class='el-html-chart-track'><div class='el-html-chart-bar' style='width:").append(width).append("%'></div></div>");
                sb.append("<div class='el-html-chart-value'>").append(Common.numberFormat.get().format(value)).append("</div>");
                sb.append("</div>");
            }
        }
        sb.append("</div>");
        return new Html(sb.toString());
    }

    private static Component buildElearningHtmlPie(String title, String description, int value, int total) {
        int safeTotal = total <= 0 ? 1 : total;
        int percent = (int) Math.round((value * 100.0) / safeTotal);
        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;
        StringBuffer sb = new StringBuffer();
        sb.append("<div class='el-html-chart'>");
        sb.append("<div class='el-html-chart-title'>").append(escapeHtmlSimple(title)).append("</div>");
        sb.append("<div class='el-html-chart-desc'>").append(escapeHtmlSimple(description)).append("</div>");
        sb.append("<div class='el-pie-css' style='--el-percent:").append(percent).append("%'>");
        sb.append("<div class='el-pie-css-inner'><div class='el-pie-css-value'>").append(percent).append("%</div><div class='el-pie-css-label'>Terpenuhi</div></div>");
        sb.append("</div>");
        sb.append("<div class='el-chart-legend'><span><i class='el-chart-dot'></i>").append(Common.numberFormat.get().format(value)).append(" aktif</span><span><i class='el-chart-dot el-chart-dot-muted'></i>").append(Common.numberFormat.get().format(Math.max(0, safeTotal - value))).append(" belum</span></div>");
        sb.append("</div>");
        return new Html(sb.toString());
    }

    private static String escapeHtmlSimple(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

}
