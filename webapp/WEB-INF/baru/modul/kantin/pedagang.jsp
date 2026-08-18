<%@page import="ais.common.Common"%>
<%
String rnd = Common.getGeneratedBarCode(7);
%>
<h5 class="fw-bold mb-3"><%=Common.getBahasaConfig("Manajemen Pedagang")%></h5>

<ul class="nav nav-tabs mb-3" id="subTabPedagang<%=rnd%>" role="tablist">
	<li class="nav-item" role="presentation">
		<button class="nav-link active" data-bs-toggle="tab"
			data-bs-target="#sub-toko<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Data Toko (Kios)")%></button>
	</li>
	<li class="nav-item" role="presentation">
		<button class="nav-link" data-bs-toggle="tab"
			data-bs-target="#sub-akun-pedagang<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Akun Pedagang")%></button>
	</li>
</ul>

<div class="tab-content">
	<div class="tab-pane fade show active" id="sub-toko<%=rnd%>"
		role="tabpanel">
		<jsp:include
			page="/WEB-INF/baru/modul/kantin/toko_dan_pedagang/toko.jsp" />
	</div>

	<div class="tab-pane fade" id="sub-akun-pedagang<%=rnd%>"
		role="tabpanel">
		<jsp:include
			page="/WEB-INF/baru/modul/kantin/toko_dan_pedagang/pedagang.jsp" />
	</div>
</div>