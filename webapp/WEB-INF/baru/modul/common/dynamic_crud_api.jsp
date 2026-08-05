<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.common.Common"%>
<%@page import="javax.servlet.http.HttpServletResponse"%>
<%@page import="ais.common.DynamicJspCrudGenerator"%>
<%@page import="org.json.JSONObject"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%
response.setCharacterEncoding("UTF-8");

Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json; charset=UTF-8");
    out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi berakhir.") + "\"}");
    return;
}

String action = request.getParameter("action") == null ? "" : request.getParameter("action");

response.setContentType("application/json; charset=UTF-8");
response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
JSONObject result = DynamicJspCrudGenerator.handleAjax(request);
out.print(result.toString());
%>
