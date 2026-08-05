<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.CriteriaSpecification"%>
<%@page import="org.hibernate.criterion.Disjunction"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.hibernate.StreamingHibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.database.model.streaming.AudioPertemuan"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.setStatus(401); 
        return;
    }

    // Parameter Paginasi
    int startIdx = Integer.parseInt(request.getParameter("mulai") == null ? "0" : request.getParameter("mulai"));
    int limitRows = Integer.parseInt(request.getParameter("banyak") == null ? "10" : request.getParameter("banyak"));
    String dir = request.getParameter("dir");

    // Parameter Filter
    String q_nama_audio = request.getParameter("q_nama_audio"); 
    String ta = request.getParameter("q_ta");
    String smt = request.getParameter("q_smt");
    String q_matakuliah = request.getParameter("q_matakuliah");
    String q_dosen = request.getParameter("q_dosen");
    
    // Parameter Struktural dari UI
    String q_fakultas = request.getParameter("q_fakultas");
    String q_jurusan = request.getParameter("q_jurusan");
    String q_yayasan = request.getParameter("q_yayasan");
    String q_sekolah = request.getParameter("q_sekolah");
    
    boolean isSivitas = tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null || tbmuser.getSiswa() != null || tbmuser.ambilGuru() != null;
    
    // CEK STATUS SUPER ADMIN (Bebas dari institusi apapun)
    boolean isSuperAdmin = !isSivitas && tbmuser.ambilFakultas() == null && tbmuser.ambilJurusan() == null && tbmuser.ambilSekolah() == null && tbmuser.ambilYayasan() == null;
    
    // Cek apakah ada filter akademik spesifik yang masuk dari UI
    boolean hasFilterAkademik = (ta != null && !ta.trim().isEmpty()) || (smt != null && !smt.trim().isEmpty()) 
                             || (q_matakuliah != null && !q_matakuliah.trim().isEmpty()) || (q_dosen != null && !q_dosen.trim().isEmpty())
                             || (q_fakultas != null && !q_fakultas.trim().isEmpty()) || (q_jurusan != null && !q_jurusan.trim().isEmpty())
                             || (q_yayasan != null && !q_yayasan.trim().isEmpty()) || (q_sekolah != null && !q_sekolah.trim().isEmpty());

    // OPTIMASI: Jika Super Admin dan TIDAK ADA filter akademik, kita BISA BYPASS kueri ke tabel Pertemuan
    boolean needPertemuanQuery = !isSuperAdmin || hasFilterAkademik;

    Session sessMain = HibernateUtil.openSession();
    Session sessStream = StreamingHibernateUtil.getInstance().currentSession();
    JSONObject responseJson = new JSONObject();

    try {
        List<Long> allowedPertemuanIds = null;

        // =========================================================================
        // LANGKAH 1: Kueri ke Pertemuan HANYA JIKA DIBUTUHKAN
        // =========================================================================
        if (needPertemuanQuery) {
            Criteria cPertemuan = sessMain.createCriteria(Pertemuan.class);
            cPertemuan.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

            // Filter Hak Akses Sivitas
            if (isSivitas) {
                Disjunction djAkses = Restrictions.disjunction();
                if (tbmuser.getSiswa() != null) djAkses.add(Restrictions.like("siswas", "%," + tbmuser.getSiswa().getId() + ",%"));
                if (tbmuser.getMahasiswa() != null) djAkses.add(Restrictions.like("mahasiswas", "%," + tbmuser.getMahasiswa().getId() + ",%"));
                if (tbmuser.ambilDosen() != null) djAkses.add(Restrictions.like("dosens", "%," + tbmuser.ambilDosen().getId() + ",%"));
                if (tbmuser.ambilGuru() != null) djAkses.add(Restrictions.like("gurus", "%," + tbmuser.ambilGuru().getId() + ",%"));
                cPertemuan.add(djAkses);
            } else if (!isSuperAdmin) {
                // Filter Institusi untuk Admin Level Menengah
                if (tbmuser.ambilJurusan() != null) cPertemuan.add(Restrictions.eq("jurusan", tbmuser.ambilJurusan()));
                else if (tbmuser.ambilFakultas() != null) { cPertemuan.createAlias("jurusan", "jStruk", CriteriaSpecification.LEFT_JOIN); cPertemuan.add(Restrictions.eq("jStruk.fakultas", tbmuser.ambilFakultas())); }
                
                if (tbmuser.ambilSekolah() != null) cPertemuan.add(Restrictions.eq("sekolah", tbmuser.ambilSekolah()));
                else if (tbmuser.ambilYayasan() != null) { cPertemuan.createAlias("sekolah", "sStruk", CriteriaSpecification.LEFT_JOIN); cPertemuan.add(Restrictions.eq("sStruk.yayasan", tbmuser.ambilYayasan())); }
            }

            // Filter Akademik dari UI
            if (ta != null && !ta.trim().isEmpty()) cPertemuan.add(Restrictions.eq("ta", ta));
            if (smt != null && !smt.trim().isEmpty()) cPertemuan.add(Restrictions.eq("smt", smt));
            
            if (q_matakuliah != null && !q_matakuliah.trim().isEmpty()) {
                cPertemuan.createAlias("perkuliahan", "prk", CriteriaSpecification.LEFT_JOIN);
                cPertemuan.createAlias("prk.matakuliah", "mk", CriteriaSpecification.LEFT_JOIN);
                cPertemuan.add(Restrictions.or(Restrictions.ilike("mk.kode", q_matakuliah, MatchMode.ANYWHERE), Restrictions.ilike("mk.nama", q_matakuliah, MatchMode.ANYWHERE)));
            }
            
            if (q_dosen != null && !q_dosen.trim().isEmpty()) {
                List<Long> idsDsn = sessMain.createCriteria(Dosen.class).setProjection(Projections.property("id")).add(Restrictions.or(Restrictions.ilike("nama", q_dosen, MatchMode.ANYWHERE), Restrictions.ilike("nidn", q_dosen, MatchMode.ANYWHERE))).list();
                if(idsDsn != null && !idsDsn.isEmpty()) {
                    Disjunction djDsn = Restrictions.disjunction();
                    for(Long idD : idsDsn) djDsn.add(Restrictions.like("dosens", "%," + idD + ",%"));
                    cPertemuan.add(djDsn);
                } else {
                    cPertemuan.add(Restrictions.sqlRestriction("1=0")); // Force empty
                }
            }
            
            // Filter Struktural dari UI
            if (q_jurusan != null && !q_jurusan.trim().isEmpty()) {
                try { cPertemuan.add(Restrictions.eq("jurusan.id", Long.parseLong(q_jurusan))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_audio.jsp:113");}
            } else if (q_fakultas != null && !q_fakultas.trim().isEmpty()) {
                cPertemuan.createAlias("jurusan", "jStruk", CriteriaSpecification.LEFT_JOIN);
                try { cPertemuan.add(Restrictions.eq("jStruk.fakultas.id", Long.parseLong(q_fakultas))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_audio.jsp:116");}
            }

            if (q_sekolah != null && !q_sekolah.trim().isEmpty()) {
                try { cPertemuan.add(Restrictions.eq("sekolah.id", Long.parseLong(q_sekolah))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_audio.jsp:120");}
            } else if (q_yayasan != null && !q_yayasan.trim().isEmpty()) {
                cPertemuan.createAlias("sekolah", "sStruk", CriteriaSpecification.LEFT_JOIN);
                try { cPertemuan.add(Restrictions.eq("sStruk.yayasan.id", Long.parseLong(q_yayasan))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_audio.jsp:123");}
            }

            cPertemuan.setProjection(Projections.property("id"));
            allowedPertemuanIds = cPertemuan.list();
        }

        // =========================================================================
        // LANGKAH 2: TARIK DATA AUDIO DARI DATABASE STREAMING
        // =========================================================================
        JSONArray dataArray = new JSONArray();
        Long countNext = 0L;
        Long countBack = 0L;
        
        boolean executeFileQuery = false;
        if (needPertemuanQuery) {
            if (allowedPertemuanIds != null && !allowedPertemuanIds.isEmpty()) executeFileQuery = true;
        } else {
            executeFileQuery = true; // Langsung hajar karena BYPASS
        }

        if (executeFileQuery) {
            Criteria cFile = sessStream.createCriteria(AudioPertemuan.class);
            Criteria cCount = sessStream.createCriteria(AudioPertemuan.class);
            
            // Terapkan Pembatasan IN-Clause Jika Tidak Bypass
            if (needPertemuanQuery) {
                Disjunction djIn = Restrictions.disjunction();
                List<Long> chunk = new ArrayList<Long>();
                for (int i = 0; i < allowedPertemuanIds.size(); i++) {
                    chunk.add(allowedPertemuanIds.get(i));
                    if (chunk.size() == 900 || i == allowedPertemuanIds.size() - 1) {
                        djIn.add(Restrictions.in("pertemuan", chunk));
                        chunk = new ArrayList<Long>();
                    }
                }
                cFile.add(djIn);
                cCount.add(djIn);
            }

            // Filter Nama Audio
            if (q_nama_audio != null && !q_nama_audio.trim().isEmpty()) {
                cFile.add(Restrictions.ilike("nama", q_nama_audio, MatchMode.ANYWHERE));
                cCount.add(Restrictions.ilike("nama", q_nama_audio, MatchMode.ANYWHERE));
            }

            cFile.setProjection(Projections.property("id"));
            cFile.setFirstResult(startIdx);
            cFile.setMaxResults(limitRows);
            cFile.addOrder(Order.desc("id")); 
            
            List<Long> fileIds = cFile.list();
            if (fileIds != null) {
                for (Long fId : fileIds) { dataArray.put(fId); }
            }

            cCount.setProjection(Projections.rowCount());
            Long totalFiles = (Long) cCount.uniqueResult();
            if(totalFiles != null) {
                long sisa = totalFiles - (startIdx + limitRows);
                if (sisa > 0) countNext = sisa;
            }
        }

        responseJson.put("data", dataArray);
        responseJson.put("countNext", countNext);
        responseJson.put("countBack", countBack);

    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_linimasa_audio.jsp:192");
        responseJson.put("data", new JSONArray());
        responseJson.put("countNext", 0);
        responseJson.put("countBack", 0);
    } finally {
        try { sessMain.disconnect(); sessMain.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_audio.jsp:197");}
        try { sessStream.disconnect(); sessStream.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_linimasa_audio.jsp:198");}
        HibernateUtil.closeSessionQuietly(sessMain);
        StreamingHibernateUtil.getInstance().closeSession();
    }

    out.print(responseJson.toString());
    out.flush();
%>