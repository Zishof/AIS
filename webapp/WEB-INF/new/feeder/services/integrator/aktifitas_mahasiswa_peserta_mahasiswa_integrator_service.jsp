<%--
    Adaptor native: Integrator Peserta Mahasiswa

    Sumber ZK   : ais.action.master.feeder.integrator.AktifitasMahasiswaPesertaMahasiswaIntegrator
    Kontrak     : ais.common.newui.feeder.NewUiFeederIntegratorController (mode aktifitas_mahasiswa_peserta)
    Catatan     : layar ini TIDAK menyentuh server Feeder. Panel Download
                  menyusun berkas .xlsx dari data lokal untuk diunggah operator
                  sendiri; panel Upload membaca berkas .xlsx kembali ke basis
                  data lokal. Kredensial Feeder hanya dipakai layar Ekspor ke
                  Feeder yang berbeda.
    Alur        : penyusunan berkas berjalan lama, sehingga dilayani sebagai
                  pekerjaan yang kemajuannya dicatat PekerjaanRegistry --
                  export_mulai, lalu detail untuk menanyakan kemajuan, lalu
                  export untuk mengunduh berkasnya.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.feeder.NewUiFeederIntegratorController" %>
<%
NewUiFeederIntegratorController.handle(request, response, "aktifitas_mahasiswa_peserta", "integrator/aktifitas_mahasiswa_peserta_mahasiswa_integrator");
%>
