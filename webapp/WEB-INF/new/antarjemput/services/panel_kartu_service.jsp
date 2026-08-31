<%--
    Adaptor native: Kartu Penjemput

    Sumber ZK   : /pages/master/antarjemput/panel_kartu.zul
    Entity      : ais.database.model.antarjemput.KartuPenjemputAntarJemput
    Pemetaan    : panel ZK ini mengelola satu entity saja, sehingga dilayani Generic CRUD.
    Catatan     : memakai dispatcher scaffold (_shared/services/dispatcher.jsp)
                  agar definisi Generic CRUD didaftarkan otomatis lewat
                  tryAutoRegister; memanggil generic-crud/dispatcher langsung
                  menghasilkan ENTITY_NOT_REGISTERED.
--%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiServiceModule", "antarjemput");
request.setAttribute("nuiServicePage", "panel_kartu");
request.setAttribute("nuiServiceTitle", "Kartu Penjemput");
request.setAttribute("nuiServiceType", "list");
request.setAttribute("nuiServiceEntities", new String[]{"KartuPenjemputAntarJemput"});
%>
<jsp:include page="/WEB-INF/new/_shared/services/dispatcher.jsp" />
