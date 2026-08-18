<%@page import="java.io.File"%>
<%@page import="java.io.InputStream"%>
<%@page import="java.io.FileOutputStream"%>
<%@page import="java.lang.reflect.Method"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.net.URLDecoder"%>
<%@page import="org.apache.commons.fileupload.FileItem"%>
<%@page import="org.apache.commons.fileupload.disk.DiskFileItemFactory"%>
<%@page import="org.apache.commons.fileupload.servlet.ServletFileUpload"%>
<%@page import="javax.servlet.http.Part"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.criterion.Criterion"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Junction"%>
<%@page import="org.apache.commons.lang.RandomStringUtils"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.KarirConfigUtil"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Agama"%>
<%@page import="ais.database.model.PengumumanAkademis"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.database.model.recruitment.CalonPegawai"%>
<%@page import="ais.database.model.recruitment.CalonPegawaiPunyaDokumen"%>
<%@page import="ais.database.model.recruitment.GelombangPendaftaranPegawai"%>
<%@page import="ais.database.model.recruitment.KelompokPendaftaranPegawai"%>
<%@page import="ais.database.model.recruitment.JadwalUjianPegawai"%>
<%@page import="ais.database.model.recruitment.RuangGelombangPendaftaranPegawaiPegawai"%>
<%@page import="ais.database.model.recruitment.RuangPegawai"%>
<%@page import="ais.database.model.recruitment.UjianPegawai"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.database.model.recruitment.VerifikasiKelengkapanCalonPegawai"%>
<%@page import="ais.delivery.email.sender.MailSender"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
    private static final String[] DOC_NAMES = new String[] {
        "Kartu Identitas",
        "Surat Keterangan Pendidikan",
        "Surat Keterangan Pengalaman Kerja",
        "Surat Lamaran",
        "Curriculum Vitae",
        "Pas Photo Terbaru"
    };
    private static final String[] DOC_FIELDS = new String[] {
        "dokumen_kartu_identitas",
        "dokumen_pendidikan",
        "dokumen_pengalaman",
        "dokumen_lamaran",
        "dokumen_cv",
        "dokumen_photo"
    };
    private static final boolean[] DOC_REQUIRED = new boolean[] { true, true, false, true, true, true };

    private String nvl(String s) { return s == null ? "" : s.trim(); }
    private boolean empty(String s) { return s == null || s.trim().length() == 0; }
    private String cfgText(String key, String def) { return KarirConfigUtil.text(key, def); }

    private Object safeInvoke(Object obj, String method) {
        try { if (obj == null) return null; Method m = obj.getClass().getMethod(method, new Class[]{}); return m.invoke(obj, new Object[]{}); } catch(Exception e) { return null; }
    }
    private void safeSet(Object obj, String method, Class type, Object value) {
        try { if (obj == null) return; Method m = obj.getClass().getMethod(method, new Class[]{type}); m.invoke(obj, new Object[]{value}); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:80");}
    }
    private void safeSetAny(Object obj, String method, Object value) {
        try {
            if (obj == null) return;
            Method[] ms = obj.getClass().getMethods();
            for (int i = 0; i < ms.length; i++) {
                Method m = ms[i];
                if (!method.equals(m.getName()) || m.getParameterTypes() == null || m.getParameterTypes().length != 1) continue;
                Class t = m.getParameterTypes()[0];
                Object v = value;
                if (value != null && (Long.TYPE.equals(t) || Long.class.equals(t))) v = Long.valueOf(String.valueOf(value));
                else if (value != null && (Integer.TYPE.equals(t) || Integer.class.equals(t))) v = Integer.valueOf(String.valueOf(value));
                else if (value != null && String.class.equals(t)) v = String.valueOf(value);
                else if (value != null && !t.isAssignableFrom(value.getClass())) continue;
                m.invoke(obj, new Object[]{v});
                return;
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:98");}
    }
    private String safeString(Object obj, String method) {
        Object v = safeInvoke(obj, method);
        return v == null ? "" : String.valueOf(v).trim();
    }
    private boolean safeBool(Object obj, String method, boolean def) {
        Object v = safeInvoke(obj, method);
        if (v == null) return def;
        return Boolean.valueOf(String.valueOf(v)).booleanValue();
    }
    private Date parseDate(String s) throws Exception {
        if (s == null || s.trim().length() == 0) return null;
        return new SimpleDateFormat("yyyy-MM-dd").parse(s.trim());
    }
    private String formatDate(Date d) {
        try { return d == null ? "" : new SimpleDateFormat("dd MMM yyyy").format(d); } catch(Exception e) { return ""; }
    }
    private String formatDateTime(Date d) {
        try { return d == null ? "" : new SimpleDateFormat("dd MMM yyyy HH:mm").format(d); } catch(Exception e) { return ""; }
    }
    private String isoDate(Date d) {
        try { return d == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(d); } catch(Exception e) { return ""; }
    }
    private Date startOfDay(Date d) {
        try {
            Calendar c = Calendar.getInstance();
            c.setTime(d == null ? new Date() : d);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            return c.getTime();
        } catch(Exception e) { return d == null ? new Date() : d; }
    }
    private boolean pendaftaranBelumDibuka(Date mulai) {
        return mulai != null && startOfDay(new Date()).before(startOfDay(mulai));
    }
    private boolean pendaftaranSudahTerlewat(Date sampai) {
        return sampai != null && startOfDay(new Date()).after(startOfDay(sampai));
    }
    private boolean lowonganDapatDaftar(GelombangPendaftaranPegawai g) {
        if (g == null) return false;
        Date mulai = firstDate(g, new String[] { "getTanggalMulai", "getMulai", "getTanggalAwal", "getWaktuMulai" });
        Date sampai = firstDate(g, new String[] { "getTanggalSelesai", "getSampai", "getSelesai", "getTanggalAkhir", "getWaktuSelesai" });
        return g.getAktif() && !pendaftaranBelumDibuka(mulai) && !pendaftaranSudahTerlewat(sampai);
    }
    private String statusPendaftaranLowongan(GelombangPendaftaranPegawai g) {
        if (g == null) return "Lowongan tidak ditemukan";
        Date mulai = firstDate(g, new String[] { "getTanggalMulai", "getMulai", "getTanggalAwal", "getWaktuMulai" });
        Date sampai = firstDate(g, new String[] { "getTanggalSelesai", "getSampai", "getSelesai", "getTanggalAkhir", "getWaktuSelesai" });
        if (!g.getAktif()) return "Tidak Aktif";
        if (pendaftaranBelumDibuka(mulai)) return "Pendaftaran Belum Dibuka";
        if (pendaftaranSudahTerlewat(sampai)) return "Pendaftaran Sudah Terlewat";
        return "Pendaftaran Dibuka";
    }
    private String htmlEscape(String s) {
        s = nvl(s);
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    private String cleanFilename(String s) {
        if (s == null || s.trim().length() == 0) return "file";
        return s.replaceAll("[^A-Za-z0-9._-]", "_");
    }
    private String getSubmittedFileName(Part part) {
        if (part == null) return "";
        String cd = part.getHeader("content-disposition");
        if (cd == null) return "file";
        String[] items = cd.split(";");
        for (int i = 0; i < items.length; i++) {
            String item = items[i].trim();
            if (item.startsWith("filename")) {
                String name = item.substring(item.indexOf('=') + 1).trim().replace("\"", "");
                int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
                return slash >= 0 ? name.substring(slash + 1) : name;
            }
        }
        return "file";
    }
    private String normalizeUsername(String s) {
        s = nvl(s).toLowerCase();
        if (s.indexOf(',') >= 0) s = s.split(",")[0].trim();
        s = s.replaceAll("[^a-z0-9@._-]", "");
        if (s.length() < 3) s = "karir" + RandomStringUtils.randomNumeric(4);
        return s;
    }
    private String buildUniqueUsername(Session db, CalonPegawai calon) {
        String base = normalizeUsername(calon == null ? "" : calon.getAlamatEmail());
        if (base.length() < 3 || base.indexOf("@") < 0) {
            String nama = calon == null ? "karir" : nvl(calon.getNama());
            String[] parts = StringUtils.split(nama, " ");
            base = normalizeUsername((parts != null && parts.length > 0 ? parts[0] : "karir") + RandomStringUtils.randomNumeric(3));
        }
        String username = base;
        int loop = 0;
        while (true) {
            try {
                Object existing = db.createCriteria(Tbmuser.class).add(Restrictions.eq("userId", username)).setMaxResults(1).uniqueResult();
                if (existing == null) return username;
            } catch(Exception e) { return username; }
            loop++;
            username = base + loop;
            if (loop > 50) return base + RandomStringUtils.randomNumeric(5);
        }
    }
    private Tbmrole ensureRoleCalonPegawai(Session db) throws Exception {
        /*
         * Penting:
         * Jangan hanya memakai ConstantValues.tbmroleCalonPegawai secara langsung.
         * Pada beberapa request JSP, object static tersebut bisa null atau detached
         * dari Session aktif. Akibatnya field userrole pada tbmuser dapat tersimpan
         * null ketika user baru dibuat.
         */
        Tbmrole role = null;
        try {
            role = (Tbmrole) db.createCriteria(Tbmrole.class)
                    .add(Restrictions.eq("roleId", Tbmrole.CALON_PEGAWAI))
                    .setMaxResults(1).uniqueResult();
        } catch(Exception e) {
            role = null;
        }
        if (role == null) {
            role = new Tbmrole();
            role.setRoleId(Tbmrole.CALON_PEGAWAI);
            role.setNama("Calon Pegawai / Karyawan");
            db.save(role);
            db.flush();
        }
        ConstantValues.tbmroleCalonPegawai = role;
        return role;
    }
    private void saveOrUpdateCandidateAccess(Tbmuser tbmuser, String passwordPlain) {
        try {
            if (tbmuser != null && tbmuser.getUserId() != null && passwordPlain != null) {
                Common.saveOrUpdateUserAccess(tbmuser, null, tbmuser.getUserId(), passwordPlain.trim(), tbmuser.getEmail());
            }
        } catch(Exception e) {
            try { Common.tampilErrorJikaAdmin(e); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:235");}
        }
    }
    private Tbmuser createOrResetCandidateUser(Session db, CalonPegawai calon, String passwordPlain) throws Exception {
        Tbmuser tbmuser = (Tbmuser) db.createCriteria(Tbmuser.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.eq("calonPegawai", calon))
                .setMaxResults(1).uniqueResult();
        if (tbmuser == null && calon.getAlamatEmail() != null && calon.getAlamatEmail().trim().length() > 0) {
            tbmuser = (Tbmuser) db.createCriteria(Tbmuser.class)
                    .add(Restrictions.or(Restrictions.eq("userId", calon.getAlamatEmail().trim()), Restrictions.eq("email", calon.getAlamatEmail().trim())))
                    .setMaxResults(1).uniqueResult();
        }

        boolean userBaru = false;
        if (tbmuser == null || tbmuser.getUserId() == null || tbmuser.getUserId().trim().length() == 0) {
            tbmuser = new Tbmuser();
            tbmuser.setUserId(buildUniqueUsername(db, calon));
            userBaru = true;
        }

        /*
         * Perbaikan utama error:
         * ERROR: null value in column "userrole" of relation "tbmuser" violates not-null constraint
         *
         * Pada versi sebelumnya object Tbmuser baru sudah dipanggil db.save(tbmuser)
         * sebelum userRole diisi. Pada Hibernate dengan assigned id/dynamic insert,
         * state insert bisa ikut membawa userrole null. Karena kolom userrole NOT NULL,
         * proses insert gagal. Sekarang semua field wajib, terutama userRole dan
         * userPassword, diisi lebih dahulu sebelum save/saveOrUpdate dipanggil.
         */
        Tbmrole roleCalonPegawai = ensureRoleCalonPegawai(db);
        if (roleCalonPegawai == null || roleCalonPegawai.getRoleId() == null) {
            throw new Exception("Role Calon Pegawai / Karyawan tidak berhasil dibuat atau ditemukan.");
        }

        tbmuser.setEmail(nvl(calon.getAlamatEmail()));
        tbmuser.setUserNama(nvl(calon.getNama()));
        tbmuser.setIs_encripted(true);
        tbmuser.setRoot(false);
        tbmuser.setAktif(true);
        tbmuser.setUserPassword(Common.desEncrypter.get().encrypt(passwordPlain.trim()));
        tbmuser.setUserRole(roleCalonPegawai);
        tbmuser.setUserShow(1);
        tbmuser.setCalonPegawai(calon);
        try { tbmuser.setHp(nvl(calon.getTeleponPegawai())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:280");}

        if (userBaru) {
            db.save(tbmuser);
        } else {
            try { Common.refreshSaveOrUpdate(db, tbmuser); } catch(Exception e) { db.saveOrUpdate(tbmuser); }
        }
        db.flush();
        return tbmuser;
    }
    private VerifikasiKelengkapanCalonPegawai findOrCreateTemplate(Session db, String nama, boolean wajib) throws Exception {
        VerifikasiKelengkapanCalonPegawai t = null;
        try {
            t = (VerifikasiKelengkapanCalonPegawai) db.createCriteria(VerifikasiKelengkapanCalonPegawai.class)
                    .add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
        } catch(Exception e) { t = null; }
        if (t == null) {
            try {
                t = (VerifikasiKelengkapanCalonPegawai) db.createCriteria(VerifikasiKelengkapanCalonPegawai.class)
                        .add(Restrictions.ilike("nama", nama, MatchMode.ANYWHERE)).setMaxResults(1).uniqueResult();
            } catch(Exception e) { t = null; }
        }
        if (t == null) {
            t = new VerifikasiKelengkapanCalonPegawai();
            safeSet(t, "setNomorInduk", String.class, nama.toUpperCase().replaceAll("[^A-Z0-9]", "_"));
            safeSet(t, "setKode", String.class, nama.toUpperCase().replaceAll("[^A-Z0-9]", "_"));
            safeSet(t, "setNama", String.class, nama);
            safeSet(t, "setKeterangan", String.class, "Dokumen persyaratan Portal KARIR: " + nama);
            safeSet(t, "setWajib", Boolean.class, Boolean.valueOf(wajib));
            safeSet(t, "setAktif", Boolean.class, Boolean.TRUE);
            safeSet(t, "setStatus", Boolean.class, Boolean.TRUE);
            db.save(t);
            db.flush();
        }
        return t;
    }
    private VerifikasiKelengkapanCalonPegawai managedTemplate(Session db, VerifikasiKelengkapanCalonPegawai t) {
        try {
            if (t != null && t.getId() != null) {
                VerifikasiKelengkapanCalonPegawai managed = (VerifikasiKelengkapanCalonPegawai) db.get(VerifikasiKelengkapanCalonPegawai.class, t.getId());
                if (managed != null) return managed;
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:322");}
        return t;
    }
    private boolean isTemplateAktif(VerifikasiKelengkapanCalonPegawai t) {
        /*
         * VerifikasiKelengkapanCalonPegawai.getAktif() menganggap null sebagai aktif.
         * Portal dan modul admin hanya boleh menampilkan dokumen yang tidak dinonaktifkan.
         */
        try { return t != null && t.getAktif(); } catch(Exception e) { return false; }
    }
    private CalonPegawaiPunyaDokumen ensureDoc(Session db, CalonPegawai calon, VerifikasiKelengkapanCalonPegawai t) throws Exception {
        if (calon == null || t == null) return null;
        t = managedTemplate(db, t);
        if (!isTemplateAktif(t)) return null;
        CalonPegawaiPunyaDokumen d = (CalonPegawaiPunyaDokumen) db.createCriteria(CalonPegawaiPunyaDokumen.class)
                .add(Restrictions.eq("calonPegawai", calon))
                .add(Restrictions.eq("verifikasiKelengkapanCalonPegawai", t))
                .addOrder(Order.desc("id"))
                .setMaxResults(1).uniqueResult();
        if (d == null) {
            d = new CalonPegawaiPunyaDokumen();
            d.setCalonPegawai(calon);
            d.setVerifikasiKelengkapanCalonPegawai(t);
            d.setStatus(CalonPegawaiPunyaDokumen.BELUM);
            d.setKeterangan("");
            try { Common.refreshSaveOrUpdate(db, d); } catch(Exception e) { db.saveOrUpdate(d); }
            db.flush();
        }
        return d;
    }
    private void addTemplateIfAbsent(Session db, List target, Set ids, VerifikasiKelengkapanCalonPegawai t) {
        try {
            if (t == null) return;
            t = managedTemplate(db, t);
            if (t == null || t.getId() == null) return;
            if (!isTemplateAktif(t)) return;
            if (!ids.contains(t.getId())) { ids.add(t.getId()); target.add(t); }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:359");}
    }
    private void refreshVerifikasiCacheIfNeeded() {
        try {
            /*
             * CalonPegawaiAction.initDokumen() mengambil daftar baris dokumen dari
             * ConstantValues.ambilBerdasarClass(VerifikasiKelengkapanCalonPegawai.class),
             * bukan langsung dari query database. Jika Portal KARIR membuat template
             * dokumen baru, cache lama bisa belum mengenal template tersebut sehingga
             * dokumen tidak tampil di modul Pendataan Calon Pegawai sampai aplikasi
             * direstart. Karena itu cache di-refresh secara aman setelah template
             * dokumen dibuat/disiapkan.
             */
            ConstantValues.hasbeeninit = false;
            ConstantValues.init();
        } catch (Throwable e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:374");}
    }
    private void ensureDocumentsForCalon(Session db, CalonPegawai calon, GelombangPendaftaranPegawai lowongan) throws Exception {
        List templatesFinal = new ArrayList();
        Set ids = new HashSet();

        /* 1) Dokumen spesifik yang ditempelkan ke GelombangPendaftaranPegawai. */
        try {
            if (lowongan != null && lowongan.getId() != null) lowongan = (GelombangPendaftaranPegawai) db.get(GelombangPendaftaranPegawai.class, lowongan.getId());
            if (lowongan != null && lowongan.getVerifikasiKelengkapanCalonPegawais() != null) {
                Iterator it = lowongan.getVerifikasiKelengkapanCalonPegawais().iterator();
                while (it.hasNext()) addTemplateIfAbsent(db, templatesFinal, ids, (VerifikasiKelengkapanCalonPegawai) it.next());
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:387");}

        /* 2) Template yang dipakai modul admin lama melalui ConstantValues cache. */
        try {
            Map map = ConstantValues.ambilBerdasarClass(VerifikasiKelengkapanCalonPegawai.class);
            if (map != null) {
                Iterator it = map.values().iterator();
                while (it.hasNext()) addTemplateIfAbsent(db, templatesFinal, ids, (VerifikasiKelengkapanCalonPegawai) it.next());
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:396");}

        /* 3) Semua template aktif yang memang sudah ada di database. */
        try {
            List templates = db.createCriteria(VerifikasiKelengkapanCalonPegawai.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .addOrder(Order.asc("id")).list();
            for (int i = 0; i < templates.size(); i++) addTemplateIfAbsent(db, templatesFinal, ids, (VerifikasiKelengkapanCalonPegawai) templates.get(i));
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:404");}

        /* 4) Fallback enam dokumen standar Portal KARIR. */
        boolean adaTemplateBaru = false;
        for (int i = 0; i < DOC_NAMES.length; i++) {
            VerifikasiKelengkapanCalonPegawai before = null;
            try { before = (VerifikasiKelengkapanCalonPegawai) db.createCriteria(VerifikasiKelengkapanCalonPegawai.class).add(Restrictions.ilike("nama", DOC_NAMES[i], MatchMode.EXACT)).setMaxResults(1).uniqueResult(); } catch(Exception e) { before = null; }
            VerifikasiKelengkapanCalonPegawai t = findOrCreateTemplate(db, DOC_NAMES[i], DOC_REQUIRED[i]);
            if (before == null && t != null && t.getId() != null) adaTemplateBaru = true;
            addTemplateIfAbsent(db, templatesFinal, ids, t);
        }

        for (int i = 0; i < templatesFinal.size(); i++) ensureDoc(db, calon, (VerifikasiKelengkapanCalonPegawai) templatesFinal.get(i));
        cleanupDuplicateDocumentsForCalon(db, calon);
        if (adaTemplateBaru) refreshVerifikasiCacheIfNeeded();
    }
    private void cleanupDuplicateDocumentsForCalon(Session db, CalonPegawai calon) {
        try {
            if (db == null || calon == null || calon.getId() == null) return;
            boolean bolehTulis = false;
            try { bolehTulis = db.getTransaction() != null && db.getTransaction().isActive(); } catch(Exception e) { bolehTulis = false; }
            if (!bolehTulis) return;
            List docs = db.createCriteria(CalonPegawaiPunyaDokumen.class)
                    .createAlias("verifikasiKelengkapanCalonPegawai", "dok", Criteria.LEFT_JOIN)
                    .add(Restrictions.eq("calonPegawai", calon))
                    .addOrder(Order.asc("id")).list();
            Map keeperByKey = new HashMap();
            for (int i = 0; i < docs.size(); i++) {
                CalonPegawaiPunyaDokumen d = (CalonPegawaiPunyaDokumen) docs.get(i);
                VerifikasiKelengkapanCalonPegawai v = d.getVerifikasiKelengkapanCalonPegawai();
                String key = v != null && v.getId() != null ? "ID:" + v.getId() : "NM:" + safeString(v, "getNama").toLowerCase();
                CalonPegawaiPunyaDokumen keep = (CalonPegawaiPunyaDokumen) keeperByKey.get(key);
                if (keep == null) {
                    keeperByKey.put(key, d);
                    continue;
                }
                String linkKeep = linkLampiran(db, keep);
                String linkDup = linkLampiran(db, d);
                if (empty(linkKeep) && !empty(linkDup)) {
                    keep.setKeterangan(d.getKeterangan());
                    keep.setStatus(d.getStatus());
                    List lams = db.createCriteria(LampiranLain.class)
                            .add(Restrictions.eq("ref", d.getId()))
                            .add(Restrictions.eq("jenis", CalonPegawaiPunyaDokumen.class.getName()))
                            .list();
                    for (int j = 0; j < lams.size(); j++) {
                        LampiranLain lam = (LampiranLain) lams.get(j);
                        safeSetAny(lam, "setRef", keep.getId());
                        db.saveOrUpdate(lam);
                    }
                    db.saveOrUpdate(keep);
                }
                try { db.delete(d); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:456"); /* jika gagal karena constraint, minimal tidak ditampilkan karena list portal/grid dideduplikasi */ }
            }
            try { db.flush(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:458");}
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:459");}
    }
    private String keyDokumenPortal(CalonPegawaiPunyaDokumen d) {
        try {
            Object dok = d == null ? null : d.getVerifikasiKelengkapanCalonPegawai();
            Object id = safeInvoke(dok, "getId");
            if (id != null) return "ID:" + String.valueOf(id);
            return "NM:" + safeString(dok, "getNama").toLowerCase();
        } catch(Exception e) {
            return "DOC:" + (d == null || d.getId() == null ? "0" : d.getId().toString());
        }
    }

    private List uniqueDokumenPortal(List docs) {
        List hasil = new ArrayList();
        Set keys = new HashSet();
        if (docs == null) return hasil;
        for (int i = 0; i < docs.size(); i++) {
            CalonPegawaiPunyaDokumen d = (CalonPegawaiPunyaDokumen) docs.get(i);
            String key = keyDokumenPortal(d);
            if (keys.contains(key)) continue;
            keys.add(key);
            hasil.add(d);
        }
        return hasil;
    }

    private Map parseMultipartFiles(HttpServletRequest request, Map multipartParams) {
        Map files = new HashMap();
        try {
            String ct = request.getContentType();
            if (ct == null || ct.toLowerCase().indexOf("multipart/") < 0) return files;
            DiskFileItemFactory factory = new DiskFileItemFactory();
            try {
                File tmp = new File(Common.REAL_PATH + "/tmp/karir_upload_tmp");
                tmp.mkdirs();
                factory.setRepository(tmp);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:496");}
            factory.setSizeThreshold(1024 * 1024);
            ServletFileUpload upload = new ServletFileUpload(factory);
            upload.setHeaderEncoding("UTF-8");
            List items = upload.parseRequest(request);
            for (int i = 0; i < items.size(); i++) {
                FileItem item = (FileItem) items.get(i);
                if (item == null) continue;
                if (item.isFormField()) {
                    try { multipartParams.put(item.getFieldName(), item.getString("UTF-8")); }
                    catch(Exception e) { multipartParams.put(item.getFieldName(), item.getString()); }
                } else if (item.getSize() > 0) {
                    files.put(item.getFieldName(), item);
                }
            }
        } catch(Throwable e) {
            try { Common.tampilErrorJikaAdmin(new Exception("Gagal membaca multipart upload Portal KARIR: " + e.getMessage(), e)); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:512");}
        }
        return files;
    }
    private String reqParam(HttpServletRequest request, Map multipartParams, String name) {
        String value = request.getParameter(name);
        if ((value == null || value.trim().length() == 0) && multipartParams != null && multipartParams.get(name) != null) value = String.valueOf(multipartParams.get(name));
        if (value == null || value.trim().length() == 0) value = queryParam(request, name);
        return nvl(value);
    }
    private String reqParamAny(HttpServletRequest request, Map multipartParams, String[] names) {
        if (names == null) return "";
        for (int i = 0; i < names.length; i++) {
            String v = reqParam(request, multipartParams, names[i]);
            if (v != null && v.trim().length() > 0) return v.trim();
        }
        return "";
    }
    private String queryParam(HttpServletRequest request, String name) {
        try {
            String qs = request.getQueryString();
            if (qs == null || qs.trim().length() == 0 || name == null) return "";
            String[] parts = qs.split("&");
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                int eq = part.indexOf('=');
                String k = eq >= 0 ? part.substring(0, eq) : part;
                String v = eq >= 0 ? part.substring(eq + 1) : "";
                k = URLDecoder.decode(k, "UTF-8");
                if (name.equalsIgnoreCase(k)) return URLDecoder.decode(v, "UTF-8");
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:543");}
        return "";
    }
    private Long parseLongSafe(String s) {
        try { if (s == null || s.trim().length() == 0) return null; return Long.valueOf(s.trim()); } catch(Exception e) { return null; }
    }
    private String linkDariKeterangan(String ket) {
        ket = nvl(ket);
        int p = ket.indexOf("FILE:");
        if (p < 0) return "";
        int end = ket.indexOf("|", p);
        return end > p ? ket.substring(p + 5, end).trim() : ket.substring(p + 5).trim();
    }
    private String linkLampiran(Session db, CalonPegawaiPunyaDokumen d) {
        try {
            if (d == null || d.getId() == null) return "";
            LampiranLain lam = (LampiranLain) db.createCriteria(LampiranLain.class)
                    .add(Restrictions.eq("ref", d.getId()))
                    .add(Restrictions.eq("jenis", CalonPegawaiPunyaDokumen.class.getName()))
                    .setMaxResults(1).uniqueResult();
            if (lam != null) {
                /* URL standar LampiranLain harus diambil dari createLinkUri(). */
                try {
                    String url = lam.createLinkUri();
                    if (url != null && url.trim().length() > 0) return url.trim();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:568");}
                if (lam.getLink() != null && lam.getLink().trim().length() > 0) return lam.getLink().trim();
                if (lam.getGdrive() != null && lam.getGdrive().trim().length() > 0) return lam.forwardGDriveUrl();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:572");}
        return linkDariKeterangan(d == null ? "" : d.getKeterangan());
    }
    private String namaRuang(RuangPegawai r) {
        if (r == null) return "";
        String nama = nvl(r.getNama());
        String kode = nvl(r.getKodeRuangan());
        return nama + (empty(kode) ? "" : " (" + kode + ")");
    }
    private String saveFile(HttpServletRequest request, Long calonId, Part filePart) throws Exception {
        if (filePart == null || filePart.getSize() <= 0) return "";
        String original = cleanFilename(getSubmittedFileName(filePart));
        File dir = new File(Common.REAL_PATH + "/media/karir_documents/" + calonId);
        dir.mkdirs();
        String fname = System.currentTimeMillis() + "_" + original;
        File outFile = new File(dir, fname);
        InputStream in = null;
        FileOutputStream fos = null;
        try {
            in = filePart.getInputStream();
            fos = new FileOutputStream(outFile);
            byte[] buf = new byte[8192]; int len;
            while ((len = in.read(buf)) > 0) fos.write(buf, 0, len);
        } finally {
            try { if (fos != null) fos.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:596");}
            try { if (in != null) in.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:597");}
        }
        return Common.ROOT + "/media/karir_documents/" + calonId + "/" + fname;
    }
    private String saveFileItem(HttpServletRequest request, Long calonId, FileItem fileItem) throws Exception {
        if (fileItem == null || fileItem.getSize() <= 0) return "";
        String original = cleanFilename(fileItem.getName());
        File dir = new File(Common.REAL_PATH + "/media/karir_documents/" + calonId);
        dir.mkdirs();
        String fname = System.currentTimeMillis() + "_" + original;
        File outFile = new File(dir, fname);
        InputStream in = null;
        FileOutputStream fos = null;
        try {
            in = fileItem.getInputStream();
            fos = new FileOutputStream(outFile);
            byte[] buf = new byte[8192]; int len;
            while ((len = in.read(buf)) > 0) fos.write(buf, 0, len);
        } finally {
            try { if (fos != null) fos.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:616");}
            try { if (in != null) in.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:617");}
            try { fileItem.delete(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:618");}
        }
        return Common.ROOT + "/media/karir_documents/" + calonId + "/" + fname;
    }
    private void saveLampiranLink(Session db, CalonPegawaiPunyaDokumen d, String namaDokumen, String link, String originalFilename) {
        try {
            if (d == null || d.getId() == null || empty(link)) return;
            LampiranLain lam = (LampiranLain) db.createCriteria(LampiranLain.class)
                    .add(Restrictions.eq("ref", d.getId()))
                    .add(Restrictions.eq("jenis", CalonPegawaiPunyaDokumen.class.getName()))
                    .setMaxResults(1).uniqueResult();
            if (lam == null) lam = new LampiranLain();
            safeSetAny(lam, "setRef", d.getId());
            safeSetAny(lam, "setJenis", CalonPegawaiPunyaDokumen.class.getName());
            safeSetAny(lam, "setNama", empty(originalFilename) ? namaDokumen : originalFilename);
            safeSetAny(lam, "setLink", link);
            safeSetAny(lam, "setDeskripsi", namaDokumen);
            try { Common.refreshSaveOrUpdate(db, lam); } catch(Exception e) { db.saveOrUpdate(lam); }
            db.flush();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:637");}
    }
    private void updateDocWithUploadedLink(Session db, CalonPegawaiPunyaDokumen d, String namaDokumen, String link, String originalFilename, String catatan) throws Exception {
        if (d == null || d.getId() == null || empty(link)) return;
        String ket = "FILE:" + link + (empty(catatan) ? "" : " | CATATAN:" + catatan);
        d.setKeterangan(ket);
        d.setStatus(CalonPegawaiPunyaDokumen.BELUM);
        try { Common.refreshSaveOrUpdate(db, d); } catch(Exception e) { db.saveOrUpdate(d); }
        db.flush();
        saveLampiranLink(db, d, namaDokumen, link, originalFilename);
    }
    private void uploadToDoc(Session db, HttpServletRequest request, CalonPegawai calon, String docName, boolean wajib, Part filePart, String catatan) throws Exception {
        if (filePart == null || filePart.getSize() <= 0) return;
        VerifikasiKelengkapanCalonPegawai t = findOrCreateTemplate(db, docName, wajib);
        CalonPegawaiPunyaDokumen d = ensureDoc(db, calon, t);
        String link = saveFile(request, calon.getId(), filePart);
        updateDocWithUploadedLink(db, d, docName, link, cleanFilename(getSubmittedFileName(filePart)), catatan);
    }
    private void uploadToDocItem(Session db, HttpServletRequest request, CalonPegawai calon, String docName, boolean wajib, FileItem fileItem, String catatan) throws Exception {
        if (fileItem == null || fileItem.getSize() <= 0) return;
        VerifikasiKelengkapanCalonPegawai t = findOrCreateTemplate(db, docName, wajib);
        CalonPegawaiPunyaDokumen d = ensureDoc(db, calon, t);
        String link = saveFileItem(request, calon.getId(), fileItem);
        updateDocWithUploadedLink(db, d, docName, link, cleanFilename(fileItem.getName()), catatan);
    }
    private Agama findAgama(Session db, String nama) {
        if (empty(nama)) return null;
        try { return (Agama) db.createCriteria(Agama.class).add(Restrictions.ilike("nama", nama.trim(), MatchMode.EXACT)).setMaxResults(1).uniqueResult(); } catch(Exception e) { return null; }
    }
    private String generateNoRegistrasi() {
        return "KAR-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "-" + RandomStringUtils.randomNumeric(3);
    }
    private String portalUrl(HttpServletRequest request) {
        String scheme = request.getScheme() == null ? "https" : request.getScheme();
        String server = request.getServerName() == null ? "" : request.getServerName();
        int port = request.getServerPort();
        String ctx = request.getContextPath() == null ? "" : request.getContextPath();
        String portPart = (port == 80 || port == 443 || port <= 0) ? "" : ":" + port;
        return scheme + "://" + server + portPart + ctx + "/karir";
    }
    private String buildEmailCandidate(HttpServletRequest request, CalonPegawai calon, Tbmuser user, String passwordPlain) {
        String lowongan = calon.getGelombangPendaftaranPegawai() == null ? "" : safeString(calon.getGelombangPendaftaranPegawai(), "getNama");
        String url = request.getScheme() + "://" + request.getServerName() + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort()) + request.getContextPath() + "/karir";
        String nama = htmlEscape(nvl(calon.getNama()));
        String lowonganEsc = htmlEscape(nvl(lowongan));
        String userId = htmlEscape(user == null ? "" : nvl(user.getUserId()));
        String pass = htmlEscape(nvl(passwordPlain));
        return "<div style='font-family:Arial,Helvetica,sans-serif;line-height:1.65;color:#172033;background:#f4f7fb;padding:24px'>"
                + "<div style='max-width:820px;margin:auto;background:#ffffff;border-radius:24px;overflow:hidden;border:1px solid #e5eaf3;box-shadow:0 18px 45px rgba(15,23,42,.10)'>"
                + "<div style='background:linear-gradient(135deg,#0f172a,#2563eb);color:white;padding:30px 34px'>"
                + "<div style='font-size:13px;letter-spacing:.08em;text-transform:uppercase;opacity:.88'>Portal KARIR</div>"
                + "<h1 style='margin:8px 0 0;font-size:28px;line-height:1.25'>" + htmlEscape(cfgText("karir_email_candidate_title", "Informasi Akun Pendaftaran Calon Pegawai")) + "</h1>"
                + "<p style='margin:12px 0 0;opacity:.9'>" + htmlEscape(cfgText("karir_email_candidate_intro", "Data pendaftaran Anda telah diterima dan akun akses Portal KARIR telah dibuat.")) + "</p>"
                + "</div>"
                + "<div style='padding:30px 34px'>"
                + "<p>Yth. <strong>" + nama + "</strong>,</p>"
                + "<p>" + htmlEscape(cfgText("karir_email_candidate_p1", "Terima kasih sudah mendaftar. Data Anda sudah tercatat dan akan diperiksa oleh panitia seleksi.")) + "</p>"
                + "<p>Lowongan yang Anda pilih adalah <strong>" + lowonganEsc + "</strong>. Setelah pendaftaran berhasil, data Anda akan masuk ke modul <strong>Pendataan Calon Pegawai</strong> di ERP. Tim administrasi akan memeriksa kesesuaian identitas, kelengkapan biodata, dokumen pendukung, serta kesesuaian kualifikasi dengan kebutuhan lowongan. Apabila dokumen sudah sesuai, status Anda dapat berlanjut ke proses verifikasi, interview, ujian, atau tahapan lain sesuai kebijakan panitia. Apabila masih ada data yang belum lengkap, dokumen kurang jelas, atau berkas perlu diperbaiki, sistem dapat menampilkan status revisi sehingga Anda dapat mengunggah ulang dokumen melalui akun yang diberikan.</p>"
                + "<p>Berikut informasi akun yang dapat digunakan untuk login ke Portal KARIR:</p>"
                + "<div style='background:#f8fafc;border:1px solid #dbe7ff;border-radius:18px;padding:18px 22px;margin:18px 0'>"
                + "<table style='width:100%;border-collapse:collapse'>"
                + "<tr><td style='padding:8px 0;color:#64748b;width:150px'>Username</td><td style='padding:8px 0'><strong style='font-size:18px;color:#0f172a'>" + userId + "</strong></td></tr>"
                + "<tr><td style='padding:8px 0;color:#64748b'>Password</td><td style='padding:8px 0'><strong style='font-size:18px;color:#0f172a'>" + pass + "</strong></td></tr>"
                + "<tr><td style='padding:8px 0;color:#64748b'>Alamat Portal</td><td style='padding:8px 0'><a href='" + htmlEscape(url) + "' style='color:#2563eb;text-decoration:none;font-weight:bold'>" + htmlEscape(url) + "</a></td></tr>"
                + "</table>"
                + "</div>"
                + "<p>" + htmlEscape(cfgText("karir_email_candidate_p2", "Setelah login, Anda dapat melihat data diri, dokumen, jadwal, dan hasil seleksi pada Portal KARIR.")) + "</p>"
                + "<p>" + htmlEscape(cfgText("karir_email_candidate_p3", "Simpan username dan password dengan baik. Gunakan fitur Kirim Ulang Akses jika email akses tidak ditemukan.")) + "</p>"
                + "<p>" + htmlEscape(cfgText("karir_email_candidate_p4", "Pastikan dokumen yang diunggah jelas dan sesuai. Jika ada revisi, segera unggah ulang dokumen yang diminta.")) + "</p>"
                + "<p>" + htmlEscape(cfgText("karir_email_candidate_p5", "Alur seleksi dimulai dari pendaftaran, pemeriksaan berkas, jadwal seleksi, lalu pengumuman hasil.")) + "</p>"
                + "<p>" + htmlEscape(cfgText("karir_email_candidate_p6", "Hubungi panitia bila ada kendala. Cek portal dan email secara berkala agar tidak melewatkan informasi penting.")) + "</p>"
                + "<p style='margin-top:26px'>Hormat kami,<br><strong>Panitia Seleksi Penerimaan Pegawai/Karyawan Baru</strong></p>"
                + "</div></div></div>";
    }
    private String buildEmailAdmin(CalonPegawai calon) {
        String lowongan = calon.getGelombangPendaftaranPegawai() == null ? "" : safeString(calon.getGelombangPendaftaranPegawai(), "getNama");
        return "<div style='font-family:Arial,sans-serif;line-height:1.7;color:#0f172a'>"
                + "<h2 style='margin:0 0 12px;color:#2563eb'>Pendaftaran Calon Pegawai Baru</h2>"
                + "<p>Berikut pendaftaran baru yang masuk dari Portal KARIR dan perlu diverifikasi melalui modul ERP Pendataan Calon Pegawai.</p>"
                + "<ul>"
                + "<li>No. Registrasi: <b>" + htmlEscape(calon.getNoRegistrasi()) + "</b></li>"
                + "<li>Nama: <b>" + htmlEscape(calon.getNama()) + "</b></li>"
                + "<li>Lowongan: <b>" + htmlEscape(lowongan) + "</b></li>"
                + "<li>Email: <b>" + htmlEscape(calon.getAlamatEmail()) + "</b></li>"
                + "<li>Telepon: <b>" + htmlEscape(calon.getTeleponPegawai()) + "</b></li>"
                + "</ul>"
                + "<p>Silakan lakukan verifikasi berkas, lengkapi jadwal interview pada catatan/parameter tambahan bila diperlukan, lalu tetapkan status diterima atau ditolak sesuai proses seleksi.</p>"
                + "</div>";
    }
    private boolean sendCandidateEmail(HttpServletRequest request, CalonPegawai calon, Tbmuser user, String passwordPlain) {
        try {
            String sender = Common.getKonfigurasi("email_pendaftaran_karir_sender", Common.getKonfigurasi("default_email", "info@ecampus.id").getNilai()).getNilai();
            String subject = Common.getKonfigurasi("email_pendaftaran_karir_subject", "Username dan Password Portal KARIR").getNilai();
            JSONArray userIds = new JSONArray(); userIds.put(user.getUserId());
            MailSender.sendMail(userIds, subject, buildEmailCandidate(request, calon, user, passwordPlain), sender, calon.getAlamatEmail(), calon);
            return true;
        } catch(Exception e) { return false; }
    }
    private void sendAdminEmail(CalonPegawai calon) {
        try {
            String sender = Common.getKonfigurasi("email_pendaftaran_karir_sender", Common.getKonfigurasi("default_email", "info@ecampus.id").getNilai()).getNilai();
            String admin = Common.getKonfigurasi("email_pendaftaran_karir_admin", sender).getNilai();
            if (admin != null && admin.trim().length() > 0) MailSender.sendMail(new JSONArray(), "Pendaftaran Calon Pegawai Baru", buildEmailAdmin(calon), sender, admin, calon);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:740");}
    }
    private JSONObject lowonganJson(Session db, GelombangPendaftaranPegawai g) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", g.getId());
        String nama = safeString(g, "getNama");
        if (empty(nama)) nama = "Lowongan Kerja";
        o.put("nama", nama);
        o.put("jenis", safeString(g, "getJenis"));
        o.put("aktif", g.getAktif());
        o.put("statusAktif", g.getAktif() ? "Aktif" : "Tidak Aktif");
        o.put("tampilFormTambahanSaatRegistrasi", g.getTampilFormTambahanSaatRegistrasi());
        o.put("tampilFormTambahanSaatLoginCalonPegawai", g.getTampilFormTambahanSaatLoginCalonPegawai());
        o.put("informasi", safeString(g, "getInformasi"));
        o.put("keterangan", safeString(g, "getKeterangan"));
        o.put("fungsiKerja", safeString(g, "getFungsiKerja"));
        o.put("pengalaman", safeString(g, "getPengalaman"));
        o.put("fasilitas", safeString(g, "getFasilitas"));
        o.put("jurusan", safeString(g, "getJurusan"));
        o.put("lulusan", safeString(g, "getLulusan"));
        o.put("persyaratan", safeString(g, "getPersyaratan"));
        o.put("tanggungJawab", safeString(g, "getTanggungJawab"));
        o.put("disclaimer", safeString(g, "getDisclaimer"));
        Object satker = safeInvoke(g, "getSatuanKerja");
        o.put("satuanKerja", safeString(satker, "getNama"));

        String kuota = firstNonEmpty(new String[] { safeString(g, "getJumlahPegawai"), safeString(g, "getJumlahDibutuhkan"), safeString(g, "getKuota"), safeString(g, "getJumlah"), safeString(g, "getDayaTampung") });
        try {
            List kelompok = db.createCriteria(KelompokPendaftaranPegawai.class)
                    .add(Restrictions.eq("gelombangPendaftaran", g))
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .addOrder(Order.asc("nama")).list();
            JSONArray arrKelompok = new JSONArray();
            int totalKuotaKelompok = 0;
            for (int i = 0; i < kelompok.size(); i++) {
                KelompokPendaftaranPegawai kp = (KelompokPendaftaranPegawai) kelompok.get(i);
                JSONObject kk = new JSONObject();
                Integer kuotaKelompok = null;
                try { kuotaKelompok = kp.getKuota(); } catch(Exception e) { kuotaKelompok = null; }
                kk.put("id", kp.getId());
                kk.put("nama", nvl(kp.getNama()));
                kk.put("deskripsi", nvl(kp.getDeskripsi()));
                kk.put("kuota", kuotaKelompok == null ? 0 : kuotaKelompok.intValue());
                totalKuotaKelompok += kuotaKelompok == null ? 0 : kuotaKelompok.intValue();
                arrKelompok.put(kk);
            }
            if ((empty(kuota) || "-".equals(kuota)) && totalKuotaKelompok > 0) kuota = String.valueOf(totalKuotaKelompok);
            o.put("kelompok", arrKelompok);
        } catch(Exception e) { o.put("kelompok", new JSONArray()); }
        o.put("kuota", empty(kuota) || "null".equalsIgnoreCase(kuota) ? "-" : kuota);

        Date mulai = firstDate(g, new String[] { "getTanggalMulai", "getMulai", "getTanggalAwal", "getWaktuMulai" });
        Date sampai = firstDate(g, new String[] { "getTanggalSelesai", "getSampai", "getSelesai", "getTanggalAkhir", "getWaktuSelesai" });
        String periode = "";
        if (mulai != null || sampai != null) periode = (mulai == null ? "" : formatDate(mulai)) + " s.d. " + (sampai == null ? "" : formatDate(sampai));
        o.put("periode", periode);
        o.put("mulai", formatDate(mulai));
        o.put("sampai", formatDate(sampai));
        boolean belumDibuka = pendaftaranBelumDibuka(mulai);
        boolean terlewat = pendaftaranSudahTerlewat(sampai);
        boolean dapatDaftar = g.getAktif() && !belumDibuka && !terlewat;
        o.put("dapatDaftar", dapatDaftar);
        o.put("pendaftaranBelumDibuka", belumDibuka);
        o.put("pendaftaranTerlewat", terlewat);
        o.put("statusPendaftaran", statusPendaftaranLowongan(g));
        o.put("pesanPendaftaran", dapatDaftar ? "Pendaftaran masih dibuka. Silakan klik Daftar Sekarang." : (terlewat ? "Pendaftaran sudah terlewat. Lowongan tetap ditampilkan sebagai informasi, tetapi pendaftaran sudah ditutup." : (belumDibuka ? "Pendaftaran belum dibuka. Lowongan ditampilkan sebagai informasi awal." : "Lowongan belum dapat digunakan untuk pendaftaran.")));
        try {
            Number pelamar = (Number) db.createCriteria(CalonPegawai.class)
                    .add(Restrictions.eq("gelombangPendaftaranPegawai", g))
                    .setProjection(Projections.rowCount()).uniqueResult();
            o.put("jumlahPelamar", pelamar == null ? 0 : pelamar.intValue());
        } catch(Exception e) { o.put("jumlahPelamar", 0); }
        try {
            int dok = g.getVerifikasiKelengkapanCalonPegawais() == null ? 0 : g.getVerifikasiKelengkapanCalonPegawais().size();
            o.put("jumlahDokumen", dok <= 0 ? DOC_NAMES.length : dok);
        } catch(Exception e) { o.put("jumlahDokumen", DOC_NAMES.length); }
        try {
            JadwalUjianPegawai j = nextJadwalLowongan(db, g);
            o.put("jadwalSeleksi", j == null ? "" : formatDateTime(j.getWaktuMulai()) + " - " + formatDateTime(j.getWaktuSampai()));
            o.put("namaJadwalSeleksi", j == null ? "" : nvl(j.getNama()));
        } catch(Exception e) { o.put("jadwalSeleksi", ""); o.put("namaJadwalSeleksi", ""); }
        return o;
    }

    private JadwalUjianPegawai nextJadwalLowongan(Session db, GelombangPendaftaranPegawai g) {
        try {
            List list = db.createCriteria(JadwalUjianPegawai.class)
                    .add(Restrictions.eq("gelombangPendaftaranPegawai", g))
                    .add(Restrictions.ge("waktuSampai", new Date()))
                    .addOrder(Order.asc("waktuMulai"))
                    .setMaxResults(1).list();
            return list == null || list.isEmpty() ? null : (JadwalUjianPegawai) list.get(0);
        } catch(Exception e) { return null; }
    }

    private String jadwalInterview(Session db, CalonPegawai c) {
        try {
            JSONObject p = readParam(c);
            String v = firstNonEmpty(new String[] { p.optString("jadwalInterview"), p.optString("waktuInterview"), p.optString("tanggalInterview"), p.optString("interview"), p.optString("jadwal") });
            if (!empty(v)) return v;
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:840");}
        try {
            GelombangPendaftaranPegawai g = c.getGelombangPendaftaranPegawai();
            if (g != null) {
                JadwalUjianPegawai j = nextJadwalLowongan(db, g);
                if (j != null) return (empty(nvl(j.getNama())) ? "Jadwal Seleksi" : nvl(j.getNama())) + ": " + formatDateTime(j.getWaktuMulai()) + " s.d. " + formatDateTime(j.getWaktuSampai());
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:847");}
        try {
            String ket = c.getKeterangan();
            if (ket != null && ket.toLowerCase().indexOf("interview") >= 0) return ket;
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:851");}
        return "";
    }

    private String ruangInterview(Session db, CalonPegawai c) {
        try {
            JSONObject p = readParam(c);
            String v = firstNonEmpty(new String[] { p.optString("ruangInterview"), p.optString("lokasiInterview"), p.optString("ruang"), p.optString("lokasi"), p.optString("tempatInterview") });
            if (!empty(v)) return v;
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:860");}
        try {
            RuangGelombangPendaftaranPegawaiPegawai rg = (RuangGelombangPendaftaranPegawaiPegawai) db.createCriteria(RuangGelombangPendaftaranPegawaiPegawai.class)
                    .add(Restrictions.eq("calonPegawai", c)).setMaxResults(1).uniqueResult();
            if (rg != null && rg.getRuangPegawai() != null) return namaRuang(rg.getRuangPegawai());
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:865");}
        try {
            GelombangPendaftaranPegawai g = c == null ? null : c.getGelombangPendaftaranPegawai();
            if (g != null) {
                JadwalUjianPegawai j = nextJadwalLowongan(db, g);
                if (j != null && j.getUjianPegawai() != null && !empty(j.getUjianPegawai().getLokasi())) return j.getUjianPegawai().getLokasi();
                List ruang = db.createCriteria(RuangPegawai.class)
                        .add(Restrictions.eq("gelombangPendaftaranPegawai", g))
                        .addOrder(Order.asc("nama"))
                        .setMaxResults(1).list();
                if (ruang != null && !ruang.isEmpty()) return namaRuang((RuangPegawai) ruang.get(0));
                if (j != null && j.getUjianPegawai() != null) {
                    ruang = db.createCriteria(RuangPegawai.class)
                            .add(Restrictions.eq("ujianPegawai", j.getUjianPegawai()))
                            .addOrder(Order.asc("nama"))
                            .setMaxResults(1).list();
                    if (ruang != null && !ruang.isEmpty()) return namaRuang((RuangPegawai) ruang.get(0));
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:884");}
        try {
            String ket = c.getKeterangan();
            if (ket != null && (ket.toLowerCase().indexOf("ruang") >= 0 || ket.toLowerCase().indexOf("lokasi") >= 0 || ket.toLowerCase().indexOf("tempat") >= 0)) return ket;
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:888");}
        return "";
    }
    private String firstNonEmpty(String[] values) {
        if (values == null) return "";
        for (int i = 0; i < values.length; i++) if (values[i] != null && values[i].trim().length() > 0 && !"null".equalsIgnoreCase(values[i].trim())) return values[i].trim();
        return "";
    }
    private Date firstDate(Object obj, String[] methods) {
        for (int i = 0; i < methods.length; i++) {
            Object v = safeInvoke(obj, methods[i]);
            if (v instanceof Date) return (Date) v;
        }
        return null;
    }
    private JSONObject calonJson(CalonPegawai c) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", c.getId());
        o.put("noRegistrasi", nvl(c.getNoRegistrasi()));
        o.put("nama", nvl(c.getNama()));
        o.put("email", nvl(c.getAlamatEmail()));
        o.put("telp", nvl(c.getTeleponPegawai()));
        o.put("alamat", nvl(c.getAlamatPegawai()));
        o.put("tempatLahir", nvl(c.getTempatLahir()));
        o.put("tanggalLahir", formatDate(c.getTanggalLahir()));
        o.put("tanggalLahirIso", isoDate(c.getTanggalLahir()));
        o.put("jenisKelamin", nvl(c.getJenisKelamin()));
        o.put("agama", c.getAgama() == null ? "" : nvl(c.getAgama().getNama()));
        o.put("tanggalPendaftaran", formatDateTime(c.getTanggalPendaftaran()));
        o.put("lowongan", c.getGelombangPendaftaranPegawai() == null ? "" : safeString(c.getGelombangPendaftaranPegawai(), "getNama"));
        return o;
    }
    private JSONObject readParam(CalonPegawai c) {
        try { String p = c.getParameterTambahan(); if (p != null && p.trim().startsWith("{")) return new JSONObject(p); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:921");}
        return new JSONObject();
    }
    private String statusCalon(Session db, CalonPegawai c, int total, int verified, int revisi) {
        if (c.getMengundurkanDiri()) return "Mengundurkan Diri";
        if (c.getDitolak()) return "Ditolak";
        if (c.getTelahDiterima() || c.getPegawai() != null) return "Diterima";
        if (c.getTerverifikasi()) return "Terverifikasi";
        String jadwal = jadwalInterview(db, c);
        if (!empty(jadwal)) return "Proses Interview";
        if (revisi > 0) return "Revisi Berkas";
        if (total > 0 && verified < total) return "Verifikasi Berkas";
        return "Seleksi Administrasi";
    }

    /*
     * Pengumuman KARIR harus disaring ketat menggunakan
     * PengumumanAkademis.UNTUK_KARIR agar portal ini tidak menampilkan
     * pengumuman akademik umum, PMB, mahasiswa, sekolah, atau kategori lain.
     * Kriteria di bawah mengikuti pola dari modul lama TampilanPengumumanKarirAction.
     */
    private Criteria initCriteriaPengumumanKarir(Session db, boolean order) {
        Date now = new Date();
        Criterion r = Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_KARIR);
        Criteria criteria = db.createCriteria(PengumumanAkademis.class)
                .add(Restrictions.or(
                        Restrictions.or(Restrictions.eq("tetapTampilkanPengumumanMeskipunSudahKelewat", true),
                                Restrictions.isNull("tetapTampilkanPengumumanMeskipunSudahKelewat")),
                        Restrictions.or(Restrictions.le("tanggal", now),
                                Restrictions.ge("sampai", now))))
                .add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
                .add(r);
        if (order) {
            try {
                criteria.createAlias("kategoriPengumuman", "kategoriPengumuman", Criteria.LEFT_JOIN)
                        .addOrder(Order.asc("kategoriPengumuman.nomorUrut"))
                        .addOrder(Order.desc("tanggal"))
                        .addOrder(Order.desc("id"));
            } catch(Exception e) {
                try { criteria.addOrder(Order.desc("tanggal")).addOrder(Order.desc("id")); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:960");}
            }
        }
        return criteria;
    }

    private String stripHtmlText(String s) {
        s = nvl(s);
        if (s.length() == 0) return "";
        s = s.replaceAll("(?i)<br\\s*/?>", " ");
        s = s.replaceAll("(?i)</p>", " ");
        s = s.replaceAll("(?is)<[^>]*>", " ");
        s = s.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'");
        s = s.replaceAll("[ \t\f\r\n]+", " ").trim();
        return s;
    }

    private JSONObject pengumumanKarirJson(PengumumanAkademis p) throws Exception {
        JSONObject o = new JSONObject();
        Object id = safeInvoke(p, "getId");
        o.put("id", id == null ? "" : String.valueOf(id));
        String judul = firstNonEmpty(new String[] { safeString(p, "getJudul"), safeString(p, "getNama"), "Pengumuman KARIR" });
        String isi = firstNonEmpty(new String[] { safeString(p, "getIsi"), safeString(p, "getKeterangan"), safeString(p, "getDeskripsi"), safeString(p, "getInformasi") });
        Date tanggal = firstDate(p, new String[] { "getTanggal", "getMulai", "getWaktu", "getTanggalMulai" });
        Date sampai = firstDate(p, new String[] { "getSampai", "getSelesai", "getTanggalSelesai" });
        Object kategori = safeInvoke(p, "getKategoriPengumuman");
        o.put("judul", judul);
        o.put("isi", stripHtmlText(isi));
        o.put("tanggal", formatDate(tanggal));
        o.put("sampai", formatDate(sampai));
        o.put("periode", (tanggal == null && sampai == null) ? "" : (tanggal == null ? "" : formatDate(tanggal)) + (sampai == null ? "" : " s.d. " + formatDate(sampai)));
        o.put("kategori", safeString(kategori, "getNama"));
        o.put("diperuntukkan", PengumumanAkademis.UNTUK_KARIR);
        return o;
    }
%>
<%
    JSONObject res = new JSONObject();
    Session db = null;
    Transaction tx = null;
    try {
        Common.ROOT = request.getContextPath();
        Common.REAL_PATH = application.getRealPath("/");
        db = HibernateUtil.openSession();
        Map globalMultipartParams = new HashMap();
        Map globalMultipartFiles = new HashMap();
        boolean multipartRequest = false;
        try { String ct = request.getContentType(); multipartRequest = ct != null && ct.toLowerCase().indexOf("multipart/") >= 0; } catch(Exception e) { multipartRequest = false; }

        String aksi = request.getParameter("action");
        if (aksi == null || aksi.trim().length() == 0) aksi = request.getParameter("aksi");
        if (aksi == null || aksi.trim().length() == 0) aksi = request.getParameter("act");
        if (aksi == null || aksi.trim().length() == 0) aksi = request.getParameter("mode");
        if (aksi == null || aksi.trim().length() == 0) aksi = queryParam(request, "action");
        if (aksi == null || aksi.trim().length() == 0) aksi = queryParam(request, "aksi");
        if (aksi == null || aksi.trim().length() == 0) aksi = queryParam(request, "act");
        if (aksi == null || aksi.trim().length() == 0) aksi = queryParam(request, "mode");

        /*
         * Pada upload multipart dari browser/Tomcat lama, request.getParameter("action")
         * kadang tidak terbaca. Jika action kosong, parse multipart sekali di awal
         * agar service tetap mengenali aksi dan file upload tidak hilang karena stream
         * dibaca ulang.
         */
        if ((aksi == null || aksi.trim().length() == 0) && multipartRequest) {
            globalMultipartFiles = parseMultipartFiles(request, globalMultipartParams);
            Object a0 = globalMultipartParams.get("action");
            if (a0 == null) a0 = globalMultipartParams.get("aksi");
            if (a0 == null) a0 = globalMultipartParams.get("act");
            if (a0 == null) a0 = globalMultipartParams.get("mode");
            if (a0 != null) aksi = String.valueOf(a0);
            if ((aksi == null || aksi.trim().length() == 0) && (globalMultipartParams.get("id") != null || globalMultipartParams.get("dokumen_id") != null || globalMultipartParams.get("document_id") != null || globalMultipartFiles.get("file") != null)) {
                aksi = "upload_dokumen";
            }
        }

        if ((aksi == null || aksi.trim().length() == 0) && multipartRequest && (queryParam(request, "id").trim().length() > 0 || queryParam(request, "dokumen_id").trim().length() > 0 || queryParam(request, "document_id").trim().length() > 0)) aksi = "upload_dokumen";
        if (aksi == null) aksi = "";
        aksi = aksi.trim().toLowerCase();
        if ("daftar".equals(aksi) || "daftar_calon_pegawai".equals(aksi) || "pendaftaran".equals(aksi) || "register_candidate".equals(aksi) || "simpan_pendaftaran".equals(aksi)) aksi = "register";
        if ("kirim_ulang_akses".equals(aksi) || "resend".equals(aksi) || "retry_email".equals(aksi) || "kirim_ulang_email".equals(aksi) || "resend_account".equals(aksi)) aksi = "resend_access";
        if ("upload".equals(aksi) || "upload_document".equals(aksi) || "upload_berkas".equals(aksi) || "upload_dokumen_calon".equals(aksi) || "upload_lampiran".equals(aksi) || "lengkapi_berkas".equals(aksi)) aksi = "upload_dokumen";

        if ("list_pengumuman_karir".equals(aksi) || "list_pengumuman".equals(aksi)) {
            Criteria cr = initCriteriaPengumumanKarir(db, true);
            cr.setMaxResults(12);
            List list = cr.list();
            JSONArray arr = new JSONArray();
            for (int i = 0; i < list.size(); i++) {
                arr.put(pengumumanKarirJson((PengumumanAkademis) list.get(i)));
            }
            res.put("status", "success");
            res.put("data", arr);
            res.put("filter", "PengumumanAkademis.UNTUK_KARIR");
        }
        else if ("list_lowongan".equals(aksi)) {
            String q = nvl(request.getParameter("q"));
            String modeTanggal = Common.getKonfigurasi("karir_tampilkan_lowongan_terlewat", "TAMPIL_DISABLED").getNilai();
            boolean sembunyikanTerlewat = "SEMBUNYIKAN".equalsIgnoreCase(modeTanggal) || "HIDE".equalsIgnoreCase(modeTanggal);
            Criteria cr = db.createCriteria(GelombangPendaftaranPegawai.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
            cr.addOrder(Order.desc("id"));
            if (!empty(q)) {
                Junction cari = Restrictions.disjunction();
                cari.add(Restrictions.ilike("nama", q, MatchMode.ANYWHERE));
                cari.add(Restrictions.ilike("keterangan", q, MatchMode.ANYWHERE));
                cari.add(Restrictions.ilike("informasi", q, MatchMode.ANYWHERE));
                cari.add(Restrictions.ilike("jenis", q, MatchMode.ANYWHERE));
                cari.add(Restrictions.ilike("fungsiKerja", q, MatchMode.ANYWHERE));
                cari.add(Restrictions.ilike("pengalaman", q, MatchMode.ANYWHERE));
                cari.add(Restrictions.ilike("jurusan", q, MatchMode.ANYWHERE));
                cari.add(Restrictions.ilike("lulusan", q, MatchMode.ANYWHERE));
                cari.add(Restrictions.ilike("persyaratan", q, MatchMode.ANYWHERE));
                cari.add(Restrictions.ilike("tanggungJawab", q, MatchMode.ANYWHERE));
                cr.add(cari);
            }
            cr.setMaxResults(200);
            List list = cr.list();
            JSONArray arr = new JSONArray();
            for (int i = 0; i < list.size(); i++) {
                GelombangPendaftaranPegawai g = (GelombangPendaftaranPegawai) list.get(i);
                if (sembunyikanTerlewat) {
                    Date sampai = firstDate(g, new String[] { "getTanggalSelesai", "getSampai", "getSelesai", "getTanggalAkhir", "getWaktuSelesai" });
                    if (pendaftaranSudahTerlewat(sampai)) continue;
                }
                arr.put(lowonganJson(db, g));
                if (arr.length() >= 60) break;
            }
            res.put("status", "success"); res.put("data", arr);
        }
        else if ("list_agama".equals(aksi)) {
            JSONArray arr = new JSONArray();
            try {
                List list = db.createCriteria(Agama.class).addOrder(Order.asc("nama")).list();
                for (int i = 0; i < list.size(); i++) { Agama a = (Agama) list.get(i); JSONObject o = new JSONObject(); o.put("id", a.getId()); o.put("nama", nvl(a.getNama())); arr.put(o); }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:1095");}
            res.put("status", "success"); res.put("data", arr);
        }
        else if ("register".equals(aksi)) {
            Map multipartParams = globalMultipartParams;
            Map multipartFiles = globalMultipartFiles;
            if ((multipartParams == null || multipartParams.isEmpty()) && (multipartFiles == null || multipartFiles.isEmpty())) {
                multipartParams = new HashMap();
                multipartFiles = parseMultipartFiles(request, multipartParams);
            }
            String nama = reqParam(request, multipartParams, "nama");
            String email = reqParam(request, multipartParams, "email");
            String telp = reqParam(request, multipartParams, "telp");
            String tempat = reqParam(request, multipartParams, "tempat_lahir");
            String tanggal = reqParam(request, multipartParams, "tanggal_lahir");
            String jk = reqParam(request, multipartParams, "jenis_kelamin");
            String alamat = reqParam(request, multipartParams, "alamat");
            String agamaNama = reqParam(request, multipartParams, "agama");
            String lowonganId = reqParam(request, multipartParams, "lowongan_id");
            String kelompokId = reqParam(request, multipartParams, "kelompok_id");
            if (empty(lowonganId)) { res.put("status", "error"); res.put("message", "Silakan pilih lowongan kerja terlebih dahulu."); }
            else if (empty(nama) || empty(email) || empty(telp) || empty(tempat) || empty(tanggal) || empty(jk) || empty(alamat)) { res.put("status", "error"); res.put("message", "Nama, tempat/tanggal lahir, jenis kelamin, alamat, telepon, dan email wajib diisi."); }
            else if (!Common.isValidEmailAddress(email)) { res.put("status", "error"); res.put("message", "Format email tidak valid."); }
            else {
                int countCalon = ((Number) db.createCriteria(CalonPegawai.class).add(Restrictions.eq("alamatEmail", email)).setProjection(Projections.rowCount()).uniqueResult()).intValue();
                int countUser = ((Number) db.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.eq("userId", email), Restrictions.eq("email", email))).setProjection(Projections.rowCount()).uniqueResult()).intValue();
                if (countCalon > 0 || countUser > 0) { res.put("status", "error"); res.put("message", "Email yang Anda masukkan telah terdaftar. Gunakan fitur Kirim Ulang Akses apabila email username/password hilang."); }
                else {
                    tx = db.beginTransaction();
                    GelombangPendaftaranPegawai lowongan = (GelombangPendaftaranPegawai) db.get(GelombangPendaftaranPegawai.class, Long.valueOf(lowonganId));
                    if (lowongan == null) { res.put("status", "error"); res.put("message", "Lowongan kerja tidak ditemukan atau sudah tidak aktif."); }
                    else if (!lowonganDapatDaftar(lowongan)) {
                        res.put("status", "error");
                        res.put("message", statusPendaftaranLowongan(lowongan) + ". Anda tidak dapat melakukan pendaftaran pada gelombang ini.");
                    }
                    else {
                        CalonPegawai calon = new CalonPegawai();
                        calon.setNoRegistrasi(generateNoRegistrasi());
                        calon.setKode(calon.getNoRegistrasi());
                        calon.setNomorInduk(calon.getNoRegistrasi());
                        calon.setNim(calon.getNoRegistrasi());
                        calon.setNamaPegawai(nama);
                        calon.setAlamatEmail(email);
                        calon.setTeleponPegawai(telp);
                        calon.setTempatLahir(tempat);
                        calon.setTanggalLahir(parseDate(tanggal));
                        calon.setJenisKelamin(jk);
                        calon.setAlamatPegawai(alamat);
                        calon.setAgama(findAgama(db, agamaNama));
                        calon.setGelombangPendaftaranPegawai(lowongan);
                        if (!empty(kelompokId)) { try { KelompokPendaftaranPegawai kp = (KelompokPendaftaranPegawai) db.get(KelompokPendaftaranPegawai.class, Long.valueOf(kelompokId)); if (kp != null) calon.setKelompokPendaftaranPegawai(kp); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:1145");} }
                        calon.setTanggalPendaftaran(new Date());
                        calon.setAktif(true);
                        calon.setTelahDiterima(false);
                        calon.setDitolak(false);
                        calon.setPernyataan(true);
                        JSONObject param = new JSONObject();
                        param.put("sumber", "Portal KARIR"); param.put("statusAwal", "Seleksi Administrasi"); param.put("lowongan", safeString(lowongan, "getNama")); param.put("waktuDaftar", formatDateTime(new Date()));
                        calon.setParameterTambahan(param.toString());
                        calon.setKeterangan("Pendaftaran awal dari Portal KARIR untuk lowongan: " + safeString(lowongan, "getNama"));
                        db.save(calon); db.flush();
                        ensureDocumentsForCalon(db, calon, lowongan);
                        for (int i = 0; i < DOC_NAMES.length; i++) {
                            FileItem fi = (FileItem) multipartFiles.get(DOC_FIELDS[i]);
                            if (fi != null && fi.getSize() > 0) {
                                uploadToDocItem(db, request, calon, DOC_NAMES[i], DOC_REQUIRED[i], fi, "Diunggah saat pendaftaran awal");
                            } else {
                                Part p = null; try { p = request.getPart(DOC_FIELDS[i]); } catch(Exception e) { p = null; }
                                uploadToDoc(db, request, calon, DOC_NAMES[i], DOC_REQUIRED[i], p, "Diunggah saat pendaftaran awal");
                            }
                        }
                        String pass = RandomStringUtils.randomAlphanumeric(8);
                        Tbmuser user = createOrResetCandidateUser(db, calon, pass);
                        tx.commit(); tx = null;
                        saveOrUpdateCandidateAccess(user, pass);
                        boolean emailOk = sendCandidateEmail(request, calon, user, pass);
                        sendAdminEmail(calon);
                        res.put("status", "success");
                        res.put("message", emailOk ? "Pendaftaran berhasil. Username dan password telah dikirim ke email Anda." : "Pendaftaran berhasil dan akun telah dibuat, tetapi email akses belum berhasil dikirim. Silakan gunakan fitur Kirim Ulang Akses atau hubungi admin.");
                    }
                    if (tx != null) { tx.commit(); tx = null; }
                }
            }
        }
        else if ("resend_access".equals(aksi)) {
            String email = nvl(request.getParameter("email"));
            if (empty(email)) { res.put("status", "error"); res.put("message", "Masukkan email yang sudah terdaftar."); }
            else {
                CalonPegawai calon = (CalonPegawai) db.createCriteria(CalonPegawai.class).add(Restrictions.eq("alamatEmail", email)).setMaxResults(1).uniqueResult();
                if (calon == null) { res.put("status", "error"); res.put("message", "Email belum terdaftar sebagai calon pegawai."); }
                else {
                    tx = db.beginTransaction(); String pass = RandomStringUtils.randomAlphanumeric(8); Tbmuser user = createOrResetCandidateUser(db, calon, pass); tx.commit(); tx = null;
                    saveOrUpdateCandidateAccess(user, pass);
                    boolean ok = sendCandidateEmail(request, calon, user, pass);
                    if (ok) { res.put("status", "success"); res.put("message", "Username dan password terbaru berhasil dikirim ulang ke email terdaftar."); }
                    else { res.put("status", "error"); res.put("message", "Password telah diperbarui, tetapi email belum berhasil dikirim. Silakan hubungi admin."); }
                }
            }
        }
        else if ("login".equals(aksi)) {
            String username = nvl(request.getParameter("username"));
            String password = nvl(request.getParameter("password"));
            Tbmuser user = null; CalonPegawai calon = null;
            List users = db.createCriteria(Tbmuser.class)
                    .add(Restrictions.or(Restrictions.eq("userId", username), Restrictions.eq("email", username)))
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .setMaxResults(5).list();
            for (int i = 0; i < users.size(); i++) {
                Tbmuser u = (Tbmuser) users.get(i);
                String p = "";
                try { p = Common.desEncrypter.get().decrypt(u.getUserPassword()); } catch(Exception e) { p = nvl(u.getUserPassword()); }
                if (password.equals(p) && u.getCalonPegawai() != null) { user = u; calon = u.getCalonPegawai(); break; }
            }
            if (calon == null) { res.put("status", "error"); res.put("message", "Username/email atau password salah."); }
            else if (!calon.getAktif()) { res.put("status", "error"); res.put("message", "Akun calon pegawai belum aktif atau tidak dapat digunakan."); }
            else {
                KarirConfigUtil.putKarirSession(request, user, calon);
                KarirConfigUtil.attachLoginCookies(request, response, user, calon);
                res.put("status", "success"); res.put("calon_id", calon.getId());
                res.put("cookieLogin", KarirConfigUtil.useLoginCookie());
            }
        }
        else if ("logout".equals(aksi)) {
            KarirConfigUtil.clearKarirSession(request);
            KarirConfigUtil.clearLoginCookies(request, response);
            res.put("status", "success");
        }
        else if ("get_profile".equals(aksi)) {
            CalonPegawai sc = KarirConfigUtil.resolveLoggedCandidate(request);
            if (sc == null) { try { sc = (CalonPegawai) request.getSession().getAttribute("CalonPegawai"); } catch(Exception e) { sc = null; } }
            if (sc == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
            else {
                tx = db.beginTransaction();
                CalonPegawai c = (CalonPegawai) db.get(CalonPegawai.class, sc.getId());
                ensureDocumentsForCalon(db, c, c == null ? null : c.getGelombangPendaftaranPegawai());
                cleanupDuplicateDocumentsForCalon(db, c);
                tx.commit(); tx = null;
                List docs = db.createCriteria(CalonPegawaiPunyaDokumen.class)
                        .createAlias("verifikasiKelengkapanCalonPegawai", "dok", Criteria.INNER_JOIN)
                        .add(Restrictions.eq("calonPegawai", c))
                        .add(Restrictions.or(Restrictions.isNull("dok.aktif"), Restrictions.eq("dok.aktif", true)))
                        .list();
                docs = uniqueDokumenPortal(docs);
                int total = docs.size(), verified = 0, revisi = 0;
                for (int i = 0; i < docs.size(); i++) { CalonPegawaiPunyaDokumen d = (CalonPegawaiPunyaDokumen) docs.get(i); if (CalonPegawaiPunyaDokumen.VERIFIKASI.equals(d.getStatus())) verified++; if (CalonPegawaiPunyaDokumen.REVISI.equals(d.getStatus())) revisi++; }
                JSONObject stat = new JSONObject();
                String status = statusCalon(db, c, total, verified, revisi);
                stat.put("status", status); stat.put("dokumenTotal", total); stat.put("dokumenTerverifikasi", verified); stat.put("dokumenRevisi", revisi); stat.put("jadwalInterview", jadwalInterview(db, c)); stat.put("ruangInterview", ruangInterview(db, c));
                stat.put("notifikasi", revisi > 0 ? "Ada dokumen yang harus direvisi. Silakan unggah ulang dokumen sesuai catatan admin." : ("Proses saat ini: " + status + ". Pantau halaman ini secara berkala untuk melihat pembaruan dari panitia seleksi."));
                res.put("status", "success"); res.put("calon", calonJson(c)); res.put("stat", stat);
                request.getSession().setAttribute("KARIR_LOGGED_IN", c); request.getSession().setAttribute("CalonPegawai", c);
            }
        }
        else if ("update_profile".equals(aksi)) {
            CalonPegawai sc = KarirConfigUtil.resolveLoggedCandidate(request);
            if (sc == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
            else {
                tx = db.beginTransaction();
                CalonPegawai c = (CalonPegawai) db.get(CalonPegawai.class, sc.getId());
                String nama = nvl(request.getParameter("nama")); String email = nvl(request.getParameter("email")); String telp = nvl(request.getParameter("telp")); String tempat = nvl(request.getParameter("tempat_lahir")); String tanggal = nvl(request.getParameter("tanggal_lahir")); String jk = nvl(request.getParameter("jenis_kelamin")); String alamat = nvl(request.getParameter("alamat")); String agamaNama = nvl(request.getParameter("agama"));
                if (empty(nama) || empty(email) || empty(telp) || empty(tempat) || empty(tanggal) || empty(jk) || empty(alamat)) { res.put("status", "error"); res.put("message", "Data wajib belum lengkap."); }
                else if (!Common.isValidEmailAddress(email)) { res.put("status", "error"); res.put("message", "Format email tidak valid."); }
                else {
                    c.setNamaPegawai(nama); c.setAlamatEmail(email); c.setTeleponPegawai(telp); c.setTempatLahir(tempat); c.setTanggalLahir(parseDate(tanggal)); c.setJenisKelamin(jk); c.setAlamatPegawai(alamat); c.setAgama(findAgama(db, agamaNama)); db.update(c);
                    Tbmuser u = (Tbmuser) db.createCriteria(Tbmuser.class).add(Restrictions.eq("calonPegawai", c)).setMaxResults(1).uniqueResult(); if (u != null) { u.setEmail(email); u.setUserNama(nama); db.update(u); request.getSession().setAttribute("KARIR_USER_LOGGED_IN", u); }
                    res.put("status", "success"); res.put("message", "Data diri berhasil diperbarui."); request.getSession().setAttribute("KARIR_LOGGED_IN", c); request.getSession().setAttribute("CalonPegawai", c);
                }
                tx.commit(); tx = null;
            }
        }
        else if ("list_dokumen".equals(aksi)) {
            CalonPegawai sc = KarirConfigUtil.resolveLoggedCandidate(request);
            if (sc == null) { try { sc = (CalonPegawai) request.getSession().getAttribute("CalonPegawai"); } catch(Exception e) { sc = null; } }
            if (sc == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
            else {
                tx = db.beginTransaction(); CalonPegawai c = (CalonPegawai) db.get(CalonPegawai.class, sc.getId()); ensureDocumentsForCalon(db, c, c == null ? null : c.getGelombangPendaftaranPegawai()); cleanupDuplicateDocumentsForCalon(db, c); tx.commit(); tx = null;
                JSONArray arr = new JSONArray();
                List docs = db.createCriteria(CalonPegawaiPunyaDokumen.class)
                        .createAlias("verifikasiKelengkapanCalonPegawai", "dok", Criteria.INNER_JOIN)
                        .add(Restrictions.eq("calonPegawai", c))
                        .add(Restrictions.or(Restrictions.isNull("dok.aktif"), Restrictions.eq("dok.aktif", true)))
                        .addOrder(Order.asc("dok.id")).addOrder(Order.asc("id")).list();
                docs = uniqueDokumenPortal(docs);
                for (int i = 0; i < docs.size(); i++) {
                    CalonPegawaiPunyaDokumen d = (CalonPegawaiPunyaDokumen) docs.get(i); Object dok = d.getVerifikasiKelengkapanCalonPegawai();
                    String ket = nvl(d.getKeterangan()); String link = linkLampiran(db, d);
                    JSONObject o = new JSONObject(); o.put("id", d.getId()); o.put("status", d.getStatus()); o.put("keterangan", ket); o.put("link", link); o.put("nama", safeString(dok, "getNama")); o.put("kode", firstNonEmpty(new String[] { safeString(dok, "getNomorInduk"), safeString(dok, "getKode") })); o.put("wajib", safeBool(dok, "getWajib", false)); arr.put(o);
                }
                res.put("status", "success"); res.put("data", arr);
            }
        }
        else if ("upload_dokumen".equals(aksi)) {
            CalonPegawai sc = KarirConfigUtil.resolveLoggedCandidate(request);
            if (sc == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
            else {
                Map multipartParams = globalMultipartParams;
                Map multipartFiles = globalMultipartFiles;
                if ((multipartParams == null || multipartParams.isEmpty()) && (multipartFiles == null || multipartFiles.isEmpty())) {
                    multipartParams = new HashMap();
                    multipartFiles = parseMultipartFiles(request, multipartParams);
                }
                Long id = parseLongSafe(reqParamAny(request, multipartParams, new String[] { "id", "dokumen_id", "document_id", "doc_id" }));
                String catatan = reqParamAny(request, multipartParams, new String[] { "catatan", "keterangan", "note" });
                FileItem fileItem = (FileItem) multipartFiles.get("file");
                Part filePart = null; if (fileItem == null) { try { filePart = request.getPart("file"); } catch(Exception e) { filePart = null; } }
                if (id == null) { res.put("status", "error"); res.put("message", "Dokumen tidak valid. ID dokumen tidak terkirim."); }
                else if ((fileItem == null || fileItem.getSize() <= 0) && (filePart == null || filePart.getSize() <= 0)) { res.put("status", "error"); res.put("message", "Pilih file terlebih dahulu."); }
                else {
                    tx = db.beginTransaction();
                    CalonPegawaiPunyaDokumen d = (CalonPegawaiPunyaDokumen) db.get(CalonPegawaiPunyaDokumen.class, id);
                    if (d == null || d.getCalonPegawai() == null || !d.getCalonPegawai().getId().equals(sc.getId())) { res.put("status", "error"); res.put("message", "Dokumen tidak valid."); }
                    else if (CalonPegawaiPunyaDokumen.VERIFIKASI.equals(d.getStatus())) { res.put("status", "error"); res.put("message", "Dokumen sudah terverifikasi dan tidak dapat diunggah ulang."); }
                    else {
                        String namaDok = d.getVerifikasiKelengkapanCalonPegawai() == null ? "Dokumen" : nvl(d.getVerifikasiKelengkapanCalonPegawai().getNama());
                        if (fileItem != null && fileItem.getSize() > 0) {
                            String link = saveFileItem(request, sc.getId(), fileItem);
                            updateDocWithUploadedLink(db, d, namaDok, link, cleanFilename(fileItem.getName()), catatan);
                        } else {
                            String link = saveFile(request, sc.getId(), filePart);
                            updateDocWithUploadedLink(db, d, namaDok, link, cleanFilename(getSubmittedFileName(filePart)), catatan);
                        }
                        res.put("status", "success"); res.put("message", "Dokumen berhasil dikirim. Status akan diverifikasi admin.");
                    }
                    tx.commit(); tx = null;
                }
            }
        }
        else { res.put("status", "error"); res.put("message", "Aksi tidak dikenali. Parameter action tidak diterima oleh service KARIR."); res.put("action", aksi); }
    } catch(Exception e) {
        try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:1324");}
        try { res.put("status", "error"); res.put("message", e.getMessage() == null ? e.toString() : e.getMessage()); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:1325");}
    } finally {
        try { if (db != null) db.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:1327");}
        try { if (db != null) db.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:1328");}
        try { if (db != null) db.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:1329");}
        try { HibernateUtil.closeSessionQuietly(db); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:1330");}
    }
    out.print(res.toString());
    out.flush();
%>
