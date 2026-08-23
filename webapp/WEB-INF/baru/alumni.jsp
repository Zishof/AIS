<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%
boolean hanya_tampil_jsp = request.getParameter("hanya_tampil_jsp") != null && request.getParameter("hanya_tampil_jsp").trim().equalsIgnoreCase("true");

String p = "";
if(request.getParameter("p") != null && !request.getParameter("p").trim().isEmpty()){
	p = request.getParameter("p").trim();
}
String s = "";
if(request.getParameter("s") != null && !request.getParameter("s").trim().isEmpty()){
	s = request.getParameter("s").trim();
}

// Parameter ini membentuk nama JSP. Hanya izinkan nama berkas sederhana; karakter kutip dan
// traversal path harus berakhir pada halaman tidak ditemukan, bukan diteruskan ke dispatcher.
boolean modulValid = p.matches("[A-Za-z0-9_-]+") && s.matches("[A-Za-z0-9_-]+");


String urlLama = "";
if(request.getParameter("urlLama") != null && !request.getParameter("urlLama").trim().isEmpty()){
	urlLama = request.getParameter("urlLama").trim();
}

if(hanya_tampil_jsp){
	if(!p.trim().isEmpty() && !s.trim().isEmpty() && modulValid){
        	  
        	  try{
		          String pg = "/WEB-INF/baru/modul/"+p+"/"+s+".jsp";
		          if (application.getResource(pg) == null) {
		          	throw new java.io.FileNotFoundException(pg);
		          }
                  %>
                  <jsp:include page="<%=pg %>"></jsp:include>
                  <%
		  }catch(Exception e){
		  	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/alumni.jsp:32");
					%>
		  	<jsp:include page="/WEB-INF/baru/componen/tidak_ketemu_page.jsp"></jsp:include>
		  	<%
		  }
	} else {
		%><jsp:include page="/WEB-INF/baru/componen/tidak_ketemu_page.jsp"></jsp:include><%
	}
} else {
%>
<jsp:include page="/WEB-INF/baru/include/header.jsp"></jsp:include>

  <body>

    <!-- ===============================================-->
    <!--    Main Content-->
    <!-- ===============================================-->
    <main class="main" id="top">
      <div class="container-fluid">
        
        <jsp:include page="/WEB-INF/baru/modul/alumni/tracer_study.jsp"></jsp:include>
        
        <jsp:include page="/WEB-INF/baru/include/dialog-modal.jsp"></jsp:include>
        
      </div>
    </main>
    <!-- ===============================================-->
    <!--    End of Main Content-->
    <!-- ===============================================-->


    <jsp:include page="/WEB-INF/baru/include/foot.jsp"></jsp:include>

  </body>

</html>

<% 
} 
%>
