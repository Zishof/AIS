<%--
    _libs.jsp — CDN dependencies terpusat untuk seluruh halaman baru2.
    Disertakan via: <%@ include file="../_libs.jsp" %>

    Untuk update versi: cukup ganti URL di sini, berlaku ke semua halaman.
    Tidak perlu compile ulang — JSP static include diproses saat JSP ditranslasikan.

    Versi saat ini (Juli 2026):
      Bootstrap   5.3.3  → https://cdnjs.cloudflare.com/ajax/libs/bootstrap/
      Font Awesome 6.6.0 → https://cdnjs.cloudflare.com/ajax/libs/font-awesome/
      Alpine.js   3.14.1 → https://cdn.jsdelivr.net/npm/alpinejs/
      Animate.css 4.1.1  → https://cdnjs.cloudflare.com/ajax/libs/animate.css/
      NProgress   0.2.0  → https://cdnjs.cloudflare.com/ajax/libs/nprogress/
--%>

<%-- ── Google Fonts (preconnect untuk performa) ─────────────────────── --%>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:ital,wght@0,200..800;1,200..800&display=swap">

<%-- ── NProgress 0.2.0 — loading bar ramping ────────────────────────── --%>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/nprogress/0.2.0/nprogress.min.css">

<%-- ── Animate.css 4.1.1 — animasi CSS siap pakai ──────────────────── --%>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css">

<%-- ── Bootstrap 5.3.3 — CSS framework & komponen ──────────────────── --%>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/bootstrap/5.3.3/css/bootstrap.min.css">

<%-- ── Font Awesome 6.6.0 — ikon lengkap ────────────────────────────── --%>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.6.0/css/all.min.css">

<%-- ── Override NProgress: sesuaikan warna ke tema baru2 ────────────── --%>
<style>
#nprogress .bar{background:linear-gradient(90deg,#1e3a8a,#4f46e5,#6d28d9)!important;height:3px!important}
#nprogress .peg{box-shadow:0 0 10px #4f46e5,0 0 5px #6d28d9!important}
#nprogress .spinner-icon{border-top-color:#4f46e5!important;border-left-color:#4f46e5!important}
</style>

<%-- ── Alpine.js 3.14.1 — reaktifitas ringan tanpa build (defer wajib) -%>
<script defer src="https://cdn.jsdelivr.net/npm/alpinejs@3.14.1/dist/cdn.min.js"></script>

<%-- ── Bootstrap JS bundle 5.3.3 (sudah termasuk Popper.js) ─────────── --%>
<script defer src="https://cdnjs.cloudflare.com/ajax/libs/bootstrap/5.3.3/js/bootstrap.bundle.min.js"></script>

<%-- ── NProgress JS — inisialisasi konfigurasi global ───────────────── --%>
<script src="https://cdnjs.cloudflare.com/ajax/libs/nprogress/0.2.0/nprogress.min.js"></script>
<script>if(typeof NProgress!=='undefined'){NProgress.configure({showSpinner:false,speed:400,trickleSpeed:200})}</script>
