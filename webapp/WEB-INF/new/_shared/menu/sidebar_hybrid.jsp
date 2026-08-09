<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="ais.common.newui.menu.NewUiHybridMenuNode" %>
<%@ page import="ais.common.newui.menu.NewUiHybridMenuSnapshot" %>
<%
NewUiHybridMenuSnapshot nuiSideSnapshot=(NewUiHybridMenuSnapshot)request.getAttribute("newUiHybridMenuSnapshot");
List<NewUiHybridMenuNode> nuiSideBranches=nuiSideSnapshot==null?null:nuiSideSnapshot.getSidebarBranches();
String nuiSideNewUrl=request.getContextPath()+"/new";
boolean nuiDashboardMenu="1".equals(request.getParameter("dashboardMenu"));boolean nuiDashboardActive=request.getParameter("dashboard")!=null||nuiDashboardMenu;
%>
<a class="nui-nav-home<%=request.getAttribute("nui_current_group_id")==null&&request.getAttribute("nui_current_menu_id")==null&&!nuiDashboardActive?" active":""%>" target="nuiMainFrame" data-shell-url="<%=nuiSideNewUrl%>" href="<%=nuiSideNewUrl%>?frame=1"><span class="nui-nav-icon"><i class="fa-solid fa-house nui-fa-icon" aria-hidden="true"></i></span><span>Beranda</span></a>
<a class="nui-nav-home<%=nuiDashboardActive?" active":""%>" target="nuiMainFrame" data-shell-url="<%=nuiSideNewUrl%>?dashboardMenu=1" href="<%=nuiSideNewUrl%>?frame=1&amp;dashboardMenu=1"><span class="nui-nav-icon"><i class="fa-solid fa-gauge-high nui-fa-icon" aria-hidden="true"></i></span><span>Dashboard</span></a>
<%if(nuiSideBranches==null||nuiSideBranches.isEmpty()){%>
<div class="nui-nav-empty">Tidak ada branch atau leaf dengan izin READ untuk peran aktif.</div>
<%}else{for(int nuiSideI=0;nuiSideI<nuiSideBranches.size();nuiSideI++){
request.setAttribute("nui_sidebar_branch",nuiSideBranches.get(nuiSideI));request.setAttribute("nui_sidebar_depth",Integer.valueOf(0));%>
<jsp:include page="/WEB-INF/new/_shared/menu/sidebar_branch.jsp" flush="false"/>
<%}}%>
