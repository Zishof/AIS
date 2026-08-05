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
    try { out.clear(); out.clearBuffer(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_kanal_pembayaran.jsp:13");}
    response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
    if (Common.getCurrentUser(request) == null) { out.print("{\"status\":\"error\", \"message\":\"Sesi Berakhir\"}"); return; }

    String ta = request.getParameter("tahunAkademik");
    String smt = request.getParameter("semester");
    String fakId = request.getParameter("fakultasId"); 
    String jurId = request.getParameter("jurusanId");
    String jkId = request.getParameter("jkId");
    String q = request.getParameter("q"); 
    
    String rentangWaktu = request.getParameter("rentangWaktu");
    String startStr = request.getParameter("startDate");
    String endStr = request.getParameter("endDate");

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Date startDate = new Date(0); // Default ke 1 Jan 1970 untuk "semua"
    Date endDate = new Date();
    Calendar cal = Calendar.getInstance();

    boolean isAllTime = "semua".equals(rentangWaktu);

    if (!isAllTime) {
        if ("custom".equals(rentangWaktu)) {
            try {
                if (startStr != null && !startStr.isEmpty()) startDate = sdf.parse(startStr);
                if (endStr != null && !endStr.isEmpty()) endDate = sdf.parse(endStr);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_kanal_pembayaran.jsp:40");}
        } else {
            if ("3_minggu".equals(rentangWaktu)) cal.add(Calendar.WEEK_OF_YEAR, -3);
            else if ("1_bulan".equals(rentangWaktu)) cal.add(Calendar.MONTH, -1);
            else if ("3_bulan".equals(rentangWaktu)) cal.add(Calendar.MONTH, -3);
            else if ("6_bulan".equals(rentangWaktu)) cal.add(Calendar.MONTH, -6);
            else if ("1_tahun".equals(rentangWaktu)) cal.add(Calendar.YEAR, -1);
            else if ("3_tahun".equals(rentangWaktu)) cal.add(Calendar.YEAR, -3);
            else cal.add(Calendar.WEEK_OF_YEAR, -1); 
            startDate = cal.getTime();
        }
    }

    Calendar endCal = Calendar.getInstance();
    endCal.setTime(endDate);
    endCal.set(Calendar.HOUR_OF_DAY, 23);
    endCal.set(Calendar.MINUTE, 59);
    endCal.set(Calendar.SECOND, 59);
    endDate = endCal.getTime();
    
    Calendar startCal = Calendar.getInstance();
    startCal.setTime(startDate);
    startCal.set(Calendar.HOUR_OF_DAY, 0);
    startCal.set(Calendar.MINUTE, 0);
    startCal.set(Calendar.SECOND, 0);
    startDate = startCal.getTime();

    Session sess = null;
    try {
        sess = HibernateUtil.openSession();
        
        // Asumsi: Kolom penyimpan kanal/metode di tabel cicilan_pembayaran bernama 'via'
        String sql = "select coalesce(cast(cp.validator as varchar), 'Kasir / Manual') as kanal, " +
                     "count(cp.id) as jml_trx, " +
                     "sum(cp.nilai) as total_nominal " +
                     "from cicilan_pembayaran cp " +
                     "left join kegiatan a on cp.kegiatan = a.id " +
                     "left join mahasiswa c on a.mahasiswa = c.id " +
                     "left join biodata_calon_mahasiswa b on a.calon_mahasiswa = b.id " +
                     "left join jurusan j on a.jurusan = j.id " +
                     "where a.aktif = true ";

        if (!isAllTime) {
            sql += " and cp.tanggal >= :start and cp.tanggal <= :end ";
        }

        // Kriteria Relasi Dasbor
        if (q != null && !q.trim().isEmpty()) {
            sql += " and (c.nama ilike :buktilike or b.nama ilike :buktilike or c.nim ilike :buktilike or b.no_registrasi ilike :buktilike) ";
        }
        if (ta != null && !ta.isEmpty()) sql += " and a.tahun_akademik = :ta ";
        if (smt != null && !smt.isEmpty()) sql += (smt.equals("Genap") ? " and a.semster % 2 = 0 " : " and a.semster % 2 = 1 ");
        if (jkId != null && !jkId.isEmpty()) sql += " and a.jenis_kegiatan = " + jkId;
        
        if (jurId != null && !jurId.isEmpty()) {
            sql += " and a.jurusan = " + jurId;
        } else if (fakId != null && !fakId.isEmpty()) {
            sql += " and j.fakultas = " + fakId;
        }

        sql += " group by coalesce(cast(cp.validator as varchar), 'Kasir / Manual') order by total_nominal desc";

        org.hibernate.SQLQuery query = sess.createSQLQuery(sql);
        
        if (!isAllTime) {
            query.setTimestamp("start", startDate);
            query.setTimestamp("end", endDate);
        }
        if (ta != null && !ta.isEmpty()) query.setString("ta", ta);
        if (q != null && !q.trim().isEmpty()) query.setString("buktilike", "%" + q.trim() + "%");

        List<Object[]> rows = query.list();
        JSONArray arr = new JSONArray();

        for (Object[] r : rows) {
            JSONObject o = new JSONObject();
            o.put("kanal", r[0] != null ? r[0].toString() : "Kasir / Manual");
            o.put("jmlTrx", r[1] != null ? ((Number)r[1]).longValue() : 0);
            o.put("nominal", r[2] != null ? ((Number)r[2]).doubleValue() : 0.0);
            arr.put(o);
        }

        JSONObject res = new JSONObject();
        res.put("status", "00");
        res.put("data", arr);
        out.print(res.toString()); out.flush();

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pembayaran/_service_kanal_pembayaran.jsp:128");
        out.print("{\"status\":\"error\", \"message\":\"Terjadi galat pada peladen saat mengkalkulasi kanal pembayaran.\"}"); out.flush();
    } finally {
        if(sess != null && sess.isOpen()) { try{sess.disconnect();}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_kanal_pembayaran.jsp:131");} try{sess.close();}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_kanal_pembayaran.jsp:131");} }
        try{HibernateUtil.closeSessionQuietly(sess);}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_kanal_pembayaran.jsp:132");}
    }
%>