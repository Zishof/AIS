<%@page import="ais.database.model.Kegiatan"%>
<%@page import="ais.common.Common"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.util.List"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>

<%
    try { out.clear(); out.clearBuffer(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_distribusi_pelunasan.jsp:10");}
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

        // 1. Klausa Kondisi Dasar
        String baseWhereClause = " from kegiatan a "
                + " left join mahasiswa c on a.mahasiswa = c.id "
                + " left join biodata_calon_mahasiswa b on a.calon_mahasiswa = b.id "
                + " left join jurusan j on a.jurusan = j.id "
                + " left join fakultas f on j.fakultas = f.id "
                + " where a.aktif=true and a.tagihan > 0.1 ";

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

        // Variabel penentu bidang pengelompokan (Grouping Field)
        // Jika filter Jurusan aktif, maka kelompokkan per Jurusan. Jika tidak, per Fakultas.
        String groupingField = (jurId != null && !jurId.trim().isEmpty()) ? "j.nama" : "coalesce(f.nama, 'Lainnya')";

        // 2. Kueri SQL Terangkum Ganda (Nested Aggregation)
        // Sub-query menghitung total tagihan & dibayar PER KEPALA (NIM)
        // Query Luar menghitung status LUNAS, MENCICIL, BELUM BAYAR per Kelompok Prodi/Fakultas
        String sqlData = "select "
            + " kelompok, "
            + " sum(case when (tot_tagihan - tot_dibayar) <= 0.1 then 1 else 0 end) as lunas, "
            + " sum(case when tot_dibayar > 0.1 and (tot_tagihan - tot_dibayar) > 0.1 then 1 else 0 end) as mencicil, "
            + " sum(case when tot_dibayar <= 0.1 and tot_tagihan > 0.1 then 1 else 0 end) as belum_bayar, "
            + " count(*) as total_mhs "
            + " from ("
            + "   select "
            + "     coalesce(c.nim, b.no_registrasi) as nim, "
            + "     " + groupingField + " as kelompok, "
            + "     sum(a.tagihan) as tot_tagihan, "
            + "     sum(a.dibayar) as tot_dibayar "
            + baseWhereClause
            + "     group by coalesce(c.nim, b.no_registrasi), " + groupingField
            + " ) as sub_mhs "
            + " group by kelompok order by kelompok asc";

        org.hibernate.SQLQuery qData = sess.createSQLQuery(sqlData);
        
        if (q != null && !q.trim().isEmpty()) qData.setString("buktilike", "%" + q.trim() + "%");
        if (ta != null && !ta.isEmpty()) qData.setString("tahunAkademik", ta);
        
        List<Object[]> objects = qData.list();
        JSONArray dataArray = new JSONArray();

        for (Object[] o : objects) {
            String kelompok = o[0] == null ? "-" : o[0].toString();
            Long lunas = o[1] != null ? ((Number)o[1]).longValue() : 0L;
            Long mencicil = o[2] != null ? ((Number)o[2]).longValue() : 0L;
            Long belumBayar = o[3] != null ? ((Number)o[3]).longValue() : 0L;
            Long totalMhs = o[4] != null ? ((Number)o[4]).longValue() : 0L;

            JSONObject barisData = new JSONObject();
            barisData.put("kelompok", kelompok);
            barisData.put("lunas", lunas);
            barisData.put("mencicil", mencicil);
            barisData.put("belum_bayar", belumBayar);
            barisData.put("total_mhs", totalMhs);
            
            dataArray.put(barisData);
        }
        
        JSONObject res = new JSONObject(); 
        res.put("status", "00"); 
        res.put("data", dataArray);
        
        out.print(res.toString()); 
        out.flush();
        
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pembayaran/_service_distribusi_pelunasan.jsp:109");
        out.print("{\"status\":\"error\", \"message\":\"Terjadi galat pada peladen saat menarik data peta distribusi.\"}");
    } finally {
        if(sess != null && sess.isOpen()) { try{sess.disconnect();}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_distribusi_pelunasan.jsp:112");} try{sess.close();}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_distribusi_pelunasan.jsp:112");} }
        try{HibernateUtil.closeSessionQuietly(sess);}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_distribusi_pelunasan.jsp:113");}
    }
%>