<%--
    Adaptor native: Persetujuan Kas Besar

    Sumber ZK   : /pages/master/akunting/persetujuan_dana_talangan.zul (PersetujuanDanaTalanganAction -> DanaTalanganAction)
    Kontrak     : ais.common.newui.akunting.NewUiPersetujuanAkuntingController (mode dana_talangan)
    Batas       : BACA SAJA. Menyetujui membangkitkan antrean pencairan dana,
                  menggerakkan disposisi SOP, dan memicu pencetakan dokumen.
                  Kontrak yang hanya menandai "disetujui" akan menghasilkan
                  pengajuan yang tampak lolos namun uangnya tidak pernah masuk
                  antrean transfer. Persetujuan tetap di layar lama.
    Catatan     : status pada entity ini nilai TURUNAN (dari kolom penyetuju dan
                  alur SOP), sehingga dibaca lewat entity, bukan SQL kolom.
--%>
<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.akunting.NewUiPersetujuanAkuntingController" %>
<%
NewUiPersetujuanAkuntingController.handle(request, response,
        NewUiPersetujuanAkuntingController.MODE_DANA_TALANGAN, "persetujuan_dana_talangan");
%>
