<%@page import="ais.action.master.library.modern.LibraryOaiPmhService"%><%@ page language="java" contentType="text/xml; charset=UTF-8" pageEncoding="UTF-8"%><%
response.setHeader("X-Content-Type-Options", "nosniff");
response.setHeader("Cache-Control", "public, max-age=60");
LibraryOaiPmhService.Result result = LibraryOaiPmhService.handle(request);
response.setStatus(result.getStatus());
out.print(result.getBody());
%>
