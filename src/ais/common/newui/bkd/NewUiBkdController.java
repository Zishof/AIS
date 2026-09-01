package ais.common.newui.bkd;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.bkd.AsesementAction;
import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.common.newui.laporan.JasperPdfUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;

/**
 * Kontrak native tiga layar Beban Kinerja Dosen (BKD).
 *
 * <p>Ketiganya bukan layar data melainkan <b>wadah</b>: Asesemen Kinerja
 * menghimpun empat belas layar penilaian, sedangkan Laporan Kinerja dan Laporan
 * Kinerja Rinci masing-masing menghimpun tiga laporan Jasper. Bentuk kontraknya
 * mengikuti sifat itu — mengumumkan isi wadahnya, lalu melayani laporan yang
 * diminta.</p>
 *
 * <h3>Asesemen Kinerja — direktori, bukan tab bersarang</h3>
 * <p>Layar ZK-nya bertab dua tingkat: lima bidang di atas, dan Bidang
 * Pendidikan sendiri memuat tujuh sub-tab. Menirunya di klien berarti menumpuk
 * halaman di dalam halaman dua lapis. Disajikan sebagai direktori berkelompok,
 * dan daftarnya dibaca dari {@link AsesementAction#TABS} — konstanta yang sama
 * yang dipakai layar ZK untuk memuat panelnya, sehingga keduanya tidak dapat
 * menyimpang.</p>
 *
 * <p>Tiga panel di antaranya <b>tidak punya rute</b>: Penelitian, Publikasi
 * Ilmiah, dan Pengabdian dibangun langsung oleh helper, bukan dengan memuat
 * sebuah halaman. Panel itu diumumkan tanpa rute beserta alasannya, bukan
 * disembunyikan — pengguna yang mengenal layar lama akan mencarinya.</p>
 *
 * <h3>Laporan — dosen wajib atau tidak, itu yang membedakan</h3>
 * <p>Enam varian laporan pada kedua layar terbagi dua bentuk. Bentuk
 * <i>ringkasan</i> menerima pegawai secara opsional: bila tidak dipilih, layar
 * ZK mengirim {@code -1} sehingga laporan mencakup seluruh dosen. Bentuk
 * <i>peringkat</i> tidak menerima pegawai sama sekali — ia memeringkat semua
 * dosen — melainkan pilihan pengurutan (kinerja atau beban). Membedakan
 * keduanya di server membuat klien tidak perlu menebak filter mana yang
 * berlaku untuk varian mana.</p>
 *
 * <p>Fail-closed: mode dan varian di luar daftar ditolak, sesi tanpa pengguna
 * ditolak, dan seluruh aksi hanya membaca.</p>
 */
public final class NewUiBkdController {

    /** Harus sama dengan awalan folder JSP sebelum {@code /uiux/}. */
    private static final String MODULE = "bkd";

    /** Asesemen Kinerja — direktori panel penilaian. */
    public static final String MODE_ASESEMEN = "asesemen";
    /** Laporan Kinerja — ringkasan dan peringkat tingkat atas. */
    public static final String MODE_LAPORAN = "laporan";
    /** Laporan Kinerja Rinci — ringkasan dan peringkat versi rinci. */
    public static final String MODE_LAPORAN_RINCI = "laporan_rinci";

    /** Bentuk filter: pegawai opsional (kosong berarti seluruh dosen). */
    private static final String BENTUK_RINGKASAN = "ringkasan";
    /** Bentuk filter: tanpa pegawai, memakai pilihan pengurutan. */
    private static final String BENTUK_PERINGKAT = "peringkat";

    /** Satu varian laporan pada layar wadah. */
    private static final class Varian {
        final String kode, nama, template, bentuk;

        Varian(String kode, String nama, String template, String bentuk) {
            this.kode = kode;
            this.nama = nama;
            this.template = template;
            this.bentuk = bentuk;
        }
    }

    private NewUiBkdController() { }

    /** Varian yang dihimpun sebuah layar, sesuai urutan tab pada layar ZK. */
    private static List<Varian> varian(String mode) {
        List<Varian> v = new ArrayList<Varian>();
        if (MODE_LAPORAN.equals(mode)) {
            v.add(new Varian("summary", "Ringkasan Kinerja Dosen", "summary_kinerja_dosen", BENTUK_RINGKASAN));
            v.add(new Varian("peringkat", "Peringkat Kinerja Dosen", "rangking_kinerja_dosen", BENTUK_PERINGKAT));
            v.add(new Varian("peringkat_semua", "Peringkat Kinerja Semua Dosen",
                    "rangking_kinerja_dosen_semua", BENTUK_PERINGKAT));
        } else if (MODE_LAPORAN_RINCI.equals(mode)) {
            v.add(new Varian("summary_rinci", "Ringkasan Kinerja Dosen (Rinci)",
                    "summary_kinerja_dosen_rinci", BENTUK_RINGKASAN));
            v.add(new Varian("peringkat_rinci", "Peringkat Kinerja Dosen (Rinci)",
                    "rangking_kinerja_dosen_rinci", BENTUK_PERINGKAT));
            // Peringkat semua dosen dipakai bersama oleh kedua layar; templatenya
            // memang satu, persis seperti pada layar ZK.
            v.add(new Varian("peringkat_semua", "Peringkat Kinerja Semua Dosen",
                    "rangking_kinerja_dosen_semua", BENTUK_PERINGKAT));
        }
        return v;
    }

    private static Varian cariVarian(String mode, String kode) {
        for (Varian v : varian(mode)) {
            if (v.kode.equals(kode)) return v;
        }
        throw new IllegalArgumentException("Varian laporan tidak dikenal pada layar ini.");
    }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String mode, String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            if (!modeDikenal(mode)) throw new IllegalArgumentException("Mode BKD tidak dikenal.");
            String action = text(request.getParameter("action"), "meta");
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403);
                fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                write(response, json);
                return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");

            if (MODE_ASESEMEN.equals(mode)) {
                asesemen(json, action);
            } else if ("meta".equals(action)) {
                metaLaporan(json, mode);
            } else if ("lookup".equals(action)) {
                lookup(json, request);
            } else if ("export".equals(action)) {
                cetak(json, request, mode);
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
            fail(json, "INTERNAL_ERROR", "Gagal memproses permintaan BKD. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiBkdController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    public static boolean modeDikenal(String mode) {
        return MODE_ASESEMEN.equals(mode) || MODE_LAPORAN.equals(mode) || MODE_LAPORAN_RINCI.equals(mode);
    }

    // -------------------------------------------------------------- asesemen

    private static void asesemen(JSONObject j, String action) throws Exception {
        if (!"meta".equals(action)) throw new IllegalArgumentException("Aksi tidak dikenal.");
        j.put("judul", "Asesemen Kinerja");
        j.put("mode", MODE_ASESEMEN);
        JSONArray tab = new JSONArray();
        for (int i = 0; i < AsesementAction.TABS.length; i++) {
            String[] t = AsesementAction.TABS[i];
            JSONObject o = new JSONObject().put("kelompok", t[0]).put("label", t[1]);
            if (t[2] == null || t[2].trim().length() == 0) {
                // Panel yang dibangun helper tidak punya halaman tersendiri.
                o.put("route", JSONObject.NULL);
                o.put("alasan", "Panel ini dibangun langsung oleh layar lama dan belum punya "
                        + "halaman tersendiri yang dapat dibuka native.");
            } else {
                o.put("route", t[2]);
            }
            tab.put(o);
        }
        j.put("tab", tab);
        j.put("catatanCakupan", "Tiap panel adalah layar tersendiri. Yang tidak berrute atau di luar "
                + "hak akses peran Anda ditandai tidak tersedia.");
    }

    // --------------------------------------------------------------- laporan

    private static void metaLaporan(JSONObject j, String mode) throws Exception {
        j.put("judul", MODE_LAPORAN.equals(mode) ? "Laporan Kinerja" : "Laporan Kinerja Rinci");
        j.put("mode", mode);

        JSONArray varian = new JSONArray();
        for (Varian v : varian(mode)) {
            varian.put(new JSONObject().put("kode", v.kode).put("nama", v.nama)
                    .put("bentuk", v.bentuk)
                    // Dinyatakan tegas supaya klien tidak menebak filter mana yang
                    // berlaku untuk varian mana.
                    .put("pakaiPegawai", BENTUK_RINGKASAN.equals(v.bentuk))
                    .put("pakaiUrut", BENTUK_PERINGKAT.equals(v.bentuk)));
        }
        j.put("varian", varian);

        int tahunKini = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
        JSONArray ta = new JSONArray();
        for (int t = tahunKini + 1; t >= tahunKini - 5; t--) {
            ta.put(t + "/" + (t + 1));
        }
        j.put("tahunAkademik", ta);
        j.put("tahunAkademikBawaan", tahunKini + "/" + (tahunKini + 1));
        j.put("semester", new JSONArray().put("Ganjil").put("Genap"));
        j.put("semesterBawaan", "Ganjil");

        JSONArray urut = new JSONArray();
        urut.put(new JSONObject().put("nilai", 1).put("nama", "Kinerja"));
        urut.put(new JSONObject().put("nilai", 2).put("nama", "Beban"));
        j.put("pilihanUrut", urut);
        j.put("urutBawaan", 1);
        j.put("catatanPegawai", "Kosongkan pegawai untuk mencakup seluruh dosen.");
    }

    /** Pencarian pegawai untuk filter ringkasan; daftar awal tampil tanpa mengetik. */
    private static void lookup(JSONObject j, HttpServletRequest r) throws Exception {
        String q = text(r.getParameter("q"), "").trim();
        JSONArray arr = new JSONArray();
        Session s = HibernateUtil.openSession();
        try {
            Criteria c = s.createCriteria(Pegawai.class).addOrder(Order.asc("nama")).setMaxResults(50);
            if (q.length() >= 2) {
                c.add(Restrictions.or(Restrictions.ilike("nama", "%" + q + "%"),
                        Restrictions.ilike("mycode", "%" + q + "%")));
            }
            for (Object o : c.list()) {
                Pegawai p = (Pegawai) o;
                arr.put(new JSONObject().put("id", p.getId()).put("nama", teks(p.getNama()))
                        .put("kode", teks(p.getMycode())));
            }
        } finally {
            s.close();
        }
        j.put("pegawai", arr);
        j.put("total", arr.length());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void cetak(JSONObject j, HttpServletRequest r, String mode) throws Exception {
        Varian v = cariVarian(mode, text(r.getParameter("varian"), ""));
        String ta = text(r.getParameter("tahunAkademik"), "").trim();
        String semester = text(r.getParameter("semester"), "").trim();
        if (ta.length() == 0) throw new IllegalArgumentException("Tahun akademik wajib dipilih.");
        if (!"Ganjil".equals(semester) && !"Genap".equals(semester)) {
            throw new IllegalArgumentException("Semester wajib dipilih.");
        }

        Map parameters = new HashMap();
        parameters.put("ta", ta);
        parameters.put("semester", semester);

        if (BENTUK_PERINGKAT.equals(v.bentuk)) {
            int urut = angka(r.getParameter("urut"), 1);
            if (urut != 1 && urut != 2) throw new IllegalArgumentException("Urutan tidak dikenal.");
            parameters.put("urut", Integer.valueOf(urut));
        } else {
            // Pegawai opsional: kosong dikirim sebagai -1 persis seperti layar ZK,
            // sehingga laporan mencakup seluruh dosen alih-alih gagal.
            Long pegawaiId = idOpsional(r.getParameter("pegawaiId"));
            parameters.put("dosen", pegawaiId == null ? Long.valueOf(-1L) : pegawaiId);
        }

        JasperPdfUtil.tulis(j, v.template, parameters, v.kode, v.nama);
        j.put("varian", v.kode);
    }

    // ------------------------------------------------------------------ util

    private static int angka(String nilai, int bawaan) {
        String x = text(nilai, "").trim();
        if (x.length() == 0) return bawaan;
        try {
            return Integer.parseInt(x);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Nilai bukan angka yang sah.");
        }
    }

    private static Long idOpsional(String nilai) {
        String v = text(nilai, "").trim();
        if (v.length() == 0) return null;
        try {
            long l = Long.parseLong(v);
            return l <= 0 ? null : Long.valueOf(l);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Pegawai tidak sah.");
        }
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
