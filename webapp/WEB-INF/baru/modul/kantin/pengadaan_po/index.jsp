<%@ page isELIgnored="true" %>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// Halaman "Pemesanan Pembelian (PO)" -- modul Pengadaan POS versi JSP.
// Seluruh aturan bisnis (penomoran, hitung nilai, keseimbangan jadwal termin,
// pagar ubah/hapus, keputusan) berada di server PengadaanPosApiHelper yang dipakai
// bersama Desktop/Android, sehingga ketiga kanal berperilaku identik.
//
// Halaman ini memakai isELIgnored="true": seluruh nilai dinamis dirangkai lewat
// penggabungan string JavaScript, tidak ada sintaks ekspresi EL yang dapat dievaluasi
// server dan mengosongkan isi skrip.
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
      <h4 class="fw-bold mb-0"><%=Common.getBahasaConfig("Pemesanan Pembelian (PO)")%></h4>
      <div class="text-muted small"><%=Common.getBahasaConfig("Pesan barang ke penyedia, termasuk pembayaran bertermin")%></div>
    </div>
    <div>
      <button class="btn btn-outline-primary fw-bold rounded-pill px-3 me-2" onclick="poDariPr<%=rnd%>()">
        <i class="fas fa-clipboard-check me-2"></i><%=Common.getBahasaConfig("Dari PR")%>
      </button>
      <a class="btn btn-outline-secondary fw-bold rounded-pill px-3 me-2" href="<%=Common.ROOT%>/baru?p=kantin&s=pengadaan_bulk&jenis=po">
        <i class="fas fa-table me-2"></i><%=Common.getBahasaConfig("Bulk Entry")%>
      </a>
      <button class="btn btn-primary fw-bold rounded-pill px-4" onclick="poForm<%=rnd%>(null)">
        <i class="fas fa-plus me-2"></i><%=Common.getBahasaConfig("Buat PO")%>
      </button>
    </div>
  </div>

  <div class="card border-0 shadow-sm mb-3">
    <div class="card-body py-3">
      <div class="row g-2 align-items-end">
        <div class="col-md-5">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Cari kode / keterangan / no. invoice")%></label>
          <input type="text" id="poCari<%=rnd%>" class="form-control" onkeydown="if(event.key==='Enter')poMuat<%=rnd%>(1)">
        </div>
        <div class="col-md-3">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Status")%></label>
          <select id="poStatus<%=rnd%>" class="form-select" onchange="poMuat<%=rnd%>(1)">
            <option value=""><%=Common.getBahasaConfig("Semua status")%></option>
            <option value="DRAFT">DRAFT</option>
            <option value="DISETUJUI">DISETUJUI</option>
            <option value="DITOLAK">DITOLAK</option>
            <option value="LUNAS">LUNAS</option>
          </select>
        </div>
        <div class="col-md-2">
          <button class="btn btn-outline-secondary w-100" onclick="poMuat<%=rnd%>(1)">
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
            <th class="text-end"><%=Common.getBahasaConfig("Sisa")%></th>
            <th><%=Common.getBahasaConfig("Status")%></th>
            <th class="text-center"><%=Common.getBahasaConfig("Aksi")%></th>
          </tr>
        </thead>
        <tbody id="poTbody<%=rnd%>">
          <tr><td colspan="7" class="text-center text-muted py-4"><%=Common.getBahasaConfig("Memuat data...")%></td></tr>
        </tbody>
      </table>
    </div>
    <div class="card-footer bg-white d-flex justify-content-between align-items-center">
      <span class="small text-muted" id="poInfo<%=rnd%>"></span>
      <div class="btn-group">
        <button class="btn btn-sm btn-outline-secondary" id="poPrev<%=rnd%>" onclick="poHal<%=rnd%>(-1)">&laquo;</button>
        <button class="btn btn-sm btn-outline-secondary" id="poNext<%=rnd%>" onclick="poHal<%=rnd%>(1)">&raquo;</button>
      </div>
    </div>
  </div>
</div>

<div class="modal fade" id="poModal<%=rnd%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-xl modal-dialog-scrollable">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="poModalJudul<%=rnd%>"><%=Common.getBahasaConfig("Buat Pemesanan Pembelian")%></h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <div id="poKunciInfo<%=rnd%>" class="alert alert-warning small d-none"></div>
        <div class="row g-2">
          <div class="col-md-6">
            <label class="form-label small mb-1"><%=Common.getBahasaConfig("Penyedia / Vendor")%> *</label>
            <div class="input-group">
              <input type="text" id="poPenyediaNama<%=rnd%>" class="form-control" readonly
                     placeholder="<%=Common.getBahasaConfig("Belum dipilih")%>">
              <button class="btn btn-outline-secondary" id="poPilihPenyedia<%=rnd%>" onclick="poCariPenyedia<%=rnd%>()">
                <i class="fas fa-search"></i>
              </button>
            </div>
          </div>
          <div class="col-md-3">
            <label class="form-label small mb-1"><%=Common.getBahasaConfig("No. invoice / referensi")%></label>
            <input type="text" id="poKodeInvoice<%=rnd%>" class="form-control">
          </div>
          <div class="col-md-3">
            <label class="form-label small mb-1"><%=Common.getBahasaConfig("Kirim paling lambat")%></label>
            <input type="date" id="poKirim<%=rnd%>" class="form-control">
          </div>
        </div>
        <div class="mb-2 mt-2">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Keterangan pesanan")%></label>
          <textarea id="poKeterangan<%=rnd%>" class="form-control" rows="2"></textarea>
        </div>
        <div class="mb-2">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Catatan kesepakatan dengan vendor")%></label>
          <textarea id="poCatatan<%=rnd%>" class="form-control" rows="2"></textarea>
        </div>

        <div class="d-flex justify-content-between align-items-center mt-3 mb-1">
          <h6 class="fw-bold mb-0"><%=Common.getBahasaConfig("Barang yang Dipesan")%></h6>
          <button class="btn btn-sm btn-outline-primary" id="poTambahBaris<%=rnd%>" onclick="poCariBarang<%=rnd%>()">
            <i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah Barang")%>
          </button>
        </div>
        <div class="table-responsive">
          <table class="table table-sm align-middle">
            <thead class="table-light">
              <tr>
                <th><%=Common.getBahasaConfig("Barang")%></th>
                <th style="width:110px"><%=Common.getBahasaConfig("Jumlah")%></th>
                <th style="width:150px"><%=Common.getBahasaConfig("Harga Beli")%></th>
                <th class="text-end" style="width:140px"><%=Common.getBahasaConfig("Subtotal")%></th>
                <th style="width:50px"></th>
              </tr>
            </thead>
            <tbody id="poBarisTbody<%=rnd%>"></tbody>
            <tfoot>
              <tr class="fw-bold">
                <td colspan="3" class="text-end"><%=Common.getBahasaConfig("Total Nilai PO")%></td>
                <td class="text-end" id="poTotal<%=rnd%>">0</td>
                <td></td>
              </tr>
            </tfoot>
          </table>
        </div>

        <div class="row g-2 align-items-end" id="poBarisDp<%=rnd%>">
          <div class="col-md-3">
            <label class="form-label small mb-1"><%=Common.getBahasaConfig("Uang muka (DP)")%></label>
            <input type="number" id="poDp<%=rnd%>" class="form-control" value="0">
          </div>
        </div>

        <hr class="my-3">
        <div class="form-check form-switch mb-2">
          <input class="form-check-input" type="checkbox" id="poBertermin<%=rnd%>" onchange="poUbahTermin<%=rnd%>()">
          <label class="form-check-label fw-bold" for="poBertermin<%=rnd%>">
            <%=Common.getBahasaConfig("Pembayaran bertermin")%>
            <span class="text-muted fw-normal small">(<%=Common.getBahasaConfig("bayar bertahap sesuai jadwal")%>)</span>
          </label>
        </div>
        <div id="poPanelTermin<%=rnd%>" class="d-none">
          <div class="d-flex justify-content-between align-items-center mb-1">
            <h6 class="fw-bold mb-0"><%=Common.getBahasaConfig("Jadwal Termin")%></h6>
            <div>
              <button class="btn btn-sm btn-outline-secondary me-1" id="poBagiRata<%=rnd%>" onclick="poBagiRata<%=rnd%>()">
                <i class="fas fa-equals me-1"></i><%=Common.getBahasaConfig("Bagi Rata")%>
              </button>
              <button class="btn btn-sm btn-outline-primary" id="poTambahTermin<%=rnd%>" onclick="poTambahTermin<%=rnd%>()">
                <i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah Termin")%>
              </button>
            </div>
          </div>
          <div class="table-responsive">
            <table class="table table-sm align-middle">
              <thead class="table-light">
                <tr>
                  <th><%=Common.getBahasaConfig("Nama termin")%></th>
                  <th style="width:160px"><%=Common.getBahasaConfig("Nilai tagih")%></th>
                  <th style="width:170px"><%=Common.getBahasaConfig("Jatuh tempo")%></th>
                  <th style="width:150px"><%=Common.getBahasaConfig("Terbayar")%></th>
                  <th style="width:50px"></th>
                </tr>
              </thead>
              <tbody id="poTerminTbody<%=rnd%>"></tbody>
            </table>
          </div>
          <div id="poTerminInfo<%=rnd%>" class="alert alert-warning small mb-1"></div>
          <div class="small text-muted fst-italic">
            <%=Common.getBahasaConfig("PO bertermin tidak memakai uang muka terpisah -- tuliskan uang muka sebagai termin pertama.")%>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Tutup")%></button>
        <button type="button" class="btn btn-primary" id="poSimpan<%=rnd%>" onclick="poSimpan<%=rnd%>()"><%=Common.getBahasaConfig("Simpan")%></button>
      </div>
    </div>
  </div>
</div>

<div class="modal fade" id="poBarangModal<%=rnd%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title"><%=Common.getBahasaConfig("Pilih Barang")%></h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <input type="text" id="poBarangCari<%=rnd%>" class="form-control mb-2"
               placeholder="<%=Common.getBahasaConfig("Cari kode / nama barang")%>"
               onkeydown="if(event.key==='Enter')poBarangMuat<%=rnd%>()">
        <div id="poBarangHasil<%=rnd%>" class="list-group"></div>
      </div>
    </div>
  </div>
</div>

<div class="modal fade" id="poPenyediaModal<%=rnd%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title"><%=Common.getBahasaConfig("Pilih Penyedia / Vendor")%></h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <input type="text" id="poPenyediaCari<%=rnd%>" class="form-control mb-2"
               placeholder="<%=Common.getBahasaConfig("Cari kode / nama penyedia")%>"
               onkeydown="if(event.key==='Enter')poPenyediaMuat<%=rnd%>()">
        <div id="poPenyediaHasil<%=rnd%>" class="list-group"></div>
      </div>
    </div>
  </div>
</div>

<div class="modal fade" id="poPrModal<%=rnd%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title"><%=Common.getBahasaConfig("Pilih Permintaan Pembelian")%></h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <div class="small text-muted mb-2">
          <%=Common.getBahasaConfig("Hanya PR berstatus DISETUJUI yang dapat dijadikan pesanan.")%>
        </div>
        <input type="text" id="poPrCari<%=rnd%>" class="form-control mb-2"
               placeholder="<%=Common.getBahasaConfig("Cari kode / keterangan PR")%>"
               onkeydown="if(event.key==='Enter')poPrMuat<%=rnd%>()">
        <div id="poPrHasil<%=rnd%>" class="list-group"></div>
      </div>
    </div>
  </div>
</div>

<script>
(function(){
  var RND = "<%=rnd%>";
  var DATA_URL = "<%=Common.ROOT%>/Data";
  var halaman = 1, totalHal = 1, poAktif = null, baris = [], termin = [];
  var penyediaId = null, terkunciKini = false;

  function el(id){ return document.getElementById(id + RND); }
  function esc(s){ return (s==null?"":String(s)).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;"); }
  function rp(n){ return "Rp " + (Number(n)||0).toLocaleString("id-ID"); }
  function angka(v){ v = String(v==null?"":v).replace(/[^0-9.]/g,""); return Number(v)||0; }

  // Tanggal pada dokumen pengadaan memakai pola hh-bb-tttt supaya satu dokumen
  // tetap terbaca layar ZKoss; input HTML memakai tttt-bb-hh, jadi dikonversi.
  function keTampilan(iso){
    if (!iso) return "";
    var p = String(iso).split("-");
    return p.length === 3 ? (p[2] + "-" + p[1] + "-" + p[0]) : "";
  }
  function keIsoTgl(tampilan){
    if (!tampilan) return "";
    var p = String(tampilan).split("-");
    return p.length === 3 ? (p[2] + "-" + p[1] + "-" + p[0]) : "";
  }

  function api(payload){
    return fetch(DATA_URL, {method:"POST", headers:{"Content-Type":"application/json"},
                            body: JSON.stringify(payload)})
      .then(function(r){ return r.json(); });
  }
  function pesan(teks, sukses){
    if (typeof tampilkanToast === "function") tampilkanToast(teks, sukses ? "bg-success text-white" : "bg-danger text-white");
    else alert(teks);
  }
  function warnaStatus(s){
    if (s === "DISETUJUI") return "success";
    if (s === "LUNAS") return "info";
    if (s === "DITOLAK") return "danger";
    return "warning";
  }

  window["poMuat" + RND] = function(hal){
    halaman = hal || halaman;
    var tbody = el("poTbody");
    tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">Memuat data...</td></tr>';
    api({action:"pengadaan_po_daftar", cari: el("poCari").value.trim(),
         status: el("poStatus").value, page: halaman, pageSize: 15})
      .then(function(d){
        if (d.status !== "00" && d.status !== "success") {
          tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger py-4">' + esc(d.description || "Gagal memuat data.") + '</td></tr>';
          return;
        }
        var rows = d.data || [];
        totalHal = Math.max(1, Math.ceil((d.total || rows.length) / 15));
        if (!rows.length) {
          tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">Belum ada Pemesanan Pembelian.</td></tr>';
        } else {
          var html = "";
          for (var i=0;i<rows.length;i++){
            var r = rows[i], st = r.status || "DRAFT";
            var tandaTermin = r.byTermin
              ? '<div class="small text-info">' + (r.jumlahTermin || 0) + ' termin</div>' : '';
            html += '<tr>'
              + '<td class="fw-bold">' + esc(r.kode) + tandaTermin + '</td>'
              + '<td>' + esc(r.tanggal || "-") + '</td>'
              + '<td>' + esc(r.penyedia || "-") + '</td>'
              + '<td class="text-end">' + rp(r.nilai) + '</td>'
              + '<td class="text-end">' + rp(r.sisa) + '</td>'
              + '<td><span class="badge bg-' + warnaStatus(st) + '">' + esc(st) + '</span></td>'
              + '<td class="text-center">' + aksiHtml(r, st) + '</td>'
              + '</tr>';
          }
          tbody.innerHTML = html;
        }
        el("poInfo").textContent = "Halaman " + halaman + " dari " + totalHal + " - total " + (d.total || rows.length) + " PO";
        el("poPrev").disabled = halaman <= 1;
        el("poNext").disabled = halaman >= totalHal;
      })
      .catch(function(){ tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger py-4">Kesalahan koneksi.</td></tr>'; });
  };

  function aksiHtml(r, st){
    var adaBayar = (Number(r.dibayar)||0) > 0;
    var h = '<button class="btn btn-sm btn-outline-primary me-1" title="Lihat / ubah" onclick="poForm' + RND + '(' + r.id + ')"><i class="fas fa-edit"></i></button>';
    if (st === "DRAFT") {
      h += '<button class="btn btn-sm btn-outline-success me-1" title="Setujui" onclick="poPutusan' + RND + '(' + r.id + ',\'SETUJUI\')"><i class="fas fa-check"></i></button>';
      h += '<button class="btn btn-sm btn-outline-danger me-1" title="Tolak" onclick="poPutusan' + RND + '(' + r.id + ',\'TOLAK\')"><i class="fas fa-times"></i></button>';
      if (!adaBayar) {
        h += '<button class="btn btn-sm btn-outline-secondary" title="Hapus" onclick="poHapus' + RND + '(' + r.id + ',\'' + esc(String(r.kode).replace(/'/g,"")) + '\')"><i class="fas fa-trash"></i></button>';
      }
    } else if ((st === "DISETUJUI" || st === "DITOLAK") && !adaBayar) {
      h += '<button class="btn btn-sm btn-outline-secondary" title="Batalkan keputusan" onclick="poPutusan' + RND + '(' + r.id + ',\'BATAL\')"><i class="fas fa-undo"></i></button>';
    }
    return h;
  }

  window["poHal" + RND] = function(delta){
    var baru = halaman + delta;
    if (baru < 1 || baru > totalHal) return;
    window["poMuat" + RND](baru);
  };

  // ---------- Baris barang ----------
  function totalPo(){
    var t = 0;
    for (var i=0;i<baris.length;i++) t += baris[i].jumlah * baris[i].harga;
    return t;
  }
  function hitungTotal(){
    el("poTotal").textContent = rp(totalPo());
    renderInfoTermin();
  }
  function renderBaris(terkunci){
    var tb = el("poBarisTbody");
    if (!baris.length){
      tb.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-3">Belum ada barang. Tambahkan minimal satu baris.</td></tr>';
      hitungTotal(); return;
    }
    var h = "";
    for (var i=0;i<baris.length;i++){
      var b = baris[i];
      var asal = b.pr_detail_id ? '<div class="small text-info">dari PR</div>' : '';
      h += '<tr>'
        + '<td>' + esc(b.nama) + asal + '</td>'
        + '<td><input type="number" class="form-control form-control-sm" value="' + b.jumlah + '"'
        + (terkunci ? ' disabled' : '') + ' oninput="poUbahBaris' + RND + '(' + i + ',\'jumlah\',this.value)"></td>'
        + '<td><input type="number" class="form-control form-control-sm" value="' + b.harga + '"'
        + (terkunci ? ' disabled' : '') + ' oninput="poUbahBaris' + RND + '(' + i + ',\'harga\',this.value)"></td>'
        + '<td class="text-end" id="poSub' + RND + i + '">' + rp(b.jumlah * b.harga) + '</td>'
        + '<td>' + (terkunci ? '' : '<button class="btn btn-sm btn-outline-danger" onclick="poHapusBaris' + RND + '(' + i + ')"><i class="fas fa-times"></i></button>') + '</td>'
        + '</tr>';
    }
    tb.innerHTML = h;
    hitungTotal();
  }
  window["poUbahBaris" + RND] = function(i, field, nilai){
    baris[i][field] = angka(nilai);
    var sel = document.getElementById("poSub" + RND + i);
    if (sel) sel.textContent = rp(baris[i].jumlah * baris[i].harga);
    hitungTotal();
  };
  window["poHapusBaris" + RND] = function(i){ baris.splice(i,1); renderBaris(false); };

  // ---------- Jadwal termin ----------
  function totalTermin(){
    var t = 0;
    for (var i=0;i<termin.length;i++) t += angka(termin[i].nilai);
    return t;
  }
  function renderInfoTermin(){
    if (!el("poBertermin").checked){ el("poTerminInfo").textContent = ""; return; }
    var selisih = totalTermin() - totalPo();
    var box = el("poTerminInfo");
    if (Math.abs(selisih) <= 1){
      box.className = "alert alert-success small mb-1";
      box.textContent = "Jadwal termin sudah menutup seluruh nilai PO (" + rp(totalTermin()) + ").";
    } else if (selisih > 0){
      box.className = "alert alert-warning small mb-1";
      box.textContent = "Jadwal termin KELEBIHAN " + rp(selisih) + " dari nilai PO. Kurangi salah satu termin.";
    } else {
      box.className = "alert alert-warning small mb-1";
      box.textContent = "Jadwal termin KURANG " + rp(-selisih) + " dari nilai PO. Tambah termin atau naikkan nilainya.";
    }
  }
  function renderTermin(terkunci){
    var tb = el("poTerminTbody");
    if (!termin.length){
      tb.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-3">Belum ada termin. Tekan "Tambah Termin" atau "Bagi Rata".</td></tr>';
      renderInfoTermin(); return;
    }
    var h = "";
    for (var i=0;i<termin.length;i++){
      var t = termin[i];
      var kunciBaris = terkunci || (Number(t.dibayar)||0) > 0;
      h += '<tr>'
        + '<td><input type="text" class="form-control form-control-sm" value="' + esc(t.nama) + '"'
        + (kunciBaris ? ' disabled' : '') + ' oninput="poUbahTerminBaris' + RND + '(' + i + ',\'nama\',this.value)"></td>'
        + '<td><input type="number" class="form-control form-control-sm" value="' + angka(t.nilai) + '"'
        + (kunciBaris ? ' disabled' : '') + ' oninput="poUbahTerminBaris' + RND + '(' + i + ',\'nilai\',this.value)"></td>'
        + '<td><input type="date" class="form-control form-control-sm" value="' + esc(keIsoTgl(t.tanggal)) + '"'
        + (kunciBaris ? ' disabled' : '') + ' onchange="poUbahTerminBaris' + RND + '(' + i + ',\'tanggalIso\',this.value)"></td>'
        + '<td class="small">' + ((Number(t.dibayar)||0) > 0 ? '<span class="text-info fw-bold">' + rp(t.dibayar) + '</span>' : '-') + '</td>'
        + '<td>' + (kunciBaris ? '' : '<button class="btn btn-sm btn-outline-danger" onclick="poHapusTermin' + RND + '(' + i + ')"><i class="fas fa-times"></i></button>') + '</td>'
        + '</tr>';
    }
    tb.innerHTML = h;
    renderInfoTermin();
  }
  window["poUbahTerminBaris" + RND] = function(i, field, nilai){
    if (field === "tanggalIso") termin[i].tanggal = keTampilan(nilai);
    else termin[i][field] = (field === "nilai") ? angka(nilai) : nilai;
    renderInfoTermin();
  };
  window["poHapusTermin" + RND] = function(i){ termin.splice(i,1); renderTermin(terkunciKini); };
  window["poTambahTermin" + RND] = function(){
    termin.push({ key:null, nama:"Termin " + (termin.length + 1), nilai:0, tanggal:"", dibayar:0 });
    renderTermin(terkunciKini);
  };
  // Bagi rata: pembulatan dibebankan ke termin terakhir supaya jumlahnya tepat
  // sama dengan nilai PO -- server menolak selisih lebih dari Rp 1.
  window["poBagiRata" + RND] = function(){
    if (!termin.length) window["poTambahTermin" + RND]();
    var n = termin.length, total = totalPo(), per = Math.floor(total / n);
    for (var i=0;i<n;i++){
      termin[i].nilai = (i === n - 1) ? (total - per * (n - 1)) : per;
      if (!String(termin[i].nama || "").trim()) termin[i].nama = "Termin " + (i + 1);
    }
    renderTermin(terkunciKini);
  };
  window["poUbahTermin" + RND] = function(){
    var aktif = el("poBertermin").checked;
    el("poPanelTermin").classList.toggle("d-none", !aktif);
    el("poBarisDp").classList.toggle("d-none", aktif);
    if (aktif && !termin.length) window["poBagiRata" + RND]();
    renderInfoTermin();
  };

  // ---------- Form PO ----------
  function bukaForm(terkunci, judul, pesanKunci){
    terkunciKini = terkunci;
    el("poModalJudul").textContent = judul;
    var kunciBox = el("poKunciInfo");
    if (terkunci){
      kunciBox.textContent = pesanKunci;
      kunciBox.classList.remove("d-none");
      el("poSimpan").classList.add("d-none");
      el("poTambahBaris").classList.add("d-none");
      el("poTambahTermin").classList.add("d-none");
      el("poBagiRata").classList.add("d-none");
      el("poPilihPenyedia").classList.add("d-none");
    } else {
      kunciBox.classList.add("d-none");
      el("poSimpan").classList.remove("d-none");
      el("poTambahBaris").classList.remove("d-none");
      el("poTambahTermin").classList.remove("d-none");
      el("poBagiRata").classList.remove("d-none");
      el("poPilihPenyedia").classList.remove("d-none");
    }
    var kolom = ["poKeterangan","poCatatan","poKodeInvoice","poKirim","poDp","poBertermin"];
    for (var i=0;i<kolom.length;i++) el(kolom[i]).disabled = terkunci;
    renderBaris(terkunci);
    renderTermin(terkunci);
    window["poUbahTermin" + RND]();
    new bootstrap.Modal(document.getElementById("poModal" + RND)).show();
  }

  function kosongkanForm(){
    poAktif = null; baris = []; termin = []; penyediaId = null;
    el("poPenyediaNama").value = "";
    el("poKeterangan").value = "";
    el("poCatatan").value = "";
    el("poKodeInvoice").value = "";
    el("poKirim").value = "";
    el("poDp").value = "0";
    el("poBertermin").checked = false;
  }

  window["poForm" + RND] = function(id){
    kosongkanForm();
    if (!id){ bukaForm(false, "Buat Pemesanan Pembelian", ""); return; }
    api({action:"pengadaan_po_detail", id:id}).then(function(d){
      if (d.status !== "00" && d.status !== "success"){ pesan(d.description || "Gagal memuat detail.", false); return; }
      poAktif = d.header || {};
      var st = poAktif.status || "DRAFT";
      var sudahDibayar = Number(poAktif.dibayar) || 0;
      // Pagar yang sama dengan server: dokumen yang sudah diputus atau sudah
      // menerima pembayaran tidak dapat disunting.
      var terkunci = st !== "DRAFT" || sudahDibayar > 0;
      penyediaId = poAktif.penyedia_id || null;
      el("poPenyediaNama").value = poAktif.penyedia || "";
      el("poKeterangan").value = poAktif.keterangan || "";
      el("poCatatan").value = poAktif.catatanKesepakatan || "";
      el("poKodeInvoice").value = poAktif.kodeInvoice || "";
      el("poKirim").value = keIsoTgl(poAktif.pengirimanPalingLambat || "");
      el("poDp").value = Number(poAktif.dp) || 0;
      el("poBertermin").checked = poAktif.byTermin === true;
      baris = (d.detail || []).map(function(x){
        return { barang_id: x.master_asset_id, nama: x.barang, jumlah: angka(x.jumlah),
                 harga: angka(x.hargaBeli), pr_detail_id: x.pr_detail_id || null };
      });
      termin = (d.termin || []).map(function(x){
        return { key: x.key || null, nama: x.nama || "", nilai: angka(x.penagihan),
                 tanggal: x.tanggalD || "", dibayar: Number(x.dibayar) || 0 };
      });
      var pesanKunci = sudahDibayar > 0
        ? ("PO ini sudah menerima pembayaran " + rp(sudahDibayar) + " sehingga tidak dapat diubah.")
        : ("PO berstatus " + st + " tidak dapat diubah. Batalkan keputusannya terlebih dahulu bila memang perlu dikoreksi.");
      bukaForm(terkunci, "PO " + (poAktif.kode || "") + "  -  " + st, pesanKunci);
    });
  };

  window["poSimpan" + RND] = function(){
    if (!penyediaId){ pesan("Pilih penyedia/vendor terlebih dahulu.", false); return; }
    if (!baris.length){ pesan("Tambahkan minimal satu baris barang.", false); return; }
    var bertermin = el("poBertermin").checked;
    if (bertermin){
      if (!termin.length){ pesan("Tambahkan minimal satu baris termin.", false); return; }
      for (var i=0;i<termin.length;i++){
        if (angka(termin[i].nilai) <= 0){ pesan("Setiap termin harus bernilai lebih dari nol.", false); return; }
      }
      var selisih = totalTermin() - totalPo();
      if (Math.abs(selisih) > 1){
        pesan("Jadwal termin belum menutup nilai PO (selisih " + rp(Math.abs(selisih))
              + "). Tekan \"Bagi Rata\" atau sesuaikan nilainya.", false);
        return;
      }
    }
    var payload = {
      action: "pengadaan_po_simpan",
      penyedia_id: penyediaId,
      keterangan: el("poKeterangan").value.trim(),
      kodeInvoice: el("poKodeInvoice").value.trim(),
      catatanKesepakatan: el("poCatatan").value.trim(),
      pengirimanPalingLambat: keTampilan(el("poKirim").value),
      dp: bertermin ? 0 : angka(el("poDp").value),
      byTermin: bertermin,
      detail: baris.map(function(b){
        var o = { master_asset_id: b.barang_id, jumlah: b.jumlah, hargaBeli: b.harga };
        if (b.pr_detail_id) o.pr_detail_id = b.pr_detail_id;
        return o;
      })
    };
    if (bertermin){
      payload.termin = termin.map(function(t){
        var o = { nama: t.nama, penagihan: angka(t.nilai), tanggalD: t.tanggal || "" };
        if (t.key) o.key = t.key;
        return o;
      });
    }
    if (poAktif && poAktif.id) payload.id = poAktif.id;
    api(payload).then(function(d){
      var ok = d.status === "00" || d.status === "success";
      pesan(ok ? ("PO tersimpan: " + (d.kode || "")) : (d.description || "Gagal menyimpan."), ok);
      if (ok){
        bootstrap.Modal.getInstance(document.getElementById("poModal" + RND)).hide();
        window["poMuat" + RND](halaman);
      }
    });
  };

  // ---------- Keputusan & hapus ----------
  window["poPutusan" + RND] = function(id, keputusan){
    var alasan = "";
    if (keputusan === "TOLAK"){
      alasan = window.prompt("Alasan penolakan (minimal 5 karakter, dibaca pembuat PO):", "");
      if (alasan === null) return;
      alasan = alasan.trim();
      if (alasan.length < 5){ pesan("Alasan penolakan minimal 5 karakter.", false); return; }
    }
    api({action:"pengadaan_po_putusan", id:id, keputusan:keputusan, alasan:alasan}).then(function(d){
      var ok = d.status === "00" || d.status === "success";
      pesan(ok ? ("Keputusan tersimpan: " + (d.statusDokumen || keputusan)) : (d.description || "Gagal menyimpan keputusan."), ok);
      if (ok) window["poMuat" + RND](halaman);
    });
  };

  window["poHapus" + RND] = function(id, kode){
    if (!window.confirm("Hapus PO " + kode + "? Hanya PO berstatus DRAFT dan belum menerima pembayaran yang dapat dihapus.")) return;
    api({action:"pengadaan_po_hapus", id:id}).then(function(d){
      var ok = d.status === "00" || d.status === "success";
      pesan(ok ? "PO dihapus." : (d.description || "Gagal menghapus."), ok);
      if (ok) window["poMuat" + RND](halaman);
    });
  };

  // ---------- Pemilih barang ----------
  window["poCariBarang" + RND] = function(){
    el("poBarangCari").value = "";
    el("poBarangHasil").innerHTML = "";
    new bootstrap.Modal(document.getElementById("poBarangModal" + RND)).show();
  };
  window["poBarangMuat" + RND] = function(){
    api({action:"pengadaan_barang_cari", keyword: el("poBarangCari").value.trim(), limit:50}).then(function(d){
      var rows = d.data || [];
      if (!rows.length){ el("poBarangHasil").innerHTML = '<div class="text-muted small py-2">Tidak ada barang ditemukan.</div>'; return; }
      var h = "";
      for (var i=0;i<rows.length;i++){
        var p = rows[i];
        h += '<a href="javascript:void(0)" class="list-group-item list-group-item-action"'
           + ' onclick="poPilihBarang' + RND + '(' + p.id + ',\'' + esc(String(p.nama).replace(/'/g,"")) + '\')">'
           + '<div class="fw-bold">' + esc(p.nama) + '</div>'
           + '<div class="small text-muted">' + esc(p.kode || "") + ' ' + esc(p.merk || "") + ' ' + esc(p.satuan || "") + '</div></a>';
      }
      el("poBarangHasil").innerHTML = h;
    });
  };
  window["poPilihBarang" + RND] = function(id, nama){
    baris.push({ barang_id:id, nama:nama, jumlah:1, harga:0, pr_detail_id:null });
    renderBaris(false);
    bootstrap.Modal.getInstance(document.getElementById("poBarangModal" + RND)).hide();
  };

  // ---------- Pemilih penyedia ----------
  window["poCariPenyedia" + RND] = function(){
    el("poPenyediaCari").value = "";
    el("poPenyediaHasil").innerHTML = "";
    new bootstrap.Modal(document.getElementById("poPenyediaModal" + RND)).show();
    window["poPenyediaMuat" + RND]();
  };
  window["poPenyediaMuat" + RND] = function(){
    api({action:"pengadaan_penyedia_cari", keyword: el("poPenyediaCari").value.trim(), limit:50}).then(function(d){
      var rows = d.data || [];
      if (!rows.length){ el("poPenyediaHasil").innerHTML = '<div class="text-muted small py-2">Tidak ada penyedia ditemukan.</div>'; return; }
      var h = "";
      for (var i=0;i<rows.length;i++){
        var p = rows[i];
        h += '<a href="javascript:void(0)" class="list-group-item list-group-item-action"'
           + ' onclick="poPilihPenyedia' + RND + '(' + p.id + ',\'' + esc(String(p.nama).replace(/'/g,"")) + '\')">'
           + '<div class="fw-bold">' + esc(p.nama) + '</div>'
           + '<div class="small text-muted">' + esc(p.kode || "") + ' ' + esc(p.alamat || "") + '</div></a>';
      }
      el("poPenyediaHasil").innerHTML = h;
    });
  };
  window["poPilihPenyedia" + RND] = function(id, nama){
    penyediaId = id;
    el("poPenyediaNama").value = nama;
    bootstrap.Modal.getInstance(document.getElementById("poPenyediaModal" + RND)).hide();
  };

  // ---------- Buat PO dari PR ----------
  window["poDariPr" + RND] = function(){
    el("poPrCari").value = "";
    el("poPrHasil").innerHTML = "";
    new bootstrap.Modal(document.getElementById("poPrModal" + RND)).show();
    window["poPrMuat" + RND]();
  };
  window["poPrMuat" + RND] = function(){
    api({action:"pengadaan_pr_daftar", status:"DISETUJUI", cari: el("poPrCari").value.trim(), page:1, pageSize:50})
      .then(function(d){
        var rows = d.data || [];
        if (!rows.length){ el("poPrHasil").innerHTML = '<div class="text-muted small py-2">Belum ada PR disetujui yang bisa dipesan.</div>'; return; }
        var h = "";
        for (var i=0;i<rows.length;i++){
          var r = rows[i];
          h += '<a href="javascript:void(0)" class="list-group-item list-group-item-action"'
             + ' onclick="poAmbilPr' + RND + '(' + r.id + ')">'
             + '<div class="d-flex justify-content-between"><span class="fw-bold">' + esc(r.kode) + '</span>'
             + '<span>' + rp(r.nilai) + '</span></div>'
             + '<div class="small text-muted">' + esc(r.keterangan || "") + '</div></a>';
        }
        el("poPrHasil").innerHTML = h;
      });
  };
  // Server mengembalikan SISA yang belum dipesan per baris PR, sehingga satu PR
  // dapat dipecah menjadi beberapa PO tanpa terjadi pemesanan berlebih.
  window["poAmbilPr" + RND] = function(prId){
    api({action:"pengadaan_po_dari_pr", pr_id: prId}).then(function(d){
      if (d.status !== "00" && d.status !== "success"){ pesan(d.description || "Gagal menyiapkan PO dari PR.", false); return; }
      var isian = d.detail || [];
      if (!isian.length){ pesan(d.catatan || "Tidak ada sisa yang perlu dipesan dari PR ini.", false); return; }
      bootstrap.Modal.getInstance(document.getElementById("poPrModal" + RND)).hide();
      kosongkanForm();
      el("poKeterangan").value = d.keterangan || "";
      baris = isian.map(function(x){
        return { barang_id: x.master_asset_id, nama: x.barang, jumlah: angka(x.jumlah),
                 harga: angka(x.hargaBeli), pr_detail_id: x.pr_detail_id || null };
      });
      bukaForm(false, "Buat PO dari " + (d.pr_kode || "PR"), "");
    });
  };

  // Muat pertama kali
  window["poMuat" + RND](1);
})();
</script>
