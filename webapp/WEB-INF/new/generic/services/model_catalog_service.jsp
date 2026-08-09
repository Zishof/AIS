<%@ page import="java.util.List" %>
<%@ page import="ais.common.Common" %>
<%@ page import="ais.action.master.generic.v2.GenericCrudAutoDefinitionFactory" %>
<%@ page import="ais.action.master.generic.v2.GenericCrudJson" %>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%
response.setHeader("Cache-Control","no-store");
if(!Common.getApakahAdmin()){response.setStatus(403);out.print("{\"success\":false,\"code\":\"SUPER_ADMIN_REQUIRED\"}");return;}
List models=GenericCrudAutoDefinitionFactory.listAdministrativeModels();
out.print(GenericCrudJson.toJson(models));
%>
