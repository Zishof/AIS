package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
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
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.GrupPertemuanAction;
import ais.action.master.MahasiswaRequestTugasAkhirAction;
import ais.action.master.MonitorKRSMahasiswaAction;
import ais.action.master.SkripsiAction;
import ais.action.master.TampilanELearningAction;
import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.AbsensiHelper;
import ais.action.master.helper.AktifitasPerkuliahanHelper;
import ais.action.master.helper.CalendarPerkuliahanMingguIniComposer;
import ais.action.master.helper.PerkuliahanUIHelper;
import ais.action.master.helper.PertemuanPunyaDiskusiHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.LiveStreamingPlayerWindow;
import ais.action.master.helper.profile.ProfileUtil;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.kkn.KelompokKknAction;
import ais.action.master.pkl.KelompokPklAction;
import ais.action.master.sekolah.helper.AbsensiSiswaHelper;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.common.AIGenerator;
import ais.common.Common;
import ais.common.CommonEmail;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.MateriDanKomentarHelper;
import ais.common.calendar.CalendarUtil;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CommonVO;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.FormulirKegiatan;
import ais.database.model.FormulirKegiatanPeserta;
import ais.database.model.GeneralValueObject;
import ais.database.model.JadwalUjianPMB;
import ais.database.model.Jurusan;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Matakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.Pegawai;
import ais.database.model.PendaftaranWisuda;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaDiskusi;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.RuangPMB;
import ais.database.model.StatusPertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.TugasPertemuan;
import ais.database.model.Wisuda;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.obe.CapaianLulusan;
import ais.database.model.obe.CapaianPembelajaranLulusan;
import ais.database.model.obe.ProfilLulusan;
import ais.database.model.obe.ReferensiLulusan;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHboxToolbar;
import ais.ui.util.MyHtmlIframe;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelBoldMerahConfig;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyLabelKecilBold;
import ais.ui.util.MyMenuitem;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTimebox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyToolbarbuttonKecilConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.SmartDateTimeUtil;
import ais.ui.util.WaktuUtil;

public class DashboardTimelinePertemuan extends MyWindow {

	private static final long serialVersionUID = 790038368339375113L;

	public static boolean tampilkan_singkron_kalendar_di_elearning = false;
	public static boolean tampilkan_kalendar_di_elearning = false;
	public static boolean tampilkan_dasbor_di_elearning = true;
	public static boolean tampilkan_obe_di_elearning = false;

	private boolean reloadBlnSd = false;

	private Center center = new Center();

	private Paging paging;
	private Grid grid;
	private Boolean ujian = false;

	private MyCheckboxConfig jadwalPerkuliahan;
	private MyCheckboxConfig jadwalKkn;
	private MyCheckboxConfig jadwalPkl;
	private MyCheckboxConfig jadwalBimbingan;
	private MyCheckboxConfig jadwalRevisi;
	private MyCheckboxConfig jadwalKonsultasi;
	private MyCheckboxConfig jadwalKonsultasiLain;
	private MyCheckboxConfig jadwalKegiatan;
	private MyCheckboxConfig tdpUjian;
	private MyCheckboxConfig tdpVideo;
	private MyCheckboxConfig tdpAudio;
	private MyCheckboxConfig tdpTugas;
	private MyCheckboxConfig tdpDiskusi;
	private MyCheckboxConfig tdpCatatan;
	private MyCheckboxConfig tdpDosenPengganti;
	private Textbox cariNamaUjian;

	final MyCheckboxConfig remedial = new MyCheckboxConfig("Remedial");
	final MyCheckboxConfig paralel = new MyCheckboxConfig("Paralel");
	final MyCheckboxConfig pra = new MyCheckboxConfig("Pra.Perkuliahan");
	final MyCheckboxConfig ekstra = new MyCheckboxConfig("Ekstrakurikuler");

	public Combobox bulanSd;

	private Tbmuser tbmuser;

	private MyCheckboxConfig jadwalPelajaran;

	private EventListener eventRefresh = null;

	public DashboardTimelinePertemuan(EventListener eventRefresh) {
		super();
		try {
			this.eventRefresh = eventRefresh;
			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardTimelinePertemuan(Boolean ujian) {
		super();
		this.ujian = ujian;
		try {

			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					init();
				}
			});

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardTimelinePertemuan(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					init();
				}
			});
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private int displayPerPage = 25;

	@SuppressWarnings("rawtypes")
	private static final java.util.concurrent.ConcurrentHashMap<String, java.util.TreeMap> TIMELINE_CACHE
			= new java.util.concurrent.ConcurrentHashMap<String, java.util.TreeMap>();
	private static final java.util.concurrent.ConcurrentHashMap<String, Long> TIMELINE_EXPIRY
			= new java.util.concurrent.ConcurrentHashMap<String, Long>();
	private static final long TIMELINE_TTL_MS = 5L * 60 * 1000;

	private Textbox cariTopik;
	private Textbox cariMk;
	private Textbox cariDosen;
	private Textbox cariMahasiswa;

	private MyTimebox cariWaktuMulai;
	private MyTimebox cariWaktuSampai;

	private Textbox cariCatatan;

	private MyCheckboxConfig semuaWaktu;

	private Combobox hari;

	private MyCheckboxConfig tdpOnline;

	private Combobox cariStatusPertemuan;

	private Combobox cariPertemunaKe;

	private Rows rowsUtama;

	private MyCheckboxConfig tdpMateri;

	private Textbox cariKelas;

	private Textbox cariRuang;

	private Combobox bulanSdCari;

	private boolean pt = false;

	private boolean ya = false;

	private void init() throws Exception {
		// Tombol Cetak PDF & Ekspor Excel TIDAK lagi dipasang sebagai baris toolbar TERPISAH di atas
		// (dulu: DashboardGridExportHelper.pasang(this,...)). Permintaan user: digabung SEJAJAR ke dalam
		// toolbar utama (Agenda/Refresh/Pencarian/periode) sebagai SATU button group -> lihat pasangTombol
		// setelah toolbar utama dibuat di bawah.
		tbmuser = Common.getCurrentUser();
		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		tampilkan_singkron_kalendar_di_elearning = Common.bolehKonfigurasi("tampilkan_singkron_kalendar_di_elearning", Konfigurasi.TIDAK_AKTIF);
		tampilkan_kalendar_di_elearning = Common.bolehKonfigurasi("tampilkan_kalendar_di_elearning", Konfigurasi.TIDAK_AKTIF);
		tampilkan_dasbor_di_elearning = Common.bolehKonfigurasi("tampilkan_dasbor_di_elearning");
		tampilkan_obe_di_elearning = Common.bolehKonfigurasi("tampilkan_obe_di_elearning", Konfigurasi.TIDAK_AKTIF);

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(north);
		// SATU BUTTON GROUP (permintaan user): jadikan toolbar ini flex-wrap rapi, lalu masukkan tombol
		// Cetak PDF & Ekspor Excel di AWAL grup — sehingga sejajar dengan Agenda/Refresh/Pencarian +
		// dropdown periode dalam satu baris (bukan lagi baris terpisah di atas).
		toolbar.setStyle("display:flex;flex-wrap:wrap;align-items:center;gap:4px;");
		DashboardGridExportHelper.pasangTombol(toolbar, this, "Timeline Pertemuan");

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Agenda", "/img/svg/calendar-check.svg");
		toolbarbutton.setParent(toolbar);
		toolbarbutton.setVisible(pt && !Common.isMobile());

		toolbarbutton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				final MyWindow window = new MyWindow("", "none", false);
				window.setHeight("97%");
				window.setWidth("97%");
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				Tabbox tabbox = new Tabbox();
				tabbox.setParent(center);
				tabbox.setHeight("100%");
				tabbox.setWidth("100%");

				Tabs tabs = new Tabs();
				tabs.setWidth("30px");
				tabs.setParent(tabbox);

				final MyTabConfig tabSoal = new MyTabConfig("Perkuliahan");
				tabSoal.setParent(tabs);

				MyTabConfig tabKkn = new MyTabConfig("KKN");
				tabKkn.setParent(tabs);

				MyTabConfig tabPkl = new MyTabConfig("PKL");
				tabPkl.setParent(tabs);

				MyTabConfig tabBimbingan = new MyTabConfig("Bimbingan");
				tabBimbingan.setParent(tabs);

				MyTabConfig tabRevisi = new MyTabConfig("Sidang dan Revisi");
				tabRevisi.setParent(tabs);

				MyTabConfig tabKonsultasi = new MyTabConfig("Konsultasi PA");
				tabKonsultasi.setParent(tabs);

				Tabpanels tabpanels = new Tabpanels();
				tabpanels.setParent(tabbox);

				Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
				tabpanelUtama.setParent(tabpanels);

				MyInclude iframe = new MyInclude("/pages/master/pertemuan.zul");
				iframe.setParent(tabpanelUtama);

				final Tabpanel tabpanelKkn = new ais.ui.util.MyTabpanel();
				tabpanelKkn.setParent(tabpanels);

				tabKkn.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (tabpanelKkn.getChildren().isEmpty()) {
							MyInclude iframe = new MyInclude("/pages/master/kkn/kelompok_kkn.zul");
							iframe.setParent(tabpanelKkn);
						}
					}
				});

				final Tabpanel tabpanelPkl = new ais.ui.util.MyTabpanel();
				tabpanelPkl.setParent(tabpanels);

				tabPkl.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (tabpanelPkl.getChildren().isEmpty()) {
							MyInclude iframe = new MyInclude("/pages/master/pkl/kelompok_pkl.zul");
							iframe.setParent(tabpanelPkl);
						}
					}
				});

				final Tabpanel tabpanelBimbingan = new ais.ui.util.MyTabpanel();
				tabpanelBimbingan.setParent(tabpanels);

				tabBimbingan.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (tabpanelBimbingan.getChildren().isEmpty()) {
							MyInclude iframe = new MyInclude("/pages/master/mahasiswa_request_tugas_akhir.zul");
							iframe.setParent(tabpanelBimbingan);
						}
					}
				});

				final Tabpanel tabpanelRevisi = new ais.ui.util.MyTabpanel();
				tabpanelRevisi.setParent(tabpanels);

				tabRevisi.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (tabpanelRevisi.getChildren().isEmpty()) {
							MyInclude iframe = new MyInclude("/pages/master/skripsi.zul");
							iframe.setParent(tabpanelRevisi);
						}
					}
				});

				final Tabpanel tabpanelKonsultasi = new ais.ui.util.MyTabpanel();
				tabpanelKonsultasi.setParent(tabpanels);

				tabKonsultasi.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (tabpanelKonsultasi.getChildren().isEmpty()) {
							MyInclude iframe = new MyInclude("/pages/master/krs_mahasiswa.zul");
							iframe.setParent(tabpanelKonsultasi);
						}
					}
				});

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				toolbar.setParent(south);

				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/svg/close-circle-line.svg");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								initSpreadsheet(false, false);
							}
						});
					}
				});
				cancel.setParent(toolbar);

				window.onModal();
			}
		});

		toolbarbutton = new MyToolbarbuttonConfig("Kalender", "/img/svg/calendar2.svg");
		toolbarbutton.setParent(toolbar);
		toolbarbutton.setVisible(tampilkan_singkron_kalendar_di_elearning && tbmuser != null
				&& tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Pilih Tanggal", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("300px");
				window.setWidth("600px");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("20%");
				column.setParent(columns);
				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();

				rows.setParent(grid);

				final MyDatebox mulai = new MyDatebox(WaktuUtil.getDate());
				final MyDatebox sampai = new MyDatebox(WaktuUtil.getDate());
				mulai.setReadonly(true);
				sampai.setReadonly(true);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
				row.appendChild(mulai);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sampai"));
				row.appendChild(sampai);

				Common.initKeterangan(rows,
						"Rentan jumlah hari untuk sekali singkron dengan Google Calendar tidak boleh lebih dari 7 hari");
				Common.initKeterangan(rows,
						"Jumlah pertemuan dalam sekali singkon dengan Google Calendar tidak boleh lebih dari 50 pertemuan");

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				MyHboxToolbar toolbar = new MyHboxToolbar();
				toolbar.setParent(south);
				MyToolbarbutton cancel = new MyToolbarbutton("fa-ban", "Batal");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbutton save = new MyToolbarbutton("fa-calendar", "Singkronkan dengan Google Calendar");
				save.setTooltiptext("Singkronkan dengan Google Calendar");
				save.setParent(toolbar);
				save.addEventListener("onClick", new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {

						Date m = mulai.getValue();
						Date s = sampai.getValue();

						int d = Common.getBetweenTwoDates(m, s);
						if (d > 7) {
							MyMessageboxConfig.show(
									"Rentan jumlah hari untuk sekali singkron dengan Google Calendar tidak boleh lebih dari 7 hari.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						CalendarUtil calendarUtil = new CalendarUtil(tbmuser);
						PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

						if (tbmuser != null && tbmuser.ambilDosen() != null
								&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
								&& tbmuser.ambilDosen().getPerguruanTinggi() != null) {
							selectedPerguruanTinggi = tbmuser.ambilDosen().getPerguruanTinggi();
						} else if (tbmuser != null && tbmuser.getMahasiswa() != null
								&& tbmuser.getMahasiswa().getJurusan() != null
								&& tbmuser.getMahasiswa().getJurusan().getFakultas() != null
								&& tbmuser.getMahasiswa().getJurusan().getFakultas().getPerguruanTinggi() != null) {
							selectedPerguruanTinggi = tbmuser.getMahasiswa().getJurusan().getFakultas()
									.getPerguruanTinggi();
						} else if (tbmuser != null && tbmuser.ambilFakultas() != null
								&& tbmuser.ambilFakultas().getPerguruanTinggi() != null) {
							selectedPerguruanTinggi = tbmuser.ambilFakultas().getPerguruanTinggi();
						}

						final List<com.google.api.services.calendar.model.Event> events = new ArrayList<com.google.api.services.calendar.model.Event>();

						calendarUtil.proses(pertemuansa, selectedPerguruanTinggi, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								List<com.google.api.services.calendar.model.Event> eventsa = (List<com.google.api.services.calendar.model.Event>) arg0
										.getData();
								events.addAll(eventsa);
							}
						});

						CalendarUtil.cretaeTimerWaiting(events, m, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								initSpreadsheet(false, false);
							}
						});
					}
				});
				window.onModal();
			}
		});

		toolbarbutton = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh-cw.svg");
		toolbarbutton.setParent(toolbar);
		toolbarbutton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet(true, true);
			}
		});

		// ── Tombol OBE ─────────────────────────────────────────────────────────────
		toolbarbutton = new MyToolbarbuttonConfig("OBE", "/img/svg/award.svg");
		toolbarbutton.setParent(toolbar);
		toolbarbutton.setVisible(tampilkan_obe_di_elearning && pt);
		toolbarbutton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				final MyWindow window = new MyWindow("Dasbor OBE per Semester", "none", false);
				window.setHeight("97%");
				window.setWidth("97%");
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

				Borderlayout bl = new ais.ui.util.MyBorderlayout();
				bl.setParent(window);

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(bl);
				Toolbar tbClose = new Toolbar();
				tbClose.setParent(south);
				MyToolbarbuttonConfig btnClose = new MyToolbarbuttonConfig("Tutup", "/img/svg/close-circle-line.svg");
				btnClose.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						window.detach();
					}
				});
				btnClose.setParent(tbClose);

				Center centerObe = new Center();
				ais.ui.util.ZkCompat.setFlex(centerObe, true);
				centerObe.setParent(bl);

				centerObe.appendChild(buildObeComponent(tbmuser));
				window.onModal();
			}
		});

		if (!ujian) {

			final MyWindow myWindowPencarian = new MyWindow("Pencarian", "none", false);
			myWindowPencarian.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			myWindowPencarian.setHeight("99%");
			myWindowPencarian.setWidth(Common.isMobile() ? "99%" : "470px");
			myWindowPencarian.setVisible(false);

			Borderlayout borderlayoutencarian = new MyBorderlayout(true);
			borderlayoutencarian.setParent(myWindowPencarian);

			South southPencarian = new South();
			southPencarian.setSize("64px");
			southPencarian.setBorder("none");
			southPencarian.setParent(borderlayoutencarian);

			Toolbar toolbarPencarian = new Toolbar();
			toolbarPencarian.setStyle("display:flex; align-items:center; gap:8px; flex-wrap:wrap;"
					+ " width:100%; box-sizing:border-box; padding:10px 12px;"
					+ " background:#f8fafc; border-top:1px solid #e5e7eb;");
			toolbarPencarian.setParent(southPencarian);
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/svg/cancel_presentation.svg");
			cancel.setTooltiptext("Tutup");
			cancel.setStyle(
					"font-weight: bold; background: #ef4444; color: white; border-radius: 6px; padding: 6px 12px; border: none; cursor: pointer;");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					myWindowPencarian.setVisible(false);
				}
			});

			bulanSdCari = initBulanSd();

			MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
			save.setTooltiptext("Cari");
			save.setStyle(
					"font-weight: bold; background: #3b82f6; color: white; border-radius: 6px; padding: 6px 14px; border: none; cursor: pointer;");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.selectComboItem(bulanSd, bulanSdCari.getSelectedItem().getValue());
					myWindowPencarian.setVisible(false);
					initSpreadsheet(false, true);
				}
			});

			// Tata rapi bar bawah: [Periode ▼]  ——(dorong ke kanan)——  [Batal] [Cari]
			MyLabelBoldAja lblPeriodeCari = new MyLabelBoldAja("Periode:");
			lblPeriodeCari.setStyle("font-size:12px;");
			toolbarPencarian.appendChild(lblPeriodeCari);
			toolbarPencarian.appendChild(bulanSdCari);
			org.zkoss.zul.Div spacerBawahCari = new org.zkoss.zul.Div();
			spacerBawahCari.setStyle("flex:1 1 auto; min-width:4px;");
			toolbarPencarian.appendChild(spacerBawahCari);
			toolbarPencarian.appendChild(cancel);
			toolbarPencarian.appendChild(save);

			toolbarbutton = new MyToolbarbuttonConfig("Pencarian", "/img/svg/search.svg");
			toolbarbutton.setParent(toolbar);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					// Cegah klik ganda: bila popup sudah terbuka, jangan panggil onModal lagi
					// (pemanggilan modal dua kali membuat tata letak jadi separuh).
					if (myWindowPencarian.isVisible()) {
						return;
					}
					myWindowPencarian.setWidth(Common.isMobile() ? "99%" : "470px");
					myWindowPencarian.setHeight("99%");
					myWindowPencarian.setVisible(true);
					// Render ulang bersih agar Borderlayout tidak kolaps saat dibuka kembali.
					myWindowPencarian.invalidate();
					myWindowPencarian.onModal();
				}
			});

			Center centerPencarian = new Center();
			centerPencarian.setParent(borderlayoutencarian);
			ais.ui.util.ZkCompat.setFlex(centerPencarian, true);
			Grid gridcari = new Grid();
			gridcari.setWidth("100%");
			gridcari.setParent(centerPencarian);
			gridcari.setHeight("100%");

			Columns columnscari = new Columns();
			columnscari.setParent(gridcari);

			MyColumnConfig columncari = new MyColumnConfig();
			columncari.setParent(columnscari);

			Rows rowscari = new Rows();

			rowscari.setParent(gridcari);

			MyFormRow rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(new MyLabelBoldAja("Topik / Judul Tugas / Jenis"));

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);

			Hbox hboxaa = new Hbox();
			rowcari.appendChild(hboxaa);

			hboxaa.appendChild(cariTopik = new Textbox());
			cariTopik.setCols(25);
			cariTopik.addEventListener("onOK", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					myWindowPencarian.setVisible(false);
					initSpreadsheet(false, true);
				}
			});

			cariStatusPertemuan = new Combobox();
			hboxaa.appendChild(cariStatusPertemuan);
			cariStatusPertemuan.setCols(6);
			Common.insertComboDanSemua(cariStatusPertemuan, "nama", StatusPertemuan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			rowcari.appendChild(new MyLabelBoldAja("Catatan / Pertemuan ke"));

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			hboxaa = new Hbox();
			rowcari.appendChild(hboxaa);
			hboxaa.appendChild(cariCatatan = new Textbox());
			cariCatatan.setCols(25);
			cariCatatan.addEventListener("onOK", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					myWindowPencarian.setVisible(false);
					initSpreadsheet(false, true);
				}
			});

			cariPertemunaKe = new Combobox();
			Comboitem comboitemPertemuan = new Comboitem("Semua Pertemuan");
			cariPertemunaKe.appendChild(comboitemPertemuan);
			cariPertemunaKe.setSelectedItem(comboitemPertemuan);
			for (int i = 1; i <= 20; i++) {
				comboitemPertemuan = new Comboitem("Pertemuan ke-" + i);
				comboitemPertemuan.setValue(i);
				cariPertemunaKe.appendChild(comboitemPertemuan);
			}

			hboxaa.appendChild(cariPertemunaKe);
			cariPertemunaKe.setCols(6);
			cariPertemunaKe.setReadonly(true);

			if (tbmuser.getSiswa() == null && tbmuser.ambilGuru() == null) {

				if (pt) {
					rowcari = new MyFormRow();
					rowcari.setParent(rowscari);
					rowcari.appendChild(new MyLabelBoldAja("Kode / Nama Matakuliah"));

					rowcari = new MyFormRow();
					rowcari.setParent(rowscari);
					rowcari.appendChild(cariMk = new Textbox());
					cariMk.setWidth("90%");
					cariMk.addEventListener("onOK", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							myWindowPencarian.setVisible(false);
							initSpreadsheet(false, true);
						}
					});

					rowcari = new MyFormRow();
					rowcari.setParent(rowscari);
					rowcari.appendChild(new MyLabelBoldAja("Nama Dosen"));

					rowcari = new MyFormRow();
					rowcari.setParent(rowscari);
					rowcari.appendChild(cariDosen = new Textbox());
					cariDosen.setWidth("90%");
					cariDosen.addEventListener("onOK", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							myWindowPencarian.setVisible(false);
							initSpreadsheet(false, true);
						}
					});
				}

				rowcari = new MyFormRow();
				rowcari.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
				rowcari.setParent(rowscari);
				rowcari.appendChild(new MyLabelBoldAja("NIM/Nama Mahasiswa (hanya untuk Bimbingan, Sidang, dan KRS)"));

				rowcari = new MyFormRow();
				rowcari.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
				rowcari.setParent(rowscari);
				rowcari.appendChild(cariMahasiswa = new Textbox());
				cariMahasiswa.setWidth("90%");
				cariMahasiswa.addEventListener("onOK", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						myWindowPencarian.setVisible(false);
						initSpreadsheet(false, true);
					}
				});

				rowcari = new MyFormRow();
				rowcari.setParent(rowscari);
				semuaWaktu = new MyCheckboxConfig("Semua Waktu");
				semuaWaktu.setChecked(true);
				rowcari.appendChild(
						new Hbox(new Component[] { new MyLabelBoldAja("Waktu Mulai atau Waktu Sampai"), semuaWaktu }));

				rowcari = new MyFormRow();
				rowcari.setParent(rowscari);
				rowcari.appendChild(new Hbox(new Component[] {
						cariWaktuMulai = new MyTimebox(new GregorianCalendar(0, 0, 0, 0, 0, 0).getTime()),
						new MyLabelConfig(" s.d "),
						cariWaktuSampai = new MyTimebox(new GregorianCalendar(0, 0, 0, 23, 59, 59).getTime()) }));

				cariWaktuMulai.setDisabled(semuaWaktu.isChecked());
				cariWaktuSampai.setDisabled(semuaWaktu.isChecked());

				semuaWaktu.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						cariWaktuMulai.setDisabled(semuaWaktu.isChecked());
						cariWaktuSampai.setDisabled(semuaWaktu.isChecked());
					}
				});

				rowcari = new MyFormRow();
				rowcari.setParent(rowscari);
				rowcari.appendChild(new MyLabelBoldAja("Hari"));

				rowcari = new MyFormRow();
				rowcari.setParent(rowscari);

				hari = new Combobox();
				rowcari.appendChild(hari);
				MyComboitemConfig comboitem;
				for (String h : Common.haris) {
					comboitem = new MyComboitemConfig();
					comboitem.setLabel(h);
					comboitem.setValue(h);
					hari.appendChild(comboitem);
				}
				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Semua Hari");
				comboitem.setValue(null);
				hari.appendChild(comboitem);
				hari.setReadonly(true);
				hari.setSelectedItem(comboitem);
				hari.setCols(5);

				// Rapi: font terbaca (bukan 7px) + tak terpotong.
				String gayaCekAgenda = "font-size:12px; white-space:nowrap;";
				remedial.setStyle(gayaCekAgenda);
				paralel.setStyle(gayaCekAgenda);
				pra.setStyle(gayaCekAgenda);
				ekstra.setStyle(gayaCekAgenda);

				rowcari = new MyFormRow();
				rowcari.setParent(rowscari);

				Vbox subPekVbox = new Vbox();
				subPekVbox.setParent(rowcari);

				org.zkoss.zul.Div subPerk = new org.zkoss.zul.Div();
				subPerk.setStyle("display:flex; flex-wrap:wrap; gap:6px 14px; align-items:center; width:100%; padding:2px 0;");
				subPerk.setParent(subPekVbox);

				jadwalPerkuliahan = new MyCheckboxConfig("Agenda Perkuliahan");
				jadwalPerkuliahan.setStyle("color:" + Pertemuan.warnas.get(0).split(",")[0] + "; font-size:12px; white-space:nowrap;");
				jadwalPerkuliahan.setChecked(true);
				jadwalPerkuliahan.setParent(subPerk);

				remedial.setParent(subPerk);
				paralel.setParent(subPerk);
				pra.setParent(subPerk);
				ekstra.setParent(subPerk);

				subPerk = new org.zkoss.zul.Div();
				subPerk.setStyle("display:flex; flex-wrap:wrap; gap:6px 12px; align-items:center; width:100%; margin-top:6px;");
				subPerk.setParent(subPekVbox);
				subPerk.appendChild(new MyLabelAgakKecil("Kelas : "));
				cariKelas = new Textbox();
				cariKelas.setParent(subPerk);
				cariKelas.setCols(4);
				cariKelas.addEventListener("onOK", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						myWindowPencarian.setVisible(false);
						initSpreadsheet(false, true);
					}
				});
				subPerk.appendChild(new MyLabelAgakKecil("Ruang : "));
				cariRuang = new Textbox();
				cariRuang.setParent(subPerk);
				cariRuang.setCols(4);
				cariRuang.addEventListener("onOK", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						myWindowPencarian.setVisible(false);
						initSpreadsheet(false, true);
					}
				});

				rowcari = new MyFormRow();
				rowcari.setParent(rowscari);

				jadwalKkn = new MyCheckboxConfig("Agenda KKN");
				jadwalKkn.setStyle("color:" + Pertemuan.warnas.get(1).split(",")[0]);
				jadwalKkn.setChecked(true);
				jadwalKkn.setParent(rowcari);

				rowcari = new MyFormRow();
				rowcari.setParent(rowscari);

				jadwalPkl = new MyCheckboxConfig("Agenda PKL");
				jadwalPkl.setStyle("color:" + Pertemuan.warnas.get(2).split(",")[0]);
				jadwalPkl.setChecked(true);
				jadwalPkl.setParent(rowcari);

				rowcari = new MyFormRow();
				rowcari.setParent(rowscari);

				jadwalBimbingan = new MyCheckboxConfig(
						"Agenda Bimbingan / Seminar / Kompre Tugas Akhir / Skripsi / Thesis");
				jadwalBimbingan.setStyle("color:" + Pertemuan.warnas.get(3).split(",")[0]);
				jadwalBimbingan.setChecked(true);
				jadwalBimbingan.setParent(rowcari);

				rowcari = new MyFormRow();
				rowcari.setParent(rowscari);

				jadwalRevisi = new MyCheckboxConfig("Agenda Sidang dan Revisi Tugas Akhir / Skripsi / Thesis");
				jadwalRevisi.setStyle("color:" + Pertemuan.warnas.get(4).split(",")[0]);
				jadwalRevisi.setChecked(true);
				jadwalRevisi.setParent(rowcari);

				rowcari = new MyFormRow();
				rowcari.setParent(rowscari);

				jadwalKonsultasi = new MyCheckboxConfig("Agenda Konsultasi Dosen Pembimbing Akademik / Dosen Wali");
				jadwalKonsultasi.setStyle("color:" + Pertemuan.warnas.get(5).split(",")[0]);
				jadwalKonsultasi.setChecked(true);
				jadwalKonsultasi.setParent(rowcari);

				rowcari = new MyFormRow();
				rowcari.setParent(rowscari);

				jadwalKonsultasiLain = new MyCheckboxConfig("Agenda Konsultasi Layanan Lain");
				jadwalKonsultasiLain.setStyle("color:" + Pertemuan.warnas.get(6).split(",")[0]);
				jadwalKonsultasiLain.setChecked(true);
				jadwalKonsultasiLain.setParent(rowcari);

				rowcari = new MyFormRow();
				rowcari.setParent(rowscari);

				jadwalKegiatan = new MyCheckboxConfig("Kegiatan lain");
				jadwalKegiatan.setStyle("color:" + Pertemuan.warnas.get(8).split(",")[0]);
				jadwalKegiatan.setChecked(true);
				jadwalKegiatan.setParent(rowcari);

				rowcari = new MyFormRow();
				rowcari.setParent(rowscari);

				jadwalPelajaran = new MyCheckboxConfig("Jadwal Pelajaran");
				jadwalPelajaran.setStyle("color:" + Pertemuan.warnas.get(7).split(",")[0]);
				jadwalPelajaran.setChecked(true);
				jadwalPelajaran.setParent(rowcari);

				jadwalPelajaran.setVisible(ya);

				if (!pt) {
					jadwalPerkuliahan.setVisible(false);
					jadwalKkn.setVisible(false);
					jadwalPkl.setVisible(false);
					jadwalBimbingan.setVisible(false);
					jadwalRevisi.setVisible(false);
					jadwalKonsultasi.setVisible(false);
					jadwalKonsultasiLain.setVisible(false);
					jadwalKegiatan.setVisible(false);
				}
			}

			if (tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null)) {
				rowcari = new MyFormRow();
				rowcari.setParent(rowscari);
				tdpOnline = new MyCheckboxConfig("Tampilkan hanya yang telah atau sedang online");
				tdpOnline.setParent(rowcari);
			}

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);

			tdpUjian = new MyCheckboxConfig("Tampilkan hanya yang ada ujian");

			Hbox rowUjian = new Hbox();
			rowUjian.setParent(rowcari);
			tdpUjian.setParent(rowUjian);
			rowUjian.appendChild(new MyLabelAgakKecil(" Nama Ujian : "));
			cariNamaUjian = new Textbox();
			cariNamaUjian.setParent(rowUjian);
			cariNamaUjian.setCols(4);
			cariNamaUjian.addEventListener("onOK", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					myWindowPencarian.setVisible(false);
					initSpreadsheet(false, true);
				}
			});

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			tdpTugas = new MyCheckboxConfig("Tampilkan hanya yang ada tugas");
			tdpTugas.setParent(rowcari);

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			tdpMateri = new MyCheckboxConfig("Tampilkan hanya yang ada materi");
			tdpMateri.setParent(rowcari);

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			tdpDiskusi = new MyCheckboxConfig("Tampilkan hanya yang ada diskusi");
			tdpDiskusi.setParent(rowcari);

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			tdpCatatan = new MyCheckboxConfig("Tampilkan hanya yang ada catatan");
			tdpCatatan.setParent(rowcari);

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			tdpVideo = new MyCheckboxConfig("Tampilkan hanya yang ada video");
			tdpVideo.setParent(rowcari);

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			tdpAudio = new MyCheckboxConfig("Tampilkan hanya yang ada audio");
			tdpAudio.setParent(rowcari);

			rowcari = new MyFormRow();
			rowcari.setParent(rowscari);
			tdpDosenPengganti = new MyCheckboxConfig("Tampilkan hanya yang ada dosen pengganti");
			tdpDosenPengganti.setParent(rowcari);

		} else {

			MyToolbarbutton buttonUjian = new MyToolbarbutton("fa-check-square", "Jadwal Ujian");
			buttonUjian.setParent(toolbar);

			buttonUjian.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					final MyWindow window = new MyWindow("Pilih Tahun Akademik dan Semester", "none", true);
					window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					window.setHeight("300px");
					window.setWidth("600px");
					final Combobox tahunAkademik = new Combobox();
					Common.generateTahunAjaran(tahunAkademik);
					final Combobox genapGanjil = new Combobox();
					org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
					comboitem.setLabel(Perkuliahan.GENAP);
					comboitem.setValue(Perkuliahan.GENAP);
					genapGanjil.appendChild(comboitem);
					comboitem = new MyComboitemConfig();
					comboitem.setLabel(Perkuliahan.GANJIL);
					comboitem.setValue(Perkuliahan.GANJIL);
					genapGanjil.appendChild(comboitem);

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setParent(borderlayout);

					MyGrid grid = new MyGrid();
					grid.setWidth("100%");
					grid.setParent(center);
					grid.setHeight("100%");

					Columns columns = new Columns();
					columns.setParent(grid);
					MyColumnConfig column = new MyColumnConfig();
					column.setWidth("20%");
					column.setParent(columns);
					column = new MyColumnConfig();
					column.setParent(columns);

					Rows rows = new Rows();

					rows.setParent(grid);

					MyFormRow row = new MyFormRow();
					row.setValign("top");

					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
					row.appendChild(tahunAkademik);
					tahunAkademik.setWidth("90%");
					tahunAkademik.setReadonly(true);

					row = new MyFormRow();

					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
					row.appendChild(genapGanjil);
					genapGanjil.setWidth("90%");
					genapGanjil.setReadonly(true);

					Common.selectComboItem(genapGanjil,
							Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

					South south = new South();
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					toolbar.setParent(south);
					MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
					cancel.setTooltiptext("Tutup");
					cancel.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();
						}
					});
					cancel.setParent(toolbar);
					MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Cetak", "/img/save.gif");
					save.setTooltiptext("Cetak");
					save.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();

							Tbmuser tbmuser = Common.getCurrentUser();
							Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
							if (mahasiswa == null) {
								String ta = (String) tahunAkademik.getSelectedItem().getValue();
								String jenisSemester = (String) genapGanjil.getSelectedItem().getValue();

								Map<String, Serializable> parameters = ais.common.HashMapGenerator
										.getRandStringSerializable();
								parameters.put("tahun_akademik", ta);
								parameters.put("jenis_semester", jenisSemester);
								parameters.put("dosen",
										tbmuser.ambilDosen() == null ? -1L : tbmuser.getDosen().getId());

								Map<String, Serializable> p1 = ais.common.HashMapGenerator
										.getRandStringSerializable(parameters);
								p1.put("jenis", "UTS");

								Map<String, Serializable> p2 = ais.common.HashMapGenerator
										.getRandStringSerializable(parameters);
								p2.put("jenis", "UAS");

								Report.generatePDFReport(Report.PDF, new Map[] { p1, p2 },
										new String[] { "Jadwal_Ujian_Dosen", "Jadwal_Ujian_Dosen" },
										new String[] { "Jadwal UTS", "Jadwal UAS" }, ais.ui.util.WaktuUtil.getDate());
							} else {
								String ta = (String) tahunAkademik.getSelectedItem().getValue();
								Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
								Integer tahapan = null;
								int semester = Common.getSemester(mahasiswa.getTahunangkatan(),
										(String) genapGanjil.getSelectedItem().getValue(),
										mahasiswa.getPindahKeKampusIniMasukSemester(), tahun,
										mahasiswa.getSemesterMulai());

								KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan,
										null);

								Map<String, Serializable> parameters = ais.common.HashMapGenerator
										.getRandStringSerializable();
								parameters.put("semester", semester);
								parameters.put("semesterNext", semester);
								parameters.put("tahapan", tahapan);
								parameters.put("mahasiswa", mahasiswa.getId());
								parameters.put("nim_mahasiswa", mahasiswa.getNim());
								parameters.put("semester_mahasiswa", semester);
								parameters.put("tahunAkademik_mahasiswa", tahunAkademik);
								parameters.put("tanggal",
										Common.dateFormat6.get().format(ais.ui.util.WaktuUtil.getDate()));
								mahasiswa.putPhoto(parameters);

								Jurusan jurusan = mahasiswa.getJurusan();
								Dosen dosen = jurusan.getKaprodi();
								parameters.put("kaprodi", dosen == null ? "(                                          )"
										: dosen.getNama());
								parameters.put("nip", dosen == null ? "" : dosen.getCode());

								parameters.put("semester_pendek", null);
								parameters.put("namamahasiswa", mahasiswa.getNama());
								parameters.put("namafakultas", mahasiswa.getJurusan().getFakultas().getNama());
								parameters.put("dosenpa", krsMahasiswa.getDosenPa() == null ? "......................."
										: krsMahasiswa.getDosenPa().getNama());
								parameters.put("nuptkosenpa",
										krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNuptk());
								parameters.put("nipdosenpa",
										krsMahasiswa.getDosenPa() == null ? "......................."
												: (krsMahasiswa.getDosenPa().getCode().isEmpty()
														? krsMahasiswa.getDosenPa().getNidn()
														: krsMahasiswa.getDosenPa().getCode()));

								Double[] batas = Common.getMinDanMaxIPK(mahasiswa, semester,
										krsMahasiswa.getSemesterPendek());
								Integer maxsks = batas[0].intValue();

								parameters.put("maksimum_sks", maxsks);

								Map<String, Serializable> p1 = ais.common.HashMapGenerator
										.getRandStringSerializable(parameters);
								p1.put("jenis", "UTS");

								Map<String, Serializable> p2 = ais.common.HashMapGenerator
										.getRandStringSerializable(parameters);
								p2.put("jenis", "UAS");

								Report.generatePDFReport(Report.PDF, new Map[] { p1, p2 },
										new String[] { "Jadwal_UTS", "Jadwal_UTS" },
										new String[] { "Jadwal UTS", "Jadwal UAS" }, ais.ui.util.WaktuUtil.getDate());
							}
						}
					});
					save.setParent(toolbar);
					window.onModal();
				}
			});

			Tbmuser tbmuser = Common.getCurrentUser();
			Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
			MyToolbarbutton petugas = new MyToolbarbutton("fa-user-secret", "Pengawas Ujian");
			petugas.setParent(toolbar);
			petugas.setVisible(mahasiswa == null);
			petugas.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					final MyWindow window = new MyWindow("Pilih Tahun Akademik dan Semester", "none", true);
					window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					window.setHeight("300px");
					window.setWidth("600px");
					final Combobox tahunAkademik = new Combobox();
					Common.generateTahunAjaran(tahunAkademik);
					final Combobox genapGanjil = new Combobox();
					org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
					comboitem.setLabel(Perkuliahan.GENAP);
					comboitem.setValue(Perkuliahan.GENAP);
					genapGanjil.appendChild(comboitem);
					comboitem = new MyComboitemConfig();
					comboitem.setLabel(Perkuliahan.GANJIL);
					comboitem.setValue(Perkuliahan.GANJIL);
					genapGanjil.appendChild(comboitem);

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setParent(borderlayout);

					MyGrid grid = new MyGrid();
					grid.setWidth("100%");
					grid.setParent(center);
					grid.setHeight("100%");

					Columns columns = new Columns();
					columns.setParent(grid);
					MyColumnConfig column = new MyColumnConfig();
					column.setWidth("20%");
					column.setParent(columns);
					column = new MyColumnConfig();
					column.setParent(columns);

					Rows rows = new Rows();

					rows.setParent(grid);

					MyFormRow row = new MyFormRow();
					row.setValign("top");

					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
					row.appendChild(tahunAkademik);
					tahunAkademik.setWidth("90%");
					tahunAkademik.setReadonly(true);

					row = new MyFormRow();

					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
					row.appendChild(genapGanjil);
					genapGanjil.setWidth("90%");
					genapGanjil.setReadonly(true);

					Common.selectComboItem(genapGanjil,
							Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

					row = new MyFormRow();

					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Pengawas"));
					final AmbilDataPegawaiBanbox ambilDataPegawai;
					row.appendChild(ambilDataPegawai = new AmbilDataPegawaiBanbox());
					ambilDataPegawai.setWidth("90%");
					ambilDataPegawai.setReadonly(true);

					Common.initKeterangan(rows, "* Kosongkan pengawas jika ingin tampil semua pegawas");

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
							window.detach();
						}
					});
					cancel.setParent(toolbar);

					MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Cetak", "/img/save.gif");
					save.setTooltiptext("Cetak");
					save.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();

							Pegawai pegawai = (Pegawai) ambilDataPegawai.getAttribute("pegawai");

							String ta = (String) tahunAkademik.getSelectedItem().getValue();
							String jenisSemester = (String) genapGanjil.getSelectedItem().getValue();

							Map<String, Serializable> parameters = ais.common.HashMapGenerator
									.getRandStringSerializable();
							parameters.put("tahun_akademik", ta);
							parameters.put("jenis_semester", jenisSemester);
							parameters.put("pengawas", pegawai == null || pegawai.getId() == null ? -1L : pegawai.getId());

							Map<String, Serializable> p1 = ais.common.HashMapGenerator
									.getRandStringSerializable(parameters);
							p1.put("jenis", "UTS");

							Map<String, Serializable> p2 = ais.common.HashMapGenerator
									.getRandStringSerializable(parameters);
							p2.put("jenis", "UAS");

							Report.generatePDFReport(Report.PDF, new Map[] { p1, p2 },
									new String[] { "Jadwal_Pengawas_Ujian", "Jadwal_Pengawas_Ujian" },
									new String[] { "Pengawas UTS", "Pengawas UAS" }, ais.ui.util.WaktuUtil.getDate());

						}
					});
					save.setParent(toolbar);
					window.onModal();
				}
			});
		}

		bulanSd = initBulanSd();
		toolbar.appendChild(bulanSd);
		bulanSd.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					if (tbmuser != null && tbmuser.getMahasiswa() != null) {
						tbmuser.getMahasiswa().put(bulanSd.getSelectedItem().getValue().toString(), "bulanSd");
					}
					if (tbmuser != null && tbmuser.getSiswa() != null) {
						tbmuser.getSiswa().put(bulanSd.getSelectedItem().getValue().toString(), "bulanSd");
					} else if (tbmuser != null) {
						tbmuser.put(bulanSd.getSelectedItem().getValue().toString(), "bulanSd");
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:1450");
				}
				reloadBlnSd = true;
				initSpreadsheet(true, true);
			}
		});

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		this.grid = new Grid();
		this.grid.setSclass("fgrid");
		this.grid.setOddRowSclass("non-odd");
		this.grid.setVflex("1"); // Mengisi sisa ruang ke bawah secara dinamis
		center.appendChild(this.grid);

		rowsUtama = new Rows();
		rowsUtama.setParent(this.grid);

		paging = new Paging();
		paging.setHeight("30px");

		initSpreadsheet(false, true);
	}

	/**
	 * Membangun Komponen Ringkasan Dasbor Interaktif (Full ZK Components).
	 * Mengambil referensi data langsung dari objek pertemuansa secara efisien tanpa
	 * beban N+1 query. Angka-angka pada Dasbor kini bersifat interaktif dan dapat
	 * diklik untuk memunculkan Popup rincian.
	 */
	public static Component buildDashboardSummary(final Tbmuser currentUser, final boolean refresh,
			final java.util.Map<String, Long> idsPertemuan) {
		if (currentUser == null)
			return null;

		final java.util.TreeMap<String, Long> dataPertemuan = normalizePertemuanIds(idsPertemuan);

		final Vbox wrapperDasbor = new Vbox();
		wrapperDasbor.setWidth("100%");
		wrapperDasbor.setStyle(
				"background:#f6f8fb; border-bottom:1px solid #e5e7eb; padding:0 0 10px 0; box-sizing:border-box;");
		wrapperDasbor.appendChild(new Html(buildELearningDashboardHeroHtml()));

		// Header dan Tombol Refresh
		Hbox headerBox = new Hbox();
		headerBox.setWidth("100%");
		headerBox.setAlign("center");
		headerBox.setPack("end");
		headerBox.setStyle("padding:8px 15px 0px 15px; box-sizing:border-box;");

		final MyToolbarbutton btnRefresh = new MyToolbarbutton("fa-refresh", "Refresh Ringkasan Dasbor");
		btnRefresh.setStyle("font-size:11px; font-weight:800; border-radius:999px; padding:6px 12px;");
		headerBox.appendChild(btnRefresh);
		wrapperDasbor.appendChild(headerBox);

		final Vbox containerDasborGrid = new Vbox();
		containerDasborGrid.setWidth("100%");
		Html htmlLoading = new Html(buildELearningLoadingHtml(
				"Mengambil ringkasan akses, materi, tugas, ujian, audio, video, kehadiran, dan kelengkapan dokumen perkuliahan...",
				8));
		containerDasborGrid.appendChild(htmlLoading);
		wrapperDasbor.appendChild(containerDasborGrid);

		final boolean[] isForcedRefresh = new boolean[] { refresh };
		final boolean[] tampilkanRincianLengkap = new boolean[] { false };
		final EventListener[] prosesLoadDataRef = new EventListener[1];

		final EventListener prosesLoadData = new EventListener() {

			private boolean adaAksesNonAdmin(TreeMap<String, String> d) {
				if (d == null || d.isEmpty())
					return false;
				for (String user : d.keySet()) {
					try {
						String[] u = user.split("-");
						if (u.length >= 3) {
							String tipe = u[2];
							if (tipe.equalsIgnoreCase("Dosen") || tipe.equalsIgnoreCase("Guru")
									|| tipe.equalsIgnoreCase("Mahasiswa") || tipe.equalsIgnoreCase("Siswa")
									|| tipe.equalsIgnoreCase("CalonMahasiswa") || tipe.equalsIgnoreCase("CalonSiswa")) {
								return true;
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:1535");
					}
				}
				return false;
			}

			private int countAksesPesertaDidik(TreeMap<String, String> d) {
				if (d == null || d.isEmpty())
					return 0;
				int count = 0;
				for (String user : d.keySet()) {
					try {
						String[] u = user.split("-");
						if (u.length >= 3) {
							String tipe = u[2];
							if (tipe.equalsIgnoreCase("Mahasiswa") || tipe.equalsIgnoreCase("Siswa")
									|| tipe.equalsIgnoreCase("CalonMahasiswa") || tipe.equalsIgnoreCase("CalonSiswa")) {
								count++;
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:1555");
					}
				}
				return count;
			}

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (dataPertemuan == null || dataPertemuan.isEmpty()) {
					if (containerDasborGrid != null) {
						Common.clear(containerDasborGrid);
					}
					containerDasborGrid.appendChild(new Html(buildEmptyDashboardHtml(
							"Tidak ada data pertemuan untuk dashboard pada rentang/filter ini.")));
					return;
				}

				// ──────────────────────────────────────────────────────────────────────────
				// JALUR CEPAT (instan dari cache): untuk SEMUA peran saat angka per-pertemuan
				// sudah hangat di ElearningRingkasanCache dan BUKAN Refresh paksa. Menampilkan
				// kartu ringkasan responsif TANPA mengiterasi pertemuan. Tombol "Lihat Rincian
				// Lengkap" menjalankan perhitungan detail (loop di bawah) APA ADANYA -> drill-
				// down & rekap tetap utuh. Gagal-aman: bila bermasalah, lanjut ke perhitungan
				// lengkap di bawah (tidak ada logika yang hilang/dimatikan).
				// ──────────────────────────────────────────────────────────────────────────
				try {
					// Mode ringkas (kartu modern) kini berlaku untuk SEMUA peran saat muat pertama
					// (bukan Refresh paksa). Sebelumnya khusus admin; dilonggarkan atas permintaan agar
					// dosen/guru/mahasiswa/siswa juga mendapat tampilan ringkas ini. Rincian per-pengguna
					// tetap tersedia lewat tombol "Lihat Rincian Lengkap" (loop perhitungan di bawah).
					boolean fastPathRingkas = currentUser != null;
					if (!tampilkanRincianLengkap[0] && fastPathRingkas && dataPertemuan != null
							&& !dataPertemuan.isEmpty()) {
						// Selalu tampilkan ringkasan kartu modern (tidak lagi menuntut cache 100% lengkap,
						// yang membuat tampilan "kembali ke versi lama"). Pertemuan yang belum ter-cache
						// dipanaskan on-demand (dibatasi) agar angkanya akurat.
						ais.common.ElearningRingkasanCache.Ringkasan ringkasanInstan = ais.common.ElearningRingkasanCache
								.pastikanDanJumlah(dataPertemuan.values(), true, 4000);
						if (containerDasborGrid != null) {
							Common.clear(containerDasborGrid);
						}
						isForcedRefresh[0] = false;
						tampilkanRincianLengkap[0] = false;

						// ── TAHAP 1 (langsung): kartu ringkasan tampil LEBIH DULU ─────────────
						// Angka penting (akses pertemuan, kehadiran, materi, tugas) muncul lebih dulu
						// TANPA menunggu agregasi grafik, sehingga tampilan terisi bertahap ("diangsur")
						// alih-alih semua muncul sekaligus saat semua selesai.
						containerDasborGrid.appendChild(
								new Html(ElearningKartuRingkasanHelper.htmlRingkasan(ringkasanInstan, true)));

						// Penanda ramping "masih menyiapkan grafik" agar pengguna tahu masih ada yang
						// menyusul — bukan layar yang berhenti. Dilepas begitu grafik siap ditempel.
						final Html penandaGrafik = new Html(
								buildInlineMiniLoadingHtml("Menyiapkan grafik aktivitas pembelajaran…"));
						containerDasborGrid.appendChild(penandaGrafik);

						// ── TAHAP 2 (tick berikutnya): grafik dihitung & ditempel SETELAH kartu ──
						// Agregasi radar/tren/corong ditunda ke round-trip berikutnya lewat timer
						// TANPA busy-overlay, agar kartu di atas sudah tampil di layar sebelum
						// pekerjaan grafik dimulai. Reuse penuh helper HTML/CSS yang sama.
						Common.createDefaultTimerNoBusy(new EventListener() {
							@Override
							public void onEvent(Event evGrafik) throws Exception {
								// Jaga-jaga: bila pengguna menekan Refresh / "Lihat Rincian Lengkap"
								// SEBELUM tick ini berjalan, container sudah di-clear sehingga penanda
								// ikut terlepas. Hentikan tahap grafik+tombol agar tidak menempel konten
								// basi ke tampilan yang sedang dibangun ulang.
								if (penandaGrafik.getParent() == null) {
									return;
								}
								try {
									String spiderKelas = ElearningKartuRingkasanHelper.htmlSpiderKelas(
											ais.common.ElearningRingkasanCache.aktivitasPerKelas(dataPertemuan.values(),
													6));
									String tren = ElearningKartuRingkasanHelper.htmlTrenKehadiran(
											ais.common.ElearningRingkasanCache
													.trenKehadiranMingguan(dataPertemuan.values()));
									String funnel = ElearningKartuRingkasanHelper.htmlFunnelPembelajaran(
											ais.common.ElearningRingkasanCache
													.funnelPembelajaran(dataPertemuan.values()));
									// "Tren Kehadiran" dijadikan tab UTAMA: ditaruh paling depan agar menjadi
									// tab pertama (kiri) sekaligus tab yang aktif secara default.
									String tabs = KartuRingkasanKit.tabbox(
											new String[] { "Tren Kehadiran", "Profil per Kelas",
													"Perjalanan Pembelajaran" },
											new String[] { tren, spiderKelas, funnel });
									try {
										penandaGrafik.detach();
									} catch (Throwable abaikanLepas) { ais.common.ErrorAuditUtil.record(abaikanLepas, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:1645");
									}
									if (tabs != null && tabs.length() > 0) {
										containerDasborGrid.appendChild(new Html(tabs));
									}
								} catch (Throwable abaikanGrafik) {
									try {
										penandaGrafik.detach();
									} catch (Throwable abaikanLepas) { ais.common.ErrorAuditUtil.record(abaikanLepas, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:1653");
									}
								}

								// ── TAHAP 3 (tick berikutnya): tombol rincian muncul TERAKHIR ──
								Common.createDefaultTimerNoBusy(new EventListener() {
									@Override
									public void onEvent(Event evTombol) throws Exception {
										Hbox barRincian = new Hbox();
										barRincian.setWidth("100%");
										barRincian.setPack("center");
										barRincian.setStyle("padding:0 15px 14px; box-sizing:border-box;");
										MyToolbarbutton btnRincian = new MyToolbarbutton("fa-list-ul",
												"Lihat Rincian Lengkap");
										btnRincian.setStyle(
												"font-size:11px; font-weight:800; border-radius:999px; padding:6px 14px;");
										btnRincian.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK,
												new EventListener() {
													@Override
													public void onEvent(Event ev) throws Exception {
														if (containerDasborGrid != null) {
															Common.clear(containerDasborGrid);
														}
														containerDasborGrid.appendChild(new Html(buildELearningLoadingHtml(
																"Mengambil rincian lengkap dashboard e-Learning...", 12)));
														Common.createDefaultTimerNoBusy(new EventListener() {
															@Override
															public void onEvent(Event event) throws Exception {
																tampilkanRincianLengkap[0] = true;
																isForcedRefresh[0] = true;
																if (prosesLoadDataRef[0] != null) {
																	prosesLoadDataRef[0].onEvent(null);
																}
															}
														});
													}
												});
										barRincian.appendChild(btnRincian);
										containerDasborGrid.appendChild(barRincian);
									}
								});
							}
						});
						return;
					}
				} catch (Throwable abaikanFastPath) { ais.common.ErrorAuditUtil.record(abaikanFastPath, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:1698");
					// Fallback aman: lanjut ke perhitungan lengkap di bawah.
				}

				try {
					final java.util.concurrent.atomic.AtomicInteger cPertemuanTarget = new java.util.concurrent.atomic.AtomicInteger(
							0);
					final java.util.concurrent.atomic.AtomicInteger cPertemuanDone = new java.util.concurrent.atomic.AtomicInteger(
							0);
					final java.util.concurrent.atomic.AtomicInteger cMateriTarget = new java.util.concurrent.atomic.AtomicInteger(
							0);
					final java.util.concurrent.atomic.AtomicInteger cMateriDone = new java.util.concurrent.atomic.AtomicInteger(
							0);
					final java.util.concurrent.atomic.AtomicInteger cTugasTarget = new java.util.concurrent.atomic.AtomicInteger(
							0);
					final java.util.concurrent.atomic.AtomicInteger cTugasDone = new java.util.concurrent.atomic.AtomicInteger(
							0);
					final java.util.concurrent.atomic.AtomicInteger cUjianTarget = new java.util.concurrent.atomic.AtomicInteger(
							0);
					final java.util.concurrent.atomic.AtomicInteger cUjianDone = new java.util.concurrent.atomic.AtomicInteger(
							0);
					final java.util.concurrent.atomic.AtomicInteger cVideoTarget = new java.util.concurrent.atomic.AtomicInteger(
							0);
					final java.util.concurrent.atomic.AtomicInteger cVideoDone = new java.util.concurrent.atomic.AtomicInteger(
							0);
					final java.util.concurrent.atomic.AtomicInteger cAudioTarget = new java.util.concurrent.atomic.AtomicInteger(
							0);
					final java.util.concurrent.atomic.AtomicInteger cAudioDone = new java.util.concurrent.atomic.AtomicInteger(
							0);

					// List Penampung untuk Popup Rincian Data
					final java.util.concurrent.ConcurrentLinkedQueue<Object[]> listPertemuanDone = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();
					final java.util.concurrent.ConcurrentLinkedQueue<Object[]> listPertemuanBelum = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();
					final java.util.concurrent.ConcurrentLinkedQueue<Object[]> listPertemuanTarget = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();

					final java.util.concurrent.ConcurrentLinkedQueue<Object[]> listMateriDone = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();
					final java.util.concurrent.ConcurrentLinkedQueue<Object[]> listMateriBelum = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();
					final java.util.concurrent.ConcurrentLinkedQueue<Object[]> listMateriTarget = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();

					final java.util.concurrent.ConcurrentLinkedQueue<Object[]> listTugasDone = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();
					final java.util.concurrent.ConcurrentLinkedQueue<Object[]> listTugasBelum = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();
					final java.util.concurrent.ConcurrentLinkedQueue<Object[]> listTugasTarget = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();

					final java.util.concurrent.ConcurrentLinkedQueue<Object[]> listUjianDone = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();
					final java.util.concurrent.ConcurrentLinkedQueue<Object[]> listUjianBelum = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();
					final java.util.concurrent.ConcurrentLinkedQueue<Object[]> listUjianTarget = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();

					final java.util.concurrent.ConcurrentLinkedQueue<Object[]> listVideoDone = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();
					final java.util.concurrent.ConcurrentLinkedQueue<Object[]> listVideoBelum = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();
					final java.util.concurrent.ConcurrentLinkedQueue<Object[]> listVideoTarget = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();

					final java.util.concurrent.ConcurrentLinkedQueue<Object[]> listAudioDone = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();
					final java.util.concurrent.ConcurrentLinkedQueue<Object[]> listAudioBelum = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();
					final java.util.concurrent.ConcurrentLinkedQueue<Object[]> listAudioTarget = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();

					final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> cLampiranUploadByJenis = new java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger>();
					final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentLinkedQueue<Object[]>> listLampiranUploadByJenis = new java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentLinkedQueue<Object[]>>();
					final java.util.TreeMap<Long, Pertemuan> perkuliahanPertemuanMap = new java.util.TreeMap<Long, Pertemuan>();

					final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> cKehadiranDosenByKode = createStatusCounterMap();
					final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> cKehadiranMahasiswaByKode = createStatusCounterMap();
					final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> cKehadiranSiswaByKode = createStatusCounterMap();
					final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> cKehadiranGuruByKode = createStatusCounterMap();
					final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentLinkedQueue<Object[]>> listKehadiranDosenByKode = createStatusQueueMap();
					final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentLinkedQueue<Object[]>> listKehadiranMahasiswaByKode = createStatusQueueMap();
					final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentLinkedQueue<Object[]>> listKehadiranSiswaByKode = createStatusQueueMap();
					final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentLinkedQueue<Object[]>> listKehadiranGuruByKode = createStatusQueueMap();

					boolean currentRefresh = isForcedRefresh[0];

					final boolean isMhs = currentUser.getMahasiswa() != null;
					final boolean isSiswa = currentUser.getSiswa() != null;
					final boolean isCalonMhs = currentUser.getBiodataCalonMahasiswa() != null;
					final boolean isCalonSiswa = currentUser.getCalonSiswa() != null;
					final boolean isPesertaDidik = isMhs || isSiswa || isCalonMhs || isCalonSiswa;

					final boolean isDosen = currentUser.ambilDosen() != null;
					final boolean isGuru = currentUser.ambilGuru() != null;
					final boolean isPengajar = isDosen || isGuru;
					final boolean isAdmin = !isPesertaDidik && !isPengajar;

					Long myIdTemp = null;
					String myIdStrTemp = "";
					if (isMhs) {
						myIdTemp = currentUser.getMahasiswa().getId();
						myIdStrTemp = myIdTemp.toString();
					} else if (isSiswa) {
						myIdTemp = currentUser.getSiswa().getId();
						myIdStrTemp = myIdTemp.toString();
					} else if (isCalonMhs) {
						myIdTemp = currentUser.getBiodataCalonMahasiswa().getId();
						myIdStrTemp = myIdTemp.toString();
					} else if (isCalonSiswa) {
						myIdTemp = currentUser.getCalonSiswa().getId();
						myIdStrTemp = myIdTemp.toString();
					}
					final Long myId = myIdTemp;
					final String myIdStr = myIdStrTemp;

					// 1. Hitung Akses Pertemuan dan Kehadiran (Base)
					// Catatan: versi ini sengaja tetap memakai data lama dari
					// pertemuan.getAbsensi(),
					// namun loading dibuat lebih informatif agar user mengetahui progres proses
					// ketika jumlah pertemuan sangat banyak.
					final int totalPertemuanKehadiran = dataPertemuan.size();
					int indexPertemuanKehadiran = 0;
					int lastProgressKehadiran = -1;
					int intervalUpdateKehadiran = totalPertemuanKehadiran <= 100 ? 5
							: Math.max(10, totalPertemuanKehadiran / 25);
					long mulaiProsesKehadiran = System.currentTimeMillis();
					updateDashboardLoading(containerDasborGrid,
							buildKehadiranLoadingMessage(0, totalPertemuanKehadiran, 0, mulaiProsesKehadiran), 10);

					for (Long idPer : dataPertemuan.values()) {
						indexPertemuanKehadiran++;
						Pertemuan p = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, idPer.toString(), true);
						if (p == null || !p.getAktif()) {
							int persenLoopSkip = hitungPersen(indexPertemuanKehadiran, totalPertemuanKehadiran);
							if (shouldUpdateKehadiranProgress(indexPertemuanKehadiran, totalPertemuanKehadiran,
									intervalUpdateKehadiran, persenLoopSkip, lastProgressKehadiran)) {
								lastProgressKehadiran = persenLoopSkip;
								updateDashboardLoading(containerDasborGrid,
										buildKehadiranLoadingMessage(indexPertemuanKehadiran, totalPertemuanKehadiran,
												persenLoopSkip, mulaiProsesKehadiran),
										10 + (int) Math.round(persenLoopSkip * 0.14));
							}
							continue;
						}

						try {
							if (p.getPerkuliahan() != null && p.getPerkuliahan().getId() != null
									&& !perkuliahanPertemuanMap.containsKey(p.getPerkuliahan().getId())) {
								perkuliahanPertemuanMap.put(p.getPerkuliahan().getId(), p);
							}
						} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:1832");
						}

						int totalPesertaDidik = (p.ambilMahasiswa() != null ? p.ambilMahasiswa().size() : 0)
								+ (p.ambilSiswa() != null ? p.ambilSiswa().size() : 0);

						int localTarget = (isPesertaDidik || isAdmin) ? 1 : totalPesertaDidik;
						int localDone = 0;

						if (isPesertaDidik) {
							TreeMap<String, String> dAkses = p.ambilData("akses", myIdStr);
							if (dAkses != null && !dAkses.isEmpty())
								localDone = 1;
						} else if (isAdmin) {
							TreeMap<String, String> dAkses = p.ambilData("akses", null);
							if (adaAksesNonAdmin(dAkses))
								localDone = 1;
						} else {
							TreeMap<String, String> dAkses = p.ambilData("akses", null);
							localDone = countAksesPesertaDidik(dAkses);
						}

						cPertemuanTarget.addAndGet(localTarget);
						cPertemuanDone.addAndGet(localDone);

						Object[] objP = new Object[] { p, p, p.getTanggal() };
						listPertemuanTarget.add(objP);
						if (localDone > 0)
							listPertemuanDone.add(objP);
						if (localDone < localTarget)
							listPertemuanBelum.add(objP);

						akumulasiKehadiranPertemuan(p, "Dosen", cKehadiranDosenByKode, listKehadiranDosenByKode);
						akumulasiKehadiranPertemuan(p, "Mahasiswa", cKehadiranMahasiswaByKode,
								listKehadiranMahasiswaByKode);
						akumulasiKehadiranPertemuan(p, "Siswa", cKehadiranSiswaByKode, listKehadiranSiswaByKode);
						akumulasiKehadiranPertemuan(p, "Guru", cKehadiranGuruByKode, listKehadiranGuruByKode);

						int persenLoop = hitungPersen(indexPertemuanKehadiran, totalPertemuanKehadiran);
						if (shouldUpdateKehadiranProgress(indexPertemuanKehadiran, totalPertemuanKehadiran,
								intervalUpdateKehadiran, persenLoop, lastProgressKehadiran)) {
							lastProgressKehadiran = persenLoop;
							int persenGlobal = 10 + (int) Math.round(persenLoop * 0.14);
							if (persenGlobal > 24) {
								persenGlobal = 24;
							}
							updateDashboardLoading(containerDasborGrid,
									buildKehadiranLoadingMessage(indexPertemuanKehadiran, totalPertemuanKehadiran,
											persenLoop, mulaiProsesKehadiran),
									persenGlobal);
						}
					}

					updateDashboardLoading(containerDasborGrid,
							"Selesai menghitung data kehadiran. Melanjutkan proses dokumen perkuliahan...", 24);
					updateDashboardLoading(containerDasborGrid,
							"Menghitung kelengkapan dokumen perkuliahan memakai SQL aggregate batch...", 24);
					final java.util.List<String> jenisLampiranRekap = buildJenisLampiranRekapELearning();
					if (perkuliahanPertemuanMap != null && !perkuliahanPertemuanMap.isEmpty()
							&& jenisLampiranRekap != null && !jenisLampiranRekap.isEmpty()) {
						try {
							java.util.List<Long> perkuliahanIds = new java.util.ArrayList<Long>(
									perkuliahanPertemuanMap.keySet());
							for (int idxChunk = 0; idxChunk < perkuliahanIds.size(); idxChunk += 1000) {
								java.util.List<Long> chunk = perkuliahanIds.subList(idxChunk,
										Math.min(perkuliahanIds.size(), idxChunk + 1000));
								StringBuilder inIds = new StringBuilder();
								for (Long id : chunk) {
									if (id == null)
										continue;
									if (inIds.length() > 0)
										inIds.append(',');
									inIds.append(id.longValue());
								}
								StringBuilder inJenis = new StringBuilder();
								for (String jenisLampiran : jenisLampiranRekap) {
									if (jenisLampiran == null)
										continue;
									if (inJenis.length() > 0)
										inJenis.append(',');
									inJenis.append('\'').append(jenisLampiran.replace("'", "''")).append('\'');
								}
								if (inIds.length() == 0 || inJenis.length() == 0)
									continue;
								String sqlLampiran = "select id, ref, jenis, nama from lampiran_lain where ref in ("
										+ inIds.toString() + ") and jenis in (" + inJenis.toString()
										+ ") order by jenis, ref, id";
								java.util.List dataLampiran;
								try {
									dataLampiran = Common.ambilSqlStreaming(sqlLampiran);
								} catch (Exception eLampiran) {
									ais.common.Common.tampilErrorJikaAdmin(eLampiran);
									continue;
								}
								if (dataLampiran == null)
									continue;
								for (Object objLampiranRow : dataLampiran) {
									try {
										Object[] rowLampiran = (Object[]) objLampiranRow;
										Long lampiranId = getLongSqlValue(rowLampiran, 0);
										Long perkuliahanId = getLongSqlValue(rowLampiran, 1);
										String jenisLampiran = getStringSqlValue(rowLampiran, 2);
										String namaLampiran = getStringSqlValue(rowLampiran, 3);
										if (lampiranId == null || perkuliahanId == null || jenisLampiran.length() == 0)
											continue;
										Pertemuan contohPertemuan = perkuliahanPertemuanMap.get(perkuliahanId);
										if (contohPertemuan == null)
											continue;
										String labelJenis = getLabelLampiranDashboardELearning(jenisLampiran);
										java.util.concurrent.atomic.AtomicInteger counterJenis = cLampiranUploadByJenis
												.get(labelJenis);
										if (counterJenis == null) {
											cLampiranUploadByJenis.putIfAbsent(labelJenis,
													new java.util.concurrent.atomic.AtomicInteger(0));
											counterJenis = cLampiranUploadByJenis.get(labelJenis);
										}
										counterJenis.incrementAndGet();
										java.util.concurrent.ConcurrentLinkedQueue<Object[]> queueJenis = listLampiranUploadByJenis
												.get(labelJenis);
										if (queueJenis == null) {
											listLampiranUploadByJenis.putIfAbsent(labelJenis,
													new java.util.concurrent.ConcurrentLinkedQueue<Object[]>());
											queueJenis = listLampiranUploadByJenis.get(labelJenis);
										}
										String linkLampiran = buildLampiranLinkFromSql(lampiranId, perkuliahanId,
												jenisLampiran, namaLampiran);
										LampiranDashboardInfo infoLampiran = new LampiranDashboardInfo(perkuliahanId,
												lampiranId, jenisLampiran, labelJenis, namaLampiran, linkLampiran,
												contohPertemuan);
										queueJenis.add(new Object[] { infoLampiran, contohPertemuan,
												contohPertemuan.getTanggal() });
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:1964");
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:1969");
						}
					}

					// 2. Ambil Semua MyDetail Materi secara Paralel
					updateDashboardLoading(containerDasborGrid,
							"Mengambil data materi, tugas, ujian, video, audio, dan diskusi dari seluruh pertemuan...",
							36);
					TreeMap<String, Object[]> materiMap = MateriDanKomentarHelper.ambilMateri(dataPertemuan,
							currentRefresh, null, false, currentUser);

					// 3. Iterasi Map dengan MULTI-THREADING
					if (materiMap != null && !materiMap.isEmpty()) {
						int threadCount = Math.min(50, materiMap.size());
						java.util.concurrent.ExecutorService executorService = java.util.concurrent.Executors
								.newFixedThreadPool(threadCount);
						final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(
								materiMap.size());

						for (final Object[] objArr : materiMap.values()) {
							executorService.execute(new Runnable() {
								@Override
								public void run() {
									try {
										if (objArr == null || objArr.length < 2)
											return;
										Object item = objArr[0];
										Pertemuan p = (Pertemuan) objArr[1];
										if (item == null || p == null || !p.getAktif())
											return;

										int totalPesertaDidik = (p.ambilMahasiswa() != null ? p.ambilMahasiswa().size()
												: 0) + (p.ambilSiswa() != null ? p.ambilSiswa().size() : 0);
										int localTarget = (isPesertaDidik || isAdmin) ? 1 : totalPesertaDidik;
										int localDone = 0;

										if (item instanceof PertemuanFileContent) {
											PertemuanFileContent m = (PertemuanFileContent) item;
											TreeMap<String, String> dMateri = p.ambilData(
													"bahan_perkulaiahan_" + m.getId(), isPesertaDidik ? myIdStr : null);
											if (isPesertaDidik) {
												if (dMateri != null && !dMateri.isEmpty())
													localDone = 1;
											} else if (isAdmin) {
												if (adaAksesNonAdmin(dMateri))
													localDone = 1;
											} else {
												localDone = countAksesPesertaDidik(dMateri);
											}
											cMateriTarget.addAndGet(localTarget);
											cMateriDone.addAndGet(localDone);
											listMateriTarget.add(objArr);
											if (localDone > 0)
												listMateriDone.add(objArr);
											if (localDone < localTarget)
												listMateriBelum.add(objArr);
										} else if (item instanceof VideoPertemuan) {
											VideoPertemuan v = (VideoPertemuan) item;
											TreeMap<String, String> dVideo = p.ambilData("video_" + v.getId(),
													isPesertaDidik ? myIdStr : null);
											if (isPesertaDidik) {
												if (dVideo != null && !dVideo.isEmpty())
													localDone = 1;
											} else if (isAdmin) {
												if (adaAksesNonAdmin(dVideo))
													localDone = 1;
											} else {
												localDone = countAksesPesertaDidik(dVideo);
											}
											cVideoTarget.addAndGet(localTarget);
											cVideoDone.addAndGet(localDone);
											listVideoTarget.add(objArr);
											if (localDone > 0)
												listVideoDone.add(objArr);
											if (localDone < localTarget)
												listVideoBelum.add(objArr);
										} else if (item instanceof AudioPertemuan) {
											AudioPertemuan a = (AudioPertemuan) item;
											TreeMap<String, String> dAudio = p.ambilData("audio_" + a.getId(),
													isPesertaDidik ? myIdStr : null);
											if (isPesertaDidik) {
												if (dAudio != null && !dAudio.isEmpty())
													localDone = 1;
											} else if (isAdmin) {
												if (adaAksesNonAdmin(dAudio))
													localDone = 1;
											} else {
												localDone = countAksesPesertaDidik(dAudio);
											}
											cAudioTarget.addAndGet(localTarget);
											cAudioDone.addAndGet(localDone);
											listAudioTarget.add(objArr);
											if (localDone > 0)
												listAudioDone.add(objArr);
											if (localDone < localTarget)
												listAudioBelum.add(objArr);
										} else if (item instanceof PertemuanPunyaUjian) {
											PertemuanPunyaUjian u = (PertemuanPunyaUjian) item;
											if (isUjianAktifSafe(u)) {
												if (isPesertaDidik) {
													List<Long> listHasilUjian = u.ambilHasilUjianMahasiswa();
													if (listHasilUjian != null) {
														for (Long ujianId : listHasilUjian) {
															ais.database.model.HasilUjianMahasiswa hsl = (ais.database.model.HasilUjianMahasiswa) GeneralValueObject
																	.ambilData(
																			ais.database.model.HasilUjianMahasiswa.class,
																			ujianId.toString());
															if (hsl != null) {
																if ((isMhs && hsl.getMahasiswa() != null
																		&& hsl.getMahasiswa().getId().equals(myId))
																		|| (isSiswa && hsl.getSiswa() != null && hsl
																				.getSiswa().getId().equals(myId))) {
																	localDone = 1;
																	break;
																}
															}
														}
													}
												} else if (isAdmin) {
													Number numU = u.ambilJumlahHasilUjianMahasiswaTelahIkut(false);
													if (numU != null && numU.intValue() > 0)
														localDone = 1;
												} else {
													Number numU = u.ambilJumlahHasilUjianMahasiswaTelahIkut(false);
													if (numU != null)
														localDone = numU.intValue();
												}
												cUjianTarget.addAndGet(localTarget);
												cUjianDone.addAndGet(localDone);
												listUjianTarget.add(objArr);
												if (localDone > 0)
													listUjianDone.add(objArr);
												if (localDone < localTarget)
													listUjianBelum.add(objArr);
											}
										} else if (item instanceof Tugas) {
											Tugas t = (Tugas) item;
											if (isPesertaDidik) {
												TreeMap<Long, ais.database.model.file.TugasFileContent> tfcs = t
														.ambilTugasFileContentTotal(
																new TreeMap<Long, ais.database.model.file.TugasFileContent>(),
																"", null, 5000);
												if (tfcs != null) {
													for (ais.database.model.file.TugasFileContent tfc : tfcs.values()) {
														if ((isMhs && tfc.getMahasiswa() != null
																&& tfc.getMahasiswa().equals(myId))
																|| (isSiswa && tfc.getSiswa() != null
																		&& tfc.getSiswa().equals(myId))) {
															localDone = 1;
															break;
														}
													}
												}
											} else if (isAdmin) {
												Number numT = t.ambilJumlahTugasFileContent(false);
												if (numT != null && numT.intValue() > 0)
													localDone = 1;
											} else {
												Number numT = t.ambilJumlahTugasFileContent(false);
												if (numT != null)
													localDone = numT.intValue();
											}
											cTugasTarget.addAndGet(localTarget);
											cTugasDone.addAndGet(localDone);
											listTugasTarget.add(objArr);
											if (localDone > 0)
												listTugasDone.add(objArr);
											if (localDone < localTarget)
												listTugasBelum.add(objArr);
										} else if (item instanceof Pertemuan) {
											Pertemuan pertTugas = (Pertemuan) item;
											if (pertTugas.getJudultugas() != null
													&& !pertTugas.getJudultugas().trim().isEmpty()) {
												if (isPesertaDidik) {
													if ((isMhs && pertTugas.ambilJumlahTugasFileContent(
															currentUser.getMahasiswa()) > 0)
															|| (isSiswa && pertTugas.ambilJumlahTugasFileContent(
																	currentUser.getSiswa()) > 0)) {
														localDone = 1;
													}
												} else if (isAdmin) {
													Number numT = pertTugas.ambilJumlahTugasFileContent(false);
													if (numT != null && numT.intValue() > 0)
														localDone = 1;
												} else {
													Number numT = pertTugas.ambilJumlahTugasFileContent(false);
													if (numT != null)
														localDone = numT.intValue();
												}
												cTugasTarget.addAndGet(localTarget);
												cTugasDone.addAndGet(localDone);
												listTugasTarget.add(objArr);
												if (localDone > 0)
													listTugasDone.add(objArr);
												if (localDone < localTarget)
													listTugasBelum.add(objArr);
											}
										}
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:2168");
									} finally {
										latch.countDown();
									}
								}
							});
						}

						try {
							latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						} finally {
							executorService.shutdown();
						}
					}

					updateDashboardLoading(containerDasborGrid,
							"Menyusun card ringkasan dan popup detail dashboard e-Learning...", 86);
					isForcedRefresh[0] = false;
					if (containerDasborGrid != null) {
						Common.clear(containerDasborGrid);
					}

					// ZK Grid Container untuk memegang 6 Buah Card Layout
					org.zkoss.zul.Div gridContainer = new org.zkoss.zul.Div();

					// Deteksi apakah perangkat Mobile atau Desktop
					boolean isMobile = Common.isMobile();
					// Jika Mobile: 2 Kolom (artinya 3 Baris). Jika Desktop: 3 Kolom (artinya 2
					// Baris).
					String gridTemplate = isMobile ? "repeat(1, minmax(0, 1fr))" : "repeat(3, minmax(0, 1fr))";

					gridContainer.setStyle("display:grid; grid-template-columns:" + gridTemplate
							+ "; gap:12px; margin:12px 15px 16px 15px; font-family:Arial, sans-serif;");
					containerDasborGrid.appendChild(gridContainer);

					// ================= DATA CARD =================
					gridContainer.appendChild(generateCardComponent("Akses Pertemuan", "Pertemuan", "#0d6efd",
							"rgba(13,110,253,0.15)", cPertemuanTarget.get(), cPertemuanDone.get(),
							isPesertaDidik ? "Telah Anda Akses" : "Telah Diakses",
							isPesertaDidik ? "Belum Anda Akses" : "Belum Diakses",
							new ArrayList<Object[]>(listPertemuanTarget), new ArrayList<Object[]>(listPertemuanDone),
							new ArrayList<Object[]>(listPertemuanBelum), currentUser, false));

					boolean tampilKehadiranPt = false;
					boolean tampilKehadiranYa = false;
					try {
						boolean[] ptYa = Common.chekPtAtauSekolah();
						tampilKehadiranPt = ptYa != null && ptYa.length > 0 && ptYa[0];
						tampilKehadiranYa = ptYa != null && ptYa.length > 1 && ptYa[1];
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:2220");
					}

					if (tampilKehadiranPt && sumStatusCounterMap(cKehadiranDosenByKode) > 0) {
						gridContainer.appendChild(generateAttendanceCardComponent("Kehadiran Dosen", "#2563eb",
								"rgba(37,99,235,0.12)", cKehadiranDosenByKode, listKehadiranDosenByKode, currentUser));
					}
					if (tampilKehadiranPt && sumStatusCounterMap(cKehadiranMahasiswaByKode) > 0) {
						gridContainer.appendChild(generateAttendanceCardComponent("Kehadiran Mahasiswa", "#16a34a",
								"rgba(22,163,74,0.12)", cKehadiranMahasiswaByKode, listKehadiranMahasiswaByKode,
								currentUser));
					}
					if (tampilKehadiranYa && sumStatusCounterMap(cKehadiranSiswaByKode) > 0) {
						gridContainer.appendChild(generateAttendanceCardComponent("Kehadiran Siswa", "#0d9488",
								"rgba(13,148,136,0.12)", cKehadiranSiswaByKode, listKehadiranSiswaByKode, currentUser));
					}
					if (tampilKehadiranYa && sumStatusCounterMap(cKehadiranGuruByKode) > 0) {
						gridContainer.appendChild(generateAttendanceCardComponent("Kehadiran Guru", "#7c3aed",
								"rgba(124,58,237,0.12)", cKehadiranGuruByKode, listKehadiranGuruByKode, currentUser));
					}

					gridContainer.appendChild(generateCardComponent("Akses Materi", "Materi", "#0dcaf0",
							"rgba(13,202,240,0.15)", cMateriTarget.get(), cMateriDone.get(),
							isPesertaDidik ? "Telah Anda Baca" : "Telah Dibaca",
							isPesertaDidik ? "Belum Anda Baca" : "Belum Dibaca",
							new ArrayList<Object[]>(listMateriTarget), new ArrayList<Object[]>(listMateriDone),
							new ArrayList<Object[]>(listMateriBelum), currentUser, false));

					gridContainer.appendChild(generateDokumenSummaryCardComponent("Dokumen Perkuliahan", "#14b8a6",
							"rgba(20,184,166,0.15)", cLampiranUploadByJenis, listLampiranUploadByJenis, currentUser));

					gridContainer.appendChild(generateCardComponent("Unggah Tugas", "Tugas", "#ffc107",
							"rgba(255,193,7,0.15)", cTugasTarget.get(), cTugasDone.get(),
							isPesertaDidik ? "Telah Anda Upload" : "Telah Diupload",
							isPesertaDidik ? "Belum Anda Upload" : "Belum Diupload",
							new ArrayList<Object[]>(listTugasTarget), new ArrayList<Object[]>(listTugasDone),
							new ArrayList<Object[]>(listTugasBelum), currentUser, true));

					gridContainer.appendChild(generateCardComponent("Ikut Ujian", "Ujian", "#dc3545",
							"rgba(220,53,69,0.15)", cUjianTarget.get(), cUjianDone.get(),
							isPesertaDidik ? "Telah Anda Ikuti" : "Telah Diikuti",
							isPesertaDidik ? "Belum Anda Ikuti" : "Belum Diikuti",
							new ArrayList<Object[]>(listUjianTarget), new ArrayList<Object[]>(listUjianDone),
							new ArrayList<Object[]>(listUjianBelum), currentUser, false));

					gridContainer.appendChild(generateCardComponent("Akses Video", "Video", "#198754",
							"rgba(25,135,84,0.15)", cVideoTarget.get(), cVideoDone.get(),
							isPesertaDidik ? "Telah Anda Lihat" : "Telah Dilihat",
							isPesertaDidik ? "Belum Anda Lihat" : "Belum Dilihat",
							new ArrayList<Object[]>(listVideoTarget), new ArrayList<Object[]>(listVideoDone),
							new ArrayList<Object[]>(listVideoBelum), currentUser, false));

					gridContainer.appendChild(generateCardComponent("Akses Audio", "Audio", "#6f42c1",
							"rgba(111,66,193,0.15)", cAudioTarget.get(), cAudioDone.get(),
							isPesertaDidik ? "Telah Anda Dengar" : "Telah Didengar",
							isPesertaDidik ? "Belum Anda Dengar" : "Belum Didengar",
							new ArrayList<Object[]>(listAudioTarget), new ArrayList<Object[]>(listAudioDone),
							new ArrayList<Object[]>(listAudioBelum), currentUser, false));

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:2280");
					if (containerDasborGrid != null) {
						Common.clear(containerDasborGrid);
					}
					containerDasborGrid.appendChild(new Html(buildErrorDashboardHtml(e)));
				}
			}
		};
		prosesLoadDataRef[0] = prosesLoadData;

		btnRefresh.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (containerDasborGrid != null) {
					Common.clear(containerDasborGrid);
				}
				containerDasborGrid.appendChild(
						new Html(buildELearningLoadingHtml("Memperbarui ringkasan dashboard e-Learning...", 12)));
				Common.createDefaultTimerNoBusy(new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						tampilkanRincianLengkap[0] = false;
						isForcedRefresh[0] = true;
						prosesLoadData.onEvent(null);
					}
				});
			}
		});

		Common.createDefaultTimerNoBusy(prosesLoadData);
		return wrapperDasbor;
	}

	public static Component buildDashboardSummary(final Tbmuser currentUser, final boolean refresh,
			final java.util.List<Long> idsPertemuan) {
		return buildDashboardSummary(currentUser, refresh, normalizePertemuanIds(idsPertemuan));
	}

	/**
	 * Membangun panel OBE per semester menggunakan ZK native components.
	 * Menampilkan filter TA + Semester (wajib), lalu stats card dan tabel status
	 * RPS OBE + CPL/CPMK per Mata Kuliah.
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	public static Component buildObeComponent(final Tbmuser currentUser) {
		final boolean isMhs = currentUser != null && currentUser.getMahasiswa() != null;
		final boolean isDsn = currentUser != null && currentUser.ambilDosen() != null;

		final Vbox wrapper = new Vbox();
		wrapper.setWidth("100%");
		wrapper.setStyle("background:#f8f9fa; min-height:100%; box-sizing:border-box;");

		// ─── Header ──────────────────────────────────────────────────────────────
		wrapper.appendChild(new Html("<div style='margin:12px 15px 0 15px; padding:16px 18px; border-radius:16px;"
				+ "background:linear-gradient(135deg,#198754 0%,#146c43 100%); color:#fff;"
				+ "box-shadow:0 12px 26px rgba(25,135,84,.18);'>"
				+ "<div style='font-size:11px;letter-spacing:.12em;text-transform:uppercase;opacity:.9;font-weight:900;'>Outcome Based Education</div>"
				+ "<div style='font-size:20px;font-weight:900;margin-top:5px;'>Dasbor OBE per Semester</div>"
				+ "<div style='font-size:12px;line-height:1.55;margin-top:7px;opacity:.9;max-width:820px;'>"
				+ "Pilih Tahun Akademik dan Semester untuk melihat status RPS OBE, CPL, CPMK, dan Bahan Kajian.</div></div>"));

		// ─── Filter ──────────────────────────────────────────────────────────────
		final Groupbox gbFilter = new Groupbox();
		gbFilter.setMold("3d");
		gbFilter.setStyle("margin:12px 15px 0 15px; border-radius:12px;");
		gbFilter.appendChild(new MyCaptionStyled("Filter Periode (Wajib)", "/img/svg/filter.svg"));
		wrapper.appendChild(gbFilter);

		Grid gridFilter = new Grid();
		gridFilter.setWidth("100%");
		gbFilter.appendChild(gridFilter);

		Columns colsF = new Columns();
		colsF.setParent(gridFilter);
		MyColumnConfig c1 = new MyColumnConfig();
		c1.setWidth("120px");
		c1.setParent(colsF);
		MyColumnConfig c2 = new MyColumnConfig();
		c2.setParent(colsF);
		MyColumnConfig c3 = new MyColumnConfig();
		c3.setWidth("130px");
		c3.setParent(colsF);
		MyColumnConfig c4 = new MyColumnConfig();
		c4.setParent(colsF);
		MyColumnConfig c5 = new MyColumnConfig();
		c5.setWidth("150px");
		c5.setParent(colsF);

		Rows rowsFilter = new Rows();
		rowsFilter.setParent(gridFilter);
		MyFormRow rowFilter = new MyFormRow();
		rowFilter.setParent(rowsFilter);
		rowFilter.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun Akademik *")));

		final Combobox cbTa = new Combobox();
		cbTa.setWidth("95%");
		cbTa.setReadonly(true);
		Comboitem itemTaAll = new Comboitem("-- Pilih --");
		itemTaAll.setValue("");
		cbTa.appendChild(itemTaAll);
		cbTa.setSelectedItem(itemTaAll);
		if (Common.tahunAngkatans != null) {
			String defTa = Common.getCurrentTahunAkademik();
			for (String ta : Common.tahunAngkatans) {
				Comboitem ci = new Comboitem(ta);
				ci.setValue(ta);
				cbTa.appendChild(ci);
				if (ta != null && ta.equals(defTa)) {
					cbTa.setSelectedItem(ci);
				}
			}
		}
		rowFilter.appendChild(cbTa);

		rowFilter.appendChild(new Label(ais.common.Common.getBahasaConfig("Semester *")));
		final Combobox cbSmt = new Combobox();
		cbSmt.setWidth("95%");
		cbSmt.setReadonly(true);
		Comboitem smtAll = new Comboitem("-- Pilih --");
		smtAll.setValue("");
		cbSmt.appendChild(smtAll);
		cbSmt.setSelectedItem(smtAll);
		boolean isGanjil = Common.isNowSemensterGanjil();
		Comboitem smtG = new Comboitem(Perkuliahan.GANJIL);
		smtG.setValue(Perkuliahan.GANJIL);
		cbSmt.appendChild(smtG);
		Comboitem smtGe = new Comboitem(Perkuliahan.GENAP);
		smtGe.setValue(Perkuliahan.GENAP);
		cbSmt.appendChild(smtGe);
		Comboitem smtSp = new Comboitem(Perkuliahan.SP);
		smtSp.setValue(Perkuliahan.SP);
		cbSmt.appendChild(smtSp);
		cbSmt.setSelectedItem(isGanjil ? smtG : smtGe);
		rowFilter.appendChild(cbSmt);

		final MyToolbarbuttonConfig btnTampilkan = new MyToolbarbuttonConfig("Tampilkan", "/img/svg/search.svg");
		btnTampilkan.setStyle(
				"font-weight:800; background:#198754; color:#fff; border-radius:8px; padding:6px 14px; border:none;");
		rowFilter.appendChild(btnTampilkan);

		// ─── Area Konten ─────────────────────────────────────────────────────────
		final Vbox contentArea = new Vbox();
		contentArea.setWidth("100%");
		contentArea.setStyle("padding:12px 15px;");
		wrapper.appendChild(contentArea);
		contentArea.appendChild(new Html(buildObePlaceholderHtml()));

		// ─── Event Listener Tombol Tampilkan ─────────────────────────────────────
		final EventListener prosesObe = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				String ta = cbTa.getSelectedItem() != null ? (String) cbTa.getSelectedItem().getValue() : "";
				String smt = cbSmt.getSelectedItem() != null ? (String) cbSmt.getSelectedItem().getValue() : "";

				if (ta == null || ta.trim().isEmpty() || smt == null || smt.trim().isEmpty()) {
					Common.clear(contentArea);
					contentArea.appendChild(
							new Html("<div style='padding:20px;text-align:center;color:#dc3545;font-weight:700;'>"
									+ "<span style='font-size:1.5rem;'>&#9888;</span> Tahun Akademik dan Semester wajib dipilih.</div>"));
					return;
				}

				Common.clear(contentArea);
				contentArea.appendChild(new Html(
						buildELearningLoadingHtml("Mengambil data OBE semester " + smt + " " + ta + "...", 10)));

				final String qTa = ta.trim();
				final String qSmt = smt.trim();

				Common.createDefaultTimerNoBusy(new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						Session sess = null;
						try {
							sess = HibernateUtil.getSessionFactory().openSession();

							Criteria crit = sess.createCriteria(Perkuliahan.class, "p")
									.add(Restrictions.eq("p.tahunAjaran", qTa))
									.add(Restrictions.eq("p.ganjilGenap", qSmt))
									.add(Restrictions.isNotNull("p.kurikulumPunyaMatakuliah"))
									.createAlias("p.kurikulumPunyaMatakuliah", "kpm")
									.createAlias("kpm.kurikulum", "kur").add(Restrictions.eq("kur.obe", Boolean.TRUE))
									.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

							if (isDsn && currentUser.ambilDosen() != null) {
								crit.add(Restrictions.eq("p.dosen1", currentUser.ambilDosen()));
							}

							List<Perkuliahan> list = (List<Perkuliahan>) crit.list();

							if (isMhs && currentUser.getMahasiswa() != null) {
								final Long mhsId = currentUser.getMahasiswa().getId();
								List<Long> joined = (List<Long>) sess
										.createCriteria(ais.database.model.Detailperkuliahan.class, "dp")
										.add(Restrictions.eq("dp.mahasiswa.id", mhsId))
										.add(Restrictions.eq("dp.persetujuan",
												ais.database.model.Detailperkuliahan.DISETUJUI))
										.setProjection(Projections.property("dp.perkuliahan.id")).list();
								Set<Long> joinedSet = new HashSet<Long>(joined);
								List<Perkuliahan> filtered = new ArrayList<Perkuliahan>();
								for (Perkuliahan p : list) {
									if (joinedSet.contains(p.getId()))
										filtered.add(p);
								}
								list = filtered;
							}

							int totalMk = list.size(), sudahRps = 0, belumRps = 0, punyaCpl = 0, punyaCpmk = 0;
							double totalMinK = 0;
							int ctrMinK = 0;
							StringBuilder sbRps = new StringBuilder();

							int rowIdx = 0;
							for (Perkuliahan p : list) {
								rowIdx++;
								try {
									ais.database.model.KurikulumPunyaMatakuliah kpm = p.getKurikulumPunyaMatakuliah();
									ais.database.model.Matakuliah mk = p.getMatakuliah();
									if (kpm == null || mk == null)
										continue;

									boolean hasCpl = mk.getCapaianLulusan() != null
											&& !mk.getCapaianLulusan().trim().isEmpty();
									boolean hasCpmk = mk.getCapaianPembelajaranLulusan() != null
											&& !mk.getCapaianPembelajaranLulusan().trim().isEmpty();
									boolean hasRps = kpm.getRincian() != null && !kpm.getRincian().trim().isEmpty()
											&& kpm.getTanggalPenyusunan() != null;
									if (hasRps)
										sudahRps++;
									else
										belumRps++;
									if (hasCpl)
										punyaCpl++;
									if (hasCpmk)
										punyaCpmk++;
									if (kpm.getMinimalKetercapaian() != null && kpm.getMinimalKetercapaian() > 0) {
										totalMinK += kpm.getMinimalKetercapaian();
										ctrMinK++;
									}

									int jCpl = hasCpl ? countUniqueRelationIds(mk.getCapaianLulusan()) : 0;
									int jCpmk = hasCpmk ? countUniqueRelationIds(mk.getCapaianPembelajaranLulusan()) : 0;
									int jBk = mk.getBahanKajian() != null && !mk.getBahanKajian().trim().isEmpty()
											? countUniqueRelationIds(mk.getBahanKajian())
											: 0;
									double minK = kpm.getMinimalKetercapaian() != null ? kpm.getMinimalKetercapaian()
											: 0;

									String namaDosen = p.getDosen1() != null && p.getDosen1().getNama() != null
											? p.getDosen1().getNama()
											: "-";
									String namaProdi = p.getJurusan() != null && p.getJurusan().getNama() != null
											? p.getJurusan().getNama()
											: "-";
									String tglRps = hasRps && kpm.getTanggalPenyusunan() != null
											? Common.dateFormat6.get().format(kpm.getTanggalPenyusunan())
											: "-";
									String badgeRps = hasRps
											? "<span style='background:rgba(25,135,84,.13);color:#146c43;border-radius:6px;padding:2px 9px;font-size:11px;font-weight:700;'>&#10003; Ada</span>"
											: "<span style='background:rgba(220,53,69,.11);color:#b02a37;border-radius:6px;padding:2px 9px;font-size:11px;font-weight:700;'>&#10007; Belum</span>";
									String badgeKunci = kpm.getDikunci() != null
											? "<span style='background:rgba(253,126,20,.13);color:#b85c00;border-radius:6px;padding:2px 7px;font-size:11px;'>&#128274;</span>"
											: "<span style='color:#adb5bd;font-size:11px;'>&#128275;</span>";
									String minKStr = minK > 0
											? "<b style='color:" + (minK >= 70 ? "#198754" : "#fd7e14") + "'>"
													+ String.format("%.0f", minK) + "</b>"
											: "<span style='color:#adb5bd'>—</span>";
									String rowBg = (rowIdx % 2 == 0) ? "#fafffe" : "#fff";

									sbRps.append("<tr style='background:").append(rowBg)
											.append(";border-bottom:1px solid #f1f3f5;'>")
											.append("<td style='padding:9px 8px;font-weight:700;color:#495057;font-size:12px;'>")
											.append(mk.getKode() != null ? mk.getKode() : "").append("</td>")
											.append("<td style='padding:9px 8px;font-size:13px;'>")
											.append(mk.getNama() != null ? mk.getNama() : "").append("</td>");
									if (!isDsn && !isMhs) {
										sbRps.append("<td style='padding:9px 8px;font-size:11px;color:#6c757d;'>")
												.append(namaProdi).append("</td>")
												.append("<td style='padding:9px 8px;font-size:11px;color:#6c757d;'>")
												.append(namaDosen).append("</td>");
									}
									sbRps.append("<td style='padding:9px 8px;text-align:center;font-weight:700;'>")
											.append(mk.getSks() != null ? mk.getSks() : 0).append("</td>")
											.append("<td style='padding:9px 8px;text-align:center;'>").append(badgeRps)
											.append("</td>")
											.append("<td style='padding:9px 8px;text-align:center;font-size:11px;color:#6c757d;'>")
											.append(tglRps).append("</td>")
											.append("<td style='padding:9px 8px;text-align:center;font-size:12px;'><b>")
											.append(jCpl).append("</b></td>")
											.append("<td style='padding:9px 8px;text-align:center;font-size:12px;'><b>")
											.append(jCpmk).append("</b></td>")
											.append("<td style='padding:9px 8px;text-align:center;font-size:12px;'><b>")
											.append(jBk).append("</b></td>")
											.append("<td style='padding:9px 8px;text-align:center;font-size:12px;'>")
											.append(minKStr).append("</td>")
											.append("<td style='padding:9px 8px;text-align:center;'>")
											.append(badgeKunci).append("</td>").append("</tr>");
								} catch (Exception ep) { ais.common.ErrorAuditUtil.record(ep, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:2577");
									/* lewati baris bermasalah */ }
							}

							int pctRps = totalMk > 0 ? (sudahRps * 100 / totalMk) : 0;
							double avgMinK = ctrMinK > 0 ? (totalMinK / ctrMinK) : 0;
							String pctColor = pctRps >= 75 ? "#198754" : (pctRps >= 40 ? "#fd7e14" : "#dc3545");

							String thStyle = "background:#f8f9fa;padding:10px 8px;font-size:11px;font-weight:800;text-transform:uppercase;letter-spacing:.5px;";
							String thStyleC = thStyle + "text-align:center;";
							String colAdmin = (!isDsn && !isMhs) ? "<th style='" + thStyle + "'>Prodi</th>"
									+ "<th style='" + thStyle + "'>Dosen</th>" : "";

							StringBuilder html = new StringBuilder();
							html.append(buildObeStatsHtml(totalMk, sudahRps, belumRps, punyaCpl, punyaCpmk, pctRps,
									pctColor, avgMinK));
							html.append(
									"<div style='background:#fff;border-radius:14px;box-shadow:0 2px 10px rgba(0,0,0,.06);overflow:hidden;margin-top:12px;'>");
							html.append("<div style='padding:13px 16px;border-bottom:1px solid #e9ecef;"
									+ "font-weight:700;font-size:13px;color:#343a40;"
									+ "display:flex;justify-content:space-between;align-items:center;'>"
									+ "<span><span style='color:#198754;margin-right:6px;'>&#9664;</span>Status RPS OBE &mdash; "
									+ qSmt + " " + qTa + "</span>"
									+ "<span style='font-size:11px;font-weight:400;color:#6c757d;'>" + totalMk
									+ " MK</span></div>");
							if (totalMk == 0) {
								html.append("<div style='padding:40px;text-align:center;color:#6c757d;'>"
										+ "<div style='font-size:2.5rem;opacity:.2;margin-bottom:10px;'>&#128218;</div>"
										+ "<b>Tidak ada Mata Kuliah OBE untuk periode ini.</b></div>");
							} else {
								html.append("<div style='overflow-x:auto;'>");
								html.append("<table style='width:100%;border-collapse:collapse;'><thead><tr>")
										.append("<th style='").append(thStyle).append("'>Kode</th>")
										.append("<th style='").append(thStyle).append("'>Mata Kuliah</th>")
										.append(colAdmin).append("<th style='").append(thStyleC).append("'>SKS</th>")
										.append("<th style='").append(thStyleC).append("'>Status RPS</th>")
										.append("<th style='").append(thStyleC).append("'>Tgl Susun</th>")
										.append("<th style='").append(thStyleC).append("'>CPL</th>")
										.append("<th style='").append(thStyleC).append("'>CPMK</th>")
										.append("<th style='").append(thStyleC).append("'>BK</th>")
										.append("<th style='").append(thStyleC).append("'>Min (%)</th>")
										.append("<th style='").append(thStyleC).append("'>Kunci</th>")
										.append("</tr></thead><tbody>").append(sbRps).append("</tbody></table></div>");
							}
							html.append("</div>");

							Common.clear(contentArea);
							contentArea.appendChild(new Html(html.toString()));

						} catch (Exception ex) {
							ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:2627");
							Common.clear(contentArea);
							contentArea.appendChild(new Html(buildErrorDashboardHtml(ex)));
						} finally {
							if (sess != null) {
								try {
									sess.clear();
								} catch (Exception ign) { ais.common.ErrorAuditUtil.record(ign, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:2634");
								}
								try {
									sess.disconnect();
								} catch (Exception ign) { ais.common.ErrorAuditUtil.record(ign, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:2638");
								}
								try {
									closeHibernateSessionQuietly(sess);
								} catch (Exception ign) { ais.common.ErrorAuditUtil.record(ign, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:2642");
								}
							}
						}
					}
				});
			}
		};

		btnTampilkan.addEventListener("onClick", prosesObe);
		cbTa.addEventListener("onChange", prosesObe);
		cbSmt.addEventListener("onChange", prosesObe);

		// Auto-load jika TA dan Semester sudah ada nilai default
		Common.createDefaultTimerNoBusy(new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				String ta = cbTa.getSelectedItem() != null ? (String) cbTa.getSelectedItem().getValue() : "";
				String smt = cbSmt.getSelectedItem() != null ? (String) cbSmt.getSelectedItem().getValue() : "";
				if (ta != null && !ta.isEmpty() && smt != null && !smt.isEmpty()) {
					prosesObe.onEvent(e);
				}
			}
		});

		return wrapper;
	}

	/** Hitung ID relasi CSV legacy tanpa token kosong maupun ID duplikat. */
	private static int countUniqueRelationIds(String csv) {
		Set<Long> ids = new HashSet<Long>();
		if (csv == null || csv.trim().isEmpty())
			return 0;
		for (String token : csv.split(",")) {
			String value = token != null ? token.trim() : "";
			if (value.isEmpty())
				continue;
			try {
				ids.add(Long.valueOf(value));
			} catch (NumberFormatException ignored) {
				// Token rusak bukan relasi yang sah dan tidak boleh dihitung.
			}
		}
		return ids.size();
	}

	private static String buildObePlaceholderHtml() {
		return "<div style='text-align:center;padding:60px 20px;color:#6c757d;'>"
				+ "<div style='font-size:3rem;opacity:.2;margin-bottom:12px;'>&#127891;</div>"
				+ "<h5 style='font-weight:700;'>Pilih Tahun Akademik dan Semester</h5>"
				+ "<p>Data OBE akan muncul setelah Anda memilih periode di atas.</p></div>";
	}

	private static String buildObeStatsHtml(int totalMk, int sudahRps, int belumRps, int punyaCpl, int punyaCpmk,
			int pct, String pctColor, double avgMinK) {

		// Donut ring via CSS conic-gradient
		String donutStyle = "width:130px;height:130px;border-radius:50%;flex-shrink:0;" + "background:conic-gradient("
				+ pctColor + " 0% " + pct + "%, #e9ecef " + pct + "% 100%);"
				+ "display:flex;align-items:center;justify-content:center;position:relative;"
				+ "box-shadow:0 4px 18px rgba(0,0,0,.1);";
		String donutInner = "<div style='width:94px;height:94px;background:#fff;border-radius:50%;"
				+ "position:absolute;display:flex;flex-direction:column;align-items:center;"
				+ "justify-content:center;line-height:1.2;'>" + "<span style='font-size:1.55rem;font-weight:900;color:"
				+ pctColor + ";'>" + pct + "%</span>"
				+ "<span style='font-size:.6rem;font-weight:700;color:#6c757d;text-transform:uppercase;letter-spacing:.5px;'>RPS OBE</span>"
				+ "</div>";

		// Health label
		String healthLabel, healthBg;
		if (pct >= 75) {
			healthLabel = "&#9989; Ketercapaian Baik";
			healthBg = "rgba(25,135,84,.1)";
		} else if (pct >= 40) {
			healthLabel = "&#9888;&#65039; Perlu Perhatian";
			healthBg = "rgba(253,126,20,.1)";
		} else {
			healthLabel = "&#128680; Perlu Tindakan Segera";
			healthBg = "rgba(220,53,69,.1)";
		}

		String avgMinKStr = avgMinK > 0 ? String.format("Rata-rata min. ketercapaian: <b>%.0f%%</b>", avgMinK) : "";

		// Progress card: donut + progress bar side-by-side
		String progressCard = "<div style='background:#fff;border-radius:14px;padding:16px 18px;"
				+ "box-shadow:0 2px 10px rgba(0,0,0,.06);margin-bottom:12px;"
				+ "display:flex;align-items:center;gap:20px;flex-wrap:wrap;'>" + "<div style='" + donutStyle + "'>"
				+ donutInner + "</div>" + "<div style='flex:1;min-width:200px;'>"
				+ "<div style='font-weight:700;font-size:13px;margin-bottom:4px;'>Progres Pengisian RPS OBE</div>"
				+ "<div style='font-size:12px;color:#6c757d;margin-bottom:10px;'>" + sudahRps + " dari " + totalMk
				+ " MK sudah memiliki RPS OBE" + (avgMinKStr.isEmpty() ? "" : " &mdash; " + avgMinKStr) + "</div>"
				+ "<div style='background:#e9ecef;border-radius:50px;height:10px;overflow:hidden;'>"
				+ "<div style='height:100%;border-radius:50px;background:" + pctColor + ";width:" + pct
				+ "%;'></div></div>" + "<div style='display:flex;justify-content:space-between;margin-top:6px;'>"
				+ "<small style='color:#6c757d;'>" + belumRps + " belum terisi</small>"
				+ "<small style='color:#6c757d;'>" + totalMk + " total</small></div>"
				+ "<div style='margin-top:10px;display:inline-block;background:" + healthBg + ";color:" + pctColor + ";"
				+ "border-radius:6px;padding:3px 10px;font-size:11px;font-weight:700;'>" + healthLabel + "</div>"
				+ "</div></div>";

		// 5 stat cards (responsive flex)
		int pctSudah = totalMk > 0 ? sudahRps * 100 / totalMk : 0;
		int pctBelum = totalMk > 0 ? belumRps * 100 / totalMk : 0;
		int pctCpl = totalMk > 0 ? punyaCpl * 100 / totalMk : 0;
		int pctCpmk = totalMk > 0 ? punyaCpmk * 100 / totalMk : 0;
		String cards = "<div style='display:flex;flex-wrap:wrap;gap:10px;margin-bottom:12px;'>"
				+ obeStatCard("Total MK OBE", String.valueOf(totalMk), "#198754", "Kurikulum OBE aktif", 100)
				+ obeStatCard("RPS Terisi", String.valueOf(sudahRps), "#0d6efd", "Sudah punya RPS OBE", pctSudah)
				+ obeStatCard("Belum RPS", String.valueOf(belumRps), "#dc3545", "Belum mengisi RPS OBE", pctBelum)
				+ obeStatCard("Punya CPL", String.valueOf(punyaCpl), "#6f42c1", "MK dengan CPL terdaftar", pctCpl)
				+ obeStatCard("Punya CPMK", String.valueOf(punyaCpmk), "#0dcaf0", "MK dengan CPMK terdaftar", pctCpmk)
				+ "</div>";

		return cards + progressCard;
	}

	private static String obeStatCard(String label, String value, String color, String sub, int pct) {
		return "<div style='flex:1;min-width:140px;background:#fff;border-radius:12px;"
				+ "padding:14px 14px 12px;box-shadow:0 2px 8px rgba(0,0,0,.06);" + "border-left:4px solid " + color
				+ ";'>"
				+ "<div style='display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:4px;'>"
				+ "<span style='font-size:1.7rem;font-weight:900;color:" + color + ";line-height:1;'>" + value
				+ "</span>" + "<span style='font-size:.7rem;font-weight:700;color:" + color + ";background:" + color
				+ "1a;" + "border-radius:4px;padding:2px 6px;align-self:flex-start;'>" + pct + "%</span></div>"
				+ "<div style='font-size:10px;font-weight:800;text-transform:uppercase;letter-spacing:.5px;color:#343a40;margin-bottom:2px;'>"
				+ label + "</div>" + "<div style='font-size:10px;color:#6c757d;margin-bottom:8px;'>" + sub + "</div>"
				+ "<div style='background:#e9ecef;border-radius:50px;height:4px;overflow:hidden;'>"
				+ "<div style='height:100%;border-radius:50px;background:" + color + ";width:" + pct
				+ "%;'></div></div>" + "</div>";
	}

	private static java.util.TreeMap<String, Long> normalizePertemuanIds(
			final java.util.Map<String, Long> idsPertemuan) {
		java.util.TreeMap<String, Long> data = new java.util.TreeMap<String, Long>();
		if (idsPertemuan == null || idsPertemuan.isEmpty()) {
			return data;
		}
		for (java.util.Map.Entry<String, Long> entry : idsPertemuan.entrySet()) {
			if (entry == null || entry.getValue() == null) {
				continue;
			}
			String key = entry.getKey() == null ? entry.getValue().toString() : entry.getKey();
			data.put(key, entry.getValue());
		}
		return data;
	}

	private static java.util.TreeMap<String, Long> normalizePertemuanIds(final java.util.List<Long> idsPertemuan) {
		java.util.TreeMap<String, Long> data = new java.util.TreeMap<String, Long>();
		if (idsPertemuan == null || idsPertemuan.isEmpty()) {
			return data;
		}
		for (Long id : idsPertemuan) {
			if (id != null) {
				data.put(id.toString(), id);
			}
		}
		return data;
	}

	private static void updateDashboardLoading(Vbox container, String message, int percent) {
		try {
			if (container == null) {
				return;
			}
			Common.clear(container);
			container.appendChild(new Html(buildELearningLoadingHtml(message, percent)));
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:2792");
		}
	}

	private static boolean shouldUpdateKehadiranProgress(int current, int total, int interval, int percent,
			int lastPercent) {
		if (total <= 0) {
			return false;
		}
		if (current <= 1 || current >= total) {
			return true;
		}
		if (interval <= 0) {
			interval = 1;
		}
		if (current % interval == 0) {
			return true;
		}
		return percent != lastPercent && percent % 5 == 0;
	}

	private static int hitungPersen(int current, int total) {
		if (total <= 0) {
			return 0;
		}
		int persen = (int) Math.round(current * 100.0 / total);
		if (persen < 0) {
			return 0;
		}
		if (persen > 100) {
			return 100;
		}
		return persen;
	}

	private static String buildKehadiranLoadingMessage(int current, int total, int percent, long startTime) {
		if (total <= 0) {
			return "Menyiapkan data kehadiran pertemuan...";
		}
		long elapsed = System.currentTimeMillis() - startTime;
		String estimasi = "";
		if (current > 0 && elapsed > 0 && current < total) {
			long remaining = (long) ((elapsed * 1.0 / current) * (total - current));
			estimasi = " Estimasi sisa waktu: " + formatDurasiRingkas(remaining) + ".";
		}
		return "Menghitung data kehadiran " + Common.numberFormat.get().format(current) + " dari "
				+ Common.numberFormat.get().format(total) + " pertemuan (" + percent
				+ "%). Sistem sedang membaca absensi dosen, mahasiswa, siswa, dan guru dari data pertemuan." + estimasi;
	}

	private static String formatDurasiRingkas(long millis) {
		if (millis <= 0) {
			return "kurang dari 1 detik";
		}
		long detik = Math.max(1, millis / 1000);
		if (detik < 60) {
			return detik + " detik";
		}
		long menit = detik / 60;
		long sisaDetik = detik % 60;
		if (menit < 60) {
			return menit + " menit" + (sisaDetik > 0 ? " " + sisaDetik + " detik" : "");
		}
		long jam = menit / 60;
		long sisaMenit = menit % 60;
		return jam + " jam" + (sisaMenit > 0 ? " " + sisaMenit + " menit" : "");
	}

	private static String buildELearningDashboardHeroHtml() {
		return "<div style=\"margin:12px 15px 0 15px; padding:16px 18px; border-radius:16px; "
				+ "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); color:#ffffff; "
				+ "box-shadow:0 12px 26px rgba(15,23,42,.14);\">"
				+ "<div style=\"font-size:11px; letter-spacing:.12em; text-transform:uppercase; opacity:.86; font-weight:900;\">Dashboard e-Learning</div>"
				+ "<div style=\"font-size:20px; font-weight:900; margin-top:5px;\">Ringkasan Aktivitas Pembelajaran Digital</div>"
				+ "<div style=\"font-size:12px; line-height:1.55; margin-top:7px; opacity:.9; max-width:820px;\">"
				+ "Melihat siapa yang sudah hadir, membaca materi, mengumpulkan tugas, mengikuti ujian, membuka video/audio, dan melengkapi dokumen pembelajaran."
				+ "</div></div>";
	}

	private static String buildELearningLoadingHtml(String message, int percent) {
		if (percent < 0) {
			percent = 0;
		}
		if (percent > 100) {
			percent = 100;
		}
		String safeMessage = escapeHtml(message == null ? "Memproses data dashboard e-Learning..." : message);
		return "<div style=\"margin:12px 15px; padding:20px; border-radius:18px; background:#ffffff; border:1px solid #e5e7eb; "
				+ "box-shadow:0 14px 32px rgba(15,23,42,.08); color:#0f172a;\">"
				+ "<div style=\"display:flex; align-items:center; justify-content:space-between; gap:12px; flex-wrap:wrap;\">"
				+ "<div><div style=\"font-size:11px; letter-spacing:.12em; text-transform:uppercase; color:#2563eb; font-weight:900;\">Memproses Dashboard e-Learning</div>"
				+ "<div style=\"font-size:18px; font-weight:900; margin-top:6px;\"><i class=\"fa fa-spinner fa-spin\"></i> Mengambil Data Pembelajaran</div>"
				+ "<div style=\"font-size:12px; color:#64748b; margin-top:8px; line-height:1.55;\">" + safeMessage
				+ "</div></div>"
				+ "<div style=\"min-width:86px; text-align:right; font-size:30px; font-weight:900; color:#0f172a;\">"
				+ percent + "%</div></div>"
				+ "<div style=\"height:12px; background:#e2e8f0; border-radius:999px; overflow:hidden; margin-top:18px;\">"
				+ "<div style=\"height:12px; width:" + percent
				+ "%; border-radius:999px; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));\"></div></div>"
				+ "<div style=\"display:flex; gap:8px; flex-wrap:wrap; margin-top:14px; font-size:11px; color:#475569;\">"
				+ "<span style=\"padding:6px 10px; border-radius:999px; background:#eff6ff; color:#1d4ed8; font-weight:800;\">Pertemuan</span>"
				+ "<span style=\"padding:6px 10px; border-radius:999px; background:#ecfdf5; color:#166534; font-weight:800;\">Kehadiran</span>"
				+ "<span style=\"padding:6px 10px; border-radius:999px; background:#ecfeff; color:#0e7490; font-weight:800;\">Materi</span>"
				+ "<span style=\"padding:6px 10px; border-radius:999px; background:#fef3c7; color:#92400e; font-weight:800;\">Tugas & Ujian</span>"
				+ "<span style=\"padding:6px 10px; border-radius:999px; background:#f8fafc; color:#475569; font-weight:800; border:1px solid #e2e8f0;\">Dokumen</span>"
				+ "</div></div>";
	}

	/**
	 * Strip loading RINGKAS (bukan kartu besar) untuk penanda "masih menyiapkan bagian berikutnya"
	 * saat tampilan dicicil bertahap. Dipakai di antara kartu ringkasan dan grafik agar pengguna
	 * tahu ada konten menyusul, bukan layar yang berhenti. Ringan (satu div + spinner).
	 */
	private static String buildInlineMiniLoadingHtml(String message) {
		String safe = escapeHtml(message == null ? "Menyiapkan…" : message);
		return "<div style=\"margin:2px 15px 14px 15px; padding:11px 16px; border-radius:14px; background:#ffffff; "
				+ "border:1px dashed #cbd5e1; color:#475569; font-size:12px; display:flex; align-items:center; gap:10px;\">"
				+ "<i class=\"fa fa-spinner fa-spin\" style=\"color:var(--ais-theme-primary,#2563eb);\"></i>"
				+ "<span>" + safe + "</span></div>";
	}

	private static String buildEmptyDashboardHtml(String message) {
		return "<div style=\"margin:12px 15px; padding:18px; border-radius:16px; background:#ffffff; border:1px dashed #cbd5e1; color:#64748b; text-align:center;\">"
				+ escapeHtml(message) + "</div>";
	}

	private static String buildErrorDashboardHtml(Exception e) {
		String msg = e == null ? "Tidak diketahui" : e.getMessage();
		return "<div style=\"margin:12px 15px; padding:18px; border-radius:16px; background:#fef2f2; border:1px solid #fecaca; color:#991b1b;\">"
				+ "<b>Gagal memuat dashboard e-Learning.</b><br/>" + escapeHtml(msg) + "</div>";
	}

	private static String escapeHtml(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'",
				"&#39;");
	}

	public static java.util.List<String> buildJenisLampiranRekapELearning() {
		java.util.List<String> result = new java.util.ArrayList<String>();
		try {
			if (Common.bolehKonfigurasi("tampilkan_rps")) {
				addJenisLampiranIfAbsent(result, LampiranLain.SILABUS);
			}
			if (Common.bolehKonfigurasi("tampilkan_sap")) {
				addJenisLampiranIfAbsent(result, LampiranLain.SAP);
			}
			if (Common.bolehKonfigurasi("tampilkan_absen_manual")) {
				addJenisLampiranIfAbsent(result, "Absen Manual");
			}
			if (Common.bolehKonfigurasi("tampilkan_soal_uts")) {
				addJenisLampiranIfAbsent(result, "Soal UTS");
			}
			if (Common.bolehKonfigurasi("tampilkan_soal_uas")) {
				addJenisLampiranIfAbsent(result, "Soal UAS");
			}
			if (AktifitasPerkuliahanHelper.lampiranLain != null) {
				for (String t : AktifitasPerkuliahanHelper.lampiranLain) {
					if (t == null || t.trim().length() == 0) {
						continue;
					}
					if (Common.bolehKonfigurasi("tampilkan_" + t, Konfigurasi.TIDAK_AKTIF)) {
						addJenisLampiranIfAbsent(result, t);
					}
				}
			}
			String tampilkanLampiranLainDiAgenda = Common.getKonfigurasi("tampilkan_lampiran_lain_di_agenda", "")
					.getNilai();
			if (tampilkanLampiranLainDiAgenda != null && tampilkanLampiranLainDiAgenda.trim().length() > 0) {
				String[] split = tampilkanLampiranLainDiAgenda.split(",");
				for (String s : split) {
					addJenisLampiranIfAbsent(result, s);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:2969");
		}
		return result;
	}

	private static void addJenisLampiranIfAbsent(java.util.List<String> result, String jenis) {
		if (result == null || jenis == null || jenis.trim().length() == 0) {
			return;
		}
		String clean = jenis.trim();
		if (!result.contains(clean)) {
			result.add(clean);
		}
	}

	public static String getLabelLampiranDashboardELearning(String jenis) {
		if (jenis == null || jenis.trim().length() == 0) {
			return "Lampiran";
		}
		if (jenis.equals(LampiranLain.SILABUS)) {
			return "RPS";
		}
		if (jenis.equals(LampiranLain.SAP)) {
			return "SAP";
		}
		return jenis;
	}

	private static String formatLampiranInfo(LampiranLain lam) {
		if (lam == null) {
			return "";
		}
		String nama = lam.getNama();
		if (nama == null) {
			nama = "";
		}
		try {
			String url = lam.createLinkUri();
			if (url != null && url.trim().length() > 0) {
				nama += "<->" + url;
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:3011");
		}
		return nama;
	}

	private static Long getLongSqlValue(Object[] row, int index) {
		if (row == null || row.length <= index || row[index] == null) {
			return null;
		}
		try {
			if (row[index] instanceof Number) {
				return Long.valueOf(((Number) row[index]).longValue());
			}
			String value = row[index].toString();
			return value == null || value.trim().length() == 0 ? null : Long.valueOf(value.trim());
		} catch (Exception e) {
			return null;
		}
	}

	private static String getStringSqlValue(Object[] row, int index) {
		if (row == null || row.length <= index || row[index] == null) {
			return "";
		}
		try {
			return row[index].toString() == null ? "" : row[index].toString().trim();
		} catch (Exception e) {
			return "";
		}
	}

	private static String buildLampiranLinkFromSql(Long lampiranId, Long ref, String jenis, String nama) {
		if (lampiranId == null) {
			return null;
		}
		try {
			LampiranLain lampiran = new LampiranLain();
			lampiran.setId(lampiranId);
			if (ref != null) {
				lampiran.setRef(ref);
			}
			lampiran.setJenis(jenis);
			lampiran.setNama(nama == null || nama.trim().length() == 0 ? jenis : nama);
			String url = lampiran.createLinkUri();
			return url == null || url.trim().length() == 0 ? null : url;
		} catch (Exception e) {
			return null;
		}
	}

	private static String loadLampiranLinkById(Long lampiranId) {
		if (lampiranId == null) {
			return null;
		}
		Session session = null;
		try {
			session = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
			LampiranLain lampiran = (LampiranLain) session.get(LampiranLain.class, lampiranId);
			if (lampiran != null) {
				String link = lampiran.createLinkUri();
				return link == null || link.trim().length() == 0 ? null : link;
			}
		} catch (Exception e) {
			return null;
		} finally {
			if (session != null) {
				try {
					session.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:3079");
				}
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:3083");
				}
				try {
					ais.common.ElearningSessionUtil.closeQuietly(session);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:3087");
				}
			}
		}
		return null;
	}

	private static void openLampiranDashboardInfo(LampiranDashboardInfo info) throws Exception {
		if (info == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, data lampiran tidak ditemukan. Langkah yang dapat dilakukan: (1) pastikan lampiran memang telah diunggah; (2) muat ulang halaman lalu coba kembali; (3) apabila lampiran tetap tidak ditemukan, mohon menghubungi administrator sistem.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		String link = info.link;
		if ((link == null || link.trim().length() == 0) && info.lampiranId != null) {
			link = loadLampiranLinkById(info.lampiranId);
		}
		if ((link == null || link.trim().length() == 0) && info.lampiranId != null) {
			link = buildLampiranLinkFromSql(info.lampiranId, info.perkuliahanId, info.jenisAsli, info.nama);
		}
		if (link != null && link.trim().length() > 0) {
			if (Common.isMobile()) {
				ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
			} else {
				String safeUrl = link.replace("'", "\\'");
				Clients.evalJavaScript("popupCenter({url: '" + safeUrl + "', title: 'Lampiran', w: 1200, h: 650});");
			}
		} else {
			MyMessageboxConfig.show(
					"Mohon maaf, tautan unduhan dokumen belum dapat dibuat saat ini. Langkah yang dapat dilakukan: (1) buka dokumen melalui menu perkuliahan terkait; (2) muat ulang halaman lalu coba kembali; (3) apabila masih belum berhasil, mohon menghubungi administrator sistem.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		}
	}

	private static class AttendanceDashboardInfo {
		private String role;
		private String kode;
		private String label;
		private String pesertaId;
		private String nama;
		private String raw;
		private Pertemuan pertemuan;

		private AttendanceDashboardInfo(String role, String kode, String pesertaId, String nama, String raw,
				Pertemuan pertemuan) {
			this.role = role == null ? "" : role;
			this.kode = normalizeStatusKode(kode);
			this.label = getStatusKehadiranLabel(this.kode);
			this.pesertaId = pesertaId == null ? "" : pesertaId;
			this.nama = nama == null ? "" : nama;
			this.raw = raw == null ? "" : raw;
			this.pertemuan = pertemuan;
		}
	}

	private static String normalizeStatusKode(String kode) {
		if (kode == null || kode.trim().length() == 0) {
			return "-";
		}
		String clean = kode.trim().toUpperCase();
		if (clean.length() > 1 && !"-".equals(clean)) {
			clean = clean.substring(0, 1);
		}
		if (!("M".equals(clean) || "A".equals(clean) || "S".equals(clean) || "I".equals(clean) || "-".equals(clean))) {
			return "-";
		}
		return clean;
	}

	private static String getStatusKehadiranLabel(String kode) {
		String clean = normalizeStatusKode(kode);
		if ("M".equals(clean)) {
			return "Hadir";
		}
		if ("A".equals(clean)) {
			return "Mangkir";
		}
		if ("S".equals(clean)) {
			return "Sakit";
		}
		if ("I".equals(clean)) {
			return "Izin";
		}
		if ("-".equals(clean)) {
			return "Belum Ada Keterangan";
		}
		return clean;
	}

	private static String safeAttendanceText(String value) {
		return value == null ? "" : value.trim();
	}

	private static String extractAttendancePesertaId(String[] split) {
		return split != null && split.length > 0 ? safeAttendanceText(split[0]) : "";
	}

	private static boolean isValidStatusKode(String kode) {
		String clean = kode == null ? "" : kode.trim().toUpperCase();
		return "M".equals(clean) || "A".equals(clean) || "S".equals(clean) || "I".equals(clean) || "-".equals(clean);
	}

	private static String extractAttendanceKode(String[] split) {
		if (split == null || split.length == 0) {
			return "-";
		}

		/*
		 * Format absensi lama pada Pertemuan.hitungStatus(...): split[0] = id peserta
		 * split[2] = kode status (M/A/S/I/-) elemen terakhir berisi role
		 * (mahasiswa/siswa/dosen/guru)
		 *
		 * Pada V43 sempat diambil dari split[split.length - 2] agar nama yang
		 * mengandung koma tetap terbaca. Namun itu menyebabkan kode status berpindah
		 * menjadi teks lain pada format existing, sehingga M/A/S/I tampil 0 dan hanya
		 * total/unknown yang bertambah. Karena itu prioritas utama harus kembali ke
		 * split[2] seperti logic asli.
		 */
		if (split.length >= 3) {
			String kode = safeAttendanceText(split[2]);
			if (kode.length() > 0) {
				return kode;
			}
		}

		for (int i = 1; i < split.length; i++) {
			String kandidat = safeAttendanceText(split[i]);
			if (isValidStatusKode(kandidat)) {
				return kandidat;
			}
		}
		return "-";
	}

	private static String extractAttendanceNama(String[] split) {
		if (split == null || split.length < 2) {
			return "";
		}
		String nama = safeAttendanceText(split[1]);
		if (isValidStatusKode(nama)) {
			return "";
		}
		return nama;
	}

	private static String resolveAttendanceParticipantName(String role, String pesertaId, String namaDariAbsensi) {
		String nama = safeAttendanceText(namaDariAbsensi);
		if (nama.length() > 0 && !"-".equals(nama)) {
			return nama;
		}
		Long id = null;
		try {
			String idText = safeAttendanceText(pesertaId);
			if (idText.length() > 0) {
				id = Long.valueOf(idText);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:3244");
		}
		if (id == null) {
			return nama;
		}
		try {
			String roleLower = role == null ? "" : role.toLowerCase();
			Object obj = null;
			if (roleLower.indexOf("mahasiswa") >= 0) {
				obj = ConstantValues.ambil(Mahasiswa.class.getName(), id);
			} else if (roleLower.indexOf("dosen") >= 0) {
				obj = ConstantValues.ambil(Dosen.class.getName(), id);
			} else if (roleLower.indexOf("siswa") >= 0) {
				obj = ConstantValues.ambil(Siswa.class.getName(), id);
			} else if (roleLower.indexOf("guru") >= 0) {
				obj = ConstantValues.ambil(Guru.class.getName(), id);
			}
			String namaObj = getStringPropertyByReflection(obj, "getNama");
			String kodeObj = getStringPropertyByReflection(obj, "getNim");
			if (kodeObj.length() == 0) {
				kodeObj = getStringPropertyByReflection(obj, "getNis");
			}
			if (kodeObj.length() == 0) {
				kodeObj = getStringPropertyByReflection(obj, "getCode");
			}
			if (namaObj.length() > 0) {
				return kodeObj.length() > 0 ? kodeObj + " - " + namaObj : namaObj;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:3272");
		}
		return nama;
	}

	private static String getStringPropertyByReflection(Object obj, String methodName) {
		if (obj == null || methodName == null) {
			return "";
		}
		try {
			Object val = obj.getClass().getMethod(methodName, new Class[0]).invoke(obj, new Object[0]);
			return val == null ? "" : val.toString().trim();
		} catch (Exception e) {
			return "";
		}
	}

	/** Warna konsisten per status kehadiran (chip card & badge popup). */
	private static String getStatusKehadiranWarna(String kode) {
		String clean = normalizeStatusKode(kode);
		if ("M".equals(clean)) {
			return "#16a34a";
		}
		if ("A".equals(clean)) {
			return "#dc2626";
		}
		if ("S".equals(clean)) {
			return "#d97706";
		}
		if ("I".equals(clean)) {
			return "#2563eb";
		}
		return "#64748b";
	}

	/**
	 * Mengambil entity peserta (Mahasiswa/Dosen/Siswa/Guru) dari role + id absensi.
	 * Dipakai popup detail kehadiran untuk menampilkan foto dan identitas lengkap.
	 */
	private static ais.database.model.GeneralValueObject resolveAttendanceEntity(String role, String pesertaId) {
		Long id = null;
		try {
			String idText = safeAttendanceText(pesertaId);
			if (idText.length() > 0) {
				id = Long.valueOf(idText);
			}
		} catch (Exception e) {
			return null;
		}
		if (id == null) {
			return null;
		}
		try {
			String roleLower = role == null ? "" : role.toLowerCase();
			Object obj = null;
			if (roleLower.indexOf("mahasiswa") >= 0) {
				obj = ConstantValues.ambil(Mahasiswa.class.getName(), id);
			} else if (roleLower.indexOf("dosen") >= 0) {
				obj = ConstantValues.ambil(Dosen.class.getName(), id);
			} else if (roleLower.indexOf("siswa") >= 0) {
				obj = ConstantValues.ambil(Siswa.class.getName(), id);
			} else if (roleLower.indexOf("guru") >= 0) {
				obj = ConstantValues.ambil(Guru.class.getName(), id);
			}
			if (obj instanceof ais.database.model.GeneralValueObject) {
				return (ais.database.model.GeneralValueObject) obj;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:3339");
		}
		return null;
	}

	/**
	 * Label identitas sesuai role: NIM (mahasiswa), NIDN (dosen), NIS/Nomor Induk
	 * (siswa), NUPTK (guru). Fallback ke id mentah absensi.
	 */
	private static String buildIdentitasKehadiran(ais.database.model.GeneralValueObject entity, String role,
			String fallbackId) {
		String roleLower = role == null ? "" : role.toLowerCase();
		String nilai = "";
		String jenis = "ID";
		if (entity != null) {
			if (roleLower.indexOf("mahasiswa") >= 0) {
				jenis = "NIM";
				nilai = getStringPropertyByReflection(entity, "getNim");
			} else if (roleLower.indexOf("dosen") >= 0) {
				jenis = "NIDN";
				nilai = getStringPropertyByReflection(entity, "getNidn");
			} else if (roleLower.indexOf("siswa") >= 0) {
				jenis = "NIS";
				nilai = getStringPropertyByReflection(entity, "getNomorInduk");
				if (nilai.length() == 0) {
					nilai = getStringPropertyByReflection(entity, "getNomorIndukNasional");
				}
			} else if (roleLower.indexOf("guru") >= 0) {
				jenis = "NUPTK";
				nilai = getStringPropertyByReflection(entity, "getNuptk");
			}
		}
		if (nilai.length() == 0) {
			nilai = safeAttendanceText(fallbackId);
			if (nilai.length() == 0) {
				nilai = "-";
			}
			return jenis.equals("ID") ? ("ID: " + nilai) : (jenis + " / ID: " + nilai);
		}
		return jenis + ": " + nilai;
	}

	private static String[] getStatusKehadiranOrder() {
		return new String[] { "M", "A", "S", "I", "-" };
	}

	private static java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> createStatusCounterMap() {
		java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> map = new java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger>();
		String[] codes = getStatusKehadiranOrder();
		for (int i = 0; i < codes.length; i++) {
			map.put(codes[i], new java.util.concurrent.atomic.AtomicInteger(0));
		}
		return map;
	}

	private static java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentLinkedQueue<Object[]>> createStatusQueueMap() {
		java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentLinkedQueue<Object[]>> map = new java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentLinkedQueue<Object[]>>();
		String[] codes = getStatusKehadiranOrder();
		for (int i = 0; i < codes.length; i++) {
			map.put(codes[i], new java.util.concurrent.ConcurrentLinkedQueue<Object[]>());
		}
		return map;
	}

	private static void akumulasiKehadiranPertemuan(Pertemuan pertemuan, String role,
			java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> counterByKode,
			java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentLinkedQueue<Object[]>> dataByKode) {
		if (pertemuan == null || role == null || counterByKode == null || dataByKode == null) {
			return;
		}
		try {
			String absensi = pertemuan.getAbsensi();
			if (absensi == null || absensi.trim().length() == 0) {
				return;
			}
			String[] nilais = absensi.split(";");
			String roleLower = role.toLowerCase();
			for (int i = 0; i < nilais.length; i++) {
				String nn = nilais[i];
				if (nn == null || !nn.toLowerCase().endsWith(roleLower)) {
					continue;
				}
				String[] split = nn.split(",");
				String pesertaId = extractAttendancePesertaId(split);
				String nama = extractAttendanceNama(split);
				String kode = extractAttendanceKode(split);
				String cleanKode = normalizeStatusKode(kode);
				nama = resolveAttendanceParticipantName(role, pesertaId, nama);
				java.util.concurrent.atomic.AtomicInteger counter = counterByKode.get(cleanKode);
				if (counter == null) {
					counter = new java.util.concurrent.atomic.AtomicInteger(0);
					java.util.concurrent.atomic.AtomicInteger existing = counterByKode.putIfAbsent(cleanKode, counter);
					if (existing != null) {
						counter = existing;
					}
				}
				counter.incrementAndGet();
				java.util.concurrent.ConcurrentLinkedQueue<Object[]> queue = dataByKode.get(cleanKode);
				if (queue == null) {
					queue = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();
					java.util.concurrent.ConcurrentLinkedQueue<Object[]> existingQueue = dataByKode
							.putIfAbsent(cleanKode, queue);
					if (existingQueue != null) {
						queue = existingQueue;
					}
				}
				AttendanceDashboardInfo info = new AttendanceDashboardInfo(role, cleanKode, pesertaId, nama, nn,
						pertemuan);
				queue.add(new Object[] { info, pertemuan, pertemuan.getTanggal() });
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:3450");
		}
	}

	private static class LampiranDashboardInfo {
		private Long perkuliahanId;
		private Long lampiranId;
		private String jenis;
		private String jenisAsli;
		private String nama;
		private String link;
		private Pertemuan pertemuan;

		private LampiranDashboardInfo(Long perkuliahanId, String jenis, LampiranLain lampiran, Pertemuan pertemuan) {
			this.perkuliahanId = perkuliahanId;
			this.jenisAsli = jenis == null ? "Lampiran" : jenis;
			this.jenis = getLabelLampiranDashboardELearning(this.jenisAsli);
			this.pertemuan = pertemuan;
			this.lampiranId = lampiran == null ? null : lampiran.getId();
			parseFormattedInfo(formatLampiranInfo(lampiran));
		}

		private LampiranDashboardInfo(Long perkuliahanId, String jenis, String formattedInfo, Pertemuan pertemuan) {
			this.perkuliahanId = perkuliahanId;
			this.jenisAsli = jenis == null ? "Lampiran" : jenis;
			this.jenis = getLabelLampiranDashboardELearning(this.jenisAsli);
			this.pertemuan = pertemuan;
			this.lampiranId = (formattedInfo == null || formattedInfo.trim().length() == 0) ? null : Long.valueOf(-1L);
			parseFormattedInfo(formattedInfo);
		}

		private LampiranDashboardInfo(Long perkuliahanId, Long lampiranId, String jenisAsli, String labelJenis,
				String nama, String link, Pertemuan pertemuan) {
			this.perkuliahanId = perkuliahanId;
			this.lampiranId = lampiranId;
			this.jenisAsli = jenisAsli == null || jenisAsli.trim().length() == 0 ? "Lampiran" : jenisAsli.trim();
			this.jenis = labelJenis == null || labelJenis.trim().length() == 0
					? getLabelLampiranDashboardELearning(this.jenisAsli)
					: labelJenis.trim();
			this.nama = nama == null || nama.trim().length() == 0 ? this.jenis : nama.trim();
			this.link = link;
			this.pertemuan = pertemuan;
		}

		private void parseFormattedInfo(String formattedInfo) {
			if (formattedInfo == null || formattedInfo.trim().length() == 0) {
				this.nama = "";
				this.link = null;
			} else {
				String[] parts = formattedInfo.split("<->", 2);
				this.nama = parts.length > 0 ? parts[0] : formattedInfo;
				this.link = parts.length > 1 ? parts[1] : null;
			}
		}

		private boolean adaLampiran() {
			return lampiranId != null;
		}

		private String getDisplayName() {
			if (adaLampiran() && nama != null && nama.trim().length() > 0) {
				return nama;
			}
			return "Belum tersedia";
		}
	}

	private static int sumStatusCounterMap(
			java.util.Map<String, java.util.concurrent.atomic.AtomicInteger> counterByKode) {
		int total = 0;
		if (counterByKode == null) {
			return 0;
		}
		String[] codes = getStatusKehadiranOrder();
		for (int i = 0; i < codes.length; i++) {
			java.util.concurrent.atomic.AtomicInteger counter = counterByKode.get(codes[i]);
			total += counter == null ? 0 : counter.get();
		}
		return total;
	}

	private static Component generateAttendanceCardComponent(final String title, String colorBorder,
			String colorBgLight, final java.util.Map<String, java.util.concurrent.atomic.AtomicInteger> counterByKode,
			final java.util.Map<String, java.util.concurrent.ConcurrentLinkedQueue<Object[]>> dataByKode,
			final Tbmuser currentUser) {

		int total = sumStatusCounterMap(counterByKode);
		if (total <= 0) {
			org.zkoss.zul.Div hidden = new org.zkoss.zul.Div();
			hidden.setVisible(false);
			return hidden;
		}

		org.zkoss.zul.Div card = new org.zkoss.zul.Div();
		card.setStyle(
				"width:100%; min-width:0; background:#fff; border-radius:16px; padding:14px; border:1px solid #e5e7eb; border-left:6px solid "
						+ colorBorder
						+ "; box-shadow:0 12px 24px rgba(15,23,42,.08); position:relative; box-sizing:border-box;");

		org.zkoss.zul.Div header = new org.zkoss.zul.Div();
		header.setStyle(
				"display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #eee; padding-bottom:8px; margin-bottom:8px;");
		Label titleLbl = new Label(title);
		titleLbl.setStyle("margin:0; font-size:12px; color:#555; text-transform:uppercase; font-weight:bold;");
		final java.util.List<Object[]> totalDataList = new ArrayList<Object[]>();
		if (dataByKode != null) {
			String[] allCodes = getStatusKehadiranOrder();
			for (int i = 0; i < allCodes.length; i++) {
				java.util.concurrent.ConcurrentLinkedQueue<Object[]> q = dataByKode.get(allCodes[i]);
				if (q != null && !q.isEmpty()) {
					totalDataList.addAll(q);
				}
			}
		}
		A countLbl = new A(Common.numberFormat.get().format(total));
		countLbl.setTooltiptext("Klik untuk melihat nama dan rincian seluruh data " + title);
		countLbl.setStyle("font-size:10px; font-weight:bold; color:" + colorBorder + "; background:" + colorBgLight
				+ "; padding:2px 6px; border-radius:4px; text-decoration:none; cursor:pointer;");
		countLbl.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				showDetailPopup(title + " - Semua Status (" + totalDataList.size() + ")", totalDataList, currentUser,
						false);
			}
		});
		header.appendChild(titleLbl);
		header.appendChild(countLbl);
		card.appendChild(header);

		Label info = new Label(
				"Klik angka total atau masing-masing status untuk melihat Nama peserta/pengajar beserta rincian pertemuan. M = Hadir, A = Mangkir/Tidak Ada Alasan, S = Sakit, I = Izin, - = Belum Ada Keterangan/Belum Absen.");
		info.setStyle("font-size:10px; color:#64748b; line-height:1.45;");
		card.appendChild(info);

		org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
		wrap.setStyle("display:flex; flex-wrap:wrap; gap:8px; margin-top:10px;");
		card.appendChild(wrap);
		String[] codes = getStatusKehadiranOrder();
		for (int i = 0; i < codes.length; i++) {
			final String kode = codes[i];
			java.util.concurrent.atomic.AtomicInteger counter = counterByKode == null ? null : counterByKode.get(kode);
			final int jumlah = counter == null ? 0 : counter.get();
			final java.util.concurrent.ConcurrentLinkedQueue<Object[]> queue = dataByKode == null ? null
					: dataByKode.get(kode);
			A a = new A(kode + " - " + getStatusKehadiranLabel(kode) + " (" + Common.numberFormat.get().format(jumlah)
					+ ")");
			/*
			 * Chip berkode warna per status (hijau=hadir, merah=mangkir, dst.) agar status
			 * terbaca sekilas tanpa membaca teksnya.
			 */
			String warnaChip = getStatusKehadiranWarna(kode);
			a.setStyle("padding:8px 10px; border-radius:999px; background:#ffffff; color:" + warnaChip
					+ "; border:1.5px solid " + warnaChip
					+ "; font-size:10px; font-weight:bold; text-decoration:none; cursor:pointer;");
			a.setTooltiptext("Klik untuk melihat daftar nama, foto, dan rincian pertemuan status "
					+ getStatusKehadiranLabel(kode));
			a.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					showDetailPopup(title + " - " + kode + " / " + getStatusKehadiranLabel(kode) + " (" + jumlah + ")",
							queue == null ? new ArrayList<Object[]>() : new ArrayList<Object[]>(queue), currentUser,
							false);
				}
			});
			wrap.appendChild(a);
		}
		return card;
	}

	private static Component generateDokumenSummaryCardComponent(final String title, String colorBorder,
			String colorBgLight, final java.util.Map<String, java.util.concurrent.atomic.AtomicInteger> counterByJenis,
			final java.util.Map<String, java.util.concurrent.ConcurrentLinkedQueue<Object[]>> dataByJenis,
			final Tbmuser currentUser) {

		org.zkoss.zul.Div card = new org.zkoss.zul.Div();
		card.setStyle(
				"width:100%; min-width:0; background:#fff; border-radius:16px; padding:14px; border:1px solid #e5e7eb; border-left:6px solid "
						+ colorBorder
						+ "; box-shadow:0 12px 24px rgba(15,23,42,.08); position:relative; box-sizing:border-box;");

		org.zkoss.zul.Div header = new org.zkoss.zul.Div();
		header.setStyle(
				"display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #eee; padding-bottom:8px; margin-bottom:8px;");
		Label titleLbl = new Label(title);
		titleLbl.setStyle("margin:0; font-size:12px; color:#555; text-transform:uppercase; font-weight:bold;");
		int totalUpload = 0;
		if (counterByJenis != null) {
			for (String jenis : counterByJenis.keySet()) {
				java.util.concurrent.atomic.AtomicInteger counter = counterByJenis.get(jenis);
				totalUpload += counter == null ? 0 : counter.get();
			}
		}
		Label countLbl = new Label(Common.numberFormat.get().format(totalUpload));
		countLbl.setStyle("font-size:10px; font-weight:bold; color:" + colorBorder + "; background:" + colorBgLight
				+ "; padding:2px 6px; border-radius:4px;");
		header.appendChild(titleLbl);
		header.appendChild(countLbl);
		card.appendChild(header);

		if (totalUpload <= 0) {
			org.zkoss.zul.Div hidden = new org.zkoss.zul.Div();
			hidden.setVisible(false);
			return hidden;
		}

		Label info = new Label(
				ais.common.Common.getBahasaConfig("Jumlah dokumen yang sudah diupload per jenis lampiran. Jenis dengan jumlah 0 disembunyikan agar tampilan lebih ringkas."));
		info.setStyle("font-size:10px; color:#64748b; line-height:1.45;");
		card.appendChild(info);

		org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
		wrap.setStyle("display:flex; flex-wrap:wrap; gap:8px; margin-top:10px;");
		card.appendChild(wrap);
		if (counterByJenis != null) {
			java.util.TreeSet<String> sortedJenis = new java.util.TreeSet<String>(counterByJenis.keySet());
			for (final String jenis : sortedJenis) {
				java.util.concurrent.atomic.AtomicInteger counter = counterByJenis.get(jenis);
				final int jumlah = counter == null ? 0 : counter.get();
				if (jumlah <= 0) {
					continue;
				}
				final java.util.concurrent.ConcurrentLinkedQueue<Object[]> queue = dataByJenis == null ? null
						: dataByJenis.get(jenis);
				A a = new A(jenis + " (" + Common.numberFormat.get().format(jumlah) + ")");
				a.setStyle(
						"padding:8px 10px; border-radius:999px; background:#eff6ff; color:#1d4ed8; border:1px solid #bfdbfe; font-size:10px; font-weight:bold; text-decoration:none;");
				a.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						showDetailPopup("Dokumen - " + jenis + " (" + jumlah + ")",
								queue == null ? new ArrayList<Object[]>() : new ArrayList<Object[]>(queue), currentUser,
								false);
					}
				});
				wrap.appendChild(a);
			}
		}
		return card;
	}

	/**
	 * Helper untuk membuat Komponen Dasbor (Card) secara Native menggunakan ZK.
	 * Memiliki dukungan event listener agar angka bisa diklik untuk membuka rincian
	 * (Popup).
	 */
	private static Component generateCardComponent(final String strInfoPrefix, final String title, String colorBorder,
			String colorBgLight, int target, int done, final String lblSelesai, final String lblBelum,
			final List<Object[]> targetList, final List<Object[]> doneList, final List<Object[]> belumList,
			final Tbmuser currentUser, final boolean hanyatugas) {

		if (target <= 0) {
			org.zkoss.zul.Div hidden = new org.zkoss.zul.Div();
			hidden.setVisible(false);
			return hidden;
		}

		int notDone = Math.max(0, target - done);
		double pct = target == 0 ? 0 : ((done * 100.0) / target);
		String pctStr = Common.numberFormat.get().format(pct) + "%";
		String stringInfo = Common.getBahasaConfig(strInfoPrefix + ": ") + Common.numberFormat.get().format(done) + "/"
				+ Common.numberFormat.get().format(target) + " (" + pctStr + ")";

		org.zkoss.zul.Div card = new org.zkoss.zul.Div();
		card.setStyle(
				"width:100%; min-width:0; background:#fff; border-radius:16px; padding:14px; border:1px solid #e5e7eb; border-left:6px solid "
						+ colorBorder
						+ "; box-shadow:0 12px 24px rgba(15,23,42,.08); position:relative; box-sizing:border-box;");

		org.zkoss.zul.Div header = new org.zkoss.zul.Div();
		header.setStyle(
				"display:flex; justify-content:space-between; align-items:center; border-bottom: 1px solid #eee; padding-bottom: 8px; margin-bottom: 8px;");
		Label titleLbl = new Label(title);
		titleLbl.setStyle("margin:0; font-size: 12px; color:#555; text-transform:uppercase; font-weight:bold;");
		Label pctLbl = new Label(pctStr);
		pctLbl.setStyle("font-size:10px; font-weight:bold; color:" + colorBorder + "; background:" + colorBgLight
				+ "; padding:2px 6px; border-radius:4px;");
		header.appendChild(titleLbl);
		header.appendChild(pctLbl);
		card.appendChild(header);

		org.zkoss.zul.Div infoDiv = new org.zkoss.zul.Div();
		infoDiv.setStyle("text-align:center; margin-bottom:12px;");
		Label infoLbl = new Label(stringInfo);
		infoLbl.setStyle("font-size:11px; color:#444; font-weight:bold;");
		infoDiv.appendChild(infoLbl);
		card.appendChild(infoDiv);

		org.zkoss.zul.Div bottom = new org.zkoss.zul.Div();
		bottom.setStyle("display:flex; justify-content:space-around; text-align:center;");

		// Kotak Selesai
		org.zkoss.zul.Div dSelesai = new org.zkoss.zul.Div();
		A aSelesai = new A(Common.numberFormat.get().format(done));
		aSelesai.setStyle("color:#198754; font-size:16px; font-weight:bold; text-decoration:none;");
		Label lSelesai = new Label(lblSelesai);
		lSelesai.setStyle("font-size:8px; color:#888; text-transform:uppercase; display:block;");
		dSelesai.appendChild(aSelesai);
		dSelesai.appendChild(lSelesai);
		aSelesai.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				showDetailPopup(title + " - " + lblSelesai, doneList, currentUser, hanyatugas);
			}
		});

		// Kotak Belum
		org.zkoss.zul.Div dBelum = new org.zkoss.zul.Div();
		dBelum.setStyle("border-left:1px solid #eee; border-right:1px solid #eee; padding: 0 8px;");
		A aBelum = new A(Common.numberFormat.get().format(notDone));
		aBelum.setStyle("color:#dc3545; font-size:16px; font-weight:bold; text-decoration:none;");
		Label lBelum = new Label(lblBelum);
		lBelum.setStyle("font-size:8px; color:#888; text-transform:uppercase; display:block;");
		dBelum.appendChild(aBelum);
		dBelum.appendChild(lBelum);
		aBelum.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				showDetailPopup(title + " - " + lblBelum, belumList, currentUser, hanyatugas);
			}
		});

		// Kotak Target
		org.zkoss.zul.Div dTarget = new org.zkoss.zul.Div();
		A aTarget = new A(Common.numberFormat.get().format(target));
		aTarget.setStyle("color:#6c757d; font-size:16px; font-weight:bold; text-decoration:none;");
		Label lTarget = new Label(ais.common.Common.getBahasaConfig("Target"));
		lTarget.setStyle("font-size:8px; color:#888; text-transform:uppercase; display:block;");
		dTarget.appendChild(aTarget);
		dTarget.appendChild(lTarget);
		aTarget.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				showDetailPopup("Keseluruhan " + title + " (Target)", targetList, currentUser, hanyatugas);
			}
		});

		bottom.appendChild(dSelesai);
		bottom.appendChild(dBelum);
		bottom.appendChild(dTarget);
		card.appendChild(bottom);

		return card;
	}

	/**
	 * Membangun Window Popup yang berisi daftar data item dari Dasbor saat diklik.
	 * Dilengkapi dengan ListModel dan RowRenderer agar rendering dilakukan per
	 * halaman (Lazy Load).
	 */
	private static void showDetailPopup(String windowTitle, List<Object[]> dataList, final Tbmuser tbmuser,
			final boolean hanyatugas) {
		final MyWindow window = new MyWindow("Rincian: " + windowTitle, "normal", true);
		window.setWidth(Common.isMobile() ? "95%" : "80%");
		// Turunkan sedikit dari 99% agar window tidak terlalu mentok dengan tepi layar
		// browser
		window.setHeight(Common.isMobile() ? "95%" : "90%");
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%"); // Pastikan borderlayout memenuhi window
		borderlayout.setParent(window);

		Center center = new Center();
		center.setBorder("none");
		center.setAutoscroll(true); // INI KUNCI UTAMA untuk memunculkan scrollbar ZK
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.Div wrapper = new org.zkoss.zul.Div();
		// Hapus 'height: 100%' dan 'overflow: auto'. Biarkan konten memanjang agar
		// Center memunculkan scroll
		wrapper.setStyle("padding: 10px;");
		wrapper.setParent(center);

		if (dataList == null || dataList.isEmpty()) {
			wrapper.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak ada data untuk ditampilkan pada kategori ini.")));
		} else {
			Grid grid = new Grid();
			grid.setSclass("fgrid");
			grid.setOddRowSclass("non-odd");
			grid.setParent(wrapper);
			grid.setMold("paging");
			grid.setPageSize(5);
			grid.setPagingPosition("top");
			grid.getPagingChild().setMold("os");

			// IMPLEMENTASI LAZY LOADING (RENDER PER HALAMAN)
			// Menggunakan ListModelList untuk membungkus data
			org.zkoss.zul.ListModelList listModel = new org.zkoss.zul.ListModelList(dataList);
			grid.setModel(listModel);

			// Menggunakan RowRenderer agar 'renderDetailRow' hanya dipanggil saat barisnya
			// benar-benar ditampilkan di layar
			grid.setRowRenderer(new ais.ui.util.MyRowRenderer() {
				@Override
				public void render(Row row, Object dataa) throws Exception {
					row.setValign("top");
					row.setStyle("border:0px; background:transparent; padding: 10px 0; border-bottom: 1px solid #eee;");
					Object[] data = (Object[]) dataa;
					// Render detail spesifik sesuai baris data pada halaman aktif
					renderDetailRow(row, data, tbmuser, hanyatugas);
				}

			});
		}

		South south = new South();
		south.setParent(borderlayout);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig btnClose = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		btnClose.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		btnClose.setParent(toolbar);

		try {
			window.doModal();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:3873");
		}
	}

	/**
	 * Melakukan render terhadap 1 baris item di dalam popup Dasbor mengikuti logika
	 * murni dari TampilanELearningAction / loadDataMateri. Dilengkapi dengan
	 * pemisahan inheritance antara class Pertemuan dan Tugas.
	 */
	private static void renderDetailRow(Row row, Object[] objects, final Tbmuser tbmuser, final boolean hanyatugas) {
		try {
			final Pertemuan pertemuan = (Pertemuan) objects[1];

			if (objects[0] instanceof AttendanceDashboardInfo) {
				final AttendanceDashboardInfo info = (AttendanceDashboardInfo) objects[0];
				Groupbox vbox = new ais.ui.util.MyGroupboxStyled();
				vbox.setWidth("95%");
				vbox.setStyle(
						"background-color:#ffffff; border:1px solid #e2e8f0; border-radius:12px; padding:14px; margin:6px 0px 12px 10px; box-shadow:0 8px 18px rgba(15,23,42,.06);");
				vbox.setParent(row);

				ais.database.model.GeneralValueObject pesertaEntity = resolveAttendanceEntity(info.role,
						info.pesertaId);
				String warnaStatus = getStatusKehadiranWarna(info.kode);

				/*
				 * Layout kartu peserta: foto di kiri, identitas + status di kanan. flex-wrap
				 * agar di layar HP foto dan teks turun baris dengan rapi.
				 */
				org.zkoss.zul.Div flex = new org.zkoss.zul.Div();
				flex.setStyle("display:flex; flex-wrap:wrap; gap:14px; align-items:flex-start;");
				flex.setParent(vbox);

				/*
				 * Foto peserta memakai helper terpusat ProfileImageUtil (pola yang sama dengan
				 * MahasiswaAction/profil) — klik foto = preview besar.
				 */
				org.zkoss.zul.Div fotoWrap = new org.zkoss.zul.Div();
				fotoWrap.setStyle("flex:0 0 auto; width:72px; min-height:72px; border-radius:12px; overflow:hidden; "
						+ "border:2px solid " + warnaStatus + "; background:#f1f5f9; text-align:center;");
				fotoWrap.setParent(flex);
				boolean fotoTampil = false;
				try {
					if (pesertaEntity != null) {
						A foto = ais.common.ProfileImageUtil.tampilkanGambarKecil(pesertaEntity, "70px", "center");
						foto.setTooltiptext("Klik untuk melihat foto ukuran besar");
						foto.setParent(fotoWrap);
						fotoTampil = true;
					}
				} catch (Exception e) {
					fotoTampil = false;
				}
				if (!fotoTampil) {
					org.zkoss.zul.Image fotoDefault = new org.zkoss.zul.Image("/img/user_default.png");
					fotoDefault.setHeight("70px");
					fotoDefault.setParent(fotoWrap);
				}

				org.zkoss.zul.Div teks = new org.zkoss.zul.Div();
				teks.setStyle("flex:1 1 220px; min-width:0;");
				teks.setParent(flex);

				String namaPeserta = resolveAttendanceParticipantName(info.role, info.pesertaId, info.nama);
				if (pesertaEntity != null) {
					String namaEntity = getStringPropertyByReflection(pesertaEntity, "getNama");
					if (namaEntity.length() > 0) {
						namaPeserta = namaEntity;
					}
				}
				if (namaPeserta == null || namaPeserta.trim().length() == 0) {
					namaPeserta = "Belum terbaca dari data absensi";
				}
				Label namaLbl = new Label(namaPeserta);
				namaLbl.setStyle("font-size:14px; font-weight:900; color:#0f172a; display:block; line-height:1.3;");
				teks.appendChild(namaLbl);

				/* Baris chip: identitas (NIM/NIDN/NIS/NUPTK), role, dan status berwarna. */
				org.zkoss.zul.Div chips = new org.zkoss.zul.Div();
				chips.setStyle("display:flex; flex-wrap:wrap; gap:6px; margin-top:7px;");
				chips.setParent(teks);

				Label identitasLbl = new Label(buildIdentitasKehadiran(pesertaEntity, info.role, info.pesertaId));
				identitasLbl.setStyle(
						"font-size:11px; font-weight:800; color:#0f172a; background:#f8fafc; border:1px solid #e2e8f0; border-radius:999px; padding:4px 10px;");
				chips.appendChild(identitasLbl);

				Label roleLbl = new Label(info.role);
				roleLbl.setStyle(
						"font-size:11px; font-weight:800; color:#334155; background:#ffffff; border:1px solid #e2e8f0; border-radius:999px; padding:4px 10px;");
				chips.appendChild(roleLbl);

				Label statusLbl = new Label(info.kode + " - " + info.label);
				statusLbl.setStyle("font-size:11px; font-weight:900; color:#ffffff; background:" + warnaStatus
						+ "; border:1px solid " + warnaStatus + "; border-radius:999px; padding:4px 10px;");
				chips.appendChild(statusLbl);

				String infoPertemuan = pertemuan == null ? ""
						: ("Pertemuan ke: " + pertemuan.getPertemuanKe() + ", " + pertemuan.info());
				Label detail = new Label(infoPertemuan);
				detail.setStyle("font-size:11px; color:#64748b; display:block; margin-top:8px; line-height:1.5;");
				teks.appendChild(detail);
				return;

			} else if (objects[0] instanceof LampiranDashboardInfo) {
				final LampiranDashboardInfo info = (LampiranDashboardInfo) objects[0];
				Groupbox vbox = new ais.ui.util.MyGroupboxStyled();
				vbox.setWidth("95%");
				vbox.setStyle(
						"background-color:#ffffff; border:1px solid #e2e8f0; border-radius:12px; padding:14px; margin:6px 0px 12px 10px; box-shadow:0 8px 18px rgba(15,23,42,.06);");
				vbox.setParent(row);

				String icon = info.adaLampiran() ? "/img/svg/card-checklist.svg" : "/img/svg/info.svg";
				Toolbarbutton fileButton = new ais.ui.util.MyToolbarbuttonConfig(
						info.jenis + " - " + info.getDisplayName(), icon);
				fileButton.setAttribute("janganDisabled", true);
				fileButton.setTooltiptext("Klik untuk membuka atau download dokumen");
				fileButton.setStyle(
						"font-size:14px; font-weight:800; color:" + (info.adaLampiran() ? "#047857" : "#b45309")
								+ "; text-decoration:none; padding-bottom:8px; display:block; cursor:pointer;");
				vbox.appendChild(fileButton);

				String infoPertemuan = pertemuan == null ? ""
						: ("Perkuliahan ID: " + info.perkuliahanId + ", contoh pertemuan ke: "
								+ pertemuan.getPertemuanKe() + ", " + pertemuan.info());
				Label detail = new Label(infoPertemuan);
				detail.setStyle(
						"font-size:11px; color:#64748b; background:#f8fafc; border:1px solid #e2e8f0; border-radius:999px; padding:5px 10px;");
				vbox.appendChild(detail);

				Label klik = new Label("Klik nama dokumen di atas untuk preview / download.");
				klik.setStyle("font-size:10px; color:#0f766e; display:block; margin-top:8px; font-weight:700;");
				vbox.appendChild(klik);

				fileButton.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						openLampiranDashboardInfo(info);
					}
				});
				return;

			} else if (objects[0] instanceof PertemuanFileContent) {
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
						"background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; margin: 6px 0px 12px 10px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);");
				vbox.setParent(row);

				String icon = pertemuanFileContent.getLokasiFisik() != null ? "/img/svg/desktop-light.svg"
						: MyMenuitem.svgIcon(pertemuanFileContent.getNama(),
								ais.database.model.file.FileFoto.icon(pertemuanFileContent.getNama()));

				Toolbarbutton downloadButton = new ais.ui.util.MyToolbarbuttonConfig(
						n.length() > 150 ? n.substring(0, 150) + "..." : n,
						!pertemuanFileContent.getGoogleBook().isEmpty() ? "/img/Apps-Google-Play-Books-icon.png"
								: icon);
				downloadButton.setTooltiptext("Download \"" + pertemuanFileContent.getNama() + "\"");
				downloadButton.setAttribute("janganDisabled", true);
				downloadButton.setStyle(
						"font-size: 14px; font-weight: 600; color: #1e40af; text-decoration: none; padding-bottom: 8px; display: block;");
				vbox.appendChild(downloadButton);

				Hbox myHbox = new Hbox();
				myHbox.setAlign("center");
				myHbox.setParent(vbox);
				TampilanELearningAction
						.dilihat(pertemuan, "bahan_perkulaiahan_" + pertemuanFileContent.getId(), "Akses", false)
						.setParent(myHbox);

				EventListener eventListener = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (ProfileUtil.chekSyarat(pertemuan.ambilVOPembelajaran(),
								pertemuanFileContent.getSyaratAkses()))
							return;
						pertemuan.masukkanData("bahan_perkulaiahan_" + pertemuanFileContent.getId());
						if (!pertemuanFileContent.getGoogleBook().isEmpty()) {
							if (Common.isMobile())
								ExecutionsCtrl.getCurrent().sendRedirect(pertemuanFileContent.getLink(), "_blank");
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
								if (pertemuanFileContent.bisaPreview()) {
									Common.displayWindow(pertemuanFileContent.merupakanGambar(), link, true, "95%",
											"95%", true, pertemuanFileContent);
								} else {
									if (Common.isMobile())
										ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
									else
										Clients.evalJavaScript(
												"popupCenter({url: '" + link + "', title: 'data', w: 1200, h: 600});");
								}
							} else {
								MyMessageboxConfig.show(
										"Mohon maaf, berkas yang Bapak/Ibu akses tidak ditemukan. Langkah yang dapat dilakukan: (1) pastikan berkas masih tersedia dan belum dihapus; (2) muat ulang halaman lalu coba kembali; (3) apabila berkas tetap tidak ditemukan, mohon menghubungi administrator sistem.", "Peringatan",
										MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							}
						}
					}
				};
				downloadButton.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, eventListener);

				A a = new A("Materi pertemuan ke: " + pertemuan.getPertemuanKe() + ", " + pertemuan.info() + ", "
						+ ((pertemuan.getTanggal() == null ? "-"
								: (SmartDateTimeUtil.getDayString(pertemuan.getTanggal(), pertemuan.getWaktuMulai())
										+ Common.dateFormat6.get().format(pertemuan.getTanggal())) + " "
										+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
												: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai()))));
				a.setStyle(
						"font-size: 11px; color: #475569; background-color: #f1f5f9; padding: 4px 10px; border-radius: 12px; display: inline-block; margin-top: 10px; text-decoration: none; border: 1px solid #cbd5e1;");
				a.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						new ais.action.master.helper.PertemuanHelper(tbmuser == null ? null : tbmuser.getMahasiswa(),
								tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
								.display(pertemuan, new DataLoader() {
									@Override
									public void loadData(Object value) {
									}
								}, 2, pertemuanFileContent);
					}
				});
				vbox.appendChild(a);

			} else if (objects[0] instanceof PertemuanPunyaUjian && !hanyatugas) {
				final PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) objects[0];
				String n = pertemuanPunyaUjian.getNama() == null ? "" : pertemuanPunyaUjian.getNama();

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
				TampilanELearningAction.dilihat(pertemuan, "ujian_" + pertemuanPunyaUjian.getId(), "Akses", false)
						.setParent(myHbox);

				Date tgl = pertemuanPunyaUjian.getMulaiUjian();
				A a = new A("Ujian pertemuan ke: " + pertemuan.getPertemuanKe() + ", " + pertemuan.info() + ", "
						+ (tgl == null ? ""
								: (SmartDateTimeUtil.getDayString(tgl, null) + Common.dateFormat51.get().format(tgl))));
				a.setStyle(
						"font-size: 11px; color: #475569; background-color: #f1f5f9; padding: 4px 10px; border-radius: 12px; display: inline-block; margin-top: 10px; text-decoration: none; border: 1px solid #cbd5e1;");
				vbox.appendChild(a);

				EventListener eventUjian = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						new ais.action.master.helper.PertemuanHelper(tbmuser == null ? null : tbmuser.getMahasiswa(),
								tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
								.display(pertemuan, new DataLoader() {
									@Override
									public void loadData(Object value) {
									}
								}, 6);
					}
				};
				downloadButton.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, eventUjian);
				a.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, eventUjian);

			} else if (objects[0] instanceof AudioPertemuan && !hanyatugas) {
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

				Hbox myHbox = new Hbox();
				myHbox.setAlign("center");
				myHbox.setParent(vbox);
				TampilanELearningAction.dilihat(pertemuan, "audio_" + audioPertemuan.getId(), "Akses", false)
						.setParent(myHbox);

				downloadButton.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (ProfileUtil.chekSyarat(pertemuan.ambilVOPembelajaran(), audioPertemuan.getSyaratAkses()))
							return;
						pertemuan.masukkanData("audio_" + audioPertemuan.getId());
						if (audioPertemuan.getGdrive() != null) {
							audioPertemuan.tampilGDrive(null);
						} else {
							String link = audioPertemuan == null ? null
									: (audioPertemuan.getLink() == null || audioPertemuan.getLink().isEmpty() ? null
											: audioPertemuan.getLink());
							if (audioPertemuan != null
									&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
								link = audioPertemuan.createLinkUri();
							}
							if (audioPertemuan != null && link != null && !link.trim().isEmpty()) {
								if (Common.isMobile())
									ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
								else
									Clients.evalJavaScript(
											"popupCenter({url: '" + link + "', title: 'data', w: 1200, h: 600});");
							} else {
								MyMessageboxConfig.show(
										"Mohon maaf, berkas yang Bapak/Ibu akses tidak ditemukan. Langkah yang dapat dilakukan: (1) pastikan berkas masih tersedia dan belum dihapus; (2) muat ulang halaman lalu coba kembali; (3) apabila berkas tetap tidak ditemukan, mohon menghubungi administrator sistem.", "Peringatan",
										MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							}
						}
					}
				});

				A a = new A("Audio pertemuan ke: " + pertemuan.getPertemuanKe() + ", " + pertemuan.info() + ", "
						+ ((pertemuan.getTanggal() == null ? "-"
								: (SmartDateTimeUtil.getDayString(pertemuan.getTanggal(), pertemuan.getWaktuMulai())
										+ Common.dateFormat6.get().format(pertemuan.getTanggal())) + " "
										+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
												: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai()))));
				a.setStyle(
						"font-size: 11px; color: #475569; background-color: #f1f5f9; padding: 4px 10px; border-radius: 12px; display: inline-block; margin-top: 10px; text-decoration: none; border: 1px solid #cbd5e1;");
				a.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						new ais.action.master.helper.PertemuanHelper(tbmuser == null ? null : tbmuser.getMahasiswa(),
								tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
								.display(pertemuan, new DataLoader() {
									@Override
									public void loadData(Object value) {
									}
								}, 4, audioPertemuan);
					}
				});
				vbox.appendChild(a);

			} else if (objects[0] instanceof VideoPertemuan && !hanyatugas) {
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

				Hbox myHbox = new Hbox();
				myHbox.setAlign("center");
				myHbox.setParent(vbox);
				TampilanELearningAction.dilihat(pertemuan, "video_" + videoPertemuan.getId(), "Akses", false)
						.setParent(myHbox);

				downloadButton.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (ProfileUtil.chekSyarat(pertemuan.ambilVOPembelajaran(), videoPertemuan.getSyaratAkses()))
							return;
						pertemuan.masukkanData("video_" + videoPertemuan.getId());
						if (videoPertemuan.getGdrive() != null) {
							videoPertemuan.tampilGDrive(null);
						} else {
							String link = videoPertemuan == null ? null
									: (videoPertemuan.getLink() == null || videoPertemuan.getLink().isEmpty() ? null
											: videoPertemuan.getLink());
							if (videoPertemuan != null
									&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
								link = videoPertemuan.createLinkUri();
							}
							if (videoPertemuan != null && link != null && !link.trim().isEmpty()) {
								if (videoPertemuan.bisaPreview())
									Common.displayWindow(videoPertemuan.merupakanGambar(), link, true, "95%", "95%",
											true, videoPertemuan);
								else {
									if (Common.isMobile())
										ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
									else
										Clients.evalJavaScript(
												"popupCenter({url: '" + link + "', title: 'data', w: 1200, h: 600});");
								}
							} else {
								MyMessageboxConfig.show(
										"Mohon maaf, berkas yang Bapak/Ibu akses tidak ditemukan. Langkah yang dapat dilakukan: (1) pastikan berkas masih tersedia dan belum dihapus; (2) muat ulang halaman lalu coba kembali; (3) apabila berkas tetap tidak ditemukan, mohon menghubungi administrator sistem.", "Peringatan",
										MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							}
						}
					}
				});

				A a = new A("Video pertemuan ke: " + pertemuan.getPertemuanKe() + ", " + pertemuan.info() + ", "
						+ ((pertemuan.getTanggal() == null ? "-"
								: (SmartDateTimeUtil.getDayString(pertemuan.getTanggal(), pertemuan.getWaktuMulai())
										+ Common.dateFormat6.get().format(pertemuan.getTanggal())) + " "
										+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
												: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai()))));
				a.setStyle(
						"font-size: 11px; color: #475569; background-color: #f1f5f9; padding: 4px 10px; border-radius: 12px; display: inline-block; margin-top: 10px; text-decoration: none; border: 1px solid #cbd5e1;");
				a.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						new ais.action.master.helper.PertemuanHelper(tbmuser == null ? null : tbmuser.getMahasiswa(),
								tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
								.display(pertemuan, new DataLoader() {
									@Override
									public void loadData(Object value) {
									}
								}, 5, videoPertemuan);
					}
				});
				vbox.appendChild(a);

			} else if (objects[0] instanceof Pertemuan && !hanyatugas) {
				// --- KARENA PERTEMUAN MERUPAKAN ANAK (CHILD) DARI TUGAS ---
				// Pengecekan Pertemuan harus dilakukan LEBIH DULU sebelum pengecekan murni
				// Tugas
				final Pertemuan pert = (Pertemuan) objects[0];

				// Memanggil fungsi layout lengkap bawaan DashboardTimelinePertemuan untuk popup
				DashboardTimelinePertemuan.displayRow(row, Common.isMobile(), pert, null, tbmuser);

			} else if (objects[0] instanceof Tugas && hanyatugas) {
				// Ini khusus untuk objek murni TugasKelompok / TugasPertemuan
				final Tugas tugas = (Tugas) objects[0];
				if (tugas.getJudultugas() != null && !tugas.getJudultugas().trim().isEmpty()) {
					String n = tugas.getJudultugas() == null ? "" : tugas.getJudultugas();
					Groupbox vbox = new ais.ui.util.MyGroupboxStyled();
					vbox.setWidth("95%");
					vbox.setStyle(
							"background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; margin: 6px 0px 12px 10px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);");
					vbox.setParent(row);

					String icon = (tugas instanceof ais.database.model.TugasKelompok) ? "/img/svg/user-group.svg"
							: "/img/svg/list-task.svg";
					Toolbarbutton downloadButton = new ais.ui.util.MyToolbarbuttonConfig(
							n.length() > 150 ? n.substring(0, 150) + "..." : n, icon);
					downloadButton.setAttribute("janganDisabled", true);
					downloadButton.setStyle(
							"font-size: 14px; font-weight: 600; color: #1e40af; text-decoration: none; padding-bottom: 8px; display: block;");
					vbox.appendChild(downloadButton);

					EventListener eventListenerTugas = new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tugas instanceof ais.database.model.TugasKelompok) {
								ais.database.model.TugasKelompok tugasKelompok = (ais.database.model.TugasKelompok) tugas;
								new ais.action.master.helper.PertemuanHelper(
										tbmuser == null ? null : tbmuser.getMahasiswa(),
										tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
										.display(pertemuan, new DataLoader() {
											@Override
											public void loadData(Object value) {
											}
										}, 3, null, tugasKelompok, null, null, null);
							} else {
								if (tugas instanceof TugasPertemuan) {
									TugasPertemuan tugasPertemuan = (TugasPertemuan) tugas;
									new ais.action.master.helper.PertemuanHelper(
											tbmuser == null ? null : tbmuser.getMahasiswa(),
											tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
											.display(pertemuan, new DataLoader() {
												@Override
												public void loadData(Object value) {
												}
											}, 3, tugasPertemuan, null, null, null, null);
								} else if (tugas instanceof Pertemuan) {
									Pertemuan pertemuandata = (Pertemuan) tugas;
									new ais.action.master.helper.PertemuanHelper(
											tbmuser == null ? null : tbmuser.getMahasiswa(),
											tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa())
											.display(pertemuandata, new DataLoader() {
												@Override
												public void loadData(Object value) {
												}
											}, 3, null, null, null, null, null);
								}
							}
						}
					};
					downloadButton.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, eventListenerTugas);

					Date tgl = tugas.getMulai() == null ? pertemuan.getTanggal() : tugas.getMulai();
					Hbox myHbox = new Hbox();
					myHbox.setAlign("center");
					myHbox.setParent(vbox);

					if (!(tugas instanceof ais.database.model.TugasKelompok) && tugas.getJudultugas() != null
							&& !tugas.getJudultugas().trim().equals("")) {
						Number tg = tugas.ambilJumlahTugasFileContent();
						MyLabelKecil labelKecil = new MyLabelKecil(
								"Upload : " + Common.numberFormat.get().format(tg.intValue()) + " peserta");
						labelKecil.setStyle(
								"font-size: 10px; font-weight: 700; color: #ffffff; background-color: #3b82f6; padding: 3px 8px; border-radius: 12px; margin-right: 10px; box-shadow: 0 2px 4px rgba(59,130,246,0.3);");
						myHbox.appendChild(labelKecil);
					}
					TampilanELearningAction.dilihat(tugas, "tugas", "Akses", false).setParent(myHbox);

					A a = new A("Tugas pertemuan ke: " + pertemuan.getPertemuanKe() + ", " + pertemuan.info() + ", "
							+ (SmartDateTimeUtil.getDayString(tgl, null) + Common.dateFormat51.get().format(tgl)));
					a.setStyle(
							"font-size: 11px; color: #475569; background-color: #f1f5f9; padding: 4px 10px; border-radius: 12px; display: inline-block; margin-top: 10px; text-decoration: none; border: 1px solid #cbd5e1;");
					a.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, eventListenerTugas);
					vbox.appendChild(a);
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:4444");
		}
	}

	private Combobox initBulanSd() {
		Combobox bulanSd = new Combobox();
		bulanSd.setStyle("font-size:12px;");

		Comboitem comboitem = new MyComboitemConfig();
		comboitem.setLabel("1 hari");
		comboitem.setValue(-1);
		bulanSd.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("3 hari");
		comboitem.setValue(0);
		bulanSd.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("1 minggu");
		comboitem.setValue(1);
		bulanSd.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("3 minggu");
		comboitem.setValue(2);
		bulanSd.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("1 bulan");
		comboitem.setValue(3);
		bulanSd.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("3 bulan");
		comboitem.setValue(4);
		bulanSd.appendChild(comboitem);

		if (tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null
				|| tbmuser.getSiswa() != null || tbmuser.ambilGuru() != null)) {

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("1 semester");
			comboitem.setValue(5);
			bulanSd.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("1 tahun");
			comboitem.setValue(6);
			bulanSd.appendChild(comboitem);
		}

		bulanSd.setReadonly(true);

		try {
			String selectedBln = null;

			if (tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null)) {
				selectedBln = "1";
			} else {
				selectedBln = "-1";
			}

			if (tbmuser != null && tbmuser.getMahasiswa() != null) {
				selectedBln = tbmuser.getMahasiswa().retreive("bulanSd");
			} else if (tbmuser != null && tbmuser.getSiswa() != null) {
				selectedBln = tbmuser.getSiswa().retreive("bulanSd");
			} else if (tbmuser != null) {
				selectedBln = tbmuser.retreive("bulanSd");
			}
			if (selectedBln != null && !selectedBln.trim().isEmpty()) {
				Common.selectComboItem(bulanSd, Integer.parseInt(selectedBln));
			}

			if (bulanSd.getSelectedItem() == null) {
				if (tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null)) {
					Common.selectComboItem(bulanSd, 1);
				} else {
					Common.selectComboItem(bulanSd, -1);
				}
			}
		} catch (Exception e) {
			if (tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null)) {
				Common.selectComboItem(bulanSd, 1);
			} else {
				Common.selectComboItem(bulanSd, -1);
			}
		}

		bulanSd.setCols(4);
		return bulanSd;
	}

	public Criteria initCriteria(boolean order) {
		try {
			return initCriteria(order, null, null, HibernateUtil.currentSession());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:4541");
			return null;
		}
	}

	public static Calendar[] plusMinus(Integer bulanSd) {

		Calendar calendarPlus = ais.ui.util.WaktuUtil.getCalendar();
		Calendar calendarMinus = ais.ui.util.WaktuUtil.getCalendar();

		if (bulanSd.equals(-1)) {
			calendarPlus.set(Calendar.DATE, calendarPlus.get(Calendar.DATE) + 1);
			calendarMinus.set(Calendar.DATE, calendarMinus.get(Calendar.DATE) - 1);
		} else if (bulanSd.equals(0)) {
			calendarPlus.set(Calendar.DATE, calendarPlus.get(Calendar.DATE) + 3);
			calendarMinus.set(Calendar.DATE, calendarMinus.get(Calendar.DATE) - 3);
		} else if (bulanSd.equals(1)) {
			calendarPlus.set(Calendar.WEEK_OF_MONTH, calendarPlus.get(Calendar.WEEK_OF_MONTH) + 1);
			calendarMinus.set(Calendar.WEEK_OF_MONTH, calendarMinus.get(Calendar.WEEK_OF_MONTH) - 1);
		} else if (bulanSd.equals(2)) {
			calendarPlus.set(Calendar.WEEK_OF_MONTH, calendarPlus.get(Calendar.WEEK_OF_MONTH) + 3);
			calendarMinus.set(Calendar.WEEK_OF_MONTH, calendarMinus.get(Calendar.WEEK_OF_MONTH) - 3);
		} else if (bulanSd.equals(3)) {
			calendarPlus.set(Calendar.MONTH, calendarPlus.get(Calendar.MONTH) + 1);
			calendarMinus.set(Calendar.MONTH, calendarMinus.get(Calendar.MONTH) - 1);
		} else if (bulanSd.equals(4)) {
			calendarPlus.set(Calendar.MONTH, calendarPlus.get(Calendar.MONTH) + 3);
			calendarMinus.set(Calendar.MONTH, calendarMinus.get(Calendar.MONTH) - 3);
		} else if (bulanSd.equals(5)) {
			calendarPlus.set(Calendar.MONTH, calendarPlus.get(Calendar.MONTH) + 6);
			calendarMinus.set(Calendar.MONTH, calendarMinus.get(Calendar.MONTH) - 6);
		} else if (bulanSd.equals(6)) {
			calendarPlus.set(Calendar.YEAR, calendarPlus.get(Calendar.YEAR) + 1);
			calendarMinus.set(Calendar.YEAR, calendarMinus.get(Calendar.YEAR) - 1);
		} else {
			calendarPlus.set(Calendar.YEAR, calendarPlus.get(Calendar.YEAR) + 2);
			calendarMinus.set(Calendar.YEAR, calendarMinus.get(Calendar.YEAR) - 2);
		}

		return new Calendar[] { calendarPlus, calendarMinus };
	}

	public Criteria initCriteria(boolean order, Date mulai, Date sampai, Session session) {
		String namaUjian = cariNamaUjian != null && !cariNamaUjian.getValue().trim().isEmpty()
				? cariNamaUjian.getValue().trim()
				: "";
		String mul = cariWaktuMulai == null || cariWaktuMulai.getValue() == null || cariWaktuMulai.isDisabled() ? null
				: Common.timeFormat2.get().format(cariWaktuMulai.getValue());
		String sam = cariWaktuSampai == null || cariWaktuSampai.getValue() == null || cariWaktuSampai.isDisabled()
				? null
				: Common.timeFormat2.get().format(cariWaktuSampai.getValue());
		Integer bulanSd = (Integer) (this.bulanSd == null || this.bulanSd.getSelectedItem() == null
				|| this.bulanSd.getSelectedItem().getValue() == null ? 1 : this.bulanSd.getSelectedItem().getValue());

		StatusPertemuan sp = (StatusPertemuan) (cariStatusPertemuan == null
				|| cariStatusPertemuan.getSelectedItem() == null ? null
						: cariStatusPertemuan.getSelectedItem().getValue());
		Integer ke = (Integer) (cariPertemunaKe == null || cariPertemunaKe.getSelectedItem() == null ? null
				: cariPertemunaKe.getSelectedItem().getValue());

		String hr = (String) (hari == null || hari.getSelectedItem() == null ? null
				: hari.getSelectedItem().getValue());
		String day = hr == null ? null
				: hr.equals(Common.haris[0]) ? "sunday"
						: hr.equals(Common.haris[1]) ? "monday"
								: hr.equals(Common.haris[2]) ? "tuesday"
										: hr.equals(Common.haris[3]) ? "wednesday"
												: hr.equals(Common.haris[4]) ? "thursday"
														: hr.equals(Common.haris[5]) ? "friday" : "saturday";

		boolean tdpVideo = this.tdpVideo != null && this.tdpVideo.isChecked();
		boolean tdpAudio = this.tdpAudio != null && this.tdpAudio.isChecked();
		boolean tdpMateri = this.tdpMateri != null && this.tdpMateri.isChecked();

		boolean jadwalPerkuliahan = this.jadwalPerkuliahan == null || this.jadwalPerkuliahan.isChecked();
		boolean jadwalPelajaran = this.jadwalPelajaran == null || this.jadwalPelajaran.isChecked();
		boolean jadwalKkn = this.jadwalKkn == null || this.jadwalKkn.isChecked();
		boolean jadwalPkl = this.jadwalPkl == null || this.jadwalPkl.isChecked();
		boolean jadwalRevisi = this.jadwalRevisi == null || this.jadwalRevisi.isChecked();
		boolean jadwalKonsultasi = this.jadwalKonsultasi == null || this.jadwalKonsultasi.isChecked();
		boolean jadwalBimbingan = this.jadwalBimbingan == null || this.jadwalBimbingan.isChecked();
		boolean jadwalKonsultasiLain = this.jadwalKonsultasiLain == null || this.jadwalKonsultasiLain.isChecked();
		boolean jadwalKegiatan = this.jadwalKegiatan == null || this.jadwalKegiatan.isChecked();
		boolean tdpUjian = (this.tdpUjian != null && this.tdpUjian.isChecked());
		boolean tdpDiskusi = (this.tdpDiskusi != null && this.tdpDiskusi.isChecked());
		boolean tdpTugas = (this.tdpTugas != null && this.tdpTugas.isChecked());
		boolean tdpCatatan = (this.tdpCatatan != null && this.tdpCatatan.isChecked());
		boolean tdpDosenPengganti = (this.tdpDosenPengganti != null && this.tdpDosenPengganti.isChecked());

		String cariTopik = (this.cariTopik == null ? "" : this.cariTopik.getValue().trim());
		String cariCatatan = (this.cariCatatan == null ? "" : this.cariCatatan.getValue().trim());

		boolean paralel = this.paralel.isChecked();
		boolean pra = this.pra.isChecked();
		boolean remedial = this.remedial.isChecked();
		String cariMk = (this.cariMk == null ? "" : this.cariMk.getValue().trim());
		boolean ekstra = this.ekstra.isChecked();

		String cariKelas = (this.cariKelas == null ? "" : this.cariKelas.getValue().trim());
		String cariRuang = (this.cariRuang == null ? "" : this.cariRuang.getValue().trim());
		String cariDosen = (this.cariDosen == null ? "" : this.cariDosen.getValue().trim());
		String cariMahasiswa = (this.cariMahasiswa == null ? "" : this.cariMahasiswa.getValue().trim());

		Sekolah sk = SekolahUtil.getSekolah();

		return initStaticCriteria(order, mulai, sampai, tbmuser, namaUjian, mul, sam, bulanSd, sp, ke, day, tdpVideo,
				tdpAudio, tdpMateri, jadwalPerkuliahan, jadwalPelajaran, jadwalKkn, jadwalPkl, jadwalRevisi,
				jadwalKonsultasi, jadwalBimbingan, jadwalKonsultasiLain, jadwalKegiatan, tdpUjian, tdpDiskusi, tdpTugas,
				tdpCatatan, tdpDosenPengganti, cariTopik, cariCatatan, paralel, pra, remedial, cariMk, ekstra,
				cariKelas, cariRuang, cariDosen, cariMahasiswa, ujian, sk, session);

	}

	@SuppressWarnings("unchecked")
	public static Criteria initStaticCriteria(boolean order, Date mulai, Date sampai, Tbmuser tbmuser, String namaUjian,
			String mul, String sam, Integer bulanSd, StatusPertemuan sp, Integer ke, String d, boolean tdpVideo,
			boolean tdpAudio, boolean tdpMateri, boolean jadwalPerkuliahan, boolean jadwalPelajaran, boolean jadwalKkn,
			boolean jadwalPkl, boolean jadwalRevisi, boolean jadwalKonsultasi, boolean jadwalBimbingan,
			boolean jadwalKonsultasiLain, boolean jadwalKegiatan, boolean tdpUjian, boolean tdpDiskusi,
			boolean tdpTugas, boolean tdpCatatan, boolean tdpDosenPengganti, String cariTopik, String cariCatatan,
			boolean paralel, boolean pra, boolean remedial, String cariMk, boolean ekstra, String cariKelas,
			String cariRuang, String cariDosen, String cariMahasiswa, Boolean ujian, Sekolah sk, Session session) {

		Fakultas fakultas = tbmuser.ambilFakultas();
		Jurusan jurusan = tbmuser.ambilJurusan();
		String program = tbmuser.ambilProgram() == null ? null : tbmuser.ambilProgram().getNama();

		Yayasan yayasan = tbmuser.ambilYayasan();
		Sekolah sekolah = tbmuser.ambilSekolah();

		try {
			Sekolah sekolahData = SekolahUtil.getSekolah();
			if (sekolahData != null && sekolahData.getId() != null) {
				sekolah = sekolahData;
			}
			Yayasan yayasanData = SekolahUtil.getYayasan();
			if (yayasanData != null && yayasanData.getId() != null) {
				yayasan = yayasanData;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:4680");
		}

		Guru guru = tbmuser.ambilGuru();
		Siswa siswa = tbmuser.getSiswa();

		if (guru != null && guru.getSekolah() != null)
			sekolah = guru.getSekolah();
		if (siswa != null && siswa.getSekolah() != null)
			sekolah = siswa.getSekolah();
		if (sekolah != null && sekolah.getId() != null)
			sk = sekolah;

		boolean[] ptYa = Common.chekPtAtauSekolah();
		boolean modulSekolah = ptYa[1];
		boolean modulPerguruanTinggi = ptYa[0];

		if (!modulPerguruanTinggi)
			modulSekolah = true;
		if (sekolah != null && sekolah.getId() != null) {
			modulPerguruanTinggi = false;
			modulSekolah = true;
		}

		if ((tbmuser != null && tbmuser.getMahasiswa() != null && tbmuser.getMahasiswa().getId() != null)
				|| (tbmuser != null && tbmuser.getDosen() != null && tbmuser.getDosen().getId() != null)) {
			modulPerguruanTinggi = true;
			modulSekolah = false;
		}

		Calendar[] cc = plusMinus(bulanSd);
		Calendar calendarPlus = cc[0];
		Calendar calendarMinus = cc[1];

		List<Long> pertemunaAdaVideos = null;
		if (tdpVideo) {
			Session session2 = null;
			try {
				session2 = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
				pertemunaAdaVideos = session2.createCriteria(VideoPertemuan.class)
						.setProjection(Projections.groupProperty("pertemuan")).list();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:4722");
			} finally {
				if (session2 != null) {
					try {
						session2.clear();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:4727");
					}
					try {
						session2.disconnect();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:4731");
					}
					try {
						ais.common.ElearningSessionUtil.closeQuietly(session2);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:4735");
					}
				}
			}
		}

		List<Long> pertemunaAdaAudio = null;
		if (tdpAudio) {
			Session session2 = null;
			try {
				session2 = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
				pertemunaAdaAudio = session2.createCriteria(AudioPertemuan.class)
						.setProjection(Projections.groupProperty("pertemuan")).list();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:4749");
			} finally {
				if (session2 != null) {
					try {
						session2.clear();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:4754");
					}
					try {
						session2.disconnect();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:4758");
					}
					try {
						ais.common.ElearningSessionUtil.closeQuietly(session2);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:4762");
					}
				}
			}
		}

		List<Long> pertemunaAdaMateris = null;
		if (tdpMateri) {
			Session session2 = null;
			try {
				session2 = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
				pertemunaAdaMateris = session2.createCriteria(PertemuanFileContent.class)
						.setProjection(Projections.groupProperty("pertemuan")).list();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:4776");
			} finally {
				if (session2 != null) {
					try {
						session2.clear();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:4781");
					}
					try {
						session2.disconnect();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:4785");
					}
					try {
						ais.common.ElearningSessionUtil.closeQuietly(session2);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:4789");
					}
				}
			}
		}

		Criteria criteria = session.createCriteria(Pertemuan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(tdpMateri ? (pertemunaAdaMateris == null || pertemunaAdaMateris.isEmpty()
						? Restrictions.sqlRestriction("false")
						: Restrictions.in("id", pertemunaAdaMateris)) : Restrictions.sqlRestriction("true"))

				.add(tdpVideo ? (pertemunaAdaVideos == null || pertemunaAdaVideos.isEmpty()
						? Restrictions.sqlRestriction("false")
						: Restrictions.in("id", pertemunaAdaVideos)) : Restrictions.sqlRestriction("true"))

				.add(tdpAudio ? (pertemunaAdaAudio == null || pertemunaAdaAudio.isEmpty()
						? Restrictions.sqlRestriction("false")
						: Restrictions.in("id", pertemunaAdaAudio)) : Restrictions.sqlRestriction("true"))

				.add(Restrictions.or(Restrictions.isNotNull("wisuda"), Restrictions.or(
						Restrictions.isNotNull("formulirKegiatan"),
						Restrictions.or(Restrictions.isNotNull("perkuliahan"), Restrictions.or(
								Restrictions.isNotNull("mahasiswaRequestTugasAkhir"),
								Restrictions.or(Restrictions.isNotNull("kelompokKkn"), Restrictions.or(
										Restrictions.isNotNull("kelompokPkl"),
										Restrictions.or(Restrictions.isNotNull("skripsi"),
												Restrictions.or(Restrictions.isNotNull("krsMahasiswa"),
														Restrictions.or(Restrictions.isNotNull("jadwalPelajaran"),
																Restrictions.isNotNull(
																		"pertemuanPunyaGrupPertemuan")))))))))))

				.add(ke == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("pertemuanKe", ke))
				.add(sp == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("statusPertemuan", sp))

				.add(mulai != null && sampai != null
						? Restrictions.sqlRestriction(
								"date(this_.tanggal) between date('" + Common.databaseDateFormat.get().format(mulai)
										+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')")
						: Restrictions.sqlRestriction("date(this_.tanggal) between date('"
								+ Common.databaseDateFormat.get().format(calendarMinus.getTime()) + "') and date('"
								+ Common.databaseDateFormat.get().format(calendarPlus.getTime()) + "')"))

				.add(d == null ? Restrictions.sqlRestriction("true")
						: Restrictions.sqlRestriction("trim(to_char(this_.tanggal, 'day')) = '" + d + "'"))

				.add(mul == null && sam == null ? Restrictions.sqlRestriction("true")
						: mul != null && sam != null
								? Restrictions.or(Restrictions.between("waktuMulai", mul, sam),
										Restrictions.between("waktuSelesai", mul, sam))
								: mul != null
										? Restrictions.or(Restrictions.ge("waktuMulai", mul),
												Restrictions.ge("waktuSelesai", mul))
										: Restrictions.or(Restrictions.le("waktuMulai", sam),
												Restrictions.le("waktuSelesai", sam)))

				.add(modulPerguruanTinggi && jadwalPerkuliahan ? Restrictions.sqlRestriction("true")
						: Restrictions.isNull("perkuliahan"))
				.add(modulSekolah && jadwalPelajaran ? Restrictions.sqlRestriction("true")
						: Restrictions.isNull("jadwalPelajaran"))
				.add(modulPerguruanTinggi && jadwalKkn ? Restrictions.sqlRestriction("true")
						: Restrictions.isNull("kelompokKkn"))
				.add(modulPerguruanTinggi && jadwalPkl ? Restrictions.sqlRestriction("true")
						: Restrictions.isNull("kelompokPkl"))
				.add(modulPerguruanTinggi && jadwalRevisi ? Restrictions.sqlRestriction("true")
						: Restrictions.isNull("skripsi"))
				.add(modulPerguruanTinggi && jadwalKonsultasi ? Restrictions.sqlRestriction("true")
						: Restrictions.isNull("krsMahasiswa"))
				.add(modulPerguruanTinggi && jadwalBimbingan ? Restrictions.sqlRestriction("true")
						: Restrictions.isNull("mahasiswaRequestTugasAkhir"))
				.add(modulPerguruanTinggi && jadwalKonsultasiLain ? Restrictions.sqlRestriction("true")
						: Restrictions.isNull("pertemuanPunyaGrupPertemuan"))
				.add(modulPerguruanTinggi && jadwalKegiatan ? Restrictions.sqlRestriction("true")
						: Restrictions.isNull("formulirKegiatan"))

				.add(tdpUjian || !namaUjian.trim().isEmpty()
						? Restrictions.sqlRestriction("this_.id in (select pertemuan from pertemuan_punya_ujian "
								+ (namaUjian.isEmpty() ? "" : "where nama ilike '%" + namaUjian + "%'")
								+ " group by pertemuan)")
						: Restrictions.sqlRestriction("true"))

				.add(tdpDiskusi
						? Restrictions.sqlRestriction(
								"this_.id in (select pertemuan from pertemuan_punya_diskusi group by pertemuan)")
						: Restrictions.sqlRestriction("true"))
				.add(tdpTugas ? Restrictions.ne("judultugas", "") : Restrictions.sqlRestriction("true"))
				.add(tdpCatatan ? Restrictions.ne("catatan", "") : Restrictions.sqlRestriction("true"))
				.add(tdpDosenPengganti ? Restrictions.isNotNull("dosenPengganti") : Restrictions.sqlRestriction("true"))
				.add(Restrictions.isNotNull("tanggal"));

		if (cariTopik != null && !cariTopik.trim().isEmpty()) {
			criteria.add(Restrictions.or(Restrictions.ilike("topik", cariTopik.trim(), MatchMode.ANYWHERE),
					Restrictions.ilike("judultugas", cariTopik.trim(), MatchMode.ANYWHERE)));
		}
		if (cariCatatan != null && !cariCatatan.trim().isEmpty()) {
			criteria.add(Restrictions.ilike("catatan", cariCatatan.trim(), MatchMode.ANYWHERE));
		}

		if (modulSekolah && guru != null) {
			criteria.createAlias("jadwalPelajaran", "jadwalPelajaran", Criteria.LEFT_JOIN);

			Criterion criterionDsn = Restrictions.eq("guruPengganti", guru.getId());
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru2", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru3", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru4", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru5", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru6", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru7", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru8", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru9", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru10", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru11", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru12", guru));

			criteria.add(criterionDsn);

		} else if (modulSekolah && siswa != null) {
			String sql = "this_.kelas_id in (select kelas_id from sekolah.kelas_punya_siswa where siswa_id="
					+ siswa.getId() + " and kelas_id is not null and aktif=true group by kelas_id)";
			Criterion criterionKls = Restrictions.sqlRestriction(sql);

			sql = "this_.kelas_les_siswa in (select kelas_id from sekolah.kelas_les_punya_siswa where siswa_id="
					+ siswa.getId() + " and kelas_id is not null and aktif=true group by kelas_id)";
			Criterion criterionLes = Restrictions.sqlRestriction(sql);

			List<Long> id = session.createCriteria(JadwalPelajaran.class).setProjection(Projections.property("id"))
					.add(Restrictions.or(criterionKls, criterionLes)).list();

			Criterion criterionMhs = id.isEmpty() ? Restrictions.sqlRestriction("false")
					: Restrictions.in("jadwalPelajaran.id", id);
			criteria.add(criterionMhs);

		} else {
			if (modulPerguruanTinggi) {
				criteria.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)
						.add(paralel ? Restrictions.or(Restrictions.sqlRestriction(
								"this_.perkuliahan in (select perkuliahan_paralel from perkuliahan where perkuliahan_paralel is not null)"),
								Restrictions.eq("perkuliahan.merupakan_paralel", true))
								: Restrictions.sqlRestriction("1=1"))
						.add(pra ? Restrictions.eq("perkuliahan.merupakanPraPerkuliahan", true)
								: Restrictions.or(Restrictions.eq("perkuliahan.merupakanPraPerkuliahan", false),
										Restrictions.isNull("perkuliahan.merupakanPraPerkuliahan")))
						.add(remedial ? Restrictions.eq("perkuliahan.merupakanRemedial", true)
								: Restrictions.or(Restrictions.isNull("perkuliahan.merupakanRemedial"),
										Restrictions.eq("perkuliahan.merupakanRemedial", false)))
						.createAlias("perkuliahan.jurusan", "jurusan", Criteria.LEFT_JOIN)
						.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.eq("jurusan.fakultas", fakultas),
										Restrictions.eq("fakultasId", fakultas.getId())))
						.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.eq("jurusanId", jurusan.getId()),
										Restrictions.eq("perkuliahan.jurusan", jurusan)))
						.add(program == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.eq("program", program),
										Restrictions.eq("perkuliahan.program", program)));

				if (cariMk != null && !cariMk.trim().isEmpty()) {
					criteria.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)
							.add(Restrictions.or(
									Restrictions.ilike("matakuliah.nama", cariMk.trim(), MatchMode.ANYWHERE),
									Restrictions.ilike("matakuliah.kode", cariMk.trim(), MatchMode.ANYWHERE)));

					if (ekstra)
						criteria.add(Restrictions.eq("matakuliah.extraKulikuler", true));
					else
						criteria.add(Restrictions.or(Restrictions.isNull("matakuliah.extraKulikuler"),
								Restrictions.eq("matakuliah.extraKulikuler", false)));

				} else if (ekstra) {
					criteria.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)
							.add(Restrictions.eq("matakuliah.extraKulikuler", true));
				} else {
					criteria.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)
							.add(Restrictions.or(Restrictions.isNull("matakuliah.extraKulikuler"),
									Restrictions.eq("matakuliah.extraKulikuler", false)));
				}

				if (cariKelas != null && !cariKelas.trim().isEmpty()) {
					criteria.add(Restrictions.ilike("perkuliahan.kelas", cariKelas.trim(), MatchMode.ANYWHERE));
				}

				if (cariRuang != null && !cariRuang.trim().isEmpty()) {
					criteria.createAlias("perkuliahan.ruang", "ruang", Criteria.LEFT_JOIN)
							.add(Restrictions.or(
									Restrictions.ilike("ruang.kodeRuangan", cariRuang.trim(), MatchMode.ANYWHERE),
									Restrictions.ilike("ruang.nama", cariRuang.trim(), MatchMode.ANYWHERE)));
				}

				if (cariDosen != null && !cariDosen.trim().isEmpty()) {
					Criterion criterionNamaDosn = Restrictions.ilike("dosen1.nama", cariDosen.trim(),
							MatchMode.ANYWHERE);
					criterionNamaDosn = Restrictions.or(criterionNamaDosn,
							Restrictions.ilike("dosen2.nama", cariDosen.trim(), MatchMode.ANYWHERE));
					criterionNamaDosn = Restrictions.or(criterionNamaDosn,
							Restrictions.ilike("dosen3.nama", cariDosen.trim(), MatchMode.ANYWHERE));
					criterionNamaDosn = Restrictions.or(criterionNamaDosn,
							Restrictions.ilike("dosen4.nama", cariDosen.trim(), MatchMode.ANYWHERE));
					criterionNamaDosn = Restrictions.or(criterionNamaDosn,
							Restrictions.ilike("dosen5.nama", cariDosen.trim(), MatchMode.ANYWHERE));
					criterionNamaDosn = Restrictions.or(criterionNamaDosn,
							Restrictions.ilike("dosen6.nama", cariDosen.trim(), MatchMode.ANYWHERE));
					criterionNamaDosn = Restrictions.or(criterionNamaDosn,
							Restrictions.ilike("dosen7.nama", cariDosen.trim(), MatchMode.ANYWHERE));
					criterionNamaDosn = Restrictions.or(criterionNamaDosn,
							Restrictions.ilike("dosen8.nama", cariDosen.trim(), MatchMode.ANYWHERE));
					criterionNamaDosn = Restrictions.or(criterionNamaDosn,
							Restrictions.ilike("dosen9.nama", cariDosen.trim(), MatchMode.ANYWHERE));
					criterionNamaDosn = Restrictions.or(criterionNamaDosn,
							Restrictions.ilike("dosen10.nama", cariDosen.trim(), MatchMode.ANYWHERE));

					criteria.createAlias("perkuliahan.dosen1", "dosen1", Criteria.LEFT_JOIN)
							.createAlias("perkuliahan.dosen2", "dosen2", Criteria.LEFT_JOIN)
							.createAlias("perkuliahan.dosen3", "dosen3", Criteria.LEFT_JOIN)
							.createAlias("perkuliahan.dosen4", "dosen4", Criteria.LEFT_JOIN)
							.createAlias("perkuliahan.dosen5", "dosen5", Criteria.LEFT_JOIN)
							.createAlias("perkuliahan.dosen6", "dosen6", Criteria.LEFT_JOIN)
							.createAlias("perkuliahan.dosen7", "dosen7", Criteria.LEFT_JOIN)
							.createAlias("perkuliahan.dosen8", "dosen8", Criteria.LEFT_JOIN)
							.createAlias("perkuliahan.dosen9", "dosen9", Criteria.LEFT_JOIN)
							.createAlias("perkuliahan.dosen10", "dosen10", Criteria.LEFT_JOIN).add(criterionNamaDosn);
				}
			}

			if (modulSekolah) {
				criteria.add(yayasan == null ? Restrictions.sqlRestriction("1=1")
						: modulPerguruanTinggi
								? Restrictions.or(Restrictions.eq("yayasanId", yayasan.getId()),
										Restrictions.isNull("jadwalPelajaran"))
								: Restrictions.eq("yayasanId", yayasan.getId()))
						.add(sekolah == null ? Restrictions.sqlRestriction("1=1")
								: modulPerguruanTinggi
										? Restrictions.or(Restrictions.eq("sekolahId", sekolah.getId()),
												Restrictions.isNull("jadwalPelajaran"))
										: Restrictions.eq("sekolahId", sekolah.getId()));
			}
		}

		if (cariMahasiswa != null && !cariMahasiswa.trim().isEmpty()) {
			criteria.createAlias("mahasiswaRequestTugasAkhir", "mahasiswaRequestTugasAkhir_cari_mhs",
					Criteria.LEFT_JOIN).createAlias("skripsi", "skripsi_cari_mhs", Criteria.LEFT_JOIN)
					.createAlias("krsMahasiswa", "krsMahasiswa_cari_mhs", Criteria.LEFT_JOIN)
					.createAlias("mahasiswaRequestTugasAkhir_cari_mhs.mahasiswa", "mahasiswa_bimbingan",
							Criteria.LEFT_JOIN)
					.createAlias("skripsi.mahasiswa", "mahasiswa_sidang", Criteria.LEFT_JOIN)
					.createAlias("krsMahasiswa.mahasiswa", "mahasiswa_krs", Criteria.LEFT_JOIN);

			String cari = cariMahasiswa.trim();
			Criterion c = Restrictions.ilike("mahasiswa_bimbingan.nim", cari, MatchMode.ANYWHERE);
			c = Restrictions.or(c, Restrictions.ilike("mahasiswa_bimbingan.nama", cari, MatchMode.ANYWHERE));
			c = Restrictions.or(c, Restrictions.ilike("mahasiswa_sidang.nim", cari, MatchMode.ANYWHERE));
			c = Restrictions.or(c, Restrictions.ilike("mahasiswa_sidang.nama", cari, MatchMode.ANYWHERE));
			c = Restrictions.or(c, Restrictions.ilike("mahasiswa_krs.nim", cari, MatchMode.ANYWHERE));
			c = Restrictions.or(c, Restrictions.ilike("mahasiswa_krs.nama", cari, MatchMode.ANYWHERE));

			criteria.add(c);
		}

		if (order) {
			criteria.addOrder(Order.asc("tanggal")).addOrder(Order.asc("waktuMulai")).addOrder(Order.asc("id"));
		}

		if (ujian) {
			criteria.createAlias("statusPertemuan", "statusPertemuan", Criteria.LEFT_JOIN)
					.add(Restrictions.or(Restrictions.ilike("statusPertemuan.nama", "UTS", MatchMode.ANYWHERE),
							Restrictions.ilike("statusPertemuan.nama", "UAS", MatchMode.ANYWHERE)));
		}

		return criteria;
	}

	public TreeMap<String, Long> pertemuansa = null;

	private Integer bulanSdlama = null;
	private Number tengahTengah = null;

	private int mulaiParam = -1;
	private int sampaiParam = -1;

	private void renderDashboardSummaryRow(final Row rowDashboardSummary, final boolean refresh) {
		try {
			if (rowDashboardSummary == null) {
				return;
			}
			Common.clear(rowDashboardSummary);
			Component ds = DashboardTimelinePertemuan.buildDashboardSummary(tbmuser, refresh, pertemuansa);
			if (ds != null) {
				rowDashboardSummary.setVisible(true);
				rowDashboardSummary.appendChild(ds);
			} else {
				rowDashboardSummary.setVisible(false);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5083");
			try {
				if (rowDashboardSummary != null) {
					Common.clear(rowDashboardSummary);
					rowDashboardSummary.appendChild(new Html(buildErrorDashboardHtml(e)));
				}
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5089");
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	private static Row createTimelineLoadingRow(Rows rows, String message, int percent) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		if (rows != null) {
			rows.appendChild(row);
		}
		updateTimelineLoadingRow(row, message, percent);
		return row;
	}

	private static void updateTimelineLoadingRow(Row row, String message, int percent) {
		try {
			if (row == null) {
				return;
			}
			row.setVisible(true);
			Common.clear(row);
			row.appendChild(new Html(buildELearningLoadingHtml(message, percent)));
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5114");
		}
	}

	private static void hideTimelineLoadingRow(Row row) {
		try {
			if (row == null) {
				return;
			}
			Common.clear(row);
			row.setVisible(false);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5126");
		}
	}

	private static String buildTimelineLoadingMessage(String jenisData, int current, int total) {
		if (total <= 0) {
			return jenisData == null ? "Menyiapkan data pertemuan..." : jenisData;
		}
		return (jenisData == null ? "Memproses data pertemuan" : jenisData) + " "
				+ Common.numberFormat.get().format(current) + " dari " + Common.numberFormat.get().format(total)
				+ " data. Daftar pertemuan akan tampil otomatis setelah proses selesai.";
	}

	private static void closeSessionFullQuietly(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5145");
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5149");
		}
		try {
			closeHibernateSessionQuietly(session);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5153");
		}
	}

	private String buildTimelineCacheKey() {
		String bln = (this.bulanSd == null || this.bulanSd.getSelectedItem() == null
				|| this.bulanSd.getSelectedItem().getValue() == null ? "1"
				: String.valueOf(this.bulanSd.getSelectedItem().getValue()));
		String hr = (this.hari == null || this.hari.getSelectedItem() == null ? ""
				: String.valueOf(this.hari.getSelectedItem().getValue()));
		String sp = (this.cariStatusPertemuan == null || this.cariStatusPertemuan.getSelectedItem() == null ? ""
				: String.valueOf(this.cariStatusPertemuan.getSelectedItem().getValue()));
		String ke = (this.cariPertemunaKe == null || this.cariPertemunaKe.getSelectedItem() == null ? ""
				: String.valueOf(this.cariPertemunaKe.getSelectedItem().getValue()));
		String mul = (this.cariWaktuMulai == null || this.cariWaktuMulai.getValue() == null
				|| this.cariWaktuMulai.isDisabled() ? ""
				: Common.timeFormat2.get().format(this.cariWaktuMulai.getValue()));
		String sam = (this.cariWaktuSampai == null || this.cariWaktuSampai.getValue() == null
				|| this.cariWaktuSampai.isDisabled() ? ""
				: Common.timeFormat2.get().format(this.cariWaktuSampai.getValue()));
		return (tbmuser == null ? "0" : String.valueOf(tbmuser.getId()))
				+ "|" + bln + "|" + hr + "|" + sp + "|" + ke + "|" + mul + "|" + sam
				+ "|" + (this.cariTopik == null ? "" : this.cariTopik.getValue().trim())
				+ "|" + (this.cariMk == null ? "" : this.cariMk.getValue().trim())
				+ "|" + (this.cariDosen == null ? "" : this.cariDosen.getValue().trim())
				+ "|" + (this.cariMahasiswa == null ? "" : this.cariMahasiswa.getValue().trim())
				+ "|" + (this.cariCatatan == null ? "" : this.cariCatatan.getValue().trim())
				+ "|" + (this.cariKelas == null ? "" : this.cariKelas.getValue().trim())
				+ "|" + (this.cariRuang == null ? "" : this.cariRuang.getValue().trim())
				+ "|" + (ujian == null ? "f" : (ujian ? "t" : "f"))
				+ "|" + (jadwalPerkuliahan == null ? "t" : (jadwalPerkuliahan.isChecked() ? "t" : "f"))
				+ "|" + (jadwalPelajaran == null ? "t" : (jadwalPelajaran.isChecked() ? "t" : "f"))
				+ "|" + (jadwalKkn == null ? "t" : (jadwalKkn.isChecked() ? "t" : "f"))
				+ "|" + (jadwalPkl == null ? "t" : (jadwalPkl.isChecked() ? "t" : "f"))
				+ "|" + (jadwalRevisi == null ? "t" : (jadwalRevisi.isChecked() ? "t" : "f"))
				+ "|" + (jadwalKonsultasi == null ? "t" : (jadwalKonsultasi.isChecked() ? "t" : "f"))
				+ "|" + (jadwalBimbingan == null ? "t" : (jadwalBimbingan.isChecked() ? "t" : "f"))
				+ "|" + (jadwalKonsultasiLain == null ? "t" : (jadwalKonsultasiLain.isChecked() ? "t" : "f"))
				+ "|" + (jadwalKegiatan == null ? "t" : (jadwalKegiatan.isChecked() ? "t" : "f"))
				+ "|" + paralel.isChecked() + "|" + pra.isChecked()
				+ "|" + remedial.isChecked() + "|" + ekstra.isChecked();
	}

	public void initSpreadsheet(final boolean refresh, final boolean jadikanAwal) throws Exception {

		final Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		final Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();

		final String mul = cariWaktuMulai == null || cariWaktuMulai.getValue() == null || cariWaktuMulai.isDisabled()
				? null
				: Common.timeFormat2.get().format(cariWaktuMulai.getValue());
		final String sam = cariWaktuSampai == null || cariWaktuSampai.getValue() == null || cariWaktuSampai.isDisabled()
				? null
				: Common.timeFormat2.get().format(cariWaktuSampai.getValue());

		final Integer bln = (Integer) (this.bulanSd == null || this.bulanSd.getSelectedItem() == null
				|| this.bulanSd.getSelectedItem().getValue() == null ? 1 : this.bulanSd.getSelectedItem().getValue());

		Calendar[] cc = plusMinus(bln);
		final Calendar calendarPlus = cc[0];
		final Calendar calendarMinus = cc[1];

		final Integer ke = (Integer) (cariPertemunaKe == null || cariPertemunaKe.getSelectedItem() == null ? null
				: cariPertemunaKe.getSelectedItem().getValue());

		final boolean mobile = Common.isMobile();

		final boolean awal;
		if (bulanSdlama == null || !bulanSdlama.equals(bln)) {
			bulanSdlama = bln;
			awal = true;
		} else {
			awal = pertemuansa == null;
		}

		// DIAGNOSTIK e-Learning center (Linimasa): tampilkan jalur & hasil pemuatan agar penyebab
		// "center blank / panel kanan loading tak selesai" terlihat di log server.
		System.out.println("[E-LEARNING-TIMELINE] initSpreadsheet mulai: dosen=" + (dosen != null) + " mahasiswa="
				+ (mahasiswa != null) + " admin=" + (dosen == null && mahasiswa == null) + " refresh=" + refresh
				+ " jadikanAwal=" + jadikanAwal + " awal=" + awal + " bulanSd=" + bln + " pertemuansaSblm="
				+ (pertemuansa == null ? "null" : ("" + pertemuansa.size())));

		if (awal || refresh) {
			paging.setAttribute("mulaiParam", null);
			paging.setAttribute("sampaiParam", null);
		}

		if (rowsUtama != null) {

			Common.clear(rowsUtama);

		}

		final MyFormRow rowDashboardSummary = new MyFormRow();
		rowDashboardSummary.setValign("top");
		rowsUtama.appendChild(rowDashboardSummary);

		final Row rowLoadingTimeline = createTimelineLoadingRow(rowsUtama,
				"Menyiapkan daftar pertemuan, agenda, absensi, materi, tugas, ujian, video, dan audio...", 4);

		final MyToolbarbuttonConfig back = new MyToolbarbuttonConfig("Tampilkan pertemuan sebelumnya.. ",
				"/img/Go-back-icon.png");

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setVisible(false);
		rowsUtama.appendChild(row);

		back.setStyle("font-size:12px;");
		row.appendChild(back);
		back.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				mulaiParam = mulaiParam - displayPerPage;
				sampaiParam = sampaiParam + displayPerPage;
				paging.setAttribute("mulaiParam", mulaiParam);
				paging.setAttribute("sampaiParam", sampaiParam);
				initSpreadsheet(false, false);
			}
		});

		if (dosen != null) {
			EventListener eventListener = new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = null;
					try {
						updateTimelineLoadingRow(rowLoadingTimeline,
								"Mengambil daftar pertemuan dosen dari cache pembelajaran...", 15);
						session = HibernateUtil.getSessionFactory().openSession();
						if (awal || refresh) {
							pertemuansa = dosen.ambilPertemuan(session);
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5287");
					} finally {
						if (session != null) {
							try {
								session.clear();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5292");
							}
							try {
								session.disconnect();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5296");
							}
							try {
								closeHibernateSessionQuietly(session);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5300");
							}
						}
					}

					renderDashboardSummaryRow(rowDashboardSummary, refresh);

					List<Long> pertemuans = dosen.ambilPertemuan(pertemuansa,
							jadwalPerkuliahan == null ? true : jadwalPerkuliahan.isChecked(),
							jadwalKkn == null ? true : jadwalKkn.isChecked(),
							jadwalPkl == null ? true : jadwalPkl.isChecked(),
							jadwalKegiatan == null ? true : jadwalKegiatan.isChecked(),
							jadwalRevisi == null ? true : jadwalRevisi.isChecked(),
							jadwalKonsultasi == null ? true : jadwalKonsultasi.isChecked(),
							jadwalBimbingan == null ? true : jadwalBimbingan.isChecked(),
							jadwalKonsultasiLain == null ? true : jadwalKonsultasiLain.isChecked(),
							tdpDiskusi == null ? false : tdpDiskusi.isChecked(),
							tdpUjian == null ? ujian : tdpUjian.isChecked(),
							cariNamaUjian != null && !cariNamaUjian.getValue().trim().isEmpty()
									? cariNamaUjian.getValue().trim()
									: "",
							tdpMateri == null ? false : tdpMateri.isChecked(),
							tdpTugas == null ? false : tdpTugas.isChecked(),
							tdpCatatan == null ? false : tdpCatatan.isChecked(),
							tdpAudio == null ? false : tdpAudio.isChecked(),
							tdpVideo == null ? false : tdpVideo.isChecked(),
							tdpDosenPengganti == null ? false : tdpDosenPengganti.isChecked(),
							ais.ui.util.WaktuUtil.getDate(), cariMk == null ? "" : cariMk.getValue().trim(),
							cariDosen == null ? "" : cariDosen.getValue().trim(), mul, sam,
							cariTopik == null ? "" : cariTopik.getValue().trim(),
							cariCatatan == null ? "" : cariCatatan.getValue().trim(),
							hari == null || hari.getSelectedItem() == null || hari.getSelectedItem().getValue() == null
									? null
									: hari.getSelectedItem().getValue().toString(),
							cariMahasiswa == null ? "" : cariMahasiswa.getValue().trim(),
							cariKelas == null ? "" : cariKelas.getValue().trim(),
							cariRuang == null ? "" : cariRuang.getValue().trim(), pra.isChecked(),
							ekstra.isChecked() ? Perkuliahan.EKSTRA : null, remedial.isChecked(), paralel.isChecked(),
							tdpOnline == null ? false : tdpOnline.isChecked(),
							(StatusPertemuan) (cariStatusPertemuan == null
									|| cariStatusPertemuan.getSelectedItem() == null ? null
											: cariStatusPertemuan.getSelectedItem().getValue()),
							ke, paging, awal || refresh || jadikanAwal, displayPerPage, back, tbmuser);

					int totalRenderPertemuan = pertemuans == null ? 0 : pertemuans.size();
					int indexRenderPertemuan = 0;
					updateTimelineLoadingRow(rowLoadingTimeline,
							buildTimelineLoadingMessage("Menampilkan agenda dosen", 0, totalRenderPertemuan), 56);
					for (Long pertemuanid : pertemuans) {
						indexRenderPertemuan++;
						int persenRender = hitungPersen(indexRenderPertemuan, totalRenderPertemuan);
						if (shouldUpdateKehadiranProgress(indexRenderPertemuan, totalRenderPertemuan, 5, persenRender,
								-1)) {
							updateTimelineLoadingRow(
									rowLoadingTimeline, buildTimelineLoadingMessage("Menampilkan agenda dosen",
											indexRenderPertemuan, totalRenderPertemuan),
									56 + (int) Math.round(persenRender * 0.34));
						}
						Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
								pertemuanid.toString());
						if (pertemuan != null) {
							if (refresh) {
								Common.refresh(pertemuan);
								pertemuan.belum("KelompokParameterTambahanPertemuan");
							}

							MyFormRow rowInner = new MyFormRow();
							rowInner.setValign("top");
							rowsUtama.appendChild(rowInner);

							Long selectedDiskusi = null;
							displayRow(rowInner, mobile, pertemuan, selectedDiskusi);
						}
					}

					hideTimelineLoadingRow(rowLoadingTimeline);

					hideTimelineLoadingRow(rowLoadingTimeline);

					mulaiParam = displayPerPage * paging.getActivePage();
					sampaiParam = displayPerPage;

					pertemuans.clear();
					pertemuans = null;

					if (paging.getAttribute("mulaiParam") != null)
						mulaiParam = (Integer) paging.getAttribute("mulaiParam");
					if (paging.getAttribute("sampaiParam") != null)
						sampaiParam = (Integer) paging.getAttribute("sampaiParam");

					if (paging != null && paging.getTotalSize() > (mulaiParam + sampaiParam)) {
						MyFormRow rowInner = new MyFormRow();
						rowInner.setValign("top");
						rowsUtama.appendChild(rowInner);

						MyToolbarbuttonConfig a = new MyToolbarbuttonConfig(
								"Tampilkan pertemuan selanjutnya.. ("
										+ (paging.getTotalSize() - (mulaiParam + sampaiParam)) + " pertemuan)",
								"/img/Button-Next-icon.png");
						a.setStyle("font-size:12px;");
						rowInner.appendChild(a);
						a.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								sampaiParam = sampaiParam + displayPerPage;
								paging.setAttribute("mulaiParam", mulaiParam);
								paging.setAttribute("sampaiParam", sampaiParam);
								initSpreadsheet(false, false);
							}
						});
					}

					if (reloadBlnSd && eventRefresh != null) {
						reloadBlnSd = false;
						eventRefresh.onEvent(arg0);
					}
				}
			};

			if (!dosen.udah("dosen_buka_elearning_" + bln + "_"
					+ Common.dateFormatWeek.get().format(ais.ui.util.WaktuUtil.getDate())) || refresh) {
				final Label label = Common.displayLoadBar(eventListener);
				new Thread(new Runnable() {
					@Override
					public void run() {
						Session session = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();
							dosen.reInitPertemuan(session, label, calendarPlus, calendarMinus);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5430");
						} finally {
							label.setValue("");
							if (session != null) {
								try {
									session.clear();
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5436");
								}
								try {
									session.disconnect();
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5440");
								}
								try {
									closeHibernateSessionQuietly(session);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5444");
								}
							}
						}
					}
				}).start();

			} else {
				eventListener.onEvent(null);
			}

		} else if (mahasiswa != null) {
			EventListener eventListener = new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = null;
					try {
						updateTimelineLoadingRow(rowLoadingTimeline,
								"Mengambil daftar pertemuan mahasiswa dari cache pembelajaran...", 15);
						session = HibernateUtil.getSessionFactory().openSession();
						if (awal || refresh) {
							pertemuansa = mahasiswa.ambilPertemuan(session);
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5468");
					} finally {
						if (session != null) {
							try {
								session.clear();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5473");
							}
							try {
								session.disconnect();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5477");
							}
							try {
								closeHibernateSessionQuietly(session);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5481");
							}
						}
					}

					renderDashboardSummaryRow(rowDashboardSummary, refresh);

					boolean wisuda = true;
					List<Long> pertemuans = mahasiswa.ambilPertemuan(pertemuansa,
							jadwalPerkuliahan == null ? true : jadwalPerkuliahan.isChecked(),
							jadwalKkn == null ? true : jadwalKkn.isChecked(),
							jadwalPkl == null ? true : jadwalPkl.isChecked(),
							jadwalKegiatan == null ? true : jadwalKegiatan.isChecked(), wisuda,
							jadwalRevisi == null ? true : jadwalRevisi.isChecked(),
							jadwalKonsultasi == null ? true : jadwalKonsultasi.isChecked(),
							jadwalBimbingan == null ? true : jadwalBimbingan.isChecked(),
							jadwalKonsultasiLain == null ? true : jadwalKonsultasiLain.isChecked(),
							tdpDiskusi == null ? false : tdpDiskusi.isChecked(),
							tdpUjian == null ? ujian : tdpUjian.isChecked(),
							cariNamaUjian != null && !cariNamaUjian.getValue().trim().isEmpty()
									? cariNamaUjian.getValue().trim()
									: "",
							tdpMateri == null ? false : tdpMateri.isChecked(),
							tdpTugas == null ? false : tdpTugas.isChecked(),
							tdpCatatan == null ? false : tdpCatatan.isChecked(),
							tdpAudio == null ? false : tdpAudio.isChecked(),
							tdpVideo == null ? false : tdpVideo.isChecked(),
							tdpDosenPengganti == null ? false : tdpDosenPengganti.isChecked(),
							ais.ui.util.WaktuUtil.getDate(), cariMk == null ? "" : cariMk.getValue().trim(),
							cariDosen == null ? "" : cariDosen.getValue().trim(), mul, sam,
							cariTopik == null ? "" : cariTopik.getValue().trim(),
							cariCatatan == null ? "" : cariCatatan.getValue().trim(),
							hari == null || hari.getSelectedItem() == null || hari.getSelectedItem().getValue() == null
									? null
									: hari.getSelectedItem().getValue().toString(),
							cariKelas == null ? "" : cariKelas.getValue().trim(),
							cariRuang == null ? "" : cariRuang.getValue().trim(), pra.isChecked(),
							ekstra.isChecked() ? Perkuliahan.EKSTRA : null, remedial.isChecked(), paralel.isChecked(),
							tdpOnline == null ? false : tdpOnline.isChecked(),
							(StatusPertemuan) (cariStatusPertemuan == null
									|| cariStatusPertemuan.getSelectedItem() == null ? null
											: cariStatusPertemuan.getSelectedItem().getValue()),
							ke, paging, awal || refresh || jadikanAwal, displayPerPage, back, tbmuser);

					int totalRenderPertemuan = pertemuans == null ? 0 : pertemuans.size();
					/*
					 * Kontainer timeline bisa sudah detach (user pindah halaman saat load bar masih
					 * berjalan); appendChild ke Rows tanpa page melempar NPE di addMoved.
					 */
					if (rowsUtama == null || rowsUtama.getDesktop() == null || rowsUtama.getPage() == null) {
						return;
					}
					int indexRenderPertemuan = 0;
					updateTimelineLoadingRow(rowLoadingTimeline,
							buildTimelineLoadingMessage("Menampilkan agenda mahasiswa", 0, totalRenderPertemuan), 56);
					for (Long pertemuanid : pertemuans) {
						indexRenderPertemuan++;
						int persenRender = hitungPersen(indexRenderPertemuan, totalRenderPertemuan);
						if (shouldUpdateKehadiranProgress(indexRenderPertemuan, totalRenderPertemuan, 5, persenRender,
								-1)) {
							updateTimelineLoadingRow(
									rowLoadingTimeline, buildTimelineLoadingMessage("Menampilkan agenda mahasiswa",
											indexRenderPertemuan, totalRenderPertemuan),
									56 + (int) Math.round(persenRender * 0.34));
						}
						Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
								pertemuanid.toString());
						if (pertemuan != null) {
							if (refresh) {
								Common.refresh(pertemuan);
								pertemuan.belum("KelompokParameterTambahanPertemuan");
							}

							MyFormRow rowInner = new MyFormRow();
							rowInner.setValign("top");
							rowsUtama.appendChild(rowInner);

							Long selectedDiskusi = null;
							displayRow(rowInner, mobile, pertemuan, selectedDiskusi);
						}
					}

					mulaiParam = displayPerPage * paging.getActivePage();
					sampaiParam = displayPerPage;

					pertemuans.clear();
					pertemuans = null;

					if (paging.getAttribute("mulaiParam") != null)
						mulaiParam = (Integer) paging.getAttribute("mulaiParam");
					if (paging.getAttribute("sampaiParam") != null)
						sampaiParam = (Integer) paging.getAttribute("sampaiParam");

					if (paging != null && paging.getTotalSize() > (mulaiParam + sampaiParam)) {
						MyFormRow rowInner = new MyFormRow();
						rowInner.setValign("top");
						rowsUtama.appendChild(rowInner);

						MyToolbarbuttonConfig a = new MyToolbarbuttonConfig(
								"Tampilkan pertemuan selanjutnya.. ("
										+ (paging.getTotalSize() - (mulaiParam + sampaiParam)) + " pertemuan)",
								"/img/Button-Next-icon.png");
						a.setStyle("font-size:12px;");
						rowInner.appendChild(a);
						a.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								sampaiParam = sampaiParam + displayPerPage;
								paging.setAttribute("mulaiParam", mulaiParam);
								paging.setAttribute("sampaiParam", sampaiParam);
								initSpreadsheet(false, false);
							}
						});
					}

					if (reloadBlnSd && eventRefresh != null) {
						reloadBlnSd = false;
						eventRefresh.onEvent(arg0);
					}
				}
			};

			if (!mahasiswa.udah("mahasiswa_buka_elearning_" + bln + "_"
					+ Common.dateFormatWeek.get().format(ais.ui.util.WaktuUtil.getDate())) || refresh) {
				final Label label = Common.displayLoadBar(eventListener);
				new Thread(new Runnable() {
					@Override
					public void run() {
						Session session = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();
							mahasiswa.reInitPertemuan(session, label, calendarPlus, calendarMinus);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5614");
						} finally {
							label.setValue("");
							if (session != null) {
								try {
									session.clear();
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5620");
								}
								try {
									session.disconnect();
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5624");
								}
								try {
									closeHibernateSessionQuietly(session);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5628");
								}
							}
						}
					}
				}).start();

			} else {
				eventListener.onEvent(null);
			}

		} else {
			try {
				paging.setPageSize(displayPerPage);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5642");
			}
			paging.setPageIncrement(Common.isMobile() ? 5 : 10);
			paging.setMold("os");
			int size = displayPerPage;

			int activePage = paging.getActivePage();
			if (awal || refresh || jadikanAwal) {
				// ── L1 cache: load pertemuansa once per filter combination ──────────
				String _tk = buildTimelineCacheKey();
				Long _te = TIMELINE_EXPIRY.get(_tk);
				if (!refresh && _te != null && _te > System.currentTimeMillis() && TIMELINE_CACHE.containsKey(_tk)) {
					@SuppressWarnings("unchecked")
					java.util.TreeMap<String, Long> _cached = (java.util.TreeMap<String, Long>) TIMELINE_CACHE.get(_tk);
					pertemuansa = new TreeMap<String, Long>(_cached);
				} else {
					pertemuansa = null;

					// LAZY LOADING: kunci-urut agenda dibangun HANYA dari PROYEKSI kolom ringan
					// (id, tanggal, waktu_mulai, waktu_selesai) — TIDAK lagi memuat SELURUH entitas Pertemuan
					// sejendela lewat GeneralValueObject.ambilData per-id (dulu memuat SEMUA pertemuan → inilah
					// yang memblok "Menyiapkan tampilan..." di ~12%). Entitas Pertemuan PENUH dimuat NANTI, hanya
					// untuk baris yang BENAR-BENAR TAMPIL (render loop di bawah: ambilData per baris, per-halaman
					// 25). Data pertemuan yang masih tersembunyi (halaman lain) baru dimuat saat dibuka.
					pertemuansa = new TreeMap<String, Long>();
					Session criteriaSession = null;
					try {
						updateTimelineLoadingRow(rowLoadingTimeline,
								"Mengambil daftar pertemuan sesuai filter pencarian...", 16);
						criteriaSession = HibernateUtil.getSessionFactory().openSession();
						Criteria tempCriteria = initCriteria(true, null, null, criteriaSession);
						if (tempCriteria != null) {
							@SuppressWarnings("unchecked")
							List<Object[]> barisPertemuan = tempCriteria
									.setProjection(Projections.projectionList().add(Projections.property("id"))
											.add(Projections.property("tanggal")).add(Projections.property("waktuMulai"))
											.add(Projections.property("waktuSelesai")))
									.list();
							int totalSusunPertemuan = barisPertemuan == null ? 0 : barisPertemuan.size();
							updateTimelineLoadingRow(rowLoadingTimeline, buildTimelineLoadingMessage(
									"Menyusun urutan agenda", totalSusunPertemuan, totalSusunPertemuan), 45);
							if (barisPertemuan != null) {
								for (Object[] baris : barisPertemuan) {
									Long idPertemuan = (Long) baris[0];
									Date tglPertemuan = (Date) baris[1];
									if (idPertemuan == null || tglPertemuan == null) {
										continue;
									}
									String waktuMulai = (String) baris[2];
									String waktuSelesai = (String) baris[3];
									String keyPert = Common.dateFormat8.get().format(tglPertemuan);
									keyPert += ("_" + (waktuMulai == null && waktuSelesai == null ? "00.00-00.00"
											: (waktuMulai == null ? "00.00" : waktuMulai) + "-"
													+ (waktuSelesai == null ? "00.00" : waktuSelesai)));
									pertemuansa.put(keyPert + "_" + idPertemuan, idPertemuan);
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5701");
					} finally {
						closeSessionFullQuietly(criteriaSession);
					}

					TIMELINE_CACHE.put(_tk, new java.util.TreeMap<String, Long>(pertemuansa));
					TIMELINE_EXPIRY.put(_tk, System.currentTimeMillis() + TIMELINE_TTL_MS);
				}

				size = pertemuansa.size();
				paging.setTotalSize(size);

				tengahTengah = 0;
				Date tanggalSekarang = WaktuUtil.getDate();
				String format = Common.dateFormat8.get().format(tanggalSekarang);
				for (String a : pertemuansa.keySet()) {
					try {
						String s = a.split("_")[0];
						if (format.equals(s)) {
							break;
						}
						Date tgl = Common.dateFormat8.get().parse(s);
						if (tgl.before(tanggalSekarang)) {
							tengahTengah = tengahTengah.intValue() + 1;
						} else {
							tengahTengah = tengahTengah.intValue() + 1;
							break;
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5729");
					}
				}

				try {
					activePage = (int) (tengahTengah.intValue() / displayPerPage);
					paging.setActivePage(activePage);
				} catch (Exception e) {
					try {
						activePage = (int) (tengahTengah.intValue() / displayPerPage);
						paging.setActivePage(activePage - 1);
					} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5740");
					}
				}
			}

			renderDashboardSummaryRow(rowDashboardSummary, refresh);

			mulaiParam = displayPerPage * paging.getActivePage();
			sampaiParam = displayPerPage;

			if (paging.getAttribute("mulaiParam") != null)
				mulaiParam = (Integer) paging.getAttribute("mulaiParam");
			if (paging.getAttribute("sampaiParam") != null)
				sampaiParam = (Integer) paging.getAttribute("sampaiParam");

			int index = 0;
			int jml = 0;
			int totalRenderPertemuan = pertemuansa == null ? 0 : pertemuansa.size();
			updateTimelineLoadingRow(rowLoadingTimeline,
					buildTimelineLoadingMessage("Menampilkan agenda", 0, totalRenderPertemuan), 62);
			for (Long pertemuanid : pertemuansa.values()) {
				if (index < mulaiParam)
					jml++;

				int persenRender = hitungPersen(index + 1, totalRenderPertemuan);
				if (shouldUpdateKehadiranProgress(index + 1, totalRenderPertemuan, 10, persenRender, -1)) {
					updateTimelineLoadingRow(rowLoadingTimeline,
							buildTimelineLoadingMessage("Menampilkan agenda", index + 1, totalRenderPertemuan),
							62 + (int) Math.round(persenRender * 0.28));
				}

				if (index >= mulaiParam && index < (mulaiParam + sampaiParam)) {
					Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
							pertemuanid.toString(), true);
					if (pertemuan != null) {
						if (refresh) {
							Common.refresh(pertemuan);
							pertemuan.belum("KelompokParameterTambahanPertemuan");
						}

						row = new MyFormRow();
						rowsUtama.appendChild(row);

						Long selectedDiskusi = null;
						displayRow(row, mobile, pertemuan, selectedDiskusi);
					}
				}
				index++;
			}

			hideTimelineLoadingRow(rowLoadingTimeline);

			back.setLabel("Tampilkan pertemuan sebelumnya.. (" + jml + " pertemuan)");
			back.getParent().setVisible(jml > 0);

			if (paging != null && paging.getTotalSize() > (mulaiParam + sampaiParam)) {
				row = new MyFormRow();
				rowsUtama.appendChild(row);

				MyToolbarbuttonConfig a = new MyToolbarbuttonConfig("Tampilkan pertemuan selanjutnya.. ("
						+ (paging.getTotalSize() - (mulaiParam + sampaiParam)) + " pertemuan)",
						"/img/Button-Next-icon.png");
				a.setStyle("font-size:12px;");
				row.appendChild(a);
				a.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						sampaiParam = sampaiParam + displayPerPage;
						paging.setAttribute("mulaiParam", mulaiParam);
						paging.setAttribute("sampaiParam", sampaiParam);
						initSpreadsheet(false, false);
					}
				});
			}

			if (reloadBlnSd && eventRefresh != null) {
				reloadBlnSd = false;
				eventRefresh.onEvent(null);
			}
		}

	}

	public static void loadKomentarDetail(final PertemuanPunyaDiskusi parent, String lebarAwal,
			TreeSet<Long> pertemuanPunyaDiskusisa, Pertemuan pertemuan, Component tabpanelUtama, String style,
			int mulai, int banyak, boolean tampilanInfo, EventListener eventListenerUtama) throws Exception {
		loadKomentarDetail(parent, lebarAwal, pertemuanPunyaDiskusisa, pertemuan, tabpanelUtama, style, mulai, banyak,
				tampilanInfo, eventListenerUtama, null);
	}

	public static void loadKomentarDetail(final PertemuanPunyaDiskusi parent, String lebarAwal,
			TreeSet<Long> pertemuanPunyaDiskusisa, Pertemuan pertemuan, Component tabpanelUtama, String style,
			int mulai, int banyak, boolean tampilanInfo, EventListener eventListenerUtama, Long selectedDiskusi)
			throws Exception {
		List<EventListener> utama = new ArrayList<EventListener>();
		loadKomentarDetail(parent, lebarAwal, pertemuanPunyaDiskusisa, pertemuan, tabpanelUtama, style, mulai, banyak,
				tampilanInfo, utama, eventListenerUtama, selectedDiskusi);
	}

	@SuppressWarnings("deprecation")
	public static void loadKomentarDetail(final PertemuanPunyaDiskusi parent, final String lebarAwal,
			final TreeSet<Long> pertemuanPunyaDiskusisa, final Pertemuan pertemuan, final Component tabpanelUtama,
			final String style, final int mulai, final int banyak, final boolean tampilanInfo,
			final List<EventListener> utama, final EventListener eventListenerUtama, final Long selectedDiskusi)
			throws Exception {

		if (tabpanelUtama != null) {

			Common.clear(tabpanelUtama);

		}

		if (tabpanelUtama instanceof HtmlBasedComponent)
			((HtmlBasedComponent) tabpanelUtama).setStyle("overflow: hidden;border: none;");

		Grid grid = new Grid();
		grid.setSclass("dgrid fgrid");
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setParent(tabpanelUtama);
		grid.setStyle("overflow: hidden;border: none;");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column1 = new MyColumnConfig();
		column1.setParent(columns);
		column1.setLabel("");
		column1.setWidth("0px");

		MyColumnConfig column2 = new MyColumnConfig();
		column2.setParent(columns);
		column2.setLabel("");
		column2.setWidth(parent == null ? "0px" : (parent.getParent() == null ? "20px" : "40px"));

		MyColumnConfig column3 = new MyColumnConfig();
		column3.setParent(columns);
		column3.setLabel("");

		Rows rows = new Rows();

		rows.setParent(grid);

		final Tbmuser tbmuser = Common.getCurrentUser();
		final Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		final Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();
		final CalonSiswa calonSiswa = tbmuser == null ? null : tbmuser.getCalonSiswa();
		final BiodataCalonMahasiswa biodataCalonMahasiswa = tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa();
		final Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		final Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();

		if (parent == null) {
			if (tampilanInfo) {
				MyFormRow rowInfo = new MyFormRow();
				rowInfo.setValign("top");
				rowInfo.setStyle(style);
				rowInfo.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(rowInfo, "3");
				rowInfo.setAlign("center");
				DashboardTimelinePertemuan.displayInfoPertemuan(pertemuan).setParent(rowInfo);
			}

			MyFormRow rowUtamaTengah = new MyFormRow();
			ais.ui.util.ZkCompat.setSpans(rowUtamaTengah, "3");
			rowUtamaTengah.setStyle(style);
			rowUtamaTengah.setValign("top");
			rowUtamaTengah.setParent(rows);

			PertemuanPunyaDiskusi pertemuanPunyaDiskusi = new PertemuanPunyaDiskusi();
			pertemuanPunyaDiskusi.setPertemuan(pertemuan);
			pertemuanPunyaDiskusi.setParent(parent);
			pertemuanPunyaDiskusi.setMahasiswa(mahasiswa);
			pertemuanPunyaDiskusi.setDosen(dosen);
			pertemuanPunyaDiskusi.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
			pertemuanPunyaDiskusi.setTbmuser(tbmuser);

			EventListener eventListenerKomentarUtama = new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Integer ban = (Integer) (arg0 != null && arg0.getName().equals("tanggap") ? arg0.getData()
							: banyak);
					loadKomentarDetail(parent, lebarAwal, pertemuanPunyaDiskusisa, pertemuan, tabpanelUtama, style,
							mulai, ban, tampilanInfo, utama, eventListenerUtama, selectedDiskusi);
				}
			};

			PertemuanPunyaDiskusiHelper.displayRow(pertemuanPunyaDiskusi, pertemuanPunyaDiskusisa, rowUtamaTengah,
					mahasiswa, biodataCalonMahasiswa, dosen, true, eventListenerKomentarUtama, eventListenerUtama);

			utama.add(eventListenerKomentarUtama);
		}

		final List<Long> pertemuanPunyaDiskusis = pertemuan.ambilPertemuanPunyaDiskusi(parent, pertemuanPunyaDiskusisa,
				mulai, banyak);

		// OPTIMASI MEMORI: Bulk Fetch Data Diskusi
		Map<Long, PertemuanPunyaDiskusi> mapDiskusi = new java.util.HashMap<Long, PertemuanPunyaDiskusi>();
		if (pertemuanPunyaDiskusis != null && !pertemuanPunyaDiskusis.isEmpty()) {
			Session bulkSession = null;
			try {
				bulkSession = HibernateUtil.getSessionFactory().openSession();
				@SuppressWarnings("unchecked")
				List<PertemuanPunyaDiskusi> listDataDiskusi = bulkSession.createCriteria(PertemuanPunyaDiskusi.class)
						.add(Restrictions.in("id", pertemuanPunyaDiskusis)).list();
				for (PertemuanPunyaDiskusi d : listDataDiskusi)
					mapDiskusi.put(d.getId(), d);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5947");
			} finally {
				if (bulkSession != null) {
					try {
						bulkSession.clear();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5952");
					}
					try {
						bulkSession.disconnect();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5956");
					}
					try {
						ais.common.ElearningSessionUtil.closeQuietly(bulkSession);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:5960");
					}
				}
			}
		}

		Row rowUtama = null;
		for (final Long pertemuanPunyaDiskusiId : pertemuanPunyaDiskusis) {
			if (pertemuanPunyaDiskusiId == null)
				continue;

			final PertemuanPunyaDiskusi pertemuanPunyaDiskusi = mapDiskusi.get(pertemuanPunyaDiskusiId);
			if (pertemuanPunyaDiskusi == null)
				continue;

			rowUtama = new MyFormRow();
			rowUtama.setValign("top");
			rowUtama.setStyle(style);
			rowUtama.setParent(rows);

			if (selectedDiskusi != null && selectedDiskusi.equals(pertemuanPunyaDiskusiId)) {
				final Row myRowSelected = rowUtama;
				Common.createDefaultTimerNoBusy(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Clients.scrollIntoView(myRowSelected);
						Common.createDefaultTimerNoBusy(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								myRowSelected.setStyle(
										"font-size:10px;font-weight: bolder;text-decoration: underline;color:red;background: yellow;");
							}
						});
					}
				});
			}

			if (pertemuanPunyaDiskusi.getParent() == null || pertemuanPunyaDiskusi.getParent().getParent() == null) {
				MyDetail detailKomentar = new MyDetail();
				detailKomentar.setOpen(true);
				detailKomentar.setParent(rowUtama);

				loadKomentarDetail(pertemuanPunyaDiskusi, lebarAwal, pertemuanPunyaDiskusisa, pertemuan, detailKomentar,
						style, 0, banyak, tampilanInfo, utama, eventListenerUtama, selectedDiskusi);
			} else {
				new Label().setParent(rowUtama);
			}

			new Label().setParent(rowUtama);

			Hbox tombol = PertemuanPunyaDiskusiHelper.displayRow(pertemuanPunyaDiskusi, pertemuanPunyaDiskusisa,
					rowUtama, mahasiswa, biodataCalonMahasiswa, dosen, false, null, eventListenerUtama);

			Pertemuan objPertemuan = pertemuanPunyaDiskusi.getPertemuan();
			boolean isKomentarTutup = (objPertemuan != null && objPertemuan.getKomentarDitutup() != null
					&& objPertemuan.getKomentarDitutup());

			if (tbmuser != null && !isKomentarTutup) {
				final MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(
						pertemuanPunyaDiskusi.getParent() == null ? "Balas" : "Tanggapi", "");
				toolbarbutton.setStyle("font-size:9px;color: #0000EE;text-decoration: underline;");
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

						PertemuanPunyaDiskusi ppdFresh = (PertemuanPunyaDiskusi) GeneralValueObject
								.ambilData(PertemuanPunyaDiskusi.class, pertemuanPunyaDiskusiId.toString());

						PertemuanPunyaDiskusiHelper.onAddKomentar(ppdFresh, baru, pertemuanPunyaDiskusisa, mahasiswa,
								dosen, biodataCalonMahasiswa, siswa, calonSiswa, new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										PertemuanPunyaDiskusi a = (PertemuanPunyaDiskusi) arg0.getData();
										if (a != null && a.getId() != null) {
											pertemuanPunyaDiskusisa.add(a.getId());
											if (toolbarbutton.getLabel().equalsIgnoreCase("Tanggapi")) {
												if (!utama.isEmpty())
													utama.get(0).onEvent(new Event("tanggap", null, 10000));
											} else {
												loadKomentarDetail(parent, lebarAwal, pertemuanPunyaDiskusisa,
														pertemuan, tabpanelUtama, style, mulai, banyak, tampilanInfo,
														utama, eventListenerUtama, selectedDiskusi);
											}
										}
									}
								});
					}
				});

				boolean boleh = false;
				if (mahasiswa != null && mahasiswa.getId() != null && pertemuanPunyaDiskusi.getMahasiswa() != null)
					boleh |= mahasiswa.getId().equals(pertemuanPunyaDiskusi.getMahasiswa().getId());
				if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null
						&& pertemuanPunyaDiskusi.getBiodataCalonMahasiswa() != null)
					boleh |= biodataCalonMahasiswa.getId()
							.equals(pertemuanPunyaDiskusi.getBiodataCalonMahasiswa().getId());
				if (siswa != null && siswa.getId() != null && pertemuanPunyaDiskusi.getSiswa() != null)
					boleh |= siswa.getId().equals(pertemuanPunyaDiskusi.getSiswa().getId());
				if (calonSiswa != null && calonSiswa.getId() != null && pertemuanPunyaDiskusi.getCalonSiswa() != null)
					boleh |= calonSiswa.getId().equals(pertemuanPunyaDiskusi.getCalonSiswa().getId());
				if (dosen != null && dosen.getId() != null && pertemuanPunyaDiskusi.getDosen() != null)
					boleh |= dosen.getId().equals(pertemuanPunyaDiskusi.getDosen().getId());
				if (guru != null && guru.getId() != null && pertemuanPunyaDiskusi.getGuru() != null)
					boleh |= guru.getId().equals(pertemuanPunyaDiskusi.getGuru().getId());
				if (tbmuser.getUserId() != null && pertemuanPunyaDiskusi.getTbmuser() != null)
					boleh |= tbmuser.getUserId().equals(pertemuanPunyaDiskusi.getTbmuser().getUserId());

				boolean bolehHapus = boleh || Common.getApakahAdmin() || (dosen != null && dosen.getId() != null);

				Toolbarbutton btnUbah = new MyToolbarbuttonConfig("Ubah");
				btnUbah.setStyle("font-size:9px;color: #0000EE;text-decoration: underline;");
				btnUbah.setVisible(boleh);
				btnUbah.setParent(tombol);

				btnUbah.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						PertemuanPunyaDiskusi ppdFresh = (PertemuanPunyaDiskusi) GeneralValueObject
								.ambilData(PertemuanPunyaDiskusi.class, pertemuanPunyaDiskusiId.toString());
						PertemuanPunyaDiskusiHelper.onAddKomentar(ppdFresh.getParent(), ppdFresh,
								pertemuanPunyaDiskusisa, mahasiswa, dosen, biodataCalonMahasiswa, siswa, calonSiswa,
								new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										PertemuanPunyaDiskusi hasilEdit = (PertemuanPunyaDiskusi) arg0.getData();
										if (hasilEdit != null && hasilEdit.getId() != null) {
											pertemuanPunyaDiskusisa.add(hasilEdit.getId());
											loadKomentarDetail(parent, lebarAwal, pertemuanPunyaDiskusisa, pertemuan,
													tabpanelUtama, style, mulai, banyak, tampilanInfo, utama,
													eventListenerUtama, selectedDiskusi);
										}
									}
								});
					}
				});

				Toolbarbutton btnHapus = new MyToolbarbuttonConfig("Hapus");
				btnHapus.setStyle("font-size:9px;color: #0000EE;text-decoration: underline;");
				btnHapus.setVisible(bolehHapus);
				btnHapus.setTooltiptext("Hapus Data");
				btnHapus.setParent(tombol);

				btnHapus.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											Session delSession = null;
											org.hibernate.Transaction tx = null;
											try {
												delSession = HibernateUtil.getSessionFactory().openSession();
												tx = delSession.beginTransaction();
												delSession.createSQLQuery(
														"delete from pertemuan_punya_diskusi where parent in (select id from pertemuan_punya_diskusi where parent="
																+ pertemuanPunyaDiskusiId + ")")
														.executeUpdate();
												delSession.createSQLQuery(
														"delete from pertemuan_punya_diskusi where parent="
																+ pertemuanPunyaDiskusiId)
														.executeUpdate();
												tx.commit();

												pertemuanPunyaDiskusisa.remove(pertemuanPunyaDiskusiId);
												Common.refreshDelete(pertemuanPunyaDiskusi);
												Common.createDefaultTimer(new EventListener() {
													@Override
													public void onEvent(Event arg0) throws Exception {
														loadKomentarDetail(parent, lebarAwal, pertemuanPunyaDiskusisa,
																pertemuan, tabpanelUtama, style, mulai, banyak,
																tampilanInfo, utama, eventListenerUtama,
																selectedDiskusi);
													}
												});
											} catch (Exception e) {
												if (tx != null)
													tx.rollback();
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig
														.show("Data ini tidak dapat dihapus. Error: " + e.getMessage());
											} finally {
												if (delSession != null) {
													try {
														delSession.clear();
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6159");
													}
													try {
														delSession.disconnect();
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6163");
													}
													try {
														ais.common.ElearningSessionUtil.closeQuietly(delSession);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6167");
													}
												}
											}
										}
									}
								});
					}
				});

				boolean isIzinkanUpload = (objPertemuan != null
						&& objPertemuan.getIzinkanUploadLampiranDiGrive() != null
						&& objPertemuan.getIzinkanUploadLampiranDiGrive());

				if (isIzinkanUpload && boleh) {
					EventListener uploadeventListener = new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {

							Session updateSession = null;
							org.hibernate.Transaction tx = null;
							try {
								updateSession = HibernateUtil.getSessionFactory().openSession();
								tx = updateSession.beginTransaction();

								PertemuanPunyaDiskusi ppdUpdate = (PertemuanPunyaDiskusi) updateSession
										.get(PertemuanPunyaDiskusi.class, pertemuanPunyaDiskusiId);
								if (ppdUpdate != null) {
									ppdUpdate.setDosen(dosen);
									ppdUpdate.setMahasiswa(mahasiswa);
									ppdUpdate.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
									Tbmuser currUser = Common.getCurrentUser();
									if (currUser != null && currUser.getUserPassword() != null
											&& !currUser.getUserPassword().trim().equals("")) {
										ppdUpdate.setTbmuser(currUser);
									}
									updateSession.update(ppdUpdate);
									pertemuanPunyaDiskusisa.add(ppdUpdate.getId());
								}
								tx.commit();
							} catch (Exception e) {
								if (tx != null)
									tx.rollback();
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6210");
							} finally {
								if (updateSession != null) {
									try {
										updateSession.clear();
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6215");
									}
									try {
										updateSession.disconnect();
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6219");
									}
									try {
										ais.common.ElearningSessionUtil.closeQuietly(updateSession);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6223");
									}
								}
							}

							LampiranLain copy = (LampiranLain) arg0.getData();
							if (copy != null) {
								Session streamSession = null;
								org.hibernate.Transaction stx = null;
								try {
									streamSession = ais.database.hibernate.StreamingHibernateUtil.getInstance()
											.getSessionFactory().openSession();
									stx = streamSession.beginTransaction();
									copy.setRef(pertemuanPunyaDiskusiId);
									streamSession.update(copy);
									stx.commit();
								} catch (Exception e) {
									if (stx != null)
										stx.rollback();
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6242");
								} finally {
									if (streamSession != null) {
										try {
											streamSession.clear();
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6247");
										}
										try {
											streamSession.disconnect();
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6251");
										}
										try {
											ais.common.ElearningSessionUtil.closeQuietly(streamSession);
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6255");
										}
									}
								}
							}

							CommonEmail.infoAdaDiskusiPerkuliahan(pertemuan, pertemuanPunyaDiskusi);
							loadKomentarDetail(parent, lebarAwal, pertemuanPunyaDiskusisa, pertemuan, tabpanelUtama,
									style, mulai, banyak, tampilanInfo, utama, eventListenerUtama, selectedDiskusi);
						}
					};

					Toolbarbutton upload = FileFotoLain.tampilkanTombolUploadGdrive(null, null, uploadeventListener,
							null, null, LampiranLain.DISKUSI, false, null, "Lampiran", null, false, Common.refSementara(),
							false, LampiranLain.class);
					upload.setImage("");
					upload.setLabel(Common.getBahasaConfig("Lampiran"));
					upload.setOrient("vertical");
					upload.setStyle("font-size:9px;color: #0000EE;text-decoration: underline;");
					tombol.appendChild(upload);
				}
			}
		}

		if (pertemuanPunyaDiskusis != null)
			pertemuanPunyaDiskusis.clear();
	}

	@SuppressWarnings("unchecked")
	public static Groupbox displayInfoPertemuan(Pertemuan pertemuan) throws Exception {
		Groupbox groupBox = new ais.ui.util.MyGroupboxStyled();
		if (pertemuan == null)
			return groupBox;

		boolean mobile = Common.isMobile();
		MyCaptionStyled label = new MyCaptionStyled(
				"Pertemuan ke " + pertemuan.getPertemuanKe() + ", " + pertemuan.info());
		label.setStyle("font-size:14px;font-weight: bolder;text-decoration: none;color:"
				+ pertemuan.warna().split(",")[0] + ";border: 1px solid " + pertemuan.warna().split(",")[0] + ";\r\n"
				+ "  padding: 5px;" + "  background-color: rgba(169,169,169,0.4);" + "  border-radius: 5px 15px;");

		groupBox.appendChild(label);
		groupBox.setWidth(mobile ? "92%" : "95%");
		Box v = null;

		Dosen dosenPengganti = null;
		Guru guruPengganti = null;
		try {
			if (pertemuan.getDosenPengganti() != null)
				dosenPengganti = (Dosen) ConstantValues.ambil(Dosen.class.getName(), pertemuan.getDosenPengganti());
			if (pertemuan.getGuruPengganti() != null)
				guruPengganti = (Guru) ConstantValues.ambil(Guru.class.getName(), pertemuan.getGuruPengganti());

			if (pertemuan.getPerkuliahan() != null) {
				if (mobile || pertemuan.getPerkuliahan().getJumlahDosen() > 2) {
					v = new Vbox();
					v.setParent(groupBox);
					ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahanUmum(v,
							pertemuan.getPerkuliahan(), false, false, dosenPengganti);
				} else {
					v = ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahanUmum(groupBox,
							pertemuan.getPerkuliahan(), false, false, dosenPengganti);
					v.appendChild(new Space());
				}
			} else if (pertemuan.getJadwalPelajaran() != null) {
				if (mobile) {
					v = new Vbox();
					v.setParent(groupBox);
					Common.displayGuruJadwalPelajaranUmum(v, pertemuan.getJadwalPelajaran(), false, false,
							guruPengganti);
				} else {
					v = Common.displayGuruJadwalPelajaranUmum(groupBox, pertemuan.getJadwalPelajaran(), false, false,
							guruPengganti);
					v.appendChild(new Space());
				}
			} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null) {
				List<CommonVO> dataDosen = pertemuan.getMahasiswaRequestTugasAkhir().dataDosen(true);
				Box hbox1 = new Vbox();
				hbox1.setParent(groupBox);

				v = MahasiswaRequestTugasAkhirAction.tampilkanInfoDosen(pertemuan.getMahasiswaRequestTugasAkhir(),
						false, dataDosen);
				v.setParent(hbox1);
				dataDosen = null;
				Box hbox = new Vbox();
				hbox.setParent(v);

				hbox.appendChild(new Space());
				hbox.appendChild(
						MahasiswaRequestTugasAkhirAction.tampilkanInfoJudul(pertemuan.getMahasiswaRequestTugasAkhir()));
				hbox1.appendChild(new Space());
				hbox1.appendChild(MahasiswaRequestTugasAkhirAction
						.tampilkanInfoMahasiswa(pertemuan.getMahasiswaRequestTugasAkhir(), null));
			} else if (pertemuan.getSkripsi() != null) {
				List<CommonVO> dataDosen = pertemuan.getSkripsi().dataDosen(true);
				Box hbox1 = new Vbox();
				hbox1.setParent(groupBox);

				v = SkripsiAction.tampilkanInfoDosen(pertemuan.getSkripsi(), false, false, dataDosen);
				v.setParent(hbox1);
				dataDosen = null;
				Box hbox = new Vbox();
				hbox.setParent(v);

				hbox.appendChild(new Space());
				hbox.appendChild(SkripsiAction.tampilkanJudul(pertemuan.getSkripsi()));
				hbox1.appendChild(new Space());
				hbox1.appendChild(SkripsiAction.tampilkanInfoMahasiswa(pertemuan.getSkripsi(), null));

			} else if (pertemuan.getKelompokKkn() != null) {
				List<Dosen> dosens = pertemuan.getKelompokKkn().populateDosenBuNama();
				Box hbox = new Vbox();
				hbox.setParent(groupBox);

				v = KelompokKknAction.tampilkanInfoDosen(pertemuan.getKelompokKkn(), false, false, dosens);
				v.setParent(hbox);
				dosens = null;
				hbox.appendChild(new Space());

				new Label(pertemuan.getKelompokKkn().getKkn() == null ? ""
						: pertemuan.getKelompokKkn().getKkn().getNama()).setParent(hbox);
				new Label(pertemuan.getKelompokKkn().getNama()).setParent(hbox);

			} else if (pertemuan.getKelompokPkl() != null) {
				List<Dosen> dosens = pertemuan.getKelompokPkl().populateDosenBuNama();
				Box hbox = new Vbox();
				hbox.setParent(groupBox);

				v = KelompokPklAction.tampilkanInfoDosen(pertemuan.getKelompokPkl(), false, false, dosens);
				v.setParent(hbox);
				dosens = null;
				hbox.appendChild(new Space());

				new Label(pertemuan.getKelompokPkl().getPkl() == null ? ""
						: pertemuan.getKelompokPkl().getPkl().getNama()).setParent(hbox);
				new Label(pertemuan.getKelompokPkl().getNama()).setParent(hbox);
			} else if (pertemuan.getFormulirKegiatan() != null) {
				Box hbox = new Hbox();
				if (mobile)
					hbox = new Vbox();
				hbox.setParent(groupBox);

				v = new Hbox();
				PerkuliahanUIHelper.displayDosen(v, pertemuan.getFormulirKegiatan().ambilDataDosens(), true);
				v.setParent(hbox);

				hbox.appendChild(new Space());
				new Label(pertemuan.getFormulirKegiatan().infoSimple()).setParent(hbox);
			} else if (pertemuan.getWisuda() != null) {
				Box hbox = new Hbox();
				if (mobile)
					hbox = new Vbox();
				hbox.setParent(groupBox);

				v = new Hbox();
				v.setParent(hbox);

				hbox.appendChild(new Space());
				new Label(pertemuan.getWisuda().infoSimple()).setParent(hbox);
			} else if (pertemuan.getKrsMahasiswa() != null) {
				Box hbox = new Hbox();
				if (mobile)
					hbox = new Vbox();
				hbox.setParent(groupBox);

				v = new Hbox();
				v.setParent(hbox);
				CommonMedia.tampilkanGambarKecil(pertemuan.getKrsMahasiswa().getDosenPa()).setParent(v);

				hbox.appendChild(new Space());
				hbox.appendChild(MonitorKRSMahasiswaAction.tampilkanInfoMahasiswa(pertemuan.getKrsMahasiswa()));

			} else if (pertemuan.getPertemuanPunyaGrupPertemuan() != null) {
				Box hbox = new Hbox();
				if (mobile)
					hbox = new Vbox();
				hbox.setParent(groupBox);

				v = new Hbox();
				v.setParent(hbox);
				CommonMedia
						.tampilkanGambarKecil(pertemuan.getPertemuanPunyaGrupPertemuan().getGrupPertemuan().getDosen())
						.setParent(v);

				hbox.appendChild(new Space());
				hbox.appendChild(
						GrupPertemuanAction.tampilkanInfoMahasiswa(pertemuan.getPertemuanPunyaGrupPertemuan()));

			} else if (pertemuan.getJadwalUjianPMB() != null) {
				Box hbox = new Hbox();
				if (mobile)
					hbox = new Vbox();
				hbox.setParent(groupBox);

				v = new Hbox();
				v.setParent(hbox);

				hbox.appendChild(new Space());
				hbox.appendChild(new Label(pertemuan.getJadwalUjianPMB().infoSimple()));

			} else if (pertemuan.getJadwalUjianPSB() != null) {
				Box hbox = new Hbox();
				if (mobile)
					hbox = new Vbox();
				hbox.setParent(groupBox);

				v = new Hbox();
				v.setParent(hbox);

				hbox.appendChild(new Space());
				hbox.appendChild(new Label(pertemuan.getJadwalUjianPSB().infoSimple()));
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		Vbox vbox = new Vbox();
		vbox.setParent(v);

		Tbmuser tbmuser = Common.getCurrentUser();

		if ((pertemuan != null && pertemuan.getPerkuliahan() != null
				&& pertemuan.getPerkuliahan().getKurikulum() != null
				&& pertemuan.getPerkuliahan().getKurikulum().apakahObe(pertemuan.getPerkuliahan().getTahunAjaran(),
						pertemuan.getPerkuliahan().getGanjilGenap()))) {
			RevisiHelper
					.createNewRevisi(Pertemuan.class, pertemuan, (pertemuan.getTanggal() == null ? "-"
							: (SmartDateTimeUtil.getDayString(pertemuan.getTanggal(), pertemuan.getWaktuMulai())
									+ Common.dateFormat6.get().format(pertemuan.getTanggal()))
									+ " "
									+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
											: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai())))
					.setParent(vbox);
		} else if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null) {

			Hbox hbox = new Hbox();
			vbox.appendChild(hbox);
			final Hbox data = new Hbox();
			hbox.appendChild(data);

			RevisiHelper
					.createNewRevisi(Pertemuan.class, pertemuan, (pertemuan.getTanggal() == null ? "-"
							: (SmartDateTimeUtil.getDayString(pertemuan.getTanggal(), pertemuan.getWaktuMulai())
									+ Common.dateFormat6.get().format(pertemuan.getTanggal()))
									+ " "
									+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
											: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai())))
					.setParent(data);

			final Hbox dataTime = new Hbox();
			hbox.appendChild(dataTime);

			final MyDatebox tanggal = new MyDatebox(pertemuan.getTanggal());
			final Timebox waktuMulai = new ais.ui.util.MyTimebox();
			waktuMulai.setFormat(Common.timeFormat.get().toPattern());
			final Timebox waktuSelesai = new ais.ui.util.MyTimebox();
			waktuSelesai.setFormat(Common.timeFormat.get().toPattern());

			tanggal.setCols(6);
			waktuMulai.setCols(1);
			waktuSelesai.setCols(1);

			dataTime.appendChild(tanggal);
			dataTime.appendChild(waktuMulai);
			dataTime.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
			dataTime.appendChild(waktuSelesai);

			tanggal.setReadonly(true);
			dataTime.setVisible(false);

			final Long idPer = pertemuan.getId();

			try {
				waktuMulai
						.setValue(pertemuan.getWaktuMulai() == null || pertemuan.getWaktuMulai().trim().isEmpty() ? null
								: Common.timeFormat2.get().parse(pertemuan.getWaktuMulai()));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6532");
			}
			try {
				waktuSelesai.setValue(
						pertemuan.getWaktuSelesai() == null || pertemuan.getWaktuSelesai().trim().isEmpty() ? null
								: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6538");
			}

			final MyHtmlIframe htlmKemampuanAkhir;
			vbox.appendChild(htlmKemampuanAkhir = new ais.ui.util.MyHtmlIframe(
					"<div style=\"font-size:14px;\"><u>Kemampuan / kompetensi yang ingin dicapai</u>:</div>"));
			htlmKemampuanAkhir.setVisible(false);

			String tpk = pertemuan.getTopik();
			List<String> urls = null;
			if (!tpk.trim().isEmpty()) {
				urls = Common.getUrls(tpk);
				tpk = tpk.replaceAll("\n", "<br>");
				for (String url : urls) {
					tpk = org.apache.commons.lang3.StringUtils.replace(tpk, url,
							"<a href='" + url + "' target='_blank'>" + url + "</a>");
				}
			}

			hbox = new Hbox();
			vbox.appendChild(hbox);
			final Html html;
			hbox.appendChild(html = new ais.ui.util.MyHtmlIframe("<div style=\"font-size:14px;\">" + tpk + "</div>"));

			final Textbox topik = new Textbox(pertemuan.getTopik());
			topik.setCols(mobile ? 30 : 50);
			topik.setRows(10);
			topik.setVisible(false);
			hbox.appendChild(topik);

			hbox = new Hbox();
			vbox.appendChild(hbox);

			final MyToolbarbuttonConfig buttonBatal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			final MyToolbarbuttonConfig buttonUbah = new MyToolbarbuttonConfig("Ubah", "/img/edit-icon.png");
			buttonUbah.setStyle("font-size:8px;");

			final MyToolbarbuttonConfig buttonSimpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			buttonSimpan.setStyle("font-size:8px;");
			buttonSimpan.setTooltiptext("Simpan Data");
			buttonSimpan.setVisible(false);
			buttonSimpan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, idPer.toString());

					html.setVisible(true);
					topik.setVisible(false);
					htlmKemampuanAkhir.setVisible(false);

					buttonSimpan.setVisible(false);
					buttonBatal.setVisible(false);
					buttonUbah.setVisible(true);

					dataTime.setVisible(false);
					data.setVisible(true);

					pertemuan.setTopik(topik.getValue());

					pertemuan.setTanggal(tanggal.getValue());
					pertemuan.setTanggalEdit(tanggal.getValue());
					pertemuan.setWaktuMulai(waktuMulai.getValue() == null ? null
							: Common.timeFormat2.get().format(waktuMulai.getValue()));
					pertemuan.setWaktuSelesai(waktuSelesai.getValue() == null ? null
							: Common.timeFormat2.get().format(waktuSelesai.getValue()));
					Common.refreshUpdate(pertemuan);

					String tpk = pertemuan.getTopik();
					List<String> urls = null;
					if (!tpk.trim().isEmpty()) {
						urls = Common.getUrls(tpk);
						tpk = tpk.replaceAll("\n", "<br>");
						for (String url : urls) {
							tpk = org.apache.commons.lang3.StringUtils.replace(tpk, url,
									"<a href='" + url + "' target='_blank'>" + url + "</a>");
						}
					}

					html.setContent("<div style=\"font-size:14px;\">" + tpk + "</div>");

					if (data != null) {

						Common.clear(data);

					}
					RevisiHelper
							.createNewRevisi(Pertemuan.class, pertemuan, (pertemuan.getTanggal() == null ? "-"
									: (SmartDateTimeUtil.getDayString(pertemuan.getTanggal(), pertemuan.getWaktuMulai())
											+ Common.dateFormat6.get().format(pertemuan.getTanggal()))
											+ " "
											+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null
													? ""
													: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai())))
							.setParent(data);
				}
			});
			buttonSimpan.setParent(hbox);

			buttonBatal.setStyle("font-size:8px;");
			buttonBatal.setTooltiptext("Batal Simpan Data");
			buttonBatal.setVisible(false);
			buttonBatal.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, idPer.toString());

					html.setVisible(true);
					topik.setVisible(false);
					htlmKemampuanAkhir.setVisible(false);

					buttonSimpan.setVisible(false);
					buttonBatal.setVisible(false);
					buttonUbah.setVisible(true);

					dataTime.setVisible(false);
					data.setVisible(true);

					html.setContent("<div style=\"font-size:14px;\">" + pertemuan.getTopik().replaceAll("\n", "<br>")
							+ "</div>");
					if (data != null) {
						Common.clear(data);
					}
					RevisiHelper
							.createNewRevisi(Pertemuan.class, pertemuan, (pertemuan.getTanggal() == null ? "-"
									: (SmartDateTimeUtil.getDayString(pertemuan.getTanggal(), pertemuan.getWaktuMulai())
											+ Common.dateFormat6.get().format(pertemuan.getTanggal()))
											+ " "
											+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null
													? ""
													: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai())))
							.setParent(data);
				}
			});
			buttonBatal.setParent(hbox);

			buttonUbah.setTooltiptext("Ubah Data");
			buttonUbah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					html.setVisible(false);
					topik.setVisible(true);
					htlmKemampuanAkhir.setVisible(true);

					buttonSimpan.setVisible(true);
					buttonBatal.setVisible(true);
					buttonUbah.setVisible(false);

					dataTime.setVisible(true);
					data.setVisible(false);
				}
			});
			buttonUbah.setParent(hbox);
		} else {
			RevisiHelper
					.createNewRevisi(Pertemuan.class, pertemuan, (pertemuan.getTanggal() == null ? "-"
							: (SmartDateTimeUtil.getDayString(pertemuan.getTanggal(), pertemuan.getWaktuMulai())
									+ Common.dateFormat6.get().format(pertemuan.getTanggal()))
									+ " "
									+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
											: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai())))
					.setParent(vbox);

			String tpk = pertemuan.getTopik();
			List<String> urls = null;
			if (!tpk.trim().isEmpty()) {
				urls = Common.getUrls(tpk);
				tpk = tpk.replaceAll("\n", "<br>");
				for (String url : urls) {
					tpk = org.apache.commons.lang3.StringUtils.replace(tpk, url,
							"<a href='" + url + "' target='_blank'>" + url + "</a>");
				}
			}

			vbox.appendChild(new ais.ui.util.MyHtmlIframe("<div style=\"font-size:14px;\">" + tpk + "</div>"));
		}

		if (pertemuan.getPerkuliahan() != null) {
			vbox.appendChild(new MyLabelKecil(pertemuan.getPerkuliahan().infoSimple(dosenPengganti)));
		} else if (pertemuan.getJadwalPelajaran() != null) {
			vbox.appendChild(new MyLabelKecil(pertemuan.getJadwalPelajaran().info(guruPengganti)));
		} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null) {
			vbox.appendChild(MahasiswaRequestTugasAkhirAction
					.tampilkanInfoDosenSimple(pertemuan.getMahasiswaRequestTugasAkhir()));
		} else if (pertemuan.getSkripsi() != null) {
			vbox.appendChild(SkripsiAction.tampilkanInfoDosenSimple(pertemuan.getSkripsi()));
		} else if (pertemuan.getKelompokKkn() != null) {
			String dsn = "";
			for (Dosen dosen : pertemuan.getKelompokKkn().populateDosenBuNama())
				dsn += dsn.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
			vbox.appendChild(new MyLabelAgakKecil("Dosen Pembimbing : " + dsn));
		} else if (pertemuan.getKelompokPkl() != null) {
			String dsn = "";
			for (Dosen dosen : pertemuan.getKelompokPkl().populateDosenBuNama())
				dsn += dsn.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
			vbox.appendChild(new MyLabelAgakKecil("Dosen Pembimbing : " + dsn));
		} else if (pertemuan.getKrsMahasiswa() != null && pertemuan.getKrsMahasiswa().getDosenPa() != null) {
			new Label("Dosen PA : " + pertemuan.getKrsMahasiswa().getDosenPa().getNama()).setParent(vbox);
		} else if (pertemuan.getPertemuanPunyaGrupPertemuan() != null
				&& pertemuan.getPertemuanPunyaGrupPertemuan().getGrupPertemuan() != null
				&& pertemuan.getPertemuanPunyaGrupPertemuan().getGrupPertemuan().getDosen() != null) {
			new Label("Dosen konsultasi: "
					+ pertemuan.getPertemuanPunyaGrupPertemuan().getGrupPertemuan().getDosen().getNama())
					.setParent(vbox);
		} else if (pertemuan.getJadwalUjianPMB() != null) {
			JadwalUjianPMB jadwalUjianPMB = pertemuan.getJadwalUjianPMB();
			Session session = null;
			List<String> ruangPMBsTemorary = new ArrayList<String>();
			try {
				if (!jadwalUjianPMB.getRuanganYgIkut().isEmpty()) {
					session = HibernateUtil.getSessionFactory().openSession();
					ruangPMBsTemorary = session.createCriteria(RuangPMB.class)
							.setProjection(Projections.property("kodeRuangan"))
							.add(Restrictions
									.sqlRestriction("this_.id in (-1" + jadwalUjianPMB.getRuanganYgIkut() + "-1)"))
							.list();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6757");
			} finally {
				if (session != null) {
					try {
						session.clear();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6762");
					}
					try {
						session.disconnect();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6766");
					}
					try {
						closeHibernateSessionQuietly(session);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6770");
					}
				}
			}

			String ruang = "<ul style='font-size:9px'>";
			for (String ruangPMB : ruangPMBsTemorary)
				ruang += "<li>" + ruangPMB + "</li>";
			new ais.ui.util.MyHtmlIframe(ruang + "</ul>").setParent(vbox);
			ruangPMBsTemorary = null;

			new Label(jadwalUjianPMB.getKeterangan()).setParent(vbox);
		}

		return groupBox;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Map pulihkanRincianObePertemuan(Long kpmId, int mingguKe) {
		if (kpmId == null || mingguKe <= 0) {
			return null;
		}
		Session session = null;
		org.hibernate.Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			KurikulumPunyaMatakuliah kpm = (KurikulumPunyaMatakuliah) session.get(KurikulumPunyaMatakuliah.class, kpmId);
			if (kpm == null) {
				return null;
			}
			JSONObject rincian;
			try {
				rincian = new JSONObject(kpm.getRincian());
			} catch (Exception e) {
				rincian = new JSONObject();
			}
			Map map = kpm.ambilRinci(rincian, mingguKe);
			if (map != null) {
				return map;
			}

			JSONObject sumber = null;
			int jarakTerdekat = Integer.MAX_VALUE;
			Iterator<String> keys = rincian.keys();
			while (keys.hasNext()) {
				try {
					JSONObject kandidat = rincian.getJSONObject(keys.next());
					if (kandidat == null || kandidat.isNull("mulaiMingguKe")) {
						continue;
					}
					int mulai = kandidat.optInt("mulaiMingguKe", mingguKe);
					int jarak = Math.abs(mulai - mingguKe);
					if (jarak < jarakTerdekat) {
						jarakTerdekat = jarak;
						sumber = kandidat;
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-recovery RPS OBE: gagal membaca kandidat rincian");
				}
			}

			JSONObject baru = sumber == null ? new JSONObject() : new JSONObject(sumber.toString());
			baru.put("mulaiMingguKe", mingguKe);
			baru.put("sampaiMingguKe", mingguKe);
			if (baru.isNull("jumlahCpmk")) {
				baru.put("jumlahCpmk", 1);
			}
			if (baru.isNull("sub_cpmk") || (baru.optString("sub_cpmk", "").trim().isEmpty())) {
				baru.put("sub_cpmk", "-1");
			}
			if (baru.isNull("indikator")) {
				baru.put("indikator", "");
			}
			if (baru.isNull("teknikDanKriteria")) {
				baru.put("teknikDanKriteria", "");
			}
			if (baru.isNull("metodePembelajaran")) {
				baru.put("metodePembelajaran", Common.getBahasaConfig("Project Based Learning atau lainnya .."));
			}
			if (baru.isNull("pembelajaranLuring")) {
				baru.put("pembelajaranLuring", "");
			}
			if (baru.isNull("pembelajaranDaring")) {
				baru.put("pembelajaranDaring", "");
			}
			if (baru.isNull("bahanKajians")) {
				baru.put("bahanKajians", new JSONObject());
			}
			if (baru.isNull("pustakaUtamas")) {
				baru.put("pustakaUtamas", new JSONObject());
			}
			if (baru.isNull("pustakaPendukungs")) {
				baru.put("pustakaPendukungs", new JSONObject());
			}
			baru.put("autoGenerated", true);
			baru.put("autoGeneratedNote", "Dibuat otomatis saat pertemuan ke-" + mingguKe
					+ " dibuka karena rincian RPS OBE belum ditemukan.");
			rincian.put(Common.getGeneratedBarCode(15), baru);
			kpm.setRincian(rincian.toString());
			session.update(kpm);
			tx.commit();
			tx = null;
			return kpm.ambilRinci(rincian, mingguKe);
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception ignored) {
				}
			}
			Common.tampilErrorJikaAdmin(e);
			return null;
		} finally {
			if (session != null) {
				try {
					session.clear();
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-recovery RPS OBE: gagal clear session");
				}
				try {
					session.disconnect();
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-recovery RPS OBE: gagal disconnect session");
				}
				try {
					closeHibernateSessionQuietly(session);
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-recovery RPS OBE: gagal close session");
				}
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void displayCatatan(Component vboxUtama, final EventListener eventListener, final Pertemuan pertemuan,
			Tbmuser tbmuser, boolean mobile) {
		List<String> urls = null;
		if (pertemuan != null && pertemuan.getPerkuliahan() != null
				&& pertemuan.getPerkuliahan().getKurikulumPunyaMatakuliah() != null
				&& pertemuan.getPerkuliahan().getKurikulum() != null
				&& pertemuan.getPerkuliahan().getKurikulum().apakahObe(pertemuan.getPerkuliahan().getTahunAjaran(),
						pertemuan.getPerkuliahan().getGanjilGenap())) {
			// KE-9: displayCatatan dapat dipanggil dari THREAD LATAR (lihat TampilanELearningAction)
			// di mana proxy KurikulumPunyaMatakuliah dari graf 'pertemuan' belum ter-inisialisasi
			// dan TIDAK ada Session aktif. getRincian()/ambilRinci() (yang di dalamnya menavigasi
			// relasi lazy spt getMatakuliah().getCapaianPembelajaranLulusan()) → LazyInitializationException
			// "could not initialize proxy - no Session". Solusi: muat instance TERKELOLA via session
			// khusus yang tetap terbuka selama perhitungan rinci, lalu ditutup di finally
			// (clear/disconnect/close). Bila proxy memang sudah ter-inisialisasi (ada session, mis.
			// dipanggil dari request UI biasa), pakai instance asal apa adanya — perilaku lama terjaga.
			JSONObject jsonArraykurikulumPunyaMatakuliah = new JSONObject();
			Map map = null;
			boolean kpmNilaiMenggunakanCpmk = true;
			KurikulumPunyaMatakuliah kpmRef = pertemuan.getPerkuliahan().getKurikulumPunyaMatakuliah();
			Session sessionRinci = null;
			try {
				KurikulumPunyaMatakuliah kpmRinci = kpmRef;
				if (kpmRef != null && kpmRef.getId() != null && !org.hibernate.Hibernate.isInitialized(kpmRef)) {
					sessionRinci = HibernateUtil.getSessionFactory().openSession();
					KurikulumPunyaMatakuliah kpmManaged = (KurikulumPunyaMatakuliah) sessionRinci
							.get(KurikulumPunyaMatakuliah.class, kpmRef.getId());
					if (kpmManaged != null) {
						kpmRinci = kpmManaged;
					}
				}
				if (kpmRinci != null) {
					try {
						jsonArraykurikulumPunyaMatakuliah = new JSONObject(kpmRinci.getRincian());
					} catch (Exception e) {
						jsonArraykurikulumPunyaMatakuliah = new JSONObject();
					}
					map = kpmRinci.ambilRinci(jsonArraykurikulumPunyaMatakuliah, pertemuan.getPertemuanKe());
					if (map == null && kpmRinci.getId() != null) {
						map = pulihkanRincianObePertemuan(kpmRinci.getId(), pertemuan.getPertemuanKe());
					}
					try {
						kpmNilaiMenggunakanCpmk = kpmRinci.getNilaiMenggunakanCpmk();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6828");
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (sessionRinci != null) {
					try {
						sessionRinci.clear();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6837");
					}
					try {
						sessionRinci.disconnect();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6841");
					}
					try {
						closeHibernateSessionQuietly(sessionRinci);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6845");
					}
				}
			}
			if (map != null) {
				try {
					JSONObject subCpmk = (JSONObject) map.get("subCpmk");
					CapaianPembelajaranLulusan capaianPembelajaranLulusanData = (CapaianPembelajaranLulusan) map
							.get("capaianPembelajaranLulusanData");
					if (subCpmk != null && capaianPembelajaranLulusanData != null) {
						JSONObject jsonObject = (JSONObject) map.get("jsonObject");
						Session session = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();
							List<CapaianLulusan> capaianLulusans = ConstantValues.simpleList(session
									.createCriteria(CapaianLulusan.class)
									.add(Restrictions.ilike("capaianPembelajaranLulusan",
											"," + capaianPembelajaranLulusanData.getId() + ",", MatchMode.ANYWHERE)),
									CapaianLulusan.class);
							Set<Long> profiles = new HashSet<Long>();
							for (CapaianLulusan c : capaianLulusans) {
								for (String d : c.getProfil().split(",")) {
									// PENCEGAHAN NumberFormatException (mis. data lama/salah format spt "3_1793"):
									// validasi dulu bahwa token benar-benar angka murni SEBELUM parseLong dipanggil,
									// supaya exception-nya tidak pernah tercipta sama sekali (bukan sekadar
									// ditangkap) -- token yang tidak valid dilewati diam-diam tanpa membanjiri
									// audit log, persis seperti perilaku lama (baris tsb tetap tidak ikut dihitung).
									String dTrim = d == null ? "" : d.trim();
									if (!dTrim.isEmpty() && dTrim.matches("\\d+")) {
										profiles.add(Long.parseLong(dTrim));
									}
								}
							}

							Set<Long> longsProfile = new HashSet<Long>();
							for (String d : pertemuan.getPerkuliahan().getMatakuliah().getProfilLulusan().split(",")) {
								String dTrim = d == null ? "" : d.trim();
								if (!dTrim.isEmpty() && dTrim.matches("\\d+")) {
									longsProfile.add(Long.parseLong(dTrim));
								}
							}

							List<ProfilLulusan> profilLulusans = ConstantValues
									.simpleList(session.createCriteria(ProfilLulusan.class)
											.add(longsProfile.isEmpty() ? Restrictions.sqlRestriction("false")
													: Restrictions.in("id", longsProfile))
											.add(profiles.isEmpty() ? Restrictions.sqlRestriction("false")
													: Restrictions.in("id", profiles)),
											ProfilLulusan.class);

							vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe("<div style=\"font-size:14px;\"><u>"
									+ Common.getBahasaConfig("Profil Lulusan (PL)") + "</u>:</div>"));
							String tpk = "<ol>";
							for (ProfilLulusan c : profilLulusans) {
								String t = c.getKode() + " " + c.getNama();
								tpk += "<li>" + t + "</li>";
							}
							tpk += "</ol>";
							vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe(
									"<div style='font-size:12px;'>" + (tpk.trim().equalsIgnoreCase("<ol></ol>")
											? "<i style='color:#888;'>Profil Lulusan (PL) belum ditentukan untuk program studi ini.</i>"
											+ "<div style='margin-top:6px;background:#fff8e1;border-left:3px solid #ffc107;padding:7px 10px;border-radius:3px;font-size:11px;line-height:1.8;'>"
											+ "<b>Cara mengisi:</b><br>"
											+ "1. Buka menu <b>Kurikulum OBE</b> &rsaquo; <b>Profil Lulusan</b>.<br>"
											+ "2. Klik tombol <b>Tambah</b>, isi kode dan nama PL, lalu simpan.<br>"
											+ "3. Muat ulang halaman ini untuk memverifikasi."
											+ "</div>"
											: tpk) + "</div>"));
											// Tombol pintas: buka layar entri datanya langsung dari peringatan ini,
											// supaya pengguna tidak perlu menelusuri menu secara manual.
											if (tpk.trim().equalsIgnoreCase("<ol></ol>")) {
												tambahTombolTambahDataObe(vboxUtama, "Profil Lulusan", "pages/master/obe/profil_lulusan.zul");
											}

							vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe("<div style=\"font-size:14px;\"><u>"
									+ Common.getBahasaConfig("Capaian Lulusan (CPL)") + "</u>:</div>"));

							tpk = "<ol>";
							for (CapaianLulusan c : capaianLulusans) {
								String t = c.getKode() + " " + c.getNama();
								tpk += "<li>" + t + "</li>";
							}
							tpk += "</ol>";
							vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe("<div style='font-size:12px;'>"
									+ (tpk.trim().equalsIgnoreCase("<ol></ol>")
											? "<i style='color:#888;'>Capaian Pembelajaran Lulusan (CPL) belum ditentukan untuk program studi ini.</i>"
											+ "<div style='margin-top:6px;background:#fff8e1;border-left:3px solid #ffc107;padding:7px 10px;border-radius:3px;font-size:11px;line-height:1.8;'>"
											+ "<b>Cara mengisi:</b><br>"
											+ "1. Buka menu <b>Kurikulum OBE</b> &rsaquo; <b>CPL (Capaian Lulusan)</b>.<br>"
											+ "2. Tambahkan data CPL dan pastikan sudah terhubung ke program studi.<br>"
											+ "3. Simpan, lalu muat ulang halaman ini."
											+ "</div>"
											: tpk)
									+ "</div>"));
									// Tombol pintas: buka layar entri datanya langsung dari peringatan ini,
									// supaya pengguna tidak perlu menelusuri menu secara manual.
									if (tpk.trim().equalsIgnoreCase("<ol></ol>")) {
										tambahTombolTambahDataObe(vboxUtama, "Capaian Lulusan (CPL)", "pages/master/obe/capaian_lulusan.zul");
									}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6923");
						} finally {
							if (session != null) {
								try {
									session.clear();
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6928");
								}
								try {
									session.disconnect();
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6932");
								}
								try {
									closeHibernateSessionQuietly(session);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:6936");
								}
							}
						}

						vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe("<div style=\"font-size:14px;\"><u>"
								+ Common.getBahasaConfig("Capaian Pembelajaran Mata Kuliah (CPMK)") + "</u>:</div>"));

						String kodeCpmk = capaianPembelajaranLulusanData.getId() == null ? ""
								: (capaianPembelajaranLulusanData.getKode() == null ? ""
										: capaianPembelajaranLulusanData.getKode());
						String namaCpmk = capaianPembelajaranLulusanData.getId() == null
								? "<i style='color:#f59e0b;'>CPMK belum dipilih &mdash; silakan edit RPS OBE dan pilih ulang Sub-CPMK pada baris ini.</i>"
								: (capaianPembelajaranLulusanData.getNama() == null ? ""
										: capaianPembelajaranLulusanData.getNama());
						vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe(
								"<div style=\"font-size:12px;\">" + kodeCpmk + " " + namaCpmk + "</div>"));

						if (!kpmNilaiMenggunakanCpmk) {
							vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe("<div style=\"font-size:14px;\"><u>"
									+ Common.getBahasaConfig("Kemampuan akhir tiap tahapan belajar (Sub-CPMK)")
									+ "</u>:</div>"));

							String kodeSubCpmk = subCpmk.isNull("kode") ? "" : subCpmk.getString("kode");
							String namaSubCpmk = subCpmk.isNull("nama") ? "Belum ditentukan"
									: subCpmk.getString("nama");
							vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe(
									"<div style=\"font-size:12px;\">" + kodeSubCpmk + " " + namaSubCpmk + "</div>"));
						}

						vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe("<div style=\"font-size:14px;\"><u>"
								+ Common.getBahasaConfig("Indikator") + "</u>:</div>"));

						String indikator = jsonObject.isNull("indikator") ? "" : jsonObject.getString("indikator");
						vboxUtama.appendChild(
								new ais.ui.util.MyHtmlIframe("<div style=\"font-size:12px;\">" + indikator + "</div>"));

						vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe("<div style=\"font-size:14px;\"><u>"
								+ Common.getBahasaConfig("Teknik & Kriteria") + "</u>:</div>"));

						String teknikDanKriteria = jsonObject.isNull("teknikDanKriteria") ? ""
								: jsonObject.getString("teknikDanKriteria");
						vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe(
								"<div style=\"font-size:12px;\">" + teknikDanKriteria + "</div>"));

						vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe("<div style=\"font-size:14px;\"><u>"
								+ Common.getBahasaConfig("Metode Pembelajaran") + "</u>:</div>"));

						String metodePembelajaran = jsonObject.isNull("metodePembelajaran")
								? "Project based learning atau lainnya .."
								: jsonObject.getString("metodePembelajaran");
						vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe(
								"<div style=\"font-size:12px;\">" + metodePembelajaran + "</div>"));

						vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe("<div style=\"font-size:14px;\"><u>"
								+ Common.getBahasaConfig("Pembelajaran Luring") + "</u>:</div>"));
						String pembelajaranLuring = jsonObject.isNull("pembelajaranLuring") ? ""
								: jsonObject.getString("pembelajaranLuring");
						vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe(
								"<div style=\"font-size:12px;\">" + pembelajaranLuring + "</div>"));

						vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe("<div style=\"font-size:14px;\"><u>"
								+ Common.getBahasaConfig("Pembelajaran Daring") + "</u>:</div>"));
						String pembelajaranDaring = jsonObject.isNull("pembelajaranDaring") ? ""
								: jsonObject.getString("pembelajaranDaring");
						vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe(
								"<div style=\"font-size:12px;\">" + pembelajaranDaring + "</div>"));

						vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe("<div style=\"font-size:14px;\"><u>"
								+ Common.getBahasaConfig("Bahan Kajian: Materi Pembelajaran") + "</u>:</div>"));

						JSONObject bahanKajians = jsonObject.isNull("bahanKajians") ? new JSONObject()
								: jsonObject.getJSONObject("bahanKajians");
						Iterator<String> pus = bahanKajians.keys();
						String tpk = "<ol>";
						while (pus.hasNext()) {
							try {
								String idBahan = pus.next();
								JSONObject p = bahanKajians.isNull(idBahan) ? new JSONObject()
										: bahanKajians.getJSONObject(idBahan);
								tpk += "<li>" + p.getString("nama") + "</li>";
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7012");
							}
						}
						tpk += "</ol>";

						vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe(
								"<div style='font-size:12px;'>" + (tpk.trim().equalsIgnoreCase("<ol></ol>")
										? "<i style='color:#888;'>Bahan kajian / materi pembelajaran belum ditentukan untuk pertemuan ini.</i>"
										+ "<div style='margin-top:6px;background:#fff8e1;border-left:3px solid #ffc107;padding:7px 10px;border-radius:3px;font-size:11px;line-height:1.8;'>"
										+ "<b>Cara mengisi:</b><br>"
										+ "1. Buka RPS OBE mata kuliah ini &rsaquo; tab <b>Rencana Mingguan Buku</b>.<br>"
										+ "2. Isi kolom <b>Bahan Kajian / Materi Pembelajaran</b> pada baris minggu yang sesuai.<br>"
										+ "3. Simpan, lalu muat ulang halaman ini."
										+ "</div>"
										: tpk) + "</div>"));
										// Tombol pintas: buka layar entri datanya langsung dari peringatan ini,
										// supaya pengguna tidak perlu menelusuri menu secara manual.
										if (tpk.trim().equalsIgnoreCase("<ol></ol>")) {
											tambahTombolTambahDataObe(vboxUtama, "Bahan Kajian", "pages/master/obe/bahan_kajian.zul");
										}

						vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe("<div style=\"font-size:14px;\"><u>"
								+ Common.getBahasaConfig("Pustaka") + "</u>:</div>"));
						JSONObject pustakaUtamas = jsonObject.isNull("pustakaUtamas") ? new JSONObject()
								: jsonObject.getJSONObject("pustakaUtamas");
						pus = pustakaUtamas.keys();
						tpk = "<ol>";
						while (pus.hasNext()) {
							try {
								String idBahan = pus.next();
								JSONObject p = pustakaUtamas.isNull(idBahan) ? new JSONObject()
										: pustakaUtamas.getJSONObject(idBahan);
								String url = "";
								try {
									ReferensiLulusan referensiLulusan = (ReferensiLulusan) ConstantValues
											.ambil(ReferensiLulusan.class.getName(), Long.parseLong(p.get("id") + ""));
									if (referensiLulusan != null) {
										LampiranLain lampiranLain = LampiranLain.ambil(referensiLulusan.getId(),
												ReferensiLulusan.class.getName());
										if (lampiranLain != null && lampiranLain.getId() != null) {
											String u = lampiranLain.createLinkUri();
											url = " <a href='" + u + "' target='_blank'>Link</a>";
										}
									}
									tpk += "<li>" + referensiLulusan.getNama() + url + "</li>";
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7046");
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7048");
							}
						}

						JSONObject pustakaPendukungs = jsonObject.isNull("pustakaPendukungs") ? new JSONObject()
								: jsonObject.getJSONObject("pustakaPendukungs");
						pus = pustakaPendukungs.keys();

						while (pus.hasNext()) {
							try {
								String idBahan = pus.next();
								JSONObject p = pustakaPendukungs.isNull(idBahan) ? new JSONObject()
										: pustakaPendukungs.getJSONObject(idBahan);
								String url = "";
								try {
									ReferensiLulusan referensiLulusan = (ReferensiLulusan) ConstantValues
											.ambil(ReferensiLulusan.class.getName(), Long.parseLong(p.get("id") + ""));
									if (referensiLulusan != null) {
										LampiranLain lampiranLain = LampiranLain.ambil(referensiLulusan.getId(),
												ReferensiLulusan.class.getName());
										if (lampiranLain != null && lampiranLain.getId() != null) {
											String u = lampiranLain.createLinkUri();
											url = " <a href='" + u + "' target='_blank'>Link</a>";
										}
									}
									tpk += "<li>" + referensiLulusan.getNama() + url + "</li>";
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7074");
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7076");
							}
						}

						tpk += "</ol>";

						vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe(
								"<div style='font-size:12px;'>" + (tpk.trim().equalsIgnoreCase("<ol></ol>")
										? "<i style='color:#888;'>Pustaka / referensi pembelajaran belum ditentukan untuk pertemuan ini.</i>"
										+ "<div style='margin-top:6px;background:#fff8e1;border-left:3px solid #ffc107;padding:7px 10px;border-radius:3px;font-size:11px;line-height:1.8;'>"
										+ "<b>Cara mengisi:</b><br>"
										+ "1. Buka RPS OBE &rsaquo; tab <b>Indikator &amp; Pustaka</b>.<br>"
										+ "2. Tambahkan referensi <b>Pustaka Utama</b> dan <b>Pustaka Pendukung</b>.<br>"
										+ "3. Pastikan pustaka sudah terhubung ke pertemuan/minggu yang sesuai.<br>"
										+ "4. Simpan, lalu muat ulang halaman ini."
										+ "</div>"
										: tpk) + "</div>"));
										// Tombol pintas: buka layar entri datanya langsung dari peringatan ini,
										// supaya pengguna tidak perlu menelusuri menu secara manual.
										if (tpk.trim().equalsIgnoreCase("<ol></ol>")) {
											tambahTombolTambahDataObe(vboxUtama, "Pustaka / Referensi", "pages/master/obe/referensi_lulusan.zul");
										}

						vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe("<div style=\"font-size:14px;\"><u>"
								+ Common.getBahasaConfig("Bobot kemampuan akhir tiap tahapan belajar (Bobot Sub-CPMK)")
								+ "</u>:</div>"));
						Double bobot = 0.0;

						if (!subCpmk.isNull("bobot")) {
							bobot = Double.parseDouble(subCpmk.get("bobot") + "");
						}
						vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe("<div style=\"font-size:12px;\">"
								+ Common.numberFormat.get().format(bobot) + "%" + "</div>"));
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7099");
				}
			} else {
				String _mingguKe = String.valueOf(pertemuan.getPertemuanKe());
				vboxUtama.appendChild(new ais.ui.util.MyHtmlIframe(
					"<div style='border-left:4px solid #dc3545;background:#fff5f5;padding:10px 14px;border-radius:4px;margin:4px 0;'>"
					+ "<div style='color:#dc3545;font-weight:bold;font-size:13px;'>&#9888; Rincian RPS OBE belum diisi untuk pertemuan ke-" + _mingguKe + "</div>"
					+ "<div style='color:#444;font-size:12px;margin-top:8px;line-height:1.8;'>"
					+ "<b>Apa artinya?</b> Sistem tidak menemukan data rencana materi OBE yang terhubung ke pertemuan ini. "
					+ "Data RPS OBE mungkin belum diisi atau belum disimpan dengan benar.<br><br>"
					+ "<b>Langkah yang perlu dilakukan dosen:</b><br>"
					+ "1. Buka halaman <b>Perkuliahan</b> &rsaquo; pilih mata kuliah ini.<br>"
					+ "2. Klik tab <b>CPMK/OBE</b> atau buka dari menu <b>RPS OBE</b>.<br>"
					+ "3. Pilih tab <b>Rencana Mingguan Buku</b> (bukan tab Rencana Agenda).<br>"
					+ "4. Cari baris <b>minggu ke-" + _mingguKe + "</b> &mdash; pastikan semua kolom wajib sudah diisi<br>"
					+ "&nbsp;&nbsp;&nbsp;(Sub-CPMK, Bahan Kajian, Metode Pembelajaran, Waktu, Indikator, dll.).<br>"
					+ "5. Jika baris minggu ke-" + _mingguKe + " belum ada, klik tombol <b>Tambah</b> dan isi datanya.<br>"
					+ "6. Klik <b>Simpan / Update</b> setelah selesai mengisi.<br>"
					+ "7. Muat ulang halaman pertemuan ini untuk memverifikasi sudah tidak ada pesan ini.<br><br>"
					+ "<b>Catatan:</b> Periksa apakah nomor minggu di RPS OBE sudah sesuai dengan pertemuan ke-" + _mingguKe + " ini.<br><br>"
					+ "<b>Jika masalah berlanjut, hubungi admin dengan menyertakan:</b><br>"
					+ "&bull; Nama mata kuliah &amp; kode &bull; Tahun akademik &amp; semester<br>"
					+ "&bull; Nomor pertemuan bermasalah (ke-" + _mingguKe + ") &bull; Screenshot pesan ini"
					+ "</div>"
					+ "</div>"));
			}

			String catat = pertemuan.getCatatan();
			urls = null;
			if (!catat.trim().isEmpty()) {
				urls = Common.getUrls(catat);
				catat = catat.replaceAll("\n", "<br>");
				for (String url : urls) {
					catat = org.apache.commons.lang3.StringUtils.replace(catat, url,
							"<a href='" + url + "' target='_blank'>" + url + "</a>");
				}
			}

			final MyHtmlIframe ct;
			vboxUtama.appendChild(ct = new ais.ui.util.MyHtmlIframe(
					"<div style=\"font-size:14px;\"><u>" + Common.getBahasaConfig("Catatan") + "</u>:</div>"));

			final MyHtmlIframe html;
			vboxUtama.appendChild(html = new ais.ui.util.MyHtmlIframe("<div style=\"font-size:12px;\">"
					+ (catat.trim().isEmpty() ? "<i>Tidak ada catatan</i>" : catat) + "</div>"));

			Hbox hbox = new Hbox();

			if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getCalonSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null) {
				final MyHtmlIframe ct1;
				vboxUtama.appendChild(ct1 = new ais.ui.util.MyHtmlIframe(
						"<div style=\"font-size:14px;\"><u>" + Common.getBahasaConfig("Catatan") + "</u>:</div>"));
				ct1.setVisible(false);

				final Textbox catatan = new Textbox(pertemuan.getCatatan());
				catatan.setWidth("99%");
				catatan.setRows(10);
				catatan.setVisible(false);
				vboxUtama.appendChild(catatan);

				catatan.addEventListener("onChange", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						pertemuan.setCatatan(catatan.getValue().trim());
					}
				});

				final MyLabelAgakKecil labelInfoCatat;
				(labelInfoCatat = new MyLabelAgakKecil(
						"*) Catatan juga bisa berisi link atau URL yang mengarah ke website, audio, video, atau file tertentu"))
						.setParent(vboxUtama);
				labelInfoCatat.setVisible(false);

				final MyToolbarbuttonKecilConfig buttonBatal = new MyToolbarbuttonKecilConfig("Batal",
						"/img/cancel.gif");
				final MyToolbarbuttonKecilConfig buttonUbah = new MyToolbarbuttonKecilConfig("Ubah",
						"/img/edit-icon.png");
				final MyToolbarbuttonKecilConfig buttonSimpan = new MyToolbarbuttonKecilConfig("Simpan",
						"/img/save.gif");

				buttonSimpan.setTooltiptext("Simpan Data");
				buttonSimpan.setVisible(false);
				buttonSimpan.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						html.setVisible(true);
						catatan.setVisible(false);
						labelInfoCatat.setVisible(false);
						ct.setVisible(true);
						ct1.setVisible(false);
						buttonSimpan.setVisible(false);
						buttonBatal.setVisible(false);
						buttonUbah.setVisible(true);

						pertemuan.setCatatan(catatan.getValue());
						Common.refreshUpdate(pertemuan);
						eventListener.onEvent(event);
					}
				});
				buttonSimpan.setParent(hbox);

				buttonBatal.setTooltiptext("Batal Simpan Data");
				buttonBatal.setVisible(false);
				buttonBatal.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						html.setVisible(true);
						catatan.setVisible(false);
						labelInfoCatat.setVisible(false);
						ct.setVisible(true);
						ct1.setVisible(false);
						buttonSimpan.setVisible(false);
						buttonBatal.setVisible(false);
						buttonUbah.setVisible(true);
					}
				});
				buttonBatal.setParent(hbox);

				buttonUbah.setTooltiptext("Ubah Data");
				buttonUbah.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						html.setVisible(false);
						catatan.setVisible(true);
						labelInfoCatat.setVisible(true);
						ct.setVisible(false);
						ct1.setVisible(true);
						buttonSimpan.setVisible(true);
						buttonBatal.setVisible(true);
						buttonUbah.setVisible(false);
					}
				});
				buttonUbah.setParent(hbox);
			}

			Row hboxCararan = tampilCatatan(vboxUtama, pertemuan, hbox, eventListener, html, tbmuser);

			if (urls != null && !urls.isEmpty()) {
				for (String u : urls) {
					MyFormRow rowLagi = new MyFormRow();
					rowLagi.setStyle("border:0px;background: transparent;");
					rowLagi.setParent(hboxCararan.getParent());
					Common.displayUrlContent(u, rowLagi);
				}
			}
			urls = null;

			ais.ui.util.MenuAksiBaris.pasang(hbox);
			ais.ui.util.MenuAksiBaris.pasang(hbox);
			hbox.setParent(vboxUtama);

		} else {

			final MyHtmlIframe bk;
			vboxUtama.appendChild(bk = new ais.ui.util.MyHtmlIframe(
					"<div style=\"font-size:14px;\"><u>" + Common.getBahasaConfig("Bahan Kajian") + "</u>:</div>"));

			String tpk = pertemuan.getBukuRujukan1();

			if (!tpk.trim().isEmpty()) {
				urls = Common.getUrls(tpk);
				tpk = tpk.replaceAll("\n", "<br>");
				for (String url : urls) {
					tpk = org.apache.commons.lang3.StringUtils.replace(tpk, url,
							"<a href='" + url + "' target='_blank'>" + url + "</a>");
				}
			}

			final Html htmlBukuRujukan1;
			vboxUtama.appendChild(htmlBukuRujukan1 = new ais.ui.util.MyHtmlIframe("<div style=\"font-size:12px;\">"
					+ (tpk.trim().isEmpty() ? "<i>" + Common.getBahasaConfig("Tidak ada bahan kajian") + "</i>" : tpk)
					+ "</div>"));

			final MyHtmlIframe metode;
			vboxUtama.appendChild(metode = new ais.ui.util.MyHtmlIframe("<div style=\"font-size:14px;\"><u>"
					+ Common.getBahasaConfig("Metode Pembelajaran") + "</u>:</div>"));

			String met = pertemuan.getMetodePembelajaran();
			urls = null;
			if (!met.trim().isEmpty()) {
				urls = Common.getUrls(met);
				met = met.replaceAll("\n", "<br>");
				for (String url : urls) {
					met = org.apache.commons.lang3.StringUtils.replace(met, url,
							"<a href='" + url + "' target='_blank'>" + url + "</a>");
				}
			}

			final Html htmlMetode;
			vboxUtama.appendChild(htmlMetode = new ais.ui.util.MyHtmlIframe("<div style=\"font-size:12px;\">"
					+ (met.trim().isEmpty() ? "<i>" + Common.getBahasaConfig("Tidak ada metode pembelajaran") + "</i>"
							: met)
					+ "</div>"));

			final MyHtmlIframe rf;
			vboxUtama.appendChild(rf = new ais.ui.util.MyHtmlIframe(
					"<div style=\"font-size:14px;\"><u>" + Common.getBahasaConfig("Referensi") + "</u>:</div>"));

			tpk = pertemuan.getBukuRujukan2();
			urls = null;
			if (!tpk.trim().isEmpty()) {
				urls = Common.getUrls(tpk);
				tpk = tpk.replaceAll("\n", "<br>");
				for (String url : urls) {
					tpk = org.apache.commons.lang3.StringUtils.replace(tpk, url,
							"<a href='" + url + "' target='_blank'>" + url + "</a>");
				}
			}

			final Html htmlBukuRujukan2;
			vboxUtama.appendChild(htmlBukuRujukan2 = new ais.ui.util.MyHtmlIframe("<div style=\"font-size:12px;\">"
					+ (tpk.trim().isEmpty() ? "<i>Tidak ada referensi</i>" : tpk) + "</div>"));

			String catat = pertemuan.getCatatan();
			urls = null;
			if (!catat.trim().isEmpty()) {
				urls = Common.getUrls(catat);
				catat = catat.replaceAll("\n", "<br>");
				for (String url : urls) {
					catat = org.apache.commons.lang3.StringUtils.replace(catat, url,
							"<a href='" + url + "' target='_blank'>" + url + "</a>");
				}
			}
			final MyHtmlIframe ct;
			vboxUtama.appendChild(ct = new ais.ui.util.MyHtmlIframe(
					"<div style=\"font-size:14px;\"><u>" + Common.getBahasaConfig("Catatan") + "</u>:</div>"));

			final MyHtmlIframe html;
			vboxUtama.appendChild(html = new ais.ui.util.MyHtmlIframe("<div style=\"font-size:12px;\">"
					+ (catat.trim().isEmpty() ? "<i>Tidak ada catatan</i>" : catat) + "</div>"));

			Hbox hbox = new Hbox();

			if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getCalonSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null) {

				final MyHtmlIframe bk1;
				vboxUtama.appendChild(bk1 = new ais.ui.util.MyHtmlIframe(
						"<div style=\"font-size:14px;\"><u>" + Common.getBahasaConfig("Bahan Kajian") + "</u>:</div>"));
				bk1.setVisible(false);

				final Textbox bukuRujukan1 = new Textbox(pertemuan.getBukuRujukan1());
				bukuRujukan1.setWidth("99%");
				bukuRujukan1.setRows(3);
				bukuRujukan1.setVisible(false);
				vboxUtama.appendChild(bukuRujukan1);

				final MyHtmlIframe metode1;
				vboxUtama.appendChild(metode1 = new ais.ui.util.MyHtmlIframe("<div style=\"font-size:14px;\"><u>"
						+ Common.getBahasaConfig("Metode Pembelajaran") + "</u>:</div>"));
				metode1.setVisible(false);

				final Textbox met1 = new Textbox(pertemuan.getMetodePembelajaran());
				met1.setWidth("99%");
				met1.setRows(3);
				met1.setVisible(false);
				vboxUtama.appendChild(met1);

				final MyHtmlIframe rf1;
				vboxUtama.appendChild(rf1 = new ais.ui.util.MyHtmlIframe(
						"<div style=\"font-size:14px;\"><u>" + Common.getBahasaConfig("Referensi") + "</u>:</div>"));
				rf1.setVisible(false);

				final Textbox bukuRujukan2 = new Textbox(pertemuan.getBukuRujukan2());
				bukuRujukan2.setWidth("99%");
				bukuRujukan2.setRows(3);
				bukuRujukan2.setVisible(false);
				vboxUtama.appendChild(bukuRujukan2);

				final MyHtmlIframe ct1;
				vboxUtama.appendChild(ct1 = new ais.ui.util.MyHtmlIframe(
						"<div style=\"font-size:14px;\"><u>" + Common.getBahasaConfig("Catatan") + "</u>:</div>"));
				ct1.setVisible(false);

				final Textbox catatan = new Textbox(pertemuan.getCatatan());
				catatan.setWidth("99%");
				catatan.setRows(10);
				catatan.setVisible(false);
				vboxUtama.appendChild(catatan);

				catatan.addEventListener("onChange", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						pertemuan.setCatatan(catatan.getValue().trim());
					}
				});

				final MyLabelAgakKecil labelInfoCatat;
				(labelInfoCatat = new MyLabelAgakKecil(
						"*) Catatan juga bisa berisi link atau URL yang mengarah ke website, audio, video, atau file tertentu"))
						.setParent(vboxUtama);
				labelInfoCatat.setVisible(false);

				final MyToolbarbuttonKecilConfig buttonBatal = new MyToolbarbuttonKecilConfig("Batal",
						"/img/cancel.gif");
				final MyToolbarbuttonKecilConfig buttonUbah = new MyToolbarbuttonKecilConfig("Ubah",
						"/img/edit-icon.png");
				final MyToolbarbuttonKecilConfig buttonSimpan = new MyToolbarbuttonKecilConfig("Simpan",
						"/img/save.gif");

				buttonSimpan.setTooltiptext("Simpan Data");
				buttonSimpan.setVisible(false);
				buttonSimpan.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						html.setVisible(true);
						catatan.setVisible(false);
						labelInfoCatat.setVisible(false);

						bk.setVisible(true);
						metode.setVisible(true);
						rf.setVisible(true);
						ct.setVisible(true);

						metode1.setVisible(false);
						met1.setVisible(false);
						bk1.setVisible(false);
						rf1.setVisible(false);
						ct1.setVisible(false);

						htmlBukuRujukan1.setVisible(true);
						htmlMetode.setVisible(true);
						bukuRujukan1.setVisible(false);

						htmlBukuRujukan2.setVisible(true);
						bukuRujukan2.setVisible(false);

						buttonSimpan.setVisible(false);
						buttonBatal.setVisible(false);
						buttonUbah.setVisible(true);

						pertemuan.setMetodePembelajaran(met1.getValue());
						pertemuan.setCatatan(catatan.getValue());
						pertemuan.setBukuRujukan1(bukuRujukan1.getValue());
						pertemuan.setBukuRujukan2(bukuRujukan2.getValue());

						Common.refreshUpdate(pertemuan);
						eventListener.onEvent(event);
					}
				});
				buttonSimpan.setParent(hbox);

				buttonBatal.setTooltiptext("Batal Simpan Data");
				buttonBatal.setVisible(false);
				buttonBatal.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						html.setVisible(true);
						catatan.setVisible(false);
						labelInfoCatat.setVisible(false);

						bk.setVisible(true);
						metode.setVisible(true);
						rf.setVisible(true);
						ct.setVisible(true);

						metode1.setVisible(false);
						met1.setVisible(false);
						bk1.setVisible(false);
						rf1.setVisible(false);
						ct1.setVisible(false);

						htmlBukuRujukan1.setVisible(true);
						htmlMetode.setVisible(true);
						bukuRujukan1.setVisible(false);

						htmlBukuRujukan2.setVisible(true);
						bukuRujukan2.setVisible(false);

						buttonSimpan.setVisible(false);
						buttonBatal.setVisible(false);
						buttonUbah.setVisible(true);
					}
				});
				buttonBatal.setParent(hbox);

				buttonUbah.setTooltiptext("Ubah Data");
				buttonUbah.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						html.setVisible(false);
						catatan.setVisible(true);
						labelInfoCatat.setVisible(true);

						bk.setVisible(false);
						metode.setVisible(false);
						rf.setVisible(false);
						ct.setVisible(false);

						metode1.setVisible(true);
						met1.setVisible(true);
						bk1.setVisible(true);
						rf1.setVisible(true);
						ct1.setVisible(true);

						htmlBukuRujukan1.setVisible(false);
						htmlMetode.setVisible(false);
						bukuRujukan1.setVisible(true);

						htmlBukuRujukan2.setVisible(false);
						bukuRujukan2.setVisible(true);

						buttonSimpan.setVisible(true);
						buttonBatal.setVisible(true);
						buttonUbah.setVisible(false);
					}
				});
				buttonUbah.setParent(hbox);
			}

			Row hboxCararan = tampilCatatan(vboxUtama, pertemuan, hbox, eventListener, html, tbmuser);

			if (urls != null && !urls.isEmpty()) {
				for (String u : urls) {
					MyFormRow rowLagi = new MyFormRow();
					rowLagi.setStyle("border:0px;background: transparent;");
					rowLagi.setParent(hboxCararan.getParent());
					Common.displayUrlContent(u, rowLagi);
				}
			}
			urls = null;
			ais.ui.util.MenuAksiBaris.pasang(hbox);
			hbox.setParent(vboxUtama);
		}
	}

	private static Row tampilCatatan(Component vboxUtama, final Pertemuan pertemuan, Hbox hbox,
			final EventListener eventListener, final Html html, Tbmuser tbmuser) {
		Row hboxCararan = Common.tampilanScroll1(vboxUtama);
		hboxCararan.getGrid().setWidth("98%");

		Hbox hbox1 = new Hbox();
		hbox1.setParent(hbox);
		LampiranLain.createDownloadUploadFileLain(hbox1, pertemuan.getId(), LampiranLain.CATATAN_PERKULIAHAN, "Catatan",
				false, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (pertemuan.getCatatan().isEmpty()) {
							pertemuan.setCatatan("Catatan terlampir");
							Common.refreshUpdate(pertemuan);
						}
						eventListener.onEvent(arg0);
					}
				}, null, false, false, false,
				tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
						&& tbmuser.getCalonSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null,
				null, false, false, hboxCararan);

		// RAPIKAN RATA KIRI: helper upload di atas (FileFotoLain.createDownloadUpload) membungkus
		// tombol "Upload Catatan" dengan Vbox/Hbox ber-hflex="1". SELAMA MASIH ADA hflex (termasuk
		// nilai "min"), ZK memaksa tabel-dalam z-hbox menjadi width:100% lalu membagi sisa ruang,
		// sehingga tombol tetap ter-sebar (Generate Catatan terdorong ke kanan) dan wadah Upload
		// memakai lebar tetap (mis. 146px). Karena itu hflex DIHAPUS TOTAL (null) — bukan "min" —
		// HANYA pada sub-pohon di call-site ini (BUKAN mengubah helper bersama yang dipakai halaman
		// lain). Tanpa flex sama sekali, z-hbox merender lebar-konten dan "Ubah / Upload Catatan /
		// Generate Catatan" menempel rapi di kiri seperti deretan tombol (Catatan/Ujian/...) di bawah.
		try {
			Object tombolAttr = hbox1.getAttribute("tombol");
			if (tombolAttr instanceof org.zkoss.zk.ui.HtmlBasedComponent) {
				org.zkoss.zk.ui.HtmlBasedComponent containerTombolCatatan = (org.zkoss.zk.ui.HtmlBasedComponent) tombolAttr;
				containerTombolCatatan.setHflex(null);
				containerTombolCatatan.setVflex(null);
				containerTombolCatatan.setWidth(null);
				org.zkoss.zk.ui.Component pembungkusFlex = containerTombolCatatan.getParent();
				if (pembungkusFlex instanceof org.zkoss.zk.ui.HtmlBasedComponent) {
					org.zkoss.zk.ui.HtmlBasedComponent pembungkus = (org.zkoss.zk.ui.HtmlBasedComponent) pembungkusFlex;
					pembungkus.setHflex(null);
					pembungkus.setVflex(null);
					pembungkus.setWidth(null);
				}
			}
			hbox1.setHflex(null);
			hbox1.setVflex(null);
			hbox1.setWidth(null);
		} catch (Exception eRapiKiri) {
			// Jangan hentikan render bila neutralisasi flex gagal — cukup catat bila admin.
			Common.tampilErrorJikaAdmin(eRapiKiri);
		}

		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null) {
			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Generate Catatan", "/img/svg/gear.svg");
			toolbarbutton.setStyle("font-size:9px");
			toolbarbutton.setParent(hbox);

			String d = "";
			for (Dosen dosen : pertemuan.ambilDosen())
				d += d.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();

			String p = "";
			for (Mahasiswa mahasiswa : pertemuan.ambilMahasiswa())
				p += p.isEmpty() ? mahasiswa.getNama() : ", " + mahasiswa.getNama();

			String alpa = "";
			String sakit = "";
			String izin = "";
			String hadir = "";
			String belum = "";
			String[] nilais = pertemuan.getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					if (nn.toLowerCase().endsWith("mahasiswa")) {
						String[] s = nn.split(",");
						Long formatId = null;
						try {
							formatId = Long.parseLong(s[0]);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7583");
						}
						if (formatId != null) {
							Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), formatId);
							String kode = s[2];
							if (kode.equalsIgnoreCase("A")) {
								if (mahasiswa != null)
									alpa += alpa.isEmpty() ? mahasiswa.getNama() : ", " + mahasiswa.getNama();
							} else if (kode.equalsIgnoreCase("S")) {
								if (mahasiswa != null)
									sakit += sakit.isEmpty() ? mahasiswa.getNama() : ", " + mahasiswa.getNama();
							} else if (kode.equalsIgnoreCase("I")) {
								if (mahasiswa != null)
									izin += izin.isEmpty() ? mahasiswa.getNama() : ", " + mahasiswa.getNama();
							} else if (kode.equalsIgnoreCase("M")) {
								if (mahasiswa != null)
									hadir += hadir.isEmpty() ? mahasiswa.getNama() : ", " + mahasiswa.getNama();
							} else {
								if (mahasiswa != null)
									belum += belum.isEmpty() ? mahasiswa.getNama() : ", " + mahasiswa.getNama();
							}
						}
					} else if (nn.toLowerCase().endsWith("siswa")) {
						String[] s = nn.split(",");
						Long formatId = null;
						try {
							formatId = Long.parseLong(s[0]);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7610");
						}
						if (formatId != null) {
							Siswa siswa = (Siswa) ConstantValues.ambil(Siswa.class.getName(), formatId);
							String kode = s[2];
							if (kode.equalsIgnoreCase("A")) {
								if (siswa != null)
									alpa += alpa.isEmpty() ? siswa.getNama() : ", " + siswa.getNama();
							} else if (kode.equalsIgnoreCase("S")) {
								if (siswa != null)
									sakit += sakit.isEmpty() ? siswa.getNama() : ", " + siswa.getNama();
							} else if (kode.equalsIgnoreCase("I")) {
								if (siswa != null)
									izin += izin.isEmpty() ? siswa.getNama() : ", " + siswa.getNama();
							} else if (kode.equalsIgnoreCase("M")) {
								if (siswa != null)
									hadir += hadir.isEmpty() ? siswa.getNama() : ", " + siswa.getNama();
							} else {
								if (siswa != null)
									belum += belum.isEmpty() ? siswa.getNama() : ", " + siswa.getNama();
							}
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7633");
				}
			}

			String tanya = "Buatkan catatan dan berita acara untuk " + pertemuan.info() + ", hari dan tanggal : "
					+ Common.dateFormat6.get().format(pertemuan.getTanggal()) + ", jam waktu mulai "
					+ pertemuan.getWaktuMulai() + " sampai " + pertemuan.getWaktuSelesai() + ", Ruangan : "
					+ (pertemuan.getRuang() == null ? "Belum ditentukan" : pertemuan.getRuang().getNama())
					+ ", Gedung : "
					+ (pertemuan.getRuang() == null || pertemuan.getRuang().getGedung() == null ? ""
							: pertemuan.getRuang().getGedung().getNama())
					+ ", \nPengajar : " + d + ", \nTopik : " + pertemuan.getTopik() + ", \nSemua peserta : " + p
					+ ", \ndaftar peserta yang hadir : " + (hadir.trim().isEmpty() ? "tidak ada yang hadir" : hadir)
					+ ", \ndaftar peserta yang sakit : " + (sakit.trim().isEmpty() ? "tidak ada yang sakit" : sakit)
					+ ", \ndaftar peserta yang izin : " + (izin.trim().isEmpty() ? "tidak ada yang izin" : izin)
					+ ",  \ndaftar peserta yang mangkir atau tidak ada alasan : "
					+ (alpa.trim().isEmpty() ? "tidak ada yang mangkir atau tidak ada alasan" : alpa)
					+ ", \ndaftar peserta yang status kehadirannya belum ditentukan : "
					+ (belum.trim().isEmpty() ? "tidak ada yang belum ditentukan kehadirannya" : belum);

			String tanyaAkhiran = "";
			String tanyaMengajar = " apa saja";
			if (pertemuan.getPerkuliahan() != null && pertemuan.getPerkuliahan().getMatakuliah() != null) {
				tanyaMengajar = " matakuliah " + pertemuan.getPerkuliahan().getMatakuliah().getNama();
				tanyaAkhiran = " pada matakuliah \"" + pertemuan.getPerkuliahan().getMatakuliah().getNama() + "\"";
			} else if (pertemuan.getJadwalPelajaran() != null
					&& pertemuan.getJadwalPelajaran().getMatapelajaran() != null) {
				tanyaMengajar = " matapelajaran " + pertemuan.getJadwalPelajaran().getMatapelajaran().getNama();
				tanyaAkhiran = " pada matapelajaran \"" + pertemuan.getJadwalPelajaran().getMatapelajaran().getNama()
						+ "\"";
			}

			toolbarbutton.addEventListener("onClick",
					AIGenerator.generateApa("Generate Catatan", "Informasikan tentang pertemuan kali ini", tanya, false,
							tanyaAkhiran,
							Common.getKonfigurasi("llama_system_catatan", "Kamu adalah Pengajar atau Dosen atau Guru ")
									.getNilai().trim(),
							null, new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									pertemuan.setCatatan(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
											.replaceAll("\n", "<br>"));
									Common.refreshUpdate(pertemuan);
									eventListener.onEvent(null);
								}
							}, tanyaMengajar, new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									html.setContent(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
											.replaceAll("\n", "<br>"));
								}
							}));
		}
		return hboxCararan;
	}

	public void displayRow(Row rowUtama, boolean mobile, Pertemuan pertemuan, Long selectedDiskusi) throws Exception {
		DashboardTimelinePertemuan.displayRow(rowUtama, mobile, pertemuan, selectedDiskusi, tbmuser);
	}

	public static void displayRow(final Row rowUtama, final boolean mobile, final Pertemuan pertemuan,
			final Long selectedDiskusi, final Tbmuser tbmuser) throws Exception {

		if (rowUtama != null) {

			Common.clear(rowUtama);

		}
		pertemuan.masukkanData("akses");

		Groupbox vboxUtama = DashboardTimelinePertemuan.displayInfoPertemuan(pertemuan);
		vboxUtama.setParent(rowUtama);

		DashboardTimelinePertemuan.displayCatatan(vboxUtama, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				displayRow(rowUtama, mobile, pertemuan, selectedDiskusi, tbmuser);
			}
		}, pertemuan, tbmuser, mobile);

		DashboardTimelinePertemuan.tampilOnline(pertemuan, vboxUtama, tbmuser, new EventListener() {
			@Override
			public void onEvent(Event a) throws Exception {
				displayRow(rowUtama, mobile, pertemuan, selectedDiskusi, tbmuser);
			}
		});

		Component aa = createVideoConrefrence(pertemuan, null, false, new EventListener() {
			@Override
			public void onEvent(Event a) throws Exception {
				displayRow(rowUtama, mobile, pertemuan, selectedDiskusi, tbmuser);
			}
		});

		MyToolbarbutton a = new MyToolbarbutton("fa-calendar-o", "Agenda");

		a.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event a) throws Exception {
				CalendarPerkuliahanMingguIniComposer.init(pertemuan, new EventListener() {
					@Override
					public void onEvent(Event a) throws Exception {
						displayRow(rowUtama, mobile, pertemuan, selectedDiskusi, tbmuser);
					}
				});
			}
		});

		if (pertemuan.getJadwalPelajaran() != null) {
			Component bb = AbsensiSiswaHelper.createTombolAbsen(pertemuan, new DataLoader() {
				@Override
				public void loadData(Object value) {
					try {
						displayRow(rowUtama, mobile, pertemuan, selectedDiskusi, tbmuser);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			});

			AktifitasPerkuliahanHelper.createKeterangan(pertemuan, new DataLoader() {
				@Override
				public void loadData(Object value) {
					Session session = null;
					try {
						if (value != null && value.equals(true)) {
							session = HibernateUtil.getSessionFactory().openSession();
							session.refresh(pertemuan);
						}
						displayRow(rowUtama, mobile, pertemuan, selectedDiskusi, tbmuser);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					} finally {
						if (session != null && value != null && value.equals(true)) {
							try {
								session.clear();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7769");
							}
							try {
								session.disconnect();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7773");
							}
							try {
								closeHibernateSessionQuietly(session);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7777");
							}
						}
					}
				}
			}, aa, a, bb, createScanFoto(tbmuser, pertemuan)).setParent(vboxUtama);
		} else {

			Component bb = AbsensiHelper.createTombolAbsen(pertemuan, true, new DataLoader() {
				@Override
				public void loadData(Object value) {
					try {
						displayRow(rowUtama, mobile, pertemuan, selectedDiskusi, tbmuser);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			});

			AktifitasPerkuliahanHelper.createKeterangan(pertemuan, new DataLoader() {
				@Override
				public void loadData(Object value) {
					Session session = null;
					try {
						if (value != null && value.equals(true)) {
							session = HibernateUtil.getSessionFactory().openSession();
							session.refresh(pertemuan);
						}
						displayRow(rowUtama, mobile, pertemuan, selectedDiskusi, tbmuser);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					} finally {
						if (session != null && value != null && value.equals(true)) {
							try {
								session.clear();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7812");
							}
							try {
								session.disconnect();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7816");
							}
							try {
								closeHibernateSessionQuietly(session);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7820");
							}
						}
					}
				}
			}, aa, a, bb, createScanFoto(tbmuser, pertemuan)).setParent(vboxUtama);
		}

		if (Common.bolehKonfigurasi("komentar_tampil_di_halaman_utama_elearning")) {
			Vbox vbox = new Vbox();
			vbox.setParent(vboxUtama);
			if (!pertemuan.udah()) {
				Session session = null;
				try {
					session = HibernateUtil.getSessionFactory().openSession();
					pertemuan.reInitPertemuanPunyaDiskusi(session);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7837");
				} finally {
					if (session != null) {
						try {
							session.clear();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7842");
						}
						try {
							session.disconnect();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7846");
						}
						try {
							closeHibernateSessionQuietly(session);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7850");
						}
					}
				}
			}

			boolean urut = false;
			try {
				String pil = tbmuser.retreive("urutkan_diskusi_berdasarkan_terlama");
				urut = (pil == null || pil.trim().isEmpty() ? false : Boolean.parseBoolean(pil));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7860");
			}

			EventListener eventListenerUtama = new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					try {
						displayRow(rowUtama, mobile, pertemuan, selectedDiskusi, tbmuser);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			};

			TreeSet<Long> pertemuanPunyaDiskusisa = pertemuan.ambilPertemuanPunyaDiskusiTotal(urut);
			DashboardTimelinePertemuan.loadKomentarDetail(null, "90px", pertemuanPunyaDiskusisa, pertemuan, vbox,
					"border:0px;background: transparent;", 0, 10, false, eventListenerUtama, selectedDiskusi);
		}
	}

	public static MyMenuitem createScanQrCode(Tbmuser tbmuser, boolean besar, boolean lbl) {
		MyMenuitem toolbarbutton = new MyMenuitem("Scan Qrcode", "/img/svg/qrcode-scan.svg");
		return (MyMenuitem) createScanQrCode(tbmuser, besar, lbl, toolbarbutton);
	}

	public static Component createScanQrCode(Tbmuser tbmuser, boolean besar, boolean lbl, Component toolbarbutton) {

		toolbarbutton.setVisible(false);
		if (tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null
				|| tbmuser.getBiodataCalonMahasiswa() != null || tbmuser.getCalonSiswa() != null
				|| tbmuser.getDosen() != null || tbmuser.getGuru() != null || tbmuser.getPegawai() != null)) {
			toolbarbutton.setVisible(true);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event a) throws Exception {

					Tbmuser tbmuser = Common.getCurrentUser();
					String q = "";
					if (tbmuser != null && tbmuser.getMahasiswa() != null)
						q = "&mahasiswa=" + tbmuser.getMahasiswa().getId();
					else if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null)
						q = "&calon_mahasiswa=" + tbmuser.getBiodataCalonMahasiswa().getId();
					else if (tbmuser != null && tbmuser.getSiswa() != null)
						q = "&siswa=" + tbmuser.getSiswa().getId();
					else if (tbmuser != null && tbmuser.getCalonSiswa() != null)
						q = "&calon_siswa=" + tbmuser.getCalonSiswa().getId();
					else if (tbmuser != null && tbmuser.getDosen() != null)
						q = "&dosen=" + tbmuser.getDosen().getId();
					else if (tbmuser != null && tbmuser.getGuru() != null)
						q = "&guru=" + tbmuser.getGuru().getId();

					if (tbmuser != null && tbmuser.getPegawai() != null)
						q += "&pegawai=" + tbmuser.getPegawai().getId();

					String host = URLEncoder.encode(
							Common.getRequestHostWithProtocol() + "/common/scan_berhasil.zul?p=" + -1 + q, "UTF-8");
					String src = Common.getRequestHostWithProtocol() + "/read_qr_codejsp.jsp?q=" + host;

					try {
						final MyWindow window = new MyWindow("Absen via QR-Code", "none", true);
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

						Borderlayout borderlayout = new Borderlayout();
						borderlayout.setParent(window);

						Center center = new Center();
						center.setBorder("none");
						center.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(center, true);

						boolean mobile = Common.isMobile();
						String tinggi = mobile ? "950px" : "650px";

						Html html = new ais.ui.util.MyHtml("<iframe src=\"" + src + "\" style=\"width:100%;height:"
								+ tinggi + ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
						html.setHeight(tinggi);
						Common.tampilanScroll(center).appendChild(html);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(south);
						MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
						cancel.setTooltiptext("Tutup");
						cancel.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}
						});
						cancel.setParent(toolbar);
						window.setVisible(true);
						window.setHeight("97%");
						window.setWidth(mobile ? "97%" : "550px");
						window.onModal();
						return;

					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:7960");
					}
				}
			});
		}
		return toolbarbutton;
	}

	public static Component createScanAbsenPegawai(Tbmuser tbmuser, boolean mobileAndroid) {
		MyMenuitem toolbarbutton = new MyMenuitem("Absen Online", "/img/svg/fingerprint.svg");
		return createScanAbsenPegawai(tbmuser, mobileAndroid, toolbarbutton);
	}

	public static Component createScanAbsenPegawai(Tbmuser tbmuser, final boolean mobileAndroid,
			Component toolbarbutton) {
		toolbarbutton.setVisible(false);
		if (tbmuser != null && (tbmuser.getPegawai() != null || tbmuser.getGuru() != null || tbmuser.getDosen() != null
				|| tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null)) {
			toolbarbutton.setVisible(
					true && Common.bolehKonfigurasi("aktifkan_absensi_pegawai_menggunakan_foto"));
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event a) throws Exception {
					boolean mobile = Common.isMobile();
					Tbmuser tbmuser = Common.getCurrentUser();
					String q = "";
					if (tbmuser != null && tbmuser.getMahasiswa() != null)
						q = "&mahasiswa=" + tbmuser.getMahasiswa().getId();
					else if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null)
						q = "&calon_mahasiswa=" + tbmuser.getBiodataCalonMahasiswa().getId();
					else if (tbmuser != null && tbmuser.getSiswa() != null)
						q = "&siswa=" + tbmuser.getSiswa().getId();
					else if (tbmuser != null && tbmuser.getCalonSiswa() != null)
						q = "&calon_siswa=" + tbmuser.getCalonSiswa().getId();
					else if (tbmuser != null && tbmuser.getDosen() != null)
						q = "&dosen=" + tbmuser.getDosen().getId();
					else if (tbmuser != null && tbmuser.getGuru() != null)
						q = "&guru=" + tbmuser.getGuru().getId();

					if (tbmuser != null && tbmuser.getPegawai() != null)
						q += "&pegawai=" + tbmuser.getPegawai().getId();
					if (tbmuser != null && tbmuser.getUserId() != null)
						q += "&userid=" + URLEncoder.encode(tbmuser.getUserId(), "UTF-8");
					String code = "P-"
							+ Common.desEncrypter.get().encrypt(Common.dateFormat8.get().format(WaktuUtil.getDate()));
					q += "&pert=" + URLEncoder.encode(code, "UTF-8");
					q += "&judul=" + URLEncoder
							.encode("Absensi harian, " + Common.dateFormat6.get().format(WaktuUtil.getDate()), "UTF-8");

					try {
						String src = Common.getRequestHostWithProtocol() + "/capture.jsp?mobile=" + mobile + q;
						String src1 = Common.getRequestHostWithProtocol() + "/capture_video.jsp?mobile=" + mobile + q;
						String src2 = Common.getRequestHostWithProtocol() + "/capture_lokasi.jsp?mobile=" + mobile + q;
						String src3 = Common.getRequestHostWithProtocol() + "/capture_keterangan.jsp?mobile=" + mobile
								+ q;

						if (mobileAndroid) {
							ExecutionsCtrl.getCurrent().sendRedirect(src, "_blank");
						} else {
							String tinggi = mobile ? "1300px" : "950px";

							final MyWindow window = new MyWindow("Absen Online", "none", true);
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

							Borderlayout borderlayout = new Borderlayout();
							borderlayout.setParent(window);

							Center center = new Center();
							center.setBorder("none");
							center.setParent(borderlayout);
							ais.ui.util.ZkCompat.setFlex(center, true);

							Tabbox tabbox = new Tabbox();
							tabbox.setHeight("100%");
							tabbox.setWidth("100%");
							tabbox.setParent(center);
							Tabs myTabs = new Tabs();
							myTabs.setParent(tabbox);

							Tabpanels mytabpanels = new Tabpanels();
							mytabpanels.setParent(tabbox);

							if (Common.bolehKonfigurasi("absen_online_menggunakan_lokasi")) {
								Tab tabUtama2 = new Tab("Lokasi");
								myTabs.appendChild(tabUtama2);
								Tabpanel tabpanelUtama2 = new ais.ui.util.MyTabpanel();
								tabpanelUtama2.setHeight(tinggi);
								tabpanelUtama2.setWidth("100%");
								tabpanelUtama2.setParent(mytabpanels);
								Html html2 = new ais.ui.util.MyHtml(
										"<iframe src=\"" + src2 + "\" style=\"width:100%;height:" + tinggi
												+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
								html2.setHeight(tinggi);
								Common.tampilanScroll(tabpanelUtama2).appendChild(html2);
							}

							if (Common.bolehKonfigurasi("absen_online_menggunakan_foto")) {
								Tab tabUtama = new Tab("Foto");
								myTabs.appendChild(tabUtama);
								Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
								tabpanelUtama.setHeight(tinggi);
								tabpanelUtama.setWidth("100%");
								tabpanelUtama.setParent(mytabpanels);
								Html html = new ais.ui.util.MyHtml(
										"<iframe src=\"" + src + "\" style=\"width:100%;height:" + tinggi
												+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
								html.setHeight(tinggi);
								Common.tampilanScroll(tabpanelUtama).appendChild(html);
							}

							if (Common.bolehKonfigurasi("absen_online_menggunakan_video")) {
								Tab tabUtama1 = new Tab("Video");
								myTabs.appendChild(tabUtama1);
								Tabpanel tabpanelUtama1 = new ais.ui.util.MyTabpanel();
								tabpanelUtama1.setHeight(tinggi);
								tabpanelUtama1.setWidth("100%");
								tabpanelUtama1.setParent(mytabpanels);
								Html html1 = new ais.ui.util.MyHtml(
										"<iframe src=\"" + src1 + "\" style=\"width:100%;height:" + tinggi
												+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
								html1.setHeight(tinggi);
								Common.tampilanScroll(tabpanelUtama1).appendChild(html1);
							}

							if (Common.bolehKonfigurasi("absen_online_menggunakan_keterangan")) {
								Tab tabUtama1 = new Tab("Isi Keterangan");
								myTabs.appendChild(tabUtama1);
								Tabpanel tabpanelUtama1 = new ais.ui.util.MyTabpanel();
								tabpanelUtama1.setHeight(tinggi);
								tabpanelUtama1.setWidth("100%");
								tabpanelUtama1.setParent(mytabpanels);
								Html html2 = new ais.ui.util.MyHtml(
										"<iframe src=\"" + src3 + "\" style=\"width:100%;height:" + tinggi
												+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
								html2.setHeight(tinggi);
								Common.tampilanScroll(tabpanelUtama1).appendChild(html2);
							}

							South south = new South();
							ais.ui.util.ZkCompat.setFlex(south, true);
							south.setParent(borderlayout);

							Toolbar toolbar = new Toolbar();
							toolbar.setParent(south);
							MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
							cancel.setTooltiptext("Tutup");
							cancel.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									window.detach();
								}
							});
							cancel.setParent(toolbar);
							window.setVisible(true);
							window.setHeight("97%");
							window.setWidth(mobile ? "97%" : "550px");
							window.onModal();
							return;
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8120");
					}
				}
			});
		}
		return toolbarbutton;
	}

	public static Toolbarbutton createScanFoto(Tbmuser tbmuser, final TreeMap<String, Long> pertemuansa) {
		Toolbarbutton toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Absen Online", "/img/svg/fingerprint.svg");
		toolbarbutton.setVisible(false);
		if (tbmuser != null && ((tbmuser.getMahasiswa() != null) || tbmuser.getSiswa() != null
				|| tbmuser.getBiodataCalonMahasiswa() != null || tbmuser.getCalonSiswa() != null
				|| (tbmuser.ambilDosen() != null) || tbmuser.ambilGuru() != null)) {
			toolbarbutton.setVisible(true);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event a) throws Exception {
					try {
						final boolean mobile = Common.isMobile();
						final Combobox pertemuan = new Combobox();
						final Center center = new Center();
						EventListener eventListenerdata = new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (center != null) {
									Common.clear(center);
								}
								Pertemuan pp = (Pertemuan) (pertemuan.getSelectedItem() == null ? null
										: pertemuan.getSelectedItem().getValue());

								if (pp != null) {
									Tbmuser tbmuser = Common.getCurrentUser();
									String q = "";
									if (tbmuser != null && tbmuser.getMahasiswa() != null)
										q = "&mahasiswa=" + tbmuser.getMahasiswa().getId();
									else if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null)
										q = "&calon_mahasiswa=" + tbmuser.getBiodataCalonMahasiswa().getId();
									else if (tbmuser != null && tbmuser.getSiswa() != null)
										q = "&siswa=" + tbmuser.getSiswa().getId();
									else if (tbmuser != null && tbmuser.getCalonSiswa() != null)
										q = "&calon_siswa=" + tbmuser.getCalonSiswa().getId();
									else if (tbmuser != null && tbmuser.getDosen() != null)
										q = "&dosen=" + tbmuser.getDosen().getId();
									else if (tbmuser != null && tbmuser.getGuru() != null)
										q = "&guru=" + tbmuser.getGuru().getId();

									if (tbmuser != null && tbmuser.getPegawai() != null)
										q += "&pegawai=" + tbmuser.getPegawai().getId();
									if (tbmuser != null && tbmuser.getUserId() != null)
										q += "&userid=" + URLEncoder.encode(tbmuser.getUserId(), "UTF-8");

									q += "&pert=" + pp.getId();
									q += "&judul=" + URLEncoder.encode(pp.info(), "UTF-8");
									String src = Common.getRequestHostWithProtocol() + "/capture.jsp?mobile=" + mobile
											+ q;
									String src1 = Common.getRequestHostWithProtocol() + "/capture_video.jsp?mobile="
											+ mobile + q;
									String src2 = Common.getRequestHostWithProtocol() + "/capture_lokasi.jsp?mobile="
											+ mobile + q;
									String src3 = Common.getRequestHostWithProtocol()
											+ "/capture_keterangan.jsp?mobile=" + mobile + q;

									String tinggi = mobile ? "1300px" : "950px";

									Tabbox tabbox = new Tabbox();
									tabbox.setHeight("100%");
									tabbox.setWidth("100%");
									tabbox.setParent(center);
									Tabs myTabs = new Tabs();
									myTabs.setParent(tabbox);

									Tabpanels mytabpanels = new Tabpanels();
									mytabpanels.setParent(tabbox);

									if (Common.bolehKonfigurasi("absen_online_menggunakan_lokasi")) {
										Tab tabUtama2 = new Tab("Lokasi");
										myTabs.appendChild(tabUtama2);
										Tabpanel tabpanelUtama2 = new ais.ui.util.MyTabpanel();
										tabpanelUtama2.setHeight(tinggi);
										tabpanelUtama2.setWidth("100%");
										tabpanelUtama2.setParent(mytabpanels);
										Html html2 = new ais.ui.util.MyHtml("<iframe src=\"" + src2
												+ "\" style=\"width:100%;height:" + tinggi
												+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
										html2.setHeight(tinggi);
										Common.tampilanScroll(tabpanelUtama2).appendChild(html2);
									}

									if (Common.bolehKonfigurasi("absen_online_menggunakan_foto")) {
										Tab tabUtama = new Tab("Foto");
										myTabs.appendChild(tabUtama);
										Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
										tabpanelUtama.setHeight(tinggi);
										tabpanelUtama.setWidth("100%");
										tabpanelUtama.setParent(mytabpanels);
										Html html = new ais.ui.util.MyHtml("<iframe src=\"" + src
												+ "\" style=\"width:100%;height:" + tinggi
												+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
										html.setHeight(tinggi);
										Common.tampilanScroll(tabpanelUtama).appendChild(html);
									}

									if (Common.bolehKonfigurasi("absen_online_menggunakan_video")) {
										Tab tabUtama1 = new Tab("Video");
										myTabs.appendChild(tabUtama1);
										Tabpanel tabpanelUtama1 = new ais.ui.util.MyTabpanel();
										tabpanelUtama1.setHeight(tinggi);
										tabpanelUtama1.setWidth("100%");
										tabpanelUtama1.setParent(mytabpanels);
										Html html1 = new ais.ui.util.MyHtml("<iframe src=\"" + src1
												+ "\" style=\"width:100%;height:" + tinggi
												+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
										html1.setHeight(tinggi);
										Common.tampilanScroll(tabpanelUtama1).appendChild(html1);
									}

									if (Common.bolehKonfigurasi("absen_online_menggunakan_keterangan")) {
										Tab tabUtama1 = new Tab("Isi Keterangan");
										myTabs.appendChild(tabUtama1);
										Tabpanel tabpanelUtama1 = new ais.ui.util.MyTabpanel();
										tabpanelUtama1.setHeight(tinggi);
										tabpanelUtama1.setWidth("100%");
										tabpanelUtama1.setParent(mytabpanels);
										Html html2 = new ais.ui.util.MyHtml("<iframe src=\"" + src3
												+ "\" style=\"width:100%;height:" + tinggi
												+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
										html2.setHeight(tinggi);
										Common.tampilanScroll(tabpanelUtama1).appendChild(html2);
									}
								}
							}
						};

						final MyWindow window = new MyWindow("Absen Online", "none", true);
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

						Borderlayout borderlayout = new Borderlayout();
						borderlayout.setParent(window);

						North north = new North();
						north.setBorder("none");
						north.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(north, true);
						north.setHeight("35px");

						Hbox hbox = new Hbox();
						hbox.setParent(north);
						hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Pilih Pertemuan : ")));
						hbox.appendChild(pertemuan);
						pertemuan.setCols(20);

						for (Long pertemuanid : pertemuansa.values()) {
							Pertemuan p = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
									pertemuanid.toString());
							Comboitem comboitem = new Comboitem("Pertemuan ke-" + p.getPertemuanKe() + " "
									+ p.getTopik() + " "
									+ (p.getStatusPertemuan() == null ? "" : p.getStatusPertemuan().getNama()) + " "
									+ (p.getTanggal() == null ? "" : Common.dateFormat6.get().format(p.getTanggal())));
							comboitem.setValue(p);
							pertemuan.appendChild(comboitem);
						}
						pertemuan.setReadonly(true);
						pertemuan.setDisabled(false);

						if (pertemuansa.size() == 1) {
							pertemuan.setSelectedIndex(0);
							pertemuan.addEventListener("onChange", eventListenerdata);
							eventListenerdata.onEvent(null);
						} else if (!pertemuansa.isEmpty()) {
							Comboitem comboitem = new Comboitem("= Pilih Pertemuan =");
							comboitem.setValue(null);
							pertemuan.appendChild(comboitem);
							pertemuan.setSelectedItem(comboitem);
							pertemuan.addEventListener("onChange", eventListenerdata);
						} else {
							Comboitem comboitem = new Comboitem("= Pertemuan / Agenda belum dibuat =");
							comboitem.setValue(null);
							pertemuan.appendChild(comboitem);
							pertemuan.setSelectedItem(comboitem);
							pertemuan.setDisabled(true);
						}

						center.setBorder("none");
						center.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(center, true);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(south);
						MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
						cancel.setTooltiptext("Tutup");
						cancel.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}
						});
						cancel.setParent(toolbar);
						window.setVisible(true);
						window.setHeight("97%");
						window.setWidth(mobile ? "97%" : "550px");
						window.onModal();

					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8328");
					}
				}
			});
		}
		return toolbarbutton;
	}

	public static MyToolbarbutton createScanFoto(Tbmuser tbmuser, final Pertemuan pertemuan) {
		MyToolbarbutton toolbarbutton = new MyToolbarbutton("fa-check-circle-o", "Absen");
		toolbarbutton.setVisible(false);
		if (tbmuser != null && ((tbmuser.getMahasiswa() != null && pertemuan.getMahasiswaBolehAbsenMenggunakanFoto())
				|| tbmuser.getSiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null
				|| tbmuser.getCalonSiswa() != null
				|| (tbmuser.ambilDosen() != null && pertemuan.getDosenBolehAbsenMenggunakanFoto())
				|| tbmuser.ambilGuru() != null)) {
			toolbarbutton.setVisible(true);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event a) throws Exception {
					boolean mobile = Common.isMobile();
					Tbmuser tbmuser = Common.getCurrentUser();
					String q = "";
					if (tbmuser != null && tbmuser.getMahasiswa() != null)
						q = "&mahasiswa=" + tbmuser.getMahasiswa().getId();
					else if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null)
						q = "&calon_mahasiswa=" + tbmuser.getBiodataCalonMahasiswa().getId();
					else if (tbmuser != null && tbmuser.getSiswa() != null)
						q = "&siswa=" + tbmuser.getSiswa().getId();
					else if (tbmuser != null && tbmuser.getCalonSiswa() != null)
						q = "&calon_siswa=" + tbmuser.getCalonSiswa().getId();
					else if (tbmuser != null && tbmuser.getDosen() != null)
						q = "&dosen=" + tbmuser.getDosen().getId();
					else if (tbmuser != null && tbmuser.getGuru() != null)
						q = "&guru=" + tbmuser.getGuru().getId();

					if (tbmuser != null && tbmuser.getPegawai() != null)
						q += "&pegawai=" + tbmuser.getPegawai().getId();
					if (tbmuser != null && tbmuser.getUserId() != null)
						q += "&userid=" + URLEncoder.encode(tbmuser.getUserId(), "UTF-8");

					q += "&pert=" + pertemuan.getId();
					q += "&judul=" + URLEncoder.encode(pertemuan.info(), "UTF-8");

					try {
						Boolean mobileAndroid = (Boolean) Sessions.getCurrent().getAttribute("mobileAndroid");

						String src = Common.getRequestHostWithProtocol() + "/capture.jsp?mobile=" + mobile + q;
						String src1 = Common.getRequestHostWithProtocol() + "/capture_video.jsp?mobile=" + mobile + q;
						String src2 = Common.getRequestHostWithProtocol() + "/capture_lokasi.jsp?mobile=" + mobile + q;
						String src3 = Common.getRequestHostWithProtocol() + "/capture_keterangan.jsp?mobile=" + mobile
								+ q;

						if (mobileAndroid != null && mobileAndroid) {
							ExecutionsCtrl.getCurrent().sendRedirect(src, "_blank");
						} else {
							String tinggi = mobile ? "1300px" : "950px";

							final MyWindow window = new MyWindow("Absen Online", "none", true);
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

							Borderlayout borderlayout = new Borderlayout();
							borderlayout.setParent(window);

							Center center = new Center();
							center.setBorder("none");
							center.setParent(borderlayout);
							ais.ui.util.ZkCompat.setFlex(center, true);

							Tabbox tabbox = new Tabbox();
							tabbox.setHeight("100%");
							tabbox.setWidth("100%");
							tabbox.setParent(center);
							Tabs myTabs = new Tabs();
							myTabs.setParent(tabbox);

							Tabpanels mytabpanels = new Tabpanels();
							mytabpanels.setParent(tabbox);

							if (Common.bolehKonfigurasi("absen_online_menggunakan_lokasi")) {
								Tab tabUtama2 = new Tab("Lokasi");
								myTabs.appendChild(tabUtama2);
								Tabpanel tabpanelUtama2 = new ais.ui.util.MyTabpanel();
								tabpanelUtama2.setHeight(tinggi);
								tabpanelUtama2.setWidth("100%");
								tabpanelUtama2.setParent(mytabpanels);
								Html html2 = new ais.ui.util.MyHtml(
										"<iframe src=\"" + src2 + "\" style=\"width:100%;height:" + tinggi
												+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
								html2.setHeight(tinggi);
								Common.tampilanScroll(tabpanelUtama2).appendChild(html2);
							}

							if (Common.bolehKonfigurasi("absen_online_menggunakan_foto")) {
								Tab tabUtama = new Tab("Foto");
								myTabs.appendChild(tabUtama);
								Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
								tabpanelUtama.setHeight(tinggi);
								tabpanelUtama.setWidth("100%");
								tabpanelUtama.setParent(mytabpanels);
								Html html = new ais.ui.util.MyHtml(
										"<iframe src=\"" + src + "\" style=\"width:100%;height:" + tinggi
												+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
								html.setHeight(tinggi);
								Common.tampilanScroll(tabpanelUtama).appendChild(html);
							}

							if (Common.bolehKonfigurasi("absen_online_menggunakan_video")) {
								Tab tabUtama1 = new Tab("Video");
								myTabs.appendChild(tabUtama1);
								Tabpanel tabpanelUtama1 = new ais.ui.util.MyTabpanel();
								tabpanelUtama1.setHeight(tinggi);
								tabpanelUtama1.setWidth("100%");
								tabpanelUtama1.setParent(mytabpanels);
								Html html1 = new ais.ui.util.MyHtml(
										"<iframe src=\"" + src1 + "\" style=\"width:100%;height:" + tinggi
												+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
								html1.setHeight(tinggi);
								Common.tampilanScroll(tabpanelUtama1).appendChild(html1);
							}

							if (Common.bolehKonfigurasi("absen_online_menggunakan_keterangan")) {
								Tab tabUtama1 = new Tab("Isi Keterangan");
								myTabs.appendChild(tabUtama1);
								Tabpanel tabpanelUtama1 = new ais.ui.util.MyTabpanel();
								tabpanelUtama1.setHeight(tinggi);
								tabpanelUtama1.setWidth("100%");
								tabpanelUtama1.setParent(mytabpanels);
								Html html2 = new ais.ui.util.MyHtml(
										"<iframe src=\"" + src3 + "\" style=\"width:100%;height:" + tinggi
												+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
								html2.setHeight(tinggi);
								Common.tampilanScroll(tabpanelUtama1).appendChild(html2);
							}

							South south = new South();
							ais.ui.util.ZkCompat.setFlex(south, true);
							south.setParent(borderlayout);

							Toolbar toolbar = new Toolbar();
							toolbar.setParent(south);
							MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
							cancel.setTooltiptext("Tutup");
							cancel.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									window.detach();
								}
							});
							cancel.setParent(toolbar);
							window.setVisible(true);
							window.setHeight("97%");
							window.setWidth(mobile ? "97%" : "550px");
							window.onModal();
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8484");
					}
				}
			});
		}
		return toolbarbutton;
	}

	public static void tampilOnline(final Pertemuan pertemuan, Component vbox, Tbmuser tbmuser,
			final EventListener eventListener) {
		try {
			TreeMap<String, String> d = pertemuan.ambilData("online", null);
			if (!d.isEmpty()) {
				StringBuilder onlBuilder = new StringBuilder("<ol>");
				int i = 0;
				for (String user : d.keySet()) {
					if (i > 1) {
						onlBuilder.append("<li>...</li>");
						break;
					}
					i++;
					try {
						String jam = d.get(user);
						String[] u = user.split("-");

						if (pertemuan.getPerkuliahan() == null) {
							onlBuilder.append("<li>").append(u[0]).append(" (").append(jam).append(") ").append(u[2])
									.append("</li>");
						} else if (u[2].equalsIgnoreCase("Mahasiswa") || u[2].equalsIgnoreCase("CalonMahasiswa")
								|| u[2].equalsIgnoreCase("CalonSiswa") || u[2].equalsIgnoreCase("Siswa")) {
							onlBuilder.append("<li>").append(u[0]).append(" (").append(jam).append(")</li>");
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8516");
					}
				}
				onlBuilder.append("</ol>");

				Vbox hbox = new Vbox();
				vbox.appendChild(hbox);
				final Html s = new ais.ui.util.MyHtml(
						"<div style=\"font-size:10px;color:blue;\"><b>Daftar peserta online :</b><br>"
								+ onlBuilder.toString() + "</div>");
				hbox.appendChild(s);

				if (!d.isEmpty()) {

					int jumlah = 0;
					for (String user : d.keySet()) {
						try {
							String[] u = user.split("-");
							if (pertemuan.getPerkuliahan() == null) {
								jumlah++;
							} else if (u[2].equalsIgnoreCase("Mahasiswa") || u[2].equalsIgnoreCase("CalonMahasiswa")
									|| u[2].equalsIgnoreCase("CalonSiswa") || u[2].equalsIgnoreCase("Siswa")) {
								jumlah++;
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8540");
						}
					}

					final Long idPertemuan = pertemuan.getId();
					final MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(
							"Tampilkan " + jumlah + " peserta online dan tidak online", "/img/online-red-icon.png");
					toolbarbutton.setStyle("font-size:9px;");
					vbox.appendChild(toolbarbutton);
					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Pertemuan pertemuanData = ((Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
									idPertemuan.toString()));
							TreeMap<String, String> dFull = pertemuanData.ambilData("online", null);
							StringBuilder onlFullBuilder = new StringBuilder("<ol>");
							for (String user : dFull.keySet()) {
								try {
									String jam = dFull.get(user);
									String[] u = user.split("-");
									if (pertemuanData.getPerkuliahan() == null) {
										onlFullBuilder.append("<li>").append(u[0]).append(" (").append(jam).append(") ")
												.append(u[2]).append("</li>");
									} else if (u[2].equalsIgnoreCase("Mahasiswa")
											|| u[2].equalsIgnoreCase("CalonMahasiswa")
											|| u[2].equalsIgnoreCase("CalonSiswa") || u[2].equalsIgnoreCase("Siswa")) {
										onlFullBuilder.append("<li>").append(u[0]).append(" (").append(jam)
												.append(")</li>");
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8570");
								}
							}
							onlFullBuilder.append("</ol>");

							List<String> ygBelumAkses = TampilanELearningAction.belumAkses(pertemuanData);

							List<Map.Entry<String, String>> elements = new LinkedList<Map.Entry<String, String>>(
									dFull.entrySet());
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

							StringBuilder notOnlBuilder = new StringBuilder("<ol>");
							for (String key : ygBelumAkses) {
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
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8610");
								}

								if (pertemuanData.getPerkuliahan() == null) {
									notOnlBuilder.append("<li>").append(u[0]).append(" ").append(u[2]).append("</li>");
								} else if (u[2].equalsIgnoreCase("Mahasiswa") || u[2].equalsIgnoreCase("CalonMahasiswa")
										|| u[2].equalsIgnoreCase("CalonSiswa") || u[2].equalsIgnoreCase("Siswa")) {
									notOnlBuilder.append("<li>").append(u[0]).append("</li>");
								}
							}
							notOnlBuilder.append("</ol>");

							int jumlahBelum = ygBelumAkses.size();
							int belumAkses = (jumlahBelum - dFull.size());

							s.setContent("<div style=\"font-size:10px;color:blue;\"><b>Daftar peserta online :</b><br>"
									+ onlFullBuilder.toString() + "</div>"
									+ (belumAkses > 0
											? "<br><div style=\"font-size:10px;color:#94938e;\"><b>Daftar peserta belum / tidak online :</b><br>"
													+ notOnlBuilder.toString() + "</div>"
											: ""));

							ygBelumAkses = null;
							dFull = null;
							toolbarbutton.detach();
						}
					});

					Toolbarbutton masukHadir = new MyToolbarbuttonConfig("Online dianggap hadir",
							"/img/svg/check2.svg");
					masukHadir.setStyle("font-size:9px;");
					masukHadir.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null
							&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
							&& tbmuser.getCalonSiswa() == null && jumlah > 0);
					masukHadir.setTooltiptext("Ikut. Vidio Conf.");
					masukHadir.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							PertemuanPunyaDiskusiHelper.aksesDianggapHadir(pertemuan, "online", "Video Conference",
									null, null, new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											CommonReportHelper.onLaporanBeritaAcara(pertemuan, null);
											if (eventListener != null)
												eventListener.onEvent(arg0);
										}
									});
						}
					});
					masukHadir.setParent(vbox);

					Toolbarbutton masukAlpa = new MyToolbarbuttonConfig("Tdk Online dianggap alpa",
							"/img/Check-icon.png");
					masukAlpa.setStyle("font-size:9px;");
					masukAlpa.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
							&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null
							&& jumlah > 0);
					masukAlpa.setTooltiptext("Tidak Ikut Vidio Conf.");
					masukAlpa.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							PertemuanPunyaDiskusiHelper.tidakAksesDianggapAlpa(pertemuan, "online",
									"Tidak ikut Video Conference", null, null, new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											CommonReportHelper.onLaporanBeritaAcara(pertemuan, null);
											if (eventListener != null)
												eventListener.onEvent(arg0);
										}
									});
						}
					});
					masukAlpa.setParent(vbox);
				}
			}

			Set<String> moderator = new HashSet<String>();
			if (pertemuan.getCalendarEvent() != null && !pertemuan.getCalendarEvent().trim().isEmpty()) {
				JSONObject jsonObject = new JSONObject(pertemuan.getCalendarEvent());
				if (!jsonObject.isNull("attendees")) {
					JSONArray attendees = jsonObject.getJSONArray("attendees");
					for (int i = 0; i < attendees.length(); i++) {
						try {
							JSONObject attendee = attendees.getJSONObject(i);
							if (!attendee.isNull("organizer") && attendee.getBoolean("organizer")) {
								moderator.add(attendee.getString("email").trim());
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8697");
						}
					}
				}
				if (!jsonObject.isNull("organizer")) {
					try {
						JSONObject organizer = jsonObject.getJSONObject("organizer");
						moderator.add(organizer.getString("email").trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8705");
					}
				}
			}

			if (!moderator.isEmpty()) {
				MyLabelKecilBold lblModerator = new MyLabelKecilBold("Moderator Online : "
						+ moderator.toString().replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\"", ""));
				lblModerator.setStyle("font-size:8px;font-weight: bolder;color:blue");
				vbox.appendChild(lblModerator);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8717");
		}
	}

	public static Toolbarbutton createVideoConrefrence(final Pertemuan pertemuan, Component hbox, boolean vertical,
			final EventListener eventListener) throws Exception {
		return createVideoConrefrence(pertemuan, hbox, vertical, false, eventListener);
	}

	public static boolean checkApakahSesuaiJadwal(Pertemuan pertemuan) throws Exception {
		if (pertemuan == null) {
			return true;
		}
		if (pertemuan.getPerkulaiahnOnlineHarusSesuaiJadwal()) {

			Calendar calendarMulai = WaktuUtil.getCalendar();
			calendarMulai.setTime(pertemuan.getTanggal());
			try {
				if (pertemuan.getWaktuMulai() != null) {
					Integer jamMulai = Integer.parseInt(pertemuan.getWaktuMulai().split("\\.")[0]);
					Integer menitMulai = Integer.parseInt(pertemuan.getWaktuMulai().split("\\.")[1]);
					calendarMulai.set(Calendar.HOUR_OF_DAY, jamMulai);
					calendarMulai.set(Calendar.MINUTE, menitMulai);
					calendarMulai.set(Calendar.SECOND, 1);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8743");
			}

			Calendar calendarSelesai = WaktuUtil.getCalendar();
			calendarSelesai.setTime(pertemuan.getTanggal());
			try {
				if (pertemuan.getWaktuSelesai() != null) {
					Integer jamMulai = Integer.parseInt(pertemuan.getWaktuSelesai().split("\\.")[0]);
					Integer menitMulai = Integer.parseInt(pertemuan.getWaktuSelesai().split("\\.")[1]);
					calendarSelesai.set(Calendar.HOUR_OF_DAY, jamMulai);
					calendarSelesai.set(Calendar.MINUTE, menitMulai);
					calendarSelesai.set(Calendar.SECOND, 1);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8757");
			}

			Date currentDate = WaktuUtil.getDate();
			if (currentDate.before(calendarMulai.getTime())) {
				String d = (SmartDateTimeUtil.getDayString(calendarMulai.getTime(), null)
						+ Common.dateFormat5.get().format(calendarMulai.getTime()));
				MyMessageboxConfig.showFormat(
						"Mohon maaf, pertemuan daring (online) ini belum dapat diakses karena baru akan dimulai pada {V1}. Langkah yang dapat dilakukan: (1) silakan kembali mengakses pertemuan pada waktu yang telah ditentukan tersebut; (2) pastikan tanggal dan waktu perangkat Anda sudah benar; (3) apabila terdapat kekeliruan jadwal, mohon menghubungi pengajar atau administrator.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, d);
				return false;
			} else if (currentDate.after(calendarSelesai.getTime())) {
				String d = (SmartDateTimeUtil.getDayString(calendarSelesai.getTime(), null)
						+ Common.dateFormat5.get().format(calendarSelesai.getTime()));
				MyMessageboxConfig.showFormat(
						"Mohon maaf, pertemuan daring (online) ini sudah tidak dapat diakses karena waktunya telah berakhir pada {V1}. Langkah yang dapat dilakukan: (1) hubungi pengajar untuk mengetahui kemungkinan penjadwalan ulang; (2) periksa apakah tersedia rekaman atau materi pengganti; (3) apabila terdapat kekeliruan jadwal, mohon menghubungi administrator.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, d);
				return false;
			}
		}
		return true;
	}

	public static Toolbarbutton createVideoConrefrence(final Pertemuan pertemuan, Component hbox, boolean vertical,
			boolean button, final EventListener eventListener) throws Exception {
		if (pertemuan == null) {
			return new ais.ui.util.MyToolbarbuttonConfig("Online", "/img/svg/camera-video.svg");
		}

		Toolbarbutton a = new ais.ui.util.MyToolbarbuttonConfig("Online", "/img/svg/camera-video.svg");

		final Dosen dosenUtama = pertemuan.dosenUtama();
		if (dosenUtama != null && !dosenUtama.getOnlineMenggunakan().equals(Dosen.TIDAK_AKTIF)
				&& !dosenUtama.getOnlineLink().trim().isEmpty()) {
			a.setVisible(true);
			if (hbox != null)
				a.setParent(hbox);

			try {
				TreeMap<String, String> d = pertemuan.ambilData("online", null);
				StringBuilder onlBuilder = new StringBuilder();

				for (String user : d.keySet()) {
					try {
						String jam = d.get(user);
						String[] u = user.split("-");
						if (u[2].equalsIgnoreCase("Dosen") || u[2].equalsIgnoreCase("Mahasiswa")
								|| u[2].equalsIgnoreCase("CalonMahasiswa") || u[2].equalsIgnoreCase("CalonSiswa")
								|| u[2].equalsIgnoreCase("Guru") || u[2].equalsIgnoreCase("Siswa")) {
							if (onlBuilder.length() > 0)
								onlBuilder.append(",");
							onlBuilder.append(u[0]).append(" (").append(jam).append(")");
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8810");
					}
				}
				if (onlBuilder.length() > 0) {
					a.setTooltiptext(onlBuilder.toString());
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8816");
			}

			a.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {
					if (!checkApakahSesuaiJadwal(pertemuan))
						return;

					String server = dosenUtama.getOnlineLink();
					pertemuan.masukkanData("online");

					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");
					}
					if (eventListener != null)
						eventListener.onEvent(null);
				}
			});

			return a;
		}

		if (pertemuan.getOnlineMenggunakan().equals(Pertemuan.JITSI)) {
			int jumlah = 0;
			try {
				TreeMap<String, String> d = pertemuan.ambilData("online", null);
				StringBuilder onlBuilder = new StringBuilder();

				for (String user : d.keySet()) {
					try {
						String jam = d.get(user);
						String[] u = user.split("-");
						if (u[2].equalsIgnoreCase("Dosen") || u[2].equalsIgnoreCase("Mahasiswa")
								|| u[2].equalsIgnoreCase("CalonMahasiswa") || u[2].equalsIgnoreCase("CalonSiswa")
								|| u[2].equalsIgnoreCase("Guru") || u[2].equalsIgnoreCase("Siswa")) {
							if (onlBuilder.length() > 0)
								onlBuilder.append(",");
							onlBuilder.append(u[0]).append(" (").append(jam).append(")");
							jumlah++;
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8860");
					}
				}
				if (onlBuilder.length() > 0) {
					a.setTooltiptext(onlBuilder.toString());
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8866");
			}

			if (hbox != null)
				a.setParent(hbox);

			Tbmuser tbmuser = Common.getCurrentUser();
			a.setVisible((pertemuan.getStatusPertemuan() != null && pertemuan.getStatusPertemuan().getNama() != null
					&& pertemuan.getStatusPertemuan().getNama().equalsIgnoreCase("Daring")) || jumlah > 0
					|| tbmuser == null || (tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null));

			a.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event evt) throws Exception {
					if (!checkApakahSesuaiJadwal(pertemuan))
						return;

					pertemuan.masukkanData("online");
					String server = pertemuan.generateJitsiLink();

					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");
					}
					if (eventListener != null)
						eventListener.onEvent(null);
				}
			});

		} else if (pertemuan.getOnlineMenggunakan().equals(Pertemuan.GOOGLE_MEET)) {
			if (hbox != null)
				a.setParent(hbox);
			try {
				TreeMap<String, String> d = pertemuan.ambilData("online", null);
				StringBuilder onlBuilder = new StringBuilder();
				for (String user : d.keySet()) {
					try {
						String jam = d.get(user);
						String[] u = user.split("-");
						if (u[2].equalsIgnoreCase("Dosen") || u[2].equalsIgnoreCase("Mahasiswa")
								|| u[2].equalsIgnoreCase("CalonMahasiswa") || u[2].equalsIgnoreCase("CalonSiswa")
								|| u[2].equalsIgnoreCase("Guru") || u[2].equalsIgnoreCase("Siswa")) {
							if (onlBuilder.length() > 0)
								onlBuilder.append(",");
							onlBuilder.append(u[0]).append(" (").append(jam).append(")");
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8914");
					}
				}
				if (onlBuilder.length() > 0) {
					a.setTooltiptext(onlBuilder.toString());
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8920");
			}

			a.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {
					String server = pertemuan.getMeetLink();
					if (server == null || server.trim().isEmpty()) {
						MyMessageboxConfig.show(
								"Mohon maaf, link Google Meet belum dimasukkan atau belum valid. Langkah yang dapat dilakukan: (1) buka pengaturan pertemuan ini; (2) masukkan link Google Meet yang benar pada kolom Link; (3) simpan dan coba lagi. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
					if (!checkApakahSesuaiJadwal(pertemuan))
						return;

					pertemuan.masukkanData("online");
					server = server + "?hs=122&ijlm=1588886137268";
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");
					}
					if (eventListener != null)
						eventListener.onEvent(null);
				}
			});

		} else if (pertemuan.getOnlineMenggunakan().equals(Pertemuan.ZOOM)) {
			if (hbox != null)
				a.setParent(hbox);
			try {
				TreeMap<String, String> d = pertemuan.ambilData("online", null);
				StringBuilder onlBuilder = new StringBuilder();
				for (String user : d.keySet()) {
					try {
						String jam = d.get(user);
						String[] u = user.split("-");
						if (u[2].equalsIgnoreCase("Dosen") || u[2].equalsIgnoreCase("Mahasiswa")
								|| u[2].equalsIgnoreCase("CalonMahasiswa") || u[2].equalsIgnoreCase("CalonSiswa")
								|| u[2].equalsIgnoreCase("Guru") || u[2].equalsIgnoreCase("Siswa")) {
							if (onlBuilder.length() > 0)
								onlBuilder.append(",");
							onlBuilder.append(u[0]).append(" (").append(jam).append(")");
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8966");
					}
				}
				if (onlBuilder.length() > 0) {
					a.setTooltiptext(onlBuilder.toString());
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:8972");
			}

			a.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {
					String server = pertemuan.getZoomLink();
					if (server == null || server.trim().isEmpty()) {
						MyMessageboxConfig.show(
								"Mohon maaf, link Zoom belum dimasukkan atau belum valid. Langkah yang dapat dilakukan: (1) buka pengaturan pertemuan ini; (2) masukkan link Zoom yang benar pada kolom Link; (3) simpan dan coba lagi. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
					if (!checkApakahSesuaiJadwal(pertemuan))
						return;

					pertemuan.masukkanData("online");
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");
					}
					if (eventListener != null)
						eventListener.onEvent(null);
				}
			});

		} else if (pertemuan.getOnlineMenggunakan().equals(Pertemuan.BBB)) {
			if (hbox != null)
				a.setParent(hbox);
			try {
				TreeMap<String, String> d = pertemuan.ambilData("online", null);
				StringBuilder onlBuilder = new StringBuilder();
				for (String user : d.keySet()) {
					try {
						String jam = d.get(user);
						String[] u = user.split("-");
						if (u[2].equalsIgnoreCase("Dosen") || u[2].equalsIgnoreCase("Mahasiswa")
								|| u[2].equalsIgnoreCase("CalonMahasiswa") || u[2].equalsIgnoreCase("CalonSiswa")
								|| u[2].equalsIgnoreCase("Guru") || u[2].equalsIgnoreCase("Siswa")) {
							if (onlBuilder.length() > 0)
								onlBuilder.append(",");
							onlBuilder.append(u[0]).append(" (").append(jam).append(")");
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:9017");
					}
				}
				if (onlBuilder.length() > 0) {
					a.setTooltiptext(onlBuilder.toString());
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:9023");
			}

			a.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {
					String server = pertemuan.getBbbLink();
					if (server == null || server.trim().isEmpty()) {
						MyMessageboxConfig.show(
								"Mohon maaf, link Big Blue Button belum dimasukkan atau belum valid. Langkah yang dapat dilakukan: (1) buka pengaturan pertemuan ini; (2) masukkan link Big Blue Button yang benar pada kolom Link; (3) simpan dan coba lagi. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
					if (!checkApakahSesuaiJadwal(pertemuan))
						return;

					pertemuan.masukkanData("online");
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");
					}
					if (eventListener != null)
						eventListener.onEvent(null);
				}
			});

		} else if (pertemuan.getOnlineMenggunakan().equals(Pertemuan.SKYPE)) {
			if (hbox != null)
				a.setParent(hbox);

			TreeMap<String, String> d = pertemuan.ambilData("online", null);
			StringBuilder onlBuilder = new StringBuilder();
			for (String user : d.keySet()) {
				try {
					String jam = d.get(user);
					String[] u = user.split("-");
					if (u[2].equalsIgnoreCase("Dosen") || u[2].equalsIgnoreCase("Mahasiswa")
							|| u[2].equalsIgnoreCase("CalonMahasiswa") || u[2].equalsIgnoreCase("CalonSiswa")
							|| u[2].equalsIgnoreCase("Guru") || u[2].equalsIgnoreCase("Siswa")) {
						if (onlBuilder.length() > 0)
							onlBuilder.append(",");
						onlBuilder.append(u[0]).append(" (").append(jam).append(")");
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:9068");
				}
			}
			if (onlBuilder.length() > 0) {
				a.setTooltiptext(onlBuilder.toString());
			}

			a.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {
					String server = pertemuan.getSkypeLink();
					if (server == null || server.trim().isEmpty()) {
						MyMessageboxConfig.show(
								"Mohon maaf, link Skype belum dimasukkan atau belum valid. Langkah yang dapat dilakukan: (1) buka pengaturan pertemuan ini; (2) masukkan link Skype yang benar pada kolom Link; (3) simpan dan coba lagi. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
					if (!checkApakahSesuaiJadwal(pertemuan))
						return;

					pertemuan.masukkanData("online");
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");
					}
					if (eventListener != null)
						eventListener.onEvent(null);
				}
			});

		} else if (pertemuan.getOnlineMenggunakan().equals(Pertemuan.WA)) {
			if (hbox != null)
				a.setParent(hbox);

			TreeMap<String, String> d = pertemuan.ambilData("online", null);
			StringBuilder onlBuilder = new StringBuilder();
			for (String user : d.keySet()) {
				try {
					String jam = d.get(user);
					String[] u = user.split("-");
					if (u[2].equalsIgnoreCase("Dosen") || u[2].equalsIgnoreCase("Mahasiswa")
							|| u[2].equalsIgnoreCase("CalonMahasiswa") || u[2].equalsIgnoreCase("CalonSiswa")
							|| u[2].equalsIgnoreCase("Guru") || u[2].equalsIgnoreCase("Siswa")) {
						if (onlBuilder.length() > 0)
							onlBuilder.append(",");
						onlBuilder.append(u[0]).append(" (").append(jam).append(")");
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:9117");
				}
			}
			if (onlBuilder.length() > 0) {
				a.setTooltiptext(onlBuilder.toString());
			}

			a.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {
					String server = pertemuan.getWaLink();
					if (server == null || server.trim().isEmpty()) {
						MyMessageboxConfig.show(
								"Mohon maaf, link Grup Whatsapp belum dimasukkan atau belum valid. Langkah yang dapat dilakukan: (1) buka pengaturan pertemuan ini; (2) masukkan link Grup Whatsapp yang benar pada kolom Link; (3) simpan dan coba lagi. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
					if (!checkApakahSesuaiJadwal(pertemuan))
						return;

					pertemuan.masukkanData("online");
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Grup Whatsapp', w: 1200, h: 600});");
					}
					if (eventListener != null)
						eventListener.onEvent(null);
				}
			});

		} else if (pertemuan.getOnlineMenggunakan().equals(Pertemuan.LAIN)) {
			if (hbox != null)
				a.setParent(hbox);

			TreeMap<String, String> d = pertemuan.ambilData("online", null);
			StringBuilder onlBuilder = new StringBuilder();
			for (String user : d.keySet()) {
				try {
					String jam = d.get(user);
					String[] u = user.split("-");
					if (u[2].equalsIgnoreCase("Dosen") || u[2].equalsIgnoreCase("Mahasiswa")
							|| u[2].equalsIgnoreCase("CalonMahasiswa") || u[2].equalsIgnoreCase("CalonSiswa")
							|| u[2].equalsIgnoreCase("Guru") || u[2].equalsIgnoreCase("Siswa")) {
						if (onlBuilder.length() > 0)
							onlBuilder.append(",");
						onlBuilder.append(u[0]).append(" (").append(jam).append(")");
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:9166");
				}
			}
			if (onlBuilder.length() > 0) {
				a.setTooltiptext(onlBuilder.toString());
			}

			a.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {
					String server = pertemuan.getLainLink();
					if (server == null || server.trim().isEmpty()) {
						MyMessageboxConfig.show("Mohon maaf, link pertemuan online belum dimasukkan atau belum valid. Langkah yang dapat dilakukan: (1) buka pengaturan pertemuan ini; (2) masukkan link yang benar pada kolom Link; (3) simpan dan coba lagi. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
					if (!checkApakahSesuaiJadwal(pertemuan))
						return;

					pertemuan.masukkanData("online");
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");
					}
					if (eventListener != null)
						eventListener.onEvent(null);
				}
			});
		} else {
			a.setVisible(false);
		}

		return a;
	}

	public static void createLive(final Pertemuan pertemuan, final EventListener myEventListener, Component hbox) {
		if (Common.bolehKonfigurasi("aktifkan_live_streaming_baru", Konfigurasi.TIDAK_AKTIF)) {
			final MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Live",
					"/img/broadcast-station-icon.png");
			String kodeStreamCari = Common.simpleDateFormat1.get().format(ais.ui.util.WaktuUtil.getDate()) + "p"
					+ pertemuan.getId();

			String kodeStreamTemp = kodeStreamCari;
			boolean ada = false;
			for (String kode : LiveStreamingPlayerWindow.steams.keySet()) {
				if (kode.startsWith(kodeStreamCari)) {
					kodeStreamTemp = kode;
					toolbarbutton.setLabel("Sedang Online");
					toolbarbutton.setImage("/img/online-red-icon.png");
					ada = true;
					break;
				}
			}

			if (!ada) {
				if (LiveStreamingPlayerWindow.steams.containsKey(kodeStreamCari)) {
					kodeStreamTemp = kodeStreamCari;
					toolbarbutton.setLabel("Sedang Online");
					toolbarbutton.setImage("/img/online-red-icon.png");
				}
			}

			Tbmuser tbmuser = Common.getCurrentUser();
			toolbarbutton.setVisible(
					pertemuan.getPublikasikanStreaming() || toolbarbutton.getLabel().equalsIgnoreCase("Sedang Online")
							|| (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null));
			toolbarbutton.setOrient("vertical");
			hbox.appendChild(toolbarbutton);

			final String kodeStream = kodeStreamTemp;
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event a) throws Exception {
					LiveStreamingPlayerWindow liveStreamingPlayerWindow = new LiveStreamingPlayerWindow(pertemuan,
							myEventListener, kodeStream);
					liveStreamingPlayerWindow.setHeight("95%");
					liveStreamingPlayerWindow.setWidth("90%");
					liveStreamingPlayerWindow.onModal();
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	public static void createCriteriaDosen(Dosen dosen, Session session, Criteria criteria, boolean perkuliahan,
			boolean kelompokKkn, boolean kelompokPkl, boolean skripsi, boolean mahasiswaRequestTugasAkhir,
			boolean krsMahasiswa, boolean grupPertemuan, boolean formulirKegiatan, boolean dosenPengganti) {
		if (dosen != null) {
			if (perkuliahan)
				criteria.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN);
			if (kelompokKkn)
				criteria.createAlias("kelompokKkn", "kelompokKkn", Criteria.LEFT_JOIN);
			if (kelompokPkl)
				criteria.createAlias("kelompokPkl", "kelompokPkl", Criteria.LEFT_JOIN);
			if (skripsi)
				criteria.createAlias("skripsi", "skripsi", Criteria.LEFT_JOIN);
			if (mahasiswaRequestTugasAkhir)
				criteria.createAlias("mahasiswaRequestTugasAkhir", "mahasiswaRequestTugasAkhir", Criteria.LEFT_JOIN);
			if (krsMahasiswa)
				criteria.createAlias("krsMahasiswa", "krsMahasiswa", Criteria.LEFT_JOIN);
			if (grupPertemuan) {
				criteria.createAlias("pertemuanPunyaGrupPertemuan", "pertemuanPunyaGrupPertemuan", Criteria.LEFT_JOIN)
						.createAlias("pertemuanPunyaGrupPertemuan.grupPertemuan", "grupPertemuan", Criteria.LEFT_JOIN);
			}

			Criterion criterionDsn = Restrictions.sqlRestriction("false");

			if (dosenPengganti) {
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("dosenPengganti", dosen.getId()));
			}

			if (formulirKegiatan) {
				List<FormulirKegiatan> formulirKegiatans = session.createCriteria(FormulirKegiatanPeserta.class)
						.setProjection(Projections.groupProperty("formulirKegiatan"))
						.add(Restrictions.eq("dosen", dosen)).list();
				if (!formulirKegiatans.isEmpty()) {
					criterionDsn = Restrictions.or(criterionDsn,
							Restrictions.in("formulirKegiatan", formulirKegiatans));
				}
			}

			if (perkuliahan) {
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("perkuliahan.dosen1", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("perkuliahan.dosen2", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("perkuliahan.dosen3", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("perkuliahan.dosen4", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("perkuliahan.dosen5", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("perkuliahan.dosen6", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("perkuliahan.dosen7", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("perkuliahan.dosen8", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("perkuliahan.dosen9", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("perkuliahan.dosen10", dosen));
			}

			if (kelompokKkn) {
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokKkn.dosen_pembimbing1", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokKkn.dosen_pembimbing2", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokKkn.dosen_pembimbing3", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokKkn.dosen_pembimbing4", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokKkn.dosen_pembimbing5", dosen));
			}

			if (kelompokPkl) {
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokPkl.dosen_pembimbing1", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokPkl.dosen_pembimbing2", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokPkl.dosen_pembimbing3", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokPkl.dosen_pembimbing4", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokPkl.dosen_pembimbing5", dosen));
			}

			if (skripsi) {
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("skripsi.ketuaSidang", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("skripsi.penguji1", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("skripsi.penguji2", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("skripsi.penguji3", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("skripsi.penguji4", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("skripsi.pembimbing", dosen));
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("skripsi.pembimbing3", dosen));
			}

			if (mahasiswaRequestTugasAkhir) {
				criterionDsn = Restrictions.or(criterionDsn,
						Restrictions.eq("mahasiswaRequestTugasAkhir.dosen1", dosen));
				criterionDsn = Restrictions.or(criterionDsn,
						Restrictions.eq("mahasiswaRequestTugasAkhir.dosen2", dosen));
				criterionDsn = Restrictions.or(criterionDsn,
						Restrictions.eq("mahasiswaRequestTugasAkhir.dosen3", dosen));
				criterionDsn = Restrictions.or(criterionDsn,
						Restrictions.eq("mahasiswaRequestTugasAkhir.dosen4", dosen));
				criterionDsn = Restrictions.or(criterionDsn,
						Restrictions.eq("mahasiswaRequestTugasAkhir.dosen5", dosen));
				criterionDsn = Restrictions.or(criterionDsn,
						Restrictions.eq("mahasiswaRequestTugasAkhir.dosen6", dosen));
			}

			if (krsMahasiswa) {
				criterionDsn = Restrictions.or(criterionDsn,
						Restrictions.and(Restrictions.isNull("krsMahasiswa.semesterPendek"), Restrictions.and(
								Restrictions.eq("krsMahasiswa.dosenPa", dosen),
								Restrictions.eq("krsMahasiswa.tahunAkademik", Common.getCurrentTahunAkademik()))));
			}

			if (grupPertemuan) {
				criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("grupPertemuan.dosen", dosen));
			}

			criteria.add(criterionDsn);
		}
	}

	@SuppressWarnings("unchecked")
	public static void createCriteriaMahasiswa(Mahasiswa mahasiswa, Session session, Criteria criteria,
			boolean perkuliahan, boolean kelompokKkn, boolean kelompokPkl, boolean skripsi,
			boolean mahasiswaRequestTugasAkhir, boolean krsMahasiswa, boolean grupPertemuan, boolean formulirKegiatan,
			boolean wisuda) {
		if (mahasiswa != null) {

			if (skripsi)
				criteria.createAlias("skripsi", "skripsi", Criteria.LEFT_JOIN);
			if (mahasiswaRequestTugasAkhir)
				criteria.createAlias("mahasiswaRequestTugasAkhir", "mahasiswaRequestTugasAkhir", Criteria.LEFT_JOIN);
			if (krsMahasiswa)
				criteria.createAlias("krsMahasiswa", "krsMahasiswa", Criteria.LEFT_JOIN);
			if (grupPertemuan)
				criteria.createAlias("pertemuanPunyaGrupPertemuan", "pertemuanPunyaGrupPertemuan", Criteria.LEFT_JOIN);

			Criterion criterionMhs = Restrictions.sqlRestriction("false");

			if (perkuliahan) {
				List<Long> perkuliahans = mahasiswa.ambilPerkuliahanDanParalel();
				if (perkuliahans != null && !perkuliahans.isEmpty()) {
					criterionMhs = Restrictions.or(criterionMhs, Restrictions.in("perkuliahan.id", perkuliahans));
				}
			}

			if (formulirKegiatan) {
				List<FormulirKegiatan> formulirKegiatans = session.createCriteria(FormulirKegiatanPeserta.class)
						.setProjection(Projections.groupProperty("formulirKegiatan"))
						.add(Restrictions.isNotNull("formulirKegiatan")).add(Restrictions.eq("mahasiswa", mahasiswa))
						.list();
				if (!formulirKegiatans.isEmpty()) {
					criterionMhs = Restrictions.or(criterionMhs,
							Restrictions.in("formulirKegiatan", formulirKegiatans));
				}
			}

			if (wisuda) {
				List<Wisuda> wisudas = session.createCriteria(PendaftaranWisuda.class)
						.add(Restrictions.eq("persetujuanWisuda", true)).add(Restrictions.isNotNull("wisuda"))
						.setProjection(Projections.groupProperty("wisuda")).add(Restrictions.eq("mahasiswa", mahasiswa))
						.list();
				if (!wisudas.isEmpty()) {
					criterionMhs = Restrictions.or(criterionMhs, Restrictions.in("wisuda", wisudas));
				}
			}

			if (kelompokKkn) {
				List<KelompokKkn> kelompokKkns = session.createCriteria(MahasiswaDapatKelompokKkn.class)
						.setProjection(Projections.groupProperty("kelompokKkn"))
						.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("diterima", true)).list();
				if (!kelompokKkns.isEmpty()) {
					criterionMhs = Restrictions.or(criterionMhs, Restrictions.in("kelompokKkn", kelompokKkns));
				}
			}

			if (kelompokPkl) {
				List<KelompokPkl> kelompokPkls = session.createCriteria(MahasiswaDapatKelompokPkl.class)
						.setProjection(Projections.groupProperty("kelompokPkl"))
						.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("diterima", true)).list();
				if (!kelompokPkls.isEmpty()) {
					criterionMhs = Restrictions.or(criterionMhs, Restrictions.in("kelompokPkl", kelompokPkls));
				}
			}

			if (skripsi) {
				criterionMhs = Restrictions.or(criterionMhs, Restrictions.eq("skripsi.mahasiswa", mahasiswa));
			}

			if (mahasiswaRequestTugasAkhir) {
				criterionMhs = Restrictions.or(criterionMhs,
						Restrictions.eq("mahasiswaRequestTugasAkhir.mahasiswa", mahasiswa));
			}

			if (krsMahasiswa) {
				criterionMhs = Restrictions.or(criterionMhs,
						Restrictions.and(Restrictions.isNull("krsMahasiswa.semesterPendek"), Restrictions.and(
								Restrictions.eq("krsMahasiswa.mahasiswa", mahasiswa),
								Restrictions.eq("krsMahasiswa.tahunAkademik", Common.getCurrentTahunAkademik()))));
			}

			if (grupPertemuan) {
				criterionMhs = Restrictions.or(criterionMhs,
						Restrictions.eq("pertemuanPunyaGrupPertemuan.mahasiswa", mahasiswa));
			}

			criteria.add(criterionMhs);
		}
	}

	private static boolean isUjianAktifSafe(PertemuanPunyaUjian pertemuanPunyaUjian) {
		if (pertemuanPunyaUjian == null) {
			return false;
		}

		Long ujianId = null;
		Long pertemuanPunyaUjianId = null;

		try {
			pertemuanPunyaUjianId = pertemuanPunyaUjian.getId();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:9457");
		}

		try {
			ais.database.model.Ujian ujian = pertemuanPunyaUjian.getUjian();
			if (ujian == null) {
				return false;
			}

			try {
				ujianId = ujian.getId();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:9468");
			}

			try {
				Boolean aktif = ujian.getAktif();
				return aktif == null || aktif.booleanValue();
			} catch (org.hibernate.LazyInitializationException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:9474");
				// Proxy Ujian berasal dari session yang sudah tertutup. Reload ringan di bawah.
			} catch (org.hibernate.SessionException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:9476");
				// Session lama sudah tertutup. Reload ringan di bawah.
			}
		} catch (org.hibernate.LazyInitializationException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:9479");
			// Relasi Ujian belum bisa dibaca karena proxy detached. Reload ringan di bawah.
		} catch (org.hibernate.SessionException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:9481");
			// Session lama sudah tertutup. Reload ringan di bawah.
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:9483");
			// Jangan sampai satu data ujian yang bermasalah menggagalkan seluruh dashboard.
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			if (ujianId != null) {
				ais.database.model.Ujian ujian = (ais.database.model.Ujian) session.get(ais.database.model.Ujian.class,
						ujianId);
				return ujian != null && (ujian.getAktif() == null || ujian.getAktif().booleanValue());
			}

			if (pertemuanPunyaUjianId != null) {
				PertemuanPunyaUjian fresh = (PertemuanPunyaUjian) session.get(PertemuanPunyaUjian.class,
						pertemuanPunyaUjianId);
				if (fresh != null && fresh.getUjian() != null) {
					Boolean aktif = fresh.getUjian().getAktif();
					return aktif == null || aktif.booleanValue();
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardTimelinePertemuan.java:9506");
		} finally {
			closeSessionFullQuietly(session);
		}

		return false;
	}

	private static void closeHibernateSessionQuietly(Session session) {
		ais.common.ElearningSessionUtil.closeQuietly(session);
	}


	/**
	 * Tambahkan tombol <b>Tambah</b> di bawah kotak peringatan data OBE yang masih kosong.
	 *
	 * <p>Kotak peringatan (&quot;Cara mengisi:&quot;) sebelumnya hanya berisi TEKS petunjuk berisi
	 * jalur menu yang harus ditelusuri sendiri oleh pengguna. Tombol ini memangkas langkah itu:
	 * sekali klik langsung membuka layar entri datanya dalam jendela modal, sehingga pengguna
	 * bisa memilih/menambah data saat itu juga lalu menutupnya dan memuat ulang halaman.</p>
	 *
	 * <p>Layar entri dibuka lewat {@link ais.ui.util.MyInclude} (pola yang sama dipakai
	 * DasboardObeElearningHelper untuk membuka RPS OBE), sehingga composer aslinya berjalan
	 * apa adanya tanpa duplikasi logika.</p>
	 *
	 * @param induk  wadah tempat tombol ditempel (umumnya vbox konten peringatan).
	 * @param judul  judul jendela sekaligus keterangan tombol.
	 * @param srcZul path ZUL layar entri, relatif terhadap {@code /WEB-INF/z/x/y/}.
	 */
	private static void tambahTombolTambahDataObe(org.zkoss.zk.ui.Component induk, final String judul,
			final String srcZul) {
		try {
			if (induk == null) {
				return;
			}
			MyToolbarbuttonConfig tombol = new MyToolbarbuttonConfig("Tambah", "/img/svg/plus-circle.svg");
			tombol.setTooltiptext("Buka layar " + judul + " untuk menambah/memilih data");
			tombol.setStyle("font-weight:bold;color:#b26a00;");
			tombol.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					final MyWindow window = new MyWindow(judul, "none", true);
					window.setHeight("95%");
					window.setWidth("95%");
					window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

					Borderlayout bl = new ais.ui.util.MyBorderlayout();
					bl.setParent(window);

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(bl);
					Toolbar toolbarTutup = new Toolbar();
					toolbarTutup.setParent(south);
					MyToolbarbuttonConfig btnTutup = new MyToolbarbuttonConfig("Tutup",
							"/img/svg/close-circle-line.svg");
					btnTutup.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event e) throws Exception {
							window.detach();
						}
					});
					btnTutup.setParent(toolbarTutup);

					Center center = new Center();
					ais.ui.util.ZkCompat.setFlex(center, true);
					center.setStyle("overflow:auto;");
					center.setParent(bl);
					ais.ui.util.MyInclude include = new ais.ui.util.MyInclude(srcZul);
					include.setHeight("12000px");
					include.setParent(center);

					window.onModal();
				}
			});
			tombol.setParent(induk);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "DashboardTimelinePertemuan.tambahTombolTambahDataObe:" + srcZul);
		}
	}

}
