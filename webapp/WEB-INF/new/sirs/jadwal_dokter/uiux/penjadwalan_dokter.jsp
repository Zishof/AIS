<%--
    Adaptor native: Penjadwalan Tenaga Medis

    Sumber ZK   : /pages/master/sirs/jadwal_dokter/penjadwalan_dokter.zul
                  (composer CalendarJadwalDokterComposer)
    Entity      : ais.database.model.sirs.JadwalDokter
    Mode        : dokter
    Pemetaan    : keenam layar jadwal memakai entity yang sama dan hanya berbeda
                  pengelompokan serta rentang waktu, sehingga dilayani satu
                  controller NewUiJadwalDokterController dengan mode berbeda.
    Batas       : kontrak ini HANYA membaca jadwal. Kalender ZK aslinya
                  interaktif (geser-taruh, sunting sel); penyuntingan tetap di
                  layar ZK sampai editor kalender native beserta validasi
                  bentrok jadwal tersedia.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "sirs/jadwal_dokter");
request.setAttribute("nuiPage", "penjadwalan_dokter");
request.setAttribute("nuiPageTitle", "Penjadwalan Tenaga Medis");
request.setAttribute("nuiPageType", "list");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
