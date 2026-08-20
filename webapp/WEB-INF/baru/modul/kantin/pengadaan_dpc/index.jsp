<%@ page isELIgnored="true" %>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// Halaman "Pembayaran Vendor" -- modul Pengadaan POS versi JSP.
// Memakai tabel pembayaran termin yang sama dengan ZKoss, dibedakan kolom toko.
// Yang membuat pembayaran DIAKUI adalah PERSETUJUAN: perhitungan kanonik pada
// PemesananPengadaanMasterAsset.hitungDibayar hanya menjumlahkan dokumen yang
// sudah disetujui, sehingga draf sengaja belum mengubah status PO.
//
// isELIgnored="true": nilai dinamis dirangkai lewat penggabungan string JavaScript.
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	out.print("{\"status\":\"error\", \"message\":\""
		+ Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
	return;
}
Pedagang pedagang = tbmuser.getPedagang();
Toko toko = pedagang == null ? null : pedagang.getToko();
String rnd = Common.getGeneratedBarCode(7);
%>
<div class="container-fluid px-0">
  <div class="d-flex justify-content-between align-items-center mb-3">
    <div>
      <h4 class="fw-bold mb-0"><%=Common.getBahasaConfig("Pembayaran Vendor")%></h4>
      <div class="text-muted small"><%=Common.getBahasaConfig("Bayar tagihan penyedia atas pesanan yang sudah disetujui")%></div>
    </div>
    <button class="btn btn-primary fw-bold rounded-pill px-4" onclick="dpBaru<%=rnd%>()">
      <i class="fas fa-money-bill-wave me-2"></i><%=Common.getBahasaConfig("Bayar Vendor")%>
    </button>
  </div>

  <%-- Dua tab pada setiap menu Pengadaan: "Dasbor" (ringkasan angka) dan
       "Pembayaran" (daftar + CRUD). Susunannya sama di keenam menu, dan
       sepadan dengan tab yang sama di Desktop/Android. --%>
  <ul class="nav nav-tabs mb-3" role="tablist">
    <li class="nav-item" role="presentation">
      <button class="nav-link active" data-bs-toggle="tab"
              data-bs-target="#tabDasbor<%=rnd%>" type="button" role="tab">
        <i class="fas fa-chart-line me-2"></i><%=Common.getBahasaConfig("Dasbor")%>
      </button>
    </li>
    <li class="nav-item" role="presentation">
      <button class="nav-link" data-bs-toggle="tab"
              data-bs-target="#tabData<%=rnd%>" type="button" role="tab">
        <i class="fas fa-list me-2"></i><%=Common.getBahasaConfig("Pembayaran")%>
      </button>
    </li>
  </ul>
  <div class="tab-content">
  <div class="tab-pane fade show active" id="tabDasbor<%=rnd%>" role="tabpanel">
    <jsp:include page="/WEB-INF/baru/include/dasbor_pengadaan.jsp">
      <jsp:param name="tahap" value="dpc"/>
    </jsp:include>
  </div>
  <div class="tab-pane fade" id="tabData<%=rnd%>" role="tabpanel">
  <div class="card border-0 shadow-sm mb-3">
    <div class="card-body py-3">
      <div class="row g-2 align-items-end">
        <div class="col-md-5">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Cari kode / keterangan")%></label>
          <input type="text" id="dpCari<%=rnd%>" class="form-control" onkeydown="if(event.key==='Enter')dpMuat<%=rnd%>(1)">
        </div>
        <div class="col-md-3">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Status")%></label>
          <select id="dpStatus<%=rnd%>" class="form-select" onchange="dpMuat<%=rnd%>(1)">
            <option value=""><%=Common.getBahasaConfig("Semua status")%></option>
            <option value="DRAFT">DRAFT</option>
            <option value="DISETUJUI">DISETUJUI</option>
          </select>
        </div>
        <div class="col-md-2">
          <button class="btn btn-outline-secondary w-100" onclick="dpMuat<%=rnd%>(1)">
            <i class="fas fa-search me-1"></i><%=Common.getBahasaConfig("Cari")%>
          </button>
        </div>
      </div>
    </div>
  </div>

  <div class="card border-0 shadow-sm">
    <div class="table-responsive">
      <table class="table table-hover align-middle mb-0">
        <thead class="table-light">
          <tr>
            <th><%=Common.getBahasaConfig("Kode")%></th>
            <th><%=Common.getBahasaConfig("Tanggal")%></th>
            <th><%=Common.getBahasaConfig("Penyedia")%></th>
            <th class="text-end"><%=Common.getBahasaConfig("Nilai")%></th>
            <th><%=Common.getBahasaConfig("Status")%></th>
            <th class="text-center"><%=Common.getBahasaConfig("Aksi")%></th>
          </tr>
        </thead>
        <tbody id="dpTbody<%=rnd%>">
          <tr><td colspan="6" class="text-center text-muted py-4"><%=Common.getBahasaConfig("Memuat data...")%></td></tr>
        </tbody>
      </table>
    </div>
    <div class="card-footer bg-white d-flex justify-content-between align-items-center">
      <span class="small text-muted" id="dpInfo<%=rnd%>"></span>
      <div class="btn-group">
        <button class="btn btn-sm btn-outline-secondary" id="dpPrev<%=rnd%>" onclick="dpHal<%=rnd%>(-1)">&laquo;</button>
        <button class="btn btn-sm btn-outline-secondary" id="dpNext<%=rnd%>" onclick="dpHal<%=rnd%>(1)">&raquo;</button>
      </div>
    </div>
  </div>
  </div>
  </div>
</div>

<div class="modal fade" id="dpModal<%=rnd%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-xl modal-dialog-scrollable">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="dpModalJudul<%=rnd%>"><%=Common.getBahasaConfig("Pembayaran Vendor")%></h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <div id="dpKunciInfo<%=rnd%>" class="alert alert-warning small d-none"></div>
        <div class="alert alert-info small">
          <%=Common.getBahasaConfig("Dokumen ini baru mengubah status pesanan setelah DISETUJUI; draf belum diakui sebagai pembayaran.")%>
        </div>
        <div class="mb-3">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Judul transfer")%></label>
          <input type="text" id="dpJudul<%=rnd%>" class="form-control"
                 placeholder="<%=Common.getBahasaConfig("Mis. Pembayaran termin I CV Sumber Rejeki")%>">
        </div>
        <div class="row g-2 mb-3">
          <div class="col-md-8">
            <label class="form-label small mb-1"><%=Common.getBahasaConfig("Cara transfer")%> *</label>
            <select id="dpCaraBayar<%=rnd%>" class="form-select"></select>
            <div class="form-text small"><%=Common.getBahasaConfig("Akun pada cara transfer dipakai saat jurnal dibentuk")%></div>
          </div>
          <div class="col-md-4">
            <label class="form-label small mb-1"><%=Common.getBahasaConfig("Tanggal realisasi")%></label>
            <input type="text" id="dpTglRealisasi<%=rnd%>" class="form-control" placeholder="dd-MM-yyyy">
          </div>
        </div>
        <div class="mb-3">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Keterangan pembayaran")%></label>
          <input type="text" id="dpKeterangan<%=rnd%>" class="form-control">
        </div>
        <h6 class="fw-bold mb-2"><%=Common.getBahasaConfig("Tagihan yang Dibayar")%></h6>
        <div class="table-responsive">
          <table class="table table-sm align-middle">
            <thead class="table-light">
              <tr>
                <th style="width:50px"></th>
                <th><%=Common.getBahasaConfig("Pesanan / Termin")%></th>
                <th class="text-end" style="width:140px"><%=Common.getBahasaConfig("Nilai Tagih")%></th>
                <th class="text-end" style="width:140px"><%=Common.getBahasaConfig("Sisa")%></th>
                <th style="width:150px"><%=Common.getBahasaConfig("Dibayar")%></th>
              </tr>
            </thead>
            <tbody id="dpBarisTbody<%=rnd%>"></tbody>
            <tfoot>
              <tr class="fw-bold">
                <td colspan="4" class="text-end"><%=Common.getBahasaConfig("Total Dibayar")%></td>
                <td id="dpTotal<%=rnd%>">0</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Tutup")%></button>
        <button type="button" class="btn btn-primary" id="dpSimpan<%=rnd%>" onclick="dpSimpan<%=rnd%>()"><%=Common.getBahasaConfig("Simpan")%></button>
      </div>
    </div>
  </div>
</div>

<div class="modal fade" id="dpVendorModal<%=rnd%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title"><%=Common.getBahasaConfig("Pilih Penyedia / Vendor")%></h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <input type="text" id="dpVendorCari<%=rnd%>" class="form-control mb-2"
               placeholder="<%=Common.getBahasaConfig("Cari kode / nama penyedia")%>"
               onkeydown="if(event.key==='Enter')dpVendorMuat<%=rnd%>()">
        <div id="dpVendorHasil<%=rnd%>" class="list-group"></div>
      </div>
    </div>
  </div>
</div>


<%-- Tombol Bantuan mengambang; isinya sepadan dengan bantuan Desktop/Android. --%>
<jsp:include page="/WEB-INF/baru/include/bantuan_pengadaan.jsp">
  <jsp:param name="tahap" value="dpc"/>
</jsp:include>

<script>
(function(){
  var RND = "<%=rnd%>";
  var DATA_URL = "<%=Common.ROOT%>/Data";
  var halaman = 1, totalHal = 1, dpAktif = null, baris = [];
  var vendorId = null, vendorNama = "", terkunciKini = false;

  function el(id){ return document.getElementById(id + RND); }
  function esc(s){ return (s==null?"":String(s)).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;"); }
  function rp(n){ return "Rp " + (Number(n)||0).toLocaleString("id-ID"); }
  function angka(v){ v = String(v==null?"":v).replace(/[^0-9.]/g,""); return Number(v)||0; }
  function api(payload){
    return fetch(DATA_URL, {method:"POST", headers:{"Content-Type":"application/json"},
                            body: JSON.stringify(payload)}).then(function(r){ return r.json(); });
  }
  function pesan(teks, sukses){
    if (typeof tampilkanToast === "function") tampilkanToast(teks, sukses ? "bg-success text-white" : "bg-danger text-white");
    else alert(teks);
  }

  window["dpMuat" + RND] = function(hal){
    halaman = hal || halaman;
    var tbody = el("dpTbody");
    tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">Memuat data...</td></tr>';
    api({action:"pengadaan_bayar_daftar", cari: el("dpCari").value.trim(),
         status: el("dpStatus").value, page: halaman, pageSize: 15})
      .then(function(d){
        if (d.status !== "00" && d.status !== "success"){
          tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger py-4">' + esc(d.description || "Gagal memuat data.") + '</td></tr>';
          return;
        }
        var rows = d.data || [];
        totalHal = Math.max(1, Math.ceil((d.total || rows.length) / 15));
        if (!rows.length){
          tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">Belum ada pembayaran vendor.</td></tr>';
        } else {
          var html = "";
          for (var i=0;i<rows.length;i++){
            var r = rows[i], st = r.status || "DRAFT", disetujui = (st === "DISETUJUI");
            var aksi = '<button class="btn btn-sm btn-outline-primary me-1" title="Lihat / ubah" onclick="dpForm' + RND + '(' + r.id + ')"><i class="fas fa-edit"></i></button>';
            if (disetujui){
              aksi += '<button class="btn btn-sm btn-outline-secondary" title="Batalkan persetujuan" onclick="dpPutusan' + RND + '(' + r.id + ',\'BATAL\')"><i class="fas fa-undo"></i></button>';
            } else {
              aksi += '<button class="btn btn-sm btn-outline-success me-1" title="Setujui" onclick="dpPutusan' + RND + '(' + r.id + ',\'SETUJUI\')"><i class="fas fa-check"></i></button>';
              aksi += '<button class="btn btn-sm btn-outline-secondary" title="Hapus" onclick="dpHapus' + RND + '(' + r.id + ',\'' + esc(String(r.kode).replace(/'/g,"")) + '\')"><i class="fas fa-trash"></i></button>';
            }
            html += '<tr>'
              + '<td class="fw-bold">' + esc(r.kode) + '</td>'
              + '<td>' + esc(r.tanggal || "-") + '</td>'
              + '<td>' + esc(r.penyedia || "-") + '</td>'
              + '<td class="text-end">' + rp(r.nilai) + '</td>'
              + '<td><span class="badge bg-' + (disetujui ? "success" : "warning") + '">' + esc(st) + '</span></td>'
              + '<td class="text-center">' + aksi + '</td>'
              + '</tr>';
          }
          tbody.innerHTML = html;
        }
        el("dpInfo").textContent = "Halaman " + halaman + " dari " + totalHal + " - total " + (d.total || rows.length) + " dokumen";
        el("dpPrev").disabled = halaman <= 1;
        el("dpNext").disabled = halaman >= totalHal;
      })
      .catch(function(){ tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger py-4">Kesalahan koneksi.</td></tr>'; });
  };
  window["dpHal" + RND] = function(delta){
    var baru = halaman + delta;
    if (baru < 1 || baru > totalHal) return;
    window["dpMuat" + RND](baru);
  };

  function total(){
    var t = 0;
    for (var i=0;i<baris.length;i++) if (baris[i].pilih) t += angka(baris[i].dibayar);
    return t;
  }
  function render(){
    var tb = el("dpBarisTbody");
    if (!baris.length){
      tb.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-3">Tidak ada tagihan terbuka untuk penyedia ini.</td></tr>';
      el("dpTotal").textContent = rp(0);
      return;
    }
    var h = "";
    for (var i=0;i<baris.length;i++){
      var b = baris[i];
      var info = (b.jatuhTempo ? (" - jatuh tempo " + esc(b.jatuhTempo)) : "")
               + (b.sudahDibayar > 0 ? (" - terbayar " + rp(b.sudahDibayar)) : "");
      h += '<tr>'
        + '<td><input type="checkbox" class="form-check-input"' + (b.pilih ? " checked" : "")
        + (terkunciKini ? " disabled" : "") + ' onchange="dpPilih' + RND + '(' + i + ',this.checked)"></td>'
        + '<td><div class="fw-bold">' + esc(b.po) + ' - ' + esc(b.termin) + '</div>'
        + '<div class="small text-muted">' + info + '</div></td>'
        + '<td class="text-end">' + rp(b.nilaiTagih) + '</td>'
        + '<td class="text-end text-info">' + rp(b.sisa) + '</td>'
        + '<td><input type="number" class="form-control form-control-sm" value="' + angka(b.dibayar) + '"'
        + ((terkunciKini || !b.pilih) ? " disabled" : "")
        + ' oninput="dpUbah' + RND + '(' + i + ',this.value)"></td>'
        + '</tr>';
    }
    tb.innerHTML = h;
    el("dpTotal").textContent = rp(total());
  }
  window["dpPilih" + RND] = function(i, nilai){
    baris[i].pilih = nilai;
    if (nilai && angka(baris[i].dibayar) <= 0) baris[i].dibayar = baris[i].sisa;
    render();
  };
  window["dpUbah" + RND] = function(i, nilai){
    baris[i].dibayar = angka(nilai);
    el("dpTotal").textContent = rp(total());
  };

  function bukaForm(terkunci, judul, pesanKunci){
    terkunciKini = terkunci;
    el("dpModalJudul").textContent = judul;
    var kunciBox = el("dpKunciInfo");
    if (terkunci){
      kunciBox.textContent = pesanKunci;
      kunciBox.classList.remove("d-none");
      el("dpSimpan").classList.add("d-none");
    } else {
      kunciBox.classList.add("d-none");
      el("dpSimpan").classList.remove("d-none");
    }
    el("dpKeterangan").disabled = terkunci;
    render();
    new bootstrap.Modal(document.getElementById("dpModal" + RND)).show();
  }

  /** Muat tagihan terbuka vendor, lalu gabungkan dengan baris dokumen bila menyunting. */
  function muatTagihan(kecualiId, barisDokumen, terkunci, judul, pesanKunci){
    var req = {action:"pengadaan_bayar_tagihan_terbuka", penyedia_id: vendorId};
    if (kecualiId) req.kecuali_bayar_id = kecualiId;
    api(req).then(function(d){
      if (d.status !== "00" && d.status !== "success"){ pesan(d.description || "Gagal memuat tagihan.", false); return; }
      var sudah = {};
      baris = [];
      for (var i=0;i<barisDokumen.length;i++){
        var x = barisDokumen[i];
        sudah[x.po_id + "|" + (x.termin_key || "")] = true;
        baris.push({ po_id:x.po_id, po:x.po, termin_key:x.termin_key || "", termin:x.termin,
                     jatuhTempo:"", nilaiTagih:angka(x.nilaiTagih), sudahDibayar:angka(x.sudahDibayar),
                     sisa:angka(x.sisa), dibayar:angka(x.dibayar), pilih:true });
      }
      var rows = d.data || [];
      for (var j=0;j<rows.length;j++){
        var t = rows[j];
        if (sudah[t.po_id + "|" + (t.termin_key || "")]) continue;
        baris.push({ po_id:t.po_id, po:t.po, termin_key:t.termin_key || "", termin:t.termin,
                     jatuhTempo:t.jatuhTempo || "", nilaiTagih:angka(t.nilaiTagih),
                     sudahDibayar:angka(t.sudahDibayar), sisa:angka(t.sisa), dibayar:0, pilih:false });
      }
      if (!baris.length){ pesan(d.catatan || "Tidak ada tagihan terbuka untuk penyedia ini.", false); return; }
      bukaForm(terkunci, judul, pesanKunci);
    });
  }

  window["dpBaru" + RND] = function(){
    el("dpVendorCari").value = "";
    el("dpVendorHasil").innerHTML = "";
    new bootstrap.Modal(document.getElementById("dpVendorModal" + RND)).show();
    window["dpVendorMuat" + RND]();
  };
  window["dpVendorMuat" + RND] = function(){
    api({action:"pengadaan_penyedia_cari", keyword: el("dpVendorCari").value.trim(), limit:50}).then(function(d){
      var rows = d.data || [];
      if (!rows.length){ el("dpVendorHasil").innerHTML = '<div class="text-muted small py-2">Tidak ada penyedia ditemukan.</div>'; return; }
      var h = "";
      for (var i=0;i<rows.length;i++){
        var p = rows[i];
        h += '<a href="javascript:void(0)" class="list-group-item list-group-item-action"'
           + ' onclick="dpPilihVendor' + RND + '(' + p.id + ',\'' + esc(String(p.nama).replace(/'/g,"")) + '\')">'
           + '<div class="fw-bold">' + esc(p.nama) + '</div>'
           + '<div class="small text-muted">' + esc(p.kode || "") + '</div></a>';
      }
      el("dpVendorHasil").innerHTML = h;
    });
  };
  window["dpPilihVendor" + RND] = function(id, nama){
    vendorId = id; vendorNama = nama;
    bootstrap.Modal.getInstance(document.getElementById("dpVendorModal" + RND)).hide();
    dpAktif = null;
    el("dpKeterangan").value = "";
    el("dpJudul").value = "";
    el("dpTglRealisasi").value = "";
    muatCaraBayar(null);
    muatTagihan(null, [], false, "Bayar " + nama, "");
  };

  // Pilihan Cara Transfer -- hanya yang aktif dan sudah punya akun, sama dengan
  // penyaring pada form Proses Transfer versi ZKoss.
  var caraBayarOpsi = [], caraBayarBawaan = null;
  function muatCaraBayar(pilih){
    api({action:"pengadaan_cara_bayar_opsi"}).then(function(d){
      caraBayarOpsi = d.data || [];
      caraBayarBawaan = d.bawaan_id || null;
      var sel = el("dpCaraBayar");
      var h = caraBayarOpsi.length ? "" : '<option value="">(belum ada Cara Transfer aktif)</option>';
      for (var i=0;i<caraBayarOpsi.length;i++){
        var c = caraBayarOpsi[i];
        h += '<option value="' + c.id + '">' + esc(c.nama || "")
           + (c.akun ? " - " + esc(c.akun) : "") + '</option>';
      }
      sel.innerHTML = h;
      var terpilih = pilih || caraBayarBawaan;
      if (terpilih) sel.value = String(terpilih);
    });
  }

  window["dpForm" + RND] = function(id){
    api({action:"pengadaan_bayar_detail", id:id}).then(function(d){
      if (d.status !== "00" && d.status !== "success"){ pesan(d.description || "Gagal memuat detail.", false); return; }
      dpAktif = d.header || {};
      var st = dpAktif.status || "DRAFT";
      var terkunci = st !== "DRAFT";
      vendorId = dpAktif.penyedia_id || null;
      vendorNama = dpAktif.penyedia || "";
      el("dpKeterangan").value = dpAktif.keterangan || "";
      el("dpJudul").value = dpAktif.judul || "";
      el("dpTglRealisasi").value = dpAktif.tanggalRealisasi || "";
      muatCaraBayar(dpAktif.cara_bayar_id || null);
      muatTagihan(dpAktif.id, d.detail || [], terkunci,
        "Pembayaran " + (dpAktif.kode || "") + " - " + st,
        "Pembayaran yang sudah disetujui tidak dapat diubah. Batalkan persetujuannya terlebih dahulu bila perlu dikoreksi.");
    });
  };

  window["dpSimpan" + RND] = function(){
    var detail = [];
    for (var i=0;i<baris.length;i++){
      var b = baris[i];
      if (!b.pilih) continue;
      if (angka(b.dibayar) <= 0){ pesan("Nilai bayar " + b.po + " harus lebih besar dari nol.", false); return; }
      if (angka(b.dibayar) > b.sisa + 1){
        pesan("Nilai bayar " + b.po + " melebihi sisa tagihannya (" + rp(b.sisa) + ").", false);
        return;
      }
      detail.push({ po_id: b.po_id, termin_key: b.termin_key, dibayar: angka(b.dibayar) });
    }
    if (!detail.length){ pesan("Centang minimal satu tagihan untuk dibayar.", false); return; }
    var caraBayarId = el("dpCaraBayar").value;
    // Cara transfer tidak menghalangi penyimpanan draf; ia dituntut saat pembayaran
    // DISETUJUI, karena di titik itulah jurnal dibentuk.
    if (!caraBayarId && caraBayarOpsi.length){
      pesan("Cara transfer belum dipilih. Draf tetap tersimpan, tetapi harus diisi sebelum pembayaran disetujui.", true);
    }
    var payload = { action:"pengadaan_bayar_simpan", penyedia_id: vendorId,
                    judul: el("dpJudul").value.trim(),
                    keterangan: el("dpKeterangan").value.trim(), detail: detail };
    if (caraBayarId) payload.cara_bayar_id = caraBayarId;
    var tglReal = el("dpTglRealisasi").value.trim();
    if (tglReal) payload.tanggalRealisasi = tglReal;
    if (dpAktif && dpAktif.id) payload.id = dpAktif.id;
    api(payload).then(function(d){
      var ok = d.status === "00" || d.status === "success";
      pesan(ok ? ("Pembayaran tersimpan: " + (d.kode || "")) : (d.description || "Gagal menyimpan."), ok);
      if (ok){
        bootstrap.Modal.getInstance(document.getElementById("dpModal" + RND)).hide();
        window["dpMuat" + RND](halaman);
      }
    });
  };

  // Saat menyetujui, penyetuju memilih apakah pembayaran masuk antrean transfer
  // bank. Pembayaran tunai tidak perlu masuk antrean pencairan, jadi ditanyakan.
  window["dpPutusan" + RND] = function(id, keputusan){
    var payload = {action:"pengadaan_bayar_putusan", id:id, keputusan:keputusan};
    if (keputusan === "SETUJUI"){
      payload.ajukanTransfer = window.confirm(
        "Ajukan transfer bank untuk pembayaran ini? OK = masuk antrean pencairan keuangan. "
        + "Batal = disetujui tanpa pengajuan transfer, misalnya bila dibayar tunai.");
    }
    api(payload).then(function(d){
      var ok = d.status === "00" || d.status === "success";
      var catatan = "";
      if (ok && (d.transferDibuat || 0) > 0) catatan = " - " + d.transferDibuat + " pengajuan transfer dibuat";
      if (ok && (d.transferDitarik || 0) > 0) catatan = " - " + d.transferDitarik + " pengajuan transfer ditarik";
      pesan(ok ? ("Keputusan tersimpan: " + (d.statusDokumen || keputusan) + catatan)
               : (d.description || "Gagal menyimpan keputusan."), ok);
      if (ok) window["dpMuat" + RND](halaman);
    });
  };
  window["dpHapus" + RND] = function(id, kode){
    if (!window.confirm("Hapus dokumen pembayaran " + kode + "? Hanya dokumen DRAFT yang dapat dihapus.")) return;
    api({action:"pengadaan_bayar_hapus", id:id}).then(function(d){
      var ok = d.status === "00" || d.status === "success";
      pesan(ok ? "Dokumen dihapus." : (d.description || "Gagal menghapus."), ok);
      if (ok) window["dpMuat" + RND](halaman);
    });
  };

  window["dpMuat" + RND](1);
})();
</script>
