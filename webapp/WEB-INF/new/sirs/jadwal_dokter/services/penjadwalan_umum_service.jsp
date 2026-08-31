<%--
    Adaptor native: Kalender Penjadwalan

    Sumber ZK   : /pages/master/sirs/jadwal_dokter/penjadwalan_umum.zul
                  (composer CalendarJadwalUmumComposer)
    Entity      : ais.database.model.sirs.JadwalDokter
    Mode        : umum
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
        NewUiJadwalDokterController.MODE_UMUM, "penjadwalan_umum");
%>
