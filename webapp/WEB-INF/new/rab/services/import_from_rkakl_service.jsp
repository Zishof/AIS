<%-- Preflight baca-saja. Eksekusi lama yang DROP SCHEMA sengaja tidak diekspos. --%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="ais.common.newui.rab.NewUiImportRkaklController" %>
<%
NewUiImportRkaklController.handle(request, response, "import_from_rkakl");
%>
