<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
long start = System.currentTimeMillis();
String trace = System.currentTimeMillis() + "-" + Math.abs(request.hashCode());
System.out.println("[INDEX-CHECK] trace=" + trace + " elapsed=0ms START uri=" + request.getRequestURI() + " query=" + request.getQueryString());
response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
response.setHeader("Pragma", "no-cache");
%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Index Check</title>
    <style>
        body{font-family:Arial,sans-serif;background:#f8fafc;color:#0f172a;margin:0;padding:36px;}
        .card{max-width:780px;margin:auto;background:#fff;border:1px solid #e2e8f0;border-radius:18px;padding:28px;box-shadow:0 18px 50px rgba(15,23,42,.08)}
        .ok{display:inline-block;background:#dcfce7;color:#166534;border-radius:999px;padding:6px 12px;font-weight:bold}
        code{background:#f1f5f9;border-radius:8px;padding:3px 7px;}
    </style>
</head>
<body>
    <div class="card">
        <span class="ok">OK</span>
        <h1>INDEX JSP CHECK BERHASIL</h1>
        <p>Jika halaman ini cepat, maka Apache, SSL, AJP, Tomcat, dan forward servlet berjalan normal.</p>
        <p>Trace: <code><%= trace %></code></p>
        <p>Context: <code><%= request.getContextPath() %></code></p>
        <p>URI: <code><%= request.getRequestURI() %></code></p>
        <p>Server: <code><%= request.getServerName() %>:<%= request.getServerPort() %></code></p>
        <p>Waktu render server: <code><%= (System.currentTimeMillis() - start) %> ms</code></p>
    </div>
</body>
</html>
<%
System.out.println("[INDEX-CHECK] trace=" + trace + " elapsed=" + (System.currentTimeMillis() - start) + "ms FINISH committed=" + response.isCommitted());
%>
