<%@ page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %><%@ page import="ais.common.newui.laporan.NewUiLaporanSirsController" %>
<%
NewUiLaporanSirsController.handle(request, response, "inventory_pemakaian_retur", "format1/sirs/inventory/laporan_pemakaian_retur_item");
%>
