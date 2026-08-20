<%@ page isELIgnored="true" %>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// Halaman "Permintaan Pembelian (PR)" -- modul Pengadaan POS versi JSP.
// Seluruh aturan bisnis (penomoran, hitung nilai, pagar ubah/hapus, keputusan)
// berada di server PengadaanPosApiHelper, dipakai bersama Desktop/Android,
// sehingga ketiga kanal berperilaku identik.
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
      <h4 class="fw-bold mb-0"><%=Common.getBahasaConfig("Permintaan Pembelian (PR)")%></h4>
      <div class="text-muted small"><%=Common.getBahasaConfig("Ajukan kebutuhan barang sebelum dipesan ke supplier")%></div>
    </div>
    <a class="btn btn-outline-secondary fw-bold rounded-pill px-3 me-2" href="<%=Common.ROOT%>/baru?p=kantin&s=pengadaan_bulk&jenis=pr">
      <i class="fas fa-table me-2"></i><%=Common.getBahasaConfig("Bulk Entry")%>
    </a>
    <button class="btn btn-primary fw-bold rounded-pill px-4" onclick="prForm<%=rnd%>(null)">
      <i class="fas fa-plus me-2"></i><%=Common.getBahasaConfig("Buat PR")%>
    </button>
  </div>

  <%-- Dua tab pada setiap menu Pengadaan: "Dasbor" (ringkasan angka) dan
       "Permintaan" (daftar + CRUD). Susunannya sama di keenam menu, dan
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
        <i class="fas fa-list me-2"></i><%=Common.getBahasaConfig("Permintaan")%>
      </button>
    </li>
  </ul>
  <div class="tab-content">
  <div class="tab-pane fade show active" id="tabDasbor<%=rnd%>" role="tabpanel">
    <jsp:include page="/WEB-INF/baru/include/dasbor_pengadaan.jsp">
      <jsp:param name="tahap" value="pr"/>
    </jsp:include>
  </div>
  <div class="tab-pane fade" id="tabData<%=rnd%>" role="tabpanel">
  <div class="card border-0 shadow-sm mb-3">
    <div class="card-body py-3">
      <div class="row g-2 align-items-end">
        <div class="col-md-5">
          <label class="form-label small fw-bold mb-1"><%=Common.getBahasaConfig("Cari kode / keterangan")%></label>
          <input type="text" id="prCari<%=rnd%>" class="form-control" onkeydown="if(event.key==='Enter')prMuat<%=rnd%>(1)">
        </div>
        <div class="col-md-3">
          <label class="form-label small fw-bold mb-1"><%=Common.getBahasaConfig("Status")%></label>
          <select id="prStatus<%=rnd%>" class="form-select" onchange="prMuat<%=rnd%>(1)">
            <option value=""><%=Common.getBahasaConfig("Semua status")%></option>
            <option value="DRAFT">DRAFT</option>
            <option value="DISETUJUI">DISETUJUI</option>
            <option value="DITOLAK">DITOLAK</option>
            <option value="TUTUP">TUTUP</option>
          </select>
        </div>
        <div class="col-md-4 d-flex gap-2">
          <button class="btn btn-outline-primary rounded-pill px-4" onclick="prMuat<%=rnd%>(1)">
            <i class="fas fa-filter me-2"></i><%=Common.getBahasaConfig("Terapkan")%>
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
            <th><%=Common.getBahasaConfig("Keterangan")%></th>
            <th class="text-end"><%=Common.getBahasaConfig("Nilai")%></th>
            <th><%=Common.getBahasaConfig("Status")%></th>
            <th class="text-center"><%=Common.getBahasaConfig("Aksi")%></th>
          </tr>
        </thead>
        <tbody id="prTbody<%=rnd%>">
          <tr><td colspan="6" class="text-center text-muted py-4"><%=Common.getBahasaConfig("Memuat data...")%></td></tr>
        </tbody>
      </table>
    </div>
    <div class="card-footer d-flex justify-content-between align-items-center">
      <span class="small text-muted" id="prInfo<%=rnd%>"></span>
      <div>
        <button class="btn btn-sm btn-outline-secondary" id="prPrev<%=rnd%>" onclick="prHal<%=rnd%>(-1)">&laquo;</button>
        <button class="btn btn-sm btn-outline-secondary" id="prNext<%=rnd%>" onclick="prHal<%=rnd%>(1)">&raquo;</button>
      </div>
    </div>
  </div>
  </div>
  </div>
</div>

<!-- Modal form PR -->
<div class="modal fade" id="prModal<%=rnd%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-dialog-scrollable">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="prModalJudul<%=rnd%>"><%=Common.getBahasaConfig("Buat Permintaan Pembelian")%></h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <div id="prKunciInfo<%=rnd%>" class="alert alert-warning small d-none"></div>
        <div class="mb-3">
          <label class="form-label small fw-bold"><%=Common.getBahasaConfig("Keterangan / kebutuhan")%></label>
          <textarea id="prKeterangan<%=rnd%>" class="form-control" rows="2"></textarea>
        </div>
        <%-- Permintaan Pembelian memotong anggaran, jadi anggarannya harus jelas
             sejak awal. Satu-satunya jalan keluar adalah menyatakan permintaan ini
             memang tanpa anggaran -- pilihan yang eksplisit. --%>
        <div class="row g-2 mb-3">
          <%-- Bandbox: kotak baca-saja + jendela pencarian, sepadan dengan bandbox
               anggaran pada versi ZKoss. Disembunyikan bila "Tanpa anggaran". --%>
          <div class="col-md-8" id="prKotakAnggaran<%=rnd%>">
            <label class="form-label small fw-bold"><%=Common.getBahasaConfig("Anggaran")%> *</label>
            <div class="input-group">
              <input type="text" id="prAnggaranNama<%=rnd%>" class="form-control" readonly
                     placeholder="<%=Common.getBahasaConfig("Belum dipilih")%>"
                     onclick="prCariAnggaran<%=rnd%>()">
              <button class="btn btn-outline-secondary" type="button" id="prTombolAnggaran<%=rnd%>"
                      onclick="prCariAnggaran<%=rnd%>()">
                <i class="fas fa-search"></i>
              </button>
            </div>
            <input type="hidden" id="prAnggaran<%=rnd%>">
            <div class="form-text small" id="prAnggaranInfo<%=rnd%>"></div>
          </div>
          <div class="col-md-4">
            <div class="form-check mt-4">
              <input class="form-check-input" type="checkbox" id="prTanpaAnggaran<%=rnd%>"
                     onchange="prGantiAnggaran<%=rnd%>()">
              <label class="form-check-label small" for="prTanpaAnggaran<%=rnd%>">
                <%=Common.getBahasaConfig("Tanpa anggaran")%>
              </label>
            </div>
          </div>
        </div>
        <div class="d-flex justify-content-between align-items-center mb-2">
          <b><%=Common.getBahasaConfig("Barang yang Diminta")%></b>
          <button class="btn btn-sm btn-outline-primary" id="prTambahBaris<%=rnd%>" onclick="prCariProduk<%=rnd%>()">
            <i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah Barang")%>
          </button>
        </div>
        <div class="table-responsive">
          <table class="table table-sm align-middle">
            <thead class="table-light">
              <tr>
                <th><%=Common.getBahasaConfig("Barang")%></th>
                <th style="width:110px"><%=Common.getBahasaConfig("Jumlah")%></th>
                <th style="width:150px"><%=Common.getBahasaConfig("Harga Modal")%></th>
                <th class="text-end" style="width:140px"><%=Common.getBahasaConfig("Subtotal")%></th>
                <th style="width:50px"></th>
              </tr>
            </thead>
            <tbody id="prBarisTbody<%=rnd%>"></tbody>
            <tfoot>
              <tr class="fw-bold">
                <td colspan="3" class="text-end"><%=Common.getBahasaConfig("Total Nilai PR")%></td>
                <td class="text-end" id="prTotal<%=rnd%>">0</td>
                <td></td>
              </tr>
            </tfoot>
          </table>
        </div>
        <div class="form-text"><%=Common.getBahasaConfig("Nilai dihitung ulang oleh server dari baris di atas, sehingga total dokumen selalu sama dengan rinciannya.")%></div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Tutup")%></button>
        <button type="button" class="btn btn-primary" id="prSimpan<%=rnd%>" onclick="prSimpan<%=rnd%>()"><%=Common.getBahasaConfig("Simpan")%></button>
      </div>
    </div>
  </div>
</div>

<!-- Modal cari produk -->
<div class="modal fade" id="prProdukModal<%=rnd%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-scrollable">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title"><%=Common.getBahasaConfig("Pilih Barang")%></h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <input type="text" id="prProdukCari<%=rnd%>" class="form-control mb-2"
               placeholder="<%=Common.getBahasaConfig("kode atau nama produk")%>"
               onkeydown="if(event.key==='Enter')prProdukMuat<%=rnd%>()">
        <div id="prProdukHasil<%=rnd%>" class="list-group"></div>
      </div>
    </div>
  </div>
</div>


<div class="modal fade" id="prAnggaranModal<%=rnd%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-lg">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title"><%=Common.getBahasaConfig("Pilih Anggaran")%></h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <input type="text" id="prAnggaranCari<%=rnd%>" class="form-control mb-2"
               placeholder="<%=Common.getBahasaConfig("Cari kode / nama anggaran")%>"
               onkeydown="if(event.key==='Enter')prMuatAnggaran<%=rnd%>()">
        <div id="prAnggaranHasil<%=rnd%>" class="list-group"></div>
      </div>
    </div>
  </div>
</div>

<%-- Tombol Bantuan mengambang; isinya sepadan dengan bantuan Desktop/Android. --%>
<jsp:include page="/WEB-INF/baru/include/bantuan_pengadaan.jsp">
  <jsp:param name="tahap" value="pr"/>
</jsp:include>

<script>
(function(){
  var RND = "<%=rnd%>";
  var DATA_URL = "<%=Common.ROOT%>/Data";
  var halaman = 1, totalHal = 1, prAktif = null, baris = [];

  function el(id){ return document.getElementById(id + RND); }
  function esc(s){ return (s==null?"":String(s)).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;"); }
  function rp(n){ return "Rp " + (Number(n)||0).toLocaleString("id-ID"); }
  function angka(v){ v = String(v==null?"":v).replace(/[^0-9.]/g,""); return Number(v)||0; }

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
    if (s === "DITOLAK") return "danger";
    if (s === "TUTUP") return "secondary";
    return "warning";
  }

  window["prMuat" + RND] = function(hal){
    halaman = hal || halaman;
    var tbody = el("prTbody");
    tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">Memuat data...</td></tr>';
    api({action:"pengadaan_pr_daftar", cari: el("prCari").value.trim(),
         status: el("prStatus").value, page: halaman, pageSize: 15})
      .then(function(d){
        if (d.status !== "00" && d.status !== "success") {
          tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger py-4">' + esc(d.description || "Gagal memuat data.") + '</td></tr>';
          return;
        }
        var rows = d.data || [];
        totalHal = Math.max(1, Math.ceil((d.total || rows.length) / 15));
        if (!rows.length) {
          tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">Belum ada Permintaan Pembelian.</td></tr>';
        } else {
          var html = "";
          for (var i=0;i<rows.length;i++){
            var r = rows[i], st = r.status || "DRAFT";
            html += '<tr>'
              + '<td class="fw-bold">' + esc(r.kode) + '</td>'
              + '<td>' + esc(r.tanggal || "-") + '</td>'
              + '<td>' + esc(r.keterangan || "") + '</td>'
              + '<td class="text-end">' + rp(r.nilai) + '</td>'
              + '<td><span class="badge bg-' + warnaStatus(st) + '">' + esc(st) + '</span></td>'
              + '<td class="text-center">' + aksiHtml(r, st) + '</td>'
              + '</tr>';
          }
          tbody.innerHTML = html;
        }
        el("prInfo").textContent = "Halaman " + halaman + " dari " + totalHal + " - total " + (d.total || rows.length) + " PR";
        el("prPrev").disabled = halaman <= 1;
        el("prNext").disabled = halaman >= totalHal;
      })
      .catch(function(){ tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger py-4">Kesalahan koneksi.</td></tr>'; });
  };

  function aksiHtml(r, st){
    var h = '<button class="btn btn-sm btn-outline-primary me-1" title="Lihat / ubah" onclick="prForm' + RND + '(' + r.id + ')"><i class="fas fa-edit"></i></button>';
    if (st === "DRAFT") {
      h += '<button class="btn btn-sm btn-outline-success me-1" title="Setujui" onclick="prPutusan' + RND + '(' + r.id + ',\'SETUJUI\')"><i class="fas fa-check"></i></button>';
      h += '<button class="btn btn-sm btn-outline-danger me-1" title="Tolak" onclick="prPutusan' + RND + '(' + r.id + ',\'TOLAK\')"><i class="fas fa-times"></i></button>';
      h += '<button class="btn btn-sm btn-outline-secondary" title="Hapus" onclick="prHapus' + RND + '(' + r.id + ',\'' + esc(r.kode) + '\')"><i class="fas fa-trash"></i></button>';
    } else if (st === "DISETUJUI" || st === "DITOLAK") {
      h += '<button class="btn btn-sm btn-outline-secondary" title="Batalkan keputusan" onclick="prPutusan' + RND + '(' + r.id + ',\'BATAL\')"><i class="fas fa-undo"></i></button>';
    }
    return h;
  }

  window["prHal" + RND] = function(delta){
    var baru = halaman + delta;
    if (baru < 1 || baru > totalHal) return;
    window["prMuat" + RND](baru);
  };

  // ---------- Form PR ----------
  function hitungTotal(){
    var t = 0;
    for (var i=0;i<baris.length;i++) t += baris[i].jumlah * baris[i].harga;
    el("prTotal").textContent = rp(t);
  }
  function renderBaris(terkunci){
    var tb = el("prBarisTbody");
    if (!baris.length){
      tb.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-3">Belum ada barang. Tambahkan minimal satu baris.</td></tr>';
      hitungTotal(); return;
    }
    var h = "";
    for (var i=0;i<baris.length;i++){
      var b = baris[i];
      h += '<tr>'
        + '<td>' + esc(b.nama) + '</td>'
        + '<td><input type="number" class="form-control form-control-sm" value="' + b.jumlah + '"'
        + (terkunci ? ' disabled' : '') + ' oninput="prUbahBaris' + RND + '(' + i + ',\'jumlah\',this.value)"></td>'
        + '<td><input type="number" class="form-control form-control-sm" value="' + b.harga + '"'
        + (terkunci ? ' disabled' : '') + ' oninput="prUbahBaris' + RND + '(' + i + ',\'harga\',this.value)"></td>'
        + '<td class="text-end" id="prSub' + RND + i + '">' + rp(b.jumlah * b.harga) + '</td>'
        + '<td>' + (terkunci ? '' : '<button class="btn btn-sm btn-outline-danger" onclick="prHapusBaris' + RND + '(' + i + ')"><i class="fas fa-times"></i></button>') + '</td>'
        + '</tr>';
    }
    tb.innerHTML = h;
    hitungTotal();
  }
  window["prUbahBaris" + RND] = function(i, field, nilai){
    baris[i][field] = angka(nilai);
    var sel = document.getElementById("prSub" + RND + i);
    if (sel) sel.textContent = rp(baris[i].jumlah * baris[i].harga);
    hitungTotal();
  };
  window["prHapusBaris" + RND] = function(i){ baris.splice(i,1); renderBaris(false); };

  // Pemilih Anggaran berbentuk bandbox: kotak baca-saja + jendela pencarian.
  // Aksinya sama dengan Desktop/Android sehingga daftar dan angkanya identik.
  var anggaranData = [];
  window["prCariAnggaran" + RND] = function(){
    if (el("prTanpaAnggaran").checked || el("prTombolAnggaran").disabled) return;
    el("prAnggaranCari").value = "";
    el("prAnggaranHasil").innerHTML = "";
    new bootstrap.Modal(document.getElementById("prAnggaranModal" + RND)).show();
    window["prMuatAnggaran" + RND]();
  };
  window["prMuatAnggaran" + RND] = function(){
    api({action:"pengadaan_anggaran_cari", keyword: el("prAnggaranCari").value.trim(), limit:50})
      .then(function(d){
        anggaranData = d.data || [];
        if (!anggaranData.length){
          el("prAnggaranHasil").innerHTML = '<div class="text-muted small py-2">'
            + esc(d.catatan || "Tidak ada anggaran aktif.") + '</div>';
          return;
        }
        var h = "";
        for (var i=0;i<anggaranData.length;i++){
          var a = anggaranData[i];
          h += '<a href="javascript:void(0)" class="list-group-item list-group-item-action" onclick="prPilihAnggaran'
             + RND + '(' + a.id + ')">'
             + '<div class="d-flex justify-content-between"><span class="fw-bold">'
             + esc(a.kode || "") + " " + esc(a.nama || "")
             + '</span><span>sisa ' + rp(a.sisa) + '</span></div>'
             + '<div class="small text-muted">pagu ' + rp(a.pagu)
             + " - realisasi " + rp(a.realisasi) + " - dalam proses " + rp(a.dalamProses)
             + '</div></a>';
        }
        el("prAnggaranHasil").innerHTML = h;
      });
  };
  window["prPilihAnggaran" + RND] = function(id){
    for (var i=0;i<anggaranData.length;i++){
      if (String(anggaranData[i].id) !== String(id)) continue;
      var a = anggaranData[i];
      el("prAnggaran").value = a.id;
      el("prAnggaranNama").value = (a.kode || "") + " " + (a.nama || "");
      el("prAnggaranInfo").textContent = "Pagu " + rp(a.pagu) + " - realisasi "
        + rp(a.realisasi) + " - sisa " + rp(a.sisa);
    }
    bootstrap.Modal.getInstance(document.getElementById("prAnggaranModal" + RND)).hide();
  };
  window["prGantiAnggaran" + RND] = function(){
    var tanpa = el("prTanpaAnggaran").checked;
    // Disembunyikan, bukan sekadar dimatikan: kolom mati yang tetap terlihat hanya
    // mengundang pertanyaan apakah masih perlu diisi.
    el("prKotakAnggaran").classList.toggle("d-none", tanpa);
    if (tanpa){
      el("prAnggaran").value = "";
      el("prAnggaranNama").value = "";
      el("prAnggaranInfo").textContent = "";
    }
  };
  function setelAnggaranAwal(id, nama){
    el("prAnggaran").value = id ? id : "";
    el("prAnggaranNama").value = nama || "";
    el("prAnggaranInfo").textContent = "";
  }

  window["prForm" + RND] = function(id){
    prAktif = null; baris = [];
    el("prKeterangan").value = "";
    el("prTanpaAnggaran").checked = false;
    el("prTanpaAnggaran").disabled = false;
    el("prTombolAnggaran").disabled = false;
    setelAnggaranAwal(null, "");
    window["prGantiAnggaran" + RND]();
    el("prKunciInfo").classList.add("d-none");
    el("prSimpan").classList.remove("d-none");
    el("prTambahBaris").classList.remove("d-none");
    el("prModalJudul").textContent = id ? "Permintaan Pembelian" : "Buat Permintaan Pembelian";
    var modal = new bootstrap.Modal(document.getElementById("prModal" + RND));
    if (!id){ renderBaris(false); modal.show(); return; }
    api({action:"pengadaan_pr_detail", id:id}).then(function(d){
      if (d.status !== "00" && d.status !== "success"){ pesan(d.description || "Gagal memuat detail.", false); return; }
      prAktif = d.header || {};
      var st = prAktif.status || "DRAFT";
      var terkunci = st !== "DRAFT";
      el("prModalJudul").textContent = "PR " + (prAktif.kode || "") + "  -  " + st;
      el("prKeterangan").value = prAktif.keterangan || "";
      el("prKeterangan").disabled = terkunci;
      el("prTanpaAnggaran").checked = prAktif.tanpaAnggaran === true;
      el("prTanpaAnggaran").disabled = terkunci;
      setelAnggaranAwal(prAktif.workspace_id || null, prAktif.anggaran || "");
      el("prTombolAnggaran").disabled = terkunci;
      window["prGantiAnggaran" + RND]();
      baris = (d.detail || []).map(function(x){
        return { produk_id: x.produk_id, nama: x.produk, jumlah: angka(x.jumlah), harga: angka(x.hargaBeli) };
      });
      if (terkunci){
        el("prKunciInfo").textContent = "PR berstatus " + st + " tidak dapat diubah. Batalkan keputusannya terlebih dahulu bila memang perlu dikoreksi.";
        el("prKunciInfo").classList.remove("d-none");
        el("prSimpan").classList.add("d-none");
        el("prTambahBaris").classList.add("d-none");
      }
      renderBaris(terkunci);
      modal.show();
    });
  };

  window["prSimpan" + RND] = function(){
    if (!baris.length){ pesan("Tambahkan minimal satu baris barang.", false); return; }
    var tanpaAnggaran = el("prTanpaAnggaran").checked;
    var anggaranId = el("prAnggaran").value;
    if (!tanpaAnggaran && !anggaranId){
      pesan("Anggaran wajib dipilih. Centang \"Tanpa anggaran\" bila permintaan ini memang tidak membebani anggaran.", false);
      return;
    }
    var payload = { action:"pengadaan_pr_simpan", keterangan: el("prKeterangan").value.trim(),
                    tanpaAnggaran: tanpaAnggaran,
                    detail: baris.map(function(b){ return { produk_id: b.produk_id, jumlah: b.jumlah, hargaBeli: b.harga }; }) };
    if (!tanpaAnggaran && anggaranId) payload.workspace_id = anggaranId;
    if (prAktif && prAktif.id) payload.id = prAktif.id;
    api(payload).then(function(d){
      var ok = d.status === "00" || d.status === "success";
      pesan(ok ? ("PR tersimpan: " + (d.kode || "")) : (d.description || "Gagal menyimpan."), ok);
      if (ok){
        bootstrap.Modal.getInstance(document.getElementById("prModal" + RND)).hide();
        window["prMuat" + RND](halaman);
      }
    });
  };

  // ---------- Keputusan & hapus ----------
  window["prPutusan" + RND] = function(id, keputusan){
    var alasan = "";
    if (keputusan === "TOLAK"){
      alasan = window.prompt("Alasan penolakan (minimal 5 karakter, dibaca pembuat PR):", "");
      if (alasan === null) return;
      alasan = alasan.trim();
      if (alasan.length < 5){ pesan("Alasan penolakan minimal 5 karakter.", false); return; }
    }
    api({action:"pengadaan_pr_putusan", id:id, keputusan:keputusan, alasan:alasan}).then(function(d){
      var ok = d.status === "00" || d.status === "success";
      pesan(ok ? ("Keputusan tersimpan: " + (d.statusPr || keputusan)) : (d.description || "Gagal menyimpan keputusan."), ok);
      if (ok) window["prMuat" + RND](halaman);
    });
  };

  window["prHapus" + RND] = function(id, kode){
    if (!window.confirm("Hapus PR " + kode + "? Hanya PR berstatus DRAFT yang dapat dihapus.")) return;
    api({action:"pengadaan_pr_hapus", id:id}).then(function(d){
      var ok = d.status === "00" || d.status === "success";
      pesan(ok ? "PR dihapus." : (d.description || "Gagal menghapus."), ok);
      if (ok) window["prMuat" + RND](halaman);
    });
  };

  // ---------- Pencarian produk ----------
  window["prCariProduk" + RND] = function(){
    el("prProdukCari").value = "";
    el("prProdukHasil").innerHTML = "";
    new bootstrap.Modal(document.getElementById("prProdukModal" + RND)).show();
  };
  window["prProdukMuat" + RND] = function(){
    var q = el("prProdukCari").value.trim();
    api({action:"katalog", cari:q, pageSize:50}).then(function(d){
      var rows = d.data || [];
      if (!rows.length){ el("prProdukHasil").innerHTML = '<div class="text-muted small py-2">Tidak ada produk ditemukan.</div>'; return; }
      var h = "";
      for (var i=0;i<rows.length;i++){
        var p = rows[i];
        h += '<a href="javascript:void(0)" class="list-group-item list-group-item-action"'
           + ' onclick="prPilihProduk' + RND + '(' + p.id + ',\'' + esc(String(p.nama).replace(/'/g,"")) + '\',' + (p.hargaBeli||0) + ')">'
           + '<div class="fw-bold">' + esc(p.nama) + '</div>'
           + '<div class="small text-muted">' + esc(p.kode || "") + ' - Modal ' + rp(p.hargaBeli) + '</div></a>';
      }
      el("prProdukHasil").innerHTML = h;
    });
  };
  window["prPilihProduk" + RND] = function(id, nama, hargaBeli){
    baris.push({ produk_id:id, nama:nama, jumlah:1, harga:angka(hargaBeli) });
    renderBaris(false);
    bootstrap.Modal.getInstance(document.getElementById("prProdukModal" + RND)).hide();
  };

  // Muat pertama kali
  window["prMuat" + RND](1);
})();
</script>
