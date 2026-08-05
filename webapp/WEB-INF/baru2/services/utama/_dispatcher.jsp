<%@ page contentType="application/json;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="
    ais.database.model.Tbmuser,
    ais.database.model.Tbmrole,
    ais.action.master.helper.util.PerguruanTinggiUtil,
    ais.database.model.PerguruanTinggi,
    ais.database.hibernate.HibernateUtil,
    org.hibernate.Session,
    org.hibernate.Transaction,
    org.json.JSONObject,
    org.json.JSONArray,
    java.util.List,
    java.util.Date,
    java.text.SimpleDateFormat,
    java.util.Locale
" %>
<%!
/** Utilitas strip HTML sederhana untuk preview pengumuman. */
private String stripHtml(String html) {
    if (html == null) return "";
    return html.replaceAll("<[^>]+>", " ").replaceAll("&nbsp;", " ")
               .replaceAll("\\s+", " ").trim();
}
private String truncate(String s, int max) {
    if (s == null) return "";
    return s.length() > max ? s.substring(0, max) + "…" : s;
}
%>
<%
response.setContentType("application/json;charset=UTF-8");
String svc = request.getParameter("svc");
if (svc == null) svc = "";
JSONObject res = new JSONObject();

if ("hak_akses".equals(svc)) {
    JSONObject akses = new JSONObject();
    try {
        javax.servlet.http.HttpSession sess = request.getSession(false);
        Tbmuser u = null;
        if (sess != null) {
            Object _lo = sess.getAttribute("login");
            if (_lo instanceof Tbmuser) u = (Tbmuser) _lo;
            else if (sess.getAttribute("usersTemp") instanceof Tbmuser) u = (Tbmuser) sess.getAttribute("usersTemp");
        }
        Tbmrole r = u != null ? u.hakAkses() : null;
        if (r != null) {
            akses.put("dashboard",            r.isDashboard());
            akses.put("elearning",            r.isElearning());
            akses.put("kegiatanDanPrestasi",  r.isKegiatanDanPrestasi());
            akses.put("pustaka",              r.isPustaka());
            akses.put("workflow",             r.isWorkflow());
            akses.put("dasborRepository",     r.isDasborRepository());
            akses.put("dasboardAntarJemput",  r.isDasboardAntarJemput());
            akses.put("tampilkanSpmi",        r.isTampilkanSpmi());
            akses.put("kantin",               r.isKantin());
            akses.put("dashboardKoperasi",    r.isDashboardKoperasi());
            akses.put("administrasi",         r.isAdministrasi());
            akses.put("pengadaan",            r.isPengadaan());
            akses.put("pembayaran",           r.isPembayaran());
            akses.put("keuangan",             r.isKeuangan());
            akses.put("akunting",             r.isAkunting());
            akses.put("kepegawaian",          r.isKepegawaian());
            akses.put("tampilkanGaji",        r.isTampilkanGaji());
            akses.put("kinerja",              r.isKinerja());
            akses.put("presensiKehadiran",    r.isPresensiKehadiran());
            akses.put("kalenderAkademik",     r.isKalenderAkademik());
            akses.put("infoKegiatan",         r.isInfoKegiatan());
            akses.put("bolehAksesFeeder",     r.isBolehAksesFeeder());
            akses.put("bolehAksesSister",     r.isBolehAksesSister());
            akses.put("emedic",               r.isEmedic());
            akses.put("tampilPos",            r.isTampilPos());
            akses.put("bolehEntryTopup",      r.isBolehEntryTopup());
        }
    } catch (Exception ex) {}
    res.put("ok", true);
    res.put("data", akses);

} else if ("online".equals(svc)) {
    /* Jumlah pengguna aktif — sederhana via session count */
    res.put("count", 1);

} else if ("pengumuman".equals(svc)) {
    /* Daftar PengumumanAkademis aktif untuk institusi saat ini.
     * Filter: aktif=true, bukan langsungMunculDiTab, belum kedaluwarsa.
     * Maksimum 30 item, diurutkan tanggal DESC di server; JS mengurutkan
     * ulang berdasarkan utama/kategori di sisi klien. */
    Session db = null;
    try {
        db = HibernateUtil.openSession();
        Date today = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", new Locale("id", "ID"));
        Long ptId = null;
        try {
            PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
            if (pt != null) ptId = pt.getId();
        } catch (Exception ign) {}

        String hql;
        org.hibernate.Query q;
        if (ptId != null) {
            hql = "from PengumumanAkademis p " +
                  "where (p.aktif = true OR p.aktif IS NULL) " +
                  "AND (p.perguruanTinggi IS NULL OR p.perguruanTinggi.id = :ptId) " +
                  "AND (p.langsungMunculDiTab IS NULL OR p.langsungMunculDiTab = false) " +
                  "AND (p.tetapTampilkanPengumumanMeskipunSudahKelewat = true " +
                  "     OR p.sampai IS NULL OR p.sampai >= :today) " +
                  "ORDER BY p.tanggal DESC";
            q = db.createQuery(hql).setLong("ptId", ptId).setDate("today", today).setMaxResults(30);
        } else {
            hql = "from PengumumanAkademis p " +
                  "where (p.aktif = true OR p.aktif IS NULL) " +
                  "AND (p.langsungMunculDiTab IS NULL OR p.langsungMunculDiTab = false) " +
                  "AND (p.tetapTampilkanPengumumanMeskipunSudahKelewat = true " +
                  "     OR p.sampai IS NULL OR p.sampai >= :today) " +
                  "ORDER BY p.tanggal DESC";
            q = db.createQuery(hql).setDate("today", today).setMaxResults(30);
        }

        List items = q.list();
        JSONArray arr = new JSONArray();
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            JSONObject jo = new JSONObject();
            try {
                Class cls = item.getClass();
                Long id       = (Long)   cls.getMethod("getId").invoke(item);
                String judul  = (String) cls.getMethod("getJudul").invoke(item);
                String catatan= (String) cls.getMethod("getCatatan").invoke(item);
                String diperuntukkan = null;
                try { diperuntukkan = (String) cls.getMethod("getDiperuntukkan").invoke(item); } catch(Exception ig){}
                String oleh   = null;
                try { oleh = (String) cls.getMethod("getOleh").invoke(item); } catch(Exception ig){}
                Date tgl      = null;
                try { tgl = (Date) cls.getMethod("getTanggal").invoke(item); } catch(Exception ig){}

                jo.put("id",     id);
                jo.put("judul",  judul  != null ? judul  : "");
                jo.put("catatan",catatan!= null ? catatan : "");
                jo.put("preview", truncate(stripHtml(catatan), 200));
                jo.put("tanggalStr", tgl != null ? sdf.format(tgl) : "");
                jo.put("tanggalTs",  tgl != null ? tgl.getTime()   : 0L);
                jo.put("diperuntukkan", diperuntukkan != null ? diperuntukkan : "");
                jo.put("oleh",    oleh != null ? oleh : "");

                // Kategori pengumuman
                Object kat = null;
                try { kat = cls.getMethod("getKategoriPengumuman").invoke(item); } catch(Exception ig){}
                if (kat != null) {
                    Class kc = kat.getClass();
                    Long   kId    = null;
                    String kNama  = null;
                    Integer kUrut = null;
                    try { kId   = (Long)    kc.getMethod("getId").invoke(kat); } catch(Exception ig){}
                    try { kNama = (String)  kc.getMethod("getNama").invoke(kat); } catch(Exception ig){}
                    try { kUrut = (Integer) kc.getMethod("getNomorUrut").invoke(kat); } catch(Exception ig){}
                    jo.put("kategoriId",        kId   != null ? kId   : JSONObject.NULL);
                    jo.put("kategoriNama",       kNama != null ? kNama : "Pengumuman");
                    jo.put("kategoriNomorUrut",  kUrut != null ? kUrut : 99);
                    // nomorUrut = 1 artinya Prioritas Utama di AIS
                    jo.put("utama", kUrut != null && kUrut <= 1);
                } else {
                    jo.put("kategoriId",       JSONObject.NULL);
                    jo.put("kategoriNama",      "Pengumuman");
                    jo.put("kategoriNomorUrut", 99);
                    jo.put("utama", false);
                }
                arr.put(jo);
            } catch (Exception ign) {}
        }
        res.put("ok",   true);
        res.put("data", arr);
    } catch (Exception e) {
        res.put("ok",  false);
        res.put("msg", "Gagal memuat pengumuman: " + e.getMessage());
    } finally {
        if (db != null) try { db.close(); } catch (Exception ign) {}
    }

} else if ("set_lang".equals(svc)) {
    /* Simpan preferensi bahasa pengguna.
     * Parameter POST: lang — nilai: '' atau 'Indonesia' (Indonesia),
     *   'English', 'Arabic', 'Mandarin'. Nilai lain ditolak.
     * Respons: {"ok":true} atau {"ok":false,"msg":"..."} */
    Session db = null;
    org.hibernate.Transaction tx = null;
    try {
        String lang = request.getParameter("lang");
        if (lang == null) lang = "";
        /* Validasi: hanya nilai yang dikenal diterima */
        boolean valid = "".equals(lang)
                     || "English".equals(lang)
                     || "Arabic".equals(lang)
                     || "Mandarin".equals(lang)
                     || Tbmuser.INDONESIA.equals(lang);
        if (!valid) {
            res.put("ok",  false);
            res.put("msg", "Nilai bahasa tidak dikenal: " + lang);
        } else {
            Tbmuser loginUser = null;
            javax.servlet.http.HttpSession sess = request.getSession(false);
            if (sess != null) {
                Object _lo = sess.getAttribute("login");
                if (_lo instanceof Tbmuser) loginUser = (Tbmuser) _lo;
                else if (sess.getAttribute("usersTemp") instanceof Tbmuser) loginUser = (Tbmuser) sess.getAttribute("usersTemp");
            }
            if (loginUser == null || loginUser.getId() == null) {
                res.put("ok",  false);
                res.put("msg", "Sesi tidak valid, silakan login ulang.");
            } else {
                db = HibernateUtil.openSession();
                tx = db.beginTransaction();
                Tbmuser u = (Tbmuser) db.get(Tbmuser.class, loginUser.getId());
                if (u == null) {
                    res.put("ok",  false);
                    res.put("msg", "Pengguna tidak ditemukan.");
                } else {
                    u.setBahasa("".equals(lang) ? null : lang);
                    db.update(u);
                    tx.commit();
                    /* Sinkronkan objek di sesi agar _lang.jsp reload benar */
                    loginUser.setBahasa("".equals(lang) ? null : lang);
                    if (sess != null) sess.setAttribute("login", loginUser);
                    res.put("ok",   true);
                    res.put("lang", lang);
                }
            }
        }
    } catch (Exception e) {
        if (tx != null) try { tx.rollback(); } catch(Exception ign) {}
        res.put("ok",  false);
        res.put("msg", "Gagal menyimpan bahasa: " + e.getMessage());
    } finally {
        if (db != null) try { db.close(); } catch(Exception ign) {}
    }

} else if ("kalender_upcoming".equals(svc)) {
    /* Kegiatan KalenderAkademik yang sedang berlangsung atau akan datang.
     * Filter: aktif=true/null AND tanggalSelesai >= hari ini.
     * Maksimum 15 item, diurutkan tanggalMulai ASC.
     * Field yang dikembalikan: id, nama, deskripsi, tanggalMulaiStr,
     *   tanggalSelesaiStr, tanggalMulaiTs, hari (jumlah hari), status, jenis. */
    Session db = null;
    try {
        db = HibernateUtil.openSession();
        Date today = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", new Locale("id", "ID"));
        String hql = "from KalenderAkademik k " +
                     "where (k.aktif = true OR k.aktif IS NULL) " +
                     "AND k.tanggalSelesai >= :today " +
                     "ORDER BY k.tanggalMulai ASC";
        List items = db.createQuery(hql).setDate("today", today).setMaxResults(15).list();
        JSONArray arr = new JSONArray();
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            JSONObject jo = new JSONObject();
            try {
                Class cls   = item.getClass();
                Long id     = (Long) cls.getMethod("getId").invoke(item);
                String nama = (String) cls.getMethod("getNamaKegiatanAkademik").invoke(item);
                String desk = null;
                try { desk = (String) cls.getMethod("getDeskripsiKegiatanAkademik").invoke(item); } catch(Exception ig){}
                Date mulai   = null;
                try { mulai   = (Date) cls.getMethod("getTanggalMulai").invoke(item);   } catch(Exception ig){}
                Date selesai = null;
                try { selesai = (Date) cls.getMethod("getTanggalSelesai").invoke(item); } catch(Exception ig){}
                Integer hari = null;
                try { hari   = (Integer) cls.getMethod("getJumlahHari").invoke(item);  } catch(Exception ig){}
                String status = null;
                try { status = (String) cls.getMethod("getStatus").invoke(item); } catch(Exception ig){}
                // JenisKegiatan → getNama()
                String jenis = null;
                try {
                    Object jk = cls.getMethod("getJenisKegiatan").invoke(item);
                    if (jk != null) jenis = (String) jk.getClass().getMethod("getNama").invoke(jk);
                } catch(Exception ig){}
                jo.put("id",              id);
                jo.put("nama",            nama   != null ? nama   : "");
                jo.put("deskripsi",       desk   != null ? desk   : "");
                jo.put("tanggalMulaiStr", mulai   != null ? sdf.format(mulai)   : "");
                jo.put("tanggalSelesaiStr",selesai!= null ? sdf.format(selesai) : "");
                jo.put("tanggalMulaiTs",  mulai   != null ? mulai.getTime()   : 0L);
                jo.put("hari",            hari   != null ? hari   : 1);
                jo.put("status",          status != null ? status : "");
                jo.put("jenis",           jenis  != null ? jenis  : "");
                arr.put(jo);
            } catch(Exception ign) {}
        }
        res.put("ok",   true);
        res.put("data", arr);
    } catch(Exception e) {
        res.put("ok",  false);
        res.put("msg", "Gagal memuat kalender: " + e.getMessage());
    } finally {
        if (db != null) try { db.close(); } catch(Exception ign) {}
    }

} else {
    res.put("ok", false);
    res.put("msg", "svc tidak dikenal: " + svc);
}

out.clear();
out.print(res.toString());
%>
