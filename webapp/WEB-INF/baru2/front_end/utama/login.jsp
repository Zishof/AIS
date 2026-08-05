<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="ais.database.model.PerguruanTinggi" %>
<%@ page import="ais.database.model.sekolah.Sekolah" %>
<%@ page import="ais.database.model.sekolah.Yayasan" %>
<%@ page import="ais.database.model.Tbmuser" %>
<%@ page import="ais.action.master.helper.util.PerguruanTinggiUtil" %>
<%@ page import="ais.action.master.sekolah.util.SekolahUtil" %>
<%@ page import="ais.common.Common" %>
<%--
    front_end/utama/login.jsp — Halaman login baru2.

    PENTING: Form menggunakan AJAX ke /login?action=ajax_login (sama persis dengan
    login2.jsp) bukan j_security_check. Ini memastikan SecurityFilter.dataLogin
    diisi dan session.setAttribute("login", tbmuser) terpasang sehingga
    baru2/index.jsp menerima sesi sebagai valid.

    Setelah login berhasil, redirect ke baru2_next (URL semula) atau /baru2.
--%>
<%!
    private String safeHtml(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
    private String safeJs(String v) {
        if (v == null) return "";
        return v.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'")
                .replace("\n", " ").replace("\r", "");
    }
%>
<%
    /* Jika sudah login → alihkan ke tujuan semula atau /baru2 */
    javax.servlet.http.HttpSession sesiHttp = request.getSession(false);
    if (sesiHttp != null && sesiHttp.getAttribute("login") != null) {
        String alihKe = (String) request.getAttribute("baru2_next");
        if (alihKe == null || alihKe.trim().isEmpty()) alihKe = Common.ROOT + "/baru2";
        response.sendRedirect(alihKe);
        return;
    }

    /* URL tujuan setelah login berhasil */
    String nextUrl = (String) request.getAttribute("baru2_next");
    if (nextUrl == null || nextUrl.trim().isEmpty()) nextUrl = Common.ROOT + "/baru2";

    /* Data institusi untuk tampilan */
    String nama = "Sistem Informasi Akademik";
    String motto = "";
    String logo  = "/img/logo.png";
    String rootCtx = Common.ROOT == null ? "" : Common.ROOT;

    try {
        PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
        if (pt != null) {
            if (pt.getNama()  != null && !pt.getNama().trim().isEmpty())  nama  = pt.getNama();
            if (pt.getMotto() != null && !pt.getMotto().trim().isEmpty()) motto = pt.getMotto();
        }
        String logoPT = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
        if (logoPT != null && !logoPT.trim().isEmpty()) logo = logoPT;

        boolean[] ptYa = Common.chekPtAtauSekolah();
        boolean sekolahMode = ptYa != null && ptYa.length > 1 && ptYa[1];
        if (sekolahMode) {
            Sekolah sekolah = SekolahUtil.getSekolah(request);
            if (sekolah != null && sekolah.getId() != null) {
                if (sekolah.getNama()  != null && !sekolah.getNama().trim().isEmpty())  nama  = sekolah.getNama();
                if (sekolah.getMotto() != null && !sekolah.getMotto().trim().isEmpty()) motto = sekolah.getMotto();
                String logoS = SekolahUtil.getSekolahMedia(request, "logo_sekolah_");
                if (logoS != null && !logoS.trim().isEmpty() && !logoS.endsWith("logo.png")) logo = logoS;
            } else {
                Yayasan yayasan = SekolahUtil.getYayasan(request);
                if (yayasan != null && yayasan.getId() != null) {
                    if (yayasan.getNama()  != null && !yayasan.getNama().trim().isEmpty())  nama  = yayasan.getNama();
                    if (yayasan.getMotto() != null && !yayasan.getMotto().trim().isEmpty()) motto = yayasan.getMotto();
                    String logoY = SekolahUtil.getYayasanMedia(request, "logo_yayasan_");
                    if (logoY != null && !logoY.trim().isEmpty() && !logoY.endsWith("logo.png")) logo = logoY;
                }
            }
        }
    } catch (Exception ex) { /* diabaikan */ }
%>
<!doctype html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title><%=Common.getBahasaConfig("Masuk")%> — <%=safeHtml(nama)%></title>
<link rel="shortcut icon" href="<%=rootCtx%>/img/favicon.ico" type="image/x-icon">
<%-- SweetAlert2 — harus sebelum body --%>
<script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<%-- CDN libs (Bootstrap, FA, Alpine) — sertakan dahulu agar style kita menang --%>
<%@ include file="../_libs.jsp" %>
<style>
:root{
  --pri:#1e40af;--pri2:#4f46e5;--acc:#6d28d9;
  --grad:linear-gradient(120deg,#1e3a8a 0%,#4f46e5 55%,#6d28d9 100%);
  --ok:#10b981;--bad:#ef4444;
  --ink:#1e293b;--ink2:#64748b;
  --line:#e6e9f0;--bg:#f5f7fb;--card:#ffffff;
  --shadow-lg:0 12px 40px rgba(30,41,59,.18);
  --r:16px;--font:"Segoe UI",system-ui,-apple-system,sans-serif;
}
*{box-sizing:border-box;margin:0;padding:0}
html,body{min-height:100vh}
body{font-family:var(--font);background:var(--grad);display:flex;align-items:center;
     justify-content:center;padding:20px;min-height:100vh}
.card{background:var(--card);border-radius:var(--r);box-shadow:var(--shadow-lg);
      width:100%;max-width:440px;overflow:hidden}
.card-header{background:var(--grad);padding:28px 32px;text-align:center;
             position:relative;overflow:hidden}
.card-header::after{content:"";position:absolute;right:-50px;top:-70px;
                    width:200px;height:200px;border-radius:50%;background:rgba(255,255,255,.07)}
.logo-wrap{width:80px;height:80px;border-radius:50%;background:rgba(255,255,255,.15);
           border:3px solid rgba(255,255,255,.3);display:flex;align-items:center;
           justify-content:center;margin:0 auto 12px;overflow:hidden;position:relative}
.logo-wrap img{max-width:90%;max-height:90%;object-fit:contain}
.inst-name{color:#fff;font-size:16px;font-weight:800;margin-bottom:4px;position:relative}
.inst-motto{color:rgba(255,255,255,.75);font-size:12px;position:relative;line-height:1.4}
.card-body{padding:28px 32px}
.card-body h2{font-size:20px;font-weight:800;color:var(--ink);margin-bottom:4px}
.card-body .sub{color:var(--ink2);font-size:13px;margin-bottom:22px}
/* Alert inline */
#loginAlert{display:none;align-items:flex-start;gap:9px;background:#fff5f5;
            border:1px solid #fecaca;border-radius:10px;padding:11px 14px;
            margin-bottom:18px;color:#dc2626;font-size:13px}
#loginAlert.aktif{display:flex}
/* Form */
.f-group{margin-bottom:16px}
.f-group label{display:block;font-size:13px;font-weight:700;color:var(--ink);margin-bottom:5px}
.inp-wrap{position:relative}
.inp-wrap input{width:100%;padding:11px 14px 11px 42px;border:1px solid var(--line);
               border-radius:10px;font-size:14px;font-family:var(--font);
               background:var(--bg);color:var(--ink);transition:.15s;outline:none}
.inp-wrap input:focus{border-color:var(--pri2);background:#fff;
                      box-shadow:0 0 0 3px rgba(79,70,229,.12)}
.inp-wrap .inp-ic{position:absolute;left:14px;top:50%;transform:translateY(-50%);
                  color:var(--ink2);font-size:14px;pointer-events:none}
.inp-wrap .eye{position:absolute;right:12px;top:50%;transform:translateY(-50%);
               color:var(--ink2);font-size:13px;cursor:pointer;padding:4px;border:none;
               background:none;font-family:inherit}
.remember{display:flex;align-items:center;gap:8px;margin-bottom:20px}
.remember input[type=checkbox]{width:16px;height:16px;accent-color:var(--pri2);cursor:pointer}
.remember label{font-size:13px;font-weight:500;color:var(--ink2);cursor:pointer;margin:0}
.btn-login{width:100%;padding:12px;border-radius:11px;background:var(--grad);color:#fff;
           font-size:15px;font-weight:800;font-family:var(--font);border:none;cursor:pointer;
           transition:.15s;display:flex;align-items:center;justify-content:center;gap:8px}
.btn-login:hover{opacity:.9;transform:translateY(-1px)}
.btn-login:disabled{opacity:.65;cursor:not-allowed;transform:none}
.pmb-link{display:flex;align-items:center;gap:8px;padding:11px 14px;border:1px solid var(--line);
          border-radius:10px;margin-top:14px;color:var(--ink2);font-size:13px;font-weight:600;
          text-decoration:none;transition:.15s}
.pmb-link:hover{border-color:var(--pri2);color:var(--pri2);background:#f0f4ff}
.pmb-link i{color:var(--pri2);font-size:14px}
.footer{text-align:center;padding:16px 0 0;border-top:1px solid var(--line);
        margin-top:20px;font-size:12px;color:var(--ink2)}
.footer a{color:var(--pri2);font-weight:600;text-decoration:none}
@media(max-width:480px){.card-body,.card-header{padding:22px 20px}}
</style>
</head>
<body>
<div class="card">
  <div class="card-header">
    <div class="logo-wrap">
      <img src="<%=rootCtx%><%=safeHtml(logo)%>" alt="Logo"
           onerror="this.onerror=null;this.src='<%=rootCtx%>/img/logo.png'">
    </div>
    <div class="inst-name"><%=safeHtml(nama)%></div>
    <%if(motto != null && !motto.trim().isEmpty()){%>
    <div class="inst-motto"><%=safeHtml(motto)%></div>
    <%}%>
  </div>
  <div class="card-body">
    <h2><%=Common.getBahasaConfig("Masuk")%></h2>
    <p class="sub"><%=Common.getBahasaConfig("Selamat datang kembali, silakan masuk ke akun Anda.")%></p>

    <%-- Alert inline untuk pesan error login --%>
    <div id="loginAlert">
      <i class="fa-solid fa-circle-exclamation" style="margin-top:1px;flex-shrink:0"></i>
      <span id="loginAlertMsg"></span>
    </div>

    <%-- Form AJAX — sama persis dengan login2.jsp; POST ke /login?action=ajax_login --%>
    <form novalidate id="loginForm" onsubmit="prosesLogin(event)">
      <div class="f-group">
        <label for="username"><%=Common.getBahasaConfig("Nama Pengguna")%></label>
        <div class="inp-wrap">
          <span class="inp-ic"><i class="fa-regular fa-user"></i></span>
          <input id="username" name="username" type="text"
                 placeholder="<%=Common.getBahasaConfigJS("Ketik username di sini...")%>"
                 autocomplete="username" required autofocus>
        </div>
      </div>
      <div class="f-group">
        <label for="password"><%=Common.getBahasaConfig("Kata Sandi")%></label>
        <div class="inp-wrap">
          <span class="inp-ic"><i class="fa-solid fa-lock"></i></span>
          <input id="password" name="password" type="password"
                 placeholder="<%=Common.getBahasaConfigJS("Masukkan kata sandi...")%>"
                 autocomplete="current-password" required>
          <button type="button" class="eye" onclick="togglePwd()" title="Tampilkan/sembunyikan">
            <i class="fa-regular fa-eye" id="eyeIc"></i>
          </button>
        </div>
      </div>
      <div class="remember">
        <input type="checkbox" id="rememberMe" name="rememberMe">
        <label for="rememberMe"><%=Common.getBahasaConfig("Ingat saya")%></label>
      </div>
      <button type="submit" class="btn-login" id="submitBtn">
        <i class="fa-solid fa-right-to-bracket"></i>
        <%=Common.getBahasaConfig("Masuk")%>
      </button>
    </form>

    <a class="pmb-link" href="<%=rootCtx%>/pmb">
      <i class="fa-solid fa-user-plus"></i>
      <span><%=Common.getBahasaConfig("Daftar sebagai Mahasiswa / Siswa Baru")%></span>
      <i class="fa-solid fa-arrow-right" style="margin-left:auto;font-size:11px"></i>
    </a>

    <div class="footer">
      &copy; <%=java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)%>
      <%=safeHtml(nama)%> &mdash;
      <a href="<%=rootCtx%>/z"><%=Common.getBahasaConfig("Tampilan Klasik")%></a>
    </div>
  </div>
</div>

<script>
/* URL tujuan setelah login berhasil */
var BARU2_NEXT = '<%=safeJs(nextUrl)%>';

function showAlert(msg) {
    var box = document.getElementById('loginAlert');
    var lbl = document.getElementById('loginAlertMsg');
    if (lbl) lbl.textContent = msg;
    if (box) { box.classList.add('aktif'); box.scrollIntoView({behavior:'smooth',block:'nearest'}); }
}
function clearAlert() {
    var box = document.getElementById('loginAlert');
    if (box) box.classList.remove('aktif');
}

function togglePwd() {
    var inp = document.getElementById('password');
    var ic  = document.getElementById('eyeIc');
    if (!inp) return;
    if (inp.type === 'password') {
        inp.type = 'text';
        ic.className = 'fa-regular fa-eye-slash';
    } else {
        inp.type = 'password';
        ic.className = 'fa-regular fa-eye';
    }
}

function cariPesanDariHtml(html) {
    if (!html) return '';
    try {
        var doc = new DOMParser().parseFromString(html, 'text/html');
        var text = (doc.body ? doc.body.innerText : html).replace(/\s+/g,' ').trim();
        var lower = text.toLowerCase();
        var kata = ['akun anda tidak aktif','akun anda tidak bisa login',
                    'pengguna tidak ditemukan','tidak diizinkan','satuan kerja',
                    'diblokir','kuota','kata sandi','password','gagal login'];
        for (var i = 0; i < kata.length; i++) {
            var idx = lower.indexOf(kata[i]);
            if (idx >= 0) {
                var end = text.indexOf('.', idx);
                if (end < 0 || end - idx > 260) end = Math.min(text.length, idx + 260);
                return text.substring(idx, end).trim();
            }
        }
    } catch(e) {}
    return '';
}

async function prosesLogin(event) {
    event.preventDefault();
    clearAlert();

    var btn  = document.getElementById('submitBtn');
    var uInp = document.getElementById('username');
    var pInp = document.getElementById('password');
    var uVal = uInp ? uInp.value.trim() : '';
    var pVal = pInp ? pInp.value : '';

    if (!uVal || !pVal) {
        showAlert('<%=Common.getBahasaConfigJS("Nama pengguna dan kata sandi tidak boleh kosong.")%>');
        if (!uVal && uInp) uInp.focus(); else if (pInp) pInp.focus();
        return;
    }

    if (btn) btn.disabled = true;

    Swal.fire({
        title: '<%=Common.getBahasaConfigJS("Memverifikasi...")%>',
        html:  '<%=Common.getBahasaConfigJS("Mohon tunggu sebentar, sedang memproses otentikasi akun Anda.")%>',
        allowOutsideClick: false,
        allowEscapeKey:    false,
        showConfirmButton: false,
        didOpen: function() { Swal.showLoading(); }
    });

    try {
        var payload = new URLSearchParams();
        payload.append('username', uVal);
        payload.append('password', pVal);
        var rmCb = document.getElementById('rememberMe');
        if (rmCb && rmCb.checked) payload.append('rememberMe', 'true');

        var resp = await fetch('<%=rootCtx%>/login?action=ajax_login', {
            method:      'POST',
            headers:     { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
            credentials: 'include',
            body:        payload.toString()
        });

        var txt = await resp.text();
        var result;
        try {
            result = JSON.parse(txt);
        } catch(e) {
            result = { status: 'error',
                       message: cariPesanDariHtml(txt) ||
                                '<%=Common.getBahasaConfigJS("Gagal memproses respon login. Silakan coba lagi.")%>' };
        }

        if (!resp.ok && result.status === 'success') {
            result.status  = 'error';
            result.message = '<%=Common.getBahasaConfigJS("Login gagal diproses oleh server. Silakan coba lagi.")%>';
        }

        if (result.status === 'success') {
            /* Simpan cookie remember-me jika diminta */
            if (rmCb && rmCb.checked && result.cookie_val) {
                var age = 15552000;
                document.cookie = 'userinfo=' + encodeURIComponent(result.cookie_val) +
                                  '; max-age=' + age + '; path=/';
                document.cookie = 'userid='   + encodeURIComponent(uVal) +
                                  '; max-age=' + age + '; path=/';
            }
            Swal.fire({
                icon: 'success',
                title: '<%=Common.getBahasaConfigJS("Berhasil!")%>',
                text:  result.message ||
                       '<%=Common.getBahasaConfigJS("Otentikasi berhasil. Mengalihkan ke dasbor...")%>',
                showConfirmButton: false,
                timer: 1200,
                showClass: { popup: 'animate__animated animate__zoomIn' },
                hideClass: { popup: 'animate__animated animate__zoomOut' }
            }).then(function() {
                /* Redirect ke URL semula (baru2_next) bukan ke /main */
                window.location.replace(BARU2_NEXT || '<%=rootCtx%>/baru2');
            });
        } else {
            var errMsg = result.message ||
                         '<%=Common.getBahasaConfigJS("Nama pengguna atau kata sandi tidak valid. Silakan periksa kembali.")%>';
            showAlert(errMsg);
            Swal.fire({
                icon: 'warning',
                title: '<%=Common.getBahasaConfigJS("Login Belum Berhasil")%>',
                text:  errMsg,
                confirmButtonText:  '<%=Common.getBahasaConfigJS("Coba Lagi")%>',
                confirmButtonColor: '#dc3545'
            });
            if (pInp) { pInp.focus(); pInp.select(); }
            if (btn) btn.disabled = false;
        }
    } catch(err) {
        var netMsg = '<%=Common.getBahasaConfigJS("Gagal terhubung ke peladen. Periksa koneksi Anda atau coba beberapa saat lagi.")%>';
        showAlert(netMsg);
        Swal.fire({
            icon:  'warning',
            title: '<%=Common.getBahasaConfigJS("Kesalahan Jaringan")%>',
            text:   netMsg,
            confirmButtonColor: '#0d6efd'
        });
        if (btn) btn.disabled = false;
    }
}
</script>
</body>
</html>
