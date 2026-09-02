<%-- Kontrak native Ticketing; scope, komentar, dan mutasi dijaga controller. --%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.ticket.NewUiTicketController" %>
<%
NewUiTicketController.handle(request, response,
        NewUiTicketController.MODE_TICKETING);
%>
