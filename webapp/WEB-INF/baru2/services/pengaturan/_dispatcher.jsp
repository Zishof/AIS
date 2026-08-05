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
  _dispatcher.jsp — Dispatcher layanan modul Pengaturan.

  Endpoint:
    GET  /baru2?modul=pengaturan&svc={svc}&{params}  → { total, data[] }
    POST /baru2?modul=pengaturan&svc={svc}           → { ok, msg }

  Daftar svc:
    GET:
      grup_list           → daftar Tbmrole (filter: nama, aktif)
      grup_list_all       → semua Tbmrole tanpa paginasi (untuk dropdown)
      grup_detail         → satu Tbmrole + semua flag akses (param: roleId)
      pengguna_list       → daftar Tbmuser (filter: nama, email, role, aktif, dll)
      pengguna_modul_role_list → daftar Role Tambahan (slot userRole2..5) SATU pengguna (param: userid)
      label_list          → daftar LabelBahasa (filter: cari, filter)

    POST:
      grup_tambah         → buat Tbmrole baru
      grup_ubah           → ubah Tbmrole
      grup_akses_simpan   → simpan semua flag modul Tbmrole
      pengguna_tambah     → buat Tbmuser baru
      pengguna_ubah       → ubah Tbmuser
      pengguna_modul_role_simpan → simpan Role Tambahan (slot userRole2..5) SATU pengguna
      pengguna_ubah_status→ aktifkan/nonaktifkan Tbmuser
      pengguna_reset_pwd  → reset password Tbmuser
      ubah_password       → ubah password sendiri (cek passwordLama)
      label_tambah        → buat LabelBahasa baru
      label_ubah          → ubah LabelBahasa
      label_hapus         → hapus LabelBahasa
--%>
<%!
    private String s(Object v) {
        if (v == null) return "";
        return String.valueOf(v).trim();
    }
    private boolean b(Object v) {
        if (v == null) return false;
        String sv = String.valueOf(v).trim().toLowerCase();
        return "true".equals(sv) || "1".equals(sv) || "yes".equals(sv);
    }
    private int i(Object v, int def) {
        try { return Integer.parseInt(String.valueOf(v).trim()); }
        catch (Exception e) { return def; }
    }
    private long l(Object v, long def) {
        try { return Long.parseLong(String.valueOf(v).trim()); }
        catch (Exception e) { return def; }
    }
    /** Simpan semua flag boolean modul ke Tbmrole dari request params. */
    private void applyModulFlags(javax.servlet.http.HttpServletRequest req, Tbmrole role) {
        String[] flags = {
            "dashboard","elearning","kegiatanDanPrestasi","pustaka","workflow",
            "dasborRepository","dasboardAntarJemput","tampilkanSpmi","kantin",
            "dashboardKoperasi","administrasi","pengadaan","pembayaran","keuangan",
            "akunting","kepegawaian","tampilkanGaji","kinerja","presensiKehadiran",
            "kalenderAkademik","infoKegiatan","bolehAksesFeeder","bolehAksesSister",
            "emedic","tampilPos","bolehEntryTopup","melihatDataPegawaiLain",
            "melihatSemuaSurat","melihatSemuaSop"
        };
        for (String flag : flags) {
            String val = req.getParameter(flag);
            if (val == null) continue;
            boolean bval = b(val);
            try {
                java.lang.reflect.Method setter = role.getClass().getMethod(
                    "set" + Character.toUpperCase(flag.charAt(0)) + flag.substring(1), boolean.class);
                setter.invoke(role, bval);
            } catch (Exception ex) {
                /* field tak ada atau nama setter beda — lewati */
            }
        }
    }
    /** Baca semua flag boolean modul dari Tbmrole ke JSONObject. */
    private JSONObject readModulFlags(Tbmrole role) throws Exception {
        JSONObject obj = new JSONObject();
        String[] flags = {
            "dashboard","elearning","kegiatanDanPrestasi","pustaka","workflow",
            "dasborRepository","dasboardAntarJemput","tampilkanSpmi","kantin",
            "dashboardKoperasi","administrasi","pengadaan","pembayaran","keuangan",
            "akunting","kepegawaian","tampilkanGaji","kinerja","presensiKehadiran",
            "kalenderAkademik","infoKegiatan","bolehAksesFeeder","bolehAksesSister",
            "emedic","tampilPos","bolehEntryTopup","melihatDataPegawaiLain",
            "melihatSemuaSurat","melihatSemuaSop"
        };
        for (String flag : flags) {
            try {
                java.lang.reflect.Method getter = role.getClass().getMethod(
                    "is" + Character.toUpperCase(flag.charAt(0)) + flag.substring(1));
                obj.put(flag, getter.invoke(role));
            } catch (Exception ex) {
                try {
                    java.lang.reflect.Method getter2 = role.getClass().getMethod(
                        "get" + Character.toUpperCase(flag.charAt(0)) + flag.substring(1));
                    obj.put(flag, getter2.invoke(role));
                } catch (Exception ex2) { obj.put(flag, false); }
            }
        }
        return obj;
    }
    /** Hash password (MD5 sederhana — ikuti pola sistem). */
    private String hashPassword(String pw) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(pw.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte bb : bytes) sb.append(String.format("%02x", bb));
            return sb.toString();
        } catch (Exception e) { return pw; }
    }
%>
<%
response.setContentType("application/json;charset=UTF-8");
String svc = request.getParameter("svc");
String mth = request.getMethod();
if (svc == null) svc = "";

JSONObject res = new JSONObject();
Session db = null;

try {
    db = HibernateUtil.openSession();

    /* ============================================================
     *  GET — grup_list
     * ============================================================ */
    if ("grup_list".equals(svc) || "grup_list_all".equals(svc)) {
        String nama   = s(request.getParameter("nama"));
        String aktif  = s(request.getParameter("aktif"));
        int halaman   = i(request.getParameter("halaman"), 1);
        int perHalaman= i(request.getParameter("perHalaman"), 20);

        StringBuilder hql = new StringBuilder("FROM Tbmrole r WHERE 1=1");
        if (!nama.isEmpty())  hql.append(" AND (LOWER(r.roleName) LIKE :nama OR LOWER(r.roleId) LIKE :nama)");
        if ("1".equals(aktif)) hql.append(" AND r.aktif=true");
        if ("0".equals(aktif)) hql.append(" AND r.aktif=false");
        hql.append(" ORDER BY r.roleName");

        Query q = db.createQuery(hql.toString());
        if (!nama.isEmpty()) q.setParameter("nama", "%" + nama.toLowerCase() + "%");
        if ("grup_list_all".equals(svc)) {
            q.setMaxResults(500);
        } else {
            q.setFirstResult((halaman - 1) * perHalaman);
            q.setMaxResults(perHalaman);
        }

        List list = q.list();
        JSONArray arr = new JSONArray();
        for (Object o : list) {
            Tbmrole r = (Tbmrole) o;
            JSONObject row = new JSONObject();
            row.put("roleId",   s(r.getRoleId()));
            row.put("roleName", s(r.getRoleName()));
            row.put("kode",     s(r.getKode()));
            row.put("aktif",    r.getAktif());
            /* Hitung jumlah user dalam role ini */
            Long jml = (Long) db.createQuery("SELECT COUNT(u) FROM Tbmuser u WHERE u.userRole=:r")
                                 .setParameter("r", r).uniqueResult();
            row.put("jumlahUser", jml != null ? jml.intValue() : 0);
            arr.put(row);
        }
        res.put("total", arr.length());
        res.put("data", arr);

    /* ============================================================
     *  GET — grup_detail (satu role + flag modul)
     * ============================================================ */
    } else if ("grup_detail".equals(svc)) {
        String roleId = s(request.getParameter("roleId"));
        Tbmrole role = (Tbmrole) db.get(Tbmrole.class, roleId);
        if (role == null) {
            res.put("total", 0); res.put("data", new JSONObject());
        } else {
            JSONObject row = readModulFlags(role);
            row.put("roleId",   s(role.getRoleId()));
            row.put("roleName", s(role.getRoleName()));
            row.put("kode",     s(role.getKode()));
            row.put("aktif",    role.getAktif());
            res.put("total", 1); res.put("data", row);
        }

    /* ============================================================
     *  GET — pengguna_list
     * ============================================================ */
    } else if ("pengguna_list".equals(svc)) {
        String nama   = s(request.getParameter("nama"));
        String email  = s(request.getParameter("email"));
        String role   = s(request.getParameter("role"));
        String aktif  = s(request.getParameter("aktif"));
        int halaman   = i(request.getParameter("halaman"), 1);
        int perHalaman= i(request.getParameter("perHalaman"), i(request.getParameter("limit"), 20));

        StringBuilder hql = new StringBuilder("FROM Tbmuser u WHERE 1=1");
        if (!nama.isEmpty())  hql.append(" AND (LOWER(u.usernama) LIKE :nama OR LOWER(u.userid) LIKE :nama)");
        if (!email.isEmpty()) hql.append(" AND LOWER(u.email) LIKE :email");
        if (!role.isEmpty())  hql.append(" AND u.userRole.roleId=:role");
        if ("1".equals(aktif)) hql.append(" AND u.aktif=true");
        if ("0".equals(aktif)) hql.append(" AND u.aktif=false");
        hql.append(" ORDER BY u.usernama");

        Query q = db.createQuery(hql.toString());
        if (!nama.isEmpty())  q.setParameter("nama",  "%" + nama.toLowerCase() + "%");
        if (!email.isEmpty()) q.setParameter("email", "%" + email.toLowerCase() + "%");
        if (!role.isEmpty())  q.setParameter("role", role);
        q.setFirstResult((halaman - 1) * perHalaman);
        q.setMaxResults(perHalaman);

        List list = q.list();
        JSONArray arr = new JSONArray();
        for (Object o : list) {
            Tbmuser u = (Tbmuser) o;
            JSONObject row = new JSONObject();
            row.put("userid",   s(u.getUserId()));
            row.put("usernama", s(u.getUserNama()));
            row.put("email",    s(u.getEmail()));
            row.put("bahasa",   s(u.getBahasa()));
            row.put("aktif",    u.getAktif());
            row.put("usershow", u.getUserShow());
            if (u.getUserRole() != null) {
                row.put("roleId",   s(u.getUserRole().getRoleId()));
                row.put("roleName", s(u.getUserRole().getRoleName()));
            }
            arr.put(row);
        }
        res.put("total", arr.length());
        res.put("data", arr);

    /* ============================================================
     *  GET — pengguna_modul_role_list (daftar Role Tambahan SATU pengguna)
     *  gap-closure dokumen "Pengaturan Terpusat": satu Tbmuser bisa pegang LEBIH
     *  DARI SATU role sekaligus (mis. "Dosen" sekaligus "Kasir Kantin" sekaligus
     *  "Manager Keuangan") -- BUKAN slot yang terikat modul tertentu (POS/Keuangan/
     *  Inventory/Akademik/dll SEMUA bisa dipakai bebas di slot mana saja; modul apa
     *  yang didapat mengikuti flag pada Tbmrole yang dipilih, bukan nomor slotnya).
     *
     *  SENGAJA TIDAK memakai tabel baru -- Tbmuser SUDAH punya 5 slot role
     *  (userRole..userRole5, lihat ambilRoles()/ambilRolesId()), sebelumnya cuma
     *  dipakai longgar sbg "role tambahan" (gerbang memilikiHakAksesTambahan,
     *  dikumpulkan jadi satu Set utk cek keanggotaan). Slot 1 (userRole) TETAP
     *  "Grup/Role" utama (dipakai hakAkses()/menu ZK besar, tidak diubah artinya,
     *  tidak muncul di daftar ini). Slot 2-5 (userRole2..5) diekspos di sini sbg 4
     *  slot GENERIK "Role Tambahan #1..#4" -- admin bebas isi role apa pun ke slot
     *  mana pun, tidak ada slot yang "dikunci" utk modul tertentu.
     * ============================================================ */
    } else if ("pengguna_modul_role_list".equals(svc)) {
        String uid = s(request.getParameter("userid"));
        JSONArray arr = new JSONArray();
        Tbmuser u = uid.isEmpty() ? null : (Tbmuser) db.get(Tbmuser.class, uid);
        if (u != null) {
            Tbmrole[] slotRole = { u.getUserRole2(), u.getUserRole3(), u.getUserRole4(), u.getUserRole5() };
            for (int si = 0; si < slotRole.length; si++) {
                JSONObject row = new JSONObject();
                row.put("slot",     si + 2); // 2,3,4,5 -> userRole2..userRole5
                row.put("roleId",   slotRole[si] != null ? s(slotRole[si].getRoleId()) : "");
                row.put("roleName", slotRole[si] != null ? s(slotRole[si].getRoleName()) : "");
                arr.put(row);
            }
        }
        res.put("total", arr.length());
        res.put("data", arr);

    /* ============================================================
     *  GET — label_list (LabelBahasa)
     * ============================================================ */
    } else if ("label_list".equals(svc)) {
        String cari    = s(request.getParameter("cari"));
        String filter  = s(request.getParameter("filter"));
        int halaman    = i(request.getParameter("halaman"), 1);
        int perHalaman = i(request.getParameter("perHalaman"), 25);

        StringBuilder hql = new StringBuilder("FROM LabelBahasa lb WHERE 1=1");
        if (!cari.isEmpty()) hql.append(" AND (LOWER(lb.nama) LIKE :cari OR LOWER(lb.indonesia) LIKE :cari OR LOWER(lb.english) LIKE :cari)");
        if ("belum_en".equals(filter)) hql.append(" AND (lb.english IS NULL OR lb.english='')");
        if ("belum_ar".equals(filter)) hql.append(" AND (lb.arab IS NULL OR lb.arab='')");
        if ("belum_zh".equals(filter)) hql.append(" AND (lb.mandarin IS NULL OR lb.mandarin='')");
        hql.append(" ORDER BY lb.nama");

        Query q = db.createQuery(hql.toString());
        if (!cari.isEmpty()) q.setParameter("cari", "%" + cari.toLowerCase() + "%");

        /* Total */
        Query qTotal = db.createQuery("SELECT COUNT(*) " + hql.toString().replace("FROM LabelBahasa lb", "FROM LabelBahasa lb"));
        if (!cari.isEmpty()) qTotal.setParameter("cari", "%" + cari.toLowerCase() + "%");
        long total = (Long) qTotal.uniqueResult();

        q.setFirstResult((halaman - 1) * perHalaman);
        q.setMaxResults(perHalaman);
        List list = q.list();

        JSONArray arr = new JSONArray();
        for (Object o : list) {
            LabelBahasa lb = (LabelBahasa) o;
            JSONObject row = new JSONObject();
            row.put("id",          lb.getId());
            row.put("nama",        s(lb.getNama()));
            row.put("indonesia",   s(lb.getIndonesia()));
            row.put("english",     s(lb.getEnglish()));
            row.put("arab",        s(lb.getArab()));
            row.put("mandarin",    s(lb.getMandarin()));
            row.put("keterangan",  s(lb.getKeterangan()));
            arr.put(row);
        }
        res.put("total", total);
        res.put("data", arr);

    /* ============================================================
     *  POST — grup_tambah
     * ============================================================ */
    } else if ("grup_tambah".equals(svc) && "POST".equalsIgnoreCase(mth)) {
        String roleId   = s(request.getParameter("roleId"));
        String roleName = s(request.getParameter("roleName"));
        if (roleId.isEmpty() || roleName.isEmpty()) {
            res.put("ok", false); res.put("msg", "Kode dan Nama wajib diisi");
        } else {
            Tbmrole existing = (Tbmrole) db.get(Tbmrole.class, roleId);
            if (existing != null) {
                res.put("ok", false); res.put("msg", "Kode grup '"+roleId+"' sudah ada");
            } else {
                Tbmrole r = new Tbmrole();
                r.setRoleId(roleId);
                r.setRoleName(roleName);
                r.setKode(s(request.getParameter("kode")));
                r.setAktif(b(request.getParameter("aktif")));
                db.beginTransaction();
                db.save(r);
                db.getTransaction().commit();
                res.put("ok", true); res.put("msg", "Grup '"+roleName+"' berhasil ditambahkan");
            }
        }

    /* ============================================================
     *  POST — grup_ubah
     * ============================================================ */
    } else if ("grup_ubah".equals(svc) && "POST".equalsIgnoreCase(mth)) {
        String roleId   = s(request.getParameter("roleId"));
        String roleName = s(request.getParameter("roleName"));
        Tbmrole r = (Tbmrole) db.get(Tbmrole.class, roleId);
        if (r == null) {
            res.put("ok", false); res.put("msg", "Grup tidak ditemukan");
        } else {
            db.beginTransaction();
            r.setRoleName(roleName);
            r.setKode(s(request.getParameter("kode")));
            r.setAktif(b(request.getParameter("aktif")));
            db.update(r);
            db.getTransaction().commit();
            res.put("ok", true); res.put("msg", "Grup berhasil diperbarui");
        }

    /* ============================================================
     *  POST — grup_akses_simpan
     * ============================================================ */
    } else if ("grup_akses_simpan".equals(svc) && "POST".equalsIgnoreCase(mth)) {
        String roleId = s(request.getParameter("roleId"));
        Tbmrole r = (Tbmrole) db.get(Tbmrole.class, roleId);
        if (r == null) {
            res.put("ok", false); res.put("msg", "Grup tidak ditemukan");
        } else {
            db.beginTransaction();
            applyModulFlags(request, r);
            db.update(r);
            db.getTransaction().commit();
            res.put("ok", true); res.put("msg", "Hak akses modul berhasil disimpan");
        }

    /* ============================================================
     *  POST — pengguna_tambah
     * ============================================================ */
    } else if ("pengguna_tambah".equals(svc) && "POST".equalsIgnoreCase(mth)) {
        String userid   = s(request.getParameter("userid"));
        String usernama = s(request.getParameter("usernama"));
        String pw       = s(request.getParameter("userpassword"));
        String roleId   = s(request.getParameter("userRole"));
        if (userid.isEmpty() || usernama.isEmpty() || pw.isEmpty() || roleId.isEmpty()) {
            res.put("ok", false); res.put("msg", "Username, Nama, Password, dan Role wajib diisi");
        } else {
            Tbmuser existing = (Tbmuser) db.get(Tbmuser.class, userid);
            if (existing != null) {
                res.put("ok", false); res.put("msg", "Username '"+userid+"' sudah digunakan");
            } else {
                Tbmrole role = (Tbmrole) db.get(Tbmrole.class, roleId);
                if (role == null) { res.put("ok",false); res.put("msg","Role tidak ditemukan"); }
                else {
                    Tbmuser u = new Tbmuser();
                    u.setUserId(userid);
                    u.setUserNama(usernama);
                    u.setUserPassword(hashPassword(pw));
                    u.setUserRole(role);
                    u.setEmail(s(request.getParameter("email")));
                    u.setBahasa(s(request.getParameter("bahasa")));
                    u.setAktif(b(request.getParameter("aktif")));
                    u.setUserShow(b(request.getParameter("usershow")));
                    db.beginTransaction();
                    db.save(u);
                    db.getTransaction().commit();
                    res.put("ok", true); res.put("msg", "Pengguna '"+usernama+"' berhasil ditambahkan");
                }
            }
        }

    /* ============================================================
     *  POST — pengguna_ubah
     * ============================================================ */
    } else if ("pengguna_ubah".equals(svc) && "POST".equalsIgnoreCase(mth)) {
        String userid   = s(request.getParameter("userid"));
        String usernama = s(request.getParameter("usernama"));
        String roleId   = s(request.getParameter("userRole"));
        Tbmuser u = (Tbmuser) db.get(Tbmuser.class, userid);
        if (u == null) {
            res.put("ok", false); res.put("msg", "Pengguna tidak ditemukan");
        } else {
            Tbmrole role = (Tbmrole) db.get(Tbmrole.class, roleId);
            db.beginTransaction();
            u.setUserNama(usernama);
            if (role != null) u.setUserRole(role);
            u.setEmail(s(request.getParameter("email")));
            u.setBahasa(s(request.getParameter("bahasa")));
            u.setAktif(b(request.getParameter("aktif")));
            u.setUserShow(b(request.getParameter("usershow")));
            db.update(u);
            db.getTransaction().commit();
            res.put("ok", true); res.put("msg", "Data pengguna berhasil diperbarui");
        }

    /* ============================================================
     *  POST — pengguna_modul_role_simpan (simpan Role Tambahan SATU pengguna)
     *  Payload: userid, modulRoleJson = JSON array [{slot,roleId}, ...] -- slot
     *  bernilai 2/3/4/5 (-> userRole2..userRole5), roleId kosong berarti KOSONGKAN
     *  slot itu (kirim SEMUA baris tiap simpan, bukan delta -- pola sama
     *  grup_akses_simpan). Slot BEBAS diisi role apa pun (POS/Keuangan/Akademik/
     *  Kepegawaian/dll) -- lihat JavaDoc pengguna_modul_role_list.
     * ============================================================ */
    } else if ("pengguna_modul_role_simpan".equals(svc) && "POST".equalsIgnoreCase(mth)) {
        String uid = s(request.getParameter("userid"));
        Tbmuser u = uid.isEmpty() ? null : (Tbmuser) db.get(Tbmuser.class, uid);
        if (u == null) {
            res.put("ok", false); res.put("msg", "Pengguna tidak ditemukan");
        } else {
            try {
                JSONArray daftar = new JSONArray(s(request.getParameter("modulRoleJson")));
                db.beginTransaction();
                for (int mi = 0; mi < daftar.length(); mi++) {
                    JSONObject baris = daftar.getJSONObject(mi);
                    int slot = i(baris.opt("slot"), 0);
                    String roleId = s(baris.opt("roleId"));
                    Tbmrole role = roleId.isEmpty() ? null : (Tbmrole) db.get(Tbmrole.class, roleId);

                    if (slot == 2) u.setUserRole2(role);
                    else if (slot == 3) u.setUserRole3(role);
                    else if (slot == 4) u.setUserRole4(role);
                    else if (slot == 5) u.setUserRole5(role);
                }
                // Slot tambahan otomatis dianggap "punya hak akses tambahan" bila salah satu terisi --
                // gerbang yg SUDAH ADA (memilikiHakAksesTambahan) supaya ambilRoles()/ambilRolesId()
                // (dipakai luas di luar fitur ini) tetap ikut memperhitungkan slot 2-5 yg baru diisi.
                boolean adaSlotTambahan = u.getUserRole2() != null || u.getUserRole3() != null || u.getUserRole4() != null || u.getUserRole5() != null;
                u.setMemilikiHakAksesTambahan(adaSlotTambahan);
                db.update(u);
                db.getTransaction().commit();
                res.put("ok", true); res.put("msg", "Role Tambahan berhasil disimpan");
            } catch (Exception eModulRole) {
                if (db.getTransaction() != null && db.getTransaction().isActive()) db.getTransaction().rollback();
                res.put("ok", false); res.put("msg", "Gagal menyimpan: " + eModulRole.getMessage());
            }
        }

    /* ============================================================
     *  POST — pengguna_ubah_status
     * ============================================================ */
    } else if ("pengguna_ubah_status".equals(svc) && "POST".equalsIgnoreCase(mth)) {
        String userid = s(request.getParameter("userid"));
        boolean aktif = b(request.getParameter("aktif"));
        Tbmuser u = (Tbmuser) db.get(Tbmuser.class, userid);
        if (u == null) {
            res.put("ok", false); res.put("msg", "Pengguna tidak ditemukan");
        } else {
            db.beginTransaction();
            u.setAktif(aktif);
            db.update(u);
            db.getTransaction().commit();
            res.put("ok", true); res.put("msg", aktif ? "Pengguna diaktifkan" : "Pengguna dinonaktifkan");
        }

    /* ============================================================
     *  POST — pengguna_reset_pwd (admin)
     * ============================================================ */
    } else if ("pengguna_reset_pwd".equals(svc) && "POST".equalsIgnoreCase(mth)) {
        String userid = s(request.getParameter("userid"));
        String pwBaru = s(request.getParameter("passwordBaru"));
        if (userid.isEmpty() || pwBaru.isEmpty()) {
            res.put("ok", false); res.put("msg", "userid dan password wajib diisi");
        } else if (pwBaru.length() < 6) {
            res.put("ok", false); res.put("msg", "Password minimal 6 karakter");
        } else {
            Tbmuser u = (Tbmuser) db.get(Tbmuser.class, userid);
            if (u == null) {
                res.put("ok", false); res.put("msg", "Pengguna tidak ditemukan");
            } else {
                db.beginTransaction();
                u.setUserPassword(hashPassword(pwBaru));
                db.update(u);
                db.getTransaction().commit();
                res.put("ok", true); res.put("msg", "Password berhasil direset");
            }
        }

    /* ============================================================
     *  POST — ubah_password (self)
     * ============================================================ */
    } else if ("ubah_password".equals(svc) && "POST".equalsIgnoreCase(mth)) {
        String pwLama = s(request.getParameter("passwordLama"));
        String pwBaru = s(request.getParameter("passwordBaru"));
        javax.servlet.http.HttpSession sess = request.getSession(false);
        Tbmuser loginUser = null;
        if (sess != null) {
            Object _lo = sess.getAttribute("login");
            if (_lo instanceof Tbmuser) loginUser = (Tbmuser) _lo;
            else if (sess.getAttribute("usersTemp") instanceof Tbmuser) loginUser = (Tbmuser) sess.getAttribute("usersTemp");
        }
        if (loginUser == null) {
            res.put("ok", false); res.put("msg", "Sesi tidak valid, silakan login ulang");
        } else if (pwLama.isEmpty() || pwBaru.isEmpty()) {
            res.put("ok", false); res.put("msg", "Password lama dan baru wajib diisi");
        } else if (pwBaru.length() < 8) {
            res.put("ok", false); res.put("msg", "Password baru minimal 8 karakter");
        } else {
            Tbmuser u = (Tbmuser) db.get(Tbmuser.class, loginUser.getUserId());
            String hashedLama = hashPassword(pwLama);
            if (u == null || !hashedLama.equals(s(u.getUserPassword()))) {
                res.put("ok", false); res.put("msg", "Password lama tidak sesuai");
            } else {
                db.beginTransaction();
                u.setUserPassword(hashPassword(pwBaru));
                db.update(u);
                db.getTransaction().commit();
                res.put("ok", true); res.put("msg", "Password berhasil diubah");
            }
        }

    /* ============================================================
     *  POST — label_tambah
     * ============================================================ */
    } else if ("label_tambah".equals(svc) && "POST".equalsIgnoreCase(mth)) {
        String nama      = s(request.getParameter("nama"));
        String indonesia = s(request.getParameter("indonesia"));
        if (nama.isEmpty() || indonesia.isEmpty()) {
            res.put("ok", false); res.put("msg", "Kunci dan teks Indonesia wajib diisi");
        } else {
            List existing = db.createQuery("FROM LabelBahasa l WHERE l.nama=:n").setParameter("n", nama).list();
            if (!existing.isEmpty()) {
                res.put("ok", false); res.put("msg", "Kunci '"+nama+"' sudah ada");
            } else {
                LabelBahasa lb = new LabelBahasa();
                lb.setNama(nama);
                lb.setIndonesia(indonesia);
                lb.setEnglish(s(request.getParameter("english")));
                lb.setArab(s(request.getParameter("arab")));
                lb.setMandarin(s(request.getParameter("mandarin")));
                lb.setKeterangan(s(request.getParameter("keterangan")));
                db.beginTransaction();
                db.save(lb);
                db.getTransaction().commit();
                res.put("ok", true); res.put("msg", "Label '"+nama+"' berhasil ditambahkan");
            }
        }

    /* ============================================================
     *  POST — label_ubah
     * ============================================================ */
    } else if ("label_ubah".equals(svc) && "POST".equalsIgnoreCase(mth)) {
        long id = l(request.getParameter("id"), -1L);
        LabelBahasa lb = id > 0 ? (LabelBahasa) db.get(LabelBahasa.class, id) : null;
        if (lb == null) {
            res.put("ok", false); res.put("msg", "Label tidak ditemukan");
        } else {
            db.beginTransaction();
            lb.setNama(s(request.getParameter("nama")));
            lb.setIndonesia(s(request.getParameter("indonesia")));
            lb.setEnglish(s(request.getParameter("english")));
            lb.setArab(s(request.getParameter("arab")));
            lb.setMandarin(s(request.getParameter("mandarin")));
            lb.setKeterangan(s(request.getParameter("keterangan")));
            db.update(lb);
            db.getTransaction().commit();
            res.put("ok", true); res.put("msg", "Label berhasil diperbarui");
        }

    /* ============================================================
     *  POST — label_hapus
     * ============================================================ */
    } else if ("label_hapus".equals(svc) && "POST".equalsIgnoreCase(mth)) {
        long id = l(request.getParameter("id"), -1L);
        LabelBahasa lb = id > 0 ? (LabelBahasa) db.get(LabelBahasa.class, id) : null;
        if (lb == null) {
            res.put("ok", false); res.put("msg", "Label tidak ditemukan");
        } else {
            db.beginTransaction();
            db.delete(lb);
            db.getTransaction().commit();
            res.put("ok", true); res.put("msg", "Label dihapus");
        }

    /* ============================================================
     *  GET — toko_list (Toko / Perusahaan, filter: nama, aktif)
     * ============================================================ */
    } else if ("toko_list".equals(svc)) {
        String nama   = s(request.getParameter("nama"));
        String aktif  = s(request.getParameter("aktif"));
        int halaman   = i(request.getParameter("halaman"), 1);
        int perHalaman= i(request.getParameter("perHalaman"), i(request.getParameter("limit"), 20));

        StringBuilder hql = new StringBuilder("FROM ais.database.model.inventory.Toko t WHERE 1=1");
        if (!nama.isEmpty())     hql.append(" AND (LOWER(t.nama) LIKE :nama OR LOWER(t.kode) LIKE :nama)");
        if ("1".equals(aktif))   hql.append(" AND t.aktif=true");
        if ("0".equals(aktif))   hql.append(" AND t.aktif=false");
        hql.append(" ORDER BY t.nama");

        Query q = db.createQuery(hql.toString());
        if (!nama.isEmpty()) q.setParameter("nama", "%" + nama.toLowerCase() + "%");
        q.setFirstResult((halaman - 1) * perHalaman);
        q.setMaxResults(perHalaman);

        List list = q.list();
        JSONArray arr = new JSONArray();
        for (Object o : list) {
            ais.database.model.inventory.Toko t = (ais.database.model.inventory.Toko) o;
            JSONObject row = new JSONObject();
            row.put("id",        t.getId());
            row.put("kode",      s(t.getKode()));
            row.put("nama",      s(t.getNama()));
            row.put("kota",      s(t.getKota()));
            row.put("telp",      s(t.getTelp()));
            row.put("aktif",     t.getAktif());
            arr.put(row);
        }
        res.put("total", arr.length());
        res.put("data", arr);

    /* ============================================================
     *  GET — toko_list_all (semua toko aktif, untuk dropdown/referensi)
     * ============================================================ */
    } else if ("toko_list_all".equals(svc)) {
        List list = db.createQuery("FROM ais.database.model.inventory.Toko t WHERE t.aktif=true ORDER BY t.nama")
                      .setMaxResults(1000).list();
        JSONArray arr = new JSONArray();
        for (Object o : list) {
            ais.database.model.inventory.Toko t = (ais.database.model.inventory.Toko) o;
            JSONObject row = new JSONObject();
            row.put("id",   t.getId());
            row.put("kode", s(t.getKode()));
            row.put("nama", s(t.getNama()));
            arr.put(row);
        }
        res.put("total", arr.length());
        res.put("data", arr);

    /* ============================================================
     *  GET — toko_detail (satu Toko lengkap, param: id)
     * ============================================================ */
    } else if ("toko_detail".equals(svc)) {
        long id = l(request.getParameter("id"), -1L);
        ais.database.model.inventory.Toko t = id > 0
            ? (ais.database.model.inventory.Toko) db.get(ais.database.model.inventory.Toko.class, Long.valueOf(id)) : null;
        if (t == null) {
            res.put("total", 0); res.put("data", new JSONObject());
        } else {
            JSONObject row = new JSONObject();
            row.put("id",             t.getId());
            row.put("kode",           s(t.getKode()));
            row.put("nama",           s(t.getNama()));
            row.put("keterangan",     s(t.getKeterangan()));
            row.put("aktif",          t.getAktif());
            row.put("alamat",         s(t.getAlamat()));
            row.put("kota",           s(t.getKota()));
            row.put("kodePos",        s(t.getKodePos()));
            row.put("telp",           s(t.getTelp()));
            row.put("email",          s(t.getEmail()));
            row.put("picNama",        s(t.getPicNama()));
            row.put("picHp",          s(t.getPicHp()));
            row.put("npwp",           s(t.getNpwp()));
            row.put("jamOperasional", s(t.getJamOperasional()));
            res.put("total", 1); res.put("data", row);
        }

    /* ============================================================
     *  POST — toko_simpan (tambah bila id kosong, ubah bila id ada)
     * ============================================================ */
    } else if ("toko_simpan".equals(svc) && "POST".equalsIgnoreCase(mth)) {
        long id = l(request.getParameter("id"), -1L);
        String nama = s(request.getParameter("nama"));
        if (nama.isEmpty()) {
            res.put("ok", false); res.put("msg", "Nama toko/perusahaan wajib diisi");
        } else {
            ais.database.model.inventory.Toko t = id > 0
                ? (ais.database.model.inventory.Toko) db.get(ais.database.model.inventory.Toko.class, Long.valueOf(id))
                : new ais.database.model.inventory.Toko();
            if (id > 0 && t == null) {
                res.put("ok", false); res.put("msg", "Toko tidak ditemukan");
            } else {
                t.setKode(s(request.getParameter("kode")));
                t.setNama(nama);
                t.setKeterangan(s(request.getParameter("keterangan")));
                t.setAktif(b(request.getParameter("aktif")));
                t.setAlamat(s(request.getParameter("alamat")));
                t.setKota(s(request.getParameter("kota")));
                t.setKodePos(s(request.getParameter("kodePos")));
                t.setTelp(s(request.getParameter("telp")));
                t.setEmail(s(request.getParameter("email")));
                t.setPicNama(s(request.getParameter("picNama")));
                t.setPicHp(s(request.getParameter("picHp")));
                t.setNpwp(s(request.getParameter("npwp")));
                t.setJamOperasional(s(request.getParameter("jamOperasional")));
                db.beginTransaction();
                if (id > 0) db.update(t); else db.save(t);
                db.getTransaction().commit();
                res.put("ok", true); res.put("msg", "Toko/Perusahaan '"+nama+"' berhasil disimpan");
            }
        }

    /* ============================================================
     *  Fallback
     * ============================================================ */
    } else if ("POST".equalsIgnoreCase(mth)) {
        res.put("ok", false);
        res.put("msg", "Layanan '"+svc+"' belum diimplementasikan");
    } else {
        res.put("total", 0);
        res.put("data", new JSONArray());
        res.put("_stub", true);
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
    if (db != null && db.isOpen()) try { db.close(); } catch (Exception e2) {}
}

out.clear();
out.print(res.toString());
%>
