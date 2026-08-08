<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="ais.common.newui.menu.NewUiHybridMenuNode" %>
<%@ page import="ais.common.newui.menu.NewUiIconUtil" %>
<%!
private String nuiBranchH(Object v){if(v==null)return "";return String.valueOf(v).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
private boolean nuiBranchPathContains(List path,Long id){if(path==null||id==null)return false;for(int i=0;i<path.size();i++){Object o=path.get(i);if(o instanceof NewUiHybridMenuNode&&id.equals(((NewUiHybridMenuNode)o).getMenuId()))return true;}return false;}
%>
<%
NewUiHybridMenuNode nuiBranch=(NewUiHybridMenuNode)request.getAttribute("nui_sidebar_branch");
Integer nuiDepthValue=(Integer)request.getAttribute("nui_sidebar_depth");int nuiDepth=nuiDepthValue==null?0:nuiDepthValue.intValue();
List nuiActivePath=(List)request.getAttribute("nui_active_path");
boolean nuiBranchOpen=nuiBranchPathContains(nuiActivePath,nuiBranch.getMenuId());
Long nuiCurrentGroup=(Long)request.getAttribute("nui_current_group_id");
boolean nuiBranchActive=nuiBranch.getMenuId().equals(nuiCurrentGroup);
String nuiBranchNewUrl=request.getContextPath()+"/new";String nuiBranchId="nuiHybridGroup"+String.valueOf(nuiBranch.getMenuId()).replace("-","v");
%>
<div class="nui-nav-node nui-nav-branch nui-nav-level-<%=Math.min(nuiDepth+1,6)%><%=nuiBranchOpen?" open":""%>" data-menu-id="<%=nuiBranch.getMenuId()%>">
  <div class="nui-nav-row">
    <button type="button" class="nui-nav-toggle nui-nav-chevron" aria-expanded="<%=nuiBranchOpen%>" aria-controls="<%=nuiBranchId%>" title="Buka/tutup submenu"><i class="fa-solid fa-chevron-down nui-fa-icon" aria-hidden="true"></i></button>
    <a class="nui-nav-link nui-nav-branch-button<%=nuiBranchActive?" active":""%>" target="nuiMainFrame" data-menu-id="<%=nuiBranch.getMenuId()%>" data-shell-url="<%=nuiBranchNewUrl%>?groupMenuId=<%=nuiBranch.getMenuId()%>" href="<%=nuiBranchNewUrl%>?frame=1&amp;groupMenuId=<%=nuiBranch.getMenuId()%>"><span class="nui-nav-icon"><i class="<%=nuiBranchH(NewUiIconUtil.classes(nuiBranch.getIcon(),true))%> nui-fa-icon" aria-hidden="true"></i></span><span><%=nuiBranchH(nuiBranch.getLabel())%></span><span class="nui-nav-count"><%=nuiBranch.getLeafCount()%></span></a>
  </div>
  <div id="<%=nuiBranchId%>" class="nui-nav-children nui-nav-branch-children">
  <%List<NewUiHybridMenuNode> nuiBranchChildren=nuiBranch.getBranchChildren();for(int nuiChildI=0;nuiChildI<nuiBranchChildren.size();nuiChildI++){
    request.setAttribute("nui_sidebar_branch",nuiBranchChildren.get(nuiChildI));request.setAttribute("nui_sidebar_depth",Integer.valueOf(nuiDepth+1));%>
    <jsp:include page="/WEB-INF/new/_shared/menu/sidebar_branch.jsp" flush="false"/>
  <%}request.setAttribute("nui_sidebar_branch",nuiBranch);request.setAttribute("nui_sidebar_depth",Integer.valueOf(nuiDepth));%>
  </div>
</div>
