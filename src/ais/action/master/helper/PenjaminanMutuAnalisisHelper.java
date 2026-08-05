package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonNotifikasi;
import ais.common.ErrorAuditUtil;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.Ambildata;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.ui.util.ZkCompat;

/**
 * <h3>PenjaminanMutuAnalisisHelper — Dasbor Penjaminan Mutu: Monitor Analisis Butir Soal</h3>
 *
 * <p>Kelas ini menyediakan dasbor terpusat bagi staf Bagian Penjaminan Mutu untuk:
 * memantau hasil Analisis Butir Soal di semua MK/dosen/periode, memberi catatan (feedback)
 * kepada dosen, serta menandai analisis sebagai "Disetujui" atau "Perlu Revisi". Setiap
 * perubahan status mengirimkan notifikasi ke dosen yang bersangkutan.</p>
 *
 * <p>Data status dan catatan disimpan di kolom {@code analisis_catatan_json} pada entitas
 * {@link PertemuanPunyaUjian} (JSON: status, catatan, reviewerId, reviewTime). Kolom ini
 * dikelola otomatis oleh Hibernate (hbm2ddl=update) sehingga tidak perlu DDL manual pada
 * tabel {@code pertemuan_punya_ujian}.</p>
 *
 * <p><b>Pola implementasi:</b> extends {@code Div}, di-render melalui timer ZK agar konstruktor
 * selesai sebelum query Hibernate berjalan (pola yang sama dengan {@code DasboardSPMI}).</p>
 *
 * <p><b>Cara pakai:</b> Instantiasi di Action/zscript, lalu setParent ke container:
 * {@code new PenjaminanMutuAnalisisHelper().setParent(container);}</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class PenjaminanMutuAnalisisHelper extends Div {

    private static final long serialVersionUID = 1L;

    // ── Status konstanta ─────────────────────────────────────────────────────
    public static final String STATUS_MENUNGGU     = "menunggu";
    public static final String STATUS_DISETUJUI    = "disetujui";
    public static final String STATUS_PERLU_REVISI = "perlu_revisi";

    // ── Filter state ─────────────────────────────────────────────────────────
    private String filterTa       = Common.getCurrentTahunAkademik();
    private String filterSemester = "";
    private String filterStatus   = "";

    // ── Filter UI (dibuat di renderFilter, dipakai di applyFilter) ───────────
    private Combobox cbTa;
    private Combobox cbSemester;
    private Combobox cbStatus;

    // ── Container grid (untuk refresh tanpa render ulang seluruh halaman) ────
    private Div divGrid;

    // =========================================================================
    // Konstruktor — entrypoint utama
    // =========================================================================

    public PenjaminanMutuAnalisisHelper() {
        setWidth("100%");
        setStyle("min-height:200px; background:#f8fafc; padding:12px 14px;"
               + " box-sizing:border-box; overflow:auto;");
        // Loading skeleton only — render() must be called by caller after setParent()
        // so that the component is attached to the ZK page before Hibernate queries run.
        try {
            tampilLoading();
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
            PesanFormalHelper.tampilkanGagalException(
                    "menampilkan halaman Analisis Penjaminan Mutu",
                    ex, new String[] {
                            "Muat ulang (refresh) halaman ini lalu coba kembali.",
                            "Periksa koneksi jaringan Anda ke server aplikasi.",
                            "Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
                    });
        }
    }

    // =========================================================================
    // Loading skeleton
    // =========================================================================

    private void tampilLoading() {
        Common.clear(this);
        appendHtml(this,
            "<div style='padding:60px 0;text-align:center;'>"
            + "<span style='font-size:36px;'>&#9203;</span>"
            + "<div style='margin-top:12px;font-size:14px;font-weight:700;color:#334155;'>"
            + "Memuat Dasbor Penjaminan Mutu&#8230;</div>"
            + "<div style='margin-top:6px;font-size:12px;color:#94a3b8;'>"
            + "Mengambil data analisis butir soal dari semua mata kuliah.</div>"
            + "</div>");
    }

    // =========================================================================
    // Render utama
    // =========================================================================

    public void render() {
        try {
            Common.clear(this);
            renderHeader();
            renderFilter();
            divGrid = new Div();
            divGrid.setWidth("100%");
            divGrid.setParent(this);
            renderDataGrid();
        } catch (Exception ex) {
            ErrorAuditUtil.record(ex, "PenjaminanMutuAnalisisHelper.render");
            Common.clear(this);
            appendHtml(this,
                "<div style='padding:20px;color:#dc2626;font-weight:700;'>"
                + "Error memuat dasbor. Periksa log server.</div>");
        }
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private void renderHeader() {
        appendHtml(this,
            "<div style='background:linear-gradient(135deg,#1e40af,#3b82f6);border-radius:12px;"
            + "padding:16px 20px;margin-bottom:12px;color:#fff;'>"
            + "<div style='font-size:18px;font-weight:900;letter-spacing:-.3px;'>"
            + "&#128269; Dasbor Penjaminan Mutu — Analisis Butir Soal</div>"
            + "<div style='font-size:12px;margin-top:4px;opacity:.85;'>"
            + "Pantau, beri catatan, dan setujui analisis butir soal dosen dari satu tempat.</div>"
            + "</div>");
    }

    // ── Filter bar ────────────────────────────────────────────────────────────

    private void renderFilter() {
        Div card = new Div();
        card.setStyle("background:#fff;border:1px solid #e2e8f0;border-radius:10px;"
                    + "padding:12px 16px;margin-bottom:12px;");
        card.setParent(this);

        Hbox hbox = new Hbox();
        hbox.setAlign("center");
        hbox.setSpacing("10px");
        hbox.setParent(card);

        appendHtml(hbox, "<span style='font-size:12px;font-weight:700;color:#475569;'>Filter:</span>");

        appendHtml(hbox, "<span style='font-size:11px;color:#64748b;'>Tahun Akademik</span>");
        cbTa = new Combobox();
        cbTa.setReadonly(true);
        cbTa.setWidth("115px");
        cbTa.setParent(hbox);
        isiComboTa();

        appendHtml(hbox, "<span style='font-size:11px;color:#64748b;'>Semester</span>");
        cbSemester = new Combobox();
        cbSemester.setReadonly(true);
        cbSemester.setWidth("105px");
        cbSemester.setParent(hbox);
        isiComboSemester();

        appendHtml(hbox, "<span style='font-size:11px;color:#64748b;'>Status</span>");
        cbStatus = new Combobox();
        cbStatus.setReadonly(true);
        cbStatus.setWidth("120px");
        cbStatus.setParent(hbox);
        isiComboStatus();

        MyToolbarbuttonConfig btnCari = new MyToolbarbuttonConfig("Cari", "/img/search.gif");
        btnCari.setParent(hbox);
        btnCari.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                applyFilter();
                Common.clear(divGrid);
                renderDataGrid();
            }
        });
    }

    private void isiComboTa() {
        Comboitem semua = new Comboitem("Semua TA");
        semua.setValue("");
        semua.setParent(cbTa);
        try {
            Session s = HibernateUtil.currentSession();
            List<String> tas = (List<String>) s.createSQLQuery(
                "SELECT DISTINCT pkl.tahun_ajaran "
                + "FROM public.pertemuan_punya_ujian ppu "
                + "JOIN public.pertemuan prt ON prt.id = ppu.pertemuan "
                + "JOIN public.perkuliahan pkl ON pkl.id = prt.perkuliahan "
                + "WHERE ppu.formatnilais IS NOT NULL "
                + "  AND ppu.formatnilais != '{}' "
                + "ORDER BY pkl.tahun_ajaran DESC LIMIT 12").list();
            for (String ta : tas) {
                if (ta == null || ta.trim().isEmpty()) continue;
                Comboitem ci = new Comboitem(ta);
                ci.setValue(ta);
                ci.setParent(cbTa);
                if (ta.equals(filterTa)) cbTa.setSelectedItem(ci);
            }
        } catch (Exception ex) {
            ErrorAuditUtil.record(ex, "PenjaminanMutuAnalisisHelper.isiComboTa");
        }
        if (cbTa.getSelectedItem() == null) cbTa.setSelectedItem(semua);
    }

    private void isiComboSemester() {
        String[] labels = { "Semua", "Ganjil", "Genap", "Semester Pendek" };
        String[] vals   = { "",      "Ganjil", "Genap", "Semester Pendek" };
        for (int i = 0; i < labels.length; i++) {
            Comboitem ci = new Comboitem(labels[i]);
            ci.setValue(vals[i]);
            ci.setParent(cbSemester);
            if (vals[i].equals(filterSemester)) cbSemester.setSelectedItem(ci);
        }
        if (cbSemester.getSelectedItem() == null && cbSemester.getItemCount() > 0)
            cbSemester.setSelectedItem(cbSemester.getItemAtIndex(0));
    }

    private void isiComboStatus() {
        String[] labels = { "Semua Status", "Menunggu Review", "Disetujui", "Perlu Revisi" };
        String[] vals   = { "",             STATUS_MENUNGGU,   STATUS_DISETUJUI, STATUS_PERLU_REVISI };
        for (int i = 0; i < labels.length; i++) {
            Comboitem ci = new Comboitem(labels[i]);
            ci.setValue(vals[i]);
            ci.setParent(cbStatus);
            if (vals[i].equals(filterStatus)) cbStatus.setSelectedItem(ci);
        }
        if (cbStatus.getSelectedItem() == null && cbStatus.getItemCount() > 0)
            cbStatus.setSelectedItem(cbStatus.getItemAtIndex(0));
    }

    private void applyFilter() {
        if (cbTa != null && cbTa.getSelectedItem() != null)
            filterTa = (String) cbTa.getSelectedItem().getValue();
        if (cbSemester != null && cbSemester.getSelectedItem() != null)
            filterSemester = (String) cbSemester.getSelectedItem().getValue();
        if (cbStatus != null && cbStatus.getSelectedItem() != null)
            filterStatus = (String) cbStatus.getSelectedItem().getValue();
    }

    // =========================================================================
    // Data Grid
    // =========================================================================

    private void renderDataGrid() {
        List<PertemuanPunyaUjian> list = loadData();

        // Filter status in-memory (karena status tersimpan di JSON, bukan kolom DB terindeks)
        List<PertemuanPunyaUjian> filtered = new ArrayList<PertemuanPunyaUjian>();
        for (PertemuanPunyaUjian ppu : list) {
            String st = getStatus(ppu);
            if (filterStatus == null || filterStatus.isEmpty() || filterStatus.equals(st))
                filtered.add(ppu);
        }

        if (filtered.isEmpty()) {
            appendHtml(divGrid,
                "<div style='padding:40px;text-align:center;color:#94a3b8;font-size:13px;'>"
                + "Tidak ada ujian dengan Analisis Butir Soal untuk filter yang dipilih.<br>"
                + "<span style='font-size:11px;'>Pastikan dosen sudah menjalankan Analisis Butir Soal "
                + "pada ujian yang dikonfigurasi OBE (ada format nilai).</span></div>");
            return;
        }

        // Jumlah per status untuk summary
        int jmlMenunggu = 0, jmlSetujui = 0, jmlRevisi = 0;
        for (PertemuanPunyaUjian ppu : filtered) {
            String s = getStatus(ppu);
            if (STATUS_DISETUJUI.equals(s)) jmlSetujui++;
            else if (STATUS_PERLU_REVISI.equals(s)) jmlRevisi++;
            else jmlMenunggu++;
        }
        appendHtml(divGrid,
            "<div style='display:flex;gap:8px;flex-wrap:wrap;margin-bottom:10px;'>"
            + buildSummaryCard(jmlMenunggu,  "Menunggu Review", "#475569", "#f1f5f9")
            + buildSummaryCard(jmlSetujui,   "Disetujui",        "#166534", "#dcfce7")
            + buildSummaryCard(jmlRevisi,    "Perlu Revisi",     "#991b1b", "#fee2e2")
            + buildSummaryCard(filtered.size(), "Total Ujian",   "#1e40af", "#eff6ff")
            + "</div>");

        // Grid tabel
        Grid grid = new Grid();
        grid.setWidth("100%");
        grid.setSclass("fgrid");
        grid.setStyle("background:#fff;border:1px solid #e2e8f0;border-radius:10px;overflow:hidden;");
        grid.setParent(divGrid);

        Columns cols = new Columns();
        cols.setParent(grid);
        cols.setSizable(true);
        String[] headers = { "Mata Kuliah", "Dosen", "Pertemuan", "Nama Ujian", "TA / Semester", "Jml Soal", "Status", "Aksi" };
        String[] widths  = { "19%",         "14%",   "7%",        "16%",        "13%",           "6%",       "10%",   "15%" };
        for (int i = 0; i < headers.length; i++) {
            Column col = new Column(headers[i]);
            col.setWidth(widths[i]);
            col.setParent(cols);
        }

        Rows rows = new Rows();
        rows.setParent(grid);

        for (final PertemuanPunyaUjian ppu : filtered) {
            Row row = new Row();
            row.setParent(rows);

            // ── Resolusi info dari relasi ──────────────────────────────────
            String mkNama = "";
            String dosenNama = "";
            String taStr = "";
            String prtKe = "";
            try {
                Pertemuan prt = ppu.getPertemuan();
                if (prt != null) {
                    prtKe = prt.getPertemuanKe() != null
                           ? String.valueOf(prt.getPertemuanKe()) : "-";
                    Perkuliahan pkl = prt.getPerkuliahan();
                    if (pkl != null) {
                        if (pkl.getMatakuliah() != null)
                            mkNama = pkl.getMatakuliah().getNama() != null
                                   ? pkl.getMatakuliah().getNama() : "";
                        taStr = (pkl.getTahunAjaran() != null ? pkl.getTahunAjaran() : "")
                              + (pkl.getGanjilGenap() != null && !pkl.getGanjilGenap().trim().isEmpty()
                                ? " / " + pkl.getGanjilGenap() : "");
                        if (pkl.getDosen1() != null)
                            dosenNama = pkl.getDosen1().getNama() != null
                                       ? pkl.getDosen1().getNama() : "";
                    }
                }
            } catch (Exception ex) {
                ErrorAuditUtil.record(ex, "PenjaminanMutuAnalisisHelper.grid.resolveRow");
            }

            new Label(mkNama).setParent(row);
            new Label(dosenNama).setParent(row);
            new Label("ke-" + prtKe).setParent(row);
            new Label(ppu.getNama() != null ? ppu.getNama() : "").setParent(row);
            new Label(taStr).setParent(row);
            new Label(String.valueOf(ppu.getJmlDitampilkan())).setParent(row);

            // Status badge
            appendHtmlCell(row, buildStatusBadge(getStatus(ppu)));

            // Tombol aksi
            Hbox aksi = new Hbox();
            aksi.setSpacing("3px");
            aksi.setParent(row);

            final PertemuanPunyaUjian ppuRef = ppu;

            // Tombol Detail (buka popup full review)
            MyToolbarbuttonConfig btnDetail = new MyToolbarbuttonConfig("Detail", "/img/edit.gif");
            btnDetail.setTooltiptext("Buka detail analisis & beri catatan");
            btnDetail.setParent(aksi);
            btnDetail.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    bukaDetailPopup(ppuRef);
                }
            });

            // Tombol cepat "Setujui"
            MyToolbarbuttonConfig btnSetujui = new MyToolbarbuttonConfig("Setujui", "/img/check.gif");
            btnSetujui.setTooltiptext("Setujui langsung tanpa catatan");
            btnSetujui.setParent(aksi);
            btnSetujui.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    simpanStatus(ppuRef, STATUS_DISETUJUI, "", Common.getCurrentUser());
                    Common.clear(divGrid);
                    renderDataGrid();
                    MyMessageboxConfig.show("Analisis berhasil disetujui. Notifikasi dikirim ke dosen.",
                        "Sukses", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                }
            });
        }
    }

    // =========================================================================
    // Detail Popup
    // =========================================================================

    private void bukaDetailPopup(final PertemuanPunyaUjian ppu) throws Exception {
        String mkNama = "";
        try {
            if (ppu.getPertemuan() != null
                    && ppu.getPertemuan().getPerkuliahan() != null
                    && ppu.getPertemuan().getPerkuliahan().getMatakuliah() != null) {
                mkNama = ppu.getPertemuan().getPerkuliahan().getMatakuliah().getNama();
                if (mkNama == null) mkNama = "";
            }
        } catch (Exception ex) { /* abaikan */ }

        final String judul = "Review Analisis — " + (ppu.getNama() != null ? ppu.getNama() : "")
                           + (mkNama.isEmpty() ? "" : " | " + mkNama);
        final MyWindow win = new MyWindow(judul, "none", true);
        win.setWidth("880px");
        win.setHeight("88%");
        Component root = ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();
        win.setParent(root);

        Borderlayout bl = new MyBorderlayout();
        bl.setParent(win);

        // ── NORTH: toolbar ────────────────────────────────────────────────────
        North north = new North();
        north.setParent(bl);
        Toolbar toolbar = new Toolbar();
        toolbar.setParent(north);

        MyToolbarbuttonConfig btnTutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
        btnTutup.setParent(toolbar);
        btnTutup.addEventListener("onClick", new EventListener() {
            @Override public void onEvent(Event e) throws Exception { win.detach(); }
        });

        // Tombol buka Analisis Butir Soal asli (dari HasilUjianMahasiswaHelper)
        try {
            Toolbarbutton btnAnalisis = HasilUjianMahasiswaHelper.analsisButirSoal(ppu,
                new Ambildata() {
                    @Override public Object ambil() { return null; }
                });
            if (btnAnalisis != null) btnAnalisis.setParent(toolbar);
        } catch (Exception ex) {
            ErrorAuditUtil.record(ex, "PenjaminanMutuAnalisisHelper.bukaDetailPopup.analisisBtn");
        }

        // ── CENTER: isi review ────────────────────────────────────────────────
        Center center = new Center();
        ZkCompat.setFlex(center, true);
        center.setStyle("overflow:auto;");
        center.setParent(bl);

        Vbox vbox = new Vbox();
        vbox.setWidth("100%");
        vbox.setStyle("padding:16px;");
        vbox.setParent(center);

        // Ringkasan info ujian
        String status  = getStatus(ppu);
        String catatan = getCatatan(ppu);

        appendHtml(vbox,
            "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;"
            + "padding:12px 16px;margin-bottom:14px;font-size:13px;line-height:1.7;'>"
            + "<b>Ujian:</b> " + esc(ppu.getNama() != null ? ppu.getNama() : "") + "<br>"
            + "<b>Mata Kuliah:</b> " + esc(mkNama) + "<br>"
            + "<b>Jumlah Soal:</b> " + ppu.getJmlDitampilkan() + "<br>"
            + "<b>Status saat ini:</b> " + buildStatusBadge(status)
            + "</div>");

        appendHtml(vbox,
            "<div style='font-weight:700;font-size:13px;color:#1e293b;margin-bottom:6px;'>"
            + "Catatan / Feedback untuk Dosen:</div>");

        final Textbox txCatatan = new Textbox(catatan);
        txCatatan.setMultiline(true);
        txCatatan.setRows(7);
        txCatatan.setWidth("100%");
        txCatatan.setStyle("font-size:13px;");
        txCatatan.setParent(vbox);

        appendHtml(vbox,
            "<div style='font-size:11px;color:#94a3b8;margin-top:4px;'>"
            + "Catatan ini akan dikirimkan sebagai notifikasi ke dosen pengampu.</div>");

        // ── SOUTH: tombol keputusan ───────────────────────────────────────────
        South south = new South();
        south.setParent(bl);
        Toolbar tbSouth = new Toolbar();
        tbSouth.setParent(south);

        final PertemuanPunyaUjian ppu2 = ppu;

        MyToolbarbuttonConfig btnSetujui = new MyToolbarbuttonConfig(
            "&#10003; Setujui Analisis", "/img/check.gif");
        btnSetujui.setStyle("color:#166534;font-weight:700;");
        btnSetujui.setTooltiptext("Tandai analisis ini sebagai Disetujui dan kirim notifikasi");
        btnSetujui.setParent(tbSouth);
        btnSetujui.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                Tbmuser reviewer = Common.getCurrentUser();
                simpanStatus(ppu2, STATUS_DISETUJUI, txCatatan.getValue(), reviewer);
                win.detach();
                Common.clear(divGrid);
                renderDataGrid();
                MyMessageboxConfig.show(
                    "Analisis berhasil disetujui. Notifikasi telah dikirim ke dosen.",
                    "Disetujui", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            }
        });

        MyToolbarbuttonConfig btnRevisi = new MyToolbarbuttonConfig(
            "&#9888; Perlu Revisi", "/img/warning.gif");
        btnRevisi.setStyle("color:#991b1b;font-weight:700;");
        btnRevisi.setTooltiptext("Tandai analisis Perlu Revisi — catatan wajib diisi");
        btnRevisi.setParent(tbSouth);
        btnRevisi.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                String cat = txCatatan.getValue();
                if (cat == null || cat.trim().isEmpty()) {
                    MyMessageboxConfig.show(
                        "Harap isi Catatan/Feedback sebelum menandai 'Perlu Revisi'.",
                        "Perhatian", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                    return;
                }
                Tbmuser reviewer = Common.getCurrentUser();
                simpanStatus(ppu2, STATUS_PERLU_REVISI, cat, reviewer);
                win.detach();
                Common.clear(divGrid);
                renderDataGrid();
                MyMessageboxConfig.show(
                    "Analisis ditandai Perlu Revisi. Notifikasi dan catatan telah dikirim ke dosen.",
                    "Perlu Revisi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            }
        });
    }

    // =========================================================================
    // Data loading
    // =========================================================================

    private List<PertemuanPunyaUjian> loadData() {
        try {
            Session s = HibernateUtil.currentSession();
            Criteria crit = s.createCriteria(PertemuanPunyaUjian.class, "ppu");
            crit.createAlias("ppu.pertemuan", "prt");
            crit.createAlias("prt.perkuliahan", "pkl");
            crit.add(Restrictions.isNotNull("ppu.formatNilais"));
            crit.add(Restrictions.ne("ppu.formatNilais", Tugas.JSON));
            if (filterTa != null && !filterTa.trim().isEmpty())
                crit.add(Restrictions.eq("pkl.tahunAjaran", filterTa));
            if (filterSemester != null && !filterSemester.trim().isEmpty())
                crit.add(Restrictions.ilike("pkl.ganjilGenap", filterSemester));
            crit.addOrder(Order.desc("ppu.id"));
            crit.setMaxResults(250);
            return (List<PertemuanPunyaUjian>) crit.list();
        } catch (Exception e) {
            ErrorAuditUtil.record(e, "PenjaminanMutuAnalisisHelper.loadData");
            return new ArrayList<PertemuanPunyaUjian>();
        }
    }

    // =========================================================================
    // Status helpers
    // =========================================================================

    /** Ambil status dari JSON. Default: {@code menunggu}. */
    public static String getStatus(PertemuanPunyaUjian ppu) {
        try {
            String json = ppu.getAnalisisCatatanJson();
            if (json == null || json.trim().isEmpty()) return STATUS_MENUNGGU;
            return new JSONObject(json).optString("status", STATUS_MENUNGGU);
        } catch (Exception e) {
            return STATUS_MENUNGGU;
        }
    }

    private static String getCatatan(PertemuanPunyaUjian ppu) {
        try {
            String json = ppu.getAnalisisCatatanJson();
            if (json == null || json.trim().isEmpty()) return "";
            return new JSONObject(json).optString("catatan", "");
        } catch (Exception e) {
            return "";
        }
    }

    private static String buildStatusBadge(String status) {
        String color, bg, label;
        if (STATUS_DISETUJUI.equals(status)) {
            color = "#166534"; bg = "#dcfce7"; label = "&#10003; Disetujui";
        } else if (STATUS_PERLU_REVISI.equals(status)) {
            color = "#991b1b"; bg = "#fee2e2"; label = "&#9888; Perlu Revisi";
        } else {
            color = "#475569"; bg = "#f1f5f9"; label = "&#9679; Menunggu";
        }
        return "<span style='display:inline-block;padding:3px 10px;border-radius:12px;"
             + "font-size:11px;font-weight:700;background:" + bg + ";color:" + color + ";'>"
             + label + "</span>";
    }

    private static String buildSummaryCard(int count, String label, String color, String bg) {
        return "<div style='flex:1 1 100px;min-width:90px;border-radius:8px;padding:10px 14px;"
             + "background:" + bg + ";text-align:center;'>"
             + "<div style='font-size:22px;font-weight:900;color:" + color + ";'>" + count + "</div>"
             + "<div style='font-size:11px;color:" + color + ";opacity:.8;margin-top:2px;'>" + label + "</div>"
             + "</div>";
    }

    // =========================================================================
    // Simpan status ke DB
    // =========================================================================

    private void simpanStatus(PertemuanPunyaUjian ppu, String status,
            String catatan, Tbmuser reviewer) {
        Session session = null;
        Transaction tx  = null;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.beginTransaction();
            PertemuanPunyaUjian managed =
                (PertemuanPunyaUjian) session.get(PertemuanPunyaUjian.class, ppu.getId());
            if (managed == null) {
                if (tx != null) tx.rollback();
                return;
            }
            JSONObject json = new JSONObject();
            json.put("status", status);
            json.put("catatan", catatan != null ? catatan : "");
            if (reviewer != null && reviewer.getId() != null)
                json.put("reviewerId", reviewer.getId());
            json.put("reviewTime", WaktuUtil.getDate().toString());
            managed.setAnalisisCatatanJson(json.toString());
            session.update(managed);
            tx.commit();
            tx = null;
            // Sinkronkan objek in-memory agar UI refresh tanpa query ulang
            ppu.setAnalisisCatatanJson(json.toString());
            // Kirim notifikasi ke dosen
            kirimNotifikasi(ppu, status, catatan != null ? catatan : "");
        } catch (Exception e) {
            if (tx != null) try { tx.rollback(); } catch (Exception ig) {
                ErrorAuditUtil.record(ig, "PenjaminanMutuAnalisisHelper.simpanStatus.rollback");
            }
            ErrorAuditUtil.record(e, "PenjaminanMutuAnalisisHelper.simpanStatus");
            Common.tampilErrorJikaAdmin(e);
            PesanFormalHelper.tampilkanGagalException(
                    "menyimpan status hasil analisis penjaminan mutu (" + status + ")",
                    e, new String[] {
                            "Muat ulang (refresh) halaman ini lalu coba simpan kembali.",
                            "Pastikan data pertemuan/ujian terkait belum dihapus oleh pengguna lain.",
                            "Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
                    });
        } finally {
            tutupSesi(session);
        }
    }

    // =========================================================================
    // Kirim notifikasi ke dosen
    // =========================================================================

    private void kirimNotifikasi(final PertemuanPunyaUjian ppu,
            final String status, final String catatan) {
        Common.createDefaultTimer(new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                Session session = null;
                try {
                    session = HibernateUtil.currentNativeSession();
                    JSONArray userIds = new JSONArray();
                    StringBuilder emails = new StringBuilder();

                    // Resolusi dosen dari Perkuliahan
                    String mkNama = "";
                    try {
                        if (ppu.getPertemuan() != null
                                && ppu.getPertemuan().getPerkuliahan() != null) {
                            Long pklId = ppu.getPertemuan().getPerkuliahan().getId();
                            Perkuliahan pkl = (Perkuliahan) session.get(Perkuliahan.class, pklId);
                            if (pkl != null) {
                                if (pkl.getMatakuliah() != null
                                        && pkl.getMatakuliah().getNama() != null)
                                    mkNama = pkl.getMatakuliah().getNama();
                                // Kumpulkan email dosen 1-3
                                Dosen[] dosens = { pkl.getDosen1(), pkl.getDosen2(), pkl.getDosen3() };
                                for (Dosen d : dosens) {
                                    if (d == null) continue;
                                    if (d.getEmail() != null && !d.getEmail().trim().isEmpty()) {
                                        if (emails.length() > 0) emails.append(",");
                                        emails.append(d.getEmail().trim());
                                    }
                                }
                                // Cari userId dosen di tbmuser
                                if (pkl.getDosen1() != null) {
                                    List<String> ids = (List<String>) session.createQuery(
                                        "SELECT u.userId FROM Tbmuser u "
                                        + "WHERE u.dosen = :d AND (u.aktif IS NULL OR u.aktif = true)")
                                        .setParameter("d", pkl.getDosen1()).list();
                                    for (String uid : ids) {
                                        if (uid != null && !uid.trim().isEmpty())
                                            userIds.put(uid);
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        ErrorAuditUtil.record(ex, "PenjaminanMutuAnalisisHelper.kirimNotifikasi.resolveUser");
                    }

                    if (userIds.length() == 0 && emails.length() == 0) return;

                    String statusLabel = STATUS_DISETUJUI.equals(status) ? "Disetujui"
                            : (STATUS_PERLU_REVISI.equals(status) ? "Perlu Revisi" : "Menunggu");
                    String namaUjian = ppu.getNama() != null ? ppu.getNama() : "";
                    String subject = "[Penjaminan Mutu] Analisis Butir Soal " + namaUjian
                                   + " — " + statusLabel;
                    String body = "Analisis Butir Soal ujian <b>" + esc(namaUjian) + "</b>"
                        + (mkNama.isEmpty() ? "" : " pada mata kuliah <b>" + esc(mkNama) + "</b>")
                        + " telah ditinjau oleh <b>Bagian Penjaminan Mutu</b>."
                        + "<br><br><b>Status:</b> " + statusLabel
                        + (catatan != null && !catatan.trim().isEmpty()
                           ? "<br><br><b>Catatan/Feedback:</b><br><blockquote style='border-left:3px solid #e2e8f0;"
                             + "padding-left:10px;color:#475569;'>" + esc(catatan) + "</blockquote>" : "")
                        + "<br>Silakan periksa dan perbaiki analisis sesuai arahan Penjaminan Mutu.";

                    String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
                    MailSender.simpanNotifikasiHalaman(
                        userIds,
                        emails.length() > 0 ? emails.toString() : null,
                        subject, body, sender, ppu,
                        null, null,
                        CommonNotifikasi.STATUS_INFO,
                        true, false, false);

                } catch (Exception ex) {
                    ErrorAuditUtil.record(ex, "PenjaminanMutuAnalisisHelper.kirimNotifikasi");
                } finally {
                    tutupSesi(session);
                }
            }
        });
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    private static void tutupSesi(Session session) {
        try {
            if (session != null && session.isOpen()) {
                try { session.disconnect(); } catch (Exception ig) {
                    ErrorAuditUtil.record(ig, "PenjaminanMutuAnalisisHelper.tutupSesi.disconnect");
                }
                try { session.close(); } catch (Exception ig) {
                    ErrorAuditUtil.record(ig, "PenjaminanMutuAnalisisHelper.tutupSesi.close");
                }
            }
        } catch (Exception e) {
            ErrorAuditUtil.record(e, "PenjaminanMutuAnalisisHelper.tutupSesi");
        }
        HibernateUtil.closeSession();
    }

    private static void appendHtml(Component parent, String html) {
        new org.zkoss.zul.Html(html).setParent(parent);
    }

    /** Tambahkan HTML dalam sel Div di dalam Row — wungkus agar setParent bekerja. */
    private static void appendHtmlCell(Row row, String html) {
        Div cell = new Div();
        cell.setParent(row);
        new org.zkoss.zul.Html(html).setParent(cell);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
