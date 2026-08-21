<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%><%@ page import="ais.action.master.library.modern.LibraryCatalogApi"%><%@ page import="org.json.JSONObject"%><%
response.setHeader("Cache-Control", "no-store");
response.setHeader("X-Content-Type-Options", "nosniff");
try {
    JSONObject result = LibraryCatalogApi.handle(request);
    if (!result.optBoolean("ok", true) && result.optString("error").startsWith("Terlalu banyak")) response.setStatus(429);
    out.print(result.toString());
} catch (Exception error) {
    ais.common.ErrorAuditUtil.record(error, "library typed catalog API");
    response.setStatus(500);
    out.print("{\"ok\":false,\"error\":\"Katalog belum dapat dimuat. Silakan coba kembali.\"}");
}
%>
