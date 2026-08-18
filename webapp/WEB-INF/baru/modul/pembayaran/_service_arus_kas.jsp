<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>

<%
    try { out.clear(); out.clearBuffer(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_arus_kas.jsp:12");}
    response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
    if (Common.getCurrentUser(request) == null) { out.print("{\"status\":\"error\", \"message\":\"Sesi Berakhir\"}"); return; }

    String ta = request.getParameter("tahunAkademik");
    String smt = request.getParameter("semester");
    String fakId = request.getParameter("fakultasId");
    String jurId = request.getParameter("jurusanId");
    String jkId = request.getParameter("jkId");
    String q = request.getParameter("q"); 

    Session sess = null;

    try {
        sess = HibernateUtil.openSession();

        // 1. Kriteria Dasar untuk Tabel Kegiatan (Penyaring Umum)
        String baseWhereClause = " from kegiatan a "
                + " left join mahasiswa c on a.mahasiswa = c.id "
                + " left join biodata_calon_mahasiswa b on a.calon_mahasiswa = b.id "
                + " left join jurusan j on a.jurusan = j.id "
                + " left join fakultas f on j.fakultas = f.id "
                + " where a.aktif=true ";

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

        // 2. Ekstraksi Total Target Tagihan Semester
        String sqlTarget = "select sum(a.tagihan) " + baseWhereClause;
        org.hibernate.SQLQuery qTarget = sess.createSQLQuery(sqlTarget);
        if (q != null && !q.trim().isEmpty()) qTarget.setString("buktilike", "%" + q.trim() + "%");
        if (ta != null && !ta.isEmpty()) qTarget.setString("tahunAkademik", ta);
        
        Number targetNum = (Number) qTarget.uniqueResult();
        Double totalTarget = targetNum != null ? targetNum.doubleValue() : 0.0;

        // 3. Ekstraksi Realisasi Arus Kas (Uang Masuk) per Bulan
        String sqlReal = "select to_char(cp.tanggal, 'YYYY-MM') as bln, sum(cp.nilai) as dibayar "
                + " from cicilan_pembayaran cp "
                + " join kegiatan a on cp.kegiatan = a.id "
                + " left join mahasiswa c on a.mahasiswa = c.id "
                + " left join biodata_calon_mahasiswa b on a.calon_mahasiswa = b.id "
                + " left join jurusan j on a.jurusan = j.id "
                + " left join fakultas f on j.fakultas = f.id "
                + " where a.aktif=true ";

        // Terapkan filter yang sama pada query ke-2
        if (q != null && !q.trim().isEmpty()) {
            sqlReal += " and (c.nama ilike :buktilike or b.nama ilike :buktilike or c.nim ilike :buktilike or b.no_registrasi ilike :buktilike) ";
        }
        if (jurId != null && !jurId.isEmpty()) sqlReal += " and a.jurusan = " + jurId;
        else if (fakId != null && !fakId.isEmpty()) sqlReal += " and j.fakultas = " + fakId;
        
        if (jkId != null && !jkId.isEmpty()) sqlReal += " and a.jenis_kegiatan = " + jkId;
        if (ta != null && !ta.isEmpty()) sqlReal += " and a.tahun_akademik = :tahunAkademik ";
        if (smt != null && !smt.isEmpty()) sqlReal += (smt.equals("Genap") ? " and a.semster % 2 = 0 " : " and a.semster % 2 = 1 ");

        sqlReal += " group by to_char(cp.tanggal, 'YYYY-MM') order by bln asc";

        org.hibernate.SQLQuery qReal = sess.createSQLQuery(sqlReal);
        if (q != null && !q.trim().isEmpty()) qReal.setString("buktilike", "%" + q.trim() + "%");
        if (ta != null && !ta.isEmpty()) qReal.setString("tahunAkademik", ta);

        List<Object[]> rows = qReal.list();
        JSONArray rincianArray = new JSONArray();

        // 4. Kalkulasi Akumulasi Tertimbang
        Double akumulasiReal = 0.0;
        
        // Translasi Nama Bulan untuk Estetika Frontend
        String[] namaBulan = {"Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember"};

        for (Object[] o : rows) {
            String blnRaw = o[0] != null ? o[0].toString() : "";
            Double nominalBulanIni = o[1] != null ? ((Number)o[1]).doubleValue() : 0.0;
            
            akumulasiReal += nominalBulanIni;
            
            String labelBulan = blnRaw;
            try {
                String[] parts = blnRaw.split("-");
                int mIdx = Integer.parseInt(parts[1]) - 1;
                labelBulan = namaBulan[mIdx] + " " + parts[0];
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_arus_kas.jsp:110");}

            JSONObject r = new JSONObject();
            r.put("bulan", labelBulan);
            r.put("nominal", nominalBulanIni);
            r.put("akumulasi", akumulasiReal);
            r.put("persen", totalTarget > 0 ? (akumulasiReal * 100.0) / totalTarget : 0.0);
            
            rincianArray.put(r);
        }
        
        JSONObject resData = new JSONObject();
        resData.put("target", totalTarget);
        resData.put("rincian", rincianArray);

        JSONObject res = new JSONObject(); 
        res.put("status", "00"); 
        res.put("data", resData);
        
        out.print(res.toString()); 
        out.flush();
        
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pembayaran/_service_arus_kas.jsp:133");
        out.print("{\"status\":\"error\", \"message\":\"Terjadi galat pada peladen saat menarik analisis arus kas.\"}");
    } finally {
        if(sess != null && sess.isOpen()) { try{sess.disconnect();}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_arus_kas.jsp:136");} try{sess.close();}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_arus_kas.jsp:136");} }
        try{HibernateUtil.closeSessionQuietly(sess);}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_arus_kas.jsp:137");}
    }
%>