<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="ais.common.newui.menu.NewUiHybridMenuNode" %><%@ page import="ais.common.newui.menu.NewUiHybridMenuRouteRegistry" %><%@ page import="ais.common.newui.menu.NewUiIconUtil" %>
<%!
private String nuiLeafH(Object v){if(v==null)return "";return String.valueOf(v).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
%>
<%
NewUiHybridMenuNode nuiLeaf=(NewUiHybridMenuNode)request.getAttribute("nui_leaf_card");
String nuiLeafNewUrl=request.getContextPath()+"/new";boolean nuiLeafBranch=nuiLeaf.isBranch();
String nuiLeafParameter=nuiLeafBranch?"groupMenuId":"menuId";String nuiLeafShellUrl=nuiLeafNewUrl+"?"+nuiLeafParameter+"="+nuiLeaf.getMenuId();
%>
<a class="nui-page-link nui-leaf-card nui-leaf-direct<%=nuiLeafBranch?" nui-menu-branch-card":""%>" data-direct="1" data-search="<%=nuiLeafH(nuiLeaf.getSearchText())%>" data-label="<%=nuiLeafH(nuiLeaf.getLabel())%>" data-order="<%=nuiLeaf.getNomorUrut()==null?0:nuiLeaf.getNomorUrut()%>" data-path="<%=nuiLeafH(nuiLeaf.getParentPath())%>" target="nuiMainFrame" data-shell-url="<%=nuiLeafShellUrl%>" href="<%=nuiLeafNewUrl+"?frame=1&amp;"+nuiLeafParameter+"="+nuiLeaf.getMenuId()%>">
  <span class="nui-leaf-icon"><i class="<%=nuiLeafH(NewUiIconUtil.classes(nuiLeaf.getIcon(),nuiLeafBranch))%> nui-fa-icon" aria-hidden="true"></i></span>
  <strong><%=nuiLeafH(nuiLeaf.getLabel())%></strong>
  <span class="nui-leaf-path"><%=nuiLeafH(nuiLeaf.getParentPath())%></span>
  <span class="nui-route-badge <%=nuiLeafBranch?"group":NewUiHybridMenuRouteRegistry.NEW_UI.equals(nuiLeaf.getRouteStatus())?"new":"pending"%>"><%=nuiLeafBranch?"Kelompok · "+nuiLeaf.getLeafCount()+" menu":NewUiHybridMenuRouteRegistry.NEW_UI.equals(nuiLeaf.getRouteStatus())?"New UI":"New UI belum dipetakan"%></span>
</a>
