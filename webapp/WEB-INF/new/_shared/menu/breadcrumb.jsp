<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %><%@ page import="ais.common.newui.menu.NewUiHybridMenuNode" %>
<%!
private String nuiCrumbH(Object v){if(v==null)return "";return String.valueOf(v).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
%>
<%List<NewUiHybridMenuNode> nuiCrumbs=(List<NewUiHybridMenuNode>)request.getAttribute("nui_breadcrumb");%>
<div class="nui-breadcrumb" aria-label="Breadcrumb"><span>Beranda</span><%if(nuiCrumbs!=null){for(int nuiCrumbI=0;nuiCrumbI<nuiCrumbs.size();nuiCrumbI++){%><span class="nui-breadcrumb-sep">/</span><span><%=nuiCrumbH(nuiCrumbs.get(nuiCrumbI).getLabel())%></span><%}}%></div>
