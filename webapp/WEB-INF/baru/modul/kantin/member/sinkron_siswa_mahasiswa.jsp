<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%--
  Sinkronisasi Siswa/Mahasiswa -> AnggotaKoperasi (versi JSP, tab baru di Manajemen Anggota).
  Setara fungsional dgn tombol ZK "Singkronkan Mahasiswa"/"Singkronkan Siswa" di
  AnggotaKoperasiAction -- KEDUANYA kini memakai ulang logika yg SAMA & SUDAH DIPERBAIKI di
  Common.checkApakahMahasiswaOtomatisMenjadiAnggotaKoperasi / checkApakahSiswaOtomatisMenjadi...
  (null-guard NIM/NIS, escape kutip-satu, refresh SEMUA field simetris saat create MAUPUN update,
  session tak lagi dibuka/ditutup per-baris). Endpoint: member/sinkron_siswa_mahasiswa_service.jsp.
--%>
<%
Tbmuser uSk = Common.getCurrentUser(request);
if (uSk==null || uSk.getUserId()==null){ response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); out.print("Unauthorized"); return; }
boolean bolehSk = ais.action.master.koperasi.helper.LokasiKantinUtil.bolehKelola(request);
String SVC = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fmember&s=sinkron_siswa_mahasiswa_service";
String R = Common.getGeneratedBarCode(6);
%>
<div class="ssm">
  <div class="mb-3">
    <div class="text-muted small"><%=Common.getBahasaConfig("Daftarkan Mahasiswa/Siswa aktif secara otomatis sebagai Anggota Koperasi. Menjalankan ulang sinkronisasi akan MEMPERBARUI data anggota yang sudah ada (nama, alamat, email) mengikuti data Mahasiswa/Siswa terbaru.")%></div>
  </div>

  <% if (!bolehSk) { %>
  <div class="alert alert-warning">
    <%=Common.getBahasaConfig("Hanya admin kantin/koperasi yang boleh menjalankan sinkronisasi.")%>
  </div>
  <% } %>

  <div class="row g-3">
    <div class="col-md-4">
      <label class="form-label small fw-bold mb-1"><%=Common.getBahasaConfig("Koperasi Tujuan")%></label>
      <select class="form-select" id="koperasi<%=R%>"></select>
    </div>
  </div>

  <div class="row g-3 mt-1">
    <!-- MAHASISWA -->
    <div class="col-lg-6">
      <div class="card border-0 shadow-sm rounded-4 h-100"><div class="card-body p-3">
        <h6 class="fw-bold"><i class="fas fa-user-graduate text-primary me-2"></i><%=Common.getBahasaConfig("Sinkronkan dari Data Mahasiswa")%></h6>
        <div class="row g-2 mt-1">
          <div class="col-6">
            <label class="form-label small mb-1"><%=Common.getBahasaConfig("Tahun Angkatan")%> *</label>
            <input type="number" class="form-control form-control-sm" id="mhsTahun<%=R%>">
          </div>
          <div class="col-6"></div>
          <div class="col-6">
            <label class="form-label small mb-1"><%=Common.getBahasaConfig("Fakultas")%></label>
            <select class="form-select form-select-sm" id="mhsFakultas<%=R%>"><option value=""><%=Common.getBahasaConfig("-- Semua --")%></option></select>
          </div>
          <div class="col-6">
            <label class="form-label small mb-1"><%=Common.getBahasaConfig("Program Studi")%></label>
            <select class="form-select form-select-sm" id="mhsJurusan<%=R%>"><option value=""><%=Common.getBahasaConfig("-- Semua --")%></option></select>
          </div>
        </div>
        <button class="btn btn-primary btn-sm mt-3" id="btnMhs<%=R%>" <%=bolehSk?"":"disabled"%>><i class="fas fa-sync me-1"></i><%=Common.getBahasaConfig("Mulai Sinkronisasi")%></button>
        <div class="small mt-2" id="hasilMhs<%=R%>"></div>
      </div></div>
    </div>

    <!-- SISWA -->
    <div class="col-lg-6">
      <div class="card border-0 shadow-sm rounded-4 h-100"><div class="card-body p-3">
        <h6 class="fw-bold"><i class="fas fa-child text-primary me-2"></i><%=Common.getBahasaConfig("Sinkronkan dari Data Siswa")%></h6>
        <div class="row g-2 mt-1">
          <div class="col-6">
            <label class="form-label small mb-1"><%=Common.getBahasaConfig("Tahun Masuk")%> *</label>
            <input type="number" class="form-control form-control-sm" id="siswaTahun<%=R%>">
          </div>
          <div class="col-6"></div>
          <div class="col-6">
            <label class="form-label small mb-1"><%=Common.getBahasaConfig("Yayasan")%></label>
            <select class="form-select form-select-sm" id="siswaYayasan<%=R%>"><option value=""><%=Common.getBahasaConfig("-- Semua --")%></option></select>
          </div>
          <div class="col-6">
            <label class="form-label small mb-1"><%=Common.getBahasaConfig("Sekolah")%></label>
            <select class="form-select form-select-sm" id="siswaSekolah<%=R%>"><option value=""><%=Common.getBahasaConfig("-- Semua --")%></option></select>
          </div>
        </div>
        <button class="btn btn-primary btn-sm mt-3" id="btnSiswa<%=R%>" <%=bolehSk?"":"disabled"%>><i class="fas fa-sync me-1"></i><%=Common.getBahasaConfig("Mulai Sinkronisasi")%></button>
        <div class="small mt-2" id="hasilSiswa<%=R%>"></div>
      </div></div>
    </div>
  </div>
</div>
<script>
(function(){
  var SVC="<%=SVC%>", RID="<%=R%>";
  var g=function(id){ return document.getElementById(id+RID); };
  var post=async function(p){ var b=Object.keys(p).map(function(k){return encodeURIComponent(k)+"="+encodeURIComponent(p[k]==null?"":p[k]);}).join("&");
    var r=await fetch(SVC,{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:b}); return await r.json(); };
  var isiSelect=function(el, data, placeholder){
    var opt='<option value="">'+placeholder+'</option>';
    (data||[]).forEach(function(x){ opt+='<option value="'+x.id+'">'+x.nama+'</option>'; });
    el.innerHTML=opt;
  };

  var tahunSkrg = new Date().getFullYear();
  g("mhsTahun").value = tahunSkrg;
  g("siswaTahun").value = tahunSkrg;

  (async function(){
    var jk=await post({aksi:"listKoperasi"});
    isiSelect(g("koperasi"), jk.status=="00"?jk.data:[], "<%=Common.getBahasaConfig("-- Pilih Koperasi --")%>");
    var jf=await post({aksi:"listFakultas"});
    isiSelect(g("mhsFakultas"), jf.status=="00"?jf.data:[], "<%=Common.getBahasaConfig("-- Semua --")%>");
    var jy=await post({aksi:"listYayasan"});
    isiSelect(g("siswaYayasan"), jy.status=="00"?jy.data:[], "<%=Common.getBahasaConfig("-- Semua --")%>");
    var jj=await post({aksi:"listJurusan"});
    isiSelect(g("mhsJurusan"), jj.status=="00"?jj.data:[], "<%=Common.getBahasaConfig("-- Semua --")%>");
    var js=await post({aksi:"listSekolah"});
    isiSelect(g("siswaSekolah"), js.status=="00"?js.data:[], "<%=Common.getBahasaConfig("-- Semua --")%>");
  })();

  g("mhsFakultas").addEventListener("change", async function(){
    var jj=await post({aksi:"listJurusan", fakultasId:g("mhsFakultas").value||""});
    isiSelect(g("mhsJurusan"), jj.status=="00"?jj.data:[], "<%=Common.getBahasaConfig("-- Semua --")%>");
  });
  g("siswaYayasan").addEventListener("change", async function(){
    var js=await post({aksi:"listSekolah", yayasanId:g("siswaYayasan").value||""});
    isiSelect(g("siswaSekolah"), js.status=="00"?js.data:[], "<%=Common.getBahasaConfig("-- Semua --")%>");
  });

  g("btnMhs").onclick = async function(){
    if (!g("koperasi").value) { tampilkanPesanGagalFormal("sinkronisasi data Mahasiswa ke Koperasi", "<%=Common.getBahasaConfig("Koperasi tujuan belum dipilih, padahal wajib dipilih.")%>", ["Pilih Koperasi tujuan terlebih dahulu."]); return; }
    if (!g("mhsTahun").value) { tampilkanPesanGagalFormal("sinkronisasi data Mahasiswa ke Koperasi", "<%=Common.getBahasaConfig("Tahun Angkatan belum diisi, padahal wajib diisi.")%>", ["Isi Tahun Angkatan terlebih dahulu."]); return; }
    var btn=g("btnMhs"); var ori=btn.innerHTML;
    btn.disabled=true; btn.innerHTML='<span class="spinner-border spinner-border-sm me-1"></span><%=Common.getBahasaConfig("Memproses...")%>';
    g("hasilMhs").innerHTML='';
    var r=await post({aksi:"sinkronMahasiswa", koperasiId:g("koperasi").value, tahun:g("mhsTahun").value, fakultasId:g("mhsFakultas").value||"", jurusanId:g("mhsJurusan").value||""});
    btn.disabled=false; btn.innerHTML=ori;
    if (r.status=="00") {
      g("hasilMhs").innerHTML='<span class="text-success"><i class="fas fa-check-circle me-1"></i>'+r.berhasil+' <%=Common.getBahasaConfig("dari")%> '+r.total+' <%=Common.getBahasaConfig("mahasiswa berhasil disinkronkan")%>'+(r.gagal>0?(', <span class="text-danger">'+r.gagal+' <%=Common.getBahasaConfig("gagal (lihat log server)")%></span>'):'')+'.</span>';
    } else {
      g("hasilMhs").innerHTML='<span class="text-danger">'+(r.message||"<%=Common.getBahasaConfig("Gagal.")%>")+'</span>';
    }
  };

  g("btnSiswa").onclick = async function(){
    if (!g("koperasi").value) { tampilkanPesanGagalFormal("sinkronisasi data Siswa ke Koperasi", "<%=Common.getBahasaConfig("Koperasi tujuan belum dipilih, padahal wajib dipilih.")%>", ["Pilih Koperasi tujuan terlebih dahulu."]); return; }
    if (!g("siswaTahun").value) { tampilkanPesanGagalFormal("sinkronisasi data Siswa ke Koperasi", "<%=Common.getBahasaConfig("Tahun Masuk belum diisi, padahal wajib diisi.")%>", ["Isi Tahun Masuk terlebih dahulu."]); return; }
    var btn=g("btnSiswa"); var ori=btn.innerHTML;
    btn.disabled=true; btn.innerHTML='<span class="spinner-border spinner-border-sm me-1"></span><%=Common.getBahasaConfig("Memproses...")%>';
    g("hasilSiswa").innerHTML='';
    var r=await post({aksi:"sinkronSiswa", koperasiId:g("koperasi").value, tahun:g("siswaTahun").value, yayasanId:g("siswaYayasan").value||"", sekolahId:g("siswaSekolah").value||""});
    btn.disabled=false; btn.innerHTML=ori;
    if (r.status=="00") {
      g("hasilSiswa").innerHTML='<span class="text-success"><i class="fas fa-check-circle me-1"></i>'+r.berhasil+' <%=Common.getBahasaConfig("dari")%> '+r.total+' <%=Common.getBahasaConfig("siswa berhasil disinkronkan")%>'+(r.gagal>0?(', <span class="text-danger">'+r.gagal+' <%=Common.getBahasaConfig("gagal (lihat log server)")%></span>'):'')+'.</span>';
    } else {
      g("hasilSiswa").innerHTML='<span class="text-danger">'+(r.message||"<%=Common.getBahasaConfig("Gagal.")%>")+'</span>';
    }
  };
})();
</script>
