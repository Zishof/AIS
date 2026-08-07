<%-- Adapter HTTP tipis untuk pergantian role aktif New UI.
     Endpoint: /new?service=1&module=_shared&page=role_switch (POST + CSRF).
     Semua logika di NewUiRoleSwitcherService. --%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="ais.common.newui.NewUiCsrfUtil" %>
<%@ page import="ais.common.newui.NewUiRoleSwitcherService" %>
<%
    response.setContentType("application/json; charset=UTF-8");
    if (!"POST".equalsIgnoreCase(request.getMethod())) {
        response.setStatus(405);
        out.print("{\"ok\":false,\"code\":\"METHOD_NOT_ALLOWED\"}");
        return;
    }
    if (session.getAttribute("mytbmuser") == null) {
        response.setStatus(401);
        out.print("{\"ok\":false,\"code\":\"UNAUTHENTICATED\"}");
        return;
    }
    if (!NewUiCsrfUtil.isValid(request)) {
        response.setStatus(403);
        out.print("{\"ok\":false,\"code\":\"CSRF_INVALID\"}");
        return;
    }
    String roleId = request.getParameter("roleId");
    boolean ok = NewUiRoleSwitcherService.switchRole(request, roleId);
    if (ok) {
        out.print("{\"ok\":true,\"code\":\"OK\",\"message\":\"Peran aktif berhasil diganti.\"}");
    } else {
        response.setStatus(400);
        out.print("{\"ok\":false,\"code\":\"SWITCH_REJECTED\",\"message\":\"Peran tidak dapat digunakan.\"}");
    }
%>
