<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    if (!Boolean.TRUE.equals(request.getAttribute("DMS_SERVICE_ALLOWED"))) {
        response.sendError(403, "Service DMS hanya dapat dipanggil melalui servlet Document.");
        return;
    }
%>
<jsp:include page="/WEB-INF/baru/modul/dms/_dms_content.jsp"></jsp:include>
