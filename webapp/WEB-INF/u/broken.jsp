<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<%
Object linkUrl = request.getAttribute("javax.servlet.error.request_uri");
if (linkUrl != null && linkUrl.toString().endsWith("pdf")) {
%>
<title>File Tidak Ditemukan</title>
<%
} else {
%>
<title>Halaman Tidak Ditemukan</title>
<%
}
%>
<title>E-Campus</title>
<link rel="shortcut icon"
	href="<%=request.getContextPath()%>/img/logo.png" type="image/x-icon" />
<meta name="viewport"
	content="width=device-width, initial-scale=1, shrink-to-fit=no" />
<meta name="description" content="Sistem Informasi Akademik Ecampus" />
<meta name="author" content="Mohammad Fauzi Murtadho" />
</head>
<body style="text-align: center;">
	<br>
	<br>
	<br>
	<br>
	<br>
	<br>
	<%
	if (linkUrl != null && linkUrl.toString().endsWith("pdf")) {
	%>
	<img alt="" width="72px"
		src="<%=request.getContextPath()%>/img/pdf.png" onerror="this.style.display='none'">
	<br>
	<h3>File tidak dapat ditampilkan</h3>
	<p>File PDF tidak ditemukan atau tidak dapat diakses.<br>
	Silakan coba unduh file menggunakan tombol Download.</p>
	<%
	} else {
	response.sendRedirect(request.getContextPath());
	}
	%>
</body>
</html>


