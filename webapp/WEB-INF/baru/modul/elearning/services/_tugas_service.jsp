<%@ page language="java" pageEncoding="UTF-8"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="java.util.List"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.*"%>
<%
    String id = request.getParameter("id");
    String jenis = request.getParameter("jenis");
    String propName = request.getParameter("propName");
    String format = request.getParameter("format");
    
    JSONArray dataArr = new JSONArray();
    Session sess = HibernateUtil.openSession();
    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm");

    try {
        VOPembelajaran vop = (VOPembelajaran) sess.createQuery("from " + jenis + " where id = :id").setLong("id", Long.parseLong(id)).uniqueResult();
        if (vop != null && propName != null && !propName.isEmpty()) {
            
            // SUMBER 1: TUGAS DARI TABEL PERTEMUAN
            List<Pertemuan> tgPertemuan = sess.createCriteria(Pertemuan.class).add(Restrictions.eq(propName, vop)).add(Restrictions.isNotNull("judultugas")).add(Restrictions.ne("judultugas", "")).list();
            if (tgPertemuan != null) {
                for(Pertemuan t : tgPertemuan) {
                    JSONObject jo = new JSONObject();
                    String judul = t.getJudultugas();
                    String safeJudul = judul.replace("'", "\\'");
                    
                    // PERHATIKAN: ID diisi dengan {id}_Pertemuan
                    String linkHtml = "<a href=\"javascript:void(0)\" onclick=\"bukaSubModalDetail('tugas', '" + t.getId() + "_Pertemuan', '" + safeJudul + "')\" class=\"text-success font-weight-bold\" title=\"Lihat Rincian\"><i class=\"fas fa-external-link-alt mr-1\"></i> " + judul + "</a>";
                    
                    jo.put("judul", linkHtml); 
                    jo.put("judul_raw", judul);
                    jo.put("mulai", t.getMulai() != null ? sdf.format(t.getMulai()) : "-");
                    jo.put("sampai", t.getSelesai() != null ? sdf.format(t.getSelesai()) : "-");
                    dataArr.put(jo);
                }
            }

            // SUMBER 2: TUGAS DARI TABEL TUGAS PERTEMUAN
            List<TugasPertemuan> tgsKhusus = sess.createCriteria(TugasPertemuan.class).createAlias("pertemuanData", "p").add(Restrictions.eq("p." + propName, vop)).list();
            if (tgsKhusus != null) {
                for(TugasPertemuan t : tgsKhusus) {
                    JSONObject jo = new JSONObject();
                    String judul = t.getJudultugas() != null ? t.getJudultugas() : "Tugas Tambahan";
                    String safeJudul = judul.replace("'", "\\'");
                    
                    // PERHATIKAN: ID diisi dengan {id}_TugasPertemuan
                    String linkHtml = "<a href=\"javascript:void(0)\" onclick=\"bukaSubModalDetail('tugas', '" + t.getId() + "_TugasPertemuan', '" + safeJudul + "')\" class=\"text-success font-weight-bold\" title=\"Lihat Rincian\"><i class=\"fas fa-external-link-alt mr-1\"></i> " + judul + "</a>";
                    
                    jo.put("judul", linkHtml); 
                    jo.put("judul_raw", judul);
                    jo.put("mulai", t.getMulai() != null ? sdf.format(t.getMulai()) : "-");
                    jo.put("sampai", t.getSelesai() != null ? sdf.format(t.getSelesai()) : "-");
                    dataArr.put(jo);
                }
            }
        }
    } catch(Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_tugas_service.jsp:62"); } finally {
        try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_tugas_service.jsp:63");} ais.common.ElearningSessionUtil.closeQuietly(sess); HibernateUtil.closeSessionQuietly(sess);
    }
    
    if ("excel".equals(format) || "pdf".equals(format)) {
        if ("excel".equals(format)) { response.setContentType("application/vnd.ms-excel; charset=UTF-8"); response.setHeader("Content-Disposition", "attachment; filename=\"Daftar_Tugas.xls\""); }
        else { response.setContentType("text/html; charset=UTF-8"); }
        out.print("<html><head><meta charset=\"UTF-8\"><title>Daftar Tugas</title>");
        if ("pdf".equals(format)) out.print("<style>body{font-family: Arial;} table{width: 100%; border-collapse: collapse;} th, td{border: 1px solid #000; padding: 8px;} th{background-color: #f2f2f2;}</style></head><body onload='window.print()'>");
        else out.print("</head><body>");
        out.print("<h2 style='text-align:center;'>Daftar Tugas</h2><table border='1'><tr><th>No</th><th>Judul Tugas</th><th>Tanggal Mulai</th><th>Tanggal Sampai</th></tr>");
        for (int i = 0; i < dataArr.length(); i++) {
            JSONObject jo = dataArr.getJSONObject(i);
            out.print("<tr><td style='text-align:center;'>" + (i+1) + "</td><td>" + jo.optString("judul_raw", "-") + "</td><td>" + jo.optString("mulai", "-") + "</td><td>" + jo.optString("sampai", "-") + "</td></tr>");
        }
        out.print("</table></body></html>"); out.flush(); return;
    }
    response.setContentType("application/json; charset=UTF-8"); out.print(dataArr.toString()); out.flush();
%>