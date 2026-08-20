<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.action.master.library.modern.LibraryVisitKioskApi" %>
<%
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("X-Content-Type-Options", "nosniff");
    out.print(LibraryVisitKioskApi.handle(request).toString());
%>
