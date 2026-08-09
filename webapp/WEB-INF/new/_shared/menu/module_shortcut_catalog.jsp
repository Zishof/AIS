<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.ArrayList" %><%@ page import="java.util.List" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="ais.common.Common" %><%@ page import="ais.database.model.Tbmrole" %><%@ page import="ais.database.model.Tbmuser" %>
<%@ page import="ais.common.newui.menu.NewUiHybridMenuNode" %><%@ page import="ais.common.newui.menu.NewUiHybridMenuSnapshot" %>
<%@ page import="ais.common.newui.menu.NewUiModuleShortcut" %><%@ page import="ais.common.newui.menu.NewUiModuleShortcutService" %>
<%@ page import="ais.common.newui.menu.NewUiModuleDashboardService" %><%@ page import="ais.common.newui.menu.NewUiModuleDashboardService.Dashboard" %>
<%@ page import="ais.common.newui.menu.NewUiHybridMenuCatalogService" %>
<%@ page import="ais.common.newui.menu.NewUiDashboardUtilityService" %>
<%@ page import="ais.common.newui.menu.NewUiDashboardUtilityService.OnlineUserInfo" %>
<%@ page import="ais.common.newui.menu.NewUiDashboardUtilityService.CustomerContact" %>
<%!
private String nuiModuleH(Object v){if(v==null)return "";return String.valueOf(v).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
%>
<%
NewUiHybridMenuSnapshot nuiModuleSnapshot=(NewUiHybridMenuSnapshot)request.getAttribute("newUiHybridMenuSnapshot");
Tbmuser nuiModuleUser=null;try{nuiModuleUser=Common.getCurrentUser(request);}catch(Exception ignored){}
Tbmrole nuiModuleRole=null;try{nuiModuleRole=nuiModuleUser==null?null:nuiModuleUser.hakAkses();}catch(Exception ignored){}
List<NewUiModuleShortcut> nuiModules=NewUiModuleShortcutService.build(nuiModuleUser,nuiModuleSnapshot);
List<OnlineUserInfo> nuiOnlineUsers=NewUiDashboardUtilityService.onlineUsers();
List<CustomerContact> nuiCustomerContacts=NewUiDashboardUtilityService.customerServices();
Number nuiAccessCount=NewUiDashboardUtilityService.accessCount();Number nuiOnlineCount=NewUiDashboardUtilityService.onlineCount();
SimpleDateFormat nuiLoginFormat=new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
String nuiModuleNewUrl=request.getContextPath()+"/new";
String nuiDashboardKey=request.getParameter("dashboard");NewUiModuleShortcut nuiActiveDashboardShortcut=null;
if(nuiDashboardKey!=null)for(int nuiDashI=0;nuiDashI<nuiModules.size();nuiDashI++)if(nuiDashboardKey.equalsIgnoreCase(nuiModules.get(nuiDashI).getKey())){nuiActiveDashboardShortcut=nuiModules.get(nuiDashI);break;}
Dashboard nuiActiveDashboard=nuiActiveDashboardShortcut==null?null:NewUiModuleDashboardService.load(nuiActiveDashboardShortcut.getKey());
if(nuiDashboardKey!=null&&nuiActiveDashboard==null){response.setStatus(403);%><jsp:include page="/WEB-INF/new/_shared/ui/403.jsp"/><%return;}
if(nuiActiveDashboard!=null){
  NewUiHybridMenuNode nuiDashboardTarget=nuiActiveDashboardShortcut.getTarget();Long nuiDashboardGroupId=nuiDashboardTarget!=null&&nuiDashboardTarget.isBranch()?nuiDashboardTarget.getMenuId():null;
  if(nuiDashboardGroupId==null&&nuiDashboardTarget!=null){List<NewUiHybridMenuNode> nuiDashboardPath=nuiModuleSnapshot.breadcrumb(nuiDashboardTarget.getMenuId());for(int nuiPathI=nuiDashboardPath.size()-1;nuiPathI>=0;nuiPathI--)if(nuiDashboardPath.get(nuiPathI).isBranch()){nuiDashboardGroupId=nuiDashboardPath.get(nuiPathI).getMenuId();break;}}
  List<NewUiHybridMenuNode> nuiDashboardMenus=NewUiHybridMenuCatalogService.allDescendants(nuiModuleSnapshot,nuiDashboardGroupId,NewUiHybridMenuCatalogService.SORT_ORDER);
  request.setAttribute("nuiActiveModuleShortcut",nuiActiveDashboardShortcut);request.setAttribute("nuiActiveModuleDashboard",nuiActiveDashboard);request.setAttribute("nuiActiveModuleMenus",nuiDashboardMenus);%><jsp:include page="/WEB-INF/new/_shared/dashboard/module_dashboard.jsp"/><%return;}
%>
<section class="nui-leaf-catalog nui-module-catalog">
  <header class="nui-module-hero">
    <div class="nui-catalog-hero-row"><div><h1>Aplikasi &amp; Modul</h1><p><%=nuiModules.size()%> modul aktif dan dapat diakses oleh role saat ini.</p></div><div class="nui-dashboard-utilities"><button type="button" class="nui-btn nui-btn-hero" data-nui-dialog-open="nuiOnlineDialog"><i class="fa-solid fa-users"></i> Online: <%=nuiModuleH(nuiOnlineCount)%></button><%if(!nuiCustomerContacts.isEmpty()){%><button type="button" class="nui-btn nui-btn-hero" data-nui-dialog-open="nuiCustomerDialog"><i class="fa-brands fa-whatsapp"></i> Layanan Pengguna</button><%}%></div></div>
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
      String nuiDashboardParam=java.net.URLEncoder.encode(nuiModule.getKey(),"UTF-8");String nuiTargetShell=nuiModuleNewUrl+"?dashboard="+nuiDashboardParam;
    %>
    <a class="nui-page-link nui-leaf-card nui-module-shortcut-card<%=nuiTargetBranch?" nui-menu-branch-card":""%>" data-direct="1" data-search="<%=nuiModuleH(nuiModule.getSearchText())%>" data-label="<%=nuiModuleH(nuiModule.getLabel())%>" data-order="<%=nuiModule.getOrder()%>" data-path="Aplikasi &amp; Modul" target="nuiMainFrame" data-shell-url="<%=nuiTargetShell%>" href="<%=nuiModuleNewUrl+"?frame=1&amp;dashboard="+nuiDashboardParam%>">
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
<dialog id="nuiOnlineDialog" class="nui-dashboard-dialog"><header><div><small>Sumber: UserOnlineCounter &amp; SecurityFilter.dataOnline</small><h2>Daftar Pengguna Online</h2></div><button type="button" class="nui-icon-btn" data-nui-dialog-close aria-label="Tutup">×</button></header><div class="nui-dashboard-summary"><span>Akses 23 jam: <strong><%=nuiModuleH(nuiAccessCount)%></strong></span><span>Login aktif: <strong><%=nuiModuleH(nuiOnlineCount)%></strong></span></div><div class="nui-table-wrap"><table class="nui-table"><thead><tr><th>Nama</th><th>Jenis pengguna</th><th>Unit</th><th>Sub Unit</th><th>Waktu Login</th></tr></thead><tbody><%if(nuiOnlineUsers.isEmpty()){%><tr><td colspan="5">Belum ada pengguna online.</td></tr><%}for(int nuiOnlineI=0;nuiOnlineI<nuiOnlineUsers.size();nuiOnlineI++){OnlineUserInfo info=nuiOnlineUsers.get(nuiOnlineI);%><tr><td><%=nuiModuleH(info.getName())%></td><td><%=nuiModuleH(info.getRole())%></td><td><%=nuiModuleH(info.getUnit())%></td><td><%=nuiModuleH(info.getSubUnit())%></td><td><%=info.getLogin()==null?"":nuiModuleH(nuiLoginFormat.format(info.getLogin()))%></td></tr><%}%></tbody></table></div></dialog>
<%if(!nuiCustomerContacts.isEmpty()){%><dialog id="nuiCustomerDialog" class="nui-dashboard-dialog nui-customer-dialog"><header><div><small>Sumber: tabel CustomerService aktif</small><h2>Layanan Pengguna</h2></div><button type="button" class="nui-icon-btn" data-nui-dialog-close aria-label="Tutup">×</button></header><div class="nui-customer-grid"><%for(int nuiContactI=0;nuiContactI<nuiCustomerContacts.size();nuiContactI++){CustomerContact contact=nuiCustomerContacts.get(nuiContactI);String phone=contact.getInternationalPhone();%><article class="nui-card nui-card-pad"><small><%=nuiModuleH(contact.getGroup())%></small><strong><%=nuiModuleH(contact.getPerson().length()==0?contact.getPhone():contact.getPerson())%></strong><span><%=nuiModuleH(contact.getPhone())%></span><div><a class="nui-btn" target="_blank" rel="noopener" href="https://web.whatsapp.com/send?phone=<%=nuiModuleH(phone)%>&amp;text=Halo.."><i class="fa-brands fa-whatsapp"></i> WhatsApp</a><a class="nui-btn" href="tel:<%=nuiModuleH(phone)%>"><i class="fa-solid fa-phone"></i> Telepon</a></div></article><%}%></div></dialog><%}%>
<button type="button" id="nuiBackToTop" class="nui-back-to-top" aria-label="Kembali ke atas" title="Kembali ke atas"><i class="fa-solid fa-arrow-up"></i></button>
