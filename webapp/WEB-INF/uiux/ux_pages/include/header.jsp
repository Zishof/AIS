<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html lang="id">
<!--begin::Head-->
<%
ais.database.model.sekolah.Sekolah sekolah = ais.action.master.sekolah.util.SekolahUtil.getSekolah(request);
String judul = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
String Telepon = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getTelepon();
String Alamat1 = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getAlamat1();
String Email = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getEmail();
String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request,
		"background_perguruanTinggi_");
String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request,
		"logo_perguruanTinggi_");
%>
<head>


<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title><%=(sekolah != null && sekolah.getId() != null) ? "eSchool " + sekolah.getNama() : "eCampus"%>
	| <%=judul%></title>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="description" content="Sistem Informasi Akademik Ecampus" />
<meta name="author" content="Mohammad Fauzi Murtadho" />
<meta name="keywords"
	content="Sistem Informasi Akademik Enterprise Sistem">
<!--end::Primary Meta Tags-->
<!--begin::Fonts-->
<link rel="stylesheet"
	href="https://cdn.jsdelivr.net/npm/@fontsource/source-sans-3@5.0.12/index.css"
	integrity="sha256-tXJfXfp6Ewt1ilPzLDtQnJV4hclT9XuaZUKyUvmyr+Q="
	crossorigin="anonymous">
<!--end::Fonts-->
<!--begin::Third Party Plugin(OverlayScrollbars)-->
<link rel="stylesheet"
	href="https://cdn.jsdelivr.net/npm/overlayscrollbars@2.3.0/styles/overlayscrollbars.min.css"
	integrity="sha256-dSokZseQNT08wYEWiz5iLI8QPlKxG+TswNRD8k35cpg="
	crossorigin="anonymous">
<!--end::Third Party Plugin(OverlayScrollbars)-->
<!--begin::Third Party Plugin(Bootstrap Icons)-->
<link rel="stylesheet"
	href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.min.css"
	integrity="sha256-Qsx5lrStHZyR9REqhUF8iQt73X06c8LGIUPzpOhwRrI="
	crossorigin="anonymous">
<!--end::Third Party Plugin(Bootstrap Icons)-->
<!--begin::Required Plugin(AdminLTE)-->
<link rel="stylesheet"
	href="<%=request.getContextPath()%>/component/adminlte/css/adminlte.css">
<!--end::Required Plugin(AdminLTE)-->
<!-- apexcharts -->
<link rel="stylesheet"
	href="https://cdn.jsdelivr.net/npm/apexcharts@3.37.1/dist/apexcharts.css"
	integrity="sha256-4MX+61mt9NVvvuPjUWdUdyfZfxSB1/Rf9WtqRHgG5S0="
	crossorigin="anonymous">
<!-- jsvectormap -->
<link rel="stylesheet"
	href="https://cdn.jsdelivr.net/npm/jsvectormap@1.5.3/dist/css/jsvectormap.min.css"
	integrity="sha256-+uGLJmmTKOqBr+2E6KDYs/NRsHxSkONXFHUL0fy2O/4="
	crossorigin="anonymous">
	<%-- <link rel="stylesheet" href="<%=request.getContextPath()%>../../../css/sidebar.css" > --%>
	<link rel="stylesheet" href="<%=request.getContextPath()%>/css/didi/sidebar.css" >
	<link rel="stylesheet" href="<%=request.getContextPath()%>/css/didi/elearning.css" >
	<link rel="stylesheet" href="<%=request.getContextPath()%>/css/didi/sidebar_kanan.css" >
			
		
<!--end::Head-->

  
</head>

<!--begin::Body-->