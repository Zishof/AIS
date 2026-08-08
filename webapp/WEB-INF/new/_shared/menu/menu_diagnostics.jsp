<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %><%@ page import="ais.common.newui.menu.NewUiHybridMenuSnapshot" %>
<%NewUiHybridMenuSnapshot nuiDiag=(NewUiHybridMenuSnapshot)request.getAttribute("newUiHybridMenuSnapshot");if(Common.getApakahAdmin()&&nuiDiag!=null&&nuiDiag.getDiagnostics().hasWarnings()){%>
<details class="nui-card nui-menu-diagnostics"><summary>Diagnostik hierarchy menu</summary><pre><%=nuiDiag.getDiagnostics().summary()%></pre></details>
<%}%>
