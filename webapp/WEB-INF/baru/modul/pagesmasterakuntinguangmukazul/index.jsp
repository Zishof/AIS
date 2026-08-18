<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    out.println(ais.common.DynamicJspCrudGenerator.generate(ais.database.model.akunting.UangMuka.class));
%>
