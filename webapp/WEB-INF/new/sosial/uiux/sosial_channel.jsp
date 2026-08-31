<%--
    Adaptor native: Kanal Sosial

    Sumber ZK   : /pages/master/sosial/sosial_channel.zul
    Entity      : ais.database.model.sosial.SosialChannel
    Pemetaan    : layar ZK mengelola satu entity kanal donasi.
    Catatan     : memakai dispatcher scaffold (_shared/services/dispatcher.jsp)
                  agar definisi Generic CRUD didaftarkan otomatis lewat
                  tryAutoRegister; memanggil generic-crud/dispatcher langsung
                  menghasilkan ENTITY_NOT_REGISTERED.
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "sosial");
request.setAttribute("nuiPage", "sosial_channel");
request.setAttribute("nuiPageTitle", "Kanal Sosial");
request.setAttribute("nuiPageType", "list");
request.setAttribute("nuiEntityCandidates", new String[]{"SosialChannel"});
%>
<jsp:include page="/WEB-INF/new/_shared/ui/page.jsp" />
