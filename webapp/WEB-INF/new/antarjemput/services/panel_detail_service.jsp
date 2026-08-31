<%--
    Adaptor native: Detail Penjemputan

    Sumber ZK   : /pages/master/antarjemput/panel_detail.zul
    Entity      : ais.database.model.antarjemput.DetailPenjemputanAntarJemput
    Pemetaan    : panel ZK ini mengelola satu entity saja, sehingga dilayani
                  scaffold Generic CRUD v2. Hak akses, validasi, dan audit
                  tetap ditegakkan server melalui dispatcher bersama.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("genericCrudEntityKey", "ais.database.model.antarjemput.DetailPenjemputanAntarJemput");
request.setAttribute("genericCrudModuleKey", "antarjemput");
request.setAttribute("genericCrudPageKey", "panel_detail");
request.getRequestDispatcher("/WEB-INF/new/_shared/generic-crud/services/dispatcher.jsp")
        .forward(request, response);
%>
