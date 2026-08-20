<%@ page isELIgnored="true" %>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// Halaman "Barang Dalam Proses" -- modul Pengadaan POS versi JSP.
// Mengikuti arti versi ZKoss (BarangDalamProsesDashboard): sumbernya PENERIMAAN
// (BAST), bukan pesanan yang belum datang. Pandangan "Belum Datang" tetap
// disediakan sebagai tab kedua karena berguna memantau kiriman yang tertunda.
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
  <div class="mb-3">
    <h4 class="fw-bold mb-0"><%=Common.getBahasaConfig("Barang Dalam Proses")%></h4>
    <div class="text-muted small" id="bpSubjudul<%=rnd%>"><%=Common.getBahasaConfig("Rekap penerimaan barang (BAST) beserta nilai dan statusnya")%></div>
  </div>

  <div class="card border-0 shadow-sm mb-3">
    <div class="card-body py-3">
      <div class="btn-group mb-3" role="group">
        <input type="radio" class="btn-check" name="bpMode<%=rnd%>" id="bpModeBast<%=rnd%>" checked
               onchange="bpGantiMode<%=rnd%>(true)">
        <label class="btn btn-outline-primary btn-sm" for="bpModeBast<%=rnd%>">
          <i class="fas fa-box-open me-1"></i><%=Common.getBahasaConfig("Sudah Diterima (BAST)")%>
        </label>
        <input type="radio" class="btn-check" name="bpMode<%=rnd%>" id="bpModeBelum<%=rnd%>"
               onchange="bpGantiMode<%=rnd%>(false)">
        <label class="btn btn-outline-primary btn-sm" for="bpModeBelum<%=rnd%>">
          <i class="fas fa-truck me-1"></i><%=Common.getBahasaConfig("Belum Datang")%>
        </label>
      </div>
      <div class="row g-2 align-items-end">
        <div class="col-md-5">
          <label class="form-label small mb-1" id="bpCariLabel<%=rnd%>"><%=Common.getBahasaConfig("Cari kode BAST / vendor / uraian")%></label>
          <input type="text" id="bpCari<%=rnd%>" class="form-control" onkeydown="if(event.key==='Enter')bpMuat<%=rnd%>(1)">
        </div>
        <div class="col-md-5" id="bpFilterBast<%=rnd%>">
          <div class="form-check mt-4">
            <input class="form-check-input" type="checkbox" id="bpCip<%=rnd%>" onchange="bpMuat<%=rnd%>(1)">
            <label class="form-check-label small" for="bpCip<%=rnd%>">
              <%=Common.getBahasaConfig("Hanya Pekerjaan Dalam Pelaksanaan (CIP)")%>
            </label>
          </div>
          <div class="form-check">
            <input class="form-check-input" type="checkbox" id="bpBelumSetuju<%=rnd%>" onchange="bpMuat<%=rnd%>(1)">
            <label class="form-check-label small" for="bpBelumSetuju<%=rnd%>">
              <%=Common.getBahasaConfig("Belum disetujui")%>
            </label>
          </div>
        </div>
        <div class="col-md-3 d-none" id="bpFilterBelum<%=rnd%>">
          <div class="form-check mt-4">
            <input class="form-check-input" type="checkbox" id="bpTerlambat<%=rnd%>" onchange="bpMuat<%=rnd%>(1)">
            <label class="form-check-label small" for="bpTerlambat<%=rnd%>">
              <%=Common.getBahasaConfig("Hanya yang terlambat")%>
            </label>
          </div>
        </div>
        <div class="col-md-2">
          <button class="btn btn-outline-secondary w-100" onclick="bpMuat<%=rnd%>(1)">
            <i class="fas fa-search me-1"></i><%=Common.getBahasaConfig("Cari")%>
          </button>
        </div>
      </div>
    </div>
  </div>

  <div class="d-flex flex-wrap gap-3 mb-3" id="bpRingkas<%=rnd%>"></div>

  <div class="card border-0 shadow-sm">
    <div class="table-responsive">
      <table class="table table-hover align-middle mb-0">
        <thead class="table-light">
          <tr id="bpThead<%=rnd%>"></tr>
        </thead>
        <tbody id="bpTbody<%=rnd%>">
          <tr><td colspan="8" class="text-center text-muted py-4"><%=Common.getBahasaConfig("Memuat data...")%></td></tr>
        </tbody>
      </table>
    </div>
    <div class="card-footer bg-white d-flex justify-content-between align-items-center">
      <span class="small text-muted" id="bpInfo<%=rnd%>"></span>
      <div class="btn-group">
        <button class="btn btn-sm btn-outline-secondary" id="bpPrev<%=rnd%>" onclick="bpHal<%=rnd%>(-1)">&laquo;</button>
        <button class="btn btn-sm btn-outline-secondary" id="bpNext<%=rnd%>" onclick="bpHal<%=rnd%>(1)">&raquo;</button>
      </div>
    </div>
  </div>
</div>


<%-- Tombol Bantuan mengambang; isinya sepadan dengan bantuan Desktop/Android. --%>
<jsp:include page="/WEB-INF/baru/include/bantuan_pengadaan.jsp">
  <jsp:param name="tahap" value="bdp"/>
</jsp:include>

<script>
(function(){
  var RND = "<%=rnd%>";
  var DATA_URL = "<%=Common.ROOT%>/Data";
  var halaman = 1, totalHal = 1, modeBast = true;

  function el(id){ return document.getElementById(id + RND); }
  function rp(n){ return "Rp " + (Number(n)||0).toLocaleString("id-ID"); }
  function num(n){ n = Number(n)||0; return (n === Math.round(n)) ? String(Math.round(n)) : String(n); }
  function api(payload){
    return fetch(DATA_URL, {method:"POST", headers:{"Content-Type":"application/json"},
                            body: JSON.stringify(payload)}).then(function(r){ return r.json(); });
  }

  var KOLOM_BAST = ["BAST","Vendor","Lokasi","No. PO","Uraian","Nilai","Tanggal","Status"];
  var KOLOM_BELUM = ["PO","Penyedia","Barang","Dipesan","Diterima","Belum datang","Nilai","Batas kirim"];
  var RATA_BAST = ["","","","","","text-end","",""];
  var RATA_BELUM = ["","","","text-end","text-end","text-end","text-end",""];

  function gambarKepala(){
    var kol = modeBast ? KOLOM_BAST : KOLOM_BELUM;
    var rata = modeBast ? RATA_BAST : RATA_BELUM;
    var h = "";
    for (var i=0;i<kol.length;i++){
      h += '<th class="' + rata[i] + '">' + esc(kol[i]) + '</th>';
    }
    el("bpThead").innerHTML = h;
  }

  window["bpGantiMode" + RND] = function(bast){
    modeBast = bast;
    el("bpFilterBast").classList.toggle("d-none", !bast);
    el("bpFilterBelum").classList.toggle("d-none", bast);
    el("bpCariLabel").textContent = bast
      ? "Cari kode BAST / vendor / uraian" : "Cari kode PO / nama barang";
    el("bpSubjudul").textContent = bast
      ? "Rekap penerimaan barang (BAST) beserta nilai dan statusnya"
      : "Barang yang sudah dipesan tetapi belum diterima";
    window["bpMuat" + RND](1);
  };

  window["bpMuat" + RND] = function(hal){
    halaman = hal || halaman;
    gambarKepala();
    var tbody = el("bpTbody");
    tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">Memuat data...</td></tr>';
    var payload = { action:"pengadaan_bdp_daftar", cari: el("bpCari").value.trim(),
                    page: halaman, pageSize: 25 };
    if (modeBast){
      payload.hanyaCip = el("bpCip").checked;
      payload.hanyaBelumDisetujui = el("bpBelumSetuju").checked;
    } else {
      payload.mode = "belum_datang";
      payload.hanyaTerlambat = el("bpTerlambat").checked;
    }
    api(payload)
      .then(function(d){
        if (d.status !== "00" && d.status !== "success"){
          tbody.innerHTML = '<tr><td colspan="8" class="text-center text-danger py-4">' + esc(d.description || "Gagal memuat data.") + '</td></tr>';
          return;
        }
        var rows = d.data || [];
        totalHal = Math.max(1, Math.ceil((d.total || rows.length) / 25));
        if (!rows.length){
          tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">'
            + esc(d.catatan || (modeBast ? "Belum ada penerimaan barang (BAST) yang tercatat."
                                         : "Tidak ada barang yang belum datang.")) + '</td></tr>';
        } else {
          tbody.innerHTML = modeBast ? barisBast(rows) : barisBelum(rows);
        }
        el("bpRingkas").innerHTML = modeBast ? ringkasBast(d) : ringkasBelum(d);
        el("bpInfo").textContent = "Halaman " + halaman + " dari " + totalHal + " - total "
          + (d.total || rows.length) + (modeBast ? " penerimaan" : " baris");
        el("bpPrev").disabled = halaman <= 1;
        el("bpNext").disabled = halaman >= totalHal;
      })
      .catch(function(){ tbody.innerHTML = '<tr><td colspan="8" class="text-center text-danger py-4">Kesalahan koneksi.</td></tr>'; });
  };

  function barisBast(rows){
    var html = "";
    for (var i=0;i<rows.length;i++){
      var r = rows[i];
      var lencana = r.disetujui
        ? '<span class="badge bg-success-subtle text-success-emphasis">' + esc(r.status || "") + '</span>'
        : '<span class="badge bg-warning-subtle text-warning-emphasis">' + esc(r.status || "") + '</span>';
      var stok = r.sudahMasukStok
        ? '<div class="small text-success">sudah masuk stok</div>'
        : '<div class="small text-warning">belum masuk stok</div>';
      html += '<tr>'
        + '<td><div class="fw-bold">' + esc(r.kode || "-") + '</div>' + stok + '</td>'
        + '<td>' + esc(r.vendor || "-") + '</td>'
        + '<td>' + esc(r.lokasi || "-") + '</td>'
        + '<td>' + esc(r.po || "-") + '</td>'
        + '<td>' + esc(r.uraian || "-") + '</td>'
        + '<td class="text-end">' + rp(r.nilai) + '</td>'
        + '<td>' + esc(r.tanggal || "-") + '</td>'
        + '<td>' + lencana + '</td>'
        + '</tr>';
    }
    return html;
  }

  function barisBelum(rows){
    var html = "";
    for (var i=0;i<rows.length;i++){
      var r = rows[i];
      var batas = r.kirimPalingLambat ? esc(r.kirimPalingLambat) : "-";
      if (r.terlambat) batas = '<span class="text-danger fw-bold">' + batas + '</span>';
      html += '<tr>'
        + '<td><div class="fw-bold">' + esc(r.po) + '</div>'
        + '<div class="small text-muted">umur ' + num(r.umurHari) + ' hari</div></td>'
        + '<td>' + esc(r.penyedia || "-") + '</td>'
        + '<td>' + esc(r.barang || "-") + '</td>'
        + '<td class="text-end">' + num(r.dipesan) + '</td>'
        + '<td class="text-end">' + num(r.diterima) + '</td>'
        + '<td class="text-end fw-bold text-info">' + num(r.sisa) + '</td>'
        + '<td class="text-end">' + rp(r.nilaiSisa) + '</td>'
        + '<td>' + batas + '</td>'
        + '</tr>';
    }
    return html;
  }

  function kotakRingkas(judul, nilai, merah){
    return '<div class="border rounded px-3 py-2 text-center' + (merah ? " border-danger" : "") + '">'
      + '<div class="text-muted" style="font-size:10px">' + esc(judul) + '</div>'
      + '<div class="fw-bold' + (merah ? " text-danger" : "") + '">' + esc(nilai) + '</div></div>';
  }

  function ringkasBast(d){
    var belum = (d.total || 0) - (d.jumlahDisetujui || 0);
    return kotakRingkas("Total BAST", d.total || 0, false)
      + kotakRingkas("Total nilai", rp(d.totalNilai), false)
      + kotakRingkas("Sudah disetujui", (d.jumlahDisetujui || 0) + " - " + rp(d.nilaiDisetujui), false)
      + kotakRingkas("Belum disetujui", belum + " - " + rp(d.nilaiBelumDisetujui), belum > 0);
  }

  function ringkasBelum(d){
    return kotakRingkas("Baris", d.total || 0, false)
      + kotakRingkas("Nilai belum datang", rp(d.totalNilai), false)
      + kotakRingkas("Lewat batas kirim", d.jumlahTerlambat || 0, (d.jumlahTerlambat || 0) > 0);
  }

  window["bpHal" + RND] = function(delta){
    var baru = halaman + delta;
    if (baru < 1 || baru > totalHal) return;
    window["bpMuat" + RND](baru);
  };

  window["bpMuat" + RND](1);
})();
</script>
