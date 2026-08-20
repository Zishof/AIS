<%@ page isELIgnored="true" %>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// Halaman "Barang Dalam Proses" -- modul Pengadaan POS versi JSP.
// Bukan dokumen tersendiri melainkan pandangan turunan dari selisih PO dan BAST,
// memakai definisi yang sama dengan pagar penerimaan di server.
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
    <div class="text-muted small"><%=Common.getBahasaConfig("Barang yang sudah dipesan tetapi belum diterima")%></div>
  </div>

  <div class="card border-0 shadow-sm mb-3">
    <div class="card-body py-3">
      <div class="row g-2 align-items-end">
        <div class="col-md-5">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Cari kode PO / nama barang")%></label>
          <input type="text" id="bpCari<%=rnd%>" class="form-control" onkeydown="if(event.key==='Enter')bpMuat<%=rnd%>(1)">
        </div>
        <div class="col-md-3">
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
          <tr>
            <th><%=Common.getBahasaConfig("PO")%></th>
            <th><%=Common.getBahasaConfig("Penyedia")%></th>
            <th><%=Common.getBahasaConfig("Barang")%></th>
            <th class="text-end"><%=Common.getBahasaConfig("Dipesan")%></th>
            <th class="text-end"><%=Common.getBahasaConfig("Diterima")%></th>
            <th class="text-end"><%=Common.getBahasaConfig("Belum datang")%></th>
            <th class="text-end"><%=Common.getBahasaConfig("Nilai")%></th>
            <th><%=Common.getBahasaConfig("Batas kirim")%></th>
          </tr>
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
  var halaman = 1, totalHal = 1;

  function el(id){ return document.getElementById(id + RND); }
  function esc(s){ return (s==null?"":String(s)).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;"); }
  function rp(n){ return "Rp " + (Number(n)||0).toLocaleString("id-ID"); }
  function num(n){ n = Number(n)||0; return (n === Math.round(n)) ? String(Math.round(n)) : String(n); }
  function api(payload){
    return fetch(DATA_URL, {method:"POST", headers:{"Content-Type":"application/json"},
                            body: JSON.stringify(payload)}).then(function(r){ return r.json(); });
  }

  window["bpMuat" + RND] = function(hal){
    halaman = hal || halaman;
    var tbody = el("bpTbody");
    tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">Memuat data...</td></tr>';
    api({action:"pengadaan_bdp_daftar", cari: el("bpCari").value.trim(),
         hanyaTerlambat: el("bpTerlambat").checked, page: halaman, pageSize: 25})
      .then(function(d){
        if (d.status !== "00" && d.status !== "success"){
          tbody.innerHTML = '<tr><td colspan="8" class="text-center text-danger py-4">' + esc(d.description || "Gagal memuat data.") + '</td></tr>';
          return;
        }
        var rows = d.data || [];
        totalHal = Math.max(1, Math.ceil((d.total || rows.length) / 25));
        if (!rows.length){
          tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">'
            + 'Tidak ada barang dalam proses. Seluruh pesanan yang disetujui sudah diterima.</td></tr>';
        } else {
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
          tbody.innerHTML = html;
        }
        var kotak = [["Baris", d.total || 0], ["Nilai belum datang", rp(d.totalNilai)],
                     ["Lewat batas kirim", d.jumlahTerlambat || 0]];
        var hr = "";
        for (var k=0;k<kotak.length;k++){
          var merah = (k === 2 && (d.jumlahTerlambat || 0) > 0);
          hr += '<div class="border rounded px-3 py-2 text-center' + (merah ? " border-danger" : "") + '">'
             + '<div class="text-muted" style="font-size:10px">' + esc(kotak[k][0]) + '</div>'
             + '<div class="fw-bold' + (merah ? " text-danger" : "") + '">' + esc(kotak[k][1]) + '</div></div>';
        }
        el("bpRingkas").innerHTML = hr;
        el("bpInfo").textContent = "Halaman " + halaman + " dari " + totalHal + " - total " + (d.total || rows.length) + " baris";
        el("bpPrev").disabled = halaman <= 1;
        el("bpNext").disabled = halaman >= totalHal;
      })
      .catch(function(){ tbody.innerHTML = '<tr><td colspan="8" class="text-center text-danger py-4">Kesalahan koneksi.</td></tr>'; });
  };
  window["bpHal" + RND] = function(delta){
    var baru = halaman + delta;
    if (baru < 1 || baru > totalHal) return;
    window["bpMuat" + RND](baru);
  };

  window["bpMuat" + RND](1);
})();
</script>
