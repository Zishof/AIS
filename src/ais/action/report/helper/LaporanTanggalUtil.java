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

    /**
     * Konstruktor privat. Kelas ini hanya berisi method statis (utility class)
     * sehingga tidak dimaksudkan untuk diinstansiasi.
     */
    private LaporanTanggalUtil() {
    }

    /**
     * Menormalisasi {@code date} ke awal hari yang sama (00:00:00.000).
     *
     * @param date tanggal/waktu sumber; boleh {@code null}.
     * @return {@link Date} baru pada awal hari yang sama dengan {@code date},
     *         atau {@code null} bila {@code date} {@code null}.
     */
    public static Date awalHari(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = WaktuUtil.getCalendar();
        calendar.setTime(date);
        normalisasiJamAwalHari(calendar);
        return calendar.getTime();
    }

    /**
     * Menormalisasi {@code date} ke akhir hari yang sama (23:59:59.999).
     * <p>
     * Dipakai agar query database yang membandingkan rentang tanggal
     * (misalnya {@code BETWEEN mulai AND sampai}) tetap mencakup seluruh
     * baris pada hari terakhir periode, walau kolom tanggalnya menyimpan
     * komponen waktu.
     *
     * @param date tanggal/waktu sumber; boleh {@code null}.
     * @return {@link Date} baru pada akhir hari yang sama dengan {@code date},
     *         atau {@code null} bila {@code date} {@code null}.
     */
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

    /**
     * Menyetel komponen jam/menit/detik/milidetik {@code calendar} ke
     * 00:00:00.000 secara in-place, tanpa mengubah tanggal (tahun/bulan/hari)
     * yang sudah tersimpan pada {@code calendar}.
     *
     * @param calendar instance {@link Calendar} yang diubah in-place; method
     *                 tidak melakukan apa pun bila {@code null}.
     */
    public static void normalisasiJamAwalHari(Calendar calendar) {
        if (calendar == null) {
            return;
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    /**
     * Membuat {@link Calendar} baru (lewat {@link WaktuUtil#getCalendar()})
     * yang disetel ke awal hari dari {@code date}.
     *
     * @param date tanggal sumber; bila {@code null}, {@link Calendar} yang
     *             dikembalikan tetap berisi waktu saat ini (default dari
     *             {@link WaktuUtil#getCalendar()}) namun jamnya dinormalisasi
     *             ke awal hari.
     * @return {@link Calendar} baru pada awal hari yang bersangkutan.
     */
    public static Calendar calendarAwalHari(Date date) {
        Calendar calendar = WaktuUtil.getCalendar();
        if (date != null) {
            calendar.setTime(date);
        }
        normalisasiJamAwalHari(calendar);
        return calendar;
    }

    /**
     * Menghitung batas eksklusif untuk loop tanggal inklusif: awal hari
     * {@code sampai} ditambah satu hari.
     * <p>
     * Dipakai pada pola loop {@code while (cursor.before(batasEksklusifBesok))}
     * agar hari terakhir periode ({@code sampai}) tetap ikut diproses tanpa
     * memerlukan perbandingan {@code <=} yang rawan salah pada komponen jam.
     *
     * @param sampai tanggal akhir periode (inklusif); lihat
     *               {@link #calendarAwalHari(Date)} untuk perlakuan
     *               {@code null}.
     * @return {@link Calendar} pada awal hari setelah {@code sampai}.
     */
    public static Calendar batasEksklusifBesok(Date sampai) {
        Calendar calendar = calendarAwalHari(sampai);
        calendar.add(Calendar.DATE, 1);
        return calendar;
    }

    /**
     * Menghitung jumlah hari kalender dari {@code mulai} sampai {@code sampai}
     * secara inklusif (kedua ujung dihitung), tanpa memperhitungkan komponen
     * jam pada kedua tanggal (dibandingkan pada awal hari masing-masing).
     * <p>
     * Perhitungan dibatasi maksimum 36600 hari (~100 tahun) sebagai pengaman
     * agar loop tidak berjalan tanpa batas bila terjadi kesalahan input;
     * pada kondisi itu method mengembalikan 36600 tanpa melempar exception.
     *
     * @param mulai  tanggal awal periode (inklusif); boleh {@code null}.
     * @param sampai tanggal akhir periode (inklusif); boleh {@code null}.
     * @return jumlah hari inklusif, atau {@code 0} bila salah satu argumen
     *         {@code null} atau bila {@code sampai} jatuh sebelum {@code mulai}.
     */
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
