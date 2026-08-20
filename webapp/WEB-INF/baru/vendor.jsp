<%@page import="ais.common.Common"%>
<%@page import="javax.servlet.RequestDispatcher"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    String authActionVendorJsp = request.getParameter("auth_action") == null ? "" : request.getParameter("auth_action").trim();
    if ("logout".equalsIgnoreCase(authActionVendorJsp)) {
        try {
            if (request.getSession(false) != null) {
                request.getSession(false).removeAttribute("VENDOR_LOGGED_IN");
                request.getSession(false).removeAttribute("VENDOR_USER_LOGGED_IN");
                request.getSession(false).removeAttribute("VENDOR_LOGIN_ERROR");
                request.getSession(false).removeAttribute("VENDOR_LAST_MESSAGE");
                request.getSession(false).removeAttribute("penyediaAsset");
                request.getSession(false).removeAttribute("vendorLogged");
                request.getSession(false).removeAttribute("vendor");
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/vendor.jsp:22");}
        response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/vendor?logout=1"));
        return;
    }

    boolean hanya_tampil_jsp = request.getParameter("hanya_tampil_jsp") != null
            && request.getParameter("hanya_tampil_jsp").trim().equalsIgnoreCase("true");

    String p = "";
    if (request.getParameter("p") != null && !request.getParameter("p").trim().isEmpty()) {
        p = request.getParameter("p").trim().replace("..", "").replace("/", "").replace("\\", "");
    }
    String s = "";
    if (request.getParameter("s") != null && !request.getParameter("s").trim().isEmpty()) {
        s = request.getParameter("s").trim().replace("..", "").replace("/", "").replace("\\", "");
    }

    String includePath = "/WEB-INF/baru/modul/vendor/landing_page.jsp";
    if (hanya_tampil_jsp && !p.trim().isEmpty() && !s.trim().isEmpty()) {
        includePath = "/WEB-INF/baru/modul/" + p + "/" + s + ".jsp";
    }

    try {
        RequestDispatcher rd = request.getRequestDispatcher(includePath);
        if (rd == null) {
            throw new Exception("Halaman tidak ditemukan: " + includePath);
        }
        rd.include(request, response);
    } catch (Exception e) {
        try { Common.tampilErrorJikaAdmin(e); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/vendor.jsp:51");}
        if (!response.isCommitted()) {
%>
<!doctype html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Portal Vendor</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
</head>
<body class="bg-light">
<jsp:include page="/WEB-INF/baru/include/pemilih_bahasa.jsp" />
<div class="container py-5">
    <div class="card border-0 shadow rounded-4">
        <div class="card-body p-4 p-lg-5">
            <div class="d-flex align-items-center gap-3 mb-3">
                <div class="rounded-circle bg-warning-subtle text-warning p-3"><i class="fas fa-triangle-exclamation fa-2x"></i></div>
                <div>
                    <h3 class="fw-bold mb-1">Portal Vendor belum dapat ditampilkan</h3>
                    <p class="text-muted mb-0">File halaman vendor belum lengkap atau terjadi error saat include JSP.</p>
                </div>
            </div>
            <a class="btn btn-primary rounded-pill px-4" href="<%=request.getContextPath()%>/vendor"><i class="fas fa-home me-2"></i>Kembali ke Portal Vendor</a>
        </div>
    </div>
</div>
<jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body>
</html>
<%
        }
    }
%>
