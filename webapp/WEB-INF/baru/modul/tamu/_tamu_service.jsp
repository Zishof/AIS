<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="java.util.List"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.json.JSONArray"%>
<%@page import="java.util.Date"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.KunjunganTamu"%>
<%@page import="org.json.JSONObject"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%
try {
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    JSONObject jsonRes = new JSONObject();
    String aksi = request.getParameter("action");
    
    Session dbSession = null;

    try {
        // Menggunakan openSession dengan cleanup di finally untuk mencegah kebocoran memori[cite: 9]
        dbSession = HibernateUtil.getSessionFactory().openSession();
        dbSession.beginTransaction();

        Date hariIni = ais.ui.util.WaktuUtil.getDate();

        if ("guest".equals(aksi)) {
            String namaTamu = request.getParameter("nama");
            String alamatTamu = request.getParameter("alamat");
            String hpTamu = request.getParameter("hp");
            String keperluanTamu = request.getParameter("keperluan");
            String keteranganTamu = request.getParameter("keterangan");
            
            if (namaTamu == null || namaTamu.trim().isEmpty() || 
                alamatTamu == null || alamatTamu.trim().isEmpty() ||
                hpTamu == null || hpTamu.trim().isEmpty() ||
                keperluanTamu == null || keperluanTamu.trim().isEmpty()) {
                
                jsonRes.put("status", "error");
                jsonRes.put("message", Common.getBahasaConfig("Mohon lengkapi semua data wajib (*)"));
            } else {
                // Logika verifikasi duplikasi sesuai KunjunganTamuAction[cite: 9]
                KunjunganTamu entriTamu = (KunjunganTamu) dbSession.createCriteria(KunjunganTamu.class)
                    .add(Restrictions.ilike("nama", namaTamu.trim(), MatchMode.EXACT))
                    .add(Restrictions.ilike("alamat", alamatTamu.trim(), MatchMode.EXACT))
                    .add(Restrictions.eq("tgl", hariIni))
                    .setMaxResults(1)
                    .uniqueResult();
                
                if (entriTamu == null) {
                    entriTamu = new KunjunganTamu();
                    entriTamu.setNama(namaTamu.trim());
                    entriTamu.setAlamat(alamatTamu.trim());
                    entriTamu.setHp(hpTamu.trim());
                    entriTamu.setKeperluan(keperluanTamu.trim());
                    entriTamu.setKeterangan(keteranganTamu != null ? keteranganTamu.trim() : "");
                    entriTamu.setTanggal(new Date());
                    
                    dbSession.save(entriTamu);
                    jsonRes.put("status", "success");
                    jsonRes.put("message", Common.getBahasaConfig("Terima kasih, kunjungan Anda telah tercatat."));
                } else {
                    jsonRes.put("status", "success");
                    jsonRes.put("message", Common.getBahasaConfig("Anda sudah tercatat berkunjung hari ini."));
                }
            }
        } 
        else if ("list".equals(aksi)) {
            int limit = 10;
            int pageIdx = 0;
            try { pageIdx = Integer.parseInt(request.getParameter("page")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/tamu/_tamu_service.jsp:78");}
            
            Long totalData = (Long) dbSession.createCriteria(KunjunganTamu.class)
                .setProjection(Projections.rowCount())
                .uniqueResult();
            
            List<KunjunganTamu> listKunjungan = dbSession.createCriteria(KunjunganTamu.class)
                .addOrder(Order.desc("id"))
                .setFirstResult(pageIdx * limit)
                .setMaxResults(limit)
                .list();
            
            JSONArray dataArray = new JSONArray();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            
            for (KunjunganTamu k : listKunjungan) {
                JSONObject obj = new JSONObject();
                obj.put("nama", k.getNama());
                obj.put("alamat", k.getAlamat());
                obj.put("keperluan", k.getKeperluan());
                obj.put("waktu", k.getTanggal() != null ? sdf.format(k.getTanggal()) : "-");
                dataArray.put(obj);
            }
            
            jsonRes.put("status", "success");
            jsonRes.put("data", dataArray);
            jsonRes.put("total", totalData);
            jsonRes.put("limit", limit);
        }

        dbSession.getTransaction().commit();

    } catch (Exception ex) {
        if (dbSession != null && dbSession.getTransaction().isActive()) {
            dbSession.getTransaction().rollback();
        }
        jsonRes.put("status", "error");
        jsonRes.put("message", "Error: " + ex.getMessage());
    } finally {
        if (dbSession != null) {
            dbSession.clear();
            dbSession.disconnect();
            dbSession.close();
        }
    }

    out.print(jsonRes.toString());
    out.flush();
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/tamu/_tamu_service.jsp:127");
}
%>