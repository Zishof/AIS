<%@page import="ais.action.servlet.Api"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
// Bridge halaman mobile -> web session kantin.
// Flutter membuka URL ini dengan token mobile, lalu diredirect ke kantin.
//
// Parameter opsional "next" memilih halaman tujuan. Nilainya TIDAK dipakai
// apa adanya: hanya nama yang dikenali di bawah yang diterima, supaya
// parameter ini tidak bisa dipakai mengalihkan pengguna ke alamat lain
// (open redirect).
String token = request.getParameter("token");

if (token != null && !token.trim().isEmpty()) {
    Object userObj = Api.tokens.get(token.trim());
    if (userObj instanceof Tbmuser) {
        Tbmuser tbmuser = (Tbmuser) userObj;
        request.getSession(true).setAttribute("mytbmuser", tbmuser);
        request.getSession(true).setAttribute("usersTemp", tbmuser);
    }
}

String next = request.getParameter("next");
String tujuan = "/baru?p=kantin/member&s=landing_page";
if ("topup".equals(next)) {
    tujuan = "/baru?p=kantin/member&s=_topup";
} else if ("va".equals(next)) {
    tujuan = "/baru?p=kantin/member&s=_transaksi_va";
} else if ("notifikasi".equals(next)) {
    tujuan = "/baru?p=kantin/member&s=notifikasi";
}

response.sendRedirect(request.getContextPath() + tujuan);
%>
