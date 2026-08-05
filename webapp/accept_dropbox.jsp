
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="ais.common.GoogleCommon"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>

<%
	String u = request.getParameter("u");
	System.out.println("u => " + u);
	Map map = new HashMap();
	map.put("state", request.getParameterValues("state"));
	map.put("code", request.getParameterValues("code"));
	GoogleCommon.codesDropbox.put(u, map);
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
</body>
</html>