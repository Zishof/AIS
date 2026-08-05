<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*, org.hibernate.*, org.hibernate.criterion.*, org.json.*" %>
<%@ page import="ais.common.*, ais.database.hibernate.*, ais.database.model.asset.*" %>
<%
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    
    JSONObject jsonResponse = new JSONObject();
    Session sess = null;

    try {
        String action = request.getParameter("action");
        sess = HibernateUtil.openSession();

        // =================================================================================
        // ACTION: GET SUMMARY (Menghitung Total ke-8 Kategori untuk KPI)
        // =================================================================================
        if ("get_summary".equals(action)) {
            int c1 = ((Number) sess.createCriteria(PermintaanPengadaanMasterAsset.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.isNull("disetujuiOleh")).setProjection(Projections.rowCount()).uniqueResult()).intValue();
            
            int c2 = ((Number) sess.createCriteria(PermintaanPengadaanMasterAsset.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.isNotNull("tanggalPersetujuan"))
                .setProjection(Projections.rowCount()).uniqueResult()).intValue();

            int c3 = ((Number) sess.createCriteria(PemesananPengadaanMasterAsset.class)
                .add(Restrictions.isNull("disetujuiOleh")).setProjection(Projections.rowCount()).uniqueResult()).intValue();

            int c4 = ((Number) sess.createCriteria(PemesananPengadaanMasterAsset.class)
                .add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.isNotNull("tanggalPersetujuan"))
                .setProjection(Projections.rowCount()).uniqueResult()).intValue();

            int c5 = ((Number) sess.createCriteria(PembayaranPengadaanMasterAssetDetail.class)
                .createAlias("pembayaranPengadaanMasterAsset", "pembayaranPengadaanMasterAsset")
                .add(Restrictions.isNotNull("pembayaranPengadaanMasterAsset.disetujuiOleh"))
                .add(Restrictions.isNotNull("pembayaranPengadaanMasterAsset.tanggalPersetujuan"))
                .setProjection(Projections.rowCount()).uniqueResult()).intValue();

            int c6 = ((Number) sess.createCriteria(PerjanjianKerjasamaMasterAsset.class)
                .add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.isNotNull("tanggalPersetujuan"))
                .setProjection(Projections.rowCount()).uniqueResult()).intValue();

            int c7 = ((Number) sess.createCriteria(PermintaanPengadaanMasterAsset.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.isNotNull("ditolakOleh")).add(Restrictions.isNotNull("tanggalDitolak"))
                .setProjection(Projections.rowCount()).uniqueResult()).intValue();

            int c8 = ((Number) sess.createCriteria(PemesananPengadaanMasterAsset.class)
                .add(Restrictions.isNotNull("ditolakOleh")).add(Restrictions.isNotNull("tanggalDitolak"))
                .setProjection(Projections.rowCount()).uniqueResult()).intValue();

            jsonResponse.put("status", "success");
            jsonResponse.put("pengajuan_pr", c1);
            jsonResponse.put("persetujuan_pr", c2);
            jsonResponse.put("pengajuan_po", c3);
            jsonResponse.put("persetujuan_po", c4);
            jsonResponse.put("pembayaran_po", c5);
            jsonResponse.put("perjanjian_ks", c6);
            jsonResponse.put("tolak_pr", c7);
            jsonResponse.put("tolak_po", c8);

            out.print(jsonResponse.toString());
            out.flush();
        }
        
        // =================================================================================
        // ACTION: GET DATA TABEL (Data Json untuk PopUp Modal & Tab Utama)
        // =================================================================================
        else if ("get_data_tabel".equals(action)) {
            String tipe = request.getParameter("tipe");
            JSONArray dataArray = new JSONArray();

            if ("pengajuan_pr".equals(tipe) || "persetujuan_pr".equals(tipe) || "tolak_pr".equals(tipe)) {
                Criteria crit = sess.createCriteria(PermintaanPengadaanMasterAsset.class)
                                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
                
                if ("pengajuan_pr".equals(tipe)) {
                    crit.add(Restrictions.isNull("disetujuiOleh"));
                } else if ("persetujuan_pr".equals(tipe)) {
                    crit.add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.isNotNull("tanggalPersetujuan"));
                } else if ("tolak_pr".equals(tipe)) {
                    crit.add(Restrictions.isNotNull("ditolakOleh")).add(Restrictions.isNotNull("tanggalDitolak"));
                }
                
                crit.addOrder(Order.desc("id"));
                List<PermintaanPengadaanMasterAsset> list = crit.list();
                for(PermintaanPengadaanMasterAsset d : list) {
                    JSONObject o = new JSONObject();
                    o.put("kode", d.getKode() != null ? d.getKode() : "-");
                    o.put("waktu_buat", d.getTanggalPembuatan() != null ? Common.dateFormat.get().format(d.getTanggalPembuatan()) : "");
                    o.put("unit_pemohon", d.getSatuanKerja() != null ? d.getSatuanKerja().getNama() : "-");
                    o.put("jumlah", Common.numberFormat.get().format(d.getNilai()));
                    o.put("waktu_acc", d.getTanggalPersetujuan() != null ? Common.dateFormat.get().format(d.getTanggalPersetujuan()) : "");
                    o.put("user_acc", d.getDisetujuiOleh() != null ? d.getDisetujuiOleh().getUserNama() : "-");
                    o.put("waktu_tolak", d.getTanggalDitolak() != null ? Common.dateFormat.get().format(d.getTanggalDitolak()) : "");
                    o.put("user_tolak", d.getDitolakOleh() != null ? d.getDitolakOleh().getUserNama() : "-");
                    dataArray.put(o);
                }
            } 
            else if ("pengajuan_po".equals(tipe) || "persetujuan_po".equals(tipe) || "tolak_po".equals(tipe)) {
                Criteria crit = sess.createCriteria(PemesananPengadaanMasterAsset.class);
                
                if ("pengajuan_po".equals(tipe)) {
                    crit.add(Restrictions.isNull("disetujuiOleh"));
                } else if ("persetujuan_po".equals(tipe)) {
                    crit.add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.isNotNull("tanggalPersetujuan"));
                } else if ("tolak_po".equals(tipe)) {
                    crit.add(Restrictions.isNotNull("ditolakOleh")).add(Restrictions.isNotNull("tanggalDitolak"));
                }
                
                crit.addOrder(Order.desc("id"));
                List<PemesananPengadaanMasterAsset> list = crit.list();
                for(PemesananPengadaanMasterAsset d : list) {
                    JSONObject o = new JSONObject();
                    o.put("kode", d.getKode() != null ? d.getKode() : "-");
                    o.put("waktu_buat", d.getTanggalPembuatan() != null ? Common.dateFormat.get().format(d.getTanggalPembuatan()) : "");
                    o.put("unit_pemohon", d.getSatuanKerja() != null ? d.getSatuanKerja().getNama() : "-");
                    o.put("jumlah", Common.numberFormat.get().format(d.getNilai()));
                    o.put("waktu_acc", d.getTanggalPersetujuan() != null ? Common.dateFormat.get().format(d.getTanggalPersetujuan()) : "");
                    o.put("user_acc", d.getDisetujuiOleh() != null ? d.getDisetujuiOleh().getUserNama() : "-");
                    o.put("waktu_tolak", d.getTanggalDitolak() != null ? Common.dateFormat.get().format(d.getTanggalDitolak()) : "");
                    o.put("user_tolak", d.getDitolakOleh() != null ? d.getDitolakOleh().getUserNama() : "-");
                    dataArray.put(o);
                }
            } 
            else if ("pembayaran_po".equals(tipe)) {
                Criteria crit = sess.createCriteria(PembayaranPengadaanMasterAssetDetail.class)
                        .createAlias("penerimaanPengadaanMasterAsset", "penerimaanPengadaanMasterAsset")
                        .createAlias("pembayaranPengadaanMasterAsset", "pembayaranPengadaanMasterAsset")
                        .createAlias("penerimaanPengadaanMasterAsset.pemesananPengadaanMasterAsset", "pemesananPengadaanMasterAsset")
                        .add(Restrictions.isNotNull("pembayaranPengadaanMasterAsset.disetujuiOleh"))
                        .add(Restrictions.isNotNull("pembayaranPengadaanMasterAsset.tanggalPersetujuan"))
                        .addOrder(Order.desc("id"));
                
                List<PembayaranPengadaanMasterAssetDetail> list = crit.list();
                for(PembayaranPengadaanMasterAssetDetail d : list) {
                    JSONObject o = new JSONObject();
                    o.put("kode", d.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset().getKode());
                    o.put("waktu_terima", d.getPenerimaanPengadaanMasterAsset().getTanggalPembuatan() != null ? Common.dateFormat.get().format(d.getPenerimaanPengadaanMasterAsset().getTanggalPembuatan()) : "");
                    o.put("waktu_bayar", d.getPembayaranPengadaanMasterAsset().getTanggalPersetujuan() != null ? Common.dateFormat.get().format(d.getPembayaranPengadaanMasterAsset().getTanggalPersetujuan()) : "");
                    o.put("penyedia", d.getPembayaranPengadaanMasterAsset().getPenyedia() != null ? d.getPembayaranPengadaanMasterAsset().getPenyedia().getNama() : "-");
                    o.put("jumlah", Common.numberFormat.get().format(d.getDibayar()));
                    dataArray.put(o);
                }
            }
            else if ("perjanjian_ks".equals(tipe)) {
                Criteria crit = sess.createCriteria(PerjanjianKerjasamaMasterAsset.class)
                        .add(Restrictions.isNotNull("disetujuiOleh"))
                        .add(Restrictions.isNotNull("tanggalPersetujuan"))
                        .addOrder(Order.desc("id"));
                
                List<PerjanjianKerjasamaMasterAsset> list = crit.list();
                for(PerjanjianKerjasamaMasterAsset d : list) {
                    JSONObject o = new JSONObject();
                    o.put("kode", d.getKode() != null ? d.getKode() : "-");
                    o.put("waktu_buat", d.getTanggalPembuatan() != null ? Common.dateFormat.get().format(d.getTanggalPembuatan()) : "");
                    o.put("waktu_acc", d.getTanggalPersetujuan() != null ? Common.dateFormat.get().format(d.getTanggalPersetujuan()) : "");
                    o.put("user_acc", d.getDisetujuiOleh() != null ? d.getDisetujuiOleh().getUserNama() : "-");
                    o.put("jumlah", Common.numberFormat.get().format(d.getDp())); 
                    dataArray.put(o);
                }
            }

            JSONObject tableJson = new JSONObject();
            tableJson.put("data", dataArray);
            out.print(tableJson.toString());
            out.flush();
        }

    } catch (Exception e) {
        out.print("{\"error\": \"Terjadi kesalahan sistem: " + e.getMessage() + "\"}");
        out.flush();
    } finally {
        if (sess != null && sess.isOpen()) {
            try { 
                sess.close(); 
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pengadaan/_monitor_pengadaan_service.jsp:179");}
        }
    }
%>