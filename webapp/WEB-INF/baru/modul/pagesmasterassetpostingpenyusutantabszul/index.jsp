<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common" %>
<%
    // /pages/master/asset/posting_penyusutan_tabs.zul
    // Tampilan tabs dari Jurnal Penyusutan — dikelola via antarmuka ZK utama.
    // Fungsionalitas tersedia di pagesmasterassetpostingpenyusutanassetzul.
    String title = Common.getBahasaConfig("Jurnal Penyusutan (Tabs)");
    String mainUrl = Common.ROOT + "/baru?p=pagesmasterassetpostingpenyusutanassetzul&s=index";
%>
<div class="card border-0 shadow-sm rounded-4 border-top border-primary border-3 m-3">
    <div class="card-body p-4 text-center">
        <div class="mb-3">
            <i class="fas fa-layer-group fa-3x text-primary opacity-75"></i>
        </div>
        <h5 class="fw-bold text-dark"><%=title%></h5>
        <p class="text-muted small mb-3">
            <%=Common.getBahasaConfig("Tampilan tabs penyusutan aset. Gunakan halaman Jurnal Penyusutan untuk akses penuh.")%>
        </p>
        <a href="<%=mainUrl%>" class="btn btn-primary rounded-pill px-4">
            <i class="fas fa-arrow-right me-2"></i><%=Common.getBahasaConfig("Buka Jurnal Penyusutan")%>
        </a>
    </div>
</div>
