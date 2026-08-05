<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// SECURITY CHECK
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	out.print("{\"status\":\"error\", \"message\":\""
	+ Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
	return;
}

String rndTab = Common.getGeneratedBarCode(7);
%>

<div class="animate__animated animate__fadeInUp">
    <div class="row mb-4">
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
                
                <div class="card-header bg-white border-0 pt-4 pb-0 px-4">
                    <h5 class="fw-bold text-dark mb-1">
                        <i class="fas fa-user-tie text-success me-2"></i><%=Common.getBahasaConfig("Pusat Karya Pegawai")%>
                    </h5>
                    <small class="text-muted mb-3 d-block"><%=Common.getBahasaConfig("Dokumentasi karya inovasi, sistem, dan kontribusi profesional pegawai.")%></small>

                    <ul class="nav nav-tabs border-bottom-0" id="karyaTabPegawai<%=rndTab%>" role="tablist">
                        <li class="nav-item" role="presentation">
                            <button class="nav-link active fw-bold text-secondary" id="karya-pegawai-dashboard-tab-<%=rndTab%>" data-bs-toggle="tab" data-bs-target="#karya-pegawai-dashboard-pane-<%=rndTab%>" type="button" role="tab" aria-selected="true">
                                <i class="fas fa-table me-2"></i><%=Common.getBahasaConfig("Ringkasan")%>
                            </button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link fw-bold text-secondary" id="karya-pegawai-data-tab-<%=rndTab%>" data-bs-toggle="tab" data-bs-target="#karya-pegawai-data-pane-<%=rndTab%>" type="button" role="tab" aria-selected="false">
                                <i class="fas fa-file-alt me-2"></i><%=Common.getBahasaConfig("Data Karya")%>
                            </button>
                        </li>
                    </ul>
                </div>
                
                <div class="card-body p-4 bg-light rounded-bottom-4">
                    <div class="tab-content" id="karyaTabPegawaiContent<%=rndTab%>">
                        
                        <div class="tab-pane fade show active" id="karya-pegawai-dashboard-pane-<%=rndTab%>" role="tabpanel" tabindex="0">
                            <jsp:include page="/WEB-INF/baru/modul/prestasi/karya/_dashboard_karya_pegawai.jsp"/>
                        </div>

                        <div class="tab-pane fade" id="karya-pegawai-data-pane-<%=rndTab%>" role="tabpanel" tabindex="0">
                            <jsp:include page="/WEB-INF/baru/modul/prestasi/karya/_karya_pegawai.jsp"/>
                        </div>

                    </div>
                </div>
                
            </div>
        </div>
    </div>
</div>
