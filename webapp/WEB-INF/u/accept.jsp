
<%@page import="ais.common.gdrive.GDriveUtilPerPengguna"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>

<%
	// Simpan authorization code ke tabel gdrive_code (shared antar node) + map memori. Perbaikan
	// multi-node: callback ini bisa mendarat di node berbeda dari halaman pengguna, jadi code harus
	// dipersistkan ke DB agar timer di node lain menemukannya. simpanCodeDrive juga mengisi map memori
	// untuk kasus single-node / node yang sama.
	GDriveUtilPerPengguna.simpanCodeDrive(request.getParameter("u"), request.getParameter("code"));
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