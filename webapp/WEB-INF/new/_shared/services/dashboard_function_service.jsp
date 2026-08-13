<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.menu.NewUiPembagianShuController" %><%
if("koperasi".equalsIgnoreCase(request.getParameter("dashboard"))&&"shu".equalsIgnoreCase(request.getParameter("dashboardView"))){NewUiPembagianShuController.handle(request,response);return;}
response.setStatus(404);out.print("{\"ok\":false,\"code\":\"DASHBOARD_FUNCTION_NOT_FOUND\"}");
%>
