<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%><%@ page import="ais.action.master.library.modern.LibraryWorkspaceApi"%><%
response.setHeader("Cache-Control", "no-store");
response.setHeader("X-Content-Type-Options", "nosniff");
try {
    out.print(LibraryWorkspaceApi.handle(request).toString());
} catch (Exception error) {
    ais.common.ErrorAuditUtil.record(error, "library typed workspace API");
    response.setStatus(500);
    out.print("{\"ok\":false,\"error\":\"Data perpustakaan belum dapat dimuat.\"}");
}
%>
