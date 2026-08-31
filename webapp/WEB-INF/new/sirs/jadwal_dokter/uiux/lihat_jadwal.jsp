<%--
    Adaptor native: Jadwal Mingguan

    Sumber ZK   : /pages/master/sirs/jadwal_dokter/lihat_jadwal.zul
                  (composer CalendarLihatJadwalComposer)
    Entity      : ais.database.model.sirs.JadwalDokter
    Mode        : lihat
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
request.setAttribute("nuiPage", "lihat_jadwal");
request.setAttribute("nuiPageTitle", "Jadwal Mingguan");
request.setAttribute("nuiPageType", "list");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
