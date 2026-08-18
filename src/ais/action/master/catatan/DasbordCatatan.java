package ais.action.master.catatan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;

import ais.common.Common;
import ais.common.DashboardCacheUtil;
import ais.ui.util.MyMessageboxConfig;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CatatanAdministrasi;
import ais.database.model.CatatanMahasiswa;
import ais.database.model.CatatanPegawai;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.CatatanGuru;
import ais.database.model.sekolah.CatatanKelasSiswa;
import ais.database.model.sekolah.CatatanSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Dasbor terpadu untuk catatan — bisa menampilkan satu modul (mahasiswa/siswa/
 * pegawai/administrasi/guru/kelas siswa) atau semua sekaligus.
 * Peran pengguna menentukan filter data: mahasiswa/siswa hanya melihat dirinya,
 * dosen/guru melihat catatan yang mereka buat, admin melihat semua.
 *
 * Gunakan {@link Lingkup} pada konstruktor untuk membatasi modul yang ditampilkan.
 * Pola: extends Div, lazy-load via timer, Hibernate criteria, chart HTML/CSS/SVG.
 */
public class DasbordCatatan extends Div {

    private static final long serialVersionUID = 1L;

    // ── Lingkup (scope) — modul catatan yang ditampilkan ─────────────
    public enum Lingkup {
        SEMUA, MAHASISWA, ADMINISTRASI, PEGAWAI, SISWA, GURU, KELAS_SISWA;

        public String getNamaModul() {
            switch (this) {
                case MAHASISWA:   return "Catatan Mahasiswa";
                case ADMINISTRASI:return "Catatan Administrasi";
                case PEGAWAI:     return "Catatan Pegawai";
                case SISWA:       return "Catatan Siswa";
                case GURU:        return "Catatan Guru";
                case KELAS_SISWA: return "Catatan Kelas Siswa";
                default:          return "Semua Catatan";
            }
        }
    }

    // ── Palet warna utama ────────────────────────────────────────────────
    private static final String CLR_PRIMER    = "var(--ais-theme-primary,#1d4ed8)";
    private static final String CLR_SUKSES    = "#15803d";
    private static final String CLR_PERINGATAN= "#b45309";
    private static final String CLR_BAHAYA    = "#b91c1c";
    private static final String CLR_MUTED     = "#64748b";
    private static final String CLR_HEADER    = "#1e3a5f";
    private static final String CLR_BG        = "#f0f4f8";

    // Palet chart (8 warna berputar)
    private static final String[] PALET = {
        "var(--ais-theme-primary,#3b82f6)","#f97316","#22c55e","#a855f7",
        "#ec4899","#14b8a6","#f43f5e","#84cc16"
    };

    private static final String[] BULAN = {
        "Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agt","Sep","Okt","Nov","Des"
    };
    private static final String[] HARI_MINGGU = {
        "Min","Sen","Sel","Rab","Kam","Jum","Sab"
    };

    private static final int PAGE_SIZE = 15;
    private static final int MAX_ROWS  = 600;

    // ── State komponen ──────────────────────────────────────────────────
    private final Lingkup      lingkup;
    private Paging             pgTabel;
    private MyGrid             gridTabel;
    private List<CatatanEntry> lastList = new ArrayList<CatatanEntry>();
    private DashData           lastData;

    // ════════════════════════════════════════════════════════════════════
    // Inner: satu baris catatan yang sudah dinormalisasi dari semua tabel
    // ════════════════════════════════════════════════════════════════════
    static final class CatatanEntry {
        final Date   tanggal;
        final String jenisNama;   // JenisCatatan*.getNama()
        final String subjekNama;  // nama mahasiswa / siswa / pegawai / agenda
        final String keterangan;
        final String sumber;      // label tabel asal
        final String rawParams;   // from getParameterTambahan()
        final String tahunAjaran; // e.g. "2024/2025"
        final Integer semester;   // 1 or 2

        CatatanEntry(Date tanggal, String jenisNama,
                     String subjekNama, String keterangan, String sumber) {
            this(tanggal, jenisNama, subjekNama, keterangan, sumber, "", null, null);
        }

        CatatanEntry(Date tanggal, String jenisNama,
                     String subjekNama, String keterangan, String sumber,
                     String rawParams, String tahunAjaran, Integer semester) {
            this.tanggal    = tanggal;
            this.jenisNama  = safeStr(jenisNama);
            this.subjekNama = safeStr(subjekNama);
            this.keterangan = safeStr(keterangan);
            this.sumber     = safeStr(sumber);
            this.rawParams  = rawParams != null ? rawParams : "";
            this.tahunAjaran = tahunAjaran;
            this.semester   = semester;
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // Inner: semua data yang sudah diagregasi untuk dashboard
    // ════════════════════════════════════════════════════════════════════
    static final class DashData {
        List<CatatanEntry>   semua    = new ArrayList<CatatanEntry>();
        // perJenis: jenisNama → jumlah
        Map<String, Integer> perJenis = new LinkedHashMap<String, Integer>();
        // perBulan: "Jan 2025" → jumlah  (diisi 12 bulan terakhir sejak awal)
        Map<String, Integer> perBulan = new LinkedHashMap<String, Integer>();
        // perHari: "Min".."Sab" → jumlah
        Map<String, Integer> perHari  = new LinkedHashMap<String, Integer>();
        int    total, bulanIni;
        double rataPerBulan;
        String namaRole    = "";
        String namaPengguna = "";

        // grup → (nilai → count) — for parameter value distribution
        Map<String, Map<String, Integer>> perGrupParam = new LinkedHashMap<String, Map<String, Integer>>();
        // label → count of entries having any non-empty value for that label
        Map<String, Integer> perLabelParam = new LinkedHashMap<String, Integer>();
        int totalDenganParam;  // entries with at least one param
        int totalTanpaParam;   // entries without params
        // tahunAjaran → count
        Map<String, Integer> perTahunAjaran = new LinkedHashMap<String, Integer>();
    }

    // ════════════════════════════════════════════════════════════════════
    // Konstruktor
    // ════════════════════════════════════════════════════════════════════

    /** Tampilkan semua modul catatan (untuk dasbor global). */
    public DasbordCatatan() {
        this(Lingkup.SEMUA);
    }

    /** Tampilkan satu modul catatan sesuai lingkup (untuk tab dalam CatatanXxxAction). */
    public DasbordCatatan(Lingkup lingkup) {
        this.lingkup = lingkup != null ? lingkup : Lingkup.SEMUA;
        setWidth("100%");
        setStyle("min-height:300px;background:" + CLR_BG
               + ";padding:12px 14px;box-sizing:border-box;overflow:auto;");
        tampilLoading();
        try {
            Common.createDefaultTimer(new EventListener() {
                public void onEvent(Event e) throws Exception {
                    renderAll();
                }
            });
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // Loading
    // ────────────────────────────────────────────────────────────────────
    private void tampilLoading() {
        Common.clear(this);
        appendHtml(this,
            "<div style='padding:80px 0;text-align:center;'>"
            + "<div style='font-size:42px;margin-bottom:16px;"
            +   "animation:dc-spin 1.2s linear infinite;display:inline-block;'>&#128218;</div>"
            + "<div style='font-size:15px;font-weight:700;color:" + CLR_HEADER + ";'>"
            +   "Memuat Dasbor Catatan&#8230;</div>"
            + "<div style='margin-top:8px;font-size:12px;color:#6b7280;'>"
            +   "Menyiapkan ringkasan, grafik, dan daftar catatan Anda.</div>"
            + "</div>"
            + "<style>@keyframes dc-spin{to{transform:rotate(360deg)}}</style>");
    }

    // ════════════════════════════════════════════════════════════════════
    // Render utama — memanggil semua sub-panel secara berurutan
    // ════════════════════════════════════════════════════════════════════
    private void renderAll() throws Exception {
        DashData d = loadDataWithCache();   // L1→L2→L3→DB
        lastData = d;
        Common.clear(this);
        renderCss();
        renderHeader(d);
        renderKartuRingkasan(d);
        renderTrenBulanan(d);
        renderDistribusiDanRadar(d);
        renderPolaMinggu(d);
        renderParameterTambahanPanel(d);
        renderTrenSemester(d);
        renderCatatanTerbaru(d);
        renderTabelLengkap(d);
    }

    /**
     * L1 — sudah terwujud: satu DashData dipakai oleh semua render method.
     * L2 — ZK Desktop attribute (per browser-tab, TTL 5 menit).
     * L3 — static ConcurrentHashMap lintas sesi (hanya untuk admin, TTL 3 menit).
     */
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
                "DasbordCatatan", lingkup.name(), isPersonal ? userId : null);

        // --- L2: ZK Desktop session ----------------------------------------
        Object fromL2 = DashboardCacheUtil.getL2(cacheKey);
        if (fromL2 instanceof DashData) return (DashData) fromL2;

        // --- L3: app-wide shared (admin only) --------------------------------
        if (!isPersonal) {
            Object fromL3 = DashboardCacheUtil.getL3(cacheKey);
            if (fromL3 instanceof DashData) {
                DashboardCacheUtil.putL2(cacheKey, fromL3);   // pemanasan L2
                return (DashData) fromL3;
            }
        }

        // --- DB load ---------------------------------------------------------
        DashData d = loadData();
        DashboardCacheUtil.putL2(cacheKey, d);
        if (!isPersonal) DashboardCacheUtil.putL3(cacheKey, d);
        return d;
    }

    // ════════════════════════════════════════════════════════════════════
    // Muat data dari semua tabel catatan sesuai peran pengguna
    // ════════════════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private DashData loadData() {
        DashData d = new DashData();
        Tbmuser user = Common.getCurrentUser();

        // Deteksi peran
        Mahasiswa mhs  = user != null ? user.getMahasiswa()   : null;
        Dosen     dos  = user != null ? user.ambilDosen()     : null;
        Siswa     sis  = user != null ? user.getSiswa()       : null;
        Guru      gur  = user != null ? user.ambilGuru()      : null;
        Pegawai   peg  = user != null ? user.ambilPegawai()   : null;

        if (mhs != null) {
            d.namaRole = "Mahasiswa"; d.namaPengguna = safeStr(mhs.getNama());
        } else if (sis != null) {
            d.namaRole = "Siswa";     d.namaPengguna = safeStr(sis.getNama());
        } else if (dos != null) {
            d.namaRole = "Dosen";     d.namaPengguna = safeStr(dos.getNama());
        } else if (gur != null) {
            d.namaRole = "Guru";      d.namaPengguna = safeStr(gur.getNama());
        } else if (peg != null) {
            d.namaRole = "Pegawai";   d.namaPengguna = safeStr(peg.getNama());
        } else {
            d.namaRole     = "Administrator";
            d.namaPengguna = user != null ? safeStr(user.getUserNama()) : "";
        }

        // Pre-isi 12 bulan terakhir dengan nilai 0 (agar grafik tetap berurutan)
        Calendar cal = Calendar.getInstance();
        for (int i = 11; i >= 0; i--) {
            Calendar tmp = (Calendar) cal.clone();
            tmp.add(Calendar.MONTH, -i);
            String key = BULAN[tmp.get(Calendar.MONTH)] + " " + tmp.get(Calendar.YEAR);
            d.perBulan.put(key, 0);
        }
        // Pre-isi hari minggu
        for (String h : HARI_MINGGU) d.perHari.put(h, 0);

        // Muat tabel sesuai lingkup — error per-tabel diabaikan agar satu tabel
        // bermasalah tidak merusak keseluruhan dasbor
        boolean semuaModul = (lingkup == Lingkup.SEMUA);
        if (semuaModul || lingkup == Lingkup.MAHASISWA)
            try { muatCatatanMahasiswa(d, mhs, dos); }       catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/catatan/DasbordCatatan.java:324"); /*skip*/ }
        if (semuaModul || lingkup == Lingkup.ADMINISTRASI)
            try { muatCatatanAdministrasi(d, mhs, sis); }    catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/catatan/DasbordCatatan.java:326"); /*skip*/ }
        if (semuaModul || lingkup == Lingkup.PEGAWAI)
            try { muatCatatanPegawai(d, peg, mhs, sis); }    catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/catatan/DasbordCatatan.java:328"); /*skip*/ }
        if (semuaModul || lingkup == Lingkup.SISWA)
            try { muatCatatanSiswa(d, sis, gur); }            catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/catatan/DasbordCatatan.java:330"); /*skip*/ }
        if (semuaModul || lingkup == Lingkup.GURU)
            try { muatCatatanGuru(d, gur, sis); }             catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/catatan/DasbordCatatan.java:332"); /*skip*/ }
        if (semuaModul || lingkup == Lingkup.KELAS_SISWA)
            try { muatCatatanKelasSiswa(d, gur, sis); }       catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/catatan/DasbordCatatan.java:334"); /*skip*/ }

        // Urutkan: terbaru di atas
        Collections.sort(d.semua, new Comparator<CatatanEntry>() {
            public int compare(CatatanEntry a, CatatanEntry b) {
                if (a.tanggal == null && b.tanggal == null) return 0;
                if (a.tanggal == null) return 1;
                if (b.tanggal == null) return -1;
                return b.tanggal.compareTo(a.tanggal);
            }
        });

        // Agregasi statistik
        int nowYear  = cal.get(Calendar.YEAR);
        int nowMonth = cal.get(Calendar.MONTH);

        for (CatatanEntry e : d.semua) {
            d.total++;
            String jk = e.jenisNama.isEmpty() ? "(tanpa jenis)" : e.jenisNama;
            Integer prev = d.perJenis.get(jk);
            d.perJenis.put(jk, prev == null ? 1 : prev + 1);
            if (e.tanggal != null) {
                Calendar c = Calendar.getInstance();
                c.setTime(e.tanggal);
                String bulanKey = BULAN[c.get(Calendar.MONTH)] + " " + c.get(Calendar.YEAR);
                if (d.perBulan.containsKey(bulanKey)) {
                    d.perBulan.put(bulanKey, d.perBulan.get(bulanKey) + 1);
                }
                if (c.get(Calendar.YEAR) == nowYear && c.get(Calendar.MONTH) == nowMonth) {
                    d.bulanIni++;
                }
                String hariKey = HARI_MINGGU[c.get(Calendar.DAY_OF_WEEK) - 1];
                d.perHari.put(hariKey, d.perHari.get(hariKey) + 1);
            }

            // Parse parameter tambahan
            if (!e.rawParams.isEmpty()) {
                d.totalDenganParam++;
                for (String line : e.rawParams.split("\n")) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    // Format: "GrupNama->Label<=>Nilai<=>URL<=>NomorUrut<=>ParamId<=>KelompokId<=>Keterangan"
                    try {
                        String grupNama = "";
                        String label    = "";
                        String nilai    = "";
                        // Split pada "<=>": bagian sebelum pertama "<=>" adalah "GrupNama->Label"
                        int firstSep = line.indexOf("<=>");
                        if (firstSep > 0) {
                            String grupLabel = line.substring(0, firstSep).trim();
                            String rest      = line.substring(firstSep + 3);
                            // nilai adalah bagian pertama dari rest (sebelum "<=>")
                            int nextSep = rest.indexOf("<=>");
                            nilai = nextSep > 0 ? rest.substring(0, nextSep).trim() : rest.trim();
                            // Split grupLabel pada "->"
                            int arrowIdx = grupLabel.indexOf("->");
                            if (arrowIdx > 0) {
                                grupNama = grupLabel.substring(0, arrowIdx).trim();
                                label    = grupLabel.substring(arrowIdx + 2).trim();
                            } else {
                                grupNama = grupLabel;
                                label    = grupLabel;
                            }
                        } else {
                            // Baris tidak berformat standar — lewati
                            continue;
                        }

                        if (grupNama.isEmpty()) grupNama = "(Umum)";
                        if (label.isEmpty()) continue;

                        // Agregasi perGrupParam
                        Map<String, Integer> nilaiMap = d.perGrupParam.get(grupNama);
                        if (nilaiMap == null) {
                            nilaiMap = new LinkedHashMap<String, Integer>();
                            d.perGrupParam.put(grupNama, nilaiMap);
                        }
                        if (!nilai.isEmpty()) {
                            Integer prevNilai = nilaiMap.get(nilai);
                            nilaiMap.put(nilai, prevNilai == null ? 1 : prevNilai + 1);
                        }

                        // Agregasi perLabelParam (hanya jika ada nilai)
                        if (!nilai.isEmpty()) {
                            Integer prevLabel = d.perLabelParam.get(label);
                            d.perLabelParam.put(label, prevLabel == null ? 1 : prevLabel + 1);
                        }
                    } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/catatan/DasbordCatatan.java:421");
                        // Abaikan baris bermasalah
                    }
                }
            } else {
                d.totalTanpaParam++;
            }

            // Agregasi tahunAjaran
            if (e.tahunAjaran != null && !e.tahunAjaran.isEmpty()) {
                Integer prevTa = d.perTahunAjaran.get(e.tahunAjaran);
                d.perTahunAjaran.put(e.tahunAjaran, prevTa == null ? 1 : prevTa + 1);
            }
        }

        // Rata-rata per bulan dari 12 bulan terakhir
        int totalNonZero = 0;
        int sumBulan = 0;
        for (int v : d.perBulan.values()) {
            if (v > 0) { totalNonZero++; sumBulan += v; }
        }
        d.rataPerBulan = totalNonZero > 0 ? (double) sumBulan / totalNonZero : 0;

        lastList = d.semua;
        return d;
    }

    @SuppressWarnings("unchecked")
    private void muatCatatanMahasiswa(DashData d, Mahasiswa mhs, Dosen dos) {
        org.hibernate.Criteria c = HibernateUtil.currentSession()
                .createCriteria(CatatanMahasiswa.class)
                .addOrder(Order.desc("id")).setMaxResults(MAX_ROWS);
        if (mhs != null)      c.add(Restrictions.eq("mahasiswa", mhs));
        else if (dos != null) c.add(Restrictions.eq("dosen", dos));
        for (CatatanMahasiswa cm : (List<CatatanMahasiswa>) c.list()) {
            String jenis   = cm.getJenisCatatanMahasiswa() != null ? cm.getJenisCatatanMahasiswa().getNama() : "";
            String subjek  = cm.getNama();
            d.semua.add(new CatatanEntry(cm.getWaktu(), jenis, subjek, cm.getKeterangan(), "Catatan Mahasiswa",
                    cm.getParameterTambahan(), cm.getTahunAjaran(), cm.getSemester()));
        }
    }

    @SuppressWarnings("unchecked")
    private void muatCatatanAdministrasi(DashData d, Mahasiswa mhs, Siswa sis) {
        // Mahasiswa dan siswa tidak berwenang melihat catatan administrasi
        if (mhs != null || sis != null) return;
        org.hibernate.Criteria c = HibernateUtil.currentSession()
                .createCriteria(CatatanAdministrasi.class)
                .addOrder(Order.desc("id")).setMaxResults(MAX_ROWS);
        for (CatatanAdministrasi ca : (List<CatatanAdministrasi>) c.list()) {
            String jenis = ca.getJenisCatatanAdministrasi() != null ? ca.getJenisCatatanAdministrasi().getNama() : "";
            d.semua.add(new CatatanEntry(ca.getWaktu(), jenis, safeStr(ca.getNama()), ca.getKeterangan(), "Catatan Administrasi",
                    "", null, null));
        }
    }

    @SuppressWarnings("unchecked")
    private void muatCatatanPegawai(DashData d, Pegawai peg, Mahasiswa mhs, Siswa sis) {
        if (mhs != null || sis != null) return;
        org.hibernate.Criteria c = HibernateUtil.currentSession()
                .createCriteria(CatatanPegawai.class)
                .addOrder(Order.desc("id")).setMaxResults(MAX_ROWS);
        if (peg != null) c.add(Restrictions.eq("pegawai", peg));
        for (CatatanPegawai cp : (List<CatatanPegawai>) c.list()) {
            String jenis  = cp.getJenisCatatanPegawai() != null ? cp.getJenisCatatanPegawai().getNama() : "";
            String subjek = cp.getPegawai() != null ? cp.getPegawai().getNama() : safeStr(cp.getNama());
            d.semua.add(new CatatanEntry(cp.getWaktu(), jenis, subjek, cp.getKeterangan(), "Catatan Pegawai",
                    cp.getParameterTambahan(), null, null));
        }
    }

    @SuppressWarnings("unchecked")
    private void muatCatatanSiswa(DashData d, Siswa sis, Guru gur) {
        org.hibernate.Criteria c = HibernateUtil.currentSession()
                .createCriteria(CatatanSiswa.class)
                .addOrder(Order.desc("id")).setMaxResults(MAX_ROWS);
        if (sis != null)      c.add(Restrictions.eq("siswa", sis));
        else if (gur != null) c.add(Restrictions.eq("guru", gur));
        for (CatatanSiswa cs : (List<CatatanSiswa>) c.list()) {
            String jenis  = cs.getJenisCatatanSiswa() != null ? cs.getJenisCatatanSiswa().getNama() : "";
            String subjek = cs.getSiswa() != null ? cs.getSiswa().getNama() : "";
            d.semua.add(new CatatanEntry(cs.getWaktu(), jenis, subjek, cs.getKeterangan(), "Catatan Siswa",
                    cs.getParameterTambahan(), cs.getTahunAjaran(), cs.getSemester()));
        }
    }

    @SuppressWarnings("unchecked")
    private void muatCatatanGuru(DashData d, Guru gur, Siswa sis) {
        if (sis != null) return; // siswa tidak berwenang melihat catatan guru
        org.hibernate.Criteria c = HibernateUtil.currentSession()
                .createCriteria(CatatanGuru.class)
                .addOrder(Order.desc("id")).setMaxResults(MAX_ROWS);
        if (gur != null) c.add(Restrictions.eq("guru", gur));
        for (CatatanGuru cg : (List<CatatanGuru>) c.list()) {
            String jenis  = cg.getJenisCatatanGuru() != null ? cg.getJenisCatatanGuru().getNama() : "";
            String subjek = cg.getGuru() != null ? cg.getGuru().getNama() : "";
            d.semua.add(new CatatanEntry(cg.getWaktu(), jenis, subjek, cg.getKeterangan(), "Catatan Guru",
                    cg.getParameterTambahan(), null, null));
        }
    }

    @SuppressWarnings("unchecked")
    private void muatCatatanKelasSiswa(DashData d, Guru gur, Siswa sis) {
        if (sis != null) return; // siswa tidak berwenang melihat catatan kelas
        org.hibernate.Criteria c = HibernateUtil.currentSession()
                .createCriteria(CatatanKelasSiswa.class)
                .addOrder(Order.desc("id")).setMaxResults(MAX_ROWS);
        if (gur != null) {
            // Guru hanya melihat catatan untuk kelas yang mereka pimpin (guruPembina)
            c.createAlias("kelasSiswa", "ks", org.hibernate.Criteria.LEFT_JOIN)
             .add(Restrictions.eq("ks.guruPembina", gur));
        }
        for (CatatanKelasSiswa ck : (List<CatatanKelasSiswa>) c.list()) {
            String jenis  = ck.getJenisCatatanKelasSiswa() != null ? ck.getJenisCatatanKelasSiswa().getNama() : "";
            String subjek = ck.getKelasSiswa() != null ? ck.getKelasSiswa().getNama() : "";
            d.semua.add(new CatatanEntry(ck.getWaktu(), jenis, subjek, ck.getKeterangan(), "Catatan Kelas Siswa",
                    ck.getParameterTambahan(), null, null));
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // CSS global dashboard — dipanggil sekali sebelum semua panel
    // ════════════════════════════════════════════════════════════════════
    private void renderCss() {
        appendHtml(this,
            "<style>"
            + ".dc-card{background:#fff;border-radius:14px;padding:14px 16px;"
            +   "box-shadow:0 2px 10px rgba(0,0,0,.07);margin-bottom:14px;}"
            + ".dc-section-title{font-size:13px;font-weight:800;color:" + CLR_HEADER + ";"
            +   "letter-spacing:.4px;margin-bottom:4px;text-transform:uppercase;}"
            + ".dc-section-desc{font-size:12px;color:" + CLR_MUTED + ";margin-bottom:12px;line-height:1.5;}"
            + ".dc-stat-val{font-size:28px;font-weight:800;line-height:1.1;}"
            + ".dc-stat-label{font-size:11px;font-weight:600;color:" + CLR_MUTED + ";margin-top:2px;}"
            + ".dc-stat-sub{font-size:10px;color:#94a3b8;margin-top:3px;}"
            + ".dc-flex{display:flex;gap:10px;flex-wrap:wrap;}"
            + ".dc-col{min-width:0;flex:1 1 120px;}"
            + ".dc-stat-card{background:#fff;border-radius:12px;padding:14px 16px;"
            +   "box-shadow:0 2px 8px rgba(0,0,0,.06);border-top:4px solid;text-align:center;}"
            + ".dc-bar-row{display:flex;align-items:center;gap:8px;margin-bottom:6px;font-size:11px;}"
            + ".dc-bar-fill{height:18px;border-radius:4px;transition:width .3s;min-width:2px;}"
            + ".dc-bar-label{width:48px;flex-shrink:0;color:" + CLR_MUTED + ";text-align:right;}"
            + ".dc-bar-val{min-width:28px;font-weight:700;color:" + CLR_HEADER + ";}"
            + ".dc-legend-item{display:flex;align-items:center;gap:6px;"
            +   "font-size:11px;color:#374151;margin-bottom:4px;}"
            + ".dc-dot{width:11px;height:11px;border-radius:2px;flex-shrink:0;}"
            + ".dc-note-card{background:#f8fafc;border-radius:10px;border-left:4px solid;"
            +   "padding:10px 12px;margin-bottom:7px;}"
            + ".dc-note-jenis{font-size:10px;font-weight:700;text-transform:uppercase;"
            +   "letter-spacing:.3px;margin-bottom:4px;}"
            + ".dc-note-subjek{font-size:12px;font-weight:600;color:#111827;margin-bottom:2px;}"
            + ".dc-note-tgl{font-size:10px;color:" + CLR_MUTED + ";}"
            + ".dc-note-ket{font-size:11px;color:#374151;margin-top:4px;line-height:1.4;}"
            + ".dc-badge{display:inline-block;padding:2px 8px;border-radius:20px;"
            +   "font-size:10px;font-weight:700;}"
            + ".dc-heatmap{display:grid;grid-template-columns:repeat(7,1fr);gap:5px;}"
            + ".dc-heatmap-cell{border-radius:4px;text-align:center;padding:8px 2px;"
            +   "font-size:10px;font-weight:700;transition:transform .2s;cursor:default;}"
            + ".dc-heatmap-cell:hover{transform:scale(1.1);}"
            + ".dc-header-bar{background:linear-gradient(135deg," + CLR_HEADER + ",#2563eb);"
            +   "border-radius:14px;padding:16px 20px;color:#fff;margin-bottom:14px;"
            +   "box-shadow:0 4px 14px rgba(30,58,95,.3);}"
            + ".dc-param-grup{background:#f8fafc;border-radius:10px;border-left:3px solid var(--ais-theme-primary,#3b82f6);"
            +   "padding:10px 12px;margin-bottom:10px;}"
            + ".dc-param-grup-title{font-size:12px;font-weight:700;color:#1e3a5f;margin-bottom:6px;}"
            + ".dc-mini-bar{display:flex;align-items:center;gap:6px;margin-bottom:4px;font-size:10px;}"
            + ".dc-mini-bar-fill{height:14px;border-radius:3px;min-width:3px;}"
            + ".dc-mini-bar-label{width:80px;flex-shrink:0;color:#64748b;overflow:hidden;"
            +   "text-overflow:ellipsis;white-space:nowrap;}"
            + ".dc-mini-bar-val{min-width:24px;font-weight:700;color:#1e3a5f;font-size:10px;}"
            + ".dc-donut-wrap{position:relative;width:80px;height:80px;border-radius:50%;"
            +   "display:inline-flex;align-items:center;justify-content:center;"
            +   "font-size:14px;font-weight:800;color:" + CLR_HEADER + ";}"
            + "@media(max-width:640px){"
            +   ".dc-stat-val{font-size:22px;}"
            +   ".dc-stat-card{padding:10px 12px;}"
            +   ".dc-header-bar{padding:12px 14px;}"
            + "}"
            + "@media(max-width:480px){"
            +   ".dc-heatmap{grid-template-columns:repeat(7,1fr);}"
            +   ".dc-bar-label{width:36px;font-size:9px;}"
            +   ".dc-param-grup-title{font-size:11px;}"
            + "}"
            + "</style>");
    }

    // ════════════════════════════════════════════════════════════════════
    // Header: salam pengguna + peran + tanggal
    // ════════════════════════════════════════════════════════════════════
    private void renderHeader(DashData d) {
        String tgl = new SimpleDateFormat("EEEE, dd MMMM yyyy",
                new java.util.Locale("id", "ID")).format(new Date());
        String badgeColor = CLR_PRIMER;
        if ("Mahasiswa".equals(d.namaRole))      badgeColor = "#0ea5e9";
        else if ("Siswa".equals(d.namaRole))     badgeColor = "#8b5cf6";
        else if ("Dosen".equals(d.namaRole))     badgeColor = "#f97316";
        else if ("Guru".equals(d.namaRole))      badgeColor = "#22c55e";
        else if ("Pegawai".equals(d.namaRole))   badgeColor = "#14b8a6";

        String judulModul = lingkup == Lingkup.SEMUA ? "Dasbor Catatan" : lingkup.getNamaModul();
        appendHtml(this,
            "<div class='dc-header-bar'>"
            + "<div style='display:flex;align-items:center;gap:12px;flex-wrap:wrap;'>"
            + "<div style='font-size:32px;'>&#128218;</div>"
            + "<div style='flex:1;min-width:0;'>"
            + "<div style='font-size:18px;font-weight:800;'>" + esc(judulModul) + "</div>"
            + "<div style='font-size:12px;opacity:.85;margin-top:2px;'>"
            +   "Selamat datang, <b>" + esc(d.namaPengguna) + "</b>"
            +   " &nbsp;&#8226;&nbsp; "
            +   "<span style='background:" + badgeColor + ";padding:2px 8px;border-radius:20px;font-size:10px;font-weight:700;'>"
            +   esc(d.namaRole) + "</span>"
            +   "&nbsp;&nbsp;" + esc(tgl)
            + "</div>"
            + "</div>"
            + "<div style='text-align:right;font-size:22px;font-weight:800;opacity:.9;'>"
            +   d.total
            + "<div style='font-size:10px;font-weight:400;opacity:.75;'>total catatan</div>"
            + "</div>"
            + "</div>"
            + "</div>");
    }

    // ════════════════════════════════════════════════════════════════════
    // 5 Kartu ringkasan statistik utama (termasuk Kelengkapan)
    // ════════════════════════════════════════════════════════════════════
    private void renderKartuRingkasan(DashData d) {
        Div wrap = kartuSection(this,
            "Ringkasan Catatan",
            "Gambaran cepat jumlah catatan yang tersimpan — berapa banyak catatan Anda secara keseluruhan, bulan ini, dan rata-ratanya.");

        Div row = buatFlex(wrap);
        int jenisUnik = d.perJenis.size();
        String rataTeks = d.rataPerBulan < 1
                ? "< 1" : String.format("%.1f", d.rataPerBulan);
        int pctKelengkapan = d.total > 0 ? (d.totalDenganParam * 100 / d.total) : 0;

        buatKartu(kartuCol(row), "Total Catatan",      d.total,   CLR_HEADER,     "Semua catatan tercatat");
        buatKartu(kartuCol(row), "Bulan Ini",          d.bulanIni, CLR_SUKSES,    "Catatan di bulan berjalan");
        buatKartuTeks(kartuCol(row), "Jenis Catatan",  jenisUnik + " jenis", CLR_PERINGATAN, "Keberagaman kategori catatan");
        buatKartuTeks(kartuCol(row), "Rata-rata/Bln",  rataTeks + " catatan", CLR_PRIMER, "Rata-rata per bulan (12 bln terakhir)");
        buatKartuTeks(kartuCol(row), "Kelengkapan",    pctKelengkapan + "%", CLR_SUKSES, "Catatan dengan parameter terisi");
    }

    // ════════════════════════════════════════════════════════════════════
    // Grafik tren — bar horizontal 12 bulan terakhir
    // ════════════════════════════════════════════════════════════════════
    private void renderTrenBulanan(DashData d) {
        Div card = buatCard(this);
        appendHtml(card,
            "<div class='dc-section-title'>&#128200; Tren Catatan 12 Bulan Terakhir</div>"
            + "<div class='dc-section-desc'>"
            + "Seberapa sering catatan dibuat setiap bulan — bulan dengan batang lebih panjang berarti lebih aktif."
            + "</div>");

        if (d.total == 0) { tampilKosong(card, "Belum ada catatan untuk ditampilkan."); return; }

        int maxVal = 1;
        for (int v : d.perBulan.values()) if (v > maxVal) maxVal = v;

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='max-width:600px;'>");
        for (Map.Entry<String, Integer> e : d.perBulan.entrySet()) {
            int val  = e.getValue();
            double w = (val * 85.0) / maxVal;
            sb.append("<div class='dc-bar-row'>")
              .append("<span class='dc-bar-label'>").append(esc(e.getKey())).append("</span>")
              .append("<div class='dc-bar-fill' style='width:").append(String.format("%.1f", w))
              .append("%;background:").append(val > 0 ? CLR_PRIMER : "#e2e8f0").append(";'></div>")
              .append("<span class='dc-bar-val'>").append(val == 0 ? "—" : val).append("</span>")
              .append("</div>");
        }
        sb.append("</div>");
        appendHtml(card, sb.toString());
    }

    // ════════════════════════════════════════════════════════════════════
    // Distribusi jenis (donut) + Spider chart — side by side
    // ════════════════════════════════════════════════════════════════════
    private void renderDistribusiDanRadar(DashData d) {
        Div baris = new Div();
        baris.setStyle("display:flex;gap:12px;flex-wrap:wrap;margin-bottom:14px;");
        baris.setParent(this);

        renderDonutJenis(baris, d);
        renderSpiderJenis(baris, d);
    }

    private void renderDonutJenis(Div parent, DashData d) {
        Div card = buatCard(parent);
        card.setStyle(card.getStyle() + "flex:1 1 260px;min-width:240px;");
        appendHtml(card,
            "<div class='dc-section-title'>&#127775; Distribusi Jenis Catatan</div>"
            + "<div class='dc-section-desc'>"
            + "Perbandingan jenis-jenis catatan yang paling sering digunakan."
            + "</div>");

        if (d.perJenis.isEmpty()) { tampilKosong(card, "Tidak ada data jenis catatan."); return; }

        // Ambil top 7 + gabung sisanya sebagai "Lainnya"
        List<Map.Entry<String, Integer>> sorted = ambilTopN(d.perJenis, 7);
        int sisanya = d.total;
        for (Map.Entry<String, Integer> e : sorted) sisanya -= e.getValue();

        // Hitung conic-gradient
        StringBuilder conic   = new StringBuilder();
        StringBuilder legend  = new StringBuilder();
        double angkaDeg = 0;
        int idx = 0;
        for (Map.Entry<String, Integer> e : sorted) {
            double pct  = d.total > 0 ? (e.getValue() * 100.0 / d.total) : 0;
            double end  = angkaDeg + pct * 3.6;
            String clr  = PALET[idx % PALET.length];
            if (conic.length() > 0) conic.append(",");
            conic.append(clr).append(" ").append(String.format("%.1f", angkaDeg))
                 .append("deg ").append(String.format("%.1f", end)).append("deg");
            legend.append("<div class='dc-legend-item'>")
                  .append("<div class='dc-dot' style='background:").append(clr).append(";'></div>")
                  .append("<span>").append(esc(potong(e.getKey(), 22)))
                  .append(" <b>(").append(e.getValue()).append(")</b></span>")
                  .append("</div>");
            angkaDeg = end;
            idx++;
        }
        if (sisanya > 0) {
            double pct = sisanya * 100.0 / d.total;
            if (conic.length() > 0) conic.append(",");
            conic.append("#cbd5e1 ").append(String.format("%.1f", angkaDeg)).append("deg 360deg");
            legend.append("<div class='dc-legend-item'>")
                  .append("<div class='dc-dot' style='background:#cbd5e1;'></div>")
                  .append("<span>Lainnya <b>(").append(sisanya).append(")</b></span></div>");
        }

        appendHtml(card,
            "<div style='display:flex;gap:20px;align-items:flex-start;flex-wrap:wrap;'>"
            + "<div style='flex-shrink:0;'>"
            + "<div style='width:140px;height:140px;border-radius:50%;"
            +   "background:conic-gradient(" + conic + ");"
            +   "position:relative;margin:auto;'>"
            + "<div style='position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);"
            +   "width:70px;height:70px;border-radius:50%;background:#fff;"
            +   "display:flex;align-items:center;justify-content:center;"
            +   "font-size:13px;font-weight:800;color:" + CLR_HEADER + ";'>"
            +   d.total + "</div>"
            + "</div>"
            + "</div>"
            + "<div style='flex:1;min-width:0;margin-top:4px;'>" + legend + "</div>"
            + "</div>");
    }

    private void renderSpiderJenis(Div parent, DashData d) {
        Div card = buatCard(parent);
        card.setStyle(card.getStyle() + "flex:1 1 260px;min-width:240px;");
        appendHtml(card,
            "<div class='dc-section-title'>&#127951; Radar Kategori</div>"
            + "<div class='dc-section-desc'>"
            + "Keseimbangan antar jenis catatan — bentuk yang lebih merata berarti penggunaan lebih beragam."
            + "</div>");

        if (d.perJenis.size() < 2) {
            tampilKosong(card, "Butuh minimal 2 jenis catatan untuk menampilkan radar.");
            return;
        }

        List<Map.Entry<String, Integer>> tops = ambilTopN(d.perJenis, 6);
        int N   = tops.size();
        int maxV = 1;
        for (Map.Entry<String, Integer> e : tops) if (e.getValue() > maxV) maxV = e.getValue();

        final int CX = 120, CY = 120, R = 85;
        StringBuilder svg = new StringBuilder();
        svg.append("<svg width='240' height='240' xmlns='http://www.w3.org/2000/svg'"
                 + " style='display:block;margin:auto;'>");

        // Ring latar (25%, 50%, 75%, 100%)
        for (int ring = 1; ring <= 4; ring++) {
            double rr = R * ring / 4.0;
            StringBuilder pp = new StringBuilder();
            for (int i = 0; i < N; i++) {
                double ang = -Math.PI / 2 + (2 * Math.PI * i / N);
                double px  = CX + rr * Math.cos(ang);
                double py  = CY + rr * Math.sin(ang);
                if (i > 0) pp.append(" ");
                pp.append(String.format("%.1f,%.1f", px, py));
            }
            svg.append("<polygon points='").append(pp)
               .append("' fill='none' stroke='#e2e8f0' stroke-width='1'/>");
        }

        // Sumbu dan label
        for (int i = 0; i < N; i++) {
            double ang = -Math.PI / 2 + (2 * Math.PI * i / N);
            double ex  = CX + R * Math.cos(ang);
            double ey  = CY + R * Math.sin(ang);
            svg.append(String.format("<line x1='%.1f' y1='%.1f' x2='%.1f' y2='%.1f'"
                     + " stroke='#cbd5e1' stroke-width='1'/>", (double)CX, (double)CY, ex, ey));

            // Label di luar ring
            double lx = CX + (R + 18) * Math.cos(ang);
            double ly = CY + (R + 18) * Math.sin(ang);
            String anchor = lx < CX - 5 ? "end" : lx > CX + 5 ? "start" : "middle";
            svg.append(String.format("<text x='%.1f' y='%.1f' text-anchor='%s'"
                     + " font-size='9' fill='#64748b'>%s</text>",
                     lx, ly + 3, anchor,
                     esc(potong(tops.get(i).getKey(), 12))));
        }

        // Polygon data
        StringBuilder polyPts = new StringBuilder();
        for (int i = 0; i < N; i++) {
            double ang  = -Math.PI / 2 + (2 * Math.PI * i / N);
            double norm = (double) tops.get(i).getValue() / maxV;
            double px   = CX + R * norm * Math.cos(ang);
            double py   = CY + R * norm * Math.sin(ang);
            if (i > 0) polyPts.append(" ");
            polyPts.append(String.format("%.1f,%.1f", px, py));
        }
        svg.append("<polygon points='").append(polyPts)
           .append("' fill='rgba(59,130,246,0.25)' stroke='var(--ais-theme-primary,#3b82f6)' stroke-width='2' stroke-linejoin='round'/>");

        // Titik data
        for (int i = 0; i < N; i++) {
            double ang  = -Math.PI / 2 + (2 * Math.PI * i / N);
            double norm = (double) tops.get(i).getValue() / maxV;
            double px   = CX + R * norm * Math.cos(ang);
            double py   = CY + R * norm * Math.sin(ang);
            svg.append(String.format("<circle cx='%.1f' cy='%.1f' r='4'"
                     + " fill='#3b82f6' stroke='#fff' stroke-width='1.5'/>", px, py));
        }
        svg.append("</svg>");
        appendHtml(card, svg.toString());
    }

    // ════════════════════════════════════════════════════════════════════
    // Pola hari dalam seminggu — seberapa aktif tiap hari
    // ════════════════════════════════════════════════════════════════════
    private void renderPolaMinggu(DashData d) {
        Div card = buatCard(this);
        appendHtml(card,
            "<div class='dc-section-title'>&#128197; Pola Hari Aktif</div>"
            + "<div class='dc-section-desc'>"
            + "Hari mana dalam seminggu yang paling banyak catatan dibuat — membantu Anda memahami kebiasaan pencatatan."
            + "</div>");

        if (d.total == 0) { tampilKosong(card, "Belum ada data."); return; }

        int maxH = 1;
        for (int v : d.perHari.values()) if (v > maxH) maxH = v;

        StringBuilder sb = new StringBuilder();
        sb.append("<div class='dc-heatmap'>");
        int hIdx = 0;
        for (Map.Entry<String, Integer> e : d.perHari.entrySet()) {
            int val  = e.getValue();
            int alpha = val == 0 ? 15 : Math.max(25, (int) (val * 85.0 / maxH) + 15);
            String bg = val == 0 ? "#f1f5f9" : "rgba(29,78,216," + (alpha / 100.0) + ")";
            String txtClr = alpha > 60 ? "#fff" : CLR_HEADER;
            sb.append("<div class='dc-heatmap-cell' style='background:").append(bg)
              .append(";color:").append(txtClr).append(";' title='")
              .append(e.getKey()).append(": ").append(val).append(" catatan'>")
              .append("<div style='font-size:11px;font-weight:800;'>").append(val).append("</div>")
              .append("<div style='font-size:9px;margin-top:2px;'>").append(e.getKey()).append("</div>")
              .append("</div>");
            hIdx++;
        }
        sb.append("</div>");
        sb.append("<div style='font-size:10px;color:#94a3b8;margin-top:8px;'>"
                + "Warna lebih gelap = lebih banyak catatan di hari tersebut.</div>");
        appendHtml(card, sb.toString());
    }

    // ════════════════════════════════════════════════════════════════════
    // Panel penilaian & parameter tambahan
    // ════════════════════════════════════════════════════════════════════
    private void renderParameterTambahanPanel(DashData d) {
        Div card = buatCard(this);
        appendHtml(card,
            "<div class='dc-section-title'>&#128203; Penilaian &amp; Parameter Catatan</div>"
            + "<div class='dc-section-desc'>"
            + "Ringkasan nilai-nilai penilaian yang terisi pada catatan — membantu melihat distribusi capaian di setiap aspek."
            + "</div>");

        if (d.perGrupParam.isEmpty()) {
            // Tampilkan kelengkapan saja jika tidak ada parameter
            int pctKelengkapan = d.total > 0 ? (d.totalDenganParam * 100 / d.total) : 0;
            appendHtml(card,
                "<div style='text-align:center;color:#94a3b8;font-size:12px;padding:16px 0;'>"
                + "&#128204; Belum ada parameter penilaian yang terisi pada catatan.</div>");
            renderDonutKelengkapan(card, d, pctKelengkapan);
            return;
        }

        int pctKelengkapan = d.total > 0 ? (d.totalDenganParam * 100 / d.total) : 0;

        // Tampilkan hingga 5 grup parameter
        int grupCount = 0;
        for (Map.Entry<String, Map<String, Integer>> grupEntry : d.perGrupParam.entrySet()) {
            if (grupCount >= 5) break;
            String grupNama = grupEntry.getKey();
            Map<String, Integer> nilaiMap = grupEntry.getValue();
            if (nilaiMap.isEmpty()) continue;

            // Hitung maxVal untuk grup ini
            int maxNilai = 1;
            for (int v : nilaiMap.values()) if (v > maxNilai) maxNilai = v;

            // Warna badge grup berputar
            String grupClr = PALET[grupCount % PALET.length];

            StringBuilder grupHtml = new StringBuilder();
            grupHtml.append("<div class='dc-param-grup' style='border-left-color:").append(grupClr).append(";'>")
                    .append("<div class='dc-param-grup-title'>")
                    .append("<span style='display:inline-block;width:10px;height:10px;border-radius:50%;"
                            + "background:").append(grupClr).append(";margin-right:6px;vertical-align:middle;'></span>")
                    .append(esc(grupNama))
                    .append("</div>");

            // Ambil top 8 nilai untuk grup ini
            List<Map.Entry<String, Integer>> topNilai = ambilTopN(nilaiMap, 8);
            for (Map.Entry<String, Integer> nilaiEntry : topNilai) {
                double barW = maxNilai > 0 ? (nilaiEntry.getValue() * 80.0 / maxNilai) : 0;
                grupHtml.append("<div class='dc-mini-bar'>")
                        .append("<span class='dc-mini-bar-label' title='").append(esc(nilaiEntry.getKey())).append("'>")
                        .append(esc(potong(nilaiEntry.getKey(), 14))).append("</span>")
                        .append("<div class='dc-mini-bar-fill' style='width:").append(String.format("%.1f", barW))
                        .append("%;background:").append(grupClr).append(";'></div>")
                        .append("<span class='dc-mini-bar-val'>").append(nilaiEntry.getValue()).append("</span>")
                        .append("</div>");
            }
            grupHtml.append("</div>");
            appendHtml(card, grupHtml.toString());
            grupCount++;
        }

        // Donut kelengkapan
        renderDonutKelengkapan(card, d, pctKelengkapan);
    }

    private void renderDonutKelengkapan(Div card, DashData d, int pctKelengkapan) {
        // Donut via SVG sederhana
        int radius = 36;
        int cx = 44, cy = 44;
        double circ = 2 * Math.PI * radius;
        double dash = circ * pctKelengkapan / 100.0;

        appendHtml(card,
            "<div style='display:flex;align-items:center;gap:16px;margin-top:12px;"
            +   "padding:10px 14px;background:#f0fdf4;border-radius:10px;'>"
            + "<svg width='88' height='88' style='flex-shrink:0;'>"
            +   "<circle cx='" + cx + "' cy='" + cy + "' r='" + radius + "'"
            +     " fill='none' stroke='#e2e8f0' stroke-width='10'/>"
            +   "<circle cx='" + cx + "' cy='" + cy + "' r='" + radius + "'"
            +     " fill='none' stroke='" + CLR_SUKSES + "' stroke-width='10'"
            +     " stroke-dasharray='" + String.format("%.1f", dash) + " " + String.format("%.1f", circ) + "'"
            +     " stroke-dashoffset='" + String.format("%.1f", circ / 4) + "'"
            +     " stroke-linecap='round'/>"
            +   "<text x='" + cx + "' y='" + (cy + 5) + "' text-anchor='middle'"
            +     " font-size='14' font-weight='800' fill='" + CLR_SUKSES + "'>" + pctKelengkapan + "%</text>"
            + "</svg>"
            + "<div>"
            + "<div style='font-size:13px;font-weight:700;color:" + CLR_HEADER + ";'>Kelengkapan Isian</div>"
            + "<div style='font-size:11px;color:" + CLR_MUTED + ";margin-top:4px;'>"
            +   "<b style='color:" + CLR_SUKSES + ";'>" + d.totalDenganParam + "</b> catatan memiliki parameter"
            + "</div>"
            + "<div style='font-size:11px;color:" + CLR_MUTED + ";margin-top:2px;'>"
            +   "<b style='color:#94a3b8;'>" + d.totalTanpaParam + "</b> catatan tanpa parameter"
            + "</div>"
            + "</div>"
            + "</div>");
    }

    // ════════════════════════════════════════════════════════════════════
    // Tren semester / tahun ajaran — grouped bar chart
    // ════════════════════════════════════════════════════════════════════
    private void renderTrenSemester(DashData d) {
        if (d.perTahunAjaran.isEmpty()) return;

        Div card = buatCard(this);
        appendHtml(card,
            "<div class='dc-section-title'>&#127979; Sebaran Tahun Ajaran</div>"
            + "<div class='dc-section-desc'>"
            + "Jumlah catatan berdasarkan tahun ajaran — melihat distribusi catatan dari waktu ke waktu."
            + "</div>");

        // Urutkan tahun ajaran
        List<Map.Entry<String, Integer>> taList = new ArrayList<Map.Entry<String, Integer>>(
                d.perTahunAjaran.entrySet());
        Collections.sort(taList, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return a.getKey().compareTo(b.getKey());
            }
        });

        int maxTa = 1;
        for (Map.Entry<String, Integer> e : taList) if (e.getValue() > maxTa) maxTa = e.getValue();

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='max-width:600px;'>");
        int tIdx = 0;
        for (Map.Entry<String, Integer> e : taList) {
            int val = e.getValue();
            double w = (val * 85.0) / maxTa;
            String clr = PALET[tIdx % PALET.length];
            sb.append("<div class='dc-bar-row'>")
              .append("<span style='width:72px;flex-shrink:0;font-size:11px;color:")
              .append(CLR_MUTED).append(";text-align:right;'>").append(esc(e.getKey())).append("</span>")
              .append("<div class='dc-bar-fill' style='width:").append(String.format("%.1f", w))
              .append("%;background:").append(clr).append(";height:18px;border-radius:4px;"
              + "min-width:2px;'></div>")
              .append("<span class='dc-bar-val'>").append(val).append("</span>")
              .append("</div>");
            tIdx++;
        }
        sb.append("</div>");
        appendHtml(card, sb.toString());
    }

    // ════════════════════════════════════════════════════════════════════
    // 5 Catatan paling baru — tampil sebagai kartu ringkas
    // ════════════════════════════════════════════════════════════════════
    private void renderCatatanTerbaru(DashData d) {
        if (d.semua.isEmpty()) return;
        Div card = buatCard(this);
        appendHtml(card,
            "<div class='dc-section-title'>&#9203; Catatan Terbaru</div>"
            + "<div class='dc-section-desc'>"
            + "Lima catatan yang terakhir ditambahkan — untuk memastikan pencatatan terkini sudah benar."
            + "</div>");

        int limit = Math.min(5, d.semua.size());
        for (int i = 0; i < limit; i++) {
            CatatanEntry e  = d.semua.get(i);
            String warna    = PALET[i % PALET.length];
            String tgl      = e.tanggal != null
                    ? new SimpleDateFormat("dd MMM yyyy",new java.util.Locale("id","ID")).format(e.tanggal)
                    : "—";
            String taInfo = "";
            if (e.tahunAjaran != null && !e.tahunAjaran.isEmpty()) {
                taInfo = " &nbsp;<span style='font-size:9px;background:#f1f5f9;padding:1px 5px;"
                       + "border-radius:3px;color:" + CLR_MUTED + ";'>TA " + esc(e.tahunAjaran) + "</span>";
                if (e.semester != null) {
                    taInfo += "<span style='font-size:9px;background:#eff6ff;padding:1px 5px;"
                            + "border-radius:3px;color:var(--ais-theme-primary,#3b82f6);margin-left:3px;'>Smt " + e.semester + "</span>";
                }
            }
            appendHtml(card,
                "<div class='dc-note-card' style='border-left-color:" + warna + ";'>"
                + "<div class='dc-note-jenis' style='color:" + warna + ";'>"
                +   esc(e.jenisNama.isEmpty() ? e.sumber : e.jenisNama) + "</div>"
                + "<div class='dc-note-subjek'>" + esc(e.subjekNama.isEmpty() ? "(tanpa nama)" : e.subjekNama) + "</div>"
                + "<div style='display:flex;gap:8px;align-items:center;margin-top:2px;flex-wrap:wrap;'>"
                + "<div class='dc-note-tgl'>&#128198; " + tgl + "</div>"
                + "<div class='dc-badge' style='background:#f1f5f9;color:" + CLR_MUTED + ";'>"
                +   esc(e.sumber) + "</div>"
                + taInfo
                + "</div>"
                + (e.keterangan.isEmpty() ? "" :
                  "<div class='dc-note-ket'>" + esc(potong(e.keterangan, 120)) + "</div>")
                + "</div>");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // Tabel lengkap ZK Grid + paging + tombol unduh Excel
    // ════════════════════════════════════════════════════════════════════
    private void renderTabelLengkap(DashData d) {
        Div card = buatCard(this);
        appendHtml(card,
            "<div class='dc-section-title'>&#128203; Daftar Semua Catatan</div>"
            + "<div class='dc-section-desc'>"
            + "Seluruh catatan yang bisa Anda akses, ditampilkan per halaman. "
            + "Klik \"Unduh Excel\" untuk menyimpan seluruh data sebagai file spreadsheet."
            + "</div>");

        // Toolbar dengan tombol unduh
        Div toolbar = new Div();
        toolbar.setStyle("display:flex;justify-content:flex-end;margin-bottom:8px;");
        toolbar.setParent(card);

        MyToolbarbuttonConfig btnExcel = new MyToolbarbuttonConfig("Unduh Excel", "/img/excel.gif");
        btnExcel.setParent(toolbar);
        btnExcel.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                unduhExcel();
            }
        });

        if (d.semua.isEmpty()) {
            tampilKosong(card, "Belum ada catatan yang dapat ditampilkan.");
            return;
        }

        // Paging
        pgTabel = new Paging();
        pgTabel.setDetailed(true);
        pgTabel.setPageSize(PAGE_SIZE);
        pgTabel.setTotalSize(d.semua.size());
        pgTabel.setVisible(d.semua.size() > PAGE_SIZE);
        pgTabel.setParent(card);
        Common.initPagingCustom(pgTabel, new EventListener() {
            public void onEvent(Event e) throws Exception {
                refreshTabel();
            }
        }, PAGE_SIZE);

        // Grid
        gridTabel = new MyGrid();
        gridTabel.setSclass("dgrid");
        gridTabel.setFixedLayout(true);
        gridTabel.setWidth("100%");
        gridTabel.setParent(card);

        org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
        cols.setParent(gridTabel);
        buatKolom(cols, "No",           "32px");
        buatKolom(cols, "Tanggal",      "12%");
        buatKolom(cols, "Jenis",        "15%");
        buatKolom(cols, "Nama/Subjek",  "17%");
        buatKolom(cols, "Keterangan",   null);
        buatKolom(cols, "Sumber",       "13%");
        buatKolom(cols, "Tahun Ajaran", "10%");

        refreshTabel();
    }

    private void refreshTabel() {
        if (gridTabel == null || lastList == null) return;
        int page = pgTabel != null ? pgTabel.getActivePage() : 0;
        int from = page * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, lastList.size());
        List<CatatanEntry> sub = from >= lastList.size()
                ? new ArrayList<CatatanEntry>()
                : lastList.subList(from, to);
        gridTabel.setRowRenderer(new TabelRenderer(from));
        gridTabel.setModelCheckMobile(new SimpleListModel(sub));
    }

    private class TabelRenderer extends MyRowRenderer {
        private int baseNo;
        TabelRenderer(int baseNo) { this.baseNo = baseNo; }

        public void render(Row row, Object obj) throws Exception {
            row.setValign("middle");
            CatatanEntry e = (CatatanEntry) obj;
            baseNo++;

            // No
            buatSel(row, String.valueOf(baseNo), "text-align:center;color:" + CLR_MUTED + ";font-size:11px;");
            // Tanggal
            String tgl = e.tanggal != null
                    ? new SimpleDateFormat("dd/MM/yyyy HH:mm").format(e.tanggal) : "—";
            buatSel(row, tgl, "font-size:11px;color:" + CLR_MUTED + ";white-space:nowrap;");
            // Jenis
            buatSel(row, e.jenisNama.isEmpty() ? "—" : e.jenisNama, "font-size:11px;font-weight:600;");
            // Nama/Subjek
            buatSel(row, e.subjekNama.isEmpty() ? "—" : e.subjekNama, "font-size:11px;");
            // Keterangan
            buatSel(row, potong(e.keterangan, 80), "font-size:11px;color:#374151;");
            // Sumber
            buatSel(row, e.sumber, "font-size:10px;color:#fff;background:" + warnaSource(e.sumber)
                    + ";border-radius:4px;padding:2px 6px;white-space:nowrap;font-weight:600;");
            // Tahun Ajaran
            String taVal = "";
            if (e.tahunAjaran != null && !e.tahunAjaran.isEmpty()) {
                taVal = e.tahunAjaran;
                if (e.semester != null) {
                    taVal += " / Smt " + e.semester;
                }
            }
            buatSel(row, taVal.isEmpty() ? "—" : taVal,
                    "font-size:10px;color:" + CLR_MUTED + ";white-space:nowrap;");
        }

        private void buatSel(Row row, String val, String style) {
            org.zkoss.zul.Label lbl = new org.zkoss.zul.Label(val == null ? "" : val);
            org.zkoss.zul.Cell cell = new org.zkoss.zul.Cell();
            cell.setStyle(style);
            cell.appendChild(lbl);
            cell.setParent(row);
        }
    }

    private static String warnaSource(String s) {
        if (s == null) return CLR_MUTED;
        if (s.contains("Mahasiswa"))  return "#0ea5e9";
        if (s.contains("Siswa"))      return "#8b5cf6";
        if (s.contains("Pegawai"))    return "#14b8a6";
        if (s.contains("Guru"))       return "#22c55e";
        if (s.contains("Administrasi")) return "#f97316";
        return CLR_MUTED;
    }

    // ════════════════════════════════════════════════════════════════════
    // Unduh Excel — Apache POI
    // ════════════════════════════════════════════════════════════════════
    private void unduhExcel() {
        if (lastList == null || lastList.isEmpty()) {
            try { MyMessageboxConfig.show("Mohon maaf, saat ini tidak ada data yang dapat diunduh. Silakan lakukan pemuatan data terlebih dahulu hingga data tampil, kemudian coba kembali."); }
            catch (Exception ie) { ais.common.Common.tampilErrorJikaAdmin(ie); }
            return;
        }
        try {
            String namaFile = "catatan_dasbor_" + System.currentTimeMillis() + ".xlsx";
            String path = Executions.getCurrent().getDesktop().getWebApp()
                    .getRealPath("/tmp/" + namaFile);
            File file = new File(path);
            file.getParentFile().mkdirs();
            file.createNewFile();

            XSSFWorkbook wb = new XSSFWorkbook();
            XSSFSheet    sh = wb.createSheet("Catatan");
            sh.setDefaultColumnWidth(20);

            // Style header — latar biru tua, teks putih, bold
            XSSFCellStyle hStyle = wb.createCellStyle();
            XSSFFont      hFont  = wb.createFont();
            hFont.setBold(true);
            hFont.setFontHeightInPoints((short) 10);
            hFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            hStyle.setFont(hFont);
            hStyle.setAlignment(HorizontalAlignment.CENTER);
            hStyle.setFillForegroundColor(
                new org.apache.poi.xssf.usermodel.XSSFColor(new java.awt.Color(30, 58, 95)));
            hStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Style data — font kecil
            XSSFCellStyle dStyle = wb.createCellStyle();
            XSSFFont      dFont  = wb.createFont();
            dFont.setFontHeightInPoints((short) 9);
            dStyle.setFont(dFont);

            // Baris header (tambah Tahun Ajaran & Semester)
            String[] headers = {"No","Tanggal","Jenis Catatan","Nama / Subjek","Keterangan","Sumber Data","Tahun Ajaran","Semester"};
            XSSFRow hRow = sh.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                XSSFCell cell = hRow.createCell(c);
                cell.setCellValue(headers[c]);
                cell.setCellStyle(hStyle);
            }
            // Lebar kolom khusus
            sh.setColumnWidth(1, 5500); sh.setColumnWidth(4, 12000);
            sh.setColumnWidth(6, 4000); sh.setColumnWidth(7, 3000);

            // Baris data
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            for (int r = 0; r < lastList.size(); r++) {
                CatatanEntry e   = lastList.get(r);
                XSSFRow      row = sh.createRow(r + 1);
                tulis(row, 0, String.valueOf(r + 1),                        dStyle);
                tulis(row, 1, e.tanggal != null ? sdf.format(e.tanggal):"", dStyle);
                tulis(row, 2, e.jenisNama,                                  dStyle);
                tulis(row, 3, e.subjekNama,                                 dStyle);
                tulis(row, 4, e.keterangan,                                 dStyle);
                tulis(row, 5, e.sumber,                                     dStyle);
                tulis(row, 6, e.tahunAjaran != null ? e.tahunAjaran : "",   dStyle);
                tulis(row, 7, e.semester != null ? e.semester.toString() : "", dStyle);
            }

            FileOutputStream fos = new FileOutputStream(file);
            wb.write(fos); fos.close(); wb.close();

            org.zkoss.zul.Filedownload.save(
                new FileInputStream(file),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "Dasbor_Catatan_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xlsx");
            file.delete();
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    private static void tulis(XSSFRow row, int col, String val, XSSFCellStyle style) {
        XSSFCell cell = row.createCell(col);
        cell.setCellValue(val == null ? "" : val);
        cell.setCellStyle(style);
    }

    // ════════════════════════════════════════════════════════════════════
    // Helper: pembuatan komponen ZK yang dapat dipakai ulang
    // ════════════════════════════════════════════════════════════════════

    /** Membuat div kartu putih dengan shadow. */
    private static Div buatCard(Component parent) {
        Div d = new Div();
        d.setSclass("dc-card");
        d.setParent(parent);
        return d;
    }

    /** Membuat div kartu dengan tambahan wrapper & judul seksi. */
    private static Div kartuSection(Component parent, String judul, String deskripsi) {
        Div card = buatCard(parent);
        appendHtml(card,
            "<div class='dc-section-title'>" + esc(judul) + "</div>"
            + "<div class='dc-section-desc'>" + esc(deskripsi) + "</div>");
        return card;
    }

    /** Flex row untuk menampung kartu-kartu statistik. */
    private static Div buatFlex(Component parent) {
        Div d = new Div();
        d.setStyle("display:flex;gap:10px;flex-wrap:wrap;");
        d.setParent(parent);
        return d;
    }

    /** Kolom fleksibel di dalam flex row. */
    private static Div kartuCol(Div parent) {
        Div d = new Div();
        d.setStyle("flex:1 1 120px;min-width:110px;");
        d.setParent(parent);
        return d;
    }

    /** Kartu statistik dengan nilai angka. */
    private static void buatKartu(Div parent, String judul, int nilai, String warna, String sub) {
        appendHtml(parent,
            "<div class='dc-stat-card' style='border-top-color:" + warna + ";'>"
            + "<div class='dc-stat-val' style='color:" + warna + ";'>" + nilai + "</div>"
            + "<div class='dc-stat-label'>" + esc(judul) + "</div>"
            + "<div class='dc-stat-sub'>" + esc(sub) + "</div>"
            + "</div>");
    }

    /** Kartu statistik dengan nilai teks (untuk rata-rata, jenis, dll.). */
    private static void buatKartuTeks(Div parent, String judul, String nilai, String warna, String sub) {
        appendHtml(parent,
            "<div class='dc-stat-card' style='border-top-color:" + warna + ";'>"
            + "<div class='dc-stat-val' style='color:" + warna + ";font-size:20px;'>" + esc(nilai) + "</div>"
            + "<div class='dc-stat-label'>" + esc(judul) + "</div>"
            + "<div class='dc-stat-sub'>" + esc(sub) + "</div>"
            + "</div>");
    }

    /** Pesan kosong di dalam kartu. */
    private static void tampilKosong(Component parent, String pesan) {
        appendHtml(parent,
            "<div style='text-align:center;color:#94a3b8;font-size:12px;"
            + "padding:24px 0;'>&#128204; " + esc(pesan) + "</div>");
    }

    /** Menambahkan kolom ke grid. */
    private static void buatKolom(org.zkoss.zul.Columns parent, String label, String width) {
        MyColumnConfig col = new MyColumnConfig();
        col.setLabel(label);
        if (width != null) col.setWidth(width);
        col.setParent(parent);
    }

    // ════════════════════════════════════════════════════════════════════
    // Helper: data / format
    // ════════════════════════════════════════════════════════════════════

    /** Mengambil top-N entry dari map berdasarkan nilai tertinggi. */
    private static List<Map.Entry<String, Integer>> ambilTopN(
            Map<String, Integer> map, int n) {
        List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });
        return list.size() > n ? list.subList(0, n) : list;
    }

    private static void appendHtml(Component parent, String html) {
        new Html(html).setParent(parent);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;")
                .replace(">","&gt;").replace("\"","&quot;");
    }

    private static String safeStr(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String potong(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
