<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.HashSet" %><%@ page import="java.util.List" %><%@ page import="java.util.Set" %>
<%@ page import="ais.common.newui.menu.NewUiHybridMenuNode" %>
<%!
private String nuiCatalogH(Object v){if(v==null)return "";return String.valueOf(v).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
%>
<%
NewUiHybridMenuNode nuiCatalogGroup=(NewUiHybridMenuNode)request.getAttribute("nui_catalog_group");
List<NewUiHybridMenuNode> nuiCatalogDirect=(List<NewUiHybridMenuNode>)request.getAttribute("nui_catalog_direct_leaves");
List<NewUiHybridMenuNode> nuiCatalogAll=(List<NewUiHybridMenuNode>)request.getAttribute("nui_catalog_all_leaves");
if(nuiCatalogDirect==null)nuiCatalogDirect=new java.util.ArrayList<NewUiHybridMenuNode>();if(nuiCatalogAll==null)nuiCatalogAll=new java.util.ArrayList<NewUiHybridMenuNode>();
Set<Long> nuiDirectIds=new HashSet<Long>();for(int nuiD=0;nuiD<nuiCatalogDirect.size();nuiD++)nuiDirectIds.add(nuiCatalogDirect.get(nuiD).getMenuId());
%>
<section class="nui-leaf-catalog" data-group-menu-id="<%=nuiCatalogGroup.getMenuId()%>">
  <jsp:include page="/WEB-INF/new/_shared/menu/breadcrumb.jsp" flush="false"/>
  <header class="nui-module-hero">
    <div class="nui-catalog-hero-row"><div><h1><%=nuiCatalogH(nuiCatalogGroup.getLabel())%></h1><p><%=nuiCatalogGroup.getLeafCount()%> menu tersedia untuk role aktif.</p></div>
    <%if(nuiCatalogGroup.isClickable()){%><a class="nui-btn nui-btn-hero" target="nuiMainFrame" data-shell-url="<%=request.getContextPath()%>/new?menuId=<%=nuiCatalogGroup.getMenuId()%>" href="<%=request.getContextPath()%>/new?frame=1&amp;menuId=<%=nuiCatalogGroup.getMenuId()%>">Buka Ringkasan</a><%}%></div>
  </header>
  <div class="nui-card nui-catalog-toolbar">
    <input id="nuiLeafSearch" class="nui-input" type="search" aria-controls="nuiLeafCatalog" placeholder="Cari seluruh root dan child dalam <%=nuiCatalogH(nuiCatalogGroup.getLabel())%>..." autocomplete="off">
    <select id="nuiLeafSort" class="nui-select" aria-label="Urutkan menu"><option value="order">Urutan Menu</option><option value="az">A–Z</option></select>
    <label class="nui-descendant-toggle"><input id="nuiIncludeDescendants" type="checkbox" checked="checked"> Tampilkan seluruh root dan child</label>
  </div>
  <%if(nuiCatalogAll.isEmpty()){%><jsp:include page="/WEB-INF/new/_shared/menu/empty_catalog.jsp" flush="false"/><%}else{%>
  <div id="nuiLeafCatalog" class="nui-page-catalog">
    <%for(int nuiLeafI=0;nuiLeafI<nuiCatalogAll.size();nuiLeafI++){NewUiHybridMenuNode nuiLeafNode=nuiCatalogAll.get(nuiLeafI);request.setAttribute("nui_leaf_card",nuiLeafNode);request.setAttribute("nui_leaf_direct",Boolean.valueOf(nuiDirectIds.contains(nuiLeafNode.getMenuId())));%>
      <jsp:include page="/WEB-INF/new/_shared/menu/leaf_card.jsp" flush="false"/>
    <%}%>
  </div>
  <div id="nuiLeafNoResult" class="nui-card nui-empty nui-leaf-no-result" hidden><div class="nui-empty-icon">⌕</div>Tidak ada menu yang cocok dengan pencarian.</div>
  <%}%>
</section>
