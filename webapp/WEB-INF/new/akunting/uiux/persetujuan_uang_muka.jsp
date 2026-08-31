<%--
    Adaptor native: Persetujuan Uang Muka

    Sumber ZK   : /pages/master/akunting/persetujuan_uang_muka.zul (PersetujuanUangMukaAction -> UangMukaAction)
    Kontrak     : ais.common.newui.akunting.NewUiPersetujuanAkuntingController (mode uang_muka)
    Batas       : BACA SAJA. Menyetujui membangkitkan antrean pencairan dana,
                  menggerakkan disposisi SOP, dan memicu pencetakan dokumen.
                  Kontrak yang hanya menandai "disetujui" akan menghasilkan
                  pengajuan yang tampak lolos namun uangnya tidak pernah masuk
                  antrean transfer. Persetujuan tetap di layar lama.
    Catatan     : status pada entity ini nilai TURUNAN (dari kolom penyetuju dan
                  alur SOP), sehingga dibaca lewat entity, bukan SQL kolom.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "akunting");
request.setAttribute("nuiModuleLabel", "Akuntansi & Keuangan");
request.setAttribute("nuiPage", "persetujuan_uang_muka");
request.setAttribute("nuiPageTitle", "Persetujuan Uang Muka");
request.setAttribute("nuiPageType", "list");
pageContext.include("/WEB-INF/new/_shared/ui/page.jsp", true);
%>
