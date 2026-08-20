<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*" %>
<%@ page import="ais.common.Common" %>
<%@ page import="ais.action.master.repository.RepositoryPublicService.ItemDetail" %>
<%@ page import="org.json.*" %>
<%!
private String repoHtml(String value) {
    if (value == null) return "";
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
}
%>
<%
boolean fragment = "true".equalsIgnoreCase(request.getParameter("hanya_tampil_jsp"));
String fragmentName = request.getParameter("s") == null ? "" : request.getParameter("s").trim();
if (fragment) {
    ais.database.model.Tbmuser fragmentUser = Common.getCurrentUser(request);
    if (fragmentUser == null || fragmentUser.getUserId() == null) {
        response.sendError(403, "Login diperlukan untuk workspace repository.");
        return;
    }
    if ("FormRepository".equals(fragmentName)) {
%><jsp:include page="/WEB-INF/baru/modul/repository/FormRepository.jsp" /><%
    } else if ("_repo_collection".equals(fragmentName)) {
%><jsp:include page="/WEB-INF/baru/modul/repository/_repo_collection.jsp" /><%
    } else {
        response.sendError(404);
    }
    return;
}

String context = request.getContextPath();
String institution = "Institusi Pendidikan";
String logo = "";
try {
    ais.database.model.PerguruanTinggi ptRepo = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
    if (ptRepo != null && ptRepo.getNama() != null) institution = ptRepo.getNama();
    logo = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
} catch (Exception ignored) {
    ais.common.ErrorAuditUtil.record(ignored, "repository public institution header");
}
String view = request.getAttribute("repoView") == null ? "home" : String.valueOf(request.getAttribute("repoView"));
ItemDetail seoItem = (ItemDetail) request.getAttribute("repoItem");
String pageTitle = seoItem == null ? "Repositori Institusi - " + institution : seoItem.title + " - " + institution;
String origin = request.getScheme() + "://" + request.getServerName() + ((request.getServerPort()==80||request.getServerPort()==443)?"":":"+request.getServerPort());
String canonical = seoItem == null ? origin + context + "/repository" : origin + context + "/repository/item/" + seoItem.id;
%>
<!doctype html>
<html lang="id">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <meta name="theme-color" content="#0b3155">
  <meta name="description" content="<%=repoHtml(seoItem == null ? "Repositori karya ilmiah, publikasi, bahan ajar, dan pengetahuan institusi." : seoItem.abstractText)%>">
  <link rel="canonical" href="<%=repoHtml(canonical)%>">
  <% if (logo != null && logo.trim().length() > 0) { %><link rel="icon" href="<%=repoHtml(logo)%>"><% } %>
  <title><%=repoHtml(pageTitle)%></title>
  <% if (seoItem != null) { %>
  <meta name="citation_title" content="<%=repoHtml(seoItem.title)%>">
  <% String[] seoAuthors = seoItem.authors == null ? new String[0] : seoItem.authors.split(";");
     for (int ai = 0; ai < seoAuthors.length; ai++) { if (seoAuthors[ai].trim().length() > 0) { %>
  <meta name="citation_author" content="<%=repoHtml(seoAuthors[ai].trim())%>">
  <% }} %>
  <meta name="citation_publication_date" content="<%=repoHtml(seoItem.year)%>">
  <meta name="citation_language" content="<%=repoHtml(seoItem.language)%>">
  <% if (seoItem.doi != null && seoItem.doi.length() > 0) { %><meta name="citation_doi" content="<%=repoHtml(seoItem.doi)%>"><% } %>
  <% if (!seoItem.files.isEmpty()) { %><meta name="citation_pdf_url" content="<%=repoHtml(request.getRequestURL().toString() + "?action=download&id=" + seoItem.files.get(0).id)%>"><% } %>
  <meta name="DC.identifier" content="<%=repoHtml(seoItem.oaiIdentifier)%>">
  <meta property="og:type" content="article">
  <meta property="og:title" content="<%=repoHtml(seoItem.title)%>">
  <meta property="og:description" content="<%=repoHtml(seoItem.abstractText)%>">
  <meta property="og:url" content="<%=repoHtml(canonical)%>">
  <meta property="og:site_name" content="<%=repoHtml(institution)%>"><meta name="twitter:card" content="summary"><meta name="twitter:title" content="<%=repoHtml(seoItem.title)%>"><meta name="twitter:description" content="<%=repoHtml(seoItem.abstractText)%>">
  <% String schemaType="ScholarlyArticle";if("Book".equalsIgnoreCase(seoItem.documentType))schemaType="Book";else if(seoItem.documentType.toLowerCase().contains("thesis"))schemaType="Thesis";else if(seoItem.documentType.toLowerCase().contains("dataset"))schemaType="Dataset";JSONObject ld=new JSONObject();ld.put("@context","https://schema.org");ld.put("@type",schemaType);ld.put("name",seoItem.title);ld.put("abstract",seoItem.abstractText);ld.put("datePublished",seoItem.year);ld.put("inLanguage",seoItem.language);ld.put("identifier",seoItem.doi.length()>0?seoItem.doi:seoItem.oaiIdentifier);ld.put("url",canonical);JSONArray ldAuthors=new JSONArray();for(String author:seoAuthors)if(author.trim().length()>0)ldAuthors.put(new JSONObject().put("@type","Person").put("name",author.trim()));ld.put("author",ldAuthors); %>
  <script type="application/ld+json"><%=ld.toString().replace("</","<\\/")%></script>
  <% } %>
  <jsp:include page="/WEB-INF/baru/modul/repository/_repository_theme.jsp" />
</head>
<body>
  <jsp:include page="/WEB-INF/baru/modul/repository/ListRepository.jsp" />
  <script src="<%=context%>/js/repository-modern.js" defer></script>
  <script>try{var c=getComputedStyle(document.documentElement).getPropertyValue('--theme-primary').trim();if(c){document.querySelector('meta[name="theme-color"]').setAttribute('content',c);}}catch(e){}</script>
</body>
</html>
