<%@page session="false"%>
<%--
  Endpoint JSON: Sinkronisasi Siswa/Mahasiswa -> AnggotaKoperasi (versi JSP -- setara ZK
  AnggotaKoperasiAction "Singkronkan Mahasiswa"/"Singkronkan Siswa"). Kandidat dicari via Hibernate
  Criteria (SAMA PERSIS dengan versi ZK) agar tak perlu menebak nama kolom SQL mentah utk
  Mahasiswa/Jurusan/Fakultas/Siswa/Sekolah/Yayasan. Logika inti (dedup, isi field, simpan) memakai
  ULANG Common.checkApakahMahasiswaOtomatisMenjadiAnggotaKoperasi/
  checkApakahSiswaOtomatisMenjadiAnggotaKoperasi -- SATU sumber logika utk kedua versi (JSP & ZK).

  Aksi (GET, populasi dropdown): listKoperasi, listFakultas, listJurusan(&fakultasId=),
    listYayasan, listSekolah(&yayasanId=)
  Aksi (POST, admin-only): sinkronMahasiswa (koperasiId,tahun,fakultasId?,jurusanId?),
    sinkronSiswa (koperasiId,tahun,yayasanId?,sekolahId?)

  Sesi currentNativeSession() -- TIDAK ditutup manual (FilterJSP tutup terpusat di akhir request).
  Common.checkApakah...OtomatisMenjadiAnggotaKoperasi SENGAJA tak lagi menutup session sendiri
  (lihat javadoc di Common.java) -- kontrak itu tetap aman di sini krn JSP memang tak boleh
  menutup currentNativeSession() secara manual.
--%>
<%@page import="java.util.*"%>
<%@page import="org.json.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.koperasi.Koperasi"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%!
    private static boolean ada(String s){ return s!=null && s.trim().length()>0; }
    private static Long parseLong(String s){ try { return ada(s) ? Long.valueOf(s.trim()) : null; } catch (Exception e) { return null; } }
    private static Integer parseInt(String s){ try { return ada(s) ? Integer.valueOf(s.trim()) : null; } catch (Exception e) { return null; } }
%>
<%
response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
JSONObject result = new JSONObject();
try {
    Tbmuser u = Common.getCurrentUser(request);
    if (u==null || u.getUserId()==null){ result.put("status","01"); result.put("message","Sesi berakhir."); out.print(result.toString()); return; }

    String aksi = request.getParameter("aksi");
    Session session = HibernateUtil.currentNativeSession();

    if ("listKoperasi".equals(aksi)) {
        JSONArray arr = new JSONArray();
        for (Object o : session.createCriteria(Koperasi.class).addOrder(org.hibernate.criterion.Order.asc("nama")).list()) {
            Koperasi k = (Koperasi) o; JSONObject j = new JSONObject();
            j.put("id", k.getId()); j.put("nama", k.getNama()==null?("Koperasi #"+k.getId()):k.getNama());
            arr.put(j);
        }
        result.put("status","00"); result.put("data", arr);

    } else if ("listFakultas".equals(aksi)) {
        JSONArray arr = new JSONArray();
        for (Object o : session.createCriteria(Fakultas.class).addOrder(org.hibernate.criterion.Order.asc("nama")).list()) {
            Fakultas f = (Fakultas) o; JSONObject j = new JSONObject();
            j.put("id", f.getId()); j.put("nama", f.getNama()==null?"-":f.getNama());
            arr.put(j);
        }
        result.put("status","00"); result.put("data", arr);

    } else if ("listJurusan".equals(aksi)) {
        Long fakultasId = parseLong(request.getParameter("fakultasId"));
        Criteria c = session.createCriteria(Jurusan.class).addOrder(org.hibernate.criterion.Order.asc("nama"));
        if (fakultasId != null) { Fakultas fk = (Fakultas) session.get(Fakultas.class, fakultasId); if (fk != null) c.add(Restrictions.eq("fakultas", fk)); }
        JSONArray arr = new JSONArray();
        for (Object o : c.list()) {
            Jurusan j2 = (Jurusan) o; JSONObject j = new JSONObject();
            j.put("id", j2.getId()); j.put("nama", j2.getNama()==null?"-":j2.getNama());
            arr.put(j);
        }
        result.put("status","00"); result.put("data", arr);

    } else if ("listYayasan".equals(aksi)) {
        JSONArray arr = new JSONArray();
        for (Object o : session.createCriteria(Yayasan.class).addOrder(org.hibernate.criterion.Order.asc("nama")).list()) {
            Yayasan y = (Yayasan) o; JSONObject j = new JSONObject();
            j.put("id", y.getId()); j.put("nama", y.getNama()==null?"-":y.getNama());
            arr.put(j);
        }
        result.put("status","00"); result.put("data", arr);

    } else if ("listSekolah".equals(aksi)) {
        Long yayasanId = parseLong(request.getParameter("yayasanId"));
        Criteria c = session.createCriteria(Sekolah.class).addOrder(org.hibernate.criterion.Order.asc("nama"));
        if (yayasanId != null) { Yayasan yy = (Yayasan) session.get(Yayasan.class, yayasanId); if (yy != null) c.add(Restrictions.eq("yayasan", yy)); }
        JSONArray arr = new JSONArray();
        for (Object o : c.list()) {
            Sekolah s = (Sekolah) o; JSONObject j = new JSONObject();
            j.put("id", s.getId()); j.put("nama", s.getNama()==null?"-":s.getNama());
            arr.put(j);
        }
        result.put("status","00"); result.put("data", arr);

    } else if ("sinkronMahasiswa".equals(aksi)) {
        boolean boleh = ais.action.master.koperasi.helper.LokasiKantinUtil.bolehKelola(request);
        if (!boleh) { result.put("status","03"); result.put("message","Hanya admin yang boleh menjalankan sinkronisasi."); out.print(result.toString()); return; }

        Long koperasiId = parseLong(request.getParameter("koperasiId"));
        Integer tahun = parseInt(request.getParameter("tahun"));
        if (koperasiId == null || tahun == null) { result.put("status","02"); result.put("message","Koperasi dan Tahun Angkatan wajib diisi."); out.print(result.toString()); return; }
        Koperasi koperasi = (Koperasi) session.get(Koperasi.class, koperasiId);
        if (koperasi == null) { result.put("status","02"); result.put("message","Koperasi tidak ditemukan."); out.print(result.toString()); return; }
        Long fakultasId = parseLong(request.getParameter("fakultasId"));
        Long jurusanId = parseLong(request.getParameter("jurusanId"));

        // Kandidat: SAMA PERSIS dgn AnggotaKoperasiAction (ZK) "Singkronkan Mahasiswa" -- filter
        // fakultas/jurusan dibandingkan thd ENTITAS termuat (bukan path titik bertingkat "a.b.c",
        // yg butuh alias terpisah tiap hop pada Hibernate Criteria mentah).
        Jurusan jurusanEntity = jurusanId != null ? (Jurusan) session.get(Jurusan.class, jurusanId) : null;
        Fakultas fakultasEntity = fakultasId != null ? (Fakultas) session.get(Fakultas.class, fakultasId) : null;
        Criteria cq = session.createCriteria(Mahasiswa.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .createAlias("jurusan", "jurusan")
                .add(Restrictions.eq("tahunangkatan", tahun))
                .setProjection(Projections.groupProperty("nim"));
        if (jurusanEntity != null) cq.add(Restrictions.eq("jurusan", jurusanEntity));
        if (fakultasEntity != null) cq.add(Restrictions.eq("jurusan.fakultas", fakultasEntity));
        @SuppressWarnings("unchecked")
        List<String> ids = cq.list();

        int berhasil = 0, gagal = 0;
        for (String nim : ids) {
            try {
                if (Common.checkApakahMahasiswaOtomatisMenjadiAnggotaKoperasi(nim, koperasi) != null) berhasil++;
            } catch (Exception exSatu) {
                // SATU nim bermasalah TIDAK BOLEH menghentikan sisa batch -- catat & lanjut (sama spt versi ZK).
                gagal++;
                exSatu.printStackTrace(); ais.common.ErrorAuditUtil.record(exSatu, "auto-audit webapp/WEB-INF/baru/modul/kantin/member/sinkron_siswa_mahasiswa_service.jsp:137");
            }
        }
        result.put("status","00"); result.put("total", ids.size()); result.put("berhasil", berhasil); result.put("gagal", gagal);

    } else if ("sinkronSiswa".equals(aksi)) {
        boolean boleh = ais.action.master.koperasi.helper.LokasiKantinUtil.bolehKelola(request);
        if (!boleh) { result.put("status","03"); result.put("message","Hanya admin yang boleh menjalankan sinkronisasi."); out.print(result.toString()); return; }

        Long koperasiId = parseLong(request.getParameter("koperasiId"));
        Integer tahun = parseInt(request.getParameter("tahun"));
        if (koperasiId == null || tahun == null) { result.put("status","02"); result.put("message","Koperasi dan Tahun Masuk wajib diisi."); out.print(result.toString()); return; }
        Koperasi koperasi = (Koperasi) session.get(Koperasi.class, koperasiId);
        if (koperasi == null) { result.put("status","02"); result.put("message","Koperasi tidak ditemukan."); out.print(result.toString()); return; }
        Long yayasanId = parseLong(request.getParameter("yayasanId"));
        Long sekolahId = parseLong(request.getParameter("sekolahId"));

        // Kandidat: SAMA PERSIS dgn AnggotaKoperasiAction (ZK) "Singkronkan Siswa" -- bandingkan
        // thd ENTITAS termuat (properti asosiasi tanpa createAlias tak reliabel dirujuk via path titik).
        Sekolah sekolahEntity = sekolahId != null ? (Sekolah) session.get(Sekolah.class, sekolahId) : null;
        Yayasan yayasanEntity = yayasanId != null ? (Yayasan) session.get(Yayasan.class, yayasanId) : null;
        Criteria cq = session.createCriteria(Siswa.class)
                .add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
                .add(Restrictions.isNotNull("sekolah"))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.eq("tahunMasuk", tahun));
        if (sekolahEntity != null) cq.add(Restrictions.eq("sekolah", sekolahEntity));
        if (yayasanEntity != null) cq.add(Restrictions.eq("yayasan", yayasanEntity));
        @SuppressWarnings("unchecked")
        List<Siswa> daftarSiswa = ConstantValues.simpleList(cq, Siswa.class);

        int berhasil = 0, gagal = 0;
        for (Siswa siswa : daftarSiswa) {
            try {
                if (Common.checkApakahSiswaOtomatisMenjadiAnggotaKoperasi(siswa, koperasi) != null) berhasil++;
            } catch (Exception exSatu) {
                gagal++;
                exSatu.printStackTrace(); ais.common.ErrorAuditUtil.record(exSatu, "auto-audit webapp/WEB-INF/baru/modul/kantin/member/sinkron_siswa_mahasiswa_service.jsp:174");
            }
        }
        result.put("status","00"); result.put("total", daftarSiswa.size()); result.put("berhasil", berhasil); result.put("gagal", gagal);

    } else {
        result.put("status","02"); result.put("message","Aksi tidak dikenali.");
    }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/member/sinkron_siswa_mahasiswa_service.jsp:183");
    try { result.put("status","99"); result.put("message","Error: "+e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/member/sinkron_siswa_mahasiswa_service.jsp:184");}
}
out.print(result.toString());
%>
