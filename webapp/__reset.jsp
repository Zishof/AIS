<%@page import="ais.action.master.sekolah.util.SekolahUtil"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.common.Common"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
</head>
<body>

	<%
		Session mySession = HibernateUtil.currentNativeSession();
	
	Sekolah sekolah = SekolahUtil.getSekolah(request);

		Integer count = ((Number) mySession
				.createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("is_encripted"),
						Restrictions.eq("is_encripted", false)))
				.setProjection(Projections.rowCount()).uniqueResult())
				.intValue();
		if (!count.equals(0)) {
			List<Mahasiswa> mahasiswas = mySession
					.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(
							Restrictions.isNull("is_encripted"),
							Restrictions.eq("is_encripted", false))).list();
			for (Mahasiswa mahasiswa : mahasiswas) {
				mahasiswa.setIs_encripted(true);
				mahasiswa.setPass(Common.desEncrypter.get().encrypt(mahasiswa
						.getPass()));
				mySession.getTransaction().begin();
				Common.refreshUpdate(mySession, mahasiswa);
				mySession.getTransaction().commit();

				out.println(mahasiswa.toString());
			}
		}

		HibernateUtil.closeSession();
	%>

</body>
</html>