<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%
    try { out.clear(); out.clearBuffer(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_trend_pembayaran.jsp:12");}
    response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
    if (Common.getCurrentUser(request) == null) { out.print("{\"status\":\"error\", \"message\":\"Sesi Berakhir\"}"); return; }

    String ta = request.getParameter("tahunAkademik");
    String smt = request.getParameter("semester");
    String fakId = request.getParameter("fakultasId"); 
    String jurId = request.getParameter("jurusanId");
    String jkId = request.getParameter("jkId");
    
    String rentangWaktu = request.getParameter("rentangWaktu");
    String startStr = request.getParameter("startDate");
    String endStr = request.getParameter("endDate");

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Date startDate = new Date();
    Date endDate = new Date();
    Calendar cal = Calendar.getInstance();

    // Kalkulasi Jangka Waktu Tanggal secara dinamis
    if ("custom".equals(rentangWaktu)) {
        try {
            if (startStr != null && !startStr.isEmpty()) startDate = sdf.parse(startStr);
            if (endStr != null && !endStr.isEmpty()) endDate = sdf.parse(endStr);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_trend_pembayaran.jsp:36");}
    } else {
        if ("3_minggu".equals(rentangWaktu)) cal.add(Calendar.WEEK_OF_YEAR, -3);
        else if ("1_bulan".equals(rentangWaktu)) cal.add(Calendar.MONTH, -1);
        else if ("3_bulan".equals(rentangWaktu)) cal.add(Calendar.MONTH, -3);
        else if ("6_bulan".equals(rentangWaktu)) cal.add(Calendar.MONTH, -6);
        else if ("1_tahun".equals(rentangWaktu)) cal.add(Calendar.YEAR, -1);
        else if ("3_tahun".equals(rentangWaktu)) cal.add(Calendar.YEAR, -3);
        else cal.add(Calendar.WEEK_OF_YEAR, -1); // Default = 1_minggu
        
        startDate = cal.getTime();
    }

    // Pastikan endDate ditarik hingga pukul 23:59:59 (Batas akhir hari)
    Calendar endCal = Calendar.getInstance();
    endCal.setTime(endDate);
    endCal.set(Calendar.HOUR_OF_DAY, 23);
    endCal.set(Calendar.MINUTE, 59);
    endCal.set(Calendar.SECOND, 59);
    endDate = endCal.getTime();
    
    // Pastikan startDate ditarik ke pukul 00:00:00 (Awal hari)
    Calendar startCal = Calendar.getInstance();
    startCal.setTime(startDate);
    startCal.set(Calendar.HOUR_OF_DAY, 0);
    startCal.set(Calendar.MINUTE, 0);
    startCal.set(Calendar.SECOND, 0);
    startDate = startCal.getTime();

    Session sess = null;
    try {
        sess = HibernateUtil.openSession();
        // Kueri Pengelompokan Berdasarkan Tanggal Transaksi (Agregasi)
        String sql = "select cast(cp.tanggal as date) as tgl, count(cp.id) as jml_trx, sum(cp.nilai) as total_nominal " +
                     "from cicilan_pembayaran cp " +
                     "left join kegiatan k on cp.kegiatan = k.id " +
                     "left join jurusan j on k.jurusan = j.id " +
                     "where cp.tanggal >= :start and cp.tanggal <= :end ";

        // Filter Parametrik Dasbor
        if (ta != null && !ta.isEmpty()) sql += " and k.tahun_akademik = :ta ";
        if (smt != null && !smt.isEmpty()) sql += (smt.equals("Genap") ? " and k.semster % 2 = 0 " : " and k.semster % 2 = 1 ");
        if (jkId != null && !jkId.isEmpty()) sql += " and k.jenis_kegiatan = " + jkId;
        
        if (jurId != null && !jurId.isEmpty()) {
            sql += " and k.jurusan = " + jurId;
        } else if (fakId != null && !fakId.isEmpty()) {
            sql += " and j.fakultas = " + fakId;
        }

        sql += " group by cast(cp.tanggal as date) order by tgl asc";

        org.hibernate.SQLQuery query = sess.createSQLQuery(sql);
        query.setTimestamp("start", startDate);
        query.setTimestamp("end", endDate);
        if (ta != null && !ta.isEmpty()) query.setString("ta", ta);

        List<Object[]> rows = query.list();
        JSONArray arr = new JSONArray();

        // Konversi Data Menjadi JSON
        for (Object[] r : rows) {
            if (r[0] != null) {
                JSONObject o = new JSONObject();
                o.put("tanggal", r[0].toString());
                o.put("jmlTrx", r[1] != null ? ((Number)r[1]).longValue() : 0);
                o.put("nominal", r[2] != null ? ((Number)r[2]).doubleValue() : 0.0);
                arr.put(o);
            }
        }

        JSONObject res = new JSONObject();
        res.put("status", "00");
        res.put("data", arr);
        out.print(res.toString()); out.flush();

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pembayaran/_service_trend_pembayaran.jsp:113");
        out.print("{\"status\":\"error\", \"message\":\"Terjadi galat pada peladen saat mengkalkulasi tren.\"}"); out.flush();
    } finally {
        if(sess != null && sess.isOpen()) { try{sess.disconnect();}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_trend_pembayaran.jsp:116");} try{sess.close();}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_trend_pembayaran.jsp:116");} }
        try{HibernateUtil.closeSessionQuietly(sess);}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_trend_pembayaran.jsp:117");}
    }
%>