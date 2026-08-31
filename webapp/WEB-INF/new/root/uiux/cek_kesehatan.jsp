<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("genericCrudEntityKey", "ais.database.model.CekKesehatan");
request.setAttribute("genericCrudModuleKey", "root");
request.setAttribute("genericCrudPageKey", "cek_kesehatan");
pageContext.include("/WEB-INF/new/_shared/generic-crud/ui/crud_page.jsp", true);
%>
