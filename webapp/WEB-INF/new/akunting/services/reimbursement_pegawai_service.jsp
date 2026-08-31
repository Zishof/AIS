<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("genericCrudEntityKey", "ais.database.model.akunting.ReimbursementPegawai");
request.setAttribute("genericCrudModuleKey", "akunting");
request.setAttribute("genericCrudPageKey", "reimbursement_pegawai");
request.getRequestDispatcher("/WEB-INF/new/_shared/generic-crud/services/dispatcher.jsp").forward(request, response);
%>
