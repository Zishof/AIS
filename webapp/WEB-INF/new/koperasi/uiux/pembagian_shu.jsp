<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="ais.common.Common" %>
<%@ page import="ais.database.model.Tbmuser" %>
<%@ page import="ais.common.newui.menu.NewUiHybridMenuAccessService" %>
<%@ page import="ais.common.newui.menu.NewUiHybridMenuNode" %>
<%@ page import="ais.common.newui.menu.NewUiHybridMenuSnapshot" %>
<%@ page import="ais.common.newui.menu.NewUiModuleFunctionService" %>
<%@ page import="ais.common.newui.menu.NewUiModuleFunctionService.Function" %>
<%
Tbmuser user=Common.getCurrentUser(request);
NewUiHybridMenuSnapshot snapshot=NewUiHybridMenuAccessService.getSnapshot(request);
List<NewUiHybridMenuNode> authorizedMenus=snapshot.getSearchableNodes();
Function function=NewUiModuleFunctionService.find("koperasi","shu",user,authorizedMenus);
if(function==null||!function.isOperationalRouteAvailable()){
    response.setStatus(403);
%><jsp:include page="/WEB-INF/new/_shared/ui/403.jsp"/><% return; }
request.setAttribute("nuiActiveModuleFunction",function);
%>
<jsp:include page="/WEB-INF/new/_shared/dashboard/pembagian_shu.jsp"/>
