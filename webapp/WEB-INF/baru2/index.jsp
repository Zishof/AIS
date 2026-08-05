<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.database.model.Tbmuser" %>
<%@ page import="ais.common.Common" %>
<%--
    baru2/index.jsp — Router/Dispatcher tunggal sistem UI baru.

    KEAMANAN: Di bawah WEB-INF, tidak bisa diakses langsung via URL.
    Semua akses wajib melalui servlet /baru2 yang sudah set Common.* dan session.

    ALUR:
      1. Cek login  → belum login  : redirect ke /baru2?p=login (halaman login baru)
      2. Ada ?svc=X                : forward ke services/{modul}/_{svc}.jsp (JSON API)
      3. Selebihnya                : forward ke front_end/{modul}/{p}.jsp (halaman HTML)

    Parameter GET yang dikenal:
      modul  — nama modul (default: utama)
      svc    — nama service (JSON, tanpa underscore prefix)
      p      — nama halaman dalam front_end/{modul}/ (default: index)

    Untuk menambah modul/halaman/service baru: cukup buat file JSP baru di
    WEB-INF/baru2/services/{modul}/_{svc}.jsp  atau
    WEB-INF/baru2/front_end/{modul}/{p}.jsp
    Tidak perlu recompile Java.
--%>
<%!
    private static final String SAFE = "^[a-zA-Z0-9_-]+$";

    private String san(String v) {
        if (v == null || v.trim().isEmpty()) return null;
        v = v.trim();
        return v.matches(SAFE) ? v : null;
    }
%>
<%
    /* ── 1. CEK LOGIN ───────────────────────────────────────────────── */
    boolean loggedIn = false;
    javax.servlet.http.HttpSession sesiHttp = request.getSession(false);
    /* "login" berisi LogLogin (bukan Tbmuser) setelah login AJAX;
     * cukup cek non-null untuk memastikan sesi aktif. */
    if (sesiHttp != null && sesiHttp.getAttribute("login") != null) {
        loggedIn = true;
    }

    /* Halaman yang boleh diakses tanpa login */
    String rawSvcCheck = request.getParameter("svc");
    String rawPageCheck = request.getParameter("p");
    boolean isPublicSvc   = false; // service publik (jika diperlukan di masa depan)
    boolean isPublicPage  = "login".equals(rawPageCheck)
                         || "lupa_password".equals(rawPageCheck)
                         || "cek_pendaftaran".equals(rawPageCheck);

    if (!loggedIn && !isPublicSvc && !isPublicPage) {
        /* Belum login dan bukan halaman publik → ke halaman login baru */
        String root = Common.ROOT == null ? "" : Common.ROOT;
        /* Simpan URL yang ingin dituju agar bisa redirect kembali setelah login */
        String next = request.getRequestURI();
        String qs   = request.getQueryString();
        if (qs != null && !qs.isEmpty()) next = next + "?" + qs;
        /* Forward ke halaman login, bukan redirect, supaya tetap di bawah WEB-INF */
        request.setAttribute("baru2_next", next);
        request.setAttribute("baru2_context", "index");
        request.getRequestDispatcher("/WEB-INF/baru2/front_end/utama/login.jsp").forward(request, response);
        return;
    }

    /* ── 2. BACA PARAMETER ──────────────────────────────────────────── */
    String baru2Context = (String) request.getAttribute("baru2_context");

    String modul = san(request.getParameter("modul"));
    String svc   = san(request.getParameter("svc"));
    String halaman = san(request.getParameter("p"));

    /* Tentukan modul default dari konteks pemanggil */
    if (modul == null) {
        if ("pmb".equals(baru2Context))       modul = "pmb";
        else if ("psb".equals(baru2Context))  modul = "psb";
        else                                   modul = "utama";
    }

    /* Tentukan halaman default */
    if (halaman == null) {
        halaman = "index"; /* selalu index.jsp — sudah cukup pintar */
    }

    /* Oper ke halaman tujuan */
    request.setAttribute("baru2_modul", modul);
    request.setAttribute("baru2_context_final", baru2Context);

    String target;
    if (svc != null) {
        request.setAttribute("baru2_svc", svc);
        /* Coba file spesifik dahulu, fallback ke _dispatcher.jsp, lalu _stub.jsp */
        String specificSvc = "/WEB-INF/baru2/services/" + modul + "/_" + svc + ".jsp";
        String dispatchSvc = "/WEB-INF/baru2/services/" + modul + "/_dispatcher.jsp";
        String stubSvc     = "/WEB-INF/baru2/services/_stub.jsp";

        java.io.InputStream testSpec = application.getResourceAsStream(specificSvc);
        if (testSpec != null) {
            try { testSpec.close(); } catch (Exception e) {}
            target = specificSvc;
        } else {
            java.io.InputStream testDisp = application.getResourceAsStream(dispatchSvc);
            if (testDisp != null) {
                try { testDisp.close(); } catch (Exception e) {}
                target = dispatchSvc;
            } else {
                target = stubSvc;
            }
        }
    } else {
        request.setAttribute("baru2_page", halaman);
        target = "/WEB-INF/baru2/front_end/" + modul + "/" + halaman + ".jsp";
    }

    if (!response.isCommitted()) {
        request.getRequestDispatcher(target).forward(request, response);
    }
%>
