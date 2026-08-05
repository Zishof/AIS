package ais.action.servlet.api;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.AktiftasHarianSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.WaktuUtil;

/**
 * <h1>AktifitasHarianSiswaApi — Layanan JSON modul "Aktifitas Harian Siswa" untuk mobile</h1>
 *
 * <p>Kelas ini menyediakan antarmuka mobile untuk modul Aktifitas Harian Siswa (entitas
 * {@link AktiftasHarianSiswa}; perhatikan ejaan model: <i>Aktiftas</i>). Modul ini mencatat
 * kegiatan harian seorang siswa — apa yang dipelajari/dikerjakan, materi terkait, serta dua kanal
 * komunikasi dua-arah antara sekolah dan keluarga: <b>pesan pembina</b> ({@code pesanPembina} — dari
 * guru/wali/pembina kepada siswa &amp; orang tua) dan <b>pesan orang tua</b> ({@code pesanOrangTua}
 * — tanggapan/catatan dari orang tua kepada sekolah). Pada sisi web, modul ini diwakili oleh
 * {@code DaftarAktifitasHarianSiswaAction}, {@code DashboardAktifitasHarianSiswaAction}, dan
 * {@code CatatanOrangTuaAktiftasHarianAction} (halaman tempat orang tua memberi komentar harian).</p>
 *
 * <h2>Skenario penggunaan</h2>
 * <p>Aplikasi mobile melayani dua peran utama untuk modul ini:</p>
 * <ol>
 *   <li><b>Guru/Pembina</b> — mencatat aktivitas harian anak didik (membuat/memperbarui entri
 *       harian: nama kegiatan, keterangan, aktifitas, materi, tanggal) dan menuliskan
 *       <i>pesan pembina</i>. Lihat {@link #simpan(HttpServletRequest, JSONObject)} dan
 *       {@link #pesanPembina(HttpServletRequest, JSONObject)}.</li>
 *   <li><b>Orang Tua/Siswa</b> — melihat aktivitas harian anaknya dan membalas dengan
 *       <i>pesan orang tua</i>. Lihat {@link #daftar(HttpServletRequest, JSONObject)},
 *       {@link #detail(HttpServletRequest, JSONObject)}, dan
 *       {@link #pesanOrangTua(HttpServletRequest, JSONObject)}.</li>
 * </ol>
 *
 * <h2>Resolusi subjek (siswa)</h2>
 * <p>Bila akun yang login adalah siswa/orang tua, subjek default diambil dari
 * {@code Tbmuser.getSiswa()} sehingga aplikasi tidak perlu mengirim id siswa dan akun tidak dapat
 * mengintip data siswa lain. Guru/admin mengirim parameter <code>siswa</code> secara eksplisit untuk
 * mencatat aktivitas anak didiknya.</p>
 *
 * <h2>Format respons</h2>
 * <p>Mengikuti konvensi {@link ApiHelperSupport}: <code>status</code> (<code>"00"</code> sukses,
 * <code>"97"</code> token/parameter salah, <code>"99"</code> kosong, <code>"500"</code> error) dan
 * <code>description</code>. Data pada <code>list</code> (array) atau <code>data</code> (objek).
 * Tanggal memakai format <code>yyyy-MM-dd</code> ({@code Common.dateFormat1}).</p>
 *
 * <h2>Kompatibilitas</h2>
 * <p>Gaya paket {@code ais.action.servlet.api}: tanpa lambda, gagal-aman (selalu mengembalikan
 * JSON), query baca via {@link HibernateUtil#getSessionFactory()}.openSession(), tulis via
 * {@link Common#refreshSaveOrUpdate(Session, Object)}. Kompatibel Java 1.7.</p>
 */
public final class AktifitasHarianSiswaApi {

    private AktifitasHarianSiswaApi() {
    }

    // ════════════════════════════════════════════════════════════════════════
    // 1) DAFTAR  — action: "aktifitas_harian_siswa_daftar"
    // ════════════════════════════════════════════════════════════════════════

    /**
     * <h2>{@code daftar} — Daftar Aktifitas Harian Siswa</h2>
     *
     * <p>Mengembalikan daftar aktivitas harian seorang siswa pada rentang tanggal tertentu, terurut
     * dari yang terbaru. Endpoint ini adalah tampilan utama bagi orang tua untuk memantau kegiatan
     * anaknya hari demi hari, sekaligus bagi guru untuk meninjau entri yang sudah dibuat. Setiap
     * elemen memuat ringkasan kegiatan beserta status apakah pesan pembina dan pesan orang tua sudah
     * terisi, agar aplikasi mobile dapat menandai entri yang menunggu tanggapan.</p>
     *
     * <h3>Permintaan</h3>
     * <ul>
     *   <li><b>token</b> (wajib).</li>
     *   <li><b>siswa</b> (opsional) — id siswa; default {@code Tbmuser.getSiswa()} untuk akun
     *       siswa/orang tua.</li>
     *   <li><b>mulai</b>, <b>sampai</b> (opsional, <code>yyyy-MM-dd</code>) — rentang tanggal; default
     *       30 hari terakhir s.d. hari ini.</li>
     * </ul>
     *
     * <h3>Respons</h3>
     * <p>Field <code>list</code> berisi array; tiap elemen: <code>id</code>, <code>tanggal</code>,
     * <code>nama</code>, <code>keterangan</code>, <code>aktifitas</code>, <code>materi</code>,
     * <code>pesanPembina</code>, <code>pesanOrangTua</code>, <code>adaPesanPembina</code> (boolean),
     * <code>adaPesanOrangTua</code> (boolean). Bila kosong, status <code>"99"</code> dengan
     * <code>list</code> berupa array kosong.</p>
     *
     * @param req  {@link HttpServletRequest}.
     * @param json payload JSON.
     * @return {@link JSONObject} status + array <code>list</code>.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject daftar(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            Siswa siswa = resolveSiswa(json, tbmuser);
            if (siswa == null) {
                return ApiHelperSupport.status("97", "Siswa tidak ditemukan");
            }
            session = HibernateUtil.getSessionFactory().openSession();
            Date[] rentang = rentangTanggal(json);
            Criteria criteria = session.createCriteria(AktiftasHarianSiswa.class)
                    .add(Restrictions.eq("siswa", siswa))
                    .add(Restrictions.ge("tanggal", rentang[0]))
                    .add(Restrictions.le("tanggal", rentang[1]))
                    .addOrder(Order.desc("tanggal"));
            List<AktiftasHarianSiswa> list = criteria.list();
            JSONArray arr = new JSONArray();
            if (list != null) {
                for (AktiftasHarianSiswa a : list) {
                    if (a == null) {
                        continue;
                    }
                    arr.put(toJson(a));
                }
            }
            hasil.put("list", arr);
            ApiHelperSupport.putStatus(hasil, arr.length() > 0 ? "00" : "99",
                    arr.length() > 0 ? "OK" : "Belum ada aktifitas harian");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal mengambil aktifitas harian siswa");
        } finally {
            if (session != null) {
                try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/AktifitasHarianSiswaApi.java:142");}
                try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/AktifitasHarianSiswaApi.java:143");}
                try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/AktifitasHarianSiswaApi.java:144");}
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2) DETAIL  — action: "aktifitas_harian_siswa_detail"
    // ════════════════════════════════════════════════════════════════════════

    /**
     * <h2>{@code detail} — Rincian satu Aktifitas Harian</h2>
     *
     * <p>Mengembalikan satu entri {@link AktiftasHarianSiswa} secara lengkap berdasarkan id,
     * termasuk identitas siswa pemilik. Dipakai aplikasi mobile saat membuka halaman detail kegiatan
     * untuk menampilkan uraian penuh sekaligus kotak pesan pembina dan pesan orang tua.</p>
     *
     * <h3>Permintaan</h3>
     * <ul>
     *   <li><b>token</b> (wajib).</li>
     *   <li><b>id</b> (wajib) — id aktifitas harian.</li>
     * </ul>
     *
     * <h3>Respons</h3>
     * <p>Field <code>data</code> berisi seluruh atribut entri (lihat {@link #daftar}) ditambah objek
     * <code>siswa</code> {id, nama, nis}. Status <code>"99"</code> bila id tidak ditemukan.</p>
     *
     * @param req  {@link HttpServletRequest}.
     * @param json payload JSON; wajib memuat <code>id</code>.
     * @return {@link JSONObject} status + objek <code>data</code>.
     */
    public static JSONObject detail(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            if (ApiHelperSupport.isNullOrEmptyJsonValue(json, "id")) {
                return ApiHelperSupport.status("97", "Id aktifitas harus dikirim");
            }
            Long detailId = Long.parseLong(ApiHelperSupport.optString(json, "id"));
            session = HibernateUtil.getSessionFactory().openSession();
            AktiftasHarianSiswa a = (AktiftasHarianSiswa) session.get(AktiftasHarianSiswa.class, detailId);
            if (a == null) {
                return ApiHelperSupport.status("99", "Aktifitas harian tidak ditemukan");
            }
            JSONObject data = toJson(a);
            JSONObject siswaObj = new JSONObject();
            if (a.getSiswa() != null) {
                siswaObj.put("id", a.getSiswa().getId());
                siswaObj.put("nama", ApiHelperSupport.safeString(a.getSiswa().getNama()));
                siswaObj.put("nis", ApiHelperSupport.safeString(a.getSiswa().getNis()));
            }
            data.put("siswa", siswaObj);
            hasil.put("data", data);
            ApiHelperSupport.putSuccess(hasil, "OK");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal mengambil detail aktifitas harian");
        } finally {
            if (session != null) {
                try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/AktifitasHarianSiswaApi.java:206");}
                try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/AktifitasHarianSiswaApi.java:207");}
                try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/AktifitasHarianSiswaApi.java:208");}
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3) SIMPAN (Guru: buat/ubah aktivitas)  — action: "aktifitas_harian_siswa_simpan"
    // ════════════════════════════════════════════════════════════════════════

    /**
     * <h2>{@code simpan} — Guru membuat/memperbarui Aktifitas Harian anak didik</h2>
     *
     * <p>Menyimpan entri aktivitas harian. Ini adalah jalur utama bagi <b>guru/pembina</b> untuk
     * "memasukkan catatan untuk anak-anak didiknya": mencatat kegiatan, keterangan, aktifitas, materi,
     * dan tanggal, serta opsional langsung mengisi pesan pembina. Bila <code>id</code> dikirim →
     * entri diperbarui; bila tidak → entri baru dibuat (pola upsert).</p>
     *
     * <h3>Permintaan</h3>
     * <ul>
     *   <li><b>token</b> (wajib).</li>
     *   <li><b>id</b> (opsional) — update bila ada; create bila tidak.</li>
     *   <li><b>siswa</b> (wajib saat create) — id siswa anak didik.</li>
     *   <li><b>tanggal</b> (opsional, <code>yyyy-MM-dd</code>) — default hari ini.</li>
     *   <li><b>nama</b> (wajib) — judul/nama kegiatan.</li>
     *   <li><b>keterangan</b>, <b>aktifitas</b>, <b>materi</b> (opsional).</li>
     *   <li><b>pesanPembina</b> (opsional) — pesan pembina yang ikut disimpan.</li>
     * </ul>
     *
     * <h3>Respons</h3>
     * <p>Status <code>"00"</code> + <code>id</code> entri tersimpan, atau <code>"97"</code> bila
     * validasi gagal. Field audit <code>oleh</code>/<code>olehId</code> diisi dari token saat create.
     * Penyimpanan via {@link Common#refreshSaveOrUpdate(Session, Object)} (transaksional).</p>
     *
     * @param req  {@link HttpServletRequest}.
     * @param json payload JSON.
     * @return {@link JSONObject} status + <code>id</code>.
     */
    public static JSONObject simpan(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            AktiftasHarianSiswa a;
            boolean baru;
            if (!ApiHelperSupport.isNullOrEmptyJsonValue(json, "id")) {
                a = (AktiftasHarianSiswa) ConstantValues.ambil(AktiftasHarianSiswa.class.getName(),
                        Long.parseLong(ApiHelperSupport.optString(json, "id")));
                if (a == null) {
                    return ApiHelperSupport.status("99", "Aktifitas harian tidak ditemukan");
                }
                baru = false;
            } else {
                a = new AktiftasHarianSiswa();
                baru = true;
            }
            Siswa siswa = resolveSiswa(json, tbmuser);
            if (baru && siswa == null) {
                return ApiHelperSupport.status("97", "Siswa harus dipilih");
            }
            String nama = ApiHelperSupport.optString(json, "nama");
            if (!ApiHelperSupport.hasText(nama)) {
                return ApiHelperSupport.status("97", "Nama kegiatan harus diisi");
            }
            if (siswa != null) {
                a.setSiswa(siswa);
            }
            a.setNama(nama.trim());
            a.setTanggal(json.isNull("tanggal") ? (a.getTanggal() == null ? WaktuUtil.getDate() : a.getTanggal())
                    : Common.dateFormat1.get().parse(ApiHelperSupport.optString(json, "tanggal")));
            if (json.has("keterangan") && !json.isNull("keterangan")) {
                a.setKeterangan(ApiHelperSupport.optString(json, "keterangan"));
            }
            if (json.has("aktifitas") && !json.isNull("aktifitas")) {
                a.setAktifitas(ApiHelperSupport.optString(json, "aktifitas"));
            }
            if (json.has("materi") && !json.isNull("materi")) {
                a.setMateri(ApiHelperSupport.optString(json, "materi"));
            }
            if (json.has("pesanPembina") && !json.isNull("pesanPembina")) {
                a.setPesanPembina(ApiHelperSupport.optString(json, "pesanPembina"));
            }
            if (baru) {
                a.setOleh(ApiHelperSupport.safeString(tbmuser.getNama()));
                a.setOlehId(ApiHelperSupport.safeString(tbmuser.getUserId()));
            }
            session = HibernateUtil.getSessionFactory().openSession();
            session.getTransaction().begin();
            Common.refreshSaveOrUpdate(session, a);
            session.getTransaction().commit();
            hasil.put("id", a.getId());
            ApiHelperSupport.putSuccess(hasil, baru ? "Aktifitas harian disimpan" : "Aktifitas harian diperbarui");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal menyimpan aktifitas harian");
        } finally {
            if (session != null) {
                try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/AktifitasHarianSiswaApi.java:307");}
                try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/AktifitasHarianSiswaApi.java:308");}
                try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/AktifitasHarianSiswaApi.java:309");}
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4) PESAN ORANG TUA  — action: "aktifitas_harian_siswa_pesan_orang_tua"
    // ════════════════════════════════════════════════════════════════════════

    /**
     * <h2>{@code pesanOrangTua} — Orang Tua mengisi/memperbarui pesan untuk satu aktivitas</h2>
     *
     * <p>Menuliskan atau memperbarui field {@code pesanOrangTua} pada satu entri aktivitas harian.
     * Inilah cara <b>orang tua memasukkan catatan untuk anaknya</b>: menanggapi kegiatan/pesan
     * pembina hari itu (mis. konfirmasi, izin, tanggapan, atau catatan kondisi anak di rumah).
     * Mengikuti perilaku halaman web {@code CatatanOrangTuaAktiftasHarianAction} yang menyimpan
     * komentar orang tua per hari.</p>
     *
     * <h3>Permintaan</h3>
     * <ul>
     *   <li><b>token</b> (wajib).</li>
     *   <li><b>id</b> (wajib) — id aktifitas harian yang ditanggapi.</li>
     *   <li><b>pesan</b> (wajib) — isi pesan orang tua.</li>
     * </ul>
     *
     * <h3>Respons</h3>
     * <p>Status <code>"00"</code> bila tersimpan; <code>"97"</code> bila token/parameter tidak valid;
     * <code>"99"</code> bila aktivitas tidak ditemukan. Keamanan: bila akun adalah siswa/orang tua,
     * layanan memastikan entri yang ditanggapi memang milik siswa pada akun (mencegah menanggapi
     * aktivitas siswa lain).</p>
     *
     * @param req  {@link HttpServletRequest}.
     * @param json payload JSON; wajib <code>id</code> dan <code>pesan</code>.
     * @return {@link JSONObject} status hasil.
     */
    public static JSONObject pesanOrangTua(HttpServletRequest req, JSONObject json) {
        return simpanPesan(req, json, true);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5) PESAN PEMBINA  — action: "aktifitas_harian_siswa_pesan_pembina"
    // ════════════════════════════════════════════════════════════════════════

    /**
     * <h2>{@code pesanPembina} — Guru/Pembina mengisi/memperbarui pesan untuk satu aktivitas</h2>
     *
     * <p>Menuliskan atau memperbarui field {@code pesanPembina} pada satu entri aktivitas harian.
     * Dipakai guru/wali untuk menyampaikan pesan kepada siswa &amp; orang tua terkait kegiatan hari
     * itu (mis. apresiasi, arahan, atau hal yang perlu ditindaklanjuti di rumah). Berpasangan dengan
     * {@link #pesanOrangTua(HttpServletRequest, JSONObject)} membentuk komunikasi dua-arah.</p>
     *
     * <h3>Permintaan</h3>
     * <ul>
     *   <li><b>token</b> (wajib).</li>
     *   <li><b>id</b> (wajib) — id aktifitas harian.</li>
     *   <li><b>pesan</b> (wajib) — isi pesan pembina.</li>
     * </ul>
     *
     * <h3>Respons</h3>
     * <p>Status <code>"00"</code> bila tersimpan; <code>"97"</code>/<code>"99"</code> sesuai validasi.
     * Penyimpanan via {@link Common#refreshSaveOrUpdate(Session, Object)}.</p>
     *
     * @param req  {@link HttpServletRequest}.
     * @param json payload JSON; wajib <code>id</code> dan <code>pesan</code>.
     * @return {@link JSONObject} status hasil.
     */
    public static JSONObject pesanPembina(HttpServletRequest req, JSONObject json) {
        return simpanPesan(req, json, false);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helper internal
    // ════════════════════════════════════════════════════════════════════════

    /** Inti penyimpanan pesan; {@code orangTua=true} mengisi pesanOrangTua, selain itu pesanPembina. */
    private static JSONObject simpanPesan(HttpServletRequest req, JSONObject json, boolean orangTua) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            if (ApiHelperSupport.isNullOrEmptyJsonValue(json, "id")) {
                return ApiHelperSupport.status("97", "Id aktifitas harus dikirim");
            }
            String pesan = ApiHelperSupport.optString(json, "pesan");
            AktiftasHarianSiswa a = (AktiftasHarianSiswa) ConstantValues.ambil(AktiftasHarianSiswa.class.getName(),
                    Long.parseLong(ApiHelperSupport.optString(json, "id")));
            if (a == null) {
                return ApiHelperSupport.status("99", "Aktifitas harian tidak ditemukan");
            }
            // Keamanan: akun siswa/orang tua hanya boleh menanggapi aktivitas miliknya sendiri.
            if (orangTua && tbmuser.getSiswa() != null && a.getSiswa() != null
                    && !tbmuser.getSiswa().getId().equals(a.getSiswa().getId())) {
                return ApiHelperSupport.status("97", "Tidak berhak menanggapi aktifitas siswa lain");
            }
            if (orangTua) {
                a.setPesanOrangTua(pesan);
            } else {
                a.setPesanPembina(pesan);
            }
            session = HibernateUtil.getSessionFactory().openSession();
            session.getTransaction().begin();
            Common.refreshSaveOrUpdate(session, a);
            session.getTransaction().commit();
            ApiHelperSupport.putSuccess(hasil, orangTua ? "Pesan orang tua tersimpan" : "Pesan pembina tersimpan");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal menyimpan pesan aktifitas harian");
        } finally {
            if (session != null) {
                try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/AktifitasHarianSiswaApi.java:421");}
                try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/AktifitasHarianSiswaApi.java:422");}
                try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/AktifitasHarianSiswaApi.java:423");}
            }
        }
    }

    /** Petakan entri aktivitas harian ke JSON. */
    private static JSONObject toJson(AktiftasHarianSiswa a) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", a.getId());
        o.put("tanggal", a.getTanggal() == null ? "" : Common.dateFormat1.get().format(a.getTanggal()));
        o.put("nama", ApiHelperSupport.safeString(a.getNama()));
        o.put("keterangan", ApiHelperSupport.safeString(a.getKeterangan()));
        o.put("aktifitas", ApiHelperSupport.safeString(a.getAktifitas()));
        o.put("materi", ApiHelperSupport.safeString(a.getMateri()));
        o.put("pesanPembina", ApiHelperSupport.safeString(a.getPesanPembina()));
        o.put("pesanOrangTua", ApiHelperSupport.safeString(a.getPesanOrangTua()));
        o.put("adaPesanPembina", ApiHelperSupport.hasText(a.getPesanPembina()));
        o.put("adaPesanOrangTua", ApiHelperSupport.hasText(a.getPesanOrangTua()));
        return o;
    }

    /** Resolusi siswa dari parameter "siswa", atau dari akun bila login sebagai siswa/orang tua. */
    private static Siswa resolveSiswa(JSONObject json, Tbmuser tbmuser) {
        try {
            if (!ApiHelperSupport.isNullOrEmptyJsonValue(json, "siswa")) {
                return (Siswa) ConstantValues.ambil(Siswa.class.getName(),
                        Long.parseLong(ApiHelperSupport.optString(json, "siswa")));
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/AktifitasHarianSiswaApi.java:451");
        }
        return tbmuser == null ? null : tbmuser.getSiswa();
    }

    /** Rentang tanggal default 30 hari terakhir s.d. hari ini; menghormati "mulai"/"sampai". */
    private static Date[] rentangTanggal(JSONObject json) throws Exception {
        Calendar cal = WaktuUtil.getCalendar();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        Date mulai = json.isNull("mulai") ? cal.getTime()
                : Common.dateFormat1.get().parse(ApiHelperSupport.optString(json, "mulai"));
        Date sampai = json.isNull("sampai") ? WaktuUtil.getDate()
                : Common.dateFormat1.get().parse(ApiHelperSupport.optString(json, "sampai"));
        return new Date[] { mulai, sampai };
    }
}
