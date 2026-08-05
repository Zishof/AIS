package ais.action.master.sekolah;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.database.model.PerguruanTinggi;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyHtml;

/**
 * <h1>DashboardAsalSekolahSiswa — Analisis Asal Sekolah &amp; Wilayah Pendaftar</h1>
 *
 * <p>Menjawab pertanyaan: <em>"Calon siswa kita datang dari sekolah dan wilayah mana
 * saja?"</em> Informasi ini krusial untuk strategi promosi — sekolah yang selama ini
 * paling banyak mengirim siswa adalah mitra alami yang perlu diprioritaskan, sementara
 * wilayah propinsi dengan sedikit pendaftar menunjukkan area yang potensial untuk
 * ekspansi jangkauan.</p>
 *
 * <p>Empat dimensi yang disajikan:</p>
 * <ul>
 *   <li><b>KPI Utama</b> — Total peminat, jumlah sekolah asal unik, top sekolah,
 *       dan top propinsi berdasarkan jumlah pendaftar.</li>
 *   <li><b>Top Sekolah Asal</b> — Horizontal bar 10 sekolah teratas berdasarkan
 *       field teks bebas {@code sekolahAsal}. Membantu identifikasi partner sekolah
 *       utama untuk kegiatan sosialisasi.</li>
 *   <li><b>Sebaran Propinsi Sekolah Asal</b> — Distribusi per propinsi dari
 *       field {@code propinsiSekolahAsal}. Berbeda dengan propinsi tempat tinggal
 *       calon; ini adalah lokasi sekolah asal.</li>
 *   <li><b>Diterima vs Peminat Per Propinsi</b> — Grouped bar memperlihatkan apakah
 *       ada disparitas diterimanya calon dari propinsi berbeda, berguna untuk
 *       evaluasi keadilan seleksi lintas wilayah.</li>
 * </ul>
 *
 * @see DashboardSiswaBase
 */
public class DashboardAsalSekolahSiswa extends DashboardSiswaBase {

    // ═══════════════════════════════════════════════════════════════
    // Inner data class
    // ═══════════════════════════════════════════════════════════════

    private static final class NamaData {
        String nama;
        int    jumlah;

        NamaData(String nama, int jumlah) {
            this.nama   = nama;
            this.jumlah = jumlah;
        }
    }

    private static final class PropinsiData {
        String nama;
        int    peminat;
        int    diterima;

        PropinsiData(String nama, int peminat) {
            this.nama    = nama;
            this.peminat = peminat;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Konstanta
    // ═══════════════════════════════════════════════════════════════

    private static final int MAX_SEKOLAH  = 10;
    private static final int MAX_PROPINSI = 8;

    // ═══════════════════════════════════════════════════════════════
    // Konstruktor
    // ═══════════════════════════════════════════════════════════════

    public DashboardAsalSekolahSiswa(PerguruanTinggi pt) {
        super(pt);
    }

    // ═══════════════════════════════════════════════════════════════
    // doRefreshSiswa
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void doRefreshSiswa(Session session, int tahunMasuk) {
        int total        = queryCount(session, tahunMasuk, "");
        int sekolahUnik  = querySekolahUnik(session, tahunMasuk);

        List<NamaData>     topSekolah  = queryTopSekolah(session, tahunMasuk);
        List<PropinsiData> propinsiList = queryPerPropinsi(session, tahunMasuk);

        String topSekolahNama = topSekolah.isEmpty() ? "-" : topSekolah.get(0).nama;
        String topPropinsi    = propinsiList.isEmpty() ? "-" : propinsiList.get(0).nama;

        StringBuilder sb = new StringBuilder(6144);

        // ── Header
        sb.append("<div style=\"margin-bottom:14px;\">")
          .append("<div style=\"font-size:18px;font-weight:700;color:#1e3a5f;margin-bottom:3px;\">")
          .append("Asal Sekolah Calon Siswa &#8212; Tahun Masuk ").append(tahunMasuk)
          .append("</div>")
          .append("<div style=\"font-size:12px;color:#9ca3af;\">")
          .append("Dari sekolah dan wilayah mana saja calon siswa kita berasal. "
                + "Gunakan untuk menentukan prioritas kunjungan promosi dan kemitraan sekolah.")
          .append("</div></div>");

        // ── KPI
        sb.append(HtmlChartHelper.kpiCards(
                new String[] { "Total Peminat", "Sekolah Unik", "Top Sekolah", "Top Propinsi" },
                new String[] { fmtAngka(total), fmtAngka(sekolahUnik),
                        abbrev(topSekolahNama, 20), abbrev(topPropinsi, 20) },
                new String[] { "", "sekolah berbeda", "terbanyak kirim siswa",
                        "terbanyak asal siswa" },
                new String[] { "", "", "", "" },
                new boolean[] { true, true, true, true },
                new String[] { "#1877f2", "#059669", "#f59e0b", "#8b5cf6" }));

        if (total == 0) {
            sb.append(fullWidth(emptyCard("Asal Sekolah",
                    "Belum ada data calon siswa untuk tahun masuk " + tahunMasuk + ".")));
            new MyHtml(sb.toString()).setParent(contentHolder);
            return;
        }

        // ── Top 10 Sekolah Asal (bar horizontal)
        if (!topSekolah.isEmpty()) {
            int      n   = topSekolah.size();
            String[] lbl = new String[n];
            double[] val = new double[n];
            for (int i = 0; i < n; i++) {
                lbl[i] = abbrev(topSekolah.get(i).nama, 30);
                val[i] = topSekolah.get(i).jumlah;
            }
            sb.append(fullWidth(HtmlChartHelper.barHorizontal(
                    "Top " + n + " Sekolah Asal Terbanyak",
                    "Daftar sekolah yang paling banyak mengirimkan calon siswa ke sekolah kita. "
                    + "Sekolah di urutan atas adalah mitra alami untuk program roadshow dan beasiswa.",
                    lbl, val, "#3b82f6")));
        } else {
            sb.append(fullWidth(emptyCard("Top Sekolah",
                    "Data sekolah asal (field teks) belum terisi di data calon siswa.")));
        }

        // ── Per Propinsi
        if (!propinsiList.isEmpty()) {
            int      n  = Math.min(propinsiList.size(), MAX_PROPINSI);
            String[] lbl = new String[n];
            double[][] vals = new double[n][2];
            boolean hasMultiBar = false;
            for (int i = 0; i < n; i++) {
                PropinsiData d = propinsiList.get(i);
                lbl[i]    = abbrev(d.nama, 16);
                vals[i][0] = d.peminat;
                vals[i][1] = d.diterima;
                if (d.diterima > 0) { hasMultiBar = true; }
            }
            if (hasMultiBar) {
                sb.append(fullWidth(HtmlChartHelper.barVerticalGrouped(
                        "Peminat vs Diterima Per Propinsi Sekolah Asal",
                        "Dua batang per propinsi: biru = total peminat, hijau = yang diterima. "
                        + "Propinsi dengan rasio diterima/peminat rendah mungkin perlu "
                        + "pendampingan lebih dalam proses seleksi.",
                        lbl,
                        new String[] { "Peminat", "Diterima" },
                        vals,
                        new String[] { "#3b82f6", "#10b981" })));
            } else {
                // Tampilkan bar tunggal peminat saja
                double[] singleVals = new double[n];
                for (int i = 0; i < n; i++) { singleVals[i] = propinsiList.get(i).peminat; }
                sb.append(fullWidth(HtmlChartHelper.barHorizontal(
                        "Sebaran Per Propinsi Sekolah Asal",
                        "Distribusi peminat berdasarkan propinsi lokasi sekolah asal. "
                        + "Propinsi dengan banyak peminat adalah area prioritas promosi.",
                        lbl, singleVals, "#8b5cf6")));
            }
        } else {
            sb.append(fullWidth(emptyCard("Per Propinsi",
                    "Data propinsi sekolah asal belum terisi di data calon siswa.")));
        }

        new MyHtml(sb.toString()).setParent(contentHolder);
    }

    // ═══════════════════════════════════════════════════════════════
    // Query methods
    // ═══════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private int queryCount(Session session, int tahunMasuk, String extraWhere) {
        try {
            String hql = "SELECT COUNT(cs) FROM CalonSiswa cs "
                    + "WHERE cs.tahunMasuk = :tahunMasuk " + extraWhere;
            Number n = (Number) session.createQuery(hql)
                    .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk)).uniqueResult();
            return n == null ? 0 : n.intValue();
        } catch (Exception e) {
            logErr("DashboardAsalSekolahSiswa.queryCount tahunMasuk=" + tahunMasuk, e);
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private int querySekolahUnik(Session session, int tahunMasuk) {
        try {
            String hql = "SELECT COUNT(DISTINCT cs.sekolahAsal) FROM CalonSiswa cs "
                    + "WHERE cs.tahunMasuk = :tahunMasuk "
                    + "AND cs.sekolahAsal IS NOT NULL AND cs.sekolahAsal <> ''";
            Number n = (Number) session.createQuery(hql)
                    .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk)).uniqueResult();
            return n == null ? 0 : n.intValue();
        } catch (Exception e) {
            logErr("DashboardAsalSekolahSiswa.querySekolahUnik tahunMasuk=" + tahunMasuk, e);
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private List<NamaData> queryTopSekolah(Session session, int tahunMasuk) {
        List<NamaData> result = new ArrayList<NamaData>();
        try {
            String hql = "SELECT cs.sekolahAsal, COUNT(cs) FROM CalonSiswa cs "
                    + "WHERE cs.tahunMasuk = :tahunMasuk "
                    + "AND cs.sekolahAsal IS NOT NULL AND cs.sekolahAsal <> '' "
                    + "GROUP BY cs.sekolahAsal ORDER BY COUNT(cs) DESC";
            List<Object[]> rows = (List<Object[]>) session.createQuery(hql)
                    .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk))
                    .setMaxResults(MAX_SEKOLAH).list();
            for (int i = 0; i < rows.size(); i++) {
                Object[] r  = rows.get(i);
                String   nm = r[0] == null ? "-" : r[0].toString().trim();
                int     cnt = r[1] == null ? 0 : ((Number) r[1]).intValue();
                if (!nm.isEmpty()) { result.add(new NamaData(nm, cnt)); }
            }
        } catch (Exception e) {
            logErr("DashboardAsalSekolahSiswa.queryTopSekolah tahunMasuk=" + tahunMasuk, e);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<PropinsiData> queryPerPropinsi(Session session, int tahunMasuk) {
        List<PropinsiData> result = new ArrayList<PropinsiData>();
        try {
            // Query 1: total peminat per propinsi sekolah asal
            String hql = "SELECT pr.nama, COUNT(cs) FROM CalonSiswa cs "
                    + "JOIN cs.propinsiSekolahAsal pr "
                    + "WHERE cs.tahunMasuk = :tahunMasuk "
                    + "GROUP BY pr.id, pr.nama ORDER BY COUNT(cs) DESC";
            List<Object[]> rows = (List<Object[]>) session.createQuery(hql)
                    .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk))
                    .setMaxResults(MAX_PROPINSI).list();
            for (int i = 0; i < rows.size(); i++) {
                Object[] r  = rows.get(i);
                String   nm = r[0] == null ? "-" : r[0].toString();
                int     cnt = r[1] == null ? 0 : ((Number) r[1]).intValue();
                result.add(new PropinsiData(nm, cnt));
            }
            // Query 2: diterima per propinsi
            if (!result.isEmpty()) {
                String hql2 = "SELECT pr.nama, COUNT(cs) FROM CalonSiswa cs "
                        + "JOIN cs.propinsiSekolahAsal pr "
                        + "WHERE cs.tahunMasuk = :tahunMasuk AND cs.telahDiterima = true "
                        + "GROUP BY pr.id, pr.nama";
                List<Object[]> rows2 = (List<Object[]>) session.createQuery(hql2)
                        .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk)).list();
                for (int i = 0; i < rows2.size(); i++) {
                    Object[] r  = rows2.get(i);
                    String   nm = r[0] == null ? "" : r[0].toString();
                    int     cnt = r[1] == null ? 0 : ((Number) r[1]).intValue();
                    for (int j = 0; j < result.size(); j++) {
                        if (nm.equals(result.get(j).nama)) {
                            result.get(j).diterima = cnt; break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logErr("DashboardAsalSekolahSiswa.queryPerPropinsi tahunMasuk=" + tahunMasuk, e);
        }
        return result;
    }
}
