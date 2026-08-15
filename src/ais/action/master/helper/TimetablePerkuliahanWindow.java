package ais.action.master.helper;

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
import org.zkoss.zul.Toolbarbutton;

import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.CommonPenjadwalan;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.JamPerkuliahan;
import ais.database.model.Jurusan;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Timetable jadwal mingguan untuk modul Perkuliahan (Perguruan Tinggi).
 *
 * Fitur:
 * - Grid Hari x Jam Perkuliahan dengan drag-and-drop ZK native
 * - Panel "Belum Terjadwal" untuk kartu yang belum dipasang
 * - Tampilan "Per Jurusan" (default) dan "Per Dosen"
 * - Kunci kartu agar tidak bisa dipindah secara tidak sengaja
 * - Deteksi konflik: dosen yang mengajar di 2 kelas di slot yang sama
 * - Tombol "Periksa Konflik" untuk validasi data
 * - Simpan perubahan ke database (hari, jamPerkuliahan, waktuMulai, waktuSelesai)
 */
public class TimetablePerkuliahanWindow extends MyWindow {

    private static final long serialVersionUID = 1L;

    private static final String DND = "ttp";
    private static final String[] HARI = {
        "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"
    };
    private static final String[] CARD_COLORS = {
        "#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6",
        "#EC4899", "#14B8A6", "#F97316", "#6366F1", "#84CC16",
        "#0EA5E9", "#A855F7", "#D97706", "#059669", "#DC2626"
    };

    // 0 = Per Jurusan, 1 = Per Dosen
    private int viewMode = 0;

    private Combobox cbTA, cbSmt, cbJurusan, cbDosen, cbKurikulum;
    private Div filterJurusanRow, filterDosenRow;
    private Button btnViewJurusan, btnViewDosen;
    private Div palette, gridArea;
    private Jurusan jurusanPengguna;
    private Fakultas fakultasPengguna;

    /** pId → {newHari, newJamId, newWm, newWs} — perubahan belum disimpan */
    private final Map<Long, Object[]>  changes     = new LinkedHashMap<Long, Object[]>();
    /** pId kartu yang dikunci */
    private final Set<Long>            locked      = new HashSet<Long>();
    /** pId kartu yang mengalami konflik dosen lintas kelas */
    private final Set<Long>            conflictIds = new HashSet<Long>();

    private int               colorCounter;
    private final Map<Long, Integer> colorMap = new HashMap<Long, Integer>();

    // ── Konstruktor ──────────────────────────────────────────────────────────

    public TimetablePerkuliahanWindow() {
        super("Timetable Perkuliahan", "none", false);
        try {
            setWidth("100%");
            setHeight("100%");
            setContentStyle("padding:0;overflow:hidden;");
            injectStyle();
            buildLayout();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            PesanFormalHelper.tampilkanGagalException(
                    "menampilkan jendela Timetable Perkuliahan",
                    e, new String[] {
                            "Muat ulang (refresh) halaman ini lalu coba buka jendela kembali.",
                            "Periksa koneksi jaringan Anda ke server aplikasi.",
                            "Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
                    });
        }
    }

    // ── CSS ──────────────────────────────────────────────────────────────────

    private void injectStyle() {
        Html css = new Html();
        css.setContent(
            "<style>" +
            ".aisttpwrap{display:flex;flex-direction:column;height:100%;" +
              "font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;" +
              "font-size:12px;}" +
            ".aisttpbar{display:flex;align-items:center;gap:6px;flex-wrap:wrap;" +
              "padding:8px 12px;border-bottom:1px solid #e2e8f0;background:#f8fafc;" +
              "color:#374151;}" +
            ".aisttpviewtab{padding:4px 12px;border-radius:4px;" +
              "border:1px solid #cbd5e1;background:#fff;color:#374151;" +
              "cursor:pointer;font-size:11px;font-weight:500;}" +
            ".aisttpviewtab.active{background:#3B82F6;color:#fff;border-color:#2563EB;}" +
            ".aisttpsep{width:1px;height:22px;background:#cbd5e1;margin:0 3px;" +
              "flex-shrink:0;}" +
            ".aisttpbody{display:flex;flex:1;overflow:hidden;min-height:0;}" +
            /* Panel kiri */
            ".aisttppanel{width:190px;min-width:190px;border-right:1px solid #e2e8f0;" +
              "display:flex;flex-direction:column;background:#fafafa;}" +
            ".aisttppanelhdr{font-size:11px;font-weight:600;color:#64748b;" +
              "padding:6px 10px;border-bottom:1px solid #e2e8f0;background:#f1f5f9;" +
              "white-space:nowrap;}" +
            ".aisttppalette{flex:1;overflow-y:auto;padding:6px;min-height:80px;}" +
            ".aisttpgridarea{flex:1;overflow:auto;padding:10px;}" +
            /* Tabel */
            ".aisttpgridtbl{display:table;border-collapse:collapse;" +
              "width:100%;min-width:720px;}" +
            ".aisttptblrow{display:table-row;}" +
            ".aisttphdrcell{display:table-cell;border:1px solid #cbd5e1;" +
              "padding:6px 4px;font-size:12px;font-weight:600;color:#1e293b;" +
              "text-align:center;background:#e2e8f0;white-space:nowrap;" +
              "min-width:110px;}" +
            ".aisttpjamcell{display:table-cell;border:1px solid #cbd5e1;" +
              "padding:4px 3px;font-size:10px;text-align:center;color:#64748b;" +
              "background:#f8fafc;vertical-align:middle;min-width:70px;max-width:84px;" +
              "white-space:pre-line;line-height:1.4;}" +
            ".aisttpcell{display:table-cell;border:1px solid #cbd5e1;" +
              "vertical-align:top;min-width:110px;padding:3px;min-height:60px;}" +
            /* Kartu */
            ".aisttpcard{border-radius:5px;padding:4px 34px 4px 6px;margin:2px 0;" +
              "font-size:11px;color:#fff;cursor:grab;position:relative;" +
              "word-break:break-word;line-height:1.4;user-select:none;}" +
            ".aisttpcard .cn{font-weight:600;}" +
            ".aisttpcard .cd{font-size:10px;opacity:.85;margin-top:2px;}" +
            ".aisttpcard .ck{font-size:10px;opacity:.72;" +
              "background:rgba(0,0,0,.18);border-radius:2px;padding:0 3px;" +
              "display:inline-block;margin-top:2px;}" +
            /* Tombol kartu */
            ".aisttpcard .cdel{position:absolute;top:2px;right:18px;" +
              "cursor:pointer;font-size:13px;line-height:1;color:#fff;" +
              "opacity:.65;padding:0;border:none;background:transparent;" +
              "font-weight:bold;}" +
            ".aisttpcard .cdel:hover{opacity:1;}" +
            ".aisttpcard .clock{position:absolute;top:3px;right:2px;" +
              "cursor:pointer;font-size:11px;line-height:1;color:#fff;" +
              "opacity:.7;padding:0;border:none;background:transparent;}" +
            ".aisttpcard .clock:hover{opacity:1;}" +
            /* State: dikunci */
            ".aisttpcard.locked{cursor:not-allowed;filter:brightness(.88);" +
              "outline:2px solid rgba(255,255,255,.5);outline-offset:-2px;}" +
            /* State: konflik dosen */
            ".aisttpcard.conflict{outline:2px solid #ef4444;" +
              "outline-offset:1px;box-shadow:0 0 0 4px rgba(239,68,68,.2);}" +
            /* Kartu MK dari kurikulum (belum ada perkuliahan) */
            ".aisttpmk{background:#ffffff!important;color:#334155!important;" +
              "border:1px dashed #94a3b8;cursor:grab;}" +
            ".aisttpmk .cn{color:#1e293b;}" +
            ".aisttpmk .cd{color:#64748b;opacity:1;}" +
            /* Sel dapat diklik untuk menambah */
            ".aisttpcell{cursor:pointer;}" +
            ".aisttpchooserrow{padding:6px 8px;border-bottom:1px solid #eef2f7;" +
              "cursor:pointer;font-size:12px;}" +
            ".aisttpchooserrow:hover{background:#eff6ff;}" +
            /* Hint */
            ".aisttphint{color:#94a3b8;font-size:12px;padding:24px 12px;" +
              "text-align:center;font-style:italic;line-height:1.6;}" +
            /* Legend */
            ".aisttplegend{font-size:10px;padding:4px 10px;color:#64748b;" +
              "display:flex;gap:10px;align-items:center;" +
              "border-top:1px solid #e2e8f0;background:#f8fafc;flex-wrap:wrap;}" +
            ".aisttpleg-item{display:flex;align-items:center;gap:4px;}" +
            ".aisttpleg-box{width:12px;height:12px;border-radius:2px;flex-shrink:0;}" +
            "</style>"
        );
        css.setParent(this);
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private void buildLayout() {
        Div wrap = new Div();
        wrap.setSclass("aisttpwrap");
        wrap.setParent(this);

        buildFilterBar(wrap);

        Div body = new Div();
        body.setSclass("aisttpbody");
        body.setParent(wrap);

        buildPalettePanel(body);

        gridArea = new Div();
        gridArea.setSclass("aisttpgridarea");
        gridArea.setParent(body);
        hint(gridArea, "Pilih filter di atas, lalu klik Tampilkan.");

        buildLegend(wrap);
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private void buildFilterBar(Div wrap) {
        Div bar = new Div();
        bar.setSclass("aisttpbar");
        bar.setParent(wrap);

        // View mode
        btnViewJurusan = new Button("Per Jurusan");
        btnViewJurusan.setSclass("aisttpviewtab active");
        btnViewJurusan.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception { switchView(0); }
        });
        btnViewJurusan.setParent(bar);

        btnViewDosen = new Button("Per Dosen");
        btnViewDosen.setSclass("aisttpviewtab");
        btnViewDosen.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception { switchView(1); }
        });
        btnViewDosen.setParent(bar);

        sep(bar);

        new Label(ais.common.Common.getBahasaConfig("TA:")).setParent(bar);
        cbTA = new Combobox();
        cbTA.setWidth("110px");
        cbTA.setReadonly(true);
        Common.generateTahunAjaran(cbTA);
        cbTA.setParent(bar);

        new Label(ais.common.Common.getBahasaConfig("Smt:")).setParent(bar);
        cbSmt = new Combobox();
        cbSmt.setWidth("90px");
        cbSmt.setReadonly(true);
        cbSmt.appendItem("Ganjil").setValue(Integer.valueOf(1));
        cbSmt.appendItem("Genap").setValue(Integer.valueOf(2));
        cbSmt.appendItem("Sem. Pendek").setValue(Integer.valueOf(3));
        cbSmt.setSelectedIndex(0);
        cbSmt.setParent(bar);

        // Filter jurusan (default tampil)
        filterJurusanRow = new Div();
        filterJurusanRow.setStyle("display:flex;align-items:center;gap:6px;");
        filterJurusanRow.setParent(bar);
        new Label(ais.common.Common.getBahasaConfig("Jurusan:")).setParent(filterJurusanRow);
        cbJurusan = new Combobox();
        cbJurusan.setWidth("155px");
        cbJurusan.setReadonly(true);
        Session s0 = HibernateUtil.currentSession();
        try {
            Tbmuser pengguna = Common.getCurrentUser();
            jurusanPengguna = pengguna == null ? null : pengguna.ambilJurusan();
            fakultasPengguna = jurusanPengguna != null ? jurusanPengguna.getFakultas()
                    : (pengguna == null ? null : pengguna.ambilFakultas());
        } catch (Exception ex) {
            jurusanPengguna = null;
            fakultasPengguna = null;
        }
        if (jurusanPengguna == null) cbJurusan.appendItem("= Semua =").setValue(null);
        org.hibernate.Criteria kriteriaJurusan = s0.createCriteria(Jurusan.class)
            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
        if (jurusanPengguna != null) {
            kriteriaJurusan.add(Restrictions.eq("id", jurusanPengguna.getId()));
        } else if (fakultasPengguna != null) {
            kriteriaJurusan.add(Restrictions.eq("fakultas", fakultasPengguna));
        }
        List jList = kriteriaJurusan.addOrder(Order.asc("nama")).setMaxResults(200).list();
        for (Object o : jList) {
            Jurusan j = (Jurusan) o;
            cbJurusan.appendItem(j.getNama()).setValue(j.getId());
        }
        cbJurusan.setSelectedIndex(0);
        cbJurusan.setDisabled(jurusanPengguna != null);
        cbJurusan.setParent(filterJurusanRow);

        // Filter Kurikulum: bila dipilih, semua MK kurikulum yang belum ada perkuliahan ikut tampil
        // di panel "Belum Terjadwal" (kartu putih bergaris putus-putus) dan dapat diseret ke sel.
        new Label(ais.common.Common.getBahasaConfig("Kurikulum:")).setParent(filterJurusanRow);
        cbKurikulum = new Combobox();
        cbKurikulum.setWidth("165px");
        cbKurikulum.setReadonly(true);
        cbKurikulum.setParent(filterJurusanRow);
        isiKurikulum(jurusanPengguna == null ? null : jurusanPengguna.getId());
        cbJurusan.addEventListener("onChange", new EventListener() {
            public void onEvent(Event e) throws Exception {
                Long jid = (cbJurusan.getSelectedItem() != null)
                    ? (Long) cbJurusan.getSelectedItem().getValue() : null;
                isiKurikulum(jid);
            }
        });

        // Filter dosen (tersembunyi)
        filterDosenRow = new Div();
        filterDosenRow.setStyle("display:none;align-items:center;gap:6px;");
        filterDosenRow.setParent(bar);
        new Label(ais.common.Common.getBahasaConfig("Dosen:")).setParent(filterDosenRow);
        cbDosen = new Combobox();
        cbDosen.setWidth("175px");
        cbDosen.setReadonly(true);
        List dList = s0.createCriteria(Dosen.class)
            .add(Restrictions.eq("aktif", Boolean.TRUE))
            .addOrder(Order.asc("nama"))
            .setMaxResults(400).list();
        for (Object o : dList) {
            Dosen d = (Dosen) o;
            cbDosen.appendItem(d.getNama()).setValue(d.getId());
        }
        if (!dList.isEmpty()) cbDosen.setSelectedIndex(0);
        cbDosen.setParent(filterDosenRow);

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
        btn(bar, "Penjadwalan Pintar", "btn btn-info btn-sm", new EventListener() {
            public void onEvent(Event e) throws Exception { bukaPenjadwalanPintar(); }
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
        panel.setSclass("aisttppanel");
        panel.setParent(body);

        Div hdr = new Div();
        hdr.setSclass("aisttppanelhdr");
        new Label(ais.common.Common.getBahasaConfig("Belum Terjadwal")).setParent(hdr);
        hdr.setParent(panel);

        palette = new Div();
        palette.setSclass("aisttppalette");
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
        leg.setSclass("aisttplegend");
        leg.setParent(wrap);

        Div i1 = new Div(); i1.setSclass("aisttpleg-item"); i1.setParent(leg);
        new Label("🔒").setParent(i1);
        new Label(ais.common.Common.getBahasaConfig("= Kartu dikunci (tidak dapat dipindah)")).setParent(i1);

        Div sep = new Div();
        sep.setStyle("width:1px;height:14px;background:#cbd5e1;margin:0 4px;");
        sep.setParent(leg);

        Div i2 = new Div(); i2.setSclass("aisttpleg-item"); i2.setParent(leg);
        Div dot = new Div();
        dot.setSclass("aisttpleg-box");
        dot.setStyle("background:#3B82F6;outline:2px solid #ef4444;outline-offset:1px;");
        dot.setParent(i2);
        new Label(ais.common.Common.getBahasaConfig("= Konflik dosen (dosen sama di slot yang sama pada kelas berbeda)")).setParent(i2);

        Div sep2 = new Div();
        sep2.setStyle("width:1px;height:14px;background:#cbd5e1;margin:0 4px;");
        sep2.setParent(leg);

        Div i3 = new Div(); i3.setSclass("aisttpleg-item"); i3.setParent(leg);
        new Label(ais.common.Common.getBahasaConfig("Satu slot bisa berisi banyak kartu (kelas berbeda, dosen berbeda)")).setParent(i3);
    }

    // ── Switch view ───────────────────────────────────────────────────────────

    private void switchView(int mode) {
        viewMode = mode;
        if (mode == 0) {
            btnViewJurusan.setSclass("aisttpviewtab active");
            btnViewDosen.setSclass("aisttpviewtab");
            filterJurusanRow.setStyle("display:flex;align-items:center;gap:6px;");
            filterDosenRow.setStyle("display:none;align-items:center;gap:6px;");
        } else {
            btnViewJurusan.setSclass("aisttpviewtab");
            btnViewDosen.setSclass("aisttpviewtab active");
            filterJurusanRow.setStyle("display:none;align-items:center;gap:6px;");
            filterDosenRow.setStyle("display:flex;align-items:center;gap:6px;");
        }
        gridArea.getChildren().clear();
        palette.getChildren().clear();
        hint(gridArea, "Klik Tampilkan untuk memuat data.");
        changes.clear();
        locked.clear();
        conflictIds.clear();
    }

    // ── Data load ─────────────────────────────────────────────────────────────

    @SuppressWarnings({"rawtypes","unchecked"})
    private void doLoad() {
        if (cbTA.getSelectedItem() == null || cbSmt.getSelectedItem() == null) {
            msgbox("Pilih Tahun Ajaran dan Semester terlebih dahulu.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }
        if (viewMode == 1 && cbDosen.getSelectedItem() == null) {
            msgbox("Pilih Dosen terlebih dahulu.", "Peringatan",
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

        buildConflictIds(ta, smt, s);

        List<JamPerkuliahan> jamList = loadJamList(s);

        if (viewMode == 0) {
            doLoadPerJurusan(ta, smt, s, jamList);
        } else {
            doLoadPerDosen(ta, smt, s, jamList);
        }
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private void doLoadPerJurusan(String ta, int smt, Session s, List<JamPerkuliahan> jamList) {
        Long jurusanId = (cbJurusan.getSelectedItem() != null)
            ? (Long) cbJurusan.getSelectedItem().getValue() : null;

        if (jurusanPengguna != null) jurusanId = jurusanPengguna.getId();

        org.hibernate.Criteria c = s.createCriteria(Perkuliahan.class)
            .add(Restrictions.eq("tahunAjaran", ta))
            .add(Restrictions.ne("merupakan_tanpa_jadwal_perkuliahan", Boolean.TRUE));
        batasiKriteriaKeLingkupPengguna(c);
        applySmt(c, smt);
        if (jurusanId != null) {
            Jurusan j = (Jurusan) s.get(Jurusan.class, jurusanId);
            if (j != null) c.add(Restrictions.eq("jurusan", j));
        }
        List<Perkuliahan> pAll = c.list();
        buildCellMapAndRebuild(pAll, jamList, false);

        // Filter Kurikulum: tampilkan MK kurikulum yang belum punya perkuliahan sebagai kartu tambahan.
        Long kurId = (cbKurikulum != null && cbKurikulum.getSelectedItem() != null)
            ? (Long) cbKurikulum.getSelectedItem().getValue() : null;
        if (kurId != null) appendMkCards(kurId, pAll, s);
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private void doLoadPerDosen(String ta, int smt, Session s, List<JamPerkuliahan> jamList) {
        Long dosenId = (Long) cbDosen.getSelectedItem().getValue();
        Dosen dosen = (Dosen) s.get(Dosen.class, dosenId);
        if (dosen == null) {
            msgbox("Dosen tidak ditemukan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }

        org.hibernate.Criteria c = s.createCriteria(Perkuliahan.class)
            .add(Restrictions.eq("tahunAjaran", ta))
            .add(Restrictions.eq("dosen1", dosen))
            .add(Restrictions.ne("merupakan_tanpa_jadwal_perkuliahan", Boolean.TRUE));
        batasiKriteriaKeLingkupPengguna(c);
        applySmt(c, smt);
        List<Perkuliahan> pAll = c.list();
        buildCellMapAndRebuild(pAll, jamList, true);
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private void buildCellMapAndRebuild(List<Perkuliahan> pAll,
                                        List<JamPerkuliahan> jamList,
                                        boolean showKelas) {
        // Perkuliahan: satu sel bisa berisi banyak kartu (kelas berbeda)
        Map<String, List<Perkuliahan>> cellMap = new HashMap<String, List<Perkuliahan>>();
        List<Perkuliahan> unscheduled = new ArrayList<Perkuliahan>();

        for (Perkuliahan p : pAll) {
            String hari = p.getHari();
            if (hari == null || hari.trim().isEmpty()) { unscheduled.add(p); continue; }
            JamPerkuliahan jam = p.getJamPerkuliahan();
            if (jam == null) {
                jam = matchJam(p.getWaktuMulai(), jamList);
            }
            if (jam == null) { unscheduled.add(p); continue; }
            String key = hari.trim() + "_" + jam.getId();
            List<Perkuliahan> bucket = cellMap.get(key);
            if (bucket == null) { bucket = new ArrayList<Perkuliahan>(); cellMap.put(key, bucket); }
            bucket.add(p);
        }

        rebuildPalette(unscheduled, showKelas);
        rebuildGrid(jamList, cellMap, showKelas);
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private List<JamPerkuliahan> loadJamList(Session s) {
        if (viewMode == 0 && cbJurusan.getSelectedItem() != null) {
            Long jid = (Long) cbJurusan.getSelectedItem().getValue();
            if (jid != null) {
                Jurusan j = (Jurusan) s.get(Jurusan.class, jid);
                if (j != null) {
                    try {
                        List<JamPerkuliahan> list = s.createCriteria(JamPerkuliahan.class)
                            .add(Restrictions.eq("jurusan", j))
                            .addOrder(Order.asc("mulai"))
                            .list();
                        if (!list.isEmpty()) return list;
                    } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/TimetablePerkuliahanWindow.java:527"); /* fallback */ }
                }
            }
        }
        return s.createCriteria(JamPerkuliahan.class)
            .addOrder(Order.asc("mulai"))
            .setMaxResults(16)
            .list();
    }

    private JamPerkuliahan matchJam(String waktuMulai, List<JamPerkuliahan> jamList) {
        if (waktuMulai == null || waktuMulai.isEmpty()) return null;
        for (JamPerkuliahan j : jamList) {
            if (waktuMulai.equals(j.getWaktuMulai())) return j;
        }
        return null;
    }

    /**
     * Bangun set pId yang dosen-nya konflik:
     * dosen1 yang sama dijadwalkan di lebih dari satu kelas pada slot yang sama.
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    private void buildConflictIds(String ta, int smt, Session s) {
        conflictIds.clear();
        try {
            org.hibernate.Criteria cc = s.createCriteria(Perkuliahan.class)
                .add(Restrictions.eq("tahunAjaran", ta));
            batasiKriteriaKeLingkupPengguna(cc);
            applySmt(cc, smt);
            List<Perkuliahan> all = cc.list();

            // key: dosenId_hari_jamId → list pId
            Map<String, List<Long>> slotMap = new HashMap<String, List<Long>>();
            for (Perkuliahan p : all) {
                if (p.getDosen1() == null) continue;
                if (p.getHari() == null || p.getHari().trim().isEmpty()) continue;
                if (p.getJamPerkuliahan() == null) continue;
                String key = p.getDosen1().getId() + "_"
                    + p.getHari().trim() + "_"
                    + p.getJamPerkuliahan().getId();
                List<Long> ids = slotMap.get(key);
                if (ids == null) { ids = new ArrayList<Long>(); slotMap.put(key, ids); }
                ids.add(p.getId());
            }
            for (List<Long> ids : slotMap.values()) {
                if (ids.size() > 1) conflictIds.addAll(ids);
            }
        } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/TimetablePerkuliahanWindow.java:574"); /* non-fatal */ }
    }

    // ── Grid build ────────────────────────────────────────────────────────────

    private void rebuildPalette(List<Perkuliahan> items, boolean showKelas) {
        palette.getChildren().clear();
        if (items.isEmpty()) {
            hint(palette, "Semua sudah terjadwal ✓");
        } else {
            for (Perkuliahan p : items) mkCard(p, showKelas).setParent(palette);
        }
    }

    private void rebuildGrid(List<JamPerkuliahan> jamList,
                             Map<String, List<Perkuliahan>> cellMap,
                             boolean showKelas) {
        gridArea.getChildren().clear();
        if (jamList.isEmpty()) {
            hint(gridArea, "Tidak ada data Jam Perkuliahan.\n"
                + "Buat Jam Perkuliahan terlebih dahulu di master data.");
            return;
        }

        Div tbl = new Div();
        tbl.setSclass("aisttpgridtbl");
        tbl.setParent(gridArea);

        // Header
        Div hdrRow = new Div();
        hdrRow.setSclass("aisttptblrow");
        hdrRow.setParent(tbl);
        hdrCell(hdrRow, "Jam / Hari");
        for (String h : HARI) hdrCell(hdrRow, h);

        for (JamPerkuliahan jam : jamList) {
            Div row = new Div();
            row.setSclass("aisttptblrow");
            row.setParent(tbl);

            Div jamCell = new Div();
            jamCell.setSclass("aisttpjamcell");
            String nama  = jam.getNama();
            String mulai = jam.getWaktuMulai();
            String sel   = jam.getWaktuSelesai();
            StringBuilder lbl = new StringBuilder();
            lbl.append(nama != null && !nama.trim().isEmpty() ? nama : "Jam");
            if (mulai != null && !mulai.isEmpty()) {
                lbl.append("\n").append(mulai);
                if (sel != null && !sel.isEmpty()) lbl.append("-").append(sel);
            }
            new Label(lbl.toString()).setParent(jamCell);
            jamCell.setParent(row);

            for (String hari : HARI) {
                String key  = hari + "_" + jam.getId();
                Div    cell = mkCell(hari, jam.getId(), jam.getWaktuMulai(), jam.getWaktuSelesai());
                List<Perkuliahan> bucket = cellMap.get(key);
                if (bucket != null) {
                    for (Perkuliahan p : bucket) mkCard(p, showKelas).setParent(cell);
                }
                cell.setParent(row);
            }
        }
    }

    private void hdrCell(Div row, String text) {
        Div c = new Div();
        c.setSclass("aisttphdrcell");
        new Label(text).setParent(c);
        c.setParent(row);
    }

    private Div mkCell(final String hari, final Long jamId,
                       final String waktuMulai, final String waktuSelesai) {
        final Div cell = new Div();
        cell.setSclass("aisttpcell");
        cell.setDroppable(DND);
        cell.setAttribute("hari", hari);
        cell.setAttribute("jamId", jamId);
        cell.setAttribute("wm",   waktuMulai);
        cell.setAttribute("ws",   waktuSelesai);
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
     * Buat kartu perkuliahan.
     *
     * @param showKelas true = mode Per Dosen: tampilkan kode kelas di baris 2;
     *                  false = mode Per Jurusan: tampilkan nama dosen di baris 2
     */
    private Div mkCard(final Perkuliahan p, final boolean showKelas) {
        final Long    pId       = p.getId();
        final boolean isLocked  = locked.contains(pId);
        final boolean isConflict = conflictIds.contains(pId);

        final Div card = new Div();
        card.setAttribute("pId", pId);
        card.setDraggable(isLocked ? "false" : DND);

        Long   colorKey = p.getMatakuliah() != null ? p.getMatakuliah().getId() : pId;
        String bg       = CARD_COLORS[color(colorKey)];
        card.setStyle("background:" + bg + ";");

        StringBuilder sc = new StringBuilder("aisttpcard");
        if (isLocked)   sc.append(" locked");
        if (isConflict) sc.append(" conflict");
        card.setSclass(sc.toString());

        // Nama mata kuliah
        final String mkNama = p.getMatakuliah() != null ? p.getMatakuliah().getNama() : "—";
        Div cn = new Div();
        cn.setSclass("cn");
        new Label((isLocked ? "🔒 " : "") + mkNama).setParent(cn);
        cn.setParent(card);

        // Baris 2: dosen atau kelas
        if (showKelas) {
            // Per-Dosen: tampilkan kode kelas
            if (p.getKelas() != null && !p.getKelas().trim().isEmpty()) {
                Div ck = new Div(); ck.setSclass("ck");
                new Label("Kls " + p.getKelas()).setParent(ck);
                ck.setParent(card);
            }
        } else {
            // Per-Jurusan: tampilkan nama dosen + kode kelas
            if (p.getDosen1() != null) {
                Div cd = new Div(); cd.setSclass("cd");
                new Label(p.getDosen1().getNama()).setParent(cd);
                cd.setParent(card);
            }
            if (p.getKelas() != null && !p.getKelas().trim().isEmpty()) {
                Div ck = new Div(); ck.setSclass("ck");
                new Label("Kls " + p.getKelas()).setParent(ck);
                ck.setParent(card);
            }
        }

        // Tombol ×
        final Toolbarbutton del = new Toolbarbutton("×");
        del.setSclass("cdel");
        del.setTooltiptext("Pindah ke Belum Terjadwal");
        del.addEventListener("onClick", new EventListener() {
            public void onEvent(Event ev) throws Exception {
                ev.stopPropagation();
                if (!locked.contains((Long) card.getAttribute("pId"))) cardToPalette(card);
            }
        });
        del.setParent(card);

        // Tombol kunci
        final Toolbarbutton lockBtn = new Toolbarbutton(isLocked ? "🔓" : "🔒");
        lockBtn.setSclass("clock");
        lockBtn.setTooltiptext(isLocked ? "Buka kunci" : "Kunci posisi ini");
        lockBtn.addEventListener("onClick", new EventListener() {
            public void onEvent(Event ev) throws Exception {
                ev.stopPropagation();
                toggleLock(card, lockBtn, mkNama);
            }
        });
        lockBtn.setParent(card);

        // Klik badan kartu -> buka dialog Edit (dosen & kelas).
        card.addEventListener("onClick", new EventListener() {
            public void onEvent(Event ev) throws Exception {
                ev.stopPropagation();
                bukaEditPerkuliahan((Long) card.getAttribute("pId"), card, showKelas);
            }
        });

        return card;
    }

    // ── Kunci / buka kunci ────────────────────────────────────────────────────

    private void toggleLock(Div card, Toolbarbutton lockBtn, String mkNama) {
        Long pId = (Long) card.getAttribute("pId");
        if (locked.contains(pId)) {
            locked.remove(pId);
            card.setDraggable(DND);
            String sc = card.getSclass().replace(" locked", "").trim();
            card.setSclass(sc);
            lockBtn.setLabel("🔒");
            lockBtn.setTooltiptext("Kunci posisi ini");
            setCardName(card, mkNama);
        } else {
            locked.add(pId);
            card.setDraggable("false");
            if (!card.getSclass().contains("locked")) card.setSclass(card.getSclass() + " locked");
            lockBtn.setLabel("🔓");
            lockBtn.setTooltiptext("Buka kunci");
            setCardName(card, "🔒 " + mkNama);
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

    private void cardToCell(Div card, Div cell) {
        // Kartu MK dari kurikulum (belum ada perkuliahan) -> buat perkuliahan baru pada slot ini.
        if (card.getAttribute("pId") == null && card.getAttribute("mkId") != null) {
            buatPerkuliahanDariMk(card, cell);
            return;
        }
        Long pId = (Long) card.getAttribute("pId");
        if (pId == null || locked.contains(pId)) return;

        card.detach();
        card.setParent(cell);
        String hari  = (String) cell.getAttribute("hari");
        Long   jamId = (Long)   cell.getAttribute("jamId");
        String wm    = (String) cell.getAttribute("wm");
        String ws    = (String) cell.getAttribute("ws");
        changes.put(pId, new Object[]{hari, jamId, wm, ws});
    }

    @SuppressWarnings("unchecked")
    private void cardToPalette(Div card) {
        Long pId = (Long) card.getAttribute("pId");
        if (locked.contains(pId)) return;

        for (Object c : new ArrayList<Object>(palette.getChildren())) {
            if (c instanceof Label) ((Component) c).detach();
        }
        card.detach();
        card.setParent(palette);
        changes.put(pId, new Object[]{null, null, null, null});
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
            msgbox("Tidak ditemukan konflik jadwal dosen. Data sudah valid.",
                "Hasil Pemeriksaan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        } else {
            msgbox("Ditemukan " + conflictIds.size() + " slot perkuliahan berkonflik.\n\n"
                + "Konflik terjadi ketika dosen yang sama mengajar di dua kelas "
                + "berbeda pada hari dan jam yang sama.\n\n"
                + "Klik Tampilkan untuk melihat kartu berkonflik (ditandai garis merah).",
                "Konflik Ditemukan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
        }
    }

    // ── Penjadwalan pintar ───────────────────────────────────────────────────

    /** Menampilkan analisis kesiapan sebelum pengguna memilih strategi otomatis. */
    @SuppressWarnings({"rawtypes","unchecked"})
    private void bukaPenjadwalanPintar() throws Exception {
        if (viewMode != 0 || cbTA.getSelectedItem() == null || cbSmt.getSelectedItem() == null
                || cbJurusan.getSelectedItem() == null) {
            msgbox("Pilih tampilan Per Jurusan, Tahun Akademik, Semester, dan Program Studi terlebih dahulu.",
                    "Data Belum Lengkap", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }
        Session session = HibernateUtil.currentSession();
        List<Perkuliahan> data = ambilPerkuliahanTerpilih(session,
                (String) cbTA.getSelectedItem().getValue(),
                ((Integer) cbSmt.getSelectedItem().getValue()).intValue());
        List<JamPerkuliahan> jam = loadJamList(session);
        buildConflictIds((String) cbTA.getSelectedItem().getValue(),
                ((Integer) cbSmt.getSelectedItem().getValue()).intValue(), session);
        final AnalisisJadwal analisis = analisisJadwal(data, jam);

        final MyWindow window = new MyWindow("Penjadwalan Pintar", "normal", true);
        window.setParent(getPage().getFirstRoot());
        window.setWidth("900px");
        window.setHeight("680px");
        window.setContentStyle("padding:12px;overflow:auto;");
        Html dashboard = new Html();
        dashboard.setContent(htmlAnalisis(analisis));
        dashboard.setParent(window);

        Div tombol = new Div();
        tombol.setStyle("display:flex;gap:8px;justify-content:flex-end;margin-top:12px;padding-top:10px;border-top:1px solid #e2e8f0;");
        tombol.setParent(window);
        Button tutup = new Button("Tutup");
        tutup.setSclass("btn btn-default btn-sm");
        tutup.addEventListener("onClick", new EventListener() {
            public void onEvent(Event event) throws Exception { window.detach(); }
        });
        tutup.setParent(tombol);
        Button riwayat = new Button("Susun dari Riwayat Tahun Lalu");
        riwayat.setSclass("btn btn-primary btn-sm");
        riwayat.setDisabled(analisis.belumTerjadwal == 0 || analisis.jumlahJam == 0);
        riwayat.addEventListener("onClick", new EventListener() {
            public void onEvent(Event event) throws Exception {
                window.detach();
                jalankanPenjadwalan(true);
            }
        });
        riwayat.setParent(tombol);
        Button pintar = new Button("Susun dengan Analisis Pintar");
        pintar.setSclass("btn btn-success btn-sm");
        pintar.setDisabled(analisis.belumTerjadwal == 0 || analisis.jumlahJam == 0);
        pintar.addEventListener("onClick", new EventListener() {
            public void onEvent(Event event) throws Exception {
                window.detach();
                jalankanPenjadwalan(false);
            }
        });
        pintar.setParent(tombol);
        window.setVisible(true);
        window.onModal();
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private List<Perkuliahan> ambilPerkuliahanTerpilih(Session session, String ta, int smt) {
        org.hibernate.Criteria kriteria = session.createCriteria(Perkuliahan.class)
                .add(Restrictions.eq("tahunAjaran", ta))
                .add(Restrictions.ne("merupakan_tanpa_jadwal_perkuliahan", Boolean.TRUE));
        applySmt(kriteria, smt);
        batasiKriteriaKeLingkupPengguna(kriteria);
        Long jurusanId = (Long) cbJurusan.getSelectedItem().getValue();
        if (jurusanPengguna != null) jurusanId = jurusanPengguna.getId();
        if (jurusanId != null) {
            Jurusan jurusan = (Jurusan) session.get(Jurusan.class, jurusanId);
            if (jurusan != null) kriteria.add(Restrictions.eq("jurusan", jurusan));
        }
        return kriteria.list();
    }

    private AnalisisJadwal analisisJadwal(List<Perkuliahan> data, List<JamPerkuliahan> jam) {
        AnalisisJadwal hasil = new AnalisisJadwal();
        hasil.total = data.size();
        hasil.jumlahJam = jam.size();
        for (Perkuliahan p : data) {
            boolean terjadwal = p.getHari() != null && !p.getHari().trim().isEmpty()
                    && (p.getJamPerkuliahan() != null || matchJam(p.getWaktuMulai(), jam) != null);
            if (terjadwal) hasil.terjadwal++; else hasil.belumTerjadwal++;
            if (p.getDosen1() == null) hasil.tanpaDosen++;
            if (p.getRuang() == null) hasil.tanpaRuang++;
        }
        hasil.konflik = conflictIds.size();
        return hasil;
    }

    private String htmlAnalisis(AnalisisJadwal a) {
        StringBuilder h = new StringBuilder();
        h.append("<div style='font-family:Arial,sans-serif;color:#334155'>")
         .append("<div style='font-size:16px;font-weight:bold;color:#1e3a5f'>Analisis Kesiapan Penjadwalan</div>")
         .append("<div style='font-size:11px;color:#64748b;margin:4px 0 12px'>Sistem hanya mengisi perkuliahan yang belum terjadwal. Jadwal yang sudah ada tidak diubah.</div>")
         .append("<div style='display:grid;grid-template-columns:repeat(6,1fr);gap:8px'>");
        kartuAnalisis(h, "Total Kelas", a.total, "#2563eb");
        kartuAnalisis(h, "Terjadwal", a.terjadwal, "#059669");
        kartuAnalisis(h, "Belum Terjadwal", a.belumTerjadwal, "#dc2626");
        kartuAnalisis(h, "Tanpa Dosen", a.tanpaDosen, "#d97706");
        kartuAnalisis(h, "Tanpa Ruang", a.tanpaRuang, "#7c3aed");
        kartuAnalisis(h, "Konflik", a.konflik, "#be123c");
        h.append("</div><div style='display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:12px'>")
         .append("<div style='border:1px solid #dbe4ee;border-radius:8px;padding:12px'><b>Data yang perlu disiapkan</b><ul style='line-height:1.8;margin:8px 0 0 18px'>");
        if (a.jumlahJam == 0) h.append("<li><b>Wajib:</b> buat Jam Perkuliahan melalui tombol Buat Waktu Default atau Kelola Jam.</li>");
        if (a.tanpaDosen > 0) h.append("<li>Lengkapi dosen pengampu pada ").append(a.tanpaDosen).append(" kelas agar bentrok dosen dapat dicegah.</li>");
        if (a.tanpaRuang > 0) h.append("<li>Lengkapi ruang pada ").append(a.tanpaRuang).append(" kelas agar bentrok ruang dapat dianalisis.</li>");
        if (a.belumTerjadwal == 0) h.append("<li>Semua kelas sudah memiliki jadwal. Gunakan Periksa Konflik untuk validasi akhir.</li>");
        h.append("</ul></div><div style='border:1px solid #dbe4ee;border-radius:8px;padding:12px'><b>Cara kerja rekomendasi</b>")
         .append("<ol style='line-height:1.8;margin:8px 0 0 18px'><li><b>Riwayat:</b> mencocokkan mata kuliah dan kelas dengan tahun akademik sebelumnya.</li>")
         .append("<li><b>Analisis pintar:</b> memilih hari dan jam dengan beban paling rendah serta menghindari bentrok dosen dan ruang.</li>")
         .append("<li>Periode aktif dan lingkup program studi tetap diperiksa sebelum data disimpan.</li></ol></div></div>")
         .append("<div style='margin-top:12px;padding:10px;border-radius:6px;background:#eff6ff;color:#1e40af'><b>Langkah berikutnya:</b> pilih salah satu strategi di bawah. Setelah selesai, periksa hasil pada timetable dan jalankan Periksa Konflik.</div></div>");
        return h.toString();
    }

    private void kartuAnalisis(StringBuilder h, String label, int nilai, String warna) {
        h.append("<div style='border:1px solid #dbe4ee;border-radius:7px;padding:9px;background:#fff'><div style='font-size:10px;color:#64748b'>")
         .append(label).append("</div><div style='font-size:22px;font-weight:bold;color:").append(warna).append("'>")
         .append(nilai).append("</div></div>");
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private void jalankanPenjadwalan(boolean dariRiwayat) {
        try {
            if (viewMode != 0 || cbTA.getSelectedItem() == null || cbSmt.getSelectedItem() == null) return;
            String ta = (String) cbTA.getSelectedItem().getValue();
            int smt = ((Integer) cbSmt.getSelectedItem().getValue()).intValue();
            Session session = HibernateUtil.currentSession();
            List<Perkuliahan> target = ambilPerkuliahanTerpilih(session, ta, smt);
            List<JamPerkuliahan> jam = loadJamList(session);
            if (jam.isEmpty()) {
                msgbox("Jam Perkuliahan belum tersedia. Buat waktu default atau kelola jam terlebih dahulu.",
                        "Data Belum Lengkap", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                return;
            }
            Map<String, Perkuliahan> riwayat = dariRiwayat ? ambilPolaRiwayat(session, ta, smt) : new HashMap<String, Perkuliahan>();
            Map<String, Integer> beban = new HashMap<String, Integer>();
            Set<String> dosenTerpakai = new HashSet<String>();
            Set<String> ruangTerpakai = new HashSet<String>();
            isiPemakaianSlot(session, ta, smt, jam, beban, dosenTerpakai, ruangTerpakai);
            int tersimpan = 0, tanpaPola = 0, tanpaSlot = 0;
            for (Perkuliahan p : target) {
                if (p.getHari() != null && !p.getHari().trim().isEmpty()) continue;
                if (!bolehMengubahJadwal(p, true)) return;
                if (p.getDosen1() == null) { tanpaSlot++; continue; }
                SlotJadwal slot = null;
                if (dariRiwayat) {
                    Perkuliahan lama = riwayat.get(kunciRiwayat(p));
                    if (lama != null) {
                        JamPerkuliahan jamCocok = matchJam(lama.getWaktuMulai(), jam);
                        if (jamCocok != null && slotTersedia(p, lama.getHari(), jamCocok, dosenTerpakai, ruangTerpakai))
                            slot = new SlotJadwal(lama.getHari(), jamCocok);
                    }
                    if (slot == null) { tanpaPola++; continue; }
                } else {
                    slot = pilihSlotTerbaik(p, jam, beban, dosenTerpakai, ruangTerpakai);
                    if (slot == null) { tanpaSlot++; continue; }
                }
                p.setHari(slot.hari);
                p.setJamPerkuliahan(slot.jam);
                p.setWaktuMulai(slot.jam.getWaktuMulai());
                p.setWaktuSelesai(slot.jam.getWaktuSelesai());
                session.saveOrUpdate(p);
                tandaiPemakaian(p, slot.hari, slot.jam, beban, dosenTerpakai, ruangTerpakai);
                tersimpan++;
            }
            session.flush();
            doLoad();
            String tambahan = dariRiwayat && tanpaPola > 0 ? "\n" + tanpaPola + " kelas tidak memiliki pola riwayat yang aman."
                    : (!dariRiwayat && tanpaSlot > 0 ? "\n" + tanpaSlot + " kelas belum memperoleh slot bebas." : "");
            msgbox(tersimpan + " kelas berhasil dijadwalkan otomatis." + tambahan
                    + "\n\nSilakan periksa hasil dan jalankan Periksa Konflik.",
                    "Penjadwalan Selesai", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            PesanFormalHelper.tampilkanGagalException("menjalankan penjadwalan pintar", e,
                    new String[] { "Periksa kelengkapan dosen, ruang, dan jam perkuliahan.",
                            "Muat ulang halaman lalu coba kembali." });
        }
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private Map<String, Perkuliahan> ambilPolaRiwayat(Session session, String ta, int smt) {
        Map<String, Perkuliahan> hasil = new HashMap<String, Perkuliahan>();
        String sebelumnya = tahunSebelumnya(ta);
        if (sebelumnya == null) return hasil;
        org.hibernate.Criteria c = session.createCriteria(Perkuliahan.class)
                .add(Restrictions.eq("tahunAjaran", sebelumnya))
                .add(Restrictions.ne("merupakan_tanpa_jadwal_perkuliahan", Boolean.TRUE));
        applySmt(c, smt);
        batasiKriteriaKeLingkupPengguna(c);
        if (cbJurusan != null && cbJurusan.getSelectedItem() != null
                && cbJurusan.getSelectedItem().getValue() != null) {
            Jurusan jurusan = (Jurusan) session.get(Jurusan.class,
                    (Long) cbJurusan.getSelectedItem().getValue());
            if (jurusan != null) c.add(Restrictions.eq("jurusan", jurusan));
        }
        for (Object object : c.list()) {
            Perkuliahan p = (Perkuliahan) object;
            if (p.getHari() != null && !p.getHari().trim().isEmpty() && p.getWaktuMulai() != null)
                hasil.put(kunciRiwayat(p), p);
        }
        return hasil;
    }

    private String tahunSebelumnya(String ta) {
        try {
            String[] bagian = ta.split("/");
            return (Integer.parseInt(bagian[0]) - 1) + "/" + (Integer.parseInt(bagian[1]) - 1);
        } catch (Exception e) { return null; }
    }

    private String kunciRiwayat(Perkuliahan p) {
        String mk = p.getMatakuliah() == null || p.getMatakuliah().getId() == null ? "-" : p.getMatakuliah().getId().toString();
        String kelas = p.getKelas() == null ? "" : p.getKelas().trim().toLowerCase();
        return mk + "|" + kelas;
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private void isiPemakaianSlot(Session session, String ta, int smt, List<JamPerkuliahan> jam,
            Map<String, Integer> beban, Set<String> dosen, Set<String> ruang) {
        org.hibernate.Criteria c = session.createCriteria(Perkuliahan.class)
                .add(Restrictions.eq("tahunAjaran", ta))
                .add(Restrictions.ne("merupakan_tanpa_jadwal_perkuliahan", Boolean.TRUE));
        applySmt(c, smt);
        for (Object object : c.list()) {
            Perkuliahan p = (Perkuliahan) object;
            JamPerkuliahan jp = p.getJamPerkuliahan() != null ? p.getJamPerkuliahan() : matchJam(p.getWaktuMulai(), jam);
            if (jp != null && p.getHari() != null && !p.getHari().trim().isEmpty())
                tandaiPemakaian(p, p.getHari(), jp, beban, dosen, ruang);
        }
    }

    private SlotJadwal pilihSlotTerbaik(Perkuliahan p, List<JamPerkuliahan> jam,
            Map<String, Integer> beban, Set<String> dosen, Set<String> ruang) {
        SlotJadwal terbaik = null;
        int nilaiTerbaik = Integer.MAX_VALUE;
        for (String hari : HARI) for (JamPerkuliahan jp : jam) {
            if (!slotTersedia(p, hari, jp, dosen, ruang)) continue;
            Integer nilai = beban.get(kunciSlot(hari, jp));
            int skor = nilai == null ? 0 : nilai.intValue();
            if (skor < nilaiTerbaik) { nilaiTerbaik = skor; terbaik = new SlotJadwal(hari, jp); }
        }
        return terbaik;
    }

    private boolean slotTersedia(Perkuliahan p, String hari, JamPerkuliahan jam,
            Set<String> dosen, Set<String> ruang) {
        if (hari == null || jam == null) return false;
        String slot = kunciSlot(hari, jam);
        return (p.getDosen1() == null || !dosen.contains(slot + "|" + p.getDosen1().getId()))
                && (p.getRuang() == null || !ruang.contains(slot + "|" + p.getRuang().getId()));
    }

    private void tandaiPemakaian(Perkuliahan p, String hari, JamPerkuliahan jam,
            Map<String, Integer> beban, Set<String> dosen, Set<String> ruang) {
        String slot = kunciSlot(hari, jam);
        Integer jumlah = beban.get(slot);
        beban.put(slot, Integer.valueOf(jumlah == null ? 1 : jumlah.intValue() + 1));
        if (p.getDosen1() != null) dosen.add(slot + "|" + p.getDosen1().getId());
        if (p.getRuang() != null) ruang.add(slot + "|" + p.getRuang().getId());
    }

    private String kunciSlot(String hari, JamPerkuliahan jam) {
        return hari.trim().toLowerCase() + "|" + normalisasiJam(jam.getWaktuMulai())
                + "|" + normalisasiJam(jam.getWaktuSelesai());
    }

    private String normalisasiJam(String jam) {
        return jam == null ? "" : jam.trim().replace('.', ':');
    }

    private static final class AnalisisJadwal {
        int total, terjadwal, belumTerjadwal, tanpaDosen, tanpaRuang, konflik, jumlahJam;
    }

    private static final class SlotJadwal {
        final String hari;
        final JamPerkuliahan jam;
        SlotJadwal(String hari, JamPerkuliahan jam) { this.hari = hari; this.jam = jam; }
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
            for (Map.Entry<Long, Object[]> entry : changes.entrySet()) {
                Perkuliahan p = (Perkuliahan) s.get(Perkuliahan.class, entry.getKey());
                if (p != null && !bolehMengubahJadwal(p, true)) return;
            }
            int saved = 0;
            for (Map.Entry<Long, Object[]> entry : changes.entrySet()) {
                Perkuliahan p = (Perkuliahan) s.get(Perkuliahan.class, entry.getKey());
                if (p == null) continue;
                Object[] change = entry.getValue();
                String newHari  = (String) change[0];
                Long   newJamId = (Long)   change[1];
                String newWm    = (String) change[2];
                String newWs    = (String) change[3];

                p.setHari(newHari != null ? newHari : "");
                JamPerkuliahan jam = (newJamId != null)
                    ? (JamPerkuliahan) s.get(JamPerkuliahan.class, newJamId) : null;
                p.setJamPerkuliahan(jam);
                if (newWm != null) p.setWaktuMulai(newWm);
                if (newWs != null) p.setWaktuSelesai(newWs);

                s.saveOrUpdate(p);
                saved++;
            }
            changes.clear();
            msgbox(saved + " perkuliahan berhasil disimpan.", "Berhasil",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            PesanFormalHelper.tampilkanGagalException(
                    "menyimpan perubahan jadwal (timetable) perkuliahan",
                    e, new String[] {
                            "Muat ulang (refresh) halaman ini lalu periksa apakah perubahan sempat tersimpan.",
                            "Ulangi kembali perubahan jadwal yang belum tersimpan.",
                            "Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
                    });
        }
    }

    // ── Waktu/Jam Perkuliahan: buat default, kelola, cetak ──────────────────────

    /** Preset waktu default (10 slot 49 menit) sesuai template jadwal. */
    private static final String[][] JAM_DEFAULT = {
        {"Jam ke-1","07.30","08.19"}, {"Jam ke-2","08.20","09.09"},
        {"Jam ke-3","09.10","09.59"}, {"Jam ke-4","10.00","10.49"},
        {"Jam ke-5","10.50","11.39"}, {"Jam ke-6","13.10","13.59"},
        {"Jam ke-7","14.00","14.49"}, {"Jam ke-8","14.50","15.39"},
        {"Jam ke-9","16.10","16.59"}, {"Jam ke-10","17.00","17.49"}
    };

    /** Mengubah string waktu "07.30" menjadi Date (untuk kolom {@code mulai} pengurutan). */
    private static java.util.Date jamKeTanggal(String hhmm) {
        try {
            String cl = hhmm.replace(".", ":").trim();
            String[] pr = cl.split(":");
            int h = Integer.parseInt(pr[0].trim());
            int m = pr.length > 1 ? Integer.parseInt(pr[1].trim()) : 0;
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.set(2000, 0, 1, h, m, 0);
            c.set(java.util.Calendar.MILLISECOND, 0);
            return c.getTime();
        } catch (Exception e) {
            return new java.util.Date();
        }
    }

    /** Membuat JamPerkuliahan default (10 slot) untuk jurusan terpilih (Jurusan WAJIB dipilih dulu). */
    @SuppressWarnings({"rawtypes","unchecked"})
    private void buatWaktuDefault() throws Exception {
        if (cbJurusan == null || cbJurusan.getSelectedItem() == null
                || cbJurusan.getSelectedItem().getValue() == null) {
            msgbox("Jurusan wajib dipilih terlebih dahulu (Jam Perkuliahan dibuat per jurusan).",
                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }
        if (!bolehMengubahJamTerpilih()) return;
        final Long jurId = (Long) cbJurusan.getSelectedItem().getValue();
        MyMessageboxConfig.show(
            "Buat waktu/jam default (10 slot, 07.30-17.49) untuk jurusan terpilih? "
                + "Jam yang sudah ada tidak akan diduplikasi.",
            "Konfirmasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
            new EventListener() {
                public void onEvent(Event e) throws Exception {
                    if (Integer.parseInt(e.getData().toString()) != MyMessageboxConfig.OK) return;
                    if (!bolehMengubahJamTerpilih()) return;
                    try {
                        Session s = HibernateUtil.currentSession();
                        Jurusan j = (Jurusan) s.get(Jurusan.class, jurId);
                        Set<String> ada = new HashSet<String>();
                        List ex = s.createCriteria(JamPerkuliahan.class)
                            .add(Restrictions.eq("jurusan", j)).list();
                        for (Object o : ex) {
                            String wm = ((JamPerkuliahan) o).getWaktuMulai();
                            if (wm != null) ada.add(wm.trim());
                        }
                        int dibuat = 0;
                        for (int i = 0; i < JAM_DEFAULT.length; i++) {
                            String[] d = JAM_DEFAULT[i];
                            if (ada.contains(d[1])) continue;
                            JamPerkuliahan jp = new JamPerkuliahan();
                            jp.setNama(d[0]);
                            jp.setJurusan(j);
                            jp.setWaktuMulai(d[1]);
                            jp.setWaktuSelesai(d[2]);
                            jp.setMulai(jamKeTanggal(d[1]));
                            s.save(jp);
                            dibuat++;
                        }
                        s.flush();
                        msgbox(dibuat + " jam perkuliahan dibuat. Klik Tampilkan untuk memuat.",
                            "Berhasil", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        doLoad();
                    } catch (Exception ex) {
                        Common.tampilErrorJikaAdmin(ex);
                        PesanFormalHelper.tampilkanGagalException(
                                "membuat jam perkuliahan default untuk jurusan terpilih",
                                ex, new String[] {
                                        "Muat ulang (refresh) halaman ini lalu coba proses ini kembali.",
                                        "Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
                                });
                    }
                }
            });
    }

    /** Dialog kelola Jam Perkuliahan (tambah/ubah/hapus manual) untuk jurusan terpilih. */
    @SuppressWarnings({"rawtypes","unchecked"})
    private void bukaKelolaJam() throws Exception {
        if (cbJurusan == null || cbJurusan.getSelectedItem() == null
                || cbJurusan.getSelectedItem().getValue() == null) {
            msgbox("Jurusan wajib dipilih terlebih dahulu (Jam Perkuliahan dikelola per jurusan).",
                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }
        if (!bolehMengubahJamTerpilih()) return;
        final Long jurId = (Long) cbJurusan.getSelectedItem().getValue();
        final Session s = HibernateUtil.currentSession();
        final Jurusan jur = (Jurusan) s.get(Jurusan.class, jurId);

        final MyWindow w = new MyWindow("Kelola Jam Perkuliahan" + (jur != null ? " - " + jur.getNama() : ""),
            "normal", true);
        w.setParent(getPage().getFirstRoot());
        w.setWidth("560px");
        w.setContentStyle("padding:10px;");

        Div body = new Div();
        body.setStyle("display:flex;flex-direction:column;gap:6px;font-size:12px;");
        body.setParent(w);
        Label info = new Label("Ubah / tambah / hapus jam. Format waktu: 07.30  (klik Simpan untuk menyimpan semua)");
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
        List ex = s.createCriteria(JamPerkuliahan.class).add(Restrictions.eq("jurusan", jur))
            .addOrder(Order.asc("mulai")).list();
        for (Object o : ex) {
            JamPerkuliahan jp = (JamPerkuliahan) o;
            asalIds.add(jp.getId());
            tambahBarisKelola(listWrap, rows, jp.getId(), jp.getNama(), jp.getWaktuMulai(), jp.getWaktuSelesai());
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
                    if (!bolehMengubahJamTerpilih()) return;
                    Session sessionSimpan = HibernateUtil.currentSession();
                    Jurusan jurusanSimpan = (Jurusan) sessionSimpan.get(Jurusan.class, jurId);
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
                        JamPerkuliahan jp = (id != null)
                            ? (JamPerkuliahan) sessionSimpan.get(JamPerkuliahan.class, id) : new JamPerkuliahan();
                        if (jp == null) jp = new JamPerkuliahan();
                        jp.setNama(tNama.getValue());
                        jp.setJurusan(jurusanSimpan);
                        jp.setWaktuMulai(wm);
                        jp.setWaktuSelesai(ws);
                        jp.setMulai(jamKeTanggal(wm));
                        sessionSimpan.saveOrUpdate(jp);
                        if (jp.getId() != null) present.add(jp.getId());
                    }
                    for (java.util.Iterator<Long> it = asalIds.iterator(); it.hasNext();) {
                        Long oid = it.next();
                        if (!present.contains(oid)) {
                            JamPerkuliahan del = (JamPerkuliahan) sessionSimpan.get(JamPerkuliahan.class, oid);
                            if (del != null) sessionSimpan.delete(del);
                        }
                    }
                    sessionSimpan.flush();
                    w.detach();
                    msgbox("Jam perkuliahan tersimpan. Klik Tampilkan untuk memuat ulang.",
                        "Berhasil", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                    doLoad();
                } catch (Exception ex) {
                    Common.tampilErrorJikaAdmin(ex);
                    PesanFormalHelper.tampilkanGagalException(
                            "menyimpan data Jam Perkuliahan",
                            ex, new String[] {
                                    "Muat ulang (refresh) halaman ini lalu periksa apakah perubahan sempat tersimpan.",
                                    "Ulangi kembali perubahan jam perkuliahan yang belum tersimpan.",
                                    "Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
                            });
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

    /** Mencetak jadwal (format tabel Hari x Jam seperti template) di jendela baru lalu print. */
    @SuppressWarnings({"rawtypes","unchecked"})
    private void cetakJadwal() {
        if (cbTA.getSelectedItem() == null || cbSmt.getSelectedItem() == null) {
            msgbox("Pilih Tahun Ajaran & Semester terlebih dahulu.", "Peringatan",
                MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }
        try {
            Session s = HibernateUtil.currentSession();
            String ta  = (String)  cbTA.getSelectedItem().getValue();
            int    smt = (Integer) cbSmt.getSelectedItem().getValue();
            List<JamPerkuliahan> jamList = loadJamList(s);

            Long jurId = (cbJurusan != null && cbJurusan.getSelectedItem() != null)
                ? (Long) cbJurusan.getSelectedItem().getValue() : null;
            Jurusan jur = (jurId != null) ? (Jurusan) s.get(Jurusan.class, jurId) : null;

            org.hibernate.Criteria c = s.createCriteria(Perkuliahan.class)
                .add(Restrictions.eq("tahunAjaran", ta))
                .add(Restrictions.ne("merupakan_tanpa_jadwal_perkuliahan", Boolean.TRUE));
            applySmt(c, smt);
            if (jur != null) c.add(Restrictions.eq("jurusan", jur));
            List<Perkuliahan> pAll = c.list();

            Map<String, List<Perkuliahan>> cellMap = new HashMap<String, List<Perkuliahan>>();
            for (Perkuliahan p : pAll) {
                String hari = p.getHari();
                if (hari == null || hari.trim().isEmpty()) continue;
                JamPerkuliahan jam = p.getJamPerkuliahan();
                if (jam == null) jam = matchJam(p.getWaktuMulai(), jamList);
                if (jam == null) continue;
                String key = hari.trim() + "_" + jam.getId();
                List<Perkuliahan> b = cellMap.get(key);
                if (b == null) { b = new ArrayList<Perkuliahan>(); cellMap.put(key, b); }
                b.add(p);
            }

            String html = buildCetakHtml(ta, smt, jur, jamList, cellMap);
            String js = "var w=window.open('','_blank');"
                + "if(w){w.document.open();w.document.write(" + org.json.JSONObject.quote(html) + ");"
                + "w.document.close();w.focus();setTimeout(function(){try{w.print();}catch(e){}},600);}"
                + "else{alert('Popup diblokir browser. Izinkan popup untuk mencetak.');}";
            Clients.evalJavaScript(js);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            PesanFormalHelper.tampilkanGagalException(
                    "mencetak jadwal (timetable) perkuliahan",
                    e, new String[] {
                            "Pastikan Tahun Ajaran dan Semester yang dipilih sudah benar.",
                            "Muat ulang (refresh) halaman ini lalu coba cetak kembali.",
                            "Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
                    });
        }
    }

    /** Menyusun HTML jadwal siap-cetak: judul + Jurusan + tabel Hari x Jam berisi MK/Kelas/Dosen. */
    private String buildCetakHtml(String ta, int smt, Jurusan jur,
                                  List<JamPerkuliahan> jamList, Map<String, List<Perkuliahan>> cellMap) {
        String smtLbl = smt == 1 ? "GANJIL" : (smt == 2 ? "GENAP" : "SEMESTER PENDEK");
        String institusi = "";
        try { institusi = Common.getKonfigurasi("label_universitas", "").getNilai(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TimetablePerkuliahanWindow.java:1161");}
        String judulJur = jur != null ? jur.getNama() : "SEMUA JURUSAN";

        StringBuilder h = new StringBuilder();
        h.append("<html><head><meta charset='UTF-8'><title>Jadwal ").append(esc(judulJur)).append("</title>");
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
        h.append("<div class='t2'>").append(esc(judulJur)).append("</div>");
        if (institusi != null && !institusi.isEmpty()) {
            h.append("<div class='t3'>").append(esc(institusi)).append("</div>");
        }
        h.append("</div>");

        h.append("<table><thead><tr><th class='dcol'>Hari</th>");
        for (JamPerkuliahan jam : jamList) {
            String wm = jam.getWaktuMulai(), ws = jam.getWaktuSelesai();
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
            for (JamPerkuliahan jam : jamList) {
                h.append("<td>");
                List<Perkuliahan> bucket = cellMap.get(hari + "_" + jam.getId());
                if (bucket != null) {
                    for (Perkuliahan p : bucket) {
                        String mk = p.getMatakuliah() != null ? p.getMatakuliah().getNama() : "-";
                        h.append("<div class='slot'><div class='mk'>").append(esc(mk)).append("</div>");
                        StringBuilder sub = new StringBuilder();
                        if (p.getKelas() != null && !p.getKelas().trim().isEmpty()) {
                            sub.append("Kls ").append(p.getKelas());
                        }
                        if (p.getDosen1() != null && p.getDosen1().getNama() != null) {
                            if (sub.length() > 0) sub.append(" - ");
                            sub.append(p.getDosen1().getNama());
                        }
                        if (sub.length() > 0) {
                            h.append("<div class='sub'>").append(esc(sub.toString())).append("</div>");
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

    // ── Filter Semester Pendek (SP) ────────────────────────────────────────────

    /**
     * Menambahkan penyaring semester pada kriteria. Untuk "Sem. Pendek" (nilai 3) memakai flag
     * {@code statusSemesterPendek = SEMESTER_PENDEK} (perkuliahan SP tidak memakai semester=3);
     * untuk Ganjil/Genap memakai {@code semester = smt} dan mengecualikan yang berflag SP.
     */
    private void applySmt(org.hibernate.Criteria c, int smt) {
        if (smt == 3) {
            c.add(Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK));
        } else {
            c.add(Restrictions.eq("ganjilGenap", smt == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP));
            c.add(Restrictions.or(Restrictions.isNull("statusSemesterPendek"),
                    Restrictions.ne("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)));
        }
    }

    private void batasiKriteriaKeLingkupPengguna(org.hibernate.Criteria c) {
        if (jurusanPengguna != null) {
            c.add(Restrictions.eq("jurusan", jurusanPengguna));
        } else if (fakultasPengguna != null) {
            c.createAlias("jurusan", "jurusanScopePengguna")
                .add(Restrictions.eq("jurusanScopePengguna.fakultas", fakultasPengguna));
        }
    }

    private boolean bolehAksesJurusan(Jurusan jurusan) {
        if (jurusanPengguna != null) {
            return jurusan != null && jurusan.getId() != null
                    && jurusan.getId().equals(jurusanPengguna.getId());
        }
        if (fakultasPengguna != null) {
            return jurusan != null && jurusan.getFakultas() != null
                    && jurusan.getFakultas().getId().equals(fakultasPengguna.getId());
        }
        return true;
    }

    private boolean bolehMengubahJadwal(Perkuliahan perkuliahan, boolean tampilkanPesan) {
        if (perkuliahan == null || !bolehAksesJurusan(perkuliahan.getJurusan())) {
            if (tampilkanPesan) {
                msgbox("Jadwal tidak dapat diubah karena berada di luar program studi pengguna.", "Peringatan",
                        MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            }
            return false;
        }
        boolean sp = Perkuliahan.SEMESTER_PENDEK.equals(perkuliahan.getStatusSemesterPendek());
        String jenisSemester = sp ? Perkuliahan.SP : perkuliahan.getGanjilGenap();
        if (jenisSemester == null || jenisSemester.trim().isEmpty()) {
            jenisSemester = perkuliahan.getSemester() != null && perkuliahan.getSemester() % 2 == 0
                    ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
        }
        boolean tidakAktif = CommonPenjadwalan.apakahPenjadwalanTidakAktif(perkuliahan.getTahunAjaran(),
                jenisSemester, sp ? Perkuliahan.SEMESTER_PENDEK : null, perkuliahan);
        if (tidakAktif && tampilkanPesan) {
            msgbox("Jadwal tidak dapat diubah karena periode penjadwalan belum aktif untuk program studi ini.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
        }
        return !tidakAktif;
    }

    private boolean bolehMengubahJamTerpilih() {
        if (cbTA == null || cbTA.getSelectedItem() == null || cbSmt == null || cbSmt.getSelectedItem() == null
                || cbJurusan == null || cbJurusan.getSelectedItem() == null
                || cbJurusan.getSelectedItem().getValue() == null) {
            msgbox("Pilih Tahun Akademik, Semester, dan Program Studi terlebih dahulu.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        Jurusan jurusan = (Jurusan) session.get(Jurusan.class,
                (Long) cbJurusan.getSelectedItem().getValue());
        if (!bolehAksesJurusan(jurusan)) {
            msgbox("Jam perkuliahan tidak dapat diubah karena program studi berada di luar lingkup pengguna.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return false;
        }
        int smt = (Integer) cbSmt.getSelectedItem().getValue();
        boolean sp = smt == 3;
        String jenisSemester = sp ? Perkuliahan.SP : (smt == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
        String tahunAkademik = (String) cbTA.getSelectedItem().getValue();
        if (CommonPenjadwalan.apakahPenjadwalanTidakAktif(tahunAkademik, jenisSemester,
                sp ? Perkuliahan.SEMESTER_PENDEK : null, jurusan.getFakultas(), jurusan, null)) {
            msgbox("Jam perkuliahan tidak dapat diubah karena periode penjadwalan belum aktif untuk program studi ini.",
                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return false;
        }
        return true;
    }

    // ── Filter Kurikulum + kartu MK ─────────────────────────────────────────────

    /** Mengisi combobox Kurikulum (opsi "tanpa kurikulum" + daftar kurikulum, disaring jurusan bila ada). */
    @SuppressWarnings({"rawtypes","unchecked"})
    private void isiKurikulum(Long jurusanId) {
        if (cbKurikulum == null) return;
        cbKurikulum.getItems().clear();
        cbKurikulum.appendItem("= Tanpa kurikulum =").setValue(null);
        try {
            Session s = HibernateUtil.currentSession();
            if (jurusanPengguna != null) jurusanId = jurusanPengguna.getId();
            org.hibernate.Criteria c = s.createCriteria(Kurikulum.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                .addOrder(Order.desc("id")).setMaxResults(200);
            if (jurusanId != null) {
                Jurusan j = (Jurusan) s.get(Jurusan.class, jurusanId);
                if (j != null) c.add(Restrictions.eq("jurusan", j));
            } else if (fakultasPengguna != null) {
                c.createAlias("jurusan", "jurusanScope")
                    .add(Restrictions.eq("jurusanScope.fakultas", fakultasPengguna));
            }
            for (Object o : c.list()) {
                Kurikulum k = (Kurikulum) o;
                cbKurikulum.appendItem(k.getNama()).setValue(k.getId());
            }
        } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/TimetablePerkuliahanWindow.java:1281"); /* non-fatal */ }
        cbKurikulum.setSelectedIndex(0);
    }

    /**
     * Menambahkan kartu MK kurikulum yang BELUM memiliki perkuliahan (untuk TA/smt/jurusan aktif) ke
     * panel "Belum Terjadwal". Kartu ini dapat diseret ke sel untuk membuat perkuliahan baru.
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    private void appendMkCards(Long kurId, List<Perkuliahan> pAll, Session s) {
        try {
            Kurikulum kur = (Kurikulum) s.get(Kurikulum.class, kurId);
            if (kur == null) return;
            Set<Long> adaMk = new HashSet<Long>();
            for (Perkuliahan p : pAll) {
                if (p.getMatakuliah() != null) adaMk.add(p.getMatakuliah().getId());
            }
            List kpmList = s.createCriteria(KurikulumPunyaMatakuliah.class)
                .add(Restrictions.eq("kurikulum", kur)).list();
            int ditambah = 0;
            for (Object o : kpmList) {
                KurikulumPunyaMatakuliah kpm = (KurikulumPunyaMatakuliah) o;
                Matakuliah mk = kpm.getMatakuliah();
                if (mk == null || !mk.getAktif().booleanValue() || adaMk.contains(mk.getId())) continue;
                if (ditambah == 0) hapusHint(palette);
                mkCard(mk, kur, kpm).setParent(palette);
                ditambah++;
            }
        } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/TimetablePerkuliahanWindow.java:1309"); /* non-fatal */ }
    }

    /** Kartu MK kurikulum (belum ada perkuliahan) — putih, garis putus-putus, dapat diseret. */
    private Div mkCard(final Matakuliah mk, final Kurikulum kur, final KurikulumPunyaMatakuliah kpm) {
        final Div card = new Div();
        card.setAttribute("mkId", mk.getId());
        card.setAttribute("kurId", kur.getId());
        card.setAttribute("kpmSmt", kpm.getSemester());
        card.setDraggable(DND);
        card.setSclass("aisttpcard aisttpmk");
        Div cn = new Div(); cn.setSclass("cn");
        new Label("+ " + (mk.getNama() != null ? mk.getNama() : "-")).setParent(cn);
        cn.setParent(card);
        Div cd = new Div(); cd.setSclass("cd");
        new Label(ais.common.Common.getBahasaConfig("MK kurikulum - seret ke sel untuk buat perkuliahan")).setParent(cd);
        cd.setParent(card);
        return card;
    }

    /** Membuat Perkuliahan baru dari kartu MK yang dijatuhkan ke sebuah sel, lalu menggantinya jadi kartu nyata. */
    private void buatPerkuliahanDariMk(Div mkCardEl, Div cell) {
        try {
            if (cbTA.getSelectedItem() == null || cbSmt.getSelectedItem() == null) return;
            Session s = HibernateUtil.currentSession();
            Long mkId  = (Long) mkCardEl.getAttribute("mkId");
            Long kurId = (Long) mkCardEl.getAttribute("kurId");
            Matakuliah mk = (Matakuliah) s.get(Matakuliah.class, mkId);
            Kurikulum  kur = (Kurikulum)  s.get(Kurikulum.class, kurId);
            if (mk == null || kur == null) return;

            String ta  = (String)  cbTA.getSelectedItem().getValue();
            int    smt = (Integer) cbSmt.getSelectedItem().getValue();

            Perkuliahan p = new Perkuliahan();
            p.setMatakuliah(mk);
            p.setKurikulum(kur);
            p.setJurusan(kur.getJurusan() != null ? kur.getJurusan() : mk.getJurusan());
            p.setTahunAjaran(ta);
            if (smt == 3) {
                p.setStatusSemesterPendek(Perkuliahan.SEMESTER_PENDEK);
                Integer ks = (Integer) mkCardEl.getAttribute("kpmSmt");
                if (ks != null) p.setSemester(ks);
            } else {
                p.setSemester(Integer.valueOf(smt));
                p.setGanjilGenap(smt == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
            }
            p.setKelas("");
            String hari  = (String) cell.getAttribute("hari");
            Long   jamId = (Long)   cell.getAttribute("jamId");
            String wm    = (String) cell.getAttribute("wm");
            String ws    = (String) cell.getAttribute("ws");
            p.setHari(hari);
            JamPerkuliahan jam = (jamId != null) ? (JamPerkuliahan) s.get(JamPerkuliahan.class, jamId) : null;
            p.setJamPerkuliahan(jam);
            if (wm != null) p.setWaktuMulai(wm);
            if (ws != null) p.setWaktuSelesai(ws);

            if (!bolehMengubahJadwal(p, true)) return;

            s.save(p);
            s.flush();

            mkCardEl.detach();
            mkCard(p, viewMode == 1).setParent(cell);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    // ── Edit kartu ──────────────────────────────────────────────────────────────

    /** Dialog edit ringkas untuk satu perkuliahan: kelas &amp; dosen pengampu. */
    @SuppressWarnings({"rawtypes","unchecked"})
    private void bukaEditPerkuliahan(final Long pId, final Div card, final boolean showKelas) throws Exception {
        if (pId == null) return;
        final Session s = HibernateUtil.currentSession();
        final Perkuliahan p = (Perkuliahan) s.get(Perkuliahan.class, pId);
        if (p == null) return;
        if (!bolehMengubahJadwal(p, true)) return;

        final MyWindow w = new MyWindow("Edit Perkuliahan", "normal", true);
        w.setParent(getPage().getFirstRoot());
        w.setWidth("430px");
        w.setContentStyle("padding:12px;");

        Div body = new Div();
        body.setStyle("display:flex;flex-direction:column;gap:8px;font-size:12px;");
        body.setParent(w);

        Label judul = new Label(p.getMatakuliah() != null ? p.getMatakuliah().getNama() : "-");
        judul.setStyle("font-weight:600;");
        judul.setParent(body);

        Div rKelas = new Div(); rKelas.setStyle("display:flex;align-items:center;gap:6px;"); rKelas.setParent(body);
        new Label(ais.common.Common.getBahasaConfig("Kelas:")).setParent(rKelas);
        final Textbox tKelas = new Textbox(p.getKelas() == null ? "" : p.getKelas());
        tKelas.setWidth("140px"); tKelas.setParent(rKelas);

        Div rDosen = new Div(); rDosen.setStyle("display:flex;align-items:center;gap:6px;"); rDosen.setParent(body);
        new Label(ais.common.Common.getBahasaConfig("Dosen:")).setParent(rDosen);
        final Combobox cbDsn = new Combobox(); cbDsn.setReadonly(true); cbDsn.setWidth("260px");
        cbDsn.appendItem("= (kosong) =").setValue(null);
        List dl = s.createCriteria(Dosen.class).add(Restrictions.eq("aktif", Boolean.TRUE))
            .addOrder(Order.asc("nama")).setMaxResults(600).list();
        int sel = 0, idx = 1;
        for (Object o : dl) {
            Dosen d = (Dosen) o;
            cbDsn.appendItem(d.getNama()).setValue(d.getId());
            if (p.getDosen1() != null && d.getId().equals(p.getDosen1().getId())) sel = idx;
            idx++;
        }
        cbDsn.setSelectedIndex(sel);
        cbDsn.setParent(rDosen);

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
                // FIX akar masalah "SessionException: Session is closed!" (KE-7/KE-15): `s`
                // (final Session di atas) ditangkap saat dialog DIBANGUN (request AWAL), tapi
                // listener ini baru dieksekusi belakangan saat tombol Simpan diklik -- request
                // yang TERPISAH. HibernateUtil.currentSession() bersifat per-request (dikelola
                // ZK), jadi `s` yang ditangkap itu sudah ditutup di request awal berakhir.
                // Ambil session yang SEDANG AKTIF untuk request klik ini, bukan yang ditangkap.
                Session sSimpan = HibernateUtil.currentSession();
                Perkuliahan target = (Perkuliahan) sSimpan.get(Perkuliahan.class, pId);
                if (target == null || !bolehMengubahJadwal(target, true)) return;
                target.setKelas(tKelas.getValue());
                Long did = (cbDsn.getSelectedItem() != null) ? (Long) cbDsn.getSelectedItem().getValue() : null;
                target.setDosen1(did == null ? null : (Dosen) sSimpan.get(Dosen.class, did));
                sSimpan.saveOrUpdate(target);
                refreshCard(card, target, showKelas);
                w.detach();
            }
        });
        btnSimpan.setParent(bar);

        w.setVisible(true);
        w.onModal();
    }

    /** Mengganti kartu lama dengan kartu segar yang mencerminkan perubahan (nama/dosen/kelas). */
    private void refreshCard(Div oldCard, Perkuliahan p, boolean showKelas) {
        Component parent = oldCard.getParent();
        Div baru = mkCard(p, showKelas);
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
                if (d.getAttribute("pId") != null || d.getAttribute("mkId") != null) kandidat.add(d);
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
            rowc.setSclass("aisttpchooserrow");
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
            if (c instanceof Label && "aisttphint".equals(((Label) c).getSclass())) {
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
        l.setSclass("aisttphint");
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
        s.setSclass("aisttpsep");
        s.setParent(bar);
    }
}
