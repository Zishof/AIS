<%--
    Adaptor native: Jadwal Antar Jemput

    Sumber ZK   : /pages/master/antarjemput/panel_jadwal.zul
    Entity      : ais.database.model.antarjemput.JadwalAntarJemput
    Pemetaan    : panel ZK ini mengelola satu entity saja, sehingga dilayani
                  scaffold Generic CRUD v2. Hak akses, validasi, dan audit
                  tetap ditegakkan server melalui dispatcher bersama.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("genericCrudEntityKey", "ais.database.model.antarjemput.JadwalAntarJemput");
request.setAttribute("genericCrudModuleKey", "antarjemput");
request.setAttribute("genericCrudPageKey", "panel_jadwal");
pageContext.include("/WEB-INF/new/_shared/generic-crud/ui/crud_page.jsp", true);
%>
