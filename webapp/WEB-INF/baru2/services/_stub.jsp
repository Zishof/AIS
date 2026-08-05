<%@ page contentType="application/json;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="org.json.JSONObject, org.json.JSONArray" %>
<%--
  _stub.jsp — Fallback global untuk semua service yang belum diimplementasikan.
  Dikembalikan ketika file spesifik _{svc}.jsp belum ada.
  UI tidak akan error, melainkan menampilkan data kosong atau pesan stub.
--%>
<%
String svc = request.getParameter("svc");
String method = request.getMethod();
response.setContentType("application/json;charset=UTF-8");
JSONObject res = new JSONObject();

if ("POST".equalsIgnoreCase(method)) {
    res.put("ok", false);
    res.put("msg", "Layanan '" + (svc != null ? svc : "?") + "' belum diimplementasikan. Hubungi pengembang backend.");
} else {
    /* GET — kembalikan data kosong agar tabel tidak error */
    boolean isRef = svc != null && svc.startsWith("ref_");
    res.put("total", 0);
    res.put("data", new JSONArray());
    if (!isRef) {
        res.put("_stub", true);
        res.put("_msg", "Stub: layanan '" + (svc != null ? svc : "?") + "' belum diimplementasikan.");
    }
}
out.clear();
out.print(res.toString());
%>
