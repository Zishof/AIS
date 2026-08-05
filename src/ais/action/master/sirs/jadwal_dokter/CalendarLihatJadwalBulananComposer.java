package ais.action.master.sirs.jadwal_dokter;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.hibernate.Session;
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
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
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
import ais.ui.util.CustomSimpleDateFormatter;

public class CalendarLihatJadwalBulananComposer extends GenericForwardComposer {

	private static final long serialVersionUID = 201011240904L;
	private SimpleCalendarModel cm;
	private Calendars calendars;

	private Combobox poly;
	private AmbilDataDokterBanbox dokter;
	private AmbilDataLokasiBanbox lokasi;

	public void onRefresh(Event event) {
		initCalendarModel();
		calendars.invalidate();
	}

	public void onBack(Event event) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(calendars.getCurrentDate());
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1);
		calendars.setCurrentDate(calendar.getTime());
		initCalendarModel();
		calendars.invalidate();
	}

	public void onNext(Event event) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(calendars.getCurrentDate());
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
		calendars.setCurrentDate(calendar.getTime());
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
		Common.insertCombo(poly, "nama", "jenis", Poly.class);

		lokasi.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		dokter.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		calendars.setDateFormatter(new CustomSimpleDateFormatter());
		calendars.setTimeslots(6);

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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/jadwal_dokter/CalendarLihatJadwalBulananComposer.java:127");
			}
			calendars.setBeginTime(mulai);
		}
		if (penjadwalanjamSelesai.getNilai().equals(Konfigurasi.AKTIF)) {
			Integer sampai = 23;
			try {
				sampai = Integer.parseInt(penjadwalanjamSelesai.getInfo1().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/jadwal_dokter/CalendarLihatJadwalBulananComposer.java:135");
			}
			calendars.setEndTime(sampai);
		}

		calendars.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				System.out.println(
						"======================================= on Chnage ==========================================");
			}
		});

		onRefresh(null);

	}

	@SuppressWarnings("unchecked")
	private void initCalendarModel() {

		Lokasi myLokasi = (Lokasi) lokasi.getAttribute("lokasi");
		Dokter myDokter = (Dokter) dokter.getAttribute("dokter");
		Poly myPoly = (Poly) (poly.getSelectedItem() == null ? null : poly.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();

		cm = new SimpleCalendarModel();
		Calendar current = Calendar.getInstance();
		current.setTime(calendars.getBeginDate());

		while (current.getTime().before(calendars.getEndDate())) {

			String currHari = Common.haris[current.get(Calendar.DAY_OF_WEEK) - 1];

			List<JadwalDokter> jadwalDokter = session.createCriteria(JadwalDokter.class)
					.add(Restrictions.eq("hari", currHari))
					.add(Restrictions.or(Restrictions.isNull("jadwalDokterDimulai"),
							Restrictions.le("jadwalDokterDimulai", current.getTime())))
					.add(Restrictions.or(Restrictions.isNull("jadwalDokterSampai"),
							Restrictions.ge("jadwalDokterSampai", current.getTime())))
					.add(myLokasi == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("lokasi", myLokasi))
					.add(myDokter == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("dokter", myDokter))
					.add(myPoly == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("poly", myPoly))

					.list();
			for (JadwalDokter myJadwalDokter : jadwalDokter) {
				cm.add(CommonSirs.createSimpleCalendarEvent(myJadwalDokter, current));
			}

			current.set(Calendar.DATE, current.get(Calendar.DATE) + 1);
		}
		calendars.setModel(cm);
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

	public void onSearchDefault(Event event) {
		onRefresh(event);
	}

	public void onEventEdit$calendars(ForwardEvent event) throws Exception {

		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();

		CalendarEvent ce = evt.getCalendarEvent();

		JadwalDokter jadwalDokter = (JadwalDokter) HibernateUtil.currentSession().createCriteria(JadwalDokter.class)
				.add(Restrictions.idEq(Long.parseLong(ce.getTitle()))).setMaxResults(1).uniqueResult();

		final Window window = new Window("Lihat Jadwal", "none", true);
		window.setParent(page.getFirstRoot());
		window.setWidth("390px");
		window.setHeight("90%");

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(window);

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
		row.appendChild(new Label(jadwalDokter.getLokasi() == null ? "" : jadwalDokter.getLokasi().getNama()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Shift")));
		row.appendChild(new Label(jadwalDokter.getShift() == null ? "" : jadwalDokter.getShift().toString()));

		Row rowdokter = new Row();
		rowdokter.setStyle("border:0px;background: transparent;");
		rowdokter.setParent(rows);
		rowdokter.appendChild(new Label(("Tenaga Medis")));
		rowdokter.appendChild(new Label(jadwalDokter.getDokter() == null ? "" : jadwalDokter.getDokter().toString()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Poly")));
		row.appendChild(new Label(jadwalDokter.getPoly() == null ? "" : jadwalDokter.getPoly().toString()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu Mulai")));
		row.appendChild(new Label(
				jadwalDokter.getShift() == null ? "" : Common.timeFormat.get().format(jadwalDokter.getShift().getMulai())));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu Selesai")));
		row.appendChild(new Label(
				jadwalDokter.getShift() == null ? "" : Common.timeFormat.get().format(jadwalDokter.getShift().getSampai())));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Hari")));
		row.appendChild(new Label(jadwalDokter.getHari() == null ? "" : jadwalDokter.getHari()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Berlaku mulai")));
		row.appendChild(new Hbox(new Component[] {
				new Label(jadwalDokter.getJadwalDokterDimulai() == null ? ""
						: Common.dateFormat2.get().format(jadwalDokter.getJadwalDokterDimulai())),
				new Label(ais.common.Common.getBahasaConfig(" s.d ")), new Label(jadwalDokter.getJadwalDokterSampai() == null ? ""
						: Common.dateFormat2.get().format(jadwalDokter.getJadwalDokterSampai())) }));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(new Label(jadwalDokter.getKeterangan() == null ? "" : jadwalDokter.getKeterangan()));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		window.onModal();
	}
}
