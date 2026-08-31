package ais.action.master.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;

import ais.common.Common;
import ais.common.DashboardCacheUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.spi.ChecklistAuditSPI;
import ais.database.model.spi.JenisAuditSPI;
import ais.database.model.spi.PenugasanAuditSPI;
import ais.database.model.spi.ProfilRisikoSPI;
import ais.database.model.spi.TemuanAuditSPI;
import ais.database.model.spi.TindakLanjutAuditSPI;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h2>DasboardSPI &mdash; Dasbor Ringkasan Satuan Pengawasan Internal</h2>
 *
 * <p>
 * Satu layar ringkas yang menjawab pertanyaan yang paling sering ditanyakan pimpinan kepada SPI:
 * "sudah sejauh mana audit tahun ini berjalan, seberapa berat temuan yang ada, dan apakah unit
 * yang bermasalah sudah menindaklanjuti rekomendasi?" &mdash; tanpa perlu membuka satu per satu
 * layar Penugasan Audit. Dasbor ini SENGAJA menggabungkan data dari SEMUA bagian modul SPI: Bagian
 * A (cakupan checklist yang sudah pernah diuji), Bagian B ({@link ProfilRisikoSPI}, sebagai
 * pembanding "rencana" terhadap "realisasi"), dan Bagian C ({@link PenugasanAuditSPI}/
 * {@link TemuanAuditSPI}/{@link TindakLanjutAuditSPI}, data pelaksanaan sesungguhnya) &mdash; satu
 * tempat yang menyatukan seluruh siklus kerja SPI dari perencanaan sampai penyelesaian.
 * </p>
 *
 * <h3>Pola implementasi: HTML/CSS/SVG murni, bukan JFreeChart</h3>
 * <p>
 * Seluruh chart (donut, bar, tren garis) dibangun manual dari string HTML/SVG yang disisipkan
 * lewat komponen {@code org.zkoss.zul.Html} &mdash; SENGAJA meniru pola yang sudah dipakai
 * {@code ais.action.master.spmi.DasboardSPMI} dan dasbor-dasbor lain di aplikasi ini. Pendekatan
 * ini dipilih (bukan pustaka JFreeChart) karena menghasilkan visual yang jauh lebih modern, ringan
 * dirender di browser, dan otomatis responsif mengikuti lebar layar tanpa perlu meng-generate
 * ulang gambar bitmap di sisi server.
 * </p>
 *
 * <h3>Caching dua lapis</h3>
 * <p>
 * Query agregasi (hitung jumlah, kelompokkan per kategori) dikerjakan sekali lalu disimpan lewat
 * {@link DashboardCacheUtil} (cache memori L2 + cache lebih persisten L3, kunci mencakup filter
 * yang aktif) &mdash; sehingga pembukaan ulang dasbor dengan filter yang sama tidak perlu
 * menghitung ulang dari database setiap kali, konsisten dengan pola performa yang sudah dipakai
 * dasbor-dasbor lain di aplikasi ini.
 * </p>
 *
 * @author e-Campus SPI Team
 */
public class DasboardSPI extends Div {

    private static final long serialVersionUID = 1L;

    private static final int ZONA_HIJAU_MIN  = 70;
    private static final int ZONA_KUNING_MIN = 50;

    private String filterTahun = "";
    private JenisAuditSPI filterJenis = null;

    private Combobox cbTahun;
    private Combobox cbJenis;

    // ----------------------------------------------------------------
    // Data container
    // ----------------------------------------------------------------

    /**
     * Tipe implementasi bersarang {@link SpiData} milik {@link DasboardSPI}. Kelas ini memberi nama pada state
     * atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardSPI}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
     * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int totalPenugasan}, {@code int
     * disetujui}, {@code int menunggu}, {@code int ditolak}, {@code int totalTemuan}, {@code int jmlKritis},
     * {@code int jmlMayor}, {@code int jmlMinor}; operasi lokal: {@code healthPct()}, {@code covPct()}, {@code
     * beratPct()}, {@code zona}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see DasboardSPI
     */
    private static class SpiData {
        int totalPenugasan, disetujui, menunggu, ditolak;

        int totalTemuan, jmlKritis, jmlMayor, jmlMinor, jmlObs, jmlSesuai, jmlBelum;

        int totalChecklistAktif, checklistTeraudit;

        int tlTotal, tlSelesai, tlBerjalan, tlTerlambat;

        int profilTinggi, profilSedang, profilRendah;

        List<Object[]> perJenis        = new ArrayList<Object[]>();
        List<Object[]> perSatuanKerja  = new ArrayList<Object[]>();
        List<Object[]> trendTemuan     = new ArrayList<Object[]>();
        List<PenugasanAuditSPI> recent = new ArrayList<PenugasanAuditSPI>();

        int healthPct() {
            int dinilai = jmlKritis + jmlMayor + jmlMinor + jmlObs + jmlSesuai;
            return dinilai == 0 ? 0 : jmlSesuai * 100 / dinilai;
        }
        int covPct() {
            return totalChecklistAktif == 0 ? 0 : checklistTeraudit * 100 / totalChecklistAktif;
        }
        int beratPct() {
            int dinilai = jmlKritis + jmlMayor + jmlMinor + jmlObs + jmlSesuai;
            return dinilai == 0 ? 0 : (jmlKritis + jmlMayor) * 100 / dinilai;
        }
        String zona() {
            int h = healthPct();
            return h >= ZONA_HIJAU_MIN ? "HIJAU" : (h >= ZONA_KUNING_MIN ? "KUNING" : "MERAH");
        }
    }

    // ----------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------

    public DasboardSPI() {
        setWidth("100%");
        setStyle("min-height:300px; background:#f1f5f9; padding:14px 16px;"
               + " box-sizing:border-box; overflow:auto;");
        try {
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

    private void tampilLoading() {
        Common.clear(this);
        appendHtml(this,
            "<div style='padding:80px 0; text-align:center;'>"
            + "<div style='font-size:40px; margin-bottom:16px; animation:spin 1.5s linear infinite;'>&#9203;</div>"
            + "<div style='font-size:15px; font-weight:800; color:#334155;'>Memuat Dasbor SPI&#8230;</div>"
            + "<div style='margin-top:8px; font-size:12px; color:#94a3b8;'>"
            + "Menghitung data penugasan, temuan, tindak lanjut, dan profil risiko.</div>"
            + "</div>"
            + "<style>@keyframes spin{to{transform:rotate(360deg)}}</style>");
    }

    // ----------------------------------------------------------------
    // Main render
    // ----------------------------------------------------------------

    private void renderAll() {
        try {
            SpiData d = loadDataWithCache();
            Common.clear(this);

            renderHero(d);
            renderFilter();

            renderPenugasanCards(d);
            renderTemuanCards(d);

            Div row1 = flexRow();
            renderZonaPanel(colDiv(row1, "flex:0 1 200px; min-width:160px;"), d);
            renderDonutHealth(colDiv(row1, "flex:1 1 220px; min-width:180px;"), d);
            renderTlSummary(colDiv(row1, "flex:1 1 240px; min-width:180px;"), d);

            Div row2 = flexRow();
            renderTrendTemuan(colDiv(row2, "flex:1 1 300px;"), d);
            renderTopSatuanKerja(colDiv(row2, "flex:1 1 300px;"), d);

            Div row3 = flexRow();
            renderDistribusiTemuan(colDiv(row3, "flex:2 1 280px;"), d);
            renderPerJenis(colDiv(row3, "flex:1 1 220px;"), d);
            renderProfilRisikoPanel(colDiv(row3, "flex:1 1 220px;"), d);

            renderRecent(this, d);
            renderActionPlan(this, d);

        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    // ----------------------------------------------------------------
    // Data queries
    // ----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private SpiData loadDataWithCache() {
        String fp = (filterTahun != null && !filterTahun.isEmpty() ? filterTahun : "all")
                  + "_" + (filterJenis != null ? filterJenis.getId() : "all");
        String key = DashboardCacheUtil.keyWithFilter("DasboardSPI", "ADMIN", null, fp);
        Object fromL2 = DashboardCacheUtil.getL2(key);
        if (fromL2 instanceof SpiData) return (SpiData) fromL2;
        Object fromL3 = DashboardCacheUtil.getL3(key);
        if (fromL3 instanceof SpiData) {
            DashboardCacheUtil.putL2(key, fromL3);
            return (SpiData) fromL3;
        }
        SpiData d = loadData();
        DashboardCacheUtil.putL2(key, d);
        DashboardCacheUtil.putL3(key, d);
        return d;
    }

    @SuppressWarnings("unchecked")
    private SpiData loadData() {
        SpiData d = new SpiData();
        Session sess = HibernateUtil.currentSession();

        d.totalPenugasan = count(buildPC());
        d.disetujui = count(buildPC().add(Restrictions.eq("status", PenugasanAuditSPI.DISETUJU)));
        d.menunggu  = count(buildPC().add(Restrictions.eq("status", PenugasanAuditSPI.PENGAJUAN)));
        d.ditolak   = count(buildPC().add(Restrictions.eq("status", PenugasanAuditSPI.DITOLAK)));

        d.totalTemuan = count(buildTC());
        d.jmlKritis   = count(buildTC().add(Restrictions.eq("klasifikasi", TemuanAuditSPI.KRITIS)));
        d.jmlMayor    = count(buildTC().add(Restrictions.eq("klasifikasi", TemuanAuditSPI.MAYOR)));
        d.jmlMinor    = count(buildTC().add(Restrictions.eq("klasifikasi", TemuanAuditSPI.MINOR)));
        d.jmlObs      = count(buildTC().add(Restrictions.eq("klasifikasi", TemuanAuditSPI.OBSERVASI)));
        d.jmlSesuai   = count(buildTC().add(Restrictions.eq("klasifikasi", TemuanAuditSPI.SESUAI)));
        d.jmlBelum    = count(buildTC().add(Restrictions.isNull("klasifikasi")));

        d.totalChecklistAktif = count(sess.createCriteria(ChecklistAuditSPI.class)
            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE))));
        Number ncov = (Number) buildTC().setProjection(Projections.countDistinct("checklistAuditSPI")).uniqueResult();
        d.checklistTeraudit = ncov == null ? 0 : ncov.intValue();

        try {
            Criteria ctlBase = sess.createCriteria(TindakLanjutAuditSPI.class)
                .createAlias("temuanAuditSPI", "tm")
                .createAlias("tm.penugasanAuditSPI", "pn")
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
            addPnAliasFilter(ctlBase, "pn");
            d.tlTotal = count(ctlBase);

            d.tlSelesai = count(tlBaseCriteria(sess).add(Restrictions.eq("status", TindakLanjutAuditSPI.SELESAI)));
            d.tlBerjalan = count(tlBaseCriteria(sess).add(Restrictions.eq("status", TindakLanjutAuditSPI.SEDANG_BERJALAN)));
            d.tlTerlambat = count(tlBaseCriteria(sess)
                .add(Restrictions.ne("status", TindakLanjutAuditSPI.SELESAI))
                .add(Restrictions.isNotNull("targetDate"))
                .add(Restrictions.lt("targetDate", new Date())));
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) DasboardSPI.loadData:tindaklanjut"); }

        // Profil risiko (Bagian B) — tidak difilter tahun/jenis penugasan, murni gambaran audit universe terkini
        try {
            List<ProfilRisikoSPI> semuaProfil = sess.createCriteria(ProfilRisikoSPI.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                    .list();
            for (ProfilRisikoSPI p : semuaProfil) {
                String zona = p.getZonaRisiko();
                if (ProfilRisikoSPI.ZONA_TINGGI.equals(zona)) d.profilTinggi++;
                else if (ProfilRisikoSPI.ZONA_SEDANG.equals(zona)) d.profilSedang++;
                else d.profilRendah++;
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) DasboardSPI.loadData:profilrisiko"); }

        try {
            List<Object[]> pj = (List<Object[]>) buildPC()
                .createAlias("jenisAuditSPI", "jns", Criteria.LEFT_JOIN)
                .setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("jns.nama"))
                    .add(Projections.rowCount()))
                .setMaxResults(15).list();
            sortByCountDesc(pj);
            d.perJenis = pj;
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) DasboardSPI.loadData:perjenis"); }

        try {
            Criteria cUnit = sess.createCriteria(TemuanAuditSPI.class)
                .createAlias("penugasanAuditSPI", "pn")
                .createAlias("pn.satuanKerja", "sk", Criteria.LEFT_JOIN)
                .add(Restrictions.or(
                    Restrictions.eq("klasifikasi", TemuanAuditSPI.KRITIS),
                    Restrictions.eq("klasifikasi", TemuanAuditSPI.MAYOR)));
            addPnAliasFilter(cUnit, "pn");
            List<Object[]> topUnit = (List<Object[]>) cUnit
                .setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("sk.nama"))
                    .add(Projections.rowCount()))
                .setMaxResults(30).list();
            sortByCountDesc(topUnit);
            d.perSatuanKerja = topUnit.size() > 8 ? topUnit.subList(0, 8) : topUnit;
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) DasboardSPI.loadData:perunit"); }

        try {
            // Dikelompokkan per tahun di sisi Java (bukan SQL group-by) — jumlah baris temuan
            // Kritis/Mayor pada praktiknya kecil (ratusan, bukan jutaan), jadi pendekatan ini
            // tetap ringan sekaligus menghindari sintaks proyeksi SQL native yang rawan berbeda
            // perilaku antar versi Hibernate/driver.
            Criteria cTrend = sess.createCriteria(TemuanAuditSPI.class)
                .createAlias("penugasanAuditSPI", "pn")
                .add(Restrictions.or(
                    Restrictions.eq("klasifikasi", TemuanAuditSPI.KRITIS),
                    Restrictions.eq("klasifikasi", TemuanAuditSPI.MAYOR)));
            if (filterJenis != null) cTrend.add(Restrictions.eq("pn.jenisAuditSPI", filterJenis));
            List<Date> tanggalList = (List<Date>) cTrend
                .setProjection(Projections.property("pn.tanggalMulai"))
                .list();

            java.util.Map<String, Integer> perTahun = new java.util.TreeMap<String, Integer>();
            java.util.Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
            for (Date tgl : tanggalList) {
                if (tgl == null) continue;
                cal.setTime(tgl);
                String thn = String.valueOf(cal.get(java.util.Calendar.YEAR));
                Integer prev = perTahun.get(thn);
                perTahun.put(thn, prev == null ? 1 : prev + 1);
            }
            List<Object[]> trend = new ArrayList<Object[]>();
            for (java.util.Map.Entry<String, Integer> e : perTahun.entrySet()) {
                trend.add(new Object[]{e.getKey(), e.getValue()});
            }
            d.trendTemuan = trend;
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) DasboardSPI.loadData:trend"); }

        d.recent = (List<PenugasanAuditSPI>) buildPC().addOrder(Order.desc("id")).setMaxResults(10).list();

        return d;
    }

    private Criteria tlBaseCriteria(Session sess) {
        Criteria c = sess.createCriteria(TindakLanjutAuditSPI.class)
            .createAlias("temuanAuditSPI", "tm")
            .createAlias("tm.penugasanAuditSPI", "pn")
            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
        addPnAliasFilter(c, "pn");
        return c;
    }

    private int count(Criteria c) {
        Number n = (Number) c.setProjection(Projections.rowCount()).uniqueResult();
        return n == null ? 0 : n.intValue();
    }

    private void sortByCountDesc(List<Object[]> list) {
        Collections.sort(list, new Comparator<Object[]>() {
            public int compare(Object[] a, Object[] b) {
                return ((Number) b[1]).intValue() - ((Number) a[1]).intValue();
            }
        });
    }

    private Criteria buildPC() {
        Session sess = HibernateUtil.currentSession();
        Criteria c = sess.createCriteria(PenugasanAuditSPI.class)
            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
        applyPenugasanFilter(c);
        return c;
    }

    private Criteria buildTC() {
        Session sess = HibernateUtil.currentSession();
        Criteria c = sess.createCriteria(TemuanAuditSPI.class).createAlias("penugasanAuditSPI", "pn");
        addPnAliasFilter(c, "pn");
        return c;
    }

    private void applyPenugasanFilter(Criteria c) {
        if (filterTahun != null && !filterTahun.isEmpty()) {
            c.add(Restrictions.sqlRestriction("extract(year from tanggal_mulai) = " + filterTahun));
        }
        if (filterJenis != null) c.add(Restrictions.eq("jenisAuditSPI", filterJenis));
    }

    private void addPnAliasFilter(Criteria c, String alias) {
        if (filterTahun != null && !filterTahun.isEmpty()) {
            c.add(Restrictions.sqlRestriction(alias + ".tanggal_mulai is not null and extract(year from "
                    + alias + ".tanggal_mulai) = " + filterTahun));
        }
        if (filterJenis != null) c.add(Restrictions.eq(alias + ".jenisAuditSPI", filterJenis));
    }

    // ================================================================
    // RENDER SECTIONS
    // ================================================================

    private void renderHero(SpiData d) {
        int pct = d.totalPenugasan == 0 ? 0 : d.disetujui * 100 / d.totalPenugasan;
        String zona = d.zona();
        String zClr = "HIJAU".equals(zona) ? "#22c55e" : ("MERAH".equals(zona) ? "#ef4444" : "#f59e0b");

        appendHtml(this,
            "<div style='border-radius:20px; padding:22px 26px; margin-bottom:14px;"
            + " background:linear-gradient(135deg,rgba(0,0,0,.22) 0%,rgba(0,0,0,0) 55%),"
            + "linear-gradient(135deg,#1e293b 0%,#334155 60%,#0f766e 100%);"
            + " color:#fff; box-shadow:0 10px 30px rgba(15,23,42,.28);'>"
            + "<div style='display:flex; flex-wrap:wrap; gap:14px; align-items:center;'>"
            + "<div style='flex:1; min-width:180px;'>"
            + "<div style='font-size:11px; font-weight:700; opacity:.75; letter-spacing:.1em;"
            + " text-transform:uppercase; margin-bottom:5px;'>Satuan Pengawasan Internal</div>"
            + "<div style='font-size:21px; font-weight:900; letter-spacing:-.03em; line-height:1.2;'>"
            + "Dasbor Audit Internal</div>"
            + "<div style='font-size:12px; opacity:.8; margin-top:6px; line-height:1.5;'>"
            + "Pantau kepatuhan, temuan, dan tindak lanjut seluruh unit kerja dalam satu layar.</div>"
            + "<div style='margin-top:12px; display:flex; gap:7px; flex-wrap:wrap;'>"
            + heroBadge(d.totalPenugasan + " Penugasan Audit")
            + heroBadge(d.disetujui + " Disetujui (" + pct + "%)")
            + heroBadge(filterTahun != null && !filterTahun.isEmpty() ? "Tahun " + esc(filterTahun) : "Semua Tahun")
            + (d.jmlKritis > 0
                ? "<span style='display:inline-block; border-radius:999px; padding:4px 12px;"
                  + " font-size:11px; font-weight:700; background:rgba(239,68,68,.35);"
                  + " border:1px solid rgba(239,68,68,.6);'>" + d.jmlKritis + " Temuan Kritis</span>"
                : heroBadge("Tidak Ada Temuan Kritis"))
            + "</div></div>"
            + "<div style='flex:0 0 auto; text-align:center; background:rgba(255,255,255,.12);"
            + " border-radius:16px; padding:16px 20px; min-width:120px;'>"
            + "<div style='font-size:10px; font-weight:700; opacity:.7; letter-spacing:.1em;'>ZONA KEPATUHAN</div>"
            + "<div style='font-size:28px; font-weight:900; color:" + zClr + "; margin:6px 0; line-height:1;'>"
            + zona + "</div>"
            + "<div style='font-size:11px; opacity:.75;'>" + d.healthPct() + "% Sesuai</div>"
            + "</div>"
            + "</div></div>");
    }

    private String heroBadge(String text) {
        return "<span style='display:inline-block; border-radius:999px; padding:4px 12px;"
             + " font-size:11px; font-weight:700; background:rgba(255,255,255,.18);'>" + esc(text) + "</span>";
    }

    private void renderFilter() {
        Div bar = new Div();
        bar.setStyle("background:#fff; border-radius:14px; padding:12px 16px;"
                   + " box-shadow:0 2px 8px rgba(0,0,0,.06); margin-bottom:14px;"
                   + " display:flex; align-items:center; gap:10px; flex-wrap:wrap;");
        bar.setParent(this);

        appendHtml(bar, "<span style='font-size:12px; font-weight:700; color:#475569;'>Filter:</span>");

        appendHtml(bar, "<span style='font-size:11px; color:#64748b;'>Tahun Pelaksanaan</span>");
        cbTahun = new Combobox();
        cbTahun.setWidth("120px");
        cbTahun.setReadonly(true);
        cbTahun.setParent(bar);
        Comboitem semuaTahun = new Comboitem("Semua");
        semuaTahun.setValue("");
        cbTahun.appendChild(semuaTahun);
        int tahunSekarang = ais.ui.util.WaktuUtil.getCalendar().get(java.util.Calendar.YEAR);
        for (int y = tahunSekarang; y >= tahunSekarang - 5; y--) {
            Comboitem ci = new Comboitem(String.valueOf(y));
            ci.setValue(String.valueOf(y));
            cbTahun.appendChild(ci);
        }
        if (filterTahun != null && !filterTahun.isEmpty()) Common.selectComboItem(cbTahun, filterTahun);
        else cbTahun.setSelectedIndex(0);

        appendHtml(bar, "<span style='font-size:11px; color:#64748b;'>Jenis Audit</span>");
        cbJenis = new Combobox();
        cbJenis.setWidth("200px");
        cbJenis.setReadonly(true);
        cbJenis.setParent(bar);
        Common.insertComboDanSemua(cbJenis, "nama", "keterangan",
            JenisAuditSPI.class, Restrictions.eq("aktif", true));
        if (filterJenis != null) Common.selectComboItem(true, cbJenis, filterJenis);
        else cbJenis.setSelectedIndex(0);

        MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig("Tampilkan", "/img/search.gif");
        btn.setParent(bar);
        btn.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                readFilter();
                tampilLoading();
                Common.createDefaultTimer(new EventListener() {
                    @Override
                    public void onEvent(Event e2) throws Exception { renderAll(); }
                });
            }
        });
    }

    private void readFilter() {
        if (cbTahun != null && cbTahun.getSelectedItem() != null) {
            Object v = cbTahun.getSelectedItem().getValue();
            filterTahun = v == null ? "" : v.toString();
        }
        if (cbJenis != null && cbJenis.getSelectedItem() != null) {
            Object v = cbJenis.getSelectedItem().getValue();
            filterJenis = (v instanceof JenisAuditSPI) ? (JenisAuditSPI) v : null;
        }
    }

    private void renderPenugasanCards(SpiData d) {
        Div row = new Div();
        row.setStyle("display:flex; gap:10px; flex-wrap:wrap; margin-bottom:10px;");
        row.setParent(this);
        metricCard(row, "Total Penugasan", d.totalPenugasan, "Semua penugasan audit terdaftar", "#dbeafe", "#1e40af", "#1d4ed8", "&#128196;");
        metricCard(row, "Disetujui",       d.disetujui,      "Penugasan sudah disahkan",         "#dcfce7", "#166534", "#16a34a", "&#10003;");
        metricCard(row, "Menunggu",        d.menunggu,       "Masih dalam proses pengajuan",      "#fef9c3", "#854d0e", "#ca8a04", "&#9203;");
        metricCard(row, "Ditolak",         d.ditolak,        "Tidak dilanjutkan / ditolak",        "#fee2e2", "#991b1b", "#dc2626", "&#10007;");
    }

    private void renderTemuanCards(SpiData d) {
        Div row = new Div();
        row.setStyle("display:flex; gap:10px; flex-wrap:wrap; margin-bottom:14px;");
        row.setParent(this);
        metricCard(row, "Total Temuan", d.totalTemuan,           "Semua item temuan tercatat", "#f1f5f9", "#334155", "#475569", "&#128202;");
        metricCard(row, "Kritis",       d.jmlKritis,              "Harus segera ditangani",     "#fee2e2", "#991b1b", "#dc2626", "&#9888;");
        metricCard(row, "Mayor",        d.jmlMayor,               "Perlu perbaikan segera",      "#ffedd5", "#9a3412", "#ea580c", "&#9651;");
        metricCard(row, "Minor",        d.jmlMinor,               "Perlu perbaikan terjadwal",   "#fef3c7", "#a06a00", "#d97706", "&#128270;");
        metricCard(row, "Sesuai",       d.jmlSesuai,              "Tidak ada masalah",           "#dcfce7", "#166534", "#22c55e", "&#127942;");
    }

    private void renderZonaPanel(Div parent, SpiData d) {
        Div panel = createPanel("Zona Kepatuhan", parent);
        appendHtml(panel,
            "<div style='font-size:10px; color:#64748b; margin-bottom:12px; line-height:1.5;'>"
            + "Klasifikasi zona berdasarkan proporsi temuan yang berstatus Sesuai.</div>");

        for (String[] z : new String[][]{
            {"HIJAU",  "&#127381;", "#dcfce7", "#166534", "#22c55e", ">= 70%", "Kepatuhan Baik"},
            {"KUNING", "&#127380;", "#fef9c3", "#854d0e", "#eab308", "50–70%",  "Perlu Perhatian"},
            {"MERAH",  "&#127379;", "#fee2e2", "#991b1b", "#ef4444", "< 50%",   "Perlu Perbaikan"}
        }) {
            boolean aktif = d.zona().equals(z[0]);
            appendHtml(panel,
                "<div style='border-radius:12px; padding:10px 12px; margin-bottom:7px; background:" + z[2] + ";"
                + (aktif ? " border:2px solid " + z[4] + "; box-shadow:0 0 0 3px " + z[4] + "22;" : "") + "'>"
                + "<div style='display:flex; justify-content:space-between; align-items:center;'>"
                + "<div><div style='font-size:12px; font-weight:900; color:" + z[3] + ";'>"
                + z[1] + " ZONA " + z[0] + (aktif ? " &#9664;" : "") + "</div>"
                + "<div style='font-size:10px; color:" + z[3] + "; opacity:.8; margin-top:2px;'>"
                + z[5] + " Sesuai — " + z[6] + "</div></div>"
                + (aktif ? "<div style='font-size:20px; font-weight:900; color:" + z[4] + ";'>" + d.healthPct() + "%</div>" : "")
                + "</div></div>");
        }
    }

    private void renderDonutHealth(Div parent, SpiData d) {
        Div panel = createPanel("Tingkat Kepatuhan", parent);
        int hp = d.healthPct();
        int cov = d.covPct();
        int berat = d.beratPct();
        String hColor = hp >= ZONA_HIJAU_MIN ? "#22c55e" : (hp >= ZONA_KUNING_MIN ? "#f59e0b" : "#ef4444");

        appendHtml(panel,
            "<div style='font-size:10px; color:#64748b; margin-bottom:10px; line-height:1.4;'>"
            + "Persentase checklist yang <b>sesuai</b> dari seluruh temuan yang sudah diklasifikasikan.</div>");
        appendHtml(panel, svgDonut(hp, hColor, "Sesuai"));
        appendHtml(panel,
            "<div style='display:flex; gap:7px; flex-wrap:wrap; margin-top:10px;'>"
            + miniScoreBox("Cakupan Checklist", cov + "%", cov >= 80 ? "#22c55e" : "#f59e0b")
            + miniScoreBox("Temuan Berat", berat + "%", berat <= 15 ? "#22c55e" : "#ef4444")
            + miniScoreBox("Diuji", d.checklistTeraudit + "/" + d.totalChecklistAktif, "#3b82f6")
            + "</div>");
        appendHtml(panel,
            "<div style='margin-top:10px;'>"
            + miniBar("Sesuai",    d.totalTemuan == 0 ? 0 : d.jmlSesuai * 100 / d.totalTemuan, "#22c55e")
            + miniBar("Observasi", d.totalTemuan == 0 ? 0 : d.jmlObs    * 100 / d.totalTemuan, "#3b82f6")
            + miniBar("Minor",     d.totalTemuan == 0 ? 0 : d.jmlMinor  * 100 / d.totalTemuan, "#d97706")
            + miniBar("Mayor",     d.totalTemuan == 0 ? 0 : d.jmlMayor  * 100 / d.totalTemuan, "#ea580c")
            + miniBar("Kritis",    d.totalTemuan == 0 ? 0 : d.jmlKritis * 100 / d.totalTemuan, "#ef4444")
            + "</div>");
    }

    private void renderTlSummary(Div parent, SpiData d) {
        Div panel = createPanel("Tindak Lanjut Auditee", parent);
        appendHtml(panel,
            "<div style='font-size:10px; color:#64748b; margin-bottom:12px; line-height:1.4;'>"
            + "Status penyelesaian tindak lanjut yang direalisasikan unit yang diaudit.</div>");

        if (d.tlTotal == 0) {
            appendHtml(panel,
                "<div style='text-align:center; padding:20px 10px;'>"
                + "<div style='font-size:28px; margin-bottom:8px;'>&#128203;</div>"
                + "<div style='font-size:12px; color:#64748b; font-weight:600;'>Belum ada tindak lanjut</div>"
                + "<div style='font-size:10px; color:#94a3b8; margin-top:4px; line-height:1.4;'>"
                + "Catat tindak lanjut pada setiap temuan di layar Penugasan Audit.</div></div>");
            return;
        }

        int tlPct = d.tlSelesai * 100 / d.tlTotal;
        appendHtml(panel, svgDonut(tlPct, tlPct >= 80 ? "#22c55e" : (tlPct >= 50 ? "#3b82f6" : "#f97316"), "Selesai"));
        appendHtml(panel,
            "<div style='display:grid; grid-template-columns:1fr 1fr; gap:7px; margin-top:10px;'>"
            + tlBox(d.tlSelesai, "Selesai", "#dcfce7", "#166534")
            + tlBox(d.tlBerjalan, "Berjalan", "#dbeafe", "#1e40af")
            + tlBox(d.tlTerlambat, "Terlambat", "#fee2e2", "#991b1b")
            + tlBox(d.tlTotal - d.tlSelesai - d.tlBerjalan - d.tlTerlambat, "Belum Dimulai", "#f1f5f9", "#475569")
            + "</div>");

        if (d.tlTerlambat > 0) {
            appendHtml(panel,
                "<div style='margin-top:10px; padding:8px 10px; background:#fee2e2; border-radius:10px;"
                + " font-size:11px; color:#991b1b; font-weight:600;'>"
                + "&#9888; " + d.tlTerlambat + " tindak lanjut melewati target waktu!</div>");
        }
    }

    private String tlBox(int val, String label, String bg, String clr) {
        return "<div style='border-radius:10px; padding:8px 10px; background:" + bg + "; text-align:center;'>"
             + "<div style='font-size:20px; font-weight:900; color:" + clr + ";'>" + val + "</div>"
             + "<div style='font-size:9px; color:" + clr + "; opacity:.8;'>" + esc(label) + "</div></div>";
    }

    private void renderTrendTemuan(Div parent, SpiData d) {
        Div panel = createPanel("Tren Temuan Kritis + Mayor per Tahun", parent);
        appendHtml(panel,
            "<div style='font-size:10px; color:#64748b; margin-bottom:10px; line-height:1.4;'>"
            + "<b>Tren menurun = kepatuhan membaik.</b></div>");
        appendHtml(panel, svgTrend(d.trendTemuan));
        if (d.trendTemuan.size() >= 2) {
            Object[] first = d.trendTemuan.get(0);
            Object[] last = d.trendTemuan.get(d.trendTemuan.size() - 1);
            int fv = ((Number) first[1]).intValue();
            int lv = ((Number) last[1]).intValue();
            boolean dec = lv < fv;
            appendHtml(panel,
                "<div style='font-size:11px; color:#475569; margin-top:8px; padding:8px 10px;"
                + " background:" + (dec ? "#dcfce7" : "#fee2e2") + "; border-radius:8px;'>"
                + (dec ? "&#9660; Tren <b>menurun</b> — tanda positif." : "&#9650; Tren <b>meningkat</b> — perlu perhatian.")
                + " Dari <b>" + fv + "</b> (" + esc(safeStr(first[0])) + ") &rarr; <b>" + lv + "</b> (" + esc(safeStr(last[0])) + ").</div>");
        } else if (d.trendTemuan.isEmpty()) {
            appendHtml(panel, "<div style='color:#94a3b8; font-size:11px; text-align:center; padding:20px;'>"
                + "Belum ada data untuk ditampilkan trendnya.</div>");
        }
    }

    private void renderTopSatuanKerja(Div parent, SpiData d) {
        Div panel = createPanel("Unit Kerja dengan Temuan Kritis/Mayor Terbanyak", parent);
        appendHtml(panel,
            "<div style='font-size:10px; color:#64748b; margin-bottom:10px; line-height:1.4;'>"
            + "Fokuskan pendampingan/tindak lanjut ke unit-unit ini.</div>");

        if (d.perSatuanKerja.isEmpty()) {
            appendHtml(panel,
                "<div style='text-align:center; padding:20px; color:#22c55e; font-size:13px; font-weight:700;'>"
                + "&#10003; Tidak ada temuan Kritis/Mayor pada periode ini!</div>");
            return;
        }

        int maxV = ((Number) d.perSatuanKerja.get(0)[1]).intValue();
        for (int i = 0; i < d.perSatuanKerja.size(); i++) {
            Object[] row = d.perSatuanKerja.get(i);
            String nama = row[0] == null ? "(Tanpa Nama)" : row[0].toString();
            int cnt = ((Number) row[1]).intValue();
            int w = maxV == 0 ? 0 : cnt * 100 / maxV;
            String bg = i == 0 ? "#fee2e2" : (i == 1 ? "#ffedd5" : "#fff7ed");
            String nbg = i == 0 ? "#ef4444" : (i == 1 ? "#f97316" : "#fb923c");
            appendHtml(panel,
                "<div style='margin:7px 0; padding:8px 10px; border-radius:10px; background:" + bg + ";'>"
                + "<div style='display:flex; justify-content:space-between; gap:6px; margin-bottom:4px;'>"
                + "<span style='font-size:10px; color:#475569; font-weight:600;'>" + (i + 1) + ". "
                + esc(nama.length() > 45 ? nama.substring(0, 44) + "…" : nama) + "</span>"
                + "<span style='font-size:11px; font-weight:800; color:#ef4444; white-space:nowrap;'>" + cnt + "</span></div>"
                + "<div style='height:7px; border-radius:4px; background:#fecaca;'>"
                + "<div style='height:7px; border-radius:4px; background:" + nbg + "; width:" + w + "%;'></div></div></div>");
        }
    }

    private void renderDistribusiTemuan(Div parent, SpiData d) {
        Div panel = createPanel("Distribusi Klasifikasi Temuan", parent);
        int total = Math.max(1, d.totalTemuan);
        appendHtml(panel, pctBar("Kritis",    d.jmlKritis, total, "#dc2626"));
        appendHtml(panel, pctBar("Mayor",     d.jmlMayor,  total, "#ea580c"));
        appendHtml(panel, pctBar("Minor",     d.jmlMinor,  total, "#d97706"));
        appendHtml(panel, pctBar("Observasi", d.jmlObs,    total, "#3b82f6"));
        appendHtml(panel, pctBar("Sesuai",    d.jmlSesuai, total, "#22c55e"));
        if (d.jmlBelum > 0) appendHtml(panel, pctBar("Belum Diklasifikasi", d.jmlBelum, total, "#94a3b8"));
        if (d.totalTemuan == 0) appendHtml(panel, emptyMsg());
    }

    private void renderPerJenis(Div parent, SpiData d) {
        Div panel = createPanel("Penugasan per Jenis Audit", parent);
        if (d.perJenis.isEmpty()) { appendHtml(panel, emptyMsg()); return; }
        int max = 1;
        for (Object[] r : d.perJenis) max = Math.max(max, ((Number) r[1]).intValue());
        for (Object[] r : d.perJenis)
            appendHtml(panel, countBar(r[0] == null ? "(Tanpa Jenis)" : r[0].toString(),
                    ((Number) r[1]).intValue(), max, "#1d4ed8"));
    }

    /** Panel tambahan yang menyatukan Bagian B (rencana) dengan Bagian C (realisasi). */
    private void renderProfilRisikoPanel(Div parent, SpiData d) {
        Div panel = createPanel("Profil Risiko Audit Universe", parent);
        appendHtml(panel,
            "<div style='font-size:10px; color:#64748b; margin-bottom:10px; line-height:1.4;'>"
            + "Sebaran zona risiko seluruh unit kerja yang pernah dinilai — bandingkan dengan unit yang benar-benar diaudit di atas.</div>");
        int total = Math.max(1, d.profilTinggi + d.profilSedang + d.profilRendah);
        appendHtml(panel, pctBar("Risiko Tinggi", d.profilTinggi, total, "#dc2626"));
        appendHtml(panel, pctBar("Risiko Sedang", d.profilSedang, total, "#d97706"));
        appendHtml(panel, pctBar("Risiko Rendah", d.profilRendah, total, "#22c55e"));
        if (d.profilTinggi + d.profilSedang + d.profilRendah == 0) appendHtml(panel, emptyMsg());
    }

    private void renderRecent(Div parent, SpiData d) {
        Div panel = createPanel("10 Penugasan Audit Terbaru", parent);
        if (d.recent.isEmpty()) { appendHtml(panel, emptyMsg()); return; }

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='overflow-x:auto; -webkit-overflow-scrolling:touch;'>");
        sb.append("<table style='width:100%; border-collapse:collapse; font-size:12px; min-width:520px;'>");
        sb.append("<tr style='background:#f8fafc;'>");
        for (String h : new String[]{"Judul Penugasan", "Unit Kerja", "Jenis Audit", "Tanggal Mulai", "Status"})
            sb.append("<th style='padding:8px 10px; text-align:left; color:#475569; font-weight:700;"
                    + " border-bottom:2px solid #e2e8f0; white-space:nowrap; font-size:11px;'>" + esc(h) + "</th>");
        sb.append("</tr>");

        for (PenugasanAuditSPI p : d.recent) {
            String st = p.getStatus();
            String sBg = PenugasanAuditSPI.DISETUJU.equals(st) ? "#dcfce7" : (PenugasanAuditSPI.DITOLAK.equals(st) ? "#fee2e2" : "#fef9c3");
            String sCl = PenugasanAuditSPI.DISETUJU.equals(st) ? "#166534" : (PenugasanAuditSPI.DITOLAK.equals(st) ? "#991b1b" : "#854d0e");
            sb.append("<tr style='border-bottom:1px solid #f1f5f9;'>")
              .append("<td style='padding:8px 10px; color:#1e293b;'>")
                .append(esc(p.getNama().length() > 40 ? p.getNama().substring(0, 39) + "…" : p.getNama())).append("</td>")
              .append("<td style='padding:8px 10px; color:#475569; font-size:11px;'>")
                .append(esc(p.getSatuanKerja() != null ? p.getSatuanKerja().getNama() : "—")).append("</td>")
              .append("<td style='padding:8px 10px; color:#475569; font-size:11px;'>")
                .append(esc(p.getJenisAuditSPI() != null ? p.getJenisAuditSPI().getNama() : "—")).append("</td>")
              .append("<td style='padding:8px 10px; color:#475569; white-space:nowrap; font-size:11px;'>")
                .append(p.getTanggalMulai() != null ? Common.dateFormat3.get().format(p.getTanggalMulai()) : "—").append("</td>")
              .append("<td style='padding:8px 10px;'><span style='border-radius:999px; padding:3px 10px;"
                    + " font-size:10px; font-weight:700; background:" + sBg + "; color:" + sCl + ";'>" + esc(st) + "</span></td>")
              .append("</tr>");
        }
        sb.append("</table></div>");
        appendHtml(panel, sb.toString());
    }

    private void renderActionPlan(Div parent, SpiData d) {
        Div panel = createPanel("Saran Tindakan Prioritas (Otomatis)", parent);
        appendHtml(panel,
            "<div style='font-size:10px; color:#64748b; margin-bottom:10px; line-height:1.4;'>"
            + "Rekomendasi berdasarkan kondisi audit saat ini — merah = prioritas utama.</div>");

        int tlPct = d.tlTotal == 0 ? 0 : d.tlSelesai * 100 / d.tlTotal;

        String sTemuan = d.jmlKritis > 0
            ? d.jmlKritis + " temuan Kritis harus segera ditindaklanjuti. Eskalasi ke pimpinan unit terkait."
            : (d.jmlMayor > 0
                ? d.jmlMayor + " temuan Mayor tercatat. Tetapkan rencana koreksi dan pantau berkala."
                : "Tidak ada temuan Kritis/Mayor — pertahankan kepatuhan yang sudah tercapai.");

        String sTl = d.tlTotal == 0
            ? "Belum ada tindak lanjut yang dicatat. Pastikan setiap temuan berat memiliki rencana tindak lanjut."
            : (d.tlTerlambat > 0
                ? d.tlTerlambat + " tindak lanjut melewati target waktu! Segera eskalasi ke pimpinan."
                : tlPct < 50 ? "Penyelesaian tindak lanjut baru " + tlPct + "%. Percepat pelaksanaan."
                             : "Tindak lanjut " + tlPct + "% selesai. Pertahankan momentum.");

        int covPct = d.covPct();
        String sCov = covPct < 50
            ? "Cakupan checklist baru " + covPct + "%. Perluas cakupan pemeriksaan pada penugasan berikutnya."
            : (covPct < 80 ? "Cakupan " + covPct + "% — masih ada checklist belum pernah diuji."
                           : "Cakupan pemeriksaan sudah baik (" + covPct + "%).");

        int hp = d.healthPct();
        String sZona = hp >= ZONA_HIJAU_MIN
            ? "Zona HIJAU (" + hp + "%). Dokumentasikan praktik baik sebagai acuan audit berikutnya."
            : (hp >= ZONA_KUNING_MIN ? "Zona KUNING (" + hp + "%). Fokus perbaikan pada unit dengan temuan Minor."
                                     : "Zona MERAH (" + hp + "%). Lakukan review menyeluruh segera!");

        String sRisiko = d.profilTinggi > 0
            ? d.profilTinggi + " unit berisiko Tinggi pada audit universe. Pastikan seluruhnya masuk PKPT tahun berjalan."
            : "Tidak ada unit berisiko Tinggi yang belum terjadwal.";

        String sPrs = d.menunggu > 0
            ? d.menunggu + " penugasan masih menunggu persetujuan. Segera tinjau agar tidak menunda pelaksanaan."
            : "Tidak ada penugasan menunggu — seluruh proses sudah diselesaikan.";

        appendHtml(panel,
            "<div style='display:flex; gap:10px; flex-wrap:wrap;'>"
            + planCard("1", "Penanganan Temuan", sTemuan, d.jmlKritis > 0 ? "high" : (d.jmlMayor > 0 ? "mid" : "low"))
            + planCard("2", "Tindak Lanjut",     sTl,     d.tlTerlambat > 0 ? "high" : (d.tlTotal == 0 ? "mid" : "low"))
            + planCard("3", "Cakupan Checklist", sCov,    covPct < 50 ? "high" : (covPct < 80 ? "mid" : "low"))
            + planCard("4", "Zona Kepatuhan",    sZona,   hp < ZONA_KUNING_MIN ? "high" : (hp < ZONA_HIJAU_MIN ? "mid" : "low"))
            + planCard("5", "Prioritas PKPT",    sRisiko, d.profilTinggi > 0 ? "mid" : "low")
            + planCard("6", "Proses Persetujuan",sPrs,    d.menunggu > 0 ? "mid" : "low")
            + "</div>");
    }

    // ================================================================
    // SVG charts + generic HTML helpers (identik pola DasboardSPMI)
    // ================================================================

    private String svgDonut(int pct, String color, String label) {
        double r = 50.0, circ = 2.0 * Math.PI * r;
        double dash = circ * pct / 100.0, gap = circ - dash;
        String da = String.format("%.2f %.2f", dash, gap);
        return "<svg viewBox='0 0 130 130' style='width:120px;height:120px;display:block;margin:auto;'"
             + " xmlns='http://www.w3.org/2000/svg'>"
             + "<circle cx='65' cy='65' r='50' fill='none' stroke='#e2e8f0' stroke-width='14'/>"
             + "<circle cx='65' cy='65' r='50' fill='none' stroke='" + color + "' stroke-width='14'"
             + " stroke-dasharray='" + da + "' stroke-linecap='round' transform='rotate(-90 65 65)'/>"
             + "<text x='65' y='60' text-anchor='middle' dominant-baseline='middle'"
             + " font-size='22' font-weight='900' fill='" + color + "'>" + pct + "%</text>"
             + "<text x='65' y='79' text-anchor='middle' font-size='9' fill='#94a3b8'>" + esc(label) + "</text>"
             + "</svg>";
    }

    private String svgTrend(List<Object[]> trend) {
        if (trend.isEmpty())
            return "<div style='text-align:center; color:#94a3b8; font-size:11px; padding:30px;'>Belum ada data tren.</div>";

        int w = 360, h = 140, padL = 32, padR = 10, padT = 14, padB = 34;
        int plotW = w - padL - padR, plotH = h - padT - padB, n = trend.size();

        int maxVal = 1;
        for (Object[] row : trend) maxVal = Math.max(maxVal, ((Number) row[1]).intValue());

        StringBuilder svg = new StringBuilder(
            "<svg viewBox='0 0 " + w + " " + h + "' style='width:100%;' xmlns='http://www.w3.org/2000/svg'>");
        svg.append("<rect x='").append(padL).append("' y='").append(padT)
           .append("' width='").append(plotW).append("' height='").append(plotH).append("' fill='#f8fafc' rx='4'/>");

        for (int lv = 1; lv <= 4; lv++) {
            int gy = padT + plotH - plotH * lv / 4;
            svg.append("<line x1='").append(padL).append("' y1='").append(gy)
               .append("' x2='").append(padL + plotW).append("' y2='").append(gy)
               .append("' stroke='#e2e8f0' stroke-width='1' stroke-dasharray='4 2'/>");
            svg.append("<text x='").append(padL - 3).append("' y='").append(gy)
               .append("' text-anchor='end' dominant-baseline='middle' font-size='8' fill='#94a3b8'>")
               .append(maxVal * lv / 4).append("</text>");
        }

        int[] px = new int[n], py = new int[n];
        for (int i = 0; i < n; i++) {
            int val = ((Number) trend.get(i)[1]).intValue();
            px[i] = padL + (n == 1 ? plotW / 2 : i * plotW / (n - 1));
            py[i] = padT + plotH - (plotH * val / maxVal);
        }

        StringBuilder area = new StringBuilder("M ").append(padL).append(" ").append(padT + plotH);
        for (int i = 0; i < n; i++) area.append(" L ").append(px[i]).append(" ").append(py[i]);
        area.append(" L ").append(padL + plotW).append(" ").append(padT + plotH).append(" Z");
        svg.append("<path d='").append(area).append("' fill='#ef444418'/>");

        StringBuilder line = new StringBuilder();
        for (int i = 0; i < n; i++) line.append(i == 0 ? "M " : " L ").append(px[i]).append(" ").append(py[i]);
        svg.append("<path d='").append(line).append("' fill='none' stroke='#ef4444' stroke-width='2.5'"
                 + " stroke-linecap='round' stroke-linejoin='round'/>");

        for (int i = 0; i < n; i++) {
            svg.append("<circle cx='").append(px[i]).append("' cy='").append(py[i])
               .append("' r='4' fill='#ef4444' stroke='#fff' stroke-width='1.5'/>");
            int val = ((Number) trend.get(i)[1]).intValue();
            svg.append("<text x='").append(px[i]).append("' y='").append(py[i] - 7)
               .append("' text-anchor='middle' font-size='8' font-weight='700' fill='#ef4444'>").append(val).append("</text>");
            String tahun = safeStr(trend.get(i)[0]);
            svg.append("<text x='").append(px[i]).append("' y='").append(h - 5)
               .append("' text-anchor='middle' font-size='8' fill='#64748b'>").append(esc(tahun)).append("</text>");
        }
        svg.append("</svg>");
        return svg.toString();
    }

    private void metricCard(Div parent, String label, int val, String sub,
                            String bg, String dark, String accent, String icon) {
        appendHtml(parent,
            "<div style='flex:1 1 130px; min-width:120px; border-radius:18px; padding:16px 14px; background:" + bg + ";'>"
            + "<div style='font-size:16px; margin-bottom:4px;'>" + icon + "</div>"
            + "<div style='font-size:9px; font-weight:700; text-transform:uppercase; letter-spacing:.09em;"
            + " color:" + dark + "; opacity:.82;'>" + esc(label) + "</div>"
            + "<div style='font-size:32px; font-weight:900; color:" + accent + "; line-height:1.05; margin-top:5px;'>" + val + "</div>"
            + "<div style='font-size:10px; color:" + dark + "; opacity:.68; margin-top:5px; line-height:1.4;'>" + esc(sub) + "</div>"
            + "</div>");
    }

    private String miniBar(String label, int pct, String color) {
        int w = Math.min(100, Math.max(0, pct));
        return "<div style='margin:5px 0;'>"
             + "<div style='display:flex; justify-content:space-between; margin-bottom:2px;'>"
             + "<span style='font-size:10px; color:#475569;'>" + esc(label) + "</span>"
             + "<span style='font-size:10px; font-weight:700; color:#1e293b;'>" + pct + "%</span></div>"
             + "<div style='height:6px; border-radius:3px; background:#e2e8f0;'>"
             + "<div style='height:6px; border-radius:3px; background:" + color + "; width:" + w + "%;'></div></div></div>";
    }

    private String miniScoreBox(String label, String val, String color) {
        return "<div style='flex:1 1 70px; border-radius:10px; padding:7px 8px; background:#f8fafc;"
             + " border:1.5px solid #e2e8f0; text-align:center;'>"
             + "<div style='font-size:14px; font-weight:900; color:" + color + ";'>" + val + "</div>"
             + "<div style='font-size:8px; color:#64748b; margin-top:2px; line-height:1.3;'>" + esc(label) + "</div></div>";
    }

    private String pctBar(String label, int val, int total, String color) {
        int pct = total == 0 ? 0 : val * 100 / total;
        return "<div style='margin:6px 0;'>"
             + "<div style='display:flex; justify-content:space-between; margin-bottom:2px;'>"
             + "<span style='font-size:11px; color:#475569;'>" + esc(label) + "</span>"
             + "<span style='font-size:11px; font-weight:700; color:#1e293b;'>" + val + " (" + pct + "%)</span></div>"
             + "<div style='height:9px; border-radius:5px; background:#f1f5f9;'>"
             + "<div style='height:9px; border-radius:5px; background:" + color + "; width:" + Math.min(100, pct) + "%;'></div></div></div>";
    }

    private String countBar(String label, int val, int max, String color) {
        int w = max == 0 ? 0 : Math.min(100, val * 100 / max);
        return "<div style='margin:5px 0;'>"
             + "<div style='display:flex; justify-content:space-between; margin-bottom:2px;'>"
             + "<span style='font-size:11px; color:#475569;'>" + esc(label) + "</span>"
             + "<span style='font-size:11px; font-weight:700; color:#1e293b;'>" + val + "</span></div>"
             + "<div style='height:8px; border-radius:4px; background:#f1f5f9;'>"
             + "<div style='height:8px; border-radius:4px; background:" + color + "; width:" + w + "%;'></div></div></div>";
    }

    private String planCard(String no, String title, String body, String urgency) {
        String bg  = "high".equals(urgency) ? "#fee2e2" : ("mid".equals(urgency) ? "#fef9c3" : "#f0fdf4");
        String clr = "high".equals(urgency) ? "#991b1b" : ("mid".equals(urgency) ? "#854d0e" : "#166534");
        String tag = "high".equals(urgency)
            ? "<span style='font-size:9px; background:#dc2626; color:#fff; border-radius:4px; padding:1px 6px; margin-left:5px;'>PRIORITAS</span>"
            : ("mid".equals(urgency)
                ? "<span style='font-size:9px; background:#f59e0b; color:#fff; border-radius:4px; padding:1px 6px; margin-left:5px;'>SEGERA</span>"
                : "");
        return "<div style='flex:1 1 180px; border-radius:14px; padding:13px 14px; background:" + bg + ";'>"
             + "<div style='font-size:9px; font-weight:900; color:" + clr + "; opacity:.65;'>SARAN " + no + tag + "</div>"
             + "<div style='font-size:12px; font-weight:800; color:" + clr + "; margin-top:4px;'>" + esc(title) + "</div>"
             + "<div style='font-size:10px; color:" + clr + "; opacity:.85; margin-top:5px; line-height:1.5;'>" + esc(body) + "</div></div>";
    }

    private Div createPanel(String title, Div parent) {
        Div panel = new Div();
        panel.setStyle("background:#fff; border-radius:18px; padding:18px 20px;"
                     + " box-shadow:0 2px 12px rgba(0,0,0,.06); margin-bottom:12px;");
        panel.setParent(parent);
        appendHtml(panel,
            "<div style='font-size:13px; font-weight:800; color:#0f172a; margin-bottom:10px;"
            + " padding-bottom:8px; border-bottom:2px solid #f1f5f9;'>" + esc(title) + "</div>");
        return panel;
    }

    private String emptyMsg() {
        return "<div style='color:#94a3b8; font-size:11px; text-align:center; padding:16px;'>"
             + "Belum ada data pada filter saat ini.</div>";
    }

    private Div flexRow() {
        Div r = new Div();
        r.setStyle("display:flex; gap:12px; flex-wrap:wrap; margin-bottom:0;");
        r.setParent(this);
        return r;
    }

    private Div colDiv(Div parent, String flex) {
        Div c = new Div();
        c.setStyle(flex + " min-width:0;");
        c.setParent(parent);
        return c;
    }

    private static String safeStr(Object o) {
        return o == null ? "" : o.toString();
    }

    private static void appendHtml(org.zkoss.zk.ui.Component parent, String html) {
        new org.zkoss.zul.Html(html).setParent(parent);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
