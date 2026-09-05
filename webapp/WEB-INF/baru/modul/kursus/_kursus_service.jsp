<%@page import="java.io.File"%>
<%@page import="java.io.InputStream"%>
<%@page import="java.io.FileOutputStream"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="org.apache.commons.fileupload.FileItem"%>
<%@page import="org.apache.commons.fileupload.disk.DiskFileItemFactory"%>
<%@page import="org.apache.commons.fileupload.servlet.ServletFileUpload"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.hibernate.StreamingHibernateUtil"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.action.master.kursus.helper.KursusUtil"%>
<%@page import="ais.database.model.kursus.KategoriProdukKursus"%>
<%@page import="ais.database.model.kursus.TingkatKelasProdukKursus"%>
<%@page import="ais.database.model.kursus.PesertaKursus"%>
<%@page import="ais.database.model.kursus.ProdukKursus"%>
<%@page import="ais.database.model.kursus.SeksiKursus"%>
<%@page import="ais.database.model.kursus.MateriKursus"%>
<%@page import="ais.database.model.kursus.PesertaPunyaProdukKursus"%>
<%@page import="ais.database.model.kursus.ProdukPeserta"%>
<%@page import="ais.database.model.kursus.ProgressMateriKursus"%>
<%@page import="ais.database.model.kursus.UlasanKursus"%>
<%@page import="ais.database.model.kursus.KuponKursus"%>
<%@page import="ais.database.model.kursus.PengumpulanTugasKursus"%>
<%@page import="ais.database.model.kursus.PercobaanKuisKursus"%>
<%@page import="ais.database.model.kursus.JawabanPercobaanKuisKursus"%>
<%@page import="ais.database.model.kursus.SertifikatKursus"%>
<%@page import="ais.database.model.Ujian"%>
<%@page import="ais.database.model.UjianPunyaSoal"%>
<%@page import="ais.database.model.BankSoal"%>
<%@page import="ais.database.model.BankSoalDetail"%>
<%@page import="ais.database.model.PenjelasanBankSoal"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Collections"%>
<%@page import="ais.database.model.bni.BniRequest"%>
<%@page import="ais.common.BniCommon"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
    private static final String JENIS_THUMBNAIL = "Gambar Produk Kursus";
    private static final String JENIS_VIDEO_MATERI = "Video Materi Kursus";

    private String nvl(String s) { return s == null ? "" : s.trim(); }
    private boolean empty(String s) { return s == null || s.trim().isEmpty(); }
    private double numParam(HttpServletRequest request, String key, double def) {
        try { String v = request.getParameter(key); return v == null || v.trim().isEmpty() ? def : Double.parseDouble(v.trim()); } catch (Exception e) { return def; }
    }
    private Long longParam(HttpServletRequest request, String key) {
        try { String v = request.getParameter(key); return v == null || v.trim().isEmpty() ? null : Long.valueOf(v.trim()); } catch (Exception e) { return null; }
    }
    private int intParam(HttpServletRequest request, String key, int def) {
        try { String v = request.getParameter(key); return v == null || v.trim().isEmpty() ? def : Integer.parseInt(v.trim()); } catch (Exception e) { return def; }
    }
    private boolean boolParam(HttpServletRequest request, String key, boolean def) {
        try { String v = request.getParameter(key); return v == null ? def : ("true".equalsIgnoreCase(v.trim()) || "1".equals(v.trim())); } catch (Exception e) { return def; }
    }
    private String cleanFilename(String s) {
        if (s == null || s.trim().isEmpty()) return "file";
        return s.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /*
     * Multipart (upload thumbnail/video) TIDAK dikonfigurasi lewat @MultipartConfig/<multipart-config>
     * di web.xml (JSP tidak bisa diberi anotasi servlet 3.0), sehingga request.getPart()/getParameter()
     * TIDAK bisa diandalkan untuk body multipart di sini -- pola yang sama seperti
     * webapp/WEB-INF/baru/modul/karir/_karir_service.jsp. Parse manual pakai Apache Commons FileUpload:
     * field teks masuk ke multipartParams, file masuk ke map files (key = nama field).
     */
    private Map parseMultipartFiles(HttpServletRequest request, Map multipartParams) {
        Map files = new HashMap();
        try {
            String ct = request.getContentType();
            if (ct == null || ct.toLowerCase().indexOf("multipart/") < 0) return files;
            DiskFileItemFactory factory = new DiskFileItemFactory();
            try {
                File tmp = new File(Common.REAL_PATH + "/tmp/kursus_upload_tmp");
                tmp.mkdirs();
                factory.setRepository(tmp);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp:parseMultipart1"); }
            factory.setSizeThreshold(1024 * 1024);
            ServletFileUpload upload = new ServletFileUpload(factory);
            upload.setHeaderEncoding("UTF-8");
            List items = upload.parseRequest(request);
            for (int i = 0; i < items.size(); i++) {
                FileItem item = (FileItem) items.get(i);
                if (item == null) continue;
                if (item.isFormField()) {
                    try { multipartParams.put(item.getFieldName(), item.getString("UTF-8")); }
                    catch (Exception e) { multipartParams.put(item.getFieldName(), item.getString()); }
                } else if (item.getSize() > 0) {
                    files.put(item.getFieldName(), item);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(new Exception(e), "auto-audit webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp:parseMultipart2");
        }
        return files;
    }
    private String reqParam(HttpServletRequest request, Map multipartParams, String name) {
        if (multipartParams != null && multipartParams.get(name) != null) return nvl(String.valueOf(multipartParams.get(name)));
        return nvl(request.getParameter(name));
    }

    /* Simpan file mentah (FileItem hasil parseMultipartFiles) ke disk media/<subfolder>/<id>/, kembalikan URL statisnya. */
    private String simpanFileKeDisk(String subfolder, Object id, FileItem fileItem) throws Exception {
        if (fileItem == null || fileItem.getSize() <= 0) return "";
        String original = cleanFilename(fileItem.getName());
        File dir = new File(Common.REAL_PATH + "/media/" + subfolder + "/" + id);
        dir.mkdirs();
        String fname = System.currentTimeMillis() + "_" + original;
        File outFile = new File(dir, fname);
        InputStream in = null;
        FileOutputStream fos = null;
        try {
            in = fileItem.getInputStream();
            fos = new FileOutputStream(outFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) fos.write(buf, 0, len);
        } finally {
            try { if (fos != null) fos.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp:simpanFile1"); }
            try { if (in != null) in.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp:simpanFile2"); }
            try { fileItem.delete(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp:simpanFile3"); }
        }
        return Common.ROOT + "/media/" + subfolder + "/" + id + "/" + fname;
    }

    /*
     * LampiranLain hanya dipetakan pada SessionFactory STREAMING (hibernate.streaming.cfg.xml),
     * tidak pada SessionFactory utama -- lihat pola yang sama di DoUpload.java. Simpan/baca lewat
     * StreamingHibernateUtil, di sesi TERPISAH dari sesi utama (db) yang dipakai di bawah.
     */
    private void simpanLinkLampiran(Long ref, String jenis, String url, String namaAsli) {
        if (ref == null || empty(url)) return;
        Session sSession = null;
        try {
            sSession = StreamingHibernateUtil.getInstance().currentSession();
            LampiranLain lam = (LampiranLain) sSession.createCriteria(LampiranLain.class)
                    .add(Restrictions.eq("ref", ref)).add(Restrictions.eq("jenis", jenis))
                    .setMaxResults(1).uniqueResult();
            if (lam == null) {
                lam = new LampiranLain();
                lam.setRef(ref);
                lam.setJenis(jenis);
            }
            lam.setNama(namaAsli);
            lam.setLink(url);
            sSession.getTransaction().begin();
            sSession.saveOrUpdate(lam);
            sSession.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp:simpanLinkLampiran");
        } finally {
            StreamingHibernateUtil.getInstance().closeSession();
        }
    }
    private String ambilLinkLampiran(Long ref, String jenis) {
        if (ref == null) return "";
        try {
            LampiranLain lam = LampiranLain.ambil(ref, jenis);
            return lam == null ? "" : nvl(lam.getLink());
        } catch (Exception e) { return ""; }
    }

    /*
     * Pastikan Tbmuser yang login sudah punya baris PesertaKursus (identitas umum di modul kursus).
     * WAJIB commit dalam transaksi sendiri di sini -- sebelumnya save()+flush() tanpa
     * beginTransaction()/commit() membuat baris PesertaKursus baru TIDAK pernah benar-benar
     * tersimpan (rollback diam-diam saat session ditutup), padahal objeknya sudah terlanjur
     * punya id hasil generate IDENTITY. Akibatnya insert ProdukKursus.instruktur yang mengacu
     * ke id "hantu" itu gagal FK constraint (produk_kursus_instruktur_fkey / peserta_kursus).
     */
    private PesertaKursus ensurePesertaKursus(Session db, Tbmuser tbmuser) throws Exception {
        if (tbmuser == null) return null;
        PesertaKursus p = tbmuser.getPesertaKursus();
        if (p != null && p.getId() != null) return p;
        p = (PesertaKursus) db.createCriteria(PesertaKursus.class).add(Restrictions.eq("tbmuser", tbmuser))
                .setMaxResults(1).uniqueResult();
        if (p == null) {
            p = new PesertaKursus();
            p.setTbmuser(tbmuser);
            p.setNama(empty(tbmuser.getUserNama()) ? tbmuser.getUserId() : tbmuser.getUserNama());
            p.setEmail(nvl(tbmuser.getEmail()));
            p.setTipePeserta(KursusUtil.UMUM);
            p.setJenisPeserta(KursusUtil.ANGGOTA_REGULER);
            p.setJenisIdentitasPeserta(KursusUtil.EMAIL);
            p.setAktif(true);
            Transaction pesertaTx = db.beginTransaction();
            try {
                db.save(p);
                db.flush();
                pesertaTx.commit();
            } catch (Exception e) {
                try { if (pesertaTx.isActive()) pesertaTx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp:ensurePesertaRollback"); }
                throw e;
            }
        }
        Transaction linkTx = null;
        try {
            tbmuser.setPesertaKursus(p);
            linkTx = db.beginTransaction();
            db.update(tbmuser);
            linkTx.commit();
        } catch (Exception e) {
            try { if (linkTx != null && linkTx.isActive()) linkTx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp:ensurePesertaLinkRollback"); }
            ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp:ensurePeserta");
        }
        return p;
    }

    /*
     * Cari role "Peserta Kursus" langsung dari DB (jangan andalkan static
     * ConstantValues.rolePesertaKursusPerpustakaan begitu saja) -- object static itu bisa
     * null/detached dari Session JSP yang aktif saat ini, pola sama seperti
     * ensureRoleCalonPegawai() di webapp/WEB-INF/baru/modul/karir/_karir_service.jsp:203-228.
     * userrole adalah FK NOT NULL di tbmuser, jadi harus dipastikan ada sebelum db.save(tbmuser).
     */
    private Tbmrole ensureRolePesertaKursusUmum(Session db) throws Exception {
        Tbmrole role = null;
        try {
            role = (Tbmrole) db.createCriteria(Tbmrole.class)
                    .add(Restrictions.eq("roleId", Tbmrole.PESERTA_KURSUS)).setMaxResults(1).uniqueResult();
        } catch (Exception e) { role = null; ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp:ensureRolePesertaKursusUmum"); }
        if (role == null) {
            role = new Tbmrole();
            role.setRoleId(Tbmrole.PESERTA_KURSUS);
            role.setRoleName("Peserta Kursus");
            db.save(role);
            db.flush();
        }
        ConstantValues.rolePesertaKursusPerpustakaan = role;
        return role;
    }

    private double ratingRataRata(Session db, ProdukKursus p) {
        try {
            Object avg = db.createCriteria(UlasanKursus.class).add(Restrictions.eq("produkKursus", p))
                    .add(Restrictions.eq("aktif", true))
                    .setProjection(org.hibernate.criterion.Projections.avg("rating")).uniqueResult();
            return avg == null ? 0.0 : ((Number) avg).doubleValue();
        } catch (Exception e) { return 0.0; }
    }
    private int jumlahUlasan(Session db, ProdukKursus p) {
        try {
            Object c = db.createCriteria(UlasanKursus.class).add(Restrictions.eq("produkKursus", p))
                    .add(Restrictions.eq("aktif", true))
                    .setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
            return c == null ? 0 : ((Number) c).intValue();
        } catch (Exception e) { return 0; }
    }
    private int jumlahSiswa(Session db, ProdukKursus p) {
        try {
            Object c = db.createCriteria(PesertaPunyaProdukKursus.class).add(Restrictions.eq("produkKursus", p))
                    .add(Restrictions.eq("status", PesertaPunyaProdukKursus.TERBELI))
                    .setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
            return c == null ? 0 : ((Number) c).intValue();
        } catch (Exception e) { return 0; }
    }

    private JSONObject produkKatalogJson(Session db, ProdukKursus p) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", p.getId());
        o.put("nama", nvl(p.getNama()));
        o.put("keterangan", nvl(p.getKeterangan()));
        o.put("harga", p.getHargaTotal());
        o.put("gratis", p.getGratis());
        o.put("kategori", p.getKategoriProdukKursus() == null ? "" : nvl(p.getKategoriProdukKursus().getNama()));
        o.put("kategoriId", p.getKategoriProdukKursus() == null ? JSONObject.NULL : p.getKategoriProdukKursus().getId());
        o.put("icon", p.getKategoriProdukKursus() == null ? "" : nvl(p.getKategoriProdukKursus().getIcon()));
        o.put("tingkat", p.getTingkatKelasProdukKursus() == null ? "" : nvl(p.getTingkatKelasProdukKursus().getNama()));
        o.put("instruktur", p.getInstruktur() == null ? "" : nvl(p.getInstruktur().getNama()));
        o.put("thumbnail", ambilLinkLampiran(p.getId(), JENIS_THUMBNAIL));
        o.put("rating", Math.round(ratingRataRata(db, p) * 10.0) / 10.0);
        o.put("jumlahUlasan", jumlahUlasan(db, p));
        o.put("jumlahSiswa", jumlahSiswa(db, p));
        return o;
    }

    private JSONObject materiJson(MateriKursus m) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", m.getId());
        o.put("judul", nvl(m.getNama()));
        o.put("keterangan", nvl(m.getKeterangan()));
        o.put("tipeKonten", m.getTipeKonten());
        o.put("durasiMenit", m.getDurasiMenit());
        o.put("urutan", m.getUrutan());
        o.put("preview", m.getPreview());
        o.put("video", ambilLinkLampiran(m.getId(), JENIS_VIDEO_MATERI));
        return o;
    }
    private JSONObject progressJson(ProgressMateriKursus prog) throws Exception {
        JSONObject o = new JSONObject();
        o.put("selesai", prog != null && Boolean.TRUE.equals(prog.getSelesai()));
        o.put("persentase", prog == null ? 0 : prog.getPersentase());
        o.put("detikVideoTerakhir", prog == null ? 0 : prog.getDetikVideoTerakhir());
        o.put("durasiDitonton", prog == null ? 0 : prog.getDurasiDitonton());
        o.put("jumlahAkses", prog == null ? 0 : prog.getJumlahAkses());
        return o;
    }
    /* =================== SERTIFIKAT (§15) =================== */
    private JSONObject sertifikatJson(SertifikatKursus s) throws Exception {
        JSONObject o = new JSONObject();
        if (s == null) { o.put("ada", false); return o; }
        o.put("ada", true);
        o.put("nomorSertifikat", nvl(s.getNomorSertifikat()));
        o.put("kodeVerifikasi", nvl(s.getKode()));
        o.put("tanggalTerbit", Common.dateFormat3.get().format(s.getTanggalTerbit()));
        o.put("nilaiAkhir", s.getNilaiAkhir() == null ? JSONObject.NULL : s.getNilaiAkhir());
        o.put("durasiBelajarMenit", s.getDurasiBelajarMenit());
        o.put("status", s.getStatus());
        return o;
    }

    /**
     * Terbitkan sertifikat sekali (idempoten) begitu SEMUA MateriKursus milik ProdukKursus
     * pada enrollment ini sudah selesai (ProgressMateriKursus.selesai=true). Dipanggil dari
     * setiap titik yang menandai materi selesai (tandai_selesai/upload_tugas/selesai_kuis).
     * Tidak mengubah logika kelulusan yang sudah ada -- murni membaca progress existing.
     */
    private void cekDanTerbitkanSertifikat(Session db, PesertaPunyaProdukKursus enr) throws Exception {
        if (enr == null || !PesertaPunyaProdukKursus.TERBELI.equals(enr.getStatus())) return;
        SertifikatKursus existing = (SertifikatKursus) db.createCriteria(SertifikatKursus.class)
                .add(Restrictions.eq("pesertaPunyaProdukKursus", enr)).setMaxResults(1).uniqueResult();
        if (existing != null) return;

        List seksiList = db.createCriteria(SeksiKursus.class)
                .add(Restrictions.eq("produkKursus", enr.getProdukKursus())).list();
        List semuaMateri = new ArrayList();
        for (int i = 0; i < seksiList.size(); i++) {
            semuaMateri.addAll(db.createCriteria(MateriKursus.class)
                    .add(Restrictions.eq("seksiKursus", (SeksiKursus) seksiList.get(i))).list());
        }
        if (semuaMateri.isEmpty()) return;

        double totalSkor = 0; int jumlahSkor = 0; int totalDetikDitonton = 0;
        for (int i = 0; i < semuaMateri.size(); i++) {
            MateriKursus m = (MateriKursus) semuaMateri.get(i);
            ProgressMateriKursus prog = (ProgressMateriKursus) db.createCriteria(ProgressMateriKursus.class)
                    .add(Restrictions.eq("pesertaPunyaProdukKursus", enr)).add(Restrictions.eq("materiKursus", m))
                    .setMaxResults(1).uniqueResult();
            if (prog == null || !Boolean.TRUE.equals(prog.getSelesai())) return;
            totalDetikDitonton += prog.getDurasiDitonton();
            if (MateriKursus.QUIZ.equals(m.getTipeKonten())) {
                PercobaanKuisKursus terbaik = (PercobaanKuisKursus) db.createCriteria(PercobaanKuisKursus.class)
                        .add(Restrictions.eq("pesertaPunyaProdukKursus", enr)).add(Restrictions.eq("materiKursus", m))
                        .addOrder(Order.desc("totalNilai")).setMaxResults(1).uniqueResult();
                if (terbaik != null) { totalSkor += terbaik.getTotalNilai(); jumlahSkor++; }
            }
        }

        Transaction tx2 = db.beginTransaction();
        SertifikatKursus sert = new SertifikatKursus();
        sert.setPesertaPunyaProdukKursus(enr);
        sert.setTanggalTerbit(new Date());
        sert.setDurasiBelajarMenit(totalDetikDitonton / 60);
        sert.setNilaiAkhir(jumlahSkor == 0 ? null : (Double) (totalSkor / jumlahSkor));
        sert.setStatus(SertifikatKursus.AKTIF);
        db.save(sert);
        db.flush();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        sert.setNomorSertifikat("SERT/" + cal.get(java.util.Calendar.YEAR) + "/" + String.format("%06d", sert.getId()));
        db.update(sert);
        tx2.commit();
    }
    private JSONObject pengumpulanJson(PengumpulanTugasKursus t) throws Exception {
        JSONObject o = new JSONObject();
        if (t == null) { o.put("ada", false); return o; }
        o.put("ada", true);
        o.put("id", t.getId());
        o.put("namaFile", nvl(t.getNamaFile()));
        o.put("link", nvl(t.getLink()));
        o.put("waktuKumpul", Common.dateFormat3.get().format(t.getWaktuKumpul()));
        o.put("status", t.getStatus());
        o.put("nilai", t.getNilai() == null ? JSONObject.NULL : t.getNilai());
        o.put("catatanPenilaian", nvl(t.getCatatanPenilaian()));
        return o;
    }

    /* Materi bertipe Quiz dimiliki instruktur mana -- dipakai utk cek hak akses kelola soal. */
    private boolean pemilikMateriKuis(MateriKursus m, PesertaKursus peserta) {
        return m != null && peserta != null && m.getSeksiKursus() != null && m.getSeksiKursus().getProdukKursus() != null
                && m.getSeksiKursus().getProdukKursus().getInstruktur() != null
                && m.getSeksiKursus().getProdukKursus().getInstruktur().getId().equals(peserta.getId());
    }

    /* Soal + kunci jawaban -- HANYA utk instruktur mengelola bank soal. */
    private JSONObject soalJsonInstruktur(Session db, BankSoal soal) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", soal.getId());
        o.put("soal", nvl(soal.getSoal()));
        o.put("jenisKoreksi", soal.getJenisKoreksi());
        o.put("jenisPilihanGanda", soal.getJenisPilihanGanda());
        o.put("skor", soal.getSkor());
        o.put("skorSalah", soal.getSkorSalah());
        JSONArray pilihanArr = new JSONArray();
        List details = db.createCriteria(BankSoalDetail.class).add(Restrictions.eq("bankSoal", soal))
                .addOrder(Order.asc("huruf")).list();
        for (int i = 0; i < details.size(); i++) {
            BankSoalDetail d = (BankSoalDetail) details.get(i);
            JSONObject po = new JSONObject();
            po.put("id", d.getId());
            po.put("huruf", nvl(d.getHuruf()));
            po.put("jawaban", nvl(d.getJawaban()));
            po.put("betul", d.getBetul());
            pilihanArr.put(po);
        }
        o.put("pilihan", pilihanArr);
        return o;
    }

    /* Soal TANPA kunci jawaban -- dikirim ke peserta saat mengerjakan kuis. */
    private JSONObject soalJsonPeserta(Session db, BankSoal soal, boolean acakJawaban) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", soal.getId());
        o.put("soal", nvl(soal.getSoal()));
        o.put("jenisKoreksi", soal.getJenisKoreksi());
        if (PenjelasanBankSoal.KOREKSI_OTOMATIS.equals(soal.getJenisKoreksi())) {
            List details = db.createCriteria(BankSoalDetail.class).add(Restrictions.eq("bankSoal", soal))
                    .addOrder(Order.asc("huruf")).list();
            if (acakJawaban) Collections.shuffle(details);
            JSONArray pilihanArr = new JSONArray();
            for (int i = 0; i < details.size(); i++) {
                BankSoalDetail d = (BankSoalDetail) details.get(i);
                JSONObject po = new JSONObject();
                po.put("id", d.getId());
                po.put("huruf", nvl(d.getHuruf()));
                po.put("jawaban", nvl(d.getJawaban()));
                pilihanArr.put(po);
            }
            o.put("pilihan", pilihanArr);
        }
        return o;
    }

    /* Pastikan MateriKursus (tipe Quiz) sudah punya Ujian sbg wadah -- buat baru bila belum ada. */
    private Ujian ambilAtauBuatUjianMateri(Session db, MateriKursus m) throws Exception {
        Ujian u = m.getUjian();
        if (u != null && u.getId() != null) return u;
        u = new Ujian();
        u.setNama("Kuis - " + m.getNama());
        u.setJenisKoreksi(PenjelasanBankSoal.KOREKSI_OTOMATIS);
        db.save(u);
        db.flush();
        m.setUjian(u);
        db.update(m);
        return u;
    }

    /*
     * Hitung ulang totalNilai/lulus/jumlahBenar satu PercobaanKuisKursus berdasarkan seluruh
     * JawabanPercobaanKuisKursus miliknya. Dipanggil setelah peserta menyelesaikan kuis, dan
     * setelah instruktur menilai jawaban esai (supaya nilai esai yg baru dikoreksi ikut terhitung).
     */
    private void recomputePercobaan(Session db, PercobaanKuisKursus p) throws Exception {
        List jwbList = db.createCriteria(JawabanPercobaanKuisKursus.class)
                .add(Restrictions.eq("percobaanKuisKursus", p)).list();
        double totalSkor = 0.0, totalMaks = 0.0;
        int jumlahBenar = 0;
        for (int i = 0; i < jwbList.size(); i++) {
            JawabanPercobaanKuisKursus j = (JawabanPercobaanKuisKursus) jwbList.get(i);
            totalSkor += j.getSkor();
            totalMaks += j.getBankSoal().getSkor();
            if (Boolean.TRUE.equals(j.getBenar())) jumlahBenar++;
        }
        double persen = totalMaks <= 0.0 ? 0.0 : Math.round((totalSkor / totalMaks) * 10000.0) / 100.0;
        Ujian u = p.getMateriKursus().getUjian();
        Transaction tx2 = db.beginTransaction();
        p.setTotalNilai(persen);
        p.setJumlahBenar(jumlahBenar);
        p.setLulus(u != null && persen >= u.getNilaiLulus());
        db.update(p);
        tx2.commit();
    }
%>
<%
response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
response.setHeader("Pragma", "no-cache");
response.setDateHeader("Expires", 0);
JSONObject res = new JSONObject();
Session db = null;
Transaction tx = null;
try {
    String aksi = request.getParameter("action");
    if (aksi == null) aksi = "";
    db = HibernateUtil.getSessionFactory().openSession();
    Tbmuser tbmuser = Common.getCurrentUser(request);

    if ("list_kategori".equals(aksi)) {
        JSONArray arr = new JSONArray();
        List list = db.createCriteria(KategoriProdukKursus.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.asc("nama")).list();
        for (int i = 0; i < list.size(); i++) {
            KategoriProdukKursus k = (KategoriProdukKursus) list.get(i);
            JSONObject o = new JSONObject();
            o.put("id", k.getId());
            o.put("nama", nvl(k.getNama()));
            o.put("icon", nvl(k.getIcon()));
            arr.put(o);
        }
        res.put("status", "success");
        res.put("data", arr);
    }
    else if ("list_tingkat".equals(aksi)) {
        JSONArray arr = new JSONArray();
        List list = db.createCriteria(TingkatKelasProdukKursus.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.asc("nama")).list();
        for (int i = 0; i < list.size(); i++) {
            TingkatKelasProdukKursus t = (TingkatKelasProdukKursus) list.get(i);
            JSONObject o = new JSONObject();
            o.put("id", t.getId());
            o.put("nama", nvl(t.getNama()));
            arr.put(o);
        }
        res.put("status", "success");
        res.put("data", arr);
    }
    else if ("daftar_member_baru".equals(aksi)) {
        /*
         * Pendaftaran mandiri utk siapapun yang BUKAN mahasiswa/siswa/dosen/guru/pegawai/
         * anggota koperasi (identitas-identitas itu sudah otomatis punya Tbmuser lewat sinkronisasi
         * data institusi masing-masing). Member baru di sini bisa berperan sebagai peserta MAUPUN
         * kreator/instruktur kursus -- begitu Tbmuser dibuat, ensurePesertaKursus() (dipanggil di
         * bawah) otomatis membuatkan baris PesertaKursus ber-tipePeserta UMUM, dan tidak ada gate
         * peran terpisah di aksi simpan_produk/beli_kursus utk membedakan "peserta" vs "kreator".
         */
        String nama = nvl(request.getParameter("nama"));
        String email = nvl(request.getParameter("email"));
        String userId = nvl(request.getParameter("userId"));
        String password = nvl(request.getParameter("password"));
        String hp = nvl(request.getParameter("hp"));

        if (empty(nama)) { res.put("status", "error"); res.put("message", "Nama lengkap harus diisi."); }
        else if (empty(email) || !Common.isValidEmailAddress(email)) { res.put("status", "error"); res.put("message", "Format email tidak valid."); }
        else if (empty(userId) || userId.length() < 4) { res.put("status", "error"); res.put("message", "Username minimal 4 karakter."); }
        else if (password.length() < 6) { res.put("status", "error"); res.put("message", "Kata sandi minimal 6 karakter."); }
        else if (Boolean.TRUE.equals(Common.checkUsername(userId, null, null))) {
            res.put("status", "error"); res.put("message", "Username \"" + userId + "\" sudah dipakai, silakan pilih username lain.");
        } else {
            boolean emailDipakai = ((Number) db.createCriteria(Tbmuser.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.eq("email", email))
                    .setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult()).intValue() > 0;
            if (emailDipakai) {
                res.put("status", "error"); res.put("message", "Email \"" + email + "\" sudah terdaftar. Silakan masuk atau gunakan email lain.");
            } else {
                /*
                 * userrole & userPassword WAJIB diisi SEBELUM db.save() -- pola sama seperti
                 * createOrResetCandidateUser() di _karir_service.jsp (komentar baris ~256-264 di sana):
                 * kolom userrole NOT NULL, insert gagal kalau diisi belakangan setelah save().
                 */
                Tbmrole role = ensureRolePesertaKursusUmum(db);
                Tbmuser baru = new Tbmuser();
                baru.setUserId(userId);
                baru.setUserRole(role);
                baru.setRoot(true);
                baru.setUserShow(1);
                baru.setIs_encripted(true);
                baru.setAktif(true);
                baru.setUserPassword(Common.desEncrypter.get().encrypt(password));
                baru.setEmail(email);
                baru.setNama(nama);
                baru.setUserNama(nama);
                baru.setHp(hp);

                tx = db.beginTransaction();
                db.save(baru);
                db.flush();
                tx.commit(); tx = null;

                ensurePesertaKursus(db, baru);

                res.put("status", "success");
                res.put("message", "Pendaftaran berhasil. Silakan masuk dengan username dan kata sandi yang baru Anda buat.");
            }
        }
    }
    else if ("list_katalog".equals(aksi)) {
        String q = nvl(request.getParameter("q"));
        Long kategoriId = longParam(request, "kategoriId");
        Long tingkatId = longParam(request, "tingkatId");
        String hargaFilter = nvl(request.getParameter("harga")); // "" | "gratis" | "berbayar"

        org.hibernate.Criteria crit = db.createCriteria(ProdukKursus.class)
                .add(Restrictions.eq("status", ProdukKursus.PUBLISHED));
        if (!q.isEmpty()) {
            crit.add(Restrictions.or(Restrictions.ilike("nama", q, MatchMode.ANYWHERE),
                    Restrictions.ilike("keterangan", q, MatchMode.ANYWHERE)));
        }
        if (kategoriId != null) {
            crit.createAlias("kategoriProdukKursus", "kk").add(Restrictions.eq("kk.id", kategoriId));
        }
        if (tingkatId != null) {
            crit.createAlias("tingkatKelasProdukKursus", "tt").add(Restrictions.eq("tt.id", tingkatId));
        }
        if ("gratis".equals(hargaFilter)) {
            crit.add(Restrictions.eq("gratis", true));
        } else if ("berbayar".equals(hargaFilter)) {
            crit.add(Restrictions.or(Restrictions.isNull("gratis"), Restrictions.eq("gratis", false)));
        }
        crit.addOrder(Order.desc("id")).setMaxResults(200);

        List list = crit.list();
        JSONArray arr = new JSONArray();
        for (int i = 0; i < list.size(); i++) {
            arr.put(produkKatalogJson(db, (ProdukKursus) list.get(i)));
        }
        res.put("status", "success");
        res.put("data", arr);
    }
    else if ("get_produk".equals(aksi)) {
        Long id = longParam(request, "id");
        ProdukKursus p = id == null ? null : (ProdukKursus) db.get(ProdukKursus.class, id);
        if (p == null) {
            res.put("status", "error"); res.put("message", "Kursus tidak ditemukan.");
        } else {
            JSONObject o = produkKatalogJson(db, p);
            o.put("deskripsi", nvl(p.getDeskripsi()));

            PesertaKursus peserta = tbmuser == null ? null : ensurePesertaKursus(db, tbmuser);
            boolean isOwner = peserta != null && p.getInstruktur() != null
                    && peserta.getId().equals(p.getInstruktur().getId());
            o.put("isOwner", isOwner);

            String statusBeli = "";
            if (peserta != null) {
                PesertaPunyaProdukKursus enr = (PesertaPunyaProdukKursus) db.createCriteria(PesertaPunyaProdukKursus.class)
                        .add(Restrictions.eq("pesertaKursus", peserta)).add(Restrictions.eq("produkKursus", p))
                        .addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
                statusBeli = enr == null ? "" : enr.getStatus();
                o.put("enrollmentId", enr == null ? JSONObject.NULL : enr.getId());
            }
            o.put("statusBeli", statusBeli);

            JSONArray seksiArr = new JSONArray();
            List seksiList = db.createCriteria(SeksiKursus.class).add(Restrictions.eq("produkKursus", p))
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .addOrder(Order.asc("urutan")).addOrder(Order.asc("id")).list();
            for (int i = 0; i < seksiList.size(); i++) {
                SeksiKursus s = (SeksiKursus) seksiList.get(i);
                JSONObject so = new JSONObject();
                so.put("id", s.getId());
                so.put("judul", nvl(s.getNama()));
                so.put("urutan", s.getUrutan());
                JSONArray materiArr = new JSONArray();
                List materiList = db.createCriteria(MateriKursus.class).add(Restrictions.eq("seksiKursus", s))
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                        .addOrder(Order.asc("urutan")).addOrder(Order.asc("id")).list();
                for (int j = 0; j < materiList.size(); j++) {
                    materiArr.put(materiJson((MateriKursus) materiList.get(j)));
                }
                so.put("materi", materiArr);
                seksiArr.put(so);
            }
            o.put("kurikulum", seksiArr);

            JSONArray ulasanArr = new JSONArray();
            List ulasanList = db.createCriteria(UlasanKursus.class).add(Restrictions.eq("produkKursus", p))
                    .add(Restrictions.eq("aktif", true)).addOrder(Order.desc("tanggal")).setMaxResults(50).list();
            for (int i = 0; i < ulasanList.size(); i++) {
                UlasanKursus u = (UlasanKursus) ulasanList.get(i);
                JSONObject uo = new JSONObject();
                uo.put("nama", u.getPesertaKursus() == null ? "" : nvl(u.getPesertaKursus().getNama()));
                uo.put("rating", u.getRating());
                uo.put("komentar", nvl(u.getKomentar()));
                uo.put("tanggal", Common.dateFormat.get().format(u.getTanggal()));
                ulasanArr.put(uo);
            }
            o.put("ulasan", ulasanArr);

            res.put("status", "success");
            res.put("data", o);
        }
    }
    else if ("list_kursus_saya_belajar".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            List list = db.createCriteria(PesertaPunyaProdukKursus.class).add(Restrictions.eq("pesertaKursus", peserta))
                    .addOrder(Order.desc("id")).list();
            JSONArray arr = new JSONArray();
            for (int i = 0; i < list.size(); i++) {
                PesertaPunyaProdukKursus e = (PesertaPunyaProdukKursus) list.get(i);
                ProdukKursus p = e.getProdukKursus();
                JSONObject o = new JSONObject();
                o.put("enrollmentId", e.getId());
                o.put("produkKursusId", p == null ? JSONObject.NULL : p.getId());
                o.put("nama", p == null ? "" : nvl(p.getNama()));
                o.put("kategori", p == null || p.getKategoriProdukKursus() == null ? "" : nvl(p.getKategoriProdukKursus().getNama()));
                o.put("status", e.getStatus());
                o.put("thumbnail", p == null ? "" : ambilLinkLampiran(p.getId(), JENIS_THUMBNAIL));
                arr.put(o);
            }
            res.put("status", "success");
            res.put("data", arr);
        }
    }
    else if ("list_kursus_saya_instruktur".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            List list = db.createCriteria(ProdukKursus.class).add(Restrictions.eq("instruktur", peserta))
                    .addOrder(Order.desc("id")).list();
            JSONArray arr = new JSONArray();
            int draft = 0, pending = 0, published = 0, totalSiswa = 0;
            double pendapatan = 0.0;
            for (int i = 0; i < list.size(); i++) {
                ProdukKursus p = (ProdukKursus) list.get(i);
                JSONObject o = new JSONObject();
                o.put("id", p.getId());
                o.put("nama", nvl(p.getNama()));
                o.put("status", p.getStatus());
                o.put("gratis", p.getGratis());
                o.put("harga", p.getHargaTotal());
                o.put("thumbnail", ambilLinkLampiran(p.getId(), JENIS_THUMBNAIL));
                int js = jumlahSiswa(db, p);
                o.put("jumlahSiswa", js);
                arr.put(o);

                if (ProdukKursus.DRAFT.equals(p.getStatus()) || ProdukKursus.REJECTED.equals(p.getStatus())) draft++;
                else if (ProdukKursus.PENDING_REVIEW.equals(p.getStatus())) pending++;
                else if (ProdukKursus.PUBLISHED.equals(p.getStatus())) published++;
                totalSiswa += js;
                if (!p.getGratis()) pendapatan += js * p.getHargaTotal();
            }
            JSONObject stat = new JSONObject();
            stat.put("draft", draft); stat.put("pending", pending); stat.put("published", published);
            stat.put("totalSiswa", totalSiswa); stat.put("pendapatan", pendapatan);
            res.put("status", "success");
            res.put("data", arr);
            res.put("stat", stat);
        }
    }
    else if ("simpan_produk".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            String nama = nvl(request.getParameter("nama"));
            if (empty(nama)) { res.put("status", "error"); res.put("message", "Judul kursus harus diisi."); }
            else {
                PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
                Long id = longParam(request, "id");
                ProdukKursus p = id == null ? null : (ProdukKursus) db.get(ProdukKursus.class, id);
                if (p != null && (p.getInstruktur() == null || !p.getInstruktur().getId().equals(peserta.getId()))) {
                    res.put("status", "error"); res.put("message", "Anda tidak berhak mengubah kursus ini.");
                } else {
                    boolean baru = (p == null);
                    if (baru) { p = new ProdukKursus(); p.setInstruktur(peserta); p.setStatus(ProdukKursus.DRAFT); }
                    p.setNama(nama);
                    p.setKeterangan(nvl(request.getParameter("keterangan")));
                    p.setDeskripsi(nvl(request.getParameter("deskripsi")));
                    p.setGratis(boolParam(request, "gratis", false));
                    p.setHargaTotal(p.getGratis() ? 0.0 : numParam(request, "harga", 0.0));
                    Long kategoriId = longParam(request, "kategoriId");
                    if (kategoriId != null) p.setKategoriProdukKursus((KategoriProdukKursus) db.get(KategoriProdukKursus.class, kategoriId));
                    Long tingkatId = longParam(request, "tingkatId");
                    if (tingkatId != null) p.setTingkatKelasProdukKursus((TingkatKelasProdukKursus) db.get(TingkatKelasProdukKursus.class, tingkatId));
                    if (!ProdukKursus.PUBLISHED.equals(p.getStatus())) {
                        // Ubahan pada kursus yang sebelumnya ditolak akan diajukan ulang lewat aksi ajukan_publikasi.
                        if (ProdukKursus.REJECTED.equals(p.getStatus())) p.setStatus(ProdukKursus.DRAFT);
                    }
                    tx = db.beginTransaction();
                    db.saveOrUpdate(p);
                    tx.commit(); tx = null;
                    res.put("status", "success");
                    res.put("id", p.getId());
                    res.put("message", baru ? "Draft kursus berhasil dibuat." : "Kursus berhasil diperbarui.");
                }
            }
        }
    }
    else if ("upload_thumbnail".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Map multipartParams = new HashMap();
            Map files = parseMultipartFiles(request, multipartParams);
            Long id = longParam(request, "id");
            if (id == null) { try { id = Long.valueOf(reqParam(request, multipartParams, "id")); } catch (Exception e) { id = null; } }
            ProdukKursus p = id == null ? null : (ProdukKursus) db.get(ProdukKursus.class, id);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            if (p == null || p.getInstruktur() == null || !p.getInstruktur().getId().equals(peserta.getId())) {
                res.put("status", "error"); res.put("message", "Kursus tidak valid.");
            } else {
                FileItem fileItem = (FileItem) files.get("file");
                String url = simpanFileKeDisk("kursus_thumbnail", p.getId(), fileItem);
                if (empty(url)) { res.put("status", "error"); res.put("message", "File thumbnail tidak ditemukan."); }
                else {
                    simpanLinkLampiran(p.getId(), JENIS_THUMBNAIL, url, cleanFilename(fileItem.getName()));
                    res.put("status", "success"); res.put("url", url);
                }
            }
        }
    }
    else if ("ajukan_publikasi".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long id = longParam(request, "id");
            ProdukKursus p = id == null ? null : (ProdukKursus) db.get(ProdukKursus.class, id);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            if (p == null || p.getInstruktur() == null || !p.getInstruktur().getId().equals(peserta.getId())) {
                res.put("status", "error"); res.put("message", "Kursus tidak valid.");
            } else {
                int jumlahMateri = 0;
                List seksiList = db.createCriteria(SeksiKursus.class).add(Restrictions.eq("produkKursus", p)).list();
                for (int i = 0; i < seksiList.size(); i++) {
                    Object c = db.createCriteria(MateriKursus.class)
                            .add(Restrictions.eq("seksiKursus", (SeksiKursus) seksiList.get(i)))
                            .setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
                    jumlahMateri += c == null ? 0 : ((Number) c).intValue();
                }
                if (seksiList.isEmpty() || jumlahMateri == 0) {
                    res.put("status", "error");
                    res.put("message", "Tambahkan minimal 1 section berisi 1 materi sebelum mengajukan publikasi.");
                } else if (!p.getGratis() && p.getHargaTotal() <= 0) {
                    res.put("status", "error");
                    res.put("message", "Kursus berbayar harus memiliki harga lebih dari 0, atau tandai sebagai gratis.");
                } else {
                    tx = db.beginTransaction();
                    p.setStatus(ProdukKursus.PENDING_REVIEW);
                    db.update(p);
                    tx.commit(); tx = null;
                    res.put("status", "success");
                    res.put("message", "Kursus berhasil diajukan untuk ditinjau admin.");
                }
            }
        }
    }
    else if ("hapus_produk".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long id = longParam(request, "id");
            ProdukKursus p = id == null ? null : (ProdukKursus) db.get(ProdukKursus.class, id);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            if (p == null || p.getInstruktur() == null || !p.getInstruktur().getId().equals(peserta.getId())) {
                res.put("status", "error"); res.put("message", "Kursus tidak valid.");
            } else if (!ProdukKursus.DRAFT.equals(p.getStatus()) && !ProdukKursus.REJECTED.equals(p.getStatus())) {
                res.put("status", "error"); res.put("message", "Hanya kursus berstatus Draft/Rejected yang dapat dihapus.");
            } else {
                tx = db.beginTransaction();
                List seksiList = db.createCriteria(SeksiKursus.class).add(Restrictions.eq("produkKursus", p)).list();
                for (int i = 0; i < seksiList.size(); i++) {
                    SeksiKursus s = (SeksiKursus) seksiList.get(i);
                    List materiList = db.createCriteria(MateriKursus.class).add(Restrictions.eq("seksiKursus", s)).list();
                    for (int j = 0; j < materiList.size(); j++) db.delete(materiList.get(j));
                    db.delete(s);
                }
                db.delete(p);
                tx.commit(); tx = null;
                res.put("status", "success"); res.put("message", "Kursus berhasil dihapus.");
            }
        }
    }
    else if ("get_kurikulum".equals(aksi)) {
        Long produkId = longParam(request, "produkKursusId");
        ProdukKursus p = produkId == null ? null : (ProdukKursus) db.get(ProdukKursus.class, produkId);
        PesertaKursus peserta = tbmuser == null ? null : ensurePesertaKursus(db, tbmuser);
        if (p == null || peserta == null || p.getInstruktur() == null || !p.getInstruktur().getId().equals(peserta.getId())) {
            res.put("status", "error"); res.put("message", "Anda tidak berhak melihat kurikulum ini.");
        } else {
            JSONArray seksiArr = new JSONArray();
            List seksiList = db.createCriteria(SeksiKursus.class).add(Restrictions.eq("produkKursus", p))
                    .addOrder(Order.asc("urutan")).addOrder(Order.asc("id")).list();
            for (int i = 0; i < seksiList.size(); i++) {
                SeksiKursus s = (SeksiKursus) seksiList.get(i);
                JSONObject so = new JSONObject();
                so.put("id", s.getId()); so.put("judul", nvl(s.getNama())); so.put("urutan", s.getUrutan());
                JSONArray materiArr = new JSONArray();
                List materiList = db.createCriteria(MateriKursus.class).add(Restrictions.eq("seksiKursus", s))
                        .addOrder(Order.asc("urutan")).addOrder(Order.asc("id")).list();
                for (int j = 0; j < materiList.size(); j++) materiArr.put(materiJson((MateriKursus) materiList.get(j)));
                so.put("materi", materiArr);
                seksiArr.put(so);
            }
            res.put("status", "success"); res.put("data", seksiArr);
        }
    }
    else if ("simpan_seksi".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long produkId = longParam(request, "produkKursusId");
            ProdukKursus p = produkId == null ? null : (ProdukKursus) db.get(ProdukKursus.class, produkId);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            if (p == null || p.getInstruktur() == null || !p.getInstruktur().getId().equals(peserta.getId())) {
                res.put("status", "error"); res.put("message", "Kursus tidak valid.");
            } else if (empty(nvl(request.getParameter("judul")))) {
                res.put("status", "error"); res.put("message", "Judul section harus diisi.");
            } else {
                Long id = longParam(request, "id");
                SeksiKursus s = id == null ? null : (SeksiKursus) db.get(SeksiKursus.class, id);
                boolean sBaru = (s == null);
                if (sBaru) { s = new SeksiKursus(); s.setProdukKursus(p); }
                s.setNama(nvl(request.getParameter("judul")));
                s.setUrutan(intParam(request, "urutan", 0));
                tx = db.beginTransaction();
                db.saveOrUpdate(s);
                tx.commit(); tx = null;
                res.put("status", "success"); res.put("id", s.getId());
            }
        }
    }
    else if ("hapus_seksi".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long id = longParam(request, "id");
            SeksiKursus s = id == null ? null : (SeksiKursus) db.get(SeksiKursus.class, id);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            if (s == null || s.getProdukKursus() == null || s.getProdukKursus().getInstruktur() == null
                    || !s.getProdukKursus().getInstruktur().getId().equals(peserta.getId())) {
                res.put("status", "error"); res.put("message", "Section tidak valid.");
            } else {
                tx = db.beginTransaction();
                List materiList = db.createCriteria(MateriKursus.class).add(Restrictions.eq("seksiKursus", s)).list();
                for (int i = 0; i < materiList.size(); i++) db.delete(materiList.get(i));
                db.delete(s);
                tx.commit(); tx = null;
                res.put("status", "success");
            }
        }
    }
    else if ("simpan_materi".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Map multipartParams = new HashMap();
            Map files = parseMultipartFiles(request, multipartParams);
            Long seksiId = null;
            try { seksiId = Long.valueOf(reqParam(request, multipartParams, "seksiId")); } catch (Exception e) { seksiId = null; }
            SeksiKursus s = seksiId == null ? null : (SeksiKursus) db.get(SeksiKursus.class, seksiId);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            String judul = reqParam(request, multipartParams, "judul");
            if (s == null || s.getProdukKursus() == null || s.getProdukKursus().getInstruktur() == null
                    || !s.getProdukKursus().getInstruktur().getId().equals(peserta.getId())) {
                res.put("status", "error"); res.put("message", "Section tidak valid.");
            } else if (empty(judul)) {
                res.put("status", "error"); res.put("message", "Judul materi harus diisi.");
            } else {
                Long id = null;
                try { id = Long.valueOf(reqParam(request, multipartParams, "id")); } catch (Exception e) { id = null; }
                MateriKursus m = id == null ? null : (MateriKursus) db.get(MateriKursus.class, id);
                boolean mBaru = (m == null);
                if (mBaru) { m = new MateriKursus(); m.setSeksiKursus(s); }
                m.setNama(judul);
                m.setKeterangan(reqParam(request, multipartParams, "keterangan"));
                m.setTipeKonten(reqParam(request, multipartParams, "tipeKonten"));
                try { m.setDurasiMenit(Integer.valueOf(reqParam(request, multipartParams, "durasiMenit"))); } catch (Exception e) { m.setDurasiMenit(0); }
                try { m.setUrutan(Integer.valueOf(reqParam(request, multipartParams, "urutan"))); } catch (Exception e) { m.setUrutan(0); }
                m.setPreview("true".equalsIgnoreCase(reqParam(request, multipartParams, "preview")) || "1".equals(reqParam(request, multipartParams, "preview")));
                tx = db.beginTransaction();
                db.saveOrUpdate(m);
                tx.commit(); tx = null;

                FileItem fileItem = (FileItem) files.get("file");
                if (fileItem != null && fileItem.getSize() > 0) {
                    String url = simpanFileKeDisk("kursus_materi", m.getId(), fileItem);
                    if (!empty(url)) simpanLinkLampiran(m.getId(), JENIS_VIDEO_MATERI, url, cleanFilename(fileItem.getName()));
                }
                res.put("status", "success"); res.put("id", m.getId());
            }
        }
    }
    else if ("hapus_materi".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long id = longParam(request, "id");
            MateriKursus m = id == null ? null : (MateriKursus) db.get(MateriKursus.class, id);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            if (m == null || m.getSeksiKursus() == null || m.getSeksiKursus().getProdukKursus() == null
                    || m.getSeksiKursus().getProdukKursus().getInstruktur() == null
                    || !m.getSeksiKursus().getProdukKursus().getInstruktur().getId().equals(peserta.getId())) {
                res.put("status", "error"); res.put("message", "Materi tidak valid.");
            } else {
                tx = db.beginTransaction();
                db.delete(m);
                tx.commit(); tx = null;
                res.put("status", "success");
            }
        }
    }
    else if ("terapkan_kupon".equals(aksi)) {
        Long produkId = longParam(request, "produkKursusId");
        String kode = nvl(request.getParameter("kode")).toUpperCase();
        ProdukKursus p = produkId == null ? null : (ProdukKursus) db.get(ProdukKursus.class, produkId);
        if (p == null) { res.put("status", "error"); res.put("message", "Kursus tidak ditemukan."); }
        else {
            KuponKursus k = (KuponKursus) db.createCriteria(KuponKursus.class).add(Restrictions.eq("kode", kode))
                    .setMaxResults(1).uniqueResult();
            if (k == null || !k.berlakuUntuk(new Date())
                    || (k.getProdukKursus() != null && !k.getProdukKursus().getId().equals(p.getId()))) {
                res.put("status", "error"); res.put("message", "Kode kupon tidak valid atau sudah tidak berlaku.");
            } else {
                double harga = p.getHargaTotal();
                double diskon = KuponKursus.PERSEN.equals(k.getTipeDiskon()) ? harga * (k.getNilai() / 100.0) : k.getNilai();
                if (diskon > harga) diskon = harga;
                double total = Math.max(0, harga - diskon);
                res.put("status", "success");
                res.put("diskon", diskon);
                res.put("total", total);
                res.put("kuponId", k.getId());
            }
        }
    }
    else if ("beli_kursus".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long produkId = longParam(request, "produkKursusId");
            String kodeKupon = nvl(request.getParameter("kodeKupon")).toUpperCase();
            ProdukKursus p = produkId == null ? null : (ProdukKursus) db.get(ProdukKursus.class, produkId);
            if (p == null || !ProdukKursus.PUBLISHED.equals(p.getStatus())) {
                res.put("status", "error"); res.put("message", "Kursus tidak tersedia untuk dibeli.");
            } else {
                PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
                PesertaPunyaProdukKursus existing = (PesertaPunyaProdukKursus) db.createCriteria(PesertaPunyaProdukKursus.class)
                        .add(Restrictions.eq("pesertaKursus", peserta)).add(Restrictions.eq("produkKursus", p))
                        .add(Restrictions.eq("status", PesertaPunyaProdukKursus.TERBELI))
                        .setMaxResults(1).uniqueResult();
                if (existing != null) {
                    res.put("status", "error"); res.put("message", "Anda sudah memiliki akses ke kursus ini.");
                } else if (p.getGratis()) {
                    tx = db.beginTransaction();
                    PesertaPunyaProdukKursus e = new PesertaPunyaProdukKursus();
                    e.setPesertaKursus(peserta); e.setProdukKursus(p);
                    e.setStatus(PesertaPunyaProdukKursus.TERBELI);
                    e.setWaktuBeli(new Date());
                    e.setHargaDibayar(0.0);
                    db.save(e);
                    ProdukPeserta pp = new ProdukPeserta();
                    pp.setPesertaKursus(peserta); pp.setProdukKursus(p); pp.setPesertaPunyaProdukKursus(e);
                    db.save(pp);
                    tx.commit(); tx = null;
                    res.put("status", "success"); res.put("gratis", true);
                    res.put("message", "Berhasil mendaftar kursus gratis. Selamat belajar!");
                } else {
                    KuponKursus kupon = null;
                    double total = p.getHargaTotal();
                    if (!kodeKupon.isEmpty()) {
                        kupon = (KuponKursus) db.createCriteria(KuponKursus.class).add(Restrictions.eq("kode", kodeKupon))
                                .setMaxResults(1).uniqueResult();
                        if (kupon != null && kupon.berlakuUntuk(new Date())
                                && (kupon.getProdukKursus() == null || kupon.getProdukKursus().getId().equals(p.getId()))) {
                            double diskon = KuponKursus.PERSEN.equals(kupon.getTipeDiskon())
                                    ? total * (kupon.getNilai() / 100.0) : kupon.getNilai();
                            if (diskon > total) diskon = total;
                            total = Math.max(0, total - diskon);
                        } else {
                            kupon = null;
                        }
                    }

                    tx = db.beginTransaction();
                    PesertaPunyaProdukKursus e = new PesertaPunyaProdukKursus();
                    e.setPesertaKursus(peserta); e.setProdukKursus(p);
                    e.setStatus(PesertaPunyaProdukKursus.PESAN);
                    e.setWaktuBeli(new Date());
                    e.setHargaDibayar(total);
                    if (kupon != null) e.setKuponKursus(kupon);
                    db.save(e);
                    if (kupon != null) {
                        kupon.setJumlahDipakai((kupon.getJumlahDipakai() == null ? 0 : kupon.getJumlahDipakai()) + 1);
                        db.update(kupon);
                    }
                    tx.commit(); tx = null;

                    if (total <= 0.1) {
                        tx = db.beginTransaction();
                        e.setStatus(PesertaPunyaProdukKursus.TERBELI);
                        db.update(e);
                        ProdukPeserta pp = new ProdukPeserta();
                        pp.setPesertaKursus(peserta); pp.setProdukKursus(p); pp.setPesertaPunyaProdukKursus(e);
                        db.save(pp);
                        tx.commit(); tx = null;
                        res.put("status", "success"); res.put("gratis", true);
                        res.put("message", "Kupon menutup seluruh biaya. Selamat belajar!");
                    } else {
                        java.util.List<PesertaPunyaProdukKursus> daftarBeli = new java.util.ArrayList<PesertaPunyaProdukKursus>();
                        daftarBeli.add(e);
                        BniRequest bniRequest = null;
                        try {
                            bniRequest = BniCommon.onSaveBni(daftarBeli, total, false);
                        } catch (Exception bniError) {
                            bniError.printStackTrace(); ais.common.ErrorAuditUtil.record(bniError, "auto-audit webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp:beli_kursus_bni");
                        }
                        if (bniRequest == null || empty(bniRequest.getVa())) {
                            res.put("status", "error");
                            res.put("message", "Gagal menghubungi gateway pembayaran BNI. Silakan coba beberapa saat lagi.");
                        } else {
                            res.put("status", "success"); res.put("gratis", false);
                            res.put("enrollmentId", e.getId());
                            res.put("va", bniRequest.getVa());
                            res.put("total", total);
                            res.put("kadaluwarsa", bniRequest.getBillExpired() == null ? "" : Common.dateFormat3.get().format(bniRequest.getBillExpired()));
                        }
                    }
                }
            }
        }
    }
    else if ("cek_status_pesanan".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long id = longParam(request, "enrollmentId");
            PesertaPunyaProdukKursus e = id == null ? null : (PesertaPunyaProdukKursus) db.get(PesertaPunyaProdukKursus.class, id);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            if (e == null || e.getPesertaKursus() == null || !e.getPesertaKursus().getId().equals(peserta.getId())) {
                res.put("status", "error"); res.put("message", "Data pesanan tidak ditemukan.");
            } else {
                res.put("status", "success"); res.put("statusPesanan", e.getStatus());
            }
        }
    }
    else if ("get_pembelajaran".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long produkId = longParam(request, "produkKursusId");
            ProdukKursus p = produkId == null ? null : (ProdukKursus) db.get(ProdukKursus.class, produkId);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            PesertaPunyaProdukKursus enr = p == null ? null : (PesertaPunyaProdukKursus) db.createCriteria(PesertaPunyaProdukKursus.class)
                    .add(Restrictions.eq("pesertaKursus", peserta)).add(Restrictions.eq("produkKursus", p))
                    .add(Restrictions.eq("status", PesertaPunyaProdukKursus.TERBELI))
                    .setMaxResults(1).uniqueResult();
            if (p == null || enr == null) {
                res.put("status", "error"); res.put("message", "Anda belum terdaftar pada kursus ini.");
            } else {
                JSONObject o = new JSONObject();
                o.put("produkKursusId", p.getId());
                o.put("nama", nvl(p.getNama()));
                o.put("enrollmentId", enr.getId());
                JSONArray seksiArr = new JSONArray();
                List seksiList = db.createCriteria(SeksiKursus.class).add(Restrictions.eq("produkKursus", p))
                        .addOrder(Order.asc("urutan")).addOrder(Order.asc("id")).list();
                int totalMateri = 0, selesaiCount = 0;
                for (int i = 0; i < seksiList.size(); i++) {
                    SeksiKursus s = (SeksiKursus) seksiList.get(i);
                    JSONObject so = new JSONObject();
                    so.put("id", s.getId()); so.put("judul", nvl(s.getNama()));
                    JSONArray materiArr = new JSONArray();
                    List materiList = db.createCriteria(MateriKursus.class).add(Restrictions.eq("seksiKursus", s))
                            .addOrder(Order.asc("urutan")).addOrder(Order.asc("id")).list();
                    int seksiTotal = 0, seksiSelesai = 0;
                    for (int j = 0; j < materiList.size(); j++) {
                        MateriKursus m = (MateriKursus) materiList.get(j);
                        JSONObject mo = materiJson(m);
                        ProgressMateriKursus prog = (ProgressMateriKursus) db.createCriteria(ProgressMateriKursus.class)
                                .add(Restrictions.eq("pesertaPunyaProdukKursus", enr)).add(Restrictions.eq("materiKursus", m))
                                .setMaxResults(1).uniqueResult();
                        boolean sudahSelesai = prog != null && prog.getSelesai();
                        mo.put("selesai", sudahSelesai);
                        mo.put("progress", progressJson(prog));
                        if (MateriKursus.TUGAS.equals(m.getTipeKonten())) {
                            PengumpulanTugasKursus t = (PengumpulanTugasKursus) db.createCriteria(PengumpulanTugasKursus.class)
                                    .add(Restrictions.eq("pesertaPunyaProdukKursus", enr)).add(Restrictions.eq("materiKursus", m))
                                    .setMaxResults(1).uniqueResult();
                            mo.put("pengumpulan", pengumpulanJson(t));
                        }
                        materiArr.put(mo);
                        totalMateri++; seksiTotal++;
                        if (sudahSelesai) { selesaiCount++; seksiSelesai++; }
                    }
                    so.put("materi", materiArr);
                    so.put("progressPersen", seksiTotal == 0 ? 0 : Math.round((seksiSelesai * 100.0) / seksiTotal));
                    seksiArr.put(so);
                }
                o.put("kurikulum", seksiArr);
                o.put("progressPersen", totalMateri == 0 ? 0 : Math.round((selesaiCount * 100.0) / totalMateri));
                SertifikatKursus sert = (SertifikatKursus) db.createCriteria(SertifikatKursus.class)
                        .add(Restrictions.eq("pesertaPunyaProdukKursus", enr)).setMaxResults(1).uniqueResult();
                o.put("sertifikat", sertifikatJson(sert));
                res.put("status", "success"); res.put("data", o);
            }
        }
    }
    else if ("tandai_selesai".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long enrollmentId = longParam(request, "enrollmentId");
            Long materiId = longParam(request, "materiKursusId");
            PesertaPunyaProdukKursus enr = enrollmentId == null ? null : (PesertaPunyaProdukKursus) db.get(PesertaPunyaProdukKursus.class, enrollmentId);
            MateriKursus m = materiId == null ? null : (MateriKursus) db.get(MateriKursus.class, materiId);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            if (enr == null || m == null || enr.getPesertaKursus() == null || !enr.getPesertaKursus().getId().equals(peserta.getId())
                    || !PesertaPunyaProdukKursus.TERBELI.equals(enr.getStatus())) {
                res.put("status", "error"); res.put("message", "Data tidak valid.");
            } else if (MateriKursus.QUIZ.equals(m.getTipeKonten()) || MateriKursus.TUGAS.equals(m.getTipeKonten())) {
                // Materi Quiz/Tugas HARUS diselesaikan lewat selesai_kuis (butuh lulus) atau
                // upload_tugas -- bukan jalur generik ini, supaya kelulusan tidak bisa dipalsukan
                // hanya dengan memanggil aksi ini langsung dengan materiKursusId yang diketahui.
                res.put("status", "error"); res.put("message", "Materi tipe ini harus diselesaikan lewat kuis/pengumpulan tugas.");
            } else {
                ProgressMateriKursus prog = (ProgressMateriKursus) db.createCriteria(ProgressMateriKursus.class)
                        .add(Restrictions.eq("pesertaPunyaProdukKursus", enr)).add(Restrictions.eq("materiKursus", m))
                        .setMaxResults(1).uniqueResult();
                tx = db.beginTransaction();
                if (prog == null) { prog = new ProgressMateriKursus(); prog.setPesertaPunyaProdukKursus(enr); prog.setMateriKursus(m); }
                prog.setSelesai(true);
                prog.setWaktuSelesai(new Date());
                db.saveOrUpdate(prog);
                tx.commit(); tx = null;
                cekDanTerbitkanSertifikat(db, enr);
                res.put("status", "success");
            }
        }
    }
    else if ("mulai_lihat_materi".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long enrollmentId = longParam(request, "enrollmentId");
            Long materiId = longParam(request, "materiKursusId");
            PesertaPunyaProdukKursus enr = enrollmentId == null ? null : (PesertaPunyaProdukKursus) db.get(PesertaPunyaProdukKursus.class, enrollmentId);
            MateriKursus m = materiId == null ? null : (MateriKursus) db.get(MateriKursus.class, materiId);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            if (enr == null || m == null || enr.getPesertaKursus() == null || !enr.getPesertaKursus().getId().equals(peserta.getId())
                    || !PesertaPunyaProdukKursus.TERBELI.equals(enr.getStatus())) {
                res.put("status", "error"); res.put("message", "Data tidak valid.");
            } else {
                ProgressMateriKursus prog = (ProgressMateriKursus) db.createCriteria(ProgressMateriKursus.class)
                        .add(Restrictions.eq("pesertaPunyaProdukKursus", enr)).add(Restrictions.eq("materiKursus", m))
                        .setMaxResults(1).uniqueResult();
                tx = db.beginTransaction();
                if (prog == null) { prog = new ProgressMateriKursus(); prog.setPesertaPunyaProdukKursus(enr); prog.setMateriKursus(m); }
                Date sekarang = new Date();
                if (prog.getWaktuMulai() == null) prog.setWaktuMulai(sekarang);
                prog.setWaktuTerakhir(sekarang);
                prog.setJumlahAkses(prog.getJumlahAkses() + 1);
                db.saveOrUpdate(prog);
                tx.commit(); tx = null;
                res.put("status", "success"); res.put("data", progressJson(prog));
            }
        }
    }
    else if ("heartbeat_progress".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long enrollmentId = longParam(request, "enrollmentId");
            Long materiId = longParam(request, "materiKursusId");
            PesertaPunyaProdukKursus enr = enrollmentId == null ? null : (PesertaPunyaProdukKursus) db.get(PesertaPunyaProdukKursus.class, enrollmentId);
            MateriKursus m = materiId == null ? null : (MateriKursus) db.get(MateriKursus.class, materiId);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            if (enr == null || m == null || enr.getPesertaKursus() == null || !enr.getPesertaKursus().getId().equals(peserta.getId())
                    || !PesertaPunyaProdukKursus.TERBELI.equals(enr.getStatus())) {
                res.put("status", "error"); res.put("message", "Data tidak valid.");
            } else {
                // Detik posisi putar & total durasi dilaporkan browser -- TIDAK dipercaya mentah-mentah (poin §13):
                // detik diklem ke rentang wajar [0, batasAtasDetik], dan batas atas dihitung dari durasiMenit
                // yang tersimpan di server (MateriKursus), bukan dari nilai durasi yang dikirim klien.
                int detikDilaporkan = (int) numParam(request, "detikSaatIni", 0.0);
                if (detikDilaporkan < 0) detikDilaporkan = 0;
                int batasAtasDetik = m.getDurasiMenit() != null && m.getDurasiMenit() > 0
                        ? (m.getDurasiMenit() * 60) + 60 /* toleransi 1 menit */ : Integer.MAX_VALUE;
                if (detikDilaporkan > batasAtasDetik) detikDilaporkan = batasAtasDetik;

                ProgressMateriKursus prog = (ProgressMateriKursus) db.createCriteria(ProgressMateriKursus.class)
                        .add(Restrictions.eq("pesertaPunyaProdukKursus", enr)).add(Restrictions.eq("materiKursus", m))
                        .setMaxResults(1).uniqueResult();
                tx = db.beginTransaction();
                if (prog == null) { prog = new ProgressMateriKursus(); prog.setPesertaPunyaProdukKursus(enr); prog.setMateriKursus(m); }
                Date sekarang = new Date();
                if (prog.getWaktuMulai() == null) prog.setWaktuMulai(sekarang);
                prog.setWaktuTerakhir(sekarang);
                prog.setDetikVideoTerakhir(detikDilaporkan);
                if (detikDilaporkan > prog.getDurasiDitonton()) prog.setDurasiDitonton(detikDilaporkan);

                int persentaseBaru;
                if (m.getDurasiMenit() != null && m.getDurasiMenit() > 0) {
                    persentaseBaru = (int) Math.round((prog.getDurasiDitonton() * 100.0) / (m.getDurasiMenit() * 60));
                } else {
                    persentaseBaru = prog.getPersentase();
                }
                if (persentaseBaru > 100) persentaseBaru = 100;
                if (persentaseBaru < 0) persentaseBaru = 0;
                if (persentaseBaru > prog.getPersentase()) prog.setPersentase(persentaseBaru);

                if (!Boolean.TRUE.equals(prog.getSelesai()) && prog.getPersentase() >= 90) {
                    prog.setSelesai(true);
                    prog.setWaktuSelesai(sekarang);
                }
                db.saveOrUpdate(prog);
                tx.commit(); tx = null;
                cekDanTerbitkanSertifikat(db, enr);
                res.put("status", "success"); res.put("data", progressJson(prog));
            }
        }
    }
    else if ("upload_tugas".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Map multipartParams = new HashMap();
            Map files = parseMultipartFiles(request, multipartParams);
            Long enrollmentId = null;
            try { enrollmentId = Long.valueOf(reqParam(request, multipartParams, "enrollmentId")); } catch (Exception e) { enrollmentId = null; }
            Long materiId = null;
            try { materiId = Long.valueOf(reqParam(request, multipartParams, "materiKursusId")); } catch (Exception e) { materiId = null; }
            PesertaPunyaProdukKursus enr = enrollmentId == null ? null : (PesertaPunyaProdukKursus) db.get(PesertaPunyaProdukKursus.class, enrollmentId);
            MateriKursus m = materiId == null ? null : (MateriKursus) db.get(MateriKursus.class, materiId);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            if (enr == null || m == null || enr.getPesertaKursus() == null || !enr.getPesertaKursus().getId().equals(peserta.getId())
                    || !PesertaPunyaProdukKursus.TERBELI.equals(enr.getStatus()) || !MateriKursus.TUGAS.equals(m.getTipeKonten())) {
                res.put("status", "error"); res.put("message", "Data tidak valid.");
            } else {
                FileItem fileItem = (FileItem) files.get("file");
                if (fileItem == null || fileItem.getSize() <= 0) {
                    res.put("status", "error"); res.put("message", "Berkas tugas belum dipilih.");
                } else {
                    String url = simpanFileKeDisk("kursus_tugas", enr.getId() + "_" + m.getId(), fileItem);
                    PengumpulanTugasKursus t = (PengumpulanTugasKursus) db.createCriteria(PengumpulanTugasKursus.class)
                            .add(Restrictions.eq("pesertaPunyaProdukKursus", enr)).add(Restrictions.eq("materiKursus", m))
                            .setMaxResults(1).uniqueResult();
                    boolean tBaru = (t == null);
                    if (tBaru) { t = new PengumpulanTugasKursus(); t.setPesertaPunyaProdukKursus(enr); t.setMateriKursus(m); }
                    t.setNamaFile(cleanFilename(fileItem.getName()));
                    t.setLink(url);
                    t.setWaktuKumpul(new Date());
                    t.setStatus(PengumpulanTugasKursus.DIKUMPULKAN);
                    t.setNilai(null);
                    t.setCatatanPenilaian(null);
                    tx = db.beginTransaction();
                    db.saveOrUpdate(t);
                    tx.commit(); tx = null;

                    ProgressMateriKursus prog = (ProgressMateriKursus) db.createCriteria(ProgressMateriKursus.class)
                            .add(Restrictions.eq("pesertaPunyaProdukKursus", enr)).add(Restrictions.eq("materiKursus", m))
                            .setMaxResults(1).uniqueResult();
                    tx = db.beginTransaction();
                    if (prog == null) { prog = new ProgressMateriKursus(); prog.setPesertaPunyaProdukKursus(enr); prog.setMateriKursus(m); }
                    prog.setSelesai(true);
                    prog.setWaktuSelesai(new Date());
                    db.saveOrUpdate(prog);
                    tx.commit(); tx = null;
                    cekDanTerbitkanSertifikat(db, enr);

                    res.put("status", "success"); res.put("message", "Tugas berhasil dikumpulkan.");
                }
            }
        }
    }
    else if ("hapus_pengumpulan_tugas".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long id = longParam(request, "id");
            PengumpulanTugasKursus t = id == null ? null : (PengumpulanTugasKursus) db.get(PengumpulanTugasKursus.class, id);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            if (t == null || t.getPesertaPunyaProdukKursus() == null || t.getPesertaPunyaProdukKursus().getPesertaKursus() == null
                    || !t.getPesertaPunyaProdukKursus().getPesertaKursus().getId().equals(peserta.getId())) {
                res.put("status", "error"); res.put("message", "Data pengajuan tidak valid.");
            } else {
                PesertaPunyaProdukKursus enr = t.getPesertaPunyaProdukKursus();
                MateriKursus m = t.getMateriKursus();
                tx = db.beginTransaction();
                db.delete(t);
                ProgressMateriKursus prog = (ProgressMateriKursus) db.createCriteria(ProgressMateriKursus.class)
                        .add(Restrictions.eq("pesertaPunyaProdukKursus", enr)).add(Restrictions.eq("materiKursus", m))
                        .setMaxResults(1).uniqueResult();
                if (prog != null) { prog.setSelesai(false); prog.setWaktuSelesai(null); db.update(prog); }
                tx.commit(); tx = null;
                res.put("status", "success"); res.put("message", "Pengajuan tugas berhasil dihapus.");
            }
        }
    }
    else if ("simpan_ulasan".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long produkId = longParam(request, "produkKursusId");
            ProdukKursus p = produkId == null ? null : (ProdukKursus) db.get(ProdukKursus.class, produkId);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            PesertaPunyaProdukKursus enr = p == null ? null : (PesertaPunyaProdukKursus) db.createCriteria(PesertaPunyaProdukKursus.class)
                    .add(Restrictions.eq("pesertaKursus", peserta)).add(Restrictions.eq("produkKursus", p))
                    .add(Restrictions.eq("status", PesertaPunyaProdukKursus.TERBELI))
                    .setMaxResults(1).uniqueResult();
            if (p == null || enr == null) {
                res.put("status", "error"); res.put("message", "Anda hanya dapat memberi ulasan pada kursus yang sudah dibeli.");
            } else {
                UlasanKursus u = (UlasanKursus) db.createCriteria(UlasanKursus.class)
                        .add(Restrictions.eq("produkKursus", p)).add(Restrictions.eq("pesertaKursus", peserta))
                        .setMaxResults(1).uniqueResult();
                boolean uBaru = (u == null);
                if (uBaru) { u = new UlasanKursus(); u.setProdukKursus(p); u.setPesertaKursus(peserta); }
                int rating = intParam(request, "rating", 5);
                if (rating < 1) rating = 1; if (rating > 5) rating = 5;
                u.setRating(rating);
                u.setKomentar(nvl(request.getParameter("komentar")));
                u.setTanggal(new Date());
                u.setAktif(true);
                tx = db.beginTransaction();
                db.saveOrUpdate(u);
                tx.commit(); tx = null;
                res.put("status", "success"); res.put("message", "Terima kasih atas ulasan Anda.");
            }
        }
    }
    else if ("list_ulasan".equals(aksi)) {
        Long produkId = longParam(request, "produkKursusId");
        ProdukKursus p = produkId == null ? null : (ProdukKursus) db.get(ProdukKursus.class, produkId);
        JSONArray arr = new JSONArray();
        if (p != null) {
            List list = db.createCriteria(UlasanKursus.class).add(Restrictions.eq("produkKursus", p))
                    .add(Restrictions.eq("aktif", true)).addOrder(Order.desc("tanggal")).setMaxResults(50).list();
            for (int i = 0; i < list.size(); i++) {
                UlasanKursus u = (UlasanKursus) list.get(i);
                JSONObject o = new JSONObject();
                o.put("nama", u.getPesertaKursus() == null ? "" : nvl(u.getPesertaKursus().getNama()));
                o.put("rating", u.getRating());
                o.put("komentar", nvl(u.getKomentar()));
                o.put("tanggal", Common.dateFormat.get().format(u.getTanggal()));
                arr.put(o);
            }
        }
        res.put("status", "success"); res.put("data", arr);
    }
    /* =================== KUIS -- INSTRUKTUR: KELOLA SOAL =================== */
    else if ("get_soal_kuis".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long materiId = longParam(request, "materiKursusId");
            MateriKursus m = materiId == null ? null : (MateriKursus) db.get(MateriKursus.class, materiId);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            if (!pemilikMateriKuis(m, peserta)) {
                res.put("status", "error"); res.put("message", "Materi tidak valid.");
            } else {
                JSONObject o = new JSONObject();
                Ujian u = m.getUjian();
                o.put("nilaiLulus", u == null ? 60.0 : u.getNilaiLulus());
                o.put("batasWaktuMenit", m.getBatasWaktuMenit() == null ? JSONObject.NULL : m.getBatasWaktuMenit());
                o.put("batasPercobaan", m.getBatasPercobaan() == null ? JSONObject.NULL : m.getBatasPercobaan());
                o.put("acakSoal", m.getAcakSoal());
                o.put("acakJawaban", m.getAcakJawaban());
                o.put("jumlahSoalDitampilkan", m.getJumlahSoalDitampilkan() == null ? JSONObject.NULL : m.getJumlahSoalDitampilkan());
                JSONArray soalArr = new JSONArray();
                if (u != null) {
                    List link = db.createCriteria(UjianPunyaSoal.class).add(Restrictions.eq("ujian", u))
                            .addOrder(Order.asc("nomorUrut")).list();
                    for (int i = 0; i < link.size(); i++) {
                        UjianPunyaSoal up = (UjianPunyaSoal) link.get(i);
                        soalArr.put(soalJsonInstruktur(db, up.getBankSoal()));
                    }
                }
                o.put("soal", soalArr);
                res.put("status", "success"); res.put("data", o);
            }
        }
    }
    else if ("simpan_config_kuis".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long materiId = longParam(request, "materiKursusId");
            MateriKursus m = materiId == null ? null : (MateriKursus) db.get(MateriKursus.class, materiId);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            if (!pemilikMateriKuis(m, peserta)) {
                res.put("status", "error"); res.put("message", "Materi tidak valid.");
            } else {
                tx = db.beginTransaction();
                Ujian u = ambilAtauBuatUjianMateri(db, m);
                u.setNilaiLulus(numParam(request, "nilaiLulus", 60.0));
                db.update(u);
                Long batasWaktu = longParam(request, "batasWaktuMenit");
                Long batasPercobaan = longParam(request, "batasPercobaan");
                Long jumlahTampil = longParam(request, "jumlahSoalDitampilkan");
                m.setBatasWaktuMenit(batasWaktu == null ? null : batasWaktu.intValue());
                m.setBatasPercobaan(batasPercobaan == null ? null : batasPercobaan.intValue());
                m.setJumlahSoalDitampilkan(jumlahTampil == null ? null : jumlahTampil.intValue());
                m.setAcakSoal(boolParam(request, "acakSoal", false));
                m.setAcakJawaban(boolParam(request, "acakJawaban", false));
                db.update(m);
                tx.commit(); tx = null;
                res.put("status", "success"); res.put("message", "Konfigurasi kuis tersimpan.");
            }
        }
    }
    else if ("simpan_soal_kuis".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long materiId = longParam(request, "materiKursusId");
            MateriKursus m = materiId == null ? null : (MateriKursus) db.get(MateriKursus.class, materiId);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            String teksSoal = nvl(request.getParameter("soal"));
            if (!pemilikMateriKuis(m, peserta)) {
                res.put("status", "error"); res.put("message", "Materi tidak valid.");
            } else if (empty(teksSoal)) {
                res.put("status", "error"); res.put("message", "Teks soal harus diisi.");
            } else {
                boolean manual = "manual".equalsIgnoreCase(nvl(request.getParameter("jenisKoreksi")));
                Long soalId = longParam(request, "soalId");
                BankSoal soal = soalId == null ? null : (BankSoal) db.get(BankSoal.class, soalId);

                tx = db.beginTransaction();
                Ujian u = ambilAtauBuatUjianMateri(db, m);
                boolean soalBaru = (soal == null);
                if (soalBaru) soal = new BankSoal();
                soal.setSoal(teksSoal);
                soal.setJenisKoreksi(manual ? PenjelasanBankSoal.KOREKSI_MANUAL : PenjelasanBankSoal.KOREKSI_OTOMATIS);
                if (!manual) {
                    String jenisPg = nvl(request.getParameter("jenisPilihanGanda"));
                    soal.setJenisPilihanGanda(empty(jenisPg) ? BankSoal.MULTIPLE_COICE : jenisPg);
                }
                soal.setSkor(numParam(request, "skor", 1.0));
                soal.setSkorSalah(numParam(request, "skorSalah", 0.0));
                db.saveOrUpdate(soal);

                if (soalBaru) {
                    int urutan = db.createCriteria(UjianPunyaSoal.class).add(Restrictions.eq("ujian", u)).list().size();
                    UjianPunyaSoal up = new UjianPunyaSoal();
                    up.setUjian(u);
                    up.setBankSoal(soal);
                    up.setNomorUrut(urutan);
                    db.save(up);
                }

                if (!manual) {
                    List existing = db.createCriteria(BankSoalDetail.class).add(Restrictions.eq("bankSoal", soal)).list();
                    for (int i = 0; i < existing.size(); i++) db.delete(existing.get(i));

                    String pilihanRaw = nvl(request.getParameter("pilihan"));
                    if (!empty(pilihanRaw)) {
                        JSONArray pilihanArr = new JSONArray(pilihanRaw);
                        for (int i = 0; i < pilihanArr.length(); i++) {
                            JSONObject po = pilihanArr.getJSONObject(i);
                            BankSoalDetail d = new BankSoalDetail();
                            d.setBankSoal(soal);
                            d.setHuruf(po.isNull("huruf") ? String.valueOf((char) ('A' + i)) : po.getString("huruf"));
                            d.setJawaban(po.isNull("jawaban") ? "" : po.getString("jawaban"));
                            d.setBetul(!po.isNull("betul") && po.getBoolean("betul"));
                            db.save(d);
                        }
                    }
                }
                tx.commit(); tx = null;
                res.put("status", "success"); res.put("id", soal.getId());
            }
        }
    }
    else if ("hapus_soal_kuis".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long soalId = longParam(request, "soalId");
            BankSoal soal = soalId == null ? null : (BankSoal) db.get(BankSoal.class, soalId);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            UjianPunyaSoal up = soal == null ? null : (UjianPunyaSoal) db.createCriteria(UjianPunyaSoal.class)
                    .add(Restrictions.eq("bankSoal", soal)).setMaxResults(1).uniqueResult();
            MateriKursus m = up == null ? null : (MateriKursus) db.createCriteria(MateriKursus.class)
                    .add(Restrictions.eq("ujian", up.getUjian())).setMaxResults(1).uniqueResult();
            if (!pemilikMateriKuis(m, peserta)) {
                res.put("status", "error"); res.put("message", "Soal tidak valid.");
            } else {
                tx = db.beginTransaction();
                List details = db.createCriteria(BankSoalDetail.class).add(Restrictions.eq("bankSoal", soal)).list();
                for (int i = 0; i < details.size(); i++) db.delete(details.get(i));
                db.delete(up);
                db.delete(soal);
                tx.commit(); tx = null;
                res.put("status", "success");
            }
        }
    }
    /* =================== KUIS -- PESERTA: MENGERJAKAN =================== */
    else if ("mulai_kuis".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long materiId = longParam(request, "materiKursusId");
            Long enrollmentId = longParam(request, "enrollmentId");
            MateriKursus m = materiId == null ? null : (MateriKursus) db.get(MateriKursus.class, materiId);
            PesertaPunyaProdukKursus enr = enrollmentId == null ? null : (PesertaPunyaProdukKursus) db.get(PesertaPunyaProdukKursus.class, enrollmentId);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            boolean valid = m != null && enr != null && enr.getPesertaKursus() != null
                    && enr.getPesertaKursus().getId().equals(peserta.getId())
                    && PesertaPunyaProdukKursus.TERBELI.equals(enr.getStatus())
                    && m.getSeksiKursus() != null && m.getSeksiKursus().getProdukKursus() != null
                    && enr.getProdukKursus() != null
                    && m.getSeksiKursus().getProdukKursus().getId().equals(enr.getProdukKursus().getId());
            Ujian u = valid ? m.getUjian() : null;
            if (!valid || u == null) {
                res.put("status", "error"); res.put("message", "Kuis tidak valid atau belum dikonfigurasi.");
            } else {
                PercobaanKuisKursus berlangsung = (PercobaanKuisKursus) db.createCriteria(PercobaanKuisKursus.class)
                        .add(Restrictions.eq("materiKursus", m)).add(Restrictions.eq("pesertaPunyaProdukKursus", enr))
                        .add(Restrictions.eq("status", PercobaanKuisKursus.BERLANGSUNG))
                        .addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
                if (berlangsung != null) {
                    res.put("status", "success");
                    res.put("percobaanId", berlangsung.getId());
                    res.put("batasWaktuMenit", m.getBatasWaktuMenit() == null ? JSONObject.NULL : m.getBatasWaktuMenit());
                    res.put("waktuMulaiMs", berlangsung.getWaktuMulai().getTime());
                    res.put("dilanjutkan", true);
                    JSONArray soalArr = new JSONArray();
                    List jwbList = db.createCriteria(JawabanPercobaanKuisKursus.class)
                            .add(Restrictions.eq("percobaanKuisKursus", berlangsung)).addOrder(Order.asc("urutanTampil")).list();
                    for (int i = 0; i < jwbList.size(); i++) {
                        JawabanPercobaanKuisKursus j = (JawabanPercobaanKuisKursus) jwbList.get(i);
                        soalArr.put(soalJsonPeserta(db, j.getBankSoal(), false));
                    }
                    res.put("soal", soalArr);
                } else {
                    int jumlahSudah = db.createCriteria(PercobaanKuisKursus.class)
                            .add(Restrictions.eq("materiKursus", m)).add(Restrictions.eq("pesertaPunyaProdukKursus", enr))
                            .add(Restrictions.eq("status", PercobaanKuisKursus.SELESAI)).list().size();
                    if (m.getBatasPercobaan() != null && jumlahSudah >= m.getBatasPercobaan()) {
                        res.put("status", "error"); res.put("message", "Batas percobaan kuis sudah tercapai.");
                    } else {
                        List link = db.createCriteria(UjianPunyaSoal.class).add(Restrictions.eq("ujian", u))
                                .addOrder(Order.asc("nomorUrut")).list();
                        List<BankSoal> daftarSoal = new ArrayList<BankSoal>();
                        for (int i = 0; i < link.size(); i++) daftarSoal.add(((UjianPunyaSoal) link.get(i)).getBankSoal());
                        if (m.getAcakSoal()) Collections.shuffle(daftarSoal);
                        if (m.getJumlahSoalDitampilkan() != null && m.getJumlahSoalDitampilkan() < daftarSoal.size()) {
                            daftarSoal = daftarSoal.subList(0, m.getJumlahSoalDitampilkan());
                        }
                        if (daftarSoal.isEmpty()) {
                            res.put("status", "error"); res.put("message", "Kuis ini belum memiliki soal.");
                        } else {
                            tx = db.beginTransaction();
                            PercobaanKuisKursus p = new PercobaanKuisKursus();
                            p.setMateriKursus(m);
                            p.setPesertaPunyaProdukKursus(enr);
                            p.setNomorPercobaan(jumlahSudah + 1);
                            p.setWaktuMulai(new Date());
                            p.setStatus(PercobaanKuisKursus.BERLANGSUNG);
                            p.setJumlahSoal(daftarSoal.size());
                            db.save(p);
                            JSONArray soalArr = new JSONArray();
                            for (int i = 0; i < daftarSoal.size(); i++) {
                                BankSoal soal = daftarSoal.get(i);
                                JawabanPercobaanKuisKursus j = new JawabanPercobaanKuisKursus();
                                j.setPercobaanKuisKursus(p);
                                j.setBankSoal(soal);
                                j.setUrutanTampil(i);
                                j.setSkor(0.0);
                                db.save(j);
                                soalArr.put(soalJsonPeserta(db, soal, m.getAcakJawaban()));
                            }
                            tx.commit(); tx = null;
                            res.put("status", "success");
                            res.put("percobaanId", p.getId());
                            res.put("batasWaktuMenit", m.getBatasWaktuMenit() == null ? JSONObject.NULL : m.getBatasWaktuMenit());
                            res.put("waktuMulaiMs", p.getWaktuMulai().getTime());
                            res.put("dilanjutkan", false);
                            res.put("soal", soalArr);
                        }
                    }
                }
            }
        }
    }
    else if ("jawab_soal_kuis".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long percobaanId = longParam(request, "percobaanId");
            Long soalId = longParam(request, "soalId");
            PercobaanKuisKursus p = percobaanId == null ? null : (PercobaanKuisKursus) db.get(PercobaanKuisKursus.class, percobaanId);
            BankSoal soal = soalId == null ? null : (BankSoal) db.get(BankSoal.class, soalId);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            boolean valid = p != null && soal != null && p.getPesertaPunyaProdukKursus() != null
                    && p.getPesertaPunyaProdukKursus().getPesertaKursus() != null
                    && p.getPesertaPunyaProdukKursus().getPesertaKursus().getId().equals(peserta.getId())
                    && PercobaanKuisKursus.BERLANGSUNG.equals(p.getStatus());
            if (!valid) {
                res.put("status", "error"); res.put("message", "Percobaan tidak valid atau sudah selesai.");
            } else {
                JawabanPercobaanKuisKursus j = (JawabanPercobaanKuisKursus) db.createCriteria(JawabanPercobaanKuisKursus.class)
                        .add(Restrictions.eq("percobaanKuisKursus", p)).add(Restrictions.eq("bankSoal", soal))
                        .setMaxResults(1).uniqueResult();
                if (j == null) {
                    res.put("status", "error"); res.put("message", "Soal tidak termasuk dalam percobaan ini.");
                } else {
                    boolean detailValid = true;
                    if (PenjelasanBankSoal.KOREKSI_OTOMATIS.equals(soal.getJenisKoreksi())) {
                        Long detailId = longParam(request, "bankSoalDetailId");
                        BankSoalDetail detail = detailId == null ? null : (BankSoalDetail) db.get(BankSoalDetail.class, detailId);
                        if (detail != null && (detail.getBankSoal() == null || !detail.getBankSoal().getId().equals(soal.getId()))) {
                            detailValid = false;
                            res.put("status", "error"); res.put("message", "Pilihan jawaban tidak valid untuk soal ini.");
                        } else {
                            tx = db.beginTransaction();
                            j.setBankSoalDetailDipilih(detail);
                            j.setSkor(detail == null ? 0.0 : detail.getSkor());
                            j.setBenar(detail != null && detail.getBetul());
                            j.setSudahDinilai(true);
                        }
                    } else {
                        tx = db.beginTransaction();
                        j.setJawabanEsai(nvl(request.getParameter("jawabanEsai")));
                        j.setSkor(0.0);
                        j.setSudahDinilai(false);
                    }
                    if (detailValid) {
                        db.update(j);
                        tx.commit(); tx = null;
                        res.put("status", "success");
                    }
                }
            }
        }
    }
    else if ("selesai_kuis".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long percobaanId = longParam(request, "percobaanId");
            PercobaanKuisKursus p = percobaanId == null ? null : (PercobaanKuisKursus) db.get(PercobaanKuisKursus.class, percobaanId);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            boolean valid = p != null && p.getPesertaPunyaProdukKursus() != null
                    && p.getPesertaPunyaProdukKursus().getPesertaKursus() != null
                    && p.getPesertaPunyaProdukKursus().getPesertaKursus().getId().equals(peserta.getId());
            if (!valid) {
                res.put("status", "error"); res.put("message", "Percobaan tidak valid.");
            } else {
                tx = db.beginTransaction();
                p.setWaktuSelesai(new Date());
                p.setStatus(PercobaanKuisKursus.SELESAI);
                db.update(p);
                tx.commit(); tx = null;
                recomputePercobaan(db, p);

                boolean sudahSelesaiMateri = false;
                MateriKursus m = p.getMateriKursus();
                if (Boolean.TRUE.equals(p.getLulus()) || m.getBatasPercobaan() == null) {
                    ProgressMateriKursus prog = (ProgressMateriKursus) db.createCriteria(ProgressMateriKursus.class)
                            .add(Restrictions.eq("pesertaPunyaProdukKursus", p.getPesertaPunyaProdukKursus()))
                            .add(Restrictions.eq("materiKursus", m)).setMaxResults(1).uniqueResult();
                    tx = db.beginTransaction();
                    if (prog == null) { prog = new ProgressMateriKursus(); prog.setPesertaPunyaProdukKursus(p.getPesertaPunyaProdukKursus()); prog.setMateriKursus(m); }
                    prog.setSelesai(true);
                    prog.setWaktuSelesai(new Date());
                    db.saveOrUpdate(prog);
                    tx.commit(); tx = null;
                    sudahSelesaiMateri = true;
                    cekDanTerbitkanSertifikat(db, p.getPesertaPunyaProdukKursus());
                }

                JSONObject o = new JSONObject();
                o.put("totalNilai", p.getTotalNilai());
                o.put("lulus", p.getLulus() == null ? JSONObject.NULL : p.getLulus());
                o.put("jumlahBenar", p.getJumlahBenar());
                o.put("jumlahSoal", p.getJumlahSoal());
                o.put("materiDitandaiSelesai", sudahSelesaiMateri);
                res.put("status", "success"); res.put("data", o);
            }
        }
    }
    else if ("get_riwayat_kuis".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long materiId = longParam(request, "materiKursusId");
            Long enrollmentId = longParam(request, "enrollmentId");
            MateriKursus m = materiId == null ? null : (MateriKursus) db.get(MateriKursus.class, materiId);
            PesertaPunyaProdukKursus enr = enrollmentId == null ? null : (PesertaPunyaProdukKursus) db.get(PesertaPunyaProdukKursus.class, enrollmentId);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            if (m == null || enr == null || enr.getPesertaKursus() == null || !enr.getPesertaKursus().getId().equals(peserta.getId())) {
                res.put("status", "error"); res.put("message", "Data tidak valid.");
            } else {
                JSONArray arr = new JSONArray();
                List list = db.createCriteria(PercobaanKuisKursus.class).add(Restrictions.eq("materiKursus", m))
                        .add(Restrictions.eq("pesertaPunyaProdukKursus", enr)).addOrder(Order.desc("nomorPercobaan")).list();
                for (int i = 0; i < list.size(); i++) {
                    PercobaanKuisKursus p = (PercobaanKuisKursus) list.get(i);
                    JSONObject o = new JSONObject();
                    o.put("id", p.getId());
                    o.put("nomorPercobaan", p.getNomorPercobaan());
                    o.put("status", p.getStatus());
                    o.put("totalNilai", p.getTotalNilai());
                    o.put("lulus", p.getLulus() == null ? JSONObject.NULL : p.getLulus());
                    o.put("waktuMulai", Common.dateFormat3.get().format(p.getWaktuMulai()));
                    arr.put(o);
                }
                res.put("status", "success"); res.put("data", arr);
            }
        }
    }
    else if ("nilai_jawaban_esai".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long jawabanId = longParam(request, "jawabanId");
            JawabanPercobaanKuisKursus j = jawabanId == null ? null : (JawabanPercobaanKuisKursus) db.get(JawabanPercobaanKuisKursus.class, jawabanId);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            MateriKursus m = j == null ? null : j.getPercobaanKuisKursus().getMateriKursus();
            if (!pemilikMateriKuis(m, peserta)) {
                res.put("status", "error"); res.put("message", "Data tidak valid.");
            } else {
                tx = db.beginTransaction();
                j.setSkor(numParam(request, "skor", 0.0));
                j.setCatatanPenilaian(nvl(request.getParameter("catatan")));
                j.setSudahDinilai(true);
                db.update(j);
                tx.commit(); tx = null;
                recomputePercobaan(db, j.getPercobaanKuisKursus());
                res.put("status", "success");
            }
        }
    }
    else if ("get_sertifikat".equals(aksi)) {
        if (tbmuser == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long enrollmentId = longParam(request, "enrollmentId");
            PesertaPunyaProdukKursus enr = enrollmentId == null ? null : (PesertaPunyaProdukKursus) db.get(PesertaPunyaProdukKursus.class, enrollmentId);
            PesertaKursus peserta = ensurePesertaKursus(db, tbmuser);
            if (enr == null || enr.getPesertaKursus() == null || !enr.getPesertaKursus().getId().equals(peserta.getId())) {
                res.put("status", "error"); res.put("message", "Data tidak valid.");
            } else {
                SertifikatKursus sert = (SertifikatKursus) db.createCriteria(SertifikatKursus.class)
                        .add(Restrictions.eq("pesertaPunyaProdukKursus", enr)).setMaxResults(1).uniqueResult();
                if (sert == null) {
                    res.put("status", "error"); res.put("message", "Sertifikat belum diterbitkan untuk kursus ini.");
                } else {
                    JSONObject o = sertifikatJson(sert);
                    o.put("namaPeserta", nvl(peserta.getNama()));
                    o.put("namaKursus", nvl(enr.getProdukKursus().getNama()));
                    o.put("namaInstruktur", enr.getProdukKursus().getInstruktur() == null ? "" : nvl(enr.getProdukKursus().getInstruktur().getNama()));
                    o.put("namaInstitusi", enr.getProdukKursus().getSatuanKerja() == null ? "" : nvl(enr.getProdukKursus().getSatuanKerja().getNama()));
                    res.put("status", "success"); res.put("data", o);
                }
            }
        }
    }
    else {
        res.put("status", "error"); res.put("message", "Aksi tidak dikenali.");
    }
} catch (Exception e) {
    try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp:rollback"); }
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp:catch");
    try { res.put("status", "error"); res.put("message", e.getMessage() == null ? e.toString() : e.getMessage()); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp:catch2"); }
} finally {
    try { if (db != null) db.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp:finally"); }
}
out.print(res.toString());
out.flush();
%>
