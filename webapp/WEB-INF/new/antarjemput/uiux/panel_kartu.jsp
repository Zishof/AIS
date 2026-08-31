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
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "antarjemput");
request.setAttribute("nuiPage", "panel_kartu");
request.setAttribute("nuiPageTitle", "Kartu Penjemput");
request.setAttribute("nuiPageType", "list");
request.setAttribute("nuiEntityCandidates", new String[]{"KartuPenjemputAntarJemput"});
%>
<jsp:include page="/WEB-INF/new/_shared/ui/page.jsp" />
