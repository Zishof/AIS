<%-- Laporan OUTLET = dasbor pergudangan dengan preset jenis "Outlet". Reuse laporan_pergudangan/index.jsp --%>
<% request.setAttribute("presetJenisNama", "Outlet");
   request.setAttribute("judulLaporan", "Laporan Outlet"); %>
<jsp:include page="/WEB-INF/baru/modul/kantin/laporan_pergudangan/index.jsp" />
