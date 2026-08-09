<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%
response.setStatus(501);
response.setHeader("Cache-Control","no-store");
response.setHeader("X-Content-Type-Options","nosniff");
out.print("{\"ok\":false,\"code\":\"NATIVE_ADAPTER_NOT_CONFIGURED\",\"message\":\"Java service New UI untuk menu ini belum dikonfigurasi.\"}");
%>
