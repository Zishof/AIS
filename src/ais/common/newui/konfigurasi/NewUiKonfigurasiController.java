package ais.common.newui.konfigurasi;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.MemoryDbUtil;
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiPermission;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;

/**
 * Kontrak native aman untuk Pengaturan Konfigurasi dan Konfigurasi Detail.
 *
 * <p>Nilai konfigurasi tidak dapat diserahkan kepada Generic CRUD: nama baris
 * adalah skema dinamis, sehingga kolom {@code nilai} dapat berisi password,
 * token, private key, atau client secret walaupun nama properti Java-nya tampak
 * biasa. Controller ini menyamarkan seluruh nilai sensitif berdasarkan nama
 * konfigurasi, tidak pernah mengirim nilai aslinya ke klien, dan hanya menerima
 * penggantian rahasia secara write-only.</p>
 *
 * <p>Nama/tahun akademik baris existing tidak dapat dipindah saat update.
 * Penambahan kunci baru hanya tersedia pada menu Konfigurasi Detail dan tetap
 * mengikuti privilege create menu. Penghapusan sengaja tidak disediakan karena
 * menghapus satu kunci sering mengaktifkan fallback default yang efeknya lebih
 * luas daripada baris yang terlihat.</p>
 */
public final class NewUiKonfigurasiController {

    public static final String PAGE_UTAMA = "konfigurasi";
    public static final String PAGE_DETAIL = "konfigurasi_detail";
    private static final String MODULE = "root";
    private static final String MASK = "********";
    private static final int BATAS = 300;

    private NewUiKonfigurasiController() {
    }

    public static void handle(HttpServletRequest request,
            HttpServletResponse response, String page) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            if (!PAGE_UTAMA.equals(page) && !PAGE_DETAIL.equals(page)) {
                throw new IllegalArgumentException("Mode konfigurasi tidak dikenal.");
            }
            String action = text(request.getParameter("action"), "meta")
                    .toLowerCase();
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, page, action)) {
                response.setStatus(403);
                fail(json, "ACTION_FORBIDDEN",
                        "Hak akses aksi konfigurasi tidak tersedia.");
                write(response, json);
                return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null || user.getUserId() == null) {
                throw new SecurityException("Sesi pengguna tidak dikenal.");
            }

            if ("meta".equals(action)) {
                meta(json, request, page);
            } else if ("list".equals(action)) {
                daftar(json, request);
            } else if ("detail".equals(action)) {
                detail(json, request);
            } else if ("update".equals(action)) {
                wajibCsrf(request);
                simpan(json, request, user, false);
            } else if ("create".equals(action) && PAGE_DETAIL.equals(page)) {
                wajibCsrf(request);
                simpan(json, request, user, true);
            } else {
                throw new IllegalArgumentException(
                        "Aksi konfigurasi tidak tersedia pada halaman ini.");
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
            fail(json, "INTERNAL_ERROR",
                    "Gagal memproses konfigurasi. Detail dicatat di log server.");
            try {
                ais.common.ErrorAuditUtil.record(e,
                        "NewUiKonfigurasiController");
            } catch (Exception ignored) {
            }
        }
        write(response, json);
    }

    private static void meta(JSONObject j, HttpServletRequest request,
            String page) throws Exception {
        NewUiPermission izin = NewUiRouteGuard.permissionFor(request,
                MODULE, page);
        j.put("judul", PAGE_DETAIL.equals(page)
                ? "Pengaturan Konfigurasi Detail"
                : "Pengaturan Konfigurasi");
        j.put("mode", page);
        j.put("bolehTambah", PAGE_DETAIL.equals(page) && izin != null
                && izin.isCanCreate());
        j.put("bolehUbah", izin != null && izin.isCanUpdate());
        j.put("bolehHapus", false);
        j.put("rahasiaDisamarkan", true);
        j.put("maksimumBaris", BATAS);
        j.put("csrfHeader", NewUiCsrfUtil.HEADER);
        j.put("csrfToken", NewUiCsrfUtil.getToken(
                request.getSession(true)));
    }

    @SuppressWarnings("unchecked")
    private static void daftar(JSONObject j, HttpServletRequest request)
            throws Exception {
        Session session = HibernateUtil.openSession();
        try {
            Criteria c = session.createCriteria(Konfigurasi.class);
            String q = text(request.getParameter("q"), "");
            String tahun = text(request.getParameter("tahunAkademik"), "");
            if (q.length() > 0) {
                c.add(Restrictions.or(
                        Restrictions.ilike("nama", q, MatchMode.ANYWHERE),
                        Restrictions.ilike("keterangan", q,
                                MatchMode.ANYWHERE)));
            }
            if (tahun.length() > 0) {
                c.add(Restrictions.eq("tahunAkademik", tahun));
            }
            List<Konfigurasi> rows = c.addOrder(Order.asc("nama"))
                    .addOrder(Order.desc("id")).setMaxResults(BATAS + 1).list();
            JSONArray data = new JSONArray();
            int jumlah = Math.min(rows.size(), BATAS);
            for (int i = 0; i < jumlah; i++) {
                data.put(baris(rows.get(i), false));
            }
            j.put("rows", data);
            j.put("total", data.length());
            j.put("dibatasi", rows.size() > BATAS);
        } finally {
            session.close();
        }
    }

    private static void detail(JSONObject j, HttpServletRequest request)
            throws Exception {
        Session session = HibernateUtil.openSession();
        try {
            Konfigurasi k = (Konfigurasi) session.get(Konfigurasi.class,
                    id(request));
            if (k == null) {
                throw new IllegalArgumentException(
                        "Konfigurasi tidak ditemukan.");
            }
            j.put("data", baris(k, true));
        } finally {
            session.close();
        }
    }

    private static JSONObject baris(Konfigurasi k, boolean lengkap)
            throws Exception {
        boolean rahasia = isSensitiveName(k.getNama());
        JSONObject row = new JSONObject()
                .put("id", k.getId())
                .put("nama", nz(k.getNama()))
                .put("nilai", rahasia ? MASK : nz(k.getNilai()))
                .put("keterangan", nz(k.getKeterangan()))
                .put("tahunAkademik", nz(k.getTahunAkademik()))
                .put("rahasia", rahasia)
                .put("terkunci", k.getDikunci() != null)
                .put("jenisNilai", jenis(k.getNilai()));
        if (lengkap) {
            row.put("info1", rahasia ? MASK : nz(k.getInfo1()));
            row.put("info2", rahasia ? MASK : nz(k.getInfo2()));
            row.put("info3", rahasia ? MASK : nz(k.getInfo3()));
            row.put("info4", rahasia ? MASK : nz(k.getInfo4()));
            row.put("info5", rahasia ? MASK : nz(k.getInfo5()));
        }
        return row;
    }

    private static void simpan(JSONObject j, HttpServletRequest request,
            Tbmuser user, boolean baru) throws Exception {
        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        Konfigurasi k = null;
        try {
            tx = session.beginTransaction();
            if (baru) {
                String nama = wajib(request, "nama",
                        "Nama konfigurasi wajib diisi.");
                if (nama.length() > 250) {
                    throw new IllegalArgumentException(
                            "Nama konfigurasi maksimal 250 karakter.");
                }
                String tahun = kosongJadiNull(request.getParameter(
                        "tahunAkademik"));
                Criteria cek = session.createCriteria(Konfigurasi.class)
                        .add(Restrictions.eq("nama", nama));
                cek.add(tahun == null
                        ? Restrictions.isNull("tahunAkademik")
                        : Restrictions.eq("tahunAkademik", tahun));
                if (cek.setMaxResults(1).uniqueResult() != null) {
                    throw new IllegalArgumentException(
                            "Nama dan tahun akademik tersebut sudah tersedia.");
                }
                k = new Konfigurasi();
                k.setNama(nama);
                k.setTahunAkademik(tahun);
            } else {
                k = (Konfigurasi) session.get(Konfigurasi.class, id(request));
                if (k == null) {
                    throw new IllegalArgumentException(
                            "Konfigurasi tidak ditemukan.");
                }
            }

            boolean rahasia = isSensitiveName(k.getNama());
            String nilai = request.getParameter("nilai");
            if (!rahasia || (nilai != null && nilai.trim().length() > 0
                    && !MASK.equals(nilai.trim()))) {
                k.setNilai(batasi(nilai, 50000, "Nilai konfigurasi"));
            }
            k.setKeterangan(batasi(request.getParameter("keterangan"),
                    10000, "Keterangan"));
            if (!rahasia) {
                k.setInfo1(batasi(request.getParameter("info1"), 10000,
                        "Info 1"));
                k.setInfo2(batasi(request.getParameter("info2"), 10000,
                        "Info 2"));
                k.setInfo3(batasi(request.getParameter("info3"), 10000,
                        "Info 3"));
                k.setInfo4(batasi(request.getParameter("info4"), 10000,
                        "Info 4"));
                k.setInfo5(batasi(request.getParameter("info5"), 10000,
                        "Info 5"));
            }
            k.setOleh(user.getUserNama());
            k.setOlehId(String.valueOf(user.getUserId()));
            if (baru) {
                session.save(k);
            } else {
                session.update(k);
            }
            tx.commit();
        } catch (Exception e) {
            rollback(tx);
            throw e;
        } finally {
            session.close();
        }

        try {
            MemoryDbUtil.getKonfigurasi().put(k.getNama(), k);
        } catch (Throwable cacheError) {
            try {
                MemoryDbUtil.resetLocalReferences();
            } catch (Throwable ignored) {
            }
        }
        j.put("id", k.getId());
        j.put("message", "Konfigurasi berhasil disimpan.");
    }

    /** Terlihat oleh self-test; tidak pernah mengembalikan isi rahasia. */
    public static boolean isSensitiveName(String raw) {
        String n = raw == null ? "" : raw.trim().toLowerCase();
        return n.indexOf("password") >= 0 || n.indexOf("passwd") >= 0
                || n.indexOf("credential") >= 0
                || n.indexOf("client_secret") >= 0
                || n.indexOf("clientsecret") >= 0
                || n.indexOf("secret") >= 0
                || n.indexOf("scret") >= 0
                || n.indexOf("private_key") >= 0
                || n.indexOf("privatekey") >= 0
                || n.indexOf("access_key") >= 0
                || n.indexOf("accesskey") >= 0
                || n.indexOf("api_key") >= 0
                || n.indexOf("apikey") >= 0
                || n.indexOf("token") >= 0
                || n.endsWith("_key") || n.endsWith(".key")
                || n.indexOf("preshared") >= 0;
    }

    private static String jenis(String nilai) {
        if (Konfigurasi.AKTIF.equalsIgnoreCase(nilai)
                || Konfigurasi.TIDAK_AKTIF.equalsIgnoreCase(nilai)) {
            return "boolean";
        }
        return "teks";
    }

    private static String wajib(HttpServletRequest r, String nama,
            String pesan) {
        String value = text(r.getParameter(nama), "");
        if (value.length() == 0) {
            throw new IllegalArgumentException(pesan);
        }
        return value;
    }

    private static String batasi(String value, int max, String label) {
        String result = value == null ? "" : value.trim();
        if (result.length() > max) {
            throw new IllegalArgumentException(label + " maksimal " + max
                    + " karakter.");
        }
        return result;
    }

    private static String kosongJadiNull(String value) {
        String result = value == null ? "" : value.trim();
        return result.length() == 0 ? null : result;
    }

    private static Long id(HttpServletRequest r) {
        try {
            String raw = r.getParameter("id");
            if (raw == null || raw.trim().length() == 0) {
                throw new Exception();
            }
            return Long.valueOf(raw.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Id konfigurasi tidak sah.");
        }
    }

    private static void wajibCsrf(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())
                || !NewUiCsrfUtil.isValid(request)) {
            throw new SecurityException(
                    "Token keamanan tidak valid. Muat ulang halaman.");
        }
    }

    private static String text(String value, String fallback) {
        return value == null ? fallback : value.trim();
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private static void rollback(Transaction tx) {
        try {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        } catch (Exception ignored) {
        }
    }

    private static void fail(JSONObject j, String code, String message)
            throws Exception {
        j.put("ok", false);
        j.put("code", code);
        j.put("message", message == null ? "Permintaan gagal." : message);
    }

    private static void write(HttpServletResponse response, JSONObject j)
            throws Exception {
        response.getWriter().write(j.toString());
    }
}
