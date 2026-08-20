<%@ page isELIgnored="true" %>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// Halaman "Bulk Entry Pengadaan" -- mengikuti skema Bulk Entry Kulakan:
// Header, tempel/Excel, tabel item, lalu Review sebelum dokumen dibuat.
//
// Satu halaman melayani PR, PO, dan BAST karena bentuk pekerjaannya identik;
// hanya isi header dan aksi simpannya yang berbeda. Pencocokan baris dilakukan
// SERVER lewat pengadaan_barang_resolve sehingga aturannya sama untuk semua kanal.
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
String jenisAwal = request.getParameter("jenis");
if (jenisAwal == null || !(jenisAwal.equals("pr") || jenisAwal.equals("po") || jenisAwal.equals("bast"))) {
	jenisAwal = "pr";
}
%>
<div class="container-fluid px-0">
  <div class="d-flex justify-content-between align-items-center mb-3">
    <div>
      <h4 class="fw-bold mb-0" id="bkJudul<%=rnd%>"><%=Common.getBahasaConfig("Bulk Entry Pengadaan")%></h4>
      <div class="text-muted small" id="bkSub<%=rnd%>"></div>
    </div>
    <div style="min-width:260px">
      <select id="bkJenis<%=rnd%>" class="form-select" onchange="bkGantiJenis<%=rnd%>()">
        <option value="pr"><%=Common.getBahasaConfig("Permintaan Pembelian (PR)")%></option>
        <option value="po"><%=Common.getBahasaConfig("Pemesanan Pembelian (PO)")%></option>
        <option value="bast"><%=Common.getBahasaConfig("Penerimaan Barang (BAST)")%></option>
      </select>
    </div>
  </div>

  <div class="card border-0 shadow-sm mb-3">
    <div class="card-body">
      <h6 class="fw-bold mb-1"><%=Common.getBahasaConfig("Header Dokumen")%></h6>
      <div class="text-muted small mb-3">
        <%=Common.getBahasaConfig("Data belum menjadi dokumen sampai Anda menekan Simpan di bagian Review.")%>
      </div>
      <div class="row g-2">
        <div class="col-md-5" id="bkBarisPenyedia<%=rnd%>">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Penyedia / Vendor")%> *</label>
          <div class="input-group">
            <input type="text" id="bkPenyediaNama<%=rnd%>" class="form-control" readonly
                   placeholder="<%=Common.getBahasaConfig("Belum dipilih")%>">
            <button class="btn btn-outline-secondary" onclick="bkCariPenyedia<%=rnd%>()"><i class="fas fa-search"></i></button>
          </div>
        </div>
        <div class="col-md-3 d-none" id="bkBarisInvoice<%=rnd%>">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("No. invoice / referensi")%></label>
          <input type="text" id="bkKodeInvoice<%=rnd%>" class="form-control">
        </div>
        <div class="col-md-2 d-none" id="bkBarisKirim<%=rnd%>">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Kirim paling lambat")%></label>
          <input type="date" id="bkKirim<%=rnd%>" class="form-control">
        </div>
        <div class="col-md-3 d-none" id="bkBarisTagihan<%=rnd%>">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("No. tagihan / faktur")%></label>
          <input type="text" id="bkKodeTagihan<%=rnd%>" class="form-control">
        </div>
        <div class="col-md-2 d-none" id="bkBarisTglTagihan<%=rnd%>">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Tanggal tagihan")%></label>
          <input type="date" id="bkTglTagihan<%=rnd%>" class="form-control">
        </div>
        <div class="col-md-2 d-none" id="bkBarisKurir<%=rnd%>">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Kurir")%></label>
          <input type="text" id="bkKurir<%=rnd%>" class="form-control">
        </div>
        <div class="col-12">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Keterangan")%></label>
          <input type="text" id="bkKeterangan<%=rnd%>" class="form-control">
        </div>
      </div>
    </div>
  </div>

  <div class="card border-0 shadow-sm mb-3">
    <div class="card-body">
      <h6 class="fw-bold mb-1"><%=Common.getBahasaConfig("Excel / Tempel Draf")%></h6>
      <div class="text-muted small mb-2">
        <%=Common.getBahasaConfig("Urutan kolom: kode/barcode, nama, jumlah, harga beli. Pemisah boleh TAB, titik koma, atau koma.")%>
      </div>
      <textarea id="bkTempel<%=rnd%>" class="form-control mb-2" rows="4"
                placeholder="<%=Common.getBahasaConfig("Tempel baris di sini")%>"></textarea>
      <div class="d-flex flex-wrap gap-2">
        <button class="btn btn-sm btn-outline-secondary" onclick="bkUnduhFormat<%=rnd%>()">
          <i class="fas fa-download me-1"></i><%=Common.getBahasaConfig("Unduh Format CSV")%>
        </button>
        <button class="btn btn-sm btn-outline-secondary" onclick="document.getElementById('bkBerkas' + '<%=rnd%>').click()">
          <i class="fas fa-file-upload me-1"></i><%=Common.getBahasaConfig("Unggah CSV / Excel")%>
        </button>
        <input type="file" id="bkBerkas<%=rnd%>" accept=".csv,.txt,.xlsx" class="d-none"
               onchange="bkUnggah<%=rnd%>(this)">
        <button class="btn btn-sm btn-outline-secondary" onclick="bkTambahDariTempelan<%=rnd%>()">
          <i class="fas fa-paste me-1"></i><%=Common.getBahasaConfig("Tambahkan dari Tempelan")%>
        </button>
        <button class="btn btn-sm btn-outline-primary" onclick="bkCekProduk<%=rnd%>()">
          <i class="fas fa-check-double me-1"></i><%=Common.getBahasaConfig("Cek Produk")%>
        </button>
        <button class="btn btn-sm btn-outline-secondary" onclick="bkTambahKosong<%=rnd%>()">
          <i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah Baris Kosong")%>
        </button>
        <button class="btn btn-sm btn-outline-danger" onclick="bkReset<%=rnd%>()">
          <i class="fas fa-redo me-1"></i><%=Common.getBahasaConfig("Reset Draf")%>
        </button>
      </div>
    </div>
  </div>

  <div class="card border-0 shadow-sm mb-3">
    <div class="card-body pb-0">
      <h6 class="fw-bold mb-2"><%=Common.getBahasaConfig("Data Item")%>
        <span id="bkJumlahBaris<%=rnd%>" class="text-muted small"></span></h6>
    </div>
    <div class="table-responsive">
      <table class="table table-sm align-middle mb-0">
        <thead class="table-light">
          <tr>
            <th style="width:50px"><%=Common.getBahasaConfig("No")%></th>
            <th style="width:160px"><%=Common.getBahasaConfig("Kode / Barcode")%></th>
            <th><%=Common.getBahasaConfig("Nama Barang")%></th>
            <th style="width:100px"><%=Common.getBahasaConfig("Jumlah")%></th>
            <th style="width:130px"><%=Common.getBahasaConfig("Harga")%></th>
            <th class="text-end" style="width:140px"><%=Common.getBahasaConfig("Subtotal")%></th>
            <th style="width:110px"><%=Common.getBahasaConfig("Status")%></th>
            <th style="width:50px"></th>
          </tr>
        </thead>
        <tbody id="bkTbody<%=rnd%>"></tbody>
      </table>
    </div>
  </div>

  <div class="card border-0 shadow-sm mb-4">
    <div class="card-body">
      <h6 class="fw-bold mb-1"><%=Common.getBahasaConfig("Review & Simpan")%></h6>
      <div class="text-muted small mb-3"><%=Common.getBahasaConfig("Periksa ringkasan berikut sebelum dokumen dibuat.")%></div>
      <div class="d-flex flex-wrap gap-3 mb-3" id="bkRingkas<%=rnd%>"></div>
      <div id="bkHalangan<%=rnd%>"></div>
      <div class="text-end mt-3">
        <button class="btn btn-primary" id="bkSimpan<%=rnd%>" onclick="bkSimpan<%=rnd%>()">
          <i class="fas fa-save me-1"></i><%=Common.getBahasaConfig("Simpan Dokumen")%>
        </button>
      </div>
    </div>
  </div>
</div>

<div class="modal fade" id="bkPenyediaModal<%=rnd%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title"><%=Common.getBahasaConfig("Pilih Penyedia / Vendor")%></h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <input type="text" id="bkPenyediaCari<%=rnd%>" class="form-control mb-2"
               placeholder="<%=Common.getBahasaConfig("Cari kode / nama penyedia")%>"
               onkeydown="if(event.key==='Enter')bkPenyediaMuat<%=rnd%>()">
        <div id="bkPenyediaHasil<%=rnd%>" class="list-group"></div>
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
  var jenis = "<%=jenisAwal%>";
  var baris = [], penyediaId = null, sibuk = false;

  function el(id){ return document.getElementById(id + RND); }
  function esc(s){ return (s==null?"":String(s)).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;"); }
  function rp(n){ return "Rp " + (Number(n)||0).toLocaleString("id-ID"); }
  function angka(v){ v = String(v==null?"":v).replace(/[^0-9.]/g,""); return Number(v)||0; }
  function keTampilan(iso){
    if (!iso) return "";
    var p = String(iso).split("-");
    return p.length === 3 ? (p[2] + "-" + p[1] + "-" + p[0]) : "";
  }
  function api(payload){
    return fetch(DATA_URL, {method:"POST", headers:{"Content-Type":"application/json"},
                            body: JSON.stringify(payload)}).then(function(r){ return r.json(); });
  }
  function pesan(teks, sukses){
    if (typeof tampilkanToast === "function") tampilkanToast(teks, sukses ? "bg-success text-white" : "bg-danger text-white");
    else alert(teks);
  }

  var JUDUL = {
    pr: ["Bulk Entry Permintaan Pembelian", "Draf permintaan massal; tersimpan sebagai PR berstatus DRAFT."],
    po: ["Bulk Entry Pemesanan Pembelian", "Draf pesanan massal; tersimpan sebagai PO berstatus DRAFT."],
    bast: ["Bulk Entry Penerimaan Barang", "Draf penerimaan massal tanpa PO; tersimpan sebagai BAST DRAFT."]
  };
  var AKSI = { pr: "pengadaan_pr_simpan", po: "pengadaan_po_simpan", bast: "pengadaan_bast_simpan" };

  window["bkGantiJenis" + RND] = function(){
    jenis = el("bkJenis").value;
    terapkanJenis();
    render();
  };
  function terapkanJenis(){
    el("bkJudul").textContent = JUDUL[jenis][0];
    el("bkSub").textContent = JUDUL[jenis][1];
    // PR tidak menyebut vendor; PO memakai invoice & batas kirim; BAST memakai faktur & kurir.
    el("bkBarisPenyedia").classList.toggle("d-none", jenis === "pr");
    el("bkBarisInvoice").classList.toggle("d-none", jenis !== "po");
    el("bkBarisKirim").classList.toggle("d-none", jenis !== "po");
    el("bkBarisTagihan").classList.toggle("d-none", jenis !== "bast");
    el("bkBarisTglTagihan").classList.toggle("d-none", jenis !== "bast");
    el("bkBarisKurir").classList.toggle("d-none", jenis !== "bast");
  }

  function pecah(t){
    if (t.indexOf("\t") >= 0) return t.split("\t");
    if (t.indexOf(";") >= 0) return t.split(";");
    return t.split(",");
  }
  window["bkTambahDariTempelan" + RND] = function(){
    var teks = el("bkTempel").value.replace(/\r/g, "").trim();
    if (!teks){ pesan("Tempelkan dulu baris fakturnya.", false); return; }
    var n = 0, garis = teks.split("\n");
    for (var i=0;i<garis.length;i++){
      var t = garis[i].trim();
      if (!t) continue;
      var k = pecah(t);
      function amb(j){ return j < k.length ? String(k[j]).trim() : ""; }
      if (!amb(0) && !amb(1)) continue;
      baris.push({ kode: amb(0), nama: amb(1), jumlah: angka(amb(2)) || 1,
                   harga: angka(amb(3)), produk_id: null, status: "BARU", catatan: "" });
      n++;
    }
    el("bkTempel").value = "";
    render();
    pesan(n + " baris ditambahkan ke draf. Tekan \"Cek Produk\" untuk mencocokkan.", true);
  };
  window["bkTambahKosong" + RND] = function(){
    baris.push({ kode:"", nama:"", jumlah:1, harga:0, produk_id:null, status:"BARU", catatan:"" });
    render();
  };
  window["bkReset" + RND] = function(){
    if (baris.length && !window.confirm("Kosongkan seluruh draf?")) return;
    baris = []; render(); pesan("Draf dikosongkan.", true);
  };
  window["bkUbah" + RND] = function(i, field, nilai){
    baris[i][field] = (field === "kode" || field === "nama") ? nilai : angka(nilai);
    if (field === "kode" || field === "nama"){ baris[i].produk_id = null; baris[i].status = "BARU"; }
    render();
  };
  window["bkHapusBaris" + RND] = function(i){ baris.splice(i,1); render(); };

  window["bkCekProduk" + RND] = function(){
    if (!baris.length){ pesan("Belum ada baris untuk dicocokkan.", false); return; }
    if (sibuk) return;
    sibuk = true;
    var kirim = baris.map(function(b){
      return { kode: b.kode, nama: b.nama, jumlah: b.jumlah, hargaBeli: b.harga };
    });
    api({action:"pengadaan_barang_resolve", baris: kirim}).then(function(d){
      sibuk = false;
      if (d.status !== "00" && d.status !== "success"){ pesan(d.description || "Gagal mencocokkan baris.", false); return; }
      var rows = d.data || [];
      for (var i=0;i<rows.length && i<baris.length;i++){
        baris[i].status = rows[i].statusCocok || "TIDAK ADA";
        baris[i].catatan = rows[i].catatan || "";
        baris[i].produk_id = rows[i].produk_id || null;
        if (baris[i].produk_id){
          baris[i].kode = rows[i].kodeProduk || baris[i].kode;
          baris[i].nama = rows[i].namaProduk || baris[i].nama;
          if (!baris[i].harga) baris[i].harga = angka(rows[i].hargaBeli);
        }
      }
      render();
      pesan("Cocok " + (d.jumlahCocok||0) + ", ganda " + (d.jumlahGanda||0)
            + ", tidak ditemukan " + (d.jumlahTidakAda||0) + ".", true);
    }).catch(function(){ sibuk = false; pesan("Kesalahan koneksi saat mencocokkan.", false); });
  };

  function warnaStatus(b){
    if (b.produk_id) return "success";
    if (b.status === "GANDA") return "warning";
    if (b.status === "TIDAK ADA") return "danger";
    return "secondary";
  }
  function subtotal(b){ return angka(b.jumlah) * angka(b.harga); }
  function totalDraf(){
    var t = 0;
    for (var i=0;i<baris.length;i++) t += subtotal(baris[i]);
    return t;
  }
  function jumlahCocok(){
    var n = 0;
    for (var i=0;i<baris.length;i++) if (baris[i].produk_id) n++;
    return n;
  }
  function halangan(){
    var h = [];
    if (!baris.length) h.push("Belum ada baris barang.");
    if (jenis !== "pr" && !penyediaId) h.push("Penyedia/vendor belum dipilih.");
    var belum = baris.length - jumlahCocok();
    if (belum > 0) h.push(belum + " baris belum cocok dengan produk. Tekan Cek Produk lalu perbaiki kode atau namanya.");
    var nol = 0;
    for (var i=0;i<baris.length;i++) if (angka(baris[i].jumlah) <= 0) nol++;
    if (nol > 0) h.push(nol + " baris memiliki jumlah nol.");
    return h;
  }

  function render(){
    var tb = el("bkTbody");
    if (!baris.length){
      tb.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-3">'
        + 'Belum ada baris. Tempel, unggah, atau tambah baris kosong.</td></tr>';
    } else {
      var h = "";
      for (var i=0;i<baris.length;i++){
        var b = baris[i];
        h += '<tr>'
          + '<td class="text-muted small">' + (i+1) + '</td>'
          + '<td><input type="text" class="form-control form-control-sm" value="' + esc(b.kode) + '"'
          + ' onchange="bkUbah' + RND + '(' + i + ',\'kode\',this.value)"></td>'
          + '<td><input type="text" class="form-control form-control-sm" value="' + esc(b.nama) + '"'
          + ' onchange="bkUbah' + RND + '(' + i + ',\'nama\',this.value)"></td>'
          + '<td><input type="number" class="form-control form-control-sm" value="' + angka(b.jumlah) + '"'
          + ' oninput="bkUbah' + RND + '(' + i + ',\'jumlah\',this.value)"></td>'
          + '<td><input type="number" class="form-control form-control-sm" value="' + angka(b.harga) + '"'
          + ' oninput="bkUbah' + RND + '(' + i + ',\'harga\',this.value)"></td>'
          + '<td class="text-end">' + rp(subtotal(b)) + '</td>'
          + '<td><span class="badge bg-' + warnaStatus(b) + '" title="' + esc(b.catatan) + '">'
          + esc(b.produk_id ? "COCOK" : b.status) + '</span></td>'
          + '<td><button class="btn btn-sm btn-outline-danger" onclick="bkHapusBaris' + RND + '(' + i + ')">'
          + '<i class="fas fa-times"></i></button></td>'
          + '</tr>';
      }
      tb.innerHTML = h;
    }
    el("bkJumlahBaris").textContent = "(" + baris.length + " baris)";

    var kotak = [["Baris", baris.length], ["Cocok", jumlahCocok()],
                 ["Belum cocok", baris.length - jumlahCocok()], ["Subtotal", rp(totalDraf())]];
    var hr = "";
    for (var k=0;k<kotak.length;k++){
      hr += '<div class="border rounded px-3 py-2 text-center">'
         + '<div class="text-muted" style="font-size:10px">' + esc(kotak[k][0]) + '</div>'
         + '<div class="fw-bold">' + esc(kotak[k][1]) + '</div></div>';
    }
    el("bkRingkas").innerHTML = hr;

    var hal = halangan(), hh = "";
    if (!hal.length){
      hh = '<div class="alert alert-success small mb-0">Draf siap disimpan.</div>';
    } else {
      for (var m=0;m<hal.length;m++){
        hh += '<div class="alert alert-danger small mb-2">' + esc(hal[m]) + '</div>';
      }
    }
    el("bkHalangan").innerHTML = hh;
    el("bkSimpan").disabled = hal.length > 0;
  }

  window["bkUnduhFormat" + RND] = function(){
    var isi = "kode_barcode,nama_barang,jumlah,harga_beli\n8999999999999,Contoh Barang,12,7500\n";
    var a = document.createElement("a");
    a.href = "data:text/csv;charset=utf-8," + encodeURIComponent(isi);
    a.download = "format-bulk-pengadaan.csv";
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  };

  // Unggah berkas ke draf. CSV/teks dibaca langsung; .xlsx dibaca sebagai ZIP
  // memakai DecompressionStream bawaan peramban, lalu sharedStrings dan sheet1
  // diambil seperlunya -- tanpa pustaka tambahan.
  window["bkUnggah" + RND] = function(input){
    var f = input.files && input.files[0];
    input.value = "";
    if (!f) return;
    var nama = String(f.name || "").toLowerCase();
    if (nama.indexOf(".xlsx") === nama.length - 5 && nama.length > 5) {
      bacaXlsx(f);
    } else {
      var pembaca = new FileReader();
      pembaca.onload = function(){ tambahTeks(String(pembaca.result || "")); };
      pembaca.readAsText(f);
    }
  };

  function tambahTeks(teks){
    el("bkTempel").value = teks.replace(/\r/g, "");
    window["bkTambahDariTempelan" + RND]();
  }

  function bacaXlsx(f){
    if (typeof DecompressionStream === "undefined"){
      pesan("Peramban ini belum mendukung pembacaan .xlsx. Simpan sebagai CSV lalu unggah ulang.", false);
      return;
    }
    f.arrayBuffer().then(function(buf){
      return ambilBerkasZip(new Uint8Array(buf));
    }).then(function(isi){
      var sheet = isi["xl/worksheets/sheet1.xml"];
      if (!sheet){ pesan("Lembar pertama tidak ditemukan di berkas Excel.", false); return; }
      var teksBersama = uraiSharedStrings(isi["xl/sharedStrings.xml"] || "");
      tambahTeks(uraiSheet(sheet, teksBersama));
    }).catch(function(){
      pesan("Gagal membaca berkas Excel. Simpan sebagai CSV lalu unggah ulang.", false);
    });
  }

  /** Ambil isi berkas di dalam ZIP (stored atau deflate) sebagai teks. */
  function ambilBerkasZip(b){
    var dv = new DataView(b.buffer, b.byteOffset, b.byteLength);
    var tugas = [], hasil = {};
    var i = 0;
    while (i + 4 <= b.length && dv.getUint32(i, true) === 0x04034b50) {
      var metode = dv.getUint16(i + 8, true);
      var ukuranKompres = dv.getUint32(i + 18, true);
      var panjangNama = dv.getUint16(i + 26, true);
      var panjangExtra = dv.getUint16(i + 28, true);
      var awalNama = i + 30;
      var nama = new TextDecoder().decode(b.subarray(awalNama, awalNama + panjangNama));
      var awalData = awalNama + panjangNama + panjangExtra;
      var data = b.subarray(awalData, awalData + ukuranKompres);
      i = awalData + ukuranKompres;
      if (nama !== "xl/worksheets/sheet1.xml" && nama !== "xl/sharedStrings.xml") continue;
      tugas.push(bukaData(nama, data, metode, hasil));
    }
    return Promise.all(tugas).then(function(){ return hasil; });
  }

  function bukaData(nama, data, metode, hasil){
    if (metode === 0){
      hasil[nama] = new TextDecoder().decode(data);
      return Promise.resolve();
    }
    var aliran = new Blob([data]).stream().pipeThrough(new DecompressionStream("deflate-raw"));
    return new Response(aliran).text().then(function(t){ hasil[nama] = t; });
  }

  function uraiSharedStrings(xml){
    var daftar = [];
    if (!xml) return daftar;
    var potongan = xml.split("<si>");
    for (var i = 1; i < potongan.length; i++){
      var teks = "";
      var cocok = potongan[i].match(/<t[^>]*>([\s\S]*?)<\/t>/g) || [];
      for (var j = 0; j < cocok.length; j++){
        teks += cocok[j].replace(/<[^>]+>/g, "");
      }
      daftar.push(bukaEntitas(teks));
    }
    return daftar;
  }

  function bukaEntitas(t){
    return String(t).replace(/&lt;/g, "<").replace(/&gt;/g, ">")
                    .replace(/&quot;/g, '"').replace(/&apos;/g, "'")
                    .replace(/&amp;/g, "&");
  }

  /** Ubah sheet XML menjadi teks berpemisah TAB agar masuk ke jalur tempelan yang sama. */
  function uraiSheet(xml, teksBersama){
    var keluar = [];
    var barisXml = xml.split("<row");
    for (var i = 1; i < barisXml.length; i++){
      var sel = barisXml[i].match(/<c[^>]*>[\s\S]*?<\/c>|<c[^>]*\/>/g) || [];
      var kolom = [];
      for (var j = 0; j < sel.length; j++){
        var s = sel[j];
        var nilai = "";
        var m = s.match(/<v>([\s\S]*?)<\/v>/);
        if (m){
          nilai = m[1];
          if (s.indexOf('t="s"') >= 0){
            var idx = parseInt(nilai, 10);
            nilai = (teksBersama[idx] === undefined) ? "" : teksBersama[idx];
          } else {
            nilai = bukaEntitas(nilai);
          }
        } else {
          var mi = s.match(/<is>[\s\S]*?<t[^>]*>([\s\S]*?)<\/t>[\s\S]*?<\/is>/);
          if (mi) nilai = bukaEntitas(mi[1]);
        }
        kolom.push(String(nilai).replace(/\t/g, " "));
      }
      var baris = kolom.join("\t");
      if (baris.replace(/\t/g, "").trim() !== "") keluar.push(baris);
    }
    // Baris pertama pada format unduhan adalah judul kolom, jadi dilewati.
    if (keluar.length > 1 && /kode|nama|jumlah|harga/i.test(keluar[0])) keluar.shift();
    return keluar.join("\n");
  }

  window["bkCariPenyedia" + RND] = function(){
    el("bkPenyediaCari").value = "";
    el("bkPenyediaHasil").innerHTML = "";
    new bootstrap.Modal(document.getElementById("bkPenyediaModal" + RND)).show();
    window["bkPenyediaMuat" + RND]();
  };
  window["bkPenyediaMuat" + RND] = function(){
    api({action:"pengadaan_penyedia_cari", keyword: el("bkPenyediaCari").value.trim(), limit:50}).then(function(d){
      var rows = d.data || [];
      if (!rows.length){ el("bkPenyediaHasil").innerHTML = '<div class="text-muted small py-2">Tidak ada penyedia ditemukan.</div>'; return; }
      var h = "";
      for (var i=0;i<rows.length;i++){
        var p = rows[i];
        h += '<a href="javascript:void(0)" class="list-group-item list-group-item-action"'
           + ' onclick="bkPilihPenyedia' + RND + '(' + p.id + ',\'' + esc(String(p.nama).replace(/'/g,"")) + '\')">'
           + '<div class="fw-bold">' + esc(p.nama) + '</div>'
           + '<div class="small text-muted">' + esc(p.kode || "") + '</div></a>';
      }
      el("bkPenyediaHasil").innerHTML = h;
    });
  };
  window["bkPilihPenyedia" + RND] = function(id, nama){
    penyediaId = id;
    el("bkPenyediaNama").value = nama;
    bootstrap.Modal.getInstance(document.getElementById("bkPenyediaModal" + RND)).hide();
    render();
  };

  window["bkSimpan" + RND] = function(){
    var hal = halangan();
    if (hal.length){ pesan(hal[0], false); return; }
    if (sibuk) return;
    sibuk = true;
    var detail = [];
    for (var i=0;i<baris.length;i++){
      var b = baris[i];
      if (!b.produk_id) continue;
      var o = { produk_id: b.produk_id, hargaBeli: angka(b.harga) };
      if (jenis === "bast") o.diterima = angka(b.jumlah); else o.jumlah = angka(b.jumlah);
      detail.push(o);
    }
    var payload = { action: AKSI[jenis], keterangan: el("bkKeterangan").value.trim(), detail: detail };
    if (penyediaId) payload.penyedia_id = penyediaId;
    if (jenis === "po"){
      payload.kodeInvoice = el("bkKodeInvoice").value.trim();
      payload.pengirimanPalingLambat = keTampilan(el("bkKirim").value);
    }
    if (jenis === "bast"){
      payload.kodeTagihan = el("bkKodeTagihan").value.trim();
      payload.tanggalTagihan = keTampilan(el("bkTglTagihan").value);
      payload.kurir = el("bkKurir").value.trim();
    }
    api(payload).then(function(d){
      sibuk = false;
      var ok = d.status === "00" || d.status === "success";
      pesan(ok ? ("Tersimpan sebagai " + (d.kode || "")) : (d.description || "Gagal menyimpan."), ok);
      if (ok){ baris = []; render(); }
    }).catch(function(){ sibuk = false; pesan("Kesalahan koneksi saat menyimpan.", false); });
  };

  el("bkJenis").value = jenis;
  terapkanJenis();
  render();
})();
</script>
