package ais.ui.util;

import java.util.Calendar;
import java.util.Date;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;

public class SmartDateTimeUtil {

	public static String getDayString(Date date, String waktu) {
		return getDayString(ais.ui.util.WaktuUtil.now(), date, waktu);
	}

	public static String getDayString(LocalDateTime dariWaktu, Date date, String waktu) {

		if (waktu == null || waktu.trim().isEmpty()) {
			waktu = null;
		}

		try {
			if (waktu != null) {
				Integer jamMulai = Integer.parseInt(waktu.split("\\.")[0]);
				Integer menitMulai = Integer.parseInt(waktu.split("\\.")[1]);

				Calendar calendar = WaktuUtil.getCalendar();
				calendar.setTime(date);
				calendar.set(Calendar.HOUR_OF_DAY, jamMulai);
				calendar.set(Calendar.MINUTE, menitMulai);
				calendar.set(Calendar.SECOND, 1);
				date = calendar.getTime();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/SmartDateTimeUtil.java:34");
		}

		LocalDateTime birthDate = new LocalDateTime(date);

		Period period = new Period(dariWaktu, birthDate);

		String s;
		if (period.getYears() < -1)
			s = Math.abs(period.getYears()) + " tahun yang lalu";
		else if (period.getYears() == -1)
			s = "Setahun yang lalu";
		else if (period.getMonths() < -1 && period.getYears() == 0)
			s = Math.abs(period.getMonths()) + " bulan yang lalu";
		else if (period.getMonths() == -1 && period.getYears() == 0)
			s = "Sebulan yang lalu";
		else if (period.getWeeks() < -1 && period.getMonths() == 0 && period.getYears() == 0)
			s = Math.abs(period.getWeeks()) + " minggu yang lalu";
		else if (period.getWeeks() == -1 && period.getMonths() == 0 && period.getYears() == 0)
			s = "Seminggu yang lalu";
		else if (period.getDays() < -2 && period.getWeeks() == 0 && period.getMonths() == 0 && period.getYears() == 0)
			s = Math.abs(period.getDays()) + " hari yang lalu";
		else if (period.getDays() == -2 && period.getWeeks() == 0 && period.getMonths() == 0 && period.getYears() == 0)
			s = "Kemarin Lusa";
		else if (period.getDays() == -1 && period.getWeeks() == 0 && period.getMonths() == 0 && period.getYears() == 0)
			s = "Kemarin";
		else if (period.getMinutes() == 0 && period.getHours() == 0 && period.getDays() == 0 && period.getWeeks() == 0
				&& period.getMonths() == 0 && period.getYears() == 0)
			s = "Baru saja";
		else if (period.getMinutes() > 0 && period.getHours() == 0 && period.getDays() == 0 && period.getWeeks() == 0
				&& period.getMonths() == 0 && period.getYears() == 0)
			s = Math.abs(period.getMinutes()) + " menit lagi";
		else if (period.getHours() > 0 && period.getDays() == 0 && period.getWeeks() == 0 && period.getMonths() == 0
				&& period.getYears() == 0)
			s = Math.abs(period.getHours()) + " jam lagi";
		else if (period.getMinutes() < 0 && period.getHours() == 0 && period.getDays() == 0 && period.getWeeks() == 0
				&& period.getMonths() == 0 && period.getYears() == 0)
			s = Math.abs(period.getMinutes()) + " menit yang lalu";
		else if (period.getHours() < 0 && period.getDays() == 0 && period.getWeeks() == 0 && period.getMonths() == 0
				&& period.getYears() == 0)
			s = Math.abs(period.getHours()) + " jam yang lalu";
		else if (period.getDays() == 0 && period.getWeeks() == 0 && period.getMonths() == 0 && period.getYears() == 0)
			s = "Hari ini";
		else if (period.getDays() == 1 && period.getWeeks() == 0 && period.getMonths() == 0 && period.getYears() == 0)
			s = "Besok";
		else if (period.getDays() == 2 && period.getWeeks() == 0 && period.getMonths() == 0 && period.getYears() == 0)
			s = "Besok Lusa";
		else if (period.getDays() > 2 && period.getWeeks() == 0 && period.getMonths() == 0 && period.getYears() == 0)
			s = Math.abs(period.getDays()) + " hari lagi";
		else if (period.getWeeks() == 1 && period.getMonths() == 0 && period.getYears() == 0)
			s = "Seminggu lagi";
		else if (period.getWeeks() > 1 && period.getMonths() == 0 && period.getYears() == 0)
			s = Math.abs(period.getWeeks()) + " minggu lagi";
		else if (period.getMonths() == 1 && period.getYears() == 0)
			s = "Sebulan lagi";
		else if (period.getMonths() > 1 && period.getYears() == 0)
			s = Math.abs(period.getMonths()) + " bulan lagi";
		else if (period.getYears() == 1)
			s = "Setahun lagi";
		else if (period.getYears() > 1)
			s = Math.abs(period.getYears()) + " tahun lagi";
		else
			s = "";
		return s + ", ";
	}

	public static String getDayStringJamMenit(LocalDateTime dariWaktu, Date date, String waktu) {

		if (waktu == null || waktu.trim().isEmpty()) {
			waktu = null;
		}

		try {
			if (waktu != null) {
				Integer jamMulai = Integer.parseInt(waktu.split("\\.")[0]);
				Integer menitMulai = Integer.parseInt(waktu.split("\\.")[1]);

				Calendar calendar = WaktuUtil.getCalendar();
				calendar.setTime(date);
				calendar.set(Calendar.HOUR_OF_DAY, jamMulai);
				calendar.set(Calendar.MINUTE, menitMulai);
				calendar.set(Calendar.SECOND, 1);
				date = calendar.getTime();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/SmartDateTimeUtil.java:119");
		}

		LocalDateTime birthDate = new LocalDateTime(date);

		Period period = new Period(dariWaktu, birthDate);

		String s;
		if (period.getMinutes() == 0 && period.getHours() == 0 && period.getDays() == 0 && period.getWeeks() == 0
				&& period.getMonths() == 0 && period.getYears() == 0)
			s = "Baru saja";
		else if (period.getMinutes() > 0 && period.getHours() == 0 && period.getDays() == 0 && period.getWeeks() == 0
				&& period.getMonths() == 0 && period.getYears() == 0)
			s = Math.abs(period.getMinutes()) + " menit";
		else
			s = Math.abs(period.getHours()) + " jam " + Math.abs(period.getMinutes()) + " menit";

		return s;
	}

}
