package ais.action.master.sekolah;

import java.util.Calendar;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Tabpanel;

import ais.action.master.pmb.statistik.DashboardPmbBase;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PerguruanTinggi;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyCombobox;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h1>DashboardSiswaBase — Kelas Dasar Dasbor Calon Siswa (PSB)</h1>
 *
 * <p>Kelas abstrak ini menyediakan pondasi bersama bagi seluruh dasbor di menu
 * Calon Siswa. Berbeda dengan {@link DashboardPmbBase} yang memfilter berdasarkan
 * Tahun Akademik (String) dan Semester, kelas ini menggunakan filter <b>Tahun Masuk
 * (Integer)</b> yang sesuai dengan field {@code CalonSiswa.tahunMasuk}.</p>
 *
 * <p>Alur kerja:</p>
 * <ol>
 *   <li>{@code build(container, defaultTA, defaultSem)} dipanggil saat tab pertama
 *       kali diklik. Membangun filter bar berisi combo Tahun Masuk + tombol Refresh,
 *       serta placeholder {@code contentHolder} dan {@code tableHolder}.</li>
 *   <li>{@code refresh()} membersihkan placeholder lalu membuka sesi Hibernate,
 *       memanggil {@code doRefreshSiswa(session, tahunMasuk)} pada subkelas.</li>
 *   <li>Subkelas mengimplementasikan {@code doRefreshSiswa} untuk mengisi konten
 *       HTML chart dan/atau ZK Grid sesuai dimensi analisis masing-masing.</li>
 * </ol>
 *
 * <p>Kelas ini meng-extend {@link DashboardPmbBase} agar seluruh metode utilitas
 * ({@code fmtAngka}, {@code escHtml}, {@code abbrev}, {@code fullWidth},
 * {@code grid2col}, {@code emptyCard}, {@code logErr}, dll.) dapat digunakan
 * langsung oleh subkelas tanpa duplikasi.</p>
 *
 * <p>Parameter {@code defaultTA} pada {@code build()} bisa berupa tahun integer
 * ("2025") atau tahun ajaran ("2025/2026"). Metode {@link #parseYear(String)} akan
 * mengekstrak angka tahun pertama secara otomatis.</p>
 *
 * @see DashboardPmbBase
 * @see DashboardStatistikSiswa
 * @see DashboardStatusSiswa
 * @see DashboardAsalSekolahSiswa
 * @see DashboardRegistrasiSiswa
 * @see DashboardHarianSiswa
 */
public abstract class DashboardSiswaBase extends DashboardPmbBase {

    protected DashboardSiswaBase(PerguruanTinggi pt) {
        super(pt);
    }

    // ═══════════════════════════════════════════════════════════════
    // build — buat filter bar + holder lalu panggil refresh
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void build(Tabpanel container, String defaultTA, String defaultSem) {
        while (container.getFirstChild() != null) { container.getFirstChild().detach(); }

        Div filterBar = new Div();
        filterBar.setStyle("display:flex;align-items:center;flex-wrap:wrap;gap:8px;"
                + "padding:10px 14px;background:#f0f4ff;border-radius:8px;margin-bottom:14px;");
        filterBar.setParent(container);

        Label lblTA = new Label(ais.common.Common.getBahasaConfig("Tahun Masuk:"));
        lblTA.setStyle("font-size:13px;font-weight:600;white-space:nowrap;");
        lblTA.setParent(filterBar);

        cboTA = new MyCombobox();
        cboTA.setWidth("110px");
        cboTA.setReadonly(true);
        cboTA.setParent(filterBar);

        int defYear  = parseYear(defaultTA);
        int curYear  = Calendar.getInstance().get(Calendar.YEAR);
        Comboitem toSelect = null;
        for (int y = curYear + 1; y >= curYear - 6; y--) {
            MyComboitemConfig ci = new MyComboitemConfig();
            ci.setLabel(String.valueOf(y));
            ci.setValue(Integer.valueOf(y));
            ci.setParent(cboTA);
            if (y == defYear) { toSelect = ci; }
        }
        if (toSelect != null) {
            cboTA.setSelectedItem(toSelect);
        } else {
            for (int i = 0; i < cboTA.getItemCount(); i++) {
                if (Integer.valueOf(curYear).equals(cboTA.getItemAtIndex(i).getValue())) {
                    cboTA.setSelectedItem(cboTA.getItemAtIndex(i));
                    break;
                }
            }
            if (cboTA.getSelectedItem() == null && cboTA.getItemCount() > 0) {
                cboTA.setSelectedItem(cboTA.getItemAtIndex(0));
            }
        }

        buildExtraFilter(filterBar);

        MyToolbarbuttonConfig btnRefresh = new MyToolbarbuttonConfig("Refresh", "/img/refresh.gif");
        btnRefresh.setParent(filterBar);
        btnRefresh.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception { refresh(); }
        });

        contentHolder = new Div();
        contentHolder.setWidth("100%");
        contentHolder.setParent(container);

        tableHolder = new Div();
        tableHolder.setWidth("100%");
        tableHolder.setStyle("margin-top:16px;");
        tableHolder.setParent(container);

        refresh();
    }

    // ═══════════════════════════════════════════════════════════════
    // refresh — bersihkan + buka session + panggil doRefreshSiswa
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void refresh() {
        clearDiv(contentHolder);
        clearDiv(tableHolder);
        int tahunMasuk = getSelectedTahunMasuk();
        Session s = null;
        try {
            s = HibernateUtil.openSession();
            doRefreshSiswa(s, tahunMasuk);
        } catch (Exception e) {
            logErr("DashboardSiswa.refresh tahunMasuk=" + tahunMasuk, e);
        } finally {
            if (s != null) { try { s.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/DashboardSiswaBase.java:146");} }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Bridge ke doRefresh DashboardPmbBase (tidak dipakai langsung)
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected final void doRefresh(Session session, String ta, String sem) {
        doRefreshSiswa(session, parseYear(ta));
    }

    /** Subkelas mengimplementasikan logika dasbor di sini. */
    protected abstract void doRefreshSiswa(Session session, int tahunMasuk);

    // ═══════════════════════════════════════════════════════════════
    // Utility methods
    // ═══════════════════════════════════════════════════════════════

    /** Tahun masuk yang dipilih di combo. */
    protected int getSelectedTahunMasuk() {
        if (cboTA != null && cboTA.getSelectedItem() != null) {
            Object v = cboTA.getSelectedItem().getValue();
            if (v instanceof Integer) { return (Integer) v; }
        }
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    /** Tambah kolom MyColumnConfig ke Columns. */
    protected void addCol(Columns cols, String label, String width) {
        MyColumnConfig col = new MyColumnConfig();
        col.setLabel(label);
        if (width != null) { col.setWidth(width); }
        col.setParent(cols);
    }

    /**
     * Parse tahun dari string. Mendukung format: "2025", "2025/2026", "2025-2026".
     * Ambil angka sebelum tanda pemisah.
     */
    protected static int parseYear(String s) {
        if (s == null || s.trim().isEmpty()) {
            return Calendar.getInstance().get(Calendar.YEAR);
        }
        String c = s.trim();
        int sep = -1;
        if (c.indexOf('/') >= 0) { sep = c.indexOf('/'); }
        else if (c.indexOf('-') >= 4) { sep = c.indexOf('-', 4); }
        if (sep > 0) { c = c.substring(0, sep); }
        try { return Integer.parseInt(c.trim()); }
        catch (NumberFormatException ex) { return Calendar.getInstance().get(Calendar.YEAR); }
    }

    private static void clearDiv(Div div) {
        if (div == null) { return; }
        while (div.getFirstChild() != null) { div.getFirstChild().detach(); }
    }
}
