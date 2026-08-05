
<%
Object linkUrl = request.getAttribute("javax.servlet.error.request_uri");
if (linkUrl != null && (linkUrl.toString().endsWith("pmb") || linkUrl.toString().endsWith("pmb.zul"))) {
	response.sendRedirect(request.getContextPath() + "/pmb");
} else if (linkUrl != null && (linkUrl.toString().endsWith("psb") || linkUrl.toString().endsWith("psb.zul"))) {
	response.sendRedirect(request.getContextPath() + "/psb");
} else if (linkUrl != null && (linkUrl.toString().endsWith("alumni") || linkUrl.toString().endsWith("alumni.zul"))) {
	response.sendRedirect(request.getContextPath() + "/alumni");
} else {
	response.sendRedirect(request.getContextPath() + "/main");
}
%>