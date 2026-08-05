package ais.action.master.rab.helper;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.zkoss.calendar.api.CalendarEvent;
import org.zkoss.calendar.api.RenderContext;
import org.zkoss.calendar.impl.SimpleCalendarModel;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;

public class HariLiburKalenderModel extends SimpleCalendarModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4085184687946280820L;
	private String filterText = "";
	@SuppressWarnings("rawtypes")
	private Set calendarEvents;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public HariLiburKalenderModel(Set calendarEvents) {
		super(calendarEvents);
		this.calendarEvents = calendarEvents;
	}

	public void setFilterText(String filterText) {
		this.filterText = filterText;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List get(Date beginDate, Date endDate, RenderContext rc) {
		List list = new LinkedList();
		long begin = beginDate.getTime();
		long end = endDate.getTime();
		Iterator i$ = _list.iterator();
		do {
			if (!i$.hasNext())
				break;
			CalendarEvent ce = (CalendarEvent) i$.next();
			long b = ce.getBeginDate().getTime();
			long e = ce.getEndDate().getTime();
			if (e >= begin
					&& b < end
					&& ce.getContent().toLowerCase()
							.contains(filterText.toLowerCase()))
				list.add(ce);
		} while (true);
		return list;
	}

	@Override
	public boolean add(CalendarEvent e) {
		if (e instanceof HariLiburKalenderEvent) {
			HariLiburKalenderEvent event = (HariLiburKalenderEvent) e;
			if (event.getHariLibur() != null) {
				Session session = HibernateUtil.currentSession();
				session.save(event.getHariLibur());
			}
		}
		return super.add(e);
	}

	@Override
	public boolean remove(CalendarEvent e) {
		if (e instanceof HariLiburKalenderEvent) {
			HariLiburKalenderEvent event = (HariLiburKalenderEvent) e;
			if (event.getHariLibur() != null
					&& event.getHariLibur().getId() != null) {
				Session session = HibernateUtil.currentSession();
				session.delete(event.getHariLibur());
			}
		}
		return super.remove(e);
	}

	@Override
	public boolean update(CalendarEvent e) {
		if (e instanceof HariLiburKalenderEvent) {
			HariLiburKalenderEvent event = (HariLiburKalenderEvent) e;
			if (event.getHariLibur() != null
					&& event.getHariLibur().getId() != null) {
				Session session = HibernateUtil.currentSession();
				Common.refreshUpdate(session,(event.getHariLibur()));
			}
		}
		return super.update(e);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setList(Set calendarEvents) {
		this.calendarEvents.addAll(calendarEvents);
		_list.clear();
		_list.addAll(this.calendarEvents);
	}

}