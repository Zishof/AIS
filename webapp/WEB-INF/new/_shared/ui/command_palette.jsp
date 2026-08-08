<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.io.IOException" %><%@ page import="java.net.URLEncoder" %><%@ page import="java.util.List" %><%@ page import="javax.servlet.jsp.JspWriter" %>
<%@ page import="ais.common.newui.NewUiMenuAccessService" %><%@ page import="ais.common.newui.NewUiMenuNode" %>
<%!
private String nuiCmdH(Object v){if(v==null)return "";return String.valueOf(v).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
private void nuiCmdNodes(JspWriter out,List<NewUiMenuNode> nodes,String newUrl,String path)throws IOException{for(int i=0;i<nodes.size();i++){NewUiMenuNode n=nodes.get(i);String p=path.length()==0?n.getLabel():path+" / "+n.getLabel();if(n.isClickable()){String href=n.isMappedToNewUi()?newUrl+"?frame=1&module="+URLEncoder.encode(n.getNewUiModule(),"UTF-8")+"&page="+URLEncoder.encode(n.getNewUiPage()==null?"index":n.getNewUiPage(),"UTF-8")+"&menu="+n.getMenuId():n.getLegacyUrl();out.print("<a target=\"nuiMainFrame\" data-shell-url=\""+nuiCmdH(newUrl+"?menu="+n.getMenuId())+"\" href=\""+nuiCmdH(href)+"\"><strong>"+nuiCmdH(n.getLabel())+"</strong><small>"+nuiCmdH(p)+"</small></a>");}nuiCmdNodes(out,n.getChildren(),newUrl,p);}}
%>
<% List<NewUiMenuNode> nuiCmdTree=(List<NewUiMenuNode>)request.getAttribute("nui_menu_tree");if(nuiCmdTree!=null)nuiCmdNodes(out,nuiCmdTree,request.getContextPath()+"/new",""); %>
