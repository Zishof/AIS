<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.ArrayList" %><%@ page import="java.util.List" %>
<%@ page import="ais.common.Common" %><%@ page import="ais.database.model.Tbmrole" %><%@ page import="ais.database.model.Tbmuser" %>
<%@ page import="ais.common.newui.menu.NewUiHybridMenuNode" %><%@ page import="ais.common.newui.menu.NewUiHybridMenuSnapshot" %>
<%@ page import="ais.common.newui.menu.NewUiModuleShortcut" %><%@ page import="ais.common.newui.menu.NewUiModuleShortcutService" %>
<%!
private String nuiModuleH(Object v){if(v==null)return "";return String.valueOf(v).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
%>
<%
NewUiHybridMenuSnapshot nuiModuleSnapshot=(NewUiHybridMenuSnapshot)request.getAttribute("newUiHybridMenuSnapshot");
Tbmuser nuiModuleUser=null;try{nuiModuleUser=Common.getCurrentUser(request);}catch(Exception ignored){}
Tbmrole nuiModuleRole=null;try{nuiModuleRole=nuiModuleUser==null?null:nuiModuleUser.hakAkses();}catch(Exception ignored){}
List<NewUiModuleShortcut> nuiModules=NewUiModuleShortcutService.build(nuiModuleRole,nuiModuleSnapshot);
String nuiModuleNewUrl=request.getContextPath()+"/new";
%>
<section class="nui-leaf-catalog nui-module-catalog">
  <header class="nui-module-hero">
    <div class="nui-catalog-hero-row"><div><h1>Aplikasi &amp; Modul</h1><p><%=nuiModules.size()%> modul aktif dan dapat diakses oleh role saat ini.</p></div></div>
  </header>
  <div class="nui-card nui-catalog-toolbar">
    <input id="nuiLeafSearch" class="nui-input" type="search" aria-controls="nuiLeafCatalog" placeholder="Cari aplikasi atau modul..." autocomplete="off">
    <select id="nuiLeafSort" class="nui-select" aria-label="Urutkan modul"><option value="order">Urutan Modul</option><option value="az">A–Z</option></select>
  </div>
  <%if(nuiModules.isEmpty()){%>
  <div class="nui-card nui-empty nui-empty-catalog"><div class="nui-empty-icon"><i class="fa-solid fa-shield-halved nui-fa-icon" aria-hidden="true"></i></div><strong>Belum ada modul yang dapat ditampilkan</strong><p>Modul hanya muncul bila flag role legacy aktif dan menu terkait mempunyai assignment serta izin READ.</p></div>
  <%}else{%>
  <div id="nuiLeafCatalog" class="nui-page-catalog">
    <%for(int nuiModuleI=0;nuiModuleI<nuiModules.size();nuiModuleI++){
      NewUiModuleShortcut nuiModule=nuiModules.get(nuiModuleI);NewUiHybridMenuNode nuiTarget=nuiModule.getTarget();
      boolean nuiTargetBranch=nuiTarget.isBranch();String nuiTargetParam=nuiTargetBranch?"groupMenuId":"menuId";
      String nuiTargetShell=nuiModuleNewUrl+"?"+nuiTargetParam+"="+nuiTarget.getMenuId();
    %>
    <a class="nui-page-link nui-leaf-card nui-module-shortcut-card<%=nuiTargetBranch?" nui-menu-branch-card":""%>" data-direct="1" data-search="<%=nuiModuleH(nuiModule.getSearchText())%>" data-label="<%=nuiModuleH(nuiModule.getLabel())%>" data-order="<%=nuiModule.getOrder()%>" data-path="Aplikasi &amp; Modul" target="nuiMainFrame" data-shell-url="<%=nuiTargetShell%>" href="<%=nuiModuleNewUrl+"?frame=1&amp;"+nuiTargetParam+"="+nuiTarget.getMenuId()%>">
      <span class="nui-leaf-icon"><i class="<%=nuiModuleH(nuiModule.getIcon())%> nui-fa-icon" aria-hidden="true"></i></span>
      <strong><%=nuiModuleH(nuiModule.getLabel())%></strong>
      <span class="nui-leaf-path"><%=nuiModuleH(nuiModule.getDescription())%></span>
      <span class="nui-route-badge <%=nuiTargetBranch?"group":"new"%>"><%=nuiTargetBranch?nuiTarget.getLeafCount()+" menu tersedia":"Buka modul"%></span>
    </a>
    <%}%>
  </div>
  <div id="nuiLeafNoResult" class="nui-card nui-empty nui-leaf-no-result" hidden><div class="nui-empty-icon"><i class="fa-solid fa-magnifying-glass nui-fa-icon" aria-hidden="true"></i></div>Tidak ada aplikasi atau modul yang cocok dengan pencarian.</div>
  <%}%>
</section>
