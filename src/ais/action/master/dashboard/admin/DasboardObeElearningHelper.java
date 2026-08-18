package ais.action.master.dashboard.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.KonfigurasiManager;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.obe.CapaianLulusan;
import ais.database.model.obe.CapaianPembelajaranLulusan;
import ais.database.model.obe.ProfilLulusan;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Dasbor OBE di e-Learning — pemantauan lengkap Sub-CPMK→CPMK→CPL→PL
 * (Loop 1: CPMK per MK, Loop 2: CPL per prodi, Loop 3: PL coverage)
 * Mengikuti pola DasboardSPMI: extends Div, lazy load via timer, Hibernate langsung.
 */
public class DasboardObeElearningHelper extends Div {

    private static final long serialVersionUID = 1L;

    // ── filter state ────────────────────────────────────────────────────────
    private String filterTa       = Common.getCurrentTahunAkademik();
    private String filterSemester = "";
    private Long   filterJurusanId = null;
    private Long   filterFakultasId = null;

    // ── MK search filter ────────────────────────────────────────────────────
    private String filterMk = "";
    private org.zkoss.zul.Textbox txFilterMk;

    // ── Pagination (10 MK per halaman) ──────────────────────────────────────
    private static final int PAGE_SIZE = 10;
    private int pageRps  = 0;
    private int pageCpl  = 0;
    private int pageCpmk = 0;
    private Div divRpsContainer;
    private Div divCplContainer;
    private Div divCpmkContainer;
    private ObeData lastData;

    // ── L1 in-process cache (5 menit TTL) ───────────────────────────────────
    private static final ConcurrentHashMap<String, ObeData> DATA_CACHE   = new ConcurrentHashMap<String, ObeData>();
    private static final ConcurrentHashMap<String, Long>    CACHE_EXPIRY = new ConcurrentHashMap<String, Long>();
    private static final long CACHE_TTL_MS = 5L * 60 * 1000;

    // ── filter widgets ──────────────────────────────────────────────────────
    private Combobox cbTa;
    private Combobox cbSemester;
    private Combobox cbFakultas;
    private Combobox cbJurusan;

    // ── current user context ────────────────────────────────────────────────
    private final Tbmuser tbmuser;

    // ====================================================================
    // Data holder
    // ====================================================================

    private static class ObeData {
        // ── Loop 0: Kesiapan RPS ──────────────────────────────────────────
        int totalMk, sudahRps, belumRps, mkPunyaCpl;
        // Kolom: [0]kode [1]nama [2]namaProdi [3]namaDosen [4]sks(int) [5]sudahRps(bool)
        //        [6]tglRps [7]jmlCpl [8]jmlCpmk [9]cplBobot [10]jurusanId(Long)
        List<Object[]> rekapMk = new ArrayList<Object[]>();
        // mkKode → total Sub-CPMK (computed after Loop 1)
        Map<String, Integer> subCpmkCountByMk  = new HashMap<String, Integer>();
        // jurusanId → PL count (computed after Loop 3)
        Map<Long, Integer>   plCountByJurusan   = new HashMap<Long, Integer>();
        // CPL → jumlah MK yang membebankan CPL tersebut
        Map<String, Integer> cplCoverage = new LinkedHashMap<String, Integer>();
        // CPL → total bobot (%)
        Map<String, Double>  cplBobotSum = new LinkedHashMap<String, Double>();

        // ── Loop 0b: CQI (ACT) tracking ──────────────────────────────────
        int mkDenganCqi;
        int mkDenganCqiLengkap; // CQI with masalah + rencana both filled

        // ── Loop 1: Monitoring CPMK & Sub-CPMK ────────────────────────────
        int mkDenganCpmk, mkDenganSubCpmk, totalCpmkUnik, totalSubCpmkUnik;
        // mkKode → list CPMK: [0]kode [1]nama [2]bobot [3]minimal [4]subCount [5]mkNama
        Map<String, List<Object[]>> cpmkPerMk  = new LinkedHashMap<String, List<Object[]>>();
        // mkKode → mkNama (untuk display)
        Map<String, String> mkNamaByKode       = new LinkedHashMap<String, String>();
        // mkKode → prodiNama
        Map<String, String> mkProdiByKode      = new LinkedHashMap<String, String>();

        // ── Loop 2: Monitoring CPL per Prodi ─────────────────────────────
        // jurusanId → namaJurusan
        Map<Long, String> namaJurusan          = new LinkedHashMap<Long, String>();
        // jurusanId → list CPL: [0]kode [1]nama [2]mkCount [3]plKode [4]plNama [5]cpmkCount
        Map<Long, List<Object[]>> cplPerJurusan = new LinkedHashMap<Long, List<Object[]>>();
        // cplId → mkCount
        Map<Long, Integer> mkCountPerCplId     = new HashMap<Long, Integer>();

        // ── Loop 3: Monitoring Profil Lulusan ─────────────────────────────
        // jurusanId → list PL: [0]kode [1]nama [2]cplCount
        Map<Long, List<Object[]>> plPerJurusan  = new LinkedHashMap<Long, List<Object[]>>();
        // plId → kode (lookup)
        Map<Long, String> plKodeById            = new HashMap<Long, String>();

        // perkuliahan id → CQI status ("OK"=lengkap, "PARTIAL"=ada tapi belum lengkap, ""=belum)
        Map<Long, String> cqiStatusByPerkId = new HashMap<Long, String>();
    }

    // ====================================================================
    // Constructor
    // ====================================================================

    public DasboardObeElearningHelper() {
        this.tbmuser = Common.getCurrentUser();
        setWidth("100%");
        setStyle("min-height:300px; background:#f0f4ff; padding:12px 14px;"
               + " box-sizing:border-box; overflow:auto;");
        try {
            // Init role-scoped filter from user's access role
            Jurusan initRoleJur = getRoleJurusan();
            ais.database.model.Fakultas initRoleFak = getRoleFakultas();
            if (initRoleJur != null) filterJurusanId = initRoleJur.getId();
            else if (initRoleFak != null) filterFakultasId = initRoleFak.getId();
            tampilLoading();
            Common.createDefaultTimer(new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    renderAll();
                }
            });
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    // ====================================================================
    // Loading state
    // ====================================================================

    private void tampilLoading() {
        Common.clear(this);
        DashboardUiUtil.injectGlobalCss(this);
        appendHtml(this, DashboardUiUtil.loadingHtml("Memuat Dasbor OBE…"));
    }

    // ====================================================================
    // Main render
    // ====================================================================

    private void renderAll() {
        try {
            ObeData d = loadDataCached();
            lastData = d;
            pageRps = 0; pageCpl = 0; pageCpmk = 0;
            Common.clear(this);
            DashboardUiUtil.injectGlobalCss(this);
            renderHero(d);
            renderFilter();
            renderStatsCards(d);

            // Loop 0: Rekap RPS (lebar penuh), lalu Cakupan CPL di bawahnya
            renderRekapRps(this, d);
            renderCqiAnalysisSummary(this, d);
            renderCplHeatmap(this, d);

            renderCplRekapTable(this, d);  // Detail CPL bobot per MK

            // Charts: radar cakupan CPL + kartu visual ringkas (donut & batang), semua HTML/CSS
            if (!d.cplCoverage.isEmpty()) renderCplRadarChart(d);
            renderObeExtraCharts(d);

            // Loop 1: CPMK & Sub-CPMK monitoring
            renderCpmkStatsCards(d);
            renderCpmkPerMkTable(d);

            // Loop 2: CPL per Prodi
            renderCplPerProdi(d);

            // Loop 3: Profil Lulusan coverage
            renderProfilLulusanCoverage(d);

            // PIKOBE Readiness Score
            renderPikobeScore(d);

            // OBE Points 3+4: Admin analisis CPL & PL di level prodi/tahun akademik
            renderAnalisisProdi(d);

        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    // ====================================================================
    // Data query — Hibernate langsung, tanpa JSP
    // ====================================================================

    @SuppressWarnings({"unchecked", "deprecation"})
    private ObeData loadData() {
        ObeData d = new ObeData();
        Session sess = HibernateUtil.currentSession();

        // ── Query Perkuliahan OBE ────────────────────────────────────────
        Criteria crit = sess.createCriteria(Perkuliahan.class, "p")
            .add(Restrictions.isNotNull("p.kurikulumPunyaMatakuliah"))
            .createAlias("p.kurikulumPunyaMatakuliah", "kpm")
            .createAlias("kpm.kurikulum", "kur")
            .add(Restrictions.eq("kur.obe", Boolean.TRUE))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        if (filterTa != null && !filterTa.isEmpty())
            crit.add(Restrictions.eq("p.tahunAjaran", filterTa));
        if (filterSemester != null && !filterSemester.isEmpty())
            crit.add(Restrictions.eq("p.ganjilGenap", filterSemester));

        Dosen dosen = tbmuser != null ? tbmuser.ambilDosen() : null;
        if (dosen != null) {
            // Perkuliahan tidak memiliki properti tunggal "dosen"; pengampu
            // tersimpan pada slot dosen1..dosen10. Cocokkan dosen yang login
            // ke salah satu slot tersebut (OR), bukan ke properti "dosen".
            org.hibernate.criterion.Disjunction dosenOr = Restrictions.disjunction();
            dosenOr.add(Restrictions.eq("p.dosen1", dosen));
            dosenOr.add(Restrictions.eq("p.dosen2", dosen));
            dosenOr.add(Restrictions.eq("p.dosen3", dosen));
            dosenOr.add(Restrictions.eq("p.dosen4", dosen));
            dosenOr.add(Restrictions.eq("p.dosen5", dosen));
            dosenOr.add(Restrictions.eq("p.dosen6", dosen));
            dosenOr.add(Restrictions.eq("p.dosen7", dosen));
            dosenOr.add(Restrictions.eq("p.dosen8", dosen));
            dosenOr.add(Restrictions.eq("p.dosen9", dosen));
            dosenOr.add(Restrictions.eq("p.dosen10", dosen));
            crit.add(dosenOr);
        }

        if (dosen == null) {
            if (filterJurusanId != null) {
                crit.add(Restrictions.eq("p.jurusan.id", filterJurusanId));
            } else if (filterFakultasId != null) {
                crit.createAlias("p.jurusan", "jur")
                    .createAlias("jur.fakultas", "fak")
                    .add(Restrictions.eq("fak.id", filterFakultasId));
            }
        }

        List<Perkuliahan> perkuliahans = (List<Perkuliahan>) crit.list();

        // Filter mahasiswa
        Mahasiswa mhs = tbmuser != null ? tbmuser.getMahasiswa() : null;
        if (mhs != null) {
            List<Long> joinedIds = (List<Long>) sess.createCriteria(
                    ais.database.model.Detailperkuliahan.class, "dp")
                .add(Restrictions.eq("dp.mahasiswa.id", mhs.getId()))
                .add(Restrictions.eq("dp.persetujuan",
                        ais.database.model.Detailperkuliahan.DISETUJUI))
                .setProjection(Projections.property("dp.perkuliahan.id"))
                .list();
            Set<Long> joinedSet = new HashSet<Long>(joinedIds);
            List<Perkuliahan> filtered = new ArrayList<Perkuliahan>();
            for (Perkuliahan p : perkuliahans)
                if (joinedSet.contains(p.getId())) filtered.add(p);
            perkuliahans = filtered;
        }

        d.totalMk = perkuliahans.size();

        // ── Loop 0: Rekap RPS + CPL coverage ──────────────────────────────
        Set<Long> allCpmkIds   = new HashSet<Long>();
        Set<Long> allJurusanIds = new HashSet<Long>();
        Map<String, String> cpmkCsvByMkKode = new LinkedHashMap<String, String>();

        for (Perkuliahan p : perkuliahans) {
            try {
                KurikulumPunyaMatakuliah kpm = p.getKurikulumPunyaMatakuliah();
                Matakuliah mk = p.getMatakuliah();
                if (kpm == null || mk == null) continue;

                boolean sudahRps = kpm.getRincian() != null && !kpm.getRincian().trim().isEmpty()
                                   && kpm.getTanggalPenyusunan() != null;
                boolean punyaCpl = mk.getCapaianLulusan() != null
                                   && !mk.getCapaianLulusan().trim().isEmpty();

                if (sudahRps) d.sudahRps++; else d.belumRps++;
                if (punyaCpl) d.mkPunyaCpl++;
                String cqiDataStr = p.getCqiData();
                if (cqiDataStr != null && !cqiDataStr.trim().isEmpty()) {
                    d.mkDenganCqi++;
                    try {
                        org.json.JSONArray cqiArr = new org.json.JSONArray(cqiDataStr);
                        for (int ci = 0; ci < cqiArr.length(); ci++) {
                            org.json.JSONObject ce = cqiArr.getJSONObject(ci);
                            if (!ce.optString("masalah").trim().isEmpty()
                                    && !ce.optString("rencana").trim().isEmpty()) {
                                d.mkDenganCqiLengkap++;
                                break;
                            }
                        }
                    } catch (Exception cqiEx) { ais.common.ErrorAuditUtil.record(cqiEx, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:328"); /* skip malformed JSON */ }
                }

                // Track CQI status per perkuliahan for rekap badge
                // Values: "ADMIN_OK"=disetujui, "ADMIN_REVISI"=revisi, "OK"=dosen isi, "PARTIAL"=belum, ""=kosong
                if (cqiDataStr != null && !cqiDataStr.trim().isEmpty()) {
                    boolean cqiLengkap = false;
                    String admGlSt = "";
                    try {
                        org.json.JSONArray cqiArr2 = new org.json.JSONArray(cqiDataStr);
                        for (int ci = 0; ci < cqiArr2.length(); ci++) {
                            org.json.JSONObject ce = cqiArr2.getJSONObject(ci);
                            if ("__meta__".equals(ce.optString("cpmk",""))) {
                                admGlSt = ce.optString("adminGlobalStatus","");
                            } else if (!ce.optString("masalah").trim().isEmpty()
                                    && !ce.optString("rencana").trim().isEmpty()) {
                                cqiLengkap = true;
                            }
                        }
                    } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:347");}
                    String cqiSt;
                    if ("Disetujui".equals(admGlSt)) cqiSt = "ADMIN_OK";
                    else if ("Revisi".equals(admGlSt)) cqiSt = "ADMIN_REVISI";
                    else cqiSt = cqiLengkap ? "OK" : "PARTIAL";
                    d.cqiStatusByPerkId.put(p.getId(), cqiSt);
                } else {
                    d.cqiStatusByPerkId.put(p.getId(), "");
                }

                int jmlCpl = 0, jmlCpmk = 0;
                if (punyaCpl) jmlCpl = mk.getCapaianLulusan().split(",").length;
                if (mk.getCapaianPembelajaranLulusan() != null
                        && !mk.getCapaianPembelajaranLulusan().trim().isEmpty())
                    jmlCpmk = mk.getCapaianPembelajaranLulusan().split(",").length;

                String namaProdi  = p.getJurusan() != null ? nvl(p.getJurusan().getNama()) : "";
                String namaDosen  = p.getDosen1()  != null ? nvl(p.getDosen1().getNama())  : "";
                String tglRps     = sudahRps && kpm.getTanggalPenyusunan() != null
                                    ? Common.dateFormat6.get().format(kpm.getTanggalPenyusunan()) : "-";
                String cplBobotStr = kpm.getCplBobot() != null ? kpm.getCplBobot() : "";
                int sks = mk.getSks() != null ? mk.getSks() : 0;
                String mkKode = nvl(mk.getKode());
                String mkNama = nvl(mk.getNama());

                d.rekapMk.add(new Object[]{
                    mkKode, mkNama, namaProdi, namaDosen,
                    sks, sudahRps, tglRps, jmlCpl, jmlCpmk, cplBobotStr,
                    p.getJurusan() != null ? p.getJurusan().getId() : null,  // [10] jurusanId
                    kpm.getId(),                                             // [11] kurikulumPunyaMatakuliah id (untuk cetak RPS)
                    p.getId()                                                // [12] perkuliahan id (untuk tab Nilai OBE & Rekap Nilai OBE)
                });

                d.mkNamaByKode.put(mkKode, mkNama);
                d.mkProdiByKode.put(mkKode, namaProdi);

                // CPL coverage dari cplBobot
                if (!cplBobotStr.isEmpty()) {
                    for (String cp : cplBobotStr.split(",")) {
                        cp = cp.trim();
                        String cplKey = cp.contains(":")
                                        ? cp.split(":", 2)[0].trim().toUpperCase()
                                        : cp.toUpperCase();
                        if (cplKey.isEmpty()) continue;
                        d.cplCoverage.put(cplKey,
                            (d.cplCoverage.containsKey(cplKey) ? d.cplCoverage.get(cplKey) : 0) + 1);
                        if (cp.contains(":")) {
                            double bv = 0;
                            try { bv = Double.parseDouble(cp.split(":", 2)[1].trim()); }
                            catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:396");}
                            d.cplBobotSum.put(cplKey,
                                (d.cplBobotSum.containsKey(cplKey) ? d.cplBobotSum.get(cplKey) : 0.0) + bv);
                        }
                    }
                } else if (punyaCpl) {
                    for (String cplId : mk.getCapaianLulusan().split(",")) {
                        String cplKey = cplId.trim().toUpperCase();
                        if (cplKey.isEmpty()) continue;
                        d.cplCoverage.put(cplKey,
                            (d.cplCoverage.containsKey(cplKey) ? d.cplCoverage.get(cplKey) : 0) + 1);
                    }
                }

                // Collect CPMK IDs for Loop 1
                String cpmkCsv = mk.getCapaianPembelajaranLulusan();
                if (cpmkCsv != null && !cpmkCsv.trim().isEmpty() && !mkKode.isEmpty()) {
                    cpmkCsvByMkKode.put(mkKode, cpmkCsv);
                    for (String idStr : cpmkCsv.split(",")) {
                        if (idStr.trim().isEmpty()) continue;
                        try { allCpmkIds.add(Long.parseLong(idStr.trim())); }
                        catch (NumberFormatException ignored) { /* token bukan angka, lewati */ }
                    }
                }

                // Collect CPL IDs for Loop 2 (MK-level CPL count)
                if (punyaCpl) {
                    for (String idStr : mk.getCapaianLulusan().split(",")) {
                        if (idStr.trim().isEmpty()) continue;
                        try {
                            Long cplId = Long.parseLong(idStr.trim());
                            d.mkCountPerCplId.put(cplId,
                                (d.mkCountPerCplId.containsKey(cplId) ? d.mkCountPerCplId.get(cplId) : 0) + 1);
                        } catch (NumberFormatException ignored) { /* token bukan angka, lewati */ }
                    }
                }

                // Collect jurusan IDs for Loop 2 & 3
                if (p.getJurusan() != null) {
                    allJurusanIds.add(p.getJurusan().getId());
                    d.namaJurusan.put(p.getJurusan().getId(), nvl(p.getJurusan().getNama()));
                }

            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:437");}
        }

        // ── Loop 1: Query CPMK entities & Sub-CPMK counts ─────────────────
        if (!allCpmkIds.isEmpty()) {
            Map<Long, CapaianPembelajaranLulusan> cpmkMap = new HashMap<Long, CapaianPembelajaranLulusan>();
            List<CapaianPembelajaranLulusan> cpmkList = (List<CapaianPembelajaranLulusan>)
                sess.createCriteria(CapaianPembelajaranLulusan.class)
                    .add(Restrictions.in("id", allCpmkIds))
                    .addOrder(Order.asc("kode"))
                    .list();
            for (CapaianPembelajaranLulusan c : cpmkList) cpmkMap.put(c.getId(), c);
            d.totalCpmkUnik = cpmkMap.size();

            Set<String> subCpmkKeysGlobal = new HashSet<String>(); // deduplicate sub-CPMK globally

            for (Map.Entry<String, String> entry : cpmkCsvByMkKode.entrySet()) {
                String mkKode = entry.getKey();
                List<Object[]> cpmkRows = new ArrayList<Object[]>();
                boolean mkHasSubCpmk = false;

                for (String idStr : entry.getValue().split(",")) {
                    try {
                        Long id = Long.parseLong(idStr.trim());
                        CapaianPembelajaranLulusan c = cpmkMap.get(id);
                        if (c == null) continue;

                        int subCount = 0;
                        if (c.getFormula() != null && !c.getFormula().trim().isEmpty()) {
                            try {
                                JSONArray arr = new JSONArray(c.getFormula());
                                subCount = arr.length();
                                // Track unique sub-CPMK keys
                                for (int i = 0; i < arr.length(); i++) {
                                    try {
                                        JSONObject sub = arr.getJSONObject(i);
                                        long key = ais.common.JsonCompatUtil.optLong(sub, "key", 0L);
                                        if (key > 0) subCpmkKeysGlobal.add(id + ":" + key);
                                    } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:475");}
                                }
                            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:477");}
                        }

                        if (subCount > 0) mkHasSubCpmk = true;

                        cpmkRows.add(new Object[]{
                            nvl(c.getKode()),                        // 0: kode
                            nvl(c.getNama()),                        // 1: nama
                            c.getBobot()   != null ? c.getBobot()   : 0.0, // 2: bobot
                            c.getMinimal() != null ? c.getMinimal() : 0.0, // 3: minimal
                            subCount                                 // 4: subCpmkCount
                        });
                    } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:489");}
                }

                if (!cpmkRows.isEmpty()) {
                    d.cpmkPerMk.put(mkKode, cpmkRows);
                    d.mkDenganCpmk++;
                    if (mkHasSubCpmk) d.mkDenganSubCpmk++;
                }
            }
            d.totalSubCpmkUnik = subCpmkKeysGlobal.size();

            // Compute total Sub-CPMK per MK for rekap table
            for (Map.Entry<String, List<Object[]>> e : d.cpmkPerMk.entrySet()) {
                int total = 0;
                for (Object[] row : e.getValue()) total += (Integer) row[4];
                d.subCpmkCountByMk.put(e.getKey(), total);
            }
        }

        // ── Loop 2 & 3: Query CPL dan PL per Jurusan ──────────────────────
        if (!allJurusanIds.isEmpty()) {
            // Query CPL per jurusan
            List<CapaianLulusan> allCpls = (List<CapaianLulusan>)
                sess.createCriteria(CapaianLulusan.class)
                    .add(Restrictions.in("jurusan.id", allJurusanIds))
                    .addOrder(Order.asc("kode"))
                    .list();

            // Collect all PL IDs from CPL.getProfil()
            Set<Long> allPlIds = new HashSet<Long>();
            Map<Long, CapaianLulusan> cplById = new HashMap<Long, CapaianLulusan>();
            for (CapaianLulusan cpl : allCpls) {
                cplById.put(cpl.getId(), cpl);
                if (cpl.getProfil() != null && !cpl.getProfil().trim().isEmpty()) {
                    for (String idStr : cpl.getProfil().split(",")) {
                        try { allPlIds.add(Long.parseLong(idStr.trim())); }
                        catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:525");}
                    }
                }
            }

            // Query PL entities
            Map<Long, ProfilLulusan> plById = new HashMap<Long, ProfilLulusan>();
            if (!allPlIds.isEmpty()) {
                List<ProfilLulusan> plList = (List<ProfilLulusan>)
                    sess.createCriteria(ProfilLulusan.class)
                        .add(Restrictions.in("id", allPlIds))
                        .addOrder(Order.asc("kode"))
                        .list();
                for (ProfilLulusan pl : plList) {
                    plById.put(pl.getId(), pl);
                    d.plKodeById.put(pl.getId(), nvl(pl.getKode()));
                }
            }

            // Build cplPerJurusan
            for (CapaianLulusan cpl : allCpls) {
                if (cpl.getJurusan() == null) continue;
                Long jid = cpl.getJurusan().getId();

                // Count CPMK linked to this CPL
                int cpmkCount = 0;
                if (cpl.getCapaianPembelajaranLulusan() != null
                        && !cpl.getCapaianPembelajaranLulusan().trim().isEmpty()) {
                    cpmkCount = cpl.getCapaianPembelajaranLulusan().split(",").length;
                }

                // Resolve PL kode from CSV
                String plKode = "", plNama = "";
                if (cpl.getProfil() != null && !cpl.getProfil().trim().isEmpty()) {
                    StringBuilder plKodeSb = new StringBuilder();
                    StringBuilder plNamaSb = new StringBuilder();
                    for (String idStr : cpl.getProfil().split(",")) {
                        try {
                            Long plId = Long.parseLong(idStr.trim());
                            ProfilLulusan pl = plById.get(plId);
                            if (pl != null) {
                                if (plKodeSb.length() > 0) { plKodeSb.append(", "); plNamaSb.append(", "); }
                                plKodeSb.append(nvl(pl.getKode()));
                                plNamaSb.append(nvl(pl.getNama()));
                            }
                        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:570");}
                    }
                    plKode = plKodeSb.toString();
                    plNama = plNamaSb.toString();
                }

                int mkCount = d.mkCountPerCplId.containsKey(cpl.getId())
                              ? d.mkCountPerCplId.get(cpl.getId()) : 0;

                if (!d.cplPerJurusan.containsKey(jid))
                    d.cplPerJurusan.put(jid, new ArrayList<Object[]>());
                d.cplPerJurusan.get(jid).add(new Object[]{
                    nvl(cpl.getKode()),  // 0: kode CPL
                    nvl(cpl.getNama()),  // 1: nama CPL
                    mkCount,             // 2: jumlah MK berkontribusi
                    plKode,             // 3: kode PL terkait
                    plNama,             // 4: nama PL terkait
                    cpmkCount           // 5: jumlah CPMK
                });
            }

            // Build plPerJurusan (PL coverage per jurusan)
            // Count CPL per PL
            Map<Long, Integer> cplCountPerPlId = new HashMap<Long, Integer>();
            for (CapaianLulusan cpl : allCpls) {
                if (cpl.getProfil() == null || cpl.getProfil().trim().isEmpty()) continue;
                for (String idStr : cpl.getProfil().split(",")) {
                    try {
                        Long plId = Long.parseLong(idStr.trim());
                        cplCountPerPlId.put(plId,
                            (cplCountPerPlId.containsKey(plId) ? cplCountPerPlId.get(plId) : 0) + 1);
                    } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:601");}
                }
            }
            for (Map.Entry<Long, ProfilLulusan> entry : plById.entrySet()) {
                ProfilLulusan pl = entry.getValue();
                if (pl.getJurusan() == null) continue;
                Long jid = pl.getJurusan().getId();
                int cplCount = cplCountPerPlId.containsKey(pl.getId())
                               ? cplCountPerPlId.get(pl.getId()) : 0;
                if (!d.plPerJurusan.containsKey(jid))
                    d.plPerJurusan.put(jid, new ArrayList<Object[]>());
                d.plPerJurusan.get(jid).add(new Object[]{
                    nvl(pl.getKode()),  // 0: kode PL
                    nvl(pl.getNama()),  // 1: nama PL
                    cplCount            // 2: CPL count
                });
            }

            // Compute PL count per jurusan for rekap table
            for (Map.Entry<Long, List<Object[]>> e : d.plPerJurusan.entrySet())
                d.plCountByJurusan.put(e.getKey(), e.getValue().size());
        }

        return d;
    }

    // ====================================================================
    // Cache + filter helpers
    // ====================================================================

    private String cacheKey() {
        Long uid = tbmuser != null ? tbmuser.getId() : null;
        return uid + "|" + filterTa + "|" + filterSemester + "|" + filterJurusanId + "|" + filterFakultasId;
    }

    private ObeData loadDataCached() {
        String key = cacheKey();
        Long exp = CACHE_EXPIRY.get(key);
        if (exp != null && exp > System.currentTimeMillis() && DATA_CACHE.containsKey(key))
            return DATA_CACHE.get(key);
        ObeData d = loadData();
        purgeExpiredCache();
        DATA_CACHE.put(key, d);
        CACHE_EXPIRY.put(key, System.currentTimeMillis() + CACHE_TTL_MS);
        return d;
    }

    /**
     * Buang entry kadaluarsa dari kedua map cache (optimasi RAM Fase 2): TTL sebelumnya
     * hanya dicek saat READ key yang sama, sehingga kombinasi filter yang tidak diakses
     * lagi menumpuk selamanya (value ObeData berisi agregat besar). Dipanggil saat put
     * (jarang — maksimal sekali per key per TTL). Race dengan put paralel key sama hanya
     * berakibat cache-miss sekali (dimuat ulang) — bukan masalah correctness.
     */
    private static void purgeExpiredCache() {
        long sekarang = System.currentTimeMillis();
        for (java.util.Iterator<java.util.Map.Entry<String, Long>> it = CACHE_EXPIRY.entrySet().iterator(); it
                .hasNext();) {
            java.util.Map.Entry<String, Long> e = it.next();
            Long kadaluarsa = e.getValue();
            if (kadaluarsa == null || kadaluarsa <= sekarang) {
                it.remove();
                DATA_CACHE.remove(e.getKey());
            }
        }
    }

    private List<Object[]> filteredRekapMk(ObeData d) {
        if (filterMk == null || filterMk.trim().isEmpty()) return d.rekapMk;
        String q = filterMk.trim().toLowerCase();
        List<Object[]> res = new ArrayList<Object[]>();
        for (Object[] r : d.rekapMk) {
            if (r[0].toString().toLowerCase().contains(q) || r[1].toString().toLowerCase().contains(q))
                res.add(r);
        }
        return res;
    }

    private List<String> filteredCpmkMkKeys(ObeData d) {
        if (filterMk == null || filterMk.trim().isEmpty())
            return new ArrayList<String>(d.cpmkPerMk.keySet());
        String q = filterMk.trim().toLowerCase();
        List<String> res = new ArrayList<String>();
        for (String mkKode : d.cpmkPerMk.keySet()) {
            String mkNama = nvl(d.mkNamaByKode.get(mkKode));
            if (mkKode.toLowerCase().contains(q) || mkNama.toLowerCase().contains(q))
                res.add(mkKode);
        }
        return res;
    }

    // ====================================================================
    // Render sections — Loop 0
    // ====================================================================

    private void renderHero(ObeData d) {
        int pct = d.totalMk == 0 ? 0 : d.sudahRps * 100 / d.totalMk;
        appendHtml(this,
            "<div style='border-radius:18px; padding:20px 24px; margin-bottom:14px;"
            + " background:linear-gradient(135deg,rgba(0,0,0,.28) 0%,rgba(0,0,0,0) 55%),"
            + "linear-gradient(135deg,#1e40af 0%,#0ea5e9 100%);"
            + " color:#fff; box-shadow:0 8px 24px rgba(30,64,175,.22);'>"
            + "<div style='font-size:19px; font-weight:900; letter-spacing:-.02em;'>"
            + "Outcome Based Education (OBE)</div>"
            + "<div style='font-size:12px; opacity:.84; margin-top:4px; line-height:1.55;'>"
            + "Pantau status RPS OBE, cakupan CPL, dan rekap CPMK seluruh mata kuliah dalam satu layar."
            + " Loop 1 &#8594; CPMK &middot; Loop 2 &#8594; CPL &middot; Loop 3 &#8594; Profil Lulusan</div>"
            + "<div style='margin-top:14px; display:flex; gap:10px; flex-wrap:wrap;'>"
            + heroBadge(d.totalMk + " MK OBE")
            + heroBadge(d.sudahRps + " Sudah RPS (" + pct + "%)")
            + heroBadge(filterTa != null && !filterTa.isEmpty() ? "TA " + esc(filterTa) : "Semua TA")
            + heroBadge(filterSemester != null && !filterSemester.isEmpty()
                        ? "Smt " + esc(filterSemester) : "Semua Semester")
            + "</div></div>");
    }

    private String heroBadge(String text) {
        return "<span style='display:inline-block; border-radius:999px; padding:4px 12px;"
               + " font-size:11px; font-weight:700; background:rgba(255,255,255,.2);'>"
               + esc(text) + "</span>";
    }

    @SuppressWarnings("deprecation")
    private void renderFilter() {
        Div bar = new Div();
        bar.setStyle("background:#fff; border-radius:14px; padding:12px 16px;"
                   + " box-shadow:0 2px 8px rgba(0,0,0,.06); margin-bottom:14px;"
                   + " display:flex; align-items:center; gap:10px; flex-wrap:wrap;");
        bar.setParent(this);

        appendHtml(bar, "<span style='font-size:12px; font-weight:700; color:#475569;'>Filter:</span>");

        appendHtml(bar, "<span style='font-size:11px; color:#64748b;'>Tahun Akademik</span>");
        cbTa = new Combobox();
        cbTa.setWidth("150px");
        cbTa.setReadonly(true);
        cbTa.setParent(bar);
        Comboitem semuaTa = new Comboitem("Semua TA"); semuaTa.setValue(""); cbTa.appendChild(semuaTa);
        Common.generateTahunAjaranDanSemua(cbTa);
        if (filterTa != null && !filterTa.isEmpty()) {
            Common.selectComboItem(cbTa, filterTa);
            if (cbTa.getSelectedItem() == null) cbTa.setSelectedIndex(0);
        } else {
            cbTa.setSelectedIndex(0);
        }

        appendHtml(bar, "<span style='font-size:11px; color:#64748b;'>Semester</span>");
        cbSemester = new Combobox();
        cbSemester.setWidth("100px");
        cbSemester.setReadonly(true);
        cbSemester.setParent(bar);
        for (String[] opt : new String[][]{{"", "Semua"}, {"Ganjil", "Ganjil"}, {"Genap", "Genap"}}) {
            Comboitem ci = new Comboitem(opt[1]); ci.setValue(opt[0]); cbSemester.appendChild(ci);
        }
        cbSemester.setSelectedIndex("Ganjil".equals(filterSemester) ? 1 : ("Genap".equals(filterSemester) ? 2 : 0));

        Dosen dosen = tbmuser != null ? tbmuser.ambilDosen() : null;
        Mahasiswa mhsRole = tbmuser != null ? tbmuser.getMahasiswa() : null;
        Jurusan roleJurusan = getRoleJurusan();
        ais.database.model.Fakultas roleFakultas = getRoleFakultas();

        if (mhsRole != null) {
            appendHtml(bar,
                "<span style='background:#eff6ff; color:#1e40af; border-radius:999px; padding:3px 10px;"
                + " font-size:10px; font-weight:700; border:1px solid #93c5fd;'>"
                + "&#128100; Perkuliahan saya</span>");
        } else if (dosen != null) {
            appendHtml(bar,
                "<span style='background:#f0fdf4; color:#166534; border-radius:999px; padding:3px 10px;"
                + " font-size:10px; font-weight:700; border:1px solid #86efac;'>"
                + "&#127979; Pengajaran saya: " + esc(dosen.getNama() != null ? dosen.getNama() : "") + "</span>");
        } else if (roleJurusan != null) {
            appendHtml(bar,
                "<span style='background:#fef3c7; color:#78350f; border-radius:999px; padding:3px 10px;"
                + " font-size:10px; font-weight:700; border:1px solid #fcd34d;'>"
                + "&#127979; Prodi: " + esc(roleJurusan.getNama() != null ? roleJurusan.getNama() : "") + "</span>");
        } else if (roleFakultas != null) {
            appendHtml(bar,
                "<span style='background:#fdf4ff; color:#581c87; border-radius:999px; padding:3px 10px;"
                + " font-size:10px; font-weight:700; border:1px solid #e9d5ff;'>"
                + "&#127979; Fakultas: " + esc(roleFakultas.getNama() != null ? roleFakultas.getNama() : "") + "</span>");
            appendHtml(bar, "<span style='font-size:11px; color:#64748b;'>Program Studi</span>");
            cbJurusan = new Combobox();
            cbJurusan.setWidth("200px");
            cbJurusan.setReadonly(true);
            cbJurusan.setParent(bar);
            Comboitem semuaJur = new Comboitem("Semua Prodi"); semuaJur.setValue(null);
            cbJurusan.appendChild(semuaJur);
            List<Jurusan> jurs = (List<Jurusan>) HibernateUtil.currentSession()
                .createCriteria(Jurusan.class)
                .add(Restrictions.eq("fakultas.id", roleFakultas.getId()))
                .addOrder(Order.asc("nama")).list();
            for (Jurusan j : jurs) {
                Comboitem ci = new Comboitem(j.getNama()); ci.setValue(j);
                cbJurusan.appendChild(ci);
                if (filterJurusanId != null && j.getId().equals(filterJurusanId))
                    cbJurusan.setSelectedItem(ci);
            }
            if (cbJurusan.getSelectedItem() == null) cbJurusan.setSelectedIndex(0);
        } else {
            appendHtml(bar, "<span style='font-size:11px; color:#64748b;'>Fakultas</span>");
            cbFakultas = new Combobox();
            cbFakultas.setWidth("180px");
            cbFakultas.setReadonly(true);
            cbFakultas.setParent(bar);

            appendHtml(bar, "<span style='font-size:11px; color:#64748b;'>Program Studi</span>");
            cbJurusan = new Combobox();
            cbJurusan.setWidth("200px");
            cbJurusan.setReadonly(true);
            cbJurusan.setParent(bar);

            Common.initFakultasDanJurusanDanSemua(cbFakultas, cbJurusan);
        }

        appendHtml(bar, "<span style='font-size:11px; color:#64748b;'>Cari MK</span>");
        txFilterMk = new org.zkoss.zul.Textbox();
        //txFilterMk.setPlaceholder("Kode / nama MK...");
        txFilterMk.setWidth("150px");
        txFilterMk.setValue(filterMk != null ? filterMk : "");
        txFilterMk.setParent(bar);
        txFilterMk.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                filterMk = txFilterMk.getValue();
                pageRps = 0; pageCpl = 0; pageCpmk = 0;
                if (lastData != null) {
                    renderRekapRpsPage(lastData);
                    renderCplRekapTablePage(lastData);
                    renderCpmkPerMkPage(lastData);
                }
            }
        });

        MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig("Tampilkan", "/img/search.gif");
        btn.setParent(bar);
        btn.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                readFilter();
                tampilLoading();
                Common.createDefaultTimer(new EventListener() {
                    @Override
                    public void onEvent(Event e2) throws Exception {
                        renderAll();
                    }
                });
            }
        });

        // ── Download OBE massal (*.xlsx, 7 sheet) ──
        MyToolbarbuttonConfig btnDownload = new MyToolbarbuttonConfig("Download OBE (Excel)", "/img/print.png");
        btnDownload.setTooltiptext("Unduh seluruh pemetaan OBE (PL, CPL, CPMK, Sub-CPMK, Bahan Kajian, Referensi) ke berkas Excel");
        btnDownload.setParent(bar);
        btnDownload.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                byte[] data = ais.action.master.obe.ObeMassExcelHelper.buildWorkbookBytes();
                String fname = "OBE_Mapping_"
                        + new java.text.SimpleDateFormat("yyyyMMdd_HHmm").format(new java.util.Date()) + ".xlsx";
                org.zkoss.zul.Filedownload.save(data,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", fname);
            }
        });

        // ── Upload OBE massal (*.xlsx): hanya tampil bila role pengguna diizinkan
        //    lewat konfigurasi hak_akses_upload_obe_excel
        //    (default: am,adAkdmk,Akademik,admfak,admprd) ──
        if (bolehUploadObe()) {
        MyToolbarbuttonConfig btnUpload = new MyToolbarbuttonConfig("Upload OBE (Excel)", "/img/new.gif");
        btnUpload.setTooltiptext("Impor data master OBE dari berkas Excel (buat/perbarui PL, CPL, CPMK, Bahan Kajian berdasarkan KODE)");
        btnUpload.setUpload("true,maxsize=-1");
        btnUpload.setParent(bar);
        btnUpload.addEventListener("onUpload", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                org.zkoss.util.media.Media media = ((org.zkoss.zk.ui.event.UploadEvent) e).getMedia();
                if (media == null) {
                    return;
                }
                if (media.getName() == null || !media.getName().toLowerCase().endsWith("xlsx")) {
                    ais.ui.util.MyMessageboxConfig.show("Mohon maaf, berkas yang Bapak/Ibu unggah harus berformat .xlsx (Microsoft Excel). Langkah yang dapat dilakukan: (1) simpan atau ekspor berkas dalam format .xlsx; (2) pastikan ekstensi berkas benar-benar .xlsx; (3) unggah kembali berkas yang sesuai.");
                    return;
                }
                String hasil = ais.action.master.obe.ObeMassExcelHelper.importWorkbook(media.getStreamData());
                ais.ui.util.MyMessageboxConfig.show(hasil);
                tampilLoading();
                Common.createDefaultTimer(new EventListener() {
                    @Override
                    public void onEvent(Event e2) throws Exception {
                        renderAll();
                    }
                });
            }
        });
        }

        // ── Hitung Ulang Ujian & Tugas (OBE) — DISEMBUNYIKAN untuk Siswa/Mahasiswa ──
        boolean pesertaDidik = tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null);
        if (!pesertaDidik) {
            MyToolbarbuttonConfig btnHitungUjian = new MyToolbarbuttonConfig("Hitung Ulang Ujian",
                    "/img/svg/check2-circle.svg");
            btnHitungUjian.setTooltiptext("Hitung ulang nilai Ujian (HasilUjianMahasiswa) seluruh MK OBE sesuai "
                    + "filter — pratinjau dulu, simpan bila cocok");
            btnHitungUjian.setParent(bar);
            btnHitungUjian.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    bukaHitungUlangObe(true);
                }
            });

            MyToolbarbuttonConfig btnHitungTugas = new MyToolbarbuttonConfig("Hitung Ulang Tugas",
                    "/img/svg/check2-circle.svg");
            btnHitungTugas.setTooltiptext("Hitung ulang nilai Tugas & Tugas Kelompok OBE (agregasi Sub-CPMK) "
                    + "seluruh MK OBE sesuai filter — pratinjau dulu, simpan bila cocok");
            btnHitungTugas.setParent(bar);
            btnHitungTugas.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    bukaHitungUlangObe(false);
                }
            });
        }
    }

    private void readFilter() {
        if (cbTa != null && cbTa.getSelectedItem() != null) {
            Object v = cbTa.getSelectedItem().getValue();
            filterTa = (v == null) ? "" : v.toString();
        }
        if (cbSemester != null && cbSemester.getSelectedItem() != null) {
            Object v = cbSemester.getSelectedItem().getValue();
            filterSemester = (v == null) ? "" : v.toString();
        }
        Jurusan rrJurusan = getRoleJurusan();
        ais.database.model.Fakultas rrFakultas = getRoleFakultas();
        if (rrJurusan != null) {
            filterJurusanId = rrJurusan.getId();
            filterFakultasId = null;
        } else if (rrFakultas != null) {
            filterFakultasId = rrFakultas.getId();
            filterJurusanId = null;
            if (cbJurusan != null && cbJurusan.getSelectedItem() != null) {
                Object v = cbJurusan.getSelectedItem().getValue();
                if (v instanceof Jurusan) filterJurusanId = ((Jurusan) v).getId();
            }
        } else {
            filterJurusanId  = null;
            filterFakultasId = null;
            if (cbJurusan != null && cbJurusan.getSelectedItem() != null) {
                Object v = cbJurusan.getSelectedItem().getValue();
                if (v instanceof Jurusan) filterJurusanId = ((Jurusan) v).getId();
            }
            if (filterJurusanId == null && cbFakultas != null && cbFakultas.getSelectedItem() != null) {
                Object v = cbFakultas.getSelectedItem().getValue();
                if (v instanceof ais.database.model.Fakultas)
                    filterFakultasId = ((ais.database.model.Fakultas) v).getId();
            }
        }
        if (txFilterMk != null) filterMk = txFilterMk.getValue();
    }

    private void renderStatsCards(ObeData d) {
        int pct = d.totalMk == 0 ? 0 : d.sudahRps * 100 / d.totalMk;
        Div row = new Div();
        row.setStyle("display:flex; gap:10px; flex-wrap:wrap; margin-bottom:14px;");
        row.setParent(this);
        metricCard(row, "Total MK OBE",    d.totalMk,    "Mata kuliah kurikulum OBE aktif",     "#dbeafe", "#1e40af", "#1d4ed8");
        metricCard(row, "RPS Terisi (" + pct + "%)", d.sudahRps, "Sudah memiliki RPS OBE lengkap", "#dcfce7", "#166534", "#16a34a");
        metricCard(row, "RPS Belum Ada",   d.belumRps,   "Belum mengisi RPS OBE",               "#fee2e2", "#991b1b", "#dc2626");
        metricCard(row, "Punya CPL",       d.mkPunyaCpl, "MK sudah punya Capaian Lulusan",      "#f3e8ff", "#6b21a8", "#7c3aed");
    }

    private void metricCard(Div parent, String label, int val, String sub,
                            String bg, String dark, String accent) {
        appendHtml(parent,
            "<div style='flex:1 1 140px; min-width:130px; border-radius:16px; padding:16px; background:" + bg + ";'>"
            + "<div style='font-size:10px; font-weight:700; text-transform:uppercase; letter-spacing:.08em;"
            + " color:" + dark + "; opacity:.8;'>" + esc(label) + "</div>"
            + "<div style='font-size:36px; font-weight:900; color:" + accent + "; line-height:1; margin-top:8px;'>"
            + val + "</div>"
            + "<div style='font-size:10px; color:" + dark + "; opacity:.7; margin-top:6px; line-height:1.4;'>"
            + esc(sub) + "</div>"
            + "</div>");
    }

    private void renderRekapRps(Div parent, ObeData d) {
        Div panel = createPanel("&#128196; Rekap Status RPS OBE per Mata Kuliah", parent);
        divRpsContainer = new Div();
        divRpsContainer.setParent(panel);
        renderRekapRpsPage(d);
    }

    private void renderRekapRpsPage(ObeData d) {
        if (divRpsContainer == null) return;
        Common.clear(divRpsContainer);
        List<Object[]> rows = filteredRekapMk(d);
        if (rows.isEmpty()) { appendHtml(divRpsContainer, emptyMsg()); return; }
        int total = rows.size();
        int from = pageRps * PAGE_SIZE;
        if (from >= total) { pageRps = 0; from = 0; }
        int to = Math.min(from + PAGE_SIZE, total);
        List<Object[]> page = rows.subList(from, to);
        appendHtml(divRpsContainer, DashboardUiUtil.pageInfoBar(pageRps, PAGE_SIZE, total));

        // Tabel dibangun sebagai komponen ZK (Grid) — bukan HTML mentah — agar
        // ikon "Cetak RPS OBE" di tiap baris bisa diklik dan memanggil server.
        Div scroll = new Div();
        scroll.setStyle("overflow-x:auto;");
        scroll.setParent(divRpsContainer);

        Grid grid = new Grid();
        grid.setWidth("100%");
        grid.setStyle("border:none; font-size:11px;");
        grid.setParent(scroll);

        Columns cols = new Columns();
        cols.setParent(grid);
        String[] heads  = {"Kode", "Mata Kuliah", "Prodi", "SKS", "Status RPS", "Tgl Susun", "PL", "CPL", "CPMK", "Sub-CPMK", "CQI", "Cetak", "Lihat"};
        String[] widths = {"7%", "20%", "11%", "4%", "8%", "8%", "4%", "4%", "5%", "6%", "8%", "7%", "5%"};
        boolean[] center = {false, false, false, true, false, false, true, true, true, true, true, true, true};
        for (int i = 0; i < heads.length; i++) {
            Column c = new Column(heads[i]);
            c.setWidth(widths[i]);
            if (center[i]) c.setAlign("center");
            c.setParent(cols);
        }

        Rows zr = new Rows();
        zr.setParent(grid);
        for (Object[] r : page) {
            boolean rps = Boolean.TRUE.equals(r[5]);
            String sBg  = rps ? "#dcfce7" : "#fee2e2";
            String sClr = rps ? "#166534" : "#991b1b";
            String sTxt = rps ? "&#10003; Terisi" : "&#10007; Belum";
            Long jurId   = r[10] != null ? (Long) r[10] : null;
            int plCount  = jurId != null && d.plCountByJurusan.containsKey(jurId)
                           ? d.plCountByJurusan.get(jurId) : 0;
            int subCount = d.subCpmkCountByMk.containsKey(r[0].toString())
                           ? d.subCpmkCountByMk.get(r[0].toString()) : 0;
            final Long kpmId = r.length > 11 && r[11] != null ? (Long) r[11] : null;
            final Long perkId = r.length > 12 && r[12] != null ? (Long) r[12] : null;

            Row row = new Row();
            row.setParent(zr);

            row.appendChild(rpsCell(r[0].toString(), "font-weight:600; color:#1e293b;"));

            // Mata Kuliah + nama dosen (dua baris)
            String mkHtml = "<div style='color:#334155;'>" + esc(r[1].toString()) + "</div>";
            if (!r[3].toString().isEmpty())
                mkHtml += "<div style='font-size:10px; color:#94a3b8;'>" + esc(r[3].toString()) + "</div>";
            row.appendChild(new Html(mkHtml));

            row.appendChild(rpsCell(r[2].toString(), "color:#475569;"));
            row.appendChild(rpsCell(String.valueOf(r[4]), null));

            row.appendChild(new Html("<span style='border-radius:999px; padding:2px 8px; font-size:10px;"
                + " font-weight:700; background:" + sBg + "; color:" + sClr + ";'>" + sTxt + "</span>"));

            row.appendChild(rpsCell(r[6].toString(), "color:#64748b;"));
            row.appendChild(rpsCell(plCount > 0 ? String.valueOf(plCount) : "-",
                    plCount > 0 ? "color:#7c3aed; font-weight:700;" : "color:#94a3b8;"));
            row.appendChild(rpsCell(String.valueOf(r[7]), null));
            row.appendChild(rpsCell(String.valueOf(r[8]), null));
            row.appendChild(rpsCell(subCount > 0 ? String.valueOf(subCount) : "-",
                    subCount > 0 ? "color:#0369a1; font-weight:700;" : "color:#94a3b8;"));

            // CQI status badge (ADMIN_OK/ADMIN_REVISI/OK/PARTIAL/"")
            String cqiStatus = perkId != null && d.cqiStatusByPerkId.containsKey(perkId)
                               ? d.cqiStatusByPerkId.get(perkId) : "";
            String cqiBg, cqiClr, cqiTxt;
            if ("ADMIN_OK".equals(cqiStatus)) {
                cqiBg = "#bbf7d0"; cqiClr = "#14532d"; cqiTxt = "&#10004; Disetujui";
            } else if ("ADMIN_REVISI".equals(cqiStatus)) {
                cqiBg = "#fecaca"; cqiClr = "#7f1d1d"; cqiTxt = "&#9888; Revisi";
            } else if ("OK".equals(cqiStatus)) {
                cqiBg = "#dcfce7"; cqiClr = "#166534"; cqiTxt = "&#10003; Lengkap";
            } else if ("PARTIAL".equals(cqiStatus)) {
                cqiBg = "#fef3c7"; cqiClr = "#78350f"; cqiTxt = "&#9888; Sebagian";
            } else {
                cqiBg = "#f1f5f9"; cqiClr = "#94a3b8"; cqiTxt = "&#9711; Belum";
            }
            final Long cqiPerkId = perkId;
            Html cqiBadge = new Html("<span style='border-radius:999px; padding:2px 7px; font-size:10px;"
                + " font-weight:700; background:" + cqiBg + "; color:" + cqiClr + "; cursor:pointer;'>"
                + cqiTxt + "</span>");
            row.appendChild(cqiBadge);

            // Ikon "Cetak RPS OBE" (HTML) — format sama dengan tombol Cetak (HTML) di halaman RPS.
            MyToolbarbuttonConfig btnCetak = new MyToolbarbuttonConfig("", "/img/print.png");
            btnCetak.setTooltiptext("Cetak RPS OBE (HTML)");
            btnCetak.setStyle("cursor:pointer;");
            if (kpmId != null) {
                btnCetak.addEventListener("onClick", new EventListener() {
                    public void onEvent(Event e) throws Exception {
                        ais.action.master.RpsObeAction.cetakHtmlByKpmId(kpmId);
                    }
                });
            } else {
                btnCetak.setDisabled(true);
            }
            row.appendChild(btnCetak);

            // Ikon "Lihat" (mata) — buka detail RPS OBE (RpsObeAction) dalam popup MyWindow.
            MyToolbarbuttonConfig btnLihat = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
            btnLihat.setTooltiptext("Lihat RPS OBE (detail)");
            btnLihat.setStyle("cursor:pointer;");
            if (kpmId != null) {
                btnLihat.addEventListener("onClick", new EventListener() {
                    public void onEvent(Event e) throws Exception {
                        bukaPopupRpsObe(kpmId, perkId);
                    }
                });
            } else {
                btnLihat.setDisabled(true);
            }
            row.appendChild(btnLihat);
        }

        renderPageNav(divRpsContainer, pageRps, total,
            new EventListener() { public void onEvent(Event e) throws Exception { pageRps--; if (lastData != null) renderRekapRpsPage(lastData); }},
            new EventListener() { public void onEvent(Event e) throws Exception { pageRps++; if (lastData != null) renderRekapRpsPage(lastData); }}
        );
    }

    /**
     * Buka detail OBE satu mata kuliah dalam popup MyWindow berisi Tabbox 3 tab —
     * meniru tampilan e-Learning: "RPS OBE", "Nilai OBE", "Rekap Nilai OBE".
     *
     * - RPS OBE selalu tampil (RpsObeAction via rps_obe.zul, param "kur").
     * - Nilai OBE & Rekap Nilai OBE butuh perkuliahan (tiap baris dasbor = satu
     *   perkuliahan, id-nya dibawa di r[12]); Rekap hanya untuk non-mahasiswa
     *   (sesuai aturan tab di AktifitasPerkuliahanHelper). Konten Nilai/Rekap
     *   dimuat lazy saat tab diklik.
     */
    /**
     * Membuka pratinjau "Hitung Ulang" nilai OBE (Ujian bila {@code ujian}=true, selain itu Tugas &amp;
     * Tugas Kelompok) untuk SELURUH perkuliahan OBE pada filter aktif. Perhitungan berjalan di thread latar
     * (non-blocking); hasilnya di-render sebagai grid (Nilai Lama &rarr; Baru) yang dapat diekspor PDF/Excel,
     * dengan tombol "Simpan Semua" untuk menuliskannya ke DB.
     */
    private void bukaHitungUlangObe(final boolean ujian) {
        final String judul = ujian ? "Hitung Ulang Ujian (OBE)" : "Hitung Ulang Tugas (OBE)";

        // Id perkuliahan dari baris terfilter (r[12] = id perkuliahan).
        final java.util.List<Long> perkIds = new java.util.ArrayList<Long>();
        try {
            for (Object[] r : filteredRekapMk(loadDataCached())) {
                if (r != null && r.length > 12 && r[12] instanceof Number) {
                    perkIds.add(((Number) r[12]).longValue());
                }
            }
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }

        final ais.ui.util.MyWindow window = new ais.ui.util.MyWindow(judul + " — Pratinjau", "normal", true);
        window.setWidth("92%");
        window.setHeight("90%");
        window.setMaximizable(true);
        window.setSizable(true);
        window.setPosition("center,center");
        window.setContentStyle("overflow:auto;");
        if (getPage() != null) {
            window.setPage(getPage());
        } else {
            window.setParent(this);
        }
        try { window.doOverlapped(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:1150");}

        org.zkoss.zul.Vbox root = new org.zkoss.zul.Vbox();
        root.setWidth("100%");
        root.setParent(window);
        final Label info = new Label(perkIds.isEmpty()
                ? "Tidak ada perkuliahan OBE pada filter saat ini."
                : "Menghitung ulang " + perkIds.size() + " perkuliahan OBE... mohon tunggu.");
        info.setStyle("font-weight:600;padding:6px;");
        info.setParent(root);
        final org.zkoss.zul.Div gridWrap = new org.zkoss.zul.Div();
        gridWrap.setWidth("100%");
        gridWrap.setParent(root);

        if (perkIds.isEmpty()) {
            return;
        }

        // Hitung di thread latar; render via poll timer (event-thread) saat siap → tidak memblokir UI.
        final java.util.List<ais.action.master.obe.HitungUlangNilaiObeHelper.Baris>[] holder = new java.util.List[] {
                null };
        final boolean[] siap = { false };
        Thread bg = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    holder[0] = ujian ? ais.action.master.obe.HitungUlangNilaiObeHelper.pratinjauUjian(perkIds, null)
                            : ais.action.master.obe.HitungUlangNilaiObeHelper.pratinjauTugas(perkIds, null);
                } catch (Throwable t) {
                    holder[0] = new java.util.ArrayList<ais.action.master.obe.HitungUlangNilaiObeHelper.Baris>();
                } finally {
                    siap[0] = true;
                }
            }
        }, "obe-hitung-ulang-preview");
        bg.setDaemon(true);
        bg.start();

        final org.zkoss.zul.Timer timer = new org.zkoss.zul.Timer(500);
        timer.setRepeats(true);
        timer.setParent(window);
        final int[] tick = { 0 };
        timer.addEventListener("onTimer", new EventListener() {
            @Override
            public void onEvent(Event ev) throws Exception {
                tick[0]++;
                if (!siap[0] && tick[0] < 3600) {
                    return;
                }
                timer.stop();
                timer.detach();
                renderHasilHitungUlang(judul, ujian, perkIds, gridWrap, info, holder[0], window);
            }
        });
        timer.start();
    }

    /** Render grid hasil pratinjau + tombol ekspor (PDF/Excel) + "Simpan Semua". */
    private void renderHasilHitungUlang(final String judul, final boolean ujian, final java.util.List<Long> perkIds,
            final org.zkoss.zul.Div gridWrap, final Label info,
            java.util.List<ais.action.master.obe.HitungUlangNilaiObeHelper.Baris> hasil,
            final ais.ui.util.MyWindow window) {
        int n = hasil == null ? 0 : hasil.size();
        info.setValue("Pratinjau selesai: " + n + " nilai dihitung ulang (BELUM disimpan). "
                + "Periksa lalu klik \"Simpan Semua\" untuk menyimpan.");

        org.zkoss.zul.Toolbar toolbar = new org.zkoss.zul.Toolbar();
        toolbar.setWidth("100%");
        toolbar.setStyle("padding:6px 8px;background:#ffffff;border:1px solid #e2e8f0;border-radius:8px;");
        toolbar.setParent(gridWrap);

        org.zkoss.zul.Grid grid = new org.zkoss.zul.Grid();
        grid.setWidth("100%");
        grid.setSclass("dgrid");
        org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
        cols.setParent(grid);
        String[] head = { "Kode MK", "Mata Kuliah", ujian ? "Ujian" : "Tugas", "Mahasiswa", "Nilai Lama", "Nilai Baru",
                "Selisih" };
        for (String h : head) {
            org.zkoss.zul.Column c = new org.zkoss.zul.Column(h);
            c.setParent(cols);
        }
        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(grid);
        if (hasil != null) {
            for (ais.action.master.obe.HitungUlangNilaiObeHelper.Baris b : hasil) {
                org.zkoss.zul.Row row = new org.zkoss.zul.Row();
                row.setParent(rows);
                new Label(nvl(b.kodeMk)).setParent(row);
                new Label(nvl(b.namaMk)).setParent(row);
                new Label(nvl(b.item)).setParent(row);
                new Label(nvl(b.mahasiswa)).setParent(row);
                new Label(Common.numberFormat.get().format(b.nilaiLama)).setParent(row);
                new Label(Common.numberFormat.get().format(b.nilaiBaru)).setParent(row);
                double d = b.nilaiBaru - b.nilaiLama;
                Label sel = new Label((d > 0 ? "+" : "") + Common.numberFormat.get().format(d));
                if (Math.abs(d) > 0.001) {
                    sel.setStyle("color:" + (d > 0 ? "#16a34a" : "#dc2626") + ";font-weight:600;");
                }
                sel.setParent(row);
            }
        }
        grid.setParent(gridWrap);

        // Ekspor PDF + Excel (mesin scraping grid).
        ais.ui.util.DashboardGridExportHelper.pasangTombol(toolbar, gridWrap, judul);

        // Simpan Semua → tulis ke DB.
        final MyToolbarbuttonConfig btnSimpan = new MyToolbarbuttonConfig("Simpan Semua", "/img/save.gif");
        btnSimpan.setTooltiptext("Tulis semua nilai baru ke database");
        btnSimpan.setParent(toolbar);
        btnSimpan.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                ais.ui.util.MyMessageboxConfig.show(
                        "Simpan " + (ujian ? "nilai Ujian" : "nilai Tugas")
                                + " hasil hitung ulang ke database untuk semua MK pada filter ini?",
                        "Konfirmasi", ais.ui.util.MyMessageboxConfig.OK | ais.ui.util.MyMessageboxConfig.CANCEL,
                        ais.ui.util.MyMessageboxConfig.QUESTION, new EventListener() {
                            @Override
                            public void onEvent(Event ev) throws Exception {
                                if (Integer.parseInt(ev.getData().toString()) != ais.ui.util.MyMessageboxConfig.OK) {
                                    return;
                                }
                                btnSimpan.setDisabled(true);
                                info.setValue("Menyimpan ke database... mohon tunggu.");
                                final boolean[] done = { false };
                                Thread t = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            if (ujian) {
                                                ais.action.master.obe.HitungUlangNilaiObeHelper.simpanUjian(perkIds,
                                                        null);
                                            } else {
                                                ais.action.master.obe.HitungUlangNilaiObeHelper.simpanTugas(perkIds,
                                                        null);
                                            }
                                        } catch (Throwable t2) { ais.common.ErrorAuditUtil.record(t2, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:1288");
                                        } finally {
                                            done[0] = true;
                                        }
                                    }
                                }, "obe-hitung-ulang-simpan");
                                t.setDaemon(true);
                                t.start();
                                final org.zkoss.zul.Timer tt = new org.zkoss.zul.Timer(500);
                                tt.setRepeats(true);
                                tt.setParent(window);
                                tt.addEventListener("onTimer", new EventListener() {
                                    @Override
                                    public void onEvent(Event e3) throws Exception {
                                        if (!done[0]) {
                                            return;
                                        }
                                        tt.stop();
                                        tt.detach();
                                        info.setValue("Selesai — nilai berhasil disimpan ke database.");
                                        ais.ui.util.MyMessageboxConfig.show("Nilai berhasil disimpan.", "Informasi",
                                                ais.ui.util.MyMessageboxConfig.OK,
                                                ais.ui.util.MyMessageboxConfig.INFORMATION);
                                    }
                                });
                                tt.start();
                            }
                        });
            }
        });
    }

    private void bukaPopupRpsObe(Long kpmId, Long perkId) {
        if (kpmId == null) {
            return;
        }
        try {
            ais.ui.util.MyWindow window = new ais.ui.util.MyWindow("Detail OBE Mata Kuliah", "normal", true);
            window.setWidth("96%");
            window.setHeight("93%");
            window.setMaximizable(true);
            window.setSizable(true);
            window.setPosition("center,center");
            window.setContentStyle("overflow:auto;");
            // Tempelkan ke page agar tampil sebagai popup (termasuk pada konteks embedded).
            if (getPage() != null) {
                window.setPage(getPage());
            } else {
                window.setParent(this);
            }

            // Perkuliahan untuk tab Nilai OBE & Rekap Nilai OBE (opsional).
            final ais.database.model.Perkuliahan perkuliahan = perkId == null ? null
                    : (ais.database.model.Perkuliahan) ais.common.ConstantValues.ambil(
                            ais.database.model.Perkuliahan.class.getName(), perkId);

            ais.database.model.Tbmuser tbmuser = null;
            try { tbmuser = ais.common.Common.getCurrentUser(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:1345");}
            final boolean bolehRekap = tbmuser != null && tbmuser.getMahasiswa() == null;

            org.zkoss.zul.Tabbox tabbox = new org.zkoss.zul.Tabbox();
            tabbox.setWidth("100%");
            tabbox.setHeight("100%");
            tabbox.setParent(window);
            org.zkoss.zul.Tabs tabs = new org.zkoss.zul.Tabs();
            tabs.setParent(tabbox);
            org.zkoss.zul.Tabpanels tabpanels = new org.zkoss.zul.Tabpanels();
            tabpanels.setParent(tabbox);

            // ── Tab 1: RPS OBE (selalu) ────────────────────────────────────────
            ais.ui.util.MyTabConfig tabRps = new ais.ui.util.MyTabConfig("RPS OBE");
            tabRps.setSelected(true);
            tabRps.setParent(tabs);
            org.zkoss.zul.Tabpanel panelRps = new org.zkoss.zul.Tabpanel();
            panelRps.setParent(tabpanels);
            // Panel scrollable: Tabbox height 100% memberi tinggi pasti pada tabpanel; konten
            // (rps_obe.zul, height 100%) di-set tinggi besar agar tabpanel meng-scroll-nya
            // (pola sama dgn PenjadwalanHelper: include 12000px). Tab tetap terlihat di atas.
            panelRps.setStyle("overflow:auto;");
            String srcRps = "/pages/master/rps_obe.zul?kur=" + kpmId
                    + (perkId != null ? "&perkuliahan=" + perkId : "");
            ais.ui.util.MyInclude incRps = new ais.ui.util.MyInclude(srcRps);
            incRps.setHeight("12000px");
            incRps.setParent(panelRps);

            // ── Tab 2: Nilai OBE & Tab 3: Rekap Nilai OBE (butuh perkuliahan) ───
            if (perkuliahan != null) {
                final ais.ui.util.MyTabConfig tabNilai = new ais.ui.util.MyTabConfig("Nilai OBE");
                tabNilai.setParent(tabs);
                final org.zkoss.zul.Tabpanel panelNilai = new org.zkoss.zul.Tabpanel();
                panelNilai.setParent(tabpanels);
                panelNilai.setStyle("overflow:auto;");
                tabNilai.addEventListener("onClick", new EventListener() {
                    public void onEvent(Event e) throws Exception {
                        if (panelNilai.getChildren().isEmpty()) {
                            ais.ui.util.MyInclude incNilai = new ais.ui.util.MyInclude(
                                    "/pages/master/nilai_obe.zul?perkuliahan=" + perkuliahan.getId());
                            incNilai.setHeight("12000px");
                            incNilai.setParent(panelNilai);
                        }
                    }
                });

                if (bolehRekap && perkuliahan.getKurikulumPunyaMatakuliah() != null) {
                    final ais.ui.util.MyTabConfig tabRekap = new ais.ui.util.MyTabConfig("Rekap Nilai OBE");
                    tabRekap.setParent(tabs);
                    final org.zkoss.zul.Tabpanel panelRekap = new org.zkoss.zul.Tabpanel();
                    panelRekap.setParent(tabpanels);
                    panelRekap.setStyle("overflow:auto;");
                    tabRekap.addEventListener("onClick", new EventListener() {
                        public void onEvent(Event e) throws Exception {
                            if (panelRekap.getChildren().isEmpty()) {
                                RekapHasilTugasPerTugasDanUjianObe rekap =
                                        new RekapHasilTugasPerTugasDanUjianObe(true, perkuliahan);
                                rekap.setClosable(false);
                                rekap.setWidth("100%");
                                rekap.setHeight("13050px");
                                panelRekap.appendChild(rekap);
                            }
                        }
                    });
                }
            }

            try {
                window.doModal();
            } catch (Exception modalEx) {
                // Pada konteks embedded, doModal() dapat gagal -> tampilkan highlighted di lapisan atas.
                window.doHighlighted();
                window.setZindex(100000);
            }
        } catch (Exception e) {
            ais.common.Common.tampilErrorJikaAdmin(e);
        }
    }

    /** Cell teks sederhana untuk Grid rekap RPS (alignment diatur lewat Column). */
    private Label rpsCell(String text, String style) {
        Label l = new Label(text == null ? "" : text);
        if (style != null) l.setStyle(style);
        return l;
    }

    private void renderCplHeatmap(Div parent, ObeData d) {
        Div panel = createPanel("&#128200; Cakupan CPL di Seluruh MK", parent);
        if (d.cplCoverage.isEmpty()) {
            appendHtml(panel,
                "<div style='color:#94a3b8; font-size:12px; text-align:center; padding:14px;'>"
                + "Isi <b>Bobot CPL per MK</b> di form RPS OBE untuk melihat heatmap.</div>");
            return;
        }
        int maxCount = 1;
        for (int v : d.cplCoverage.values()) if (v > maxCount) maxCount = v;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : d.cplCoverage.entrySet()) {
            String cpl  = entry.getKey();
            int count   = entry.getValue();
            int pct     = count * 100 / maxCount;
            double bobot = d.cplBobotSum.containsKey(cpl) ? d.cplBobotSum.get(cpl) : 0.0;
            String color = pct >= 80 ? "#16a34a" : (pct >= 50 ? "#2563eb" : (pct >= 25 ? "#d97706" : "#dc2626"));
            sb.append("<div style='margin:7px 0;'>");
            sb.append("<div style='display:flex; justify-content:space-between; margin-bottom:3px;'>");
            sb.append("<span style='font-size:11px; font-weight:700; color:#334155;'>").append(esc(cpl)).append("</span>");
            sb.append("<span style='font-size:11px; color:#1e293b;'>").append(count).append(" MK");
            if (bobot > 0) sb.append(" &nbsp;<span style='color:#64748b;font-size:10px;'>&Sigma;").append(String.format("%.0f", bobot)).append("%</span>");
            sb.append("</span></div>");
            sb.append("<div style='height:12px; border-radius:6px; background:#e2e8f0;'>");
            sb.append("<div style='height:12px; border-radius:6px; background:").append(color)
              .append("; width:").append(Math.min(100, pct)).append("%;'></div></div></div>");
        }
        appendHtml(panel, sb.toString());
    }

    private void renderCplRekapTable(Div parent, ObeData d) {
        if (d.rekapMk.isEmpty()) return;
        boolean hasCplBobot = false;
        for (Object[] r : d.rekapMk) if (!r[9].toString().isEmpty()) { hasCplBobot = true; break; }
        if (!hasCplBobot) return;
        Div panel = createPanel("&#127919; Detail CPL Bobot per Mata Kuliah", parent);
        divCplContainer = new Div();
        divCplContainer.setParent(panel);
        renderCplRekapTablePage(d);
    }

    private void renderCplRekapTablePage(ObeData d) {
        if (divCplContainer == null) return;
        Common.clear(divCplContainer);
        List<Object[]> allRows = filteredRekapMk(d);
        List<Object[]> rows = new ArrayList<Object[]>();
        for (Object[] r : allRows) if (!r[9].toString().isEmpty()) rows.add(r);
        if (rows.isEmpty()) { appendHtml(divCplContainer, emptyMsg()); return; }
        int total = rows.size();
        int from = pageCpl * PAGE_SIZE;
        if (from >= total) { pageCpl = 0; from = 0; }
        int to = Math.min(from + PAGE_SIZE, total);
        List<Object[]> page = rows.subList(from, to);
        appendHtml(divCplContainer, DashboardUiUtil.pageInfoBar(pageCpl, PAGE_SIZE, total));
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='overflow-x:auto;'>"
                + "<table class='dash-tbl' style='width:100%; border-collapse:collapse; font-size:11px;'>");
        sb.append("<tr style='background:#f8fafc;'>");
        for (String h : new String[]{"Mata Kuliah", "Prodi", "CPL yang Dibebankan (Bobot)"}) {
            sb.append("<th style='padding:7px 8px; text-align:left; color:#475569; font-weight:700;"
                    + " border-bottom:2px solid #e2e8f0;'>").append(esc(h)).append("</th>");
        }
        sb.append("</tr>");
        for (Object[] r : page) {
            String cplBobot = r[9].toString();
            sb.append("<tr style='border-bottom:1px solid #f1f5f9;'>");
            sb.append("<td style='padding:6px 8px; color:#1e293b;'><b>").append(esc(r[0].toString())).append("</b> ")
              .append(esc(r[1].toString())).append("</td>");
            sb.append("<td style='padding:6px 8px; color:#475569;'>").append(esc(r[2].toString())).append("</td>");
            sb.append("<td style='padding:6px 8px;'>");
            for (String cp : cplBobot.split(",")) {
                cp = cp.trim();
                if (cp.isEmpty()) continue;
                if (cp.contains(":")) {
                    String[] kv = cp.split(":", 2);
                    sb.append("<span style='display:inline-block; background:#dbeafe; border:1px solid #93c5fd;"
                            + " border-radius:4px; padding:2px 7px; margin:2px; font-size:10px; font-weight:700;'>")
                      .append(esc(kv[0].trim())).append(" <b>").append(esc(kv[1].trim())).append("%</b></span>");
                } else {
                    sb.append("<span style='display:inline-block; background:#f1f5f9; border:1px solid #cbd5e1;"
                            + " border-radius:4px; padding:2px 7px; margin:2px; font-size:10px;'>")
                      .append(esc(cp)).append("</span>");
                }
            }
            sb.append("</td></tr>");
        }
        sb.append("</table></div>");
        appendHtml(divCplContainer, sb.toString());
        renderPageNav(divCplContainer, pageCpl, total,
            new EventListener() { public void onEvent(Event e) throws Exception { pageCpl--; if (lastData != null) renderCplRekapTablePage(lastData); }},
            new EventListener() { public void onEvent(Event e) throws Exception { pageCpl++; if (lastData != null) renderCplRekapTablePage(lastData); }}
        );
    }

    // ====================================================================
    // Chart: CPL Coverage Radar + Distribution
    // ====================================================================

    /**
     * Kumpulan kartu visual ringkas Dasbor OBE (donut & batang) dalam grid
     * responsif — 2 kolom di komputer, menumpuk di HP. Semua HTML/CSS via
     * DashboardUiKit (reuse), tanpa JFreeChart.
     */
    private void renderObeExtraCharts(ObeData d) {
        if (d.totalMk <= 0) return;
        Div wrap = new Div();
        wrap.setStyle("display:grid; grid-template-columns:repeat(auto-fit,minmax(300px,1fr));"
                    + " gap:12px; margin:8px 0;");
        wrap.setParent(this);
        appendHtml(wrap, rpsDonutHtml(d));
        appendHtml(wrap, cqiDonutHtml(d));
        appendHtml(wrap, cplRankingHtml(d));
        appendHtml(wrap, obeReadinessHtml(d));
    }

    /** Donat: proporsi mata kuliah yang RPS-nya sudah vs belum dibuat. */
    private String rpsDonutHtml(ObeData d) {
        java.util.LinkedHashMap<String, Double> parts = new java.util.LinkedHashMap<String, Double>();
        parts.put("Sudah RPS", (double) d.sudahRps);
        parts.put("Belum RPS", (double) d.belumRps);
        return DashboardUiKit.donut("&#129518; Proporsi Kesiapan RPS OBE",
            "Bagian mata kuliah yang RPS OBE-nya sudah dibuat dibanding yang belum.",
            parts, false, emptyMsg());
    }

    /** Donat: seberapa banyak mata kuliah yang sudah mengisi rencana perbaikan (CQI). */
    private String cqiDonutHtml(ObeData d) {
        int lengkap = d.mkDenganCqiLengkap;
        int belumLengkap = Math.max(0, d.mkDenganCqi - d.mkDenganCqiLengkap);
        int belum = Math.max(0, d.totalMk - d.mkDenganCqi);
        java.util.LinkedHashMap<String, Double> parts = new java.util.LinkedHashMap<String, Double>();
        parts.put("CQI Lengkap", (double) lengkap);
        parts.put("CQI Belum Lengkap", (double) belumLengkap);
        parts.put("Belum Ada CQI", (double) belum);
        return DashboardUiKit.donut("&#128260; Tindak Lanjut Perbaikan (CQI)",
            "Banyaknya mata kuliah yang sudah mengisi rencana perbaikan mutu (CQI) secara lengkap.",
            parts, false, emptyMsg());
    }

    /** Batang: peringkat CPL berdasarkan jumlah mata kuliah yang membebankannya. */
    private String cplRankingHtml(ObeData d) {
        java.util.List<java.util.Map.Entry<String, Integer>> entries =
            new java.util.ArrayList<java.util.Map.Entry<String, Integer>>(d.cplCoverage.entrySet());
        java.util.Collections.sort(entries, new java.util.Comparator<java.util.Map.Entry<String, Integer>>() {
            public int compare(java.util.Map.Entry<String, Integer> a, java.util.Map.Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });
        java.util.LinkedHashMap<String, Integer> sorted = new java.util.LinkedHashMap<String, Integer>();
        int shown = 0;
        for (java.util.Map.Entry<String, Integer> e : entries) {
            if (shown++ >= 12) break;
            sorted.put(e.getKey(), e.getValue());
        }
        return DashboardUiKit.barList("&#128200; Peringkat Cakupan CPL",
            "CPL yang paling sering dibebankan ke mata kuliah berada di urutan teratas.",
            sorted, "#3b82f6", "MK", false, emptyMsg());
    }

    /** Batang: tahapan kelengkapan dokumen OBE dari total MK sampai RPS terisi. */
    private String obeReadinessHtml(ObeData d) {
        java.util.LinkedHashMap<String, Double> funnel = new java.util.LinkedHashMap<String, Double>();
        funnel.put("Total MK OBE", (double) d.totalMk);
        funnel.put("Punya CPL", (double) d.mkPunyaCpl);
        funnel.put("Punya CPMK", (double) d.mkDenganCpmk);
        funnel.put("Punya Sub-CPMK", (double) d.mkDenganSubCpmk);
        funnel.put("Sudah RPS", (double) d.sudahRps);
        return DashboardUiKit.barList("&#129513; Kelengkapan Dokumen OBE",
            "Tahapan dari seluruh mata kuliah OBE sampai RPS terisi; terlihat di tahap mana paling banyak tertinggal.",
            funnel, "#10b981", "MK", false, emptyMsg());
    }

    private void renderCplRadarChart(ObeData d) {
        Div panel = createPanel("&#128202; Grafik Cakupan CPL (Spider/Radar Chart)", this);

        // Collect CPL data sorted
        List<String> cplKeys  = new ArrayList<String>(d.cplCoverage.keySet());
        if (cplKeys.isEmpty()) return;

        // Cap at 16 CPLs for readability
        if (cplKeys.size() > 16) cplKeys = cplKeys.subList(0, 16);
        int n = cplKeys.size();
        int maxMk = 1;
        for (String k : cplKeys) if (d.cplCoverage.get(k) > maxMk) maxMk = d.cplCoverage.get(k);

        // Build SVG radar chart
        int svgSize = 320;
        int cx = svgSize / 2;
        int cy = svgSize / 2;
        int r  = 120;
        StringBuilder svg = new StringBuilder();
        svg.append("<svg viewBox='0 0 ").append(svgSize).append(" ").append(svgSize)
           .append("' xmlns='http://www.w3.org/2000/svg' style='max-width:320px;display:block;margin:0 auto;'>");

        // Draw grid circles (4 levels)
        for (int level = 1; level <= 4; level++) {
            int gr = r * level / 4;
            svg.append("<circle cx='").append(cx).append("' cy='").append(cy).append("' r='").append(gr)
               .append("' fill='none' stroke='#e2e8f0' stroke-width='1'/>");
        }

        // Draw axes
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n - Math.PI / 2;
            int x2 = (int)(cx + r * Math.cos(angle));
            int y2 = (int)(cy + r * Math.sin(angle));
            svg.append("<line x1='").append(cx).append("' y1='").append(cy)
               .append("' x2='").append(x2).append("' y2='").append(y2)
               .append("' stroke='#cbd5e1' stroke-width='1'/>");
        }

        // Draw target polygon (75% = 3 MKs if max is 4, or at maxMk level)
        StringBuilder targetPts = new StringBuilder();
        int targetR = r * 3 / 4;  // 75% of radius = target line
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n - Math.PI / 2;
            int x = (int)(cx + targetR * Math.cos(angle));
            int y = (int)(cy + targetR * Math.sin(angle));
            if (targetPts.length() > 0) targetPts.append(" ");
            targetPts.append(x).append(",").append(y);
        }
        svg.append("<polygon points='").append(targetPts)
           .append("' fill='#fef9c3' fill-opacity='0.5' stroke='#fcd34d' stroke-width='1.5' stroke-dasharray='4,2'/>");

        // Draw actual values polygon
        StringBuilder dataPts = new StringBuilder();
        for (int i = 0; i < n; i++) {
            String k  = cplKeys.get(i);
            int    mk = d.cplCoverage.get(k);
            int    vr = r * mk / maxMk;
            double angle = 2 * Math.PI * i / n - Math.PI / 2;
            int x = (int)(cx + vr * Math.cos(angle));
            int y = (int)(cy + vr * Math.sin(angle));
            if (dataPts.length() > 0) dataPts.append(" ");
            dataPts.append(x).append(",").append(y);
        }
        svg.append("<polygon points='").append(dataPts)
           .append("' fill='#3b82f6' fill-opacity='0.25' stroke='#2563eb' stroke-width='2'/>");

        // Draw data point dots and labels
        for (int i = 0; i < n; i++) {
            String k  = cplKeys.get(i);
            int    mk = d.cplCoverage.get(k);
            int    vr = r * mk / maxMk;
            double angle = 2 * Math.PI * i / n - Math.PI / 2;
            int px = (int)(cx + vr * Math.cos(angle));
            int py = (int)(cy + vr * Math.sin(angle));
            svg.append("<circle cx='").append(px).append("' cy='").append(py)
               .append("' r='4' fill='#2563eb' stroke='#ffffff' stroke-width='1.5'/>");
            // Label outside the chart
            int lx = (int)(cx + (r + 18) * Math.cos(angle));
            int ly = (int)(cy + (r + 18) * Math.sin(angle));
            String anchor = Math.cos(angle) < -0.1 ? "end" : (Math.cos(angle) > 0.1 ? "start" : "middle");
            svg.append("<text x='").append(lx).append("' y='").append(ly + 4)
               .append("' text-anchor='").append(anchor)
               .append("' font-size='9' font-weight='700' fill='#334155'>")
               .append(esc(k)).append("</text>");
        }

        // Legend
        svg.append("<g transform='translate(4,").append(svgSize - 30).append(")'>");
        svg.append("<rect x='0' y='0' width='10' height='10' fill='#2563eb' fill-opacity='0.5'/>");
        svg.append("<text x='14' y='9' font-size='9' fill='#475569'>MK yang memuat CPL</text>");
        svg.append("<rect x='110' y='0' width='10' height='10' fill='#fcd34d' fill-opacity='0.7'/>");
        svg.append("<text x='124' y='9' font-size='9' fill='#475569'>Target 75%</text>");
        svg.append("</g>");

        svg.append("</svg>");

        // Bar chart (horizontal) alongside radar
        StringBuilder bars = new StringBuilder();
        bars.append("<div style='overflow:auto; max-height:280px; flex:1; min-width:200px;'>");
        bars.append("<div style='font-size:11px; font-weight:700; color:#475569; margin-bottom:8px;'>"
                + "Distribusi MK per CPL</div>");
        for (String k : cplKeys) {
            int mk   = d.cplCoverage.get(k);
            int pct  = mk * 100 / maxMk;
            String col = mk >= maxMk * 3 / 4 ? "#22c55e" : (mk >= maxMk / 2 ? "#f59e0b" : "#ef4444");
            double bobot = d.cplBobotSum.containsKey(k) ? d.cplBobotSum.get(k) : 0.0;
            bars.append("<div style='margin:5px 0;'>");
            bars.append("<div style='display:flex; justify-content:space-between; margin-bottom:2px;'>");
            bars.append("<span style='font-size:11px; font-weight:700; color:#334155;'>").append(esc(k)).append("</span>");
            bars.append("<span style='font-size:11px; color:#1e293b;'>").append(mk).append(" MK");
            if (bobot > 0) bars.append(" &nbsp;<span style='color:#64748b; font-size:10px;'>&Sigma;")
                              .append(String.format("%.0f", bobot)).append("%</span>");
            bars.append("</span></div>");
            bars.append("<div style='height:10px; border-radius:5px; background:#e2e8f0;'>");
            bars.append("<div style='height:10px; border-radius:5px; background:").append(col)
                .append("; width:").append(pct).append("%;'></div></div></div>");
        }
        bars.append("</div>");

        appendHtml(panel,
            "<div style='display:flex; gap:16px; flex-wrap:wrap; align-items:flex-start;'>"
            + svg
            + bars
            + "</div>"
            + "<div style='font-size:10px; color:#94a3b8; margin-top:8px;'>"
            + "Radar menunjukkan cakupan setiap CPL di seluruh MK OBE. Area kuning = target &ge;75% dari MK yang memuat CPL terbanyak.</div>");
    }

    // ====================================================================
    // Render sections — Loop 1: CPMK & Sub-CPMK
    // ====================================================================

    private void renderCpmkStatsCards(ObeData d) {
        appendHtml(this,
            "<div style='font-size:13px; font-weight:800; color:#0f172a; margin:16px 0 10px;"
            + " padding-bottom:8px; border-bottom:2px solid #e2e8f0;'>"
            + "&#128203; Loop 1 &mdash; Monitoring CPMK &amp; Sub-CPMK</div>");

        Div row = new Div();
        row.setStyle("display:flex; gap:10px; flex-wrap:wrap; margin-bottom:14px;");
        row.setParent(this);
        metricCard(row, "MK punya CPMK",     d.mkDenganCpmk,    "Mata kuliah yang ada CPMK-nya",          "#fef3c7", "#78350f", "#d97706");
        metricCard(row, "MK punya Sub-CPMK", d.mkDenganSubCpmk, "CPMK minimal 1 Sub-CPMK terdefinisi",   "#ecfdf5", "#065f46", "#059669");
        metricCard(row, "Total CPMK unik",   d.totalCpmkUnik,   "Jumlah CPMK unik dari semua MK",        "#eff6ff", "#1e3a8a", "#3b82f6");
        metricCard(row, "Total Sub-CPMK",    d.totalSubCpmkUnik,"Total Sub-CPMK terdefinisi di formula",  "#fdf4ff", "#581c87", "#a855f7");
    }

    private void renderCpmkPerMkTable(ObeData d) {
        Div panel = createPanel("&#128203; Detail CPMK &amp; Sub-CPMK per Mata Kuliah (Loop 1)", this);
        if (d.cpmkPerMk.isEmpty()) {
            appendHtml(panel, "<div style='color:#94a3b8; font-size:12px; padding:14px;'>"
                + "Belum ada CPMK yang dipetakan ke mata kuliah. "
                + "Petakan CPMK ke MK melalui menu <b>CPMK &rarr; Relasi CPMK ke MK</b>.</div>");
            return;
        }
        divCpmkContainer = new Div();
        divCpmkContainer.setParent(panel);
        renderCpmkPerMkPage(d);
    }

    private void renderCpmkPerMkPage(ObeData d) {
        if (divCpmkContainer == null) return;
        Common.clear(divCpmkContainer);
        List<String> keys = filteredCpmkMkKeys(d);
        int total = keys.size();
        if (total == 0) { appendHtml(divCpmkContainer, emptyMsg()); return; }
        int from = pageCpmk * PAGE_SIZE;
        if (from >= total) { pageCpmk = 0; from = 0; }
        int to = Math.min(from + PAGE_SIZE, total);
        List<String> pageKeys = keys.subList(from, to);
        appendHtml(divCpmkContainer, DashboardUiUtil.pageInfoBar(pageCpmk, PAGE_SIZE, total));
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='overflow-x:auto;'>"
                + "<table class='dash-tbl' style='width:100%; border-collapse:collapse; font-size:11px;'>");
        sb.append("<tr style='background:#fef9c3;'>");
        for (String h : new String[]{"Mata Kuliah", "Prodi", "Kode CPMK", "Nama CPMK", "Bobot", "Min. Capaian", "Sub-CPMK"}) {
            sb.append("<th style='padding:7px 8px; text-align:left; color:#713f12; font-weight:700;"
                    + " border-bottom:2px solid #fde68a; white-space:nowrap;'>").append(esc(h)).append("</th>");
        }
        sb.append("</tr>");
        boolean zebra = false;
        for (String mkKode : pageKeys) {
            List<Object[]> cpmkRows = d.cpmkPerMk.get(mkKode);
            if (cpmkRows == null) continue;
            String mkNama = nvl(d.mkNamaByKode.get(mkKode));
            String prodi  = nvl(d.mkProdiByKode.get(mkKode));
            String rowBg  = zebra ? "#fafafa" : "#ffffff";
            zebra = !zebra;
            int rowspan = cpmkRows.size();
            boolean firstRow = true;
            for (Object[] cr : cpmkRows) {
                String kode    = (String)  cr[0];
                String nama    = (String)  cr[1];
                double bobot   = (Double)  cr[2];
                double minimal = (Double)  cr[3];
                int subCount   = (Integer) cr[4];
                sb.append("<tr style='border-bottom:1px solid #fde68a; background:").append(rowBg).append(";'>");
                if (firstRow) {
                    sb.append("<td rowspan='").append(rowspan).append("'"
                        + " style='padding:6px 8px; font-weight:700; color:#1e293b; vertical-align:top;"
                        + " border-left:3px solid #f59e0b;'>"
                        + "<b>").append(esc(mkKode)).append("</b><br>"
                        + "<span style='font-weight:400; color:#64748b;'>").append(esc(mkNama)).append("</span></td>");
                    sb.append("<td rowspan='").append(rowspan).append("'"
                        + " style='padding:6px 8px; color:#475569; vertical-align:top;'>").append(esc(prodi)).append("</td>");
                    firstRow = false;
                }
                sb.append("<td style='padding:6px 8px; font-weight:600; color:#0f172a;'>").append(esc(kode)).append("</td>");
                sb.append("<td style='padding:6px 8px; color:#334155;'>").append(esc(nama)).append("</td>");
                String bobotStr = bobot > 0 ? String.format("%.0f%%", bobot) : "-";
                sb.append("<td style='padding:6px 8px; text-align:center; color:#1e293b;'>").append(bobotStr).append("</td>");
                String minStr = minimal > 0 ? String.format("%.0f%%", minimal) : "-";
                sb.append("<td style='padding:6px 8px; text-align:center; color:#64748b;'>").append(minStr).append("</td>");
                if (subCount > 0) {
                    sb.append("<td style='padding:6px 8px; text-align:center;'>"
                        + "<span style='background:#d1fae5; color:#065f46; border-radius:999px;"
                        + " padding:2px 8px; font-size:10px; font-weight:700;'>")
                      .append(subCount).append(" Sub</span></td>");
                } else {
                    sb.append("<td style='padding:6px 8px; text-align:center;'>"
                        + "<span style='background:#fee2e2; color:#991b1b; border-radius:999px;"
                        + " padding:2px 8px; font-size:10px;'>Belum</span></td>");
                }
                sb.append("</tr>");
            }
        }
        sb.append("</table></div>");
        appendHtml(divCpmkContainer, sb.toString());
        renderPageNav(divCpmkContainer, pageCpmk, total,
            new EventListener() { public void onEvent(Event e) throws Exception { pageCpmk--; if (lastData != null) renderCpmkPerMkPage(lastData); }},
            new EventListener() { public void onEvent(Event e) throws Exception { pageCpmk++; if (lastData != null) renderCpmkPerMkPage(lastData); }}
        );
    }

    // ====================================================================
    // Render sections — Loop 2: CPL per Prodi
    // ====================================================================

    private void renderCplPerProdi(ObeData d) {
        appendHtml(this,
            "<div style='font-size:13px; font-weight:800; color:#0f172a; margin:16px 0 10px;"
            + " padding-bottom:8px; border-bottom:2px solid #e2e8f0;'>"
            + "&#127775; Loop 2 &mdash; Monitoring CPL per Program Studi</div>");

        if (d.cplPerJurusan.isEmpty()) {
            Div panel = createPanel("CPL per Program Studi", this);
            appendHtml(panel, "<div style='color:#94a3b8; font-size:12px; padding:14px;'>"
                + "Belum ada CPL yang terdefinisi untuk program studi ini.</div>");
            return;
        }

        for (Map.Entry<Long, List<Object[]>> entry : d.cplPerJurusan.entrySet()) {
            Long jid = entry.getKey();
            String namaProdi = nvl(d.namaJurusan.get(jid));
            List<Object[]> cplRows = entry.getValue();

            Div panel = createPanel("&#127775; CPL Prodi: " + esc(namaProdi), this);
            StringBuilder sb = new StringBuilder();
            sb.append("<div style='overflow-x:auto;'>"
                    + "<table style='width:100%; border-collapse:collapse; font-size:11px;'>");
            sb.append("<tr style='background:#f0fdf4;'>");
            for (String h : new String[]{"Kode CPL", "Nama CPL", "MK Berkontribusi", "CPMK", "Profil Lulusan Terkait"}) {
                sb.append("<th style='padding:7px 8px; text-align:left; color:#14532d; font-weight:700;"
                        + " border-bottom:2px solid #bbf7d0; white-space:nowrap;'>").append(esc(h)).append("</th>");
            }
            sb.append("</tr>");

            int totalMkContrib = 0;
            for (Object[] cr : cplRows) {
                String kode     = (String)  cr[0];
                String nama     = (String)  cr[1];
                int mkCount     = (Integer) cr[2];
                String plKode   = (String)  cr[3];
                String plNama   = (String)  cr[4];
                int cpmkCount   = (Integer) cr[5];
                totalMkContrib += mkCount;

                String mkBg  = mkCount > 0 ? "#dcfce7" : "#fee2e2";
                String mkClr = mkCount > 0 ? "#166534" : "#991b1b";

                sb.append("<tr style='border-bottom:1px solid #e2e8f0;'>");
                sb.append("<td style='padding:6px 8px; font-weight:700; color:#15803d; white-space:nowrap;'>")
                  .append(esc(kode)).append("</td>");
                sb.append("<td style='padding:6px 8px; color:#334155;'>").append(esc(nama)).append("</td>");
                sb.append("<td style='padding:6px 8px; text-align:center;'>"
                    + "<span style='background:").append(mkBg).append("; color:").append(mkClr)
                  .append("; border-radius:999px; padding:2px 8px; font-size:10px; font-weight:700;'>")
                  .append(mkCount).append(" MK</span></td>");
                sb.append("<td style='padding:6px 8px; text-align:center; color:#1e293b;'>")
                  .append(cpmkCount > 0 ? cpmkCount : "-").append("</td>");
                if (!plKode.isEmpty()) {
                    sb.append("<td style='padding:6px 8px;'>"
                        + "<span style='background:#fef9c3; color:#713f12; border-radius:4px;"
                        + " padding:2px 7px; font-size:10px; font-weight:700;'>")
                      .append(esc(plKode)).append("</span>"
                        + " <span style='color:#78350f; font-size:10px;'>")
                      .append(esc(plNama)).append("</span></td>");
                } else {
                    sb.append("<td style='padding:6px 8px; color:#94a3b8; font-size:10px;'>-</td>");
                }
                sb.append("</tr>");
            }
            sb.append("<tr style='background:#f0fdf4; font-weight:700;'>");
            sb.append("<td colspan='2' style='padding:6px 8px; color:#14532d;'>Total " + cplRows.size() + " CPL</td>");
            sb.append("<td style='padding:6px 8px; text-align:center; color:#14532d;'>" + totalMkContrib + " MK</td>");
            sb.append("<td colspan='2'></td></tr>");
            sb.append("</table></div>");
            appendHtml(panel, sb.toString());
        }
    }

    // ====================================================================
    // Render sections — Loop 3: Profil Lulusan
    // ====================================================================

    private void renderProfilLulusanCoverage(ObeData d) {
        appendHtml(this,
            "<div style='font-size:13px; font-weight:800; color:#0f172a; margin:16px 0 10px;"
            + " padding-bottom:8px; border-bottom:2px solid #e2e8f0;'>"
            + "&#127891; Loop 3 &mdash; Monitoring Profil Lulusan (PL)</div>");

        if (d.plPerJurusan.isEmpty()) {
            Div panel = createPanel("Profil Lulusan", this);
            appendHtml(panel, "<div style='color:#94a3b8; font-size:12px; padding:14px;'>"
                + "Belum ada Profil Lulusan yang terhubung dengan CPL. "
                + "Hubungkan PL ke CPL via menu <b>CPL &rarr; Relasi CPL ke Profil Lulusan</b>.</div>");
            return;
        }

        for (Map.Entry<Long, List<Object[]>> entry : d.plPerJurusan.entrySet()) {
            Long jid = entry.getKey();
            String namaProdi = nvl(d.namaJurusan.get(jid));
            List<Object[]> plRows = entry.getValue();

            Div panel = createPanel("&#127891; Profil Lulusan Prodi: " + esc(namaProdi), this);

            int totalPl    = plRows.size();
            int plTercakup = 0;
            for (Object[] pr : plRows) if ((Integer)pr[2] > 0) plTercakup++;
            int pctCover = totalPl == 0 ? 0 : plTercakup * 100 / totalPl;

            // Summary bar
            String coverColor = pctCover >= 80 ? "#16a34a" : (pctCover >= 50 ? "#2563eb" : (pctCover >= 25 ? "#d97706" : "#dc2626"));
            appendHtml(panel,
                "<div style='margin-bottom:12px;'>"
                + "<div style='display:flex; justify-content:space-between; margin-bottom:4px;'>"
                + "<span style='font-size:11px; color:#475569;'>Cakupan PL (" + plTercakup + " dari " + totalPl + " terhubung CPL)</span>"
                + "<span style='font-size:11px; font-weight:700; color:" + coverColor + ";'>" + pctCover + "%</span></div>"
                + "<div style='height:10px; border-radius:5px; background:#e2e8f0;'>"
                + "<div style='height:10px; border-radius:5px; background:" + coverColor + "; width:" + pctCover + "%;'></div></div></div>");

            StringBuilder sb = new StringBuilder();
            sb.append("<div style='overflow-x:auto;'>"
                    + "<table style='width:100%; border-collapse:collapse; font-size:11px;'>");
            sb.append("<tr style='background:#fffbeb;'>");
            for (String h : new String[]{"Kode PL", "Nama Profil Lulusan", "CPL Berkontribusi", "Status"}) {
                sb.append("<th style='padding:7px 8px; text-align:left; color:#78350f; font-weight:700;"
                        + " border-bottom:2px solid #fde68a;'>").append(esc(h)).append("</th>");
            }
            sb.append("</tr>");

            for (Object[] pr : plRows) {
                String kode  = (String)  pr[0];
                String nama  = (String)  pr[1];
                int cplCount = (Integer) pr[2];

                String status, stBg, stClr;
                if (cplCount >= 3)      { status = "Tercakup baik";   stBg = "#dcfce7"; stClr = "#166534"; }
                else if (cplCount >= 1) { status = "Tercakup";        stBg = "#fef9c3"; stClr = "#713f12"; }
                else                    { status = "Belum ada CPL";   stBg = "#fee2e2"; stClr = "#991b1b"; }

                sb.append("<tr style='border-bottom:1px solid #fde68a;'>");
                sb.append("<td style='padding:6px 8px; font-weight:700; color:#b45309;'>").append(esc(kode)).append("</td>");
                sb.append("<td style='padding:6px 8px; color:#334155;'>").append(esc(nama)).append("</td>");
                sb.append("<td style='padding:6px 8px; text-align:center; color:#1e293b;'>")
                  .append(cplCount > 0 ? cplCount + " CPL" : "-").append("</td>");
                sb.append("<td style='padding:6px 8px;'><span style='border-radius:999px; padding:2px 8px;"
                    + " font-size:10px; font-weight:700; background:").append(stBg).append("; color:").append(stClr).append(";'>")
                  .append(status).append("</span></td>");
                sb.append("</tr>");
            }
            sb.append("</table></div>");
            appendHtml(panel, sb.toString());
        }
    }

    // ====================================================================
    // Render sections — PIKOBE Readiness Score
    // ====================================================================

    private void renderPikobeScore(ObeData d) {
        if (d.totalMk == 0) return;

        appendHtml(this,
            "<div style='font-size:13px; font-weight:800; color:#0f172a; margin:16px 0 10px;"
            + " padding-bottom:8px; border-bottom:2px solid #e2e8f0;'>"
            + "&#127942; Skor PIKOBE &mdash; Indeks Kesiapan OBE (Skala 0&ndash;400)</div>");

        // ── PLAN (maks 100): Desain & Keselarasan Kurikulum ──────────────
        int planCpl    = d.totalMk == 0 ? 0 : d.mkPunyaCpl      * 25 / d.totalMk;
        int planCpmk   = d.totalMk == 0 ? 0 : d.mkDenganCpmk    * 25 / d.totalMk;
        int planSubCpm = d.totalMk == 0 ? 0 : d.mkDenganSubCpmk * 25 / d.totalMk;
        int mkDenganBobot = 0;
        for (Object[] r : d.rekapMk) if (!r[9].toString().isEmpty()) mkDenganBobot++;
        int planBobot  = d.totalMk == 0 ? 0 : mkDenganBobot * 25 / d.totalMk;
        int planTotal  = planCpl + planCpmk + planSubCpm + planBobot;  // maks 100

        // ── DO (maks 100): Proses & Metode Pembelajaran ───────────────────
        // RPS terisi = bukti perencanan pembelajaran sudah didokumentasikan
        int doRps100  = d.totalMk == 0 ? 0 : d.sudahRps * 50 / d.totalMk;   // maks 50
        // CPMK terdefinisi = bukti strategi pembelajaran selaras CPL
        int doCpmk50  = d.totalMk == 0 ? 0 : d.mkDenganCpmk * 50 / d.totalMk; // maks 50
        int doTotal   = doRps100 + doCpmk50;  // maks 100

        // ── CHECK (maks 100): Penilaian/Asesmen CPMK ─────────────────────
        // Proxy: MK yang sudah mengisi bobot CPL (artinya instrumen penilaian sudah dirancang)
        int checkBobot = d.totalMk == 0 ? 0 : mkDenganBobot * 50 / d.totalMk;  // maks 50
        // MK yang sudah punya Sub-CPMK (indikator terformulasi = instrumen siap)
        int checkSub   = d.totalMk == 0 ? 0 : d.mkDenganSubCpmk * 50 / d.totalMk; // maks 50
        int checkTotal = checkBobot + checkSub;  // maks 100

        // ── ACT (maks 100): Evaluasi & Perbaikan Berkelanjutan (CQI) ─────
        int actCqi     = d.totalMk == 0 ? 0 : d.mkDenganCqi * 50 / d.totalMk;       // maks 50
        int actLengkap = d.totalMk == 0 ? 0 : d.mkDenganCqiLengkap * 50 / d.totalMk; // maks 50
        int actTotal   = actCqi + actLengkap;

        // ── Total PIKOBE 0–400 ────────────────────────────────────────────
        int totalScore = planTotal + doTotal + checkTotal + actTotal;

        // ── Tier thresholds (skala 400) ───────────────────────────────────
        // Platinum: >350 (CHECK+ACT ≥ 151); Gold: 301-350 (CHECK+ACT ≥ 125);
        // Silver: 201-300 (CHECK ≥ 90); Bronze: 100-200
        String tier, tierColor, tierBg;
        if (totalScore > 350 && (checkTotal + actTotal) >= 151)
            { tier = "&#127775; Platinum"; tierColor = "#1e3a8a"; tierBg = "#dbeafe"; }
        else if (totalScore >= 301 && (checkTotal + actTotal) >= 125)
            { tier = "&#129351; Gold";     tierColor = "#78350f"; tierBg = "#fef3c7"; }
        else if (totalScore >= 201 && checkTotal >= 90)
            { tier = "&#129354; Silver";   tierColor = "#1f2937"; tierBg = "#f3f4f6"; }
        else if (totalScore >= 100)
            { tier = "&#129353; Bronze";   tierColor = "#7c2d12"; tierBg = "#fff7ed"; }
        else
            { tier = "&#128308; Rintisan"; tierColor = "#7f1d1d"; tierBg = "#fef2f2"; }

        Div panel = createPanel("&#127942; Skor PIKOBE Kesiapan OBE", this);
        appendHtml(panel,
            "<div style='display:flex; gap:16px; flex-wrap:wrap; align-items:flex-start;'>"

            // Left: big score (skala 400)
            + "<div style='background:" + tierBg + "; border-radius:16px; padding:20px 24px;"
            +    " text-align:center; min-width:150px;'>"
            + "<div style='font-size:40px; font-weight:900; color:" + tierColor + ";'>" + totalScore + "</div>"
            + "<div style='font-size:11px; color:" + tierColor + "; font-weight:700; margin-top:4px;'>"
            + "dari 400</div>"
            + "<div style='font-size:14px; font-weight:900; color:" + tierColor + "; margin-top:8px;'>"
            + tier + "</div>"
            + "<div style='font-size:10px; color:" + tierColor + "; margin-top:6px; opacity:0.8;'>"
            + "PLAN:" + planTotal + " DO:" + doTotal + "<br>CHECK:" + checkTotal + " ACT:" + actTotal + "</div>"
            + "</div>"

            // Right: breakdown per domain
            + "<div style='flex:1; min-width:260px;'>"
            + "<div style='font-size:11px; font-weight:700; color:#3b82f6; margin-bottom:6px;'>"
            + "PLAN &mdash; Desain &amp; Keselarasan Kurikulum (maks 100)</div>"
            + pikobeBar100("CPL terpetakan di MK",      planCpl,    "#3b82f6", 100)
            + pikobeBar100("CPMK terdefinisi per MK",   planCpmk,   "#6366f1", 100)
            + pikobeBar100("Sub-CPMK terformulasi",     planSubCpm, "#8b5cf6", 100)
            + pikobeBar100("Bobot CPL terisi",          planBobot,  "#06b6d4", 100)
            + "<div style='font-size:11px; font-weight:700; color:#22c55e; margin:10px 0 6px;'>"
            + "DO &mdash; Proses &amp; Metode Pembelajaran (maks 100)</div>"
            + pikobeBar100("RPS OBE terisi",            doRps100,   "#22c55e", 50)
            + pikobeBar100("CPMK selaras strategi",     doCpmk50,   "#16a34a", 50)
            + "<div style='font-size:11px; font-weight:700; color:#f59e0b; margin:10px 0 6px;'>"
            + "CHECK &mdash; Penilaian &amp; Asesmen CPMK (maks 100)</div>"
            + pikobeBar100("Instrumen bobot CPL",       checkBobot, "#f59e0b", 50)
            + pikobeBar100("Indikator Sub-CPMK ada",   checkSub,   "#d97706", 50)
            + "<div style='font-size:11px; font-weight:700; color:#f43f5e; margin:10px 0 6px;'>"
            + "ACT &mdash; Evaluasi &amp; CQI (maks 100)</div>"
            + pikobeBar100("MK dengan CQI tercatat",          actCqi,     "#f43f5e", 50)
            + pikobeBar100("MK dengan CQI lengkap (mslh+rncn)", actLengkap, "#be123c", 50)
            + (d.mkDenganCqi == 0
               ? "<div style='font-size:10px; color:#94a3b8; font-style:italic; margin:2px 0 6px;'>"
                 + "&#128295; Belum ada CQI. Buka <b>Portofolio MK</b> &rarr; <b>Edit CQI</b> untuk setiap mata kuliah.</div>"
               : "")
            + "<div style='font-size:10px; color:#94a3b8; margin-top:10px; padding-top:8px; border-top:1px solid #e2e8f0;'>"
            + "Tier PIKOBE: Rintisan &lt;100 | Bronze 100&ndash;200 | Silver 201&ndash;300 (CHECK&ge;90) | "
            + "Gold 301&ndash;350 (CHECK+ACT&ge;125) | Platinum &gt;350 (CHECK+ACT&ge;151)"
            + "</div>"
            + "</div>"

            + "</div>"

            // Improvement suggestions
            + buildPikobeRecommendations(d, planCpl, planCpmk, planSubCpm, planBobot, doRps100 * 2));

        appendHtml(panel, "");
    }

    private String pikobeBar(String label, int val, String color) {
        return pikobeBar100(label, val, color, 100);
    }

    private String pikobeBar100(String label, int val, String color, int maxVal) {
        int pct = maxVal <= 0 ? 0 : Math.min(100, val * 100 / maxVal);
        String display = maxVal == 100 ? val + " pt" : val + "/" + maxVal + " pt";
        return "<div style='margin:4px 0;'>"
            + "<div style='display:flex; justify-content:space-between; margin-bottom:2px;'>"
            + "<span style='font-size:11px; color:#334155;'>" + esc(label) + "</span>"
            + "<span style='font-size:11px; font-weight:700; color:" + color + ";'>" + display + "</span></div>"
            + "<div style='height:7px; border-radius:4px; background:#e2e8f0;'>"
            + "<div style='height:7px; border-radius:4px; background:" + color + "; width:" + pct + "%;'></div></div></div>";
    }

    private String buildPikobeRecommendations(ObeData d, int planCpl, int planCpmk,
                                               int planSubCpm, int planBobot, int doRps) {
        List<String> recs = new ArrayList<String>();
        if (planCpl < 100)    recs.add("Petakan <b>CPL</b> ke seluruh mata kuliah OBE (" + (d.totalMk - d.mkPunyaCpl) + " MK belum).");
        if (planCpmk < 100)   recs.add("Buat dan petakan <b>CPMK</b> ke MK (" + (d.totalMk - d.mkDenganCpmk) + " MK belum).");
        if (planSubCpm < 100) recs.add("Isi formula <b>Sub-CPMK</b> di setiap CPMK (" + (d.totalMk - d.mkDenganSubCpmk) + " MK belum).");
        if (planBobot < 100) {
            int mkDenganBobot = 0;
            for (Object[] r : d.rekapMk) if (!r[9].toString().isEmpty()) mkDenganBobot++;
            recs.add("Isi kolom <b>Bobot CPL</b> di form RPS OBE (" + (d.totalMk - mkDenganBobot) + " MK belum).");
        }
        if (doRps < 100)      recs.add("Lengkapi <b>RPS</b> untuk " + d.belumRps + " mata kuliah yang belum mengisi.");
        if (d.mkDenganCqi < d.totalMk)
            recs.add("Catat <b>CQI</b> (Evaluasi &amp; Perbaikan) via Portofolio MK &rarr; Edit CQI untuk "
                    + (d.totalMk - d.mkDenganCqi) + " MK yang belum.");

        if (recs.isEmpty()) return "<div style='background:#dcfce7; border-radius:12px; padding:12px 16px;"
            + " font-size:12px; color:#166534; font-weight:700; margin-top:10px;'>"
            + "&#10003; Semua indikator OBE sudah terpenuhi! Siap untuk pengukuran pencapaian CPMK/CPL.</div>";

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background:#fff7ed; border-radius:12px; padding:12px 16px;"
                + " font-size:11px; color:#7c2d12; margin-top:10px;'>"
                + "<div style='font-weight:700; margin-bottom:8px;'>&#128161; Rekomendasi Perbaikan:</div>");
        for (String r : recs)
            sb.append("<div style='margin:4px 0;'>&#8226; ").append(r).append("</div>");
        sb.append("</div>");
        return sb.toString();
    }

    // ====================================================================
    // Helpers
    // ====================================================================

    private void renderPageNav(Div container, int currentPage, int total,
            EventListener prevEv, EventListener nextEv) {
        DashboardUiUtil.renderPageNav(container, currentPage, PAGE_SIZE, total, prevEv, nextEv);
    }

    private Div createPanel(String title, Div parent) {
        Div panel = new Div();
        panel.setStyle("background:#fff; border-radius:18px; padding:18px 20px;"
                     + " box-shadow:0 2px 12px rgba(0,0,0,.06); margin-bottom:12px;");
        panel.setParent(parent);
        appendHtml(panel,
            "<div style='font-size:13px; font-weight:800; color:#0f172a; margin-bottom:12px;"
            + " padding-bottom:8px; border-bottom:2px solid #f1f5f9;'>" + title + "</div>");
        return panel;
    }

    private String emptyMsg() {
        return DashboardUiUtil.emptyHtml("Belum ada data pada filter saat ini.");
    }

    private static void appendHtml(org.zkoss.zk.ui.Component parent, String html) {
        new org.zkoss.zul.Html(html).setParent(parent);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    /**
     * Apakah pengguna saat ini boleh melihat tombol "Upload OBE (Excel)".
     * Dikendalikan konfigurasi hak_akses_upload_obe_excel (daftar roleId dipisah
     * koma; default am,adAkdmk,Akademik,admfak,admprd). Mendukung "*"/"semua".
     * Download tetap selalu tersedia; hanya Upload yang dibatasi.
     */
    private boolean bolehUploadObe() {
        try {
            String daftar = Common.getKonfigurasi("hak_akses_upload_obe_excel",
                    "am,adAkdmk,Akademik,admfak,admprd").getNilai();
            if (daftar == null || daftar.trim().length() == 0) {
                daftar = "am,adAkdmk,Akademik,admfak,admprd";
            }
            String roleId = (tbmuser == null || tbmuser.hakAkses() == null)
                    ? "" : nvl(tbmuser.hakAkses().getRoleId()).trim();
            if (roleId.length() == 0) {
                return false;
            }
            String[] arr = daftar.split(",");
            for (int i = 0; i < arr.length; i++) {
                String r = arr[i] == null ? "" : arr[i].trim();
                if (r.length() == 0) {
                    continue;
                }
                if ("*".equals(r) || "semua".equalsIgnoreCase(r) || r.equalsIgnoreCase(roleId)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private Jurusan getRoleJurusan() {
        if (tbmuser == null) return null;
        ais.database.model.Tbmrole role = tbmuser.hakAkses();
        if (role == null) return null;
        return role.getJurusan();
    }

    private ais.database.model.Fakultas getRoleFakultas() {
        if (tbmuser == null) return null;
        ais.database.model.Tbmrole role = tbmuser.hakAkses();
        if (role == null) return null;
        if (role.getJurusan() != null) return null;
        return role.getFakultas();
    }

    /** Tampilkan ringkasan CQI (analisis dosen + evaluasi admin) per mata kuliah di dasbor. */
    @SuppressWarnings("unchecked")
    private void renderCqiAnalysisSummary(Div parent, ObeData d) {
        if (d.rekapMk.isEmpty()) return;
        // Hitung berapa yang sudah punya CQI
        int punya = 0;
        for (Object[] r : d.rekapMk) {
            Long pid = r.length > 12 && r[12] != null ? (Long) r[12] : null;
            if (pid != null) {
                String st = d.cqiStatusByPerkId.containsKey(pid) ? d.cqiStatusByPerkId.get(pid) : "";
                if (!st.isEmpty()) punya++;
            }
        }
        int totalMk = d.rekapMk.size();
        int pct = totalMk == 0 ? 0 : punya * 100 / totalMk;

        Div panel = createPanel("&#128295; Analisis Dosen & CQI per Mata Kuliah", parent);
        appendHtml(panel,
            "<div style='font-size:12px; color:#64748b; margin-bottom:12px; padding:10px 14px;"
            + " background:#fff7ed; border-radius:10px; border:1px solid #fed7aa;'>"
            + "<b>CQI (Continuous Quality Improvement)</b> adalah catatan analisis dosen atas ketercapaian CPMK "
            + "setiap semester — berisi identifikasi masalah, analisis penyebab, dan rencana tindak lanjut perbaikan. "
            + "Admin dan Kaprodi dapat meninjau dan menambahkan evaluasi di setiap baris Rekap RPS. "
            + "<br>Status: <b>" + punya + "/" + totalMk + " MK</b> sudah punya CQI (" + pct + "%).</div>");

        // Tabel ringkasan CQI
        Grid grid = new Grid();
        grid.setWidth("100%");
        grid.setStyle("border:1px solid #e5e7eb; border-radius:12px; font-size:11px;");
        grid.setMold("paging");
        grid.setPageSize(10);
        grid.setParent(panel);

        Columns cols = new Columns();
        cols.setParent(grid);
        String[] headers = {"Mata Kuliah", "Prodi", "Dosen", "Status CQI", "Aksi"};
        String[] wids    = {"30%", "20%", "20%", "15%", "15%"};
        boolean[] cntr   = {false, false, false, true, true};
        for (int i = 0; i < headers.length; i++) {
            Column c = new Column(headers[i]);
            c.setWidth(wids[i]);
            if (cntr[i]) c.setAlign("center");
            c.setParent(cols);
        }

        Rows rows = new Rows();
        rows.setParent(grid);

        for (Object[] r : d.rekapMk) {
            final Long pid = r.length > 12 && r[12] != null ? (Long) r[12] : null;
            final Long kid = r.length > 11 && r[11] != null ? (Long) r[11] : null;
            String st = pid != null && d.cqiStatusByPerkId.containsKey(pid)
                        ? d.cqiStatusByPerkId.get(pid) : "";
            String bg, clr, txt;
            if ("ADMIN_OK".equals(st)) {
                bg = "#bbf7d0"; clr = "#14532d"; txt = "&#10004; Disetujui";
            } else if ("ADMIN_REVISI".equals(st)) {
                bg = "#fecaca"; clr = "#7f1d1d"; txt = "&#9888; Revisi Admin";
            } else if ("OK".equals(st)) {
                bg = "#dcfce7"; clr = "#166534"; txt = "&#10003; Lengkap";
            } else if ("PARTIAL".equals(st)) {
                bg = "#fef3c7"; clr = "#78350f"; txt = "&#9888; Sebagian";
            } else {
                bg = "#fee2e2"; clr = "#991b1b"; txt = "&#10007; Belum";
            }

            Row row = new Row();
            row.setParent(rows);

            Html mkHtml = new Html("<div style='font-weight:600;color:#0f172a;'>" + esc(r[1].toString())
                + "</div><div style='font-size:10px;color:#94a3b8;'>" + esc(r[0].toString()) + "</div>");
            row.appendChild(mkHtml);
            row.appendChild(new Html("<span style='color:#475569;'>" + esc(r[2].toString()) + "</span>"));
            row.appendChild(new Html("<span style='color:#64748b;font-size:11px;'>" + esc(r[3].toString()) + "</span>"));
            row.appendChild(new Html("<span style='border-radius:999px;padding:2px 8px;font-size:10px;"
                + "font-weight:700;background:" + bg + ";color:" + clr + ";'>" + txt + "</span>"));

            // Tombol "Edit CQI" untuk perkuliahan ini
            MyToolbarbuttonConfig btnCqi = new MyToolbarbuttonConfig("Edit CQI", "/img/svg/edit.svg");
            btnCqi.setTooltiptext("Buka editor CQI untuk mata kuliah ini");
            btnCqi.setStyle("background:#c2410c;color:#fff;border-radius:6px;padding:3px 10px;border:0;font-size:11px;");
            if (pid != null) {
                btnCqi.addEventListener("onClick", new EventListener() {
                    public void onEvent(Event e) throws Exception {
                        bukaEditorCqiUntukPerkuliahan(pid, kid);
                    }
                });
            } else {
                btnCqi.setDisabled(true);
            }
            row.appendChild(btnCqi);
        }
    }

    /**
     * Buka editor CQI untuk perkuliahan tertentu (dari dasbor, bukan dari Nilai OBE).
     * Editor ini memungkinkan dosen mengisi/mengedit CQI per CPMK, dan admin mengisi evaluasi.
     */
    @SuppressWarnings("unchecked")
    private void bukaEditorCqiUntukPerkuliahan(final Long perkId, final Long kpmId) {
        try {
            Session sess = null;
            final Perkuliahan perk;
            try {
                sess = HibernateUtil.openSession();
                perk = (Perkuliahan) sess.get(Perkuliahan.class, perkId);
                if (perk == null) { return; }

                Matakuliah mk = perk.getMatakuliah();
                if (mk == null) { return; }

                // Load CPMK list
                final List<CapaianPembelajaranLulusan> cpmkList = new ArrayList<CapaianPembelajaranLulusan>();
                java.util.Set<Long> ids = new java.util.HashSet<Long>();
                if (mk.getCapaianPembelajaranLulusan() != null
                        && !mk.getCapaianPembelajaranLulusan().trim().isEmpty()) {
                    for (String idStr : mk.getCapaianPembelajaranLulusan().split(",")) {
                        try { ids.add(Long.parseLong(idStr.trim())); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:2350");}
                    }
                }
                if (!ids.isEmpty()) {
                    cpmkList.addAll((List<CapaianPembelajaranLulusan>)
                        sess.createCriteria(CapaianPembelajaranLulusan.class)
                            .add(Restrictions.in("id", ids))
                            .addOrder(Order.asc("kode")).list());
                }

                // Parse existing CQI JSON
                final java.util.Map<String, JSONObject> cqiMap = new java.util.LinkedHashMap<String, JSONObject>();
                String cqiRaw = perk.getCqiData();
                if (cqiRaw != null && !cqiRaw.trim().isEmpty()) {
                    try {
                        JSONArray arr = new JSONArray(cqiRaw);
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject en = arr.optJSONObject(i);
                            if (en != null && !en.optString("cpmk","").isEmpty())
                                cqiMap.put(en.optString("cpmk",""), en);
                        }
                    } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:2371");}
                }

                // Determine role
                String roleId = (tbmuser == null || tbmuser.hakAkses() == null)
                                ? "" : nvl(tbmuser.hakAkses().getRoleId()).trim();
                final boolean isAdmin = "am".equals(roleId) || "adAkdmk".equals(roleId)
                                     || "Akademik".equals(roleId) || "admfak".equals(roleId)
                                     || "admprd".equals(roleId) || "Kajur".equals(roleId);
                final boolean isDosen = tbmuser != null && tbmuser.ambilDosen() != null;
                final boolean bolehEdit = isDosen || isAdmin;

                // Build window
                final ais.ui.util.MyWindow win = new ais.ui.util.MyWindow();
                win.setTitle("CQI — " + nvl(mk.getNama()) + " | " + nvl(perk.getTahunAjaran()));
                win.setWidth("1050px");
                win.setHeight("92%");
                win.setClosable(true);
                win.setBorder("normal");
                org.zkoss.zk.ui.Page page = ais.ui.util.ZkCompat.getCurrentPage();
                if (page != null) page.getFirstRoot().appendChild(win);

                org.zkoss.zul.Borderlayout bl = new org.zkoss.zul.Borderlayout();
                ais.ui.util.ZkCompat.setFlex(bl, true);
                bl.setParent(win);

                org.zkoss.zul.Center center = new org.zkoss.zul.Center();
                center.setAutoscroll(true);
                center.setParent(bl);

                org.zkoss.zul.Vbox centerWrap = new org.zkoss.zul.Vbox();
                centerWrap.setWidth("100%");
                centerWrap.setVflex("1");
                centerWrap.setParent(center);

                // Info header
                appendHtml(centerWrap,
                    "<div style='font-family:system-ui,sans-serif;padding:10px 14px;background:#fff7ed;"
                    + "border-bottom:1px solid #fed7aa;font-size:12px;color:#78350f;'>"
                    + "<b>CQI (Continuous Quality Improvement)</b> — Loop 1 per Semester<br>"
                    + "Isi rencana perbaikan per CPMK. "
                    + (isAdmin ? "<span style='color:#1e40af;font-weight:700;'>&#128272; Mode Admin: dapat mengisi Evaluasi Admin.</span>" : "")
                    + "</div>");

                ais.ui.util.MyGrid grid = new ais.ui.util.MyGrid();
                grid.setWidth("100%");
                grid.setParent(centerWrap);

                org.zkoss.zul.Columns colsDef = new org.zkoss.zul.Columns();
                colsDef.setParent(grid);
                String[][] colDefs = {
                    {"CPMK","10%"}, {"Masalah / Gap",null}, {"Analisis Penyebab",null},
                    {"Rencana Tindak Lanjut",null}, {"PJ","9%"}, {"Target Waktu","10%"},
                    {"Status","8%"}, {"Evaluasi Admin","12%"}
                };
                for (int i = 0; i < colDefs.length; i++) {
                    String[] cd = colDefs[i];
                    ais.ui.util.MyColumnConfig c = new ais.ui.util.MyColumnConfig(cd[0]);
                    if (cd[1] != null) c.setWidth(cd[1]);
                    c.setParent(colsDef);
                }

                Rows rows = new Rows();
                rows.setParent(grid);

                final List<String> cpmkKodes = new ArrayList<String>();
                final List<ais.ui.util.MyTextbox> masalahList  = new ArrayList<ais.ui.util.MyTextbox>();
                final List<ais.ui.util.MyTextbox> analisisList = new ArrayList<ais.ui.util.MyTextbox>();
                final List<ais.ui.util.MyTextbox> rencanaList  = new ArrayList<ais.ui.util.MyTextbox>();
                final List<ais.ui.util.MyTextbox> pjList       = new ArrayList<ais.ui.util.MyTextbox>();
                final List<ais.ui.util.MyTextbox> targetList   = new ArrayList<ais.ui.util.MyTextbox>();
                final List<ais.ui.util.MyTextbox> statusList   = new ArrayList<ais.ui.util.MyTextbox>();
                final List<ais.ui.util.MyTextbox> adminKomList = new ArrayList<ais.ui.util.MyTextbox>();

                for (int ci = 0; ci < cpmkList.size(); ci++) {
                    CapaianPembelajaranLulusan cpmk = cpmkList.get(ci);
                    String kode = nvl(cpmk.getKode());
                    JSONObject entry = cqiMap.containsKey(kode) ? cqiMap.get(kode) : new JSONObject();
                    cpmkKodes.add(kode);

                    Row row = new Row();
                    row.setValign("top");
                    row.setParent(rows);

                    org.zkoss.zul.Vbox vb = new org.zkoss.zul.Vbox();
                    Label lKode = new Label(kode);
                    lKode.setStyle("font-weight:bold;color:#7c3aed;");
                    lKode.setParent(vb);
                    Label lNama = new Label(nvl(cpmk.getNama()));
                    lNama.setStyle("font-size:10px;color:#64748b;");
                    lNama.setParent(vb);
                    vb.setParent(row);

                    ais.ui.util.MyTextbox tbMasalah = new ais.ui.util.MyTextbox(entry.optString("masalah",""));
                    tbMasalah.setRows(2); tbMasalah.setWidth("95%");
                    tbMasalah.setDisabled(!bolehEdit);
                    masalahList.add(tbMasalah); tbMasalah.setParent(row);

                    ais.ui.util.MyTextbox tbAnalisis = new ais.ui.util.MyTextbox(entry.optString("analisis",""));
                    tbAnalisis.setRows(2); tbAnalisis.setWidth("95%");
                    tbAnalisis.setDisabled(!bolehEdit);
                    analisisList.add(tbAnalisis); tbAnalisis.setParent(row);

                    ais.ui.util.MyTextbox tbRencana = new ais.ui.util.MyTextbox(entry.optString("rencana",""));
                    tbRencana.setRows(2); tbRencana.setWidth("95%");
                    tbRencana.setDisabled(!bolehEdit);
                    rencanaList.add(tbRencana); tbRencana.setParent(row);

                    ais.ui.util.MyTextbox tbPj = new ais.ui.util.MyTextbox(entry.optString("pj",""));
                    tbPj.setWidth("95%"); tbPj.setDisabled(!bolehEdit);
                    pjList.add(tbPj); tbPj.setParent(row);

                    ais.ui.util.MyTextbox tbTarget = new ais.ui.util.MyTextbox(entry.optString("targetWaktu",""));
                    tbTarget.setWidth("95%"); tbTarget.setDisabled(!bolehEdit);
                    targetList.add(tbTarget); tbTarget.setParent(row);

                    ais.ui.util.MyTextbox tbStatus = new ais.ui.util.MyTextbox(entry.optString("status","Planned"));
                    tbStatus.setWidth("95%"); tbStatus.setDisabled(!bolehEdit);
                    statusList.add(tbStatus); tbStatus.setParent(row);

                    // Admin evaluation column
                    String adminKomVal = entry.optString("adminKomentar","");
                    if (isAdmin) {
                        ais.ui.util.MyTextbox tbAdminKom = new ais.ui.util.MyTextbox(adminKomVal);
                        tbAdminKom.setRows(2); tbAdminKom.setWidth("95%");
                        tbAdminKom.setTooltiptext("Evaluasi/komentar admin atas analisis dosen ini");
                        adminKomList.add(tbAdminKom); tbAdminKom.setParent(row);
                    } else {
                        adminKomList.add(null);
                        if (adminKomVal.isEmpty()) {
                            row.appendChild(new Label("-"));
                        } else {
                            Html aHtml = new Html("<span style='font-size:10px;color:#1e40af;font-style:italic;'>"
                                + esc(adminKomVal) + "</span>");
                            row.appendChild(aHtml);
                        }
                    }
                }

                // South: Save
                org.zkoss.zul.South south = new org.zkoss.zul.South();
                ais.ui.util.ZkCompat.setFlex(south, true);
                south.setStyle("background:#fff;border-top:1px solid #e2e8f0;padding:10px;");
                south.setParent(bl);

                org.zkoss.zul.Toolbar stb = new org.zkoss.zul.Toolbar();
                stb.setStyle("float:right;background:transparent;border:none;");
                stb.setParent(south);

                MyToolbarbuttonConfig btnBatal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
                btnBatal.addEventListener("onClick", new EventListener() {
                    public void onEvent(Event e) throws Exception { win.detach(); }
                });
                btnBatal.setParent(stb);

                if (bolehEdit) {
                    MyToolbarbuttonConfig btnSimpan = new MyToolbarbuttonConfig("Simpan CQI", "/img/save.gif");
                    btnSimpan.setStyle("font-weight:bold;background:#c2410c;color:#fff;border-radius:6px;padding:5px 14px;border:0;");
                    btnSimpan.addEventListener("onClick", new EventListener() {
                        public void onEvent(Event e) throws Exception {
                            JSONArray result = new JSONArray();
                            for (int i = 0; i < cpmkKodes.size(); i++) {
                                JSONObject en = new JSONObject();
                                en.put("cpmk",       cpmkKodes.get(i));
                                en.put("masalah",    masalahList.get(i).getValue().trim());
                                en.put("analisis",   analisisList.get(i).getValue().trim());
                                en.put("rencana",    rencanaList.get(i).getValue().trim());
                                en.put("pj",         pjList.get(i).getValue().trim());
                                en.put("targetWaktu",targetList.get(i).getValue().trim());
                                en.put("status",     statusList.get(i).getValue().trim().isEmpty()
                                        ? "Planned" : statusList.get(i).getValue().trim());
                                if (isAdmin && adminKomList.get(i) != null) {
                                    en.put("adminKomentar", adminKomList.get(i).getValue().trim());
                                } else {
                                    // preserve existing admin comment
                                    JSONObject existEntry = cqiMap.containsKey(cpmkKodes.get(i))
                                            ? cqiMap.get(cpmkKodes.get(i)) : new JSONObject();
                                    en.put("adminKomentar", existEntry.optString("adminKomentar",""));
                                }
                                result.put(en);
                            }
                            // Save via separate session
                            Session saveSess = null;
                            try {
                                saveSess = HibernateUtil.openSession();
                                Perkuliahan perkToSave = (Perkuliahan) saveSess.get(Perkuliahan.class, perkId);
                                if (perkToSave != null) {
                                    perkToSave.setCqiData(result.toString());
                                    saveSess.update(perkToSave);
                                    saveSess.flush();
                                }
                            } finally {
                                if (saveSess != null) {
                                    try { saveSess.flush(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:2564");}
                                    try { saveSess.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:2565");}
                                }
                            }
                            win.detach();
                            // Invalidate cache so next render reflects saved data
                            String key = cacheKey();
                            DATA_CACHE.remove(key);
                            CACHE_EXPIRY.remove(key);
                            ais.ui.util.MyMessageboxConfig.show("CQI berhasil disimpan.",
                                    "Info", ais.ui.util.MyMessageboxConfig.OK,
                                    ais.ui.util.MyMessageboxConfig.INFORMATION);
                        }
                    });
                    btnSimpan.setParent(stb);
                }
                win.onModal();
            } finally {
                if (sess != null) {
                    try { sess.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:2583");}
                }
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    // ====================================================================
    // OBE Point 3+4: Analisis Prodi CPL & PL (admin/kajur only)
    // Stores in konfigurasi:
    //   CPL key: obe_prodi_cpl_{jurusanId}_{ta}_{semester}
    //   PL  key: obe_prodi_pl_{jurusanId}_{ta}
    // ====================================================================

    private void renderAnalisisProdi(final ObeData d) {
        if (tbmuser == null || tbmuser.ambilDosen() != null || tbmuser.getMahasiswa() != null) return;
        if (d.cplPerJurusan.isEmpty() && d.plPerJurusan.isEmpty()) return;

        Div secDiv = new Div();
        secDiv.setWidth("100%");
        secDiv.setStyle("margin-top:18px;background:#fff;border:1px solid #c7d2fe;"
                + "border-radius:12px;overflow:hidden;box-shadow:0 1px 4px rgba(99,102,241,0.08);");
        secDiv.setParent(this);

        appendHtml(secDiv,
            "<div style='background:linear-gradient(135deg,#4338ca,#7c3aed);color:#fff;"
            + "padding:14px 18px;'>"
            + "<div style='font-size:15px;font-weight:700;'>Analisis Prodi OBE — CPL &amp; PL (Admin/Kajur)</div>"
            + "<div style='font-size:11px;opacity:0.85;margin-top:2px;'>"
            + "OBE Point 3 &amp; 4: Catatan analisis CPL per tahun akademik dan PL secara keseluruhan di level Program Studi. "
            + "Hanya admin/Kajur yang dapat mengisi bagian ini.</div></div>");

        Div bodyDiv = new Div();
        bodyDiv.setStyle("padding:14px 16px;");
        bodyDiv.setParent(secDiv);

        // Iterate jurusan
        for (Map.Entry<Long, List<Object[]>> entry : d.cplPerJurusan.entrySet()) {
            final Long jurusanId = entry.getKey();
            final List<Object[]> cplList = entry.getValue();
            final String namaJur = d.namaJurusan.containsKey(jurusanId)
                    ? d.namaJurusan.get(jurusanId) : "Prodi #" + jurusanId;
            final List<Object[]> plList = d.plPerJurusan.containsKey(jurusanId)
                    ? d.plPerJurusan.get(jurusanId) : new ArrayList<Object[]>();

            if (filterJurusanId != null && !filterJurusanId.equals(jurusanId)) continue;

            appendHtml(bodyDiv,
                "<div style='font-size:13px;font-weight:700;color:#4338ca;margin:8px 0;"
                + "padding:6px 10px;background:#eef2ff;border-radius:6px;'>"
                + escHtml(namaJur) + "</div>");

            // ── CPL Analysis (OBE Point 3) ─────────────────────────────────────
            org.zkoss.zul.Button btnCpl = new org.zkoss.zul.Button("Edit Analisis CPL — " + namaJur);
            btnCpl.setStyle("margin:4px 0;");
            btnCpl.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    openCplAnalisisPopup(jurusanId, namaJur, cplList);
                }
            });
            btnCpl.setParent(bodyDiv);

            // Show CPL summary if exists
            appendHtml(bodyDiv, buildCplAnalisisSummaryHtml(jurusanId, cplList));

            // ── PL Analysis (OBE Point 4) ──────────────────────────────────────
            if (!plList.isEmpty()) {
                org.zkoss.zul.Button btnPl = new org.zkoss.zul.Button("Edit Analisis PL — " + namaJur);
                btnPl.setStyle("margin:4px 0 4px 8px;");
                btnPl.addEventListener("onClick", new EventListener() {
                    public void onEvent(Event e) throws Exception {
                        openPlAnalisisPopup(jurusanId, namaJur, plList);
                    }
                });
                btnPl.setParent(bodyDiv);
                appendHtml(bodyDiv, buildPlAnalisisSummaryHtml(jurusanId, plList));
            }
        }
    }

    private void openCplAnalisisPopup(final Long jurusanId, final String namaJur,
            final List<Object[]> cplList) {
        try {
            ais.ui.util.MyWindow win = new ais.ui.util.MyWindow();
            win.setTitle("Analisis CPL Prodi: " + namaJur + " — TA " + filterTa);
            win.setWidth("90%"); win.setHeight("88%");
            org.zkoss.zul.Vbox vb = new org.zkoss.zul.Vbox();
            vb.setWidth("100%");
            vb.setStyle("padding:12px;box-sizing:border-box;overflow:auto;");
            vb.setParent(win);

            appendHtml(vb,
                "<div style='background:#eef2ff;border:1px solid #c7d2fe;border-radius:8px;"
                + "padding:10px 14px;margin-bottom:10px;font-size:12px;'>"
                + "<b>OBE Point 3</b>: Catatan analisis CPL di level prodi untuk TA <b>"
                + escHtml(filterTa) + "</b> Semester <b>" + escHtml(filterSemester) + "</b>.</div>");

            String cplKey = "obe_prodi_cpl_" + jurusanId + "_" + filterTa + "_" + filterSemester;
            JSONObject cplData = new JSONObject();
            try {
                ais.database.model.Konfigurasi k = Common.getKonfigurasi(cplKey, "{}");
                if (k != null && k.getNilai() != null && !k.getNilai().trim().isEmpty()) {
                    cplData = new JSONObject(k.getNilai());
                }
            } catch (Exception ign) { ais.common.ErrorAuditUtil.record(ign, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:2688");}

            final List<String> kodes = new ArrayList<String>();
            final List<org.zkoss.zul.Textbox> tbCatatanList  = new ArrayList<org.zkoss.zul.Textbox>();
            final List<org.zkoss.zul.Textbox> tbAnalisisList = new ArrayList<org.zkoss.zul.Textbox>();
            final List<org.zkoss.zul.Textbox> tbRencanaList  = new ArrayList<org.zkoss.zul.Textbox>();

            Div scrollDiv = new Div();
            scrollDiv.setStyle("max-height:60vh;overflow-y:auto;");
            scrollDiv.setParent(vb);

            for (Object[] cpl : cplList) {
                String kode = cpl[0] != null ? String.valueOf(cpl[0]) : "";
                String nama  = cpl[1] != null ? String.valueOf(cpl[1]) : "";
                JSONObject ex = cplData.optJSONObject(kode);
                if (ex == null) ex = new JSONObject();

                appendHtml(scrollDiv,
                    "<div style='background:#eef2ff;font-size:12px;font-weight:700;color:#3730a3;"
                    + "padding:6px 12px;margin-top:8px;border-radius:4px;'>"
                    + escHtml(kode) + " — " + escHtml(nama) + "</div>");

                Grid g = new Grid(); g.setWidth("100%");
                Columns cols = new Columns();
                Column c1 = new Column(); c1.setWidth("130px"); c1.setParent(cols);
                Column c2 = new Column(); c2.setParent(cols);
                cols.setParent(g);
                Rows rows = new Rows(); rows.setParent(g);

                org.zkoss.zul.Textbox tbCat  = buildAnalRow(rows, "Catatan",  ex.optString("catatan",""));
                org.zkoss.zul.Textbox tbAnal = buildAnalRow(rows, "Analisis", ex.optString("analisis",""));
                org.zkoss.zul.Textbox tbRen  = buildAnalRow(rows, "Rencana",  ex.optString("rencana",""));

                g.setParent(scrollDiv);
                kodes.add(kode);
                tbCatatanList.add(tbCat);
                tbAnalisisList.add(tbAnal);
                tbRencanaList.add(tbRen);
            }

            final String finalCplKey = cplKey;
            org.zkoss.zul.Button btnSave = new org.zkoss.zul.Button("Simpan Analisis CPL");
            btnSave.setStyle("margin-top:12px;");
            btnSave.addEventListener("onClick", new EventListener() {
                public void onEvent(Event event) throws Exception {
                    JSONObject result = new JSONObject();
                    for (int i = 0; i < kodes.size(); i++) {
                        JSONObject e2 = new JSONObject();
                        e2.put("catatan",  tbCatatanList.get(i).getValue().trim());
                        e2.put("analisis", tbAnalisisList.get(i).getValue().trim());
                        e2.put("rencana",  tbRencanaList.get(i).getValue().trim());
                        result.put(kodes.get(i), e2);
                    }
                    KonfigurasiManager.simpanKonfigurasi(finalCplKey, result.toString());
                    ais.ui.util.MyMessageboxConfig.show("Analisis CPL prodi berhasil disimpan",
                        "Info", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
                }
            });
            btnSave.setParent(vb);
            win.doModal();
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    private void openPlAnalisisPopup(final Long jurusanId, final String namaJur,
            final List<Object[]> plList) {
        try {
            ais.ui.util.MyWindow win = new ais.ui.util.MyWindow();
            win.setTitle("Analisis PL Prodi: " + namaJur + " — TA " + filterTa);
            win.setWidth("90%"); win.setHeight("85%");
            org.zkoss.zul.Vbox vb = new org.zkoss.zul.Vbox();
            vb.setWidth("100%");
            vb.setStyle("padding:12px;box-sizing:border-box;overflow:auto;");
            vb.setParent(win);

            appendHtml(vb,
                "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-radius:8px;"
                + "padding:10px 14px;margin-bottom:10px;font-size:12px;'>"
                + "<b>OBE Point 4</b>: Catatan analisis Profil Lulusan (PL) di level prodi "
                + "secara keseluruhan untuk <b>" + escHtml(namaJur) + "</b>.</div>");

            String plKey = "obe_prodi_pl_" + jurusanId + "_" + filterTa;
            JSONObject plData = new JSONObject();
            try {
                ais.database.model.Konfigurasi k = Common.getKonfigurasi(plKey, "{}");
                if (k != null && k.getNilai() != null && !k.getNilai().trim().isEmpty()) {
                    plData = new JSONObject(k.getNilai());
                }
            } catch (Exception ign) { ais.common.ErrorAuditUtil.record(ign, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DasboardObeElearningHelper.java:2777");}

            final List<String> kodes = new ArrayList<String>();
            final List<org.zkoss.zul.Textbox> tbCatatanList  = new ArrayList<org.zkoss.zul.Textbox>();
            final List<org.zkoss.zul.Textbox> tbAnalisisList = new ArrayList<org.zkoss.zul.Textbox>();
            final List<org.zkoss.zul.Textbox> tbRencanaList  = new ArrayList<org.zkoss.zul.Textbox>();

            Div scrollDiv = new Div();
            scrollDiv.setStyle("max-height:55vh;overflow-y:auto;");
            scrollDiv.setParent(vb);

            for (Object[] pl : plList) {
                String kode = pl[0] != null ? String.valueOf(pl[0]) : "";
                String nama  = pl[1] != null ? String.valueOf(pl[1]) : "";
                JSONObject ex = plData.optJSONObject(kode);
                if (ex == null) ex = new JSONObject();

                appendHtml(scrollDiv,
                    "<div style='background:#f0fdf4;font-size:12px;font-weight:700;color:#166534;"
                    + "padding:6px 12px;margin-top:8px;border-radius:4px;'>"
                    + escHtml(kode) + " — " + escHtml(nama) + "</div>");

                Grid g = new Grid(); g.setWidth("100%");
                Columns cols = new Columns();
                Column c1 = new Column(); c1.setWidth("130px"); c1.setParent(cols);
                Column c2 = new Column(); c2.setParent(cols);
                cols.setParent(g);
                Rows rows = new Rows(); rows.setParent(g);

                org.zkoss.zul.Textbox tbCat  = buildAnalRow(rows, "Catatan",  ex.optString("catatan",""));
                org.zkoss.zul.Textbox tbAnal = buildAnalRow(rows, "Analisis Gap", ex.optString("analisis",""));
                org.zkoss.zul.Textbox tbRen  = buildAnalRow(rows, "Rekomendasi", ex.optString("rencana",""));

                g.setParent(scrollDiv);
                kodes.add(kode);
                tbCatatanList.add(tbCat);
                tbAnalisisList.add(tbAnal);
                tbRencanaList.add(tbRen);
            }

            final String finalPlKey = plKey;
            org.zkoss.zul.Button btnSave = new org.zkoss.zul.Button("Simpan Analisis PL");
            btnSave.setStyle("margin-top:12px;");
            btnSave.addEventListener("onClick", new EventListener() {
                public void onEvent(Event event) throws Exception {
                    JSONObject result = new JSONObject();
                    for (int i = 0; i < kodes.size(); i++) {
                        JSONObject e2 = new JSONObject();
                        e2.put("catatan",  tbCatatanList.get(i).getValue().trim());
                        e2.put("analisis", tbAnalisisList.get(i).getValue().trim());
                        e2.put("rencana",  tbRencanaList.get(i).getValue().trim());
                        result.put(kodes.get(i), e2);
                    }
                    KonfigurasiManager.simpanKonfigurasi(finalPlKey, result.toString());
                    ais.ui.util.MyMessageboxConfig.show("Analisis PL prodi berhasil disimpan",
                        "Info", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
                }
            });
            btnSave.setParent(vb);
            win.doModal();
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    private static org.zkoss.zul.Textbox buildAnalRow(Rows rows, String label, String value) {
        Row row = new Row(); row.setParent(rows);
        new Label(label).setParent(row);
        org.zkoss.zul.Textbox tb = new org.zkoss.zul.Textbox(value != null ? value : "");
        tb.setRows(2); tb.setWidth("98%"); tb.setMultiline(true);
        tb.setParent(row);
        return tb;
    }

    private String buildCplAnalisisSummaryHtml(Long jurusanId, List<Object[]> cplList) {
        try {
            String key = "obe_prodi_cpl_" + jurusanId + "_" + filterTa + "_" + filterSemester;
            ais.database.model.Konfigurasi k = Common.getKonfigurasi(key, "{}");
            if (k == null || k.getNilai() == null || k.getNilai().trim().isEmpty()
                    || "{}".equals(k.getNilai().trim())) {
                return "<div style='font-size:11px;color:#94a3b8;font-style:italic;margin:4px 0 8px;'>"
                        + "Belum ada analisis CPL prodi.</div>";
            }
            JSONObject data = new JSONObject(k.getNilai());
            StringBuilder sb = new StringBuilder();
            sb.append("<div style='font-size:11px;margin:4px 0 8px;'>");
            for (Object[] cpl : cplList) {
                String kode = cpl[0] != null ? String.valueOf(cpl[0]) : "";
                JSONObject e = data.optJSONObject(kode);
                if (e != null && !e.optString("catatan","").isEmpty()) {
                    sb.append("<span style='background:#eef2ff;border-radius:4px;padding:2px 6px;margin:2px;display:inline-block;'>")
                      .append(escHtml(kode)).append(": ").append(escHtml(e.optString("catatan","").length() > 40
                            ? e.optString("catatan").substring(0,40) + "…" : e.optString("catatan",""))).append("</span>");
                }
            }
            sb.append("</div>");
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private String buildPlAnalisisSummaryHtml(Long jurusanId, List<Object[]> plList) {
        try {
            String key = "obe_prodi_pl_" + jurusanId + "_" + filterTa;
            ais.database.model.Konfigurasi k = Common.getKonfigurasi(key, "{}");
            if (k == null || k.getNilai() == null || k.getNilai().trim().isEmpty()
                    || "{}".equals(k.getNilai().trim())) {
                return "<div style='font-size:11px;color:#94a3b8;font-style:italic;margin:4px 0 8px;'>"
                        + "Belum ada analisis PL prodi.</div>";
            }
            JSONObject data = new JSONObject(k.getNilai());
            StringBuilder sb = new StringBuilder();
            sb.append("<div style='font-size:11px;margin:4px 0 8px;'>");
            for (Object[] pl : plList) {
                String kode = pl[0] != null ? String.valueOf(pl[0]) : "";
                JSONObject e = data.optJSONObject(kode);
                if (e != null && !e.optString("catatan","").isEmpty()) {
                    sb.append("<span style='background:#f0fdf4;border-radius:4px;padding:2px 6px;margin:2px;display:inline-block;'>")
                      .append(escHtml(kode)).append(": ").append(escHtml(e.optString("catatan","").length() > 40
                            ? e.optString("catatan").substring(0,40) + "…" : e.optString("catatan",""))).append("</span>");
                }
            }
            sb.append("</div>");
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }
}
