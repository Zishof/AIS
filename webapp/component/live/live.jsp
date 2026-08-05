<%@page import="ais.common.Common"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<html><head>
<meta http-equiv="content-type" content="text/html; charset=UTF-8">
	<script type="text/javascript" src="flowplayer-3.2.11.min.js"></script>
	<link rel="stylesheet" type="text/css" href="style.css">
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
		<div id="wowza" style="width:100%;height:100%;margin:0 auto;text-align:center">
		</div>
		
		<script>
		
		$f("wowza", "flowplayer-3.2.15.swf", {
		    clip: {
		        url: '<%=kodeStream%>',
		        scaling: 'fit',
			live: true,
			autoPlay: true,
		        provider: 'hddn'
		    },
		    plugins: {
		        hddn: {
		            url: "flowplayer.rtmp-3.2.11.swf",
					netConnectionUrl: '<%=urlRtmp%>'
		        }
		    },
		    canvas: {
		        backgroundGradient: 'none'
		    }
		});
		$f("wowza").play();
		</script>
	<%
	}
	%>

</a>

</body></html>
