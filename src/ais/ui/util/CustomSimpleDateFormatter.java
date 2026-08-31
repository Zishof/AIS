package ais.ui.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.zkoss.calendar.api.DateFormatter;

/**
 * Implementasi {@link DateFormatter} kustom untuk komponen kalender ZK ({@code
 * org.zkoss.calendar}) yang dipakai di layar-layar AIS yang menampilkan komponen kalender
 * visual (mis. jadwal). Menyediakan format tampilan berbeda untuk setiap konteks label
 * kalender (nama hari, tanggal, waktu, popup, nomor minggu), dengan pola format hari/minggu
 * yang dapat disesuaikan lewat konstruktor {@link #CustomSimpleDateFormatter(String)}.
 * Instance {@link SimpleDateFormat} untuk tiap konteks dibuat malas (lazy, hanya sekali) dan
 * dipakai ulang antar pemanggilan, dengan {@link TimeZone} diset ulang setiap pemanggilan agar
 * mengikuti zona waktu yang diminta pemanggil komponen kalender.
 */
public class CustomSimpleDateFormatter implements DateFormatter {
	/** Pola format nama hari (dipakai {@link #getCaptionByDate}); default {@code "EEEEE"} (satu huruf inisial hari). */
	private String _dayFormat = "EEEEE";
	/** Pola format singkatan hari (dipakai {@link #getCaptionByDayOfWeek}); default {@code "EEE"}. */
	private String _weekFormat = "EEE";
	/** Pola format jam:menit (dipakai {@link #getCaptionByTimeOfDay}); default {@code "HH:mm"}. */
	private String _timeFormat = "HH:mm";
	/** Pola format tanggal untuk popup kalender (dipakai {@link #getCaptionByPopup}); default {@code "EEE, MMM/d"}. */
	private String _ppFormat = "EEE, MMM/d";
	/** Instance formatter yang dibuat malas (lazy) dan dipakai ulang untuk masing-masing konteks di atas. */
	private SimpleDateFormat _df, _wf, _tf, _pf;

	/** Membuat formatter dengan seluruh pola format bawaan (default). */
	public CustomSimpleDateFormatter() {

	}

	/**
	 * Membuat formatter dengan pola format nama hari kustom, pola konteks lain tetap default.
	 *
	 * @param _dayFormat pola {@link SimpleDateFormat} untuk label hari (mis. {@code "EEEE"} untuk nama hari penuh)
	 */
	public CustomSimpleDateFormatter(String _dayFormat) {
		this._dayFormat = _dayFormat;
	}

	/**
	 * Mengembalikan label hari untuk satu tanggal, memakai pola {@link #_dayFormat}.
	 *
	 * @param date     tanggal yang akan diformat
	 * @param locale   locale untuk pemformatan nama hari
	 * @param timezone zona waktu yang diterapkan sebelum pemformatan
	 * @return label hari sesuai pola {@link #_dayFormat}
	 */
	public String getCaptionByDate(Date date, Locale locale, TimeZone timezone) {
		if (_df == null) {
			_df = new SimpleDateFormat(_dayFormat, locale);
		}
		_df.setTimeZone(timezone);
		return _df.format(date);
	}

	/**
	 * Mengembalikan label tanggal untuk sel hari dalam tampilan bulan: bila tanggal jatuh pada
	 * tanggal 1, label memakai format {@code "MMM d"} (mis. "Jan 1") agar bulan terlihat jelas
	 * pada pergantian bulan; selain itu hanya mengembalikan angka tanggal (hari dalam bulan).
	 *
	 * @param date     tanggal yang akan diformat
	 * @param locale   locale untuk pemformatan nama bulan
	 * @param timezone zona waktu yang diterapkan sebelum pemformatan
	 * @return label tanggal, memuat nama bulan bila tanggal 1, atau sekadar angka tanggal
	 */
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

	/**
	 * Mengembalikan label singkatan hari dalam minggu, memakai pola {@link #_weekFormat}.
	 *
	 * @param date     tanggal yang akan diformat
	 * @param locale   locale untuk pemformatan nama hari
	 * @param timezone zona waktu yang diterapkan sebelum pemformatan
	 * @return label singkatan hari sesuai pola {@link #_weekFormat}
	 */
	public String getCaptionByDayOfWeek(Date date, Locale locale, TimeZone timezone) {
		if (_wf == null) {
			_wf = new SimpleDateFormat(_weekFormat, locale);
		}
		_wf.setTimeZone(timezone);
		return _wf.format(date);
	}

	/**
	 * Mengembalikan label jam:menit, memakai pola {@link #_timeFormat}.
	 *
	 * @param date     tanggal/waktu yang akan diformat
	 * @param locale   locale untuk pemformatan
	 * @param timezone zona waktu yang diterapkan sebelum pemformatan
	 * @return label waktu sesuai pola {@link #_timeFormat}
	 */
	public String getCaptionByTimeOfDay(Date date, Locale locale, TimeZone timezone) {
		if (_tf == null) {
			_tf = new SimpleDateFormat(_timeFormat, locale);
		}
		_tf.setTimeZone(timezone);
		return _tf.format(date);
	}

	/**
	 * Mengembalikan label tanggal untuk popup pemilih kalender, memakai pola {@link #_ppFormat}.
	 *
	 * @param date     tanggal yang akan diformat
	 * @param locale   locale untuk pemformatan
	 * @param timezone zona waktu yang diterapkan sebelum pemformatan
	 * @return label tanggal sesuai pola {@link #_ppFormat}
	 */
	public String getCaptionByPopup(Date date, Locale locale, TimeZone timezone) {
		if (_pf == null) {
			_pf = new SimpleDateFormat(_ppFormat, locale);
		}
		_pf.setTimeZone(timezone);
		return _pf.format(date);
	}

	/**
	 * Mengembalikan nomor minggu dalam tahun untuk tanggal yang diberikan, sebagai teks angka.
	 *
	 * @param date     tanggal yang akan dihitung nomor mingguannya
	 * @param locale   locale yang menentukan konvensi awal minggu
	 * @param timezone zona waktu yang diterapkan sebelum perhitungan
	 * @return nomor minggu dalam tahun, dalam bentuk teks
	 */
	public String getCaptionByWeekOfYear(Date date, Locale locale, TimeZone timezone) {
		Calendar cal = Calendar.getInstance(timezone, locale);
		cal.setTime(date);
		return String.valueOf(cal.get(Calendar.WEEK_OF_YEAR));
	}
}