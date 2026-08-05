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
boolean sekolah = ptYa[1]; // Sekolah

String rndTab = Common.getGeneratedBarCode(5);
%>

<%
if (admin) {
%>
<div class="row animate__animated animate__fadeIn">
	<div class="col-12">
		<div class="card border-0 shadow-sm rounded-4">
			<div class="card-header bg-white border-0 pt-4 pb-0 px-4">
				<h5 class="fw-bold text-dark mb-3">
					<i class="fas fa-lightbulb text-primary me-2"></i><%=Common.getBahasaConfig("Manajemen Organisasi")%>
				</h5>
				<ul class="nav nav-tabs border-bottom-0"
					id="organisasiTab<%=rndTab%>" role="tablist">

					<%
					if (pt) {
					%>
					<li class="nav-item" role="presentation">
						<button class="nav-link active"
							id="organisasi-mahasiswa-tab-<%=rndTab%>" data-bs-toggle="pill"
							data-bs-target="#organisasi-mahasiswa-pane-<%=rndTab%>"
							type="button" role="tab"
							aria-controls="organisasi-mahasiswa-pane-<%=rndTab%>"
							aria-selected="true">
							<i class="fas fa-users me-2"></i><%=Common.getBahasaConfig("Organisasi Mahasiswa")%></button>
					</li>
					<%
					}
					%>

					<%
					if (sekolah) {
					%>
					<li class="nav-item" role="presentation">
						<button class="nav-link <%=!pt ? "active" : ""%>"
							id="organisasi-siswa-tab-<%=rndTab%>" data-bs-toggle="pill"
							data-bs-target="#organisasi-siswa-pane-<%=rndTab%>" type="button"
							role="tab" aria-controls="organisasi-siswa-pane-<%=rndTab%>"
							aria-selected="<%=!pt%>">
							<i class="fas fa-users me-2"></i><%=Common.getBahasaConfig("Organisasi Siswa")%></button>
					</li>
					<%
					}
					%>

					<%
					if (pt) {
					%>
					<li class="nav-item" role="presentation">
						<button class="nav-link" id="organisasi-dosen-tab-<%=rndTab%>"
							data-bs-toggle="pill"
							data-bs-target="#organisasi-dosen-pane-<%=rndTab%>" type="button"
							role="tab" aria-controls="organisasi-dosen-pane-<%=rndTab%>"
							aria-selected="false">
							<i class="fas fa-user-tie me-2"></i><%=Common.getBahasaConfig("Organisasi Dosen")%></button>
					</li>
					<%
					}
					%>

					<%-- <li class="nav-item" role="presentation">
                            <button class="nav-link" id="organisasi-pegawai-tab-<%=rndTab%>" data-bs-toggle="pill" data-bs-target="#organisasi-pegawai-pane-<%=rndTab%>" type="button" role="tab" aria-controls="organisasi-pegawai-pane-<%=rndTab%>" aria-selected="false"><i class="fas fa-user-cog me-2"></i><%=Common.getBahasaConfig("Organisasi Pegawai")%></button>
                        </li> --%>
				</ul>
			</div>

			<div class="card-body p-4 pt-0 tab-content"
				id="organisasiTabContent<%=rndTab%>">

				<%
				if (pt) {
				%>
				<div class="tab-pane fade show active"
					id="organisasi-mahasiswa-pane-<%=rndTab%>" role="tabpanel"
					aria-labelledby="organisasi-mahasiswa-tab-<%=rndTab%>" tabindex="0">
					<jsp:include
						page="/WEB-INF/baru/modul/prestasi/organisasi/_tab_organisasi_mahasiswa.jsp" />
				</div>
				<%
				}
				%>

				<%
				if (sekolah) {
				%>
				<div class="tab-pane fade <%=!pt ? "show active" : ""%>"
					id="organisasi-siswa-pane-<%=rndTab%>" role="tabpanel"
					aria-labelledby="organisasi-siswa-tab-<%=rndTab%>" tabindex="0">
					<jsp:include
						page="/WEB-INF/baru/modul/prestasi/organisasi/_tab_organisasi_siswa.jsp" />
				</div>
				<%
				}
				%>

				<%
				if (pt) {
				%>
				<div class="tab-pane fade" id="organisasi-dosen-pane-<%=rndTab%>"
					role="tabpanel" aria-labelledby="organisasi-dosen-tab-<%=rndTab%>"
					tabindex="0">
					<jsp:include
						page="/WEB-INF/baru/modul/prestasi/organisasi/_tab_organisasi_dosen.jsp" />
				</div>
				<%
				}
				%>

				<%-- <div class="tab-pane fade" id="organisasi-pegawai-pane-<%=rndTab%>" role="tabpanel" aria-labelledby="organisasi-pegawai-tab-<%=rndTab%>" tabindex="0">
                        <jsp:include page="/WEB-INF/baru/modul/prestasi/organisasi/_tab_organisasi_pegawai.jsp" />
                    </div> --%>
			</div>
		</div>
	</div>
</div>


<%
} else if (currentMahasiswa != null) {
%>
<div class="animate__animated animate__fadeIn">
	<jsp:include
		page="/WEB-INF/baru/modul/prestasi/organisasi/_tab_organisasi_mahasiswa.jsp" />
</div>

<%
} else if (currentSiswa != null) {
%>
<div class="animate__animated animate__fadeIn">
	<jsp:include
		page="/WEB-INF/baru/modul/prestasi/organisasi/_tab_organisasi_siswa.jsp" />
</div>

<%
} else if (currentDosen != null) {
%>
<div class="animate__animated animate__fadeIn">
	<jsp:include
		page="/WEB-INF/baru/modul/prestasi/organisasi/_tab_organisasi_dosen.jsp" />
</div>

<%-- <% } else if (currentPegawai != null || currentGuru != null) { %>
    <div class="animate__animated animate__fadeIn">
        <jsp:include page="/WEB-INF/baru/modul/prestasi/organisasi/_tab_organisasi_pegawai.jsp" />
    </div> --%>
<%
}
%>
