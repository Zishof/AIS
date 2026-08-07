<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("genericCrudEntityKey", "ais.database.model.Agama");
request.setAttribute("genericCrudModuleKey", "root");
request.setAttribute("genericCrudPageKey", "agama");
request.getRequestDispatcher("/WEB-INF/new/_shared/generic-crud/services/dispatcher.jsp").forward(request, response);
%>
