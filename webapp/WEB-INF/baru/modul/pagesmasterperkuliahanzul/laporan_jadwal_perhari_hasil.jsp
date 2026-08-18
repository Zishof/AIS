<%@page import="ais.action.report.format1.akademik.LaporanPerkuliahanJspHelper"%>
<%@page import="ais.action.report.format1.akademik.LaporanPerkuliahanJspHelper.Param"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    Param p = LaporanPerkuliahanJspHelper.parseParam(request);
    if (p.isEmpty()) {
%>
<div class="alert alert-info border-0 shadow-sm">
    <i class="fas fa-filter me-2"></i>Isi filter terlebih dahulu lalu klik <strong>Tampilkan</strong>.
</div>
<%
        return;
    }
    Session sess = null;
    try {
        sess = HibernateUtil.openSession();
        out.print(LaporanPerkuliahanJspHelper.buildRingkasan(sess, p));
        out.print(LaporanPerkuliahanJspHelper.buildTabelJadwalPerHari(sess, p));
    } catch (Exception ex) {
        out.print("<div class='alert alert-danger'><i class='fas fa-exclamation-triangle me-2'></i>"
                + LaporanPerkuliahanJspHelper.esc(ex.getMessage()) + "</div>");
    } finally {
        if (sess != null && sess.isOpen()) try { sess.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmasterperkuliahanzul/laporan_jadwal_perhari_hasil.jsp:25");}
    }
%>
