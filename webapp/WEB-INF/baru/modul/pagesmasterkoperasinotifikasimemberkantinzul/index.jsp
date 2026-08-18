<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.DynamicTableGenerator" %>
<%
    // /pages/master/koperasi/notifikasi_member_kantin.zul
    String title = "Notifikasi Saya";
    String html = "<div style=\"font-family:Arial,sans-serif;padding:24px;\">" +
        "<h2 style=\"color:#2563eb;margin-bottom:8px;\">" + title + "</h2>" +
        "<div style=\"background:#f0f7ff;border:1px solid #bfdbfe;border-radius:8px;padding:16px;color:#1e40af;\">" +
        "<p style=\"margin:0;\"><strong>Halaman ini dikelola melalui tampilan ZK (ZUL).</strong></p>" +
        "<p style=\"margin:8px 0 0;\">Untuk tampilan penuh, gunakan menu <em>" + title + "</em> pada antarmuka utama.</p>" +
        "<p style=\"margin:8px 0 0;font-size:0.85em;color:#3b82f6;\">URL ZK: /pages/master/koperasi/notifikasi_member_kantin.zul</p>" +
        "</div></div>";
    out.println(html);
%>
