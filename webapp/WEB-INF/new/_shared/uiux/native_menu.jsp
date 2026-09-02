<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="ais.common.newui.menu.NewUiHybridMenuNode" %>
<%@ page import="ais.common.newui.menu.NewUiUnavailableRouteRegistry" %>
<%!
private String nuiNativeH(Object value){if(value==null)return "";return String.valueOf(value).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
%>
<%
NewUiHybridMenuNode nuiNativeNode=(NewUiHybridMenuNode)request.getAttribute("nui_selected_menu_node");
String nuiNativeTitle=nuiNativeNode==null?"Menu New UI":nuiNativeNode.getLabel();
String nuiNativePath=nuiNativeNode==null?"":nuiNativeNode.getFullPath();
NewUiUnavailableRouteRegistry.Entry nuiUnavailable=
        (NewUiUnavailableRouteRegistry.Entry)request.getAttribute("nui_unavailable_route");
%>
<section class="nui-page nui-native-menu">
  <header class="nui-page-head">
    <div><div class="nui-breadcrumb">Beranda / <%=nuiNativeH(nuiNativePath)%></div><h1 class="nui-page-title"><%=nuiNativeH(nuiNativeTitle)%></h1><p class="nui-page-desc">Halaman mandiri New UI. Akses tetap mengikuti menu dan hak READ role aktif.</p></div>
  </header>
  <% if(nuiUnavailable!=null){ %>
  <article class="nui-card">
    <div class="nui-card-pad"><div class="nui-empty"><div class="nui-empty-icon"><i class="fa-solid fa-triangle-exclamation nui-fa-icon" aria-hidden="true"></i></div>
      <strong><%=nuiNativeH(nuiUnavailable.getTitle())%></strong>
      <p class="nui-page-desc"><%=nuiNativeH(nuiUnavailable.getMessage())%></p>
      <p class="nui-page-desc"><small>Kode: <%=nuiNativeH(nuiUnavailable.getCode())%></small></p>
    </div></div>
  </article>
  <% }else{ %>
  <article class="nui-card">
    <div class="nui-toolbar"><input class="nui-input" type="search" placeholder="Cari data dalam <%=nuiNativeH(nuiNativeTitle)%>…" aria-label="Cari data"><button class="nui-btn" type="button" data-nui-action data-message="Filter New UI siap dihubungkan ke Java service.">Filter</button></div>
    <div class="nui-card-pad"><div class="nui-empty"><div class="nui-empty-icon"><i class="fa-solid fa-layer-group nui-fa-icon" aria-hidden="true"></i></div><strong>Adapter New UI sedang dikonfigurasi</strong><p class="nui-page-desc">Menu tetap berada di shell New UI dan tidak membuka iframe, redirect, atau tampilan aplikasi lain.</p></div></div>
  </article>
  <% } %>
</section>
