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
	if(!p.trim().isEmpty() && !s.trim().isEmpty()){

		java.util.Set<String> pmbAllowedPairs = new java.util.HashSet<String>(java.util.Arrays.asList(
			"pmb/landing_page", "pmb/_sukses_login", "pmb/_pendaftaran_mahasiswa",
			"pmb/_pendaftaran_mahasiswa_service", "pmb/_pendaftaran_mahasiswa_validasi_service",
			"pmb/_form_tambahan_pendaftaran_mahasiswa", "pmb/_form_verifikasi_berkas_mahasiswa",
			"pmb/_cetak_biodata_calon_mahasiswa", "pmb/_cetak_kartu_pendaftaran", "pmb/_cetak_kartu_ujian",
			"pmb/_cetak_bukti_diterima", "pmb/_cetak_e_ktm", "pmb/_cetak_bukti_pembayaran_mhs",
			"pmb/_cetak_bukti_bayar", "pmb/_bukti_bayar", "pmb/_alur_pendaftaran",
			"pmb/_ikut_ujian_online", "pmb/_ikut_ujian_online_service", "pmb/_wawancara", "pmb/_wawancara_service",
			"pmb/_tampilkan_berkas_di_sukses_login", "pmb/_gelombang", "pmb/_pengumuman", "pmb/_pilihan_prodi",
			"bayarmhs/pembayaran_online_mhs", "common/upload_component", "home/pengumuman_rinci"
		));

		boolean psFormatValid = p.matches("[A-Za-z0-9_/-]+") && s.matches("[A-Za-z0-9_/-]+") && !p.contains("..") && !s.contains("..");

		if(!psFormatValid){
			response.sendError(400);
		} else if(!pmbAllowedPairs.contains(p+"/"+s)){
			response.sendError(404);
		} else {
        	  try{
        		  String pg = "/WEB-INF/baru/modul/"+p+"/"+s+".jsp";
                  %>
                  <jsp:include page="<%=pg %>"></jsp:include>
                  <%
        	  }catch(Exception e){
        		  e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/pmb.jsp:32");
 					%>
        		  <jsp:include page="/WEB-INF/baru/componen/tidak_ketemu_page.jsp"></jsp:include>
        		  <%
        	  }
		}

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
        
        <jsp:include page="/WEB-INF/baru/modul/pmb/landing_page.jsp"></jsp:include>
        
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