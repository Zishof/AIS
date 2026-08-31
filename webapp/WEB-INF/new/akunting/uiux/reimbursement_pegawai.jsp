<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("genericCrudEntityKey", "ais.database.model.akunting.ReimbursementPegawai");
request.setAttribute("genericCrudModuleKey", "akunting");
request.setAttribute("genericCrudPageKey", "reimbursement_pegawai");
pageContext.include("/WEB-INF/new/_shared/generic-crud/ui/crud_page.jsp", true);
%>
