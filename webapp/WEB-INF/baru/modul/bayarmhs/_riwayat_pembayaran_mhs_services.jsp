<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.database.model.Kegiatan"%>
<%@page import="ais.database.model.ItemBiaya"%>
<%@page import="ais.database.model.CicilanPembayaran"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Disjunction"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.type.StandardBasicTypes"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Date"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>

<%!
    // =========================================================================================
    // FUNGSI PEMBANTU KONSOLIDASI FILTER (DRY: Don't Repeat Yourself)
    // =========================================================================================
    private void terapkanKriteriaPencarian(Criteria baseCrit, HttpServletRequest request) {
        String isSiswaStr = request.getParameter("hanya_untuk_mahasiswa"); 
        String keywordPelanggan = request.getParameter("q");
        String keywordItem = request.getParameter("keywordItem");
        
        String jkIdStr = request.getParameter("jkId");
        String paramHanyaJkId = request.getParameter("hanya_untuk_jenis_kegiatan");
        if (paramHanyaJkId != null && !paramHanyaJkId.trim().isEmpty()) { jkIdStr = paramHanyaJkId.trim(); }
        
        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");
        String smtStr = request.getParameter("smt");
        String tahunStr = request.getParameter("ta");
        String bulanStr = request.getParameter("bulan");
        String[] itemBiayaIds = request.getParameterValues("itemBiayaIds");

        if (isSiswaStr != null && !isSiswaStr.trim().isEmpty()) {
            if ("true".equalsIgnoreCase(isSiswaStr)) baseCrit.add(Restrictions.isNotNull("mhs.id"));
            else if ("false".equalsIgnoreCase(isSiswaStr)) baseCrit.add(Restrictions.isNotNull("calon.id"));
        }

        if (keywordPelanggan != null && !keywordPelanggan.trim().isEmpty()) {
            String searchStr = "%" + keywordPelanggan.trim().toLowerCase() + "%";
            Disjunction orQ = Restrictions.disjunction();
            orQ.add(Restrictions.ilike("mhs.nim", searchStr));
            orQ.add(Restrictions.ilike("mhs.nama", searchStr));
            orQ.add(Restrictions.ilike("calon.noRegistrasi", searchStr));
            orQ.add(Restrictions.ilike("calon.nama", searchStr));
            baseCrit.add(orQ);
        }
        
        if (keywordItem != null && !keywordItem.trim().isEmpty()) {
            String searchItemStr = "%" + keywordItem.trim().toLowerCase() + "%";
            Disjunction orQItem = Restrictions.disjunction();
            orQItem.add(Restrictions.ilike("ib.nama", searchItemStr));
            orQItem.add(Restrictions.ilike("cp.keterangan", searchItemStr));
            baseCrit.add(orQItem);
        }

        if (jkIdStr != null && !jkIdStr.trim().isEmpty()) {
            try { baseCrit.add(Restrictions.eq("keg.jenisKegiatan.id", Long.parseLong(jkIdStr))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_riwayat_pembayaran_mhs_services.jsp:67");}
        }

        if (startDateStr != null && !startDateStr.trim().isEmpty() && endDateStr != null && !endDateStr.trim().isEmpty()) {
            try {
                Date date1 = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(startDateStr);
                Date date2 = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(endDateStr);
                
                java.util.Calendar calEnd = java.util.Calendar.getInstance();
                calEnd.setTime(date2); 
                calEnd.set(java.util.Calendar.HOUR_OF_DAY, 23); 
                calEnd.set(java.util.Calendar.MINUTE, 59); 
                calEnd.set(java.util.Calendar.SECOND, 59);
                
                baseCrit.add(Restrictions.ge("cp.tanggal", date1));
                baseCrit.add(Restrictions.le("cp.tanggal", calEnd.getTime()));
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_riwayat_pembayaran_mhs_services.jsp:83");}
        }

        if (smtStr != null && !smtStr.trim().isEmpty()) {
            try { baseCrit.add(Restrictions.eq("keg.semster", Integer.parseInt(smtStr))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_riwayat_pembayaran_mhs_services.jsp:87");}
        }
        if (tahunStr != null && !tahunStr.trim().isEmpty()) {
            baseCrit.add(Restrictions.eq("keg.tahunAkademik", tahunStr.trim()));
        }
        if (bulanStr != null && !bulanStr.trim().isEmpty()) {
            try { baseCrit.add(Restrictions.sqlRestriction("extract(month from {alias}.tanggal) = ?", Integer.parseInt(bulanStr), StandardBasicTypes.INTEGER)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_riwayat_pembayaran_mhs_services.jsp:93");}
        }
        
        if (itemBiayaIds != null && itemBiayaIds.length > 0) {
            List<Long> ids = new ArrayList<Long>();
            for (String ibId : itemBiayaIds) {
                try { if(ibId != null && !ibId.trim().isEmpty()) ids.add(Long.parseLong(ibId)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_riwayat_pembayaran_mhs_services.jsp:99");}
            }
            if(!ids.isEmpty()) baseCrit.add(Restrictions.in("cp.itemBiaya.id", ids));
        }
    }
%>

<%
    try { out.clear(); out.clearBuffer(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_riwayat_pembayaran_mhs_services.jsp:107");}
    
    String action = request.getParameter("action");
    if (action == null || action.trim().isEmpty()) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Perintah aksi komunikasi tidak ditemukan.") + "\"}");
        out.flush(); return;
    }

    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        if ("EXPORT_DATA".equals(action)) { out.print("Akses Ditolak."); return; }
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
        out.flush(); return;
    }

    Session sess = null;

    try {
        sess = HibernateUtil.openSession();

        // =========================================================================================
        // AKSI KHUSUS 1: AMBIL ITEM BIAYA DINAMIS
        // =========================================================================================
        if ("GET_ITEM_BIAYA".equals(action)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            String filterJkIdStr = request.getParameter("jkId");
            
            JSONObject resultJson = new JSONObject();
            JSONArray dataArray = new JSONArray();

            if (filterJkIdStr != null && !filterJkIdStr.trim().isEmpty()) {
                Long filterJkId = Long.parseLong(filterJkIdStr);
                String hqlItem = "select ib from ItemBiaya ib " +
                                 "where exists (" +
                                 "   select 1 from DetailSettingBiaya dsb " +
                                 "   where dsb.itemBiaya.id = ib.id and dsb.settingBiaya.jenisKegiatan.id = :jkId" +
                                 ") " +
                                 "order by ib.nama asc";
                                 
                List<ItemBiaya> items = sess.createQuery(hqlItem).setLong("jkId", filterJkId).list();
                for(ItemBiaya ib : items) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", ib.getId());
                    obj.put("nama", ib.getNama());
                    dataArray.put(obj);
                }
            }
            
            resultJson.put("status", "00");
            resultJson.put("data", dataArray);
            out.print(resultJson.toString());
            out.flush(); return;
        }

        // =========================================================================================
        // AKSI KHUSUS 2: CETAK STRUK KUITANSI (MENGEMBALIKAN ALAMAT URL PDF)
        // =========================================================================================
        if ("CETAK_STRUK".equals(action)) {
            response.setContentType("text/plain"); // Format balasan adalah string URL murni atau pesan galat
            response.setCharacterEncoding("UTF-8");
            try {
                String idStr = request.getParameter("id");
                String jkIdStr = request.getParameter("jkId");
                String smtStr = request.getParameter("smt");
                String isMahasiswaStr = request.getParameter("isMahasiswa");

                if (idStr == null || jkIdStr == null || idStr.trim().isEmpty() || jkIdStr.trim().isEmpty()) {
                    response.setStatus(400);
                    out.print(Common.getBahasaConfig("Parameter identitas pelanggan tidak valid."));
                    out.flush(); return;
                }

                long id = Long.parseLong(idStr);
                long jkId = Long.parseLong(jkIdStr);
                int smtInt = (smtStr != null && !smtStr.trim().isEmpty()) ? Integer.parseInt(smtStr) : 1;
                boolean isMahasiswa = "true".equalsIgnoreCase(isMahasiswaStr);

                ais.database.model.VOMahasiswa personCetak = (ais.database.model.VOMahasiswa) (isMahasiswa 
                                ? ais.common.ConstantValues.ambil(ais.database.model.Mahasiswa.class.getName(), id, true) 
                                : ais.common.ConstantValues.ambil(ais.database.model.BiodataCalonMahasiswa.class.getName(), id, true));
                ais.database.model.JenisKegiatan jkCetak = (ais.database.model.JenisKegiatan) ais.common.ConstantValues.ambil(ais.database.model.JenisKegiatan.class.getName(), jkId, true);
                
                ais.database.model.Kegiatan kCetak = null;
                if (personCetak != null && jkCetak != null) {
                    kCetak = personCetak.ambilKegiatansRefresh(smtInt, jkCetak, true);
                }

                if (kCetak != null) {
                    java.io.File fileStruk = null;
                    if (isMahasiswa) {
                        fileStruk = ais.action.report.CommonReportHelper.cetakBuktipembayaranMahasiswa(kCetak, true);
                    } else {
                        fileStruk = ais.action.report.CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kCetak, true);
                    }
                    
                    if (fileStruk != null && fileStruk.exists()) {
                        String pathPdf = "/pdf?p=" + java.net.URLEncoder.encode(ais.common.Common.desEncrypter.get().encrypt(fileStruk.getName()), "UTF-8");
                        out.print(ais.common.Common.ROOT + pathPdf);
                    } else {
                        response.setStatus(500);
                        out.print(Common.getBahasaConfig("Gagal memproduksi dokumen struk pembayaran."));
                    }
                } else {
                    response.setStatus(404);
                    out.print(Common.getBahasaConfig("Data kegiatan transaksi pembayaran tidak ditemukan."));
                }
            } catch (Exception e) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/bayarmhs/_riwayat_pembayaran_mhs_services.jsp:219");
                response.setStatus(500);
                out.print(Common.getBahasaConfig("Galat internal peladen: ") + e.getMessage());
            }
            out.flush(); return;
        }

        // =========================================================================================
        // PERSIAPAN KRITERIA UNTUK LOAD & EXPORT DATA
        // =========================================================================================
        boolean isUserMhs = (tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null);
        boolean editPriv = false, deletePriv = false, isAdminPriv = false, bolehMerubahCicilan = false;
        
        if (tbmuser.getUserId() != null) {
            editPriv = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
            deletePriv = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE, tbmuser);
            if (Common.getApakahAdminLain(tbmuser)) { editPriv = true; deletePriv = true; isAdminPriv = true; }
        }

        if (!isUserMhs && tbmuser.hakAkses() != null) {
            try { bolehMerubahCicilan = tbmuser.hakAkses().getRoleId().trim().equalsIgnoreCase(Tbmrole.ADMINISTRATOR); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_riwayat_pembayaran_mhs_services.jsp:239");}
            if (!bolehMerubahCicilan) {
                String admLain = Common.getKonfigurasi("admin_yang_bisa_menghapus_data_pembayaran_mahasiswa", "am").getNilai();
                if (admLain != null) {
                    for (String a : admLain.split(";")) {
                        if (a.trim().equalsIgnoreCase(tbmuser.hakAkses().getRoleId())) { bolehMerubahCicilan = true; break; }
                    }
                }
            }
        }

        Criteria baseCrit = sess.createCriteria(CicilanPembayaran.class, "cp");
        baseCrit.createAlias("cp.kegiatan", "keg");
        baseCrit.createAlias("keg.mahasiswa", "mhs", org.hibernate.sql.JoinFragment.LEFT_OUTER_JOIN);
        baseCrit.createAlias("keg.calonMahasiswa", "calon", org.hibernate.sql.JoinFragment.LEFT_OUTER_JOIN);
        baseCrit.createAlias("cp.itemBiaya", "ib", org.hibernate.sql.JoinFragment.LEFT_OUTER_JOIN);

        terapkanKriteriaPencarian(baseCrit, request);
        SimpleDateFormat sdfGroup = new SimpleDateFormat("dd MMMM yyyy HH:mm");

        // =========================================================================================
        // AKSI: MENGAMBIL DATA UNTUK TABEL (AJAX JSON)
        // =========================================================================================
        if ("LOAD_DATA".equals(action)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            int pageNum = 1;
            if (request.getParameter("page") != null) {
                try { pageNum = Integer.parseInt(request.getParameter("page")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_riwayat_pembayaran_mhs_services.jsp:268");}
                if(pageNum < 1) pageNum = 1;
            }

            JSONObject resultJson = new JSONObject();
            JSONArray dataArray = new JSONArray();

            baseCrit.setProjection(Projections.rowCount());
            Number jmlNum = (Number) baseCrit.uniqueResult();
            int totalRecords = jmlNum != null ? jmlNum.intValue() : 0;
            
            baseCrit.setProjection(null);
            baseCrit.setResultTransformer(Criteria.ROOT_ENTITY);

            int limit = 15;
            int offset = (pageNum - 1) * limit;
            
            baseCrit.addOrder(Order.desc("cp.tanggal"));
            baseCrit.addOrder(Order.desc("cp.id"));
            baseCrit.setFirstResult(offset);
            baseCrit.setMaxResults(limit);
            
            List<CicilanPembayaran> listData = baseCrit.list();

            for(CicilanPembayaran cp : listData) {
                Kegiatan k = cp.getKegiatan();
                if(k == null) continue;
                
                JSONObject obj = new JSONObject();
                boolean isMhs = k.getMahasiswa() != null;
                Long entityId = isMhs ? k.getMahasiswa().getId() : (k.getCalonMahasiswa() != null ? k.getCalonMahasiswa().getId() : 0L);
                
                obj.put("groupId", (cp.getTanggal() != null ? sdfGroup.format(cp.getTanggal()) : "-") + "_" + entityId);
                obj.put("waktu", cp.getTanggal() != null ? sdfGroup.format(cp.getTanggal()) : "-");
                obj.put("via", cp.getJenisPembayaran() != null ? cp.getJenisPembayaran().getNama() : Common.getBahasaConfig("Manual / Lainnya"));
                
                obj.put("idCicilan", cp.getId());
                obj.put("idKegiatan", k.getId());
                obj.put("idPelanggan", entityId);
                obj.put("isMahasiswa", isMhs);
                obj.put("jkId", k.getJenisKegiatan() != null ? k.getJenisKegiatan().getId() : "");
                obj.put("smt", k.getSemster() != null ? k.getSemster() : 1);
                
                obj.put("nis", isMhs ? k.getMahasiswa().getNim() : (k.getCalonMahasiswa() != null ? k.getCalonMahasiswa().getNoRegistrasi() : "-"));
                obj.put("namaSiswa", isMhs ? k.getMahasiswa().getNama() : (k.getCalonMahasiswa() != null ? k.getCalonMahasiswa().getNama() : "-"));
                obj.put("kegiatan", k.getJenisKegiatan() != null ? k.getJenisKegiatan().getNamaKegiatan() : "-");
                obj.put("ta", k.getTahunAkademik() != null ? k.getTahunAkademik() : "-");
                
                boolean canEdit = editPriv || isAdminPriv;
                boolean canDelete = (deletePriv || isAdminPriv) && bolehMerubahCicilan && cp.getPostingHistory() == null;
                obj.put("bisaEdit", canEdit || canDelete);
                
                obj.put("nominal", cp.getNilai() != null ? cp.getNilai() : 0.0);
                obj.put("item", cp.getItemBiaya() != null ? cp.getItemBiaya().getNama() : "-");
                obj.put("ket", cp.getKeterangan() != null ? cp.getKeterangan() : "-");
                
                dataArray.put(obj);
            }
            
            int totalPages = (int) Math.ceil((double) totalRecords / limit);

            resultJson.put("status", "00");
            resultJson.put("data", dataArray);
            resultJson.put("total", totalRecords);
            resultJson.put("totalPages", totalPages);
            
            out.print(resultJson.toString());
            out.flush(); return;
        }

        // =========================================================================================
        // AKSI: MENGHASILKAN DOKUMEN EKSPOR (EXCEL & PDF)
        // =========================================================================================
        if ("EXPORT_DATA".equals(action)) {
            String exportType = request.getParameter("exportType");
            int startRow = 1, endRow = 1000;
            if (request.getParameter("startRow") != null) try { startRow = Integer.parseInt(request.getParameter("startRow")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_riwayat_pembayaran_mhs_services.jsp:344");}
            if (request.getParameter("endRow") != null) try { endRow = Integer.parseInt(request.getParameter("endRow")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_riwayat_pembayaran_mhs_services.jsp:345");}
            
            int exportOffset = startRow - 1;
            int exportLimit = endRow - startRow + 1;
            
            baseCrit.addOrder(Order.desc("cp.tanggal"));
            baseCrit.addOrder(Order.desc("cp.id"));
            baseCrit.setFirstResult(exportOffset < 0 ? 0 : exportOffset);
            baseCrit.setMaxResults(exportLimit < 1 ? 1 : exportLimit);
            
            List<CicilanPembayaran> listExport = baseCrit.list();

            if ("excel".equals(exportType)) {
                response.setContentType("application/vnd.ms-excel");
                response.setHeader("Content-Disposition", "attachment; filename=\"Riwayat_Pembayaran_" + ais.ui.util.WaktuUtil.getDate().getTime() + ".xls\"");
            } else {
                response.setContentType("text/html;charset=UTF-8");
            }
            %>
            <html>
            <head>
                <title><%= Common.getBahasaConfig("Dokumen Laporan Riwayat Transaksi") %></title>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; font-size: 12px; }
                    .table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }
                    .table th, .table td { border: 1px solid #ddd; padding: 10px; text-align: left; }
                    .table th { background-color: #f4f6f8; font-weight: bold; text-align: center; }
                    .num { text-align: right; }
                    .header-doc { text-align: center; margin-bottom: 25px; }
                </style>
            </head>
            <body <%= "pdf".equals(exportType) ? "onload='window.print()'" : "" %>>
                <div class="header-doc">
                    <h2 style="margin-bottom: 5px;"><%= Common.getBahasaConfig("Laporan Riwayat Transaksi Pelanggan") %></h2>
                    <p style="color: #666;"><%= Common.getBahasaConfig("Rentang Data Baris Ke:") %> <%= startRow %> - <%= endRow %></p>
                </div>
                <table class="table">
                    <thead>
                        <tr>
                            <th><%= Common.getBahasaConfig("No.") %></th>
                            <th><%= Common.getBahasaConfig("Waktu Transaksi") %></th>
                            <th><%= Common.getBahasaConfig("NIM / No.Reg") %></th>
                            <th><%= Common.getBahasaConfig("Nama Pelanggan") %></th>
                            <th><%= Common.getBahasaConfig("Kategori") %></th>
                            <th><%= Common.getBahasaConfig("Jenis Kegiatan") %></th>
                            <th><%= Common.getBahasaConfig("Rincian Item Biaya") %></th>
                            <th><%= Common.getBahasaConfig("Keterangan") %></th>
                            <th><%= Common.getBahasaConfig("Nominal (Rp)") %></th>
                        </tr>
                    </thead>
                    <tbody>
                        <% 
                        int no = 1; double granTotal = 0;
                        for(CicilanPembayaran cp : listExport) { 
                            Kegiatan k = cp.getKegiatan(); if(k == null) continue;
                            boolean isMhsE = k.getMahasiswa() != null;
                            String identitasE = isMhsE ? k.getMahasiswa().getNim() : (k.getCalonMahasiswa() != null ? k.getCalonMahasiswa().getNoRegistrasi() : "-");
                            String namaE = isMhsE ? k.getMahasiswa().getNama() : (k.getCalonMahasiswa() != null ? k.getCalonMahasiswa().getNama() : "-");
                            String statE = isMhsE ? Common.getBahasaConfig("Mahasiswa") : Common.getBahasaConfig("Calon Mahasiswa");
                            String tglE = cp.getTanggal() != null ? sdfGroup.format(cp.getTanggal()) : "-";
                            String jkE = k.getJenisKegiatan() != null ? k.getJenisKegiatan().getNamaKegiatan() : "-";
                            String ibE = cp.getItemBiaya() != null ? cp.getItemBiaya().getNama() : "-";
                            double valE = cp.getNilai() != null ? cp.getNilai() : 0; granTotal += valE;
                        %>
                        <tr>
                            <td style="text-align: center;"><%= no++ %></td>
                            <td><%= tglE %></td>
                            <td><%= identitasE %></td>
                            <td><%= namaE %></td>
                            <td style="text-align: center;"><%= statE %></td>
                            <td><%= jkE %></td>
                            <td><%= ibE %></td>
                            <td><%= cp.getKeterangan() != null ? cp.getKeterangan() : "-" %></td>
                            <td class="num"><%= String.format("%,.0f", valE) %></td>
                        </tr>
                        <% } %>
                    </tbody>
                    <tfoot>
                        <tr>
                            <th colspan="8" style="text-align: right; font-size: 14px;"><%= Common.getBahasaConfig("Total Akumulasi Transaksi:") %></th>
                            <th class="num" style="font-size: 14px;"><%= String.format("%,.0f", granTotal) %></th>
                        </tr>
                    </tfoot>
                </table>
            </body>
            </html>
            <%
            out.flush(); return; 
        }

    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/bayarmhs/_riwayat_pembayaran_mhs_services.jsp:436");
        if(!"EXPORT_DATA".equals(action) && !"CETAK_STRUK".equals(action)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Terjadi galat teknis di peladen.") + "\"}");
            out.flush();
        }
    } finally {
        if(sess != null) {
            try { sess.clear(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_riwayat_pembayaran_mhs_services.jsp:445");} 
            try { sess.disconnect(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_riwayat_pembayaran_mhs_services.jsp:446");}
            try { sess.close(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_riwayat_pembayaran_mhs_services.jsp:447");}
        }
        try{ HibernateUtil.closeSessionQuietly(sess); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_riwayat_pembayaran_mhs_services.jsp:449");}
    }
%>