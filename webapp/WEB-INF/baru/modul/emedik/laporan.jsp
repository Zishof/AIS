<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
String jenisLaporan = "kunjungan".equals(request.getParameter("jenis")) ? "kunjungan" : "tagihan";
String baseLaporan = request.getContextPath() + "/baru?p=emedik&s=laporan&jenis=";
%>
<div class="mb-3"><h4>Laporan eMedik</h4><p class="text-muted">Monitoring pendaftaran, tagihan, dan pembayaran layanan medis.</p></div>
<ul class="nav nav-tabs mb-3">
  <li class="nav-item"><a class="nav-link <%= "tagihan".equals(jenisLaporan) ? "active" : "" %>" href="<%=baseLaporan%>tagihan">Status Pembayaran</a></li>
  <li class="nav-item"><a class="nav-link <%= "kunjungan".equals(jenisLaporan) ? "active" : "" %>" href="<%=baseLaporan%>kunjungan">Kunjungan Pasien</a></li>
</ul>
<% if ("kunjungan".equals(jenisLaporan)) { %>
<jsp:include page="/WEB-INF/baru/modul/pagesmastersirspendaftaranrawatjalanzul/index.jsp" />
<% } else { %>
<jsp:include page="/WEB-INF/baru/modul/pagesmastersirsstatuspembayaranzul/index.jsp" />
<% } %>
