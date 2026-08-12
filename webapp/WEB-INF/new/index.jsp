<%-- Hybrid Menu V2 shell: assignment job_has_menu + RolePrivilage, branch di sidebar, leaf di katalog. --%>
<%@ page import="java.io.File" %><%@ page import="java.util.ArrayList" %><%@ page import="java.util.List" %>
<%@ page import="ais.common.Common" %><%@ page import="ais.database.model.Tbmuser" %>
<%@ page import="ais.common.newui.menu.NewUiHybridMenuAccessService" %><%@ page import="ais.common.newui.menu.NewUiHybridMenuCatalogService" %>
<%@ page import="ais.common.newui.menu.NewUiHybridMenuNode" %><%@ page import="ais.common.newui.menu.NewUiHybridMenuRouteGuard" %>
<%@ page import="ais.common.newui.menu.NewUiHybridMenuRouteRegistry" %><%@ page import="ais.common.newui.menu.NewUiHybridMenuSnapshot" %>
<%@ page import="ais.common.newui.menu.NewUiNativeJspResolver" %>
<%@ page import="ais.common.newui.menu.NewUiNativeSubrouteRegistry" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%!
private String nuiShellH(Object v){if(v==null)return "";return String.valueOf(v).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
private boolean nuiExists(ServletContext app,String path){try{String real=app.getRealPath(path);if(real!=null)return new File(real).isFile();return app.getResource(path)!=null;}catch(Exception e){return false;}}
private Long nuiLong(String value){try{return value==null||value.trim().length()==0?null:Long.valueOf(value);}catch(Exception e){return null;}}
private boolean nuiEqual(String a,String b){return a==null?b==null:a.equals(b);}
%>
<%
if(session.getAttribute("mytbmuser")==null){response.sendRedirect(request.getContextPath()+"/logoff");return;}

boolean service="1".equals(request.getParameter("service"));boolean frame="1".equals(request.getParameter("frame"));
String requestedModule=request.getParameter("module");String requestedPage=request.getParameter("page");
String requestedDashboard=request.getParameter("dashboard");
String requestedDashboardView=request.getParameter("dashboardView");
boolean requestedDashboardMenu="1".equals(request.getParameter("dashboardMenu"));
if(service&&"_shared".equals(requestedModule)&&"role_switch".equals(requestedPage)){
    String sharedTarget="/WEB-INF/new/_shared/services/role_switch_service.jsp";
    request.getRequestDispatcher(sharedTarget).forward(request,response);return;
}

NewUiHybridMenuSnapshot snapshot=NewUiHybridMenuAccessService.getSnapshot(request);
request.setAttribute("newUiHybridMenuSnapshot",snapshot);request.setAttribute("nui_menu_tree",snapshot.getSidebarBranches());

Long menuId=nuiLong(request.getParameter("menuId"));if(menuId==null)menuId=nuiLong(request.getParameter("menu"));
Long groupMenuId=nuiLong(request.getParameter("groupMenuId"));if(menuId!=null)groupMenuId=null;
NewUiHybridMenuNode selected=menuId==null?null:snapshot.findAssigned(menuId);
NewUiHybridMenuNode group=groupMenuId==null?null:snapshot.findVisible(groupMenuId);
String menuStatus=menuId==null?null:NewUiHybridMenuRouteGuard.evaluateMenu(snapshot,menuId);
String groupStatus=groupMenuId==null?null:NewUiHybridMenuRouteGuard.evaluateGroup(snapshot,groupMenuId);
String nativeSubroute=request.getParameter("nativeSubroute");

int httpStatus=200;String target="/WEB-INF/new/_shared/ui/home.jsp";String module="_shared";String pageName="home";
if(groupMenuId!=null){
    if(!NewUiHybridMenuRouteRegistry.NEW_UI.equals(groupStatus)){httpStatus=403;target="/WEB-INF/new/_shared/ui/403.jsp";group=null;}
    else{
        target="/WEB-INF/new/_shared/menu/leaf_catalog.jsp";pageName="leaf_catalog";
        request.setAttribute("nui_catalog_group",group);
        request.setAttribute("nui_catalog_all_nodes",NewUiHybridMenuCatalogService.allDescendants(snapshot,groupMenuId,NewUiHybridMenuCatalogService.SORT_ORDER));
    }
}else if(menuId!=null){
    if(NewUiHybridMenuRouteRegistry.FORBIDDEN.equals(menuStatus)||selected==null){httpStatus=403;target="/WEB-INF/new/_shared/ui/403.jsp";}
    else if(NewUiHybridMenuRouteRegistry.NOT_FOUND.equals(menuStatus)){httpStatus=404;target="/WEB-INF/new/_shared/ui/404.jsp";}
    else if(NewUiHybridMenuRouteRegistry.NOT_MAPPED.equals(menuStatus)){httpStatus=404;target="/WEB-INF/new/_shared/menu/not_mapped.jsp";}
    else if(NewUiHybridMenuRouteRegistry.NEW_UI.equals(menuStatus)){
        NewUiNativeSubrouteRegistry.Route subroute=nativeSubroute==null?null:NewUiNativeSubrouteRegistry.resolve(selected,nativeSubroute);
        if(nativeSubroute!=null&&subroute==null){httpStatus=403;target="/WEB-INF/new/_shared/ui/403.jsp";}
        else if(subroute!=null){
            module=subroute.getModule();pageName=subroute.getPage();target=subroute.target(service);
            request.setAttribute("nui_native_subroute",nativeSubroute);
            request.setAttribute("nuiServiceEndpointOverride",request.getContextPath()+"/new?service=1&menuId="+menuId+"&nativeSubroute="+java.net.URLEncoder.encode(nativeSubroute,"UTF-8"));
        }else{
        module=selected.getNewUiModule();pageName=selected.getNewUiPage()==null?"index":selected.getNewUiPage();
        if("_shared".equals(module)&&"native_menu".equals(pageName)){
            NewUiNativeJspResolver.Result nativeRoute=NewUiNativeJspResolver.resolve(application,selected.getExistingUrl(),service);
            if(nativeRoute==null){target="/WEB-INF/new/_shared/"+(service?"services/native_menu_service.jsp":"uiux/native_menu.jsp");}
            else{module=nativeRoute.getModule();pageName=nativeRoute.getPage();target=nativeRoute.getTarget();request.setAttribute("nui_native_module",module);request.setAttribute("nui_native_page",pageName);}
        }else target="/WEB-INF/new/"+module+(service?"/services/":"/uiux/")+pageName+(service?"_service.jsp":".jsp");
        }
        if(httpStatus==200&&((requestedModule!=null&&!nuiEqual(requestedModule,module))||(requestedPage!=null&&!nuiEqual(requestedPage,pageName)))){httpStatus=403;target="/WEB-INF/new/_shared/ui/403.jsp";}
    }
}else if(service||requestedModule!=null||requestedPage!=null){
    httpStatus=403;target="/WEB-INF/new/_shared/ui/403.jsp";
}

Long activeId=groupMenuId!=null?groupMenuId:menuId;List<NewUiHybridMenuNode> activePath=activeId==null?new ArrayList<NewUiHybridMenuNode>():snapshot.breadcrumb(activeId);
Long currentGroupId=groupMenuId;
if(currentGroupId==null){for(int pathI=0;pathI<activePath.size();pathI++)if(activePath.get(pathI).isBranch())currentGroupId=activePath.get(pathI).getMenuId();}
request.setAttribute("nui_current_menu_id",menuId);request.setAttribute("nui_current_group_id",currentGroupId);
request.setAttribute("nui_active_path",activePath);request.setAttribute("nui_breadcrumb",activePath);
request.setAttribute("nui_selected_menu_node",selected);
request.setAttribute("nui_current_module",module);request.setAttribute("nui_current_page",pageName);

if(httpStatus!=200)response.setStatus(httpStatus);
if(httpStatus==200&&!nuiExists(application,target)){
    if(service){response.setStatus(404);response.setContentType("application/json; charset=UTF-8");out.print("{\"ok\":false,\"code\":\"SERVICE_NOT_FOUND\"}");return;}
    response.setStatus(404);target="/WEB-INF/new/_shared/ui/404.jsp";
}
if(service){
    if(selected==null||!NewUiHybridMenuRouteRegistry.NEW_UI.equals(menuStatus)){response.setStatus(403);response.setContentType("application/json; charset=UTF-8");out.print("{\"ok\":false,\"code\":\"FORBIDDEN\"}");return;}
    String action=request.getParameter("action");if(action==null||action.trim().length()==0)action="meta";
    if(!NewUiHybridMenuRouteGuard.isActionAuthorized(request,selected,action)){response.setStatus(403);response.setContentType("application/json; charset=UTF-8");out.print("{\"ok\":false,\"code\":\"ACTION_FORBIDDEN\"}");return;}
    response.setContentType("application/json; charset=UTF-8");request.getRequestDispatcher(target).forward(request,response);return;
}

Tbmuser current=null;try{current=Common.getCurrentUser(request);}catch(Exception ignored){}
String currentName=current==null?"Pengguna":current.getUserNama();if(currentName==null||currentName.trim().length()==0)currentName=current==null?"Pengguna":current.getUserId();if(currentName==null||currentName.trim().length()==0)currentName="Pengguna";
String newUrl=request.getContextPath()+"/new";String frameSrc;
if(requestedDashboard!=null&&requestedDashboard.trim().length()>0){frameSrc=newUrl+"?frame=1&dashboard="+java.net.URLEncoder.encode(requestedDashboard,"UTF-8");if(requestedDashboardView!=null&&requestedDashboardView.trim().length()>0)frameSrc+="&dashboardView="+java.net.URLEncoder.encode(requestedDashboardView,"UTF-8");}
else if(requestedDashboardMenu)frameSrc=newUrl+"?frame=1&dashboardMenu=1";
else if(menuId!=null)frameSrc=newUrl+"?frame=1&menuId="+menuId;
else if(groupMenuId!=null)frameSrc=newUrl+"?frame=1&groupMenuId="+groupMenuId;
else frameSrc=newUrl+"?frame=1";
if(nativeSubroute!=null&&menuId!=null)frameSrc+="&nativeSubroute="+java.net.URLEncoder.encode(nativeSubroute,"UTF-8");
if(frame){
%>
<!doctype html><html lang="id"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex,nofollow"><title>eCampus &amp; eSchool</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-sRIl4kxILFvY47J16cr9ZwB07vP4J8+LH7qKQnuqkuIAvNWLzeN8tE5YBujZqJLB" crossorigin="anonymous"><link id="nuiFontAwesome" href="https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@7.3.0/css/all.min.css" rel="stylesheet" crossorigin="anonymous" onerror="this.onerror=null;this.href='https://unpkg.com/@fortawesome/fontawesome-free@7.3.0/css/all.min.css'"><link href="https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@7.3.0/css/v4-shims.min.css" rel="stylesheet" crossorigin="anonymous"><style><%@ include file="/WEB-INF/new/_shared/assets/new-ui.css" %></style></head>
<body class="nui-frame-body"><div class="nui-frame-content"><%pageContext.include(target,true);%><jsp:include page="/WEB-INF/new/_shared/menu/menu_diagnostics.jsp" flush="false"/></div><div id="nuiLoading" class="nui-loading"></div><div id="nuiToast" class="nui-toast"></div><script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js" integrity="sha384-FKyoEForCGlyvwx9Hj09JcYn3nv7wiPVlz7YYwJrWVcXK/BmnVDxM+D2scQbITxI" crossorigin="anonymous"></script><script><%@ include file="/WEB-INF/new/_shared/assets/new-ui.js" %></script></body></html>
<%return;}%>
<!doctype html>
<html lang="id"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex,nofollow"><title>eCampus &amp; eSchool — Modern UI</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-sRIl4kxILFvY47J16cr9ZwB07vP4J8+LH7qKQnuqkuIAvNWLzeN8tE5YBujZqJLB" crossorigin="anonymous"><link id="nuiFontAwesome" href="https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@7.3.0/css/all.min.css" rel="stylesheet" crossorigin="anonymous" onerror="this.onerror=null;this.href='https://unpkg.com/@fortawesome/fontawesome-free@7.3.0/css/all.min.css'"><link href="https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@7.3.0/css/v4-shims.min.css" rel="stylesheet" crossorigin="anonymous"><style><%@ include file="/WEB-INF/new/_shared/assets/new-ui.css" %></style></head>
<body data-nui-menu-cache="<%=nuiShellH(snapshot.getUserId())%>|<%=nuiShellH(snapshot.getRoleId())%>">
<div id="nuiLoading" class="nui-loading"></div><div class="nui-app">
  <aside id="nuiSidebar" class="nui-sidebar"><div class="nui-brand"><div class="nui-logo"><i class="fa-solid fa-graduation-cap nui-fa-icon" aria-hidden="true"></i></div><div><strong>eCampus &amp; eSchool</strong><small>Enterprise Education</small></div></div><nav class="nui-nav" aria-label="Menu utama"><jsp:include page="/WEB-INF/new/_shared/ui/sidebar.jsp" flush="false"/></nav><div class="nui-sidebar-foot"><i class="fa-solid fa-circle nui-fa-icon" aria-hidden="true"></i> Online &nbsp; • &nbsp; Menu sesuai peran aktif</div></aside>
  <main class="nui-main"><header class="nui-topbar"><button id="nuiMenuToggle" class="nui-menu-toggle" aria-label="Buka menu"><i class="fa-solid fa-bars nui-fa-icon" aria-hidden="true"></i></button><div class="nui-global-search"><i class="fa-solid fa-magnifying-glass nui-fa-icon" aria-hidden="true"></i><input id="nuiGlobalSearch" placeholder="Cari menu yang boleh diakses..." aria-label="Pencarian menu"><span class="nui-kbd">Ctrl + K</span></div><div class="nui-top-actions"><jsp:include page="/WEB-INF/new/_shared/ui/role_switcher.jsp" flush="false"/><jsp:include page="/WEB-INF/new/_shared/ui/toolbar_utilities.jsp" flush="false"/><button class="nui-icon-btn" aria-label="Notifikasi"><i class="fa-regular fa-bell nui-fa-icon" aria-hidden="true"></i><span class="nui-badge">0</span></button><button class="nui-icon-btn" aria-label="Pesan"><i class="fa-regular fa-envelope nui-fa-icon" aria-hidden="true"></i></button><div class="nui-user"><div class="nui-avatar"><%=nuiShellH(currentName.substring(0,1).toUpperCase())%></div><div><strong><%=nuiShellH(currentName)%></strong><small>Pengguna aktif</small></div></div></div></header>
    <iframe id="nuiMainFrame" name="nuiMainFrame" class="nui-main-frame" src="<%=nuiShellH(frameSrc)%>" title="Konten utama eCampus"></iframe><footer class="nui-footer"><span>© 2026 eCampus &amp; eSchool</span><span>Hybrid Menu • RBAC fail-closed</span></footer></main>
</div><button type="button" id="nuiBackToTop" class="nui-back-to-top" aria-label="Kembali ke atas" title="Kembali ke atas"><i class="fa-solid fa-arrow-up"></i></button><div id="nuiToast" class="nui-toast"></div><div id="nuiCommand" class="nui-command" role="dialog" aria-modal="true" aria-label="Cari menu"><div class="nui-command-box"><input id="nuiCommandInput" class="nui-input" placeholder="Ketik nama menu..."><div class="nui-command-results"><jsp:include page="/WEB-INF/new/_shared/ui/command_palette.jsp" flush="false"/></div></div></div><script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js" integrity="sha384-FKyoEForCGlyvwx9Hj09JcYn3nv7wiPVlz7YYwJrWVcXK/BmnVDxM+D2scQbITxI" crossorigin="anonymous"></script><script><%@ include file="/WEB-INF/new/_shared/assets/new-ui.js" %></script></body></html>
