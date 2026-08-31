<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("genericCrudEntityKey", "ais.database.model.sosial.SosialChannel");
request.setAttribute("genericCrudModuleKey", "sosial");
request.setAttribute("genericCrudPageKey", "sosial_channel");
request.getRequestDispatcher("/WEB-INF/new/_shared/generic-crud/services/dispatcher.jsp").forward(request, response);
%>
