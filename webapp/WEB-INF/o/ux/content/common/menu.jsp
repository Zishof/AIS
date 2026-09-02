<%@page import="java.net.URLEncoder"%>
<%@page import="ais.action.master.helper.MainHelper"%>
<%@page import="ais.database.model.file.FileFotoLain"%>
<%@page import="ais.database.model.Menu"%>
<%@page import="java.util.Set"%>
<%@page import="ais.database.model.Tbmrole"%>
<%
try {
	//Tbmrole tbmrole = (Tbmrole) request.getAttribute("tbmrole");
	Set<Menu> menus = (Set<Menu>) request.getAttribute("menus");
	java.util.List<Menu> menusList = menus == null
			? new java.util.ArrayList<Menu>() : new java.util.ArrayList<Menu>(menus);
	Menu root = (Menu) request.getAttribute("menu");

	for (Menu menu : menus) {
		if (menu.getAktif() != null && !menu.getAktif()) {
	continue;
		}

		if ((root == null && menu.getRoot().equals(0L)) || (root != null && menu.getRoot().equals(root.getChild()))) {
	String icon = FileFotoLain.iconAwesome(menu.getLabel());
	Boolean ada = MainHelper.hasChild(menu.getChild(), menusList);

	String subjudul = URLEncoder.encode(menu.getLabel(), "UTF-8");
	String judul = URLEncoder.encode(menu.getLabel(), "UTF-8");

	request.setAttribute("menu", menu);

	if (ada) {
%>

<div data-kt-menu-trigger="click" class="menu-item menu-accordion">

	<!--begin:Menu link-->
	<span class="menu-link"> <span class="menu-icon"> <i
			class="fad <%=icon%>"></i></span> <span class="menu-title"><%=menu.getLabel()%></span>
		<span class="menu-arrow"></span>
	</span>

	<!--begin:Menu sub-->
	<div class="menu-sub menu-sub-accordion">
		<jsp:include page="/WEB-INF/o/ux/content/common/menu.jsp">
			<jsp:param value="${menus}" name="menus" />
			<jsp:param value="${menu}" name="menu" />
		</jsp:include>
	</div>

</div>




<%
} else {

String urlLink;
if (menu.getUrl().startsWith(request.getContextPath())) {
	urlLink = menu.getUrl();
} else {
	String l = URLEncoder.encode(request.getContextPath() + menu.getUrl(), "UTF-8");

	urlLink = request.getContextPath() + "/ux/m.jsp?l=" + l + "&subjudul=" + subjudul + "&judul=" + judul;
}
%>


<!--begin:Menu item-->
<div class="menu-item">
	<!--begin:Menu link-->
	<a class="menu-link" href="<%=urlLink%>"> <span class="menu-icon">
			<i class="fad <%=icon%>"></i>
	</span> <span class="menu-title"><%=menu.getLabel()%></span>
	</a>
	<!--end:Menu link-->
</div>
<!--end:Menu item-->

<%
}
%>

<%
}
}
} catch (Exception e) {
e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/o/ux/content/common/menu.jsp:84");
}
%>