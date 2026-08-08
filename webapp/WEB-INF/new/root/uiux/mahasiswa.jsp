<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("genericCrudEntityKey", "ais.database.model.Mahasiswa");
request.setAttribute("genericCrudModuleKey", "root");
request.setAttribute("genericCrudPageKey", "mahasiswa");
pageContext.include("/WEB-INF/new/_shared/generic-crud/ui/crud_page.jsp", true);
%>
