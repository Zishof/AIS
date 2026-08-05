package ais.action.master.sekolah;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbarbutton;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;

/**
 * DasborKeuanganSiswaAction — dasbor keuangan modul PSB (Penerimaan Siswa Baru).
 *
 * Menyajikan ringkasan tagihan dan pembayaran seluruh calon siswa baru: kartu
 * ringkasan, donut status pelunasan, grafik batang per tahun masuk, dan tabel
 * rekap dengan ekspor Excel.
 *
 * DATA SOURCE:
 * - Entity utama: Tagihan (sekolah) dengan FK ke CalonSiswa.
 * - Field Tagihan yang dipakai: nominal (jumlah tagihan), dibayar (terbayar).
 * - Lunas = Tagihan.dibayar >= Tagihan.nominal (per record Tagihan).
 *
 * FILTER:
 * - Tahun Masuk (CalonSiswa.tahunMasuk, Integer). Nilai 0 = semua tahun.
 *
 * SESSION: openSession() + finally clear/disconnect/close. Tidak memakai currentSession().
 * KOMPATIBILITAS: Java 1.7 — tanpa lambda, try-with-resources, diamond, atau Stream.
 */
public class DasborKeuanganSiswaAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 20260704L;

    private Div rootContainer;

    private int filterTahun = 0;

    private Combobox cboTahun;
    private Div      divDasborContent;

    private long         totalCalonSiswa = 0L;
    private long         totalTagihan    = 0L;
    private long         totalDibayar    = 0L;
    private long         totalLunas      = 0L;
    private List<Object[]> dataPerTahun  = new ArrayList<Object[]>();

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        filterTahun = Calendar.getInstance().get(Calendar.YEAR);
        bangunHeaderDanFilter();
        muatDasbor();
    }

    // =========================================================================
    // Header & filter bar
    // =========================================================================

    private void bangunHeaderDanFilter() {
        new Html(buildCss()).setParent(rootContainer);

        new Html(
            "<div class='dgk-header'>" +
            "<div class='dgk-title'>&#x1F4CA; Dasbor Keuangan Calon Siswa</div>" +
            "<div class='dgk-subtitle'>Ringkasan tagihan dan pembayaran seluruh calon siswa baru." +
            " Atur filter lalu klik <b>Tampilkan</b> untuk memperbarui data.</div>" +
            "</div>"
        ).setParent(rootContainer);

        Div bar = new Div();
        bar.setSclass("dgk-filter-bar");
        bar.setParent(rootContainer);

        // Combobox Tahun Masuk
        new Label(ais.common.Common.getBahasaConfig("Tahun Masuk:")).setParent(bar);
        cboTahun = new Combobox();
        cboTahun.setWidth("88px");
        cboTahun.setReadonly(true);
        Comboitem ciSemua = new Comboitem("Semua");
        ciSemua.setValue(Integer.valueOf(0));
        cboTahun.appendChild(ciSemua);
        int nowYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int t = nowYear + 1; t >= nowYear - 5; t--) {
            Comboitem ci = new Comboitem(String.valueOf(t));
            ci.setValue(Integer.valueOf(t));
            cboTahun.appendChild(ci);
            if (t == filterTahun) cboTahun.setSelectedItem(ci);
        }
        if (cboTahun.getSelectedItem() == null) cboTahun.setSelectedIndex(0);
        cboTahun.setParent(bar);

        // Tombol Tampilkan
        Toolbarbutton btnTampilkan = new Toolbarbutton("Tampilkan");
        btnTampilkan.setImage("/img/search.gif");
        btnTampilkan.setParent(bar);
        btnTampilkan.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                Comboitem ciT = cboTahun.getSelectedItem();
                Object vT = ciT != null ? ciT.getValue() : null;
                filterTahun = (vT instanceof Integer) ? ((Integer) vT).intValue() : 0;
                muatDasbor();
            }
        });

        divDasborContent = new Div();
        divDasborContent.setStyle("margin:0;padding:0 16px 24px;box-sizing:border-box;");
        divDasborContent.setParent(rootContainer);
    }

    // =========================================================================
    // Load dashboard
    // =========================================================================

    private void muatDasbor() {
        Common.clear(divDasborContent);
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            queryData(s);
        } catch (Exception ex) {
            ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/sekolah/DasborKeuanganSiswaAction.java:146");
        } finally {
            try { s.clear(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/DasborKeuanganSiswaAction.java:148");}
            try { s.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/DasborKeuanganSiswaAction.java:149");}
            try { s.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/DasborKeuanganSiswaAction.java:150");}
        }
        renderDasbor();
    }

    // =========================================================================
    // Query
    // =========================================================================

    private void queryData(Session s) {
        // --- Total CalonSiswa ---
        try {
            Query q = s.createQuery(
                "SELECT COUNT(cs.id) FROM CalonSiswa cs" + buildWhere("cs"));
            applyParams(q);
            Number n = (Number) q.uniqueResult();
            totalCalonSiswa = n == null ? 0L : n.longValue();
        } catch (Exception e) {
            totalCalonSiswa = 0L;
        }

        // --- Total tagihan ---
        try {
            Query q = s.createQuery(
                "SELECT SUM(t.nominal) FROM Tagihan t WHERE t.calonSiswa IS NOT NULL" +
                buildWhereAnd("t.calonSiswa"));
            applyParams(q);
            Number n = (Number) q.uniqueResult();
            totalTagihan = n == null ? 0L : n.longValue();
        } catch (Exception e) {
            totalTagihan = 0L;
        }

        // --- Total dibayar ---
        try {
            Query q = s.createQuery(
                "SELECT SUM(t.dibayar) FROM Tagihan t WHERE t.calonSiswa IS NOT NULL" +
                buildWhereAnd("t.calonSiswa"));
            applyParams(q);
            Number n = (Number) q.uniqueResult();
            totalDibayar = n == null ? 0L : n.longValue();
        } catch (Exception e) {
            totalDibayar = 0L;
        }

        // --- Lunas: calon siswa yang memiliki tagihan dengan dibayar >= nominal ---
        try {
            Query q = s.createQuery(
                "SELECT COUNT(DISTINCT t.calonSiswa.id) FROM Tagihan t " +
                "WHERE t.calonSiswa IS NOT NULL AND t.dibayar >= t.nominal" +
                buildWhereAnd("t.calonSiswa"));
            applyParams(q);
            Number n = (Number) q.uniqueResult();
            totalLunas = n == null ? 0L : n.longValue();
        } catch (Exception e) {
            totalLunas = 0L;
        }

        // --- Per tahun masuk (grafik batang) ---
        try {
            Query q = s.createQuery(
                "SELECT cs.tahunMasuk, COUNT(DISTINCT cs.id), SUM(t.nominal), SUM(t.dibayar) " +
                "FROM Tagihan t JOIN t.calonSiswa cs " +
                "WHERE t.calonSiswa IS NOT NULL " +
                "GROUP BY cs.tahunMasuk ORDER BY cs.tahunMasuk DESC");
            q.setMaxResults(8);
            dataPerTahun = q.list();
        } catch (Exception e) {
            dataPerTahun = new ArrayList<Object[]>();
        }
    }

    private String buildWhere(String alias) {
        if (filterTahun > 0) return " WHERE " + alias + ".tahunMasuk = :tahunMasuk";
        return "";
    }

    private String buildWhereAnd(String alias) {
        if (filterTahun > 0) return " AND " + alias + ".tahunMasuk = :tahunMasuk";
        return "";
    }

    private void applyParams(Query q) {
        if (filterTahun > 0) q.setInteger("tahunMasuk", filterTahun);
    }

    // =========================================================================
    // Render dashboard
    // =========================================================================

    private void renderDasbor() {
        String filterLabel = filterTahun > 0 ? "Tahun Masuk " + filterTahun : "Semua Tahun";
        String updated = new SimpleDateFormat("dd MMM yyyy HH:mm").format(new Date());
        new Html(
            "<div class='dgk-filter-info'>Menampilkan: <b>" + filterLabel + "</b>" +
            " &nbsp;&bull;&nbsp; Diperbarui: " + updated + "</div>"
        ).setParent(divDasborContent);

        new Html(buildKartuRingkasan()).setParent(divDasborContent);
        new Html(buildDuaKolom()).setParent(divDasborContent);
        new Html(buildGrafikPerTahun()).setParent(divDasborContent);
        buildTabelRekap();
    }

    // =========================================================================
    // HTML builders
    // =========================================================================

    private String buildKartuRingkasan() {
        long sisa = Math.max(0L, totalTagihan - totalDibayar);
        int pctDibayar = totalTagihan > 0
            ? (int) Math.round(totalDibayar * 100.0 / totalTagihan) : 0;
        return "<div class='dgk-cards'>" +
            kartu("var(--ais-theme-primary,#1e40af)", "#dbeafe", "&#x1F465;", "Total Calon Siswa",
                fmt(totalCalonSiswa), "Calon yang terdaftar di sistem") +
            kartu("#5b21b6", "#ede9fe", "&#x1F4B0;", "Total Tagihan",
                "Rp " + fmt(totalTagihan), "Seluruh tagihan biaya pendaftaran") +
            kartu("#14532d", "#dcfce7", "&#x2705;", "Sudah Dibayar",
                "Rp " + fmt(totalDibayar), pctDibayar + "% dari total tagihan") +
            kartu("#7f1d1d", "#fee2e2", "&#x23F3;", "Sisa Belum Lunas",
                "Rp " + fmt(sisa), "Masih harus dilunasi") +
            "</div>";
    }

    private String kartu(String warna, String bg, String ikon, String lbl, String val, String sub) {
        return "<div class='dgk-card' style='border-top:3px solid " + warna + ";'>" +
            "<div class='dgk-card-ico' style='background:" + bg + ";color:" + warna + ";'>" + ikon + "</div>" +
            "<div class='dgk-card-lbl'>" + esc(lbl) + "</div>" +
            "<div class='dgk-card-val'>" + esc(val) + "</div>" +
            "<div class='dgk-card-sub'>" + sub + "</div>" +
            "</div>";
    }

    private String buildDuaKolom() {
        return "<div class='dgk-2col'>" + buildDonut() + buildProgressPanel() + "</div>";
    }

    private String buildDonut() {
        long belum = Math.max(0L, totalCalonSiswa - totalLunas);
        int pct = totalCalonSiswa > 0
            ? (int) Math.round(totalLunas * 100.0 / totalCalonSiswa) : 0;
        return "<div class='dgk-sect'>" +
            "<div class='dgk-sect-title'>Status Pelunasan</div>" +
            "<div class='dgk-sect-desc'>Komposisi calon siswa yang sudah melunasi tagihan.</div>" +
            "<div style='display:flex;align-items:center;justify-content:center;gap:24px;flex-wrap:wrap;padding:12px 0;'>" +
            "<div style='width:130px;height:130px;border-radius:50%;" +
            "background:conic-gradient(#16a34a 0% " + pct + "%, #e2e8f0 " + pct + "% 100%);" +
            "position:relative;box-shadow:0 4px 12px rgba(0,0,0,.12);flex:0 0 130px;'>" +
            "<div style='position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);" +
            "width:86px;height:86px;border-radius:50%;background:#fff;" +
            "display:flex;flex-direction:column;align-items:center;justify-content:center;gap:2px;'>" +
            "<div style='font-size:22px;font-weight:900;color:#1e293b;'>" + pct + "%</div>" +
            "<div style='font-size:9px;font-weight:700;color:#64748b;letter-spacing:.5px;'>LUNAS</div>" +
            "</div></div>" +
            "<div style='display:flex;flex-direction:column;gap:12px;'>" +
            legendItem("#16a34a", "Sudah Lunas", fmt(totalLunas)) +
            legendItem("#cbd5e1", "Belum Lunas", fmt(belum)) +
            "</div></div></div>";
    }

    private String legendItem(String color, String label, String val) {
        return "<div style='display:flex;align-items:center;gap:8px;'>" +
            "<div style='width:13px;height:13px;border-radius:50%;background:" + color + ";flex:0 0 13px;'></div>" +
            "<div><div style='font-weight:700;font-size:13px;color:#1e293b;'>" + val + "</div>" +
            "<div style='font-size:11px;color:#64748b;'>" + esc(label) + "</div></div></div>";
    }

    private String buildProgressPanel() {
        long sisa = Math.max(0L, totalTagihan - totalDibayar);
        int pct = totalTagihan > 0
            ? (int) Math.round(totalDibayar * 100.0 / totalTagihan) : 0;
        String color = pct >= 80 ? "#16a34a" : pct >= 50 ? "#f59e0b" : "#ef4444";
        return "<div class='dgk-sect'>" +
            "<div class='dgk-sect-title'>Realisasi Pembayaran</div>" +
            "<div class='dgk-sect-desc'>Seberapa besar tagihan yang sudah terealisasi dari total yang seharusnya diterima.</div>" +
            "<div style='padding:12px 0 4px;'>" +
            "<div style='font-size:36px;font-weight:900;color:" + color + ";line-height:1;'>" + pct + "%</div>" +
            "<div style='font-size:11px;color:#64748b;margin:4px 0 12px;'>realisasi pembayaran keseluruhan</div>" +
            "<div style='height:12px;background:#f1f5f9;border-radius:6px;overflow:hidden;margin-bottom:16px;'>" +
            "<div style='height:100%;width:" + pct + "%;background:linear-gradient(90deg," + color + ",#86efac);border-radius:6px;'></div></div>" +
            "<div style='display:grid;grid-template-columns:1fr 1fr;gap:10px;'>" +
            "<div style='background:#f0fdf4;border-radius:8px;padding:10px;'>" +
            "<div style='font-size:9px;font-weight:700;color:#16a34a;text-transform:uppercase;letter-spacing:.5px;'>Dibayar</div>" +
            "<div style='font-size:14px;font-weight:800;color:#14532d;word-break:break-all;'>Rp " + fmt(totalDibayar) + "</div></div>" +
            "<div style='background:#fef2f2;border-radius:8px;padding:10px;'>" +
            "<div style='font-size:9px;font-weight:700;color:#ef4444;text-transform:uppercase;letter-spacing:.5px;'>Sisa</div>" +
            "<div style='font-size:14px;font-weight:800;color:#7f1d1d;word-break:break-all;'>Rp " + fmt(sisa) + "</div></div>" +
            "</div></div></div>";
    }

    private String buildGrafikPerTahun() {
        if (dataPerTahun == null || dataPerTahun.isEmpty()) {
            return "<div class='dgk-sect'><div class='dgk-sect-title'>Tagihan per Tahun Masuk</div>" +
                "<div style='padding:24px;text-align:center;color:#94a3b8;font-size:13px;'>" +
                "Tidak ada data tersedia.</div></div>";
        }
        long maxT = 1L;
        for (Object[] r : dataPerTahun) {
            long t = r[2] == null ? 0L : ((Number) r[2]).longValue();
            if (t > maxT) maxT = t;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='dgk-sect'>");
        sb.append("<div class='dgk-sect-title'>Tagihan per Tahun Masuk</div>");
        sb.append("<div class='dgk-sect-desc'>Total tagihan dan realisasi pembayaran per angkatan calon siswa.</div>");
        sb.append("<div style='padding:8px 0;'>");
        for (Object[] r : dataPerTahun) {
            int  thn = r[0] == null ? 0 : ((Number) r[0]).intValue();
            long jml = r[1] == null ? 0L : ((Number) r[1]).longValue();
            long tag = r[2] == null ? 0L : ((Number) r[2]).longValue();
            long byr = r[3] == null ? 0L : ((Number) r[3]).longValue();
            int  wT  = (int) Math.round(tag * 100.0 / maxT);
            int  wB  = tag > 0 ? (int) Math.round(byr * 100.0 / maxT) : 0;
            int  pct = tag > 0 ? (int) Math.round(byr * 100.0 / tag)  : 0;
            sb.append("<div style='margin-bottom:16px;'>");
            sb.append("<div style='display:flex;justify-content:space-between;margin-bottom:4px;'>");
            sb.append("<span style='font-size:13px;font-weight:700;color:#1e293b;'>").append(thn).append("</span>");
            sb.append("<span style='font-size:11px;color:#64748b;'>")
              .append(fmt(jml)).append(" calon &bull; ").append(pct).append("% terbayar</span>");
            sb.append("</div>");
            sb.append("<div style='height:10px;background:#e2e8f0;border-radius:5px;overflow:hidden;margin-bottom:3px;'>");
            sb.append("<div style='height:100%;width:").append(wT).append("%;background:#94a3b8;border-radius:5px;'></div></div>");
            sb.append("<div style='height:10px;background:#e2e8f0;border-radius:5px;overflow:hidden;'>");
            sb.append("<div style='height:100%;width:").append(wB)
              .append("%;background:linear-gradient(90deg,#1d4ed8,#60a5fa);border-radius:5px;'></div></div>");
            sb.append("<div style='display:flex;gap:12px;margin-top:4px;'>");
            sb.append("<span style='font-size:10px;color:#64748b;'>&#x25A0; Tagihan: Rp ").append(fmt(tag)).append("</span>");
            sb.append("<span style='font-size:10px;color:var(--ais-theme-primary,#1d4ed8);'>&#x25A0; Dibayar: Rp ").append(fmt(byr)).append("</span>");
            sb.append("</div></div>");
        }
        sb.append("</div></div>");
        return sb.toString();
    }

    private void buildTabelRekap() {
        Div sect = new Div();
        sect.setSclass("dgk-sect");
        sect.setParent(divDasborContent);

        Div hdr = new Div();
        hdr.setStyle("display:flex;justify-content:space-between;align-items:flex-start;" +
            "flex-wrap:wrap;gap:8px;margin-bottom:12px;");
        hdr.setParent(sect);
        new Html(
            "<div><div class='dgk-sect-title'>Rekap per Tahun Masuk</div>" +
            "<div class='dgk-sect-desc'>Detail tagihan calon siswa per angkatan.</div></div>"
        ).setParent(hdr);

        Toolbarbutton btnXls = new Toolbarbutton("Download Excel");
        btnXls.setImage("/img/xls.gif");
        btnXls.setParent(hdr);
        btnXls.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                downloadExcel();
            }
        });

        Grid grid = new Grid();
        grid.setWidth("100%");
        grid.setSclass("dgrid");
        grid.setParent(sect);

        Columns cols = new Columns();
        cols.setParent(grid);
        String[] headers = {"Tahun", "Jml Calon", "Total Tagihan", "Sudah Dibayar", "Sisa Belum Lunas", "% Terbayar"};
        String[] widths  = {"68px", "80px", "150px", "140px", "155px", "90px"};
        for (int i = 0; i < headers.length; i++) {
            Column c = new Column(headers[i]);
            c.setWidth(widths[i]);
            c.setParent(cols);
        }

        Rows rows = new Rows();
        rows.setParent(grid);

        if (dataPerTahun == null || dataPerTahun.isEmpty()) {
            Row r = new Row();
            r.setSpans("6");
            r.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak ada data")));
            r.setParent(rows);
        } else {
            long totCln = 0L, totTag = 0L, totByr = 0L;
            for (Object[] rec : dataPerTahun) {
                int  thn  = rec[0] == null ? 0 : ((Number) rec[0]).intValue();
                long jml  = rec[1] == null ? 0L : ((Number) rec[1]).longValue();
                long tag  = rec[2] == null ? 0L : ((Number) rec[2]).longValue();
                long byr  = rec[3] == null ? 0L : ((Number) rec[3]).longValue();
                long sisa = Math.max(0L, tag - byr);
                int  pct  = tag > 0 ? (int) Math.round(byr * 100.0 / tag) : 0;
                String pctColor = pct >= 80 ? "#16a34a" : pct >= 50 ? "#f59e0b" : "#ef4444";
                totCln += jml; totTag += tag; totByr += byr;

                Row r = new Row();
                r.setParent(rows);
                r.appendChild(new Label(String.valueOf(thn)));
                r.appendChild(new Label(fmt(jml)));
                r.appendChild(new Label("Rp " + fmt(tag)));
                r.appendChild(new Label("Rp " + fmt(byr)));
                r.appendChild(new Label("Rp " + fmt(sisa)));
                r.appendChild(new Html(
                    "<div style='display:flex;align-items:center;gap:6px;'>" +
                    "<div style='flex:1;height:8px;background:#f1f5f9;border-radius:4px;overflow:hidden;'>" +
                    "<div style='height:100%;width:" + pct + "%;background:" + pctColor + ";border-radius:4px;'></div></div>" +
                    "<span style='font-size:11px;font-weight:700;color:" + pctColor + ";min-width:30px;text-align:right;'>" + pct + "%</span>" +
                    "</div>"
                ));
            }
            long totSisa = Math.max(0L, totTag - totByr);
            int  totPct  = totTag > 0 ? (int) Math.round(totByr * 100.0 / totTag) : 0;
            Row rFoot = new Row();
            rFoot.setStyle("font-weight:bold;background:#f8fafc;border-top:2px solid #e2e8f0;");
            rFoot.setParent(rows);
            rFoot.appendChild(new Label(ais.common.Common.getBahasaConfig("TOTAL")));
            rFoot.appendChild(new Label(fmt(totCln)));
            rFoot.appendChild(new Label("Rp " + fmt(totTag)));
            rFoot.appendChild(new Label("Rp " + fmt(totByr)));
            rFoot.appendChild(new Label("Rp " + fmt(totSisa)));
            rFoot.appendChild(new Label(totPct + "%"));
        }
    }

    // =========================================================================
    // Excel download
    // =========================================================================

    private void downloadExcel() {
        try {
            org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.ss.usermodel.Sheet sh = wb.createSheet("Rekap Keuangan Calon Siswa");

            String[] headers = {"Tahun Masuk", "Jml Calon", "Total Tagihan", "Sudah Dibayar",
                "Sisa Belum Lunas", "% Terbayar"};
            org.apache.poi.ss.usermodel.Row hRow = sh.createRow(0);
            for (int i = 0; i < headers.length; i++) hRow.createCell(i).setCellValue(headers[i]);

            int rowNum = 1;
            long totCln = 0L, totTag = 0L, totByr = 0L;
            if (dataPerTahun != null) {
                for (Object[] rec : dataPerTahun) {
                    int  thn  = rec[0] == null ? 0 : ((Number) rec[0]).intValue();
                    long jml  = rec[1] == null ? 0L : ((Number) rec[1]).longValue();
                    long tag  = rec[2] == null ? 0L : ((Number) rec[2]).longValue();
                    long byr  = rec[3] == null ? 0L : ((Number) rec[3]).longValue();
                    long sisa = Math.max(0L, tag - byr);
                    int  pct  = tag > 0 ? (int) Math.round(byr * 100.0 / tag) : 0;
                    totCln += jml; totTag += tag; totByr += byr;
                    org.apache.poi.ss.usermodel.Row row = sh.createRow(rowNum++);
                    row.createCell(0).setCellValue(thn);
                    row.createCell(1).setCellValue(jml);
                    row.createCell(2).setCellValue(tag);
                    row.createCell(3).setCellValue(byr);
                    row.createCell(4).setCellValue(sisa);
                    row.createCell(5).setCellValue(pct + "%");
                }
            }
            org.apache.poi.ss.usermodel.Row rTot = sh.createRow(rowNum);
            rTot.createCell(0).setCellValue("TOTAL");
            rTot.createCell(1).setCellValue(totCln);
            rTot.createCell(2).setCellValue(totTag);
            rTot.createCell(3).setCellValue(totByr);
            rTot.createCell(4).setCellValue(Math.max(0L, totTag - totByr));
            rTot.createCell(5).setCellValue(totTag > 0 ? (int) Math.round(totByr * 100.0 / totTag) + "%" : "0%");
            for (int i = 0; i < headers.length; i++) sh.autoSizeColumn(i);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            baos.close();

            Filedownload.save(
                new ByteArrayInputStream(baos.toByteArray()),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "Rekap_Keuangan_CalonSiswa.xlsx"
            );
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/DasborKeuanganSiswaAction.java:525");
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String fmt(long v) {
        try { return Common.numberFormat.get().format(v); }
        catch (Exception e) { return String.valueOf(v); }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String buildCss() {
        return "<style>" +
            ".dgk-header{padding:16px 16px 6px;}" +
            ".dgk-title{font-size:20px;font-weight:900;color:#1e293b;margin:0 0 4px;}" +
            ".dgk-subtitle{font-size:12px;color:#64748b;line-height:1.5;}" +
            ".dgk-filter-bar{display:flex;align-items:center;gap:10px;flex-wrap:wrap;" +
              "padding:10px 16px;background:#f8fafc;" +
              "border-top:1px solid #e2e8f0;border-bottom:1px solid #e2e8f0;margin-bottom:16px;}" +
            ".dgk-filter-info{font-size:11px;color:#64748b;padding:0 0 10px;}" +
            ".dgk-cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(185px,1fr));" +
              "gap:12px;margin-bottom:16px;}" +
            ".dgk-card{background:#fff;border:1px solid #e2e8f0;border-radius:12px;padding:16px;" +
              "box-shadow:0 2px 8px rgba(0,0,0,.04);}" +
            ".dgk-card-ico{width:42px;height:42px;border-radius:10px;display:flex;" +
              "align-items:center;justify-content:center;font-size:20px;margin-bottom:10px;}" +
            ".dgk-card-lbl{font-size:10px;text-transform:uppercase;letter-spacing:.5px;" +
              "color:#64748b;font-weight:700;margin-bottom:4px;}" +
            ".dgk-card-val{font-size:17px;font-weight:900;color:#1e293b;word-break:break-all;line-height:1.2;}" +
            ".dgk-card-sub{font-size:10px;color:#94a3b8;margin-top:5px;}" +
            ".dgk-2col{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:14px;}" +
            ".dgk-sect{background:#fff;border:1px solid #e2e8f0;border-radius:12px;padding:16px;" +
              "margin-bottom:14px;box-shadow:0 2px 8px rgba(0,0,0,.04);}" +
            ".dgk-sect:last-child{margin-bottom:0;}" +
            ".dgk-sect-title{font-size:14px;font-weight:800;color:#1e293b;margin:0 0 3px;}" +
            ".dgk-sect-desc{font-size:11px;color:#94a3b8;margin:0 0 10px;}" +
            "@media(max-width:640px){" +
              ".dgk-2col{grid-template-columns:1fr;}" +
              ".dgk-cards{grid-template-columns:1fr 1fr;}" +
            "}" +
            "</style>";
    }
}
