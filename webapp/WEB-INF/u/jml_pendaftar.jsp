<%@page import="ais.common.Common"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<link rel="shortcut icon"
	href="<%=request.getContextPath()%>/img/logo.png" type="image/x-icon" />
<meta name="viewport"
	content="width=device-width, initial-scale=1, shrink-to-fit=no" />
<meta name="description" content="Sistem Informasi Akademik Ecampus" />
<meta name="author" content="Mohammad Fauzi Murtadho" />
<title>Jumlah Pendaftar</title>
</head>
<body>
	<%
		String prodi = request.getParameter("prodi");
		String ta = request.getParameter("ta");
		String tambah = request.getParameter("tambah");

		Session mySession = HibernateUtil.currentNativeSession();
		Number jumlah = (Number) mySession.createCriteria(BiodataCalonMahasiswa.class)
				.add(ta == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahun", Integer.parseInt(ta.trim())))
				.add(request.getParameter("smt") == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("semesterMulai", request.getParameter("smt")))
				.add(prodi == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("prodi1.id", Long.parseLong(prodi.trim())),
								Restrictions.eq("prodiLulus.id", Long.parseLong(prodi.trim()))))
				.setProjection(Projections.rowCount()).uniqueResult();
		int tambahan = (tambah==null||!Common.isNumber(tambah)?0:Integer.parseInt(tambah.trim()));
		out.println((jumlah.intValue()+tambahan));
		HibernateUtil.closeSession();
	%>
</body>
</html>