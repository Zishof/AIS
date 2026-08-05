<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.kursus.PesertaKursus"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%
try{
	Tbmuser tbmuser = Common.getCurrentUser(request);
	if(tbmuser != null && tbmuser.getPesertaKursus() ==null){
		Siswa siswa = tbmuser.getSiswa();
		Mahasiswa mahasiswa = tbmuser.getMahasiswa();
		PesertaKursus pesertaKursus = null;
		for (Object o : ConstantValues.ambilBerdasarClass(PesertaKursus.class).values()) {
			PesertaKursus p = (PesertaKursus) o;
			if (p.getSiswa() != null && p.getSiswa().getId() != null && siswa != null && siswa.getId() != null
					&& siswa.getId().equals(p.getSiswa().getId())) {
				pesertaKursus = p;
				break;
			} else if (p.getMahasiswa() != null && p.getMahasiswa().getId() != null && mahasiswa != null
					&& mahasiswa.getId() != null && mahasiswa.getId().equals(p.getMahasiswa().getId())) {
				pesertaKursus = p;
				break;
			} else if (tbmuser.getUserId() != null && p.getTbmuser() != null
					&& p.getTbmuser().getUserId() != null
					&& p.getTbmuser().getUserId().equals(tbmuser.getUserId())) {
				pesertaKursus = p;
				break;
			}
		}
		
		if(pesertaKursus == null){
			Session mySession = HibernateUtil.currentNativeSession();
			
			pesertaKursus =	(PesertaKursus)mySession.createCriteria(PesertaKursus.class)
					.add(Restrictions.eq("kode", tbmuser.getUserId() ))
					.setMaxResults(1)
					.uniqueResult();
			if(pesertaKursus == null){
				pesertaKursus = new PesertaKursus();
				pesertaKursus.setTbmuser(tbmuser);
				pesertaKursus.setMahasiswa(mahasiswa);
				pesertaKursus.setSiswa(siswa);
				pesertaKursus.setNama(tbmuser.getUserNama());
				pesertaKursus.setEmail(tbmuser.getEmail());
				pesertaKursus.setTelp(tbmuser.getHp());
				
				mySession.getTransaction().begin();
				mySession.save(pesertaKursus);
				mySession.getTransaction().commit();
			}
			// mySession.disconnect();
			if (mySession.isOpen()) {mySession.disconnect();mySession.close();}
			HibernateUtil.closeSession();
		}
		
		tbmuser.setPesertaKursus(pesertaKursus); 
		
		session.setAttribute("PesertaKursus", tbmuser.getPesertaKursus());
		session.setAttribute("mytbmuser", tbmuser);
		session.setAttribute("usersTemp", tbmuser);
		session.setAttribute("user", tbmuser);
	}
}catch(Exception e){
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/o/kursus/inc/header.jsp:70");
}
%>   
<!DOCTYPE html>
<html lang="id">

<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <meta name="description" content="<%=ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama()%> &#8211; Media Pembelajaran Online">
    <meta name="author" content="Fauzi">
    <title>Kursus | <%=ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama()%></title>

    <!-- Favicons-->
    <link rel="shortcut icon" href="<%=request.getContextPath() %>/img/logo.png" type="image/x-icon">
    <link rel="apple-touch-icon" type="image/x-icon" href="<%=request.getContextPath() %>/img/apple-touch-icon-57x57-precomposed.png">
    <link rel="apple-touch-icon" type="image/x-icon" sizes="72x72" href="<%=request.getContextPath() %>/img/apple-touch-icon-72x72-precomposed.png">
    <link rel="apple-touch-icon" type="image/x-icon" sizes="114x114" href="<%=request.getContextPath() %>/img/apple-touch-icon-114x114-precomposed.png">
    <link rel="apple-touch-icon" type="image/x-icon" sizes="144x144" href="<%=request.getContextPath() %>/img/apple-touch-icon-144x144-precomposed.png">

    <!-- BASE CSS -->
    <link href="<%=request.getContextPath() %>/kursus/css/bootstrap.min.css" rel="stylesheet">
    <link href="<%=request.getContextPath() %>/kursus/css/style.css" rel="stylesheet">
	<link href="<%=request.getContextPath() %>/kursus/css/vendors.css" rel="stylesheet">
	<link href="<%=request.getContextPath() %>/kursus/css/icon_fonts/css/all_icons.min.css" rel="stylesheet">

    <!-- YOUR CUSTOM CSS -->
    <link href="<%=request.getContextPath() %>/kursus/css/custom.css" rel="stylesheet">

</head>

