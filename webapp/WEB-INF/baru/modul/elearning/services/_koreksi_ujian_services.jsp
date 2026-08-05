<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.*"%>
<%@page import="org.json.JSONObject"%>
<%@page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%
JSONObject resp = new JSONObject();
Session mySession = null;
Transaction tx = null;

try {
    String idDetailParam = request.getParameter("idDetail");
    String koreksiText = request.getParameter("koreksi");
    String nilaiParam = request.getParameter("nilai");
    
    if (idDetailParam == null || idDetailParam.trim().isEmpty()) throw new Exception("ID Detail tidak valid.");

    mySession = HibernateUtil.getSessionFactory().openSession();
    tx = mySession.beginTransaction();
    
    HasilUjianMahasiswaDetail humd = (HasilUjianMahasiswaDetail) mySession.get(HasilUjianMahasiswaDetail.class, Long.parseLong(idDetailParam));
    
    if (humd != null) {
        // Update Teks Koreksi (Bisa untuk PG & Esai)
        if (koreksiText != null) {
            humd.setKoreksi(koreksiText.trim());
        }
        
        // Update Skor (Hanya untuk Esai / Non-PG yang mengirim parameter 'nilai')
        if (nilaiParam != null && !nilaiParam.trim().isEmpty()) {
            Double nilaiUpdate = Double.parseDouble(nilaiParam);
            Double maxSkor = humd.getBankSoal().getSkor();
            
            // Pengamanan backend
            if (nilaiUpdate > maxSkor) nilaiUpdate = maxSkor;
            if (nilaiUpdate < 0) nilaiUpdate = 0.0;
            
            humd.setNilai(nilaiUpdate);
        }
        
        mySession.update(humd);
        tx.commit(); tx = null;

        resp.put("status", "success");
    } else {
        resp.put("status", "error");
        resp.put("message", "Data jawaban tidak ditemukan.");
    }
} catch (Exception e) {
    if (tx != null) tx.rollback();
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_koreksi_ujian_services.jsp:53");
    resp.put("status", "error");
    resp.put("message", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
} finally {
    ais.common.ElearningSessionUtil.closeQuietly(mySession);
}

out.print(resp.toString());
out.flush();
%>