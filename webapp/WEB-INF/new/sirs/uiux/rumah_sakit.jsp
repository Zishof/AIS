<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
request.setAttribute("nuiModule", "sirs");
request.setAttribute("nuiModuleLabel", "SIRS / Klinik / Rumah Sakit");
request.setAttribute("nuiPage", "rumah_sakit");
request.setAttribute("nuiPageTitle", "Profil Fasilitas Kesehatan");
request.setAttribute("nuiPageType", "master");
request.setAttribute("nuiPageDescription", "Kelola profil, domain, dan tema Rumah Sakit, Puskesmas, Posyandu, Klinik, serta fasilitas kesehatan lainnya.");
request.setAttribute("nuiSourceClass", "RumahSakitAction");
request.setAttribute("nuiSourcePackage", "ais.action.master.sirs");
request.setAttribute("nuiSourcePath", "src/ais/action/master/sirs/RumahSakitAction.java");
request.setAttribute("nuiSourceKind", "class");
request.setAttribute("nuiSourceExtends", "GenericCrudAction<RumahSakit>");
request.setAttribute("nuiSourceMethods", new String[]{"initCriteria", "onSave", "render"});
request.setAttribute("nuiEntityCandidates", new String[]{"RumahSakit"});
%>
<jsp:include page="/WEB-INF/new/_shared/ui/page.jsp" />
