<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<div class="text-end mb-2"><a class="btn btn-success btn-sm me-2" href="<%=request.getContextPath()%>/baru?p=apotik&amp;s=help&amp;mode=qa&amp;menu=apotik_racikan"><i class="fas fa-comments"></i> Tanya Jawab</a><a class="btn btn-warning btn-sm" href="<%=request.getContextPath()%>/baru?p=apotik&amp;s=help&amp;menu=apotik_racikan"><i class="fas fa-question-circle"></i> Bantuan</a></div>
<% out.println(ais.common.DynamicJspCrudGenerator.generate(ais.database.model.sirs.Racikan.class)); %>
