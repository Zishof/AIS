<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    Boolean loggedLanding = (Boolean) request.getAttribute("DMS_LOGGED_IN");
    boolean dmsLoggedInLanding = loggedLanding != null && loggedLanding.booleanValue();
%>
<jsp:include page="/WEB-INF/baru/modul/dms/_header_dms.jsp"></jsp:include>
<% if (dmsLoggedInLanding) { %>
    <jsp:include page="/WEB-INF/baru/modul/dms/setelah_login.jsp"></jsp:include>
<% } else { %>
    <jsp:include page="/WEB-INF/baru/modul/dms/sebelum_login.jsp"></jsp:include>
<% } %>
<jsp:include page="/WEB-INF/baru/modul/dms/konten.jsp"></jsp:include>
<jsp:include page="/WEB-INF/baru/modul/dms/_footer_dms.jsp"></jsp:include>
