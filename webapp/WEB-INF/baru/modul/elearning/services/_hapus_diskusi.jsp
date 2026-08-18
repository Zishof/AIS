<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.model.PertemuanPunyaDiskusi"%>
<%@page import="ais.common.Common"%>
<%@page import="org.json.JSONObject"%>
<%@page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%
JSONObject responseJson = new JSONObject();

if (Common.getCurrentUser(request) == null) {
    responseJson.put("status", "error");
    responseJson.put("message", Common.getBahasaConfig("Sesi berakhir."));
    out.print(responseJson.toString()); return;
}

String idKomentarParam = request.getParameter("idKomentar");

if (idKomentarParam == null || idKomentarParam.trim().isEmpty()) {
    responseJson.put("status", "error");
    responseJson.put("message", Common.getBahasaConfig("Identitas komentar tidak ditemukan."));
    out.print(responseJson.toString()); return;
}

Session mySession = null;
Transaction tx = null;

try {
    mySession = HibernateUtil.getSessionFactory().openSession();
    tx = mySession.beginTransaction();

    Long idKomentar = Long.parseLong(idKomentarParam.trim());

    // 1. Ambil objek komentar terlebih dahulu untuk mendapatkan relasi ke tabel Pertemuan
    PertemuanPunyaDiskusi targetKomentar = (PertemuanPunyaDiskusi) mySession.get(PertemuanPunyaDiskusi.class, idKomentar);
    Pertemuan pertemuan = null;
    if (targetKomentar != null) {
        pertemuan = targetKomentar.getPertemuan();
    }

    // Hapus seluruh balasan (anak) terlebih dahulu untuk menjaga integritas data (Cascade manual)
    mySession.createSQLQuery("DELETE FROM pertemuan_punya_diskusi WHERE parent IN (SELECT id FROM pertemuan_punya_diskusi WHERE parent = :idParent)")
             .setParameter("idParent", idKomentar)
             .executeUpdate();

    mySession.createSQLQuery("DELETE FROM pertemuan_punya_diskusi WHERE parent = :idParent")
             .setParameter("idParent", idKomentar)
             .executeUpdate();

    // Hapus komentar induk (utama)
    mySession.createSQLQuery("DELETE FROM pertemuan_punya_diskusi WHERE id = :idKomentar")
             .setParameter("idKomentar", idKomentar)
             .executeUpdate();

    tx.commit();

    // 2. Re-init file cache JSON — wrapped in separate try-catch because transaction is already committed;
    // a reInit failure must not trigger rollback on committed data
    if (pertemuan != null) {
        try { pertemuan.reInitPertemuanPunyaDiskusi(mySession); } catch (Exception reInitEx) { ais.common.ErrorAuditUtil.record(reInitEx, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_hapus_diskusi.jsp:62"); /* non-critical cache refresh */ }
    }

    responseJson.put("status", "success");
    responseJson.put("message", Common.getBahasaConfig("Diskusi berhasil dihapus."));

} catch (Exception e) {
    if (tx != null) tx.rollback();
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_hapus_diskusi.jsp:70");
    responseJson.put("status", "error");
    responseJson.put("message", Common.getBahasaConfig("Gagal menghapus data dari sistem."));
} finally {
    ais.common.ElearningSessionUtil.closeQuietly(mySession);
}

out.print(responseJson.toString());
out.flush();
%>