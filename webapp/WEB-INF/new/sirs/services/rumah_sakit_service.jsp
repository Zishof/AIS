<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiServiceModule", "sirs");
request.setAttribute("nuiServicePage", "rumah_sakit");
request.setAttribute("nuiServiceTitle", "Profil Fasilitas Kesehatan");
request.setAttribute("nuiServiceType", "master");
request.setAttribute("nuiServiceSourceClass", "RumahSakitAction");
request.setAttribute("nuiServiceSourcePackage", "ais.action.master.sirs");
request.setAttribute("nuiServiceSourcePath", "src/ais/action/master/sirs/RumahSakitAction.java");
request.setAttribute("nuiServiceMethods", new String[]{"initCriteria", "onSave", "render"});
request.setAttribute("nuiServiceEntities", new String[]{"RumahSakit"});
%>
<jsp:include page="/WEB-INF/new/_shared/services/dispatcher.jsp" />
