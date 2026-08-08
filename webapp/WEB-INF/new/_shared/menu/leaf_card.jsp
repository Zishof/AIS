<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="ais.common.newui.menu.NewUiHybridMenuNode" %><%@ page import="ais.common.newui.menu.NewUiHybridMenuRouteRegistry" %>
<%!
private String nuiLeafH(Object v){if(v==null)return "";return String.valueOf(v).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
%>
<%
NewUiHybridMenuNode nuiLeaf=(NewUiHybridMenuNode)request.getAttribute("nui_leaf_card");Boolean nuiLeafDirect=(Boolean)request.getAttribute("nui_leaf_direct");
String nuiLeafNewUrl=request.getContextPath()+"/new";boolean nuiLeafRedirect=NewUiHybridMenuRouteRegistry.LEGACY_REDIRECT.equals(nuiLeaf.getRouteStatus());
%>
<a class="nui-page-link nui-leaf-card<%=Boolean.TRUE.equals(nuiLeafDirect)?" nui-leaf-direct":" nui-leaf-descendant"%>" data-direct="<%=Boolean.TRUE.equals(nuiLeafDirect)?"1":"0"%>" data-search="<%=nuiLeafH(nuiLeaf.getSearchText())%>" data-label="<%=nuiLeafH(nuiLeaf.getLabel())%>" data-order="<%=nuiLeaf.getNomorUrut()==null?0:nuiLeaf.getNomorUrut()%>" data-path="<%=nuiLeafH(nuiLeaf.getParentPath())%>" <%=nuiLeafRedirect?"target=\"_blank\"":"target=\"nuiMainFrame\""%> <%=nuiLeafRedirect?"":"data-shell-url=\""+nuiLeafNewUrl+"?menuId="+nuiLeaf.getMenuId()+"\""%> href="<%=nuiLeafRedirect?nuiLeafH(nuiLeaf.getResolvedUrl()):nuiLeafNewUrl+"?frame=1&amp;menuId="+nuiLeaf.getMenuId()%>">
  <span class="nui-leaf-icon"><%=nuiLeaf.getIcon()==null||nuiLeaf.getIcon().trim().length()==0?"◇":nuiLeafH(nuiLeaf.getIcon())%></span>
  <strong><%=nuiLeafH(nuiLeaf.getLabel())%></strong>
  <span class="nui-leaf-path"><%=nuiLeafH(nuiLeaf.getParentPath())%></span>
  <span class="nui-route-badge <%=NewUiHybridMenuRouteRegistry.NEW_UI.equals(nuiLeaf.getRouteStatus())?"new":"legacy"%>"><%=NewUiHybridMenuRouteRegistry.NEW_UI.equals(nuiLeaf.getRouteStatus())?"New UI":"Legacy aman"%></span>
</a>
