package ais.action.master.sekolah;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.database.model.PerguruanTinggi;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHtml;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.UIUtil;

/**
 * <h1>DashboardRegistrasiSiswa — Status Pembayaran &amp; Daftar Ulang Calon Siswa</h1>
 *
 * <p>Menjawab pertanyaan: <em>"Dari calon yang sudah diterima, berapa yang sudah
 * menyelesaikan administrasi pendaftaran dan daftar ulang?"</em> Ini adalah dasbor
 * operasional kritis yang dimonitor tim keuangan dan PSB setiap hari.</p>
 *
 * <p>Dua tahap pembayaran yang dipantau:</p>
 * <ol>
 *   <li><b>Pembayaran Pendaftaran</b> — ditandai dengan field
 *       {@code riwayatPembayaranPendaftaran} yang tidak kosong. Menunjukkan komitmen
 *       awal calon untuk mengikuti proses seleksi secara resmi.</li>
 *   <li><b>Pembayaran Daftar Ulang</b> — ditandai dengan field
 *       {@code riwayatPembayaranDaftarUlang} yang tidak kosong. Merupakan
 *       konfirmasi akhir bahwa calon benar-benar akan menjadi siswa.</li>
 * </ol>
 *
 * <p>Calon yang sudah diterima ({@code telahDiterima = true}) tapi belum menyelesaikan
 * daftar ulang merupakan risiko <em>no-show</em> yang harus segera ditindaklanjuti
 * sebelum kursi dialihkan ke gelombang berikutnya.</p>
 *
 * <p>Dasbor menampilkan tabel rekap per jurusan yang bisa diunduh sebagai Excel
 * untuk kebutuhan laporan internal.</p>
 *
 * @see DashboardSiswaBase
 */
public class DashboardRegistrasiSiswa extends DashboardSiswaBase {

    // ═══════════════════════════════════════════════════════════════
    // Inner data class
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tipe implementasi bersarang {@link JurusanRegData} milik {@link DashboardRegistrasiSiswa}. Kelas ini memberi
     * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * DashboardRegistrasiSiswa}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan
     * dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Object id}, {@code String nama},
     * {@code int diterima}, {@code int bayarPendaftaran}, {@code int bayarDaftarUlang}. Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see DashboardRegistrasiSiswa
     */
    private static final class JurusanRegData {
        Object id;
        String nama;
        int    diterima;
        int    bayarPendaftaran;
        int    bayarDaftarUlang;

        JurusanRegData(Object id, String nama, int diterima) {
            this.id       = id;
            this.nama     = nama;
            this.diterima = diterima;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Konstanta
    // ═══════════════════════════════════════════════════════════════

    private static final int MAX_JURUSAN = 8;

    // ═══════════════════════════════════════════════════════════════
    // Konstruktor
    // ═══════════════════════════════════════════════════════════════

    public DashboardRegistrasiSiswa(PerguruanTinggi pt) {
        super(pt);
    }

    // ═══════════════════════════════════════════════════════════════
    // doRefreshSiswa
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void doRefreshSiswa(Session session, int tahunMasuk) {
        int total       = queryCount(session, tahunMasuk, "");
        int diterima    = queryCount(session, tahunMasuk,
                "AND cs.telahDiterima = true ");
        int bayarDaftar = queryCount(session, tahunMasuk,
                "AND cs.riwayatPembayaranPendaftaran IS NOT NULL "
                + "AND cs.riwayatPembayaranPendaftaran <> '' ");
        int daftarUlang = queryCount(session, tahunMasuk,
                "AND cs.riwayatPembayaranDaftarUlang IS NOT NULL "
                + "AND cs.riwayatPembayaranDaftarUlang <> '' ");
        int belumDU     = diterima - daftarUlang;
        if (belumDU < 0) { belumDU = 0; }

        List<JurusanRegData> jurusanList = queryPerJurusan(session, tahunMasuk);

        StringBuilder sb = new StringBuilder(6144);

        // ── Header
        sb.append("<div style=\"margin-bottom:14px;\">")
          .append("<div style=\"font-size:18px;font-weight:700;color:#1e3a5f;margin-bottom:3px;\">")
          .append("Status Registrasi &amp; Daftar Ulang &#8212; Tahun Masuk ").append(tahunMasuk)
          .append("</div>")
          .append("<div style=\"font-size:12px;color:#9ca3af;\">")
          .append("Pantau berapa calon yang diterima sudah menyelesaikan pembayaran pendaftaran "
                + "dan daftar ulang. Selisih = calon yang berpotensi tidak hadir (no-show).")
          .append("</div></div>");

        // ── KPI
        double pctDaftar = total > 0 ? (bayarDaftar * 100.0 / total) : 0;
        double pctDU     = diterima > 0 ? (daftarUlang * 100.0 / diterima) : 0;
        sb.append(HtmlChartHelper.kpiCards(
                new String[] { "Total Peminat", "Diterima",
                        "Bayar Pendaftaran", "Daftar Ulang", "Belum DU" },
                new String[] { fmtAngka(total), fmtAngka(diterima),
                        fmtAngka(bayarDaftar) + " (" + fmt1(pctDaftar) + "%)",
                        fmtAngka(daftarUlang) + " (" + fmt1(pctDU) + "%)",
                        fmtAngka(belumDU) },
                new String[] { "", "lulus seleksi", "dari peminat",
                        "dari diterima", "perlu follow-up" },
                new String[] { "", "", "", "", "" },
                new boolean[] { true, true, true, pctDU >= 80, belumDU == 0 },
                new String[] { "#1877f2", "#7c3aed", "#059669", "#10b981", "#dc2626" }));

        // ── Corong pembayaran
        sb.append(fullWidth(HtmlChartHelper.barHorizontal(
                "Corong Administrasi — Peminat sampai Daftar Ulang",
                "Alur lengkap: dari total peminat, yang diterima, yang bayar pendaftaran, "
                + "hingga yang menyelesaikan daftar ulang. "
                + "Selisih besar antara 'Diterima' dan 'Daftar Ulang' perlu penanganan cepat.",
                new String[] { "Total Peminat", "Diterima", "Bayar Pendaftaran", "Daftar Ulang" },
                new double[] { total, diterima, bayarDaftar, daftarUlang },
                "#7c3aed")));

        // ── Grouped bar per jurusan
        if (!jurusanList.isEmpty()) {
            int n        = Math.min(jurusanList.size(), MAX_JURUSAN);
            String[]   cats  = new String[n];
            double[][] vals  = new double[n][2];
            for (int i = 0; i < n; i++) {
                JurusanRegData d = jurusanList.get(i);
                cats[i]    = abbrev(d.nama, 18);
                vals[i][0] = d.diterima;
                vals[i][1] = d.bayarDaftarUlang;
            }
            sb.append(fullWidth(HtmlChartHelper.barVerticalGrouped(
                    "Diterima vs Daftar Ulang Per Jurusan",
                    "Per jurusan: biru = yang diterima, ungu = yang sudah daftar ulang. "
                    + "Jurusan dengan selisih besar membutuhkan follow-up telepon/WA segera.",
                    cats,
                    new String[] { "Diterima", "Daftar Ulang" },
                    vals,
                    new String[] { "#3b82f6", "#7c3aed" })));
        }

        new MyHtml(sb.toString()).setParent(contentHolder);

        // ── Tabel rekap
        buildTable(jurusanList, diterima, daftarUlang);
    }

    // ═══════════════════════════════════════════════════════════════
    // Tabel ZK Grid
    // ═══════════════════════════════════════════════════════════════

    private void buildTable(List<JurusanRegData> list, int totDiterima, int totDU) {
        if (list.isEmpty()) { return; }

        Div hdr = new Div();
        hdr.setStyle("display:flex;align-items:center;justify-content:space-between;"
                + "flex-wrap:wrap;gap:8px;margin-bottom:8px;");
        hdr.setParent(tableHolder);

        Div titleDiv = new Div();
        titleDiv.setParent(hdr);
        new MyLabelBoldAja("Status Daftar Ulang Per Jurusan").setParent(titleDiv);
        Label lbl = new Label(ais.common.Common.getBahasaConfig("Rincian diterima vs sudah daftar ulang per jurusan."));
        lbl.setStyle("font-size:11px;color:#6b7280;display:block;margin-top:2px;");
        lbl.setParent(titleDiv);

        final MyGrid grid = new MyGrid();
        grid.setWidth("100%");
        grid.setFixedLayout(true);
        grid.setParent(tableHolder);

        Columns cols = new Columns();
        cols.setParent(grid);
        addCol(cols, "No.",            "40px");
        addCol(cols, "Jurusan",        null);
        addCol(cols, "Diterima",       "90px");
        addCol(cols, "Daftar Ulang",   "100px");
        addCol(cols, "Belum DU",       "85px");
        addCol(cols, "Konversi (%)",   "100px");

        Rows rows = new Rows();
        rows.setParent(grid);

        for (int i = 0; i < list.size(); i++) {
            JurusanRegData d = list.get(i);
            int    belum     = d.diterima - d.bayarDaftarUlang;
            double pct       = d.diterima > 0 ? (d.bayarDaftarUlang * 100.0 / d.diterima) : 0;

            Row row = new Row();
            row.setParent(rows);
            new Label(String.valueOf(i + 1)).setParent(row);
            new Label(d.nama).setParent(row);
            new Label(fmtAngka(d.diterima)).setParent(row);
            new Label(fmtAngka(d.bayarDaftarUlang)).setParent(row);

            Label lblBelum = new Label(fmtAngka(belum));
            if (belum > 0) { lblBelum.setStyle("color:#dc2626;font-weight:600;"); }
            lblBelum.setParent(row);

            Label lblPct = new Label(fmt1(pct) + "%");
            lblPct.setStyle("color:" + (pct >= 80 ? "#16a34a" : pct >= 50 ? "#d97706" : "#dc2626")
                    + ";font-weight:600;");
            lblPct.setParent(row);
        }

        double totPct = totDiterima > 0 ? (totDU * 100.0 / totDiterima) : 0;
        Row rowTotal = new Row();
        rowTotal.setStyle("font-weight:bold;background:#f0f4ff;");
        rowTotal.setParent(rows);
        new Label("").setParent(rowTotal);
        new Label(ais.common.Common.getBahasaConfig("TOTAL")).setParent(rowTotal);
        new Label(fmtAngka(totDiterima)).setParent(rowTotal);
        new Label(fmtAngka(totDU)).setParent(rowTotal);
        new Label(fmtAngka(totDiterima - totDU)).setParent(rowTotal);
        new Label(fmt1(totPct) + "%").setParent(rowTotal);

        MyToolbarbuttonConfig btnDownload = new MyToolbarbuttonConfig("Unduh Excel", "/img/excel.png");
        btnDownload.setStyle("margin-top:8px;");
        btnDownload.setParent(tableHolder);
        btnDownload.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                UIUtil.downloadGrid(grid);
            }
        });
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
            logErr("DashboardRegistrasiSiswa.queryCount tahunMasuk=" + tahunMasuk, e);
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private List<JurusanRegData> queryPerJurusan(Session session, int tahunMasuk) {
        Map<Object, JurusanRegData> map = new LinkedHashMap<Object, JurusanRegData>();

        // Query 1: diterima per jurusan
        try {
            String hql = "SELECT ps.id, ps.nama, COUNT(cs) FROM CalonSiswa cs "
                    + "JOIN cs.penjurusanSekolah ps "
                    + "WHERE cs.tahunMasuk = :tahunMasuk AND cs.telahDiterima = true "
                    + "GROUP BY ps.id, ps.nama ORDER BY COUNT(cs) DESC";
            List<Object[]> rows = (List<Object[]>) session.createQuery(hql)
                    .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk))
                    .setMaxResults(MAX_JURUSAN).list();
            for (int i = 0; i < rows.size(); i++) {
                Object[] r  = rows.get(i);
                Object   id = r[0];
                String   nm = r[1] == null ? "-" : r[1].toString();
                int     cnt = r[2] == null ? 0 : ((Number) r[2]).intValue();
                map.put(id, new JurusanRegData(id, nm, cnt));
            }
        } catch (Exception e) {
            logErr("DashboardRegistrasiSiswa.queryPerJurusan diterima tahunMasuk=" + tahunMasuk, e);
        }

        // Query 2: bayar pendaftaran per jurusan
        try {
            String hql = "SELECT ps.id, COUNT(cs) FROM CalonSiswa cs "
                    + "JOIN cs.penjurusanSekolah ps "
                    + "WHERE cs.tahunMasuk = :tahunMasuk "
                    + "AND cs.riwayatPembayaranPendaftaran IS NOT NULL "
                    + "AND cs.riwayatPembayaranPendaftaran <> '' "
                    + "GROUP BY ps.id";
            List<Object[]> rows = (List<Object[]>) session.createQuery(hql)
                    .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk)).list();
            for (int i = 0; i < rows.size(); i++) {
                Object[] r  = rows.get(i);
                Object   id = r[0];
                int     cnt = r[1] == null ? 0 : ((Number) r[1]).intValue();
                if (map.containsKey(id)) { map.get(id).bayarPendaftaran = cnt; }
            }
        } catch (Exception e) {
            logErr("DashboardRegistrasiSiswa.queryPerJurusan bayarDaftar tahunMasuk=" + tahunMasuk, e);
        }

        // Query 3: daftar ulang per jurusan
        try {
            String hql = "SELECT ps.id, COUNT(cs) FROM CalonSiswa cs "
                    + "JOIN cs.penjurusanSekolah ps "
                    + "WHERE cs.tahunMasuk = :tahunMasuk "
                    + "AND cs.riwayatPembayaranDaftarUlang IS NOT NULL "
                    + "AND cs.riwayatPembayaranDaftarUlang <> '' "
                    + "GROUP BY ps.id";
            List<Object[]> rows = (List<Object[]>) session.createQuery(hql)
                    .setParameter("tahunMasuk", Integer.valueOf(tahunMasuk)).list();
            for (int i = 0; i < rows.size(); i++) {
                Object[] r  = rows.get(i);
                Object   id = r[0];
                int     cnt = r[1] == null ? 0 : ((Number) r[1]).intValue();
                if (map.containsKey(id)) { map.get(id).bayarDaftarUlang = cnt; }
            }
        } catch (Exception e) {
            logErr("DashboardRegistrasiSiswa.queryPerJurusan daftarUlang tahunMasuk=" + tahunMasuk, e);
        }

        return new ArrayList<JurusanRegData>(map.values());
    }
}
