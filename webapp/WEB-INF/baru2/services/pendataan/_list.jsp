<%@ page contentType="application/json;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common, org.json.JSONArray, org.json.JSONObject" %>
<%--
  _list.jsp — Stub endpoint daftar data untuk semua modul pendataan.

  Semua svc _list diarahkan ke sini untuk sementara.
  Implementasi nyata: masing-masing Action/Helper yang sudah ada (MahasiswaAction,
  DosenAction, MatakuliahAction, dll.) akan diintegrasikan oleh pengembang backend.

  Konvensi respons JSON:
    {
      "total": <int>,          // total baris (sebelum paginasi)
      "data": [                // array objek per baris
        { "id": 1, "field1": "...", ... }
      ]
    }

  Filter diterima via GET params: q, nama, nim, kode, ta, smt, status, hal, ukuran
    hal    = nomor halaman (0-based), default 0
    ukuran = jumlah baris per halaman, default 20
--%>
<%
response.setContentType("application/json;charset=UTF-8");
JSONObject res = new JSONObject();
res.put("total", 0);
res.put("data", new JSONArray());
out.clear();
out.print(res.toString());
%>
