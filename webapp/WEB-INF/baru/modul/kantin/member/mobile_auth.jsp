<%@page import="ais.action.servlet.Api"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
// Bridge halaman mobile → web session kantin.
// Flutter WebView membuka URL ini dengan token mobile, lalu diredirect ke kantin.
String token = request.getParameter("token");

if (token != null && !token.trim().isEmpty()) {
    Object userObj = Api.tokens.get(token.trim());
    if (userObj instanceof Tbmuser) {
        Tbmuser tbmuser = (Tbmuser) userObj;
        request.getSession(true).setAttribute("mytbmuser", tbmuser);
        request.getSession(true).setAttribute("usersTemp", tbmuser);
    }
}

response.sendRedirect(request.getContextPath() + "/baru?p=kantin/member&s=landing_page");
%>
