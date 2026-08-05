<%@page import="java.net.URLEncoder"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<html>
<head>
<title>E-Campus</title>
<link rel="shortcut icon"
	href="<%=request.getContextPath()%>/img/favicon.ico" />

<%
	String url = "login.zul?rand=" + Common.randLong()
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
	
%>

<meta http-equiv="refresh" content="0;URL='<%=url%>'" />

<title>E-Campus</title>
<link rel="shortcut icon"
	href="<%=request.getContextPath()%>/img/logo.png" type="image/x-icon"/>
<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no"/>
<meta name="description" content="Sistem Informasi Akademik Ecampus"/>
<meta name="author" content="Mohammad Fauzi Murtadho"/>
</head>

<body>
	Harap tunggu, sedang menyiapkan data ....
	<br>
	<img src="loading_icon.gif" width="287" height="141" border="0" alt="">
</body>

</html>