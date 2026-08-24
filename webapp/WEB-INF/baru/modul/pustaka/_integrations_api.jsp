<%@page import="ais.action.master.library.modern.LibraryIntegrationGateway"%><%@page import="org.json.JSONObject"%><%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%><%
response.setHeader("Cache-Control", "no-store");
response.setHeader("X-Content-Type-Options", "nosniff");
try {
    out.print(LibraryIntegrationGateway.handle(request).toString());
} catch (Exception e) {
    ais.common.ErrorAuditUtil.record(e, "library integration gateway");
    response.setStatus(500);
    out.print(new JSONObject().put("ok", false).put("status", "error")
        .put("error", "Integrasi gagal diproses.").toString());
}
%>
