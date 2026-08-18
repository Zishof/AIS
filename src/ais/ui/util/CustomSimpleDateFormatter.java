package ais.ui.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.zkoss.calendar.api.DateFormatter;

public class CustomSimpleDateFormatter implements DateFormatter {
	private String _dayFormat = "EEEEE";
	private String _weekFormat = "EEE";
	private String _timeFormat = "HH:mm";
	private String _ppFormat = "EEE, MMM/d";
	private SimpleDateFormat _df, _wf, _tf, _pf;

	public CustomSimpleDateFormatter() {

	}

	public CustomSimpleDateFormatter(String _dayFormat) {
		this._dayFormat = _dayFormat;
	}

	public String getCaptionByDate(Date date, Locale locale, TimeZone timezone) {
		if (_df == null) {
			_df = new SimpleDateFormat(_dayFormat, locale);
		}
		_df.setTimeZone(timezone);
		return _df.format(date);
	}

	public String getCaptionByDateOfMonth(Date date, Locale locale, TimeZone timezone) {
		Calendar cal = Calendar.getInstance(timezone, locale);
		cal.setTime(date);
		if (cal.get(Calendar.DAY_OF_MONTH) == 1) {
			SimpleDateFormat sd = new SimpleDateFormat("MMM d", locale);
			sd.setTimeZone(timezone);
			return sd.format(date);
		}
		return Integer.toString(cal.get(Calendar.DAY_OF_MONTH));
	}

	public String getCaptionByDayOfWeek(Date date, Locale locale, TimeZone timezone) {
		if (_wf == null) {
			_wf = new SimpleDateFormat(_weekFormat, locale);
		}
		_wf.setTimeZone(timezone);
		return _wf.format(date);
	}

	public String getCaptionByTimeOfDay(Date date, Locale locale, TimeZone timezone) {
		if (_tf == null) {
			_tf = new SimpleDateFormat(_timeFormat, locale);
		}
		_tf.setTimeZone(timezone);
		return _tf.format(date);
	}

	public String getCaptionByPopup(Date date, Locale locale, TimeZone timezone) {
		if (_pf == null) {
			_pf = new SimpleDateFormat(_ppFormat, locale);
		}
		_pf.setTimeZone(timezone);
		return _pf.format(date);
	}

	public String getCaptionByWeekOfYear(Date date, Locale locale, TimeZone timezone) {
		Calendar cal = Calendar.getInstance(timezone, locale);
		cal.setTime(date);
		return String.valueOf(cal.get(Calendar.WEEK_OF_YEAR));
	}
}