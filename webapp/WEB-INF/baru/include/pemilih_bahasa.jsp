<%@ page pageEncoding="UTF-8" %>
<%@ page import="ais.common.Common" %>
<%@ page import="ais.database.model.Tbmuser" %>
<%--
  Pemilih Bahasa universal yang dapat di-include di halaman /baru mana pun yang TIDAK memakai
  include/header.jsp atau include/navbar.jsp. Menyediakan:
   (1) Pemrosesan parameter ?lang=id|en|ar  -> set bahasa aktif (+ simpan permanen bila login).
   (2) Pemilih bahasa mengambang (fixed, pojok kanan-atas) yang di-inject ke <body> via JavaScript,
       sekaligus membuang parameter 'lang' dari URL (muat ulang bersih dalam bahasa baru).
  Otomatis menyembunyikan diri bila di halaman sudah ada pemilih (#ais-lang-switcher / -navbar).
  Pemakaian:  <jsp:include page="/WEB-INF/baru/include/pemilih_bahasa.jsp" />
--%>
<%
{
	String _plInc = request.getParameter("lang");
	if (_plInc != null && _plInc.trim().length() > 0) {
		Common.initBahasaParameter(_plInc.trim());
	}
}
String _lgAktifInc = Common.currentLang();
String _lgKodeInc = Common.kodeBahasaAktif();
%>
<script>
(function(){
	try {
		var u = new URL(window.location.href);
		if (u.searchParams.has('lang')) { u.searchParams.delete('lang'); window.location.replace(u.toString()); return; }
	} catch(e){}
	var _lg = '<%=_lgKodeInc%>';
	var _ctx = '<%=request.getContextPath()%>';
	var _names = { id:'Indonesia', en:'English', ar:'العربية', zh:'中文' };
	var _flags = { id:'indonesia', en:'united-kingdom', ar:'saudi-arabia', zh:'china' };
	function _ganti(k){
		try { var u=new URL(window.location.href); u.searchParams.set('lang',k); window.location.href=u.toString(); }
		catch(e){ window.location.href='?lang='+k; }
	}
	function _inject(){
		if (document.getElementById('ais-lang-switcher') || document.getElementById('ais-lang-switcher-navbar')) return;
		var wrap = document.createElement('div');
		wrap.id = 'ais-lang-switcher';
		wrap.style.cssText = 'position:fixed;top:6px;right:8px;z-index:20000;';
		var btn = document.createElement('a'); btn.href='javascript:void(0)';
		btn.style.cssText='display:inline-flex;align-items:center;gap:3px;background:rgba(255,255,255,.92);padding:4px 6px;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,.2);cursor:pointer;text-decoration:none;';
		var bim=document.createElement('img'); bim.src=_ctx+'/component/assets/media/flags/'+(_flags[_lg]||_flags.id)+'.svg';
		bim.style.cssText='width:20px;height:13px;border-radius:3px;display:block;box-shadow:0 0 0 1px rgba(0,0,0,.15);';
		var car=document.createElement('span'); car.innerHTML='&#9662;'; car.style.cssText='font-size:10px;color:#94a3b8;';
		btn.appendChild(bim); btn.appendChild(car);
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
