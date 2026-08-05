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
            List<PertemuanPunyaUjian> ujians = sess.createCriteria(PertemuanPunyaUjian.class).createAlias("pertemuan", "p").add(Restrictions.eq("p." + propName, vop)).list();
            if (ujians != null) {
                for(PertemuanPunyaUjian u : ujians) {
                    JSONObject jo = new JSONObject();
                    String nama = u.getNama() != null ? u.getNama() : "Ujian Tanpa Nama";
                    String safeNama = nama.replace("'", "\\'");
                    String linkHtml = "<a href=\"javascript:void(0)\" onclick=\"bukaSubModalDetail('ujian', '" + u.getId() + "', '" + safeNama + "')\" class=\"text-danger font-weight-bold\" title=\"Lihat Rincian\"><i class=\"fas fa-external-link-alt mr-1\"></i> " + nama + "</a>";
                    
                    jo.put("nama", linkHtml);
                    jo.put("nama_raw", nama);
                    jo.put("tanggal", u.getMulaiUjian() != null ? sdf.format(u.getMulaiUjian()) : "-");
                    jo.put("sifat", (u.getUjian() != null && u.getUjian().getJenisKoreksi() != null) ? u.getUjian().getJenisKoreksi() : "-");
                    dataArr.put(jo);
                }
            }
        }
    } catch(Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_ujian_service.jsp:39"); } finally {
        try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_ujian_service.jsp:40");} ais.common.ElearningSessionUtil.closeQuietly(sess); HibernateUtil.closeSessionQuietly(sess);
    }
    
    if ("excel".equals(format) || "pdf".equals(format)) {
        if ("excel".equals(format)) { response.setContentType("application/vnd.ms-excel; charset=UTF-8"); response.setHeader("Content-Disposition", "attachment; filename=\"Daftar_Ujian.xls\""); }
        else { response.setContentType("text/html; charset=UTF-8"); }
        out.print("<html><head><meta charset=\"UTF-8\"><title>Daftar Ujian</title>");
        if ("pdf".equals(format)) out.print("<style>body{font-family: Arial;} table{width: 100%; border-collapse: collapse;} th, td{border: 1px solid #000; padding: 8px;} th{background-color: #f2f2f2;}</style></head><body onload='window.print()'>");
        else out.print("</head><body>");
        out.print("<h2 style='text-align:center;'>Daftar Ujian</h2><table border='1'><tr><th>No</th><th>Nama Ujian</th><th>Tanggal Ujian</th><th>Sifat Ujian</th></tr>");
        for (int i = 0; i < dataArr.length(); i++) {
            JSONObject jo = dataArr.getJSONObject(i);
            out.print("<tr><td style='text-align:center;'>" + (i+1) + "</td><td>" + jo.optString("nama_raw", "-") + "</td><td>" + jo.optString("tanggal", "-") + "</td><td>" + jo.optString("sifat", "-") + "</td></tr>");
        }
        out.print("</table></body></html>"); out.flush(); return;
    }
    response.setContentType("application/json; charset=UTF-8"); out.print(dataArr.toString()); out.flush();
%>