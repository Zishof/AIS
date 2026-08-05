<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*, org.hibernate.*, org.hibernate.criterion.*, org.json.*" %>
<%@ page import="ais.common.*, ais.database.hibernate.*, ais.database.model.*" %>
<%
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    
    JSONObject jsonResponse = new JSONObject();
    Session sess = null;

    try {
        String action = request.getParameter("action");
        sess = HibernateUtil.openSession();

        // =================================================================================
        // ACTION: GET SUMMARY (Menghitung Total untuk KPI Dasbor Kepegawaian)
        // =================================================================================
        if ("get_summary".equals(action)) {
            int totalPegawaiAktif = ((Number) sess.createCriteria(Pegawai.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .setProjection(Projections.rowCount()).uniqueResult()).intValue();

            int totalPegawaiNonAktif = ((Number) sess.createCriteria(Pegawai.class)
                .add(Restrictions.eq("aktif", false))
                .setProjection(Projections.rowCount()).uniqueResult()).intValue();

            int totalUnitKerja = ((Number) sess.createCriteria(ais.database.model.rab.SatuanKerja.class)
                .add(Restrictions.eq("defaultItem", true))
                .setProjection(Projections.rowCount()).uniqueResult()).intValue();

            jsonResponse.put("status", "success");
            jsonResponse.put("total_aktif", totalPegawaiAktif);
            jsonResponse.put("total_nonaktif", totalPegawaiNonAktif);
            jsonResponse.put("total_unit_kerja", totalUnitKerja);

            out.print(jsonResponse.toString());
            out.flush();
        }
        
        // =================================================================================
        // ACTION: GET DATA TABEL & GRAFIK (Data Json untuk Tab Kategori Dasbor)
        // =================================================================================
        else if ("get_data_tabel".equals(action)) {
            String tipe = request.getParameter("tipe");
            JSONArray dataArray = new JSONArray();

            Criteria crit = sess.createCriteria(Pegawai.class, "p")
                .add(Restrictions.eq("p.aktif", true));

            ProjectionList projList = Projections.projectionList();
            String propertyName = "";

            if ("unit_kerja".equals(tipe)) {
                crit.createAlias("p.satuanKerja", "sk", Criteria.LEFT_JOIN);
                crit.add(Restrictions.or(Restrictions.isNull("p.satuanKerja"), Restrictions.eq("sk.defaultItem", true)));
                propertyName = "sk.nama";
            } else if ("pendidikan".equals(tipe)) {
                crit.createAlias("p.pendidikan", "pd", Criteria.LEFT_JOIN);
                propertyName = "pd.nama"; 
            } else if ("status".equals(tipe)) {
                crit.createAlias("p.statusKepegawaian", "skp", Criteria.LEFT_JOIN);
                propertyName = "skp.nama"; 
            } else if ("agama".equals(tipe)) {
                crit.createAlias("p.agama", "agm", Criteria.LEFT_JOIN);
                propertyName = "agm.nama"; 
            } else if ("tipe".equals(tipe)) {
                crit.createAlias("p.tipeMasaKerja", "tmk", Criteria.LEFT_JOIN);
                propertyName = "tmk.nama"; 
            } else if ("ptkp".equals(tipe)) {
                crit.createAlias("p.ptkpPegawai", "ptkp", Criteria.LEFT_JOIN);
                propertyName = "ptkp.nama"; 
            } else if ("ikatan".equals(tipe)) {
                crit.createAlias("p.ikatanKerjaDosen", "ikd", Criteria.LEFT_JOIN);
                propertyName = "ikd.nama"; 
            } else if ("masa_kerja".equals(tipe)) {
                crit.createAlias("p.masaKerja", "mk", Criteria.LEFT_JOIN);
                propertyName = "mk.nama"; 
            } else if ("asuransi".equals(tipe)) {
                crit.createAlias("p.asuransiPegawai1", "ap", Criteria.LEFT_JOIN);
                propertyName = "ap.nama"; 
            } else {
                propertyName = "p.id"; // Fallback
            }

            projList.add(Projections.groupProperty(propertyName));
            projList.add(Projections.rowCount());
            crit.setProjection(projList);

            List<Object[]> results = crit.list();
            int totalKeseluruhan = 0;

            for (Object[] row : results) {
                Object kategoriObj = row[0];
                Number jumlah = (Number) row[1];
                
                String namaKategori = (kategoriObj != null && !kategoriObj.toString().trim().isEmpty()) 
                                      ? kategoriObj.toString() : Common.getBahasaConfig("Tidak Ditentukan");

                JSONObject o = new JSONObject();
                o.put("kategori", namaKategori);
                o.put("jumlah", Common.numberFormat.get().format(jumlah.intValue())); // Untuk Tabel
                o.put("jumlah_raw", jumlah.intValue());                         // Tambahan Untuk Grafik
                dataArray.put(o);
                
                totalKeseluruhan += jumlah.intValue();
            }

            JSONObject tableJson = new JSONObject();
            tableJson.put("data", dataArray);
            tableJson.put("total_keseluruhan", Common.numberFormat.get().format(totalKeseluruhan));
            
            out.print(tableJson.toString());
            out.flush();
        }

    } catch (Exception e) {
        out.print("{\"error\": \"Terjadi kesalahan sistem: " + e.getMessage() + "\"}");
        out.flush();
    } finally {
        if (sess != null && sess.isOpen()) {
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kepegawaian/_monitor_kepegawaian_service.jsp:121");}
        }
    }
%>