package ais.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;

/**
 * Util ringan untuk menampilkan ringkasan jurnal pembayaran pada dashboard pembayaran.
 * Kompatibel Java 1.6/1.7 dan ZK 5.5, tanpa lambda/stream dan tanpa JFreeChart.
 */
public final class DashboardJurnalPembayaranUtil {

    private static final int LIMIT_ROW = 12;

    private DashboardJurnalPembayaranUtil() {
    }

    public static void renderJurnalMahasiswaPanel(Component parent, String title, String description) {
        renderPanel(parent, title, description, true);
    }

    public static void renderJurnalSiswaPanel(Component parent, String title, String description) {
        renderPanel(parent, title, description, false);
    }

    private static void renderPanel(Component parent, String title, String description, boolean mahasiswa) {
        if (parent == null) {
            return;
        }
        Component targetParent = parent;
        if (parent instanceof MyPortallayout) {
            MyPortalchildren pc = new MyPortalchildren();
            pc.setWidth("100%");
            pc.setStyle("padding:6px; box-sizing:border-box;");
            pc.setParent(parent);
            targetParent = pc;
        }

        Panel panel = new ais.ui.util.MyPanelConfig();
        panel.setTitle(title == null || title.trim().length() == 0 ? "Ringkasan Jurnal Pembayaran" : title);
        panel.setBorder("none");
        panel.setCollapsible(true);
        panel.setClosable(false);
        panel.setMaximizable(false);
        panel.setMinimizable(false);
        panel.setStyle("margin-bottom:12px; border:1px solid #e5e7eb; border-radius:18px; overflow:hidden;"
                + "background:#ffffff; box-shadow:0 12px 26px rgba(15,23,42,.08);");
        panel.setParent(targetParent);

        Panelchildren pc = new Panelchildren();
        pc.setStyle("padding:0; background:#ffffff;");
        pc.setParent(panel);

        Vbox wrap = new Vbox();
        wrap.setWidth("100%");
        wrap.setStyle("padding:14px; box-sizing:border-box; background:#f8fafc; overflow:auto;");
        wrap.setParent(pc);

        List rows = mahasiswa ? loadMahasiswaRows() : loadSiswaRows();
        renderIntro(wrap, description, rows, mahasiswa);
        renderGrid(wrap, rows);
    }

    private static void renderIntro(Component parent, String description, List rows, boolean mahasiswa) {
        double debet = 0.0;
        double kredit = 0.0;
        int lengkap = 0;
        if (rows != null) {
            for (int i = 0; i < rows.size(); i++) {
                JurnalDashboardRow r = (JurnalDashboardRow) rows.get(i);
                debet += r.debet;
                kredit += r.kredit;
                if (r.akunLengkap) {
                    lengkap++;
                }
            }
        }
        int total = rows == null ? 0 : rows.size();
        int persenLengkap = total == 0 ? 0 : (int) Math.round((lengkap * 100.0) / total);
        int persenSeimbang = (debet + kredit) <= 0.1 ? 0 : (int) Math.round((Math.min(debet, kredit) * 100.0) / Math.max(debet, kredit));
        if (persenSeimbang > 100) {
            persenSeimbang = 100;
        }

        StringBuilder html = new StringBuilder();
        html.append("<div style='border-radius:18px; padding:16px; color:#0f172a; background:linear-gradient(135deg,#ffffff,#eff6ff); border:1px solid #dbeafe;'>");
        html.append("<div style='display:flex; justify-content:space-between; gap:12px; flex-wrap:wrap; align-items:flex-start;'>");
        html.append("<div style='min-width:240px; flex:1;'>");
        html.append("<div style='font-size:17px; font-weight:900;'>Arah jurnal pembayaran</div>");
        html.append("<div style='font-size:12px; color:#475569; margin-top:5px; line-height:1.55;'>");
        html.append(escape(description == null || description.trim().length() == 0
                ? "Menunjukkan akun kas/bank, piutang, pendapatan, denda, dan diskon yang membentuk jurnal pembayaran. Data ini membantu petugas melihat apakah akun sudah lengkap sebelum transaksi diposting."
                : description));
        html.append("</div></div>");
        html.append(metric("Total Debit", money(debet), "Kas/bank/deposit dan potongan yang masuk sisi debit."));
        html.append(metric("Total Kredit", money(kredit), "Pendapatan, piutang, denda, dan kewajiban yang masuk sisi kredit."));
        html.append(metric("Akun Lengkap", persenLengkap + "%", "Semakin tinggi semakin sedikit akun yang belum diatur."));
        html.append("</div>");
        html.append("<div style='margin-top:14px; display:flex; gap:10px; flex-wrap:wrap;'>");
        html.append(bar("Kelengkapan Akun", persenLengkap));
        html.append(bar("Keseimbangan Debit Kredit", persenSeimbang));
        html.append(bar(mahasiswa ? "Kesiapan Jurnal Mahasiswa" : "Kesiapan Jurnal Siswa", (persenLengkap + persenSeimbang) / 2));
        html.append("</div>");
        html.append("<div style='font-size:11px; color:#64748b; margin-top:10px;'>Nilai diambil sebagai ringkasan historis/preview. Detail transaksi tetap mengikuti proses posting yang sudah ada.</div>");
        html.append("</div>");
        new Html(html.toString()).setParent(parent);
    }

    private static String metric(String title, String value, String note) {
        return "<div style='min-width:150px; padding:12px 14px; border-radius:15px; background:#ffffff; border:1px solid #e2e8f0; box-shadow:0 8px 18px rgba(15,23,42,.06);'>"
                + "<div style='font-size:11px; color:#64748b; font-weight:800;'>" + escape(title) + "</div>"
                + "<div style='font-size:19px; font-weight:900; color:#1d4ed8; margin-top:3px;'>" + escape(value) + "</div>"
                + "<div style='font-size:10px; color:#64748b; margin-top:4px; line-height:1.35;'>" + escape(note) + "</div></div>";
    }

    private static String bar(String label, int percent) {
        int p = percent < 0 ? 0 : (percent > 100 ? 100 : percent);
        return "<div style='flex:1; min-width:210px; padding:10px 12px; border-radius:14px; background:#ffffff; border:1px solid #e2e8f0;'>"
                + "<div style='display:flex; justify-content:space-between; font-size:11px; color:#334155; font-weight:800;'><span>" + escape(label) + "</span><span>" + p + "%</span></div>"
                + "<div style='margin-top:7px; height:10px; border-radius:999px; background:#e2e8f0; overflow:hidden;'>"
                + "<div style='height:10px; width:" + p + "%; border-radius:999px; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));'></div></div></div>";
    }

    private static void renderGrid(Component parent, List rows) {
        Grid grid = new Grid();
        grid.setWidth("100%");
        grid.setMold("paging");
        grid.setPageSize(10);
        grid.setStyle("margin-top:12px; border-radius:14px; overflow:hidden; background:#ffffff; border:1px solid #e5e7eb;");
        grid.setParent(parent);

        Columns columns = new Columns();
        columns.setParent(grid);
        new MyColumnConfig("Arah").setParent(columns);
        new MyColumnConfig("Akun").setParent(columns);
        new MyColumnConfig("Debit").setParent(columns);
        new MyColumnConfig("Kredit").setParent(columns);
        new MyColumnConfig("Catatan").setParent(columns);

        Rows rs = new Rows();
        rs.setParent(grid);
        double totalD = 0.0;
        double totalK = 0.0;
        if (rows == null || rows.isEmpty()) {
            Row r = new Row();
            r.setParent(rs);
            ais.ui.util.ZkCompat.setSpans(r, "5");
            r.appendChild(new org.zkoss.zul.Label("Belum ada data akun jurnal yang bisa diringkas berdasarkan filter/data saat ini."));
        } else {
            for (int i = 0; i < rows.size(); i++) {
                JurnalDashboardRow data = (JurnalDashboardRow) rows.get(i);
                totalD += data.debet;
                totalK += data.kredit;
                Row r = new Row();
                r.setParent(rs);
                r.appendChild(new org.zkoss.zul.Label(data.debet >= data.kredit ? "Debit" : "Kredit"));
                r.appendChild(new org.zkoss.zul.Label(data.kodeAkun + " - " + data.namaAkun));
                r.appendChild(new org.zkoss.zul.Label(money(data.debet)));
                r.appendChild(new org.zkoss.zul.Label(money(data.kredit)));
                r.appendChild(new org.zkoss.zul.Label(data.catatan));
            }
        }
        Foot foot = new Foot();
        foot.setParent(grid);
        Footer f = new Footer("Total");
        f.setSpan(2);
        f.setParent(foot);
        new Footer(money(totalD)).setParent(foot);
        new Footer(money(totalK)).setParent(foot);
        new Footer(Math.abs(totalD - totalK) <= 0.1 ? "Seimbang" : "Perlu cek akun/nominal").setParent(foot);
    }

    private static List loadMahasiswaRows() {
        List rows = new ArrayList();
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            // Batasan per-baris: nilai & denda maksimal 100 juta (realistis 1 cicilan mhs).
            // Cap lama 1 triliun terlalu longgar sehingga data historis korup bisa lolos.
            // Scope: 12 bulan terakhir supaya konsisten dengan panel Status Tagihan.
            final String CAP = "100000000";   // 100 juta
            final String DATE_FILTER = "and cp.tanggal_tagihan >= (current_date - interval '365 days') ";
            String sqlDebet = "select coalesce(a.kode,'-') kode, coalesce(a.nama,'Akun pembayaran belum diatur') nama, "
                    + "coalesce(sum(coalesce(cp.nilai,0) + coalesce(cp.denda,0)),0) nilai "
                    + "from public.cicilan_pembayaran cp "
                    + "inner join public.kegiatan keg on keg.id = cp.kegiatan and keg.aktif = true "
                    + "left join public.jenis_pembayaran jp on cp.jenis_pembayaran = jp.id "
                    + "left join akunting.akun a on jp.akun = a.id "
                    + "where coalesce(cp.nilai,0) > 0.1 "
                    + "and coalesce(cp.nilai,0) < " + CAP + " "
                    + "and coalesce(cp.denda,0) < " + CAP + " "
                    + DATE_FILTER
                    + "group by a.kode, a.nama order by nilai desc limit " + LIMIT_ROW;
            rows.addAll(queryRows(session, sqlDebet, true, "Akun penerimaan dari jenis pembayaran mahasiswa (12 bulan terakhir)."));
            String sqlKredit = "select coalesce(a.kode,'-') kode, coalesce(a.nama,'Akun biaya belum diatur') nama, "
                    + "coalesce(sum(coalesce(cp.nilai,0)),0) nilai "
                    + "from public.cicilan_pembayaran cp "
                    + "inner join public.kegiatan keg on keg.id = cp.kegiatan and keg.aktif = true "
                    + "left join public.item_biaya ib on cp.item_biaya = ib.id "
                    + "left join akunting.akun a on a.id = coalesce( "
                    + "   (select x.akun from public.item_biaya_punya_piutang x where x.item_biaya = ib.id order by x.id desc limit 1), "
                    + "   (select x.akun from public.item_biaya_punya_dibayar_dimuka x where x.item_biaya = ib.id order by x.id desc limit 1), "
                    + "   (select x.akun from public.item_biaya_punya_akun x where x.item_biaya = ib.id order by x.id desc limit 1) "
                    + ") "
                    + "where coalesce(cp.nilai,0) > 0.1 "
                    + "and coalesce(cp.nilai,0) < " + CAP + " "
                    + DATE_FILTER
                    + "group by a.kode, a.nama order by nilai desc limit " + LIMIT_ROW;
            rows.addAll(queryRows(session, sqlKredit, false, "Akun piutang/dibayar dimuka/pendapatan dari pengaturan item biaya mahasiswa (12 bulan terakhir)."));

            String sqlKreditDenda = "select coalesce(a.kode,'-') kode, coalesce(a.nama,'Akun denda belum diatur') nama, "
                    + "coalesce(sum(coalesce(cp.denda,0)),0) nilai "
                    + "from public.cicilan_pembayaran cp "
                    + "inner join public.kegiatan keg on keg.id = cp.kegiatan and keg.aktif = true "
                    + "left join public.item_biaya ib on cp.item_biaya = ib.id "
                    + "left join akunting.akun a on a.id = (select x.akun from public.item_biaya_punya_pendapatan_denda x where x.item_biaya = ib.id order by x.id desc limit 1) "
                    + "where coalesce(cp.denda,0) > 0.1 "
                    + "and coalesce(cp.denda,0) < " + CAP + " "
                    + DATE_FILTER
                    + "group by a.kode, a.nama order by nilai desc limit " + LIMIT_ROW;
            rows.addAll(queryRows(session, sqlKreditDenda, false, "Akun pendapatan denda dari pengaturan item biaya mahasiswa."));
        } catch (Exception e) {
            rows.add(errorRow("Ringkasan jurnal mahasiswa belum bisa dihitung: " + e.getMessage()));
        } finally {
            closeSession(session);
        }
        return rows;
    }

    private static List loadSiswaRows() {
        List rows = new ArrayList();
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            String sqlDebet = "select coalesce(a.kode,'-') kode, coalesce(a.nama,'Akun pembayaran belum diatur') nama, "
                    + "coalesce(sum(coalesce(psd.nominal,0)),0) nilai "
                    + "from sekolah.pembayaran_siswa_detail psd "
                    + "left join sekolah.pembayaran_siswa ps on psd.pembayaran_siswa_id = ps.id "
                    + "left join sekolah.akun_pembayaran_siswa aps on ps.akun_pembayaran_siswa_id = aps.id "
                    + "left join akunting.akun a on aps.akun_id = a.id "
                    + "where coalesce(psd.nominal,0) > 0.1 and coalesce(psd.nominal,0) < 1000000000000 "
                    + "group by a.kode, a.nama order by nilai desc limit " + LIMIT_ROW;
            rows.addAll(queryRows(session, sqlDebet, true, "Akun kas/bank dari cara pembayaran siswa."));
            String sqlKredit = "select coalesce(a.kode,'-') kode, coalesce(a.nama,'Akun item biaya belum diatur') nama, "
                    + "coalesce(sum(coalesce(psd.nominal,0)),0) nilai "
                    + "from sekolah.pembayaran_siswa_detail psd "
                    + "left join sekolah.item_biaya_sekolah ib on psd.item_biaya_id = ib.id "
                    + "left join akunting.akun a on coalesce(ib.akun_piutang_id, ib.akun_dibayar_dimuka_id, ib.akun_id) = a.id "
                    + "where coalesce(psd.nominal,0) > 0.1 and coalesce(psd.nominal,0) < 1000000000000 "
                    + "group by a.kode, a.nama order by nilai desc limit " + LIMIT_ROW;
            rows.addAll(queryRows(session, sqlKredit, false, "Akun piutang/pendapatan dari item biaya siswa."));
        } catch (Exception e) {
            rows.add(errorRow("Ringkasan jurnal siswa belum bisa dihitung: " + e.getMessage()));
        } finally {
            closeSession(session);
        }
        return rows;
    }

    private static List queryRows(Session session, String sql, boolean debet, String catatan) {
        List result = new ArrayList();
        try {
            SQLQuery q = session.createSQLQuery(sql);
            List list = q.list();
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    Object[] o = (Object[]) list.get(i);
                    String kode = o[0] == null ? "-" : String.valueOf(o[0]);
                    String nama = o[1] == null ? "Akun belum diatur" : String.valueOf(o[1]);
                    double nilai = toDouble(o[2]);
                    if (Math.abs(nilai) <= 0.1) {
                        continue;
                    }
                    JurnalDashboardRow row = new JurnalDashboardRow();
                    row.kodeAkun = kode;
                    row.namaAkun = nama;
                    row.debet = debet ? nilai : 0.0;
                    row.kredit = debet ? 0.0 : nilai;
                    row.catatan = catatan;
                    row.akunLengkap = !"-".equals(kode) && nama != null && nama.indexOf("belum diatur") < 0;
                    result.add(row);
                }
            }
        } catch (Exception e) {
            result.add(errorRow("Query jurnal belum sesuai struktur database ini: " + e.getMessage()));
        }
        return result;
    }

    private static JurnalDashboardRow errorRow(String pesan) {
        JurnalDashboardRow row = new JurnalDashboardRow();
        row.kodeAkun = "-";
        row.namaAkun = "Informasi";
        row.debet = 0.0;
        row.kredit = 0.0;
        row.catatan = pesan == null ? "Belum ada informasi." : pesan;
        row.akunLengkap = false;
        return row;
    }

    private static double toDouble(Object o) {
        if (o == null) {
            return 0.0;
        }
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static String money(double d) {
        try {
            return Common.numberFormat.get().format(d);
        } catch (Exception e) {
            return String.valueOf(Math.round(d));
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static void closeSession(Session session) {
        if (session == null) {
            return;
        }
        try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/DashboardJurnalPembayaranUtil.java:352");}
        try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/DashboardJurnalPembayaranUtil.java:353");}
        try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/DashboardJurnalPembayaranUtil.java:354");}
    }

    /**
     * Tipe implementasi bersarang {@link JurnalDashboardRow} milik {@link DashboardJurnalPembayaranUtil}. Kelas
     * ini memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * DashboardJurnalPembayaranUtil}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
     * kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String kodeAkun}, {@code String
     * namaAkun}, {@code double debet}, {@code double kredit}, {@code String catatan}, {@code boolean akunLengkap}.
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see DashboardJurnalPembayaranUtil
     */
    private static class JurnalDashboardRow {
        String kodeAkun;
        String namaAkun;
        double debet;
        double kredit;
        String catatan;
        boolean akunLengkap;
    }
}
