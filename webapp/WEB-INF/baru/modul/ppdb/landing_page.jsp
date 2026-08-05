<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.sekolah.CalonSiswa"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="javax.servlet.http.Cookie"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String authAction = request.getParameter("auth_action");
    
    if ("login".equals(authAction)) {
        String idSiswaStr = request.getParameter("siswa_id");
        if (idSiswaStr != null && !idSiswaStr.trim().isEmpty()) {
            try {
                CalonSiswa siswaLogin = (CalonSiswa) GeneralValueObject.ambilData(CalonSiswa.class, idSiswaStr.trim(), true);
                if (siswaLogin != null) {
                    Common.setLogin(request, response, siswaLogin);
                    out.print("{\"status\":\"success\"}");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/ppdb/landing_page.jsp:21");
            }
        }
        out.print("{\"status\":\"error\"}");
        return;
        
    } else if ("logout".equals(authAction)) {
        try {
            Common.setLogout(); 
        } catch(Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/ppdb/landing_page.jsp:31");
        }
        
        try {
            request.getSession().invalidate();
            Cookie cookieSiswa = new Cookie("calonSiswa", Common.desEncrypter.get().encrypt("-1"));
            cookieSiswa.setMaxAge(15552000);
            response.addCookie(cookieSiswa);
        } catch(Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/ppdb/landing_page.jsp:40");
        }
        
        out.print("<script>window.location.replace('" + Common.ROOT + "/ppdb');</script>");
        return;
    }
    
    CalonSiswa siswaLogged = Common.isLoginCalonSiswa(request);
%>

<jsp:include page="/WEB-INF/baru/modul/ppdb/_header_ppdb.jsp"></jsp:include>

<% if (siswaLogged != null) { %>
    <jsp:include page="/WEB-INF/baru/modul/ppdb/_sukses_login.jsp"></jsp:include>
<% } else { %>
    <jsp:include page="/WEB-INF/baru/modul/ppdb/_sebelum_login.jsp"></jsp:include>
<% } %>

<jsp:include page="/WEB-INF/baru/modul/ppdb/_footer_ppdb.jsp"></jsp:include>