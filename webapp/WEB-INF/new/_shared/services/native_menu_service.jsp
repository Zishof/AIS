<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="ais.common.newui.menu.NewUiUnavailableRouteRegistry" %>
<%@ page import="org.json.JSONObject" %>
<%
response.setHeader("Cache-Control","no-store");
response.setHeader("X-Content-Type-Options","nosniff");
NewUiUnavailableRouteRegistry.Entry unavailable=
        (NewUiUnavailableRouteRegistry.Entry)request.getAttribute("nui_unavailable_route");
if(unavailable!=null){
    response.setStatus(unavailable.getHttpStatus());
    out.print(new JSONObject().put("success",false).put("code",unavailable.getCode())
            .put("message",unavailable.getMessage()).put("evidence",unavailable.getEvidence()).toString());
}else{
    response.setStatus(501);
    out.print(new JSONObject().put("success",false).put("code","NATIVE_ADAPTER_NOT_CONFIGURED")
            .put("message","Java service New UI untuk menu ini belum dikonfigurasi.").toString());
}
%>
