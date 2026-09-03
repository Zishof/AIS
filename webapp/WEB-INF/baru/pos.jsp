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


String urlLama = "";
if(request.getParameter("urlLama") != null && !request.getParameter("urlLama").trim().isEmpty()){
	urlLama = request.getParameter("urlLama").trim();
}

if(hanya_tampil_jsp){
	// PERBAIKAN KEAMANAN (task_1f9c66d3, rujuk r83764 webapp/WEB-INF/baru/tamu.jsp): sebelumnya p/s
	// diambil mentah tanpa daftar putih -- proksi anonim ke JSP layanan modul APA PUN. Inventarisasi
	// menyeluruh (grep atas seluruh webapp/ utk "hanya_tampil_jsp"/"/pos?") TIDAK menemukan satupun
	// pemanggil sah utk dispatcher root pos.jsp ini -- semua layanan kantin/pos (layar_pelanggan,
	// pos_offline_service, survey_kepuasan_service, verifikasi_pin_service,
	// verifikasi_biometric_service, cetak_struk) selalu dipanggil lewat dispatcher /baru
	// (p=kantin%2Fpos) yang sudah punya gerbang otorisasi modul kantin sendiri di Baru.java, BUKAN
	// lewat /pos?hanya_tampil_jsp=true. Karena tidak ada pasangan p/s yang sah utk rute ini, daftar
	// putih sengaja dikosongkan (deny-all) alih-alih menebak/membuka kombinasi baru.
	boolean psDiizinkan = false;
	if(!p.trim().isEmpty() && !s.trim().isEmpty() && psDiizinkan){

        	  try{
        		  String pg = "/WEB-INF/baru/modul/"+p+"/"+s+".jsp";
                  %>
                  <jsp:include page="<%=pg %>"></jsp:include>
                  <%
        	  }catch(Exception e){
        		  e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/pos.jsp:32");
 					%>
        		  <jsp:include page="/WEB-INF/baru/componen/tidak_ketemu_page.jsp"></jsp:include>
        		  <%
        	  }

    } else if(!p.trim().isEmpty() && !s.trim().isEmpty()){
    	response.sendError(403);
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
        
        <jsp:include page="/WEB-INF/baru/modul/kantin/pos/_pos.jsp"></jsp:include>
        
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