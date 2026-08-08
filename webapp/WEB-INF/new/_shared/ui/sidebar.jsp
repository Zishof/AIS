<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.List" %>
<%@ page import="javax.servlet.jsp.JspWriter" %>
<%@ page import="ais.common.newui.NewUiMenuAccessService" %>
<%@ page import="ais.common.newui.NewUiMenuNode" %>
<%!
private String nuiSideH(Object v){if(v==null)return "";return String.valueOf(v).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
private boolean nuiSideContains(NewUiMenuNode n,Long id){if(n==null||id==null)return false;if(id.equals(n.getMenuId()))return true;for(int i=0;i<n.getChildren().size();i++)if(nuiSideContains(n.getChildren().get(i),id))return true;return false;}
private String nuiSideFrame(NewUiMenuNode n,String newUrl)throws IOException{if(n.isMappedToNewUi())return newUrl+"?frame=1&module="+URLEncoder.encode(n.getNewUiModule(),"UTF-8")+"&page="+URLEncoder.encode(n.getNewUiPage()==null?"index":n.getNewUiPage(),"UTF-8")+"&menu="+n.getMenuId();return n.getLegacyUrl();}
private void nuiSideNode(JspWriter out,NewUiMenuNode n,String newUrl,Long current,int depth)throws IOException{
 boolean branch=n.hasChildren();boolean open=nuiSideContains(n,current);String id="nuiGroup"+n.getMenuId();
 out.print("<div class=\"nui-nav-node depth-"+Math.min(depth,6)+(open?" open":"")+"\">");
 out.print("<div class=\"nui-nav-row\">");
 if(n.isClickable())out.print("<a class=\"nui-nav-link"+(n.getMenuId().equals(current)?" active":"")+"\" target=\"nuiMainFrame\" data-menu-id=\""+n.getMenuId()+"\" data-shell-url=\""+nuiSideH(newUrl+"?menu="+n.getMenuId())+"\" href=\""+nuiSideH(nuiSideFrame(n,newUrl))+"\"><span class=\"nui-nav-icon\">◇</span><span>"+nuiSideH(n.getLabel())+(n.isMappedToNewUi()?"":"<small class=\"nui-legacy-badge\">Tampilan lama</small>")+"</span>"+(n.getReadableDescendantCount()>0?"<span class=\"nui-nav-count\">"+n.getReadableDescendantCount()+"</span>":"")+"</a>");
 else out.print("<span class=\"nui-nav-heading\"><span class=\"nui-nav-icon\">◇</span><span>"+nuiSideH(n.getLabel())+"</span>"+(n.getReadableDescendantCount()>0?"<span class=\"nui-nav-count\">"+n.getReadableDescendantCount()+"</span>":"")+"</span>");
 if(branch)out.print("<button type=\"button\" class=\"nui-nav-toggle\" aria-expanded=\""+open+"\" aria-controls=\""+id+"\" title=\"Buka/tutup submenu\">⌄</button>");
 out.print("</div>");
 if(branch){out.print("<div id=\""+id+"\" class=\"nui-nav-children\">");for(int i=0;i<n.getChildren().size();i++)nuiSideNode(out,n.getChildren().get(i),newUrl,current,depth+1);out.print("</div>");}
 out.print("</div>");
}
%>
<% List<NewUiMenuNode> tree=(List<NewUiMenuNode>)request.getAttribute("nui_menu_tree");Long current=(Long)request.getAttribute("nui_current_menu_id");String newUrl=request.getContextPath()+"/new";
if(tree==null||tree.isEmpty()){%><div class="nui-nav-empty">Tidak ada menu dengan izin READ untuk peran aktif.</div><%}else{for(int i=0;i<tree.size();i++){out.print("<div class=\"nui-nav-group\">");nuiSideNode(out,tree.get(i),newUrl,current,0);out.print("</div>");}}%>
