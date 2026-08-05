<!DOCTYPE HTML>
<%@page import="ais.common.Common"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<html><head>
<meta http-equiv="content-type" content="text/html; charset=UTF-8">
	<script type="text/javascript" src="jwplayer.js"></script>
	<title><%=request.getParameter("app")%></title>
	
	<style type="text/css">
	
		html { 
		  background: url('<%=request.getContextPath()%>/img/flash_dibolehkan.jpg') no-repeat center center fixed; 
		  -webkit-background-size: cover;
		  -moz-background-size: cover;
		  -o-background-size: cover;
		  background-size: cover;
		}
	
	</style>
	
	
</head>
<body>
<%
String urlRtmp = request.getParameter("urlRtmp");
String alamatStream = StringUtils.replace(urlRtmp, "rtmp", "http");
String kodeStream = request.getParameter("app");
String urlDownload = alamatStream + "/" + kodeStream;
%>
<a href="<%=urlDownload%>">
	<%
	if(Common.isMobile(request)){
		%>
		<img alt="" src="<%=request.getContextPath()%>/img/Button-Play-icon_rtmp.png"> 
	<%
	} else {
	%>
		<div id='player'></div>
	    <script>
		var player = jwplayer('player');
		player.setup({
	            'flashplayer': 'player.swf',
	            'id': 'player',
	            'width': '100%',
	            'height': <%=request.getParameter("height")==null?"340" : "'"+request.getParameter("height")+"'"%>,
	            'autoplay': <%=request.getParameter("autoplay")==null?"false" : request.getParameter("autoplay")%>,
	            'streamer': '<%=request.getParameter("urlRtmp")%>',
	            'file': '<%=request.getParameter("app")%>',
	            'image':'<%=request.getContextPath()%>/img/logo.png'
	        });
	    </script>
	<%
	}
	%>

</a>

</body></html>
