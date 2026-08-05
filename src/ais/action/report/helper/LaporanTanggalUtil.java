package ais.action.report.helper;

import java.util.Calendar;
import java.util.Date;

import ais.ui.util.WaktuUtil;

/**
 * Utilitas kecil untuk menyamakan cara laporan menghitung rentang tanggal.
 *
 * Banyak laporan memakai tanggal akhir yang sudah dinormalisasi sampai 23:59:59,
 * lalu Calendar laporan ditambah satu hari agar loop bersifat inklusif. Jika jam
 * akhir tidak diturunkan dulu ke 00:00:00, tanggal setelah periode ikut tercetak.
 * Class ini menjaga agar query database tetap bisa memakai akhir hari, sedangkan
 * iterasi tampilan selalu memakai tanggal murni.
 */
public final class LaporanTanggalUtil {

    private LaporanTanggalUtil() {
    }

    public static Date awalHari(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = WaktuUtil.getCalendar();
        calendar.setTime(date);
        normalisasiJamAwalHari(calendar);
        return calendar.getTime();
    }

    public static Date akhirHari(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = WaktuUtil.getCalendar();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    public static void normalisasiJamAwalHari(Calendar calendar) {
        if (calendar == null) {
            return;
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    public static Calendar calendarAwalHari(Date date) {
        Calendar calendar = WaktuUtil.getCalendar();
        if (date != null) {
            calendar.setTime(date);
        }
        normalisasiJamAwalHari(calendar);
        return calendar;
    }

    public static Calendar batasEksklusifBesok(Date sampai) {
        Calendar calendar = calendarAwalHari(sampai);
        calendar.add(Calendar.DATE, 1);
        return calendar;
    }

    public static int jumlahHariInklusif(Date mulai, Date sampai) {
        if (mulai == null || sampai == null) {
            return 0;
        }
        Calendar awal = calendarAwalHari(mulai);
        Calendar akhir = calendarAwalHari(sampai);
        if (akhir.before(awal)) {
            return 0;
        }
        int jumlah = 0;
        while (!awal.after(akhir)) {
            jumlah++;
            awal.add(Calendar.DATE, 1);
            if (jumlah > 36600) {
                break;
            }
        }
        return jumlah;
    }
}
