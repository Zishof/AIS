<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
String jenisLaporan = "kunjungan".equals(request.getParameter("jenis")) ? "kunjungan" : "tagihan";
String baseLaporan = request.getContextPath() + "/baru?p=emedik&s=laporan&jenis=";
%>
<div class="mb-3 d-flex justify-content-between"><div><h4>Laporan eMedik</h4><p class="text-muted">Monitoring pendaftaran, tagihan, dan pembayaran layanan medis.</p></div><div><a class="btn btn-success align-self-start me-2" href="<%=request.getContextPath()%>/baru?p=emedik&amp;s=help&amp;mode=qa&amp;menu=emedik_laporan"><i class="fas fa-comments"></i> Tanya Jawab</a><a class="btn btn-warning align-self-start" href="<%=request.getContextPath()%>/baru?p=emedik&amp;s=help&amp;menu=emedik_laporan"><i class="fas fa-question-circle"></i> Bantuan</a></div></div>
<ul class="nav nav-tabs mb-3">
  <li class="nav-item"><a class="nav-link <%= "tagihan".equals(jenisLaporan) ? "active" : "" %>" href="<%=baseLaporan%>tagihan">Status Pembayaran</a></li>
  <li class="nav-item"><a class="nav-link <%= "kunjungan".equals(jenisLaporan) ? "active" : "" %>" href="<%=baseLaporan%>kunjungan">Kunjungan Pasien</a></li>
</ul>
<% if ("kunjungan".equals(jenisLaporan)) { %>
<jsp:include page="/WEB-INF/baru/modul/pagesmastersirspendaftaranrawatjalanzul/index.jsp" />
<% } else { %>
<jsp:include page="/WEB-INF/baru/modul/pagesmastersirsstatuspembayaranzul/index.jsp" />
<% } %>
