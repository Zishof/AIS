package ais.action.master.rab;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.calendar.Calendars;
import org.zkoss.calendar.event.CalendarsEvent;
import org.zkoss.calendar.impl.SimpleCalendarModel;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;

import ais.action.master.rab.helper.AcaraPunyaJenisParameterHelper;
import ais.action.master.rab.helper.AmbilDataWorkspaceBanbox;
import ais.action.master.rab.helper.KalenderEvent;
import ais.action.master.rab.helper.KalenderModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.Acara;
import ais.database.model.rab.AcaraPunyaJenisParameter;
import ais.database.model.rab.Workspace;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KalenderAction extends GenericForwardComposer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2305608876280722903L;
	private static final String CALENDAR_EVENT = "Caneldar_Event";
	private static final String CTRL_EVENT = "Ctrl_Event";
	private static final SimpleDateFormat DefaultDateFormat = new SimpleDateFormat("yyyy/MM/dd");
	private static final SimpleDateFormat DefaultMonthFormat = new SimpleDateFormat("MMM / yyyy");
	private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
	private MyToolbarbuttonConfig currentDate;
	private Textbox filter_text;
	private Menuitem prevPage, nextPage;
	private Calendars calendars;

	// Data Chooser Popup
	private Popup dateChooserPopup;
	private KalenderModel scm;
	private Calendar dateChooser;
	private MyButtonConfig dateConfirm;

	// Module - Event Popup
	private MyWindow EventPop;
	private MyDatebox EventPop$ppbegin, EventPop$ppend;
	private Listbox EventPop$ppbt, EventPop$ppet;
	private MyCheckboxConfig EventPop$ppallDay, EventPop$pplocked;
	private Textbox EventPop$headColor, EventPop$cntColor;
	private AmbilDataWorkspaceBanbox EventPop$anggaran;
	private MyGrid EventPop$gridParameter;
	private Textbox EventPop$ppcnt;
	private Tabpanel EventPop$parameter;
	private Tabpanel EventPop$jurnalPengeluaran;
	private Tabpanel EventPop$jurnalUmum;
	private Tabpanel EventPop$indikator;
	private MyButtonConfig EventPop$deleteBtn;

	private Acara acara;

	@SuppressWarnings({ "unchecked" })
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		initTimeDropdown();

		Session session = HibernateUtil.currentSession();
		List<Acara> acaras = session.createCriteria(Acara.class).list();
		List<KalenderEvent> kalenderEvents = new ArrayList<KalenderEvent>();
		for (Acara acara : acaras) {
			kalenderEvents.add(new KalenderEvent(acara));
		}
		acaras = null;
		scm = new KalenderModel(kalenderEvents);
		if (calendars != null) { calendars.setModel(scm); }
		if (currentDate != null) { currentDate.setLabel(DefaultMonthFormat.format(ais.ui.util.WaktuUtil.getDate())); }
		if (timeFormat != null) { timeFormat.setTimeZone(calendars.getDefaultTimeZone()); }

		EventPop$anggaran.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// System.out.println("acara = " + acara);
				if (acara != null) {
					Common.clear(EventPop$jurnalPengeluaran);
					EventPop$parameter.getLinkedTab().setSelected(true);
					acara.setWorkspace((Workspace) EventPop$anggaran.getAttribute("workspace"));
					AcaraPunyaJenisParameterHelper workspacePunyaJenisParameterHelper = new AcaraPunyaJenisParameterHelper(
							EventPop$gridParameter);
					workspacePunyaJenisParameterHelper.initDetail(acara, null);
				}
			}
		});
	}

	public void onClick$tabJurnalPengeluaran$EventPop(Event event) throws Exception {
		// System.out.println("EventPop$jurnalPengeluaran.getChildren() = "
		// + EventPop$jurnalPengeluaran.getChildren());

		if (EventPop$anggaran.getAttribute("workspace") == null) {
			MyMessageboxConfig.show("Pilih salah satu anggaran", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			EventPop$indikator.getLinkedTab().setSelected(true);
			return;
		}

		if (EventPop$jurnalPengeluaran.getChildren().size() == 0) {
			Date beginDate = EventPop$ppbegin.getValue(), endDate = EventPop$ppend.getValue();
			acara.setWorkspace((Workspace) EventPop$anggaran.getAttribute("workspace"));
			acara.setId(null);
			acara.setCntColor(EventPop$cntColor.getValue());
			acara.setHeadColor(EventPop$headColor.getValue());
			acara.setKeterangan(EventPop$ppcnt.getValue());
			acara.setNama("");
			acara.setPpbegin(beginDate);
			acara.setPpend(endDate);
			acara.setPplocked(EventPop$pplocked.isChecked());
			acara.setWorkspace((Workspace) EventPop$anggaran.getAttribute("workspace"));

			if (acara.getWorkspace() != null) {
				HibernateUtil.currentSession().saveOrUpdate(acara);
			}

			session.setAttribute("workspace", EventPop$anggaran.getAttribute("workspace"));
			session.setAttribute("acara", acara);

			MyInclude include = new MyInclude("/pages/master/akunting/transaksi_jurnal_pengeluaran.zul");
			EventPop$jurnalPengeluaran.appendChild(include);
		}
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
	 * @see <a href="http://goo.gl/X6678" target="_zkdemo">ZK Calendar Essentials -
	 * Implementing ZK Calendar Event Listeners</a>
	 */
	public void onEventUpdate$calendars(ForwardEvent event) {
		// Get Original Event
		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();
		KalenderEvent sce = (KalenderEvent) evt.getCalendarEvent();
		sce.setBeginDate(evt.getBeginDate());
		sce.setEndDate(evt.getEndDate());
		((SimpleCalendarModel) calendars.getModel()).update(sce);
	}

	// Create an popup for new event on calendar

	public void onEventCreate$calendars(ForwardEvent event) throws Exception {
		updatePopup((CalendarsEvent) event.getOrigin());
	}

	// Confirm create/update event on canendar
	@SuppressWarnings("unchecked")
	public void onClick$okBtn$EventPop(ForwardEvent event) {
		CalendarsEvent calevt = (CalendarsEvent) EventPop.getAttribute(CALENDAR_EVENT);
		Calendar cal = Calendar.getInstance(calendars.getDefaultTimeZone());
		Date beginDate = EventPop$ppbegin.getValue(), endDate = EventPop$ppend.getValue();
		int beginMin = 0, endMin = 0;
		if (EventPop.getAttribute(CTRL_EVENT) == null) {// Create
			if (!EventPop$ppallDay.isChecked()) {
				String[] times = EventPop$ppbt.getSelectedItem().getLabel().split(":");
				cal.setTime(beginDate);
				cal = setCalendar(cal, Integer.parseInt(times[0]), Integer.parseInt(times[1]), 0, 0);
				beginDate = cal.getTime();
				beginMin = cal.get(Calendar.MINUTE);
				times = EventPop$ppet.getSelectedItem().getLabel().split(":");
				cal.setTime(endDate);
				cal = setCalendar(cal, Integer.parseInt(times[0]), Integer.parseInt(times[1]), 0, 0);
				endDate = cal.getTime();
				endMin = cal.get(Calendar.MINUTE);
			}

			if (!beginDate.before(endDate)) {
				EventPop.setVisible(false);
				alert("The end date cannot be earlier than or equal to begin date!");
				calevt.clearGhost();
				return;
			}
			if (beginMin == 5 || beginMin == 25 || beginMin == 35 || beginMin == 55) {
				EventPop.setVisible(false);
				alert("The begin minute:" + beginMin + ", is not supported");
				calevt.clearGhost();
				return;
			}
			if (endMin == 5 || endMin == 25 || endMin == 35 || endMin == 55) {
				EventPop.setVisible(false);
				alert("The end minute:" + endMin + ", doesn't support");
				calevt.clearGhost();
				return;
			}

			acara.setId(null);
			acara.setCntColor(EventPop$cntColor.getValue());
			acara.setHeadColor(EventPop$headColor.getValue());
			acara.setKeterangan(EventPop$ppcnt.getValue());
			acara.setNama("");
			acara.setPpbegin(beginDate);
			acara.setPpend(endDate);
			acara.setPplocked(EventPop$pplocked.isChecked());
			acara.setWorkspace((Workspace) EventPop$anggaran.getAttribute("workspace"));

			scm.add(new KalenderEvent(acara));

			Session session = HibernateUtil.currentSession();
			session.createSQLQuery(
					"delete from rab.realisasi_workspace_punya_jenis_parameter where acara = " + acara.getId())
					.executeUpdate();
			List<Component> rows = EventPop$gridParameter == null || EventPop$gridParameter.getRows() == null
					? new ArrayList<Component>()
					: EventPop$gridParameter.getRows().getChildren();
			for (Component row : rows) {
				AcaraPunyaJenisParameter realisasiWorkspacePunyaJenisParameter = (AcaraPunyaJenisParameter) row
						.getAttribute("realisasiWorkspacePunyaJenisParameter");
				realisasiWorkspacePunyaJenisParameter.setId(null);
				realisasiWorkspacePunyaJenisParameter.setAcara(acara);
				session.save(realisasiWorkspacePunyaJenisParameter);
			}

			EventPop$ppcnt.setRawValue("");
			EventPop$ppbt.setSelectedIndex(0);
			EventPop$ppet.setSelectedIndex(0);
			EventPop.setVisible(false);

		} else {// Update
			KalenderEvent ce = (KalenderEvent) EventPop.getAttribute(CTRL_EVENT);
			if (!EventPop$ppallDay.isChecked()) {
				String[] times = EventPop$ppbt.getSelectedItem().getLabel().split(":");
				cal.setTime(beginDate);
				cal = setCalendar(cal, Integer.parseInt(times[0]), Integer.parseInt(times[1]), 0, 0);
				beginDate = cal.getTime();
				times = EventPop$ppet.getSelectedItem().getLabel().split(":");
				cal.setTime(endDate);
				cal = setCalendar(cal, Integer.parseInt(times[0]), Integer.parseInt(times[1]), 0, 0);
				endDate = cal.getTime();
			} else {
				cal.setTime(beginDate);
				cal = setCalendar(cal, 0, 0, 0, 0);
				beginDate = cal.getTime();
				beginMin = 0;
				cal.setTime(endDate);
				cal = setCalendar(cal, 0, 0, 0, 0);
				endDate = cal.getTime();
				endMin = 0;
			}
			if (!beginDate.before(endDate)) {
				EventPop.setVisible(false);
				alert("The end date cannot be earlier than or equal to begin date!");
				calevt.clearGhost();
				return;
			}
			if (beginMin == 5 || beginMin == 25 || beginMin == 35 || beginMin == 55) {
				EventPop.setVisible(false);
				alert("The begin minute:" + beginMin + ", is not supported");
				calevt.clearGhost();
				return;
			}
			if (endMin == 5 || endMin == 25 || endMin == 35 || endMin == 55) {
				EventPop.setVisible(false);
				alert("The end minute:" + endMin + ", doesn't support");
				calevt.clearGhost();
				return;
			}
			ce.setHeaderColor(EventPop$headColor.getValue());
			ce.setContentColor(EventPop$cntColor.getValue());
			ce.setBeginDate(beginDate);
			ce.setEndDate(endDate);
			ce.setContent(EventPop$ppcnt.getValue());
			ce.setLocked(EventPop$pplocked.isChecked());
			ce.getAcara().setWorkspace((Workspace) EventPop$anggaran.getAttribute("workspace"));
			scm.update(ce);

			Session session = HibernateUtil.currentSession();
			session.createSQLQuery(
					"delete from rab.realisasi_workspace_punya_jenis_parameter where acara = " + ce.getAcara().getId())
					.executeUpdate();
			List<Row> rows = EventPop$gridParameter.getRows().getChildren();
			for (Row row : rows) {
				AcaraPunyaJenisParameter realisasiWorkspacePunyaJenisParameter = (AcaraPunyaJenisParameter) row
						.getAttribute("realisasiWorkspacePunyaJenisParameter");
				realisasiWorkspacePunyaJenisParameter.setId(null);
				realisasiWorkspacePunyaJenisParameter.setAcara(ce.getAcara());
				session.save(realisasiWorkspacePunyaJenisParameter);
			}

			EventPop.setVisible(false);
			EventPop.setAttribute(CTRL_EVENT, null);
		}
	}

	// Cancel create event on canendar

	public void onClick$cancelBtn$EventPop(ForwardEvent event) {
		EventPop$ppcnt.setRawValue("");
		EventPop$ppbt.setSelectedIndex(0);
		EventPop$ppet.setSelectedIndex(0);
		EventPop.setVisible(false);
		CalendarsEvent calevt = (CalendarsEvent) EventPop.getAttribute(CALENDAR_EVENT);
		if (calevt.getCalendarEvent() == null)
			calevt.clearGhost();
	}

	// Delete created event on canendar

	public void onClick$deleteBtn$EventPop(ForwardEvent event) {
		try {
			MyMessageboxConfig.show("Are you sure to delete the event!", "Question",
					MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
					new EventListener() {
						public void onEvent(Event evt) throws Exception {
							if (((Integer) evt.getData()).intValue() != MyMessageboxConfig.OK)
								return;
							scm.remove((KalenderEvent) EventPop.getAttribute(CTRL_EVENT));
						}
					});
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
		EventPop.setVisible(false);
	}

	// Edit exists event
	public void onEventEdit$calendars(ForwardEvent event) throws Exception {
		updatePopup((CalendarsEvent) event.getOrigin());
	}

	private void pageChange(int page) {
		if (page > 0)
			calendars.previousPage();
		else
			calendars.nextPage();
		currentDate.setLabel(DefaultMonthFormat.format(calendars.getCurrentDate()));
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
	private void updatePopup(CalendarsEvent evt) throws Exception {
		Integer[] location = getPopupPosition(evt);
		EventPop.setLeft(location[0] + "px");
		EventPop.setTop(location[1] + "px");
		if (evt.getCalendarEvent() == null) {
			EventPop$deleteBtn.setVisible(false);
			int timeslots = calendars.getTimeslots();
			int timeslotTime = 60 / timeslots;

			String[] times = timeFormat.format(evt.getBeginDate()).split(":");
			int hours = Integer.parseInt(times[0]) * timeslots;
			int mins = Integer.parseInt(times[1]);
			int bdTimeSum = hours + mins;
			hours += mins / timeslotTime;
			EventPop$ppbt.setSelectedIndex(hours * 12 / timeslots);

			times = timeFormat.format(evt.getEndDate()).split(":");
			hours = Integer.parseInt(times[0]) * timeslots;
			mins = Integer.parseInt(times[1]);
			int edTimeSum = hours + mins;
			hours += mins / timeslotTime;
			EventPop$ppet.setSelectedIndex(hours * 12 / timeslots);
			boolean isAllday = (bdTimeSum + edTimeSum) == 0;

			acara = new Acara();

			EventPop$ppbegin.setValue(evt.getBeginDate());
			EventPop$ppend.setValue(evt.getEndDate());
			EventPop$ppallDay.setChecked(isAllday);
			EventPop$pplocked.setChecked(false);

			EventPop$ppbt.setVisible(!isAllday);
			EventPop$ppet.setVisible(!isAllday);
			EventPop.setAttribute(CTRL_EVENT, null);
			evt.stopClearGhost();
		} else {
			EventPop$deleteBtn.setVisible(true);
			KalenderEvent cevt = (KalenderEvent) evt.getCalendarEvent();
			acara = cevt.getAcara();
			String[] times = timeFormat.format(cevt.getBeginDate()).split(":");
			int hours = Integer.parseInt(times[0]);
			int mins = Integer.parseInt(times[1]);
			int bdTimeSum = hours + mins;
			EventPop$ppbt.setSelectedIndex(hours * 12 + mins / 5);

			times = timeFormat.format(cevt.getEndDate()).split(":");
			hours = Integer.parseInt(times[0]);
			mins = Integer.parseInt(times[1]);
			int edTimeSum = hours + mins;
			EventPop$ppet.setSelectedIndex(hours * 12 + mins / 5);

			boolean isAllday = (bdTimeSum + edTimeSum) == 0;
			EventPop$ppbegin.setValue(cevt.getBeginDate());
			EventPop$ppend.setValue(cevt.getEndDate());
			EventPop$ppallDay.setChecked(isAllday);
			EventPop$pplocked.setChecked(cevt.isLocked());
			EventPop$ppbt.setVisible(!isAllday);
			EventPop$ppet.setVisible(!isAllday);
			EventPop$ppcnt.setValue(cevt.getOriginContent());
			EventPop$headColor.setValue(cevt.getHeaderColor());
			EventPop$cntColor.setValue(cevt.getContentColor());

			EventPop$anggaran.setAttribute("workspace", cevt.getAcara().getWorkspace());
			EventPop$anggaran
					.setValue(cevt.getAcara().getWorkspace() == null ? "" : cevt.getAcara().getWorkspace().toString());

			AcaraPunyaJenisParameterHelper workspacePunyaJenisParameterHelper = new AcaraPunyaJenisParameterHelper(
					EventPop$gridParameter);
			try {
				workspacePunyaJenisParameterHelper.initDetail(cevt.getAcara(), null);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}

			// store for the edit marco component.
			EventPop.setAttribute(CTRL_EVENT, cevt);
		}
		EventPop.setAttribute(CALENDAR_EVENT, evt);
		EventPop.onModal();
		Common.clear(EventPop$jurnalPengeluaran);
		EventPop$indikator.getLinkedTab().setSelected(true);
	}

	// Create Calendar Event - Init Time Dropdown List
	private void initTimeDropdown() {
		List dateTime = new LinkedList();

		Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
		cal = setCalendar(cal, 0, 0, 0, 0);

		for (int i = 0; i < 288; i++) {
			dateTime.add(timeFormat.format(cal.getTime()));
			cal.add(Calendar.MINUTE, 5);
		}
		EventPop$ppbt.setModel(new ListModelList(dateTime));
		EventPop$ppet.setModel(new ListModelList(dateTime));
	}

	private Calendar setCalendar(Calendar cal, int hod, int min, int sec, int milsec) {
		cal.set(Calendar.HOUR_OF_DAY, hod);
		cal.set(Calendar.MINUTE, min);
		cal.set(Calendar.SECOND, sec);
		cal.set(Calendar.MILLISECOND, milsec);
		return cal;
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
