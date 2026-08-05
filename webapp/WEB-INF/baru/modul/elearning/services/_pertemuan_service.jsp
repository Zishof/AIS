<%@ page language="java" pageEncoding="UTF-8"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.hibernate.Session"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.Comparator"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.*"%>
<%
    String id = request.getParameter("id");
    String jenis = request.getParameter("jenis");
    String format = request.getParameter("format");
    
    JSONArray dataArr = new JSONArray();
    Session sess = HibernateUtil.openSession();
    SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMM yyyy");
    SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");

    try {
        VOPembelajaran vop = (VOPembelajaran) sess.createQuery("from " + jenis + " where id = :id").setLong("id", Long.parseLong(id)).uniqueResult();
        if (vop != null) {
            List<Pertemuan> pertemuans = vop.ambilPertemuanList();
            if (pertemuans != null) {
                Collections.sort(pertemuans, new Comparator<Pertemuan>() {
                    public int compare(Pertemuan p1, Pertemuan p2) {
                        return (p1.getPertemuanKe() == null ? 0 : p1.getPertemuanKe()) - (p2.getPertemuanKe() == null ? 0 : p2.getPertemuanKe());
                    }
                });

                for(Pertemuan p : pertemuans) {
                    JSONObject jo = new JSONObject();

                    // 0. ID pertemuan (dipakai untuk aksi buat data baru per-baris di Ringkasan)
                    jo.put("id", p.getId());

                    // 1 & 2. Data Dasar & Info
                    jo.put("pertemuanKe", p.getPertemuanKe() != null ? p.getPertemuanKe() : "-");
                    String infoPert = VOPembelajaran.infoSimple(p);
                    jo.put("infoPertemuan", infoPert != null ? infoPert : "-");
                    
                    // 3. Topik & Link Popup
                    String topik = p.getTopik() != null ? p.getTopik() : "Tanpa Topik";
                    String safeTopik = topik.replace("'", "\\'");
                    String linkHtml = "<a href=\"javascript:void(0)\" onclick=\"bukaSubModalDetail('pertemuan', '" + p.getId() + "', '" + safeTopik + "')\" class=\"text-primary font-weight-bold\" title=\"Lihat Rincian\"><i class=\"fas fa-external-link-alt mr-1\"></i> " + topik + "</a>";
                    jo.put("topik", linkHtml); 
                    jo.put("topik_raw", topik); 
                    
                    // 4. Data Rincian RPP/Silabus
                    jo.put("indikator", p.getIndikator() != null ? p.getIndikator() : "-");
                    jo.put("waktupembelajaran", p.getWaktupembelajaran() != null ? p.getWaktupembelajaran() : "-");
                    jo.put("pengalamanBelajar", p.getPengalamanBelajar() != null ? p.getPengalamanBelajar() : "-");
                    jo.put("tugasDanPenilaian", p.getTugasDanPenilaian() != null ? p.getTugasDanPenilaian() : "-");
                    jo.put("metodePembelajaran", p.getMetodePembelajaran() != null && !p.getMetodePembelajaran().trim().isEmpty() ? p.getMetodePembelajaran() : "-");
                    jo.put("bukuRujukan1", p.getBukuRujukan1() != null && !p.getBukuRujukan1().trim().isEmpty() ? p.getBukuRujukan1() : "-");
                    jo.put("bukuRujukan2", p.getBukuRujukan2() != null && !p.getBukuRujukan2().trim().isEmpty() ? p.getBukuRujukan2() : "-");
                    jo.put("catatan", p.getCatatan() != null && !p.getCatatan().trim().isEmpty() ? p.getCatatan() : "-");
                    
                    // 5. Data Lokasi dan Waktu
                    jo.put("ruang", p.getRuang() != null && p.getRuang().getNama() != null ? p.getRuang().getNama() : "-");
                    jo.put("tanggal", p.getTanggal() != null ? sdfDate.format(p.getTanggal()) : "-");
                    
                    String strMulai = p.getMulai() != null ? sdfTime.format(p.getMulai()) : (p.getWaktuMulai() != null ? p.getWaktuMulai() : "-");
                    String strSelesai = p.getSelesai() != null ? sdfTime.format(p.getSelesai()) : (p.getWaktuSelesai() != null ? p.getWaktuSelesai() : "-");
                    jo.put("mulai", strMulai);
                    jo.put("selesai", strSelesai);
                    
                    dataArr.put(jo);
                }
            }
        }
    } catch(Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_pertemuan_service.jsp:73"); } finally {
        try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_pertemuan_service.jsp:74");} ais.common.ElearningSessionUtil.closeQuietly(sess); HibernateUtil.closeSessionQuietly(sess);
    }
    
    if ("excel".equals(format) || "pdf".equals(format)) {
        if ("excel".equals(format)) {
            response.setContentType("application/vnd.ms-excel; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"Daftar_Pertemuan.xls\"");
        } else { response.setContentType("text/html; charset=UTF-8"); }
        
        out.print("<html><head><meta charset=\"UTF-8\"><title>Daftar Pertemuan</title>");
        if ("pdf".equals(format)) out.print("<style>body{font-family: Arial; font-size: 11px;} table{width: 100%; border-collapse: collapse;} th, td{border: 1px solid #000; padding: 5px;} th{background-color: #f2f2f2;}</style></head><body onload='window.print()'>");
        else out.print("</head><body>");
        
        out.print("<h2 style='text-align:center;'>Daftar Pertemuan</h2><table border='1'>");
        out.print("<tr><th>Ke</th><th>Topik Pembahasan</th><th>Info Pertemuan</th><th>Tanggal</th><th>Jam</th><th>Ruangan</th><th>Metode Pembelajaran</th><th>Waktu Pemb.</th><th>Indikator</th><th>Pengalaman Belajar</th><th>Penilaian/Tugas</th><th>Rujukan Utama</th><th>Rujukan 2</th><th>Catatan</th></tr>");
        for (int i = 0; i < dataArr.length(); i++) {
            JSONObject jo = dataArr.getJSONObject(i);
            out.print("<tr>");
            out.print("<td style='text-align:center;'>" + jo.optString("pertemuanKe", "-") + "</td>");
            out.print("<td>" + jo.optString("topik_raw", "-") + "</td>");
            out.print("<td>" + jo.optString("infoPertemuan", "-") + "</td>");
            out.print("<td>" + jo.optString("tanggal", "-") + "</td>");
            out.print("<td style='text-align:center;'>" + jo.optString("mulai", "-") + " - " + jo.optString("selesai", "-") + "</td>");
            out.print("<td>" + jo.optString("ruang", "-") + "</td>");
            out.print("<td>" + jo.optString("metodePembelajaran", "-") + "</td>");
            out.print("<td>" + jo.optString("waktupembelajaran", "-") + "</td>");
            out.print("<td>" + jo.optString("indikator", "-") + "</td>");
            out.print("<td>" + jo.optString("pengalamanBelajar", "-") + "</td>");
            out.print("<td>" + jo.optString("tugasDanPenilaian", "-") + "</td>");
            out.print("<td>" + jo.optString("bukuRujukan1", "-") + "</td>");
            out.print("<td>" + jo.optString("bukuRujukan2", "-") + "</td>");
            out.print("<td>" + jo.optString("catatan", "-") + "</td>");
            out.print("</tr>");
        }
        out.print("</table></body></html>"); out.flush(); return;
    }
    response.setContentType("application/json; charset=UTF-8"); out.print(dataArr.toString()); out.flush();
%>