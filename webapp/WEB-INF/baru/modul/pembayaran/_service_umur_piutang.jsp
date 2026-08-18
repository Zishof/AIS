<%@page import="ais.database.model.Kegiatan"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%@page import="ais.common.Common"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.util.List"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>

<%
    try { out.clear(); out.clearBuffer(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_umur_piutang.jsp:13");}
    response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
    if (Common.getCurrentUser(request) == null) { out.print("{\"status\":\"error\", \"message\":\"Sesi Berakhir\"}"); return; }

    String ta = request.getParameter("tahunAkademik");
    String smt = request.getParameter("semester");
    String fakId = request.getParameter("fakultasId");
    String jurId = request.getParameter("jurusanId");
    String jkId = request.getParameter("jkId");
    
    if (ta == null || ta.isEmpty()) ta = Common.getCurrentTahunAkademik();
    if (smt == null || smt.isEmpty()) smt = "Ganjil"; // Fallback aman
    
    Session sess = null;

    try {
        sess = HibernateUtil.openSession();
        
        // Kalkulasi Indeks Semester Berjalan (Patokan TA dan Semester yang Dipilih di Filter)
        // Asumsi TA format: "2025/2026"
        int selectedYear = 0;
        try { selectedYear = Integer.parseInt(ta.split("/")[0]); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_umur_piutang.jsp:34");}
        int selectedSmt = smt.equalsIgnoreCase("Genap") ? 1 : 0;
        int currentIdx = (selectedYear * 2) + selectedSmt;

        // Logika SQL native menggunakan fungsi matematis untuk menentukan selisih umur semester
        // Semester Transaksi = (Tahun_Akademik_Transaksi * 2) + (Semester_Genap_Atau_Ganjil)
        // Umur (Aging) = Current_Idx - Trans_Idx
        
        String sql = "select " +
            "(case when :jurId is not null then j.nama else coalesce(f.nama, 'Lainnya') end) as kelompok, " +
            "sum(case when (:currentIdx - (cast(substring(a.tahun_akademik from 1 for 4) as integer) * 2 + (case when a.semster % 2 = 0 then 1 else 0 end))) <= 0 then (a.tagihan - a.dibayar) else 0 end) as piutang_berjalan, " +
            "sum(case when (:currentIdx - (cast(substring(a.tahun_akademik from 1 for 4) as integer) * 2 + (case when a.semster % 2 = 0 then 1 else 0 end))) = 1 then (a.tagihan - a.dibayar) else 0 end) as piutang_1smt, " +
            "sum(case when (:currentIdx - (cast(substring(a.tahun_akademik from 1 for 4) as integer) * 2 + (case when a.semster % 2 = 0 then 1 else 0 end))) = 2 then (a.tagihan - a.dibayar) else 0 end) as piutang_2smt, " +
            "sum(case when (:currentIdx - (cast(substring(a.tahun_akademik from 1 for 4) as integer) * 2 + (case when a.semster % 2 = 0 then 1 else 0 end))) >= 3 then (a.tagihan - a.dibayar) else 0 end) as piutang_lama " +
            "from kegiatan a " +
            "left join jurusan j on a.jurusan = j.id " +
            "left join fakultas f on j.fakultas = f.id " +
            "where a.aktif = true and (a.tagihan - a.dibayar) > 0.1 and length(a.tahun_akademik) >= 4 ";

        // Penambahan Kriteria Filter
        if (jurId != null && !jurId.isEmpty()) {
            sql += " and a.jurusan = " + jurId;
        } else if (fakId != null && !fakId.isEmpty()) {
            sql += " and j.fakultas = " + fakId;
        }
        
        if (jkId != null && !jkId.isEmpty()) {
            sql += " and a.jenis_kegiatan = " + jkId;
        }

        sql += " group by kelompok order by kelompok asc";

        org.hibernate.SQLQuery query = sess.createSQLQuery(sql);
        
        // Atur Parameter
        query.setInteger("currentIdx", currentIdx);
        query.setString("jurId", (jurId != null && !jurId.isEmpty() ? jurId : null)); // Null safe inject

        List<Object[]> rows = query.list();
        JSONArray dataArray = new JSONArray();

        for (Object[] r : rows) {
            String kelompok = r[0] != null ? r[0].toString() : "-";
            Double piutangBerjalan = r[1] != null ? ((Number)r[1]).doubleValue() : 0.0;
            Double piutang1Smt = r[2] != null ? ((Number)r[2]).doubleValue() : 0.0;
            Double piutang2Smt = r[3] != null ? ((Number)r[3]).doubleValue() : 0.0;
            Double piutangLama = r[4] != null ? ((Number)r[4]).doubleValue() : 0.0;
            
            Double totalRow = piutangBerjalan + piutang1Smt + piutang2Smt + piutangLama;
            
            if (totalRow > 0.01) {
                JSONObject o = new JSONObject();
                o.put("kelompok", kelompok);
                o.put("berjalan", piutangBerjalan);
                o.put("smt1", piutang1Smt);
                o.put("smt2", piutang2Smt);
                o.put("lama", piutangLama);
                dataArray.put(o);
            }
        }
        
        JSONObject res = new JSONObject(); 
        res.put("status", "00"); 
        res.put("data", dataArray);
        
        out.print(res.toString()); 
        out.flush();
        
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pembayaran/_service_umur_piutang.jsp:103");
        out.print("{\"status\":\"error\", \"message\":\"Terjadi galat pada peladen saat kalkulasi umur piutang.\"}");
    } finally {
        if(sess != null && sess.isOpen()) { try{sess.disconnect();}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_umur_piutang.jsp:106");} try{sess.close();}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_umur_piutang.jsp:106");} }
        try{HibernateUtil.closeSessionQuietly(sess);}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_umur_piutang.jsp:107");}
    }
%>