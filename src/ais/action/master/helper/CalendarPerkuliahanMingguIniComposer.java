package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.zkoss.calendar.Calendars;
import org.zkoss.calendar.api.CalendarEvent;
import org.zkoss.calendar.event.CalendarsEvent;
import org.zkoss.calendar.impl.SimpleCalendarEvent;
import org.zkoss.calendar.impl.SimpleCalendarModel;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.GrupPertemuanAction;
import ais.action.master.KrsMahasiswaAction;
import ais.action.master.MahasiswaRequestTugasAkhirAction;
import ais.action.master.SkripsiAction;
import ais.action.master.kkn.KelompokKknAction;
import ais.action.master.pkl.KelompokPklAction;
import ais.action.master.sekolah.helper.AktifitasPembelajaranHelper;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.FormulirKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Kurikulum;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaGrupPertemuan;
import ais.database.model.Ruang;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.Wisuda;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDiv;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Composer ZK untuk kalender "Agenda Minggu Ini" — tampilan read-only lintas jenis kegiatan
 * (berbeda dari {@link CalendarPerkuliahanComposer} yang berfokus pada satu ruangan dan
 * mendukung tambah/edit jadwal drag-and-drop). Satu kalender di sini dapat menampilkan
 * gabungan tujuh jenis {@link Pertemuan} sekaligus, masing-masing dapat dimatikan lewat
 * checkbox filter: perkuliahan, KKN, PKL, bimbingan tugas akhir, revisi/sidang skripsi,
 * konsultasi (KRS/dosen PA), dan konsultasi lain (grup pertemuan bebas). Filter tambahan
 * mendukung tahun akademik, semester, kelas, fakultas/jurusan (berjenjang), program, ruangan,
 * dosen, mahasiswa, dan kurikulum — bila pengguna login adalah dosen atau mahasiswa, filter
 * tersebut otomatis dipersempit ke jadwal miliknya sendiri (lewat parameter {@code dosen}/
 * {@code mahasiswa} pada {@link #ambilData}) dan sebagian kontrol filter disembunyikan/dikunci.
 *
 * <p>
 * Mengklik satu event kalender membuka jendela modal berisi rincian pertemuan
 * ({@link #displayRinci}) yang bercabang menurut jenis kegiatan pertemuan tersebut (perkuliahan,
 * jadwal pelajaran sekolah, KKN, PKL, formulir kegiatan, wisuda, skripsi, tugas akhir, KRS, atau
 * grup pertemuan), masing-masing mendelegasikan ke helper aktivitas yang sesuai (mis.
 * {@link AktifitasPerkuliahanHelper}, {@link AktifitasSkripsiHelper},
 * {@link AktifitasKrsMahasiswaHelper}, dsb.).
 * </p>
 *
 * <p>
 * Sejumlah method helper privat ({@code prepareCheckbox}, {@code safeSetEventListener},
 * {@code safeAttribute}, {@code safeValue}) sengaja dibungkus try/catch dan menerima
 * {@code null}/tipe apa pun tanpa melempar, karena banyak komponen filter (checkbox warna,
 * banbox pencarian, dsb.) bersifat opsional tergantung halaman ZUL pemanggil — tidak semua
 * halaman yang memakai composer ini menyertakan seluruh komponen tersebut.
 * </p>
 */
public class CalendarPerkuliahanMingguIniComposer extends GenericForwardComposer {

	protected static final long serialVersionUID = 201011240904L;
	protected SimpleCalendarModel cm;
	protected Calendars calendars;

	protected Combobox tahunAjaran;
	protected Combobox semester;
	protected org.zkoss.zul.Bandbox kelas;
	protected Combobox fakultas;
	protected Combobox jurusan;
	protected Combobox program;
	protected AmbilDataRuangBanbox ruang;
	protected AmbilDataDosenBanbox dosen;
	protected AmbilDataMahasiswaBanbox mahasiswa;
	protected AmbilDataKurikulumBanbox kurikulum;

	private MyCheckboxConfig jadwalPerkuliahan;
	private MyCheckboxConfig jadwalKkn;
	private MyCheckboxConfig jadwalPkl;
	private MyCheckboxConfig jadwalBimbingan;
	private MyCheckboxConfig jadwalRevisi;
	private MyCheckboxConfig jadwalKonsultasi;
	private MyCheckboxConfig jadwalKonsultasiLain;

	protected Tbmuser tbmuser = Common.getCurrentUser();

	protected SimpleDateFormat dateFormat = new SimpleDateFormat("HH.mm");

	protected Integer semesterPendek = null;

	private Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
	// private Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();

	/** Membungkus {@link #displayRinci} dalam satu baris grid ber-accordion yang selalu terbuka; dipakai sebagai isi jendela modal oleh {@link #init}. */
	public static MyGrid tampilInit(final Pertemuan pertemuan, final EventListener eventListener) throws Exception {

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);

		final MyDetail detail = new MyDetail();
		detail.setParent(row);

		MyDiv groupbox = CalendarPerkuliahanMingguIniComposer.displayRinci(row, pertemuan, eventListener);
		detail.appendChild(groupbox);
		detail.setOpen(true);
		return grid;
	}

	/**
	 * Merender rincian satu {@link Pertemuan} ke {@code row}, bercabang menurut jenis kegiatan
	 * yang terisi pada pertemuan tersebut (perkuliahan, jadwal pelajaran sekolah, KKN, PKL,
	 * formulir kegiatan, wisuda, skripsi, tugas akhir, KRS, atau grup pertemuan) — hanya satu
	 * cabang yang berlaku per pertemuan, masing-masing menampilkan header info singkat lalu
	 * mendelegasikan detail lengkap ke helper aktivitas yang sesuai jenisnya.
	 *
	 * @param row           kontainer ZK tempat header info ditambahkan
	 * @param pertemuan     pertemuan yang rinciannya ditampilkan
	 * @param eventListener diteruskan ke beberapa helper aktivitas untuk callback penyegaran
	 * @return {@link MyDiv} berisi detail lengkap kegiatan (diisi oleh helper aktivitas terkait)
	 */
	public static MyDiv displayRinci(Component row, Pertemuan pertemuan, EventListener eventListener) throws Exception {
		Tbmuser tbmuser = Common.getCurrentUser();
		Perkuliahan perkuliahan = pertemuan.getPerkuliahan();
		JadwalPelajaran jadwalPelajaran = pertemuan.getJadwalPelajaran();
		KelompokKkn kelompokKkn = pertemuan.getKelompokKkn();
		KelompokPkl kelompokPkl = pertemuan.getKelompokPkl();
		FormulirKegiatan formulirKegiatan = pertemuan.getFormulirKegiatan();
		Skripsi skripsi = pertemuan.getSkripsi();
		MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = pertemuan.getMahasiswaRequestTugasAkhir();
		KrsMahasiswa krsMahasiswa = pertemuan.getKrsMahasiswa();
		PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = pertemuan.getPertemuanPunyaGrupPertemuan();
		Wisuda wisuda = pertemuan.getWisuda();

		MyDiv groupbox = new MyDiv();
		if (perkuliahan != null) {

			row.appendChild(new MyLabelBold(perkuliahan.info()));

			AktifitasPerkuliahanHelper aktifitasPerkuliahanHelper = new AktifitasPerkuliahanHelper(
					tbmuser.getMahasiswa(), null, true);

			groupbox.setStyle("min-height: 400px;");
			int banyak = 1;
			try {
				banyak = Integer
						.parseInt(Common.getKonfigurasi("tampilan_jumlah_agenda_perkuliahan", banyak + "").getNilai());
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/CalendarPerkuliahanMingguIniComposer.java:180");
			}
			aktifitasPerkuliahanHelper.initDetail(perkuliahan, groupbox, 0, banyak);

		} else if (jadwalPelajaran != null) {

			row.appendChild(new MyLabelBold(jadwalPelajaran.info()));

			AktifitasPembelajaranHelper aktifitasPerkuliahanHelper = new AktifitasPembelajaranHelper(tbmuser.getSiswa(),
					null);

			groupbox.setStyle("min-height: 400px;");
			aktifitasPerkuliahanHelper.initDetail(jadwalPelajaran, groupbox, 0, 1);

		} else if (kelompokKkn != null) {

			Hbox hbox = new Hbox();
			hbox.setParent(row);

			Vbox vbox = new Vbox();
			hbox.appendChild(vbox);

			vbox.appendChild(new MyLabelBold(kelompokKkn.getNama_kelompok()));
			vbox.appendChild(new Label(kelompokKkn.getAlamat()));
			vbox.appendChild(new Label((kelompokKkn.getTanggal_mulai() == null ? ""
					: Common.dateFormat.get().format(kelompokKkn.getTanggal_mulai()))
					+ (kelompokKkn.getTanggal_selesai() == null ? ""
							: " s.d " + Common.dateFormat.get().format(kelompokKkn.getTanggal_selesai()))));
			vbox.appendChild(new Label(kelompokKkn.getKkn().getNama()));

			hbox.appendChild(KelompokKknAction.tampilkanInfoDosen(kelompokKkn, false, true));

			AktifitasKknHelper aktifitasPerkuliahanHelper = new AktifitasKknHelper();

			groupbox.setStyle("min-height: 400px;");
			aktifitasPerkuliahanHelper.initDetail(kelompokKkn, groupbox);

		} else if (kelompokPkl != null) {

			Hbox hbox = new Hbox();
			hbox.setParent(row);

			Vbox vbox = new Vbox();
			hbox.appendChild(vbox);

			vbox.appendChild(new MyLabelBold(kelompokPkl.getNama_kelompok()));
			vbox.appendChild(new Label(kelompokPkl.getAlamat()));
			vbox.appendChild(new Label((kelompokPkl.getTanggal_mulai() == null ? ""
					: Common.dateFormat.get().format(kelompokPkl.getTanggal_mulai()))
					+ (kelompokPkl.getTanggal_selesai() == null ? ""
							: " s.d " + Common.dateFormat.get().format(kelompokPkl.getTanggal_selesai()))));
			vbox.appendChild(new Label(kelompokPkl.getPkl().getNama()));
			hbox.appendChild(KelompokPklAction.tampilkanInfoDosen(kelompokPkl, false, true));

			AktifitasPklHelper aktifitasPerkuliahanHelper = new AktifitasPklHelper();

			groupbox.setStyle("min-height: 400px;");
			aktifitasPerkuliahanHelper.initDetail(kelompokPkl, groupbox);

		} else if (formulirKegiatan != null) {

			Hbox hbox = new Hbox();
			hbox.setParent(row);

			hbox.appendChild(new MyLabelBold(formulirKegiatan.getNama()));

			AktifitasFormulirKegiatanHelper aktifitasFormulirKegiatanHelper = new AktifitasFormulirKegiatanHelper();

			groupbox.setStyle("min-height: 400px;");
			aktifitasFormulirKegiatanHelper.initDetail(formulirKegiatan, groupbox);

		} else if (wisuda != null) {

			Hbox hbox = new Hbox();
			hbox.setParent(row);

			hbox.appendChild(new MyLabelBold(wisuda.getMoto()));
			hbox.appendChild(new MyLabelAgakKecil(wisuda.getKeterangan()));

			AktifitasWisudaHelper aktifitasFormulirKegiatanHelper = new AktifitasWisudaHelper();

			groupbox.setStyle("min-height: 400px;");
			aktifitasFormulirKegiatanHelper.initDetail(wisuda, groupbox);

		} else if (skripsi != null) {

			Hbox hbox = new Hbox();
			row.appendChild(hbox);
			hbox.appendChild(SkripsiAction.tampilkanInfoMahasiswa(skripsi, eventListener));
			hbox.appendChild(SkripsiAction.tampilkanInfoDosen(skripsi, false, true));

			AktifitasSkripsiHelper aktifitasPerkuliahanHelper = new AktifitasSkripsiHelper();

			groupbox.setStyle("min-height: 400px;");
			aktifitasPerkuliahanHelper.initDetail(skripsi, groupbox);

		} else if (mahasiswaRequestTugasAkhir != null) {

			Hbox hbox = new Hbox();
			row.appendChild(hbox);
			hbox.appendChild(
					MahasiswaRequestTugasAkhirAction.tampilkanInfoMahasiswa(mahasiswaRequestTugasAkhir, eventListener));
			hbox.appendChild(MahasiswaRequestTugasAkhirAction.tampilkanInfoDosen(mahasiswaRequestTugasAkhir, true));

			AktifitasTugasAkhirHelper aktifitasPerkuliahanHelper = new AktifitasTugasAkhirHelper();

			groupbox.setStyle("min-height: 400px;");
			aktifitasPerkuliahanHelper.initDetail(mahasiswaRequestTugasAkhir, groupbox);

		} else if (krsMahasiswa != null) {

			String krs = krsMahasiswa.getMahasiswa().rubahKeteranganPengambilanKRS(krsMahasiswa.getSemester(),
					krsMahasiswa.getTahapan(), krsMahasiswa.getSemesterPendek(), krsMahasiswa, false);

			final Html html = new ais.ui.util.MyHtml(krs);
			final Html komentarshtml = new ais.ui.util.MyHtml("");
			final MyLabelAgakKecil catatan = new MyLabelAgakKecil(krsMahasiswa.getCatatan());
			final MyLabelAgakKecil catatanKhs = new MyLabelAgakKecil(krsMahasiswa.getCatatanKhs());

			Box hbox = Common.isMobile() ? new Vbox() : new Hbox();
			row.appendChild(hbox);
			KrsMahasiswaAction.displayRow(hbox, krsMahasiswa, html, komentarshtml, catatan, catatanKhs, eventListener);

			AktifitasKrsMahasiswaHelper aktifitasKrsMahasiswaHelper = new AktifitasKrsMahasiswaHelper();

			groupbox.setStyle("min-height: 500px;");
			aktifitasKrsMahasiswaHelper.initDetail(krsMahasiswa, groupbox);

		} else if (pertemuanPunyaGrupPertemuan != null) {

			Hbox hbox = new Hbox();
			row.appendChild(hbox);

			CommonMedia.tampilkanGambarKecil(pertemuan.getPertemuanPunyaGrupPertemuan().getGrupPertemuan().getDosen())
					.setParent(hbox);
			hbox.appendChild(new Space());
			hbox.appendChild(GrupPertemuanAction.tampilkanInfoMahasiswa(pertemuan.getPertemuanPunyaGrupPertemuan()));

			groupbox.setStyle("min-height: 500px;");
			PenjadwalanGrupPertemuanHelper.displayRow(groupbox, pertemuanPunyaGrupPertemuan);

		}
		return groupbox;
	}

	/** Membuka jendela modal (hampir layar penuh) berisi {@link #tampilInit} untuk {@code pertemuan}, dengan tombol Tutup yang memicu {@code eventListener} sebelum menutup jendela. Dijalankan asinkron lewat {@link Common#createDefaultTimer}. */
	@SuppressWarnings({})
	public static void init(final Pertemuan pertemuan, final EventListener eventListener) throws Exception {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final Window addWindow = new Window("", "none", false);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
				addWindow.setHeight("99%");
				addWindow.setWidth("99%");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(addWindow);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				center.appendChild(tampilInit(pertemuan, eventListener));

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
						eventListener.onEvent(event);
						addWindow.detach();
					}
				});
				cancel.setParent(toolbar);

				addWindow.onModal();
			}
		});
	}

	/** Mundur satu minggu: menggeser {@link #calendar} referensi, membangun ulang model, lalu memindahkan halaman kalender ZK ke periode sebelumnya. */
	public void onBack(Event event) {
		calendar.set(Calendar.WEEK_OF_MONTH, calendar.get(Calendar.WEEK_OF_MONTH) - 1);
		initCalendarModel();
		calendars.previousPage();
	}

	/** Maju satu minggu: menggeser {@link #calendar} referensi, membangun ulang model, lalu memindahkan halaman kalender ZK ke periode berikutnya. */
	public void onNext(Event event) {
		calendar.set(Calendar.WEEK_OF_MONTH, calendar.get(Calendar.WEEK_OF_MONTH) + 1);
		initCalendarModel();
		calendars.nextPage();
	}

	/** Membangun ulang model kalender (asinkron) sesuai filter saat ini dan meminta ZK me-render ulang komponen kalender bila ada. */
	public void onRefresh(Event event) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initCalendarModel();
				if (calendars != null) {
					calendars.invalidate();
				}
			}
		});

	}

	/** Cek keamanan standar layar sebelum komponen ZK dirakit. */
	@Override
	public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private Row row1;
	private Row row2;


	/** Menerapkan gaya visual (border, radius, shadow) pada komponen kalender bila tersedia; kegagalan diabaikan diam-diam. */
	private void configureCalendarUi() {
		try {
			if (calendars != null) {
				calendars.setWidth("100%");
				calendars.setHeight("100%");
				calendars.setStyle("border:1px solid #dbe3ef; border-radius:18px; overflow:hidden; "
						+ "background:#ffffff; box-shadow:0 12px 28px rgba(15,23,42,.08);");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanMingguIniComposer.java:413");
		}
	}

	/** Mewarnai label checkbox filter jenis jadwal sesuai palet warna kalender ({@link Pertemuan#warnas}, indeks {@code warnaIndex}) dan mencentangnya secara default; komponen {@code null} (tidak ada di halaman ZUL) dilewati. */
	private void prepareCheckbox(MyCheckboxConfig checkbox, int warnaIndex) {
		try {
			if (checkbox != null) {
				checkbox.setStyle("color:" + Pertemuan.warnas.get(warnaIndex).split(",")[0] + "; font-weight:700;");
				checkbox.setChecked(true);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanMingguIniComposer.java:423");
		}
	}

	/** Memasang {@code listener} pada komponen banbox pencarian (kelas/ruang/dosen/mahasiswa/kurikulum) bila tipenya cocok salah satu dari lima jenis banbox yang dikenal; tipe lain atau {@code null} diabaikan diam-diam. */
	private void safeSetEventListener(Object component, final EventListener listener) {
		try {
			if (component instanceof AmbilDataKelasBanbox) {
				((AmbilDataKelasBanbox) component).setEventListener(listener);
			} else if (component instanceof AmbilDataRuangBanbox) {
				((AmbilDataRuangBanbox) component).setEventListener(listener);
			} else if (component instanceof AmbilDataDosenBanbox) {
				((AmbilDataDosenBanbox) component).setEventListener(listener);
			} else if (component instanceof AmbilDataMahasiswaBanbox) {
				((AmbilDataMahasiswaBanbox) component).setEventListener(listener);
			} else if (component instanceof AmbilDataKurikulumBanbox) {
				((AmbilDataKurikulumBanbox) component).setEventListener(listener);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanMingguIniComposer.java:440");
		}
	}

	/** Mengambil atribut ZK {@code key} dari {@code component} bila berupa {@link org.zkoss.zk.ui.Component}; mengembalikan {@code null} untuk tipe lain atau bila atribut tidak ada. */
	private Object safeAttribute(Object component, String key) {
		try {
			if (component instanceof org.zkoss.zk.ui.Component) {
				return ((org.zkoss.zk.ui.Component) component).getAttribute(key);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanMingguIniComposer.java:449");
		}
		return null;
	}

	/** Mengambil nilai teks dari {@code component} bila berupa {@link org.zkoss.zul.Bandbox} atau {@link org.zkoss.zul.Textbox} (di-trim); mengembalikan string kosong untuk tipe lain, {@code null} value, atau kegagalan apa pun. */
	private String safeValue(Object component) {
		try {
			if (component instanceof org.zkoss.zul.Bandbox) {
				String value = ((org.zkoss.zul.Bandbox) component).getValue();
				return value == null ? "" : value.trim();
			}
			if (component instanceof org.zkoss.zul.Textbox) {
				String value = ((org.zkoss.zul.Textbox) component).getValue();
				return value == null ? "" : value.trim();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanMingguIniComposer.java:464");
		}
		return "";
	}

	/**
	 * Inisialisasi pasca-render: mewarnai & mencentang seluruh checkbox filter jenis jadwal,
	 * memasang listener refresh pada banbox pencarian (kelas/ruang/dosen/mahasiswa/kurikulum),
	 * mengisi combobox semester (1-23) dan tahun ajaran, mengatur jam kerja/zona waktu/gaya
	 * kalender, mengisi combobox fakultas/jurusan berjenjang dan program, mempersempit &
	 * mengunci filter fakultas/jurusan bila pengguna login punya batasan hak akses, serta
	 * menyembunyikan filter tahun ajaran/kelas untuk dosen/mahasiswa yang login. Semua akses ke
	 * komponen filter memakai method {@code safe*} agar komponen yang tidak ada di halaman ZUL
	 * pemanggil tidak menyebabkan {@link NullPointerException}. Diakhiri dengan pemuatan awal
	 * kalender ({@link #onRefresh}) dan (pada mobile) menyembunyikan baris filter tambahan.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		prepareCheckbox(jadwalPerkuliahan, 0);
		prepareCheckbox(jadwalKkn, 1);
		prepareCheckbox(jadwalPkl, 2);
		prepareCheckbox(jadwalBimbingan, 3);
		prepareCheckbox(jadwalRevisi, 4);
		prepareCheckbox(jadwalKonsultasi, 5);
		prepareCheckbox(jadwalKonsultasiLain, 6);

		safeSetEventListener(kelas, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		safeSetEventListener(dosen, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		safeSetEventListener(mahasiswa, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		safeSetEventListener(kurikulum, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		safeSetEventListener(ruang, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		if (semester != null) {
			for (int i = 1; i <= 23; i++) {
				org.zkoss.zul.Comboitem comboitemSemester = new org.zkoss.zul.Comboitem();
				comboitemSemester.setLabel(i + "");
				comboitemSemester.setValue(i);
				semester.appendChild(comboitemSemester);
			}
		}

		if (tahunAjaran != null) {
			Common.generateTahunAjaran(tahunAjaran);
			org.zkoss.zul.Comboitem comboitemTahun = new org.zkoss.zul.Comboitem();
			comboitemTahun.setLabel("Semua");
			comboitemTahun.setValue(null);
			tahunAjaran.appendChild(comboitemTahun);
			tahunAjaran.setSelectedItem(comboitemTahun);
		}

		if (calendars != null) {
			calendars.setTimeslots(4);
			configureCalendarUi();
			Konfigurasi penjadwalanjamMulai = Common.getKonfigurasi("penjadwalan_jam_mulai", Konfigurasi.AKTIF, "7", "",
					"");
			Konfigurasi penjadwalanjamSelesai = Common.getKonfigurasi("penjadwalan_jam_selesai", Konfigurasi.AKTIF, "23",
					"", "");
			Konfigurasi penjadwalanTimezone = Common.getKonfigurasi("penjadwalan_timezone", Konfigurasi.AKTIF,
					"Jakarta=GMT+7", "", "");

			if (penjadwalanTimezone.getNilai().equals(Konfigurasi.AKTIF)) {
				calendars.setTimeZone(penjadwalanTimezone.getInfo1());
			}

			if (penjadwalanjamMulai.getNilai().equals(Konfigurasi.AKTIF)) {
				Integer mulai = 7;
				try {
					mulai = Integer.parseInt(penjadwalanjamMulai.getInfo1().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanMingguIniComposer.java:556");
				}
				calendars.setBeginTime(mulai);
			}
			if (penjadwalanjamSelesai.getNilai().equals(Konfigurasi.AKTIF)) {
				Integer sampai = 23;
				try {
					sampai = Integer.parseInt(penjadwalanjamSelesai.getInfo1().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanMingguIniComposer.java:564");
				}
				calendars.setEndTime(sampai);
			}
		}

		if (jurusan != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		}

		if (fakultas != null) {
			Common.insertCombo(fakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		}
		class FakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				if (jurusan == null || fakultas == null) {
					return;
				}
				Common.clear(jurusan);
				jurusan.setSelectedItem(null);
				if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
			}

		}

		if (fakultas != null) {
			fakultas.addEventListener("onChange", new FakultasEventListener());
		}

		if (program != null) {
			Common.initPrograms(program);
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilFakultas() != null && fakultas != null) {
			Common.selectComboItem(fakultas, tbmuser.ambilFakultas());
			if (jurusan != null) {
				Common.clear(jurusan);
				Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			}
			fakultas.setDisabled(true);
		} else if (fakultas != null) {
			fakultas.setDisabled(false);
		}

		if (tbmuser != null && tbmuser.ambilJurusan() != null && jurusan != null) {
			Common.pilihJurusan(jurusan, tbmuser.ambilJurusan());
			jurusan.setDisabled(true);
		} else if (jurusan != null) {
			jurusan.setDisabled(false);
		}

		if (tahunAjaran != null && tahunAjaran.getParent() != null) {
			tahunAjaran.getParent()
					.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);
		}
		if (kelas != null && kelas.getParent() != null) {
			kelas.getParent().setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);
		}

		if (calendars != null) {
			calendars.addEventListener(Events.ON_CHANGE, new EventListener() {


			@Override
			public void onEvent(Event arg0) throws Exception {
				System.out.println(
						"======================================= on Chnage ==========================================");
			}
		});
		}
		onRefresh(null);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (row1 != null && row2 != null && Common.isMobile()) {
					row1.setVisible(false);
					row2.setVisible(false);
				}
			}
		});

	}

	/**
	 * Mengambil seluruh {@link Pertemuan} aktif dalam rentang tanggal {@code mulai}..{@code sampai}
	 * yang cocok dengan filter jenis jadwal (checkbox — jenis yang tidak dicentang disaring
	 * dengan mensyaratkan kolom relasinya {@code null}) DAN filter kepemilikan:
	 * <ul>
	 * <li>Bila {@code dosen} diberikan: OR atas seluruh peran dosen yang mungkin (pengajar 1-10
	 * pada perkuliahan, pembimbing 1-5 KKN/PKL, ketua sidang/penguji 1-4/pembimbing skripsi,
	 * dosen 1-6 tugas akhir, dosen PA KRS) — parameter fakultas/jurusan/dll. lain DIABAIKAN.</li>
	 * <li>Selain itu, bila {@code mahasiswa} diberikan: OR atas subquery SQL native ke tabel
	 * relasi kepesertaan (detailperkuliahan, kelompok KKN/PKL, skripsi, tugas akhir, KRS, grup
	 * pertemuan) — parameter fakultas/jurusan/dll. lain juga DIABAIKAN.</li>
	 * <li>Bila keduanya {@code null}: filter umum berlaku (kurikulum, tahun akademik, semester,
	 * kelas, ruang, fakultas, jurusan, program), masing-masing {@code null} berarti tidak
	 * menyaring.</li>
	 * </ul>
	 * Hanya salah satu dari tiga cabang (dosen/mahasiswa/filter umum) yang aktif per panggilan.
	 *
	 * @return daftar pertemuan yang cocok, tidak diurutkan secara eksplisit
	 */
	@SuppressWarnings("unchecked")
	public static List<Pertemuan> ambilData(String tahunAkademik, Integer semester, String kelas, Fakultas fakultas,
			Jurusan jurusan, String program, Ruang ruang, Dosen dosen, Kurikulum myKurikulum, Mahasiswa mahasiswa,
			Date mulai, Date sampai, MyCheckboxConfig jadwalPerkuliahan, MyCheckboxConfig jadwalKkn,
			MyCheckboxConfig jadwalPkl, MyCheckboxConfig jadwalRevisi, MyCheckboxConfig jadwalKonsultasi,
			MyCheckboxConfig jadwalBimbingan, MyCheckboxConfig jadwalKonsultasiLain) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pertemuan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(jadwalPerkuliahan == null || jadwalPerkuliahan.isChecked() ? Restrictions.sqlRestriction("true")
						: Restrictions.isNull("perkuliahan"))
				.add(jadwalKkn == null || jadwalKkn.isChecked() ? Restrictions.sqlRestriction("true")
						: Restrictions.isNull("kelompokKkn"))
				.add(jadwalPkl == null || jadwalPkl.isChecked() ? Restrictions.sqlRestriction("true")
						: Restrictions.isNull("kelompokPkl"))
				.add(jadwalRevisi == null || jadwalRevisi.isChecked() ? Restrictions.sqlRestriction("true")
						: Restrictions.isNull("skripsi"))
				.add(jadwalKonsultasi == null || jadwalKonsultasi.isChecked() ? Restrictions.sqlRestriction("true")
						: Restrictions.isNull("krsMahasiswa"))
				.add(jadwalBimbingan == null || jadwalBimbingan.isChecked() ? Restrictions.sqlRestriction("true")
						: Restrictions.isNull("mahasiswaRequestTugasAkhir"))

				.add(jadwalKonsultasiLain == null || jadwalKonsultasiLain.isChecked()
						? Restrictions.sqlRestriction("true")
						: Restrictions.isNull("pertemuanPunyaGrupPertemuan"));

		if (dosen != null) {

			criteria.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)
					.createAlias("kelompokKkn", "kelompokKkn", Criteria.LEFT_JOIN)
					.createAlias("kelompokPkl", "kelompokPkl", Criteria.LEFT_JOIN)
					.createAlias("skripsi", "skripsi", Criteria.LEFT_JOIN)
					.createAlias("mahasiswaRequestTugasAkhir", "mahasiswaRequestTugasAkhir", Criteria.LEFT_JOIN)
					.createAlias("krsMahasiswa", "krsMahasiswa", Criteria.LEFT_JOIN);

			Criterion criterionDsn = Restrictions.eq("perkuliahan.dosen1", dosen);
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("perkuliahan.dosen2", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("perkuliahan.dosen3", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("perkuliahan.dosen4", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("perkuliahan.dosen5", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("perkuliahan.dosen6", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("perkuliahan.dosen7", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("perkuliahan.dosen8", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("perkuliahan.dosen9", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("perkuliahan.dosen10", dosen));

			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokKkn.dosen_pembimbing1", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokKkn.dosen_pembimbing2", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokKkn.dosen_pembimbing3", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokKkn.dosen_pembimbing4", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokKkn.dosen_pembimbing5", dosen));

			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokPkl.dosen_pembimbing1", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokPkl.dosen_pembimbing2", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokPkl.dosen_pembimbing3", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokPkl.dosen_pembimbing4", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("kelompokPkl.dosen_pembimbing5", dosen));

			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("skripsi.ketuaSidang", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("skripsi.penguji1", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("skripsi.penguji2", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("skripsi.penguji3", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("skripsi.penguji4", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("skripsi.pembimbing", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("skripsi.pembimbing3", dosen));

			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("mahasiswaRequestTugasAkhir.dosen1", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("mahasiswaRequestTugasAkhir.dosen2", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("mahasiswaRequestTugasAkhir.dosen3", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("mahasiswaRequestTugasAkhir.dosen4", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("mahasiswaRequestTugasAkhir.dosen5", dosen));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("mahasiswaRequestTugasAkhir.dosen6", dosen));

			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("krsMahasiswa.dosenPa", dosen));

			// String sql = "this_.pertemuan_punya_grup_pertemuan in (select
			// b.id from grup_pertemuan b where b.dosen=" + dosen.getId()
			// + ")";
			//
			// criterionDsn = Restrictions.or(criterionDsn,
			// Restrictions.sqlRestriction(sql));

			criteria.add(criterionDsn);

		}

		else if (mahasiswa != null) {
			String sql = "this_.perkuliahan in (select perkuliahan from detailperkuliahan a where a.mahasiswa="
					+ mahasiswa.getId() + " group by perkuliahan)";
			Criterion criterionMhs = Restrictions.sqlRestriction(sql);

			sql = "this_.kelompok_kkn in (select kelompok_kkn from mahasiswa_dapat_kelompok_kelompok_kkn a where a.mahasiswa="
					+ mahasiswa.getId() + " group by kelompok_kkn)";

			criterionMhs = Restrictions.or(criterionMhs, Restrictions.sqlRestriction(sql));

			sql = "this_.kelompok_pkl in (select kelompok_pkl from mahasiswa_dapat_kelompok_kelompok_pkl a where a.mahasiswa="
					+ mahasiswa.getId() + " group by kelompok_pkl)";

			criterionMhs = Restrictions.or(criterionMhs, Restrictions.sqlRestriction(sql));

			sql = "this_.skripsi in (select id from skripsi a where a.mahasiswa=" + mahasiswa.getId() + ")";

			criterionMhs = Restrictions.or(criterionMhs, Restrictions.sqlRestriction(sql));

			sql = "this_.mahasiswa_request_tugas_akhir in (select id from mahasiswa_request_tugas_akhir a where a.mahasiswa="
					+ mahasiswa.getId() + ")";

			criterionMhs = Restrictions.or(criterionMhs, Restrictions.sqlRestriction(sql));

			sql = "this_.krs_mahasiswa in (select id from krs_mahasiswa a where a.mahasiswa=" + mahasiswa.getId() + ")";

			criterionMhs = Restrictions.or(criterionMhs, Restrictions.sqlRestriction(sql));

			sql = "this_.id in (select pertemuan from pertemuan_punya_grup_pertemuan a where a.mahasiswa="
					+ mahasiswa.getId() + ")";

			criterionMhs = Restrictions.or(criterionMhs, Restrictions.sqlRestriction(sql));

			criteria.add(criterionMhs);

		} else {
			criteria.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)
					.createAlias("perkuliahan.jurusan", "jurusan", Criteria.LEFT_JOIN)

					.add(myKurikulum == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("perkuliahan.kurikulum", myKurikulum))

					.add(tahunAkademik == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("perkuliahan.tahunAjaran", tahunAkademik))

					.add(semester == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("perkuliahan.semester", semester))

					.add(kelas == null || kelas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("perkuliahan.kelas", kelas))

					.add(ruang == null ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.eq("ruang", fakultas),
									Restrictions.eq("perkuliahan.ruang", ruang)));

			criteria.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
					: Restrictions.or(Restrictions.eq("jurusan.fakultas", fakultas),
							Restrictions.eq("fakultasId", fakultas.getId())))

					.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.eq("jurusanId", jurusan.getId()),
									Restrictions.eq("perkuliahan.jurusan", jurusan)))

					.add(program == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.eq("program", program),
									Restrictions.eq("perkuliahan.program", program)));

		}

		criteria.add(Restrictions.between("tanggal", mulai, sampai));

		return criteria.list();
	}

	/**
	 * Menghasilkan laporan PDF "SKS Dosen Periode" dari {@link #pertemuan} yang sedang
	 * ditampilkan di kalender: mengumpulkan seluruh kombinasi unik dosen+pertemuan (satu
	 * pertemuan dapat melibatkan beberapa dosen), diurutkan otomatis via {@link TreeMap}, dengan
	 * baris laporan yang deskripsinya bercabang menurut jenis kegiatan pertemuan (perkuliahan,
	 * pembimbing KKN/PKL, sidang skripsi, pembimbing skripsi/TA, pembimbing akademik KRS, atau
	 * grup pertemuan). Periode laporan diambil dari rentang tanggal minimum-maksimum pertemuan
	 * yang tampil.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onAgendaDosen(Event event) throws Exception {
		if (pertemuan != null) {
			Map parameters = ais.common.HashMapGenerator.getRand();

			Date tanggalMulai = null;
			Date tanggalSampai = null;
			TreeMap<String, Object[]> treeMap = new TreeMap<String, Object[]>();
			for (Pertemuan p : pertemuan) {
				for (Dosen dosen : p.ambilDosen()) {
					treeMap.put(dosen.getId() + "_" + Common.dateFormat8.get().format(p.getTanggal()) + "_"
							+ p.getWaktuMulai() + "_" + p.getWaktuSelesai(), new Object[] { p, dosen });
				}

				if (tanggalMulai == null || p.getTanggal().before(tanggalMulai)) {
					tanggalMulai = p.getTanggal();
				}
				if (tanggalSampai == null || p.getTanggal().after(tanggalSampai)) {
					tanggalSampai = p.getTanggal();
				}
			}

			parameters.put("periode", (tanggalMulai == null ? "" : Common.dateFormat4.get().format(tanggalMulai))
					+ (tanggalSampai == null ? "" : " s.d " + Common.dateFormat4.get().format(tanggalSampai)));

			List<Map> maps = new ArrayList<Map>();
			for (String key : treeMap.keySet()) {
				Object[] o = treeMap.get(key);
				Pertemuan p = (Pertemuan) o[0];
				Dosen d = (Dosen) o[1];
				Map map = new java.util.HashMap();
				map.put("dosen1", d.getId());
				map.put("nama_dosen", d.getNama());
				map.put("waktu", Common.dateFormat4.get().format(p.getTanggal()) + ", " + p.getWaktuMulai() + " s.d "
						+ p.getWaktuSelesai());
				if (p.getPerkuliahan() != null && p.getPerkuliahan().getMatakuliah() != null) {
					map.put("matakuliah", p.getPerkuliahan().getMatakuliah().getKode() + "-"
							+ p.getPerkuliahan().getMatakuliah().getNama());
					map.put("smt_kls", p.getPerkuliahan().getSemester() + " / " + p.getPerkuliahan().getKelas());
					map.put("jumlah_mhs", p.getPerkuliahan().ambilJumlahDetailperkuliahan());
				} else if (p.getKelompokKkn() != null) {
					map.put("matakuliah", "Pembimbing KKN " + p.getKelompokKkn().getNama_kelompok());
					map.put("smt_kls", p.getKelompokKkn().getNama_kelompok());
					map.put("jumlah_mhs", p.getKelompokKkn().ambilJumlahDetailperkuliahanLangsung());
				} else if (p.getKelompokPkl() != null) {
					map.put("matakuliah", "Pembimbing PKL " + p.getKelompokPkl().getNama_kelompok());
					map.put("smt_kls", p.getKelompokPkl().getNama_kelompok());
					map.put("jumlah_mhs", p.getKelompokPkl().ambilJumlahDetailperkuliahanLangsung());
				} else if (p.getSkripsi() != null) {
					map.put("matakuliah", "Sidang Skripsi/TA/Thesis \"" + p.getSkripsi().getMahasiswa().getNim() + " "
							+ p.getSkripsi().getMahasiswa().getNama() + "\"");
					map.put("smt_kls", p.getSkripsi().getSemester() + " / " + p.getSkripsi().getMahasiswa().getKelas());
					map.put("jumlah_mhs", 1);
				} else if (p.getMahasiswaRequestTugasAkhir() != null) {
					map.put("matakuliah",
							"Pembimbing Skripsi/TA/Thesis \""
									+ p.getMahasiswaRequestTugasAkhir().getMahasiswa().getNim() + " "
									+ p.getMahasiswaRequestTugasAkhir().getMahasiswa().getNama() + "\"");
					map.put("smt_kls", p.getMahasiswaRequestTugasAkhir().getSemester() + " / "
							+ p.getMahasiswaRequestTugasAkhir().getMahasiswa().getKelas());
					map.put("jumlah_mhs", 1);
				} else if (p.getKrsMahasiswa() != null) {
					map.put("matakuliah", "Pembimbing Akademik \"" + p.getKrsMahasiswa().getMahasiswa().getNim() + " "
							+ p.getKrsMahasiswa().getMahasiswa().getNama() + "\"");
					map.put("smt_kls",
							p.getKrsMahasiswa().getSemester() + " / " + p.getKrsMahasiswa().getMahasiswa().getKelas());
					map.put("jumlah_mhs", 1);
				} else if (p.getPertemuanPunyaGrupPertemuan() != null) {
					map.put("matakuliah", p.getPertemuanPunyaGrupPertemuan().getGrupPertemuan().getNama());
					map.put("smt_kls", p.getPertemuanPunyaGrupPertemuan().getMahasiswa().currentSemester() + " / "
							+ p.getPertemuanPunyaGrupPertemuan().getMahasiswa().getKelas());
					map.put("jumlah_mhs", 1);
				}

				maps.add(map);
			}
			parameters.put("maps", maps);
			Report.generatePDFReport(Report.PDF, parameters, "sks_dosen_periode", ais.ui.util.WaktuUtil.getDate());
		}
	}

	private List<Pertemuan> pertemuan = null;

	/**
	 * Membangun ulang model kalender ({@link SimpleCalendarModel}) untuk rentang satu minggu
	 * sebelum dan sesudah {@link #calendar} referensi (total ~3 minggu jendela tampil),
	 * membaca seluruh nilai filter dari komponen ZK (lewat method {@code safe*}) dan
	 * mendelegasikan pengambilan data ke {@link #ambilData}. Setiap {@link Pertemuan} hasil
	 * diubah menjadi event kalender lewat
	 * {@code CalendarPerkuliahanBulanIniComposer.createEvent} dan hasilnya disimpan ke
	 * {@link #pertemuan} (dipakai ulang oleh {@link #onAgendaDosen}).
	 */
	protected void initCalendarModel() {

		String tahunAkademik = tahunAjaran == null || tahunAjaran.getSelectedItem() == null || tahunAjaran.getSelectedItem().getValue() == null
				? null
				: tahunAjaran.getSelectedItem().getValue().toString();
		Integer semester = (Integer) (this.semester == null || this.semester.getSelectedItem() == null ? null
				: this.semester.getSelectedItem().getValue());
		String kelas = safeValue(this.kelas);
		Fakultas fakultas = (Fakultas) (this.fakultas == null || this.fakultas.getSelectedItem() == null
				|| this.fakultas.getSelectedItem().getValue() == null ? null
						: this.fakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (this.jurusan == null || this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());
		String program = (String) (this.program == null || this.program.getSelectedItem() == null
				|| this.program.getSelectedItem().getValue() == null ? null
						: this.program.getSelectedItem().getValue());

		Ruang ruang = (Ruang) safeAttribute(this.ruang, "ruang");
		Dosen myDosen = (Dosen) safeAttribute(dosen, "dosen");
		Kurikulum myKurikulum = (Kurikulum) safeAttribute(kurikulum, "kurikulum");

		Mahasiswa myMahasiswa = (Mahasiswa) safeAttribute(mahasiswa, "mahasiswa");

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(this.calendar.getTime());
		calendar.set(Calendar.WEEK_OF_MONTH, calendar.get(Calendar.WEEK_OF_MONTH) - 1);
		Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
		calendar1.setTime(this.calendar.getTime());
		calendar1.set(Calendar.WEEK_OF_MONTH, calendar1.get(Calendar.WEEK_OF_MONTH) + 1);

		cm = new SimpleCalendarModel();

		pertemuan = CalendarPerkuliahanMingguIniComposer.ambilData(tahunAkademik, semester, kelas, fakultas, jurusan,
				program, ruang, myDosen, myKurikulum, myMahasiswa, calendar.getTime(), calendar1.getTime(),
				jadwalPerkuliahan, jadwalKkn, jadwalPkl, jadwalRevisi, jadwalKonsultasi, jadwalBimbingan,
				jadwalKonsultasiLain);
		for (Pertemuan myPertemuan : pertemuan) {
			cm.add(CalendarPerkuliahanBulanIniComposer.createEvent(myPertemuan));
		}
		if (calendars != null) {
			calendars.setModel(cm);
		}
	}

	/** Ditangkap saat pengguna mencoba menggambar event baru pada kalender ini; kalender ini bersifat read-only (agenda gabungan lintas jenis, bukan editor jadwal), jadi hanya mencegah ZK menghapus ghost event tanpa membuat data apa pun. */
	public void onEventCreate$calendars(ForwardEvent event) throws Exception {

		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();

		evt.stopClearGhost();
	}

	/**
	 * Ditangkap saat pengguna mengklik event kalender untuk melihat rinciannya. Judul event
	 * berformat khusus: bila diawali {@code "-"} (bagian sebelum {@code "-"} kosong), sisanya
	 * adalah id pertemuan yang dinegasikan (konvensi penanda dari
	 * {@code CalendarPerkuliahanBulanIniComposer.createEvent} untuk kasus tertentu); selain itu,
	 * bagian sebelum {@code "-"} pertama adalah id pertemuan langsung. Pertemuan yang
	 * ditemukan dibuka lewat {@link #init(Pertemuan, EventListener)} (jendela modal rincian).
	 * Kegagalan parsing/pencarian diabaikan diam-diam.
	 */
	public void onEventEdit$calendars(ForwardEvent event) throws Exception {

		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();

		CalendarEvent ce = evt.getCalendarEvent();

		try {
			if (ce.getTitle().split("-")[0].trim().isEmpty()) {
				Pertemuan pertemuan = (Pertemuan) HibernateUtil.currentSession().createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.idEq(-Long.parseLong(ce.getTitle().split("-")[1]))).setMaxResults(1)
						.uniqueResult();

				CalendarPerkuliahanMingguIniComposer.init(pertemuan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				});

			} else {
				Pertemuan pertemuan = (Pertemuan) HibernateUtil.currentSession().createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.idEq(Long.parseLong(ce.getTitle().split("-")[0]))).setMaxResults(1)
						.uniqueResult();

				CalendarPerkuliahanMingguIniComposer.init(pertemuan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub

					}
				});

			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanMingguIniComposer.java:993");

		}

	}

	/** Menyinkronkan perubahan tampilan (geser/ubah durasi event lewat drag, bila diizinkan komponen kalender) ke model kalender in-memory — murni pembaruan UI, tidak menyentuh database. */
	public void onEventUpdate$calendars(ForwardEvent event) {
		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();
		org.zkoss.calendar.Calendars cal = (org.zkoss.calendar.Calendars) evt.getTarget();
		SimpleCalendarModel m = (SimpleCalendarModel) cal.getModel();
		SimpleCalendarEvent sce = (SimpleCalendarEvent) evt.getCalendarEvent();
		sce.setBeginDate(evt.getBeginDate());
		sce.setEndDate(evt.getEndDate());
		m.update(sce);
	}

}
