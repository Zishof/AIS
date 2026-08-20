<%@page import="ais.common.Common"%><%@page import="ais.common.ConstantValues"%><%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%><%
response.setHeader("X-Content-Type-Options", "nosniff");
response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
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
        try{
            java.util.Set<String> pustakaPages = new java.util.HashSet<String>(java.util.Arrays.asList(
                "katalog", "populer", "sirkulasi", "kunjungan", "dashboard", "beranda_anggota", "integrasi",
                "_informasi_pustaka", "_item_rinci", "_catalog_api", "_beranda_anggota_service",
                "_login_pustaka_service", "_welpus_service", "_workspace_api", "_integrations_api", "_oai"));
            if ("pustaka".equals(p) && !pustakaPages.contains(s)) {
                response.sendError(404);
                return;
            }
            if (!p.matches("[A-Za-z0-9_/-]+") || !s.matches("[A-Za-z0-9_/-]+") || p.contains("..") || s.contains("..")) {
                response.sendError(400);
                return;
            }
            if ("_oai".equals(s)) response.setContentType("text/xml; charset=UTF-8");
            else if (s.endsWith("_api") || s.endsWith("_service")) response.setContentType("application/json; charset=UTF-8");
            String pg = "/WEB-INF/baru/modul/"+p+"/"+s+".jsp";
            %>
            <jsp:include page="<%=pg %>"></jsp:include>
            <%
        }catch(Exception e){
            ais.common.ErrorAuditUtil.record(e, "library-modern route adapter");
            %>
            <jsp:include page="/WEB-INF/baru/componen/tidak_ketemu_page.jsp"></jsp:include>
            <%
        }
    }
} else {
%>
<jsp:include page="/WEB-INF/baru/include/header.jsp"></jsp:include>

  <body>

    <main class="main" id="top">
      <div class="container-fluid">
        
        <jsp:include page="/WEB-INF/baru/modul/pustaka/landing_page.jsp"></jsp:include>
        
        <%
        String idBuku = request.getParameter("id");
        if (idBuku != null && !idBuku.trim().isEmpty()) {
        %>
            <jsp:include page="/WEB-INF/baru/modul/pustaka/_item_rinci.jsp">
                <jsp:param name="id" value="<%=idBuku%>" />
            </jsp:include>
        <%
        }
        %>
        <jsp:include page="/WEB-INF/baru/include/dialog-modal.jsp"></jsp:include>
      </div>
    </main>
    <jsp:include page="/WEB-INF/baru/include/foot.jsp"></jsp:include>
  <jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body>
</html>
<% } %>
