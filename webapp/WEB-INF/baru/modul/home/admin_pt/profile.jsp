<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null) return;

    PerguruanTinggi pt = tbmuser.getPerguruanTinggi();

    int jam = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
    String waktu = (jam < 10) ? "Pagi" : (jam < 15) ? "Siang" : (jam < 18) ? "Sore" : "Malam";

    long jmlMahasiswa = 0, jmlDosen = 0, jmlProdi = 0, jmlFakultas = 0;
    Session sess = null;
    try {
        sess = HibernateUtil.openSession();
        Number n;
        n = (Number) sess.createCriteria(Mahasiswa.class).setProjection(Projections.rowCount()).uniqueResult();
        if (n != null) jmlMahasiswa = n.longValue();
        n = (Number) sess.createCriteria(Dosen.class).setProjection(Projections.rowCount()).uniqueResult();
        if (n != null) jmlDosen = n.longValue();
        n = (Number) sess.createCriteria(Jurusan.class).setProjection(Projections.rowCount()).uniqueResult();
        if (n != null) jmlProdi = n.longValue();
        n = (Number) sess.createCriteria(Fakultas.class).setProjection(Projections.rowCount()).uniqueResult();
        if (n != null) jmlFakultas = n.longValue();
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/home/admin_pt/profile.jsp:34");
        // graceful degradation — tampilkan 0
    } finally {
        if (sess != null && sess.isOpen()) try { sess.close(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/home/admin_pt/profile.jsp:37");}
    }

    String ptNama = pt != null && pt.getNama() != null ? pt.getNama() : Common.getBahasaConfig("Perguruan Tinggi");
    String root = Common.ROOT;
%>

<div class="card border-0 shadow-sm rounded-3 mb-3 overflow-hidden animate__animated animate__fadeInRight">
    <div class="card-header border-0 pt-4 pb-3 bg-light"
         style="background-image:url('<%=request.getContextPath()%>/img/corner-2.png');background-position:right top;background-repeat:no-repeat;background-size:contain;">
        <div class="text-muted small fw-bold text-uppercase mb-1" style="letter-spacing:.5px">
            <i class="fas fa-sun me-1 text-warning"></i><%=Common.getBahasaConfig("Selamat")%> <%=Common.getBahasaConfig(waktu)%>
        </div>
        <h5 class="text-primary fw-bold mb-0"><%=Common.getBahasaConfig("Administrator")%></h5>
        <div class="text-muted small mt-1"><%=ptNama%></div>
    </div>

    <div class="card-body p-3">
        <div class="row g-2 mb-3">
            <div class="col-6">
                <div class="p-3 border rounded text-center bg-white shadow-sm h-100">
                    <div class="bg-primary bg-opacity-10 p-2 rounded-circle text-primary d-inline-flex mb-2">
                        <i class="fas fa-user-graduate"></i>
                    </div>
                    <div class="fw-bold fs-5 text-dark"><%=jmlMahasiswa%></div>
                    <div class="text-muted" style="font-size:.72rem"><%=Common.getBahasaConfig("Mahasiswa")%></div>
                </div>
            </div>
            <div class="col-6">
                <div class="p-3 border rounded text-center bg-white shadow-sm h-100">
                    <div class="bg-success bg-opacity-10 p-2 rounded-circle text-success d-inline-flex mb-2">
                        <i class="fas fa-chalkboard-teacher"></i>
                    </div>
                    <div class="fw-bold fs-5 text-dark"><%=jmlDosen%></div>
                    <div class="text-muted" style="font-size:.72rem"><%=Common.getBahasaConfig("Dosen")%></div>
                </div>
            </div>
            <div class="col-6">
                <div class="p-3 border rounded text-center bg-white shadow-sm h-100">
                    <div class="bg-warning bg-opacity-10 p-2 rounded-circle text-warning d-inline-flex mb-2">
                        <i class="fas fa-layer-group"></i>
                    </div>
                    <div class="fw-bold fs-5 text-dark"><%=jmlProdi%></div>
                    <div class="text-muted" style="font-size:.72rem"><%=Common.getBahasaConfig("Program Studi")%></div>
                </div>
            </div>
            <div class="col-6">
                <div class="p-3 border rounded text-center bg-white shadow-sm h-100">
                    <div class="bg-info bg-opacity-10 p-2 rounded-circle text-info d-inline-flex mb-2">
                        <i class="fas fa-building"></i>
                    </div>
                    <div class="fw-bold fs-5 text-dark"><%=jmlFakultas%></div>
                    <div class="text-muted" style="font-size:.72rem"><%=Common.getBahasaConfig("Fakultas")%></div>
                </div>
            </div>
        </div>

        <div class="d-flex flex-column gap-2">
            <a href="<%=root%>/baru?p=pagesmastermahasiswazul&s=index"
               class="btn btn-outline-primary btn-sm text-start">
                <i class="fas fa-user-graduate me-2"></i><%=Common.getBahasaConfig("Data Mahasiswa")%>
            </a>
            <a href="<%=root%>/baru?p=pagesmasterperkuliahanzul&s=index"
               class="btn btn-outline-success btn-sm text-start">
                <i class="fas fa-chalkboard me-2"></i><%=Common.getBahasaConfig("Perkuliahan")%>
            </a>
            <a href="<%=root%>/baru?p=pagesmasterdosenzul&s=index"
               class="btn btn-outline-info btn-sm text-start">
                <i class="fas fa-id-badge me-2"></i><%=Common.getBahasaConfig("Data Dosen")%>
            </a>
            <a href="<%=root%>/baru?p=pagesmasterjurusanzul&s=index"
               class="btn btn-outline-secondary btn-sm text-start">
                <i class="fas fa-sitemap me-2"></i><%=Common.getBahasaConfig("Program Studi")%>
            </a>
        </div>
    </div>
</div>
