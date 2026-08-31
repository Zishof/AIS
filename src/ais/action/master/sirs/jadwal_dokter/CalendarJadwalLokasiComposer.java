package ais.action.master.sirs.jadwal_dokter;

import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.calendar.Calendars;
import org.zkoss.calendar.api.CalendarEvent;
import org.zkoss.calendar.event.CalendarsEvent;
import org.zkoss.calendar.impl.SimpleCalendarEvent;
import org.zkoss.calendar.impl.SimpleCalendarModel;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.sirs.helper.AmbilDataDokterBanbox;
import ais.action.master.sirs.helper.AmbilDataLokasiBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.CommonSirs;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.JadwalDokter;
import ais.database.model.sirs.Poly;
import ais.database.model.sirs.Shift;
import ais.ui.util.CustomSimpleDateFormatter;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyTextbox;

/**
 * Composer ZK (modul SIRS/rumah sakit) untuk menampilkan dan mengelola jadwal dokter
 * ({@link JadwalDokter}) dalam bentuk kalender mingguan interaktif memakai komponen ZK
 * {@link Calendars} — mengikuti pola pengikatan event otomatis {@code GenericForwardComposer}
 * (method {@code onXxx$namaKomponen} secara otomatis terpasang sebagai listener komponen ZUL
 * bernama sama). Kalender selalu terikat pada satu {@code Lokasi} (ruangan/poli) yang dipilih;
 * memilih lokasi baru atau menekan tombol refresh ({@link #onRefresh(Event)}) memuat ulang model
 * kalender lewat {@code CommonSirs.initCalendarModel}.
 *
 * <h2>Interaksi kalender</h2>
 * <ul>
 * <li>Menggambar rentang waktu kosong pada kalender ({@link #onEventCreate$calendars(ForwardEvent)})
 * membuka jendela tambah jadwal, otomatis mengisi shift yang cocok dengan lokasi dan hari yang
 * dipilih.</li>
 * <li>Mengklik event jadwal yang sudah ada ({@link #onEventEdit$calendars(ForwardEvent)}) membuka
 * jendela ubah/hapus jadwal tersebut.</li>
 * <li>Menggeser (drag) event pada kalender ({@link #onEventUpdate$calendars(ForwardEvent)})
 * memperbarui waktu jadwal secara langsung.</li>
 * <li>Navigasi kalender: {@link #onBack(Event)}/{@link #onNext(Event)} (geser satu minggu),
 * {@link #onToday(ForwardEvent)}, {@link #onMoveDate(ForwardEvent)}, {@link #onSwitchTimeZone(ForwardEvent)},
 * {@link #onUpdateFirstDayOfWeek(ForwardEvent)}, {@link #onUpdateView(ForwardEvent)}.</li>
 * </ul>
 * <p>
 * {@link #doAfterCompose(Component)} memvalidasi sesi login (mengarahkan ke {@code /logoff} bila
 * tidak valid atau tidak punya hak baca), mengonfigurasi tampilan kalender (format tanggal, jumlah
 * slot waktu, jam mulai/selesai, dan zona waktu — semuanya dapat diatur lewat konfigurasi
 * {@code penjadwalan_jam_mulai}/{@code penjadwalan_jam_selesai}/{@code penjadwalan_timezone}), dan
 * menyiapkan combobox hari. {@link #onSave()} memvalidasi field wajib form tambah/ubah jadwal
 * (shift, dokter, waktu mulai/selesai, hari, lokasi, poli) sebelum menyimpan.
 * </p>
 */
public class CalendarJadwalLokasiComposer extends GenericForwardComposer {

	private static final long serialVersionUID = 201011240904L;
	private Calendars calendars;

	private MyDatebox ppbegin = new MyDatebox();
	private MyDatebox ppend = new MyDatebox();

	private Window addWindow;
	private Window editWindow;

	private Label waktuMulai;
	private Label waktuSelesai;

	private Combobox shift;
	private Combobox poly;
	private AmbilDataDokterBanbox dokter;
	private Combobox hari;
	private AmbilDataLokasiBanbox lokasi;
	private MyTextbox keterangan;

	private Datebox jadwalDokterDimulai;
	private Datebox jadwalDokterSampai;

	private Combobox ppcolor;

	private JadwalDokter jadwalDokter;

	/** Menggeser tampilan kalender mundur satu minggu dan memuat ulang modelnya. */
	public void onBack(Event event) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(calendars.getCurrentDate());
		calendar.set(Calendar.WEEK_OF_YEAR,
				calendar.get(Calendar.WEEK_OF_YEAR) - 1);
		calendars.setCurrentDate(calendar.getTime());
		initCalendarModel();
		calendars.invalidate();
	}

	/** Menggeser tampilan kalender maju satu minggu dan memuat ulang modelnya. */
	public void onNext(Event event) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(calendars.getCurrentDate());
		calendar.set(Calendar.WEEK_OF_YEAR,
				calendar.get(Calendar.WEEK_OF_YEAR) + 1);
		calendars.setCurrentDate(calendar.getTime());
		initCalendarModel();
		calendars.invalidate();
	}

	@SuppressWarnings({})
	private void init(final JadwalDokter jadwalDokter) throws Exception {

		this.jadwalDokter = jadwalDokter;
		Borderlayout borderlayout = new Borderlayout();

		if (jadwalDokter.getId() == null) {
			Common.clear(addWindow);
			addWindow.setTitle(jadwalDokter.getId() == null ? "Tambah Jadwal"
					: "Ubah Jadwal");
			addWindow.setWidth("590px");
			addWindow.setHeight("90%");
			borderlayout.setParent(addWindow);
		} else {
			Common.clear(editWindow);
			editWindow.setTitle(jadwalDokter.getId() == null ? "Tambah Jadwal"
					: "Ubah Jadwal");
			editWindow.setWidth("590px");
			editWindow.setHeight("90%");
			borderlayout.setParent(editWindow);
		}

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Lokasi")));
		row.appendChild(new Label(jadwalDokter.getLokasi() == null ? ""
				: jadwalDokter.getLokasi().getNama()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Shift")));
		row.appendChild(shift = new Combobox());
		Common.insertCombo(shift, "nama", "keteranganLabel", Shift.class);
		Common.selectComboItem(shift, jadwalDokter.getShift());
		shift.setWidth("90%");

		Row rowdokter = new Row();
		rowdokter.setStyle("border:0px;background: transparent;");
		rowdokter.setParent(rows);
		rowdokter.appendChild(new Label(("Tenaga Medis")));
		Hbox hbox = new Hbox();
		hbox.appendChild(dokter = new AmbilDataDokterBanbox());
		rowdokter.appendChild(hbox);
		dokter.setValue(jadwalDokter.getDokter() == null ? "" : (jadwalDokter
				.getDokter().getNama()));
		dokter.setAttribute("dokter", jadwalDokter.getDokter());
		dokter.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Poly")));
		row.appendChild(poly = new Combobox());
		Common.insertCombo(poly, "nama", "jenis", Poly.class);
		Common.selectComboItem(poly, jadwalDokter.getPoly());
		poly.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu Mulai")));
		row.appendChild(waktuMulai = new Label(
				jadwalDokter.getShift() == null ? "" : Common.timeFormat.get()
						.format(jadwalDokter.getShift().getMulai())));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu Selesai")));
		row.appendChild(waktuSelesai = new Label(
				jadwalDokter.getShift() == null ? "" : Common.timeFormat.get()
						.format(jadwalDokter.getShift().getSampai())));

		final EventListener shistEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Shift myShift = (Shift) (shift.getSelectedItem() == null ? null
						: shift.getSelectedItem().getValue());
				if (myShift != null) {
					waktuMulai.setValue(Common.timeFormat.get().format(myShift
							.getMulai()));
					waktuSelesai.setValue(Common.timeFormat.get().format(myShift
							.getSampai()));
				}
			}
		};

		EventListener lokasiEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				shift.setSelectedItem(null);
				Common.clear(shift);
				if (lokasi.getAttribute("lokasi") != null) {
					Common.insertCombo(
							shift,
							"nama",
							"keteranganLabel",
							Shift.class,
							Restrictions.eq("lokasi",
									lokasi.getAttribute("lokasi")));
					Common.selectComboItem(shift, jadwalDokter.getShift());
				}

				shistEventListener.onEvent(null);
			}
		};

		shift.addEventListener("onChange", shistEventListener);
		lokasi.setEventListener(lokasiEventListener);
		lokasiEventListener.onEvent(null);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Hari")));
		Common.selectComboItem(hari, (jadwalDokter.getHari() == null ? ""
				: jadwalDokter.getHari()));
		row.appendChild(hari);
		hari.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Berlaku mulai")));
		row.appendChild(new Hbox(new Component[] {
				jadwalDokterDimulai = new Datebox(jadwalDokter
						.getJadwalDokterDimulai()),
				new Label(ais.common.Common.getBahasaConfig(" s.d ")),
				jadwalDokterSampai = new Datebox(jadwalDokter
						.getJadwalDokterSampai()) }));

		jadwalDokterDimulai.setFormat(Common.dateFormat2.get().toPattern());
		jadwalDokterSampai.setFormat(Common.dateFormat2.get().toPattern());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Warna")));
		row.appendChild(ppcolor = new Combobox());
		ppcolor.setStyle("color:#D96666;font-weight: bold;");
		ppcolor.addEventListener(Events.ON_SELECT, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				switch (ppcolor.getSelectedIndex()) {
				case 0:
					ppcolor.setStyle("color:#D96666;font-weight: bold;");
					break;
				case 1:
					ppcolor.setStyle("color:#668CD9;font-weight: bold;");
					break;
				case 2:
					ppcolor.setStyle("color:#4CB052;font-weight: bold;");
					break;
				case 3:
					ppcolor.setStyle("color:#BFBF4D;font-weight: bold;");
					break;
				case 4:
					ppcolor.setStyle("color:#B373B3;font-weight: bold;");
					break;
				}

			}
		});
		Comboitem comboitem = new Comboitem("Merah");
		comboitem.setSclass("red");
		comboitem.setValue("#A32929,#D96666");
		ppcolor.appendChild(comboitem);
		comboitem = new Comboitem("Biru");
		comboitem.setSclass("blue");
		comboitem.setValue("#3467CE,#668CD9");
		ppcolor.appendChild(comboitem);
		comboitem = new Comboitem("Hijau");
		comboitem.setSclass("green");
		comboitem.setValue("#0D7813,#4CB052");
		ppcolor.appendChild(comboitem);
		comboitem = new Comboitem("Khaki");
		comboitem.setSclass("khaki");
		comboitem.setValue("#88880E,#BFBF4D");
		ppcolor.appendChild(comboitem);
		comboitem = new Comboitem("Ungu");
		comboitem.setSclass("purple");
		comboitem.setValue("#7A367A,#B373B3");
		ppcolor.appendChild(comboitem);

		Common.selectComboItem(ppcolor,
				jadwalDokter.getWarna() == null ? "#A32929,#D96666"
						: jadwalDokter.getWarna());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(
				jadwalDokter.getKeterangan() == null ? "" : jadwalDokter
						.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
				editWindow.setVisible(false);
				onClick$cancelBtn$addWindow(null);
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (jadwalDokter.getId() == null) {
					onClick$okBtn$addWindow(null);
				} else {
					onClick$okBtn$editWindow(null);
				}

			}
		});
		save.setParent(toolbar);
		cancel = new ais.ui.util.MyToolbarbuttonConfig("Hapus", "/img/delete.gif");
		cancel.setVisible(jadwalDokter.getId() != null);
		cancel.setTooltiptext("Hapus");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onClick$deleteBtn$editWindow(null);
			}
		});
		cancel.setParent(toolbar);
	}

	/** Memuat ulang model kalender untuk lokasi yang sedang dipilih. */
	public void onRefresh(Event event) {
		initCalendarModel();
		calendars.invalidate();
	}

	/**
	 * Inisialisasi composer setelah komponen ZK ter-wiring: memvalidasi sesi login (redirect ke
	 * {@code /logoff} bila tidak valid/tidak berhak baca), memasang listener refresh pada pemilihan
	 * lokasi, dan mengonfigurasi tampilan kalender (format tanggal, slot waktu, jam mulai/selesai,
	 * zona waktu sesuai konfigurasi aplikasi) serta combobox hari.
	 */
	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}

		lokasi.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		calendars.setDateFormatter(new CustomSimpleDateFormatter());
		calendars.setTimeslots(4);
		Konfigurasi penjadwalanjamMulai = Common.getKonfigurasi(
				"penjadwalan_jam_mulai", Konfigurasi.AKTIF, "0", "", "");
		Konfigurasi penjadwalanjamSelesai = Common.getKonfigurasi(
				"penjadwalan_jam_selesai", Konfigurasi.AKTIF, "24", "", "");

		Konfigurasi penjadwalanTimezone = Common.getKonfigurasi(
				"penjadwalan_timezone", Konfigurasi.AKTIF, "Jakarta=GMT+7", "",
				"");

		if (penjadwalanTimezone.getNilai().equals(Konfigurasi.AKTIF)) {
			calendars.setTimeZone(penjadwalanTimezone.getInfo1());
		}

		if (penjadwalanjamMulai.getNilai().equals(Konfigurasi.AKTIF)) {
			Integer mulai = 7;
			try {
				mulai = Integer.parseInt(penjadwalanjamMulai.getInfo1().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/jadwal_dokter/CalendarJadwalLokasiComposer.java:405");
			}
			calendars.setBeginTime(mulai);
		}
		if (penjadwalanjamSelesai.getNilai().equals(Konfigurasi.AKTIF)) {
			Integer sampai = 23;
			try {
				sampai = Integer.parseInt(penjadwalanjamSelesai.getInfo1()
						.trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/jadwal_dokter/CalendarJadwalLokasiComposer.java:414");
			}
			calendars.setEndTime(sampai);
		}

		hari = new Combobox();
		for (String h : Common.haris) {
			Comboitem comboitem = new Comboitem();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari.appendChild(comboitem);

		}

		calendars.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				System.out
						.println("======================================= on Chnage ==========================================");
			}
		});

	}

	private void initCalendarModel() {

		Lokasi myLokasi = (Lokasi) lokasi.getAttribute("lokasi");

		if (myLokasi == null)
			return;
		CommonSirs.initCalendarModel(myLokasi, null, null, calendars);
	}

	/**
	 * Handler saat pengguna menggambar rentang waktu kosong baru pada kalender (buat event): mensyaratkan
	 * lokasi sudah dipilih, mencari {@link Shift} yang cocok dengan lokasi dan hari terpilih, lalu
	 * membuka jendela tambah jadwal dengan field terisi awal sesuai rentang yang digambar.
	 */
	public void onEventCreate$calendars(ForwardEvent event) throws Exception {

		Lokasi myLokasi = (Lokasi) lokasi.getAttribute("lokasi");
		if (myLokasi == null) {
			MyMessageboxConfig.show("Mohon maaf, Lokasi wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) gunakan kolom pencarian untuk memilih Lokasi; (2) pastikan pilihan tidak dikosongkan; (3) ulangi kembali setelah Lokasi ditentukan.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();

		Shift shift = (Shift) HibernateUtil.currentSession()
				.createCriteria(Shift.class)
				.add(Restrictions.eq("lokasi", myLokasi))
				.add(Restrictions.le("mulai", evt.getBeginDate()))
				.addOrder(Order.desc("mulai")).setMaxResults(1).uniqueResult();

		if (shift == null) {
			MyMessageboxConfig
					.showFormat("Mohon maaf, Shift untuk lokasi \"{V1}\" belum ditemukan. Langkah yang dapat dilakukan: (1) buka menu pengaturan Shift terlebih dahulu; (2) tambahkan Shift untuk lokasi tersebut; (3) ulangi kembali pembuatan jadwal setelah Shift tersedia.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, myLokasi.getNama());
			return;
		}

		JadwalDokter jadwalDokter = new JadwalDokter();
		jadwalDokter.setLokasi(myLokasi);
		jadwalDokter.setShift(shift);
		jadwalDokter.setJadwalDokterDimulai(evt.getBeginDate());

		init(jadwalDokter);
		addWindow.setPosition("center");

		ppbegin.setTimeZone(TimeZone.getDefault());
		ppbegin.setValue(evt.getBeginDate());
		ppend.setTimeZone(TimeZone.getDefault());
		ppend.setValue(evt.getEndDate());

		Calendar begin = Calendar.getInstance(TimeZone.getDefault());
		begin.setTime(ppbegin.getValue());
		hari.setSelectedIndex(begin.get(Calendar.DAY_OF_WEEK) - 1);

		addWindow.setVisible(true);
		addWindow.doModal();
		addWindow.setAttribute("calevent", evt);
		evt.stopClearGhost();
	}

	/** Menutup jendela tambah jadwal dan membersihkan "ghost" event sementara pada kalender. */
	public void onClose$addWindow(ForwardEvent event) {
		event.getOrigin().stopPropagation();
		((CalendarsEvent) addWindow.getAttribute("calevent")).clearGhost();
		addWindow.setVisible(false);
	}

	/** Tombol OK jendela tambah jadwal: memanggil {@link #onSave()}; bila berhasil, menutup jendela dan menyegarkan kalender setelah jeda singkat lewat {@link Timer}. */
	public void onClick$okBtn$addWindow(ForwardEvent event) throws Exception {

		if (onSave()) {
			addWindow.setVisible(false);

			Timer timer = new Timer(500);
			timer.setParent(page.getFirstRoot());
			timer.addEventListener("onTimer", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onRefresh(arg0);
				}
			});
			timer.start();
		}

	}

	/** Tombol Batal (dipakai bersama oleh jendela tambah dan ubah): menutup kedua jendela dan membersihkan "ghost" event kalender terkait. */
	public void onClick$cancelBtn$addWindow(ForwardEvent event) {
		addWindow.setVisible(false);
		try {
			((CalendarsEvent) addWindow.getAttribute("calevent")).clearGhost();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/jadwal_dokter/CalendarJadwalLokasiComposer.java:524");
		}
		editWindow.setVisible(false);
		try {
			((CalendarsEvent) editWindow.getAttribute("calevent")).clearGhost();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/jadwal_dokter/CalendarJadwalLokasiComposer.java:529");
		}
	}

	/**
	 * Handler saat pengguna mengklik event jadwal yang sudah ada pada kalender: memuat
	 * {@link JadwalDokter} berdasarkan id yang tersimpan di judul event kalender, mengisi jendela
	 * ubah dengan data tersebut (waktu, warna indikator sesuai kombinasi header/content color
	 * bawaan kalender), dan menampilkannya.
	 */
	public void onEventEdit$calendars(ForwardEvent event) throws Exception {

		Lokasi myLokasi = (Lokasi) lokasi.getAttribute("lokasi");
		if (myLokasi == null) {
			MyMessageboxConfig.show("Mohon maaf, Lokasi wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) gunakan kolom pencarian untuk memilih Lokasi; (2) pastikan pilihan tidak dikosongkan; (3) ulangi kembali setelah Lokasi ditentukan.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();

		TimeZone tz = TimeZone.getDefault();

		editWindow.setPosition("center");
		CalendarEvent ce = evt.getCalendarEvent();

		JadwalDokter jadwalDokter = (JadwalDokter) HibernateUtil
				.currentSession().createCriteria(JadwalDokter.class)
				.add(Restrictions.idEq(Long.parseLong(ce.getTitle())))
				.setMaxResults(1).uniqueResult();

		init(jadwalDokter);

		addWindow.setPosition("center");
		boolean isAllday = false;

		ppbegin.setTimeZone(tz);
		ppbegin.setValue(ce.getBeginDate());
		ppend.setTimeZone(tz);
		ppend.setValue(ce.getEndDate());
		waktuMulai.setVisible(!isAllday);
		waktuSelesai.setVisible(!isAllday);

		String colors = ce.getHeaderColor() + "," + ce.getContentColor();
		int index = 0;
		if ("#3467CE,#668CD9".equals(colors))
			index = 1;
		else if ("#0D7813,#4CB052".equals(colors))
			index = 2;
		else if ("#88880E,#BFBF4D".equals(colors))
			index = 3;
		else if ("#7A367A,#B373B3".equals(colors))
			index = 4;

		switch (index) {
		case 0:
			ppcolor.setStyle("color:#D96666;font-weight: bold;");
			break;
		case 1:
			ppcolor.setStyle("color:#668CD9;font-weight: bold;");
			break;
		case 2:
			ppcolor.setStyle("color:#4CB052;font-weight: bold;");
			break;
		case 3:
			ppcolor.setStyle("color:#BFBF4D;font-weight: bold;");
			break;
		case 4:
			ppcolor.setStyle("color:#B373B3;font-weight: bold;");
			break;
		}
		ppcolor.setSelectedIndex(index);
		editWindow.setVisible(true);
		editWindow.onModal();

		Calendar begin = Calendar.getInstance(TimeZone.getDefault());
		begin.setTime(ppbegin.getValue());
		hari.setSelectedIndex(begin.get(Calendar.DAY_OF_WEEK) - 1);

		// store for the edit marco component.
		editWindow.setAttribute("ce", ce);
	}

	/** Menutup jendela ubah jadwal. */
	public void onClose$editWindow(ForwardEvent event) {
		event.getOrigin().stopPropagation();
		editWindow.setVisible(false);
	}

	/** Tombol OK jendela ubah jadwal: memanggil {@link #onSave()}; bila berhasil, menutup jendela dan menyegarkan kalender setelah jeda singkat. */
	public void onClick$okBtn$editWindow(ForwardEvent event) throws Exception {
		if (onSave()) {

			editWindow.setVisible(false);

			Timer timer = new Timer(500);
			timer.setParent(page.getFirstRoot());
			timer.addEventListener("onTimer", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onRefresh(arg0);
				}
			});
			timer.start();
		}

	}

	/** Tombol Hapus jendela ubah jadwal: meminta konfirmasi, lalu menghapus {@link JadwalDokter} terkait, menutup jendela, dan menyegarkan kalender setelah jeda singkat. */
	public void onClick$deleteBtn$editWindow(ForwardEvent event) {
		try {
			MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus jadwal ini? Data jadwal yang telah dihapus tidak dapat dikembalikan.",
					"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
					MyMessageboxConfig.QUESTION, new EventListener() {
						public void onEvent(Event evt) throws Exception {
							if (((Integer) evt.getData()).intValue() != MyMessageboxConfig.OK)
								return;
							SimpleCalendarEvent calendarEvent = (SimpleCalendarEvent) editWindow
									.getAttribute("ce");
							Long id = new Long(calendarEvent.getTitle());
							Session session = HibernateUtil.currentSession();
							JadwalDokter jadwalDokter = (JadwalDokter) session
									.createCriteria(JadwalDokter.class)
									.add(Restrictions.idEq(id))
									.setMaxResults(1).uniqueResult();
							session.delete(jadwalDokter);
							editWindow.setVisible(false);

							Timer timer = new Timer(500);
							timer.setParent(page.getFirstRoot());
							timer.addEventListener("onTimer",
									new EventListener() {

										@Override
										public void onEvent(Event arg0)
												throws Exception {
											onRefresh(arg0);
										}
									});
							timer.start();

						}
					});
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/jadwal_dokter/CalendarJadwalLokasiComposer.java:665");
		}
	}

	/** Handler saat pengguna menggeser (drag) event jadwal pada kalender: memperbarui waktu event pada model kalender agar tampilan tetap konsisten sebelum perubahan dipersistenkan. */
	public void onEventUpdate$calendars(ForwardEvent event) {
		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();
		org.zkoss.calendar.Calendars cal = (org.zkoss.calendar.Calendars) evt
				.getTarget();
		SimpleCalendarModel m = (SimpleCalendarModel) cal.getModel();
		SimpleCalendarEvent sce = (SimpleCalendarEvent) evt.getCalendarEvent();
		sce.setBeginDate(evt.getBeginDate());
		sce.setEndDate(evt.getEndDate());
		m.update(sce);
	}

	/** Navigasi halaman kalender maju/mundur sesuai tombol panah yang diklik pada toolbar bawaan komponen kalender. */
	public void onMoveDate(ForwardEvent event) {
		if ("arrow-left".equals(event.getData()))
			calendars.previousPage();
		else
			calendars.nextPage();

	}

	/** Mengatur tanggal aktif kalender ke hari ini. */
	public void onToday(ForwardEvent event) {
		calendars.setCurrentDate(Calendar.getInstance(TimeZone.getDefault())
				.getTime());

	}

	/** Menggeser zona waktu aktif kalender ke zona waktu berikutnya pada daftar zona waktu terdaftar. */
	@SuppressWarnings("rawtypes")
	public void onSwitchTimeZone(ForwardEvent event) {
		Map<?, ?> zone = calendars.getTimeZones();
		if (!zone.isEmpty()) {
			Map.Entry me = (Map.Entry) zone.entrySet().iterator().next();
			calendars.removeTimeZone((TimeZone) me.getKey());
			calendars.addTimeZone((String) me.getValue(),
					(TimeZone) me.getKey());
		}

	}

	/** Mengatur hari pertama minggu kalender sesuai pilihan pada listbox pengaturan tampilan. */
	public void onUpdateFirstDayOfWeek(ForwardEvent event) {
		Listbox listbox = (Listbox) event.getOrigin().getTarget();
		calendars.setFirstDayOfWeek(listbox.getSelectedItem().getLabel());

	}

	/** Mengubah mode tampilan kalender (Day/5 Days/Week menjadi mold "default" dengan jumlah hari sesuai, atau mold "month" untuk tampilan bulanan). */
	public void onUpdateView(ForwardEvent event) {
		String text = String.valueOf(event.getData());
		int days = "Day".equals(text) ? 1 : "5 Days".equals(text) ? 5 : "Week"
				.equals(text) ? 7 : 0;

		if (days > 0) {
			calendars.setMold("default");
			calendars.setDays(days);
		} else
			calendars.setMold("month");
	}

	/**
	 * Memvalidasi dan menyimpan form tambah/ubah jadwal dokter (dipakai bersama oleh jendela tambah
	 * dan ubah). Field wajib: shift, dokter (tenaga medis), waktu mulai, waktu selesai, hari,
	 * lokasi, dan poli. Setiap pelanggaran validasi menampilkan pesan peringatan dan mengembalikan
	 * {@code false} tanpa menyimpan.
	 *
	 * @return {@code true} bila jadwal berhasil disimpan
	 */
	public boolean onSave() throws Exception {

		if (shift.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Shift wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Shift pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Shift ditentukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (dokter.getAttribute("dokter") == null) {
			MyMessageboxConfig.show("Mohon maaf, Tenaga Medis wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) gunakan kolom pencarian untuk memilih Tenaga Medis; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Tenaga Medis ditentukan.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (waktuMulai.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Waktu Mulai wajib terisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Shift yang memiliki waktu mulai; (2) pastikan waktu mulai tidak kosong; (3) simpan kembali data setelah waktu mulai terisi.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (waktuSelesai.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Waktu Selesai wajib terisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Shift yang memiliki waktu selesai; (2) pastikan waktu selesai tidak kosong; (3) simpan kembali data setelah waktu selesai terisi.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (hari.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Hari wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Hari pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Hari ditentukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (lokasi.getAttribute("lokasi") == null) {
			MyMessageboxConfig.show("Mohon maaf, Lokasi wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) gunakan kolom pencarian untuk memilih Lokasi; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Lokasi ditentukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (poly.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Poli wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Poli pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Poli ditentukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jadwalDokter.getId() != null) {
			jadwalDokter = (JadwalDokter) session.load(JadwalDokter.class,  jadwalDokter.getId());
		}

		jadwalDokter.setPoly((Poly) (poly.getSelectedItem() == null ? null
				: poly.getSelectedItem().getValue()));
		jadwalDokter.setKeterangan(keterangan.getValue());
		jadwalDokter.setJadwalDokterDimulai(jadwalDokterDimulai.getValue());
		jadwalDokter.setJadwalDokterSampai(jadwalDokterSampai.getValue());

		jadwalDokter.setHari(!hari.isVisible()
				|| hari.getSelectedItem() == null ? null : hari
				.getSelectedItem().getValue().toString());

		jadwalDokter.setDokter((Dokter) dokter.getAttribute("dokter"));

		jadwalDokter.setShift((Shift) (shift.getSelectedItem() == null ? null
				: shift.getSelectedItem().getValue()));

		jadwalDokter.setLokasi((Lokasi) (lokasi.isVisible() ? lokasi
				.getAttribute("lokasi") : null));

		jadwalDokter.setWarna(ppcolor.getSelectedItem() == null ? null
				: ppcolor.getSelectedItem().getValue().toString());

		Common.refreshSaveOrUpdate(session, jadwalDokter);

		return true;
	}

	/** Alias untuk {@link #onRefresh(Event)} — memuat ulang model kalender. */
	public void onSearchDefault(Event event) {
		onRefresh(event);
	}

}
