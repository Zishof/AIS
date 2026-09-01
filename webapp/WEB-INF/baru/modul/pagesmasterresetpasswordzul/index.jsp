<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
String actionNative = request.getParameter("action");
if (actionNative != null) {
    response.setStatus(409);
    out.print("{\"success\":false,\"code\":\"RESET_PASSWORD_SPECIAL_ENDPOINT\","
            + "\"message\":\"Reset password wajib memakai endpoint khusus dan tidak boleh membuka CRUD pengguna mentah.\"}");
    return;
}
out.println(ais.common.DynamicJspCrudGenerator.generate(ais.database.model.Tbmuser.class));
%>
