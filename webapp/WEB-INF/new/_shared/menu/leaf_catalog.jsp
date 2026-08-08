<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="ais.common.newui.menu.NewUiHybridMenuNode" %>
<%!
private String nuiCatalogH(Object v){if(v==null)return "";return String.valueOf(v).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
%>
<%
NewUiHybridMenuNode nuiCatalogGroup=(NewUiHybridMenuNode)request.getAttribute("nui_catalog_group");
List<NewUiHybridMenuNode> nuiCatalogNodes=(List<NewUiHybridMenuNode>)request.getAttribute("nui_catalog_all_nodes");
if(nuiCatalogNodes==null)nuiCatalogNodes=new java.util.ArrayList<NewUiHybridMenuNode>();
%>
<section class="nui-leaf-catalog" data-group-menu-id="<%=nuiCatalogGroup.getMenuId()%>">
  <jsp:include page="/WEB-INF/new/_shared/menu/breadcrumb.jsp" flush="false"/>
  <header class="nui-module-hero">
    <div class="nui-catalog-hero-row"><div><h1><%=nuiCatalogH(nuiCatalogGroup.getLabel())%></h1><p><%=nuiCatalogNodes.size()%> child dan sub-child tersedia untuk role aktif.</p></div>
    <%if(nuiCatalogGroup.isClickable()){%><a class="nui-btn nui-btn-hero" target="nuiMainFrame" data-shell-url="<%=request.getContextPath()%>/new?menuId=<%=nuiCatalogGroup.getMenuId()%>" href="<%=request.getContextPath()%>/new?frame=1&amp;menuId=<%=nuiCatalogGroup.getMenuId()%>">Buka Ringkasan</a><%}%></div>
  </header>
  <div class="nui-card nui-catalog-toolbar">
    <input id="nuiLeafSearch" class="nui-input" type="search" aria-controls="nuiLeafCatalog" placeholder="Cari child dan sub-child dalam <%=nuiCatalogH(nuiCatalogGroup.getLabel())%>..." autocomplete="off">
    <select id="nuiLeafSort" class="nui-select" aria-label="Urutkan menu"><option value="order">Urutan Menu</option><option value="az">A–Z</option></select>
  </div>
  <%if(nuiCatalogNodes.isEmpty()){%><jsp:include page="/WEB-INF/new/_shared/menu/empty_catalog.jsp" flush="false"/><%}else{%>
  <div id="nuiLeafCatalog" class="nui-page-catalog">
    <%for(int nuiLeafI=0;nuiLeafI<nuiCatalogNodes.size();nuiLeafI++){NewUiHybridMenuNode nuiLeafNode=nuiCatalogNodes.get(nuiLeafI);request.setAttribute("nui_leaf_card",nuiLeafNode);%>
      <jsp:include page="/WEB-INF/new/_shared/menu/leaf_card.jsp" flush="false"/>
    <%}%>
  </div>
  <div id="nuiLeafNoResult" class="nui-card nui-empty nui-leaf-no-result" hidden><div class="nui-empty-icon">⌕</div>Tidak ada menu yang cocok dengan pencarian.</div>
  <%}%>
</section>
