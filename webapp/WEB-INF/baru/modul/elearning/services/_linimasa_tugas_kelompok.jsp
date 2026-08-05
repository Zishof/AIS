<%@page import="ais.database.model.TugasKelompok"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Disjunction"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.CriteriaSpecification"%>
<%@page import="java.util.Date"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    response.setStatus(401); 
    return;
}

String ta = request.getParameter("q_ta") != null && !request.getParameter("q_ta").trim().isEmpty() ? request.getParameter("q_ta") : request.getParameter("ta");
String smt = request.getParameter("q_smt") != null && !request.getParameter("q_smt").trim().isEmpty() ? request.getParameter("q_smt") : request.getParameter("smt");

String dir = request.getParameter("dir"); 
String refDateStr = request.getParameter("refDate");
String mulaiStr = request.getParameter("mulai") == null ? "0" : request.getParameter("mulai");
String banyakStr = request.getParameter("banyak") == null ? "10" : request.getParameter("banyak");

int startIdx = Integer.parseInt(mulaiStr);
int limitRows = Integer.parseInt(banyakStr);

// Parameter Filter Pencarian
String q_nama_tugas = request.getParameter("q_nama_tugas");

String q_fakultas = request.getParameter("q_fakultas");
String q_jurusan = request.getParameter("q_jurusan");
String q_yayasan = request.getParameter("q_yayasan");
String q_sekolah = request.getParameter("q_sekolah");

String q_matakuliah = request.getParameter("q_matakuliah");
String q_kelas_kuliah = request.getParameter("q_kelas_kuliah");
String q_matapelajaran = request.getParameter("q_matapelajaran");
String q_kelas_siswa = request.getParameter("q_kelas_siswa");

String q_dosen = request.getParameter("q_dosen");
String q_mahasiswa = request.getParameter("q_mahasiswa");
String q_guru = request.getParameter("q_guru");
String q_siswa = request.getParameter("q_siswa");

String q_topik = request.getParameter("q_topik");
String q_catatan = request.getParameter("q_catatan");
String q_indikator = request.getParameter("q_indikator");
String q_metode = request.getParameter("q_metode");
String q_buku1 = request.getParameter("q_buku1");
String q_buku2 = request.getParameter("q_buku2");
String q_ruang = request.getParameter("q_ruang");

String q_tgl_mulai = request.getParameter("q_tgl_mulai");
String q_tgl_selesai = request.getParameter("q_tgl_selesai");

String q_ke_mulai = request.getParameter("q_ke_mulai");
String q_ke_selesai = request.getParameter("q_ke_selesai");

// Parameter Sumber Kegiatan
String[] arrSumber = {
    "perkuliahan", "jadwalUjianPMB", "mahasiswaRequestTugasAkhir", "kelompokKkn", "kelompokPkl", 
    "skripsi", "krsMahasiswa", "jadwalUjianPSB", "jadwalPertemuanPSB", "jadwalUjianPegawai", 
    "jadwalPelajaran", "kelasLesSiswa", "pertemuanPunyaGrupPertemuan", "formulirKegiatan", "komponenDataProdukKursus"
};

Siswa siswa = tbmuser.getSiswa();
Mahasiswa mahasiswa = tbmuser.getMahasiswa();
Dosen dosen = tbmuser.ambilDosen();
Guru guru = tbmuser.ambilGuru();
Fakultas fakultas = tbmuser.ambilFakultas();
Jurusan jurusan = tbmuser.ambilJurusan();
Sekolah sekolah = tbmuser.ambilSekolah();
Yayasan yayasan = tbmuser.ambilYayasan();

Date sekarang = WaktuUtil.getDate();
Date refDate = sekarang;
if (refDateStr != null && !refDateStr.trim().isEmpty()) {
    try { refDate = new Date(Long.parseLong(refDateStr)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas_kelompok.jsp:99");}
}

Session sess = HibernateUtil.openSession();
JSONObject responseJson = new JSONObject();

try {
    List<Long> listJurusanIds = null;
    if (q_jurusan != null && !q_jurusan.trim().isEmpty()) {
        try { listJurusanIds = new ArrayList<Long>(); listJurusanIds.add(Long.parseLong(q_jurusan)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas_kelompok.jsp:108");}
    } else if (q_fakultas != null && !q_fakultas.trim().isEmpty()) {
        try {
            Fakultas fakSearch = (Fakultas) sess.get(Fakultas.class, Long.parseLong(q_fakultas));
            if (fakSearch != null) {
                listJurusanIds = sess.createCriteria(Jurusan.class).setProjection(Projections.property("id")).add(Restrictions.eq("fakultas", fakSearch)).list();
                if (listJurusanIds == null) listJurusanIds = new ArrayList<Long>();
                if (listJurusanIds.isEmpty()) listJurusanIds.add(-1L); 
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas_kelompok.jsp:117");}
    }

    List<Long> listSekolahIds = null;
    if (q_sekolah != null && !q_sekolah.trim().isEmpty()) {
        try { listSekolahIds = new ArrayList<Long>(); listSekolahIds.add(Long.parseLong(q_sekolah)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas_kelompok.jsp:122");}
    } else if (q_yayasan != null && !q_yayasan.trim().isEmpty()) {
        try {
            Yayasan yaySearch = (Yayasan) sess.get(Yayasan.class, Long.parseLong(q_yayasan));
            if (yaySearch != null) {
                listSekolahIds = sess.createCriteria(Sekolah.class).setProjection(Projections.property("id")).add(Restrictions.eq("yayasan", yaySearch)).list();
                if (listSekolahIds == null) listSekolahIds = new ArrayList<Long>();
                if (listSekolahIds.isEmpty()) listSekolahIds.add(-1L);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas_kelompok.jsp:131");}
    }

    Disjunction djDosen = null; boolean kosongDosen = false;
    if (q_dosen != null && !q_dosen.trim().isEmpty()) {
        List<Long> ids = sess.createCriteria(Dosen.class).setProjection(Projections.property("id")).add(Restrictions.or(Restrictions.ilike("nama", q_dosen, MatchMode.ANYWHERE), Restrictions.ilike("nidn", q_dosen, MatchMode.ANYWHERE))).list();
        if (ids != null && !ids.isEmpty()) { djDosen = Restrictions.disjunction(); for (Long id : ids) djDosen.add(Restrictions.like("pert.dosens", "%," + id + ",%")); } 
        else kosongDosen = true;
    }

    Disjunction djMhs = null; boolean kosongMhs = false;
    if (q_mahasiswa != null && !q_mahasiswa.trim().isEmpty()) {
        List<Long> ids = sess.createCriteria(Mahasiswa.class).setProjection(Projections.property("id")).add(Restrictions.or(Restrictions.ilike("nama", q_mahasiswa, MatchMode.ANYWHERE), Restrictions.ilike("nim", q_mahasiswa, MatchMode.ANYWHERE))).list();
        if (ids != null && !ids.isEmpty()) { djMhs = Restrictions.disjunction(); for (Long id : ids) djMhs.add(Restrictions.like("pert.mahasiswas", "%," + id + ",%")); } 
        else kosongMhs = true;
    }

    Disjunction djGuru = null; boolean kosongGuru = false;
    if (q_guru != null && !q_guru.trim().isEmpty()) {
        List<Long> ids = sess.createCriteria(Guru.class).setProjection(Projections.property("id")).add(Restrictions.or(Restrictions.ilike("nama", q_guru, MatchMode.ANYWHERE), Restrictions.ilike("nuptk", q_guru, MatchMode.ANYWHERE))).list();
        if (ids != null && !ids.isEmpty()) { djGuru = Restrictions.disjunction(); for (Long id : ids) djGuru.add(Restrictions.like("pert.gurus", "%," + id + ",%")); } 
        else kosongGuru = true;
    }

    Disjunction djSiswa = null; boolean kosongSiswa = false;
    if (q_siswa != null && !q_siswa.trim().isEmpty()) {
        List<Long> ids = sess.createCriteria(Siswa.class).setProjection(Projections.property("id")).add(Restrictions.or(Restrictions.ilike("nama", q_siswa, MatchMode.ANYWHERE), Restrictions.ilike("nomorInduk", q_siswa, MatchMode.ANYWHERE))).list();
        if (ids != null && !ids.isEmpty()) { djSiswa = Restrictions.disjunction(); for (Long id : ids) djSiswa.add(Restrictions.like("pert.siswas", "%," + id + ",%")); } 
        else kosongSiswa = true;
    }

    // --------------------------------------------------------------------------------------
    // PERBAIKAN ALIAS: Kita gunakan alias "pert" agar tidak terjadi tabrakan nama dengan field "pertemuan"
    // --------------------------------------------------------------------------------------
    Criteria criteriaList = sess.createCriteria(TugasKelompok.class).createAlias("pertemuanData", "pert");
    Criteria criteriaCountNext = sess.createCriteria(TugasKelompok.class).createAlias("pertemuanData", "pert");
    Criteria criteriaCountBack = sess.createCriteria(TugasKelompok.class).createAlias("pertemuanData", "pert");

    Criteria[] allCriteria = {criteriaList, criteriaCountNext, criteriaCountBack};
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    
    boolean joinPerkuliahan = (q_matakuliah != null && !q_matakuliah.trim().isEmpty()) || (q_kelas_kuliah != null && !q_kelas_kuliah.trim().isEmpty());
    boolean joinJadwal = (q_matapelajaran != null && !q_matapelajaran.trim().isEmpty()) || (q_kelas_siswa != null && !q_kelas_siswa.trim().isEmpty());
    
    for (Criteria c : allCriteria) {
        
        c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))); 
        c.add(Restrictions.or(Restrictions.isNull("pert.aktif"), Restrictions.eq("pert.aktif", true))); 
        
        if (siswa != null || mahasiswa != null || dosen != null || guru != null) {
            Disjunction djAkses = Restrictions.disjunction();
            if (siswa != null) djAkses.add(Restrictions.like("pert.siswas", "%," + siswa.getId() + ",%"));
            if (mahasiswa != null) djAkses.add(Restrictions.like("pert.mahasiswas", "%," + mahasiswa.getId() + ",%"));
            if (dosen != null) djAkses.add(Restrictions.like("pert.dosens", "%," + dosen.getId() + ",%"));
            if (guru != null) djAkses.add(Restrictions.like("pert.gurus", "%," + guru.getId() + ",%"));
            c.add(djAkses);
        }

        if (jurusan != null) {
            c.add(Restrictions.eq("pert.jurusan", jurusan));
        } else if (fakultas != null) {
            c.createAlias("pert.jurusan", "jStruk", CriteriaSpecification.LEFT_JOIN);
            c.add(Restrictions.eq("jStruk.fakultas", fakultas));
        }

        if (sekolah != null) {
            c.add(Restrictions.eq("pert.sekolah", sekolah));
        } else if (yayasan != null) {
            c.createAlias("pert.sekolah", "sStruk", CriteriaSpecification.LEFT_JOIN);
            c.add(Restrictions.eq("sStruk.yayasan", yayasan));
        }

        if (q_nama_tugas != null && !q_nama_tugas.trim().isEmpty()) {
            c.add(Restrictions.ilike("judultugas", q_nama_tugas, MatchMode.ANYWHERE));
        }

        // PENERAPAN FILTER SUMBER KEGIATAN (Namespace "pert.")
        for(String fName : arrSumber) {
            String val = request.getParameter("q_has_" + fName);
            if("1".equals(val)) {
                c.add(Restrictions.isNotNull("pert." + fName));
            } else if ("0".equals(val)) {
                c.add(Restrictions.isNull("pert." + fName));
            }
        }

        if (ta != null && !ta.trim().isEmpty()) c.add(Restrictions.eq("pert.ta", ta));
        if (smt != null && !smt.trim().isEmpty()) c.add(Restrictions.eq("pert.smt", smt));

        if (listJurusanIds != null && !listJurusanIds.isEmpty()) c.add(Restrictions.in("pert.jurusan.id", listJurusanIds));
        if (listSekolahIds != null && !listSekolahIds.isEmpty()) c.add(Restrictions.in("pert.sekolah.id", listSekolahIds));

        if (kosongDosen || kosongMhs || kosongGuru || kosongSiswa) {
            c.add(Restrictions.sqlRestriction("1=0")); 
        } else {
            if (djDosen != null) c.add(djDosen);
            if (djMhs != null) c.add(djMhs);
            if (djGuru != null) c.add(djGuru);
            if (djSiswa != null) c.add(djSiswa);
        }

        if (joinPerkuliahan) {
            c.createAlias("pert.perkuliahan", "prk", CriteriaSpecification.LEFT_JOIN);
            if (q_matakuliah != null && !q_matakuliah.trim().isEmpty()) {
                c.createAlias("prk.matakuliah", "mk", CriteriaSpecification.LEFT_JOIN);
                c.add(Restrictions.or(
                    Restrictions.ilike("mk.kode", q_matakuliah, MatchMode.ANYWHERE),
                    Restrictions.ilike("mk.nama", q_matakuliah, MatchMode.ANYWHERE)
                ));
            }
            if (q_kelas_kuliah != null && !q_kelas_kuliah.trim().isEmpty()) {
                c.add(Restrictions.ilike("prk.kelas", q_kelas_kuliah, MatchMode.ANYWHERE));
            }
        }

        if (joinJadwal) {
            c.createAlias("pert.jadwalPelajaran", "jp", CriteriaSpecification.LEFT_JOIN);
            if (q_matapelajaran != null && !q_matapelajaran.trim().isEmpty()) {
                c.createAlias("jp.matapelajaran", "mp", CriteriaSpecification.LEFT_JOIN);
                c.add(Restrictions.or(
                    Restrictions.ilike("mp.kode", q_matapelajaran, MatchMode.ANYWHERE),
                    Restrictions.ilike("mp.nama", q_matapelajaran, MatchMode.ANYWHERE)
                ));
            }
            if (q_kelas_siswa != null && !q_kelas_siswa.trim().isEmpty()) {
                c.createAlias("jp.kelas", "ks", CriteriaSpecification.LEFT_JOIN);
                c.add(Restrictions.ilike("ks.nama", q_kelas_siswa, MatchMode.ANYWHERE));
            }
        }

        if (q_topik != null && !q_topik.trim().isEmpty()) c.add(Restrictions.ilike("pert.topik", q_topik, MatchMode.ANYWHERE));
        if (q_catatan != null && !q_catatan.trim().isEmpty()) c.add(Restrictions.ilike("pert.catatan", q_catatan, MatchMode.ANYWHERE));
        if (q_indikator != null && !q_indikator.trim().isEmpty()) c.add(Restrictions.ilike("pert.indikator", q_indikator, MatchMode.ANYWHERE));
        if (q_metode != null && !q_metode.trim().isEmpty()) c.add(Restrictions.ilike("pert.metodePembelajaran", q_metode, MatchMode.ANYWHERE));
        if (q_buku1 != null && !q_buku1.trim().isEmpty()) c.add(Restrictions.ilike("pert.bukuRujukan1", q_buku1, MatchMode.ANYWHERE));
        if (q_buku2 != null && !q_buku2.trim().isEmpty()) c.add(Restrictions.ilike("pert.bukuRujukan2", q_buku2, MatchMode.ANYWHERE));
        
        if (q_ruang != null && !q_ruang.trim().isEmpty()){
            c.createAlias("pert.ruang", "ruangAlias", CriteriaSpecification.LEFT_JOIN); 
            c.add(Restrictions.or(
                Restrictions.ilike("ruangAlias.kode", q_ruang, MatchMode.ANYWHERE), 
                Restrictions.ilike("ruangAlias.nama", q_ruang, MatchMode.ANYWHERE)
            )); 
        }

        // AMAN: Memakai properti alias "pert"
        if (q_ke_mulai != null && !q_ke_mulai.trim().isEmpty()) {
            try { c.add(Restrictions.ge("pert.pertemuanKe", Integer.parseInt(q_ke_mulai))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas_kelompok.jsp:278");}
        }
        if (q_ke_selesai != null && !q_ke_selesai.trim().isEmpty()) {
            try { c.add(Restrictions.le("pert.pertemuanKe", Integer.parseInt(q_ke_selesai))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas_kelompok.jsp:281");}
        }

        if (q_tgl_mulai != null && !q_tgl_mulai.trim().isEmpty()) {
            try { c.add(Restrictions.ge("mulai", sdf.parse(q_tgl_mulai))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas_kelompok.jsp:285");}
        }
        if (q_tgl_selesai != null && !q_tgl_selesai.trim().isEmpty()) {
            try { 
                Date tglSelesai = sdf.parse(q_tgl_selesai);
                tglSelesai.setHours(23); tglSelesai.setMinutes(59); tglSelesai.setSeconds(59); 
                c.add(Restrictions.le("mulai", tglSelesai)); 
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas_kelompok.jsp:292");}
        }
    }

    // 4. Eksekusi Kueri Data List
    criteriaList.setProjection(Projections.property("id"));
    criteriaList.setFirstResult(startIdx);
    criteriaList.setMaxResults(limitRows);
    
    // PEMBELAHAN MUTLAK refDate BERDASARKAN "mulai"
    if ("prev".equalsIgnoreCase(dir)) {
        criteriaList.add(Restrictions.lt("mulai", refDate));
        criteriaList.addOrder(Order.desc("mulai"));
    } else {
        criteriaList.add(Restrictions.ge("mulai", refDate));
        criteriaList.addOrder(Order.asc("mulai"));
    }
    
    List<Long> pertemuns = criteriaList.list();
    JSONArray dataArray = new JSONArray();
    if (pertemuns != null) {
        for (Long id : pertemuns) { dataArray.put(id); }
    }

    // 5. Eksekusi Perhitungan Paginasi
    criteriaCountNext.setProjection(Projections.rowCount());
    criteriaCountNext.add(Restrictions.ge("mulai", sekarang));
    Long countNext = (Long) criteriaCountNext.uniqueResult();

    criteriaCountBack.setProjection(Projections.rowCount());
    criteriaCountBack.add(Restrictions.lt("mulai", sekarang));
    Long countBack = (Long) criteriaCountBack.uniqueResult();

    responseJson.put("data", dataArray);
    responseJson.put("countNext", countNext != null ? countNext : 0);
    responseJson.put("countBack", countBack != null ? countBack : 0);

} catch(Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas_kelompok.jsp:330");
    responseJson.put("data", new JSONArray());
    responseJson.put("countNext", 0);
    responseJson.put("countBack", 0);
} finally {
    try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas_kelompok.jsp:335");}
    ais.common.ElearningSessionUtil.closeQuietly(sess);
    HibernateUtil.closeSessionQuietly(sess);
}

out.print(responseJson.toString());
out.flush();
%>