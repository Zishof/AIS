<%@page import="ais.action.master.library.modern.LibraryOperationsApi"%><%@page import="org.json.JSONObject"%><%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%><%
response.setHeader("Cache-Control", "no-store");response.setHeader("X-Content-Type-Options", "nosniff");
try{out.print(LibraryOperationsApi.handle(request).toString());}catch(Exception e){ais.common.ErrorAuditUtil.record(e,"library operations API");response.setStatus(422);out.print(new JSONObject().put("ok",false).put("status","error").put("error",e.getMessage()==null?"Operasi perpustakaan gagal diproses.":e.getMessage()).toString());}
%>
