<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Collections" %>
<%@ page import="ais.common.newui.menu.NewUiHybridMenuNode" %>
<%@ page import="ais.common.newui.menu.NewUiHybridMenuTreeBuilder" %>
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
boolean nuiHasSidebarChildren=!nuiBranch.getBranchChildren().isEmpty()||(nuiBranch.isDirectLeavesInSidebar()&&!nuiBranch.getDirectLeaves().isEmpty());
String nuiBranchNewUrl=request.getContextPath()+"/new";String nuiBranchId="nuiHybridGroup"+String.valueOf(nuiBranch.getMenuId()).replace("-","v");
%>
<div class="nui-nav-node nui-nav-branch nui-nav-level-<%=Math.min(nuiDepth+1,6)%><%=nuiBranchOpen?" open":""%>" data-menu-id="<%=nuiBranch.getMenuId()%>">
  <div class="nui-nav-row">
    <%if(nuiHasSidebarChildren){%><button type="button" class="nui-nav-toggle nui-nav-chevron" aria-expanded="<%=nuiBranchOpen%>" aria-controls="<%=nuiBranchId%>" title="Buka/tutup submenu"><i class="fa-solid fa-chevron-down nui-fa-icon" aria-hidden="true"></i></button><%}else{%><span class="nui-nav-toggle" aria-hidden="true"></span><%}%>
    <a class="nui-nav-link nui-nav-branch-button<%=nuiBranchActive?" active":""%>" target="nuiMainFrame" data-menu-id="<%=nuiBranch.getMenuId()%>" data-shell-url="<%=nuiBranchNewUrl%>?groupMenuId=<%=nuiBranch.getMenuId()%>" href="<%=nuiBranchNewUrl%>?frame=1&amp;groupMenuId=<%=nuiBranch.getMenuId()%>"><span class="nui-nav-icon"><i class="<%=nuiBranchH(NewUiIconUtil.classes(nuiBranch.getIcon(),nuiBranch.getLabel(),true))%> nui-fa-icon" aria-hidden="true"></i></span><span><%=nuiBranchH(nuiBranch.getLabel())%></span><span class="nui-nav-count"><%=nuiBranch.getLeafCount()%></span></a>
  </div>
  <%if(nuiHasSidebarChildren){%><div id="<%=nuiBranchId%>" class="nui-nav-children nui-nav-branch-children">
  <%List<NewUiHybridMenuNode> nuiSidebarChildren=new ArrayList<NewUiHybridMenuNode>();nuiSidebarChildren.addAll(nuiBranch.getBranchChildren());if(nuiBranch.isDirectLeavesInSidebar())nuiSidebarChildren.addAll(nuiBranch.getDirectLeaves());Collections.sort(nuiSidebarChildren,NewUiHybridMenuTreeBuilder.NODE_ORDER);for(int nuiChildI=0;nuiChildI<nuiSidebarChildren.size();nuiChildI++){NewUiHybridMenuNode nuiChild=nuiSidebarChildren.get(nuiChildI);if(nuiChild.isBranch()){request.setAttribute("nui_sidebar_branch",nuiChild);request.setAttribute("nui_sidebar_depth",Integer.valueOf(nuiDepth+1));%>
    <jsp:include page="/WEB-INF/new/_shared/menu/sidebar_branch.jsp" flush="false"/>
  <%}else{NewUiHybridMenuNode nuiLeaf=nuiChild;boolean nuiLeafActive=nuiLeaf.getMenuId().equals(request.getAttribute("nui_current_menu_id"));%>
    <div class="nui-nav-node nui-nav-leaf nui-nav-level-<%=Math.min(nuiDepth+2,6)%>" data-menu-id="<%=nuiLeaf.getMenuId()%>">
      <a class="nui-nav-link<%=nuiLeafActive?" active":""%>" target="nuiMainFrame" data-menu-id="<%=nuiLeaf.getMenuId()%>" data-shell-url="<%=nuiBranchNewUrl%>?menuId=<%=nuiLeaf.getMenuId()%>" href="<%=nuiBranchNewUrl%>?frame=1&amp;menuId=<%=nuiLeaf.getMenuId()%>"><span class="nui-nav-icon"><i class="<%=nuiBranchH(NewUiIconUtil.classes(nuiLeaf.getIcon(),nuiLeaf.getLabel(),false))%> nui-fa-icon" aria-hidden="true"></i></span><span><%=nuiBranchH(nuiLeaf.getLabel())%></span></a>
    </div>
  <%}}request.setAttribute("nui_sidebar_branch",nuiBranch);request.setAttribute("nui_sidebar_depth",Integer.valueOf(nuiDepth));%>
  </div><%}%>
</div>
