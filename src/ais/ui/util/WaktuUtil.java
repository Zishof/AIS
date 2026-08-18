package ais.ui.util;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

import org.joda.time.LocalDateTime;

import ais.common.Common;

public class WaktuUtil {

	public static int PENAMBAHAN_WAKTU = 0;

	public static String gteTimezoneName() {
		String timeZone = "Asia/Jakarta";
		if (PENAMBAHAN_WAKTU == 1) {
			timeZone = "Asia/Makassar";
		} else if (PENAMBAHAN_WAKTU == 2) {
			timeZone = "Asia/Jayapura";
		}
		return timeZone;
	}

	public static void reinit() {
		try {
			PENAMBAHAN_WAKTU = Integer
					.parseInt(Common.getKonfigurasi("PENAMBAHAN_WAKTU", PENAMBAHAN_WAKTU + "").getNilai());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/WaktuUtil.java:32");
		}
	}

	public static Calendar getCalendar() {
		Calendar calendar = Calendar.getInstance();
		if (PENAMBAHAN_WAKTU != 0) {
			calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + PENAMBAHAN_WAKTU);
		}
		return calendar;
	}
	

	public static Date duaTahunLalu() {
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 2);
		return calendar.getTime();
	}

	public static Date tahunLalu() {
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
		return calendar.getTime();
	}

	public static String formatTanggalLengkap(Date tanggal) {
		return Common.dateFormat51.get().format(tanggal);
	}

	public static Date tahunAkandatang() {
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
		return calendar.getTime();
	}

	public static Date kemarin() {
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
		return calendar.getTime();
	}

	public static Date besok() {
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		return calendar.getTime();
	}

	public static Date minggudepan() {
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.WEEK_OF_MONTH, calendar.get(Calendar.WEEK_OF_MONTH) + 1);
		return calendar.getTime();
	}

	public static Date bulandepan() {
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
		return calendar.getTime();
	}

	public static Date besoklusa() {
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 2);
		return calendar.getTime();
	}

	public static Date besok(Date date) {
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.setTime(date);
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		return calendar.getTime();
	}

	public static Date getDate() {
		return WaktuUtil.getCalendar().getTime();
	}

	public static LocalDateTime sekarang() {
		return new LocalDateTime(WaktuUtil.getDate());
	}

	public static LocalDateTime now() {
		return new LocalDateTime(WaktuUtil.getDate());
	}

	public static String formatDate(Date tanggal, String format) {
		if (tanggal == null || format == null || format.isEmpty())
			return "";

		// DateTimeFormatter thread-safe, aman jika ingin dijadikan static final di
		// level class
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

		return tanggal.toInstant().atZone(ZoneId.systemDefault()).format(formatter);
	}
	
	
	public static Date parseDate(String tanggal, String format) {
	    if (tanggal == null || format == null || format.isEmpty()) {
	        return null;
	    }

	    try {
	        // Membuat formatter
	        SimpleDateFormat sdf = new SimpleDateFormat(format);
	        // Mengubah teks menjadi Date
	        return sdf.parse(tanggal); 
	    } catch (Exception e) {
	        // Jika format tanggal salah/tidak sesuai, kembalikan null atau lempar exception
	        System.err.println("Gagal parsing tanggal: " + e.getMessage());
	        return null; 
	    }
	}
}
