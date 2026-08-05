package ais.action.master.prestasi;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;

import ais.common.Common;
import ais.common.DashboardCacheUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.PrestasiDosen;
import ais.database.model.PrestasiMahasiswa;
import ais.database.model.PrestasiPegawai;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.PrestasiGuru;
import ais.database.model.sekolah.PrestasiSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Dasbor prestasi terpadu — satu kelas melayani semua peran (Siswa, Mahasiswa,
 * Dosen, Guru, Pegawai, Semua). Lingkup dikontrol via konstruktor.
 *
 * Cache L1 = DashData lokal dalam renderAll() — 1x query, n panel.
 * Cache L2 = ZK Desktop attribute, per browser-tab, TTL 5 menit.
 * Cache L3 = static ConcurrentHashMap lintas sesi, TTL 3 menit (admin saja).
 */
public class DasbordPrestasi extends Div {

    private static final long serialVersionUID = 1L;

    // ══ Lingkup ═══════════════════════════════════════════════════════════════
    public enum Lingkup {
        SEMUA, MAHASISWA, SISWA, PEGAWAI, DOSEN, GURU;

        public String getNamaModul() {
            switch (this) {
                case MAHASISWA: return "Prestasi Mahasiswa";
                case SISWA:     return "Prestasi Siswa";
                case PEGAWAI:   return "Prestasi Pegawai";
                case DOSEN:     return "Prestasi Dosen";
                case GURU:      return "Prestasi Guru";
                default:        return "Semua Prestasi";
            }
        }
        public String getEmoji() {
            switch (this) {
                case MAHASISWA: return "&#127891;";
                case SISWA:     return "&#128100;";
                case PEGAWAI:   return "&#128188;";
                case DOSEN:     return "&#128218;";
                case GURU:      return "&#9997;";
                default:        return "&#127941;";
            }
        }
    }

    // ══ Palet & konstanta ══════════════════════════════════════════════════════
    private static final String CLR_PRIMER  = "#d97706";
    private static final String CLR_SUKSES  = "#059669";
    private static final String CLR_UNGU    = "#7c3aed";
    private static final String CLR_BAHAYA  = "#dc2626";
    private static final String CLR_MUTED   = "#64748b";
    private static final String CLR_HEADER  = "#78350f";
    private static final String CLR_BG      = "#fffbeb";

    private static final String[] PALET = {
        "#d97706","var(--ais-theme-primary,#2563eb)","#16a34a","#9333ea",
        "#e11d48","#0891b2","#ea580c","#65a30d"
    };
    private static final String[] BULAN = {
        "Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agt","Sep","Okt","Nov","Des"
    };
    private static final String[] HARI = { "Min","Sen","Sel","Rab","Kam","Jum","Sab" };

    private static final int PAGE_SIZE = 15;
    private static final int MAX_ROWS  = 800;

    // ══ State komponen ═════════════════════════════════════════════════════════
    private final Lingkup lingkup;
    private Paging        pgTabel;
    private MyGrid        gridTabel;
    private List<PrsEntry> lastList = new ArrayList<PrsEntry>();
    private DashData       lastData;

    // ══ Data model: satu entri prestasi yang sudah dinormalisasi ══════════════
    static final class PrsEntry {
        final Date   tanggal;
        final String kategoriNama;
        final String cabangNama;
        final String subjekNama;     // nama orang yang berprestasi
        final String namaPrestasi;   // judul/nama kegiatan
        final String sumber;         // "Prestasi Siswa", "Prestasi Dosen", dst
        final String juara;          // "Juara 1", "Juara 2", dst
        final Integer peringkat;
        final String capaian;        // "Lokal", "Regional", "Nasional", dst
        final String tempat;
        final String penyelenggara;
        final Integer tahun;

        PrsEntry(Date tanggal, String kategoriNama, String cabangNama,
                 String subjekNama, String namaPrestasi, String sumber,
                 String juara, Integer peringkat, String capaian,
                 String tempat, String penyelenggara, Integer tahun) {
            this.tanggal       = tanggal;
            this.kategoriNama  = ss(kategoriNama);
            this.cabangNama    = ss(cabangNama);
            this.subjekNama    = ss(subjekNama);
            this.namaPrestasi  = ss(namaPrestasi);
            this.sumber        = ss(sumber);
            this.juara         = ss(juara);
            this.peringkat     = peringkat;
            this.capaian       = ss(capaian);
            this.tempat        = ss(tempat);
            this.penyelenggara = ss(penyelenggara);
            this.tahun         = tahun;
        }
    }

    // ══ Data agregat yang dimuat 1× dan dipakai semua panel (L1) ═══════════════
    static final class DashData {
        List<PrsEntry>       semua         = new ArrayList<PrsEntry>();
        Map<String, Integer> perKategori   = new LinkedHashMap<String, Integer>();
        Map<String, Integer> perCabang     = new LinkedHashMap<String, Integer>();
        Map<String, Integer> perBulan      = new LinkedHashMap<String, Integer>();
        Map<String, Integer> perHari       = new LinkedHashMap<String, Integer>();
        Map<String, Integer> perTahun      = new LinkedHashMap<String, Integer>();
        Map<String, Integer> perJuara      = new LinkedHashMap<String, Integer>();
        Map<String, Integer> perCapaian    = new LinkedHashMap<String, Integer>();
        Map<String, Integer> perPenyelenggara = new LinkedHashMap<String, Integer>();
        Map<String, Integer> topPersons    = new LinkedHashMap<String, Integer>();

        int    total, bulanIni, juara1, juara2, juara3, juaraLain;
        double rataPerBulan;
        String namaRole     = "";
        String namaPengguna = "";
    }

    // ══ Konstruktor ════════════════════════════════════════════════════════════
    public DasbordPrestasi()               { this(Lingkup.SEMUA); }
    public DasbordPrestasi(Lingkup lingkup) {
        this.lingkup = lingkup != null ? lingkup : Lingkup.SEMUA;
        setWidth("100%");
        setStyle("min-height:300px;background:" + CLR_BG
               + ";padding:12px 14px;box-sizing:border-box;overflow:auto;");
        tampilLoading();
        try {
            Common.createDefaultTimer(new EventListener() {
                public void onEvent(Event e) throws Exception { renderAll(); }
            });
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    // ── Loading spinner ────────────────────────────────────────────────────────
    private void tampilLoading() {
        Common.clear(this);
        h(this,
            "<div style='padding:80px 0;text-align:center;'>"
            + "<div style='font-size:48px;margin-bottom:16px;"
            +   "animation:dps-spin 1.2s linear infinite;display:inline-block;'>"
            +   lingkup.getEmoji() + "</div>"
            + "<div style='font-size:15px;font-weight:800;color:" + CLR_HEADER + ";'>"
            +   "Memuat " + esc(lingkup.getNamaModul()) + "&#8230;</div>"
            + "<div style='margin-top:8px;font-size:12px;color:#6b7280;'>"
            +   "Menyiapkan ringkasan, grafik, dan daftar lengkap prestasi.</div>"
            + "</div>"
            + "<style>@keyframes dps-spin{to{transform:rotate(360deg)}}</style>");
    }

    // ══ Render utama: L1 — 1x load, banyak panel ══════════════════════════════
    private void renderAll() throws Exception {
        DashData d = loadDataWithCache();
        lastData   = d;
        Common.clear(this);
        renderCss();
        renderHeader(d);
        renderKartuRingkasan(d);       // 4 kartu stat utama
        renderSorotan(d);              // insight otomatis (narasi singkat)
        renderPencapaianJuara(d);      // kartu juara 1/2/3 + lainnya
        renderTingkatKeberhasilan(d);  // gauge % juara 1 & podium
        renderTrenBulanan(d);          // bar chart 12 bulan
        renderPertumbuhanKumulatif(d); // sparkline pertumbuhan kumulatif
        renderTrenTahunan(d);          // bar chart per tahun
        renderDistribusiKategori(d);   // donut + legend
        renderSpiderCabang(d);         // spider chart cabang/bidang
        renderPolaMinggu(d);           // heatmap hari dalam seminggu
        renderDistribusiCapaian(d);    // Lokal/Regional/Nasional/Internasional
        renderDistribusiJuara(d);      // distribusi peringkat
        renderTopPersons(d);           // top 8 paling sering berprestasi
        renderTopPenyelenggara(d);     // top penyelenggara
        renderPrestasiBaru(d);         // 5 terbaru - highlight cards
        renderPrestasiUnggulan(d);     // juara 1 semua entry
        renderTabelLengkap(d);         // ZK grid + tombol download
    }

    // ══ Cache L2 + L3 ═════════════════════════════════════════════════════════
    private DashData loadDataWithCache() {
        Tbmuser user      = Common.getCurrentUser();
        Long    userId    = (user != null) ? user.getId() : null;
        boolean isPersonal = (user != null && (
                user.getSiswa()     != null ||
                user.getMahasiswa() != null ||
                user.ambilGuru()    != null ||
                user.ambilDosen()   != null ||
                user.ambilPegawai() != null));

        String cacheKey = DashboardCacheUtil.key(
                "DasbordPrestasi", lingkup.name(), isPersonal ? userId : null);

        Object fromL2 = DashboardCacheUtil.getL2(cacheKey);
        if (fromL2 instanceof DashData) return (DashData) fromL2;

        if (!isPersonal) {
            Object fromL3 = DashboardCacheUtil.getL3(cacheKey);
            if (fromL3 instanceof DashData) {
                DashboardCacheUtil.putL2(cacheKey, fromL3);
                return (DashData) fromL3;
            }
        }

        DashData d = loadData();
        DashboardCacheUtil.putL2(cacheKey, d);
        if (!isPersonal) DashboardCacheUtil.putL3(cacheKey, d);
        return d;
    }

    // ══ Muat data dari DB (hanya dipanggil saat cache miss) ════════════════════
    @SuppressWarnings("unchecked")
    private DashData loadData() {
        DashData  d    = new DashData();
        Tbmuser   user = Common.getCurrentUser();
        Mahasiswa mhs  = user != null ? user.getMahasiswa() : null;
        Dosen     dos  = user != null ? user.ambilDosen()   : null;
        Siswa     sis  = user != null ? user.getSiswa()     : null;
        Guru      gur  = user != null ? user.ambilGuru()    : null;
        Pegawai   peg  = user != null ? user.ambilPegawai() : null;

        // Tetapkan nama peran & pengguna
        if      (mhs != null) { d.namaRole = "Mahasiswa"; d.namaPengguna = ss(mhs.getNama()); }
        else if (sis != null) { d.namaRole = "Siswa";     d.namaPengguna = ss(sis.getNama()); }
        else if (peg != null) { d.namaRole = "Pegawai";   d.namaPengguna = ss(peg.getNama()); }
        else if (dos != null) { d.namaRole = "Dosen";     d.namaPengguna = ss(dos.getNama()); }
        else if (gur != null) { d.namaRole = "Guru";      d.namaPengguna = ss(gur.getNama()); }
        else {
            d.namaRole = "Administrator";
            d.namaPengguna = user != null ? ss(user.getUserNama()) : "";
        }

        // Inisialisasi slot bulan & hari kosong
        Calendar cal = Calendar.getInstance();
        for (int i = 11; i >= 0; i--) {
            Calendar t = (Calendar) cal.clone(); t.add(Calendar.MONTH, -i);
            d.perBulan.put(BULAN[t.get(Calendar.MONTH)] + " " + t.get(Calendar.YEAR), 0);
        }
        for (String h : HARI) d.perHari.put(h, 0);

        boolean all = (lingkup == Lingkup.SEMUA);
        if (all || lingkup == Lingkup.MAHASISWA)
            try { muatMahasiswa(d, mhs, dos);           } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/prestasi/DasbordPrestasi.java:296"); /* skip */ }
        if (all || lingkup == Lingkup.SISWA)
            try { muatSiswa(d, sis, gur);               } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/prestasi/DasbordPrestasi.java:298"); /* skip */ }
        if (all || lingkup == Lingkup.PEGAWAI)
            try { muatPegawai(d, peg, mhs, sis);        } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/prestasi/DasbordPrestasi.java:300"); /* skip */ }
        if (all || lingkup == Lingkup.DOSEN)
            try { muatDosen(d, dos, mhs, sis);          } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/prestasi/DasbordPrestasi.java:302"); /* skip */ }
        if (all || lingkup == Lingkup.GURU)
            try { muatGuru(d, gur, sis, mhs);           } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/prestasi/DasbordPrestasi.java:304"); /* skip */ }

        // Urutkan berdasarkan tanggal terbaru
        Collections.sort(d.semua, new Comparator<PrsEntry>() {
            public int compare(PrsEntry a, PrsEntry b) {
                if (a.tanggal == null && b.tanggal == null) return 0;
                if (a.tanggal == null) return 1;
                if (b.tanggal == null) return -1;
                return b.tanggal.compareTo(a.tanggal);
            }
        });

        // Agregasi semua field
        int nowY = cal.get(Calendar.YEAR), nowM = cal.get(Calendar.MONTH);
        for (PrsEntry e : d.semua) {
            d.total++;

            // Per kategori
            incr(d.perKategori, e.kategoriNama.isEmpty() ? "(tanpa kategori)" : e.kategoriNama);

            // Per cabang
            if (!e.cabangNama.isEmpty()) incr(d.perCabang, e.cabangNama);

            // Per bulan & per hari
            if (e.tanggal != null) {
                Calendar c = Calendar.getInstance(); c.setTime(e.tanggal);
                String bk = BULAN[c.get(Calendar.MONTH)] + " " + c.get(Calendar.YEAR);
                if (d.perBulan.containsKey(bk)) d.perBulan.put(bk, d.perBulan.get(bk) + 1);
                if (c.get(Calendar.YEAR) == nowY && c.get(Calendar.MONTH) == nowM) d.bulanIni++;
                d.perHari.put(HARI[c.get(Calendar.DAY_OF_WEEK) - 1],
                              d.perHari.get(HARI[c.get(Calendar.DAY_OF_WEEK) - 1]) + 1);
            }

            // Per tahun
            if (e.tahun != null && e.tahun > 2000) incr(d.perTahun, String.valueOf(e.tahun));

            // Per juara / peringkat
            String jr = e.juara.isEmpty()
                      ? (e.peringkat != null && e.peringkat > 0 ? "Peringkat " + e.peringkat : "(lainnya)")
                      : e.juara;
            incr(d.perJuara, jr);
            String jl = jr.toLowerCase();
            if      (jl.contains("1") && (jl.contains("juara") || jl.contains("peringkat"))) d.juara1++;
            else if (jl.contains("2") && (jl.contains("juara") || jl.contains("peringkat"))) d.juara2++;
            else if (jl.contains("3") && (jl.contains("juara") || jl.contains("peringkat"))) d.juara3++;
            else if (!jl.isEmpty()) d.juaraLain++;

            // Per capaian tingkat
            if (!e.capaian.isEmpty()) incr(d.perCapaian, e.capaian);

            // Per penyelenggara (top)
            if (!e.penyelenggara.isEmpty()) incr(d.perPenyelenggara, e.penyelenggara);

            // Top persons
            if (!e.subjekNama.isEmpty()) incr(d.topPersons, e.subjekNama);
        }

        // Rata-rata per bulan
        int sumB = 0, cntB = 0;
        for (int v : d.perBulan.values()) { if (v > 0) { sumB += v; cntB++; } }
        d.rataPerBulan = cntB > 0 ? (double) sumB / cntB : 0;

        // Urutkan topPersons by count desc
        List<Map.Entry<String, Integer>> tpList =
            new ArrayList<Map.Entry<String, Integer>>(d.topPersons.entrySet());
        Collections.sort(tpList, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });
        Map<String, Integer> tpSorted = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> en : tpList) tpSorted.put(en.getKey(), en.getValue());
        d.topPersons = tpSorted;

        lastList = d.semua;
        return d;
    }

    // ── Loader per tipe ───────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void muatMahasiswa(DashData d, Mahasiswa mhs, Dosen dos) {
        org.hibernate.Criteria c = HibernateUtil.currentSession()
                .createCriteria(PrestasiMahasiswa.class)
                .addOrder(Order.desc("id")).setMaxResults(MAX_ROWS);
        if (mhs != null)      c.add(Restrictions.eq("mahasiswa", mhs));
        else if (dos != null) c.add(Restrictions.eq("dosenPembina1", dos));
        for (PrestasiMahasiswa p : (List<PrestasiMahasiswa>) c.list()) {
            String kat  = p.getKategoriPrestasiMahasiswa() != null ? p.getKategoriPrestasiMahasiswa().getNama() : "";
            String cab  = p.getCabangPrestasiMahasiswa()   != null ? p.getCabangPrestasiMahasiswa().getNama()   : "";
            String subj = p.getMahasiswa() != null ? p.getMahasiswa().getNama() : "";
            d.semua.add(new PrsEntry(p.getTanggal(), kat, cab, subj, p.getNama(),
                "Mahasiswa", p.getJuara(), p.getPeringkat(), p.getCapaian(),
                p.getTempat(), p.getPenyelenggara(), p.getTahun()));
        }
    }

    @SuppressWarnings("unchecked")
    private void muatSiswa(DashData d, Siswa sis, Guru gur) {
        org.hibernate.Criteria c = HibernateUtil.currentSession()
                .createCriteria(PrestasiSiswa.class)
                .addOrder(Order.desc("id")).setMaxResults(MAX_ROWS);
        if (sis != null)      c.add(Restrictions.eq("siswa", sis));
        else if (gur != null) c.add(Restrictions.eq("guru", gur));
        for (PrestasiSiswa p : (List<PrestasiSiswa>) c.list()) {
            String kat  = p.getKategoriPrestasiSiswa() != null ? p.getKategoriPrestasiSiswa().getNama() : "";
            String cab  = p.getCabangPrestasiSiswa()   != null ? p.getCabangPrestasiSiswa().getNama()   : "";
            String subj = p.getSiswa() != null ? p.getSiswa().getNama() : "";
            d.semua.add(new PrsEntry(p.getTanggal(), kat, cab, subj, p.getNama(),
                "Siswa", p.getJuara(), p.getPeringkat(), p.getCapaian(),
                p.getTempat(), p.getPenyelenggara(), p.getTahun()));
        }
    }

    @SuppressWarnings("unchecked")
    private void muatPegawai(DashData d, Pegawai peg, Mahasiswa mhs, Siswa sis) {
        if (mhs != null || sis != null) return;
        org.hibernate.Criteria c = HibernateUtil.currentSession()
                .createCriteria(PrestasiPegawai.class)
                .addOrder(Order.desc("id")).setMaxResults(MAX_ROWS);
        if (peg != null) c.add(Restrictions.eq("pegawai", peg));
        for (PrestasiPegawai p : (List<PrestasiPegawai>) c.list()) {
            String kat  = p.getKategoriPrestasiPegawai() != null ? p.getKategoriPrestasiPegawai().getNama() : "";
            String cab  = p.getCabangPrestasiPegawai()   != null ? p.getCabangPrestasiPegawai().getNama()   : "";
            String subj = p.getPegawai() != null ? p.getPegawai().getNama() : "";
            d.semua.add(new PrsEntry(p.getTanggal(), kat, cab, subj, p.getNama(),
                "Pegawai", p.getJuara(), p.getPeringkat(), p.getCapaian(),
                p.getTempat(), p.getPenyelenggara(), p.getTahun()));
        }
    }

    @SuppressWarnings("unchecked")
    private void muatDosen(DashData d, Dosen dos, Mahasiswa mhs, Siswa sis) {
        if (mhs != null || sis != null) return;
        org.hibernate.Criteria c = HibernateUtil.currentSession()
                .createCriteria(PrestasiDosen.class)
                .addOrder(Order.desc("id")).setMaxResults(MAX_ROWS);
        if (dos != null) c.add(Restrictions.eq("dosen", dos));
        for (PrestasiDosen p : (List<PrestasiDosen>) c.list()) {
            String kat  = p.getKategoriPrestasiDosen() != null ? p.getKategoriPrestasiDosen().getNama() : "";
            String cab  = p.getCabangPrestasiDosen()   != null ? p.getCabangPrestasiDosen().getNama()   : "";
            String subj = p.getDosen() != null ? p.getDosen().getNama() : "";
            d.semua.add(new PrsEntry(p.getTanggal(), kat, cab, subj, p.getNama(),
                "Dosen", p.getJuara(), p.getPeringkat(), p.getCapaian(),
                p.getTempat(), p.getPenyelenggara(), p.getTahun()));
        }
    }

    @SuppressWarnings("unchecked")
    private void muatGuru(DashData d, Guru gur, Siswa sis, Mahasiswa mhs) {
        if (sis != null || mhs != null) return;
        org.hibernate.Criteria c = HibernateUtil.currentSession()
                .createCriteria(PrestasiGuru.class)
                .addOrder(Order.desc("id")).setMaxResults(MAX_ROWS);
        if (gur != null) c.add(Restrictions.eq("guru", gur));
        for (PrestasiGuru p : (List<PrestasiGuru>) c.list()) {
            String kat  = p.getKategoriPrestasiGuru() != null ? p.getKategoriPrestasiGuru().getNama() : "";
            String cab  = p.getCabangPrestasiGuru()   != null ? p.getCabangPrestasiGuru().getNama()   : "";
            String subj = p.getGuru() != null ? p.getGuru().getNama() : "";
            d.semua.add(new PrsEntry(p.getTanggal(), kat, cab, subj, p.getNama(),
                "Guru", p.getJuara(), p.getPeringkat(), p.getCapaian(),
                p.getTempat(), p.getPenyelenggara(), p.getTahun()));
        }
    }

    // ══ CSS ═══════════════════════════════════════════════════════════════════
    private void renderCss() {
        h(this,
          "<style>"
          + ":root{--dps-primer:#d97706;--dps-bg:#fffbeb;--dps-header:#78350f;}"
          + ".dps-wrap{font-family:'Segoe UI',Arial,sans-serif;}"
          // Header bar gradient
          + ".dps-header{background:linear-gradient(135deg,#78350f 0%,#d97706 100%);"
          +   "border-radius:16px;padding:18px 20px;color:#fff;margin-bottom:14px;"
          +   "box-shadow:0 4px 16px rgba(120,53,15,.28);}"
          + ".dps-header-title{font-size:20px;font-weight:900;letter-spacing:.3px;}"
          + ".dps-header-sub{font-size:12px;opacity:.85;margin-top:4px;}"
          + ".dps-header-badge{display:inline-block;background:rgba(255,255,255,.2);"
          +   "border-radius:20px;padding:2px 10px;font-size:11px;font-weight:700;}"
          + ".dps-header-num{font-size:28px;font-weight:900;opacity:.95;text-align:right;}"
          + ".dps-header-num-lbl{font-size:10px;font-weight:400;opacity:.75;}"
          // Kartu section
          + ".dps-card{background:#fff;border-radius:14px;padding:16px 18px;"
          +   "box-shadow:0 2px 10px rgba(0,0,0,.07);margin-bottom:14px;}"
          + ".dps-card-title{font-size:13px;font-weight:800;color:#78350f;"
          +   "letter-spacing:.4px;margin-bottom:3px;text-transform:uppercase;}"
          + ".dps-card-desc{font-size:11.5px;color:#64748b;margin-bottom:12px;line-height:1.5;}"
          // Grid stat card
          + ".dps-stat-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;"
          +   "margin-bottom:14px;}"
          + ".dps-sc{background:#fff;border-radius:12px;padding:14px 16px;"
          +   "box-shadow:0 2px 8px rgba(0,0,0,.06);border-top:4px solid;text-align:center;}"
          + ".dps-sc-val{font-size:26px;font-weight:800;line-height:1.1;}"
          + ".dps-sc-lbl{font-size:11px;font-weight:600;color:#64748b;margin-top:3px;}"
          + ".dps-sc-sub{font-size:10px;color:#94a3b8;margin-top:2px;}"
          // Flex grid 2 kolom
          + ".dps-g2{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-bottom:14px;}"
          + ".dps-g3{display:grid;grid-template-columns:repeat(3,1fr);gap:14px;margin-bottom:14px;}"
          // Juara highlight
          + ".dps-juara-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;"
          +   "margin-bottom:14px;}"
          + ".dps-jcard{border-radius:12px;padding:12px 10px;text-align:center;"
          +   "box-shadow:0 2px 8px rgba(0,0,0,.09);}"
          + ".dps-jcard-num{font-size:28px;font-weight:900;}"
          + ".dps-jcard-lbl{font-size:11px;font-weight:700;margin-top:4px;}"
          + ".dps-jcard-sub{font-size:10px;opacity:.75;margin-top:2px;}"
          // Bar chart
          + ".dps-bar-row{display:flex;align-items:center;gap:6px;margin-bottom:6px;font-size:11px;}"
          + ".dps-bar-label{width:60px;flex-shrink:0;color:#64748b;text-align:right;"
          +   "overflow:hidden;white-space:nowrap;text-overflow:ellipsis;}"
          + ".dps-bar-track{flex:1;background:#f1f5f9;border-radius:4px;height:18px;"
          +   "overflow:hidden;position:relative;}"
          + ".dps-bar-fill{height:100%;border-radius:4px;transition:width .5s;min-width:2px;}"
          + ".dps-bar-val{width:32px;flex-shrink:0;font-weight:700;color:#78350f;text-align:left;}"
          // Donut + legend
          + ".dps-donut-wrap{display:flex;gap:14px;align-items:flex-start;flex-wrap:wrap;}"
          + ".dps-legend{display:flex;flex-direction:column;gap:5px;flex:1;min-width:120px;}"
          + ".dps-leg-item{display:flex;align-items:center;gap:6px;font-size:11px;color:#374151;}"
          + ".dps-dot{width:11px;height:11px;border-radius:2px;flex-shrink:0;}"
          // Heatmap hari
          + ".dps-heatmap{display:grid;grid-template-columns:repeat(7,1fr);gap:5px;}"
          + ".dps-hm-cell{border-radius:6px;padding:10px 2px;text-align:center;"
          +   "font-size:10px;font-weight:700;}"
          // Note card recent
          + ".dps-note{background:#fffbeb;border-radius:10px;border-left:4px solid;"
          +   "padding:10px 12px;margin-bottom:7px;}"
          + ".dps-note-badge{font-size:10px;font-weight:700;text-transform:uppercase;"
          +   "letter-spacing:.3px;margin-bottom:3px;}"
          + ".dps-note-name{font-size:12px;font-weight:700;color:#111827;margin-bottom:2px;}"
          + ".dps-note-tgl{font-size:10px;color:#64748b;}"
          + ".dps-note-judul{font-size:11px;color:#374151;margin-top:4px;line-height:1.4;}"
          // Prestasi unggulan
          + ".dps-unggulan{background:linear-gradient(135deg,#fef9c3,#fff7ed);"
          +   "border-radius:12px;padding:12px 14px;margin-bottom:8px;"
          +   "border-left:4px solid #d97706;position:relative;overflow:hidden;}"
          + ".dps-unggulan::after{content:'\\2605';position:absolute;right:12px;top:10px;"
          +   "font-size:20px;color:#fbbf24;opacity:.5;}"
          + ".dps-unggulan-rank{font-size:10px;font-weight:800;color:#d97706;"
          +   "text-transform:uppercase;letter-spacing:.5px;}"
          + ".dps-unggulan-name{font-size:13px;font-weight:700;color:#1e293b;margin:2px 0;}"
          + ".dps-unggulan-meta{font-size:10px;color:#64748b;}"
          // Capaian badges
          + ".dps-capaian-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(120px,1fr));gap:8px;}"
          + ".dps-capaian-card{border-radius:10px;padding:12px 8px;text-align:center;"
          +   "font-size:11px;font-weight:700;}"
          // Top person row
          + ".dps-tp-row{display:flex;align-items:center;gap:8px;margin-bottom:6px;font-size:11px;}"
          + ".dps-tp-rank{width:22px;height:22px;border-radius:50%;display:flex;align-items:center;"
          +   "justify-content:center;font-size:10px;font-weight:800;flex-shrink:0;}"
          + ".dps-tp-name{flex:1;color:#1e293b;font-weight:600;}"
          + ".dps-tp-bar{flex:2;height:12px;background:#f1f5f9;border-radius:4px;overflow:hidden;}"
          + ".dps-tp-fill{height:100%;border-radius:4px;background:#d97706;}"
          + ".dps-tp-cnt{width:28px;text-align:right;font-weight:700;color:#78350f;}"
          // Sorotan otomatis (insight)
          + ".dps-sorot-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:10px;}"
          + ".dps-sorot{display:flex;align-items:center;gap:10px;border-radius:12px;padding:11px 13px;"
          +   "box-shadow:0 1px 4px rgba(0,0,0,.05);}"
          + ".dps-sorot-emoji{font-size:24px;flex-shrink:0;line-height:1;}"
          + ".dps-sorot-lbl{font-size:10.5px;color:#64748b;font-weight:600;}"
          + ".dps-sorot-val{font-size:14px;font-weight:800;margin-top:1px;line-height:1.25;}"
          // Gauge setengah lingkaran
          + ".dps-gauge-wrap{display:grid;grid-template-columns:1fr 1fr;gap:14px;align-items:end;}"
          + ".dps-gauge{text-align:center;}"
          + ".dps-gauge-lbl{font-size:11.5px;font-weight:700;color:#475569;margin-top:2px;}"
          // Responsive
          + "@media(max-width:900px){"
          +   ".dps-stat-grid{grid-template-columns:repeat(2,1fr);}"
          +   ".dps-juara-grid{grid-template-columns:repeat(2,1fr);}"
          +   ".dps-g2,.dps-g3{grid-template-columns:1fr;}"
          + "}"
          + "@media(max-width:480px){"
          +   ".dps-stat-grid{grid-template-columns:repeat(2,1fr);}"
          +   ".dps-juara-grid{grid-template-columns:repeat(2,1fr);}"
          +   ".dps-sc-val{font-size:20px;}"
          +   ".dps-heatmap{gap:3px;}"
          + "}"
          + "</style>");
    }

    // ── 1. Header ──────────────────────────────────────────────────────────────
    private void renderHeader(DashData d) {
        String tgl = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id","ID")).format(new Date());
        String badgeClr = badgeWarna(d.namaRole);
        String judul = lingkup.getNamaModul();

        h(this,
          "<div class='dps-header'>"
          + "<div style='display:flex;align-items:center;gap:14px;flex-wrap:wrap;'>"
          + "<div style='font-size:36px;'>" + lingkup.getEmoji() + "</div>"
          + "<div style='flex:1;min-width:0;'>"
          + "<div class='dps-header-title'>" + esc(judul) + "</div>"
          + "<div class='dps-header-sub'>"
          +   "Selamat datang, <b>" + esc(d.namaPengguna) + "</b>&nbsp;"
          +   "<span class='dps-header-badge' style='background:" + badgeClr + ";'>"
          +   esc(d.namaRole) + "</span>"
          +   "&nbsp;&nbsp;" + esc(tgl)
          + "</div></div>"
          + "<div class='dps-header-num'>" + d.total
          + "<div class='dps-header-num-lbl'>total prestasi</div></div>"
          + "</div></div>");
    }

    // ── 2. Kartu ringkasan (4 kartu stat utama) ────────────────────────────────
    private void renderKartuRingkasan(DashData d) {
        String rata = d.rataPerBulan < 1 ? "< 1" : String.format("%.1f", d.rataPerBulan);
        h(this,
          "<div class='dps-stat-grid'>"
          + statCard(d.total,              "Total Prestasi",      "Semua prestasi yang tercatat",       CLR_PRIMER)
          + statCard(d.bulanIni,           "Bulan Ini",           "Prestasi di bulan berjalan",          CLR_SUKSES)
          + statCardTeks(d.perKategori.size() + " kategori", "Keberagaman Kategori","Ragam bidang yang diraih", CLR_UNGU)
          + statCardTeks(rata + " / bln",  "Rata-rata",           "Rata-rata per bulan (12 bln terakhir)", CLR_PRIMER)
          + "</div>");
    }

    // ── 3. Highlight Juara 1 / 2 / 3 / Lainnya ────────────────────────────────
    private void renderPencapaianJuara(DashData d) {
        if (d.juara1 + d.juara2 + d.juara3 + d.juaraLain == 0) return;
        h(this,
          "<div style='margin-bottom:14px;'>"
          + "<div class='dps-card-title' style='margin-bottom:8px;'>&#127942; Pencapaian Juara</div>"
          + "<div class='dps-card-desc' style='margin-bottom:10px;'>Ringkasan peringkat yang berhasil diraih — Juara 1 adalah pencapaian tertinggi yang paling membanggakan.</div>"
          + "<div class='dps-juara-grid'>"
          + juaraCard("&#129351;", "Juara 1",    d.juara1,    "#fef9c3", "#78350f",  "#d97706")
          + juaraCard("&#129352;", "Juara 2",    d.juara2,    "#f1f5f9", "#1e3a5f",  "#64748b")
          + juaraCard("&#129353;", "Juara 3",    d.juara3,    "#fff7ed", "#7c2d12",  "#ea580c")
          + juaraCard("&#127941;", "Lainnya",    d.juaraLain, "#f0fdf4", "#14532d",  "#16a34a")
          + "</div></div>");
    }

    // ── Sorotan otomatis: rangkuman temuan penting dalam kalimat singkat ───────
    private void renderSorotan(DashData d) {
        if (d.total == 0) return;

        String bulanTop = ""; int bulanTopVal = 0;
        for (Map.Entry<String, Integer> en : d.perBulan.entrySet())
            if (en.getValue() > bulanTopVal) { bulanTopVal = en.getValue(); bulanTop = en.getKey(); }

        String cabangTop = ""; int cabangTopVal = 0;
        for (Map.Entry<String, Integer> en : d.perCabang.entrySet())
            if (en.getValue() > cabangTopVal) { cabangTopVal = en.getValue(); cabangTop = en.getKey(); }

        String katTop = ""; int katTopVal = 0;
        for (Map.Entry<String, Integer> en : d.perKategori.entrySet())
            if (en.getValue() > katTopVal) { katTopVal = en.getValue(); katTop = en.getKey(); }

        String tingkatTinggi = "";
        String[] urut = { "Internasional", "Nasional", "Regional", "Provinsi", "Kota", "Kabupaten", "Lokal" };
        for (int i = 0; i < urut.length && tingkatTinggi.isEmpty(); i++)
            for (String k : d.perCapaian.keySet())
                if (k.toLowerCase().contains(urut[i].toLowerCase())) { tingkatTinggi = k; break; }

        StringBuilder chips = new StringBuilder();
        if (!katTop.isEmpty())
            chips.append(sorotanChip("&#127775;", "Bidang paling sering diraih",
                esc(cut(katTop, 28)) + " (" + katTopVal + "&times;)", "#fffbeb", "#92400e"));
        if (bulanTopVal > 0)
            chips.append(sorotanChip("&#128200;", "Bulan paling produktif",
                esc(bulanTop) + " (" + bulanTopVal + " prestasi)", "#eff6ff", "var(--ais-theme-primary,#1e40af)"));
        if (d.juara1 > 0)
            chips.append(sorotanChip("&#129351;", "Juara 1 berhasil diraih",
                d.juara1 + " kali", "#fef9c3", "#854d0e"));
        if (!tingkatTinggi.isEmpty())
            chips.append(sorotanChip("&#127757;", "Tingkat tertinggi yang dicapai",
                esc(tingkatTinggi), "#f0fdf4", "#166534"));
        if (!cabangTop.isEmpty())
            chips.append(sorotanChip("&#129309;", "Cabang andalan",
                esc(cut(cabangTop, 28)), "#faf5ff", "#6b21a8"));
        if (chips.length() == 0) return;

        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#128161; Sorotan Penting</div>"
            + "<div class='dps-card-desc'>Hal-hal paling menonjol dari seluruh data, dirangkum otomatis agar cepat dipahami tanpa membaca grafik satu per satu.</div>"
            + "<div class='dps-sorot-grid'>" + chips + "</div>");
    }

    private String sorotanChip(String emoji, String label, String nilaiHtml, String bg, String clr) {
        return "<div class='dps-sorot' style='background:" + bg + ";'>"
            + "<div class='dps-sorot-emoji'>" + emoji + "</div>"
            + "<div style='min-width:0;'><div class='dps-sorot-lbl'>" + esc(label) + "</div>"
            + "<div class='dps-sorot-val' style='color:" + clr + ";'>" + nilaiHtml + "</div></div></div>";
    }

    // ── Gauge tingkat keberhasilan: % Juara 1 & % masuk podium ────────────────
    private void renderTingkatKeberhasilan(DashData d) {
        if (d.total == 0) return;
        int podium = d.juara1 + d.juara2 + d.juara3;
        int pctJ1  = (int) Math.round((double) d.juara1 * 100 / d.total);
        int pctPod = (int) Math.round((double) podium  * 100 / d.total);
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#127919; Tingkat Keberhasilan Juara</div>"
            + "<div class='dps-card-desc'>Dari semua prestasi, berapa persen yang meraih Juara 1, dan berapa persen yang masuk podium (Juara 1, 2, atau 3).</div>"
            + "<div class='dps-gauge-wrap'>"
            +   gaugeSemicircle(pctJ1,  "Juara 1",            CLR_PRIMER)
            +   gaugeSemicircle(pctPod, "Masuk Podium (1–3)", CLR_SUKSES)
            + "</div>");
    }

    private String gaugeSemicircle(int pct, String label, String clr) {
        if (pct < 0) pct = 0;
        if (pct > 100) pct = 100;
        double r = 70, cx = 90, cy = 90;
        double len  = Math.PI * r;          // panjang busur setengah lingkaran
        double fill = len * pct / 100.0;
        String path = String.format(Locale.US,
            "M %.1f,%.1f A %.1f,%.1f 0 0 1 %.1f,%.1f", cx - r, cy, r, r, cx + r, cy);
        return "<div class='dps-gauge'>"
            + "<svg viewBox='0 0 180 104' style='width:100%;max-width:210px;display:block;margin:0 auto;'"
            +   " xmlns='http://www.w3.org/2000/svg'>"
            + "<path d='" + path + "' fill='none' stroke='#f1f5f9' stroke-width='14' stroke-linecap='round'/>"
            + "<path d='" + path + "' fill='none' stroke='" + clr + "' stroke-width='14' stroke-linecap='round'"
            +   " stroke-dasharray='" + String.format(Locale.US, "%.1f", fill) + ",9999'/>"
            + "<text x='90' y='86' text-anchor='middle' font-size='30' font-weight='800' fill='" + clr + "'>"
            +   pct + "%</text></svg>"
            + "<div class='dps-gauge-lbl'>" + esc(label) + "</div></div>";
    }

    // ── Sparkline pertumbuhan kumulatif 12 bulan ──────────────────────────────
    private void renderPertumbuhanKumulatif(DashData d) {
        if (d.perBulan.isEmpty()) return;
        List<String>  labels = new ArrayList<String>(d.perBulan.keySet());
        List<Integer> kum    = new ArrayList<Integer>();
        int run = 0;
        for (String k : labels) { run += d.perBulan.get(k); kum.add(run); }
        if (run == 0) return;

        int n = kum.size();
        double W = 300, H = 70, padX = 4, padY = 6;
        double maxV = run;                  // kumulatif terakhir = nilai tertinggi
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < n; i++) {
            double x = padX + (W - 2 * padX) * (n == 1 ? 0 : (double) i / (n - 1));
            double y = (H - padY) - (H - 2 * padY) * (maxV > 0 ? (double) kum.get(i) / maxV : 0);
            line.append(i == 0 ? "M" : "L").append(String.format(Locale.US, "%.1f %.1f ", x, y));
        }
        StringBuilder area = new StringBuilder(line);
        double lastX = padX + (W - 2 * padX);
        area.append("L").append(String.format(Locale.US, "%.1f %.1f ", lastX, H - padY));
        area.append("L").append(String.format(Locale.US, "%.1f %.1f Z", padX, H - padY));

        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#128201; Pertumbuhan Total Prestasi</div>"
            + "<div class='dps-card-desc'>Jumlah prestasi yang terus ditambahkan dari bulan ke bulan — garis yang naik makin tajam berarti makin sering meraih prestasi belakangan ini.</div>"
            + "<svg viewBox='0 0 300 70' preserveAspectRatio='none' style='width:100%;height:92px;display:block;'"
            +   " xmlns='http://www.w3.org/2000/svg'>"
            + "<path d='" + area + "' fill='" + CLR_PRIMER + "' fill-opacity='.12'/>"
            + "<path d='" + line + "' fill='none' stroke='" + CLR_PRIMER + "' stroke-width='2.5'"
            +   " stroke-linejoin='round' stroke-linecap='round'/></svg>"
            + "<div style='display:flex;justify-content:space-between;font-size:10px;color:#94a3b8;margin-top:2px;'>"
            +   "<span>" + esc(labels.get(0)) + "</span>"
            +   "<span style='font-weight:700;color:#78350f;'>Total " + run + " prestasi</span>"
            +   "<span>" + esc(labels.get(n - 1)) + "</span></div>");
    }

    // ── 4. Tren 12 bulan ──────────────────────────────────────────────────────
    private void renderTrenBulanan(DashData d) {
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#128200; Tren Prestasi 12 Bulan Terakhir</div>"
            + "<div class='dps-card-desc'>Seberapa aktif prestasi dicapai setiap bulannya — bulan dengan bar lebih panjang berarti lebih banyak prestasi berhasil diraih.</div>");
        int maxV = 1;
        for (int v : d.perBulan.values()) if (v > maxV) maxV = v;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> en : d.perBulan.entrySet()) {
            int v = en.getValue();
            sb.append(barRow(en.getKey(), v, maxV, CLR_PRIMER));
        }
        h(card, sb.toString().isEmpty() ? kosong() : sb.toString());
    }

    // ── 5. Tren per tahun ─────────────────────────────────────────────────────
    private void renderTrenTahunan(DashData d) {
        if (d.perTahun.size() < 2) return;
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#128197; Perbandingan Antar Tahun</div>"
            + "<div class='dps-card-desc'>Jumlah prestasi tiap tahun, untuk melihat apakah dari tahun ke tahun makin meningkat.</div>");
        List<Map.Entry<String, Integer>> list =
            new ArrayList<Map.Entry<String, Integer>>(d.perTahun.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return a.getKey().compareTo(b.getKey());
            }
        });
        int maxV = 1;
        for (Map.Entry<String, Integer> en : list) if (en.getValue() > maxV) maxV = en.getValue();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> en : list) {
            sb.append(barRow(en.getKey(), en.getValue(), maxV, "var(--ais-theme-primary,#2563eb)"));
        }
        h(card, sb.toString().isEmpty() ? kosong() : sb.toString());
    }

    // ── 6. Distribusi kategori (donut) ────────────────────────────────────────
    private void renderDistribusiKategori(DashData d) {
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#127775; Distribusi Kategori Prestasi</div>"
            + "<div class='dps-card-desc'>Bidang prestasi mana yang paling banyak diraih — bagian lingkaran yang lebih besar berarti lebih sering.</div>");
        if (d.perKategori.isEmpty()) { h(card, kosong()); return; }
        List<Map.Entry<String, Integer>> top = topN(d.perKategori, 8);
        // conic gradient
        StringBuilder conic = new StringBuilder();
        double start = 0;
        for (int i = 0; i < top.size(); i++) {
            double pct = (double) top.get(i).getValue() / Math.max(d.total, 1) * 100;
            if (i > 0) conic.append(",");
            conic.append(PALET[i % PALET.length]).append(" ")
                 .append(String.format("%.1f", start)).append("% ")
                 .append(String.format("%.1f", start + pct)).append("%");
            start += pct;
        }
        StringBuilder legend = new StringBuilder();
        for (int i = 0; i < top.size(); i++) {
            legend.append("<div class='dps-leg-item'>"
                + "<div class='dps-dot' style='background:" + PALET[i % PALET.length] + ";'></div>"
                + "<div>" + esc(cut(top.get(i).getKey(), 26))
                + " <b>(" + top.get(i).getValue() + ")</b></div></div>");
        }
        h(card, "<div class='dps-donut-wrap'>"
            + "<div style='width:130px;height:130px;border-radius:50%;background:conic-gradient("
            + conic + ");flex-shrink:0;box-shadow:0 2px 8px rgba(0,0,0,.1);'></div>"
            + "<div class='dps-legend'>" + legend + "</div></div>");
    }

    // ── 7. Spider chart cabang/bidang ─────────────────────────────────────────
    private void renderSpiderCabang(DashData d) {
        List<Map.Entry<String, Integer>> top = topN(d.perCabang, 7);
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#129309; Radar Bidang / Cabang Prestasi</div>"
            + "<div class='dps-card-desc'>Gambaran kekuatan tiap bidang dalam bentuk jaring — ujung yang makin jauh dari pusat berarti bidang itu makin sering diraih.</div>");
        if (top.size() < 3) { h(card, kosong()); return; }
        int n = top.size();
        int maxV = 1;
        for (Map.Entry<String, Integer> e : top) if (e.getValue() > maxV) maxV = e.getValue();
        double cx = 110, cy = 110, r = 80;
        StringBuilder pts = new StringBuilder(), gridLines = new StringBuilder(), lbls = new StringBuilder();
        for (int lvl = 1; lvl <= 4; lvl++) {
            StringBuilder ring = new StringBuilder();
            for (int i = 0; i < n; i++) {
                double a = -Math.PI / 2 + 2 * Math.PI * i / n;
                double rr = r * lvl / 4;
                if (i > 0) ring.append(" ");
                ring.append(String.format("%.1f,%.1f", cx + rr * Math.cos(a), cy + rr * Math.sin(a)));
            }
            gridLines.append("<polygon points='" + ring + "' fill='none' stroke='#fef3c7' stroke-width='1'/>");
        }
        for (int i = 0; i < n; i++) {
            double a   = -Math.PI / 2 + 2 * Math.PI * i / n;
            double rat = (double) top.get(i).getValue() / maxV;
            double px  = cx + r * rat * Math.cos(a);
            double py  = cy + r * rat * Math.sin(a);
            if (i > 0) pts.append(" ");
            pts.append(String.format("%.1f,%.1f", px, py));
            double lx = cx + (r + 18) * Math.cos(a);
            double ly = cy + (r + 18) * Math.sin(a);
            lbls.append(String.format(
                "<text x='%.1f' y='%.1f' text-anchor='middle' dominant-baseline='middle'"
                + " font-size='8' fill='#78350f'>%s</text>",
                lx, ly, esc(cut(top.get(i).getKey(), 10))));
            gridLines.append(String.format("<line x1='%.1f' y1='%.1f' x2='%.1f' y2='%.1f'"
                + " stroke='#fde68a' stroke-width='1'/>", cx, cy, cx + r * Math.cos(a), cy + r * Math.sin(a)));
        }
        h(card,
          "<div style='display:flex;justify-content:center;'>"
          + "<svg viewBox='0 0 220 220' style='width:100%;max-width:220px;display:block;'"
          +   " xmlns='http://www.w3.org/2000/svg'>"
          + gridLines
          + "<polygon points='" + pts + "' fill='" + CLR_PRIMER + "' fill-opacity='.3'"
          +   " stroke='" + CLR_PRIMER + "' stroke-width='2'/>"
          + lbls
          + "</svg></div>");
    }

    // ── 8. Heatmap hari dalam seminggu ────────────────────────────────────────
    private void renderPolaMinggu(DashData d) {
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#128197; Pola Prestasi per Hari</div>"
            + "<div class='dps-card-desc'>Hari mana yang paling banyak menjadi tanggal pencapaian atau pendaftaran prestasi — membantu mengetahui ritme aktif berprestasi dalam seminggu.</div>");
        int maxH = 1;
        for (int v : d.perHari.values()) if (v > maxH) maxH = v;
        StringBuilder sb = new StringBuilder("<div class='dps-heatmap'>");
        for (Map.Entry<String, Integer> en : d.perHari.entrySet()) {
            int v = en.getValue();
            double intensity = maxH > 0 ? (double) v / maxH : 0;
            int r2 = (int)(240 + (217 - 240) * intensity);
            int g2 = (int)(240 + (119 - 240) * intensity);
            int b2 = (int)(240 + (6   - 240) * intensity);
            String bg = "rgb(" + r2 + "," + g2 + "," + b2 + ")";
            String fg = intensity > 0.55 ? "#fff" : "#374151";
            sb.append("<div class='dps-hm-cell' style='background:" + bg + ";color:" + fg + ";'"
                + " title='" + esc(en.getKey()) + ": " + v + " prestasi'>"
                + esc(en.getKey()) + "<br><b>" + v + "</b></div>");
        }
        sb.append("</div>");
        h(card, sb.toString());
    }

    // ── 9. Distribusi capaian tingkat (Lokal/Regional/Nasional/Internasional) ──
    private void renderDistribusiCapaian(DashData d) {
        if (d.perCapaian.isEmpty()) return;
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#127757; Tingkat Kompetisi Prestasi</div>"
            + "<div class='dps-card-desc'>Sebaran tingkat lomba yang diikuti, dari lokal sampai internasional.</div>");
        String[] capWarna = {"#e0f2fe","#bbf7d0","#fef9c3","#fde8d8","#ede9fe","#fce7f3"};
        String[] capTeks  = {"#0c4a6e","#14532d","#78350f","#7c2d12","#4c1d95","#831843"};
        List<Map.Entry<String, Integer>> list = topN(d.perCapaian, 6);
        StringBuilder sb = new StringBuilder("<div class='dps-capaian-grid'>");
        for (int i = 0; i < list.size(); i++) {
            String warnaBg  = capWarna[i % capWarna.length];
            String warnaTxt = capTeks[i % capTeks.length];
            sb.append("<div class='dps-capaian-card' style='background:" + warnaBg
                + ";color:" + warnaTxt + ";'>"
                + "<div style='font-size:22px;font-weight:900;'>" + list.get(i).getValue() + "</div>"
                + "<div>" + esc(list.get(i).getKey()) + "</div></div>");
        }
        sb.append("</div>");
        h(card, sb.toString());
    }

    // ── 10. Distribusi juara / peringkat ──────────────────────────────────────
    private void renderDistribusiJuara(DashData d) {
        if (d.perJuara.isEmpty()) return;
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#127939; Sebaran Peringkat yang Diraih</div>"
            + "<div class='dps-card-desc'>Perincian setiap peringkat atau kategori juara — membantu melihat sebaran tingkat keberhasilan dalam setiap kompetisi yang diikuti.</div>");
        List<Map.Entry<String, Integer>> top = topN(d.perJuara, 8);
        int maxV = 1;
        for (Map.Entry<String, Integer> en : top) if (en.getValue() > maxV) maxV = en.getValue();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < top.size(); i++) {
            sb.append(barRow(top.get(i).getKey(), top.get(i).getValue(), maxV,
                             PALET[i % PALET.length]));
        }
        h(card, sb.toString().isEmpty() ? kosong() : sb.toString());
    }

    // ── 11. Top persons ───────────────────────────────────────────────────────
    private void renderTopPersons(DashData d) {
        if (d.topPersons.isEmpty()) return;
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#127947; Individu Paling Berprestasi</div>"
            + "<div class='dps-card-desc'>Delapan orang dengan jumlah prestasi terbanyak — menampilkan siapa yang paling aktif dan konsisten dalam meraih penghargaan.</div>");
        List<Map.Entry<String, Integer>> top8 = topN(d.topPersons, 8);
        int maxV = top8.isEmpty() ? 1 : top8.get(0).getValue();
        String[] rankClr = {"#fbbf24","#94a3b8","#f97316","#86efac","#93c5fd","#c4b5fd","#fca5a5","#6ee7b7"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < top8.size(); i++) {
            String bg = rankClr[i % rankClr.length];
            int pct = maxV > 0 ? top8.get(i).getValue() * 100 / maxV : 0;
            sb.append("<div class='dps-tp-row'>"
                + "<div class='dps-tp-rank' style='background:" + bg + ";'>" + (i + 1) + "</div>"
                + "<div class='dps-tp-name'>" + esc(cut(top8.get(i).getKey(), 24)) + "</div>"
                + "<div class='dps-tp-bar'>"
                + "<div class='dps-tp-fill' style='width:" + pct + "%;background:" + PALET[i % PALET.length] + ";'></div>"
                + "</div>"
                + "<div class='dps-tp-cnt'>" + top8.get(i).getValue() + "</div>"
                + "</div>");
        }
        h(card, sb.toString().isEmpty() ? kosong() : sb.toString());
    }

    // ── 12. Top penyelenggara ─────────────────────────────────────────────────
    private void renderTopPenyelenggara(DashData d) {
        if (d.perPenyelenggara.isEmpty()) return;
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#127963; Penyelenggara Kompetisi Terbanyak</div>"
            + "<div class='dps-card-desc'>Lembaga atau institusi yang paling sering menjadi penyelenggara kompetisi di mana prestasi berhasil diraih.</div>");
        List<Map.Entry<String, Integer>> top6 = topN(d.perPenyelenggara, 6);
        int maxV = 1;
        for (Map.Entry<String, Integer> en : top6) if (en.getValue() > maxV) maxV = en.getValue();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < top6.size(); i++) {
            sb.append(barRow(top6.get(i).getKey(), top6.get(i).getValue(), maxV,
                             PALET[(i + 3) % PALET.length]));
        }
        h(card, sb.toString().isEmpty() ? kosong() : sb.toString());
    }

    // ── 13. 5 prestasi terbaru ────────────────────────────────────────────────
    private void renderPrestasiBaru(DashData d) {
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>" + lingkup.getEmoji()
            + " Prestasi Paling Baru</div>"
            + "<div class='dps-card-desc'>Lima prestasi yang paling baru tercatat — untuk memantau pencapaian terkini secara cepat tanpa harus membuka seluruh daftar.</div>");
        List<PrsEntry> lima = d.semua.size() > 5 ? d.semua.subList(0, 5) : d.semua;
        if (lima.isEmpty()) { h(card, kosong()); return; }
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", new Locale("id","ID"));
        for (int i = 0; i < lima.size(); i++) {
            PrsEntry e = lima.get(i); String clr = PALET[i % PALET.length];
            h(card,
              "<div class='dps-note' style='border-left-color:" + clr + ";'>"
              + "<div class='dps-note-badge' style='color:" + clr + ";'>"
              +   esc(e.kategoriNama) + (e.cabangNama.isEmpty() ? "" : " · " + esc(e.cabangNama))
              +   " · " + esc(e.sumber) + "</div>"
              + "<div class='dps-note-name'>" + esc(e.subjekNama) + "</div>"
              + "<div style='display:flex;gap:10px;flex-wrap:wrap;'>"
              +   "<div class='dps-note-tgl'>"
              +   (e.tanggal != null ? sdf.format(e.tanggal) : "-") + "</div>"
              +   (e.juara.isEmpty() ? "" : "<div style='font-size:10px;background:#fef9c3;"
              +     "color:#78350f;border-radius:4px;padding:1px 6px;font-weight:700;'>"
              +     esc(e.juara) + "</div>")
              +   (e.tempat.isEmpty() ? "" : "<div class='dps-note-tgl'>&#128205; " + esc(cut(e.tempat,30)) + "</div>")
              + "</div>"
              + (e.namaPrestasi.isEmpty() ? "" :
                 "<div class='dps-note-judul'>" + esc(cut(e.namaPrestasi, 120)) + "</div>")
              + "</div>");
        }
    }

    // ── 14. Prestasi unggulan (Juara 1) ──────────────────────────────────────
    private void renderPrestasiUnggulan(DashData d) {
        List<PrsEntry> unggulan = new ArrayList<PrsEntry>();
        for (PrsEntry e : d.semua) {
            String j = e.juara.toLowerCase();
            if (j.contains("1") && (j.contains("juara") || j.contains("peringkat"))) {
                unggulan.add(e);
                if (unggulan.size() >= 6) break;
            }
        }
        if (unggulan.isEmpty()) return;
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#129351; Prestasi Unggulan — Juara 1 / Peringkat 1</div>"
            + "<div class='dps-card-desc'>Daftar pencapaian terbaik berupa Juara 1 atau Peringkat 1 — inilah puncak prestasi yang berhasil diraih dan layak dibanggakan.</div>");
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", new Locale("id","ID"));
        for (PrsEntry e : unggulan) {
            h(card,
              "<div class='dps-unggulan'>"
              + "<div class='dps-unggulan-rank'>" + esc(e.juara)
              +   (e.capaian.isEmpty() ? "" : " &nbsp;&#8226;&nbsp; " + esc(e.capaian)) + "</div>"
              + "<div class='dps-unggulan-name'>" + esc(e.namaPrestasi) + "</div>"
              + "<div class='dps-unggulan-meta'>" + esc(e.subjekNama)
              +   (e.tanggal != null ? " &nbsp;&#183;&nbsp; " + sdf.format(e.tanggal) : "")
              +   (e.penyelenggara.isEmpty() ? "" : " &nbsp;&#183;&nbsp; " + esc(cut(e.penyelenggara, 30)))
              + "</div></div>");
        }
    }

    // ── 15. Tabel lengkap + download ─────────────────────────────────────────
    private void renderTabelLengkap(DashData d) {
        Div card = buatCard();
        h(card, "<div class='dps-card-title'>&#128196; Daftar Lengkap Prestasi</div>"
            + "<div class='dps-card-desc'>Semua data prestasi dalam satu tabel. Tekan tombol unduh untuk menyimpannya ke Excel.</div>");
        if (d.semua.isEmpty()) { h(card, kosong()); return; }

        // Tombol download Excel
        MyToolbarbuttonConfig btnExcel = new MyToolbarbuttonConfig();
        btnExcel.setLabel("Unduh Excel"); btnExcel.setImage("/img/excel.gif");
        btnExcel.setTooltiptext("Unduh seluruh data prestasi ke file Excel");
        btnExcel.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception { unduhExcel(); }
        });
        btnExcel.setParent(card);

        pgTabel = new Paging();
        pgTabel.setPageSize(PAGE_SIZE);
        pgTabel.setTotalSize(d.semua.size());
        pgTabel.setParent(card);

        gridTabel = new MyGrid();
        gridTabel.setWidth("100%");
        gridTabel.setSclass("dgrid");
        gridTabel.setFixedLayout(true);
        gridTabel.setParent(card);

        org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
        cols.setParent(gridTabel);
        buatKol(cols, "Tanggal",     "90px");
        buatKol(cols, "Sumber",      "90px");
        buatKol(cols, "Kategori",    "15%");
        buatKol(cols, "Cabang",      "12%");
        buatKol(cols, "Nama",        null);
        buatKol(cols, "Prestasi",    "22%");
        buatKol(cols, "Juara",       "70px");
        buatKol(cols, "Tingkat",     "80px");
        buatKol(cols, "Penyelenggara","15%");

        final List<PrsEntry> daftar = d.semua;
        Common.initPagingCustom(pgTabel, new EventListener() {
            public void onEvent(Event e) throws Exception {
                int from = pgTabel.getActivePage() * PAGE_SIZE;
                int to   = Math.min(from + PAGE_SIZE, daftar.size());
                gridTabel.setModel(new SimpleListModel(daftar.subList(from, to)));
            }
        }, PAGE_SIZE);

        gridTabel.setRowRenderer(new MyRowRenderer() {
            @Override
            public void render(Row row, Object data) throws Exception {
                PrsEntry pe = (PrsEntry) data;
                SimpleDateFormat sf = new SimpleDateFormat("dd/MM/yy", new Locale("id","ID"));
                sel(row, pe.tanggal != null ? sf.format(pe.tanggal) : "-");
                sel(row, pe.sumber);
                sel(row, pe.kategoriNama);
                sel(row, pe.cabangNama);
                sel(row, pe.subjekNama);
                sel(row, pe.namaPrestasi);
                sel(row, pe.juara);
                sel(row, pe.capaian);
                sel(row, pe.penyelenggara);
            }
        });

        int to = Math.min(PAGE_SIZE, d.semua.size());
        gridTabel.setModel(new SimpleListModel(d.semua.subList(0, to)));
    }

    // ── Download Excel (ByteArrayOutputStream — tanpa file temp) ─────────────
    private void unduhExcel() {
        try {
            if (lastData == null || lastData.semua.isEmpty()) {
                MyMessageboxConfig.show("Tidak ada data untuk diunduh.");
                return;
            }
            XSSFWorkbook wb = new XSSFWorkbook();
            // Style header
            XSSFCellStyle hStyle = wb.createCellStyle();
            hStyle.setFillForegroundColor(
                new org.apache.poi.xssf.usermodel.XSSFColor(new java.awt.Color(120, 53, 15)));
            hStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            hStyle.setAlignment(HorizontalAlignment.CENTER);
            XSSFFont hFont = wb.createFont();
            hFont.setBold(true);
            hFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            hStyle.setFont(hFont);

            // Sheet: daftar
            XSSFSheet sheet = wb.createSheet("Prestasi");
            String[] hdrs = {
                "No","Tanggal","Sumber","Kategori","Cabang",
                "Nama Individu","Nama Prestasi","Juara/Peringkat",
                "Tingkat","Penyelenggara","Tempat","Keterangan","Tahun"
            };
            XSSFRow hRow = sheet.createRow(0);
            for (int i = 0; i < hdrs.length; i++) {
                XSSFCell cell = hRow.createCell(i);
                cell.setCellValue(hdrs[i]);
                cell.setCellStyle(hStyle);
            }
            SimpleDateFormat sf = new SimpleDateFormat("dd/MM/yyyy");
            List<PrsEntry> list = lastData.semua;
            for (int i = 0; i < list.size(); i++) {
                PrsEntry pe  = list.get(i);
                XSSFRow  row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(pe.tanggal != null ? sf.format(pe.tanggal) : "");
                row.createCell(2).setCellValue(pe.sumber);
                row.createCell(3).setCellValue(pe.kategoriNama);
                row.createCell(4).setCellValue(pe.cabangNama);
                row.createCell(5).setCellValue(pe.subjekNama);
                row.createCell(6).setCellValue(pe.namaPrestasi);
                row.createCell(7).setCellValue(pe.juara);
                row.createCell(8).setCellValue(pe.capaian);
                row.createCell(9).setCellValue(pe.penyelenggara);
                row.createCell(10).setCellValue(pe.tempat);
                row.createCell(11).setCellValue("");   // keterangan - ada di model tapi tidak disimpan di entry
                row.createCell(12).setCellValue(pe.tahun != null ? pe.tahun : 0);
            }
            for (int i = 0; i < hdrs.length; i++) sheet.autoSizeColumn(i);

            // Sheet: ringkasan per kategori
            XSSFSheet shRek = wb.createSheet("Rekap Kategori");
            XSSFRow rHdr = shRek.createRow(0);
            rHdr.createCell(0).setCellValue("Kategori"); rHdr.getCell(0).setCellStyle(hStyle);
            rHdr.createCell(1).setCellValue("Jumlah");   rHdr.getCell(1).setCellStyle(hStyle);
            int rRow = 1;
            for (Map.Entry<String, Integer> en : lastData.perKategori.entrySet()) {
                XSSFRow row = shRek.createRow(rRow++);
                row.createCell(0).setCellValue(en.getKey());
                row.createCell(1).setCellValue(en.getValue());
            }
            shRek.autoSizeColumn(0); shRek.autoSizeColumn(1);

            // Tulis ke ByteArrayOutputStream — tidak perlu file temp
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            wb.close();

            String namaFile = "Dasbor_Prestasi_"
                    + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".xlsx";
            org.zkoss.zul.Filedownload.save(baos.toByteArray(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                namaFile);
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    // ══ Private helpers ════════════════════════════════════════════════════════

    private Div buatCard() {
        Div d = new Div(); d.setSclass("dps-card"); d.setParent(this); return d;
    }

    private void h(Component parent, String html) {
        Html node = new Html(); node.setContent(html); node.setParent(parent);
    }

    private void sel(Row row, String val) {
        org.zkoss.zul.Label lbl = new org.zkoss.zul.Label(val != null ? val : "");
        lbl.setParent(row);
    }

    private void buatKol(org.zkoss.zul.Columns cols, String label, String width) {
        MyColumnConfig col = new MyColumnConfig();
        col.setLabel(label);
        if (width != null) col.setWidth(width);
        col.setParent(cols);
    }

    private String statCard(int val, String label, String sub, String clr) {
        return "<div class='dps-sc' style='border-top-color:" + clr + ";'>"
            + "<div class='dps-sc-val' style='color:" + clr + ";'>" + val + "</div>"
            + "<div class='dps-sc-lbl'>" + esc(label) + "</div>"
            + "<div class='dps-sc-sub'>" + esc(sub) + "</div></div>";
    }

    private String statCardTeks(String val, String label, String sub, String clr) {
        return "<div class='dps-sc' style='border-top-color:" + clr + ";'>"
            + "<div class='dps-sc-val' style='color:" + clr + ";font-size:18px;'>" + esc(val) + "</div>"
            + "<div class='dps-sc-lbl'>" + esc(label) + "</div>"
            + "<div class='dps-sc-sub'>" + esc(sub) + "</div></div>";
    }

    private String juaraCard(String emoji, String label, int val, String bg, String clr, String dot) {
        return "<div class='dps-jcard' style='background:" + bg + ";color:" + clr + ";border-top:4px solid " + dot + ";'>"
            + "<div style='font-size:24px;'>" + emoji + "</div>"
            + "<div class='dps-jcard-num' style='color:" + dot + ";'>" + val + "</div>"
            + "<div class='dps-jcard-lbl'>" + esc(label) + "</div>"
            + "<div class='dps-jcard-sub'>pencapaian</div></div>";
    }

    private String barRow(String label, int val, int maxVal, String clr) {
        double pct = maxVal > 0 ? (double) val / maxVal * 100 : 0;
        return "<div class='dps-bar-row'>"
            + "<div class='dps-bar-label' title='" + esc(label) + "'>" + esc(cut(label, 10)) + "</div>"
            + "<div class='dps-bar-track'>"
            + "<div class='dps-bar-fill' style='width:" + String.format("%.1f", pct) + "%;background:" + clr + ";'></div>"
            + "</div>"
            + "<div class='dps-bar-val'>" + val + "</div></div>";
    }

    private String kosong() {
        return "<div style='text-align:center;padding:24px;color:#94a3b8;font-size:12px;'>"
            + "&#128270; Belum ada data untuk ditampilkan.</div>";
    }

    private static void incr(Map<String, Integer> map, String key) {
        Integer v = map.get(key);
        map.put(key, v == null ? 1 : v + 1);
    }

    private static List<Map.Entry<String, Integer>> topN(Map<String, Integer> map, int n) {
        List<Map.Entry<String, Integer>> list =
            new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });
        return list.size() > n ? list.subList(0, n) : list;
    }

    private static String badgeWarna(String role) {
        if ("Mahasiswa".equals(role)) return "rgba(14,165,233,.6)";
        if ("Siswa".equals(role))     return "rgba(139,92,246,.6)";
        if ("Pegawai".equals(role))   return "rgba(20,184,166,.6)";
        if ("Dosen".equals(role))     return "rgba(249,115,22,.6)";
        if ("Guru".equals(role))      return "rgba(34,197,94,.6)";
        return "rgba(100,116,139,.5)";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }

    private static String ss(Object o) { return o == null ? "" : o.toString().trim(); }

    private static String cut(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
