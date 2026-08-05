<%@ page contentType="application/json;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<%@ page import="ais.database.model.Tbmuser" %>
<%@ page import="ais.database.model.Tbmrole" %>
<%--
    _sesi.jsp — Service: informasi sesi pengguna yang sedang login
    URL: /baru2?modul=utama&svc=sesi
    Response: JSON
--%>
<%!
    private String safe(String v) {
        if (v == null) return "";
        return v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }
%>
<%
    String username = "";
    String namaLengkap = "";
    String role = "";
    String avatar = "";
    boolean loggedIn = false;

    try {
        javax.servlet.http.HttpSession sesi = request.getSession(false);
        if (sesi != null) {
            /* "login" berisi LogLogin setelah AJAX login; Tbmuser ada di "usersTemp" */
            Object loginAttr = sesi.getAttribute("login");
            Tbmuser user = null;
            if (loginAttr instanceof Tbmuser) {
                user = (Tbmuser) loginAttr;
            } else if (sesi.getAttribute("usersTemp") instanceof Tbmuser) {
                user = (Tbmuser) sesi.getAttribute("usersTemp");
            }
            if (user != null) {
                loggedIn = true;
                username = user.getUsername() == null ? "" : user.getUsername();
                namaLengkap = user.getUsername() == null ? "" : user.getUsername();
                // Coba ambil nama lengkap dari pegawai jika ada
                try {
                    if (user.getPegawai() != null && user.getPegawai().getNama() != null) {
                        namaLengkap = user.getPegawai().getNama();
                    } else if (user.getMahasiswa() != null && user.getMahasiswa().getNama() != null) {
                        namaLengkap = user.getMahasiswa().getNama();
                    }
                } catch (Exception ex) { /* ignore */ }
                // Role
                try {
                    Tbmrole hakAkses = user.hakAkses();
                    if (hakAkses != null && hakAkses.getRoleId() != null) {
                        role = hakAkses.getRoleId();
                    }
                } catch (Exception ex) { /* ignore */ }
                // Avatar initials
                if (!namaLengkap.trim().isEmpty()) {
                    String[] parts = namaLengkap.trim().split("\\s+");
                    avatar = parts.length >= 2
                        ? ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase()
                        : ("" + parts[0].charAt(0)).toUpperCase();
                } else if (!username.isEmpty()) {
                    avatar = username.substring(0, Math.min(2, username.length())).toUpperCase();
                }
            }
        }
    } catch (Exception e) {
        // ignore
    }
    response.setHeader("Cache-Control", "no-cache");
%>
{"loggedIn":<%=loggedIn%>,"username":"<%=safe(username)%>","namaLengkap":"<%=safe(namaLengkap)%>","role":"<%=safe(role)%>","avatar":"<%=safe(avatar)%>"}
