<%@ page import="ais.common.Common" %>
<%@ page import="ais.action.master.generic.v2.GenericCrudDefinition" %>
<%@ page import="ais.action.master.generic.v2.GenericCrudDefinitionRegistry" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
if(!Common.getApakahAdmin()){response.setStatus(403);out.print("Akses Model CRUD hanya untuk Super Admin.");return;}
String mappedEntity=request.getParameter("entity");
GenericCrudDefinition definition=GenericCrudDefinitionRegistry.tryAdministrativeRegister("generic","model_crud",mappedEntity);
if(definition==null){response.setStatus(404);out.print("Model tidak ditemukan, tidak mapped, abstract, atau termasuk kategori sensitif.");return;}
request.setAttribute("genericCrudEntityKey",definition.getEntityKey());
request.setAttribute("genericCrudModuleKey","generic");
request.setAttribute("genericCrudPageKey","model_crud");
%>
<jsp:include page="/WEB-INF/new/_shared/generic-crud/ui/crud_page.jsp" />
