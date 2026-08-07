<%-- 404 Tidak Ditemukan — New UI. Dipakai untuk route yang tidak terdaftar/tidak ada. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% try { response.setStatus(404); } catch (Exception ignored) {} %>
<!doctype html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title>404 — Tidak Ditemukan</title>
    <style>
        body{margin:0;font-family:Segoe UI,Roboto,Arial,sans-serif;background:#0f172a;color:#e2e8f0;display:flex;min-height:100vh;align-items:center;justify-content:center}
        .box{max-width:520px;padding:40px;text-align:center}
        .code{font-size:64px;font-weight:800;letter-spacing:2px;color:#38bdf8;margin:0}
        h1{font-size:20px;margin:12px 0 8px}
        p{color:#94a3b8;line-height:1.6;margin:0}
    </style>
</head>
<body>
    <div class="box">
        <p class="code">404</p>
        <h1>Halaman Tidak Ditemukan</h1>
        <p>Route yang diminta tidak terdaftar atau halamannya tidak tersedia.</p>
    </div>
</body>
</html>
