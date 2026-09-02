package ais.common.newui.audit;

import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.helper.LaporanKunjunganPenggunaHelper;
import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;

/**
 * Kontrak read-only halaman Log Login / Laporan Kunjungan Sistem.
 *
 * <p>Log akses merupakan data audit. Karena itu controller ini tidak memakai
 * Generic CRUD dan sengaja tidak mengirim {@code header}, id sesi, atau relasi
 * entity mentah. Klien hanya menerima informasi yang memang ditampilkan layar
 * lama: waktu, identitas pengguna, kanal, IP, unit, status, dan keterangan.</p>
 *
 * <p>Daftar dan ringkasan memakai filter yang sama. Ekspor Excel menggunakan
 * pembangun workbook milik layar ZK agar isi laporan desktop/Android tidak
 * memiliki rumus dan definisi metrik yang berbeda.</p>
 */
public final class NewUiLogLoginController {

    private static final String MODULE = "root";
    private static final String PAGE = "log_login";
    private static final int PAGE_SIZE_DEFAULT = 50;
    private static final int PAGE_SIZE_MAX = 100;

    private static final String JENIS =
            "case when a.mahasiswa is not null then 'Mahasiswa' "
            + "when a.dosen is not null then 'Dosen' "
            + "when a.pegawai is not null then 'Pegawai' "
            + "when a.guru is not null then 'Guru' "
            + "when a.siswa is not null then 'Siswa' "
            + "when a.tbmuser is not null then 'Admin / Operator' "
            + "when a.penduduk is not null then 'Penduduk / Umum' "
            + "else 'Lainnya' end";

    private static final String MOBILE =
            "(coalesce(a.description,'') ilike '%android%' "
            + "or coalesce(a.description,'') ilike '%mobile%' "
            + "or coalesce(a.description,'') ilike '%ios%' "
            + "or coalesce(a.description,'') ilike '%iphone%')";

    private static final String[] SEMUA_JENIS = {
        "Mahasiswa", "Dosen", "Pegawai", "Guru", "Siswa",
        "Admin / Operator", "Penduduk / Umum", "Lainnya"
    };

    private NewUiLogLoginController() {
    }

    public static void handle(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        JSONObject json = new JSONObject();
        try {
            String action = text(request.getParameter("action"), "meta").toLowerCase();
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, PAGE, action)) {
                response.setStatus(403);
                fail(json, "ACTION_FORBIDDEN", "Hak baca laporan kunjungan tidak tersedia.");
                write(response, json);
                return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null || user.getUserId() == null) {
                throw new SecurityException("Sesi pengguna tidak dikenal.");
            }
            if ("meta".equals(action)) {
                meta(json);
            } else if ("options".equals(action)) {
                options(json, request);
            } else if ("list".equals(action)) {
                list(json, request);
            } else if ("ringkasan".equals(action)) {
                ringkasan(json, request);
            } else if ("export".equals(action) || "export_xlsx".equals(action)) {
                export(request, response);
                return;
            } else {
                throw new IllegalArgumentException("Aksi laporan kunjungan tidak dikenal.");
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
                    "Gagal memproses laporan kunjungan. Detail dicatat di log server.");
            try {
                ais.common.ErrorAuditUtil.record(e, "NewUiLogLoginController");
            } catch (Exception ignored) {
            }
        }
        write(response, json);
    }

    private static void meta(JSONObject j) throws Exception {
        Filter f = Filter.defaults();
        j.put("judul", "Laporan Kunjungan Sistem");
        j.put("readOnly", true);
        j.put("mulaiBawaan", format(f.mulai));
        j.put("sampaiBawaan", format(f.sampai));
        j.put("pageSize", PAGE_SIZE_DEFAULT);
        j.put("pageSizeMaximum", PAGE_SIZE_MAX);
        j.put("pilihanJenis", new JSONArray(SEMUA_JENIS));
        j.put("kolomSensitifDihilangkan", new JSONArray(
                new String[] {"header", "sessionid"}));
        j.put("eksporMengikuti", new JSONArray(
                new String[] {"mulai", "sampai", "fakultas", "jurusan", "yayasan", "sekolah"}));
        j.put("catatanEkspor",
                "Excel lengkap mengikuti periode dan unit seperti laporan lama; pencarian teks, IP, dan jenis hanya menyaring daftar layar.");
    }

    private static void options(JSONObject j, HttpServletRequest request)
            throws Exception {
        String kind = text(request.getParameter("kind"), "").toLowerCase();
        String q = text(request.getParameter("q"), "").toLowerCase();
        if (q.length() > 100) q = q.substring(0, 100);
        String table;
        if ("fakultas".equals(kind)) table = "fakultas";
        else if ("jurusan".equals(kind)) table = "jurusan";
        else if ("yayasan".equals(kind)) table = "sekolah.yayasan";
        else if ("sekolah".equals(kind)) table = "sekolah.sekolah";
        else throw new IllegalArgumentException("Jenis pilihan unit tidak dikenal.");

        Session s = HibernateUtil.openSession();
        try {
            SQLQuery query = s.createSQLQuery("select id,nama from " + table
                    + (q.length() == 0 ? "" : " where lower(coalesce(nama,'')) like :q")
                    + " order by nama limit 500");
            if (q.length() > 0) query.setString("q", "%" + q + "%");
            JSONArray rows = new JSONArray();
            @SuppressWarnings("unchecked")
            List<Object[]> result = query.list();
            for (Object[] row : result) {
                rows.put(new JSONObject().put("id", number(row[0]))
                        .put("nama", string(row[1])));
            }
            j.put("rows", rows).put("kind", kind)
                    .put("dibatasi", result.size() >= 500);
        } finally {
            s.close();
        }
    }

    private static void list(JSONObject j, HttpServletRequest request)
            throws Exception {
        Filter f = Filter.from(request, true);
        Session s = HibernateUtil.openSession();
        try {
            String from = " from log_login a "
                    + "left join fakultas f on f.id=a.fakultas "
                    + "left join jurusan p on p.id=a.jurusan "
                    + "left join sekolah.yayasan y on y.id=a.yayasan "
                    + "left join sekolah.sekolah se on se.id=a.sekolah ";
            SQLQuery count = s.createSQLQuery("select count(*)" + from + f.where);
            bind(count, f);
            long total = number(count.uniqueResult());

            String select = "select a.id,a.\"login\",a.logout,a.ip,a.nama,"
                    + JENIS + ",coalesce(f.nama,y.nama,''),coalesce(p.nama,se.nama,''),"
                    + "a.success_status,a.description," + MOBILE;
            SQLQuery query = s.createSQLQuery(select + from + f.where
                    + " order by a.id desc");
            bind(query, f);
            query.setFirstResult((f.page - 1) * f.pageSize);
            query.setMaxResults(f.pageSize);
            @SuppressWarnings("unchecked")
            List<Object[]> result = query.list();
            JSONArray rows = new JSONArray();
            for (Object[] row : result) rows.put(toJson(row));
            j.put("rows", rows).put("total", total)
                    .put("pageNumber", f.page).put("pageSize", f.pageSize)
                    .put("mulai", format(f.mulai)).put("sampai", format(f.sampai));
        } finally {
            s.close();
        }
    }

    private static void ringkasan(JSONObject j, HttpServletRequest request)
            throws Exception {
        Filter f = Filter.from(request, true);
        Session s = HibernateUtil.openSession();
        try {
            SQLQuery totalQ = s.createSQLQuery("select count(*),count(distinct a.nama),"
                    + "sum(case when a.success_status then 1 else 0 end),"
                    + "sum(case when a.success_status is not true then 1 else 0 end),"
                    + "sum(case when " + MOBILE + " then 1 else 0 end),"
                    + "count(distinct date(a.\"login\")) from log_login a " + f.where);
            bind(totalQ, f);
            Object[] total = (Object[]) totalQ.uniqueResult();
            long kunjungan = number(total[0]);
            j.put("total", kunjungan)
                    .put("penggunaUnik", number(total[1]))
                    .put("berhasil", number(total[2]))
                    .put("gagal", number(total[3]))
                    .put("mobile", number(total[4]))
                    .put("web", kunjungan - number(total[4]))
                    .put("hariAktif", number(total[5]));

            SQLQuery jenis = s.createSQLQuery("select " + JENIS + ",count(*) "
                    + "from log_login a " + f.where + " group by 1 order by 2 desc");
            bind(jenis, f);
            j.put("perJenis", pairs(jenis.list()));

            SQLQuery harian = s.createSQLQuery(
                    "select to_char(a.\"login\",'YYYY-MM-DD'),count(*),"
                    + "sum(case when a.success_status is not true then 1 else 0 end) "
                    + "from log_login a " + f.where + " group by 1 order by 1");
            bind(harian, f);
            JSONArray tren = new JSONArray();
            @SuppressWarnings("unchecked")
            List<Object[]> h = harian.list();
            for (Object[] row : h) {
                tren.put(new JSONObject().put("tanggal", string(row[0]))
                        .put("total", number(row[1])).put("gagal", number(row[2])));
            }
            j.put("trenHarian", tren);
        } finally {
            s.close();
        }
    }

    private static void export(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Filter f = Filter.from(request, false);
        byte[] bytes = LaporanKunjunganPenggunaHelper.buatExcel(
                f.mulai, f.sampai, f.fakultas, f.jurusan, f.yayasan, f.sekolah);
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\""
                + LaporanKunjunganPenggunaHelper.namaBerkasExcel() + "\"");
        response.setContentLength(bytes.length);
        OutputStream out = response.getOutputStream();
        out.write(bytes);
        out.flush();
    }

    private static JSONObject toJson(Object[] r) throws Exception {
        Date login = date(r[1]);
        Date logout = date(r[2]);
        long lama = login == null ? 0L
                : Math.max(0L, (logout == null ? new Date() : logout).getTime() - login.getTime());
        return new JSONObject().put("id", number(r[0]))
                .put("login", epoch(login)).put("logout", epoch(logout))
                .put("lamaDetik", lama / 1000L)
                .put("ip", string(r[3])).put("nama", string(r[4]))
                .put("jenis", string(r[5])).put("unit", string(r[6]))
                .put("subUnit", string(r[7]))
                .put("sukses", r[8] == null || Boolean.TRUE.equals(r[8]))
                .put("keterangan", string(r[9])).put("mobile", Boolean.TRUE.equals(r[10]));
    }

    private static JSONObject pairs(List<?> list) throws Exception {
        JSONObject result = new JSONObject();
        for (Object value : list) {
            Object[] row = (Object[]) value;
            result.put(string(row[0]), number(row[1]));
        }
        return result;
    }

    private static void bind(SQLQuery query, Filter f) {
        query.setTimestamp("mulai", f.mulai);
        query.setTimestamp("akhir", f.akhirEksklusif);
        if (f.q.length() > 0) query.setString("q", "%" + f.q.toLowerCase() + "%");
        if (f.ip.length() > 0) query.setString("ip", "%" + f.ip.toLowerCase() + "%");
        if (f.fakultas != null) query.setLong("fakultas", f.fakultas.longValue());
        if (f.jurusan != null) query.setLong("jurusan", f.jurusan.longValue());
        if (f.yayasan != null) query.setLong("yayasan", f.yayasan.longValue());
        if (f.sekolah != null) query.setLong("sekolah", f.sekolah.longValue());
        if (!f.jenis.isEmpty()) query.setParameterList("jenis", f.jenis);
    }

    private static final class Filter {
        Date mulai, sampai, akhirEksklusif;
        String q = "", ip = "", where;
        Long fakultas, jurusan, yayasan, sekolah;
        Collection<String> jenis = new ArrayList<String>();
        int page = 1, pageSize = PAGE_SIZE_DEFAULT;

        static Filter defaults() {
            Filter f = new Filter();
            Calendar c = Calendar.getInstance();
            f.sampai = atStart(c.getTime());
            c.add(Calendar.DATE, -30);
            f.mulai = atStart(c.getTime());
            c.setTime(f.sampai);
            c.add(Calendar.DATE, 1);
            f.akhirEksklusif = c.getTime();
            return f;
        }

        static Filter from(HttpServletRequest r, boolean detail) {
            Filter f = defaults();
            f.mulai = parseDate(r.getParameter("mulai"), f.mulai);
            f.sampai = parseDate(r.getParameter("sampai"), f.sampai);
            if (f.mulai.after(f.sampai)) {
                throw new IllegalArgumentException("Tanggal mulai tidak boleh melewati tanggal selesai.");
            }
            Calendar end = Calendar.getInstance();
            end.setTime(atStart(f.sampai));
            end.add(Calendar.DATE, 1);
            f.akhirEksklusif = end.getTime();
            f.q = detail ? limited(r.getParameter("q"), 150) : "";
            f.ip = detail ? limited(r.getParameter("ip"), 80) : "";
            f.fakultas = positive(r.getParameter("fakultas"));
            f.jurusan = positive(r.getParameter("jurusan"));
            f.yayasan = positive(r.getParameter("yayasan"));
            f.sekolah = positive(r.getParameter("sekolah"));
            if (detail) f.jenis = selected(r.getParameter("jenis"));
            f.page = integer(r.getParameter("page"), 1, 1, 1000000);
            f.pageSize = integer(r.getParameter("pageSize"), PAGE_SIZE_DEFAULT, 1, PAGE_SIZE_MAX);
            StringBuilder w = new StringBuilder("where a.\"login\">=:mulai and a.\"login\"<:akhir");
            if (f.q.length() > 0) w.append(" and (lower(coalesce(a.nama,'')) like :q or lower(coalesce(a.description,'')) like :q or lower(coalesce(a.link_profile,'')) like :q)");
            if (f.ip.length() > 0) w.append(" and lower(coalesce(a.ip,'')) like :ip");
            if (f.fakultas != null) w.append(" and a.fakultas=:fakultas");
            if (f.jurusan != null) w.append(" and a.jurusan=:jurusan");
            if (f.yayasan != null) w.append(" and a.yayasan=:yayasan");
            if (f.sekolah != null) w.append(" and a.sekolah=:sekolah");
            if (!f.jenis.isEmpty()) w.append(" and ").append(JENIS).append(" in (:jenis)");
            f.where = w.toString();
            return f;
        }
    }

    private static Collection<String> selected(String raw) {
        List<String> result = new ArrayList<String>();
        if (raw == null || raw.trim().length() == 0) return result;
        String[] values = raw.split(",");
        for (int i = 0; i < values.length; i++) {
            String candidate = values[i].trim();
            boolean valid = false;
            for (int j = 0; j < SEMUA_JENIS.length; j++) {
                if (SEMUA_JENIS[j].equals(candidate)) { valid = true; break; }
            }
            if (!valid) throw new IllegalArgumentException("Jenis pengguna tidak sah.");
            if (!result.contains(candidate)) result.add(candidate);
        }
        return result;
    }

    private static Date parseDate(String raw, Date fallback) {
        if (raw == null || raw.trim().length() == 0) return fallback;
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        f.setLenient(false);
        try { return f.parse(raw.trim()); }
        catch (ParseException e) { throw new IllegalArgumentException("Format tanggal harus yyyy-MM-dd."); }
    }

    private static Date atStart(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date); c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static Long positive(String raw) {
        if (raw == null || raw.trim().length() == 0) return null;
        try {
            Long value = Long.valueOf(raw.trim());
            return value.longValue() > 0 ? value : null;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Id filter unit tidak sah.");
        }
    }

    private static int integer(String raw, int fallback, int min, int max) {
        if (raw == null || raw.trim().length() == 0) return fallback;
        try {
            int value = Integer.parseInt(raw.trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException e) { return fallback; }
    }

    private static String limited(String raw, int max) {
        String value = text(raw, "");
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String format(Date value) {
        return new SimpleDateFormat("yyyy-MM-dd").format(value);
    }

    private static Date date(Object value) {
        return value instanceof Date ? (Date) value : null;
    }

    private static Object epoch(Date value) {
        return value == null ? JSONObject.NULL : Long.valueOf(value.getTime());
    }

    private static long number(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(String.valueOf(value)); }
        catch (Exception e) { return 0L; }
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String text(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value.trim();
    }

    private static void fail(JSONObject j, String code, String message)
            throws Exception {
        j.put("ok", false).put("code", code).put("message", message);
    }

    private static void write(HttpServletResponse response, JSONObject json)
            throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(json.toString());
    }
}
