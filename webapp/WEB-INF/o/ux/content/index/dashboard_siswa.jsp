<%@page import="ais.database.model.sekolah.JadwalPelajaran"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="java.util.List"%>
<%@page
	import="ais.action.master.dashboard.sekolah.DashboardStatistikJadwalMengajar"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null && tbmuser.getSiswa() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}

String ta = Common.getCurrentTahunAkademik();
boolean ganjil = Common.isNowSemensterGanjil();
String idsmt = request.getParameter("idsmt");

if (idsmt != null) {
	ta = (Integer.parseInt(idsmt.substring(0, 4)) - 1) + "/" + (idsmt.substring(0, 4));
	ganjil = Integer.parseInt(idsmt.substring(4, 5)) == 1;
}

Integer semester = ganjil ? 1 : 2;

List<JadwalPelajaran> dataJadwals = DashboardStatistikJadwalMengajar.ambilDataSemua(tbmuser.getSiswa(),
		tbmuser.getGuru(), ta, semester);
request.setAttribute("dataJadwals", dataJadwals);
%>

<!--begin::Row-->
<div class="row g-5 g-xl-10 mb-5">
	<jsp:include page="/WEB-INF/o/ux/content/index/beranda_kiri_siswa.jsp">
		<jsp:param value="${dataJadwals}" name="dataJadwals" />
	</jsp:include>
	<jsp:include page="/WEB-INF/o/ux/content/index/beranda_kanan_siswa.jsp">
		<jsp:param value="${dataJadwals}" name="dataJadwals" />
	</jsp:include>
</div>
<!--end::Row-->
