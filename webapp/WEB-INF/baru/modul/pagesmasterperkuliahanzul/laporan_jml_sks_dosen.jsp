<%@page import="ais.action.report.format1.akademik.LaporanPerkuliahanJspHelper"%>
<%@page import="ais.action.report.format1.akademik.LaporanPerkuliahanJspHelper.Param"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    Param p = LaporanPerkuliahanJspHelper.parseParam(request);
    String hasilUrl = Common.ROOT + "/baru?p=pagesmasterperkuliahanzul&s=laporan_jml_sks_dosen_hasil&hanya_tampil_jsp=true";
%>
<div class="mb-3">
    <button onclick="history.back()" class="btn btn-sm btn-outline-secondary mb-2">
        <i class="fas fa-arrow-left me-1"></i>Kembali
    </button>
    <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="<%=Common.ROOT%>/baru?p=pagesmasterperkuliahanzul&s=index">Perkuliahan</a></li>
            <li class="breadcrumb-item active">Jumlah SKS Dosen</li>
        </ol>
    </nav>
    <h5 class="fw-bold text-primary"><i class="fas fa-weight me-2"></i>Jumlah SKS Per Dosen</h5>
    <p class="text-muted small">Total SKS yang diampu tiap dosen pengampu.</p>
</div>
<%=LaporanPerkuliahanJspHelper.buildFilterFormAjax(p, hasilUrl, "laporan-jml-sks-dosen", "hasil-jml-sks-dosen")%>
<div id="hasil-jml-sks-dosen">
    <div class="alert alert-light border shadow-sm">
        <i class="fas fa-arrow-up me-2 text-muted"></i>Isi filter lalu klik <strong>Tampilkan</strong>.
    </div>
</div>
