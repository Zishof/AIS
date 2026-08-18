<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.Yayasan"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null) return;

    Yayasan yayasan   = tbmuser.ambilYayasan();
    Sekolah sekolah   = tbmuser.ambilSekolah();

    int jam = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
    String waktu = (jam < 10) ? "Pagi" : (jam < 15) ? "Siang" : (jam < 18) ? "Sore" : "Malam";

    long jmlSiswa = 0, jmlGuru = 0;
    Session sess = null;
    try {
        sess = HibernateUtil.openSession();
        Number n;
        n = (Number) sess.createCriteria(Siswa.class).setProjection(Projections.rowCount()).uniqueResult();
        if (n != null) jmlSiswa = n.longValue();
        n = (Number) sess.createCriteria(Guru.class).setProjection(Projections.rowCount()).uniqueResult();
        if (n != null) jmlGuru = n.longValue();
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/home/admin_sekolah/profile.jsp:30");
        // graceful degradation
    } finally {
        if (sess != null && sess.isOpen()) try { sess.close(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/home/admin_sekolah/profile.jsp:33");}
    }

    String instNama = sekolah != null && sekolah.getNama() != null
        ? sekolah.getNama()
        : (yayasan != null && yayasan.getNama() != null ? yayasan.getNama() : Common.getBahasaConfig("Institusi"));
    String root = Common.ROOT;
%>

<div class="card border-0 shadow-sm rounded-3 mb-3 overflow-hidden animate__animated animate__fadeInRight">
    <div class="card-header border-0 pt-4 pb-3 bg-light"
         style="background-image:url('<%=request.getContextPath()%>/img/corner-4.png');background-position:right top;background-repeat:no-repeat;background-size:contain;">
        <div class="text-muted small fw-bold text-uppercase mb-1" style="letter-spacing:.5px">
            <i class="fas fa-sun me-1 text-warning"></i><%=Common.getBahasaConfig("Selamat")%> <%=Common.getBahasaConfig(waktu)%>
        </div>
        <h5 class="text-success fw-bold mb-0"><%=Common.getBahasaConfig("Admin Sekolah")%></h5>
        <div class="text-muted small mt-1"><%=instNama%></div>
    </div>

    <div class="card-body p-3">
        <div class="row g-2 mb-3">
            <div class="col-6">
                <div class="p-3 border rounded text-center bg-white shadow-sm h-100">
                    <div class="bg-warning bg-opacity-10 p-2 rounded-circle text-warning d-inline-flex mb-2">
                        <i class="fas fa-child"></i>
                    </div>
                    <div class="fw-bold fs-5 text-dark"><%=jmlSiswa%></div>
                    <div class="text-muted" style="font-size:.72rem"><%=Common.getBahasaConfig("Siswa")%></div>
                </div>
            </div>
            <div class="col-6">
                <div class="p-3 border rounded text-center bg-white shadow-sm h-100">
                    <div class="bg-success bg-opacity-10 p-2 rounded-circle text-success d-inline-flex mb-2">
                        <i class="fas fa-chalkboard-teacher"></i>
                    </div>
                    <div class="fw-bold fs-5 text-dark"><%=jmlGuru%></div>
                    <div class="text-muted" style="font-size:.72rem"><%=Common.getBahasaConfig("Guru")%></div>
                </div>
            </div>
        </div>

        <div class="d-flex flex-column gap-2">
            <a href="<%=root%>/baru?p=pagesmastersiswasekolahzul&s=index"
               class="btn btn-outline-warning btn-sm text-start">
                <i class="fas fa-child me-2"></i><%=Common.getBahasaConfig("Data Siswa")%>
            </a>
            <a href="<%=root%>/baru?p=pagesmasterjadwalpelajaranzul&s=index"
               class="btn btn-outline-success btn-sm text-start">
                <i class="fas fa-calendar-alt me-2"></i><%=Common.getBahasaConfig("Jadwal Pelajaran")%>
            </a>
            <a href="<%=root%>/baru?p=pagesmasterabsensikelassekolahzul&s=index"
               class="btn btn-outline-info btn-sm text-start">
                <i class="fas fa-fingerprint me-2"></i><%=Common.getBahasaConfig("Absensi Kelas")%>
            </a>
            <a href="<%=root%>/baru?p=pagesmasterpenilaiansiswasekolahzul&s=index"
               class="btn btn-outline-secondary btn-sm text-start">
                <i class="far fa-check-circle me-2"></i><%=Common.getBahasaConfig("Penilaian Siswa")%>
            </a>
        </div>
    </div>
</div>
