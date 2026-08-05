<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>

<%
String accept = request.getParameter("accept") == null ? "" : request.getParameter("accept");
String pertemuanFileContent = request.getParameter("pertemuanFileContent") == null
		? ""
		: request.getParameter("pertemuanFileContent");

String uploadUrl = Common.getRequestHostWithProtocol(request);
if (!Common.getKonfigurasi("upload_scrorm_url", "").getNilai().trim().isEmpty()) {
	uploadUrl = Common.getKonfigurasi("upload_scrorm_url", "").getNilai().trim();
}
%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
.progress-wrapper {
	width: 100%;
}

.progress-wrapper .progress {
	background-color: green;
	width: 0%;
	padding: 5px 0px 5px 0px;
}
</style>
<script
	src="https://cdnjs.cloudflare.com/ajax/libs/jquery/3.2.1/jquery.js"></script>
<script>
	
	function postFile() {
		var formdata = new FormData();
		formdata.append('nama', $('#file1')[0].files[0].name);
		formdata.append('pertemuanFileContent', '<%=pertemuanFileContent%>');
		formdata.append('file1', $('#file1')[0].files[0]);

		var request = new XMLHttpRequest();

		request.upload.addEventListener('progress', function(e) {
			var file1Size = $('#file1')[0].files[0].size;

			if (e.loaded <= file1Size) {
				var percent = Math.round(e.loaded / file1Size * 100);
				$('#progress-bar-file1').width(percent + '%').html(
						percent + '%');
			}

			if (e.loaded == e.total) {
				$('#progress-bar-file1').width(100 + '%').html(100 + '%');
			}
		});

		request.open('post', '<%=uploadUrl%>/DoUpload');
		request.timeout = 45000;
		request.send(formdata);
	}
</script>
</head>
<body>
	<form id="form1">
		<input id="file1" type="file" <%=accept%> onchange="postFile();" />
		<div class="progress-wrapper">
			<div id="progress-bar-file1" class="progress"></div>
		</div>
	</form>
</body>
</html>