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
	// Daftar putih: dispatcher ini (dipanggil lewat /dsh atau /dashboard, lihat
	// ais.action.servlet.Dashboard -- pengecekan sesinya bergantung pada konfigurasi
	// "akses_ke_dashboard_tanpa_login_tidak_diizinkan" sehingga bisa saja publik) hanya
	// boleh menjadi proksi ke berkas statistik yang benar-benar dipakai oleh tab-tab
	// modul/dsh/_dashboard.jsp dan modul/dsh/_dashboard_sekolah.jsp. Tanpa ini, p/s bebas
	// dari klien bisa meng-include berkas modul LAIN (keuangan/kepegawaian/akuntansi, dst.)
	// yang tidak punya cek sesi/otorisasi sendiri.
	java.util.Set<String> allowedPasangan = new java.util.HashSet<String>(java.util.Arrays.asList(
		"pagesmastermatakuliahzul|_statistik_matakuliah",
		"pagesmasterkurikulumzul|_statistik_kurikulum",
		"pagesmasterdosenzul|_statistik_dosen",
		"pagesmastermahasiswazul|_statistik_mahasiswa_aktif",
		"pagesmasterperkuliahanzul|_statistik_perkuliahan",
		"elearning|_statistik_pertemuan",
		"alumni|_statistik_alumni",
		"pagesmastersekolahmatapelajaranzul|_statistik_matapelajaran",
		"pagesmastersekolahkurikulumsekolahzul|_statistik_kurikulum_sekolah",
		"pagesmastersekolahguruzul|_statistik_guru"
	));
	boolean formatDasarValid = p.matches("[A-Za-z0-9_/-]+") && s.matches("[A-Za-z0-9_/-]+")
			&& !p.contains("..") && !s.contains("..");
	boolean psDiizinkan = formatDasarValid && allowedPasangan.contains(p + "|" + s);
	if(!p.trim().isEmpty() && !s.trim().isEmpty() && psDiizinkan){

        	  try{
        		  String pg = "/WEB-INF/baru/modul/"+p+"/"+s+".jsp";
                  %>
                  <jsp:include page="<%=pg %>"></jsp:include>
                  <%
        	  }catch(Exception e){
        		  e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/dashboard.jsp:32");
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
        
        <jsp:include page="/WEB-INF/baru/modul/dsh/_dashboard.jsp">
        	<jsp:param value="tampilkan_header" name="true"/>
        </jsp:include>
        
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