<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("genericCrudEntityKey", "ais.database.model.RuangPMB");
request.setAttribute("genericCrudModuleKey", "root");
request.setAttribute("genericCrudPageKey", "ruang_pmb");
pageContext.include("/WEB-INF/new/_shared/generic-crud/ui/crud_page.jsp", true);
%>
