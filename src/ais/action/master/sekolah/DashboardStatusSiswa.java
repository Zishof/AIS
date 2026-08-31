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
 * <h1>DashboardStatusSiswa — Analisis Status Seleksi Calon Siswa</h1>
 *
 * <p>Menjawab pertanyaan: <em>"Bagaimana distribusi akhir status seleksi setiap
 * calon siswa?"</em> Tidak semua pendaftar melalui jalur yang sama. Sebagian
 * diterima, sebagian ditolak, ada yang mengundurkan diri, dan ada yang masih
 * dalam proses.</p>
 *
 * <p>Empat status yang dipantau (bisa overlap — satu calon bisa terverifikasi
 * sekaligus diterima):</p>
 * <ul>
 *   <li><b>Diterima</b> ({@code telahDiterima = true}) — resmi masuk sebagai siswa.</li>
 *   <li><b>Ditolak</b> ({@code ditolak = true}) — tidak memenuhi syarat seleksi.</li>
 *   <li><b>Mengundurkan Diri</b> ({@code mengundurkanDiri = true}) — calon menarik
 *       pendaftarannya setelah mendaftar.</li>
 *   <li><b>Dapat NIS</b> ({@code siswa IS NOT NULL}) — sudah resmi terdaftar
 *       sebagai siswa dengan Nomor Induk Siswa.</li>
 * </ul>
 *
 * <p>Calon yang tidak memiliki satu pun flag di atas dikategorikan sebagai
 * <em>Belum Ditentukan</em> — masih menunggu hasil seleksi. Jumlah ini penting
 * dipantau mendekati akhir periode PSB agar tidak ada calon yang "terlupakan".</p>
 *
 * <p>Dasbor juga menampilkan distribusi status per jurusan untuk melihat apakah
 * ada jurusan dengan tingkat penolakan atau pengunduran diri yang tidak wajar.</p>
 *
 * @see DashboardSiswaBase
 */
public class DashboardStatusSiswa extends DashboardSiswaBase {

    // ═══════════════════════════════════════════════════════════════
    // Inner data class
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tipe implementasi bersarang {@link JurusanStatusData} milik {@link DashboardStatusSiswa}. Kelas ini memberi
     * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DashboardStatusSiswa}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
     * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Object id}, {@code String nama},
     * {@code int diterima}, {@code int ditolak}, {@code int mundur}. Aturan bisnis bersama tetap berada pada kelas
     * induk atau service yang dipanggilnya.</p>
     *
     * @see DashboardStatusSiswa
     */
    private static final class JurusanStatusData {
        Object id;
        String nama;
        int    diterima;
        int    ditolak;
        int    mundur;

        JurusanStatusData(Object id, String nama) {
            this.id   = id;
            this.nama = nama;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Konstanta
    // ═══════════════════════════════════════════════════════════════

    private static final int MAX_JURUSAN = 8;

    // ═══════════════════════════════════════════════════════════════
    // Konstruktor
    // ═══════════════════════════════════════════════════════════════

    public DashboardStatusSiswa(PerguruanTinggi pt) {
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
        int ditolak      = queryCount(session, tahunMasuk, "AND cs.ditolak = true ");
        int mundur       = queryCount(session, tahunMasuk, "AND cs.mengundurkanDiri = true ");
        int dapatNis     = queryCount(session, tahunMasuk, "AND cs.siswa IS NOT NULL ");
        int belum        = total - diterima - ditolak - mundur;
        if (belum < 0) { belum = 0; }

        List<JurusanStatusData> jurusanList = queryPerJurusanStatus(session, tahunMasuk);

        StringBuilder sb = new StringBuilder(6144);

        // ── Header
        sb.append("<div style=\"margin-bottom:14px;\">")
          .append("<div style=\"font-size:18px;font-weight:700;color:#1e3a5f;margin-bottom:3px;\">")
          .append("Status Seleksi Calon Siswa &#8212; Tahun Masuk ").append(tahunMasuk)
          .append("</div>")
          .append("<div style=\"font-size:12px;color:#9ca3af;\">")
          .append("Distribusi akhir status setiap calon: diterima, ditolak, "
                + "mengundurkan diri, atau masih dalam proses. "
                + "Pantau agar tidak ada calon yang terlupakan menjelang akhir PSB.")
          .append("</div></div>");

        // ── KPI
        double pctDiterima = total > 0 ? (diterima * 100.0 / total) : 0;
        sb.append(HtmlChartHelper.kpiCards(
                new String[] { "Total Peminat", "Diterima", "Ditolak",
                        "Mundur", "Belum Ditentukan" },
                new String[] { fmtAngka(total),
                        fmtAngka(diterima) + " (" + fmt1(pctDiterima) + "%)",
                        fmtAngka(ditolak), fmtAngka(mundur), fmtAngka(belum) },
                new String[] { "", "dari peminat", "tidak lolos seleksi",
                        "menarik diri", "perlu tindak lanjut" },
                new String[] { "", "", "", "", "" },
                new boolean[] { true, true, ditolak == 0, mundur == 0, belum == 0 },
                new String[] { "#1877f2", "#059669", "#dc2626", "#f59e0b", "#9ca3af" }));

        // ── Donut komposisi status
        int prosesVerif = terverifikasi - diterima; // terverifikasi tapi belum diterima
        if (prosesVerif < 0) { prosesVerif = 0; }
        String[] donutLabels = { "Diterima", "Ditolak", "Mundur", "Dapat NIS", "Belum" };
        double[] donutVals   = { diterima, ditolak, mundur, dapatNis, belum };
        String[] donutColors = { "#10b981", "#ef4444", "#f59e0b", "#3b82f6", "#9ca3af" };
        String donutHtml = HtmlChartHelper.donut(
                "Komposisi Status Seleksi",
                "Proporsi status akhir seluruh pendaftar. "
                + "Idealnya porsi 'Belum Ditentukan' mendekati nol di akhir periode PSB.",
                donutLabels, donutVals, donutColors, "");

        // ── Corong jalur sukses
        String corongHtml = HtmlChartHelper.barHorizontal(
                "Jalur Sukses: Peminat → Dapat NIS",
                "Tahapan keberhasilan: terverifikasi berkas, diterima secara resmi, "
                + "lalu mendapatkan Nomor Induk Siswa. "
                + "Selisih antar tahap adalah potensi yang hilang.",
                new String[] { "Total Peminat", "Terverifikasi", "Diterima", "Dapat NIS" },
                new double[] { total, terverifikasi, diterima, dapatNis },
                "#059669");

        sb.append(grid2col(donutHtml, corongHtml));

        // ── Per Jurusan: Diterima vs Ditolak vs Mundur
        if (!jurusanList.isEmpty()) {
            int n        = Math.min(jurusanList.size(), MAX_JURUSAN);
            String[]   cats  = new String[n];
            double[][] vals  = new double[n][3];
            for (int i = 0; i < n; i++) {
                JurusanStatusData d = jurusanList.get(i);
                cats[i]    = abbrev(d.nama, 16);
                vals[i][0] = d.diterima;
                vals[i][1] = d.ditolak;
                vals[i][2] = d.mundur;
            }
            sb.append(fullWidth(HtmlChartHelper.barVerticalGrouped(
                    "Status Seleksi Per Jurusan",
                    "Tiga batang per jurusan: hijau = diterima, merah = ditolak, "
                    + "kuning = mengundurkan diri. "
                    + "Jurusan dengan batang merah tinggi perlu evaluasi proses seleksinya.",
                    cats,
                    new String[] { "Diterima", "Ditolak", "Mundur" },
                    vals,
                    new String[] { "#10b981", "#ef4444", "#f59e0b" })));
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
            logErr("DashboardStatusSiswa.queryCount tahunMasuk=" + tahunMasuk
                    + " where=" + extraWhere, e);
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private List<JurusanStatusData> queryPerJurusanStatus(Session session, int tahunMasuk) {
        Map<Object, JurusanStatusData> map = new LinkedHashMap<Object, JurusanStatusData>();

        // Seed map dengan semua jurusan yang punya pendaftar
        try {
            String hql = "SELECT ps.id, ps.nama FROM CalonSiswa cs "
                    + "JOIN cs.penjurusanSekolah ps "
                    + "WHERE cs.tahunMasuk = :tahunMasuk "
                    + "GROUP BY ps.id, ps.nama ORDER BY COUNT(cs) DESC";
            List<Object[]> rows = (List<Object[]>) session.createQuery(hql)
                    .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk))
                    .setMaxResults(MAX_JURUSAN).list();
            for (int i = 0; i < rows.size(); i++) {
                Object[] r = rows.get(i);
                Object id  = r[0];
                String nm  = r[1] == null ? "-" : r[1].toString();
                map.put(id, new JurusanStatusData(id, nm));
            }
        } catch (Exception e) {
            logErr("DashboardStatusSiswa.queryPerJurusanStatus seed tahunMasuk=" + tahunMasuk, e);
        }
        if (map.isEmpty()) { return new ArrayList<JurusanStatusData>(); }

        // Diterima per jurusan
        fillJurusanCount(session, tahunMasuk, map, "AND cs.telahDiterima = true ", "diterima");
        // Ditolak per jurusan
        fillJurusanCount(session, tahunMasuk, map, "AND cs.ditolak = true ", "ditolak");
        // Mundur per jurusan
        fillJurusanCount(session, tahunMasuk, map, "AND cs.mengundurkanDiri = true ", "mundur");

        return new ArrayList<JurusanStatusData>(map.values());
    }

    @SuppressWarnings("unchecked")
    private void fillJurusanCount(Session session, int tahunMasuk,
            Map<Object, JurusanStatusData> map, String extraWhere, String target) {
        try {
            String hql = "SELECT ps.id, COUNT(cs) FROM CalonSiswa cs "
                    + "JOIN cs.penjurusanSekolah ps "
                    + "WHERE cs.tahunMasuk = :tahunMasuk " + extraWhere
                    + "GROUP BY ps.id";
            List<Object[]> rows = (List<Object[]>) session.createQuery(hql)
                    .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk)).list();
            for (int i = 0; i < rows.size(); i++) {
                Object[] r  = rows.get(i);
                Object   id = r[0];
                int     cnt = r[1] == null ? 0 : ((Number) r[1]).intValue();
                JurusanStatusData d = map.get(id);
                if (d == null) { continue; }
                if ("diterima".equals(target)) { d.diterima = cnt; }
                else if ("ditolak".equals(target)) { d.ditolak = cnt; }
                else if ("mundur".equals(target)) { d.mundur = cnt; }
            }
        } catch (Exception e) {
            logErr("DashboardStatusSiswa.fillJurusanCount target=" + target
                    + " tahunMasuk=" + tahunMasuk, e);
        }
    }
}
