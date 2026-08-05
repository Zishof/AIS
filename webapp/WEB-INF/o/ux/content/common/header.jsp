<%@page import="ais.action.master.sekolah.util.SekolahUtil"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%

String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request,
		"logo_perguruanTinggi_");
if (logo_PerguruanTinggi == null || logo_PerguruanTinggi.trim().isEmpty()) {
	logo_PerguruanTinggi = "/img/logo.png";
} 
Sekolah sekolah = SekolahUtil.getSekolah(request);
String judul = Common.getKonfigurasi("judul_header", "eCampus").getNilai();
if (sekolah != null && sekolah.getId() != null) {
	judul =  Common.getKonfigurasi("judul_header_sekolah", "eSchool").getNilai();
}

String title = (judul.isEmpty() ? "" : judul+" | ") + (sekolah!=null && sekolah.getId() != null ? sekolah.getNama() : ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama());
ais.database.model.sekolah.Yayasan yayasan = ais.action.master.sekolah.util.SekolahUtil.getYayasan(request);
if(sekolah != null && sekolah.getId() != null){
	title = (judul.isEmpty() ? "" : judul+" | ") + sekolah.getNama();
	
	String logo_PerguruanTinggi_local = ais.action.master.sekolah.util.SekolahUtil.getSekolahMedia(request,
					"logo_sekolah_");
			if(logo_PerguruanTinggi_local != null && !logo_PerguruanTinggi_local.endsWith("logo.png")){
				logo_PerguruanTinggi = logo_PerguruanTinggi_local;
			}
} else if(yayasan != null && yayasan.getId() != null){
	title = yayasan.getNama();
	
	String logo_PerguruanTinggi_local = ais.action.master.sekolah.util.SekolahUtil.getYayasanMedia(request,
					"logo_yayasan_");
			if(logo_PerguruanTinggi_local != null && !logo_PerguruanTinggi_local.endsWith("logo.png")){
				logo_PerguruanTinggi = logo_PerguruanTinggi_local;
			}
}
%>

<!--begin::Head-->
	<head><base href="">
		<title><%=title%></title> 
		<meta charset="utf-8" />
		<meta name="description" content="" />
		<meta name="keywords" content="" />
		<meta name="viewport" content="width=device-width, initial-scale=1" />
		<meta property="og:locale" content="id_ID" />
		<meta property="og:type" content="article" />
		<meta property="og:title" content="" />
		<meta property="og:url" content="" />
		<meta property="og:site_name" content="Fauzi" />
		<link rel="canonical" href="" />
		<link rel="shortcut icon" type="image/x-icon" href="<%=logo_PerguruanTinggi%>" />
		<!--begin::Fonts-->
		<link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Poppins:300,400,500,600,700" />
		<!--end::Fonts-->
		<!--begin::Vendor Stylesheets(used by this page)-->
		<link href="<%=request.getContextPath() %>/component/assets/plugins/custom/fullcalendar/fullcalendar.bundle.css" rel="stylesheet" type="text/css" />
		<link href="<%=request.getContextPath() %>/component/assets/plugins/custom/datatables/datatables.bundle.css" rel="stylesheet" type="text/css" />
		<!--end::Vendor Stylesheets-->
		<!--begin::Global Stylesheets Bundle(used by all pages)-->
		<link href="<%=request.getContextPath() %>/component/assets/plugins/global/plugins.bundle.css" rel="stylesheet" type="text/css" />
		<link href="<%=request.getContextPath() %>/component/assets/plugins/global/fontawesome/css/all.min.css" rel="stylesheet" type="text/css" />
		<link href="<%=request.getContextPath() %>/component/assets/css/style.bundle.css" rel="stylesheet" type="text/css" />
		<link href="<%=request.getContextPath() %>/css/common_style.css" rel="stylesheet" type="text/css" />
		
		
		<!--end::Global Stylesheets Bundle-->
		
		
		<script type="text/javascript" src="<%=request.getContextPath() %>/js/common.js"></script>
	</head>
	<!--end::Head-->