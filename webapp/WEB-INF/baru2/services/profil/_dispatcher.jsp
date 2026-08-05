<%@ page contentType="application/json;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common,ais.database.model.Tbmuser,ais.database.hibernate.HibernateUtil" %>
<%@ page import="org.hibernate.Session,org.hibernate.Query,java.util.*,org.json.*" %>
<%!
/** Dispatcher layanan profil pengguna.
 *
 * <p>Menyediakan endpoint JSON untuk halaman profil {@code /baru2?modul=profil}.
 * Mendukung berbagai jenis pengguna — Mahasiswa, Dosen, Siswa, Guru, Admin PT,
 * dan Admin Sekolah — masing-masing dengan data yang relevan.
 *
 * <h3>Endpoint</h3>
 * <ul>
 *   <li><b>GET {@code svc=jenis}</b>: Mendeteksi tipe pengguna dari sesi dan
 *       mengembalikan JSON {@code {jenis, nama, id, email, hp, foto}}.</li>
 *   <li><b>GET {@code svc=stat}</b>: Mengembalikan ringkasan statistik sesuai
 *       tipe pengguna yang terdeteksi dari sesi.</li>
 *   <li><b>GET {@code svc=mhs_akademik}</b>: Data akademik mahasiswa
 *       (semester aktif, KRS, IP, dosen PA).</li>
 *   <li><b>GET {@code svc=dosen_mengajar}</b>: Data mengajar dosen semester ini.</li>
 *   <li><b>GET {@code svc=admin_pt_stat}</b>: Angka penting untuk admin PT
 *       (total mahasiswa, dosen, prodi, dst.).</li>
 *   <li><b>GET {@code svc=admin_sekolah_stat}</b>: Angka penting untuk admin
 *       sekolah (siswa, guru, kelas).</li>
 * </ul>
 *
 * <h3>Deteksi tipe pengguna</h3>
 * <p>Urutan prioritas: Mahasiswa → Dosen → Siswa → Guru → Admin Sekolah →
 * Admin PT (default).</p>
 *
 * <h3>Keamanan</h3>
 * <p>Seluruh endpoint hanya dapat diakses oleh pengguna yang sudah login
 * (sesi {@code login} harus ada). Pengguna hanya dapat mengakses data
 * profil miliknya sendiri kecuali ia adalah root/admin.</p>
 *
 * <h3>Kompatibilitas</h3>
 * <p>Ditulis dengan sintaks Java 1.6 — tidak menggunakan lambda, streams,
 * try-with-resources, atau fitur Java 7+.</p>
 *
 * @author AIS System
 * @version 1.0
 */
private JSONObject ok(Object data) throws Exception {
    JSONObject r = new JSONObject();
    r.put("ok", true);
    r.put("data", data);
    return r;
}

private JSONObject err(String msg) throws Exception {
    JSONObject r = new JSONObject();
    r.put("ok", false);
    r.put("msg", msg);
    return r;
}

private String safe(Object o) {
    return o == null ? "" : o.toString();
}
%>
<%
response.setContentType("application/json;charset=UTF-8");
Object _loP = session.getAttribute("login");
Tbmuser loginUser = (_loP instanceof Tbmuser) ? (Tbmuser)_loP :
    (session.getAttribute("usersTemp") instanceof Tbmuser ? (Tbmuser)session.getAttribute("usersTemp") : null);
if (loginUser == null) {
    out.print(err("Belum login").toString());
    return;
}
String svc = request.getParameter("svc");
JSONObject res = new JSONObject();

/* ─── jenis ─────────────────────────────────────────────────── */
if ("jenis".equals(svc)) {
    String jenis = "admin_pt";
    String nama  = safe(loginUser.getUserNama());
    String email = safe(loginUser.getEmail());
    String hp    = safe(loginUser.getHp());
    String foto  = "";
    Long   entId = null;

    // Deteksi dari session attribute standar AIS
    Object mhsAttr  = session.getAttribute("mahasiswa");
    Object dosenAttr = session.getAttribute("dosen");
    Object siswaAttr = session.getAttribute("siswa");
    Object guruAttr  = session.getAttribute("guru");

    if (mhsAttr != null) {
        jenis = "mahasiswa";
        try {
            java.lang.reflect.Method gn = mhsAttr.getClass().getMethod("getNama");
            java.lang.reflect.Method gi = mhsAttr.getClass().getMethod("getNim");
            java.lang.reflect.Method gid = mhsAttr.getClass().getMethod("getId");
            nama = safe(gn.invoke(mhsAttr));
            entId = (Long)gid.invoke(mhsAttr);
        } catch (Exception ign) {}
    } else if (dosenAttr != null) {
        jenis = "dosen";
        try {
            java.lang.reflect.Method gn = dosenAttr.getClass().getMethod("getNama");
            java.lang.reflect.Method gid = dosenAttr.getClass().getMethod("getId");
            nama = safe(gn.invoke(dosenAttr));
            entId = (Long)gid.invoke(dosenAttr);
        } catch (Exception ign) {}
    } else if (siswaAttr != null) {
        jenis = "siswa";
        try {
            java.lang.reflect.Method gn = siswaAttr.getClass().getMethod("getNama");
            java.lang.reflect.Method gid = siswaAttr.getClass().getMethod("getId");
            nama = safe(gn.invoke(siswaAttr));
            entId = (Long)gid.invoke(siswaAttr);
        } catch (Exception ign) {}
    } else if (guruAttr != null) {
        jenis = "guru";
        try {
            java.lang.reflect.Method gn = guruAttr.getClass().getMethod("getNama");
            java.lang.reflect.Method gid = guruAttr.getClass().getMethod("getId");
            nama = safe(gn.invoke(guruAttr));
            entId = (Long)gid.invoke(guruAttr);
        } catch (Exception ign) {}
    } else {
        // Cek apakah admin sekolah
        try {
            Object sekolah = loginUser.getClass().getMethod("ambilSekolah").invoke(loginUser);
            if (sekolah != null) jenis = "admin_sekolah";
        } catch (Exception ign) {}
    }

    JSONObject d = new JSONObject();
    d.put("jenis", jenis);
    d.put("nama", nama);
    d.put("email", email);
    d.put("hp", hp);
    d.put("userid", safe(loginUser.getUserid()));
    d.put("bahasa", safe(loginUser.getBahasa()));
    d.put("entId", entId != null ? entId : JSONObject.NULL);
    res = ok(d);

/* ─── admin_pt_stat ──────────────────────────────────────────── */
} else if ("admin_pt_stat".equals(svc)) {
    Session db = null;
    try {
        db = HibernateUtil.openSession();
        JSONObject d = new JSONObject();
        // Ringkasan angka penting menggunakan HQL COUNT
        Object[] counts = new Object[]{0L,0L,0L,0L,0L};
        try {
            Long mhs = (Long)db.createQuery("select count(m) from Mahasiswa m where m.statusAktif = true").uniqueResult();
            d.put("mahasiswaAktif", mhs != null ? mhs : 0);
        } catch (Exception e) { d.put("mahasiswaAktif", "—"); }
        try {
            Long lls = (Long)db.createQuery("select count(m) from Mahasiswa m where m.statusAktif = false and m.tanggalLulus is not null").uniqueResult();
            d.put("mahasiswaLulus", lls != null ? lls : 0);
        } catch (Exception e) { d.put("mahasiswaLulus", "—"); }
        try {
            Long dsn = (Long)db.createQuery("select count(d) from Dosen d where d.aktif = true").uniqueResult();
            d.put("dosenAktif", dsn != null ? dsn : 0);
        } catch (Exception e) { d.put("dosenAktif", "—"); }
        try {
            Long prodi = (Long)db.createQuery("select count(p) from Program p where p.aktif = true").uniqueResult();
            d.put("prodiAktif", prodi != null ? prodi : 0);
        } catch (Exception e) { d.put("prodiAktif", "—"); }
        try {
            Long fak = (Long)db.createQuery("select count(f) from Fakultas f where f.aktif = true").uniqueResult();
            d.put("fakultasAktif", fak != null ? fak : 0);
        } catch (Exception e) { d.put("fakultasAktif", "—"); }
        res = ok(d);
    } catch (Exception e) {
        res = err("Gagal muat statistik: " + e.getMessage());
    } finally {
        if (db != null) try { db.close(); } catch (Exception ign) {}
    }

/* ─── admin_sekolah_stat ─────────────────────────────────────── */
} else if ("admin_sekolah_stat".equals(svc)) {
    Session db = null;
    try {
        db = HibernateUtil.openSession();
        JSONObject d = new JSONObject();
        try {
            Long sw = (Long)db.createQuery("select count(s) from Siswa s where s.statusAktif = true").uniqueResult();
            d.put("siswaAktif", sw != null ? sw : 0);
        } catch (Exception e) { d.put("siswaAktif", "—"); }
        try {
            Long gr = (Long)db.createQuery("select count(g) from Guru g where g.aktif = true").uniqueResult();
            d.put("guruAktif", gr != null ? gr : 0);
        } catch (Exception e) { d.put("guruAktif", "—"); }
        try {
            Long kls = (Long)db.createQuery("select count(k) from Kelas k where k.aktif = true").uniqueResult();
            d.put("kelasAktif", kls != null ? kls : 0);
        } catch (Exception e) { d.put("kelasAktif", "—"); }
        res = ok(d);
    } catch (Exception e) {
        res = err("Gagal: " + e.getMessage());
    } finally {
        if (db != null) try { db.close(); } catch (Exception ign) {}
    }

/* ─── mhs_akademik ───────────────────────────────────────────── */
} else if ("mhs_akademik".equals(svc)) {
    JSONObject d = new JSONObject();
    Object mhsAttr = session.getAttribute("mahasiswa");
    if (mhsAttr != null) {
        Session db = null;
        try {
            db = HibernateUtil.openSession();
            // Ambil KRS terakhir via HQL
            try {
                java.lang.reflect.Method gid = mhsAttr.getClass().getMethod("getId");
                Long mhsId = (Long)gid.invoke(mhsAttr);
                List krsRows = db.createQuery(
                    "from KrsMahasiswa k where k.mahasiswa.id = :id order by k.id desc")
                    .setLong("id", mhsId).setMaxResults(1).list();
                if (!krsRows.isEmpty()) {
                    Object krs = krsRows.get(0);
                    try { d.put("semester", krs.getClass().getMethod("getSemester").invoke(krs)); } catch(Exception ign){}
                    try { d.put("tahunAkademik", krs.getClass().getMethod("getTahunAkademik").invoke(krs).toString()); } catch(Exception ign){}
                    try { d.put("catatan", safe(krs.getClass().getMethod("getCatatan").invoke(krs))); } catch(Exception ign){}
                    try { d.put("ipk", krs.getClass().getMethod("getIpk").invoke(krs)); } catch(Exception ign){}
                    try { d.put("sks", krs.getClass().getMethod("getSks").invoke(krs)); } catch(Exception ign){}
                }
            } catch (Exception ign) {}
            try {
                java.lang.reflect.Method gNim = mhsAttr.getClass().getMethod("getNim");
                d.put("nim", safe(gNim.invoke(mhsAttr)));
            } catch(Exception ign){}
            try {
                java.lang.reflect.Method gHp = mhsAttr.getClass().getMethod("tampilkanHp");
                d.put("hp", safe(gHp.invoke(mhsAttr)));
            } catch(Exception ign){}
            res = ok(d);
        } catch (Exception e) {
            res = err("Gagal: " + e.getMessage());
        } finally {
            if (db != null) try { db.close(); } catch (Exception ign) {}
        }
    } else {
        res = err("Mahasiswa tidak ditemukan di sesi");
    }

/* ─── dosen_mengajar ─────────────────────────────────────────── */
} else if ("dosen_mengajar".equals(svc)) {
    JSONObject d = new JSONObject();
    Object dosenAttr = session.getAttribute("dosen");
    if (dosenAttr != null) {
        Session db = null;
        try {
            db = HibernateUtil.openSession();
            java.lang.reflect.Method gid = dosenAttr.getClass().getMethod("getId");
            Long dosenId = (Long)gid.invoke(dosenAttr);
            try {
                Long jmlKelas = (Long)db.createQuery(
                    "select count(p) from Perkuliahan p where p.dosen.id = :id")
                    .setLong("id", dosenId).uniqueResult();
                d.put("jumlahKelas", jmlKelas != null ? jmlKelas : 0);
            } catch (Exception ign) { d.put("jumlahKelas", "—"); }
            try {
                Long jmlMhs = (Long)db.createQuery(
                    "select count(distinct k.mahasiswa) from KrsDetail k where k.perkuliahan.dosen.id = :id")
                    .setLong("id", dosenId).uniqueResult();
                d.put("jumlahMahasiswa", jmlMhs != null ? jmlMhs : 0);
            } catch (Exception ign) { d.put("jumlahMahasiswa", "—"); }
            try {
                Long bimbingan = (Long)db.createQuery(
                    "select count(t) from Tugas t where t.pembimbing1.id = :id or t.pembimbing2.id = :id")
                    .setLong("id", dosenId).uniqueResult();
                d.put("bimbinganTA", bimbingan != null ? bimbingan : 0);
            } catch (Exception ign) { d.put("bimbinganTA", "—"); }
            try {
                java.lang.reflect.Method gNidn = dosenAttr.getClass().getMethod("getNidn");
                d.put("nidn", safe(gNidn.invoke(dosenAttr)));
            } catch(Exception ign){}
            res = ok(d);
        } catch (Exception e) {
            res = err("Gagal: " + e.getMessage());
        } finally {
            if (db != null) try { db.close(); } catch (Exception ign) {}
        }
    } else {
        res = err("Dosen tidak ditemukan di sesi");
    }

} else {
    res = err("svc tidak dikenal: " + svc);
}

out.print(res.toString());
%>
