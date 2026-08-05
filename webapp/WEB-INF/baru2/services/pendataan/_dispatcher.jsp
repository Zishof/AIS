<%@ page contentType="application/json;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="
    ais.common.Common,
    ais.database.hibernate.HibernateUtil,
    ais.database.model.*,
    org.hibernate.Session,
    org.hibernate.Query,
    org.json.JSONArray,
    org.json.JSONObject,
    java.util.List
" %>
<%--
  _dispatcher.jsp — Dispatcher terpusat untuk semua layanan modul Pendataan.

  Endpoint pola:
    GET  /baru2?modul=pendataan&svc={svc}&{params}  → { total, data[] }
    POST /baru2?modul=pendataan&svc={svc}           → { ok, msg }

  Daftar svc yang ditangani di sini:
    ref_fakultas           → daftar fakultas aktif
    ref_prodi              → daftar prodi (filter: ?fakultasId=)
    ref_prodi_all          → semua prodi
    ref_tahun_akademik     → semua tahun akademik
    mahasiswa_list         → daftar mahasiswa (paginasi + filter)
    dosen_list             → daftar dosen (paginasi + filter)
    matakuliah_list        → daftar MK
    (dst — lihat switch-case di bawah)

  Setiap svc yang belum diimplementasikan akan mengembalikan data stub kosong,
  bukan error 500, sehingga UI tetap berfungsi tanpa backend lengkap.
--%>
<%!
    /** Bungkus nilai string JSON-safe */
    private String s(Object v) {
        if (v == null) return "";
        return String.valueOf(v).trim();
    }
    private int i(Object v, int def) {
        try { return Integer.parseInt(String.valueOf(v).trim()); }
        catch (Exception e) { return def; }
    }
%>
<%
response.setContentType("application/json;charset=UTF-8");

String svc  = request.getParameter("svc");
String mth  = request.getMethod();
if (svc == null) svc = "";

JSONObject res = new JSONObject();
Session db = null;

try {
    db = HibernateUtil.openSession();

    /* ============================================================
     *  GET — referensi data master
     * ============================================================ */
    if ("ref_fakultas".equals(svc)) {
        List list = db.createQuery("FROM Fakultas f WHERE f.aktif=true ORDER BY f.nama").list();
        JSONArray arr = new JSONArray();
        for (Object o : list) {
            Fakultas f = (Fakultas) o;
            JSONObject row = new JSONObject();
            row.put("id", f.getId());
            row.put("nama", s(f.getNama()));
            arr.put(row);
        }
        res.put("total", arr.length());
        res.put("data", arr);

    } else if ("ref_prodi".equals(svc) || "ref_prodi_all".equals(svc)) {
        String fakId = request.getParameter("fakultasId");
        String hql = "FROM ProgramStudi p WHERE 1=1"
                   + ("ref_prodi".equals(svc) && fakId != null && !fakId.isEmpty() ? " AND p.fakultas.id=:fak" : "")
                   + " ORDER BY p.nama";
        Query q = db.createQuery(hql);
        if ("ref_prodi".equals(svc) && fakId != null && !fakId.isEmpty()) {
            q.setParameter("fak", Long.parseLong(fakId));
        }
        List list = q.list();
        JSONArray arr = new JSONArray();
        for (Object o : list) {
            ProgramStudi p = (ProgramStudi) o;
            JSONObject row = new JSONObject();
            row.put("id", p.getId());
            row.put("nama", s(p.getNama()));
            if (p.getFakultas() != null) row.put("fakultasId", p.getFakultas().getId());
            arr.put(row);
        }
        res.put("total", arr.length());
        res.put("data", arr);

    } else if ("ref_tahun_akademik".equals(svc)) {
        List list = db.createQuery("FROM TahunAkademik t ORDER BY t.tahunMulai DESC, t.semester ASC").list();
        JSONArray arr = new JSONArray();
        for (Object o : list) {
            TahunAkademik t = (TahunAkademik) o;
            JSONObject row = new JSONObject();
            row.put("id", t.getId());
            String label = t.getTahunMulai() + "/" + t.getTahunSelesai()
                         + " " + (t.getSemester() == 1 ? "Ganjil" : t.getSemester() == 2 ? "Genap" : "SP");
            row.put("label", label);
            arr.put(row);
        }
        res.put("total", arr.length());
        res.put("data", arr);

    /* ============================================================
     *  GET/POST — modul-modul (stub sampai backend siap)
     * ============================================================ */
    } else if (svc.endsWith("_list") || svc.endsWith("_detail") || svc.startsWith("ref_")) {
        /* Stub GET — kembalikan data kosong */
        res.put("total", 0);
        res.put("data", new JSONArray());
        res.put("_stub", true);

    } else if ("POST".equalsIgnoreCase(mth)) {
        /* Stub POST — semua aksi simpan/ubah/hapus yang belum diimplementasikan */
        res.put("ok", false);
        res.put("msg", "Layanan '" + svc + "' belum diimplementasikan. Hubungi pengembang backend.");

    } else {
        /* Fallback GET lainnya */
        res.put("total", 0);
        res.put("data", new JSONArray());
    }

} catch (Exception ex) {
    res = new JSONObject();
    res.put("ok", false);
    res.put("msg", "Error: " + ex.getMessage());
    if (!"POST".equalsIgnoreCase(mth)) {
        res.put("total", 0);
        res.put("data", new JSONArray());
    }
} finally {
    if (db != null && db.isOpen()) db.close();
}

out.clear();
out.print(res.toString());
%>
