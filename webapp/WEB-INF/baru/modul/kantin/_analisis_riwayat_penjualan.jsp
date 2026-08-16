<%@page isELIgnored="true"%>
<%@page import="ais.common.Common"%>
<%
String rndAnalitik = Common.getGeneratedBarCode(7);
%>
<style>
  .sales-analysis-<%=rndAnalitik%> .metric-card{border:0;border-radius:16px;box-shadow:0 4px 18px rgba(15,23,42,.07);height:100%}
  .sales-analysis-<%=rndAnalitik%> .chart-card{border:0;border-radius:18px;box-shadow:0 4px 18px rgba(15,23,42,.07);height:100%}
  .sales-analysis-<%=rndAnalitik%> canvas{width:100%;height:260px;display:block}
  .sales-analysis-<%=rndAnalitik%> .analysis-list li{margin-bottom:12px}
  .sales-analysis-<%=rndAnalitik%> .bar-row{display:grid;grid-template-columns:minmax(120px,1.2fr) 3fr auto;gap:10px;align-items:center;margin:10px 0}
  .sales-analysis-<%=rndAnalitik%> .bar-track{height:12px;background:#e2e8f0;border-radius:999px;overflow:hidden}
  .sales-analysis-<%=rndAnalitik%> .bar-fill{height:100%;background:linear-gradient(90deg,#166534,#22c55e);border-radius:999px}
  .sales-analysis-<%=rndAnalitik%> .analysis-clickable{cursor:pointer;transition:transform .15s ease,box-shadow .15s ease}
  .sales-analysis-<%=rndAnalitik%> .analysis-clickable:hover{transform:translateY(-2px);box-shadow:0 8px 24px rgba(15,23,42,.13)}
  .sales-analysis-<%=rndAnalitik%> .analysis-clickable:focus{outline:3px solid rgba(22,101,52,.25);outline-offset:2px}
  @media print{
    @page{size:A4 landscape;margin:7mm}
    body *{visibility:hidden!important}.sales-analysis-<%=rndAnalitik%>,.sales-analysis-<%=rndAnalitik%> *{visibility:visible!important}
    .sales-analysis-<%=rndAnalitik%>{position:absolute;left:0;top:0;width:100%;background:#fff;zoom:.62}.analysis-actions-<%=rndAnalitik%>{display:none!important}
    .sales-analysis-<%=rndAnalitik%> .chart-card,.sales-analysis-<%=rndAnalitik%> .metric-card{box-shadow:none;border:1px solid #cbd5e1;break-inside:avoid}
  }
</style>

<section class="sales-analysis-<%=rndAnalitik%>">
  <div class="card border-0 shadow-sm rounded-4 mb-4">
    <div class="card-body p-4">
      <div class="d-flex flex-wrap justify-content-between align-items-center gap-3">
        <div>
          <h4 class="fw-bold mb-1"><i class="fas fa-chart-line text-success me-2"></i><%=Common.getBahasaConfig("Analisis Riwayat Penjualan")%></h4>
          <p class="text-muted mb-0"><%=Common.getBahasaConfig("Ringkasan keputusan dihitung dari seluruh transaksi pada periode yang dipilih, bukan hanya baris yang sedang terlihat.")%></p>
        </div>
        <div class="analysis-actions-<%=rndAnalitik%> d-flex flex-wrap gap-2 align-items-end">
          <div><label class="form-label small mb-1"><%=Common.getBahasaConfig("Dari tanggal")%></label><input id="saStart<%=rndAnalitik%>" type="date" class="form-control form-control-sm"></div>
          <div><label class="form-label small mb-1"><%=Common.getBahasaConfig("Sampai tanggal")%></label><input id="saEnd<%=rndAnalitik%>" type="date" class="form-control form-control-sm"></div>
          <button class="btn btn-success btn-sm" onclick="loadSalesAnalysis<%=rndAnalitik%>()"><i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Terapkan")%></button>
          <button class="btn btn-outline-danger btn-sm" onclick="window.print()"><i class="fas fa-file-pdf me-1"></i><%=Common.getBahasaConfig("Cetak PDF")%></button>
        </div>
      </div>
    </div>
  </div>

  <div id="saStatus<%=rndAnalitik%>" class="alert alert-light border"><%=Common.getBahasaConfig("Pilih periode lalu tekan Terapkan untuk memuat analisis.")%></div>
  <div id="saContent<%=rndAnalitik%>" class="d-none">
    <div id="saMetrics<%=rndAnalitik%>" class="row g-3 mb-4"></div>
    <div class="row g-4 mb-4">
      <div class="col-xl-8"><div class="card chart-card"><div class="card-body"><h5 class="fw-bold"><%=Common.getBahasaConfig("Tren Omzet Harian")%></h5><canvas id="saTrend<%=rndAnalitik%>" width="900" height="260"></canvas><div id="saTrendTable<%=rndAnalitik%>" class="table-responsive mt-3"></div></div></div></div>
      <div class="col-xl-4"><div class="card chart-card"><div class="card-body"><h5 class="fw-bold"><%=Common.getBahasaConfig("Kesehatan Penjualan")%></h5><canvas id="saRadar<%=rndAnalitik%>" width="420" height="260"></canvas><p class="small text-muted mb-0"><%=Common.getBahasaConfig("Nilai makin mendekati tepi berarti kondisi makin baik. Skor merupakan indikator, bukan pengganti pemeriksaan supervisor.")%></p></div></div></div>
    </div>
    <div class="row g-4 mb-4">
      <div class="col-lg-6"><div class="card chart-card"><div class="card-body"><h5 class="fw-bold"><%=Common.getBahasaConfig("Produk Penyumbang Omzet")%></h5><div id="saProducts<%=rndAnalitik%>"></div></div></div></div>
      <div class="col-lg-6"><div class="card chart-card"><div class="card-body"><h5 class="fw-bold"><%=Common.getBahasaConfig("Komposisi Metode Pembayaran")%></h5><canvas id="saPayments<%=rndAnalitik%>" width="520" height="260"></canvas><div id="saPaymentLegend<%=rndAnalitik%>" class="small mt-2"></div></div></div></div>
    </div>
    <div class="row g-4 mb-4">
      <div class="col-lg-6"><div class="card chart-card"><div class="card-body"><h5 class="fw-bold"><%=Common.getBahasaConfig("Jam Ramai")%></h5><div id="saHours<%=rndAnalitik%>"></div></div></div></div>
      <div class="col-lg-6"><div class="card chart-card"><div class="card-body"><h5 class="fw-bold"><%=Common.getBahasaConfig("Kinerja Kasir")%></h5><div id="saCashiers<%=rndAnalitik%>"></div></div></div></div>
    </div>
    <div class="row g-4 mb-4">
      <div class="col-lg-6"><div class="card chart-card"><div class="card-body"><h5 class="fw-bold"><%=Common.getBahasaConfig("Pola Hari dalam Minggu")%></h5><div id="saDays<%=rndAnalitik%>"></div></div></div></div>
      <div class="col-lg-6"><div class="card chart-card"><div class="card-body"><h5 class="fw-bold"><%=Common.getBahasaConfig("Distribusi Nilai Keranjang")%></h5><div id="saBaskets<%=rndAnalitik%>"></div></div></div></div>
    </div>
    <div class="row g-4 mb-4">
      <div class="col-lg-6"><div class="card chart-card"><div class="card-body"><h5 class="fw-bold"><%=Common.getBahasaConfig("Candlestick Perubahan Omzet")%></h5><canvas id="saCandle<%=rndAnalitik%>" width="520" height="260"></canvas><p class="small text-muted mb-0">Buka memakai omzet periode sebelumnya; tutup memakai omzet periode berjalan.</p></div></div></div>
      <div class="col-lg-6"><div class="card chart-card"><div class="card-body"><h5 class="fw-bold"><%=Common.getBahasaConfig("Heatmap Aktivitas Hari dan Jam")%></h5><div id="saHeatmap<%=rndAnalitik%>" class="d-grid gap-1" style="grid-template-columns:repeat(6,minmax(54px,1fr))"></div></div></div></div>
    </div>
    <div class="card chart-card mb-4"><div class="card-body">
      <h5 class="fw-bold"><i class="fas fa-coins text-success me-2"></i><%=Common.getBahasaConfig("Analisis Laba Kotor")%></h5>
      <div id="saGrossMetrics<%=rndAnalitik%>" class="row g-3 mb-4"></div>
      <div class="row g-4">
        <div class="col-xl-6"><div class="border rounded-3 p-3 analysis-clickable" tabindex="0" id="saGrossTrendCard<%=rndAnalitik%>"><h6 class="fw-bold">Tren Penjualan, HPP &amp; Laba Kotor</h6><canvas id="saGrossTrend<%=rndAnalitik%>" width="600" height="260"></canvas></div></div>
        <div class="col-xl-6"><div class="border rounded-3 p-3 analysis-clickable" tabindex="0" id="saGrossCandleCard<%=rndAnalitik%>"><h6 class="fw-bold">Candlestick Laba Transaksi Harian</h6><canvas id="saGrossCandle<%=rndAnalitik%>" width="600" height="260"></canvas></div></div>
        <div class="col-12"><div class="border rounded-3 p-3 analysis-clickable" tabindex="0" id="saGrossProductsCard<%=rndAnalitik%>"><h6 class="fw-bold">Produk Penyumbang Laba Kotor</h6><div id="saGrossProducts<%=rndAnalitik%>"></div></div></div>
      </div>
    </div></div>
    <div class="card chart-card mb-4"><div class="card-body"><h5 class="fw-bold"><i class="fas fa-shield-alt text-danger me-2"></i><%=Common.getBahasaConfig("Rekap Risiko, Promo, dan Retur")%></h5><div id="saRisks<%=rndAnalitik%>" class="table-responsive"></div></div></div>
    <div class="card chart-card mb-4"><div class="card-body"><h5 class="fw-bold"><i class="fas fa-lightbulb text-warning me-2"></i><%=Common.getBahasaConfig("Analisis Cerdas dan Tindakan yang Disarankan")%></h5><p class="text-muted"><%=Common.getBahasaConfig("Saran berikut dibuat memakai aturan yang transparan dari angka periode terpilih. Periksa kondisi lapangan sebelum menjalankan keputusan.")%></p><ol id="saInsights<%=rndAnalitik%>" class="analysis-list"></ol></div></div>
  </div>
</section>

<div class="modal fade" id="saDetailModal<%=rndAnalitik%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-xl modal-dialog-scrollable"><div class="modal-content">
    <div class="modal-header"><h5 id="saDetailTitle<%=rndAnalitik%>" class="modal-title fw-bold">Detail Analisis</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
    <div class="modal-body"><div id="saDetailTable<%=rndAnalitik%>" class="table-responsive"></div></div>
    <div class="modal-footer"><button type="button" class="btn btn-outline-success" id="saExportExcel<%=rndAnalitik%>"><i class="fas fa-file-excel me-1"></i>Download Excel</button><button type="button" class="btn btn-outline-danger" id="saExportPdf<%=rndAnalitik%>"><i class="fas fa-file-pdf me-1"></i>Download PDF</button><button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Tutup</button></div>
  </div></div>
</div>

<script>
(() => {
  const id = '<%=rndAnalitik%>', root = '<%=Common.ROOT%>';
  const $ = suffix => document.getElementById(suffix + id);
  const rp = n => new Intl.NumberFormat('id-ID',{style:'currency',currency:'IDR',maximumFractionDigits:0}).format(Number(n||0));
  const num = n => Number(n||0), esc = s => String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const iso = d => d.toISOString().slice(0,10);
  const end = new Date(), start = new Date(); start.setDate(end.getDate()-29); $('saStart').value=iso(start); $('saEnd').value=iso(end);
  let detailTitle='',detailHeaders=[],detailRows=[];
  function safeFile(s){return String(s||'analisis').replace(/[^a-z0-9_-]+/gi,'-').replace(/^-|-$/g,'').toLowerCase()||'analisis';}
  function detailTable(){return '<table class="table table-striped table-hover align-middle"><thead><tr>'+detailHeaders.map(h=>'<th>'+esc(h)+'</th>').join('')+'</tr></thead><tbody>'+detailRows.map(r=>'<tr>'+detailHeaders.map((h,i)=>'<td>'+esc(r[i]??'')+'</td>').join('')+'</tr>').join('')+'</tbody></table>';}
  function showDetail(title,headers,rows){detailTitle=title;detailHeaders=headers;detailRows=rows;$('saDetailTitle').textContent=title;$('saDetailTable').innerHTML=rows.length?detailTable():'<div class="alert alert-light border">Belum ada data pada periode ini.</div>';const el=$('saDetailModal');if(window.bootstrap&&bootstrap.Modal){bootstrap.Modal.getOrCreateInstance(el).show()}else{el.classList.add('show');el.style.display='block';el.removeAttribute('aria-hidden');}}
  function wire(node,title,headers,rows){const el=$(node);if(!el)return;el.classList.add('analysis-clickable');el.setAttribute('tabindex','0');const open=()=>showDetail(title,headers,rows);el.onclick=open;el.onkeydown=e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();open();}};}
  $('saExportExcel').onclick=()=>{const html='<html><head><meta charset="UTF-8"></head><body><h2>'+esc(detailTitle)+'</h2>'+detailTable()+'</body></html>',a=document.createElement('a');a.href=URL.createObjectURL(new Blob([html],{type:'application/vnd.ms-excel;charset=utf-8'}));a.download=safeFile(detailTitle)+'.xls';a.click();setTimeout(()=>URL.revokeObjectURL(a.href),1000);};
  $('saExportPdf').onclick=()=>{const w=window.open('','_blank');if(!w)return;w.document.write('<html><head><title>'+esc(detailTitle)+'</title><style>@page{size:A4 landscape;margin:10mm}body{font:11px Arial}table{width:100%;border-collapse:collapse}th,td{border:1px solid #bbb;padding:5px;text-align:left}th{background:#eef2f7}</style></head><body><h2>'+esc(detailTitle)+'</h2>'+detailTable()+'</body></html>');w.document.close();w.focus();setTimeout(()=>w.print(),250);};

  function bars(target, rows, label, value, formatter){
    const max=Math.max(1,...rows.map(value));
    $(target).innerHTML=rows.length?rows.map(r=>`<div class="bar-row"><span class="text-truncate" title="${esc(label(r))}">${esc(label(r))}</span><div class="bar-track"><div class="bar-fill" style="width:${Math.max(2,value(r)*100/max)}%"></div></div><strong>${formatter(value(r))}</strong></div>`).join(''):'<p class="text-muted">Belum ada data.</p>';
  }
  function line(canvas, rows){
    const c=$(canvas),x=c.getContext('2d'),w=c.width,h=c.height,p=38;x.clearRect(0,0,w,h);x.strokeStyle='#cbd5e1';x.beginPath();x.moveTo(p,10);x.lineTo(p,h-p);x.lineTo(w-10,h-p);x.stroke();
    if(!rows.length)return;const vals=rows.map(r=>num(r.omzet)),mx=Math.max(1,...vals);x.strokeStyle='#15803d';x.lineWidth=3;x.beginPath();rows.forEach((r,i)=>{const px=p+i*(w-p-15)/Math.max(1,rows.length-1),py=h-p-(num(r.omzet)/mx)*(h-p-20);i?x.lineTo(px,py):x.moveTo(px,py)});x.stroke();x.fillStyle='#15803d';rows.forEach((r,i)=>{const px=p+i*(w-p-15)/Math.max(1,rows.length-1),py=h-p-(num(r.omzet)/mx)*(h-p-20);x.beginPath();x.arc(px,py,3,0,Math.PI*2);x.fill()});
  }
  function donut(canvas, rows){
    const c=$(canvas),x=c.getContext('2d'),cx=c.width/2,cy=c.height/2,r=90,colors=['#166534','#22c55e','#0ea5e9','#f59e0b','#8b5cf6','#ef4444','#64748b'];x.clearRect(0,0,c.width,c.height);const total=rows.reduce((a,b)=>a+num(b.omzet),0)||1;let a=-Math.PI/2;rows.forEach((v,i)=>{const q=num(v.omzet)/total*Math.PI*2;x.beginPath();x.moveTo(cx,cy);x.arc(cx,cy,r,a,a+q);x.closePath();x.fillStyle=colors[i%colors.length];x.fill();a+=q});x.beginPath();x.arc(cx,cy,50,0,Math.PI*2);x.fillStyle='#fff';x.fill();x.fillStyle='#0f172a';x.font='bold 15px sans-serif';x.textAlign='center';x.fillText(rp(total),cx,cy+5);$('saPaymentLegend').innerHTML=rows.map((r,i)=>`<span class="me-3"><b style="color:${colors[i%colors.length]}">●</b> ${esc(r.nama)} ${Math.round(num(r.omzet)*100/total)}%</span>`).join('');
  }
  function radar(canvas, vals){
    const c=$(canvas),x=c.getContext('2d'),cx=c.width/2,cy=c.height/2,r=95,n=vals.length;x.clearRect(0,0,c.width,c.height);x.strokeStyle='#cbd5e1';for(let ring=1;ring<=4;ring++){x.beginPath();vals.forEach((v,i)=>{const a=-Math.PI/2+i*2*Math.PI/n,rr=r*ring/4,px=cx+Math.cos(a)*rr,py=cy+Math.sin(a)*rr;i?x.lineTo(px,py):x.moveTo(px,py)});x.closePath();x.stroke()}x.beginPath();vals.forEach((v,i)=>{const a=-Math.PI/2+i*2*Math.PI/n,rr=r*Math.max(0,Math.min(100,v.score))/100,px=cx+Math.cos(a)*rr,py=cy+Math.sin(a)*rr;i?x.lineTo(px,py):x.moveTo(px,py)});x.closePath();x.fillStyle='rgba(34,197,94,.25)';x.fill();x.strokeStyle='#15803d';x.lineWidth=2;x.stroke();x.fillStyle='#334155';x.font='12px sans-serif';x.textAlign='center';vals.forEach((v,i)=>{const a=-Math.PI/2+i*2*Math.PI/n;x.fillText(v.name,cx+Math.cos(a)*(r+25),cy+Math.sin(a)*(r+25)+4)})
  }
  function candle(canvas,rows){const c=$(canvas),x=c.getContext('2d'),w=c.width,h=c.height,p=32;x.clearRect(0,0,w,h);if(!rows.length)return;const vals=rows.map(r=>num(r.omzet)),mx=Math.max(1,...vals),bw=(w-p-10)/rows.length;rows.forEach((r,i)=>{const close=num(r.omzet),open=i?num(rows[i-1].omzet):close,high=Math.max(open,close),low=Math.min(open,close),px=p+bw*(i+.5),yy=v=>h-p-v/mx*(h-p-15),col=close>=open?'#16a34a':'#dc2626';x.strokeStyle=col;x.lineWidth=2;x.beginPath();x.moveTo(px,yy(high));x.lineTo(px,yy(low));x.stroke();x.fillStyle=col;x.fillRect(px-Math.min(8,bw*.28),Math.min(yy(open),yy(close)),Math.min(16,bw*.56),Math.max(3,Math.abs(yy(open)-yy(close))))});}
  function grossTrend(canvas,rows){const c=$(canvas),x=c.getContext('2d'),w=c.width,h=c.height,p=35,series=[['omzet','#166534'],['hpp','#f59e0b'],['labaKotor','#16a34a']];x.clearRect(0,0,w,h);const vals=rows.flatMap(r=>series.map(s=>num(r[s[0]]))),mx=Math.max(1,...vals),mn=Math.min(0,...vals),range=Math.max(1,mx-mn);x.strokeStyle='#e2e8f0';for(let i=0;i<=4;i++){x.beginPath();x.moveTo(p,i*(h-p)/4);x.lineTo(w-5,i*(h-p)/4);x.stroke()}series.forEach(s=>{x.strokeStyle=s[1];x.lineWidth=2.3;x.beginPath();rows.forEach((r,i)=>{const px=p+i*(w-p-10)/Math.max(1,rows.length-1),py=h-p-(num(r[s[0]])-mn)/range*(h-p-10);i?x.lineTo(px,py):x.moveTo(px,py)});x.stroke()});}
  function grossCandle(canvas,rows){const c=$(canvas),x=c.getContext('2d'),w=c.width,h=c.height,p=30;x.clearRect(0,0,w,h);if(!rows.length)return;const hi=Math.max(0,...rows.map(r=>num(r.high))),lo=Math.min(0,...rows.map(r=>num(r.low))),range=Math.max(1,hi-lo),bw=(w-p-8)/rows.length,yy=v=>h-p-(v-lo)/range*(h-p-8);if(lo<0&&hi>0){x.strokeStyle='#94a3b8';x.beginPath();x.moveTo(p,yy(0));x.lineTo(w,yy(0));x.stroke()}rows.forEach((r,i)=>{const o=num(r.open),cl=num(r.close),col=cl>=o?'#16a34a':'#dc2626',px=p+bw*(i+.5);x.strokeStyle=col;x.beginPath();x.moveTo(px,yy(num(r.high)));x.lineTo(px,yy(num(r.low)));x.stroke();x.fillStyle=col;x.fillRect(px-Math.min(8,bw*.25),Math.min(yy(o),yy(cl)),Math.min(16,bw*.5),Math.max(2,Math.abs(yy(o)-yy(cl))))});}
  function heatmap(rows){const max=Math.max(1,...rows.map(r=>num(r.transaksi)));$('saHeatmap').innerHTML=rows.map(r=>{const p=num(r.transaksi)/max;return `<div title="${esc(r.nama)}: ${num(r.transaksi)} transaksi" style="padding:10px 5px;border-radius:7px;text-align:center;font-size:11px;background:rgba(22,101,52,${.08+.85*p});color:${p>.55?'#fff':'#334155'}">${esc(r.nama)}<br><b>${num(r.transaksi)}</b></div>`}).join('')||'<p class="text-muted">Belum ada data.</p>';}
  window['loadSalesAnalysis'+id]=async function(){
    $('saStatus').className='alert alert-info';$('saStatus').textContent='<%=Common.getBahasaConfigJS("Sedang menghitung seluruh transaksi pada periode terpilih...")%>';
    try{
      const response=await fetch(root+'/Api_eBisnis',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({action:'laporan_riwayat_penjualan_analitik',tglMulai:$('saStart').value,tglSampai:$('saEnd').value})});
      const d=await response.json();if(d.status!=='success')throw new Error(d.message||'Data belum dapat dimuat.');
      const k=d.kpi||{},p=d.pembanding||{},growth=p.omzet?((num(k.omzet)-num(p.omzet))/num(p.omzet)*100):(k.omzet?100:0),member=k.transaksi?num(k.transaksiMember)*100/num(k.transaksi):0;
      const retur=d.retur||{},metrics=[['Transaksi',num(k.transaksi).toLocaleString('id-ID'),'Jumlah nota'],['Omzet',rp(k.omzet),'Pendapatan periode'],['Rata-rata',rp(k.rataRata),'Nilai per nota'],['Barang terjual',num(k.qty).toLocaleString('id-ID'),'Total kuantitas'],['Pertumbuhan',(growth>=0?'+':'')+growth.toFixed(1)+'%','Dibanding periode sebelumnya'],['Tidak valid',num(k.tidakValid).toLocaleString('id-ID'),'Master berbeda dengan detail'],['Nilai retur',rp(retur.nilai),num(retur.transaksi)+' transaksi'],['Biaya promo',rp(num(k.diskon)+num(k.cashback)),'Diskon + cashback'],['Eksposur selisih',rp(k.nilaiTidakValid),'Perlu rekonsiliasi']];
      $('saMetrics').innerHTML=metrics.map((m,i)=>`<div class="col-12 col-sm-6 col-lg-4 col-xl-3"><div id="saMetric${i}${id}" class="card metric-card analysis-clickable" tabindex="0"><div class="card-body"><div class="small text-muted">${m[0]}</div><div class="fs-5 fw-bold">${m[1]}</div><div class="small text-muted">${m[2]}</div></div></div></div>`).join('');
      metrics.forEach((m,i)=>wire('saMetric'+i,m[0],['Ukuran','Nilai','Keterangan'],[[m[0],m[1],m[2]]]));
      line('saTrend',d.tren||[]);candle('saCandle',d.tren||[]);heatmap(d.hari||[]);$('saTrendTable').innerHTML='<table class="table table-sm"><thead><tr><th>Tanggal</th><th>Transaksi</th><th class="text-end">Omzet</th></tr></thead><tbody>'+ (d.tren||[]).map(r=>`<tr><td>${esc(r.tanggal)}</td><td>${r.transaksi}</td><td class="text-end">${rp(r.omzet)}</td></tr>`).join('')+'</tbody></table>';
      bars('saProducts',d.produk||[],r=>r.nama,r=>num(r.omzet),rp);bars('saHours',(d.jam||[]).sort((a,b)=>num(b.transaksi)-num(a.transaksi)),r=>String(r.jam).padStart(2,'0')+':00',r=>num(r.transaksi),n=>n+' trx');bars('saCashiers',d.kasir||[],r=>r.nama,r=>num(r.omzet),rp);bars('saDays',d.hari||[],r=>r.nama,r=>num(r.omzet),rp);bars('saBaskets',d.keranjang||[],r=>r.rentang,r=>num(r.transaksi),n=>n+' trx');donut('saPayments',d.metode||[]);
      const returnRate=k.omzet?num(retur.nilai)*100/num(k.omzet):0,promoBase=num(k.omzet)+num(k.diskon),promoRate=promoBase?(num(k.diskon)+num(k.cashback))*100/promoBase:0,validRate=k.transaksi?(num(k.transaksi)-num(k.tidakValid))*100/num(k.transaksi):100;
      $('saRisks').innerHTML=`<table class="table table-sm align-middle mb-0"><thead><tr><th>Indikator</th><th class="text-end">Nilai</th><th class="text-end">Rasio / Catatan</th></tr></thead><tbody><tr><td>Validitas transaksi</td><td class="text-end">${num(k.transaksi)-num(k.tidakValid)} / ${num(k.transaksi)}</td><td class="text-end">${validRate.toFixed(1)}% valid</td></tr><tr><td>Eksposur selisih data</td><td class="text-end">${rp(k.nilaiTidakValid)}</td><td class="text-end">rekonsiliasi supervisor</td></tr><tr><td>Diskon</td><td class="text-end">${rp(k.diskon)}</td><td class="text-end">biaya promosi</td></tr><tr><td>Cashback</td><td class="text-end">${rp(k.cashback)}</td><td class="text-end">kewajiban/member benefit</td></tr><tr><td>Retur penjualan</td><td class="text-end">${rp(retur.nilai)}</td><td class="text-end">${returnRate.toFixed(2)}% omzet</td></tr></tbody></table>`;
      const paymentTop=(d.metode||[])[0],peak=[...(d.jam||[])].sort((a,b)=>num(b.transaksi)-num(a.transaksi))[0],productTop=(d.produk||[])[0],topShare=k.omzet&&productTop?num(productTop.omzet)*100/num(k.omzet):0,payShare=k.omzet&&paymentTop?num(paymentTop.omzet)*100/num(k.omzet):0;
      radar('saRadar',[{name:'Pertumbuhan',score:Math.max(0,Math.min(100,50+growth))},{name:'Validitas',score:k.transaksi?100-num(k.tidakValid)*100/num(k.transaksi):100},{name:'Member',score:member},{name:'Sebaran Produk',score:100-topShare},{name:'Sebaran Bayar',score:100-payShare}]);
      const insights=[];
      if(num(k.tidakValid)>0)insights.push(`<b>${k.tidakValid} transaksi perlu diperiksa.</b> Total master dan rincian berbeda. Buka Riwayat Penjualan, aktifkan filter Transaksi tidak valid, lalu cocokkan struk dan detail sebelum membuat koreksi.`);else insights.push('<b>Integritas transaksi baik.</b> Tidak ditemukan selisih total master dan detail pada periode ini. Pertahankan pemeriksaan harian.');
      insights.push(growth<0?`<b>Omzet turun ${Math.abs(growth).toFixed(1)}%.</b> Bandingkan produk yang turun, ketersediaan stok, jadwal petugas, dan promo terhadap periode sebelumnya sebelum menambah pembelian.`:`<b>Omzet tumbuh ${growth.toFixed(1)}%.</b> Pastikan stok produk utama dan kapasitas petugas cukup agar pertumbuhan tidak menimbulkan kehabisan stok atau antrean.`);
      if(peak)insights.push(`<b>Jam teramai sekitar ${String(peak.jam).padStart(2,'0')}:00.</b> Jadwalkan petugas, persiapan uang kecil, perangkat, dan pengisian rak sebelum jam tersebut.`);
      if(paymentTop)insights.push(`<b>${esc(paymentTop.nama)} menyumbang sekitar ${payShare.toFixed(0)}% omzet.</b> Bila proporsinya terlalu dominan, siapkan prosedur cadangan ketika kanal pembayaran itu terganggu.`);
      if(productTop)insights.push(`<b>${esc(productTop.nama)} adalah penyumbang terbesar (${topShare.toFixed(0)}%).</b> Jaga stok minimumnya dan hindari ketergantungan berlebih dengan mengembangkan produk pendamping.`);
      if(member<30)insights.push(`<b>Identifikasi member baru ${member.toFixed(0)}%.</b> Dorong kasir menanyakan nomor telepon secara sopan dan gunakan nomor yang sudah ada agar tidak membuat member ganda.`);
      const topFive=(d.produk||[]).slice(0,5).reduce((a,r)=>a+num(r.omzet),0),topFiveShare=k.omzet?topFive*100/num(k.omzet):0;if(topFiveShare>=70)insights.push(`<b>Konsentrasi produk tinggi: lima produk menyumbang ${topFiveShare.toFixed(1)}% omzet.</b> Tetapkan stok pengaman dan siapkan produk pengganti agar kekosongan satu produk tidak langsung menekan penjualan.`);
      if(num(retur.nilai)>0)insights.push(`<b>Retur ${rp(retur.nilai)} (${returnRate.toFixed(2)}% omzet).</b> Kelompokkan alasan retur dan periksa produk yang berulang sebelum memutuskan tindakan ke pemasok, rak, atau kasir.`);
      if(num(k.diskon)+num(k.cashback)>0)insights.push(`<b>Biaya promo ${rp(num(k.diskon)+num(k.cashback))} (${promoRate.toFixed(1)}% nilai sebelum diskon).</b> Pertahankan promo hanya bila kenaikan transaksi, keranjang, atau retensi menutup biayanya.`);
      const days=[...(d.hari||[])].sort((a,b)=>num(b.omzet)-num(a.omzet));if(days.length>1)insights.push(`<b>Hari terkuat ${esc(days[0].nama)}, terlemah ${esc(days[days.length-1].nama)}.</b> Sesuaikan jadwal petugas dan stok dengan hari kuat; uji bundling relevan pada hari lemah.`);
      $('saInsights').innerHTML=insights.map(x=>'<li>'+x+'</li>').join('');$('saStatus').className='alert alert-success';$('saStatus').textContent=`Analisis ${d.tglMulai} sampai ${d.tglSampai} berhasil dimuat. Semua grafik dan tabel dapat dicetak melalui tombol Cetak PDF.`;$('saContent').classList.remove('d-none');
      const laba=d.labaKotor||{},lr=laba.ringkasan||{},lp=laba.produk||[],lt=laba.tren||[],lc=laba.candle||[],lr0=d.labaKotorPembanding||{};
      const grossMetrics=[['Laba kotor',rp(lr.labaKotor),'Penjualan dikurangi HPP'],['HPP',rp(lr.hpp),'Harga pokok penjualan'],['Margin',num(lr.marginPersen).toFixed(2)+'%','Margin laba kotor'],['Produk margin negatif',num(lr.produkMarginNegatif).toLocaleString('id-ID'),'Perlu evaluasi harga/HPP'],['Produk tanpa HPP',num(lr.produkTanpaHpp).toLocaleString('id-ID'),'Perlu melengkapi HPP'],['Qty tanpa HPP',num(lr.qtyTanpaHpp).toLocaleString('id-ID'),'Belum tercakup perhitungan']];
      $('saGrossMetrics').innerHTML=grossMetrics.map((m,i)=>`<div class="col-12 col-sm-6 col-xl-4"><div id="saGrossMetric${i}${id}" class="card metric-card analysis-clickable" tabindex="0"><div class="card-body"><div class="small text-muted">${m[0]}</div><div class="fs-5 fw-bold">${m[1]}</div><div class="small text-muted">${m[2]}</div></div></div></div>`).join('');
      const grossSummary=[['Penjualan',rp(lr.omzet),rp(lr0.omzet)],['HPP',rp(lr.hpp),rp(lr0.hpp)],['Laba kotor',rp(lr.labaKotor),rp(lr0.labaKotor)],['Margin',num(lr.marginPersen).toFixed(2)+'%',num(lr0.marginPersen).toFixed(2)+'%'],['Produk margin negatif',num(lr.produkMarginNegatif),num(lr0.produkMarginNegatif)],['Produk tanpa HPP',num(lr.produkTanpaHpp),num(lr0.produkTanpaHpp)]];
      grossMetrics.forEach((m,i)=>wire('saGrossMetric'+i,'Ringkasan Laba Kotor',['Ukuran','Periode Ini','Periode Sebelumnya'],grossSummary));
      grossTrend('saGrossTrend',lt);grossCandle('saGrossCandle',lc);bars('saGrossProducts',lp,r=>r.nama,r=>num(r.labaKotor),rp);
      const trendRows=(d.tren||[]).map(r=>[r.tanggal,r.transaksi,rp(r.omzet),rp(r.rataRata)]),productRows=(d.produk||[]).map(r=>[r.nama,r.qty,r.transaksi,rp(r.omzet)]),paymentRows=(d.metode||[]).map(r=>[r.nama,r.transaksi,rp(r.omzet)]),hourRows=(d.jam||[]).map(r=>[String(r.jam).padStart(2,'0')+':00',r.transaksi,rp(r.omzet)]),cashierRows=(d.kasir||[]).map(r=>[r.nama,r.transaksi,rp(r.omzet),rp(r.rataRata)]),dayRows=(d.hari||[]).map(r=>[r.nama,r.transaksi,rp(r.omzet),rp(r.rataRata)]),basketRows=(d.keranjang||[]).map(r=>[r.rentang,r.transaksi,rp(r.omzet)]),grossTrendRows=lt.map(r=>[r.tanggal,rp(r.omzet),rp(r.hpp),rp(r.labaKotor),num(r.marginPersen).toFixed(2)+'%']),grossCandleRows=lc.map(r=>[r.tanggal,rp(r.open),rp(r.high),rp(r.low),rp(r.close),r.transaksi]),grossProductRows=lp.map(r=>[r.nama,r.qty,rp(r.omzet),rp(r.hpp),rp(r.labaKotor),num(r.marginPersen).toFixed(2)+'%']);
      wire('saTrend','Tren Omzet Harian',['Tanggal','Transaksi','Omzet','Rata-rata'],trendRows);wire('saRadar','Kesehatan Penjualan',['Dimensi','Skor'],[{name:'Pertumbuhan',score:Math.max(0,Math.min(100,50+growth))},{name:'Validitas',score:validRate},{name:'Member',score:member},{name:'Sebaran Produk',score:100-topShare},{name:'Sebaran Bayar',score:100-payShare}].map(r=>[r.name,r.score.toFixed(1)]));wire('saProducts','Produk Penyumbang Omzet',['Produk','Qty','Transaksi','Omzet'],productRows);wire('saPayments','Komposisi Metode Pembayaran',['Metode','Transaksi','Omzet'],paymentRows);wire('saHours','Jam Ramai',['Jam','Transaksi','Omzet'],hourRows);wire('saCashiers','Kinerja Kasir',['Kasir','Transaksi','Omzet','Rata-rata'],cashierRows);wire('saDays','Pola Hari dalam Minggu',['Hari','Transaksi','Omzet','Rata-rata'],dayRows);wire('saBaskets','Distribusi Nilai Keranjang',['Rentang','Transaksi','Omzet'],basketRows);wire('saCandle','Candlestick Perubahan Omzet',['Tanggal','Transaksi','Omzet','Rata-rata'],trendRows);wire('saHeatmap','Heatmap Aktivitas Hari dan Jam',['Hari','Transaksi','Omzet','Rata-rata'],dayRows);wire('saRisks','Rekap Risiko, Promo, dan Retur',['Indikator','Nilai'],[['Transaksi tidak valid',k.tidakValid],['Eksposur selisih',rp(k.nilaiTidakValid)],['Diskon',rp(k.diskon)],['Cashback',rp(k.cashback)],['Retur',rp(retur.nilai)]]);wire('saInsights','Analisis Cerdas dan Tindakan',['No','Rekomendasi'],insights.map((x,i)=>[i+1,x.replace(/<[^>]*>/g,'')]));wire('saGrossTrendCard','Tren Penjualan, HPP & Laba Kotor',['Tanggal','Penjualan','HPP','Laba','Margin'],grossTrendRows);wire('saGrossCandleCard','Candlestick Laba Transaksi Harian',['Tanggal','Open','High','Low','Close','Transaksi'],grossCandleRows);wire('saGrossProductsCard','Produk Penyumbang Laba Kotor',['Produk','Qty','Penjualan','HPP','Laba','Margin'],grossProductRows);
    }catch(e){$('saStatus').className='alert alert-danger';$('saStatus').textContent='<%=Common.getBahasaConfigJS("Analisis belum dapat dimuat. Muat ulang halaman, periksa akses toko, lalu coba kembali.")%> '+e.message;}
  };
  // Muat satu kali sesudah partial terpasang. Konten tetap ringan karena hanya
  // agregasi server-side; pengguna dapat mengganti periode tanpa memuat halaman.
  setTimeout(()=>window['loadSalesAnalysis'+id](),120);
})();
</script>
