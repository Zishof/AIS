<%@page import="ais.database.model.TugasPertemuan"%>
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
<%@page import="java.util.Collections"%>
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

// Kriteria tambahan untuk tugas
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
    try { refDate = new Date(Long.parseLong(refDateStr)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas.jsp:97");}
}

Session sess = HibernateUtil.openSession();
JSONObject responseJson = new JSONObject();

try {
    List<Long> listJurusanIds = null;
    if (q_jurusan != null && !q_jurusan.trim().isEmpty()) {
        try { listJurusanIds = new ArrayList<Long>(); listJurusanIds.add(Long.parseLong(q_jurusan)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas.jsp:106");}
    } else if (q_fakultas != null && !q_fakultas.trim().isEmpty()) {
        try {
            Fakultas fakSearch = (Fakultas) sess.get(Fakultas.class, Long.parseLong(q_fakultas));
            if (fakSearch != null) {
                listJurusanIds = sess.createCriteria(Jurusan.class).setProjection(Projections.property("id")).add(Restrictions.eq("fakultas", fakSearch)).list();
                if (listJurusanIds == null) listJurusanIds = new ArrayList<Long>();
                if (listJurusanIds.isEmpty()) listJurusanIds.add(-1L); 
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas.jsp:115");}
    }

    List<Long> listSekolahIds = null;
    if (q_sekolah != null && !q_sekolah.trim().isEmpty()) {
        try { listSekolahIds = new ArrayList<Long>(); listSekolahIds.add(Long.parseLong(q_sekolah)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas.jsp:120");}
    } else if (q_yayasan != null && !q_yayasan.trim().isEmpty()) {
        try {
            Yayasan yaySearch = (Yayasan) sess.get(Yayasan.class, Long.parseLong(q_yayasan));
            if (yaySearch != null) {
                listSekolahIds = sess.createCriteria(Sekolah.class).setProjection(Projections.property("id")).add(Restrictions.eq("yayasan", yaySearch)).list();
                if (listSekolahIds == null) listSekolahIds = new ArrayList<Long>();
                if (listSekolahIds.isEmpty()) listSekolahIds.add(-1L);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas.jsp:129");}
    }

    List<Long> idsDosen = null; boolean kosongDosen = false;
    if (q_dosen != null && !q_dosen.trim().isEmpty()) {
        idsDosen = sess.createCriteria(Dosen.class).setProjection(Projections.property("id")).add(Restrictions.or(Restrictions.ilike("nama", q_dosen, MatchMode.ANYWHERE), Restrictions.ilike("nidn", q_dosen, MatchMode.ANYWHERE))).list();
        if (idsDosen == null || idsDosen.isEmpty()) kosongDosen = true;
    }

    List<Long> idsMhs = null; boolean kosongMhs = false;
    if (q_mahasiswa != null && !q_mahasiswa.trim().isEmpty()) {
        idsMhs = sess.createCriteria(Mahasiswa.class).setProjection(Projections.property("id")).add(Restrictions.or(Restrictions.ilike("nama", q_mahasiswa, MatchMode.ANYWHERE), Restrictions.ilike("nim", q_mahasiswa, MatchMode.ANYWHERE))).list();
        if (idsMhs == null || idsMhs.isEmpty()) kosongMhs = true;
    }

    List<Long> idsGuru = null; boolean kosongGuru = false;
    if (q_guru != null && !q_guru.trim().isEmpty()) {
        idsGuru = sess.createCriteria(Guru.class).setProjection(Projections.property("id")).add(Restrictions.or(Restrictions.ilike("nama", q_guru, MatchMode.ANYWHERE), Restrictions.ilike("nuptk", q_guru, MatchMode.ANYWHERE))).list();
        if (idsGuru == null || idsGuru.isEmpty()) kosongGuru = true;
    }

    List<Long> idsSiswa = null; boolean kosongSiswa = false;
    if (q_siswa != null && !q_siswa.trim().isEmpty()) {
        idsSiswa = sess.createCriteria(Siswa.class).setProjection(Projections.property("id")).add(Restrictions.or(Restrictions.ilike("nama", q_siswa, MatchMode.ANYWHERE), Restrictions.ilike("nomorInduk", q_siswa, MatchMode.ANYWHERE))).list();
        if (idsSiswa == null || idsSiswa.isEmpty()) kosongSiswa = true;
    }

    // =========================================================================
    // 3A. INISIASI CRITERIA: PERTEMUAN
    // =========================================================================
    Criteria criteriaListP = sess.createCriteria(Pertemuan.class);
    Criteria criteriaCountNextP = sess.createCriteria(Pertemuan.class);
    Criteria criteriaCountBackP = sess.createCriteria(Pertemuan.class);
    Criteria[] allCriteriaP = {criteriaListP, criteriaCountNextP, criteriaCountBackP};

    // =========================================================================
    // 3B. INISIASI CRITERIA: TUGAS PERTEMUAN
    // =========================================================================
    Criteria criteriaListT = sess.createCriteria(TugasPertemuan.class).createAlias("pertemuanData", "pertemuan");
    Criteria criteriaCountNextT = sess.createCriteria(TugasPertemuan.class).createAlias("pertemuanData", "pertemuan");
    Criteria criteriaCountBackT = sess.createCriteria(TugasPertemuan.class).createAlias("pertemuanData", "pertemuan");
    Criteria[] allCriteriaT = {criteriaListT, criteriaCountNextT, criteriaCountBackT};

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    boolean joinPerkuliahan = (q_matakuliah != null && !q_matakuliah.trim().isEmpty()) || (q_kelas_kuliah != null && !q_kelas_kuliah.trim().isEmpty());
    boolean joinJadwal = (q_matapelajaran != null && !q_matapelajaran.trim().isEmpty()) || (q_kelas_siswa != null && !q_kelas_siswa.trim().isEmpty());

    // =========================================================================
    // 4A. APLIKASI FILTER KE PERTEMUAN (Tanpa Alias 'pertemuan.')
    // =========================================================================
    for (Criteria c : allCriteriaP) {
        c.add(Restrictions.isNotNull("judultugas"));
        c.add(Restrictions.ne("judultugas", ""));
        c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))); 
        
        if (q_nama_tugas != null && !q_nama_tugas.trim().isEmpty()) c.add(Restrictions.ilike("judultugas", q_nama_tugas, MatchMode.ANYWHERE));

        // FILTER SUMBER KEGIATAN (Direct to Pertemuan)
        for(String fName : arrSumber) {
            String val = request.getParameter("q_has_" + fName);
            if("1".equals(val)) {
                c.add(Restrictions.isNotNull(fName));
            } else if ("0".equals(val)) {
                c.add(Restrictions.isNull(fName));
            }
        }

        if (siswa != null || mahasiswa != null || dosen != null || guru != null) {
            Disjunction dj = Restrictions.disjunction();
            if (siswa != null) dj.add(Restrictions.like("siswas", "%," + siswa.getId() + ",%"));
            if (mahasiswa != null) dj.add(Restrictions.like("mahasiswas", "%," + mahasiswa.getId() + ",%"));
            if (dosen != null) dj.add(Restrictions.like("dosens", "%," + dosen.getId() + ",%"));
            if (guru != null) dj.add(Restrictions.like("gurus", "%," + guru.getId() + ",%"));
            c.add(dj);
        }

        if (jurusan != null) c.add(Restrictions.eq("jurusan", jurusan));
        else if (fakultas != null) { c.createAlias("jurusan", "jStruk", CriteriaSpecification.LEFT_JOIN); c.add(Restrictions.eq("jStruk.fakultas", fakultas)); }

        if (sekolah != null) c.add(Restrictions.eq("sekolah", sekolah));
        else if (yayasan != null) { c.createAlias("sekolah", "sStruk", CriteriaSpecification.LEFT_JOIN); c.add(Restrictions.eq("sStruk.yayasan", yayasan)); }

        if (ta != null && !ta.trim().isEmpty()) c.add(Restrictions.eq("ta", ta));
        if (smt != null && !smt.trim().isEmpty()) c.add(Restrictions.eq("smt", smt));

        if (listJurusanIds != null && !listJurusanIds.isEmpty()) c.add(Restrictions.in("jurusan.id", listJurusanIds));
        if (listSekolahIds != null && !listSekolahIds.isEmpty()) c.add(Restrictions.in("sekolah.id", listSekolahIds));

        if (kosongDosen || kosongMhs || kosongGuru || kosongSiswa) { c.add(Restrictions.sqlRestriction("1=0")); } 
        else {
            if (idsDosen != null) { Disjunction dj = Restrictions.disjunction(); for(Long id: idsDosen) dj.add(Restrictions.like("dosens", "%,"+id+",%")); c.add(dj); }
            if (idsMhs != null) { Disjunction dj = Restrictions.disjunction(); for(Long id: idsMhs) dj.add(Restrictions.like("mahasiswas", "%,"+id+",%")); c.add(dj); }
            if (idsGuru != null) { Disjunction dj = Restrictions.disjunction(); for(Long id: idsGuru) dj.add(Restrictions.like("gurus", "%,"+id+",%")); c.add(dj); }
            if (idsSiswa != null) { Disjunction dj = Restrictions.disjunction(); for(Long id: idsSiswa) dj.add(Restrictions.like("siswas", "%,"+id+",%")); c.add(dj); }
        }

        if (joinPerkuliahan) {
            c.createAlias("perkuliahan", "prk", CriteriaSpecification.LEFT_JOIN);
            if (q_matakuliah != null && !q_matakuliah.trim().isEmpty()) { c.createAlias("prk.matakuliah", "mk", CriteriaSpecification.LEFT_JOIN); c.add(Restrictions.or(Restrictions.ilike("mk.kode", q_matakuliah, MatchMode.ANYWHERE), Restrictions.ilike("mk.nama", q_matakuliah, MatchMode.ANYWHERE))); }
            if (q_kelas_kuliah != null && !q_kelas_kuliah.trim().isEmpty()) { c.add(Restrictions.ilike("prk.kelas", q_kelas_kuliah, MatchMode.ANYWHERE)); }
        }

        if (joinJadwal) {
            c.createAlias("jadwalPelajaran", "jp", CriteriaSpecification.LEFT_JOIN);
            if (q_matapelajaran != null && !q_matapelajaran.trim().isEmpty()) { c.createAlias("jp.matapelajaran", "mp", CriteriaSpecification.LEFT_JOIN); c.add(Restrictions.or(Restrictions.ilike("mp.kode", q_matapelajaran, MatchMode.ANYWHERE), Restrictions.ilike("mp.nama", q_matapelajaran, MatchMode.ANYWHERE))); }
            if (q_kelas_siswa != null && !q_kelas_siswa.trim().isEmpty()) { c.createAlias("jp.kelas", "ks", CriteriaSpecification.LEFT_JOIN); c.add(Restrictions.ilike("ks.nama", q_kelas_siswa, MatchMode.ANYWHERE)); }
        }

        if (q_topik != null && !q_topik.trim().isEmpty()) c.add(Restrictions.ilike("topik", q_topik, MatchMode.ANYWHERE));
        if (q_catatan != null && !q_catatan.trim().isEmpty()) c.add(Restrictions.ilike("catatan", q_catatan, MatchMode.ANYWHERE));
        if (q_indikator != null && !q_indikator.trim().isEmpty()) c.add(Restrictions.ilike("indikator", q_indikator, MatchMode.ANYWHERE));
        if (q_metode != null && !q_metode.trim().isEmpty()) c.add(Restrictions.ilike("metodePembelajaran", q_metode, MatchMode.ANYWHERE));
        if (q_buku1 != null && !q_buku1.trim().isEmpty()) c.add(Restrictions.ilike("bukuRujukan1", q_buku1, MatchMode.ANYWHERE));
        if (q_buku2 != null && !q_buku2.trim().isEmpty()) c.add(Restrictions.ilike("bukuRujukan2", q_buku2, MatchMode.ANYWHERE));
        
        if (q_ruang != null && !q_ruang.trim().isEmpty()){ c.createAlias("ruang", "ruangAlias", CriteriaSpecification.LEFT_JOIN); c.add(Restrictions.or(Restrictions.ilike("ruangAlias.kode", q_ruang, MatchMode.ANYWHERE), Restrictions.ilike("ruangAlias.nama", q_ruang, MatchMode.ANYWHERE))); }

        if (q_tgl_mulai != null && !q_tgl_mulai.trim().isEmpty()) { try { c.add(Restrictions.ge("mulai", sdf.parse(q_tgl_mulai))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas.jsp:246");} }
        if (q_tgl_selesai != null && !q_tgl_selesai.trim().isEmpty()) { try { Date tSelesai = sdf.parse(q_tgl_selesai); tSelesai.setHours(23); tSelesai.setMinutes(59); tSelesai.setSeconds(59); c.add(Restrictions.le("mulai", tSelesai)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas.jsp:247");} }
    }

    // =========================================================================
    // 4B. APLIKASI FILTER KE TUGAS PERTEMUAN (Menggunakan Alias 'pertemuan.')
    // =========================================================================
    for (Criteria c : allCriteriaT) {
        c.add(Restrictions.isNotNull("judultugas"));
        c.add(Restrictions.ne("judultugas", ""));
        c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))); 
        c.add(Restrictions.or(Restrictions.isNull("pertemuan.aktif"), Restrictions.eq("pertemuan.aktif", true))); 
        
        if (q_nama_tugas != null && !q_nama_tugas.trim().isEmpty()) c.add(Restrictions.ilike("judultugas", q_nama_tugas, MatchMode.ANYWHERE));

        // FILTER SUMBER KEGIATAN (Dengan namespace "pertemuan." karena alias join)
        for(String fName : arrSumber) {
            String val = request.getParameter("q_has_" + fName);
            if("1".equals(val)) {
                c.add(Restrictions.isNotNull("pertemuan." + fName));
            } else if ("0".equals(val)) {
                c.add(Restrictions.isNull("pertemuan." + fName));
            }
        }

        if (siswa != null || mahasiswa != null || dosen != null || guru != null) {
            Disjunction dj = Restrictions.disjunction();
            if (siswa != null) dj.add(Restrictions.like("pertemuan.siswas", "%," + siswa.getId() + ",%"));
            if (mahasiswa != null) dj.add(Restrictions.like("pertemuan.mahasiswas", "%," + mahasiswa.getId() + ",%"));
            if (dosen != null) dj.add(Restrictions.like("pertemuan.dosens", "%," + dosen.getId() + ",%"));
            if (guru != null) dj.add(Restrictions.like("pertemuan.gurus", "%," + guru.getId() + ",%"));
            c.add(dj);
        }

        if (jurusan != null) c.add(Restrictions.eq("pertemuan.jurusan", jurusan));
        else if (fakultas != null) { c.createAlias("pertemuan.jurusan", "jStruk", CriteriaSpecification.LEFT_JOIN); c.add(Restrictions.eq("jStruk.fakultas", fakultas)); }

        if (sekolah != null) c.add(Restrictions.eq("pertemuan.sekolah", sekolah));
        else if (yayasan != null) { c.createAlias("pertemuan.sekolah", "sStruk", CriteriaSpecification.LEFT_JOIN); c.add(Restrictions.eq("sStruk.yayasan", yayasan)); }

        if (ta != null && !ta.trim().isEmpty()) c.add(Restrictions.eq("pertemuan.ta", ta));
        if (smt != null && !smt.trim().isEmpty()) c.add(Restrictions.eq("pertemuan.smt", smt));

        if (listJurusanIds != null && !listJurusanIds.isEmpty()) c.add(Restrictions.in("pertemuan.jurusan.id", listJurusanIds));
        if (listSekolahIds != null && !listSekolahIds.isEmpty()) c.add(Restrictions.in("pertemuan.sekolah.id", listSekolahIds));

        if (kosongDosen || kosongMhs || kosongGuru || kosongSiswa) { c.add(Restrictions.sqlRestriction("1=0")); } 
        else {
            if (idsDosen != null) { Disjunction dj = Restrictions.disjunction(); for(Long id: idsDosen) dj.add(Restrictions.like("pertemuan.dosens", "%,"+id+",%")); c.add(dj); }
            if (idsMhs != null) { Disjunction dj = Restrictions.disjunction(); for(Long id: idsMhs) dj.add(Restrictions.like("pertemuan.mahasiswas", "%,"+id+",%")); c.add(dj); }
            if (idsGuru != null) { Disjunction dj = Restrictions.disjunction(); for(Long id: idsGuru) dj.add(Restrictions.like("pertemuan.gurus", "%,"+id+",%")); c.add(dj); }
            if (idsSiswa != null) { Disjunction dj = Restrictions.disjunction(); for(Long id: idsSiswa) dj.add(Restrictions.like("pertemuan.siswas", "%,"+id+",%")); c.add(dj); }
        }

        if (joinPerkuliahan) {
            c.createAlias("pertemuan.perkuliahan", "prk", CriteriaSpecification.LEFT_JOIN);
            if (q_matakuliah != null && !q_matakuliah.trim().isEmpty()) { c.createAlias("prk.matakuliah", "mk", CriteriaSpecification.LEFT_JOIN); c.add(Restrictions.or(Restrictions.ilike("mk.kode", q_matakuliah, MatchMode.ANYWHERE), Restrictions.ilike("mk.nama", q_matakuliah, MatchMode.ANYWHERE))); }
            if (q_kelas_kuliah != null && !q_kelas_kuliah.trim().isEmpty()) { c.add(Restrictions.ilike("prk.kelas", q_kelas_kuliah, MatchMode.ANYWHERE)); }
        }

        if (joinJadwal) {
            c.createAlias("pertemuan.jadwalPelajaran", "jp", CriteriaSpecification.LEFT_JOIN);
            if (q_matapelajaran != null && !q_matapelajaran.trim().isEmpty()) { c.createAlias("jp.matapelajaran", "mp", CriteriaSpecification.LEFT_JOIN); c.add(Restrictions.or(Restrictions.ilike("mp.kode", q_matapelajaran, MatchMode.ANYWHERE), Restrictions.ilike("mp.nama", q_matapelajaran, MatchMode.ANYWHERE))); }
            if (q_kelas_siswa != null && !q_kelas_siswa.trim().isEmpty()) { c.createAlias("jp.kelas", "ks", CriteriaSpecification.LEFT_JOIN); c.add(Restrictions.ilike("ks.nama", q_kelas_siswa, MatchMode.ANYWHERE)); }
        }

        if (q_topik != null && !q_topik.trim().isEmpty()) c.add(Restrictions.ilike("pertemuan.topik", q_topik, MatchMode.ANYWHERE));
        if (q_catatan != null && !q_catatan.trim().isEmpty()) c.add(Restrictions.ilike("pertemuan.catatan", q_catatan, MatchMode.ANYWHERE));
        if (q_indikator != null && !q_indikator.trim().isEmpty()) c.add(Restrictions.ilike("pertemuan.indikator", q_indikator, MatchMode.ANYWHERE));
        if (q_metode != null && !q_metode.trim().isEmpty()) c.add(Restrictions.ilike("pertemuan.metodePembelajaran", q_metode, MatchMode.ANYWHERE));
        if (q_buku1 != null && !q_buku1.trim().isEmpty()) c.add(Restrictions.ilike("pertemuan.bukuRujukan1", q_buku1, MatchMode.ANYWHERE));
        if (q_buku2 != null && !q_buku2.trim().isEmpty()) c.add(Restrictions.ilike("pertemuan.bukuRujukan2", q_buku2, MatchMode.ANYWHERE));
        
        if (q_ruang != null && !q_ruang.trim().isEmpty()){ c.createAlias("pertemuan.ruang", "ruangAlias", CriteriaSpecification.LEFT_JOIN); c.add(Restrictions.or(Restrictions.ilike("ruangAlias.kode", q_ruang, MatchMode.ANYWHERE), Restrictions.ilike("ruangAlias.nama", q_ruang, MatchMode.ANYWHERE))); }

        if (q_tgl_mulai != null && !q_tgl_mulai.trim().isEmpty()) { try { c.add(Restrictions.ge("mulai", sdf.parse(q_tgl_mulai))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas.jsp:321");} }
        if (q_tgl_selesai != null && !q_tgl_selesai.trim().isEmpty()) { try { Date tSelesai = sdf.parse(q_tgl_selesai); tSelesai.setHours(23); tSelesai.setMinutes(59); tSelesai.setSeconds(59); c.add(Restrictions.le("mulai", tSelesai)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas.jsp:322");} }
    }

    // =========================================================================
    // 5. EKSEKUSI KUERI & IN-MEMORY UNION ALL (DUAL-QUERY MERGE)
    // =========================================================================
    int fetchLimit = startIdx + limitRows;
    final boolean isPrev = "prev".equalsIgnoreCase(dir);

    // Fetch Pertemuan DENGAN PEMBATAS refDate MUTLAK
    criteriaListP.setProjection(Projections.projectionList().add(Projections.property("id")).add(Projections.property("mulai")));
    criteriaListP.setFirstResult(0);
    criteriaListP.setMaxResults(fetchLimit);
    if (isPrev) {
        criteriaListP.add(Restrictions.lt("mulai", refDate));
        criteriaListP.addOrder(Order.desc("mulai"));
    } else {
        criteriaListP.add(Restrictions.ge("mulai", refDate));
        criteriaListP.addOrder(Order.asc("mulai"));
    }
    List<Object[]> resPRaw = criteriaListP.list();

    // Fetch TugasPertemuan DENGAN PEMBATAS refDate MUTLAK
    criteriaListT.setProjection(Projections.projectionList().add(Projections.property("id")).add(Projections.property("mulai")));
    criteriaListT.setFirstResult(0);
    criteriaListT.setMaxResults(fetchLimit);
    if (isPrev) {
        criteriaListT.add(Restrictions.lt("mulai", refDate));
        criteriaListT.addOrder(Order.desc("mulai"));
    } else {
        criteriaListT.add(Restrictions.ge("mulai", refDate));
        criteriaListT.addOrder(Order.asc("mulai"));
    }
    List<Object[]> resTRaw = criteriaListT.list();

    // Union All In-Memory dengan Penanda Simple Class Name
    List<Object[]> mergedList = new ArrayList<Object[]>();
    
    if (resPRaw != null) {
        for (Object[] row : resPRaw) {
            mergedList.add(new Object[]{ row[0], row[1], Pertemuan.class.getSimpleName() });
        }
    }
    if (resTRaw != null) {
        for (Object[] row : resTRaw) {
            mergedList.add(new Object[]{ row[0], row[1], TugasPertemuan.class.getSimpleName() });
        }
    }

    // Pengurutan Gabungan berdasarkan Tanggal (Elemen [1])
    Collections.sort(mergedList, new java.util.Comparator<Object[]>() {
        public int compare(Object[] o1, Object[] o2) {
            Date d1 = (Date) o1[1];
            Date d2 = (Date) o2[1];
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return isPrev ? 1 : -1;
            if (d2 == null) return isPrev ? -1 : 1;
            return isPrev ? d2.compareTo(d1) : d1.compareTo(d2);
        }
    });

    // Paging Extraction (Slice) & Format Penggabungan ID_CLASSNAME
    JSONArray dataArray = new JSONArray();
    int endIdx = Math.min(startIdx + limitRows, mergedList.size());
    for (int i = startIdx; i < endIdx; i++) {
        Object[] item = mergedList.get(i);
        String combinedIdentifier = item[0].toString() + "_" + item[2].toString();
        dataArray.put(combinedIdentifier);
    }

    // =========================================================================
    // 6. PERHITUNGAN PAGINASI (GABUNGAN KEDUA TABEL)
    // =========================================================================
    
    // Pertemuan Counts
    criteriaCountNextP.setProjection(Projections.rowCount());
    criteriaCountNextP.add(Restrictions.ge("mulai", sekarang));
    Long countNextP = (Long) criteriaCountNextP.uniqueResult();

    criteriaCountBackP.setProjection(Projections.rowCount());
    criteriaCountBackP.add(Restrictions.lt("mulai", sekarang));
    Long countBackP = (Long) criteriaCountBackP.uniqueResult();

    // TugasPertemuan Counts
    criteriaCountNextT.setProjection(Projections.rowCount());
    criteriaCountNextT.add(Restrictions.ge("mulai", sekarang));
    Long countNextT = (Long) criteriaCountNextT.uniqueResult();

    criteriaCountBackT.setProjection(Projections.rowCount());
    criteriaCountBackT.add(Restrictions.lt("mulai", sekarang));
    Long countBackT = (Long) criteriaCountBackT.uniqueResult();

    Long totalNext = (countNextP != null ? countNextP : 0) + (countNextT != null ? countNextT : 0);
    Long totalBack = (countBackP != null ? countBackP : 0) + (countBackT != null ? countBackT : 0);

    responseJson.put("data", dataArray);
    responseJson.put("countNext", totalNext);
    responseJson.put("countBack", totalBack);

} catch(Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas.jsp:422");
    responseJson.put("data", new JSONArray());
    responseJson.put("countNext", 0);
    responseJson.put("countBack", 0);
} finally {
    try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_tugas.jsp:427");}
    ais.common.ElearningSessionUtil.closeQuietly(sess);
    HibernateUtil.closeSessionQuietly(sess);
}

out.print(responseJson.toString());
out.flush();
%>