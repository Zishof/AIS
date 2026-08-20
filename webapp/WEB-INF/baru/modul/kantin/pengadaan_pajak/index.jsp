<%@ page isELIgnored="true" %>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// Halaman "Bayar Pajak" -- tahap penutup rantai Pengadaan POS.
// Bentuknya mengikuti layar Pertanggungjawaban Pajak versi ZKoss: satu rekaman
// setoran mewakili satu jenis pajak, dengan DPP, nilai, NPWP, nama wajib pajak,
// NTPN, dan tanggal setor sebagai bukti.
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
    <h4 class="fw-bold mb-0"><%=Common.getBahasaConfig("Bayar Pajak")%></h4>
    <div class="text-muted small"><%=Common.getBahasaConfig("Setor PPh yang dipotong dan catat PPN dari pembayaran vendor")%></div>
  </div>

  <ul class="nav nav-tabs mb-3">
    <li class="nav-item">
      <a class="nav-link active" href="javascript:void(0)" id="pjTabA<%=rnd%>" onclick="pjTab<%=rnd%>('terutang')">
        <%=Common.getBahasaConfig("Terutang")%>
      </a>
    </li>
    <li class="nav-item">
      <a class="nav-link" href="javascript:void(0)" id="pjTabB<%=rnd%>" onclick="pjTab<%=rnd%>('setoran')">
        <%=Common.getBahasaConfig("Riwayat Setoran")%>
      </a>
    </li>
  </ul>

  <div id="pjPanelTerutang<%=rnd%>">
    <div class="d-flex flex-wrap gap-3 mb-3" id="pjRingkas<%=rnd%>"></div>
    <div class="card border-0 shadow-sm mb-3">
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th style="width:40px"></th>
              <th><%=Common.getBahasaConfig("Pembayaran")%></th>
              <th><%=Common.getBahasaConfig("Penyedia")%></th>
              <th><%=Common.getBahasaConfig("PO / Termin")%></th>
              <th class="text-end"><%=Common.getBahasaConfig("DPP")%></th>
              <th class="text-end"><%=Common.getBahasaConfig("PPh")%></th>
              <th class="text-end"><%=Common.getBahasaConfig("PPN")%></th>
            </tr>
          </thead>
          <tbody id="pjTbody<%=rnd%>">
            <tr><td colspan="7" class="text-center text-muted py-4"><%=Common.getBahasaConfig("Memuat data...")%></td></tr>
          </tbody>
        </table>
      </div>
      <div class="card-footer bg-white d-flex justify-content-between align-items-center">
        <span class="small text-muted" id="pjInfo<%=rnd%>"></span>
        <div>
          <button class="btn btn-sm btn-outline-secondary me-2" onclick="pjSetor<%=rnd%>('PPN')">
            <i class="fas fa-receipt me-1"></i><%=Common.getBahasaConfig("Setor PPN")%>
          </button>
          <button class="btn btn-sm btn-primary" onclick="pjSetor<%=rnd%>('PPH')">
            <i class="fas fa-landmark me-1"></i><%=Common.getBahasaConfig("Setor PPh")%>
          </button>
        </div>
      </div>
    </div>
  </div>

  <div id="pjPanelSetoran<%=rnd%>" class="d-none">
    <div class="card border-0 shadow-sm">
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th><%=Common.getBahasaConfig("Kode")%></th>
              <th><%=Common.getBahasaConfig("Jenis")%></th>
              <th class="text-end"><%=Common.getBahasaConfig("DPP")%></th>
              <th class="text-end"><%=Common.getBahasaConfig("Nilai")%></th>
              <th><%=Common.getBahasaConfig("NTPN")%></th>
              <th><%=Common.getBahasaConfig("Tanggal setor")%></th>
              <th class="text-center"><%=Common.getBahasaConfig("Aksi")%></th>
            </tr>
          </thead>
          <tbody id="pjSetoranTbody<%=rnd%>"></tbody>
        </table>
      </div>
    </div>
  </div>
</div>

<div class="modal fade" id="pjModal<%=rnd%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="pjModalJudul<%=rnd%>"><%=Common.getBahasaConfig("Setor Pajak")%></h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <div class="fw-bold mb-3" id="pjRingkasSetor<%=rnd%>"></div>
        <div class="mb-3">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("NTPN")%> *</label>
          <input type="text" id="pjNtpn<%=rnd%>" class="form-control">
          <div class="form-text"><%=Common.getBahasaConfig("Nomor Transaksi Penerimaan Negara")%></div>
        </div>
        <div class="mb-3">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Tanggal setor")%> *</label>
          <input type="date" id="pjTanggal<%=rnd%>" class="form-control">
        </div>
        <div class="mb-3">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("NPWP")%></label>
          <input type="text" id="pjNpwp<%=rnd%>" class="form-control">
        </div>
        <div class="mb-3">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Nama wajib pajak")%></label>
          <input type="text" id="pjNamaWp<%=rnd%>" class="form-control">
        </div>
        <div class="mb-2">
          <label class="form-label small mb-1"><%=Common.getBahasaConfig("Keterangan")%></label>
          <input type="text" id="pjKeterangan<%=rnd%>" class="form-control">
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
        <button type="button" class="btn btn-primary" onclick="pjSimpanSetor<%=rnd%>()"><%=Common.getBahasaConfig("Setor")%></button>
      </div>
    </div>
  </div>
</div>


<%-- Tombol Bantuan mengambang; isinya sepadan dengan bantuan Desktop/Android. --%>
<jsp:include page="/WEB-INF/baru/include/bantuan_pengadaan.jsp">
  <jsp:param name="tahap" value="pajak"/>
</jsp:include>

<script>
(function(){
  var RND = "<%=rnd%>";
  var DATA_URL = "<%=Common.ROOT%>/Data";
  var terutang = [], setoran = [], dipilih = {}, jenisSetor = "PPH";

  function el(id){ return document.getElementById(id + RND); }
  function esc(s){ return (s==null?"":String(s)).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;"); }
  function rp(n){ return "Rp " + (Number(n)||0).toLocaleString("id-ID"); }
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

  window["pjTab" + RND] = function(nama){
    var terutangAktif = (nama === "terutang");
    el("pjPanelTerutang").classList.toggle("d-none", !terutangAktif);
    el("pjPanelSetoran").classList.toggle("d-none", terutangAktif);
    el("pjTabA").classList.toggle("active", terutangAktif);
    el("pjTabB").classList.toggle("active", !terutangAktif);
  };

  // Pajak kini datang dari DUA sumber (pembayaran vendor dan penerimaan barang),
  // sehingga id baris saja tidak lagi unik -- dipakai kunci gabungan sumber+id.
  function kunciBaris(r){
    var sumber = r.sumber || "PEMBAYARAN";
    var id = (sumber === "BAST") ? r.bast_detail_id : r.detail_id;
    return sumber + "|" + id;
  }
  function totalDipilih(kunci){
    var t = 0;
    for (var i=0;i<terutang.length;i++){
      if (dipilih[kunciBaris(terutang[i])]) t += Number(terutang[i][kunci]) || 0;
    }
    return t;
  }
  function jumlahDipilih(){
    var n = 0;
    for (var k in dipilih) { if (dipilih.hasOwnProperty(k)) n++; }
    return n;
  }
  function renderRingkas(){
    el("pjInfo").textContent = "Dipilih " + jumlahDipilih() + " baris - PPh " + rp(totalDipilih("pph"))
                             + ", PPN " + rp(totalDipilih("ppn"));
  }
  window["pjPilih" + RND] = function(kunci, nilai){
    if (nilai) dipilih[kunci] = true; else delete dipilih[kunci];
    renderRingkas();
  };

  window["pjMuat" + RND] = function(){
    var tbody = el("pjTbody");
    tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">Memuat data...</td></tr>';
    api({action:"pengadaan_pajak_terutang"}).then(function(d){
      if (d.status !== "00" && d.status !== "success"){
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger py-4">' + esc(d.description || "Gagal memuat data.") + '</td></tr>';
        return;
      }
      terutang = d.data || [];
      dipilih = {};
      if (!terutang.length){
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">'
          + esc(d.catatan || 'Tidak ada pajak terutang.') + '</td></tr>';
      } else {
        var h = "";
        for (var i=0;i<terutang.length;i++){
          var r = terutang[i];
          var catatanPajak = r.namaPajak ? ('<div class="small text-muted">' + esc(r.namaPajak) + '</div>') : "";
          var kunci = kunciBaris(r);
          var dariBast = (r.sumber === "BAST");
          var belumSah = (r.dokumenDisetujui === false);
          var lencana = '<span class="badge ' + (dariBast ? 'bg-primary-subtle text-primary-emphasis' : 'bg-info-subtle text-info-emphasis') + ' me-1">'
                      + (dariBast ? 'BAST' : 'BAYAR') + '</span>';
          var catatanSah = belumSah ? '<div class="small text-warning">dokumen belum disetujui - belum dapat disetor</div>' : "";
          h += '<tr>'
            + '<td><input type="checkbox" class="form-check-input"' + (belumSah ? ' disabled' : '')
            + ' onchange="pjPilih' + RND + '(&quot;' + kunci + '&quot;,this.checked)"></td>'
            + '<td>' + lencana + '<span class="fw-bold">' + esc(r.dokumen || r.bayar || "") + '</span>'
            + '<div class="small text-muted">' + esc(r.tanggal || "") + '</div>' + catatanSah + '</td>'
            + '<td>' + esc(r.penyedia || "-") + '</td>'
            + '<td class="small">' + esc(r.po || "") + ' ' + esc(r.termin || "")
            + (r.barang ? ('<div class="text-muted">' + esc(r.barang) + '</div>') : "") + '</td>'
            + '<td class="text-end">' + rp(r.dpp) + '</td>'
            + '<td class="text-end fw-bold">' + rp(r.pph) + catatanPajak + '</td>'
            + '<td class="text-end">' + rp(r.ppn) + '</td>'
            + '</tr>';
        }
        tbody.innerHTML = h;
      }
      var kotak = [["Baris", d.total || 0], ["PPh terutang", rp(d.totalPph)], ["PPN tercatat", rp(d.totalPpn)]];
      var hr = "";
      for (var k=0;k<kotak.length;k++){
        hr += '<div class="border rounded px-3 py-2 text-center">'
           + '<div class="text-muted" style="font-size:10px">' + esc(kotak[k][0]) + '</div>'
           + '<div class="fw-bold">' + esc(kotak[k][1]) + '</div></div>';
      }
      el("pjRingkas").innerHTML = hr;
      renderRingkas();
    });

    api({action:"pengadaan_pajak_daftar"}).then(function(d){
      setoran = d.data || [];
      var tb = el("pjSetoranTbody");
      if (!setoran.length){
        tb.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">Belum ada setoran pajak tercatat.</td></tr>';
        return;
      }
      var h = "";
      for (var i=0;i<setoran.length;i++){
        var r = setoran[i];
        var aksi = r.aktif
          ? '<button class="btn btn-sm btn-outline-secondary" title="Batalkan setoran" onclick="pjBatal' + RND + '(' + r.id + ')"><i class="fas fa-undo"></i></button>'
          : '<span class="small text-muted">dibatalkan</span>';
        var jenis = esc(r.jenis) + (r.jenisPajak ? (" - " + esc(r.jenisPajak)) : "");
        h += '<tr>'
          + '<td class="fw-bold">' + esc(r.kode) + '</td>'
          + '<td>' + jenis + '</td>'
          + '<td class="text-end">' + rp(r.dpp) + '</td>'
          + '<td class="text-end">' + rp(r.nilai) + '</td>'
          + '<td>' + esc(r.ntpn || "-") + '</td>'
          + '<td>' + esc(r.tanggalSetor || "-") + '</td>'
          + '<td class="text-center">' + aksi + '</td>'
          + '</tr>';
      }
      tb.innerHTML = h;
    });
  };

  window["pjSetor" + RND] = function(jenis){
    if (!jumlahDipilih()){ pesan("Centang minimal satu baris pajak.", false); return; }
    var nilai = totalDipilih(jenis === "PPH" ? "pph" : "ppn");
    if (nilai <= 0){ pesan("Baris terpilih tidak memiliki " + jenis + " untuk disetor.", false); return; }
    jenisSetor = jenis;
    el("pjModalJudul").textContent = "Setor " + jenis;
    el("pjRingkasSetor").textContent = jumlahDipilih() + " baris - nilai " + rp(nilai);
    el("pjNtpn").value = "";
    el("pjNpwp").value = "";
    el("pjNamaWp").value = "";
    el("pjKeterangan").value = "";
    new bootstrap.Modal(document.getElementById("pjModal" + RND)).show();
  };

  window["pjSimpanSetor" + RND] = function(){
    var ntpn = el("pjNtpn").value.trim();
    var tgl = keTampilan(el("pjTanggal").value);
    if (!ntpn){ pesan("NTPN wajib diisi sebagai bukti setor.", false); return; }
    if (!tgl){ pesan("Tanggal setor wajib diisi.", false); return; }
    var detail = [];
    for (var k in dipilih){
      if (!dipilih.hasOwnProperty(k)) continue;
      var bagian = k.split("|");
      var id = Number(bagian[1]);
      detail.push(bagian[0] === "BAST" ? {bast_detail_id: id} : {detail_id: id});
    }
    api({action:"pengadaan_pajak_setor", jenis: jenisSetor, ntpn: ntpn, tanggalSetor: tgl,
         npwp: el("pjNpwp").value.trim(), namaWp: el("pjNamaWp").value.trim(),
         keterangan: el("pjKeterangan").value.trim(), detail: detail})
      .then(function(d){
        var ok = d.status === "00" || d.status === "success";
        pesan(ok ? ("Setoran " + (d.kode || "") + " tercatat: " + rp(d.nilai))
                 : (d.description || "Gagal mencatat setoran."), ok);
        if (ok){
          bootstrap.Modal.getInstance(document.getElementById("pjModal" + RND)).hide();
          window["pjMuat" + RND]();
        }
      });
  };

  window["pjBatal" + RND] = function(id){
    if (!window.confirm("Batalkan setoran ini? Pajaknya kembali menjadi terutang dan dapat disetor ulang.")) return;
    api({action:"pengadaan_pajak_batal", id:id}).then(function(d){
      var ok = d.status === "00" || d.status === "success";
      pesan(ok ? ("Setoran dibatalkan; " + (d.barisDilepas || 0) + " baris kembali terutang.")
               : (d.description || "Gagal membatalkan setoran."), ok);
      if (ok) window["pjMuat" + RND]();
    });
  };

  window["pjMuat" + RND]();
})();
</script>
