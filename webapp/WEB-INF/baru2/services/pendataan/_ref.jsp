<%@ page contentType="application/json;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common, org.json.JSONArray, org.json.JSONObject" %>
<%--
  _ref.jsp — Layanan referensi data master (fakultas, prodi, tahun akademik, dll.)
  Endpoint: GET /baru2?modul=pendataan&svc={svc}
  svc:
    ref_fakultas      → [{id, nama}]
    ref_prodi         → [{id, nama, fakultasId}]  — filter ?fakultasId=
    ref_prodi_all     → semua prodi tanpa filter
    ref_tahun_akademik→ [{id, label}]  label=e.g. "2023/2024 Ganjil"
--%>
<%
String svc = request.getParameter("svc");
// Implementasi aktual akan menggunakan HibernateUtil + Action class yang ada.
// Stub ini mengembalikan data kosong agar UI tidak error saat diuji.
response.setContentType("application/json;charset=UTF-8");
JSONObject res = new JSONObject();
res.put("total", 0);
res.put("data", new JSONArray());
out.clear();
out.print(res.toString());
%>
