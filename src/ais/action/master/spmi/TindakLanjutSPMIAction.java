package ais.action.master.spmi;

import java.util.Date;
import java.util.List;

import org.json.JSONObject;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.KonfigurasiManager;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.spmi.HasilTemuanSPMI;
import ais.database.model.spmi.TindakLanjutTemuanSPMI;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * CRUD tindak lanjut temuan SPMI (fase Pengendalian dalam siklus PPEPP).
 * Dapat dipanggil langsung dari baris temuan di HasilSPMIAction.
 */
public class TindakLanjutSPMIAction extends BaseSPMIAction {

    private static final long serialVersionUID = 1L;

    // ---- form fields ----
    private Textbox   deskripsi;
    private Textbox   picNama;
    private MyDatebox targetDate;
    private MyDatebox tanggalSelesai;
    private MyIntbox  progressPersen;
    private Combobox  statusTl;
    private Textbox   keterangan;

    // ---- state ----
    private TindakLanjutTemuanSPMI current;
    private HasilTemuanSPMI        parentTemuan;

    // =========================================================
    // Static entry point — opens popup from HasilSPMIAction row
    // =========================================================

    /**
     * Buka popup tindak lanjut untuk satu temuan.
     * Dipanggil dari baris temuan di HasilSPMIAction.
     */
    public static void openForTemuan(HasilTemuanSPMI temuan, Component anchor) throws Exception {
        // Use hasilTemuan text as title; fall back to skenario name if empty
        String judul = (temuan.getNama() != null && !temuan.getNama().trim().isEmpty())
                ? abbreviate(temuan.getNama(), 50)
                : (temuan.getSkenarioSPMI() != null ? abbreviate(temuan.getSkenarioSPMI().getNama(), 50) : "");
        MyWindow win = new MyWindow();
        win.setTitle("Tindak Lanjut Temuan — " + judul);
        win.setWidth("700px");
        win.setHeight("88%");
        win.setClosable(true);
        win.setBorder("normal");
        win.setMode("popup");
        win.setPosition("center,center");
        win.setParent(anchor.getPage().getFirstRoot());

        buildTindakLanjutPanel(temuan, win);
        win.setVisible(true);
        win.onModal();
    }

    /**
     * Builds an inline panel (non-modal) for embedding inside another ZUL.
     */
    public static Div buildInlinePanel(HasilTemuanSPMI temuan) throws Exception {
        Div panel = new Div();
        panel.setStyle("width:100%; box-sizing:border-box;");
        buildTindakLanjutPanel(temuan, panel);
        return panel;
    }

    @SuppressWarnings("unchecked")
    private static void buildTindakLanjutPanel(HasilTemuanSPMI temuan, Component container) throws Exception {
        Common.clear(container);

        Session sess = HibernateUtil.currentSession();
        List<TindakLanjutTemuanSPMI> list = sess.createCriteria(TindakLanjutTemuanSPMI.class)
                .add(Restrictions.eq("hasilTemuanSPMI", temuan))
                .addOrder(Order.asc("id"))
                .list();

        String skenarioNama = temuan.getSkenarioSPMI() != null ? temuan.getSkenarioSPMI().getNama() : "";
        String hasilTemuan  = temuan.getNama() != null && !temuan.getNama().trim().isEmpty()
                ? temuan.getNama() : "(belum diisi)";
        appendHtml(container,
            "<div style='font-size:11px; color:#64748b; padding:10px 14px 6px; line-height:1.55;"
            + " background:#f8fafc; border-radius:8px; margin:6px 14px;'>"
            + "<b>Daftar Tilik/Skenario:</b> " + esc(skenarioNama)
            + "<br><b>Hasil Temuan Audit:</b> " + esc(hasilTemuan)
            + "<br><b>Status Temuan:</b> " + statusBadge(temuan.getStatus())
            + "</div>");

        // Tabel tindak lanjut yang sudah ada
        if (!list.isEmpty()) {
            appendHtml(container,
                "<div style='padding:0 14px 8px;'>"
                + "<div style='font-size:12px; font-weight:700; color:#0f172a; margin-bottom:6px;'>"
                + "Daftar Tindak Lanjut</div>"
                + "<table style='width:100%; border-collapse:collapse; font-size:11px;'>"
                + "<tr style='background:#f8fafc;'>"
                + "<th style='padding:6px 8px; text-align:left; border-bottom:2px solid #e2e8f0;'>Deskripsi</th>"
                + "<th style='padding:6px 8px; text-align:left; border-bottom:2px solid #e2e8f0;'>PIC</th>"
                + "<th style='padding:6px 8px; text-align:left; border-bottom:2px solid #e2e8f0;'>Target</th>"
                + "<th style='padding:6px 8px; text-align:left; border-bottom:2px solid #e2e8f0;'>Progress</th>"
                + "<th style='padding:6px 8px; text-align:left; border-bottom:2px solid #e2e8f0;'>Status</th>"
                + "</tr>"
                + buildTlRows(list)
                + "</table></div>");
        } else {
            appendHtml(container,
                "<div style='padding:12px 14px; color:#94a3b8; font-size:12px;'>"
                + "Belum ada tindak lanjut yang dicatat untuk temuan ini.</div>");
        }

        // Form tambah tindak lanjut baru
        appendHtml(container,
            "<div style='padding:8px 14px 4px; font-size:12px; font-weight:700; color:#0f172a;"
            + " border-top:1px solid #f1f5f9;'>Tambah Tindak Lanjut Baru</div>");

        buildAddForm(temuan, container, list, sess);

        // ── Rapat Tinjauan Manajemen (RTM) — Fase Pengendalian PPEPP ─────────
        if (temuan.getHasilSPMI() != null) {
            buildRtmSection(temuan.getHasilSPMI().getId(), container);
        }
    }

    private static final String RTM_PREFIX = "spmi_rtm_";

    private static void buildRtmSection(final Long hasilSPMIId, final Component container) {
        appendHtml(container,
            "<div style='margin-top:12px;padding:8px 14px 4px;font-size:12px;font-weight:700;"
            + "color:#6b21a8;border-top:2px solid #e9d5ff;background:#faf5ff;border-radius:4px 4px 0 0;'>"
            + "&#9997; Rapat Tinjauan Manajemen (RTM) — Pengendalian PPEPP</div>");

        // Load existing RTM data
        JSONObject rtmData = new JSONObject();
        try {
            ais.database.model.Konfigurasi k = Common.getKonfigurasi(RTM_PREFIX + hasilSPMIId, "{}");
            if (k != null && k.getNilai() != null && !k.getNilai().trim().isEmpty()
                    && !k.getNilai().trim().equals("{}")) {
                rtmData = new JSONObject(k.getNilai());
            }
        } catch (Exception ign) { ais.common.ErrorAuditUtil.record(ign, "auto-audit(empty-catch) src/ais/action/master/spmi/TindakLanjutSPMIAction.java:167");}

        final String existTanggal   = rtmData.optString("tanggal", "");
        final String existPeserta   = rtmData.optString("peserta", "");
        final String existAgenda    = rtmData.optString("agenda", "");
        final String existKesimpulan = rtmData.optString("kesimpulan", "");
        final String existKeputusan  = rtmData.optString("keputusan", "");

        if (!rtmData.has("tanggal") || existTanggal.isEmpty()) {
            appendHtml(container,
                "<div style='padding:8px 14px;font-size:11px;color:#7c3aed;background:#faf5ff;"
                + "font-style:italic;'>Belum ada catatan RTM untuk AMI ini. Isi form di bawah jika RTM sudah dilaksanakan.</div>");
        } else {
            appendHtml(container,
                "<div style='padding:8px 14px;background:#faf5ff;font-size:11px;'>"
                + "<b>Tanggal RTM:</b> " + esc(existTanggal)
                + " &nbsp;|&nbsp; <b>Peserta:</b> " + esc(existPeserta)
                + "<br><b>Agenda:</b> " + esc(existAgenda)
                + "<br><b>Kesimpulan:</b> " + esc(existKesimpulan)
                + "<br><b>Keputusan Tindak Lanjut:</b> " + esc(existKeputusan)
                + "</div>");
        }

        // Collapsible edit form
        appendHtml(container,
            "<div style='padding:6px 14px 4px;font-size:11px;font-weight:700;color:#6b21a8;"
            + "background:#faf5ff;'>Edit / Catat RTM</div>");

        org.zkoss.zul.Grid rtmGrid = new org.zkoss.zul.Grid();
        rtmGrid.setWidth("100%");
        rtmGrid.setSclass("dgrid");
        org.zkoss.zul.Columns rtmCols = new org.zkoss.zul.Columns();
        org.zkoss.zul.Column rc1 = new org.zkoss.zul.Column(); rc1.setWidth("140px"); rc1.setParent(rtmCols);
        org.zkoss.zul.Column rc2 = new org.zkoss.zul.Column(); rc2.setParent(rtmCols);
        rtmCols.setParent(rtmGrid);
        Rows rtmRows = new Rows(); rtmRows.setParent(rtmGrid);

        final Textbox tbRtmTanggal = addRtmRow(rtmRows, "Tanggal RTM", existTanggal, false);
        final Textbox tbRtmPeserta = addRtmRow(rtmRows, "Peserta RTM", existPeserta, false);
        final Textbox tbRtmAgenda  = addRtmRow(rtmRows, "Agenda", existAgenda, true);
        final Textbox tbRtmKesimpulan = addRtmRow(rtmRows, "Kesimpulan RTM", existKesimpulan, true);
        final Textbox tbRtmKeputusan  = addRtmRow(rtmRows, "Keputusan / Tindak Lanjut", existKeputusan, true);

        Div rtmContainer = new Div();
        rtmContainer.setStyle("background:#faf5ff;padding:0 14px 10px;border-radius:0 0 4px 4px;");
        rtmContainer.setParent(container);
        rtmGrid.setParent(rtmContainer);

        org.zkoss.zul.Button btnRtm = new org.zkoss.zul.Button("Simpan Catatan RTM");
        btnRtm.setStyle("margin-top:8px;background:#7c3aed;color:#fff;border:none;");
        btnRtm.addEventListener("onClick", new EventListener() {
            public void onEvent(Event event) throws Exception {
                try {
                    JSONObject save = new JSONObject();
                    save.put("tanggal",    tbRtmTanggal.getValue().trim());
                    save.put("peserta",    tbRtmPeserta.getValue().trim());
                    save.put("agenda",     tbRtmAgenda.getValue().trim());
                    save.put("kesimpulan", tbRtmKesimpulan.getValue().trim());
                    save.put("keputusan",  tbRtmKeputusan.getValue().trim());
                    KonfigurasiManager.simpanKonfigurasi(RTM_PREFIX + hasilSPMIId, save.toString());
                    MyMessageboxConfig.show("Catatan RTM berhasil disimpan",
                        "Info", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                } catch (Exception ex) {
                    Common.tampilErrorJikaAdmin(ex);
                }
            }
        });
        btnRtm.setParent(rtmContainer);
    }

    private static Textbox addRtmRow(Rows rows, String label, String value, boolean multi) {
        Row row = new Row(); row.setParent(rows);
        new Label(label).setParent(row);
        Textbox tb = new Textbox(value != null ? value : "");
        tb.setWidth("98%");
        if (multi) { tb.setRows(2); tb.setMultiline(true); }
        tb.setParent(row);
        return tb;
    }

    @SuppressWarnings("unchecked")
    private static void buildAddForm(final HasilTemuanSPMI temuan, final Component container,
            final List<TindakLanjutTemuanSPMI> existingList, final Session sess) {

        final Textbox tbDeskripsi   = new Textbox();
        tbDeskripsi.setRows(2);
        tbDeskripsi.setWidth("98%");
        tbDeskripsi.setTooltiptext("Uraikan rencana perbaikan / koreksi yang akan dilakukan");

        final Textbox tbPic         = new Textbox();
        tbPic.setWidth("98%");
        tbPic.setTooltiptext("Nama penanggung jawab pelaksanaan tindak lanjut");

        final MyDatebox dbTarget    = new MyDatebox(null);
        dbTarget.setFormat(Common.dateFormat3.get().toPattern());
        dbTarget.setReadonly(true);

        final MyDatebox dbSelesai   = new MyDatebox(null);
        dbSelesai.setFormat(Common.dateFormat3.get().toPattern());
        dbSelesai.setReadonly(true);

        final MyIntbox  nbProgress  = new MyIntbox(0);
        nbProgress.setWidth("80px");
        nbProgress.setTooltiptext("0 – 100");

        final Combobox  cbStatus    = new Combobox();
        for (String s : TindakLanjutTemuanSPMI.statusLabel.keySet()) {
            Comboitem ci = new Comboitem(s);
            ci.setValue(s);
            cbStatus.appendChild(ci);
        }
        cbStatus.setSelectedIndex(0);
        cbStatus.setReadonly(true);
        cbStatus.setWidth("160px");

        final Textbox tbKeterangan  = new Textbox();
        tbKeterangan.setRows(2);
        tbKeterangan.setWidth("98%");

        // Grid for the form
        org.zkoss.zul.Grid formGrid = new org.zkoss.zul.Grid();
        formGrid.setWidth("100%");
        formGrid.setSclass("dgrid");
        org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
        cols.setParent(formGrid);
        ais.ui.util.MyColumnConfig c1 = new ais.ui.util.MyColumnConfig();
        c1.setWidth("140px");
        c1.setParent(cols);
        new ais.ui.util.MyColumnConfig().setParent(cols);

        Rows rows = new Rows();
        rows.setParent(formGrid);

        addRow(rows, "Deskripsi Tindak Lanjut *", tbDeskripsi);
        addRow(rows, "PIC (Penanggung Jawab)", tbPic);
        addRow(rows, "Target Penyelesaian", dbTarget);
        addRow(rows, "Tanggal Selesai", dbSelesai);
        addRow(rows, "Progress (%)", nbProgress);
        addRow(rows, "Status", cbStatus);
        addRow(rows, "Keterangan", tbKeterangan);

        formGrid.setParent(container);

        // Save button
        org.zkoss.zul.Toolbar tb = new org.zkoss.zul.Toolbar();
        tb.setStyle("padding:8px 14px;");
        tb.setParent(container);

        MyToolbarbuttonConfig btnSave = new MyToolbarbuttonConfig("Simpan Tindak Lanjut", "/img/save.gif");
        btnSave.setParent(tb);
        btnSave.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                String desc = tbDeskripsi.getValue() == null ? "" : tbDeskripsi.getValue().trim();
                if (desc.isEmpty()) {
                    MyMessageboxConfig.show("Mohon maaf, deskripsi tindak lanjut belum diisi. "
                            + "Langkah yang dapat dilakukan: (1) isi kolom Deskripsi Tindak Lanjut pada form; "
                            + "(2) pastikan teks tidak kosong atau hanya berisi spasi; "
                            + "(3) ulangi proses simpan. "
                            + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                            MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                    return;
                }
                TindakLanjutTemuanSPMI tl = new TindakLanjutTemuanSPMI(temuan);
                tl.setDeskripsi(desc);
                tl.setPicNama(tbPic.getValue());
                tl.setTargetDate(dbTarget.getValue());
                tl.setTanggalSelesai(dbSelesai.getValue());
                tl.setProgressPersen(nbProgress.getValue() == null ? 0 : nbProgress.getValue());
                tl.setStatus(cbStatus.getSelectedItem() == null ? TindakLanjutTemuanSPMI.BELUM_DIMULAI
                        : (String) cbStatus.getSelectedItem().getValue());
                tl.setKeterangan(tbKeterangan.getValue());
                Common.refreshSaveOrUpdate(tl);

                // Refresh panel
                buildTindakLanjutPanel(temuan, container);
            }
        });
    }

    // =====================================================================
    // ZK lifecycle (when used as standalone controller)
    // =====================================================================

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();
        initPrivileges();
        initPagingListener();
    }

    @Override
    public void init(GeneralValueObject obj) throws Exception {
        current = (TindakLanjutTemuanSPMI) obj;
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria c = session.createCriteria(TindakLanjutTemuanSPMI.class)
                .add(searchaktif == null || searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (parentTemuan != null)
            c.add(Restrictions.eq("hasilTemuanSPMI", parentTemuan));
        if (order) c.addOrder(Order.asc("id"));
        return c;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSearchDefault(Event event) {
        if (paging == null || grid == null) return;
        Common.initPaging(initCriteria(false), paging);
        List<TindakLanjutTemuanSPMI> data = initCriteria(true)
                .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                .setFirstResult(Common.ROWS_COUNT_ON_PAGE * paging.getActivePage())
                .list();
        refreshGridData(data, new TlRenderer());
    }

    class TlRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            final TindakLanjutTemuanSPMI item = (TindakLanjutTemuanSPMI) obj;
            row.setValign("top");

            new Label(item.getDeskripsi()).setParent(row);
            new Label(item.getPicNama() == null ? "" : item.getPicNama()).setParent(row);
            new Label(item.getTargetDate() == null ? ""
                    : Common.dateFormat3.get().format(item.getTargetDate())).setParent(row);
            new Label(item.getProgressPersen() + "%").setParent(row);

            // Status dengan warna
            Label statusLbl = new Label(item.getStatus());
            String sColor = TindakLanjutTemuanSPMI.SELESAI.equals(item.getStatus()) ? "#22c55e"
                    : TindakLanjutTemuanSPMI.TERLAMBAT.equals(item.getStatus()) ? "#ef4444"
                    : TindakLanjutTemuanSPMI.SEDANG_BERJALAN.equals(item.getStatus()) ? "#3b82f6"
                    : "#94a3b8";
            statusLbl.setStyle("color:" + sColor + "; font-weight:700;");
            statusLbl.setParent(row);

            Common.copyEditDeleteButtons(edit, delete, item, TindakLanjutSPMIAction.this).setParent(row);
        }
    }

    // =====================================================================
    // HTML helpers (package-private so DasboardSPMI can reuse)
    // =====================================================================

    static String statusBadge(String st) {
        if (st == null) return "";
        String bg  = HasilTemuanSPMI.KTS_MYR1.equals(st) ? "#fee2e2"
                   : HasilTemuanSPMI.KTS_MNR1.equals(st) ? "#ffedd5"
                   : HasilTemuanSPMI.S1.equals(st)        ? "#dcfce7"
                   : HasilTemuanSPMI.LS1.equals(st)       ? "#d1fae5"
                   : "#dbeafe";
        String clr = HasilTemuanSPMI.KTS_MYR1.equals(st) ? "#991b1b"
                   : HasilTemuanSPMI.KTS_MNR1.equals(st) ? "#9a3412"
                   : HasilTemuanSPMI.S1.equals(st)        ? "#166534"
                   : HasilTemuanSPMI.LS1.equals(st)       ? "#064e3b"
                   : "#1e40af";
        String label = HasilTemuanSPMI.statusData.containsKey(st)
                ? HasilTemuanSPMI.statusData.get(st) : st;
        return "<span style='border-radius:999px; padding:2px 9px; font-size:10px; font-weight:700;"
             + " background:" + bg + "; color:" + clr + ";'>" + esc(label) + "</span>";
    }

    private static String buildTlRows(List<TindakLanjutTemuanSPMI> list) {
        StringBuilder sb = new StringBuilder();
        for (TindakLanjutTemuanSPMI tl : list) {
            String st   = tl.getStatus();
            String sBg  = TindakLanjutTemuanSPMI.SELESAI.equals(st)         ? "#dcfce7"
                        : TindakLanjutTemuanSPMI.TERLAMBAT.equals(st)       ? "#fee2e2"
                        : TindakLanjutTemuanSPMI.SEDANG_BERJALAN.equals(st) ? "#dbeafe"
                        : "#f1f5f9";
            String sClr = TindakLanjutTemuanSPMI.SELESAI.equals(st)         ? "#166534"
                        : TindakLanjutTemuanSPMI.TERLAMBAT.equals(st)       ? "#991b1b"
                        : TindakLanjutTemuanSPMI.SEDANG_BERJALAN.equals(st) ? "#1e40af"
                        : "#64748b";
            int pct = tl.getProgressPersen();
            String barColor = pct >= 100 ? "#22c55e" : (pct >= 50 ? "#3b82f6" : "#f97316");

            sb.append("<tr style='border-bottom:1px solid #f1f5f9;'>")
              .append("<td style='padding:7px 8px; color:#1e293b;'>").append(esc(tl.getDeskripsi())).append("</td>")
              .append("<td style='padding:7px 8px; color:#475569;'>")
                .append(esc(tl.getPicNama() != null ? tl.getPicNama() : "—")).append("</td>")
              .append("<td style='padding:7px 8px; color:#475569; white-space:nowrap;'>")
                .append(tl.getTargetDate() != null ? Common.dateFormat3.get().format(tl.getTargetDate()) : "—")
                .append("</td>")
              .append("<td style='padding:7px 8px; min-width:90px;'>")
                .append("<div style='font-size:10px; color:#64748b; margin-bottom:2px;'>").append(pct).append("%</div>")
                .append("<div style='height:6px; border-radius:3px; background:#e2e8f0;'>")
                .append("<div style='height:6px; border-radius:3px; background:").append(barColor)
                .append("; width:").append(Math.min(100, pct)).append("%;'></div></div></td>")
              .append("<td style='padding:7px 8px;'>")
                .append("<span style='border-radius:999px; padding:2px 9px; font-size:10px; font-weight:700;")
                .append(" background:").append(sBg).append("; color:").append(sClr).append(";'>")
                .append(esc(st)).append("</span></td>")
              .append("</tr>");
        }
        return sb.toString();
    }

    private static void addRow(Rows rows, String label, Component input) {
        ais.ui.util.MyFormRow row = new ais.ui.util.MyFormRow();
        row.setValign("top");
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig(label));
        input.setParent(row);
    }

    private static void appendHtml(Component parent, String html) {
        new org.zkoss.zul.Html(html).setParent(parent);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String abbreviate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
