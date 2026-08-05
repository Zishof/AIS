package ais.action.master.sekolah;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;

import ais.database.model.PerguruanTinggi;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyHtml;

/**
 * <h1>DashboardStatistikSiswa — Ringkasan Utama Calon Siswa</h1>
 *
 * <p>Menjawab pertanyaan: <em>"Bagaimana gambaran keseluruhan penerimaan siswa baru
 * tahun ini?"</em> Dasbor ini adalah halaman pertama yang dibuka oleh pengelola PSB
 * untuk mendapatkan gambaran cepat tanpa harus membuka laporan terpisah.</p>
 *
 * <p>Lima dimensi yang disajikan:</p>
 * <ul>
 *   <li><b>KPI Utama</b> — Total peminat, terverifikasi, diterima, dapat NIS,
 *       dan rasio konversi NIS/peminat.</li>
 *   <li><b>Corong Penerimaan</b> — Alur dari peminat → terverifikasi → diterima
 *       → dapat NIS. Selisih antar tahap = calon yang gugur di tiap tahap.</li>
 *   <li><b>Distribusi Gender</b> — Donut pie proporsi laki-laki vs perempuan.
 *       Ketidakseimbangan gender bisa menjadi bahan evaluasi strategi promosi.</li>
 *   <li><b>Peminat Per Gelombang</b> — Bar horizontal menunjukkan distribusi
 *       pendaftar antar gelombang PSB dalam satu tahun masuk.</li>
 *   <li><b>Per Jurusan</b> — Grouped bar peminat vs diterima per penjurusan sekolah.
 *       Jurusan dengan daya saing tinggi (banyak peminat, sedikit diterima) tampil jelas.</li>
 * </ul>
 *
 * <p>Filter yang tersedia: Tahun Masuk (Integer). Semua query menggunakan
 * {@code cs.tahunMasuk = :tahunMasuk} pada entity {@code CalonSiswa}.</p>
 *
 * @see DashboardSiswaBase
 */
public class DashboardStatistikSiswa extends DashboardSiswaBase {

    // ═══════════════════════════════════════════════════════════════
    // Inner data classes
    // ═══════════════════════════════════════════════════════════════

    private static final class JurusanData {
        Object id;
        String nama;
        int    total;
        int    diterima;

        JurusanData(Object id, String nama, int total) {
            this.id    = id;
            this.nama  = nama;
            this.total = total;
        }
    }

    private static final class GelombangData {
        String nama;
        int    total;

        GelombangData(String nama, int total) {
            this.nama  = nama;
            this.total = total;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Konstanta
    // ═══════════════════════════════════════════════════════════════

    private static final int MAX_JURUSAN   = 8;
    private static final int MAX_GELOMBANG = 10;

    // ═══════════════════════════════════════════════════════════════
    // Konstruktor
    // ═══════════════════════════════════════════════════════════════

    public DashboardStatistikSiswa(PerguruanTinggi pt) {
        super(pt);
    }

    // ═══════════════════════════════════════════════════════════════
    // doRefreshSiswa
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void doRefreshSiswa(Session session, int tahunMasuk) {
        int total        = queryCount(session, tahunMasuk, "");
        int terverifikasi = queryCount(session, tahunMasuk, "AND cs.terverifikasi = true ");
        int diterima     = queryCount(session, tahunMasuk, "AND cs.telahDiterima = true ");
        int dapatNis     = queryCount(session, tahunMasuk, "AND cs.siswa IS NOT NULL ");

        int[]             gender       = queryGender(session, tahunMasuk);
        List<JurusanData> jurusanList  = queryPerJurusan(session, tahunMasuk);
        List<GelombangData> gelombList = queryPerGelombang(session, tahunMasuk);

        StringBuilder sb = new StringBuilder(8192);

        // ── Header
        sb.append("<div style=\"margin-bottom:14px;\">")
          .append("<div style=\"font-size:18px;font-weight:700;color:#1e3a5f;margin-bottom:3px;\">")
          .append("Statistik Utama Calon Siswa &#8212; Tahun Masuk ").append(tahunMasuk)
          .append("</div>")
          .append("<div style=\"font-size:12px;color:#9ca3af;\">")
          .append("Ringkasan penerimaan: dari total peminat hingga yang berhasil mendapatkan NIS "
                + "sebagai siswa baru resmi.")
          .append("</div></div>");

        // ── KPI
        double konversiNis = total > 0 ? (dapatNis * 100.0 / total) : 0;
        sb.append(HtmlChartHelper.kpiCards(
                new String[] { "Total Peminat", "Terverifikasi", "Diterima",
                        "Dapat NIS", "Konversi NIS" },
                new String[] { fmtAngka(total), fmtAngka(terverifikasi),
                        fmtAngka(diterima), fmtAngka(dapatNis),
                        fmt1(konversiNis) + "%" },
                new String[] { "", "dari peminat", "dari peminat",
                        "resmi terdaftar", "total → NIS" },
                new String[] { "", "", "", "", "" },
                new boolean[] { true, true, true, true, konversiNis >= 50 },
                new String[] { "#1877f2", "#059669", "#7c3aed", "#e4496b", "#f59e0b" }));

        // ── Corong penerimaan
        sb.append(fullWidth(HtmlChartHelper.barHorizontal(
                "Corong Penerimaan — Dari Peminat Sampai Dapat NIS",
                "Empat tahap seleksi yang harus dilalui calon siswa. "
                + "Selisih antar batang menunjukkan jumlah calon yang tidak melanjutkan ke tahap berikutnya. "
                + "Corong yang merata menandakan proses seleksi yang baik.",
                new String[] { "Total Peminat", "Terverifikasi", "Diterima", "Dapat NIS" },
                new double[] { total, terverifikasi, diterima, dapatNis },
                "#7c3aed")));

        // ── Gender + Per Gelombang berdampingan
        String genderHtml    = buildGenderHtml(gender);
        String gelombangHtml = buildGelombangHtml(gelombList);
        sb.append(grid2col(genderHtml, gelombangHtml));

        // ── Per Jurusan (grouped bar)
        if (!jurusanList.isEmpty()) {
            int n        = Math.min(jurusanList.size(), MAX_JURUSAN);
            String[]   cats  = new String[n];
            double[][] vals  = new double[n][2];
            for (int i = 0; i < n; i++) {
                JurusanData d = jurusanList.get(i);
                cats[i]    = abbrev(d.nama, 18);
                vals[i][0] = d.total;
                vals[i][1] = d.diterima;
            }
            sb.append(fullWidth(HtmlChartHelper.barVerticalGrouped(
                    "Peminat vs Diterima Per Jurusan",
                    "Setiap jurusan ditampilkan dua batang: biru = total peminat, hijau = yang diterima. "
                    + "Jurusan dengan selisih kecil berarti hampir semua peminat berhasil diterima.",
                    cats,
                    new String[] { "Peminat", "Diterima" },
                    vals,
                    new String[] { "#3b82f6", "#10b981" })));
        }

        new MyHtml(sb.toString()).setParent(contentHolder);
    }

    // ═══════════════════════════════════════════════════════════════
    // Sub-builder HTML
    // ═══════════════════════════════════════════════════════════════

    private String buildGenderHtml(int[] gender) {
        if (gender[0] + gender[1] == 0) {
            return emptyCard("Distribusi Gender", "Data gender belum tersedia.");
        }
        return HtmlChartHelper.donut(
                "Distribusi Gender",
                "Proporsi jenis kelamin calon siswa tahun masuk ini. "
                + "Keseimbangan gender mencerminkan inklusivitas program penerimaan sekolah.",
                new String[] { "Laki-laki", "Perempuan" },
                new double[] { gender[0], gender[1] },
                new String[] { "#3b82f6", "#e4496b" }, "");
    }

    private String buildGelombangHtml(List<GelombangData> list) {
        if (list.isEmpty()) {
            return emptyCard("Per Gelombang", "Tidak ada data gelombang untuk tahun ini.");
        }
        int n = list.size();
        String[] lbl = new String[n];
        double[] val = new double[n];
        for (int i = 0; i < n; i++) {
            lbl[i] = abbrev(list.get(i).nama, 15);
            val[i] = list.get(i).total;
        }
        return HtmlChartHelper.barHorizontal(
                "Peminat Per Gelombang PSB",
                "Distribusi pendaftar berdasarkan gelombang penerimaan. "
                + "Gelombang pertama biasanya menyerap paling banyak peminat.",
                lbl, val, "#6366f1");
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
                    .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk))
                    .uniqueResult();
            return n == null ? 0 : n.intValue();
        } catch (Exception e) {
            logErr("DashboardStatistikSiswa.queryCount tahunMasuk=" + tahunMasuk
                    + " where=" + extraWhere, e);
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private int[] queryGender(Session session, int tahunMasuk) {
        int laki = 0, perempuan = 0;
        try {
            String hql = "SELECT cs.jenisKelamin, COUNT(cs) FROM CalonSiswa cs "
                    + "WHERE cs.tahunMasuk = :tahunMasuk AND cs.jenisKelamin IS NOT NULL "
                    + "GROUP BY cs.jenisKelamin";
            List<Object[]> rows = (List<Object[]>) session.createQuery(hql)
                    .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk)).list();
            for (int i = 0; i < rows.size(); i++) {
                Object[] r  = rows.get(i);
                String   jk = r[0] == null ? "" : r[0].toString().trim().toLowerCase();
                int     cnt = r[1] == null ? 0 : ((Number) r[1]).intValue();
                boolean isLaki = jk.startsWith("l") || "pria".equals(jk) || "male".equals(jk);
                if (isLaki) { laki += cnt; } else { perempuan += cnt; }
            }
        } catch (Exception e) {
            logErr("DashboardStatistikSiswa.queryGender tahunMasuk=" + tahunMasuk, e);
        }
        return new int[] { laki, perempuan };
    }

    @SuppressWarnings("unchecked")
    private List<JurusanData> queryPerJurusan(Session session, int tahunMasuk) {
        Map<Object, JurusanData> map = new LinkedHashMap<Object, JurusanData>();

        // Query 1: total peminat per jurusan
        try {
            String hql = "SELECT ps.id, ps.nama, COUNT(cs) FROM CalonSiswa cs "
                    + "JOIN cs.penjurusanSekolah ps "
                    + "WHERE cs.tahunMasuk = :tahunMasuk "
                    + "GROUP BY ps.id, ps.nama ORDER BY COUNT(cs) DESC";
            List<Object[]> rows = (List<Object[]>) session.createQuery(hql)
                    .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk))
                    .setMaxResults(MAX_JURUSAN).list();
            for (int i = 0; i < rows.size(); i++) {
                Object[] r  = rows.get(i);
                Object   id = r[0];
                String   nm = r[1] == null ? "-" : r[1].toString();
                int     cnt = r[2] == null ? 0 : ((Number) r[2]).intValue();
                map.put(id, new JurusanData(id, nm, cnt));
            }
        } catch (Exception e) {
            logErr("DashboardStatistikSiswa.queryPerJurusan peminat tahunMasuk=" + tahunMasuk, e);
        }

        // Query 2: diterima per jurusan
        try {
            String hql = "SELECT ps.id, COUNT(cs) FROM CalonSiswa cs "
                    + "JOIN cs.penjurusanSekolah ps "
                    + "WHERE cs.tahunMasuk = :tahunMasuk AND cs.telahDiterima = true "
                    + "GROUP BY ps.id";
            List<Object[]> rows = (List<Object[]>) session.createQuery(hql)
                    .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk)).list();
            for (int i = 0; i < rows.size(); i++) {
                Object[] r  = rows.get(i);
                Object   id = r[0];
                int     cnt = r[1] == null ? 0 : ((Number) r[1]).intValue();
                if (map.containsKey(id)) { map.get(id).diterima = cnt; }
            }
        } catch (Exception e) {
            logErr("DashboardStatistikSiswa.queryPerJurusan diterima tahunMasuk=" + tahunMasuk, e);
        }

        return new ArrayList<JurusanData>(map.values());
    }

    @SuppressWarnings("unchecked")
    private List<GelombangData> queryPerGelombang(Session session, int tahunMasuk) {
        List<GelombangData> result = new ArrayList<GelombangData>();
        try {
            String hql = "SELECT g.nama, COUNT(cs) FROM CalonSiswa cs "
                    + "JOIN cs.gelombangPendaftaranPsb g "
                    + "WHERE cs.tahunMasuk = :tahunMasuk "
                    + "GROUP BY g.id, g.nama ORDER BY g.id";
            List<Object[]> rows = (List<Object[]>) session.createQuery(hql)
                    .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk))
                    .setMaxResults(MAX_GELOMBANG).list();
            for (int i = 0; i < rows.size(); i++) {
                Object[] r  = rows.get(i);
                String   nm = r[0] == null ? "-" : r[0].toString();
                int     cnt = r[1] == null ? 0 : ((Number) r[1]).intValue();
                result.add(new GelombangData(nm, cnt));
            }
        } catch (Exception e) {
            logErr("DashboardStatistikSiswa.queryPerGelombang tahunMasuk=" + tahunMasuk, e);
        }
        return result;
    }
}
