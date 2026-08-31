<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("genericCrudEntityKey", "ais.database.model.sosial.SosialChannel");
request.setAttribute("genericCrudModuleKey", "sosial");
request.setAttribute("genericCrudPageKey", "sosial_channel");
pageContext.include("/WEB-INF/new/_shared/generic-crud/ui/crud_page.jsp", true);
%>
