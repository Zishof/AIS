<%@ page contentType="application/json;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.database.model.PerguruanTinggi" %>
<%@ page import="ais.database.model.sekolah.Sekolah" %>
<%@ page import="ais.database.model.sekolah.Yayasan" %>
<%@ page import="ais.action.master.helper.util.PerguruanTinggiUtil" %>
<%@ page import="ais.action.master.sekolah.util.SekolahUtil" %>
<%@ page import="ais.common.Common" %>
<%@ page import="org.json.JSONObject" %>
<%--
    _info.jsp — Service: informasi institusi (nama, motto, logo, warna, dsb.)
    URL: /baru2?modul=utama&svc=info
    Response: JSON
--%>
<%!
    private String safe(String v) {
        if (v == null) return "";
        return v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }
%>
<%
    String nama = "";
    String namaSingkat = "";
    String motto = "";
    String logo = "/img/logo.png";
    String warna = "#1e40af";
    String wa = "";
    String alamat = "";
    String email = "";
    String website = "";
    String root = Common.ROOT == null ? "" : Common.ROOT;

    try {
        PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
        boolean[] ptAtauSekolah = Common.chekPtAtauSekolah();
        boolean sekolahMode = ptAtauSekolah != null && ptAtauSekolah.length > 1 && ptAtauSekolah[1];

        if (pt != null) {
            nama = pt.getNama() == null ? "" : pt.getNama();
            namaSingkat = nama;
            motto = pt.getMotto() == null ? "eCampus Information System" : pt.getMotto();
            wa = pt.getWa() == null ? "" : pt.getWa();
            alamat = pt.getAlamat1() == null ? "" : pt.getAlamat1();
            email = pt.getEmail() == null ? "" : pt.getEmail();
            website = pt.getWebsite() == null ? "" : pt.getWebsite();
        }

        // Ambil logo PT
        String logoPT = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
        if (logoPT != null && !logoPT.trim().isEmpty()) logo = logoPT;

        if (sekolahMode) {
            Sekolah sekolah = SekolahUtil.getSekolah(request);
            if (sekolah != null && sekolah.getId() != null) {
                if (sekolah.getNama() != null && !sekolah.getNama().trim().isEmpty()) nama = sekolah.getNama();
                if (sekolah.getMotto() != null && !sekolah.getMotto().trim().isEmpty()) motto = sekolah.getMotto();
                if (sekolah.getWa() != null && !sekolah.getWa().trim().isEmpty()) wa = sekolah.getWa();
                String logoSekolah = SekolahUtil.getSekolahMedia(request, "logo_sekolah_");
                if (logoSekolah != null && !logoSekolah.trim().isEmpty() && !logoSekolah.endsWith("logo.png")) logo = logoSekolah;
            } else {
                Yayasan yayasan = SekolahUtil.getYayasan(request);
                if (yayasan != null && yayasan.getId() != null) {
                    if (yayasan.getNama() != null && !yayasan.getNama().trim().isEmpty()) nama = yayasan.getNama();
                    if (yayasan.getMotto() != null && !yayasan.getMotto().trim().isEmpty()) motto = yayasan.getMotto();
                    if (yayasan.getWa() != null && !yayasan.getWa().trim().isEmpty()) wa = yayasan.getWa();
                    if (yayasan.getWarna() != null && !yayasan.getWarna().trim().isEmpty()) warna = yayasan.getWarna();
                    String logoYayasan = SekolahUtil.getYayasanMedia(request, "logo_yayasan_");
                    if (logoYayasan != null && !logoYayasan.trim().isEmpty() && !logoYayasan.endsWith("logo.png")) logo = logoYayasan;
                }
            }
        }
    } catch (Exception e) {
        // ignore, gunakan nilai default
    }
    response.setHeader("Cache-Control", "no-cache");
%>
{"nama":"<%=safe(nama)%>","namaSingkat":"<%=safe(namaSingkat)%>","motto":"<%=safe(motto)%>","logo":"<%=safe(logo)%>","warna":"<%=safe(warna)%>","wa":"<%=safe(wa)%>","alamat":"<%=safe(alamat)%>","email":"<%=safe(email)%>","website":"<%=safe(website)%>","root":"<%=safe(root)%>"}
