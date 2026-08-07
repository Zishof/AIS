<%@ page import="ais.common.Common" %>
<%@ page import="ais.action.master.generic.v2.GenericCrudDefinition" %>
<%@ page import="ais.action.master.generic.v2.GenericCrudDefinitionRegistry" %>
<%@ page import="ais.action.master.generic.v2.GenericCrudHttpController" %>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%
if(!Common.getApakahAdmin()){response.setStatus(403);out.print("{\"success\":false,\"code\":\"SUPER_ADMIN_REQUIRED\"}");return;}
GenericCrudDefinition definition=GenericCrudDefinitionRegistry.tryAdministrativeRegister("generic","model_crud",request.getParameter("entity"));
if(definition==null){response.setStatus(404);out.print("{\"success\":false,\"code\":\"MODEL_NOT_ALLOWED\"}");return;}
request.setAttribute("genericCrudEntityKey",definition.getEntityKey());
request.setAttribute("genericCrudModuleKey","generic");
request.setAttribute("genericCrudPageKey","model_crud");
GenericCrudHttpController.handle(request,response);
%>
