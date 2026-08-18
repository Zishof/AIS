<%@page import="ais.database.model.koperasi.PembelianAnggotaKoperasi"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
//2. SECURITY CHECK
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	out.print("{\"status\":\"error\", \"message\":\""
	+ Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
	return;
}
String rnd = Common.getGeneratedBarCode(7);


%>

<div class="row mb-3 animate__animated animate__fadeInDown">
	<div class="col-12">
		<ul class="nav nav-pills bg-white p-2 rounded-pill shadow-sm border"
			id="posMainTabs<%=rnd%>" role="tablist">
			<li class="nav-item flex-fill text-center" role="presentation">
				<button class="nav-link active rounded-pill fw-bold"
					id="tab-kasir-<%=rnd%>" data-bs-toggle="pill"
					data-bs-target="#pane-kasir-<%=rnd%>" type="button" role="tab">
					<i class="fas fa-cash-register me-2"></i><%=Common.getBahasaConfig("POS Transaksi")%>
				</button>
			</li>
			<li class="nav-item flex-fill text-center" role="presentation">
				<button class="nav-link rounded-pill fw-bold"
					id="tab-riwayat-<%=rnd%>" data-bs-toggle="pill"
					data-bs-target="#pane-riwayat-<%=rnd%>" type="button" role="tab">
					<i class="fas fa-history me-2"></i><%=Common.getBahasaConfig("Riwayat Transaksi")%>
				</button>
			</li>
			<li class="nav-item flex-fill text-center" role="presentation">
				<button class="nav-link rounded-pill fw-bold"
					id="tab-sinkron-<%=rnd%>" data-bs-toggle="pill"
					data-bs-target="#pane-sinkron-<%=rnd%>" type="button" role="tab">
					<i class="fas fa-rotate me-2"></i><%=Common.getBahasaConfig("Sinkronisasi POS")%>
				</button>
			</li>
			<li class="nav-item flex-fill text-center" role="presentation">
				<button class="nav-link rounded-pill fw-bold"
					id="tab-analitik-<%=rnd%>" data-bs-toggle="pill"
					data-bs-target="#pane-analitik-<%=rnd%>" type="button" role="tab">
					<i class="fas fa-chart-line me-2"></i><%=Common.getBahasaConfig("Analisis Penjualan")%>
				</button>
			</li>
		</ul>
	</div>
</div>

<div class="tab-content" id="posMainTabsContent<%=rnd%>">

	<div class="tab-pane fade show active" id="pane-kasir-<%=rnd%>"
		role="tabpanel">
		<jsp:include page="/WEB-INF/baru/modul/kantin/pos/_pos.jsp">
			<jsp:param name="rnd" value="<%=rnd%>" />
		</jsp:include>
	</div>


	<div class="tab-pane fade" id="pane-riwayat-<%=rnd%>" role="tabpanel">
		<jsp:include
			page="/WEB-INF/baru/modul/kantin/pos/_daftar_transaksi.jsp">
			<jsp:param name="rnd" value="<%=rnd%>" />
		</jsp:include>
	</div>

	<div class="tab-pane fade" id="pane-sinkron-<%=rnd%>" role="tabpanel">
		<jsp:include
			page="/WEB-INF/baru/modul/kantin/pos/_riwayat_sinkronisasi.jsp">
			<jsp:param name="rnd" value="<%=rnd%>" />
		</jsp:include>
	</div>

	<div class="tab-pane fade" id="pane-analitik-<%=rnd%>" role="tabpanel">
		<jsp:include page="/WEB-INF/baru/modul/kantin/_analisis_riwayat_penjualan.jsp" />
	</div>
</div>
