<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.CriteriaSpecification"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Disjunction"%>
<%@page import="org.hibernate.criterion.DetachedCriteria"%>
<%@page import="org.hibernate.criterion.Subqueries"%>
<%@page import="ais.database.model.file.PertemuanFileContent"%>
<%@page import="ais.database.model.streaming.AudioPertemuan"%>
<%@page import="ais.database.model.streaming.VideoPertemuan"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.hibernate.StreamingHibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.database.model.sekolah.*"%>
<%@page import="ais.database.model.kkn.*"%>
<%@page import="ais.database.model.pkl.*"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonMedia"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.setStatus(401); 
        return;
    }

    String kategori = request.getParameter("kategori");
    if (kategori == null || kategori.isEmpty()) kategori = "perkuliahan";

    String ta = request.getParameter("q_ta");
    String smt = request.getParameter("q_smt");
    String q_cari = request.getParameter("q_cari");

    int startIdx = Integer.parseInt(request.getParameter("mulai") == null ? "0" : request.getParameter("mulai"));
    int limitRows = Integer.parseInt(request.getParameter("banyak") == null ? "10" : request.getParameter("banyak"));

    String q_fakultas = request.getParameter("q_fakultas");
    String q_jurusan = request.getParameter("q_jurusan");
    String q_yayasan = request.getParameter("q_yayasan");
    String q_sekolah = request.getParameter("q_sekolah");

    boolean isSiswa = tbmuser.getSiswa() != null;
    boolean isGuru = tbmuser.ambilGuru() != null;
    boolean isMahasiswa = tbmuser.getMahasiswa() != null;
    boolean isDosen = tbmuser.ambilDosen() != null;
    boolean isSivitas = isSiswa || isGuru || isMahasiswa || isDosen;

    boolean[] ptYa = Common.chekPtAtauSekolah(tbmuser);
    boolean apakahPerguruanTInggi = ptYa[0];
    boolean apakahSekolah = ptYa[1];

    if (!isSivitas) {
        if (tbmuser.ambilJurusan() != null) q_jurusan = tbmuser.ambilJurusan().getId().toString();
        if (tbmuser.ambilFakultas() != null) q_fakultas = tbmuser.ambilFakultas().getId().toString();
        if (tbmuser.ambilSekolah() != null) q_sekolah = tbmuser.ambilSekolah().getId().toString();
        if (tbmuser.ambilYayasan() != null) q_yayasan = tbmuser.ambilYayasan().getId().toString();
    } else {
        q_jurusan = null; q_fakultas = null; q_sekolah = null; q_yayasan = null;
    }

    Session sess = HibernateUtil.openSession();
    Session sessStream = null;
    try { sessStream = StreamingHibernateUtil.getInstance().openSession(); } catch (Exception exStr) { ais.common.ErrorAuditUtil.record(exStr, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:70");}
    JSONArray dataArray = new JSONArray();
    long totalRows = 0;

    try {
        List<Long> listJurusanIds = null;
        if (q_jurusan != null && !q_jurusan.trim().isEmpty() && apakahPerguruanTInggi) {
            try { listJurusanIds = new ArrayList<Long>(); listJurusanIds.add(Long.parseLong(q_jurusan)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:77");}
        } else if (q_fakultas != null && !q_fakultas.trim().isEmpty() && apakahPerguruanTInggi) {
            try {
                Fakultas fakSearch = (Fakultas) sess.get(Fakultas.class, Long.parseLong(q_fakultas));
                if (fakSearch != null) {
                    listJurusanIds = sess.createCriteria(Jurusan.class).setProjection(Projections.property("id")).add(Restrictions.eq("fakultas", fakSearch)).list();
                    if (listJurusanIds == null) listJurusanIds = new ArrayList<Long>();
                    if (listJurusanIds.isEmpty()) listJurusanIds.add(-1L);
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:86");}
        }

        List<Long> listSekolahIds = null;
        if (q_sekolah != null && !q_sekolah.trim().isEmpty() && apakahSekolah) {
            try { listSekolahIds = new ArrayList<Long>(); listSekolahIds.add(Long.parseLong(q_sekolah)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:91");}
        } else if (q_yayasan != null && !q_yayasan.trim().isEmpty() && apakahSekolah) {
            try {
                Yayasan yaySearch = (Yayasan) sess.get(Yayasan.class, Long.parseLong(q_yayasan));
                if (yaySearch != null) {
                    listSekolahIds = sess.createCriteria(Sekolah.class).setProjection(Projections.property("id")).add(Restrictions.eq("yayasan", yaySearch)).list();
                    if (listSekolahIds == null) listSekolahIds = new ArrayList<Long>();
                    if (listSekolahIds.isEmpty()) listSekolahIds.add(-1L);
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:100");}
        }

        Criteria c = null;
        if (kategori.equals("perkuliahan") && apakahPerguruanTInggi) {
            c = sess.createCriteria(Perkuliahan.class);
            if (q_cari != null && !q_cari.trim().isEmpty()) {
                c.createAlias("matakuliah", "mk", CriteriaSpecification.LEFT_JOIN);
                c.add(Restrictions.ilike("mk.nama", q_cari, MatchMode.ANYWHERE));
            }
            if (ta != null && !ta.isEmpty()) c.add(Restrictions.eq("tahunAjaran", ta));
            if (smt != null && !smt.isEmpty()) {
                if(smt.equals(Perkuliahan.GANJIL)) c.add(Restrictions.sqlRestriction("this_.semester % 2 = 1"));
                else if(smt.equals(Perkuliahan.GENAP)) c.add(Restrictions.sqlRestriction("this_.semester % 2 = 0"));
            }
            if (isMahasiswa) {
                DetachedCriteria subCriteria = DetachedCriteria.forClass(Detailperkuliahan.class, "dp");
                subCriteria.add(Restrictions.eq("dp.mahasiswa", tbmuser.getMahasiswa()));
                subCriteria.add(Restrictions.eq("dp.persetujuan", Detailperkuliahan.DISETUJUI));
                subCriteria.setProjection(Projections.property("dp.perkuliahan.id"));
                c.add(Subqueries.propertyIn("id", subCriteria));
            } else if (isDosen) {
                Dosen d = tbmuser.ambilDosen();
                Disjunction djDosen = Restrictions.disjunction();
                djDosen.add(Restrictions.eq("dosen1", d));
                for(int i=2; i<=10; i++) djDosen.add(Restrictions.eq("dosen"+i, d));
                c.add(djDosen);
            }
            if (listJurusanIds != null && !listJurusanIds.isEmpty()) {
                if (!c.toString().contains("matakuliah")) c.createAlias("matakuliah", "mk", CriteriaSpecification.LEFT_JOIN);
                c.add(Restrictions.in("mk.jurusan.id", listJurusanIds));
            }
        } 
        else if (kategori.equals("jadwal_pelajaran") && apakahSekolah) {
            c = sess.createCriteria(JadwalPelajaran.class);
            if (q_cari != null && !q_cari.trim().isEmpty()) {
                c.createAlias("matapelajaran", "mp", CriteriaSpecification.LEFT_JOIN);
                c.add(Restrictions.ilike("mp.nama", q_cari, MatchMode.ANYWHERE));
            }
            if (ta != null && !ta.isEmpty()) c.add(Restrictions.eq("tahunAjaran", ta));
            if (smt != null && !smt.isEmpty()) {
                try { c.add(Restrictions.eq("semester", Integer.parseInt(smt))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:141");}
            }
            if (listSekolahIds != null && !listSekolahIds.isEmpty()) {
                c.createAlias("kelas", "kls", CriteriaSpecification.LEFT_JOIN);
                c.add(Restrictions.in("kls.sekolah.id", listSekolahIds));
            }
        } 
        else if (kategori.equals("tugas_akhir") && apakahPerguruanTInggi) {
            c = sess.createCriteria(MahasiswaRequestTugasAkhir.class);
            if (q_cari != null && !q_cari.trim().isEmpty()) c.add(Restrictions.ilike("judul", q_cari, MatchMode.ANYWHERE));
            if (ta != null && !ta.isEmpty()) c.add(Restrictions.eq("tahunAkademik", ta));
            if (isMahasiswa) c.add(Restrictions.eq("mahasiswa", tbmuser.getMahasiswa()));
            if (listJurusanIds != null && !listJurusanIds.isEmpty()) {
                c.createAlias("mahasiswa", "mhs", CriteriaSpecification.LEFT_JOIN);
                c.add(Restrictions.in("mhs.jurusan.id", listJurusanIds));
            }
        } 
        else if (kategori.equals("skripsi") && apakahPerguruanTInggi) {
            c = sess.createCriteria(Skripsi.class);
            if (q_cari != null && !q_cari.trim().isEmpty()) c.add(Restrictions.ilike("judul", q_cari, MatchMode.ANYWHERE));
            if (ta != null && !ta.isEmpty()) c.add(Restrictions.eq("tahunAkademik", ta));
            if (isMahasiswa) c.add(Restrictions.eq("mahasiswa", tbmuser.getMahasiswa()));
            if (listJurusanIds != null && !listJurusanIds.isEmpty()) {
                c.createAlias("mahasiswa", "mhs", CriteriaSpecification.LEFT_JOIN);
                c.add(Restrictions.in("mhs.jurusan.id", listJurusanIds));
            }
        } 
        else if (kategori.equals("kkn") && apakahPerguruanTInggi) {
            c = sess.createCriteria(KelompokKkn.class);
            if (q_cari != null && !q_cari.trim().isEmpty()) c.add(Restrictions.ilike("nama_kelompok", q_cari, MatchMode.ANYWHERE));
        } 
        else if (kategori.equals("pkl") && apakahPerguruanTInggi) {
            c = sess.createCriteria(KelompokPkl.class);
            if (q_cari != null && !q_cari.trim().isEmpty()) c.add(Restrictions.ilike("nama_kelompok", q_cari, MatchMode.ANYWHERE));
        } 
        else if (kategori.equals("krs") && apakahPerguruanTInggi) {
            c = sess.createCriteria(KrsMahasiswa.class);
            if (q_cari != null && !q_cari.trim().isEmpty()) c.add(Restrictions.ilike("nama", q_cari, MatchMode.ANYWHERE));
            if (ta != null && !ta.isEmpty()) c.add(Restrictions.eq("tahunAkademik", ta));
            if (isMahasiswa) c.add(Restrictions.eq("mahasiswa", tbmuser.getMahasiswa()));
            if (listJurusanIds != null && !listJurusanIds.isEmpty()) {
                c.createAlias("mahasiswa", "mhs", CriteriaSpecification.LEFT_JOIN);
                c.add(Restrictions.in("mhs.jurusan.id", listJurusanIds));
            }
        } 
        else if (kategori.equals("kegiatan")) {
            c = sess.createCriteria(FormulirKegiatan.class);
            if (q_cari != null && !q_cari.trim().isEmpty()) c.add(Restrictions.ilike("nama", q_cari, MatchMode.ANYWHERE));
        } 
        else if (kategori.equals("les") && apakahSekolah) {
            c = sess.createCriteria(KelasLesSiswa.class);
            if (q_cari != null && !q_cari.trim().isEmpty()) c.add(Restrictions.ilike("nama", q_cari, MatchMode.ANYWHERE));
        }

        if (c != null) {
            c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
            c.setProjection(Projections.rowCount());
            Long rowCount = (Long) c.uniqueResult();
            if (rowCount != null) totalRows = rowCount;

            c.setProjection(null);
            c.setResultTransformer(Criteria.ROOT_ENTITY);
            c.setFirstResult(startIdx);
            c.setMaxResults(limitRows);
            c.addOrder(Order.desc("id"));
            List<?> results = c.list();

            for (Object obj : results) {
                VOPembelajaran vop = (VOPembelajaran) obj;
                JSONObject jo = new JSONObject();
                jo.put("id", vop.getId());
                
                String title = ""; String sub1 = ""; String sub2 = ""; String sub3 = "";
                
                if (vop instanceof Perkuliahan) {
                    Perkuliahan p = (Perkuliahan) vop;
                    title = p.getMatakuliah() != null ? p.getMatakuliah().getNama() : "Tanpa Matakuliah";
                    sub1 = (p.getMatakuliah() != null ? p.getMatakuliah().getSks() + " SKS" : "0 SKS") + " - " + (p.getMatakuliah() != null ? p.getMatakuliah().getKode() : "");
                    sub2 = (p.getHari() != null ? p.getHari() : "") + " | " + (p.getWaktuMulai() != null ? p.getWaktuMulai() : "00.00") + "-" + (p.getWaktuSelesai() != null ? p.getWaktuSelesai() : "00.00") + " | " + (p.getRuang() != null ? p.getRuang().getNama() : "");
                    sub3 = "TA: " + (p.getTahunAjaran() != null ? p.getTahunAjaran() : "") + " | Smt: " + p.getSemester() + (p.getKelas() != null ? " | Kls: " + p.getKelas() : "");
                } else if (vop instanceof JadwalPelajaran) {
                    JadwalPelajaran p = (JadwalPelajaran) vop;
                    title = p.getMatapelajaran() != null ? p.getMatapelajaran().getNama() : "Tanpa Matapelajaran";
                    sub1 = "Smt: " + p.getSemester(); sub2 = p.getHari() != null ? p.getHari() : ""; sub3 = "TA: " + (p.getTahunAjaran() != null ? p.getTahunAjaran() : "");
                } else if (vop instanceof MahasiswaRequestTugasAkhir) {
                    MahasiswaRequestTugasAkhir p = (MahasiswaRequestTugasAkhir) vop;
                    title = p.getJudul() != null && !p.getJudul().isEmpty() ? p.getJudul() : "Tanpa Judul";
                    sub1 = p.getNama() != null ? p.getNama() : ""; sub2 = p.getStatus() != null ? p.getStatus() : ""; sub3 = "TA: " + (p.getTahunAkademik() != null ? p.getTahunAkademik() : "") + " | Smt: " + p.getSemester();
                } else if (vop instanceof Skripsi) {
                    Skripsi p = (Skripsi) vop;
                    title = p.getJudul() != null && !p.getJudul().isEmpty() ? p.getJudul() : "Tanpa Judul";
                    sub1 = p.getMahasiswa() != null ? p.getMahasiswa().getNama() : ""; sub2 = p.getTahunAkademik() != null ? p.getTahunAkademik() : ""; sub3 = "Smt: " + p.getSemester();
                } else if (vop instanceof KelompokKkn) {
                    KelompokKkn p = (KelompokKkn) vop; title = p.getNama() != null ? p.getNama() : "Tanpa Nama Kelompok";
                } else if (vop instanceof KelompokPkl) {
                    KelompokPkl p = (KelompokPkl) vop; title = p.getNama() != null ? p.getNama() : "Tanpa Nama Kelompok";
                } else if (vop instanceof KrsMahasiswa) {
                    KrsMahasiswa p = (KrsMahasiswa) vop; title = p.getNama() != null ? p.getNama() : "KRS Mahasiswa";
                    sub1 = "Smt: " + p.getSemester(); sub2 = p.getCatatan() != null ? p.getCatatan() : "";
                } else if (vop instanceof FormulirKegiatan) {
                    FormulirKegiatan p = (FormulirKegiatan) vop; title = p.getNama() != null ? p.getNama() : "Kegiatan Akademik"; sub1 = p.getJenisKegiatan() != null ? p.getJenisKegiatan() : "";
                } else if (vop instanceof KelasLesSiswa) {
                    KelasLesSiswa p = (KelasLesSiswa) vop; title = p.getNama() != null ? p.getNama() : "Kelas Les"; sub1 = p.getKeterangan() != null ? p.getKeterangan() : "";
                }
                
                jo.put("title", title);
                jo.put("subtitle1", sub1);
                jo.put("subtitle2", sub2);
                jo.put("subtitle3", sub3);
                
                JSONArray pengajars = new JSONArray();
                try {
                    List<Long> dIds = vop.populateDosenBuId();
                    if (dIds != null) {
                        for(Long idD : dIds) {
                            Dosen dd = (Dosen) sess.get(Dosen.class, idD);
                            if (dd != null) {
                                JSONObject pd = new JSONObject();
                                pd.put("nama", dd.getNama());
                                try { pd.put("foto", CommonMedia.getUrlFotoPengguna(new Tbmuser(dd))); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:260");}
                                pengajars.put(pd);
                            }
                        }
                    }
                    List<Long> gIds = vop.populateGuruBuId();
                    if (gIds != null) {
                        for(Long idG : gIds) {
                            Guru gg = (Guru) sess.get(Guru.class, idG);
                            if (gg != null) {
                                JSONObject pg = new JSONObject();
                                pg.put("nama", gg.getNama());
                                try { pg.put("foto", CommonMedia.getUrlFotoPengguna(new Tbmuser(gg))); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:272");}
                                pengajars.put(pg);
                            }
                        }
                    }
                    if (pengajars.length() == 0) {
                        List<Long> mIds = vop.ambilMahasiswaById();
                        if (mIds != null && !mIds.isEmpty()) {
                            Mahasiswa mm = (Mahasiswa) sess.get(Mahasiswa.class, mIds.get(0));
                            if (mm != null) {
                                JSONObject pm = new JSONObject(); pm.put("nama", mm.getNama());
                                try { pm.put("foto", CommonMedia.getUrlFotoPengguna(new Tbmuser(mm))); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:283");}
                                pengajars.put(pm);
                            }
                        } else {
                            List<Long> sIds = vop.ambilSiswaById();
                            if (sIds != null && !sIds.isEmpty()) {
                                Siswa ss = (Siswa) sess.get(Siswa.class, sIds.get(0));
                                if (ss != null) {
                                    JSONObject ps = new JSONObject(); ps.put("nama", ss.getNama());
                                    try { ps.put("foto", CommonMedia.getUrlFotoPengguna(new Tbmuser(ss))); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:292");}
                                    pengajars.put(ps);
                                }
                            }
                        }
                    }
                } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:298");}
                jo.put("pengajars", pengajars);
                
                int mhsCount = 0, pertCount = 0;
                try {
                    List<Long> mIds = vop.ambilMahasiswaById(); List<Long> sIds = vop.ambilSiswaById();
                    mhsCount = (mIds != null ? mIds.size() : 0) + (sIds != null ? sIds.size() : 0);
                    Integer pt = vop.ambilJumlahPertemuan(); if(pt != null) pertCount = pt;
                } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:306");}
                
                jo.put("count_mahasiswa", mhsCount);
                jo.put("count_pertemuan", pertCount);

                String propName = null;
                if (vop instanceof Perkuliahan) propName = "perkuliahan";
                else if (vop instanceof JadwalPelajaran) propName = "jadwalPelajaran";
                else if (vop instanceof MahasiswaRequestTugasAkhir) propName = "mahasiswaRequestTugasAkhir";
                else if (vop instanceof Skripsi) propName = "skripsi";
                else if (vop instanceof KelompokKkn) propName = "kelompokKkn";
                else if (vop instanceof KelompokPkl) propName = "kelompokPkl";
                else if (vop instanceof KrsMahasiswa) propName = "krsMahasiswa";
                else if (vop instanceof FormulirKegiatan) propName = "formulirKegiatan";
                else if (vop instanceof KelasLesSiswa) propName = "kelasLesSiswa";
                
                jo.put("jenis_vop", vop.getClass().getSimpleName());
                jo.put("prop_name", propName != null ? propName : "");
                
                // === TAMBAHAN UNTUK OBE ===
                boolean isObe = false;
                String idKur = "";
                if (vop instanceof Perkuliahan) {
                    Perkuliahan p = (Perkuliahan) vop;
                    if(p.getKurikulum() != null) {
                        isObe = p.getKurikulum().apakahObe(p.getTahunAjaran(), p.getGanjilGenap());
                    }
                    if(p.ambilKurikulumPunyaMatakuliah() != null) {
                        idKur = p.ambilKurikulumPunyaMatakuliah().getId().toString();
                    }
                }
                jo.put("is_obe", isObe);
                jo.put("id_kur", idKur);
                // ==========================
                
                long cUjian = 0, cTugas = 0, cTugasKlp = 0;
                if (propName != null) {
                    try {
                        Long lUjian = (Long) sess.createCriteria(PertemuanPunyaUjian.class).createAlias("pertemuan", "p").add(Restrictions.eq("p." + propName, vop)).setProjection(Projections.rowCount()).uniqueResult();
                        if (lUjian != null) cUjian = lUjian;
                        
                        Long lTugas1 = (Long) sess.createCriteria(Pertemuan.class).add(Restrictions.eq(propName, vop)).add(Restrictions.isNotNull("judultugas")).add(Restrictions.ne("judultugas", "")).setProjection(Projections.rowCount()).uniqueResult();
                        Long lTugas2 = (Long) sess.createCriteria(TugasPertemuan.class).createAlias("pertemuanData", "p").add(Restrictions.eq("p." + propName, vop)).add(Restrictions.isNotNull("judultugas")).add(Restrictions.ne("judultugas", "")).setProjection(Projections.rowCount()).uniqueResult();
                        cTugas = (lTugas1 != null ? lTugas1 : 0) + (lTugas2 != null ? lTugas2 : 0);
                        
                        Long lTugasKlp = (Long) sess.createCriteria(TugasKelompok.class).createAlias("pertemuanData", "p").add(Restrictions.eq("p." + propName, vop)).setProjection(Projections.rowCount()).uniqueResult();
                        if (lTugasKlp != null) cTugasKlp = lTugasKlp;
                    } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:353");}
                }
                
                jo.put("count_ujian", cUjian); jo.put("count_tugas", cTugas); jo.put("count_tugas_kelompok", cTugasKlp);
                // ── Pertemuan IDs (diperlukan untuk query streaming DB) ────────────────
                List<Long> pertIds = null;
                if (propName != null) {
                    try {
                        pertIds = (List<Long>) sess.createCriteria(Pertemuan.class)
                            .add(Restrictions.eq(propName, vop))
                            .setProjection(Projections.property("id"))
                            .list();
                    } catch (Exception exP) { ais.common.ErrorAuditUtil.record(exP, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:365");}
                }

                // ── Diskusi (PertemuanPunyaDiskusi → pertemuan.propName) ───────────────
                long cDiskusi = 0;
                if (propName != null) {
                    try {
                        Long lDiskusi = (Long) sess.createCriteria(PertemuanPunyaDiskusi.class)
                            .createAlias("pertemuan", "pJoin")
                            .add(Restrictions.eq("pJoin." + propName, vop))
                            .setProjection(Projections.rowCount())
                            .uniqueResult();
                        if (lDiskusi != null) cDiskusi = lDiskusi;
                    } catch (Exception exD) { ais.common.ErrorAuditUtil.record(exD, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:378");}
                }

                // ── Sesuai RPS (Pertemuan.sesuai = true) ──────────────────────────────
                long cSesuaiRps = 0;
                if (propName != null) {
                    try {
                        Long lSesuai = (Long) sess.createCriteria(Pertemuan.class)
                            .add(Restrictions.eq(propName, vop))
                            .add(Restrictions.eq("sesuai", Boolean.TRUE))
                            .setProjection(Projections.rowCount())
                            .uniqueResult();
                        if (lSesuai != null) cSesuaiRps = lSesuai;
                    } catch (Exception exR) { ais.common.ErrorAuditUtil.record(exR, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:391");}
                }

                // ── Buku Referensi (Pertemuan dgn bukuRujukan1/2 terisi) ───────────────
                long cBukuRef = 0;
                if (propName != null) {
                    try {
                        Long lBukuRef = (Long) sess.createCriteria(Pertemuan.class)
                            .add(Restrictions.eq(propName, vop))
                            .add(Restrictions.or(
                                Restrictions.and(Restrictions.isNotNull("bukuRujukan1"), Restrictions.ne("bukuRujukan1", "")),
                                Restrictions.and(Restrictions.isNotNull("bukuRujukan2"), Restrictions.ne("bukuRujukan2", ""))
                            ))
                            .setProjection(Projections.rowCount())
                            .uniqueResult();
                        if (lBukuRef != null) cBukuRef = lBukuRef;
                    } catch (Exception exBr) { ais.common.ErrorAuditUtil.record(exBr, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:407");}
                }

                // ── Buku Ajar (MatakuliahPunyaBukuBahanAjar → matakuliah) ─────────────
                long cBukuAjar = 0;
                if (vop instanceof Perkuliahan) {
                    Perkuliahan pkBuku = (Perkuliahan) vop;
                    try {
                        if (pkBuku.getMatakuliah() != null) {
                            Long lBukuAjar = (Long) sess.createCriteria(MatakuliahPunyaBukuBahanAjar.class)
                                .add(Restrictions.eq("matakuliah", pkBuku.getMatakuliah()))
                                .setProjection(Projections.rowCount())
                                .uniqueResult();
                            if (lBukuAjar != null) cBukuAjar = lBukuAjar;
                        }
                    } catch (Exception exBa) { ais.common.ErrorAuditUtil.record(exBa, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:422");}
                }

                // ── Audio (streaming DB, pertemuan IN pertIds) ─────────────────────────
                long cAudio = 0;
                if (sessStream != null && pertIds != null && !pertIds.isEmpty()) {
                    try {
                        Long lAudio = (Long) sessStream.createCriteria(AudioPertemuan.class)
                            .add(Restrictions.in("pertemuan", pertIds))
                            .setProjection(Projections.rowCount())
                            .uniqueResult();
                        if (lAudio != null) cAudio = lAudio;
                    } catch (Exception exA) { ais.common.ErrorAuditUtil.record(exA, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:434");}
                }

                // ── Video (streaming DB, pertemuan IN pertIds) ─────────────────────────
                long cVideo = 0;
                if (sessStream != null && pertIds != null && !pertIds.isEmpty()) {
                    try {
                        Long lVideo = (Long) sessStream.createCriteria(VideoPertemuan.class)
                            .add(Restrictions.in("pertemuan", pertIds))
                            .setProjection(Projections.rowCount())
                            .uniqueResult();
                        if (lVideo != null) cVideo = lVideo;
                    } catch (Exception exV) { ais.common.ErrorAuditUtil.record(exV, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:446");}
                }

                // ── Materi/Konten File (streaming DB, pertemuan IN pertIds) ───────────
                long cMateri = 0;
                if (sessStream != null && pertIds != null && !pertIds.isEmpty()) {
                    try {
                        Long lMateri = (Long) sessStream.createCriteria(PertemuanFileContent.class)
                            .add(Restrictions.in("pertemuan", pertIds))
                            .setProjection(Projections.rowCount())
                            .uniqueResult();
                        if (lMateri != null) cMateri = lMateri;
                    } catch (Exception exM) { ais.common.ErrorAuditUtil.record(exM, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:458");}
                }

                jo.put("count_diskusi", cDiskusi);
                jo.put("count_materi", cMateri);
                jo.put("count_audio", cAudio);
                jo.put("count_video", cVideo);
                jo.put("count_sesuai_rps", cSesuaiRps);
                jo.put("count_buku_referensi", cBukuRef);
                jo.put("count_buku_ajar", cBukuAjar);
                
                dataArray.put(jo);
            }
        }
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:473");
    } finally {
        try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ringkasan_service.jsp:475");}
        ais.common.ElearningSessionUtil.closeQuietly(sess);
        HibernateUtil.closeSessionQuietly(sess);
        ais.common.ElearningSessionUtil.closeQuietly(sessStream);
    }

    JSONObject resp = new JSONObject();
    resp.put("data", dataArray);
    resp.put("total", totalRows);
    out.print(resp.toString());
    out.flush();
%>