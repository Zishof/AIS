<%@page import="ais.database.model.Tbmuser"%>
<%@page
	import="ais.action.master.dashboard.sekolah.DashboardStatistikJadwalMengajar"%>
<%@page import="ais.database.model.sekolah.JadwalPelajaran"%>
<%@page import="java.util.List"%>
<%@page import="ais.common.Common"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
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

<jsp:include page="/WEB-INF/o/ux/content/elearning/siswa/info_elearning.jsp">
	<jsp:param value="${dataJadwals}" name="dataJadwals" />
</jsp:include>

<jsp:include page="/WEB-INF/o/ux/content/elearning/linimasa.jsp"></jsp:include>

<jsp:include
	page="/WEB-INF/o/ux/content/elearning/pilihan_tahun_akademik.jsp"></jsp:include>
<jsp:include
	page="/WEB-INF/o/ux/content/elearning/siswa/daftar_matpel_elearning.jsp">
	<jsp:param value="${dataJadwals}" name="dataJadwals" />
</jsp:include>



