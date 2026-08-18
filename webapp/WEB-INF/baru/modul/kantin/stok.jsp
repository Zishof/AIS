<%@page import="ais.common.Common"%>
<%
String rnd = Common.getGeneratedBarCode(7);
%>
<h5 class="fw-bold mb-3"><%=Common.getBahasaConfig("Manajemen Stok Barang")%></h5>
<ul class="nav nav-tabs mb-3" id="subTabStok<%=rnd%>" role="tablist">
	<li class="nav-item" role="presentation">
		<button class="nav-link active" data-bs-toggle="tab"
			data-bs-target="#sub-kartu-stok<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Kartu Mutasi Stok")%></button>
	</li>
	<li class="nav-item" role="presentation">
		<button class="nav-link" data-bs-toggle="tab"
			data-bs-target="#sub-stok-opname<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Stok Opname")%></button>
	</li>
	<li class="nav-item" role="presentation">
		<button class="nav-link" data-bs-toggle="tab"
			data-bs-target="#sub-kedaluwarsa<%=rnd%>" type="button" role="tab"><i class="fas fa-calendar-xmark me-1"></i><%=Common.getBahasaConfig("Kedaluwarsa")%></button>
	</li>
	<li class="nav-item" role="presentation">
		<button class="nav-link" data-bs-toggle="tab"
			data-bs-target="#sub-opname-scan<%=rnd%>" type="button" role="tab"><i class="fas fa-barcode me-1"></i><%=Common.getBahasaConfig("SO by Scan (HP/PDT)")%></button>
	</li>
	<li class="nav-item" role="presentation">
		<button class="nav-link" data-bs-toggle="tab"
			data-bs-target="#sub-mutasi-outlet<%=rnd%>" type="button" role="tab"><i class="fas fa-truck-loading me-1"></i><%=Common.getBahasaConfig("Mutasi Antar Outlet")%></button>
	</li>
</ul>
<div class="tab-content">
	<div class="tab-pane fade show active" id="sub-kartu-stok<%=rnd%>"
		role="tabpanel">
		<jsp:include page="/WEB-INF/baru/modul/kantin/stok/mutasi_stok.jsp" />
	</div>
	<div class="tab-pane fade" id="sub-stok-opname<%=rnd%>" role="tabpanel">
		<jsp:include page="/WEB-INF/baru/modul/kantin/stok/index.jsp" />
	</div>
	<div class="tab-pane fade" id="sub-kedaluwarsa<%=rnd%>" role="tabpanel">
		<jsp:include page="/WEB-INF/baru/modul/kantin/stok_expired.jsp" />
	</div>
	<div class="tab-pane fade" id="sub-opname-scan<%=rnd%>" role="tabpanel">
		<jsp:include page="/WEB-INF/baru/modul/kantin/stok/opname_scan.jsp" />
	</div>
	<div class="tab-pane fade" id="sub-mutasi-outlet<%=rnd%>" role="tabpanel">
		<jsp:include page="/WEB-INF/baru/modul/kantin/stok/mutasi_antar_outlet.jsp" />
	</div>
</div>
