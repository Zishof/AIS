<%--
  Dasbor "Laporan Pergudangan" (Outlet & Gudang) — versi JSP.
  KPI + 4 visual (Bar nilai per lokasi, Donut komposisi per jenis, Tren mutasi harian, Radar profil
  lokasi) memakai SVG HTML+CSS (TANPA JFreeChart) + tabel rincian + tombol Download Excel.
  Data via laporan_pergudangan/service.jsp (reuse StokLokasiUtil). Preset jenis via attribute/param.
--%>
<%@page import="ais.common.Common"%>
<%
String rp = ais.common.Common.getGeneratedBarCode(6);
String presetJns = (String) request.getAttribute("presetJenisNama");
if (presetJns == null) presetJns = request.getParameter("jns");
if (presetJns == null) presetJns = "";
String judul = (String) request.getAttribute("judulLaporan");
if (judul == null || judul.trim().length() == 0) judul = "Laporan Pergudangan";
String svc = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Flaporan_pergudangan&s=service";
%>
<div class="lpg-<%=rp%>">
  <style>
    .lpg-<%=rp%> .grid2{display:grid;grid-template-columns:repeat(auto-fit,minmax(320px,1fr));gap:14px}
    .lpg-<%=rp%> .kpis{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:10px}
    .lpg-<%=rp%> .kpi{border:1px solid #eef2f7;border-radius:14px;padding:12px 14px;background:#fff}
    .lpg-<%=rp%> .kpi .n{font-size:1.25rem;font-weight:800;color:#0f172a}
    .lpg-<%=rp%> .kpi .l{font-size:.72rem;color:#64748b;text-transform:uppercase;letter-spacing:.03em}
    .lpg-<%=rp%> .pan{border:1px solid #eef2f7;border-radius:16px;background:#fff;padding:14px}
    .lpg-<%=rp%> .pan h6{font-weight:800;margin:0}
    .lpg-<%=rp%> .pan .desc{font-size:.76rem;color:#64748b;margin:2px 0 10px}
    .lpg-<%=rp%> .lg{display:flex;flex-wrap:wrap;gap:6px 14px;font-size:.75rem;color:#475569;margin-top:8px}
    .lpg-<%=rp%> .lg i{width:10px;height:10px;border-radius:3px;display:inline-block;margin-right:5px;vertical-align:middle}
    .lpg-<%=rp%> table td,.lpg-<%=rp%> table th{vertical-align:middle}
    .lpg-<%=rp%> .subrow{background:#f8fafc;font-weight:700}
  </style>

  <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
    <div>
      <h5 class="fw-bold mb-0"><i class="fas fa-warehouse text-primary me-2"></i><span id="lpgJudul<%=rp%>"><%=Common.getBahasaConfig(judul)%></span></h5>
      <div class="text-muted small"><%=Common.getBahasaConfig("Ringkasan stok & nilai barang di tiap tempat, lengkap dengan grafik supaya cepat dibaca.")%></div>
    </div>
    <div class="d-flex gap-2 flex-wrap">
      <select id="lpgJenis<%=rp%>" class="form-select form-select-sm" style="max-width:170px"><option value="">Semua jenis</option></select>
      <select id="lpgLokasi<%=rp%>" class="form-select form-select-sm" style="max-width:170px"><option value="">Semua lokasi</option></select>
      <input id="lpgCari<%=rp%>" class="form-control form-control-sm" style="max-width:170px" placeholder="Cari produk…"/>
      <button class="btn btn-primary btn-sm" onclick="lpgLoad<%=rp%>()"><i class="fas fa-filter me-1"></i>Terapkan</button>
      <button class="btn btn-success btn-sm" onclick="lpgExcel<%=rp%>()"><i class="fas fa-file-excel me-1"></i>Download Excel</button>
    </div>
  </div>

  <div class="kpis mb-3" id="lpgKpi<%=rp%>"></div>

  <div class="grid2 mb-3">
    <div class="pan">
      <h6><i class="fas fa-chart-bar text-primary me-1"></i>Nilai Stok per Lokasi</h6>
      <div class="desc"><%=Common.getBahasaConfig("Tempat mana yang menyimpan barang paling banyak nilainya.")%></div>
      <div id="lpgBar<%=rp%>"></div>
    </div>
    <div class="pan">
      <h6><i class="fas fa-chart-pie text-primary me-1"></i>Komposisi per Jenis</h6>
      <div class="desc"><%=Common.getBahasaConfig("Perbandingan nilai barang antar jenis tempat (Gudang, Outlet, dll).")%></div>
      <div id="lpgDonut<%=rp%>"></div>
    </div>
    <div class="pan">
      <h6><i class="fas fa-chart-line text-primary me-1"></i>Tren Mutasi Harian (30 hari)</h6>
      <div class="desc"><%=Common.getBahasaConfig("Naik-turun barang masuk dan keluar tiap hari sebulan terakhir.")%></div>
      <div id="lpgTrend<%=rp%>"></div>
    </div>
    <div class="pan">
      <h6><i class="fas fa-bullseye text-primary me-1"></i>Profil Lokasi (Radar)</h6>
      <div class="desc"><%=Common.getBahasaConfig("Membandingkan beberapa tempat dari sisi nilai, ragam barang, dan jumlah stok.")%></div>
      <div id="lpgRadar<%=rp%>"></div>
    </div>
  </div>

  <div class="pan mb-3">
    <h6><i class="fas fa-sitemap text-primary me-1"></i>Rekap Nilai Stok per Gudang (Hierarki Pusat/Cabang)</h6>
    <div class="desc"><%=Common.getBahasaConfig("Nilai persediaan dikelompokkan per Gudang — kolom \"Induk\" menunjukkan gudang pusatnya bila ini gudang cabang. Baris \"(Tanpa Gudang)\" berarti Lokasi belum ditautkan ke Gudang manapun (atur lewat tombol \"Kelola Gudang\" pada Master Lokasi).")%></div>
    <div class="table-responsive">
      <table class="table table-sm table-hover align-middle">
        <thead class="table-light"><tr>
          <th><%= Common.getBahasaConfig("Gudang") %></th><th><%= Common.getBahasaConfig("Induk") %></th>
          <th class="text-end"><%= Common.getBahasaConfig("Lokasi") %></th><th class="text-end"><%= Common.getBahasaConfig("Jenis Barang") %></th>
          <th class="text-end"><%= Common.getBahasaConfig("Nilai (Rp)") %></th>
        </tr></thead>
        <tbody id="lpgGudangBody<%=rp%>"><tr><td colspan="5" class="text-center text-muted py-4"><i class="fas fa-spinner fa-spin me-1"></i>Memuat…</td></tr></tbody>
      </table>
    </div>
  </div>

  <div class="pan">
    <h6><i class="fas fa-table text-primary me-1"></i>Rincian Stok &amp; Nilai</h6>
    <div class="desc"><%=Common.getBahasaConfig("Daftar barang per tempat: jumlah stok, serta nilainya versi harga beli dan versi rata-rata biaya.")%></div>
    <div class="table-responsive">
      <table class="table table-sm table-hover align-middle" id="lpgTable<%=rp%>">
        <thead class="table-light"><tr>
          <th><%= Common.getBahasaConfig("Lokasi / Produk") %></th><th class="text-end"><%= Common.getBahasaConfig("Qty") %></th><th class="text-end"><%= Common.getBahasaConfig("Harga Beli") %></th>
          <th class="text-end"><%= Common.getBahasaConfig("Nilai (Beli)") %></th><th class="text-end"><%= Common.getBahasaConfig("Rata2 Biaya") %></th><th class="text-end"><%= Common.getBahasaConfig("Nilai (Rata2)") %></th>
        </tr></thead>
        <tbody id="lpgBody<%=rp%>"><tr><td colspan="6" class="text-center text-muted py-4"><i class="fas fa-spinner fa-spin me-1"></i>Memuat…</td></tr></tbody>
      </table>
    </div>
  </div>
</div>

<script>
(function(){
  var SVC='<%=svc%>', PRESET='<%=presetJns%>'.toLowerCase(), rekap=[], tren=[], jenis=[], lokasi=[], perGudang=[], presetDone=false;
  function esc(s){return (s==null?'':String(s)).replace(/[&<>"]/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c];});}
  function fmt(n){ return (Math.round((n||0))).toLocaleString('id-ID'); }
  function fmt2(n){ return (Math.round((n||0)*100)/100).toLocaleString('id-ID'); }
  var PAL=['#0d6efd','#16a34a','#f59e0b','#dc2626','#7c3aed','#0891b2','#db2777','#65a30d','#ea580c','#334155'];

  function fill(el,list,empty){ var h=empty?'<option value="">'+empty+'</option>':''; list.forEach(function(x){h+='<option value="'+x.id+'">'+esc(x.nama)+'</option>';}); el.innerHTML=h; }

  // ---------- SVG chart builders (reusable) ----------
  function svgBar(items){ // horizontal bars
    if(!items.length) return '<div class="text-muted small py-3">Tidak ada data.</div>';
    var max=Math.max.apply(null,items.map(function(i){return i.value;}))||1, W=440,rowH=26,H=items.length*rowH+6, lblW=120, barW=W-lblW-70;
    var s='<svg viewBox="0 0 '+W+' '+H+'" width="100%" style="max-height:'+(H+4)+'px">';
    items.forEach(function(it,i){ var y=i*rowH+4, w=Math.max(2,barW*it.value/max);
      s+='<text x="0" y="'+(y+15)+'" font-size="11" fill="#334155">'+esc(it.label.length>16?it.label.substr(0,15)+'…':it.label)+'</text>';
      s+='<rect x="'+lblW+'" y="'+y+'" width="'+w+'" height="16" rx="4" fill="'+(it.color||PAL[i%PAL.length])+'"></rect>';
      s+='<text x="'+(lblW+w+5)+'" y="'+(y+13)+'" font-size="10.5" fill="#64748b">'+fmt(it.value)+'</text>';
    }); return s+'</svg>';
  }
  function svgDonut(items){
    var tot=items.reduce(function(a,b){return a+b.value;},0); if(tot<=0) return '<div class="text-muted small py-3">Tidak ada data.</div>';
    var cx=90,cy=90,r=70,rin=42,a=-Math.PI/2,s='<svg viewBox="0 0 300 180" width="100%" style="max-height:190px">';
    items.forEach(function(it,i){ var frac=it.value/tot, a2=a+frac*2*Math.PI, x1=cx+r*Math.cos(a),y1=cy+r*Math.sin(a),x2=cx+r*Math.cos(a2),y2=cy+r*Math.sin(a2);
      var xi2=cx+rin*Math.cos(a2),yi2=cy+rin*Math.sin(a2),xi1=cx+rin*Math.cos(a),yi1=cy+rin*Math.sin(a),big=frac>0.5?1:0;
      s+='<path d="M'+x1+' '+y1+' A'+r+' '+r+' 0 '+big+' 1 '+x2+' '+y2+' L'+xi2+' '+yi2+' A'+rin+' '+rin+' 0 '+big+' 0 '+xi1+' '+yi1+' Z" fill="'+(it.color||PAL[i%PAL.length])+'"></path>'; a=a2; });
    s+='<text x="'+cx+'" y="'+(cy-2)+'" text-anchor="middle" font-size="12" font-weight="700" fill="#0f172a">'+fmt(tot)+'</text>';
    s+='<text x="'+cx+'" y="'+(cy+14)+'" text-anchor="middle" font-size="9" fill="#64748b">Total Nilai</text></svg>';
    var lg='<div class="lg">'; items.forEach(function(it,i){ lg+='<span><i style="background:'+(it.color||PAL[i%PAL.length])+'"></i>'+esc(it.label)+' ('+Math.round(it.value/tot*100)+'%)</span>'; });
    return s+lg+'</div>';
  }
  function svgTrend(rows){
    if(!rows.length) return '<div class="text-muted small py-3">Belum ada mutasi.</div>';
    var W=440,H=170,pad=26, max=Math.max(1,Math.max.apply(null,rows.map(function(r){return Math.max(r.masuk,r.keluar);})));
    var xw=(W-pad*2)/Math.max(1,rows.length-1);
    function pts(key){ return rows.map(function(r,i){ return (pad+i*xw)+','+(H-pad-(H-pad*2)*r[key]/max); }).join(' '); }
    var s='<svg viewBox="0 0 '+W+' '+H+'" width="100%" style="max-height:180px">';
    s+='<line x1="'+pad+'" y1="'+(H-pad)+'" x2="'+(W-pad)+'" y2="'+(H-pad)+'" stroke="#e2e8f0"></line>';
    s+='<polyline fill="none" stroke="#16a34a" stroke-width="2" points="'+pts('masuk')+'"></polyline>';
    s+='<polyline fill="none" stroke="#dc2626" stroke-width="2" points="'+pts('keluar')+'"></polyline>';
    s+='<text x="'+pad+'" y="14" font-size="10" fill="#16a34a">■ Masuk</text><text x="'+(pad+70)+'" y="14" font-size="10" fill="#dc2626">■ Keluar</text>';
    s+='<text x="'+pad+'" y="'+(H-8)+'" font-size="9" fill="#94a3b8">'+esc(rows[0].tgl)+'</text>';
    s+='<text x="'+(W-pad)+'" y="'+(H-8)+'" font-size="9" fill="#94a3b8" text-anchor="end">'+esc(rows[rows.length-1].tgl)+'</text>';
    return s+'</svg>';
  }
  function svgRadar(axes, series){
    if(!series.length||!axes.length) return '<div class="text-muted small py-3">Tidak ada data.</div>';
    var cx=110,cy=100,r=80,n=axes.length,s='<svg viewBox="0 0 300 200" width="100%" style="max-height:210px">';
    for(var g=1;g<=3;g++){ var pp=''; for(var i=0;i<n;i++){var a=-Math.PI/2+i*2*Math.PI/n; pp+=(cx+r*g/3*Math.cos(a))+','+(cy+r*g/3*Math.sin(a))+' ';} s+='<polygon points="'+pp+'" fill="none" stroke="#e2e8f0"></polygon>'; }
    for(i=0;i<n;i++){ a=-Math.PI/2+i*2*Math.PI/n; s+='<text x="'+(cx+(r+14)*Math.cos(a))+'" y="'+(cy+(r+14)*Math.sin(a))+'" font-size="9" fill="#64748b" text-anchor="middle">'+esc(axes[i])+'</text>'; }
    series.forEach(function(se,k){ var pp=''; for(i=0;i<n;i++){a=-Math.PI/2+i*2*Math.PI/n; var v=Math.max(0,Math.min(1,se.values[i])); pp+=(cx+r*v*Math.cos(a))+','+(cy+r*v*Math.sin(a))+' ';}
      s+='<polygon points="'+pp+'" fill="'+se.color+'22" stroke="'+se.color+'" stroke-width="1.5"></polygon>'; });
    var lg='<div class="lg">'; series.forEach(function(se){ lg+='<span><i style="background:'+se.color+'"></i>'+esc(se.name)+'</span>'; });
    return s+'</svg>'+lg+'</div>';
  }

  function agg(byKey, valKey){ var m={},arr=[]; rekap.forEach(function(r){ var k=r[byKey]||'-'; if(!m[k]){m[k]={label:r[byKey==='lokasi'?'lokasi':'jenis'],value:0,warna:r.jenisWarna,items:{}};arr.push(k);} m[k].value+=r[valKey]||0; m[k].items[r.produkId]=1; }); return {m:m,keys:arr}; }

  function render(){
    // KPI
    var lokSet={},prdSet={},qty=0,nb=0,na=0;
    rekap.forEach(function(r){ lokSet[r.lokasiId]=1; prdSet[r.produkId]=1; qty+=r.qty; nb+=r.nilaiBeli; na+=r.nilaiAvg; });
    var kpi=[['Lokasi',Object.keys(lokSet).length],['Jenis Barang',Object.keys(prdSet).length],['Total Qty',fmt2(qty)],['Nilai (Harga Beli)','Rp '+fmt(nb)],['Nilai (Rata2 Biaya)','Rp '+fmt(na)]];
    document.getElementById('lpgKpi<%=rp%>').innerHTML=kpi.map(function(k){return '<div class="kpi"><div class="n">'+k[1]+'</div><div class="l">'+k[0]+'</div></div>';}).join('');

    // Bar per lokasi (nilai beli)
    var perLok=agg('lokasi','nilaiBeli'); var barItems=perLok.keys.map(function(k,i){return {label:perLok.m[k].label,value:perLok.m[k].value,color:PAL[i%PAL.length]};}).sort(function(a,b){return b.value-a.value;}).slice(0,10);
    document.getElementById('lpgBar<%=rp%>').innerHTML=svgBar(barItems);
    // Donut per jenis (nilai beli)
    var perJns={},jk=[]; rekap.forEach(function(r){ var k=r.jenis||'(tanpa jenis)'; if(!perJns[k]){perJns[k]={label:k,value:0,color:r.jenisWarna};jk.push(k);} perJns[k].value+=r.nilaiBeli; });
    document.getElementById('lpgDonut<%=rp%>').innerHTML=svgDonut(jk.map(function(k,i){return {label:k,value:perJns[k].value,color:perJns[k].color||PAL[i%PAL.length]};}));
    // Trend
    document.getElementById('lpgTrend<%=rp%>').innerHTML=svgTrend(tren);
    // Radar: top 4 lokasi, metrik [Nilai, Ragam Barang, Total Qty]
    var lokStat={},lo=[]; rekap.forEach(function(r){ if(!lokStat[r.lokasiId]){lokStat[r.lokasiId]={name:r.lokasi,nilai:0,item:{},qty:0};lo.push(r.lokasiId);} var st=lokStat[r.lokasiId]; st.nilai+=r.nilaiBeli; st.item[r.produkId]=1; st.qty+=r.qty; });
    var stats=lo.map(function(id){var s=lokStat[id];return {name:s.name,nilai:s.nilai,ragam:Object.keys(s.item).length,qty:s.qty};}).sort(function(a,b){return b.nilai-a.nilai;}).slice(0,4);
    var mN=Math.max.apply(null,stats.map(function(s){return s.nilai;}))||1, mR=Math.max.apply(null,stats.map(function(s){return s.ragam;}))||1, mQ=Math.max.apply(null,stats.map(function(s){return s.qty;}))||1;
    document.getElementById('lpgRadar<%=rp%>').innerHTML=svgRadar(['Nilai','Ragam','Qty'], stats.map(function(s,i){return {name:s.name,color:PAL[i%PAL.length],values:[s.nilai/mN,s.ragam/mR,s.qty/mQ]};}));

    // Tabel grup per lokasi + subtotal + grand total
    var b=document.getElementById('lpgBody<%=rp%>'), h='', curr=null, sub={q:0,nb:0,na:0}, g={q:0,nb:0,na:0};
    function subRow(name){ h+='<tr class="subrow"><td>Σ '+esc(name)+'</td><td class="text-end">'+fmt2(sub.q)+'</td><td></td><td class="text-end">'+fmt(sub.nb)+'</td><td></td><td class="text-end">'+fmt(sub.na)+'</td></tr>'; }
    if(!rekap.length){ b.innerHTML='<tr><td colspan="6" class="text-center text-muted py-4">Belum ada stok. Catat lewat "Mutasi Gudang".</td></tr>'; return; }
    rekap.forEach(function(r){
      if(curr!==null && r.lokasi!==curr){ subRow(curr); sub={q:0,nb:0,na:0}; }
      if(r.lokasi!==curr){ curr=r.lokasi; h+='<tr class="table-light"><td colspan="6" class="fw-bold"><i class="fas fa-map-marker-alt text-primary me-1"></i>'+esc(r.lokasi)+(r.jenis?' <span class="badge rounded-pill" style="background:'+(r.jenisWarna||'#0d6efd')+'">'+esc(r.jenis)+'</span>':'')+'</td></tr>'; }
      h+='<tr><td class="ps-3">'+esc(r.nama)+(r.kode?' <span class="text-muted small">'+esc(r.kode)+'</span>':'')+'</td>'
        +'<td class="text-end">'+fmt2(r.qty)+'</td><td class="text-end">'+fmt(r.hargaBeli)+'</td><td class="text-end">'+fmt(r.nilaiBeli)+'</td>'
        +'<td class="text-end">'+fmt(r.avgCost)+'</td><td class="text-end">'+fmt(r.nilaiAvg)+'</td></tr>';
      sub.q+=r.qty; sub.nb+=r.nilaiBeli; sub.na+=r.nilaiAvg; g.q+=r.qty; g.nb+=r.nilaiBeli; g.na+=r.nilaiAvg;
    });
    subRow(curr);
    h+='<tr class="table-primary fw-bold"><td>TOTAL</td><td class="text-end">'+fmt2(g.q)+'</td><td></td><td class="text-end">'+fmt(g.nb)+'</td><td></td><td class="text-end">'+fmt(g.na)+'</td></tr>';
    b.innerHTML=h;

    // Fase 2: rollup per Gudang (hierarki pusat/cabang) -- TAMBAHAN, bukan pengganti tabel di atas.
    var gb=document.getElementById('lpgGudangBody<%=rp%>');
    if(!perGudang.length){ gb.innerHTML='<tr><td colspan="5" class="text-center text-muted py-4">Belum ada data stok.</td></tr>'; }
    else{
      var gh='';
      perGudang.forEach(function(pg){
        var tanpaGudang = pg.gudangId===''||pg.gudangId==null;
        gh+='<tr'+(tanpaGudang?' class="text-muted"':'')+'>'
          +'<td class="fw-bold">'+esc(pg.gudangNama)+'</td>'
          +'<td>'+(pg.indukNama?esc(pg.indukNama):'<span class="text-muted">—</span>')+'</td>'
          +'<td class="text-end">'+fmt(pg.jumlahLokasi)+'</td>'
          +'<td class="text-end">'+fmt(pg.jumlahProduk)+'</td>'
          +'<td class="text-end fw-bold">Rp '+fmt(pg.nilaiTotal)+'</td></tr>';
      });
      gb.innerHTML=gh;
    }
  }

  window.lpgLoad<%=rp%>=function(){
    var p=new URLSearchParams();
    p.append('jenis',document.getElementById('lpgJenis<%=rp%>').value); p.append('lokasi',document.getElementById('lpgLokasi<%=rp%>').value);
    p.append('cari',document.getElementById('lpgCari<%=rp%>').value);
    fetch(SVC+'&'+p.toString()).then(function(r){return r.json();}).then(function(j){
      rekap=(j&&j.rekap)||[]; tren=(j&&j.tren)||[]; jenis=(j&&j.jenis)||[]; lokasi=(j&&j.lokasi)||[]; perGudang=(j&&j.perGudang)||[];
      var jSel=document.getElementById('lpgJenis<%=rp%>'), keepJ=jSel.value; fill(jSel,jenis,'Semua jenis'); jSel.value=keepJ;
      var lSel=document.getElementById('lpgLokasi<%=rp%>'), keepL=lSel.value; fill(lSel,lokasi,'Semua lokasi'); lSel.value=keepL;
      if(PRESET && !presetDone){ presetDone=true; for(var i=0;i<jenis.length;i++){ if((jenis[i].nama||'').toLowerCase().indexOf(PRESET)>=0){ jSel.value=jenis[i].id; return window.lpgLoad<%=rp%>(); } } }
      render();
    });
  };
  window.lpgExcel<%=rp%>=function(){
    var rows='<tr><th>Lokasi</th><th>Jenis</th><th>Kode</th><th>Produk</th><th>Qty</th><th>Harga Beli</th><th>Nilai (Beli)</th><th>Rata2 Biaya</th><th>Nilai (Rata2)</th></tr>';
    rekap.forEach(function(r){ rows+='<tr><td>'+esc(r.lokasi)+'</td><td>'+esc(r.jenis)+'</td><td>'+esc(r.kode)+'</td><td>'+esc(r.nama)+'</td>'
      +'<td>'+r.qty+'</td><td>'+r.hargaBeli+'</td><td>'+r.nilaiBeli+'</td><td>'+Math.round(r.avgCost)+'</td><td>'+Math.round(r.nilaiAvg)+'</td></tr>'; });
    var html='<html xmlns:x="urn:schemas-microsoft-com:office:excel"><head><meta charset="UTF-8"></head><body><table border="1">'+rows+'</table></body></html>';
    var blob=new Blob(['﻿'+html],{type:'application/vnd.ms-excel'}); var a=document.createElement('a');
    a.href=URL.createObjectURL(blob); a.download='laporan_pergudangan_'+new Date().toISOString().slice(0,10)+'.xls'; a.click(); URL.revokeObjectURL(a.href);
  };
  window.lpgLoad<%=rp%>();
})();
</script>
