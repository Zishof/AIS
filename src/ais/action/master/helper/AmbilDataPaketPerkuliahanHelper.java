package ais.action.master.helper;

import ais.common.CommonSearchFilterHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.South;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.PaketPerkuliahan;
import ais.database.model.PembagianKuotaPerkuliahanBerdasarkantahunAngkatan;
import ais.database.model.Perkuliahan;
import ais.database.model.Program;
import ais.database.model.Tbmuser;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataPaketPerkuliahanHelper {

    private String tahunAjaran;
    private Integer semester;
    private Mahasiswa mahasiswa;

    // Filter combos — not shown in UI, values used in onSearchDefault()
    private Combobox searchfakultas = new Combobox();
    private Combobox jurusanCombobox = new Combobox();
    private Combobox programCombobox = new Combobox();
    private Combobox semesterPerkuliahan;

    private List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs;
    private Integer semesterPendek;
    private PaketPerkuliahan paketPerkuliahan;
    private MyToolbarbuttonConfig ambil;
    private String kelas;
    private Boolean reload = false;

    private Map<Long, Perkuliahan> mapPerkuliahan = new HashMap<Long, Perkuliahan>();

    // UI state for card layout
    private Div cardContainer;
    private Label totalSksLabel;
    private Label paketNameLabel;
    private Label mkCountLabel;

    private static final String[] CARD_COLORS = {
        "#3B82F6", "#10B981", "#F59E0B", "#8B5CF6",
        "#EC4899", "#EF4444", "#0EA5E9", "#14B8A6"
    };

    public AmbilDataPaketPerkuliahanHelper(Integer semesterPendek) {
        this.semesterPendek = semesterPendek;
    }

    // ── CSS injection ────────────────────────────────────────────────────────

    private void injectCardStyles() {
        String css =
            ".krs-paket-info-header{background:linear-gradient(135deg,#1a3a5c 0%,#2d6a9f 100%);" +
            "color:#fff;padding:12px 18px;display:flex;flex-wrap:wrap;gap:8px;align-items:center;min-height:58px;}" +
            ".krs-info-name{font-weight:700;font-size:1.05em;display:block;}" +
            ".krs-info-nim{font-size:0.78em;color:rgba(255,255,255,0.82);display:block;margin-top:1px;}" +
            ".krs-info-chips{display:flex;flex-wrap:wrap;gap:5px;align-items:center;margin-left:auto;}" +
            ".krs-info-chip{background:rgba(255,255,255,0.18);border-radius:20px;" +
            "padding:3px 10px;font-size:0.8em;white-space:nowrap;}" +
            ".krs-info-chip-hl{background:rgba(255,255,255,0.35);border-radius:20px;" +
            "padding:3px 12px;font-size:0.8em;font-weight:700;white-space:nowrap;}" +
            ".krs-paket-summary{display:flex;align-items:center;flex-wrap:wrap;gap:8px;" +
            "padding:7px 18px;background:#EFF6FF;border-bottom:1px solid #BFDBFE;}" +
            ".krs-summary-label{font-size:0.8em;color:#1e40af;}" +
            ".krs-summary-paket-name{font-weight:700;color:#1D4ED8;font-size:0.88em;flex:1;min-width:0;}" +
            ".krs-total-sks-badge{background:#1D4ED8;color:#fff;border-radius:20px;" +
            "padding:2px 12px;font-size:0.8em;font-weight:700;white-space:nowrap;}" +
            ".krs-mk-count{color:#475569;font-size:0.8em;}" +
            ".krs-paket-cards{display:grid;" +
            "grid-template-columns:repeat(auto-fill,minmax(310px,1fr));gap:13px;padding:14px;}" +
            ".krs-mk-card{background:#fff;border-radius:10px;" +
            "box-shadow:0 2px 8px rgba(0,0,0,0.08);border-left:4px solid #3B82F6;" +
            "overflow:hidden;transition:box-shadow 0.2s,transform 0.15s;}" +
            ".krs-mk-card:hover{box-shadow:0 6px 20px rgba(0,0,0,0.14);transform:translateY(-1px);}" +
            ".krs-mk-card-head{padding:11px 13px 7px;display:flex;" +
            "align-items:flex-start;gap:7px;flex-wrap:wrap;}" +
            ".krs-mk-code{background:#EEF2FF;color:#4338CA;border-radius:6px;" +
            "padding:2px 8px;font-size:0.74em;font-weight:700;font-family:monospace;" +
            "white-space:nowrap;flex-shrink:0;}" +
            ".krs-mk-name{font-weight:700;font-size:0.9em;color:#1e293b;flex:1;line-height:1.35;}" +
            ".krs-sks-badge{background:linear-gradient(135deg,#7C3AED,#A855F7);" +
            "color:#fff;border-radius:20px;padding:2px 10px;" +
            "font-size:0.74em;font-weight:700;white-space:nowrap;flex-shrink:0;}" +
            ".krs-mk-jadwal{padding:5px 13px 10px;border-top:1px solid #f1f5f9;" +
            "background:#fafbfc;font-size:0.83em;color:#475569;}" +
            ".krs-no-jadwal{display:inline-flex;align-items:center;gap:5px;" +
            "background:#FEF9C3;color:#854D0E;border-radius:6px;" +
            "padding:3px 10px;font-size:0.81em;font-weight:600;}" +
            ".krs-empty-state{display:flex;flex-direction:column;align-items:center;" +
            "justify-content:center;padding:60px 20px;color:#94a3b8;text-align:center;}" +
            ".krs-empty-icon{font-size:3em;margin-bottom:12px;}" +
            ".krs-empty-title{font-weight:700;font-size:1.05em;color:#64748b;margin-bottom:6px;}" +
            ".krs-empty-desc{font-size:0.88em;max-width:320px;line-height:1.5;}" +
            ".krs-paket-footer{padding:10px 18px;background:#f8fafc;" +
            "border-top:2px solid #e2e8f0;display:flex;" +
            "justify-content:flex-end;gap:8px;align-items:center;}" +
            ".krs-footer-info{font-size:0.8em;color:#94a3b8;flex:1;}" +
            "@media(max-width:600px){" +
            ".krs-paket-cards{grid-template-columns:1fr;padding:8px;}" +
            ".krs-info-chips{margin-left:0;}" +
            "}";

        String js = "(function(){var id='krs-paket-css';" +
            "if(document.getElementById(id))return;" +
            "var s=document.createElement('style');s.id=id;" +
            "s.textContent='" + css + "';" +
            "document.head.appendChild(s);})();";
        Clients.evalJavaScript(js);
    }

    // ── HTML helpers ─────────────────────────────────────────────────────────

    private static String esc(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static void addChip(Div parent, String text, boolean highlight) {
        if (text == null || text.trim().isEmpty()) return;
        String cls = highlight ? "krs-info-chip-hl" : "krs-info-chip";
        org.zkoss.zul.Html chip = new org.zkoss.zul.Html(
            "<span class='" + cls + "'>" + esc(text) + "</span>");
        chip.setParent(parent);
    }

    // ── Card builder ─────────────────────────────────────────────────────────

    private Div buildMkCard(KurikulumPunyaMatakuliah kpm, String accentColor) {
        final Matakuliah matakuliah = kpm.getMatakuliah();

        Div card = new Div();
        card.setSclass("krs-mk-card");
        card.setStyle("border-left-color:" + accentColor + ";");

        // Header row: code chip + name + sks badge
        Div head = new Div();
        head.setSclass("krs-mk-card-head");
        head.setParent(card);

        String kode = (matakuliah != null && matakuliah.getKode() != null) ? matakuliah.getKode() : "-";
        new org.zkoss.zul.Html("<span class='krs-mk-code'>" + esc(kode) + "</span>").setParent(head);

        String nama = (matakuliah != null && matakuliah.getNama() != null) ? matakuliah.getNama() : "-";
        Label nameLabel = new Label(nama);
        nameLabel.setSclass("krs-mk-name");
        nameLabel.setParent(head);

        int sks = (matakuliah != null && matakuliah.getSks() != null) ? matakuliah.getSks() : 0;
        new org.zkoss.zul.Html("<span class='krs-sks-badge'>" + sks + " SKS</span>").setParent(head);

        // Jadwal section
        Div jadwalDiv = new Div();
        jadwalDiv.setSclass("krs-mk-jadwal");
        jadwalDiv.setParent(card);

        if (matakuliah == null) {
            new org.zkoss.zul.Html("<span class='krs-no-jadwal'>&#9888; Data matakuliah tidak lengkap</span>")
                .setParent(jadwalDiv);
            return card;
        }

        Session session = null;
        List<Perkuliahan> perkuliahans = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            perkuliahans = ConstantValues.simpleList(
                session.createCriteria(Perkuliahan.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.or(Restrictions.isNull("tampilkanSaatPengambilanKrs"),
                        Restrictions.eq("tampilkanSaatPengambilanKrs", true)))
                    .add(Restrictions.eq("matakuliah.id", matakuliah.getId()))
                    .add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
                        : Restrictions.eq("statusSemesterPendek", semesterPendek))
                    .add(Restrictions.eq("jurusan.id", mahasiswa.getJurusan().getId()))
                    .add(Restrictions.eq("program", mahasiswa.getProgram()))
                    .add(Restrictions.ilike("kelas", kelas, MatchMode.EXACT))
                    .add(Restrictions.eq("semester", semester))
                    .add(Restrictions.eq("tahunAjaran", tahunAjaran))
                    .add(Restrictions.or(Restrictions.eq("merupakan_paralel", false),
                        Restrictions.isNull("merupakan_paralel"))),
                Perkuliahan.class);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        } finally {
            if (session != null) {
                try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:220");}
                try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:221");}
                try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:222");}
            }
        }

        if (perkuliahans == null || perkuliahans.isEmpty()) {
            new org.zkoss.zul.Html("<span class='krs-no-jadwal'>&#9432; Jadwal belum tersedia</span>")
                .setParent(jadwalDiv);
        } else {
            Vbox vbox = new Vbox();
            vbox.setStyle("gap:4px;");
            for (Perkuliahan perkuliahan : perkuliahans) {
                mapPerkuliahan.put(matakuliah.getId(), perkuliahan);
                Hbox hbox = new Hbox();
                hbox.setStyle("flex-wrap:wrap;gap:6px;align-items:center;margin-bottom:3px;");
                PerkuliahanUIHelper.displayDosenPerkuliahanUmum(hbox, perkuliahan, true);
                PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(hbox, perkuliahan);
                hbox.setParent(vbox);
            }
            vbox.setParent(jadwalDiv);
        }

        return card;
    }

    private static void buildEmptyState(Div container, String title, String desc) {
        Common.clear(container);
        Div empty = new Div();
        empty.setSclass("krs-empty-state");
        empty.setParent(container);
        new org.zkoss.zul.Html("<div class='krs-empty-icon'>&#128218;</div>").setParent(empty);
        new org.zkoss.zul.Html("<div class='krs-empty-title'>" + esc(title) + "</div>").setParent(empty);
        new org.zkoss.zul.Html("<div class='krs-empty-desc'>" + esc(desc) + "</div>").setParent(empty);
    }

    // ── Business logic (unchanged) ───────────────────────────────────────────

    public void getSelectedPerkuliahansCekJikaBelumAda() {
        if (Common.bolehKonfigurasi("untuk_pengambilan_krs_paket_jika_jadwal_belum_dibuat_otomatis_membuat_jadwal_dengan_waktu_ruang_dosen_yang_kosong", Konfigurasi.TIDAK_AKTIF)) {

            Session mySession = null;
            boolean localTransaction = false;

            try {
                mySession = HibernateUtil.getSessionFactory().openSession();
                if (!mySession.getTransaction().isActive()) {
                    mySession.beginTransaction();
                    localTransaction = true;
                }

                for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
                    if (kurikulumPunyaMatakuliah != null && kurikulumPunyaMatakuliah.getMatakuliah() != null) {
                        boolean ada = mapPerkuliahan.containsKey(kurikulumPunyaMatakuliah.getMatakuliah().getId());
                        if (!ada) {
                            Perkuliahan perkuliahan = new Perkuliahan();
                            perkuliahan.setMatakuliah(kurikulumPunyaMatakuliah.getMatakuliah());
                            perkuliahan.setKurikulum(kurikulumPunyaMatakuliah.getKurikulum());
                            perkuliahan.setJurusan(mahasiswa.getJurusan());
                            perkuliahan.setProgram(mahasiswa.getProgram());
                            perkuliahan.setKelas(kelas);
                            perkuliahan.setSemester(semester);
                            perkuliahan.setTahunAjaran(tahunAjaran);
                            perkuliahan.setMerupakan_paralel(false);
                            perkuliahan.setMerupakan_tanpa_dosen(true);
                            perkuliahan.setMerupakan_tanpa_jadwal_perkuliahan(true);
                            perkuliahan.setMerupakan_tanpa_ruangan(true);
                            perkuliahan.populateKurikulumPunyaMatakuliah();
                            Common.refreshSaveOrUpdate(mySession, perkuliahan);
                        }
                    }
                }

                if (localTransaction) {
                    mySession.getTransaction().commit();
                }
            } catch (Exception e) {
                if (localTransaction && mySession != null && mySession.getTransaction().isActive()) {
                    try { mySession.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:298");}
                }
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:300");
            } finally {
                if (mySession != null) {
                    try { if (mySession.isOpen()) mySession.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:303");}
                    try { mySession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:304");}
                    try { if (mySession.isOpen()) mySession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:305");}
                }
            }
        }
    }

    @SuppressWarnings({})
    public boolean save() throws Exception {
        Tbmuser tbmuser = Common.getCurrentUser();
        getSelectedPerkuliahansCekJikaBelumAda();

        StringBuilder peringatanKapasitasRuangan = new StringBuilder();
        Session mySession = null;
        boolean localTransaction = false;

        try {
            mySession = HibernateUtil.getSessionFactory().openSession();
            if (!mySession.getTransaction().isActive()) {
                mySession.beginTransaction();
                localTransaction = true;
            }

            for (Perkuliahan perkuliahan : mapPerkuliahan.values()) {
                if (!Common.checkMatakuliahPrasyarat(perkuliahan.getMatakuliah(), mahasiswa, semester)) {
                    continue;
                }

                int detailperkuliahanCount = KrsUtilHelper.ambilJumlahDetailperkuliahan(mySession, perkuliahan, mahasiswa, reload);

                if (detailperkuliahanCount == 0) {
                    Integer jumlahUdahMasuk = KrsUtilHelper.ambilJumlahDetailperkuliahan(mySession, perkuliahan, false);
                    Integer kapasitasKelas = perkuliahan.getKapasitasKelas();

                    PembagianKuotaPerkuliahanBerdasarkantahunAngkatan pembagian = KrsUtilHelper
                            .ambilPembagianKuotaPerkuliahanBerdasarkantahunAngkatan(mySession, perkuliahan, mahasiswa.getTahunangkatan(), reload);

                    Number kuota = pembagian == null ? null : pembagian.getKuota();
                    if (kuota != null) {
                        kapasitasKelas = kuota.intValue();
                    }

                    jumlahUdahMasuk++;

                    if (jumlahUdahMasuk > kapasitasKelas) {
                        peringatanKapasitasRuangan.append("Kapasitas perkuliahan ")
                                .append(perkuliahan.info())
                                .append(" sudah penuh. Maksimal: ").append(kapasitasKelas)
                                .append(". Pilihlah jadwal perkuliahan lainnya.\n");
                        continue;
                    }

                    Detailperkuliahan detailperkuliahan = new Detailperkuliahan(tbmuser, AmbilDataPaketPerkuliahanHelper.class);
                    detailperkuliahan.setNilaiHuruf("");
                    detailperkuliahan.setTotalNilai(0.0);
                    detailperkuliahan.setMahasiswa(mahasiswa);
                    detailperkuliahan.setPerkuliahan(perkuliahan);
                    detailperkuliahan.setSemester(AmbilDataPaketPerkuliahanHelper.this.semester);
                    detailperkuliahan.setPersetujuan(Detailperkuliahan.DISETUJUI);

                    Common.refreshSaveOrUpdate(mySession, detailperkuliahan);
                }
            }

            if (localTransaction) {
                mySession.getTransaction().commit();
            }
        } catch (Exception e) {
            if (localTransaction && mySession != null && mySession.getTransaction().isActive()) {
                try { mySession.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:373");}
            }
            Common.tampilErrorJikaAdmin(e);
        } finally {
            if (mySession != null) {
                try { if (mySession.isOpen()) mySession.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:378");}
                try { mySession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:379");}
                try { if (mySession.isOpen()) mySession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:380");}
            }
        }

        if (peringatanKapasitasRuangan.length() > 0) {
            MyMessageboxConfig.show(peringatanKapasitasRuangan.toString(), "Peringatan",
                MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
        }

        return true;
    }

    // ── display() ────────────────────────────────────────────────────────────

    public void display(final Mahasiswa mahasiswa, final String tahunAjaran, final Integer semester,
            final DataLoader dataLoader) throws Exception {
        this.mahasiswa = mahasiswa;
        this.tahunAjaran = tahunAjaran;
        this.semester = semester;

        kelas = mahasiswa.getKelas();
        kelas = kelas == null ? "" : kelas.trim();

        // Init filter combos (not rendered, only read in onSearchDefault)
        initFilterCombos();

        injectCardStyles();

        final MyWindow window = new MyWindow();
        ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
        window.setTitle("Ambil Paket Perkuliahan");
        window.setWidth("98%");
        window.setHeight("97%");
        window.setBorder("none");

        Borderlayout bl = new ais.ui.util.MyBorderlayout();
        bl.setParent(window);

        // ── NORTH: Info mahasiswa + summary bar ──────────────────────────────
        North north = new North();
        north.setParent(bl);
        north.setBorder("none");
        north.setSize("118px");
        north.setCollapsible(false);
        north.setSplittable(false);

        Div northWrap = new Div();
        northWrap.setStyle("background:#fff;height:100%;overflow:hidden;");
        northWrap.setParent(north);

        // Info header
        Div infoHeader = new Div();
        infoHeader.setSclass("krs-paket-info-header");
        infoHeader.setParent(northWrap);

        // Avatar + name/nim
        org.zkoss.zul.Html avatar = new org.zkoss.zul.Html(
            "<span style='width:36px;height:36px;border-radius:50%;background:rgba(255,255,255,0.22);" +
            "display:inline-flex;align-items:center;justify-content:center;" +
            "font-size:1.2em;flex-shrink:0;'>&#128100;</span>");
        avatar.setParent(infoHeader);

        Vbox nameBox = new Vbox();
        nameBox.setStyle("min-width:0;margin-left:4px;");
        nameBox.setParent(infoHeader);

        new org.zkoss.zul.Html(
            "<span class='krs-info-name'>" + esc(mahasiswa.getNama()) + "</span>" +
            "<span class='krs-info-nim'>NIM: " + esc(mahasiswa.getNim()) + "</span>")
            .setParent(nameBox);

        // Info chips on the right
        Div chips = new Div();
        chips.setSclass("krs-info-chips");
        chips.setParent(infoHeader);

        if (mahasiswa.getJurusan() != null) {
            addChip(chips, mahasiswa.getJurusan().getNama(), false);
        }
        addChip(chips, mahasiswa.getProgram(), false);
        if (kelas != null && !kelas.isEmpty()) {
            addChip(chips, "Kelas " + kelas, false);
        }
        addChip(chips, "Semester " + semester, true);
        addChip(chips, tahunAjaran, false);

        // Summary bar: paket name + total SKS
        Div summaryBar = new Div();
        summaryBar.setSclass("krs-paket-summary");
        summaryBar.setParent(northWrap);

        new org.zkoss.zul.Html("<span class='krs-summary-label'>Paket:</span>").setParent(summaryBar);

        paketNameLabel = new Label(ais.common.Common.getBahasaConfig("Memuat..."));
        paketNameLabel.setSclass("krs-summary-paket-name");
        paketNameLabel.setParent(summaryBar);

        mkCountLabel = new Label("");
        mkCountLabel.setSclass("krs-mk-count");
        mkCountLabel.setParent(summaryBar);

        new org.zkoss.zul.Html("<span class='krs-summary-label'>Total SKS:</span>").setParent(summaryBar);

        totalSksLabel = new Label("0");
        totalSksLabel.setSclass("krs-total-sks-badge");
        totalSksLabel.setParent(summaryBar);

        // ── CENTER: Scrollable card container ────────────────────────────────
        Center center = new Center();
        center.setParent(bl);
        center.setAutoscroll(true);
        ais.ui.util.ZkCompat.setFlex(center, true);
        center.setBorder("none");

        cardContainer = new Div();
        cardContainer.setSclass("krs-paket-cards");
        cardContainer.setParent(center);

        // ── SOUTH: Action buttons ────────────────────────────────────────────
        South south = new South();
        south.setParent(bl);
        south.setBorder("none");
        south.setSize("58px");

        Div footer = new Div();
        footer.setSclass("krs-paket-footer");
        footer.setParent(south);

        new org.zkoss.zul.Html("<span class='krs-footer-info'>Pilih paket dan klik Ambil Paket untuk mendaftar.</span>")
            .setParent(footer);

        MyToolbarbuttonConfig batalBtn = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        batalBtn.setTooltiptext("Tutup tanpa mengambil paket");
        batalBtn.addEventListener("onClick", new EventListener() {
            public void onEvent(Event event) throws Exception {
                window.detach();
            }
        });
        batalBtn.setParent(footer);

        ambil = new MyToolbarbuttonConfig("Ambil Paket", "/img/save.gif");
        ambil.setTooltiptext("Daftarkan semua mata kuliah dalam paket ini");
        ambil.setDisabled(true);
        ambil.addEventListener("onClick", new EventListener() {
            public void onEvent(Event event) throws Exception {
                save();
                Common.createDefaultTimer(new EventListener() {
                    public void onEvent(Event arg0) throws Exception {
                        dataLoader.loadData(true);
                        window.detach();
                    }
                });
            }
        });
        ambil.setParent(footer);

        window.setVisible(true);
        try {
            window.onModal();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }

        onSearchDefault(null);
    }

    // ── Filter combo init (values used in query, not shown in UI) ────────────

    @SuppressWarnings("unchecked")
    private void initFilterCombos() throws Exception {
        Common.insertCombo(searchfakultas, new String[]{"nama", "kode"}, Fakultas.class,
            Restrictions.eq("aktif", true));
        Common.selectComboItem(searchfakultas, this.mahasiswa.getJurusan().getFakultas());

        Common.insertCombo(jurusanCombobox, "nama", Jurusan.class,
            Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
            Restrictions.eq("fakultas", this.mahasiswa.getJurusan().getFakultas()));
        Common.selectComboItem(jurusanCombobox, this.mahasiswa.getJurusan());

        Common.insertCombo(programCombobox, "namaBaru", Program.class,
            Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Program programselected = (Program) session.createCriteria(Program.class)
                .add(Restrictions.eq("nama", this.mahasiswa.getProgram())).uniqueResult();
            Common.selectComboItem(programCombobox, programselected);
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:569");
        } finally {
            if (session != null) {
                try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:572");}
                try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:573");}
                try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:574");}
            }
        }

        semesterPerkuliahan = new Combobox();
        int maxSemesterPilihan = 25;
        try {
            maxSemesterPilihan = Integer.parseInt(
                Common.getKonfigurasi("max_semester_pilihan", "25").getNilai().trim());
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:583");}

        for (int i = 1; i < maxSemesterPilihan; i++) {
            org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
            comboitem.setLabel(String.valueOf(i));
            comboitem.setValue(i);
            semesterPerkuliahan.appendChild(comboitem);
        }
        Common.selectComboItem(semesterPerkuliahan, this.semester);
    }

    // ── onSearchDefault ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public void onSearchDefault(Event event) throws Exception {
        Program program = (Program) (programCombobox.getSelectedItem() == null ? null
            : programCombobox.getSelectedItem().getValue());

        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();

            Integer smtValue = (Integer) semesterPerkuliahan.getSelectedItem().getValue();

            Criteria criteriaPaket = session.createCriteria(PaketPerkuliahan.class)
                .add(Restrictions.sqlRestriction(smtValue + " between minsmt and maxsmt"))
                .add(Restrictions.sqlRestriction(mahasiswa.getTahunangkatan() + " between mulai and sampai"))
                .add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
                    : Restrictions.eq("statusSemesterPendek", semesterPendek))
                .createAlias("kurikulum", "kurikulum", Criteria.LEFT_JOIN)
                .createAlias("kurikulum.program", "program", Criteria.LEFT_JOIN)
                .createAlias("kurikulum.jurusan", "jurusan", Criteria.LEFT_JOIN);

            if (searchfakultas.getSelectedItem() != null && searchfakultas.getSelectedItem().getValue() != null) {
                criteriaPaket.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false));
            }

            if (jurusanCombobox.getSelectedItem() != null) {
                criteriaPaket.add(CommonSearchFilterHelper.eqSelectedWithId("kurikulum.jurusan", jurusanCombobox, false));
            }

            if (program != null) {
                criteriaPaket.add(Restrictions.or(Restrictions.isNull("kurikulum.program"),
                    Restrictions.eq("program.nama", program.getNama())));
            } else if (programCombobox.getSelectedItem() != null) {
                criteriaPaket.add(Restrictions.or(Restrictions.isNull("kurikulum.program"),
                    Restrictions.eq("program.nama", programCombobox.getSelectedItem().getLabel())));
            }

            paketPerkuliahan = (PaketPerkuliahan) ConstantValues.simpleObject(
                criteriaPaket.add(Restrictions.eq("tahunAkademik", tahunAjaran))
                    .addOrder(Order.desc("angkatanMulai"))
                    .addOrder(Order.desc("angkatanSampai"))
                    .addOrder(Order.desc("id"))
                    .setMaxResults(1),
                PaketPerkuliahan.class);

            if (paketPerkuliahan == null) {
                buildEmptyState(cardContainer, "Paket tidak ditemukan",
                    "Tidak ada paket perkuliahan yang sesuai dengan data Anda untuk semester " + smtValue + ".");
                paketNameLabel.setValue("Tidak ditemukan");
                mkCountLabel.setValue("");
                totalSksLabel.setValue("0");
                ambil.setDisabled(true);
                return;
            }

            kurikulumPunyaMatakuliahs = session.createCriteria(KurikulumPunyaMatakuliah.class)
                .add(Restrictions.eq("kurikulum", paketPerkuliahan.getKurikulum()))
                .add(Restrictions.eq("semester", smtValue)).list();

            if (kurikulumPunyaMatakuliahs.isEmpty()) {
                buildEmptyState(cardContainer, "Belum ada matakuliah",
                    "Paket semester " + smtValue + " belum memiliki matakuliah terdaftar.");
                paketNameLabel.setValue(paketPerkuliahan.getNama() != null ? paketPerkuliahan.getNama() : "-");
                mkCountLabel.setValue("");
                totalSksLabel.setValue("0");
                ambil.setDisabled(true);
                return;
            }

            // Build cards
            Common.clear(cardContainer);
            mapPerkuliahan.clear();

            int totalSks = 0;
            int colorIdx = 0;
            for (KurikulumPunyaMatakuliah kpm : kurikulumPunyaMatakuliahs) {
                String color = CARD_COLORS[colorIdx % CARD_COLORS.length];
                colorIdx++;
                Div card = buildMkCard(kpm, color);
                card.setParent(cardContainer);
                Matakuliah mk = kpm.getMatakuliah();
                if (mk != null && mk.getSks() != null) {
                    totalSks += mk.getSks();
                }
            }

            String paketNama = paketPerkuliahan.getNama() != null ? paketPerkuliahan.getNama() : "Paket Perkuliahan";
            paketNameLabel.setValue(paketNama);
            mkCountLabel.setValue(kurikulumPunyaMatakuliahs.size() + " matakuliah");
            totalSksLabel.setValue(String.valueOf(totalSks));
            ambil.setDisabled(false);

        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:688");
        } finally {
            if (session != null) {
                try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:691");}
                try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:692");}
                try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPaketPerkuliahanHelper.java:693");}
            }
        }
    }
}
