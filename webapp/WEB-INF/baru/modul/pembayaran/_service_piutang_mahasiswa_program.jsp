<%@page import="ais.database.model.Kegiatan"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%@page import="ais.common.Common"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Disjunction"%>
<%@page import="org.hibernate.type.Type"%>
<%@page import="org.hibernate.Hibernate"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.util.List"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%
    try { out.clear(); out.clearBuffer(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_piutang_mahasiswa_program.jsp:20");}
    response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
    if (Common.getCurrentUser(request) == null) { out.print("{\"status\":\"error\", \"message\":\"Sesi Berakhir\"}"); return; }

    String ta = request.getParameter("tahunAkademik"); 
    String smt = request.getParameter("semester");
    String fakId = request.getParameter("fakultasId"); // Tangkap Parameter Filter Fakultas
    String jurId = request.getParameter("jurusanId"); 
    String jkId = request.getParameter("jkId");
    String q = request.getParameter("q"); // Tangkap Parameter Pencarian NIM/Nama
    
    Session sess = null;

    try {
        sess = HibernateUtil.openSession();
        String sql = "sum(case when this_.tagihan>0.1 then 1 else 0 end) as jumlahTagihan, sum(case when this_.dibayar>0.1 then 1 else 0 end) as jumlahDibayar, sum(case when (this_.tagihan-this_.dibayar)>0.1 then 1 else 0 end) as jumlahPiutang";

        Criteria crit = sess.createCriteria(Kegiatan.class).add(Restrictions.eq("aktif", true))
            .setProjection(Projections.projectionList()
                .add(Projections.groupProperty("program")).add(Projections.groupProperty("jenisKegiatan"))
                .add(Projections.sum("tagihan")).add(Projections.sum("dibayar"))
                .add(Projections.sqlProjection(sql, new String[]{"jumlahTagihan", "jumlahDibayar", "jumlahPiutang"}, new Type[]{Hibernate.DOUBLE, Hibernate.DOUBLE, Hibernate.DOUBLE})))
            .add(Restrictions.eq("tahunAkademik", ta))
            .add(Restrictions.isNotNull("program"))
            .add(Restrictions.isNotNull("jenisKegiatan"))
            .addOrder(Order.asc("program")).addOrder(Order.asc("jenisKegiatan")).setMaxResults(500);

        if (smt != null && !smt.isEmpty()) crit.add(Restrictions.in("semster", smt.equalsIgnoreCase("Genap") ? Common.genap : Common.ganjil));
        if (jkId != null && !jkId.isEmpty()) { JenisKegiatan jk = (JenisKegiatan)sess.get(JenisKegiatan.class, Long.parseLong(jkId)); if(jk!=null) crit.add(Restrictions.eq("jenisKegiatan", jk)); }
        
        // Pengecekan Fakultas & Jurusan Bertingkat
        if (jurId != null && !jurId.isEmpty()) { 
            Jurusan j = (Jurusan)sess.get(Jurusan.class, Long.parseLong(jurId)); 
            if(j!=null) crit.add(Restrictions.eq("jurusan", j)); 
        } else if (fakId != null && !fakId.isEmpty()) {
            Fakultas f = (Fakultas)sess.get(Fakultas.class, Long.parseLong(fakId));
            if (f != null) {
                crit.createAlias("jurusan", "j");
                crit.add(Restrictions.eq("j.fakultas", f));
            }
        }
        
        // Pengecekan Pencarian berdasarkan NIM / Nama Mahasiswa (Reguler maupun Calon)
        if (q != null && !q.trim().isEmpty()) {
            // Menggunakan LEFT_JOIN (Kriteria Relasi Eksternal) agar tidak terjadi kehilangan data 
            // jika salah satu kolom relasi mahasiswa atau calon_mahasiswa bernilai null
            crit.createAlias("mahasiswa", "mhs", Criteria.LEFT_JOIN);
            crit.createAlias("calonMahasiswa", "cmb", Criteria.LEFT_JOIN);
            
            // Membangun klausa OR (Disjunction) untuk fleksibilitas pencarian
            Disjunction orQuery = Restrictions.disjunction();
            orQuery.add(Restrictions.ilike("mhs.nim", q, MatchMode.ANYWHERE));
            orQuery.add(Restrictions.ilike("mhs.nama", q, MatchMode.ANYWHERE));
            orQuery.add(Restrictions.ilike("cmb.noRegistrasi", q, MatchMode.ANYWHERE));
            orQuery.add(Restrictions.ilike("cmb.nama", q, MatchMode.ANYWHERE));
            
            crit.add(orQuery);
        }

        List<Object[]> rows = crit.list(); JSONArray arr = new JSONArray();

        for (Object[] r : rows) {
            String prog = (String) r[0]; JenisKegiatan jk = (JenisKegiatan) r[1];
            Double tagihan = r[2] != null ? ((Number)r[2]).doubleValue() : 0.0;
            Double dibayar = r[3] != null ? ((Number)r[3]).doubleValue() : 0.0;
            Double jmlTag = r[4] != null ? ((Number)r[4]).doubleValue() : 0.0;
            Double jmlDib = r[5] != null ? ((Number)r[5]).doubleValue() : 0.0;
            Double jmlPiu = r[6] != null ? ((Number)r[6]).doubleValue() : 0.0;
            
            if (tagihan > 0.01) {
                JSONObject o = new JSONObject();
                String nmKegiatan = jk.getNamaKegiatan() != null ? jk.getNamaKegiatan() : "-";
                
                o.put("kelompok", prog != null ? prog : "-"); 
                o.put("jenisKegiatan", nmKegiatan);
                o.put("tagihan", tagihan); 
                o.put("dibayar", dibayar); 
                o.put("piutang", tagihan - dibayar);
                o.put("jmlTagihan", jmlTag); 
                o.put("jmlDibayar", jmlDib); 
                o.put("jmlPiutang", jmlPiu);
                o.put("persen", (dibayar * 100.0) / tagihan); 
                arr.put(o);
            }
        }
        JSONObject res = new JSONObject(); res.put("status", "00"); res.put("data", arr);
        out.print(res.toString()); out.flush();
    } catch (Exception e) { 
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pembayaran/_service_piutang_mahasiswa_program.jsp:108"); 
        out.print("{\"status\":\"error\", \"message\":\"Terjadi galat saat memproses data pangkalan.\"}"); 
        out.flush();
    } finally { 
        try{HibernateUtil.closeSessionQuietly(sess);}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_piutang_mahasiswa_program.jsp:112");} 
    }
%>