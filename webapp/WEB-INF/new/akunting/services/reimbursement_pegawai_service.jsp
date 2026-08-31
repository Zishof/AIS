<%--
    Adaptor native: Reimbursement Pegawai

    Sumber ZK   : /pages/master/akunting/reimbursement_pegawai.zul
    Entity      : ais.database.model.akunting.ReimbursementPegawai
    Pemetaan    : layar ZK mengelola satu entity reimbursement.
    Catatan     : memakai dispatcher scaffold (_shared/services/dispatcher.jsp)
                  agar definisi Generic CRUD didaftarkan otomatis lewat
                  tryAutoRegister; memanggil generic-crud/dispatcher langsung
                  menghasilkan ENTITY_NOT_REGISTERED.
--%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiServiceModule", "akunting");
request.setAttribute("nuiServicePage", "reimbursement_pegawai");
request.setAttribute("nuiServiceTitle", "Reimbursement Pegawai");
request.setAttribute("nuiServiceType", "list");
request.setAttribute("nuiServiceEntities", new String[]{"ReimbursementPegawai"});
%>
<jsp:include page="/WEB-INF/new/_shared/services/dispatcher.jsp" />
