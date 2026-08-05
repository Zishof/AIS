<%@page import="ais.action.report.format1.akademik.LaporanPerkuliahanJspHelper"%>
<%@page import="ais.action.report.format1.akademik.LaporanPerkuliahanJspHelper.Param"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    Param p = LaporanPerkuliahanJspHelper.parseParam(request);
    String hasilUrl = Common.ROOT + "/baru?p=pagesmasterperkuliahanzul&s=laporan_rekap_dosen_hasil&hanya_tampil_jsp=true";
%>
<div class="mb-3">
    <button onclick="history.back()" class="btn btn-sm btn-outline-secondary mb-2">
        <i class="fas fa-arrow-left me-1"></i>Kembali
    </button>
    <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="<%=Common.ROOT%>/baru?p=pagesmasterperkuliahanzul&s=index">Perkuliahan</a></li>
            <li class="breadcrumb-item active">Rekap Dosen</li>
        </ol>
    </nav>
    <h5 class="fw-bold text-primary"><i class="fas fa-chart-bar me-2"></i>Rekap Beban Mengajar Dosen</h5>
    <p class="text-muted small">Rekap total kelas, SKS, dan hari mengajar per dosen.</p>
</div>
<%=LaporanPerkuliahanJspHelper.buildFilterFormAjax(p, hasilUrl, "laporan-rekap-dosen", "hasil-rekap-dosen")%>
<div id="hasil-rekap-dosen">
    <div class="alert alert-light border shadow-sm">
        <i class="fas fa-arrow-up me-2 text-muted"></i>Isi filter lalu klik <strong>Tampilkan</strong>.
    </div>
</div>
