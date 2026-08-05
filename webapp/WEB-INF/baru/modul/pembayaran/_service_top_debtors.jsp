<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.util.List"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="java.math.BigInteger"%>

<%
    try { out.clear(); out.clearBuffer(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_top_debtors.jsp:11");}
    response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
    if (Common.getCurrentUser(request) == null) { out.print("{\"status\":\"error\", \"message\":\"Sesi Berakhir\"}"); return; }

    String ta = request.getParameter("tahunAkademik");
    String smt = request.getParameter("semester");
    String fakId = request.getParameter("fakultasId");
    String jurId = request.getParameter("jurusanId");
    String jkId = request.getParameter("jkId");
    String q = request.getParameter("q"); 
    
    String startStr = request.getParameter("start");
    String limitStr = request.getParameter("limit");
    
    int offsetNum = 0; 
    int limitNum = 500; 
    
    try { if (startStr != null && !startStr.trim().isEmpty()) offsetNum = Integer.parseInt(startStr); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_top_debtors.jsp:28");}
    try { if (limitStr != null && !limitStr.trim().isEmpty()) limitNum = Integer.parseInt(limitStr); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_top_debtors.jsp:29");}
    
    if (limitNum > 5000) {
        out.print("{\"status\":\"error\", \"message\":\"Batas maksimal penampilan data adalah 5.000 baris.\"}");
        out.flush();
        return;
    }

    Session sess = null;

    try {
        sess = HibernateUtil.openSession();

        // 1. Membangun Kriteria SQL Dasar (Bergabung dengan entitas Mahasiswa & Prodi)
        String baseWhereClause = " from kegiatan a "
                + " left join mahasiswa c on a.mahasiswa = c.id "
                + " left join biodata_calon_mahasiswa b on a.calon_mahasiswa = b.id "
                + " left join jurusan j on a.jurusan = j.id "
                + " left join fakultas f on j.fakultas = f.id "
                + " where a.aktif=true and (a.tagihan - a.dibayar) > 0.1 ";

        if (q != null && !q.trim().isEmpty()) {
            baseWhereClause += " and (c.nama ilike :buktilike or b.nama ilike :buktilike or c.nim ilike :buktilike or b.no_registrasi ilike :buktilike) ";
        }

        if (jurId != null && !jurId.isEmpty()) {
            baseWhereClause += " and a.jurusan = " + jurId;
        } else if (fakId != null && !fakId.isEmpty()) {
            baseWhereClause += " and j.fakultas = " + fakId;
        }

        if (jkId != null && !jkId.isEmpty()) {
            baseWhereClause += " and a.jenis_kegiatan = " + jkId;
        }
        if (ta != null && !ta.isEmpty()) {
            baseWhereClause += " and a.tahun_akademik = :tahunAkademik ";
        }
        if (smt != null && !smt.isEmpty()) {
            baseWhereClause += (smt.equals("Genap") ? " and a.semster % 2 = 0 " : " and a.semster % 2 = 1 ");
        }

        // --- MENGHITUNG TOTAL BARIS UNTUK PAGINASI ---
        String sqlCount = "select count(*) from (select coalesce(c.nim, b.no_registrasi) " + baseWhereClause + " group by coalesce(c.nim, b.no_registrasi), coalesce(c.nama, b.nama)) as sub";
        org.hibernate.SQLQuery qCount = sess.createSQLQuery(sqlCount);
        
        if (q != null && !q.trim().isEmpty()) qCount.setString("buktilike", "%" + q.trim() + "%");
        if (ta != null && !ta.isEmpty()) qCount.setString("tahunAkademik", ta);
        
        BigInteger totalRecordsObj = (BigInteger) qCount.uniqueResult();
        int totalRecords = totalRecordsObj != null ? totalRecordsObj.intValue() : 0;
        int totalPages = (int) Math.ceil((double) totalRecords / limitNum);
        int currentPage = (offsetNum / limitNum) + 1;

        // --- MENGAMBIL DATA TOP DEBTORS ---
        String sqlData = "select "
                + " coalesce(c.nim, b.no_registrasi) as nim, "
                + " coalesce(c.nama, b.nama) as nama, "
                + " coalesce(f.nama, '-') as fakultas, "
                + " coalesce(j.nama, '-') as jurusan, "
                + " sum(a.tagihan) as total_tagihan, "
                + " sum(a.dibayar) as total_dibayar, "
                + " sum(a.tagihan - a.dibayar) as total_piutang "
                + baseWhereClause
                + " group by coalesce(c.nim, b.no_registrasi), coalesce(c.nama, b.nama), f.nama, j.nama "
                + " order by total_piutang desc "
                + " limit " + limitNum + " offset " + offsetNum;

        org.hibernate.SQLQuery qData = sess.createSQLQuery(sqlData);
        
        if (q != null && !q.trim().isEmpty()) qData.setString("buktilike", "%" + q.trim() + "%");
        if (ta != null && !ta.isEmpty()) qData.setString("tahunAkademik", ta);
        
        List<Object[]> objects = qData.list();
        JSONArray dataArray = new JSONArray();

        for (Object[] o : objects) {
            String nim = o[0] == null ? "-" : o[0].toString();
            String nama = o[1] == null ? "-" : o[1].toString();
            String fakultas = o[2] == null ? "-" : o[2].toString();
            String jurusan = o[3] == null ? "-" : o[3].toString();
            
            Double tagihan = o[4] != null ? ((Number)o[4]).doubleValue() : 0.0;
            Double dibayar = o[5] != null ? ((Number)o[5]).doubleValue() : 0.0;
            Double piutang = o[6] != null ? ((Number)o[6]).doubleValue() : 0.0;

            JSONObject barisData = new JSONObject();
            barisData.put("nim", nim);
            barisData.put("nama", nama);
            barisData.put("fakultas", fakultas);
            barisData.put("jurusan", jurusan);
            barisData.put("tagihan", tagihan);
            barisData.put("dibayar", dibayar);
            barisData.put("piutang", piutang);
            
            dataArray.put(barisData);
        }
        
        JSONObject res = new JSONObject(); 
        res.put("status", "00"); 
        res.put("data", dataArray);
        res.put("totalRecords", totalRecords);
        res.put("totalPages", totalPages);
        res.put("currentPage", currentPage);
        res.put("offset", offsetNum);
        
        out.print(res.toString()); 
        out.flush();
        
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pembayaran/_service_top_debtors.jsp:138");
        out.print("{\"status\":\"error\", \"message\":\"Terjadi galat pada peladen saat menarik data penunggak.\"}");
    } finally {
        if(sess != null && sess.isOpen()) { try{sess.disconnect();}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_top_debtors.jsp:141");} try{sess.close();}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_top_debtors.jsp:141");} }
        try{HibernateUtil.closeSessionQuietly(sess);}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_top_debtors.jsp:142");}
    }
%>