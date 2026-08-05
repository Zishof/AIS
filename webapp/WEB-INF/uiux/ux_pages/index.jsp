<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%
String pageData = request.getParameter("page");
String halaman = pageData==null||pageData.trim().isEmpty() ? "/WEB-INF/uiux/ux_pages/content/beranda.jsp" : "/WEB-INF/uiux/ux_pages/content/"+pageData+".jsp";

//cara pindah halaman
if(pageData!= null && !pageData.trim().isEmpty() && pageData.trim().equals("ruang_kelas")){
		halaman = "/WEB-INF/uiux/ux_pages/content/ruang_kelas.jsp" ;
}

if(pageData!= null && !pageData.trim().isEmpty() && (pageData.trim().equals("e-learning") || pageData.trim().equals("e-Learning"))){
	halaman = "/WEB-INF/uiux/ux_pages/content/e-Learning.jsp" ;
}

if(pageData!= null && !pageData.trim().isEmpty() && pageData.trim().equals("tugas_ya")){
	halaman = "/WEB-INF/uiux/ux_pages/content/menu2/tugas_ya.jsp" ;
}

if(pageData!= null && !pageData.trim().isEmpty() && pageData.trim().equals("kelas_online")){
	halaman = "/WEB-INF/uiux/ux_pages/content/menu2/kelas_online.jsp" ;
}

if(pageData!= null && !pageData.trim().isEmpty() && pageData.trim().equals("materi_ya")){
	halaman = "/WEB-INF/uiux/ux_pages/content/menu2/materi_ya.jsp" ;
}

if(pageData!= null && !pageData.trim().isEmpty() && pageData.trim().equals("ujian_ya")){
	halaman = "/WEB-INF/uiux/ux_pages/content/menu2/ujian_ya.jsp" ;
}






//end

%>
<jsp:include page="/WEB-INF/uiux/ux_pages/include/header.jsp"></jsp:include>

<body class="layout-fixed sidebar-expand-lg bg-body-tertiary"> <!--begin::App Wrapper-->
    <div class="app-wrapper"> <!--begin::Header-->
        <jsp:include page="/WEB-INF/uiux/ux_pages/include/navbar.jsp"></jsp:include>
        <jsp:include page="/WEB-INF/uiux/ux_pages/include/sidebar.jsp"></jsp:include>
        
       	
        <jsp:include page="<%=halaman %>"></jsp:include>
        
        
        <!--begin::Footer-->
        <jsp:include page="/WEB-INF/uiux/ux_pages/include/footer.jsp"></jsp:include>
    </div> <!--end::App Wrapper--> <!--begin::Script--> <!--begin::Third Party Plugin(OverlayScrollbars)-->
    <jsp:include page="/WEB-INF/uiux/ux_pages/include/footer_bawah.jsp"></jsp:include>
</body><!--end::Body-->

</html>