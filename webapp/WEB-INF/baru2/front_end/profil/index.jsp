<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common,ais.database.model.Tbmuser" %>
<%
Object _loProf = session.getAttribute("login");
Tbmuser loginUser = (_loProf instanceof Tbmuser) ? (Tbmuser)_loProf :
    (session.getAttribute("usersTemp") instanceof Tbmuser ? (Tbmuser)session.getAttribute("usersTemp") : null);

/* Deteksi tipe pengguna berdasarkan atribut sesi standar AIS.
 * Urutan prioritas: mahasiswa → dosen → siswa → guru → admin_sekolah → admin_pt */
String tipe = "admin_pt";
if (session.getAttribute("mahasiswa") != null)      tipe = "mahasiswa";
else if (session.getAttribute("dosen") != null)     tipe = "dosen";
else if (session.getAttribute("siswa") != null)     tipe = "siswa";
else if (session.getAttribute("guru") != null)      tipe = "guru";
else if (loginUser != null) {
    try {
        Object sek = loginUser.getClass().getMethod("ambilSekolah").invoke(loginUser);
        if (sek != null) tipe = "admin_sekolah";
    } catch (Exception ign) {}
}

String subPage = "/WEB-INF/baru2/front_end/profil/" + tipe + ".jsp";
request.getRequestDispatcher(subPage).forward(request, response);
%>
