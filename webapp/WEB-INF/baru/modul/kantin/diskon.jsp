<%@page import="ais.common.Common"%>
<%
String rnd = Common.getGeneratedBarCode(7);
%>
<h5 class="fw-bold mb-3"><%=Common.getBahasaConfig("Pengaturan Diskon")%></h5>
<ul class="nav nav-tabs mb-3" id="subTabDiskon<%=rnd%>" role="tablist">
	<li class="nav-item" role="presentation">
		<button class="nav-link active" data-bs-toggle="tab"
			data-bs-target="#sub-aturan-diskon<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Aturan Diskon")%></button>
	</li>
	<li class="nav-item" role="presentation">
		<button class="nav-link" data-bs-toggle="tab"
			data-bs-target="#sub-pencairan-diskon<%=rnd%>" type="button"
			role="tab"><%=Common.getBahasaConfig("Pencairan Diskon")%></button>
	</li>
	<li class="nav-item" role="presentation">
		<button class="nav-link" data-bs-toggle="tab"
			data-bs-target="#sub-monitor-diskon<%=rnd%>" type="button"
			role="tab"><%=Common.getBahasaConfig("Monitor Diskon")%></button>
	</li>
</ul>
<div class="tab-content">
	<div class="tab-pane fade show active" id="sub-aturan-diskon<%=rnd%>"
		role="tabpanel">
		<jsp:include page="/WEB-INF/baru/modul/kantin/diskon/index.jsp" />
	</div>
	<div class="tab-pane fade" id="sub-pencairan-diskon<%=rnd%>"
		role="tabpanel">
		<jsp:include
			page="/WEB-INF/baru/modul/kantin/diskon/pencairan_diskon.jsp" />
	</div>
	<div class="tab-pane fade" id="sub-monitor-diskon<%=rnd%>"
		role="tabpanel">
		<jsp:include
			page="/WEB-INF/baru/modul/kantin/diskon/monitor.jsp" />
	</div>
</div>