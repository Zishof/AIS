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
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "kalender/perkuliahan");
request.setAttribute("nuiPage", "penjadwalan_kelas");
request.setAttribute("nuiPageTitle", "Penjadwalan Kelas");
request.setAttribute("nuiPageType", "list");
request.setAttribute("nuiEntityCandidates", new String[]{"Perkuliahan"});
%>
<jsp:include page="/WEB-INF/new/_shared/ui/page.jsp" />
