<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%><%@ page import="ais.action.master.library.modern.LibraryEngagementApi"%><%
response.setHeader("Cache-Control","no-store");response.setHeader("X-Content-Type-Options","nosniff");
try{out.print(LibraryEngagementApi.handle(request).toString());}catch(Exception e){ais.common.ErrorAuditUtil.record(e,"library engagement API");response.setStatus(500);out.print("{\"ok\":false,\"error\":\"Layanan belum dapat dimuat.\"}");}
%>
