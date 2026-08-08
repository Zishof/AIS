<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("genericCrudEntityKey", "ais.database.model.Mahasiswa");
request.setAttribute("genericCrudModuleKey", "root");
request.setAttribute("genericCrudPageKey", "mahasiswa");
request.getRequestDispatcher("/WEB-INF/new/_shared/generic-crud/services/dispatcher.jsp").forward(request, response);
%>
