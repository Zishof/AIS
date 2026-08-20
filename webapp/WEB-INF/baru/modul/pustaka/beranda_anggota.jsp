<%@page import="ais.common.Common"%>
<%@page import="ais.common.newui.NewUiCsrfUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String memberCsrf = NewUiCsrfUtil.getToken(request.getSession());
    String memberRnd = Common.getGeneratedBarCode(7);
%>
<section class="library-modern" id="memberPortal<%=memberRnd%>">
  <div class="library-container library-page">
    <header class="library-page-head">
      <div><span class="library-eyebrow">Akun perpustakaan</span><h1>Portal anggota</h1><p class="library-muted" id="memberIdentity<%=memberRnd%>">Memuat identitas dan status keanggotaan…</p></div>
      <button class="library-button" type="button" onclick="window.print()">Cetak ringkasan</button>
    </header>

    <div class="library-kpis" aria-label="Ringkasan akun">
      <article class="library-card library-kpi"><small>Pinjaman aktif</small><strong id="memberActive<%=memberRnd%>">—</strong><span class="library-muted">Sedang berada pada anggota</span></article>
      <article class="library-card library-kpi"><small>Mendekati jatuh tempo</small><strong id="memberDue<%=memberRnd%>">—</strong><span class="library-muted">Dalam tiga hari</span></article>
      <article class="library-card library-kpi"><small>Terlambat</small><strong id="memberOverdue<%=memberRnd%>">—</strong><span class="library-muted">Perlu segera ditindaklanjuti</span></article>
      <article class="library-card library-kpi"><small>Reservasi aktif</small><strong id="memberHolds<%=memberRnd%>">—</strong><span class="library-muted">Belum kedaluwarsa</span></article>
      <article class="library-card library-kpi"><small>Denda tercatat</small><strong id="memberFine<%=memberRnd%>">—</strong><span class="library-muted">Nilai historis yang dinilai sistem</span></article>
      <article class="library-card library-kpi"><small>Notifikasi tindakan</small><strong id="memberNotifications<%=memberRnd%>">—</strong><span class="library-muted">Jatuh tempo, terlambat, dan reservasi</span></article>
    </div>

    <div class="library-card library-card-pad">
      <div class="library-toolbar" role="tablist" aria-label="Aktivitas anggota">
        <button class="library-button library-button-primary" type="button" data-member-tab="loans">Peminjaman</button>
        <button class="library-button" type="button" data-member-tab="holds">Reservasi</button>
        <button class="library-button" type="button" data-member-tab="favorites">Favorit</button>
        <button class="library-button" type="button" data-member-tab="visits">Kunjungan</button>
        <div class="library-toolbar-field"><label for="memberKeyword<%=memberRnd%>">Cari aktivitas</label><input class="library-input" id="memberKeyword<%=memberRnd%>" placeholder="Judul, ISBN, kode, atau lokasi"></div>
        <button class="library-button" type="button" id="memberSearch<%=memberRnd%>">Cari</button>
      </div>
      <div id="memberNotice<%=memberRnd%>" class="library-muted" role="status" aria-live="polite"></div>
      <div class="library-table-wrap"><table class="library-table"><thead id="memberHead<%=memberRnd%>"></thead><tbody id="memberBody<%=memberRnd%>"><tr><td>Memuat data…</td></tr></tbody></table></div>
      <div class="library-pagination"><button class="library-button" id="memberPrev<%=memberRnd%>" type="button">Sebelumnya</button><span id="memberPage<%=memberRnd%>">Halaman 1</span><button class="library-button" id="memberNext<%=memberRnd%>" type="button">Berikutnya</button></div>
    </div>
  </div>
</section>
<script>
(function () {
  var root = '<%=Common.ROOT%>';
  var endpoint = root + '/pustaka?hanya_tampil_jsp=true&p=pustaka&s=_beranda_anggota_service';
  var csrf = '<%=memberCsrf%>', tab = 'loans', page = 1, total = 0, pageSize = 10;
  var esc = window.LibraryModern ? LibraryModern.escapeHtml : function(v){return String(v == null ? '' : v).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];});};
  var body = document.getElementById('memberBody<%=memberRnd%>'), head = document.getElementById('memberHead<%=memberRnd%>'), notice = document.getElementById('memberNotice<%=memberRnd%>');
  function url(action, extra){ var p = new URLSearchParams(Object.assign({action:action,page:page,pageSize:pageSize,keyword:document.getElementById('memberKeyword<%=memberRnd%>').value},extra||{})); return endpoint + '&' + p.toString(); }
  function get(action){ return fetch(url(action),{credentials:'same-origin',headers:{Accept:'application/json'}}).then(function(r){return r.json();}); }
  function mutate(action, data){ var p=new URLSearchParams(Object.assign({action:action,nui_csrf:csrf},data||{})); return fetch(endpoint,{method:'POST',credentials:'same-origin',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8',Accept:'application/json','X-NUI-CSRF':csrf},body:p.toString()}).then(function(r){return r.json();}); }
  function message(text,error){ notice.textContent=text||'';notice.style.color=error?'var(--library-danger)':'var(--library-success)'; }
  function summary(){ get('summary').then(function(r){if(!r.ok)throw new Error(r.error);csrf=r.csrf||csrf;var d=r.data,m=d.member;document.getElementById('memberIdentity<%=memberRnd%>').textContent=m.name+' · Kartu digital '+m.code+' · '+(m.active?'Keanggotaan aktif':'Keanggotaan tidak aktif');document.getElementById('memberActive<%=memberRnd%>').textContent=d.activeLoans;document.getElementById('memberDue<%=memberRnd%>').textContent=d.dueSoon;document.getElementById('memberOverdue<%=memberRnd%>').textContent=d.overdue;document.getElementById('memberHolds<%=memberRnd%>').textContent=d.activeHolds;document.getElementById('memberFine<%=memberRnd%>').textContent=new Intl.NumberFormat('id-ID',{style:'currency',currency:'IDR',maximumFractionDigits:0}).format(d.assessedFine||0);document.getElementById('memberNotifications<%=memberRnd%>').textContent=d.notifications||0;}).catch(function(e){message(e.message,true);}); }
  function renderRows(rows){
    if(tab==='loans'){head.innerHTML='<tr><th>Koleksi</th><th>Pinjam / jatuh tempo</th><th>Status</th><th>Tindakan</th></tr>';body.innerHTML=rows.map(function(x){return '<tr><td><strong>'+esc(x.title)+'</strong><br><span class="library-muted">'+esc(x.authors)+'</span></td><td>'+esc(x.tanggal_pinjam)+'<br><strong>'+esc(x.dueDate||'-')+'</strong></td><td>'+esc(x.status)+' · '+x.renewals+'/'+x.maxRenewals+' perpanjangan</td><td>'+(x.returned?'—':'<button class="library-button" data-renew="'+x.detailId+'">Perpanjang</button>')+'</td></tr>';}).join('');}
    if(tab==='holds'){head.innerHTML='<tr><th>Koleksi</th><th>Perpustakaan</th><th>Kadaluarsa</th><th>Status / tindakan</th></tr>';body.innerHTML=rows.map(function(x){return '<tr><td><strong>'+esc(x.buku)+'</strong><br>'+esc(x.kode)+'</td><td>'+esc(x.library)+'</td><td>'+esc(x.expires)+'</td><td>'+esc(x.status)+(x.status==='Pesan'?' <button class="library-button" data-cancel="'+x.holdId+'">Batalkan</button>':'')+'</td></tr>';}).join('');}
    if(tab==='favorites'){head.innerHTML='<tr><th>Koleksi</th><th>Pengarang</th><th>Ditambahkan</th><th>Tindakan</th></tr>';body.innerHTML=rows.map(function(x){return '<tr><td><strong>'+esc(x.title)+'</strong></td><td>'+esc(x.authors)+'</td><td>'+esc(x.date)+'</td><td><button class="library-button" data-favorite="'+x.itemId+'">Hapus</button></td></tr>';}).join('');}
    if(tab==='visits'){head.innerHTML='<tr><th>Waktu</th><th>Perpustakaan</th><th>Keterangan</th></tr>';body.innerHTML=rows.map(function(x){return '<tr><td>'+esc(x.tanggal)+'</td><td><strong>'+esc(x.perpustakaan)+'</strong></td><td>'+esc(x.keterangan)+'</td></tr>';}).join('');}
    if(!rows.length)body.innerHTML='<tr><td colspan="4" class="library-state">Belum ada data pada bagian ini.</td></tr>';
  }
  function load(){body.innerHTML='<tr><td colspan="4" class="library-state">Memuat data…</td></tr>';get(tab).then(function(r){if(!r.ok)throw new Error(r.error);csrf=r.csrf||csrf;total=r.total||0;renderRows(r.data||[]);document.getElementById('memberPage<%=memberRnd%>').textContent='Halaman '+page+' dari '+Math.max(1,Math.ceil(total/pageSize));document.getElementById('memberPrev<%=memberRnd%>').disabled=page<=1;document.getElementById('memberNext<%=memberRnd%>').disabled=page*pageSize>=total;}).catch(function(e){body.innerHTML='<tr><td colspan="4" class="library-state">'+esc(e.message)+'</td></tr>';});}
  document.querySelectorAll('#memberPortal<%=memberRnd%> [data-member-tab]').forEach(function(b){b.addEventListener('click',function(){document.querySelectorAll('#memberPortal<%=memberRnd%> [data-member-tab]').forEach(function(x){x.classList.remove('library-button-primary');});b.classList.add('library-button-primary');tab=b.getAttribute('data-member-tab');page=1;load();});});
  body.addEventListener('click',function(e){var b=e.target.closest('button');if(!b)return;var action,data;if(b.hasAttribute('data-renew')){action='renew';data={detailId:b.getAttribute('data-renew')};}if(b.hasAttribute('data-cancel')){action='hold_cancel';data={holdId:b.getAttribute('data-cancel')};}if(b.hasAttribute('data-favorite')){action='favorite_toggle';data={itemId:b.getAttribute('data-favorite')};}if(!action)return;b.disabled=true;mutate(action,data).then(function(r){if(!r.ok)throw new Error(r.error);csrf=r.csrf||csrf;message(r.message,false);summary();load();}).catch(function(err){message(err.message,true);b.disabled=false;});});
  document.getElementById('memberSearch<%=memberRnd%>').onclick=function(){page=1;load();};document.getElementById('memberPrev<%=memberRnd%>').onclick=function(){if(page>1){page--;load();}};document.getElementById('memberNext<%=memberRnd%>').onclick=function(){if(page*pageSize<total){page++;load();}};
  summary();load();
}());
</script>
