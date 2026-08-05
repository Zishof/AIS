<%-- Laporan GUDANG = dasbor pergudangan dengan preset jenis "Gudang". Reuse laporan_pergudangan/index.jsp --%>
<% request.setAttribute("presetJenisNama", "Gudang");
   request.setAttribute("judulLaporan", "Laporan Gudang"); %>
<jsp:include page="/WEB-INF/baru/modul/kantin/laporan_pergudangan/index.jsp" />
