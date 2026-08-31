<%--
    Adaptor native: Penjadwalan Dosen

    Sumber ZK   : /pages/master/kalender/perkuliahan/penjadwalan_dosen.zul
                  (composer CalendarPerkuliahanDosenComposer)
    Entity      : ais.database.model.Perkuliahan
    Pemetaan    : kedua layar penjadwalan perkuliahan adalah kalender atas
                  entity Perkuliahan; sumbu pengelompokannya saja yang berbeda
                  (per dosen). Data yang sama dilayani scaffold Generic CRUD.
    Batas       : tampilan kalender beserta interaksi geser-taruh belum
                  direproduksi; halaman ini menyajikan datanya sebagai tabel
                  yang dapat disaring. Penjadwalan ulang tetap melalui layar ZK.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("genericCrudEntityKey", "ais.database.model.Perkuliahan");
request.setAttribute("genericCrudModuleKey", "kalender/perkuliahan");
request.setAttribute("genericCrudPageKey", "penjadwalan_dosen");
request.getRequestDispatcher("/WEB-INF/new/_shared/generic-crud/services/dispatcher.jsp")
        .forward(request, response);
%>
