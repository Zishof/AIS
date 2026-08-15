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
  @media print{
    body *{visibility:hidden!important}.sales-analysis-<%=rndAnalitik%>,.sales-analysis-<%=rndAnalitik%> *{visibility:visible!important}
    .sales-analysis-<%=rndAnalitik%>{position:absolute;left:0;top:0;width:100%;background:#fff}.analysis-actions-<%=rndAnalitik%>{display:none!important}
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
    <div class="card chart-card mb-4"><div class="card-body"><h5 class="fw-bold"><i class="fas fa-lightbulb text-warning me-2"></i><%=Common.getBahasaConfig("Analisis Cerdas dan Tindakan yang Disarankan")%></h5><p class="text-muted"><%=Common.getBahasaConfig("Saran berikut dibuat memakai aturan yang transparan dari angka periode terpilih. Periksa kondisi lapangan sebelum menjalankan keputusan.")%></p><ol id="saInsights<%=rndAnalitik%>" class="analysis-list"></ol></div></div>
  </div>
</section>

<script>
(() => {
  const id = '<%=rndAnalitik%>', root = '<%=Common.ROOT%>';
  const $ = suffix => document.getElementById(suffix + id);
  const rp = n => new Intl.NumberFormat('id-ID',{style:'currency',currency:'IDR',maximumFractionDigits:0}).format(Number(n||0));
  const num = n => Number(n||0), esc = s => String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const iso = d => d.toISOString().slice(0,10);
  const end = new Date(), start = new Date(); start.setDate(end.getDate()-29); $('saStart').value=iso(start); $('saEnd').value=iso(end);

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
  window['loadSalesAnalysis'+id]=async function(){
    $('saStatus').className='alert alert-info';$('saStatus').textContent='<%=Common.getBahasaConfigJS("Sedang menghitung seluruh transaksi pada periode terpilih...")%>';
    try{
      const response=await fetch(root+'/Api_eBisnis',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({action:'laporan_riwayat_penjualan_analitik',tglMulai:$('saStart').value,tglSampai:$('saEnd').value})});
      const d=await response.json();if(d.status!=='success')throw new Error(d.message||'Data belum dapat dimuat.');
      const k=d.kpi||{},p=d.pembanding||{},growth=p.omzet?((num(k.omzet)-num(p.omzet))/num(p.omzet)*100):(k.omzet?100:0),member=k.transaksi?num(k.transaksiMember)*100/num(k.transaksi):0;
      const metrics=[['Transaksi',num(k.transaksi).toLocaleString('id-ID'),'Jumlah nota'],['Omzet',rp(k.omzet),'Pendapatan periode'],['Rata-rata',rp(k.rataRata),'Nilai per nota'],['Barang terjual',num(k.qty).toLocaleString('id-ID'),'Total kuantitas'],['Pertumbuhan',(growth>=0?'+':'')+growth.toFixed(1)+'%','Dibanding periode sebelumnya'],['Tidak valid',num(k.tidakValid).toLocaleString('id-ID'),'Master berbeda dengan detail']];
      $('saMetrics').innerHTML=metrics.map(m=>`<div class="col-6 col-lg-2"><div class="card metric-card"><div class="card-body"><div class="small text-muted">${m[0]}</div><div class="fs-5 fw-bold">${m[1]}</div><div class="small text-muted">${m[2]}</div></div></div></div>`).join('');
      line('saTrend',d.tren||[]);$('saTrendTable').innerHTML='<table class="table table-sm"><thead><tr><th>Tanggal</th><th>Transaksi</th><th class="text-end">Omzet</th></tr></thead><tbody>'+ (d.tren||[]).map(r=>`<tr><td>${esc(r.tanggal)}</td><td>${r.transaksi}</td><td class="text-end">${rp(r.omzet)}</td></tr>`).join('')+'</tbody></table>';
      bars('saProducts',d.produk||[],r=>r.nama,r=>num(r.omzet),rp);bars('saHours',(d.jam||[]).sort((a,b)=>num(b.transaksi)-num(a.transaksi)),r=>String(r.jam).padStart(2,'0')+':00',r=>num(r.transaksi),n=>n+' trx');bars('saCashiers',d.kasir||[],r=>r.nama,r=>num(r.omzet),rp);donut('saPayments',d.metode||[]);
      const paymentTop=(d.metode||[])[0],peak=[...(d.jam||[])].sort((a,b)=>num(b.transaksi)-num(a.transaksi))[0],productTop=(d.produk||[])[0],topShare=k.omzet&&productTop?num(productTop.omzet)*100/num(k.omzet):0,payShare=k.omzet&&paymentTop?num(paymentTop.omzet)*100/num(k.omzet):0;
      radar('saRadar',[{name:'Pertumbuhan',score:Math.max(0,Math.min(100,50+growth))},{name:'Validitas',score:k.transaksi?100-num(k.tidakValid)*100/num(k.transaksi):100},{name:'Member',score:member},{name:'Sebaran Produk',score:100-topShare},{name:'Sebaran Bayar',score:100-payShare}]);
      const insights=[];
      if(num(k.tidakValid)>0)insights.push(`<b>${k.tidakValid} transaksi perlu diperiksa.</b> Total master dan rincian berbeda. Buka Riwayat Penjualan, aktifkan filter Transaksi tidak valid, lalu cocokkan struk dan detail sebelum membuat koreksi.`);else insights.push('<b>Integritas transaksi baik.</b> Tidak ditemukan selisih total master dan detail pada periode ini. Pertahankan pemeriksaan harian.');
      insights.push(growth<0?`<b>Omzet turun ${Math.abs(growth).toFixed(1)}%.</b> Bandingkan produk yang turun, ketersediaan stok, jadwal petugas, dan promo terhadap periode sebelumnya sebelum menambah pembelian.`:`<b>Omzet tumbuh ${growth.toFixed(1)}%.</b> Pastikan stok produk utama dan kapasitas petugas cukup agar pertumbuhan tidak menimbulkan kehabisan stok atau antrean.`);
      if(peak)insights.push(`<b>Jam teramai sekitar ${String(peak.jam).padStart(2,'0')}:00.</b> Jadwalkan petugas, persiapan uang kecil, perangkat, dan pengisian rak sebelum jam tersebut.`);
      if(paymentTop)insights.push(`<b>${esc(paymentTop.nama)} menyumbang sekitar ${payShare.toFixed(0)}% omzet.</b> Bila proporsinya terlalu dominan, siapkan prosedur cadangan ketika kanal pembayaran itu terganggu.`);
      if(productTop)insights.push(`<b>${esc(productTop.nama)} adalah penyumbang terbesar (${topShare.toFixed(0)}%).</b> Jaga stok minimumnya dan hindari ketergantungan berlebih dengan mengembangkan produk pendamping.`);
      if(member<30)insights.push(`<b>Identifikasi member baru ${member.toFixed(0)}%.</b> Dorong kasir menanyakan nomor telepon secara sopan dan gunakan nomor yang sudah ada agar tidak membuat member ganda.`);
      $('saInsights').innerHTML=insights.map(x=>'<li>'+x+'</li>').join('');$('saStatus').className='alert alert-success';$('saStatus').textContent=`Analisis ${d.tglMulai} sampai ${d.tglSampai} berhasil dimuat. Semua grafik dan tabel dapat dicetak melalui tombol Cetak PDF.`;$('saContent').classList.remove('d-none');
    }catch(e){$('saStatus').className='alert alert-danger';$('saStatus').textContent='<%=Common.getBahasaConfigJS("Analisis belum dapat dimuat. Muat ulang halaman, periksa akses toko, lalu coba kembali.")%> '+e.message;}
  };
  // Muat satu kali sesudah partial terpasang. Konten tetap ringan karena hanya
  // agregasi server-side; pengguna dapat mengganti periode tanpa memuat halaman.
  setTimeout(()=>window['loadSalesAnalysis'+id](),120);
})();
</script>
