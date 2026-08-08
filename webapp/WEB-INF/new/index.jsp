<%-- New UI shell. Sidebar/command/breadcrumb bersumber tunggal dari tabel Menu + RBAC. --%>
<%@ page import="java.io.File" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.List" %>
<%@ page import="ais.common.Common" %>
<%@ page import="ais.database.model.Tbmuser" %>
<%@ page import="ais.common.newui.NewUiMenuAccessService" %>
<%@ page import="ais.common.newui.NewUiMenuNode" %>
<%@ page import="ais.common.newui.NewUiRouteGuard" %>
<%@ page import="ais.common.newui.NewUiRouteRegistry" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%!
private boolean nuiSafePath(String s){return s!=null&&s.length()>0&&s.indexOf("..")<0&&s.indexOf('\\')<0&&!s.startsWith("/")&&s.matches("[A-Za-z0-9_\\-/]+");}
private String nuiShellH(Object v){if(v==null)return "";return String.valueOf(v).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
private boolean nuiExists(ServletContext app,String path){try{String real=app.getRealPath(path);if(real!=null)return new File(real).isFile();return app.getResource(path)!=null;}catch(Exception e){return false;}}
private Long nuiLong(String value){try{return value==null?null:Long.valueOf(value);}catch(Exception e){return null;}}
%>
<%
if(session.getAttribute("mytbmuser")==null){response.sendRedirect(request.getContextPath()+"/logoff");return;}
List<NewUiMenuNode> nuiMenuTree=NewUiMenuAccessService.getAccessibleTree(request);
request.setAttribute("nui_menu_tree",nuiMenuTree);

String requestedModule=request.getParameter("module");
String requestedPage=request.getParameter("page");
boolean service="1".equals(request.getParameter("service"));
boolean frame="1".equals(request.getParameter("frame"));
Long currentMenuId=nuiLong(request.getParameter("menu"));
NewUiMenuNode selected=currentMenuId==null?null:NewUiMenuAccessService.findById(request,currentMenuId);
if(currentMenuId!=null&&selected==null){
    if(service){response.setStatus(403);response.setContentType("application/json; charset=UTF-8");out.print("{\"ok\":false,\"code\":\"FORBIDDEN\"}");return;}
    response.setStatus(403);
    requestedModule="_shared";requestedPage="403";
}
if(selected!=null&&selected.isMappedToNewUi()){
    requestedModule=selected.getNewUiModule();requestedPage=selected.getNewUiPage();
}
String module=nuiSafePath(requestedModule)?requestedModule:"_shared";
String pageName=nuiSafePath(requestedPage)?requestedPage:"home";
if(selected==null&&"_shared".equals(module)&&!"403".equals(pageName))pageName="home";

request.setAttribute("nui_current_menu_id",currentMenuId);
request.setAttribute("nui_current_module",module);
request.setAttribute("nui_current_page",pageName);
request.setAttribute("nui_breadcrumb",currentMenuId==null?new java.util.ArrayList<NewUiMenuNode>():NewUiMenuAccessService.breadcrumb(request,currentMenuId));

String target="/WEB-INF/new/"+module+(service?"/services/":"/uiux/")+pageName+(service?"_service.jsp":".jsp");
if(module.startsWith("_shared"))target="/WEB-INF/new/_shared/"+(service?"services/":"ui/")+pageName+(service?"_service.jsp":".jsp");
int routeStatus=NewUiRouteGuard.evaluate(request,module,pageName);
if(!module.startsWith("_shared")&&routeStatus!=NewUiRouteRegistry.MAPPED_AND_AUTHORIZED){
    if(service){response.setStatus(routeStatus==NewUiRouteRegistry.MAPPED_BUT_FORBIDDEN?403:404);response.setContentType("application/json; charset=UTF-8");out.print(routeStatus==NewUiRouteRegistry.MAPPED_BUT_FORBIDDEN?"{\"ok\":false,\"code\":\"FORBIDDEN\"}":"{\"ok\":false,\"code\":\"ROUTE_NOT_MAPPED\"}");return;}
    response.setStatus(routeStatus==NewUiRouteRegistry.MAPPED_BUT_FORBIDDEN?403:404);
    target=routeStatus==NewUiRouteRegistry.MAPPED_BUT_FORBIDDEN?"/WEB-INF/new/_shared/ui/403.jsp":"/WEB-INF/new/_shared/ui/404.jsp";
}
if(!nuiExists(application,target)){
    if(service){response.setStatus(404);response.setContentType("application/json; charset=UTF-8");out.print("{\"ok\":false,\"code\":\"SERVICE_NOT_FOUND\"}");return;}
    target="/WEB-INF/new/_shared/ui/404.jsp";
}
if(service){
    String action=request.getParameter("action");if(action==null||action.trim().length()==0)action="meta";
    if(!NewUiRouteGuard.isActionAuthorized(request,module,pageName,action)){response.setStatus(403);response.setContentType("application/json; charset=UTF-8");out.print("{\"ok\":false,\"code\":\"ACTION_FORBIDDEN\"}");return;}
    response.setContentType("application/json; charset=UTF-8");request.getRequestDispatcher(target).forward(request,response);return;
}

Tbmuser current=null;try{current=Common.getCurrentUser(request);}catch(Exception ignored){}
String currentName=current==null?"Pengguna":current.getUserNama();if(currentName==null||currentName.trim().length()==0)currentName=current==null?"Pengguna":current.getUserId();if(currentName==null||currentName.trim().length()==0)currentName="Pengguna";
String newUrl=request.getContextPath()+"/new";
String frameSrc;
if(selected!=null&&!selected.isMappedToNewUi()&&selected.getLegacyUrl()!=null){frameSrc=selected.getLegacyUrl();}
else{frameSrc=newUrl+"?frame=1&module="+URLEncoder.encode(module,"UTF-8")+"&page="+URLEncoder.encode(pageName,"UTF-8")+(currentMenuId==null?"":"&menu="+currentMenuId);}
if(frame){
%>
<!doctype html><html lang="id"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex,nofollow"><title>eCampus &amp; eSchool</title><style><%@ include file="/WEB-INF/new/_shared/assets/new-ui.css" %></style></head>
<body class="nui-frame-body"><div class="nui-frame-content"><%pageContext.include(target,true);%></div><div id="nuiLoading" class="nui-loading"></div><div id="nuiToast" class="nui-toast"></div><script><%@ include file="/WEB-INF/new/_shared/assets/new-ui.js" %></script></body></html>
<% return; } %>
<!doctype html>
<html lang="id">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex,nofollow"><title>eCampus &amp; eSchool — Modern UI</title><style><%@ include file="/WEB-INF/new/_shared/assets/new-ui.css" %></style></head>
<body>
<div id="nuiLoading" class="nui-loading"></div>
<div class="nui-app">
  <aside id="nuiSidebar" class="nui-sidebar">
    <div class="nui-brand"><div class="nui-logo">◆</div><div><strong>eCampus &amp; eSchool</strong><small>Enterprise Education</small></div></div>
    <nav class="nui-nav" aria-label="Menu utama"><jsp:include page="/WEB-INF/new/_shared/ui/sidebar.jsp" flush="false"/></nav>
    <div class="nui-sidebar-foot">● Online &nbsp; • &nbsp; Menu sesuai peran aktif</div>
  </aside>
  <main class="nui-main">
    <header class="nui-topbar">
      <button id="nuiMenuToggle" class="nui-menu-toggle" aria-label="Buka menu">☰</button>
      <div class="nui-global-search"><input id="nuiGlobalSearch" placeholder="Cari menu yang boleh diakses..." aria-label="Pencarian menu"><span class="nui-kbd">Ctrl + K</span></div>
      <div class="nui-top-actions"><jsp:include page="/WEB-INF/new/_shared/ui/role_switcher.jsp" flush="false"/><button class="nui-icon-btn" aria-label="Notifikasi">♢<span class="nui-badge">0</span></button><button class="nui-icon-btn" aria-label="Pesan">✉</button><div class="nui-user"><div class="nui-avatar"><%=nuiShellH(currentName.substring(0,1).toUpperCase())%></div><div><strong><%=nuiShellH(currentName)%></strong><small>Pengguna aktif</small></div></div></div>
    </header>
    <iframe id="nuiMainFrame" name="nuiMainFrame" class="nui-main-frame" src="<%=nuiShellH(frameSrc)%>" title="Konten utama eCampus"></iframe>
    <footer class="nui-footer"><span>© 2026 eCampus &amp; eSchool</span><span>Menu database • RBAC fail-closed</span></footer>
  </main>
</div>
<div id="nuiToast" class="nui-toast"></div>
<div id="nuiCommand" class="nui-command" role="dialog" aria-modal="true" aria-label="Cari menu"><div class="nui-command-box"><input id="nuiCommandInput" class="nui-input" placeholder="Ketik nama menu..."><div class="nui-command-results"><jsp:include page="/WEB-INF/new/_shared/ui/command_palette.jsp" flush="false"/></div></div></div>
<script><%@ include file="/WEB-INF/new/_shared/assets/new-ui.js" %></script>
</body></html>
