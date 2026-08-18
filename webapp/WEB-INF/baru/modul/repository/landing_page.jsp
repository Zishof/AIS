<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
// ── Routing hub untuk modul Repository ────────────────────────────────────
// Mode hanya_tampil_jsp=true : hanya render fragmen JSP (dipakai oleh fetch/AJAX).
// Mode normal                : render halaman penuh dengan header/footer.

boolean hanyaTampil = "true".equalsIgnoreCase(
    request.getParameter("hanya_tampil_jsp") == null ? ""
        : request.getParameter("hanya_tampil_jsp").trim());

String p = request.getParameter("p") == null ? "" : request.getParameter("p").trim();
String s = request.getParameter("s") == null ? "" : request.getParameter("s").trim();

// ── Sanitasi path: hanya izinkan karakter alfanumerik dan underscore ───────
// Mencegah directory traversal (mis. p=../../include)
p = p.replaceAll("[^a-zA-Z0-9_]", "");
s = s.replaceAll("[^a-zA-Z0-9_]", "");

// ── Whitelist sub-halaman yang boleh dimuat di modul ini ──────────────────
// Semua request fragment HARUS berprefiks "repository" sebagai modul
boolean validModule = "repository".equals(p);

if (hanyaTampil) {
    if (validModule && !s.isEmpty()) {
        String pg = "/WEB-INF/baru/modul/" + p + "/" + s + ".jsp";
        try {
%>
            <jsp:include page="<%=pg%>" />
<%
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/repository/landing_page.jsp:31");
%>
            <jsp:include page="/WEB-INF/baru/componen/tidak_ketemu_page.jsp" />
<%
        }
    }
    // Jika validasi gagal: diam, tidak output apapun (404 logic di controller)
} else {
%>
<jsp:include page="/WEB-INF/baru/include/header.jsp" />

<body>
    <main class="main" id="top">
        <div class="container-fluid">

            <jsp:include page="/WEB-INF/baru/modul/repository/ListRepository.jsp" />

            <jsp:include page="/WEB-INF/baru/include/dialog-modal.jsp" />

        </div>
    </main>

    <jsp:include page="/WEB-INF/baru/include/foot.jsp" />
</body>
</html>
<%
}
%>
