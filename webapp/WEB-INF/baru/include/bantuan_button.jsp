<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%><%--
    Tombol "Bantuan" mengambang untuk halaman JSP.
    Key panduan diturunkan dari nama berkas JSP yang sedang dirender
    (request.getServletPath()); tombol hanya muncul bila berkas
    WEB-INF/bantuan/<key>.html tersedia. Klik -> modal berisi panduan
    dari servlet /bantuan?key=<key>. Aman disertakan di halaman mana pun.
--%><%
    String __sp = request.getServletPath();
    String __key = __sp == null ? "" : __sp;
    int __sl = Math.max(__key.lastIndexOf('/'), __key.lastIndexOf('\\'));
    if (__sl >= 0) __key = __key.substring(__sl + 1);
    if (__key.toLowerCase().endsWith(".jsp")) __key = __key.substring(0, __key.length() - 4);
    __key = __key.toLowerCase().replaceAll("[^a-z0-9_\\-]", "");
    // Varian visual login (login2/login3/login5) memakai ulang panduan "login".
    if (__key.matches("login[0-9]+")) __key = "login";
    String __rp = __key.length() == 0 ? null : application.getRealPath("/WEB-INF/bantuan/" + __key + ".html");
    boolean __ada = __rp != null && new java.io.File(__rp).isFile();
    // Pusat Panduan (kumpulan panduan per peran) selalu tersedia, terlepas dari
    // ada tidaknya panduan khusus halaman ini.
    String __rpPusat = application.getRealPath("/WEB-INF/bantuan/panduan.html");
    boolean __adaPusat = __rpPusat != null && new java.io.File(__rpPusat).isFile();
    if (__ada || __adaPusat) {
        String __ctx = request.getContextPath();
%>
<% if (__ada) { %>
<div id="kbjspQaFab" onclick="kbjspOpenMode('qa')" title="Tanya jawab sesuai halaman ini"
     style="position:fixed;right:16px;bottom:62px;z-index:99990;cursor:pointer;background:#15803d;color:#fff;
     border-radius:22px;padding:9px 15px;box-shadow:0 4px 14px rgba(21,128,61,.35);
     font:600 13px 'Segoe UI',Arial,sans-serif;display:inline-flex;align-items:center;gap:6px;">
  <span style="font-size:15px;line-height:1;">&#128172;</span><span>Tanya Jawab</span>
</div>
<div id="kbjspFab" onclick="kbjspOpenMode('help')" title="Panduan penggunaan halaman ini"
     style="position:fixed;right:16px;bottom:16px;z-index:99990;cursor:pointer;background:#1d4ed8;color:#fff;
     border-radius:22px;padding:9px 15px;box-shadow:0 4px 14px rgba(29,78,216,.35);
     font:600 13px 'Segoe UI',Arial,sans-serif;display:inline-flex;align-items:center;gap:6px;">
  <span style="font-size:15px;line-height:1;">&#63;</span><span>Bantuan</span>
</div>
<% } %>
<% if (__adaPusat) { %>
<div id="kbjspPusatFab" onclick="kbjspOpenMode('pusat')" title="Kumpulan panduan menurut peran pengguna"
     style="position:fixed;right:16px;bottom:<%= __ada ? "108px" : "16px" %>;z-index:99990;cursor:pointer;
     background:#0f766e;color:#fff;border-radius:22px;padding:9px 15px;
     box-shadow:0 4px 14px rgba(15,118,110,.35);
     font:600 13px 'Segoe UI',Arial,sans-serif;display:inline-flex;align-items:center;gap:6px;">
  <span style="font-size:15px;line-height:1;">&#128218;</span><span>Panduan</span>
</div>
<% } %>
<div id="kbjspOverlay" onclick="if(event.target===this)kbjspClose()"
     style="display:none;position:fixed;inset:0;top:0;left:0;right:0;bottom:0;z-index:99991;
     background:rgba(15,23,42,.55);padding:3vh 3vw;box-sizing:border-box;">
  <div style="max-width:920px;height:94vh;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;
       display:flex;flex-direction:column;box-shadow:0 12px 40px rgba(0,0,0,.35);">
    <div style="flex:0 0 auto;display:flex;justify-content:space-between;align-items:center;background:#eef2f7;
         border-bottom:1px solid #d7dee8;padding:9px 14px;font:600 14px 'Segoe UI',Arial,sans-serif;color:#0f172a;">
      <span id="kbjspTitle">Pusat Bantuan</span>
      <span onclick="kbjspClose()" style="cursor:pointer;font-size:22px;line-height:1;color:#64748b;padding:0 4px;">&times;</span>
    </div>
    <iframe id="kbjspFrame" src="about:blank" style="flex:1 1 auto;border:0;width:100%;height:100%;"></iframe>
  </div>
</div>
<script type="text/javascript">
function kbjspOpen(){ kbjspOpenMode('help'); }
function kbjspOpenMode(mode){
  var f=document.getElementById('kbjspFrame');
  // Bila halaman ini tidak punya panduan khusus, arahkan ke Pusat Panduan.
  if(!<%= __ada %>) mode='pusat';
  var qa=mode==='qa', pusat=mode==='pusat', url;
  if(pusat){ url='<%= __ctx %>/bantuan?key=panduan'; }
  else { url='<%= __ctx %>/bantuan?key=<%= __key %>'+(qa?'&mode=qa':''); }
  if(f.getAttribute('data-mode')!==mode){ f.src=url; f.setAttribute('data-mode',mode); }
  document.getElementById('kbjspTitle').textContent=
      pusat?'Pusat Panduan':(qa?'Tanya Jawab Halaman':'Pusat Bantuan');
  document.getElementById('kbjspOverlay').style.display='block';
}
function kbjspClose(){ document.getElementById('kbjspOverlay').style.display='none'; }
document.addEventListener('keydown',function(e){ if(e.key==='Escape'||e.keyCode===27) kbjspClose(); });
</script>
<% } %>
