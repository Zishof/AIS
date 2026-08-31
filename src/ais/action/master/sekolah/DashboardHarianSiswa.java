package ais.action.master.sekolah;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;

import ais.database.model.PerguruanTinggi;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyHtml;

/**
 * <h1>DashboardHarianSiswa — Tren Pendaftaran Harian Calon Siswa (PSB)</h1>
 *
 * <p>Menjawab pertanyaan: <em>"Kapan puncak pendaftaran calon siswa, dan apakah
 * ada penurunan signifikan yang perlu diantisipasi?"</em> Dasbor ini membantu tim
 * PSB memantau denyut nadi penerimaan secara real-time, sehingga lonjakan atau
 * penurunan mendadak bisa segera direspons.</p>
 *
 * <p>Tiga panel yang disajikan:</p>
 * <ul>
 *   <li><b>KPI Hari Ini</b> — Jumlah pendaftar hari ini, kemarin, 7 hari terakhir,
 *       rata-rata harian, dan total keseluruhan tahun masuk berjalan.</li>
 *   <li><b>Tren 30 Hari Terakhir</b> — Line chart harian selama 30 hari ke belakang.
 *       Pola dan tren terlihat jelas: apakah pendaftaran naik, stabil, atau menurun.</li>
 *   <li><b>Perbandingan Mingguan</b> — Bar grouped per hari dalam seminggu: minggu ini
 *       vs minggu lalu. Berguna untuk memahami pola weekday vs weekend.</li>
 * </ul>
 *
 * <p>Field yang digunakan: {@code CalonSiswa.tanggalPendaftaran} (Date).
 * Filter: {@code cs.tahunMasuk = :tahunMasuk} dengan komparasi tanggal via
 * {@code YEAR()}, {@code MONTH()}, dan {@code DAY()} HQL functions.</p>
 *
 * @see DashboardSiswaBase
 */
public class DashboardHarianSiswa extends DashboardSiswaBase {

    // ═══════════════════════════════════════════════════════════════
    // Inner data class
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tipe implementasi bersarang {@link HariData} milik {@link DashboardHarianSiswa}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DashboardHarianSiswa}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
     * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String label}, {@code int jumlah}.
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see DashboardHarianSiswa
     */
    private static final class HariData {
        String label;
        int    jumlah;

        HariData(String label, int jumlah) {
            this.label  = label;
            this.jumlah = jumlah;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Konstanta
    // ═══════════════════════════════════════════════════════════════

    private static final int    WINDOW_DAYS = 30;
    private static final String[] NAMA_HARI = {
        "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"
    };

    // ═══════════════════════════════════════════════════════════════
    // Konstruktor
    // ═══════════════════════════════════════════════════════════════

    public DashboardHarianSiswa(PerguruanTinggi pt) {
        super(pt);
    }

    // ═══════════════════════════════════════════════════════════════
    // doRefreshSiswa
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void doRefreshSiswa(Session session, int tahunMasuk) {
        Date today     = new Date();
        Date yesterday = shiftDays(today, -1);
        Date sevenDaysAgo = shiftDays(today, -7);

        int hariIni  = queryCountBetween(session, tahunMasuk, today,     shiftDays(today, 1));
        int kemarin  = queryCountBetween(session, tahunMasuk, yesterday, today);
        int tujuhHar = queryCountBetween(session, tahunMasuk, sevenDaysAgo, shiftDays(today, 1));
        int total    = queryCountYear(session, tahunMasuk);

        double rataRata = tujuhHar / 7.0;

        List<HariData> tren30  = queryTren(session, tahunMasuk, today);
        int[][]        mingguan = queryWeekly(session, tahunMasuk, today);

        StringBuilder sb = new StringBuilder(8192);

        // ── Header
        sb.append("<div style=\"margin-bottom:14px;\">")
          .append("<div style=\"font-size:18px;font-weight:700;color:#1e3a5f;margin-bottom:3px;\">")
          .append("Tren Pendaftaran Harian &#8212; Tahun Masuk ").append(tahunMasuk)
          .append("</div>")
          .append("<div style=\"font-size:12px;color:#9ca3af;\">")
          .append("Pantau denyut nadi pendaftaran secara real-time. "
                + "Lonjakan atau penurunan mendadak harus segera direspons tim PSB.")
          .append("</div></div>");

        // ── KPI
        sb.append(HtmlChartHelper.kpiCards(
                new String[] { "Hari Ini", "Kemarin", "7 Hari Terakhir",
                        "Rata-rata/Hari", "Total Tahun Ini" },
                new String[] { fmtAngka(hariIni), fmtAngka(kemarin),
                        fmtAngka(tujuhHar), fmt1(rataRata), fmtAngka(total) },
                new String[] { "pendaftar baru", "pendaftar", "total pendaftar",
                        "7 hari terakhir", "tahun masuk " + tahunMasuk },
                new String[] { "", "", "", "", "" },
                new boolean[] { hariIni > 0, true, true, rataRata >= 1, true },
                new String[] { "#059669", "#3b82f6", "#7c3aed", "#f59e0b", "#1877f2" }));

        // ── Tren 30 hari
        if (!tren30.isEmpty()) {
            int n = tren30.size();
            String[] lbl = new String[n];
            double[] val = new double[n];
            for (int i = 0; i < n; i++) {
                lbl[i] = tren30.get(i).label;
                val[i] = tren30.get(i).jumlah;
            }
            sb.append(fullWidth(HtmlChartHelper.lineMulti(
                    "Tren Pendaftaran 30 Hari Terakhir",
                    "Jumlah calon siswa baru yang mendaftar setiap harinya dalam 30 hari terakhir. "
                    + "Puncak pendaftaran biasanya terjadi menjelang penutupan gelombang — "
                    + "pastikan server dan tim siap menghadapi lonjakan.",
                    lbl,
                    new String[] { "Pendaftar" },
                    new double[][] { val },
                    new String[] { "#3b82f6" })));
        } else {
            sb.append(fullWidth(emptyCard("Tren 30 Hari",
                    "Belum ada data tanggal pendaftaran untuk tahun masuk " + tahunMasuk + ".")));
        }

        // ── Perbandingan mingguan
        if (mingguan != null) {
            double[][] weekVals = new double[7][2];
            for (int d = 0; d < 7; d++) {
                weekVals[d][0] = mingguan[0][d];
                weekVals[d][1] = mingguan[1][d];
            }
            sb.append(fullWidth(HtmlChartHelper.barVerticalGrouped(
                    "Perbandingan: Minggu Ini vs Minggu Lalu",
                    "Jumlah pendaftar per hari dalam seminggu: biru = minggu ini, "
                    + "hijau = minggu lalu. Pola hari kerja vs akhir pekan terlihat jelas.",
                    NAMA_HARI,
                    new String[] { "Minggu Ini", "Minggu Lalu" },
                    weekVals,
                    new String[] { "#3b82f6", "#10b981" })));
        }

        new MyHtml(sb.toString()).setParent(contentHolder);
    }

    // ═══════════════════════════════════════════════════════════════
    // Query methods
    // ═══════════════════════════════════════════════════════════════

    /** Hitung pendaftar antara [from, to) berdasarkan tanggalPendaftaran. */
    @SuppressWarnings("unchecked")
    private int queryCountBetween(Session session, int tahunMasuk, Date from, Date to) {
        try {
            String hql = "SELECT COUNT(cs) FROM CalonSiswa cs "
                    + "WHERE cs.tahunMasuk = :tahunMasuk "
                    + "AND cs.tanggalPendaftaran >= :from "
                    + "AND cs.tanggalPendaftaran < :to";
            Number n = (Number) session.createQuery(hql)
                    .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk))
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .uniqueResult();
            return n == null ? 0 : n.intValue();
        } catch (Exception e) {
            logErr("DashboardHarianSiswa.queryCountBetween tahunMasuk=" + tahunMasuk, e);
            return 0;
        }
    }

    /** Total pendaftar seluruh tahun masuk (tidak dibatasi tanggal). */
    @SuppressWarnings("unchecked")
    private int queryCountYear(Session session, int tahunMasuk) {
        try {
            String hql = "SELECT COUNT(cs) FROM CalonSiswa cs "
                    + "WHERE cs.tahunMasuk = :tahunMasuk";
            Number n = (Number) session.createQuery(hql)
                    .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk)).uniqueResult();
            return n == null ? 0 : n.intValue();
        } catch (Exception e) {
            logErr("DashboardHarianSiswa.queryCountYear tahunMasuk=" + tahunMasuk, e);
            return 0;
        }
    }

    /** Tren harian 30 hari terakhir dari tanggal today. */
    @SuppressWarnings("unchecked")
    private List<HariData> queryTren(Session session, int tahunMasuk, Date today) {
        List<HariData> result = new ArrayList<HariData>();
        try {
            Date from = shiftDays(today, -WINDOW_DAYS + 1);
            String hql = "SELECT YEAR(cs.tanggalPendaftaran), "
                    + "MONTH(cs.tanggalPendaftaran), "
                    + "DAY(cs.tanggalPendaftaran), COUNT(cs) "
                    + "FROM CalonSiswa cs "
                    + "WHERE cs.tahunMasuk = :tahunMasuk "
                    + "AND cs.tanggalPendaftaran >= :from "
                    + "AND cs.tanggalPendaftaran <= :today "
                    + "GROUP BY YEAR(cs.tanggalPendaftaran), "
                    + "MONTH(cs.tanggalPendaftaran), "
                    + "DAY(cs.tanggalPendaftaran)";
            List<Object[]> rows = (List<Object[]>) session.createQuery(hql)
                    .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk))
                    .setParameter("from", from)
                    .setParameter("today", shiftDays(today, 1))
                    .list();

            // Buat array 30 slot
            int[] counts = new int[WINDOW_DAYS];
            String[] labels = new String[WINDOW_DAYS];
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM");
            Calendar cal = Calendar.getInstance();
            for (int i = 0; i < WINDOW_DAYS; i++) {
                cal.setTime(shiftDays(today, -(WINDOW_DAYS - 1 - i)));
                labels[i] = sdf.format(cal.getTime());
                int yr = cal.get(Calendar.YEAR);
                int mo = cal.get(Calendar.MONTH) + 1;
                int dy = cal.get(Calendar.DAY_OF_MONTH);
                for (int j = 0; j < rows.size(); j++) {
                    Object[] r = rows.get(j);
                    int ry = r[0] == null ? 0 : ((Number) r[0]).intValue();
                    int rm = r[1] == null ? 0 : ((Number) r[1]).intValue();
                    int rd = r[2] == null ? 0 : ((Number) r[2]).intValue();
                    if (ry == yr && rm == mo && rd == dy) {
                        counts[i] = r[3] == null ? 0 : ((Number) r[3]).intValue();
                        break;
                    }
                }
            }
            for (int i = 0; i < WINDOW_DAYS; i++) {
                result.add(new HariData(labels[i], counts[i]));
            }
        } catch (Exception e) {
            logErr("DashboardHarianSiswa.queryTren tahunMasuk=" + tahunMasuk, e);
        }
        return result;
    }

    /** Perbandingan per hari-dalam-minggu: minggu ini [0] vs minggu lalu [1]. */
    @SuppressWarnings("unchecked")
    private int[][] queryWeekly(Session session, int tahunMasuk, Date today) {
        int[][] result = new int[2][7];
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(today);
            // cari Senin minggu ini
            int dow = cal.get(Calendar.DAY_OF_WEEK);
            // Java: SUNDAY=1, MONDAY=2, ..., SATURDAY=7
            int daysFromMon = (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY);
            Date monThis = shiftDays(today, -daysFromMon);
            Date monLast = shiftDays(monThis, -7);

            // Minggu ini: Senin s.d. Minggu
            Date endThis = shiftDays(monThis, 7);
            String hql = "SELECT YEAR(cs.tanggalPendaftaran), "
                    + "MONTH(cs.tanggalPendaftaran), "
                    + "DAY(cs.tanggalPendaftaran), COUNT(cs) "
                    + "FROM CalonSiswa cs "
                    + "WHERE cs.tahunMasuk = :tahunMasuk "
                    + "AND cs.tanggalPendaftaran >= :from "
                    + "AND cs.tanggalPendaftaran < :to "
                    + "GROUP BY YEAR(cs.tanggalPendaftaran), "
                    + "MONTH(cs.tanggalPendaftaran), "
                    + "DAY(cs.tanggalPendaftaran)";

            // Minggu ini
            List<Object[]> thisWeek = (List<Object[]>) session.createQuery(hql)
                    .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk))
                    .setParameter("from", monThis).setParameter("to", endThis).list();
            fillWeekBucket(result[0], thisWeek, monThis);

            // Minggu lalu
            List<Object[]> lastWeek = (List<Object[]>) session.createQuery(hql)
                    .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk))
                    .setParameter("from", monLast).setParameter("to", monThis).list();
            fillWeekBucket(result[1], lastWeek, monLast);
        } catch (Exception e) {
            logErr("DashboardHarianSiswa.queryWeekly tahunMasuk=" + tahunMasuk, e);
        }
        return result;
    }

    private void fillWeekBucket(int[] bucket, List<Object[]> rows, Date monDate) {
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < rows.size(); i++) {
            Object[] r  = rows.get(i);
            int yr = r[0] == null ? 0 : ((Number) r[0]).intValue();
            int mo = r[1] == null ? 0 : ((Number) r[1]).intValue();
            int dy = r[2] == null ? 0 : ((Number) r[2]).intValue();
            int cnt = r[3] == null ? 0 : ((Number) r[3]).intValue();
            // hitung offset dari Senin
            for (int d = 0; d < 7; d++) {
                cal.setTime(shiftDays(monDate, d));
                if (cal.get(Calendar.YEAR) == yr
                        && (cal.get(Calendar.MONTH) + 1) == mo
                        && cal.get(Calendar.DAY_OF_MONTH) == dy) {
                    bucket[d] = cnt;
                    break;
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Utility
    // ═══════════════════════════════════════════════════════════════

    private static Date shiftDays(Date base, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(base);
        cal.add(Calendar.DAY_OF_MONTH, days);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
