<%@page import="java.net.URLEncoder"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<html>
<head>
<title>E-Campus</title>
<link rel="shortcut icon"
	href="<%=request.getContextPath()%>/img/logo.png" type="image/x-icon"/>
<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no"/>
<meta name="description" content="Sistem Informasi Akademik Ecampus"/>
<meta name="author" content="Mohammad Fauzi Murtadho"/>


 <%
if(request.getRequestURI().toLowerCase().contains("ojs") ){
	
}
	String url = "login.jsp?rand=" + Common.randLong()
			+ (request.getParameter("error_capcha") != null ? "&error_capcha=true" : "")
			+ (request.getParameter("login_error") != null
					? "&login_error=" + URLEncoder.encode(request.getParameter("login_error"), "UTF-8") : "");
	if (session.getAttribute("usersTemp") != null) {
		url = request.getContextPath() +"/main?rand=" + Common.randLong()
				+ (request.getParameter("error_capcha") != null ? "&error_capcha=true" : "")
				+ (request.getParameter("login_error") != null
						? "&login_error=" + URLEncoder.encode(request.getParameter("login_error"), "UTF-8")
						: "");
	}
	
	response.sendRedirect(url);
%> 

<%-- <meta http-equiv="refresh" content="0;URL='<%=url%>'" /> --%>
<title>E-Campus</title>
<link rel="shortcut icon"
	href="<%=request.getContextPath()%>/img/logo.png" type="image/x-icon"/>
<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no"/>
<meta name="description" content="Sistem Informasi Akademik Ecampus"/>
<meta name="author" content="Mohammad Fauzi Murtadho"/>
</head>

<body style="text-align:center;">
		<br>
	<br>
	<br>
	<br>
	<br>
	<br>
	Harap tunggu, sedang menyiapkan data ....
	<br>
	<img alt="" width="25%" src="<%=request.getContextPath()%>/loading.gif">
</body>

</html>