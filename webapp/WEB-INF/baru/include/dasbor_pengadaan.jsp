<%@ page isELIgnored="true" %>
<%@page import="ais.common.Common"%>
<%
// Dasbor Pengadaan versi JSP -- satu berkas melayani enam tahap.
//
// Disertakan lewat <jsp:include> dengan param "tahap" (pr, po, bast, tagihan,
// dpc, pajak). Aksi server `pengadaan_dasbor` mengembalikan bentuk SERAGAM
// (kpi, tren, komposisi, peringkat, daftar, corong), sehingga perender di sini
// cukup satu -- sama seperti tab Dasbor pada Desktop/Android.
//
// Grafiknya dirangkai dari HTML+CSS biasa, mengikuti cara DashboardUiKit versi
// ZKoss; tidak ada pustaka grafik luar yang perlu dimuat.
//
// isELIgnored="true": nilai dinamis dirangkai lewat penggabungan string.
String tahapDasbor = request.getParameter("tahap");
if (tahapDasbor == null) { tahapDasbor = "pr"; }
String rndDasbor = Common.getGeneratedBarCode(7);
%>
<div class="mb-2 d-flex align-items-center gap-2">
  <span class="small text-muted"><%=Common.getBahasaConfig("Periode")%></span>
  <select id="dbBulan<%=rndDasbor%>" class="form-select form-select-sm" style="width:auto"
          onchange="dbMuat<%=rndDasbor%>()">
    <option value="3">3 <%=Common.getBahasaConfig("bulan")%></option>
    <option value="6">6 <%=Common.getBahasaConfig("bulan")%></option>
    <option value="12" selected>12 <%=Common.getBahasaConfig("bulan")%></option>
    <option value="24">24 <%=Common.getBahasaConfig("bulan")%></option>
  </select>
  <button class="btn btn-sm btn-outline-secondary" onclick="dbMuat<%=rndDasbor%>()">
    <i class="fas fa-rotate me-1"></i><%=Common.getBahasaConfig("Muat ulang")%>
  </button>
</div>

<div id="dbKpi<%=rndDasbor%>" class="row g-2 mb-3"></div>
<div id="dbIsi<%=rndDasbor%>"></div>

<script>
(function(){
  var RND = "<%=rndDasbor%>";
  var TAHAP = "<%=tahapDasbor%>";
  var DATA_URL = "<%=Common.ROOT%>/Data";

  function el(id){ return document.getElementById(id + RND); }
  function esc(s){ return (s==null?"":String(s)).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;"); }
  function rp(n){ return "Rp " + (Number(n)||0).toLocaleString("id-ID"); }
  function api(payload){
    return fetch(DATA_URL, {method:"POST", headers:{"Content-Type":"application/json"},
                            body: JSON.stringify(payload)}).then(function(r){ return r.json(); });
  }
  var PALET = ["#1d4ed8","#0ea5e9","#15803d","#b45309","#dc2626","#7c3aed","#0f766e","#64748b"];

  function panel(judul, isi){
    return '<div class="card border-0 shadow-sm mb-3"><div class="card-body">'
         + '<div class="fw-bold mb-2" style="font-size:13px">' + esc(judul) + '</div>'
         + isi + '</div></div>';
  }

  /* Batang vertikal: tinggi relatif terhadap nilai terbesar. */
  function barVertikal(data, fmt){
    if (!data || !data.length) return '<div class="text-muted small">Belum ada data.</div>';
    var maks = 0;
    for (var i=0;i<data.length;i++){ maks = Math.max(maks, Number(data[i].nilai)||0); }
    if (maks <= 0) maks = 1;
    var h = '<div class="d-flex align-items-end gap-1" style="height:150px">';
    for (var j=0;j<data.length;j++){
      var v = Number(data[j].nilai)||0;
      var t = Math.max(2, Math.round((v / maks) * 130));
      h += '<div class="flex-fill text-center" title="' + esc(data[j].label) + ': ' + fmt(v) + '">'
         + '<div style="height:' + (132 - t) + 'px"></div>'
         + '<div style="height:' + t + 'px;background:#1d4ed8;border-radius:3px 3px 0 0"></div>'
         + '<div class="text-muted" style="font-size:9px;white-space:nowrap;overflow:hidden">'
         + esc(data[j].label) + '</div></div>';
    }
    return h + '</div>';
  }

  /* Batang mendatar berperingkat. */
  function barMendatar(data, fmt){
    if (!data || !data.length) return '<div class="text-muted small">Belum ada data.</div>';
    var maks = 0;
    for (var i=0;i<data.length;i++){ maks = Math.max(maks, Number(data[i].nilai)||0); }
    if (maks <= 0) maks = 1;
    var h = "";
    for (var j=0;j<data.length;j++){
      var v = Number(data[j].nilai)||0;
      var w = Math.max(1, Math.round((v / maks) * 100));
      h += '<div class="mb-1">'
         + '<div class="d-flex justify-content-between" style="font-size:11px">'
         + '<span>' + esc(data[j].label) + '</span><span class="fw-bold">' + fmt(v) + '</span></div>'
         + '<div style="background:#e2e8f0;border-radius:4px;height:8px">'
         + '<div style="width:' + w + '%;background:' + PALET[j % PALET.length]
         + ';height:8px;border-radius:4px"></div></div></div>';
    }
    return h;
  }

  /* Komposisi proporsional dalam satu batang. */
  function stackProporsi(data){
    if (!data || !data.length) return '<div class="text-muted small">Belum ada data.</div>';
    var total = 0;
    for (var i=0;i<data.length;i++){ total += Number(data[i].nilai)||0; }
    if (total <= 0) return '<div class="text-muted small">Belum ada data.</div>';
    var bar = '<div class="d-flex" style="height:16px;border-radius:8px;overflow:hidden">';
    var ket = '<div class="d-flex flex-wrap gap-2 mt-2">';
    for (var j=0;j<data.length;j++){
      var v = Number(data[j].nilai)||0;
      var p = (v / total) * 100;
      bar += '<div style="width:' + p + '%;background:' + PALET[j % PALET.length]
           + '" title="' + esc(data[j].label) + ' = ' + v + '"></div>';
      ket += '<span style="font-size:11px"><span style="display:inline-block;width:10px;height:10px;'
           + 'background:' + PALET[j % PALET.length] + ';border-radius:2px"></span> '
           + esc(data[j].label) + ' (' + v + ')</span>';
    }
    return bar + '</div>' + ket + '</div>';
  }

  function kartuKpi(k){
    var warna = k.warna || "#1d4ed8";
    return '<div class="col-6 col-md-3">'
         + '<div class="border rounded p-2 h-100" style="border-color:' + warna + '33 !important;'
         + 'background:' + warna + '0f">'
         + '<div class="fw-bold" style="color:' + warna + ';font-size:15px">' + esc(k.nilai) + '</div>'
         + '<div class="text-muted" style="font-size:11px">' + esc(k.label) + '</div>'
         + (k.catatan ? '<div class="text-muted" style="font-size:10px">' + esc(k.catatan) + '</div>' : '')
         + '</div></div>';
  }

  function tabelPerhatian(judul, baris){
    if (!baris || !baris.length) return "";
    var h = '<div class="table-responsive"><table class="table table-sm mb-0"><tbody>';
    for (var i=0;i<baris.length;i++){
      var b = baris[i];
      h += '<tr><td class="small"><span class="fw-bold">' + esc(b.kode || "-") + '</span>'
         + (b.keterangan ? '<div class="text-muted" style="font-size:10px">' + esc(b.keterangan) + '</div>' : '')
         + '</td>'
         + '<td class="text-end small text-warning">' + (Number(b.umurHari) > 0 ? (b.umurHari + ' hari') : '') + '</td>'
         + '<td class="text-end small fw-bold">' + rp(b.nilai) + '</td></tr>';
    }
    return panel(judul, h + '</tbody></table></div>');
  }

  window["dbMuat" + RND] = function(){
    var bulan = Number(el("dbBulan").value) || 12;
    el("dbKpi").innerHTML = '<div class="col-12 text-muted small py-3">Memuat dasbor...</div>';
    el("dbIsi").innerHTML = "";
    api({action:"pengadaan_dasbor", tahap: TAHAP, bulan: bulan}).then(function(d){
      if (d.status !== "00" && d.status !== "success"){
        el("dbKpi").innerHTML = '<div class="col-12 text-danger small py-3">'
          + esc(d.description || "Gagal memuat dasbor.") + '</div>';
        return;
      }
      var kpi = d.kpi || [];
      var hk = "";
      for (var i=0;i<kpi.length;i++){ hk += kartuKpi(kpi[i]); }
      el("dbKpi").innerHTML = hk || '<div class="col-12 text-muted small py-3">'
        + esc(d.catatanKosong || "Belum ada data pada periode ini.") + '</div>';

      var tren = d.tren || [];
      var trenNilai = [], trenJumlah = [];
      for (var t=0;t<tren.length;t++){
        trenNilai.push({label: tren[t].label, nilai: tren[t].nilai});
        trenJumlah.push({label: tren[t].label, nilai: tren[t].jumlah});
      }

      var h = "";
      if ((d.corong || []).length){
        h += panel("Ringkasan Tahapan (PR sampai Bayar)",
                   barMendatar(d.corong, function(v){ return String(v); }));
      }
      if (trenNilai.length){
        h += panel(d.trenJudul || "Tren Nilai per Bulan", barVertikal(trenNilai, rp));
        h += panel("Jumlah Dokumen per Bulan", barVertikal(trenJumlah, function(v){ return String(v); }));
      }
      if ((d.komposisi || []).length){
        h += panel(d.komposisiJudul || "Komposisi Status", stackProporsi(d.komposisi));
      }
      if ((d.peringkat || []).length){
        h += panel(d.peringkatJudul || "Peringkat", barMendatar(d.peringkat, rp));
      }
      if ((d.caraBayar || []).length){
        h += panel("Komposisi Cara Transfer", stackProporsi(d.caraBayar));
      }
      h += tabelPerhatian(d.daftarJudul || "Perlu Perhatian", d.daftar);
      el("dbIsi").innerHTML = h;
    }).catch(function(){
      el("dbKpi").innerHTML = '<div class="col-12 text-danger small py-3">Kesalahan koneksi.</div>';
    });
  };

  window["dbMuat" + RND]();
})();
</script>
