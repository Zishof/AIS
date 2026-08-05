<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
/* Ganti Bahasa (Indonesia/English/Arab): ?lang=id|en|ar → set bahasa aktif (+ simpan permanen bila login).
   Pengalihan bersih (buang parameter lang) dilakukan di sisi klien pada <script> di bawah. */
String _plHdr = request.getParameter("lang");
if (_plHdr != null && _plHdr.trim().length() > 0) {
	ais.common.Common.initBahasaParameter(_plHdr.trim());
}
String _lgAktifHdr = ais.common.Common.currentLang();
String _lgKode = ais.common.Common.kodeBahasaAktif();
ais.database.model.sekolah.Sekolah sekolah = ais.action.master.sekolah.util.SekolahUtil.getSekolah(request);
PerguruanTinggi pt = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
String judulNamaPerguruanTinggi = pt != null && pt.getNama() != null ? pt.getNama() : "";
String Telepon = pt != null && pt.getTelepon() != null ? pt.getTelepon() : "";
String Motto = pt != null && pt.getMotto() != null ? pt.getMotto() : "";
String Alamat1 = pt != null && pt.getAlamat1() != null ? pt.getAlamat1() : "";
String Email = pt != null && pt.getEmail() != null ? pt.getEmail() : "";
String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");

String judulHeader = "";
try {
    judulHeader = Common.getKonfigurasi("judul_header", "eCampus").getNilai();
} catch (Exception e) {
    judulHeader = "eCampus"; // Fallback nilai jika konfigurasi tidak ditemukan
}
%>
<!DOCTYPE html>
<%
// Atribut lang/dir mengikuti bahasa aktif — Arab = RTL, lainnya LTR. Sebelumnya hardcode id-ID/ltr
// sehingga halaman tidak pernah beralih ke RTL walau bahasa Arab dipilih.
String _htmlDirHdr = "ar".equals(_lgKode) ? "rtl" : "ltr";
String _htmlLangHdr = "id".equals(_lgKode) ? "id-ID" : _lgKode;
%>
<html data-bs-theme="light" lang="<%=_htmlLangHdr%>" dir="<%=_htmlDirHdr%>">

<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    
    <title><%= judulHeader + " | " + judulNamaPerguruanTinggi %></title>
    <link rel="shortcut icon" href="<%=logo_PerguruanTinggi %>" type="image/png"/>
    <link rel="manifest" href="<%=request.getContextPath() %>/component/uiux/assets/img/favicons/manifest.json">
    <meta name="msapplication-TileImage" content="<%=request.getContextPath() %>/component/uiux/assets/img/favicons/mstile-150x150.png">
    <meta name="theme-color" content="#ffffff">

    <script src="<%=request.getContextPath() %>/component/uiux/assets/js/config.js"></script>
    <script src="<%=request.getContextPath() %>/component/uiux/vendors/simplebar/simplebar.min.js"></script>
    <!-- Helper pesan formal (sukses/gagal) reusable -- sejalan dengan ais.common.PesanFormalHelper (Java/ZK) -->
    <script src="<%=request.getContextPath() %>/js/pesan-formal.js"></script>
    <link href="https://cdn.jsdelivr.net/npm/summernote@0.8.18/dist/summernote-lite.min.css" rel="stylesheet">
    <script data-cfasync="false" src="https://code.jquery.com/jquery-3.5.1.min.js"></script>
    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/summernote@0.8.18/dist/summernote.min.js"></script>
    <script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/chart.js"></script>

    <link rel="preconnect" href="https://fonts.gstatic.com">
    <link href="https://fonts.googleapis.com/css?family=Open+Sans:300,400,500,600,700&amp;display=swap" rel="stylesheet">
    
    <link href="<%=request.getContextPath() %>/component/uiux/vendors/simplebar/simplebar.min.css" rel="stylesheet">
    <link href="<%=request.getContextPath() %>/component/uiux/assets/css/theme-rtl.css" rel="stylesheet" id="style-rtl">
    <link href="<%=request.getContextPath() %>/component/uiux/assets/css/theme.css" rel="stylesheet" id="style-default">
    <link href="<%=request.getContextPath() %>/component/uiux/assets/css/user-rtl.css" rel="stylesheet" id="user-style-rtl">
    <link href="<%=request.getContextPath() %>/component/uiux/assets/css/user.css" rel="stylesheet" id="user-style-default">
    
    <link href="<%=request.getContextPath() %>/css/bandbox.css" rel="stylesheet">
    <link href="<%=request.getContextPath() %>/css/timeline.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css"/>
	
    <% long cacheBuster = System.currentTimeMillis(); %>
    
   

    <% 
    String rawCss = pt.getCss();
    if (rawCss != null && !rawCss.trim().isEmpty()) {
        String fileName = rawCss.substring(rawCss.lastIndexOf("/") + 1);
    %>
    	 <link href="<%=request.getContextPath() %>/css/baru/base-theme.css?v=<%= cacheBuster %>" rel="stylesheet">
    
    	<link href="<%=request.getContextPath() %>/css/baru/custom-elearning.css?v=<%= cacheBuster %>" rel="stylesheet">
        <link href="<%=request.getContextPath() %>/css/baru/<%= fileName %>?v=<%= cacheBuster %>" rel="stylesheet">
    <% } %>
    
    <script>
      var isRTL = JSON.parse(localStorage.getItem('isRTL'));
      if (isRTL) {
        var linkDefault = document.getElementById('style-default');
        var userLinkDefault = document.getElementById('user-style-default');
        if(linkDefault) linkDefault.setAttribute('disabled', true);
        if(userLinkDefault) userLinkDefault.setAttribute('disabled', true);
        document.querySelector('html').setAttribute('dir', 'rtl');
      } else {
        var linkRTL = document.getElementById('style-rtl');
        var userLinkRTL = document.getElementById('user-style-rtl');
        if(linkRTL) linkRTL.setAttribute('disabled', true);
        if(userLinkRTL) userLinkRTL.setAttribute('disabled', true);
      }
    </script>

    <!-- ═══ Pemilih Bahasa universal (Indonesia/English/Arab) ═══ -->
    <script>
    (function(){
      try {
        var u = new URL(window.location.href);
        if (u.searchParams.has('lang')) {   // bahasa sudah di-set server; muat ulang bersih tanpa param
          u.searchParams.delete('lang');
          window.location.replace(u.toString());
          return;
        }
      } catch(e){}
      var _lg = '<%=_lgKode%>';
      var _ctx = '<%=request.getContextPath()%>';
      var _names = { id:'Indonesia', en:'English', ar:'العربية', zh:'中文' };
      var _flags = { id:'indonesia', en:'united-kingdom', ar:'saudi-arabia', zh:'china' };
      function _ganti(k){
        try { var u=new URL(window.location.href); u.searchParams.set('lang',k); window.location.href=u.toString(); }
        catch(e){ window.location.href='?lang='+k; }
      }
      function _inject(){
        // Jangan tampil ganda: bila pemilih di navbar sudah ada, lewati floating.
        // Cek via id DAN class .ais-lang-dd (navbar.jsp) — sebagian halaman merender navbar tanpa id.
        if (document.getElementById('ais-lang-switcher') || document.getElementById('ais-lang-switcher-navbar')
            || document.querySelector('.ais-lang-dd') || document.querySelector('.ais-lang-menu')) return;
        var wrap = document.createElement('div');
        wrap.id = 'ais-lang-switcher';
        wrap.style.cssText = 'position:fixed;top:6px;right:8px;z-index:20000;';
        // Tombol: bendera AKTIF + panah (combobox tertutup)
        var btn = document.createElement('a'); btn.href='javascript:void(0)';
        btn.style.cssText='display:inline-flex;align-items:center;gap:3px;background:rgba(255,255,255,.92);padding:4px 6px;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,.2);cursor:pointer;text-decoration:none;';
        var bim=document.createElement('img'); bim.src=_ctx+'/component/assets/media/flags/'+(_flags[_lg]||_flags.id)+'.svg';
        bim.style.cssText='width:20px;height:13px;border-radius:3px;display:block;box-shadow:0 0 0 1px rgba(0,0,0,.15);';
        var car=document.createElement('span'); car.innerHTML='&#9662;'; car.style.cssText='font-size:10px;color:#94a3b8;';
        btn.appendChild(bim); btn.appendChild(car);
        // Menu: daftar bendera
        var menu=document.createElement('div');
        menu.style.cssText='display:none;position:absolute;right:0;top:100%;margin-top:6px;background:#fff;border:1px solid #e2e8f0;border-radius:8px;box-shadow:0 6px 18px rgba(0,0,0,.18);padding:6px;';
        ['id','en','ar','zh'].forEach(function(k){
          var a=document.createElement('a'); a.href='javascript:void(0)'; a.title=_names[k]; a.style.cssText='display:block;padding:5px;';
          var im=document.createElement('img'); im.src=_ctx+'/component/assets/media/flags/'+_flags[k]+'.svg';
          im.style.cssText='width:30px;height:20px;border-radius:3px;display:block;';
          a.appendChild(im); a.addEventListener('click', function(){ _ganti(k); }); menu.appendChild(a);
        });
        btn.addEventListener('click', function(ev){ ev.stopPropagation(); menu.style.display=(menu.style.display==='block')?'none':'block'; });
        document.addEventListener('click', function(e){ if(!wrap.contains(e.target)) menu.style.display='none'; });
        wrap.appendChild(btn); wrap.appendChild(menu);
        (document.body || document.documentElement).appendChild(wrap);
      }
      if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', _inject); else _inject();
    })();
    </script>

</head>