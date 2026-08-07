<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="ais.common.Common" %>
<%@ page import="ais.action.master.generic.v2.GenericCrudAutoDefinitionFactory" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%!
private String mcH(Object value){if(value==null)return "";return String.valueOf(value).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
%>
<%
if(!Common.getApakahAdmin()){response.setStatus(403);%><div class="nui-card nui-card-pad"><h2>Akses ditolak</h2><p>Model Catalog hanya tersedia untuk Super Admin.</p></div><%return;}
List models=GenericCrudAutoDefinitionFactory.listAdministrativeModels();
String base=request.getContextPath()+"/new?frame=1&module=generic&page=model_crud&entity=";
%>
<section class="nui-page">
  <header class="nui-page-head"><div><div class="nui-breadcrumb">Sistem / Generic CRUD</div><h1 class="nui-page-title">Model CRUD Catalog</h1><p class="nui-page-desc"><%=models.size()%> model Hibernate yang lolos allow-list. Model credential, token, file, audit, log, queue, dan pembayaran diblokir otomatis.</p></div></header>
  <article class="nui-card"><div class="nui-toolbar"><input class="nui-input" type="search" data-model-search placeholder="Cari model, package, atau tabel…"><span class="nui-chip"><%=models.size()%> model</span></div>
    <div class="nui-table-wrap"><table class="nui-table"><thead><tr><th>Model</th><th>Package</th><th>Entity Hibernate</th><th>Aksi</th></tr></thead><tbody data-model-rows>
    <%for(int i=0;i<models.size();i++){Map model=(Map)models.get(i);String key=String.valueOf(model.get("entityKey"));String search=(String.valueOf(model.get("displayName"))+" "+key+" "+model.get("tableName")).toLowerCase();%>
      <tr data-model-row data-search="<%=mcH(search)%>"><td><strong><%=mcH(model.get("displayName"))%></strong><br><small><%=mcH(key)%></small></td><td><%=mcH(model.get("packageName"))%></td><td><%=mcH(model.get("tableName"))%></td><td><a class="nui-btn nui-btn-primary" href="<%=mcH(base+URLEncoder.encode(key,"UTF-8"))%>">Buka CRUD</a></td></tr>
    <%}%></tbody></table></div>
  </article>
</section>
<script>(function(){var input=document.querySelector('[data-model-search]');if(!input)return;input.addEventListener('input',function(){var q=input.value.toLowerCase(),rows=document.querySelectorAll('[data-model-row]');for(var i=0;i<rows.length;i++)rows[i].hidden=rows[i].getAttribute('data-search').indexOf(q)<0;});}());</script>
