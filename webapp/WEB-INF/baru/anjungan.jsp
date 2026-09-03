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
	// Daftar putih: dispatcher publik ini hanya boleh menjadi proksi ke berkas
	// layanan milik modulnya SENDIRI (dipanggil dari _belum_login_anjungan.jsp).
	// Tanpa ini, p/s bebas dari klien bisa meng-include berkas _service.jsp modul
	// LAIN (keuangan/kepegawaian/akuntansi, dst.) yang tidak punya cek sesi sendiri.
	boolean psDiizinkan = "anjungan".equals(p)
			&& ("_login_pustaka_service".equals(s) || "_login_qrcode_service".equals(s));
	if(!p.trim().isEmpty() && !s.trim().isEmpty() && psDiizinkan){

        	  try{
        		  String pg = "/WEB-INF/baru/modul/"+p+"/"+s+".jsp";
                  %>
                  <jsp:include page="<%=pg %>"></jsp:include>
                  <%
        	  }catch(Exception e){
        		  e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/anjungan.jsp:32");
 					%>
        		  <jsp:include page="/WEB-INF/baru/componen/tidak_ketemu_page.jsp"></jsp:include>
        		  <%
        	  }

    } else if(!p.trim().isEmpty() && !s.trim().isEmpty()){
        response.sendError(403);
    }
} else {
%>
<jsp:include page="/WEB-INF/baru/modul/anjungan/anjungan.jsp"></jsp:include>
<jsp:include page="/WEB-INF/baru/include/pemilih_bahasa.jsp" />
<jsp:include page="/WEB-INF/baru/include/dialog-modal.jsp"></jsp:include>
<jsp:include page="/WEB-INF/baru/include/foot.jsp"></jsp:include>
<% 
} 
%>