<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="java.util.List"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.json.JSONArray"%>
<%@page import="java.util.Date"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.library.Anggota"%>
<%@page import="ais.database.model.library.KunjunganAnggota"%>
<%@page import="ais.database.model.library.Perpustakaan"%>
<%@page import="ais.action.master.library.util.LibraryUtil"%>
<%@page import="org.json.JSONObject"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%
try {
    // Mengamankan halaman agar tidak di-_cache_ oleh peramban
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    JSONObject jsonRes = new JSONObject();
    String aksi = request.getParameter("action");
    
    Session dbSession = null;

    try {
        dbSession = HibernateUtil.openSession();
        dbSession.beginTransaction();

        // Mengambil record perpustakaan aktif yang akan menjadi lokasi kunjungan
        Perpustakaan lokasiPerpus = (Perpustakaan) dbSession.createCriteria(Perpustakaan.class)
            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
            .addOrder(Order.asc("id"))
            .setMaxResults(1)
            .uniqueResult();
        
        if (lokasiPerpus == null) {
            jsonRes.put("status", "error");
            jsonRes.put("message", Common.getBahasaConfig("Lokasi perpustakaan belum terkonfigurasi pada sistem."));
            out.print(jsonRes.toString());
            return;
        }

        Date hariIni = ais.ui.util.WaktuUtil.getDate();

        // -------------------------------------------------------------
        // LOGIKA 1: Pemrosesan Anggota (Scan / Input Teks)
        // -------------------------------------------------------------
        if ("scan".equals(aksi)) {
            String idAnggota = request.getParameter("kode");
            
            if (idAnggota == null || idAnggota.trim().isEmpty()) {
                jsonRes.put("status", "error");
                jsonRes.put("message", Common.getBahasaConfig("Silakan masukkan kode identitas yang sah."));
            } else {
                // Pencarian data anggota yang terdaftar di basis data
                Anggota profilAnggota = LibraryUtil.cariAnggotaDariIdentitas(dbSession, idAnggota);

                if (profilAnggota == null) {
                    jsonRes.put("status", "error");
                    jsonRes.put("message", Common.getBahasaConfig("Identitas tidak ditemukan dalam daftar keanggotaan."));
                } else {
                    // Mencegah duplikasi entri kunjungan pada hari yang sama
                    KunjunganAnggota entriKunjungan = (KunjunganAnggota) dbSession.createCriteria(KunjunganAnggota.class)
                        .add(Restrictions.eq("anggota", profilAnggota))
                        .add(Restrictions.eq("perpustakaan", lokasiPerpus))
                        .add(Restrictions.eq("tgl", hariIni))
                        .setMaxResults(1)
                        .uniqueResult();

                    if (entriKunjungan == null) {
                        entriKunjungan = new KunjunganAnggota();
                        entriKunjungan.setPerpustakaan(lokasiPerpus);
                        entriKunjungan.setAnggota(profilAnggota);
                        entriKunjungan.setTanggal(hariIni);
                        entriKunjungan.setTgl(hariIni);
                        
                        dbSession.save(entriKunjungan);
                        
                        jsonRes.put("status", "success");
                        jsonRes.put("message", Common.getBahasaConfig("Selamat membaca, ") + profilAnggota.getNama() + "!");
                    } else {
                        jsonRes.put("status", "success");
                        jsonRes.put("message", Common.getBahasaConfig("Selamat datang kembali hari ini, ") + profilAnggota.getNama() + ".");
                    }
                }
            }
        } 
        
        // -------------------------------------------------------------
        // LOGIKA 2: Pemrosesan Pengunjung Non-Anggota
        // -------------------------------------------------------------
        else if ("guest".equals(aksi)) {
            String namaTamu = request.getParameter("nama");
            String alamatTamu = request.getParameter("alamat");
            String infoTambahan = request.getParameter("keterangan");
            
            if (namaTamu == null || namaTamu.trim().isEmpty() || alamatTamu == null || alamatTamu.trim().isEmpty()) {
                jsonRes.put("status", "error");
                jsonRes.put("message", Common.getBahasaConfig("Data form tidak lengkap."));
            } else {
                // Memverifikasi apakah tamu tersebut sudah mendaftar hari ini
                KunjunganAnggota entriTamu = (KunjunganAnggota) dbSession.createCriteria(KunjunganAnggota.class)
                    .add(Restrictions.isNull("anggota"))
                    .add(Restrictions.eq("nama", namaTamu.trim()))
                    .add(Restrictions.eq("alamat", alamatTamu.trim()))
                    .add(Restrictions.eq("perpustakaan", lokasiPerpus))
                    .add(Restrictions.eq("tgl", hariIni))
                    .setMaxResults(1)
                    .uniqueResult();
                
                if (entriTamu == null) {
                    entriTamu = new KunjunganAnggota();
                    entriTamu.setPerpustakaan(lokasiPerpus);
                    entriTamu.setAnggota(null); 
                    entriTamu.setNama(namaTamu.trim());
                    entriTamu.setAlamat(alamatTamu.trim());
                    entriTamu.setKeterangan(infoTambahan != null ? infoTambahan.trim() : "");
                    entriTamu.setTanggal(hariIni);
                    entriTamu.setTgl(hariIni);
                    
                    dbSession.save(entriTamu);
                    
                    jsonRes.put("status", "success");
                    jsonRes.put("message", Common.getBahasaConfig("Kunjungan tercatat. Terima kasih, ") + namaTamu.trim());
                } else {
                    jsonRes.put("status", "success");
                    jsonRes.put("message", Common.getBahasaConfig("Anda telah tercatat mengunjungi kami hari ini."));
                }
            }
        } // -------------------------------------------------------------
        // LOGIKA 3: Pengambilan Daftar Kunjungan Terakhir (Paging)
        // -------------------------------------------------------------
        else if ("list".equals(aksi)) {
            int limit = 10;
            int pageIdx = 0;
            try { pageIdx = Integer.parseInt(request.getParameter("page")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pustaka/_welpus_service.jsp:137");}
            
            // Hitung Total Data untuk Paging
            Long totalData = (Long) dbSession.createCriteria(KunjunganAnggota.class)
                .add(Restrictions.eq("perpustakaan", lokasiPerpus))
                .add(Restrictions.eq("tgl", hariIni))
                .setProjection(Projections.rowCount())
                .uniqueResult();
            
            // Ambil Data Kunjungan
            List<KunjunganAnggota> listKunjungan = dbSession.createCriteria(KunjunganAnggota.class)
                .add(Restrictions.eq("perpustakaan", lokasiPerpus))
                .add(Restrictions.eq("tgl", hariIni))
                .addOrder(Order.desc("id"))
                .setFirstResult(pageIdx * limit)
                .setMaxResults(limit)
                .list();
            
            JSONArray dataArray = new JSONArray();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            
            for (KunjunganAnggota k : listKunjungan) {
                JSONObject obj = new JSONObject();
                // Cek apakah Anggota atau Tamu Umum
                if (k.getAnggota() != null) {
                    obj.put("nama", k.getAnggota().getNama());
                    obj.put("status", "Anggota");
                } else {
                    obj.put("nama", k.getNama());
                    obj.put("status", "Tamu");
                }
                obj.put("waktu", sdf.format(k.getTanggal()));
                dataArray.put(obj);
            }
            
            jsonRes.put("status", "success");
            jsonRes.put("data", dataArray);
            jsonRes.put("total", totalData);
            jsonRes.put("limit", limit);
        }else {
            jsonRes.put("status", "error");
            jsonRes.put("message", "Perintah tidak dikenali oleh peladen.");
        }

        dbSession.getTransaction().commit();

    } catch (Exception ex) {
        if (dbSession != null && dbSession.getTransaction().isActive()) {
            dbSession.getTransaction().rollback();
        }
        jsonRes.put("status", "error");
        jsonRes.put("message", "Kesalahan sistem internal: " + ex.getMessage());
    } finally {
        // Session dibuka via openSession(), wajib ditutup di sini agar tidak bocor
        HibernateUtil.closeSessionQuietly(dbSession);
    }

    out.print(jsonRes.toString());
    out.flush();
} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pustaka/_welpus_service.jsp:195");
    System.err.println("Kesalahan pengambilan data institusi: " + e.getMessage());
}
%>
