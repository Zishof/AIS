<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%-- Penerimaan Barang/Jasa (BAST) — integrasi langsung ke modul JSP kantin (kulakan).
     Saat disetujui, stok otomatis bertambah (DetailTransaksiAsset + sinkron kantin).
     Toko/Pedagang opsional untuk admin; otomatis terkunci bila login sebagai pedagang. --%>
<jsp:include page="/WEB-INF/baru/modul/kantin/kulakan/penerimaan_asset.jsp" />
