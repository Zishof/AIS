<%--
    Adaptor native: Penjadwalan Kelas

    Sumber ZK   : /pages/master/kalender/perkuliahan/penjadwalan_kelas.zul
    Entity      : ais.database.model.Perkuliahan
    Pemetaan    : kalender perkuliahan adalah tampilan lain atas entity Perkuliahan; tampilan kalendernya belum direproduksi, datanya disajikan sebagai tabel.
    Catatan     : memakai dispatcher scaffold (_shared/services/dispatcher.jsp)
                  agar definisi Generic CRUD didaftarkan otomatis lewat
                  tryAutoRegister; memanggil generic-crud/dispatcher langsung
                  menghasilkan ENTITY_NOT_REGISTERED.
--%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiServiceModule", "kalender/perkuliahan");
request.setAttribute("nuiServicePage", "penjadwalan_kelas");
request.setAttribute("nuiServiceTitle", "Penjadwalan Kelas");
request.setAttribute("nuiServiceType", "list");
request.setAttribute("nuiServiceEntities", new String[]{"Perkuliahan"});
%>
<jsp:include page="/WEB-INF/new/_shared/services/dispatcher.jsp" />
