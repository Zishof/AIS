<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*, org.hibernate.*, org.hibernate.criterion.*, org.json.*" %>
<%@ page import="ais.common.*, ais.database.hibernate.*, ais.database.model.*, ais.database.model.sekolah.*, ais.database.model.sop.*" %>
<%!
    // ---------------------------------------------------------------------------------
    // HELPER METHODS
    // ---------------------------------------------------------------------------------
    private Criterion getUserRestriction(Tbmuser user, String prefix) {
        String p = prefix.isEmpty() ? "" : prefix + ".";
        if (user != null && (user.getMahasiswa() != null || user.getSiswa() != null)) {
            return Restrictions.or(Restrictions.eq(p + "mahasiswa", user.getMahasiswa()), Restrictions.eq(p + "siswa", user.getSiswa()));
        }
        return Restrictions.eq(p + "diajukanOleh", user);
    }

    private String getOlehName(Tbmuser user, Siswa siswa, Mahasiswa mahasiswa) {
        if (user != null) return user.getUserNama();
        if (mahasiswa != null) return mahasiswa.getNama();
        if (siswa != null) return siswa.getNama();
        return "-";
    }

    private String getStatusTerakhir(Session session, DisposisiAlurSop disposisiAlurSop) {
        try {
            DisposisiAlurSop disposisiTerakhir = (DisposisiAlurSop) session.createCriteria(DisposisiAlurSop.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.isNotNull("alurSop"))
                    .add(Restrictions.eq("disposisiSop", disposisiAlurSop.getDisposisiSop()))
                    .addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

            if (disposisiTerakhir != null && !disposisiTerakhir.getId().equals(disposisiAlurSop.getId())) {
                if (disposisiTerakhir.getAlurSop() != null && disposisiTerakhir.getAlurSop().getPenolakanAdaDiSini()
                        && disposisiTerakhir.getSebelumnya() != null && disposisiTerakhir.getSebelumnya().getAlurSop() != null) {
                    return "<span class='text-danger fw-bold'><i class='fas fa-times-circle'></i> Ditolak: " + disposisiTerakhir.getSebelumnya().getAlurSop().getAktor() + " - " + disposisiTerakhir.getSebelumnya().getAlurSop().getNama() + "</span>";
                } else if (disposisiTerakhir.getDiajukanOleh() == null && disposisiTerakhir.getSiswa() == null && disposisiTerakhir.getMahasiswa() == null) {
                    return "<span class='text-info fw-bold'><i class='fas fa-hourglass-half'></i> Menunggu: " + disposisiTerakhir.getAlurSop().getAktor() + " - " + disposisiTerakhir.getAlurSop().getNama() + "</span>";
                } else {
                    String olehInfo = getOlehName(disposisiTerakhir.getDiajukanOleh(), disposisiTerakhir.getSiswa(), disposisiTerakhir.getMahasiswa());
                    String tahap = disposisiTerakhir.getSetelahnya() == null || disposisiTerakhir.getSetelahnya().getAlurSop() == null ? "" : "\"" + disposisiTerakhir.getSetelahnya().getAlurSop().getNama() + "\"";
                    return "<span class='text-success fw-bold'><i class='fas fa-check-circle'></i> Proses " + tahap + " oleh: " + olehInfo + " (" + disposisiTerakhir.getAlurSop().getAktor() + ")</span>";
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoppengajuananda/sop_monitor_data.jsp:43");}
        return "<span class='badge bg-primary bg-gradient'>Proses Berjalan</span>";
    }
%>
<%
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    
    JSONObject jsonResponse = new JSONObject();
    Session sess = null;
    
    try {
        Tbmuser tbmuser = Common.getCurrentUser(request);
        if(tbmuser == null) {
            out.print("{\"error\": \"Sesi telah habis\"}");
            return;
        }

        String action = request.getParameter("action");
        sess = HibernateUtil.openSession();

     // =================================================================================
        // ACTION: GET SUMMARY (MENGHITUNG JUMLAH DATA UNTUK KPI DASHBOARD)
        // =================================================================================
        if ("get_summary".equals(action)) {
            
            // 1. Hitung: Menunggu Disposisi
            Criteria cMenunggu = sess.createCriteria(DisposisiAlurSop.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .setProjection(Projections.rowCount())
                .add(Restrictions.or(Restrictions.isNull("selesai"), Restrictions.eq("selesai", false)))
                .add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop").add(Restrictions.isNotNull("alurSop.sop"))
                .createAlias("alurSop.aktorSop", "aktorSop", Criteria.LEFT_JOIN)
                .createAlias("disposisiSop", "disposisiSop")
                .createAlias("sebelumnya", "sebelumnya", Criteria.LEFT_JOIN);
            cMenunggu.add(AktorSop.buatCriterion(tbmuser, true, cMenunggu))
                .add(Restrictions.and(Restrictions.isNull("diajukanOleh"), Restrictions.and(Restrictions.isNull("siswa"), Restrictions.isNull("mahasiswa"))))
                .add(Restrictions.isNull("setelahnya")).add(Restrictions.isNotNull("sebelumnya"))
                .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)));
            int countMenunggu = ((Number) cMenunggu.uniqueResult()).intValue();

            // 2. Hitung: Baru Disposisi (Disposisi Terbaru)
            int countDisposisi = ((Number) sess.createCriteria(DisposisiAlurSop.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .setProjection(Projections.rowCount()).add(Restrictions.isNotNull("alurSop"))
                .createAlias("alurSop", "alurSop").add(Restrictions.isNotNull("alurSop.sop"))
                .add(Restrictions.eq("alurSop.start", false)).add(Restrictions.isNotNull("waktu"))
                .createAlias("disposisiSop", "disposisiSop").add(getUserRestriction(tbmuser, ""))
                .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)))
                .uniqueResult()).intValue();

         // 3. Hitung: Pengajuan Baru (Diperbarui agar sinkron dengan tabel riil)
            // Sebelumnya: Menghitung Template AlurSop (Sistem mendeteksi 315 jenis formulir SOP)
            // Sekarang: Menghitung transaksi DisposisiAlurSop yang benar-benar sedang diproses (1 data)
            int countPengajuan = ((Number) sess.createCriteria(DisposisiAlurSop.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .setProjection(Projections.rowCount()).add(Restrictions.isNotNull("alurSop"))
                .createAlias("alurSop", "alurSop").add(Restrictions.isNotNull("alurSop.sop"))
                .add(Restrictions.isNull("diajukanOleh")).add(Restrictions.isNull("mahasiswa")).add(Restrictions.isNull("siswa"))
                .createAlias("disposisiSop", "disposisiSop").add(getUserRestriction(tbmuser, "disposisiSop"))
                .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)))
                .uniqueResult()).intValue();

            // 4. Hitung: Telah Selesai
            Criterion critSelesai = Restrictions.isNull("alurSop.setelahnya");
            critSelesai = Restrictions.and(critSelesai, Restrictions.isNull("alurSop.setelahnya2"));
            critSelesai = Restrictions.and(critSelesai, Restrictions.isNull("alurSop.setelahnya3"));
            critSelesai = Restrictions.and(critSelesai, Restrictions.isNull("alurSop.setelahnya4"));
            critSelesai = Restrictions.and(critSelesai, Restrictions.isNull("alurSop.setelahnya5"));
            critSelesai = Restrictions.or(critSelesai, Restrictions.eq("selesai", true));
            
            int countSelesai = ((Number) sess.createCriteria(DisposisiAlurSop.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .setProjection(Projections.rowCount()).add(Restrictions.isNotNull("alurSop"))
                .createAlias("alurSop", "alurSop").add(Restrictions.isNotNull("alurSop.sop"))
                .add(Restrictions.eq("alurSop.start", false))
                .add(Restrictions.or(Restrictions.eq("selesai", true), Restrictions.isNull("setelahnya")))
                .add(critSelesai).add(Restrictions.isNotNull("waktu")).createAlias("disposisiSop", "disposisiSop")
                .add(getUserRestriction(tbmuser, "disposisiSop"))
                .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)))
                .uniqueResult()).intValue();

            // 5. Hitung: Menunggu Aktor Lain
            int countMenungguAktor = ((Number) sess.createCriteria(DisposisiAlurSop.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .setProjection(Projections.rowCount()).add(Restrictions.isNotNull("alurSop"))
                .createAlias("alurSop", "alurSop").add(Restrictions.isNotNull("alurSop.sop"))
                .add(Restrictions.isNull("diajukanOleh")).add(Restrictions.isNull("mahasiswa")).add(Restrictions.isNull("siswa"))
                .createAlias("disposisiSop", "disposisiSop").add(getUserRestriction(tbmuser, "disposisiSop"))
                .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)))
                .uniqueResult()).intValue();

            // 6. Hitung: Mendekati Deadline
            Criteria cDeadline = sess.createCriteria(DisposisiAlurSop.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .setProjection(Projections.rowCount()).add(Restrictions.isNotNull("alurSop"))
                .createAlias("alurSop", "alurSop").add(Restrictions.isNotNull("alurSop.sop"))
                .createAlias("alurSop.aktorSop", "aktorSop", Criteria.LEFT_JOIN)
                .createAlias("disposisiSop", "disposisiSop").createAlias("sebelumnya", "sebelumnya", Criteria.LEFT_JOIN);
            cDeadline.add(AktorSop.buatCriterion(tbmuser, true, cDeadline))
                .add(Restrictions.isNull("diajukanOleh")).add(Restrictions.isNull("mahasiswa")).add(Restrictions.isNull("siswa"))
                .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)));
            int countDeadline = ((Number) cDeadline.uniqueResult()).intValue();

            // Status Konfigurasi Urutan SOP
            boolean urutanSopAktif = Common.getKonfigurasi("konfigurasi_urutan_sop", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF);

            // --- TIPS DEBUGGING (Opsional): Cetak ke Console Server Tomcat ---
            // System.out.println("--- DEBUG HASIL GET_SUMMARY ---");
            // System.out.println("Menunggu: " + countMenunggu + " | Disposisi: " + countDisposisi + " | Pengajuan: " + countPengajuan);

            // =========================================================================
            // DI BAWAH INI ADALAH KODE YANG ANDA KIRIMKAN (Memasukkan hasil ke JSON)
            // =========================================================================
            jsonResponse.put("status", "success");
            jsonResponse.put("urutanSopAktif", urutanSopAktif);
            jsonResponse.put("menunggu", countMenunggu);
            jsonResponse.put("disposisi", countDisposisi);
            jsonResponse.put("pengajuan", countPengajuan);
            jsonResponse.put("selesai", countSelesai);
            jsonResponse.put("menunggu_aktor", countMenungguAktor);
            jsonResponse.put("deadline", countDeadline);

            // Wajib di-print agar bisa dibaca oleh AJAX DataTables
            out.print(jsonResponse.toString());
            out.flush();
        }
     
     
     // =================================================================================
        // ACTION: DOWNLOAD EXCEL LANGSUNG DARI SERVER (MENDAPATKAN SELURUH DATA TANPA LIMIT)
        // =================================================================================
        else if ("download_excel".equals(action)) {
            String tipe = request.getParameter("tipe"); 
            if(tipe == null) tipe = "menunggu_disposisi";

            // Memaksa peramban (browser) mengunduh keluaran sebagai file Excel
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment; filename=\"Rekap_SOP_" + tipe.toUpperCase() + ".xls\"");

            // Membuat kerangka tabel HTML (Akan terbaca sempurna sebagai kolom dan baris oleh MS Excel)
            out.println("<html><body>");
            out.println("<table border='1'>");
            out.println("<tr>");
            out.println("<th style='background-color:#1a4087; color:white;'>No</th>");
            out.println("<th style='background-color:#1a4087; color:white;'>Nama SOP</th>");
            out.println("<th style='background-color:#1a4087; color:white;'>Pengaju</th>");
            out.println("<th style='background-color:#1a4087; color:white;'>Waktu Pengajuan</th>");
            out.println("<th style='background-color:#1a4087; color:white;'>Aktor Tujuan</th>");
            out.println("<th style='background-color:#1a4087; color:white;'>Tahapan SOP</th>");
            out.println("<th style='background-color:#1a4087; color:white;'>Batas Waktu</th>");
            out.println("</tr>");

            // Kriteria Utama (Sama persis dengan get_data_tabel & get_summary)
            Criteria criteria = sess.createCriteria(DisposisiAlurSop.class);
            criteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
                    .add(Restrictions.isNotNull("alurSop.sop"));

            // Logika Penyaringan Berdasarkan Tipe
            if ("pengajuan_baru".equals(tipe)) {
                criteria.add(Restrictions.isNull("diajukanOleh")).add(Restrictions.isNull("mahasiswa")).add(Restrictions.isNull("siswa"))
                        .createAlias("disposisiSop", "disposisiSop").add(getUserRestriction(tbmuser, "disposisiSop"))
                        .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)))
                        .addOrder(Order.desc("id"));
            } else if ("baru_disposisi".equals(tipe)) {
                criteria.add(Restrictions.eq("alurSop.start", false)).add(Restrictions.isNotNull("waktu"))
                        .createAlias("disposisiSop", "disposisiSop").add(getUserRestriction(tbmuser, "")) 
                        .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)))
                        .addOrder(Order.desc("id"));
            } else if ("menunggu_disposisi".equals(tipe)) {
                criteria.add(Restrictions.or(Restrictions.isNull("selesai"), Restrictions.eq("selesai", false)))
                        .createAlias("alurSop.aktorSop", "aktorSop", Criteria.LEFT_JOIN)
                        .createAlias("disposisiSop", "disposisiSop").createAlias("sebelumnya", "sebelumnya", Criteria.LEFT_JOIN);
                criteria.add(AktorSop.buatCriterion(tbmuser, true, criteria))
                        .add(Restrictions.and(Restrictions.isNull("diajukanOleh"), Restrictions.and(Restrictions.isNull("siswa"), Restrictions.isNull("mahasiswa"))))
                        .add(Restrictions.isNull("setelahnya")).add(Restrictions.isNotNull("sebelumnya"))
                        .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)))
                        .addOrder(Order.desc("id"));
            } else if ("selesai".equals(tipe)) {
                Criterion criterion1 = Restrictions.isNull("alurSop.setelahnya");
                criterion1 = Restrictions.and(criterion1, Restrictions.isNull("alurSop.setelahnya2"));
                criterion1 = Restrictions.and(criterion1, Restrictions.isNull("alurSop.setelahnya3"));
                criterion1 = Restrictions.and(criterion1, Restrictions.isNull("alurSop.setelahnya4"));
                criterion1 = Restrictions.and(criterion1, Restrictions.isNull("alurSop.setelahnya5"));
                criterion1 = Restrictions.or(criterion1, Restrictions.eq("selesai", true));

                criteria.add(Restrictions.eq("alurSop.start", false))
                        .add(Restrictions.or(Restrictions.eq("selesai", true), Restrictions.isNull("setelahnya")))
                        .add(criterion1).add(Restrictions.isNotNull("waktu")).createAlias("disposisiSop", "disposisiSop")
                        .add(getUserRestriction(tbmuser, "disposisiSop"))
                        .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)))
                        .addOrder(Order.desc("id"));
            } else if ("menunggu_aktor".equals(tipe)) {
                criteria.add(Restrictions.isNull("diajukanOleh")).add(Restrictions.isNull("mahasiswa")).add(Restrictions.isNull("siswa"))
                        .createAlias("disposisiSop", "disposisiSop").add(getUserRestriction(tbmuser, "disposisiSop"))
                        .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)))
                        .addOrder(Order.desc("id"));
            } else if ("deadline".equals(tipe)) {
                criteria.createAlias("alurSop.aktorSop", "aktorSop", Criteria.LEFT_JOIN)
                        .createAlias("disposisiSop", "disposisiSop").createAlias("sebelumnya", "sebelumnya", Criteria.LEFT_JOIN);
                criteria.add(AktorSop.buatCriterion(tbmuser, true, criteria))
                        .add(Restrictions.isNull("diajukanOleh")).add(Restrictions.isNull("mahasiswa")).add(Restrictions.isNull("siswa"))
                        .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)))
                        .addOrder(Order.desc("waktuMaksimal"));
            }

            // PERHATIKAN: Tidak ada criteria.setMaxResults(200) di sini agar seluruh data terambil
            List<DisposisiAlurSop> listData = criteria.list();
            int nomorUrut = 1;
            
            for(DisposisiAlurSop d : listData) {
                String namaSop = d.getDisposisiSop().getSop().getNama() != null ? d.getDisposisiSop().getSop().getNama() : "-";
                String namaPengaju = getOlehName(d.getDisposisiSop().getDiajukanOleh(), d.getDisposisiSop().getSiswa(), d.getDisposisiSop().getMahasiswa());
                String waktuPengajuan = d.getDisposisiSop().getWaktu() != null ? Common.dateFormat.get().format(d.getDisposisiSop().getWaktu()) : "-";
                String aktor = d.getAlurSop().getAktor() != null ? d.getAlurSop().getAktor() : "-";
                String tahap = d.getAlurSop().getNama() != null ? d.getAlurSop().getNama() : "-";
                String deadline = d.getWaktuMaksimal() != null ? Common.dateFormat51.get().format(d.getWaktuMaksimal()) : "-";

                out.println("<tr>");
                out.println("<td>" + (nomorUrut++) + "</td>");
                out.println("<td>" + namaSop + "</td>");
                out.println("<td>" + namaPengaju + "</td>");
                out.println("<td>" + waktuPengajuan + "</td>");
                out.println("<td>" + aktor + "</td>");
                out.println("<td>" + tahap + "</td>");
                out.println("<td>" + deadline + "</td>");
                out.println("</tr>");
            }
            
            out.println("</table>");
            out.println("<jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body></html>");
            out.flush();
            return; // Hentikan eksekusi di sini agar file excel tidak tercampur dengan whitespace file JSP
        }
        
        // =================================================================================
        // ACTION: GET DATA TABEL (UNTUK DATATABLES)
        // =================================================================================
        else if ("get_data_tabel".equals(action)) {
            String tipe = request.getParameter("tipe"); 
            if(tipe == null) tipe = "menunggu_disposisi";

            JSONArray dataArray = new JSONArray();
            Criteria criteria = sess.createCriteria(DisposisiAlurSop.class);
            
            criteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
                    .add(Restrictions.isNotNull("alurSop.sop"));

            if ("pengajuan_baru".equals(tipe)) {
                criteria.add(Restrictions.isNull("diajukanOleh")).add(Restrictions.isNull("mahasiswa")).add(Restrictions.isNull("siswa"))
                        .createAlias("disposisiSop", "disposisiSop")
                        .add(getUserRestriction(tbmuser, "disposisiSop"))
                        .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)))
                        .addOrder(Order.desc("id"));
                        
            } else if ("baru_disposisi".equals(tipe)) {
                criteria.add(Restrictions.eq("alurSop.start", false))
                        .add(Restrictions.isNotNull("waktu")).createAlias("disposisiSop", "disposisiSop")
                        .add(getUserRestriction(tbmuser, "")) 
                        .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)))
                        .addOrder(Order.desc("id"));
                        
            } else if ("menunggu_disposisi".equals(tipe)) {
                criteria.add(Restrictions.or(Restrictions.isNull("selesai"), Restrictions.eq("selesai", false)))
                        .createAlias("alurSop.aktorSop", "aktorSop", Criteria.LEFT_JOIN)
                        .createAlias("disposisiSop", "disposisiSop")
                        .createAlias("sebelumnya", "sebelumnya", Criteria.LEFT_JOIN);
                criteria.add(AktorSop.buatCriterion(tbmuser, true, criteria))
                        .add(Restrictions.and(Restrictions.isNull("diajukanOleh"), Restrictions.and(Restrictions.isNull("siswa"), Restrictions.isNull("mahasiswa"))))
                        .add(Restrictions.isNull("setelahnya")).add(Restrictions.isNotNull("sebelumnya"))
                        .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)))
                        .addOrder(Order.desc("id"));
                        
            } else if ("selesai".equals(tipe)) {
                Criterion criterion1 = Restrictions.isNull("alurSop.setelahnya");
                criterion1 = Restrictions.and(criterion1, Restrictions.isNull("alurSop.setelahnya2"));
                criterion1 = Restrictions.and(criterion1, Restrictions.isNull("alurSop.setelahnya3"));
                criterion1 = Restrictions.and(criterion1, Restrictions.isNull("alurSop.setelahnya4"));
                criterion1 = Restrictions.and(criterion1, Restrictions.isNull("alurSop.setelahnya5"));
                criterion1 = Restrictions.or(criterion1, Restrictions.eq("selesai", true));

                criteria.add(Restrictions.eq("alurSop.start", false))
                        .add(Restrictions.or(Restrictions.eq("selesai", true), Restrictions.isNull("setelahnya")))
                        .add(criterion1).add(Restrictions.isNotNull("waktu"))
                        .createAlias("disposisiSop", "disposisiSop")
                        .add(getUserRestriction(tbmuser, "disposisiSop"))
                        .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)))
                        .addOrder(Order.desc("id"));
                        
            } else if ("menunggu_aktor".equals(tipe)) {
                criteria.add(Restrictions.isNull("diajukanOleh")).add(Restrictions.isNull("mahasiswa")).add(Restrictions.isNull("siswa"))
                        .createAlias("disposisiSop", "disposisiSop")
                        .add(getUserRestriction(tbmuser, "disposisiSop"))
                        .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)))
                        .addOrder(Order.desc("id"));
                        
            } else if ("deadline".equals(tipe)) {
                criteria.createAlias("alurSop.aktorSop", "aktorSop", Criteria.LEFT_JOIN)
                        .createAlias("disposisiSop", "disposisiSop")
                        .createAlias("sebelumnya", "sebelumnya", Criteria.LEFT_JOIN);
                criteria.add(AktorSop.buatCriterion(tbmuser, true, criteria))
                        .add(Restrictions.isNull("diajukanOleh")).add(Restrictions.isNull("mahasiswa")).add(Restrictions.isNull("siswa"))
                        .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)))
                        .addOrder(Order.desc("waktuMaksimal"));
            }

            criteria.setMaxResults(200); 
            List<DisposisiAlurSop> listData = criteria.list();
            
            for(DisposisiAlurSop d : listData) {
                JSONObject obj = new JSONObject();
                obj.put("id", d.getId());
                String namaPengaju = getOlehName(d.getDisposisiSop().getDiajukanOleh(), d.getDisposisiSop().getSiswa(), d.getDisposisiSop().getMahasiswa());
                
                String infoSop = "<b>" + d.getDisposisiSop().getSop().getNama() + "</b><br>" + 
                                 "<small class='text-muted'>Oleh: " + namaPengaju + "</small>";
                obj.put("sop_nama", infoSop);
                obj.put("waktu_pengajuan", d.getDisposisiSop().getWaktu() != null ? Common.dateFormat.get().format(d.getDisposisiSop().getWaktu()) : "-");
                obj.put("aktor", d.getAlurSop().getAktor());
                obj.put("tahap", d.getAlurSop().getNama());
                obj.put("status_detail", getStatusTerakhir(sess, d));

                if(d.getWaktuMaksimal() != null) {
                     obj.put("deadline", "<span class='text-danger fw-bold'><i class='fas fa-clock'></i> " + Common.dateFormat51.get().format(d.getWaktuMaksimal()) + "</span>");
                } else {
                     obj.put("deadline", "-");
                }
                dataArray.put(obj);
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
        if (sess != null) {
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoppengajuananda/sop_monitor_data.jsp:386");}
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersoppengajuananda/sop_monitor_data.jsp:387");}
        }
        HibernateUtil.closeSessionQuietly(sess);
    }
%>