<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
	


    <!-- ===============================================-->
    <!--    JavaScripts-->
    <!-- ===============================================-->
    <script src="<%=request.getContextPath() %>/component/uiux/vendors/popper/popper.min.js"></script>
    <script src="<%=request.getContextPath() %>/component/uiux/vendors/bootstrap/bootstrap.min.js"></script>
    <script src="<%=request.getContextPath() %>/component/uiux/vendors/anchorjs/anchor.min.js"></script>
    <script src="<%=request.getContextPath() %>/component/uiux/vendors/is/is.min.js"></script>
    <script src="<%=request.getContextPath() %>/component/uiux/vendors/echarts/echarts.min.js"></script>
    <script src="<%=request.getContextPath() %>/component/uiux/vendors/fontawesome/all.min.js"></script>
    <script src="<%=request.getContextPath() %>/component/uiux/vendors/lodash/lodash.min.js"></script>
    <script src="<%=request.getContextPath() %>/component/uiux/vendors/list.js/list.min.js"></script>
    <script src="<%=request.getContextPath() %>/component/uiux/assets/js/theme.js"></script>

    <!-- Menu aksi baris "..." untuk tabel CRUD. Berdiri sendiri: tanpa
         Bootstrap/jQuery, dan panelnya dipindah ke <body> saat dibuka supaya
         tidak terpotong oleh kotak bergulir table-responsive. -->
    <script src="<%=request.getContextPath() %>/js/aksi_baris.js"></script>
    <script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"></script>
    <script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js"></script>

    <jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp" />