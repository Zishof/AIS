<%@page import="ais.common.Common"%>
<%
String rnd = Common.getGeneratedBarCode(7);
%>

<h5 class="fw-bold mb-3"><%=Common.getBahasaConfig("Manajemen Anggota")%></h5>
<ul class="nav nav-tabs mb-3" id="subTabMember<%=rnd%>" role="tablist">
	<li class="nav-item" role="presentation">
		<button class="nav-link active" data-bs-toggle="tab"
			data-bs-target="#sub-data-member<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Data Member Baru")%></button>
	</li>
	<li class="nav-item" role="presentation">
		<button class="nav-link" data-bs-toggle="tab"
			data-bs-target="#sub-jenis-member<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Jenis Member")%></button>
	</li>
	<li class="nav-item" role="presentation">
		<button class="nav-link" data-bs-toggle="tab"
			data-bs-target="#sub-tipe-member<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Tipe Member")%></button>
	</li>
	<li class="nav-item" role="presentation">
		<button class="nav-link" data-bs-toggle="tab"
			data-bs-target="#sub-topup<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Topup")%></button>
	</li>
	<li class="nav-item" role="presentation">
		<button class="nav-link" data-bs-toggle="tab"
			data-bs-target="#sub-mutasi-tabungan<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Mutasi Tabungan")%></button>
	</li>
	<li class="nav-item" role="presentation">
		<button class="nav-link" data-bs-toggle="tab"
			data-bs-target="#sub-mutasi-hutang<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Mutasi Hutang")%></button>
	</li>
	<li class="nav-item" role="presentation">
		<button class="nav-link" data-bs-toggle="tab"
			data-bs-target="#sub-notifikasi<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Notifikasi")%></button>
	</li>
	<li class="nav-item" role="presentation">
		<button class="nav-link" data-bs-toggle="tab"
			data-bs-target="#sub-sinkron<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Sinkronisasi Siswa/Mahasiswa")%></button>
	</li>
</ul>
<div class="tab-content">
	<div class="tab-pane fade show active" id="sub-data-member<%=rnd%>"
		role="tabpanel">
		<jsp:include page="/WEB-INF/baru/modul/kantin/member/index.jsp" />
	</div>
	<div class="tab-pane fade" id="sub-jenis-member<%=rnd%>"
		role="tabpanel">
		<jsp:include
			page="/WEB-INF/baru/modul/kantin/member/jenis_anggota_koperasi.jsp" />
	</div>
	<div class="tab-pane fade" id="sub-tipe-member<%=rnd%>" role="tabpanel">
		<jsp:include
			page="/WEB-INF/baru/modul/kantin/member/tipe_anggota_koperasi.jsp" />
	</div>
	<div class="tab-pane fade" id="sub-topup<%=rnd%>" role="tabpanel">
		<jsp:include
			page="/WEB-INF/baru/modul/kantin/member/_manajemen_topup.jsp" />
	</div>
	<div class="tab-pane fade" id="sub-mutasi-tabungan<%=rnd%>" role="tabpanel">
		<jsp:include
			page="/WEB-INF/baru/modul/kantin/member/_mutasi_tabungan.jsp" />
	</div>
	<div class="tab-pane fade" id="sub-mutasi-hutang<%=rnd%>" role="tabpanel">
		<jsp:include
			page="/WEB-INF/baru/modul/kantin/member/_mutasi_hutang.jsp" />
	</div>
	<div class="tab-pane fade" id="sub-notifikasi<%=rnd%>" role="tabpanel">
		<jsp:include
			page="/WEB-INF/baru/modul/kantin/member/notifikasi.jsp" />
	</div>
	<div class="tab-pane fade" id="sub-sinkron<%=rnd%>" role="tabpanel">
		<jsp:include
			page="/WEB-INF/baru/modul/kantin/member/sinkron_siswa_mahasiswa.jsp" />
	</div>
</div>
