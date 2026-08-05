<%@ page contentType="application/json;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common, org.json.JSONObject" %>
<%--
  _aksi.jsp — Stub endpoint aksi POST untuk semua modul pendataan.

  svc yang ditangani: *_tambah, *_ubah, *_hapus, *_setujui, *_proses, dst.

  Konvensi respons JSON:
    { "ok": true,  "msg": "Berhasil disimpan" }
    { "ok": false, "msg": "Deskripsi error" }

  Implementasi nyata: delegasikan ke Action class yang sesuai:
    mahasiswa_tambah → MahasiswaAction.simpan(params)
    mahasiswa_ubah   → MahasiswaAction.ubah(id, params)
    mahasiswa_hapus  → MahasiswaAction.hapus(id)
--%>
<%
response.setContentType("application/json;charset=UTF-8");
JSONObject res = new JSONObject();
res.put("ok", false);
res.put("msg", "Layanan belum diimplementasikan. Hubungi pengembang backend.");
out.clear();
out.print(res.toString());
%>
