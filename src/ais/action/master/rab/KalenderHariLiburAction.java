package ais.action.master.rab;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.calendar.Calendars;
import org.zkoss.calendar.event.CalendarsEvent;
import org.zkoss.calendar.impl.SimpleCalendarModel;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Textbox;

import ais.action.master.rab.helper.HariLiburKalenderEvent;
import ais.action.master.rab.helper.HariLiburKalenderModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.HariLibur;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KalenderHariLiburAction extends GenericForwardComposer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2305608876280722903L;
	private static final String CALENDAR_EVENT = "Caneldar_Event";
	private static final String CTRL_EVENT = "Ctrl_Event";
	private static final SimpleDateFormat DefaultDateFormat = new SimpleDateFormat(
			"yyyy/MM/dd");
	private static final SimpleDateFormat DefaultMonthFormat = new SimpleDateFormat(
			"MMM / yyyy");
	private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
	private MyToolbarbuttonConfig currentDate;
	private Textbox filter_text;
	private Calendars calendars;

	// Data Chooser Popup
	private Popup dateChooserPopup;
	private HariLiburKalenderModel scm;
	// Module - Event Popup
	private MyWindow EventPop;
	private MyDatebox EventPop$ppbegin;
	private Textbox EventPop$ppcnt;
	private MyCheckboxConfig EventPop$libur;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
			org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		Session session = HibernateUtil.currentSession();
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		if (calendar != null) { calendar.setTime(calendars.getBeginDate()); }
		int max = calendar.getMaximum(Calendar.DATE);
		Set<HariLiburKalenderEvent> hariLiburs = new HashSet<HariLiburKalenderEvent>();
		for (int i = 1; i <= max; i++) {
			calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(calendars.getBeginDate());
			calendar.set(Calendar.DATE, i);
			HariLibur hariLibur = (HariLibur) session
					.createCriteria(HariLibur.class)
					.add(Restrictions.eq("tanggal", calendar.getTime()))
					.setMaxResults(1).uniqueResult();
			if (hariLibur == null) {
				hariLibur = new HariLibur();
				hariLibur.setTanggal(calendar.getTime());
				session.save(hariLibur);
			}
			hariLiburs.add(new HariLiburKalenderEvent(hariLibur));
		}

		scm = new HariLiburKalenderModel(hariLiburs);
		if (calendars != null) { calendars.setModel(scm); }
		if (currentDate != null) { currentDate.setLabel(DefaultMonthFormat.format(ais.ui.util.WaktuUtil.getDate())); }
		if (timeFormat != null) { timeFormat.setTimeZone(calendars.getDefaultTimeZone()); }
	}

	public void onEventFilter(ForwardEvent event) {
		String value = filter_text.getValue();
		scm.setFilterText(value);
		calendars.setModel(scm);
	}

	// Handle Client Event "onMoveDate", triggered by mouse scroll
	public void onMoveDate$calendars(ForwardEvent event) {
		Event mevt = event.getOrigin();
		pageChange(Integer.parseInt(mevt.getData().toString()));
	}

	public void onClick$prevPage() {
		pageChange(1);
	}

	public void onClick$nextPage() {
		pageChange(-1);
	}

	/*
	 * @see <a href="http://goo.gl/X6678" target="_zkdemo">ZK Calendar
	 * Essentials - Implementing ZK Calendar Event Listeners</a>
	 */
	public void onEventUpdate$calendars(ForwardEvent event) {
		// Get Original Event
		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();
		HariLiburKalenderEvent sce = (HariLiburKalenderEvent) evt
				.getCalendarEvent();
		sce.setBeginDate(evt.getBeginDate());
		sce.setEndDate(evt.getEndDate());
		((SimpleCalendarModel) calendars.getModel()).update(sce);
	}

	// Create an popup for new event on calendar

	public void onEventCreate$calendars(ForwardEvent event) {
		updatePopup((CalendarsEvent) event.getOrigin());
	}

	// Confirm create/update event on canendar
	public void onClick$okBtn$EventPop(ForwardEvent event) {
		Date beginDate = EventPop$ppbegin.getValue();
		if (EventPop.getAttribute(CTRL_EVENT) == null) {// Create

			HariLibur hariLibur = new HariLibur();
			hariLibur.setKeterangan(EventPop$ppcnt.getValue());
			hariLibur.setNama("");
			hariLibur.setTanggal(beginDate);
			hariLibur.setLibur(EventPop$libur.isChecked());

			scm.add(new HariLiburKalenderEvent(hariLibur));
			EventPop$ppcnt.setRawValue("");
			EventPop.setVisible(false);
		} else {// Update
			HariLiburKalenderEvent ce = (HariLiburKalenderEvent) EventPop
					.getAttribute(CTRL_EVENT);
			ce.setBeginDate(beginDate);
			ce.setContent(EventPop$ppcnt.getValue());
			ce.setLocked(true);
			ce.getHariLibur().setLibur(EventPop$libur.isChecked());
			scm.update(ce);
			EventPop.setVisible(false);
			EventPop.setAttribute(CTRL_EVENT, null);
		}
	}

	// Cancel create event on canendar

	public void onClick$cancelBtn$EventPop(ForwardEvent event) {
		EventPop$ppcnt.setRawValue("");
		EventPop.setVisible(false);
		CalendarsEvent calevt = (CalendarsEvent) EventPop
				.getAttribute(CALENDAR_EVENT);
		if (calevt.getCalendarEvent() == null)
			calevt.clearGhost();
	}

	// Edit exists event
	public void onEventEdit$calendars(ForwardEvent event) {
		updatePopup((CalendarsEvent) event.getOrigin());
	}

	private void pageChange(int page) {

		if (page > 0) {

			Session session = HibernateUtil.currentSession();
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(calendars.getBeginDate());
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
			int max = calendar.getMaximum(Calendar.DATE);
			Set<HariLiburKalenderEvent> hariLiburs = new HashSet<HariLiburKalenderEvent>();
			for (int i = 1; i <= max; i++) {
				calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(calendars.getBeginDate());
				calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
				calendar.set(Calendar.DATE, i);
				HariLibur hariLibur = (HariLibur) session
						.createCriteria(HariLibur.class)
						.add(Restrictions.eq("tanggal", calendar.getTime()))
						.setMaxResults(1).uniqueResult();
				if (hariLibur == null) {
					hariLibur = new HariLibur();
					hariLibur.setTanggal(calendar.getTime());
					session.save(hariLibur);
				}
				hariLiburs.add(new HariLiburKalenderEvent(hariLibur));
			}
			((HariLiburKalenderModel) calendars.getModel()).setList(hariLiburs);
			calendars.previousPage();
		} else {

			Session session = HibernateUtil.currentSession();
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(calendars.getBeginDate());
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
			int max = calendar.getMaximum(Calendar.DATE);
			Set<HariLiburKalenderEvent> hariLiburs = new HashSet<HariLiburKalenderEvent>();
			for (int i = 1; i <= max; i++) {
				calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(calendars.getBeginDate());
				calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
				calendar.set(Calendar.DATE, i);
				HariLibur hariLibur = (HariLibur) session
						.createCriteria(HariLibur.class)
						.add(Restrictions.eq("tanggal", calendar.getTime()))
						.setMaxResults(1).uniqueResult();
				if (hariLibur == null) {
					hariLibur = new HariLibur();
					hariLibur.setTanggal(calendar.getTime());
					session.save(hariLibur);
				}
				hariLiburs.add(new HariLiburKalenderEvent(hariLibur));
			}
			((HariLiburKalenderModel) calendars.getModel()).setList(hariLiburs);

			calendars.nextPage();
		}
		currentDate.setLabel(DefaultMonthFormat.format(calendars
				.getCurrentDate()));
	}

	public void onClick$dateConfirm(ForwardEvent event) {
		dateChooserPopup.close();
	}

	public void onChange$dateChooser(ForwardEvent event) {
		InputEvent ie = (InputEvent) event.getOrigin();
		try {
			calendars.setCurrentDate(DefaultDateFormat.parse(ie.getValue()));
		} catch (ParseException e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	// Update popup data while open
	private void updatePopup(CalendarsEvent evt) {
		Integer[] location = getPopupPosition(evt);
		EventPop.setLeft(location[0] + "px");
		EventPop.setTop(location[1] + "px");
		if (evt.getCalendarEvent() == null) {
			EventPop$ppbegin.setValue(evt.getBeginDate());

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(evt.getBeginDate());
			Boolean libur = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
					|| calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
			EventPop$libur.setChecked(libur);
			EventPop.setAttribute(CTRL_EVENT, null);
			evt.stopClearGhost();
		} else {
			HariLiburKalenderEvent cevt = (HariLiburKalenderEvent) evt
					.getCalendarEvent();
			EventPop$ppbegin.setValue(cevt.getBeginDate());
			EventPop$ppcnt.setValue(cevt.getOriginContent());
			EventPop$libur.setChecked(cevt.getHariLibur().getLibur());
			// store for the edit marco component.
			EventPop.setAttribute(CTRL_EVENT, cevt);
		}
		EventPop.setAttribute(CALENDAR_EVENT, evt);
		EventPop.setVisible(true);
	}

	// Count popup position prevent out of browser view
	private Integer[] getPopupPosition(CalendarsEvent evt) {
		int left = evt.getX();
		int top = evt.getY();

		if (top + 245 > evt.getDesktopHeight())
			top = evt.getDesktopHeight() - 245;
		if (left + 410 > evt.getDesktopWidth())
			left = evt.getDesktopWidth() - 410;
		return new Integer[] { left, top };
	}
}
