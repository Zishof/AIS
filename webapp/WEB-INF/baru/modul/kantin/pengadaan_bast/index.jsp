<%@ page isELIgnored="true" %>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// Halaman "Penerimaan Barang/Jasa (BAST)" -- modul Pengadaan POS versi JSP.
// Seluruh aturan bisnis (penomoran, pagar penerimaan berlebih, hitung nilai
// berikut potongan dan PPN, pagar ubah/hapus) berada di server
// PengadaanPosApiHelper yang dipakai bersama Desktop/Android.
//
// Halaman memakai isELIgnored="true": nilai dinamis dirangkai lewat penggabungan
// string JavaScript, tanpa sintaks ekspresi EL yang dapat dievaluasi server.
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
      <h4 class="fw-bold mb-0"><%=Common.getBahasaConfig("Penerimaan Barang/Jasa (BAST)")%></h4>
      <div class="text-muted small"><%=Common.getBahasaConfig("Catat barang yang datang dari penyedia")%></div>
    </div>
    <div>
      <button class="btn btn-outline-primary fw-bold rounded-pill px-3 me-2" onclick="bsDariPo<%=rnd%>()">
        <i class="fas fa-clipboard-check me-2"></i><%=Common.getBahasaConfig("Dari PO")%>
      </button>
      <a class="btn btn-outline-secondary fw-bold rounded-pill px-3 me-2" href="<%=Common.ROOT%>/baru?p=kantin&s=pengadaan_bulk&jenis=bast">
        <i class="fas fa-table me-2"></i><%=Common.getBahasaConfig("Bulk Entry")%>
      </a>
      <button class="btn btn-primary fw-bold rounded-pill px-4" onclick="bsForm<%=rnd%>(null)">
        <i class="fas fa-plus me-2"></i><%=Common.getBahasaConfig("Terima Langsung")%>
      </button>
    </div>
  </div>

  <%-- Dua tab pada setiap menu Pengadaan: "Dasbor" (ringkasan angka) dan
       "Penerimaan" (daftar + CRUD). Susunannya sama di keenam menu, dan
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
        <i class="fas fa-list me-2"></i><%=Common.getBahasaConfig("Penerimaan")%>
      </button>
    </li>
  </ul>
  <div class="tab-content">
  <div class="tab-pane fade show active" id="tabDasbor<%=rnd%>" role="tabpanel">
    <jsp:include page="/WEB-INF/baru/include/dasbor_pengadaan.jsp">
      <jsp:param name="tahap" value="bast"/>
    </jsp:include>
  </div>
  <div class="tab-pane fade" id="tabData<%=rnd%>" role="tabpanel">
  <div class="card border-0 shadow-sm mb-3">
    <div class="card-body py-3">
      <div class="row g-2 align-items-end">
        <div class="col-md-5">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Cari kode / keterangan / no. tagihan")%></label>
          <input type="text" id="bsCari<%=rnd%>" class="form-control" onkeydown="if(event.key==='Enter')bsMuat<%=rnd%>(1)">
        </div>
        <div class="col-md-3">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Status")%></label>
          <select id="bsStatus<%=rnd%>" class="form-select" onchange="bsMuat<%=rnd%>(1)">
            <option value=""><%=Common.getBahasaConfig("Semua status")%></option>
            <option value="DRAFT">DRAFT</option>
            <option value="DISETUJUI">DISETUJUI</option>
          </select>
        </div>
        <div class="col-md-2">
          <button class="btn btn-outline-secondary w-100" onclick="bsMuat<%=rnd%>(1)">
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
            <th><%=Common.getBahasaConfig("Sumber")%></th>
            <th class="text-end"><%=Common.getBahasaConfig("Nilai")%></th>
            <th><%=Common.getBahasaConfig("Status")%></th>
            <th class="text-center"><%=Common.getBahasaConfig("Aksi")%></th>
          </tr>
        </thead>
        <tbody id="bsTbody<%=rnd%>">
          <tr><td colspan="7" class="text-center text-muted py-4"><%=Common.getBahasaConfig("Memuat data...")%></td></tr>
        </tbody>
      </table>
    </div>
    <div class="card-footer bg-white d-flex justify-content-between align-items-center">
      <span class="small text-muted" id="bsInfo<%=rnd%>"></span>
      <div class="btn-group">
        <button class="btn btn-sm btn-outline-secondary" id="bsPrev<%=rnd%>" onclick="bsHal<%=rnd%>(-1)">&laquo;</button>
        <button class="btn btn-sm btn-outline-secondary" id="bsNext<%=rnd%>" onclick="bsHal<%=rnd%>(1)">&raquo;</button>
      </div>
    </div>
  </div>
  </div>
  </div>
</div>

<div class="modal fade" id="bsModal<%=rnd%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-xl modal-dialog-scrollable">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="bsModalJudul<%=rnd%>"><%=Common.getBahasaConfig("Terima Barang")%></h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <div id="bsKunciInfo<%=rnd%>" class="alert alert-warning small d-none"></div>
        <div id="bsPoInfo<%=rnd%>" class="alert alert-info small d-none"></div>
        <div class="row g-2">
          <div class="col-md-4" id="bsBarisPenyedia<%=rnd%>">
            <label class="form-label small mb-1"><%=Common.getBahasaConfig("Penyedia / Vendor")%></label>
            <div class="input-group">
              <input type="text" id="bsPenyediaNama<%=rnd%>" class="form-control" readonly
                     placeholder="<%=Common.getBahasaConfig("Belum dipilih")%>">
              <button class="btn btn-outline-secondary" id="bsPilihPenyedia<%=rnd%>" onclick="bsCariPenyedia<%=rnd%>()">
                <i class="fas fa-search"></i>
              </button>
            </div>
          </div>
          <div class="col-md-3">
            <label class="form-label small mb-1"><%=Common.getBahasaConfig("No. tagihan / faktur")%></label>
            <input type="text" id="bsKodeTagihan<%=rnd%>" class="form-control">
          </div>
          <div class="col-md-2">
            <label class="form-label small mb-1"><%=Common.getBahasaConfig("Tanggal tagihan")%></label>
            <input type="date" id="bsTglTagihan<%=rnd%>" class="form-control">
          </div>
          <div class="col-md-3">
            <label class="form-label small mb-1"><%=Common.getBahasaConfig("Kurir / pengirim")%></label>
            <input type="text" id="bsKurir<%=rnd%>" class="form-control">
          </div>
        </div>
        <div class="mb-2 mt-2">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Keterangan penerimaan")%></label>
          <textarea id="bsKeterangan<%=rnd%>" class="form-control" rows="2"></textarea>
        </div>

        <div class="d-flex justify-content-between align-items-center mt-3 mb-1">
          <h6 class="fw-bold mb-0"><%=Common.getBahasaConfig("Barang yang Diterima")%></h6>
          <div class="d-flex align-items-center">
            <div class="form-check form-switch me-3">
              <input class="form-check-input" type="checkbox" id="bsDiskonPersen<%=rnd%>" onchange="bsRenderBaris<%=rnd%>()">
              <label class="form-check-label small" for="bsDiskonPersen<%=rnd%>"><%=Common.getBahasaConfig("Potongan dalam persen")%></label>
            </div>
            <button class="btn btn-sm btn-outline-primary" id="bsTambahBaris<%=rnd%>" onclick="bsCariBarang<%=rnd%>()">
              <i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah Barang")%>
            </button>
          </div>
        </div>
        <div class="table-responsive">
          <table class="table table-sm align-middle">
            <thead class="table-light">
              <tr>
                <th><%=Common.getBahasaConfig("Barang")%></th>
                <th style="width:110px"><%=Common.getBahasaConfig("Diterima")%></th>
                <th style="width:130px"><%=Common.getBahasaConfig("Harga")%></th>
                <th style="width:110px"><%=Common.getBahasaConfig("Potongan")%></th>
                <th style="width:90px"><%=Common.getBahasaConfig("PPN %")%></th>
                <th style="width:90px"><%=Common.getBahasaConfig("PPh %")%></th>
                <th class="text-end" style="width:140px"><%=Common.getBahasaConfig("Subtotal")%></th>
                <th style="width:50px"></th>
              </tr>
            </thead>
            <tbody id="bsBarisTbody<%=rnd%>"></tbody>
            <tfoot>
              <tr class="fw-bold">
                <td colspan="6" class="text-end"><%=Common.getBahasaConfig("Total Nilai Penerimaan")%></td>
                <td class="text-end" id="bsTotal<%=rnd%>">0</td>
                <td></td>
              </tr>
            </tfoot>
          </table>
        </div>
        <div class="small text-muted fst-italic">
          <%=Common.getBahasaConfig("Total di atas hanya pratinjau; server menghitung ulang memakai rumus yang sama dengan versi ZKoss.")%>
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Tutup")%></button>
        <button type="button" class="btn btn-outline-warning d-none" id="bsBackOrder<%=rnd%>" onclick="bsBackOrder<%=rnd%>()">
          <i class="fas fa-rotate-left me-2"></i><%=Common.getBahasaConfig("Back Order / Pesan Kembali")%>
        </button>
        <button type="button" class="btn btn-primary" id="bsSimpan<%=rnd%>" onclick="bsSimpan<%=rnd%>()"><%=Common.getBahasaConfig("Simpan")%></button>
      </div>
    </div>
  </div>
</div>

<div class="modal fade" id="bsBarangModal<%=rnd%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title"><%=Common.getBahasaConfig("Pilih Barang")%></h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <input type="text" id="bsBarangCari<%=rnd%>" class="form-control mb-2"
               placeholder="<%=Common.getBahasaConfig("Cari kode / nama barang")%>"
               onkeydown="if(event.key==='Enter')bsBarangMuat<%=rnd%>()">
        <div id="bsBarangHasil<%=rnd%>" class="list-group"></div>
      </div>
    </div>
  </div>
</div>

<div class="modal fade" id="bsPenyediaModal<%=rnd%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title"><%=Common.getBahasaConfig("Pilih Penyedia / Vendor")%></h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <input type="text" id="bsPenyediaCari<%=rnd%>" class="form-control mb-2"
               placeholder="<%=Common.getBahasaConfig("Cari kode / nama penyedia")%>"
               onkeydown="if(event.key==='Enter')bsPenyediaMuat<%=rnd%>()">
        <div id="bsPenyediaHasil<%=rnd%>" class="list-group"></div>
      </div>
    </div>
  </div>
</div>

<div class="modal fade" id="bsPoModal<%=rnd%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title"><%=Common.getBahasaConfig("Pilih Pemesanan Pembelian")%></h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <div class="small text-muted mb-2">
          <%=Common.getBahasaConfig("Hanya PO berstatus DISETUJUI yang dapat diterima barangnya.")%>
        </div>
        <input type="text" id="bsPoCari<%=rnd%>" class="form-control mb-2"
               placeholder="<%=Common.getBahasaConfig("Cari kode / keterangan PO")%>"
               onkeydown="if(event.key==='Enter')bsPoMuat<%=rnd%>()">
        <div id="bsPoHasil<%=rnd%>" class="list-group"></div>
      </div>
    </div>
  </div>
</div>



<%-- Back Order / Pesan Kembali: dipakai bila barang datang kurang dari pesanan. --%>
<div class="modal fade" id="bsBoModal<%=rnd%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-lg">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="bsBoJudul<%=rnd%>"><%=Common.getBahasaConfig("Back Order")%></h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <div class="alert alert-warning py-2 small">
          <%=Common.getBahasaConfig("Sisa pesanan ini akan DITUTUP. Sesudah ditutup, pesanan tidak menerima barang lagi -- kekurangannya diterima pada pesanan susulan.")%>
        </div>
        <div class="btn-group mb-2" role="group">
          <input type="radio" class="btn-check" name="bsBoTindakan<%=rnd%>" id="bsBoPesan<%=rnd%>" checked
                 onchange="bsBoGanti<%=rnd%>(true)">
          <label class="btn btn-outline-primary btn-sm" for="bsBoPesan<%=rnd%>"><%=Common.getBahasaConfig("Pesan kembali")%></label>
          <input type="radio" class="btn-check" name="bsBoTindakan<%=rnd%>" id="bsBoTutup<%=rnd%>"
                 onchange="bsBoGanti<%=rnd%>(false)">
          <label class="btn btn-outline-primary btn-sm" for="bsBoTutup<%=rnd%>"><%=Common.getBahasaConfig("Tutup sisa saja")%></label>
        </div>
        <div class="mb-2">
          <label class="form-label small"><%=Common.getBahasaConfig("Alasan")%> *</label>
          <input type="text" id="bsBoAlasan<%=rnd%>" class="form-control form-control-sm"
                 placeholder="<%=Common.getBahasaConfig("mis. stok vendor habis, barang tidak sesuai spesifikasi")%>">
        </div>
        <div class="mb-2" id="bsBoBatasBox<%=rnd%>">
          <label class="form-label small"><%=Common.getBahasaConfig("Batas kirim (hh-bb-tttt)")%></label>
          <input type="text" id="bsBoBatas<%=rnd%>" class="form-control form-control-sm" placeholder="dd-MM-yyyy">
        </div>
        <div id="bsBoIsi<%=rnd%>"></div>
      </div>
      <div class="modal-footer">
        <span id="bsBoRingkas<%=rnd%>" class="me-auto small fw-bold"></span>
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
        <button type="button" class="btn btn-primary" id="bsBoKirim<%=rnd%>" onclick="bsBoKirim<%=rnd%>()">
          <i class="fas fa-rotate-left me-2"></i><%=Common.getBahasaConfig("Tutup & Pesan Kembali")%>
        </button>
      </div>
    </div>
  </div>
</div>

<%-- Pratinjau cetak dokumen Pengadaan.
     Server merender PDF memakai templat JasperReports yang SAMA dengan versi
     ZKoss, lalu URL-nya ditampilkan di dalam bingkai. Pembaca PDF bawaan
     peramban menyediakan tombol cetak dan unduh, sehingga pengguna melihat
     dokumennya lebih dulu sebelum memutuskan mencetak. --%>
<div class="modal fade" id="cetakModal<%=rnd%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-xl modal-dialog-centered">
    <div class="modal-content" style="height:88vh">
      <div class="modal-header">
        <h5 class="modal-title" id="cetakJudul<%=rnd%>"><%=Common.getBahasaConfig("Pratinjau Cetak")%></h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body p-0" style="overflow:hidden">
        <div id="cetakMemuat<%=rnd%>" class="text-muted small p-4"><%=Common.getBahasaConfig("Menyiapkan dokumen...")%></div>
        <iframe id="cetakBingkai<%=rnd%>" style="width:100%;height:100%;border:0;display:none"></iframe>
      </div>
      <div class="modal-footer">
        <a id="cetakUnduh<%=rnd%>" class="btn btn-outline-secondary d-none" target="_blank" href="#">
          <i class="fas fa-download me-2"></i><%=Common.getBahasaConfig("Buka di tab baru")%>
        </a>
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Tutup")%></button>
        <button type="button" class="btn btn-primary" id="cetakTombol<%=rnd%>" onclick="cetakSekarang<%=rnd%>()">
          <i class="fas fa-print me-2"></i><%=Common.getBahasaConfig("Cetak")%>
        </button>
      </div>
    </div>
  </div>
</div>

<%-- Tombol Bantuan mengambang; isinya sepadan dengan bantuan Desktop/Android. --%>
<jsp:include page="/WEB-INF/baru/include/bantuan_pengadaan.jsp">
  <jsp:param name="tahap" value="bast"/>
</jsp:include>

<script>
(function(){
  var RND = "<%=rnd%>";
  var DATA_URL = "<%=Common.ROOT%>/Data";
  var halaman = 1, totalHal = 1, bsAktif = null, baris = [];
  var penyediaId = null, poId = null, poKode = "", terkunciKini = false;

  function el(id){ return document.getElementById(id + RND); }
  function esc(s){ return (s==null?"":String(s)).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;"); }
  function rp(n){ return "Rp " + (Number(n)||0).toLocaleString("id-ID"); }
  function angka(v){ v = String(v==null?"":v).replace(/[^0-9.]/g,""); return Number(v)||0; }

  // Dokumen pengadaan memakai pola hh-bb-tttt agar tetap terbaca layar ZKoss;
  // input HTML memakai tttt-bb-hh sehingga perlu dikonversi bolak-balik.
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

  // Tombol cetak per baris. Pratinjau lebih dulu; templat cetaknya sama dengan
  // versi ZKoss sehingga hasilnya identik.
  function tombolCetak(r){
    return '<button class="btn btn-sm btn-outline-secondary me-1" title="Cetak / pratinjau"'
         + ' onclick="cetakDokumen' + RND + '(\'bast\',' + r.id
         + ',\'' + esc(r.kode || "") + '\')">'
         + '<i class="fas fa-print"></i></button>';
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

  window["bsMuat" + RND] = function(hal){
    halaman = hal || halaman;
    var tbody = el("bsTbody");
    tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">Memuat data...</td></tr>';
    api({action:"pengadaan_bast_daftar", cari: el("bsCari").value.trim(),
         status: el("bsStatus").value, page: halaman, pageSize: 15})
      .then(function(d){
        if (d.status !== "00" && d.status !== "success") {
          tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger py-4">' + esc(d.description || "Gagal memuat data.") + '</td></tr>';
          return;
        }
        var rows = d.data || [];
        totalHal = Math.max(1, Math.ceil((d.total || rows.length) / 15));
        if (!rows.length) {
          tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">Belum ada Penerimaan Barang.</td></tr>';
        } else {
          var html = "";
          for (var i=0;i<rows.length;i++){
            var r = rows[i], st = r.status || "DRAFT";
            var sumber = r.tanpaPemesanan ? "Tanpa PO" : (r.po || "-");
            html += '<tr>'
              + '<td class="fw-bold">' + esc(r.kode) + '</td>'
              + '<td>' + esc(r.tanggal || "-") + '</td>'
              + '<td>' + esc(r.penyedia || "-") + '</td>'
              + '<td class="small">' + esc(sumber) + '</td>'
              + '<td class="text-end">' + rp(r.nilai) + '</td>'
              + '<td><span class="badge bg-' + (st === "DISETUJUI" ? "success" : "warning") + '">' + esc(st) + '</span></td>'
              + '<td class="text-center">' + aksiHtml(r, st) + '</td>'
              + '</tr>';
          }
          tbody.innerHTML = html;
        }
        el("bsInfo").textContent = "Halaman " + halaman + " dari " + totalHal + " - total " + (d.total || rows.length) + " BAST";
        el("bsPrev").disabled = halaman <= 1;
        el("bsNext").disabled = halaman >= totalHal;
      })
      .catch(function(){ tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger py-4">Kesalahan koneksi.</td></tr>'; });
  };

  function aksiHtml(r, st){
    var h = tombolCetak(r) + '<button class="btn btn-sm btn-outline-primary me-1" title="Lihat / ubah" onclick="bsForm' + RND + '(' + r.id + ')"><i class="fas fa-edit"></i></button>';
    if (st === "DISETUJUI") {
      h += '<button class="btn btn-sm btn-outline-secondary me-1" title="Batalkan persetujuan" onclick="bsPutusan' + RND + '(' + r.id + ',\'BATAL\')"><i class="fas fa-undo"></i></button>';
      if (r.sudahSinkron) {
        h += '<span class="badge bg-success" title="Sudah masuk stok lewat faktur ' + esc(r.nomorFakturKulakan || "") + '"><i class="fas fa-check"></i></span>';
      } else {
        h += '<button class="btn btn-sm btn-outline-info" title="Sinkronkan ke stok Kulakan" onclick="bsSinkron' + RND + '(' + r.id + ')"><i class="fas fa-sync-alt"></i></button>';
      }
    } else {
      h += '<button class="btn btn-sm btn-outline-success me-1" title="Setujui" onclick="bsPutusan' + RND + '(' + r.id + ',\'SETUJUI\')"><i class="fas fa-check"></i></button>';
      h += '<button class="btn btn-sm btn-outline-secondary" title="Hapus" onclick="bsHapus' + RND + '(' + r.id + ',\'' + esc(String(r.kode).replace(/'/g,"")) + '\')"><i class="fas fa-trash"></i></button>';
    }
    return h;
  }

  window["bsHal" + RND] = function(delta){
    var baru = halaman + delta;
    if (baru < 1 || baru > totalHal) return;
    window["bsMuat" + RND](baru);
  };

  // ---------- Baris barang ----------
  function diskonPersen(){ return el("bsDiskonPersen").checked; }
  // Pratinjau memakai rumus yang sama dengan entitas server:
  // (diterima x harga) dikurangi potongan, lalu ditambah PPN.
  function subtotal(b){
    var dpp0 = angka(b.diterima) * angka(b.harga);
    var pot = diskonPersen() ? (angka(b.potongan) / 100.0) * dpp0 : angka(b.potongan);
    var dpp = dpp0 - pot;
    return dpp + (angka(b.ppn) / 100.0) * dpp;
  }
  function hitungTotal(){
    var t = 0;
    for (var i=0;i<baris.length;i++) t += subtotal(baris[i]);
    el("bsTotal").textContent = rp(t);
  }
  window["bsRenderBaris" + RND] = function(){ renderBaris(terkunciKini); };
  function renderBaris(terkunci){
    var tb = el("bsBarisTbody");
    if (!baris.length){
      tb.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-3">Belum ada barang. Tambahkan minimal satu baris.</td></tr>';
      hitungTotal(); return;
    }
    var h = "";
    for (var i=0;i<baris.length;i++){
      var b = baris[i];
      var lebih = (b.sisaBoleh !== null && b.sisaBoleh !== undefined && angka(b.diterima) > b.sisaBoleh + 0.000001);
      var catatan = (b.sisaBoleh === null || b.sisaBoleh === undefined) ? ''
        : '<div class="small ' + (lebih ? 'text-danger' : 'text-info') + '">sisa boleh diterima: ' + b.sisaBoleh + '</div>';
      h += '<tr>'
        + '<td>' + esc(b.nama) + catatan + '</td>'
        + '<td><input type="number" class="form-control form-control-sm' + (lebih ? ' is-invalid' : '') + '" value="' + angka(b.diterima) + '"'
        + (terkunci ? ' disabled' : '') + ' oninput="bsUbahBaris' + RND + '(' + i + ',\'diterima\',this.value)"></td>'
        + '<td><input type="number" class="form-control form-control-sm" value="' + angka(b.harga) + '"'
        + (terkunci ? ' disabled' : '') + ' oninput="bsUbahBaris' + RND + '(' + i + ',\'harga\',this.value)"></td>'
        + '<td><input type="number" class="form-control form-control-sm" value="' + angka(b.potongan) + '"'
        + (terkunci ? ' disabled' : '') + ' oninput="bsUbahBaris' + RND + '(' + i + ',\'potongan\',this.value)"></td>'
        + '<td><input type="number" class="form-control form-control-sm" value="' + angka(b.ppn) + '"'
        + (terkunci ? ' disabled' : '') + ' oninput="bsUbahBaris' + RND + '(' + i + ',\'ppn\',this.value)"></td>'
        + '<td><input type="number" class="form-control form-control-sm" value="' + angka(b.pph) + '"'
        + (terkunci ? ' disabled' : '') + ' oninput="bsUbahBaris' + RND + '(' + i + ',\'pph\',this.value)"></td>'
        + '<td class="text-end" id="bsSub' + RND + i + '">' + rp(subtotal(b)) + '</td>'
        + '<td>' + (terkunci ? '' : '<button class="btn btn-sm btn-outline-danger" onclick="bsHapusBaris' + RND + '(' + i + ')"><i class="fas fa-times"></i></button>') + '</td>'
        + '</tr>';
    }
    tb.innerHTML = h;
    hitungTotal();
  }
  window["bsUbahBaris" + RND] = function(i, field, nilai){
    baris[i][field] = angka(nilai);
    var sel = document.getElementById("bsSub" + RND + i);
    if (sel) sel.textContent = rp(subtotal(baris[i]));
    hitungTotal();
    // Baris digambar ulang agar penanda "melebihi sisa" ikut menyesuaikan.
    if (field === "diterima") renderBaris(terkunciKini);
  };
  window["bsHapusBaris" + RND] = function(i){ baris.splice(i,1); renderBaris(false); };

  // ---------- Form ----------
  function bukaForm(terkunci, judul, pesanKunci){
    terkunciKini = terkunci;
    el("bsModalJudul").textContent = judul;
    var kunciBox = el("bsKunciInfo");
    if (terkunci){
      kunciBox.textContent = pesanKunci;
      kunciBox.classList.remove("d-none");
      el("bsSimpan").classList.add("d-none");
      el("bsTambahBaris").classList.add("d-none");
      el("bsPilihPenyedia").classList.add("d-none");
    } else {
      kunciBox.classList.add("d-none");
      el("bsSimpan").classList.remove("d-none");
      el("bsPilihPenyedia").classList.remove("d-none");
      // Baris penerimaan atas PO ditentukan PO-nya; barang bebas hanya utk tanpa PO.
      el("bsTambahBaris").classList.toggle("d-none", poId !== null);
    }
    var poBox = el("bsPoInfo");
    if (poId !== null){
      poBox.textContent = "Penerimaan atas " + poKode + ". Jumlah diterima tidak boleh melebihi sisa yang dipesan.";
      poBox.classList.remove("d-none");
      el("bsBarisPenyedia").classList.add("d-none");
    } else {
      poBox.classList.add("d-none");
      el("bsBarisPenyedia").classList.remove("d-none");
    }
    var kolom = ["bsKeterangan","bsKodeTagihan","bsTglTagihan","bsKurir","bsDiskonPersen"];
    for (var i=0;i<kolom.length;i++) el(kolom[i]).disabled = terkunci;
    renderBaris(terkunci);
    // Back Order hanya masuk akal pada penerimaan yang sudah tersimpan dan
    // berasal dari sebuah PO -- kekurangannya dihitung dari BAST yang tercatat.
    el("bsBackOrder").classList.toggle("d-none", !(poId && bsAktif && bsAktif.id));
    new bootstrap.Modal(document.getElementById("bsModal" + RND)).show();
  }

  function kosongkanForm(){
    bsAktif = null; baris = []; penyediaId = null; poId = null; poKode = "";
    el("bsPenyediaNama").value = "";
    el("bsKeterangan").value = "";
    el("bsKodeTagihan").value = "";
    el("bsTglTagihan").value = "";
    el("bsKurir").value = "";
    el("bsDiskonPersen").checked = false;
  }

  window["bsForm" + RND] = function(id){
    kosongkanForm();
    if (!id){ bukaForm(false, "Terima Barang (tanpa PO)", ""); return; }
    api({action:"pengadaan_bast_detail", id:id}).then(function(d){
      if (d.status !== "00" && d.status !== "success"){ pesan(d.description || "Gagal memuat detail.", false); return; }
      bsAktif = d.header || {};
      var st = bsAktif.status || "DRAFT";
      var terkunci = st !== "DRAFT";
      penyediaId = bsAktif.penyedia_id || null;
      poId = bsAktif.po_id || null;
      poKode = bsAktif.po || "";
      el("bsPenyediaNama").value = bsAktif.penyedia || "";
      el("bsKeterangan").value = bsAktif.keterangan || "";
      el("bsKodeTagihan").value = bsAktif.kodeTagihan || "";
      el("bsTglTagihan").value = keIsoTgl(bsAktif.tanggalTagihan || "");
      el("bsKurir").value = bsAktif.kurir || "";
      baris = (d.detail || []).map(function(x){
        return { barang_id: x.produk_id, master_asset_id: x.master_asset_id, nama: x.barang, diterima: angka(x.diterima),
                 harga: angka(x.hargaBeli), potongan: angka(x.hargaPotongan), ppn: angka(x.persenPpn), pph: angka(x.persenPph),
                 po_detail_id: x.po_detail_id || null,
                 sisaBoleh: (x.sisaBolehDiterima === null || x.sisaBolehDiterima === undefined)
                            ? null : Number(x.sisaBolehDiterima) };
      });
      bukaForm(terkunci, "BAST " + (bsAktif.kode || "") + "  -  " + st,
               "BAST yang sudah disetujui tidak dapat diubah. Batalkan persetujuannya terlebih dahulu bila memang perlu dikoreksi.");
    });
  };

  window["bsSimpan" + RND] = function(){
    if (!baris.length){ pesan("Tambahkan minimal satu baris barang.", false); return; }
    if (poId === null && !penyediaId){ pesan("Pilih penyedia/vendor untuk penerimaan tanpa PO.", false); return; }
    for (var i=0;i<baris.length;i++){
      var b = baris[i];
      if (angka(b.diterima) <= 0){ pesan("Jumlah diterima untuk " + b.nama + " harus lebih besar dari nol.", false); return; }
      if (b.sisaBoleh !== null && b.sisaBoleh !== undefined && angka(b.diterima) > b.sisaBoleh + 0.000001){
        pesan("Jumlah diterima untuk " + b.nama + " melebihi sisa yang dipesan (" + b.sisaBoleh + ").", false);
        return;
      }
    }
    var payload = {
      action: "pengadaan_bast_simpan",
      keterangan: el("bsKeterangan").value.trim(),
      kodeTagihan: el("bsKodeTagihan").value.trim(),
      tanggalTagihan: keTampilan(el("bsTglTagihan").value),
      kurir: el("bsKurir").value.trim(),
      detail: baris.map(function(b){
        var o = { diterima: angka(b.diterima),
                  hargaBeli: angka(b.harga), hargaPotongan: angka(b.potongan),
                  diskonPersen: diskonPersen(), persenPpn: angka(b.ppn), persenPph: angka(b.pph) };
        if (b.barang_id) o.produk_id = b.barang_id; else o.master_asset_id = b.master_asset_id;
        if (b.po_detail_id) o.po_detail_id = b.po_detail_id;
        return o;
      })
    };
    if (poId !== null) payload.po_id = poId;
    if (penyediaId) payload.penyedia_id = penyediaId;
    if (bsAktif && bsAktif.id) payload.id = bsAktif.id;
    api(payload).then(function(d){
      var ok = d.status === "00" || d.status === "success";
      pesan(ok ? ("BAST tersimpan: " + (d.kode || "")) : (d.description || "Gagal menyimpan."), ok);
      if (ok){
        bootstrap.Modal.getInstance(document.getElementById("bsModal" + RND)).hide();
        window["bsMuat" + RND](halaman);
      }
    });
  };

  // ---------- Keputusan & hapus ----------
  window["bsPutusan" + RND] = function(id, keputusan){
    api({action:"pengadaan_bast_putusan", id:id, keputusan:keputusan}).then(function(d){
      var ok = d.status === "00" || d.status === "success";
      pesan(ok ? ("Keputusan tersimpan: " + (d.statusDokumen || keputusan)) : (d.description || "Gagal menyimpan keputusan."), ok);
      if (ok) window["bsMuat" + RND](halaman);
    });
  };
  window["bsHapus" + RND] = function(id, kode){
    if (!window.confirm("Hapus BAST " + kode + "? Hanya dokumen berstatus DRAFT yang dapat dihapus.")) return;
    api({action:"pengadaan_bast_hapus", id:id}).then(function(d){
      var ok = d.status === "00" || d.status === "success";
      pesan(ok ? "BAST dihapus." : (d.description || "Gagal menghapus."), ok);
      if (ok) window["bsMuat" + RND](halaman);
    });
  };

  // ---------- Sinkronisasi ke Kulakan ----------
  // Server menolak sinkronisasi kedua karena akan menggandakan stok, jadi
  // konfirmasi di sini menegaskan bahwa langkah ini hanya sekali.
  window["bsSinkron" + RND] = function(id){
    if (!window.confirm("Tambahkan barang penerimaan ini ke stok toko sebagai faktur Kulakan? "
        + "Langkah ini hanya dapat dilakukan sekali untuk penerimaan ini.")) return;
    api({action:"pengadaan_bast_sinkron_kulakan", id:id}).then(function(d){
      var ok = d.status === "00" || d.status === "success";
      pesan(ok ? ("Stok bertambah lewat faktur " + (d.nomorFaktur || "") + " (" + (d.jumlahBaris || 0) + " baris).")
               : (d.description || "Gagal menyinkronkan."), ok);
      if (ok) window["bsMuat" + RND](halaman);
    });
  };

  // ---------- Pemilih barang ----------
  window["bsCariBarang" + RND] = function(){
    el("bsBarangCari").value = "";
    el("bsBarangHasil").innerHTML = "";
    new bootstrap.Modal(document.getElementById("bsBarangModal" + RND)).show();
  };
  window["bsBarangMuat" + RND] = function(){
    api({action:"pengadaan_barang_cari", keyword: el("bsBarangCari").value.trim(), limit:50}).then(function(d){
      var rows = d.data || [];
      if (!rows.length){ el("bsBarangHasil").innerHTML = '<div class="text-muted small py-2">Tidak ada barang ditemukan.</div>'; return; }
      var h = "";
      for (var i=0;i<rows.length;i++){
        var p = rows[i];
        h += '<a href="javascript:void(0)" class="list-group-item list-group-item-action"'
           + ' onclick="bsPilihBarang' + RND + '(' + p.id + ',\'' + esc(String(p.nama).replace(/'/g,"")) + '\')">'
           + '<div class="fw-bold">' + esc(p.nama) + '</div>'
           + '<div class="small text-muted">' + esc(p.kode || "") + ' ' + esc(p.satuan || "") + '</div></a>';
      }
      el("bsBarangHasil").innerHTML = h;
    });
  };
  window["bsPilihBarang" + RND] = function(id, nama){
    baris.push({ barang_id:id, master_asset_id:null, nama:nama, diterima:1, harga:0, potongan:0, ppn:0, pph:0,
                 po_detail_id:null, sisaBoleh:null });
    renderBaris(false);
    bootstrap.Modal.getInstance(document.getElementById("bsBarangModal" + RND)).hide();
  };

  // ---------- Pemilih penyedia ----------
  window["bsCariPenyedia" + RND] = function(){
    el("bsPenyediaCari").value = "";
    el("bsPenyediaHasil").innerHTML = "";
    new bootstrap.Modal(document.getElementById("bsPenyediaModal" + RND)).show();
    window["bsPenyediaMuat" + RND]();
  };
  window["bsPenyediaMuat" + RND] = function(){
    api({action:"pengadaan_penyedia_cari", keyword: el("bsPenyediaCari").value.trim(), limit:50}).then(function(d){
      var rows = d.data || [];
      if (!rows.length){ el("bsPenyediaHasil").innerHTML = '<div class="text-muted small py-2">Tidak ada penyedia ditemukan.</div>'; return; }
      var h = "";
      for (var i=0;i<rows.length;i++){
        var p = rows[i];
        h += '<a href="javascript:void(0)" class="list-group-item list-group-item-action"'
           + ' onclick="bsPilihPenyedia' + RND + '(' + p.id + ',\'' + esc(String(p.nama).replace(/'/g,"")) + '\')">'
           + '<div class="fw-bold">' + esc(p.nama) + '</div>'
           + '<div class="small text-muted">' + esc(p.kode || "") + ' ' + esc(p.alamat || "") + '</div></a>';
      }
      el("bsPenyediaHasil").innerHTML = h;
    });
  };
  window["bsPilihPenyedia" + RND] = function(id, nama){
    penyediaId = id;
    el("bsPenyediaNama").value = nama;
    bootstrap.Modal.getInstance(document.getElementById("bsPenyediaModal" + RND)).hide();
  };

  // ---------- Terima dari PO ----------
  window["bsDariPo" + RND] = function(){
    el("bsPoCari").value = "";
    el("bsPoHasil").innerHTML = "";
    new bootstrap.Modal(document.getElementById("bsPoModal" + RND)).show();
    window["bsPoMuat" + RND]();
  };
  window["bsPoMuat" + RND] = function(){
    api({action:"pengadaan_po_daftar", status:"DISETUJUI", cari: el("bsPoCari").value.trim(), page:1, pageSize:50})
      .then(function(d){
        var rows = d.data || [];
        if (!rows.length){ el("bsPoHasil").innerHTML = '<div class="text-muted small py-2">Belum ada PO disetujui yang bisa diterima.</div>'; return; }
        var h = "";
        for (var i=0;i<rows.length;i++){
          var r = rows[i];
          h += '<a href="javascript:void(0)" class="list-group-item list-group-item-action"'
             + ' onclick="bsAmbilPo' + RND + '(' + r.id + ')">'
             + '<div class="d-flex justify-content-between"><span class="fw-bold">' + esc(r.kode) + '</span>'
             + '<span>' + rp(r.nilai) + '</span></div>'
             + '<div class="small text-muted">' + esc(r.penyedia || "") + '</div></a>';
        }
        el("bsPoHasil").innerHTML = h;
      });
  };
  // Server mengembalikan SISA yang belum diterima per baris, sehingga satu PO
  // dapat diterima bertahap tanpa penerimaan berlebih.
  window["bsAmbilPo" + RND] = function(id){
    api({action:"pengadaan_bast_dari_po", po_id: id}).then(function(d){
      if (d.status !== "00" && d.status !== "success"){ pesan(d.description || "Gagal menyiapkan penerimaan.", false); return; }
      var isian = d.detail || [];
      if (!isian.length){ pesan(d.catatan || "Tidak ada sisa yang perlu diterima dari PO ini.", false); return; }
      bootstrap.Modal.getInstance(document.getElementById("bsPoModal" + RND)).hide();
      kosongkanForm();
      poId = d.po_id;
      poKode = d.po_kode || "";
      penyediaId = d.penyedia_id || null;
      el("bsPenyediaNama").value = d.penyedia || "";
      el("bsKeterangan").value = d.keterangan || "";
      baris = isian.map(function(x){
        return { barang_id: x.produk_id, master_asset_id: x.master_asset_id, nama: x.barang, diterima: angka(x.diterima),
                 harga: angka(x.hargaBeli), potongan: 0, ppn: 0, pph: 0,
                 po_detail_id: x.po_detail_id || null,
                 sisaBoleh: Number(x.sisaBolehDiterima) };
      });
      bukaForm(false, "Terima Barang dari " + poKode, "");
    });
  };


  // ---------- Back Order / Pesan Kembali ----------
  // Sisa pesanan lama SELALU ditutup lebih dulu; tanpa itu jumlah yang sama akan
  // terhitung dua kali dan permintaan asalnya tampak dipesan melebihi yang diminta.
  var boBaris = [], boPoId = null, boPenyediaId = null;
  window["bsBackOrder" + RND] = function(){
    if (!poId){ pesan("Back order hanya berlaku untuk penerimaan atas sebuah PO.", false); return; }
    boPoId = poId;
    el("bsBoJudul").textContent = "Back Order - " + (poKode || "");
    el("bsBoAlasan").value = "";
    el("bsBoBatas").value = "";
    el("bsBoIsi").innerHTML = "";
    el("bsBoRingkas").innerHTML = "";
    document.getElementById("bsBoPesan" + RND).checked = true;
    window["bsBoGanti" + RND](true);
    new bootstrap.Modal(document.getElementById("bsBoModal" + RND)).show();
    api({action:"pengadaan_po_kekurangan", po_id: boPoId}).then(function(d){
      if (d.status !== "00" && d.status !== "success"){
        el("bsBoIsi").innerHTML = '<div class="text-danger small">' + esc(d.description || "Gagal memuat kekurangan pesanan.") + '</div>';
        return;
      }
      boPenyediaId = d.penyedia_id || null;
      boBaris = (d.detail || []).filter(function(x){ return angka(x.kurang) > 0; });
      if (!boBaris.length){
        el("bsBoIsi").innerHTML = '<div class="text-muted small">Tidak ada kekurangan pada ' + esc(d.po || "") + ' - seluruh barang sudah diterima lengkap.</div>';
        el("bsBoKirim").disabled = true;
        return;
      }
      el("bsBoKirim").disabled = false;
      var h = '<table class="table table-sm"><thead><tr><th style="width:36px"></th><th>Barang</th>'
            + '<th class="text-end">Dipesan</th><th class="text-end">Diterima</th><th class="text-end">Kurang</th>'
            + '<th class="text-end" style="width:110px">Pesan ulang</th><th class="text-end">Nilai</th></tr></thead><tbody>';
      for (var i=0;i<boBaris.length;i++){
        var x = boBaris[i];
        h += '<tr><td><input type="checkbox" class="form-check-input" id="boCek' + i + RND + '" checked onchange="bsBoHitung' + RND + '()"></td>'
           + '<td class="small">' + esc(x.barang || "") + '</td>'
           + '<td class="text-end small">' + angka(x.dipesan) + '</td>'
           + '<td class="text-end small">' + angka(x.diterima) + '</td>'
           + '<td class="text-end small fw-bold">' + angka(x.kurang) + '</td>'
           + '<td><input type="number" class="form-control form-control-sm text-end" id="boJml' + i + RND + '"'
           + ' value="' + angka(x.kurang) + '" min="0" step="any" onchange="bsBoHitung' + RND + '()"></td>'
           + '<td class="text-end small" id="boNil' + i + RND + '">' + rp(x.nilaiKurang) + '</td></tr>';
      }
      h += '</tbody></table>';
      el("bsBoIsi").innerHTML = h;
      window["bsBoHitung" + RND]();
    });
  };
  window["bsBoGanti" + RND] = function(pesanKembali){
    el("bsBoBatasBox").classList.toggle("d-none", !pesanKembali);
    el("bsBoIsi").classList.toggle("d-none", !pesanKembali);
    el("bsBoKirim").innerHTML = pesanKembali
      ? '<i class="fas fa-rotate-left me-2"></i>Tutup &amp; Pesan Kembali'
      : '<i class="fas fa-ban me-2"></i>Tutup Sisa';
  };
  window["bsBoHitung" + RND] = function(){
    var n = 0, total = 0;
    for (var i=0;i<boBaris.length;i++){
      var c = document.getElementById("boCek" + i + RND);
      var j = document.getElementById("boJml" + i + RND);
      var t = document.getElementById("boNil" + i + RND);
      var jml = j ? angka(j.value) : 0;
      var sub = jml * angka(boBaris[i].hargaBeli);
      if (t) t.innerHTML = rp(sub);
      if (c && c.checked && jml > 0){ n++; total += sub; }
    }
    el("bsBoRingkas").innerHTML = n + " barang &middot; " + rp(total);
  };
  window["bsBoKirim" + RND] = function(){
    var alasan = el("bsBoAlasan").value.trim();
    if (!alasan){ pesan("Alasan wajib diisi - keputusan menutup sisa pesanan harus dapat ditelusuri.", false); return; }
    var pesanKembali = document.getElementById("bsBoPesan" + RND).checked;
    var det = [];
    if (pesanKembali){
      for (var i=0;i<boBaris.length;i++){
        var c = document.getElementById("boCek" + i + RND);
        var j = document.getElementById("boJml" + i + RND);
        if (!c || !c.checked) continue;
        var jml = j ? angka(j.value) : 0;
        if (jml <= 0) continue;
        det.push({ po_detail_id: boBaris[i].po_detail_id, jumlah: jml });
      }
      if (!det.length){ pesan("Centang barang yang ingin dipesan ulang, atau pilih Tutup sisa saja.", false); return; }
    }
    var payload = { action:"pengadaan_po_back_order", po_id: boPoId, alasan: alasan,
                    tindakan: pesanKembali ? "pesan_kembali" : "tutup_saja" };
    if (pesanKembali){
      payload.detail = det;
      if (boPenyediaId) payload.penyedia_id = boPenyediaId;
      var batas = el("bsBoBatas").value.trim();
      if (batas) payload.pengirimanPalingLambat = batas;
    }
    api(payload).then(function(d){
      if (d.status !== "00" && d.status !== "success"){ pesan(d.description || "Gagal memproses back order.", false); return; }
      bootstrap.Modal.getInstance(document.getElementById("bsBoModal" + RND)).hide();
      bootstrap.Modal.getInstance(document.getElementById("bsModal" + RND)).hide();
      pesan(d.description || "Back order diproses.", true);
      window["bsMuat" + RND](1);
    });
  };
  // ---------- Cetak dokumen ----------
  window["cetakDokumen" + RND] = function(tahap, id, kode){
    el("cetakJudul").textContent = "Pratinjau Cetak " + (kode || "");
    el("cetakMemuat").classList.remove("d-none");
    el("cetakBingkai").style.display = "none";
    el("cetakUnduh").classList.add("d-none");
    new bootstrap.Modal(document.getElementById("cetakModal" + RND)).show();
    api({action:"pengadaan_cetak", tahap: tahap, id: id}).then(function(d){
      if (d.status !== "00" && d.status !== "success"){
        el("cetakMemuat").textContent = d.description || "Dokumen gagal dicetak.";
        return;
      }
      el("cetakMemuat").classList.add("d-none");
      el("cetakBingkai").src = d.url;
      el("cetakBingkai").style.display = "block";
      el("cetakUnduh").href = d.url;
      el("cetakUnduh").classList.remove("d-none");
    }).catch(function(){
      el("cetakMemuat").textContent = "Kesalahan koneksi saat menyiapkan dokumen.";
    });
  };
  window["cetakSekarang" + RND] = function(){
    var bingkai = el("cetakBingkai");
    if (!bingkai.src || bingkai.style.display === "none") return;
    try {
      bingkai.contentWindow.focus();
      bingkai.contentWindow.print();
    } catch (e) {
      // Sebagian peramban memblokir print() lintas-bingkai; buka di tab baru
      // supaya pengguna tetap dapat mencetak lewat pembaca PDF bawaannya.
      window.open(bingkai.src, "_blank");
    }
  };

  // Muat pertama kali
  window["bsMuat" + RND](1);
})();
</script>
