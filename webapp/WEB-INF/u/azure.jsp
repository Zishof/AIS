
<%@page import="ais.common.GoogleCommon"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>

<%
	GoogleCommon.codesAzure.put(request.getParameter("u"), request.getParameter("code"));
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title></title>
</head>
<body>
	<script type="text/javascript">
		window.close();
	</script>
<jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body>
</html>