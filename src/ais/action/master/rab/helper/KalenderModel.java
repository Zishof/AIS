package ais.action.master.rab.helper;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.calendar.api.CalendarEvent;
import org.zkoss.calendar.api.RenderContext;
import org.zkoss.calendar.impl.SimpleCalendarModel;
import ais.ui.util.MyMessageboxConfig;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.AcaraHasTransaksi;

public class KalenderModel extends SimpleCalendarModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4085184687946280820L;
	private String filterText = "";

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public KalenderModel(List calendarEvents) {
		super(calendarEvents);

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
		if (e instanceof KalenderEvent) {
			KalenderEvent event = (KalenderEvent) e;
			if (event.getAcara() != null) {
				Session session = HibernateUtil.currentSession();
				session.saveOrUpdate(event.getAcara());
			}
		}
		return super.add(e);
	}

	@Override
	public boolean remove(CalendarEvent e) {
		if (e instanceof KalenderEvent) {
			KalenderEvent event = (KalenderEvent) e;
			Session session = HibernateUtil.currentSession();
			Integer count = ((Number) session
					.createCriteria(AcaraHasTransaksi.class)
					.add(Restrictions.eq("acara", event.getAcara()))
					.setProjection(Projections.rowCount()).uniqueResult())
					.intValue();
			if (!count.equals(0)) {
				try {
					MyMessageboxConfig
							.show("Acara ini mempunyai jurnal transaksi, anda tidak bisa menghapus data acara yang memiliki jurnal transaksi kecuali dihapus terlebih dahulu jurnal terseebut.",
									"Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
				} catch (Exception e1) {
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/rab/helper/KalenderModel.java:88");
				}
				return false;
			}

			if (event.getAcara() != null && event.getAcara().getId() != null) {

				session.createSQLQuery(
						"delete from rab.realisasi_workspace_punya_jenis_parameter where acara = "
								+ event.getAcara().getId()).executeUpdate();
				// session.createSQLQuery(
				// "delete from akunting.acara_has_transaksi where acara = "
				// + event.getAcara().getId()).executeUpdate();
				session.delete(event.getAcara());
			}
		}
		return super.remove(e);
	}

	@Override
	public boolean update(CalendarEvent e) {
		if (e instanceof KalenderEvent) {
			KalenderEvent event = (KalenderEvent) e;
			if (event.getAcara() != null && event.getAcara().getId() != null) {
				Session session = HibernateUtil.currentSession();
				Common.refreshUpdate(session,(event.getAcara()));
			}
		}
		return super.update(e);
	}

}