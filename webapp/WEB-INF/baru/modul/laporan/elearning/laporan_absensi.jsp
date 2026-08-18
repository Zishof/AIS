<%@page import="org.zkoss.zul.Toolbar"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.*, java.io.*"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.action.report.*"%>

<%
    // 1. Ambil Data Pertemuan
    String idPertemuan = request.getParameter("pertemuan");
    Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, idPertemuan, true);

    // 2. Siapkan Parameter Laporan
    List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
    Map parameters = ais.common.HashMapGenerator.getRand();
    
    // Generate data map
    CommonReportHelper.mapLaporanBeritaAcara(pertemuan, null, maps, parameters, null);
    parameters.put("maps", maps);

    // 3. Generate File PDF
    File file = Report.generateFileReport(Report.PDF, parameters, "BeritaAcaraPerkuliahan", ais.ui.util.WaktuUtil.getDate(), new Toolbar());
    String pdfUrl = Common.CURRENT_URL + "/report/" + file.getName();
%>

<div class="card shadow-sm mb-4">
    <div class="card-header bg-white py-3 border-bottom">
        <h5 class="mb-0 text-primary">
            <i class="fas fa-file-alt me-2"></i><%=Common.getBahasaConfig("Laporan Kehadiran")%>
        </h5>
    </div>

    <div class="card-body">
        <jsp:include page="/WEB-INF/baru/componen/pdf2.jsp">
            <jsp:param name="pdf_url" value="<%=pdfUrl%>" />
        </jsp:include>
    </div>
</div>