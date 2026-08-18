<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.KrsMahasiswa"%>
<%
KrsMahasiswa krsMahasiswa = (KrsMahasiswa) request.getAttribute("krsMahasiswa");
Mahasiswa mahasiswa = krsMahasiswa.getMahasiswa();
%>

<!--begin::Details content-->
<div class="pb-5 fs-6">
	<!--begin::Details item-->
	<div class="fw-bold mt-5">Nama</div>
	<div class="text-gray-600"><%=mahasiswa.getNama()%></div>
	<!--begin::Details item-->
	<div class="fw-bold mt-5">NIM</div>
	<div class="text-gray-600"><%=mahasiswa.getNim()%></div>
	<!--begin::Details item-->
	<!--begin::Details item-->
	<div class="fw-bold mt-5">Program</div>
	<div class="text-gray-600"><%=mahasiswa.getProgram()%></div>
	<!--begin::Details item-->
	<!--begin::Details item-->
	<div class="fw-bold mt-5">Fakultas</div>
	<div class="text-gray-600"><%=mahasiswa.getJurusan().getFakultas().getNama()%></div>
	<!--begin::Details item-->
	<!--begin::Details item-->
	<div class="fw-bold mt-5">Prodi</div>
	<div class="text-gray-600"><%=mahasiswa.getJurusan().getNama()%></div>
	<!--begin::Details item-->
	<!--begin::Details item-->
	<div class="fw-bold mt-5">Semester / Kelas</div>
	<div class="text-gray-600"><%=krsMahasiswa.getSemester() + " / " + krsMahasiswa.getKelas()%></div>
	<!--begin::Details item-->
	<!--begin::Details item-->
	<div class="fw-bold mt-5">Dosen Pembimbing/Wali</div>
	<div class="text-gray-600"><%=krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNama()%></div>
	<!--begin::Details item-->
	<!--begin::Details item-->
	<div class="fw-bold mt-5">SKS Semester</div>
	<div class="text-gray-600"><%=krsMahasiswa.getSksYangDiambil()%></div>
	<!--begin::Details item-->
	<div class="fw-bold mt-5">SKS Kumulatif</div>
	<div class="text-gray-600"><%=krsMahasiswa.getSksk()%></div>
	<!--begin::Details item-->
	<div class="fw-bold mt-5">IP Semester</div>
	<div class="text-gray-600"><%=Common.numberFormat.get().format(krsMahasiswa.getIps())%></div>
	<!--begin::Details item-->
	<div class="fw-bold mt-5">IP Kumulatif</div>
	<div class="text-gray-600"><%=Common.numberFormat.get().format(krsMahasiswa.getIpk())%></div>
	<!--begin::Details item-->
	<div class="fw-bold mt-5">Telp.</div>
	<div class="text-gray-600"><%=mahasiswa.getTelp()%></div>
	<!--begin::Details item-->
	<div class="fw-bold mt-5">Email.</div>
	<div class="text-gray-600"><%=mahasiswa.getEmail()%></div>
</div>
<!--end::Details content-->
