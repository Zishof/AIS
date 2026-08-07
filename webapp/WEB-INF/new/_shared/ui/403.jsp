<%-- 403 Akses Ditolak — New UI. Dipakai route guard untuk permintaan UI tak terautorisasi. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% try { response.setStatus(403); } catch (Exception ignored) {} %>
<!doctype html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title>403 — Akses Ditolak</title>
    <style>
        body{margin:0;font-family:Segoe UI,Roboto,Arial,sans-serif;background:#0f172a;color:#e2e8f0;display:flex;min-height:100vh;align-items:center;justify-content:center}
        .box{max-width:520px;padding:40px;text-align:center}
        .code{font-size:64px;font-weight:800;letter-spacing:2px;color:#f87171;margin:0}
        h1{font-size:20px;margin:12px 0 8px}
        p{color:#94a3b8;line-height:1.6;margin:0}
    </style>
</head>
<body>
    <div class="box">
        <p class="code">403</p>
        <h1>Akses Ditolak</h1>
        <p>Anda tidak memiliki hak akses untuk membuka halaman ini pada peran yang sedang aktif.
           Silakan hubungi administrator apabila Anda merasa seharusnya berhak.</p>
    </div>
</body>
</html>
