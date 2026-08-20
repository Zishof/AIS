<%@ page isELIgnored="true" %>
<%--
  ============================================================================
  Tab "Satuan Kerja" pada halaman Anggota (versi JSP).
  Kelola satuan kerja, lalu pilih member mana saja yang tergolong di dalamnya.

  Data: member/satuan_kerja_service.jsp -> SatuanKerjaKantinHelper (kelas yang
  SAMA dipakai POS Desktop/Android), sehingga daftar dan aturan penugasannya
  tidak mungkin berbeda antar kanal.

  isELIgnored SENGAJA dinyalakan: berkas ini memakai template literal
  JavaScript, dan ${...} di dalamnya akan dievaluasi peladen bila EL aktif.
  ============================================================================
--%>
<%@page import="ais.common.Common"%>
<%
String rndSk = Common.getGeneratedBarCode(7);
String svcSk = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin&s=member/satuan_kerja_service";
%>

<div class="d-flex flex-wrap gap-2 align-items-center mb-3">
  <div class="flex-grow-1" style="min-width:220px">
    <input type="text" id="skCari<%=rndSk%>" class="form-control"
           placeholder="<%=Common.getBahasaConfig("Cari kode/nama satuan kerja...")%>">
  </div>
  <div class="form-check">
    <input class="form-check-input" type="checkbox" id="skNonaktif<%=rndSk%>">
    <label class="form-check-label small" for="skNonaktif<%=rndSk%>">
      <%=Common.getBahasaConfig("Tampilkan yang nonaktif juga")%>
    </label>
  </div>
  <button type="button" class="btn btn-primary" id="skTambah<%=rndSk%>">
    <i class="fas fa-plus me-1"></i><%=Common.getBahasaConfig("Tambah Satuan Kerja")%>
  </button>
</div>

<div class="table-responsive">
  <table class="table table-sm table-hover align-middle">
    <thead class="table-light">
      <tr>
        <th><%=Common.getBahasaConfig("Kode")%></th>
        <th><%=Common.getBahasaConfig("Nama")%></th>
        <th><%=Common.getBahasaConfig("Keterangan")%></th>
        <th class="text-end"><%=Common.getBahasaConfig("Jumlah Member")%></th>
        <th class="text-center"><%=Common.getBahasaConfig("Status")%></th>
        <th class="text-center"><%=Common.getBahasaConfig("Aksi")%></th>
      </tr>
    </thead>
    <tbody id="skBody<%=rndSk%>">
      <tr><td colspan="6" class="text-center text-muted py-4">
        <%=Common.getBahasaConfig("Memuat...")%>
      </td></tr>
    </tbody>
  </table>
</div>

<div class="modal fade" id="skModal<%=rndSk%>" tabindex="-1">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <h6 class="modal-title" id="skModalJudul<%=rndSk%>"></h6>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <input type="hidden" id="skId<%=rndSk%>">
        <div class="mb-2">
          <label class="form-label small"><%=Common.getBahasaConfig("Kode")%></label>
          <input type="text" class="form-control" id="skKode<%=rndSk%>">
        </div>
        <div class="mb-2">
          <label class="form-label small"><%=Common.getBahasaConfig("Nama")%> *</label>
          <input type="text" class="form-control" id="skNama<%=rndSk%>">
        </div>
        <div class="mb-2">
          <label class="form-label small"><%=Common.getBahasaConfig("Keterangan")%></label>
          <input type="text" class="form-control" id="skKeterangan<%=rndSk%>">
        </div>
        <div class="mb-2">
          <label class="form-label small"><%=Common.getBahasaConfig("Alamat")%></label>
          <input type="text" class="form-control" id="skAlamat<%=rndSk%>">
        </div>
        <div class="form-check">
          <input class="form-check-input" type="checkbox" id="skAktif<%=rndSk%>" checked>
          <label class="form-check-label" for="skAktif<%=rndSk%>">
            <%=Common.getBahasaConfig("Aktif")%>
          </label>
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-light" data-bs-dismiss="modal">
          <%=Common.getBahasaConfig("Batal")%>
        </button>
        <button type="button" class="btn btn-primary" id="skSimpan<%=rndSk%>">
          <%=Common.getBahasaConfig("Simpan")%>
        </button>
      </div>
    </div>
  </div>
</div>

<div class="modal fade" id="skModalMember<%=rndSk%>" tabindex="-1">
  <div class="modal-dialog modal-lg">
    <div class="modal-content">
      <div class="modal-header">
        <h6 class="modal-title" id="skMemberJudul<%=rndSk%>"></h6>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <input type="hidden" id="skMemberSkId<%=rndSk%>">
        <input type="text" class="form-control mb-2" id="skMemberCari<%=rndSk%>"
               placeholder="<%=Common.getBahasaConfig("Cari kode/nama member...")%>">
        <div class="small text-muted mb-1" id="skMemberJumlah<%=rndSk%>"></div>
        <div id="skMemberList<%=rndSk%>" style="max-height:50vh;overflow:auto"></div>
        <div class="small fst-italic text-muted mt-2">
          <%=Common.getBahasaConfig("Member yang dicentang menjadi anggota satuan kerja ini; yang dilepas centangnya dikeluarkan.")%>
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-light" data-bs-dismiss="modal">
          <%=Common.getBahasaConfig("Batal")%>
        </button>
        <button type="button" class="btn btn-primary" id="skMemberSimpan<%=rndSk%>">
          <%=Common.getBahasaConfig("Simpan")%>
        </button>
      </div>
    </div>
  </div>
</div>

<script>
(function(){
  var SVC = "<%=svcSk%>";
  var RND = "<%=rndSk%>";
  var el = function(id){ return document.getElementById(id + RND); };
  var esc = function(s){
    return (s==null?"":String(s)).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;");
  };
  // Pilihan member disimpan di luar daftar yang tampil: mencari ulang TIDAK
  // boleh menghapus centang yang belum sempat disimpan.
  var terpilih = {};

  function panggil(params){
    var body = new URLSearchParams(params).toString();
    return fetch(SVC, {
      method: "POST",
      headers: {"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"},
      body: body
    }).then(function(r){ return r.json(); });
  }

  function muatDaftar(){
    var tbody = el("skBody");
    tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">Memuat...</td></tr>';
    panggil({
      aksi: "list",
      cari: el("skCari").value || "",
      tampilkan_nonaktif: el("skNonaktif").checked ? "true" : "false"
    }).then(function(res){
      var rows = (res && res.data) ? res.data : [];
      if (!rows.length) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">Belum ada satuan kerja.</td></tr>';
        return;
      }
      var h = "";
      for (var i=0;i<rows.length;i++){
        var r = rows[i];
        var aktif = r.aktif === true;
        h += '<tr>'
           + '<td>' + esc(r.kode) + '</td>'
           + '<td>' + esc(r.nama) + '</td>'
           + '<td>' + esc(r.keterangan) + '</td>'
           + '<td class="text-end">' + (r.jumlahMember || 0) + '</td>'
           + '<td class="text-center"><span class="badge ' + (aktif ? 'bg-success' : 'bg-secondary') + '">'
           + (aktif ? 'Aktif' : 'Nonaktif') + '</span></td>'
           + '<td class="text-center">'
           + '<button type="button" class="btn btn-sm btn-outline-secondary me-1" data-aksi="member" data-id="' + esc(r.id) + '" data-nama="' + esc(r.nama) + '" title="Atur member"><i class="fas fa-users"></i></button>'
           + '<button type="button" class="btn btn-sm btn-outline-primary me-1" data-aksi="ubah" data-id="' + esc(r.id) + '" title="Ubah"><i class="fas fa-pen"></i></button>'
           + (aktif ? '<button type="button" class="btn btn-sm btn-outline-danger" data-aksi="nonaktif" data-id="' + esc(r.id) + '" data-nama="' + esc(r.nama) + '" title="Nonaktifkan"><i class="fas fa-ban"></i></button>' : '')
           + '</td></tr>';
      }
      tbody.innerHTML = h;
      simpanBaris(rows);
    }).catch(function(){
      tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger py-4">Gagal memuat data.</td></tr>';
    });
  }

  var cacheBaris = {};
  function simpanBaris(rows){
    cacheBaris = {};
    for (var i=0;i<rows.length;i++){ cacheBaris[String(rows[i].id)] = rows[i]; }
  }

  function bukaForm(data){
    el("skModalJudul").textContent = data ? "Ubah Satuan Kerja" : "Tambah Satuan Kerja";
    el("skId").value = data ? data.id : "";
    el("skKode").value = data ? (data.kode || "") : "";
    el("skNama").value = data ? (data.nama || "") : "";
    el("skKeterangan").value = data ? (data.keterangan || "") : "";
    el("skAlamat").value = data ? (data.alamat || "") : "";
    el("skAktif").checked = data ? data.aktif === true : true;
    new bootstrap.Modal(el("skModal")).show();
  }

  function muatMember(){
    var wadah = el("skMemberList");
    wadah.innerHTML = '<div class="text-muted small py-3">Memuat...</div>';
    panggil({
      aksi: "anggota_list",
      satuan_kerja_id: el("skMemberSkId").value,
      cari: el("skMemberCari").value || ""
    }).then(function(res){
      var rows = (res && res.data) ? res.data : [];
      if (!rows.length) {
        wadah.innerHTML = '<div class="text-muted small py-3">Tidak ada member yang cocok.</div>';
        perbaruiJumlah();
        return;
      }
      var h = "";
      for (var i=0;i<rows.length;i++){
        var m = rows[i];
        var id = String(m.id);
        // Centang awal hanya diambil saat id itu belum pernah disentuh pengguna.
        if (!(id in terpilih) && m.terpilih === true) { terpilih[id] = true; }
        var ket = [];
        if (m.kode) { ket.push(esc(m.kode)); }
        if (m.punyaAkun !== true) { ket.push("tanpa akun login"); }
        if (m.satuanKerjaNama && m.satuanKerjaNama !== el("skMemberJudul").getAttribute("data-nama")) {
          ket.push("kini di: " + esc(m.satuanKerjaNama));
        }
        h += '<div class="form-check py-1 border-bottom">'
           + '<input class="form-check-input skChk" type="checkbox" value="' + esc(id) + '" id="skM' + esc(id) + RND + '"'
           + (terpilih[id] ? ' checked' : '') + '>'
           + '<label class="form-check-label" for="skM' + esc(id) + RND + '">'
           + esc(m.nama) + '<br><span class="small text-muted">' + ket.join("  &middot;  ") + '</span>'
           + '</label></div>';
      }
      wadah.innerHTML = h;
      perbaruiJumlah();
    }).catch(function(){
      wadah.innerHTML = '<div class="text-danger small py-3">Gagal memuat member.</div>';
    });
  }

  function perbaruiJumlah(){
    var n = 0;
    for (var k in terpilih) { if (terpilih[k]) { n++; } }
    el("skMemberJumlah").textContent = n + " member dipilih";
  }

  document.addEventListener("change", function(ev){
    var t = ev.target;
    if (t && t.classList && t.classList.contains("skChk")) {
      terpilih[String(t.value)] = t.checked;
      perbaruiJumlah();
    }
  });

  document.addEventListener("click", function(ev){
    var b = ev.target && ev.target.closest ? ev.target.closest("button[data-aksi]") : null;
    if (!b || !el("skBody").contains(b)) { return; }
    var id = b.getAttribute("data-id");
    var aksi = b.getAttribute("data-aksi");
    if (aksi === "ubah") {
      bukaForm(cacheBaris[String(id)]);
    } else if (aksi === "member") {
      terpilih = {};
      el("skMemberSkId").value = id;
      var nama = b.getAttribute("data-nama") || "";
      el("skMemberJudul").textContent = "Member — " + nama;
      el("skMemberJudul").setAttribute("data-nama", nama);
      el("skMemberCari").value = "";
      new bootstrap.Modal(el("skModalMember")).show();
      muatMember();
    } else if (aksi === "nonaktif") {
      if (!confirm('"' + (b.getAttribute("data-nama") || "") + '" akan disembunyikan dari daftar.\n\nBarisnya tidak dihapus karena dapat dirujuk member, pegawai, dan dokumen lain. Member yang sudah ditugaskan tidak diubah.')) { return; }
      panggil({ aksi: "hapus", id: id }).then(muatDaftar);
    }
  });

  el("skTambah").addEventListener("click", function(){ bukaForm(null); });

  el("skSimpan").addEventListener("click", function(){
    if (!el("skNama").value.trim()) { alert("Nama satuan kerja wajib diisi."); return; }
    panggil({
      aksi: "simpan",
      id: el("skId").value || "",
      kode: el("skKode").value.trim(),
      nama: el("skNama").value.trim(),
      keterangan: el("skKeterangan").value.trim(),
      alamat: el("skAlamat").value.trim(),
      aktif: el("skAktif").checked ? "true" : "false"
    }).then(function(res){
      if (res && res.status !== "00") { alert(res.description || "Gagal menyimpan."); return; }
      bootstrap.Modal.getInstance(el("skModal")).hide();
      muatDaftar();
    });
  });

  el("skMemberSimpan").addEventListener("click", function(){
    var ids = [];
    for (var k in terpilih) { if (terpilih[k]) { ids.push(k); } }
    panggil({
      aksi: "anggota_simpan",
      satuan_kerja_id: el("skMemberSkId").value,
      anggota_id: ids.join(",")
    }).then(function(res){
      if (res && res.status !== "00") { alert(res.description || "Gagal menyimpan."); return; }
      bootstrap.Modal.getInstance(el("skModalMember")).hide();
      muatDaftar();
    });
  });

  var timerCari = null;
  el("skCari").addEventListener("input", function(){
    clearTimeout(timerCari);
    timerCari = setTimeout(muatDaftar, 450);
  });
  el("skNonaktif").addEventListener("change", muatDaftar);

  var timerCariMember = null;
  el("skMemberCari").addEventListener("input", function(){
    clearTimeout(timerCariMember);
    timerCariMember = setTimeout(muatMember, 450);
  });

  muatDaftar();
})();
</script>
