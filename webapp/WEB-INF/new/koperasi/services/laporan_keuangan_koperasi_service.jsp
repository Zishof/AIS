<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="ais.common.newui.NewUiRouteGuard" %>
<%@ page import="ais.common.newui.menu.NewUiKoperasiFinancialService" %>
<%@ page import="ais.common.newui.menu.NewUiKoperasiFinancialService.Summary" %>
<%
response.setHeader("Cache-Control","no-store");String action=request.getParameter("action");if(action==null)action="detail";
if(!NewUiRouteGuard.isActionAuthorized(request,"koperasi","laporan_keuangan_koperasi",action)){response.setStatus(403);out.print("{\"ok\":false,\"code\":\"ACTION_FORBIDDEN\"}");return;}
try{Summary value=NewUiKoperasiFinancialService.load();if("export".equalsIgnoreCase(action)){response.reset();response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");response.setHeader("Content-Disposition","attachment; filename=laporan_keuangan_koperasi.xlsx");NewUiKoperasiFinancialService.writeWorkbook(value,response.getOutputStream());return;}JSONObject data=new JSONObject();data.put("totalAset",value.totalAset);data.put("kas",value.kas);data.put("piutang",value.outstandingPokok);data.put("modal",value.modalSendiri);data.put("kewajiban",value.simpananSukarela);data.put("pendapatanJasa",value.jasaDiterima);data.put("arusKasBersih",value.arusKasBersih);data.put("ppap",value.totalPpap);JSONObject root=new JSONObject();root.put("ok",true);root.put("data",data);out.print(root.toString());}catch(Exception error){response.setStatus(500);try{ais.common.ErrorAuditUtil.record(error,"New UI service laporan keuangan koperasi");}catch(Exception ignored){}JSONObject root=new JSONObject();root.put("ok",false);root.put("code","REPORT_FAILED");root.put("message",error.getMessage()==null?"Laporan gagal dimuat.":error.getMessage());out.print(root.toString());}
%>
