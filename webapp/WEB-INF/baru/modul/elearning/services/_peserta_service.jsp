<%@ page language="java" pageEncoding="UTF-8"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.hibernate.Session"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.database.model.sekolah.*"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonMedia"%>
<%
    String id = request.getParameter("id");
    String jenis = request.getParameter("jenis");
    String format = request.getParameter("format");
    
    JSONArray dataArr = new JSONArray();
    Session sess = HibernateUtil.openSession();
    try {
        VOPembelajaran vop = (VOPembelajaran) sess.createQuery("from " + jenis + " where id = :id").setLong("id", Long.parseLong(id)).uniqueResult();
        if (vop != null) {
            List<Long> mIds = vop.ambilMahasiswaById();
            if (mIds != null) {
                for(Long mId : mIds) {
                    Mahasiswa m = (Mahasiswa) sess.get(Mahasiswa.class, mId);
                    if (m != null) {
                        JSONObject jo = new JSONObject();
                        jo.put("nim", m.getNim() != null ? m.getNim() : "-");
                        jo.put("nama", m.getNama() != null ? m.getNama() : "-");
                        
                        String foto = Common.ROOT + "/images/default_user.png";
                        try {
                            String f = CommonMedia.getUrlFotoPengguna(new Tbmuser(m));
                            if(f != null && !f.isEmpty()) foto = f;
                            if(!foto.startsWith("http") && !foto.startsWith("data:") && !foto.startsWith(Common.ROOT)) {
                                foto = Common.ROOT + (foto.startsWith("/") ? foto : "/" + foto);
                            }
                        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_peserta_service.jsp:37");}
                        jo.put("foto", foto);
                        
                        dataArr.put(jo);
                    }
                }
            }
            List<Long> sIds = vop.ambilSiswaById();
            if (sIds != null) {
                for(Long sId : sIds) {
                    Siswa s = (Siswa) sess.get(Siswa.class, sId);
                    if (s != null) {
                        JSONObject jo = new JSONObject();
                        jo.put("nim", s.getNomorInduk() != null ? s.getNomorInduk() : "-");
                        jo.put("nama", s.getNama() != null ? s.getNama() : "-");
                        
                        String foto = Common.ROOT + "/images/default_user.png";
                        try {
                            String f = CommonMedia.getUrlFotoPengguna(new Tbmuser(s));
                            if(f != null && !f.isEmpty()) foto = f;
                            if(!foto.startsWith("http") && !foto.startsWith("data:") && !foto.startsWith(Common.ROOT)) {
                                foto = Common.ROOT + (foto.startsWith("/") ? foto : "/" + foto);
                            }
                        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_peserta_service.jsp:60");}
                        jo.put("foto", foto);
                        
                        dataArr.put(jo);
                    }
                }
            }
        }
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_peserta_service.jsp:69");
    } finally {
        try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_peserta_service.jsp:71");}
        ais.common.ElearningSessionUtil.closeQuietly(sess);
        HibernateUtil.closeSessionQuietly(sess);
    }
    
    // Logika Ekspor Excel & PDF Mandiri
    if ("excel".equals(format) || "pdf".equals(format)) {
        if ("excel".equals(format)) {
            response.setContentType("application/vnd.ms-excel; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"Daftar_Peserta.xls\"");
        } else {
            response.setContentType("text/html; charset=UTF-8");
        }
        
        out.print("<html><head><meta charset=\"UTF-8\"><title>Daftar Peserta</title>");
        if ("pdf".equals(format)) {
            out.print("<style>body{font-family: Arial, sans-serif;} table{width: 100%; border-collapse: collapse;} th, td{border: 1px solid #000; padding: 8px; text-align: left;} th{background-color: #f2f2f2;}</style>");
            out.print("</head><body onload='window.print()'>");
        } else {
            out.print("</head><body>");
        }
        
        out.print("<h2 style='text-align:center;'>Daftar Peserta</h2>");
        out.print("<table border='1'>");
        out.print("<tr><th>No</th><th>NIM / No. Induk</th><th>Nama Peserta</th></tr>");
        for (int i = 0; i < dataArr.length(); i++) {
            JSONObject jo = dataArr.getJSONObject(i);
            String nim = jo.has("nim") ? jo.getString("nim") : "-";
            String nama = jo.has("nama") ? jo.getString("nama") : "-";
            out.print("<tr><td>" + (i+1) + "</td><td>" + nim + "</td><td>" + nama + "</td></tr>");
        }
        out.print("</table></body></html>");
        out.flush();
        return;
    }

    response.setContentType("application/json; charset=UTF-8");
    out.print(dataArr.toString());
    out.flush();
%>