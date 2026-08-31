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
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiServiceModule", "sosial");
request.setAttribute("nuiServicePage", "sosial_channel");
request.setAttribute("nuiServiceTitle", "Kanal Sosial");
request.setAttribute("nuiServiceType", "list");
request.setAttribute("nuiServiceEntities", new String[]{"SosialChannel"});
%>
<jsp:include page="/WEB-INF/new/_shared/services/dispatcher.jsp" />
