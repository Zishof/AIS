package ais.action.master.rab.helper;

import java.util.Calendar;
import java.util.Date;

import org.zkoss.calendar.impl.SimpleCalendarEvent;

import ais.database.model.rab.HariLibur;

public class HariLiburKalenderEvent extends SimpleCalendarEvent {

	private HariLibur hariLibur;

	public HariLiburKalenderEvent(HariLibur hariLibur) {
		this.hariLibur = hariLibur;
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(hariLibur.getTanggal());
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);

		Date beginDate = calendar.getTime();

		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(hariLibur.getTanggal());
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);

		Date endDate = calendar.getTime();
		String headerColor = hariLibur.getLibur() ? "red;" : "blue;";
		String contentColor = hariLibur.getLibur() ? "red;" : "blue;";
		String content = hariLibur.getKeterangan();
		String title = hariLibur.getNama();
		boolean locked = true;

		setHeaderColor(headerColor);
		setContentColor(contentColor);
		setContent(content);
		setTitle(title);
		setBeginDate(beginDate);
		setEndDate(endDate);
		setLocked(locked);
	}

	public HariLibur getHariLibur() {
		hariLibur.setKeterangan(super.getContent());
		hariLibur.setNama(getTitle());
		hariLibur.setTanggal(getBeginDate());
		return hariLibur;
	}

	public void setHariLibur(HariLibur hariLibur) {
		this.hariLibur = hariLibur;
	}

	@Override
	public String getContent() {
		String originContent = super.getContent();
		return "<div style=\"width: 100%;height: 100%; background-color: "
				+ (hariLibur.getLibur() ? "red" : "blue")
				+ ";\"><font style=\"color:yellow;font-size: xx-large;backgroud-color:"
				+ (hariLibur.getLibur() ? "red" : "blue") + ";\">"
				+ originContent + "</font><div>";
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