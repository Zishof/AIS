<%@page import="ais.common.CommonMedia"%>
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
Pedagang pedagang = tbmuser.getPedagang();
Toko toko = pedagang == null ? null : pedagang.getToko();
String rnd = Common.getGeneratedBarCode(7);

// Menyiapkan path foto (Ganti 'foto_path' dengan getter yang sesuai dari model Anda jika ada)
String pathFotoPedagang = CommonMedia.getUrlFotoPengguna(tbmuser);
%>

<div class="container-fluid px-0">
	<div
		class="card border-0 shadow-sm rounded-4 mb-4 border-start border-primary border-5">
		<div class="card-body p-3">
			<div class="d-flex justify-content-between align-items-center">
				<div class="d-flex align-items-center">
					<%
					if (toko != null && pedagang != null) {
					%>
					<div class="position-relative me-3">
						<div
							class="rounded-circle border border-3 border-white shadow-sm overflow-hidden"
							style="width: 70px; height: 70px;">
							<img src="<%=pathFotoPedagang%>" class="img-fluid w-100 h-100"
								style="object-fit: cover;" alt="Seller Photo">
						</div>
						<span
							class="position-absolute bottom-0 end-0 p-1 bg-success border border-2 border-white rounded-circle"
							title="<%=Common.getBahasaConfig("Online")%>"></span>
					</div>

					<div>
						<h4 class="fw-bold text-dark mb-0"><%=toko.getNama()%></h4>
						<div class="d-flex align-items-center gap-2 mt-1">
							<span class="text-muted small"> <i
								class="fas fa-user-circle text-primary me-1"></i><%=tbmuser.getUserNama()%>
							</span> <span
								class="badge bg-primary bg-opacity-10 text-primary border border-primary border-opacity-25 rounded-pill px-2 small fw-semibold">
								<i class="fas fa-check-circle me-1 small"></i><%=Common.getBahasaConfig("Akun Penjual")%>
							</span>
						</div>
					</div>
					<%
					} else {
					%>
					<div class="bg-primary bg-opacity-10 p-3 rounded-circle me-3">
						<i class="fas fa-store-alt fa-2x text-primary"></i>
					</div>
					<div>
						<h4 class="fw-bold text-dark mb-1"><%=Common.getBahasaConfig("Manajemen Kantin")%></h4>
						<p class="text-muted small mb-0"><%=Common.getBahasaConfig("Kelola dasbor analitik, transaksi penjualan, dan konfigurasi master data.")%></p>
					</div>
					<%
					}
					%>
				</div>

				<div class="d-none d-md-block text-end pe-3">
					<div class="text-secondary small fw-bold"><%=Common.getBahasaConfig("Selamat Datang")%></div>
					<div class="text-dark fw-bold"><%=tbmuser.getUserNama()%></div>
				</div>
			</div>
		</div>
	</div>

	<div class="card border-0 shadow-sm rounded-4 mb-4">
		<div class="card-body p-2">
		
		<jsp:include page="/WEB-INF/baru/modul/kantin/dashboard.jsp" />
		
		</div>
	</div>

	
</div>