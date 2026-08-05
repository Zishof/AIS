<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common" %>
<%@ page import="ais.database.hibernate.HibernateUtil" %>
<%@ page import="org.hibernate.Session" %>
<%@ page import="org.hibernate.FetchMode" %>
<%@ page import="org.hibernate.criterion.Restrictions" %>
<%@ page import="org.hibernate.criterion.Projections" %>
<%@ page import="org.hibernate.criterion.Order" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="org.json.JSONArray" %>
<%@ page import="ais.database.model.Tbmuser" %>
<%@ page import="ais.database.model.library.Anggota" %>
<%@ page import="ais.database.model.library.PeminjamanPengadaanItem" %>
<%@ page import="ais.database.model.library.KembaliPengadaanItem" %>
<%@ page import="ais.database.model.library.PesananAnggota" %>
<%@ page import="ais.database.model.library.KunjunganAnggota" %>

<%
    JSONObject jsonResponse = new JSONObject();

    // =========================================================
    // 1. VALIDASI OTORISASI PENGGUNA
    // =========================================================
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null) {
        jsonResponse.put("status", "error");
        jsonResponse.put("message", Common.getBahasaConfig("Anda harus masuk (login) sebagai anggota perpustakaan untuk mengakses layanan ini."));
        out.print(jsonResponse.toString());
        return;
    }

    Anggota currentAnggota = Anggota.buatAtauAmbilAnggota(tbmuser, true);
    if (currentAnggota == null || currentAnggota.getId() == null) {
        jsonResponse.put("status", "error");
        jsonResponse.put("message", Common.getBahasaConfig("Data keanggotaan perpustakaan Anda tidak ditemukan di dalam sistem."));
        out.print(jsonResponse.toString());
        return;
    }

    Long anggotaId = currentAnggota.getId();
    String action = request.getParameter("action");

    // =========================================================
    // 2. TANGKAP PARAMETER FILTER PENCARIAN & PAGINASI
    // =========================================================
    String keyword = request.getParameter("keyword");
    String startDateStr = request.getParameter("start_date");
    String endDateStr = request.getParameter("end_date");

    Date startDate = null;
    Date endDate = null;

    // Mengurai Tanggal dengan aman
    try {
        if (startDateStr != null && !startDateStr.trim().isEmpty()) {
            startDate = ais.ui.util.WaktuUtil.parseDate(startDateStr + " 00:00:00", "yyyy-MM-dd HH:mm:ss");
        }
        if (endDateStr != null && !endDateStr.trim().isEmpty()) {
            endDate = ais.ui.util.WaktuUtil.parseDate(endDateStr + " 23:59:59", "yyyy-MM-dd HH:mm:ss");
        }
    } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pustaka/_beranda_anggota_service.jsp:64");}

    // Pengaturan Nilai Bawaan (Default) 5 Tahun Terakhir jika null
    if (startDate == null || endDate == null) {
        Calendar cal = Calendar.getInstance();
        if (endDate == null) {
            endDate = cal.getTime();
        }
        if (startDate == null) {
            cal.add(Calendar.YEAR, -5);
            startDate = cal.getTime();
        }
    }

    // Paginasi
    int pageNum = 1;
    int limit = 10;
    try {
        if (request.getParameter("page") != null) pageNum = Integer.parseInt(request.getParameter("page"));
        if (request.getParameter("limit") != null) limit = Integer.parseInt(request.getParameter("limit"));
    } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pustaka/_beranda_anggota_service.jsp:84");}
    
    int offset = (pageNum - 1) * limit;
    Session dbSession = null;

    try {
        dbSession = HibernateUtil.openSession();

        // =========================================================
        // 3A. RINGKASAN DATA (METRIK DASHBOARD)
        // =========================================================
        if ("summary".equals(action)) {
            Number countPinjam = (Number) dbSession.createCriteria(PeminjamanPengadaanItem.class).add(Restrictions.eq("anggota.id", anggotaId)).setProjection(Projections.rowCount()).uniqueResult();
            Number countKembali = (Number) dbSession.createCriteria(KembaliPengadaanItem.class).createAlias("peminjamanPengadaanItem", "pinjam").add(Restrictions.eq("pinjam.anggota.id", anggotaId)).setProjection(Projections.rowCount()).uniqueResult();
            Number countPesan = (Number) dbSession.createCriteria(PesananAnggota.class).add(Restrictions.eq("anggota.id", anggotaId)).setProjection(Projections.rowCount()).uniqueResult();
            Number countKunjung = (Number) dbSession.createCriteria(KunjunganAnggota.class).add(Restrictions.eq("anggota.id", anggotaId)).setProjection(Projections.rowCount()).uniqueResult();

            JSONObject summary = new JSONObject();
            summary.put("pinjam", countPinjam != null ? countPinjam.intValue() : 0);
            summary.put("kembali", countKembali != null ? countKembali.intValue() : 0);
            summary.put("pesan", countPesan != null ? countPesan.intValue() : 0);
            summary.put("kunjung", countKunjung != null ? countKunjung.intValue() : 0);

            jsonResponse.put("status", "success");
            jsonResponse.put("data", summary);
        } 
        
        // =========================================================
        // 3B. RIWAYAT PEMINJAMAN (PAGINASI + FILTER DINAMIS)
        // =========================================================
        else if ("history_pinjam".equals(action)) {
            org.hibernate.Criteria cCount = dbSession.createCriteria(PeminjamanPengadaanItem.class).add(Restrictions.eq("anggota.id", anggotaId));
            org.hibernate.Criteria cData = dbSession.createCriteria(PeminjamanPengadaanItem.class).add(Restrictions.eq("anggota.id", anggotaId)).setFetchMode("kembaliPengadaanItem", FetchMode.JOIN);

            // Filter Rentang Tanggal
            cCount.add(Restrictions.ge("tanggalPembuatan", startDate)).add(Restrictions.le("tanggalPembuatan", endDate));
            cData.add(Restrictions.ge("tanggalPembuatan", startDate)).add(Restrictions.le("tanggalPembuatan", endDate));

            // Filter Kata Kunci (Mencari di Keterangan, Kode, atau Judul & ISBN Buku melalui Sub-Query agar tidak N+1)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kwSafe = keyword.replace("'", "''"); // Anti SQL-Injection Dasar
                String sqlFilter = "({alias}.keterangan ILIKE '%" + kwSafe + "%' OR {alias}.kode ILIKE '%" + kwSafe + "%' OR {alias}.id IN (SELECT pid.peminjaman_pengadaan_item FROM library.peminjaman_pengadaan_item_detail pid INNER JOIN library.item i ON pid.item = i.id WHERE i.nama ILIKE '%" + kwSafe + "%' OR i.isbn ILIKE '%" + kwSafe + "%'))";
                
                cCount.add(Restrictions.sqlRestriction(sqlFilter));
                cData.add(Restrictions.sqlRestriction(sqlFilter));
            }

            Number totalData = (Number) cCount.setProjection(Projections.rowCount()).uniqueResult();
            List<PeminjamanPengadaanItem> listPinjam = cData.addOrder(Order.desc("tanggalPembuatan")).setFirstResult(offset).setMaxResults(limit).list();
            
            JSONArray arr = new JSONArray();
            for (PeminjamanPengadaanItem p : listPinjam) {
                JSONObject obj = new JSONObject();
                obj.put("kode", p.getKode());
                obj.put("keterangan", p.getKeterangan() != null ? p.getKeterangan() : "-");
                obj.put("tanggal_pinjam", p.getTanggalPembuatan() != null ? ais.ui.util.WaktuUtil.formatDate(p.getTanggalPembuatan(), "dd-MM-yyyy HH:mm") : "-");
                
                boolean isKembali = p.getKembaliPengadaanItem() != null;
                obj.put("status", isKembali ? Common.getBahasaConfig("Telah Dikembalikan") : Common.getBahasaConfig("Sedang Dipinjam"));
                obj.put("tanggal_kembali", isKembali && p.getKembaliPengadaanItem().getTanggalPembuatan() != null ? ais.ui.util.WaktuUtil.formatDate(p.getKembaliPengadaanItem().getTanggalPembuatan(), "dd-MM-yyyy HH:mm") : "-");
                arr.put(obj);
            }
            
            jsonResponse.put("status", "success");
            jsonResponse.put("total_data", totalData != null ? totalData.intValue() : 0);
            jsonResponse.put("data", arr);
        }

        // =========================================================
        // 3C. RIWAYAT PEMESANAN (PAGINASI + FILTER DINAMIS)
        // =========================================================
        else if ("history_pesan".equals(action)) {
            org.hibernate.Criteria cCount = dbSession.createCriteria(PesananAnggota.class).add(Restrictions.eq("anggota.id", anggotaId));
            org.hibernate.Criteria cData = dbSession.createCriteria(PesananAnggota.class).add(Restrictions.eq("anggota.id", anggotaId));

            // Filter Rentang Tanggal
            cCount.add(Restrictions.ge("tanggal", startDate)).add(Restrictions.le("tanggal", endDate));
            cData.add(Restrictions.ge("tanggal", startDate)).add(Restrictions.le("tanggal", endDate));

            // Filter Kata Kunci
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = "%" + keyword.trim() + "%";
                cCount.createAlias("item", "itm");
                cData.createAlias("item", "itm");
                
             // Menggunakan disjunction() untuk menampung lebih dari dua kondisi OR (ATAU)
                org.hibernate.criterion.Disjunction searchCrit = Restrictions.disjunction();
                searchCrit.add(Restrictions.ilike("kode", kw));
                searchCrit.add(Restrictions.ilike("itm.nama", kw)); // Judul Buku
                searchCrit.add(Restrictions.ilike("itm.isbn", kw)); // ISBN Buku

                cCount.add(searchCrit);
                cData.add(searchCrit);
            } else {
                cData.setFetchMode("item", FetchMode.JOIN); // Efisiensi jika tidak di-alias
            }

            Number totalData = (Number) cCount.setProjection(Projections.rowCount()).uniqueResult();
            List<PesananAnggota> listPesan = cData.addOrder(Order.desc("tanggal")).setFirstResult(offset).setMaxResults(limit).list();
            
            JSONArray arr = new JSONArray();
            for (PesananAnggota p : listPesan) {
                JSONObject obj = new JSONObject();
                obj.put("kode", p.getKode());
                obj.put("tanggal", p.getTanggal() != null ? ais.ui.util.WaktuUtil.formatDate(p.getTanggal(), "dd-MM-yyyy HH:mm") : "-");
                obj.put("buku", p.getItem() != null ? p.getItem().getNama() : "-");
                obj.put("status", p.getStatus() != null ? p.getStatus() : "-");
                arr.put(obj);
            }
            
            jsonResponse.put("status", "success");
            jsonResponse.put("total_data", totalData != null ? totalData.intValue() : 0);
            jsonResponse.put("data", arr);
        }

        // =========================================================
        // 3D. RIWAYAT KUNJUNGAN (PAGINASI + FILTER DINAMIS)
        // =========================================================
        else if ("history_kunjung".equals(action)) {
            org.hibernate.Criteria cCount = dbSession.createCriteria(KunjunganAnggota.class).add(Restrictions.eq("anggota.id", anggotaId));
            org.hibernate.Criteria cData = dbSession.createCriteria(KunjunganAnggota.class).add(Restrictions.eq("anggota.id", anggotaId));

            // Filter Rentang Tanggal
            cCount.add(Restrictions.ge("tanggal", startDate)).add(Restrictions.le("tanggal", endDate));
            cData.add(Restrictions.ge("tanggal", startDate)).add(Restrictions.le("tanggal", endDate));

            // Filter Kata Kunci
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = "%" + keyword.trim() + "%";
                cCount.createAlias("perpustakaan", "prp");
                cData.createAlias("perpustakaan", "prp");
                
                org.hibernate.criterion.Criterion searchCrit = Restrictions.or(
                    Restrictions.ilike("keterangan", kw),
                    Restrictions.ilike("prp.nama", kw) // Nama Perpustakaan
                );
                cCount.add(searchCrit);
                cData.add(searchCrit);
            } else {
                cData.setFetchMode("perpustakaan", FetchMode.JOIN); // Efisiensi jika tidak di-alias
            }

            Number totalData = (Number) cCount.setProjection(Projections.rowCount()).uniqueResult();
            List<KunjunganAnggota> listKunjung = cData.addOrder(Order.desc("tanggal")).setFirstResult(offset).setMaxResults(limit).list();
            
            JSONArray arr = new JSONArray();
            for (KunjunganAnggota k : listKunjung) {
                JSONObject obj = new JSONObject();
                obj.put("tanggal", k.getTanggal() != null ? ais.ui.util.WaktuUtil.formatDate(k.getTanggal(), "dd-MM-yyyy HH:mm") : "-");
                obj.put("perpustakaan", k.getPerpustakaan() != null ? k.getPerpustakaan().getNama() : "-");
                obj.put("keterangan", k.getKeterangan() != null ? k.getKeterangan() : "-");
                arr.put(obj);
            }
            
            jsonResponse.put("status", "success");
            jsonResponse.put("total_data", totalData != null ? totalData.intValue() : 0);
            jsonResponse.put("data", arr);
        }

    } catch (Exception e) {
        jsonResponse.put("status", "error");
        jsonResponse.put("message", Common.getBahasaConfig("Terjadi kesalahan sistem internal: ") + e.getMessage());
    } finally {
        // PERBAIKAN: Penutupan Session secara aman
        try {
            if (dbSession != null && dbSession.isOpen()) {
                dbSession.disconnect();
                dbSession.close();
            }
        } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pustaka/_beranda_anggota_service.jsp:253");}
        HibernateUtil.closeSessionQuietly(dbSession);
    }

    out.print(jsonResponse.toString());
    out.flush();
%>