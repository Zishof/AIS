<%@page import="java.io.BufferedReader"%>
<%@page import="java.io.FileReader"%>
<%@page import="java.io.Reader"%>
<%@page import="java.util.Date"%>
<%@page import="ais.action.report.Report"%>
<%@page import="java.io.File"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.io.Serializable"%>
<%@page import="java.util.Map"%>
<%@page import="ais.database.model.employ.KenaikanPangkat"%>
<%@page import="ais.database.model.employ.RiwayatPelatihanPegawai"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.common.Common"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.Pegawai"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.database.model.employ.RiwayatPendidikanPegawai"%>

<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">




<html>

<head>
<meta http-equiv=Content-Type content="text/html; charset=windows-1252">
<meta name=Generator content="Microsoft Word 14 (filtered)">


</head>

<body lang=IN>

	<%
		String email = request.getParameter("email") == null ? "-11111111"
				: request.getParameter("email");
		String userId = request.getParameter("userId") == null ? "-11111111"
				: request.getParameter("userId");
	%>

	


	<%
		if (email.equals("default@liferay.com")
				|| request.getParameter("email") == null) {
			out.println("<font style=\"font-family: serif;font-size: x-large;color: blue;\">Anda harus login terlebih dahulu sebelum melihat daftar riwayat hidup pegawai.</font></body></html");
			return;
		}

		Session mySession = HibernateUtil.currentNativeSession();
		Tbmuser tbmuser = (Tbmuser) mySession
				.createCriteria(Tbmuser.class)
				.add(Restrictions.or(Restrictions.eq("userId", userId),
						Restrictions.eq("email", email))).setMaxResults(1)
				.uniqueResult();
		Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();

		if (pegawai == null) {
			out.println("<font style=\"font-family: serif;font-size: xx-large;color: red;\">Akun anda "
					+ (request.getParameter("email") == null ? "" : "("
							+ email + ")")
					+ " belum terhubung ke data pegawai. Harap segera menghubungi administrator untuk menghubungkan data anda ke data pegawai.</font></body></html");
			HibernateUtil.closeSession();
			return;
		}

		final Map<String, Serializable> parameters = new HashMap<String, Serializable>();
		parameters.put("id", pegawai.getId());
		File file = Report.generateFileReport(Report.PDF, parameters,
				"employ/daftar_riwayat_hidup", new Date(),
				getServletContext(), Common.locale);
	%>
	
<!-- 	<a -->
<%-- 		href="<%=request.getContextPath()%>/AmbilLaporanDaftarRiwayatHidup?type=<%=Report.PDF%>&email=<%=email%>&userId=<%=userId%>"> --%>
<%-- 		<img alt="Cetak" src="<%=request.getContextPath()%>/img/print.png"> --%>
<!-- 		Cetak -->
<!-- 	</a> -->

	<br> 

	<iframe height="550px" width="100%" align="top"
		src="<%=request.getContextPath()%>/report/<%=file.getName()%>"></iframe>

</body>

</html>
