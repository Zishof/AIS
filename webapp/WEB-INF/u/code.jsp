
<%
	if (request.getParameter("state") != null) {
		response.sendRedirect(request.getParameter("state") + "&code=" + request.getParameter("code"));
	} else {
%>

<%@ page contentType="text/html;charset=UTF-8"%>
<%@ page import="ais.common.Common" %>
<html>
<head>
<title>Kode Google Drive</title>
<meta name="viewport"
	content="width=device-width, initial-scale=1, shrink-to-fit=no" />
<meta name="description" content="Sistem Informasi Akademik Ecampus" />
<meta name="author" content="Mohammad Fauzi Murtadho" />


<script>
	function myFunction() {
		var copyText = document.getElementById("myInput");
		copyText.select();
		copyText.setSelectionRange(0, 99999)
		document.execCommand("copy");
	}
</script>

</head>

<body>
	<div style="font-size: large">
		Copy / salin kode berikut :<br> <input id="myInput" type="text"
			size="140" readonly value="${params.code}" style="font-size: medium">
	</div>
	<br>
	<button onclick="myFunction()"><%= Common.getBahasaConfig("COPY / SALIN") %></button>
	<button type="button" onclick="window.close();"><%= Common.getBahasaConfig("TUTUP") %></button>
<jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body>
</html>

<%
	}
%>