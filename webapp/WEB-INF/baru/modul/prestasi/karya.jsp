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

// Admin: Pengguna yang bukan Mahasiswa, Siswa, Dosen, atau Guru
boolean admin = currentMahasiswa == null && currentDosen == null && currentSiswa == null && currentGuru == null;

boolean[] ptYa = Common.chekPtAtauSekolah(tbmuser);
boolean pt = ptYa[0]; // Perguruan Tinggi
boolean ya = ptYa[1]; // Yayasan/Sekolah
String rndTab = Common.getGeneratedBarCode(7);
%>

<% if (admin) { %>
    <div class="animate__animated animate__fadeInUp">
        <div class="card border-0 shadow-sm rounded-4 border-top border-primary border-4">
            <div class="card-header bg-white border-0 pt-4 pb-0 px-4">
                <h5 class="fw-bold text-dark mb-3">
                    <i class="fas fa-lightbulb text-primary me-2"></i><%=Common.getBahasaConfig("Manajemen Karya")%>
                </h5>
                
                <ul class="nav nav-tabs border-bottom-0" id="karyaTab<%=rndTab%>" role="tablist">
                    <% if (pt) { %>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link active fw-bold text-secondary" id="karya-mhs-tab-<%=rndTab%>" data-bs-toggle="tab" data-bs-target="#karya-mhs-pane-<%=rndTab%>" type="button" role="tab">
                            <i class="fas fa-user-graduate me-1"></i><%=Common.getBahasaConfig("Mahasiswa")%>
                        </button>
                    </li>
                    <% } %>

                    <% if (ya) { %>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link <%= !pt ? "active" : "" %> fw-bold text-secondary" id="karya-siswa-tab-<%=rndTab%>" data-bs-toggle="tab" data-bs-target="#karya-siswa-pane-<%=rndTab%>" type="button" role="tab">
                            <i class="fas fa-user me-1"></i><%=Common.getBahasaConfig("Siswa")%>
                        </button>
                    </li>
                    <% } %>

                    <% if (pt) { %>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link fw-bold text-secondary" id="karya-dosen-tab-<%=rndTab%>" data-bs-toggle="tab" data-bs-target="#karya-dosen-pane-<%=rndTab%>" type="button" role="tab">
                            <i class="fas fa-chalkboard-teacher me-1"></i><%=Common.getBahasaConfig("Dosen")%>
                        </button>
                    </li>
                    <% } %>

                    <%-- <li class="nav-item" role="presentation">
                        <button class="nav-link fw-bold text-secondary" id="karya-pegawai-tab-<%=rndTab%>" data-bs-toggle="tab" data-bs-target="#karya-pegawai-pane-<%=rndTab%>" type="button" role="tab">
                            <i class="fas fa-user-tie me-1"></i><%=Common.getBahasaConfig("Pegawai")%>
                        </button>
                    </li> --%>
                </ul>
            </div>
            
            <div class="card-body p-4 bg-light rounded-bottom-4">
                <div class="tab-content" id="karyaTabContent<%=rndTab%>">
                    <% if (pt) { %>
                    <div class="tab-pane fade show active" id="karya-mhs-pane-<%=rndTab%>" role="tabpanel">
                        <jsp:include page="/WEB-INF/baru/modul/prestasi/karya/_tab_karya_mahasiswa.jsp" />
                    </div>
                    <% } %>

                    <% if (ya) { %>
                    <div class="tab-pane fade <%= !pt ? "show active" : "" %>" id="karya-siswa-pane-<%=rndTab%>" role="tabpanel">
                        <jsp:include page="/WEB-INF/baru/modul/prestasi/karya/_tab_karya_siswa.jsp" />
                    </div>
                    <% } %>
                    
                    <% if (pt) { %>
                    <div class="tab-pane fade" id="karya-dosen-pane-<%=rndTab%>" role="tabpanel">
                        <jsp:include page="/WEB-INF/baru/modul/prestasi/karya/_tab_karya_dosen.jsp" />
                    </div>
                    <% } %>

                    <%-- <div class="tab-pane fade" id="karya-pegawai-pane-<%=rndTab%>" role="tabpanel">
                        <jsp:include page="/WEB-INF/baru/modul/prestasi/karya/_tab_karya_pegawai.jsp" />
                    </div> --%>
                </div>
            </div>
        </div>
    </div>


<% } else if (currentMahasiswa != null) { %>
    <div class="animate__animated animate__fadeIn">
        <jsp:include page="/WEB-INF/baru/modul/prestasi/karya/_tab_karya_mahasiswa.jsp" />
    </div>

<% } else if (currentSiswa != null) { %>
    <div class="animate__animated animate__fadeIn">
        <jsp:include page="/WEB-INF/baru/modul/prestasi/karya/_tab_karya_siswa.jsp" />
    </div>

<% } else if (currentDosen != null) { %>
    <div class="animate__animated animate__fadeIn">
        <jsp:include page="/WEB-INF/baru/modul/prestasi/karya/_tab_karya_dosen.jsp" />
    </div>

<% } else if (currentPegawai != null) { %>
    <div class="animate__animated animate__fadeIn">
        <jsp:include page="/WEB-INF/baru/modul/prestasi/karya/_tab_karya_pegawai.jsp" />
    </div>
<% } %>