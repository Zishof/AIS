package ais.action.master.spmi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.KonfigurasiManager;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.MyMessageboxConfig;
import ais.database.model.Jurusan;
import ais.database.model.spmi.ButirMutuSPMI;
import ais.database.model.spmi.IndikatorSPMI;
import ais.database.model.spmi.JenisSPMI;
import ais.database.model.spmi.StandarSPMI;
import ais.ui.util.MyWindow;

/**
 * Modul Pelaksanaan SPMI — Sasaran Mutu per Indikator per Prodi per Semester.
 * Fase P-2 (Pelaksanaan) dalam siklus PPEPP.
 *
 * Data disimpan di tabel konfigurasi (tanpa DDL baru).
 * Key format: spmi_sasaran_{jurusanId}_{ta}_{masa}_{jenisSPMIId}
 * Value: JSON {"indikatorId": {"target":"...", "capaian":"...", "bukti":"...", "status":"..."}}
 */
public class SasaranMutuSPMIAction {

    private static final String KONFIG_PREFIX = "spmi_sasaran_";

    // ── Status opsi ─────────────────────────────────────────────────────────
    private static final String[] STATUS_OPTIONS = {
        "Belum Tercapai", "Sedang Berjalan", "Tercapai", "Dilampaui"
    };

    /**
     * Buka popup Sasaran Mutu untuk satu HasilSPMI (jenis, jurusan, TA, masa sudah diketahui).
     */
    public static void openForHasilSPMI(final Long jurusanId, final String namaJurusan,
            final String ta, final String masa,
            final Long jenisSPMIId, final String namaJenis,
            final boolean editable, final Component anchor) {
        try {
            final MyWindow win = new MyWindow();
            win.setTitle("Sasaran Mutu SPMI — Pelaksanaan");
            win.setWidth("95%"); win.setHeight("92%");
            Vbox vb = new Vbox();
            vb.setWidth("100%"); vb.setStyle("padding:10px;box-sizing:border-box;");
            vb.setParent(win);

            // Header info
            appendHtml(vb, "<div style='background:#eff6ff;border:1px solid #bfdbfe;border-radius:8px;"
                    + "padding:10px 14px;margin-bottom:12px;font-size:12px;'>"
                    + "<b>Program Studi:</b> " + esc(namaJurusan)
                    + " &nbsp;|&nbsp; <b>Jenis SPMI:</b> " + esc(namaJenis)
                    + " &nbsp;|&nbsp; <b>TA:</b> " + esc(ta)
                    + " &nbsp;|&nbsp; <b>Semester:</b> " + esc(masa)
                    + "</div>");

            appendHtml(vb, "<div style='font-size:11px;color:#64748b;margin-bottom:10px;'>"
                    + "Fase <b>Pelaksanaan</b> PPEPP: isi target, capaian aktual, dan bukti pelaksanaan "
                    + "per indikator standar sebelum/sesudah audit mutu internal.</div>");

            buildSasaranForm(vb, jurusanId, ta, masa, jenisSPMIId, editable, win);

            win.doModal();
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static void buildSasaranForm(final Component container,
            final Long jurusanId, final String ta, final String masa,
            final Long jenisSPMIId, final boolean editable, final MyWindow win) {
        Common.clear(container);

        // Re-append header
        appendHtml(container, "<div style='background:#eff6ff;border:1px solid #bfdbfe;border-radius:8px;"
                + "padding:10px 14px;margin-bottom:12px;font-size:12px;'>"
                + "<b>Program Studi ID:</b> " + jurusanId
                + " &nbsp;|&nbsp; <b>TA:</b> " + esc(ta)
                + " &nbsp;|&nbsp; <b>Semester:</b> " + esc(masa)
                + "</div>");

        // Load indikator list via currentSession (ZK context)
        Session sess = HibernateUtil.currentSession();

        // Query: Indikator → ButirMutu → Standar → JenisSPMI
        List<IndikatorSPMI> indikators = new ArrayList<IndikatorSPMI>();
        try {
            indikators = (List<IndikatorSPMI>) sess.createCriteria(IndikatorSPMI.class, "ind")
                .createAlias("ind.butirMutuSPMI", "butir")
                .createAlias("butir.standarSPMI", "standar")
                .add(Restrictions.eq("standar.jenisSPMI.id", jenisSPMIId))
                .add(Restrictions.eq("ind.aktif", Boolean.TRUE))
                .addOrder(Order.asc("standar.nomorUrut"))
                .addOrder(Order.asc("butir.nomorUrut"))
                .addOrder(Order.asc("ind.nomorUrut"))
                .list();
        } catch (Exception ex) {
            appendHtml(container, "<div style='color:red;padding:8px;'>Gagal memuat indikator: "
                    + esc(ex.getMessage()) + "</div>");
            return;
        }

        if (indikators.isEmpty()) {
            appendHtml(container, "<div style='color:#94a3b8;font-style:italic;padding:12px;'>"
                    + "Belum ada indikator untuk jenis SPMI ini. "
                    + "Silakan tambahkan indikator terlebih dahulu di menu master SPMI.</div>");
            return;
        }

        // Load existing konfigurasi
        String konfigKey = buildKey(jurusanId, ta, masa, jenisSPMIId);
        JSONObject existing = new JSONObject();
        try {
            ais.database.model.Konfigurasi k = Common.getKonfigurasi(konfigKey, "{}");
            if (k != null && k.getNilai() != null && !k.getNilai().trim().isEmpty()) {
                existing = new JSONObject(k.getNilai());
            }
        } catch (Exception ign) { ais.common.ErrorAuditUtil.record(ign, "auto-audit(empty-catch) src/ais/action/master/spmi/SasaranMutuSPMIAction.java:140");}

        // Group by Standar → ButirMutu for display
        Map<String, Map<String, List<IndikatorSPMI>>> grouped =
                new LinkedHashMap<String, Map<String, List<IndikatorSPMI>>>();
        for (IndikatorSPMI ind : indikators) {
            ButirMutuSPMI butir = ind.getButirMutuSPMI();
            StandarSPMI standar = butir != null ? butir.getStandarSPMI() : null;
            String standarKey = standar != null ? standar.getNomorUrut() + ". " + standar.getNama() : "(tanpa standar)";
            String butirKey   = butir  != null ? butir.getNomorUrut()  + ". " + butir.getNama()  : "(tanpa butir mutu)";
            if (!grouped.containsKey(standarKey)) grouped.put(standarKey, new LinkedHashMap<String, List<IndikatorSPMI>>());
            if (!grouped.get(standarKey).containsKey(butirKey)) grouped.get(standarKey).put(butirKey, new ArrayList<IndikatorSPMI>());
            grouped.get(standarKey).get(butirKey).add(ind);
        }

        // -- Build ZK form rows ---
        final List<Long>    indIds     = new ArrayList<Long>();
        final List<Textbox> tbTargets  = new ArrayList<Textbox>();
        final List<Textbox> tbCapaians = new ArrayList<Textbox>();
        final List<Textbox> tbBuktis   = new ArrayList<Textbox>();
        final List<Combobox> cbStatuses = new ArrayList<Combobox>();

        Div scrollDiv = new Div();
        scrollDiv.setStyle("max-height:55vh;overflow-y:auto;border:1px solid #e2e8f0;border-radius:6px;");
        scrollDiv.setParent(container);

        for (Map.Entry<String, Map<String, List<IndikatorSPMI>>> standarEntry : grouped.entrySet()) {
            appendHtml(scrollDiv, "<div style='background:#1e40af;color:#fff;font-size:11px;font-weight:700;"
                    + "padding:6px 12px;'>" + esc(standarEntry.getKey()) + "</div>");

            for (Map.Entry<String, List<IndikatorSPMI>> butirEntry : standarEntry.getValue().entrySet()) {
                appendHtml(scrollDiv, "<div style='background:#dbeafe;font-size:11px;font-weight:600;"
                        + "padding:4px 12px 4px 20px;color:#1e40af;'>" + esc(butirEntry.getKey()) + "</div>");

                for (IndikatorSPMI ind : butirEntry.getValue()) {
                    JSONObject indData = new JSONObject();
                    try {
                        if (existing.has(String.valueOf(ind.getId()))) {
                            indData = existing.getJSONObject(String.valueOf(ind.getId()));
                        }
                    } catch (Exception ign) { ais.common.ErrorAuditUtil.record(ign, "auto-audit(empty-catch) src/ais/action/master/spmi/SasaranMutuSPMIAction.java:180");}

                    Div indRow = new Div();
                    indRow.setStyle("padding:8px 12px 8px 28px;border-bottom:1px solid #f1f5f9;");
                    indRow.setParent(scrollDiv);

                    appendHtml(indRow, "<div style='font-size:11px;font-weight:600;color:#0f172a;margin-bottom:6px;'>"
                            + esc(ind.getNomorUrut() + ". " + ind.getNama()) + "</div>");

                    org.zkoss.zul.Grid grid = new org.zkoss.zul.Grid();
                    grid.setWidth("100%");
                    org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
                    org.zkoss.zul.Column c1 = new org.zkoss.zul.Column("Label"); c1.setWidth("140px"); c1.setParent(cols);
                    org.zkoss.zul.Column c2 = new org.zkoss.zul.Column("Nilai"); c2.setParent(cols);
                    cols.setParent(grid);
                    Rows rows = new Rows(); rows.setParent(grid);

                    Textbox tbTarget  = buildFormRow(rows, "Target Standar",     indData.optString("target",""), editable, false);
                    Textbox tbCapaian = buildFormRow(rows, "Capaian Aktual",     indData.optString("capaian",""), editable, false);
                    Textbox tbBukti   = buildFormRow(rows, "Bukti Pelaksanaan",  indData.optString("bukti",""), editable, true);

                    Row statusRow = new Row(); statusRow.setParent(rows);
                    new Label(ais.common.Common.getBahasaConfig("Status")).setParent(statusRow);
                    Combobox cbSt = new Combobox();
                    cbSt.setReadonly(true); cbSt.setWidth("180px");
                    for (String s : STATUS_OPTIONS) {
                        Comboitem ci = new Comboitem(s); ci.setValue(s); cbSt.appendChild(ci);
                    }
                    String existStatus = indData.optString("status", STATUS_OPTIONS[0]);
                    for (int si = 0; si < STATUS_OPTIONS.length; si++) {
                        if (STATUS_OPTIONS[si].equals(existStatus)) { cbSt.setSelectedIndex(si); break; }
                    }
                    cbSt.setDisabled(!editable);
                    cbSt.setParent(statusRow);

                    grid.setParent(indRow);

                    indIds.add(ind.getId());
                    tbTargets.add(tbTarget);
                    tbCapaians.add(tbCapaian);
                    tbBuktis.add(tbBukti);
                    cbStatuses.add(cbSt);
                }
            }
        }

        if (editable) {
            Hbox hbBtn = new Hbox();
            hbBtn.setStyle("margin-top:12px;gap:8px;");
            hbBtn.setParent(container);

            org.zkoss.zul.Button btnSimpan = new org.zkoss.zul.Button("Simpan Sasaran Mutu");
            btnSimpan.setSclass("btn-primary");
            btnSimpan.addEventListener("onClick", new EventListener() {
                public void onEvent(Event event) throws Exception {
                    saveSasaranMutu(jurusanId, ta, masa, jenisSPMIId,
                            indIds, tbTargets, tbCapaians, tbBuktis, cbStatuses);
                }
            });
            btnSimpan.setParent(hbBtn);

            org.zkoss.zul.Button btnClose = new org.zkoss.zul.Button("Tutup");
            btnClose.addEventListener("onClick", new EventListener() {
                public void onEvent(Event event) throws Exception { win.detach(); }
            });
            btnClose.setParent(hbBtn);
        }
    }

    private static Textbox buildFormRow(Rows rows, String label, String value,
            boolean editable, boolean multiline) {
        Row row = new Row(); row.setParent(rows);
        new Label(label).setParent(row);
        Textbox tb = new Textbox(value != null ? value : "");
        tb.setWidth("98%");
        if (multiline) { tb.setRows(2); tb.setMultiline(true); }
        tb.setReadonly(!editable);
        tb.setParent(row);
        return tb;
    }

    private static void saveSasaranMutu(Long jurusanId, String ta, String masa, Long jenisSPMIId,
            List<Long> indIds, List<Textbox> tbTargets, List<Textbox> tbCapaians,
            List<Textbox> tbBuktis, List<Combobox> cbStatuses) {
        try {
            JSONObject result = new JSONObject();
            for (int i = 0; i < indIds.size(); i++) {
                JSONObject entry = new JSONObject();
                entry.put("target",  tbTargets.get(i).getValue().trim());
                entry.put("capaian", tbCapaians.get(i).getValue().trim());
                entry.put("bukti",   tbBuktis.get(i).getValue().trim());
                entry.put("status",  cbStatuses.get(i).getValue() != null
                        ? cbStatuses.get(i).getValue().toString() : STATUS_OPTIONS[0]);
                result.put(String.valueOf(indIds.get(i)), entry);
            }
            String key = buildKey(jurusanId, ta, masa, jenisSPMIId);
            KonfigurasiManager.simpanKonfigurasi(key, result.toString());
            MyMessageboxConfig.show("Sasaran mutu berhasil disimpan",
                    "Info", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    /**
     * Bangun ringkasan capaian Sasaran Mutu dalam bentuk HTML (untuk embed di HasilSPMI tampil).
     */
    @SuppressWarnings("unchecked")
    public static String buildRingkasanHtml(Long jurusanId, String ta, String masa, Long jenisSPMIId) {
        try {
            String key = buildKey(jurusanId, ta, masa, jenisSPMIId);
            ais.database.model.Konfigurasi k = Common.getKonfigurasi(key, "{}");
            if (k == null || k.getNilai() == null || k.getNilai().trim().isEmpty()
                    || "{}".equals(k.getNilai().trim())) {
                return "<div style='color:#94a3b8;font-size:11px;font-style:italic;'>"
                        + "Sasaran mutu belum diisi untuk periode ini.</div>"
                        + "<div style='margin-top:6px;background:#fff8e1;border-left:3px solid #ffc107;"
                        + "padding:7px 10px;border-radius:3px;font-size:11px;line-height:1.8;'>"
                        + "<b>Cara mengatasi:</b><br>"
                        + "1. Buka menu Sasaran Mutu SPMI dan pilih periode yang sesuai.<br>"
                        + "2. Isi target sasaran mutu untuk setiap indikator standar yang tersedia.<br>"
                        + "3. Simpan perubahan lalu muat ulang halaman ini."
                        + "</div>";
            }
            JSONObject data = new JSONObject(k.getNilai());
            if (data.length() == 0) return "<div style='color:#94a3b8;font-size:11px;font-style:italic;'>"
                    + "Sasaran mutu belum diisi.</div>"
                    + "<div style='margin-top:6px;background:#fff8e1;border-left:3px solid #ffc107;"
                    + "padding:7px 10px;border-radius:3px;font-size:11px;line-height:1.8;'>"
                    + "<b>Cara mengatasi:</b><br>"
                    + "1. Buka menu Sasaran Mutu SPMI dan pilih periode yang sesuai.<br>"
                    + "2. Isi target sasaran mutu untuk setiap indikator standar yang tersedia.<br>"
                    + "3. Simpan perubahan lalu muat ulang halaman ini."
                    + "</div>";

            StringBuilder sb = new StringBuilder();
            sb.append("<table style='width:100%;border-collapse:collapse;font-size:11px;'>");
            sb.append("<tr style='background:#f1f5f9;'>");
            sb.append("<th style='padding:5px 8px;text-align:left;border-bottom:2px solid #e2e8f0;'>Indikator ID</th>");
            sb.append("<th style='padding:5px 8px;text-align:left;border-bottom:2px solid #e2e8f0;'>Target</th>");
            sb.append("<th style='padding:5px 8px;text-align:left;border-bottom:2px solid #e2e8f0;'>Capaian</th>");
            sb.append("<th style='padding:5px 8px;text-align:left;border-bottom:2px solid #e2e8f0;'>Status</th>");
            sb.append("</tr>");
            for (String indId : JSONObject.getNames(data) != null ? JSONObject.getNames(data) : new String[0]) {
                JSONObject entry = data.optJSONObject(indId);
                if (entry == null) continue;
                String status  = entry.optString("status", "-");
                String color   = "Tercapai".equals(status) || "Dilampaui".equals(status) ? "#166534" : "#991b1b";
                String bgColor = "Tercapai".equals(status) || "Dilampaui".equals(status) ? "#dcfce7" : "#fee2e2";
                sb.append("<tr>");
                sb.append("<td style='padding:4px 8px;border-bottom:1px solid #f1f5f9;'>#").append(esc(indId)).append("</td>");
                sb.append("<td style='padding:4px 8px;border-bottom:1px solid #f1f5f9;'>").append(esc(entry.optString("target","-"))).append("</td>");
                sb.append("<td style='padding:4px 8px;border-bottom:1px solid #f1f5f9;'>").append(esc(entry.optString("capaian","-"))).append("</td>");
                sb.append("<td style='padding:4px 8px;border-bottom:1px solid #f1f5f9;'>"
                        + "<span style='background:").append(bgColor).append(";color:").append(color)
                        .append(";border-radius:4px;padding:2px 6px;font-weight:700;'>")
                        .append(esc(status)).append("</span></td>");
                sb.append("</tr>");
            }
            sb.append("</table>");
            return sb.toString();
        } catch (Exception ex) {
            return "<div style='color:red;font-size:11px;'>Gagal memuat ringkasan: " + esc(ex.getMessage()) + "</div>";
        }
    }

    private static String buildKey(Long jurusanId, String ta, String masa, Long jenisSPMIId) {
        return KONFIG_PREFIX + jurusanId + "_" + ta + "_" + masa + "_" + jenisSPMIId;
    }

    private static void appendHtml(Component parent, String html) {
        new Html(html).setParent(parent);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
