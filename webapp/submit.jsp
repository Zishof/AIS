<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>Harap tunggu, sedang menyiapkan data ....</title>
</head>
<body>
	<form name="inputForm" id="inputForm"
		action="<%=request.getParameter("url")%>" method="POST"
		target="_blank"></form>

	<script type="text/javascript">
		document.getElementById("inputForm").submit();
	</script>
</body>
</html>
