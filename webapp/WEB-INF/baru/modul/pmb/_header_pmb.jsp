<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%!
    private String pmbSafe(String value) {
        return value == null ? "" : value.trim();
    }

    private String pmbHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String pmbSafeThemeFile(String value) {
        String theme = pmbSafe(value);
        if (theme.length() == 0) {
            return "";
        }
        theme = theme.replace('\\', '/');
        int lastSlash = theme.lastIndexOf('/');
        if (lastSlash >= 0) {
            theme = theme.substring(lastSlash + 1);
        }
        theme = theme.replace("..", "").replace("%", "").replace("?", "").replace("#", "");
        if (theme.length() == 0) {
            return "";
        }
        if (!theme.toLowerCase().endsWith(".css")) {
            theme = theme + ".css";
        }
        return theme;
    }
%>

<%
    String rnd = Common.getGeneratedBarCode(7);
    BiodataCalonMahasiswa camaLogged = Common.isLogin(request);
    boolean isLoggedIn = (camaLogged != null);

    PerguruanTinggi perguruanTinggi = null;
    try {
        perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);
    } catch (Exception e) {
        perguruanTinggi = null;
    }

    String judulNamaPerguruanTinggi = perguruanTinggi != null ? perguruanTinggi.getNama() : "";
    if (judulNamaPerguruanTinggi == null || judulNamaPerguruanTinggi.trim().length() == 0) {
        judulNamaPerguruanTinggi = Common.getBahasaConfig("Institusi Pendidikan");
    }

    String Motto = perguruanTinggi != null ? perguruanTinggi.getMotto() : "";
    String logo_PerguruanTinggi = "";
    try {
        logo_PerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
    } catch (Exception e) {
        logo_PerguruanTinggi = "";
    }

    String themeCssPmb = "";
    try {
        String rawCss = perguruanTinggi != null ? perguruanTinggi.getCss() : null;
        if (rawCss != null && rawCss.trim().length() > 0) {
            themeCssPmb = rawCss;
        }

        // Mengikuti pola login2.jsp: tema aktif mengikuti CSS institusi.
        // Jika halaman berada pada konteks sekolah, CSS sekolah menimpa CSS perguruan tinggi.
        ais.database.model.sekolah.Sekolah sekolahPmb = ais.action.master.sekolah.util.SekolahUtil.getSekolah(request);
        if (sekolahPmb != null && sekolahPmb.getId() != null && sekolahPmb.getCss() != null
                && sekolahPmb.getCss().trim().length() > 0) {
            themeCssPmb = sekolahPmb.getCss();
        }
    } catch (Exception e) {
        themeCssPmb = "";
    }

    if (themeCssPmb == null || themeCssPmb.trim().length() == 0) {
        try {
            Konfigurasi konfigurasiTheme = Common.getKonfigurasi("tema_pmb_css", "");
            if (konfigurasiTheme != null && konfigurasiTheme.getNilai() != null
                    && konfigurasiTheme.getNilai().trim().length() > 0) {
                themeCssPmb = konfigurasiTheme.getNilai();
            }
        } catch (Exception e) {
            themeCssPmb = "";
        }
    }

    themeCssPmb = pmbSafeThemeFile(themeCssPmb);
    long vPmb = System.currentTimeMillis();
%>

<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
<script>
/* PMB memakai Font Awesome mode CSS/webfont. Auto SVG dimatikan agar tidak muncul icon tanda tanya saat JS Font Awesome gagal mengenali alias icon. */
window.FontAwesomeConfig = window.FontAwesomeConfig || {};
window.FontAwesomeConfig.autoReplaceSvg = false;
window.FontAwesomeConfig.observeMutations = false;
</script>
<link id="pmbFontAwesomeCss" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" rel="stylesheet" crossorigin="anonymous" referrerpolicy="no-referrer">
<link id="pmbFontAwesomeShimCss" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/v4-shims.min.css" rel="stylesheet" crossorigin="anonymous" referrerpolicy="no-referrer">
<link href="<%=Common.ROOT%>/css/baru/base-theme.css?v=<%=vPmb%>" rel="stylesheet">
<link href="<%=Common.ROOT%>/css/baru/base-pmb.css?v=<%=vPmb%>" rel="stylesheet">
<% if (themeCssPmb != null && themeCssPmb.trim().length() > 0) { %>
<link href="<%=Common.ROOT%>/css/baru/<%=pmbHtml(themeCssPmb)%>?v=<%=vPmb%>" rel="stylesheet">
<% } %>

<script>
window.showLoadingPMB = function() {
    var loader = document.getElementById('globalLoaderPMB');
    if(loader) { loader.classList.remove('d-none'); loader.classList.add('d-flex'); }
};

window.hideLoadingPMB = function() {
    var loader = document.getElementById('globalLoaderPMB');
    if(loader) { loader.classList.remove('d-flex'); loader.classList.add('d-none'); }
};

(function() {
    if (typeof window.pmbEscapeHtml !== 'function') {
        window.pmbEscapeHtml = function(value) {
            if (value === null || typeof value === 'undefined') return '';
            return String(value)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        };
    }

    if (typeof window.pmbLoadFontAwesomeFallback !== 'function') {
        window.pmbLoadFontAwesomeFallback = function() {
            if (document.getElementById('pmbFontAwesomeFallbackCss')) {
                return;
            }
            var fallback = document.createElement('link');
            fallback.id = 'pmbFontAwesomeFallbackCss';
            fallback.rel = 'stylesheet';
            fallback.href = 'https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@6.5.2/css/all.min.css';
            fallback.crossOrigin = 'anonymous';
            document.head.appendChild(fallback);
        };
    }

    if (typeof window.pmbEnsureFontAwesome !== 'function') {
        window.pmbEnsureFontAwesome = function(node) {
            var rootNode = node && node.querySelectorAll ? node : document;
            var icons = rootNode.querySelectorAll('i[class*="fa-"], span[class*="fa-"]');
            for (var i = 0; i < icons.length; i++) {
                var icon = icons[i];
                icon.setAttribute('aria-hidden', 'true');

                /* Jangan pernah mengisi isi tag icon dengan karakter fallback.
                 * Tanda tanya biasanya muncul karena Font Awesome JS mengubah icon
                 * yang tidak dikenali menjadi SVG missing-icon. PMB cukup memakai CSS webfont.
                 */
                if (icon.innerHTML && icon.innerHTML.replace(/\s+/g, '').length > 0) {
                    icon.innerHTML = '';
                }

                var cls = icon.className || '';
                if (cls.indexOf('fa-solid') < 0 && cls.indexOf('fa-regular') < 0 && cls.indexOf('fa-brands') < 0
                        && cls.indexOf('fas') < 0 && cls.indexOf('far') < 0 && cls.indexOf('fab') < 0) {
                    icon.className = (cls + ' fas').replace(/\s+/g, ' ').replace(/^\s+|\s+$/g, '');
                }
            }
        };

        window.pmbCheckFontAwesomeReady = function() {
            try {
                var test = document.createElement('i');
                test.className = 'fas fa-check';
                test.style.position = 'absolute';
                test.style.left = '-9999px';
                test.style.top = '-9999px';
                document.body.appendChild(test);
                var font = window.getComputedStyle(test, ':before').getPropertyValue('font-family') || window.getComputedStyle(test).getPropertyValue('font-family') || '';
                var content = window.getComputedStyle(test, ':before').getPropertyValue('content') || '';
                document.body.removeChild(test);
                if (font.toLowerCase().indexOf('awesome') < 0 || !content || content === 'none' || content === 'normal') {
                    window.pmbLoadFontAwesomeFallback();
                }
            } catch (e) {
                window.pmbLoadFontAwesomeFallback();
            }
        };

        window.pmbObserveFontAwesome = function() {
            if (!window.MutationObserver || !document.body || document.body.getAttribute('data-pmb-icon-observer') === 'true') {
                return;
            }
            document.body.setAttribute('data-pmb-icon-observer', 'true');
            var timerIconPMB = null;
            var observer = new MutationObserver(function() {
                if (timerIconPMB) {
                    window.clearTimeout(timerIconPMB);
                }
                timerIconPMB = window.setTimeout(function(){ window.pmbEnsureFontAwesome(); }, 120);
            });
            observer.observe(document.body, { childList: true, subtree: true });
        };
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', function() {
                window.pmbEnsureFontAwesome();
                window.pmbObserveFontAwesome();
                window.setTimeout(window.pmbCheckFontAwesomeReady, 500);
            });
        } else {
            window.pmbEnsureFontAwesome();
            window.pmbObserveFontAwesome();
            window.setTimeout(window.pmbCheckFontAwesomeReady, 500);
        }
        window.setTimeout(window.pmbEnsureFontAwesome, 900);
    }
})();
</script>

<div id="globalLoaderPMB" class="d-none align-items-center justify-content-center" style="position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(15, 23, 42, 0.72); z-index: 9999; backdrop-filter: blur(7px);">
    <div class="text-center text-white p-4 rounded-4 shadow-lg pmb-loader-card">
        <div class="spinner-border mb-3 text-warning" style="width: 3.5rem; height: 3.5rem;" role="status"></div>
        <h5 class="fw-bold tracking-wide"><%= Common.getBahasaConfig("Mohon Tunggu Sebentar...") %></h5>
        <p class="small text-white-50 mb-0"><%= Common.getBahasaConfig("Sistem sedang memuat data Anda.") %></p>
    </div>
</div>

<div class="page-bg-pmb"></div>

<div class="hero-section-pmb pt-3 pb-5 mb-5 text-center">
    <div class="hero-pattern-pmb"></div>
    <div class="container position-relative pt-2 pb-2 d-flex justify-content-end gap-2" style="min-height: 40px; z-index: 2;">
        <% if (!isLoggedIn) { %>
            <a href="<%=Common.ROOT%>/pmb?versilama=true" class="btn btn-light text-primary rounded-pill shadow border-0 btn-login-pmb">
                <i class="fas fa-desktop me-2"></i><%= Common.getBahasaConfig("Gunakan Tampilan Klasik") %>
            </a>
        <% } %>
    </div>

    <div class="container position-relative pb-4 pt-2" style="z-index: 2;">
        <% if (logo_PerguruanTinggi != null && logo_PerguruanTinggi.trim().length() > 0) { %>
            <img src="<%= pmbHtml(logo_PerguruanTinggi) %>" alt="<%= Common.getBahasaConfig("Logo Institusi") %>" class="img-fluid mb-3 logo-shadow-pmb" style="max-height: 140px; object-fit: contain;">
        <% } %>

        <h2 class="fw-bold text-white text-uppercase text-shadow-pmb mb-2" style="letter-spacing: 1.5px;">
            <%= pmbHtml(judulNamaPerguruanTinggi) %>
        </h2>
        
        <% if (Motto != null && Motto.trim().length() > 0) { %>
            <h5 class="fw-light text-warning fst-italic mb-4 text-shadow-pmb">
                &quot;<%= pmbHtml(Motto) %>&quot;
            </h5>
        <% } else { %>
             <div class="mb-4"></div>
        <% } %>

        <div class="d-inline-block badge-pmb-custom rounded-pill px-4 py-2 mb-3 shadow">
            <h3 class="fw-bold mb-0 text-white text-shadow-pmb">
                <i class="fas fa-user-graduate me-2 text-warning"></i><%= Common.getBahasaConfig("Penerimaan Mahasiswa Baru") %>
            </h3>
        </div>

        <p class="lead fw-normal mb-2 text-white opacity-90 mx-auto" style="max-width: 850px; font-size: 1.15rem; line-height: 1.6;">
            <%= Common.getBahasaConfig("Temukan jalur pendaftaran, pilih program studi, lalu lengkapi data dengan langkah yang mudah diikuti.") %>
        </p>
    </div>
</div>
