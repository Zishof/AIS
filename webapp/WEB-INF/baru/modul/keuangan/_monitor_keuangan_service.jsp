<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*, org.hibernate.*, org.hibernate.criterion.*, org.json.*" %>
<%@ page import="ais.common.*, ais.database.hibernate.*" %>
<%@ page import="ais.database.model.akunting.*" %>
<%
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    
    JSONObject jsonResponse = new JSONObject();
    Session sess = null;
    
    try {
        String action = request.getParameter("action");
        sess = HibernateUtil.openSession();
        
        // =================================================================================
        // ACTION: GET SUMMARY (Menghitung Total untuk Dasbor KPI)
        // =================================================================================
        if ("get_summary".equals(action)) {
            // 1. Pengajuan Kasbon
            int c1 = ((Number) sess.createCriteria(UangMuka.class)
                .add(Restrictions.isNull("disetujuiOleh")).setProjection(Projections.rowCount()).uniqueResult()).intValue();
                
            // 2. Persetujuan Kasbon
            int c2 = ((Number) sess.createCriteria(UangMuka.class)
                .add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.isNotNull("tanggalPersetujuan"))
                .setProjection(Projections.rowCount()).uniqueResult()).intValue();
                
            // 3. Pengajuan Talangan / Kas Besar
            int c3 = ((Number) sess.createCriteria(DanaTalangan.class)
                .add(Restrictions.isNull("disetujuiOleh")).setProjection(Projections.rowCount()).uniqueResult()).intValue();
                
            // 4. Persetujuan Talangan / Kas Besar
            int c4 = ((Number) sess.createCriteria(DanaTalangan.class)
                .add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.isNotNull("tanggalPersetujuan"))
                .setProjection(Projections.rowCount()).uniqueResult()).intValue();
                
            // 5. Pengajuan Laporan Pertanggungjawaban (LPJ)
            int c5 = ((Number) sess.createCriteria(Pertangungjawaban.class)
                .add(Restrictions.isNull("disetujuiOleh")).setProjection(Projections.rowCount()).uniqueResult()).intValue();
                
            // 6. Persetujuan Laporan Pertanggungjawaban (LPJ)
            int c6 = ((Number) sess.createCriteria(Pertangungjawaban.class)
                .add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.isNotNull("tanggalPersetujuan"))
                .setProjection(Projections.rowCount()).uniqueResult()).intValue();
                
            // 7. Pengeluaran Kas Kecil
            int c7 = ((Number) sess.createCriteria(KasKecil.class)
                .add(Restrictions.isNull("disetujuiOleh")).setProjection(Projections.rowCount()).uniqueResult()).intValue();
                
            // 8. Persetujuan Kas Kecil
            int c8 = ((Number) sess.createCriteria(KasKecil.class)
                .add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.isNotNull("tanggalPersetujuan"))
                .setProjection(Projections.rowCount()).uniqueResult()).intValue();
                
            // 9. Pengeluaran Penggantian Kas Kecil
            int c9 = ((Number) sess.createCriteria(PenggantianKasKecil.class)
                .add(Restrictions.isNotNull("kasKecil")).add(Restrictions.isNull("disetujuiOleh"))
                .setProjection(Projections.rowCount()).uniqueResult()).intValue();
                
            // 10. Persetujuan Penggantian Kas Kecil
            int c10 = ((Number) sess.createCriteria(PenggantianKasKecil.class)
                .add(Restrictions.isNotNull("kasKecil")).add(Restrictions.isNotNull("disetujuiOleh"))
                .add(Restrictions.isNotNull("tanggalPersetujuan")).setProjection(Projections.rowCount()).uniqueResult()).intValue();
                
            // 11. Persetujuan Kas Besar
            int c11 = ((Number) sess.createCriteria(KasBesar.class)
                .add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.isNotNull("tanggalPersetujuan"))
                .setProjection(Projections.rowCount()).uniqueResult()).intValue();

            jsonResponse.put("status", "success");
            jsonResponse.put("pengajuan_kasbon", c1);
            jsonResponse.put("persetujuan_kasbon", c2);
            jsonResponse.put("pengajuan_talangan", c3);
            jsonResponse.put("persetujuan_talangan", c4);
            jsonResponse.put("pengajuan_lpj", c5);
            jsonResponse.put("persetujuan_lpj", c6);
            jsonResponse.put("pengajuan_kaskecil", c7);
            jsonResponse.put("persetujuan_kaskecil", c8);
            jsonResponse.put("pengajuan_gantikaskecil", c9);
            jsonResponse.put("persetujuan_gantikaskecil", c10);
            jsonResponse.put("persetujuan_kasbesar", c11);

            out.print(jsonResponse.toString());
            out.flush();
        }
        
        // =================================================================================
        // ACTION: GET DATA TABEL (Data Json untuk DataTables)
        // =================================================================================
        else if ("get_data_tabel".equals(action)) {
            String tipe = request.getParameter("tipe");
            JSONArray dataArray = new JSONArray();
            Criteria crit = null;

            // Inisialisasi Kriteria Berdasarkan Tipe
            if ("pengajuan_kasbon".equals(tipe) || "persetujuan_kasbon".equals(tipe)) {
                crit = sess.createCriteria(UangMuka.class);
                if ("pengajuan_kasbon".equals(tipe)) crit.add(Restrictions.isNull("disetujuiOleh"));
                else crit.add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.isNotNull("tanggalPersetujuan"));
            } 
            else if ("pengajuan_talangan".equals(tipe) || "persetujuan_talangan".equals(tipe)) {
                crit = sess.createCriteria(DanaTalangan.class);
                if ("pengajuan_talangan".equals(tipe)) crit.add(Restrictions.isNull("disetujuiOleh"));
                else crit.add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.isNotNull("tanggalPersetujuan"));
            }
            else if ("pengajuan_lpj".equals(tipe) || "persetujuan_lpj".equals(tipe)) {
                crit = sess.createCriteria(Pertangungjawaban.class);
                if ("pengajuan_lpj".equals(tipe)) crit.add(Restrictions.isNull("disetujuiOleh"));
                else crit.add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.isNotNull("tanggalPersetujuan"));
            }
            else if ("pengajuan_kaskecil".equals(tipe) || "persetujuan_kaskecil".equals(tipe)) {
                crit = sess.createCriteria(KasKecil.class);
                if ("pengajuan_kaskecil".equals(tipe)) crit.add(Restrictions.isNull("disetujuiOleh"));
                else crit.add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.isNotNull("tanggalPersetujuan"));
            }
            else if ("pengajuan_gantikaskecil".equals(tipe) || "persetujuan_gantikaskecil".equals(tipe)) {
                crit = sess.createCriteria(PenggantianKasKecil.class).add(Restrictions.isNotNull("kasKecil"));
                if ("pengajuan_gantikaskecil".equals(tipe)) crit.add(Restrictions.isNull("disetujuiOleh"));
                else crit.add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.isNotNull("tanggalPersetujuan"));
            }
            else if ("persetujuan_kasbesar".equals(tipe)) {
                crit = sess.createCriteria(KasBesar.class);
                crit.add(Restrictions.isNotNull("disetujuiOleh")).add(Restrictions.isNotNull("tanggalPersetujuan"));
            }

            // Eksekusi Pengambilan Data (Bebas dari Reflection Error)
            if (crit != null) {
                crit.addOrder(Order.desc("id"));
                List<?> listData = crit.list();

                for (Object item : listData) {
                    JSONObject o = new JSONObject();
                    
                    String kode = "-";
                    String waktuBuat = "";
                    String waktuAcc = "";
                    String unitPemohon = "-";
                    String userAcc = "-";
                    Double jumlah = 0.0;
                    
                    // Pendekatan Casting InstanceOf (Sangat Efisien & Aman)
                    if (item instanceof UangMuka) {
                        UangMuka d = (UangMuka) item;
                        kode = d.getKode();
                        if (d.getTanggalPembuatan() != null) waktuBuat = Common.dateFormat.get().format(d.getTanggalPembuatan());
                        if (d.getTanggalPersetujuan() != null) waktuAcc = Common.dateFormat.get().format(d.getTanggalPersetujuan());
                        if (d.getSatuanKerja() != null) unitPemohon = d.getSatuanKerja().getNama();
                        if (d.getDisetujuiOleh() != null) userAcc = d.getDisetujuiOleh().getUserNama();
                        jumlah = d.getNilai();
                    } 
                    else if (item instanceof DanaTalangan) {
                        DanaTalangan d = (DanaTalangan) item;
                        kode = d.getKode();
                        if (d.getTanggalPembuatan() != null) waktuBuat = Common.dateFormat.get().format(d.getTanggalPembuatan());
                        if (d.getTanggalPersetujuan() != null) waktuAcc = Common.dateFormat.get().format(d.getTanggalPersetujuan());
                        if (d.getSatuanKerja() != null) unitPemohon = d.getSatuanKerja().getNama();
                        if (d.getDisetujuiOleh() != null) userAcc = d.getDisetujuiOleh().getUserNama();
                        jumlah = d.getNilai();
                    }
                    else if (item instanceof Pertangungjawaban) {
                        Pertangungjawaban d = (Pertangungjawaban) item;
                        kode = d.getKode();
                        if (d.getTanggalPembuatan() != null) waktuBuat = Common.dateFormat.get().format(d.getTanggalPembuatan());
                        if (d.getTanggalPersetujuan() != null) waktuAcc = Common.dateFormat.get().format(d.getTanggalPersetujuan());
                        if (d.getSatuanKerja() != null) unitPemohon = d.getSatuanKerja().getNama();
                        if (d.getDisetujuiOleh() != null) userAcc = d.getDisetujuiOleh().getUserNama();
                        jumlah = d.getNilai();
                    }
                    else if (item instanceof KasKecil) {
                        KasKecil d = (KasKecil) item;
                        kode = d.getKode();
                        if (d.getTanggalPembuatan() != null) waktuBuat = Common.dateFormat.get().format(d.getTanggalPembuatan());
                        if (d.getTanggalPersetujuan() != null) waktuAcc = Common.dateFormat.get().format(d.getTanggalPersetujuan());
                        if (d.getSatuanKerja() != null) unitPemohon = d.getSatuanKerja().getNama();
                        if (d.getDisetujuiOleh() != null) userAcc = d.getDisetujuiOleh().getUserNama();
                        jumlah = d.getNilai();
                    }
                    else if (item instanceof PenggantianKasKecil) {
                        PenggantianKasKecil d = (PenggantianKasKecil) item;
                        kode = d.getKode();
                        if (d.getTanggalPembuatan() != null) waktuBuat = Common.dateFormat.get().format(d.getTanggalPembuatan());
                        if (d.getTanggalPersetujuan() != null) waktuAcc = Common.dateFormat.get().format(d.getTanggalPersetujuan());
                        if (d.getSatuanKerja() != null) unitPemohon = d.getSatuanKerja().getNama();
                        if (d.getDisetujuiOleh() != null) userAcc = d.getDisetujuiOleh().getUserNama();
                        jumlah = d.getNilai();
                    }
                    else if (item instanceof KasBesar) {
                        KasBesar d = (KasBesar) item;
                        kode = d.getKode();
                        if (d.getTanggalPembuatan() != null) waktuBuat = Common.dateFormat.get().format(d.getTanggalPembuatan());
                        if (d.getTanggalPersetujuan() != null) waktuAcc = Common.dateFormat.get().format(d.getTanggalPersetujuan());
                        if (d.getSatuanKerja() != null) unitPemohon = d.getSatuanKerja().getNama();
                        if (d.getDisetujuiOleh() != null) userAcc = d.getDisetujuiOleh().getUserNama();
                        jumlah = d.getNilai();
                    }

                    // Mapping ke JSON Output
                    o.put("kode", kode != null ? kode : "-");
                    o.put("waktu_buat", waktuBuat);
                    o.put("unit_pemohon", unitPemohon);
                    o.put("jumlah", Common.numberFormat.get().format(jumlah != null ? jumlah : 0.0));
                    o.put("waktu_acc", waktuAcc);
                    o.put("user_acc", userAcc);
                    
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
        // Penutupan Sesi secara Aman
        if (sess != null && sess.isOpen()) {
            try { 
                sess.close(); 
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/keuangan/_monitor_keuangan_service.jsp:224");
                // Ignore exception on close
            }
        }
    }
%>