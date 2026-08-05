<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // Contoh pemakaian paling singkat:
    out.println(ais.common.DynamicJspCrudGenerator.generate(ais.database.model.Agama.class));
%>