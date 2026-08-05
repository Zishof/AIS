<%@page import="ais.database.model.Pegawai"%>
<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.Mahasiswa"%>
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

Mahasiswa currentMahasiswa = tbmuser.getMahasiswa();
Dosen currentDosen = tbmuser.ambilDosen();
Siswa currentSiswa = tbmuser.getSiswa();
Guru currentGuru = tbmuser.ambilGuru();
Pegawai currentPegawai = tbmuser.ambilPegawai();

// Admin diartikan sebagai pengguna yang bukan mahasiswa, bukan dosen, bukan siswa, dan bukan guru (Bisa jadi Pegawai murni atau Super Admin)
boolean admin = currentMahasiswa == null && currentDosen == null && currentSiswa == null && currentGuru == null;
boolean[] ptYa = Common.chekPtAtauSekolah(tbmuser);
boolean pt = ptYa[0];
boolean ya = ptYa[1];
String rndTab = Common.getGeneratedBarCode(7); 
%>

<% if (admin) { %>
    <div class="animate__animated animate__fadeInUp">
        <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
            <div class="card-header bg-white border-0 pt-4 pb-0 px-4">
                <h5 class="fw-bold text-dark mb-3">
                    <i class="fas fa-clipboard-list text-primary me-2"></i><%=Common.getBahasaConfig("Pusat Manajemen Kegiatan")%>
                </h5>
                
                <ul class="nav nav-tabs border-bottom-0" id="kegiatanTab<%=rndTab%>" role="tablist">
                	<% if (pt) { %>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link active fw-bold text-secondary" id="mhs-tab-<%=rndTab%>" data-bs-toggle="tab" data-bs-target="#mhs-pane-<%=rndTab%>" type="button" role="tab" aria-selected="true">
                            <i class="fas fa-user-graduate me-1"></i><%=Common.getBahasaConfig("Mahasiswa")%>
                        </button>
                    </li>
                    <% } %>
                    <% if (ya) { %>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link fw-bold text-secondary" id="siswa-tab-<%=rndTab%>" data-bs-toggle="tab" data-bs-target="#siswa-pane-<%=rndTab%>" type="button" role="tab" aria-selected="false">
                            <i class="fas fa-user me-1"></i><%=Common.getBahasaConfig("Siswa")%>
                        </button>
                    </li>
                    <% } %>
                    <% if (pt) { %>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link fw-bold text-secondary" id="dosen-tab-<%=rndTab%>" data-bs-toggle="tab" data-bs-target="#dosen-pane-<%=rndTab%>" type="button" role="tab" aria-selected="false">
                            <i class="fas fa-chalkboard-teacher me-1"></i><%=Common.getBahasaConfig("Dosen")%>
                        </button>
                    </li>
                     <% } %>
                     <% if (ya) { %>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link fw-bold text-secondary" id="guru-tab-<%=rndTab%>" data-bs-toggle="tab" data-bs-target="#guru-pane-<%=rndTab%>" type="button" role="tab" aria-selected="false">
                            <i class="fas fa-book-reader me-1"></i><%=Common.getBahasaConfig("Guru")%>
                        </button>
                    </li>
                    <% } %>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link fw-bold text-secondary" id="pegawai-tab-<%=rndTab%>" data-bs-toggle="tab" data-bs-target="#pegawai-pane-<%=rndTab%>" type="button" role="tab" aria-selected="false">
                            <i class="fas fa-user-tie me-1"></i><%=Common.getBahasaConfig("Pegawai")%>
                        </button>
                    </li>
                </ul>
            </div>
            
            <div class="card-body p-4 bg-light rounded-bottom-4">
                <div class="tab-content" id="kegiatanTabContent<%=rndTab%>">
                <% if (pt) { %>
                    <div class="tab-pane fade show active" id="mhs-pane-<%=rndTab%>" role="tabpanel" tabindex="0">
                        <jsp:include page="/WEB-INF/baru/modul/kegiatan/_tab_mahasiswa.jsp" />
                    </div>
                    <% } %>
                    <% if (ya) { %>
                    <div class="tab-pane fade" id="siswa-pane-<%=rndTab%>" role="tabpanel" tabindex="0">
                        <jsp:include page="/WEB-INF/baru/modul/kegiatan/_tab_siswa.jsp" />
                    </div>
                    <% } %>
                    <% if (pt) { %>
                    <div class="tab-pane fade" id="dosen-pane-<%=rndTab%>" role="tabpanel" tabindex="0">
                        <jsp:include page="/WEB-INF/baru/modul/kegiatan/_tab_dosen.jsp" />
                    </div>
                    <% } %>
                    <% if (ya) { %>
                    <div class="tab-pane fade" id="guru-pane-<%=rndTab%>" role="tabpanel" tabindex="0">
                        <jsp:include page="/WEB-INF/baru/modul/kegiatan/_tab_guru.jsp" />
                    </div>
                    <% } %>
                    <div class="tab-pane fade" id="pegawai-pane-<%=rndTab%>" role="tabpanel" tabindex="0">
                        <jsp:include page="/WEB-INF/baru/modul/kegiatan/_tab_pegawai.jsp" />
                    </div>
                </div>
            </div>
        </div>
    </div>

<% } else if (currentMahasiswa != null) { %>
    <div class="animate__animated animate__fadeIn">
        <jsp:include page="/WEB-INF/baru/modul/kegiatan/_tab_mahasiswa.jsp" />
    </div>

<% } else if (currentSiswa != null) { %>
    <div class="animate__animated animate__fadeIn">
        <jsp:include page="/WEB-INF/baru/modul/kegiatan/_tab_siswa.jsp" />
    </div>

<% } else if (currentDosen != null) { %>
    <div class="animate__animated animate__fadeIn">
        <jsp:include page="/WEB-INF/baru/modul/kegiatan/_tab_dosen.jsp" />
    </div>

<% } else if (currentGuru != null) { %>
    <div class="animate__animated animate__fadeIn">
        <jsp:include page="/WEB-INF/baru/modul/kegiatan/_tab_guru.jsp" />
    </div>
<% } %>