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
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.sirs.NewUiJadwalDokterController" %>
<%
NewUiJadwalDokterController.handle(request, response,
        NewUiJadwalDokterController.MODE_DOKTER, "penjadwalan_dokter");
%>
