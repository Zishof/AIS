<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
String jenisPendaftaran = request.getParameter("jenis");
if (!"rawat_jalan".equals(jenisPendaftaran) && !"rawat_inap".equals(jenisPendaftaran)
        && !"ugd".equals(jenisPendaftaran)) jenisPendaftaran = "booking";
String basePendaftaran = request.getContextPath() + "/baru?p=emedik&s=pendaftaran&jenis=";
%>
<div class="mb-3 d-flex justify-content-between"><div><h4>Pendaftaran Pasien</h4><p class="text-muted">Booking dan registrasi rawat jalan, rawat inap, serta UGD.</p></div><a class="btn btn-warning align-self-start" href="<%=request.getContextPath()%>/baru?p=emedik&amp;s=help&amp;menu=emedik_pendaftaran"><i class="fas fa-question-circle"></i> Bantuan</a></div>
<ul class="nav nav-tabs mb-3">
  <li class="nav-item"><a class="nav-link <%= "booking".equals(jenisPendaftaran) ? "active" : "" %>" href="<%=basePendaftaran%>booking">Booking</a></li>
  <li class="nav-item"><a class="nav-link <%= "rawat_jalan".equals(jenisPendaftaran) ? "active" : "" %>" href="<%=basePendaftaran%>rawat_jalan">Rawat Jalan</a></li>
  <li class="nav-item"><a class="nav-link <%= "rawat_inap".equals(jenisPendaftaran) ? "active" : "" %>" href="<%=basePendaftaran%>rawat_inap">Rawat Inap</a></li>
  <li class="nav-item"><a class="nav-link <%= "ugd".equals(jenisPendaftaran) ? "active" : "" %>" href="<%=basePendaftaran%>ugd">UGD</a></li>
</ul>
<% if ("rawat_jalan".equals(jenisPendaftaran)) { %>
<jsp:include page="/WEB-INF/baru/modul/pagesmastersirspendaftaranrawatjalanzul/index.jsp" />
<% } else if ("rawat_inap".equals(jenisPendaftaran)) { %>
<jsp:include page="/WEB-INF/baru/modul/pagesmastersirspendaftaranrawatinapzul/index.jsp" />
<% } else if ("ugd".equals(jenisPendaftaran)) { %>
<jsp:include page="/WEB-INF/baru/modul/pagesmastersirspendaftaranrawatugdzul/index.jsp" />
<% } else { %>
<jsp:include page="/WEB-INF/baru/modul/pagesmastersirsbookingregistrasizul/index.jsp" />
<% } %>
