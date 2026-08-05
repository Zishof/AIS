package ais.action.master.spmi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
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
import ais.database.model.spmi.ButirMutuSPMI;
import ais.database.model.spmi.HasilSPMI;
import ais.database.model.spmi.HasilTemuanSPMI;
import ais.database.model.spmi.IndikatorSPMI;
import ais.database.model.spmi.StandarSPMI;
import ais.ui.util.MyWindow;

/**
 * Modul Peningkatan SPMI — Fase P-5 (Peningkatan) dalam siklus PPEPP.
 *
 * Setelah AMI + Tindak Lanjut selesai, gunakan halaman ini untuk:
 * 1. Melihat capaian vs target per indikator
 * 2. Mengusulkan target baru yang lebih tinggi untuk siklus berikutnya
 * 3. Mencatat keputusan revisi standar
 *
 * Data disimpan di tabel konfigurasi.
 * Key: spmi_peningkatan_{hasilSPMIId}
 * Value: JSON array usul peningkatan per butir mutu
 */
public class PeningkatanSPMIAction {

    private static final String KONFIG_PREFIX = "spmi_peningkatan_";

    private static final String[] STATUS_USUL = {
        "Diusulkan", "Disetujui", "Ditolak", "Ditunda"
    };

    /**
     * Buka popup Peningkatan Standar untuk satu HasilSPMI yang sudah selesai.
     */
    public static void openForHasilSPMI(final HasilSPMI hasil, final boolean isAdmin,
            final Component anchor) {
        if (hasil == null) return;
        try {
            String judul = "Peningkatan Standar SPMI — " + (hasil.getNama() != null ? hasil.getNama() : "");
            final MyWindow win = new MyWindow();
            win.setTitle(judul);
            win.setWidth("95%"); win.setHeight("92%");
            Vbox vb = new Vbox();
            vb.setWidth("100%"); vb.setStyle("padding:10px;box-sizing:border-box;overflow:auto;");
            vb.setParent(win);

            appendHtml(vb, "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-radius:8px;"
                    + "padding:10px 14px;margin-bottom:10px;font-size:12px;'>"
                    + "<b>Fase Peningkatan (P-5 PPEPP)</b>: Tinjau hasil AMI dan usulkan target standar "
                    + "yang lebih tinggi untuk siklus berikutnya jika target periode ini sudah tercapai/dilampaui."
                    + "</div>");

            buildPeningkatanPanel(vb, hasil, isAdmin, win);
            win.doModal();
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static void buildPeningkatanPanel(final Component container, final HasilSPMI hasil,
            final boolean isAdmin, final MyWindow win) {
        Common.clear(container);

        Session sess = HibernateUtil.currentSession();

        // Load HasilTemuan untuk HasilSPMI ini
        List<HasilTemuanSPMI> temuanList = new ArrayList<HasilTemuanSPMI>();
        try {
            temuanList = (List<HasilTemuanSPMI>) sess.createCriteria(HasilTemuanSPMI.class)
                .add(Restrictions.eq("hasilSPMI", hasil))
                .add(Restrictions.eq("aktif", Boolean.TRUE))
                .addOrder(Order.asc("id"))
                .list();
        } catch (Exception ex) {
            appendHtml(container, "<div style='color:red;padding:8px;'>Gagal memuat temuan: "
                    + esc(ex.getMessage()) + "</div>");
            return;
        }

        // Ringkasan temuan
        int totalTemuan = temuanList.size();
        int sesuai = 0; int ktsMinor = 0; int ktsMajor = 0; int obs = 0; int ls = 0;
        for (HasilTemuanSPMI t : temuanList) {
            String st = t.getStatus() != null ? t.getStatus() : "";
            if ("S".equals(st) || "Sesuai".equalsIgnoreCase(st))              sesuai++;
            else if ("KTS MNR".equalsIgnoreCase(st) || st.contains("Minor")) ktsMinor++;
            else if ("KTS MYR".equalsIgnoreCase(st) || st.contains("Mayor")) ktsMajor++;
            else if ("O".equals(st) || "Observasi".equalsIgnoreCase(st))      obs++;
            else if ("LS".equals(st) || st.contains("Lampaui"))               ls++;
        }
        appendHtml(container, "<div style='display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px;'>"
                + badge("Total Temuan", String.valueOf(totalTemuan), "#334155", "#f1f5f9")
                + badge("Sesuai", String.valueOf(sesuai), "#166534", "#dcfce7")
                + badge("Melebihi Standar", String.valueOf(ls), "#1e40af", "#dbeafe")
                + badge("KTS Minor", String.valueOf(ktsMinor), "#92400e", "#fef3c7")
                + badge("KTS Mayor", String.valueOf(ktsMajor), "#991b1b", "#fee2e2")
                + badge("Observasi", String.valueOf(obs), "#6b21a8", "#f3e8ff")
                + "</div>");

        // Load existing peningkatan data
        String konfigKey = KONFIG_PREFIX + hasil.getId();
        JSONArray existing = new JSONArray();
        try {
            ais.database.model.Konfigurasi k = Common.getKonfigurasi(konfigKey, "[]");
            if (k != null && k.getNilai() != null && !k.getNilai().trim().isEmpty()) {
                String v = k.getNilai().trim();
                if (v.startsWith("[")) {
                    existing = new JSONArray(v);
                }
            }
        } catch (Exception ign) { ais.common.ErrorAuditUtil.record(ign, "auto-audit(empty-catch) src/ais/action/master/spmi/PeningkatanSPMIAction.java:138");}

        // Map existing by butirId for lookup
        Map<String, JSONObject> existingMap = new LinkedHashMap<String, JSONObject>();
        for (int i = 0; i < existing.length(); i++) {
            JSONObject e = existing.optJSONObject(i);
            if (e != null && e.has("butirId")) {
                existingMap.put(String.valueOf(e.optLong("butirId")), e);
            }
        }

        // Load indikator untuk konteks
        List<IndikatorSPMI> indikators = new ArrayList<IndikatorSPMI>();
        if (hasil.getJenisSPMI() != null) {
            try {
                indikators = (List<IndikatorSPMI>) sess.createCriteria(IndikatorSPMI.class, "ind")
                    .createAlias("ind.butirMutuSPMI", "butir")
                    .createAlias("butir.standarSPMI", "standar")
                    .add(Restrictions.eq("standar.jenisSPMI.id", hasil.getJenisSPMI().getId()))
                    .add(Restrictions.eq("ind.aktif", Boolean.TRUE))
                    .addOrder(Order.asc("standar.nomorUrut"))
                    .addOrder(Order.asc("butir.nomorUrut"))
                    .addOrder(Order.asc("ind.nomorUrut"))
                    .list();
            } catch (Exception ign) { ais.common.ErrorAuditUtil.record(ign, "auto-audit(empty-catch) src/ais/action/master/spmi/PeningkatanSPMIAction.java:162");}
        }

        // Group by ButirMutu
        Map<Long, ButirMutuSPMI> butirMap = new LinkedHashMap<Long, ButirMutuSPMI>();
        for (IndikatorSPMI ind : indikators) {
            if (ind.getButirMutuSPMI() != null) butirMap.put(ind.getButirMutuSPMI().getId(), ind.getButirMutuSPMI());
        }

        if (butirMap.isEmpty()) {
            appendHtml(container, "<div style='color:#94a3b8;font-style:italic;padding:12px;'>"
                    + "Tidak ada butir mutu yang terkait dengan jenis SPMI ini.</div>");
        } else {
            appendHtml(container, "<div style='font-weight:700;font-size:13px;margin-bottom:8px;color:#0f172a;'>"
                    + "Usul Peningkatan Target per Butir Mutu</div>");
        }

        final List<Long>     butirIds        = new ArrayList<Long>();
        final List<Textbox>  tbTargetLamas   = new ArrayList<Textbox>();
        final List<Textbox>  tbTargetBarus   = new ArrayList<Textbox>();
        final List<Textbox>  tbAlasans       = new ArrayList<Textbox>();
        final List<Combobox> cbStatuses      = new ArrayList<Combobox>();

        Div scrollDiv = new Div();
        scrollDiv.setStyle("max-height:45vh;overflow-y:auto;border:1px solid #e2e8f0;border-radius:6px;");
        scrollDiv.setParent(container);

        for (Map.Entry<Long, ButirMutuSPMI> butirEntry : butirMap.entrySet()) {
            ButirMutuSPMI butir = butirEntry.getValue();
            JSONObject ex = existingMap.containsKey(String.valueOf(butir.getId()))
                    ? existingMap.get(String.valueOf(butir.getId())) : new JSONObject();

            String standarNama = butir.getStandarSPMI() != null ? butir.getStandarSPMI().getNama() : "";
            Div butirDiv = new Div();
            butirDiv.setStyle("padding:8px 12px;border-bottom:1px solid #f1f5f9;");
            butirDiv.setParent(scrollDiv);

            appendHtml(butirDiv, "<div style='font-size:11px;color:#64748b;'>" + esc(standarNama) + "</div>");
            appendHtml(butirDiv, "<div style='font-size:12px;font-weight:700;color:#0f172a;margin-bottom:6px;'>"
                    + esc(butir.getNomorUrut() + ". " + butir.getNama()) + "</div>");

            org.zkoss.zul.Grid grid = new org.zkoss.zul.Grid();
            grid.setWidth("100%");
            org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
            org.zkoss.zul.Column c1 = new org.zkoss.zul.Column(); c1.setWidth("160px"); c1.setParent(cols);
            org.zkoss.zul.Column c2 = new org.zkoss.zul.Column(); c2.setParent(cols);
            cols.setParent(grid);
            Rows rows = new Rows(); rows.setParent(grid);

            Textbox tbTgtLama = buildRow(rows, "Target Saat Ini", ex.optString("targetLama",""), isAdmin, false);
            Textbox tbTgtBaru = buildRow(rows, "Target Diusulkan (Baru)", ex.optString("targetBaru",""), isAdmin, false);
            Textbox tbAlasan  = buildRow(rows, "Alasan / Justifikasi", ex.optString("alasan",""), isAdmin, true);

            Row stRow = new Row(); stRow.setParent(rows);
            new Label(ais.common.Common.getBahasaConfig("Status Usul")).setParent(stRow);
            Combobox cbSt = new Combobox();
            cbSt.setReadonly(true); cbSt.setWidth("160px");
            for (String s : STATUS_USUL) {
                Comboitem ci = new Comboitem(s); ci.setValue(s); cbSt.appendChild(ci);
            }
            String stVal = ex.optString("statusUsul", STATUS_USUL[0]);
            for (int si = 0; si < STATUS_USUL.length; si++) {
                if (STATUS_USUL[si].equals(stVal)) { cbSt.setSelectedIndex(si); break; }
            }
            cbSt.setDisabled(!isAdmin);
            cbSt.setParent(stRow);

            grid.setParent(butirDiv);

            butirIds.add(butir.getId());
            tbTargetLamas.add(tbTgtLama);
            tbTargetBarus.add(tbTgtBaru);
            tbAlasans.add(tbAlasan);
            cbStatuses.add(cbSt);
        }

        if (isAdmin) {
            Hbox hbBtn = new Hbox();
            hbBtn.setStyle("margin-top:12px;gap:8px;");
            hbBtn.setParent(container);

            org.zkoss.zul.Button btnSimpan = new org.zkoss.zul.Button("Simpan Usul Peningkatan");
            btnSimpan.setSclass("btn-primary");
            btnSimpan.addEventListener("onClick", new EventListener() {
                public void onEvent(Event event) throws Exception {
                    savePeningkatan(hasil.getId(), butirIds, tbTargetLamas, tbTargetBarus, tbAlasans, cbStatuses);
                }
            });
            btnSimpan.setParent(hbBtn);

            org.zkoss.zul.Button btnClose = new org.zkoss.zul.Button("Tutup");
            btnClose.addEventListener("onClick", new EventListener() {
                public void onEvent(Event event) throws Exception { win.detach(); }
            });
            btnClose.setParent(hbBtn);
        } else {
            org.zkoss.zul.Button btnClose = new org.zkoss.zul.Button("Tutup");
            btnClose.setStyle("margin-top:12px;");
            btnClose.addEventListener("onClick", new EventListener() {
                public void onEvent(Event event) throws Exception { win.detach(); }
            });
            btnClose.setParent(container);
        }
    }

    private static Textbox buildRow(Rows rows, String label, String value, boolean editable, boolean multi) {
        Row row = new Row(); row.setParent(rows);
        new Label(label).setParent(row);
        Textbox tb = new Textbox(value != null ? value : "");
        tb.setWidth("98%");
        if (multi) { tb.setRows(2); tb.setMultiline(true); }
        tb.setReadonly(!editable);
        tb.setParent(row);
        return tb;
    }

    private static void savePeningkatan(Long hasilId, List<Long> butirIds,
            List<Textbox> tbTargetLamas, List<Textbox> tbTargetBarus,
            List<Textbox> tbAlasans, List<Combobox> cbStatuses) {
        try {
            JSONArray result = new JSONArray();
            for (int i = 0; i < butirIds.size(); i++) {
                JSONObject entry = new JSONObject();
                entry.put("butirId",    butirIds.get(i));
                entry.put("targetLama", tbTargetLamas.get(i).getValue().trim());
                entry.put("targetBaru", tbTargetBarus.get(i).getValue().trim());
                entry.put("alasan",     tbAlasans.get(i).getValue().trim());
                entry.put("statusUsul", cbStatuses.get(i).getValue() != null
                        ? cbStatuses.get(i).getValue().toString() : STATUS_USUL[0]);
                result.put(entry);
            }
            KonfigurasiManager.simpanKonfigurasi(KONFIG_PREFIX + hasilId, result.toString());
            MyMessageboxConfig.show("Usul peningkatan berhasil disimpan",
                    "Info", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    /**
     * Ringkasan HTML status peningkatan (untuk embed di HasilSPMI view).
     */
    public static String buildRingkasanHtml(Long hasilId) {
        try {
            ais.database.model.Konfigurasi k = Common.getKonfigurasi(KONFIG_PREFIX + hasilId, "[]");
            if (k == null || k.getNilai() == null || k.getNilai().trim().isEmpty()
                    || "[]".equals(k.getNilai().trim())) {
                return "<div style='color:#94a3b8;font-size:11px;font-style:italic;'>"
                        + "Belum ada usul peningkatan standar untuk AMI ini.</div>";
            }
            JSONArray arr = new JSONArray(k.getNilai());
            if (arr.length() == 0) return "<div style='color:#94a3b8;font-size:11px;font-style:italic;'>"
                    + "Belum ada usul peningkatan.</div>";

            StringBuilder sb = new StringBuilder();
            sb.append("<table style='width:100%;border-collapse:collapse;font-size:11px;'>");
            sb.append("<tr style='background:#f0fdf4;'>");
            sb.append("<th style='padding:5px 8px;text-align:left;border-bottom:2px solid #bbf7d0;'>Butir ID</th>");
            sb.append("<th style='padding:5px 8px;text-align:left;border-bottom:2px solid #bbf7d0;'>Target Lama</th>");
            sb.append("<th style='padding:5px 8px;text-align:left;border-bottom:2px solid #bbf7d0;'>Target Baru</th>");
            sb.append("<th style='padding:5px 8px;text-align:left;border-bottom:2px solid #bbf7d0;'>Status</th>");
            sb.append("</tr>");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject e = arr.optJSONObject(i);
                if (e == null) continue;
                String st  = e.optString("statusUsul", "-");
                String clr = "Disetujui".equals(st) ? "#166534" : "Ditolak".equals(st) ? "#991b1b" : "#78350f";
                String bg  = "Disetujui".equals(st) ? "#dcfce7" : "Ditolak".equals(st) ? "#fee2e2" : "#fef3c7";
                sb.append("<tr>");
                sb.append("<td style='padding:4px 8px;border-bottom:1px solid #f1f5f9;'>#").append(e.optLong("butirId")).append("</td>");
                sb.append("<td style='padding:4px 8px;border-bottom:1px solid #f1f5f9;'>").append(esc(e.optString("targetLama","-"))).append("</td>");
                sb.append("<td style='padding:4px 8px;border-bottom:1px solid #f1f5f9;font-weight:700;'>").append(esc(e.optString("targetBaru","-"))).append("</td>");
                sb.append("<td style='padding:4px 8px;border-bottom:1px solid #f1f5f9;'>"
                        + "<span style='background:").append(bg).append(";color:").append(clr)
                        .append(";border-radius:4px;padding:2px 6px;font-weight:700;'>")
                        .append(esc(st)).append("</span></td>");
                sb.append("</tr>");
            }
            sb.append("</table>");
            return sb.toString();
        } catch (Exception ex) {
            return "<div style='color:red;font-size:11px;'>Gagal memuat ringkasan peningkatan: "
                    + esc(ex.getMessage()) + "</div>";
        }
    }

    private static String badge(String label, String value, String clr, String bg) {
        return "<div style='background:" + bg + ";color:" + clr + ";border-radius:6px;"
                + "padding:6px 12px;font-size:11px;font-weight:700;'>"
                + label + ": " + value + "</div>";
    }

    private static void appendHtml(Component parent, String html) {
        new Html(html).setParent(parent);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
