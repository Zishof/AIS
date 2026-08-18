<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.sendRedirect(request.getContextPath() + "/kursus/login.jsp");
	return;
} else {
%>
<jsp:include page="/WEB-INF/o/kursus/inc/header.jsp"></jsp:include>
<body>
	<div id="page">
		<jsp:include page="/WEB-INF/o/kursus/inc/menu.jsp"></jsp:include>

		<jsp:include page="/WEB-INF/o/kursus/content/cart_3.jsp"></jsp:include>

		<jsp:include page="/WEB-INF/o/kursus/inc/footer.jsp"></jsp:include>
	</div>
	<jsp:include page="/WEB-INF/o/kursus/inc/js.jsp"></jsp:include>
</body>
</html>
<%
}
%>