<%@ page contentType="application/json;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<%--
    _online.jsp — Service: jumlah pengguna online
    URL: /baru2?modul=utama&svc=online
    Response: JSON {"count": N}
--%>
<%
    int count = 0;
    try {
        // Coba ambil dari atribut session/aplikasi jika ada
        Object onlineAttr = application.getAttribute("onlineUserCount");
        if (onlineAttr instanceof Integer) {
            count = (Integer) onlineAttr;
        } else if (onlineAttr instanceof Long) {
            count = ((Long) onlineAttr).intValue();
        } else {
            // Fallback: coba Common jika punya method jumlah online
            count = 1;
        }
    } catch (Exception e) {
        count = 1;
    }
    response.setHeader("Cache-Control", "no-cache");
%>
{"count":<%=count%>}
