<%@ page isELIgnored="true" %>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// Halaman "Terima Tagihan Vendor" -- modul Pengadaan POS versi JSP.
// Pada model pengadaan yang sudah ada, menerima tagihan bukan dokumen tersendiri
// melainkan tahap di atas BAST: nomor dan tanggal faktur dicapkan pada penerimaan
// yang sudah disetujui. Aturannya berada di server PengadaanPosApiHelper yang
// dipakai bersama Desktop/Android.
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
  <div class="mb-3">
    <h4 class="fw-bold mb-0"><%=Common.getBahasaConfig("Terima Tagihan Vendor")%></h4>
    <div class="text-muted small"><%=Common.getBahasaConfig("Catat faktur vendor atas barang yang sudah diterima")%></div>
  </div>

  <div class="card border-0 shadow-sm mb-3">
    <div class="card-body py-3">
      <div class="row g-2 align-items-end">
        <div class="col-md-5">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Cari kode BAST / keterangan / no. faktur")%></label>
          <input type="text" id="tgCari<%=rnd%>" class="form-control" onkeydown="if(event.key==='Enter')tgMuat<%=rnd%>(1)">
        </div>
        <div class="col-md-3">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Status tagihan")%></label>
          <select id="tgStatus<%=rnd%>" class="form-select" onchange="tgMuat<%=rnd%>(1)">
            <option value=""><%=Common.getBahasaConfig("Semua")%></option>
            <option value="BELUM"><%=Common.getBahasaConfig("Belum ditagih")%></option>
            <option value="SUDAH"><%=Common.getBahasaConfig("Sudah ditagih")%></option>
          </select>
        </div>
        <div class="col-md-2">
          <button class="btn btn-outline-secondary w-100" onclick="tgMuat<%=rnd%>(1)">
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
            <th><%=Common.getBahasaConfig("BAST")%></th>
            <th><%=Common.getBahasaConfig("Tanggal")%></th>
            <th><%=Common.getBahasaConfig("Penyedia")%></th>
            <th><%=Common.getBahasaConfig("PO")%></th>
            <th class="text-end"><%=Common.getBahasaConfig("Nilai")%></th>
            <th><%=Common.getBahasaConfig("Faktur")%></th>
            <th class="text-center"><%=Common.getBahasaConfig("Aksi")%></th>
          </tr>
        </thead>
        <tbody id="tgTbody<%=rnd%>">
          <tr><td colspan="7" class="text-center text-muted py-4"><%=Common.getBahasaConfig("Memuat data...")%></td></tr>
        </tbody>
      </table>
    </div>
    <div class="card-footer bg-white d-flex justify-content-between align-items-center">
      <span class="small text-muted" id="tgInfo<%=rnd%>"></span>
      <div class="btn-group">
        <button class="btn btn-sm btn-outline-secondary" id="tgPrev<%=rnd%>" onclick="tgHal<%=rnd%>(-1)">&laquo;</button>
        <button class="btn btn-sm btn-outline-secondary" id="tgNext<%=rnd%>" onclick="tgHal<%=rnd%>(1)">&raquo;</button>
      </div>
    </div>
  </div>
</div>

<div class="modal fade" id="tgModal<%=rnd%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="tgModalJudul<%=rnd%>"><%=Common.getBahasaConfig("Terima Tagihan")%></h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <div class="small text-muted mb-3" id="tgRingkas<%=rnd%>"></div>
        <div class="mb-3">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Nomor tagihan / faktur vendor")%> *</label>
          <input type="text" id="tgKode<%=rnd%>" class="form-control">
          <div class="form-text"><%=Common.getBahasaConfig("Sesuai dokumen tagihan yang diterima")%></div>
        </div>
        <div class="mb-2">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Tanggal tagihan")%> *</label>
          <input type="date" id="tgTanggal<%=rnd%>" class="form-control">
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
        <button type="button" class="btn btn-primary" onclick="tgSimpan<%=rnd%>()"><%=Common.getBahasaConfig("Terima Tagihan")%></button>
      </div>
    </div>
  </div>
</div>


<%-- Tombol Bantuan mengambang; isinya sepadan dengan bantuan Desktop/Android. --%>
<jsp:include page="/WEB-INF/baru/include/bantuan_pengadaan.jsp">
  <jsp:param name="tahap" value="tagihan"/>
</jsp:include>

<script>
(function(){
  var RND = "<%=rnd%>";
  var DATA_URL = "<%=Common.ROOT%>/Data";
  var halaman = 1, totalHal = 1, aktif = null;

  function el(id){ return document.getElementById(id + RND); }
  function esc(s){ return (s==null?"":String(s)).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;"); }
  function rp(n){ return "Rp " + (Number(n)||0).toLocaleString("id-ID"); }

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

  function api(payload){
    return fetch(DATA_URL, {method:"POST", headers:{"Content-Type":"application/json"},
                            body: JSON.stringify(payload)})
      .then(function(r){ return r.json(); });
  }
  function pesan(teks, sukses){
    if (typeof tampilkanToast === "function") tampilkanToast(teks, sukses ? "bg-success text-white" : "bg-danger text-white");
    else alert(teks);
  }

  window["tgMuat" + RND] = function(hal){
    halaman = hal || halaman;
    var tbody = el("tgTbody");
    tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">Memuat data...</td></tr>';
    api({action:"pengadaan_tagihan_daftar", cari: el("tgCari").value.trim(),
         status: el("tgStatus").value, page: halaman, pageSize: 15})
      .then(function(d){
        if (d.status !== "00" && d.status !== "success") {
          tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger py-4">' + esc(d.description || "Gagal memuat data.") + '</td></tr>';
          return;
        }
        var rows = d.data || [];
        totalHal = Math.max(1, Math.ceil((d.total || rows.length) / 15));
        if (!rows.length) {
          tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">'
            + 'Belum ada penerimaan barang yang siap ditagihkan. Setujui dulu BAST-nya di menu Penerimaan Barang.</td></tr>';
        } else {
          var html = "";
          for (var i=0;i<rows.length;i++){
            var r = rows[i], sudah = (r.status === "SUDAH");
            var faktur = sudah
              ? '<div class="fw-bold text-success">' + esc(r.kodeTagihan) + '</div>'
                + '<div class="small text-muted">' + esc(r.tanggalTagihan) + '</div>'
              : '<span class="badge bg-warning">BELUM DITAGIH</span>';
            var aksi = '<button class="btn btn-sm btn-outline-success me-1" title="' + (sudah ? 'Ubah data faktur' : 'Terima tagihan') + '"'
              + ' onclick="tgForm' + RND + '(' + r.id + ')"><i class="fas fa-file-invoice-dollar"></i></button>';
            if (sudah) {
              aksi += '<button class="btn btn-sm btn-outline-secondary" title="Batalkan tagihan"'
                + ' onclick="tgBatal' + RND + '(' + r.id + ',\'' + esc(String(r.kodeTagihan).replace(/'/g,"")) + '\')"><i class="fas fa-undo"></i></button>';
            }
            html += '<tr>'
              + '<td class="fw-bold">' + esc(r.kode) + '</td>'
              + '<td>' + esc(r.tanggal || "-") + '</td>'
              + '<td>' + esc(r.penyedia || "-") + '</td>'
              + '<td class="small">' + esc(r.po || "-") + '</td>'
              + '<td class="text-end">' + rp(r.nilai) + '</td>'
              + '<td>' + faktur + '</td>'
              + '<td class="text-center">' + aksi + '</td>'
              + '</tr>';
          }
          tbody.innerHTML = html;
        }
        el("tgInfo").textContent = "Halaman " + halaman + " dari " + totalHal + " - total " + (d.total || rows.length) + " dokumen";
        el("tgPrev").disabled = halaman <= 1;
        el("tgNext").disabled = halaman >= totalHal;
        window["tgData" + RND] = rows;
      })
      .catch(function(){ tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger py-4">Kesalahan koneksi.</td></tr>'; });
  };

  window["tgHal" + RND] = function(delta){
    var baru = halaman + delta;
    if (baru < 1 || baru > totalHal) return;
    window["tgMuat" + RND](baru);
  };

  window["tgForm" + RND] = function(id){
    var rows = window["tgData" + RND] || [];
    aktif = null;
    for (var i=0;i<rows.length;i++){ if (rows[i].id === id) aktif = rows[i]; }
    if (!aktif) return;
    el("tgModalJudul").textContent = (aktif.status === "SUDAH" ? "Ubah Data Faktur - " : "Terima Tagihan - ") + (aktif.kode || "");
    el("tgRingkas").textContent = (aktif.penyedia || "-") + "  -  " + rp(aktif.nilai);
    el("tgKode").value = aktif.kodeTagihan || "";
    el("tgTanggal").value = keIsoTgl(aktif.tanggalTagihan || "");
    new bootstrap.Modal(document.getElementById("tgModal" + RND)).show();
  };

  window["tgSimpan" + RND] = function(){
    if (!aktif) return;
    var kode = el("tgKode").value.trim();
    var tgl = keTampilan(el("tgTanggal").value);
    if (!kode){ pesan("Nomor tagihan/faktur vendor wajib diisi.", false); return; }
    if (!tgl){ pesan("Tanggal tagihan wajib diisi.", false); return; }
    api({action:"pengadaan_tagihan_terima", id: aktif.id, kodeTagihan: kode, tanggalTagihan: tgl})
      .then(function(d){
        var ok = d.status === "00" || d.status === "success";
        pesan(ok ? ("Tagihan tersimpan: " + (d.kodeTagihan || "")) : (d.description || "Gagal menyimpan tagihan."), ok);
        if (ok){
          bootstrap.Modal.getInstance(document.getElementById("tgModal" + RND)).hide();
          window["tgMuat" + RND](halaman);
        }
      });
  };

  window["tgBatal" + RND] = function(id, kodeTagihan){
    if (!window.confirm("Batalkan tagihan " + kodeTagihan + "? Nomor dan tanggal fakturnya akan dikosongkan.")) return;
    api({action:"pengadaan_tagihan_batal", id:id}).then(function(d){
      var ok = d.status === "00" || d.status === "success";
      pesan(ok ? "Tagihan dibatalkan." : (d.description || "Gagal membatalkan tagihan."), ok);
      if (ok) window["tgMuat" + RND](halaman);
    });
  };

  // Muat pertama kali
  window["tgMuat" + RND](1);
})();
</script>
