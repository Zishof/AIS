<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %><%@ page import="ais.common.newui.menu.NewUiHybridMenuNode" %><%@ page import="ais.common.newui.menu.NewUiHybridMenuSnapshot" %><%@ page import="ais.common.newui.menu.NewUiHybridMenuRouteRegistry" %>
<%!
private String nuiCmdH(Object v){if(v==null)return "";return String.valueOf(v).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
%>
<%
NewUiHybridMenuSnapshot nuiCmdSnapshot=(NewUiHybridMenuSnapshot)request.getAttribute("newUiHybridMenuSnapshot");
List<NewUiHybridMenuNode> nuiCmdNodes=nuiCmdSnapshot==null?null:nuiCmdSnapshot.getSearchableNodes();String nuiCmdNew=request.getContextPath()+"/new";
if(nuiCmdNodes!=null){for(int nuiCmdI=0;nuiCmdI<nuiCmdNodes.size();nuiCmdI++){NewUiHybridMenuNode nuiCmdNode=nuiCmdNodes.get(nuiCmdI);String nuiCmdParam=nuiCmdNode.isBranch()?"groupMenuId":"menuId";boolean nuiCmdRedirect=nuiCmdNode.isLeaf()&&NewUiHybridMenuRouteRegistry.LEGACY_REDIRECT.equals(nuiCmdNode.getRouteStatus());%>
<a target="<%=nuiCmdRedirect?"_blank":"nuiMainFrame"%>" data-search="<%=nuiCmdH(nuiCmdNode.getSearchText())%>" <%=nuiCmdRedirect?"":"data-shell-url=\""+nuiCmdNew+"?"+nuiCmdParam+"="+nuiCmdNode.getMenuId()+"\""%> href="<%=nuiCmdRedirect?nuiCmdH(nuiCmdNode.getResolvedUrl()):nuiCmdNew+"?frame=1&amp;"+nuiCmdParam+"="+nuiCmdNode.getMenuId()%>"><strong><%=nuiCmdH(nuiCmdNode.getLabel())%></strong><small><%=nuiCmdH(nuiCmdNode.getFullPath())%> · <%=nuiCmdNode.isBranch()?"Kelompok":"Menu"%></small></a>
<%}}%>
