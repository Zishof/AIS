<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common" %>
<%
if (Common.getCurrentUser(request) == null) {
    response.sendError(403);
    return;
}
%>
<section class="repo-card repo-card-pad" role="status">
  <h2>Koleksi dikelola dari konsol internal</h2>
  <p>Editor koleksi generik lama telah dinonaktifkan untuk menutup mutasi berbasis nama kelas dari browser.</p>
  <p>Gunakan tab <strong>Collection</strong> pada konsol ZK Repository untuk melihat koleksi dan status sinkronisasinya.</p>
</section>
