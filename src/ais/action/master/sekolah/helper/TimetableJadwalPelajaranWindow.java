package ais.action.master.sekolah.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JamPelajaran;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.JenisJadwalPelajaran;
import ais.database.model.sekolah.KurikulumPunyaMatapelajaran;
import ais.database.model.sekolah.KurikulumSekolah;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Timetable jadwal mingguan untuk modul Jadwal Pelajaran Sekolah.
 *
 * Fitur:
 * - Grid Hari x Jam Pelajaran dengan drag-and-drop ZK native
 * - Panel "Belum Terjadwal" untuk kartu yang belum dipasang
 * - Tampilan "Per Kelas" (default) dan "Per Guru"
 * - Kunci kartu agar tidak bisa dipindah secara tidak sengaja
 * - Deteksi konflik: guru yang mengajar di 2 kelas di slot yang sama
 * - Tombol "Periksa Konflik" untuk validasi data
 * - Simpan perubahan ke database
 */
public class TimetableJadwalPelajaranWindow extends MyWindow {

    private static final long serialVersionUID = 1L;

    private static final String DND = "tt";
    private static final String[] HARI = {
        "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"
    };
    private static final String[] CARD_COLORS = {
        "#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6",
        "#EC4899", "#14B8A6", "#F97316", "#6366F1", "#84CC16",
        "#0EA5E9", "#A855F7", "#D97706", "#059669", "#DC2626"
    };

    // 0 = Per Kelas, 1 = Per Guru
    private int viewMode = 0;

    private Combobox cbTA, cbSmt, cbKelas, cbGuru, cbKurikulum;
    private Div filterKelasRow, filterGuruRow;
    private Button btnViewKelas, btnViewGuru;
    private Div palette, gridArea;

    /** jpId → {newHari, newJamId} — perubahan belum disimpan */
    private final Map<Long, Object[]>  changes     = new LinkedHashMap<Long, Object[]>();
    /** jpId kartu yang dikunci (tidak bisa dipindah) */
    private final Set<Long>            locked      = new HashSet<Long>();
    /** jpId kartu yang mengalami konflik guru lintas kelas */
    private final Set<Long>            conflictIds = new HashSet<Long>();

    private int               colorCounter;
    private final Map<Long, Integer> colorMap = new HashMap<Long, Integer>();

    // ── Konstruktor ──────────────────────────────────────────────────────────

    public TimetableJadwalPelajaranWindow() {
        super("Timetable Jadwal Pelajaran", "none", false);
        try {
            setWidth("100%");
            setHeight("100%");
            setContentStyle("padding:0;overflow:hidden;");
            injectStyle();
            buildLayout();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    // ── CSS ──────────────────────────────────────────────────────────────────

    private void injectStyle() {
        Html css = new Html();
        css.setContent(
            "<style>" +
            ".aisttwrap{display:flex;flex-direction:column;height:100%;" +
              "font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;" +
              "font-size:12px;}" +
            ".aisttbar{display:flex;align-items:center;gap:6px;flex-wrap:wrap;" +
              "padding:8px 12px;border-bottom:1px solid #e2e8f0;background:#f8fafc;" +
              "color:#374151;}" +
            ".aisttviewtab{padding:4px 12px;border-radius:4px;" +
              "border:1px solid #cbd5e1;background:#fff;color:#374151;" +
              "cursor:pointer;font-size:11px;font-weight:500;}" +
            ".aisttviewtab.active{background:#3B82F6;color:#fff;border-color:#2563EB;}" +
            ".aisttsep{width:1px;height:22px;background:#cbd5e1;margin:0 3px;" +
              "flex-shrink:0;}" +
            ".aisttbody{display:flex;flex:1;overflow:hidden;min-height:0;}" +
            /* Panel kiri */
            ".aisttpanel{width:180px;min-width:180px;border-right:1px solid #e2e8f0;" +
              "display:flex;flex-direction:column;background:#fafafa;}" +
            ".aisttpanelhdr{font-size:11px;font-weight:600;color:#64748b;" +
              "padding:6px 10px;border-bottom:1px solid #e2e8f0;background:#f1f5f9;" +
              "white-space:nowrap;}" +
            ".aisttpalette{flex:1;overflow-y:auto;padding:6px;min-height:80px;}" +
            ".aisttgridarea{flex:1;overflow:auto;padding:10px;}" +
            /* Tabel grid */
            ".aisttgridtbl{display:table;border-collapse:collapse;" +
              "width:100%;min-width:680px;}" +
            ".aistttblrow{display:table-row;}" +
            ".aistthdrcell{display:table-cell;border:1px solid #cbd5e1;" +
              "padding:6px 4px;font-size:12px;font-weight:600;color:#1e293b;" +
              "text-align:center;background:#e2e8f0;white-space:nowrap;" +
              "min-width:105px;}" +
            ".aisttjamcell{display:table-cell;border:1px solid #cbd5e1;" +
              "padding:4px 3px;font-size:10px;text-align:center;color:#64748b;" +
              "background:#f8fafc;vertical-align:middle;min-width:68px;max-width:80px;" +
              "white-space:pre-line;line-height:1.4;}" +
            ".aisttcell{display:table-cell;border:1px solid #cbd5e1;" +
              "vertical-align:top;min-width:105px;padding:3px;min-height:60px;}" +
            ".aisttcell-drop{background:#eff6ff!important;}" +
            /* Kartu jadwal */
            ".aisttcard{border-radius:5px;padding:4px 34px 4px 6px;margin:2px 0;" +
              "font-size:11px;color:#fff;cursor:grab;position:relative;" +
              "word-break:break-word;line-height:1.4;user-select:none;}" +
            ".aisttcard .cn{font-weight:600;}" +
            ".aisttcard .cg{font-size:10px;opacity:.85;margin-top:2px;}" +
            /* Tombol pada kartu */
            ".aisttcard .cdel{position:absolute;top:2px;right:18px;" +
              "cursor:pointer;font-size:13px;line-height:1;color:#fff;" +
              "opacity:.65;padding:0;border:none;background:transparent;" +
              "font-weight:bold;}" +
            ".aisttcard .cdel:hover{opacity:1;}" +
            ".aisttcard .clock{position:absolute;top:3px;right:2px;" +
              "cursor:pointer;font-size:11px;line-height:1;color:#fff;" +
              "opacity:.7;padding:0;border:none;background:transparent;}" +
            ".aisttcard .clock:hover{opacity:1;}" +
            /* State: dikunci */
            ".aisttcard.locked{cursor:not-allowed;filter:brightness(.88);" +
              "outline:2px solid rgba(255,255,255,.5);outline-offset:-2px;}" +
            /* State: konflik guru */
            ".aisttcard.conflict{outline:2px solid #ef4444;" +
              "outline-offset:1px;box-shadow:0 0 0 4px rgba(239,68,68,.2);}" +
            /* Kartu Mapel dari kurikulum (belum ada jadwal) */
            ".aisttmk{background:#ffffff!important;color:#334155!important;" +
              "border:1px dashed #94a3b8;cursor:grab;}" +
            ".aisttmk .cn{color:#1e293b;}" +
            ".aisttmk .cg{color:#64748b;}" +
            ".aisttcell{cursor:pointer;}" +
            ".aisttchooserrow{padding:6px 8px;border-bottom:1px solid #eef2f7;" +
              "cursor:pointer;font-size:12px;}" +
            ".aisttchooserrow:hover{background:#eff6ff;}" +
            /* Hint/empty */
            ".aistthint{color:#94a3b8;font-size:12px;padding:24px 12px;" +
              "text-align:center;font-style:italic;line-height:1.6;}" +
            /* Legend bawah */
            ".aisttlegend{font-size:10px;padding:4px 10px;color:#64748b;" +
              "display:flex;gap:10px;align-items:center;" +
              "border-top:1px solid #e2e8f0;background:#f8fafc;" +
              "flex-wrap:wrap;}" +
            ".aisttleg-item{display:flex;align-items:center;gap:4px;}" +
            ".aisttleg-box{width:12px;height:12px;border-radius:2px;flex-shrink:0;}" +
            "</style>"
        );
        css.setParent(this);
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private void buildLayout() {
        Div wrap = new Div();
        wrap.setSclass("aisttwrap");
        wrap.setParent(this);

        buildFilterBar(wrap);

        Div body = new Div();
        body.setSclass("aisttbody");
        body.setParent(wrap);

        buildPalettePanel(body);

        gridArea = new Div();
        gridArea.setSclass("aisttgridarea");
        gridArea.setParent(body);
        hint(gridArea, "Pilih filter di atas, lalu klik Tampilkan.");

        buildLegend(wrap);
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private void buildFilterBar(Div wrap) {
        Div bar = new Div();
        bar.setSclass("aisttbar");
        bar.setParent(wrap);

        // Tampilan toggle
        btnViewKelas = new Button("Per Kelas");
        btnViewKelas.setSclass("aisttviewtab active");
        btnViewKelas.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception { switchView(0); }
        });
        btnViewKelas.setParent(bar);

        btnViewGuru = new Button("Per Guru");
        btnViewGuru.setSclass("aisttviewtab");
        btnViewGuru.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception { switchView(1); }
        });
        btnViewGuru.setParent(bar);

        sep(bar);

        new Label(ais.common.Common.getBahasaConfig("TA:")).setParent(bar);
        cbTA = new Combobox();
        cbTA.setWidth("110px");
        cbTA.setReadonly(true);
        Common.generateTahunAjaran(cbTA);
        cbTA.setParent(bar);

        new Label(ais.common.Common.getBahasaConfig("Smt:")).setParent(bar);
        cbSmt = new Combobox();
        cbSmt.setWidth("78px");
        cbSmt.setReadonly(true);
        cbSmt.appendItem("Ganjil").setValue(Integer.valueOf(1));
        cbSmt.appendItem("Genap").setValue(Integer.valueOf(2));
        cbSmt.setSelectedIndex(0);
        cbSmt.setParent(bar);

        // Filter kelas (default tampil)
        filterKelasRow = new Div();
        filterKelasRow.setStyle("display:flex;align-items:center;gap:6px;");
        filterKelasRow.setParent(bar);
        new Label(ais.common.Common.getBahasaConfig("Kelas:")).setParent(filterKelasRow);
        cbKelas = new Combobox();
        cbKelas.setWidth("140px");
        cbKelas.setReadonly(true);
        Session s0 = HibernateUtil.currentSession();
        List kList = s0.createCriteria(KelasSiswa.class)
            .add(Restrictions.eq("aktif", Boolean.TRUE))
            .addOrder(Order.asc("nama"))
            .setMaxResults(300).list();
        for (Object o : kList) {
            KelasSiswa k = (KelasSiswa) o;
            cbKelas.appendItem(k.getNama()).setValue(k.getId());
        }
        if (!kList.isEmpty()) cbKelas.setSelectedIndex(0);
        cbKelas.setParent(filterKelasRow);

        // Filter Kurikulum: bila dipilih, semua Mata Pelajaran kurikulum yang belum ada jadwal ikut tampil
        // di panel "Belum Terjadwal" (kartu putih bergaris putus-putus) dan dapat diseret ke sel.
        new Label(ais.common.Common.getBahasaConfig("Kurikulum:")).setParent(filterKelasRow);
        cbKurikulum = new Combobox();
        cbKurikulum.setWidth("155px");
        cbKurikulum.setReadonly(true);
        cbKurikulum.appendItem("= Tanpa kurikulum =").setValue(null);
        List kurList = s0.createCriteria(KurikulumSekolah.class)
            .addOrder(Order.desc("id")).setMaxResults(200).list();
        for (Object o : kurList) {
            KurikulumSekolah k = (KurikulumSekolah) o;
            cbKurikulum.appendItem(k.getNama()).setValue(k.getId());
        }
        cbKurikulum.setSelectedIndex(0);
        cbKurikulum.setParent(filterKelasRow);

        // Filter guru (tersembunyi)
        filterGuruRow = new Div();
        filterGuruRow.setStyle("display:none;align-items:center;gap:6px;");
        filterGuruRow.setParent(bar);
        new Label(ais.common.Common.getBahasaConfig("Guru:")).setParent(filterGuruRow);
        cbGuru = new Combobox();
        cbGuru.setWidth("155px");
        cbGuru.setReadonly(true);
        List gList = s0.createCriteria(Guru.class)
            .add(Restrictions.eq("aktif", Boolean.TRUE))
            .addOrder(Order.asc("nama"))
            .setMaxResults(300).list();
        for (Object o : gList) {
            Guru g = (Guru) o;
            cbGuru.appendItem(g.getNama()).setValue(g.getId());
        }
        if (!gList.isEmpty()) cbGuru.setSelectedIndex(0);
        cbGuru.setParent(filterGuruRow);

        sep(bar);

        btn(bar, "Tampilkan", "btn btn-primary btn-sm", new EventListener() {
            public void onEvent(Event e) throws Exception { doLoad(); }
        });
        btn(bar, "Simpan", "btn btn-success btn-sm", new EventListener() {
            public void onEvent(Event e) throws Exception { doSave(); }
        });
        btn(bar, "Periksa Konflik", "btn btn-warning btn-sm", new EventListener() {
            public void onEvent(Event e) throws Exception { doCheckConflict(); }
        });
        btn(bar, "Reset", "btn btn-default btn-sm", new EventListener() {
            public void onEvent(Event e) throws Exception {
                changes.clear(); locked.clear(); conflictIds.clear(); doLoad();
            }
        });

        sep(bar);

        btn(bar, "Buat Waktu Default", "btn btn-info btn-sm", new EventListener() {
            public void onEvent(Event e) throws Exception { buatWaktuDefault(); }
        });
        btn(bar, "Kelola Jam", "btn btn-default btn-sm", new EventListener() {
            public void onEvent(Event e) throws Exception { bukaKelolaJam(); }
        });
        btn(bar, "Cetak", "btn btn-default btn-sm", new EventListener() {
            public void onEvent(Event e) throws Exception { cetakJadwal(); }
        });
    }

    private void buildPalettePanel(Div body) {
        Div panel = new Div();
        panel.setSclass("aisttpanel");
        panel.setParent(body);

        Div hdr = new Div();
        hdr.setSclass("aisttpanelhdr");
        new Label(ais.common.Common.getBahasaConfig("Belum Terjadwal")).setParent(hdr);
        hdr.setParent(panel);

        palette = new Div();
        palette.setSclass("aisttpalette");
        palette.setDroppable(DND);
        palette.addEventListener("onDrop", new EventListener() {
            public void onEvent(Event ev) throws Exception {
                DropEvent de = (DropEvent) ev;
                if (de.getDragged() instanceof Div) cardToPalette((Div) de.getDragged());
            }
        });
        palette.setParent(panel);
        hint(palette, "Pilih filter\ndan klik Tampilkan");
    }

    private void buildLegend(Div wrap) {
        Div leg = new Div();
        leg.setSclass("aisttlegend");
        leg.setParent(wrap);

        legItem(leg, "🔒", null, "= Kartu dikunci (tidak dapat dipindah)");
        legSep(leg);
        Div dot = new Div();
        dot.setSclass("aisttleg-box");
        dot.setStyle("background:#3B82F6;outline:2px solid #ef4444;outline-offset:1px;");
        dot.setParent(leg);
        new Label(ais.common.Common.getBahasaConfig("= Konflik guru (guru sama di slot yang sama pada kelas berbeda)")).setParent(leg);
    }

    // ── Switch view ───────────────────────────────────────────────────────────

    private void switchView(int mode) {
        viewMode = mode;
        if (mode == 0) {
            btnViewKelas.setSclass("aisttviewtab active");
            btnViewGuru.setSclass("aisttviewtab");
            filterKelasRow.setStyle("display:flex;align-items:center;gap:6px;");
            filterGuruRow.setStyle("display:none;align-items:center;gap:6px;");
        } else {
            btnViewKelas.setSclass("aisttviewtab");
            btnViewGuru.setSclass("aisttviewtab active");
            filterKelasRow.setStyle("display:none;align-items:center;gap:6px;");
            filterGuruRow.setStyle("display:flex;align-items:center;gap:6px;");
        }
        gridArea.getChildren().clear();
        palette.getChildren().clear();
        hint(gridArea, "Klik Tampilkan untuk memuat data.");
        changes.clear();
        locked.clear();
        conflictIds.clear();
    }

    // ── Data load ─────────────────────────────────────────────────────────────

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void doLoad() {
        if (cbTA.getSelectedItem() == null || cbSmt.getSelectedItem() == null) {
            msgbox("Pilih Tahun Ajaran dan Semester terlebih dahulu.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }
        if (viewMode == 0 && cbKelas.getSelectedItem() == null) {
            msgbox("Pilih Kelas terlebih dahulu.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }
        if (viewMode == 1 && cbGuru.getSelectedItem() == null) {
            msgbox("Pilih Guru terlebih dahulu.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }

        changes.clear();
        colorMap.clear();
        colorCounter = 0;
        conflictIds.clear();

        final String ta  = (String)  cbTA.getSelectedItem().getValue();
        final int    smt = (Integer) cbSmt.getSelectedItem().getValue();

        Session s = HibernateUtil.currentSession();

        // Deteksi konflik guru lintas kelas
        buildConflictIds(ta, smt, s);

        // Muat daftar jam pelajaran
        List<JamPelajaran> jamList = loadJamList(s, ta);

        if (viewMode == 0) {
            doLoadPerKelas(ta, smt, s, jamList);
        } else {
            doLoadPerGuru(ta, smt, s, jamList);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void doLoadPerKelas(String ta, int smt, Session s, List<JamPelajaran> jamList) {
        Long kelasId = (Long) cbKelas.getSelectedItem().getValue();
        KelasSiswa kelas = (KelasSiswa) s.get(KelasSiswa.class, kelasId);
        if (kelas == null) {
            msgbox("Kelas tidak ditemukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }

        List<JadwalPelajaran> jpAll = s.createCriteria(JadwalPelajaran.class)
            .add(Restrictions.eq("tahunAjaran", ta))
            .add(Restrictions.eq("semester", Integer.valueOf(smt)))
            .add(Restrictions.eq("kelas", kelas))
            .list();

        Map<String, JadwalPelajaran> cellMap = new HashMap<String, JadwalPelajaran>();
        List<JadwalPelajaran> unscheduled = new ArrayList<JadwalPelajaran>();

        for (JadwalPelajaran jp : jpAll) {
            String hari = jp.getHari();
            JamPelajaran jam = jp.getJamPelajaran();
            if (hari != null && !hari.trim().isEmpty() && jam != null) {
                String key = hari.trim() + "_" + jam.getId();
                if (!cellMap.containsKey(key)) {
                    cellMap.put(key, jp);
                } else {
                    unscheduled.add(jp);
                }
            } else {
                unscheduled.add(jp);
            }
        }

        rebuildPalette(unscheduled, false);
        rebuildGrid(jamList, cellMap, false);

        // Filter Kurikulum: tampilkan Mapel kurikulum yang belum punya jadwal untuk kelas ini.
        Long kurId = (cbKurikulum != null && cbKurikulum.getSelectedItem() != null)
            ? (Long) cbKurikulum.getSelectedItem().getValue() : null;
        if (kurId != null) appendMpCards(kurId, kelas, jpAll, s);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void doLoadPerGuru(String ta, int smt, Session s, List<JamPelajaran> jamList) {
        Long guruId = (Long) cbGuru.getSelectedItem().getValue();
        Guru guru = (Guru) s.get(Guru.class, guruId);
        if (guru == null) {
            msgbox("Guru tidak ditemukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }

        List<JadwalPelajaran> jpAll = s.createCriteria(JadwalPelajaran.class)
            .add(Restrictions.eq("tahunAjaran", ta))
            .add(Restrictions.eq("semester", Integer.valueOf(smt)))
            .add(Restrictions.eq("guru", guru))
            .list();

        Map<String, JadwalPelajaran> cellMap = new HashMap<String, JadwalPelajaran>();
        List<JadwalPelajaran> unscheduled = new ArrayList<JadwalPelajaran>();

        for (JadwalPelajaran jp : jpAll) {
            String hari = jp.getHari();
            JamPelajaran jam = jp.getJamPelajaran();
            if (hari != null && !hari.trim().isEmpty() && jam != null) {
                String key = hari.trim() + "_" + jam.getId();
                if (!cellMap.containsKey(key)) {
                    cellMap.put(key, jp);
                } else {
                    // Sama slot → konflik, tampilkan di palette
                    unscheduled.add(jp);
                }
            } else {
                unscheduled.add(jp);
            }
        }

        rebuildPalette(unscheduled, true);
        rebuildGrid(jamList, cellMap, true);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<JamPelajaran> loadJamList(Session s, String ta) {
        if (viewMode == 0 && cbKelas.getSelectedItem() != null) {
            Long kelasId = (Long) cbKelas.getSelectedItem().getValue();
            KelasSiswa kelas = (KelasSiswa) s.get(KelasSiswa.class, kelasId);
            if (kelas != null) {
                try {
                    Object sekolah = kelas.getSekolah();
                    if (sekolah != null) {
                        List<JamPelajaran> list = s.createCriteria(JamPelajaran.class)
                            .add(Restrictions.eq("sekolah", sekolah))
                            .add(Restrictions.eq("aktif", Boolean.TRUE))
                            .addOrder(Order.asc("waktuMulaiD"))
                            .list();
                        if (!list.isEmpty()) return list;
                    }
                } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TimetableJadwalPelajaranWindow.java:536"); /* fallback */ }
            }
        }
        return s.createCriteria(JamPelajaran.class)
            .add(Restrictions.eq("aktif", Boolean.TRUE))
            .addOrder(Order.asc("waktuMulaiD"))
            .setMaxResults(20)
            .list();
    }

    /**
     * Bangun set jpId yang guru-nya konflik:
     * guru yang sama dijadwalkan di lebih dari satu kelas pada slot hari+jam yang sama.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void buildConflictIds(String ta, int smt, Session s) {
        conflictIds.clear();
        try {
            List<JadwalPelajaran> all = s.createCriteria(JadwalPelajaran.class)
                .add(Restrictions.eq("tahunAjaran", ta))
                .add(Restrictions.eq("semester", Integer.valueOf(smt)))
                .list();

            // key: guruId_hari_jamId → list jpId
            Map<String, List<Long>> slotMap = new HashMap<String, List<Long>>();
            for (JadwalPelajaran jp : all) {
                if (jp.getGuru() == null) continue;
                if (jp.getHari() == null || jp.getHari().trim().isEmpty()) continue;
                if (jp.getJamPelajaran() == null) continue;
                String key = jp.getGuru().getId() + "_"
                    + jp.getHari().trim() + "_"
                    + jp.getJamPelajaran().getId();
                List<Long> ids = slotMap.get(key);
                if (ids == null) { ids = new ArrayList<Long>(); slotMap.put(key, ids); }
                ids.add(jp.getId());
            }
            for (List<Long> ids : slotMap.values()) {
                if (ids.size() > 1) conflictIds.addAll(ids);
            }
        } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TimetableJadwalPelajaranWindow.java:575"); /* non-fatal, abaikan */ }
    }

    // ── Grid build ────────────────────────────────────────────────────────────

    private void rebuildPalette(List<JadwalPelajaran> items, boolean showKelas) {
        palette.getChildren().clear();
        if (items.isEmpty()) {
            hint(palette, "Semua sudah terjadwal ✓");
        } else {
            for (JadwalPelajaran jp : items) {
                mkCard(jp, showKelas).setParent(palette);
            }
        }
    }

    private void rebuildGrid(List<JamPelajaran> jamList,
                             Map<String, JadwalPelajaran> cellMap,
                             boolean showKelas) {
        gridArea.getChildren().clear();
        if (jamList.isEmpty()) {
            hint(gridArea, "Tidak ada data Jam Pelajaran.\n"
                + "Buat Jam Pelajaran terlebih dahulu di master data sekolah.");
            return;
        }

        Div tbl = new Div();
        tbl.setSclass("aisttgridtbl");
        tbl.setParent(gridArea);

        // Header row
        Div hdrRow = new Div();
        hdrRow.setSclass("aistttblrow");
        hdrRow.setParent(tbl);
        hdrCell(hdrRow, "Jam / Hari");
        for (String h : HARI) hdrCell(hdrRow, h);

        // Baris per jam pelajaran
        for (JamPelajaran jam : jamList) {
            Div row = new Div();
            row.setSclass("aistttblrow");
            row.setParent(tbl);

            // Label jam
            Div jamCell = new Div();
            jamCell.setSclass("aisttjamcell");
            String nama  = jam.getNama();
            String mulai = jam.getMulaiS();
            String sel   = jam.getSampaiS();
            StringBuilder lbl = new StringBuilder();
            lbl.append(nama != null && !nama.trim().isEmpty() ? nama : "Jam");
            if (mulai != null && !mulai.isEmpty()) {
                lbl.append("\n").append(mulai);
                if (sel != null && !sel.isEmpty()) lbl.append("-").append(sel);
            }
            new Label(lbl.toString()).setParent(jamCell);
            jamCell.setParent(row);

            // Satu sel per hari
            for (String hari : HARI) {
                String key  = hari + "_" + jam.getId();
                Div    cell = mkCell(hari, jam.getId());
                JadwalPelajaran jp = cellMap.get(key);
                if (jp != null) mkCard(jp, showKelas).setParent(cell);
                cell.setParent(row);
            }
        }
    }

    private void hdrCell(Div row, String text) {
        Div c = new Div();
        c.setSclass("aistthdrcell");
        new Label(text).setParent(c);
        c.setParent(row);
    }

    private Div mkCell(final String hari, final Long jamId) {
        final Div cell = new Div();
        cell.setSclass("aisttcell");
        cell.setDroppable(DND);
        cell.setAttribute("hari",  hari);
        cell.setAttribute("jamId", jamId);
        cell.addEventListener("onDrop", new EventListener() {
            public void onEvent(Event ev) throws Exception {
                DropEvent de = (DropEvent) ev;
                if (de.getDragged() instanceof Div) cardToCell((Div) de.getDragged(), cell);
            }
        });
        // Klik sel -> pilih item dari "Belum Terjadwal" untuk ditambahkan ke slot ini.
        cell.addEventListener("onClick", new EventListener() {
            public void onEvent(Event ev) throws Exception { bukaTambahKeCell(cell); }
        });
        return cell;
    }

    /**
     * Buat kartu jadwal.
     *
     * @param showKelas true = tampilkan nama kelas di baris 2 (mode Per Guru);
     *                  false = tampilkan nama guru (mode Per Kelas)
     */
    private Div mkCard(final JadwalPelajaran jp, final boolean showKelas) {
        final Long    jpId      = jp.getId();
        final boolean isLocked  = locked.contains(jpId);
        final boolean isConflict = conflictIds.contains(jpId);

        final Div card = new Div();
        card.setAttribute("jpId", jpId);
        card.setDraggable(isLocked ? "false" : DND);

        Long   colorKey = jp.getMatapelajaran() != null ? jp.getMatapelajaran().getId() : jpId;
        String bg       = CARD_COLORS[color(colorKey)];
        card.setStyle("background:" + bg + ";");

        StringBuilder sc = new StringBuilder("aisttcard");
        if (isLocked)   sc.append(" locked");
        if (isConflict) sc.append(" conflict");
        card.setSclass(sc.toString());

        // Nama mata pelajaran (+ ikon kunci jika locked)
        final String mpNama = jp.getMatapelajaran() != null ? jp.getMatapelajaran().getNama() : "—";
        Div cn = new Div();
        cn.setSclass("cn");
        new Label((isLocked ? "🔒 " : "") + mpNama).setParent(cn);
        cn.setParent(card);

        // Baris 2: nama kelas atau nama guru
        if (showKelas) {
            if (jp.getKelas() != null) {
                Div cg = new Div(); cg.setSclass("cg");
                new Label(jp.getKelas().getNama()).setParent(cg);
                cg.setParent(card);
            }
        } else {
            if (jp.getGuru() != null && jp.getGuru().getNama() != null) {
                Div cg = new Div(); cg.setSclass("cg");
                new Label(jp.getGuru().getNama()).setParent(cg);
                cg.setParent(card);
            }
        }

        // Tombol × — lepas ke palette
        final Toolbarbutton del = new Toolbarbutton("×");
        del.setSclass("cdel");
        del.setTooltiptext("Pindah ke Belum Terjadwal");
        del.addEventListener("onClick", new EventListener() {
            public void onEvent(Event ev) throws Exception {
                ev.stopPropagation();
                if (!locked.contains((Long) card.getAttribute("jpId"))) cardToPalette(card);
            }
        });
        del.setParent(card);

        // Tombol kunci / buka kunci
        final Toolbarbutton lockBtn = new Toolbarbutton(isLocked ? "🔓" : "🔒");
        lockBtn.setSclass("clock");
        lockBtn.setTooltiptext(isLocked ? "Buka kunci" : "Kunci posisi ini");
        lockBtn.addEventListener("onClick", new EventListener() {
            public void onEvent(Event ev) throws Exception {
                ev.stopPropagation();
                toggleLock(card, lockBtn, del, mpNama);
            }
        });
        lockBtn.setParent(card);

        // Klik badan kartu -> buka dialog Edit (guru).
        card.addEventListener("onClick", new EventListener() {
            public void onEvent(Event ev) throws Exception {
                ev.stopPropagation();
                bukaEditJadwal((Long) card.getAttribute("jpId"), card, showKelas);
            }
        });

        return card;
    }

    // ── Kunci / buka kunci ────────────────────────────────────────────────────

    private void toggleLock(Div card, Toolbarbutton lockBtn, Toolbarbutton del, String mpNama) {
        Long jpId = (Long) card.getAttribute("jpId");
        if (locked.contains(jpId)) {
            // Buka kunci
            locked.remove(jpId);
            card.setDraggable(DND);
            String sc = card.getSclass().replace(" locked", "").trim();
            card.setSclass(sc);
            lockBtn.setLabel("🔒");
            lockBtn.setTooltiptext("Kunci posisi ini");
            // Perbarui nama (hapus ikon kunci)
            setCardName(card, mpNama);
        } else {
            // Kunci
            locked.add(jpId);
            card.setDraggable("false");
            if (!card.getSclass().contains("locked")) card.setSclass(card.getSclass() + " locked");
            lockBtn.setLabel("🔓");
            lockBtn.setTooltiptext("Buka kunci");
            setCardName(card, "🔒 " + mpNama);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setCardName(Div card, String nama) {
        for (Object child : card.getChildren()) {
            if (child instanceof Div) {
                Div d = (Div) child;
                if ("cn".equals(d.getSclass())) {
                    for (Object lc : d.getChildren()) {
                        if (lc instanceof Label) { ((Label) lc).setValue(nama); return; }
                    }
                }
            }
        }
    }

    // ── DnD handlers ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void cardToCell(Div card, Div cell) {
        // Kartu Mapel dari kurikulum (belum ada jadwal) -> buat JadwalPelajaran baru pada slot ini.
        if (card.getAttribute("jpId") == null && card.getAttribute("mpId") != null) {
            buatJadwalDariMp(card, cell);
            return;
        }
        Long jpId = (Long) card.getAttribute("jpId");
        if (jpId == null || locked.contains(jpId)) return; // dikunci/tak valid, abaikan

        // Per-Kelas: satu kartu per sel
        if (viewMode == 0) {
            for (Object child : new ArrayList<Object>(cell.getChildren())) {
                if (child instanceof Div && DND.equals(((Div) child).getDraggable())) {
                    msgbox("Slot sudah terisi! Kosongkan slot tujuan terlebih dahulu.",
                        "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                    return;
                }
            }
        }

        card.detach();
        card.setParent(cell);
        String hari  = (String) cell.getAttribute("hari");
        Long   jamId = (Long)   cell.getAttribute("jamId");
        changes.put(jpId, new Object[]{hari, jamId});
    }

    @SuppressWarnings("unchecked")
    private void cardToPalette(Div card) {
        Long jpId = (Long) card.getAttribute("jpId");
        if (locked.contains(jpId)) return;

        // Hapus label placeholder
        for (Object c : new ArrayList<Object>(palette.getChildren())) {
            if (c instanceof Label) ((Component) c).detach();
        }
        card.detach();
        card.setParent(palette);
        changes.put(jpId, new Object[]{null, null});
    }

    // ── Periksa Konflik ───────────────────────────────────────────────────────

    private void doCheckConflict() {
        if (cbTA.getSelectedItem() == null || cbSmt.getSelectedItem() == null) {
            msgbox("Pilih TA dan Semester terlebih dahulu.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }
        String ta  = (String)  cbTA.getSelectedItem().getValue();
        int    smt = (Integer) cbSmt.getSelectedItem().getValue();
        buildConflictIds(ta, smt, HibernateUtil.currentSession());

        if (conflictIds.isEmpty()) {
            msgbox("Tidak ditemukan konflik jadwal guru. Data sudah valid.",
                "Hasil Pemeriksaan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        } else {
            msgbox("Ditemukan " + conflictIds.size() + " slot berkonflik.\n\n"
                + "Konflik terjadi ketika guru yang sama dijadwalkan di dua kelas "
                + "berbeda pada hari dan jam yang sama.\n\n"
                + "Klik Tampilkan untuk melihat kartu berkonflik (ditandai garis merah).",
                "Konflik Ditemukan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
        }
    }

    // ── Simpan ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void doSave() {
        if (changes.isEmpty()) {
            msgbox("Tidak ada perubahan untuk disimpan.", "Info",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }
        try {
            Session s = HibernateUtil.currentSession();
            int saved = 0;
            for (Map.Entry<Long, Object[]> entry : changes.entrySet()) {
                JadwalPelajaran jp = (JadwalPelajaran) s.get(JadwalPelajaran.class, entry.getKey());
                if (jp == null) continue;
                String newHari  = (String) entry.getValue()[0];
                Long   newJamId = (Long)   entry.getValue()[1];
                jp.setHari(newHari);
                jp.setJamPelajaran(newJamId != null
                    ? (JamPelajaran) s.get(JamPelajaran.class, newJamId)
                    : null);
                s.saveOrUpdate(jp);
                saved++;
            }
            changes.clear();
            msgbox(saved + " jadwal berhasil disimpan.", "Berhasil",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    // ── Waktu/Jam Pelajaran: buat default, kelola, cetak ────────────────────────

    /** Preset waktu default (10 slot 49 menit, format HH:mm) sesuai template jadwal. */
    private static final String[][] JAM_DEFAULT = {
        {"Jam ke-1","07:30","08:19"}, {"Jam ke-2","08:20","09:09"},
        {"Jam ke-3","09:10","09:59"}, {"Jam ke-4","10:00","10:49"},
        {"Jam ke-5","10:50","11:39"}, {"Jam ke-6","13:10","13:59"},
        {"Jam ke-7","14:00","14:49"}, {"Jam ke-8","14:50","15:39"},
        {"Jam ke-9","16:10","16:59"}, {"Jam ke-10","17:00","17:49"}
    };

    /** Mengambil satu JenisJadwalPelajaran default (untuk sekolah tsb bila ada, jika tidak, yang mana saja). */
    @SuppressWarnings({"rawtypes","unchecked"})
    private static JenisJadwalPelajaran jenisDefault(Session s, Sekolah sekolah) {
        try {
            JenisJadwalPelajaran j = (JenisJadwalPelajaran) s.createCriteria(JenisJadwalPelajaran.class)
                .add(Restrictions.eq("sekolah", sekolah)).setMaxResults(1).uniqueResult();
            if (j != null) return j;
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TimetableJadwalPelajaranWindow.java:908"); /* fallback */ }
        try {
            return (JenisJadwalPelajaran) s.createCriteria(JenisJadwalPelajaran.class).setMaxResults(1).uniqueResult();
        } catch (Exception e) { return null; }
    }

    /** Membuat JamPelajaran default (10 slot) untuk sekolah dari kelas terpilih (Kelas WAJIB dipilih dulu). */
    @SuppressWarnings({"rawtypes","unchecked"})
    private void buatWaktuDefault() throws Exception {
        if (cbKelas == null || cbKelas.getSelectedItem() == null || cbKelas.getSelectedItem().getValue() == null) {
            msgbox("Pilih Kelas terlebih dahulu (Jam Pelajaran dibuat per sekolah dari kelas terpilih).",
                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }
        final Long kelasId = (Long) cbKelas.getSelectedItem().getValue();
        MyMessageboxConfig.show(
            "Buat waktu/jam default (10 slot, 07:30-17:49) untuk sekolah dari kelas terpilih? "
                + "Jam yang sudah ada tidak akan diduplikasi.",
            "Konfirmasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
            new EventListener() {
                public void onEvent(Event e) throws Exception {
                    if (Integer.parseInt(e.getData().toString()) != MyMessageboxConfig.OK) return;
                    try {
                        Session s = HibernateUtil.currentSession();
                        KelasSiswa kelas = (KelasSiswa) s.get(KelasSiswa.class, kelasId);
                        if (kelas == null || kelas.getSekolah() == null) {
                            msgbox("Sekolah dari kelas tidak ditemukan.", "Peringatan",
                                MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                            return;
                        }
                        Sekolah sekolah = kelas.getSekolah();
                        JenisJadwalPelajaran jenis = jenisDefault(s, sekolah);
                        if (jenis == null) {
                            msgbox("Belum ada 'Jenis Jadwal Pelajaran'. Buat satu dahulu di master data, lalu ulangi.",
                                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                            return;
                        }
                        Set<String> ada = new HashSet<String>();
                        List ex = s.createCriteria(JamPelajaran.class)
                            .add(Restrictions.eq("sekolah", sekolah)).list();
                        for (Object o : ex) {
                            String ms = ((JamPelajaran) o).getMulaiS();
                            if (ms != null) ada.add(ms.trim());
                        }
                        int dibuat = 0;
                        for (int i = 0; i < JAM_DEFAULT.length; i++) {
                            String[] d = JAM_DEFAULT[i];
                            if (ada.contains(d[1])) continue;
                            JamPelajaran jp = new JamPelajaran();
                            jp.setNama(d[0]);
                            jp.setSekolah(sekolah);
                            jp.setJenisJadwalPelajaran(jenis);
                            jp.setAktif(Boolean.TRUE);
                            jp.setMulaiS(d[1]);
                            jp.setSampaiS(d[2]);
                            s.save(jp);
                            dibuat++;
                        }
                        s.flush();
                        msgbox(dibuat + " jam pelajaran dibuat. Klik Tampilkan untuk memuat.",
                            "Berhasil", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        doLoad();
                    } catch (Exception ex) {
                        Common.tampilErrorJikaAdmin(ex);
                    }
                }
            });
    }

    /** Dialog kelola Jam Pelajaran (tambah/ubah/hapus manual) untuk sekolah dari kelas terpilih. */
    @SuppressWarnings({"rawtypes","unchecked"})
    private void bukaKelolaJam() throws Exception {
        if (cbKelas == null || cbKelas.getSelectedItem() == null || cbKelas.getSelectedItem().getValue() == null) {
            msgbox("Pilih Kelas terlebih dahulu (Jam Pelajaran dikelola per sekolah dari kelas terpilih).",
                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }
        final Long kelasId = (Long) cbKelas.getSelectedItem().getValue();
        final Session s = HibernateUtil.currentSession();
        final KelasSiswa kelas = (KelasSiswa) s.get(KelasSiswa.class, kelasId);
        if (kelas == null || kelas.getSekolah() == null) {
            msgbox("Sekolah dari kelas tidak ditemukan.", "Peringatan",
                MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }
        final Sekolah sekolah = kelas.getSekolah();
        final JenisJadwalPelajaran jenisBaru = jenisDefault(s, sekolah);

        final MyWindow w = new MyWindow("Kelola Jam Pelajaran", "normal", true);
        w.setParent(getPage().getFirstRoot());
        w.setWidth("560px");
        w.setContentStyle("padding:10px;");

        Div body = new Div();
        body.setStyle("display:flex;flex-direction:column;gap:6px;font-size:12px;");
        body.setParent(w);
        Label info = new Label("Ubah / tambah / hapus jam. Format waktu: 07:30  (klik Simpan untuk menyimpan semua)");
        info.setStyle("font-size:11px;color:#64748b;");
        info.setParent(body);

        Div head = new Div();
        head.setStyle("display:flex;gap:6px;font-weight:600;");
        head.setParent(body);
        addKolHead(head, "Nama", "160px");
        addKolHead(head, "Mulai", "90px");
        addKolHead(head, "Selesai", "90px");

        final Div listWrap = new Div();
        listWrap.setStyle("max-height:340px;overflow:auto;display:flex;flex-direction:column;gap:4px;");
        listWrap.setParent(body);

        final java.util.List<Object[]> rows = new java.util.ArrayList<Object[]>();
        final Set<Long> asalIds = new HashSet<Long>();
        List ex = s.createCriteria(JamPelajaran.class).add(Restrictions.eq("sekolah", sekolah))
            .add(Restrictions.eq("aktif", Boolean.TRUE)).addOrder(Order.asc("waktuMulaiD")).list();
        for (Object o : ex) {
            JamPelajaran jp = (JamPelajaran) o;
            asalIds.add(jp.getId());
            tambahBarisKelola(listWrap, rows, jp.getId(), jp.getNama(), jp.getMulaiS(), jp.getSampaiS());
        }

        Div bar = new Div();
        bar.setStyle("display:flex;gap:8px;justify-content:space-between;margin-top:8px;");
        bar.setParent(body);
        Button btnTambah = new Button("+ Tambah Baris"); btnTambah.setSclass("btn btn-default btn-sm");
        btnTambah.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                tambahBarisKelola(listWrap, rows, null, "", "", "");
            }
        });
        btnTambah.setParent(bar);

        Div kanan = new Div(); kanan.setStyle("display:flex;gap:8px;"); kanan.setParent(bar);
        Button btnTutup = new Button("Tutup"); btnTutup.setSclass("btn btn-default btn-sm");
        btnTutup.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception { w.detach(); }
        });
        btnTutup.setParent(kanan);
        Button btnSimpan = new Button("Simpan"); btnSimpan.setSclass("btn btn-success btn-sm");
        btnSimpan.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                try {
                    Set<Long> present = new HashSet<Long>();
                    for (int i = 0; i < rows.size(); i++) {
                        Object[] r = rows.get(i);
                        Long id = (Long) r[0];
                        Textbox tNama = (Textbox) r[1];
                        Textbox tWm = (Textbox) r[2];
                        Textbox tWs = (Textbox) r[3];
                        String wm = tWm.getValue() == null ? "" : tWm.getValue().trim();
                        String ws = tWs.getValue() == null ? "" : tWs.getValue().trim();
                        if (wm.isEmpty()) continue;
                        JamPelajaran jp = (id != null)
                            ? (JamPelajaran) s.get(JamPelajaran.class, id) : new JamPelajaran();
                        if (jp == null) jp = new JamPelajaran();
                        jp.setNama(tNama.getValue());
                        jp.setSekolah(sekolah);
                        if (jp.getId() == null) {
                            jp.setJenisJadwalPelajaran(jenisBaru);
                            jp.setAktif(Boolean.TRUE);
                        }
                        jp.setMulaiS(wm);
                        jp.setSampaiS(ws);
                        s.saveOrUpdate(jp);
                        if (jp.getId() != null) present.add(jp.getId());
                    }
                    for (java.util.Iterator<Long> it = asalIds.iterator(); it.hasNext();) {
                        Long oid = it.next();
                        if (!present.contains(oid)) {
                            JamPelajaran del = (JamPelajaran) s.get(JamPelajaran.class, oid);
                            if (del != null) s.delete(del);
                        }
                    }
                    s.flush();
                    w.detach();
                    msgbox("Jam pelajaran tersimpan. Klik Tampilkan untuk memuat ulang.",
                        "Berhasil", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                    doLoad();
                } catch (Exception ex) {
                    Common.tampilErrorJikaAdmin(ex);
                }
            }
        });
        btnSimpan.setParent(kanan);

        w.setVisible(true);
        w.onModal();
    }

    private static void addKolHead(Div head, String label, String width) {
        Label l = new Label(label);
        l.setStyle("width:" + width + ";display:inline-block;");
        l.setParent(head);
    }

    private void tambahBarisKelola(Div listWrap, final java.util.List<Object[]> rows, Long id,
                                   String nama, String wm, String ws) {
        final Div row = new Div();
        row.setStyle("display:flex;gap:6px;align-items:center;");
        final Textbox tNama = new Textbox(nama == null ? "" : nama); tNama.setWidth("160px");
        final Textbox tWm = new Textbox(wm == null ? "" : wm); tWm.setWidth("90px");
        final Textbox tWs = new Textbox(ws == null ? "" : ws); tWs.setWidth("90px");
        row.appendChild(tNama); row.appendChild(tWm); row.appendChild(tWs);
        final Object[] holder = new Object[]{ id, tNama, tWm, tWs, row };
        Toolbarbutton del = new Toolbarbutton("Hapus");
        del.setStyle("color:#dc2626;cursor:pointer;");
        del.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                rows.remove(holder);
                row.detach();
            }
        });
        row.appendChild(del);
        row.setParent(listWrap);
        rows.add(holder);
    }

    /** Mencetak jadwal pelajaran (format tabel Hari x Jam) di jendela baru lalu print. */
    @SuppressWarnings({"rawtypes","unchecked"})
    private void cetakJadwal() {
        if (cbTA.getSelectedItem() == null || cbSmt.getSelectedItem() == null
                || cbKelas.getSelectedItem() == null || cbKelas.getSelectedItem().getValue() == null) {
            msgbox("Pilih Tahun Ajaran, Semester, & Kelas terlebih dahulu.", "Peringatan",
                MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }
        try {
            Session s = HibernateUtil.currentSession();
            String ta  = (String)  cbTA.getSelectedItem().getValue();
            int    smt = (Integer) cbSmt.getSelectedItem().getValue();
            Long kelasId = (Long) cbKelas.getSelectedItem().getValue();
            KelasSiswa kelas = (KelasSiswa) s.get(KelasSiswa.class, kelasId);
            List<JamPelajaran> jamList = loadJamList(s, ta);

            List<JadwalPelajaran> jpAll = s.createCriteria(JadwalPelajaran.class)
                .add(Restrictions.eq("tahunAjaran", ta))
                .add(Restrictions.eq("semester", Integer.valueOf(smt)))
                .add(Restrictions.eq("kelas", kelas))
                .list();

            Map<String, List<JadwalPelajaran>> cellMap = new HashMap<String, List<JadwalPelajaran>>();
            for (JadwalPelajaran jp : jpAll) {
                String hari = jp.getHari();
                JamPelajaran jam = jp.getJamPelajaran();
                if (hari == null || hari.trim().isEmpty() || jam == null) continue;
                String key = hari.trim() + "_" + jam.getId();
                List<JadwalPelajaran> b = cellMap.get(key);
                if (b == null) { b = new ArrayList<JadwalPelajaran>(); cellMap.put(key, b); }
                b.add(jp);
            }

            String html = buildCetakHtml(ta, smt, kelas, jamList, cellMap);
            String js = "var w=window.open('','_blank');"
                + "if(w){w.document.open();w.document.write(" + org.json.JSONObject.quote(html) + ");"
                + "w.document.close();w.focus();setTimeout(function(){try{w.print();}catch(e){}},600);}"
                + "else{alert('Popup diblokir browser. Izinkan popup untuk mencetak.');}";
            Clients.evalJavaScript(js);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    /** Menyusun HTML jadwal siap-cetak: judul + Kelas + tabel Hari x Jam berisi Mapel/Guru. */
    private String buildCetakHtml(String ta, int smt, KelasSiswa kelas,
                                  List<JamPelajaran> jamList, Map<String, List<JadwalPelajaran>> cellMap) {
        String smtLbl = smt == 1 ? "GANJIL" : "GENAP";
        String institusi = "";
        try { institusi = Common.getKonfigurasi("judul_header_sekolah", "").getNilai(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TimetableJadwalPelajaranWindow.java:1175");}
        String judulKelas = kelas != null ? kelas.getNama() : "KELAS";

        StringBuilder h = new StringBuilder();
        h.append("<html><head><meta charset='UTF-8'><title>Jadwal ").append(esc(judulKelas)).append("</title>");
        h.append("<style>");
        h.append("*{box-sizing:border-box;font-family:Arial,Helvetica,sans-serif;}");
        h.append("body{margin:14px;color:#111;}");
        h.append(".jhead{text-align:center;margin-bottom:8px;}");
        h.append(".jhead .t1{font-size:15px;font-weight:bold;}");
        h.append(".jhead .t2{font-size:22px;font-weight:bold;letter-spacing:.5px;}");
        h.append(".jhead .t3{font-size:11px;color:#333;}");
        h.append("table{border-collapse:collapse;width:100%;table-layout:fixed;}");
        h.append("th,td{border:1px solid #333;padding:3px;font-size:10px;vertical-align:top;}");
        h.append("th{background:#f2f2f2;text-align:center;}");
        h.append(".dcol{font-size:16px;font-weight:bold;text-align:center;vertical-align:middle;width:66px;background:#fff;}");
        h.append(".mk{font-weight:bold;font-size:10px;}");
        h.append(".sub{font-size:9px;color:#333;}");
        h.append(".slot{margin-bottom:3px;padding-bottom:2px;border-bottom:1px dotted #bbb;}");
        h.append(".slot:last-child{border-bottom:none;margin-bottom:0;padding-bottom:0;}");
        h.append("@media print{body{margin:6px;} .noprint{display:none;} @page{size:landscape;}}");
        h.append("</style></head><body>");
        h.append("<div class='noprint' style='text-align:right;margin-bottom:6px;'>")
         .append("<button onclick='window.print()'>Cetak</button></div>");
        h.append("<div class='jhead'>");
        h.append("<div class='t1'>JADWAL ").append(smtLbl).append(" ").append(esc(ta)).append("</div>");
        h.append("<div class='t2'>").append(esc(judulKelas)).append("</div>");
        if (institusi != null && !institusi.isEmpty()) {
            h.append("<div class='t3'>").append(esc(institusi)).append("</div>");
        }
        h.append("</div>");

        h.append("<table><thead><tr><th class='dcol'>Hari</th>");
        for (JamPelajaran jam : jamList) {
            String wm = jam.getMulaiS(), ws = jam.getSampaiS();
            h.append("<th>");
            if (wm != null && !wm.isEmpty()) {
                h.append(esc(wm));
                if (ws != null && !ws.isEmpty()) h.append("<br>-<br>").append(esc(ws));
            } else {
                h.append(esc(jam.getNama() == null ? "Jam" : jam.getNama()));
            }
            h.append("</th>");
        }
        h.append("</tr></thead><tbody>");
        for (int di = 0; di < HARI.length; di++) {
            String hari = HARI[di];
            h.append("<tr><td class='dcol'>").append(hari).append("</td>");
            for (JamPelajaran jam : jamList) {
                h.append("<td>");
                List<JadwalPelajaran> bucket = cellMap.get(hari + "_" + jam.getId());
                if (bucket != null) {
                    for (JadwalPelajaran jp : bucket) {
                        String mk = jp.getMatapelajaran() != null ? jp.getMatapelajaran().getNama() : "-";
                        h.append("<div class='slot'><div class='mk'>").append(esc(mk)).append("</div>");
                        if (jp.getGuru() != null && jp.getGuru().getNama() != null) {
                            h.append("<div class='sub'>").append(esc(jp.getGuru().getNama())).append("</div>");
                        }
                        h.append("</div>");
                    }
                }
                h.append("</td>");
            }
            h.append("</tr>");
        }
        h.append("</tbody></table></body></html>");
        return h.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ── Filter Kurikulum + kartu Mapel ──────────────────────────────────────────

    /**
     * Menambahkan kartu Mata Pelajaran dari kurikulum yang BELUM memiliki jadwal (untuk kelas ini) ke
     * panel "Belum Terjadwal". Kartu dapat diseret ke sel untuk membuat JadwalPelajaran baru.
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    private void appendMpCards(Long kurId, KelasSiswa kelas, List<JadwalPelajaran> jpAll, Session s) {
        try {
            KurikulumSekolah kur = (KurikulumSekolah) s.get(KurikulumSekolah.class, kurId);
            if (kur == null) return;
            Set<Long> adaMp = new HashSet<Long>();
            for (JadwalPelajaran jp : jpAll) {
                if (jp.getMatapelajaran() != null) adaMp.add(jp.getMatapelajaran().getId());
            }
            List kpmList = s.createCriteria(KurikulumPunyaMatapelajaran.class)
                .add(Restrictions.eq("kurikulumSekolah", kur)).list();
            int ditambah = 0;
            for (Object o : kpmList) {
                KurikulumPunyaMatapelajaran kpm = (KurikulumPunyaMatapelajaran) o;
                Matapelajaran mp = kpm.getMatapelajaran();
                if (mp == null || !mp.getAktif().booleanValue() || adaMp.contains(mp.getId())) continue;
                if (ditambah == 0) hapusHint(palette);
                mkMpCard(mp, kur).setParent(palette);
                ditambah++;
            }
        } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TimetableJadwalPelajaranWindow.java:1275"); /* non-fatal */ }
    }

    /** Kartu Mapel kurikulum (belum ada jadwal) — putih, garis putus-putus, dapat diseret. */
    private Div mkMpCard(final Matapelajaran mp, final KurikulumSekolah kur) {
        final Div card = new Div();
        card.setAttribute("mpId", mp.getId());
        card.setAttribute("kurId", kur.getId());
        card.setDraggable(DND);
        card.setSclass("aisttcard aisttmk");
        Div cn = new Div(); cn.setSclass("cn");
        new Label("+ " + (mp.getNama() != null ? mp.getNama() : "-")).setParent(cn);
        cn.setParent(card);
        Div cg = new Div(); cg.setSclass("cg");
        new Label(ais.common.Common.getBahasaConfig("Mapel kurikulum - seret ke sel untuk buat jadwal")).setParent(cg);
        cg.setParent(card);
        return card;
    }

    /** Membuat JadwalPelajaran baru dari kartu Mapel yang dijatuhkan ke sel, lalu jadikan kartu nyata. */
    private void buatJadwalDariMp(Div mpCardEl, Div cell) {
        try {
            if (cbTA.getSelectedItem() == null || cbSmt.getSelectedItem() == null
                    || cbKelas.getSelectedItem() == null) return;
            Session s = HibernateUtil.currentSession();
            Long mpId    = (Long) mpCardEl.getAttribute("mpId");
            Long kelasId = (Long) cbKelas.getSelectedItem().getValue();
            Matapelajaran mp = (Matapelajaran) s.get(Matapelajaran.class, mpId);
            KelasSiswa    kelas = (KelasSiswa)  s.get(KelasSiswa.class, kelasId);
            if (mp == null || kelas == null) return;

            String ta  = (String)  cbTA.getSelectedItem().getValue();
            int    smt = (Integer) cbSmt.getSelectedItem().getValue();

            JadwalPelajaran jp = new JadwalPelajaran();
            jp.setMatapelajaran(mp);
            jp.setKelas(kelas);
            jp.setTahunAjaran(ta);
            jp.setSemester(Integer.valueOf(smt));
            String hari  = (String) cell.getAttribute("hari");
            Long   jamId = (Long)   cell.getAttribute("jamId");
            jp.setHari(hari);
            JamPelajaran jam = (jamId != null) ? (JamPelajaran) s.get(JamPelajaran.class, jamId) : null;
            jp.setJamPelajaran(jam);

            s.save(jp);
            s.flush();

            mpCardEl.detach();
            mkCard(jp, viewMode == 1).setParent(cell);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    // ── Edit kartu ──────────────────────────────────────────────────────────────

    /** Dialog edit ringkas untuk satu jadwal pelajaran: guru pengampu. */
    @SuppressWarnings({"rawtypes","unchecked"})
    private void bukaEditJadwal(final Long jpId, final Div card, final boolean showKelas) throws Exception {
        if (jpId == null) return;
        final Session s = HibernateUtil.currentSession();
        final JadwalPelajaran jp = (JadwalPelajaran) s.get(JadwalPelajaran.class, jpId);
        if (jp == null) return;

        final MyWindow w = new MyWindow("Edit Jadwal Pelajaran", "normal", true);
        w.setParent(getPage().getFirstRoot());
        w.setWidth("430px");
        w.setContentStyle("padding:12px;");

        Div body = new Div();
        body.setStyle("display:flex;flex-direction:column;gap:8px;font-size:12px;");
        body.setParent(w);

        Label judul = new Label(jp.getMatapelajaran() != null ? jp.getMatapelajaran().getNama() : "-");
        judul.setStyle("font-weight:600;");
        judul.setParent(body);

        Div rGuru = new Div(); rGuru.setStyle("display:flex;align-items:center;gap:6px;"); rGuru.setParent(body);
        new Label(ais.common.Common.getBahasaConfig("Guru:")).setParent(rGuru);
        final Combobox cbG = new Combobox(); cbG.setReadonly(true); cbG.setWidth("260px");
        cbG.appendItem("= (kosong) =").setValue(null);
        List gl = s.createCriteria(Guru.class).add(Restrictions.eq("aktif", Boolean.TRUE))
            .addOrder(Order.asc("nama")).setMaxResults(600).list();
        int sel = 0, idx = 1;
        for (Object o : gl) {
            Guru g = (Guru) o;
            cbG.appendItem(g.getNama()).setValue(g.getId());
            if (jp.getGuru() != null && g.getId().equals(jp.getGuru().getId())) sel = idx;
            idx++;
        }
        cbG.setSelectedIndex(sel);
        cbG.setParent(rGuru);

        Div bar = new Div();
        bar.setStyle("display:flex;gap:8px;justify-content:flex-end;margin-top:6px;");
        bar.setParent(body);
        Button btnBatal = new Button("Batal"); btnBatal.setSclass("btn btn-default btn-sm");
        btnBatal.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception { w.detach(); }
        });
        btnBatal.setParent(bar);
        Button btnSimpan = new Button("Simpan"); btnSimpan.setSclass("btn btn-success btn-sm");
        btnSimpan.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                Long gid = (cbG.getSelectedItem() != null) ? (Long) cbG.getSelectedItem().getValue() : null;
                jp.setGuru(gid == null ? null : (Guru) s.get(Guru.class, gid));
                s.saveOrUpdate(jp);
                refreshCard(card, jp, showKelas);
                w.detach();
            }
        });
        btnSimpan.setParent(bar);

        w.setVisible(true);
        w.onModal();
    }

    /** Mengganti kartu lama dengan kartu segar yang mencerminkan perubahan (guru/kelas). */
    private void refreshCard(Div oldCard, JadwalPelajaran jp, boolean showKelas) {
        Component parent = oldCard.getParent();
        Div baru = mkCard(jp, showKelas);
        oldCard.detach();
        if (parent != null) baru.setParent(parent);
    }

    // ── Klik sel: tambah dari daftar "Belum Terjadwal" ──────────────────────────

    /** Menampilkan daftar item "Belum Terjadwal" untuk ditempatkan ke {@code cell} (klik = pindah). */
    @SuppressWarnings("unchecked")
    private void bukaTambahKeCell(final Div cell) throws Exception {
        java.util.List<Div> kandidat = new java.util.ArrayList<Div>();
        for (Object o : palette.getChildren()) {
            if (o instanceof Div) {
                Div d = (Div) o;
                if (d.getAttribute("jpId") != null || d.getAttribute("mpId") != null) kandidat.add(d);
            }
        }
        if (kandidat.isEmpty()) {
            msgbox("Belum ada item di panel 'Belum Terjadwal'. Pilih Kurikulum atau klik Tampilkan lebih dulu.",
                "Info", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }
        final MyWindow w = new MyWindow("Tambah ke Slot Ini", "normal", true);
        w.setParent(getPage().getFirstRoot());
        w.setWidth("360px");
        w.setContentStyle("padding:0;");
        Div list = new Div();
        list.setStyle("max-height:380px;overflow:auto;");
        list.setParent(w);
        for (int i = 0; i < kandidat.size(); i++) {
            final Div kartu = kandidat.get(i);
            Div rowc = new Div();
            rowc.setSclass("aisttchooserrow");
            new Label(namaKartu(kartu)).setParent(rowc);
            rowc.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    cardToCell(kartu, cell);
                    w.detach();
                }
            });
            rowc.setParent(list);
        }
        w.setVisible(true);
        w.onModal();
    }

    @SuppressWarnings("unchecked")
    private static String namaKartu(Div kartu) {
        for (Object c : kartu.getChildren()) {
            if (c instanceof Div && "cn".equals(((Div) c).getSclass())) {
                for (Object lc : ((Div) c).getChildren()) {
                    if (lc instanceof Label) return ((Label) lc).getValue();
                }
            }
        }
        return "(item)";
    }

    @SuppressWarnings("unchecked")
    private static void hapusHint(Div panel) {
        for (Object c : new ArrayList<Object>(panel.getChildren())) {
            if (c instanceof Label && "aistthint".equals(((Label) c).getSclass())) {
                ((Component) c).detach();
            }
        }
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private static void msgbox(String msg, String title, int btn, String icon) {
        try { MyMessageboxConfig.show(msg, title, btn, icon); }
        catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    private int color(Long id) {
        if (!colorMap.containsKey(id)) colorMap.put(id, colorCounter++ % CARD_COLORS.length);
        return colorMap.get(id);
    }

    private static void hint(Div parent, String text) {
        Label l = new Label(text);
        l.setSclass("aistthint");
        l.setParent(parent);
    }

    private static void btn(Div bar, String label, String sclass, EventListener listener) {
        Button b = new Button(label);
        b.setSclass(sclass);
        b.addEventListener("onClick", listener);
        b.setParent(bar);
    }

    private static void sep(Div bar) {
        Div s = new Div();
        s.setSclass("aisttsep");
        s.setParent(bar);
    }

    private static void legItem(Div leg, String icon, Div box, String text) {
        Div item = new Div();
        item.setSclass("aisttleg-item");
        if (icon != null) new Label(icon).setParent(item);
        if (box  != null) box.setParent(item);
        new Label(text).setParent(item);
        item.setParent(leg);
    }

    private static void legSep(Div leg) {
        Div s = new Div();
        s.setStyle("width:1px;height:14px;background:#cbd5e1;margin:0 4px;");
        s.setParent(leg);
    }
}
