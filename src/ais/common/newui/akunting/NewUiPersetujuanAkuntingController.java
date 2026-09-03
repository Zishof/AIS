package ais.common.newui.akunting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.akunting.PostingJurnalAction;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.DanaTalangan;
import ais.database.model.akunting.KasKecil;
import ais.database.model.akunting.PenggantianKasKecil;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.akunting.PertangungjawabanKasBesar;
import ais.database.model.akunting.UangMuka;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;

/**
 * Kontrak native tujuh layar akunting: enam antrean persetujuan dan satu wadah
 * tab Draft Jurnal.
 *
 * <h3>Mengapa keenam antrean dilayani satu kontrak</h3>
 * <p>Uang muka, kas besar (dana talangan), kas kecil, penggantian kas kecil,
 * pertanggungjawaban, dan pertanggungjawaban kas besar adalah enam entity yang
 * berbeda, tetapi keenamnya turunan {@link DataSop} dengan inti yang identik:
 * kode, nama, keterangan, nilai, pengaju, penyetuju, tanggal, satuan kerja, dan
 * status dengan kosakata yang sama persis (Pengajuan/Disetujui/Ditolak).
 * Menuliskan enam kontrak yang sama hanya akan melipatgandakan tempat
 * berbuat salah.</p>
 *
 * <h3>Mengapa dibaca lewat entity, bukan SQL</h3>
 * <p><b>Status pada keenam entity adalah nilai TURUNAN</b>, bukan isi kolom:
 * berubah menjadi "Disetujui" begitu kolom penyetuju terisi, dan menjadi
 * "Ditolak" bila alur SOP-nya berhenti di simpul penolakan. Membaca kolom
 * {@code status} apa adanya lewat SQL akan menampilkan status yang berbeda dari
 * layar ZK — persis kesalahan yang pernah terjadi dan diperbaiki pada kontrak
 * persetujuan transaksi koperasi.</p>
 *
 * <h3>Mengapa BACA SAJA</h3>
 * <p>Menyetujui pada layar ZK bukan sekadar mengisi kolom penyetuju. Ia juga
 * membangkitkan {@code DaftarPengajuanTransfer} (antrean pencairan dana),
 * menggerakkan disposisi SOP, menyentuh riwayat posting, dan memicu pencetakan
 * dokumen. Kontrak yang hanya menandai "disetujui" akan menghasilkan pengajuan
 * yang tampak lolos namun uangnya tidak pernah masuk antrean transfer — cacat
 * yang baru ketahuan ketika seseorang menagih dananya. Sampai seluruh rangkaian
 * itu tersedia native, persetujuan tetap dilakukan di layar lama dan kontrak
 * ini hanya menyajikan antrean beserta rinciannya. Server mengumumkannya lewat
 * {@code bolehUbah:false} agar klien tidak menampilkan tombol yang menjanjikan
 * hal yang tidak ada.</p>
 *
 * <p>Fail-closed: mode tak dikenal ditolak, sesi tanpa pengguna ditolak, dan
 * tidak ada satu pun aksi yang menulis.</p>
 */
public final class NewUiPersetujuanAkuntingController {

    /** Harus sama dengan awalan folder JSP sebelum {@code /uiux/}. */
    private static final String MODULE = "akunting";

    public static final String MODE_UANG_MUKA = "uang_muka";
    public static final String MODE_DANA_TALANGAN = "dana_talangan";
    public static final String MODE_KAS_KECIL = "kas_kecil";
    public static final String MODE_PENGGANTIAN_KAS_KECIL = "penggantian_kas_kecil";
    public static final String MODE_PERTANGGUNGJAWABAN = "pertanggungjawaban";
    public static final String MODE_PERTANGGUNGJAWABAN_KAS_BESAR = "pertanggungjawaban_kas_besar";
    /** Wadah tab Draft Jurnal; tidak punya data sendiri. */
    public static final String MODE_DRAFT_JURNAL = "draft_jurnal";

    private static final String PENGAJUAN = UangMuka.PENGAJUAN;
    private static final String DISETUJU = UangMuka.DISETUJU;
    private static final String DITOLAK = UangMuka.DITOLAK;

    /**
     * Batas baris yang diambil sebelum penyaringan status di Java.
     *
     * <p>Status turunan tidak dapat disaring di basis data untuk "Pengajuan"
     * versus "Ditolak" (keduanya berkolom penyetuju kosong), sehingga
     * kandidatnya diambil lebih dulu lalu dipilah. Karena itu jumlah baris yang
     * tampil bisa lebih sedikit daripada batas ini; keadaan itu dilaporkan
     * lewat {@code terpotong} supaya pengguna tahu daftarnya belum utuh.</p>
     */
    private static final int BATAS_KANDIDAT = 400;

    private NewUiPersetujuanAkuntingController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String mode, String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            if (!modeDikenal(mode)) throw new IllegalArgumentException("Mode akunting tidak dikenal.");
            String action = text(request.getParameter("action"), "meta");
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403);
                fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                write(response, json);
                return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");
            // Satuan kerja yang berhak dilihat pengguna login. Sama seperti
            // initCriteria() di layar ZK pembandingnya, kandidat/detail HARUS
            // dibatasi dengan set ini di level Hibernate.
            Set<SatuanKerja> allowed = SekolahUtil.ambilSatuanKerjas();

            if (MODE_DRAFT_JURNAL.equals(mode)) {
                draftJurnal(json, action);
            } else if ("meta".equals(action)) {
                meta(json, mode);
            } else if ("list".equals(action)) {
                daftar(json, request, mode, allowed);
            } else if ("detail".equals(action)) {
                detail(json, request, mode, allowed);
            } else {
                throw new IllegalArgumentException("Aksi tidak dikenal.");
            }
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403);
            fail(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            fail(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal memuat antrean persetujuan. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiPersetujuanAkuntingController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    public static boolean modeDikenal(String mode) {
        return MODE_UANG_MUKA.equals(mode) || MODE_DANA_TALANGAN.equals(mode)
                || MODE_KAS_KECIL.equals(mode) || MODE_PENGGANTIAN_KAS_KECIL.equals(mode)
                || MODE_PERTANGGUNGJAWABAN.equals(mode)
                || MODE_PERTANGGUNGJAWABAN_KAS_BESAR.equals(mode)
                || MODE_DRAFT_JURNAL.equals(mode);
    }

    /** Entity yang dibaca tiap mode. */
    static Class<?> entity(String mode) {
        if (MODE_UANG_MUKA.equals(mode)) return UangMuka.class;
        if (MODE_DANA_TALANGAN.equals(mode)) return DanaTalangan.class;
        if (MODE_KAS_KECIL.equals(mode)) return KasKecil.class;
        if (MODE_PENGGANTIAN_KAS_KECIL.equals(mode)) return PenggantianKasKecil.class;
        if (MODE_PERTANGGUNGJAWABAN.equals(mode)) return Pertangungjawaban.class;
        return PertangungjawabanKasBesar.class;
    }

    static String judul(String mode) {
        if (MODE_UANG_MUKA.equals(mode)) return "Persetujuan Uang Muka";
        if (MODE_DANA_TALANGAN.equals(mode)) return "Persetujuan Kas Besar";
        if (MODE_KAS_KECIL.equals(mode)) return "Persetujuan Pengeluaran Kas Kecil";
        if (MODE_PENGGANTIAN_KAS_KECIL.equals(mode)) return "Penggantian Kas Kecil";
        if (MODE_PERTANGGUNGJAWABAN.equals(mode)) return "Persetujuan Pertanggungjawaban Uang Muka";
        if (MODE_PERTANGGUNGJAWABAN_KAS_BESAR.equals(mode)) return "Persetujuan Pertanggungjawaban Kas Besar";
        return "Draft Jurnal";
    }

    // ------------------------------------------------------------------ meta

    private static void meta(JSONObject j, String mode) throws Exception {
        j.put("judul", judul(mode));
        j.put("mode", mode);
        // Klien TIDAK boleh menampilkan tombol setujui/tolak. Lihat catatan kelas.
        j.put("bolehUbah", false);
        j.put("alasanBacaSaja", "Persetujuan membangkitkan antrean pencairan dana, menggerakkan "
                + "disposisi SOP, dan memicu pencetakan dokumen. Rangkaian itu belum tersedia "
                + "secara native, sehingga persetujuan tetap dilakukan di layar lama.");
        JSONArray status = new JSONArray();
        status.put(PENGAJUAN);
        status.put(DISETUJU);
        status.put(DITOLAK);
        j.put("pilihanStatus", status);
        j.put("statusBawaan", PENGAJUAN);

        int thn = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
        JSONArray tahun = new JSONArray();
        tahun.put(0); // 0 = semua tahun
        for (int t = thn; t >= thn - 5; t--) {
            tahun.put(t);
        }
        j.put("pilihanTahun", tahun);
        j.put("tahunBawaan", thn);
        j.put("batasKandidat", BATAS_KANDIDAT);
    }

    // ------------------------------------------------------------------ list

    private static void daftar(JSONObject j, HttpServletRequest request, String mode, Set<SatuanKerja> allowed) throws Exception {
        String status = text(request.getParameter("status"), PENGAJUAN);
        if (!PENGAJUAN.equals(status) && !DISETUJU.equals(status) && !DITOLAK.equals(status)) {
            throw new IllegalArgumentException("Status tidak dikenal.");
        }
        int tahun = tahunParam(request);
        String kw = text(request.getParameter("q"), "").trim().toLowerCase();

        List<DataSop> kandidat = kandidat(mode, status, tahun, allowed);
        JSONArray rows = new JSONArray();
        double total = 0;
        for (DataSop d : kandidat) {
            // Status dibaca lewat getter agar turunan SOP-nya ikut terhitung.
            if (!status.equals(status(d))) continue;
            if (kw.length() > 0 && !cocok(d, kw)) continue;
            double nilai = nilai(d);
            total += nilai;
            rows.put(baris(d, nilai));
        }
        j.put("rows", rows);
        j.put("status", status);
        j.put("tahun", tahun);
        j.put("jumlah", rows.length());
        j.put("totalNilai", total);
        j.put("terpotong", kandidat.size() >= BATAS_KANDIDAT);
    }

    /**
     * Kandidat dari basis data sebelum status turunannya dinilai.
     *
     * <p>"Disetujui" dapat disaring tepat di basis data karena setara dengan
     * kolom penyetuju terisi. "Pengajuan" dan "Ditolak" sama-sama berkolom
     * penyetuju kosong dan hanya dibedakan alur SOP, sehingga keduanya diambil
     * bersama lalu dipilah di Java.</p>
     */
    @SuppressWarnings("unchecked")
    private static List<DataSop> kandidat(String mode, String status, int tahun, Set<SatuanKerja> allowed) {
        String nama = entity(mode).getSimpleName();
        StringBuilder hql = new StringBuilder("select distinct t from " + nama + " t "
                + "left join fetch t.dibuatOleh du "
                + "left join fetch t.disetujuiOleh su "
                + "left join fetch t.satuanKerja sk "
                // Disposisi ikut diambil supaya getStatus() tidak memicu satu
                // kueri lazy per baris saat menilai penolakan alur SOP.
                + "left join fetch t.disposisiSop ds "
                + "left join fetch ds.disposisiEnd de "
                + "left join fetch de.alurSop al where 1=1");
        if (DISETUJU.equals(status)) {
            hql.append(" and t.disetujuiOleh is not null");
        } else {
            hql.append(" and t.disetujuiOleh is null");
        }
        if (tahun > 0) {
            hql.append(" and t.tahun = :tahun");
        }
        // Dibatasi ke satuan kerja yang berhak dilihat pengguna login, sama
        // seperti initCriteria() pada layar ZK pembandingnya. Kandidat berkolom
        // satuanKerja kosong tetap ditampilkan (mengikuti pola yang sama pada
        // NewUiStandingInstructionService); bila pengguna tidak berhak atas
        // satuan kerja manapun, hanya kandidat tanpa satuan kerja yang tampil.
        boolean batasi = allowed != null && !allowed.isEmpty();
        if (batasi) {
            hql.append(" and (sk is null or sk in (:allowed))");
        } else {
            hql.append(" and sk is null");
        }
        hql.append(" order by t.id desc");
        Query q = HibernateUtil.currentSession().createQuery(hql.toString());
        if (tahun > 0) q.setParameter("tahun", Integer.valueOf(tahun));
        if (batasi) q.setParameterList("allowed", allowed);
        q.setMaxResults(BATAS_KANDIDAT);
        List<DataSop> hasil = new ArrayList<DataSop>();
        for (Object o : q.list()) {
            hasil.add((DataSop) o);
        }
        return hasil;
    }

    // ---------------------------------------------------------------- detail

    private static void detail(JSONObject j, HttpServletRequest request, String mode, Set<SatuanKerja> allowed) throws Exception {
        Long id = idWajib(request.getParameter("id"));
        Session session = HibernateUtil.currentSession();
        Object o = session.get(entity(mode), id);
        if (o == null) throw new IllegalArgumentException("Pengajuan tidak ditemukan.");
        DataSop d = (DataSop) o;
        // list() sudah dibatasi ke satuan kerja yang berhak dilihat pengguna;
        // detail() dijaga sama supaya id tidak bisa ditebak untuk membaca data
        // finansial lintas satuan kerja.
        ensureScope(d, allowed);
        JSONObject isi = baris(d, nilai(d));
        isi.put("keterangan", teks(d.getKeterangan()));
        isi.put("kodeUnik", teks(panggilTeks(d, "getKodeUnik")));
        isi.put("bulan", panggilAngka(d, "getBulan"));
        isi.put("tahun", panggilAngka(d, "getTahun"));
        isi.put("tanggalTransaksi", tanggal(panggilTanggal(d, "getTanggalTransaksi")));
        isi.put("tanggalPembuatan", tanggal(panggilTanggal(d, "getTanggalPembuatan")));
        j.put("data", isi);
    }

    // ---------------------------------------------------------- draft jurnal

    /**
     * Wadah tab Draft Jurnal.
     *
     * <p>Layar ini tidak punya data sendiri; ia memuat sampai dua puluh layar
     * posting lain. Daftar tabnya dibaca dari {@link PostingJurnalAction#TABS}
     * — satu sumber yang sama dengan layar ZK — dan tetap tunduk pada saklar
     * Konfigurasi, sehingga tab yang dimatikan admin juga tidak muncul di sini.
     * Rutenya dikembalikan dalam bentuk URL menu (tanpa awalan internal ZK)
     * supaya klien dapat mencocokkannya dengan katalog menu miliknya.</p>
     */
    private static void draftJurnal(JSONObject j, String action) throws Exception {
        if (!"meta".equals(action)) throw new IllegalArgumentException("Aksi tidak dikenal.");
        j.put("judul", "Draft Jurnal");
        j.put("mode", MODE_DRAFT_JURNAL);
        JSONArray tab = new JSONArray();
        String p = Konfigurasi.POSTING_JURNAL_TAB_PREFIX;
        for (int i = 0; i < PostingJurnalAction.TABS.length; i++) {
            String[] t = PostingJurnalAction.TABS[i];
            if (!Common.bolehKonfigurasi(p + t[0])) continue;
            tab.put(new JSONObject().put("slug", t[0]).put("label", t[1])
                    .put("route", ruteMenu(t[2])));
        }
        j.put("tab", tab);
        j.put("catatanCakupan", "Tiap tab adalah layar tersendiri. Yang belum punya layar native "
                + "akan ditandai tidak tersedia oleh klien.");
    }

    /** {@code /WEB-INF/z/x/y/pages/...} → {@code /pages/...} seperti URL menu. */
    static String ruteMenu(String zul) {
        String awalan = "/WEB-INF/z/x/y";
        return zul != null && zul.startsWith(awalan) ? zul.substring(awalan.length()) : zul;
    }

    // ------------------------------------------------------------------ util

    /** Inti yang sama untuk keenam entity. */
    private static JSONObject baris(DataSop d, double nilai) throws Exception {
        // id/kode/nama/keterangan dideklarasikan GeneralValueObject sehingga dapat
        // dipanggil langsung; hanya getter yang tidak ada di kelas basis yang
        // memerlukan refleksi.
        return new JSONObject()
                .put("id", d.getId() == null ? 0L : d.getId().longValue())
                .put("kode", teks(d.getKode()))
                .put("nama", teks(d.getNama()))
                .put("nilai", nilai)
                .put("pengaju", pengguna(panggilObjek(d, "getDibuatOleh")))
                .put("penyetuju", pengguna(panggilObjek(d, "getDisetujuiOleh")))
                .put("satuanKerja", namaSatuanKerja(panggilObjek(d, "getSatuanKerja")))
                .put("tanggal", tanggal(panggilTanggal(d, "getTanggalPembuatan")))
                .put("tanggalPersetujuan", tanggal(panggilTanggal(d, "getTanggalPersetujuan")))
                .put("status", status(d));
    }

    /**
     * Status pengajuan.
     *
     * <p>{@link DataSop} tidak mendeklarasikan getter ini walau keenam
     * turunannya memilikinya, sehingga dipanggil lewat refleksi seperti getter
     * bersama lainnya. Nilainya turunan: "Disetujui" bila penyetuju terisi,
     * "Ditolak" bila alur SOP berhenti di simpul penolakan.</p>
     */
    private static String status(DataSop d) {
        String s = panggilTeks(d, "getStatus");
        return s == null || s.trim().length() == 0 ? PENGAJUAN : s;
    }

    private static boolean cocok(DataSop d, String kw) {
        String gabung = (teks(d.getKode()) + " " + teks(d.getNama()) + " "
                + teks(d.getKeterangan())).toLowerCase();
        return gabung.contains(kw);
    }

    private static double nilai(DataSop d) {
        Object v = panggilObjek(d, "getNilai");
        return v instanceof Number ? ((Number) v).doubleValue() : 0.0;
    }

    /**
     * Pemanggilan getter lewat refleksi, untuk yang TIDAK ada di kelas basis.
     *
     * <p>{@code GeneralValueObject} sudah mendeklarasikan id, kode, nama, dan
     * keterangan sehingga keempatnya dipanggil langsung. Sisanya — nilai,
     * pengaju, penyetuju, satuan kerja, tanggal, dan status — dimiliki keenam
     * entity dengan nama yang sama persis namun tanpa antarmuka yang
     * mendeklarasikannya. Refleksi menjaga kontrak tetap satu; bila kelak
     * antarmuka bersama ditambahkan pada model, bagian ini dapat diganti
     * pemanggilan langsung tanpa mengubah bentuk JSON-nya.</p>
     */
    /**
     * Menolak akses bila satuan kerja pengajuan {@code d} berada di luar
     * {@code allowed}. Kandidat tanpa satuan kerja selalu diizinkan, sama
     * seperti penyaringan di {@link #kandidat}.
     */
    private static void ensureScope(DataSop d, Set<SatuanKerja> allowed) {
        Object sk = panggilObjek(d, "getSatuanKerja");
        if (sk == null) return;
        Object skId = panggilObjek(sk, "getId");
        if (allowed != null) {
            for (SatuanKerja u : allowed) {
                if (u.getId() != null && u.getId().equals(skId)) return;
            }
        }
        throw new IllegalArgumentException("Pengajuan tidak ditemukan.");
    }

    private static Object panggilObjek(Object target, String getter) {
        try {
            return target.getClass().getMethod(getter).invoke(target);
        } catch (Exception e) {
            return null;
        }
    }

    private static String panggilTeks(Object target, String getter) {
        Object v = panggilObjek(target, getter);
        return v == null ? null : v.toString();
    }

    private static java.util.Date panggilTanggal(Object target, String getter) {
        Object v = panggilObjek(target, getter);
        return v instanceof java.util.Date ? (java.util.Date) v : null;
    }

    private static long panggilAngka(Object target, String getter) {
        Object v = panggilObjek(target, getter);
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    private static String pengguna(Object tbmuser) {
        if (tbmuser == null) return "";
        String nama = panggilTeks(tbmuser, "getUserNama");
        if (nama != null && nama.trim().length() > 0) return nama;
        return teks(panggilTeks(tbmuser, "getUserId"));
    }

    private static String namaSatuanKerja(Object satuanKerja) {
        return satuanKerja == null ? "" : teks(panggilTeks(satuanKerja, "getNama"));
    }

    private static int tahunParam(HttpServletRequest request) {
        String v = text(request.getParameter("tahun"), "").trim();
        if (v.length() == 0) return 0;
        try {
            int t = Integer.parseInt(v);
            if (t == 0) return 0;
            if (t < 2000 || t > 2999) throw new NumberFormatException();
            return t;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Tahun tidak sah.");
        }
    }

    private static Long idWajib(String nilai) {
        try {
            long l = Long.parseLong(text(nilai, "").trim());
            if (l <= 0) throw new NumberFormatException();
            return Long.valueOf(l);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Pengajuan belum dipilih.");
        }
    }

    private static String tanggal(java.util.Date d) {
        return d == null ? "" : new java.text.SimpleDateFormat("dd-MM-yyyy").format(d);
    }

    private static String teks(String s) {
        return s == null ? "" : s;
    }

    private static String text(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value;
    }

    private static void fail(JSONObject json, String code, String message) {
        try {
            json.put("ok", false);
            json.put("code", code);
            json.put("message", message == null ? "" : message);
        } catch (Exception ignored) { }
    }

    private static void write(HttpServletResponse response, JSONObject json) throws Exception {
        response.getWriter().write(json.toString());
    }
}
