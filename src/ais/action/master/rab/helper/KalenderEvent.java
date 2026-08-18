package ais.action.master.rab.helper;

import java.sql.Time;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.calendar.impl.SimpleCalendarEvent;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.Acara;
import ais.database.model.rab.AcaraPunyaJenisParameter;
import ais.database.model.rab.WorkspacePunyaJenisParameter;

public class KalenderEvent extends SimpleCalendarEvent {

	private Acara acara;

	public KalenderEvent(Acara acara) {
		this.setAcara(acara);
		Date beginDate = acara.getPpbegin();
		Date endDate = acara.getPpend();
		String headerColor = acara.getHeadColor();
		String contentColor = acara.getCntColor();
		String content = acara.getKeterangan();
		String title = acara.getNama();
		boolean locked = acara.getPplocked();

		setHeaderColor(headerColor);
		setContentColor(contentColor);
		setContent(content);
		setTitle(title);
		setBeginDate(beginDate);
		setEndDate(endDate);
		setLocked(locked);
	}

	public Acara getAcara() {
		acara.setCntColor(getContentColor());
		acara.setHeadColor(getHeaderColor());
		acara.setKeterangan(super.getContent());
		acara.setNama(getTitle());
		acara.setPpbegin(getBeginDate());
		acara.setPpend(getEndDate());
		acara.setPplocked(isLocked());
		return acara;
	}

	public void setAcara(Acara acara) {
		this.acara = acara;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String getContent() {
		String originContent = super.getContent();
		if (acara != null && acara.getWorkspace() != null) {
			originContent += (originContent.trim().equals("") ? "" : "<br>")
					+ acara.getWorkspace().toString();

			Session session = HibernateUtil.currentSession();
			List<WorkspacePunyaJenisParameter> workspacePunyaJenisParameters = session
					.createCriteria(WorkspacePunyaJenisParameter.class)
					.add(Restrictions.eq("workspace", acara.getWorkspace()))
					.list();
			for (WorkspacePunyaJenisParameter workspacePunyaJenisParameter : workspacePunyaJenisParameters) {

				AcaraPunyaJenisParameter realisasiWorkspacePunyaJenisParameter = (AcaraPunyaJenisParameter) session
						.createCriteria(
								AcaraPunyaJenisParameter.class)
						.add(Restrictions.eq("workspacePunyaJenisParameter",
								workspacePunyaJenisParameter))
						.add(Restrictions.eq("acara", acara)).setMaxResults(1)
						.uniqueResult();

				String value = "-";
				if (realisasiWorkspacePunyaJenisParameter != null) {
					if (workspacePunyaJenisParameter.getJenisParameter()
							.getTypedata().equals(String.class.getName())) {
						value = realisasiWorkspacePunyaJenisParameter
								.getJenisParameterValue();
					} else if (workspacePunyaJenisParameter.getJenisParameter()
							.getTypedata().equals(Integer.class.getName())) {
						value = realisasiWorkspacePunyaJenisParameter
								.getJenisParameterValueInteger() == null ? value
								: Common.numberFormat.get()
										.format(realisasiWorkspacePunyaJenisParameter
												.getJenisParameterValueInteger());
					} else if (workspacePunyaJenisParameter.getJenisParameter()
							.getTypedata().equals(Double.class.getName())) {
						value = realisasiWorkspacePunyaJenisParameter
								.getJenisParameterValueDouble() == null ? value
								: Common.numberFormat.get()
										.format(realisasiWorkspacePunyaJenisParameter
												.getJenisParameterValueDouble());
					} else if (workspacePunyaJenisParameter.getJenisParameter()
							.getTypedata().equals(Date.class.getName())) {
						value = realisasiWorkspacePunyaJenisParameter
								.getJenisParameterValueDate() == null ? value
								: Common.dateFormat.get()
										.format(realisasiWorkspacePunyaJenisParameter
												.getJenisParameterValueDate());
					} else if (workspacePunyaJenisParameter.getJenisParameter()
							.getTypedata().equals(Time.class.getName())) {
						value = realisasiWorkspacePunyaJenisParameter
								.getJenisParameterValueTime() == null ? value
								: Common.timeFormat.get()
										.format(realisasiWorkspacePunyaJenisParameter
												.getJenisParameterValueTime());
					}
				}

				originContent += (originContent.trim().equals("") ? "" : "<br>")
						+ workspacePunyaJenisParameter.getJenisParameter()
								.getNama() + " : " + value;
			}
		}
		return "<font style=\"font-size: 8px;\">" + originContent + "</font>";
	}

	public String getOriginContent() {
		String originContent = super.getContent();
		return originContent;
	}

	@Override
	public void setContent(String content) {
		super.setContent(content);
	}

}