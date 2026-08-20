<%@ page isELIgnored="true" %>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// Halaman "Anggaran (RAB Bulanan)" -- versi JSP dari menu yang sama di Desktop/Android.
//
// Seluruh aturan bisnis (agregat induk, rollup realisasi, salin revisi, keaktifan
// penggunaan anggaran, gerbang hak akses) berada di server AnggaranApiHelper yang dipakai
// bersama ketiga kanal, sehingga angka yang tampil di JSP identik dengan yang tampil di
// aplikasi Desktop/Android dan dengan layar ZK asalnya (workspace_bulanan,
// workspace_revisi_bulanan, realisasi_bulanan, penggunaan_anggaran).
//
// Memakai isELIgnored="true": seluruh nilai dinamis dirangkai lewat penggabungan string
// JavaScript, tidak ada sintaks EL yang dapat dievaluasi server dan mengosongkan isi skrip.
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	out.print("{\"status\":\"error\", \"message\":\""
		+ Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
	return;
}
String rnd = Common.getGeneratedBarCode(7);
%>
<div class="container-fluid px-0">
  <div class="d-flex justify-content-between align-items-center mb-3">
    <div>
      <h4 class="fw-bold mb-0"><%=Common.getBahasaConfig("Anggaran (RAB Bulanan)")%></h4>
      <div class="text-muted small"><%=Common.getBahasaConfig("Rencana belanja per bulan, revisi, realisasi, dan penggunaan anggaran")%></div>
    </div>
    <div class="d-flex gap-2">
      <button type="button" class="btn btn-sm btn-outline-secondary" id="btnExcel<%=rnd%>">
        <i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Download Excel")%>
      </button>
      <button type="button" class="btn btn-sm btn-outline-secondary" id="btnPdf<%=rnd%>">
        <i class="fas fa-file-pdf me-1"></i><%=Common.getBahasaConfig("Cetak PDF")%>
      </button>
    </div>
  </div>

  <div class="card border-0 shadow-sm mb-3">
    <div class="card-body py-3">
      <div class="row g-2 align-items-end">
        <div class="col-6 col-md-2">
          <label class="form-label small fw-semibold text-secondary mb-1"><%=Common.getBahasaConfig("Tahun Anggaran")%></label>
          <select class="form-select form-select-sm" id="tahun<%=rnd%>"></select>
        </div>
        <div class="col-6 col-md-3">
          <label class="form-label small fw-semibold text-secondary mb-1"><%=Common.getBahasaConfig("Satuan Kerja")%></label>
          <select class="form-select form-select-sm" id="satker<%=rnd%>"></select>
        </div>
        <div class="col-6 col-md-2">
          <label class="form-label small fw-semibold text-secondary mb-1"><%=Common.getBahasaConfig("Sumber Dana")%></label>
          <select class="form-select form-select-sm" id="sumberDana<%=rnd%>"></select>
        </div>
        <div class="col-6 col-md-2">
          <label class="form-label small fw-semibold text-secondary mb-1"><%=Common.getBahasaConfig("Revisi")%></label>
          <select class="form-select form-select-sm" id="revisi<%=rnd%>"></select>
        </div>
        <div class="col-8 col-md-2">
          <label class="form-label small fw-semibold text-secondary mb-1"><%=Common.getBahasaConfig("Cari kode / nama")%></label>
          <input type="text" class="form-control form-control-sm" id="cari<%=rnd%>" autocomplete="off">
        </div>
        <div class="col-4 col-md-1 d-grid">
          <button type="button" class="btn btn-sm btn-primary" id="btnTerapkan<%=rnd%>">
            <i class="fas fa-filter me-1"></i><%=Common.getBahasaConfig("Terapkan")%>
          </button>
        </div>
      </div>
      <div class="d-flex flex-wrap gap-2 mt-3" id="aksiTambah<%=rnd%>">
        <button type="button" class="btn btn-sm btn-success" id="btnTambahItem<%=rnd%>">
          <i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah Item")%>
        </button>
        <button type="button" class="btn btn-sm btn-outline-primary" id="btnRevisiBaru<%=rnd%>">
          <i class="fas fa-code-branch me-1"></i><%=Common.getBahasaConfig("Buat Revisi Baru")%>
        </button>
        <button type="button" class="btn btn-sm btn-success d-none" id="btnTambahPakai<%=rnd%>">
          <i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Catat Penggunaan")%>
        </button>
      </div>
    </div>
  </div>

  <div class="d-flex flex-wrap gap-2 mb-2" id="kartu<%=rnd%>"></div>

  <ul class="nav nav-tabs mb-2" id="tab<%=rnd%>">
    <li class="nav-item"><a class="nav-link active" href="#" data-tab="0"><%=Common.getBahasaConfig("Rencana Bulanan")%></a></li>
    <li class="nav-item"><a class="nav-link" href="#" data-tab="1"><%=Common.getBahasaConfig("Realisasi")%></a></li>
    <li class="nav-item"><a class="nav-link" href="#" data-tab="2"><%=Common.getBahasaConfig("Penggunaan Anggaran")%></a></li>
  </ul>

  <div class="alert alert-light border small py-2" id="catatan<%=rnd%>">
    <i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Baris induk menampilkan jumlah seluruh turunannya, baik pagu maupun realisasi.")%>
  </div>

  <div class="table-responsive" style="max-height:62vh; overflow:auto;">
    <table class="table table-sm table-hover align-middle mb-0" id="tabel<%=rnd%>">
      <thead class="table-light" style="position:sticky; top:0; z-index:2;"><tr id="kepala<%=rnd%>"></tr></thead>
      <tbody id="isi<%=rnd%>"></tbody>
    </table>
  </div>
</div>

<%-- Formulir item anggaran --%>
<div class="modal fade" id="modalItem<%=rnd%>" tabindex="-1">
  <div class="modal-dialog modal-lg modal-dialog-scrollable">
    <div class="modal-content">
      <div class="modal-header">
        <h6 class="modal-title" id="judulItem<%=rnd%>"><%=Common.getBahasaConfig("Tambah Item Anggaran")%></h6>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <input type="hidden" id="itemId<%=rnd%>">
        <div class="row g-2">
          <div class="col-md-4">
            <label class="form-label small"><%=Common.getBahasaConfig("Kode")%></label>
            <input type="text" class="form-control form-control-sm" id="itemKode<%=rnd%>">
          </div>
          <div class="col-md-8">
            <label class="form-label small"><%=Common.getBahasaConfig("Nama Item")%> *</label>
            <input type="text" class="form-control form-control-sm" id="itemNama<%=rnd%>">
          </div>
          <div class="col-md-12">
            <label class="form-label small"><%=Common.getBahasaConfig("Induk")%></label>
            <select class="form-select form-select-sm" id="itemInduk<%=rnd%>"></select>
          </div>
          <div class="col-md-4">
            <label class="form-label small"><%=Common.getBahasaConfig("Volume")%></label>
            <input type="number" class="form-control form-control-sm" id="itemQty<%=rnd%>" value="1">
          </div>
          <div class="col-md-4">
            <label class="form-label small"><%=Common.getBahasaConfig("Satuan")%></label>
            <input type="text" class="form-control form-control-sm" id="itemSatuan<%=rnd%>">
          </div>
          <div class="col-md-4">
            <label class="form-label small"><%=Common.getBahasaConfig("Harga Satuan")%></label>
            <input type="number" class="form-control form-control-sm" id="itemHarga<%=rnd%>" value="0">
          </div>
          <div class="col-md-12">
            <label class="form-label small"><%=Common.getBahasaConfig("Keterangan")%></label>
            <input type="text" class="form-control form-control-sm" id="itemKeterangan<%=rnd%>">
          </div>
        </div>
        <hr class="my-3">
        <div class="fw-bold small mb-1"><%=Common.getBahasaConfig("Rencana per Bulan")%></div>
        <div class="text-muted small mb-2"><%=Common.getBahasaConfig("Untuk item yang punya turunan, nilai bulanan dihitung ulang server dari jumlah anaknya.")%></div>
        <div class="row g-2" id="itemBulan<%=rnd%>"></div>
        <div class="mt-2 fw-bold" id="itemTotal<%=rnd%>"></div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-sm btn-secondary" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
        <button type="button" class="btn btn-sm btn-primary" id="btnSimpanItem<%=rnd%>"><%=Common.getBahasaConfig("Simpan")%></button>
      </div>
    </div>
  </div>
</div>

<%-- Formulir penggunaan anggaran --%>
<div class="modal fade" id="modalPakai<%=rnd%>" tabindex="-1">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <h6 class="modal-title"><%=Common.getBahasaConfig("Catat Penggunaan Anggaran")%></h6>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <input type="hidden" id="pakaiId<%=rnd%>">
        <div class="mb-2">
          <label class="form-label small"><%=Common.getBahasaConfig("Item Anggaran")%> *</label>
          <select class="form-select form-select-sm" id="pakaiItem<%=rnd%>"></select>
        </div>
        <div class="mb-2">
          <label class="form-label small"><%=Common.getBahasaConfig("Kode / No. Bukti")%></label>
          <input type="text" class="form-control form-control-sm" id="pakaiKode<%=rnd%>">
        </div>
        <div class="mb-2">
          <label class="form-label small"><%=Common.getBahasaConfig("Uraian")%> *</label>
          <input type="text" class="form-control form-control-sm" id="pakaiNama<%=rnd%>">
        </div>
        <div class="mb-2">
          <label class="form-label small"><%=Common.getBahasaConfig("Nilai")%> *</label>
          <input type="number" class="form-control form-control-sm" id="pakaiNilai<%=rnd%>" value="0">
        </div>
        <div class="mb-2">
          <label class="form-label small"><%=Common.getBahasaConfig("Tanggal")%></label>
          <input type="date" class="form-control form-control-sm" id="pakaiWaktu<%=rnd%>">
        </div>
        <div class="mb-2">
          <label class="form-label small"><%=Common.getBahasaConfig("Keterangan")%></label>
          <input type="text" class="form-control form-control-sm" id="pakaiKeterangan<%=rnd%>">
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-sm btn-secondary" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
        <button type="button" class="btn btn-sm btn-primary" id="btnSimpanPakai<%=rnd%>"><%=Common.getBahasaConfig("Simpan")%></button>
      </div>
    </div>
  </div>
</div>

<script>
(function(){
  var DATA_URL = "<%=Common.ROOT%>/Data";
  var BULAN = ["Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agu","Sep","Okt","Nov","Des"];
  var tabAktif = 0;
  var konteks = {tahun:[], satker:[], sumberDana:[]};
  var item = [], realisasi = [], penggunaan = [];
  var ringkasanBulan = [], paguBulan = [], realisasiBulan = [];
  var totalPagu = 0, totalRealisasi = 0, totalPakai = 0;
  var hak = {create:true, update:true, "delete":true};

  function el(id){ return document.getElementById(id + "<%=rnd%>"); }
  function api(payload){
    return fetch(DATA_URL, {method:"POST", headers:{"Content-Type":"application/json"},
                            body: JSON.stringify(payload)})
      .then(function(r){ return r.json(); });
  }
  function pesan(teks, sukses){
    if (typeof tampilkanToast === "function") tampilkanToast(teks, sukses ? "bg-success text-white" : "bg-danger text-white");
    else alert(teks);
  }
  function esc(t){
    return String(t === null || t === undefined ? "" : t)
      .replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;");
  }
  function rp(n){
    var v = Number(n || 0);
    return v.toLocaleString("id-ID", {maximumFractionDigits:0});
  }
  function angka(id){ var v = parseFloat((el(id).value || "").replace(",", ".")); return isNaN(v) ? 0 : v; }

  // Baris respons Data servlet memakai konvensi lama ("00" sukses) sedangkan PosApi
  // memakai "success"; keduanya diterima supaya halaman ini tahan bila endpointnya berubah.
  function sukses(d){ return d && (d.status === "00" || d.status === "success"); }
  function alasan(d){ return (d && (d.description || d.message)) || "Permintaan belum berhasil."; }

  function filter(){
    var f = {
      tahun: parseInt(el("tahun").value || "0", 10),
      satkerId: parseInt(el("satker").value || "0", 10),
      sumberDanaId: parseInt(el("sumberDana").value || "0", 10),
      revisi: parseInt(el("revisi").value || "1", 10)
    };
    var cari = (el("cari").value || "").trim();
    if (cari) f.cari = cari;
    return f;
  }

  function bacaHak(d){
    if (d && d.hak) {
      hak = {create: d.hak.create !== false, update: d.hak.update !== false, "delete": d.hak["delete"] !== false};
    }
    el("btnTambahItem").classList.toggle("d-none", !(hak.create && tabAktif === 0));
    el("btnRevisiBaru").classList.toggle("d-none", !(hak.create && tabAktif === 0));
    el("btnTambahPakai").classList.toggle("d-none", !(hak.create && tabAktif === 2));
  }

  // ---- pohon: baris anak diberi indentasi sesuai kedalaman induknya, sama seperti
  //      tabel Kode Akun dan pohon pada layar ZK.
  function pohon(sumber){
    var anakDari = {}, akar = [], semua = {}, i;
    for (i = 0; i < sumber.length; i++) { semua["k" + sumber[i].id] = true; }
    for (i = 0; i < sumber.length; i++) {
      var p = sumber[i].parentId;
      if (p === null || p === undefined || !semua["k" + p]) { akar.push(sumber[i]); }
      else {
        if (!anakDari["k" + p]) anakDari["k" + p] = [];
        anakDari["k" + p].push(sumber[i]);
      }
    }
    var hasil = [];
    function telusuri(node, level){
      node._level = level;
      hasil.push(node);
      var anak = anakDari["k" + node.id] || [];
      for (var j = 0; j < anak.length; j++) { telusuri(anak[j], level + 1); }
    }
    for (i = 0; i < akar.length; i++) { telusuri(akar[i], 0); }
    return hasil;
  }

  function labelItem(a){
    var indent = "";
    for (var i = 0; i < (a._level || 0); i++) { indent += "&nbsp;&nbsp;&nbsp;&nbsp;"; }
    return indent + esc((a.kode || "") + " " + (a.nama || ""));
  }

  // ---- definisi kolom & baris per tab (dipakai tabel, Excel, dan PDF sekaligus)
  function kolom(){
    var i, k;
    if (tabAktif === 0) {
      k = ["Tingkat","Kode","Item","Total Setahun"];
      for (i = 0; i < 12; i++) { k.push(BULAN[i]); }
      return k;
    }
    if (tabAktif === 1) {
      k = ["Tingkat","Kode","Item","Pagu","Realisasi","Sisa","Persen","Transaksi"];
      for (i = 0; i < 12; i++) { k.push(BULAN[i]); }
      return k;
    }
    return ["Waktu","Kode","Uraian","Item Anggaran","Sumber","Nilai","Aktif"];
  }

  function baris(){
    var hasil = [], i, j;
    if (tabAktif === 0) {
      var pohonItem = pohon(item);
      for (i = 0; i < pohonItem.length; i++) {
        var a = pohonItem[i];
        var r = [a._level || 0, a.kode || "", ((a.kode || "") + " " + (a.nama || "")).trim(), Number(a.hargaTotal || 0)];
        for (j = 0; j < 12; j++) { r.push(Number((a.bulan || [])[j] || 0)); }
        hasil.push(r);
      }
      return hasil;
    }
    if (tabAktif === 1) {
      var pohonReal = pohon(realisasi);
      for (i = 0; i < pohonReal.length; i++) {
        var b = pohonReal[i];
        var r2 = [b._level || 0, b.kode || "", ((b.kode || "") + " " + (b.nama || "")).trim(),
                  Number(b.hargaTotal || 0), Number(b.realisasi || 0), Number(b.sisa || 0),
                  Number(b.persen || 0), Number(b.jumlahTransaksi || 0)];
        for (j = 0; j < 12; j++) { r2.push(Number((b.realisasiBulan || [])[j] || 0)); }
        hasil.push(r2);
      }
      return hasil;
    }
    for (i = 0; i < penggunaan.length; i++) {
      var p = penggunaan[i];
      hasil.push([p.waktu || "", p.kode || "", p.nama || "", p.workspaceLabel || "",
                  p.sumber || "", Number(p.nilai || 0), p.aktif ? "Ya" : "Tidak"]);
    }
    return hasil;
  }

  function gambarKartu(){
    var h = "";
    function kartu(judul, nilai, warna){
      return '<div class="border rounded-3 px-3 py-2 bg-light-subtle"><div class="small text-muted">' + esc(judul)
           + '</div><div class="fw-bold ' + warna + '">' + rp(nilai) + '</div></div>';
    }
    if (tabAktif === 0) { h = kartu("Total Pagu Setahun", totalPagu, ""); }
    else if (tabAktif === 1) {
      h = kartu("Pagu", totalPagu, "") + kartu("Realisasi", totalRealisasi, "text-success")
        + kartu("Sisa", totalPagu - totalRealisasi, "text-warning");
    } else { h = kartu("Total Penggunaan Aktif", totalPakai, "text-success"); }
    el("kartu").innerHTML = h;
    el("catatan").classList.toggle("d-none", tabAktif === 2);
  }

  function gambarTabel(){
    var kol = kolom(), i;
    var kepala = "";
    for (i = 0; i < kol.length; i++) {
      var kanan = (tabAktif === 2 ? (i === 5) : (i >= 3)) ? ' class="text-end"' : "";
      kepala += "<th" + kanan + ">" + esc(kol[i]) + "</th>";
    }
    kepala += '<th class="text-center" style="width:120px;">Aksi</th>';
    el("kepala").innerHTML = kepala;

    var isi = "", data;
    if (tabAktif === 0) {
      data = pohon(item);
      for (i = 0; i < data.length; i++) {
        var a = data[i];
        isi += "<tr><td>" + (a._level || 0) + "</td><td>" + esc(a.kode || "") + "</td><td>" + labelItem(a) + "</td>"
             + '<td class="text-end fw-semibold">' + rp(a.hargaTotal) + "</td>";
        for (var j = 0; j < 12; j++) { isi += '<td class="text-end">' + rp((a.bulan || [])[j]) + "</td>"; }
        isi += '<td class="text-center">' + tombolItem(a) + "</td></tr>";
      }
    } else if (tabAktif === 1) {
      data = pohon(realisasi);
      for (i = 0; i < data.length; i++) {
        var b = data[i];
        isi += "<tr><td>" + (b._level || 0) + "</td><td>" + esc(b.kode || "") + "</td><td>" + labelItem(b) + "</td>"
             + '<td class="text-end">' + rp(b.hargaTotal) + "</td>"
             + '<td class="text-end text-success fw-semibold">' + rp(b.realisasi) + "</td>"
             + '<td class="text-end">' + rp(b.sisa) + "</td>"
             + '<td class="text-end">' + Number(b.persen || 0).toFixed(1) + "%</td>"
             + '<td class="text-end">' + Number(b.jumlahTransaksi || 0) + "</td>";
        for (var k = 0; k < 12; k++) { isi += '<td class="text-end">' + rp((b.realisasiBulan || [])[k]) + "</td>"; }
        isi += '<td class="text-center text-muted small">&mdash;</td></tr>';
      }
    } else {
      for (i = 0; i < penggunaan.length; i++) {
        var p = penggunaan[i];
        isi += "<tr><td>" + esc(p.waktu || "") + "</td><td>" + esc(p.kode || "") + "</td><td>" + esc(p.nama || "") + "</td>"
             + "<td>" + esc(p.workspaceLabel || "") + "</td><td>" + esc(p.sumber || "") + "</td>"
             + '<td class="text-end">' + rp(p.nilai) + "</td>"
             + "<td>" + (p.aktif ? "Ya" : "Tidak") + "</td>"
             + '<td class="text-center">' + tombolPakai(p) + "</td></tr>";
      }
    }
    if (!isi) {
      isi = '<tr><td colspan="' + (kol.length + 1) + '" class="text-center text-muted py-4">'
          + "Belum ada data untuk penyaring ini.</td></tr>";
    }
    el("isi").innerHTML = isi;
  }

  function tombolItem(a){
    var h = "";
    if (hak.create) {
      h += '<button class="btn btn-sm btn-link p-0 me-2 aksi-anak" data-id="' + a.id + '" title="Tambah item di bawahnya"><i class="fas fa-plus"></i></button>';
    }
    if (hak.update) {
      h += '<button class="btn btn-sm btn-link p-0 me-2 aksi-ubah" data-id="' + a.id + '" title="Ubah"><i class="fas fa-pen"></i></button>';
    }
    if (hak["delete"]) {
      h += '<button class="btn btn-sm btn-link p-0 text-danger aksi-hapus" data-id="' + a.id + '" title="Hapus"><i class="fas fa-trash"></i></button>';
    }
    return h || '<span class="text-muted small">&mdash;</span>';
  }

  function tombolPakai(p){
    // Baris milik dokumen lain (uang muka, kas kecil, jurnal, gaji) TIDAK boleh disunting
    // di sini -- pembatalannya harus lewat dokumen asal supaya realisasi tetap sinkron.
    if (p.sumber !== "Entri Manual") {
      return '<i class="fas fa-lock text-muted" title="Berasal dari dokumen lain"></i>';
    }
    var h = "";
    if (hak.update) {
      h += '<button class="btn btn-sm btn-link p-0 me-2 pakai-ubah" data-id="' + p.id + '" title="Ubah"><i class="fas fa-pen"></i></button>';
    }
    if (hak["delete"]) {
      h += '<button class="btn btn-sm btn-link p-0 text-danger pakai-hapus" data-id="' + p.id + '" title="Hapus"><i class="fas fa-trash"></i></button>';
    }
    return h || '<span class="text-muted small">&mdash;</span>';
  }

  // ---- pemuatan data
  function muatKonteks(){
    return api({action:"anggaran_konteks"}).then(function(d){
      if (!sukses(d)) { pesan(alasan(d), false); return; }
      konteks.tahun = d.tahun || [];
      konteks.satker = d.satuanKerja || [];
      konteks.sumberDana = d.sumberDana || [];
      bacaHak(d);
      var i, o = "";
      for (i = 0; i < konteks.tahun.length; i++) { o += '<option value="' + konteks.tahun[i] + '">' + konteks.tahun[i] + "</option>"; }
      el("tahun").innerHTML = o;
      o = "";
      for (i = 0; i < konteks.satker.length; i++) {
        o += '<option value="' + konteks.satker[i].id + '">' + esc((konteks.satker[i].kode || "") + " " + (konteks.satker[i].nama || "")) + "</option>";
      }
      el("satker").innerHTML = o;
      isiSumberDana();
      return muatRevisi();
    });
  }

  function isiSumberDana(){
    var tahun = parseInt(el("tahun").value || "0", 10);
    var o = '<option value="0">= Semua =</option>', i;
    for (i = 0; i < konteks.sumberDana.length; i++) {
      var sd = konteks.sumberDana[i];
      if (sd.tahun && tahun && sd.tahun !== tahun) { continue; }
      o += '<option value="' + sd.id + '">' + esc(sd.nama || "") + "</option>";
    }
    el("sumberDana").innerHTML = o;
  }

  function muatRevisi(){
    var f = filter();
    return api({action:"anggaran_revisi_list", tahun:f.tahun, satkerId:f.satkerId, sumberDanaId:f.sumberDanaId})
      .then(function(d){
        if (!sukses(d)) { pesan(alasan(d), false); return; }
        var daftar = d.data || [], o = "", i;
        for (i = 0; i < daftar.length; i++) {
          o += '<option value="' + daftar[i].revisi + '">Revisi ' + daftar[i].revisi + " &middot; " + daftar[i].jumlahItem + " item</option>";
        }
        el("revisi").innerHTML = o || '<option value="1">Revisi 1</option>';
        return muatTab();
      });
  }

  function muatTab(){
    var f = filter();
    var aksi = tabAktif === 0 ? "anggaran_item_list" : (tabAktif === 1 ? "anggaran_realisasi_list" : "anggaran_penggunaan_list");
    f.action = aksi;
    return api(f).then(function(d){
      if (!sukses(d)) { pesan(alasan(d), false); return; }
      bacaHak(d);
      if (tabAktif === 0) {
        item = d.data || [];
        ringkasanBulan = d.ringkasanBulan || [];
        totalPagu = d.totalPagu || 0;
      } else if (tabAktif === 1) {
        realisasi = d.data || [];
        paguBulan = d.paguBulan || [];
        realisasiBulan = d.realisasiBulan || [];
        totalPagu = d.totalPagu || 0;
        totalRealisasi = d.totalRealisasi || 0;
      } else {
        penggunaan = d.data || [];
        totalPakai = d.totalAktif || 0;
        // Daftar item tetap dibutuhkan sebagai pilihan pada formulir penggunaan.
        if (!item.length) {
          var g = filter(); g.action = "anggaran_item_list";
          return api(g).then(function(x){ if (sukses(x)) { item = x.data || []; } gambarKartu(); gambarTabel(); });
        }
      }
      gambarKartu();
      gambarTabel();
    });
  }

  // ---- formulir item
  function isiPilihanInduk(terpilih, kecualiId){
    var o = '<option value="0">= Item Akar =</option>', i;
    var daftar = pohon(item);
    for (i = 0; i < daftar.length; i++) {
      if (kecualiId && daftar[i].id === kecualiId) { continue; }
      o += '<option value="' + daftar[i].id + '">' + esc((daftar[i].kode || "") + " " + (daftar[i].nama || "")) + "</option>";
    }
    el("itemInduk").innerHTML = o;
    el("itemInduk").value = String(terpilih || 0);
  }

  function isiBulanForm(nilai){
    var h = "", i;
    for (i = 0; i < 12; i++) {
      h += '<div class="col-6 col-md-3"><label class="form-label small mb-0">' + BULAN[i] + "</label>"
         + '<input type="number" class="form-control form-control-sm bulan-input" data-i="' + i + '" value="'
         + Number((nilai || [])[i] || 0) + '"></div>';
    }
    el("itemBulan").innerHTML = h;
    hitungTotalForm();
    var input = el("itemBulan").querySelectorAll(".bulan-input");
    for (i = 0; i < input.length; i++) { input[i].addEventListener("input", hitungTotalForm); }
  }

  function hitungTotalForm(){
    var input = el("itemBulan").querySelectorAll(".bulan-input"), t = 0, i;
    for (i = 0; i < input.length; i++) { t += parseFloat(input[i].value || "0") || 0; }
    el("itemTotal").innerHTML = "Total setahun: " + rp(t);
  }

  function bukaFormItem(data, indukId){
    el("itemId").value = data ? data.id : "";
    el("itemKode").value = data ? (data.kode || "") : "";
    el("itemNama").value = data ? (data.nama || "") : "";
    el("itemKeterangan").value = data ? (data.keterangan || "") : "";
    el("itemQty").value = data ? (data.qty || 1) : 1;
    el("itemSatuan").value = data ? (data.satuanVolume || "") : "";
    el("itemHarga").value = data ? (data.hargaSatuan || 0) : 0;
    el("judulItem").innerHTML = data ? "Ubah Item Anggaran" : (indukId ? "Tambah Item di Bawahnya" : "Tambah Item Anggaran");
    isiPilihanInduk(data ? data.parentId : indukId, data ? data.id : 0);
    isiBulanForm(data ? data.bulan : []);
    new bootstrap.Modal(el("modalItem")).show();
  }

  function simpanItem(){
    var f = filter();
    var input = el("itemBulan").querySelectorAll(".bulan-input"), bulan = [], i;
    for (i = 0; i < input.length; i++) { bulan.push(parseFloat(input[i].value || "0") || 0); }
    if (!(el("itemNama").value || "").trim()) { pesan("Nama item anggaran wajib diisi.", false); return; }
    var payload = {
      action:"anggaran_item_simpan",
      tahun: f.tahun, satkerId: f.satkerId, sumberDanaId: f.sumberDanaId, revisi: f.revisi,
      parentId: parseInt(el("itemInduk").value || "0", 10),
      kode: (el("itemKode").value || "").trim(),
      nama: (el("itemNama").value || "").trim(),
      keterangan: (el("itemKeterangan").value || "").trim(),
      qty: angka("itemQty"), satuanVolume: (el("itemSatuan").value || "").trim(),
      hargaSatuan: angka("itemHarga"), bulan: bulan
    };
    if (el("itemId").value) { payload.id = parseInt(el("itemId").value, 10); }
    api(payload).then(function(d){
      if (!sukses(d)) { pesan(alasan(d), false); return; }
      pesan(d.message || "Tersimpan.", true);
      bootstrap.Modal.getInstance(el("modalItem")).hide();
      muatTab();
    });
  }

  // ---- formulir penggunaan
  function bukaFormPakai(data){
    el("pakaiId").value = data ? data.id : "";
    el("pakaiKode").value = data ? (data.kode || "") : "";
    el("pakaiNama").value = data ? (data.nama || "") : "";
    el("pakaiNilai").value = data ? (data.nilai || 0) : 0;
    el("pakaiKeterangan").value = data ? (data.keterangan || "") : "";
    el("pakaiWaktu").value = data && data.waktu ? String(data.waktu).substring(0, 10)
                                                : new Date().toISOString().substring(0, 10);
    var o = "", i, daftar = pohon(item);
    for (i = 0; i < daftar.length; i++) {
      o += '<option value="' + daftar[i].id + '">' + esc((daftar[i].kode || "") + " " + (daftar[i].nama || "")) + "</option>";
    }
    el("pakaiItem").innerHTML = o;
    if (data && data.workspaceId) { el("pakaiItem").value = String(data.workspaceId); }
    new bootstrap.Modal(el("modalPakai")).show();
  }

  function simpanPakai(){
    if (!(el("pakaiNama").value || "").trim() || !el("pakaiItem").value) {
      pesan("Item anggaran dan uraian wajib diisi.", false);
      return;
    }
    var payload = {
      action:"anggaran_penggunaan_simpan",
      workspaceId: parseInt(el("pakaiItem").value, 10),
      kode: (el("pakaiKode").value || "").trim(),
      nama: (el("pakaiNama").value || "").trim(),
      keterangan: (el("pakaiKeterangan").value || "").trim(),
      nilai: angka("pakaiNilai"),
      waktu: (el("pakaiWaktu").value || "") + " 00:00:00"
    };
    if (el("pakaiId").value) { payload.id = parseInt(el("pakaiId").value, 10); }
    api(payload).then(function(d){
      if (!sukses(d)) { pesan(alasan(d), false); return; }
      pesan(d.message || "Tersimpan.", true);
      bootstrap.Modal.getInstance(el("modalPakai")).hide();
      muatTab();
    });
  }

  // ---- ekspor: isi berkas mengikuti TAB AKTIF beserta penyaringnya
  function judulTab(){
    return tabAktif === 0 ? "Rencana Bulanan" : (tabAktif === 1 ? "Realisasi" : "Penggunaan Anggaran");
  }
  function konteksTeks(){
    var satker = el("satker").options[el("satker").selectedIndex];
    var sd = el("sumberDana").options[el("sumberDana").selectedIndex];
    var t = "Tahun " + (el("tahun").value || "-") + " · Satker " + (satker ? satker.text : "-")
          + " · Sumber Dana " + (sd ? sd.text : "-") + " · Revisi " + (el("revisi").value || "-");
    var cari = (el("cari").value || "").trim();
    return cari ? (t + " · Cari \"" + cari + "\"") : t;
  }
  function namaBerkas(){
    var d = new Date();
    function dd(n){ return (n < 10 ? "0" : "") + n; }
    return "Anggaran_" + judulTab().replace(/ /g, "_") + "_" + (el("tahun").value || "0")
         + "_R" + (el("revisi").value || "0") + "_" + d.getFullYear() + dd(d.getMonth() + 1) + dd(d.getDate())
         + "_" + dd(d.getHours()) + dd(d.getMinutes());
  }
  function tabelHtmlEkspor(){
    var kol = kolom(), data = baris(), h = "", i, j;
    h += "<table border='1'><thead><tr>";
    for (i = 0; i < kol.length; i++) { h += "<th>" + esc(kol[i]) + "</th>"; }
    h += "</tr></thead><tbody>";
    for (i = 0; i < data.length; i++) {
      h += "<tr>";
      for (j = 0; j < data[i].length; j++) {
        var v = data[i][j];
        // Angka dikirim apa adanya supaya Excel memperlakukannya sebagai angka,
        // bukan teks yang harus dibersihkan dulu sebelum dijumlah.
        h += (typeof v === "number") ? ('<td style="mso-number-format:0">' + v + "</td>") : ("<td>" + esc(v) + "</td>");
      }
      h += "</tr>";
    }
    h += "</tbody></table>";
    return h;
  }
  function unduhExcel(){
    var data = baris();
    if (!data.length) { pesan("Tidak ada data untuk diunduh pada tab ini.", false); return; }
    var html = '<html xmlns:x="urn:schemas-microsoft-com:office:excel"><head><meta charset="utf-8"></head><body>'
             + "<h3>Anggaran (RAB Bulanan) &mdash; " + esc(judulTab()) + "</h3><div>" + esc(konteksTeks()) + "</div>"
             + tabelHtmlEkspor() + "</body></html>";
    var blob = new Blob(["﻿", html], {type:"application/vnd.ms-excel"});
    var a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = namaBerkas() + ".xls";
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    pesan(data.length + " baris disiapkan ke Excel.", true);
  }
  function cetakPdf(){
    var data = baris();
    if (!data.length) { pesan("Tidak ada data untuk dicetak pada tab ini.", false); return; }
    var ringkas = tabAktif === 0 ? ("Total pagu setahun: " + rp(totalPagu))
                : (tabAktif === 1 ? ("Pagu: " + rp(totalPagu) + "   Realisasi: " + rp(totalRealisasi)
                                     + "   Sisa: " + rp(totalPagu - totalRealisasi))
                                  : ("Total penggunaan aktif: " + rp(totalPakai)));
    var w = window.open("", "_blank");
    if (!w) { pesan("Jendela cetak diblokir peramban. Izinkan pop-up untuk halaman ini.", false); return; }
    // Mendatar: tab rencana dan realisasi punya dua belas kolom bulan.
    w.document.write('<html><head><meta charset="utf-8"><title>' + esc(namaBerkas()) + "</title>"
      + "<style>@page{size:A4 landscape;margin:10mm;} body{font-family:Arial,sans-serif;font-size:9px;}"
      + "table{border-collapse:collapse;width:100%;} th,td{border:1px solid #999;padding:2px 4px;}"
      + "th{background:#eee;} td.n{text-align:right;} h3{margin:0;} .ket{color:#555;margin-bottom:6px;}</style></head><body>"
      + "<h3>Anggaran (RAB Bulanan) &mdash; " + esc(judulTab()) + "</h3>"
      + '<div class="ket">' + esc(konteksTeks()) + "</div>");
    var kol = kolom(), i, j;
    w.document.write("<table><thead><tr>");
    for (i = 0; i < kol.length; i++) { w.document.write("<th>" + esc(kol[i]) + "</th>"); }
    w.document.write("</tr></thead><tbody>");
    for (i = 0; i < data.length; i++) {
      w.document.write("<tr>");
      for (j = 0; j < data[i].length; j++) {
        var v = data[i][j];
        w.document.write(typeof v === "number" ? ('<td class="n">' + rp(v) + "</td>") : ("<td>" + esc(v) + "</td>"));
      }
      w.document.write("</tr>");
    }
    w.document.write("</tbody></table><p>" + esc(ringkas) + "</p></body></html>");
    w.document.close();
    w.focus();
    w.print();
  }

  // ---- kejadian
  el("tab").addEventListener("click", function(ev){
    var a = ev.target.closest("a[data-tab]");
    if (!a) { return; }
    ev.preventDefault();
    var link = el("tab").querySelectorAll("a[data-tab]");
    for (var i = 0; i < link.length; i++) { link[i].classList.remove("active"); }
    a.classList.add("active");
    tabAktif = parseInt(a.getAttribute("data-tab"), 10);
    bacaHak(null);
    muatTab();
  });
  el("btnTerapkan").addEventListener("click", muatTab);
  el("cari").addEventListener("keydown", function(e){ if (e.key === "Enter") { muatTab(); } });
  el("tahun").addEventListener("change", function(){ isiSumberDana(); muatRevisi(); });
  el("satker").addEventListener("change", muatRevisi);
  el("sumberDana").addEventListener("change", muatRevisi);
  el("revisi").addEventListener("change", muatTab);
  el("btnTambahItem").addEventListener("click", function(){ bukaFormItem(null, 0); });
  el("btnSimpanItem").addEventListener("click", simpanItem);
  el("btnTambahPakai").addEventListener("click", function(){ bukaFormPakai(null); });
  el("btnSimpanPakai").addEventListener("click", simpanPakai);
  el("btnExcel").addEventListener("click", unduhExcel);
  el("btnPdf").addEventListener("click", cetakPdf);
  el("btnRevisiBaru").addEventListener("click", function(){
    if (!confirm("Seluruh item revisi tertinggi akan disalin menjadi revisi berikutnya beserta hierarkinya. Lanjutkan?")) { return; }
    var f = filter();
    api({action:"anggaran_revisi_baru", tahun:f.tahun, satkerId:f.satkerId, sumberDanaId:f.sumberDanaId})
      .then(function(d){
        if (!sukses(d)) { pesan(alasan(d), false); return; }
        pesan(d.message || "Revisi baru dibuat.", true);
        muatRevisi();
      });
  });

  el("isi").addEventListener("click", function(ev){
    var t = ev.target.closest("button");
    if (!t) { return; }
    var id = parseInt(t.getAttribute("data-id") || "0", 10);
    var i, data = null;
    if (t.classList.contains("aksi-anak") || t.classList.contains("aksi-ubah") || t.classList.contains("aksi-hapus")) {
      for (i = 0; i < item.length; i++) { if (item[i].id === id) { data = item[i]; } }
    } else {
      for (i = 0; i < penggunaan.length; i++) { if (penggunaan[i].id === id) { data = penggunaan[i]; } }
    }
    if (t.classList.contains("aksi-anak")) { bukaFormItem(null, id); return; }
    if (t.classList.contains("aksi-ubah")) { bukaFormItem(data, 0); return; }
    if (t.classList.contains("aksi-hapus")) {
      if (!confirm("Hapus item \"" + ((data && data.nama) || "") + "\"? Item yang punya turunan atau sudah dipakai realisasi akan ditolak server.")) { return; }
      api({action:"anggaran_item_hapus", id:id}).then(function(d){
        if (!sukses(d)) { pesan(alasan(d), false); return; }
        pesan(d.message || "Terhapus.", true);
        muatTab();
      });
      return;
    }
    if (t.classList.contains("pakai-ubah")) { bukaFormPakai(data); return; }
    if (t.classList.contains("pakai-hapus")) {
      if (!confirm("Hapus penggunaan anggaran ini?")) { return; }
      api({action:"anggaran_penggunaan_hapus", id:id}).then(function(d){
        if (!sukses(d)) { pesan(alasan(d), false); return; }
        pesan(d.message || "Terhapus.", true);
        muatTab();
      });
    }
  });

  muatKonteks();
})();
</script>
