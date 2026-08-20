<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%><%--
    Tombol bantuan mengambang untuk halaman JSP.

    Kunci panduan diturunkan dari nama berkas JSP yang sedang dirender
    (request.getServletPath()); panduan khusus halaman hanya ditawarkan bila
    berkas WEB-INF/bantuan/<kunci>.html memang ada. Aman disertakan di halaman
    mana pun.

    BENTUK KEBAB (satu tombol, menu muncul saat diklik).
    Sebelumnya tiga tombol pil ditumpuk vertikal di sudut kanan-bawah. Halaman
    pemasaran (erp.jsp) juga punya tombol mengambangnya sendiri di sudut yang
    SAMA, sehingga keempatnya berebut ruang dan saling bertindih. Dengan satu
    tombol bulat 48px yang membuka menu, sisi kanan-bawah hanya terpakai satu
    slot dan tumpang tindih itu hilang.
--%><%
    // Sakelar induk ON/OFF (konfigurasi "bantuan_tombol_tampil", default aktif) --
    // sama persis dengan yang dipakai sisi ZK, sehingga satu setelan mematikan tombol
    // Bantuan di SELURUH halaman. Bila konfigurasi gagal dibaca, pertahankan perilaku
    // lama: tombol tetap tampil.
    boolean __kbTampil = true;
    try {
        __kbTampil = ais.action.master.helper.BantuanGlobalHook.tombolBantuanAktif();
    } catch (Throwable __kbT) {
        __kbTampil = true;
    }
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
    // Pusat Panduan (seluruh panduan, menurut peran dan per modul) selalu tersedia,
    // terlepas dari ada tidaknya panduan khusus halaman ini.
    String __rpPusat = application.getRealPath("/WEB-INF/bantuan/panduan.html");
    boolean __adaPusat = __rpPusat != null && new java.io.File(__rpPusat).isFile();
    if (__kbTampil && (__ada || __adaPusat)) {
        String __ctx = request.getContextPath();
%>
<style type="text/css">
/* Sudut kanan-bawah hanya dipakai SATU slot: tombol bulat 48px pada bottom:16px,
   sehingga tepi atasnya di 64px. Tombol mengambang milik halaman lain cukup
   ditempatkan di atas garis itu untuk terhindar dari tumpang tindih. */
#kbjspWrap{position:fixed;right:16px;bottom:16px;z-index:99990;
  font:600 13px 'Segoe UI',Arial,sans-serif;}
#kbjspFab{width:48px;height:48px;border:0;border-radius:50%;cursor:pointer;
  background:#1d4ed8;color:#fff;font:700 20px 'Segoe UI',Arial,sans-serif;line-height:1;
  box-shadow:0 6px 18px rgba(29,78,216,.38);display:flex;align-items:center;
  justify-content:center;padding:0;transition:transform .15s ease,background .15s ease;}
#kbjspFab:hover{background:#1e40af;transform:translateY(-2px);}
#kbjspFab:focus-visible{outline:3px solid #93c5fd;outline-offset:2px;}
#kbjspWrap.kbjsp-buka #kbjspFab{background:#0f172a;transform:rotate(45deg);}
#kbjspMenu{position:absolute;right:0;bottom:60px;min-width:224px;background:#fff;
  border:1px solid #dbe3ec;border-radius:12px;overflow:hidden;
  box-shadow:0 14px 38px rgba(15,23,42,.20);display:none;}
#kbjspWrap.kbjsp-buka #kbjspMenu{display:block;}
#kbjspMenu .kbjsp-judul{padding:9px 14px 7px;font-size:11px;letter-spacing:.06em;
  text-transform:uppercase;color:#64748b;background:#f8fafc;border-bottom:1px solid #eef2f7;}
#kbjspMenu button{display:flex;align-items:center;gap:10px;width:100%;border:0;
  background:none;text-align:left;padding:11px 14px;cursor:pointer;color:#0f172a;
  font:600 13px 'Segoe UI',Arial,sans-serif;}
#kbjspMenu button:hover{background:#eff6ff;color:#1d4ed8;}
#kbjspMenu button:focus-visible{outline:2px solid #1d4ed8;outline-offset:-2px;}
#kbjspMenu button + button{border-top:1px solid #f1f5f9;}
#kbjspMenu .kbjsp-ikon{font-size:15px;line-height:1;width:18px;text-align:center;}
@media (prefers-reduced-motion:reduce){#kbjspFab{transition:none;}}
</style>

<div id="kbjspWrap">
  <div id="kbjspMenu" role="menu" aria-label="Pilihan bantuan">
    <div class="kbjsp-judul">Bantuan</div>
<% if (__ada) { %>
    <button type="button" role="menuitem" onclick="kbjspOpenMode('help')"
            title="Panduan modul yang sedang Anda buka">
      <span class="kbjsp-ikon">&#128214;</span><span>Bantuan Halaman Ini</span>
    </button>
    <button type="button" role="menuitem" onclick="kbjspOpenMode('qa')"
            title="Tanya jawab sesuai halaman ini">
      <span class="kbjsp-ikon">&#128172;</span><span>Tanya Jawab</span>
    </button>
<% } %>
<% if (__adaPusat) { %>
    <button type="button" role="menuitem" onclick="kbjspOpenMode('pusat')"
            title="Daftar seluruh panduan: menurut peran dan per modul">
      <span class="kbjsp-ikon">&#128218;</span><span>Semua Panduan</span>
    </button>
<% } %>
  </div>
  <button type="button" id="kbjspFab" aria-haspopup="true" aria-expanded="false"
          aria-controls="kbjspMenu" title="Bantuan" onclick="kbjspToggle(event)">?</button>
</div>

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
function kbjspMenuTutup(){
  var w=document.getElementById('kbjspWrap');
  if(!w){return;}
  w.className='';
  var f=document.getElementById('kbjspFab');
  if(f){f.setAttribute('aria-expanded','false');}
}
function kbjspToggle(ev){
  if(ev){ev.stopPropagation();}
  var w=document.getElementById('kbjspWrap');
  if(!w){return;}
  var buka=w.className.indexOf('kbjsp-buka')<0;
  w.className=buka?'kbjsp-buka':'';
  var f=document.getElementById('kbjspFab');
  if(f){f.setAttribute('aria-expanded',buka?'true':'false');}
}
/* Dipertahankan demi pemanggil lama yang mungkin masih memakainya. */
function kbjspOpen(){ kbjspOpenMode('help'); }
function kbjspOpenMode(mode){
  kbjspMenuTutup();
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
/* Klik di luar menutup menu; tombol & menu sendiri dikecualikan. */
document.addEventListener('click',function(e){
  var w=document.getElementById('kbjspWrap');
  if(w && w.className.indexOf('kbjsp-buka')>=0 && !w.contains(e.target)){ kbjspMenuTutup(); }
});
document.addEventListener('keydown',function(e){
  if(e.key==='Escape'||e.keyCode===27){ kbjspMenuTutup(); kbjspClose(); }
});
</script>
<% } %>
