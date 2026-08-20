<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common" %>
<%
if (Common.getCurrentUser(request) == null) {
    response.sendError(403);
    return;
}
%>
<section class="repo-card repo-card-pad" role="status">
  <h2>Deposit dipindahkan ke konsol internal</h2>
  <p>Form generik lama telah dinonaktifkan karena mengirim nama kelas Java dan melewati pemeriksaan login pada endpoint mutasi.</p>
  <p>Gunakan menu <strong>Repository → Item Repository</strong> pada aplikasi internal. Portal publik ini tidak menjalankan mutasi data.</p>
</section>
