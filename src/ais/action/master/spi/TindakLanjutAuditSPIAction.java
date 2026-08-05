package ais.action.master.spi;

import java.util.List;

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
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.spi.TemuanAuditSPI;
import ais.database.model.spi.TindakLanjutAuditSPI;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h2>TindakLanjutAuditSPIAction &mdash; Pengendali Panel Realisasi Tindak Lanjut Auditee</h2>
 *
 * <p>
 * Pengendali ZK yang membangun dan mengelola panel daftar-dan-tambah tindak lanjut untuk satu
 * {@link TemuanAuditSPI}. Panel ini TIDAK berdiri sendiri sebagai layar ZUL terpisah &mdash; kelas
 * ini dipanggil secara langsung (bukan lewat pola composer {@code apply=} biasa) baik sebagai
 * jendela popup modal ({@link #openForTemuan(TemuanAuditSPI, Component)}, dipakai dari daftar
 * temuan pada {@link TemuanAuditSPIAction}) maupun sebagai panel tertanam di dalam halaman lain
 * ({@link #buildInlinePanel(TemuanAuditSPI)}). Pola ini SENGAJA meniru persis
 * {@code ais.action.master.spmi.TindakLanjutSPMIAction} yang sudah production-proven, tanpa bagian
 * "Rapat Tinjauan Manajemen (RTM)" milik modul tersebut &mdash; RTM adalah istilah spesifik siklus
 * PPEPP penjaminan mutu akademik yang tidak relevan bagi audit internal umum.
 * </p>
 *
 * @author e-Campus SPI Team
 */
public class TindakLanjutAuditSPIAction extends BaseSPIAction {

    private static final long serialVersionUID = 1L;

    // =========================================================
    // Static entry points
    // =========================================================

    /** Membuka panel tindak lanjut sebagai jendela popup modal untuk satu temuan. */
    public static void openForTemuan(TemuanAuditSPI temuan, Component anchor) throws Exception {
        String judul = temuan.getChecklistSnapshot() == null ? "" : abbreviate(temuan.getChecklistSnapshot(), 50);
        MyWindow win = new MyWindow();
        win.setTitle("Tindak Lanjut Temuan — " + judul);
        win.setWidth("700px");
        win.setHeight("85%");
        win.setClosable(true);
        win.setBorder("normal");
        win.setMode("popup");
        win.setPosition("center,center");
        win.setParent(anchor.getPage().getFirstRoot());

        buildPanel(temuan, win);
        win.setVisible(true);
        win.onModal();
    }

    /** Membangun panel tertanam (non-modal) untuk disisipkan ke dalam ZUL/popup lain. */
    public static Div buildInlinePanel(TemuanAuditSPI temuan) throws Exception {
        Div panel = new Div();
        panel.setStyle("width:100%; box-sizing:border-box;");
        buildPanel(temuan, panel);
        return panel;
    }

    @SuppressWarnings("unchecked")
    private static void buildPanel(final TemuanAuditSPI temuan, final Component container) throws Exception {
        Common.clear(container);

        Session sess = HibernateUtil.currentSession();
        List<TindakLanjutAuditSPI> list = sess.createCriteria(TindakLanjutAuditSPI.class)
                .add(Restrictions.eq("temuanAuditSPI", temuan))
                .addOrder(Order.asc("id"))
                .list();

        appendHtml(container,
            "<div style='font-size:11px; color:#64748b; padding:10px 14px 6px; line-height:1.55;"
            + " background:#f8fafc; border-radius:8px; margin:6px 14px;'>"
            + "<b>Rekomendasi Auditor:</b> " + esc(temuan.getRekomendasi() == null ? "(belum diisi)" : temuan.getRekomendasi())
            + "</div>");

        if (!list.isEmpty()) {
            appendHtml(container,
                "<div style='padding:0 14px 8px;'>"
                + "<div style='font-size:12px; font-weight:700; color:#0f172a; margin-bottom:6px;'>"
                + "Riwayat Tindak Lanjut</div>"
                + "<table style='width:100%; border-collapse:collapse; font-size:11px;'>"
                + "<tr style='background:#f8fafc;'>"
                + "<th style='padding:6px 8px; text-align:left; border-bottom:2px solid #e2e8f0;'>Deskripsi</th>"
                + "<th style='padding:6px 8px; text-align:left; border-bottom:2px solid #e2e8f0;'>PIC</th>"
                + "<th style='padding:6px 8px; text-align:left; border-bottom:2px solid #e2e8f0;'>Target</th>"
                + "<th style='padding:6px 8px; text-align:left; border-bottom:2px solid #e2e8f0;'>Progress</th>"
                + "<th style='padding:6px 8px; text-align:left; border-bottom:2px solid #e2e8f0;'>Status</th>"
                + "</tr>"
                + buildRows(list)
                + "</table></div>");
        } else {
            appendHtml(container,
                "<div style='padding:12px 14px; color:#94a3b8; font-size:12px;'>"
                + "Belum ada tindak lanjut yang dicatat untuk temuan ini.</div>");
        }

        appendHtml(container,
            "<div style='padding:8px 14px 4px; font-size:12px; font-weight:700; color:#0f172a;"
            + " border-top:1px solid #f1f5f9;'>Catat Tindak Lanjut Baru</div>");

        buildAddForm(temuan, container);
    }

    @SuppressWarnings("unchecked")
    private static void buildAddForm(final TemuanAuditSPI temuan, final Component container) {
        final Textbox tbDeskripsi = new Textbox();
        tbDeskripsi.setRows(2);
        tbDeskripsi.setWidth("98%");
        tbDeskripsi.setTooltiptext("Uraikan tindakan nyata yang sudah/sedang dilakukan auditee");

        final Textbox tbPic = new Textbox();
        tbPic.setWidth("98%");
        tbPic.setTooltiptext("Nama penanggung jawab pelaksanaan tindak lanjut di sisi auditee");

        final MyDatebox dbTarget = new MyDatebox(null);
        dbTarget.setFormat(Common.dateFormat3.get().toPattern());
        dbTarget.setReadonly(true);

        final MyDatebox dbSelesai = new MyDatebox(null);
        dbSelesai.setFormat(Common.dateFormat3.get().toPattern());
        dbSelesai.setReadonly(true);

        final MyIntbox nbProgress = new MyIntbox(0);
        nbProgress.setWidth("80px");
        nbProgress.setTooltiptext("0 – 100");

        final Combobox cbStatus = new Combobox();
        for (String s : TindakLanjutAuditSPI.statusLabel.keySet()) {
            Comboitem ci = new Comboitem(s);
            ci.setValue(s);
            cbStatus.appendChild(ci);
        }
        cbStatus.setSelectedIndex(0);
        cbStatus.setReadonly(true);
        cbStatus.setWidth("160px");

        final Textbox tbKeterangan = new Textbox();
        tbKeterangan.setRows(2);
        tbKeterangan.setWidth("98%");

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
                    MyMessageboxConfig.show("Mohon maaf, Deskripsi Tindak Lanjut belum diisi."
                            + " Langkah yang dapat dilakukan:"
                            + " (1) isi kolom deskripsi tindak lanjut di atas dengan uraian langkah yang telah atau akan diambil;"
                            + " (2) pastikan deskripsi tidak kosong dan relevan dengan temuan yang ditindaklanjuti;"
                            + " (3) klik tombol Simpan Tindak Lanjut kembali."
                            + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                            MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                    return;
                }
                TindakLanjutAuditSPI tl = new TindakLanjutAuditSPI(temuan);
                tl.setDeskripsi(desc);
                tl.setPicNama(tbPic.getValue());
                tl.setTargetDate(dbTarget.getValue());
                tl.setTanggalSelesai(dbSelesai.getValue());
                tl.setProgressPersen(nbProgress.getValue() == null ? 0 : nbProgress.getValue());
                tl.setStatus(cbStatus.getSelectedItem() == null ? TindakLanjutAuditSPI.BELUM_DIMULAI
                        : (String) cbStatus.getSelectedItem().getValue());
                tl.setKeterangan(tbKeterangan.getValue());
                Common.refreshSaveOrUpdate(tl);

                buildPanel(temuan, container);
            }
        });
    }

    // =====================================================================
    // ZK lifecycle (standalone-controller path — not normally used, kept for
    // consistency with BaseSPIAction's contract; the panel is normally built
    // via the static entry points above)
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
        // no-op: entries are created inline via buildAddForm's save button
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria c = session.createCriteria(TindakLanjutAuditSPI.class)
                .add(searchaktif == null || searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (order) c.addOrder(Order.asc("id"));
        return c;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSearchDefault(Event event) {
        if (paging == null || grid == null) return;
        Common.initPaging(initCriteria(false), paging);
        List<TindakLanjutAuditSPI> data = initCriteria(true)
                .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                .setFirstResult(Common.ROWS_COUNT_ON_PAGE * paging.getActivePage())
                .list();
        refreshGridData(data, new ais.ui.util.MyRowRenderer() {
            @Override
            public void render(Row row, Object obj) throws Exception {
                TindakLanjutAuditSPI item = (TindakLanjutAuditSPI) obj;
                row.setValign("top");
                new Label(item.getDeskripsi()).setParent(row);
                new Label(item.getStatus()).setParent(row);
            }
        });
    }

    // =====================================================================
    // HTML helpers
    // =====================================================================

    private static String buildRows(List<TindakLanjutAuditSPI> list) {
        StringBuilder sb = new StringBuilder();
        for (TindakLanjutAuditSPI tl : list) {
            String st = tl.getStatus();
            String sBg = TindakLanjutAuditSPI.SELESAI.equals(st) ? "#dcfce7"
                    : TindakLanjutAuditSPI.TERLAMBAT.equals(st) ? "#fee2e2"
                    : TindakLanjutAuditSPI.SEDANG_BERJALAN.equals(st) ? "#dbeafe"
                    : "#f1f5f9";
            String sClr = TindakLanjutAuditSPI.SELESAI.equals(st) ? "#166534"
                    : TindakLanjutAuditSPI.TERLAMBAT.equals(st) ? "#991b1b"
                    : TindakLanjutAuditSPI.SEDANG_BERJALAN.equals(st) ? "#1e40af"
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
