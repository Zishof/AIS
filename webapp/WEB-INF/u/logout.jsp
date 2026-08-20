<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>Infromasi</title>
</head>
<body style="text-align: center;">
	<div style="font-size: x-large; color: red;">
		<%=request.getParameter("login_error")%>
	</div>
	<br>
	<a href="<%=request.getContextPath()%>/logoff">Login</a>
<jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body>
</html>
