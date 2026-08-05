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

public class CalendarJadwalDokterComposer extends GenericForwardComposer {

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

	public void onBack(Event event) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(calendars.getCurrentDate());
		calendar.set(Calendar.WEEK_OF_YEAR, calendar.get(Calendar.WEEK_OF_YEAR) - 1);
		calendars.setCurrentDate(calendar.getTime());
		initCalendarModel();
		calendars.invalidate();
	}

	public void onNext(Event event) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(calendars.getCurrentDate());
		calendar.set(Calendar.WEEK_OF_YEAR, calendar.get(Calendar.WEEK_OF_YEAR) + 1);
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
			addWindow.setTitle(jadwalDokter.getId() == null ? "Tambah Jadwal Dokter" : "Ubah Jadwal Dokter");
			addWindow.setWidth("590px");
			addWindow.setHeight("90%");
			borderlayout.setParent(addWindow);
		} else {
			Common.clear(editWindow);
			editWindow.setTitle(jadwalDokter.getId() == null ? "Tambah Jadwal Dokter" : "Ubah Jadwal Dokter");
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
		row.appendChild(lokasi = new AmbilDataLokasiBanbox());
		lokasi.setValue(jadwalDokter.getLokasi() == null ? "" : (jadwalDokter.getLokasi().toString()));
		lokasi.setAttribute("lokasi", jadwalDokter.getLokasi());
		lokasi.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Shift")));
		row.appendChild(shift = new Combobox());
		Common.insertCombo(shift, "nama", "keteranganLabel", Shift.class);
		Common.selectComboItem(shift, jadwalDokter.getShift());
		shift.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(("Tenaga Medis")));
		row.appendChild(new Label(((Dokter) dokter.getAttribute("dokter")).getNama()));

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
				jadwalDokter.getShift() == null ? "" : Common.timeFormat.get().format(jadwalDokter.getShift().getMulai())));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu Selesai")));
		row.appendChild(waktuSelesai = new Label(
				jadwalDokter.getShift() == null ? "" : Common.timeFormat.get().format(jadwalDokter.getShift().getSampai())));

		final EventListener shistEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Shift myShift = (Shift) (shift.getSelectedItem() == null ? null : shift.getSelectedItem().getValue());
				if (myShift != null) {
					waktuMulai.setValue(Common.timeFormat.get().format(myShift.getMulai()));
					waktuSelesai.setValue(Common.timeFormat.get().format(myShift.getSampai()));
				}
			}
		};

		EventListener lokasiEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				shift.setSelectedItem(null);
				Common.clear(shift);
				if (lokasi.getAttribute("lokasi") != null) {
					Common.insertCombo(shift, "nama", "keteranganLabel", Shift.class,
							Restrictions.eq("lokasi", lokasi.getAttribute("lokasi")));
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
		Common.selectComboItem(hari, (jadwalDokter.getHari() == null ? "" : jadwalDokter.getHari()));
		row.appendChild(hari);
		hari.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Berlaku mulai")));
		row.appendChild(
				new Hbox(new Component[] { jadwalDokterDimulai = new Datebox(jadwalDokter.getJadwalDokterDimulai()),
						new Label(ais.common.Common.getBahasaConfig(" s.d ")), jadwalDokterSampai = new Datebox(jadwalDokter.getJadwalDokterSampai()) }));

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

		Common.selectComboItem(ppcolor, jadwalDokter.getWarna() == null ? "#A32929,#D96666" : jadwalDokter.getWarna());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(
				keterangan = new MyTextbox(jadwalDokter.getKeterangan() == null ? "" : jadwalDokter.getKeterangan()));
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

	public void onRefresh(Event event) {
		initCalendarModel();
		calendars.invalidate();
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}

		dokter.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		calendars.setDateFormatter(new CustomSimpleDateFormatter());
		calendars.setTimeslots(4);
		Konfigurasi penjadwalanjamMulai = Common.getKonfigurasi("penjadwalan_jam_mulai", Konfigurasi.AKTIF, "0", "",
				"");
		Konfigurasi penjadwalanjamSelesai = Common.getKonfigurasi("penjadwalan_jam_selesai", Konfigurasi.AKTIF, "24",
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/jadwal_dokter/CalendarJadwalDokterComposer.java:378");
			}
			calendars.setBeginTime(mulai);
		}
		if (penjadwalanjamSelesai.getNilai().equals(Konfigurasi.AKTIF)) {
			Integer sampai = 23;
			try {
				sampai = Integer.parseInt(penjadwalanjamSelesai.getInfo1().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/jadwal_dokter/CalendarJadwalDokterComposer.java:386");
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
				System.out.println(
						"======================================= on Chnage ==========================================");
			}
		});

	}

	private void initCalendarModel() {

		Dokter myDokter = (Dokter) dokter.getAttribute("dokter");

		if (myDokter == null)
			return;
		CommonSirs.initCalendarModel(null, myDokter, null, calendars);
	}

	public void onEventCreate$calendars(ForwardEvent event) throws Exception {

		Dokter myDokter = (Dokter) dokter.getAttribute("dokter");
		if (myDokter == null) {
			MyMessageboxConfig.show("Mohon maaf, Tenaga Medis wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) gunakan kolom pencarian untuk memilih Tenaga Medis; (2) pastikan pilihan tidak dikosongkan; (3) ulangi kembali setelah Tenaga Medis ditentukan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();

		Shift shift = (Shift) HibernateUtil.currentSession().createCriteria(Shift.class)
				.add(Restrictions.le("mulai", evt.getBeginDate())).addOrder(Order.desc("mulai")).setMaxResults(1)
				.uniqueResult();

		if (shift == null) {
			MyMessageboxConfig.show("Mohon maaf, Shift untuk jadwal ini belum ditemukan. Langkah yang dapat dilakukan: (1) buka menu pengaturan Shift terlebih dahulu; (2) tambahkan Shift yang sesuai dengan waktu jadwal; (3) ulangi kembali pembuatan jadwal setelah Shift tersedia.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		JadwalDokter jadwalDokter = new JadwalDokter();
		jadwalDokter.setDokter(myDokter);
		jadwalDokter.setShift(shift);
		jadwalDokter.setLokasi(shift.getLokasi());
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

	public void onClose$addWindow(ForwardEvent event) {
		event.getOrigin().stopPropagation();
		((CalendarsEvent) addWindow.getAttribute("calevent")).clearGhost();
		addWindow.setVisible(false);
	}

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

	public void onClick$cancelBtn$addWindow(ForwardEvent event) {
		addWindow.setVisible(false);
		try {
			((CalendarsEvent) addWindow.getAttribute("calevent")).clearGhost();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/jadwal_dokter/CalendarJadwalDokterComposer.java:493");
		}
		editWindow.setVisible(false);
		try {
			((CalendarsEvent) editWindow.getAttribute("calevent")).clearGhost();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/jadwal_dokter/CalendarJadwalDokterComposer.java:498");
		}
	}

	public void onEventEdit$calendars(ForwardEvent event) throws Exception {

		Dokter myDokter = (Dokter) dokter.getAttribute("dokter");
		if (myDokter == null) {
			MyMessageboxConfig.show("Mohon maaf, Tenaga Medis wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) gunakan kolom pencarian untuk memilih Tenaga Medis; (2) pastikan pilihan tidak dikosongkan; (3) ulangi kembali setelah Tenaga Medis ditentukan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();

		TimeZone tz = TimeZone.getDefault();

		editWindow.setPosition("center");
		CalendarEvent ce = evt.getCalendarEvent();

		JadwalDokter jadwalDokter = (JadwalDokter) HibernateUtil.currentSession().createCriteria(JadwalDokter.class)
				.add(Restrictions.idEq(Long.parseLong(ce.getTitle()))).setMaxResults(1).uniqueResult();

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

	public void onClose$editWindow(ForwardEvent event) {
		event.getOrigin().stopPropagation();
		editWindow.setVisible(false);
	}

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

	public void onClick$deleteBtn$editWindow(ForwardEvent event) {
		try {
			MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus jadwal ini? Data jadwal yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
					MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
						public void onEvent(Event evt) throws Exception {
							if (((Integer) evt.getData()).intValue() != MyMessageboxConfig.OK)
								return;
							SimpleCalendarEvent calendarEvent = (SimpleCalendarEvent) editWindow.getAttribute("ce");
							Long id = new Long(calendarEvent.getTitle());
							Session session = HibernateUtil.currentSession();
							JadwalDokter jadwalDokter = (JadwalDokter) session.createCriteria(JadwalDokter.class)
									.add(Restrictions.idEq(id)).setMaxResults(1).uniqueResult();
							session.delete(jadwalDokter);
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
					});
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/jadwal_dokter/CalendarJadwalDokterComposer.java:626");
		}
	}

	public void onEventUpdate$calendars(ForwardEvent event) {
		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();
		org.zkoss.calendar.Calendars cal = (org.zkoss.calendar.Calendars) evt.getTarget();
		SimpleCalendarModel m = (SimpleCalendarModel) cal.getModel();
		SimpleCalendarEvent sce = (SimpleCalendarEvent) evt.getCalendarEvent();
		sce.setBeginDate(evt.getBeginDate());
		sce.setEndDate(evt.getEndDate());
		m.update(sce);
	}

	public void onMoveDate(ForwardEvent event) {
		if ("arrow-left".equals(event.getData()))
			calendars.previousPage();
		else
			calendars.nextPage();

	}

	public void onToday(ForwardEvent event) {
		calendars.setCurrentDate(Calendar.getInstance(TimeZone.getDefault()).getTime());

	}

	@SuppressWarnings("rawtypes")
	public void onSwitchTimeZone(ForwardEvent event) {
		Map<?, ?> zone = calendars.getTimeZones();
		if (!zone.isEmpty()) {
			Map.Entry me = (Map.Entry) zone.entrySet().iterator().next();
			calendars.removeTimeZone((TimeZone) me.getKey());
			calendars.addTimeZone((String) me.getValue(), (TimeZone) me.getKey());
		}

	}

	public void onUpdateFirstDayOfWeek(ForwardEvent event) {
		Listbox listbox = (Listbox) event.getOrigin().getTarget();
		calendars.setFirstDayOfWeek(listbox.getSelectedItem().getLabel());

	}

	public void onUpdateView(ForwardEvent event) {
		String text = String.valueOf(event.getData());
		int days = "Day".equals(text) ? 1 : "5 Days".equals(text) ? 5 : "Week".equals(text) ? 7 : 0;

		if (days > 0) {
			calendars.setMold("default");
			calendars.setDays(days);
		} else
			calendars.setMold("month");
	}

	public boolean onSave() throws Exception {

		if (shift.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Shift wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Shift pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Shift ditentukan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (dokter.getAttribute("dokter") == null) {
			MyMessageboxConfig.show("Mohon maaf, Tenaga Medis wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) gunakan kolom pencarian untuk memilih Tenaga Medis; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Tenaga Medis ditentukan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (waktuMulai.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Waktu Mulai wajib terisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Shift yang memiliki waktu mulai; (2) pastikan waktu mulai tidak kosong; (3) simpan kembali data setelah waktu mulai terisi.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (waktuSelesai.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Waktu Selesai wajib terisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Shift yang memiliki waktu selesai; (2) pastikan waktu selesai tidak kosong; (3) simpan kembali data setelah waktu selesai terisi.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (hari.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Hari wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Hari pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Hari ditentukan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (lokasi.getAttribute("lokasi") == null) {
			MyMessageboxConfig.show("Mohon maaf, Lokasi wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) gunakan kolom pencarian untuk memilih Lokasi; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Lokasi ditentukan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (poly.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Poli wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Poli pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Poli ditentukan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jadwalDokter.getId() != null) {
			jadwalDokter = (JadwalDokter) session.load(JadwalDokter.class, jadwalDokter.getId());
		}

		jadwalDokter.setPoly((Poly) (poly.getSelectedItem() == null ? null : poly.getSelectedItem().getValue()));
		jadwalDokter.setKeterangan(keterangan.getValue());
		jadwalDokter.setJadwalDokterDimulai(jadwalDokterDimulai.getValue());
		jadwalDokter.setJadwalDokterSampai(jadwalDokterSampai.getValue());

		jadwalDokter.setHari(!hari.isVisible() || hari.getSelectedItem() == null ? null
				: hari.getSelectedItem().getValue().toString());

		jadwalDokter.setDokter((Dokter) dokter.getAttribute("dokter"));

		jadwalDokter.setShift((Shift) (shift.getSelectedItem() == null ? null : shift.getSelectedItem().getValue()));

		jadwalDokter.setLokasi((Lokasi) (lokasi.isVisible() ? lokasi.getAttribute("lokasi") : null));

		jadwalDokter
				.setWarna(ppcolor.getSelectedItem() == null ? null : ppcolor.getSelectedItem().getValue().toString());

		Common.refreshSaveOrUpdate(session, jadwalDokter);

		return true;
	}

	public void onSearchDefault(Event event) {
		onRefresh(event);
	}

}
