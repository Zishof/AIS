<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%-- Redirect ke hub page Antar Jemput yang lengkap --%>
<%
    String ctx = request.getContextPath();
    response.sendRedirect(ctx + "/baru?p=antarjemput&_mob=" + request.getParameter("_mob"));
%>
